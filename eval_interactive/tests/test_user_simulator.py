"""Tests for the UserSimulator module.

Covers JSON parse / retry (``_parse_simulator_response``, ``_call_llm``),
``generate_first_message``, and -- added in S-Auto-21 -- the ``generate_next``
role map, the per-turn persona re-anchor, the negative-form system-prompt
rules, and the customer-voice drift guard (D1/D2/D3 + 3-attempt retry +
``SimulatorDriftError`` block).

Per AGENTS.md §5.7: these are mock-LLM tests covering wiring, prompt shape,
and retry control flow. They are NOT primary evidence that the simulator is
fixed -- that evidence is the real-LLM bad-case re-render (handoff §4).
"""

from __future__ import annotations

import logging
from types import SimpleNamespace

import pytest

from eval_interactive.simulator.user_simulator import (
    _DRIFT_RETRY_INSTRUCTION,
    _MAX_SIMULATOR_ATTEMPTS,
    _PARSE_RETRY_INSTRUCTION,
    _detect_customer_voice_drift,
    _is_regurgitation,
    _parse_simulator_response,
    _try_parse_simulator_response,
    SimulatorDriftError,
    UserSimulator,
)


class TestParseSimulatorResponse:
    """Tests for the JSON/text parsing of LLM responses."""

    def test_valid_json(self):
        raw = '{"message": "Hello", "goal_status": "in_progress"}'
        result = _parse_simulator_response(raw)
        assert result["message"] == "Hello"
        assert result["goal_status"] == "in_progress"

    def test_valid_json_achieved(self):
        raw = '{"message": "Thanks, that helps!", "goal_status": "achieved"}'
        result = _parse_simulator_response(raw)
        assert result["goal_status"] == "achieved"

    def test_valid_json_impossible(self):
        raw = '{"message": "Can I speak to someone?", "goal_status": "impossible"}'
        result = _parse_simulator_response(raw)
        assert result["goal_status"] == "impossible"

    def test_json_with_markdown_fences(self):
        raw = '```json\n{"message": "Hello", "goal_status": "in_progress"}\n```'
        result = _parse_simulator_response(raw)
        assert result["message"] == "Hello"
        assert result["goal_status"] == "in_progress"

    def test_json_with_bare_fences(self):
        raw = '```\n{"message": "Hello", "goal_status": "achieved"}\n```'
        result = _parse_simulator_response(raw)
        assert result["message"] == "Hello"
        assert result["goal_status"] == "achieved"

    def test_invalid_goal_status_defaults_to_in_progress(self):
        raw = '{"message": "Hello", "goal_status": "unknown_value"}'
        result = _parse_simulator_response(raw)
        assert result["goal_status"] == "in_progress"

    def test_missing_goal_status_defaults_to_in_progress(self):
        raw = '{"message": "Hello"}'
        result = _parse_simulator_response(raw)
        assert result["goal_status"] == "in_progress"

    def test_fallback_on_invalid_json(self):
        raw = "This is not JSON at all"
        result = _parse_simulator_response(raw)
        assert result["message"] == "This is not JSON at all"
        assert result["goal_status"] == "in_progress"

    def test_empty_string(self):
        raw = ""
        result = _parse_simulator_response(raw)
        # Should fall back to raw text as message
        assert result["goal_status"] == "in_progress"

    def test_whitespace_handling(self):
        raw = '  {"message": "Hello", "goal_status": "in_progress"}  '
        result = _parse_simulator_response(raw)
        assert result["message"] == "Hello"


