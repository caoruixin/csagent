package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // ---------------- Sprint §B1: distress detector ----------------

    @Test
    void detectDistress_youAreNotHelping() {
        assertTrue(resolver.detectDistressSignal("you are not helping at all"));
    }

    @Test
    void detectDistress_yourNoHelping_typo() {
        // cs_interactive_029 verbatim — "YOUR NO HELPING AT ALL".
        assertTrue(resolver.detectDistressSignal("YOUR NO HELPING AT ALL"));
    }

    @Test
    void detectDistress_noOneIsHelping() {
        assertTrue(resolver.detectDistressSignal("no one is helping me with this"));
        assertTrue(resolver.detectDistressSignal("nobody helps"));
    }

    @Test
    void detectDistress_followedYourSoCalledProcess() {
        // cs_interactive_029 verbatim signal.
        assertTrue(resolver.detectDistressSignal(
                "I HAVE ALREADY FOLLOWED YOUR SO CALLED PROCESS"));
        assertTrue(resolver.detectDistressSignal(
                "i've followed the process and nothing happened"));
    }

    @Test
    void detectDistress_thisIsRidiculous() {
        assertTrue(resolver.detectDistressSignal("this is ridiculous"));
        assertTrue(resolver.detectDistressSignal("this is absurd"));
        assertTrue(resolver.detectDistressSignal("this is a joke"));
    }

    @Test
    void detectDistress_howLongDoIHaveToWait() {
        // cs_interactive_002 seed message.
        assertTrue(resolver.detectDistressSignal(
                "How long do I have to wait to sort this out?"));
    }

    @Test
    void detectDistress_sinceDayOne() {
        assertTrue(resolver.detectDistressSignal(
                "Its been this way since day 1."));
    }

    @Test
    void detectDistress_allCapsShout() {
        // ≥ 8 letters and ≥ 70% uppercase.
        assertTrue(resolver.detectDistressSignal("HI MY ACCOUNT IS LOCKED"));
        assertTrue(resolver.detectDistressSignal("PLEASE FIX THIS NOW"));
    }

    @Test
    void detectDistress_normalQuestion_returnsFalse() {
        assertFalse(resolver.detectDistressSignal(
                "How do I change my email address on the app?"));
        assertFalse(resolver.detectDistressSignal("Thank you"));
    }

    @Test
    void detectDistress_shortAcronymNotShout() {
        // Three-letter all-caps tokens like FAQ / UK / NHS must NOT
        // qualify — the threshold is 8 letters minimum.
        assertFalse(resolver.detectDistressSignal("FAQ"));
        assertFalse(resolver.detectDistressSignal("Hi"));
        assertFalse(resolver.detectDistressSignal("OK"));
    }

    @Test
    void detectDistress_blankInput_returnsFalse() {
        assertFalse(resolver.detectDistressSignal(null));
        assertFalse(resolver.detectDistressSignal(""));
        assertFalse(resolver.detectDistressSignal("   "));
    }

    // ---------------- Sprint §B1: precedence integration ----------------

    @Test
    void userDistressBeatsFaqMissThresholdExceeded() {
        // cs_interactive_002 / cs_014: distress detector stamps
        // user_distress; the FAQ-miss budget close-out cannot overwrite it.
        assertEquals("user_distress",
                resolver.resolve("user_distress", "faq_miss_threshold_exceeded"));
    }

    @Test
    void userDistressBeatsTurnBudget_andClarification() {
        assertEquals("user_distress",
                resolver.resolve("user_distress", "turn_budget_exhausted"));
        assertEquals("user_distress",
                resolver.resolve("user_distress", "clarification_budget_exhausted"));
    }

    @Test
    void userRequestedStillBeatsUserDistress() {
        // Tier 0 priority: an explicit "speak to a human" request
        // outranks distress (cs_interactive_029 expects user_requested
        // even though distress signals are also present).
        assertEquals("user_requested",
                resolver.resolve("user_distress", "user_requested"));
        assertEquals("user_requested",
                resolver.resolve("user_requested", "user_distress"));
    }

    // ---------------- Sprint 096 / S-Auto-44 (M-Auto-9 WP1) ----------------
    // agent_unable_to_resolve: the bot-initiated, in-scope,
    // exhausted-resolution, unresolved-handover reason. It is the
    // LOWEST-priority reason (priority 50), so EVERY other genuinely-present
    // canonical reason wins over it in resolve(), in BOTH orders. It is a
    // SEMANTIC reason, not a terminal-close.

    /** §3 / §7 — canonical identity: returns as-is, not coerced. */
    @Test
    void agentUnableToResolve_isCanonical() {
        assertEquals("agent_unable_to_resolve",
                resolver.canonicalize("agent_unable_to_resolve"));
        assertTrue(EscalationReasonResolver.CANONICAL_REASONS.contains("agent_unable_to_resolve"));
    }

    /** §2-class characterization: when nothing higher is present, the
     *  LLM-selected bot-initiated reason survives resolve() unchanged. */
    @Test
    void agentUnableToResolve_survivesWhenNothingHigherPresent() {
        assertEquals("agent_unable_to_resolve",
                resolver.resolve(null, "agent_unable_to_resolve"));
        assertEquals("agent_unable_to_resolve",
                resolver.resolve("agent_unable_to_resolve", "agent_unable_to_resolve"));
    }

    /** §6 — it is a SEMANTIC reason, NOT a terminal-close, despite its
     *  numerically-low priority sitting past the budget family. */
    @Test
    void agentUnableToResolve_isNotTerminalClose() {
        assertFalse(resolver.isTerminalCloseReason("agent_unable_to_resolve"));
    }

    /** §6 — it is the strictly lowest-priority reason (highest number). */
    @Test
    void agentUnableToResolve_hasLowestPriority() {
        int p = EscalationReasonResolver.priorityOf("agent_unable_to_resolve");
        assertEquals(50, p);
        for (String other : EscalationReasonResolver.CANONICAL_REASONS) {
            if (other.equals("agent_unable_to_resolve")) {
                continue;
            }
            assertTrue(EscalationReasonResolver.priorityOf(other) < p,
                    other + " (priority " + EscalationReasonResolver.priorityOf(other)
                            + ") must outrank agent_unable_to_resolve (50)");
        }
    }

    /**
     * §6 precedence proof — every higher-priority reason WINS over
     * agent_unable_to_resolve in BOTH orders (existing session reason vs
     * incoming candidate). Covers genuine user signal, safety/dispute/
     * compliance, intake/scope, infra, and the budget family. The new
     * value can never displace any of them.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            // Tier 0 — genuine user signal
            "imminent_harm", "user_requested", "user_distress",
            // Tier 1 — safety / dispute / compliance
            "trust_safety_required", "payment_dispute_detected",
            "appeal_requires_human", "incorrect_deletion_appeal",
            "gdpr_intake", "identity_verification_required", "account_compliance",
            // Tier 2 — intake / scope
            "intake_complete_for_uc_g", "intake_complete_for_uc_h",
            "intake_complete_for_uc_i", "intake_complete_for_uc_j",
            "intake_complete_for_uc_k", "incomplete_intake",
            "out_of_scope", "tool_scope_blocked",
            // Tier 3 — infra
            "service_degraded", "runtime_error_threshold",
            // Tier 4 — budget / terminal-close
            "clarification_budget_exhausted", "faq_miss_threshold_exceeded",
            "turn_budget_exhausted"
    })
    void everyOtherReasonBeatsAgentUnableToResolve_bothOrders(String higher) {
        // candidate = higher, existing = new value -> higher upgrades.
        assertEquals(higher,
                resolver.resolve("agent_unable_to_resolve", higher),
                higher + " must beat agent_unable_to_resolve (candidate order)");
        // existing = higher, candidate = new value -> higher is kept.
        assertEquals(higher,
                resolver.resolve(higher, "agent_unable_to_resolve"),
                higher + " must beat agent_unable_to_resolve (existing order)");
    }
}
