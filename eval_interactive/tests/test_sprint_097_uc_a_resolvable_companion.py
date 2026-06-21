"""Sprint 097 / S-Auto-45 (M-Auto-9 WP2) characterization — the satisfiable
UC-A entity-context companion `cs_uc_a_loaded_listing_resolvable`.

Zero-LLM. Pins two things the real-LLM bounded run cannot itself certify
cheaply, and which a future edit must not silently break:

1. The companion CaseSpec loads + carries the §5 trace contract (UC-A,
   resolve, no escalation, the consult→ground advisory tool sequence —
   `record_outcome` retired per the OQ-S93.1 ROUTE (a) verdict, since the
   grounding-gated terminal is the canonical one-shot closure — the
   anti-misroute forbidden tools, the identical-to-PRIMARY
   `conditional_outcome_acceptance` block).

2. The declarative conditional-outcome evaluator — the SAME generic evaluator
   the two PRIMARY use, keyed on the CaseSpec block and the simulator's own
   Phase-1 `user_state` series, NEVER on the case id — accepts the target
   SATISFIED → resolve terminal and still rejects a false resolve / an
   unsatisfied escalation. This is the "resolve CAN land, but only genuinely"
   contract WP2 exists to demonstrate.

These run entirely on synthetic traces (no LLM). They are projection /
acceptance-wiring characterization per iteration_governance §5.7 — the
real-LLM bounded run is the eval-evidence gate for the behaviour itself.
"""

from __future__ import annotations

from pathlib import Path

from eval_interactive.case_spec.loader import load_case_spec
from eval_interactive.scoring import conditional_outcome as co
from eval_interactive.scoring.conditional_outcome import (
    CONDITIONAL_ELIGIBLE,
    FAIL,
    PASS,
    evaluate_conditional_outcome,
    parse_conditional_outcome_acceptance,
)
from eval_interactive.trace.models import SessionState, TraceData, TurnTrace


_COMPANION = (
    Path(__file__).resolve().parents[1]
    / "case_specs"
    / "bad_cases"
    / "cs_uc_a_loaded_listing_resolvable.yaml"
)


def _companion():
    return load_case_spec(_COMPANION)


def _grounded_turn(idx: int, *, grounded: bool, active_uc: str = "UC-A") -> TurnTrace:
    """A grounded turn = tool call + source_ids + a non-empty answer (the
    closure-qualified marker). EXPIRED-status diagnosis stands in for the
    listing-grounded answer."""
    return TurnTrace(
        turn_index=idx,
        user_message="why can't I find my Vintage Record Player ad?",
        bot_response=(
            "Your ad AD-2007 'Vintage Record Player' has EXPIRED, so it no "
            "longer shows in search. You can repost it to make it live again."
            if grounded
            else "Have you tried checking your search filters?"
        ),
        tool_calls=[{"tool_name": "get_customer_context"}] if grounded else [],
        source_ids=["ka4P2000000063pIAA"] if grounded else [],
        phase_before="UNDERSTAND",
        phase_after="RESOLVE" if grounded else "UNDERSTAND",
        active_use_case=active_uc,
        latency_ms=1,
        projected_context={},
    )