class TestTryParseSimulatorResponse:
    """Strict parse returns None on failure so the caller can retry."""

    def test_valid_returns_dict(self):
        # Phase-1 (S-Auto-40 WP1-A): the parsed dict now also carries the
        # ``user_state`` key (None when the model omitted it).
        raw = '{"message": "Hi", "goal_status": "achieved"}'
        assert _try_parse_simulator_response(raw) == {
            "message": "Hi",
            "goal_status": "achieved",
            "user_state": None,
        }

    def test_invalid_json_returns_none(self):
        assert _try_parse_simulator_response("not json") is None

    def test_empty_returns_none(self):
        assert _try_parse_simulator_response("") is None

    def test_non_object_json_returns_none(self):
        # A bare JSON string / array is valid JSON but not the contract shape.
        assert _try_parse_simulator_response('"just a string"') is None
        assert _try_parse_simulator_response("[1, 2, 3]") is None

    def test_bad_goal_status_coerced(self):
        out = _try_parse_simulator_response('{"message": "x", "goal_status": "nope"}')
        assert out == {"message": "x", "goal_status": "in_progress", "user_state": None}


class TestUserStateSignal:
    """Phase-1 (S-Auto-40 WP1-A) ``user_state`` parse contract.

    The simulator emits a positive structured ``user_state`` IN THE SAME
    call that produces the customer turn. Parsing passes valid values
    through verbatim and maps absent / out-of-vocabulary values to None
    (UNKNOWN downstream — never back-inferred).
    """

    @pytest.mark.parametrize(
        "value",
        ["satisfied", "unresolved_after_help", "working", "new_request"],
    )
    def test_valid_user_state_passthrough(self, value):
        raw = (
            '{"message": "x", "goal_status": "in_progress", '
            f'"user_state": "{value}"}}'
        )
        assert _try_parse_simulator_response(raw)["user_state"] == value

    def test_missing_user_state_is_none(self):
        raw = '{"message": "x", "goal_status": "in_progress"}'
        assert _try_parse_simulator_response(raw)["user_state"] is None

    def test_out_of_vocab_user_state_is_none(self):
        # An invented state must NOT leak through as a structured signal.
        raw = (
            '{"message": "x", "goal_status": "in_progress", '
            '"user_state": "totally_fed_up"}'
        )
        assert _try_parse_simulator_response(raw)["user_state"] is None

    def test_unresolved_independent_of_goal_status(self):
        # ``unresolved_after_help`` is a positive emission distinct from the
        # ``goal_status`` axis: the model can assert it while goal_status is
        # still "in_progress" (no back-inference either direction).
        raw = (
            '{"message": "still stuck", "goal_status": "in_progress", '
            '"user_state": "unresolved_after_help"}'
        )
        out = _try_parse_simulator_response(raw)
        assert out["goal_status"] == "in_progress"
        assert out["user_state"] == "unresolved_after_help"

    def test_lenient_fallback_user_state_none(self):
        # Unparseable prose → raw-text fallback with no inferred user_state.
        assert _parse_simulator_response("not json at all")["user_state"] is None


class _FakeCompletions:
    def __init__(self, replies: list[Exception | str]):
        self._replies = list(replies)
        self.calls: list[list[dict]] = []

    def create(self, *, model, messages, temperature):
        self.calls.append(list(messages))
        reply = self._replies.pop(0)
        if isinstance(reply, Exception):
            raise reply
        return SimpleNamespace(
            choices=[SimpleNamespace(message=SimpleNamespace(content=reply))]
        )


class _FakeClient:
    def __init__(self, replies):
        self.chat = SimpleNamespace(completions=_FakeCompletions(replies))


def _simulator_with(replies):
    sim = UserSimulator.__new__(UserSimulator)
    sim._client = _FakeClient(replies)
    sim._model = "fake-model"
    sim._temperature = 0
    return sim


