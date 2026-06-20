"""Tests for Wave B1.4: trace contract validation.

Covers:
* Strict mode raises ``TraceContractError`` for missing/invalid fields.
* Lenient mode records warnings on ``TraceData.contract_warnings`` and
  preserves the legacy default-to-empty behaviour.
* Tool-call specific requirements (``request_handover.escalation_reason``
  must be a canonical ``EscalationTrigger`` value; ``record_outcome``
  must carry an ``outcome_class``).
* Executor integration: a contract-violating trace produces a result
  with ``status="CONTRACT_VIOLATION"`` and a synthetic L1 entry whose
  name encodes the missing field.
"""

from __future__ import annotations

from typing import Any

import pytest

from eval_interactive.trace.collector import (
    CONDITIONAL_SESSION_FIELDS,
    CONTAINMENT_OUTCOME_VALUES,
    REQUIRED_SESSION_FIELDS,
    REQUIRED_TOOL_CALL_FIELDS,
    REQUIRED_TURN_FIELDS,
    TERMINAL_PHASES,
    TOOL_SPECIFIC_REQUIRED_ARGS,
    TraceCollector,
    TraceContractError,
)


# ---------------------------------------------------------------------------
# Stub agent client -- returns canned responses so tests run offline.
# ---------------------------------------------------------------------------


class _StubAgentClient:
    """Minimal stand-in for AgentClient used only by the collector."""

    def __init__(
        self,
        *,
        session: dict | None = None,
        trace: list[dict] | None = None,
        events: list[dict] | None = None,
        handover_logs: list[dict] | None = None,
    ):
        self._session = session if session is not None else {}
        self._trace = trace if trace is not None else []
        self._events = events if events is not None else []
        self._handover_logs = handover_logs if handover_logs is not None else []

    def get_session(self, session_id: str) -> dict:
        return self._session

    def get_trace(self, session_id: str) -> list[dict]:
        return self._trace

    def get_events(self, session_id: str) -> list[dict]:
        return self._events

    def get_handover_logs(self) -> list[dict]:
        return self._handover_logs

    def close(self) -> None:  # pragma: no cover -- not exercised here
        pass


def _well_formed_session() -> dict:
    """Session payload populating every required field."""
    return {
        "active_use_case": "UC-A",
        "containment_outcome": "resolved",
        "current_phase": "CLOSE",
        "escalation_reason": "",
        "total_bot_turns": 3,
        "candidate_use_cases": ["UC-A"],
    }


def _well_formed_turn() -> dict:
    """Single turn populating all required turn fields."""
    return {
        "turn_index": 0,
        "user_message": "hi",
        "bot_response": "hello",
        "phase_before": "INTAKE",
        "phase_after": "CLOSE",
        "tool_calls": [
            {"tool_name": "search_knowledge", "arguments": {"query": "foo"}},
        ],
    }


# ---------------------------------------------------------------------------
# Required-field-map sanity tests
# ---------------------------------------------------------------------------


class TestContractMap:
    """Ensure the contract map exposes the documented required fields."""

    def test_session_contract_includes_current_phase(self) -> None:
        assert "current_phase" in REQUIRED_SESSION_FIELDS

    def test_active_use_case_is_conditional(self) -> None:
        # Routing happens on the first user message, so this field is
        # only required once at least one bot turn has run.
        assert "active_use_case" in CONDITIONAL_SESSION_FIELDS
        assert "active_use_case" not in REQUIRED_SESSION_FIELDS

    def test_containment_outcome_is_conditional(self) -> None:
        # Set only when the session terminates; mid-conversation
        # sessions legitimately have no outcome yet.
        assert "containment_outcome" in CONDITIONAL_SESSION_FIELDS
        assert "containment_outcome" not in REQUIRED_SESSION_FIELDS

    def test_terminal_phases_match_server(self) -> None:
        assert TERMINAL_PHASES == frozenset({"CLOSE", "ESCALATE"})

    def test_turn_contract_includes_phase_after(self) -> None:
        assert "phase_after" in REQUIRED_TURN_FIELDS

    def test_tool_call_contract_includes_tool_name_and_arguments(self) -> None:
        assert "tool_name" in REQUIRED_TOOL_CALL_FIELDS
        assert "arguments" in REQUIRED_TOOL_CALL_FIELDS

    def test_request_handover_requires_escalation_reason(self) -> None:
        assert "escalation_reason" in TOOL_SPECIFIC_REQUIRED_ARGS["request_handover"]

    def test_record_outcome_requires_outcome_class(self) -> None:
        assert "outcome_class" in TOOL_SPECIFIC_REQUIRED_ARGS["record_outcome"]

    def test_containment_outcome_enum_documented(self) -> None:
        assert CONTAINMENT_OUTCOME_VALUES == frozenset(
            {"resolved", "escalated", "abandoned", "timeout"}
        )


