package com.gumtree.csagent.service.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotEvent;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.enums.EventType;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotSessionRepository;
import com.gumtree.csagent.service.runtime.UseCaseRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClassifyUseCaseTool} (Phase 2 Fix 3c).
 *
 * <p>Verifies: valid commit writes session+event, unknown UC fails,
 * out-of-range confidence fails, missing args fail, non-numeric confidence
 * fails. Confirms the UC registry is the source of truth and the tool's
 * happy path returns a payload the AgentRunLoop can accumulate.
 */
@ExtendWith(MockitoExtension.class)
class ClassifyUseCaseToolTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private BotSessionRepository botSessionRepository;
    @Mock private BotEventRepository botEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ClassifyUseCaseTool tool;

    @BeforeEach
    void setUp() {
        tool = new ClassifyUseCaseTool(useCaseRegistry, botSessionRepository,
                botEventRepository, objectMapper);
    }

    @Test
    void getName_returnsClassifyUseCase() {
        assertEquals("classify_use_case", tool.getName());
    }

    @Test
    void execute_validCommit_setsSessionAndEmitsEvent() {
        BotSession session = newSession();
        when(useCaseRegistry.isKnownUseCase("UC-A")).thenReturn(true);
        Map<String, Object> args = new HashMap<>();
        args.put("use_case_id", "UC-A");
        args.put("confidence", 0.85);
        args.put("reasoning", "User is asking about ad status");

        ToolResult result = tool.execute(session, args);

        assertTrue(result.isSuccess(), "Valid classification should succeed");
        assertEquals("UC-A", session.getActiveUseCase());
        assertEquals(0, BigDecimal.valueOf(0.85).setScale(2, java.math.RoundingMode.HALF_UP)
                .compareTo(session.getIntentConfidence()));
        verify(botSessionRepository).save(session);

        ArgumentCaptor<BotEvent> eventCaptor = ArgumentCaptor.forClass(BotEvent.class);
        verify(botEventRepository).save(eventCaptor.capture());
        BotEvent event = eventCaptor.getValue();
        assertEquals(EventType.CLASSIFICATION_COMMITTED.name(), event.getEventType());
        assertEquals("test-session", event.getSessionId());
        assertNotNull(event.getPayload());
        assertTrue(event.getPayload().contains("UC-A"));

        Map<String, Object> data = result.getData();
        assertEquals(Boolean.TRUE, data.get("committed"));
        assertEquals("UC-A", data.get("use_case_id"));
    }

    @Test
    void execute_unknownUseCase_failsWithReason() {
        BotSession session = newSession();
        when(useCaseRegistry.isKnownUseCase("UC-Z")).thenReturn(false);
        Map<String, Object> args = Map.of("use_case_id", "UC-Z", "confidence", 0.9);

        ToolResult result = tool.execute(session, args);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().startsWith("unknown_use_case"),
                "Error must start with unknown_use_case reason code");
        assertNull(session.getActiveUseCase(), "Session must not be mutated on validation failure");
        verify(botSessionRepository, never()).save(any());
        verify(botEventRepository, never()).save(any());
    }

    @Test
    void execute_confidenceOutOfRange_failsWithReason() {
        BotSession session = newSession();
        when(useCaseRegistry.isKnownUseCase("UC-A")).thenReturn(true);
        Map<String, Object> args = Map.of("use_case_id", "UC-A", "confidence", 1.5);

        ToolResult result = tool.execute(session, args);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().startsWith("invalid_confidence"));
        assertNull(session.getActiveUseCase());
        verify(botSessionRepository, never()).save(any());
    }

    @Test
    void execute_missingUseCaseId_returnsError() {
        BotSession session = newSession();
        Map<String, Object> args = Map.of("confidence", 0.9);

        ToolResult result = tool.execute(session, args);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("use_case_id"));
        verifyNoInteractions(botSessionRepository, botEventRepository);
    }

    @Test
    void execute_missingConfidence_returnsError() {
        BotSession session = newSession();
        when(useCaseRegistry.isKnownUseCase("UC-B")).thenReturn(true);
        Map<String, Object> args = Map.of("use_case_id", "UC-B");

        ToolResult result = tool.execute(session, args);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("confidence"));
    }

    @Test
    void execute_nonNumericConfidence_returnsError() {
        BotSession session = newSession();
        when(useCaseRegistry.isKnownUseCase("UC-C")).thenReturn(true);
        Map<String, Object> args = Map.of("use_case_id", "UC-C", "confidence", "high");

        ToolResult result = tool.execute(session, args);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("confidence"));
    }

    private BotSession newSession() {
        return BotSession.builder()
                .sessionId("test-session")
                .currentPhase("DISCOVER")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
