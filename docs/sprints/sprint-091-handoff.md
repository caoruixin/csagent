---
title: Sprint 091 / S-Auto-37 (M-Auto-7 S-Y1.7) handoff — noise-aware fitness gate (V3)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-16
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  infra (eval-framework) sub-sprint, PREEMPTS the S-Y2 pilot per §5.8. Replaces
  the autoloop fitness gate's zero-tolerance majority-flip / max-drop count
  rules with a stability-tiered Beta-Binomial (TOST + BH-FDR) rule (V3), so the
  gate stops false-discarding behaviour-neutral candidates on n=3-5 sampling
  noise — while STRENGTHENING the §5.4 anti-误杀 floor and preserving the §1.4
  tier0 floor byte-for-byte. Changes autoloop/** + autoloop/config.yaml ONLY.
  NO re-bless (acceptance oracle = archived candidate replay, zero-LLM).
  Acceptance = the §Scope #9 oracle (autoloop/tests/test_fitness_gate_oracle.py)
  reproduces the re-pinned V3 matrix; full autoloop pytest green (387, was 348).
---

# Sprint 091 / S-Auto-37 — S-Y1.7 noise-aware fitness gate (V3) handoff

## TL;DR

The P0.7 retrospective calibration (`docs/solutions/p07-calibration/`) found the
old gate (`anchor_outcome_max_drop_cases: 0` + raw tier2/shadow count gates)
false-discards a behaviour-neutral candidate ~92-95% of the time at n=3-5 — it
selects on noise, not quality. This sub-sprint encodes the human-decided V3 rule
(majority-flip TIER-S floor + TIER-N Beta-Binomial posterior + count≥2 cross-case
+ noise-aware C1 tier2 + C2 shadow). The acceptance oracle replays the 12 archived
M-Auto-7 pre-pilot candidate experiments through the production
`tier_evaluator.evaluate(...)` and reproduces the re-pinned V3 matrix exactly;
the production evaluator agrees with the extended `calibrate.py` reference (V5) on
every experiment.

## Implemented rule + final parameter values

Lexicographic 5-layer evaluator (first failing layer short-circuits; floors
override the statistic):

- **Layer 0 — tier0 safety floor: UNCHANGED, byte-for-byte** (`_evaluate_layer0`).
  F0: a `_TIER0_PY_FAMILY` check False in candidate-majority but True/unknown in
  baseline → discard.
- **Layer 1 — FS anti-误杀 floor + TIER-N posterior cross-case rule.**
  - **FS (binding floor, the §5.4 anti-误杀 floor, STRENGTHENED):** any baseline
    pass_rate==1.0 (TIER-S) case that **majority-flips to fail** (candidate
    k/n ≤ 0.5) → discard. Suite-wide (incl. shadow), like F0. **Majority-flip,
    NOT any-attempt-fail** (F1: any-fail itself false-discards a true-1.0 case
    showing 4/5 ~20-25% of the time at n=5).
  - **TIER-N (0 < baseline pass_rate < 1):** Jeffreys Beta-Binomial
    `Beta(0.5+k, 0.5+n-k)`, TOST margin δ; per-case `P_regress ≥ 0.80` →
    regressed. **Cross-case discard** if BH-FDR(α) flags ≥1 (joint with the
    per-case 0.80 floor) OR ≥ `cross_case_count` regressed cases.
  - **TIER-F (baseline pass_rate==0.0):** improvement-direction only — cannot
    regress (P_regress vs a negative margin ≈ 0); credited to the Layer-3
    improved set when `P_improve ≥ 0.80` (the autoloop's job is fixing hard-0
    cases; the S-Y2 pilot moves a TIER-F primary target).
- **Layer 2 — C1 noise-aware critical-flow.** A tier2 mandatory-failure increase
  is credited only via the affected case's OUTCOME posterior; the SAME count≥
  rule applies over the union of (outcome-regressed) ∪ (tier2-strong) cases. A
  single strong critical-flow regression is released (count<2); a knife-edge
  increase (0.50 ≤ P_regress < 0.80) holds the candidate AMBIGUOUS but does not
  gate.
- **Layer 3 — improvement.** ≥1 case with `P_improve ≥ 0.80` (TIER-N or TIER-F)
  OR a supported tier2 mandatory-failure reduction ≥ `improvement_min_cases`.
- **Layer 4 — C2 noise-aware shadow.** Beta-Binomial posterior on the shadow
  aggregate majority-pass count over comparable cases; discard only on a
  statistically-supported drop (`P_regress ≥ 0.80`). Per-case shadow firewall
  UNCHANGED (`ShadowAuditDetail`, audit=True only).

**Final parameter values** (`config.yaml fitness.tier_decision`, evidence-backed
defaults — see F5 ceiling): δ=0.10, p_regress=0.80, p_ambiguous_low=0.50,
α_FDR=0.10, cross_case_count=2. Sampling (`fitness`): `samples_per_case` 3→**5**;
`fitness.pilot.primary_targets_samples`=**11**. Removed:
`anchor_outcome_max_drop_cases`, `shadow_max_drop_pct`.

Math kernel: new `autoloop/autoloop/scoring/posterior.py` (`betai` / `betainv` /
`posterior_regress_improve` / `bh_flag`), ported verbatim from `calibrate.py`,
dependency-free (no numpy/scipy), unit-tested vs known incomplete-beta values
(`tests/test_posterior.py`). Per-case (k,n,tier) plumbing added to
`baseline_loader.CaseBaselineStat`; `aggregate.classify_tier` added.

## Oracle test — reproduced V3 matrix (`tests/test_fitness_gate_oracle.py`)

Production `evaluate(...)` over the 12 archived candidate dirs vs the prepilot
baseline (`m-auto-7-prepilot-baseline-20260608`):

| exp | decision | classification | mechanism |
|---|---|---|---|
| 67, 68, 73, 74 | DISCARD | discard | F0 tier0 `escalation_compliance@cs11s01` |
| 78 | DISCARD | discard | FS TIER-S anti-误杀 control `cs_uc_a_generic_policy_question` 1.00→1/3 |
| 69 | DISCARD | discard | cross-case regressed (union count 3 ≥ 2) |
| 71 | keep | ambiguous | 1 strong regression released (count<2); `uc_g_gdpr` tier2 knife-edge |
| 77, 79 | keep | keep_eligible | TIER-N releases + posterior improvement |
| 75 | keep | ambiguous | TIER-N release; `wmkb` tier2 knife-edge |
| 66 | keep | ambiguous | C1: tier2 3→4 driver `wmkb` P_regress=0.797 < 0.80 |
| 72 | keep | ambiguous | C2: shadow 25%→21.1% (P_regress=0.314) released |

TIER classification over the 29-case gating set reconciles exactly with
calibrate.py: TIER-S=3 (`cs066`, `cs_uc_a_generic_policy_question`,
`anchor_outcome_uc_k_tech`), TIER-N=21, TIER-F=5.

The extended reference `calibrate.py` (variant **V5_EXTENDED**) reproduces the
SAME matrix independently (run `python3 docs/solutions/p07-calibration/calibrate.py`).

## C1 resolution (Layer-2 noise-aware) + exp-66 verdict

C1 gates a tier2 mandatory-failure increase through the affected case's outcome
posterior, applying the count≥2 cross-case rule over (outcome-regressed) ∪
(tier2-strong). Mathematically a tier2-strong case (P_regress ≥ 0.80) is already
an outcome-regressed case, so on in-scope (TIER-N) data Layer 2 never tightens
beyond Layer 1; its independent reach is a TIER-S/TIER-F case with a tier2
increase that Layer 1's TIER-N loop did not cover.

- **exp-66 → keep / AMBIGUOUS.** Its tier2 3→4 is driven solely by
  `wmkb_uc_a_trader_flag_secondary_uc_h` (baseline 8/11=0.727, candidate 2/5),
  `P_regress = 0.797` — just under 0.80 → the noise-aware Layer-2 does NOT gate
  it, but the ambiguous-band increase classifies the candidate AMBIGUOUS (held
  for stage-2), not a clean keep. The doc's "exp-66 must remain discarded" was a
  hard-count artifact (F5 knife-edge at 0.797 vs 0.80), not a noise-aware truth.
- The same `wmkb` 0.797 knife-edge appears in exp-72 and exp-75 (all three
  escalation/grounding candidates perturb wmkb), so those are also classified
  AMBIGUOUS — the honest, principled output (decisions all keep). exp-71's
  ambiguity is `uc_g_gdpr` (P_regress=0.682).

## C2 resolution (shadow noise-aware) + exp-72 verdict

C2 replaces the raw `shadow_max_drop_pct: 3.0` count gate with a Beta-Binomial
posterior on the shadow aggregate majority-pass count over comparable cases
(δ/p_regress reused from `tier_decision`). Per-case shadow firewall unchanged.

- **exp-72 → keep.** Shadow dropped 25.0% (5/20) → 21.1% (4/19), a 3.9pp ≈ <1
  case wobble; `P_regress = 0.314` ≪ 0.80 → released (matches §P0.7). The old
  3pp count gate discarded it; the noise-aware test correctly does not.

## P0.6c instrumentation + TIMEOUT-routing audit

- **`non_comparable_rate`** per-suite summary added to the verdict
  (`tier_breakdown["non_comparable_rate"]`, observational). New §5.9 preflight
  alert `preflight.check_non_comparable_rate(config, results_dir, threshold=0.20)`
  warns when any suite exceeds 20% non-comparable (degraded measurement
  substrate). Standalone (NOT in the default 6-check `run_preflight` gate — it
  needs a sampled results dir; invoked by the §5.9 pre-flight pass).
- **TIMEOUT-routing audit (observational, clean):** across the 12 archived dirs,
  all **179** attempts with `invalid_reason='infra_error'` are `valid=False`
  (0 invariant violations) → excluded from the (k,n) vote → route to
  `non_comparable`, NEVER counted as a failed draw. The 95 `valid=True` attempts
  that carry a `TIMEOUT` failure_tag have NO `invalid_reason` — they are complete,
  provider-comparable draws (a turn-level timeout the session recovered from) and
  are correctly counted. The authoritative routing signal is the eval_runner's
  `valid`/`invalid_reason`, not the broad failure_tags heuristic.

## No floor weakened (confirmation)

- **F0 tier0:** `_evaluate_layer0` is byte-for-byte unchanged; exp-67/68/73/74
  still discard on `escalation_compliance@cs11s01`.
- **FS anti-误杀:** STRENGTHENED into an explicit suite-wide TIER-S majority-flip
  floor. exp-78's anti-误杀 control (1/3) still discards; the 4/5 flakes
  (exp-77/79) correctly do NOT trip it (any-fail would have false-discarded
  them). The floor only ever STRENGTHENS — the statistic never overrides it.
- exp-69-vs-exp-79 discrimination preserved: count≥2 separates exp-69 (3 strong
  regressions → discard) from exp-71/79 (1 → release).

## scoring_code_baseline_sha delta + no-re-bless rationale

- `_SCORING_CODE_FILES` widened 5→6: `posterior.py` joins (it produces the
  posteriors the gate consumes, same rationale as `aggregate.py` in S-Auto-16;
  leaving it out would be a drift hole). `eval_runner.py` byte-identical.
- SHA: `0d86b08f…` (pre-S-Y1.7) → **`f2f983cc86e7d29e18d4659da3a317131eecd624df14a78ce26286f30d590d79`**.
  Recompute: `uv run --directory autoloop python -c "from autoloop.scoring.gaming
  import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"`.
  `scoring_code_drift` check returns 0 flags against the updated value.
- **NO re-bless.** This is `infra` scoring-logic on FIXED recorded data; the
  acceptance oracle is the archived candidate replay, not a new real-LLM
  baseline. `baseline_dir` and `docs/current_eval_baseline.md` are UNCHANGED.

## F5 power ceiling (scope honesty)

The exp-66..79 data is itself in the low-n regime (candidate n=5, TIMEOUT-shrunk
to 3-4) P0.4 calls unreliable. This oracle certifies (i) the floors (tier0 + FS
anti-误杀 majority-flip), (ii) the qualitative signal>noise improvement, and
(iii) the C1/C2 noise-aware wiring. It does **NOT** certify the precise TIER-N
knobs (δ=0.10, the 0.80 threshold, count≥2) at n=5 — exp-66 and exp-79 are
statistically indistinguishable at this n (each has one case at P_regress≈0.8-0.9).
The knobs are evidence-backed defaults **confirmed on the first S-Y2 pilot run
under the new gate** (samples_per_case=5 / primary 11), NOT by this zero-cost pass.

## Test / eval evidence

- **Acceptance (primary, zero-LLM):** `tests/test_fitness_gate_oracle.py` (22
  assertions) reproduces the V3 matrix on the production `evaluate(...)`. Mocked/
  offline replay is PRIMARY evidence here — this is `infra` scoring on FIXED
  recorded data, not a prompt/behaviour change, so §5.7 does not apply.
- **autoloop pytest:** 387 passed (baseline 348 + new posterior/oracle/V3 tests;
  no regression). `posterior.py` unit-tested vs known incomplete-beta values.
- Java + UI untouched (no `server/**` / `ui/**`). `git diff --stat` confirms only
  `autoloop/**` + `autoloop/config.yaml` in the committable scope.

## Hard-fence compliance

- Only `autoloop/**` + `autoloop/config.yaml` changed in the committed scope.
  `server/**`, `eval_interactive/**` (CaseSpecs/fixtures/simulator/judge/scoring),
  and all baseline dirs are byte-identical.
- No semantic hardcode: stability-tiered Beta-Binomial / TOST / BH-FDR applied
  uniformly by baseline tier; no keyword/regex/enum/per-UC/per-CaseSpec branch;
  no CaseSpec id in the gate logic (case ids appear only in data-driven discard
  reasons, as the pre-existing Layer-0 floor already does).
- No `current_eval_baseline.md` flip; no `baseline_dir` move.
- `docs/solutions/p07-calibration/calibrate.py` extension (V5) + the handoff are
  docs close-bundle artefacts (human-bundled at close), NOT in the autoloop commit.

## Codex review plan (per-sub-sprint, REQUIRED — §4.3 trigger #3)

Review the V3 rule + C1/C2 wiring + the oracle under the §4.1 nine-question
anti-hardcode kernel; verdict to `docs/codex-findings.md` (§4.2 header). Must
complete before S-Y2 Part C runs.

## §7 Layer-classification + anti-hardcode stanza

- **Target failure layer:** `infra` (eval-framework: the autoloop 5-layer fitness
  gate / scoring). Defect = the zero-tolerance rule's ~92-95% false-discard rate
  on n=3-5 noise.
- **Tier-0 invariant:** Adds none. PRESERVES the §1.4 tier0 floor (Layer 0
  unchanged) and STRENGTHENS the §5.4 anti-误杀 floor into the explicit TIER-S
  majority-flip floor (FS).
- **Semantic hardcode:** None (stability-tiered statistic only).
- **Generalization coverage:** retrospective oracle over 12 archived experiments
  (four families: RESOLVE-FAQ A / DISCOVER B / escalation_policy C / anti-误杀
  control); floors + cross-case discrimination + C1/C2 validated. Forward knob
  confirmation on the next S-Y2 pilot run at P0.4 sampling.
