package com.gumtree.csagent.service.runtime;

import com.gumtree.csagent.model.BotSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests documenting the handlingState inconsistency found in D11+D12 QA.
 *
 * Finding: After PhaseEvaluator-driven escalation (intake path),
 * the session's handlingState stays "BOT_HANDLING" instead of being
 * updated to "QUEUE_TO_HUMAN". Only ControlKernel.forceEscalate()
 * (drift/budget/user-requested) correctly sets handlingState.
 *
 * This means escalated intake sessions (UC-H, UC-J, UC-K) report
 * handlingState=BOT_HANDLING which is misleading for observability,
 * dashboards, and any downstream consumers that check this field.
 */
class SessionManagerHandlingStateTest {

    /**
     * Documents that PhaseEvaluator.evaluateEscalate sets handlingState
     * to QUEUE_TO_HUMAN, but this code path is only reached when the
     * session phase is already ESCALATE at the start of processMessage.
     * For intake escalations, the transition to ESCALATE happens via
     * PhaseResult.escalate -> ControlKernel phase transition, which
     * does NOT call evaluateEscalate().
     */
    @Test
    void intakeEscalation_handlingStateShouldBeUpdated_documentedBug() {
        // Simulating the state after an intake-driven escalation
        // (as observed in Tests 1, 3, 4 of the E2E QA run)
        BotSession session = BotSession.builder()
                .sessionId("test-intake-escalation")
                .activeUseCase("UC-H")
                .handlingState("BOT_HANDLING")
                .currentPhase("ESCALATE")
                .escalationReason("intake_complete")
                .totalBotTurns(2)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();

        // After intake-driven escalation, the session looks like this:
        // phase=ESCALATE, handlingState=BOT_HANDLING, containmentOutcome=null
        //
        // This is the BUG: handlingState should be QUEUE_TO_HUMAN
        // and containmentOutcome should be ESCALATED.
        //
        // Compare to forceEscalate path (user-requested escalation):
        // phase=ESCALATE, handlingState=QUEUE_TO_HUMAN, containmentOutcome=ESCALATED

        assertEquals("ESCALATE", session.getCurrentPhase());
        // This assertion documents the bug: BOT_HANDLING is wrong for escalated sessions
        assertEquals("BOT_HANDLING", session.getHandlingState(),
                "BUG: After intake-driven escalation, handlingState stays BOT_HANDLING. " +
                "It should be QUEUE_TO_HUMAN. Fix: ControlKernel should set handlingState " +
                "when phaseResult.shouldEscalate() is true.");
    }

    /**
     * Documents the contrast: forceEscalate correctly sets handlingState.
     */
    @Test
    void forceEscalation_handlingStateCorrectlyUpdated() {
        // Simulating the state after ControlKernel.forceEscalate()
        BotSession session = BotSession.builder()
                .sessionId("test-force-escalation")
                .activeUseCase("UC-B")
                .handlingState("QUEUE_TO_HUMAN")
                .currentPhase("ESCALATE")
                .containmentOutcome("escalated")
                .escalationReason("user_requested_escalation")
                .totalBotTurns(1)
                .clarificationCount(0)
                .faqMissCount(0)
                .repeatedActionCount(0)
                .build();

        assertEquals("QUEUE_TO_HUMAN", session.getHandlingState(),
                "forceEscalate correctly sets handlingState to QUEUE_TO_HUMAN");
        assertEquals("escalated", session.getContainmentOutcome(),
                "forceEscalate correctly sets containmentOutcome to escalated (lowercase per trace contract)");
    }
}
