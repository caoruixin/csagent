package com.gumtree.csagent.service.tools;

import com.gumtree.csagent.repository.BotEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ToolPolicyEnforcer (D11/D12 - Tool Layer Wiring & Policy Enforcement).
 * Verifies that tool access is correctly restricted per UC based on tool-policy.yaml.
 */
@ExtendWith(MockitoExtension.class)
class ToolPolicyEnforcerTest {

    private ToolPolicyEnforcer enforcer;

    @Mock
    private BotEventRepository botEventRepository;

    @BeforeEach
    void setUp() {
        enforcer = new ToolPolicyEnforcer(botEventRepository);
        enforcer.init();
    }

    // --- search_knowledge: only allowed for FAQ UCs ---

    @ParameterizedTest
    // WS-3 / A3 (2026-07-25): UC-K joined the knowledge-capable set when its
    // registry path moved INTAKE -> PARTIAL.
    @ValueSource(strings = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP", "UC-K"})
    void isToolAllowed_searchKnowledge_faqUcs_shouldAllow(String ucId) {
        assertTrue(enforcer.isToolAllowed("search_knowledge", ucId),
                "search_knowledge should be allowed for FAQ UC: " + ucId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UC-G", "UC-H", "UC-I", "UC-J"})
    void isToolAllowed_searchKnowledge_intakeUcs_shouldBlock(String ucId) {
        assertFalse(enforcer.isToolAllowed("search_knowledge", ucId),
                "search_knowledge should be BLOCKED for INTAKE UC: " + ucId);
    }

    @Test
    void isToolAllowed_searchKnowledge_nullUc_shouldBlock() {
        assertFalse(enforcer.isToolAllowed("search_knowledge", null),
                "search_knowledge should be blocked when no UC is set");
    }

    // --- resolve_article: same policy as search_knowledge ---

    @ParameterizedTest
    @ValueSource(strings = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP", "UC-K"})
    void isToolAllowed_resolveArticle_faqUcs_shouldAllow(String ucId) {
        assertTrue(enforcer.isToolAllowed("resolve_article", ucId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UC-G", "UC-H", "UC-I", "UC-J"})
    void isToolAllowed_resolveArticle_intakeUcs_shouldBlock(String ucId) {
        assertFalse(enforcer.isToolAllowed("resolve_article", ucId));
    }

    // --- request_handover: allowed for ALL ---

    @ParameterizedTest
    @ValueSource(strings = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP",
                             "UC-G", "UC-H", "UC-I", "UC-J", "UC-K"})
    void isToolAllowed_requestHandover_allUcs_shouldAllow(String ucId) {
        assertTrue(enforcer.isToolAllowed("request_handover", ucId),
                "request_handover should be allowed for ALL UCs");
    }

    @Test
    void isToolAllowed_requestHandover_nullUc_shouldAllow() {
        assertTrue(enforcer.isToolAllowed("request_handover", null),
                "request_handover with ALL policy should allow null UC");
    }

    // --- record_outcome: allowed for ALL ---

    @Test
    void isToolAllowed_recordOutcome_anyUc_shouldAllow() {
        assertTrue(enforcer.isToolAllowed("record_outcome", "UC-A"));
        assertTrue(enforcer.isToolAllowed("record_outcome", "UC-J"));
        assertTrue(enforcer.isToolAllowed("record_outcome", null));
    }

    // --- create_case_controlled: only UC-H, UC-J, UC-K ---

    @ParameterizedTest
    @ValueSource(strings = {"UC-H", "UC-J", "UC-K"})
    void isToolAllowed_createCaseControlled_allowedUcs_shouldAllow(String ucId) {
        assertTrue(enforcer.isToolAllowed("create_case_controlled", ucId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP",
                             "UC-G", "UC-I"})
    void isToolAllowed_createCaseControlled_otherUcs_shouldBlock(String ucId) {
        assertFalse(enforcer.isToolAllowed("create_case_controlled", ucId),
                "create_case_controlled should be blocked for " + ucId);
    }

    @Test
    void isToolAllowed_createCaseControlled_ucI_shouldBlock() {
        assertFalse(enforcer.isToolAllowed("create_case_controlled", "UC-I"),
                "UC-I (Refund) must NOT create cases");
    }

    // --- get_customer_context: specific UCs ---

    @ParameterizedTest
    @ValueSource(strings = {"UC-A", "UC-C", "UC-D", "UC-F", "UC-FP", "UC-K"})
    void isToolAllowed_getCustomerContext_allowedUcs_shouldAllow(String ucId) {
        assertTrue(enforcer.isToolAllowed("get_customer_context", ucId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UC-B", "UC-E", "UC-G", "UC-H", "UC-I", "UC-J"})
    void isToolAllowed_getCustomerContext_blockedUcs_shouldBlock(String ucId) {
        assertFalse(enforcer.isToolAllowed("get_customer_context", ucId));
    }

    // --- Unknown tool ---

    @Test
    void isToolAllowed_unknownTool_shouldBlock() {
        assertFalse(enforcer.isToolAllowed("nonexistent_tool", "UC-A"),
                "Unknown tools should be blocked by default");
    }

    // --- getToolType ---

    @Test
    void getToolType_agentVisibleTool_shouldReturnAgentVisible() {
        assertEquals("AGENT_VISIBLE", enforcer.getToolType("search_knowledge"));
        assertEquals("AGENT_VISIBLE", enforcer.getToolType("request_handover"));
        assertEquals("AGENT_VISIBLE", enforcer.getToolType("record_outcome"));
        assertEquals("AGENT_VISIBLE", enforcer.getToolType("get_customer_context"));
    }

    @Test
    void getToolType_runtimeOnlyTool_shouldReturnRuntimeOnly() {
        assertEquals("RUNTIME_ONLY", enforcer.getToolType("lookup_customer_account"));
        assertEquals("RUNTIME_ONLY", enforcer.getToolType("create_case_controlled"));
        assertEquals("RUNTIME_ONLY", enforcer.getToolType("get_moderation_review_context"));
    }

    @Test
    void getToolType_unknownTool_shouldReturnNull() {
        assertNull(enforcer.getToolType("nonexistent_tool"));
    }

    // --- getVisibleToolsForUc ---

    @Test
    void getVisibleToolsForUc_faqUc_shouldIncludeSearchKnowledge() {
        List<String> tools = enforcer.getVisibleToolsForUc("UC-A");
        assertTrue(tools.contains("search_knowledge"),
                "FAQ UC-A should have search_knowledge visible");
        assertTrue(tools.contains("resolve_article"));
        assertTrue(tools.contains("request_handover"));
        assertTrue(tools.contains("record_outcome"));
    }

    @Test
    void getVisibleToolsForUc_intakeUc_shouldExcludeSearchKnowledge() {
        List<String> tools = enforcer.getVisibleToolsForUc("UC-G");
        assertFalse(tools.contains("search_knowledge"),
                "INTAKE UC-G should NOT have search_knowledge visible");
        assertFalse(tools.contains("resolve_article"),
                "INTAKE UC-G should NOT have resolve_article visible");
        assertTrue(tools.contains("request_handover"),
                "UC-G should still have request_handover");
        assertTrue(tools.contains("record_outcome"),
                "UC-G should still have record_outcome");
    }

    @Test
    void getVisibleToolsForUc_shouldNeverIncludeRuntimeOnlyTools() {
        for (String uc : List.of("UC-A", "UC-B", "UC-C", "UC-G", "UC-H", "UC-J")) {
            List<String> tools = enforcer.getVisibleToolsForUc(uc);
            assertFalse(tools.contains("lookup_customer_account"),
                    "RUNTIME_ONLY tools should never be visible for UC: " + uc);
            assertFalse(tools.contains("create_case_controlled"),
                    "RUNTIME_ONLY tools should never be visible for UC: " + uc);
        }
    }

    // --- emitBlockedEvent ---

    @Test
    void emitBlockedEvent_shouldPersistEvent() {
        enforcer.emitBlockedEvent("session-123", "search_knowledge", "UC-G");

        verify(botEventRepository, times(1)).save(argThat(event ->
                "session-123".equals(event.getSessionId()) &&
                "TOOL_SCOPE_BLOCKED".equals(event.getEventType()) &&
                event.getPayload().contains("search_knowledge") &&
                event.getPayload().contains("UC-G")
        ));
    }

    @Test
    void emitBlockedEvent_nullUc_shouldHandleGracefully() {
        assertDoesNotThrow(() ->
                enforcer.emitBlockedEvent("session-123", "some_tool", null));

        verify(botEventRepository, times(1)).save(any());
    }
}
