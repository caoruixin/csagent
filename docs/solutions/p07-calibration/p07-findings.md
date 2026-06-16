---
title: P0.7 retrospective calibration — findings (exp-66..79, zero-LLM-cost)
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: docs/solutions/p07-calibration/calibrate.py + calibration-results.json
last_reviewed: 2026-06-15
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Zero-LLM-cost acceptance-evidence step gating the P0 fitness-measurement
  sprint (resumed M-Auto-4 scope) per the S-Y2 analysis doc §P0.7. Recomputes
  the 14 autoloop verdicts under the proposed P0 rule from EXISTING case_results
  JSON only (no new eval run). Reproducible: `python3 docs/solutions/p07-calibration/calibrate.py`.
  Verdict: CONDITIONAL PASS — validates the floors + the core noise>signal
  thesis, but (a) forces one correction to the literal P0.1 TIER-S rule and
  (b) surfaces two threshold/scope decisions + one power ceiling that the
  sprint contract must resolve before tier_evaluator.py is touched.
---

# P0.7 retrospective calibration — findings (2026-06-15)

## 0. What was run

Recomputed the 14 autoloop verdicts (exp-66..79) under the proposed P0 rule
against the n=9 (effective n=11) pre-pilot baseline
(`m-auto-7-prepilot-baseline-20260608`), using ONLY the per-case `case_results`
JSON already on disk. No LLM, no new eval run.

- **12 of 14 are calibratable.** exp-70 / exp-76 were discarded *pre-eval* by
  the anti-hardcode sandbox (`anti_hardcode_rejected:Q1.if_then_decision_tree`)
  — the vector S-Y1.5b/c/d fixed — and have **no run dir / no eval data**, so
  the fitness-gate calibration cannot touch them. They are out of scope for a
  *fitness-gate* calibration by construction.
- Candidate runs used **n=5** attempts/case (TIMEOUT-shrunk to 3–4 on some
  cases), not the n=3 the analysis doc assumed. This matters (§4).

Four rule variants were evaluated (full table in `calibration-results.json`,
script `calibrate.py`):

| Variant | TIER-S rule | shadow Layer-4 count gate |
|---|---|---|
| **V1** literal P0.1 | any-attempt-fail → discard | on |
| **V2** sensitivity | any-attempt-fail (infra/TIMEOUT attempts stripped) | on |
| **V3** corrected | **majority-flip → discard** | on |
| **V4** corrected, shadow off | majority-flip | off |

TIER classification over the 29-case gating set (bad_cases + anchor_outcome):
**TIER-S = 3** (`cs066`, `cs_uc_a_generic_policy_question` = the anti-误杀
control, `anchor_outcome_uc_k_tech`), **TIER-F = 5**, **TIER-N = 21**. This
reconciles exactly with the doc's §2.1 inventory and with the historical
discard reasons (baseline majority-pass = 8 bad_cases + 8 anchors).

## 1. Verdict matrix vs the §P0.7 expected pattern

`historical` = recorded `discard_reason`. `V1` = literal rule. `V3` = corrected
rule (majority-flip TIER-S). "✓/✗" = match against the §P0.7 expected pattern.

| exp | family / field | historical discard | §P0.7 expects | V1 | V3 | V3 ✓? |
|---|---|---|---|---|---|---|
| 67 | C `escalation_policy` | tier0 escalation_compliance @cs11s01 | remain discard | DISCARD(tier0) | DISCARD(tier0) | ✓ |
| 68 | C `escalation_policy` | tier0 escalation_compliance @cs11s01 | remain discard | DISCARD(tier0) | DISCARD(tier0) | ✓ |
| 73 | C `escalation_policy` | tier0 escalation_compliance @cs11s01 | remain discard | DISCARD(tier0) | DISCARD(tier0) | ✓ |
| 74 | C `critical_steps[0]` | tier0 escalation_compliance @cs11s01 | remain discard | DISCARD(tier0) | DISCARD(tier0) | ✓ |
| 78 | A `grounding_instruction` | tier1 bad_cases 8→7 | **remain discard (anti-误杀)** | DISCARD(TIER-S) | **DISCARD(TIER-S 1/3)** | ✓✓✓ |
| 69 | B `grounding_instruction` | tier1 anchor 8→5 | **regressed (multi-case)** | DISCARD(TIER-S 4/5) | **DISCARD(regressed, 3 cases)** | ✓✓ |
| 71 | A `critical_steps[1]` | tier1 anchor 8→6 | reclassify off-discard | KEEP-elig | KEEP-elig | ✓ |
| 77 | A `critical_steps[0]` | tier1 anchor 8→7 | reclassify off-discard | DISCARD(TIER-S 4/5) | KEEP-elig | ✓ |
| 79 | B `critical_steps[2]` | tier1 anchor 8→7 | ambiguous (single flaky) | DISCARD(TIER-S 4/5) | KEEP-elig | ✓ (off-discard) |
| 75 | B `grounding_instruction` | tier2 per_uc UC-A 0→1 | (discriminate) | DISCARD(TIER-S 3/5) | KEEP-elig | ~ |
| 66 | C `escalation_policy` | tier2 aggregate 3→4 | remain discard | KEEP-elig | **KEEP-elig** | **✗** |
| 72 | A `grounding_instruction` | tier2 aggregate 3→4 | reclassify off-discard | DISCARD(shadow 3.9pp) | **DISCARD(shadow 3.9pp)** | **✗** |

