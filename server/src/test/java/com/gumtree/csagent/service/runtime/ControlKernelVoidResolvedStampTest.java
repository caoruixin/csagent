package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ResolveDisposition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 077 / S-Auto-22 (OQ-S77 #4 — runtime trace-contract honesty).
 *
 * <p>Pins {@link ControlKernel#shouldVoidResolvedStamp} (the COMPANION to
 * {@link ControlKernel#isResolvedSuccessTerminal}) and the
 * {@link ControlKernel#voidResolvedStamp} mutation. The rule: a prior
 * {@code containment_outcome="resolved"} stamp is downgraded to
 * {@code "incomplete_after_partial_answer"} ONLY when a later turn reaches a
 * runtime-observable unresolved failure terminal (MAX_STEPS / ERROR /
 * DEADLINE_EXCEEDED / LLM_UNAVAILABLE).
 *
 * <p>Central discipline is anti-误杀 in BOTH directions:
 * <ul>
 *   <li>a legitimately-completed resolve (FINAL_ANSWER, the goal_achieved
 *       one-shot) is NEVER voided;</li>
 *   <li>a session that genuinely failed AFTER an earlier resolved stamp IS
 *       downgraded so the trace stays internally consistent.</li>
 * </ul>
 */
class ControlKernelVoidResolvedStampTest {

    private BotSession session(String containment) {
        BotSession s = new BotSession();
        s.setSessionId("sess-void");
        s.setResolveDisposition(ResolveDisposition.ANSWERED_SUBTASK.name());
        s.setArticlesShown(new String[] {"ka44J000000gKxqQAE"});
        s.setContainmentOutcome(containment);
        return s;
    }

    private AgentRunResult finalAnswer() {
        return AgentRunResult.finalAnswer(
                "Your ad was removed because it breached our posting rules.",
                List.of(), List.of());
    }

    // ---- direction (a): resolved + runtime failure terminal -> void ----

    @Test
    void resolved_plus_maxSteps_voids() {
        BotSession s = session("resolved");
        AgentRunResult r = AgentRunResult.maxSteps(List.of(), List.of());
        assertTrue(ControlKernel.shouldVoidResolvedStamp(s, r),
                "a prior resolved stamp invalidated by a later MAX_STEPS terminal must void");
    }

    @Test
    void resolved_plus_error_voids() {
        BotSession s = session("resolved");
        AgentRunResult r = AgentRunResult.error("dispatcher blew up");
        assertTrue(ControlKernel.shouldVoidResolvedStamp(s, r));
    }

    @Test
    void resolved_plus_deadlineExceeded_voids() {
        BotSession s = session("resolved");
        AgentRunResult r = AgentRunResult.deadlineExceeded(
                "llm wall-clock exceeded", List.of(), List.of(), null);
        assertTrue(ControlKernel.shouldVoidResolvedStamp(s, r));
    }

    @Test
    void resolved_plus_llmUnavailable_voids() {
        BotSession s = session("resolved");
        AgentRunResult r = AgentRunResult.llmUnavailable(
                "provider chain exhausted", List.of(), List.of(), null);
        assertTrue(ControlKernel.shouldVoidResolvedStamp(s, r));
    }

    // ---- direction (b): legitimate completion / wrong stamp -> NEVER void ----

    @Test
    void resolved_plus_finalAnswer_neverVoids() {
        // The goal_achieved one-shot path: a FINAL_ANSWER resolve must keep its
        // resolved stamp (isResolvedSuccessTerminal stamped it; this companion
        // must not undo it).
        BotSession s = session("resolved");
        assertFalse(ControlKernel.shouldVoidResolvedStamp(s, finalAnswer()),
                "a legitimate FINAL_ANSWER resolve must never be voided");
    }

    @Test
    void resolved_plus_escalate_neverVoids() {
        BotSession s = session("resolved");
        AgentRunResult esc = AgentRunResult.escalate(
                "trust_safety_required", List.of(), List.of());
        assertFalse(ControlKernel.shouldVoidResolvedStamp(s, esc),
                "ESCALATE is not a runtime failure terminal for this guard");
    }

    @Test
    void resolved_plus_clarification_neverVoids() {
        BotSession s = session("resolved");
        AgentRunResult clar = AgentRunResult.clarification(
                "Which advert do you mean?", List.of(), List.of(), null, null);
        assertFalse(ControlKernel.shouldVoidResolvedStamp(s, clar));
    }

    @Test
    void escalated_containment_neverVoids() {
        // Only a "resolved" stamp can be voided -- an escalated terminal is left
        // untouched even on a failure terminal.
        BotSession s = session("escalated");
        AgentRunResult r = AgentRunResult.error("boom");
        assertFalse(ControlKernel.shouldVoidResolvedStamp(s, r),
                "only a prior 'resolved' stamp is voidable; 'escalated' is left untouched");
    }

    @Test
    void blankContainment_neverVoids() {
        BotSession s = session(null);
        AgentRunResult r = AgentRunResult.error("boom");
        assertFalse(ControlKernel.shouldVoidResolvedStamp(s, r),
                "a blank containment was never stamped resolved; nothing to void");
    }

    @Test
    void downgradedContainment_isNotReVoided() {
        // Idempotence: once downgraded the value is no longer "resolved" so a
        // subsequent failure terminal does not re-trigger.
        BotSession s = session(ControlKernel.CONTAINMENT_INCOMPLETE_AFTER_PARTIAL_ANSWER);
        AgentRunResult r = AgentRunResult.error("boom");
        assertFalse(ControlKernel.shouldVoidResolvedStamp(s, r));
    }

    @Test
    void nullArgsAreSafe() {
        assertFalse(ControlKernel.shouldVoidResolvedStamp(null, finalAnswer()));
        assertFalse(ControlKernel.shouldVoidResolvedStamp(session("resolved"), null));
    }

    // ---- the mutation records provenance and writes the honest value ----

    @Test
    void voidResolvedStamp_preservesProvenance_andDowngrades() {
        BotSession s = session("resolved");
        ControlKernel.voidResolvedStamp(s);
        assertEquals(ControlKernel.CONTAINMENT_INCOMPLETE_AFTER_PARTIAL_ANSWER,
                s.getContainmentOutcome(),
                "containment is overwritten with the honest incomplete value");
        assertEquals("resolved", s.getPriorContainmentOutcome(),
                "the prior 'resolved' value is preserved as provenance");
    }
}
