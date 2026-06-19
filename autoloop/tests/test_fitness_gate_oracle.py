"""S-Y1.7 acceptance oracle — zero-LLM replay of the noise-aware V3 fitness
gate over the 12 archived M-Auto-7 pre-pilot candidate experiments.

This is the PRIMARY acceptance evidence for the sub-sprint (contract §Scope #9).
It runs the production `tier_evaluator.evaluate(...)` on the recorded
`autoloop/results/runs/exp-{N}/eval/` candidate dirs against the blessed
pre-pilot baseline and pins the re-pinned V3 verdict matrix. No LLM, no new eval
run — the candidate replay is FIXED recorded data, so this offline replay is
PRIMARY evidence (§5.7 does not apply: nothing about the measured variable is
mocked; the scoring logic is the unit under test).

The matrix agrees with the validated reference
`docs/solutions/p07-calibration/calibrate.py` (extended to the C1/C2 Layer-2/4
wiring) on every experiment. F5 power ceiling: this certifies the floors +
direction + C1/C2 wiring; the precise knobs (delta=0.10, 0.80, count=2) are
confirmed on the first S-Y2 pilot run, NOT by this zero-cost pass.

Skips cleanly when the archived runs / baseline are not present (fresh checkout
or CI without the recorded artefacts).
"""

from __future__ import annotations

from pathlib import Path

import pytest
import yaml

from autoloop.scoring import evaluate, load

# Repo root = .../csagent-latest ; this file is autoloop/tests/<here>.
_REPO_ROOT = Path(__file__).resolve().parents[2]
_CONFIG = _REPO_ROOT / "autoloop" / "config.yaml"
_RUNS = _REPO_ROOT / "autoloop" / "results" / "runs"


# Re-pinned V3 matrix (contract §Scope #9). `decision` is the binding pin;
# `mechanism` is a substring the discard_reason must contain.
#
# S-Auto-42 reroute (2026-06-19): exp-67/68/73/74 were historically pinned to the
# tier0 floor on `cs11s01_uc_d_two_emails_one_account` — a BASELINE-FLAGGED FLAKY
# shadow case. The bool-only INCONCLUSIVE_FLAKY defer rule DEFERS a flaky tier0
# violation instead of auto-attributing it as a candidate regression, so the
# discard DECISION is preserved (no KEEP-widening) but the MECHANISM reroutes:
#   67/73 -> the real later-layer regression the tier0 short-circuit had masked
#            (FS anti-误杀 flip / cross-case count);
#   68/74 -> HOLD_INCONCLUSIVE_FLAKY (otherwise keep-eligible; the deferred flaky
#            tier0 check holds it; never KEEP).
# All four remain `discard` (test_oracle_decision_matches_pinned_matrix unchanged).
_DISCARDS = {
    67: ("FS_tier_s", "tier1_anti_kill_tier_s_flip", "cs_uc_a_generic_policy_question"),
    68: ("hold_flaky", "hold_inconclusive_flaky", "cs11s01"),
    73: ("cross_case", "tier1_outcome_regressed", None),
    74: ("hold_flaky", "hold_inconclusive_flaky", "cs11s01"),
    78: ("FS_tier_s", "tier1_anti_kill_tier_s_flip", "cs_uc_a_generic_policy_question"),
    69: ("cross_case", "tier1_outcome_regressed", None),
}
_KEEPS = [66, 71, 72, 75, 77, 79]
_ALL = sorted(list(_DISCARDS) + _KEEPS)


def _load_config() -> dict:
    if not _CONFIG.exists():
        pytest.skip("autoloop/config.yaml not found")
    return yaml.safe_load(_CONFIG.read_text(encoding="utf-8"))


def _load_baseline(cfg: dict):
    baseline_dir = _REPO_ROOT / cfg["fitness"]["baseline_dir"]
    if not baseline_dir.exists():
        pytest.skip(f"prepilot baseline not present at {baseline_dir}")
    return load(baseline_dir, config=cfg)


def _exp_dir(n: int) -> Path:
    return _RUNS / f"exp-{n}" / "eval"


def _evaluate(n: int):
    cfg = _load_config()
    d = _exp_dir(n)
    if not d.exists():
        pytest.skip(f"archived candidate run exp-{n} not present at {d}")
    base = _load_baseline(cfg)
    return evaluate(d, base, config=cfg)


