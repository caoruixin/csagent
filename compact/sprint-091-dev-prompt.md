# Dev prompt — Sprint 091 / S-Auto-37 (M-Auto-7 S-Y1.7): Autoloop fitness-measurement reliability (P0)

> Self-contained executable view of `docs/sprint_objective.md` (S-Y1.7). Paste to
> start; the work is fully within this prompt's embedded contract. Source-of-truth
> is `docs/sprint_objective.md`; if they ever diverge, the objective wins.

## 1. Role identity

You are the **dev agent for Sprint 091 / M-Auto-7 S-Y1.7**. One-line goal:
**replace the autoloop fitness gate's zero-tolerance majority-flip rule with a
noise-aware, per-case-stability-tiered statistical rule (the V3 variant), so the
gate stops false-discarding behaviour-neutral / on-target candidates on n=3–5
sampling noise — while strictly preserving the §5.4 anti-误杀 floor and the §1.4
tier0 safety floor.** This is an `infra` (eval-framework) sprint that PREEMPTS
the S-Y2 pilot per §5.8; it changes `autoloop/**` + `config.yaml` ONLY.

## 2. Read order (minimal)

1. `AGENTS.md` (auto-loaded — governance chain).
2. **This prompt** (embedded contract below).
3. **The acceptance evidence + reference implementation** (READ FIRST — it is the
   executable spec your code must agree with):
   - `docs/solutions/p07-calibration/calibrate.py` — the validated reference
     implementation of the rule (the `betai` / `posterior_regress_improve` /
     floor / cross-case logic). Your `tier_evaluator` MUST agree with it on the
     archived data.
   - `docs/solutions/p07-calibration/p07-findings.md` — the calibration findings
     (F1 why any-fail TIER-S was rejected; F4 the exp-66/72 grounding; F5 the
     power ceiling) + §5 the locked human decisions.
