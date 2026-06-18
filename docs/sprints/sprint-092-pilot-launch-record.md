---
title: "Sprint 092 / S-Auto-38 — M-Auto-7 pilot-scoped baseline launch & approval record"
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-18
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Run-scoped (NOT global) activation record for the split-rescored M-Auto-7
  pilot baseline. Binds via autoloop/config.pilot-s-auto-38.yaml (--config
  override) ONLY. Global config.fitness.baseline_dir and
  docs/current_eval_baseline.md are deliberately UNCHANGED; canonical
  promotion stays a separate milestone-close decision. exp-82 remains
  WITHDRAWN; no PRIMARY success is claimed. The -n1 real-LLM smoke is NOT yet
  launched — it is proposed for approval at the end of this record.
---

# Sprint 092 / S-Auto-38 — pilot-scoped baseline launch & approval record

## 1. What is being bound (run-scoped only)

The next M-Auto-7 smoke / pilot binds, via a `--config` override, a NEW
zero-LLM **split-rescored** baseline — without moving any global pointer.

| field | value |
|-------|-------|
| **Output (pilot) baseline path** | `eval_interactive/results/m-auto-7-prepilot-baseline-20260618-s_auto_38_split_full/` |
| **Source baseline** | `eval_interactive/results/m-auto-7-prepilot-baseline-20260608` (June-8 prepilot) |
| **Re-score method** | zero-LLM full re-score of the June-8 recorded draws under the WP-A escalation split (no new LLM, no new draws) |
| **Materialization harness** | `eval_interactive/analysis/rescore_s_auto_38_full_baseline.py` |
| **Harness commit SHA** | `2f5c5e9efa430c1524f7e24ed30e0a8474559f88` |
| **Run-scoped override** | `autoloop/config.pilot-s-auto-38.yaml` (commit `7dc5a0d438ee228adac583363bf783eb53900c97`) |
| **Re-bless provenance stamp** | `78c9f8a635162b3a` (16-char, 3 split-touched scoring files; from the tier-0 re-bless `_rebless_metadata.json`) |
| **Operative six-file scoring pin** | `7df8173cd4ef35741ec613038dc8cf29151d5b9880b585fac53442371fafebc0` (`gaming._SCORING_CODE_FILES`; set in the override `fitness.scoring_code_baseline_sha`) |
| **Gate definition version** | `tier0_escalation_split_s_auto_38_v1` |

**Unchanged (binding):** `autoloop/config.yaml` (`fitness.baseline_dir` still
the June-8 dir; `scoring_code_baseline_sha` still the OLD `f2f983cc…`) and
`docs/current_eval_baseline.md` (still M-Auto-6 `m-auto-6-baseline-shared-20260607`).
The pilot binding lives ONLY in the `--config` override.

## 2. Layer-0 vs composite distinction (load-bearing)

Two different deltas, do not conflate:

- **Layer-0 tier0-contribution delta = 6 cases** (Steps 1–3 re-bless;
  `others_pass AND escalation_check_majority`): cs001, cs011, cs01s02,
  cs11s02, cs76s01, cs76s02.
- **Gate-consumed composite `majority_passed` delta = 5 cases** (this
  artifact; the field `baseline_loader` actually reads): cs001, cs011,
  cs01s02, **cs11s01**, cs11s02.

They differ because **majority-of-ANDs ≠ AND-of-majorities**. Pilot binding
and acceptance use the **composite materialization** (this artifact), all 5
changes False→True, 0 draw/case regressions.

### Per-case annotations (human-confirmed 2026-06-18)

- **cs11s01** (`composite-only`): escalation Part-1 (behaviour) correct; OLD
  fail was the now-advisory reason-family label → composite majority 3/11→7/11.
  Classified **`dual_path_conflict`**; lands **TIER-N** (`pass_rate≈0.64`),
  **NOT** a stable TIER-S anti-误杀 floor. **Pending product-owner override
  review** (WP-B companion record, observation-only). Not excluded.
- **cs76s01 / cs76s02** (`tier0-only`): Part-2 demotion only removes the
  reason-label fail; both retain independent `L2_GATE:correct_uc` / critical
  TIER2 intake failures → `majority_passed` correctly stays **False**. They are
  NOT promoted to baseline pass by the escalation demotion.

## 3. Sign-offs

- **Step 1 — blast-radius review:** APPROVED (43 historical tier-0 flips all
  `PART2_DEMOTION`; 6 baseline tier-0 cases; self-check 341/341; 0 regressions;
  negative control still discards).
- **Steps 2–3 — canonical re-bless + strict compare:** APPROVED (3 suite maps
  byte-identical canonical vs preview; changed set == known 6; SHA match).
- **Step 4 — Option-R direction + 5-case composite baseline:** APPROVED
  (cs11s01 TIER-N w/ annotations; cs76s01/02 stay fails; Layer-0/composite
  distinction documented).
