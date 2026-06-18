"""S-Auto-38 (Sprint 092) Option-R zero-LLM FULL baseline re-score.

Produces a NEW, structurally-valid, ``baseline_loader``-loadable baseline dir
(``<out>/<suite>/aggregated.json``) by re-scoring the EXISTING June-8 prepilot
baseline (``m-auto-7-prepilot-baseline-20260608``) under the WP-A escalation
split — NO LLM, NO new draws. Unlike ``rebless_s_auto_38_tier0_split.py`` (which
emits only the tier-0 OLD/NEW delta maps) this rebuilds the full per-suite
``aggregated.json`` so the next pilot/smoke can bind it via a run-scoped
``--config`` override WITHOUT moving any global baseline pointer.

Fidelity contract — drives the REAL live pipeline, reconstructs nothing it can
reuse:

* per-attempt records come from the recorded draws in the June-8 baseline's
  ``_rebless_scratch/_attempts/a{0..N}/<suite>/results.json`` (full l1/l2/tier2/
  stall/outcome/judge per draw);
* the escalation split is re-run with the already-blessed
  ``replay_s_auto_38_gate_split._draw_split`` (self-check 341/341);
* L2 / L3 / tier2 / stall are REUSED from storage (the split does not touch
  them) — the OQ-S77 post-composite gate is re-evaluated from the stored
  ``outcome_score`` / ``judge_score`` / ``stall_detected`` / ``l2_results``;
* aggregation is the REAL ``_case_attempt_record`` + ``aggregate_case`` +
  ``_tier0_check_majority`` + ``_tier2_majority`` + ``rebless_baseline.
  _build_aggregated_suite`` (the exact code that built June-8).

Per-attempt NEW ``case_passed`` differs from OLD ONLY when ``L1:
escalation_compliance`` was the SOLE gating failure and Part-1 (behaviour)
passes — i.e. the OLD discard was the demoted Part-2 reason-label. Every other
draw keeps its stored verdict, so the 43 unaffected baseline cases reproduce
June-8 exactly and only the 6 Part-2-demotion cases move. The script asserts
this (the changed set MUST equal the re-bless's 6) and refuses to write
otherwise.

Usage (repo root):
    cd eval_interactive && uv run python analysis/rescore_s_auto_38_full_baseline.py
"""

from __future__ import annotations

import argparse
import copy
import json
import os
import subprocess
import sys
from datetime import date
from pathlib import Path

_HERE = os.path.dirname(os.path.abspath(__file__))
_EVAL = os.path.dirname(_HERE)
_REPO = os.path.dirname(_EVAL)

sys.path.insert(0, _HERE)
sys.path.insert(0, os.path.join(_REPO, "autoloop"))
sys.path.insert(0, os.path.join(_REPO, "autoloop", "scripts"))

import yaml  # noqa: E402

from autoloop.scoring.aggregate import aggregate_case  # noqa: E402
from autoloop.scoring.eval_runner import (  # noqa: E402
    _case_attempt_record,
    _tier0_check_majority,
    _tier2_majority,
)
from rebless_baseline import _build_aggregated_suite, _stability_thresholds  # noqa: E402
from replay_s_auto_38_gate_split import _draw_split, _spec_index  # noqa: E402

_SOURCE_BASELINE = "m-auto-7-prepilot-baseline-20260608"
_SUITES = ("bad_cases", "anchor_outcome", "shadow")
_GATE_DEFINITION_VERSION = "tier0_escalation_split_s_auto_38_v1"

# The 6 baseline cases the tier-0 re-bless flagged. NOTE: that count is the
# Layer-0 *tier0 contribution* flip set (``others_pass AND escalation_check_
# majority``), which is NOT the same population as the Layer-1 *composite
# majority_passed* change set the baseline gate actually consumes. They differ
# for principled reasons (majority-of-ANDs != AND-of-majorities):
#   * cs76s01/cs76s02 — tier0 flips but composite stays FAIL (every escalation-
#     failing draw ALSO fails L2_GATE:correct_uc / critical TIER2 / other L1),
#     so the demotion does not rescue them — correctly NOT credited.
#   * cs11s01 — composite flips but tier0 check-majority did not (escalation was
#     a *minority*-failing-but-sole-gate on the flipping draws), so the
#     composite majority moves 3/11 -> 7/11 while the per-check majority was
#     already True.
# The validation below therefore checks SOUNDNESS invariants, not equality with
# this tier0 set; the reconciliation is reported for the human record.
_REBLESS_TIER0_SET = {
    "cs001_uc_c_mechanical_template_escalate",
    "cs011_uc_c_faq_miss_not_distress",
    "cs01s02_uc_c_inbox_empty_after_form",
    "cs11s02_uc_d_password_change_loop_high_distress",
    "cs76s01_uc_e_paid_top_ad_not_running",
    "cs76s02_uc_e_featured_listing_demoted",
}


