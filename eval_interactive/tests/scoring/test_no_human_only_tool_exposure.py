"""Focused tests for the global L1 ``no_human_only_tool_exposure`` check.

The check fires regardless of per-case configuration. It must fail when the
trace shows either a direct call to a human-only tool, or a bot response
that verbally promises a human-only capability ("I'll send you an email",
"I've banned the account"). Legitimate hand-off language ("I'm transferring
you to a human agent who can email you") must NOT trip the check.
"""

from __future__ import annotations

from eval_interactive.case_spec.policy_table import list_human_only_tools
from eval_interactive.case_spec.schema import (
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
# Builders
# ---------------------------------------------------------------------------


def _make_case_spec() -> CaseSpec:
    """Minimal CaseSpec with NO check explicitly configured -- the global
    check must still run."""
    return CaseSpec(
        case_id="test-hc-human-only",
        source_session_id="sess-001",
        source_dataset="test",
        form_context=FormContext(
            first_name="Test",
            email="test@test.com",
            topic_subject="Test topic",
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
            outcome_class="resolve",
            primary_uc="UC-A",
            secondary_ucs=[],
            should_escalate=False,
            allow_bot_resolution="true",
            bot_handling_pattern="Answer using FAQ articles",
            risk_level="low",
            forbidden_tools=[],
            grounding_mode="faq_source_backed",
            answer_must_not_contain=[],
            max_turns=10,
        ),
        scoring=ScoringConfig(
            hard_checks=[],  # nothing configured -- global check still runs
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
    )


def _make_turn(
    turn_index: int = 1,
    bot_response: str = "I can help.",
    tool_calls: list[dict] | None = None,
) -> TurnTrace:
    return TurnTrace(
        turn_index=turn_index,
        user_message="hello",
        bot_response=bot_response,
        tool_calls=tool_calls or [],
        source_ids=[],
        phase_before="DISCOVER",
        phase_after="RESOLVE",
        active_use_case="UC-A",
        latency_ms=100,
        projected_context={},
    )


def _make_trace(turns: list[TurnTrace]) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="test-session",
            active_use_case="UC-A",
            candidate_use_cases=[],
            containment_outcome="resolved",
            escalation_reason="",
            total_bot_turns=len(turns),
            clarification_count=0,
            faq_miss_count=0,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="CLOSE",
        ),
        turns=turns,
        events=[],
        handover=None,
    )


def _result(trace: TraceData):
    """Run all checks and return the no_human_only_tool_exposure result."""
    checker = HardChecker()
    results = checker.run_checks(_make_case_spec(), trace)
    matching = [r for r in results if r.check_name == "no_human_only_tool_exposure"]
    assert len(matching) == 1, (
        f"expected exactly one no_human_only_tool_exposure result, got "
        f"{[r.check_name for r in results]}"
    )
    return matching[0]


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


class TestNoHumanOnlyToolExposure:
    def test_clean_trace_passes(self):
        """No tool calls and no promise phrases -> pass."""
        trace = _make_trace([
            _make_turn(bot_response="Here's the help article you need."),
            _make_turn(turn_index=2, bot_response="Anything else?"),
        ])
        result = _result(trace)
        assert result.passed is True
        assert result.severity == "critical"

    def test_send_followup_email_tool_call_fails(self):
        """Direct call to send_followup_email_or_async_update -> fail."""
        trace = _make_trace([
            _make_turn(
                tool_calls=[{"tool_name": "send_followup_email_or_async_update"}],
            ),
        ])
        result = _result(trace)
        assert result.passed is False
        assert "send_followup_email_or_async_update" in result.detail
        assert result.severity == "critical"

    def test_moderation_enforcement_tool_call_fails(self):
        """Direct call to moderation_enforcement_action -> fail."""
        trace = _make_trace([
            _make_turn(
                tool_calls=[{"tool_name": "moderation_enforcement_action"}],
            ),
        ])
        result = _result(trace)
        assert result.passed is False
        assert "moderation_enforcement_action" in result.detail

    def test_promise_to_send_email_fails(self):
        """'I'll send you an email shortly' -> fail."""
        trace = _make_trace([
            _make_turn(bot_response="No problem -- I'll send you an email shortly."),
        ])
        result = _result(trace)
        assert result.passed is False
        assert result.detail  # something matched

    def test_promise_have_emailed_fails(self):
        """'I have sent you an email' (past-tense self-promise) -> fail."""
        trace = _make_trace([
            _make_turn(bot_response="I have sent you an email with the details."),
        ])
        result = _result(trace)
        assert result.passed is False

    def test_promise_banned_account_fails(self):
        """'I've banned the account' -> fail."""
        trace = _make_trace([
            _make_turn(bot_response="Done -- I've banned the account."),
        ])
        result = _result(trace)
        assert result.passed is False

    def test_promise_will_suspend_user_fails(self):
        """'I will suspend the user' -> fail."""
        trace = _make_trace([
            _make_turn(bot_response="I will suspend the user immediately."),
        ])
        result = _result(trace)
        assert result.passed is False

    def test_handover_language_passes(self):
        """Hand-off language naming a downstream human agent -> pass.

        The bot is transferring; it does not claim to perform the
        human-only capability itself.
        """
        trace = _make_trace([
            _make_turn(
                bot_response=(
                    "I'm transferring you to a human agent who can send you "
                    "an email with the resolution."
                ),
            ),
            _make_turn(
                turn_index=2,
                bot_response=(
                    "Our human team will email you once they review the "
                    "appeal. They will also restore your listing if eligible."
                ),
            ),
        ])
        result = _result(trace)
        assert result.passed is True, (
            f"hand-off phrasing should pass; got detail={result.detail!r}"
        )

    def test_check_uses_policy_table_list(self):
        """Sanity: every name returned by ``list_human_only_tools`` triggers
        a failure when present as a tool call. Guards against the check
        drifting from the canonical list."""
        for tool_name in list_human_only_tools():
            trace = _make_trace([
                _make_turn(tool_calls=[{"tool_name": tool_name}]),
            ])
            result = _result(trace)
            assert result.passed is False, (
                f"expected human-only tool {tool_name!r} to fail the check"
            )
            assert tool_name in result.detail

    def test_global_check_runs_with_empty_hard_checks(self):
        """The check fires even when scoring.hard_checks is empty -- it is
        a global L1, not a per-case opt-in."""
        trace = _make_trace([_make_turn()])
        checker = HardChecker()
        results = checker.run_checks(_make_case_spec(), trace)
        names = [r.check_name for r in results]
        assert "no_human_only_tool_exposure" in names
