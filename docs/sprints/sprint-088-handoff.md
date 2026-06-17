---
title: Sprint 088 / S-Auto-33 (M-Auto-7 S-Y2) Part C handoff — CS4 entity-context autoloop pilot (CORE GATE)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-06-17
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Part C (autoloop authors → human reviews → merge) dev-side record. The pilot
  RAN end-to-end (-n 3 real-LLM, 0 infra errors); steering STRONG PASS
  (full_on_gap_hit_rate 66.7%, up from Run-1's 0%); 0 keeps; PRIMARIES did NOT
  flip. Verdict: §3.4 hand-authored fallback (VALID-ALTERNATE per milestone
  §5.1.5), NOT extend -n 16 (§11.3 strong-pass + 0-keep branch). Part D
  (close re-bless + per-sub-sprint Codex + close) NOT started — pending the
  §3.4 fallback decision + human go-ahead. No re-bless in Part C
  (scoring_code_baseline_sha unchanged).
---

# Sprint 088 / S-Auto-33 — S-Y2 Part C handoff (CS4 entity-context autoloop pilot)

## TL;DR

The CS4 entity-context autoloop pilot **ran end-to-end** — the M-Auto-7 CORE
GATE is **MET** (milestone §5.1.5: "the pilot RUNNING is the gate"). Steering
is a **STRONG PASS** (`full_on_gap_hit_rate = 66.7%`, a clean reversal of
Run-1's 0%), confirming the S-Y1.5 P0-C steering and the S-Y1.5b/c/d
anti-hardcode detector fix work as intended. But **0 of 3 candidates were
kept** and **neither Tier-1 primary flipped**. Per the §11.3 calibrated
decision rule (≥50% strong-pass + 0 keep), the verdict is **§3.4 hand-authored
fallback** (a VALID-ALTERNATE outcome), **not** extend `-n 16`.

---

# Run-3 (2026-06-16) — exp-82 / exp-83 under the S-Y1.7 V3 gate — human §4.1 review

> **The rest of this document (from "## Run parameters" down) is the Run-2
> record** (exp-77/78/79, 2026-06-12, OLD zero-tolerance fitness gate, keep=0/3,
> verdict "§3.4 hand-authored fallback"). Run-3 below **supersedes the §3.4
> hand-authored-fallback plan**: a fresh pilot under the new **S-Y1.7 noise-aware
> V3 fitness gate** produced two autoloop-authored KEEPS. The §3.4 fallback is no
> longer the path; these two candidates are now the merge question.

## TL;DR (Run-3)

Pilot **keep=2 / discard=2 / error=0** in ~2.3 h (~33–38 min/exp; the ~8×
speedup from the sonnet proposer + suite-parallelism-4 held; 0 `APITimeoutError`).
Both discards were the `tier0_escalation_compliance@cs11s01` floor firing
correctly. The two keeps are **principle-level `$.procedure` edits**, one per CS4
surface, both clean on the automated propose-stage checks. The **human §4.1
nine-question anti-hardcode review (this section) is the binding gate** — neither
candidate has been merged (per the human's instruction #3).

- **exp-82** — `resolve_faq_grounded_answer.yaml` `$.procedure` (RESOLVE-FAQ surface).
- **exp-83** — `discover_triage.yaml` `$.procedure` (DISCOVER surface).

Both live on forensic branches `autoloop/exp-82` / `autoloop/exp-83`
(merge-base `1cb6c500` = current HEAD). **Nothing merged.**

## What actually changed (YAML-aware field diff)

The git diffstats (exp-82 +174/−128; exp-83 +103/−75) are **YAML reserialization
noise** — a round-trip re-indented every list and re-quoted every string. A
field-level YAML compare (base `1cb6c500` vs each branch) shows **exactly one
field changed in each: `$.procedure`** — a pure append. Every other field
(`tools_required`, `guardrails`, `grounding_instruction`, `escalation_policy`,
`critical_steps`, `valid_terminal_outcomes`, `state_inheritance`) is **byte-for-byte
value-identical**. Tool schema, capability boundary, and grounding/safety floors
are therefore provably untouched (kernel Q7 ✓).

- **exp-82 appended** (to RESOLVE-FAQ `$.procedure`): when the session carries
  listing context, *anchor the `search_knowledge` query and grounded answer on
  the listing's observable state*; if listing context is present but the user
  gave no ad-id, *proceed on the available context rather than blocking*, and
  invite the ad-id only if further lookup is needed.
- **exp-83 appended** (two paragraphs to DISCOVER `$.procedure`): an
  "ad-identifier-absent posture" (don't defer classification just because no
  ad-id is present — *classify promptly toward the ad-visibility / ad-status FAQ
  UC*) and a "loaded-listing context guidance" (use projection-carried
  listing_context as a classification anchor → higher-confidence UC-A
  classification).

## §4.1 nine-question anti-hardcode verdicts

Automated propose-stage gates (recorded in `experiments.jsonl`) for BOTH:
`sandbox=ACCEPT` (1 whitelisted path), `content_validator=PASS`,
`anti_hardcode detector=PASS` (rule_id null, placeholder false),
`flag_for_codex=false`. Those are the rule-based pre-eval gates; the
nine-question kernel review (`docs/current/anti-hardcode-review-kernel.md`) is
below.

### exp-82 — verdict: **`approve`** (not a semantic hardcode)

| Q | Finding |
|---|---|
| Q1 keyword/regex/if-else/enum/UC-matrix? | **No.** Principle-level narrative; the one conditional ("if listing context present but no ad-id → proceed") is keyed on **observable projection state**, not a user-utterance keyword or a UC enum. |
| Q2 Tier-0 justification needed? | N/A — no hardcode to justify. |
| Q3 could be a soft projected signal? | It **already consumes** a projected soft signal (`listing_context`) and tells the LLM how to reason over it — the preferred direction, not a Java/prompt branch. |
| Q4 encodes eval case text / CaseSpec id / trace phrasing? | **No.** Abstract phrasing ("ad's current status, category"); no case ids, no quoted user phrases (grep-verified). |
| Q5 moves ownership LLM→Java? | **No** — stays in the prompt; expands what the LLM reasons over. |
| Q6 if-else block vs principle/observable-state? | **Observable-state / principle altitude.** Mildly imperative ("anchor", "proceed") but not an utterance if-else. |
| Q7 preserves schema / capability / PII / grounding floor? | **Yes — verified value-identical** (see field diff); the text reinforces the `search_knowledge → grounded answer` path. |
| Q8 generalization coverage? | Target + neighbor + negative across `anchor_outcome`+`bad_cases`+ held-out `shadow`. (Caveat: under-powered at n≈5 — see OQ-A.) |
| Q9 sunset plan if temporary? | N/A — principle-level prompt edit, not a temporary hardcode; rollback = don't merge. |

### exp-83 — verdict: **`approve` on strict anti-hardcode grounds, but flagged as the WEAKER candidate — do NOT seed/merge**

Strict kernel pass: no new keyword/regex/enum/case-text; only `$.procedure`
changed; floors preserved (Q7 ✓). But two flags, both **§5-acceptance**, not
§4.1-hardcode:

1. **Q4/Q6 lean (soft):** more prescriptive than exp-82 — it steers toward a
   *named UC outcome* ("classify promptly toward the ad-visibility / ad-status
   FAQ UC") and its phrasing ("why it is not performing, what happened to it,
   whether it is visible") edges closer to paraphrasing the target cases'
   question shapes. Still principle-level in the file's existing Sprint-7 idiom,
   so not a reject — but it is the over-steering end of the band.
2. **Binding anti-误杀 erosion (the decisive flag):** the anti-误杀 control
   `cs_uc_a_generic_policy_question` **held 1.0 (5/5) under exp-82** but
   **eroded to 0.6 (3/5) under exp-83** — and the mechanism is confirmed in the
   attempts: a1 failed on `L2_GATE:correct_uc` (a **UC misclassification**), the
   exact over-steering signature. The Run-2 handoff already named this control
   "the binding anti-误杀 gate" and steered fixes toward the RESOLVE-FAQ surface,
   away from `discover_triage` (Run-2 §"Signals", lines 119–125). exp-83 is the
   `discover_triage` edit that trips it.

## Per-case fitness (this run; base from the n=11 pre-pilot baseline)

`★`=primary target, `⛨`=anti-误杀 control, `▷`=tier-2 neighbor. cand n is the
draw count actually achieved this run (NOT 11 — see OQ-A).

| case | role | BASE | exp-82 | exp-83 |
|---|---|---|---|---|
| `cs_uc_a_no_ad_id_ad_specific` | ★ | 0/11 = 0.00 | **1/4 = 0.25 ↑** | 0/5 = 0.00 — |
| `cs_uc_a_loaded_listing` | ★ | 1/11 = 0.09 | 0/5 = 0.00 ↓ | **1/5 = 0.20 ↑** |
| `cs_uc_a_generic_policy_question` | ⛨ | 1.00 | **1.00 ✓ HELD** | **0.60 ⚠ ERODED** |
| `cs_uc_a_lookup_failed` | ▷ | 1/11 = 0.09 | 2/5 = 0.40 ↑ | 2/5 = 0.40 ↑ |
| `cs_uc_fp_loaded_moderation` | ▷ | 0/11 = 0.00 | 1/5 = 0.20 ↑ | 1/5 = 0.20 ↑ |

**Neither primary flips to majority-PASS.** exp-82 lifts `no_ad_id` (0→0.25) but
regresses its own `loaded_listing` (0.09→0.00); exp-83 lifts `loaded_listing`
(0.09→0.20) but leaves `no_ad_id` dead-flat. So the two surfaces are
**complementary** — each moves the primary the other can't.

**Neighbor regressions the V3 gate tolerated** (high `P_regress`, kept anyway):
- exp-82: `anchor_uc_e_promotion` 0.89→0.60 (Preg 0.773), `cs014_uc_c_faq_miss`
  0.82→0.60 (Preg 0.678), `wmkb_uc_a` 0.73→0.60 (Preg 0.536).
- exp-83: `anchor_uc_fp_removed` 0.70→0.25 (**Preg 0.877**), `anchor_uc_h_appeal`
  0.27→0.00 (Preg 0.771), `cs015_uc_fp_appeal` 0.36→0.20 (Preg 0.57).

These are TIER-N at n=5 (wide CIs), which is why the noise-aware V3 gate did not
discard — but they are real and need n=11 power to adjudicate (OQ-A).

## DECISION — exp-82 is the PREFERRED CANDIDATE / refinement seed (NOT merged)

- **§4.1 (anti-hardcode): exp-82 = `approve`.** Clean principle-level
  projection-consuming edit on the on-gap RESOLVE-FAQ surface; all floors
  provably preserved.
- **§5 (acceptance / merge): HOLD.** No primary flips to majority-PASS, and the
  n≈5 sampling under-powers both the lifts and the neighbor regressions. Per the
  human's instruction #3, **nothing is merged.**
- **exp-82 is recorded as the preferred candidate and the refinement seed** for
  the next iteration: it held the binding anti-误杀 control at 1.0, sits on the
  surface Run-2 already identified as the right one, and produced the only lift
  on the hard `no_ad_id` primary. A refinement should **graft exp-83's
  `loaded_listing` lift onto exp-82's RESOLVE-FAQ surface** (the two are
  complementary) **without** exp-83's `discover_triage` UC-steer, which is what
  eroded the anti-误杀 control.
- **exp-83 is NOT recommended as a seed:** the anti-误杀 erosion (1.0→0.6 via a
  `correct_uc` misclassification) + the severe `uc_fp_removed` neighbor
  regression are the over-elicitation signature the Run-2 handoff warned the
  DISCOVER surface would carry.

## ⚠️ UPDATE 2026-06-17 — exp-82 targeted re-eval at n=13 OVERTURNS the "preferred seed" conclusion

After wiring `primary_targets_samples` (OQ-A), exp-82's **identical candidate**
was re-evaluated through the real pipeline (proposer pinned to exp-82's stored
hypothesis; apply → eval → V3 gate; `iteration_id=exp82-reval`, ~46 min). The
primaries now draw **n=13** (3 uniform + 2 retry + 8 oversample). Result:

| metric | original exp-82 (06-16) | **re-eval at n=13 (06-17)** |
|---|---|---|
| `cs_uc_a_no_ad_id_ad_specific` | 1/4 = 0.25, **P_improve 0.768** | **0/13 = 0.0, stable_fail, P_improve 0.059 / P_regress 0.084** |
| `cs_uc_a_loaded_listing` | 0/5 = 0.0 | **0/13 = 0.0, stable_fail, P_improve 0.022 / P_regress 0.397** |
| `cs_uc_a_generic_policy_question` (⛨, n=5) | 1.00 HELD | **0.75 flaky_pass** (not oversampled; soft erosion in flake band) |
| `anchor_uc_e_promotion` (▷, n=5) | 0.60 (Preg 0.773) | **0.60 flaky_pass** — regression reproduces |
| timeout / non-comparable | — | **0 timeouts; 0 non-comparable in bad_cases/anchor; 3 in shadow; 20/51 flaky** |

**Conclusion — exp-82's apparent primary lift was small-sample noise.** At n=4
the `no_ad_id` "lift" scored `P_improve=0.768`; at n=13 it collapses to **0.059**
(0/13). `loaded_listing` is `P_improve=0.022` with a mild regression lean
(0.091→0.0, P_regress 0.397, below the 0.8 gate). **Neither primary moves at
proper power.** The §4.1-era "preferred candidate / refinement seed" status —
which leaned on the 0→0.25 `no_ad_id` lift — **does not survive n=13 and is
withdrawn.** The RESOLVE-FAQ *surface* may still be correct, but exp-82's
*specific edit* does not move the targets; a refinement must find a genuinely
different edit, not graft onto exp-82's.

**Verdict instability (separate, important):** the re-eval **discarded at tier-0**
(`escalation_compliance@cs11s01_uc_d` + `cs40s02_uc_k`, neither in the
baseline-ignore list) — yet the **same candidate was a KEEP in its original
run**. These tier-0 escalation cases are **run-to-run flaky** (stable within a
run, variable across runs), so the zero-tolerance tier-0 delta gate flips
keep↔discard on the same candidate. This is the dominant measurement-stability
risk surfaced by this investigation (see OQ-E).

## Open questions (Run-3)

- **OQ-A — `primary_targets_samples=11` wiring: FIXED (re-run pending).** Root
  cause: `config.fitness.pilot.primary_targets_samples: 11` was read **nowhere**
  in `eval_runner.py`; the n-loop applied a single uniform `n =
  fitness.samples_per_case` (=3, +retries to ~5) to **every** case, so the two
  primaries got n=4/5, not 11 — flip-detection was under-powered and "no flip"
  partly reflected measurement, not the candidate. **Fix landed** (human
  instruction #5): `eval_runner._run_majority_passes` now runs
  `primary_targets_samples − samples_per_case` EXTRA primary-ONLY passes after
  the uniform passes, appending the draws to the owning suite's per-case records
  (aggregation already supports a variable per-case n). Fence-clean — uses the
  existing `eval-interactive run --path` (dir form), no new CLI flag; the
  mini-suite scratch dir is named after the owning suite so the oversampled
  draws carry `source_suite=bad_cases` and are scored on the SAME
  human-judgment path as the base draws (commensurable). Validated: 3 new unit
  tests (lift-only-primaries + 2 no-op guards) + full autoloop suite **393
  passed**. **Live-validated 2026-06-17**: smoke `-n 1` (exp-85) and the
  exp82-reval re-eval both drove the two primaries to **n=13 comparable draws**
  (5 base + 8 oversample), mini-suite built + symlinked correctly, no crash.
  Committed `192e1ae4` (substrate). **Full pilot tranche still HELD** per the
  human's instruction — only the wiring smoke + the single targeted exp-82
  re-eval were run.
- **OQ-B — the V3 gate under-protects the anti-误杀 control at n=3.** exp-83's
  control erosion 1.0→0.6 did **not** trip the TIER-S floor because 3/5 is still
  a majority-pass; at n=3 a 1.0→0.6 drop sits inside the noise band the
  majority-flip rule tolerates. `primary_targets_samples` oversamples
  **primaries only** — `anti_kill_control` stays at n=3 and remains
  under-protected even after OQ-A is wired. Candidate follow-up: oversample (or
  apply a stricter floor to) `pilot.anti_kill_control` too. (The exp-82 re-eval
  corroborates: control read 0.75 flaky_pass at n=5 — too few draws to judge.)
- **OQ-C — tier-0 discard short-circuits the tier-1 verdict breakdown.** When a
  candidate fails the tier-0 gate, `tier_evaluator` never runs the tier-1
  posterior stage, so `verdict.tier_breakdown.tier1_outcome.tier_n.per_case` is
  **empty even though the raw eval results (incl. the n≥11 primary draws) exist
  on disk**. Both exp-85 (smoke) and exp82-reval hit this — the per-case
  posteriors had to be recomputed manually from the raw `results.json` +
  baseline via `tier_evaluator.posterior_regress_improve`. Follow-up: persist
  the tier-1 per-case table even on a tier-0 discard (observation-only), so a
  reviewer gets the full picture without a manual recompute.
- **OQ-D — primary oversampling runs BEFORE the tier-0 reject is known →
  wasted eval.** The 8 primary-only oversample passes execute during the eval
  phase, but the tier-0 gate (which can discard the candidate outright) is only
  evaluated afterward. exp82-reval spent the full ~46 min — including the 8
  oversample passes — on a candidate that tier-0 then discarded. Follow-up:
  gate-order or short-circuit so the expensive primary oversampling is skipped
  (or deferred) when a cheap tier-0 pre-check already condemns the candidate.
- **OQ-E — tier-0 escalation_compliance cases are run-to-run FLAKY and flip the
  keep/discard verdict.** exp-82 was a KEEP in its original run and a DISCARD on
  re-eval of the **identical** candidate, solely because
  `escalation_compliance@cs11s01_uc_d` (and `cs40s02_uc_k`) failed tier-0 this
  run but not the original — and neither is in the baseline-ignore list. With a
  zero-tolerance tier-0 delta gate, a single such across-run flake decides
  keep↔discard regardless of the candidate's actual merit. This is the dominant
  measurement-stability risk for the whole pilot; it likely needs the same
  noise-aware treatment (sampling + posterior floor) the tier-1 gate already has,
  or a stability re-bless of the cs11s01 / cs40s02 escalation cases.

## Hard-fence + firewall compliance (Run-3)

- Both candidates edited exactly one allowed `$.procedure` field on an allowed
  skill YAML (sandbox `ACCEPT`, 0 rejected paths). No Java/runtime,
  CaseSpec/fixtures/scoring, or `baseline_dir` / canonical-flip change.
- Nothing merged to `auto-loop-branch`; candidates isolated on forensic
  branches. Shadow per-case results were read **only for this post-run human
  review** (legitimate review-capacity access), not consumed by the dev/proposer
  during candidate generation.

---

# Run-2 (2026-06-12) — exp-77/78/79 record (OLD gate, keep=0/3) — superseded by Run-3 above

## Run parameters

| Field | Value |
|---|---|
| Command | `caffeinate -i uv run python -m autoloop run -n 3` |
| Preflight | `ok=6/0/0` (foreground :8080 clear, pg, redis, meta-LLM key, clean tree, baseline loads 3 suites) |
| Comparison baseline | `config.fitness.baseline_dir = eval_interactive/results/m-auto-7-prepilot-baseline-20260608` (pilot fitness baseline; NOT a canonical flip) |
| Pilot block | `active_sprint=S-Y2`; primary_targets `[cs_uc_a_no_ad_id_ad_specific, cs_uc_a_loaded_listing]`; anti_kill_control `[cs_uc_a_generic_policy_question]`; tier2_neighbors `[cs_uc_a_lookup_failed, cs_uc_fp_loaded_moderation]`; phase_hint `[DISCOVER, RESOLVE]`; use_case_hint `[UC-A]`; `lessons.enabled=false` |
| Result | **keep=0 / discard=3 / error=0** (of 3) |
| Wall time | ~7.5 h (exp-77 7170s, exp-78 10790s, exp-79 8951s) |
| scoring_code_baseline_sha | `0d86b08f…` UNCHANGED → **no re-bless in Part C** |
| Working tree after run | clean; on `auto-loop-branch`; HEAD `47ac0f70` unchanged (per-exp commits live on `autoloop/exp-{77,78,79}` forensic branches) |

## Per-iteration record

All three candidates **passed the anti-hardcode sandbox cleanly** (verdict
`PASS`, `rule_id=null`, `flag_for_codex=false`) and **reached eval** — i.e. the
combo's over-discard regression is gone. None were a Q1.if_then `$.procedure`
shape, so the positive `FLAG_FOR_CODEX` routing path was not exercised this run
(the proposer chose `desc` / `grounding_instruction` text edits); the **negative
payoff is confirmed: 0 anti-hardcode false-positive pre-eval discards** (vs the
prior tranche's exp-70/76, which were killed on `Q1.if_then_decision_tree`).

| exp | surface edited | anti-hardcode | reached eval | discard reason | full_on_gap |
|---|---|---|---|---|---|
| **77** | `resolve_faq_grounded_answer.yaml` `$.critical_steps[0].desc` | PASS | yes | `tier1_anchor_outcome_regression_8→7` (`anchor_outcome_uc_a_visibility`) | **HIT** |
| **78** | `resolve_faq_grounded_answer.yaml` `$.grounding_instruction` | PASS | yes | `tier1_bad_cases_regression_8→7` (anti-误杀 control regressed, see below) | **HIT** |
| **79** | `discover_triage.yaml` `$.critical_steps[2].desc` | PASS | yes | `tier1_anchor_outcome_regression_8→7` (`anchor_outcome_uc_d_login`) | partial (skill-miss) |

- All discards were at the **tier1 fitness gate** (not anti-hardcode, not safety).
- **Safety floor CLEAN** on all three (L0 `tier0_safety_floor_clean`,
  `python_tier0_family` 0 new failing cases; safety/escalation_compliance green).
- exp-79's candidate is a genuinely on-target idea (an "understand-vs-act"
  reframing of the DISCOVER intake-routing step to keep UC-A questions on the
  FAQ path against UC-H/UC-FP topic-adjacency pull) but it (a) targets
  `discover_triage` not the on-gap `resolve_faq_grounded_answer` (skill-miss),
  and (b) regressed the `uc_d_login` anchor.

## 4-layer hit-rate (authoritative — `config_validator.compute_pilot_hit_rates`)

| Layer | Rate | Run-1 ref |
|---|---|---|
| `phase_usecase_hit_rate` | **100.0%** (3/3) | — |
| `skill_hit_rate` | **66.7%** (2/3 — exp-79 = `discover_triage`, not `resolve_faq`) | ~50% ref |
| `field_family_hit_rate` | **100.0%** (3/3) | ~70% ref |
| **`full_on_gap_hit_rate`** | **66.7%** (2/3) | **0% (Run-1)** |

`partial_hit_breakdown`: `{phase_usecase+skill+field_family: 2, phase_usecase+field_family: 1}`.

Per §11.3 (calibrated 2026-06-09 round-2): **≥50% = steering STRONG PASS → look
at keep rate; 0 keep → §3.4 fallback; do NOT extend `-n 16`.**

## PRIMARIES outcome (must-resolve question) — NO FLIP

Per-case `majority_passed` / `pass_rate` (bad_cases suite, n=3 samples) vs the
pre-pilot baseline:

| case | role | BASE | exp-77 | exp-78 | exp-79 |
|---|---|---|---|---|---|
| `cs_uc_a_no_ad_id_ad_specific` | **Tier-1 primary** | fail 0.00 | fail 0.00 | fail 0.00 | fail 0.00 |
| `cs_uc_a_loaded_listing` | **Tier-1 primary** | fail 0.09 | fail 0.20 | fail 0.20 | fail 0.00 |
| `cs_uc_a_generic_policy_question` | **anti-误杀 control** | PASS 1.00 | PASS 0.80 | **fail 0.33** | PASS 0.60 |
| `cs_uc_a_lookup_failed` | Tier-2 neighbor | fail 0.09 | **PASS 0.60** | **PASS 0.60** | fail 0.40 |
| `cs_uc_fp_loaded_moderation` | Tier-2 neighbor | fail 0.00 | fail 0.00 | fail 0.00 | fail 0.00 |
| `alice_uc_a_uc_h_misclass` | EXTEND | fail 0.00 | fail 0.20 | fail 0.00 | fail 0.00 |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | EXTEND | PASS 0.73 | PASS 0.60 | PASS 0.80 | PASS 0.80 |

**PRIMARIES: NO FLIP.** `no_ad_id` is dead-flat 0.00 across baseline + all three
candidates — a single skill-yaml text edit does not move it. `loaded_listing`
nudged 0.09→0.20 under both `resolve_faq` edits (positive but sub-threshold, not
a flip) and regressed to 0.00 under exp-79.

### Signals for the §3.4 fallback author

- **The surface is NOT inert for `loaded_listing`**: both `resolve_faq` edits
  (`critical_steps[0].desc`, `grounding_instruction`) moved it 0.09→0.20. The
  hand-authored procedure should concentrate on this surface.
- **`cs_uc_a_lookup_failed` (Tier-2 neighbor) FLIPPED to PASS** (0.09→0.60)
  under both `resolve_faq` edits — the on-gap surface helps the adjacent
  graceful-degradation neighbor.
- **`no_ad_id` is the hard target** (dead 0.00) — likely needs the
  entity-context-verification step worded so the agent recognises an
  ad-specific question lacking an ad identifier and resolves via the visibility
  FAQ path rather than routing to intake.
- **anti-误杀 discipline held**: exp-78 regressed the negative control
  (1.00→0.33) and was correctly **NOT kept** (discarded at the fitness gate).
  The fallback procedure must keep `cs_uc_a_generic_policy_question` at PASS —
  this is the binding anti-误杀 gate.
- **DISCOVER-phase intake-routing edits carry collateral risk**: exp-79's
  discover_triage reframing regressed `uc_d_login`. Prefer the RESOLVE-FAQ
  surface unless a DISCOVER edit can be shown not to disturb other UC anchors.

> Note: exp-77 (`uc_a_visibility`) and exp-79 (`uc_d_login`) anchor regressions
> are n=3-majority single-case drops under the strict `max_drop_cases=0` policy
> and may include the pre-existing semantic flakiness documented at M-Auto-6
> close; the gate discards them regardless, so this does not change the
> 0-keep result.

## Carried-flag disposition (from the pre-pilot baseline §)

1. **`cs_uc_a_lookup_failed` Tier-2 demotion** — CONFIRMED: baseline fail 0.09;
   notably movable (flipped to PASS 0.60 under the two `resolve_faq` edits).
   Stays a Tier-2 neighbor / graceful-degradation guard, not a primary target.
2. **OQ-S87.uc-j-loop-detected** (observation) — no recurrence affecting the
   gate; safety floor stayed CLEAN on all three candidate runs. Carry to the
   milestone §5.6 safety review unchanged.
3. **OQ-S87.shadow-infra-noise** (shadow cs59s 400) — shadow firewall respected
   (shadow per-case results NOT read during this analysis). Existing R-item;
   carry unchanged.

## Verdict — §3.4 HAND-AUTHORED FALLBACK (VALID-ALTERNATE)

- **Pilot GATE (milestone §5.1.5): MET** — ran end-to-end, 0 infra errors,
  safety clean, all candidates reached eval.
- **Combo payoff: CONFIRMED (negative)** — 0 anti-hardcode false-positive
  pre-eval discards; the over-discard vector that blocked Part C is gone.
- **Steering: STRONG PASS** — `full_on_gap_hit_rate` 66.7% (≥50%), up from 0%.
- **keep rate: 0/3; PRIMARIES: NO FLIP.**
- **Decision (§11.3): invoke §3.4 hand-authored fallback; do NOT extend `-n 16`.**

### Not chosen, and why

- **PROCEED (merge + Part D)** — ruled out: 0 keeps, no primary flip, nothing to
  merge.
- **Extend `-n 16`** — ruled out by §11.3: strong-pass (≥50%) does not extend;
  more sampling of the same proposer on the same surface is low-yield when the
  hard primary (`no_ad_id`) is dead-flat across all attempts (~40 h for low
  expected yield).

## Next steps (Part C → Part D)

1. **Human go-ahead on §3.4 fallback** (recommended) — deliver/dev hand-authors
   a procedure within the SAME 6×4 mutable surface (most plausibly
   `resolve_faq_grounded_answer.yaml` `$.procedure` / `$.grounding_instruction`
   / `$.critical_steps[*].desc`), targeting `loaded_listing` + `no_ad_id`
   entity-context verification, held to the SAME CaseSpec gate + the SAME §4.1
   review. This is a binding semantic merge candidate and needs another
   real-LLM CaseSpec gate run + human §4.1 review — hence surfaced for
   confirmation rather than authored unilaterally.
2. **Part D** (after a fallback procedure is merged): milestone-close re-bless
   (writes a new dated dir; no canonical flip) → per-sub-sprint Codex §4.1
   review of the merged skill-yaml diff (REQUIRED, §4.3 trigger #2) → close.
3. **No re-bless in Part C** (scoring_code_baseline_sha unchanged).

## Hard-fence compliance

- Mutable surface respected: every candidate edited exactly one allowed
  `$.field` on an allowed skill YAML (sandbox `ACCEPT`, 0 rejected paths).
- No Java/runtime, CaseSpec/fixtures/scoring, or `baseline_dir` /
  `current_eval_baseline` canonical-flip change. Working tree clean on
  `auto-loop-branch`; no skill files committed to the working branch.
- Shadow firewall respected (no shadow per-case results read).
