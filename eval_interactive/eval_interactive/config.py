"""Configuration loading for eval_interactive.

Loads eval_interactive.yaml, resolves ${ENV_VAR} references from environment
variables (using python-dotenv to load .env.local from the project root).
"""

from __future__ import annotations

import logging
import os
import re
from dataclasses import dataclass, field
from pathlib import Path

import yaml
from dotenv import load_dotenv

logger = logging.getLogger(__name__)

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
class JudgeConfig:
    """Connection details for the L3 LLM judge.

    Kept separate from :class:`LLMConfig` (which drives the *user
    simulator*) so the simulated customer and the grader are not forced to
    be the same model on the same provider — sharing one endpoint leaves
    their error correlation unisolated (WS-5 item 3).

    All fields default to empty; :func:`resolve_judge_config` fills any
    empty field from the ``llm`` section so an environment that has not set
    ``JUDGE_*`` keeps working exactly as before.
    """

    base_url: str = ""
    api_key: str = ""
    model: str = ""
    temperature: float | None = None  # None -> inherit llm.temperature


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


# Per-case wall-clock ceiling for a batch run (``batch.timeout_per_session_seconds``).
#
# WAS 120s, which is not a realistic budget for this harness:
#
# * 120s is EXACTLY the session-create read budget on its own
#   (``simulator.agent_client.SESSION_CREATE_READ_TIMEOUT_SECONDS`` = 120.0), so
#   the deadline could fire before the first customer message was ever sent.
# * Each subsequent turn costs one bot request (read ceiling
#   ``agent_client.DEFAULT_READ_TIMEOUT_SECONDS`` = 90.0) plus one simulator LLM
#   call, and 262 of the 488 corpus specs declare ``max_turns: 15``. A session
#   that never exceeded a single documented per-request budget can therefore
#   legitimately run 120 + 15*90 = 1470s before the simulator and L3 judge calls
#   are counted at all.
# * MEASURED: on the qwen-plus simulator / kimi-k2.6 judge provider combination a
#   single session takes 4-9 minutes (240-540s). At 120s a healthy session was
#   truncated as a matter of course — one real baseline arm recorded 4/5 cases as
#   TIMEOUT while the abandoned workers went on to finish normally.
#
# 1800s (30 min) is the smallest round value above the 1470s per-request-budget
# bound, i.e. above every duration a HEALTHY session can reach without some
# individual request already having blown its own timeout. It is still ~3.3x the
# measured 9-minute worst case, so a genuinely hung session is caught.
#
# Raising this is not a substitute for the cancellation fix: see
# ``simulator.session_runner.SessionCancelled`` — the deadline can only request
# cancellation cooperatively, it cannot kill the worker thread.
DEFAULT_TIMEOUT_PER_SESSION_SECONDS = 1800


@dataclass
class BatchConfig:
    # parallel=1 default per M3-Eval close evidence (bad-case suite stability
    # requires sequential runs); opt-in to higher parallel via CLI --parallel.
    parallel: int = 1
    timeout_per_session_seconds: int = DEFAULT_TIMEOUT_PER_SESSION_SECONDS


@dataclass
class ReportConfig:
    output_dir: str = "results/"


@dataclass
class Config:
    bot: BotConfig = field(default_factory=BotConfig)
    llm: LLMConfig = field(default_factory=LLMConfig)
    judge: JudgeConfig = field(default_factory=JudgeConfig)
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

    judge_raw = raw.get("judge", {}) or {}
    judge_temp_raw = judge_raw.get("temperature")
    judge = JudgeConfig(
        base_url=judge_raw.get("base_url", "") or "",
        api_key=judge_raw.get("api_key", "") or "",
        model=judge_raw.get("model", "") or "",
        temperature=(
            float(judge_temp_raw)
            if judge_temp_raw not in (None, "")
            else None
        ),
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
            batch_raw.get(
                "timeout_per_session_seconds",
                DEFAULT_TIMEOUT_PER_SESSION_SECONDS,
            )
        ),
    )

    report_raw = raw.get("report", {})
    report = ReportConfig(output_dir=report_raw.get("output_dir", "results/"))

    return Config(
        bot=bot,
        llm=llm,
        judge=judge,
        simulator=simulator,
        stall_detector=stall_detector,
        batch=batch,
        report=report,
    )


def resolve_judge_config(config: Config) -> JudgeConfig:
    """Return the judge's effective connection details.

    Any field the ``judge`` section leaves empty falls back to the
    corresponding ``llm`` field (``temperature`` falls back to
    ``llm.temperature``, i.e. 0.0 for grading — NOT
    ``llm.simulator_temperature``). Falling back is logged at WARNING with
    the exact field list so a missing ``JUDGE_*`` env var is visible but
    never fatal: an environment that predates this split keeps running with
    the previous single-endpoint behaviour.

    Returns a NEW ``JudgeConfig`` and does not mutate ``config``, so the
    declared-vs-effective distinction stays inspectable.
    """
    judge = config.judge
    fell_back: list[str] = []

    base_url = judge.base_url
    if not base_url:
        base_url = config.llm.base_url
        fell_back.append("base_url")

    api_key = judge.api_key
    if not api_key:
        api_key = config.llm.api_key
        fell_back.append("api_key")

    model = judge.model
    if not model:
        model = config.llm.model
        fell_back.append("model")

    temperature = judge.temperature
    if temperature is None:
        temperature = config.llm.temperature
        fell_back.append("temperature")

    if fell_back:
        logger.warning(
            "judge config incomplete: %s not set (checked JUDGE_BASE_URL / "
            "JUDGE_API_KEY / JUDGE_MODEL); falling back to the simulator's "
            "`llm` section. The L3 judge and the user simulator will share a "
            "provider, so their errors are correlated. Set the JUDGE_* env "
            "vars to isolate them.",
            ", ".join(fell_back),
        )

    return JudgeConfig(
        base_url=base_url,
        api_key=api_key,
        model=model,
        temperature=temperature,
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
