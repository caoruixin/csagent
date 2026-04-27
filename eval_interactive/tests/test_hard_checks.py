"""Tests for the HardChecker (L1) module."""

from __future__ import annotations

import pytest

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.hard_checks import HardChecker, HardCheckResult
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.trace.models import (
    EventEntry,
    HandoverData,
    SessionState,
    TraceData,
    TurnTrace,
)


def _make_case_spec(
    hard_checks: list[str] | None = None,
    outcome_class: str = "resolve",
    primary_uc: str = "UC-A",
    should_escalate: bool = False,
    risk_level: str = "low",
    forbidden_tools: list[str] | None = None,
    grounding_mode: str = "faq_source_backed",
    answer_must_not_contain: list[str] | None = None,
    max_turns: int = 10,
) -> CaseSpec:
    if hard_checks is None:
        hard_checks = HardChecker.ALL_CHECKS
    return CaseSpec(
        case_id="test-hc-001",
        source_session_id="sess-001",
        source_dataset="test",
        form_context=FormContext(
            first_name="Test",
            email="test@test.com",
            topic_subject="Test topic",
        ),
        persona=Persona(
            goal_summary="Test goal",
            frustration_level="none",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["Hello"],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class=outcome_class,
            primary_uc=primary_uc,
            secondary_ucs=[],
            should_escalate=should_escalate,
            risk_level=risk_level,
            forbidden_tools=forbidden_tools or [],
            grounding_mode=grounding_mode,
            answer_must_not_contain=answer_must_not_contain or [],
            max_turns=max_turns,
        ),
        scoring=ScoringConfig(
            hard_checks=hard_checks,
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
    )


def _make_trace(
    turns: list[TurnTrace] | None = None,
    active_use_case: str = "UC-A",
    containment_outcome: str = "resolved",
    total_bot_turns: int = 3,
    clarification_count: int = 0,
    faq_miss_count: int = 0,
    handover: HandoverData | None = None,
) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="test-session",
            active_use_case=active_use_case,
            candidate_use_cases=[],
            containment_outcome=containment_outcome,
            escalation_reason="",
            total_bot_turns=total_bot_turns,
            clarification_count=clarification_count,
            faq_miss_count=faq_miss_count,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="CLOSE",
        ),
        turns=turns or [],
        events=[],
        handover=handover,
    )


def _make_turn(
    turn_index: int = 1,
    user_message: str = "hello",
    bot_response: str = "I can help",
    action_selected: str = "answer",
    tool_calls: list[dict] | None = None,
    source_ids: list[str] | None = None,
    phase_before: str = "DISCOVER",
    phase_after: str = "RESOLVE",
) -> TurnTrace:
    return TurnTrace(
        turn_index=turn_index,
        user_message=user_message,
        bot_response=bot_response,
        action_selected=action_selected,
        tool_calls=tool_calls or [],
        source_ids=source_ids or [],
        phase_before=phase_before,
        phase_after=phase_after,
        active_use_case="UC-A",
        latency_ms=100,
        projected_context={},
    )