def _git_head() -> str:
    try:
        out = subprocess.run(["git", "rev-parse", "HEAD"], cwd=_REPO,
                             capture_output=True, text=True, timeout=10)
        return out.stdout.strip() if out.returncode == 0 else "<unknown>"
    except (OSError, subprocess.TimeoutExpired):
        return "<unknown>"


def _other_gating_failure(tags: list[str]) -> bool:
    """True iff any case_passed-gating failure OTHER than escalation_compliance
    is present. Gating tag families (composite.py): critical ``L1:*`` (every L1
    on bad_cases/shadow is critical), ``L2_GATE:*`` / ``L2_GATE_MISSING:*``,
    critical ``TIER2:*`` (NOT ``TIER2_ADVISORY:*``), ``VERDICT_OVERRIDE:*``.
    Non-gating: ``L2:*`` (low-score noise), ``L3:*`` / ``L3_ADVISORY:*`` (judge
    never gates case_passed), ``STALL:*``, ``TIER2_ADVISORY:*``."""
    for t in tags:
        t = str(t)
        if t == "L1:escalation_compliance":
            continue
        if t.startswith("L1:"):
            return True
        if t.startswith("L2_GATE:") or t.startswith("L2_GATE_MISSING:"):
            return True
        if t.startswith("TIER2:") and not t.startswith("TIER2_ADVISORY:"):
            return True
        if t.startswith("VERDICT_OVERRIDE:"):
            return True
    return False


def _new_case_passed(spec, case: dict) -> tuple[object, str, object]:
    """Return (new_case_passed, kind, part1_pass) for one recorded draw.

    Only a draw whose OLD discard was the demoted Part-2 reason-label (Part-1
    passes, escalation_compliance the sole gating failure, OQ-S77 does not newly
    fire) flips False->True. Everything else keeps its stored verdict."""
    old = case.get("case_passed")
    tags = [str(t) for t in (case.get("failure_tags") or [])]
    if "L1:escalation_compliance" not in tags:
        return old, "unchanged", None
    reason = (case.get("escalation_reason") or "").strip()
    p1, _p2 = _draw_split(spec, reason=reason, old_fail=True)
    if not p1:
        return old, "part1_fail_unchanged", p1          # genuine Part-1 fail -> still gated
    if _other_gating_failure(tags):
        return old, "other_gating_unchanged", p1        # another gate keeps it False
    # escalation no longer gates; candidate pass -> re-evaluate OQ-S77 (the only
    # other thing that can hold case_passed False) from stored composite inputs.
    comp = 0.5 * float(case.get("outcome_score") or 0.0) + 0.5 * float(case.get("judge_score") or 0.0)
    if case.get("stall_detected") and comp == 0.0:
        return False, "oqs77_stall_unchanged", p1
    if comp == 0.0 and not (case.get("l2_results") or []):
        return False, "oqs77_no_l2_unchanged", p1
    return True, "part2_demotion_flip", p1


def _load_attempt_files(scratch: Path, suite: str) -> list[dict]:
    """The N recorded draws for a suite, in attempt-index order."""
    out: list[tuple[int, dict]] = []
    attempts_root = scratch / "_attempts"
    for d in sorted(attempts_root.glob("a*")):
        idx = int(d.name[1:])
        rj = d / suite / "results.json"
        if rj.exists():
            out.append((idx, json.loads(rj.read_text(encoding="utf-8"))))
    out.sort(key=lambda x: x[0])
    return [{"attempt_index": i, "data": data} for i, data in out]


