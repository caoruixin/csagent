"""Tests for the OutcomeChecker (L2) module."""

from __future__ import annotations

import pytest

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.outcome_checks import OutcomeChecker, OutcomeCheckResult
from eval_interactive.trace.models import (
    HandoverData,
    SessionState,
    TraceData,
    TurnTrace,
)


def _make_case_spec(
    outcome_checks: list[str] | None = None,
    outcome_class: str = "resolve",
    primary_uc: str = "UC-A",
    secondary_ucs: list[str] | None = None,
    should_escalate: bool = False,
    expected_tool_sequence: list[str] | None = None,
    max_turns: int = 10,
) -> CaseSpec:
    if outcome_checks is None:
        outcome_checks = OutcomeChecker.ALL_CHECKS
    return CaseSpec(
        case_id="test-oc-001",
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
            secondary_ucs=secondary_ucs or [],
            should_escalate=should_escalate,
            expected_tool_sequence=expected_tool_sequence or [],
            max_turns=max_turns,
        ),
        scoring=ScoringConfig(
            hard_checks=[],
            outcome_checks=outcome_checks,
            llm_judge_dimensions=[],
        ),
    )


def _make_trace(
    turns: list[TurnTrace] | None = None,
    active_use_case: str = "UC-A",
    candidate_use_cases: list[str] | None = None,
    containment_outcome: str = "resolved",
    total_bot_turns: int = 3,
    handover: HandoverData | None = None,
) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="test-session",
            active_use_case=active_use_case,
            candidate_use_cases=candidate_use_cases or [],
            containment_outcome=containment_outcome,
            escalation_reason="",
            total_bot_turns=total_bot_turns,
            clarification_count=0,
            faq_miss_count=0,
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
    tool_calls: list[dict] | None = None,
    source_ids: list[str] | None = None,
    action_selected: str = "answer",
    phase_before: str = "RESOLVE",
    phase_after: str = "RESOLVE",
) -> TurnTrace:
    return TurnTrace(
        turn_index=turn_index,
        user_message="hello",
        bot_response="response",
        action_selected=action_selected,
        tool_calls=tool_calls or [],
        source_ids=source_ids or [],
        phase_before=phase_before,
        phase_after=phase_after,
        active_use_case="UC-A",
        latency_ms=100,
        projected_context={},
    )


