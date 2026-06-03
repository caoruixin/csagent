"""Tests for `autoloop.scoring.tier_evaluator`.

Synthetic results.json fixtures + direct unit tests, one per layer
of the 5-layer lexicographic verdict. Adversarial fixture validates
the "no down-tier compensation" Constitution §1.6 invariant. Shadow
firewall test validates that the loop-facing API surface NEVER
exposes per-case shadow info.

No live subprocess calls; no real `eval-interactive run`. The fixture
results.json blobs are minimal enough to be inlined and trivially
human-auditable.
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from autoloop.scoring import (
    BaselineSnapshot,
    LexicographicVerdict,
    ShadowAuditDetail,
    SuiteSnapshot,
    evaluate,
)


# --- Default fitness config used by every test -----------------------


DEFAULT_CONFIG = {
    "fitness": {
        "suites": [
            {"name": "bad_cases", "path": "x", "parallel": 1},
            {"name": "anchor_outcome", "path": "x", "parallel": 4},
            {"name": "shadow", "path": "x", "parallel": 4},
        ],
        "improvement_threshold_mode": "case_count",
        "improvement_min_cases": 1,
        "shadow_max_drop_pct": 3.0,
        "anchor_outcome_max_drop_cases": 0,
    }
}


# --- Helpers ---------------------------------------------------------


def _make_case(
    case_id: str,
    primary_uc: str = "UC-A",
    *,
    case_passed: bool = True,
    l1_results: list[dict] | None = None,
    tier2_mandatory_failures: list[str] | None = None,
    tier2_advisory_failures: list[str] | None = None,
) -> dict:
    """Construct a minimal case_results[i] entry."""
    per_step = []
    if tier2_mandatory_failures:
        for sid in tier2_mandatory_failures:
            per_step.append({
                "step_id": sid,
                "severity": "mandatory",
                "outcome": "FAIL",
                "desc": "",
                "detail": "",
            })
    if tier2_advisory_failures:
        for sid in tier2_advisory_failures:
            per_step.append({
                "step_id": sid,
                "severity": "advisory",
                "outcome": "FAIL",
                "desc": "",
                "detail": "",
            })
    return {
        "case_id": case_id,
        "primary_uc": primary_uc,
        "case_passed": case_passed,
        "case_passed_authority": "programmatic",
        "l1_results": l1_results
        or [
            {"check": "no_pii_leakage", "passed": True, "detail": ""},
            {"check": "no_critical_policy_violation", "passed": True, "detail": ""},
            {"check": "escalation_compliance", "passed": True, "detail": ""},
            {"check": "phase_transition_validity", "passed": True, "detail": ""},
            {"check": "no_human_only_tool_exposure", "passed": True, "detail": ""},
        ],
        "tier2_result": {
            "passed": not bool(tier2_mandatory_failures),
            "severity": "mandatory" if tier2_mandatory_failures else "advisory",
            "failed_step_ids": list((tier2_mandatory_failures or []) + (tier2_advisory_failures or [])),
            "per_step": per_step,
        },
        "failure_tags": [],
    }


def _write_suite(
    suite_dir: Path,
    cases: list[dict],
    *,
    tier_breakdown: dict | None = None,
) -> Path:
    """Write a per-suite results.json. Returns the path."""
    suite_dir.mkdir(parents=True, exist_ok=True)
    payload = {
        "run_id": suite_dir.name,
        "case_results": cases,
        "summary": {"total_cases": len(cases)},
    }
    if tier_breakdown is not None:
        payload["tier_breakdown"] = tier_breakdown
    p = suite_dir / "results.json"
    p.write_text(json.dumps(payload), encoding="utf-8")
    return p


def _baseline_from_dir(tmp_path: Path, *, current: dict[str, list[dict]]) -> BaselineSnapshot:
    """Helper: build a BaselineSnapshot directly via baseline_loader.load
    after writing synthetic per-suite results.json files. Each
    `current[suite_name]` is the list of case_results.
    """
    from autoloop.scoring import load
    base_dir = tmp_path / "baseline"
    base_dir.mkdir(parents=True, exist_ok=True)
    for suite_name, cases in current.items():
        _write_suite(base_dir / suite_name, cases)
    return load(base_dir, config=DEFAULT_CONFIG)


def _current_dir(tmp_path: Path, *, suites: dict[str, list[dict]]) -> Path:
    """Helper: write a `current_results/` directory with per-suite subdirs."""
    cur_dir = tmp_path / "current"
    cur_dir.mkdir(parents=True, exist_ok=True)
    for suite_name, cases in suites.items():
        _write_suite(cur_dir / suite_name, cases)
    return cur_dir


# --- Layer 0 fixtures ------------------------------------------------


def test_layer0_clean_pass(tmp_path: Path):
    """All Tier-0 Python family checks PASS → Layer 0 passes; remaining
    layers still evaluated (improvement may or may not gate)."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01"), _make_case("sh02")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01"), _make_case("sh02")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[0].passed is True
    assert v.layer_results[0].name == "tier0_safety"


