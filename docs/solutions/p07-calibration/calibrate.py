#!/usr/bin/env python3
"""P0.7 retrospective calibration — ZERO-LLM-cost recomputation of the
14 autoloop verdicts (exp-66..79) under the proposed P0 fitness-measurement
rule, against the n=9 (effective n=11) pre-pilot baseline.

Source-of-truth inputs (read-only; nothing is mutated):
  - baseline per-case:  eval_interactive/results/m-auto-7-prepilot-baseline-20260608/{suite}/aggregated.json
  - candidate per-case: autoloop/results/runs/exp-{N}/eval/{suite}/results.json
  - historical verdict: autoloop/results/experiments.jsonl (iteration_id == exp-N)

NO external deps (no numpy/scipy in the autoloop env). Beta math via math.lgamma
+ Numerical-Recipes regularized incomplete beta `betai`, inverse by bisection.

The proposed rule (S-Y2 analysis §5 P0):
  Floors (binding, override the statistic; lexicographic, first match wins):
    F0  tier0 delta floor   — UNCHANGED current Layer-0 logic: a TIER0_PY_FAMILY
                              check False in candidate-majority but True/unknown in
                              baseline for the same (suite,case) -> DISCARD.
    FS  TIER-S floor (P0.1) — any baseline-1.0 case with a candidate valid-attempt
                              fail -> DISCARD (anti-误杀 binding floor).
    F4  shadow Layer-4      — UNCHANGED: shadow aggregate pass-rate drop > 3.0pp -> DISCARD.
  Statistic on TIER-N cases (bad_cases + anchor_outcome = the 29-case gating set):
    Beta-Binomial Jeffreys posterior Beta(0.5+k, 0.5+n-k); TOST margin delta=0.10.
    P_regress = P(p_cand < p_base - delta);  P_improve = P(p_cand > p_base + delta).
    per-case:  P_regress>=0.80 -> regressed ; 0.50<=P_regress<0.80 -> ambiguous ; else non-regressed.
    cross-case discard if EITHER: BH-FDR(alpha=0.10) flags >=1 regressed case,
                                  OR >=2 cases with P_regress>=0.80.
  Verdict: DISCARD(floor) | DISCARD(regressed) | KEEP-eligible(>=1 improved) |
           AMBIGUOUS(>=1 ambiguous, Simon stage-2) | NON-REGRESSED(neutral hold).
"""
from __future__ import annotations
import json, math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / "eval_interactive/results/m-auto-7-prepilot-baseline-20260608"
RUNS = ROOT / "autoloop/results/runs"
EXPJL = ROOT / "autoloop/results/experiments.jsonl"

GATING_SUITES = ["bad_cases", "anchor_outcome"]   # the 29-case BH set
ALL_SUITES = ["bad_cases", "anchor_outcome", "shadow"]
EXPS = [66, 67, 68, 69, 71, 72, 73, 74, 75, 77, 78, 79]  # 70/76 discarded pre-eval (no eval data)

DELTA = 0.10
P_REGRESS = 0.80
P_AMBIG_LOW = 0.50
ALPHA_FDR = 0.10
COUNT_THRESH = 2
SHADOW_MAX_DROP_PP = 3.0
TIER0_PY_FAMILY = ("no_pii_leakage", "no_human_only_tool_exposure",
                   "no_critical_policy_violation", "escalation_compliance",
                   "phase_transition_validity")

# ---------- Beta math (dependency-free) ----------
def _betacf(a, b, x):
    MAXIT, EPS, FPMIN = 300, 3e-14, 1e-300
    qab, qap, qam = a + b, a + 1.0, a - 1.0
    c = 1.0
    d = 1.0 - qab * x / qap
    if abs(d) < FPMIN: d = FPMIN
    d = 1.0 / d
    h = d
    for m in range(1, MAXIT + 1):
        m2 = 2 * m
        aa = m * (b - m) * x / ((qam + m2) * (a + m2))
        d = 1.0 + aa * d
        if abs(d) < FPMIN: d = FPMIN
        c = 1.0 + aa / c
        if abs(c) < FPMIN: c = FPMIN
        d = 1.0 / d
        h *= d * c
        aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
        d = 1.0 + aa * d
        if abs(d) < FPMIN: d = FPMIN
        c = 1.0 + aa / c
        if abs(c) < FPMIN: c = FPMIN
        d = 1.0 / d
        de = d * c
        h *= de
        if abs(de - 1.0) < EPS: break
    return h

