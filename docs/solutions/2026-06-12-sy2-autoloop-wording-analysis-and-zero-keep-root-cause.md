---
title: S-Y2 autoloop candidate-wording inventory + zero-keep root cause + optimization plan
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: autoloop/results/experiments.jsonl + autoloop/results/runs/exp-{66..79}/ + eval_interactive/results/m-auto-7-prepilot-baseline-20260608/
last_reviewed: 2026-06-15
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Analysis-only deliverable requested by the human after S-Y2 Part C closed
  0-keep (see docs/sprints/sprint-088-handoff.md). Inventories the LLM-authored
  wording candidates across exp-66..79, root-causes why the loop has produced
  zero keeps over 14 consecutive iterations, and proposes an optimization plan.
  NO code change is made by this doc; every plan item is a proposal requiring
  its own sprint contract + human authorization. Extraction methodology:
  python json parse of experiments.jsonl (hypothesis.before_value/after_value
  difflib), per-case pass_rate from suite results.json case_results, baseline
  rates from the n=9 prepilot re-bless. 2026-06-15 revision: §5 plan
  re-prioritized per human review — P0 is now fitness-measurement reliability
  (with named industry-standard methods: Beta-Binomial Bayesian posterior,
  Wilson CI, TOST equivalence margin, Benjamini-Hochberg FDR, Simon two-stage
  design); TIMEOUT investigation (formerly P0) deferred as
  network-attributable and tracked as a §5.6 carried observation.
---

# S-Y2 autoloop wording inventory + zero-keep root cause (2026-06-12)

## 0. TL;DR

Across 14 iterations (exp-66..79, two tranches) the autoloop produced
**high-quality, on-principle wording candidates** — the proposer is *not* the
bottleneck. Zero keeps is over-determined by three stacked causes, in
descending weight:

1. **Statistical: the fitness gate's zero-tolerance majority-flip rule under
   n=3 sampling is a ~90–95% false-discard machine.** Most baseline-passing
   cases sit at true pass rates 0.64–0.91 (flaky zone); a behaviour-neutral
   candidate survives tier-1 alone with probability ≈ **11%**, and all gates
   with ≈ 5–8%. Expected keeps in 14 trials ≤ 1 even if every candidate were
   perfect. The gate is currently selecting on noise, not on quality
   (signal-to-noise ≈ 1:1). This is the PAUSED **M-Auto-4** hypothesis, now
   directly evidenced.
2. **Infra: TIMEOUT contamination inside candidate runs degrades exactly the
   cases the pilot needs to measure.** exp-77's run had 5/17 bad_cases
   all-attempts-TIMEOUT including BOTH primaries; exp-78's `no_ad_id` had 2/5
   TIMEOUT + 1 trace_minimum attempts.
3. **Semantic: the remaining true gap on `no_ad_id` is second-order.** The
   wording DID elicit the closure-criterion behaviour (exp-78 attempt-0:
   asks for ad ref → `get_customer_context` → listing-anchored answer, tool
   sequence 5/5); the case still fails because the session tail ends in
   escalate (`user_requested` / `faq_miss`) instead of `resolve` under
   simulator pressure. A one-paragraph edit fixes the head of the behaviour,
   not the tail.

## 1. Wording inventory — three families

Full before→after diffs + rationales extracted to this analysis from
`experiments.jsonl` `hypothesis` fields (exp-66/69/70/71/72/75/76/77/78/79).

### Family A — RESOLVE-side entity-context anchoring
`resolve_faq_grounded_answer.yaml` (exp-70 `$.procedure`, exp-76
`$.procedure`, exp-71 `$.critical_steps[1].desc`, exp-72
`$.grounding_instruction`, exp-77 `$.critical_steps[0].desc`, exp-78
`$.grounding_instruction`)

Recurring moves (converged across 6 independent proposals):

- **Listing-context anchoring**: when projection carries `listing_context`,
  treat it as the case anchor; the FAQ article complements (not substitutes)
  the listing state. (exp-70/71/77/78)
- **One focused clarifying ask**: ad-specific question + no ad reference →
  ask once for the listing reference before generic answer / unanchored
  search / early `faq_miss` handover. (exp-70/76/71/72/77/78 — all six)
- **Graceful lookup-failure fallback**: failed lookup is a legitimate resolve
  outcome — state honestly what the system shows, pair with the policy
  surface. (exp-70/71/72)
