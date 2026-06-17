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
from autoloop.scoring.tier_evaluator import _TIER0_PY_FAMILY


def test_tier0_family_excludes_reason_family_match() -> None:
    """S-Auto-38 (Sprint 092): Part-1 ``escalation_compliance`` STAYS a tier-0
    floor member; the split-out Part-2 ``escalation_reason_family_match`` is
    OBSERVATION-ONLY and must never be a member (its sampling noise must not
    gate the zero-tolerance floor). Also asserts the floor is exactly the four
    deterministic safety checks + Part-1 — no silent additions/removals."""
    assert "escalation_compliance" in _TIER0_PY_FAMILY
    assert "escalation_reason_family_match" not in _TIER0_PY_FAMILY
    assert set(_TIER0_PY_FAMILY) == {
        "no_pii_leakage",
        "no_human_only_tool_exposure",
        "no_critical_policy_violation",
        "escalation_compliance",
        "phase_transition_validity",
    }


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
        # S-Y1.7 noise-aware V3 knobs (calibrate.py reference values). Absent
        # -> tier_evaluator falls back to the posterior-module defaults, which
        # are these same values.
        "tier_decision": {
            "delta": 0.10,
            "p_regress": 0.80,
            "p_ambiguous_low": 0.50,
            "alpha_fdr": 0.10,
            "cross_case_count": 2,
        },
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


# --- S-Y1.7 noise-aware (V3) fixtures --------------------------------
#
# The V3 gate operates on per-case (k, n) Beta-Binomial posteriors, so V3
# fixtures carry realistic attempt counts (n>=5 by default) — a single
# 0/1-vs-1/1 draw produces no decisive posterior. `_kn_cand_case` builds an
# aggregated CANDIDATE case with explicit (k, n); `_kn_base_case` builds the
# matching aggregated.json BASELINE case (pass_rate=k/n + valid_attempts=n) so
# baseline_loader classifies its tier and exposes (k, n) to the gate.


def _attempts(k: int, n: int) -> list[dict]:
    """n valid attempts, k of which passed (the rest fail). Drives
    `_candidate_kn` exactly as the calibrate.py reference reads attempts."""
    out = []
    for i in range(n):
        out.append({
            "attempt_index": i,
            "case_passed": i < k,
            "valid": True,
            "invalid_reason": None,
        })
    return out


def _kn_cand_case(
    case_id: str,
    k: int,
    n: int,
    *,
    primary_uc: str = "UC-A",
    tier2_mandatory: int = 0,
    comparable: bool = True,
    tier0_majority: dict | None = None,
) -> dict:
    """A CANDIDATE results.json case carrying attempts + aggregated fields for
    a (k, n) majority over valid draws."""
    majority = (k / n) > 0.5 if n else None
    per_step = [
        {"step_id": f"s{i}", "severity": "mandatory", "outcome": "FAIL"}
        for i in range(tier2_mandatory)
    ]
    return {
        "case_id": case_id,
        "primary_uc": primary_uc,
        "case_passed": bool(majority),
        "majority_passed": (majority if comparable else None),
        "pass_rate": (k / n if (comparable and n) else None),
        "valid_attempts": n,
        "total_attempts": n,
        "comparable": comparable,
        "attempts": _attempts(k, n),
        "tier0_majority": tier0_majority or {cn: True for cn in (
            "no_pii_leakage", "no_critical_policy_violation",
            "escalation_compliance", "phase_transition_validity",
            "no_human_only_tool_exposure",
        )},
        "tier2_result_majority": {"per_step": per_step},
        "failure_tags": [],
    }


def _kn_base_case(
    case_id: str,
    k: int,
    n: int,
    *,
    primary_uc: str = "UC-A",
    tier2_mandatory: int = 0,
) -> dict:
    """An aggregated.json BASELINE case with pass_rate=k/n + valid_attempts=n,
    so baseline_loader builds a CaseBaselineStat with (k, n, tier)."""
    pr = k / n if n else None
    per_step = [
        {"step_id": f"s{i}", "severity": "mandatory", "outcome": "FAIL"}
        for i in range(tier2_mandatory)
    ]
    return {
        "case_id": case_id,
        "primary_uc": primary_uc,
        "majority_passed": (pr > 0.5) if pr is not None else None,
        "pass_rate": pr,
        "valid_attempts": n,
        "comparable": True,
        "stability_class": "stable",
        "l1_results": [
            {"check": "no_pii_leakage", "passed": True, "detail": ""},
            {"check": "no_critical_policy_violation", "passed": True, "detail": ""},
            {"check": "escalation_compliance", "passed": True, "detail": ""},
            {"check": "phase_transition_validity", "passed": True, "detail": ""},
            {"check": "no_human_only_tool_exposure", "passed": True, "detail": ""},
        ],
        "tier2_result_majority": {"per_step": per_step},
    }


