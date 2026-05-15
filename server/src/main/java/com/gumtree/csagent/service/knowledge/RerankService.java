package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.config.KnowledgeRetrievalProperties;
import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.LlmClient;
import com.gumtree.csagent.service.observability.LlmCallLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reranks candidate chunks using LLM-based relevance scoring.
 * Each chunk is scored 1-5 for how well it answers the user query.
 *
 * <p>Sprint 15 §M2: the neutral fallback score returned on parse /
 * call failure is sourced from
 * {@link com.gumtree.csagent.config.KnowledgeRetrievalProperties}
 * (default 2.5, unchanged). All fallback paths log a structured
 * diagnostic so observability can attribute a missing answer to
 * rerank failure rather than a low-relevance match.
 */
@Slf4j
@Service
public class RerankService {

    private static final String SYSTEM_PROMPT = """
            You are a relevance scoring assistant. Your job is to rate how well a text passage \
            answers a user's question.

            Score from 1 to 5:
            5 = Passage perfectly and directly answers the question
            4 = Passage mostly answers the question with minor gaps
            3 = Passage is partially relevant but incomplete
            2 = Passage is tangentially related
            1 = Passage is completely irrelevant to the question

            Respond with ONLY a single integer (1, 2, 3, 4, or 5). No explanation needed.""";

    private static final Pattern SCORE_PATTERN = Pattern.compile("[1-5]");

    private static final ExecutorService RERANK_EXECUTOR = Executors.newFixedThreadPool(8);

    private final LlmClient llmClient;
    private final LlmCallLogger llmCallLogger;
    private final String modelName;
    private final KnowledgeRetrievalProperties retrievalProps;

    public RerankService(LlmClient llmClient, LlmCallLogger llmCallLogger,
                         LlmProperties llmProperties,
                         KnowledgeRetrievalProperties retrievalProps) {
        this.llmClient = llmClient;
        this.llmCallLogger = llmCallLogger;
        this.modelName = llmProperties.getKimi().getModel();
        this.retrievalProps = retrievalProps;
    }

    /**
     * Rerank a list of candidate chunks by LLM relevance scoring.
     * Scores all candidates in parallel using a fixed thread pool.
     *
     * @param query      the user query
     * @param candidates list of candidates with chunk text and metadata
     * @param sessionId  session identifier for LLM call logging (may be null)
     * @param turnIndex  turn index for LLM call logging
     * @return sorted list with scores, highest first
     */
    public List<ScoredCandidate> rerank(String query, List<RerankCandidate> candidates,
                                         String sessionId, int turnIndex) {
        // Submit all scoring tasks in parallel
        List<CompletableFuture<ScoredCandidate>> futures = candidates.stream()
                .map(candidate -> CompletableFuture.supplyAsync(
                        () -> {
                            double score = scoreCandidate(query, candidate.chunkText(),
                                    sessionId, turnIndex);
                            return new ScoredCandidate(
                                    candidate.articleId(),
                                    candidate.chunkId(),
                                    candidate.chunkText(),
                                    candidate.sectionHeading(),
                                    candidate.cosineSimilarity(),
                                    score
                            );
                        },
                        RERANK_EXECUTOR
                ).exceptionally(ex -> {
                    // Sprint 15 §M2 diagnostic: future-level failure drops to
                    // the configured fallback score so the rerank pipeline
                    // can keep running. The fallback score is configured to
                    // sit strictly below the answer gate so a fallback
                    // result never satisfies the gate by accident.
                    log.warn("Rerank future failed for chunk {}: {} (using fallback score {})",
                            candidate.chunkId(), ex.getMessage(),
                            retrievalProps.getRerankFallbackScore());
                    return new ScoredCandidate(
                            candidate.articleId(),
                            candidate.chunkId(),
                            candidate.chunkText(),
                            candidate.sectionHeading(),
                            candidate.cosineSimilarity(),
                            retrievalProps.getRerankFallbackScore()
                    );
                }))
                .toList();

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results
        List<ScoredCandidate> scored = futures.stream()
                .map(CompletableFuture::join)
                .sorted(Comparator.comparingDouble(ScoredCandidate::rerankScore).reversed())
                .toList();

        log.info("LLM [rerank] scored {} candidates for query '{}', top score: {}",
                candidates.size(), query,
                scored.isEmpty() ? "N/A" : scored.get(0).rerankScore());

        return scored;
    }

    /**
     * Score a single chunk against the query using LLM.
     */
    private double scoreCandidate(String query, String chunkText,
                                   String sessionId, int turnIndex) {
        String userMessage = String.format(
                "User question: %s\n\nText passage: %s\n\nScore (1-5):",
                query, chunkText
        );

        long start = System.currentTimeMillis();
        String requestSummary = query.length() > 200 ? query.substring(0, 200) : query;
        try {
            LlmRequest request = LlmRequest.builder()
                    .systemPrompt(SYSTEM_PROMPT)
                    .messages(List.of(
                            ChatMessage.builder()
                                    .role("user")
                                    .content(userMessage)
                                    .build()
                    ))
                    .temperature(0.0)
                    .maxTokens(8)
                    .build();

            LlmResponse response = llmClient.chat(request);
            int elapsed = (int) (System.currentTimeMillis() - start);

            llmCallLogger.log(sessionId, turnIndex, "rerank", modelName,
                    response.getPromptTokens(), response.getCompletionTokens(),
                    elapsed, requestSummary,
                    response.getContent() != null ? response.getContent().trim() : null);

            return parseScore(response.getContent());

        } catch (Exception e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            // Sprint 15 §M2 diagnostic: rerank LLM call failure → fallback score.
            log.warn("Rerank LLM call failed; reason=rerank_call_failure msg='{}' fallback_score={}",
                    e.getMessage(), retrievalProps.getRerankFallbackScore());
            llmCallLogger.logFailure(sessionId, turnIndex, "rerank", modelName,
                    elapsed, requestSummary, e.getMessage());
            return retrievalProps.getRerankFallbackScore();
        }
    }

    /**
     * Parse the integer score from LLM response. Falls back to the
     * configured rerank-fallback score if unparseable. Sprint 15 §M2
     * surfaces the fallback reason in logs for observability.
     */
    private double parseScore(String content) {
        if (content == null || content.isBlank()) {
            log.debug("Rerank parse fallback: reason=empty_response fallback_score={}",
                    retrievalProps.getRerankFallbackScore());
            return retrievalProps.getRerankFallbackScore();
        }

        Matcher matcher = SCORE_PATTERN.matcher(content.trim());
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }

        log.warn("Rerank parse fallback: reason=unparseable response='{}' fallback_score={}",
                content, retrievalProps.getRerankFallbackScore());
        return retrievalProps.getRerankFallbackScore();
    }

    /**
     * Input candidate for reranking.
     */
    public record RerankCandidate(
            String articleId,
            String chunkId,
            String chunkText,
            String sectionHeading,
            double cosineSimilarity
    ) {}

    /**
     * Scored candidate after reranking.
     */
    public record ScoredCandidate(
            String articleId,
            String chunkId,
            String chunkText,
            String sectionHeading,
            double cosineSimilarity,
            double rerankScore
    ) {}
}
