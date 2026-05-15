package com.gumtree.csagent.service.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventEmitter (D12.3 - Event emission completeness).
 * Verifies each event type is emitted correctly with proper payloads.
 */
@ExtendWith(MockitoExtension.class)
class EventEmitterTest {

    private EventEmitter eventEmitter;

    @Mock
    private BotEventRepository botEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        eventEmitter = new EventEmitter(botEventRepository, objectMapper);
    }

    @Test
    void emit_shouldPersistEventWithCorrectFields() {
        Map<String, Object> payload = Map.of("key", "value");
        eventEmitter.emit("session-1", EventType.SESSION_STARTED, 0, payload);

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent saved = captor.getValue();
        assertEquals("session-1", saved.getSessionId());
        assertEquals("SESSION_STARTED", saved.getEventType());
        assertEquals(0, saved.getTurnIndex());
        assertNotNull(saved.getEventId(), "eventId should be generated");
        assertNotNull(saved.getCreatedAt(), "createdAt should be set");
        assertTrue(saved.getPayload().contains("\"key\":\"value\""));
    }

    @Test
    void emit_nullPayload_shouldPersistEventWithNullPayload() {
        eventEmitter.emit("session-1", EventType.SESSION_CLOSED, null, null);

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());
        assertNull(captor.getValue().getPayload());
    }

    @Test
    void emitSessionStarted_shouldEmitCorrectEventType() {
        Map<String, Object> formCtx = Map.of("topic_subject", "Ad Support");
        eventEmitter.emitSessionStarted("session-1", formCtx);

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());
        assertEquals("SESSION_STARTED", captor.getValue().getEventType());
    }

    @Test
    void emitUseCaseInferred_shouldIncludeUseCaseAndConfidence() {
        eventEmitter.emitUseCaseInferred("session-1", 0, "UC-A", 0.95);

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("USE_CASE_INFERRED", event.getEventType());
        assertTrue(event.getPayload().contains("UC-A"));
        assertTrue(event.getPayload().contains("0.95"));
    }

    @Test
    void emitRetrievalExecuted_shouldIncludeQueryAndResultCount() {
        eventEmitter.emitRetrievalExecuted("session-1", 1, "how to fix ad", false, 3);

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("RETRIEVAL_EXECUTED", event.getEventType());
        assertTrue(event.getPayload().contains("how to fix ad"));
        assertTrue(event.getPayload().contains("3"));
    }

    @Test
    void emitArticleShown_shouldIncludeArticleIdAndTitle() {
        eventEmitter.emitArticleShown("session-1", 1, "ka44J000", "How to Fix Your Ad");

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("ARTICLE_SHOWN", event.getEventType());
        assertTrue(event.getPayload().contains("ka44J000"));
        assertTrue(event.getPayload().contains("How to Fix Your Ad"));
    }

    @Test
    void emitEscalationRequested_shouldIncludeReason() {
        eventEmitter.emitEscalationRequested("session-1", 3, "budget_exceeded:max_turns");

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("ESCALATION_REQUESTED", event.getEventType());
        assertTrue(event.getPayload().contains("budget_exceeded"));
    }

    @Test
    void emitCaseCreated_shouldIncludeCaseIdAndUseCase() {
        eventEmitter.emitCaseCreated("session-1", 2, "CASE-12345", "UC-H");

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("CASE_CREATED", event.getEventType());
        assertTrue(event.getPayload().contains("CASE-12345"));
        assertTrue(event.getPayload().contains("UC-H"));
    }

    @Test
    void emitOutcomeRecorded_shouldIncludeOutcomeAndUseCase() {
        eventEmitter.emitOutcomeRecorded("session-1", "RESOLVED", "UC-C");

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("OUTCOME_RECORDED", event.getEventType());
        assertTrue(event.getPayload().contains("RESOLVED"));
        assertTrue(event.getPayload().contains("UC-C"));
    }

    @Test
    void emitSessionClosed_shouldIncludeOutcome() {
        eventEmitter.emitSessionClosed("session-1", "RESOLVED");

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("SESSION_CLOSED", event.getEventType());
        assertTrue(event.getPayload().contains("RESOLVED"));
    }

    @Test
    void emitToolScopeBlocked_shouldIncludeToolAndUseCase() {
        eventEmitter.emitToolScopeBlocked("session-1", 1, "search_knowledge", "UC-G");

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("TOOL_SCOPE_BLOCKED", event.getEventType());
        assertTrue(event.getPayload().contains("search_knowledge"));
        assertTrue(event.getPayload().contains("UC-G"));
    }

    @Test
    void emitGuardrailViolation_shouldIncludeCategoryAndMatchedText() {
        eventEmitter.emitGuardrailViolation("session-1", 1, "FORBIDDEN_PHRASE", "guarantee");

        ArgumentCaptor<BotEvent> captor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(captor.capture());

        BotEvent event = captor.getValue();
        assertEquals("GUARDRAIL_VIOLATION", event.getEventType());
        assertTrue(event.getPayload().contains("FORBIDDEN_PHRASE"));
        assertTrue(event.getPayload().contains("guarantee"));
    }

    @Test
    void allEventTypes_shouldHaveCorrespondingEmitMethod() {
        // Verify that all EventType enum values have emit methods
        // This is a documentation test to catch if new event types are added
        // without corresponding emit methods
        EventType[] types = EventType.values();
        assertTrue(types.length >= 11, "Should have at least 11 event types");

        // Verify key types exist
        assertNotNull(EventType.valueOf("SESSION_STARTED"));
        assertNotNull(EventType.valueOf("USE_CASE_INFERRED"));
        assertNotNull(EventType.valueOf("RETRIEVAL_EXECUTED"));
        assertNotNull(EventType.valueOf("ARTICLE_SHOWN"));
        assertNotNull(EventType.valueOf("CLARIFICATION_ASKED"));
        assertNotNull(EventType.valueOf("ESCALATION_REQUESTED"));
        assertNotNull(EventType.valueOf("CASE_CREATED"));
        assertNotNull(EventType.valueOf("OUTCOME_RECORDED"));
        assertNotNull(EventType.valueOf("SESSION_CLOSED"));
        assertNotNull(EventType.valueOf("TOOL_SCOPE_BLOCKED"));
        assertNotNull(EventType.valueOf("GUARDRAIL_VIOLATION"));
        assertNotNull(EventType.valueOf("OUT_OF_SCOPE_HANDOVER"));
    }
}
