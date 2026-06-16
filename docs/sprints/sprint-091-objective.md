---
title: Sprint 091 / S-Auto-37 (M-Auto-7 S-Y1.7) — Autoloop fitness-measurement reliability (P0)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-16
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  INSERTED M-Auto-7 pre-pilot blocker S-Y1.7, promoted 2026-06-16, that PREEMPTS
  the S-Y2 CORE-GATE pilot per §5.8 (framework-defect priority: this is an
  infra/eval-framework defect — the autoloop fitness gate — and S-Y2 is a
  semantic sub-sprint that must not burn more real-LLM tranches on an unreliable
  gate). Resumes the PAUSED M-Auto-4 "Autoloop Fitness Measurement Reliability"
  scope as an M-Auto-7 sub-sprint (human decision 2026-06-16: insert S-Y1.7
  rather than formally pause M-Auto-7 to resume full M-Auto-4). The S-Y2 / Sprint
  088 contract is HELD verbatim at compact/sprint-088-objective-HELD.md (+ its
  executable view compact/sprint-088-dev-prompt.md) and re-promoted on S-Y1.7
  close. Acceptance evidence (zero-LLM P0.7 retrospective calibration, CONDITIONAL
  PASS): docs/solutions/p07-calibration/{p07-findings.md,calibrate.py,
  calibration-results.json}. Source/design: docs/solutions/2026-06-12-sy2-autoloop-wording-analysis-and-zero-keep-root-cause.md §5 P0
  + docs/solutions/2026-06-15-p0-fitness-measurement-reliability-sprint-contract.md
  (the promoted draft). Human decisions 2026-06-16: (1) encode V3 = majority-flip
  TIER-S; (2) keep tier2 + shadow gates made noise-aware; cross-case stays
  count>=2.
---

