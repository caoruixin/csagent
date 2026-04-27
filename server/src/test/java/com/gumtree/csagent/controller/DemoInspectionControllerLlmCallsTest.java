package com.gumtree.csagent.controller;

import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.LlmCallLog;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.repository.LlmCallLogRepository;
import com.gumtree.csagent.repository.MockHandoverLogRepository;
import com.gumtree.csagent.service.mock.MockGumtreeApiService;
import com.gumtree.csagent.service.mock.MockSalesforceService;
import com.gumtree.csagent.service.observability.LocalEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DemoInspectionController.getSessionLlmCalls() (D14.3).
 * Verifies the /v1/demo/sessions/{id}/llm-calls endpoint returns correct data.
 */
@ExtendWith(MockitoExtension.class)
class DemoInspectionControllerLlmCallsTest {

    @Mock private MockSalesforceService salesforceService;
    @Mock private MockGumtreeApiService gumtreeApiService;
    @Mock private BotSessionRepository sessionRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private MockHandoverLogRepository handoverLogRepository;
    @Mock private LlmCallLogRepository llmCallLogRepository;
    @Mock private MockProperties mockProperties;
    @Mock private LocalEventStore localEventStore;

    private DemoInspectionController controller;

    @BeforeEach
    void setUp() {
        controller = new DemoInspectionController(
                salesforceService, gumtreeApiService,
                sessionRepository, eventRepository,
                handoverLogRepository, llmCallLogRepository,
                mockProperties, localEventStore);
    }

    @Test
    void getSessionLlmCalls_withData_shouldReturnLogs() {
        List<LlmCallLog> logs = List.of(
                LlmCallLog.builder()
                        .id(1L)
                        .sessionId("sess-1")
                        .turnIndex(1)
                        .callType("routing")
                        .model("test-model")
                        .promptTokens(80)
                        .completionTokens(30)
                        .latencyMs(150)
                        .success(true)
                        .createdAt(OffsetDateTime.now())
                        .build(),
                LlmCallLog.builder()
                        .id(2L)
                        .sessionId("sess-1")
                        .turnIndex(1)
                        .callType("chat")
                        .model("test-model")
                        .promptTokens(200)
                        .completionTokens(100)
                        .latencyMs(500)
                        .success(true)
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        when(llmCallLogRepository.findBySessionIdOrderByCreatedAt("sess-1")).thenReturn(logs);

        ResponseEntity<List<LlmCallLog>> response = controller.getSessionLlmCalls("sess-1");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("routing", response.getBody().get(0).getCallType());
        assertEquals("chat", response.getBody().get(1).getCallType());
    }

    @Test
    void getSessionLlmCalls_noData_shouldReturnEmptyList() {
        when(llmCallLogRepository.findBySessionIdOrderByCreatedAt("nonexistent")).thenReturn(Collections.emptyList());

        ResponseEntity<List<LlmCallLog>> response = controller.getSessionLlmCalls("nonexistent");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getSessionLlmCalls_withFailedCalls_shouldIncludeThem() {
        List<LlmCallLog> logs = List.of(
                LlmCallLog.builder()
                        .id(1L)
                        .sessionId("sess-fail")
                        .turnIndex(1)
                        .callType("rerank")
                        .model("test-model")
                        .promptTokens(0)
                        .completionTokens(0)
                        .latencyMs(1000)
                        .success(false)
                        .errorMessage("Connection timeout")
                        .createdAt(OffsetDateTime.now())
                        .build()
        );

        when(llmCallLogRepository.findBySessionIdOrderByCreatedAt("sess-fail")).thenReturn(logs);

        ResponseEntity<List<LlmCallLog>> response = controller.getSessionLlmCalls("sess-fail");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertFalse(response.getBody().get(0).getSuccess());
        assertEquals("Connection timeout", response.getBody().get(0).getErrorMessage());
    }

    @Test
    void getSessionLlmCalls_shouldQueryBySessionId() {
        when(llmCallLogRepository.findBySessionIdOrderByCreatedAt("sess-specific"))
                .thenReturn(Collections.emptyList());

        controller.getSessionLlmCalls("sess-specific");

        verify(llmCallLogRepository).findBySessionIdOrderByCreatedAt("sess-specific");
    }
}
