package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 7 §I2 — focused regression for the intake_state projection +
 * UC-G/H/I/J/K intake-complete runtime guard. Anchored on cs_interactive_066
 * (UC-K — required fields {@code [platform, repro_steps_or_error_message]}).
 *
 * <p>Tests:
 * <ul>
 *   <li>intake_state is projected for intake UCs and absent for FAQ UCs.</li>
 *   <li>fields_collected / fields_remaining / intake_complete update as
 *       fields are persisted to {@code session.intakeFields}.</li>
 *   <li>Intake systemInstruction references intake_state.</li>
 *   <li>{@link AgentRunLoopImpl#shouldRejectIncompleteIntakeHandover}
 *       rejects premature {@code intake_complete_for_uc_k} for UC-K when
 *       fields are missing.</li>
 *   <li>The guard allows the handover once both required fields are
 *       collected.</li>
 *   <li>Non-intake / FAQ-path UCs are unaffected by the new guard.</li>
 *   <li>Other handover reasons (e.g. user_requested) pass through.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class Sprint7IntakeStateTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ContextProjectionBuilder builder;
    private PhaseEvaluator phaseEvaluator;

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private ToolPolicyEnforcer toolPolicyEnforcer;
    @Mock private com.gumtree.csagent.service.knowledge.KnowledgeSearchService knowledgeSearchService;
    @Mock private com.gumtree.csagent.service.guardrails.ScriptLibraryService scriptLibrary;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ActionParser actionParser;
    @Mock private com.gumtree.csagent.service.tools.CreateCaseControlledTool createCaseTool;
    @Mock private com.gumtree.csagent.service.observability.EventEmitter eventEmitter;
    @Mock private com.gumtree.csagent.service.tools.ToolDispatcher toolDispatcher;

    @BeforeEach
    void setUp() {
        builder = new ContextProjectionBuilder(objectMapper, useCaseRegistry, controlPolicy, toolPolicyEnforcer);
        builder.initToolSchemas();
        phaseEvaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, builder, actionParser, objectMapper,
                createCaseTool, eventEmitter, toolDispatcher,
                com.gumtree.csagent.service.runtime.skill.SkillTestFixtures.productionRegistry());
        lenient().when(controlPolicy.getMaxBotTurnsFaq()).thenReturn(6);
        lenient().when(controlPolicy.getMaxBotTurnsIntake()).thenReturn(8);
        lenient().when(controlPolicy.getMaxClarificationRounds()).thenReturn(3);
        lenient().when(controlPolicy.getMaxFaqMiss()).thenReturn(2);
    }

    // -------- IntakeFieldsRegistry contract --------

    @Test
    void intakeFieldsRegistry_pinsCanonicalRequiredFieldsPerUc() {
        assertEquals(List.of("platform", "repro_steps_or_error_message"),
                IntakeFieldsRegistry.requiredFieldsFor("UC-K"));
        assertEquals(List.of("ad_id_or_listing_url", "registered_email", "stated_reason_or_context"),
                IntakeFieldsRegistry.requiredFieldsFor("UC-H"));
        assertEquals(List.of("report_target", "report_type", "description"),
                IntakeFieldsRegistry.requiredFieldsFor("UC-J"));
        assertEquals(List.of(), IntakeFieldsRegistry.requiredFieldsFor("UC-A"));
    }

    @Test
    void intakeFieldsRegistry_intakeCompleteRequiresAllFields() {
        Map<String, String> partial = Map.of("platform", "Chrome on Windows 11");
        assertFalse(IntakeFieldsRegistry.intakeComplete("UC-K", partial));

        Map<String, String> full = Map.of(
                "platform", "Chrome on Windows 11",
                "repro_steps_or_error_message", "Phone option missing after update");
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-K", full));
        assertEquals(List.of(), IntakeFieldsRegistry.fieldsRemaining("UC-K", full));
    }

    @Test
    void intakeFieldsRegistry_aliasesNormaliseToCanonical() {
        Map<String, String> aliased = IntakeFieldsRegistry.mergeFields(
                new HashMap<>(),
                Map.of("os", "Windows 11", "repro_steps", "Tap the new ad button — option missing"));
        assertTrue(aliased.containsKey("platform"));
        assertTrue(aliased.containsKey("repro_steps_or_error_message"));
        assertTrue(IntakeFieldsRegistry.intakeComplete("UC-K", aliased));
    }

    // -------- intake_state projection --------

    @Test
    void buildProjection_ucKIntake_emptyState_surfacesAllRequiredAsRemaining() throws Exception {
        BotSession session = intakeSession("UC-K");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-K", "Technical Issue Intake", List.of("Technical Support"), "MEDIUM", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(ucDef);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-K")).thenReturn(List.of("request_handover"));

        String projection = builder.buildProjection(session, List.of(), null, "the option disappeared");
        JsonNode root = objectMapper.readTree(projection);

        assertTrue(root.has("intake_state"), "intake_state must be present for UC-K");
        JsonNode is = root.get("intake_state");
        assertEquals(2, is.get("required_fields").size());
        assertEquals(0, is.get("fields_collected").size());
        assertEquals(2, is.get("fields_remaining").size());
        assertFalse(is.get("intake_complete").asBoolean());
    }

    @Test
    void buildProjection_ucKIntake_partialState_movesFieldFromRemainingToCollected() throws Exception {
        BotSession session = intakeSession("UC-K");
        session.setIntakeFields("{\"platform\":\"Chrome on Windows 11\"}");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-K", "Technical Issue Intake", List.of("Technical Support"), "MEDIUM", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(ucDef);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-K")).thenReturn(List.of("request_handover"));

        String projection = builder.buildProjection(session, List.of(), null, "the option disappeared");
        JsonNode root = objectMapper.readTree(projection);
        JsonNode is = root.get("intake_state");

        assertEquals(1, is.get("fields_collected").size());
        assertEquals("Chrome on Windows 11", is.get("fields_collected").get("platform").asText());
        assertEquals(1, is.get("fields_remaining").size());
        assertEquals("repro_steps_or_error_message", is.get("fields_remaining").get(0).asText());
        assertFalse(is.get("intake_complete").asBoolean());
    }

    @Test
    void buildProjection_ucKIntake_complete_marksIntakeComplete() throws Exception {
        BotSession session = intakeSession("UC-K");
        session.setIntakeFields(
                "{\"platform\":\"Chrome on Windows 11\",\"repro_steps_or_error_message\":\"option missing\"}");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-K", "Technical Issue Intake", List.of("Technical Support"), "MEDIUM", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(ucDef);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-K")).thenReturn(List.of("request_handover"));

        String projection = builder.buildProjection(session, List.of(), null, "thanks");
        JsonNode root = objectMapper.readTree(projection);
        JsonNode is = root.get("intake_state");

        assertEquals(0, is.get("fields_remaining").size());
        assertTrue(is.get("intake_complete").asBoolean());
    }

    @Test
    void buildProjection_faqUc_doesNotEmitIntakeState() throws Exception {
        BotSession session = BotSession.builder()
                .sessionId("test-faq")
                .activeUseCase("UC-A")
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0).clarificationCount(0).faqMissCount(0).repeatedActionCount(0)
                .build();
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A")).thenReturn(List.of("search_knowledge"));

        String projection = builder.buildProjection(session, List.of(), null, "is my ad visible?");
        JsonNode root = objectMapper.readTree(projection);
        assertFalse(root.has("intake_state"),
                "intake_state must NOT be projected for FAQ-path UCs (cs_095 / cs_001 / cs_011 guard)");
    }

    // -------- Intake systemInstruction --------

    @Test
    void intakePlan_systemInstruction_referencesIntakeState() {
        BotSession session = intakeSession("UC-K");
        UseCaseRegistryService.UseCaseDefinition ucDef = new UseCaseRegistryService.UseCaseDefinition(
                "UC-K", "Technical Issue Intake", List.of("Technical Support"), "MEDIUM", false, "INTAKE");
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(ucDef);

        PhasePlan plan = phaseEvaluator.plan(session, "the option disappeared", List.of());
        assertNotNull(plan);
        String si = plan.systemInstruction();
        assertTrue(si.contains("intake_state"),
                "Intake systemInstruction must reference intake_state");
        assertTrue(si.contains("fields_remaining"));
        assertTrue(si.contains("fields_collected"));
        assertTrue(si.contains("intake_complete_for_uc_k"));
        assertTrue(si.contains("platform")
                        && si.contains("repro_steps_or_error_message"),
                "Intake systemInstruction must enumerate UC-K canonical fields");
    }

    // -------- shouldRejectIncompleteIntakeHandover --------

    @Test
    void shouldRejectIncompleteIntakeHandover_rejectsWhenAllFieldsMissing() {
        BotSession session = intakeSession("UC-K");
        ToolCall call = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_k"));
        assertTrue(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(intakePlan("UC-K"), call, session));
    }

    @Test
    void shouldRejectIncompleteIntakeHandover_rejectsWhenOneFieldMissing() {
        BotSession session = intakeSession("UC-K");
        session.setIntakeFields("{\"platform\":\"Android 14\"}");
        ToolCall call = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_k"));
        assertTrue(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(intakePlan("UC-K"), call, session));
    }

    @Test
    void shouldRejectIncompleteIntakeHandover_allowsWhenAllFieldsPresent() {
        BotSession session = intakeSession("UC-K");
        session.setIntakeFields(
                "{\"platform\":\"Chrome on Windows 11\",\"repro_steps_or_error_message\":\"button gone\"}");
        ToolCall call = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_k"));
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(intakePlan("UC-K"), call, session));
    }

    @Test
    void shouldRejectIncompleteIntakeHandover_allowsUserRequestedRegardlessOfFields() {
        BotSession session = intakeSession("UC-K");
        ToolCall call = new ToolCall("request_handover",
                Map.of("escalation_reason", "user_requested"));
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(intakePlan("UC-K"), call, session),
                "user_requested handover must always pass — Sprint 6 G1 cs176 contract");
    }

    @Test
    void shouldRejectIncompleteIntakeHandover_allowsIncompleteIntakeReason() {
        BotSession session = intakeSession("UC-K");
        ToolCall call = new ToolCall("request_handover",
                Map.of("escalation_reason", "incomplete_intake"));
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(intakePlan("UC-K"), call, session),
                "incomplete_intake is the canonical fallback when fields cannot be collected");
    }

    @Test
    void shouldRejectIncompleteIntakeHandover_doesNotFireForFaqPathUc() {
        BotSession session = BotSession.builder()
                .sessionId("test-faq").activeUseCase("UC-A").currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING").totalBotTurns(0).clarificationCount(0)
                .faqMissCount(0).repeatedActionCount(0)
                .build();
        ToolCall call = new ToolCall("request_handover",
                Map.of("escalation_reason", "intake_complete_for_uc_k"));
        assertFalse(AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(faqPlan("UC-A"), call, session));
    }

    // -------- persistInlineIntakeFields --------

    @Test
    void persistInlineIntakeFields_mergesAliasedFieldsIntoSession() {
        BotSession session = intakeSession("UC-K");
        AgentRunLoopImpl loop = new AgentRunLoopImpl(null, null, null, null, objectMapper);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("escalation_reason", "intake_complete_for_uc_k");
        args.put("intake_fields", Map.of(
                "os", "Chrome on Windows 11",
                "repro_steps", "phone option disappeared after the last update"));
        ToolCall call = new ToolCall("request_handover", args);

        loop.persistInlineIntakeFields(session, call);
        assertNotNull(session.getIntakeFields());
        assertTrue(session.getIntakeFields().contains("platform"),
                "alias 'os' should normalise to canonical 'platform'");
        assertTrue(session.getIntakeFields().contains("repro_steps_or_error_message"),
                "alias 'repro_steps' should normalise to canonical 'repro_steps_or_error_message'");
    }

    @Test
    void persistInlineIntakeFields_noOpWhenNoIntakeFieldsArgument() {
        BotSession session = intakeSession("UC-K");
        AgentRunLoopImpl loop = new AgentRunLoopImpl(null, null, null, null, objectMapper);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("escalation_reason", "intake_complete_for_uc_k");
        ToolCall call = new ToolCall("request_handover", args);
        loop.persistInlineIntakeFields(session, call);
        assertNull(session.getIntakeFields(),
                "no intake_fields argument should not write to session.intakeFields");
    }

    // -------- helpers --------

    private BotSession intakeSession(String uc) {
        return BotSession.builder()
                .sessionId("test-intake-" + uc)
                .activeUseCase(uc)
                .currentPhase("RESOLVE")
                .handlingState("BOT_HANDLING")
                .totalBotTurns(0).clarificationCount(0).faqMissCount(0).repeatedActionCount(0)
                .build();
    }

    private PhasePlan intakePlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("intake")
                .allowedTools(List.of("request_handover"))
                .maxToolSteps(3)
                .build();
    }

    private PhasePlan faqPlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("faq")
                .allowedTools(List.of("search_knowledge", "resolve_article", "request_handover", "record_outcome"))
                .maxToolSteps(4)
                .build();
    }
}
