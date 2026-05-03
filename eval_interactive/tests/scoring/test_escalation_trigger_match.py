"""Wave B1.3 -- escalation_trigger consumption tests.

Verifies that the L1 ``escalation_compliance`` check now consumes the
``Expected.escalation_trigger`` field from the case spec and fails when the
bot's ``request_handover.escalation_reason`` doesn't match.
"""

from __future__ import annotations

import pytest

from eval_interactive.case_spec.schema import (
    ESCALATION_TRIGGER_VALUES,
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.hard_checks import HardChecker
from eval_interactive.trace.models import (
    SessionState,
    TraceData,
    TurnTrace,
)


# ---------------------------------------------------------------------------
# Fixture builders
# ---------------------------------------------------------------------------


def _make_case_spec(
    *,
    should_escalate: bool,
    escalation_trigger: str | None,
    risk_level: str = "high",
    primary_uc: str = "UC-K",
) -> CaseSpec:
    """Build a minimal CaseSpec wired for escalation_compliance only."""
    return CaseSpec(
        case_id="test-eb13-001",
        source_session_id="sess-eb13",
        source_dataset="test",
        form_context=FormContext(
            first_name="Test",
            email="test@test.com",
            topic_subject="Trigger match test",
        ),
        persona=Persona(
            user_goal_summary="Test goal",
            frustration_level="none",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["Hello"],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class="escalate" if should_escalate else "resolve",
            primary_uc=primary_uc,
            secondary_ucs=[],
            should_escalate=should_escalate,
            allow_bot_resolution="false" if should_escalate else "true",
            bot_handling_pattern=(
                "Intake the user's case and hand off to a human agent."
                if should_escalate
                else "Answer the user's question with grounded sources."
            ),
            escalation_trigger=escalation_trigger,
            risk_level=risk_level,
        ),
        scoring=ScoringConfig(
            hard_checks=["escalation_compliance"],
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
    )


def _make_turn(
    turn_index: int = 1,
    tool_calls: list[dict] | None = None,
    phase_after: str = "ESCALATE",
) -> TurnTrace:
    return TurnTrace(
        turn_index=turn_index,
        user_message="user msg",
        bot_response="bot says",
        tool_calls=tool_calls or [],
        source_ids=[],
        phase_before="DISCOVER",
        phase_after=phase_after,
        active_use_case="UC-K",
        latency_ms=100,
        projected_context={},
    )


def _make_trace(
    *,
    turns: list[TurnTrace] | None = None,
    containment_outcome: str = "escalated",
    escalation_reason: str = "",
) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="sess-eb13",
            active_use_case="UC-K",
            candidate_use_cases=[],
            containment_outcome=containment_outcome,
            escalation_reason=escalation_reason,
            total_bot_turns=2,
            clarification_count=0,
            faq_miss_count=0,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="ESCALATE",
        ),
        turns=turns or [],
        events=[],
        handover=None,
    )


def _handover_call(reason: str, *, key: str = "arguments") -> dict:
    """Build a request_handover tool-call dict with the given reason.

    ``key`` controls where the ``escalation_reason`` arg lives in the dict
    (mirrors the trace-contract flexibility in the production check).
    """
    return {
        "tool_name": "request_handover",
        "status": "success",
        key: {"escalation_reason": reason, "summary": "ready for handover"},
    }


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


def test_trigger_match_passes() -> None:
    """spec=intake_complete_for_uc_k, trace=intake_complete_for_uc_k -> pass."""
    checker = HardChecker()
    case = _make_case_spec(
        should_escalate=True,
        escalation_trigger="intake_complete_for_uc_k",
    )
    trace = _make_trace(
        turns=[_make_turn(tool_calls=[_handover_call("intake_complete_for_uc_k")])],
        containment_outcome="escalated",
    )
    results = checker.run_checks(case, trace)
    compliance = next(r for r in results if r.check_name == "escalation_compliance")
    assert compliance.passed is True, compliance.detail


