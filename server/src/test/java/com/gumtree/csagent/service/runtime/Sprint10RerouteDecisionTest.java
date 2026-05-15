package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.DriftResult;
import com.gumtree.csagent.model.IntentClassification;
import com.gumtree.csagent.model.IntentClassification.IntentRelation;
import com.gumtree.csagent.model.RerouteDecision;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Sprint 10 §L1/§L2 — pin the runtime reroute decision matrix and the
 * kernel state mutation that flows from it. Each test exercises one of
 * the matrix cells listed in {@code docs/sprint_objective.md} and
 * verifies:
 *
 * <ol>
 *   <li>the {@link RerouteDecider} picks the correct
 *       {@link RerouteAction};</li>
 *   <li>{@code ControlKernel.applyRerouteDecision} mutates
 *       {@code session.activeUseCase} / {@code session.currentPhase}
 *       per the action;</li>
 *   <li>the §L2 minimal projection slots
 *       ({@code previous_active_use_case}, {@code drift_type},
 *       {@code current_task_type}, {@code primary_entity},
 *       {@code issue_status_summary}) are populated.</li>
 * </ol>
 *
 * <p>An already-committed UC is never overwritten unless the action
 * explicitly switches it (SOFT / RISK / REBOUND).
 */
@ExtendWith(MockitoExtension.class)
class Sprint10RerouteDecisionTest {

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
        // The reroute logic looks up isValidTransition for every phase
        // change; lenient stubbing returns true for the legal Sprint 10
        // transitions and false otherwise.
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

    private BotSession session(String phase, String uc) {
        BotSession s = new BotSession();
        s.setSessionId("sess-l1");
        s.setCurrentPhase(phase);
        s.setActiveUseCase(uc);
        s.setFormTopicSubject("Ad Support");
        s.setFormContext("{\"ad_id\":\"AD-1001\",\"description\":\"my ad disappeared\"}");
        return s;
    }

    private DriftResult noDrift() {
        return DriftResult.builder().type(DriftResult.DriftType.NONE).build();
    }

    // ─────────────────────────────────────────────────────────────
    // Test 1 — UC-A / CONFIRM + "I haven't got replies" -> UC-C, no handover
    // ─────────────────────────────────────────────────────────────