def betai(a, b, x):
    """Regularized incomplete beta I_x(a,b) = Beta(a,b) CDF at x."""
    if x <= 0.0: return 0.0
    if x >= 1.0: return 1.0
    lbeta = math.lgamma(a + b) - math.lgamma(a) - math.lgamma(b)
    bt = math.exp(lbeta + a * math.log(x) + b * math.log(1.0 - x))
    if x < (a + 1.0) / (a + b + 2.0):
        return bt * _betacf(a, b, x) / a
    return 1.0 - bt * _betacf(b, a, 1.0 - x) / b

def betainv(a, b, p, lo=0.0, hi=1.0):
    """Inverse Beta CDF via bisection (betai monotone in x)."""
    for _ in range(80):
        mid = 0.5 * (lo + hi)
        if betai(a, b, mid) < p: lo = mid
        else: hi = mid
    return 0.5 * (lo + hi)

_Q = 2000  # stratified quantile midpoints for E_{p_base}[...]
_QCACHE: dict = {}
def _base_quantiles(k_b, n_b):
    key = (k_b, n_b)
    qs = _QCACHE.get(key)
    if qs is None:
        ab, bb = 0.5 + k_b, 0.5 + (n_b - k_b)
        qs = [betainv(ab, bb, (j + 0.5) / _Q) for j in range(_Q)]
        _QCACHE[key] = qs
    return qs

def posterior_regress_improve(k_b, n_b, k_c, n_c, delta=DELTA):
    """P_regress = P(p_c < p_b - d); P_improve = P(p_c > p_b + d).
    Stratified over base posterior quantile midpoints (handles Jeffreys
    endpoint singularity exactly). Base quantiles depend only on (k_b,n_b)
    -> cached, since the baseline case is fixed across all experiments."""
    ac, bc = 0.5 + k_c, 0.5 + (n_c - k_c)
    qs = _base_quantiles(k_b, n_b)
    pr = sum(betai(ac, bc, q - delta) for q in qs) / _Q
    pi = sum(1.0 - betai(ac, bc, q + delta) for q in qs) / _Q
    return pr, pi

# ---------- load ----------
def _tier2_mand(tier2):
    """Majority-collapsed Tier-2 mandatory-failure count for one case."""
    per_step = (tier2 or {}).get("per_step") or []
    return sum(1 for s in per_step
               if s.get("severity") == "mandatory" and s.get("outcome") == "FAIL")

def load_baseline():
    out = {}
    for suite in ALL_SUITES:
        d = json.load(open(BASE / suite / "aggregated.json"))
        cases = {}
        for c in d["case_results"]:
            cid = c["case_id"]
            va, pr = c.get("valid_attempts"), c.get("pass_rate")
            comparable = c.get("comparable")
            k = round(pr * va) if (pr is not None and va) else None
            cases[cid] = dict(case_id=cid, k=k, n=va, pass_rate=pr, comparable=comparable,
                              tier0_majority=c.get("tier0_majority") or {},
                              tier2_mand=_tier2_mand(c.get("tier2_result_majority")),
                              l1_results=c.get("l1_results") or [])
        out[suite] = cases
    return out

