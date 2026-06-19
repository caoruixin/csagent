"""§3.3 anti-widen replay battery (S-Auto-40 WP1-A), zero-LLM.

Proves the declarative conditional-outcome evaluator never lets a trace into
the unresolved-accepted branch unless it is genuinely a positive,
post-closure-qualified-help UNRESOLVED escalation WITH a per-trace
adjudication artifact. Every other shape — early/lazy escalation,
escalation-after-satisfaction, non-grounded escalation, false resolve,
neutral / new-goal / unknown user-state, handover-caused missing signal,
pre-help unresolved, stale adjudication — must FAIL or stay
CONDITIONAL_ELIGIBLE (never auto-PASS).

These run entirely on synthetic traces (no LLM): they exercise the decision
logic and the closure/user-state/adjudication reducers.
"""

from __future__ import annotations

import pytest

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring import conditional_outcome as co
from eval_interactive.scoring.composite import compute_composite
from eval_interactive.scoring.conditional_outcome import (
    CONDITIONAL_ELIGIBLE,
    FAIL,
    PASS,
    closure_criterion_version,
    evaluate_conditional_outcome,
)
from eval_interactive.scoring.hard_checks import HardChecker
from eval_interactive.scoring.outcome_checks import OutcomeChecker
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.trace.models import SessionState, TraceData, TurnTrace


CLOSURE = "Bot consults the specific listing and uses the listing data."


def _case_spec(
    primary_uc: str = "UC-A",
    with_block: bool = True,
    closure_criterion: str = CLOSURE,
) -> CaseSpec:
    return CaseSpec(
        case_id="cs_uc_a_loaded_listing",
        source_session_id="src",
        source_dataset="test",
        form_context=FormContext(first_name="R", email="r@e.com", topic_subject="Ad"),
        persona=Persona(
            user_goal_summary="why no views", frustration_level="mild",
            verbosity="normal", drift_behavior="none", seed_messages=["hi"],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class="resolve", primary_uc=primary_uc, secondary_ucs=[],
            should_escalate=False, allow_bot_resolution="true", max_turns=5,
        ),
        scoring=ScoringConfig(
            hard_checks=[], outcome_checks=["correct_uc", "correct_outcome"],
            llm_judge_dimensions=[],
        ),
        closure_criterion=closure_criterion,
        conditional_outcome_acceptance=(
            {
                "schema_version": 1,
                "satisfied_outcome": "resolve",
                "unresolved_accept_outcome": "escalate",
                "require_closure_precondition": True,
            }
            if with_block
            else None
        ),
    )


def _turn(idx, *, grounded: bool, active_uc="UC-A") -> TurnTrace:
    return TurnTrace(
        turn_index=idx,
        user_message="u",
        bot_response="here is your answer" if grounded else "let me think",
        tool_calls=[{"tool_name": "get_customer_context"}] if grounded else [],
        source_ids=["ka123"] if grounded else [],
        phase_before="UNDERSTAND",
        phase_after="RESOLVE" if grounded else "UNDERSTAND",
        active_use_case=active_uc,
        latency_ms=1,
        projected_context={},
    )


def _trace(
    *,
    outcome: str,
    turns,
    signals,
    session_id="sess-X",
    active_uc="UC-A",
) -> TraceData:
    ss = SessionState(
        session_id=session_id,
        active_use_case=active_uc,
        candidate_use_cases=[active_uc],
        containment_outcome=outcome,
        escalation_reason="",
        total_bot_turns=len(turns),
        clarification_count=0,
        faq_miss_count=0,
        form_context={},
        customer_context={},
        articles_shown=[],
        current_phase="DONE",
    )
    return TraceData(
        session_state=ss, turns=turns, events=[], handover=None,
        user_state_signals=signals,
    )


def _sig(turn_id, user_state=None, goal_status="in_progress"):
    return {
        "turn_id": turn_id, "produced_user_turn": turn_id + 1,
        "user_state": user_state, "goal_status": goal_status,
        "signal_source": "simulator_generate_next", "schema_version": 1,
    }


