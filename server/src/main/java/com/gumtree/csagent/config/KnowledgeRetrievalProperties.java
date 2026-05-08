package com.gumtree.csagent.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sprint 15 §M2 — externalized retrieval / answer-gate / rerank
 * thresholds. Defaults match the previously hardcoded constants in
 * {@code KnowledgeSearchService} and {@code RerankService} exactly:
 *
 * <pre>
 *   KnowledgeSearchService.ANN_LIMIT                = 20
 *   KnowledgeSearchService.RETRIEVAL_GATE_THRESHOLD = 0.3
 *   KnowledgeSearchService.ANSWER_GATE_THRESHOLD    = 3.5
 *   KnowledgeSearchService.RERANK_CANDIDATES        = 8
 *   KnowledgeSearchService.TOP_RESULTS              = 3
 *   RerankService neutral fallback score            = 2.5
 * </pre>
 *
 * <p>Sprint 15 §M2 does NOT tune thresholds, change FAQ answerability
 * semantics, or change corpus content. Future threshold changes
 * become auditable config changes rather than hidden Java edits.
 *
 * <p>Validation rules (fail fast on out-of-range values):
 * <ul>
 *   <li>{@code annLimit} ≥ {@code rerankCandidates} ≥ {@code topResults} ≥ 1
 *   <li>{@code retrievalGateThreshold} ∈ [0.0, 1.0] (cosine similarity range)
 *   <li>{@code answerGateThreshold} ∈ [1.0, 5.0] (rerank LLM 1–5 scale)
 *   <li>{@code rerankFallbackScore} ∈ [1.0, 5.0]
 *   <li>{@code rerankFallbackScore} &lt; {@code answerGateThreshold} so a
 *       fallback parse cannot itself satisfy the answer gate.
 * </ul>
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "knowledge.retrieval")
public class KnowledgeRetrievalProperties {

    /** Max ANN candidates fetched from pgvector. Default: 20. */
    private int annLimit = 20;

    /** Cosine-similarity floor on the top ANN hit; below = retrieval_miss. */
    private double retrievalGateThreshold = 0.3;

    /** Rerank-score floor on the top reranked candidate; below = answer_miss. */
    private double answerGateThreshold = 3.5;

    /** Number of post-dedup candidates submitted to the rerank LLM. */
    private int rerankCandidates = 8;

    /** Number of hits returned to callers after answer gate. */
    private int topResults = 3;

    /**
     * Neutral fallback rerank score used when the LLM call fails or
     * the LLM response cannot be parsed as an integer 1–5. Strictly
     * less than {@link #answerGateThreshold} so a parse-failure
     * sequence cannot pass the answer gate by accident.
     */
    private double rerankFallbackScore = 2.5;

    @PostConstruct
    public void validate() {
        if (annLimit < 1) {
            throw new IllegalStateException("knowledge.retrieval.ann-limit must be >= 1, got " + annLimit);
        }
        if (rerankCandidates < 1) {
            throw new IllegalStateException(
                    "knowledge.retrieval.rerank-candidates must be >= 1, got " + rerankCandidates);
        }
        if (topResults < 1) {
            throw new IllegalStateException(
                    "knowledge.retrieval.top-results must be >= 1, got " + topResults);
        }
        if (annLimit < rerankCandidates) {
            throw new IllegalStateException(
                    "knowledge.retrieval.ann-limit (" + annLimit
                            + ") must be >= rerank-candidates (" + rerankCandidates + ")");
        }
        if (rerankCandidates < topResults) {
            throw new IllegalStateException(
                    "knowledge.retrieval.rerank-candidates (" + rerankCandidates
                            + ") must be >= top-results (" + topResults + ")");
        }
        if (Double.isNaN(retrievalGateThreshold)
                || retrievalGateThreshold < 0.0 || retrievalGateThreshold > 1.0) {
            throw new IllegalStateException(
                    "knowledge.retrieval.retrieval-gate-threshold must be in [0.0, 1.0], got "
                            + retrievalGateThreshold);
        }
        if (Double.isNaN(answerGateThreshold)
                || answerGateThreshold < 1.0 || answerGateThreshold > 5.0) {
            throw new IllegalStateException(
                    "knowledge.retrieval.answer-gate-threshold must be in [1.0, 5.0], got "
                            + answerGateThreshold);
        }
        if (Double.isNaN(rerankFallbackScore)
                || rerankFallbackScore < 1.0 || rerankFallbackScore > 5.0) {
            throw new IllegalStateException(
                    "knowledge.retrieval.rerank-fallback-score must be in [1.0, 5.0], got "
                            + rerankFallbackScore);
        }
        if (rerankFallbackScore >= answerGateThreshold) {
            throw new IllegalStateException(
                    "knowledge.retrieval.rerank-fallback-score (" + rerankFallbackScore
                            + ") must be < answer-gate-threshold (" + answerGateThreshold
                            + ") so a fallback score never satisfies the answer gate");
        }
        log.info("Knowledge retrieval config validated: ann-limit={}, retrieval-gate={}, "
                        + "rerank-candidates={}, top-results={}, answer-gate={}, fallback-score={}",
                annLimit, retrievalGateThreshold, rerankCandidates, topResults,
                answerGateThreshold, rerankFallbackScore);
    }
}