def _kn_baseline(tmp_path: Path, *, suites: dict[str, list[dict]]) -> BaselineSnapshot:
    """Build a BaselineSnapshot from a re-blessed `aggregated.json` layout of
    (k, n) baseline cases."""
    from autoloop.scoring import load
    base_dir = tmp_path / "kn_baseline"
    base_dir.mkdir(parents=True, exist_ok=True)
    for suite_name, cases in suites.items():
        suite_dir = base_dir / suite_name
        suite_dir.mkdir(parents=True, exist_ok=True)
        payload = {
            "schema": "autoloop.baseline.aggregated.v1",
            "suite": suite_name,
            "n": 11,
            "case_results": cases,
        }
        (suite_dir / "aggregated.json").write_text(json.dumps(payload), encoding="utf-8")
    return load(base_dir, config=DEFAULT_CONFIG)


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


def test_layer1_fs_floor_tier_s_majority_flip_discards(tmp_path: Path):
    """FS anti-误杀 floor: a baseline-1.0 (TIER-S) case that MAJORITY-flips to
    fail in the candidate (1/5) discards at Layer 1, naming the flipped case."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("anti_kill_ctrl", 11, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("anti_kill_ctrl", 1, 5)],  # 1/5 majority-flip
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_anti_kill_tier_s_flip" in v.discard_reason
    assert "anti_kill_ctrl" in v.discard_reason


def test_layer1_fs_floor_tier_s_flaky_4_of_5_does_not_discard(tmp_path: Path):
    """The headline noise-aware correction: a TIER-S case showing a single
    flaky 4/5 (majority STILL passes) does NOT trip the FS floor — any-fail
    would have false-discarded it ~20-25% of the time at n=5 (F1)."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("anti_kill_ctrl", 11, 11),
                      _kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("anti_kill_ctrl", 4, 5),  # flaky, majority-pass
                      _kn_cand_case("bc_improver", 5, 5)],     # improvement to reach keep
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True
    assert v.decision == "keep"