## 2. Findings

### F1 — The literal P0.1 TIER-S rule ("any attempt failure → discard") is itself a false-discard machine at n=5. **(must correct)**

V1 discards exp-69/75/77/79 on a single flaky attempt — a baseline-1.0 case
(`cs_uc_a_generic_policy_question`, `uc_k_tech`, `cs066`, `cs01s01`) showing
**4/5** in the candidate. A true-1.0 case at n=5 produces ≥1 failure ~20–25% of
the time, so "any attempt fail" relocates the exact disease P0 is curing into
the TIER-S tier. **Correction: TIER-S must be majority-flip (or posterior-based),
not any-fail.** Under majority-flip (V3) the anti-误杀 protection still fires
where it should (F2).

### F2 — The anti-误杀 floor works, and the tier0 floor works. **(core PASS)**

- **exp-78** — `cs_uc_a_generic_policy_question` genuinely majority-flipped
  **1.00 → 1/3** (real over-elicitation). V3's majority-flip TIER-S floor
  discards it. This is the single most important guarantee (§5.4 anti-误杀) and
  it holds.
- **exp-67/68/73/74** — the tier0 delta floor (unchanged current Layer-0 logic)
  exclusively catches the genuine route-error class: `escalation_compliance`
  newly broken on `cs11s01` (a shadow case; the tier0 floor is suite-wide, not
  firewalled). These stay discarded under every variant.

### F3 — The tiered statistic discriminates genuine multi-case harm from single-case noise. **(core PASS)**

- **exp-69** (historical anchor 8→5) → **DISCARD(regressed)** with **3** TIER-N
  cases at P_regress ≥ 0.80 (`uc_e_promotion` 0.93, `uc_fp_removed` 0.99,
  `iwzx` 0.89). Genuine harm, correctly killed.
- **exp-79 / exp-71 / exp-77** (single flaky drops / on-target Family A) →
  reclassified **off discard**. The count≥2 cross-case rule is what separates
  exp-69 (3 regressed) from exp-79 (1 regressed). This is the headline
  improvement over `max_drop_cases=0`.

### F4 — Two residual mismatches → two scope decisions the contract must make. **(decisions)**

- **exp-66 stays KEEP-eligible (doc expected discard).** Its historical discard
  was **tier2** (escalation-flow aggregate 3→4), NOT tier0 — so the doc's
  "Family-C escalation" lumping was imprecise. The per-case *outcome* posterior
  does not see escalation-/tier2-flow regressions, and its one strong regression
  (`uc_i_payment` 4/11→0/5, P_reg 0.88) is tolerated by the count≥2 threshold.
  → **Decision C1**: does P0 retain a (noise-aware) tier2/escalation-flow gate,
  and/or discard on **≥1** strong regression rather than ≥2?
- **exp-72 stays DISCARD on shadow (doc expected reclassify).** Its only barrier
  is the shadow Layer-4 **count** gate (3pp): shadow dropped 25→21.1% (3.9pp ≈
  <1 case). V4 (shadow gate off) flips it to KEEP-eligible. The shadow and tier2
  **count** gates are noise sources P0 does **not** propose to change (P0 scopes
  to the 29-case bad+anchor set). → **Decision C2**: extend the noise-aware
  treatment to shadow/tier2, or keep them as separate count gates.

