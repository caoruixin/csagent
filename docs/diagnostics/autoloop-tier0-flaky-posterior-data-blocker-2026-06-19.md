---
title: "Blocker: tier0 INCONCLUSIVE_FLAKY posterior needs per-check (k,n) that is not persisted"
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: eval_runner._tier0_check_majority + persisted results.json/aggregated.json (read-only)
last_reviewed: 2026-06-19
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  S-Auto-42 first-pass implementation of the INCONCLUSIVE_FLAKY tier0 confirmation
  path was built and reverted after a zero-LLM historical replay exposed a
  fundamental data-availability blocker that caused improper KEEP-widening. No
  code shipped; gate/baseline/canonical-pointer unchanged. HELD for direction.
---

# Blocker: tier0 flaky posterior needs per-check (k,n) that is not persisted

## What was built (and reverted)
A safe-core INCONCLUSIVE_FLAKY path in `tier_evaluator._evaluate_layer0`:
deterministic/non-flaky violations unchanged; baseline-flagged flaky violations →
`posterior_regress_improve(k_b,n_b,k_c,n_c,δ)` → CONFIRMED (`P_regress≥0.80`,
discard) / CLEARED (`P_regress<0.50`) / INCONCLUSIVE (middle, defer→HOLD, never
KEEP). 5 unit tests passed; the 34-case `test_tier_evaluator.py` golden suite
passed (deterministic + non-flaky unchanged).

## The blocker (found by zero-LLM historical replay)
The posterior needs per-check **(k,n)** for both candidate and baseline. That
data is **not reliably available**:

- The authoritative per-check verdict is `tier0_majority[check]`, a **bool only**
  — computed by `eval_runner._tier0_check_majority` from each attempt's
  per-attempt `_l1_checks` map, which is **NOT persisted** in results.json /
  aggregated.json. So the per-check **counts are unavailable**.
- `failure_tags` (which IS persisted) **undercounts** per-check failures. Proof
  (real archived trace, exp-68 `cs11s01_uc_d_two_emails_one_account`, shadow):
  only **2/5** attempts carry `L1:escalation_compliance`, yet
  `tier0_majority[escalation_compliance] = False` (authoritative majority-fail).
  So ≥1 escalation failure has no failure tag.
- The **frozen baseline** carries neither per-check counts nor per-attempt
  `_l1_checks` — only the aggregated `tier0_majority` bool + the same unreliable
  `failure_tags`. So baseline per-check `(k,n)` cannot be recovered from the
  baseline file at all without re-scoring it.

## Why this matters (improper KEEP-widening)
Using `failure_tags` as the `(k,n)` source **over-estimates** candidate
pass-rate (undercounts failures) → under-estimates regression → improperly
CLEARS. On the zero-LLM oracle replay this flipped **exp-68 discard→KEEP**
(`cs11s01` read as 3/5, `P_regress=0.398` → CLEARED → keep), a direct violation
of the "no improper KEEP widening" requirement and of the pinned V3 oracle
matrix. exp-67/73/74 changed mechanism (still discard) but exp-68 widened KEEP.

## Options (for direction — none implemented)
1. **Persist per-check (k,n) in eval_runner** (`_tier0_check_majority` already
   computes the per-check bool lists; emit counts alongside). Scoring-SHA change
   + re-pin. But the **frozen baseline still lacks the counts** — it would need a
   re-score to carry them, which conflicts with "no re-bless / no baseline move".
2. **Re-score the baseline offline** to add per-check counts (zero-LLM if the
   per-attempt per-check verdicts were persisted — they are NOT, so this needs a
   re-run = re-bless). Conflicts with the holds.
3. **Different signal**: the bool-only `tier0_majority` cannot drive a
   Beta-Binomial posterior (no counts). A coarser flaky-aware rule (e.g.
   consult the baseline `stability_class`/`flaky` to DEFER-not-discard a flaky
   tier0 violation pending bounded confirmation, without a per-check posterior)
   could be a measurement-honest interim — but it changes the spec (no posterior
   confirmation) and still needs the bounded-confirmation step to resolve HOLDs.
4. **Hold the whole reliability fix** until the eval pipeline persists per-check
   counts on BOTH sides (candidate + a re-blessed baseline), then implement the
   posterior path as specified.

## Disposition
No code shipped (reverted; tree clean, oracle + tier_evaluator green). exp-90
proposal frozen. No gate/baseline/re-bless/canonical-pointer change, no real
iteration. HELD for human direction on the (k,n) data-availability decision.
