"""Tests for the config loading module."""

from __future__ import annotations

import logging
import os
import tempfile
from pathlib import Path
from types import SimpleNamespace

import pytest
import yaml

from eval_interactive.config import (
    Config,
    JudgeConfig,
    LLMConfig,
    load_config,
    resolve_judge_config,
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
        assert config.batch.parallel == 1

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


# ---------------------------------------------------------------------------
# WS-5 item 3: judge config is separate from the simulator's `llm` section
# ---------------------------------------------------------------------------


class TestJudgeConfigSeparation:
    """The L3 judge reads its own section, with a fallback to ``llm``.

    NOTE (iteration_governance.md §5.7): these are WIRING tests. They prove
    which endpoint/model the judge is constructed with; they say nothing
    about whether decoupling the provider changes any score. That question
    needs a real-LLM rerun.
    """

    def test_judge_section_parsed_from_env(self, tmp_path: Path, monkeypatch):
        monkeypatch.setenv("JUDGE_BASE_URL", "https://judge.example/v1")
        monkeypatch.setenv("JUDGE_API_KEY", "sk-judge")
        monkeypatch.setenv("JUDGE_MODEL", "judge-model")
        config_data = {
            "llm": {
                "base_url": "https://sim.example/v1",
                "api_key": "sk-sim",
                "model": "sim-model",
                "temperature": 0.0,
            },
            "judge": {
                "base_url": "${JUDGE_BASE_URL}",
                "api_key": "${JUDGE_API_KEY}",
                "model": "${JUDGE_MODEL}",
                "temperature": 0.0,
            },
        }
        config_file = tmp_path / "judge_config.yaml"
        with open(config_file, "w") as f:
            yaml.dump(config_data, f)

        config = load_config(config_file)
        effective = resolve_judge_config(config)
        assert effective.model == "judge-model"
        assert effective.base_url == "https://judge.example/v1"
        assert effective.api_key == "sk-judge"
        # ...and the simulator is untouched by the split.
        assert config.llm.model == "sim-model"

    def test_missing_judge_env_falls_back_to_llm_with_warning(self, caplog):
        config = Config(
            llm=LLMConfig(
                base_url="https://sim.example/v1",
                api_key="sk-sim",
                model="sim-model",
                temperature=0.25,
                simulator_temperature=0.7,
            ),
            judge=JudgeConfig(),
        )
        with caplog.at_level(logging.WARNING, logger="eval_interactive.config"):
            effective = resolve_judge_config(config)

        assert effective.model == "sim-model"
        assert effective.base_url == "https://sim.example/v1"
        assert effective.api_key == "sk-sim"
        # temperature falls back to llm.temperature (grading), NOT
        # llm.simulator_temperature.
        assert effective.temperature == 0.25
        assert any(
            "judge config incomplete" in rec.message for rec in caplog.records
        )

    def test_partial_judge_config_falls_back_per_field(self, caplog):
        config = Config(
            llm=LLMConfig(
                base_url="https://sim.example/v1",
                api_key="sk-sim",
                model="sim-model",
                temperature=0.0,
            ),
            judge=JudgeConfig(model="judge-model"),
        )
        with caplog.at_level(logging.WARNING, logger="eval_interactive.config"):
            effective = resolve_judge_config(config)

        assert effective.model == "judge-model"          # declared
        assert effective.base_url == "https://sim.example/v1"  # fallback
        warnings = [r.getMessage() for r in caplog.records if "judge config" in r.getMessage()]
        assert warnings, "a partial judge config must still warn"
        # The warning names only the fields that actually fell back.
        fell_back = warnings[0].split("incomplete: ")[1].split(" not set")[0]
        assert "base_url" in fell_back
        assert "api_key" in fell_back
        assert "model" not in fell_back

    def test_resolve_does_not_mutate_declared_config(self):
        config = Config(
            llm=LLMConfig(base_url="http://sim", api_key="k", model="m"),
            judge=JudgeConfig(),
        )
        resolve_judge_config(config)
        assert config.judge.model == ""  # declared state stays inspectable

    def test_llm_judge_uses_judge_endpoint(self):
        """The judge client is built from the judge section, not ``llm``."""
        from eval_interactive.scoring.llm_judge import LlmJudge

        config = Config(
            llm=LLMConfig(
                base_url="https://sim.example/v1",
                api_key="sk-sim",
                model="sim-model",
                temperature=0.0,
            ),
            judge=JudgeConfig(
                base_url="https://judge.example/v1",
                api_key="sk-judge",
                model="judge-model",
                temperature=0.0,
            ),
        )
        judge = LlmJudge(config)
        assert judge._model == "judge-model"
        assert str(judge._client.base_url).rstrip("/") == "https://judge.example/v1"

    def test_shipped_config_wires_three_distinct_providers(self):
        """The repo's own eval_interactive.yaml declares a `judge:` section."""
        shipped = Path(__file__).resolve().parent.parent / "eval_interactive.yaml"
        raw = yaml.safe_load(shipped.read_text(encoding="utf-8"))
        assert "judge" in raw, "eval_interactive.yaml must declare a judge section"
        assert raw["judge"]["base_url"] == "${JUDGE_BASE_URL}"
        assert raw["judge"]["api_key"] == "${JUDGE_API_KEY}"
        assert raw["judge"]["model"] == "${JUDGE_MODEL}"


class TestJudgeTemperatureFallback:
    """A provider that pins its own temperature must not silently blank L3.

    Observed live on moonshot ``kimi-k2.6``: every judged dimension 400'd on
    ``temperature=0.0`` and fell through to ``_DEFAULT_SCORE``, so the whole
    L3 layer degraded to a constant with only a log line to show for it.
    """

    def test_detector_matches_a_400_naming_temperature(self):
        from eval_interactive.scoring.llm_judge import _is_temperature_rejection

        class _Err(Exception):
            status_code = 400

        exc = _Err(
            "Error code: 400 - {'error': {'message': 'invalid temperature: "
            "only 1 is allowed for this model'}}"
        )
        assert _is_temperature_rejection(exc) is True

    def test_detector_ignores_other_400s_and_other_statuses(self):
        from eval_interactive.scoring.llm_judge import _is_temperature_rejection

        class _Err(Exception):
            def __init__(self, msg, status):
                super().__init__(msg)
                self.status_code = status

        # a 400 that is NOT about temperature must keep the normal path
        assert _is_temperature_rejection(_Err("model not found", 400)) is False
        # a 429 mentioning temperature must not be swallowed either
        assert _is_temperature_rejection(_Err("temperature", 429)) is False
        assert _is_temperature_rejection(_Err("boom", None)) is False

    def test_judge_reissues_without_temperature_and_keeps_the_score(self):
        from eval_interactive.scoring import llm_judge as lj
        from eval_interactive.scoring.llm_judge import LlmJudge

        lj._TEMPERATURE_PINNED_MODELS.discard("pinned-temp-model")

        class _Rejected(Exception):
            status_code = 400

        calls: list[dict] = []

        class _Completions:
            def create(self, **kwargs):
                calls.append(kwargs)
                if "temperature" in kwargs:
                    raise _Rejected("invalid temperature: only 1 is allowed")
                return SimpleNamespace(
                    choices=[SimpleNamespace(message=SimpleNamespace(
                        content='{"score": 5, "reasoning": "ok"}'
                    ))]
                )

        judge = LlmJudge(
            Config(
                llm=LLMConfig(base_url="http://x", api_key="k", model="m"),
                judge=JudgeConfig(
                    base_url="http://judge", api_key="k", model="pinned-temp-model",
                    temperature=0.0,
                ),
            )
        )
        judge._client = SimpleNamespace(chat=SimpleNamespace(completions=_Completions()))

        result = judge._call_llm("relevance", "prompt")
        assert result.score == 5.0          # NOT the 3.0 default
        assert judge._temperature_unsupported is True
        assert "temperature" in calls[0]     # first attempt sent it
        assert "temperature" not in calls[1]  # re-issue dropped it

        # A later dimension in the same run skips the doomed first attempt.
        calls.clear()
        judge._call_llm("groundedness", "prompt")
        assert "temperature" not in calls[0]
        assert len(calls) == 1

    def test_pinned_model_is_memoized_across_judge_instances(self):
        """A fresh LlmJudge for the next case must not re-probe the 400."""
        from eval_interactive.scoring import llm_judge as lj
        from eval_interactive.scoring.llm_judge import LlmJudge

        lj._TEMPERATURE_PINNED_MODELS.add("already-known-pinned")
        try:
            judge = LlmJudge(
                Config(
                    llm=LLMConfig(base_url="http://x", api_key="k", model="m"),
                    judge=JudgeConfig(
                        base_url="http://judge", api_key="k",
                        model="already-known-pinned", temperature=0.0,
                    ),
                )
            )
            assert judge._temperature_unsupported is True
        finally:
            lj._TEMPERATURE_PINNED_MODELS.discard("already-known-pinned")
