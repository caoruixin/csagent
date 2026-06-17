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

import json
import os
import shutil
import subprocess
import time
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Literal

from .aggregate import (
    INVALID_INFRA_ERROR,
    INVALID_PROVIDER_MIXED,
    MIN_VALID_ATTEMPTS_DEFAULT,
    AttemptRecord,
    aggregate_case,
    majority_bool,
)


_REPO_ROOT = Path(__file__).resolve().parents[3]

# Raw escalation reasons the runtime coerces on a transport/deadline give-up.
# SOURCE OF TRUTH: autoloop.loop._INFRA_ESCALATION_REASONS — mirrored here
# (not imported) to avoid an import cycle (loop imports eval_runner at module
# load). Keep in sync; both demote infra-degraded evidence rather than scoring
# it as a fitness signal. Used per-attempt so a degraded draw is dropped from
# the majority vote instead of double-counted as a stable regression.
_INFRA_ESCALATION_REASONS = frozenset({"service_degraded", "runtime_error_threshold"})
_INFRA_FAILURE_TAG_SUBSTRINGS = ("ReadTimeout", "Timeout", "Deadline", "service_degraded")


# --- Exceptions ------------------------------------------------------


class EvalRunnerTimeoutError(Exception):
    """Raised when a suite exceeds `timeout_seconds`."""

    def __init__(self, suite: str, elapsed: float):
        super().__init__(f"suite '{suite}' timed out after {elapsed:.1f}s")
        self.suite = suite
        self.elapsed = elapsed