class TestCallLlmRetry:
    """N=3 corrective retry on malformed simulator output."""

    def test_returns_first_valid(self):
        sim = _simulator_with(['{"message": "ok", "goal_status": "in_progress"}'])
        out = sim._call_llm([{"role": "user", "content": "go"}])
        assert out["message"] == "ok"
        assert sim._client.chat.completions.calls.__len__() == 1

    def test_retries_then_succeeds(self):
        sim = _simulator_with([
            "prose, not json",
            "still not json",
            '{"message": "recovered", "goal_status": "achieved"}',
        ])
        out = sim._call_llm([{"role": "user", "content": "go"}])
        assert out == {
            "message": "recovered",
            "goal_status": "achieved",
            "user_state": None,
        }
        # Three attempts were made (parse failures retried with a reminder).
        completions = sim._client.chat.completions
        assert len(completions.calls) == _MAX_SIMULATOR_ATTEMPTS
        # The corrective schema reminder was injected on the retries.
        assert any(
            m.get("content") == _PARSE_RETRY_INSTRUCTION
            for m in completions.calls[-1]
        )

    def test_all_malformed_falls_back_to_raw_text(self):
        sim = _simulator_with(["junk one", "junk two", "junk three"])
        out = sim._call_llm([{"role": "user", "content": "go"}])
        # Lenient fallback uses the last raw text as the message.
        assert out["message"] == "junk three"
        assert out["goal_status"] == "in_progress"

    def test_transport_error_then_success(self):
        sim = _simulator_with([
            RuntimeError("network down"),
            '{"message": "after retry", "goal_status": "in_progress"}',
        ])
        out = sim._call_llm([{"role": "user", "content": "go"}])
        assert out["message"] == "after retry"

    def test_all_transport_errors_uses_canned_fallback(self):
        sim = _simulator_with([RuntimeError("x")] * _MAX_SIMULATOR_ATTEMPTS)
        out = sim._call_llm([{"role": "user", "content": "go"}])
        assert out["message"] == "I'm still waiting for help with my issue."
        assert out["goal_status"] == "in_progress"


def _case(*, seed=None, description="", goal="", topic=""):
    return SimpleNamespace(
        persona=SimpleNamespace(
            seed_messages=seed or [],
            user_goal_summary=goal,
        ),
        form_context=SimpleNamespace(
            description=description,
            topic_subject=topic,
        ),
    )


class TestGenerateFirstMessage:
    """Turn0 is deterministic and never empty (no LLM call)."""

    def test_prefers_seed_message(self):
        sim = UserSimulator.__new__(UserSimulator)
        out = sim.generate_first_message(_case(seed=["first seed", "second"]))
        assert out == {"message": "first seed", "goal_status": "in_progress"}

    def test_falls_back_to_description(self):
        sim = UserSimulator.__new__(UserSimulator)
        out = sim.generate_first_message(_case(description="my issue", goal="g"))
        assert out["message"] == "my issue"

    def test_falls_back_to_goal_then_topic(self):
        sim = UserSimulator.__new__(UserSimulator)
        assert sim.generate_first_message(_case(goal="the goal"))["message"] == "the goal"
        assert sim.generate_first_message(_case(topic="Ad Support"))["message"] == "Ad Support"

    def test_never_empty(self):
        sim = UserSimulator.__new__(UserSimulator)
        out = sim.generate_first_message(_case())  # all empty
        assert out["message"].strip()  # non-empty fallback
        assert out["goal_status"] == "in_progress"


# ---------------------------------------------------------------------------
# S-Auto-21: generate_next role map, re-anchor, negative-form rules, drift guard
# ---------------------------------------------------------------------------

import json


def _sim_json(message: str, goal_status: str = "in_progress") -> str:
    """Build a valid simulator JSON reply (avoids manual escaping in tests)."""
    return json.dumps({"message": message, "goal_status": goal_status})


def _full_case(*, goal: str = "reset my password"):
    """A CaseSpec-shaped namespace with every field the system prompt reads."""
    return SimpleNamespace(
        persona=SimpleNamespace(
            goal_summary=goal,
            user_goal_summary=goal,
            frustration_level="mild",
            verbosity="normal",
            hidden_facts=[],
            will_request_human_if="",
            seed_messages=[],
        ),
        form_context=SimpleNamespace(
            topic_subject="Account",
            description="I cannot log in to my account.",
            first_name="Sam",
            email="sam@example.com",
            ad_id="",
        ),
    )