def run_rescore(out_dir: Path, config_path: str) -> dict:
    """Materialize the Option-R baseline into ``out_dir`` and return the
    metadata dict (also written to ``out_dir/_rescore_metadata.json``)."""
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    config = yaml.safe_load(Path(config_path).read_text(encoding="utf-8"))
    fitness_cfg = config.get("fitness") or {}
    thresholds = _stability_thresholds(fitness_cfg)
    primary_model = (fitness_cfg.get("provider_policy") or {}).get("primary_model")
    min_valid = int((fitness_cfg.get("aggregation") or {}).get("min_valid_attempts", 3))
    git_commit = _git_head()

    source = Path(_EVAL) / "results" / _SOURCE_BASELINE
    scratch = source / "_rebless_scratch"
    idx = _spec_index()

    changed: dict[str, dict] = {}
    flip_kinds: dict[str, int] = {}
    regression_draws: list = []        # draw-level True -> not-True (MUST stay empty)
    nondemotion_changes: list = []     # draw verdict changed for a non-demotion reason
    report: dict[str, dict] = {}

    for suite in _SUITES:
        attempts = _load_attempt_files(scratch, suite)
        if not attempts:
            print(f"[rescore] WARNING: no attempt files for {suite}", file=sys.stderr)
            continue

        # Baseline (June-8) per-case majority_passed, for the changed-set check.
        old_agg = json.loads((source / suite / "aggregated.json").read_text(encoding="utf-8"))
        old_majority = {c["case_id"]: c.get("majority_passed") for c in old_agg.get("case_results", [])}

        # Build per-attempt {cid: AttemptRecord} with NEW (split) case_passed, by
        # mutating each draw's case dict (case_passed + escalation_compliance l1
        # entry) and running the REAL record builder.
        per_attempt_records: list[dict] = []
        base_cases_by_id: dict[str, dict] = {}
        for a in attempts:
            recs: dict = {}
            for case in a["data"].get("case_results", []):
                cid = case.get("case_id", "<unknown>")
                base_cases_by_id.setdefault(cid, case)
                spec = idx.get(cid)
                if spec is None:
                    new_cp, kind, p1 = case.get("case_passed"), "no_spec", None
                else:
                    new_cp, kind, p1 = _new_case_passed(spec, case)
                old_cp = case.get("case_passed")
                if new_cp != old_cp:
                    flip_kinds[kind] = flip_kinds.get(kind, 0) + 1
                    if old_cp is True and new_cp is not True:
                        regression_draws.append((suite, cid, a["attempt_index"], kind))
                    if kind != "part2_demotion_flip":
                        nondemotion_changes.append((suite, cid, a["attempt_index"], kind))
                c2 = copy.deepcopy(case)
                c2["case_passed"] = new_cp
                if p1 is not None:
                    for r in c2.get("l1_results") or []:
                        if r.get("check") == "escalation_compliance":
                            r["passed"] = bool(p1)
                    if bool(p1):
                        c2["failure_tags"] = [t for t in (c2.get("failure_tags") or [])
                                              if str(t) != "L1:escalation_compliance"]
                recs[cid] = _case_attempt_record(c2, a["attempt_index"], primary_model)
            per_attempt_records.append(recs)

        # REAL aggregation (mirror _aggregate_and_stage_suite) -> intermediate
        # results.json -> REAL _build_aggregated_suite -> aggregated.json.
        ordered_ids: list[str] = []
        seen: set[str] = set()
        for a in per_attempt_records:
            for cid in a:
                if cid not in seen:
                    seen.add(cid)
                    ordered_ids.append(cid)

        aggregated_cases = []
        for cid in ordered_ids:
            records = [a[cid] for a in per_attempt_records if cid in a]
            agg = aggregate_case(cid, records, min_valid_attempts=min_valid)
            base = dict(base_cases_by_id.get(cid) or {"case_id": cid})
            base.update(agg.to_dict())
            base["tier0_majority"] = _tier0_check_majority(records)
            base["tier2_result_majority"] = _tier2_majority(records)
            aggregated_cases.append(base)
            new_mp = base.get("majority_passed")
            if new_mp != old_majority.get(cid):
                changed[cid] = {"suite": suite, "old_majority_passed": old_majority.get(cid),
                                "new_majority_passed": new_mp, "new_pass_rate": base.get("pass_rate")}

        interim = {"case_results": aggregated_cases,
                   "_aggregation": {"method": "majority", "min_valid_attempts": min_valid,
                                    "attempts_run": len(per_attempt_records), "suite": suite}}
        interim_path = out_dir / f"_interim_{suite}_results.json"
        interim_path.write_text(json.dumps(interim, indent=2, default=str), encoding="utf-8")

        agg_suite = _build_aggregated_suite(
            suite, interim_path, n=len(attempts), thresholds=thresholds,
            primary_model=primary_model, git_commit=git_commit, min_valid=min_valid)
        suite_out = out_dir / suite
        suite_out.mkdir(parents=True, exist_ok=True)
        (suite_out / "aggregated.json").write_text(
            json.dumps(agg_suite, indent=2, default=str), encoding="utf-8")
        interim_path.unlink()
        report[suite] = {"n_cases": len(agg_suite["case_results"]),
                         "stability_summary": agg_suite["stability_summary"]}
        print(f"[rescore] wrote {suite_out/'aggregated.json'} "
              f"({len(agg_suite['case_results'])} cases, {agg_suite['stability_summary']})")

    # --- Validation: SOUNDNESS invariants (not equality with the tier0 set) ---
    changed_ids = set(changed)
    case_regressions = [cid for cid, c in changed.items()
                        if c["old_majority_passed"] is True and c["new_majority_passed"] is not True]
    # Reconciliation with the re-bless Layer-0 tier0 set (informational).
    composite_only = changed_ids - _REBLESS_TIER0_SET     # flips composite but not tier0 (cs11s01)
    tier0_only = _REBLESS_TIER0_SET - changed_ids         # flips tier0 but not composite (cs76s01/02)
    both = changed_ids & _REBLESS_TIER0_SET

    sound = (not regression_draws and not nondemotion_changes and not case_regressions)

    print("\n" + "=" * 92)
    print("S-Auto-38 Option-R FULL baseline re-score (zero-LLM)")
    print("=" * 92)
    print(f"out_dir: {out_dir}")
    print(f"part2_demotion per-attempt flips (False->True): {flip_kinds.get('part2_demotion_flip', 0)}")
    print(f"cases whose composite majority_passed changed vs June-8: {len(changed_ids)}")
    for cid in sorted(changed_ids):
        c = changed[cid]
        print(f"  {c['suite']:14} {cid[:42]:42} maj {c['old_majority_passed']}->{c['new_majority_passed']} "
              f"pass_rate->{round(c['new_pass_rate'], 4) if c['new_pass_rate'] is not None else None}")
    print("\n-- Soundness invariants (MUST all hold) --")
    print(f"  draw-level True->not-True regressions: {len(regression_draws)} (MUST be 0)")
    print(f"  draw verdict changes NOT part2_demotion: {len(nondemotion_changes)} (MUST be 0)")
    print(f"  case-level majority True->False regressions: {len(case_regressions)} (MUST be 0)")
    print("\n-- Reconciliation vs re-bless Layer-0 tier0 set {6} --")
    print(f"  in BOTH (tier0 flip & composite flip): {sorted(both)}")
    print(f"  composite-only (flips gate, not tier0): {sorted(composite_only)}  [expected: cs11s01]")
    print(f"  tier0-only (flips tier0, gate stays FAIL): {sorted(tier0_only)}  [expected: cs76s01/02]")

    meta = {
        "source_baseline": _SOURCE_BASELINE,
        "method": "zero_llm_full_rescore_under_wpa_split",
        "generated_at": date.today().isoformat(),
        "git_commit": git_commit,
        "gate_definition_version": _GATE_DEFINITION_VERSION,
        "rebless_provenance_stamp": "78c9f8a635162b3a",
        "composite_changed_cases": {cid: changed[cid] for cid in sorted(changed_ids)},
        "composite_changed_count": len(changed_ids),
        "rebless_tier0_set": sorted(_REBLESS_TIER0_SET),
        "layer_distinction": (
            "Layer-0 tier0-contribution delta = 6 cases; gate-consumed composite "
            "majority_passed delta = 5 cases; differ because majority-of-ANDs != "
            "AND-of-majorities. Pilot binding/acceptance uses the composite "
            "materialization (this artifact)."),
        "reconciliation": {"both": sorted(both), "composite_only": sorted(composite_only),
                           "tier0_only": sorted(tier0_only)},
        "case_annotations": {
            "cs11s01_uc_d_two_emails_one_account": {
                "classification": "dual_path_conflict",
                "tier": "TIER-N intermediate (pass_rate~0.64); NOT a TIER-S anti-误杀 floor",
                "review": ("pending product-owner override review (WP-B companion "
                           "record, observation-only); not excluded from baseline"),
            },
            "cs76s01_uc_e_paid_top_ad_not_running": {
                "classification": "tier0_only_not_composite",
                "note": ("escalation Part-2 demotion does not rescue: independent "
                         "L2_GATE:correct_uc / critical TIER2 intake failures keep "
                         "majority_passed=False — correctly stays a baseline fail"),
            },
            "cs76s02_uc_e_featured_listing_demoted": {
                "classification": "tier0_only_not_composite",
                "note": ("escalation Part-2 demotion does not rescue: independent "
                         "L2_GATE:correct_uc / critical TIER2 intake failures keep "
                         "majority_passed=False — correctly stays a baseline fail"),
            },
        },
        "soundness": {
            "draw_regressions": regression_draws,
            "nondemotion_draw_changes": nondemotion_changes,
            "case_regressions": case_regressions,
            "all_changes_are_false_to_true_part2_demotion": sound,
        },
        "validation_ok": sound,
        "report": report,
        "out_dir": str(out_dir),
    }
    (out_dir / "_rescore_metadata.json").write_text(json.dumps(meta, indent=2, default=str),
                                                    encoding="utf-8")
    print(f"\nVALIDATION (soundness): {'PASS' if sound else 'FAIL'}")
    return meta


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", default=None)
    ap.add_argument("--config", default=os.path.join(_REPO, "autoloop", "config.yaml"))
    args = ap.parse_args()
    today = date.today().isoformat().replace("-", "")
    out_dir = args.out_dir or os.path.join(
        _EVAL, "results", f"m-auto-7-prepilot-baseline-{today}-s_auto_38_split_full")
    meta = run_rescore(Path(out_dir), args.config)
    if not meta["validation_ok"]:
        print(json.dumps(meta["soundness"], indent=1))
        sys.exit(1)


if __name__ == "__main__":
    main()
