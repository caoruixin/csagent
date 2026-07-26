"""Tests for the composite scorer module."""

from __future__ import annotations

import pytest

from eval_interactive.scoring.composite import CompositeScore, compute_composite
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.stall_detector import StallResult


class TestComputeComposite:
    def test_all_pass_no_judge(self):
        """Sprint 105 (item 2): no L3 configured -> composite renormalises.

        BEFORE-STATE, preserved verbatim as the documented prior
        expectation of this test::

            assert result.judge_score == 0.0
            assert result.composite == 0.5  # 0.5 * 1.0 + 0.5 * 0.0
            assert result.failure_tags == []

        That arithmetic scored the *absence* of a judge signal as the
        *failure* of it: a case with a perfect outcome and no configured
        L3 dim was capped at 0.5 against a 0.7 pass bar, so it could
        never pass however well the bot behaved. ``judge_score`` is still
        0.0 — it is an absent value, which is what ``judge_measured``
        now says explicitly — but the composite no longer halves against
        it. See ``tests/test_sprint_105_loop_c_and_ladder.py`` for the
        full before/after characterisation.
        """
        l1 = [
            HardCheckResult("no_forbidden_tools", True),
            HardCheckResult("budget_enforcement", True),
        ]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-001", l1, l2, [], stall)
        assert result.case_passed is True
        assert result.outcome_score == 1.0
        assert result.judge_score == 0.0
        assert result.judge_measured is False
        assert result.judge_basis == "none"
        # Renormalised onto the only layer that carries signal.
        assert result.composite == 1.0
        assert result.failure_tags == ["L3_UNMEASURED:no_dims_configured"]

    def test_all_pass_with_judge(self):
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [
            JudgeResult("groundedness", 5.0),
            JudgeResult("relevance", 5.0),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-002", l1, l2, l3, stall)
        assert result.case_passed is True
        assert result.outcome_score == 1.0
        assert result.judge_score == 1.0  # 5/5
        assert result.composite == 1.0

    def test_l1_failure_zeros_composite(self):
        l1 = [
            HardCheckResult("no_forbidden_tools", True),
            HardCheckResult("no_pii_leakage", False, "PII found"),
        ]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [JudgeResult("groundedness", 5.0)]
        stall = StallResult(detected=False)
        result = compute_composite("case-003", l1, l2, l3, stall)
        assert result.case_passed is False
        assert result.composite == 0.0
        assert "L1:no_pii_leakage" in result.failure_tags

    def test_low_l2_adds_failure_tag(self):
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [
            OutcomeCheckResult("correct_uc", 0.3),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-004", l1, l2, [], stall)
        assert "L2:correct_uc" in result.failure_tags

    def test_low_l3_adds_failure_tag(self):
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [JudgeResult("groundedness", 2.0)]
        stall = StallResult(detected=False)
        result = compute_composite("case-005", l1, l2, l3, stall)
        assert "L3:groundedness" in result.failure_tags

    def test_stall_adds_failure_tag(self):
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        stall = StallResult(
            detected=True,
            turn_index=3,
            failure_tag="STALL_AFTER_TOOL_INTENT",
        )
        result = compute_composite("case-006", l1, l2, [], stall)
        assert "STALL:STALL_AFTER_TOOL_INTENT" in result.failure_tags

    def test_empty_l1_defaults_to_passed(self):
        """No L1 checks means case_passed=True by default."""
        l2 = [OutcomeCheckResult("correct_uc", 0.8)]
        stall = StallResult(detected=False)
        result = compute_composite("case-007", [], l2, [], stall)
        assert result.case_passed is True

    def test_empty_l2_gives_zero_outcome(self):
        l1 = [HardCheckResult("no_stall", True)]
        stall = StallResult(detected=False)
        result = compute_composite("case-008", l1, [], [], stall)
        assert result.outcome_score == 0.0

    def test_judge_score_normalized_to_0_1(self):
        """Judge score 3.0/5 should give 0.6 normalized."""
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [JudgeResult("groundedness", 3.0)]
        stall = StallResult(detected=False)
        result = compute_composite("case-009", l1, l2, l3, stall)
        assert result.judge_score == 0.6

    def test_mixed_scores(self):
        """Composite = 0.5 * outcome + 0.5 * judge."""
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("turn_efficiency", 0.5),
        ]
        l3 = [JudgeResult("groundedness", 4.0)]
        stall = StallResult(detected=False)
        result = compute_composite("case-010", l1, l2, l3, stall)
        assert result.outcome_score == 0.75
        assert result.judge_score == 0.8
        expected_composite = 0.5 * 0.75 + 0.5 * 0.8
        assert abs(result.composite - expected_composite) < 0.001
