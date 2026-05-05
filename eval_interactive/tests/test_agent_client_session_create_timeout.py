"""Tests for the Sprint 6 §G0 session-create timeout / ReadTimeout retry.

These tests exercise the eval-client AgentClient.create_session path:

- The wider 120s read timeout is configured on session-create only.
- On httpx.ReadTimeout, create_session retries exactly once and then
  re-raises if the retry also times out.
- A 4xx / 5xx HTTP response is NOT retried (semantic failures stay
  non-retryable).
- session_runner records a ReadTimeout-tagged ``creation_error`` when
  the retry exhausts, and a generic repr otherwise.
"""

from __future__ import annotations

from unittest.mock import patch

import httpx
import pytest

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.simulator.agent_client import (
    DEFAULT_READ_TIMEOUT_SECONDS,
    SESSION_CREATE_READ_TIMEOUT_SECONDS,
    AgentClient,
)


def _make_client() -> AgentClient:
    return AgentClient("http://localhost:8080")


def _build_case(case_id: str) -> CaseSpec:
    return CaseSpec(
        case_id=case_id,
        source_session_id=case_id,
        source_dataset="ut",
        form_context=FormContext(
            first_name="U",
            email="u@example.com",
            topic_subject="Ad Support",
            ad_id="",
            description="test",
        ),
        persona=Persona(
            user_goal_summary="ut",
            frustration_level="none",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["hi"],
            hidden_facts=[],
            will_request_human_if="",
        ),
        expected=Expected(
            outcome_class="resolve",
            primary_uc="UC-B",
            secondary_ucs=[],
            should_escalate=False,
            allow_bot_resolution="true",
            bot_handling_pattern="ut",
            escalation_trigger=None,
            risk_level="low",
            expected_tool_sequence=[],
            forbidden_tools=[],
            grounding_mode="faq_source_backed",
            answer_must_not_contain=[],
            max_turns=1,
        ),
        scoring=ScoringConfig(
            hard_checks=[],
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
    )


def test_session_create_timeout_constants_widened_to_120s() -> None:
    assert SESSION_CREATE_READ_TIMEOUT_SECONDS == 120.0
    # Default (used for send_message / get_trace etc.) stays at 60s.
    assert DEFAULT_READ_TIMEOUT_SECONDS == 60.0


def test_create_session_retries_once_on_read_timeout_then_succeeds() -> None:
    client = _make_client()
    calls = {"n": 0}

    def fake_post(url, json=None, timeout=None):  # noqa: ARG001
        calls["n"] += 1
        if calls["n"] == 1:
            raise httpx.ReadTimeout("timed out")

        class _Resp:
            status_code = 200

            def raise_for_status(self) -> None:
                return None

            def json(self) -> dict:
                return {"session_id": "sess-abc", "reply_text": "hi"}

        return _Resp()

    with patch.object(client.client, "post", side_effect=fake_post), \
            patch("eval_interactive.simulator.agent_client.time.sleep"):
        out = client.create_session({"first_name": "x"})

    assert out == {"session_id": "sess-abc", "reply_text": "hi"}
    assert calls["n"] == 2


def test_create_session_raises_after_two_consecutive_read_timeouts() -> None:
    client = _make_client()
    calls = {"n": 0}

    def fake_post(url, json=None, timeout=None):  # noqa: ARG001
        calls["n"] += 1
        raise httpx.ReadTimeout("timed out")

    with patch.object(client.client, "post", side_effect=fake_post), \
            patch("eval_interactive.simulator.agent_client.time.sleep"):
        with pytest.raises(httpx.ReadTimeout):
            client.create_session({"first_name": "x"})

    # Bounded retry: 1 initial + 1 retry == 2 attempts max.
    assert calls["n"] == 2


def test_create_session_does_not_retry_on_4xx_or_5xx() -> None:
    """Auth / scope / 5xx failures stay non-retryable on session-create."""
    client = _make_client()
    calls = {"n": 0}

    class _Resp:
        def __init__(self, status: int) -> None:
            self.status_code = status
            self.request = httpx.Request("POST", "http://localhost/x")

        def raise_for_status(self) -> None:
            raise httpx.HTTPStatusError(
                f"{self.status_code}",
                request=self.request,
                response=httpx.Response(self.status_code, request=self.request),
            )

        def json(self) -> dict:  # pragma: no cover - never called
            return {}

    def fake_post_401(url, json=None, timeout=None):  # noqa: ARG001
        calls["n"] += 1
        return _Resp(401)

    with patch.object(client.client, "post", side_effect=fake_post_401):
        with pytest.raises(httpx.HTTPStatusError):
            client.create_session({})
    assert calls["n"] == 1

    calls["n"] = 0

    def fake_post_503(url, json=None, timeout=None):  # noqa: ARG001
        calls["n"] += 1
        return _Resp(503)

    with patch.object(client.client, "post", side_effect=fake_post_503):
        with pytest.raises(httpx.HTTPStatusError):
            client.create_session({})
    assert calls["n"] == 1


def test_create_session_passes_widened_per_request_timeout() -> None:
    """create_session should apply the 120s read timeout per request."""
    client = _make_client()
    captured = {}

    def fake_post(url, json=None, timeout=None):  # noqa: ARG001
        captured["timeout"] = timeout

        class _Resp:
            status_code = 200

            def raise_for_status(self) -> None:
                return None

            def json(self) -> dict:
                return {"session_id": "s", "reply_text": ""}

        return _Resp()

    with patch.object(client.client, "post", side_effect=fake_post):
        client.create_session({})

    t = captured["timeout"]
    assert isinstance(t, httpx.Timeout)
    # httpx.Timeout exposes .read for the read budget.
    assert t.read == SESSION_CREATE_READ_TIMEOUT_SECONDS


def test_session_runner_tags_read_timeout_creation_error() -> None:
    """Verify SessionRunner produces a ReadTimeout-tagged creation_error."""
    from types import SimpleNamespace

    from eval_interactive.simulator.session_runner import SessionRunner

    case = _build_case("cs_ut_readtimeout")

    class _Agent:
        def create_session(self, _form):  # noqa: D401
            raise httpx.ReadTimeout("timed out")

        def close(self) -> None:
            pass

    sim = SimpleNamespace(generate_first_message=lambda c: {"message": "x"})
    stall = SimpleNamespace()

    runner = SessionRunner(_Agent(), sim, stall)
    result = runner.run_session(case)

    assert result.stop_reason == "session_create_failed"
    assert result.creation_error is not None
    assert "ReadTimeout" in result.creation_error
    assert "120s" in result.creation_error


def test_session_runner_does_not_tag_read_timeout_for_other_errors() -> None:
    """Non-ReadTimeout failures keep the legacy repr() classification."""
    from types import SimpleNamespace

    from eval_interactive.simulator.session_runner import SessionRunner

    case = _build_case("cs_ut_other")

    class _Agent:
        def create_session(self, _form):  # noqa: D401
            raise httpx.ConnectError("connection refused")

        def close(self) -> None:
            pass

    sim = SimpleNamespace(generate_first_message=lambda c: {"message": "x"})
    stall = SimpleNamespace()

    runner = SessionRunner(_Agent(), sim, stall)
    result = runner.run_session(case)

    assert result.stop_reason == "session_create_failed"
    assert "ReadTimeout" not in (result.creation_error or "")
    assert "ConnectError" in result.creation_error
