"""Tests for the UserSimulator module -- focuses on _parse_simulator_response."""

from __future__ import annotations

from types import SimpleNamespace

import pytest

from eval_interactive.simulator.user_simulator import (
    _MAX_SIMULATOR_ATTEMPTS,
    _PARSE_RETRY_INSTRUCTION,
    _parse_simulator_response,
    _try_parse_simulator_response,
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
        raw = '{"message": "Hi", "goal_status": "achieved"}'
        assert _try_parse_simulator_response(raw) == {
            "message": "Hi",
            "goal_status": "achieved",
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
        assert out == {"message": "x", "goal_status": "in_progress"}


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
        assert out == {"message": "recovered", "goal_status": "achieved"}
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
