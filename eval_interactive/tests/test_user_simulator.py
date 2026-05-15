"""Tests for the UserSimulator module -- focuses on _parse_simulator_response."""

from __future__ import annotations

import pytest

from eval_interactive.simulator.user_simulator import _parse_simulator_response


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