# ---------------------------------------------------------------------------
# Happy path
# ---------------------------------------------------------------------------


class TestHappyPath:
    """A trace with all required fields collects cleanly in either mode."""

    def test_strict_mode_no_exception(self) -> None:
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[_well_formed_turn()],
        )
        collector = TraceCollector(client, contract_mode="strict")
        trace = collector.collect("sess-1")
        assert trace.session_state.active_use_case == "UC-A"
        assert trace.session_state.containment_outcome == "resolved"
        assert trace.contract_warnings == []

    def test_lenient_mode_no_warnings(self) -> None:
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[_well_formed_turn()],
        )
        collector = TraceCollector(client, contract_mode="lenient")
        trace = collector.collect("sess-1")
        assert trace.contract_warnings == []

    def test_default_mode_is_strict(self) -> None:
        # Constructing without arg should default to strict.
        client = _StubAgentClient(session={}, trace=[])
        collector = TraceCollector(client)
        assert collector.contract_mode == "strict"


# ---------------------------------------------------------------------------
# Strict-mode failures
# ---------------------------------------------------------------------------


class TestStrictModeFailures:
    """Strict mode raises TraceContractError on the first violation."""

    def test_missing_active_use_case_raises(self) -> None:
        bad = _well_formed_session()
        bad.pop("active_use_case")
        client = _StubAgentClient(session=bad, trace=[_well_formed_turn()])
        collector = TraceCollector(client, contract_mode="strict")
        with pytest.raises(TraceContractError) as exc_info:
            collector.collect("sess-strict")
        assert exc_info.value.field == "active_use_case"
        assert exc_info.value.phase == "session"
        assert exc_info.value.session_id == "sess-strict"
        # available_keys should reflect the *other* keys that WERE present
        assert "containment_outcome" in exc_info.value.available_keys
        assert "active_use_case" not in exc_info.value.available_keys

    def test_missing_containment_outcome_raises(self) -> None:
        bad = _well_formed_session()
        bad.pop("containment_outcome")
        client = _StubAgentClient(session=bad, trace=[_well_formed_turn()])
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "containment_outcome"

    def test_invalid_containment_outcome_enum_raises(self) -> None:
        bad = _well_formed_session()
        bad["containment_outcome"] = "wibble"
        client = _StubAgentClient(session=bad, trace=[_well_formed_turn()])
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "containment_outcome"
        assert "enum_violation" in exc_info.value.reason

    def test_missing_phase_after_in_turn_raises(self) -> None:
        bad_turn = _well_formed_turn()
        bad_turn.pop("phase_after")
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[bad_turn],
        )
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "phase_after"
        assert exc_info.value.phase == "turn"

    def test_request_handover_missing_escalation_reason_raises(self) -> None:
        turn = _well_formed_turn()
        turn["tool_calls"] = [
            {"tool_name": "request_handover", "arguments": {}},
        ]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "escalation_reason"
        assert exc_info.value.phase == "tool_call"

    def test_request_handover_invalid_escalation_reason_enum_raises(self) -> None:
        turn = _well_formed_turn()
        turn["tool_calls"] = [
            {
                "tool_name": "request_handover",
                "arguments": {"escalation_reason": "not_a_real_value"},
            },
        ]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "escalation_reason"
        assert "enum_violation" in exc_info.value.reason

    def test_request_handover_agent_unable_to_resolve_is_accepted(self) -> None:
        # Sprint 096 / S-Auto-44 (M-Auto-9 WP1): the new canonical value is a
        # member of ESCALATION_TRIGGER_VALUES, so strict-mode collection must
        # NOT raise an enum_violation for it (the collector derives its enum
        # check from the single schema source).
        turn = _well_formed_turn()
        turn["tool_calls"] = [
            {
                "tool_name": "request_handover",
                "arguments": {"escalation_reason": "agent_unable_to_resolve"},
            },
        ]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        # Must complete without raising on the escalation_reason field.
        trace = TraceCollector(client, contract_mode="strict").collect("s")
        assert trace is not None

    def test_record_outcome_missing_outcome_class_raises(self) -> None:
        turn = _well_formed_turn()
        turn["tool_calls"] = [
            {"tool_name": "record_outcome", "arguments": {}},
        ]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "outcome_class"

    def test_tool_call_missing_tool_name_raises(self) -> None:
        turn = _well_formed_turn()
        turn["tool_calls"] = [{"arguments": {}}]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "tool_name"

    def test_tool_call_missing_arguments_key_raises(self) -> None:
        turn = _well_formed_turn()
        turn["tool_calls"] = [{"tool_name": "search_knowledge"}]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("s")
        assert exc_info.value.field == "arguments"

    def test_tool_call_with_empty_arguments_dict_passes(self) -> None:
        # Empty dict {} is a valid value for ``arguments`` -- only a
        # totally absent key is a violation. Use a non-special tool to
        # avoid tripping tool-specific extra checks.
        turn = _well_formed_turn()
        turn["tool_calls"] = [{"tool_name": "search_knowledge", "arguments": {}}]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        trace = TraceCollector(client, contract_mode="strict").collect("s")
        assert trace.turns[0].tool_calls[0]["arguments"] == {}