class TestCorrectUC:
    def test_pass_matching_uc(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_uc"], primary_uc="UC-A")
        trace = _make_trace(active_use_case="UC-A")
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_fail_mismatched_uc(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_uc"], primary_uc="UC-A")
        trace = _make_trace(active_use_case="UC-B")
        results = checker.run_checks(case, trace)
        assert results[0].score == 0.0

    def test_case_insensitive(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_uc"], primary_uc="uc-a")
        trace = _make_trace(active_use_case="UC-A")
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_strips_numeric_suffix(self):
        """UC-A-1 should match UC-A."""
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_uc"], primary_uc="UC-A-1")
        trace = _make_trace(active_use_case="UC-A-2")
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0


class TestCorrectOutcome:
    def test_pass_resolve_resolved(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_outcome"], outcome_class="resolve")
        trace = _make_trace(containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_pass_escalate_escalated(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_outcome"], outcome_class="escalate")
        trace = _make_trace(containment_outcome="escalated")
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_fail_resolve_but_escalated(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_outcome"], outcome_class="resolve")
        trace = _make_trace(containment_outcome="escalated")
        results = checker.run_checks(case, trace)
        assert results[0].score == 0.0


class TestToolSequenceMatch:
    def test_pass_exact_match(self):
        checker = OutcomeChecker()
        case = _make_case_spec(
            outcome_checks=["tool_sequence_match"],
            expected_tool_sequence=["search_knowledge", "resolve_article"],
        )
        turns = [
            _make_turn(tool_calls=[{"tool_name": "search_knowledge"}]),
            _make_turn(turn_index=2, tool_calls=[{"tool_name": "resolve_article"}]),
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_pass_no_expected_sequence(self):
        checker = OutcomeChecker()
        case = _make_case_spec(
            outcome_checks=["tool_sequence_match"],
            expected_tool_sequence=[],
        )
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_partial_credit(self):
        checker = OutcomeChecker()
        case = _make_case_spec(
            outcome_checks=["tool_sequence_match"],
            expected_tool_sequence=["search_knowledge", "resolve_article"],
        )
        turns = [_make_turn(tool_calls=[{"tool_name": "search_knowledge"}])]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        assert 0.0 < results[0].score < 1.0


class TestTurnEfficiency:
    def test_pass_within_budget(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["turn_efficiency"], max_turns=10)
        trace = _make_trace(total_bot_turns=5)
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_pass_at_exact_budget(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["turn_efficiency"], max_turns=10)
        trace = _make_trace(total_bot_turns=10)
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_decay_over_budget(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["turn_efficiency"], max_turns=10)
        trace = _make_trace(total_bot_turns=15)
        results = checker.run_checks(case, trace)
        assert results[0].score == 0.5

    def test_zero_at_double_budget(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["turn_efficiency"], max_turns=10)
        trace = _make_trace(total_bot_turns=20)
        results = checker.run_checks(case, trace)
        assert results[0].score == 0.0


class TestHandoverCompleteness:
    def test_pass_not_escalated(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["handover_completeness"])
        trace = _make_trace(containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_fail_no_handover_data(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["handover_completeness"])
        trace = _make_trace(containment_outcome="escalated", handover=None)
        results = checker.run_checks(case, trace)
        assert results[0].score == 0.0

    def test_pass_complete_handover(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["handover_completeness"])
        handover = HandoverData(
            log_id="log1",
            session_id="s1",
            handover_payload={
                "session_id": "s1",
                "primary_use_case": "UC-A",
                "summary": "User issue",
                "escalation_reason": "complex",
                "total_bot_turns": 5,
            },
            customer_message="",
            transcript=[],
            transfer_result="",
        )
        trace = _make_trace(containment_outcome="escalated", handover=handover)
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_partial_handover(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["handover_completeness"])
        handover = HandoverData(
            log_id="log1",
            session_id="s1",
            handover_payload={
                "session_id": "s1",
                "primary_use_case": "UC-A",
                # missing summary, escalation_reason, total_bot_turns
            },
            customer_message="",
            transcript=[],
            transfer_result="",
        )
        trace = _make_trace(containment_outcome="escalated", handover=handover)
        results = checker.run_checks(case, trace)
        assert 0.0 < results[0].score < 1.0


class TestIssuePreservation:
    def test_pass_primary_in_candidates(self):
        checker = OutcomeChecker()
        case = _make_case_spec(
            outcome_checks=["issue_preservation"],
            primary_uc="UC-A",
        )
        trace = _make_trace(candidate_use_cases=["UC-A", "UC-B"])
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0

    def test_partial_secondary_preserved(self):
        checker = OutcomeChecker()
        case = _make_case_spec(
            outcome_checks=["issue_preservation"],
            primary_uc="UC-A",
            secondary_ucs=["UC-B"],
        )
        trace = _make_trace(candidate_use_cases=["UC-B", "UC-C"])
        results = checker.run_checks(case, trace)
        assert results[0].score == 0.5

    def test_fail_nothing_preserved(self):
        checker = OutcomeChecker()
        case = _make_case_spec(
            outcome_checks=["issue_preservation"],
            primary_uc="UC-A",
        )
        trace = _make_trace(candidate_use_cases=["UC-X", "UC-Y"])
        results = checker.run_checks(case, trace)
        assert results[0].score == 0.0

    def test_pass_empty_candidates(self):
        checker = OutcomeChecker()
        case = _make_case_spec(
            outcome_checks=["issue_preservation"],
            primary_uc="UC-A",
        )
        trace = _make_trace(candidate_use_cases=[])
        results = checker.run_checks(case, trace)
        assert results[0].score == 1.0


class TestRunChecksFiltering:
    def test_only_configured_checks_run(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=["correct_uc", "turn_efficiency"])
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        names = [r.check_name for r in results]
        assert "correct_uc" in names
        assert "turn_efficiency" in names
        assert len(names) == 2

    def test_empty_outcome_checks_returns_nothing(self):
        checker = OutcomeChecker()
        case = _make_case_spec(outcome_checks=[])
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        assert results == []