def load_candidate(n):
    out = {}
    for suite in ALL_SUITES:
        p = RUNS / f"exp-{n}" / "eval" / suite / "results.json"
        if not p.exists():
            out[suite] = {}; continue
        d = json.load(open(p))
        cases = {}
        for c in d["case_results"]:
            cid = c["case_id"]
            atts = c.get("attempts") or []
            # direct attempt-level counts
            valid = [a for a in atts if a.get("valid") is True]
            k_dir = sum(1 for a in valid if a.get("case_passed") is True)
            n_dir = len(valid)
            # infra-stripped variant (P0.6c sensitivity): drop TIMEOUT / infra attempts
            def is_infra(a):
                tags = a.get("failure_tags") or []
                ir = (a.get("invalid_reason") or "")
                return any(t in ("TIMEOUT", "INVALID_INFRA_ERROR", "infra_error") for t in tags) \
                       or "infra" in ir.lower() or (a.get("failure_reason") == "timeout")
            valid_ns = [a for a in valid if not is_infra(a)]
            k_ns = sum(1 for a in valid_ns if a.get("case_passed") is True)
            n_ns = len(valid_ns)
            cases[cid] = dict(case_id=cid, k=k_dir, n=n_dir, k_ns=k_ns, n_ns=n_ns,
                              pass_rate=c.get("pass_rate"), valid_attempts=c.get("valid_attempts"),
                              comparable=c.get("comparable"), majority_passed=c.get("majority_passed"),
                              tier0_majority=c.get("tier0_majority") or {},
                              tier2_majority=c.get("tier2_result_majority") or {},
                              primary_uc=c.get("primary_uc"))
        out[suite] = cases
    return out

def load_hist():
    out = {}
    for line in open(EXPJL):
        line = line.strip()
        if not line: continue
        r = json.loads(line)
        out[r.get("iteration_id")] = dict(decision=r.get("decision"),
                                          discard_reason=r.get("discard_reason"),
                                          field=(r.get("hypothesis") or {}).get("target_field_path"),
                                          file=((r.get("hypothesis") or {}).get("target_skill_file") or "").split("/")[-1])
    return out

# ---------- tier classification ----------
def classify_tier(b):
    if b["pass_rate"] is None or not b["comparable"]:
        return "EXCL"
    if b["pass_rate"] >= 0.999:
        return "TIER-S"
    if b["pass_rate"] <= 0.001:
        return "TIER-F"
    return "TIER-N"

# ---------- floors ----------
def tier0_floor(cand, base):
    """Replicate current Layer-0 delta logic across ALL suites."""
    # baseline tier0 truth: prefer tier0_majority, else l1_results
    for suite in ALL_SUITES:
        bsuite = base[suite]; csuite = cand.get(suite, {})
        for cid, cc in csuite.items():
            tm = cc.get("tier0_majority") or {}
            violated = [cn for cn, ok in tm.items() if cn in TIER0_PY_FAMILY and ok is False]
            if not violated: continue
            bc = bsuite.get(cid)
            for cn in violated:
                base_ok = None
                if bc:
                    btm = bc.get("tier0_majority") or {}
                    if cn in btm:
                        base_ok = btm[cn] is True
                    else:
                        for chk in bc.get("l1_results") or []:
                            if chk.get("check") == cn:
                                base_ok = chk.get("passed") is True
                # baseline False -> pre-existing, ignore. else (True/unknown) -> new violation.
                if base_ok is False:
                    continue
                return f"tier0_{cn}_failed_on_{cid}"
    return None

def tier_s_floor(cand, base, tiers, use_ns=False, mode="any_fail"):
    """mode='any_fail' (literal P0.1) | 'majority_flip' (noise-aware correction)."""
    hits = []
    for suite in ALL_SUITES:
        for cid, b in base[suite].items():
            if tiers[(suite, cid)] != "TIER-S": continue
            cc = cand.get(suite, {}).get(cid)
            if not cc: continue
            k = cc["k_ns"] if use_ns else cc["k"]
            n = cc["n_ns"] if use_ns else cc["n"]
            if not n: continue
            if mode == "any_fail":
                if k < n: hits.append(f"{cid}({k}/{n})")
            else:  # majority_flip: candidate majority is fail
                if (k / n) <= 0.5: hits.append(f"{cid}({k}/{n})")
    return hits