4. **The code you will change** (read before editing):
   - `autoloop/autoloop/scoring/tier_evaluator.py` — the 5-layer evaluator
     (`evaluate`, `_evaluate_layer0..4`, `_effective_case_passed`,
     `_suite_passed_count`, `_tier2_mandatory_metrics`).
   - `autoloop/autoloop/scoring/baseline_loader.py` — `BaselineSnapshot` /
     `SuiteSnapshot` (extend to expose per-case `(k,n,pass_rate,stability_class)`).
   - `autoloop/autoloop/scoring/aggregate.py` — `aggregate_case` /
     `classify_stability` + `stability_thresholds` (P0.6c instrumentation lives
     near here).
   - `autoloop/config.yaml` — the `fitness:` block (the knobs in §Scope #7).
5. **The oracle data (read-only):** `eval_interactive/results/m-auto-7-prepilot-baseline-20260608/`
   (per-suite `aggregated.json`) + `autoloop/results/runs/exp-{66,67,68,69,71,72,73,74,75,77,78,79}/eval/`.

Do NOT broadly explore. Everything you need is here + the files above.

## 3. Context (why this sprint exists)

The P0.7 retrospective calibration (zero-LLM; recomputed the 14 autoloop verdicts
from existing `case_results` JSON) found the current gate
(`anchor_outcome_max_drop_cases: 0` + raw tier2/shadow count gates) false-discards
a behaviour-neutral candidate ~92–95% of the time at n=3–5 — it selects on noise,
not quality. It also REJECTED the analysis doc's literal "any-attempt-fail" TIER-S
rule (it discards a true-1.0 case showing 4/5, which happens ~20–25% of the time
at n=5) in favour of **majority-flip TIER-S (V3)**. Human decisions (2026-06-16):
encode V3; keep tier2 + shadow gates but make them noise-aware; cross-case stays
count≥2. The knobs are not certifiable at n=5 (F5) — they are evidence-backed
defaults confirmed on the next real-LLM pilot run.

---

# EMBEDDED CONTRACT (S-Y1.7)

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` (eval-framework: the autoloop 5-layer fitness gate / scoring). Governs which `semantic_planner` skill-yaml candidates survive to the §4.1 gate — hence §7 is filled. |
| **§7 stanza** | **REQUIRED** (see §7 below). |
| **Per-sub-sprint Codex** | **REQUIRED** (§4.3 trigger #3) — must complete before S-Y2 Part C runs. |
| **Re-bless** | **NONE** — `autoloop/**` + `config.yaml` only; `scoring_code_baseline_sha` changes but the acceptance oracle is the archived candidate replay, not a new real-LLM baseline. |

## The encoded rule (V3) — normative spec

Evaluation stays **lexicographic** (first failing layer short-circuits). Floors
override the statistic.

### Floors (binding)
- **F0 — tier0 delta floor (Layer 0, UNCHANGED).** Keep `_evaluate_layer0`
  byte-for-byte: a `_TIER0_PY_FAMILY` check False in candidate-majority but
  True/unknown in baseline for the same (suite,case) → discard.
- **FS — TIER-S anti-误杀 floor (NEW = the §5.4 floor).** Any TIER-S case
  (baseline pass_rate == 1.0; incl. anti-误杀 control `cs_uc_a_generic_policy_question`,
  `uc_k_tech`, `cs066`, `cs01s01`) that **majority-flips to fail** in the
  candidate → discard. **NOT any-attempt-fail.**

### TIER classification (per baseline case)
Consume the EXISTING `stability_thresholds` + `classify_stability` on each
baseline `aggregated.json` (P0.1 — currently only the re-bless reads them; the
gate must too):
- **TIER-S** = baseline pass_rate == 1.0 → FS floor (majority-flip → discard).
- **TIER-F** = baseline pass_rate == 0.0 → improvement-direction only; cannot gate discard.
- **TIER-N** = 0 < pass_rate < 1.0 (21/29 gating cases) → Beta-Binomial posterior.

### TIER-N statistic (replaces the Layer-1 max_drop rule)
Jeffreys-prior Beta-Binomial, TOST margin **δ = 0.10**:
```
p_base ~ Beta(0.5 + k_base, 0.5 + n_base - k_base)
p_cand ~ Beta(0.5 + k_cand, 0.5 + n_cand - k_cand)
P_regress = P(p_cand < p_base - δ)    # one-sided TOST lower
P_improve = P(p_cand > p_base + δ)    # one-sided TOST upper
```
Per-case: `P_regress ≥ 0.80` → regressed; `0.50 ≤ P_regress < 0.80` → ambiguous;
`< 0.50` → non-regressed. Symmetric on `P_improve` for improved. **Reuse the math
in `calibrate.py` verbatim** (`betai` + stratified `posterior_regress_improve`,
base-quantile cached) — no numpy/scipy in the env.

### Cross-case rule (Layer 1; count≥2)
Discard if EITHER: BH-FDR at **α = 0.10** (treat `1 − P_regress` as quasi-p,
joint with per-case `P_regress ≥ 0.80`) flags ≥1; OR ≥ **2** TIER-N cases at
`P_regress ≥ 0.80`.

### Layer 2 (tier2 critical-flow) — RETAINED, noise-aware (C1)
Gate any mandatory-failure INCREASE through the affected case's outcome
posterior: it counts toward discard only if that case's `P_regress ≥ 0.80`.
Grounded: exp-66's tier2 3→4 is driven by `wmkb_uc_a_trader_flag_secondary_uc_h`
at `P_regress=0.797` < 0.80 → **exp-66 = AMBIGUOUS, not discard** (F5 knife-edge).

### Layer 4 (shadow) — RETAINED, noise-aware (C2)
Replace the raw `shadow_max_drop_pct: 3.0` with a noise-aware test (Beta-Binomial
posterior on the shadow aggregate pass count; discard only on a
statistically-supported drop). Grounded: exp-72's 3.9pp ≈ <1 case → **released**.
Per-case shadow firewall UNCHANGED.

### Sample sizes (P0.4) + invalid-attempt accounting (P0.6c)
| Parameter | Current | New |
|---|---|---|
| `fitness.samples_per_case` (default) | 3 | **5** |
| `fitness.pilot.primary_targets` samples | 3 | **11** |
| `fitness.anchor_outcome_max_drop_cases` | 0 | **removed** (replaced) |
| TIER thresholds (δ / P_regress / P_ambig / α_FDR / count) | n/a | **0.10 / 0.80 / 0.50 / 0.10 / 2** |
| `non_comparable_rate` summary + §5.9 preflight alert >20% | none | **add** (observational) |

## Scope (numbered)

1. **TIER + per-case (k,n) plumbing** — extend `SuiteSnapshot` to expose per-case
   `(k_base, n_base, pass_rate, stability_class)`; candidate `_CurrentSuite`
   already has per-case `pass_rate`/`valid_attempts`. Add `classify_tier()`.
2. **Beta-Binomial posterior module** — port `betai` + `posterior_regress_improve`
   from `calibrate.py`; unit-test against known incomplete-beta values.
3. **Rewrite `_evaluate_layer1`** — FS TIER-S floor + per-TIER-N posteriors +
   classify + BH-FDR/count≥2 cross-case rule. Remove `anchor_outcome_max_drop_cases`.
4. **`_evaluate_layer2` noise-aware** (C1).
5. **`_evaluate_layer4` noise-aware** (C2); firewall unchanged.
6. **`_evaluate_layer3`** improvement gate → posterior "improved" (≥1 case
   `P_improve ≥ 0.80` OR supported tier2 reduction).
7. **`config.yaml fitness`** — `samples_per_case` 3→5; add `pilot.primary_targets`
   oversampling 11; remove `anchor_outcome_max_drop_cases`; add a `tier_decision`
   block (δ/thresholds/α_FDR/count). Comment each with the P0.x rationale.
8. **Instrumentation (P0.6c)** — `non_comparable_rate` per-suite summary + §5.9
   preflight alert >20%; audit TIMEOUT/`INVALID_INFRA_ERROR` exhausted-retry
   attempts route to `non_comparable` (excluded), not failed. Observational only.
9. **Acceptance oracle test (zero-LLM)** — run the new `evaluate(...)` on the 12
   archived candidate dirs vs the prepilot baseline; assert:

   | exp | pinned verdict | mechanism |
   |---|---|---|
   | 67, 68, 73, 74 | **DISCARD** | F0 tier0 (escalation_compliance@cs11s01) |
   | 78 | **DISCARD** | FS TIER-S (anti-误杀 control 1.00→1/3) |
   | 69 | **DISCARD** | cross-case regressed (≥2 at P_regress≥0.80) |
   | 71, 77, 79, 75 | **off-discard** | TIER-N releases (≤1 regressed) |
   | 66 | **off-discard (AMBIGUOUS)** | tier2 driver wmkb P_regress=0.797 < 0.80 (C1) |
   | 72 | **off-discard** | shadow ≈ <1 case, noise-aware shadow releases (C2) |

   `calibrate.py` is the reference impl; the evaluator must agree with it on this
   data. (As committed `calibrate.py` does floors + Layer-1 only — exp-66
   keep-eligible, exp-72 shadow-discarded; you EXTEND it to the C1/C2 Layer-2/4
   wiring, at which point 66→ambiguous and 72→off-discard, and re-pin the matrix.)

## Hard fences / STOP conditions

- **No change outside `autoloop/**` + `autoloop/config.yaml`.** `server/**`,
  `eval_interactive/**` (CaseSpecs/fixtures/simulator/judge/scoring), baselines —
  byte-identical.
- **The §5.4 anti-误杀 floor may only be STRENGTHENED, never weakened.** FS + F0
  are floors; the statistic NEVER overrides them. Forbidden: letting a genuine
  anti-误杀 control regression (like exp-78's 1/3) pass under a "noise" framing.
- **No semantic hardcode** — stability-tiered statistic only; no keyword/regex/
  enum/per-UC/per-CaseSpec branch; **no CaseSpec id in the gate logic.**
- **No `current_eval_baseline.md` canonical flip; no `baseline_dir` move.**
- **Do NOT tune thresholds to force the doc's expected pattern on exp-66/72.**
  Whatever the principled noise-aware wiring decides is what the oracle pins
  (§1.6 — eval is evidence, not authority; do not p-hack).
- **STOP + escalate** if: the C1/C2 wiring can't preserve the exp-69-vs-exp-79
  discrimination (count≥2) without breaking a floor; or your posterior impl
  disagrees with `calibrate.py` on the archived data (they MUST agree).

## Test / eval requirements

- **Acceptance = §Scope #9 oracle test passes** + V3 matrix reproduced + handoff
  records the C1/C2 resolution for exp-66/72.
- **autoloop pytest: no regression** (current **348**; new tests add to it).
- **Java + UI untouched** (no `server/**` / `ui/**`).
- **Mocked/offline replay is PRIMARY evidence here** — this is `infra`
  scoring-logic on FIXED recorded data, not a prompt/behaviour change, so §5.7
  does not apply.
- **F5 power ceiling (record in handoff):** the oracle certifies floors +
  direction + C1/C2 wiring; the precise knobs (δ, 0.80, count) are confirmed on
  the first S-Y2 pilot run under the new gate, NOT by this sprint.

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (eval-framework: the autoloop 5-layer fitness
gate / scoring). Defect = the zero-tolerance majority-flip rule's ~92–95%
false-discard rate on n=3–5 noise. Secondary surface: governs which
`semantic_planner` candidates reach the §4.1 gate.

**Tier-0 invariant:** Adds no Tier-0 invariant. PRESERVES the §1.4 tier0 floor
(Layer 0 unchanged) and STRENGTHENS the §5.4 anti-误杀 floor into the explicit
TIER-S majority-flip floor (FS).

**Semantic hardcode:** None. A stability-tiered Beta-Binomial / TOST / BH-FDR
rule applied uniformly by baseline stability tier — no keyword/regex/enum/per-UC/
per-CaseSpec branch; no CaseSpec id in the gate. Defenses: the oracle pins
behaviour to recorded data; anti-误杀 majority-flip is a hard discard; Codex §4.1
review. Sunset: n/a.

**Generalization coverage:** retrospective oracle over **12** archived
experiments (four families: RESOLVE-FAQ A / DISCOVER B / escalation_policy C /
anti-误杀 control) — floors + cross-case discrimination validated. Forward knob
confirmation on the next S-Y2 pilot run at P0.4 sampling. Per-CaseSpec
target/neighbor/negative/shadow counts: n/a (measurement-layer change; coverage
is the experiment corpus).

## Codex review plan

Per-sub-sprint Codex REQUIRED (trigger #3). Review the rule + C1/C2 wiring + the
oracle test under the §4.1 nine-question kernel; verdict to `docs/codex-findings.md`
(§4.2 header). Must complete before S-Y2 Part C runs.

## Handoff requirements (`docs/sprints/sprint-091-handoff.md`)

Record: implemented rule + final parameter values; the oracle test's reproduced
V3 matrix; the **C1 resolution** (Layer-2 noise-aware wiring + exp-66 verdict);
the **C2 resolution** (shadow noise-aware test + exp-72 verdict); the
`non_comparable_rate` instrumentation + TIMEOUT-routing audit result (P0.6c);
confirmation no floor was weakened; the `scoring_code_baseline_sha` delta + the
no-re-bless rationale; and the **F5 power-ceiling** statement.

## Commit discipline

Stage explicitly by file (NO `git add -A` — autoloop dirty-index hazard). All
commits `autoloop/**` + `config.yaml` only. Deliver close-bundle artefacts
(`sprint_objective.md`, `10-handoff.md`, `action_bank.md`, handoff) bundled by
the human at close.

## 4. Self-check before declaring done

- [ ] `tier_evaluator` agrees with `calibrate.py` (extended to C1/C2) on all 12
      archived experiments; the §Scope #9 V3 matrix is reproduced exactly.
- [ ] FS TIER-S floor is **majority-flip**, never any-attempt-fail; exp-78's
      anti-误杀 control (1/3) still discards; the 4/5 flakes (exp-77/79) do NOT.
- [ ] F0 tier0 floor is byte-for-byte unchanged; exp-67/68/73/74 still discard.
- [ ] Layer-2 (C1) + Layer-4 (C2) are noise-aware; exp-66 → AMBIGUOUS, exp-72 →
      off-discard; the shadow per-case firewall is unchanged.
- [ ] No CaseSpec id / keyword / regex / enum / per-UC branch in the gate logic.
- [ ] Only `autoloop/**` + `config.yaml` changed; `git diff --stat` confirms no
      `server/**` / `eval_interactive/**` / baseline touch.
- [ ] autoloop pytest green (≥348 + new); posterior module unit-tested vs known
      incomplete-beta values.
- [ ] Handoff records the C1/C2 resolutions, the `scoring_code_baseline_sha`
      delta + no-re-bless rationale, the P0.6c audit, and the F5 power ceiling.
- [ ] No `baseline_dir` / `current_eval_baseline.md` change.
