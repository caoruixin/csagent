"""Tests for `autoloop.scoring.posterior` — the noise-aware fitness gate's
Beta-Binomial / TOST / BH-FDR kernel (S-Y1.7).

The `betai` checks pin the regularized incomplete beta to KNOWN closed-form
values (no LLM, no fixtures). The posterior + BH checks pin the qualitative
behaviour the gate relies on. The module is ported verbatim from
`docs/solutions/p07-calibration/calibrate.py`; these tests are the
independent guard that the port did not drift from the reference.
"""

from __future__ import annotations

import math

from autoloop.scoring.posterior import (
    betai,
    betainv,
    bh_flag,
    posterior_regress_improve,
)


# --- betai vs known closed-form values -------------------------------


def test_betai_known_polynomial_value():
    # I_0.5(2,3) = 0.6875 (calibrate.py self-test anchor).
    assert abs(betai(2, 3, 0.5) - 0.6875) < 1e-9


def test_betai_symmetric_jeffreys_midpoint():
    # I_0.5(0.5,0.5) = 0.5 by symmetry of the arcsine distribution.
    assert abs(betai(0.5, 0.5, 0.5) - 0.5) < 1e-9


def test_betai_uniform_is_identity():
    # I_x(1,1) = x  (Beta(1,1) is Uniform[0,1]).
    for x in (0.1, 0.25, 0.5, 0.73, 0.9):
        assert abs(betai(1, 1, x) - x) < 1e-9


def test_betai_endpoints_clamp():
    assert betai(2, 5, 0.0) == 0.0
    assert betai(2, 5, 1.0) == 1.0
    assert betai(2, 5, -0.3) == 0.0
    assert betai(2, 5, 1.3) == 1.0


def test_betai_complement_identity():
    # I_x(a,b) = 1 - I_{1-x}(b,a).
    a, b, x = 2.5, 4.0, 0.37
    assert abs(betai(a, b, x) - (1.0 - betai(b, a, 1.0 - x))) < 1e-9


def test_betainv_roundtrips_betai():
    a, b = 3.5, 2.5
    for p in (0.05, 0.3, 0.5, 0.8, 0.95):
        x = betainv(a, b, p)
        assert abs(betai(a, b, x) - p) < 1e-6


# --- posterior_regress_improve ---------------------------------------


def test_posterior_identical_counts_symmetric():
    # Same (k,n) on both sides -> P_regress == P_improve by symmetry.
    pr, pi = posterior_regress_improve(4, 8, 4, 8)
    assert abs(pr - pi) < 1e-3


def test_posterior_clear_regression_high_pregress():
    # Baseline strong-pass (10/11) vs candidate all-fail (0/5) -> P_regress high.
    pr, pi = posterior_regress_improve(10, 11, 0, 5)
    assert pr > 0.95
    assert pi < 0.01


def test_posterior_clear_improvement_high_pimprove():
    # Baseline all-fail (0/11) vs candidate strong-pass (5/5) -> P_improve high.
    pr, pi = posterior_regress_improve(0, 11, 5, 5)
    assert pi > 0.95
    assert pr < 0.01


def test_posterior_probabilities_in_unit_interval():
    for kb, nb, kc, nc in [(8, 11, 2, 5), (8, 11, 4, 5), (3, 5, 1, 3)]:
        pr, pi = posterior_regress_improve(kb, nb, kc, nc)
        assert 0.0 <= pr <= 1.0
        assert 0.0 <= pi <= 1.0


def test_posterior_reproduces_calibrate_knife_edge():
    """The exp-66 C1 knife-edge: baseline wmkb 8/11 (0.727) vs candidate 2/5.
    The reference pins P_regress = 0.797 — just under the 0.80 discard
    threshold (the whole reason exp-66 is AMBIGUOUS, not DISCARD).
    """
    pr, _pi = posterior_regress_improve(8, 11, 2, 5)
    assert abs(pr - 0.797) < 0.01
    assert pr < 0.80


# --- bh_flag ----------------------------------------------------------


def test_bh_flag_empty():
    assert bh_flag({}) == []


def test_bh_flag_no_strong_case_returns_empty():
    # All weak P_regress -> nothing flagged even if BH would reject.
    assert bh_flag({"a": 0.2, "b": 0.3, "c": 0.1}) == []


def test_bh_flag_joins_with_per_case_floor():
    # A case below the per-case P_regress floor is never flagged even if its
    # quasi-p is the smallest.
    flagged = bh_flag({"weak": 0.79, "strong": 0.99}, alpha=0.10, p_regress=0.80)
    assert "weak" not in flagged


def test_bh_flag_strong_unanimous_regression():
    flagged = bh_flag({"a": 0.99, "b": 0.98, "c": 0.97}, alpha=0.10)
    assert set(flagged) == {"a", "b", "c"}