def _adj(case_spec, session_id="sess-X", version=None):
    return {
        session_id: {
            "trace_id": session_id,
            "case_id": case_spec.case_id,
            "closure_criterion_version": version
            if version is not None
            else closure_criterion_version(case_spec),
            "verdict": "accept_escalation",
            "reviewer": "test",
            "rationale": "genuine post-help unresolved",
            "timestamp": "2026-06-19T00:00:00Z",
        }
    }


def _v(cs, trace, adjudications=None):
    return evaluate_conditional_outcome(cs, trace, adjudications or {}).verdict


# --- The matrix --------------------------------------------------------

def test_grounded_satisfied_resolve_PASS():
    cs = _case_spec()
    tr = _trace(
        outcome="resolved",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "satisfied", "achieved")],
    )
    assert _v(cs, tr) == PASS


def test_grounded_unresolved_closure_escalate_is_CONDITIONAL_then_PASS_with_adjudication():
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    # No adjudication → CONDITIONAL_ELIGIBLE (never auto-PASS).
    res = evaluate_conditional_outcome(cs, tr, {})
    assert res.verdict == CONDITIONAL_ELIGIBLE
    assert res.score == 0.0
    # Adjudicated → PASS.
    assert _v(cs, tr, _adj(cs)) == PASS


def test_early_lazy_escalation_no_signal_FAIL():
    # Bot escalated with no grounded help and no user_state signal.
    cs = _case_spec()
    tr = _trace(outcome="escalated", turns=[_turn(1, grounded=False)], signals=[])
    assert _v(cs, tr) == FAIL


def test_escalation_after_satisfaction_FAIL():
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "satisfied", "achieved")],
    )
    assert _v(cs, tr) == FAIL


def test_unresolved_but_non_grounded_escalation_FAIL():
    # Positive UNRESOLVED but NO closure-qualified grounded-help marker.
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=False)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    assert _v(cs, tr) == FAIL


def test_false_resolve_unresolved_user_FAIL():
    # Bot stamped resolved but the customer is positively UNRESOLVED.
    cs = _case_spec()
    tr = _trace(
        outcome="resolved",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    assert _v(cs, tr) == FAIL


def test_unknown_after_grounded_escalation_FAIL():
    # Grounded help present but no user_state signal at all (handover-caused
    # missing signal / user did not respond) → UNKNOWN → not accepted.
    cs = _case_spec()
    tr = _trace(outcome="escalated", turns=[_turn(1, grounded=True)], signals=[])
    assert _v(cs, tr) == FAIL


def test_neutral_working_only_escalation_FAIL():
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "working")],
    )
    assert _v(cs, tr) == FAIL


def test_new_goal_only_escalation_FAIL():
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "new_request")],
    )
    assert _v(cs, tr) == FAIL


def test_pre_help_unresolved_not_post_help_FAIL():
    # The unresolved assertion is at turn 1 but the closure marker is at
    # turn 2 → the signal is NOT post-help → not a qualifying stance.
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=False), _turn(2, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    assert _v(cs, tr) == FAIL


def test_satisfaction_then_escalation_latest_wins_FAIL():
    # Customer satisfied at turn 1 (latest positive stance) but the bot
    # still escalated → FAIL.
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "satisfied", "achieved")],
    )
    assert _v(cs, tr) == FAIL


def test_goal_impossible_not_mapped_to_unresolved():
    # goal_status=impossible must NOT count as UNRESOLVED; with no positive
    # user_state it reduces to UNKNOWN → escalate not accepted.
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, None, "impossible")],
    )
    res = evaluate_conditional_outcome(cs, tr, {})
    assert res.user_state == co.UNKNOWN
    assert res.verdict == FAIL


def test_stale_adjudication_closure_version_mismatch_stays_conditional():
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    stale = _adj(cs, version="deadbeef0000")  # wrong closure-criterion version
    assert _v(cs, tr, stale) == CONDITIONAL_ELIGIBLE


