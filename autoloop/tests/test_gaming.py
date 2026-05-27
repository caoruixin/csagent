"""Tests for the S-Auto-4 anti-gaming detector.

Each check has a positive (trigger) fixture and a negative (clean)
fixture. D3 regressions are explicit:

- `scoring_code_drift.baseline_missing` emits WARN when
  `scoring_code_baseline_sha` is None (NOT a guess).
- `shadow_set_leakage` is CONFIG-DRIVEN: with no
  `shadow_leak_signatures` in config, even a record containing
  shadow-like tokens MUST NOT flag.
"""

from __future__ import annotations

import json
from pathlib import Path

from autoloop.scoring import gaming as _gaming


# ---------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------


def _write_results_json(
    root: Path,
    suite: str,
    *,
    case_results=None,
) -> Path:
    suite_dir = root / suite
    suite_dir.mkdir(parents=True, exist_ok=True)
    payload = {"case_results": case_results or []}
    rj = suite_dir / "results.json"
    rj.write_text(json.dumps(payload), encoding="utf-8")
    return rj


# ---------------------------------------------------------------------
# Check 1 — anomalous_metric_movement
# ---------------------------------------------------------------------


def test_anomalous_metric_movement_triggers_on_large_improvement_small_diff():
    iteration_record = {
        "hypothesis": {
            "before_value": "step one",  # 8 chars
            "after_value": "step two",   # 8 chars (diff: 0 → falls back to len(after))
        },
        "verdict": {
            "tier_breakdown": {
                "improvement_threshold": {
                    "bad_cases_passed_delta": 5,
                    "anchor_outcome_passed_delta": 0,
                    "tier2_mandatory_failure_reduction": 0,
                },
            },
        },
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records=[],
        eval_artefacts={},
        config={"gaming": {"anomalous_metric_movement_threshold": 0.05}},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "anomalous_metric_movement" in rule_ids
    flag = next(f for f in flags if f.rule_id == "anomalous_metric_movement")
    assert flag.severity == "WARN"


def test_anomalous_metric_movement_clean_no_flag():
    iteration_record = {
        "hypothesis": {
            "before_value": "x" * 200,
            "after_value": "x" * 250,
        },
        "verdict": {
            "tier_breakdown": {
                "improvement_threshold": {
                    "bad_cases_passed_delta": 1,
                    "anchor_outcome_passed_delta": 0,
                    "tier2_mandatory_failure_reduction": 0,
                },
            },
        },
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records=[],
        eval_artefacts={},
        config={"gaming": {"anomalous_metric_movement_threshold": 0.05}},
    )
    rule_ids = [f.rule_id for f in flags]
    # 1 improvement / 50 diff_chars = 0.02 < 0.05 threshold.
    assert "anomalous_metric_movement" not in rule_ids


# ---------------------------------------------------------------------
# Check 2 — identical_eval_traces_across_different_hypotheses
# ---------------------------------------------------------------------


def test_identical_eval_traces_triggers_on_distinct_fingerprints_same_hash(tmp_path: Path):
    results_root = tmp_path / "current"
    _write_results_json(
        results_root, "bad_cases",
        case_results=[{"case_id": "x", "case_passed": True}],
    )
    cur_hash = _gaming._hash_results_dir(results_root)
    assert cur_hash is not None

    iteration_record = {
        "iteration_id": "exp-2",
        "hypothesis": {"fingerprint": "fp-current"},
    }
    prior = {
        "iteration_id": "exp-1",
        "hypothesis": {"fingerprint": "fp-prior"},
        "_results_hash": cur_hash,
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records=[prior],
        eval_artefacts={"results_root": results_root},
        config={},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "identical_eval_traces_across_different_hypotheses" in rule_ids
    flag = next(
        f for f in flags
        if f.rule_id == "identical_eval_traces_across_different_hypotheses"
    )
    assert flag.severity == "ERROR"


def test_identical_eval_traces_clean_distinct_hash(tmp_path: Path):
    results_root = tmp_path / "current"
    _write_results_json(
        results_root, "bad_cases",
        case_results=[{"case_id": "x", "case_passed": True}],
    )

    iteration_record = {
        "iteration_id": "exp-2",
        "hypothesis": {"fingerprint": "fp-current"},
    }
    prior = {
        "iteration_id": "exp-1",
        "hypothesis": {"fingerprint": "fp-prior"},
        "_results_hash": "deadbeef" * 8,  # distinct hash
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records=[prior],
        eval_artefacts={"results_root": results_root},
        config={},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "identical_eval_traces_across_different_hypotheses" not in rule_ids


# ---------------------------------------------------------------------
# Check 3 — suspect_baseline_manipulation
# ---------------------------------------------------------------------


def test_suspect_baseline_manipulation_git_lookup_failed_emits_warn(monkeypatch):
    """If `git log` returns nonzero / no commits, we emit a WARN
    rule_id rather than guessing the baseline was tampered."""
    import subprocess as sp

    class _FakeProc:
        def __init__(self, returncode=1, stdout="", stderr="no log"):
            self.returncode = returncode
            self.stdout = stdout
            self.stderr = stderr

    def fake_run(*args, **kwargs):
        return _FakeProc()

    monkeypatch.setattr(sp, "run", fake_run)
    monkeypatch.setattr(_gaming.subprocess, "run", fake_run)

    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={},
        config={"fitness": {"baseline_dir": "eval_interactive/results/x"}},
    )
    rule_ids = [f.rule_id for f in flags]
    assert any(r.startswith("suspect_baseline_manipulation") for r in rule_ids)


def test_suspect_baseline_manipulation_no_baseline_dir_no_flag():
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={},
        config={"fitness": {}},  # no baseline_dir
    )
    rule_ids = [f.rule_id for f in flags]
    assert not any(
        r.startswith("suspect_baseline_manipulation") for r in rule_ids
    )


# ---------------------------------------------------------------------
# Check 4 — eval_time_gaming_via_timeout_skip
# ---------------------------------------------------------------------


def test_eval_time_gaming_triggers_above_absolute_floor(tmp_path: Path):
    results_root = tmp_path / "current"
    cases = [
        {"case_id": "a", "terminal_outcome": "timeout"},
        {"case_id": "b", "terminal_outcome": "skipped"},
        {"case_id": "c", "terminal_outcome": "completed"},
        {"case_id": "d", "terminal_outcome": "completed"},
    ]  # 50% skip/timeout — well above 10% floor
    _write_results_json(results_root, "bad_cases", case_results=cases)
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={"results_root": results_root},
        config={},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "eval_time_gaming_via_timeout_skip" in rule_ids


def test_eval_time_gaming_clean_low_skip_rate(tmp_path: Path):
    results_root = tmp_path / "current"
    cases = [
        {"case_id": f"c{i}", "terminal_outcome": "completed"}
        for i in range(20)
    ]
    _write_results_json(results_root, "bad_cases", case_results=cases)
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={"results_root": results_root},
        config={},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "eval_time_gaming_via_timeout_skip" not in rule_ids


# ---------------------------------------------------------------------
# Check 5 — scoring_code_drift
# ---------------------------------------------------------------------


def test_scoring_code_drift_baseline_missing_emits_warn():
    """D3 regression: when `scoring_code_baseline_sha` is None, we
    emit a `baseline_missing` WARN rather than computing the current
    SHA and silently using it as a baseline."""
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={},
        config={"fitness": {"scoring_code_baseline_sha": None}},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "scoring_code_drift.baseline_missing" in rule_ids
    flag = next(f for f in flags if f.rule_id == "scoring_code_drift.baseline_missing")
    assert flag.severity == "WARN"
    # Evidence MUST surface that no baseline was set.
    assert flag.evidence.get("baseline_sha") is None


def test_scoring_code_drift_baseline_unset_treated_as_missing():
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={},
        config={"fitness": {}},  # entirely absent
    )
    rule_ids = [f.rule_id for f in flags]
    assert "scoring_code_drift.baseline_missing" in rule_ids