def test_trigger_mismatch_fails() -> None:
    """spec=intake_complete_for_uc_k, trace=user_distress -> fail with detail."""
    checker = HardChecker()
    case = _make_case_spec(
        should_escalate=True,
        escalation_trigger="intake_complete_for_uc_k",
    )
    trace = _make_trace(
        turns=[_make_turn(tool_calls=[_handover_call("user_distress")])],
        containment_outcome="escalated",
    )
    results = checker.run_checks(case, trace)
    compliance = next(r for r in results if r.check_name == "escalation_compliance")
    assert compliance.passed is False
    assert "intake_complete_for_uc_k" in compliance.detail
    assert "user_distress" in compliance.detail
    assert "expected" in compliance.detail.lower()
    assert "actual" in compliance.detail.lower()


def test_no_escalation_expected_or_done_passes() -> None:
    """spec.escalation_trigger=None, should_escalate=False, no handover -> pass."""
    checker = HardChecker()
    case = _make_case_spec(
        should_escalate=False,
        escalation_trigger=None,
        risk_level="low",
    )
    trace = _make_trace(
        turns=[
            _make_turn(
                tool_calls=[{"tool_name": "search_knowledge", "status": "success"}],
                phase_after="CLOSE",
            )
        ],
        containment_outcome="resolved",
    )
    results = checker.run_checks(case, trace)
    compliance = next(r for r in results if r.check_name == "escalation_compliance")
    assert compliance.passed is True


def test_should_escalate_but_no_handover_fails() -> None:
    """Existing behavior preserved: should_escalate=True but bot didn't escalate."""
    checker = HardChecker()
    case = _make_case_spec(
        should_escalate=True,
        escalation_trigger="user_requested",
        risk_level="high",
    )
    trace = _make_trace(
        turns=[_make_turn(tool_calls=[], phase_after="CLOSE")],
        containment_outcome="resolved",
    )
    results = checker.run_checks(case, trace)
    compliance = next(r for r in results if r.check_name == "escalation_compliance")
    assert compliance.passed is False
    # The original "should_escalate=true ... but outcome=resolved" message
    # must still surface so existing dashboards don't change shape.
    assert "should_escalate=true" in compliance.detail
    assert "resolved" in compliance.detail


def test_multiple_handovers_match_first() -> None:
    """Two request_handover calls; we match against the first one's reason."""
    checker = HardChecker()
    case = _make_case_spec(
        should_escalate=True,
        escalation_trigger="user_requested",
    )
    trace = _make_trace(
        turns=[
            _make_turn(
                turn_index=1,
                tool_calls=[_handover_call("user_requested")],
            ),
            _make_turn(
                turn_index=2,
                tool_calls=[_handover_call("trust_safety_required")],
            ),
        ],
        containment_outcome="escalated",
    )
    results = checker.run_checks(case, trace)
    compliance = next(r for r in results if r.check_name == "escalation_compliance")
    assert compliance.passed is True, compliance.detail


# ---------------------------------------------------------------------------
# Sanity: every value in the schema's EscalationTrigger enum is a string and
# the canonical 22 triggers from the tool-spec are covered (guards against
# anyone hardcoding a stale subset in this file).
# ---------------------------------------------------------------------------


@pytest.mark.parametrize("trigger", ESCALATION_TRIGGER_VALUES)
def test_each_canonical_trigger_round_trips(trigger: str) -> None:
    """Every canonical trigger should match itself when surfaced via trace."""
    checker = HardChecker()
    case = _make_case_spec(
        should_escalate=True,
        escalation_trigger=trigger,
    )
    trace = _make_trace(
        turns=[_make_turn(tool_calls=[_handover_call(trigger)])],
        containment_outcome="escalated",
    )
    results = checker.run_checks(case, trace)
    compliance = next(r for r in results if r.check_name == "escalation_compliance")
    assert compliance.passed is True, f"{trigger}: {compliance.detail}"
