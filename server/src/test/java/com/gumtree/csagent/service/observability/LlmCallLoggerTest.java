package com.gumtree.csagent.service.observability;

import com.gumtree.csagent.model.LlmCallLog;
import com.gumtree.csagent.repository.LlmCallLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LlmCallLogger (D14.3 - LLM Call Log).
 * Verifies logging persistence, exception isolation, and field mapping.
 */
@ExtendWith(MockitoExtension.class)
class LlmCallLoggerTest {

    @Mock
    private LlmCallLogRepository repository;

    private LlmCallLogger logger;

    @BeforeEach
    void setUp() {
        logger = new LlmCallLogger(repository);
    }

    // --- C3: log() persists an LlmCallLog entity ---

    @Test
    void log_shouldPersistEntity() {
        logger.log("sess-1", 2, "chat", "test-model",
                100, 50, 250, "Hello user", "Hello response");

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository, times(1)).save(captor.capture());

        LlmCallLog saved = captor.getValue();
        assertEquals("sess-1", saved.getSessionId());
        assertEquals(2, saved.getTurnIndex());
        assertEquals("chat", saved.getCallType());
        assertEquals("test-model", saved.getModel());
        assertEquals(100, saved.getPromptTokens());
        assertEquals(50, saved.getCompletionTokens());
        assertEquals(250, saved.getLatencyMs());
        assertEquals("Hello user", saved.getRequestSummary());
        assertEquals("Hello response", saved.getResponseSummary());
        assertTrue(saved.getSuccess(), "Successful call should have success=true");
        assertNull(saved.getErrorMessage(), "Successful call should have no error message");
        assertNotNull(saved.getCreatedAt(), "CreatedAt should be set");
    }

    @Test
    void log_withNullTurnIndex_shouldPersistWithNullTurnIndex() {
        // Routing calls pass null turnIndex
        logger.log("sess-1", null, "routing", "test-model",
                80, 30, 150, "topic=Ad Support", "{\"use_case\":\"UC-A\"}");

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getTurnIndex(), "Routing calls should have null turnIndex");
        assertEquals("routing", captor.getValue().getCallType());
    }

    @Test
    void log_withNullSummaries_shouldPersistWithNulls() {
        logger.log("sess-1", 1, "rerank", "test-model",
                0, 0, 80, null, null);

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getRequestSummary());
        assertNull(captor.getValue().getResponseSummary());
    }

    @Test
    void log_shouldTruncateLongRequestSummary() {
        String longRequest = "A".repeat(600);

        logger.log("sess-1", 1, "chat", "test-model",
                100, 50, 200, longRequest, "short");

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository).save(captor.capture());

        assertEquals(500, captor.getValue().getRequestSummary().length(),
                "Request summary should be truncated to 500 chars");
    }

    @Test
    void log_shouldTruncateLongResponseSummary() {
        String longResponse = "B".repeat(600);

        logger.log("sess-1", 1, "chat", "test-model",
                100, 50, 200, "short", longResponse);

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository).save(captor.capture());

        assertEquals(500, captor.getValue().getResponseSummary().length(),
                "Response summary should be truncated to 500 chars");
    }

    // --- C3: log() swallows exceptions ---

    @Test
    void log_whenRepositoryThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("Database connection refused"))
                .when(repository).save(any(LlmCallLog.class));

        // Should NOT throw
        assertDoesNotThrow(() -> logger.log("sess-1", 1, "chat", "test-model",
                100, 50, 200, "request", "response"),
                "Logging failure must NEVER propagate");
    }

    @Test
    void log_whenRepositoryThrowsNPE_shouldNotPropagate() {
        doThrow(new NullPointerException("unexpected null"))
                .when(repository).save(any(LlmCallLog.class));

        assertDoesNotThrow(() -> logger.log("sess-1", 1, "chat", "test-model",
                100, 50, 200, "request", "response"),
                "Even NPE during logging must not propagate");
    }

    // --- logFailure() ---

    @Test
    void logFailure_shouldPersistWithSuccessFalse() {
        logger.logFailure("sess-2", 3, "rerank", "test-model",
                500, "test query", "Connection timeout");

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository).save(captor.capture());

        LlmCallLog saved = captor.getValue();
        assertEquals("sess-2", saved.getSessionId());
        assertEquals(3, saved.getTurnIndex());
        assertEquals("rerank", saved.getCallType());
        assertFalse(saved.getSuccess(), "Failed call should have success=false");
        assertEquals("Connection timeout", saved.getErrorMessage());
        assertEquals(0, saved.getPromptTokens(), "Failed call should have 0 prompt tokens");
        assertEquals(0, saved.getCompletionTokens(), "Failed call should have 0 completion tokens");
        assertEquals(500, saved.getLatencyMs());
    }

    @Test
    void logFailure_shouldTruncateLongErrorMessage() {
        String longError = "E".repeat(1200);

        logger.logFailure("sess-1", 1, "chat", "test-model",
                200, "request", longError);

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository).save(captor.capture());

        assertEquals(1000, captor.getValue().getErrorMessage().length(),
                "Error message should be truncated to 1000 chars");
    }

    @Test
    void logFailure_whenRepositoryThrows_shouldNotPropagate() {
        doThrow(new RuntimeException("DB down"))
                .when(repository).save(any(LlmCallLog.class));

        assertDoesNotThrow(() -> logger.logFailure("sess-1", 1, "chat", "test-model",
                200, "request", "error msg"),
                "logFailure must swallow exceptions");
    }

    @Test
    void logFailure_withNullTurnIndex_shouldPersist() {
        // Routing failure logs have null turnIndex
        logger.logFailure("sess-1", null, "routing", "test-model",
                300, "topic=something", "API error");

        ArgumentCaptor<LlmCallLog> captor = ArgumentCaptor.forClass(LlmCallLog.class);
        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getTurnIndex());
        assertFalse(captor.getValue().getSuccess());
    }
}
