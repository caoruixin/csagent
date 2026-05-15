"""Wave B1.1 (refined HIGH-5): Tests for composite case_passed gating logic.

These tests verify that ``case_passed`` requires BOTH all L1 hard checks
AND all "mandatory L2" outcome checks to pass, with the conditional
mandatory rules:

* always-mandatory: ``correct_uc``, ``correct_outcome``
* if ``outcome_class == 'escalate'``: ``handover_completeness``
* fail-closed: missing mandatory checks count as failures

Note (HIGH-5): ``escalation_compliance`` used to be in the conditional
mandatory L2 set but it is actually an L1 hard check. Listing it at L2
caused otherwise-passing escalate cases to be zeroed via
``L2_GATE_MISSING:escalation_compliance`` whenever the L2 list didn't
also configure it. It now lives only at L1 (and runs globally), so the
L1 gate covers it without an L2 duplicate.

Stub objects are used for ``CaseSpec`` so tests don't depend on YAML
loading. Only the attributes the composite scorer actually reads
(``expected.should_escalate`` and ``expected.outcome_class``) are
populated.
"""

from __future__ import annotations

from dataclasses import dataclass, field

import pytest

from eval_interactive.scoring.composite import (
    CompositeScore,
    _compute_mandatory_l2,
    compute_composite,
)
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.stall_detector import StallResult


# ---------------------------------------------------------------------------
# Stubs
# ---------------------------------------------------------------------------


@dataclass
class _StubExpected:
    should_escalate: bool = False
    outcome_class: str = "resolve"
    # Codex 2026-05-03 round 3 promoted ``case_id_present`` and
    # ``tool_sequence_match`` to conditionally-mandatory L2 gates that
    # depend on the expected primary UC and tool sequence. Stub defaults
    # leave them off so the existing gate-tests stay focused on the
    # outcome_class branch they were written for.
    primary_uc: str = ""
    expected_tool_sequence: list = field(default_factory=list)


@dataclass
class _StubCaseSpec:
    """Minimal stand-in for CaseSpec used by composite scorer.

    Only ``expected.should_escalate`` and ``expected.outcome_class`` are
    consumed by the gating logic, so we don't need the full schema.
    """

    expected: _StubExpected = field(default_factory=_StubExpected)
    case_id: str = "stub-case"


def _l1_pass(*names: str) -> list[HardCheckResult]:
    return [HardCheckResult(n, True) for n in names] or [
        HardCheckResult("no_stall", True)
    ]


def _l3_high() -> list[JudgeResult]:
    """L3 scores high enough that the 0.5*L2 + 0.5*L3 composite would be
    >= 0.7 if it were not zeroed by a gate failure. Used to verify that
    a gate failure forces composite to 0 even when L3 is strong."""
    return [JudgeResult("groundedness", 5.0), JudgeResult("relevance", 5.0)]


def _stall_clean() -> StallResult:
    return StallResult(detected=False)


# ---------------------------------------------------------------------------
# Helper / introspection tests
# ---------------------------------------------------------------------------


class TestComputeMandatoryL2:
    def test_always_mandatory_subset_only(self):
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult("turn_efficiency", 0.4),  # not mandatory
        ]
        mand = _compute_mandatory_l2(spec, l2)
        names = {r.check_name for r in mand}
        assert names == {"correct_uc", "correct_outcome"}

    def test_escalate_adds_handover_completeness(self):
        """outcome_class=escalate -> mandatory L2 adds handover_completeness.

        ``escalation_compliance`` is intentionally NOT in the mandatory L2
        set (HIGH-5): it is an L1 hard check now.
        """
        spec = _StubCaseSpec(_StubExpected(should_escalate=True, outcome_class="escalate"))
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult("handover_completeness", 1.0),
        ]
        mand = _compute_mandatory_l2(spec, l2)
        names = {r.check_name for r in mand}
        assert names == {
            "correct_uc",
            "correct_outcome",
            "handover_completeness",
        }

    def test_should_escalate_does_not_add_l2_gate(self):
        """should_escalate=true alone (without outcome_class=escalate) must
        not add any L2 mandatory gate beyond the always-mandatory pair.

        Regression for HIGH-5: ``escalation_compliance`` was wrongly added
        as a mandatory L2 gate when ``should_escalate=true``.
        """
        spec = _StubCaseSpec(_StubExpected(should_escalate=True, outcome_class="resolve"))
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        mand = _compute_mandatory_l2(spec, l2)
        names = {r.check_name for r in mand}
        assert names == {"correct_uc", "correct_outcome"}


# ---------------------------------------------------------------------------
# Gate behaviour tests
# ---------------------------------------------------------------------------


