"""Tests for the S-Auto-38 Option-R zero-LLM full baseline re-score harness
(``analysis/rescore_s_auto_38_full_baseline.py``).

Two tiers:

* PURE-LOGIC (always run) — the draw-level demotion gating predicate
  (``_other_gating_failure``) and the per-draw verdict re-score branching
  (``_new_case_passed`` with ``_draw_split`` monkeypatched), so the gate
  semantics are pinned independent of any recorded baseline.
* INTEGRATION (skipif the June-8 prepilot baseline scratch is present) —
  materializes the baseline into a tmp dir via ``run_rescore`` and asserts:
  the OLD reconstruction (43 unaffected cases reproduce June-8 majority_passed),
  the composite delta (exactly the 5 gate-consumed cases, all False->True), and
  a clean native load (``baseline_loader.load`` 0 warnings + drift cleared).
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path

import pytest

_TESTS = Path(__file__).resolve().parent
_EVAL = _TESTS.parent
_REPO = _EVAL.parent
_ANALYSIS = _EVAL / "analysis"
_SOURCE_BASELINE = _EVAL / "results" / "m-auto-7-prepilot-baseline-20260608"
_NEW_SHA = "7df8173cd4ef35741ec613038dc8cf29151d5b9880b585fac53442371fafebc0"

_COMPOSITE_CHANGED = {
    "cs001_uc_c_mechanical_template_escalate",
    "cs011_uc_c_faq_miss_not_distress",
    "cs01s02_uc_c_inbox_empty_after_form",
    "cs11s01_uc_d_two_emails_one_account",
    "cs11s02_uc_d_password_change_loop_high_distress",
}


def _load_harness():
    if str(_ANALYSIS) not in sys.path:
        sys.path.insert(0, str(_ANALYSIS))
    spec = importlib.util.spec_from_file_location(
        "rescore_s_auto_38_full_baseline",
        _ANALYSIS / "rescore_s_auto_38_full_baseline.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


try:
    _H = _load_harness()
    _IMPORT_ERR = None
except Exception as exc:  # autoloop/eval_interactive cross-import unavailable
    _H = None
    _IMPORT_ERR = exc

needs_harness = pytest.mark.skipif(_H is None, reason=f"harness import failed: {_IMPORT_ERR}")
needs_baseline = pytest.mark.skipif(
    not (_SOURCE_BASELINE / "_rebless_scratch" / "_attempts").is_dir(),
    reason="June-8 prepilot baseline scratch not present locally")


# --------------------------------------------------------------------------
# PURE-LOGIC — draw-level demotion gating predicate
# --------------------------------------------------------------------------

@needs_harness
@pytest.mark.parametrize("tags,expected", [
    ([], False),
    (["L1:escalation_compliance"], False),
    (["L1:escalation_compliance", "TIER2_ADVISORY:record-outcome"], False),
    (["L1:escalation_compliance", "L3_ADVISORY:groundedness",
      "TIER2_ADVISORY:x"], False),
    (["L1:escalation_compliance", "L2:correct_uc"], False),          # low-L2 noise, non-gating
    (["L1:escalation_compliance", "L3:premature_finish"], False),    # judge never gates case_passed
    (["L1:escalation_compliance", "STALL:loop"], False),             # bare stall tag non-gating
    (["L1:escalation_compliance", "L2_GATE:correct_uc"], True),
    (["L1:escalation_compliance", "L2_GATE_MISSING:correct_outcome"], True),
    (["L1:escalation_compliance", "TIER2:uc-i-intake-complete-before-handover"], True),
    (["L1:escalation_compliance", "L1:no_forbidden_tools"], True),
    (["L1:escalation_compliance", "VERDICT_OVERRIDE:stall_promoted"], True),
])
def test_other_gating_failure(tags, expected):
    assert _H._other_gating_failure(tags) is expected


# --------------------------------------------------------------------------
# PURE-LOGIC — per-draw verdict re-score branching (_draw_split patched)
# --------------------------------------------------------------------------

def _case(*, case_passed, tags, reason="user_requested",
          outcome=1.0, judge=0.0, stall=False, l2n=1):
    return {
        "case_passed": case_passed,
        "failure_tags": list(tags),
        "escalation_reason": reason,
        "outcome_score": outcome,
        "judge_score": judge,
        "stall_detected": stall,
        "l2_results": [{"check": "handover_completeness"}] * l2n,
    }


@needs_harness
def test_new_case_passed_non_escalation_unchanged(monkeypatch):
    monkeypatch.setattr(_H, "_draw_split", lambda *a, **k: (_ for _ in ()).throw(
        AssertionError("must not be called for a non-escalation draw")))
    cp, kind, p1 = _H._new_case_passed(object(), _case(case_passed=False, tags=["L2_GATE:correct_uc"]))
    assert (cp, kind, p1) == (False, "unchanged", None)


@needs_harness
def test_new_case_passed_part1_fail_unchanged(monkeypatch):
    monkeypatch.setattr(_H, "_draw_split", lambda *a, **k: (False, False))  # Part-1 genuinely fails
    cp, kind, p1 = _H._new_case_passed(object(), _case(case_passed=False, tags=["L1:escalation_compliance"]))
    assert cp is False and kind == "part1_fail_unchanged" and p1 is False


@needs_harness
def test_new_case_passed_part2_demotion_flip(monkeypatch):
    monkeypatch.setattr(_H, "_draw_split", lambda *a, **k: (True, False))   # Part-1 ok, Part-2 fail
    cp, kind, p1 = _H._new_case_passed(
        object(), _case(case_passed=False, tags=["L1:escalation_compliance", "TIER2_ADVISORY:x"]))
    assert cp is True and kind == "part2_demotion_flip" and p1 is True


@needs_harness
def test_new_case_passed_other_gating_blocks_flip(monkeypatch):
    monkeypatch.setattr(_H, "_draw_split", lambda *a, **k: (True, False))
    cp, kind, p1 = _H._new_case_passed(
        object(), _case(case_passed=False, tags=["L1:escalation_compliance", "L2_GATE:correct_uc"]))
    assert cp is False and kind == "other_gating_unchanged"


@needs_harness
def test_new_case_passed_oqs77_stall_blocks_flip(monkeypatch):
    monkeypatch.setattr(_H, "_draw_split", lambda *a, **k: (True, False))
    cp, kind, p1 = _H._new_case_passed(
        object(), _case(case_passed=False, tags=["L1:escalation_compliance"],
                        outcome=0.0, judge=0.0, stall=True))
    assert cp is False and kind == "oqs77_stall_unchanged"


@needs_harness
def test_new_case_passed_oqs77_no_l2_blocks_flip(monkeypatch):
    monkeypatch.setattr(_H, "_draw_split", lambda *a, **k: (True, False))
    cp, kind, p1 = _H._new_case_passed(
        object(), _case(case_passed=False, tags=["L1:escalation_compliance"],
                        outcome=0.0, judge=0.0, stall=False, l2n=0))
    assert cp is False and kind == "oqs77_no_l2_unchanged"


# --------------------------------------------------------------------------
# INTEGRATION — materialize + native load (needs the June-8 baseline locally)
# --------------------------------------------------------------------------

@pytest.fixture(scope="module")
def materialized(tmp_path_factory):
    if _H is None or not (_SOURCE_BASELINE / "_rebless_scratch" / "_attempts").is_dir():
        pytest.skip("harness or June-8 baseline unavailable")
    out = tmp_path_factory.mktemp("rescore_baseline")
    meta = _H.run_rescore(out, str(_REPO / "autoloop" / "config.yaml"))
    return out, meta


@needs_harness
@needs_baseline
def test_composite_delta_is_the_five_gate_cases(materialized):
    _out, meta = materialized
    assert meta["validation_ok"] is True
    assert set(meta["composite_changed_cases"]) == _COMPOSITE_CHANGED
    assert meta["composite_changed_count"] == 5
    for cid, c in meta["composite_changed_cases"].items():
        assert c["old_majority_passed"] is False and c["new_majority_passed"] is True


@needs_harness
@needs_baseline
def test_soundness_no_regressions(materialized):
    _out, meta = materialized
    s = meta["soundness"]
    assert s["draw_regressions"] == []
    assert s["nondemotion_draw_changes"] == []
    assert s["case_regressions"] == []


@needs_harness
@needs_baseline
def test_reconciliation_layer0_vs_composite(materialized):
    _out, meta = materialized
    rec = meta["reconciliation"]
    assert rec["composite_only"] == ["cs11s01_uc_d_two_emails_one_account"]
    assert rec["tier0_only"] == ["cs76s01_uc_e_paid_top_ad_not_running",
                                 "cs76s02_uc_e_featured_listing_demoted"]


@needs_harness
@needs_baseline
def test_old_reconstruction_unchanged_cases_reproduce_june8(materialized):
    """Every case NOT in the 5 reproduces June-8's majority_passed exactly."""
    import json
    out, _meta = materialized
    for suite in ("bad_cases", "anchor_outcome", "shadow"):
        old = {c["case_id"]: c.get("majority_passed")
               for c in json.loads((_SOURCE_BASELINE / suite / "aggregated.json").read_text())["case_results"]}
        new = {c["case_id"]: c.get("majority_passed")
               for c in json.loads((out / suite / "aggregated.json").read_text())["case_results"]}
        for cid, mp in new.items():
            if cid not in _COMPOSITE_CHANGED:
                assert mp == old.get(cid), f"{suite}/{cid} drifted: {old.get(cid)} -> {mp}"


@needs_harness
@needs_baseline
def test_native_baseline_loader_clean(materialized):
    import copy
    import yaml
    from autoloop.scoring import baseline_loader, gaming
    out, _meta = materialized
    cfg = yaml.safe_load((_REPO / "autoloop" / "config.yaml").read_text())
    ov = copy.deepcopy(cfg)
    ov["fitness"]["scoring_code_baseline_sha"] = _NEW_SHA
    snap = baseline_loader.load(out, config=ov)
    assert snap.warnings == []
    assert set(snap.snapshots) == {"bad_cases", "anchor_outcome", "shadow"}
    assert {s: snap.snapshots[s].case_passed_count for s in snap.snapshots} == {
        "bad_cases": 10, "anchor_outcome": 8, "shadow": 8}
    # scoring_code_drift clears under the NEW six-file pin.
    assert gaming._check_scoring_code_drift(config=ov) == []
