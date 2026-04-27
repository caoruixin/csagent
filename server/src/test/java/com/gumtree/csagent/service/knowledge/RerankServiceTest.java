package com.gumtree.csagent.service.knowledge;

import com.gumtree.csagent.config.LlmProperties;
import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.LlmClient;
import com.gumtree.csagent.service.observability.LlmCallLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RerankService (D14.1 - Parallel Rerank).
 * Verifies parallel scoring, fault tolerance, result ordering, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class RerankServiceTest {

    @Mock
    private LlmClient llmClient;

    @Mock
    private LlmCallLogger llmCallLogger;

    private RerankService rerankService;

    @BeforeEach
    void setUp() {
        LlmProperties props = new LlmProperties();
        props.getKimi().setModel("test-model");
        rerankService = new RerankService(llmClient, llmCallLogger, props);
    }

    // --- C1: Parallel rerank returns results sorted by score ---

    @Test
    void rerank_multipleCandidates_shouldReturnSortedByScoreDescending() {
        // Given: three candidates, LLM returns different scores for each
        // We use content matching to return different scores per call
        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(buildLlmResponse("3"))
                .thenReturn(buildLlmResponse("5"))
                .thenReturn(buildLlmResponse("1"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "How to reset password", "Security", 0.8),
                new RerankService.RerankCandidate("art-2", "chunk-2", "Password reset via email", "Account", 0.7),
                new RerankService.RerankCandidate("art-3", "chunk-3", "Unrelated billing info", "Billing", 0.6)
        );

        // When
        List<RerankService.ScoredCandidate> results = rerankService.rerank("reset password", candidates, "sess-1", 1);

        // Then: results should be sorted descending by score
        assertEquals(3, results.size());
        assertTrue(results.get(0).rerankScore() >= results.get(1).rerankScore(),
                "First result should have highest or equal score");
        assertTrue(results.get(1).rerankScore() >= results.get(2).rerankScore(),
                "Results should be sorted descending by rerankScore");
    }

    @Test
    void rerank_singleCandidate_shouldReturnOneResult() {
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(buildLlmResponse("4"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "How to reset password", "Security", 0.9)
        );

        List<RerankService.ScoredCandidate> results = rerankService.rerank("reset password", candidates, "sess-1", 1);

        assertEquals(1, results.size());
        assertEquals(4.0, results.get(0).rerankScore());
        assertEquals("art-1", results.get(0).articleId());
        assertEquals("chunk-1", results.get(0).chunkId());
    }

    @Test
    void rerank_emptyList_shouldReturnEmptyList() {
        List<RerankService.ScoredCandidate> results = rerankService.rerank("test query", List.of(), "sess-1", 1);

        assertEquals(0, results.size());
        verify(llmClient, never()).chat(any());
    }

    // --- C1: Failing candidate doesn't block others ---

    @Test
    void rerank_oneFailingCandidate_shouldNotBlockOthers() {
        // Given: first call succeeds with score 5, second throws exception, third succeeds with score 4
        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(buildLlmResponse("5"))
                .thenThrow(new RuntimeException("LLM timeout"))
                .thenReturn(buildLlmResponse("4"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "Text one", "Section1", 0.8),
                new RerankService.RerankCandidate("art-2", "chunk-2", "Text two", "Section2", 0.7),
                new RerankService.RerankCandidate("art-3", "chunk-3", "Text three", "Section3", 0.6)
        );

        // When
        List<RerankService.ScoredCandidate> results = rerankService.rerank("test query", candidates, "sess-1", 1);

        // Then: all 3 candidates should have results (failed one gets fallback score 2.5)
        assertEquals(3, results.size());

        // Verify the failed candidate got the fallback score of 2.5
        boolean hasFailbackScore = results.stream().anyMatch(sc -> sc.rerankScore() == 2.5);
        assertTrue(hasFailbackScore, "Failed candidate should receive fallback score of 2.5");
    }

    @Test
    void rerank_allCandidatesFail_shouldReturnAllWithFallbackScores() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("LLM down"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "Text one", "Section1", 0.8),
                new RerankService.RerankCandidate("art-2", "chunk-2", "Text two", "Section2", 0.7)
        );

        List<RerankService.ScoredCandidate> results = rerankService.rerank("test query", candidates, "sess-1", 1);

        assertEquals(2, results.size());
        // All should have the fallback score of 2.5
        for (RerankService.ScoredCandidate sc : results) {
            assertEquals(2.5, sc.rerankScore(), "All failed candidates should get fallback score 2.5");
        }
    }

    // --- C1: Results collected correctly from parallel execution ---

    @Test
    void rerank_shouldPreserveAllCandidateMetadata() {
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(buildLlmResponse("4"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-42", "chunk-99", "Specific chunk text", "My Section", 0.75)
        );

        List<RerankService.ScoredCandidate> results = rerankService.rerank("query", candidates, "sess-1", 1);

        assertEquals(1, results.size());
        RerankService.ScoredCandidate sc = results.get(0);
        assertEquals("art-42", sc.articleId());
        assertEquals("chunk-99", sc.chunkId());
        assertEquals("Specific chunk text", sc.chunkText());
        assertEquals("My Section", sc.sectionHeading());
        assertEquals(0.75, sc.cosineSimilarity());
        assertEquals(4.0, sc.rerankScore());
    }

    @Test
    void rerank_shouldLogCalls_viaLlmCallLogger() {
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(buildLlmResponse("4"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "text", "section", 0.8)
        );

        rerankService.rerank("test", candidates, "sess-log", 3);

        verify(llmCallLogger, atLeastOnce()).log(
                eq("sess-log"), eq(3), eq("rerank"), eq("test-model"),
                anyInt(), anyInt(), anyInt(), anyString(), anyString());
    }

    @Test
    void rerank_failedCandidate_shouldLogFailure() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("Timeout"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "text", "section", 0.8)
        );

        rerankService.rerank("test", candidates, "sess-fail", 2);

        verify(llmCallLogger, atLeastOnce()).logFailure(
                eq("sess-fail"), eq(2), eq("rerank"), eq("test-model"),
                anyInt(), anyString(), anyString());
    }

    @Test
    void rerank_nullSessionId_shouldNotThrow() {
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(buildLlmResponse("3"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "text", "section", 0.8)
        );

        // Should not throw when sessionId is null (REST API path)
        assertDoesNotThrow(() -> rerankService.rerank("query", candidates, null, 0));
    }

    // --- Score parsing edge cases ---

    @Test
    void rerank_llmReturnsNonNumeric_shouldUseFallbackScore() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(buildLlmResponse("This passage is somewhat relevant"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "text", "section", 0.8)
        );

        List<RerankService.ScoredCandidate> results = rerankService.rerank("query", candidates, "sess-1", 1);

        assertEquals(1, results.size());
        assertEquals(2.5, results.get(0).rerankScore(), "Non-parseable score should fallback to 2.5");
    }

    @Test
    void rerank_llmReturnsEmptyContent_shouldUseFallbackScore() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(buildLlmResponse(""));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "text", "section", 0.8)
        );

        List<RerankService.ScoredCandidate> results = rerankService.rerank("query", candidates, "sess-1", 1);

        assertEquals(1, results.size());
        assertEquals(2.5, results.get(0).rerankScore(), "Empty content should fallback to 2.5");
    }

    @Test
    void rerank_llmReturnsScoreWithText_shouldExtractScore() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenReturn(buildLlmResponse("Score: 4"));

        List<RerankService.RerankCandidate> candidates = List.of(
                new RerankService.RerankCandidate("art-1", "chunk-1", "text", "section", 0.8)
        );

        List<RerankService.ScoredCandidate> results = rerankService.rerank("query", candidates, "sess-1", 1);

        assertEquals(1, results.size());
        assertEquals(4.0, results.get(0).rerankScore(), "Should extract the score from text with number");
    }

    private LlmResponse buildLlmResponse(String content) {
        return LlmResponse.builder()
                .content(content)
                .finishReason("stop")
                .promptTokens(50)
                .completionTokens(5)
                .latencyMs(100)
                .build();
    }
}
