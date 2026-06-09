"""Composite scorer -- aggregates L1/L2/L3 scores into a final result."""

from __future__ import annotations

from dataclasses import dataclass, field

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.skill_procedure_check import (
    Tier2Result,
    tier2_results_to_gate,
)
from eval_interactive.scoring.stall_detector import StallResult


# ---------------------------------------------------------------------------
# Mandatory L2 gates (Wave B1.1)
# ---------------------------------------------------------------------------
#
# Mandatory L2 outcome checks must pass for ``case_passed`` to be True. These
# gates close the audit-finding hole where a strong L3 judge score could mask
# an outcome failure (e.g. ``correct_uc=0`` or ``correct_outcome=0``).
#
# Always-mandatory checks: required for every case.
# Conditionally-mandatory: required only when the case spec triggers them.
# Fail-closed: if a mandatory check is configured-but-missing from
# ``l2_results`` (or simply absent), it is treated as a failure.

_ALWAYS_MANDATORY_L2: tuple[str, ...] = (
    "correct_uc",
    "correct_outcome",
)


# Codex 2026-05-04 round 4 §"What Should Become Soft or Diagnostic":
# tool_sequence_match is now a diagnostic dimension, not a release gate.
# The previous round's >=0.9 threshold has been removed. Defaults to 1.0
# for any check still listed as a mandatory gate.
_GATE_THRESHOLDS: dict[str, float] = {}


def _conditional_mandatory_l2(case_spec: CaseSpec) -> tuple[str, ...]:
    """Return the conditionally-mandatory L2 check names for this case.

    S-Cleanup-3 (#4) demoted ``handover_completeness`` and
    ``case_id_present`` from mandatory-on-escalate to Tier-3 advisory
    per the M3-Eval four-tier pyramid: they are still computed and
    recorded in ``l2_results`` for trend reporting but no longer flip
    ``case_passed``. Mirrors the S-Eval-1 (D-2.5) severity convention
    where advisory checks are observation, not gating. Safety floor is
    unaffected — these were process-completeness checks, not safety
    invariants (Tier-0 safety lives in ``hard_checks.py``).

    Codex 2026-05-04 round 4 (pre-S-Cleanup-3 history) introduced the
    two checks as mandatory because losing the linkage produced a
    useless handover; M3-Eval reclassified them as Tier-3 advisory
    because the four-tier pyramid treats process-completeness as
    observation, not a release gate.

    Note: ``escalation_compliance`` is *intentionally not* listed here.
    It is an L1 hard check (see ``hard_checks.py``) that runs globally on
    every case, so the L1 gate already enforces it. Listing it at L2
    would (a) duplicate logic and (b) fail-close the composite as
    ``L2_GATE_MISSING`` whenever the L2 list does not also configure it
    -- which masked otherwise-passing escalate cases (HIGH-5).
    """
    return ()


def _mandatory_l2_names(case_spec: CaseSpec) -> tuple[str, ...]:
    """Full mandatory L2 set for this case (always + conditional)."""
    return _ALWAYS_MANDATORY_L2 + _conditional_mandatory_l2(case_spec)


def _compute_mandatory_l2(
    case_spec: CaseSpec,
    l2_results: list[OutcomeCheckResult],
) -> list[OutcomeCheckResult]:
    """Return the subset of ``l2_results`` that are mandatory for this case.

    Order follows the canonical ``_mandatory_l2_names`` ordering. Missing
    mandatory checks are NOT synthesized here -- the gate logic in
    ``compute_composite`` is responsible for fail-closed handling so that a
    missing check still produces an explicit failure tag.

    S-Eval-1 (M3-Eval): when ``case_spec.scoring.outcome_checks`` is empty
    (the spec opted out of per-case L2 gating, as the new outcome-only
    ``anchor_outcome`` fixtures do), this returns an empty list and the
    mandatory-L2 gate is skipped entirely.
    """
    if not case_spec.scoring.outcome_checks:
        return []
    mandatory_names = set(_mandatory_l2_names(case_spec))
    return [r for r in l2_results if r.check_name in mandatory_names]


