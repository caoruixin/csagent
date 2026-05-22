"""Configuration loading for eval_interactive.

Loads eval_interactive.yaml, resolves ${ENV_VAR} references from environment
variables (using python-dotenv to load .env.local from the project root).
"""

from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from pathlib import Path

import yaml
from dotenv import load_dotenv

# Project root: three levels up from this file
# eval_interactive/eval_interactive/config.py -> project root
_PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
_ENV_LOCAL = _PROJECT_ROOT / ".env.local"
_DEFAULT_CONFIG = Path(__file__).resolve().parent.parent / "eval_interactive.yaml"

_ENV_VAR_PATTERN = re.compile(r"\$\{(\w+)\}")


def _resolve_env_vars(value: str) -> str:
    """Replace ${ENV_VAR} placeholders with actual environment variable values."""

    def _replace(match: re.Match) -> str:
        var_name = match.group(1)
        env_val = os.environ.get(var_name, "")
        return env_val

    return _ENV_VAR_PATTERN.sub(_replace, value)


def _resolve_recursively(obj: object) -> object:
    """Walk a nested dict/list and resolve all string values."""
    if isinstance(obj, str):
        return _resolve_env_vars(obj)
    if isinstance(obj, dict):
        return {k: _resolve_recursively(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_resolve_recursively(item) for item in obj]
    return obj


@dataclass
class BotConfig:
    base_url: str = "http://localhost:8080"


@dataclass
class LLMConfig:
    base_url: str = ""
    api_key: str = ""
    model: str = ""
    temperature: float = 0.0
    simulator_temperature: float = 0.7


@dataclass
class DefaultPersona:
    frustration_level: str = "none"
    verbosity: str = "normal"
    drift_behavior: str = "none"


@dataclass
class SimulatorConfig:
    max_turns: int = 15
    default_persona: DefaultPersona = field(default_factory=DefaultPersona)


@dataclass
class StallDetectorConfig:
    promise_patterns: list[str] = field(default_factory=list)
    followup_window_turns: int = 2


@dataclass
class BatchConfig:
    # parallel=1 default per M3-Eval close evidence (bad-case suite stability
    # requires sequential runs); opt-in to higher parallel via CLI --parallel.
    parallel: int = 1
    timeout_per_session_seconds: int = 120


@dataclass
class ReportConfig:
    output_dir: str = "results/"


@dataclass
class Config:
    bot: BotConfig = field(default_factory=BotConfig)
    llm: LLMConfig = field(default_factory=LLMConfig)
    simulator: SimulatorConfig = field(default_factory=SimulatorConfig)
    stall_detector: StallDetectorConfig = field(default_factory=StallDetectorConfig)
    batch: BatchConfig = field(default_factory=BatchConfig)
    report: ReportConfig = field(default_factory=ReportConfig)


def _build_config(raw: dict) -> Config:
    """Build a typed Config from a raw resolved dict."""
    bot_raw = raw.get("bot", {})
    bot = BotConfig(base_url=bot_raw.get("base_url", "http://localhost:8080"))

    llm_raw = raw.get("llm", {})
    llm = LLMConfig(
        base_url=llm_raw.get("base_url", ""),
        api_key=llm_raw.get("api_key", ""),
        model=llm_raw.get("model", ""),
        temperature=float(llm_raw.get("temperature", 0.0)),
        simulator_temperature=float(llm_raw.get("simulator_temperature", 0.7)),
    )

    sim_raw = raw.get("simulator", {})
    persona_raw = sim_raw.get("default_persona", {})
    default_persona = DefaultPersona(
        frustration_level=persona_raw.get("frustration_level", "none"),
        verbosity=persona_raw.get("verbosity", "normal"),
        drift_behavior=persona_raw.get("drift_behavior", "none"),
    )
    simulator = SimulatorConfig(
        max_turns=int(sim_raw.get("max_turns", 15)),
        default_persona=default_persona,
    )

    stall_raw = raw.get("stall_detector", {})
    stall_detector = StallDetectorConfig(
        promise_patterns=stall_raw.get("promise_patterns", []),
        followup_window_turns=int(stall_raw.get("followup_window_turns", 2)),
    )

    batch_raw = raw.get("batch", {})
    batch = BatchConfig(
        parallel=int(batch_raw.get("parallel", 1)),
        timeout_per_session_seconds=int(
            batch_raw.get("timeout_per_session_seconds", 120)
        ),
    )

    report_raw = raw.get("report", {})
    report = ReportConfig(output_dir=report_raw.get("output_dir", "results/"))

    return Config(
        bot=bot,
        llm=llm,
        simulator=simulator,
        stall_detector=stall_detector,
        batch=batch,
        report=report,
    )


def load_config(config_path: str | Path | None = None) -> Config:
    """Load configuration from YAML, resolving env var placeholders.

    Args:
        config_path: Path to the YAML config file. Defaults to
                     eval_interactive.yaml next to pyproject.toml.

    Returns:
        A fully-resolved Config dataclass.
    """
    # Load .env.local from project root
    if _ENV_LOCAL.exists():
        load_dotenv(_ENV_LOCAL, override=False)

    if config_path is None:
        config_path = _DEFAULT_CONFIG

    config_path = Path(config_path)
    if not config_path.exists():
        raise FileNotFoundError(f"Config file not found: {config_path}")

    with open(config_path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    resolved = _resolve_recursively(raw)
    return _build_config(resolved)
