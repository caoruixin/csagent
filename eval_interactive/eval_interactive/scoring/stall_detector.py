"""Stall detector -- detects promise-without-followup patterns.

Scans bot responses for promises of action (e.g. "let me check")
and verifies that a visible result follows within a configurable
turn window.  Flags stalls with specific failure tags:
  STALL_AFTER_TOOL_INTENT   -- promised action, no tool call occurred
  TOOL_ERROR_NOT_SURFACED   -- tool call happened but errored, bot
                               did not explain the failure to the user
  PLACEHOLDER_WITHOUT_FOLLOWUP -- promise made, but no substantive
                               information appeared in the window
"""

from __future__ import annotations

import logging
import re
from dataclasses import dataclass

from eval_interactive.config import Config

logger = logging.getLogger(__name__)

# Patterns that indicate a visible result was delivered
_VISIBLE_RESULT_PATTERNS: list[re.Pattern] = [
    # Specific information delivered
    re.compile(
        r"(article|link|url|case\s*id|reference|ticket|account\s*status|reason)",
        re.IGNORECASE,
    ),
    # Error explanation (bot admitting it couldn't do something)
    re.compile(
        r"(wasn't able to|wasn't able|unable to find|unfortunately|"
        r"couldn't find|could not find|no results|not found)",
        re.IGNORECASE,
    ),
    # Handover / escalation
    re.compile(
        r"(connect you|transfer|escalat|handover|hand over|"
        r"created a case|human agent|live agent)",
        re.IGNORECASE,
    ),
    # Follow-up question that moves conversation forward
    re.compile(
        r"(could you (provide|share|confirm|tell)|"
        r"can you (provide|share|confirm|tell)|"
        r"what is your|may I ask|would you mind)",
        re.IGNORECASE,
    ),
]


@dataclass
class StallResult:
    """Outcome of stall detection across a transcript."""

    detected: bool = False
    turn_index: int = -1
    pattern_matched: str = ""
    failure_tag: str = ""
    # STALL_AFTER_TOOL_INTENT | TOOL_ERROR_NOT_SURFACED | PLACEHOLDER_WITHOUT_FOLLOWUP


class StallDetector:
    """Detects bot stalls: promises action without delivering visible result.

    Scans bot responses for promise patterns and checks whether a
    substantive follow-up occurs within the configured turn window.
    """

    def __init__(self, config: Config):
        """Initialize the stall detector from application config.

        Args:
            config: Application config with stall_detector section.
        """
        self._followup_window = config.stall_detector.followup_window_turns
        self._promise_patterns: list[re.Pattern] = []
        for pat_str in config.stall_detector.promise_patterns:
            try:
                self._promise_patterns.append(re.compile(pat_str, re.IGNORECASE))
            except re.error as exc:
                logger.warning("Invalid promise pattern '%s': %s", pat_str, exc)

    def detect(
        self,
        transcript: list[dict],
        turn_traces: list[dict] | None = None,
    ) -> StallResult:
        """Scan bot responses for promise patterns and cross-reference with results.

        For each bot turn that matches a promise pattern:
        1. Check if a tool_call occurred in this turn or subsequent turns
           within the followup window (via turn_traces if provided).
        2. Check if a visible result appeared within the followup window.
        3. If NO visible result is found, flag as a stall with the
           appropriate failure tag.

        Args:
            transcript: List of turn dicts [{role, message, turn_index}].
            turn_traces: Optional list of TurnTrace-like dicts. Each may
                contain a "tool_calls" key (list of dicts with "status").

        Returns:
            StallResult. If any bot turn is classified as a stall,
            detected=True and the earliest stall is reported.
        """
        bot_turns = [
            (idx, t)
            for idx, t in enumerate(transcript)
            if t.get("role") == "bot"
        ]

        for list_pos, (trans_idx, turn) in enumerate(bot_turns):
            bot_msg = turn.get("message", "")
            matched_pattern = self._match_promise(bot_msg)
            if matched_pattern is None:
                continue

            turn_index = turn.get("turn_index", trans_idx)

            # Determine if a tool_call happened at this turn or within the window
            has_tool_call = False
            has_tool_error = False
            if turn_traces:
                window_traces = self._get_window_traces(
                    turn_traces, turn_index
                )
                for trace in window_traces:
                    calls = trace.get("tool_calls", [])
                    if calls:
                        has_tool_call = True
                        for call in calls:
                            if call.get("status") in (
                                "error",
                                "failed",
                                "failure",
                            ):
                                has_tool_error = True

            # Check for visible result in subsequent bot turns within the window
            has_visible_result = self._has_visible_result_in_window(
                bot_turns, list_pos
            )

            if has_visible_result:
                # Promise was fulfilled -- no stall
                continue

            # Classify the stall failure
            failure_tag = self._classify_failure(
                has_tool_call, has_tool_error
            )

            return StallResult(
                detected=True,
                turn_index=turn_index,
                pattern_matched=matched_pattern,
                failure_tag=failure_tag,
            )

        return StallResult(detected=False)

    def _match_promise(self, text: str) -> str | None:
        """Return the matched pattern string if text contains a promise, else None."""
        for pattern in self._promise_patterns:
            if pattern.search(text):
                return pattern.pattern
        return None

    def _get_window_traces(
        self,
        turn_traces: list[dict],
        start_turn_index: int,
    ) -> list[dict]:
        """Get trace entries for the turn and the followup window."""
        end_turn = start_turn_index + self._followup_window
        results = []
        for trace in turn_traces:
            t_idx = trace.get("turn_index", -1)
            if start_turn_index <= t_idx <= end_turn:
                results.append(trace)
        return results

    def _has_visible_result_in_window(
        self,
        bot_turns: list[tuple[int, dict]],
        promise_list_pos: int,
    ) -> bool:
        """Check if any bot turn in the followup window contains a visible result.

        Looks at the promise turn itself and the next followup_window bot turns.
        """
        end_pos = min(
            promise_list_pos + self._followup_window + 1,
            len(bot_turns),
        )

        # Check the promise turn itself: it might contain both the promise
        # and the result in one message (e.g. "Let me check... I found
        # article XYZ.")
        _, promise_turn = bot_turns[promise_list_pos]
        promise_msg = promise_turn.get("message", "")
        for pattern in _VISIBLE_RESULT_PATTERNS:
            if pattern.search(promise_msg):
                return True

        # Check subsequent bot turns within the window
        for pos in range(promise_list_pos + 1, end_pos):
            _, turn = bot_turns[pos]
            msg = turn.get("message", "")
            for pattern in _VISIBLE_RESULT_PATTERNS:
                if pattern.search(msg):
                    return True

        return False

    @staticmethod
    def _classify_failure(has_tool_call: bool, has_tool_error: bool) -> str:
        """Determine the failure tag based on tool activity."""
        if not has_tool_call:
            return "STALL_AFTER_TOOL_INTENT"
        if has_tool_error:
            return "TOOL_ERROR_NOT_SURFACED"
        return "PLACEHOLDER_WITHOUT_FOLLOWUP"
