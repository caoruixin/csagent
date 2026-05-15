"""Session runner -- orchestrates a single user-bot conversation loop.

Coordinates the AgentClient, UserSimulator, and stop-condition checks
to execute a full simulated conversation and produce a SessionResult.
"""

from __future__ import annotations

import logging
import time
import uuid
from dataclasses import dataclass, field

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.scoring.stall_detector import StallDetector
from eval_interactive.simulator.agent_client import AgentClient
from eval_interactive.simulator.user_simulator import UserSimulator

logger = logging.getLogger(__name__)


@dataclass
class SessionResult:
    """Result of a single interactive evaluation session."""

    session_id: str
    case_id: str
    transcript: list[dict] = field(default_factory=list)
    # [{role: "user"|"bot", message: str, turn_index: int}]
    stop_reason: str = ""
    # bot_ended | max_turns_exceeded | stall_detected |
    # loop_detected | goal_achieved | goal_impossible | session_create_failed
    total_turns: int = 0
    elapsed_ms: int = 0
    bot_greeting: str = ""
    creation_error: str | None = None
    # Set when create_session raises. Carries the original exception repr
    # so the executor can short-circuit trace collection and surface the
    # real cause (e.g. ConnectionRefused) instead of a downstream contract
    # violation. session_id is set to "error-<hex>" in this case.


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

    def run_session(self, case_spec: CaseSpec) -> SessionResult:
        """Run a complete evaluation session.

        Flow:
        1. Create bot session with form_context -> greeting.
        2. Generate first user message from simulator.
        3. Loop up to max_turns, checking stop conditions each turn.
        4. Return SessionResult with transcript and stop_reason.

        Args:
            case_spec: The case specification driving this session.

        Returns:
            A SessionResult capturing the full transcript and outcome.
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
            # Send user message to bot
            try:
                bot_resp = self._agent.send_message(
                    result.session_id, user_msg
                )
            except Exception as exc:
                logger.error("Bot API error on turn %d: %s", turn, exc)
                # Record the user message that caused the error
                result.transcript.append({
                    "role": "user",
                    "message": user_msg,
                    "turn_index": turn,
                })
                result.stop_reason = "bot_ended"
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
            sim_result = self._simulator.generate_next(
                case_spec, result.transcript, bot_reply
            )

            goal_status = sim_result.get("goal_status", "in_progress")

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
