package com.gumtree.csagent.service.knowledge;

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

    public RerankService(LlmClient llmClient, LlmCallLogger llmCallLogger,
                         LlmProperties llmProperties) {
        this.llmClient = llmClient;
        this.llmCallLogger = llmCallLogger;
        this.modelName = llmProperties.getKimi().getModel();
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
                    log.warn("Rerank future failed for chunk {}: {}", candidate.chunkId(), ex.getMessage());
                    return new ScoredCandidate(
                            candidate.articleId(),
                            candidate.chunkId(),
                            candidate.chunkText(),
                            candidate.sectionHeading(),
                            candidate.cosineSimilarity(),
                            2.5
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
            log.warn("Failed to score chunk for reranking: {}", e.getMessage());
            llmCallLogger.logFailure(sessionId, turnIndex, "rerank", modelName,
                    elapsed, requestSummary, e.getMessage());
            // Return neutral score on failure
            return 2.5;
        }
    }

    /**
     * Parse the integer score from LLM response. Falls back to 2.5 if unparseable.
     */
    private double parseScore(String content) {
        if (content == null || content.isBlank()) {
            return 2.5;
        }

        Matcher matcher = SCORE_PATTERN.matcher(content.trim());
        if (matcher.find()) {
            return Double.parseDouble(matcher.group());
        }

        log.warn("Could not parse rerank score from LLM response: '{}'", content);
        return 2.5;
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