def shadow_layer4(cand, base):
    bs = base["shadow"]; cs = cand.get("shadow", {})
    # aggregate pass-rate over comparable cases (majority verdict)
    def rate(cases, ismaj):
        tot = 0; pas = 0
        for cid, c in cases.items():
            if ismaj:
                if c.get("pass_rate") is None or not c.get("comparable"): continue
                tot += 1; pas += 1 if c.get("majority_passed") else 0
            else:
                if c["pass_rate"] is None or not c["comparable"]: continue
                tot += 1; pas += 1 if (c["pass_rate"] > 0.5) else 0
        return (pas / tot * 100.0) if tot else None, tot, pas
    br, bt, bp = rate(bs, False)
    cr, ct, cp = rate(cs, True)
    if br is None or cr is None: return None, (br, cr)
    drop = br - cr
    return (drop if drop > SHADOW_MAX_DROP_PP else None), (round(br, 1), round(cr, 1), round(drop, 1))

# ---------- BH-FDR ----------
def bh_flag(pregress_by_case, alpha=ALPHA_FDR):
    items = [(cid, pr) for cid, pr in pregress_by_case.items()]
    m = len(items)
    if m == 0: return []
    # quasi-p = 1 - P_regress ; BH ascending
    pv = sorted(((cid, 1.0 - pr, pr) for cid, pr in items), key=lambda t: t[1])
    kmax = 0
    for i, (cid, q, pr) in enumerate(pv, start=1):
        if q <= (i / m) * alpha:
            kmax = i
    rejected = [pv[i][0] for i in range(kmax)]
    # joint with per-case P_regress>=0.80
    return [cid for cid in rejected if pregress_by_case[cid] >= P_REGRESS]

def shadow_posterior(cand, base):
    """C2 noise-aware shadow: Beta-Binomial posterior on the shadow MAJORITY
    pass count over comparable cases. Returns (P_regress, info). Replaces the
    raw 3pp count gate — discard only on a statistically-supported drop."""
    bs, cs = base["shadow"], cand.get("shadow", {})
    n_b = sum(1 for b in bs.values() if b["comparable"] and b["pass_rate"] is not None)
    k_b = sum(1 for b in bs.values() if b["comparable"] and (b["pass_rate"] or 0.0) > 0.5)
    n_c = sum(1 for c in cs.values() if c.get("comparable") and c.get("majority_passed") is not None)
    k_c = sum(1 for c in cs.values() if c.get("comparable") and c.get("majority_passed") is True)
    if not n_b or not n_c:
        return None, (None, None, None)
    pr, _pi = posterior_regress_improve(k_b, n_b, k_c, n_c)
    info = (round(k_b / n_b * 100, 1), round(k_c / n_c * 100, 1), round((k_b/n_b - k_c/n_c)*100, 1))
    return pr, info


