package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.IntentClassification;
import com.gumtree.csagent.model.IntentClassification.IntentRelation;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.RerouteDecision.RerouteAction;
import com.gumtree.csagent.model.ResolveDisposition;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolCall;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import com.gumtree.csagent.service.tools.ToolPolicyEnforcer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sprint 12 §N1 — targeted runtime alignment validation suite.
 *
 * <p>Consolidates the ten Sprint 12 spec scenarios into deterministic Java
 * regressions over the Sprint 10 reroute layer + Sprint 11 progressive
 * resolve layer (no live LLM dependence). Each test pins one scenario
 * end-to-end through {@link RuntimeIntentClassifier} +
 * {@link RerouteDecider} + {@code ControlKernel.applyRerouteDecision} +
 * {@link ResolveDispositionEvaluator} + the §M1 record-outcome guard, and
 * additionally checks the Sprint 12 §N0 observability fields surfaced
 * onto {@link BotSession} (predicted_use_case, intent_relation,
 * reroute_action, phase_transition_reason, resolve_disposition,
 * record_outcome_guard_result, terminal_evidence) so a reviewer reading
 * the trace evidence can answer the audit questions enumerated in the
 * objective.
 *
 * <p>This suite intentionally does NOT expand smoke / anchor / promotion
 * hard gates and does NOT touch CaseSpecs. It mirrors the deterministic
 * Sprint 10 + Sprint 11 fixtures but focuses on cross-cutting
 * Sprint 12 contracts.
 *
 * <p>Scenarios:
 * <ol>
 *   <li>UC-A → UC-C soft shift on "I haven't got replies".</li>
 *   <li>UC-A same-issue dissatisfaction on "I still can't see my ad".</li>
 *   <li>UC-A same-UC follow-up on "how long is it active for?".</li>
 *   <li>UC-A progressive listing diagnostic — multi-turn entity reuse and
 *       final {@code user_requested} escalation.</li>
 *   <li>UC-C messaging follow-up stays UC-C / RESOLVE.</li>
 *   <li>UC-J risk shift on "I was scammed".</li>
 *   <li>Explicit human request → {@code user_requested}.</li>
 *   <li>Payment ambiguity negative guard — must not blindly route UC-I.</li>
 *   <li>{@code record_outcome(resolve)} guard rejects premature close.</li>
 *   <li>{@code ResolveDisposition} terminal-evidence contract — non-slot
 *       UC-A same-UC answer without terminal evidence stays RESOLVE;
 *       valid terminal evidence may lead to READY_TO_CONFIRM.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class Sprint12RuntimeAlignmentValidationTest {

    @Mock private BotTurnRepository turnRepository;
    @Mock private BotEventRepository eventRepository;
    @Mock private BudgetChecker budgetChecker;
    @Mock private DriftDetector driftDetector;
    @Mock private PhaseEvaluator phaseEvaluator;
    @Mock private ControlPolicyService controlPolicy;
    @Mock private CreateCaseControlledTool createCaseTool;
    @Mock private EventEmitter eventEmitter;
    @Mock private ContextProjectionBuilder contextProjectionBuilder;
    @Mock private AgentRunLoop agentRunLoop;

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
                eventEmitter, contextProjectionBuilder, new AgentRunLoopProperties(),
                agentRunLoop, resolver, classifier, decider);
    }

    private BotSession session(String phase, String uc, String formContext) {
        BotSession s = new BotSession();
        s.setSessionId("sess-sprint12");
        s.setCurrentPhase(phase);
        s.setActiveUseCase(uc);
        s.setFormTopicSubject("Ad Support");
        s.setFormContext(formContext);
        return s;
    }

    private static DriftResult noDrift() {
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

    private ContextProjectionBuilder realProjectionBuilder() {
        UseCaseRegistryService useCaseRegistry = mock(UseCaseRegistryService.class);
        ControlPolicyService controlPolicyMock = mock(ControlPolicyService.class);
        ToolPolicyEnforcer toolPolicyEnforcer = mock(ToolPolicyEnforcer.class);
        UseCaseRegistryService.UseCaseDefinition ucA = new UseCaseRegistryService.UseCaseDefinition(
                "UC-A", "Ad Status & Visibility", List.of("Ad Support"), "LOW", true, "FAQ");
        UseCaseRegistryService.UseCaseDefinition ucC = new UseCaseRegistryService.UseCaseDefinition(
                "UC-C", "Replies / Messaging", List.of("Replies"), "LOW", true, "FAQ");
        UseCaseRegistryService.UseCaseDefinition ucJ = new UseCaseRegistryService.UseCaseDefinition(
                "UC-J", "Trust & Safety", List.of("Trust & Safety"), "HIGH", true, "INTAKE");
        lenient().when(useCaseRegistry.getUseCase("UC-A")).thenReturn(ucA);
        lenient().when(useCaseRegistry.getUseCase("UC-C")).thenReturn(ucC);
        lenient().when(useCaseRegistry.getUseCase("UC-J")).thenReturn(ucJ);
        lenient().when(controlPolicyMock.getMaxBotTurnsFaq()).thenReturn(15);
        lenient().when(controlPolicyMock.getMaxBotTurnsIntake()).thenReturn(20);
        lenient().when(controlPolicyMock.getMaxClarificationRounds()).thenReturn(2);
        lenient().when(controlPolicyMock.getMaxFaqMiss()).thenReturn(2);
        lenient().when(toolPolicyEnforcer.getVisibleToolsForUc(anyString()))
                .thenReturn(List.of("search_knowledge", "resolve_article",
                        "record_outcome", "request_handover"));
        ContextProjectionBuilder builder = new ContextProjectionBuilder(
                objectMapper, useCaseRegistry, controlPolicyMock, toolPolicyEnforcer);
        builder.initToolSchemas();
        return builder;
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 1 — UC-A → UC-C soft shift, no generic handover
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario1_ucA_to_ucC_softShift_noGenericHandover() {
        BotSession s = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-1001\",\"description\":\"my ad\"}");
        kernel.applyRerouteDecision(s, "I haven't got replies", noDrift(), "CONFIRM");

        // Behavioural contract — Sprint 10 reroute matrix.
        assertEquals("UC-C", s.getActiveUseCase(),
                "Soft shift must commit UC-C as the new active UC.");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "CONFIRM + soft-shift must transition into RESOLVE so the new UC's plan runs.");
        assertEquals("UC-A", s.getPreviousActiveUseCase());
        assertEquals("SOFT_SHIFT", s.getDriftType());
        assertNull(s.getEscalationReason(),
                "Soft shift must not stamp an escalation reason / generic handover.");

        // Sprint 12 §N0 — observability: trace must let a reviewer audit
        // why the bot soft-shifted.
        assertEquals("UC-C", s.getPredictedUseCase());
        assertEquals(IntentRelation.NEW_LOW_RISK_UC.name(), s.getIntentRelation());
        assertEquals(RerouteAction.SOFT_SHIFT_TO_DISCOVER.name(), s.getRerouteAction());
        assertEquals("soft_shift_to_uc_c", s.getPhaseTransitionReason());
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 2 — UC-A same-issue dissatisfaction → UC-A / RESOLVE
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario2_ucA_sameIssue_stillCantSeeMyAd_reboundsToResolve() {
        BotSession s = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-1001\",\"description\":\"my ad\"}");
        kernel.applyRerouteDecision(s, "I still can't see my ad", noDrift(), "CONFIRM");

        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Same-issue dissatisfaction from CONFIRM must rebound to RESOLVE.");
        assertEquals("SAME_ISSUE", s.getDriftType());
        assertEquals("listing_visibility_diagnostic", s.getCurrentTaskType());
        assertEquals("listing", s.getPrimaryEntityType());
        assertEquals("AD-1001", s.getPrimaryEntityValue(),
                "primary_entity must come from form_context.ad_id.");

        // Sprint 12 §N0 — reviewer audit fields.
        assertEquals(IntentRelation.SAME_ISSUE.name(), s.getIntentRelation());
        assertEquals(RerouteAction.REBOUND_TO_RESOLVE.name(), s.getRerouteAction());
        assertEquals("same_issue_resolve_rebound", s.getPhaseTransitionReason());
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 3 — UC-A same-UC follow-up → UC-A / RESOLVE
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario3_ucA_sameUcFollowup_howLongIsItActive_reboundsToResolve() {
        BotSession s = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-1002\",\"description\":\"my ad\"}");
        kernel.applyRerouteDecision(s, "how long is it active for?", noDrift(), "CONFIRM");

        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("RESOLVE", s.getCurrentPhase());
        assertEquals("SAME_UC_NEW_TASK", s.getDriftType());
        assertEquals("listing_lifecycle_followup", s.getCurrentTaskType());
        assertEquals("AD-1002", s.getPrimaryEntityValue(),
                "Same-UC follow-up must reuse the form_context ad_id.");

        // Sprint 12 §N0 — reviewer audit fields.
        assertEquals(IntentRelation.SAME_UC_NEW_TASK.name(), s.getIntentRelation());
        assertEquals(RerouteAction.REBOUND_TO_RESOLVE.name(), s.getRerouteAction());
        assertEquals("same_uc_followup_resolve_rebound", s.getPhaseTransitionReason());
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 4 — UC-A progressive listing diagnostic, multi-turn
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario4_progressiveListingDiagnostic_entityReuse_finalUserRequested() {
        // Turn 1: "How do I find my ad?" — Sprint 10 MVP shapes do not
        // match. CONTINUE_CURRENT; UC-A / RESOLVE preserved.
        BotSession s = session("RESOLVE", "UC-A",
                "{\"description\":\"can't find my ad\"}");
        kernel.applyRerouteDecision(s, "How do I find my ad?", noDrift(), "RESOLVE");
        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("RESOLVE", s.getCurrentPhase());
        assertEquals(RerouteAction.CONTINUE_CURRENT.name(), s.getRerouteAction(),
                "Sprint 12 §N0 — first turn must record CONTINUE_CURRENT so a reviewer can "
                        + "audit why the bot stayed in UC-A.");

        // The bot's soft FAQ-grounded answer asks for the advert ID.
        AgentRunResult softAnswer = AgentRunResult.finalAnswer(
                "Here is how to find your ad in My Ads. If you want me to check, "
                        + "send the advert ID and I will look it up.",
                List.of(), List.of(), null, null);
        ResolveDisposition softDisposition = ResolveDispositionEvaluator.evaluate(
                faqResolvePlan("UC-A"), softAnswer);
        assertEquals(ResolveDisposition.ASKED_FOR_SLOT, softDisposition,
                "Soft next-step answer must remain in RESOLVE as ASKED_FOR_SLOT.");

        // Turn 2: "Why can't I find my ad?" — the same-issue regex fires.
        kernel.applyRerouteDecision(s, "Why can't I find my ad?", noDrift(), "RESOLVE");
        // Intent fires only when the message phrasing matches the §L0
        // patterns. "Why can't I find" itself is informational; the
        // classifier should preserve UC-A. Either UNKNOWN or SAME_ISSUE
        // is acceptable; we pin that the UC and phase do not regress.
        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("RESOLVE", s.getCurrentPhase());

        // Turn 3: user provides an ad_id ("1234567890"). Sprint 11 §M0
        // same-UC capture stamps primary_entity and persists into
        // form_context.
        kernel.applyRerouteDecision(s, "1234567890", noDrift(), "RESOLVE");
        assertEquals("listing", s.getPrimaryEntityType());
        assertEquals("1234567890", s.getPrimaryEntityValue());
        assertEquals("user_message.ad_id", s.getLastEntityContextRef());
        assertTrue(s.getFormContext().contains("\"ad_id\":\"1234567890\""),
                "Captured ad_id must be persisted into form_context for cross-turn reuse.");

        // Turn 4: expiry/status follow-up reuses the entity.
        kernel.applyRerouteDecision(s, "How long is it active for?", noDrift(), "RESOLVE");
        assertEquals("listing_lifecycle_followup", s.getCurrentTaskType());
        assertEquals("1234567890", s.getPrimaryEntityValue(),
                "primary_entity must persist across the same-UC follow-up.");
        assertEquals("SAME_UC_NEW_TASK", s.getDriftType());

        // Turn 5: explicit human request — classifier surfaces
        // HUMAN_REQUEST and the resolver continues to flag explicit-human
        // for the kernel's step 2.5 forceEscalate path.
        IntentClassification humanClassification = classifier.classify(
                s, "I want a human", noDrift());
        assertEquals(IntentRelation.HUMAN_REQUEST, humanClassification.relation());
        assertTrue(resolver.detectExplicitUserEscalation("I want a human"),
                "Explicit human request must continue to surface user_requested via "
                        + "EscalationReasonResolver.");
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 5 — UC-C messaging follow-up stays UC-C / RESOLVE
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario5_ucC_messagingFollowup_staysUcC_resolve() {
        BotSession s = session("RESOLVE", "UC-C", "{}");
        kernel.applyRerouteDecision(s, "I haven't got replies", noDrift(), "RESOLVE");

        assertEquals("UC-C", s.getActiveUseCase(),
                "UC-C session repeating the same complaint must stay UC-C.");
        assertEquals("RESOLVE", s.getCurrentPhase());
        assertEquals(IntentRelation.SAME_ISSUE.name(), s.getIntentRelation());
        // Already in RESOLVE — RerouteDecider returns CONTINUE_CURRENT for
        // SAME_ISSUE; phase_transition_reason remains the classifier-
        // surfaced default for an uneventful turn.
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 6 — Risk shift to UC-J on "I was scammed"
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario6_iWasScammed_riskShiftsToUcJ_intakePath_notFaq() {
        BotSession s = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-1003\",\"description\":\"my ad\"}");
        kernel.applyRerouteDecision(s, "I was scammed", noDrift(), "CONFIRM");

        assertEquals("UC-J", s.getActiveUseCase(),
                "Risk shift must commit UC-J (Trust & Safety intake UC).");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Risk shift must land in RESOLVE so the intake plan runs, not generic FAQ.");
        assertEquals("RISK_SHIFT", s.getDriftType());
        assertEquals(IntentRelation.NEW_HIGH_RISK_UC.name(), s.getIntentRelation());
        assertEquals(RerouteAction.RISK_SHIFT_TO_INTAKE.name(), s.getRerouteAction());
        assertEquals("risk_shift_to_uc_j", s.getPhaseTransitionReason());
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 7 — Explicit human request → user_requested
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario7_explicitHumanRequest_userRequested() {
        // Resolver-side contract: detectExplicitUserEscalation flags
        // explicit-human shapes so the kernel's step 2.5 forceEscalate
        // applies user_requested before the planner runs.
        assertTrue(resolver.detectExplicitUserEscalation("I want a human"));
        assertTrue(resolver.detectExplicitUserEscalation("Please call me back"));
        assertTrue(resolver.detectExplicitUserEscalation("Can I speak to an agent?"));

        // Classifier-side contract: surface HUMAN_REQUEST so the
        // observability slots reflect the relation.
        BotSession s = session("RESOLVE", "UC-A", "{}");
        IntentClassification c = classifier.classify(s, "I want a human", noDrift());
        assertEquals(IntentRelation.HUMAN_REQUEST, c.relation());
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 8 — Payment ambiguity negative guard (no blind UC-I)
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario8_paymentAmbiguity_paidForTopAd_doesNotBlindlyRouteToUcI() {
        BotSession s = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-1004\",\"description\":\"paid for promotion\"}");
        kernel.applyRerouteDecision(s,
                "I paid for Top Ad but it's not showing", noDrift(), "CONFIRM");

        assertNotEquals("UC-I", s.getActiveUseCase(),
                "Payment-ambiguity ad-visibility shape must NOT route to UC-I.");
        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("listing_visibility_paid_promotion", s.getCurrentTaskType());
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Same-issue from CONFIRM rebounds to RESOLVE; the negative guard does not "
                        + "downgrade the rebound itself.");
        assertEquals(IntentRelation.SAME_ISSUE.name(), s.getIntentRelation());
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 9 — record_outcome(resolve) guard rejects premature close
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario9_recordOutcomeGuard_rejectsPrematureResolveBeforeConfirm() {
        // RESOLVE / FAQ + record_outcome(resolve) without CONFIRM /
        // CLOSE / terminal evidence must be rejected.
        PhasePlan plan = faqResolvePlan("UC-A");
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"1234567890\"}");
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("outcome_class", "resolve");
        ToolCall call = ToolCall.builder().name("record_outcome").arguments(args).build();
        assertTrue(AgentRunLoopImpl.shouldRejectPrematureResolveOutcome(plan, s, call),
                "record_outcome(resolve) before CONFIRM / terminal evidence must be rejected.");

        // CONFIRM phase: the guard must let the call through.
        BotSession confirmSession = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"1234567890\"}");
        PhasePlan confirmPlan = PhasePlan.builder()
                .phase("CONFIRM")
                .useCase("UC-A")
                .objective("confirm")
                .allowedTools(List.of("record_outcome", "request_handover"))
                .maxToolSteps(2)
                .build();
        assertFalse(AgentRunLoopImpl.shouldRejectPrematureResolveOutcome(
                        confirmPlan, confirmSession, call),
                "CONFIRM-phase record_outcome(resolve) must pass through the guard.");
    }

    // ─────────────────────────────────────────────────────────────
    // Scenario 10 — ResolveDisposition terminal-evidence contract
    // ─────────────────────────────────────────────────────────────

    @Test
    void scenario10a_nonSlotSameUcAnswer_withoutTerminalEvidence_staysResolve() {
        // Sprint 11.1 closure regression. A non-question, non-slot
        // factual answer ("Your advert is active for 30 days from
        // posting.") without a successful record_outcome dispatch must
        // map to ANSWERED_SUBTASK, NOT READY_TO_CONFIRM.
        PhasePlan plan = faqResolvePlan("UC-A");
        AgentRunResult listingStatusAnswer = AgentRunResult.finalAnswer(
                "Your advert is active for 30 days from posting.",
                List.of(), List.of(), null, null);
        ResolveDisposition disposition =
                ResolveDispositionEvaluator.evaluate(plan, listingStatusAnswer);
        assertEquals(ResolveDisposition.ANSWERED_SUBTASK, disposition,
                "Non-slot UC-A same-UC answer without terminal evidence must stay RESOLVE "
                        + "as ANSWERED_SUBTASK.");
    }

    @Test
    void scenario10b_validTerminalEvidence_mayLeadToReadyToConfirm() {
        // Deterministic terminal evidence: a successful record_outcome
        // dispatch on this run earns READY_TO_CONFIRM.
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
                "Successful record_outcome dispatch is the deterministic terminal condition "
                        + "for READY_TO_CONFIRM.");
    }

    // ─────────────────────────────────────────────────────────────
    // Sprint 12 §N0 observability — projection surface
    // ─────────────────────────────────────────────────────────────

    @Test
    void observability_projectionSurfacesAllSprint12Fields() {
        // Build a session through a soft-shift turn so the §N0 fields
        // populate, then assert every Sprint 12 observability key the
        // objective enumerates is present in the JSON projection.
        BotSession s = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-1099\",\"description\":\"ad support\"}");
        kernel.applyRerouteDecision(s, "I haven't got replies", noDrift(), "CONFIRM");
        // Stamp the post-loop signals the kernel ordinarily sets.
        s.setResolveDisposition(ResolveDisposition.ANSWERED_SUBTASK.name());
        s.setTaskStatus(ResolveDisposition.ANSWERED_SUBTASK.toTaskStatusToken());
        s.setRecordOutcomeAttempted(Boolean.TRUE);
        s.setRecordOutcomeSucceeded(Boolean.FALSE);
        s.setRecordOutcomeGuardResult("rejected:progressive_resolve_record_outcome_premature");

        ContextProjectionBuilder builder = realProjectionBuilder();
        String json = builder.buildProjection(s, List.of(), null, "I haven't got replies");

        // The projection MUST surface every Sprint 12 observability key
        // listed in the objective so a reviewer can audit one trace
        // payload end-to-end.
        for (String key : List.of(
                "drift_history", "task_history",
                "phase_transition_reason", "reroute_action",
                "intent_relation", "predicted_use_case",
                "previous_active_use_case", "active_use_case",
                "current_task_type", "task_status",
                "primary_entity", "resolve_disposition",
                "terminal_evidence", "record_outcome_guard_result")) {
            assertTrue(json.contains("\"" + key + "\""),
                    "Sprint 12 §N0 — projection must surface '" + key
                            + "'. Actual JSON: " + json);
        }

        // Spot-check a few values to make sure aliases carry meaningful
        // content (not just the key shape).
        assertTrue(json.contains("\"reroute_action\":\"SOFT_SHIFT_TO_DISCOVER\""));
        assertTrue(json.contains("\"intent_relation\":\"NEW_LOW_RISK_UC\""));
        assertTrue(json.contains("\"predicted_use_case\":\"UC-C\""));
        assertTrue(json.contains("\"phase_transition_reason\":\"soft_shift_to_uc_c\""));
        assertTrue(json.contains("\"resolve_disposition\":\"ANSWERED_SUBTASK\""));
        assertTrue(json.contains(
                "\"record_outcome_guard_result\":\"rejected:progressive_resolve_record_outcome_premature\""));
        assertTrue(json.contains("\"record_outcome_attempted\":true"));
        assertTrue(json.contains("\"record_outcome_succeeded\":false"));
    }

    @Test
    void observability_projectionDriftHistory_aggregatesAcrossTurns() throws Exception {
        // A reviewer must be able to read the trajectory of drift / task
        // signals from the latest turn's projection alone. Build two
        // prior bot_turns with prior projections and assert the new
        // projection rolls them up.
        ContextProjectionBuilder builder = realProjectionBuilder();
        BotSession s1 = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-2001\",\"description\":\"my ad\"}");
        kernel.applyRerouteDecision(s1, "I haven't got replies", noDrift(), "CONFIRM");
        String firstProjection = builder.buildProjection(s1, List.of(), null,
                "I haven't got replies");
        BotTurn t1 = BotTurn.builder()
                .turnId("t1")
                .sessionId(s1.getSessionId())
                .turnIndex(1)
                .userMessage("I haven't got replies")
                .projectedContext(firstProjection)
                .build();

        BotSession s2 = session("RESOLVE", "UC-C",
                "{\"description\":\"replies\"}");
        kernel.applyRerouteDecision(s2, "I haven't got replies", noDrift(), "RESOLVE");
        String secondProjection = builder.buildProjection(s2, List.of(t1), null,
                "I haven't got replies");
        BotTurn t2 = BotTurn.builder()
                .turnId("t2")
                .sessionId(s2.getSessionId())
                .turnIndex(2)
                .userMessage("I haven't got replies")
                .projectedContext(secondProjection)
                .build();

        // Latest turn's projection should reflect history of t1 + t2.
        BotSession s3 = session("RESOLVE", "UC-C", "{}");
        s3.setActiveUseCase("UC-C");
        kernel.applyRerouteDecision(s3, "still nothing", noDrift(), "RESOLVE");
        String latest = builder.buildProjection(s3, List.of(t1, t2), null, "still nothing");

        JsonNode root = objectMapper.readTree(latest);
        JsonNode drift = root.get("drift_history");
        JsonNode task = root.get("task_history");
        assertNotNull(drift, "drift_history must be emitted as a JSON array.");
        assertNotNull(task, "task_history must be emitted as a JSON array.");
        assertTrue(drift.isArray());
        assertTrue(task.isArray());
        assertTrue(drift.size() >= 1,
                "drift_history must include at least one prior-turn entry.");
        // Pin one prior-turn signal so the trace evidence is meaningful
        // — t1 was the soft shift.
        boolean foundSoftShift = false;
        for (JsonNode entry : drift) {
            JsonNode tok = entry.get("drift_type");
            if (tok != null && "SOFT_SHIFT".equals(tok.asText(""))) {
                foundSoftShift = true;
                break;
            }
        }
        assertTrue(foundSoftShift,
                "drift_history must surface at least one SOFT_SHIFT entry from t1. "
                        + "Actual drift_history: " + drift);
    }
}
