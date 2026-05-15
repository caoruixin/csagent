package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.config.KnowledgeRetrievalProperties;
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
 *
 * <p>Sprint 15 §M2 — the previously hardcoded thresholds (ANN limit,
 * retrieval gate, rerank candidate count, answer gate, top-results)
 * are now sourced from {@link com.gumtree.csagent.config.KnowledgeRetrievalProperties}.
 * Defaults are unchanged. Sprint 15 also adds attribution diagnostics
 * for retrieval_miss / answer_miss caused by threshold gates and for
 * top-rank rerank-fallback usage.
 *
 * <p>Sprint 14 §L0 — KB published-safety / canonical URL audit:
 * the pgvector ANN now joins {@code kb_articles} with an
 * {@code is_published = true} predicate
 * ({@code findNearestByEmbeddingPublishedOnly} /
 * {@code findNearestByEmbeddingWithUcTagsPublishedOnly}) so unpublished
 * articles never surface as a search hit. The hit-projection step also
 * filters out any article whose post-fetch {@code isPublished} is false (a
 * defense-in-depth check in case ANN returns stale data) and stamps a
 * {@code canonicalUrlMissing} flag on every hit so missing-URL cases are
 * observable downstream rather than silently coalesced into a {@code null}.
 */
@Slf4j
@Service
public class KnowledgeSearchService {

    private final EmbeddingClient embeddingClient;
    private final KbChunkRepository kbChunkRepository;
    private final KbArticleRepository kbArticleRepository;
    private final RerankService rerankService;
    private final KnowledgeRetrievalProperties retrievalProps;

    public KnowledgeSearchService(EmbeddingClient embeddingClient,
                                   KbChunkRepository kbChunkRepository,
                                   KbArticleRepository kbArticleRepository,
                                   RerankService rerankService,
                                   KnowledgeRetrievalProperties retrievalProps) {
        this.embeddingClient = embeddingClient;
        this.kbChunkRepository = kbChunkRepository;
        this.kbArticleRepository = kbArticleRepository;
        this.rerankService = rerankService;
        this.retrievalProps = retrievalProps;
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
        int annLimit = retrievalProps.getAnnLimit();
        double retrievalGate = retrievalProps.getRetrievalGateThreshold();
        double answerGate = retrievalProps.getAnswerGateThreshold();
        int rerankCandidatesLimit = retrievalProps.getRerankCandidates();
        int topResultsLimit = retrievalProps.getTopResults();
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

        // Step 2: pgvector ANN search. Sprint 14 §L0 — published-only filter
        // is enforced at the SQL layer so the rerank / answer-gate stages
        // never see an unpublished candidate.
        List<KbChunk> nearestChunks;
        if (ucTags != null && !ucTags.isEmpty()) {
            String[] ucTagsArray = ucTags.toArray(new String[0]);
            nearestChunks = kbChunkRepository.findNearestByEmbeddingWithUcTagsPublishedOnly(
                    embeddingStr, ucTagsArray, annLimit);
        } else {
            nearestChunks = kbChunkRepository.findNearestByEmbeddingPublishedOnly(
                    embeddingStr, annLimit);
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
        boolean retrievalMiss = topSimilarity < retrievalGate;

        if (retrievalMiss) {
            // Sprint 15 §M2 diagnostic: retrieval_miss caused by the
            // configurable retrieval gate. Threshold value is logged so a
            // trace reader can confirm config-vs-runtime alignment.
            log.info("Knowledge search: retrieval_miss reason=retrieval_gate "
                            + "top_cosine_sim={} threshold={}",
                    topSimilarity, retrievalGate);
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

        // Step 5: Take top configured rerank-candidates for reranking
        List<ChunkWithScore> dedupedList = new ArrayList<>(bestPerArticle.values());
        dedupedList.sort(Comparator.comparingDouble(ChunkWithScore::similarity).reversed());
        List<ChunkWithScore> toRerank = dedupedList.subList(0, Math.min(rerankCandidatesLimit, dedupedList.size()));

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
        boolean answerMiss = reranked.isEmpty() || reranked.get(0).rerankScore() < answerGate;
        boolean faqMiss = retrievalMiss || answerMiss;

        if (answerMiss) {
            // Sprint 15 §M2 diagnostic: answer_miss caused by the
            // configurable answer gate. The fallback-score field surfaces
            // when the top score equals the rerank fallback (parse / call
            // failure), so observers can attribute the miss to
            // rerank-fallback rather than a low-relevance match.
            double topScore = reranked.isEmpty() ? Double.NaN : reranked.get(0).rerankScore();
            boolean fallbackUsed = !reranked.isEmpty()
                    && Double.compare(topScore, retrievalProps.getRerankFallbackScore()) == 0;
            log.info("Knowledge search: answer_miss reason=answer_gate "
                            + "top_rerank_score={} threshold={} top_is_fallback_score={}",
                    reranked.isEmpty() ? "N/A" : Double.toString(topScore),
                    answerGate, fallbackUsed);
        }

        // Step 8: Build top configured-results hits
        // Fetch articles for metadata
        Set<String> articleIds = reranked.stream()
                .limit(topResultsLimit)
                .map(RerankService.ScoredCandidate::articleId)
                .collect(Collectors.toSet());
        Map<String, KbArticle> articleMap = kbArticleRepository.findAllById(articleIds)
                .stream()
                .collect(Collectors.toMap(KbArticle::getArticleId, a -> a));

        // Sprint 14 §L0 — defense-in-depth: even after the SQL-layer
        // published-only filter, drop any candidate whose post-fetch
        // KbArticle is unpublished (covers stale ANN cache / mocked-repo
        // tests). Stamp `canonical_url_missing=true` whenever the article
        // landed in the index without a Help_Site_URL so missing-URL gaps
        // are observable in trace evidence rather than silently null.
        List<KnowledgeHit> hits = reranked.stream()
                .limit(topResultsLimit)
                .map(sc -> {
                    KbArticle article = articleMap.get(sc.articleId());
                    if (article != null && Boolean.FALSE.equals(article.getIsPublished())) {
                        return null;
                    }
                    String canonicalUrl = article != null ? article.getSourceUrl() : null;
                    boolean canonicalUrlMissing = canonicalUrl == null || canonicalUrl.isBlank();
                    return KnowledgeHit.builder()
                            .sourceId(sc.articleId())
                            .title(article != null ? article.getTitle() : "")
                            .snippet(truncateSnippet(sc.chunkText(), 300))
                            .canonicalUrl(canonicalUrl)
                            .canonicalUrlMissing(canonicalUrlMissing)
                            .score(sc.rerankScore())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
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