def test_layer0_python_family_discard_on_pii(tmp_path: Path):
    """A no_pii_leakage failure in current → Layer 0 discards."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    bad_case = _make_case("bc01", l1_results=[
        {"check": "no_pii_leakage", "passed": False, "detail": "PII leaked"},
    ])
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [bad_case],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[0].passed is False
    assert "no_pii_leakage" in v.discard_reason
    assert "bc01" in v.discard_reason


def test_layer0_java_replay_critical_policy_violation_discard(tmp_path: Path):
    """A `tier_breakdown.critical_policy_violation > 0` in current's
    results.json → Layer 0 discards. (Java replay gate path; this
    only fires when results.json HAPPENS to carry tier_breakdown.)"""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur_dir = tmp_path / "current"
    cur_dir.mkdir(parents=True, exist_ok=True)
    _write_suite(
        cur_dir / "bad_cases",
        [_make_case("bc01")],
        tier_breakdown={"critical_policy_violation": 1, "wrong_containment": 0.0},
    )
    _write_suite(cur_dir / "anchor_outcome", [_make_case("ao01")])
    _write_suite(cur_dir / "shadow", [_make_case("sh01")])
    v = evaluate(cur_dir, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[0].passed is False
    assert "critical_policy_violation" in v.discard_reason


# --- Layer 1 fixtures ------------------------------------------------


def test_layer1_bad_cases_regression_discard(tmp_path: Path):
    """bad_cases passed 5/12 → 4/12 must discard at Layer 1."""
    bc_baseline = [_make_case(f"bc{i:02d}", case_passed=(i < 5)) for i in range(12)]
    bc_current = [_make_case(f"bc{i:02d}", case_passed=(i < 4)) for i in range(12)]
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": bc_baseline,
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": bc_current,
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_bad_cases_regression_5_to_4" in v.discard_reason


def test_layer1_anchor_outcome_regression_discard(tmp_path: Path):
    """anchor_outcome 10/12 → 9/12 discards (max_drop=0 default)."""
    ao_baseline = [_make_case(f"ao{i:02d}", case_passed=(i < 10)) for i in range(12)]
    ao_current = [_make_case(f"ao{i:02d}", case_passed=(i < 9)) for i in range(12)]
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": ao_baseline,
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": ao_current,
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_anchor_outcome_regression_10_to_9" in v.discard_reason


def test_layer1_clean_pass_proceeds_to_higher_layers(tmp_path: Path):
    """No regression at Layer 1 → tier breakdown advances to Layers 2+."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],  # improvement!
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True


# --- Layer 2 fixtures ------------------------------------------------


def test_layer2_aggregate_increase_discard(tmp_path: Path):
    """Tier-2 mandatory failure count rising in aggregate discards."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        # NEW mandatory failure introduced → aggregate goes 0 → 1.
        "bad_cases": [_make_case("bc01", tier2_mandatory_failures=["s1"])],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[2].passed is False
    assert "tier2_critical_flow_regression_aggregate" in v.discard_reason


def test_layer2_per_uc_increase_discard(tmp_path: Path):
    """Aggregate stays flat but UC-H goes 1 → 2 → discards."""
    baseline = _baseline_from_dir(tmp_path, current={
        # 1 mandatory failure in UC-H and 1 in UC-A (aggregate=2).
        "bad_cases": [
            _make_case("bc01", primary_uc="UC-H", tier2_mandatory_failures=["s1"]),
            _make_case("bc02", primary_uc="UC-A", tier2_mandatory_failures=["s2"]),
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        # 2 mandatory failures in UC-H, 0 in UC-A (aggregate still 2;
        # UC-H rose 1→2).
        "bad_cases": [
            _make_case("bc01", primary_uc="UC-H", tier2_mandatory_failures=["s1", "s3"]),
            _make_case("bc02", primary_uc="UC-A"),
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[2].passed is False
    assert "tier2_critical_flow_regression" in v.discard_reason
    assert "UC-H" in v.discard_reason


def test_layer2_clean_pass_proceeds(tmp_path: Path):
    """Tier-2 flat → Layer 2 passes; verdict continues into Layer 3."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[2].passed is True