# ---------------------------------------------------------------------------
# Conditional contracts -- empty fields are OK when the precondition fails
# ---------------------------------------------------------------------------


class TestConditionalContracts:
    """active_use_case and containment_outcome are only required when
    the session has actually progressed (had turns / reached terminal).
    """

    def test_pre_routing_session_no_turns_does_not_require_active_use_case(
        self,
    ) -> None:
        # Session was created but ended before the first bot turn ran.
        # The bot legitimately has no UC yet -- this must not be a
        # contract violation.
        session = {
            "current_phase": "DISCOVER",
            "total_bot_turns": 0,
        }
        client = _StubAgentClient(session=session, trace=[])
        collector = TraceCollector(client, contract_mode="strict")
        trace = collector.collect("sess-pre-routing")
        assert trace.session_state.active_use_case == ""
        assert trace.contract_warnings == []

    def test_mid_conversation_session_does_not_require_containment_outcome(
        self,
    ) -> None:
        # current_phase is non-terminal and no handover -- the session
        # is still in progress, so an empty containment_outcome is fine.
        session = {
            "active_use_case": "UC-A",
            "current_phase": "RESOLVE",
            "total_bot_turns": 2,
        }
        client = _StubAgentClient(
            session=session,
            trace=[_well_formed_turn(), _well_formed_turn()],
        )
        collector = TraceCollector(client, contract_mode="strict")
        trace = collector.collect("sess-mid")
        assert trace.session_state.containment_outcome == ""
        assert trace.contract_warnings == []

    def test_terminal_phase_close_still_requires_containment_outcome(
        self,
    ) -> None:
        # current_phase=CLOSE means the session ended; containment_outcome
        # MUST be set. This protects against silent regressions in the
        # server-side outcome-recording path.
        session = {
            "active_use_case": "UC-A",
            "current_phase": "CLOSE",
            "total_bot_turns": 3,
        }
        client = _StubAgentClient(session=session, trace=[_well_formed_turn()])
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("sess-close")
        assert exc_info.value.field == "containment_outcome"
        assert exc_info.value.reason == "missing_at_terminal_phase"

    def test_terminal_phase_escalate_still_requires_containment_outcome(
        self,
    ) -> None:
        session = {
            "active_use_case": "UC-K",
            "current_phase": "ESCALATE",
            "total_bot_turns": 3,
        }
        client = _StubAgentClient(session=session, trace=[_well_formed_turn()])
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("sess-esc")
        assert exc_info.value.field == "containment_outcome"

    def test_turns_present_still_requires_active_use_case(self) -> None:
        # If the bot took turns but never recorded a UC, that IS a real
        # contract violation -- routing is supposed to happen on turn 1.
        session = {
            "current_phase": "RESOLVE",
            "total_bot_turns": 1,
        }
        client = _StubAgentClient(session=session, trace=[_well_formed_turn()])
        with pytest.raises(TraceContractError) as exc_info:
            TraceCollector(client, contract_mode="strict").collect("sess-noruc")
        assert exc_info.value.field == "active_use_case"
        assert exc_info.value.reason == "missing_after_turns"


# ---------------------------------------------------------------------------
# Lenient mode -- collects, warns, never raises
# ---------------------------------------------------------------------------


