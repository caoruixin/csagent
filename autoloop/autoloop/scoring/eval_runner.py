"""Subprocess wrapper around `eval-interactive run` for the v1 fitness
suite.

S-Auto-2 deliverable. Invokes the existing `eval-interactive run` CLI
exactly once per fitness suite (bad_cases, anchor_outcome, shadow);
captures results.json + stderr tail; surfaces timeouts and non-zero
exit codes for the loop orchestrator (S-Auto-3) to act on.

Hard fences enforced structurally in this module:

- NEVER invokes `mvn`, `spring-boot:run`, `git`, or any subprocess
  other than `uv run eval-interactive run`. (Verified by a grep test
  in `tests/test_eval_runner.py`.)
- NEVER mutates `eval_interactive/eval_interactive/**` or any
  case_spec — it CONSUMES `eval-interactive run` as a black box.
- NEVER introduces a new CLI flag on `eval-interactive run`. If a
  future S-Auto-2.X needs a new flag, that change belongs in
  `eval_interactive/` and is OUT of S-Auto-2 scope; STOP-and-surface
  per the §"Hard fences" clause of the sub-sprint contract.
"""

from __future__ import annotations

import os
import subprocess
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal


_REPO_ROOT = Path(__file__).resolve().parents[3]


# --- Exceptions ------------------------------------------------------


class EvalRunnerTimeoutError(Exception):
    """Raised when a suite exceeds `timeout_seconds`."""

    def __init__(self, suite: str, elapsed: float):
        super().__init__(f"suite '{suite}' timed out after {elapsed:.1f}s")
        self.suite = suite
        self.elapsed = elapsed


# --- Dataclasses -----------------------------------------------------


@dataclass
class SuiteRunSpec:
    """One suite's run plan. The v1 fitness suite is composed of
    exactly three of these (bad_cases / anchor_outcome / shadow).
    """

    name: Literal["bad_cases", "anchor_outcome", "shadow"]
    path: Path
    parallel: int


@dataclass
class SuiteRunResult:
    """The outcome of running one suite via `eval-interactive run`."""

    suite_name: str
    results_dir: Path
    results_json: Path
    elapsed_seconds: float
    exit_code: int
    error_tail: str | None = None


# --- Public API ------------------------------------------------------


def run_suite(
    spec: SuiteRunSpec,
    *,
    results_root: Path,
    config: dict[str, Any],
    timeout_seconds: int = 1800,
) -> SuiteRunResult:
    """Run one suite, writing results to `<results_root>/<suite>/`.

    Subprocess contract (verbatim from S-Auto-2 sub-sprint contract):
        cd <repo_root>/eval_interactive && \\
        uv run eval-interactive run --path <spec.path> \\
            --parallel <spec.parallel> \\
            --output-dir <results_root>/<spec.name>/

    `cwd` is `repo_root/eval_interactive`; the environment is
    inherited unchanged (LLM provider config flows through
    `eval_interactive/.env`).
    """
    suite_out_dir = Path(results_root) / spec.name
    suite_out_dir.mkdir(parents=True, exist_ok=True)

    cwd = _REPO_ROOT / "eval_interactive"
    cmd = [
        "uv",
        "run",
        "eval-interactive",
        "run",
        "--path",
        str(spec.path),
        "--parallel",
        str(spec.parallel),
        "--output-dir",
        str(suite_out_dir.resolve()),
    ]

    start = time.monotonic()
    try:
        proc = subprocess.run(
            cmd,
            cwd=str(cwd),
            env=os.environ.copy(),
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
    except subprocess.TimeoutExpired as e:
        elapsed = time.monotonic() - start
        raise EvalRunnerTimeoutError(spec.name, elapsed) from e

    elapsed = time.monotonic() - start
    error_tail: str | None = None
    if proc.returncode != 0 and proc.stderr:
        error_tail = "\n".join((proc.stderr or "").splitlines()[-50:])

    return SuiteRunResult(
        suite_name=spec.name,
        results_dir=suite_out_dir,
        results_json=suite_out_dir / "results.json",
        elapsed_seconds=elapsed,
        exit_code=proc.returncode,
        error_tail=error_tail,
    )


def run_v1_fitness_suite(
    *,
    results_root: Path,
    config: dict[str, Any],
) -> dict[str, SuiteRunResult]:
    """Run the 3 v1 fitness suites (bad_cases + anchor_outcome + shadow).

    Sequential by default (config.fitness.parallel_suites=False); the
    parallel knob is reserved for M-Auto-2+.

    Each suite is invoked with its own --parallel value from
    `config.fitness.suites[].parallel`. If a suite times out, the
    exception propagates immediately and downstream suites are NOT
    run — partial fitness evidence is worse than no fitness evidence
    in the loop's lexicographic ordering.
    """
    fitness_cfg = (config or {}).get("fitness", {}) or {}
    suites_cfg = fitness_cfg.get("suites") or []
    timeout = int(fitness_cfg.get("eval_suite_timeout_seconds", 1800))

    if fitness_cfg.get("parallel_suites"):
        raise NotImplementedError(
            "parallel_suites=True is reserved for M-Auto-2; v1 runs sequentially"
        )

    results: dict[str, SuiteRunResult] = {}
    for entry in suites_cfg:
        if not isinstance(entry, dict):
            continue
        spec = SuiteRunSpec(
            name=entry["name"],
            path=Path(entry["path"]),
            parallel=int(entry.get("parallel", 4)),
        )
        results[spec.name] = run_suite(
            spec,
            results_root=results_root,
            config=config,
            timeout_seconds=timeout,
        )
    return results