def test_reject_escalation_verdict_does_not_flip_to_pass():
    # A human `reject_escalation` adjudication is an auditable record only — it
    # must NOT flip the trace to PASS (only `accept_escalation` does). The trace
    # stays CONDITIONAL_ELIGIBLE. (S-Auto-40 Phase-2: all 4 real traces REJECTED.)
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    reject = {
        "sess-X": {
            "trace_id": "sess-X", "case_id": cs.case_id,
            "closure_criterion_version": closure_criterion_version(cs),
            "verdict": "reject_escalation", "reviewer": "test",
            "rationale": "closure not met", "timestamp": "2026-06-19T00:00:00Z",
        }
    }
    assert _v(cs, tr, reject) == CONDITIONAL_ELIGIBLE


def test_adjudication_for_other_trace_does_not_apply():
    cs = _case_spec()
    tr = _trace(
        outcome="escalated",
        turns=[_turn(1, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
        session_id="sess-REAL",
    )
    other = _adj(cs, session_id="sess-DIFFERENT")
    assert _v(cs, tr, other) == CONDITIONAL_ELIGIBLE


# --- Composite-level integration: other mandatory gates still bite ------

def _run_outcome_and_composite(cs, tr):
    l2 = OutcomeChecker().run_checks(cs, tr)
    return compute_composite(
        cs.case_id, [], l2, [],
        StallResult(detected=False),
        case_spec=cs,
    )


def test_composite_satisfied_resolve_passes():
    cs = _case_spec()
    tr = _trace(outcome="resolved", turns=[_turn(1, grounded=True)],
                signals=[_sig(1, "satisfied", "achieved")])
    assert _run_outcome_and_composite(cs, tr).case_passed is True


def test_composite_conditional_eligible_does_not_pass_without_adjudication():
    cs = _case_spec()
    tr = _trace(outcome="escalated", turns=[_turn(1, grounded=True)],
                signals=[_sig(1, "unresolved_after_help")])
    # No registry entry (default load finds none for this synthetic id) →
    # correct_outcome scores 0.0 → mandatory L2 gate fails.
    cmp = _run_outcome_and_composite(cs, tr)
    assert cmp.case_passed is False


def test_composite_uc_misclass_still_fails_even_if_outcome_accepted():
    # Even an adjudicated escalation cannot rescue a UC-misclassified trace:
    # correct_uc is an independent mandatory gate.
    cs = _case_spec(primary_uc="UC-A")
    tr = _trace(outcome="escalated", turns=[_turn(1, grounded=True, active_uc="UC-B")],
                signals=[_sig(1, "unresolved_after_help")], active_uc="UC-B")
    # correct_outcome would be PASS via adjudication, but we score with the
    # default (no adjudication) loader through run_checks; regardless,
    # correct_uc fails because active_uc=UC-B != expected UC-A.
    l2 = OutcomeChecker().run_checks(cs, tr)
    uc = next(r for r in l2 if r.check_name == "correct_uc")
    assert uc.score < 1.0
    assert _run_outcome_and_composite(cs, tr).case_passed is False


def test_neighbor_without_block_uses_legacy_resolve_only():
    # A case WITHOUT the declarative block must not touch the conditional
    # evaluator: an escalate on a resolve-expected case → correct_outcome 0.0.
    cs = _case_spec(with_block=False)
    tr = _trace(outcome="escalated", turns=[_turn(1, grounded=True)],
                signals=[_sig(1, "unresolved_after_help")])
    l2 = OutcomeChecker().run_checks(cs, tr)
    oc = next(r for r in l2 if r.check_name == "correct_outcome")
    assert oc.score == 0.0


def test_evaluate_raises_without_block():
    cs = _case_spec(with_block=False)
    tr = _trace(outcome="resolved", turns=[_turn(1, grounded=True)],
                signals=[_sig(1, "satisfied", "achieved")])
    with pytest.raises(ValueError):
        evaluate_conditional_outcome(cs, tr, {})
