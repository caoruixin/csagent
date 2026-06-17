"""Beta-Binomial posterior + TOST + BH-FDR primitives for the noise-aware
fitness gate (S-Y1.7, M-Auto-7).

This module is the statistical core the tier_evaluator's noise-aware Layer 1 /
Layer 2 / Layer 4 consume. It replaces the old zero-tolerance majority-flip /
max-drop count rules with a stability-tiered Beta-Binomial test that stops the
gate false-discarding behaviour-neutral candidates on n=3-5 sampling noise.

Purity contract (load-bearing, mirrors `aggregate.py`): NO I/O, NO network, NO
file reads, NO subprocess, NO clock/random. Deterministic function of its
inputs, so it is trivially unit-testable without an LLM (§5.7) and the gate's
posterior input stays auditable. NO numpy/scipy (not in the autoloop env): Beta
math is `math.lgamma` + a Numerical-Recipes regularized incomplete beta
`betai`, inverse by bisection.

The math is ported VERBATIM from the validated reference implementation
`docs/solutions/p07-calibration/calibrate.py` (the P0.7 retrospective
calibration). The tier_evaluator MUST agree with that reference on the archived
candidate replay (the §Scope #9 oracle); keeping the kernel identical is how
that agreement is guaranteed.
"""

from __future__ import annotations

import math

# --- Default knobs (evidence-backed; not certifiable at n=5 — F5 power
# ceiling). The tier_evaluator sources the live values from config.yaml
# `fitness.tier_decision`; these defaults keep the module self-contained and
# match the calibrate.py reference. ---------------------------------------
DELTA = 0.10          # TOST equivalence margin
P_REGRESS = 0.80      # per-case "regressed" / "improved" posterior threshold
P_AMBIG_LOW = 0.50    # ambiguous band lower bound
ALPHA_FDR = 0.10      # Benjamini-Hochberg false-discovery rate
COUNT_THRESH = 2      # cross-case discard count (>= this many regressed)

_Q = 2000             # stratified quantile midpoints for E_{p_base}[...]


# --- Beta math (dependency-free) ------------------------------------------


def _betacf(a: float, b: float, x: float) -> float:
    MAXIT, EPS, FPMIN = 300, 3e-14, 1e-300
    qab, qap, qam = a + b, a + 1.0, a - 1.0
    c = 1.0
    d = 1.0 - qab * x / qap
    if abs(d) < FPMIN:
        d = FPMIN
    d = 1.0 / d
    h = d
    for m in range(1, MAXIT + 1):
        m2 = 2 * m
        aa = m * (b - m) * x / ((qam + m2) * (a + m2))
        d = 1.0 + aa * d
        if abs(d) < FPMIN:
            d = FPMIN
        c = 1.0 + aa / c
        if abs(c) < FPMIN:
            c = FPMIN
        d = 1.0 / d
        h *= d * c
        aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
        d = 1.0 + aa * d
        if abs(d) < FPMIN:
            d = FPMIN
        c = 1.0 + aa / c
        if abs(c) < FPMIN:
            c = FPMIN
        d = 1.0 / d
        de = d * c
        h *= de
        if abs(de - 1.0) < EPS:
            break
    return h


def betai(a: float, b: float, x: float) -> float:
    """Regularized incomplete beta I_x(a,b) = Beta(a,b) CDF at x."""
    if x <= 0.0:
        return 0.0
    if x >= 1.0:
        return 1.0
    lbeta = math.lgamma(a + b) - math.lgamma(a) - math.lgamma(b)
    bt = math.exp(lbeta + a * math.log(x) + b * math.log(1.0 - x))
    if x < (a + 1.0) / (a + b + 2.0):
        return bt * _betacf(a, b, x) / a
    return 1.0 - bt * _betacf(b, a, 1.0 - x) / b


def betainv(a: float, b: float, p: float, lo: float = 0.0, hi: float = 1.0) -> float:
    """Inverse Beta CDF via bisection (betai monotone in x)."""
    for _ in range(80):
        mid = 0.5 * (lo + hi)
        if betai(a, b, mid) < p:
            lo = mid
        else:
            hi = mid
    return 0.5 * (lo + hi)


_QCACHE: dict[tuple[int, int], list[float]] = {}


def _base_quantiles(k_b: int, n_b: int) -> list[float]:
    """Stratified quantile midpoints of the baseline Jeffreys posterior.

    Cached on (k_b, n_b): the baseline case is fixed across all candidate
    experiments, so its quantiles are reused. This is a pure memo (no state
    leaks across distinct inputs), so the module's purity contract holds.
    """
    key = (k_b, n_b)
    qs = _QCACHE.get(key)
    if qs is None:
        ab, bb = 0.5 + k_b, 0.5 + (n_b - k_b)
        qs = [betainv(ab, bb, (j + 0.5) / _Q) for j in range(_Q)]
        _QCACHE[key] = qs
    return qs


def posterior_regress_improve(
    k_b: int, n_b: int, k_c: int, n_c: int, delta: float = DELTA
) -> tuple[float, float]:
    """P_regress = P(p_c < p_b - delta); P_improve = P(p_c > p_b + delta).

    Both posteriors are Jeffreys Beta(0.5 + k, 0.5 + n - k). The expectation
    over the baseline posterior is taken by stratifying over its quantile
    midpoints (handles the Jeffreys endpoint singularity exactly). Base
    quantiles depend only on (k_b, n_b) -> cached.
    """
    ac, bc = 0.5 + k_c, 0.5 + (n_c - k_c)
    qs = _base_quantiles(k_b, n_b)
    pr = sum(betai(ac, bc, q - delta) for q in qs) / _Q
    pi = sum(1.0 - betai(ac, bc, q + delta) for q in qs) / _Q
    return pr, pi


def bh_flag(
    pregress_by_case: dict[str, float],
    *,
    alpha: float = ALPHA_FDR,
    p_regress: float = P_REGRESS,
) -> list[str]:
    """Benjamini-Hochberg FDR over the per-case regression posteriors.

    Treats `1 - P_regress` as a quasi-p value (ascending), applies the BH
    step-up at `alpha`, then JOINS the rejection set with the per-case
    `P_regress >= p_regress` floor so a BH rejection that is not itself a
    strong per-case signal does not gate. Returns the case keys flagged as
    genuine multi-case regression.
    """
    items = list(pregress_by_case.items())
    m = len(items)
    if m == 0:
        return []
    pv = sorted(((cid, 1.0 - pr) for cid, pr in items), key=lambda t: t[1])
    kmax = 0
    for i, (_cid, q) in enumerate(pv, start=1):
        if q <= (i / m) * alpha:
            kmax = i
    rejected = [pv[i][0] for i in range(kmax)]
    return [cid for cid in rejected if pregress_by_case[cid] >= p_regress]
