"""Unit tests for the observation-only objective-alignment classifier
(S-Auto-50 / M-Auto-12 WP1).

Covers the four labels + UNSCOPED + edge cases, config-default-overridable
thresholds (an override flips the label), and the structural invariant that the
classifier never mutates the verdict / its inputs. Zero-LLM, zero-I/O — pure
function under test.
"""

from __future__ import annotations

import copy
from dataclasses import FrozenInstanceError

import pytest

from autoloop.scoring import objective_alignment as oa
from autoloop.scoring.objective_alignment import classify, evaluate_alignment


# --- per_case fixtures (mirror tier_breakdown['tier1_outcome']['tier_n']
#     ['per_case'] entries; keys are "<suite>:<case_id>") ----------------


def _entry(tier, p_base, p_cand, P_improve, P_regress):
    return {
        "tier": tier,
        "p_base": p_base,
        "p_cand": p_cand,
        "P_improve": P_improve,
        "P_regress": P_regress,
    }


# --- The four labels ---------------------------------------------------


def test_full_success_all_primaries_majority_pass():
    per_case = {
        "bad_cases:cs_a": _entry("TIER-N", 0.2, 0.7, 0.9, 0.02),
        "bad_cases:cs_b": _entry("TIER-F", 0.0, 0.8, 0.95, 0.01),
    }
    r = classify(["cs_a", "cs_b"], per_case, "keep")
    assert r.label == "FULL_SUCCESS"
    assert r.merge_eligible is True


def test_objective_keep_one_primary_improves_no_regression():
    # cs_a credibly improves (P_improve >= 0.8) but does not majority-pass;
    # cs_b is flat but does not regress.
    per_case = {
        "bad_cases:cs_a": _entry("TIER-N", 0.2, 0.45, 0.85, 0.05),
        "bad_cases:cs_b": _entry("TIER-N", 0.3, 0.3, 0.1, 0.1),
    }
    r = classify(["cs_a", "cs_b"], per_case, "keep")
    assert r.label == "OBJECTIVE_KEEP"
    assert r.merge_eligible is True


def test_objective_keep_via_majority_flip():
    # cs_a flips from a baseline majority-fail to a candidate majority-pass.
    per_case = {"bad_cases:cs_a": _entry("TIER-N", 0.4, 0.6, 0.5, 0.1)}
    r = classify(["cs_a"], per_case, "keep")
    assert r.label == "FULL_SUCCESS"  # also majority-passes
    # A flip that does NOT reach majority-pass overall still counts as improve:
    per_case2 = {
        "bad_cases:cs_a": _entry("TIER-N", 0.4, 0.6, 0.5, 0.1),
        "bad_cases:cs_b": _entry("TIER-N", 0.3, 0.3, 0.1, 0.1),
    }
    r2 = classify(["cs_a", "cs_b"], per_case2, "keep")
    assert r2.label == "OBJECTIVE_KEEP"


def test_peripheral_only_keep_with_nonprimary_improvement_no_stuck_primary():
    # Gate KEEP, no PRIMARY credible improvement, both PRIMARIES present and
    # non-zero (not stuck-hard), a NON-PRIMARY case credibly improved.
    per_case = {
        "bad_cases:cs_a": _entry("TIER-N", 0.3, 0.3, 0.1, 0.1),
        "bad_cases:cs_b": _entry("TIER-N", 0.25, 0.25, 0.05, 0.1),
        "anchor_outcome:other": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01),
    }
    r = classify(["cs_a", "cs_b"], per_case, "keep")
    assert r.label == "PERIPHERAL_ONLY"
    assert r.merge_eligible is False
    assert r.nonprimary_improved == ["anchor_outcome:other"]


def test_off_target_no_primary_credible_improvement():
    # No PRIMARY improvement and a PRIMARY stuck at hard zero (TIER-F at 0 with
    # P_improve < thr → OFF_TARGET, even with a non-PRIMARY improvement present).
    per_case = {
        "bad_cases:cs_a": _entry("TIER-F", 0.0, 0.0, 0.059, 0.084),
        "bad_cases:cs_b": _entry("TIER-N", 0.091, 0.077, 0.146, 0.223),
        "anchor_outcome:other": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01),
    }
    r = classify(["cs_a", "cs_b"], per_case, "keep")
    assert r.label == "OFF_TARGET"
    assert r.merge_eligible is False


def test_off_target_primary_credible_regression_blocks_objective_keep():
    # One PRIMARY improves, another credibly regresses → not OBJECTIVE_KEEP.
    per_case = {
        "bad_cases:cs_a": _entry("TIER-N", 0.2, 0.45, 0.85, 0.05),
        "bad_cases:cs_b": _entry("TIER-N", 0.6, 0.3, 0.02, 0.9),
    }
    r = classify(["cs_a", "cs_b"], per_case, "keep")
    assert r.label == "OFF_TARGET"
    assert r.merge_eligible is False


# --- UNSCOPED + edge cases --------------------------------------------


def test_unscoped_empty_primaries_never_crashes():
    r = classify([], {"bad_cases:cs_a": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01)}, "keep")
    assert r.label == "UNSCOPED"
    assert r.merge_eligible is False
    # None primary_targets is equivalent.
    assert classify(None, None, "discard").label == "UNSCOPED"


def test_primary_absent_from_posteriors_recorded_as_diagnostic():
    # PRIMARY not in per_case (e.g. the verdict short-circuited at Tier-0).
    r = classify(["cs_missing"], {"bad_cases:other": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01)}, "discard")
    assert r.label == "OFF_TARGET"
    assert r.absent_primaries == ["cs_missing"]
    assert any("primary_absent_from_posteriors" in d for d in r.diagnostics)
    assert r.per_primary[0]["present"] is False
    assert r.per_primary[0]["stuck_hard"] is True