@dataclass
class CompositeScore:
    """Aggregated score for a single evaluation case."""

    case_id: str
    case_passed: bool  # All L1 hard checks AND all mandatory L2 gates passed
    l1_results: list[HardCheckResult]
    l2_results: list[OutcomeCheckResult]
    l3_results: list[JudgeResult]
    outcome_score: float  # mean(L2 scores)
    judge_score: float  # mean(L3 scores) / 5.0 -> 0..1
    composite: float  # 0.5 * outcome + 0.5 * judge (or 0.0 if not case_passed)
    stall_result: StallResult
    failure_tags: list[str] = field(default_factory=list)
    # Wave B1.1 diagnostics: surface mandatory-L2 gate state to executor /
    # report layers so they can distinguish L1 failures from L2 gate failures.
    mandatory_l2_passed: bool = True
    mandatory_l2_failures: list[str] = field(default_factory=list)
    # Sprint 43 (S-Eval-2): NEW Tier-2 ``skill_procedure_followship`` gate
    # band. Default is the empty-list / advisory PASS produced by
    # ``tier2_results_to_gate(())``; existing callers that have not yet
    # been updated keep the legacy two-tier gate semantics.
    tier2_result: Tier2Result = field(
        default_factory=lambda: tier2_results_to_gate(())
    )
    # OQ-S77 (S-Auto-22): when a post-composite false-positive gate promotes
    # an otherwise-"passing" verdict to FAIL, records which gate fired
    # (``stall_promoted`` / ``no_l2_evidence_to_pass``). Empty string when no
    # override applied. The same reason is also surfaced as a
    # ``VERDICT_OVERRIDE:<reason>`` failure tag so it reaches the serialised
    # ``case_results[].failure_tags`` without a separate executor field.
    verdict_reason: str = ""
    detail: str = ""


