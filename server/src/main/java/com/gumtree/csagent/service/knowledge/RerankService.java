package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.model.ChatMessage;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    private final LlmClient llmClient;

    public RerankService(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Rerank a list of candidate chunks by LLM relevance scoring.
     *
     * @param query      the user query
     * @param candidates list of candidates with chunk text and metadata
     * @return sorted list with scores, highest first
     */
    public List<ScoredCandidate> rerank(String query, List<RerankCandidate> candidates) {
        List<ScoredCandidate> scored = new ArrayList<>();

        for (RerankCandidate candidate : candidates) {
            double score = scoreCandidate(query, candidate.chunkText());
            scored.add(new ScoredCandidate(
                    candidate.articleId(),
                    candidate.chunkId(),
                    candidate.chunkText(),
                    candidate.sectionHeading(),
                    candidate.cosineSimilarity(),
                    score
            ));
        }

        // Sort by rerank score descending
        scored.sort(Comparator.comparingDouble(ScoredCandidate::rerankScore).reversed());

        log.debug("Reranked {} candidates for query '{}'. Top score: {}",
                candidates.size(), query,
                scored.isEmpty() ? "N/A" : scored.get(0).rerankScore());

        return scored;
    }

    /**
     * Score a single chunk against the query using LLM.
     */
    private double scoreCandidate(String query, String chunkText) {
        String userMessage = String.format(
                "User question: %s\n\nText passage: %s\n\nScore (1-5):",
                query, chunkText
        );

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
            return parseScore(response.getContent());

        } catch (Exception e) {
            log.warn("Failed to score chunk for reranking: {}", e.getMessage());
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
