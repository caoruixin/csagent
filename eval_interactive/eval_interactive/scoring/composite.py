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
    # Sprint 105 (item 2). False when this case has NO usable gating L3
    # signal — either no gating dim was configured, or every gating dim
    # fell back to ``_DEFAULT_SCORE``. In that state ``judge_score`` is
    # ``0.0`` as an *absent* value, not as a measured zero, and the
    # composite is renormalised onto the outcome term alone. Consumers
    # that average or trend ``judge_score`` MUST filter on this flag;
    # treating an unmeasured 0.0 as a measured 0.0 is exactly the Loop C
    # defect this field exists to make visible.
    judge_measured: bool = True
    # Which L3 population produced ``judge_score``:
    #   "gating"            — one or more critical-severity dims (normal)
    #   "advisory_fallback" — no gating dim; advisory dims used instead
    #   "none"              — no usable L3 signal; judge_score is absent,
    #                         not zero, and the composite renormalises
    judge_basis: str = "gating"
    # Number of gating + advisory L3 dims whose score came from the
    # fallback constant rather than the judge LLM (Sprint 105 item 3).
    l3_fallback_calls: int = 0
    l3_total_calls: int = 0


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
    - ``outcome_score = mean(gating L2 scores)`` if any, else 0.0
    - ``judge_score = mean(measured gating L3 scores) / 5.0`` if any, else
      0.0 with ``judge_measured=False`` (Sprint 105 item 2: absence of a
      signal is no longer scored as failure of it). A gating dim whose
      score came from the fallback constant is not a measurement and is
      excluded from the mean (Sprint 105 item 3).
    - ``composite``:
        * ``0.0`` when ``case_passed`` is False;
        * ``0.5 * outcome + 0.5 * judge`` when L3 is measured;
        * ``outcome`` when L3 is unmeasured but gating L2 exists
          (renormalised onto the component that carries signal);
        * ``0.0`` when neither layer carries signal.
      The judge term is the only one renormalised away — a missing
      *outcome* term never is, so a pass always rests on L2 evidence.
    - A case is "successful" if ``case_passed AND composite >= 0.7`` (the
      0.7 threshold lives in the executor / summary layer, not here).
      Sprint 105 did not move this threshold.

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
    #
    # Sprint 105 (item 3): a dim whose score came from ``_DEFAULT_SCORE``
    # because the judge call failed or its response would not parse is
    # NOT a measurement. Averaging it in lets the whole L3 layer decay to
    # the constant 3.0 (judge_score 0.6) while still looking scored —
    # observed live on 2026-07-25 when the judge model rejected an
    # explicit ``temperature`` and every dimension 400'd twice. Fallbacks
    # are therefore dropped from the mean; if that empties the gating set,
    # L3 is *unmeasured* rather than zero (see below).
    gating_l3 = [r for r in l3_results if getattr(r, "severity", "critical") != "advisory"]
    advisory_l3 = [r for r in l3_results if getattr(r, "severity", "critical") == "advisory"]
    measured_l3 = [r for r in gating_l3 if not getattr(r, "fallback_reason", "")]
    measured_advisory_l3 = [
        r for r in advisory_l3 if not getattr(r, "fallback_reason", "")
    ]
    l3_fallback_calls = sum(
        1 for r in l3_results if getattr(r, "fallback_reason", "")
    )

    # -- Loop C (Sprint 105 item 2) --
    # The old rule was ``judge_score = 0.0`` whenever there were no gating
    # L3 results, and ``composite = 0.5 * outcome + 0.5 * judge``
    # unconditionally. That scores the *absence* of a signal as the
    # *failure* of it: a spec whose only configured L3 dims are advisory
    # (all `promotion/` specs, and all 19 `bad_cases`, which configure
    # none at all) could never exceed ``composite = 0.5`` against a pass
    # bar of 0.7 — no matter how well the bot behaved. A ruler that
    # returns FAIL for every input has no discriminating power; per
    # Constitution §1.6 a constant is not evidence.
    #
    # The fix does NOT lower the 0.7 bar and does NOT promote any dim to
    # gating — both would be ruler-widening under §5.4. Instead it stops
    # discarding the judge signal that actually exists, and only
    # renormalises when there is genuinely none:
    #
    #   gating L3 measured                  -> 0.5*outcome + 0.5*judge(gating)
    #   no gating L3, advisory L3 measured  -> 0.5*outcome + 0.5*judge(advisory)
    #   no L3 signal at all, gating L2      -> outcome  (renormalised)
    #   neither layer carries signal        -> 0.0, no-evidence gate fires
    #
    # **Advisory dims are a fallback signal, never a dilutant.** S-Eval-5
    # demoted `groundedness` / `relevance` / `tone_appropriateness` /
    # `user_goal_achievement` out of the composite so they could not skew
    # a mean that already had gating dims in it. That intent is preserved
    # exactly: whenever a gating dim is measured, advisory dims are
    # excluded as before. S-Eval-5 simply did not contemplate the case
    # where the advisory dims are the *only* judge signal — which is every
    # `promotion/` spec — and there, discarding them is strictly less
    # discriminating than using them. Concretely, on the one recorded
    # session whose verdict this sprint moves, the advisory judge caught a
    # real turn-3 citation defect (`groundedness=2.0`); renormalising it
    # away would have scored that session a flat 1.0.
    #
    # Renormalisation, when it does apply, is deliberately one-directional:
    # it never applies to a missing *outcome* term. Passing on a judge
    # score with no L2 evidence would be a pass resting on nothing about
    # whether the bot did the right thing, which OQ-S77 #3 refuses.
    if measured_l3:
        judge_basis = "gating"
        judge_score = (sum(r.score for r in measured_l3) / len(measured_l3)) / 5.0
    elif measured_advisory_l3:
        judge_basis = "advisory_fallback"
        judge_score = (
            sum(r.score for r in measured_advisory_l3) / len(measured_advisory_l3)
        ) / 5.0
    else:
        judge_basis = "none"
        judge_score = 0.0
    judge_measured = judge_basis != "none"

    # -- Composite --
    if not case_passed:
        composite = 0.0
    elif judge_measured:
        composite = 0.5 * outcome_score + 0.5 * judge_score
    elif gating_l2:
        composite = outcome_score
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
    #
    # Sprint 105 note on the interaction with judge renormalisation: both
    # gates below key on ``composite == 0.0``, and renormalisation cannot
    # open a hole under them. With no gating L2 the outcome term is 0.0
    # and the judge term is halved, so the ceiling is 0.5 when L3 is
    # measured and 0.0 when it is not — neither can reach the 0.7 bar.
    # A pass therefore still requires gating L2 evidence, which is what
    # OQ-S77 #3 exists to enforce.
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

    # Sprint 105 (items 2 + 3): make the two "no L3 signal" states legible
    # in the serialised tags rather than leaving them to be inferred from
    # a 0.0 that looks like a measured zero.
    #   L3_UNMEASURED:no_gating_dims_configured — the CaseSpec configured
    #     no gating L3 dim (or only advisory ones). Expected on
    #     `promotion/` specs and on all 19 `bad_cases`; the composite is
    #     renormalised onto the outcome term.
    #   L3_UNMEASURED:all_gating_dims_fell_back — gating dims WERE
    #     configured but every one of them returned the fallback constant.
    #     This is a judge-infrastructure failure, not a bot result, and a
    #     run carrying it must not be quoted as evidence.
    if judge_basis == "advisory_fallback":
        failure_tags.append("L3_BASIS:advisory_fallback")
    elif not judge_measured:
        if gating_l3:
            failure_tags.append("L3_UNMEASURED:all_gating_dims_fell_back")
        elif advisory_l3:
            failure_tags.append("L3_UNMEASURED:all_advisory_dims_fell_back")
        else:
            failure_tags.append("L3_UNMEASURED:no_dims_configured")
    for r in l3_results:
        reason = getattr(r, "fallback_reason", "")
        if reason:
            failure_tags.append(f"L3_FALLBACK:{r.dimension}:{reason}")

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
        judge_measured=judge_measured,
        judge_basis=judge_basis,
        l3_fallback_calls=l3_fallback_calls,
        l3_total_calls=len(l3_results),
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
