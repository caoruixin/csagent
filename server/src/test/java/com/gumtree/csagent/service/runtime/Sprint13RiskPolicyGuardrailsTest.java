package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.config.AgentRunLoopProperties;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.IntentClassification;
import com.gumtree.csagent.model.IntentClassification.IntentRelation;
import com.gumtree.csagent.model.RerouteDecision.RerouteAction;
import com.gumtree.csagent.repository.BotEventRepository;
import com.gumtree.csagent.repository.BotTurnRepository;
import com.gumtree.csagent.service.observability.EventEmitter;
import com.gumtree.csagent.service.tools.CreateCaseControlledTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 13 §O2 — eval guardrails for risk-policy distinctions.
 *
 * <p>Pins the runtime-side contract that risk-related user messages do
 * NOT all collapse into immediate handover. Each test mirrors one row
 * of the canonical risk taxonomy in
 * {@code docs/runtime_freeze_and_risk_policy.md}:
 * <ul>
 *   <li>Level 1 — observe only (paid-Top-Ad-not-showing).</li>
 *   <li>Level 2 — constrained continue (money back without dispute).</li>
 *   <li>Level 3a — explicit human request (user_requested).</li>
 *   <li>Level 3b — high-risk intake (scam → UC-J).</li>
 *   <li>Level 3b — GDPR / account deletion (delete account → UC-G).</li>
 *   <li>Negative guard — generic ad visibility stays normal flow.</li>
 * </ul>
 *
 * <p>This suite is deterministic. It uses the existing Sprint 10 / 11 /
 * 12 fixture shape (mocked repositories, real
 * {@link RuntimeIntentClassifier}, {@link RerouteDecider},
 * {@link EscalationReasonResolver}). It does NOT call the live LLM, the
 * FAQ service, or the agent run loop. It does NOT promote new live
 * CaseSpecs and does NOT modify any existing hard gate.
 *
 * <p>Sprint 13 §O0 / §O1 / §O2 explicitly forbid runtime main-flow
 * changes. These guardrails encode the CURRENT contract; they will fail
 * (loudly, deterministically) if a future change regresses the risk
 * taxonomy without an explicit migration.
 */