- **Loaded framing does not change the route**: anchor on the primary subject;
  emotional framing alone is not grounds for ungrounded reassurance or
  premature handover. (exp-72/76)

Quality: directly matches the CaseSpec closure criteria; eval evidence shows
the intended behaviour actually occurring (§3). Measured effect:
`loaded_listing` 0.09→0.20 under both exp-77/78; Tier-2 neighbor
`lookup_failed` 0.09→**0.60 (majority flip to PASS)** under both.

### Family B — DISCOVER-side routing (understand-vs-act)
`discover_triage.yaml` (exp-69 `$.grounding_instruction`, exp-75
`$.grounding_instruction`, exp-79 `$.critical_steps[2].desc`)

Recurring moves: identifiers are supporting evidence, **not the routing
key**; intake commitment requires an explicit ACT signal (appeal / report /
dispute / delete); a FAQ-shaped UNDERSTAND question stays FAQ-path even when
topic-adjacent to an intake surface; absence of an ad_id never promotes a
question into intake.

Quality: on-principle and addresses the `alice_uc_a_uc_h_misclass` /
`no_ad_id` misroute half. Risk profile is worse: DISCOVER text is shared by
all UCs — exp-69 coincided with the run's worst anchor drop (8→5), exp-79
with `uc_d_login` 8→7 (plausibly noise, see §2).

### Family C — escalation_policy distress/stuck-loop additions (stale-lessons artifact)
`$.escalation_policy` on various skills (exp-66/67/68/73; exp-74 adjacent)

