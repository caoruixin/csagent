"""Two eval-framework (§3 ``infra``) defects found on a real simulator run.

Defect 2 — a backend HTTP error was recorded as a legitimate bot terminal
-----------------------------------------------------------------------
``SessionRunner.run_session``'s ``send_message`` except-branch stamped
``stop_reason="bot_ended"`` — the identical value the loop stamps when the bot
legitimately sets ``should_end_chat=true``. A backend 500 was therefore
indistinguishable in ``results.json`` from a normal bot-initiated close, and was
scored in full as a bot behaviour failure with the real cause only in stderr.
Worse, ``bot_ended`` is explicitly EXCLUDED from
``HardChecker._TERMINAL_FAILURE_STOP_REASONS`` ("NOT failures and never
appear"), so a session that stamped ``containment_outcome="resolved"`` on an
earlier turn and then died on a 500 skipped the Mode-3 stale-stamp
contradiction and could VACUOUS-PASS ``trace_minimum``.

Fixed by routing transport failures the same way ``SimulatorDriftError`` is
routed: ``stop_reason="error"``, which is deliberately NOT in
``_VALID_TERMINAL_STOP_REASONS`` and IS in
``_TERMINAL_FAILURE_STOP_REASONS``.

Defect 3 — the abandoned worker thread kept running
--------------------------------------------------
``BatchExecutor._run_one`` bounds a case with ``asyncio.wait_for``, but the body
runs in ``asyncio.to_thread`` and asyncio cannot cancel a worker thread. A
timed-out case ran to completion in the background (double-spending the LLM
budget), its real result was discarded, and a synthetic ``TIMEOUT`` row shaped
exactly like a scored FAIL was persisted in its place. Fixed with a cooperative
``threading.Event`` checkpoint + an honest, self-describing TIMEOUT row.
"""

from __future__ import annotations

import asyncio
import threading
from types import SimpleNamespace

import httpx
import pytest

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.hard_checks import HardChecker
from eval_interactive.simulator.session_runner import (
    SessionCancelled,
    SessionRunner,
    check_cancelled,
)
from eval_interactive.trace.models import SessionState, TraceData, TurnTrace


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


