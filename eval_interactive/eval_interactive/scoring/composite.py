"""Composite scorer -- aggregates L1/L2/L3 scores into a final result."""

from __future__ import annotations

from dataclasses import dataclass, field

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
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


def _conditional_mandatory_l2(case_spec: CaseSpec) -> tuple[str, ...]:
    """Return the conditionally-mandatory L2 check names for this case.

    Note: ``escalation_compliance`` is *intentionally not* listed here.
    It is an L1 hard check (see ``hard_checks.py``) that runs globally on
    every case, so the L1 gate already enforces it. Listing it at L2
    would (a) duplicate logic and (b) fail-close the composite as
    ``L2_GATE_MISSING`` whenever the L2 list does not also configure it
    -- which masked otherwise-passing escalate cases (HIGH-5).
    """
    expected = case_spec.expected
    conditional: list[str] = []
    if expected.outcome_class == "escalate":
        conditional.append("handover_completeness")
    return tuple(conditional)


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
    """
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
    detail: str = ""


def compute_composite(
    case_id: str,
    l1_results: list[HardCheckResult],
    l2_results: list[OutcomeCheckResult],
    l3_results: list[JudgeResult],
    stall_result: StallResult,
    case_spec: CaseSpec | None = None,
) -> CompositeScore:
    """Aggregate all scoring layers into a single CompositeScore.

    Rules (Wave B1.1, refined HIGH-5):
    - ``case_passed = all(l1) AND all(mandatory_l2)``
    - Mandatory L2 always: ``correct_uc``, ``correct_outcome``
    - Mandatory L2 if ``outcome_class == 'escalate'``: ``handover_completeness``
    - ``escalation_compliance`` is an L1 hard check (global) -- not an
      L2 gate. The L1 gate already covers it.
    - Missing mandatory check is fail-closed (treated as a failure).
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
    l1_passed = all(r.passed for r in l1_results) if l1_results else True

    # -- Mandatory L2 gate (Wave B1.1) --
    mandatory_l2_passed = True
    mandatory_l2_failures: list[str] = []

    if case_spec is not None:
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

    case_passed = l1_passed and mandatory_l2_passed

    # -- L2 mean --
    if l2_results:
        outcome_score = sum(r.score for r in l2_results) / len(l2_results)
    else:
        outcome_score = 0.0

    # -- L3 mean (normalized to 0..1) --
    if l3_results:
        judge_score = (sum(r.score for r in l3_results) / len(l3_results)) / 5.0
    else:
        judge_score = 0.0

    # -- Composite --
    if case_passed:
        composite = 0.5 * outcome_score + 0.5 * judge_score
    else:
        composite = 0.0

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

    for r in l3_results:
        if r.score < 3.0:
            failure_tags.append(f"L3:{r.dimension}")

    if stall_result.detected:
        tag = stall_result.failure_tag or "STALL"
        failure_tags.append(f"STALL:{tag}")

    # -- Detail / explanation --
    detail = _build_detail(
        case_passed=case_passed,
        l1_passed=l1_passed,
        mandatory_l2_passed=mandatory_l2_passed,
        l1_failures=[r.check_name for r in l1_results if not r.passed],
        mandatory_l2_failures=mandatory_l2_failures,
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
        detail=detail,
    )


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _outcome_passed(r: OutcomeCheckResult) -> bool:
    """An outcome check "passes" the gate iff its score is 1.0.

    Mandatory L2 gates are pass/fail in spirit even though the underlying
    OutcomeCheckResult carries a 0..1 score. We require a perfect 1.0 so
    that partial-credit (e.g. handover completeness with one missing field)
    still trips the gate -- this matches the audit's intent: outcome
    failures must not be masked by graded L3 scores.
    """
    return r.score >= 1.0


def _build_detail(
    *,
    case_passed: bool,
    l1_passed: bool,
    mandatory_l2_passed: bool,
    l1_failures: list[str],
    mandatory_l2_failures: list[str],
) -> str:
    """Human-readable explanation of the gate outcome."""
    if case_passed:
        return "case_passed=True (all L1 and all mandatory L2 gates passed)"

    parts: list[str] = ["case_passed=False"]
    if not l1_passed:
        parts.append(f"L1 failed: {l1_failures}")
    if not mandatory_l2_passed:
        parts.append(f"mandatory L2 failed/missing: {mandatory_l2_failures}")
    return "; ".join(parts)
