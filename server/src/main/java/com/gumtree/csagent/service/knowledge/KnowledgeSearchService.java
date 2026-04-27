package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.KbArticle;
import com.gumtree.csagent.model.KbChunk;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.model.KnowledgeSearchResult;
import com.gumtree.csagent.repository.KbArticleRepository;
import com.gumtree.csagent.repository.KbChunkRepository;
import com.gumtree.csagent.service.embedding.EmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core knowledge retrieval service.
 *
 * Pipeline:
 *   Query -> Embed(768-dim) -> pgvector ANN(LIMIT 20)
 *     -> Retrieval Gate (top1 cosine_sim < 0.3 -> retrieval_miss)
 *     -> Article dedup (highest scoring chunk per article)
 *     -> Rerank top 4-8 (via RerankService)
 *     -> Answer Gate (top rerank score < 3.5 -> answer_miss)
 *     -> Return top 3 { source_id, title, snippet, canonical_url, score }
 *     -> faq_miss = retrieval_miss OR answer_miss
 */
@Slf4j
@Service
public class KnowledgeSearchService {

    private static final int ANN_LIMIT = 20;
    private static final double RETRIEVAL_GATE_THRESHOLD = 0.3;
    private static final double ANSWER_GATE_THRESHOLD = 3.5;
    private static final int RERANK_CANDIDATES = 8;
    private static final int TOP_RESULTS = 3;

    private final EmbeddingClient embeddingClient;
    private final KbChunkRepository kbChunkRepository;
    private final KbArticleRepository kbArticleRepository;
    private final RerankService rerankService;

    public KnowledgeSearchService(EmbeddingClient embeddingClient,
                                   KbChunkRepository kbChunkRepository,
                                   KbArticleRepository kbArticleRepository,
                                   RerankService rerankService) {
        this.embeddingClient = embeddingClient;
        this.kbChunkRepository = kbChunkRepository;
        this.kbArticleRepository = kbArticleRepository;
        this.rerankService = rerankService;
    }