Driven by the May-31 pre-CS4 lessons (L-004/L-005, "escalation boundary is
fragile → escalate more readily"). Off-pilot-target, and the only family
that consistently tripped **tier0 escalation_compliance** (encouraging
handover on cases that require containment). Correctly neutralized by the
S-Y1.5 `lessons.enabled: false` opt-out — Families A/B dominate the
post-S-Y1.5 tranche; no escalation_policy proposal appeared in run-2/3.

**Observation (§1.7-adjacent, for milestone-shared Codex):** all six Family-A
proposals converge on phrasing very close to the CaseSpec closure-criterion
language ("ask one focused clarifying question"). The analyzer's input is the
failure landscape (transcripts + failure tags), not CaseSpec text, and the
phrase is natural CS practice — but the milestone-shared review should
spot-check that no CaseSpec `closure_criterion` text reaches the proposer
prompt (same red line as the pilot-block case-id rule).

## 2. Root cause #1 — the fitness gate is a false-discard machine under n=3

### 2.1 Baseline truth: most "passing" cases are flaky

Per-case `pass_rate` from the n=9 prepilot baseline
(`m-auto-7-prepilot-baseline-20260608`, suite `results.json` `case_results`):

- 8 baseline-passing anchors: 0.64, 0.73, 0.73, 0.89, 0.90, 0.70, 0.64, 1.00
  — **only `uc_k_tech` is stable at 1.0**.
- 8 baseline-passing bad_cases: 0.82, 0.91, 1.00, 0.82, 1.00, 0.91, 0.64,
  0.73 — only `cs066` and the anti-误杀 control are stable at 1.0.

### 2.2 The arithmetic

Candidate runs use n=3 attempts/case, majority pass = ≥2/3, and the
lexicographic gate discards on **any single** majority drop
(`max_drop_cases=0`). For true pass rate `p`,
`P(majority-fail) = (1-p)³ + 3p(1-p)²`:

| true p | P(3-attempt majority flips to fail) |
|---|---|
| 0.64 | **30.1%** |
| 0.70 | 21.6% |
| 0.73 | 18.3% |
| 0.82 | 8.7% |
| 0.89–0.91 | 2.3–3.4% |
| 1.00 | 0% |

For a **behaviour-neutral** candidate:

- P(no anchor majority drop) = Π over the 8 passing anchors ≈ **0.24**
- P(no bad_case majority drop) ≈ **0.45**
- P(tier-1 survives) ≈ 0.24 × 0.45 ≈ **11%**
- After tier0 escalation_compliance flakes (cs11s01-class, observed flipping
  between "failing" and "pre-existing-ignored" across runs), tier2
  critical-flow aggregate, and shadow: overall survival ≈ **5–8%**.

Expected keeps in 14 iterations, even if **every candidate were perfectly
neutral-or-better**: ≤ 1. Observed: 0. The 14/14 discard record requires no
quality explanation at all.

### 2.3 The discard pattern confirms noise, not harm

Every tier-1/tier-2 discard regressed a **different** case, each sitting in
the flaky zone: `uc_a_visibility` (0.64) at exp-77, `uc_d_login` (0.73) at
exp-79, `cs11s01` escalation_compliance at exp-67/68/73/74, tier2 aggregate
3→4 at exp-66/72, UC-A 0→1 at exp-75, anchors 8→6/8→5 at exp-71/69.
Consistent harm would re-hit the same cases.

Two genuine catches stand out as NOT noise — proving the gate's *shape* is
right even though its *statistics* are wrong:

- exp-78: anti-误杀 control 1.00→0.33. True rate 1.0 cannot produce ≤1/3 by
  sampling; this was a real over-elicitation regression, correctly killed.
- Family C: tier0 escalation_compliance kills on escalate-more wording were
  directionally correct.

### 2.4 Sharpest formulation

A *neutral* candidate that survives the kill gates has ≈67% chance of showing
a lucky "improvement" (some failing flaky case up-flips) → false-keep ≈ 3%.
A *genuinely good* candidate (true `loaded_listing` lift to 0.2–0.3) has
keep ≈ 0.5–2%. **The gate cannot currently distinguish a good candidate from
a neutral one** — selection is dominated by which flaky case happens to flip
this run. This is precisely the PAUSED M-Auto-4 milestone scope ("Autoloop
Fitness Measurement Reliability"), now with direct quantitative evidence.

## 3. Root cause #2 — infra TIMEOUT contamination inside candidate runs

- exp-77 bad_cases: **5/17 cases all-attempts-TIMEOUT** (`alice_uc_a`,
  `cs012`, `cs_uc_a_loaded_listing`, `cs_uc_a_lookup_failed`,
  `cs_uc_a_no_ad_id_ad_specific`) — both primaries + one neighbor
  unmeasurable in the very iteration meant to measure them.
- exp-78 `no_ad_id`: attempts = [outcome-fail, outcome-fail, trace_minimum,
  TIMEOUT, TIMEOUT] — 3/5 attempts invalid or infra-tagged.

TIMEOUT-shrunken attempt sets make majorities higher-variance still, and the
loss concentrates on the pilot's target cases (long multi-turn sessions).
This is an `infra`-layer eval-framework defect per §3.2-Q1; **§5.8
framework-defect priority applies**: it should be root-caused before more
multi-hour real-LLM tranches are burned.

## 4. Root cause #3 — `no_ad_id`'s true residual gap is second-order

CaseSpec closure: turn-1 must NOT search_knowledge before verifying the ad
reference; outcome must be `resolve`.

exp-78 attempt-0 transcript (candidate wording live): bot asks for the ad
reference → user supplies AD-1001 → bot calls `get_customer_context` →
listing-anchored answer ("live and active, check filters/timing...") → tool
sequence LCS 5/5. **The wording's intended behaviour happened.** The case
still failed `L2:correct_outcome actual=escalate not in ['resolve']`: the
simulated user kept pressing ("checked filters, still can't find it"), the
bot's follow-ups stayed generic (spam folder, keyword tips), and the session
ended `user_requested` (attempt-0) / `faq_miss_threshold_exceeded`
(attempt-1).

Decomposition of the remaining gap:

- **semantic_planner (bot-side, real)**: after anchoring, the bot's
  explanation is filler rather than the product-truthful close (indexing
  delay window, what "live" means for search, citation, then
  `record_outcome` resolved). A candidate that fixes the head but not this
  tail can never flip the case.
- **eval_spec / judge_calibration (open question, human review)**: does the
  "cooperative, mildly puzzled" persona ever accept a correct answer, or
  does it press until escalation regardless? If the latter, closure (a) is
  unreachable by any bot behaviour and the CaseSpec needs §5.4-conscious
  review (fix-the-CaseSpec path, NOT an override to mask a bot mistake —
  the bot's filler follow-ups are independently improvable).

`loaded_listing` (0.09→0.20 under Family A) responds to the same head-fix and
likely shares the tail problem at lower intensity.

## 5. Optimization plan (proposal only — NO code changed by this doc)

### 5.0 Prioritization (revised 2026-06-15 per human review)

The 14/14 discard pattern is over-determined; the human review on 2026-06-15
classified TIMEOUT contamination as network-attributable and deferrable. The
remaining measurement defect (§2 root cause #1, dominant on weight) is the
binding obstacle: with the current zero-tolerance majority-flip rule under
n=3 sampling, a **behaviour-neutral** candidate has ≈5–8% all-gate survival.
The gate cannot currently distinguish a good candidate from a noisy one. So:

- **P0 — Fitness measurement reliability** (was P1; promoted, fully
  rewritten with named statistical methods + concrete parameter sheet)
- **P1 — §3.4 hand-authored fallback content** (was P2; unchanged scope)
- **P2 — Loop search mechanics** (was P3; unchanged scope)
- **P3 — Eval-spec review question** (was P4; unchanged scope)
- **Deferred — TIMEOUT root-cause brief** (was P0; tracked as a §5.6
  carried observation per human direction)

Per §5.8, measurement fixes precede further multi-hour real-LLM semantic
runs. P1 can proceed in parallel because its gate is the human §4.1 review,
not the autoloop fitness gate.

### P0 — Fitness measurement reliability (lead priority; = resume PAUSED M-Auto-4 scope)

#### P0.0 — Problem statement (one paragraph)

Eval pass/fail per attempt is binary; the LLM is stochastic; "true" per-case
pass rate p is estimated from n attempts. The current gate
(`anchor_outcome_max_drop_cases: 0`, `aggregation.method: majority`,
`samples_per_case: 3`) treats this estimate as noiseless. From the n=9
prepilot baseline, most "passing" cases have true p between 0.64 and 0.91;
P(majority flip from such a case under n=3 sampling alone) is **18–30%**.
With 8 baseline-passing anchors + 8 bad_cases tested simultaneously, the
neutral-candidate false-discard rate is ~92–95%. The proposal below replaces
the count rule with named, well-understood methods from clinical-trial /
A/B-testing / stochastic-eval practice, while preserving the §5.4 anti-误杀
invariant (no eval-side override of a real bug) by retaining
zero-tolerance on a *stability-tiered* subset of cases.

Useful prior art already in `autoloop/config.yaml`: the
`stability_thresholds` block + `aggregate.classify_stability` are wired into
the baseline re-bless pipeline (S-Auto-17 / OQ-S72.2); the gate itself does
not yet consult those classifications. P0 is largely *wiring + parameter*
work, not from-scratch construction.

#### P0.1 — Per-case stability tiers (binds the anti-误杀 floor)

Classify each baseline case from its n=9 observation:

| tier | criterion | decision rule on candidate |
|---|---|---|
| **TIER-S (Stable)** | baseline 9/9 attempts pass (Wilson 95% lower ≈ 0.70 — weak in isolation, conservatively augmented by by-design anchors below) | **Any attempt failure → discard.** Binding §5.4 anti-误杀 / safety floor. |
| **TIER-N (Noisy)** | baseline 1/9 to 8/9 pass (the bulk: 14/17 bad_cases + 9/12 anchors are here) | Bayesian posterior with equivalence margin δ (P0.2). |
| **TIER-F (Failing)** | baseline 0/9 pass | Improvement direction only; cannot regress below floor; cannot gate discard. |

TIER-S by design includes: the anti-误杀 control
(`cs_uc_a_generic_policy_question`), `uc_k_tech`, `cs066`, the entire
`tier0_safety` family. The "9/9 observed" rule is an operational floor
intentionally chosen to be conservative: cases that were genuinely 1.0 will
satisfy it; cases borderline at p=0.9 may sometimes drop to TIER-N at
re-bless and that is acceptable.

This subsumes the prior P1's "regression re-test (sequential confirmation)"
idea — the structure now is "test with the right statistic, then optionally
re-test under Simon's two-stage if borderline" (P0.5).

#### P0.2 — Beta-Binomial Bayesian decision on TIER-N cases (with TOST equivalence margin δ)

For each TIER-N case, model pass-rate as Beta-distributed with Jeffreys
prior `Beta(0.5, 0.5)`:

- Baseline posterior: `Beta(0.5 + k_base, 0.5 + 9 - k_base)`
- Candidate posterior: `Beta(0.5 + k_cand, 0.5 + n_cand - k_cand)`

Compute by numerical integration:

```
P_regress = P(p_cand < p_base - δ)         # one-sided TOST lower
P_improve = P(p_cand > p_base + δ)         # one-sided TOST upper
P_equiv   = 1 - P_regress - P_improve      # within ±δ equivalence band
```

Per-case verdict on TIER-N:

| posterior P_regress | classification | gate effect |
|---|---|---|
| ≥ 0.80 | **regressed** | counts toward discard (subject to P0.3 cross-case rule) |
| 0.50–0.80 | **ambiguous** | eligible for Simon Stage-2 re-test (P0.5) |
| < 0.50 | **non-regressed** | no gate effect |

Symmetric thresholds on P_improve classify "improved" cases (drives the
keep eligibility under tier-1 outcome family / improvement_threshold layer).

**Why TOST / equivalence margin**: a strict null of "no change" (δ=0)
flags every flaky case as borderline. The δ margin formalizes "noise we
tolerate without flagging" — a 0.10pp drift at p=0.7 is well inside the
n=3 sampling noise band and should not trip a discard.

#### P0.3 — Cross-case decision rule (Benjamini-Hochberg FDR + count threshold)

29 cases tested simultaneously (17 bad_cases + 12 anchors) inflates the
family-wise false-discovery rate. Two complementary thresholds — the run
discards if EITHER fires:

1. **Benjamini-Hochberg FDR**: treat `1 - P_regress` as a quasi-p-value;
   apply BH correction at **α_FDR = 0.10**; discard if ≥ 1 TIER-N case
   survives correction (joint with the per-case threshold ≥ 0.80).
2. **Raw count threshold (interpretable fallback)**: discard if ≥ 2
   TIER-N cases have `P_regress ≥ 0.80`.

The OR-of-two is robust to either threshold being miscalibrated against
real noise; both can be tuned in P0.7 retrospective calibration.

Plus the binding floors (independent of P0.3 above):
- ANY TIER-S case attempt-fail → discard (P0.1).
- ANY existing `tier0_safety` (java_gates + python_tier0_family) failure
  → discard (unchanged from current).

#### P0.4 — Sample-size adjustment

| set | current | proposed | rationale |
|---|---|---|---|
| baseline n | 9 | 9 | unchanged |
| `samples_per_case` (default) | 3 | **5** | per-case SE drops ~22%; cost +67% per iteration. Standard inflection point for binomial estimation. |
| `samples_per_case` (pilot `primary_targets`) | 3 | **11** | matches baseline n; lift detection on `loaded_listing` 0.09→0.30 becomes feasible (two-sample z ≈ 1.24); on `no_ad_id` 0.00→0.20 yields z ≈ 1.5. |

Pilot primary oversampling is bounded: 2 cases × (11-5)=12 extra attempts
per iteration, ~14% over the new default. The asymmetric allocation is
justified: those are the only cases the pilot exists to move; spending more
attempts where signal is expected is a basic variance-reduction move.

(Note: an alternative — Common Random Numbers / paired sampling — would be
more powerful but requires the simulator's LLM to be deterministically
seeded, which the current stack does not support. Worth raising as a P2
follow-on if needed.)

#### P0.5 — Simon two-stage adaptive re-test

Standard adaptive-trial design adapted to per-case re-sampling. When the
Stage-1 (n=5 default, n=11 primary) read produces 1–3 "ambiguous" TIER-N
cases (P0.2 mid-band) AND no TIER-S/safety failure:

- **Stage 2**: re-sample ONLY those 1–3 cases at +5 attempts; recompute the
  posterior on n=10–16 total attempts per re-tested case; finalize the
  per-case verdict at the same 0.80 threshold.

Cost saving vs uniformly raising n: ~3 cases × 5 attempts vs 29 cases × 5
attempts = **~85% cheaper** for the same statistical confidence on the
cases that actually need it. The Bayesian formulation in P0.2 accommodates
Stage-2 by construction — posterior with more observations is just a
tighter posterior; no separate re-test statistic to calibrate.

Stage-2 triggers ONLY when Stage-1 is borderline. A clean Stage-1 result
(0 or 4+ ambiguous cases) finalizes immediately.

#### P0.6a — Baseline-vs-candidate eval-criterion symmetry (audit + n-asymmetry handling)

The candidate's per-case `majority_passed` and the baseline's per-case
`majority_passed` are computed by the SAME `autoloop.scoring.aggregate.aggregate_case`
function (verified 2026-06-15 against `tier_evaluator._effective_case_passed`
+ `baseline_loader._summarize_aggregated`). Same `passes > valid_attempts/2`
formula. Same `min_valid_attempts=3` floor. Same `INVALID_INFRA_ERROR`
routing. Same `comparable=False` → excluded from gating. The per-attempt
judgement comes from the same `eval-interactive` simulator + judge +
`primary_model=deepseek-v4-flash` provider-comparability filter — and S-Y2
Part C made no Java/runtime/CaseSpec change, so the underlying simulator
behaviour is byte-identical between baseline and candidate.

So the eval CRITERION is symmetric. The asymmetry is purely STATISTICAL:

| dimension | baseline | candidate (current) |
|---|---|---|
| `n_attempts` per case (spec) | 9 | 3 |
| `n_attempts` per case (effective, due to `attempt_retry_cap=2` + provider drops) | 9–11 | 2–3 |
| Distance from `min_valid_attempts=3` floor | comfortable (8 attempts of headroom) | **at the floor** (1 invalid attempt → `non_comparable`) |
| Same-`p` majority-flip rate | low | high |

This asymmetry is the deeper reason "direction-right" candidates can show
fewer passes than baseline even when the underlying behaviour is honestly
neutral or improved.

**Required audit (read-only, no code change to plan)**:

1. Confirm `aggregate_case` is the SOLE producer of `majority_passed` on
   BOTH paths (the candidate `eval/<suite>/results.json` and the baseline
   `aggregated.json`). If any code path computes `majority_passed`
   independently, treat that as a defect to file before P0 lands.
2. Confirm the per-attempt `case_passed` boolean comes from the same
   `eval-interactive` rubric on both sides (judge config + L1/L2 gate set
   + CaseSpec closure criterion). Verify the candidate side does NOT
   substitute a different `case_passed` interpretation.
3. Spot-check `comparable=False` routing on both sides: a case that gets
   `min_valid_attempts` failure on either side must be EXCLUDED from
   gating identically.

The audit is read-only and does not block P0 implementation; it surfaces
a defect-class assertion that should be made explicit in the sub-sprint
contract (§7 stanza).

**Bayesian framework naturally handles the n-asymmetry**: the Beta-Binomial
posterior `Beta(0.5 + k, 0.5 + n - k)` has spread inversely proportional to
√n. With n_base=9 and n_cand=3, the candidate's posterior is ~1.7× wider
than the baseline's, so P_regress correctly down-weights candidate-side
"observations" that are merely sampling noise. The proposed n_cand 3→5
default + 3→11 primary_targets (P0.4) narrows this asymmetry directly.

#### P0.6b — Session-level outcome scoring as a partial-credit gap (links to P3)

Independent of measurement noise, the eval CRITERION itself loses gradient
information about partial improvements. Concrete evidence from exp-78
attempt-0 (full transcript in §4): candidate wording elicited the
closure-criterion behaviour for turns 1–3 (asked for ad ref → user gave
AD-1001 → `get_customer_context` → listing-anchored answer, LCS 5/5), but
the case failed `L2:correct_outcome actual=escalate not in ['resolve']`
because the simulated user kept pressing (and the bot's filler follow-ups
fell to escalate by turn 6+).

Session-level binary outcomes mean "did the right thing for turns 1–3 then
fell off" scores identically to "did the wrong thing from turn 1." This
hides a real gradient that the loop could otherwise hill-climb on — the
fitness gate would see a candidate that *partially* fixes a case as
indistinguishable from one that *doesn't move it*. Two threads of work:

- **In scope for P0 (measurement)**: surface per-attempt turn-level
  features into the proposer's analyzer input (e.g.
  `behaviour_match_turns_1_to_n: True`) so the LLM can hill-climb on
  partial progress that the case-pass binary masks. This is observational
  signal addition, not a gate change.
- **Out of scope for P0, kicks to P3 (eval-spec review)**: the persona
  termination behaviour itself. If `no_ad_id` / `loaded_listing` personae
  press until escalation regardless of bot quality, that is an
  `eval_spec` defect to fix in the CaseSpec under §5.4-conscious review,
  separately from the bot-side filler-answer gap. P3 carries this.

#### P0.6c — Invalid-attempt accounting (audit current behaviour; small fixes)

Already partially in place: `aggregation.min_valid_attempts: 3` +
`attempt_retry_cap: 2`. Required audit:

1. **Verify**: when `attempt_retry_cap` is exhausted and the case still
   has < `min_valid_attempts`, does the case mark `non_comparable` (correct)
   or count as failed (incorrect, silently shrinks majority denominator)?
   This was the §3 evidence pattern for exp-77's 5/17 TIMEOUT cases —
   confirm it routes to `non_comparable`, not failure.
2. **Add**: a `non_comparable_rate` field on the per-suite summary +
   §5.9 pre-flight alert when this exceeds 20% across the run. This is
   purely observational; does not gate verdict (the gate already excludes
   `non_comparable` from the per-case pass map).

This subitem is the smallest in scope and may already be fully implemented;
audit-only.

#### P0.7 — Retrospective calibration (acceptance evidence; no new LLM cost)

Before live deployment, recompute the 14 verdicts (exp-66..79) against the
n=9 prepilot baseline using the proposed rule. Expected pattern:

- **Family A on RESOLVE-FAQ** (exp-77/78/71/72/76/70 — partial Tier-2
  flips + flaky single-case drops): should reclassify from `discard` to
  `ambiguous` or `non-regressed` (the loaded_listing 0.09→0.20 nudges
  become visible; the single-case anchor flips like uc_a_visibility 0.64
  fail-fluctuation lose statistical weight).
- **Family C on escalation_policy** (exp-66/67/68/73/74 — tier0
  escalation_compliance failures): MUST remain discarded — tier0 floor is
  unchanged, this is a real route-error class.
- **The exp-78 anti-误杀 regression** (control 1.00→0.33 — TIER-S, real
  over-elicitation): MUST remain discarded — failure to discard
  invalidates P0.1 and the calibration fails.
- **Family B on DISCOVER** (exp-69/75/79): heterogeneous result expected;
  the principle test is "does the rule discriminate exp-79's
  uc_d_login 8→7 (single-case flaky drop) from exp-69's 8→5 (genuine
  multi-case harm)?" — the proposed rule should keep the first as
  ambiguous and the second as regressed.

If the calibration produces this pattern, proceed to live deployment as a
§7-semantic-eligible sub-sprint of resumed M-Auto-4 scope. If not, iterate
on δ / posterior threshold / TIER-S inclusion before live deployment.
Retrospective calibration costs zero LLM time — only the per-case
posteriors need to be recomputed from existing `case_results` JSON.

#### P0.8 — Parameter sheet (consolidates above)

| Parameter | Current | Proposed | Source |
|---|---|---|---|
| `samples_per_case` (baseline) | 9 | 9 | unchanged |
| `samples_per_case` (candidate default) | 3 | **5** | P0.4 |
| `samples_per_case` (pilot `primary_targets`) | 3 | **11** | P0.4 |
| `anchor_outcome_max_drop_cases` | 0 | **deprecated** (replaced by tiered rule) | P0.3 |
| TIER-S decision rule | implicit in majority | **any attempt-fail → discard** | P0.1 (anti-误杀 floor) |
| TIER-N decision rule | majority + max_drop_cases | **Beta-Binomial posterior + δ + threshold** | P0.2 |
| TIER-F decision rule | implicit in majority | **improvement direction only** | P0.1 |
| Practical equivalence margin δ | n/a | **0.10** | P0.2 (≈1× n=3 noise floor) |
| Posterior decision threshold (regressed) | n/a | **0.80** | P0.2 |
| Posterior decision threshold (ambiguous) | n/a | **0.50–0.80** | P0.2 |
| Cross-case discard rule | max_drop_cases=0 | **BH-FDR α=0.10 OR ≥2 cases @ P_regress≥0.80** | P0.3 |
| Sequential re-test trigger | none | **1–3 ambiguous TIER-N cases, no TIER-S/safety failure** | P0.5 |
| Stage-2 sample addition | n/a | **+5 attempts per ambiguous case** | P0.5 |
| `min_valid_attempts` floor | 3 | 3 | unchanged (audit per P0.6) |
| `attempt_retry_cap` | 2 | 2 | unchanged (audit per P0.6) |
| Baseline-vs-candidate eval criterion | assumed symmetric | **audit + sub-sprint assertion** | P0.6a |
| Turn-level partial-credit signal | not surfaced | **observational input to analyzer** | P0.6b |
| `non_comparable_rate` alert | none | **>20% surfaces §5.9 pre-flight warning** | P0.6c |
| Calibration acceptance evidence | n/a | **retrospective on exp-66..79 (no new LLM cost)** | P0.7 |
| `stability_thresholds` consumers | rebless_baseline.py only | **+ gate tier_evaluator** | P0.1 (wiring fix) |

#### Industry references (named methods)

- **Beta-Binomial Bayesian inference for binary outcomes**: Gelman et al.,
  *Bayesian Data Analysis* (3rd ed.), §2.4; Berger, *Statistical Decision
  Theory and Bayesian Analysis* (2nd ed.), §4.2. The de-facto
  small-sample binomial estimator in modern A/B testing platforms
  (Optimizely, Bayesian Stats package in R/Python).
- **Wilson score confidence interval** for binomial proportions: Wilson,
  *JASA* 22(158), 1927; Brown, Cai, DasGupta, *Statistical Science*
  16(2), 2001 — the recommended replacement for Wald CI at small n.
- **Equivalence testing / Two One-Sided Tests (TOST)** with practical
  margin δ: Schuirmann, *J. Pharmacokin. Biopharm.* 15, 1987; widely used
  in clinical-trial non-inferiority designs and (more recently) in
  reproducibility analysis (Lakens, *Soc. Psychol. Personality Sci.* 8,
  2017).
- **Benjamini-Hochberg FDR**: Benjamini & Hochberg, *JRSS-B* 57(1),
  1995 — multiple-comparison correction standard in genomics + ML.
- **Simon's two-stage adaptive design**: Simon, *Controlled Clinical
  Trials* 10, 1989 — the canonical stop-or-continue rule for
  phase-II clinical trials, directly transferable to per-case re-test.
- **Sequential probability ratio test (SPRT)** background: Wald,
  *Sequential Analysis* (1947). Informs the P0.2/P0.5 cost-benefit
  framing (continue sampling iff posterior is mid-band).

### P1 — §3.4 hand-authored fallback content (semantic; already authorized by the S-Y2 contract)

Compose from the validated fragments, on `resolve_faq_grounded_answer.yaml`
within the 6×4 surface: exp-78's listing-context anchoring + the
one-focused-ask + exp-72's graceful lookup-fallback + exp-76's
loaded-framing-doesn't-change-route, **plus the missing tail**: after
anchoring on listing state, give the product-truthful close (what the
listing's observable state means for the user's question, citation, then
`record_outcome` resolved) instead of generic filler. Same CaseSpec gate,
same human §4.1 review. Avoid the DISCOVER surface in the same candidate
(Family B's collateral profile; revisit only if the RESOLVE-side fallback
leaves the misroute half open).

Note: under the P0 rule, this candidate's evaluation will benefit
immediately — the Tier-2 neighbor flip and the loaded_listing 0.09→0.20
nudge become *measurable* rather than masked by majority-flip noise.

### P2 — Loop search mechanics (autoloop; post-measurement-fix)

1. **Surface pass_rate deltas of pilot cases to the analyzer/proposer**:
   `loaded_listing` 0.09→0.20 was invisible (majority still fail), so the
   loop cannot hill-climb on partial progress — the strongest available
   gradient signal is currently discarded.
2. **Lineage/refinement operator**: allow "refine the previous discarded
   candidate" (mutation lineage) instead of fresh proposals each iteration;
   exp-77/78's partial progress was thrown away.
3. **Two-field coordinated bundle (design decision, human)**: failure shapes
   spanning DISCOVER routing + RESOLVE behaviour cannot be fixed by any
   single-field candidate under program.md fence #9. Either accept that
   class as §3.4-fallback-only, or design a bounded 2-field bundle variant
   (program.md revision; Codex per-candidate review unchanged).

### P3 — Eval-spec review question (human, §5.4-conscious)

Review the `no_ad_id` (and `loaded_listing`) simulator persona termination
behaviour against the closure criterion using the exp-78 attempt-0 evidence:
cooperative persona + correct bot behaviour + outcome still `escalate
user_requested`. If the persona cannot accept any answer, that is an
`eval_spec` defect to fix in the CaseSpec (with the override documented per
§5.4) — separately from the genuine bot-side filler-answer gap, which stays
a `semantic_planner` target.

### Deferred — TIMEOUT root-cause brief (network-attributed)

Per human review 2026-06-15: TIMEOUT contamination attributed to network
conditions and deferred. Tracked as a §5.6 carried observation, NOT a §5.8
framework-defect blocker. P0.6c above ensures TIMEOUTs are properly accounted
for in measurements regardless of whether their root cause is addressed
(invalid-attempt routing + `non_comparable_rate` alert). Revisit if the
TIMEOUT-tag rate exceeds 20% in a future run OR if P0.7 retrospective
calibration shows TIMEOUT-driven case-classification artifacts.

## 6. What this does NOT change

- S-Y2 Part C verdict stands (handoff: `docs/sprints/sprint-088-handoff.md`):
  CORE GATE met, steering strong-pass, §3.4 fallback is the next step.
- No edit to autoloop code, fitness config, CaseSpecs, skills, or baselines
  is made by this analysis. Every P-item above requires its own sprint
  contract, §7 stanza where semantic, and human go-ahead.