    @Test
    void test1_ucA_confirm_haventGotReplies_softShiftsToUcC_noHandover() {
        BotSession s = session("CONFIRM", "UC-A");
        kernel.applyRerouteDecision(s, "I haven't got replies", noDrift(), "CONFIRM");

        assertEquals("UC-C", s.getActiveUseCase(),
                "Soft shift must commit UC-C as the new active UC.");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "CONFIRM + soft-shift must transition to RESOLVE so the new UC's plan runs.");
        assertEquals("UC-A", s.getPreviousActiveUseCase(),
                "previous_active_use_case must capture the pre-mutation UC.");
        assertEquals("SOFT_SHIFT", s.getDriftType(),
                "drift_type must be SOFT_SHIFT for a NEW_LOW_RISK_UC reroute.");
        assertNotEquals("ESCALATE", s.getCurrentPhase(),
                "Soft shift must not escalate — no handover should fire.");
        assertNull(s.getEscalationReason(),
                "No escalation reason should be stamped on a soft shift.");
    }

    // ─────────────────────────────────────────────────────────────
    // Test 2 — UC-A / CONFIRM + "I still can't see my ad" -> UC-A, RESOLVE
    // ─────────────────────────────────────────────────────────────

    @Test
    void test2_ucA_confirm_stillCantSeeMyAd_reboundsToResolve_sameUc() {
        BotSession s = session("CONFIRM", "UC-A");
        kernel.applyRerouteDecision(s, "I still can't see my ad", noDrift(), "CONFIRM");

        assertEquals("UC-A", s.getActiveUseCase(),
                "Same-issue rebound must keep UC-A.");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Same-issue from CONFIRM must rebound to RESOLVE.");
        assertEquals("SAME_ISSUE", s.getDriftType());
        assertEquals("listing", s.getPrimaryEntityType());
        assertEquals("AD-1001", s.getPrimaryEntityValue(),
                "Primary entity value must come from form_context.ad_id.");
        assertEquals("listing_visibility_diagnostic", s.getCurrentTaskType());
        assertEquals("open", s.getIssueStatusSummary());
    }

    // ─────────────────────────────────────────────────────────────
    // Test 3 — UC-A / CONFIRM + "how long is it active for?" -> UC-A, RESOLVE
    // ─────────────────────────────────────────────────────────────

    @Test
    void test3_ucA_confirm_howLongIsItActive_reboundsToResolve_sameUc() {
        BotSession s = session("CONFIRM", "UC-A");
        kernel.applyRerouteDecision(s, "how long is it active for?", noDrift(), "CONFIRM");

        assertEquals("UC-A", s.getActiveUseCase());
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Same-UC follow-up from CONFIRM must rebound to RESOLVE.");
        assertEquals("SAME_UC_NEW_TASK", s.getDriftType());
        assertEquals("listing_lifecycle_followup", s.getCurrentTaskType());
    }

    // ─────────────────────────────────────────────────────────────
    // Test 4 — UC-A + "I was scammed" -> UC-J intake/handover path, not FAQ
    // ─────────────────────────────────────────────────────────────

    @Test
    void test4_iWasScammed_riskShiftsToUcJ_intakePath() {
        BotSession s = session("CONFIRM", "UC-A");
        kernel.applyRerouteDecision(s, "I was scammed", noDrift(), "CONFIRM");

        assertEquals("UC-J", s.getActiveUseCase(),
                "Risk shift must commit UC-J (Trust & Safety intake UC).");
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Risk shift must land in RESOLVE so the RESOLVE_INTAKE plan runs (intake path), "
                        + "not generic FAQ.");
        assertEquals("RISK_SHIFT", s.getDriftType());
        assertEquals("UC-A", s.getPreviousActiveUseCase());
    }

    // ─────────────────────────────────────────────────────────────
    // Test 5 — Explicit human request via the classifier surface (HUMAN_REQUEST)
    // ─────────────────────────────────────────────────────────────

    @Test
    void test5_explicitHumanRequest_classifierSurfacesHumanRequest() {
        // The kernel's existing step 2.5 forceEscalate path performs the
        // user_requested escalation BEFORE the classifier fires in
        // production. This test pins that the classifier itself still
        // identifies HUMAN_REQUEST so callers (and the trace observability
        // payload) remain consistent.
        IntentClassification c = classifier.classify(
                session("RESOLVE", "UC-A"),
                "I want a human",
                noDrift());
        assertEquals(IntentRelation.HUMAN_REQUEST, c.relation());

        RerouteDecision d = decider.decide(session("RESOLVE", "UC-A"), c);
        assertEquals(RerouteAction.ESCALATE_IMMEDIATELY, d.action());
        assertEquals("user_requested_via_classifier", d.transitionReason());
    }

    // ─────────────────────────────────────────────────────────────
    // Test 6 — UC-A + "I paid for Top Ad but it's not showing" -> not blindly UC-I
    // ─────────────────────────────────────────────────────────────

    @Test
    void test6_paidForTopAd_notShowing_notBlindlyUcI() {
        BotSession s = session("CONFIRM", "UC-A");
        kernel.applyRerouteDecision(s,
                "I paid for Top Ad but it's not showing", noDrift(), "CONFIRM");

        assertNotEquals("UC-I", s.getActiveUseCase(),
                "Payment-ambiguity ad-visibility shape must NOT route to UC-I.");
        assertEquals("UC-A", s.getActiveUseCase(),
                "Payment-ambiguity ad-visibility shape must keep UC-A.");
        assertEquals("listing_visibility_paid_promotion", s.getCurrentTaskType());
        assertEquals("RESOLVE", s.getCurrentPhase(),
                "Same-issue from CONFIRM rebounds to RESOLVE; the negative guard does not "
                        + "downgrade the rebound itself.");
    }

    // ─────────────────────────────────────────────────────────────
    // Test 7 — Projection snapshot includes the §L2 fields
    // ─────────────────────────────────────────────────────────────

    @Test
    void test7_projectionSnapshot_includesSprint10L2Fields() {
        BotSession s = session("CONFIRM", "UC-A");
        kernel.applyRerouteDecision(s, "I still can't see my ad", noDrift(), "CONFIRM");

        // Build the projection from the current session and assert that
        // each of the five §L2 slots is present. This is intentionally
        // a contract on the projection JSON shape, not the values
        // (values are pinned by tests 1-6 above).
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
                objectMapper, useCaseRegistry, controlPolicyMock, toolPolicyEnforcer);
        realBuilder.initToolSchemas();

        String json = realBuilder.buildProjection(s, java.util.List.of(), null,
                "I still can't see my ad");
        assertTrue(json.contains("\"previous_active_use_case\":\"UC-A\""),
                "Projection must surface previous_active_use_case.");
        assertTrue(json.contains("\"drift_type\":\"SAME_ISSUE\""),
                "Projection must surface drift_type.");
        assertTrue(json.contains("\"current_task_type\":\"listing_visibility_diagnostic\""),
                "Projection must surface current_task_type.");
        assertTrue(json.contains("\"primary_entity\""),
                "Projection must surface primary_entity.");
        assertTrue(json.contains("\"ad_id\":\"AD-1001\""),
                "primary_entity must carry the ad_id when present in form_context.");
        assertTrue(json.contains("\"issue_status_summary\":\"open\""),
                "Projection must surface issue_status_summary.");
    }

    // ─────────────────────────────────────────────────────────────
    // Test 8 — Already-committed UC is preserved unless action requires switch
    // ─────────────────────────────────────────────────────────────

    @Test
    void test8_committedUc_isPreserved_whenRerouteIsContinueCurrent() {
        BotSession s = session("DISCOVER", "UC-A");
        // Plain "okay" produces UNKNOWN; CONTINUE_CURRENT must not touch UC.
        kernel.applyRerouteDecision(s, "okay", noDrift(), "DISCOVER");
        assertEquals("UC-A", s.getActiveUseCase(),
                "CONTINUE_CURRENT must never overwrite a committed UC.");
        assertEquals("DISCOVER", s.getCurrentPhase(),
                "CONTINUE_CURRENT must never change the phase.");
    }

    @Test
    void test8b_committedUc_isPreserved_acrossSameIssueRebound() {
        // Same-issue rebound only changes the phase; the UC itself is
        // never overwritten by the reroute.
        BotSession s = session("CONFIRM", "UC-A");
        kernel.applyRerouteDecision(s, "I still can't see my ad", noDrift(), "CONFIRM");
        assertEquals("UC-A", s.getActiveUseCase(),
                "Same-issue rebound preserves the committed UC.");
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

}
