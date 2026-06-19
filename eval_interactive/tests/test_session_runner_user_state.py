"""Phase-1 (S-Auto-40 WP1-A) tests for the per-turn ``user_state`` signal
series accumulated by ``SessionRunner.run_session``.

These cover the measurement contract wiring only (no real LLM): each
``generate_next`` call's positive structured ``user_state`` is persisted in
the SAME turn, tagged with provenance (``turn_id`` / ``signal_source`` /
``schema_version``), and NO signal is fabricated after a ``bot_ended`` break
(the escalation path never calls ``generate_next``, so it carries no terminal
signal — by design, read as UNKNOWN downstream).
"""

from __future__ import annotations

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.simulator.session_runner import SessionRunner
from eval_interactive.simulator.user_simulator import (
    USER_STATE_SCHEMA_VERSION,
    USER_STATE_SIGNAL_SOURCE,
)


def _case_spec(max_turns: int = 5) -> CaseSpec:
    return CaseSpec(
        case_id="cs_uc_a_phase1_probe",
        source_session_id="sess-probe",
        source_dataset="test",
        form_context=FormContext(
            first_name="Sam", email="s@e.com", topic_subject="Ad Support",
            description="my ad isn't showing",
        ),
        persona=Persona(
            user_goal_summary="find ad", frustration_level="mild",
            verbosity="normal", drift_behavior="none",
            seed_messages=["my ad isn't showing"], hidden_facts=[],
        ),
        expected=Expected(
            outcome_class="resolve", primary_uc="UC-A", secondary_ucs=[],
            should_escalate=False, allow_bot_resolution="true", max_turns=max_turns,
        ),
        scoring=ScoringConfig(hard_checks=[], outcome_checks=["correct_outcome"],
                              llm_judge_dimensions=[]),
    )


class _FakeAgent:
    """Scripted bot. ``bot_turns`` is a list of (reply_text, should_end)."""

    def __init__(self, bot_turns):
        self._bot_turns = list(bot_turns)
        self._i = 0

    def create_session(self, form_dict):
        return {"session_id": "sess-probe", "reply_text": "Hi, how can I help?"}

    def send_message(self, session_id, user_msg):
        reply, should_end = self._bot_turns[self._i]
        self._i += 1
        return {"reply_text": reply, "should_end_chat": should_end}


class _FakeSimulator:
    """Scripted customer. ``next_turns`` is a list of dicts returned by
    ``generate_next`` (message / goal_status / user_state)."""

    def __init__(self, next_turns):
        self._next_turns = list(next_turns)
        self._i = 0
        self.generate_next_calls = 0

    def generate_first_message(self, case_spec):
        return {"message": "my ad isn't showing", "goal_status": "in_progress"}

    def generate_next(self, case_spec, transcript, bot_reply):
        self.generate_next_calls += 1
        out = self._next_turns[self._i]
        self._i += 1
        return out


def _run(bot_turns, next_turns, max_turns=5):
    runner = SessionRunner(_FakeAgent(bot_turns), _FakeSimulator(next_turns), None)
    return runner.run_session(_case_spec(max_turns))


def test_post_help_unresolved_signal_then_escalation_no_terminal_signal():
    # Turn 1: bot gives (grounded) help, chat continues.
    # generate_next -> customer is positively UNRESOLVED after that help.
    # Turn 2: bot ends the chat (escalation/handover) -> break BEFORE any
    #         further generate_next, so NO signal is stamped for the
    #         escalation turn.
    result = _run(
        bot_turns=[
            ("Here is the article on ad visibility ...", False),
            ("I'll connect you with a human agent.", True),
        ],
        next_turns=[
            {"message": "that didn't help, can I speak to someone?",
             "goal_status": "in_progress", "user_state": "unresolved_after_help"},
        ],
    )
    assert result.stop_reason == "bot_ended"
    # Exactly one signal — the post-help UNRESOLVED one. No fabricated
    # post-terminal signal.
    assert len(result.user_state_signals) == 1
    sig = result.user_state_signals[0]
    assert sig["user_state"] == "unresolved_after_help"
    assert sig["turn_id"] == 1          # reacting to the bot's turn-1 help
    assert sig["produced_user_turn"] == 2
    assert sig["signal_source"] == USER_STATE_SIGNAL_SOURCE
    assert sig["schema_version"] == USER_STATE_SCHEMA_VERSION


def test_early_escalation_turn1_yields_no_signal():
    # Bot ends on turn 1 (early/lazy escalation): generate_next never runs,
    # so there is no user_state signal at all (UNKNOWN downstream).
    result = _run(
        bot_turns=[("I'll hand you to a human now.", True)],
        next_turns=[],
    )
    assert result.stop_reason == "bot_ended"
    assert result.user_state_signals == []


def test_satisfied_terminal_records_signal():
    # Bot helps; customer is satisfied and goal achieved on the follow-up.
    result = _run(
        bot_turns=[("Your listing AD-2002 is LIVE; try refreshing it.", False)],
        next_turns=[
            {"message": "thanks, that helps!", "goal_status": "achieved",
             "user_state": "satisfied"},
        ],
    )
    assert result.stop_reason == "goal_achieved"
    assert len(result.user_state_signals) == 1
    assert result.user_state_signals[0]["user_state"] == "satisfied"
    assert result.user_state_signals[0]["goal_status"] == "achieved"


def test_each_turn_independently_stamped_no_carry_forward():
    # Two generate_next calls -> two independently stamped signals with
    # distinct turn_ids. A "working" turn-1 state is NOT carried forward
    # onto the turn-2 "unresolved_after_help" state.
    result = _run(
        bot_turns=[
            ("Let me look that up ...", False),
            ("Here is the visibility article ...", False),
            ("I'll connect you to a human.", True),
        ],
        next_turns=[
            {"message": "ok", "goal_status": "in_progress", "user_state": "working"},
            {"message": "still not working", "goal_status": "in_progress",
             "user_state": "unresolved_after_help"},
        ],
    )
    states = [(s["turn_id"], s["user_state"]) for s in result.user_state_signals]
    assert states == [(1, "working"), (2, "unresolved_after_help")]