def compute_composite(
    case_id: str,
    l1_results: list[HardCheckResult],
    l2_results: list[OutcomeCheckResult],
    l3_results: list[JudgeResult],
    stall_result: StallResult,
    case_spec: CaseSpec | None = None,
    tier2_result: Tier2Result | None = None,
) -> CompositeScore:
    """Aggregate all scoring layers into a single CompositeScore.

    Rules (Wave B1.1, refined HIGH-5; Sprint 43 S-Eval-2 adds Tier-2;
    S-Cleanup-3 (#4) demotes ``handover_completeness`` +
    ``case_id_present`` to Tier-3 advisory):
    - ``case_passed = all(l1) AND all(mandatory_l2) AND tier2_passed``
    - Mandatory L2 always: ``correct_uc``, ``correct_outcome``
    - ``handover_completeness`` and ``case_id_present`` are now Tier-3
      advisory (computed + recorded, not gating) per the M3-Eval
      four-tier pyramid.
    - ``escalation_compliance`` is an L1 hard check (global) -- not an
      L2 gate. The L1 gate already covers it.
    - Missing mandatory check is fail-closed (treated as a failure).
    - Tier-2 (Sprint 43 / S-Eval-2): the
      ``skill_procedure_followship`` extractor produces per-step
      PASS/FAIL/N/A; ``tier2_results_to_gate`` reduces them to a
      ``Tier2Result`` with severity ``critical`` (a mandatory step
      failed) or ``advisory`` (only advisory steps failed, or no
      applicable steps). Only ``severity="critical"`` Tier-2 fails
      flip ``case_passed``. Callers that do not pass ``tier2_result``
      get the empty-list default (Tier-2 PASS / advisory; no gate
      effect — backward-compat with pre-Sprint-43 call sites).
    - ``outcome_score = mean(L2 scores)`` if any L2 results, else 0.0
    - ``judge_score = mean(L3 scores) / 5.0`` if any L3 results, else 0.0
    - ``composite = 0.0 if not case_passed else 0.5 * outcome + 0.5 * judge``
    - A case is "successful" if ``case_passed AND composite >= 0.7`` (the
      0.7 threshold lives in the executor / summary layer, not here).

    ``case_spec`` is optional only for backwards compatibility with callers
    that have not yet been updated. When omitted, mandatory-L2 gating is
    skipped (legacy behaviour: only L1 gates apply). New callers should
    always pass it.
    """
    # -- L1 gate --
    # S-Eval-1 (M3-Eval): results tagged ``severity="advisory"`` are recorded
    # for diagnostics but do not contribute to the gate (e.g. demoted
    # ``no_forbidden_tools`` violations on outcome-only specs).
    critical_l1 = [r for r in l1_results if getattr(r, "severity", "critical") != "advisory"]
    l1_passed = all(r.passed for r in critical_l1) if critical_l1 else True

    # -- Mandatory L2 gate (Wave B1.1; S-Eval-1 opt-out when scoring.outcome_checks is empty) --
    mandatory_l2_passed = True
    mandatory_l2_failures: list[str] = []

    if case_spec is not None and case_spec.scoring.outcome_checks:
        mandatory_names = _mandatory_l2_names(case_spec)
        results_by_name = {r.check_name: r for r in l2_results}
        for name in mandatory_names:
            r = results_by_name.get(name)
            if r is None:
                # Fail-closed: a configured-but-missing mandatory check is a
                # gate failure. Tag form: ``L2_GATE_MISSING:<name>``.
                mandatory_l2_passed = False
                mandatory_l2_failures.append(name)
            elif not _outcome_passed(r):
                mandatory_l2_passed = False
                mandatory_l2_failures.append(name)

    # -- Tier-2 gate (Sprint 43 / S-Eval-2 — skill_procedure_followship) --
    # Tier-2 contributes to case_passed only when its severity is "critical"
    # (i.e., a mandatory critical_step on the active Skill failed for a
    # case whose active_use_case matches the step's mandatory_for set).
    # Advisory-severity Tier-2 results never flip case_passed, mirroring
    # the S-Eval-1 (D-2.5) severity convention on HardCheckResult /
    # OutcomeCheckResult.
    if tier2_result is None:
        tier2_result = tier2_results_to_gate(())
    tier2_critical_failed = (not tier2_result.passed) and tier2_result.severity == "critical"

    case_passed = l1_passed and mandatory_l2_passed and not tier2_critical_failed

    # -- L2 mean --
    # S-Eval-1 (M3-Eval): exclude advisory results (Tier-3 diagnostics) from
    # the outcome-score mean so demoted dims like ``tool_sequence_match`` do
    # not skew the composite.
    gating_l2 = [r for r in l2_results if getattr(r, "severity", "critical") != "advisory"]
    if gating_l2:
        outcome_score = sum(r.score for r in gating_l2) / len(gating_l2)
    else:
        outcome_score = 0.0

    # -- L3 mean (normalized to 0..1) --
    # S-Eval-5 (M3-Eval): exclude advisory L3 dims from the judge_score
    # mean so the three demoted dims (`groundedness`, `relevance`,
    # `tone_appropriateness`) and the new `user_goal_achievement`
    # supplementary dim do not factor into `composite_score`. Their
    # numeric scores remain on `l3_results` and continue to be
    # serialised into `case_results[].l3_results[]` for trend
    # reporting. Mirrors the S-Eval-1 (D-2.5) severity convention
    # already applied to L1 / L2 above. Critical L3 dims
    # (`premature_finish`, `stall_quality`) continue to feed the
    # judge_score mean as before.
    gating_l3 = [r for r in l3_results if getattr(r, "severity", "critical") != "advisory"]
    if gating_l3:
        judge_score = (sum(r.score for r in gating_l3) / len(gating_l3)) / 5.0
    else:
        judge_score = 0.0

    # -- Composite --
    if case_passed:
        composite = 0.5 * outcome_score + 0.5 * judge_score
    else:
        composite = 0.0

    # -- OQ-S77 (S-Auto-22): post-composite false-positive gates --
    # The S-Auto-21 simfixed re-bless exposed an OPPOSITE-direction
    # measurement artifact to the S-Auto-19/20/21 false-negative fixes: a
    # session that stalled / looped / reached an impossible terminal could
    # still report ``case_passed=True`` when an earlier turn stamped
    # ``containment_outcome="resolved"`` AND ``l2_results=[]`` left the
    # mandatory-L2 gate vacuously True (see
    # ``docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md``). The two
    # gates below promote such a verdict to FAIL. They ONLY ever flip
    # pass->fail (never fail->pass), so they cannot mask a real L1/L2/Tier-2
    # failure already caught above. The terminal-failure-vs-resolved-stamp
    # contradiction (OQ-S77 #2) is handled upstream in
    # ``hard_checks._check_trace_minimum`` (an L1 fail that flows through
    # ``l1_passed`` here), so it is not re-implemented in this block.
    verdict_reason = ""
    if case_passed:
        if stall_result.detected and composite == 0.0:
            # OQ-S77 #1 (option 1a — read the typed ``stall_result.detected``
            # boolean directly; no string-prefix matching). A detected stall
            # that produced NO positively-scored outcome (composite == 0) is a
            # session-level failure regardless of L2/judge/containment.
            #
            # Scope discipline: gated on ``composite == 0`` rather than a
            # blanket "any stall -> fail". A blanket promotion would mis-fail a
            # draw the stall detector FALSE-flags on an early "let me look into
            # this" that the session then recovers from with a valid scored
            # outcome (composite > 0) — e.g. a validly-escalated draw whose
            # recovery/escalation falls outside the detector's follow-up window
            # (observed on cs095 a4: stall_detected=True, escalated,
            # composite=0.5). Flipping such a draw would drop its case below
            # majority and silently lose a legitimate F->P flip
            # (anti-误杀 invariant §5.1). ``composite == 0`` isolates the
            # genuine terminal stalls (no scored recovery) from the detector's
            # out-of-window false positives. This selectivity is also why
            # ``TIER2_ADVISORY:*`` and other tag families are NOT promoted.
            case_passed = False
            composite = 0.0
            verdict_reason = "stall_promoted"
        elif composite == 0.0 and not l2_results:
            # OQ-S77 #3: refuse a "pass" that rests on ZERO positive evidence —
            # no L2 outcome checks recorded (``l2_results == []``) AND a zero
            # composite (so no gating L3 signal either). A ``resolved``
            # containment stamp alone is not a sufficient basis to pass; the
            # case needs an L2 outcome check or a judge signal.
            #
            # This is the structural form of option 3a WITHOUT a per-case
            # allowlist: ``composite == 0`` is itself self-limiting, so a
            # legitimately-resolved case that earned any L2/L3 credit
            # (composite > 0) is never caught, and no legitimate pass on the
            # corpus matches this fingerprint. We deliberately do NOT key on
            # ``containment_outcome`` here (which would require threading the
            # trace into ``compute_composite`` / editing the executor, outside
            # this sub-sprint's scope): a pass with zero scored evidence has no
            # basis to pass regardless of the stamp, so the broader predicate
            # is strictly more conservative and stays within the scoring layer.
            case_passed = False
            verdict_reason = "no_l2_evidence_to_pass"

    # -- Failure tags --
    failure_tags: list[str] = []

    for r in l1_results:
        if not r.passed:
            failure_tags.append(f"L1:{r.check_name}")

    # Mandatory-L2 gate-failure tags (distinct prefix so reports can pull
    # them out without double-counting against the generic L2 noise floor).
    for name in mandatory_l2_failures:
        if name in {r.check_name for r in l2_results}:
            failure_tags.append(f"L2_GATE:{name}")
        else:
            failure_tags.append(f"L2_GATE_MISSING:{name}")

    # Generic low-L2 tags (kept for backward compatibility with reports).
    for r in l2_results:
        if r.score < 0.5:
            failure_tags.append(f"L2:{r.check_name}")

    # S-Eval-5 (M3-Eval): advisory-severity L3 dims (the three demoted
    # legacy dims + new ``user_goal_achievement``) get a distinct
    # ``L3_ADVISORY:`` prefix so reports can pull them apart from the
    # critical-severity ``L3:`` failure floor (parity with the
    # ``TIER2_ADVISORY:`` convention from Sprint 43 / S-Eval-2).
    # Critical L3 dims keep the legacy ``L3:`` prefix for backward
    # compatibility with downstream readers.
    for r in l3_results:
        if r.score < 3.0:
            if getattr(r, "severity", "critical") == "advisory":
                failure_tags.append(f"L3_ADVISORY:{r.dimension}")
            else:
                failure_tags.append(f"L3:{r.dimension}")

    if stall_result.detected:
        tag = stall_result.failure_tag or "STALL"
        failure_tags.append(f"STALL:{tag}")

    # OQ-S77 (S-Auto-22): record which post-composite false-positive gate (if
    # any) flipped the verdict, so the reason reaches the serialised
    # ``case_results[].failure_tags`` without adding a new executor field.
    if verdict_reason:
        failure_tags.append(f"VERDICT_OVERRIDE:{verdict_reason}")

    # Sprint 43 (S-Eval-2): Tier-2 failure tags. Only critical-severity
    # failures contribute to the case_passed gate but advisory failures
    # are still surfaced as tags for trend reporting (mirrors how
    # advisory L1 results are recorded after S-Eval-1 D-2.5).
    if tier2_critical_failed:
        for step_id in tier2_result.failed_step_ids:
            failure_tags.append(f"TIER2:{step_id}")
    elif tier2_result.severity == "advisory" and tier2_result.failed_step_ids:
        # Advisory-only failures: gate stays PASS but the failed step
        # ids are surfaced as TIER2_ADVISORY:<id> tags for trend reports.
        for step_id in tier2_result.failed_step_ids:
            failure_tags.append(f"TIER2_ADVISORY:{step_id}")

    # -- Detail / explanation --
    detail = _build_detail(
        case_passed=case_passed,
        l1_passed=l1_passed,
        mandatory_l2_passed=mandatory_l2_passed,
        tier2_critical_failed=tier2_critical_failed,
        l1_failures=[r.check_name for r in l1_results if not r.passed],
        mandatory_l2_failures=mandatory_l2_failures,
        tier2_failed_step_ids=(
            list(tier2_result.failed_step_ids) if tier2_critical_failed else []
        ),
        verdict_reason=verdict_reason,
    )

    return CompositeScore(
        case_id=case_id,
        case_passed=case_passed,
        l1_results=l1_results,
        l2_results=l2_results,
        l3_results=l3_results,
        outcome_score=round(outcome_score, 4),
        judge_score=round(judge_score, 4),
        composite=round(composite, 4),
        stall_result=stall_result,
        failure_tags=failure_tags,
        mandatory_l2_passed=mandatory_l2_passed,
        mandatory_l2_failures=mandatory_l2_failures,
        tier2_result=tier2_result,
        verdict_reason=verdict_reason,
        detail=detail,
    )


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _outcome_passed(r: OutcomeCheckResult) -> bool:
    """An outcome check "passes" the gate iff its score meets the threshold.

    Mandatory L2 gates are pass/fail in spirit even though the underlying
    OutcomeCheckResult carries a 0..1 score. The default threshold is 1.0
    so that partial-credit (e.g. handover completeness with one missing
    field) still trips the gate -- this matches the audit's intent:
    outcome failures must not be masked by graded L3 scores. Specific
    checks may override the threshold via ``_GATE_THRESHOLDS`` when the
    underlying scorer awards graded partial credit and a near-match is
    operationally acceptable (Codex 2026-05-03 round 3 — applies to
    ``tool_sequence_match`` only).
    """
    threshold = _GATE_THRESHOLDS.get(r.check_name, 1.0)
    return r.score + 1e-9 >= threshold


