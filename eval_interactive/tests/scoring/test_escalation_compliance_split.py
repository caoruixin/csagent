"""S-Auto-38 (Sprint 092) — escalation_compliance gate-split invariants (WP-A).

Proves the split of the bundled ``escalation_compliance`` into:

* Part-1 ``escalation_compliance`` — escalate-vs-don't BEHAVIOUR, STAYS a
  tier-0 floor (``tier_evaluator._TIER0_PY_FAMILY`` member).
* Part-2 ``escalation_reason_family_match`` — the stochastic reason-family
  match, OBSERVATION-ONLY (advisory severity; NOT in ``_TIER0_PY_FAMILY``).

Covers plan §7 verification items at the unit level:
  * item 2 negative control: a genuine should-escalate-but-didn't still fails
    Part-1 (tier-0).
  * item 5 other-invariant: Part-1 detail string byte-identical to legacy.
  * the demotion: a cross-family reason mismatch no longer flips
    ``case_passed`` (the composite gate) nor the tier-0 floor.
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
from eval_interactive.scoring.hard_checks import HardChecker
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.trace.models import SessionState, TraceData, TurnTrace


def _spec(*, should_escalate, escalation_trigger, risk_level="medium",
          primary_uc="UC-D", session_id="sess-split") -> CaseSpec:
    return CaseSpec(
        case_id="test-split-001",
        source_session_id=session_id,
        source_dataset="test",
        form_context=FormContext(first_name="T", email="a@b.c", topic_subject="t"),
        persona=Persona(
            user_goal_summary="g", frustration_level="none", verbosity="normal",
            drift_behavior="none", seed_messages=["hi"], hidden_facts=[],
            will_request_human_if="bot cannot resolve after 2 attempts",
        ),
        expected=Expected(
            outcome_class="escalate" if should_escalate else "resolve",
            primary_uc=primary_uc, secondary_ucs=[],
            should_escalate=should_escalate,
            allow_bot_resolution="true",
            bot_handling_pattern="<fixture>",
            escalation_trigger=escalation_trigger,
            risk_level=risk_level,
        ),
        scoring=ScoringConfig(hard_checks=[], outcome_checks=[], llm_judge_dimensions=[]),
    )


def _handover_turn(reason: str) -> TurnTrace:
    return TurnTrace(
        turn_index=1, user_message="u", bot_response="b",
        tool_calls=[{"tool_name": "request_handover",
                     "arguments": {"escalation_reason": reason}}],
        source_ids=[], phase_before="DISCOVER", phase_after="ESCALATE",
        active_use_case="UC-D", latency_ms=10, projected_context={},
    )


def _trace(*, turns=None, containment="escalated") -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="sess-split", active_use_case="UC-D", candidate_use_cases=[],
            containment_outcome=containment, escalation_reason="",
            total_bot_turns=1, clarification_count=0, faq_miss_count=0,
            form_context={}, customer_context={}, articles_shown=[],
            current_phase="ESCALATE",
        ),
        turns=turns or [], events=[], handover=None,
    )


def _by(results, name):
    return next(r for r in results if r.check_name == name)


# NOTE: the assertion that ``escalation_compliance`` ∈ ``_TIER0_PY_FAMILY`` and
# ``escalation_reason_family_match`` ∉ it lives in the autoloop suite
# (tests/test_tier_evaluator.py::test_tier0_family_excludes_reason_family_match)
# because ``autoloop`` is not importable from the eval_interactive env.


# --- the demotion: cross-family mismatch is observation-only --------------

def test_cs11s01_shape_crossfamily_is_observation_only() -> None:
    """Medium-risk UC-D case (cs11s01 shape): bot escalates with a cross-family
    reason (user_requested vs expected faq_miss_threshold_exceeded). Part-1
    PASSES; Part-2 advisory-fails; the advisory fail does NOT flip case_passed."""
    checker = HardChecker()
    case = _spec(should_escalate=True, escalation_trigger="faq_miss_threshold_exceeded",
                 risk_level="medium")
    trace = _trace(turns=[_handover_turn("user_requested")])
    results = checker.run_checks(case, trace)

    assert _by(results, "escalation_compliance").passed is True
    fm = _by(results, "escalation_reason_family_match")
    assert fm.passed is False
    assert fm.severity == "advisory"

    # The composite gate is NOT flipped by the advisory Part-2 fail: with an
    # otherwise-clean L1 set + a passing mandatory L2, case_passed stays True.
    l2 = [
        OutcomeCheckResult(check_name="correct_uc", score=1.0),
        OutcomeCheckResult(check_name="correct_outcome", score=1.0),
    ]
    case.scoring = ScoringConfig(
        hard_checks=[], outcome_checks=["correct_uc", "correct_outcome"],
        llm_judge_dimensions=[],
    )
    comp = compute_composite(
        case_id="c", l1_results=results, l2_results=l2, l3_results=[],
        stall_result=StallResult(detected=False), case_spec=case,
    )
    assert comp.case_passed is True, comp.failure_tags


# --- negative control: genuine should-escalate-but-didn't still fails -----

def test_negative_control_should_escalate_but_didnt_fails_part1() -> None:
    """risk=critical, should_escalate, bot did NOT escalate -> Part-1 fails
    (tier-0). The reason label is irrelevant here."""
    checker = HardChecker()
    case = _spec(should_escalate=True, escalation_trigger="trust_safety_required",
                 risk_level="critical", primary_uc="UC-J")
    trace = _trace(turns=[], containment="resolved")
    results = checker.run_checks(case, trace)
    ec = _by(results, "escalation_compliance")
    assert ec.passed is False
    # Part-1 detail byte-identical to the legacy bundled check.
    assert ec.detail == "should_escalate=true, risk=critical, but outcome=resolved"


def test_high_risk_escalated_passes_part1_regardless_of_reason() -> None:
    """risk=high, escalated, but wrong reason family -> Part-1 PASSES (behaviour
    floor satisfied); only Part-2 advisory-records the mismatch."""
    checker = HardChecker()
    case = _spec(should_escalate=True, escalation_trigger="trust_safety_required",
                 risk_level="high", primary_uc="UC-J")
    trace = _trace(turns=[_handover_turn("user_requested")])
    results = checker.run_checks(case, trace)
    assert _by(results, "escalation_compliance").passed is True
    assert _by(results, "escalation_reason_family_match").passed is False
    assert _by(results, "escalation_reason_family_match").severity == "advisory"


def test_same_family_pick_passes_part2() -> None:
    """intake_complete_for_uc_j and trust_safety_required share the trust_safety
    family -> Part-2 passes."""
    checker = HardChecker()
    case = _spec(should_escalate=True, escalation_trigger="trust_safety_required",
                 risk_level="high", primary_uc="UC-J")
    trace = _trace(turns=[_handover_turn("intake_complete_for_uc_j")])
    results = checker.run_checks(case, trace)
    assert _by(results, "escalation_reason_family_match").passed is True