class TestCompositeGate:
    def test_all_l1_pass_all_mandatory_l2_pass_threshold_met(self):
        """All L1 + all mandatory L2 + composite >= 0.7 -> PASS."""
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass("no_forbidden_tools", "no_pii_leakage")
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        l3 = _l3_high()
        result = compute_composite("c1", l1, l2, l3, _stall_clean(), case_spec=spec)
        assert result.case_passed is True
        assert result.mandatory_l2_passed is True
        assert result.mandatory_l2_failures == []
        assert result.composite >= 0.7

    def test_one_l1_fail_forces_composite_zero(self):
        """One L1 failure -> composite forced to 0, case_passed=False."""
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = [
            HardCheckResult("no_forbidden_tools", True),
            HardCheckResult("no_pii_leakage", False, "PII leak"),
        ]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite("c2", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is False
        assert result.composite == 0.0
        assert "L1:no_pii_leakage" in result.failure_tags
        assert "L1 failed" in result.detail

    def test_correct_uc_fail_zeros_case_even_with_high_l3(self):
        """All L1 pass but correct_uc=0 -> case_passed=False, composite=0.

        This is the audit-finding hole that Wave B1.1 closes: a high L3
        judge score MUST NOT mask an outcome failure.
        """
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 0.0, "expected=UC-A, actual=UC-B"),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite("c3", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is False
        assert result.composite == 0.0
        assert result.mandatory_l2_passed is False
        assert "correct_uc" in result.mandatory_l2_failures
        assert "L2_GATE:correct_uc" in result.failure_tags
        assert "mandatory L2 failed" in result.detail

    def test_correct_outcome_fail_zeros_case(self):
        """All L1 pass but correct_outcome=0 -> case_passed=False."""
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 0.0, "expected=resolve, actual=escalate"),
        ]
        result = compute_composite("c4", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is False
        assert result.composite == 0.0
        assert "correct_outcome" in result.mandatory_l2_failures
        assert "L2_GATE:correct_outcome" in result.failure_tags

    def test_should_escalate_no_l2_escalation_gate(self):
        """should_escalate=true must NOT add an L2 escalation gate.

        Regression test for HIGH-5: previously,
        ``L2_GATE_MISSING:escalation_compliance`` zeroed out otherwise-
        passing escalate cases (cs_interactive_040 was the canonical
        example). ``escalation_compliance`` is now an L1-only check, so
        a case with all L1 + always-mandatory L2 + handover_completeness
        passing must reach the composite formula (not be forced to 0).
        """
        spec = _StubCaseSpec(_StubExpected(should_escalate=True, outcome_class="escalate"))
        l1 = _l1_pass()
        # No escalation_compliance at L2 -- it's an L1 check.
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult("handover_completeness", 1.0),
        ]
        result = compute_composite("c5", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is True
        assert result.mandatory_l2_passed is True
        assert result.mandatory_l2_failures == []
        # No L2_GATE_MISSING tag for escalation_compliance.
        assert all(
            "escalation_compliance" not in tag for tag in result.failure_tags
        ), result.failure_tags
        assert result.composite >= 0.7

    def test_outcome_class_escalate_handover_fails(self):
        """outcome_class=escalate but handover_completeness fails -> case_passed=False."""
        spec = _StubCaseSpec(_StubExpected(should_escalate=True, outcome_class="escalate"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult(
                "handover_completeness", 0.6, "missing fields: ['summary', 'escalation_reason']"
            ),
        ]
        result = compute_composite("c6", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is False
        assert result.composite == 0.0
        assert "handover_completeness" in result.mandatory_l2_failures
        assert "L2_GATE:handover_completeness" in result.failure_tags

    def test_should_escalate_false_escalation_compliance_not_mandatory(self):
        """should_escalate=false -> no L2 escalation gate (still true).

        Missing or failing an ``escalation_compliance`` L2 entry must not
        cause case_passed=False, regardless of should_escalate. (L1
        handles escalation correctness.)
        """
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        # Missing escalation_compliance entirely -- should be fine.
        l2_missing = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result_missing = compute_composite(
            "c7a", l1, l2_missing, _l3_high(), _stall_clean(), case_spec=spec
        )
        assert result_missing.case_passed is True
        assert result_missing.mandatory_l2_passed is True

        # Present but failing -- still fine, since not mandatory at L2.
        l2_failing = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult("escalation_compliance", 0.0, "should not be mandatory at L2"),
        ]
        result_failing = compute_composite(
            "c7b", l1, l2_failing, _l3_high(), _stall_clean(), case_spec=spec
        )
        assert result_failing.case_passed is True
        assert result_failing.mandatory_l2_passed is True

    def test_outcome_class_resolve_handover_completeness_not_mandatory(self):
        """outcome_class=resolve -> handover_completeness is NOT mandatory."""
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        # handover_completeness present but at 0.0 -- still allowed because
        # the case is a resolve, not an escalate.
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult("handover_completeness", 0.0, "no handover (irrelevant)"),
        ]
        result = compute_composite("c8", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is True
        assert result.mandatory_l2_passed is True

    def test_legacy_no_case_spec_skips_l2_gate(self):
        """Backward-compat path: if case_spec is omitted, only L1 gates apply.

        This documents the legacy behaviour for callers (e.g. the existing
        test_composite.py suite) that don't yet pass case_spec.
        """
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 0.0),
            OutcomeCheckResult("correct_outcome", 0.0),
        ]
        result = compute_composite("c-legacy", l1, l2, [], _stall_clean())
        assert result.case_passed is True  # only L1 considered
        assert result.mandatory_l2_passed is True  # default