- **Smoke (`-n 1`):** **NOT approved yet** — proposed in §5.

## 4. Preflight evidence (zero-LLM)

- Harness tests: **23 pass** (`tests/test_rescore_s_auto_38_full_baseline.py`)
  — draw-level demotion gating truth table, per-draw re-score branching, OLD
  reconstruction (43 unaffected cases reproduce June-8), composite delta == the
  5 gate cases, native load clean.
- Full native `autoloop preflight --config config.pilot-s-auto-38.yaml`:
  **ok=6 warn=0 fail=0** (incl. `clean_working_tree`, `baseline_dir_loads` —
  3 suites, run_id `…-s_auto_38_split_full`).
- `baseline_loader.load` under the override: **0 warnings**; case_passed_count
  bad_cases **10/17**, anchor_outcome **8/12**, shadow **8/22** (= +5 vs June-8
  21).
- `gaming._check_scoring_code_drift` under the override: **NONE (cleared)**;
  control under the OLD committed pin → `scoring_code_drift.sha_changed` ERROR.

## 5. Non-claims (binding)

- **exp-82 remains WITHDRAWN** (n=13 primaries 0/13). This binding restores
  gate trustworthiness only; it does not revive exp-82.
- **No PRIMARY success is claimed.** The smoke is a machinery / gate-trust
  validation, not a fitness-certifying run.
- **Full pilot tranche stays HELD.** The smoke does not auto-advance to it.
- **No canonical promotion.** Global pointer + `current_eval_baseline.md`
  unchanged; promotion is a separate milestone-close decision.

## 6. Git state at record time

Working tree clean at `7dc5a0d4` (the Option-R harness+tests `2f5c5e9` and the
pilot override `7dc5a0d` are committed; the baseline artifact dir is gitignored
under `eval_interactive/results/*` and regenerable from the committed harness).

## 7. exp-86 `-n 1` real-LLM smoke outcome (2026-06-18)

Command: `cd autoloop && caffeinate -i uv run python -m autoloop run --config
config.pilot-s-auto-38.yaml -n 1` (log `autoloop/results/smoke-s-auto-38-n1-20260618.log`,
gitignored). Exit 0, elapsed 3977s (~66 min). **Gate-trust acceptance: PASS.
No STOP condition.**

| item | value |
|------|-------|
| run dir / experiment | `exp-86` (`autoloop/results/runs/exp-86/`) |
| proposer model | `claude-sonnet-4-6` (proposed a UC-A grounding skill edit for `loaded_listing` / `no_ad_id`) |
| baseline loaded | `m-auto-7-prepilot-baseline-20260618-s_auto_38_split_full` (3 suites, 0 warnings) under pin `7df8173c…` |
| in-run preflight | ok=6 warn=0 fail=0 |
| gate verdict | **keep**, `discard_reason=null` |

Per-suite (all `exit=0`): bad_cases 17c/101 draws/100 valid/0 mixed/7 timeout
(attempts_run=13, PRIMARY oversample); anchor_outcome 12c/60/59/0/0 (run 5);
shadow 22c/110/98/0/13 timeout (run 5), non_comparable 9.1%.

Gate basis: tier0_safety 51 checked, `failing_cases=[]`, `stable_reproduction=true`;
tier1 TIER-S floor hits `[]`; tier2 one increase (`iwzx_uc_k_advert_on_hold_restore`,
below the 2-case cross-case gate); improvement +1 (`anchor_outcome_uc_g_gdpr`) ≥ min 1;
shadow 0.40→0.35 `regression_detected=false`.

**Escalation split behaved as designed (live):** enforced tier-0 family includes
`escalation_compliance` (Part-1), excludes `escalation_reason_family_match`
(Part-2). 8 cases had Part-2 mismatch — all with Part-1=True; 5 still
composite-pass (cs001, cs011, cs01s02, cs11s01, cs11s02), 3 fail for non-Part-2
reasons. 0 genuine Part-1 failures → no escalation tier-0 discard, none from
Part-2. **Part-2 recorded but non-gating, confirmed live.**

Gaming flags: **1 WARN** `suspect_baseline_manipulation.git_lookup_failed`
("git log produced no commits for autoloop/config.yaml") — benign cwd/override
artifact (run launched from `autoloop/` with the `--config` override); **no
`scoring_code_drift`**. git status clean.

**Not a PRIMARY success:** both declared PRIMARY targets (`cs_uc_a_loaded_listing`,
`cs_uc_a_no_ad_id_ad_specific`) stayed `0/13` after PRIMARY oversampling; the KEEP
rode a peripheral (gdpr) improvement. exp-82 stays WITHDRAWN; full pilot tranche
stays HELD. Follow-up (M-Auto-7): target-alignment KEEP semantics + primary-first
staged eval (drafted post-smoke; not yet implemented).
