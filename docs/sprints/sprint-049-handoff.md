---
title: Sprint 49 / S-Cleanup-3 handoff — Tier-2 phase-plan-scoped fix (#9) + handover_completeness demotion (#4)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff for Sprint 49 / S-Cleanup-3)
last_reviewed: 2026-05-23
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Dev handoff for Sprint 49 / S-Cleanup-3 (third and final sub-sprint
  of Milestone M4-Eval-Cleanup). §12 closure verdict reserved for
  deliver-agent + human at sub-sprint close. On close, M4-Eval-Cleanup
  goes to milestone close.
---

# Sprint 49 / S-Cleanup-3 handoff — Tier-2 phase-plan-scoped fix (#9) + handover_completeness demotion (#4)

## 1. Identity

- **Sprint number**: 49 (global) / S-Cleanup-3 (M4-Eval-Cleanup sub-sprint 3 of 3).
- **Milestone**: M4-Eval-Cleanup (`docs/milestone_objective.md`).
- **Branch**: `refactor/remove-the-shackles`.
- **HEAD prior to dev**: `8f32dbd` (S-Cleanup-2 close-bundle + governance consolidation per `docs/10-handoff.md` §1 lead).
- **Dev commit SHA**: appended at commit time (see §11 bundle policy).

## 2. Scope landed

Per `docs/sprint_objective.md` §4. Numstat is from `git diff --numstat <S3-files>` against HEAD `8f32dbd` (same shape as `git show --numstat <commit>` post-commit).

| Item | File | insertions | deletions | Notes |
|------|------|------------|-----------|-------|
| #9 | `eval_interactive/eval_interactive/batch/executor.py` | 57 | 17 | NEW module-level `_collect_presented_step_ids(per_turn_trace)` helper that returns the union of `phase_plan.critical_steps[].id` across all turns. `BatchExecutor._compute_tier2_result` now (a) computes the presented-id set first, (b) returns the inert `tier2_results_to_gate(())` default when the set is empty (defensive — DO NOT fall back to all-Skills), and (c) keeps only per-step results whose `step_id` is in the presented set. Per-step N/A-by-`mandatory_for` semantics in `skill_procedure_check.py:extract()` UNCHANGED. |
| #4 | `eval_interactive/eval_interactive/scoring/composite.py` | 21 | 33 | `_conditional_mandatory_l2` now returns `()` for every case — `handover_completeness` + `case_id_present` are no longer added to the mandatory-L2 set on `outcome_class == "escalate"`. The two helpers `_CASE_ID_UCS` + `_uc_family` that fed the removed UC-H/J/K branch are deleted as dead code (separate `outcome_checks.py` copies remain untouched). `compute_composite` docstring updated to record the demotion (S-Cleanup-3 #4) and reference the M3-Eval pyramid. Tier-3 advisory recording in `OutcomeCheckResult` is unchanged — the two dims are still computed and serialised, just no longer gating. |
| #9 + #4 | `eval_interactive/tests/test_composite_gate.py` | 53 | 19 | Inverted `test_escalate_adds_handover_completeness` → `test_escalate_does_not_add_handover_completeness` (asserts the demoted dims are NOT in the mandatory-L2 set). Inverted `test_outcome_class_escalate_handover_fails` → `test_outcome_class_escalate_handover_completeness_demoted` (asserts a failing handover_completeness does NOT flip `case_passed`). NEW `test_outcome_class_escalate_case_id_present_demoted` covering the UC-H absent-case-id case (pre-S-Cleanup-3 raised `L2_GATE_MISSING:case_id_present`). Module docstring updated to record the demotion. |
| #9 | `eval_interactive/tests/test_s_eval_5_l3_repositioning.py` | 20 | 14 | Renamed `test_executor_compute_tier2_result_iterates_all_skills` → `test_executor_compute_tier2_result_aggregates_presented_skills`. Test now seeds both skill ids into `phase_plan.critical_steps` so post-S-Cleanup-3 scoping recognises them as presented; the load-bearing assertion (a mandatory FAIL on a presented step flips Tier-2 to critical) is preserved. |
| #9 | `eval_interactive/tests/test_tier2_phase_plan_scoping.py` | 425 (new file) | 0 | NEW test module — 14 tests across 4 test classes covering: (a) `_collect_presented_step_ids` helper unit coverage (5 tests — union across turns / empty no-phase_plan / empty critical_steps / empty trace / ignore stepless dicts); (b) `TestPhasePlanScopedTier2` target / neighbor / negative-multi-phase / negative-multi-phase-with-failing-step (4 tests); (c) `TestDefensiveDefault` no-phase_plan / empty-trace / phase_plan-empty-critical_steps / extractor-missing (4 tests); (d) `TestUcMandatoryForInterplay` UC-not-in-mandatory_for → N/A (1 test). |

**Total S3-scope mutations** (modified files only, via `git diff --shortstat <S3-files>`): **4 files changed, +151 insertions, -83 deletions**.
**Plus NEW test file** `tests/test_tier2_phase_plan_scoping.py`: **425 lines**.

Reproducible commands:

```bash
git diff --numstat -- \
  eval_interactive/eval_interactive/batch/executor.py \
  eval_interactive/eval_interactive/scoring/composite.py \
  eval_interactive/tests/test_composite_gate.py \
  eval_interactive/tests/test_s_eval_5_l3_repositioning.py
git diff --shortstat -- <same paths>
wc -l eval_interactive/tests/test_tier2_phase_plan_scoping.py
```

**Files NOT staged** (deliver-agent / human bundle at close commit per `iteration_governance.md` §8.7):

- `docs/sprint_objective.md` (deliver-agent — S3 launch §1 lead update)
- `compact/sprint-049-dev-prompt.md` (deliver-agent — untracked prompt artefact)

**Governance touch (CONDITIONAL §4 row from sprint_objective)**: NOT taken. No `iteration_governance.md` §5.6 edit was needed for the #4 demotion — the M3-Eval four-tier pyramid intent (Tier-3 advisory for process-completeness) is already in scope per the existing §5.5 + §5.6 wording, and the code change is self-explanatory via the updated `_conditional_mandatory_l2` docstring + `compute_composite` rules block. Deliver-agent may fold a §5.6 clarification at M4-Eval-Cleanup close if desired; flagged here for visibility.

## 3. §3 layer-classification + anti-hardcode self-walk

The §7 stanza in `docs/sprint_objective.md` §3 holds against the actual commit:

- **Target failure layer**: `judge_calibration` (primary — Tier-2 wiring #9: the gate's correctness on the same prompt + CaseSpec) + `eval_spec` (secondary — #4 handover_completeness / case_id_present mandatory→advisory demotion).
- **Tier-0 invariant**: This sprint adds no Tier-0 invariant. `docs/runtime_freeze_and_risk_policy.md` §1 / §2 UNTOUCHED (confirmed below in §10 hard-fence checklist). The #4 demotion does not touch a safety floor — the demoted checks are process-completeness (handover summary fields, case_id linkage), not Tier-0 safety invariants (PII, identity verification, imminent harm), which remain on the L1 `hard_checks.py` surface.
- **Semantic hardcode**: No semantic hardcode introduced. (a) #9 scopes Tier-2 evaluation by the set of `critical_steps[].id` the runtime emitted into `per_turn_trace[].phase_plan.critical_steps` — observable trace state, NOT a keyword / regex / per-UC matrix. The step ids come from the runtime's own per-turn projection (single-source-of-truth `critical_steps` per M3-Eval); the eval side only consumes them. (b) #4 REMOVES a mandatory gate (demotion to advisory) — it shrinks, not expands, the deterministic surface; no hardcode added.
- **Generalization coverage**: target = the confirmed misflip (UC-A FAQ-resolve session, no escalation) covered by `TestPhasePlanScopedTier2::test_target_resolve_no_escalate_does_not_flip` PLUS the empirical anchor before/after delta in §6. Neighbor = UC-A escalation session covered by `test_neighbor_escalate_session_evaluates_escalate_step`; UC-K resolve session (UC-K-not-in-mandatory_for variant) covered by `TestUcMandatoryForInterplay`. Negative = multi-phase session entering BOTH resolve and escalate covered by `test_negative_multi_phase_session_evaluates_both` + `test_negative_multi_phase_one_failing_step_flips_gate` (proves the fix does not over-narrow). Defensive = empty / absent phase_plan covered by the 4-test `TestDefensiveDefault` class. Shadow = N/A (this is eval-harness gate logic, not a bot semantic surface; no held-out bot-behaviour cases apply).

### §4.1 nine-question kernel self-walk

Per `iteration_governance.md` §4.1, walked against the S3 staged diff:

1. **Q1 (keyword / regex / if-else / enum / per-UC matrix for a semantic decision)** — NO. #9 scopes Tier-2 evaluation by a set of step ids the runtime EMITTED into `per_turn_trace[].phase_plan.critical_steps[].id`. The set is observable runtime state, not a keyword / regex / per-UC matrix. No new identifier is hardcoded; no per-UC routing matrix is introduced; no message-content match is added. #4 REMOVES gates (shrinks the mandatory-L2 surface) and so cannot add a hardcode.
2. **Q2 (justified as protecting Tier-0)** — N/A. Neither change adds a new gate; #9 narrows an existing gate by observable state; #4 demotes existing gates.
3. **Q3 (could be projected as soft signal instead)** — N/A (Q1 = no). The phase-plan-scoping IS already consumption of a soft signal — the runtime's per-turn projection — so the eval side mirrors the LLM-side observable surface rather than duplicating routing.
4. **Q4 (encodes visible-eval case text / trace phrasing / CaseSpec id)** — NO. Neither change references any case_id, trace phrasing, or visible-eval surface. The new helper reads `phase_plan.critical_steps[].id`, which is the same single-source-of-truth surface the runtime LLM consumes.
5. **Q5 (moves semantic ownership LLM → Java)** — NO. The LLM-side semantic surface is untouched. `server/src/main/java/**`, `server/src/main/resources/system_prompt.txt`, and Skill YAMLs are all UNTOUCHED. #9 is eval-side gate correctness; #4 is eval-side gate demotion. No new Java decision was added; no LLM decision was moved to Java.
6. **Q6 (adds if-else block to prompt)** — NO. `server/src/main/resources/system_prompt.txt` UNTOUCHED.
7. **Q7 (preserves tool schema / capability / PII / grounding floor)** — YES (preserved). Tool schema, capability boundary, PII floor, grounding floor are all server-side; S3 makes no `server/` changes. The Java Tier-0 safety floor on `hard_checks.py` is also UNTOUCHED — the #4 demotion only affects the L2 mandatory-set, which sits above the safety floor.
8. **Q8 (ships target / neighbor / negative / shadow coverage)** — YES (per §3 stanza). 14 new tests + inverted `test_composite_gate` tests cover target / neighbor / negative / defensive. Shadow = N/A (eval-harness gate logic, not a bot semantic surface). Empirical anchor-before/after delta provides additional coverage on the production gate path.
9. **Q9 (rollback plan if temporary)** — N/A. Both changes are durable — #9 is a correctness fix (the previous behaviour was a bug); #4 is a permanent demotion per M3-Eval pyramid intent. `git revert <s3-commit>` reverses both if needed; no sunset plan applies.

**Verdict (self-assessment)**: `approve`. Clean §4.1 pass per Q1-Q9. The change is correctness-driven (the memo §3 offline reproduction confirms #9 is a bug fix, not a discretionary design choice) and scoping-by-observable-state, not hardcode.

## 4. Java baseline

`cd server && mvn test -q` (no flags):

```
[ERROR] Failures:
[ERROR]   SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53
    ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor ==> expected: <true> but was: <false>
[ERROR] Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2
```

**Baseline preservation**: `1163 / 1-inherited / 0 / 2` — IDENTICAL to S-Cleanup-2 close per `docs/10-handoff.md` §1 lead and the M3-Eval baseline. The 1 inherited failure persists per OQ-S41.5 STATUS QUO. S3 touches NO `server/` code (eval-side gate correctness + demotion only).

Reproducible command: `cd server && mvn test -q 2>&1 | tail -3`.

## 5. Python baseline

`cd eval_interactive && uv run pytest --tb=no -q` (full suite):

**Pre-S3 (per OQ-S47.3 baseline)**: `7 failed / 441 passed`.

**Post-S3** (S3 changes applied):

```
7 failed, 456 passed in 12.12s
```

The 7 pre-existing env-specific failures (`test_case_spec_overrides::test_v2_schema_loads_cleanly`, `test_case_spec_overrides::test_smoke_review_report_tracks_smoke_set_and_overrides`, 5x `test_corpus_lint::test_regenerated_corpus_bucket_lints_clean[*]`, `test_corpus_lint::test_full_corpus_lints_clean_with_smoke_subset_flag`) persist unchanged. No NEW failures.

**Pass-count delta**: +15 (441 → 456). Breakdown: +14 new tests in `test_tier2_phase_plan_scoping.py`, +1 new test (`test_outcome_class_escalate_case_id_present_demoted`) in `test_composite_gate.py`. The two inverted tests in `test_composite_gate.py` and the renamed test in `test_s_eval_5_l3_repositioning.py` are net-zero (rename + invert).

Reproducible command: `cd eval_interactive && uv run pytest --tb=no -q 2>&1 | tail -5`.

Item-specific:

- `cd eval_interactive && uv run pytest tests/test_tier2_phase_plan_scoping.py -v` → 14 passed.
- `cd eval_interactive && uv run pytest tests/test_composite_gate.py tests/test_s_eval_5_l3_repositioning.py tests/test_tier2_phase_plan_scoping.py -v` → 63 passed (covers the inverted #4 tests + the renamed #9 wiring test + all new tests in one pass).

## 6. Per-item verification + empirical evidence

### 6.1 #9 phase-plan-scoped Tier-2 — empirical anchor before/after

Per sprint_objective §4 step 1 + step 4. Backend ran on `http://localhost:8080` throughout; LLM keys (Moonshot) loaded from runtime config; HEAD `8f32dbd` for the before-run, post-fix working tree for the after-run.

**Before-fix (HEAD `8f32dbd` semantics — the running `eval-interactive` Python process loaded `executor.py` and `composite.py` from disk at 13:12:33, 36 seconds BEFORE the S3 edits hit the working tree at 13:13:09; Python imports are not hot-reloaded mid-process, so the 159-case run consumed the pre-edit bytecode end-to-end)**:

Run id: `20260523-051234` — `cd eval_interactive && uv run eval-interactive run --set anchor --parallel 1 --label s-cleanup-3-before`. Elapsed: 4 616 661 ms (~77 min). Started 13:12:33, completed 14:30:11.

| Metric | Value |
|--------|-------|
| Total cases | 159 |
| FAIL (case_passed = False) | **159** |
| PASS (case_passed = True) | **0** |
| Cases with `escalate-via-request-handover` in `failed_step_ids` | **119** |
| Cases with `search-knowledge-before-faq-answer` in `failed_step_ids` | **20** |
| Cases with BOTH escalate-step AND search-knowledge step in `failed_step_ids` | 20 |
| Cases with ANY misflip-shape Tier-2 fail (escalate OR search-knowledge) | **119** |
| Cases where `escalate-via-request-handover` is the ONLY failed Tier-2 step | 12 |
| Cases whose only failure tags are `TIER2:` (no L1, no L2_GATE) | 3 |
| Cases failed due to `L2_GATE:handover_completeness` | 0 |
| Cases failed due to `L2_GATE_MISSING:case_id_present` | 0 |

The mean composite was `0.0000` — every one of 159 cases zeroed because case_passed flipped on a mandatory Tier-2 failure. The 119 cases with `escalate-via-request-handover` in failed_step_ids = exactly the §3 misflip pattern (the escalate Skill's mandatory step evaluated against sessions that never traversed the ESCALATE phase). The 20 cases with `search-knowledge-before-faq-answer` failing = the symmetric misflip on the escalate path (the resolve_faq Skill's mandatory step evaluated against escalate-path sessions). The 0 / 0 on the L2_GATE rows shows the #4 demotion is not load-bearing for the anchor-suite pass rate (handover_completeness only mandatory on `outcome_class == "escalate"` and case_id_present only on UC-H/J/K; the 82 escalate cases here passed the OutcomeChecker's auto-computed handover_completeness score, and UC-K=8 cases happened to carry a present case_id). The #4 demotion is correctness for the M3-Eval pyramid, not a pass-rate driver on the current anchor distribution.

Per-UC FAIL breakdown (all 100%): UC-A=13/13, UC-B=5/5, UC-C=77/77, UC-D=38/38, UC-E=11/11, UC-F=1/1, UC-FP=6/6, UC-K=8/8.

Misflip-shape jq extraction (run against `results/<run_id>/results.json`; the shape is `case_results[].tier2_result.failed_step_ids` at the top level — there is no nested `.composite` envelope in the serialised CaseResult):

```bash
# Count cases where the escalate Skill's mandatory step was the / a flipping cause.
jq '[.case_results[] | select(
  .case_passed == false and
  (.tier2_result.failed_step_ids | index("escalate-via-request-handover")) != null
)] | length' results/<before_run_id>/results.json

# Sub-shape: cases where escalate-via-request-handover is the ONLY failed Tier-2 step
# (the cleanest "no other reason for failure" misflip).
jq '[.case_results[] | select(
  .case_passed == false and
  .tier2_result.failed_step_ids == ["escalate-via-request-handover"]
)] | length' results/<before_run_id>/results.json
```

**After-fix (working-tree HEAD with S3 changes applied; new `eval-interactive run` process started at 14:32:21, importing the post-edit `executor.py` + `composite.py` from disk)**:

Run id: `20260523-063218` — same command with `--label s-cleanup-3-after`. Elapsed: 4 714 868 ms (~79 min). Started 14:32:21, completed 15:50:56.

| Metric | Value |
|--------|-------|
| Total cases | 159 |
| FAIL (case_passed = False) | **148** |
| PASS (case_passed = True) | **11** |
| Cases with `escalate-via-request-handover` in `failed_step_ids` | **0** |
| Cases with `search-knowledge-before-faq-answer` in `failed_step_ids` | **13** |
| Cases failed due to `L2_GATE:handover_completeness` | 0 |
| Cases failed due to `L2_GATE_MISSING:case_id_present` | 0 |

Top failure-tag distribution (residual legitimate failures, NOT misflips):

```
  75x L3_ADVISORY:groundedness          (advisory; does not gate)
  72x TIER2_ADVISORY:record-outcome-on-grounded-answer  (advisory; does not gate)
  71x L2_GATE:correct_outcome           (real outcome mismatch)
  43x L1:source_citation_present        (real L1 hard-check failure)
  42x CONTRACT_VIOLATION:active_use_case (bot did not classify use case)
  33x L1:trace_minimum                  (real L1 hard-check failure)
  32x L2_GATE:correct_uc                (real outcome mismatch)
  22x L1:escalation_compliance          (real L1 hard-check failure)
```

**Before/after delta**:

| Metric | Before | After | Δ |
|--------|--------|-------|---|
| `case_passed = True` count | 0 | 11 | **+11** |
| `escalate-via-request-handover` misflips | 119 | 0 | **−119** |
| `search-knowledge-before-faq-answer` in failed_step_ids | 20 | 13 | −7 |
| Mean composite | 0.0000 | 0.0346 | +0.0346 |
| Mean outcome | 0.5193 | 0.4899 | −0.0294 (advisory L3 reweighting) |
| Stall rate | 5.7% | 8.8% | +3.1pp (orthogonal LLM-side variance) |

The +11 PASS-count delta + the **119 → 0 elimination of the escalate-step misflip** confirms #9 resolves the bug on the production gate path, mirroring the memo §3 offline reproduction. The 11 PASS cases have `case_passed = True` but `composite < 0.7` (the executor's PASS / FAIL stdout label is `case_passed AND composite >= 0.7`); they are now correctly gated through Tier-2 and merely held back by graded L2 / L3 scores. The residual 13 `search-knowledge-before-faq-answer` failures are LEGITIMATE Tier-2 fails — the runtime's `phase_plan.critical_steps` DID present this step for those resolve-path cases, and the bot did not dispatch `search_knowledge` before answering. The 7-case drop from 20 → 13 reflects the fix dropping the step on escalate-path sessions that never traversed the RESOLVE phase (the symmetric misflip on the other half of the §3 mechanism).

Reproducible commands:

```bash
RUN_BEFORE=eval_interactive/results/20260523-051234/results.json
RUN_AFTER=eval_interactive/results/20260523-063218/results.json
jq '[.case_results[] | select(.case_passed)] | length' $RUN_BEFORE $RUN_AFTER
jq '[.case_results[] | select((.tier2_result.failed_step_ids // [] | index("escalate-via-request-handover")) != null)] | length' $RUN_BEFORE $RUN_AFTER
```

### 6.2 #9 unit test coverage

`cd eval_interactive && uv run pytest tests/test_tier2_phase_plan_scoping.py -v` → 14 passed.

Coverage per §3 stanza:

- **Target** `test_target_resolve_no_escalate_does_not_flip` — UC-A FAQ-resolve session whose `phase_plan.critical_steps` carries only resolve + terminal step ids → escalate step is NOT evaluated → Tier-2 PASSes. The exact memo §3 misflip scenario.
- **Neighbor** `test_neighbor_escalate_session_evaluates_escalate_step` — UC-A escalate session whose `phase_plan.critical_steps` carries the escalate step id → step IS evaluated; PASSes with `request_handover` dispatched, FAILs without it.
- **Negative (multi-phase)** `test_negative_multi_phase_session_evaluates_both` — session entering both resolve and escalate phases → both Skills' presented steps evaluated → gate reflects the union (must not over-narrow).
- **Negative (multi-phase, one failing)** `test_negative_multi_phase_one_failing_step_flips_gate` — multi-phase session where the resolve step's trace_check fails → Tier-2 flips on the resolve step only; the escalate step's presence does not introduce a false positive on the unrelated session path.
- **Defensive** 4 tests in `TestDefensiveDefault` — no-phase_plan / empty-trace / phase_plan-with-empty-critical_steps / extractor-missing all return inert PASS/advisory (NO fall-back to all-Skills, which would re-introduce the bug).
- **UC interplay** `test_uc_not_in_mandatory_for_yields_na_not_fail` — extractor's per-step N/A-by-mandatory_for semantics preserved (UC-K with a UC-A/B/C-only step → N/A, gate stays PASS).
- **Helper unit** 5 tests in `TestCollectPresentedStepIds` — `_collect_presented_step_ids` correctness across empty / multi-turn / stepless-dict cases.

### 6.3 #4 demotion verified

`grep -n "handover_completeness\|case_id_present" eval_interactive/eval_interactive/scoring/composite.py`:

```
44:    S-Cleanup-3 (#4) demoted ``handover_completeness`` and
45:    ``case_id_present`` from mandatory-on-escalate to Tier-3 advisory
54:      ``handover_completeness`` and ``case_id_present`` are now Tier-3
```

All three occurrences are in docstrings recording the demotion. `_conditional_mandatory_l2` now returns `()` — neither check is added to the mandatory-L2 set under any condition.

Demotion tests:

- `test_escalate_does_not_add_handover_completeness` — inverted from pre-S3 to assert the mandatory-L2 set does NOT include `handover_completeness` on `outcome_class == "escalate"`.
- `test_outcome_class_escalate_handover_completeness_demoted` — full-pipeline test: an escalate case with a failing `handover_completeness` (score 0.6) does NOT flip `case_passed`; no `L2_GATE:handover_completeness` failure tag.
- `test_outcome_class_escalate_case_id_present_demoted` — new test: a UC-H escalate case with `case_id_present` ABSENT from l2_results does NOT raise `L2_GATE_MISSING:case_id_present` and does NOT flip `case_passed`.

**Safety floor verification**: per §7 stop conditions in sprint_objective, the demoted dims were checked against `hard_checks.py` schema. Neither dim appears in the Tier-0 hard-checks set (`no_pii_leakage`, `no_critical_policy_violation`, etc.); they were always L2 process-completeness checks, never Tier-0 safety. Demotion preserves the safety floor.

### 6.4 Bad-case suite regression-safety

`cd eval_interactive && uv run eval-interactive run --set bad_cases --parallel 1 --label s-cleanup-3-badcase-regression`:

Run id: `20260523-075141`. Elapsed: 340 107 ms (~5.7 min). All 12 cases ran end-to-end without crashes; all 12 carry `case_passed_authority = "human_review"` per S-Cleanup-2 (programmatic verdict informational only).

| Case | UC | programmatic case_passed | composite | `containment_outcome` |
|------|----|--------------------------|-----------|----------------------|
| `alice_uc_a_uc_h_misclass` | UC-A | true | 0.500 | escalated |
| `cs001_uc_c_mechanical_template_escalate` | UC-C | true | 0.500 | escalated |
| `cs011_uc_c_faq_miss_not_distress` | UC-C | false | 0.000 | escalated |
| `cs012_uc_fp_late_phone_failure_path` | UC-FP | false | 0.000 | escalated |
| `cs014_uc_c_faq_miss_not_distress` | UC-C | true | 0.500 | escalated |
| `cs015_uc_fp_appeal_edit_repost` | UC-FP | true | 0.500 | escalated |
| `cs029_uc_d_account_locked_callback` | UC-D | false | 0.000 | (empty) |
| `cs066_uc_k_in_app_feature_regression` | UC-K | false | 0.000 | escalated |
| `cs095_uc_d_email_recovery_misroute` | UC-D | false | 0.000 | escalated |
| `fg5q_uc_fp_phone_rejected_repost` | UC-FP | false | 0.000 | (empty) |
| `iwzx_uc_k_advert_on_hold_restore` | UC-K | true | 0.500 | escalated |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | UC-A | false | 0.000 | (empty) |

Failure-tag distribution (across all 12 cases):

```
  5x TIER2_ADVISORY:record-outcome-on-grounded-answer  (advisory; does not gate)
  2x TIER2:search-knowledge-before-faq-answer          (legitimate — step presented, not searched)
  2x L1:trace_minimum                                  (real L1)
  2x L1:no_pii_leakage                                 (real L1)
  1x TIER2_ADVISORY:resolve-article-after-search-hit   (advisory)
  1x TIER2:uc-k-intake-complete-before-handover        (legitimate — intake gap)
  1x TIER2:uc-h-intake-complete-before-handover        (legitimate — intake gap)
  1x L2:case_id_present                                 (low-L2 informational; not gating per §4 demotion)
  1x L1:escalation_compliance                          (real L1)
  1x CONTRACT_VIOLATION:active_use_case                (pre-existing CONTRACT trace issue)
```

**Regression-safety verdict (programmatic-only — §5.6 human-review is the authoritative gate)**:

- Distribution is qualitatively consistent with M3-Eval close behaviour (per `milestone_objective.md` §5 acceptance bar shape: 5 PASS / 4 IMPROVING / 3 FAIL was the human-judgment classification on the M3-Eval close human-review pass). The post-S3 programmatic case_passed split is 6 true / 6 false, which is informational and not the gate.
- ZERO cases failed due to the escalate-step misflip (`escalate-via-request-handover` absent from all 12 cases' failed_step_ids — confirmed by `jq '[.case_results[] | select((.tier2_result.failed_step_ids // [] | index("escalate-via-request-handover")) != null)] | length'` → 0). Consistent with the §6.1 anchor evidence.
- The 2 `TIER2:search-knowledge-before-faq-answer` and the 2 `TIER2:uc-{k,h}-intake-complete-before-handover` failures are LEGITIMATE Tier-2 fails — the runtime's `phase_plan.critical_steps` presented these steps and the bot did not satisfy them. The fix is correctly EVALUATING these (not over-narrowing them away).
- The 1 `L2:case_id_present` is now a low-L2 informational tag (the dim's score is still computed by `OutcomeChecker`), not a `L2_GATE:` failure. The #4 demotion is observable.
- No new CONTRACT violations introduced. The 1 `CONTRACT_VIOLATION:active_use_case` is the pre-existing pattern documented across prior sprints.

Deliver-agent + human classify the qualitative PASS / IMPROVING / FAIL per-case against `closure_criterion` at S-Cleanup-3 sub-sprint close / M4-Eval-Cleanup milestone close per §5.6.

Reproducible commands:

```bash
RUN=eval_interactive/results/20260523-075141/results.json
jq '.case_results | sort_by(.case_id) | .[] | {case_id, case_passed, composite_score, containment_outcome, primary_uc, case_passed_authority}' $RUN
jq '[.case_results[] | (.failure_tags // [])[]] | group_by(.) | map({tag: .[0], count: length}) | sort_by(.count) | reverse' $RUN
```

## 7. #9 negative-control evidence (multi-phase session still evaluates both)

`test_negative_multi_phase_session_evaluates_both` and `test_negative_multi_phase_one_failing_step_flips_gate` in `tests/test_tier2_phase_plan_scoping.py` are the synthetic negative-control evidence. Both PASS.

Real-trace negative-control (from `results/20260523-024518/results.json`): UC-C single-phase RESOLVE session carries `phase_plan.critical_steps` of 5 resolve_faq step ids (`search-knowledge-before-faq-answer`, `resolve-article-after-search-hit`, `record-outcome-on-grounded-answer`, `consult-moderation-context-on-removal-explanation`, `reroute-on-actionable-appeal-intent`) — no escalate step id, no terminal step id. The fix correctly drops the escalate Skill's mandatory step on this session.

Empirical multi-phase confirmation will surface in the anchor-after run §6.1 once misflip-count drops to 0 while sessions that legitimately ESCALATE still flip Tier-2 only on their own Skill's failures (not on cross-Skill mandatory steps).

## 8. Open questions surfaced

- **OQ-S49.1 — Residual 148/159 anchor FAILs post-fix.** The S3 #9 fix eliminated all 119 escalate-step misflips (anchor-before `escalate-via-request-handover` failures: 119 → 0) and lifted `case_passed = True` from 0 → 11. The remaining 148 FAILs are legitimate failure shapes — top tags `L2_GATE:correct_outcome` (71x), `L1:source_citation_present` (43x), `CONTRACT_VIOLATION:active_use_case` (42x), `L1:trace_minimum` (33x), `L2_GATE:correct_uc` (32x), `L1:escalation_compliance` (22x), `TIER2:search-knowledge-before-faq-answer` (13x, down from 20). These are bot-side / contract-trace issues, NOT eval-side gate bugs. The 13 `search-knowledge-before-faq-answer` Tier-2 fails are the symmetric mechanism the memo §3 described — a presented-step Tier-2 evaluating a legitimate flow (resolve_faq) where the bot did not search before answering. Deliver-agent + human review at close to decide whether any of these warrant a follow-on R-item; many already track to existing M4+ candidates (UC-G/H/I/J bad-case seeding, semantic-planner soft-signal extension, etc.) per `milestone_objective.md` §11.
- **OQ-S49.2 — §5.6 governance text fold-in.** The #4 demotion docstring in `_conditional_mandatory_l2` describes the M3-Eval pyramid intent inline. If deliver-agent + human prefer a §5.6 governance text clarification at milestone close, the natural insertion point is `iteration_governance.md` §5.6's "What stays as hard close gate" subsection (after the "Grounding floor unchanged" bullet) — a 1-2 sentence cross-reference noting handover_completeness / case_id_present are Tier-3 advisory. Per sprint_objective §4 governance row, dev prefers leaving this for deliver-agent at milestone close.
- **OQ-S49.3 — Composite threshold vs gate.** The 11 post-fix `case_passed = True` cases all have `composite < 0.7` (the executor's PASS / FAIL stdout label is `case_passed AND composite >= 0.7`). They are now correctly gated through Tier-2 but held back by graded L2 / L3 scores. The "Passed: 0" line in the stdout summary is consistent with this — pre-S3 ALL cases zeroed via `case_passed = False`; post-S3 the gate correctly identifies 11 as passing but the composite-threshold metric still reads 0. Deliver-agent / human may consider whether the executor summary should also surface a `Gate-passed: <N>` line distinct from the threshold-passed count; that is a reporting enhancement, not a correctness issue, and sits outside S3 scope.

## 9. Drift items

- `eval_interactive/eval_interactive/scoring/outcome_checks.py` retains its own `_CASE_ID_UCS` and `_uc_family` (separate copies from the deleted composite.py originals; lines 31 + 498 + ~497). These are used by the outcome-checks scorer to COMPUTE `case_id_present` / `handover_completeness` scores — the demotion is gate-side only; the scores themselves are still produced and serialised for trend reporting. Drift = none; this is the intended split between "score is computed" and "score gates `case_passed`".
- The `_conditional_mandatory_l2(case_spec)` function now ignores its `case_spec` parameter. Kept as a parameter for the `_mandatory_l2_names(case_spec)` call site without a signature change. If a future sub-sprint resurrects conditional mandatory L2 logic, the parameter is in place. Drift = low (a structural placeholder, not a code-smell).

## 10. Hard fence honored checklist

Per `docs/sprint_objective.md` §5:

- [x] No `server/src/main/java/**` touched — confirmed by `git diff --stat -- server/` returning empty.
- [x] No `server/src/main/resources/skills/*.yaml` touched — confirmed. The misflip is fixed by changing eval consumption, not by editing `mandatory_for` on the escalate step.
- [x] No `server/src/main/resources/system_prompt.txt` touched.
- [x] No `docs/runtime_freeze_and_risk_policy.md` touched.
- [x] No `docs/current/doc_governance.md` / `agent_context_guide.md` touched.
- [x] No `eval_interactive/case_specs/**` fixture edits (no Alice / bad_cases / anchor / shadow YAML touched).
- [x] No `eval_interactive/case_specs/shadow/` reads — none of the S3 code reads from that directory; dev did not open any shadow file.
- [x] No `skill_procedure_check.py:extract()` per-step N/A-by-`mandatory_for` semantics change — diff = 0 on that file. The S3 fix is at the executor orchestration layer (which steps are kept), not the per-step evaluator (how a single step is evaluated).
- [x] No `sets.py` `_OPT_IN_SETS` / `is_human_judgment_suite` change — diff = 0 on that file (S-Cleanup-2 surface, untouched).
- [x] No `docs/sprints/sprint-NNN-*` archive edits — only NEW `sprint-049-handoff.md`. No archive (sprint-001 … sprint-048) was opened with intent to edit.
- [x] No `docs/milestones/*` archive edits.
- [x] No `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `docs/milestone_objective.md` edits — deliver-agent / review-agent territory.
- [x] No governance text edit — the optional §4 row "governance text (CONDITIONAL)" was NOT taken; deliver-agent may fold at milestone close (OQ-S49.2).

## 11. R-item flip request

None requested at S3 close. The #9 fix is correctness-driven and does not close an R-item; the #4 demotion implements M3-Eval pyramid intent and does not close an R-item (the M3-Eval close already noted handover_completeness as a Tier-3 candidate). Deliver-agent may surface new R-items at milestone close based on OQ-S49.1 if the non-misflip FAILs (legitimate bot failures or other Tier-2 mandatory-pair misflips) warrant follow-on work.

## 12. Closure verdict (deferred)

Reserved for deliver-agent + human at S-Cleanup-3 sub-sprint close. On close, M4-Eval-Cleanup goes to milestone close (Codex milestone-shared review per §4.3 default + bad-case suite regression-safety rerun + milestone close artefacts per `iteration_governance.md` §8.4).
