"""Session runner -- orchestrates a single user-bot conversation loop.

Coordinates the AgentClient, UserSimulator, and stop-condition checks
to execute a full simulated conversation and produce a SessionResult.
"""

from __future__ import annotations

import logging
import threading
import time
import uuid
from dataclasses import dataclass, field

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.scoring.stall_detector import StallDetector
from eval_interactive.simulator.agent_client import AgentClient
from eval_interactive.simulator.user_simulator import (
    USER_STATE_SCHEMA_VERSION,
    USER_STATE_SIGNAL_SOURCE,
    UserSimulator,
)

logger = logging.getLogger(__name__)


class SessionCancelled(RuntimeError):
    """Raised at a cooperative checkpoint when the run was abandoned.

    ``BatchExecutor._run_one`` bounds each case with ``asyncio.wait_for``, but
    the case body runs in ``asyncio.to_thread`` and **asyncio cannot cancel a
    worker thread**. Before this exception existed, a session that overran
    ``batch.timeout_per_session_seconds`` kept running to completion in the
    background: the batch spent the LLM budget twice (the abandoned worker
    finished its turns and its L3 judge call), threw the real result away, and
    persisted a synthetic ``TIMEOUT`` row in its place. Observed verbatim on a
    real baseline arm — 4/5 cases recorded ``TIMEOUT`` while the log carried
    their ``FAIL ... turns=5`` completion lines.

    The runner and the executor now poll a ``threading.Event`` at cheap
    checkpoints (turn boundaries, and before each of the two remaining LLM
    surfaces) and raise this to unwind promptly.

    Cancellation is **cooperative, therefore approximate**: a checkpoint can
    only be reached between blocking calls, so a worker signalled while inside
    a bot HTTP request (``agent_client.DEFAULT_READ_TIMEOUT_SECONDS`` = 90s) or
    an LLM call finishes that one call first. ``_timeout_result`` states this
    on the row rather than presenting the deadline as a clean terminal.
    """


def check_cancelled(
    cancel_event: threading.Event | None, case_id: str, where: str
) -> None:
    """Raise :class:`SessionCancelled` iff cancellation has been requested.

    Public because ``batch.executor`` places the same checkpoint around the
    post-session stages (trace collection, the L3 judge call) that also cost
    real time and LLM budget.
    """
    if cancel_event is not None and cancel_event.is_set():
        raise SessionCancelled(
            f"case {case_id}: run abandoned by the batch timeout; stopping at "
            f"{where} instead of finishing the session in the background"
        )


@dataclass
class SessionResult:
    """Result of a single interactive evaluation session."""

    session_id: str
    case_id: str
    transcript: list[dict] = field(default_factory=list)
    # [{role: "user"|"bot", message: str, turn_index: int}]
    stop_reason: str = ""
    # bot_ended | max_turns_exceeded | stall_detected |
    # loop_detected | goal_achieved | goal_impossible |
    # session_create_failed | error
    total_turns: int = 0
    elapsed_ms: int = 0
    bot_greeting: str = ""
    # Phase-1 measurement contract (S-Auto-40 WP1-A). One entry per
    # ``generate_next`` call that actually ran, recorded in the SAME call
    # that produced the customer turn — never from a post-``bot_ended`` LLM
    # call. Each entry:
    #   {turn_id, produced_user_turn, user_state, goal_status,
    #    signal_source, schema_version}
    # ``turn_id`` is the latest BOT turn the customer was reacting to, so a
    # downstream consumer can align the signal against the closure-qualified
    # grounded-help structural marker (which bot turn it is). A signal is
    # never carried forward across turns; each is independently stamped.
    user_state_signals: list[dict] = field(default_factory=list)
    creation_error: str | None = None
    # Set when create_session raises. Carries the original exception repr
    # so the executor can short-circuit trace collection and surface the
    # real cause (e.g. ConnectionRefused) instead of a downstream contract
    # violation. session_id is set to "error-<hex>" in this case.
    transport_error: str | None = None
    # Set when ``send_message`` raises MID-SESSION (HTTP 5xx, ReadTimeout,
    # ConnectError, malformed body). Mirrors ``creation_error`` for the
    # in-loop case: the session_id IS real (the session was created), but
    # the conversation was cut short by a transport / backend failure rather
    # than by anything the bot decided. Carries the original exception repr
    # so the executor can surface the real cause in ``results.json``
    # (``INFRA:BotTransportError``) instead of leaving it in stderr.
    #
    # ``stop_reason`` is ``"error"`` in this case, NOT ``"bot_ended"`` --
    # see the ``send_message`` except-branch in ``run_session`` for why.


