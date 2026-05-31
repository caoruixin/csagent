"""Subprocess wrapper around `eval-interactive run` for the v1 fitness
suite.

S-Auto-2 deliverable; revised at S-Auto-7 (M-Auto-1B) per Blocker B
fix path (b): the original S-Auto-2 contract assumed a
`--output-dir` flag on `eval-interactive run` that does not exist
in the eval-interactive CLI. Rather than introduce a new CLI flag
(fence #14 violation), this module now adapts to eval-interactive's
native output convention: the harness writes to
`eval_interactive/results/<YYYYMMDD-HHMMSS>/` (UTC timestamp). This
module snapshots that directory before invoking the subprocess,
identifies the newly-created run dir after, and symlinks
`<results_root>/<suite_name>` -> `<eval_interactive/results>/<ts>/`
so downstream consumers (baseline_loader, tier_evaluator, gaming)
continue to see the historical `<results_root>/<suite>/results.json`
contract transparently. The eval-interactive CLI is byte-identical
to its pre-S-Auto-7 form (fix path (b), not (a)).

Hard fences enforced structurally in this module:

- NEVER invokes `mvn`, `spring-boot:run`, `git`, or any subprocess
  other than `uv run eval-interactive run`. (Verified by a grep test
  in `tests/test_eval_runner.py`.)
- NEVER mutates `eval_interactive/eval_interactive/**` or any
  case_spec — it CONSUMES `eval-interactive run` as a black box.
- NEVER introduces a new CLI flag on `eval-interactive run`. If a
  future S-Auto-X needs a new flag, that change belongs in
  `eval_interactive/` and is OUT of substrate scope; STOP-and-surface
  per the §"Hard fences" clause of the sub-sprint contract.
"""

from __future__ import annotations

import os
import shutil
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
    """Run one suite; stage results under `<results_root>/<suite>/`.

    Subprocess contract (S-Auto-7 Blocker B fix path (b); supersedes
    the S-Auto-2 --output-dir contract):
        cd <repo_root>/eval_interactive && \\
        uv run eval-interactive run --path <spec.path> \\
            --parallel <spec.parallel>

    eval-interactive writes to its config-default
    `eval_interactive/results/<YYYYMMDD-HHMMSS>/` (UTC). This
    function snapshots that directory before invoking the
    subprocess and identifies the new run dir afterwards via
    set-difference + mtime tiebreaker, then symlinks
    `<results_root>/<spec.name>` to it. Downstream consumers see
    the `<results_root>/<suite>/results.json` shape transparently
    via the symlink.

    `cwd` is `repo_root/eval_interactive`; the environment is
    inherited unchanged (LLM provider config flows through
    `eval_interactive/.env`).
    """
    results_root = Path(results_root)
    results_root.mkdir(parents=True, exist_ok=True)
    suite_link = results_root / spec.name

    cwd = _REPO_ROOT / "eval_interactive"
    # spec.path is repo-root-relative (per config.fitness.suites[].path), but
    # eval-interactive runs with cwd=eval_interactive — a relative path would
    # double the `eval_interactive/` segment and FileNotFoundError. Resolve to
    # an absolute path so it is cwd-independent (OQ-S65.5 fix).
    resolved_path = spec.path if spec.path.is_absolute() else (_REPO_ROOT / spec.path)
    cmd = [
        "uv",
        "run",
        "eval-interactive",
        "run",
        "--path",
        str(resolved_path.resolve()),
        "--parallel",
        str(spec.parallel),
    ]

    # Ensure CSAGENT_BACKEND_URL is set for the eval-interactive
    # subprocess. The autoloop loop orchestrator (Sprint 56 /
    # S-Auto-3) sets this to the alt-port the applier brought up;
    # standalone callers fall back to the regular port. This bridges
    # the env-var indirection in `eval_interactive/eval_interactive.yaml`
    # without changing the eval_runner signature (per S-Auto-3 contract).
    sub_env = os.environ.copy()
    if "CSAGENT_BACKEND_URL" not in sub_env:
        sub_env["CSAGENT_BACKEND_URL"] = "http://localhost:8080"

    # Snapshot eval-interactive's results/ directory listing BEFORE
    # the subprocess so we can identify the newly-created timestamp
    # dir afterwards. Done lazily — if the dir does not yet exist
    # (first-ever run), `before_ts_dirs` is empty.
    ei_results_dir = cwd / "results"
    before_ts_dirs: set[Path] = (
        {p for p in ei_results_dir.iterdir() if p.is_dir()}
        if ei_results_dir.exists()
        else set()
    )

    start = time.monotonic()
    try:
        proc = subprocess.run(
            cmd,
            cwd=str(cwd),
            env=sub_env,
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
    except subprocess.TimeoutExpired as e:
        elapsed = time.monotonic() - start
        raise EvalRunnerTimeoutError(spec.name, elapsed) from e

    elapsed = time.monotonic() - start

    new_ts_dir = _locate_new_results_dir(ei_results_dir, before_ts_dirs)

    error_tail: str | None = None
    if proc.returncode != 0 and proc.stderr:
        error_tail = "\n".join((proc.stderr or "").splitlines()[-50:])

    # Stage <results_root>/<suite>/ as a symlink to the eval-
    # interactive run dir so downstream consumers see the historical
    # contract. If the subprocess failed before writing a run dir,
    # `new_ts_dir` is None and the link is not created; callers can
    # detect via `results_json.exists()` or `exit_code != 0`.
    if new_ts_dir is not None:
        if suite_link.is_symlink() or suite_link.is_file():
            suite_link.unlink()
        elif suite_link.exists():
            # Pre-existing real directory at this path is unexpected
            # under normal use (the per-iter results_root is created
            # fresh by the orchestrator), but be safe.
            shutil.rmtree(suite_link)
        suite_link.symlink_to(new_ts_dir.resolve(), target_is_directory=True)

    return SuiteRunResult(
        suite_name=spec.name,
        results_dir=suite_link,
        results_json=suite_link / "results.json",
        elapsed_seconds=elapsed,
        exit_code=proc.returncode,
        error_tail=error_tail,
    )


def _locate_new_results_dir(
    ei_results_dir: Path,
    before_ts_dirs: set[Path],
) -> Path | None:
    """Find the eval-interactive run dir created by the just-completed
    subprocess.

    Returns the newest-by-mtime directory under `ei_results_dir` that
    did NOT exist in `before_ts_dirs`. If no new directory appeared
    (e.g. subprocess failed before writing), returns None. If multiple
    new directories appeared (e.g. concurrent runs — not expected
    under v1 sequential execution but defensive), returns the newest.
    """
    if not ei_results_dir.exists():
        return None
    after = {p for p in ei_results_dir.iterdir() if p.is_dir()}
    new_dirs = sorted(
        after - before_ts_dirs,
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    )
    return new_dirs[0] if new_dirs else None


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