class EvalRunnerConfigError(Exception):
    """Raised when the k-of-n majority path cannot run because a required
    fitness config value is missing — currently the configured primary
    model name for provider-comparability anchoring (S-Auto-17 / OQ-S72.1).

    Fail-safe by design: provider comparability MUST anchor on the
    configured primary, never on the empirical modal chat model (a
    whole-run fallback would otherwise be silently scored as all-primary).
    When the anchor cannot be sourced the run fails loudly rather than
    degrading to the unsafe modal heuristic.
    """


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
    """The outcome of running one suite via `eval-interactive run`.

    `attempts` is the S-Auto-16 per-attempt list: at the committed
    `samples_per_case=1` it is None (the single-run primitive's result is
    returned verbatim — byte-identical to pre-sprint). At `n>1` it carries
    the underlying per-attempt `SuiteRunResult`s whose per-case pass/fail
    draws were aggregated by majority into `results_json`. It is an
    additive field; downstream consumers (loop, gaming, baseline_loader)
    that read `results_json` / `exit_code` are unaffected by its presence.
    """

    suite_name: str
    results_dir: Path
    results_json: Path
    elapsed_seconds: float
    exit_code: int
    error_tail: str | None = None
    attempts: list["SuiteRunResult"] | None = None


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

    S-Auto-16 k-of-n: when `config.fitness.samples_per_case > 1` each
    suite is run `n` times and the per-case pass/fail signal is collapsed
    to a MAJORITY vote over provider-comparable attempts (see
    `aggregate.py`). At the COMMITTED default `samples_per_case=1` the
    function takes the single-pass path verbatim — byte-identical to the
    pre-sprint behaviour (same per-suite symlink staging, same return
    shape, no aggregated file, no injected fields). The `n>1` path is
    exercised by tests and the §6.1 variance-measurement run only; the
    live loop stays inert until the baseline re-bless (S-Auto-17).

    Each suite is invoked with its own --parallel value from
    `config.fitness.suites[].parallel`. If a suite times out, the
    exception propagates immediately and downstream suites are NOT
    run — partial fitness evidence is worse than no fitness evidence
    in the loop's lexicographic ordering.
    """
    fitness_cfg = (config or {}).get("fitness", {}) or {}

    if fitness_cfg.get("parallel_suites"):
        raise NotImplementedError(
            "parallel_suites=True is reserved for M-Auto-2; v1 runs sequentially"
        )

    try:
        n = int(fitness_cfg.get("samples_per_case", 1))
    except (TypeError, ValueError):
        n = 1
    if n < 1:
        n = 1

    if n == 1:
        return _run_single_pass(results_root=results_root, config=config)
    return _run_majority_passes(results_root=results_root, config=config, n=n)


def _suite_specs(fitness_cfg: dict[str, Any]) -> list[SuiteRunSpec]:
    specs: list[SuiteRunSpec] = []
    for entry in fitness_cfg.get("suites") or []:
        if not isinstance(entry, dict):
            continue
        specs.append(
            SuiteRunSpec(
                name=entry["name"],
                path=Path(entry["path"]),
                parallel=int(entry.get("parallel", 4)),
            )
        )
    return specs


def _run_single_pass(
    *,
    results_root: Path,
    config: dict[str, Any],
) -> dict[str, SuiteRunResult]:
    """The pre-S-Auto-16 single-draw pass. Each suite is run once and
    staged at `<results_root>/<suite>/` via the symlink convention. This
    is the COMMITTED (inert, n=1) code path — kept verbatim so the live
    loop's behaviour is byte-identical to today.
    """
    fitness_cfg = (config or {}).get("fitness", {}) or {}
    timeout = int(fitness_cfg.get("eval_suite_timeout_seconds", 1800))

    results: dict[str, SuiteRunResult] = {}
    for spec in _suite_specs(fitness_cfg):
        results[spec.name] = run_suite(
            spec,
            results_root=results_root,
            config=config,
            timeout_seconds=timeout,
        )
    return results


# --- S-Auto-16: k-of-n majority pass ---------------------------------


def _run_majority_passes(
    *,
    results_root: Path,
    config: dict[str, Any],
    n: int,
) -> dict[str, SuiteRunResult]:
    """Run each suite `n` times into scratch attempt dirs, then collapse
    the per-case pass/fail draws to a majority vote and write an
    aggregated `<results_root>/<suite>/results.json` (a REAL file, not a
    symlink) that the tier_evaluator consumes.

    Retry: when a case has fewer than `min_valid_attempts` provider-
    comparable draws, up to `attempt_retry_cap` additional full passes are
    run; a case still short of the floor is marked non-comparable and does
    not gate.
    """
    fitness_cfg = (config or {}).get("fitness", {}) or {}
    timeout = int(fitness_cfg.get("eval_suite_timeout_seconds", 1800))
    agg_cfg = fitness_cfg.get("aggregation") or {}
    try:
        min_valid = int(agg_cfg.get("min_valid_attempts", MIN_VALID_ATTEMPTS_DEFAULT))
    except (TypeError, ValueError):
        min_valid = MIN_VALID_ATTEMPTS_DEFAULT
    try:
        retry_cap = int(agg_cfg.get("attempt_retry_cap", 2))
    except (TypeError, ValueError):
        retry_cap = 2

    # Configured-primary anchor (OQ-S72.1 fix): provider comparability is
    # measured against the CONFIGURED primary chat model, sourced ONCE for the
    # whole run, never against the per-run empirical modal model. Fail-safe:
    # when unset we raise rather than degrade to "modal == primary" (which
    # would silently treat a whole-run fallback as all-primary).
    primary_model = _configured_primary_model(config)
    if primary_model is None:
        raise EvalRunnerConfigError(
            "fitness.provider_policy.primary_model is unset/empty; provider "
            "comparability cannot anchor on the configured primary. Refusing "
            "to fall back to the empirical modal chat model (a whole-run "
            "fallback would be mis-scored as all-primary). Set "
            "fitness.provider_policy.primary_model to the backend's configured "
            "chat model (e.g. 'deepseek-v4-flash')."
        )

    specs = _suite_specs(fitness_cfg)
    attempts_root = Path(results_root) / "_attempts"

    # Per suite: list of {case_id: AttemptRecord} across all attempts run.
    per_suite_records: dict[str, list[dict[str, AttemptRecord]]] = {
        s.name: [] for s in specs
    }
    # Per suite: list of underlying per-attempt SuiteRunResults.
    per_suite_attempts: dict[str, list[SuiteRunResult]] = {s.name: [] for s in specs}

    def _run_one_attempt(attempt_index: int) -> None:
        attempt_root = attempts_root / f"a{attempt_index}"
        attempt_root.mkdir(parents=True, exist_ok=True)
        for spec in specs:
            sr = run_suite(
                spec,
                results_root=attempt_root,
                config=config,
                timeout_seconds=timeout,
            )
            per_suite_attempts[spec.name].append(sr)
            per_suite_records[spec.name].append(
                _attempt_records_for_suite(sr, attempt_index, primary_model)
            )

    for i in range(n):
        _run_one_attempt(i)

    # Retry loop: add passes while any suite has a case short of the floor.
    extra = 0
    while extra < retry_cap and _any_case_below_floor(
        per_suite_records, min_valid
    ):
        _run_one_attempt(n + extra)
        extra += 1

    # S-Y1.7 P0.4 — primary-target oversampling. After the uniform passes,
    # spend ADDITIONAL attempts ONLY on the active pilot's `primary_targets`
    # so the posterior on the cases the candidate is trying to MOVE is tight
    # enough to credit a real lift over noise (n=`primary_targets_samples`),
    # while every other case stays at `samples_per_case`. No-op when no pilot
    # primary_targets are configured (the pre-pilot general-hill-climber path)
    # or when the target does not exceed `n`. The extra draws are appended to
    # the owning suite's per-case records; aggregation already supports a
    # variable per-case attempt count.
    _run_primary_target_oversample(
        config=config,
        specs=specs,
        n=n,
        retry_cap=retry_cap,
        attempts_root=attempts_root,
        timeout=timeout,
        primary_model=primary_model,
        per_suite_records=per_suite_records,
        per_suite_attempts=per_suite_attempts,
    )

    results: dict[str, SuiteRunResult] = {}
    for spec in specs:
        results[spec.name] = _aggregate_and_stage_suite(
            suite_name=spec.name,
            results_root=Path(results_root),
            per_attempt_records=per_suite_records[spec.name],
            attempt_results=per_suite_attempts[spec.name],
            min_valid=min_valid,
        )
    return results


# --- S-Y1.7 P0.4: primary-target oversampling -----------------------


def _resolve_primary_oversample_plan(
    config: dict[str, Any], specs: list[SuiteRunSpec]
) -> tuple[dict[str, list[tuple[str, Path]]], int]:
    """Map the active pilot's `primary_targets` to the configured suites
    that own them, and return that plan plus the target sample count.

    Source of the two knobs (the existing config schema):
      * `pilot.primary_targets`            — TOP-LEVEL `pilot:` block.
      * `fitness.pilot.primary_targets_samples` — under `fitness:`.

    A primary is matched to a suite by the repo-wide filename convention
    (the case_spec file basename equals its `case_id`): the first
    configured suite whose `--path` directory contains `<case_id>.yaml`
    (or `.yml`) owns it. A primary that resolves to no configured suite
    is skipped (logged by the caller) rather than aborting the pilot.

    Returns `({}, target)` — a no-op plan — when no pilot primary_targets
    are configured (the pre-pilot general-hill-climber path).
    """
    pilot = (config or {}).get("pilot") or {}
    primaries = [c for c in (pilot.get("primary_targets") or []) if isinstance(c, str)]
    fitness_cfg = (config or {}).get("fitness", {}) or {}
    fpilot = fitness_cfg.get("pilot") or {}
    try:
        target = int(fpilot.get("primary_targets_samples", 0))
    except (TypeError, ValueError):
        target = 0
    if not primaries or target <= 0:
        return {}, target

    plan: dict[str, list[tuple[str, Path]]] = {}
    assigned: set[str] = set()
    for spec in specs:
        suite_dir = spec.path if spec.path.is_absolute() else (_REPO_ROOT / spec.path)
        for cid in primaries:
            if cid in assigned:
                continue
            for ext in (".yaml", ".yml"):
                candidate = suite_dir / f"{cid}{ext}"
                if candidate.exists():
                    plan.setdefault(spec.name, []).append((cid, candidate.resolve()))
                    assigned.add(cid)
                    break
    return plan, target


def _run_primary_target_oversample(
    *,
    config: dict[str, Any],
    specs: list[SuiteRunSpec],
    n: int,
    retry_cap: int,
    attempts_root: Path,
    timeout: int,
    primary_model: str | None,
    per_suite_records: dict[str, list[dict[str, AttemptRecord]]],
    per_suite_attempts: dict[str, list[SuiteRunResult]],
) -> None:
    """Run `primary_targets_samples - n` EXTRA passes that exercise ONLY the
    pilot's primary_targets, appending their per-case draws to the owning
    suite's records so the primaries reach the oversample n while every other
    case stays at `samples_per_case`.

    Fence-clean: the extra passes use the EXISTING `eval-interactive run
    --path` flag (which accepts a directory) — no new CLI flag. The mini-suite
    is a scratch directory NAMED after the owning suite that symlinks the real
    primary case_spec files; naming it after the suite makes the loaded
    CaseSpecs carry `source_suite=<suite>` so a human-judgment suite
    (`bad_cases` / `anchor_outcome`, per eval_interactive `_OPT_IN_SETS`) is
    scored on the SAME path as the base draws — the oversampled draws are
    commensurable with them.
    """
    plan, target = _resolve_primary_oversample_plan(config, specs)
    if not plan:
        return
    extra = target - int(n)
    if extra <= 0:
        return

    suite_parallel = {s.name: s.parallel for s in specs}
    mini_parent = Path(attempts_root) / "_primary_oversample"
    for suite_name, items in plan.items():
        if suite_name not in per_suite_records:
            # A primary resolved to a suite that is not in the run set; skip
            # rather than fabricate a suite bucket.
            continue
        primary_ids = {cid for cid, _ in items}
        mini_dir = mini_parent / suite_name
        mini_dir.mkdir(parents=True, exist_ok=True)
        for _cid, src in items:
            link = mini_dir / src.name
            if not link.exists():
                os.symlink(src, link)
        spec = SuiteRunSpec(
            name=f"{suite_name}__primary_oversample",
            path=mini_dir,
            parallel=int(suite_parallel.get(suite_name, 4)),
        )
        for j in range(extra):
            # Offset the attempt index past the retry-loop band (n..n+retry_cap)
            # so AttemptRecord.attempt_index stays unique across the run.
            attempt_index = n + retry_cap + j
            attempt_root = mini_parent / f"{suite_name}_a{j}"
            attempt_root.mkdir(parents=True, exist_ok=True)
            sr = run_suite(
                spec,
                results_root=attempt_root,
                config=config,
                timeout_seconds=timeout,
            )
            per_suite_attempts[suite_name].append(sr)
            recs = _attempt_records_for_suite(sr, attempt_index, primary_model)
            # Defensive: keep only the primaries (the mini-suite already
            # contains only them, but never let an unexpected case_id leak
            # into another case's vote).
            recs = {cid: rec for cid, rec in recs.items() if cid in primary_ids}
            per_suite_records[suite_name].append(recs)


def _configured_primary_model(config: dict[str, Any]) -> str | None:
    """The CONFIGURED primary chat model name (OQ-S72.1 anchor).

    Read from `config.fitness.provider_policy.primary_model`. Returns None
    when unset/empty so the caller can apply the fail-safe (raise) instead
    of degrading to the empirical modal model. Source of truth: the
    backend's configured chat model — the exact string eval-interactive
    records in `llm_calls[].model` for `callType=="chat"` (e.g.
    `deepseek-v4-flash`). The product runtime keeps its own fallback chain;
    this governs the FITNESS harness comparability anchor only.
    """
    pp = ((config or {}).get("fitness") or {}).get("provider_policy") or {}
    val = pp.get("primary_model")
    if isinstance(val, str) and val.strip():
        return val.strip()
    return None


def _attempt_records_for_suite(
    sr: SuiteRunResult, attempt_index: int, primary_model: str
) -> dict[str, AttemptRecord]:
    """Build {case_id: AttemptRecord} for one attempt of one suite by
    reading its results.json and deriving provider comparability from the
    existing `llm_calls[].model` field (no server change, no log parsing).

    `primary_model` is the CONFIGURED primary chat model (OQ-S72.1): a
    `chat` call whose model differs from it is a real fallback
    (`fallback_count>0` → provider_mixed), regardless of how often the
    fallback was engaged across the run.
    """
    data = _read_results_json(sr)
    cases = (data or {}).get("case_results") or []
    out: dict[str, AttemptRecord] = {}
    for case in cases:
        cid = case.get("case_id", "<unknown>")
        out[cid] = _case_attempt_record(case, attempt_index, primary_model)
    return out


def _read_results_json(sr: SuiteRunResult) -> dict[str, Any] | None:
    rj = getattr(sr, "results_json", None)
    if not isinstance(rj, (str, Path)):
        return None
    p = Path(rj)
    if not p.exists():
        return None
    try:
        with p.open("r", encoding="utf-8") as f:
            return json.load(f)
    except (OSError, json.JSONDecodeError):
        return None


# Only the AGENT chat call is fitness-relevant for provider comparability.
# eval-interactive's results.json tags each llm_call with `callType`: the
# agent loop is `chat` (the model FallbackLlmClient wraps), while `rerank`
# / embedding / judge calls legitimately use a DIFFERENT model by design
# (e.g. chat=deepseek, rerank=kimi). A true fallback is a `chat` call whose
# model differs from the chat primary — NOT model diversity across call
# types. Scoping to `chat` is what keeps the vote over one agent-model
# population. A call with no `callType` is treated as `chat` (back-compat).
_FITNESS_CALL_TYPE = "chat"


def _chat_models(case: dict[str, Any]) -> list[str]:
    out: list[str] = []
    for call in case.get("llm_calls") or []:
        if call.get("callType", _FITNESS_CALL_TYPE) != _FITNESS_CALL_TYPE:
            continue
        model = call.get("model")
        if isinstance(model, str) and model:
            out.append(model)
    return out


def _case_attempt_record(
    case: dict[str, Any], attempt_index: int, primary_model: str | None
) -> AttemptRecord:
    models = _chat_models(case)
    distinct = sorted({m for m in models if m})
    fallback_count = (
        sum(1 for m in models if m and m != primary_model)
        if primary_model is not None
        else 0
    )
    actual_model = (
        primary_model if primary_model in distinct or not distinct
        else (distinct[0] if len(distinct) == 1 else ",".join(distinct))
    )
    if len(distinct) == 1:
        actual_model = distinct[0]
    elif len(distinct) > 1:
        actual_model = ",".join(distinct)

    infra = _case_is_infra_degraded(case)
    if infra:
        valid, reason, passed = False, INVALID_INFRA_ERROR, None
    elif fallback_count > 0:
        valid, reason, passed = False, INVALID_PROVIDER_MIXED, case.get("case_passed")
    else:
        valid, reason, passed = True, None, case.get("case_passed")

    record = AttemptRecord(
        attempt_index=attempt_index,
        case_passed=passed,
        valid=valid,
        invalid_reason=reason,
        failure_reason=case.get("stop_reason"),
        failure_tags=list(case.get("failure_tags") or []),
        escalation_reason=case.get("escalation_reason"),
        actual_provider=("primary" if fallback_count == 0 else "mixed"),
        actual_model=actual_model,
        fallback_count=fallback_count,
        request_id=case.get("request_id"),
    )
    # Stash per-check / per-step detail for the tier_evaluator's L0
    # stable-reproduction and L2 majority (kept off the pure AttemptRecord
    # API as dynamic attributes so aggregate.py stays decoupled from the
    # eval-interactive results schema).
    record._l1_checks = {  # type: ignore[attr-defined]
        c.get("check"): c.get("passed") is True
        for c in (case.get("l1_results") or [])
        if isinstance(c.get("check"), str)
    }
    tier2 = case.get("tier2_result") or {}
    record._tier2_mandatory_fail_steps = [  # type: ignore[attr-defined]
        s.get("step_id")
        for s in (tier2.get("per_step") or [])
        if s.get("severity") == "mandatory"
        and s.get("outcome") == "FAIL"
        and s.get("step_id")
    ]
    return record


def _case_is_infra_degraded(case: dict[str, Any]) -> bool:
    """Mirror of `autoloop.loop._case_is_infra_degraded` (kept local to
    avoid an import cycle). A transport/deadline/first-turn-abort signal →
    the draw is infra-degraded and excluded from the vote. A generic
    fitness FAIL is deliberately NOT infra (it must still reach the vote).
    """
    if not isinstance(case, dict):
        return False
    if case.get("status") == "ERROR":
        return True
    if case.get("status") == "CONTRACT_VIOLATION":
        cv = case.get("contract_violation") or {}
        if isinstance(cv, dict) and cv.get("field") == "active_use_case":
            return True
    if case.get("escalation_reason") in _INFRA_ESCALATION_REASONS:
        return True
    for tag in case.get("failure_tags") or []:
        if any(sub in str(tag) for sub in _INFRA_FAILURE_TAG_SUBSTRINGS):
            return True
    return False


def _any_case_below_floor(
    per_suite_records: dict[str, list[dict[str, AttemptRecord]]],
    min_valid: int,
) -> bool:
    for attempts in per_suite_records.values():
        valid_counts: Counter[str] = Counter()
        seen: set[str] = set()
        for attempt in attempts:
            for cid, rec in attempt.items():
                seen.add(cid)
                if rec.valid and rec.case_passed is not None:
                    valid_counts[cid] += 1
        for cid in seen:
            if valid_counts[cid] < min_valid:
                return True
    return False


def _aggregate_and_stage_suite(
    *,
    suite_name: str,
    results_root: Path,
    per_attempt_records: list[dict[str, AttemptRecord]],
    attempt_results: list[SuiteRunResult],
    min_valid: int,
) -> SuiteRunResult:
    """Collapse the per-attempt draws to a majority verdict per case and
    write a real aggregated results.json the tier_evaluator can read.
    """
    # Use the first readable attempt's results.json as the base payload so
    # we preserve every original field (primary_uc, status, l1_results,
    # tier2_result, transcript, ...) that downstream consumers rely on.
    base_payload: dict[str, Any] | None = None
    base_cases_by_id: dict[str, dict[str, Any]] = {}
    for sr in attempt_results:
        data = _read_results_json(sr)
        if data is None:
            continue
        if base_payload is None:
            base_payload = data
        for case in data.get("case_results") or []:
            cid = case.get("case_id", "<unknown>")
            base_cases_by_id.setdefault(cid, case)

    # Union of case ids, preserving first-seen order.
    ordered_ids: list[str] = []
    seen: set[str] = set()
    for attempt in per_attempt_records:
        for cid in attempt:
            if cid not in seen:
                seen.add(cid)
                ordered_ids.append(cid)

    aggregated_cases: list[dict[str, Any]] = []
    for cid in ordered_ids:
        records = [a[cid] for a in per_attempt_records if cid in a]
        agg = aggregate_case(cid, records, min_valid_attempts=min_valid)
        base = dict(base_cases_by_id.get(cid) or {"case_id": cid})
        base.update(agg.to_dict())
        base["tier0_majority"] = _tier0_check_majority(records)
        base["tier2_result_majority"] = _tier2_majority(records)
        aggregated_cases.append(base)

    payload: dict[str, Any] = dict(base_payload or {})
    payload["case_results"] = aggregated_cases
    payload["_aggregation"] = {
        "method": "majority",
        "min_valid_attempts": min_valid,
        "attempts_run": len(per_attempt_records),
        "suite": suite_name,
    }

    suite_dir = results_root / suite_name
    suite_dir.mkdir(parents=True, exist_ok=True)
    out_json = suite_dir / "results.json"
    with out_json.open("w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, default=str)

    readable = any(_read_results_json(sr) is not None for sr in attempt_results)
    exit_code = 0 if readable else 1
    error_tail = next(
        (sr.error_tail for sr in attempt_results if sr.error_tail), None
    )
    elapsed = sum(getattr(sr, "elapsed_seconds", 0.0) or 0.0 for sr in attempt_results)

    return SuiteRunResult(
        suite_name=suite_name,
        results_dir=suite_dir,
        results_json=out_json,
        elapsed_seconds=elapsed,
        exit_code=exit_code,
        error_tail=error_tail,
        attempts=list(attempt_results),
    )


def _tier0_check_majority(records: list[AttemptRecord]) -> dict[str, bool]:
    """Per-Tier-0-check majority of `passed` across VALID attempts, keyed
    by check name. The tier_evaluator's L0 reads this to require a Tier-0
    violation to reproduce in the candidate majority before it gates
    (stable-reproduction). Built from each attempt's case dict — but the
    AttemptRecord does not carry l1_results, so we recompute from the
    stashed per-attempt check map.
    """
    # The check-level pass map per attempt is stashed on the record by
    # `_case_attempt_record` consumers; if unavailable, returns empty (the
    # tier_evaluator then falls back to single-draw l1_results).
    per_check: dict[str, list[bool]] = {}
    for rec in records:
        if not (rec.valid and rec.case_passed is not None):
            continue
        checks = getattr(rec, "_l1_checks", None)
        if not checks:
            continue
        for name, passed in checks.items():
            per_check.setdefault(name, []).append(bool(passed))
    out: dict[str, bool] = {}
    for name, vals in per_check.items():
        mb = majority_bool(vals)
        if mb is not None:
            out[name] = mb
    return out


def _tier2_majority(records: list[AttemptRecord]) -> dict[str, Any]:
    """Majority-collapsed tier2 per_step: a mandatory step counts as a
    FAIL only if it FAILs in the majority of valid attempts. Shaped so the
    tier_evaluator's `_tier2_mandatory_metrics` can read it unchanged.
    """
    valid = [r for r in records if r.valid and r.case_passed is not None]
    n_valid = len(valid)
    if n_valid == 0:
        return {"per_step": []}
    step_fail_counts: Counter[str] = Counter()
    for rec in valid:
        steps = getattr(rec, "_tier2_mandatory_fail_steps", None) or []
        for sid in steps:
            step_fail_counts[sid] += 1
    per_step = [
        {"step_id": sid, "severity": "mandatory", "outcome": "FAIL"}
        for sid, cnt in step_fail_counts.items()
        if cnt > (n_valid / 2)
    ]
    return {"per_step": per_step}