def _trace(*, outcome: str, turns, signals, session_id="sess-wp2") -> TraceData:
    ss = SessionState(
        session_id=session_id,
        active_use_case="UC-A",
        candidate_use_cases=["UC-A"],
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


# --- 1. CaseSpec §5 trace contract --------------------------------------

def test_companion_loads_and_carries_uc_a_resolve_contract():
    cs = _companion()
    assert cs.case_id == "cs_uc_a_loaded_listing_resolvable"
    assert cs.expected.primary_uc == "UC-A"
    assert cs.expected.outcome_class == "resolve"
    # The companion expects a bot resolution, NOT an escalation.
    assert cs.expected.should_escalate is False
    assert cs.expected.escalation_trigger is None
    assert cs.expected.allow_bot_resolution == "true"
    # VISIBLE development/contract anchor (loaded from the bad_cases suite).
    assert cs.source_suite == "bad_cases"
    assert cs.closure_criterion is not None


def test_companion_tool_sequence_is_consult_then_ground_record_outcome_retired():
    # S-Auto-48 (M-Auto-10 WP2 route-(a) follow-up): `record_outcome` is RETIRED
    # from the required canonical one-shot sequence per the OQ-S93.1 ROUTE (a)
    # verdict. On the canonical one-shot satisfiable flow the simulator ends on
    # `goal_achieved` before a CONFIRM turn, so the grounding-gated
    # `isResolvedSuccessTerminal` terminal — not a landed `record_outcome` — is
    # the canonical closure. `tool_sequence_match` is advisory severity (it does
    # not gate the composite outcome score), so this correction changes no scored
    # acceptance bar; the conditional-outcome bar is pinned unchanged below.
    cs = _companion()
    assert cs.expected.expected_tool_sequence == [
        "classify_use_case",
        "get_customer_context",
        "search_knowledge",
        "resolve_article",
    ]
    # The overly-literal explicit-record_outcome trace expectation is retired:
    # the canonical one-shot satisfiable sequence does NOT require record_outcome.
    assert "record_outcome" not in cs.expected.expected_tool_sequence
    # Anti-misroute fences: an EXPIRED lifecycle question must not become a
    # UC-K case creation or a message-moderation (UC-FP-adjacent) probe.
    assert "create_case_controlled" in cs.expected.forbidden_tools
    assert "get_message_moderation_context" in cs.expected.forbidden_tools


def test_companion_acceptance_block_is_identical_shape_to_primary():
    # The companion differs from the PRIMARY PRIMARILY in the persona, NOT in
    # the acceptance mechanism: the declarative block is the same generic
    # shape (satisfied→resolve; unresolved→escalate is at most conditional).
    cs = _companion()
    acc = parse_conditional_outcome_acceptance(cs.conditional_outcome_acceptance)
    assert acc is not None
    assert acc.satisfied_outcome == "resolve"
    assert acc.unresolved_accept_outcome == "escalate"
    assert acc.require_closure_precondition is True

    primary = load_case_spec(_COMPANION.parent / "cs_uc_a_loaded_listing.yaml")
    pacc = parse_conditional_outcome_acceptance(
        primary.conditional_outcome_acceptance
    )
    assert (acc.satisfied_outcome, acc.unresolved_accept_outcome,
            acc.require_closure_precondition) == (
        pacc.satisfied_outcome, pacc.unresolved_accept_outcome,
        pacc.require_closure_precondition,
    )


# --- 2. The target terminal: SATISFIED → resolve LANDS ------------------

def test_satisfied_resolve_lands_pass():
    """The §5 target: grounded EXPIRED diagnosis + simulator user_state
    `satisfied` + containment resolved → PASS (resolve CAN land).

    This proves a genuine SATISFIED + grounded interaction records RESOLVE via
    the CANONICAL grounding-gated terminal: the trace carries NO `record_outcome`
    tool call, yet the scored conditional-outcome bar PASSes from
    `containment_outcome=resolved` + the satisfied user_state. This is precisely
    why the explicit-`record_outcome` `expected_tool_sequence` requirement could
    be retired (OQ-S93.1 route (a)) without touching any scored acceptance bar.
    """
    cs = _companion()
    tr = _trace(
        outcome="resolved",
        turns=[_grounded_turn(1, grounded=True)],
        signals=[_sig(1, "satisfied", "achieved")],
    )
    res = evaluate_conditional_outcome(cs, tr, {})
    assert res.verdict == PASS
    assert res.user_state == co.SATISFIED
    assert res.closure_present is True


def test_false_resolve_on_unresolved_user_fails():
    """A resolve recorded while the customer is still positively UNRESOLVED
    after help is a false resolve → FAIL (no metric-forced resolve)."""
    cs = _companion()
    tr = _trace(
        outcome="resolved",
        turns=[_grounded_turn(1, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    assert evaluate_conditional_outcome(cs, tr, {}).verdict == FAIL


def test_unsatisfied_escalation_is_not_auto_passed():
    """Control value: a grounded answer the user is NOT satisfied with, then
    an escalate, is at most CONDITIONAL_ELIGIBLE (never auto-PASS) — the
    companion does not get a free pass for shallow/incomplete help."""
    cs = _companion()
    tr = _trace(
        outcome="escalated",
        turns=[_grounded_turn(1, grounded=True)],
        signals=[_sig(1, "unresolved_after_help")],
    )
    res = evaluate_conditional_outcome(cs, tr, {})
    assert res.verdict == CONDITIONAL_ELIGIBLE
    assert res.score == 0.0


def test_generic_answer_no_signal_does_not_resolve():
    """A generic, non-grounded answer (no tool call / no source_ids) that
    escalates with no positive user_state → FAIL. Mirrors the FAIL condition
    (e): merely reverting to generic FAQ advice does not satisfy."""
    cs = _companion()
    tr = _trace(
        outcome="escalated",
        turns=[_grounded_turn(1, grounded=False)],
        signals=[],
    )
    assert evaluate_conditional_outcome(cs, tr, {}).verdict == FAIL


def test_goal_impossible_alone_is_not_unresolved():
    """Per the recorded M-Auto-9 product decision: simulator `goal_impossible`
    alone is a neutral signal (UNKNOWN), never coerced to UNRESOLVED. With a
    grounded escalate it is therefore not accepted (FAIL) — and it does NOT
    auto-hard-fail a resolve either."""
    cs = _companion()
    tr = _trace(
        outcome="escalated",
        turns=[_grounded_turn(1, grounded=True)],
        signals=[_sig(1, None, "impossible")],
    )
    res = evaluate_conditional_outcome(cs, tr, {})
    assert res.user_state == co.UNKNOWN
    assert res.verdict == FAIL


def test_acceptance_keys_on_user_state_not_case_id():
    """Anti-hardcode: the verdict is driven by the simulator user_state
    series, not the case id. The SAME trace shape flips PASS/FAIL purely on
    the user_state signal, proving acceptance is `user_state`-driven."""
    cs = _companion()
    base_turns = [_grounded_turn(1, grounded=True)]
    sat = _trace(outcome="resolved", turns=base_turns,
                 signals=[_sig(1, "satisfied", "achieved")])
    unres = _trace(outcome="resolved", turns=base_turns,
                   signals=[_sig(1, "unresolved_after_help")])
    assert evaluate_conditional_outcome(cs, sat, {}).verdict == PASS
    assert evaluate_conditional_outcome(cs, unres, {}).verdict == FAIL