    /**
     * Search knowledge base for relevant articles matching the query.
     *
     * @param query     user's search query
     * @param ucTags    optional UC tags to filter results (may be null or empty)
     * @param sessionId session identifier for LLM call logging (may be null)
     * @param turnIndex turn index for LLM call logging
     * @return search result with hits and miss flags
     */
    public KnowledgeSearchResult search(String query, List<String> ucTags,
                                         String sessionId, int turnIndex) {
        long startTime = System.currentTimeMillis();
        log.info("Knowledge search: query='{}', ucTags={}", query, ucTags);

        // Step 1: Embed the query
        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingClient.embed(query);
        } catch (Exception e) {
            log.error("Knowledge search: embedding failed for query='{}': {}", query, e.getMessage(), e);
            return KnowledgeSearchResult.builder()
                    .hits(List.of())
                    .retrievalMiss(true)
                    .answerMiss(false)
                    .faqMiss(true)
                    .build();
        }
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            log.warn("Knowledge search: embedding returned null/empty for query='{}'", query);
            return KnowledgeSearchResult.builder()
                    .hits(List.of())
                    .retrievalMiss(true)
                    .answerMiss(false)
                    .faqMiss(true)
                    .build();
        }
        String embeddingStr = embeddingToString(queryEmbedding);

        // Step 2: pgvector ANN search
        List<KbChunk> nearestChunks;
        if (ucTags != null && !ucTags.isEmpty()) {
            String[] ucTagsArray = ucTags.toArray(new String[0]);
            nearestChunks = kbChunkRepository.findNearestByEmbeddingWithUcTags(embeddingStr, ucTagsArray, ANN_LIMIT);
        } else {
            nearestChunks = kbChunkRepository.findNearestByEmbedding(embeddingStr, ANN_LIMIT);
        }

        if (nearestChunks.isEmpty()) {
            log.info("Knowledge search: no chunks found, returning retrieval_miss");
            return KnowledgeSearchResult.builder()
                    .hits(List.of())
                    .retrievalMiss(true)
                    .answerMiss(false)
                    .faqMiss(true)
                    .build();
        }

        // Step 3: Calculate cosine similarities and check retrieval gate
        List<ChunkWithScore> scoredChunks = new ArrayList<>();
        for (KbChunk chunk : nearestChunks) {
            double similarity = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            scoredChunks.add(new ChunkWithScore(chunk, similarity));
        }
        scoredChunks.sort(Comparator.comparingDouble(ChunkWithScore::similarity).reversed());

        double topSimilarity = scoredChunks.get(0).similarity();
        boolean retrievalMiss = topSimilarity < RETRIEVAL_GATE_THRESHOLD;

        if (retrievalMiss) {
            log.info("Knowledge search: retrieval_miss (top cosine_sim={:.4f} < {})",
                    topSimilarity, RETRIEVAL_GATE_THRESHOLD);
            return KnowledgeSearchResult.builder()
                    .hits(List.of())
                    .retrievalMiss(true)
                    .answerMiss(false)
                    .faqMiss(true)
                    .build();
        }

        // Step 4: Article dedup - keep highest scoring chunk per article
        Map<String, ChunkWithScore> bestPerArticle = new LinkedHashMap<>();
        for (ChunkWithScore cs : scoredChunks) {
            String articleId = cs.chunk().getArticleId();
            bestPerArticle.merge(articleId, cs,
                    (existing, candidate) -> candidate.similarity() > existing.similarity() ? candidate : existing);
        }

        // Step 5: Take top RERANK_CANDIDATES for reranking
        List<ChunkWithScore> dedupedList = new ArrayList<>(bestPerArticle.values());
        dedupedList.sort(Comparator.comparingDouble(ChunkWithScore::similarity).reversed());
        List<ChunkWithScore> toRerank = dedupedList.subList(0, Math.min(RERANK_CANDIDATES, dedupedList.size()));

        // Build rerank candidates
        List<RerankService.RerankCandidate> rerankCandidates = toRerank.stream()
                .map(cs -> new RerankService.RerankCandidate(
                        cs.chunk().getArticleId(),
                        cs.chunk().getChunkId(),
                        cs.chunk().getChunkText(),
                        cs.chunk().getSectionHeading(),
                        cs.similarity()
                ))
                .collect(Collectors.toList());

        // Step 6: Rerank via LLM
        List<RerankService.ScoredCandidate> reranked = rerankService.rerank(query, rerankCandidates, sessionId, turnIndex);

        // Step 7: Answer gate
        boolean answerMiss = reranked.isEmpty() || reranked.get(0).rerankScore() < ANSWER_GATE_THRESHOLD;
        boolean faqMiss = retrievalMiss || answerMiss;

        if (answerMiss) {
            log.info("Knowledge search: answer_miss (top rerank_score={} < {})",
                    reranked.isEmpty() ? "N/A" : reranked.get(0).rerankScore(),
                    ANSWER_GATE_THRESHOLD);
        }

        // Step 8: Build top 3 hits
        // Fetch articles for metadata
        Set<String> articleIds = reranked.stream()
                .limit(TOP_RESULTS)
                .map(RerankService.ScoredCandidate::articleId)
                .collect(Collectors.toSet());
        Map<String, KbArticle> articleMap = kbArticleRepository.findAllById(articleIds)
                .stream()
                .collect(Collectors.toMap(KbArticle::getArticleId, a -> a));

        List<KnowledgeHit> hits = reranked.stream()
                .limit(TOP_RESULTS)
                .map(sc -> {
                    KbArticle article = articleMap.get(sc.articleId());
                    return KnowledgeHit.builder()
                            .sourceId(sc.articleId())
                            .title(article != null ? article.getTitle() : "")
                            .snippet(truncateSnippet(sc.chunkText(), 300))
                            .canonicalUrl(article != null ? article.getSourceUrl() : null)
                            .score(sc.rerankScore())
                            .build();
                })
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Knowledge search completed in {}ms: {} hits, faq_miss={}", elapsed, hits.size(), faqMiss);

        return KnowledgeSearchResult.builder()
                .hits(hits)
                .retrievalMiss(retrievalMiss)
                .answerMiss(answerMiss)
                .faqMiss(faqMiss)
                .build();
    }

    /**
     * Convert float[] embedding to pgvector string format "[0.1,0.2,...]".
     */
    private String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Calculate cosine similarity between two vectors.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 0.0 : dotProduct / denom;
    }

    /**
     * Truncate text to maxLen characters, breaking at word boundary.
     */
    private String truncateSnippet(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        int breakAt = text.lastIndexOf(' ', maxLen);
        if (breakAt <= 0) {
            breakAt = maxLen;
        }
        return text.substring(0, breakAt) + "...";
    }

    private record ChunkWithScore(KbChunk chunk, double similarity) {}
}