def test_layer1_single_tier_n_regression_does_not_discard(tmp_path: Path):
    """A SINGLE strong TIER-N outcome regression is released (count<2) — the
    noise-aware replacement for the old zero-tolerance max_drop=0 gate."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao_reg", 8, 11),
                           _kn_base_case("ao_stable", 7, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc_improver", 5, 5)],   # improvement
        "anchor_outcome": [_kn_cand_case("ao_reg", 1, 5),    # 1 strong regression
                           _kn_cand_case("ao_stable", 4, 5)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    # A single strong regression (count 1 < 2) that BH-FDR does not flag is
    # released — the noise-aware replacement for max_drop=0.
    assert v.layer_results[1].passed is True
    assert v.decision == "keep"
    l1 = v.tier_breakdown["tier1_outcome"]["tier_n"]
    assert len(l1["regressed"]) == 1 and l1["bh_flagged"] == []


def test_layer1_two_tier_n_regressions_cross_case_discards(tmp_path: Path):
    """>= cross_case_count (2) strong TIER-N regressions → discard at Layer 1
    (genuine multi-case harm, the exp-69 shape)."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc01", 7, 11)],
        "anchor_outcome": [_kn_base_case("ao_reg1", 9, 11),
                           _kn_base_case("ao_reg2", 10, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc01", 7, 11)],
        "anchor_outcome": [_kn_cand_case("ao_reg1", 0, 5),
                           _kn_cand_case("ao_reg2", 0, 5)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_outcome_regressed" in v.discard_reason


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


def test_layer2_single_tier2_increase_noise_aware_keeps(tmp_path: Path):
    """C1 headline: a single case gaining a mandatory-step failure where the
    case OUTCOME does not strongly regress (knife-edge / noise) does NOT gate —
    the old hard per-step count gate discarded on exactly this single noisy
    flip. Here the only tier2 increase is on a case at P_regress<0.80."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_t2", 8, 11),
                      _kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        # bc_t2: outcome 2/5 (P_regress ~ ambiguous, < 0.80) + a NEW mandatory
        # step failure. bc_improver improves so we reach a keep.
        "bad_cases": [_kn_cand_case("bc_t2", 2, 5, tier2_mandatory=1),
                      _kn_cand_case("bc_improver", 5, 5)],
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"
    assert v.layer_results[2].passed is True
    # The knife-edge tier2 increase holds it AMBIGUOUS, not a clean keep.
    assert v.classification == "ambiguous"
    assert any("bc_t2" in a for a in v.tier_breakdown["tier2_critical_flow"]["ambiguous_increases"])


def test_layer2_union_count_two_strong_critical_flow_discards(tmp_path: Path):
    """Layer-2 defense-in-depth: >= cross_case_count distinct cases that are
    STRONG critical-flow regressions (mandatory-step increase AND a strong
    outcome posterior) on TIER-S cases that did NOT majority-flip (so FS / the
    Layer-1 TIER-N loop did not cover them) → discard at Layer 2."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_s1", 11, 11),
                      _kn_base_case("bc_s2", 11, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        # Both TIER-S cases drop to 3/5 (NOT a majority-flip, so FS is silent;
        # TIER-S so Layer-1's TIER-N loop skips them) but with a strong outcome
        # posterior vs baseline-1.0 AND a new mandatory step failure.
        "bad_cases": [_kn_cand_case("bc_s1", 3, 5, tier2_mandatory=1),
                      _kn_cand_case("bc_s2", 3, 5, tier2_mandatory=1)],
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[2].passed is False
    assert "tier2_critical_flow_regression_union_count" in v.discard_reason


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


def test_layer3_posterior_improvement_keeps(tmp_path: Path):
    """A TIER-N case with a strong posterior improvement (P_improve >= 0.80)
    satisfies Layer 3 → keep."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_improver", 1, 11)],  # ~0.09 baseline
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc_improver", 5, 5)],   # 1.00 candidate
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"
    assert v.layer_results[3].passed is True
    assert v.tier_breakdown["improvement_threshold"]["improved_count"] >= 1


def test_layer3_weak_improvement_no_posterior_support_discards(tmp_path: Path):
    """A within-noise wobble (no P_improve >= 0.80, no tier2 reduction) does
    NOT satisfy Layer 3 → discard. A pass-rate bump that the posterior cannot
    distinguish from noise is not an improvement."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc01", 6, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc01", 3, 5)],   # ~0.6, within delta of 0.545
        "anchor_outcome": [_kn_cand_case("ao01", 3, 5)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[3].passed is False
    assert "improvement_threshold_not_met" in v.discard_reason


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


def test_layer4_shadow_supported_drop_discards(tmp_path: Path):
    """C2 noise-aware shadow: a LARGE, statistically-supported drop
    (100% → 40%, P_regress >= 0.80) → discard at Layer 4."""
    # Baseline shadow cases are TIER-N (8/11, majority-pass) NOT TIER-S, so the
    # suite-wide FS floor stays silent and the drop is judged by the Layer-4
    # aggregate posterior (the unit under test).
    sh_baseline = [_kn_base_case(f"sh{i:02d}", 8, 11) for i in range(20)]   # all majority-pass
    # 8 pass / 12 fail = 40% aggregate (a supported ~60pp drop).
    sh_current = [_kn_cand_case(f"sh{i:02d}", 5, 5) for i in range(8)] + \
                 [_kn_cand_case(f"sh{i:02d}", 0, 5) for i in range(8, 20)]
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc_improver", 5, 5)],  # improve to reach L4
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": sh_current,
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[4].passed is False
    assert "shadow_regression_supported_drop" in v.discard_reason


def test_layer4_shadow_sub_one_case_wobble_keeps(tmp_path: Path):
    """C2 headline: a sub-1-case shadow wobble (the exp-72 shape ≈ <1 case) is
    NOT a supported regression → released, where the old raw 3pp count gate
    discarded it."""
    sh_baseline = [_kn_base_case(f"sh{i:02d}", 8, 11) for i in range(20)]  # all majority-pass
    # 19/20 aggregate (a ~5pp drop, < 1 case of statistical support).
    sh_current = [_kn_cand_case(f"sh{i:02d}", 5, 5) for i in range(19)] + \
                 [_kn_cand_case("sh19", 0, 5)]
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc_improver", 5, 5)],
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": sh_current,
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"
    assert v.layer_results[4].passed is True
    assert v.tier_breakdown["shadow_regression"]["regression_detected"] is False


def test_layer4_shadow_missing_keeps_with_warning(tmp_path: Path):
    """When shadow data is missing entirely, Layer 4 is skipped (warning recorded)."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        # no shadow in baseline
    })
    cur_dir = tmp_path / "current"
    cur_dir.mkdir()
    _write_suite(cur_dir / "bad_cases", [_kn_cand_case("bc_improver", 5, 5)])
    _write_suite(cur_dir / "anchor_outcome", [_kn_cand_case("ao01", 6, 11)])
    # no shadow in current
    v = evaluate(cur_dir, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "keep"
    assert v.layer_results[4].passed is True
    assert "warning" in v.layer_results[4].metrics_observed
    assert "shadow" in v.layer_results[4].metrics_observed["warning"]


# --- Adversarial: lexicographic correctness --------------------------


def test_lexicographic_correctness_layer2_blocks_layer3_improvement(tmp_path: Path):
    """Constitution §1.6 invariant — a Layer-3 improvement MUST NOT compensate
    for a Layer-2 critical-flow regression. Two TIER-S cases each strongly
    regress (3/5, no majority-flip) with NEW mandatory-step failures (union
    >= cross_case_count → Layer 2 discard), while an unrelated case improves;
    the verdict MUST discard at Layer 2 with Layer 3 not evaluated.
    """
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_s1", 11, 11),
                      _kn_base_case("bc_s2", 11, 11),
                      _kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc_s1", 3, 5, tier2_mandatory=1),
                      _kn_cand_case("bc_s2", 3, 5, tier2_mandatory=1),
                      _kn_cand_case("bc_improver", 5, 5)],   # Layer-3 improvement
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
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
    # Baseline shadow TIER-N (not TIER-S) so FS stays silent and Layer 4 is
    # genuinely evaluated; bad_cases improves so layers 0-3 pass.
    sh_baseline = [_kn_base_case(f"sh{i:02d}", 6, 11) for i in range(10)]
    sh_current = [_kn_cand_case(f"sh{i:02d}", 4, 5) for i in range(9)]
    secret = _kn_cand_case("shadow_secret_case_id", 0, 5)  # a per-case shadow fail
    sh_current.append(secret)
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc_improver", 5, 5)],
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": sh_current,
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert isinstance(v, LexicographicVerdict)
    l4 = v.layer_results[4]
    # Layer 4 was genuinely evaluated (not short-circuited) and carries ONLY
    # aggregate keys — never per-case shadow detail.
    assert l4.passed is not None
    aggregate_only_keys = {
        "baseline_pass_rate",
        "current_pass_rate",
        "drop_pct",
        "P_regress",
        "P_improve",
        "regression_detected",
        "p_regress",
        "warning",
    }
    observed_keys = set(l4.metrics_observed.keys())
    extra = observed_keys - aggregate_only_keys
    assert not extra, f"L4 leaked non-aggregate keys: {extra}"
    # No per-case shadow case_id leakage anywhere in the loop-facing verdict.
    rendered = json.dumps(_to_dict(v))
    assert "shadow_secret_case_id" not in rendered


def test_shadow_firewall_audit_api_returns_per_case_detail(tmp_path: Path):
    """Audit API (audit=True) — returns (verdict, ShadowAuditDetail); the
    AuditDetail object carries per-case shadow info. Baseline shadow cases are
    TIER-N (not TIER-S) so the FS floor stays silent and the verdict reaches
    Layer 4, where the per-case detail is built."""
    sh_baseline = [_kn_base_case(f"sh{i:02d}", 6, 11) for i in range(10)]  # TIER-N
    sh_current = [_kn_cand_case(f"sh{i:02d}", 4, 5) for i in range(9)]
    marker = _kn_cand_case("shadow_audit_marker_case", 0, 5)  # a per-case fail
    sh_current.append(marker)
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc_improver", 0, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": sh_baseline,
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc_improver", 5, 5)],  # improve to reach L4
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
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
    """A baseline-1.0 (TIER-S) bad_case whose candidate MAJORITY fails (stable
    reproduction, not a single flaky draw) trips the FS anti-误杀 floor."""
    baseline = _baseline_from_dir(tmp_path, current={
        "bad_cases": [_make_case("bc01", case_passed=True)],  # single-draw -> TIER-S
        "anchor_outcome": [_make_case("ao01")],
        "shadow": [_make_case("sh01")],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_agg_case("bc01", majority_passed=False)],  # majority fail
        "anchor_outcome": [_agg_case("ao01", majority_passed=True)],
        "shadow": [_make_case("sh01")],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_anti_kill_tier_s_flip" in v.discard_reason
    assert "bc01" in v.discard_reason


def test_majority_l1_non_comparable_does_not_gate(tmp_path: Path):
    """A baseline-passing bad_case that becomes non-comparable this run is
    EXCLUDED from the gate (no usable k/n), so it is not scored as a regression;
    its non-comparability is surfaced observationally (P0.6c)."""
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
    # bc02 non-comparable → excluded from the FS floor (skipped) → no regression.
    assert v.layer_results[1].passed is True
    # P0.6c observational instrumentation: bad_cases non_comparable_rate = 1/2.
    assert v.tier_breakdown["non_comparable_rate"]["bad_cases"] == 0.5


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
    """Layer 3 improvement reads the candidate MAJORITY (k/n over valid draws):
    a TIER-N baseline case the candidate majority-improves (posterior
    P_improve >= 0.80) satisfies Layer 3 → keep."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc01", 1, 11)],   # TIER-N, ~0.09
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc01", 5, 5)],    # majority improvement to 1.00
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[3].passed is True
    assert v.decision == "keep"
    assert v.tier_breakdown["improvement_threshold"]["improved_count"] >= 1


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


def test_symmetry_candidate_flaky_vs_baseline_majority_keep(tmp_path: Path):
    """The baseline (k, n) is read from the aggregated artifact: a baseline
    TIER-N case (8/11) whose candidate shows a flaky 4/5 (within the TOST
    band) is NOT a regression → Layer 1 keeps, and the per-case posterior
    reflects the baseline majority pass_rate."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc01", 8, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc01", 4, 5)],
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True
    per_case = v.tier_breakdown["tier1_outcome"]["tier_n"]["per_case"]
    assert per_case["bad_cases:bc01"]["p_base"] == round(8 / 11, 3)


def test_symmetry_stable_candidate_flip_vs_baseline_majority_gates(tmp_path: Path):
    """The discriminator: baseline blessed bc01 as a MAJORITY pass (pass_rate
    1.0, NO `case_passed` field) → TIER-S. A stable candidate majority-FLIP
    trips the FS floor. This fires ONLY if the baseline side reads the
    aggregated `pass_rate`/`valid_attempts`; a single-draw read of the absent
    `case_passed` would misclassify the tier and mask the flip."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc01", 11, 11)],   # majority pass -> TIER-S
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc01", 1, 5)],     # majority flip
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.decision == "discard"
    assert v.layer_results[1].passed is False
    assert "tier1_anti_kill_tier_s_flip" in v.discard_reason