@pytest.mark.parametrize("n", _ALL)
def test_oracle_decision_matches_pinned_matrix(n: int):
    """Every archived experiment reproduces its pinned keep/discard decision."""
    v = _evaluate(n)
    expected = "discard" if n in _DISCARDS else "keep"
    assert v.decision == expected, (
        f"exp-{n}: expected {expected}, got {v.decision} "
        f"(reason={v.discard_reason}, class={v.classification})"
    )


@pytest.mark.parametrize("n", sorted(_DISCARDS))
def test_oracle_discard_mechanism(n: int):
    """Each discard fires via the pinned mechanism (floor / FS / cross-case)."""
    v = _evaluate(n)
    _label, primary, secondary = _DISCARDS[n]
    assert v.decision == "discard"
    assert primary in (v.discard_reason or ""), (
        f"exp-{n}: reason {v.discard_reason!r} missing {primary!r}"
    )
    if secondary is not None:
        assert secondary in (v.discard_reason or ""), (
            f"exp-{n}: reason {v.discard_reason!r} missing {secondary!r}"
        )


def test_oracle_exp66_ambiguous_via_c1_tier2_knife_edge():
    """exp-66: off-discard but AMBIGUOUS — its tier2 3→4 is driven solely by
    wmkb_uc_a_trader_flag_secondary_uc_h at P_regress=0.797 < 0.80 (C1
    knife-edge). The noise-aware Layer 2 does NOT gate, but the ambiguous-band
    tier2 increase holds the candidate ambiguous rather than cleanly keep."""
    v = _evaluate(66)
    assert v.decision == "keep"
    assert v.classification == "ambiguous"
    l2 = v.tier_breakdown["tier2_critical_flow"]
    amb_cases = {a.split(":", 1)[-1] for a in l2["ambiguous_increases"]}
    assert any("wmkb" in c for c in amb_cases), l2["ambiguous_increases"]
    # No STRONG tier2 regression and no cross-case discard.
    assert l2["strong_regressions"] == []
    # The wmkb knife-edge sits just under the 0.80 discard threshold.
    wmkb = next(i for i in l2["increases"] if "wmkb" in i["case_id"])
    assert 0.50 <= wmkb["P_regress"] < 0.80


def test_oracle_exp72_shadow_released_via_c2():
    """exp-72: historically discarded on the raw 3pp shadow count gate (shadow
    25%→21.1% ≈ <1 case). The C2 noise-aware shadow posterior releases it —
    the drop is not a statistically-supported regression."""
    v = _evaluate(72)
    assert v.decision == "keep"
    l4 = v.tier_breakdown["shadow_regression"]
    assert l4["regression_detected"] is False
    assert l4["P_regress"] < 0.80
    # The drop is real but small (sub-1-case wobble), ~3-4pp.
    assert 0.0 < l4["drop_pct"] < 6.0


def test_oracle_exp69_vs_exp71_cross_case_discrimination():
    """The headline discrimination the count≥2 rule buys: exp-69 (genuine
    multi-case harm, ≥2 strong regressions) DISCARDS; exp-71 (a single strong
    regression + flaky drops) is RELEASED. A zero-tolerance gate killed both."""
    v69 = _evaluate(69)
    v71 = _evaluate(71)
    assert v69.decision == "discard"
    assert v71.decision == "keep"
    # exp-69 has >= cross_case_count strongly-regressed TIER-N cases.
    l1_69 = v69.tier_breakdown["tier1_outcome"]["tier_n"]
    assert len(l1_69["regressed"]) >= l1_69["cross_case_count"]
    # exp-71 has fewer than the threshold (a single strong regression).
    l1_71 = v71.tier_breakdown["tier1_outcome"]["tier_n"]
    assert len(l1_71["regressed"]) < l1_71["cross_case_count"]


def test_oracle_anti_kill_floor_only_majority_flip_not_any_fail():
    """FS anti-误杀 floor is MAJORITY-flip, never any-attempt-fail: exp-78's
    anti-误杀 control majority-flips (discard), but the 4/5 flakes in
    exp-77/exp-79 do NOT trip it (those release)."""
    assert _evaluate(78).decision == "discard"
    assert _evaluate(77).decision == "keep"
    assert _evaluate(79).decision == "keep"
