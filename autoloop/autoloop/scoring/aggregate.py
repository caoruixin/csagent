"""Pure per-case majority aggregation over repeated, provider-comparable
samples (S-Auto-16, M-Auto-4).

This module is the fitness-measurement-reliability core: given the
per-attempt pass/fail records for ONE case (the k-of-n repeated draws
the eval_runner n-loop produces), it returns a single per-case verdict
that is the MAJORITY vote over the *valid* attempts only. A single noisy
draw can no longer flip the case verdict.

Purity contract (load-bearing): this module performs NO I/O, NO network,
NO file reads, NO subprocess, NO clock/random. It is a deterministic
function of its inputs. The eval_runner owns extracting per-attempt
records from results.json (including the provider-comparability `valid`
flag); this module owns only the cardinality computation. Keeping it pure
makes it trivially unit-testable without an LLM (§5.7) and keeps the
fitness verdict's majority input auditable.

Validity / comparability rules (primary-only fitness policy, §0):
  - An attempt served by the fallback provider (provider_mixed) or one
    that never produced a comparable result (infra_error) is INVALID:
    excluded from BOTH the numerator and the denominator of the vote.
  - `min_valid_attempts` (default 3) is the floor for a gating verdict.
    Below it the case is `non_comparable` / `infra_error` and does NOT
    gate — it is neither an improvement nor a stable regression. There
    is NO tie heuristic: the vote is a strict majority (`passes >
    valid/2`), so an even split resolves to fail.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

MIN_VALID_ATTEMPTS_DEFAULT = 3

# Per-attempt invalid reasons (set by the eval_runner from provider metadata).
INVALID_PROVIDER_MIXED = "provider_mixed"
INVALID_INFRA_ERROR = "infra_error"


# --- S-Auto-17: per-case stability classification --------------------
#
# A cardinality policy over the MAJORITY pass_rate (NOT over case content):
# how reproducible is a case's pass/fail under repeated sampling. The
# classification is the input to the re-bless stability report and the
# overnight go/no-go gate (OQ-S72.2): near-coinflip cases (p≈0.5) get ~0
# variance reduction from a k-of-n majority, so they are surfaced as an
# eval_spec / semantic-ambiguity signal rather than force-stabilized with a
# larger n. Thresholds are a tunable config knob
# (`fitness.stability_thresholds`), defaulted here.
STABILITY_STABLE = "stable"
STABILITY_REDUCIBLE_FLAKY = "reducible-flaky"
STABILITY_NEAR_COINFLIP = "near-coinflip"
STABILITY_NON_COMPARABLE = "non_comparable"

DEFAULT_STABILITY_THRESHOLDS: dict[str, float] = {
    # stable if pass_rate <= stable_low OR >= stable_high (a hard anchor).
    "stable_low": 0.2,
    "stable_high": 0.8,
    # near-coinflip if coinflip_low <= pass_rate <= coinflip_high (≈0.5;
    # an eval_spec / semantic-ambiguity candidate, NOT an n=5 candidate).
    "coinflip_low": 0.4,
    "coinflip_high": 0.6,
}


def classify_stability(
    pass_rate: float | None,
    thresholds: dict[str, float] | None = None,
) -> str:
    """Classify a case's reproducibility from its majority pass_rate.

    Returns one of `stable` / `reducible-flaky` / `near-coinflip` /
    `non_comparable`. A `None` pass_rate (non-comparable case) yields
    `non_comparable`. This is a pure threshold on a rate (a cardinality
    policy); it never reads case content, so it introduces no semantic
    hardcode (§1.7-clean).

    Default bands (tunable via `thresholds`):
      - `stable`         pass_rate <= 0.2 or >= 0.8
      - `near-coinflip`  0.4 <= pass_rate <= 0.6
      - `reducible-flaky` everything else (a clear lean that still jitters)
    """
    if pass_rate is None:
        return STABILITY_NON_COMPARABLE
    t = {**DEFAULT_STABILITY_THRESHOLDS, **(thresholds or {})}
    if pass_rate <= t["stable_low"] or pass_rate >= t["stable_high"]:
        return STABILITY_STABLE
    if t["coinflip_low"] <= pass_rate <= t["coinflip_high"]:
        return STABILITY_NEAR_COINFLIP
    return STABILITY_REDUCIBLE_FLAKY


@dataclass
class AttemptRecord:
    """One repeated sample of a single case.

    `case_passed` is the programmatic per-case pass/fail of this draw.
    `valid` is False when the draw is not provider-comparable
    (`invalid_reason` names why: `provider_mixed` for a fallback-served
    attempt, `infra_error` for a transport/deadline-degraded attempt).
    The §1 provider fields are recorded for audit and are derived from the
    eval results.json `llm_calls[].model` field (see eval_runner); they do
    not influence the cardinality computation here beyond the `valid` flag.
    """

    attempt_index: int
    case_passed: bool | None
    valid: bool = True
    invalid_reason: str | None = None
    failure_reason: str | None = None
    failure_tags: list[Any] = field(default_factory=list)
    escalation_reason: str | None = None
    actual_provider: str | None = None
    actual_model: str | None = None
    fallback_count: int = 0
    request_id: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "attempt_index": self.attempt_index,
            "case_passed": self.case_passed,
            "valid": self.valid,
            "invalid_reason": self.invalid_reason,
            "failure_reason": self.failure_reason,
            "failure_tags": list(self.failure_tags or []),
            "escalation_reason": self.escalation_reason,
            "actual_provider": self.actual_provider,
            "actual_model": self.actual_model,
            "fallback_count": self.fallback_count,
            "request_id": self.request_id,
        }


@dataclass
class CaseAggregate:
    """The majority verdict for one case over its repeated attempts.

    `majority_passed` is the single gating signal the tier_evaluator
    consumes. It is None when the case is non-comparable (insufficient
    valid attempts) — a None verdict NEVER gates (neither improvement nor
    regression).
    """

    case_id: str
    majority_passed: bool | None
    pass_rate: float | None
    valid_attempts: int
    total_attempts: int
    comparable: bool
    flaky: bool
    status: str  # stable_pass|stable_fail|flaky_pass|flaky_fail|non_comparable|infra_error
    attempts: list[AttemptRecord] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "majority_passed": self.majority_passed,
            "pass_rate": self.pass_rate,
            "valid_attempts": self.valid_attempts,
            "total_attempts": self.total_attempts,
            "comparable": self.comparable,
            "flaky": self.flaky,
            "aggregate_status": self.status,
            "attempts": [a.to_dict() for a in self.attempts],
        }


def majority_bool(values: list[bool]) -> bool | None:
    """Strict-majority vote over a list of booleans.

    Returns None for an empty list. Strict majority means `True` requires
    more than half the votes; an even split resolves to `False` (no tie
    heuristic, mirroring `aggregate_case`). Used by the tier_evaluator to
    collapse a per-Tier-0-check `passed` signal across attempts.
    """
    if not values:
        return None
    trues = sum(1 for v in values if v is True)
    return trues > (len(values) / 2)


def aggregate_case(
    case_id: str,
    records: list[AttemptRecord],
    *,
    min_valid_attempts: int = MIN_VALID_ATTEMPTS_DEFAULT,
) -> CaseAggregate:
    """Collapse one case's repeated attempts into a majority verdict.

    Majority is computed over VALID attempts only (a valid attempt is
    provider-comparable and produced a definite pass/fail). Invalid
    attempts are excluded from numerator AND denominator.

    Insufficient-valid rule: when `valid_attempts < min_valid_attempts`
    the verdict is non-comparable — `majority_passed=None`, which does not
    gate. The status is `infra_error` when at least one attempt was
    infra-degraded (the case never had a fair chance to be measured),
    otherwise `non_comparable`.
    """
    total_attempts = len(records)
    valid_records = [
        r for r in records if r.valid and r.case_passed is not None
    ]
    valid_attempts = len(valid_records)

    if valid_attempts < min_valid_attempts:
        had_infra = any(
            r.invalid_reason == INVALID_INFRA_ERROR for r in records
        )
        return CaseAggregate(
            case_id=case_id,
            majority_passed=None,
            pass_rate=None,
            valid_attempts=valid_attempts,
            total_attempts=total_attempts,
            comparable=False,
            flaky=False,
            status=INVALID_INFRA_ERROR if had_infra else "non_comparable",
            attempts=list(records),
        )

    passes = sum(1 for r in valid_records if r.case_passed is True)
    pass_rate = passes / valid_attempts
    majority_passed = passes > (valid_attempts / 2)
    flaky = 0 < passes < valid_attempts  # not unanimous across valid draws

    if flaky:
        status = "flaky_pass" if majority_passed else "flaky_fail"
    else:
        status = "stable_pass" if majority_passed else "stable_fail"

    return CaseAggregate(
        case_id=case_id,
        majority_passed=majority_passed,
        pass_rate=pass_rate,
        valid_attempts=valid_attempts,
        total_attempts=total_attempts,
        comparable=True,
        flaky=flaky,
        status=status,
        attempts=list(records),
    )
