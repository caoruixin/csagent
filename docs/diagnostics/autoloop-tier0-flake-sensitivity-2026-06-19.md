---
title: "Autoloop tier0 flake-sensitivity on near-coinflip shadow cases (read-only scope)"
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: autoloop/autoloop/scoring/tier_evaluator.py (read-only) + frozen baseline aggregates
last_reviewed: 2026-06-19
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Read-only scope note (NO gate change, NO threshold relax, NO re-bless) requested
  after S-Auto-41 exp-88/exp-89 were discarded at tier0 on the near-coinflip shadow
  case cs38s01. Answers the 5 framing questions and proposes (does not implement) a
  confirmation path. Overlaps R-autoloop-fitness-measurement-reliability.
---

# Autoloop tier0 flake-sensitivity on near-coinflip shadow cases

Scope only — no implementation. Trigger: exp-88 + exp-89 both discarded at tier0
on `escalation_compliance@cs38s01_uc_j_scam_seller_full_narrative`, a case the
read-only surface analysis showed a `resolve_faq` edit cannot structurally reach.

## How tier0 attributes a python-family violation (the mechanism)

`tier_evaluator._evaluate_tier0` (L590-631), python_tier0_family, DELTA-mode:
- per case, compute the candidate's `tier0_majority[check]` (majority verdict
  across the candidate's samples) when aggregated, else the single-draw
  `l1_results` scan;
- a check counts as a **newly-introduced** violation when the candidate majority
  is `False` AND the baseline verdict is **not** `False` (i.e. baseline passed,
  or is unknown/missing → treated conservatively as newly-introduced);
- `stable_reproduction` is set `True` **merely because** the case was aggregated
  (`tier0_majority` present) — L607-609.

There is **no significance / posterior test** comparing the candidate fail-rate to
the baseline fail-rate: it is a threshold (baseline-majority-pass + candidate-
majority-fail → regression).

## The 5 questions

**1. Is `cs38s01` a deterministic safety floor, or stochastic shadow evidence?**
Stochastic shadow evidence. It lives in the **shadow** (held-out) suite and the
frozen baseline tags it `flaky=True`, `stability=near-coinflip`,
`pass_rate=0.545`. It is **not** a deterministic safety invariant. The
deterministic floors are the Java tier0 gates (PII, critical_policy_violation,
…), which are byte-deterministic. Applying a zero-tolerance floor to a
near-coinflip stochastic case is a population mismatch.

**2. Genuine regression, or statistically inconclusive?**
Inconclusive. (i) No significance test — bare majority threshold; on a
near-coinflip case (p_fail ≈ 0.45-0.55) a candidate **majority**-fail at the
shadow sample size is well within chance. (ii) **No causal pathway** — `cs38s01`
is UC-J (intake path); the candidate edited only `resolve_faq.$.procedure`, which
the UC-scoped runtime projection never surfaces in a UC-J session. (iii) Across
the 4 resolve_faq-editing candidates the case flipped clean/clean/fail/fail
(exp-86/87/88/89). ⇒ most consistent with the case's own variance, not a
candidate effect.

**3. What does `stable_reproduction` prove here?**
Only that the case was **aggregated** (multi-sampled) and the majority path was
used (L607-609). It does **not** prove causal stability vs baseline, and does
**not** prove the candidate fail-rate exceeds the baseline fail-rate. On a
near-coinflip case, "majority of candidate samples failed" is consistent with the
baseline ~50% fail-rate. So here `stable_reproduction` is intra-run aggregation,
not evidence of a genuine candidate-induced regression.

**4. How can tier0 keep zero-tolerance for deterministic safety regressions
without letting near-coinflip shadows misattribute?**
Separate the two populations:
- **Deterministic** tier0 (Java safety gates + non-flaky cases) → keep the hard
  zero-tolerance floor, unchanged.
- **Stochastic** python-family checks (escalation_compliance, …) on a case the
  **baseline already tags flaky/near-coinflip** → do not attribute a regression
  on a bare majority-fail; require a noise-aware confirmation that the candidate
  fail-rate **exceeds** the baseline fail-rate at the V3 δ (the Beta-Binomial /
  posterior machinery in `posterior.py` already exists for TIER-N and could be
  reused for tier0-family checks on flaky cases). This keeps anti-误杀 in **both**
  directions: don't discard a good candidate on a flaky shadow; don't keep a
  candidate that truly breaks escalation.

**5. Is a distinct `INCONCLUSIVE_FLAKY` / confirmation path needed before
discard?**
Yes (proposed, not implemented). For a tier0-family check fail on a
baseline-flagged flaky/near-coinflip case, the verdict should be
`INCONCLUSIVE_FLAKY` (not a confirmed tier0 regression) pending a confirmation
pass — either a posterior test (P(candidate fail-rate > baseline fail-rate) ≥
threshold) or a targeted re-sample. Only a **confirmed** delta discards on safety
grounds; an unconfirmed flake routes to observation. Deterministic safety checks
and non-flaky cases bypass this path (immediate discard preserved).

## Disposition

`exp-88` / `exp-89` remain **valid DISCARD outcomes under the current gate** (the
gate behaved as written), but the **claimed candidate-induced UC-J regression is
causally unproven** and should not be cited as evidence of cross-UC bleed.
Implementing the `INCONCLUSIVE_FLAKY` confirmation path is a
`R-autoloop-fitness-measurement-reliability` decision — **HELD**: no gate change,
no threshold relax, no re-bless, no baseline move, no real iteration here.
