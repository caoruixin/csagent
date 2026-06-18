package com.gumtree.csagent.service.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.BotTurn;
import com.gumtree.csagent.model.PhasePlan;
import com.gumtree.csagent.model.PhaseTransitionDecision;
import com.gumtree.csagent.model.ResolveDisposition;
import com.gumtree.csagent.model.TerminalOutcome;
import com.gumtree.csagent.model.ToolEvent;
import com.gumtree.csagent.service.runtime.skill.SkillGuardrailDispatcher;
import com.gumtree.csagent.service.runtime.skill.SkillTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 093 / S-Auto-39 — RESOLVE→CONFIRM deadlock characterization suite.
 *
 * <p>Root cause (Step-0 attribution): a grounded UC-A RESOLVE answer maps to
 * {@link ResolveDisposition#ANSWERED_SUBTASK} and stays in RESOLVE, while
 * {@code record_outcome(resolve)} is correctly rejected outside CONFIRM/CLOSE
 * by the premature-resolve guard — so the session can never legitimately reach
 * CONFIRM and the loop eventually escalates with a mis-attributed reason.
 *
 * <p>The PRIMARY fix is a STRUCTURAL RESOLVE→CONFIRM transition: when a
 * grounded FINAL_ANSWER was delivered on a PRIOR RESOLVE turn (a persisted
 * {@link BotTurn} with {@code phase_after == RESOLVE} and non-empty
 * {@code source_ids}) AND a subsequent user turn produces another confident
 * grounded non-slot answer, the runtime enters CONFIRM so the CONFIRM Skill
 * can record the outcome. The premature guard is PRESERVED; no user-message
 * content is inspected.
 *
 * <p>This suite pins:
 * <ol>
 *   <li>deadlock shape now reaches CONFIRM (records become possible);</li>
 *   <li>a genuinely premature record (RESOLVE, NO prior grounded answer /
 *       no subsequent user turn) still stays in RESOLVE and is still rejected
 *       by the guard;</li>
 *   <li>no early-CLOSE (promotion targets CONFIRM, never CLOSE), genuine
 *       record_outcome failures still retry, slot-requests still stay, and a
 *       successful record still earns the unchanged READY_TO_CONFIRM path.</li>
 * </ol>
 */
class Sprint93ResolveConfirmDeadlockTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── fixtures ──────────────────────────────────────────────────

    private PhaseEvaluator realEvaluator() {
        return new PhaseEvaluator(
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
    }

    private static PhasePlan faqResolvePlan() {
        return PhasePlan.builder()
                .phase("RESOLVE")
                .useCase("UC-A")
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

    private static BotSession resolveSession() {
        BotSession s = new BotSession();
        s.setSessionId("sess-deadlock");
        s.setCurrentPhase("RESOLVE");
        s.setActiveUseCase("UC-A");
        s.setFormTopicSubject("Ad Support");
        s.setFormContext("{\"description\":\"can't find my ad\"}");
        return s;
    }

    /** A persisted prior turn that delivered a grounded answer and stayed in RESOLVE. */
    private static BotTurn priorGroundedResolveTurn() {
        return BotTurn.builder()
                .turnId("t-prior")
                .sessionId("sess-deadlock")
                .turnIndex(0)
                .userMessage("I can't see my advert anymore.")
                .botResponse("Your advert was removed for a policy violation; check your email.")
                .sourceIds(new String[]{"ka44J000000gKv5QAE"})
                .phaseBefore("RESOLVE")
                .phaseAfter("RESOLVE")
                .activeUseCase("UC-A")
                .build();
    }

    /** A prior DISCOVER turn that ran search_knowledge while clarifying (NOT an answer). */
    private static BotTurn priorDiscoverSearchTurn() {
        return BotTurn.builder()
                .turnId("t-discover")
                .sessionId("sess-deadlock")
                .turnIndex(0)
                .userMessage("hi I can't find my advert")
                .botResponse("Could you tell me a bit more about your ad?")
                .sourceIds(new String[]{"ka44J000000gKv5QAE"})
                .phaseBefore("DISCOVER")
                .phaseAfter("DISCOVER")
                .activeUseCase("UC-A")
                .build();
    }

    private static AgentRunResult groundedAnswer(String text, List<ToolEvent> toolEvents) {
        return AgentRunResult.finalAnswer(text, List.of(), toolEvents, null, null);
    }

    /**
     * Mirror the kernel wiring: derive the structural precondition from the
     * persisted history via the real static helper, stash it on the session
     * transient, then invoke the 3-arg evaluator entry point.
     */
    private static PhaseTransitionDecision interpret(PhaseEvaluator evaluator,
                                                     PhasePlan plan,
                                                     AgentRunResult result,
                                                     BotSession session,
                                                     List<BotTurn> history) {
        session.setPriorGroundedResolveAnswer(
                PhaseEvaluator.priorGroundedResolveAnswerDelivered(history));
        return evaluator.interpretRunResult(plan, result, session);
    }

    private static ToolEvent prematureRejectedRecordOutcome() {
        return new ToolEvent(0, 0, "record_outcome",
                Map.of("outcome_class", "resolve"),
                false, null,
                SkillGuardrailDispatcher.PROGRESSIVE_RESOLVE_REJECT_REASON, 0L);
    }

    private static final String GROUNDED_TEXT =
            "Your advert was removed for a policy violation. "
                    + "Check your email and spam folder for the notification.";

    // ── 1. Deadlock recovers ──────────────────────────────────────

    @Test
    void deadlockShape_priorGroundedAnswer_plusPrematureRejectedRecord_reachesConfirm() {
        // The canonical deadlock turn: a grounded answer was delivered on a
        // prior RESOLVE turn, the user has come back, the bot re-delivers a
        // grounded non-slot answer AND its record_outcome(resolve) is rejected
        // ONLY by the premature guard. The runtime must now enter CONFIRM so
        // the record can land on the CONFIRM turn.
        PhaseEvaluator evaluator = realEvaluator();
        AgentRunResult result = groundedAnswer(GROUNDED_TEXT,
                List.of(prematureRejectedRecordOutcome()));

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), result, resolveSession(), List.of(priorGroundedResolveTurn()));

        assertEquals("CONFIRM", decision.nextPhase(),
                "A grounded answer re-delivered after a prior grounded RESOLVE turn must "
                        + "promote RESOLVE -> CONFIRM so record_outcome can land.");
        assertEquals("progressive_resolve_confirmable", decision.transitionReason());
        assertNotEquals("CLOSE", decision.nextPhase(),
                "The structural promotion must target CONFIRM, never CLOSE (no early-close).");
    }

    @Test
    void deadlockShape_priorGroundedAnswer_groundedAnswerWithoutRecordAttempt_reachesConfirm() {
        // Same structural precondition but the bot did NOT retry record_outcome
        // this turn (it simply re-answered). The promotion is independent of
        // whether record_outcome was attempted.
        PhaseEvaluator evaluator = realEvaluator();
        AgentRunResult result = groundedAnswer(GROUNDED_TEXT, List.of());

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), result, resolveSession(), List.of(priorGroundedResolveTurn()));

        assertEquals("CONFIRM", decision.nextPhase());
        assertEquals("progressive_resolve_confirmable", decision.transitionReason());
    }

    // ── 2. Premature record / first answer still protected ────────

    @Test
    void firstGroundedAnswer_noPriorGroundedTurn_withRecordAttempt_staysResolve() {
        // The FIRST grounded answer turn (empty history = no subsequent user
        // turn yet) that also attempts record_outcome must still stay in
        // RESOLVE via the unchanged Sprint 9 §O1 retry path. This is the
        // premature-collapse protection: a grounded answer cannot reach
        // CONFIRM on the same turn it first forms.
        PhaseEvaluator evaluator = realEvaluator();
        AgentRunResult result = groundedAnswer(GROUNDED_TEXT,
                List.of(prematureRejectedRecordOutcome()));

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), result, resolveSession(), List.of());

        assertEquals("RESOLVE", decision.nextPhase(),
                "A grounded answer with no prior grounded RESOLVE turn must NOT reach CONFIRM.");
        assertEquals("record_outcome_failed_retry", decision.transitionReason());
    }

    @Test
    void firstGroundedAnswer_noPriorGroundedTurn_noRecordAttempt_staysResolveProgressive() {
        // First grounded answer, no record_outcome attempt, empty history.
        // Unchanged ANSWERED_SUBTASK -> progressive_resolve_stay.
        PhaseEvaluator evaluator = realEvaluator();
        AgentRunResult result = groundedAnswer(GROUNDED_TEXT, List.of());

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), result, resolveSession(), List.of());

        assertEquals("RESOLVE", decision.nextPhase());
        assertEquals("progressive_resolve_stay", decision.transitionReason());
    }

    @Test
    void priorDiscoverSearchTurn_doesNotCountAsPriorGroundedAnswer_staysResolve() {
        // A prior DISCOVER turn that merely ran search_knowledge while
        // clarifying (phase_after == DISCOVER) is NOT a delivered grounded
        // answer, so the first RESOLVE answer must still stay in RESOLVE.
        PhaseEvaluator evaluator = realEvaluator();
        AgentRunResult result = groundedAnswer(GROUNDED_TEXT, List.of());

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), result, resolveSession(), List.of(priorDiscoverSearchTurn()));

        assertEquals("RESOLVE", decision.nextPhase(),
                "Prior retrieval alone (DISCOVER search) must not satisfy the "
                        + "prior-grounded-answer precondition.");
        assertEquals("progressive_resolve_stay", decision.transitionReason());
    }

    @Test
    void prematureGuard_intact_rejectsResolveInResolve_allowsInConfirm() {
        // The premature-resolve guard itself is UNTOUCHED: record_outcome(resolve)
        // is still rejected in RESOLVE and still permitted in CONFIRM/CLOSE.
        PhasePlan plan = faqResolvePlan();
        assertTrue(ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(
                        plan, "RESOLVE", "resolve"),
                "record_outcome(resolve) in RESOLVE must still be rejected.");
        assertFalse(ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(
                        plan, "CONFIRM", "resolve"),
                "record_outcome(resolve) in CONFIRM must still be permitted.");
        assertFalse(ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(
                        plan, "CLOSE", "resolve"),
                "record_outcome(resolve) in CLOSE must still be permitted.");
    }

    // ── 3. Anti-误杀 / no-collateral cases ─────────────────────────

    @Test
    void slotRequestAnswer_withPriorGroundedTurn_staysResolve() {
        // Even with a prior grounded answer, a turn where the bot ASKS for the
        // advert ID (ASKED_FOR_SLOT) must NOT be promoted to CONFIRM — the bot
        // is still working the issue, not confirming a resolution.
        PhaseEvaluator evaluator = realEvaluator();
        AgentRunResult slotRequest = groundedAnswer(
                "Here is how to find your ad. Send the advert ID if you want me to check it.",
                List.of());

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), slotRequest, resolveSession(), List.of(priorGroundedResolveTurn()));

        assertEquals("RESOLVE", decision.nextPhase(),
                "A slot-request answer must stay in RESOLVE even with prior grounding.");
        assertEquals("progressive_resolve_stay", decision.transitionReason());
    }

    @Test
    void genuineRecordOutcomeFailure_withPriorGroundedTurn_stillRetriesInResolve() {
        // A GENUINE (non-premature) record_outcome failure — e.g. a persistence
        // error — must still route to the unchanged Sprint 9 §O1
        // record_outcome_failed_retry path, NOT the structural promotion. The
        // promotion is reserved for premature-guard rejections.
        PhaseEvaluator evaluator = realEvaluator();
        ToolEvent genuineFailure = new ToolEvent(0, 0, "record_outcome",
                Map.of("outcome_class", "resolve"),
                false, null, "record_outcome_persistence_error", 0L);
        AgentRunResult result = groundedAnswer(GROUNDED_TEXT, List.of(genuineFailure));

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), result, resolveSession(), List.of(priorGroundedResolveTurn()));

        assertEquals("RESOLVE", decision.nextPhase(),
                "A genuine record_outcome failure must keep the Sprint 9 §O1 retry behaviour.");
        assertEquals("record_outcome_failed_retry", decision.transitionReason());
    }

    @Test
    void successfulRecordOutcome_unchangedReadyToConfirmPath() {
        // A SUCCESSFUL record_outcome on this run still earns the original
        // READY_TO_CONFIRM -> CONFIRM "answer_provided" path (not the new
        // structural-promotion reason). No double-path / double-record.
        PhaseEvaluator evaluator = realEvaluator();
        ToolEvent successfulRecord = new ToolEvent(0, 0, "record_outcome",
                Map.of("outcome_class", "resolve"),
                true, Map.of("ok", true), null, 5L);
        AgentRunResult result = groundedAnswer("All set. Glad I could help.",
                List.of(successfulRecord));

        PhaseTransitionDecision decision = interpret(
                evaluator, faqResolvePlan(), result, resolveSession(), List.of(priorGroundedResolveTurn()));

        assertEquals("CONFIRM", decision.nextPhase());
        assertEquals("answer_provided", decision.transitionReason(),
                "A successful record must keep the READY_TO_CONFIRM path, not the "
                        + "structural promotion reason.");
    }

    // ── 4. Helper unit coverage ───────────────────────────────────

    @Test
    void recordOutcomeRejectedOnlyByPrematureGuard_truthTable() {
        assertTrue(PhaseEvaluator.recordOutcomeRejectedOnlyByPrematureGuard(
                        groundedAnswer(GROUNDED_TEXT, List.of(prematureRejectedRecordOutcome()))),
                "premature-only rejection -> true");
        assertFalse(PhaseEvaluator.recordOutcomeRejectedOnlyByPrematureGuard(
                        groundedAnswer(GROUNDED_TEXT, List.of(new ToolEvent(0, 0, "record_outcome",
                                Map.of("outcome_class", "resolve"), false, null,
                                "some_other_error", 0L)))),
                "non-premature failure -> false");
        assertFalse(PhaseEvaluator.recordOutcomeRejectedOnlyByPrematureGuard(
                        groundedAnswer(GROUNDED_TEXT, List.of(new ToolEvent(0, 0, "record_outcome",
                                Map.of("outcome_class", "resolve"), true, Map.of("ok", true), null, 0L)))),
                "successful record -> false");
        assertFalse(PhaseEvaluator.recordOutcomeRejectedOnlyByPrematureGuard(
                        groundedAnswer(GROUNDED_TEXT, List.of())),
                "no record_outcome attempt -> false");
    }

    @Test
    void priorGroundedResolveAnswerDelivered_truthTable() {
        assertTrue(PhaseEvaluator.priorGroundedResolveAnswerDelivered(
                        List.of(priorGroundedResolveTurn())),
                "RESOLVE turn with source_ids -> true");
        assertFalse(PhaseEvaluator.priorGroundedResolveAnswerDelivered(
                        List.of(priorDiscoverSearchTurn())),
                "DISCOVER search turn -> false");
        assertFalse(PhaseEvaluator.priorGroundedResolveAnswerDelivered(List.of()),
                "empty history -> false");
        // RESOLVE turn but no grounding (sourceIds null) -> false
        BotTurn ungrounded = BotTurn.builder().turnId("t").sessionId("s").turnIndex(0)
                .phaseAfter("RESOLVE").build();
        assertFalse(PhaseEvaluator.priorGroundedResolveAnswerDelivered(List.of(ungrounded)),
                "RESOLVE turn without source_ids -> false");
    }
}
