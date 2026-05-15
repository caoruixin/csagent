package com.gumtree.csagent.service.runtime;

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

    @Mock
    private LlmCallLogger llmCallLogger;

    private LlmInvocationService service;

    @BeforeEach
    void setUp() {
        LlmProperties llmProperties = new LlmProperties();
        llmProperties.getKimi().setModel("test-model");
        service = new LlmInvocationService(llmClient, llmCallLogger, llmProperties);
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

        LlmResponse result = service.invokeChat("{}", "hello", "sess-1", 1);

        assertNotNull(result);
        assertEquals("stop", result.getFinishReason());
        assertTrue(result.getContent().contains("ask_user"));
        verify(llmClient, times(1)).chat(any());
    }

    @Test
    void invokeChat_whenLlmThrowsTransportRuntimeException_throwsLlmUnavailable() {
        // Sprint 8.1 §M2: transport-class failures (the message itself
        // mentions "connection") classify as llm_connect_failed and now
        // propagate as LlmUnavailableException instead of returning a
        // synthetic SAFE_ESCALATION_RESPONSE — that prevents K0 from
        // mistaking the synthetic shape for real LLM evidence.
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("connection refused"));

        com.gumtree.csagent.service.llm.LlmUnavailableException ex =
                assertThrows(com.gumtree.csagent.service.llm.LlmUnavailableException.class,
                        () -> service.invokeChat("{}", "hello", "sess-1", 1));
        assertNotNull(ex.getFailureClass(), "failure_class telemetry tag must be populated");
    }

    @Test
    void invokeChat_whenLlmThrowsConnectException_throwsLlmUnavailable() {
        // Sprint 8.1 §M2: a wrapped ConnectException is an infra failure
        // and must propagate as LlmUnavailableException.
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("LLM API call failed",
                        new java.net.ConnectException("Connection refused")));

        assertThrows(com.gumtree.csagent.service.llm.LlmUnavailableException.class,
                () -> service.invokeChat("{}", "hello", "sess-1", 1));
    }

    @Test
    void invokeChat_whenLlmThrowsDeadlineExceededException_propagates() {
        // Sprint 8.1 §M2: deadline exhaustion MUST surface honestly —
        // never converted to a synthetic SAFE_ESCALATION_RESPONSE.
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new com.gumtree.csagent.service.llm.LlmDeadlineExceededException(
                        "deadline exceeded"));

        assertThrows(com.gumtree.csagent.service.llm.LlmDeadlineExceededException.class,
                () -> service.invokeChat("{}", "hello", "sess-1", 1));
    }

    @Test
    void invokeChat_whenLlmThrowsNPE_returnsSafeEscalation() {
        // Sprint 8.1 §M2: non-infra exceptions (e.g. NPE from a real LLM
        // response with malformed shape) keep the legacy SAFE_ESCALATION
        // fallback so the loop has something to parse — but the synthetic
        // marker is detected by the K0 evidence gate so it never counts
        // as real reasoning.
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new NullPointerException("unexpected null"));

        LlmResponse result = service.invokeChat("{}", "hello", "sess-1", 1);

        assertNotNull(result);
        assertEquals("error_fallback", result.getFinishReason());
        assertEquals(LlmInvocationService.SYNTHETIC_SAFE_ESCALATION_FINISH_REASON,
                result.getFinishReason());
    }

    @Test
    void invokeRouting_whenLlmSucceeds_shouldReturnLlmResponse() {
        LlmResponse expected = LlmResponse.builder()
                .content("{\"use_case\":\"UC-A\",\"confidence\":0.85}")
                .finishReason("stop")
                .latencyMs(100)
                .build();

        when(llmClient.chat(any(LlmRequest.class))).thenReturn(expected);

        LlmResponse result = service.invokeRouting("UC-A: Test", "Ad Support", "my ad is broken", "sess-1");

        assertNotNull(result);
        assertEquals("stop", result.getFinishReason());
        assertTrue(result.getContent().contains("UC-A"));
    }

    @Test
    void invokeRouting_whenLlmFails_shouldReturnSafeEscalation() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        LlmResponse result = service.invokeRouting("UC-A: Test", "Ad Support", "broken ad", "sess-1");

        assertNotNull(result);
        assertEquals("error_fallback", result.getFinishReason());
        assertTrue(result.getContent().contains("request_handover"));
    }

    @Test
    void invokeRouting_whenTopicSubjectNull_shouldNotThrowNPE() {
        LlmResponse expected = LlmResponse.builder()
                .content("{\"use_case\":\"UC-A\",\"confidence\":0.5}")
                .finishReason("stop")
                .build();
        when(llmClient.chat(any(LlmRequest.class))).thenReturn(expected);

        // Should not throw NPE when topicSubject is null
        LlmResponse result = service.invokeRouting("UC-A: Test", null, null, "sess-1");
        assertNotNull(result);
    }

    @Test
    void invokeChat_safeEscalationResponse_shouldBeValidJson() {
        // Sprint 8.1 §M2: parse-class failures still return SAFE_ESCALATION
        // (NPE simulates a malformed real LLM response). The content must
        // remain valid JSON so the loop's parser does not crash.
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new NullPointerException("unexpected null"));

        LlmResponse result = service.invokeChat("{}", "hello", "sess-1", 1);

        assertDoesNotThrow(() -> {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getContent());
        }, "Safe escalation content must be valid JSON");
    }

    @Test
    void invokeChat_safeEscalationResponse_shouldHaveZeroLatency() {
        when(llmClient.chat(any(LlmRequest.class)))
                .thenThrow(new RuntimeException("error"));

        LlmResponse result = service.invokeChat("{}", "hello", "sess-1", 1);

        assertEquals(0, result.getLatencyMs(),
                "Safe escalation should have 0 latency (no real LLM call)");
    }
}