def _build_detail(
    *,
    case_passed: bool,
    l1_passed: bool,
    mandatory_l2_passed: bool,
    tier2_critical_failed: bool,
    l1_failures: list[str],
    mandatory_l2_failures: list[str],
    tier2_failed_step_ids: list[str],
    verdict_reason: str = "",
) -> str:
    """Human-readable explanation of the gate outcome."""
    if case_passed:
        return (
            "case_passed=True (all L1, all mandatory L2, and Tier-2 "
            "skill_procedure_followship gates passed)"
        )

    parts: list[str] = ["case_passed=False"]
    if not l1_passed:
        parts.append(f"L1 failed: {l1_failures}")
    if not mandatory_l2_passed:
        parts.append(f"mandatory L2 failed/missing: {mandatory_l2_failures}")
    if tier2_critical_failed:
        parts.append(f"Tier-2 mandatory critical_steps failed: {tier2_failed_step_ids}")
    # OQ-S77 (S-Auto-22): a post-composite false-positive gate may flip the
    # verdict even when L1 / mandatory-L2 / Tier-2 all passed; name the gate so
    # the detail is not just a bare "case_passed=False".
    if verdict_reason:
        parts.append(f"OQ-S77 false-positive gate: {verdict_reason}")
    return "; ".join(parts)
