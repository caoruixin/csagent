"""S-Auto-50 zero-LLM label-replay oracle (M-Auto-12 WP1).

PRIMARY validation evidence for the sub-sprint (contract §Scope #4): recompute
the observation-only objective-alignment label for each archived M-Auto-7
pilot-smoke candidate exp-81…86 from its recorded
`autoloop/results/runs/exp-{N}/eval/` dir + the run's PRIMARY targets, against
the run-scoped Option-R baseline the smoke actually bound
(`config.pilot-s-auto-38.yaml`). No LLM, no new eval run — the candidate replay
is FIXED recorded data (§5.7 does not apply; nothing about the measured
variable is mocked, the label logic is the unit under test).

Two binding pins:
  1. exp-86 → OFF_TARGET (the proposal §2 motivating conclusion + this
     sub-sprint's hard assert), despite its gate KEEP on a single peripheral
     improvement (`anchor_outcome_uc_g_gdpr`).
  2. The label is OBSERVATION-ONLY: it is computed by reading the verdict, never
     by mutating it. This file pins the exp-81…86 keep/discard decisions
     alongside the labels to demonstrate the label sits on top without moving
     the gate; the separate `test_fitness_gate_oracle.py` keep/discard matrix
     (exp-66…79) is left byte-unchanged and asserted intact below.

Skips cleanly when the archived runs / baseline / Option-R config are absent.
"""

from __future__ import annotations

from pathlib import Path

import pytest
import yaml

from autoloop.scoring import evaluate, evaluate_alignment, load

_REPO_ROOT = Path(__file__).resolve().parents[2]
# The exp-81…86 pilot smoke bound the run-scoped Option-R split-full baseline
# via this override config (proposal §1 evidence base), NOT the global
# config.yaml pointer. Replaying against it reproduces the proposal §2 numbers.
_CONFIG = _REPO_ROOT / "autoloop" / "config.pilot-s-auto-38.yaml"
_RUNS = _REPO_ROOT / "autoloop" / "results" / "runs"

# Pinned exp-81…86 matrix: (gate_decision, objective_alignment_label).
# All six replay to OFF_TARGET — exp-81…85 short-circuit at the Tier-0 floor so
# their PRIMARY posteriors are absent (treated as not-improved); exp-86 keeps on
# a peripheral improvement while a PRIMARY (cs_uc_a_no_ad_id_ad_specific) is
# stuck at TIER-F 0 with P_improve 0.059 < 0.8.
_PINNED = {
    81: ("discard", "OFF_TARGET"),
    82: ("discard", "OFF_TARGET"),
    83: ("discard", "OFF_TARGET"),
    84: ("discard", "OFF_TARGET"),
    85: ("discard", "OFF_TARGET"),
    86: ("keep", "OFF_TARGET"),
}
_ALL = sorted(_PINNED)


def _load_config() -> dict:
    if not _CONFIG.exists():
        pytest.skip(f"Option-R config not found at {_CONFIG}")
    return yaml.safe_load(_CONFIG.read_text(encoding="utf-8"))


def _load_baseline(cfg: dict):
    baseline_dir = _REPO_ROOT / cfg["fitness"]["baseline_dir"]
    if not baseline_dir.exists():
        pytest.skip(f"Option-R baseline not present at {baseline_dir}")
    return load(baseline_dir, config=cfg)


def _exp_dir(n: int) -> Path:
    return _RUNS / f"exp-{n}" / "eval"


def _evaluate_with_label(n: int):
    cfg = _load_config()
    d = _exp_dir(n)
    if not d.exists():
        pytest.skip(f"archived candidate run exp-{n} not present at {d}")
    base = _load_baseline(cfg)
    verdict = evaluate(d, base, config=cfg)
    primaries = list((cfg.get("pilot") or {}).get("primary_targets") or [])
    alignment = evaluate_alignment(verdict, primaries, cfg)
    return verdict, alignment


