"""Baseline snapshot loader for the auto-loop tier evaluator.

S-Auto-2 deliverable. Reads a baseline results directory (or per-suite
subdirs) into a `BaselineSnapshot` keyed by suite name. The
`tier_evaluator.evaluate(...)` function consumes this snapshot to
compute non-regression bars at Layers 1 and 2.

The baseline directory is named EXPLICITLY by
`config.fitness.baseline_dir`. There is NO "latest results dir"
heuristic by design — auto-loop must never silently anchor to a
stale results directory that would shift baselines under it.

Layout convention (per S-Auto-2 contract, mirroring eval_runner's
per-suite `--output-dir <root>/<suite>/` pattern):

    <baseline_dir>/
        bad_cases/results.json
        anchor_outcome/results.json
        shadow/results.json

A flat `<baseline_dir>/results.json` mixing all suites is also
supported as a fallback; cases are then filtered by case_id
matching the suite directory listing (see `_load_flat_results`).
Missing suites surface as a warning + `SuiteSnapshot.case_passed_count =
None`; the tier_evaluator treats this as "no baseline → keep gate" so
the loop does not fail catastrophically on first-ever runs before
baselines exist.
"""

from __future__ import annotations

import json
import warnings
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


# --- Exceptions ------------------------------------------------------


class BaselineLoadError(Exception):
    """Raised when a baseline_dir is structurally unreadable (path
    missing entirely OR a results.json is malformed JSON)."""


# --- Dataclasses -----------------------------------------------------


@dataclass
class SuiteSnapshot:
    """Aggregated counts for one suite at the baseline run."""

    suite_name: str
    case_passed_count: int | None
    case_passed_rate: float | None
    tier2_mandatory_failure_count: int | None
    tier2_mandatory_failure_by_uc: dict[str, int] = field(default_factory=dict)
    raw_results_json: Path | None = None
    total_cases: int = 0
    warning: str | None = None


@dataclass
class BaselineSnapshot:
    """A frozen snapshot of the baseline run, keyed by suite name."""

    baseline_run_id: str
    baseline_dir: Path
    snapshots: dict[str, SuiteSnapshot]
    tier0_baseline: dict[str, Any]
    captured_at: str
    warnings: list[str] = field(default_factory=list)


# --- Public API ------------------------------------------------------


def load(baseline_dir: Path, *, config: dict[str, Any]) -> BaselineSnapshot:
    """Load a baseline snapshot from `baseline_dir`.

    `config["fitness"]["suites"]` names the suites the loop cares
    about (default v1: bad_cases, anchor_outcome, shadow). Each suite
    is loaded independently; a missing suite produces a warning but
    does not raise.

    `baseline_dir` is taken as authoritative — the caller is
    responsible for resolving the literal pointer in
    `config.fitness.baseline_dir` against the repo root.
    """
    baseline_dir = Path(baseline_dir)
    if not baseline_dir.exists():
        raise BaselineLoadError(f"baseline_dir does not exist: {baseline_dir}")
    if not baseline_dir.is_dir():
        raise BaselineLoadError(f"baseline_dir is not a directory: {baseline_dir}")

    fitness_cfg = (config or {}).get("fitness", {}) or {}
    suites_cfg = fitness_cfg.get("suites") or []
    if not suites_cfg:
        raise BaselineLoadError(
            "config.fitness.suites is empty; cannot determine which suites to load"
        )

    snapshots: dict[str, SuiteSnapshot] = {}
    snapshot_warnings: list[str] = []

    flat_results = _find_flat_results(baseline_dir)
    flat_data = _read_json(flat_results) if flat_results else None

    for suite_entry in suites_cfg:
        suite_name = suite_entry.get("name") if isinstance(suite_entry, dict) else suite_entry
        if not suite_name:
            continue

        suite_dir = baseline_dir / suite_name
        per_suite_json = suite_dir / "results.json"

        if per_suite_json.exists():
            data = _read_json(per_suite_json)
            snapshot = _summarize(suite_name, data, per_suite_json)
        elif flat_data is not None:
            snapshot = _summarize_from_flat(suite_name, flat_data, flat_results)
        else:
            msg = (
                f"baseline missing suite '{suite_name}': "
                f"neither {per_suite_json} nor a flat results.json found"
            )
            warnings.warn(msg)
            snapshot_warnings.append(msg)
            snapshot = SuiteSnapshot(
                suite_name=suite_name,
                case_passed_count=None,
                case_passed_rate=None,
                tier2_mandatory_failure_count=None,
                warning=msg,
            )

        snapshots[suite_name] = snapshot

    return BaselineSnapshot(
        baseline_run_id=baseline_dir.name,
        baseline_dir=baseline_dir,
        snapshots=snapshots,
        tier0_baseline={},
        captured_at=datetime.now(timezone.utc).isoformat(timespec="seconds"),
        warnings=snapshot_warnings,
    )