class TestNoForbiddenTools:
    def test_pass_no_forbidden_tools_configured(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_forbidden_tools"], forbidden_tools=[])
        trace = _make_trace(turns=[_make_turn()])
        results = checker.run_checks(case, trace)
        assert len(results) == 1
        assert results[0].passed is True

    def test_pass_no_forbidden_tools_invoked(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["no_forbidden_tools"],
            forbidden_tools=["delete_account"],
        )
        turn = _make_turn(tool_calls=[{"tool_name": "search_knowledge"}])
        trace = _make_trace(turns=[turn])
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_fail_forbidden_tool_invoked(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["no_forbidden_tools"],
            forbidden_tools=["delete_account"],
        )
        turn = _make_turn(tool_calls=[{"tool_name": "delete_account"}])
        trace = _make_trace(turns=[turn])
        results = checker.run_checks(case, trace)
        assert results[0].passed is False
        assert "delete_account" in results[0].detail


class TestBudgetEnforcement:
    def test_pass_within_budget(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"], max_turns=10)
        trace = _make_trace(total_bot_turns=5, clarification_count=1, faq_miss_count=1)
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_fail_too_many_clarifications(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"])
        trace = _make_trace(clarification_count=3)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False
        assert "clarification_count" in results[0].detail

    def test_fail_too_many_faq_misses(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"])
        trace = _make_trace(faq_miss_count=3)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False
        assert "faq_miss_count" in results[0].detail

    def test_fail_exceeds_max_turns(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"], max_turns=5)
        trace = _make_trace(total_bot_turns=6)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False
        assert "total_bot_turns" in results[0].detail


class TestPhaseTransitionValidity:
    def test_pass_valid_transitions(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["phase_transition_validity"])
        turns = [
            _make_turn(turn_index=1, phase_before="INIT", phase_after="DISCOVER"),
            _make_turn(turn_index=2, phase_before="DISCOVER", phase_after="RESOLVE"),
            _make_turn(turn_index=3, phase_before="RESOLVE", phase_after="CONFIRM"),
            _make_turn(turn_index=4, phase_before="CONFIRM", phase_after="CLOSE"),
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_pass_same_phase_stays(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["phase_transition_validity"])
        turns = [
            _make_turn(turn_index=1, phase_before="DISCOVER", phase_after="DISCOVER"),
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_fail_invalid_transition(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["phase_transition_validity"])
        turns = [
            _make_turn(turn_index=1, phase_before="INIT", phase_after="CLOSE"),
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False
        assert "INIT->CLOSE" in results[0].detail


class TestNoCriticalPolicyViolation:
    def test_pass_clean_responses(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_critical_policy_violation"])
        turns = [_make_turn(bot_response="I can help you with that.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_fail_forbidden_phrase(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_critical_policy_violation"])
        turns = [_make_turn(bot_response="As an AI language model, I cannot do that.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False

    def test_fail_answer_must_not_contain(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["no_critical_policy_violation"],
            answer_must_not_contain=["cancel your account"],
        )
        turns = [_make_turn(bot_response="You should cancel your account to fix this.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False
        assert "cancel your account" in results[0].detail


class TestNoPiiLeakage:
    def test_pass_no_pii(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [_make_turn(bot_response="Your ad has been updated.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_fail_email_in_response(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [_make_turn(bot_response="Your email is user@example.com")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False

    def test_fail_phone_in_response(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [_make_turn(bot_response="Your phone number is 07123456789")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].passed is False


class TestEscalationCompliance:
    def test_pass_escalation_not_required(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["escalation_compliance"],
            should_escalate=False,
        )
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_pass_escalation_required_and_done(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["escalation_compliance"],
            should_escalate=True,
            risk_level="critical",
        )
        trace = _make_trace(containment_outcome="escalated")
        results = checker.run_checks(case, trace)
        assert results[0].passed is True

    def test_fail_escalation_required_but_not_done(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["escalation_compliance"],
            should_escalate=True,
            risk_level="high",
        )
        trace = _make_trace(containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        assert results[0].passed is False


class TestNoStall:
    def test_pass_no_stall_detected(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_stall"])
        trace = _make_trace()
        stall = StallResult(detected=False)
        results = checker.run_checks(case, trace, stall)
        assert results[0].passed is True

    def test_fail_stall_detected(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_stall"])
        trace = _make_trace()
        stall = StallResult(
            detected=True,
            turn_index=3,
            failure_tag="STALL_AFTER_TOOL_INTENT",
        )
        results = checker.run_checks(case, trace, stall)
        assert results[0].passed is False
        assert "STALL_AFTER_TOOL_INTENT" in results[0].detail

    def test_pass_no_stall_result_provided(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_stall"])
        trace = _make_trace()
        results = checker.run_checks(case, trace, stall_result=None)
        assert results[0].passed is True


class TestRunChecksFiltering:
    def test_only_configured_checks_run(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage", "budget_enforcement"])
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        names = sorted([r.check_name for r in results])
        assert names == ["budget_enforcement", "no_pii_leakage"]

    def test_empty_hard_checks_returns_nothing(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        assert results == []