# Sprint 091 / S-Auto-37 — Autoloop fitness-measurement reliability (P0)

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | `infra` (eval-framework: the autoloop 5-layer fitness gate / scoring in `autoloop/autoloop/scoring/`). Governs *which* `semantic_planner` skill-yaml candidates survive to the §4.1 human gate — hence §7 is filled despite the infra core. |
| **§7 stanza** | **REQUIRED** (the gate selects semantic candidates; influences a routing/keep decision) — see §7 below. |
| **Per-sub-sprint Codex (§4.3)** | **REQUIRED** — the new statistical decision surface on the fitness gate sits on the §1.7 "do not optimize visible eval at the cost of generalization" line + the §5.4 anti-误杀 floor (§4.3 trigger #3). Must complete **before S-Y2 Part C runs**. |
| **Re-bless** | **NONE.** Changes `autoloop/**` scoring/gate + `config.yaml` only (no `server/**`, no `eval_interactive/**` CaseSpecs/fixtures/simulator/judge). `scoring_code_baseline_sha` WILL change (the gate is scoring code), but no prepilot-baseline re-bless is required for this sprint's verdict — the acceptance oracle is the archived candidate data replayed offline. The knobs are confirmed on the next S-Y2 pilot run under the new gate (F5). |

## Goal

Replace the autoloop fitness gate's zero-tolerance majority-flip rule
(`anchor_outcome_max_drop_cases: 0` + the raw tier2 / shadow count gates) with a
**noise-aware, per-case-stability-tiered statistical rule** so the gate stops
discarding behaviour-neutral / on-target candidates on n=3–5 sampling noise
(measured ~92–95% false-discard for a neutral candidate — the PAUSED M-Auto-4
thesis, now directly evidenced) **while strictly preserving the §5.4 anti-误杀
floor and the §1.4 tier0 safety floor.** The rule is the V3 variant validated by
the P0.7 retrospective calibration.

## Why now (governance hook)

- **§5.8 framework-defect priority.** §3 layer = `infra`, scope = the eval
  framework (autoloop scoring/gate). By §5.8 this PREEMPTS the S-Y2 semantic
  pilot: S-Y2 Part C must not run further real-LLM tranches until this lands.
  Inserted as a pre-pilot blocker (parallel to S-Y1.5), slot **S-Y1.7**.
- **P0.7 conditional pass.** Evidence: `docs/solutions/p07-calibration/`. Floors
  (tier0 + anti-误杀 majority-flip) and the signal>noise direction validated on
  12 archived experiments; the literal P0.1 "any-attempt-fail" TIER-S rule was
  REJECTED (false-discard machine at n=5) for V3 majority-flip; precise TIER-N
  knobs are not certifiable at n=5 (F5) and are encoded as evidence-backed
  defaults confirmed on the first P0.4-sampled run.

## The encoded rule (V3) — normative spec

Evaluation stays **lexicographic** (first failing layer short-circuits). The
binding floors override the statistic.

### Floors (binding; unchanged or strengthened)

- **F0 — tier0 delta floor (Layer 0, UNCHANGED).** `_evaluate_layer0`'s
  delta-mode logic stays byte-for-byte: a `_TIER0_PY_FAMILY` check False in the
  candidate majority but True/unknown in the baseline for the same (suite,case)
  → discard. (Calibration: exclusively catches exp-67/68/73/74 escalation_
  compliance@cs11s01 — the genuine route-error class.)
- **FS — TIER-S anti-误杀 floor (NEW, the §5.4 floor).** Any **TIER-S** case
  (baseline pass_rate == 1.0 / stability `stable`; includes the anti-误杀
  control `cs_uc_a_generic_policy_question`, `uc_k_tech`, `cs066`, `cs01s01`)
  that **majority-flips to fail** in the candidate → discard. **NOT
  any-attempt-fail** (V1, rejected by P0.7-F1: a true-1.0 case shows 4/5
  ~20–25% of the time at n=5). (Calibration: catches exp-78's control 1.00→1/3,
  the real over-elicitation; does NOT fire on the 4/5 flakes V1 wrongly killed.)

### TIER classification (per baseline case, from the n=9/eff-11 baseline)

Consume the EXISTING `stability_thresholds` + `aggregate.classify_stability`
already on each baseline `aggregated.json` (P0.1 wiring — currently only the
re-bless pipeline reads them; the gate must too):

| tier | criterion | gate treatment |
|---|---|---|
| **TIER-S** | baseline pass_rate == 1.0 (all valid attempts pass) | FS floor: majority-flip → discard |
| **TIER-F** | baseline pass_rate == 0.0 | improvement-direction only; cannot regress below floor; cannot gate discard |
| **TIER-N** | 0 < baseline pass_rate < 1.0 (the bulk: 21/29 gating cases) | Beta-Binomial posterior (below) |

### TIER-N statistic (replaces the Layer-1 `max_drop_cases` rule)

Per TIER-N case, Jeffreys-prior Beta-Binomial posteriors with TOST margin
**δ = 0.10**:

```
p_base ~ Beta(0.5 + k_base, 0.5 + n_base - k_base)
p_cand ~ Beta(0.5 + k_cand, 0.5 + n_cand - k_cand)
P_regress = P(p_cand < p_base - δ)      # one-sided TOST lower
P_improve = P(p_cand > p_base + δ)      # one-sided TOST upper
```

Per-case classification: `P_regress ≥ 0.80` → **regressed**; `0.50 ≤ P_regress
< 0.80` → **ambiguous**; `< 0.50` → **non-regressed**. Symmetric on `P_improve`
for **improved**.

### Cross-case rule (Layer 1; count≥2 per human decision 2026-06-16)

Discard if **EITHER**: Benjamini-Hochberg FDR at **α = 0.10** (treating
`1 − P_regress` as quasi-p-values, joint with per-case `P_regress ≥ 0.80`) flags
≥1 case; **OR** ≥ **2** TIER-N cases have `P_regress ≥ 0.80`. (Calibration:
exp-69 = 3 regressed → discard; exp-79 = 1 regressed → released. count≥2 is the
exp-69-vs-exp-79 discriminator — the headline win.)

### Layer 2 (tier2 critical-flow) — RETAINED, made noise-aware (human C1)

Keep the layer, but **gate any mandatory-failure increase through the per-case
outcome posterior**: a tier2 mandatory-failure increase counts toward discard
only if the affected case's outcome posterior shows `P_regress ≥ 0.80`.
**Grounded outcome (verified against exp-66 data):** exp-66's historical tier2
3→4 is driven solely by `wmkb_uc_a_trader_flag_secondary_uc_h` (cand 2/5 vs base
0.727), `P_regress = 0.797` — just under 0.80 = AMBIGUOUS, not a supported
regression. So the noise-aware Layer-2 classifies **exp-66 as AMBIGUOUS, not
DISCARD** (the doc's "exp-66 must remain discarded" was a hard-count artifact —
an F5 knife-edge at 0.797 vs 0.80). The precise per-step→case attribution is a
wiring sub-decision (see §Carried decisions); the oracle test pins exp-66 =
AMBIGUOUS.

### Layer 4 (shadow) — RETAINED, made noise-aware (human C2)

Keep the firewalled aggregate-only gate, but **replace the raw
`shadow_max_drop_pct: 3.0` threshold with a noise-aware test** (Beta-Binomial
posterior on the shadow aggregate pass count, discard only on a
statistically-supported drop). **Grounded outcome:** exp-72's 3.9pp drop ≈ 0.78
of one shadow case (25.0→21.1% over ~20 comparable cases) — below any sane
noise-aware bar, so the test **releases exp-72** (matches §P0.7). Per-case shadow
detail stays firewalled from the meta-agent (program.md §3.B.8 / tier_evaluator
shadow firewall UNCHANGED). Oracle pins exp-72 = off-discard.

### Sample sizes (P0.4) + invalid-attempt accounting (P0.6c)

| Parameter | Current | New | Source |
|---|---|---|---|
| `fitness.samples_per_case` (default) | 3 | **5** | P0.4 |
| `fitness.pilot.primary_targets` samples | 3 | **11** | P0.4 (matches baseline n; makes lift detection feasible) |
| `fitness.anchor_outcome_max_drop_cases` | 0 | **removed/deprecated** (replaced by tiered rule) | P0.3 |
| TIER thresholds (δ, P_regress, P_ambig, α_FDR, count) | n/a | **0.10 / 0.80 / 0.50 / 0.10 / 2** | P0.2/P0.3 (defaults; F5) |
| `non_comparable_rate` per-suite summary + §5.9 preflight alert >20% | none | **add** (observational, non-gating) | P0.6c |

## Scope (numbered, dev-executable; all changes under `autoloop/**` + `config.yaml`)

1. **TIER classification + per-case (k,n) plumbing.** Extend `baseline_loader.py`
   (`SuiteSnapshot`) to expose per-case `(k_base, n_base, pass_rate,
   stability_class)` to the evaluator; the candidate `_CurrentSuite` already
   carries per-case `pass_rate`/`valid_attempts`. Add a `classify_tier()` helper
   consuming `stability_thresholds`.
2. **Beta-Binomial posterior module.** Add a dependency-free posterior helper
   (the autoloop env has no numpy/scipy) — reuse the validated math from
   `docs/solutions/p07-calibration/calibrate.py` (`betai` + stratified
   `posterior_regress_improve`); unit-test against known incomplete-beta values.
3. **Rewrite Layer 1 (`_evaluate_layer1`)** to: apply FS (TIER-S majority-flip
   floor), compute per-TIER-N posteriors, classify regressed/ambiguous/improved,
   apply the BH-FDR + count≥2 cross-case rule. Remove `anchor_outcome_max_drop_cases`.
4. **Make Layer 2 (`_evaluate_layer2`) noise-aware** per the C1 spec (gate
   mandatory-failure increases through the affected case's outcome posterior).
5. **Make Layer 4 (`_evaluate_layer4`) noise-aware** per the C2 spec (posterior
   on the shadow aggregate; firewall unchanged).
6. **Adapt Layer 3 (`_evaluate_layer3`)** improvement gate to the posterior
   "improved" classification (≥1 case `P_improve ≥ 0.80` OR a supported tier2
   reduction).
7. **Config (`config.yaml fitness`):** `samples_per_case` 3→5; add
   `pilot.primary_targets` oversampling to 11; remove `anchor_outcome_max_drop_cases`;
   add a `tier_decision` block (δ / thresholds / α_FDR / count). Comment every
   change with the P0.x rationale.
8. **Instrumentation (P0.6c):** add `non_comparable_rate` to the per-suite
   summary; add the §5.9 preflight alert at >20%; audit that TIMEOUT /
   `INVALID_INFRA_ERROR` exhausted-retry attempts route to `non_comparable`
   (excluded), not counted as failed. Observational only — does NOT gate.
9. **Acceptance oracle test (zero-LLM):** characterization test running the new
   `evaluate(...)` on the 12 archived candidate dirs
   (`autoloop/results/runs/exp-{66,67,68,69,71,72,73,74,75,77,78,79}/eval/`)
   against the prepilot baseline, asserting the **full V3 verdict matrix**:

   | exp | pinned verdict | mechanism |
   |---|---|---|
   | 67, 68, 73, 74 | **DISCARD** | F0 tier0 (escalation_compliance@cs11s01) |
   | 78 | **DISCARD** | FS TIER-S (anti-误杀 control 1.00→1/3 majority-flip) |
   | 69 | **DISCARD** | cross-case regressed (≥2 TIER-N at P_regress≥0.80) |
   | 71, 77, 79, 75 | **off-discard** | TIER-N statistic releases (≤1 regressed) |
   | 66 | **off-discard (AMBIGUOUS)** | tier2 driver wmkb P_regress=0.797 < 0.80 (C1; F5 knife-edge) |
   | 72 | **off-discard** | shadow drop 3.9pp ≈ <1 case, noise-aware shadow releases (C2) |

   The test is the executable spec; `calibrate.py` is the reference
   implementation the evaluator must agree with on this data. (`calibrate.py` as
   committed implements only the floors + Layer-1 statistic — it leaves exp-66
   keep-eligible and exp-72 shadow-discarded; the dev EXTENDS it to the C1/C2
   noise-aware Layer-2/Layer-4, at which point 66→ambiguous and 72→off-discard,
   and re-pins the matrix.)

## Hard fences / STOP conditions

- **No change outside `autoloop/**` + `autoloop/config.yaml`.** `server/**`,
  `eval_interactive/**` (CaseSpecs, fixtures, simulator, judge, scoring),
  baselines — all byte-identical.
- **The §5.4 anti-误杀 floor may only be STRENGTHENED, never weakened.** FS
  (TIER-S) + F0 (tier0) are the floors; the statistic NEVER overrides them.
  (Forbidden: using the noise-aware framing to let a genuine anti-误杀 control
  regression — like exp-78's 1/3 — pass.)
- **No semantic hardcode.** Stability-tiered statistic only; no keyword / regex /
  enum / per-UC / per-CaseSpec branch; no CaseSpec id in the gate logic.
- **No `current_eval_baseline.md` canonical flip** and no `baseline_dir` move.
- **Do NOT tune thresholds to force the doc's expected pattern on exp-66/72.**
  Those are C1/C2 wiring outcomes; whatever the principled noise-aware wiring
  decides is what the oracle pins (§1.6 — eval is evidence, not authority; do
  not p-hack the calibration).
- **STOP** and escalate to the human if: the C1/C2 wiring cannot preserve the
  exp-69-vs-exp-79 discrimination (count≥2) without breaking a floor; or the
  posterior implementation disagrees with `calibrate.py` on the archived data
  (they must agree — `calibrate.py` is the spec).

## Test / eval requirements

- **Acceptance = the §Scope #9 zero-LLM oracle test passes** + the V3 matrix is
  reproduced + the handoff records the C1/C2 resolution for exp-66/72.
- **autoloop pytest: no regression** (current 348; new tests add to it).
- **Java + UI baselines untouched** (no `server/**` / `ui/**` change).
- **Mocked/offline replay is acceptable as PRIMARY evidence here** — this is an
  `infra` scoring-logic change, NOT a prompt/behaviour change, so §5.7 (which
  bars mocked-LLM as evidence of a *behaviour* change) does not apply; the
  measured variable is the gate's arithmetic on fixed recorded data.
- **F5 power-ceiling (record in handoff):** the retrospective oracle certifies
  floors + direction + the C1/C2 wiring; the precise knobs (δ, 0.80, count) are
  CONFIRMED on the first S-Y2 pilot run under the new gate, not by this sprint.

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (eval-framework: the autoloop 5-layer fitness
gate / scoring in `autoloop/autoloop/scoring/`). The defect is the
zero-tolerance majority-flip rule's ~92–95% false-discard rate on n=3–5 sampling
noise. Secondary surface: the rule governs which `semantic_planner` skill-yaml
candidates reach the §4.1 human gate (hence §7 is filled, not claimed exempt).

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. It PRESERVES the
existing §1.4 tier0 safety floor (Layer 0, unchanged) and STRENGTHENS the §5.4
anti-误杀 floor into an explicit TIER-S majority-flip floor (FS).

**Semantic hardcode:** No semantic hardcode introduced. The change is a
stability-tiered Beta-Binomial / TOST / BH-FDR statistical decision rule applied
uniformly to all cases by their baseline stability tier — no keyword, regex,
enum, per-UC, or per-CaseSpec branch; no CaseSpec id in the gate. Defenses: the
oracle test pins behaviour to recorded data; the anti-误杀 control's
majority-flip is a hard discard; Codex §4.1 reviews the rule. Sunset plan: n/a
(no hardcode introduced).

**Generalization coverage:** retrospective oracle over **12** archived
experiments (exp-66..79 minus the 2 pre-eval-discarded) covering the four
families (RESOLVE-FAQ A / DISCOVER B / escalation_policy C / anti-误杀 control) —
floors + cross-case discrimination validated. Forward confirmation of the knobs
on the next S-Y2 pilot run at P0.4 sampling (n=5 default / n=11 primary).
Per-CaseSpec target/neighbor/negative/shadow counts: n/a — this is a
measurement-layer change, not a per-CaseSpec semantic change; coverage is the
experiment corpus, not a case family.

## Codex review plan (§4.3)

**Per-sub-sprint Codex REQUIRED** (trigger #3 — new statistical decision surface
on the fitness gate near the §1.7 / §5.4 line). Review the rule + the C1/C2
wiring + the oracle test under the §4.1 nine-question kernel; verdict to
`docs/codex-findings.md` (§4.2 header). Must complete **before S-Y2 Part C runs**.
Prompt: `compact/sprint-091-review-prompt.md` (deliver authors at dev close).

## Handoff requirements

`docs/sprints/sprint-091-handoff.md` must record: the implemented rule + final
parameter values; the oracle test's reproduced V3 matrix; the **C1 resolution**
(how Layer-2 noise-awareness was wired + exp-66's resulting verdict) and the
**C2 resolution** (shadow noise-aware test + exp-72's verdict); the
`non_comparable_rate` instrumentation + the TIMEOUT-routing audit result
(P0.6c); confirmation no floor was weakened; the `scoring_code_baseline_sha`
delta + the no-re-bless rationale; and the **F5 power-ceiling** statement.

## Commit discipline

Stage explicitly by file (NO `git add -A` — autoloop dirty-index hazard). All
commits `autoloop/**` + `config.yaml` only. Deliver close-bundle artefacts
(`sprint_objective.md`, `10-handoff.md`, `action_bank.md`, handoff) bundled by
the human at close.

## Carried decisions (resolve in-scope, with Codex)

- **C1 wiring** — the precise per-step→case attribution for Layer-2
  noise-awareness (how a tier2 mandatory-failure increase maps to "which case's
  outcome posterior gates it"). Human chose "keep tier2, noise-aware"; the exact
  statistic is a dev+Codex wiring choice pinned by the exp-66 oracle
  (exp-66 → AMBIGUOUS).
- **C2 wiring** — the exact noise-aware shadow test (aggregate Beta-Binomial
  posterior vs a significance-corrected threshold). Human chose "keep shadow,
  noise-aware"; pinned by the exp-72 oracle (exp-72 → off-discard).
- **F5** — knob confirmation deferred to the first post-merge S-Y2 pilot run.

## On close → resume S-Y2

S-Y1.7 close re-promotes the HELD S-Y2 contract
(`compact/sprint-088-objective-HELD.md`, executable view
`compact/sprint-088-dev-prompt.md`) back into `docs/sprint_objective.md`; S-Y2
Part C then runs under the new fitness gate (the F5 knob-confirmation run).
