package com.gumtree.csagent.service.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.model.KbChunk;
import com.gumtree.csagent.repository.KbArticleRepository;
import com.gumtree.csagent.repository.KbChunkRepository;
import com.gumtree.csagent.service.embedding.EmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ApplicationRunner that ingests knowledge base articles into the database
 * when the --ingest argument is passed.
 *
 * Reads from:
 *   - data/knowledge/knowledge_base_articles.json
 *   - data/knowledge/article_uc_mapping.csv
 *
 * For each article:
 *   1. Save to kb_articles table
 *   2. Clean HTML, chunk text (ChunkingService)
 *   3. Embed each chunk via EmbeddingClient.embedBatch()
 *   4. Save chunks with embeddings to kb_chunks table
 */
@Slf4j
@Component
public class KnowledgeIngestionRunner implements ApplicationRunner {

    private static final String ARTICLES_JSON_PATH = "data/knowledge/knowledge_base_articles.json";
    private static final String UC_MAPPING_CSV_PATH = "data/knowledge/article_uc_mapping.csv";
    private static final int EMBEDDING_BATCH_SIZE = 20;

    private final KbArticleRepository kbArticleRepository;
    private final KbChunkRepository kbChunkRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    public KnowledgeIngestionRunner(KbArticleRepository kbArticleRepository,
                                     KbChunkRepository kbChunkRepository,
                                     ChunkingService chunkingService,
                                     EmbeddingClient embeddingClient,
                                     ObjectMapper objectMapper) {
        this.kbArticleRepository = kbArticleRepository;
        this.kbChunkRepository = kbChunkRepository;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("ingest") && !args.getNonOptionArgs().contains("--ingest")) {
            log.debug("--ingest not specified, skipping knowledge ingestion");
            return;
        }

        log.info("=== Knowledge Ingestion Started ===");
        long startTime = System.currentTimeMillis();

        // Load UC mapping from CSV
        Map<String, String[]> ucMappings = loadUcMappings();
        log.info("Loaded {} UC mappings from CSV", ucMappings.size());

        // Load articles from JSON
        List<JsonNode> articles = loadArticlesJson();
        log.info("Loaded {} articles from JSON", articles.size());

        // Get existing article IDs to skip
        Set<String> existingIds = kbArticleRepository.findAll().stream()
                .map(KbArticle::getArticleId)
                .collect(Collectors.toSet());
        log.info("Found {} existing articles in DB (will be skipped)", existingIds.size());

        int articlesProcessed = 0;
        int articlesSkipped = 0;
        int totalChunks = 0;

        for (JsonNode articleNode : articles) {
            String articleId = articleNode.path("article_id").asText(null);
            if (articleId == null || articleId.isBlank()) {
                log.warn("Skipping article with missing article_id");
                continue;
            }

            // Skip already-ingested articles
            if (existingIds.contains(articleId)) {
                articlesSkipped++;
                continue;
            }

            try {
                int chunksCreated = processArticle(articleNode, ucMappings);
                totalChunks += chunksCreated;
                articlesProcessed++;

                if (articlesProcessed % 10 == 0) {
                    log.info("Progress: {} articles processed, {} chunks created", articlesProcessed, totalChunks);
                }
            } catch (Exception e) {
                log.error("Failed to process article {}: {}", articleId, e.getMessage(), e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== Knowledge Ingestion Complete ===");
        log.info("  Articles processed: {}", articlesProcessed);
        log.info("  Articles skipped (already in DB): {}", articlesSkipped);
        log.info("  Total chunks created: {}", totalChunks);
        log.info("  Total time: {}ms ({}s)", elapsed, elapsed / 1000.0);
    }

    /**
     * Process a single article: save article, chunk text, embed, save chunks.
     */
    private int processArticle(JsonNode articleNode, Map<String, String[]> ucMappings) {
        String articleId = articleNode.path("article_id").asText();
        String title = articleNode.path("title").asText("");
        String summary = articleNode.path("summary").isNull() ? null : articleNode.path("summary").asText();
        String contentPlain = articleNode.path("content_plain").asText("");
        String sourceUrl = articleNode.path("source_url").isNull() ? null : articleNode.path("source_url").asText();
        String urlCategory = articleNode.path("url_category").isNull() ? null : articleNode.path("url_category").asText();
        boolean published = articleNode.path("published_status").asBoolean(true);
        int tokenEstimate = articleNode.path("token_estimate").asInt(0);

        // UC tags: prefer JSON field, fall back to CSV mapping
        String[] ucTags = extractUcTags(articleNode);
        if ((ucTags == null || ucTags.length == 0) && ucMappings.containsKey(articleId)) {
            ucTags = ucMappings.get(articleId);
        }

        // Step 1: Save article
        OffsetDateTime now = OffsetDateTime.now();
        KbArticle article = KbArticle.builder()
                .articleId(articleId)
                .title(title)
                .summary(summary)
                .description(contentPlain)
                .sourceUrl(sourceUrl)
                .urlCategory(urlCategory)
                .ucTags(ucTags)
                .isPublished(published)
                .tokenCount(tokenEstimate)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        kbArticleRepository.save(article);

        // Step 2: Clean HTML and chunk text
        String cleanedText = chunkingService.cleanHtml(contentPlain);
        List<ChunkingService.ChunkRecord> chunks = chunkingService.chunk(title, cleanedText);

        if (chunks.isEmpty()) {
            log.debug("No chunks generated for article '{}'", title);
            return 0;
        }

        // Step 3: Embed chunks in batches
        List<String> chunkTexts = chunks.stream()
                .map(ChunkingService.ChunkRecord::text)
                .collect(Collectors.toList());

        List<float[]> allEmbeddings = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, chunkTexts.size());
            List<String> batch = chunkTexts.subList(i, end);
            List<float[]> batchEmbeddings = embeddingClient.embedBatch(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }

        // Step 4: Save chunks
        List<KbChunk> kbChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkingService.ChunkRecord chunkRecord = chunks.get(i);
            float[] embedding = i < allEmbeddings.size() ? allEmbeddings.get(i) : new float[768];

            KbChunk kbChunk = KbChunk.builder()
                    .chunkId(articleId + "_chunk_" + chunkRecord.index())
                    .articleId(articleId)
                    .chunkIndex(chunkRecord.index())
                    .chunkText(chunkRecord.text())
                    .embedding(embedding)
                    .tokenCount(chunkingService.estimateTokens(chunkRecord.text()))
                    .sectionHeading(chunkRecord.sectionHeading())
                    .createdAt(now)
                    .build();
            kbChunks.add(kbChunk);
        }

        kbChunkRepository.saveAll(kbChunks);
        log.debug("Article '{}': {} chunks saved", title, kbChunks.size());

        return kbChunks.size();
    }

