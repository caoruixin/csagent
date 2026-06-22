"""Observation-only objective-alignment label for autoloop verdicts.

S-Auto-50 deliverable (M-Auto-12 WP1) per
`docs/proposals/m_auto_7_objective_alignment_and_staged_eval.md` §4 + §8.2.

This module answers the question the keep/discard gate does NOT: *did the
candidate move the run's declared PRIMARY objective?* It is computed AFTER the
existing `tier_evaluator.evaluate(...)` verdict, reading ONLY the run's PRIMARY
target list (`pilot_snapshot.primary_targets`) and the gate's own per-case
Tier-1 posteriors (`verdict.tier_breakdown['tier1_outcome']['tier_n']['per_case']`).

It is **strictly observation-only**:

- it never reads or mutates `verdict.decision` / `verdict.discard_reason`;
- it does NOT participate in the 5-layer safety/noise gate;
- `merge_eligible` is RECORDED for a future seed/merge consumer but NO consumer
  is wired here (record-now-wire-later, proposal §8.2 step 2).

Layer classification (iteration_governance §3): `eval_spec` / reporting. No
Tier-0 invariant, no keyword/regex/per-case hardcode. The two thresholds
(`p_improve`, `p_regress`) are SUGGESTED DEFAULTS read from config and
overridable by the adopter (framework-defaults rule); they are NOT Tier-0.

Label taxonomy (proposal §4):

    FULL_SUCCESS    — every PRIMARY target majority-passes.
    OBJECTIVE_KEEP  — >=1 PRIMARY credibly improves (majority-flip OR
                      P_improve >= p_improve) AND no PRIMARY credibly regresses.
    PERIPHERAL_ONLY — gate KEEP, no PRIMARY credible improvement, but a
                      non-PRIMARY case credibly improved AND no PRIMARY is
                      stuck-at-hard-zero / regressed (a benign peripheral keep).
    OFF_TARGET      — no PRIMARY credible improvement (the catch-all).
    UNSCOPED        — the run declared no PRIMARY targets (never crashes).

PERIPHERAL_ONLY vs OFF_TARGET — disambiguation note. The proposal's literal
PERIPHERAL_ONLY rule ("gate KEEP but improvement only on non-PRIMARY") and its
OFF_TARGET rule ("no PRIMARY credible improvement, all P_improve < thr, no
flip") BOTH match the motivating exp-86 (gate KEEP carried by a single
non-PRIMARY improvement, neither PRIMARY improved). The binding pin — both the
proposal §2 headline conclusion and this sub-sprint's contract — is
**exp-86 → OFF_TARGET**. We resolve the overlap with the proposal's own
per-PRIMARY note: *"TIER-F at 0 with P_improve < thr → OFF_TARGET"*. A PRIMARY
that is stuck at hard-zero (or absent from the posteriors) forces OFF_TARGET,
so the PERIPHERAL_ONLY carve-out is reserved for the benign case where the
PRIMARY targets at least *exist and are non-zero* and none regressed. exp-86's
`cs_uc_a_no_ad_id_ad_specific` is exactly "TIER-F at 0, P_improve 0.059 < 0.8"
→ stuck-hard → OFF_TARGET. Both labels remain reachable + tested; the
functional consequence is identical (both are non-merge, non-seed), so the
exact boundary is observation-only.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from .posterior import P_REGRESS

# Suggested default for the credible-improvement bar (proposal §4). Overridable
# via `fitness.objective_alignment.p_improve`. NOT a Tier-0 invariant.
DEFAULT_P_IMPROVE = 0.8

LABELS = (
    "FULL_SUCCESS",
    "OBJECTIVE_KEEP",
    "PERIPHERAL_ONLY",
    "OFF_TARGET",
    "UNSCOPED",
)


@dataclass(frozen=True)
class PrimarySignal:
    """One PRIMARY target's distilled signal, derived from its Tier-1
    posterior entry (or marked absent). Pure projection of the gate's own
    numbers — this module computes no new posterior."""

    case_id: str
    present: bool
    tier: str | None = None
    p_base: float | None = None
    p_cand: float | None = None
    p_improve: float | None = None
    p_regress: float | None = None
    majority_pass: bool = False
    majority_flip: bool = False
    credible_improve: bool = False
    credible_regress: bool = False
    stuck_hard: bool = False


@dataclass(frozen=True)
class ObjectiveAlignment:
    """The observation-only alignment verdict attached to an iteration.

    `merge_eligible` is recorded only; NO consumer reads it yet
    (record-now-wire-later). `diagnostics` carries non-fatal notes such as a
    PRIMARY target that was absent from the posteriors.
    """

    label: str
    merge_eligible: bool
    gate_decision: str
    primary_targets: list[str]
    per_primary: list[dict[str, Any]] = field(default_factory=list)
    absent_primaries: list[str] = field(default_factory=list)
    nonprimary_improved: list[str] = field(default_factory=list)
    thresholds: dict[str, float] = field(default_factory=dict)
    diagnostics: list[str] = field(default_factory=list)


def _cid_of(key: str) -> str:
    """The bare case-id from a per_case key. Keys are "<suite>:<case_id>"
    (e.g. "bad_cases:cs_uc_a_loaded_listing"); a bare key passes through."""
    return key.split(":", 1)[-1] if ":" in key else key


def _match_entry(
    primary: str, per_case: dict[str, dict[str, Any]]
) -> dict[str, Any] | None:
    """Find the posterior entry for a PRIMARY target by its case-id, matching
    either the bare key or the "<suite>:<case_id>" form."""
    for key, entry in (per_case or {}).items():
        if key == primary or _cid_of(key) == primary:
            return entry
    return None


def _primary_signal(
    primary: str,
    per_case: dict[str, dict[str, Any]],
    *,
    p_improve: float,
    p_regress: float,
) -> PrimarySignal:
    entry = _match_entry(primary, per_case)
    if entry is None:
        # Absent from the posteriors (e.g. the verdict short-circuited at the
        # Tier-0 floor, so Tier-1 was never built). Treat as not-improved and
        # stuck-hard; a diagnostic is recorded by the caller.
        return PrimarySignal(case_id=primary, present=False, stuck_hard=True)

    p_base = entry.get("p_base")
    p_cand = entry.get("p_cand")
    pi = entry.get("P_improve")
    pr = entry.get("P_regress")

    majority_pass = p_cand is not None and p_cand > 0.5
    majority_flip = majority_pass and (p_base is None or p_base <= 0.5)
    credible_improve = majority_flip or (pi is not None and pi >= p_improve)
    credible_regress = pr is not None and pr >= p_regress
    # "TIER-F at 0 with P_improve < thr → OFF_TARGET" (proposal §4 per-PRIMARY
    # note), generalized: a present PRIMARY sitting at hard zero with no
    # credible improvement is stuck. (Absent PRIMARIES are handled above.)
    stuck_hard = (
        not credible_improve
        and not majority_pass
        and (p_cand is None or p_cand == 0.0)
    )
    return PrimarySignal(
        case_id=primary,
        present=True,
        tier=entry.get("tier"),
        p_base=p_base,
        p_cand=p_cand,
        p_improve=pi,
        p_regress=pr,
        majority_pass=majority_pass,
        majority_flip=majority_flip,
        credible_improve=credible_improve,
        credible_regress=credible_regress,
        stuck_hard=stuck_hard,
    )


def _nonprimary_improved(
    per_case: dict[str, dict[str, Any]],
    primary_set: set[str],
    *,
    p_improve: float,
) -> list[str]:
    """Per-case keys (non-PRIMARY) that credibly improved — the same
    credible-improvement bar applied to the gate's non-PRIMARY posteriors.
    Used only to distinguish a peripheral keep from a fully off-target one."""
    out: list[str] = []
    for key, entry in (per_case or {}).items():
        if _cid_of(key) in primary_set or key in primary_set:
            continue
        p_base = entry.get("p_base")
        p_cand = entry.get("p_cand")
        pi = entry.get("P_improve")
        flip = (p_cand is not None and p_cand > 0.5) and (
            p_base is None or p_base <= 0.5
        )
        if flip or (pi is not None and pi >= p_improve):
            out.append(key)
    return out


def classify(
    primary_targets: list[str] | None,
    per_case_tier_n: dict[str, dict[str, Any]] | None,
    gate_decision: str,
    *,
    p_improve: float = DEFAULT_P_IMPROVE,
    p_regress: float = P_REGRESS,
) -> ObjectiveAlignment:
    """Deterministic, pure, no-I/O classifier.

    Inputs are the run's PRIMARY target list, the gate's per-case Tier-1
    posteriors (`tier_breakdown['tier1_outcome']['tier_n']['per_case']`, keyed
    "<suite>:<case_id>"), and the gate decision ("keep" / "discard"). Returns
    an `ObjectiveAlignment`. Never mutates its inputs.
    """
    primaries = list(primary_targets or [])
    per_case = per_case_tier_n or {}
    is_keep = gate_decision == "keep"

    if not primaries:
        return ObjectiveAlignment(
            label="UNSCOPED",
            merge_eligible=False,
            gate_decision=gate_decision,
            primary_targets=[],
            thresholds={"p_improve": p_improve, "p_regress": p_regress},
            diagnostics=["no_primary_targets_declared"],
        )

    signals = [
        _primary_signal(p, per_case, p_improve=p_improve, p_regress=p_regress)
        for p in primaries
    ]
    primary_set = set(primaries)

    improved = [s.case_id for s in signals if s.credible_improve]
    regressed = [s.case_id for s in signals if s.credible_regress]
    stuck = [s.case_id for s in signals if s.stuck_hard]
    absent = [s.case_id for s in signals if not s.present]
    all_pass = all(s.majority_pass for s in signals)
    nonprimary_improved = _nonprimary_improved(
        per_case, primary_set, p_improve=p_improve
    )

    if all_pass:
        label = "FULL_SUCCESS"
    elif improved and not regressed:
        label = "OBJECTIVE_KEEP"
    elif (
        is_keep
        and not improved
        and not regressed
        and not stuck
        and nonprimary_improved
    ):
        label = "PERIPHERAL_ONLY"
    else:
        label = "OFF_TARGET"

    merge_eligible = is_keep and label in ("FULL_SUCCESS", "OBJECTIVE_KEEP")

    diagnostics: list[str] = []
    if absent:
        diagnostics.append("primary_absent_from_posteriors:" + ",".join(absent))

    return ObjectiveAlignment(
        label=label,
        merge_eligible=merge_eligible,
        gate_decision=gate_decision,
        primary_targets=primaries,
        per_primary=[_signal_dict(s) for s in signals],
        absent_primaries=absent,
        nonprimary_improved=nonprimary_improved,
        thresholds={"p_improve": p_improve, "p_regress": p_regress},
        diagnostics=diagnostics,
    )


def _signal_dict(s: PrimarySignal) -> dict[str, Any]:
    return {
        "case_id": s.case_id,
        "present": s.present,
        "tier": s.tier,
        "p_base": s.p_base,
        "p_cand": s.p_cand,
        "P_improve": s.p_improve,
        "P_regress": s.p_regress,
        "majority_pass": s.majority_pass,
        "majority_flip": s.majority_flip,
        "credible_improve": s.credible_improve,
        "credible_regress": s.credible_regress,
        "stuck_hard": s.stuck_hard,
    }


def _thresholds_from_config(config: dict[str, Any] | None) -> tuple[float, float]:
    """Read the two thresholds from config, defaulting to the suggested values.

    `p_improve`  ← `fitness.objective_alignment.p_improve` (default 0.8).
    `p_regress`  ← `fitness.tier_decision.p_regress` (the gate's own threshold;
                   reused so "credible regression" means the same thing here as
                   in the Tier-1 gate). Defaults to the posterior P_REGRESS.
    """
    fitness_cfg = (config or {}).get("fitness") or {}
    oa_cfg = fitness_cfg.get("objective_alignment") or {}
    td_cfg = fitness_cfg.get("tier_decision") or {}
    try:
        p_improve = float(oa_cfg.get("p_improve", DEFAULT_P_IMPROVE))
    except (TypeError, ValueError):
        p_improve = DEFAULT_P_IMPROVE
    try:
        p_regress = float(td_cfg.get("p_regress", P_REGRESS))
    except (TypeError, ValueError):
        p_regress = P_REGRESS
    return p_improve, p_regress


def evaluate_alignment(
    verdict: Any,
    primary_targets: list[str] | None,
    config: dict[str, Any] | None = None,
) -> ObjectiveAlignment:
    """Convenience wrapper for the loop: extract the per-case Tier-1 posteriors
    from a `LexicographicVerdict`, read the thresholds from config, and
    classify. Reads `verdict.decision` only (never writes it). Safe on a
    short-circuited verdict whose `tier1_outcome` block is absent (→ empty
    posteriors → PRIMARIES absent → OFF_TARGET + diagnostic)."""
    p_improve, p_regress = _thresholds_from_config(config)
    tier_breakdown = getattr(verdict, "tier_breakdown", None) or {}
    per_case = (
        (tier_breakdown.get("tier1_outcome") or {})
        .get("tier_n", {})
        .get("per_case", {})
    )
    gate_decision = getattr(verdict, "decision", "discard")
    return classify(
        primary_targets,
        per_case,
        gate_decision,
        p_improve=p_improve,
        p_regress=p_regress,
    )