# --- Layer 3 fixtures ------------------------------------------------


def test_layer3_bad_cases_improvement_keeps(tmp_path: Path):
    """bad_cases +1 → keep."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [
            _make_case("bc01", case_passed=False),
            _make_case("bc02", case_passed=False),
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [
            _make_case("bc01", case_passed=True),  # +1 improvement
            _make_case("bc02", case_passed=False),
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"
    assert v.layer_results[3].passed is True


def test_layer3_anchor_outcome_improvement_keeps(tmp_path: Path):
    """anchor_outcome +1 → keep."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [
            _make_case("ao01", case_passed=False),
            _make_case("ao02", case_passed=True),
        ],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [
            _make_case("ao01", case_passed=True),  # +1
            _make_case("ao02", case_passed=True),
        ],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"


def test_layer3_tier2_mandatory_reduction_keeps(tmp_path: Path):
    """Tier-2 mandatory failure count drops by 1 → keep."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [
            _make_case("bc01", tier2_mandatory_failures=["s1"]),
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [
            _make_case("bc01"),  # mandatory failure removed
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"


def test_layer3_no_improvement_discards(tmp_path: Path):
    """Every signal flat → discard at Layer 3."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[3].passed is False
    assert "improvement_threshold_not_met" in v.discard_reason


# --- Layer 4 fixtures ------------------------------------------------


def test_layer4_shadow_regression_above_threshold_discard(tmp_path: Path):
    """Shadow drops 5% (> 3% threshold) → discard at Layer 4."""
    sh_baseline = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(20)]
    # 19/20 = 95% baseline; 18/20 = 90% current → drop 5%
    sh_baseline[0]["case_passed"] = False  # baseline 19/20
    sh_current = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(20)]
    sh_current[0]["case_passed"] = False
    sh_current[1]["case_passed"] = False  # current 18/20
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],  # improve so we reach L4
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_current,
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[4].passed is False
    assert "shadow_regression_drop" in v.discard_reason


def test_layer4_shadow_within_threshold_keeps(tmp_path: Path):
    """Shadow drops 2% (< 3% threshold) → keep."""
    # baseline: 50/50 = 100%; current: 49/50 = 98% → drop 2%
    sh_baseline = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(50)]
    sh_current = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(50)]
    sh_current[0]["case_passed"] = False
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_current,
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"
    assert v.layer_results[4].passed is True


