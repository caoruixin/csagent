package com.gumtree.csagent.service.runtime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parameterized coverage for {@link ControlKernel#mapBudgetToEscalationReason(String)}.
 * Codex 2026-05-03 round 3: the helper had no direct tests; this locks the
 * Phase 2 §2.4 precedence (clarification &gt; faq-miss &gt; per-path / total
 * / repeated turn caps) into a regression gate.
 */
class ControlKernelEscalationReasonTest {

    @ParameterizedTest(name = "[{index}] bucket={0} -> {1}")
    @CsvSource({
            "max-clarification-rounds, clarification_budget_exhausted",
            "max-faq-miss, faq_miss_threshold_exceeded",
            "max-bot-turns-faq, turn_budget_exhausted",
            "max-bot-turns-intake, turn_budget_exhausted",
            "max-total-bot-turns, turn_budget_exhausted",
            "max-repeated-same-action, turn_budget_exhausted",
            "unknown-bucket, turn_budget_exhausted",
    })
    void mapsKnownBucketsToCanonicalReason(String bucket, String expectedReason) {
        assertEquals(expectedReason, ControlKernel.mapBudgetToEscalationReason(bucket));
    }

    @org.junit.jupiter.api.Test
    void nullBucketFallsBackToTurnBudgetExhausted() {
        assertEquals("turn_budget_exhausted", ControlKernel.mapBudgetToEscalationReason(null));
    }
}
