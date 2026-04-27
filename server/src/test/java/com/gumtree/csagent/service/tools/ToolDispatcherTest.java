package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.config.MockProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.service.guardrails.ProgressPlaceholderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ToolDispatcher (D12 - Harness Completion).
 * Verifies tool lookup, policy enforcement, dispatching, and latency tracking.
 */
@ExtendWith(MockitoExtension.class)
class ToolDispatcherTest {

    private ToolDispatcher dispatcher;

    @Mock
    private ToolPolicyEnforcer policyEnforcer;

    @Mock
    private ProgressPlaceholderService placeholderService;

    @Mock
    private MockProperties mockProperties;

    @Mock
    private Tool mockSearchKnowledge;

    @Mock
    private Tool mockRequestHandover;

    @BeforeEach
    void setUp() {
        when(mockSearchKnowledge.getName()).thenReturn("search_knowledge");
        when(mockRequestHandover.getName()).thenReturn("request_handover");

        dispatcher = new ToolDispatcher(
                policyEnforcer,
                List.of(mockSearchKnowledge, mockRequestHandover),
                placeholderService,
                mockProperties
        );
    }

    // --- Tool lookup ---

    @Test
    void hasToolNamed_registeredTool_shouldReturnTrue() {
        assertTrue(dispatcher.hasToolNamed("search_knowledge"));
        assertTrue(dispatcher.hasToolNamed("request_handover"));
    }

    @Test
    void hasToolNamed_unknownTool_shouldReturnFalse() {
        assertFalse(dispatcher.hasToolNamed("nonexistent_tool"));
    }

    // --- Policy enforcement ---

    @Test
    void dispatch_policyAllowed_shouldExecuteTool() {
        BotSession session = buildSession("UC-A");
        when(policyEnforcer.isToolAllowed("search_knowledge", "UC-A")).thenReturn(true);
        when(mockSearchKnowledge.execute(eq(session), any())).thenReturn(ToolResult.ok(Map.of("data", "test")));
        when(mockProperties.getToolLatencyMs()).thenReturn(0);

        ToolResult result = dispatcher.dispatch("search_knowledge", session, Map.of());

        assertTrue(result.isSuccess());
        verify(mockSearchKnowledge).execute(eq(session), any());
    }

    @Test
    void dispatch_policyBlocked_shouldReturnError() {
        BotSession session = buildSession("UC-G");
        when(policyEnforcer.isToolAllowed("search_knowledge", "UC-G")).thenReturn(false);

        ToolResult result = dispatcher.dispatch("search_knowledge", session, Map.of());

        assertFalse(result.isSuccess(), "Blocked tool should return error");
        assertTrue(result.getErrorMessage().contains("not allowed"),
                "Error should explain the tool is not allowed");
        verify(policyEnforcer).emitBlockedEvent("test-session", "search_knowledge", "UC-G");
        verify(mockSearchKnowledge, never()).execute(any(), any());
    }

    @Test
    void dispatch_unknownTool_shouldReturnError() {
        BotSession session = buildSession("UC-A");

        ToolResult result = dispatcher.dispatch("nonexistent_tool", session, Map.of());

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Unknown tool"));
    }

    // --- Latency tracking ---

    @Test
    void dispatch_shouldTrackLatency() {
        BotSession session = buildSession("UC-A");
        when(policyEnforcer.isToolAllowed("search_knowledge", "UC-A")).thenReturn(true);
        when(mockSearchKnowledge.execute(eq(session), any())).thenReturn(ToolResult.ok(Map.of()));
        when(mockProperties.getToolLatencyMs()).thenReturn(0);

        ToolResult result = dispatcher.dispatch("search_knowledge", session, Map.of());

        assertTrue(result.isSuccess());
        assertTrue(result.getLatencyMs() >= 0, "Latency should be tracked");
    }

    // --- Tool exception handling ---

    @Test
    void dispatch_toolThrowsException_shouldReturnError() {
        BotSession session = buildSession("UC-A");
        when(policyEnforcer.isToolAllowed("search_knowledge", "UC-A")).thenReturn(true);
        when(mockSearchKnowledge.execute(eq(session), any()))
                .thenThrow(new RuntimeException("Database connection failed"));
        when(mockProperties.getToolLatencyMs()).thenReturn(0);

        ToolResult result = dispatcher.dispatch("search_knowledge", session, Map.of());

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Database connection failed"));
    }

    // --- getVisibleToolsForUc delegation ---

    @Test
    void getVisibleToolsForUc_shouldDelegateToEnforcer() {
        when(policyEnforcer.getVisibleToolsForUc("UC-A"))
                .thenReturn(List.of("search_knowledge", "request_handover"));

        List<String> tools = dispatcher.getVisibleToolsForUc("UC-A");

        assertEquals(2, tools.size());
        assertTrue(tools.contains("search_knowledge"));
        assertTrue(tools.contains("request_handover"));
        verify(policyEnforcer).getVisibleToolsForUc("UC-A");
    }

    private BotSession buildSession(String activeUseCase) {
        return BotSession.builder()
                .sessionId("test-session")
                .activeUseCase(activeUseCase)
                .handlingState("BOT_HANDLING")
                .currentPhase("RESOLVE")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
