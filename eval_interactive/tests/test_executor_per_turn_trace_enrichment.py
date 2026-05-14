"""Sprint 28 — per-case per-turn trace dump enrichment on ``results.json``.

Regression tests for the writer-side enrichment that attaches a
``per_turn_trace`` list to every case result, one entry per bot turn,
carrying the three R-item-named per-turn fields:

- ``tool_calls`` (verbatim from ``TurnTrace.tool_calls``)
- ``phase_plan`` (the ``phase_plan`` sub-object inside ``projected_context``)
- ``projection`` (the full per-turn ``projected_context`` dict)

The fourth R-item-named field, ``LlmCallEvents``, is already carried by
the Sprint 25 ``case_results[].llm_calls[]`` enrichment and is **not**
re-shipped per Sprint 28's scope discipline (only the named R-item
fields, no opportunistic additions).

The properties under test:

1. ``BatchExecutor._build_per_turn_trace`` is pure: given a populated
   ``trace_data`` it returns one entry per turn with all three fields.
2. Defensive defaults: an empty ``trace_data.turns`` returns ``[]``; a
   ``projected_context`` missing ``phase_plan`` yields ``phase_plan ==
   None``; a non-dict ``projected_context`` or non-list ``tool_calls``
   falls back to ``{}`` / ``[]`` respectively rather than raising.
3. ``_build_case_result`` round-trips the new field into the case
   result dict without disturbing existing fields (``llm_calls``,
   ``transcript`` byte-identical to pre-Sprint-28).
4. ``_timeout_result`` / ``_error_result`` / ``_contract_violation_result``
   each populate ``per_turn_trace`` with an empty list so a downstream
   consumer can always ``case.get("per_turn_trace", [])`` and never see
   a ``KeyError`` regardless of case status.
5. The full populated result dict is JSON-serialisable end-to-end (no
   ``datetime`` or other non-JSON types leak through ``projected_context``).
"""

from __future__ import annotations