def test_layer4_shadow_missing_keeps_with_warning(tmp_path: Path):
    """When shadow data is missing entirely, Layer 4 is skipped (warning recorded)."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        # no shadow in baseline
    })
    cur_dir = tmp_path / "current"
    cur_dir.mkdir()
    _write_suite(cur_dir / "bad_cases", [_make_case("bc01", case_passed=True)])
    _write_suite(cur_dir / "anchor_outcome", [_make_case("ao01")])
    # no shadow in current
    v = evaluate(cur_dir, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"
    assert v.layer_results[4].passed is True
    assert "warning" in v.layer_results[4].metrics_observed
    assert "shadow" in v.layer_results[4].metrics_observed["warning"]


# --- Adversarial: lexicographic correctness --------------------------


def test_lexicographic_correctness_layer2_blocks_layer3_improvement(tmp_path: Path):
    """Constitution §1.6 invariant — improving bad_cases (Layer 3 signal)
    MUST NOT compensate for a Tier-2 (Layer 2) regression. The
    verdict MUST discard at Layer 2 even though Layer 3 would
    otherwise have been satisfied.
    """
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [
            _make_case("bc01", case_passed=False),  # baseline 0 passed
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [
            # bc01 now passes (Layer 3 IMPROVEMENT) but also introduces
            # a NEW mandatory Tier-2 failure (Layer 2 REGRESSION).
            _make_case(
                "bc01",
                case_passed=True,
                tier2_mandatory_failures=["new_step_failure"],
            ),
        ],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[2].passed is False
    assert "tier2_critical_flow_regression" in v.discard_reason
    # Layer 3 must NOT have been evaluated (short-circuited at L2)
    assert v.layer_results[3].passed is None
    assert "not_evaluated_short_circuit_at_layer_2" in v.layer_results[3].reason


# --- Shadow firewall test --------------------------------------------


def test_shadow_firewall_default_api_no_per_case_data(tmp_path: Path):
    """Default API (audit=False) — Layer 4 LayerResult.metrics_observed
    contains ONLY aggregate keys; per-case shadow data NEVER leaks.
    The tier_breakdown surface likewise carries only aggregate keys.
    """
    sh_baseline = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(10)]
    sh_current = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(10)]
    sh_current[0]["case_passed"] = False
    sh_current[0]["case_id"] = "shadow_secret_case_id"
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_current,
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert isinstance(v, LexicographicVerdict)
    l4 = v.layer_results[4]
    aggregate_only_keys = {
        "baseline_pass_rate",
        "current_pass_rate",
        "drop_pct",
        "regression_detected",
        "max_drop_pct",
        "warning",
    }
    observed_keys = set(l4.metrics_observed.keys())
    extra = observed_keys - aggregate_only_keys
    assert not extra, f"L4 leaked non-aggregate keys: {extra}"
    # No per-case case_id leakage anywhere in the verdict.
    rendered = json.dumps(_to_dict(v))
    assert "shadow_secret_case_id" not in rendered


def test_shadow_firewall_audit_api_returns_per_case_detail(tmp_path: Path):
    """Audit API (audit=True) — returns (verdict, ShadowAuditDetail);
    the AuditDetail object carries per-case shadow info.
    """
    sh_baseline = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(10)]
    sh_current = [_make_case(f"sh{i:02d}", case_passed=True) for i in range(10)]
    sh_current[0]["case_passed"] = False
    sh_current[0]["case_id"] = "shadow_audit_marker_case"
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": sh_current,
    })
    result = evaluate(cur, baseline, config=DEFAULT_CONFIG, audit=True)
    assert isinstance(result, tuple)
    verdict, audit = result
    assert isinstance(verdict, LexicographicVerdict)
    assert isinstance(audit, ShadowAuditDetail)
    case_ids = [c.get("case_id") for c in audit.per_case_failures]
    assert "shadow_audit_marker_case" in case_ids


# --- Short-circuit semantics -----------------------------------------


def test_short_circuit_layer0_marks_higher_layers_not_evaluated(tmp_path: Path):
    """Layer 0 fail → Layers 1..4 carry passed=None + standard reason."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01")],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    bad_case = _make_case("bc01", l1_results=[
        {"check": "no_pii_leakage", "passed": False, "detail": "leak"},
    ])
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [bad_case],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[0].passed is False
    for i in range(1, 5):
        assert v.layer_results[i].passed is None
        assert v.layer_results[i].reason == "not_evaluated_short_circuit_at_layer_0"


# --- Misc helpers ----------------------------------------------------


def _to_dict(v: LexicographicVerdict) -> dict:
    return {
        "decision": v.decision,
        "discard_reason": v.discard_reason,
        "tier_breakdown": v.tier_breakdown,
        "layer_results": [
            {
                "layer": lr.layer,
                "name": lr.name,
                "passed": lr.passed,
                "reason": lr.reason,
                "metrics_observed": lr.metrics_observed,
            }
            for lr in v.layer_results
        ],
    }


# --- S-Auto-16: majority-awareness (n>1) -----------------------------


def _agg_case(
    case_id: str,
    *,
    majority_passed: bool | None,
    comparable: bool = True,
    primary_uc: str = "UC-A",
    tier0_majority: dict | None = None,
    tier2_majority_steps: list[str] | None = None,
) -> dict:
    """A case carrying S-Auto-16 aggregation fields (the n>1 shape)."""
    base = _make_case(
        case_id,
        primary_uc,
        case_passed=bool(majority_passed) if majority_passed is not None else False,
    )
    base["majority_passed"] = majority_passed
    base["comparable"] = comparable
    base["pass_rate"] = None
    base["valid_attempts"] = 3 if comparable else 1
    base["aggregate_status"] = (
        "non_comparable" if not comparable
        else ("stable_pass" if majority_passed else "stable_fail")
    )
    if tier0_majority is not None:
        base["tier0_majority"] = tier0_majority
    if tier2_majority_steps is not None:
        base["tier2_result_majority"] = {
            "per_step": [
                {"step_id": s, "severity": "mandatory", "outcome": "FAIL"}
                for s in tier2_majority_steps
            ]
        }
    return base


