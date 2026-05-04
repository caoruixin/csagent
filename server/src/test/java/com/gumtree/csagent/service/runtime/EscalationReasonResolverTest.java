package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 2026-05-04 §A1: deterministic precedence for escalation_reason.
 * The resolver must:
 *   - keep an existing higher-priority semantic reason when a lower-priority
 *     candidate (e.g. turn_budget_exhausted) arrives;
 *   - upgrade to a higher-priority candidate when the existing is lower;
 *   - canonicalize legacy literals (user_requested_escalation, drift_hard_shift,
 *     etc.) into the 23-value enum used by the L1 trace contract;
 *   - detect explicit user-driven escalation patterns (callback / "speak to a
 *     human") on a per-turn basis so {@link ControlKernel} can stamp
 *     ``user_requested`` BEFORE the budget check fires.
 */
class EscalationReasonResolverTest {

    private final EscalationReasonResolver resolver = new EscalationReasonResolver();

    // ---------------- precedence (resolve) ----------------

    @Test
    void userRequestedBeatsTurnBudgetExhausted() {
        // cs_interactive_029 shape: user asked for callback first, budget
        // check fired later — budget must NOT overwrite user_requested.
        assertEquals("user_requested",
                resolver.resolve("user_requested", "turn_budget_exhausted"));
    }

    @Test
    void userRequestedBeatsClarificationBudgetExhausted() {
        assertEquals("user_requested",
                resolver.resolve("user_requested", "clarification_budget_exhausted"));
    }

    @Test
    void userRequestedBeatsFaqMissThresholdExceeded() {
        assertEquals("user_requested",
                resolver.resolve("user_requested", "faq_miss_threshold_exceeded"));
    }

    @Test
    void userDistressBeatsTurnBudgetExhausted() {
        assertEquals("user_distress",
                resolver.resolve("user_distress", "turn_budget_exhausted"));
    }

    @Test
    void imminentHarmBeatsEverything() {
        assertEquals("imminent_harm",
                resolver.resolve("trust_safety_required", "imminent_harm"));
        assertEquals("imminent_harm",
                resolver.resolve("imminent_harm", "user_requested"));
    }

    @Test
    void trustSafetyRequiredBeatsBudget() {
        assertEquals("trust_safety_required",
                resolver.resolve("trust_safety_required", "turn_budget_exhausted"));
    }

    @Test
    void intakeCompleteBeatsServiceDegraded() {
        assertEquals("intake_complete_for_uc_k",
                resolver.resolve("intake_complete_for_uc_k", "service_degraded"));
    }

    @Test
    void clarificationBudgetBeatsPlainTurnBudget() {
        // Both budget close-outs, but clarification is the more specific
        // reason per Phase 2 §2.4 — must win.
        assertEquals("clarification_budget_exhausted",
                resolver.resolve("turn_budget_exhausted", "clarification_budget_exhausted"));
    }

    @Test
    void candidateUpgradesLowerPriorityExisting() {
        // session.escalationReason was set to turn_budget_exhausted; the
        // user then explicitly asks for a callback. The candidate must win.
        assertEquals("user_requested",
                resolver.resolve("turn_budget_exhausted", "user_requested"));
    }

    @Test
    void nullExistingReturnsCandidate() {
        assertEquals("user_requested",
                resolver.resolve(null, "user_requested"));
    }

    @Test
    void nullCandidateKeepsExisting() {
        assertEquals("user_requested",
                resolver.resolve("user_requested", null));
    }

    @Test
    void blankBothReturnsNull() {
        assertNull(resolver.resolve("", ""));
        assertNull(resolver.resolve(null, null));
    }

    @Test
    void tieKeepsExistingForStability() {
        // Equal priority => existing wins so a repeated stamp is a no-op.
        assertEquals("turn_budget_exhausted",
                resolver.resolve("turn_budget_exhausted", "turn_budget_exhausted"));
    }

    // ---------------- canonicalize ----------------

    @Test
    void canonicalize_returnsEnumValueAsIs() {
        assertEquals("user_requested", resolver.canonicalize("user_requested"));
        assertEquals("intake_complete_for_uc_k",
                resolver.canonicalize("intake_complete_for_uc_k"));
        assertEquals("turn_budget_exhausted",
                resolver.canonicalize("turn_budget_exhausted"));
    }

    @Test
    void canonicalize_mapsLegacyUserRequestedEscalation() {
        // Old runtime literal — must collapse onto the canonical user_requested
        // enum so the L1 escalation_reason_consistency check stays green.
        assertEquals("user_requested", resolver.canonicalize("user_requested_escalation"));
        assertEquals("user_requested", resolver.canonicalize("callback_requested"));
    }

    @Test
    void canonicalize_mapsLegacyDriftAndUnknownToServiceDegraded() {
        assertEquals("service_degraded", resolver.canonicalize("drift_hard_shift"));
        assertEquals("service_degraded", resolver.canonicalize("agent_error"));
        assertEquals("service_degraded", resolver.canonicalize("system_failure"));
        assertEquals("service_degraded", resolver.canonicalize("llm_determined_escalation"));
    }

    @Test
    void canonicalize_mapsArbitraryNonCanonicalToServiceDegraded() {
        assertEquals("service_degraded", resolver.canonicalize("nope_not_a_real_reason"));
    }

    @Test
    void canonicalize_returnsNullForBlank() {
        assertNull(resolver.canonicalize(null));
        assertNull(resolver.canonicalize(""));
        assertNull(resolver.canonicalize("   "));
    }

    // ---------------- detectExplicitUserEscalation ----------------

    @Test
    void detectExplicit_callMeNow() {
        assertTrue(resolver.detectExplicitUserEscalation("can you please call me now?"));
    }

    @Test
    void detectExplicit_callBack() {
        assertTrue(resolver.detectExplicitUserEscalation("I want a call back"));
    }

    @Test
    void detectExplicit_phoneMe() {
        assertTrue(resolver.detectExplicitUserEscalation("phone me on my mobile"));
    }

    @Test
    void detectExplicit_callback() {
        assertTrue(resolver.detectExplicitUserEscalation("can I get a callback please"));
    }

    @Test
    void detectExplicit_speakToAHuman() {
        assertTrue(resolver.detectExplicitUserEscalation("I want to speak to a human"));
    }

    @Test
    void detectExplicit_transferMeToAnAgent() {
        assertTrue(resolver.detectExplicitUserEscalation("Transfer me to an agent"));
    }

    @Test
    void detectExplicit_canSomeoneRingMe() {
        assertTrue(resolver.detectExplicitUserEscalation("Can someone ring me back"));
    }

    @Test
    void detectExplicit_normalQuestion_returnsFalse() {
        assertFalse(resolver.detectExplicitUserEscalation(
                "How do I receive notifications when buyers reply?"));
    }

    @Test
    void detectExplicit_blankInput_returnsFalse() {
        assertFalse(resolver.detectExplicitUserEscalation(null));
        assertFalse(resolver.detectExplicitUserEscalation(""));
        assertFalse(resolver.detectExplicitUserEscalation("   "));
    }

    @Test
    void detectExplicit_caseInsensitive() {
        assertTrue(resolver.detectExplicitUserEscalation("CAN YOU PLEASE CALL ME NOW"));
    }

    // ---------------- terminal-close classification ----------------

    @Test
    void isTerminalCloseReason_recognisesBudgetReasons() {
        assertTrue(resolver.isTerminalCloseReason("turn_budget_exhausted"));
        assertTrue(resolver.isTerminalCloseReason("clarification_budget_exhausted"));
        assertTrue(resolver.isTerminalCloseReason("faq_miss_threshold_exceeded"));
    }

    @Test
    void isTerminalCloseReason_falseForSemanticReasons() {
        assertFalse(resolver.isTerminalCloseReason("user_requested"));
        assertFalse(resolver.isTerminalCloseReason("trust_safety_required"));
        assertFalse(resolver.isTerminalCloseReason("intake_complete_for_uc_k"));
        assertFalse(resolver.isTerminalCloseReason("service_degraded"));
    }

    @Test
    void isTerminalCloseReason_falseForBlank() {
        assertFalse(resolver.isTerminalCloseReason(null));
        assertFalse(resolver.isTerminalCloseReason(""));
    }
}
