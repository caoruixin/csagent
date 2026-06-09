package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.ResolveDisposition;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.runtime.skill.DispatchContext;
import com.gumtree.csagent.service.runtime.skill.RejectVerdict;
import com.gumtree.csagent.service.runtime.skill.SkillGuardrailDispatcher;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 11 — progressive same-UC resolve regression suite.
 *
 * <p>Pins the §M0 minimal task / entity state, the §M1
 * {@link ResolveDisposition} runtime checkpoint + RESOLVE → CONFIRM
 * transition guard, and the {@code record_outcome(resolve)} guard
 * against premature single-factual-answer hard confirms. Sprint 10
 * reroute tests remain green; the Sprint 10 §L2 projection slots
 * are still emitted alongside the Sprint 11 §M0 additions.
 *
 * <p>Test groups:
 * <ol>
 *   <li>Progressive UC-A flow — multi-turn:
 *       "How do I find my ad?" → soft answer (stays RESOLVE) →
 *       "Why can't I find my ad?" → ad_id ask →
 *       user provides ad_id → primary_entity reused →
 *       "How long is it active for?" → stays UC-A / RESOLVE →
 *       user asks for human → ESCALATE / user_requested.</li>
 *   <li>UC-C same-UC follow-up — UC-C messaging context with a same-UC
 *       follow-up stays UC-C / RESOLVE.</li>
 *   <li>Record-outcome guard — record_outcome(resolve) is rejected
 *       after a single factual answer; allowed in CONFIRM / CLOSE.</li>
 *   <li>Projection snapshot — current_task_type, task_status,
 *       primary_entity, issue_status_summary visible alongside the
 *       Sprint 10 slots.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class Sprint11ProgressiveResolveTest {

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private PhaseEvaluator phaseEvaluator;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private com.gumtree.csagent.service.runtime.AgentRunLoop agentRunLoop;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EscalationReasonResolver resolver = new EscalationReasonResolver();
    private final RuntimeIntentClassifier classifier =
            new RuntimeIntentClassifier(resolver, objectMapper);
    private final RerouteDecider decider = new RerouteDecider();

    private ControlKernel kernel;

    @BeforeEach
    void setUp() {
        lenient().when(controlPolicy.isValidTransition(anyString(), anyString())).thenAnswer(inv -> {
            String from = inv.getArgument(0);
            String to = inv.getArgument(1);
            return switch (from) {
                case "CONFIRM" -> "RESOLVE".equals(to) || "DISCOVER".equals(to)
                        || "ESCALATE".equals(to) || "CLOSE".equals(to);
                case "RESOLVE" -> "CONFIRM".equals(to) || "ESCALATE".equals(to);
                case "DISCOVER" -> "RESOLVE".equals(to) || "ESCALATE".equals(to);
                default -> false;
            };
        });
        kernel = new ControlKernel(
                turnRepository, eventRepository, budgetChecker, driftDetector,
                phaseEvaluator, controlPolicy, objectMapper, createCaseTool,
                eventEmitter, contextProjectionBuilder, new com.gumtree.csagent.config.AgentRunLoopProperties(),
                agentRunLoop, resolver, classifier, decider);
    }

    private BotSession sessionWithoutAd() {
        BotSession s = new BotSession();
        s.setSessionId("sess-progressive");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-A");
        s.setFormTopicSubject("Ad Support");
        s.setFormContext("{\"description\":\"can't find my ad\"}");
        return s;
    }

    private BotSession sessionWithAd(String adId) {
        BotSession s = new BotSession();
        s.setSessionId("sess-progressive");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-A");
        s.setFormTopicSubject("Ad Support");
        s.setFormContext("{\"ad_id\":\"" + adId + "\",\"description\":\"can't find my ad\"}");
        return s;
    }

    private DriftResult noDrift() {
        return DriftResult.builder().type(DriftResult.DriftType.NONE).build();
    }

    private static PhasePlan faqResolvePlan(String uc) {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase(uc)
                .objective("test")
                .allowedTools(List.of(
                        "search_knowledge", "resolve_article",
                        "record_outcome", "request_handover"))
                .maxToolSteps(4)
                .validTerminalOutcomes(Set.of(
                        TerminalOutcome.FINAL_ANSWER,
                        TerminalOutcome.CLARIFICATION_NEEDED,
                        TerminalOutcome.ESCALATE))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Test group 1 — Progressive UC-A flow
    // ─────────────────────────────────────────────────────────────

    @Test
    void test1a_progressiveUcA_softAnswer_staysResolve_noHardConfirm() {
        // Turn 1: "How do I find my ad?" — Sprint 10 MVP shapes do not
        // match. The kernel should leave UC-A / RESOLVE untouched and
        // not collapse to CONFIRM via FINAL_ANSWER.
        BotSession s = sessionWithoutAd();
        kernel.applyRerouteDecision(s, "How do I find my ad?", noDrift(), "RESOLVE");
        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Soft progressive UC-A query must remain in RESOLVE.");

        // Now simulate a soft FAQ-grounded answer that asks for the
        // ad_id ("send the advert ID if you want me to check it"). The
        // ResolveDispositionEvaluator must classify it as
        // ASKED_FOR_SLOT, so the planner stays in RESOLVE.
        AgentRunResult softAnswer = AgentRunResult.finalAnswer(
                "Here is how to find your ad in My Ads. If you want me to check, "
                        + "send the advert ID and I will look it up.",
                List.of(), List.of(), null, null);
        ResolveDisposition disposition = ResolveDispositionEvaluator.evaluate(
                faqResolvePlan("UC-A"), softAnswer);
        assertEquals(ResolveDisposition.ASKED_FOR_SLOT, disposition,
                "A soft next-step answer asking for the advert ID must remain in RESOLVE.");
    }

    @Test
    void test1b_progressiveUcA_userProvidesAdId_primaryEntityReused() {
        // Turn after the bot asked for the advert ID; the user replies
        // with an unadorned numeric ad_id. The classifier MVP shapes
        // do not fire (no "still can't see" / "how long" wording), so
        // CONTINUE_CURRENT runs. The Sprint 11 §M0 same-UC ad_id
        // capture must still stamp primary_entity.
        BotSession s = sessionWithoutAd();
        kernel.applyRerouteDecision(s, "1234567890", noDrift(), "RESOLVE");
        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("listing", s.getPrimaryEntityType(),
                "Same-UC ad_id capture must stamp primary_entity_type=listing.");
        assertEquals("1234567890", s.getPrimaryEntityValue(),
                "Same-UC ad_id capture must persist the user-provided ad_id.");
        assertEquals("user_message.ad_id", s.getLastEntityContextRef(),
                "last_entity_context_ref must surface the capture origin.");
        // The form_context must now carry the ad_id so subsequent turns
        // and FAQ tools see it without the runtime needing to re-capture.
        assertTrue(s.getFormContext().contains("\"ad_id\":\"1234567890\""),
                "Captured ad_id must be persisted into form_context for cross-turn reuse.");
    }

    @Test
    void test1c_progressiveUcA_followupQuestion_reusesEntity_staysUcA() {
        // After the previous turn captured the ad_id, the user asks
        // "How long is it active for?". The Sprint 10 MVP follow-up
        // pattern matches; the classifier returns SAME_UC_NEW_TASK,
        // and the form-context fast path provides the ad_id.
        BotSession s = sessionWithAd("1234567890");
        kernel.applyRerouteDecision(s, "How long is it active for?", noDrift(), "RESOLVE");
        assertEquals("UC-A", s.getActiveUseCase(),
                "Same-UC follow-up must stay UC-A (no soft shift).");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Already in RESOLVE; no terminal transition.");
        assertEquals("listing_lifecycle_followup", s.getCurrentTaskType());
        assertEquals("listing", s.getPrimaryEntityType());
        assertEquals("1234567890", s.getPrimaryEntityValue(),
                "Primary entity must be reused from form_context across same-UC follow-ups.");
        assertEquals("SAME_UC_NEW_TASK", s.getDriftType());
    }

    @Test
    void test1d_progressiveUcA_userAsksForHuman_escalatesUserRequested() {
        // Explicit human-help request is precedence-handled by the
        // EscalationReasonResolver. The classifier surfaces
        // HUMAN_REQUEST; the kernel's step 2.5 is what actually
        // escalates in production. We pin the classifier output here
        // so the trace observability remains consistent.
        BotSession s = sessionWithAd("1234567890");
        var classification = classifier.classify(s, "I want a human", noDrift());
        assertEquals(
                com.gumtree.csagent.model.IntentClassification.IntentRelation.HUMAN_REQUEST,
                classification.relation());
        assertTrue(resolver.detectExplicitUserEscalation("I want a human"),
                "Resolver must continue to flag explicit human-help so kernel step 2.5 fires "
                        + "user_requested before the planner runs.");
    }

    // ─────────────────────────────────────────────────────────────
    // Test group 2 — UC-C same-UC follow-up
    // ─────────────────────────────────────────────────────────────

    @Test
    void test2_ucC_sameUcFollowup_staysUcC_noSoftShift_noHandover() {
        // Session is already in UC-C (messaging / replies); the user
        // asks a same-UC follow-up like "I haven't got replies". The
        // classifier returns SAME_ISSUE for an active UC-C session.
        BotSession s = new BotSession();
        s.setSessionId("sess-uc-c");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-C");
        s.setFormTopicSubject("Replies");
        s.setFormContext("{}");

        kernel.applyRerouteDecision(s, "I haven't got replies", noDrift(), "RESOLVE");

        assertEquals("UC-C", s.getActiveUseCase(),
                "Same-UC UC-C follow-up must stay UC-C.");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Already in RESOLVE; no terminal transition.");
        assertNotEquals("ESCALATE", s.getCurrentPhase(),
                "Same-UC UC-C follow-up must not escalate or generate a generic handover.");
    }

    // ─────────────────────────────────────────────────────────────
    // Test group 3 — Record-outcome guard
    // ─────────────────────────────────────────────────────────────

    @Test
    void test3a_recordOutcomeGuard_rejectsResolveAfterSingleFactualAnswer() {
        // RESOLVE / FAQ plan + record_outcome(outcome_class=resolve) on
        // the very first factual answer, with no CONFIRM round and no
        // CLOSE phase. The Sprint 11 guard must reject it.
        PhasePlan plan = faqResolvePlan("UC-A");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("outcome_class", "resolve");
        ToolCall call = ToolCall.builder().name("record_outcome").arguments(args).build();

        boolean reject = dispatcherRejectsPrematureResolve(
                plan, sessionWithAd("1234567890"), call);
        assertTrue(reject,
                "record_outcome(resolve) on a RESOLVE plan must be rejected when the "
                        + "user has not confirmed and the session is not in CLOSE.");
    }

    @Test
    void test3b_recordOutcomeGuard_allowsResolveInConfirmPhase() {
        // CONFIRM phase: the user has accepted the answer; record_outcome
        // is the canonical close-out tool.
        PhasePlan plan = PhasePlan.builder()
                .phase("CONFIRM")
                .useCase("UC-A")
                .objective("confirm")
                .allowedTools(List.of("record_outcome", "request_handover"))
                .maxToolSteps(2)
                .build();
        BotSession s = sessionWithAd("1234567890");
        s.setCurrentPhase("CONFIRM");

        Map<String, Object> args = new LinkedHashMap<>();
        args.put("outcome_class", "resolve");
        ToolCall call = ToolCall.builder().name("record_outcome").arguments(args).build();

        boolean reject = dispatcherRejectsPrematureResolve(plan, s, call);
        assertFalse(reject,
                "record_outcome(resolve) on a CONFIRM plan must pass through the guard.");
    }

    @Test
    void test3c_recordOutcomeGuard_allowsResolveAfterDeterministicTerminalCondition() {
        // Deterministic terminal condition met: the LLM successfully
        // dispatched record_outcome on this run. ResolveDispositionEvaluator
        // must report READY_TO_CONFIRM so PhaseEvaluator transitions to
        // CONFIRM (not staying in RESOLVE).
        PhasePlan plan = faqResolvePlan("UC-A");
        ToolEvent successfulRecord = new ToolEvent(
                0, 0, "record_outcome",
                Map.of("outcome_class", "resolve"),
                true, Map.of("ok", true), null, 5L);
        AgentRunResult result = AgentRunResult.finalAnswer(
                "All set. Glad I could help.",
                List.of(), List.of(successfulRecord), null, null);

        ResolveDisposition disposition = ResolveDispositionEvaluator.evaluate(plan, result);
        assertEquals(ResolveDisposition.READY_TO_CONFIRM, disposition,
                "A successful record_outcome dispatch on this run is the deterministic "
                        + "terminal condition that earns RESOLVE → CONFIRM.");
    }

    @Test
    void test3d_recordOutcomeGuard_softAnswerStaysInResolve() {
        // The bot delivered a soft answer ("send the advert ID") with
        // no record_outcome dispatch — must stay in RESOLVE.
        PhasePlan plan = faqResolvePlan("UC-A");
        AgentRunResult softAnswer = AgentRunResult.finalAnswer(
                "Here is how to find your ad. Send the advert ID if you want me to check it.",
                List.of(), List.of(), null, null);
        ResolveDisposition disposition = ResolveDispositionEvaluator.evaluate(plan, softAnswer);
        assertNotEquals(ResolveDisposition.READY_TO_CONFIRM, disposition,
                "A soft next-step answer must not earn READY_TO_CONFIRM.");
        assertTrue(
                disposition == ResolveDisposition.ASKED_FOR_SLOT
                        || disposition == ResolveDisposition.ANSWERED_SUBTASK,
                "A soft answer maps to ASKED_FOR_SLOT or ANSWERED_SUBTASK; got " + disposition);
    }

    @Test
    void test3e_listingStatusAnswer_withoutTerminalEvidence_isAnsweredSubtask() {
        // Sprint 11.1 closure regression — UC-A same-UC progressive
        // resolve. The bot delivers a non-question, non-slot grounded
        // listing expiry / status answer ("Your advert is active for
        // 30 days from posting."). No successful record_outcome on
        // this run, no CONFIRM round yet. The disposition must be
        // ANSWERED_SUBTASK (stay in RESOLVE) — NOT READY_TO_CONFIRM —
        // so a single factual answer does not collapse into
        // CONFIRM → record_outcome on the same turn.
        PhasePlan plan = faqResolvePlan("UC-A");
        AgentRunResult listingStatusAnswer = AgentRunResult.finalAnswer(
                "Your advert is active for 30 days from posting.",
                List.of(), List.of(), null, null);
        ResolveDisposition disposition =
                ResolveDispositionEvaluator.evaluate(plan, listingStatusAnswer);
        assertEquals(ResolveDisposition.ANSWERED_SUBTASK, disposition,
                "A non-question listing expiry/status answer without successful "
                        + "record_outcome must map to ANSWERED_SUBTASK so the session "
                        + "stays in RESOLVE.");
    }

    @Test
    void test3f_listingStatusAnswer_withoutTerminalEvidence_phaseStaysResolve() {
        // PhaseEvaluator.interpretRunResult wiring regression for the
        // Sprint 11.1 closure. Same listing expiry / status answer fed
        // through the real PhaseEvaluator must keep the session in
        // RESOLVE with task_status=answered_subtask.
        PhasePlan plan = faqResolvePlan("UC-A");
        AgentRunResult listingStatusAnswer = AgentRunResult.finalAnswer(
                "Your advert is active for 30 days from posting.",
                List.of(), List.of(), null, null);

        PhaseEvaluator real = new PhaseEvaluator(
                org.mockito.Mockito.mock(UseCaseRegistryService.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.knowledge.KnowledgeSearchService.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.guardrails.ScriptLibraryService.class),
                org.mockito.Mockito.mock(LlmInvocationService.class),
                org.mockito.Mockito.mock(ContextProjectionBuilder.class),
                org.mockito.Mockito.mock(ActionParser.class),
                objectMapper,
                org.mockito.Mockito.mock(com.gumtree.csagent.service.tools.CreateCaseControlledTool.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.observability.EventEmitter.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.tools.ToolDispatcher.class),
                com.gumtree.csagent.service.runtime.skill.SkillTestFixtures.productionRegistry(), null);
        BotSession s = sessionWithAd("1234567890");
        PhaseTransitionDecision decision =
                real.interpretRunResult(plan, listingStatusAnswer, s);
        assertEquals("RESOLVE", decision.nextPhase(),
                "Listing expiry/status answer without terminal evidence must keep "
                        + "the session in RESOLVE.");
        assertEquals("progressive_resolve_stay", decision.transitionReason(),
                "Transition reason must reflect progressive-resolve stay, not the "
                        + "answer_provided hard-confirm contract.");
        assertEquals("answered_subtask", s.getTaskStatus(),
                "Sprint 11 §M0 task_status must surface answered_subtask.");
    }

    @Test
    void test3g_terminalAnswerText_withoutSuccessfulRecordOutcome_doesNotEarnReadyToConfirm() {
        // Sprint 11.1 strengthening of test3c — the same closing-style
        // text ("All set. Glad I could help.") WITHOUT a successful
        // record_outcome tool event MUST NOT return READY_TO_CONFIRM.
        // Only deterministic terminal evidence earns CONFIRM.
        PhasePlan plan = faqResolvePlan("UC-A");
        AgentRunResult resultNoEvidence = AgentRunResult.finalAnswer(
                "All set. Glad I could help.",
                List.of(), List.of(), null, null);
        ResolveDisposition disposition =
                ResolveDispositionEvaluator.evaluate(plan, resultNoEvidence);
        assertNotEquals(ResolveDisposition.READY_TO_CONFIRM, disposition,
                "Without a successful record_outcome dispatch on this run, even "
                        + "closing-style text must not earn READY_TO_CONFIRM.");
        assertEquals(ResolveDisposition.ANSWERED_SUBTASK, disposition,
                "Terminal-style text without terminal evidence must default to "
                        + "ANSWERED_SUBTASK so the session stays in RESOLVE.");
    }

    // ─────────────────────────────────────────────────────────────
    // Test group 4 — Projection snapshot
    // ─────────────────────────────────────────────────────────────

    @Test
    void test4_projectionSnapshot_exposesSprint11FieldsAlongsideSprint10() {
        BotSession s = sessionWithAd("1234567890");
        kernel.applyRerouteDecision(s, "How long is it active for?", noDrift(), "RESOLVE");
        s.setTaskStatus("answered_subtask");

        UseCaseRegistryService useCaseRegistry =
                org.mockito.Mockito.mock(UseCaseRegistryService.class);
        ControlPolicyService controlPolicyMock =
                org.mockito.Mockito.mock(ControlPolicyService.class);
        com.gumtree.csagent.service.tools.ToolPolicyEnforcer toolPolicyEnforcer =
                org.mockito.Mockito.mock(com.gumtree.csagent.service.tools.ToolPolicyEnforcer.class);
        UseCaseRegistryService.UseCaseDefinition ucDef =
                new UseCaseRegistryService.UseCaseDefinition(
                        "UC-A", "Ad Status & Visibility", java.util.List.of("Ad Support"),
                        "LOW", true, "FAQ");
        when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucDef);
        lenient().when(controlPolicyMock.getMaxBotTurnsFaq()).thenReturn(15);
        lenient().when(controlPolicyMock.getMaxClarificationRounds()).thenReturn(2);
        lenient().when(controlPolicyMock.getMaxFaqMiss()).thenReturn(2);
        when(toolPolicyEnforcer.getVisibleToolsForUc("UC-A"))
                .thenReturn(java.util.List.of("search_knowledge", "resolve_article"));

        ContextProjectionBuilder realBuilder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicyMock, toolPolicyEnforcer, null);
        realBuilder.initToolSchemas();

        String json = realBuilder.buildProjection(s, java.util.List.of(), null,
                "How long is it active for?");

        // Sprint 10 §L2 fields remain intact:
        assertTrue(json.contains("\"previous_active_use_case\""),
                "Sprint 10 previous_active_use_case must remain in the projection.");
        assertTrue(json.contains("\"drift_type\":\"SAME_UC_NEW_TASK\""),
                "Sprint 10 drift_type must remain in the projection.");
        assertTrue(json.contains("\"current_task_type\":\"listing_lifecycle_followup\""),
                "current_task_type must surface the same-UC follow-up task.");
        assertTrue(json.contains("\"primary_entity\""),
                "primary_entity must be present.");
        assertTrue(json.contains("\"ad_id\":\"1234567890\""),
                "primary_entity must carry the reused ad_id.");
        assertTrue(json.contains("\"issue_status_summary\":\"open\""),
                "issue_status_summary must remain in the projection.");

        // Sprint 11 §M0 additions:
        assertTrue(json.contains("\"task_status\":\"answered_subtask\""),
                "Sprint 11 task_status must surface the progressive resolve checkpoint.");
        assertTrue(json.contains("\"last_entity_context_ref\""),
                "Sprint 11 last_entity_context_ref must be emitted "
                        + "(may be form_context.ad_id or user_message.ad_id).");
    }

    // ─────────────────────────────────────────────────────────────
    // Test group 5 — PhaseEvaluator RESOLVE → ? wiring (regression)
    // ─────────────────────────────────────────────────────────────

    @Test
    void test5_phaseEvaluator_softFinalAnswer_doesNotHardConfirm() {
        // PhaseEvaluator.mapFinalAnswer for RESOLVE / FAQ must consult
        // the ResolveDisposition evaluator. A soft "send the advert
        // ID" answer must stay in RESOLVE, not transition to CONFIRM.
        PhasePlan plan = faqResolvePlan("UC-A");
        AgentRunResult softAnswer = AgentRunResult.finalAnswer(
                "Here is how to find your ad. Send the advert ID if you want me to check it.",
                List.of(), List.of(), null, null);

        // We exercise interpretRunResult through a minimal real
        // PhaseEvaluator with mocked dependencies — the only branch
        // we need is mapFinalAnswer(RESOLVE/FAQ).
        PhaseEvaluator real = new PhaseEvaluator(
                org.mockito.Mockito.mock(UseCaseRegistryService.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.knowledge.KnowledgeSearchService.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.guardrails.ScriptLibraryService.class),
                org.mockito.Mockito.mock(LlmInvocationService.class),
                org.mockito.Mockito.mock(ContextProjectionBuilder.class),
                org.mockito.Mockito.mock(ActionParser.class),
                objectMapper,
                org.mockito.Mockito.mock(com.gumtree.csagent.service.tools.CreateCaseControlledTool.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.observability.EventEmitter.class),
                org.mockito.Mockito.mock(com.gumtree.csagent.service.tools.ToolDispatcher.class),
                SkillTestFixtures.productionRegistry(), null);
        BotSession s = sessionWithAd("1234567890");
        PhaseTransitionDecision decision = real.interpretRunResult(plan, softAnswer, s);
        assertEquals("RESOLVE", decision.nextPhase(),
                "A soft final answer must keep the session in RESOLVE; "
                        + "do not collapse to CONFIRM and short-circuit the close.");
        assertNotNull(s.getTaskStatus(),
                "Sprint 11 §M0 — task_status must be populated by the disposition evaluator.");
    }

    // ─────────────────────────────────────────────────────────────
    // Sprint 39 dispatcher helper — Sprint 11 §M1's
    // shouldRejectPrematureResolveOutcome predicate now lives inside the
    // SkillGuardrailDispatcher's premature_resolve_outcome_guard handler
    // (Sprint 37 freeze §8.2.3). The handler still delegates to the
    // untouched ResolveDispositionEvaluator frozen surface per
    // runtime_freeze_and_risk_policy.md §1.1 #3; only the invocation
    // site moves.
    // ─────────────────────────────────────────────────────────────
    private boolean dispatcherRejectsPrematureResolve(PhasePlan plan,
                                                       BotSession session,
                                                       ToolCall call) {
        SkillGuardrailDispatcher dispatcher = SkillTestFixtures.productionDispatcher();
        Object rawOutcome = call.getArguments() == null
                ? null : call.getArguments().get("outcome_class");
        if (rawOutcome == null && call.getArguments() != null) {
            rawOutcome = call.getArguments().get("outcome");
        }
        String outcomeClass = rawOutcome == null ? null : rawOutcome.toString();
        DispatchContext ctx = new DispatchContext(
                plan, session, Map.of(), null, Optional.empty());
        Optional<RejectVerdict> verdict =
                dispatcher.checkBeforeOutcomePersist(plan, outcomeClass, ctx);
        if (verdict.isEmpty()) return false;
        assertEquals(SkillGuardrailDispatcher.PROGRESSIVE_RESOLVE_REJECT_REASON,
                verdict.get().predicateName(),
                "premature_resolve_outcome_guard must use the Sprint 11 §M1 "
                        + "canonical reject-reason label");
        return true;
    }
}