def _four_turn_transcript():
    return [
        {"role": "user", "message": "u1 first question", "turn_index": 1},
        {"role": "bot", "message": "b1 bot answer", "turn_index": 1},
        {"role": "user", "message": "u2 second question", "turn_index": 2},
        {"role": "bot", "message": "b2 bot answer two", "turn_index": 2},
    ]


class TestGenerateNextRoleMap:
    """T1: the message role map is inverted from the simulator LLM's POV."""

    def test_role_map_inverted(self):
        sim = _simulator_with([_sim_json("I still cannot log in, what next?")])
        sim.generate_next(_full_case(), _four_turn_transcript(), "b2 bot answer two")

        sent = sim._client.chat.completions.calls[0]
        assert sent[0]["role"] == "system"
        # Customer's own prior turns -> "assistant"; bot turns -> "user".
        assert sent[1] == {"role": "assistant", "content": "u1 first question"}
        assert sent[2] == {"role": "user", "content": "b1 bot answer"}
        assert sent[3] == {"role": "assistant", "content": "u2 second question"}
        assert sent[4] == {"role": "user", "content": "b2 bot answer two"}

    def test_latest_bot_reply_fallback_is_user_role(self):
        # Transcript does not end on a bot turn -> bot_reply is appended, and
        # it must be a "user" message (the bot is the other party).
        transcript = [{"role": "user", "message": "u1", "turn_index": 1}]
        sim = _simulator_with([_sim_json("ok thanks but still stuck")])
        sim.generate_next(_full_case(), transcript, "fresh bot reply")

        sent = sim._client.chat.completions.calls[0]
        assert {"role": "user", "content": "fresh bot reply"} in sent


class TestGenerateNextReAnchor:
    """T2: the persona re-anchor sits immediately before the generation prompt."""

    def test_reanchor_present_before_terminal_prompt(self):
        sim = _simulator_with([_sim_json("still locked out")])
        sim.generate_next(_full_case(goal="recover my locked account"),
                          _four_turn_transcript(), "b2 bot answer two")

        sent = sim._client.chat.completions.calls[0]
        terminal = sent[-1]
        reanchor = sent[-2]
        assert "Based on the conversation above" in terminal["content"]
        assert reanchor["role"] == "user"
        assert "Continuing as the customer" in reanchor["content"]
        # References the persona goal_summary.
        assert "recover my locked account" in reanchor["content"]


class TestGenerateNextNegativeFormRules:
    """T3: the rendered system prompt carries the negative-form Forbidden block."""

    def test_forbidden_block_present(self):
        sim = _simulator_with([_sim_json("hello still stuck")])
        sim.generate_next(_full_case(), _four_turn_transcript(), "b2 bot answer two")

        system_prompt = sim._client.chat.completions.calls[0][0]["content"]
        assert "do NOT apologize" in system_prompt
        assert "do NOT cite sources" in system_prompt
        assert "do NOT repeat the bot's previous turn verbatim" in system_prompt