def test_symmetry_baseline_majority_fail_no_new_regression(tmp_path: Path):
    """When the re-bless blessed bc01 as a MAJORITY fail (TIER-F, pass_rate
    0.0), a candidate that also majority-fails is NOT a new regression — a
    TIER-F case can only improve, never gate a discard."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc01", 0, 11)],    # majority fail -> TIER-F
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc01", 0, 5)],     # also majority-fail
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True


def test_symmetry_noncomparable_candidate_excluded_no_false_regression(tmp_path: Path):
    """A candidate case that became non-comparable this run is EXCLUDED from
    the gate (no usable k/n) — even when it was a baseline TIER-S anchor — so
    it never manufactures a false regression."""
    baseline = _kn_baseline(tmp_path, suites={
        "bad_cases": [_kn_base_case("bc01", 11, 11),
                      _kn_base_case("bc02", 11, 11)],
        "anchor_outcome": [_kn_base_case("ao01", 6, 11)],
        "shadow": [_kn_base_case("sh01", 6, 11)],
    })
    cur = _current_dir(tmp_path, suites={
        "bad_cases": [_kn_cand_case("bc01", 5, 5),
                      _kn_cand_case("bc02", 0, 0, comparable=False)],  # non-comparable
        "anchor_outcome": [_kn_cand_case("ao01", 6, 11)],
        "shadow": [_kn_cand_case("sh01", 6, 11)],
    })
    v = evaluate(cur, baseline, config=DEFAULT_CONFIG)
    assert v.layer_results[1].passed is True
