"""Tests for `autoloop.scoring.aggregate` — the pure per-case majority core.

No LLM, no I/O. Every test is a deterministic function of inlined
per-attempt records, per the §5.7 purity contract: this module is the
fitness-measurement-reliability cardinality computation, and these tests
are wiring/logic evidence (NOT a substitute for the real-LLM §6.1
variance run).
"""

from __future__ import annotations

from autoloop.scoring.aggregate import (
    DEFAULT_STABILITY_THRESHOLDS,
    AttemptRecord,
    CaseAggregate,
    aggregate_case,
    classify_stability,
    majority_bool,
)


def _att(idx: int, passed: bool | None, *, valid: bool = True,
         invalid_reason: str | None = None,
         actual_model: str | None = "deepseek-v4-flash",
         fallback_count: int = 0) -> AttemptRecord:
    return AttemptRecord(
        attempt_index=idx,
        case_passed=passed,
        valid=valid,
        invalid_reason=invalid_reason,
        actual_model=actual_model,
        fallback_count=fallback_count,
    )


# --- majority up / down ---------------------------------------------


def test_majority_pass_unanimous():
    agg = aggregate_case("c1", [_att(0, True), _att(1, True), _att(2, True)])
    assert agg.majority_passed is True
    assert agg.pass_rate == 1.0
    assert agg.valid_attempts == 3
    assert agg.comparable is True
    assert agg.flaky is False
    assert agg.status == "stable_pass"


def test_majority_fail_unanimous():
    agg = aggregate_case("c1", [_att(0, False), _att(1, False), _att(2, False)])
    assert agg.majority_passed is False
    assert agg.pass_rate == 0.0
    assert agg.status == "stable_fail"
    assert agg.flaky is False


def test_majority_up_2_of_3_minority_fail_does_not_flip():
    # minority-fail → flaky → majority verdict PASS → does NOT gate as a fail.
    agg = aggregate_case("c1", [_att(0, True), _att(1, True), _att(2, False)])
    assert agg.majority_passed is True
    assert abs(agg.pass_rate - (2 / 3)) < 1e-9
    assert agg.flaky is True
    assert agg.status == "flaky_pass"


def test_majority_down_2_of_3_majority_fail_gates():
    # majority-fail → majority verdict FAIL → gates.
    agg = aggregate_case("c1", [_att(0, False), _att(1, False), _att(2, True)])
    assert agg.majority_passed is False
    assert abs(agg.pass_rate - (1 / 3)) < 1e-9
    assert agg.flaky is True
    assert agg.status == "flaky_fail"


# --- provider_mixed exclusion ---------------------------------------


def test_provider_mixed_excluded_from_numerator_and_denominator():
    # 3 nominal attempts but one is fallback-served (provider_mixed) →
    # only 2 valid → below min_valid_attempts=3 → non_comparable.
    records = [
        _att(0, True),
        _att(1, True),
        _att(2, False, valid=False, invalid_reason="provider_mixed",
             actual_model="kimi-k2", fallback_count=1),
    ]
    agg = aggregate_case("c1", records)
    assert agg.valid_attempts == 2
    assert agg.total_attempts == 3
    assert agg.comparable is False
    assert agg.majority_passed is None
    assert agg.status == "non_comparable"


def test_provider_mixed_excluded_majority_over_remaining_valid():
    # 4 attempts, one provider_mixed dropped → 3 valid, 2 pass → majority pass.
    records = [
        _att(0, True),
        _att(1, True),
        _att(2, False),
        _att(3, True, valid=False, invalid_reason="provider_mixed",
             actual_model="kimi-k2", fallback_count=1),
    ]
    agg = aggregate_case("c1", records)
    assert agg.valid_attempts == 3
    assert agg.majority_passed is True
    assert abs(agg.pass_rate - (2 / 3)) < 1e-9


# --- infra_error exclusion ------------------------------------------


def test_infra_error_excluded_and_labels_status():
    records = [
        _att(0, True),
        _att(1, None, valid=False, invalid_reason="infra_error"),
        _att(2, None, valid=False, invalid_reason="infra_error"),
    ]
    agg = aggregate_case("c1", records)
    assert agg.valid_attempts == 1
    assert agg.comparable is False
    assert agg.majority_passed is None
    # at least one infra_error attempt → status reflects infra, not generic.
    assert agg.status == "infra_error"


# --- sub-threshold → non_comparable ---------------------------------


