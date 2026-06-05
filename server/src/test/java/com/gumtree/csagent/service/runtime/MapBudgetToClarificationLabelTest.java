package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — R2.a #5 phase-aware
 * {@code max-repeated-same-action} → {@code clarification_budget_exhausted}
 * re-map.
 *
 * <p>Anti-误杀 invariant #12: the {@code max-repeated-same-action} budget is
 * NOT DISCOVER-only ({@code ControlKernel.trackRepeatedAction} fires on any
 * repeated action / tool-call in any phase), so the re-map MUST be phase-aware.
 * A genuine RESOLVE / INTAKE repeated-TOOL-call budget must keep the generic
 * {@code turn_budget_exhausted}; only a DISCOVER free-text clarification
 * repetition is relabeled. No new enum value is introduced — the re-map reuses
 * the existing {@code clarification_budget_exhausted} member.
 */
class MapBudgetToClarificationLabelTest {

    // --- #5 positive: DISCOVER free-text clarification repetition relabeled ---

    @Test
    void discoverFreeTextAnswerRepetition_relabeledToClarification() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", "answer"));
    }

    @Test
    void discoverLegacyClarifyKeyRepetition_relabeledToClarification() {
        // The legacy PhaseEvaluator.deriveRepetitionKey emits "clarify".
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", "clarify"));
    }

    @Test
    void discoverPhaseLabelIsCaseInsensitive() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "discover", "answer"));
    }

    // --- #5 negative (multi-phase guard): non-DISCOVER must NOT be relabeled ---

    @Test
    void resolvePhaseRepeatedToolCall_keepsTurnBudgetExhausted() {
        // RESOLVE repeated search_knowledge (a tool call) must keep the
        // generic reason — relabeling it would be a new false-label artifact.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "RESOLVE", "search_knowledge"));
    }

    @Test
    void intakePhaseRepeatedAction_keepsTurnBudgetExhausted() {
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "INTAKE", "answer"));
    }

    // --- #5 negative (free-text guard): DISCOVER repeated TOOL call NOT relabeled ---

    @Test
    void discoverPhaseRepeatedToolCall_keepsTurnBudgetExhausted() {
        // Even in DISCOVER, a repeated classify_use_case (tool) is not a
        // free-text clarification, so it stays turn_budget_exhausted.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", "classify_use_case"));
    }

    @Test
    void nullLastActionInDiscover_keepsTurnBudgetExhausted() {
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-repeated-same-action", "DISCOVER", null));
    }

    // --- #5 negative (other budgets unchanged, regardless of phase / action) ---

    @Test
    void maxClarificationRoundsBudget_unchangedInDiscover() {
        assertEquals("clarification_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-clarification-rounds", "DISCOVER", "answer"));
    }

    @Test
    void maxFaqMissBudget_unchanged() {
        assertEquals("faq_miss_threshold_exceeded",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-faq-miss", "DISCOVER", "answer"));
    }

    @Test
    void maxTotalBotTurnsBudget_inDiscover_staysTurnBudgetExhausted() {
        // A non-clarification budget that happens to fire in DISCOVER on a
        // free-text turn must NOT be relabeled — only max-repeated-same-action
        // is re-mapped.
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(
                        "max-total-bot-turns", "DISCOVER", "answer"));
    }

    @Test
    void nullBucket_staysTurnBudgetExhausted() {
        assertEquals("turn_budget_exhausted",
                ControlKernel.mapBudgetToEscalationReason(null, "DISCOVER", "answer"));
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
