"""S-Auto-42 — bool-only INCONCLUSIVE_FLAKY tier0 defer rule
(R-autoloop-fitness-measurement-reliability, safe core; no posterior, no
re-sampling). Deterministic + non-flaky tier0 behaviour is UNCHANGED (the
existing test_tier_evaluator.py golden suite is the proof). These tests cover the
new flaky-case DEFER path and the HOLD_INCONCLUSIVE_FLAKY verdict (never KEEP).

Per-check (k,n) for a posterior is NOT reliably persisted (tier0_majority is a
bool only; failure_tags undercount; the frozen baseline carries no counts — see
docs/diagnostics/autoloop-tier0-flaky-posterior-data-blocker-2026-06-19.md), so
this first pass uses the persisted bool signals only: a baseline-flagged flaky
tier0 violation is DEFERRED (HOLD, never KEEP, never auto-discard).
"""

from __future__ import annotations

import json
from pathlib import Path

from autoloop.scoring import load
from autoloop.scoring.tier_evaluator import evaluate

_PY = (
    "no_pii_leakage", "no_critical_policy_violation", "escalation_compliance",
    "phase_transition_validity", "no_human_only_tool_exposure",
)
_CFG = {"fitness": {
    "suites": [
        {"name": "bad_cases", "path": "x", "parallel": 1},
        {"name": "anchor_outcome", "path": "x", "parallel": 4},
        {"name": "shadow", "path": "x", "parallel": 4},
    ],
    "improvement_threshold_mode": "case_count",
    "improvement_min_cases": 1,
    "tier_decision": {
        "delta": 0.10, "p_regress": 0.80, "p_ambiguous_low": 0.50,
        "alpha_fdr": 0.10, "cross_case_count": 2,
    },
}}


def _l1(esc_passed):
    return [{"check": c, "passed": (esc_passed if c == "escalation_compliance" else True),
             "detail": ""} for c in _PY]


def _base(cid, *, flaky, esc_passed=True, pr=1.0, n=11):
    k = round(pr * n)
    return {
        "case_id": cid, "primary_uc": "UC-J", "pass_rate": pr,
        "valid_attempts": n, "comparable": True, "flaky": flaky,
        "stability_class": "near-coinflip" if flaky else "stable",
        "majority_passed": pr > 0.5,
        "attempts": [{"attempt_index": i, "valid": True, "case_passed": i < k,
                      "failure_tags": []} for i in range(n)],
        "l1_results": _l1(esc_passed), "tier2_result_majority": {"per_step": []},
    }


def _cand(cid, *, esc_violation=True, pr=1.0, n=5):
    k = round(pr * n)
    tm = {c: True for c in _PY}
    if esc_violation:
        tm["escalation_compliance"] = False  # triggers the tier0 violation path
    return {
        "case_id": cid, "primary_uc": "UC-J", "case_passed": pr > 0.5,
        "majority_passed": pr > 0.5, "pass_rate": pr, "valid_attempts": n,
        "total_attempts": n, "comparable": True,
        "attempts": [{"attempt_index": i, "valid": True, "case_passed": i < k,
                      "failure_tags": []} for i in range(n)],
        "tier0_majority": tm, "tier2_result_majority": {"per_step": []},
        "l1_results": _l1(not esc_violation), "failure_tags": [],
    }


def _write(d, suites, *, aggregated):
    d.mkdir(parents=True, exist_ok=True)
    for s, cases in suites.items():
        sd = d / s
        sd.mkdir(parents=True, exist_ok=True)
        payload = {"run_id": s, "case_results": cases, "summary": {"total_cases": len(cases)}}
        if aggregated:
            payload["schema"] = "autoloop.baseline.aggregated.v1"
        (sd / ("aggregated.json" if aggregated else "results.json")).write_text(
            json.dumps(payload), encoding="utf-8")
    return d


def _run(tmp, *, base_shadow, cand_shadow, keep_eligible=False):
    base_bc, cand_bc = [_base("bc", flaky=False)], [_cand("bc", esc_violation=False)]
    if keep_eligible:
        # an improving bad_case so the candidate clears layer-3.
        base_bc.append(_base("imp", flaky=False, pr=2 / 11, esc_passed=False))
        cand_bc.append(_cand("imp", esc_violation=False, pr=1.0))
    base = load(_write(tmp / "b", {
        "bad_cases": base_bc, "anchor_outcome": [_base("ao", flaky=False)],
        "shadow": base_shadow}, aggregated=True), config=_CFG)
    cur = _write(tmp / "c", {
        "bad_cases": cand_bc, "anchor_outcome": [_cand("ao", esc_violation=False)],
        "shadow": cand_shadow}, aggregated=False)
    return evaluate(cur, base, config=_CFG)


def _t0(v):
    return v.layer_results[0].metrics_observed["python_tier0_family"]


def test_flaky_tier0_violation_is_deferred_not_failed(tmp_path):
    v = _run(tmp_path, base_shadow=[_base("ff", flaky=True)],
             cand_shadow=[_cand("ff", pr=1.0)])  # case passes overall, esc violation
    df = _t0(v)["deferred_flaky"]
    assert df and df[0]["check"] == "escalation_compliance" and df[0]["case_id"] == "ff"
    assert df[0]["baseline_flaky"] is True
    assert "escalation_compliance@ff" not in _t0(v)["failing_cases"]
    assert v.layer_results[0].passed is True  # tier0 did NOT fail on the flaky check


def test_deferred_flaky_holds_never_keeps(tmp_path):
    # otherwise keep-eligible + deferred flaky tier0 -> HOLD (never KEEP).
    v = _run(tmp_path, base_shadow=[_base("ff", flaky=True)],
             cand_shadow=[_cand("ff", pr=1.0)], keep_eligible=True)
    assert v.decision == "discard"
    assert v.classification == "hold_inconclusive_flaky"
    assert "hold_inconclusive_flaky" in (v.discard_reason or "")
    assert _t0(v)["deferred_flaky"]


def test_non_flaky_violation_immediate_discard_unchanged(tmp_path):
    v = _run(tmp_path, base_shadow=[_base("nf", flaky=False)],
             cand_shadow=[_cand("nf", pr=0.0)])
    assert v.decision == "discard"
    assert v.layer_results[0].passed is False
    assert "escalation_compliance@nf" in _t0(v)["failing_cases"]
    assert _t0(v)["deferred_flaky"] == []
    assert v.discard_reason == "tier0_escalation_compliance_failed_on_nf"


def test_flaky_pre_existing_baseline_failure_still_ignored(tmp_path):
    # baseline already fails the check -> pre-existing, ignored, never deferred.
    v = _run(tmp_path,
             base_shadow=[_base("ff", flaky=True, esc_passed=False, pr=0.2)],
             cand_shadow=[_cand("ff", pr=0.0)])
    assert "escalation_compliance@ff" in _t0(v)["pre_existing_baseline_failures_ignored"]
    assert _t0(v)["deferred_flaky"] == []
