"""Tests for `autoloop.scoring.baseline_loader`."""

from __future__ import annotations

import json
import warnings
from pathlib import Path

import pytest

from autoloop.scoring import (
    BaselineLoadError,
    BaselineSnapshot,
    SuiteSnapshot,
    load,
)


DEFAULT_CONFIG = {
    "fitness": {
        "suites": [
            {"name": "bad_cases", "path": "x", "parallel": 1},
            {"name": "anchor_outcome", "path": "x", "parallel": 4},
            {"name": "shadow", "path": "x", "parallel": 4},
        ],
    }
}


def _write_results(path: Path, cases: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "run_id": path.parent.name,
        "case_results": cases,
        "summary": {"total_cases": len(cases)},
    }
    path.write_text(json.dumps(payload), encoding="utf-8")


def _case(
    case_id: str,
    *,
    passed: bool = True,
    primary_uc: str = "UC-A",
    mandatory_failures: list[str] | None = None,
) -> dict:
    per_step = []
    if mandatory_failures:
        for sid in mandatory_failures:
            per_step.append({
                "step_id": sid,
                "severity": "mandatory",
                "outcome": "FAIL",
            })
    return {
        "case_id": case_id,
        "primary_uc": primary_uc,
        "case_passed": passed,
        "tier2_result": {
            "passed": not bool(mandatory_failures),
            "severity": "mandatory" if mandatory_failures else "advisory",
            "failed_step_ids": mandatory_failures or [],
            "per_step": per_step,
        },
    }


# --- Happy path ------------------------------------------------------


def test_load_happy_path_three_suites_present(tmp_path: Path):
    base = tmp_path / "baseline"
    _write_results(
        base / "bad_cases" / "results.json",
        [_case("bc01"), _case("bc02", passed=False, mandatory_failures=["s1"])],
    )
    _write_results(
        base / "anchor_outcome" / "results.json",
        [_case("ao01"), _case("ao02", primary_uc="UC-H")],
    )
    _write_results(
        base / "shadow" / "results.json",
        [_case("sh01"), _case("sh02"), _case("sh03", passed=False)],
    )

    snap = load(base, config=DEFAULT_CONFIG)

    assert isinstance(snap, BaselineSnapshot)
    assert set(snap.snapshots.keys()) == {"bad_cases", "anchor_outcome", "shadow"}

    bc = snap.snapshots["bad_cases"]
    assert bc.case_passed_count == 1
    assert bc.total_cases == 2
    assert bc.tier2_mandatory_failure_count == 1

    ao = snap.snapshots["anchor_outcome"]
    assert ao.case_passed_count == 2
    assert ao.tier2_mandatory_failure_count == 0

    sh = snap.snapshots["shadow"]
    assert sh.case_passed_count == 2
    assert sh.case_passed_rate == pytest.approx(2 / 3)


# --- Missing suite ---------------------------------------------------


def test_load_missing_shadow_returns_none_with_warning(tmp_path: Path):
    base = tmp_path / "baseline"
    _write_results(base / "bad_cases" / "results.json", [_case("bc01")])
    _write_results(base / "anchor_outcome" / "results.json", [_case("ao01")])
    # NOTE: no shadow subdir

    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        snap = load(base, config=DEFAULT_CONFIG)

    assert snap.snapshots["shadow"].case_passed_count is None
    assert snap.snapshots["shadow"].case_passed_rate is None
    assert snap.snapshots["shadow"].tier2_mandatory_failure_count is None
    assert snap.snapshots["shadow"].warning is not None
    assert any("shadow" in str(w.message) for w in caught)
    assert any("shadow" in w for w in snap.warnings)


def test_load_per_uc_tier2_bucketing(tmp_path: Path):
    """Tier-2 mandatory failures bucketed by primary_uc."""
    base = tmp_path / "baseline"
    _write_results(
        base / "bad_cases" / "results.json",
        [
            _case("bc01", primary_uc="UC-A", mandatory_failures=["s1"]),
            _case("bc02", primary_uc="UC-H", mandatory_failures=["s2", "s3"]),
            _case("bc03", primary_uc="UC-A"),
        ],
    )
    _write_results(base / "anchor_outcome" / "results.json", [_case("ao01")])
    _write_results(base / "shadow" / "results.json", [_case("sh01")])

    snap = load(base, config=DEFAULT_CONFIG)
    bc = snap.snapshots["bad_cases"]
    assert bc.tier2_mandatory_failure_by_uc == {"UC-A": 1, "UC-H": 2}
    assert bc.tier2_mandatory_failure_count == 3


# --- Malformed baseline ---------------------------------------------


def test_load_missing_baseline_dir_raises(tmp_path: Path):
    bogus = tmp_path / "does_not_exist"
    with pytest.raises(BaselineLoadError) as exc:
        load(bogus, config=DEFAULT_CONFIG)
    assert "does not exist" in str(exc.value)


def test_load_malformed_results_json_raises(tmp_path: Path):
    base = tmp_path / "baseline"
    bc_dir = base / "bad_cases"
    bc_dir.mkdir(parents=True)
    (bc_dir / "results.json").write_text("{not valid json", encoding="utf-8")
    _write_results(base / "anchor_outcome" / "results.json", [_case("ao01")])
    _write_results(base / "shadow" / "results.json", [_case("sh01")])

    with pytest.raises(BaselineLoadError) as exc:
        load(base, config=DEFAULT_CONFIG)
    assert "malformed JSON" in str(exc.value)
    assert "bad_cases" in str(exc.value)


def test_load_empty_suites_config_raises(tmp_path: Path):
    base = tmp_path / "baseline"
    base.mkdir()
    bad_cfg = {"fitness": {"suites": []}}
    with pytest.raises(BaselineLoadError) as exc:
        load(base, config=bad_cfg)
    assert "suites" in str(exc.value)


# --- Flat results.json fallback --------------------------------------


def test_load_flat_results_json_fallback(tmp_path: Path):
    """When per-suite subdirs are absent but a flat results.json
    exists, the loader uses it for every requested suite (with a
    warning noting source_suite is unknown)."""
    base = tmp_path / "baseline"
    _write_results(
        base / "results.json",
        [_case("bc01"), _case("ao01"), _case("sh01")],
    )

    snap = load(base, config=DEFAULT_CONFIG)
    for suite_name in ("bad_cases", "anchor_outcome", "shadow"):
        s = snap.snapshots[suite_name]
        assert s.case_passed_count == 3  # all 3 attributed to each suite
        assert s.warning is not None
        assert "flat results.json" in s.warning
