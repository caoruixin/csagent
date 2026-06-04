package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.AgentRunResult;
import com.gumtree.csagent.model.BotSession;
import com.gumtree.csagent.model.ResolveDisposition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 074 / S-Auto-19 (#1 runtime — trace-contract completion).
 *
 * <p>Pins {@link ControlKernel#isResolvedSuccessTerminal} — the gate that
 * decides whether the agent loop should stamp {@code
 * containment_outcome="resolved"} on a one-shot grounded-answer terminal
 * that did not route through a dedicated record_outcome-only CLOSE turn.
 *
 * <p>The central discipline is anti-误杀: the gate must ONLY fire on a
 * genuinely resolved success terminal (FINAL_ANSWER + READY_TO_CONFIRM) and
 * must NEVER fire on an unresolved terminal (MAX_STEPS / ERROR / escalation /
 * progressive RESOLVE). The static helper is pinned directly so the rule is
 * verifiable without the full Spring context.
 */
class ControlKernelResolvedSuccessTerminalTest {

    private BotSession session(ResolveDisposition disposition, String containment) {
        BotSession s = new BotSession();
        s.setSessionId("sess-st");
        if (disposition != null) {
            s.setResolveDisposition(disposition.name());
        }
        s.setContainmentOutcome(containment);
        return s;
    }

    private AgentRunResult finalAnswer() {
        return AgentRunResult.finalAnswer(
                "Your ad was removed because it breached our posting rules.",
                List.of(), List.of());
    }

    // ---- characterization: a real one-shot resolved terminal stamps resolved

    @Test
    void finalAnswer_readyToConfirm_blankContainment_isResolvedTerminal() {
        BotSession s = session(ResolveDisposition.READY_TO_CONFIRM, null);
        assertTrue(ControlKernel.isResolvedSuccessTerminal(s, finalAnswer()),
                "FINAL_ANSWER + READY_TO_CONFIRM + blank containment is a resolved success terminal");
    }

    // ---- anti-误杀 counter-tests: unresolved terminals NEVER stamp resolved

    @Test
    void maxSteps_neverResolved() {
        BotSession s = session(ResolveDisposition.READY_TO_CONFIRM, null);
        AgentRunResult maxSteps = AgentRunResult.maxSteps(List.of(), List.of());
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, maxSteps),
                "MAX_STEPS is an unresolved budget terminal and must never stamp resolved");
    }

    @Test
    void error_neverResolved() {
        BotSession s = session(ResolveDisposition.READY_TO_CONFIRM, null);
        AgentRunResult error = AgentRunResult.error("dispatcher blew up");
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, error),
                "ERROR is an infra failure terminal and must never stamp resolved");
    }

    @Test
    void escalate_neverResolved() {
        BotSession s = session(ResolveDisposition.ESCALATE, null);
        AgentRunResult esc = AgentRunResult.escalate(
                "trust_safety_required", List.of(), List.of());
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, esc),
                "ESCALATE terminal must never stamp resolved (escalation stamps 'escalated')");
    }

    @Test
    void clarification_neverResolved() {
        BotSession s = session(ResolveDisposition.ASKED_FOR_SLOT, null);
        AgentRunResult clar = AgentRunResult.clarification(
                "Which advert do you mean?", List.of(), List.of(), null, null);
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, clar),
                "CLARIFICATION_NEEDED is not a terminal resolution");
    }

    @Test
    void finalAnswer_askedForSlot_notResolved() {
        // FINAL_ANSWER but the disposition is a progressive slot-ask: the
        // bot delivered text but the issue is NOT resolved yet.
        BotSession s = session(ResolveDisposition.ASKED_FOR_SLOT, null);
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, finalAnswer()),
                "FINAL_ANSWER with ASKED_FOR_SLOT is progressive, not a resolved terminal");
    }

    @Test
    void finalAnswer_continueResolve_notResolved() {
        BotSession s = session(ResolveDisposition.CONTINUE_RESOLVE, null);
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, finalAnswer()),
                "FINAL_ANSWER with CONTINUE_RESOLVE is progressive, not a resolved terminal");
    }

    @Test
    void existingContainmentNeverOverwritten() {
        // Even a textbook resolved-terminal shape must not fire when a
        // containment value (e.g. escalated) is already stamped.
        BotSession s = session(ResolveDisposition.READY_TO_CONFIRM, "escalated");
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, finalAnswer()),
                "a pre-existing containment_outcome must never be overwritten");
    }

    @Test
    void nullArgsAreSafe() {
        assertFalse(ControlKernel.isResolvedSuccessTerminal(null, finalAnswer()));
        assertFalse(ControlKernel.isResolvedSuccessTerminal(
                session(ResolveDisposition.READY_TO_CONFIRM, null), null));
    }

    @Test
    void nullDisposition_notResolved() {
        BotSession s = session(null, null);
        assertFalse(ControlKernel.isResolvedSuccessTerminal(s, finalAnswer()),
                "no resolve_disposition recorded -> not a confirmed resolved terminal");
    }

    // -----------------------------------------------------------------
    // S-Auto-19 (#2 runtime) — resolveAnswerTurnSourceIds: the answer turn
    // carries the session's resolved grounding when it retrieved nothing
    // itself; a turn with no session grounding carries none.
    // -----------------------------------------------------------------

    @Test
    void answerTurn_fromPriorRetrieval_carriesSessionResolvedIds() {
        // This turn ran no search_knowledge (empty per-turn ids), but the
        // session has accumulated grounding from a prior retrieval turn.
        String[] sessionResolved = {"ka44J000000gKv5QAE", "ka4P200000004ZtIAI"};
        String[] out = ControlKernel.resolveAnswerTurnSourceIds(
                new String[0], sessionResolved,
                "Your ad was removed because it breached our posting rules.",
                "CONFIRM");
        org.junit.jupiter.api.Assertions.assertArrayEquals(sessionResolved, out,
                "answer turn composed from a prior retrieval turn carries the session's resolved source ids");
    }

    @Test
    void answerTurn_nullPerTurn_fromPriorRetrieval_carriesSessionResolvedIds() {
        String[] sessionResolved = {"ka44J000000gKv5QAE"};
        String[] out = ControlKernel.resolveAnswerTurnSourceIds(
                null, sessionResolved,
                "Here is what happened to your advert.",
                "RESOLVE");
        org.junit.jupiter.api.Assertions.assertArrayEquals(sessionResolved, out);
    }

    @Test
    void perTurnSourceIds_neverOverwritten() {
        // When this turn retrieved its own sources, they win — no fallback.
        String[] thisTurn = {"ka_this_turn"};
        String[] sessionResolved = {"ka_prior"};
        String[] out = ControlKernel.resolveAnswerTurnSourceIds(
                thisTurn, sessionResolved, "An answer.", "CONFIRM");
        org.junit.jupiter.api.Assertions.assertArrayEquals(thisTurn, out,
                "a non-empty per-turn source set is authoritative and is never overwritten");
    }

    @Test
    void noSessionGrounding_carriesNone() {
        // A genuinely never-searched answer (no per-turn ids, no session
        // grounding) carries no sources.
        String[] out = ControlKernel.resolveAnswerTurnSourceIds(
                new String[0], new String[0],
                "Your ad was removed because of a payment problem.",
                "CONFIRM");
        assertTrue(out == null || out.length == 0,
                "an answer turn with no session grounding carries no source ids");
    }

    @Test
    void escalationTurn_doesNotInheritGrounding() {
        // Escalation/handover turns make no grounding claim -> keep none even
        // when the session accumulated source ids.
        String[] sessionResolved = {"ka_prior"};
        String[] out = ControlKernel.resolveAnswerTurnSourceIds(
                new String[0], sessionResolved,
                "I'm connecting you with a human agent.", "ESCALATE");
        assertTrue(out == null || out.length == 0,
                "an ESCALATE turn does not inherit session grounding");
    }

    @Test
    void blankAnswer_doesNotInheritGrounding() {
        String[] sessionResolved = {"ka_prior"};
        String[] out = ControlKernel.resolveAnswerTurnSourceIds(
                new String[0], sessionResolved, "   ", "CONFIRM");
        assertTrue(out == null || out.length == 0,
                "a blank-reply turn does not inherit session grounding");
    }
}