def test_sub_threshold_one_of_one_non_comparable():
    agg = aggregate_case("c1", [_att(0, True)])
    assert agg.valid_attempts == 1
    assert agg.majority_passed is None
    assert agg.status == "non_comparable"


def test_sub_threshold_one_of_two_non_comparable():
    # 1 valid + 1 provider_mixed → 1 valid < 3 → non_comparable (no infra).
    agg = aggregate_case("c1", [
        _att(0, True),
        _att(1, False, valid=False, invalid_reason="provider_mixed"),
    ])
    assert agg.valid_attempts == 1
    assert agg.majority_passed is None
    assert agg.status == "non_comparable"


def test_custom_min_valid_attempts_allows_lower_threshold():
    agg = aggregate_case("c1", [_att(0, True)], min_valid_attempts=1)
    assert agg.comparable is True
    assert agg.majority_passed is True
    assert agg.status == "stable_pass"


# --- pass_rate ------------------------------------------------------


def test_pass_rate_over_valid_only():
    # 5 attempts: 3 pass, 1 fail (valid), 1 provider_mixed dropped.
    records = [
        _att(0, True),
        _att(1, True),
        _att(2, True),
        _att(3, False),
        _att(4, True, valid=False, invalid_reason="provider_mixed"),
    ]
    agg = aggregate_case("c1", records)
    assert agg.valid_attempts == 4
    assert agg.pass_rate == 0.75
    assert agg.majority_passed is True  # 3 > 4/2
    assert agg.flaky is True


def test_even_split_resolves_to_fail_no_tie_heuristic():
    # 2/4 valid pass → strict majority requires >2 → False.
    agg = aggregate_case("c1", [_att(0, True), _att(1, True),
                                _att(2, False), _att(3, False)])
    assert agg.valid_attempts == 4
    assert agg.majority_passed is False
    assert agg.pass_rate == 0.5


# --- to_dict shape --------------------------------------------------


def test_to_dict_carries_attempts_and_aggregate_fields():
    agg = aggregate_case("c1", [_att(0, True), _att(1, True), _att(2, False)])
    d = agg.to_dict()
    assert d["majority_passed"] is True
    assert d["valid_attempts"] == 3
    assert d["total_attempts"] == 3
    assert d["comparable"] is True
    assert d["aggregate_status"] == "flaky_pass"
    assert len(d["attempts"]) == 3
    assert d["attempts"][2]["case_passed"] is False
    assert d["attempts"][0]["actual_model"] == "deepseek-v4-flash"


# --- majority_bool helper -------------------------------------------


def test_majority_bool_basic():
    assert majority_bool([True, True, False]) is True
    assert majority_bool([False, False, True]) is False
    assert majority_bool([True, False]) is False  # even split → False
    assert majority_bool([]) is None
    assert majority_bool([True]) is True


# --- S-Auto-17: per-case stability classification -------------------


def test_classify_stability_stable_low_and_high():
    # ≤0.2 or ≥0.8 → stable hard anchor (boundaries inclusive).
    assert classify_stability(0.0) == "stable"
    assert classify_stability(0.2) == "stable"
    assert classify_stability(0.8) == "stable"
    assert classify_stability(1.0) == "stable"


def test_classify_stability_near_coinflip_band():
    # 0.4..0.6 inclusive → near-coinflip (an eval_spec/semantic candidate).
    assert classify_stability(0.4) == "near-coinflip"
    assert classify_stability(0.5) == "near-coinflip"
    assert classify_stability(0.6) == "near-coinflip"


def test_classify_stability_reducible_flaky_between_bands():
    # a clear lean that still jitters → reducible-flaky.
    assert classify_stability(0.3) == "reducible-flaky"
    assert classify_stability(0.7) == "reducible-flaky"


def test_classify_stability_none_is_non_comparable():
    assert classify_stability(None) == "non_comparable"


def test_classify_stability_custom_thresholds_override():
    # Tighten the coinflip band so 0.3 falls inside it.
    thr = {"coinflip_low": 0.25, "coinflip_high": 0.75, "stable_low": 0.1, "stable_high": 0.9}
    assert classify_stability(0.3, thr) == "near-coinflip"
    assert classify_stability(0.05, thr) == "stable"
    assert classify_stability(0.85, thr) == "reducible-flaky"


def test_classify_stability_defaults_constant_shape():
    for k in ("stable_low", "stable_high", "coinflip_low", "coinflip_high"):
        assert k in DEFAULT_STABILITY_THRESHOLDS