### F5 — Power ceiling: the exp-66..79 data is itself in the low-n regime P0 calls unreliable. **(scope honesty)**

Candidate n=5 (TIMEOUT-shrunk to 3–4) is the SAME under-powered regime P0.4
proposes to fix (n=5 default / n=11 primary). Posteriors swing on 0/5 vs 5/5
outcomes. Concretely, **exp-66 and exp-79 are statistically indistinguishable**
at this n — each has exactly one case at P_regress ≈ 0.88 — so the doc's
expectation that one discards and the other does not is **not achievable by any
noise-aware rule** at n=5; it was an artifact of the historical *mechanism*
(tier2 vs tier1), not of signal. **Conclusion:** P0.7 retrospective calibration
can certify (i) the floors (tier0 + anti-误杀 majority-flip) and (ii) the
qualitative signal>noise improvement, but it **cannot** certify the precise
TIER-N knobs (δ=0.10, 0.80 threshold, count rule) at n=5. Final knob calibration
needs the P0.4 resampling, which is not zero-LLM-cost. The "zero-cost" gate has
a real ceiling — and that is itself evidence FOR P0.4.

## 3. Bottom line

- **V1 (rule as literally written): FAIL** — the any-fail TIER-S floor must be
  corrected.
- **V3 (majority-flip TIER-S): CONDITIONAL PASS** — reproduces the §P0.7 pattern
  on every CRITICAL item (anti-误杀 exp-78 held; tier0 Family C held; multi-case
  exp-69 flagged regressed; flaky/on-target exp-71/77/79 released off-discard),
  with two residual mismatches (exp-66, exp-72) that are not noise artifacts but
  **named scope decisions** (C1 tier2/escalation-flow + cross-case threshold;
  C2 shadow/tier2 count-gate scope).
- The calibration did its job: it caught a real mis-calibration in the proposal
  and bounded what zero-cost evidence can and cannot certify — **before** any
  LLM cost or `tier_evaluator.py` edit.

## 4. Inputs to the sprint contract (if the human proceeds)

1. Encode **majority-flip (or posterior) TIER-S**, never any-attempt-fail (F1).
2. Keep the **tier0 delta floor unchanged** (F2) and the **TIER-N Beta-Binomial
   + δ + BH/count** core (F3).
3. Resolve **C1** (tier2/escalation-flow gate + ≥1-vs-≥2 cross-case threshold)
   and **C2** (shadow/tier2 count-gate scope) explicitly in scope.
4. State the **power ceiling** (F5): retrospective calibration certifies floors
   + direction only; the precise knobs are confirmed on the first P0.4-sampled
   real-LLM run, not by this zero-cost pass.

## 5. Resolution (human decisions, 2026-06-15)

- **Proceed → write contract encoding V3** (majority-flip TIER-S). **PROMOTED
  2026-06-16** to `docs/sprint_objective.md` (Sprint 091 / S-Auto-37 = M-Auto-7
  pre-pilot blocker **S-Y1.7**, preempting S-Y2 Part C per §5.8; dev prompt
  `compact/sprint-091-dev-prompt.md`). The draft
  `docs/solutions/2026-06-15-p0-fitness-measurement-reliability-sprint-contract.md`
  is now `status: superseded` (superseded_by the live objective).
- **C1 / C2: keep tier2 + shadow gates, made noise-aware; cross-case stays count≥2.**
  Grounded against the data:
  - **exp-66 → AMBIGUOUS (not discard).** Its tier2 3→4 is driven solely by
    `wmkb_uc_a_trader_flag_secondary_uc_h` (cand 2/5 vs base 0.727), `P_regress =
    0.797` — just under 0.80, i.e. ambiguous. The noise-aware Layer-2 therefore
    does not gate it. The doc's "exp-66 must remain discarded" was a hard-count
    artifact, not a noise-aware truth (F5 knife-edge at 0.797 vs 0.80).
  - **exp-72 → off-discard.** Its shadow drop (3.9pp ≈ <1 case) is below any
    noise-aware bar; the noise-aware shadow test releases it (matches §P0.7).
- The committed `calibrate.py` implements the floors + Layer-1 statistic only;
  the dev EXTENDS it to the C1/C2 noise-aware Layer-2/Layer-4 and re-pins the
  oracle matrix (contract §Scope #9).