@pytest.mark.parametrize("n", _ALL)
def test_label_replay_matches_pinned_matrix(n: int):
    """Every archived exp-81…86 reproduces its pinned (gate, label) pair."""
    verdict, alignment = _evaluate_with_label(n)
    exp_gate, exp_label = _PINNED[n]
    assert verdict.decision == exp_gate, (
        f"exp-{n}: gate expected {exp_gate}, got {verdict.decision}"
    )
    assert alignment.label == exp_label, (
        f"exp-{n}: label expected {exp_label}, got {alignment.label} "
        f"(per_primary={alignment.per_primary})"
    )


def test_exp86_is_off_target_despite_gate_keep():
    """The headline pin: exp-86 gate-KEEPs but is OFF_TARGET at the objective
    level (neither PRIMARY moved; the keep was a peripheral improvement)."""
    verdict, alignment = _evaluate_with_label(86)
    assert verdict.decision == "keep"
    assert alignment.label == "OFF_TARGET"
    assert alignment.merge_eligible is False
    # The keep was carried by a non-PRIMARY improvement; neither PRIMARY did.
    assert alignment.nonprimary_improved  # peripheral improvement exists
    assert all(not p["credible_improve"] for p in alignment.per_primary)
    # cs_uc_a_no_ad_id_ad_specific is the stuck TIER-F-at-0 primary.
    stuck = [p["case_id"] for p in alignment.per_primary if p["stuck_hard"]]
    assert "cs_uc_a_no_ad_id_ad_specific" in stuck


def test_label_is_observation_only_gate_decision_unchanged():
    """Computing the label is a pure read: re-running evaluate() and labelling
    yields the same gate decision as evaluate() alone (label never moves it)."""
    cfg = _load_config()
    base = _load_baseline(cfg)
    primaries = list((cfg.get("pilot") or {}).get("primary_targets") or [])
    for n in _ALL:
        d = _exp_dir(n)
        if not d.exists():
            pytest.skip(f"archived candidate run exp-{n} not present at {d}")
        gate_only = evaluate(d, base, config=cfg).decision
        v2 = evaluate(d, base, config=cfg)
        before = v2.decision
        evaluate_alignment(v2, primaries, cfg)
        assert v2.decision == before == gate_only, (
            f"exp-{n}: labelling changed the gate decision"
        )


def test_replay_report_no_true_objective_keep_among_pilot_smoke():
    """Validation-report invariant (handoff §5): among exp-81…86, no candidate
    is a true OBJECTIVE_KEEP/FULL_SUCCESS and none is merge-eligible; the sole
    gate-KEEP (exp-86) is OFF_TARGET, not PERIPHERAL_ONLY."""
    labels = {}
    for n in _ALL:
        _verdict, alignment = _evaluate_with_label(n)
        labels[n] = alignment.label
        assert alignment.merge_eligible is False
    assert all(lbl == "OFF_TARGET" for lbl in labels.values()), labels
    assert "OBJECTIVE_KEEP" not in labels.values()
    assert "FULL_SUCCESS" not in labels.values()


def test_fitness_gate_oracle_keep_discard_matrix_byte_unchanged():
    """Observation-only proof: the pre-existing keep/discard oracle matrix
    (exp-66…79, `test_fitness_gate_oracle.py`) is untouched by this sub-sprint.
    Importing its module-level pins guards against an accidental edit."""
    from tests import test_fitness_gate_oracle as oracle

    assert oracle._DISCARDS == {
        67: ("FS_tier_s", "tier1_anti_kill_tier_s_flip", "cs_uc_a_generic_policy_question"),
        68: ("hold_flaky", "hold_inconclusive_flaky", "cs11s01"),
        73: ("cross_case", "tier1_outcome_regressed", None),
        74: ("hold_flaky", "hold_inconclusive_flaky", "cs11s01"),
        78: ("FS_tier_s", "tier1_anti_kill_tier_s_flip", "cs_uc_a_generic_policy_question"),
        69: ("cross_case", "tier1_outcome_regressed", None),
    }
    assert oracle._KEEPS == [66, 71, 72, 75, 77, 79]