def test_majority_l1_flaky_minority_fail_does_not_gate(tmp_path: Path):
    """A bad_case that passed in baseline and whose candidate MAJORITY
    still passes (a minority flaky fail) does NOT regress Layer 1."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=True)],  # 2/3 pass
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True


def test_majority_l1_stable_majority_fail_gates(tmp_path: Path):
    """A bad_case that passed in baseline and whose candidate MAJORITY
    fails (stable reproduction) DOES regress Layer 1."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=False)],  # 2/3 fail
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_bad_cases_regression" in v.discard_reason


def test_majority_l1_non_comparable_does_not_gate(tmp_path: Path):
    """A baseline-passing bad_case that becomes non-comparable this run is
    excluded from BOTH counts, so it is not scored as a regression."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=True),
                      _make_case("bc02", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=True),
                      _agg_case("bc02", majority_passed=None, comparable=False)],
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    # bc02 non-comparable → baseline credit 1 → effective baseline 1 == current 1.
    assert v.layer_results[1].passed is True
    assert v.tier_breakdown["tier1_outcome"]["bad_cases"]["non_comparable_excluded"] == 1


def test_majority_l0_minority_violation_does_not_discard(tmp_path: Path):
    """A Tier-0 violation in the MINORITY of candidate samples
    (tier0_majority keeps it passed) does NOT trip Layer 0."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=True,
                                tier0_majority={"no_pii_leakage": True})],
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[0].passed is True
    assert v.tier_breakdown["tier0_safety"]["python_tier0_family"]["stable_reproduction"] is True


def test_majority_l0_majority_violation_discards(tmp_path: Path):
    """A Tier-0 violation reproduced in the MAJORITY of candidate samples
    (and absent from baseline) trips Layer 0."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=False,
                                tier0_majority={"no_pii_leakage": False})],
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[0].passed is False
    assert "no_pii_leakage" in v.discard_reason
    assert "bc01" in v.discard_reason


def test_majority_l3_improvement_uses_majority(tmp_path: Path):
    """Layer 3 improvement counts the candidate MAJORITY pass."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=True)],  # majority improvement
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[3].passed is True
    assert v.decision == "keep"


