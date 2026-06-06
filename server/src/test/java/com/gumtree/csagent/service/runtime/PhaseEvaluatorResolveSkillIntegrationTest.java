package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.service.guardrails.ScriptLibraryService;
import com.gumtree.csagent.service.knowledge.KnowledgeSearchService;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 39 behavioural-equivalence integration test per Sprint 37 freeze
 * §6.4 (decision (e) §6.2.5 + §6.2.6 RESOLVE-INTAKE + RESOLVE-FAQ).
 *
 * <p>For each of the 2 RESOLVE phase Skills migrated in Sprint 39, this test
 * pins the post-migration PhasePlan composed by the SkillRegistry +
 * {@code composeSkillPhasePlan(...)} template substitution to the GOLDEN
 * pre-migration PhasePlan that the legacy hardcoded Java-string branches in
 * {@code PhaseEvaluator.java:417-530} (HEAD {@code 5787806}) produced.
 *
 * <p>The golden strings below are bit-for-bit equivalent to what the legacy
 * branches concatenated, including the template-substitution placeholders
 * resolved per Sprint 37 freeze §6.2.5 (5 named placeholders + the dev-
 * judgement 6th {@code {intake_required_fields}} placeholder; see handoff
 * §7 OQ). For RESOLVE-INTAKE × UC-K the assembled systemInstruction matches
 * the legacy {@code buildIntakeSystemInstruction(...)} output for the
 * canonical 2-field UC-K intake spec.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhaseEvaluatorResolveSkillIntegrationTest {

    @Mock private UseCaseRegistryService useCaseRegistry;
    @Mock private KnowledgeSearchService knowledgeSearchService;
    @Mock private ScriptLibraryService scriptLibrary;
    @Mock private LlmInvocationService llmInvocation;
    @Mock private ContextProjectionBuilder contextProjection;
    @Mock private ActionParser actionParser;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ToolDispatcher toolDispatcher;

    private PhaseEvaluator evaluator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        evaluator = new PhaseEvaluator(
                useCaseRegistry, knowledgeSearchService, scriptLibrary,
                llmInvocation, contextProjection, actionParser,
                objectMapper, createCaseTool, eventEmitter, toolDispatcher,
                SkillTestFixtures.productionRegistry(), null);

        // Stub UC definitions used by composeSkillPhasePlan's {uc_name}
        // substitution. Names mirror the production
        // server/src/main/resources/config/use-case-registry.yaml entries
        // for the FAQ-path UCs and INTAKE-path UCs Sprint 39 migrates.
        lenient().when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef("UC-A", "Listing Visibility", "FAQ"));
        lenient().when(useCaseRegistry.getUseCase("UC-B")).thenReturn(ucDef("UC-B", "Posting Help", "FAQ"));
        lenient().when(useCaseRegistry.getUseCase("UC-C")).thenReturn(ucDef("UC-C", "Replies & Messaging", "FAQ"));
        lenient().when(useCaseRegistry.getUseCase("UC-D")).thenReturn(ucDef("UC-D", "Account & Login", "FAQ"));
        lenient().when(useCaseRegistry.getUseCase("UC-E")).thenReturn(ucDef("UC-E", "Safety", "FAQ"));
        lenient().when(useCaseRegistry.getUseCase("UC-F")).thenReturn(ucDef("UC-F", "Payments", "FAQ"));
        lenient().when(useCaseRegistry.getUseCase("UC-FP")).thenReturn(ucDef("UC-FP", "Featured Payments", "FAQ"));
        lenient().when(useCaseRegistry.getUseCase("UC-G")).thenReturn(ucDef("UC-G", "GDPR / Data Action", "INTAKE"));
        lenient().when(useCaseRegistry.getUseCase("UC-H")).thenReturn(ucDef("UC-H", "Ad Removal Appeal", "INTAKE"));
        lenient().when(useCaseRegistry.getUseCase("UC-I")).thenReturn(ucDef("UC-I", "Refund / Payment Dispute", "INTAKE"));
        lenient().when(useCaseRegistry.getUseCase("UC-J")).thenReturn(ucDef("UC-J", "Trust & Safety Report", "INTAKE"));
        lenient().when(useCaseRegistry.getUseCase("UC-K")).thenReturn(ucDef("UC-K", "Technical Issue Intake", "INTAKE"));
    }

    private UseCaseRegistryService.UseCaseDefinition ucDef(String id, String name, String path) {
        return new UseCaseRegistryService.UseCaseDefinition(
                id, name, List.of("Support"), "MEDIUM", false, path);
    }

    private BotSession session(String uc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-resolve-equiv-" + uc);
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase(uc);
        return s;
    }

    // -------------------- RESOLVE-FAQ golden strings (static) --------------------

    private static final String FAQ_PROCEDURE =
            "You are a helpful Gumtree customer support agent. "
                    + "Resolve the user's issue using the provided tools. "
                    + "FAQ-path RESOLVE flow (S1): the intended terminal sequence is "
                    + "search_knowledge -> resolve_article -> grounded customer-facing "
                    + "answer (with a source_id citation) -> record_outcome. "
                    + "Only escalate via request_handover after a valid resolve "
                    + "attempt cannot complete (no viable hit, or resolve_article "
                    + "could not produce a grounded answer).";

    private static final String FAQ_GROUNDING =
            "If tool data contains specific information about the user's case "
                    + "(account/ad/moderation), answer from that first. "
                    + "For policy/process explanations, you MUST call search_knowledge "
                    + "first if accumulated_tool_results.search_knowledge is empty; "
                    + "do not produce a factual customer-facing answer without "
                    + "grounded knowledge evidence. After search_knowledge returns a "
                    + "viable hit, you MUST call resolve_article for the top hit "
                    + "before answering the customer; cite the source_id in your "
                    + "user_message. If search_knowledge returns no viable hit, "
                    + "request_handover with escalation_reason="
                    + "'faq_miss_threshold_exceeded' is allowed. After a search_knowledge "
                    + "returns a viable hit (faq_miss=false), do NOT re-search this turn — "
                    + "draft your grounded answer from the existing hits via resolve_article, "
                    + "or escalate; a fresh search_knowledge is only warranted if the prior "
                    + "result was faq_miss=true or your new query is materially different "
                    + "from what you already searched.";

    private static final String FAQ_ESCALATION =
            "Escalate via request_handover if (a) the user explicitly requests "
                    + "a human (use escalation_reason='user_requested', priority 1), "
                    + "or (b) search_knowledge returned no viable hit and you "
                    + "cannot answer (use 'faq_miss_threshold_exceeded'), or "
                    + "(c) the issue is genuinely out of scope (use 'out_of_scope'). "
                    + "Do NOT short-circuit to request_handover("
                    + "'faq_miss_threshold_exceeded') when search_knowledge "
                    + "already returned a viable hit and resolve_article has not "
                    + "yet been attempted — the runtime will refuse such a "
                    + "handover and require a resolve_article attempt first.";

    private void assertGoldenFaq(PhasePlan plan, String expectedUcName) {
        assertNotNull(plan);
        assertEquals("RESOLVE", plan.phase());
        assertEquals("Determine the customer's issue and provide a grounded, helpful answer for "
                        + expectedUcName,
                plan.objective());
        assertEquals(List.of("get_customer_context", "search_knowledge",
                        "resolve_article", "record_outcome", "request_handover"),
                plan.allowedTools());
        assertEquals(Set.of("form_context", "customer_context", "listing_context"),
                plan.requiredContextKeys());
        assertEquals(6, plan.maxToolSteps());
        assertEquals(false, plan.allowInterimMessage());
        assertEquals(Set.of(TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE),
                plan.validTerminalOutcomes());
        assertEquals(FAQ_PROCEDURE, plan.systemInstruction());
        assertEquals(FAQ_GROUNDING, plan.groundingInstruction());
        assertEquals(FAQ_ESCALATION, plan.escalationPolicy());
    }

    @Test
    void faq_ucA_composesGoldenPhasePlan() {
        assertGoldenFaq(evaluator.plan(session("UC-A"), "msg", List.of()), "Listing Visibility");
    }

    @Test
    void faq_ucB_composesGoldenPhasePlan() {
        assertGoldenFaq(evaluator.plan(session("UC-B"), "msg", List.of()), "Posting Help");
    }

    @Test
    void faq_ucC_composesGoldenPhasePlan() {
        assertGoldenFaq(evaluator.plan(session("UC-C"), "msg", List.of()), "Replies & Messaging");
    }

    @Test
    void faq_ucD_composesGoldenPhasePlan() {
        assertGoldenFaq(evaluator.plan(session("UC-D"), "msg", List.of()), "Account & Login");
    }

    @Test
    void faq_ucE_composesGoldenPhasePlan() {
        assertGoldenFaq(evaluator.plan(session("UC-E"), "msg", List.of()), "Safety");
    }

    @Test
    void faq_ucF_composesGoldenPhasePlan() {
        assertGoldenFaq(evaluator.plan(session("UC-F"), "msg", List.of()), "Payments");
    }

    @Test
    void faq_ucFP_composesGoldenPhasePlan() {
        assertGoldenFaq(evaluator.plan(session("UC-FP"), "msg", List.of()), "Featured Payments");
    }

    // -------------------- RESOLVE-INTAKE golden strings (per-UC dynamic) --------------------

    private String intakeProcedure(String ucName, String ucId, String teamName,
                                   String requiredFields, String intakeCompleteTrigger) {
        return "You are a Gumtree customer support agent collecting intake information for "
                + ucName + ". Your role: (1) acknowledge the user's issue with empathy, "
                + "(2) ask for any missing required details, "
                + "(3) confirm the team handling this is " + teamName + " and SLA is 24-48 hours, "
                + "(4) call request_handover when intake is complete. Do not attempt to resolve "
                + "the issue yourself — you are an intake agent only. Read the projected "
                + "`intake_state.fields_remaining` array — that is the canonical list of "
                + "required fields not yet collected for " + ucId + " (canonical required set: "
                + requiredFields + "). Ask ONLY for the next field in "
                + "`intake_state.fields_remaining`; do NOT repeat questions about fields "
                + "already in `intake_state.fields_collected`. When "
                + "`intake_state.fields_remaining` is empty AND "
                + "`intake_state.intake_complete` is true, call `request_handover` with "
                + "escalation_reason='" + intakeCompleteTrigger + "' AND include the "
                + "collected values under `arguments.intake_fields` (e.g. "
                + "{\"intake_fields\": {\"field_a\": \"value_a\", ...}}). The runtime refuses "
                + "an `intake_complete_for_*` handover when any required field is missing — "
                + "it will downgrade the call and hint which fields are still needed.";
    }

    private String intakeGrounding(String teamName, String caseCreationNote) {
        return "Use fixed-script templates and standard intake questions. "
                + "Do NOT cite knowledge articles. Do NOT search the knowledge base. "
                + "Your job is to collect required information and escalate to the human "
                + teamName + " team. " + caseCreationNote;
    }

    private String intakeEscalation(String intakeCompleteTrigger) {
        return "Escalate via request_handover with reason='" + intakeCompleteTrigger + "' "
                + "once intake fields are collected. "
                + "Escalate immediately if the user explicitly requests human help.";
    }

    private static final String CASE_CREATION_NOTE =
            "A tracking case will be created automatically by the runtime when you escalate; "
                    + "you do not need to call any case-creation tool yourself.";

    private void assertGoldenIntake(PhasePlan plan, String ucId, String ucName,
                                     String teamName, String requiredFields,
                                     String intakeCompleteTrigger, String caseCreationNote) {
        assertNotNull(plan);
        assertEquals("RESOLVE", plan.phase());
        assertEquals(ucId, plan.useCase());
        assertEquals("Collect required intake details for " + ucName
                        + " and hand over to the " + teamName + " team",
                plan.objective());
        // Sprint 080 / R7 — update_intake_fields added to the RESOLVE-INTAKE
        // Skill's tools_required so the LLM can accumulate intake fields
        // across turns without triggering handover.
        assertEquals(List.of("request_handover", "update_intake_fields"), plan.allowedTools());
        assertEquals(Set.of("form_context", "customer_context"), plan.requiredContextKeys());
        assertEquals(3, plan.maxToolSteps());
        assertEquals(false, plan.allowInterimMessage());
        assertEquals(Set.of(TerminalOutcome.CLARIFICATION_NEEDED, TerminalOutcome.ESCALATE),
                plan.validTerminalOutcomes());
        assertEquals(intakeProcedure(ucName, ucId, teamName, requiredFields, intakeCompleteTrigger),
                plan.systemInstruction());
        assertEquals(intakeGrounding(teamName, caseCreationNote), plan.groundingInstruction());
        assertEquals(intakeEscalation(intakeCompleteTrigger), plan.escalationPolicy());
    }

    @Test
    void intake_ucG_composesGoldenPhasePlan() {
        // UC-G — GDPR; NOT in needsCase set, so {case_creation_note} = "".
        assertGoldenIntake(evaluator.plan(session("UC-G"), "msg", List.of()),
                "UC-G", "GDPR / Data Action", "Data Protection",
                "[registered_email, data_request_type]",
                "intake_complete_for_uc_g", "");
    }

    @Test
    void intake_ucH_composesGoldenPhasePlan() {
        // UC-H — Ad Removal Appeal; in needsCase set, so {case_creation_note}
        // is populated.
        assertGoldenIntake(evaluator.plan(session("UC-H"), "msg", List.of()),
                "UC-H", "Ad Removal Appeal", "Ad Support",
                "[ad_id_or_listing_url, registered_email, stated_reason_or_context]",
                "intake_complete_for_uc_h", CASE_CREATION_NOTE);
    }

    @Test
    void intake_ucI_composesGoldenPhasePlan() {
        // UC-I — Refund; NOT in needsCase set.
        assertGoldenIntake(evaluator.plan(session("UC-I"), "msg", List.of()),
                "UC-I", "Refund / Payment Dispute", "Payments",
                "[transaction_reference, dispute_reason]",
                "intake_complete_for_uc_i", "");
    }

    @Test
    void intake_ucJ_composesGoldenPhasePlan() {
        // UC-J — Trust & Safety; in needsCase set.
        assertGoldenIntake(evaluator.plan(session("UC-J"), "msg", List.of()),
                "UC-J", "Trust & Safety Report", "Trust & Safety",
                "[report_target, report_type, description]",
                "intake_complete_for_uc_j", CASE_CREATION_NOTE);
    }

    @Test
    void intake_ucK_composesGoldenPhasePlan() {
        // UC-K — Technical Issue; in needsCase set.
        assertGoldenIntake(evaluator.plan(session("UC-K"), "msg", List.of()),
                "UC-K", "Technical Issue Intake", "Technical Support",
                "[platform, repro_steps_or_error_message]",
                "intake_complete_for_uc_k", CASE_CREATION_NOTE);
    }

    // -------------------- legacy paths preserved unchanged --------------------

    @Test
    void resolve_unknownUc_returnsNull() {
        // No Skill applies → plan returns null (was the legacy fallback).
        assertEquals(null,
                evaluator.plan(session("UC-UNKNOWN"), "msg", List.of()));
    }

    @Test
    void resolve_nullActiveUc_returnsNull() {
        BotSession s = session(null);
        s.setActiveUseCase(null);
        assertEquals(null, evaluator.plan(s, "msg", List.of()));
    }
}