# --- Internals -------------------------------------------------------


def _find_flat_results(baseline_dir: Path) -> Path | None:
    """A flat `<baseline_dir>/results.json` is the fallback shape if
    per-suite subdirs are not present.
    """
    flat = baseline_dir / "results.json"
    return flat if flat.exists() else None


def _read_json(path: Path) -> dict[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except json.JSONDecodeError as e:
        raise BaselineLoadError(f"malformed JSON in {path}: {e}") from e
    except OSError as e:
        raise BaselineLoadError(f"cannot read {path}: {e}") from e


def _summarize(
    suite_name: str, data: dict[str, Any], json_path: Path
) -> SuiteSnapshot:
    """Aggregate one suite's cases into a `SuiteSnapshot`."""
    cases = data.get("case_results") or []
    return _aggregate_cases(suite_name, cases, json_path)


def _summarize_from_flat(
    suite_name: str, data: dict[str, Any], json_path: Path
) -> SuiteSnapshot:
    """When the baseline is a flat results.json mixing all suites, we
    cannot identify suite membership by a `source_suite` field (it
    does not exist in the schema). Caller has the option of
    pre-filtering by case_id pattern; until that is wired we return a
    snapshot covering all cases in the flat file — which is correct
    only when the baseline genuinely has just one suite present.
    """
    cases = data.get("case_results") or []
    snap = _aggregate_cases(suite_name, cases, json_path)
    snap.warning = (
        f"flat results.json used for suite '{suite_name}'; "
        f"source_suite field is not present in schema, so all "
        f"{snap.total_cases} cases were attributed to this suite"
    )
    return snap


def _aggregate_cases(
    suite_name: str, cases: list[dict[str, Any]], json_path: Path
) -> SuiteSnapshot:
    if not cases:
        return SuiteSnapshot(
            suite_name=suite_name,
            case_passed_count=0,
            case_passed_rate=0.0,
            tier2_mandatory_failure_count=0,
            raw_results_json=json_path,
            total_cases=0,
        )

    passed = sum(1 for c in cases if c.get("case_passed") is True)
    total = len(cases)

    mandatory_fail_count = 0
    by_uc: dict[str, int] = {}
    for c in cases:
        tier2 = c.get("tier2_result") or {}
        per_step = tier2.get("per_step") or []
        mandatory_fails = [
            s for s in per_step
            if s.get("severity") == "mandatory" and s.get("outcome") == "FAIL"
        ]
        if mandatory_fails:
            mandatory_fail_count += len(mandatory_fails)
            uc = c.get("primary_uc") or "unknown"
            by_uc[uc] = by_uc.get(uc, 0) + len(mandatory_fails)

    return SuiteSnapshot(
        suite_name=suite_name,
        case_passed_count=passed,
        case_passed_rate=passed / total if total else 0.0,
        tier2_mandatory_failure_count=mandatory_fail_count,
        tier2_mandatory_failure_by_uc=by_uc,
        raw_results_json=json_path,
        total_cases=total,
    )