class SessionRunner:
    """Runs a single evaluation session end-to-end.

    Orchestrates the conversation loop between the AgentClient (bot)
    and UserSimulator (simulated customer), checking for stop conditions
    after each turn.
    """

    def __init__(
        self,
        agent_client: AgentClient,
        user_simulator: UserSimulator,
        stall_detector: StallDetector,
    ):
        """Initialize the session runner.

        Args:
            agent_client: HTTP client for the CS Agent under test.
            user_simulator: LLM-based user turn generator.
            stall_detector: Detects bot stall patterns (not used for
                stop condition but available for post-hoc analysis).
        """
        self._agent = agent_client
        self._simulator = user_simulator
        self._stall_detector = stall_detector

    def run_session(
        self,
        case_spec: CaseSpec,
        cancel_event: threading.Event | None = None,
    ) -> SessionResult:
        """Run a complete evaluation session.

        Flow:
        1. Create bot session with form_context -> greeting.
        2. Generate first user message from simulator.
        3. Loop up to max_turns, checking stop conditions each turn.
        4. Return SessionResult with transcript and stop_reason.

        Args:
            case_spec: The case specification driving this session.
            cancel_event: Optional cooperative-cancellation flag set by
                ``BatchExecutor._run_one`` when the batch deadline fires. Polled
                twice per turn (before the bot call and before the simulator
                call); raises :class:`SessionCancelled` so an abandoned worker
                stops spending LLM budget instead of running to completion in
                the background. ``None`` (the default) disables the polling
                entirely, so every existing caller is unaffected.

        Returns:
            A SessionResult capturing the full transcript and outcome.

        Raises:
            SessionCancelled: cancellation was requested at a checkpoint.
        """
        start_ns = time.monotonic_ns()
        result = SessionResult(
            session_id="",
            case_id=case_spec.case_id,
        )
        max_turns = case_spec.expected.max_turns

        # ---- Step 1: Create bot session ----
        form_dict = {
            "first_name": case_spec.form_context.first_name,
            "email": case_spec.form_context.email,
            "topic_subject": case_spec.form_context.topic_subject,
            "ad_id": case_spec.form_context.ad_id,
            "description": case_spec.form_context.description,
        }

        try:
            session_resp = self._agent.create_session(form_dict)
        except Exception as exc:
            # Sprint 6 §G0 (closure-normalized): tag ReadTimeout
            # distinctly so the executor / handoff classifier can
            # separate a session-create read timeout from a semantic
            # 4xx / 5xx failure (auth, scope, backend bug). The
            # agent_client uses the wider 120s read budget but does
            # NOT retry; if we are here on a ReadTimeout the 120s
            # budget was insufficient on this attempt.
            import httpx as _httpx
            if isinstance(exc, _httpx.ReadTimeout):
                tagged = (
                    f"ReadTimeout('{exc}'). "
                    f"create_session exceeded the eval-client 120s read "
                    f"budget — upstream LLM/backend latency, not a "
                    f"semantic failure."
                )
                logger.error("Failed to create bot session: %s", tagged)
                result.creation_error = tagged
            else:
                logger.error("Failed to create bot session: %s", exc, exc_info=True)
                result.creation_error = repr(exc)
            result.session_id = f"error-{uuid.uuid4().hex[:8]}"
            result.stop_reason = "session_create_failed"
            result.elapsed_ms = _elapsed_ms(start_ns)
            return result

        result.session_id = session_resp.get("session_id", "")
        result.bot_greeting = session_resp.get("reply_text", "")

        # ---- Codex round 5 P0: include the create-session form turn in the
        # transcript so L3 judge / results.json / report HTML all see what
        # the customer originally asked. Without this, cases like
        # cs_interactive_192 surface a transcript that begins on the
        # follow-up message ("OK. What items are allowed?") even though the
        # bot already answered the form description in `bot_greeting`. The
        # form-text turn is recorded at turn_index=0 (distinct from
        # turn_index=1 which is the simulator's first follow-up). Skip when
        # the form has no description -- topic_subject alone is metadata,
        # not a user-visible question.
        form_user_text = (case_spec.form_context.description or "").strip()
        if form_user_text:
            result.transcript.append({
                "role": "user",
                "message": form_user_text,
                "turn_index": 0,
                "source": "form_context",
            })
        if result.bot_greeting:
            result.transcript.append({
                "role": "bot",
                "message": result.bot_greeting,
                "turn_index": 0,
                "source": "session_create",
            })

        # ---- Step 2: First user message ----
        first_msg = self._simulator.generate_first_message(case_spec)
        user_msg = first_msg["message"]

        # ---- Step 3: Conversation loop ----
        for turn in range(1, max_turns + 1):
            check_cancelled(
                cancel_event, case_spec.case_id, f"turn {turn} (before bot call)"
            )
            # Send user message to bot
            try:
                bot_resp = self._agent.send_message(
                    result.session_id, user_msg
                )
            except Exception as exc:
                # A transport / backend failure is NOT a terminal the bot
                # chose. Before this fix the branch stamped
                # ``stop_reason="bot_ended"`` -- the exact same value line
                # ~212 below stamps when the bot legitimately sets
                # ``should_end_chat=true``. Two consequences, both observed
                # on a real run where POST
                # /v1/chat/sessions/{id}/messages returned 500:
                #
                # 1. The two are INDISTINGUISHABLE in ``results.json``, so a
                #    transport failure was recorded in full as a bot
                #    behaviour failure (blank ``containment_outcome`` ->
                #    ``trace_minimum`` FAIL, ``correct_uc``=0.0,
                #    ``correct_outcome``=0.0) with the real cause visible
                #    only in stderr.
                # 2. Worse, ``bot_ended`` is explicitly excluded from
                #    ``hard_checks._TERMINAL_FAILURE_STOP_REASONS`` ("NOT
                #    failures and never appear"), so a session that stamped
                #    ``containment_outcome="resolved"`` on an earlier turn
                #    and THEN died on a 500 skipped the Mode-3
                #    stale-stamp contradiction and could VACUOUS-PASS
                #    ``trace_minimum``.
                #
                # ``"error"`` is the value the repo already uses for exactly
                # this class of event: ``SimulatorDriftError``'s docstring
                # states it lands on ``stop_reason="error"`` deliberately
                # because that is NOT in
                # ``hard_checks._VALID_TERMINAL_STOP_REASONS`` and therefore
                # cannot vacuous-pass. HTTP / transport failures now take
                # the same route. It is additionally IN
                # ``_TERMINAL_FAILURE_STOP_REASONS``, which closes (2).
                logger.error(
                    "Bot API error on turn %d: %s", turn, exc, exc_info=True
                )
                # Record the user message that caused the error
                result.transcript.append({
                    "role": "user",
                    "message": user_msg,
                    "turn_index": turn,
                })
                result.transport_error = repr(exc)
                result.stop_reason = "error"
                break

            bot_reply = bot_resp.get("reply_text", "")
            should_end = bot_resp.get("should_end_chat", False)

            # Record the turn (both user and bot)
            result.transcript.append({
                "role": "user",
                "message": user_msg,
                "turn_index": turn,
            })
            result.transcript.append({
                "role": "bot",
                "message": bot_reply,
                "turn_index": turn,
            })

            # ---- Stop condition checks (priority order) ----

            # 1. Bot says the chat should end
            if should_end:
                result.stop_reason = "bot_ended"
                break

            # 2. Max turns reached
            if turn >= max_turns:
                result.stop_reason = "max_turns_exceeded"
                break

            # 3. Loop detection: last 2 bot replies identical
            bot_replies = [
                t["message"]
                for t in result.transcript
                if t["role"] == "bot"
            ]
            if len(bot_replies) >= 2 and bot_replies[-1] == bot_replies[-2]:
                result.stop_reason = "loop_detected"
                break

            # ---- Generate next user message ----
            check_cancelled(
                cancel_event,
                case_spec.case_id,
                f"turn {turn} (before simulator call)",
            )
            sim_result = self._simulator.generate_next(
                case_spec, result.transcript, bot_reply
            )

            goal_status = sim_result.get("goal_status", "in_progress")

            # Phase-1 (S-Auto-40 WP1-A): persist the positive structured
            # user-state signal produced by THIS ``generate_next`` call,
            # immediately, tagged with provenance. ``turn_id`` is the bot turn
            # the customer is reacting to (the just-received ``turn``); the
            # message this call produces lands at ``turn + 1``. Recorded for
            # every state (including None / "working") so the downstream
            # evaluator sees the full per-turn series and can decide
            # SATISFIED / UNRESOLVED / UNKNOWN without back-inference. No
            # signal is emitted after ``bot_ended`` because ``generate_next``
            # is not called past the break above — that is intentional (no
            # post-terminal LLM call); such draws carry no terminal signal and
            # read as UNKNOWN downstream.
            result.user_state_signals.append({
                "turn_id": turn,
                "produced_user_turn": turn + 1,
                "user_state": sim_result.get("user_state"),
                "goal_status": goal_status,
                "signal_source": USER_STATE_SIGNAL_SOURCE,
                "schema_version": USER_STATE_SCHEMA_VERSION,
            })

            if goal_status == "achieved":
                result.stop_reason = "goal_achieved"
                # Record the final user "thank you" message if present
                final_msg = sim_result.get("message", "")
                if final_msg:
                    result.transcript.append({
                        "role": "user",
                        "message": final_msg,
                        "turn_index": turn + 1,
                    })
                break

            if goal_status == "impossible":
                result.stop_reason = "goal_impossible"
                final_msg = sim_result.get("message", "")
                if final_msg:
                    result.transcript.append({
                        "role": "user",
                        "message": final_msg,
                        "turn_index": turn + 1,
                    })
                break

            # Continue with the new user message
            user_msg = sim_result.get("message", "")

        # If the loop finished without setting a stop_reason (shouldn't happen,
        # but be defensive)
        if not result.stop_reason:
            result.stop_reason = "max_turns_exceeded"

        result.total_turns = len(
            [t for t in result.transcript if t["role"] == "user"]
        )
        result.elapsed_ms = _elapsed_ms(start_ns)
        return result


def _elapsed_ms(start_ns: int) -> int:
    """Calculate elapsed milliseconds since start_ns."""
    return int((time.monotonic_ns() - start_ns) / 1_000_000)