def _case(case_id: str = "cs_ut_transport", max_turns: int = 5) -> CaseSpec:
    return CaseSpec(
        case_id=case_id,
        source_session_id=case_id,
        source_dataset="ut",
        form_context=FormContext(
            first_name="U",
            email="u@example.com",
            topic_subject="Ad Support",
            ad_id="",
            description="why was my ad removed",
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
            primary_uc="UC-A",
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
            max_turns=max_turns,
        ),
        scoring=ScoringConfig(
            hard_checks=[],
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
    )


class _Sim:
    """Minimal simulator stub; counts LLM-equivalent calls."""

    def __init__(self) -> None:
        self.next_calls = 0

    def generate_first_message(self, _case) -> dict:
        return {"message": "why was my ad removed?"}

    def generate_next(self, _case, _transcript, _bot_reply) -> dict:
        self.next_calls += 1
        return {
            "message": "ok and then?",
            "user_state": "working",
            "goal_status": "in_progress",
        }


def _http_500() -> httpx.HTTPStatusError:
    request = httpx.Request(
        "POST", "http://localhost:8080/v1/chat/sessions/sess-1/messages"
    )
    response = httpx.Response(500, request=request, text="Internal Server Error")
    return httpx.HTTPStatusError(
        "Server error '500 Internal Server Error'",
        request=request,
        response=response,
    )


class _AgentRaisingOn:
    """Agent stub whose ``send_message`` raises on a chosen turn."""

    def __init__(self, fail_on_call: int, exc: Exception) -> None:
        self._fail_on_call = fail_on_call
        self._exc = exc
        self.calls = 0

    def create_session(self, _form) -> dict:
        return {"session_id": "sess-1", "reply_text": "Hi, how can I help?"}

    def send_message(self, _sid, _msg) -> dict:
        self.calls += 1
        if self.calls == self._fail_on_call:
            raise self._exc
        return {"reply_text": "Let me check that.", "should_end_chat": False}

    def close(self) -> None:
        return None


def _runner(agent, sim=None) -> SessionRunner:
    return SessionRunner(agent, sim or _Sim(), SimpleNamespace())


# ---------------------------------------------------------------------------
# Defect 2 — transport failure is not a legitimate bot terminal
# ---------------------------------------------------------------------------


class TestDefect2TransportErrorIsNotBotEnded:
    @pytest.mark.parametrize(
        "exc",
        [
            _http_500(),
            httpx.ReadTimeout("read timed out"),
            httpx.ConnectError("connection refused"),
            ValueError("malformed JSON body"),
        ],
    )
    def test_transport_failure_stamps_error_not_bot_ended(self, exc):
        result = _runner(_AgentRaisingOn(1, exc)).run_session(_case())
        assert result.stop_reason == "error"
        assert result.stop_reason != "bot_ended"
        assert result.transport_error is not None

    def test_transport_error_carries_the_real_cause_out_of_stderr(self):
        result = _runner(_AgentRaisingOn(1, _http_500())).run_session(_case())
        assert "500" in result.transport_error

    def test_a_real_bot_initiated_close_is_still_bot_ended(self):
        """The two must NOT be conflated — this is the other half of the fix."""

        class _AgentEndingChat(_AgentRaisingOn):
            def send_message(self, _sid, _msg) -> dict:
                self.calls += 1
                return {"reply_text": "Glad that helped!", "should_end_chat": True}

        result = _runner(_AgentEndingChat(99, _http_500())).run_session(_case())
        assert result.stop_reason == "bot_ended"
        assert result.transport_error is None

    def test_partial_transcript_is_preserved_for_diagnosis(self):
        """The user turn that triggered the failure stays in the transcript."""
        result = _runner(_AgentRaisingOn(2, _http_500())).run_session(_case())
        assert result.stop_reason == "error"
        users = [t for t in result.transcript if t["role"] == "user"]
        assert len(users) >= 2

    # -- The scoring consequence, which is the point of the fix --------------

    @staticmethod
    def _trace(containment_outcome: str) -> TraceData:
        return TraceData(
            session_state=SessionState(
                session_id="sess-1",
                active_use_case="UC-A",
                candidate_use_cases=["UC-A"],
                containment_outcome=containment_outcome,
                escalation_reason="",
                total_bot_turns=1,
                clarification_count=0,
                faq_miss_count=0,
                form_context={},
                customer_context={},
                articles_shown=[],
                current_phase="RESOLVE",
            ),
            turns=[
                TurnTrace(
                    turn_index=0,
                    user_message="why was my ad removed?",
                    bot_response="Let me check that.",
                    tool_calls=[{"tool_name": "search_knowledge"}],
                    source_ids=["faq-1"],
                    phase_before="DISCOVER",
                    phase_after="RESOLVE",
                    active_use_case="UC-A",
                    latency_ms=10,
                    projected_context={},
                )
            ],
            events=[],
            handover=None,
        )

    def test_error_is_not_a_valid_terminal_so_blank_outcome_still_fails(self):
        result = next(
            r
            for r in HardChecker().run_checks(
                _case(), self._trace(""), None, "error"
            )
            if r.check_name == "trace_minimum"
        )
        assert result.passed is False
        assert "error" in result.detail

    def test_error_contradicts_a_stale_resolved_stamp(self):
        """The vacuous-pass channel ``bot_ended`` left open.

        ``error`` is in ``_TERMINAL_FAILURE_STOP_REASONS``; ``bot_ended`` is
        deliberately not. So a session that stamped ``resolved`` and then died
        on a 500 now FAILS ``trace_minimum`` instead of being credited.
        """
        trace = self._trace("resolved")

        under_bot_ended = next(
            r
            for r in HardChecker().run_checks(_case(), trace, None, "bot_ended")
            if r.check_name == "trace_minimum"
        )
        under_error = next(
            r
            for r in HardChecker().run_checks(_case(), trace, None, "error")
            if r.check_name == "trace_minimum"
        )

        # The old label credited the stale stamp; the new one contradicts it.
        assert under_bot_ended.passed is True
        assert under_error.passed is False
        assert "contradicted by terminal stop_reason=error" in under_error.detail

    def test_executor_surfaces_an_infra_tag_and_keeps_the_evidence(self):
        from eval_interactive.batch.executor import BatchExecutor

        class _Stub(BatchExecutor):
            def __init__(self) -> None:  # skip Config init
                pass

        case = _case()
        transcript = [{"role": "user", "message": "hi", "turn_index": 1}]
        out = _Stub()._error_result(
            case,
            "bot_transport_failed on turn 1: HTTPStatusError(500)",
            failure_kind="BotTransportError",
            session_id="sess-1",
            transcript=transcript,
            total_turns=1,
            stop_reason="error",
        )
        assert "INFRA:BotTransportError" in out["failure_tags"]
        assert out["status"] == "ERROR"
        assert out["stop_reason"] == "error"
        # Diagnostic evidence survives — the session really does exist.
        assert out["session_id"] == "sess-1"
        assert out["transcript"] == transcript
        assert out["total_turns"] == 1

    def test_error_result_defaults_are_unchanged_for_existing_callers(self):
        from eval_interactive.batch.executor import BatchExecutor

        class _Stub(BatchExecutor):
            def __init__(self) -> None:
                pass

        out = _Stub()._error_result(_case(), "boom")
        assert out["session_id"] == ""
        assert out["transcript"] == []
        assert out["total_turns"] == 0
        assert out["stop_reason"] == "error"


# ---------------------------------------------------------------------------
# Defect 3 — cooperative cancellation + an honest TIMEOUT row
# ---------------------------------------------------------------------------


class TestDefect3TimeoutAndAbandonedWorker:
    def test_default_is_a_realistic_budget(self):
        """120s was below the session-create read budget alone."""
        from eval_interactive.config import (
            DEFAULT_TIMEOUT_PER_SESSION_SECONDS,
            BatchConfig,
        )
        from eval_interactive.simulator.agent_client import (
            DEFAULT_READ_TIMEOUT_SECONDS,
            SESSION_CREATE_READ_TIMEOUT_SECONDS,
        )

        assert BatchConfig().timeout_per_session_seconds == (
            DEFAULT_TIMEOUT_PER_SESSION_SECONDS
        )
        # Must exceed the wall time a HEALTHY 15-turn session can reach without
        # any single request exceeding its own documented budget.
        healthy_bound = (
            SESSION_CREATE_READ_TIMEOUT_SECONDS + 15 * DEFAULT_READ_TIMEOUT_SECONDS
        )
        assert DEFAULT_TIMEOUT_PER_SESSION_SECONDS > healthy_bound
        # And the measured 4-9 minute range must fit with room to spare.
        assert DEFAULT_TIMEOUT_PER_SESSION_SECONDS > 9 * 60

    def test_yaml_default_matches_the_dataclass_default(self):
        from pathlib import Path

        import yaml

        from eval_interactive.config import DEFAULT_TIMEOUT_PER_SESSION_SECONDS

        cfg = yaml.safe_load(
            (
                Path(__file__).resolve().parents[1] / "eval_interactive.yaml"
            ).read_text(encoding="utf-8")
        )
        assert (
            cfg["batch"]["timeout_per_session_seconds"]
            == DEFAULT_TIMEOUT_PER_SESSION_SECONDS
        )

    def test_check_cancelled_is_inert_without_an_event(self):
        check_cancelled(None, "cs_ut", "nowhere")
        check_cancelled(threading.Event(), "cs_ut", "nowhere")

    def test_cancelled_session_stops_instead_of_finishing(self):
        """The headline fix: the abandoned worker must not run to completion."""
        cancel = threading.Event()
        cancel.set()
        sim = _Sim()
        agent = _AgentRaisingOn(99, _http_500())

        with pytest.raises(SessionCancelled):
            _runner(agent, sim).run_session(_case(max_turns=5), cancel_event=cancel)

        # Zero bot calls and zero simulator LLM calls after cancellation.
        assert agent.calls == 0
        assert sim.next_calls == 0

    def test_cancellation_mid_session_stops_further_llm_spend(self):
        """Set the flag after turn 1; the worker must not buy turn 2."""
        cancel = threading.Event()
        sim = _Sim()

        class _AgentSettingFlag(_AgentRaisingOn):
            def send_message(self, _sid, _msg) -> dict:
                self.calls += 1
                cancel.set()
                return {"reply_text": "checking", "should_end_chat": False}

        agent = _AgentSettingFlag(99, _http_500())
        with pytest.raises(SessionCancelled):
            _runner(agent, sim).run_session(_case(max_turns=10), cancel_event=cancel)

        assert agent.calls == 1
        # The simulator checkpoint sits before ``generate_next``, so the
        # post-deadline simulator LLM call is never paid for.
        assert sim.next_calls == 0

    def test_uncancelled_session_is_byte_identical(self):
        """``cancel_event=None`` must not change any existing behaviour."""
        agent_a, agent_b = (
            _AgentRaisingOn(99, _http_500()),
            _AgentRaisingOn(99, _http_500()),
        )
        without = _runner(agent_a).run_session(_case(max_turns=3))
        with_unset = _runner(agent_b).run_session(
            _case(max_turns=3), cancel_event=threading.Event()
        )
        assert without.stop_reason == with_unset.stop_reason
        assert without.total_turns == with_unset.total_turns
        assert len(without.transcript) == len(with_unset.transcript)

    def test_run_one_sets_the_flag_when_the_deadline_fires(self):
        """The deadline must actually SIGNAL the worker, not just walk away."""
        from eval_interactive.batch.executor import BatchExecutor

        seen: dict[str, threading.Event | None] = {"event": None}

        class _Stub(BatchExecutor):
            def __init__(self) -> None:
                self._config = SimpleNamespace(
                    batch=SimpleNamespace(timeout_per_session_seconds=1)
                )

            async def _execute_case(self, case_spec, cancel_event=None):
                seen["event"] = cancel_event
                await asyncio.sleep(30)  # overrun the 1s deadline
                raise AssertionError("should not be reached")

        case = _case()
        out = asyncio.run(_Stub()._run_one(case, asyncio.Semaphore(1)))

        assert out["status"] == "TIMEOUT"
        assert seen["event"] is not None
        assert seen["event"].is_set(), "deadline must request cancellation"

    def test_timeout_row_declares_itself_unmeasured(self):
        """The row must not read as a clean scored verdict."""
        from eval_interactive.batch.executor import BatchExecutor

        class _Stub(BatchExecutor):
            def __init__(self) -> None:
                self._config = SimpleNamespace(
                    batch=SimpleNamespace(timeout_per_session_seconds=1800)
                )

        row = _Stub()._timeout_result(_case())
        assert row["measurement_valid"] is False
        assert row["cancellation"] == "cooperative_requested"
        assert "INFRA:TIMEOUT_WORKER_NOT_FORCIBLY_KILLED" in row["failure_tags"]
        caveat = row["measurement_caveat"]
        # The three things a reader must know.
        assert "UNMEASURED" in caveat
        assert "COOPERATIVE" in caveat
        assert "not measurements" in caveat

    def test_cancelled_row_declares_itself_unmeasured(self):
        from eval_interactive.batch.executor import BatchExecutor

        class _Stub(BatchExecutor):
            def __init__(self) -> None:
                pass

        row = _Stub()._cancelled_result(_case(), "stopped at turn 3")
        assert row["measurement_valid"] is False
        assert row["status"] == "CANCELLED"
        assert row["cancellation"] == "cooperative_honoured"
        assert "INFRA:CANCELLED" in row["failure_tags"]