def test_discard_verdict_still_labelled_merge_ineligible():
    # A DISCARD where a PRIMARY credibly improved is still labelled (OBJECTIVE_KEEP)
    # but is NOT merge-eligible (merge requires gate KEEP).
    per_case = {"bad_cases:cs_a": _entry("TIER-N", 0.2, 0.45, 0.9, 0.05)}
    r = classify(["cs_a"], per_case, "discard")
    assert r.label == "OBJECTIVE_KEEP"
    assert r.merge_eligible is False


def test_discard_never_peripheral_only():
    # PERIPHERAL_ONLY requires gate KEEP; the same shape under DISCARD → OFF_TARGET.
    per_case = {
        "bad_cases:cs_a": _entry("TIER-N", 0.3, 0.3, 0.1, 0.1),
        "anchor_outcome:other": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01),
    }
    assert classify(["cs_a"], per_case, "keep").label == "PERIPHERAL_ONLY"
    assert classify(["cs_a"], per_case, "discard").label == "OFF_TARGET"


def test_suite_prefixed_and_bare_keys_both_match():
    per_case_prefixed = {"bad_cases:cs_a": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01)}
    per_case_bare = {"cs_a": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01)}
    assert classify(["cs_a"], per_case_prefixed, "keep").label == "FULL_SUCCESS"
    assert classify(["cs_a"], per_case_bare, "keep").label == "FULL_SUCCESS"


# --- thresholds are config-default-overridable -------------------------


def test_threshold_override_flips_label():
    # cs_a sits at P_improve 0.146 (the exp-86 loaded_listing posterior) and a
    # second PRIMARY stuck at hard zero. Default p_improve=0.8 → OFF_TARGET.
    per_case = {
        "bad_cases:cs_a": _entry("TIER-N", 0.091, 0.077, 0.146, 0.223),
        "bad_cases:cs_b": _entry("TIER-F", 0.0, 0.0, 0.059, 0.084),
    }
    default = classify(["cs_a", "cs_b"], per_case, "keep")
    assert default.label == "OFF_TARGET"
    # Lowering the credible-improvement bar makes cs_a improve → OBJECTIVE_KEEP.
    lowered = classify(["cs_a", "cs_b"], per_case, "keep", p_improve=0.1)
    assert lowered.label == "OBJECTIVE_KEEP"
    assert lowered.thresholds["p_improve"] == 0.1


def test_p_regress_threshold_override_changes_regression_verdict():
    per_case = {"bad_cases:cs_a": _entry("TIER-N", 0.6, 0.45, 0.05, 0.6)}
    # Default p_regress=0.8: P_regress 0.6 is NOT a credible regression.
    assert not classify(["cs_a"], per_case, "discard").per_primary[0]["credible_regress"]
    # Lower the bar to 0.5: now it IS a credible regression.
    r = classify(["cs_a"], per_case, "discard", p_regress=0.5)
    assert r.per_primary[0]["credible_regress"] is True


def test_evaluate_alignment_reads_thresholds_from_config():
    class _V:
        decision = "keep"
        tier_breakdown = {
            "tier1_outcome": {
                "tier_n": {
                    "per_case": {
                        "bad_cases:cs_a": _entry("TIER-N", 0.091, 0.077, 0.146, 0.223),
                        "bad_cases:cs_b": _entry("TIER-F", 0.0, 0.0, 0.059, 0.084),
                    }
                }
            }
        }

    cfg_default = {"fitness": {"tier_decision": {"p_regress": 0.8}}}
    assert evaluate_alignment(_V(), ["cs_a", "cs_b"], cfg_default).label == "OFF_TARGET"
    cfg_override = {
        "fitness": {
            "tier_decision": {"p_regress": 0.8},
            "objective_alignment": {"p_improve": 0.1},
        }
    }
    assert evaluate_alignment(_V(), ["cs_a", "cs_b"], cfg_override).label == "OBJECTIVE_KEEP"


def test_evaluate_alignment_short_circuited_verdict_no_tier1_block():
    # A verdict that short-circuited at Tier-0 has no `tier1_outcome` block.
    class _V:
        decision = "discard"
        tier_breakdown = {"tier0_safety": {}}

    r = evaluate_alignment(_V(), ["cs_a"], {"fitness": {}})
    assert r.label == "OFF_TARGET"
    assert r.absent_primaries == ["cs_a"]


# --- structural invariants: observation-only, no mutation --------------


def test_classifier_does_not_mutate_inputs():
    per_case = {
        "bad_cases:cs_a": _entry("TIER-F", 0.0, 0.0, 0.059, 0.084),
        "anchor_outcome:other": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01),
    }
    primaries = ["cs_a"]
    per_case_before = copy.deepcopy(per_case)
    primaries_before = list(primaries)
    classify(primaries, per_case, "keep")
    assert per_case == per_case_before
    assert primaries == primaries_before


def test_evaluate_alignment_does_not_mutate_verdict_decision():
    class _V:
        decision = "keep"
        discard_reason = None
        tier_breakdown = {
            "tier1_outcome": {"tier_n": {"per_case": {"bad_cases:cs_a": _entry("TIER-F", 0.0, 0.0, 0.05, 0.08)}}}
        }

    v = _V()
    evaluate_alignment(v, ["cs_a"], {"fitness": {}})
    assert v.decision == "keep"
    assert v.discard_reason is None


def test_result_is_frozen_dataclass():
    r = classify(["cs_a"], {"bad_cases:cs_a": _entry("TIER-N", 0.2, 0.7, 0.9, 0.01)}, "keep")
    with pytest.raises(FrozenInstanceError):
        r.label = "MUTATED"  # type: ignore[misc]
