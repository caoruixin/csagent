package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.KnowledgeHit;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContextProjectionBuilder (D11.2 - Context Projection Enhancement).
 * Verifies that the 5 new fields (task_summary, allowed_actions, risk_flags,
 * budget_state, tool_schemas) are correctly included in the projected context.
 */
@ExtendWith(MockitoExtension.class)
class ContextProjectionBuilderTest {

    private ContextProjectionBuilder builder;
    private ObjectMapper objectMapper;

    @Mock
    private UseCaseRegistryService useCaseRegistry;

    @Mock
    private ControlPolicyService controlPolicy;

    @Mock
    private ToolPolicyEnforcer toolPolicyEnforcer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer);
    }

    // --- D11.2: task_summary field ---

    @Test
    void buildProjection_withActiveUc_shouldContainTaskSummary() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");
        session.setFormTopicSubject("Ad Support");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of("search_knowledge", "resolve_article"));

        String projection = builder.buildProjection(session, List.of(), null, "my ad is missing");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("task_summary"), "Projection should contain task_summary");
        String taskSummary = root.get("task_summary").asText();
        assertTrue(taskSummary.contains("Ad Support"), "task_summary should reference topic");
        assertTrue(taskSummary.contains("Ad Status & Visibility"), "task_summary should reference UC name");
    }

    @Test
    void buildProjection_noActiveUc_shouldStillContainTaskSummary() throws Exception {
        BotSession session = buildSession(null, "DISCOVER");
        session.setFormTopicSubject("Ad Support");

        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        String projection = builder.buildProjection(session, List.of(), null, "help me");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("task_summary"));
        String taskSummary = root.get("task_summary").asText();
        assertTrue(taskSummary.contains("Ad Support"));
    }

    // --- D11.2: allowed_actions field ---

    @Test
    void buildProjection_resolvePhase_faqUc_shouldIncludeFullActionSet() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of());

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("allowed_actions"));
        JsonNode actions = root.get("allowed_actions");
        assertTrue(actions.isArray());

        List<String> actionList = new java.util.ArrayList<>();
        actions.forEach(n -> actionList.add(n.asText()));
        assertTrue(actionList.contains("retrieve_knowledge"), "FAQ UC in RESOLVE should allow retrieve_knowledge");
        assertTrue(actionList.contains("answer_grounded"), "FAQ UC in RESOLVE should allow answer_grounded");
        assertTrue(actionList.contains("ask_user"), "FAQ UC in RESOLVE should allow ask_user");
        assertTrue(actionList.contains("escalate_human"), "FAQ UC in RESOLVE should allow escalate_human");
        assertTrue(actionList.contains("finish"), "FAQ UC in RESOLVE should allow finish");
    }

    @Test
    void buildProjection_resolvePhase_intakeUc_shouldOnlyAllowAskUserAndEscalate() throws Exception {
        BotSession session = buildSession("UC-H", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-H", "Ad Removal Appeal", List.of("Ad Support"), "HIGH", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-H")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-H")).thenReturn(List.of());

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        JsonNode actions = root.get("allowed_actions");
        List<String> actionList = new java.util.ArrayList<>();
        actions.forEach(n -> actionList.add(n.asText()));
        assertTrue(actionList.contains("ask_user"), "Intake UC in RESOLVE should allow ask_user");
        assertTrue(actionList.contains("escalate_human"), "Intake UC in RESOLVE should allow escalate_human");
        assertFalse(actionList.contains("retrieve_knowledge"), "Intake UC should NOT allow retrieve_knowledge");
        assertFalse(actionList.contains("finish"), "Intake UC should NOT allow finish");
    }

    @Test
    void buildProjection_discoverPhase_shouldAllowAskUserAndEscalate() throws Exception {
        BotSession session = buildSession(null, "DISCOVER");

        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        JsonNode actions = root.get("allowed_actions");
        List<String> actionList = new java.util.ArrayList<>();
        actions.forEach(n -> actionList.add(n.asText()));
        assertTrue(actionList.contains("ask_user"));
        assertTrue(actionList.contains("escalate_human"));
        assertEquals(2, actionList.size(), "DISCOVER phase should only have ask_user and escalate_human");
    }

    // --- D11.2: risk_flags field ---

    @Test
    void buildProjection_withActiveUc_shouldContainRiskFlags() throws Exception {
        BotSession session = buildSession("UC-J", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-J", "Trust & Safety Report", List.of(), "CRITICAL", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-J")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-J")).thenReturn(List.of());

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("risk_flags"));
        JsonNode riskFlags = root.get("risk_flags");
        assertTrue(riskFlags.isArray());
        assertEquals(1, riskFlags.size());
        assertEquals("CRITICAL", riskFlags.get(0).asText());
    }

    @Test
    void buildProjection_noActiveUc_shouldReturnEmptyRiskFlags() throws Exception {
        BotSession session = buildSession(null, "DISCOVER");

        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("risk_flags"));
        assertEquals(0, root.get("risk_flags").size(), "No active UC should yield empty risk_flags");
    }

    // --- D11.2: budget_state field ---

    @Test
    void buildProjection_shouldContainBudgetState() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");
        session.setTotalBotTurns(3);
        session.setClarificationCount(1);
        session.setFaqMissCount(0);

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of());

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("budget_state"));
        JsonNode budget = root.get("budget_state");
        assertEquals(3, budget.get("total_bot_turns").asInt());
        assertEquals(6, budget.get("max_bot_turns").asInt());
        assertEquals(1, budget.get("clarification_count").asInt());
        assertEquals(3, budget.get("max_clarification").asInt());
        assertEquals(0, budget.get("faq_miss_count").asInt());
        assertEquals(2, budget.get("max_faq_miss").asInt());
    }

    // --- D11.2: tool_schemas field ---

    @Test
    void buildProjection_withVisibleTools_shouldContainToolSchemas() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(
                List.of("search_knowledge", "resolve_article", "lookup_listing"));

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("tool_schemas"));
        JsonNode tools = root.get("tool_schemas");
        assertTrue(tools.isArray());
        assertEquals(3, tools.size());
    }

    @Test
    void buildProjection_noActiveUc_shouldReturnEmptyToolSchemas() throws Exception {
        BotSession session = buildSession(null, "DISCOVER");

        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("tool_schemas"));
        assertEquals(0, root.get("tool_schemas").size());
    }

    // --- D11.2: customer_context and listing_context in projection ---

    @Test
    void buildProjection_withCustomerContext_shouldIncludeInProjection() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");
        session.setCustomerContext("{\"account_status\":\"ACTIVE\",\"account_type\":\"personal\"}");
        session.setListingContext("{\"ad_id\":\"AD-1001\",\"status\":\"LIVE\"}");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of());

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("customer_context"), "Projection should include customer_context when set");
        assertEquals("ACTIVE", root.get("customer_context").get("account_status").asText());
        assertTrue(root.has("listing_context"), "Projection should include listing_context when set");
        assertEquals("AD-1001", root.get("listing_context").get("ad_id").asText());
    }

    @Test
    void buildProjection_withoutCustomerContext_shouldNotIncludeField() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");
        // customerContext and listingContext are null by default

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of());

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertFalse(root.has("customer_context"), "Projection should NOT include customer_context when null");
        assertFalse(root.has("listing_context"), "Projection should NOT include listing_context when null");
    }

    // --- PII redaction ---

    @Test
    void buildProjection_emailInUserMessage_shouldBeRedacted() throws Exception {
        BotSession session = buildSession(null, "DISCOVER");

        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        String projection = builder.buildProjection(session, List.of(), null, "contact me at jane@example.com");
        JsonNode root = objectMapper.readTree(projection);

        String currentMsg = root.get("current_user_message").asText();
        assertFalse(currentMsg.contains("jane@example.com"), "Email should be redacted");
        assertTrue(currentMsg.contains("[REDACTED_EMAIL]"), "Should contain redaction placeholder");
    }

    // --- Knowledge instruction ---

    @Test
    void buildProjection_withKnowledgeHits_shouldIncludeKnowledgeInstruction() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of());

        KnowledgeHit hit = KnowledgeHit.builder()
                .sourceId("KB-001")
                .title("Ad Visibility FAQ")
                .snippet("Your ad may not be visible due to...")
                .score(0.85)
                .build();

        String projection = builder.buildProjection(session, List.of(), List.of(hit), "my ad is missing");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("knowledge_hits"));
        assertTrue(root.has("knowledge_instruction"));
        String instruction = root.get("knowledge_instruction").asText();
        assertTrue(instruction.contains("answer_grounded"), "Instruction should tell LLM to use answer_grounded");
        assertTrue(instruction.contains("Do NOT return action 'retrieve_knowledge'"),
                "Instruction should tell LLM not to use retrieve_knowledge");
    }

    private BotSession buildSession(String activeUseCase, String phase) {
        return BotSession.builder()
                .sessionId("test-session-ctx")
                .activeUseCase(activeUseCase)
                .currentPhase(phase)
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();
    }
}
