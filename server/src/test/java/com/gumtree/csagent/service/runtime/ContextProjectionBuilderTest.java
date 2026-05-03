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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContextProjectionBuilder.
 * Verifies projection fields (task_summary, risk_flags, budget_state, tool_schemas)
 * after the Phase 0 §0.6 deviation removed the action abstraction layer in favor of
 * single-layer tool-use. tool_schemas now carries full per-tool schema objects
 * (name + description + arguments_schema) and there is no top-level allowed_actions field.
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
        // @PostConstruct is not invoked by Spring in unit tests; trigger schema init manually.
        builder.initToolSchemas();
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

    // --- Phase 0 §0.6 deviation: allowed_actions has been removed ---

    @Test
    void buildProjection_shouldNotContainAllowedActions_faqUc() throws Exception {
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

        assertFalse(root.has("allowed_actions"),
                "Projection MUST NOT contain top-level allowed_actions per Phase 0 §0.6 deviation");
    }

    @Test
    void buildProjection_shouldNotContainAllowedActions_intakeUc() throws Exception {
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

        assertFalse(root.has("allowed_actions"),
                "Projection MUST NOT contain top-level allowed_actions per Phase 0 §0.6 deviation");
    }

    @Test
    void buildProjection_shouldNotContainAllowedActions_discoverPhase() throws Exception {
        BotSession session = buildSession(null, "DISCOVER");

        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertFalse(root.has("allowed_actions"),
                "Projection MUST NOT contain top-level allowed_actions per Phase 0 §0.6 deviation");
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

    // --- tool_schemas field (Phase 0 §0.6: enriched to full schema objects) ---

    @Test
    void buildProjection_ucA_toolSchemasShouldContainFullSchemaObjects() throws Exception {
        BotSession session = buildSession("UC-A", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        // UC-A agent_visible per tool-policy.yaml: search_knowledge, resolve_article,
        // get_customer_context, request_handover, record_outcome.
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of(
                "search_knowledge", "resolve_article", "get_customer_context",
                "request_handover", "record_outcome"));

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("tool_schemas"));
        JsonNode tools = root.get("tool_schemas");
        assertTrue(tools.isArray(), "tool_schemas must be an array");
        assertEquals(5, tools.size());

        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            assertTrue(tool.isObject(), "Each tool_schemas entry must be an object, not a string");
            assertTrue(tool.has("name"), "Each entry must have 'name'");
            assertTrue(tool.has("description"), "Each entry must have 'description'");
            assertTrue(tool.has("arguments_schema"), "Each entry must have 'arguments_schema'");
            assertTrue(tool.get("arguments_schema").isObject(),
                    "arguments_schema must be a JSON object (JSON Schema-style)");
            assertFalse(tool.get("description").asText().isBlank(),
                    "description must be non-empty");
            names.add(tool.get("name").asText());
        }

        assertTrue(names.contains("search_knowledge"));
        assertTrue(names.contains("resolve_article"));
        assertTrue(names.contains("get_customer_context"));
        assertTrue(names.contains("request_handover"));
        assertTrue(names.contains("record_outcome"));
    }

    @Test
    void buildProjection_ucK_toolSchemasShouldExcludeKnowledgeTools() throws Exception {
        BotSession session = buildSession("UC-K", "RESOLVE");

        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-K", "Technical Bug Report", List.of("Technical Support"), "MEDIUM", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(ucDef);
        when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);
        when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
        // UC-K agent_visible per tool-policy.yaml: get_customer_context, request_handover,
        // record_outcome (NO search_knowledge / resolve_article).
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-K")).thenReturn(List.of(
                "get_customer_context", "request_handover", "record_outcome"));

        String projection = builder.buildProjection(session, List.of(), null, "test");
        JsonNode root = objectMapper.readTree(projection);

        JsonNode tools = root.get("tool_schemas");
        assertTrue(tools.isArray());

        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            assertTrue(tool.isObject());
            names.add(tool.get("name").asText());
        }

        assertFalse(names.contains("search_knowledge"),
                "UC-K must NOT expose search_knowledge per tool-policy.yaml");
        assertFalse(names.contains("resolve_article"),
                "UC-K must NOT expose resolve_article per tool-policy.yaml");
        assertTrue(names.contains("get_customer_context"));
        assertTrue(names.contains("request_handover"));
        assertTrue(names.contains("record_outcome"));
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
        assertTrue(instruction.contains("Do NOT call search_knowledge again"),
                "Instruction should tell LLM not to call search_knowledge again");
        assertTrue(instruction.contains("non-empty user_message"),
                "Instruction should describe answering via user_message");
        assertTrue(instruction.contains("request_handover"),
                "Instruction should describe escalation via request_handover tool_call");
        assertTrue(instruction.contains("source_ids"),
                "Instruction should describe citing source_ids from knowledge_hits");
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