@ExtendWith(MockitoExtension.class)
class Sprint13RiskPolicyGuardrailsTest {

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
        s.setSessionId("sess-sprint13");
        s.setCurrentPhase(phase);
        s.setActiveUseCase(uc);
        s.setFormTopicSubject("Ad Support");
        s.setFormContext(formContext);
        return s;
    }

    private static DriftResult noDrift() {
        return DriftResult.builder().type(DriftResult.DriftType.NONE).build();
    }

    /** A DriftResult shaped like the legacy DriftDetector hard-shift output
     *  (used only for the Level 2 / 3b cases where the Sprint 10 MVP
     *  shapes do not fire and the kernel falls back to drift). */
    private static DriftResult hardShift(String targetUc) {
        return DriftResult.builder()
                .type(DriftResult.DriftType.HARD_SHIFT)
                .newUseCase(targetUc)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Level 1 — Observe only: "I paid for Top Ad but it is not showing"
    // ─────────────────────────────────────────────────────────────

    @Test
    void observeOnly_paidTopAdNotShowing_staysUcA_noEscalation_noUcI() {
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-1099\",\"description\":\"paid for promotion\"}");

        // Drift detector mock returns NONE — the Sprint 10 negative guard
        // in RuntimeIntentClassifier is what drives the Level-1 behaviour.
        kernel.applyRerouteDecision(s, "I paid for Top Ad but it is not showing",
                noDrift(), "RESOLVE");

        // Routing — must NOT route to UC-I.
        assertNotEquals("UC-I", s.getActiveUseCase(),
                "Sprint 13 Level 1 (observe-only) — paid-Top-Ad ad-visibility shape "
                        + "must not route to UC-I (Payments) just because of the "
                        + "'paid' keyword.");
        assertEquals("UC-A", s.getActiveUseCase(),
                "Paid-Top-Ad ad-visibility must stay on UC-A.");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Already in RESOLVE — same-issue rebound is a no-op here.");

        // Escalation — must NOT stamp escalation_reason from a Level 1 signal.
        assertNull(s.getEscalationReason(),
                "Level 1 risk signal must not stamp an escalation_reason.");

        // Task type — runtime correctly tags the paid-promotion shape.
        assertEquals("listing_visibility_paid_promotion", s.getCurrentTaskType());
    }

    @Test
    void observeOnly_paidTopAdNotShowing_classifierSurfacesNegativeGuard() {
        // Classifier-level pin: the negative guard fires for the
        // payment-ambiguity ad-visibility shape.
        assertTrue(RuntimeIntentClassifier.matchesPaymentAmbiguityAdVisibility(
                "I paid for Top Ad but it is not showing"),
                "PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN must fire on the canonical "
                        + "Sprint 10 §L0 shape so the Sprint 13 Level 1 (observe-only) "
                        + "behaviour holds even when the active UC is something else.");

        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-1099\"}");
        IntentClassification c = classifier.classify(s,
                "I paid for Top Ad but it's not showing", noDrift());
        assertEquals("UC-A", c.predictedUseCase(),
                "Predicted UC must remain UC-A — never UC-I — for the paid-Top-Ad shape.");
    }

    // ─────────────────────────────────────────────────────────────
    // Level 2 — Constrained continue: "I want my money back because my ad is not visible"
    // ─────────────────────────────────────────────────────────────

    @Test
    void constrainedContinue_moneyBack_doesNotStampEscalationReason() {
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-2099\",\"description\":\"my ad\"}");

        // The legacy DriftDetector hard-shifts on "money back" to UC-I.
        // The runtime layer ROUTES to UC-I but does NOT escalate.
        kernel.applyRerouteDecision(s,
                "I want my money back because my ad is not visible",
                hardShift("UC-I"), "RESOLVE");

        // No escalation_reason from a Level 2 keyword alone.
        assertNull(s.getEscalationReason(),
                "Sprint 13 Level 2 — 'money back' alone must not stamp "
                        + "escalation_reason. The bot must continue safely (no refund "
                        + "promise, no liability decision) under the prompt's safety "
                        + "rules. Routing to UC-I is acceptable; auto-handover is not.");
    }

    @Test
    void constrainedContinue_moneyBack_classifierDoesNotEscalateImmediately() {
        // Classifier-level pin: "money back" does NOT match the explicit
        // human-request pattern, the distress pattern, the UC-J risk
        // pattern, or the payment-ambiguity ad-visibility pattern.
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-2099\"}");

        assertFalse(resolver.detectExplicitUserEscalation(
                "I want my money back because my ad is not visible"),
                "'money back' is NOT an explicit human-request shape.");
        assertFalse(RuntimeIntentClassifier.matchesUcJRiskShift(
                "I want my money back because my ad is not visible"),
                "'money back' is NOT a UC-J trust-safety shape.");

        // With NONE drift, the classifier returns UNKNOWN — the runtime
        // does not escalate just from 'money back'. Routing is delegated
        // to the legacy DriftDetector (HARD_SHIFT to UC-I).
        IntentClassification cNoDrift = classifier.classify(s,
                "I want my money back because my ad is not visible", noDrift());
        assertEquals(IntentRelation.UNKNOWN, cNoDrift.relation(),
                "Sprint 13 Level 2 — without a DriftDetector hard-shift, the "
                        + "classifier returns UNKNOWN and the kernel leaves state "
                        + "untouched. The runtime never auto-stamps user_requested.");

        // With the legacy hard-shift signal, the classifier exposes the
        // routing as NEW_HIGH_RISK_UC for UC-I, but this is a routing
        // decision, NOT a handover. The decider's RISK_SHIFT_TO_INTAKE
        // does not stamp an escalation_reason.
        IntentClassification cWithDrift = classifier.classify(s,
                "I want my money back because my ad is not visible",
                hardShift("UC-I"));
        assertEquals(IntentRelation.NEW_HIGH_RISK_UC, cWithDrift.relation(),
                "Drift-driven UC-I routing surfaces NEW_HIGH_RISK_UC — but this is a "
                        + "routing decision, not an escalation_reason stamp.");
    }

    // ─────────────────────────────────────────────────────────────
    // Level 3a — Immediate escalation: "I want to speak to a human"
    // ─────────────────────────────────────────────────────────────

    @Test
    void explicitHumanRequest_userRequested_resolverDetectsAllShapes() {
        // Resolver-level pin: every Sprint 13 §3.3a shape must fire so
        // the kernel forceEscalate path applies user_requested before
        // the planner runs.
        assertTrue(resolver.detectExplicitUserEscalation(
                "I want to speak to a human"));
        assertTrue(resolver.detectExplicitUserEscalation(
                "Can I speak to an agent?"));
        assertTrue(resolver.detectExplicitUserEscalation(
                "Please call me back"));
        assertTrue(resolver.detectExplicitUserEscalation(
                "Connect me to a person"));
        assertTrue(resolver.detectExplicitUserEscalation(
                "I want a human"));
    }

    @Test
    void explicitHumanRequest_classifierSurfacesHumanRequestRelation() {
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-3099\"}");
        IntentClassification c = classifier.classify(s,
                "I want to speak to a human", noDrift());
        assertEquals(IntentRelation.HUMAN_REQUEST, c.relation(),
                "Sprint 13 Level 3a — classifier must surface HUMAN_REQUEST so the "
                        + "Sprint 12 §N0 trace records the explicit-human signal alongside "
                        + "the kernel's existing forceEscalate(user_requested) path.");
    }

    // ─────────────────────────────────────────────────────────────
    // Level 3b — High-risk intake: "I was scammed"
    // ─────────────────────────────────────────────────────────────

    @Test
    void scammed_routesToUcJ_intakePath_notFaq() {
        BotSession s = session("CONFIRM", "UC-A",
                "{\"ad_id\":\"AD-4099\",\"description\":\"my ad\"}");

        kernel.applyRerouteDecision(s, "I was scammed", noDrift(), "CONFIRM");

        // Routing — must risk-shift to UC-J trust-safety intake.
        assertEquals("UC-J", s.getActiveUseCase(),
                "Sprint 13 Level 3b — UC-J risk shift on 'I was scammed'.");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Risk shift must land in RESOLVE so the intake plan runs.");
        assertEquals("RISK_SHIFT", s.getDriftType());
        assertEquals(RerouteAction.RISK_SHIFT_TO_INTAKE.name(), s.getRerouteAction());
        assertEquals(IntentRelation.NEW_HIGH_RISK_UC.name(), s.getIntentRelation());
        assertEquals("risk_shift_to_uc_j", s.getPhaseTransitionReason());

        // The runtime risk-shift on its own does not stamp escalation_reason
        // — that happens at intake completion (intake_complete_for_uc_j) or
        // when a Tier-1 trust_safety_required signal is detected later.
        assertNull(s.getEscalationReason(),
                "Risk shift to UC-J is a routing decision; escalation_reason is "
                        + "stamped at intake completion, not at the risk-shift turn.");
    }

    // ─────────────────────────────────────────────────────────────
    // Level 3b — GDPR / account deletion: "delete my account"
    // ─────────────────────────────────────────────────────────────

    @Test
    void gdprDeleteMyAccount_routesToUcG_intakePath_notFaq() {
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-5099\"}");

        // The DriftDetector hard-shifts on "delete my account" → UC-G.
        kernel.applyRerouteDecision(s, "Delete my account",
                hardShift("UC-G"), "RESOLVE");

        // Routing — must risk-shift to UC-G GDPR intake.
        assertEquals("UC-G", s.getActiveUseCase(),
                "Sprint 13 Level 3b — UC-G risk shift on 'Delete my account'. The "
                        + "runtime never claims deletion has been performed; it routes "
                        + "to the GDPR intake.");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Risk shift must land in RESOLVE so the GDPR intake plan runs.");
        assertEquals(RerouteAction.RISK_SHIFT_TO_INTAKE.name(), s.getRerouteAction());
        assertEquals(IntentRelation.NEW_HIGH_RISK_UC.name(), s.getIntentRelation());
        assertEquals("risk_shift_to_uc_g", s.getPhaseTransitionReason());

        // No escalation_reason stamped at the routing turn — gdpr_intake /
        // intake_complete_for_uc_g is set when the intake plan completes.
        assertNull(s.getEscalationReason(),
                "Risk shift to UC-G is a routing decision; escalation_reason "
                        + "(gdpr_intake / intake_complete_for_uc_g) is stamped at intake "
                        + "completion, not at the risk-shift turn.");
    }

    @Test
    void gdprDeleteMyData_alsoRoutesToUcG() {
        // Pin the alternate GDPR phrasings ("delete my data", "GDPR",
        // "right to be forgotten") so a future regex tweak cannot
        // silently drop one.
        BotSession s = session("RESOLVE", "UC-A", "{}");
        kernel.applyRerouteDecision(s, "I want my data deleted under GDPR",
                hardShift("UC-G"), "RESOLVE");
        assertEquals("UC-G", s.getActiveUseCase(),
                "GDPR data-deletion phrasings must risk-shift to UC-G.");
        assertEquals(RerouteAction.RISK_SHIFT_TO_INTAKE.name(), s.getRerouteAction());
    }

    // ─────────────────────────────────────────────────────────────
    // Negative guard — generic ad visibility stays normal flow
    // ─────────────────────────────────────────────────────────────

    @Test
    void negativeGuard_genericAdVisibility_staysUcA_noOverEscalation() {
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-6099\",\"description\":\"my ad\"}");

        // No risk vocabulary — just a generic FAQ-class question.
        kernel.applyRerouteDecision(s, "How do I find my ad?",
                noDrift(), "RESOLVE");

        assertEquals("UC-A", s.getActiveUseCase(),
                "Generic ad-visibility question must stay UC-A.");
        assertEquals("RESOLVE", s.getCurrentPhase());
        assertEquals(RerouteAction.CONTINUE_CURRENT.name(), s.getRerouteAction(),
                "Generic FAQ-class question must produce CONTINUE_CURRENT — no "
                        + "over-escalation.");
        assertNull(s.getEscalationReason(),
                "No risk vocabulary → no escalation_reason.");
    }

    @Test
    void negativeGuard_followupDuration_staysUcA_noOverEscalation() {
        BotSession s = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-6100\"}");
        kernel.applyRerouteDecision(s, "How long is it active for?",
                noDrift(), "RESOLVE");

        assertEquals("UC-A", s.getActiveUseCase(),
                "UC-A same-UC follow-up must stay UC-A — no over-escalation.");
        assertEquals("RESOLVE", s.getCurrentPhase());
        assertEquals("listing_lifecycle_followup", s.getCurrentTaskType());
        assertEquals(IntentRelation.SAME_UC_NEW_TASK.name(), s.getIntentRelation());
        assertNull(s.getEscalationReason());
    }

    // ─────────────────────────────────────────────────────────────
    // Cross-cutting — risk_flag vs escalation_trigger separation
    // ─────────────────────────────────────────────────────────────

    @Test
    void riskFlag_doesNotImplyEscalationTrigger_acrossLevel1AndLevel2() {
        // Sprint 13 §4 distinguishes risk_flag (informational) from
        // escalation_trigger (boolean that fires the kernel's escalation
        // path). The Level 1 paid-Top-Ad case AND the Level 2 money-back
        // case both exhibit a risk_flag without firing an
        // escalation_trigger.
        BotSession l1 = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-7001\",\"description\":\"paid for promotion\"}");
        kernel.applyRerouteDecision(l1, "I paid for Top Ad but it is not showing",
                noDrift(), "RESOLVE");

        BotSession l2 = session("RESOLVE", "UC-A",
                "{\"ad_id\":\"AD-7002\"}");
        kernel.applyRerouteDecision(l2,
                "I want my money back because my ad is not visible",
                hardShift("UC-I"), "RESOLVE");

        // Both turns produce observability fields a reviewer can audit
        // (predictedUseCase / intentRelation / rerouteAction /
        // phaseTransitionReason populated by Sprint 12 §N0) but neither
        // produces an escalation_reason stamp.
        assertNull(l1.getEscalationReason(),
                "Level 1 paid-Top-Ad must not fire the escalation_trigger.");
        assertNull(l2.getEscalationReason(),
                "Level 2 money-back must not fire the escalation_trigger.");
    }
}