def test_scoring_code_drift_matching_sha_clean():
    """When the configured baseline SHA matches current, no flag fires."""
    current_sha = _gaming._compute_scoring_code_sha()
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={},
        config={"fitness": {"scoring_code_baseline_sha": current_sha}},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "scoring_code_drift.baseline_missing" not in rule_ids
    assert "scoring_code_drift.sha_changed" not in rule_ids


def test_scoring_code_drift_diverged_sha_errors():
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={},
        config={"fitness": {"scoring_code_baseline_sha": "0" * 64}},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "scoring_code_drift.sha_changed" in rule_ids
    flag = next(f for f in flags if f.rule_id == "scoring_code_drift.sha_changed")
    assert flag.severity == "ERROR"


# ---------------------------------------------------------------------
# Check 6 — shadow_set_leakage
# ---------------------------------------------------------------------


def test_shadow_set_leakage_triggers_on_configured_signature():
    iteration_record = {
        "iteration_id": "exp-leak",
        "hypothesis": {"rationale": "matched cs59s in some upstream lookup"},
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records=[],
        eval_artefacts={},
        config={"gaming": {"shadow_leak_signatures": ["cs59s"]}},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "shadow_set_leakage" in rule_ids
    flag = next(f for f in flags if f.rule_id == "shadow_set_leakage")
    assert flag.severity == "ERROR"


def test_shadow_set_leakage_d3_no_default_signatures():
    """D3 regression: with no `shadow_leak_signatures` in config, the
    detector MUST NOT have any implicit default that would read the
    shadow corpus. Even an iteration record containing `cs59s` does
    NOT flag without explicit config opt-in."""
    iteration_record = {
        "iteration_id": "exp-leak-check",
        "hypothesis": {"rationale": "contains cs59s but config is empty"},
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records=[],
        eval_artefacts={},
        config={"gaming": {}},  # NO shadow_leak_signatures key
    )
    rule_ids = [f.rule_id for f in flags]
    assert "shadow_set_leakage" not in rule_ids


def test_shadow_set_leakage_empty_list_in_config_does_not_trigger():
    iteration_record = {
        "iteration_id": "exp-leak-check",
        "hypothesis": {"rationale": "contains cs59s; explicit empty list"},
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records=[],
        eval_artefacts={},
        config={"gaming": {"shadow_leak_signatures": []}},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "shadow_set_leakage" not in rule_ids


# ---------------------------------------------------------------------
# Check 7 — tier2_measurement_contract_change_attempt
# ---------------------------------------------------------------------


def test_tier2_measurement_contract_change_attempt_triggers(tmp_path: Path):
    """A critical_step that previously FAILed now scores N/A → ERROR."""
    base_root = tmp_path / "baseline"
    cur_root = tmp_path / "current"
    _write_results_json(
        base_root, "bad_cases",
        case_results=[
            {
                "case_id": "a",
                "tier2_result": {
                    "per_step": [{"step_id": "s1", "outcome": "FAIL"}],
                },
            },
        ],
    )
    _write_results_json(
        cur_root, "bad_cases",
        case_results=[
            {
                "case_id": "a",
                "tier2_result": {
                    "per_step": [{"step_id": "s1", "outcome": "N/A"}],
                },
            },
        ],
    )
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={
            "results_root": cur_root,
            "baseline_dir": base_root,
        },
        config={},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "tier2_measurement_contract_change_attempt" in rule_ids
    flag = next(
        f for f in flags
        if f.rule_id == "tier2_measurement_contract_change_attempt"
    )
    assert flag.severity == "ERROR"


def test_tier2_measurement_contract_change_attempt_clean_no_flag(tmp_path: Path):
    base_root = tmp_path / "baseline"
    cur_root = tmp_path / "current"
    _write_results_json(
        base_root, "bad_cases",
        case_results=[
            {"case_id": "a", "tier2_result": {"per_step": [{"step_id": "s1", "outcome": "FAIL"}]}},
        ],
    )
    _write_results_json(
        cur_root, "bad_cases",
        case_results=[
            {"case_id": "a", "tier2_result": {"per_step": [{"step_id": "s1", "outcome": "FAIL"}]}},
        ],
    )
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={
            "results_root": cur_root,
            "baseline_dir": base_root,
        },
        config={},
    )
    rule_ids = [f.rule_id for f in flags]
    assert "tier2_measurement_contract_change_attempt" not in rule_ids


# ---------------------------------------------------------------------
# Detector global behaviour
# ---------------------------------------------------------------------


def test_detect_disabled_returns_empty():
    flags = _gaming.detect(
        {"hypothesis": {}},
        recent_records=[],
        eval_artefacts={},
        config={"gaming": {"enabled": False}},
    )
    assert flags == []


def test_detect_returns_observation_only_severities():
    """Sanity: every flag is WARN or ERROR; no other severities."""
    flags = _gaming.detect(
        {"hypothesis": {}, "verdict": {}},
        recent_records=[],
        eval_artefacts={},
        config={"fitness": {}},
    )
    for f in flags:
        assert f.severity in ("WARN", "ERROR")
