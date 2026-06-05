"""OQ-S77 (S-Auto-22) — false-positive gate tests.

Sprint 077 / S-Auto-22 closes the OPPOSITE-direction measurement artifact the
S-Auto-21 simfixed re-bless exposed: stalled / looped / impossible sessions
reported ``case_passed=true`` when an earlier turn stamped
``containment_outcome="resolved"`` AND ``l2_results=[]`` left the mandatory-L2
gate vacuously True. See
``docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md``.

Three fixes, each with an anti-误杀 counter-test:

- Fix #1 (``composite.py``): a detected stall with NO positively-scored
  outcome (composite == 0) flips ``case_passed`` to False. Scoped to
  composite == 0 so a stall the detector FALSE-flags on a validly-recovered /
  escalated draw (composite > 0) is NOT mis-failed.
- Fix #2 (``hard_checks.py`` ``trace_minimum`` Mode-3): a
  ``containment_outcome="resolved"`` contradicted by a terminal-failure
  ``stop_reason`` FAILs. ``goal_impossible`` is deliberately excluded;
  ``escalated`` / ``goal_achieved`` are never contradicted.
- Fix #3 (``composite.py``): a pass resting on ZERO positive evidence
  (``l2_results == []`` AND composite == 0) is refused.
"""

from __future__ import annotations

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.composite import compute_composite
from eval_interactive.scoring.hard_checks import HardChecker, HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.trace.models import SessionState, TraceData, TurnTrace


# ---------------------------------------------------------------------------
# Builders
# ---------------------------------------------------------------------------


def _spec(outcome_checks: list[str] | None = None) -> CaseSpec:
    return CaseSpec(
        case_id="oq-s77",
        source_session_id="s",
        source_dataset="d",
        form_context=FormContext(first_name="x", email="x@x", topic_subject="t"),
        persona=Persona(
            user_goal_summary="g",
            frustration_level="none",
            verbosity="terse",
            drift_behavior="none",
            seed_messages=[],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class="resolve",
            primary_uc="UC-A",
            secondary_ucs=[],
            should_escalate=False,
            allow_bot_resolution="true",
        ),
        scoring=ScoringConfig(
            hard_checks=[],
            outcome_checks=outcome_checks or [],
            llm_judge_dimensions=[],
        ),
    )


def _trace(containment_outcome: str, turns: list[TurnTrace] | None = None) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="s",
            active_use_case="UC-A",
            candidate_use_cases=[],
            containment_outcome=containment_outcome,
            escalation_reason="",
            total_bot_turns=3,
            clarification_count=0,
            faq_miss_count=0,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="CLOSE",
        ),
        turns=turns or [],
        events=[],
        handover=None,
    )


_PASS_L1 = [HardCheckResult("no_pii_leakage", True)]


# ---------------------------------------------------------------------------
# Fix #1 — stall promotion (composite.py), scoped to composite == 0
# ---------------------------------------------------------------------------


class TestStallPromotion:
    def test_stall_with_zero_composite_fails(self):
        """The vacuous fingerprint: stall + empty L2 + no judge -> FAIL."""
        result = compute_composite(
            "c", _PASS_L1, [], [], StallResult(detected=True, failure_tag="PLACEHOLDER_WITHOUT_FOLLOWUP"),
            _spec(outcome_checks=[]),
        )
        assert result.case_passed is False
        assert result.verdict_reason == "stall_promoted"
        assert "VERDICT_OVERRIDE:stall_promoted" in result.failure_tags

    def test_stall_with_positive_composite_is_spared(self):
        """Anti-误杀 (cs095 a4): a stall the detector false-flags on a draw
        that still earned a positive scored outcome (composite > 0, e.g. a
        valid escalation) is NOT promoted to FAIL."""
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite(
            "c", _PASS_L1, l2, [], StallResult(detected=True, failure_tag="PLACEHOLDER_WITHOUT_FOLLOWUP"),
            _spec(outcome_checks=["correct_uc", "correct_outcome"]),
        )
        assert result.composite > 0.0
        assert result.case_passed is True
        assert result.verdict_reason == ""
        assert "VERDICT_OVERRIDE:stall_promoted" not in result.failure_tags
        # The STALL tag is still recorded for trend reporting; only the gate
        # promotion is suppressed.
        assert any(t.startswith("STALL:") for t in result.failure_tags)

    def test_no_stall_no_promotion(self):
        """No stall -> the stall gate never fires (#3 may still apply)."""
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite(
            "c", _PASS_L1, l2, [], StallResult(detected=False),
            _spec(outcome_checks=["correct_uc", "correct_outcome"]),
        )
        assert result.case_passed is True
        assert result.verdict_reason == ""


# ---------------------------------------------------------------------------
# Fix #3 — refuse zero-positive-evidence pass (composite.py)
# ---------------------------------------------------------------------------


