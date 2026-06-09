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
 * For each NEW article (insert path, --ingest or --reconcile):
 *   1. Save to kb_articles table
 *   2. Clean HTML, chunk text (ChunkingService)
 *   3. Embed each chunk via EmbeddingClient.embedBatch()
 *   4. Save chunks with embeddings to kb_chunks table
 *
 * <p>R8 (Sub-sprint S-Auto-28) — the runner also accepts {@code --reconcile},
 * a metadata-only data-application mode. Under {@code --reconcile}, an article
 * already present in {@code kb_articles} has ONLY its mutable curation columns
 * ({@code search_knowledge_eligible} / {@code is_published} / {@code uc_tags})
 * UPDATEd in place from the committed JSON source-of-truth — content columns
 * and the {@code kb_chunks} embedding pipeline are never touched. Plain
 * {@code --ingest} keeps its byte-for-byte insert-only skip-existing
 * semantics. The metadata reconcile exists because the insert-only loader can
 * never apply a curation-flag change (e.g. R6's
 * {@code search_knowledge_eligible: true -> false}) to a row that already
 * exists in a populated corpus.
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
        boolean ingest = args.containsOption("ingest") || args.getNonOptionArgs().contains("--ingest");
        boolean reconcile = args.containsOption("reconcile") || args.getNonOptionArgs().contains("--reconcile");
        if (!ingest && !reconcile) {
            log.debug("--ingest / --reconcile not specified, skipping knowledge ingestion");
            return;
        }

        // --reconcile is the metadata-only data-application mode; plain
        // --ingest is insert-only. The two flags name different modes, not the
        // same mode with a flag stack (--ingest --reconcile is non-canonical
        // and not pinned anywhere; at the parser level either flag opens the
        // gate). When --reconcile is present it wins the mode selection.
        String mode = reconcile ? "Reconcile" : "Ingestion";
        log.info("=== Knowledge {} Started ===", mode);
        long startTime = System.currentTimeMillis();

        // Load UC mapping from CSV
        Map<String, String[]> ucMappings = loadUcMappings();
        log.info("Loaded {} UC mappings from CSV", ucMappings.size());

        // Load articles from JSON
        List<JsonNode> articles = loadArticlesJson();
        log.info("Loaded {} articles from JSON", articles.size());

        // Index existing articles by id. The entity is kept in hand (not just
        // the id) so --reconcile can UPDATE the mutable curation columns in
        // place without a per-article findById round-trip; --ingest still only
        // needs presence (containsKey) to skip.
        Map<String, KbArticle> existingById = kbArticleRepository.findAll().stream()
                .collect(Collectors.toMap(KbArticle::getArticleId, a -> a, (a, b) -> a));
        log.info("Found {} existing articles in DB", existingById.size());

        IngestionStats stats = processArticles(articles, ucMappings, existingById, reconcile);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("=== Knowledge {} Complete ===", mode);
        if (reconcile) {
            log.info("  articlesReconciled={} articlesInsertedNew={} articlesUnchanged={}",
                    stats.articlesReconciled(), stats.articlesInsertedNew(), stats.articlesUnchanged());
        } else {
            log.info("  Articles processed: {}", stats.articlesInsertedNew());
            log.info("  Articles skipped (already in DB): {}", stats.articlesUnchanged());
        }
        log.info("  Total chunks created: {}", stats.totalChunks());
        log.info("  Total time: {}ms ({}s)", elapsed, elapsed / 1000.0);
    }

    /**
     * Shared per-article application loop for both {@code --ingest}
     * (insert-only) and {@code --reconcile} (metadata-only update of existing
     * rows + insert of new rows). Package-private so the insert / skip /
     * reconcile branches can be unit-tested with mocked repositories without
     * reading the on-disk corpus.
     *
     * <p>Branch semantics, first match wins per article:
     * <ul>
     *   <li>existing row + {@code reconcile} → {@link #reconcileExisting}
     *       (curation-only UPDATE; counts reconciled vs unchanged);</li>
     *   <li>existing row + plain ingest → skip (counted unchanged; no write,
     *       no chunk/embed — byte-for-byte unchanged insert-only behaviour);</li>
     *   <li>new row (either mode) → {@link #processArticle} insert path
     *       (chunk + embed + save).</li>
     * </ul>
     */
    IngestionStats processArticles(List<JsonNode> articles,
                                   Map<String, String[]> ucMappings,
                                   Map<String, KbArticle> existingById,
                                   boolean reconcile) {
        int articlesReconciled = 0;
        int articlesInsertedNew = 0;
        int articlesUnchanged = 0;
        int totalChunks = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (JsonNode articleNode : articles) {
            String articleId = articleNode.path("article_id").asText(null);
            if (articleId == null || articleId.isBlank()) {
                log.warn("Skipping article with missing article_id");
                continue;
            }

            KbArticle existing = existingById.get(articleId);
            if (existing != null) {
                if (reconcile) {
                    if (reconcileExisting(articleNode, ucMappings, existing, now)) {
                        articlesReconciled++;
                    } else {
                        articlesUnchanged++;
                    }
                } else {
                    // Plain --ingest: skip already-ingested articles, untouched.
                    articlesUnchanged++;
                }
                continue;
            }

            // New article: insert path (chunk + embed + save) under BOTH modes.
            try {
                int chunksCreated = processArticle(articleNode, ucMappings);
                totalChunks += chunksCreated;
                articlesInsertedNew++;

                if (articlesInsertedNew % 10 == 0) {
                    log.info("Progress: {} articles inserted, {} chunks created",
                            articlesInsertedNew, totalChunks);
                }
            } catch (Exception e) {
                log.error("Failed to process article {}: {}", articleId, e.getMessage(), e);
            }
        }

        return new IngestionStats(articlesReconciled, articlesInsertedNew, articlesUnchanged, totalChunks);
    }

    /**
     * R8 (Sub-sprint S-Auto-28) — reconcile an already-present article's
     * MUTABLE CURATION columns to the committed JSON source-of-truth, in place.
     *
     * <p>Sets ONLY {@code search_knowledge_eligible} / {@code is_published} /
     * {@code uc_tags} from JSON, reusing the {@link #buildKbArticleFromJson}
     * parse idioms ({@code path(...).asBoolean(true)} and
     * {@link #extractUcTagsStatic}). Content columns ({@code title} /
     * {@code summary} / {@code description} / {@code source_url} /
     * {@code url_category} / {@code token_count}) and the embedding pipeline
     * ({@code kb_chunks}) are NEVER touched — the existing managed entity is
     * loaded, the curation fields are mutated, and {@code save()} issues the
     * row UPDATE. No re-chunk, no re-embed (anti-误杀 #2 / #3).
     *
     * <p>The entity is saved (and {@code updatedAt} bumped) ONLY when at least
     * one curation column actually changed; an already-in-sync row issues no
     * write and is counted "unchanged". That is what makes the run summary
     * report {@code articlesReconciled} as the count of rows whose curation
     * actually moved (e.g. exactly the 2 templates R6 flipped).
     *
     * @return {@code true} iff a curation column changed (row was UPDATEd).
     */
    boolean reconcileExisting(JsonNode articleNode,
                              Map<String, String[]> ucMappings,
                              KbArticle existing,
                              OffsetDateTime now) {
        List<String> changes = new ArrayList<>();

        // search_knowledge_eligible — primitive boolean; absent/null defaults
        // true (the published_status idiom reused from buildKbArticleFromJson).
        boolean desiredEligible = articleNode.path("search_knowledge_eligible").asBoolean(true);
        if (existing.isSearchKnowledgeEligible() != desiredEligible) {
            changes.add("search_knowledge_eligible " + existing.isSearchKnowledgeEligible()
                    + " -> " + desiredEligible);
            existing.setSearchKnowledgeEligible(desiredEligible);
        }

        // is_published — JSON key is `published_status`; absent/null defaults true.
        boolean desiredPublished = articleNode.path("published_status").asBoolean(true);
        if (!Boolean.valueOf(desiredPublished).equals(existing.getIsPublished())) {
            changes.add("is_published " + existing.getIsPublished() + " -> " + desiredPublished);
            existing.setIsPublished(desiredPublished);
        }

        // uc_tags — mirror the insert-path derivation EXACTLY (JSON array
        // first, CSV mapping fallback) so reconcile never clobbers CSV-derived
        // tags on an article whose JSON omits the field.
        String[] desiredUcTags = extractUcTagsStatic(articleNode);
        if ((desiredUcTags == null || desiredUcTags.length == 0)
                && ucMappings != null && ucMappings.containsKey(existing.getArticleId())) {
            desiredUcTags = ucMappings.get(existing.getArticleId());
        }
        if (!Arrays.equals(existing.getUcTags(), desiredUcTags)) {
            changes.add("uc_tags " + Arrays.toString(existing.getUcTags())
                    + " -> " + Arrays.toString(desiredUcTags));
            existing.setUcTags(desiredUcTags);
        }

        if (changes.isEmpty()) {
            return false;
        }

        existing.setUpdatedAt(now);
        kbArticleRepository.save(existing);
        log.info("Knowledge reconcile: article_id={} curation updated: {}",
                existing.getArticleId(), String.join("; ", changes));
        return true;
    }

    /**
     * Per-run application counts. {@code articlesReconciled} is the number of
     * existing rows whose curation columns actually changed (reconcile mode);
     * {@code articlesInsertedNew} the new-article inserts (either mode);
     * {@code articlesUnchanged} existing rows skipped (ingest mode) or already
     * in sync (reconcile mode).
     */
    record IngestionStats(int articlesReconciled,
                          int articlesInsertedNew,
                          int articlesUnchanged,
                          int totalChunks) {
    }

    /**
     * Process a single article: save article, chunk text, embed, save chunks.
     */
    private int processArticle(JsonNode articleNode, Map<String, String[]> ucMappings) {
        OffsetDateTime now = OffsetDateTime.now();
        KbArticle article = buildKbArticleFromJson(articleNode, ucMappings, now);

        String articleId = article.getArticleId();
        String title = article.getTitle();
        String contentPlain = article.getDescription();

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
     * Sprint 14 §L0 — package-private helper that maps a single article
     * JSON node into a {@link KbArticle}. Extracted from
     * {@link #processArticle} so the FAQ → KbArticle field-preservation
     * contract (most importantly {@code source_url} / Help_Site_URL and
     * {@code is_published}) can be unit-tested without standing up the
     * embedding client / chunker / repository wiring.
     */
    static KbArticle buildKbArticleFromJson(JsonNode articleNode,
                                             Map<String, String[]> ucMappings,
                                             OffsetDateTime now) {
        String articleId = articleNode.path("article_id").asText();
        String title = articleNode.path("title").asText("");
        String summary = articleNode.path("summary").isNull() ? null : articleNode.path("summary").asText();
        String contentPlain = articleNode.path("content_plain").asText("");
        String sourceUrl = articleNode.path("source_url").isNull() ? null : articleNode.path("source_url").asText();
        String urlCategory = articleNode.path("url_category").isNull() ? null : articleNode.path("url_category").asText();
        // `published_status` defaults to TRUE so curated CSV exports without
        // a published flag still ingest as published; if the JSON sets it
        // explicitly to false we must respect that and NOT silently coerce.
        boolean published = articleNode.path("published_status").asBoolean(true);
        // R6 (Sub-sprint C-2b) — corpus-curation flag. Mirrors the
        // `published_status` idiom: absent / null defaults to TRUE so
        // articles that never carried the field stay visible on the
        // search surface; an explicit false hides them from search only.
        boolean searchKnowledgeEligible =
                articleNode.path("search_knowledge_eligible").asBoolean(true);
        int tokenEstimate = articleNode.path("token_estimate").asInt(0);

        // UC tags: prefer JSON field, fall back to CSV mapping
        String[] ucTags = extractUcTagsStatic(articleNode);
        if ((ucTags == null || ucTags.length == 0) && ucMappings != null
                && ucMappings.containsKey(articleId)) {
            ucTags = ucMappings.get(articleId);
        }

        return KbArticle.builder()
                .articleId(articleId)
                .title(title)
                .summary(summary)
                .description(contentPlain)
                .sourceUrl(sourceUrl)
                .urlCategory(urlCategory)
                .ucTags(ucTags)
                .isPublished(published)
                .searchKnowledgeEligible(searchKnowledgeEligible)
                .tokenCount(tokenEstimate)
                .version(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static String[] extractUcTagsStatic(JsonNode articleNode) {
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