# ---------- main per-exp evaluation ----------
def evaluate_exp(n, base, tiers, use_ns=False, tier_s_mode="any_fail",
                 shadow_floor=True, c1=False, c2_noise=False):
    """c1=True   -> noise-aware Layer-2 tier2 per-case (S-Y1.7 extension):
                    a mandatory-failure INCREASE is credited only if the case's
                    OUTCOME posterior is a strong regression; the SAME count>=2
                    cross-case rule then applies (union with outcome-regressed).
                    A knife-edge increase (0.50<=P_regress<0.80) -> AMBIGUOUS.
       c2_noise=True -> noise-aware Layer-4 shadow posterior (replaces the 3pp
                    count gate)."""
    cand = load_candidate(n)
    res = dict(exp=n, floors=[], regressed=[], ambiguous=[], improved=[], nonreg=[],
               per_case={}, shadow=None, tier2_strong=[], tier2_ambiguous=[])
    # F0 tier0
    t0 = tier0_floor(cand, base)
    if t0: res["floors"].append(("F0_tier0", t0))
    # FS tier-S
    ts = tier_s_floor(cand, base, tiers, use_ns=use_ns, mode=tier_s_mode)
    if ts: res["floors"].append(("FS_tier_s", ",".join(ts)))
    # F4 shadow — legacy 3pp count gate OR (c2_noise) noise-aware posterior.
    if c2_noise:
        spr, sdinfo = shadow_posterior(cand, base)
        res["shadow"] = sdinfo
        if spr is not None and spr >= P_REGRESS:
            res["floors"].append(("F4_shadow", f"P_regress_{spr:.2f}"))
    else:
        sd, sdinfo = shadow_layer4(cand, base)
        res["shadow"] = sdinfo
        if shadow_floor and sd is not None: res["floors"].append(("F4_shadow", f"drop_{sd:.1f}pp"))
    # statistic on TIER-N (gating suites). With c1 (S-Y1.7 extension) TIER-F
    # cases are also scored — "improvement-direction only": they CANNOT regress
    # (P_regress vs a negative margin ~0) but CAN be credited as improved (the
    # autoloop's job is fixing hard-0 cases; the S-Y2 pilot moves a TIER-F
    # primary target). TIER-S is handled by the FS floor and excluded here.
    eligible = ("TIER-N", "TIER-F") if c1 else ("TIER-N",)
    pregress = {}
    for suite in GATING_SUITES:
        for cid, b in base[suite].items():
            tier = tiers[(suite, cid)]
            if tier not in eligible: continue
            cc = cand.get(suite, {}).get(cid)
            if not cc: continue
            if cc.get("comparable") is False: continue
            k_c = cc["k_ns"] if use_ns else cc["k"]
            n_c = cc["n_ns"] if use_ns else cc["n"]
            if not n_c: continue
            pr, pi = posterior_regress_improve(b["k"], b["n"], k_c, n_c)
            key = f"{suite}:{cid}"
            res["per_case"][key] = dict(tier=tier, k_b=b["k"], n_b=b["n"], k_c=k_c, n_c=n_c,
                                        p_base=round(b["pass_rate"], 3),
                                        p_cand=round(k_c / n_c, 3),
                                        P_regress=round(pr, 3), P_improve=round(pi, 3))
            if tier == "TIER-N":
                pregress[key] = pr
                if pr >= P_REGRESS: res["regressed"].append(key)
                elif pr >= P_AMBIG_LOW: res["ambiguous"].append(key)
                else: res["nonreg"].append(key)
            if pi >= P_REGRESS: res["improved"].append(key)
    res["bh_flagged"] = bh_flag(pregress)
    # C1 — noise-aware Layer-2 tier2 per-case (gating suites).
    if c1:
        for suite in GATING_SUITES:
            for cid, cc in cand.get(suite, {}).items():
                cand_t2 = _tier2_mand(cc.get("tier2_majority"))
                b = base[suite].get(cid)
                base_t2 = b["tier2_mand"] if b else 0
                if cand_t2 <= base_t2: continue
                key = f"{suite}:{cid}"
                pr = None
                if b and b["k"] is not None and b["n"]:
                    k_c = cc["k_ns"] if use_ns else cc["k"]
                    n_c = cc["n_ns"] if use_ns else cc["n"]
                    if n_c: pr, _pi = posterior_regress_improve(b["k"], b["n"], k_c, n_c)
                if pr is not None and pr >= P_REGRESS: res["tier2_strong"].append(key)
                elif pr is not None and pr >= P_AMBIG_LOW: res["tier2_ambiguous"].append(key)
    # union of outcome-regressed and tier2-strong for the count>=2 rule.
    regressed_union = set(res["regressed"]) | set(res["tier2_strong"])
    # verdict
    if res["floors"]:
        res["verdict"] = "DISCARD(floor)"
        res["verdict_reason"] = "; ".join(f"{a}:{b}" for a, b in res["floors"])
    elif res["bh_flagged"] or len(regressed_union) >= COUNT_THRESH:
        res["verdict"] = "DISCARD(regressed)"
        res["verdict_reason"] = f"BH={res['bh_flagged']} count_regressed_union={len(regressed_union)}"
    elif res["tier2_ambiguous"]:
        res["verdict"] = "AMBIGUOUS"
        res["verdict_reason"] = f"tier2_ambiguous={res['tier2_ambiguous']} (C1 knife-edge, Simon stage-2)"
    elif res["improved"]:
        res["verdict"] = "KEEP-eligible"
        res["verdict_reason"] = f"improved={res['improved']}"
    elif res["ambiguous"]:
        res["verdict"] = "AMBIGUOUS"
        res["verdict_reason"] = f"ambiguous={res['ambiguous']} (Simon stage-2)"
    else:
        res["verdict"] = "NON-REGRESSED"
        res["verdict_reason"] = "neutral hold (no regression, no improvement)"
    return res

