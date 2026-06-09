package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — R2.a #5 phase-aware
 * {@code max-repeated-same-action} → {@code clarification_budget_exhausted}
 * re-map, EXTENDED at Sprint 080 / S-Auto-25 Sub-sprint C-1 (R2.a#5-ext) to
 * also cover the RESOLVE-INTAKE free-text clarification path (c14).
 *
 * <p>Anti-误杀 invariant #12: the {@code max-repeated-same-action} budget is
 * NOT DISCOVER-only ({@code ControlKernel.trackRepeatedAction} fires on any
 * repeated action / tool-call in any phase), so the re-map MUST be phase- AND
 * UC-aware. A genuine RESOLVE repeated-TOOL-call budget, and a RESOLVE
 * non-intake UC, both keep the generic {@code turn_budget_exhausted}; only a
 * free-text clarification repetition in DISCOVER, or in RESOLVE on an intake
 * UC, is relabeled. No new enum value is introduced — the re-map reuses the
 * existing {@code clarification_budget_exhausted} member.
 */
class MapBudgetToClarificationLabelTest {

    /** The only three values {@code mapBudgetToEscalationReason} can return,
     *  all members of the canonical 23-value escalation_reason enum. */
    private static final Set<String> CANONICAL_BUDGET_REASONS = Set.of(
            "turn_budget_exhausted",
            "clarification_budget_exhausted",
            "faq_miss_threshold_exceeded");

    // --- #5 positive: DISCOVER free-text clarification repetition relabeled ---

    @Test
    void discoverFreeTextAnswerRepetition_relabeledToClarification() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", "answer", null));
    }

    @Test
    void discoverLegacyClarifyKeyRepetition_relabeledToClarification() {
        // The legacy PhaseEvaluator.deriveRepetitionKey emits "clarify".
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", "clarify", null));
    }

    @Test
    void discoverPhaseLabelIsCaseInsensitive() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "discover", "answer", null));
    }

    // --- R2.a#5-ext positive: RESOLVE-INTAKE free-text clarification relabeled ---

    @Test
    void resolveIntakeFreeTextAnswerRepetition_relabeledToClarification() {
        // c14 shape: UC-J intake collection in RESOLVE, repeated free-text
        // clarification question → clarification budget, not turn budget.
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "RESOLVE", "answer", "UC-J"));
    }

    @Test
    void resolveIntakeLegacyClarifyKeyRepetition_relabeledToClarification() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "RESOLVE", "clarify", "UC-J"));
    }

    @Test
    void resolveIntakePhaseLabelIsCaseInsensitive() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "resolve", "answer", "UC-G"));
    }

    @Test
    void resolveIntakeAllIntakeUcs_relabeledToClarification() {
        for (String uc : new String[] {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}) {
            assertEquals("clarification_budget_exhausted",
                    ControlKernel.mapBudgetToEscalationReason(
                            "max-repeated-same-action", "RESOLVE", "answer", uc),
                    "intake UC " + uc + " should relabel in RESOLVE");
        }
    }

    // --- R2.a#5-ext negative (anti-误杀 #12): RESOLVE non-intake UC unchanged ---

    @Test
    void resolveNonIntakeUcFreeText_keepsTurnBudgetExhausted() {
        // RESOLVE-FAQ UC (UC-A) doing a free-text answer is NOT intake
        // clarification — it must keep the generic reason.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "RESOLVE", "answer", "UC-A"));
    }

    @Test
    void resolveNullActiveUcFreeText_keepsTurnBudgetExhausted() {
        // No committed UC in RESOLVE → cannot be an intake clarification.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "RESOLVE", "answer", null));
    }

    @Test
    void resolveIntakeUcRepeatedToolCall_keepsTurnBudgetExhausted() {
        // Even on an intake UC, a repeated TOOL call (not free-text) stays
        // turn_budget_exhausted — only free-text clarification is relabeled.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "RESOLVE", "search_knowledge", "UC-J"));
    }

    // --- #5 negative (multi-phase guard): non-DISCOVER must NOT be relabeled ---

    @Test
    void resolvePhaseRepeatedToolCall_keepsTurnBudgetExhausted() {
        // RESOLVE repeated search_knowledge (a tool call) must keep the
        // generic reason — relabeling it would be a new false-label artifact.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "RESOLVE", "search_knowledge", "UC-A"));
    }

    @Test
    void intakeLiteralPhaseRepeatedAction_keepsTurnBudgetExhausted() {
        // The literal phase string "INTAKE" is neither DISCOVER nor RESOLVE, so
        // even an intake UC there falls through to turn_budget_exhausted.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "INTAKE", "answer", "UC-J"));
    }

    // --- #5 negative (free-text guard): DISCOVER repeated TOOL call NOT relabeled ---

    @Test
    void discoverPhaseRepeatedToolCall_keepsTurnBudgetExhausted() {
        // Even in DISCOVER, a repeated classify_use_case (tool) is not a
        // free-text clarification, so it stays turn_budget_exhausted.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", "classify_use_case", null));
    }

    @Test
    void nullLastActionInDiscover_keepsTurnBudgetExhausted() {
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", null, null));
    }

    // --- #5 negative (other budgets unchanged, regardless of phase / action) ---

    @Test
    void maxClarificationRoundsBudget_unchangedInDiscover() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-clarification-rounds", "DISCOVER", "answer", null));
    }

    @Test
    void maxFaqMissBudget_unchanged() {
        assertEquals("faq_miss_threshold_exceeded",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-faq-miss", "DISCOVER", "answer", null));
    }

    @Test
    void maxFaqMissBudget_inResolveIntake_unchanged() {
        // R2.a#5-ext must not touch other budgets even on an intake UC.
        assertEquals("faq_miss_threshold_exceeded",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-faq-miss", "RESOLVE", "answer", "UC-J"));
    }

    @Test
    void maxTotalBotTurnsBudget_inDiscover_staysTurnBudgetExhausted() {
        // A non-clarification budget that happens to fire in DISCOVER on a
        // free-text turn must NOT be relabeled — only max-repeated-same-action
        // is re-mapped.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-total-bot-turns", "DISCOVER", "answer", null));
    }

    @Test
    void nullBucket_staysTurnBudgetExhausted() {
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(null, "DISCOVER", "answer", null));
    }

    // --- #5 negative (no new enum): every return is a canonical member ---

    @Test
    void resolveIntakeRelabel_returnsCanonicalEnumMember() {
        String reason = ControlKernel.mapBudgetToEscalationReason(
                "max-repeated-same-action", "RESOLVE", "answer", "UC-J");
        assertTrue(CANONICAL_BUDGET_REASONS.contains(reason),
                "R2.a#5-ext must reuse an existing enum value, got: " + reason);
    }

    // --- Single-arg overload (phase-unaware default) is unchanged ---

    @Test
    void singleArgOverload_maxRepeatedSameAction_staysTurnBudgetExhausted() {
        // The existing ControlKernelEscalationReasonTest locks this; re-assert
        // here so the new overload cannot drift the legacy default.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason("max-repeated-same-action"));
    }
}
