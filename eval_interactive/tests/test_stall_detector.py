"""Tests for the StallDetector module."""

from __future__ import annotations

import pytest

from eval_interactive.config import (
    Config,
    StallDetectorConfig,
    BotConfig,
    LLMConfig,
    SimulatorConfig,
    BatchConfig,
    ReportConfig,
)
from eval_interactive.scoring.stall_detector import StallDetector, StallResult


def _make_config(
    patterns: list[str] | None = None,
    followup_window: int = 2,
) -> Config:
    """Build a minimal Config with stall detector settings."""
    if patterns is None:
        patterns = [
            r"let me (check|look|find|verify|search)",
            r"i('m| am) (checking|looking|searching|investigating)",
            r"one moment",
            r"i'll (look into|check|investigate|find)",
            r"thanks for your patience",
            r"just a moment",
        ]
    return Config(
        stall_detector=StallDetectorConfig(
            promise_patterns=patterns,
            followup_window_turns=followup_window,
        ),
    )


class TestStallDetectorCleanTranscripts:
    """Transcripts that should NOT trigger stall detection."""

    def test_empty_transcript(self):
        detector = StallDetector(_make_config())
        result = detector.detect([])
        assert result.detected is False

    def test_no_bot_turns(self):
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "user", "message": "Hello"},
            {"role": "user", "message": "Anyone there?"},
        ]
        result = detector.detect(transcript)
        assert result.detected is False

    def test_no_promise_patterns(self):
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "How can I help you?"},
            {"role": "user", "message": "My ad is not showing"},
            {"role": "bot", "message": "I found an article about ad visibility."},
        ]
        result = detector.detect(transcript)
        assert result.detected is False

    def test_promise_followed_by_article_result(self):
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "How can I help you?"},
            {"role": "user", "message": "My ad is not showing"},
            {"role": "bot", "message": "Let me check that for you."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Here is an article: https://help.gumtree.com/ads"},
        ]
        result = detector.detect(transcript)
        assert result.detected is False

    def test_promise_with_inline_result(self):
        """Promise and visible result in the SAME bot message."""
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me check... I found an article about this."},
        ]
        result = detector.detect(transcript)
        assert result.detected is False

    def test_promise_followed_by_escalation(self):
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me look into this."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "I'll connect you with a human agent."},
        ]
        result = detector.detect(transcript)
        assert result.detected is False

    def test_promise_followed_by_unable_to_find(self):
        """Error explanation counts as a visible result."""
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me search for that."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Unfortunately, I couldn't find any matching articles."},
        ]
        result = detector.detect(transcript)
        assert result.detected is False

    def test_promise_followed_by_clarifying_question(self):
        """A follow-up question moves conversation forward -- no stall."""
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me check that for you."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Could you provide your ad ID?"},
        ]
        result = detector.detect(transcript)
        assert result.detected is False


class TestStallDetectorDetectsStalls:
    """Transcripts that SHOULD trigger stall detection."""

    def test_promise_without_followup(self):
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "How can I help you?"},
            {"role": "user", "message": "My ad is not showing"},
            {"role": "bot", "message": "Let me check that for you."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Is there anything else I can help with?"},
        ]
        result = detector.detect(transcript)
        assert result.detected is True
        assert "let me" in result.pattern_matched.lower()

    def test_stall_returns_earliest_index(self):
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me check.", "turn_index": 1},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Just a moment.", "turn_index": 2},
            {"role": "user", "message": "still waiting"},
            {"role": "bot", "message": "Thanks for waiting!", "turn_index": 3},
        ]
        result = detector.detect(transcript)
        assert result.detected is True
        # Should report the earliest stall
        assert result.turn_index == 1

    def test_one_moment_stall(self):
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "One moment please."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Is there anything else I can help with?"},
        ]
        result = detector.detect(transcript)
        assert result.detected is True
        assert "one moment" in result.pattern_matched.lower()

    def test_stall_failure_tag_no_tool_call(self):
        """Without tool call info, failure tag should be STALL_AFTER_TOOL_INTENT."""
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me check that."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Is there anything else?"},
        ]
        result = detector.detect(transcript)
        assert result.detected is True
        assert result.failure_tag == "STALL_AFTER_TOOL_INTENT"

    def test_stall_with_tool_error_not_surfaced(self):
        """Tool call happened but errored, and bot did not explain."""
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me check that.", "turn_index": 1},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Is there anything else?", "turn_index": 2},
        ]
        turn_traces = [
            {"turn_index": 1, "tool_calls": [{"tool_name": "search_knowledge", "status": "error"}]},
            {"turn_index": 2, "tool_calls": []},
        ]
        result = detector.detect(transcript, turn_traces)
        assert result.detected is True
        assert result.failure_tag == "TOOL_ERROR_NOT_SURFACED"

    def test_stall_placeholder_without_followup(self):
        """Tool call succeeded but bot didn't show the result."""
        detector = StallDetector(_make_config())
        transcript = [
            {"role": "bot", "message": "Let me check that.", "turn_index": 1},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Is there anything else?", "turn_index": 2},
        ]
        turn_traces = [
            {"turn_index": 1, "tool_calls": [{"tool_name": "search_knowledge", "status": "success"}]},
            {"turn_index": 2, "tool_calls": []},
        ]
        result = detector.detect(transcript, turn_traces)
        assert result.detected is True
        assert result.failure_tag == "PLACEHOLDER_WITHOUT_FOLLOWUP"


class TestStallDetectorEdgeCases:
    """Edge cases and boundary conditions."""

    def test_invalid_regex_pattern_skipped(self):
        """Invalid patterns should be skipped with a warning, not crash."""
        config = _make_config(patterns=["[invalid(regex", "let me check"])
        detector = StallDetector(config)
        assert len(detector._promise_patterns) == 1  # only valid one loaded

    def test_empty_patterns_list(self):
        config = _make_config(patterns=[])
        detector = StallDetector(config)
        transcript = [
            {"role": "bot", "message": "Let me check that for you."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Is there anything else?"},
        ]
        result = detector.detect(transcript)
        assert result.detected is False  # no patterns to match

    def test_followup_window_zero(self):
        """With window=0, promise turn itself must contain the result."""
        config = _make_config(followup_window=0)
        detector = StallDetector(config)
        transcript = [
            {"role": "bot", "message": "Let me check that."},
            {"role": "user", "message": "ok"},
            {"role": "bot", "message": "Here is the article you need."},
        ]
        result = detector.detect(transcript)
        # Window=0 means only the promise turn is checked.
        # The next turn has the article but is outside window.
        assert result.detected is True

    def test_stall_result_default_values(self):
        result = StallResult()
        assert result.detected is False
        assert result.turn_index == -1
        assert result.pattern_matched == ""
        assert result.failure_tag == ""
