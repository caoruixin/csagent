"""Sprint 25 — per-LLM-call latency enrichment on ``results.json``.

Regression tests for the writer-side enrichment that attaches the bot's
``llm_call_log`` rows to each case result. Three properties:

1. ``BatchExecutor._fetch_llm_calls`` is best-effort: a backend failure
   produces an empty list, not an exception (the case still gets
   scored; the field is just empty).
2. ``BatchExecutor._build_case_result`` round-trips the fetched list
   verbatim into a top-level ``llm_calls`` field on the case result.
3. The placeholder result builders (``_timeout_result``,
   ``_error_result``, ``_contract_violation_result``) all populate
   ``llm_calls`` with an empty list so the schema is uniform across
   every status (a downstream consumer can always ``case.get(
   "llm_calls", [])`` and never see a ``KeyError``).
"""

from __future__ import annotations

from types import SimpleNamespace
from typing import Any

import pytest

from eval_interactive.batch.executor import BatchExecutor
from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.composite import CompositeScore
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.simulator.session_runner import SessionResult
from eval_interactive.trace.collector import TraceContractError


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


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


class _StubExecutor(BatchExecutor):
    """BatchExecutor that skips Config init.

    Only the per-case helpers under test are exercised; nothing reads
    ``self._config``. Mirrors the pattern in
    ``test_agent_client_session_create_timeout.py``.
    """

    def __init__(self) -> None:  # noqa: D401
        # intentionally skipping super().__init__
        # only the per-case helpers are exercised by these tests
        self._config = SimpleNamespace(
            batch=SimpleNamespace(timeout_per_session_seconds=60),
        )


def _make_session_result(session_id: str = "sess-xyz") -> SessionResult:
    return SessionResult(
        session_id=session_id,
        case_id="cs_ut_llm_calls",
        transcript=[],
        total_turns=1,
        stop_reason="resolved",
        elapsed_ms=1234,
    )


def _make_trace_data() -> Any:
    return SimpleNamespace(
        session_state=SimpleNamespace(
            session_id="sess-xyz",
            active_use_case="UC-B",
            candidate_use_cases=[],
            containment_outcome="resolved",
            escalation_reason="",
            total_bot_turns=1,
            clarification_count=0,
            faq_miss_count=0,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="CLOSE",
        ),
        turns=[],
        events=[],
        handover=None,
        contract_warnings=[],
    )


def _make_composite_score() -> CompositeScore:
    return CompositeScore(
        case_id="cs_ut_llm_calls",
        case_passed=True,
        composite=0.9,
        outcome_score=0.9,
        judge_score=0.9,
        failure_tags=[],
        l1_results=[],
        l2_results=[],
        l3_results=[],
        stall_result=StallResult(detected=False, failure_tag=""),
    )


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


def test_fetch_llm_calls_returns_rows_on_success() -> None:
    """Happy path: the endpoint's payload is passed through verbatim."""
    rows = [
        {"call_type": "chat", "turn_index": 0, "latency_ms": 4321, "success": True},
        {"call_type": "rerank", "turn_index": 0, "latency_ms": 880, "success": True},
    ]

    class _Agent:
        def get_llm_calls(self, session_id: str) -> list[dict]:
            assert session_id == "sess-xyz"
            return rows

    out = BatchExecutor._fetch_llm_calls(_Agent(), "sess-xyz", "cs_ut")
    assert out == rows


def test_fetch_llm_calls_swallows_endpoint_errors() -> None:
    """A backend error becomes an empty list, never an exception."""

    class _Agent:
        def get_llm_calls(self, session_id: str) -> list[dict]:
            raise RuntimeError("bot 500")

    out = BatchExecutor._fetch_llm_calls(_Agent(), "sess-xyz", "cs_ut")
    assert out == []


def test_fetch_llm_calls_skips_empty_session_id() -> None:
    """An error-path session with no real session_id must not hit the bot.

    ``_error_result`` / ``_contract_violation_result`` may produce
    placeholder rows with ``session_id=""``; calling the bot for those
    is wasted work (and would log a confusing warning).
    """
    called = {"n": 0}

    class _Agent:
        def get_llm_calls(self, session_id: str) -> list[dict]:
            called["n"] += 1
            return [{"x": 1}]

    out = BatchExecutor._fetch_llm_calls(_Agent(), "", "cs_ut")
    assert out == []
    assert called["n"] == 0


def test_build_case_result_includes_llm_calls_field() -> None:
    """Successful-case result includes the ``llm_calls`` field verbatim."""
    rows = [
        {"call_type": "chat", "turn_index": 0, "latency_ms": 5000, "success": True},
    ]
    executor = _StubExecutor()
    result = executor._build_case_result(
        _build_case("cs_ut_llm_calls"),
        _make_session_result(),
        _make_trace_data(),
        _make_composite_score(),
        rows,
    )
    assert result["llm_calls"] == rows


def test_build_case_result_defaults_llm_calls_to_empty_list_when_omitted() -> None:
    """Backwards-compat: callers that don't pass ``llm_calls`` get []."""
    executor = _StubExecutor()
    result = executor._build_case_result(
        _build_case("cs_ut_llm_calls"),
        _make_session_result(),
        _make_trace_data(),
        _make_composite_score(),
    )
    assert result["llm_calls"] == []


def test_timeout_result_includes_empty_llm_calls() -> None:
    executor = _StubExecutor()
    out = executor._timeout_result(_build_case("cs_ut"))
    assert out["status"] == "TIMEOUT"
    assert out["llm_calls"] == []


def test_error_result_includes_empty_llm_calls() -> None:
    executor = _StubExecutor()
    out = executor._error_result(_build_case("cs_ut"), "boom")
    assert out["status"] == "ERROR"
    assert out["llm_calls"] == []


def test_contract_violation_result_includes_empty_llm_calls() -> None:
    executor = _StubExecutor()
    exc = TraceContractError(
        field="containment_outcome",
        phase="session",
        session_id="sess-xyz",
        available_keys=["active_use_case"],
        reason="missing",
    )
    out = executor._contract_violation_result(_build_case("cs_ut"), exc)
    assert out["status"] == "CONTRACT_VIOLATION"
    assert out["llm_calls"] == []