def test_n1_unanimous_majority_reproduces_single_draw_verdict(tmp_path: Path):
    """n=1 invariance proxy: an aggregated case set whose every majority
    equals the single draw (unanimous, comparable) produces an IDENTICAL
    verdict to the plain single-draw shape. Proves the majority path does
    not diverge from the pre-sprint evaluator when attempts agree."""
    # Plain (n=1) candidate.
    baseline = _baseline_from_dir(tmp_path / "a", current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01", case_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    plain = _current_dir(tmp_path / "a", suites={
        "bad_cases": [_make_case("bc01", case_passed=True)],
        "anchor_outcome": [_make_case("ao01", case_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v_plain = evaluate(plain, baseline, config=DEFAULT_CONFIG)

    # Aggregated (n>1) candidate with the SAME logical outcomes.
    baseline2 = _baseline_from_dir(tmp_path / "b", current={
        "bad_cases": [_make_case("bc01", case_passed=False)],
        "anchor_outcome": [_make_case("ao01", case_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    agg = _current_dir(tmp_path / "b", suites={
        "bad_cases": [_agg_case("bc01", majority_passed=True,
                                tier0_majority={"no_pii_leakage": True})],
        "anchor_outcome": [_agg_case("ao01", majority_passed=True,
                                     tier0_majority={"no_pii_leakage": True})],
        "shadow": [_make_case("sh01")],
    })
    v_agg = evaluate(agg, baseline2, config=DEFAULT_CONFIG)

    assert v_agg.decision == v_plain.decision
    assert v_agg.discard_reason == v_plain.discard_reason
    assert [lr.passed for lr in v_agg.layer_results] == \
           [lr.passed for lr in v_plain.layer_results]


# --- S-Auto-17: baseline-majority symmetry (aggregated baseline) -----


def _agg_baseline_case(
    case_id: str,
    *,
    majority_passed: bool | None,
    pass_rate: float | None = None,
    stability_class: str = "stable",
    primary_uc: str = "UC-A",
) -> dict:
    """An aggregated.json BASELINE case. Carries ONLY `majority_passed`
    (NOT `case_passed`) so a test that passes proves the baseline side reads
    the MAJORITY verdict, not a single draw."""
    return {
        "case_id": case_id,
        "primary_uc": primary_uc,
        "majority_passed": majority_passed,
        "pass_rate": pass_rate,
        "comparable": majority_passed is not None,
        "stability_class": stability_class,
        "l1_results": [
            {"check": "no_pii_leakage", "passed": True, "detail": "majority"},
            {"check": "no_critical_policy_violation", "passed": True, "detail": "majority"},
            {"check": "escalation_compliance", "passed": True, "detail": "majority"},
            {"check": "phase_transition_validity", "passed": True, "detail": "majority"},
            {"check": "no_human_only_tool_exposure", "passed": True, "detail": "majority"},
        ],
        "tier2_result_majority": {"per_step": []},
    }


def _aggregated_baseline(tmp_path: Path, *, suites: dict[str, list[dict]]) -> BaselineSnapshot:
    """Build a BaselineSnapshot from a re-blessed `aggregated.json` layout."""
    from autoloop.scoring import load
    base_dir = tmp_path / "agg_baseline"
    base_dir.mkdir(parents=True, exist_ok=True)
    for suite_name, cases in suites.items():
        suite_dir = base_dir / suite_name
        suite_dir.mkdir(parents=True, exist_ok=True)
        payload = {
            "schema": "autoloop.baseline.aggregated.v1",
            "suite": suite_name,
            "git_commit": "deadbeef",
            "captured_at": "2026-06-03T00:00:00+00:00",
            "n": 5,
            "case_results": cases,
        }
        (suite_dir / "aggregated.json").write_text(json.dumps(payload), encoding="utf-8")
    return load(base_dir, config=DEFAULT_CONFIG)


def test_symmetry_candidate_majority_vs_baseline_majority_keep(tmp_path: Path):
    """Both sides majority at n=3: baseline majority-pass + candidate
    majority-pass (a single noisy minority flip) → Layer 1 does NOT gate."""
    baseline = _aggregated_baseline(tmp_path, suites={
        "bad_cases": [_agg_baseline_case("bc01", majority_passed=True)],
        "anchor_outcome": [_agg_baseline_case("ao01", majority_passed=True)],
        "shadow": [_agg_baseline_case("sh01", majority_passed=True)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=True)],  # 2/3 pass
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True
    assert v.tier_breakdown["tier1_outcome"]["bad_cases"]["baseline_passed"] == 1


def test_symmetry_stable_candidate_regression_vs_baseline_majority_gates(tmp_path: Path):
    """The discriminator: baseline blessed bc01 as a MAJORITY pass; a stable
    candidate majority-FAIL regresses Layer 1. This gates ONLY if the
    baseline side reads `majority_passed` (=1). If it read the absent
    single-draw `case_passed` (=0) the regression would be masked."""
    baseline = _aggregated_baseline(tmp_path, suites={
        "bad_cases": [_agg_baseline_case("bc01", majority_passed=True)],
        "anchor_outcome": [_agg_baseline_case("ao01", majority_passed=True)],
        "shadow": [_agg_baseline_case("sh01", majority_passed=True)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=False)],  # 2/3 fail
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_bad_cases_regression" in v.discard_reason


def test_symmetry_baseline_majority_fail_no_new_regression(tmp_path: Path):
    """When the re-bless blessed bc01 as a MAJORITY fail (baseline pass
    count 0), a candidate that also majority-fails is NOT a new regression."""
    baseline = _aggregated_baseline(tmp_path, suites={
        "bad_cases": [_agg_baseline_case("bc01", majority_passed=False)],
        "anchor_outcome": [_agg_baseline_case("ao01", majority_passed=True)],
        "shadow": [_agg_baseline_case("sh01", majority_passed=True)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=False)],
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True
    assert v.tier_breakdown["tier1_outcome"]["bad_cases"]["baseline_passed"] == 0


def test_symmetry_noncomparable_credit_uses_baseline_majority(tmp_path: Path):
    """`_baseline_case_pass_map` symmetry: a candidate case that became
    non-comparable is credited against the baseline MAJORITY pass. bc02 is
    non-comparable this run and was a baseline majority-pass → credited, so
    no false regression."""
    baseline = _aggregated_baseline(tmp_path, suites={
        "bad_cases": [_agg_baseline_case("bc01", majority_passed=True),
                      _agg_baseline_case("bc02", majority_passed=True)],
        "anchor_outcome": [_agg_baseline_case("ao01", majority_passed=True)],
        "shadow": [_agg_baseline_case("sh01", majority_passed=True)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=True),
                      _agg_case("bc02", majority_passed=None, comparable=False)],
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True
    assert v.tier_breakdown["tier1_outcome"]["bad_cases"]["non_comparable_excluded"] == 1