    /**
     * Extract UC tags from the JSON article node.
     */
    private String[] extractUcTags(JsonNode articleNode) {
        JsonNode ucTagsNode = articleNode.path("uc_tags");
        if (ucTagsNode.isMissingNode() || ucTagsNode.isNull()) {
            return new String[0];
        }
        if (ucTagsNode.isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode tag : ucTagsNode) {
                tags.add(tag.asText());
            }
            return tags.toArray(new String[0]);
        }
        return new String[0];
    }

    /**
     * Load UC mapping from CSV file.
     * CSV format: article_id,title,url_category,uc_tags,search_eligible,...
     * uc_tags column uses pipe '|' as delimiter for multiple tags.
     */
    private Map<String, String[]> loadUcMappings() throws IOException {
        Map<String, String[]> mappings = new HashMap<>();
        Path csvPath = resolveDataPath(UC_MAPPING_CSV_PATH);

        List<String> lines;
        if (csvPath != null && Files.exists(csvPath)) {
            lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        } else {
            // Try classpath
            InputStream is = getClass().getClassLoader().getResourceAsStream(UC_MAPPING_CSV_PATH);
            if (is == null) {
                log.warn("UC mapping CSV not found at {} or on classpath", UC_MAPPING_CSV_PATH);
                return mappings;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                lines = reader.lines().collect(Collectors.toList());
            }
        }

        // Skip header
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",", -1);
            if (parts.length < 4) continue;

            String articleId = parts[0].trim();
            String ucTagsStr = parts[3].trim();

            if (!ucTagsStr.isEmpty()) {
                String[] tags = ucTagsStr.split("\\|");
                for (int j = 0; j < tags.length; j++) {
                    tags[j] = tags[j].trim();
                }
                mappings.put(articleId, tags);
            }
        }

        return mappings;
    }

    /**
     * Load articles from the JSON file.
     */
    /**
     * Resolve a data file path, trying the current directory first, then the parent
     * directory (for when run from the server/ subdirectory).
     */
    private Path resolveDataPath(String relativePath) {
        Path direct = Path.of(relativePath);
        if (Files.exists(direct)) {
            return direct;
        }
        Path fromParent = Path.of("..").resolve(relativePath);
        if (Files.exists(fromParent)) {
            return fromParent;
        }
        return null;
    }

    private List<JsonNode> loadArticlesJson() throws IOException {
        Path jsonPath = resolveDataPath(ARTICLES_JSON_PATH);

        JsonNode root;
        if (jsonPath != null && Files.exists(jsonPath)) {
            root = objectMapper.readTree(jsonPath.toFile());
        } else {
            // Try classpath
            InputStream is = getClass().getClassLoader().getResourceAsStream(ARTICLES_JSON_PATH);
            if (is == null) {
                throw new IOException("Articles JSON not found at " + ARTICLES_JSON_PATH + " or on classpath");
            }
            root = objectMapper.readTree(is);
        }

        JsonNode articlesNode = root.path("articles");
        if (!articlesNode.isArray()) {
            throw new IOException("Expected 'articles' array in JSON");
        }

        List<JsonNode> articles = new ArrayList<>();
        for (JsonNode node : articlesNode) {
            articles.add(node);
        }
        return articles;
    }
}