import json
from types import SimpleNamespace
from typing import Any

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
from eval_interactive.trace.models import TurnTrace


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
    ``test_executor_llm_calls_enrichment.py``.
    """

    def __init__(self) -> None:
        self._config = SimpleNamespace(
            batch=SimpleNamespace(timeout_per_session_seconds=60),
        )


def _make_session_result(session_id: str = "sess-xyz") -> SessionResult:
    return SessionResult(
        session_id=session_id,
        case_id="cs_ut_per_turn_trace",
        transcript=[{"role": "user", "content": "hi"}],
        total_turns=1,
        stop_reason="resolved",
        elapsed_ms=1234,
    )


def _make_turn(
    *,
    turn_index: int = 0,
    tool_calls: list[dict] | None = None,
    projected_context: dict | None = None,
    phase_before: str = "DISCOVER",
    phase_after: str = "RESOLVE",
) -> TurnTrace:
    return TurnTrace(
        turn_index=turn_index,
        user_message=f"u{turn_index}",
        bot_response=f"b{turn_index}",
        tool_calls=tool_calls if tool_calls is not None else [],
        source_ids=[],
        phase_before=phase_before,
        phase_after=phase_after,
        active_use_case="UC-B",
        latency_ms=1500,
        projected_context=projected_context if projected_context is not None else {},
    )


def _make_trace_data(turns: list[TurnTrace] | None = None) -> Any:
    return SimpleNamespace(
        session_state=SimpleNamespace(
            session_id="sess-xyz",
            active_use_case="UC-B",
            candidate_use_cases=[],
            containment_outcome="resolved",
            escalation_reason="",
            total_bot_turns=len(turns) if turns is not None else 0,
            clarification_count=0,
            faq_miss_count=0,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="CLOSE",
        ),
        turns=turns if turns is not None else [],
        events=[],
        handover=None,
        contract_warnings=[],
    )


def _make_composite_score() -> CompositeScore:
    return CompositeScore(
        case_id="cs_ut_per_turn_trace",
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


def test_per_turn_trace_populated_for_every_turn() -> None:
    """Populated case: every turn yields a dict with all three R-item fields."""
    turns = [
        _make_turn(
            turn_index=0,
            tool_calls=[
                {"tool_name": "search_knowledge", "status": "ok", "latency_ms": 700}
            ],
            projected_context={
                "phase_plan": {
                    "phase": "DISCOVER",
                    "allowed_tools": ["search_knowledge", "classify_use_case"],
                    "system_instruction": "You are in the DISCOVER phase.",
                },
                "intake_state": {"intake_complete": False},
                "candidate_use_cases": ["UC-B"],
            },
        ),
        _make_turn(
            turn_index=1,
            tool_calls=[
                {"tool_name": "resolve_article", "status": "ok", "latency_ms": 420}
            ],
            projected_context={
                "phase_plan": {
                    "phase": "RESOLVE",
                    "allowed_tools": ["resolve_article", "record_outcome"],
                    "system_instruction": "You are in the RESOLVE phase.",
                },
                "intake_state": {"intake_complete": True},
                "candidate_use_cases": ["UC-B"],
            },
        ),
    ]
    executor = _StubExecutor()
    result = executor._build_case_result(
        _build_case("cs_ut_per_turn_trace"),
        _make_session_result(),
        _make_trace_data(turns),
        _make_composite_score(),
        llm_calls=[],
    )

    per_turn_trace = result["per_turn_trace"]
    assert isinstance(per_turn_trace, list)
    assert len(per_turn_trace) == 2

    for i, record in enumerate(per_turn_trace):
        assert set(record.keys()) == {"tool_calls", "phase_plan", "projection"}
        assert isinstance(record["tool_calls"], list)
        assert isinstance(record["projection"], dict)
        # phase_plan is read from inside projection; both must agree.
        assert record["phase_plan"] == record["projection"]["phase_plan"]
        assert record["phase_plan"]["phase"] == (
            "DISCOVER" if i == 0 else "RESOLVE"
        )

    # Turn-0 tool_calls verbatim — single search_knowledge entry.
    assert per_turn_trace[0]["tool_calls"][0]["tool_name"] == "search_knowledge"
    # Turn-1 tool_calls verbatim — single resolve_article entry.
    assert per_turn_trace[1]["tool_calls"][0]["tool_name"] == "resolve_article"


def test_per_turn_trace_empty_when_trace_has_no_turns() -> None:
    """Defensive: ``trace_data.turns == []`` yields ``per_turn_trace = []``.

    A session that fails before producing any bot turn (e.g. a
    pre-bot-turn abort) must still produce a uniform schema row.
    """
    executor = _StubExecutor()
    result = executor._build_case_result(
        _build_case("cs_ut_empty_turns"),
        _make_session_result(),
        _make_trace_data(turns=[]),
        _make_composite_score(),
        llm_calls=[],
    )
    assert result["per_turn_trace"] == []


def test_per_turn_trace_handles_missing_phase_plan_in_projection() -> None:
    """A turn whose projection lacks ``phase_plan`` reports ``phase_plan = None``.

    ``ContextProjectionBuilder`` writes ``phase_plan`` only when a
    ``PhasePlan`` is present — a pre-plan turn (e.g. session-create
    bootstrap) projects ``intake_state`` / ``candidate_use_cases`` /
    ``already_called`` but not ``phase_plan``. The serialiser must
    surface this as ``None``, not raise.
    """
    turn = _make_turn(
        turn_index=0,
        projected_context={
            # No phase_plan key — pre-plan projection shape.
            "intake_state": {"intake_complete": False},
            "candidate_use_cases": ["UC-B"],
        },
    )
    per_turn_trace = BatchExecutor._build_per_turn_trace(_make_trace_data([turn]))
    assert len(per_turn_trace) == 1
    assert per_turn_trace[0]["phase_plan"] is None
    # projection is still surfaced unchanged.
    assert per_turn_trace[0]["projection"] == {
        "intake_state": {"intake_complete": False},
        "candidate_use_cases": ["UC-B"],
    }


def test_per_turn_trace_falls_back_when_fields_malformed() -> None:
    """Non-dict projected_context / non-list tool_calls fall back, not raise.

    Belt-and-braces — ``TraceCollector`` already validates and parses
    JSONB; an upstream regression that lets a malformed row through
    must not blow up the result writer.
    """
    bad_turn = SimpleNamespace(
        tool_calls="not-a-list",
        projected_context="not-a-dict",
    )
    per_turn_trace = BatchExecutor._build_per_turn_trace(
        SimpleNamespace(turns=[bad_turn])
    )
    assert len(per_turn_trace) == 1
    assert per_turn_trace[0] == {
        "tool_calls": [],
        "phase_plan": None,
        "projection": {},
    }


def test_existing_fields_byte_identical_pre_sprint_28() -> None:
    """Backwards-compat: the Sprint 25 ``llm_calls`` and ``transcript``
    fields and ``case_id`` / ``composite_score`` / ``failure_tags`` are
    preserved byte-identical — Sprint 28 is strictly additive.
    """
    llm_calls = [
        {"call_type": "chat", "turn_index": 0, "latency_ms": 4200, "success": True},
        {"call_type": "rerank", "turn_index": 0, "latency_ms": 880, "success": True},
    ]
    turn = _make_turn(
        turn_index=0,
        tool_calls=[{"tool_name": "search_knowledge"}],
        projected_context={"phase_plan": {"phase": "DISCOVER"}},
    )
    executor = _StubExecutor()
    result = executor._build_case_result(
        _build_case("cs_ut_per_turn_trace"),
        _make_session_result(),
        _make_trace_data([turn]),
        _make_composite_score(),
        llm_calls=llm_calls,
    )
    # Sprint 25 enrichment preserved verbatim.
    assert result["llm_calls"] == llm_calls
    # Transcript preserved verbatim.
    assert result["transcript"] == [{"role": "user", "content": "hi"}]
    # Pre-existing scalars unchanged.
    assert result["case_id"] == "cs_ut_per_turn_trace"
    assert result["composite_score"] == 0.9
    assert result["failure_tags"] == []
    # The new field is the only Sprint-28 addition.
    assert "per_turn_trace" in result


def test_full_result_dict_is_json_serializable() -> None:
    """JSON-serialisability: no datetime, ndarray, or set leaks into projection.

    ``ContextProjectionBuilder`` writes via Jackson ``ObjectMapper``, so
    the wire shape is JSON-native; ``TraceCollector._parse_jsonb_field``
    parses with ``json.loads``. Belt-and-braces — assert that the
    serialised case result round-trips through ``json.dumps`` cleanly.
    """
    turn = _make_turn(
        turn_index=0,
        tool_calls=[
            {
                "tool_name": "search_knowledge",
                "result_count": 3,
                "faq_miss": False,
                "latency_ms": 712,
            }
        ],
        projected_context={
            "phase_plan": {
                "phase": "DISCOVER",
                "allowed_tools": ["search_knowledge"],
                "valid_terminal_outcomes": ["RESOLVED", "ESCALATED_BY_BOT"],
            },
            "intake_state": {
                "required_fields": ["a", "b"],
                "fields_collected": {"a": "v"},
                "fields_remaining": ["b"],
                "intake_complete": False,
            },
        },
    )
    executor = _StubExecutor()
    result = executor._build_case_result(
        _build_case("cs_ut_per_turn_trace"),
        _make_session_result(),
        _make_trace_data([turn]),
        _make_composite_score(),
        llm_calls=[],
    )
    encoded = json.dumps(result)
    decoded = json.loads(encoded)
    assert decoded["per_turn_trace"][0]["phase_plan"]["phase"] == "DISCOVER"
    assert decoded["per_turn_trace"][0]["projection"]["intake_state"][
        "intake_complete"
    ] is False


def test_timeout_error_contract_violation_results_emit_empty_per_turn_trace() -> None:
    """All three placeholder paths populate ``per_turn_trace = []``.

    Schema uniformity: downstream consumers can always call
    ``case.get("per_turn_trace", [])`` and never see a ``KeyError`` —
    irrespective of whether the case PASSed, TIMED OUT, ERRORed, or hit
    a trace contract violation.
    """
    executor = _StubExecutor()

    timeout = executor._timeout_result(_build_case("cs_ut"))
    assert timeout["status"] == "TIMEOUT"
    assert timeout["per_turn_trace"] == []

    err = executor._error_result(_build_case("cs_ut"), "boom")
    assert err["status"] == "ERROR"
    assert err["per_turn_trace"] == []

    exc = TraceContractError(
        field="containment_outcome",
        phase="session",
        session_id="sess-xyz",
        available_keys=["active_use_case"],
        reason="missing",
    )
    cv = executor._contract_violation_result(_build_case("cs_ut"), exc)
    assert cv["status"] == "CONTRACT_VIOLATION"
    assert cv["per_turn_trace"] == []
