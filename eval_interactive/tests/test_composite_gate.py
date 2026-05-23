"""Wave B1.1 (refined HIGH-5): Tests for composite case_passed gating logic.

These tests verify that ``case_passed`` requires BOTH all L1 hard checks
AND all "mandatory L2" outcome checks to pass, with the mandatory rules:

* always-mandatory: ``correct_uc``, ``correct_outcome``
* S-Cleanup-3 (#4) demoted ``handover_completeness`` and
  ``case_id_present`` to Tier-3 advisory; they are computed + recorded
  but no longer gate ``case_passed``.
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
from eval_interactive.scoring.skill_procedure_check import (
    CriticalStepResult,
    Tier2Result,
    tier2_results_to_gate,
)
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
class _StubScoring:
    """Stub for ``CaseSpec.scoring`` -- only ``outcome_checks`` is consumed
    by the composite gate after S-Eval-1 (M3-Eval) added the opt-in /
    opt-out gate behaviour.
    """

    outcome_checks: list = field(
        default_factory=lambda: ["correct_uc", "correct_outcome"]
    )


@dataclass
class _StubCaseSpec:
    """Minimal stand-in for CaseSpec used by composite scorer.

    ``expected.should_escalate`` / ``expected.outcome_class`` /
    ``expected.primary_uc`` drive the conditional-mandatory rules.
    ``scoring.outcome_checks`` is read by S-Eval-1 (M3-Eval) opt-in:
    when the list is empty the mandatory-L2 gate is skipped.
    """

    expected: _StubExpected = field(default_factory=_StubExpected)
    case_id: str = "stub-case"
    scoring: _StubScoring = field(default_factory=_StubScoring)


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

    def test_escalate_does_not_add_handover_completeness(self):
        """S-Cleanup-3 (#4): outcome_class=escalate must NOT add
        ``handover_completeness`` or ``case_id_present`` to the
        mandatory-L2 set — they are now Tier-3 advisory per the
        M3-Eval pyramid.
        """
        spec = _StubCaseSpec(_StubExpected(should_escalate=True, outcome_class="escalate"))
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult("handover_completeness", 1.0),
        ]
        mand = _compute_mandatory_l2(spec, l2)
        names = {r.check_name for r in mand}
        assert names == {"correct_uc", "correct_outcome"}

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

    def test_outcome_class_escalate_handover_completeness_demoted(self):
        """S-Cleanup-3 (#4) demotion: outcome_class=escalate with a
        failing handover_completeness must NOT flip case_passed —
        the check is now Tier-3 advisory.

        Pre-S-Cleanup-3 this test asserted the gate flipped; the
        inversion is the demotion's primary contract.
        """
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
        assert result.case_passed is True
        assert result.mandatory_l2_passed is True
        assert "handover_completeness" not in result.mandatory_l2_failures
        assert not any(
            tag.startswith("L2_GATE:handover_completeness")
            or tag.startswith("L2_GATE_MISSING:handover_completeness")
            for tag in result.failure_tags
        ), result.failure_tags

    def test_outcome_class_escalate_case_id_present_demoted(self):
        """S-Cleanup-3 (#4) demotion: outcome_class=escalate with a
        missing case_id_present must NOT flip case_passed — the
        check is now Tier-3 advisory.

        Pre-S-Cleanup-3 the absence of case_id_present on a UC-H/J/K
        escalate case tripped ``L2_GATE_MISSING:case_id_present``;
        the demotion removes that gate.
        """
        spec = _StubCaseSpec(
            _StubExpected(should_escalate=True, outcome_class="escalate", primary_uc="UC-H")
        )
        l1 = _l1_pass()
        # case_id_present intentionally absent — pre-S-Cleanup-3 this
        # raised L2_GATE_MISSING.
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite("c6b", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is True
        assert result.mandatory_l2_passed is True
        assert "case_id_present" not in result.mandatory_l2_failures
        assert not any(
            "case_id_present" in tag for tag in result.failure_tags
        ), result.failure_tags

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


# ---------------------------------------------------------------------------
# Sprint 43 (S-Eval-2) — Tier-2 skill_procedure_followship gate wiring
# ---------------------------------------------------------------------------


def _step_pass(step_id: str, severity: str = "mandatory") -> CriticalStepResult:
    return CriticalStepResult(
        step_id=step_id, desc=step_id, outcome="PASS", severity=severity  # type: ignore[arg-type]
    )


def _step_fail(step_id: str, severity: str = "mandatory") -> CriticalStepResult:
    return CriticalStepResult(
        step_id=step_id, desc=step_id, outcome="FAIL", severity=severity  # type: ignore[arg-type]
    )


def _step_na(step_id: str, severity: str = "mandatory") -> CriticalStepResult:
    return CriticalStepResult(
        step_id=step_id, desc=step_id, outcome="N/A", severity=severity  # type: ignore[arg-type]
    )


class TestTier2Gate:
    """Sprint 43 (S-Eval-2): Tier-2 ``skill_procedure_followship`` band wiring
    in :func:`compute_composite` per dev prompt Outcome 5.

    Mirrors the S-Eval-1 (D-2.5) severity-driven gate model:

    - ``severity="critical"`` Tier-2 fail → flips ``case_passed``.
    - ``severity="advisory"`` Tier-2 fail → recorded as
      ``TIER2_ADVISORY:<step_id>`` tag; does NOT flip ``case_passed``.
    - Empty ``critical_steps[]`` → Tier-2 PASS / advisory; no gate effect
      (parity invariant from contract §9 hard gate).
    - Step whose ``mandatory_for`` does not match active UC → N/A at the
      extractor; the gate sees only PASS / N/A and does not flip.
    """

    def test_empty_tier2_default_does_not_flip_gate(self):
        # No tier2_result supplied → default empty advisory PASS.
        # Existing two-tier behaviour preserved (backward-compat invariant
        # for callers that have not yet been updated to pass tier2_result).
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite("c-t2-empty", l1, l2, _l3_high(), _stall_clean(), case_spec=spec)
        assert result.case_passed is True
        assert result.tier2_result.passed is True
        assert result.tier2_result.severity == "advisory"

    def test_mandatory_tier2_failure_flips_case_passed(self):
        # Active Skill carries a mandatory critical_step for UC-A; case is
        # UC-A; the step's trace_check returned FAIL at extractor → Tier-2
        # severity=critical → case_passed=False even if L1 and mandatory L2
        # all pass.
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        tier2 = tier2_results_to_gate([_step_fail("search_before_answer", "mandatory")])
        result = compute_composite(
            "c-t2-mand-fail",
            l1, l2, _l3_high(), _stall_clean(),
            case_spec=spec, tier2_result=tier2,
        )
        assert result.case_passed is False
        assert result.composite == 0.0
        assert "TIER2:search_before_answer" in result.failure_tags
        assert "Tier-2 mandatory critical_steps failed" in result.detail
        assert result.tier2_result.severity == "critical"
        assert result.tier2_result.failed_step_ids == ["search_before_answer"]

    def test_advisory_tier2_failure_does_not_flip_case_passed(self):
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        tier2 = tier2_results_to_gate([_step_fail("polish_step", "advisory")])
        result = compute_composite(
            "c-t2-adv-fail",
            l1, l2, _l3_high(), _stall_clean(),
            case_spec=spec, tier2_result=tier2,
        )
        assert result.case_passed is True
        # Advisory failure still surfaces as a tag (for trend reports).
        assert "TIER2_ADVISORY:polish_step" in result.failure_tags
        assert result.tier2_result.passed is True
        assert result.tier2_result.severity == "advisory"

    def test_all_steps_NA_does_not_flip_case_passed(self):
        # Step's mandatory_for did not match active UC → extractor returned
        # N/A → tier2_results_to_gate returns PASS / critical (no mandatory
        # fails, no advisory fails).
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        tier2 = tier2_results_to_gate([_step_na("uc_a_only_step", "mandatory")])
        result = compute_composite(
            "c-t2-na",
            l1, l2, _l3_high(), _stall_clean(),
            case_spec=spec, tier2_result=tier2,
        )
        assert result.case_passed is True
        assert result.tier2_result.passed is True

    def test_all_steps_pass_no_failure_tags(self):
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = _l1_pass()
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        tier2 = tier2_results_to_gate([
            _step_pass("step_a", "mandatory"),
            _step_pass("step_b", "advisory"),
        ])
        result = compute_composite(
            "c-t2-allpass",
            l1, l2, _l3_high(), _stall_clean(),
            case_spec=spec, tier2_result=tier2,
        )
        assert result.case_passed is True
        # Detail string explicitly names Tier-2 in the success message.
        assert "Tier-2" in result.detail
        # No TIER2 / TIER2_ADVISORY tags on a clean pass.
        for tag in result.failure_tags:
            assert not tag.startswith("TIER2"), f"unexpected Tier-2 tag {tag!r} on a clean pass"

    def test_mandatory_tier2_failure_combined_with_l1_failure(self):
        # Both L1 and Tier-2 mandatory fail → detail names both.
        spec = _StubCaseSpec(_StubExpected(should_escalate=False, outcome_class="resolve"))
        l1 = [
            HardCheckResult("no_forbidden_tools", True),
            HardCheckResult("no_pii_leakage", False, "PII leak"),
        ]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        tier2 = tier2_results_to_gate([_step_fail("critical_search", "mandatory")])
        result = compute_composite(
            "c-t2-combined",
            l1, l2, _l3_high(), _stall_clean(),
            case_spec=spec, tier2_result=tier2,
        )
        assert result.case_passed is False
        assert "L1 failed" in result.detail
        assert "Tier-2 mandatory critical_steps failed" in result.detail
        assert "L1:no_pii_leakage" in result.failure_tags
        assert "TIER2:critical_search" in result.failure_tags