def main():
    # self-test betai
    assert abs(betai(2, 3, 0.5) - 0.6875) < 1e-9, betai(2, 3, 0.5)
    assert abs(betai(0.5, 0.5, 0.5) - 0.5) < 1e-9
    base = load_baseline()
    hist = load_hist()
    tiers = {}
    for suite in ALL_SUITES:
        for cid, b in base[suite].items():
            tiers[(suite, cid)] = classify_tier(b)
    # tier summary
    from collections import Counter
    tc = Counter(tiers[(s, cid)] for s in GATING_SUITES for cid in base[s])
    print("== TIER classification over 29 gating cases (bad_cases+anchor):", dict(tc))
    print("   TIER-S:", [cid for s in GATING_SUITES for cid in base[s] if tiers[(s, cid)] == "TIER-S"])
    print("   TIER-F:", [cid for s in GATING_SUITES for cid in base[s] if tiers[(s, cid)] == "TIER-F"])
    print()
    results = {}
    VARIANTS = [
        ("V1_LITERAL_P0.1 (any-fail TIER-S, shadow floor on)", dict(use_ns=False, tier_s_mode="any_fail", shadow_floor=True)),
        ("V2_SENSITIVITY (infra/TIMEOUT stripped, any-fail TIER-S)", dict(use_ns=True, tier_s_mode="any_fail", shadow_floor=True)),
        ("V3_CORRECTED (majority-flip TIER-S, shadow floor on)", dict(use_ns=False, tier_s_mode="majority_flip", shadow_floor=True)),
        ("V4_CORRECTED_NO_SHADOW_COUNT (majority-flip TIER-S, shadow floor off)", dict(use_ns=False, tier_s_mode="majority_flip", shadow_floor=False)),
        ("V5_EXTENDED (S-Y1.7 SHIPPED: majority-flip TIER-S + C1 tier2 + C2 shadow noise-aware)",
         dict(use_ns=False, tier_s_mode="majority_flip", shadow_floor=False, c1=True, c2_noise=True)),
    ]
    for variant, kw in VARIANTS:
        print(f"\n########## VARIANT: {variant} ##########")
        hdr = f"{'exp':>4} {'fam/field':<24} {'historical discard':<46} {'PROPOSED verdict':<19} reason"
        print(hdr); print("-" * len(hdr))
        vres = {}
        for n in EXPS:
            r = evaluate_exp(n, base, tiers, **kw)
            h = hist.get(f"exp-{n}", {})
            fam = h.get("field", "?")
            print(f"{n:>4} {fam:<24} {str(h.get('discard_reason'))[:46]:<46} {r['verdict']:<19} {r['verdict_reason'][:78]}")
            vres[n] = r
        results[variant] = vres
    # dump full detail
    out = {}
    for variant, vres in results.items():
        out[variant] = {n: vres[n] for n in vres}
    (Path(__file__).parent / "calibration-results.json").write_text(json.dumps(out, indent=1))
    print("\nFull per-case detail -> calibration-results.json")
    return results

if __name__ == "__main__":
    main()
