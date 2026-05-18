package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.LlmCallEvent;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolEvent;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PhaseEvaluator#plan(BotSession, String, List)} and
 * {@link PhaseEvaluator#interpretRunResult(PhasePlan, AgentRunResult, BotSession)}
 * (D16.B). Verifies the FAQ branch produces a complete plan and
 * non-migrated phases / UCs return null (legacy fallback).
 */
@ExtendWith(MockitoExtension.class)
class PhaseEvaluatorPlanTest {

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
    }

    private BotSession session(String phase, String uc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-1");
        s.setCurrentPhase(phase);
        s.setActiveUseCase(uc);
        return s;
    }

    @Test
    void plan_resolveFaqUc_returnsFullPlan() {
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-A", "Ad Status & Visibility",
                        List.of("Ad Support"), "LOW", true, "FAQ"));

        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-A"), "AD-1002", List.of());

        assertNotNull(plan);
        assertEquals("RESOLVE", plan.phase());
        assertEquals("UC-A", plan.useCase());
        assertNotNull(plan.objective());
        assertTrue(plan.objective().contains("Ad Status"));
        assertTrue(plan.allowedTools().contains("get_customer_context"));
        assertTrue(plan.allowedTools().contains("search_knowledge"));
        assertTrue(plan.allowedTools().contains("resolve_article"));
        assertTrue(plan.allowedTools().contains("request_handover"));
        assertEquals(4, plan.maxToolSteps());
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.FINAL_ANSWER));
        assertNotNull(plan.systemInstruction());
        assertNotNull(plan.groundingInstruction());
    }

    @Test
    void plan_resolveIntakeUcH_caseCreationIsRuntimeOnly() {
        // Codex 1.8 + customer_service_tool_spec_v0_2.yaml: create_case_controlled
        // is a runtime_only tool. The LLM must NOT see it in allowedTools; the
        // runtime creates the tracking case before handover deterministically.
        when(useCaseRegistry.getUseCase("UC-H")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-H", "Ad Removal Appeal",
                        List.of("Ad Support"), "MEDIUM", false, "INTAKE"));

        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-H"), "msg", List.of());

        assertNotNull(plan);
        assertEquals("RESOLVE", plan.phase());
        assertEquals("UC-H", plan.useCase());
        assertTrue(plan.allowedTools().contains("request_handover"));
        assertFalse(plan.allowedTools().contains("create_case_controlled"),
                "UC-H must NOT expose create_case_controlled to the LLM (runtime_only tool)");
        assertFalse(plan.allowedTools().contains("search_knowledge"),
                "INTAKE plan must NOT allow search_knowledge");
        assertFalse(plan.allowedTools().contains("resolve_article"),
                "INTAKE plan must NOT allow resolve_article");
        assertFalse(plan.allowedTools().contains("get_customer_context"),
                "INTAKE plan must NOT allow get_customer_context");
        assertEquals(3, plan.maxToolSteps());
        assertNotNull(plan.systemInstruction());
        assertFalse(plan.systemInstruction().contains("create_case_controlled"),
                "Intake system instruction must not steer the LLM toward calling create_case_controlled");
        assertNotNull(plan.groundingInstruction());
        assertNotNull(plan.escalationPolicy());
    }

    @Test
    void plan_resolveIntakeUcG_returnsPlanWithoutCaseCreation() {
        when(useCaseRegistry.getUseCase("UC-G")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-G", "Data Protection",
                        List.of("Privacy"), "HIGH", false, "INTAKE"));

        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-G"), "msg", List.of());

        assertNotNull(plan);
        assertEquals(List.of("request_handover"), plan.allowedTools(),
                "UC-G must only allow request_handover (no case creation)");
        assertFalse(plan.allowedTools().contains("create_case_controlled"));
        assertFalse(plan.allowedTools().contains("search_knowledge"));
    }

    @Test
    void plan_resolveIntakeUcI_returnsPlanWithoutCaseCreation() {
        when(useCaseRegistry.getUseCase("UC-I")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-I", "Payments",
                        List.of("Payments"), "HIGH", false, "INTAKE"));

        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-I"), "msg", List.of());

        assertNotNull(plan);
        assertEquals(List.of("request_handover"), plan.allowedTools(),
                "UC-I must only allow request_handover (no case creation)");
        assertFalse(plan.allowedTools().contains("create_case_controlled"));
        assertFalse(plan.allowedTools().contains("search_knowledge"));
    }

    @Test
    void plan_resolveIntakeUcJ_caseCreationIsRuntimeOnly() {
        // Codex 1.8: case creation is runtime-only; LLM must not see it.
        when(useCaseRegistry.getUseCase("UC-J")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-J", "Trust & Safety",
                        List.of("Safety"), "HIGH", false, "INTAKE"));

        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-J"), "msg", List.of());

        assertNotNull(plan);
        assertEquals(List.of("request_handover"), plan.allowedTools(),
                "UC-J must only allow request_handover; case creation is runtime-only");
        assertFalse(plan.allowedTools().contains("create_case_controlled"));
        assertFalse(plan.allowedTools().contains("search_knowledge"));
    }

    @Test
    void plan_resolveIntakeUcK_caseCreationIsRuntimeOnly() {
        // Codex 1.8: case creation is runtime-only; LLM must not see it.
        when(useCaseRegistry.getUseCase("UC-K")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-K", "Technical Support",
                        List.of("Tech"), "MEDIUM", false, "INTAKE"));

        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-K"), "msg", List.of());

        assertNotNull(plan);
        assertEquals(List.of("request_handover"), plan.allowedTools(),
                "UC-K must only allow request_handover; case creation is runtime-only");
        assertFalse(plan.allowedTools().contains("create_case_controlled"));
        assertFalse(plan.allowedTools().contains("search_knowledge"));
    }

    @Test
    void plan_intakeTerminalOutcomes_excludeFinalAnswer() {
        when(useCaseRegistry.getUseCase("UC-H")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-H", "Ad Removal Appeal",
                        List.of("Ad Support"), "MEDIUM", false, "INTAKE"));

        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-H"), "msg", List.of());

        assertNotNull(plan);
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.ESCALATE),
                "INTAKE plan must allow ESCALATE");
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.CLARIFICATION_NEEDED),
                "INTAKE plan must allow CLARIFICATION_NEEDED");
        assertFalse(plan.validTerminalOutcomes().contains(TerminalOutcome.FINAL_ANSWER),
                "INTAKE plan must NOT allow FINAL_ANSWER (intake never produces final answers)");
    }

    @Test
    void interpretRunResult_intakeFinalAnswer_treatedAsClarification() {
        when(useCaseRegistry.getUseCase("UC-H")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-H", "Ad Removal Appeal",
                        List.of("Ad Support"), "MEDIUM", false, "INTAKE"));
        PhasePlan intakePlan = evaluator.plan(session("RESOLVE", "UC-H"), "msg", List.of());

        AgentRunResult result = AgentRunResult.finalAnswer(
                "Could you tell me which ad and what's the reason?", List.of(), List.of());
        PhaseTransitionDecision decision = evaluator.interpretRunResult(intakePlan, result, null);

        assertEquals("RESOLVE", decision.nextPhase(),
                "INTAKE FINAL_ANSWER should stay in RESOLVE (clarification, not termination)");
        assertEquals("clarification_asked", decision.transitionReason());
        assertNull(decision.escalationReason());
        assertEquals("Could you tell me which ad and what's the reason?", decision.responseText());
    }

    @Test
    void interpretRunResult_faqFinalAnswer_withoutTerminalEvidence_staysInResolve() {
        // Sprint 11.1 closure — a FAQ FINAL_ANSWER without deterministic
        // terminal evidence (no successful record_outcome dispatch on
        // this run, no CONFIRM round yet) is a same-UC subtask answer,
        // not a hard close. The session must stay in RESOLVE so the LLM
        // does not collapse a single factual answer into CONFIRM →
        // record_outcome on the same turn.
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-A", "Ad Status & Visibility",
                        List.of("Ad Support"), "LOW", true, "FAQ"));
        PhasePlan faqPlan = evaluator.plan(session("RESOLVE", "UC-A"), "msg", List.of());

        AgentRunResult result = AgentRunResult.finalAnswer(
                "Here is your answer.", List.of(), List.of());
        PhaseTransitionDecision decision = evaluator.interpretRunResult(faqPlan, result, null);

        assertEquals("RESOLVE", decision.nextPhase(),
                "Without deterministic terminal evidence (successful record_outcome), "
                        + "FAQ FINAL_ANSWER must stay in RESOLVE rather than collapse to CONFIRM.");
        assertEquals("progressive_resolve_stay", decision.transitionReason());
    }

    @Test
    void plan_discoverPhase_returnsDiscoverPlan() {
        // D16.D: DISCOVER is now migrated; plan() returns a non-null PhasePlan.
        PhasePlan plan = evaluator.plan(session("DISCOVER", "UC-A"), "msg", List.of());
        assertNotNull(plan);
        assertEquals("DISCOVER", plan.phase());
        assertEquals("UC-A", plan.useCase());
        assertNotNull(plan.objective());
        assertTrue(plan.objective().toLowerCase().contains("identify"));
        assertTrue(plan.allowedTools().contains("search_knowledge"));
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.CLARIFICATION_NEEDED));
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.FINAL_ANSWER));
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.ESCALATE));
        assertEquals(2, plan.maxToolSteps());
    }

    @Test
    void plan_discoverPhase_nullActiveUc_stillReturnsPlan() {
        // DISCOVER may have no UC committed yet — plan() must still work.
        PhasePlan plan = evaluator.plan(session("DISCOVER", null), "msg", List.of());
        assertNotNull(plan);
        assertEquals("DISCOVER", plan.phase());
        assertNull(plan.useCase());
    }

    @Test
    void plan_confirmPhase_returnsConfirmPlan() {
        PhasePlan plan = evaluator.plan(session("CONFIRM", "UC-A"), "yes", List.of());
        assertNotNull(plan);
        assertEquals("CONFIRM", plan.phase());
        assertEquals("UC-A", plan.useCase());
        assertTrue(plan.allowedTools().contains("record_outcome"));
        assertTrue(plan.allowedTools().contains("request_handover"));
        assertEquals(2, plan.maxToolSteps(),
                "CONFIRM needs 2 steps so the loop can call record_outcome then emit a closing message");
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.FINAL_ANSWER));
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.CLARIFICATION_NEEDED));
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.ESCALATE));
    }

    @Test
    void plan_closePhase_returnsClosePlan() {
        PhasePlan plan = evaluator.plan(session("CLOSE", "UC-A"), "thanks", List.of());
        assertNotNull(plan);
        assertEquals("CLOSE", plan.phase());
        assertEquals(List.of("record_outcome"), plan.allowedTools(),
                "CLOSE plan must only allow record_outcome");
        assertEquals(2, plan.maxToolSteps());
        assertEquals(Set.of(TerminalOutcome.FINAL_ANSWER), plan.validTerminalOutcomes(),
                "CLOSE plan only validly terminates with FINAL_ANSWER");
    }

    @Test
    void plan_escalatePhase_nonIntakeUc_returnsEscalatePlanWithoutCaseTool() {
        PhasePlan plan = evaluator.plan(session("ESCALATE", "UC-A"), "ok", List.of());
        assertNotNull(plan);
        assertEquals("ESCALATE", plan.phase());
        assertTrue(plan.allowedTools().contains("request_handover"));
        assertTrue(plan.allowedTools().contains("record_outcome"));
        assertFalse(plan.allowedTools().contains("create_case_controlled"),
                "ESCALATE plan for non-intake UC should not include create_case_controlled");
        assertEquals(2, plan.maxToolSteps());
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.ESCALATE));
        assertTrue(plan.validTerminalOutcomes().contains(TerminalOutcome.FINAL_ANSWER));
    }

    @Test
    void plan_escalatePhase_intakeUc_excludesCreateCaseControlled() {
        // Codex 1.8: case creation is runtime-only — even on ESCALATE for an
        // INTAKE UC the LLM must not see create_case_controlled. The runtime
        // creates the tracking case in ControlKernel.createCaseIfNeeded /
        // PhaseEvaluator.createCaseIfAllowed before request_handover fires.
        PhasePlan plan = evaluator.plan(session("ESCALATE", "UC-H"), "ok", List.of());
        assertNotNull(plan);
        assertEquals("ESCALATE", plan.phase());
        assertTrue(plan.allowedTools().contains("request_handover"));
        assertTrue(plan.allowedTools().contains("record_outcome"));
        assertFalse(plan.allowedTools().contains("create_case_controlled"),
                "ESCALATE plan must never expose create_case_controlled to the LLM (runtime_only)");
    }

    @Test
    void plan_nullActiveUc_returnsNull() {
        PhasePlan plan = evaluator.plan(session("RESOLVE", null), "msg", List.of());
        assertNull(plan);
    }

    @Test
    void plan_unknownUcInRegistry_returnsNull() {
        // Sprint 39 — post-migration, plan() no longer calls
        // useCaseRegistry.getUseCase() directly for the RESOLVE branch
        // (composition flows through SkillRegistry.select). UC-X is not
        // in either RESOLVE Skill's applicable_use_cases, so the registry
        // returns Optional.empty() and plan returns null.
        PhasePlan plan = evaluator.plan(session("RESOLVE", "UC-X"), "msg", List.of());
        assertNull(plan);
    }

    @Test
    void interpretRunResult_finalAnswer_transitionsToConfirm() {
        AgentRunResult result = AgentRunResult.finalAnswer(
                "Here is your answer.", List.of(), List.of());
        PhaseTransitionDecision decision = evaluator.interpretRunResult(null, result, null);

        assertEquals("CONFIRM", decision.nextPhase());
        assertEquals("Here is your answer.", decision.responseText());
        assertNull(decision.escalationReason());
        assertEquals("answer_provided", decision.transitionReason());
    }

    @Test
    void interpretRunResult_escalate_transitionsToEscalate() {
        AgentRunResult result = AgentRunResult.escalate(
                "user_distress", List.of(), List.of());
        PhaseTransitionDecision decision = evaluator.interpretRunResult(null, result, null);

        assertEquals("ESCALATE", decision.nextPhase());
        assertEquals("user_distress", decision.escalationReason());
    }

    @Test
    void interpretRunResult_maxSteps_transitionsToEscalate() {
        AgentRunResult result = AgentRunResult.maxSteps(List.of(), List.of());
        PhaseTransitionDecision decision = evaluator.interpretRunResult(null, result, null);

        assertEquals("ESCALATE", decision.nextPhase());
        // Round-1 Step 3 cleanup: MAX_STEPS now emits the canonical
        // `turn_budget_exhausted` escalation reason (was legacy
        // `agent_max_steps_exceeded`).
        assertEquals("turn_budget_exhausted", decision.escalationReason());
        assertEquals("max_steps_exceeded", decision.transitionReason());
    }

    @Test
    void interpretRunResult_error_transitionsToEscalate() {
        // Step 3b: ERROR only escalates on the SECOND consecutive runtime
        // error within the same phase; the first ERROR stays in-phase with
        // a retry message. To exercise the escalation branch we seed the
        // session with a prior error count of 1 so this ERROR triggers the
        // `runtime_error_threshold` path.
        BotSession sess = session("RESOLVE", "UC-A");
        sess.setRuntimeErrorCount(1);
        AgentRunResult result = AgentRunResult.error("boom");
        PhaseTransitionDecision decision = evaluator.interpretRunResult(null, result, sess);

        assertEquals("ESCALATE", decision.nextPhase());
        assertEquals("runtime_error_threshold", decision.escalationReason());
        assertEquals("agent_error", decision.transitionReason());
    }

    @Test
    void interpretRunResult_nullResult_safeEscalation() {
        PhaseTransitionDecision decision = evaluator.interpretRunResult(null, null, null);
        assertEquals("ESCALATE", decision.nextPhase());
    }

    // ── D16.D — interpretRunResult phase-aware mapping ──────────────────────

    @Test
    void interpretRunResult_discover_finalAnswerWithUc_transitionsToResolve() {
        PhasePlan discoverPlan = evaluator.plan(session("DISCOVER", null), "msg", List.of());
        BotSession sessionWithUc = session("DISCOVER", "UC-A");
        AgentRunResult result = AgentRunResult.finalAnswer(
                "Got it — sounds like an Ad question.", List.of(), List.of());
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(discoverPlan, result, sessionWithUc);

        assertEquals("RESOLVE", decision.nextPhase(),
                "DISCOVER FINAL_ANSWER with active UC should transition to RESOLVE");
        assertEquals("uc_identified", decision.transitionReason());
    }

    @Test
    void interpretRunResult_discover_finalAnswerWithoutUc_staysInDiscover() {
        PhasePlan discoverPlan = evaluator.plan(session("DISCOVER", null), "msg", List.of());
        BotSession sessionWithoutUc = session("DISCOVER", null);
        AgentRunResult result = AgentRunResult.finalAnswer(
                "Could you tell me a bit more?", List.of(), List.of());
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(discoverPlan, result, sessionWithoutUc);

        assertEquals("DISCOVER", decision.nextPhase(),
                "DISCOVER FINAL_ANSWER without UC should stay in DISCOVER (clarification)");
        assertEquals("clarification_asked", decision.transitionReason());
    }

    @Test
    void interpretRunResult_discover_clarificationNeeded_staysInDiscover() {
        PhasePlan discoverPlan = evaluator.plan(session("DISCOVER", null), "msg", List.of());
        AgentRunResult result = AgentRunResult.finalAnswer(
                "Which type of issue are you having?", List.of(), List.of());
        // Use a real CLARIFICATION_NEEDED outcome by constructing AgentRunResult manually.
        // Simpler: rely on FINAL_ANSWER without UC (covered above) and verify CLARIFICATION_NEEDED
        // path explicitly via the plan's outcome.
        AgentRunResult clarification = new AgentRunResult(
                List.of(),
                List.of(),
                List.of(),
                TerminalOutcome.CLARIFICATION_NEEDED,
                "Which type of issue are you having?",
                Optional.empty(),
                null,
                null);
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(discoverPlan, clarification, session("DISCOVER", null));
        assertEquals("DISCOVER", decision.nextPhase(),
                "CLARIFICATION_NEEDED in DISCOVER should keep the session in DISCOVER");
        assertEquals("clarification_asked", decision.transitionReason());
    }

    @Test
    void interpretRunResult_confirm_finalAnswer_transitionsToClose() {
        PhasePlan confirmPlan = evaluator.plan(session("CONFIRM", "UC-A"), "thanks", List.of());
        AgentRunResult result = AgentRunResult.finalAnswer(
                "You're welcome!", List.of(), List.of());
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(confirmPlan, result, session("CONFIRM", "UC-A"));

        assertEquals("CLOSE", decision.nextPhase(),
                "CONFIRM FINAL_ANSWER (user satisfied) should transition to CLOSE");
        assertEquals("user_satisfied", decision.transitionReason());
    }

    @Test
    void interpretRunResult_confirm_clarificationNeeded_staysInConfirm() {
        PhasePlan confirmPlan = evaluator.plan(session("CONFIRM", "UC-A"), "?", List.of());
        AgentRunResult clarification = new AgentRunResult(
                List.of(),
                List.of(),
                List.of(),
                TerminalOutcome.CLARIFICATION_NEEDED,
                "Did that answer your question?",
                Optional.empty(),
                null,
                null);
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(confirmPlan, clarification, session("CONFIRM", "UC-A"));

        assertEquals("CONFIRM", decision.nextPhase(),
                "CLARIFICATION_NEEDED in CONFIRM should keep session in CONFIRM");
    }

    @Test
    void interpretRunResult_confirm_escalate_transitionsToEscalate() {
        PhasePlan confirmPlan = evaluator.plan(session("CONFIRM", "UC-A"), "no", List.of());
        AgentRunResult result = AgentRunResult.escalate(
                "user_dissatisfied", List.of(), List.of());
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(confirmPlan, result, session("CONFIRM", "UC-A"));

        assertEquals("ESCALATE", decision.nextPhase());
        // `user_dissatisfied` is NOT in the canonical 23-value
        // escalation_reason enum, so PhaseEvaluator.canonicalize() maps it to
        // the catch-all `service_degraded`. (This preserves the L1 trace
        // contract; CONFIRM-phase user dissatisfaction surfaces as
        // service_degraded rather than a non-canonical literal.)
        assertEquals("service_degraded", decision.escalationReason());
    }

    @Test
    void interpretRunResult_close_finalAnswer_staysInClose() {
        PhasePlan closePlan = evaluator.plan(session("CLOSE", "UC-A"), "bye", List.of());
        AgentRunResult result = AgentRunResult.finalAnswer(
                "Goodbye, have a great day!", List.of(), List.of());
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(closePlan, result, session("CLOSE", "UC-A"));

        assertEquals("CLOSE", decision.nextPhase());
        assertEquals("session_closed", decision.transitionReason());
    }

    @Test
    void interpretRunResult_escalate_finalAnswer_staysInEscalate() {
        PhasePlan escalatePlan = evaluator.plan(session("ESCALATE", "UC-A"), "ok", List.of());
        AgentRunResult result = AgentRunResult.finalAnswer(
                "I've passed this to a specialist.", List.of(), List.of());
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(escalatePlan, result, session("ESCALATE", "UC-A"));

        assertEquals("ESCALATE", decision.nextPhase());
        assertEquals("handover_completed", decision.transitionReason());
    }

    @Test
    void interpretRunResult_escalate_escalateOutcome_transitionsToEscalate() {
        PhasePlan escalatePlan = evaluator.plan(session("ESCALATE", "UC-H"), "ok", List.of());
        AgentRunResult result = AgentRunResult.escalate(
                "intake_complete_for_uc_h", List.of(), List.of());
        PhaseTransitionDecision decision =
                evaluator.interpretRunResult(escalatePlan, result, session("ESCALATE", "UC-H"));

        assertEquals("ESCALATE", decision.nextPhase());
        assertEquals("intake_complete_for_uc_h", decision.escalationReason());
    }
}
