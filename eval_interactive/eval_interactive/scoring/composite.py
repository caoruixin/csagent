"""Composite scorer -- aggregates L1/L2/L3 scores into a final result."""

from __future__ import annotations

from dataclasses import dataclass, field

from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.stall_detector import StallResult


@dataclass
class CompositeScore:
    """Aggregated score for a single evaluation case."""

    case_id: str
    case_passed: bool  # All L1 hard checks passed
    l1_results: list[HardCheckResult]
    l2_results: list[OutcomeCheckResult]
    l3_results: list[JudgeResult]
    outcome_score: float  # mean(L2 scores)
    judge_score: float  # mean(L3 scores) / 5.0 -> 0..1
    composite: float  # 0.5 * outcome + 0.5 * judge (or 0.0 if not case_passed)
    stall_result: StallResult
    failure_tags: list[str] = field(default_factory=list)


def compute_composite(
    case_id: str,
    l1_results: list[HardCheckResult],
    l2_results: list[OutcomeCheckResult],
    l3_results: list[JudgeResult],
    stall_result: StallResult,
) -> CompositeScore:
    """Aggregate all scoring layers into a single CompositeScore.

    Rules:
    - case_passed = all L1 hard checks passed
    - outcome_score = mean(L2 scores) if L2 else 0.0
    - judge_score = (mean(L3 scores) / 5.0) if L3 else 0.0
    - composite = 0.0 if not case_passed else 0.5 * outcome_score + 0.5 * judge_score
    - A case is "successful" if case_passed AND composite >= 0.7

    Failure tags are collected from all layers.
    """
    # -- L1 --
    case_passed = all(r.passed for r in l1_results) if l1_results else True

    # -- L2 --
    if l2_results:
        outcome_score = sum(r.score for r in l2_results) / len(l2_results)
    else:
        outcome_score = 0.0

    # -- L3 --
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

    for r in l2_results:
        if r.score < 0.5:
            failure_tags.append(f"L2:{r.check_name}")

    for r in l3_results:
        if r.score < 3.0:
            failure_tags.append(f"L3:{r.dimension}")

    if stall_result.detected:
        tag = stall_result.failure_tag or "STALL"
        failure_tags.append(f"STALL:{tag}")

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
    )
