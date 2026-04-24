package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.LlmRequest;
import com.gumtree.csagent.model.LlmResponse;
import com.gumtree.csagent.service.llm.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LlmInvocationService.
 * Validates graceful degradation when LLM client fails.
 */
@ExtendWith(MockitoExtension.class)
class LlmInvocationServiceTest {

    @Mock
    private LlmClient llmClient;

    private LlmInvocationService service;

    @BeforeEach
    void setUp() {
        service = new LlmInvocationService(llmClient);
        // Set prompt templates via reflection (normally loaded by @PostConstruct)
        ReflectionTestUtils.setField(service, "systemPromptTemplate", "You are a test system prompt.");
        ReflectionTestUtils.setField(service, "routingPromptTemplate",
                "Classify: {uc_candidates} topic={topic_subject} desc={description}");
    }

    @Test
    void invokeChat_whenLlmSucceeds_shouldReturnLlmResponse() {
        LlmResponse expected = LlmResponse.builder()
                .content("{\"action\":\"ask_user\",\"parameters\":{},\"user_message\":\"How can I help?\"}")
                .finishReason("stop")
                .latencyMs(150)
                .promptTokens(100)
                .completionTokens(50)
                .build();

        when(llmClient.chat(any(LlmRequest.class))).thenReturn(expected);

        LlmResponse result = service.invokeChat("{}", "hello");

        assertNotNull(result);
        assertEquals("stop", result.getFinishReason());
        assertTrue(result.getContent().contains("ask_user"));
        verify(llmClient, times(1)).chat(any());
    }

    @Test
    void invokeChat_whenLlmThrowsRuntimeException_shouldReturnSafeEscalation() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        LlmResponse result = service.invokeChat("{}", "hello");

        assertNotNull(result, "Should return safe escalation, not null");
        assertEquals("error_fallback", result.getFinishReason());
        assertTrue(result.getContent().contains("escalate_human"),
                "Safe escalation should contain escalate_human action");
        assertTrue(result.getContent().contains("technical issue"),
                "Safe escalation should mention technical issue");
    }

    @Test
    void invokeChat_whenLlmThrowsConnectException_shouldReturnSafeEscalation() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("LLM API call failed",
                        new java.net.ConnectException("Connection refused")));

        LlmResponse result = service.invokeChat("{}", "hello");

        assertNotNull(result);
        assertEquals("error_fallback", result.getFinishReason());
        assertTrue(result.getContent().contains("escalate_human"));
    }

    @Test
    void invokeChat_whenLlmThrowsNPE_shouldReturnSafeEscalation() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new NullPointerException("unexpected null"));

        LlmResponse result = service.invokeChat("{}", "hello");

        assertNotNull(result);
        assertEquals("error_fallback", result.getFinishReason());
    }

    @Test
    void invokeRouting_whenLlmSucceeds_shouldReturnLlmResponse() {
        LlmResponse expected = LlmResponse.builder()
                .content("{\"use_case\":\"UC-A\",\"confidence\":0.85}")
                .finishReason("stop")
                .latencyMs(100)
                .build();

        when(llmClient.chat(any(LlmRequest.class))).thenReturn(expected);

        LlmResponse result = service.invokeRouting("UC-A: Test", "Ad Support", "my ad is broken");

        assertNotNull(result);
        assertEquals("stop", result.getFinishReason());
        assertTrue(result.getContent().contains("UC-A"));
    }

    @Test
    void invokeRouting_whenLlmFails_shouldReturnSafeEscalation() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        LlmResponse result = service.invokeRouting("UC-A: Test", "Ad Support", "broken ad");

        assertNotNull(result);
        assertEquals("error_fallback", result.getFinishReason());
        assertTrue(result.getContent().contains("escalate_human"));
    }

    @Test
    void invokeRouting_whenTopicSubjectNull_shouldNotThrowNPE() {
        LlmResponse expected = LlmResponse.builder()
                .content("{\"use_case\":\"UC-A\",\"confidence\":0.5}")
                .finishReason("stop")
                .build();
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(expected);

        // Should not throw NPE when topicSubject is null
        LlmResponse result = service.invokeRouting("UC-A: Test", null, null);
        assertNotNull(result);
    }

    @Test
    void invokeChat_safeEscalationResponse_shouldBeValidJson() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("timeout"));

        LlmResponse result = service.invokeChat("{}", "hello");

        // Verify the safe escalation content is valid JSON
        assertDoesNotThrow(() -> {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getContent());
        }, "Safe escalation content must be valid JSON");
    }

    @Test
    void invokeChat_safeEscalationResponse_shouldHaveZeroLatency() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("error"));

        LlmResponse result = service.invokeChat("{}", "hello");

        assertEquals(0, result.getLatencyMs(),
                "Safe escalation should have 0 latency (no real LLM call)");
    }
}
