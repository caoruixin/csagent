"""Tests for the config loading module."""

from __future__ import annotations

import os
import tempfile
from pathlib import Path

import pytest
import yaml

from eval_interactive.config import (
    Config,
    load_config,
    _resolve_env_vars,
    _resolve_recursively,
)


class TestResolveEnvVars:
    def test_no_vars(self):
        assert _resolve_env_vars("hello world") == "hello world"

    def test_single_var(self, monkeypatch):
        monkeypatch.setenv("MY_TEST_VAR", "resolved_value")
        assert _resolve_env_vars("${MY_TEST_VAR}") == "resolved_value"

    def test_multiple_vars(self, monkeypatch):
        monkeypatch.setenv("HOST", "localhost")
        monkeypatch.setenv("PORT", "8080")
        result = _resolve_env_vars("http://${HOST}:${PORT}")
        assert result == "http://localhost:8080"

    def test_missing_var_resolves_to_empty(self):
        # Ensure the var definitely doesn't exist
        var_name = "DEFINITELY_NOT_SET_12345"
        os.environ.pop(var_name, None)
        result = _resolve_env_vars(f"${{{var_name}}}")
        assert result == ""


class TestResolveRecursively:
    def test_nested_dict(self, monkeypatch):
        monkeypatch.setenv("TEST_KEY", "val")
        obj = {"a": {"b": "${TEST_KEY}"}}
        result = _resolve_recursively(obj)
        assert result == {"a": {"b": "val"}}

    def test_list(self, monkeypatch):
        monkeypatch.setenv("TEST_KEY", "val")
        obj = ["${TEST_KEY}", "plain"]
        result = _resolve_recursively(obj)
        assert result == ["val", "plain"]

    def test_non_string_passthrough(self):
        assert _resolve_recursively(42) == 42
        assert _resolve_recursively(True) is True
        assert _resolve_recursively(None) is None


class TestLoadConfig:
    def test_load_from_custom_path(self, tmp_path: Path):
        config_data = {
            "bot": {"base_url": "http://test:9090"},
            "llm": {
                "base_url": "https://api.example.com",
                "api_key": "test-key",
                "model": "test-model",
            },
            "simulator": {"max_turns": 20},
            "stall_detector": {
                "promise_patterns": ["let me check"],
                "followup_window_turns": 3,
            },
            "batch": {"parallel": 2, "timeout_per_session_seconds": 60},
            "report": {"output_dir": "output/"},
        }
        config_file = tmp_path / "test_config.yaml"
        with open(config_file, "w") as f:
            yaml.dump(config_data, f)

        config = load_config(config_file)
        assert config.bot.base_url == "http://test:9090"
        assert config.llm.model == "test-model"
        assert config.simulator.max_turns == 20
        assert config.batch.parallel == 2
        assert config.stall_detector.followup_window_turns == 3

    def test_missing_config_raises(self):
        with pytest.raises(FileNotFoundError):
            load_config("/nonexistent/config.yaml")

    def test_defaults_for_missing_sections(self, tmp_path: Path):
        config_file = tmp_path / "minimal.yaml"
        with open(config_file, "w") as f:
            yaml.dump({"bot": {"base_url": "http://bot:80"}}, f)

        config = load_config(config_file)
        assert config.bot.base_url == "http://bot:80"
        assert config.llm.api_key == ""
        assert config.simulator.max_turns == 15
        assert config.batch.parallel == 5

    def test_env_var_resolution_in_config(self, tmp_path: Path, monkeypatch):
        monkeypatch.setenv("TEST_API_KEY", "sk-secret")
        config_data = {
            "llm": {"api_key": "${TEST_API_KEY}"},
        }
        config_file = tmp_path / "env_config.yaml"
        with open(config_file, "w") as f:
            yaml.dump(config_data, f)

        config = load_config(config_file)
        assert config.llm.api_key == "sk-secret"