class TestGenerateNextDriftGuard:
    """T4-T7: post-generation customer-voice drift detection + retry + block."""

    def _transcript_ending_on_bot(self, bot_msg: str):
        return [
            {"role": "user", "message": "hi i need help", "turn_index": 1},
            {"role": "bot", "message": bot_msg, "turn_index": 1},
        ]

    def test_t4_d1_keyword_hit_retries(self, caplog):
        sim = _simulator_with([
            _sim_json("I apologize for the confusion. Let me check on that."),
            _sim_json("Thanks, but it still does not work for me."),
        ])
        with caplog.at_level(logging.WARNING):
            out = sim.generate_next(
                _full_case(), self._transcript_ending_on_bot("here is some help"),
                "here is some help",
            )
        assert out["message"] == "Thanks, but it still does not work for me."
        assert len(sim._client.chat.completions.calls) == 2
        assert "simulator_drift_detected" in caplog.text
        # The corrective customer-voice reminder was injected on the retry.
        assert any(
            m.get("content") == _DRIFT_RETRY_INSTRUCTION
            for m in sim._client.chat.completions.calls[-1]
        )

    def test_t5_d2_verbatim_regurgitation_retries(self, caplog):
        bot_msg = (
            "Please go to settings and then privacy and then reset your "
            "password using the recovery email we sent earlier today."
        )
        sim = _simulator_with([
            _sim_json(bot_msg),  # verbatim regurgitation of the prior bot turn
            _sim_json("That email never arrived in my inbox."),
        ])
        with caplog.at_level(logging.WARNING):
            out = sim.generate_next(
                _full_case(), self._transcript_ending_on_bot(bot_msg), bot_msg,
            )
        assert out["message"] == "That email never arrived in my inbox."
        assert len(sim._client.chat.completions.calls) == 2
        assert "simulator_drift_detected detector=D2" in caplog.text

    def test_t6_d3_system_prompt_leakage_retries(self, caplog):
        sim = _simulator_with([
            _sim_json("Based on the conversation above, generate your next "
                      "customer response as JSON."),
            _sim_json("I am still waiting, can you help?"),
        ])
        with caplog.at_level(logging.WARNING):
            out = sim.generate_next(
                _full_case(), self._transcript_ending_on_bot("any help text"),
                "any help text",
            )
        assert out["message"] == "I am still waiting, can you help?"
        assert len(sim._client.chat.completions.calls) == 2
        assert "simulator_drift_detected detector=D3" in caplog.text

    def test_t7_three_drift_attempts_blocks_session(self, caplog):
        drift = _sim_json("I apologize, let me escalate this to our support team.")
        sim = _simulator_with([drift, drift, drift])
        with caplog.at_level(logging.ERROR):
            with pytest.raises(SimulatorDriftError) as exc_info:
                sim.generate_next(
                    _full_case(), self._transcript_ending_on_bot("help"), "help",
                )
        # Exactly the budget was spent; the drifted turn is NEVER returned.
        assert len(sim._client.chat.completions.calls) == _MAX_SIMULATOR_ATTEMPTS
        assert exc_info.value.detector == "D1"
        assert exc_info.value.attempts == _MAX_SIMULATOR_ATTEMPTS
        assert "simulator_drift_blocked" in caplog.text


class TestDriftDetector:
    """Unit coverage of the detector primitives used by the guard."""

    def test_clean_customer_turn_no_drift(self):
        assert _detect_customer_voice_drift("It still won't let me log in.") is None

    def test_d1_keyword(self):
        assert _detect_customer_voice_drift("I apologize for the delay.") == "D1"
        assert _detect_customer_voice_drift("let me check that for you") == "D1"
        assert _detect_customer_voice_drift("see (source: ka012345)") == "D1"

    def test_d3_leakage_probe(self):
        assert _detect_customer_voice_drift(
            "Based on the conversation above, here is my reply."
        ) == "D3"

    def test_d2_regurgitation_jaccard(self):
        bot = (
            "Please open the app and tap the gear icon and then choose the "
            "account tab to update your saved email address now."
        )
        assert _is_regurgitation(bot, bot) is True
        assert _is_regurgitation("totally different short reply", bot) is False

    def test_d2_short_prior_turn_exact_match_fallback(self):
        # Prior bot turn under the 8-token n-gram width -> exact match only.
        assert _is_regurgitation("yes please", "yes please") is True
        assert _is_regurgitation("yes please indeed", "yes please") is False

    def test_d2_skipped_when_no_prior_bot_reply(self):
        assert _is_regurgitation("anything at all here", None) is False