class TestZeroEvidenceRefusal:
    def test_empty_l2_zero_composite_fails(self):
        """uc_b/uc_a-style: empty L2 + zero composite + no stall -> FAIL."""
        result = compute_composite(
            "c", _PASS_L1, [], [], StallResult(detected=False),
            _spec(outcome_checks=[]),
        )
        assert result.case_passed is False
        assert result.verdict_reason == "no_l2_evidence_to_pass"
        assert "VERDICT_OVERRIDE:no_l2_evidence_to_pass" in result.failure_tags

    def test_empty_l2_but_positive_judge_passes(self):
        """Anti-误杀: empty L2 is fine when the judge gives a positive signal
        (composite > 0) -- the case rests on real evidence, not a stamp."""
        l3 = [JudgeResult("premature_finish", 5.0)]
        result = compute_composite(
            "c", _PASS_L1, [], l3, StallResult(detected=False),
            _spec(outcome_checks=[]),
        )
        assert result.composite > 0.0
        assert result.case_passed is True
        assert result.verdict_reason == ""

    def test_nonempty_l2_with_evidence_passes(self):
        """Anti-误杀: a legitimate resolve with L2 outcome evidence passes."""
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite(
            "c", _PASS_L1, l2, [], StallResult(detected=False),
            _spec(outcome_checks=["correct_uc", "correct_outcome"]),
        )
        assert result.case_passed is True
        assert result.verdict_reason == ""

    def test_l1_failure_unaffected_by_override(self):
        """A case already failing on L1 stays failed; the override does not
        rewrite the (more specific) L1 reason."""
        l1 = [HardCheckResult("no_pii_leakage", False, "leaked")]
        result = compute_composite(
            "c", l1, [], [], StallResult(detected=False),
            _spec(outcome_checks=[]),
        )
        assert result.case_passed is False
        # The override block only runs when the case would otherwise pass.
        assert result.verdict_reason == ""


# ---------------------------------------------------------------------------
# Fix #2 — terminal-failure overrides resolved stamp (hard_checks.py Mode-3)
# ---------------------------------------------------------------------------


class TestTerminalFailureOverride:
    def _trace_min(self, containment: str, stop_reason: str) -> HardCheckResult:
        checker = HardChecker()
        case = _make_case()
        trace = _trace(containment)
        results = checker.run_checks(case, trace, stop_reason=stop_reason)
        return next(r for r in results if r.check_name == "trace_minimum")

    def test_resolved_plus_loop_detected_fails(self):
        tm = self._trace_min("resolved", "loop_detected")
        assert tm.passed is False
        assert "contradicted" in tm.detail.lower()

    def test_resolved_plus_error_fails(self):
        tm = self._trace_min("resolved", "error")
        assert tm.passed is False

    def test_resolved_plus_contract_violation_fails(self):
        tm = self._trace_min("resolved", "contract_violation")
        assert tm.passed is False

    def test_resolved_plus_max_turns_exceeded_fails(self):
        tm = self._trace_min("resolved", "max_turns_exceeded")
        assert tm.passed is False

    # ---- anti-误杀 counter-cases ----

    def test_resolved_plus_goal_achieved_passes(self):
        """A legitimately-completed one-shot that stamped resolved passes."""
        tm = self._trace_min("resolved", "goal_achieved")
        assert tm.passed is True

    def test_resolved_plus_bot_ended_passes(self):
        tm = self._trace_min("resolved", "bot_ended")
        assert tm.passed is True

    def test_resolved_plus_goal_impossible_passes(self):
        """goal_impossible is DELIBERATELY excluded from the terminal-failure
        set (ambiguous persona-gave-up signal; S-Auto-20 precedent; would
        mis-fail full-evidence shadow draws). The vacuous goal_impossible
        draws are gated by the composite-side #3 zero-evidence rule instead."""
        tm = self._trace_min("resolved", "goal_impossible")
        assert tm.passed is True

    def test_escalated_plus_loop_detected_passes(self):
        """Only ``resolved`` is contradicted by Mode-3 -- an ``escalated``
        terminal is left untouched."""
        tm = self._trace_min("escalated", "loop_detected")
        assert tm.passed is True


def _make_case() -> CaseSpec:
    """A spec with no per-case hard_checks (the global set, incl. trace_minimum,
    still runs) for the Mode-3 trace_minimum tests."""
    return CaseSpec(
        case_id="oq-s77-tm",
        source_session_id="s",
        source_dataset="d",
        form_context=FormContext(first_name="x", email="x@x", topic_subject="t"),
        persona=Persona(
            user_goal_summary="g",
            frustration_level="none",
            verbosity="terse",
            drift_behavior="none",
            seed_messages=[],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class="resolve",
            primary_uc="UC-A",
            secondary_ucs=[],
            should_escalate=False,
            allow_bot_resolution="true",
        ),
        scoring=ScoringConfig(hard_checks=[], outcome_checks=[], llm_judge_dimensions=[]),
    )