class TestLenientMode:
    """Lenient mode never raises; warnings show up on TraceData."""

    def test_missing_active_use_case_records_warning(self) -> None:
        bad = _well_formed_session()
        bad.pop("active_use_case")
        client = _StubAgentClient(session=bad, trace=[_well_formed_turn()])
        collector = TraceCollector(client, contract_mode="lenient")
        trace = collector.collect("sess-lenient")
        assert trace.session_state.active_use_case == ""
        assert trace.contract_warnings, "expected at least one warning"
        joined = " ".join(trace.contract_warnings)
        assert "active_use_case" in joined
        assert "phase=session" in joined

    def test_missing_phase_after_records_warning(self) -> None:
        bad_turn = _well_formed_turn()
        bad_turn.pop("phase_after")
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[bad_turn],
        )
        collector = TraceCollector(client, contract_mode="lenient")
        trace = collector.collect("sess-l")
        assert trace.turns[0].phase_after == ""
        assert any("phase_after" in w for w in trace.contract_warnings)

    def test_invalid_escalation_reason_records_warning(self) -> None:
        turn = _well_formed_turn()
        turn["tool_calls"] = [
            {
                "tool_name": "request_handover",
                "arguments": {"escalation_reason": "not_real"},
            },
        ]
        client = _StubAgentClient(
            session=_well_formed_session(),
            trace=[turn],
        )
        collector = TraceCollector(client, contract_mode="lenient")
        trace = collector.collect("sess-l")
        assert any(
            "escalation_reason" in w and "enum_violation" in w
            for w in trace.contract_warnings
        )

    def test_invalid_contract_mode_value_raises(self) -> None:
        client = _StubAgentClient()
        with pytest.raises(ValueError):
            TraceCollector(client, contract_mode="loose")  # type: ignore[arg-type]


# ---------------------------------------------------------------------------
# Executor integration -- contract violations produce CONTRACT_VIOLATION
# results with a synthetic L1 entry.
# ---------------------------------------------------------------------------


class TestExecutorIntegration:
    """The executor catches TraceContractError and shapes the result."""

    def _make_case_spec(self) -> Any:
        """Minimal CaseSpec stand-in -- only the attrs the executor reads."""

        class _Expected:
            primary_uc = "UC-A"
            outcome_class = "resolve"

        class _Spec:
            case_id = "test-case-001"
            expected = _Expected()

        return _Spec()

    def test_contract_violation_result_shape(self) -> None:
        from eval_interactive.batch.executor import BatchExecutor
        from eval_interactive.trace.collector import TraceContractError

        # We don't need a real Config -- the result-builder path
        # doesn't read it. Pass None to keep the test isolated.
        executor = BatchExecutor.__new__(BatchExecutor)  # bypass __init__

        exc = TraceContractError(
            field="active_use_case",
            phase="session",
            session_id="sess-x",
            available_keys=["containment_outcome", "current_phase"],
            reason="missing",
        )
        result = executor._contract_violation_result(self._make_case_spec(), exc)

        assert result["status"] == "CONTRACT_VIOLATION"
        assert result["case_passed"] is False
        assert result["composite_score"] == 0.0
        assert result["stop_reason"] == "contract_violation"
        # Failure tag must be distinct from TIMEOUT / ERROR.
        assert any(
            tag.startswith("CONTRACT_VIOLATION") for tag in result["failure_tags"]
        )
        # l1_results must contain a single synthetic entry naming the field.
        assert len(result["l1_results"]) == 1
        l1 = result["l1_results"][0]
        assert l1["check"] == "trace_contract_active_use_case"
        assert l1["passed"] is False
        # Structured contract_violation block for downstream tooling.
        cv = result["contract_violation"]
        assert cv["field"] == "active_use_case"
        assert cv["phase"] == "session"
        assert cv["reason"] == "missing"
        assert "containment_outcome" in cv["available_keys"]

    def test_contract_violation_distinct_from_timeout_and_error(self) -> None:
        """CONTRACT_VIOLATION must not be conflated with TIMEOUT or ERROR."""
        from eval_interactive.batch.executor import BatchExecutor
        from eval_interactive.trace.collector import TraceContractError

        executor = BatchExecutor.__new__(BatchExecutor)
        exc = TraceContractError(
            field="phase_after",
            phase="turn",
            session_id="s",
            available_keys=[],
            reason="missing",
        )
        result = executor._contract_violation_result(self._make_case_spec(), exc)
        assert result["status"] == "CONTRACT_VIOLATION"
        assert result["status"] != "TIMEOUT"
        assert result["status"] != "ERROR"
        # No TIMEOUT / ERROR tags should leak in.
        for tag in result["failure_tags"]:
            assert not tag.startswith("TIMEOUT")
            assert not tag.startswith("ERROR")
