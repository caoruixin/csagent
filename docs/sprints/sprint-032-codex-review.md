## Sprint Review Decision
decision: fix_required
blocking_count: 2
summary: Sprint 32 is correctly limited to eval-spec surfaces in the commit range and introduces no production/prompt/Java-test semantic hardcode, but it does not close cleanly. Finding 1 is a scope-path mismatch against the active Sprint 32 contract for shadow CaseSpecs. Finding 2 is a case-family substance gap: the target/neighbor family does not reliably validate the `alternate_candidate_use_cases` worked-example signal because the target rerun lacks UC-C in the slot and neighbor #2 has an empty slot / ROUTED intake.

## Review Evidence
- Review range: `8d3e73b..c9edb37` (`HEAD` = `c9edb37a2cc172e3f19c7d4afcdd5e4ccb7b844d`) in a detached clean worktree at `/tmp/csagent-s32-review.zMehic`; active Sprint 32 objective and dev prompt were read from the deliver-agent-owned current workspace because they are intentionally not in the dev commit.
- Loaded governance and context: `AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`, `docs/sprint_objective.md`, `compact/sprint-032-dev-prompt.md`, `docs/sprints/sprint-032-handoff.md`, Sprint 31 close/fix handoffs, visible/shadow manifests, shadow access boundary, source brief, and all new visible/shadow CaseSpecs.
- Diff-scope check: `git diff --name-status 8d3e73b..HEAD` shows only the Sprint 32 source brief, new visible family, new shadow files, two manifest appends, and `docs/sprints/sprint-032-handoff.md`; no `server/src/main/**`, `server/src/test/**`, prompt, eval harness, smoke CaseSpec, existing visible family, existing shadow family, `docs/current/**`, `docs/foundational/**`, `docs/proposals/**`, older sprint archive, override, action-bank, objective, compact prompt, or Codex findings file appears in the commit range.
- Re-ran schema loading: visible path loaded 5 specs; actual shadow path loaded 2 specs; the Sprint 32 objective/review-specified direct shadow path is absent.
- Re-ran handoff extraction recipes against the cited dev result files in `eval_interactive/results/20260516-084603/results.json`, `eval_interactive/results/20260516-084749/results.json`, and Sprint 31 reference `eval_interactive/results/20260516-024934/results.json`; numeric summaries and per-case slot lists reproduced.
- Re-ran `mvn -q -pl server test` in the clean worktree; surefire XML reports `Tests run: 917, Failures: 0, Errors: 0, Skipped: 2` (see Validation Runs note for baseline discrepancy).
- Re-ran the Sprint 32 family through a clean server on port `18080` using the current workspace `.env.local` simulator credentials; the run emitted `results/20260516-091410/results.json` covering 5 cases, but provider latency caused contract violations on 3 cases and corroborates the existing latency-drift R-item.

## Blocking Findings
1. Scope-path mismatch for shadow CaseSpecs against the Sprint 32 contract. The active objective and review scope list the new shadow directory as `eval_interactive/case_specs_shadow/sprint32_alternate_uc/` (`docs/sprint_objective.md:86`, `docs/sprint_objective.md:193`), and the review gate allows only that shadow surface. The dev commit instead adds files under `eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/`, while the direct objective path does not exist and loader access to `case_specs_shadow/sprint32_alternate_uc` fails with `FileNotFoundError`. I recognize that `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md:91` documents `case_specs_shadow/case_families/<family_id>/`; that means the objective/review contract and access-boundary convention conflict, but under the supplied scope-discipline gate this is still a blocking close mismatch.

   Diff snippet:
   ```diff
   +++ b/eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/cs32s01_uc_c_uc_a_reverse_drift.yaml
   @@ -0,0 +1,68 @@
   +case_id: cs32s01_uc_c_uc_a_reverse_drift
   +source_session_id: synthetic-sprint32-shadow-01
   +source_dataset: case_family_authored
   ```

2. The authored family does not satisfy the target/neighbor soft-signal shape it was supposed to validate. Sprint 32's objective requires the target UC-A->UC-C worked-example and at least two neighbors that share the `AMBIGUOUS` intake + topic-shift-to-intake-alternate shape (`docs/sprint_objective.md:46`, `docs/sprint_objective.md:67`, `docs/sprint_objective.md:68`, `docs/sprint_objective.md:332`). The dev's own cited family rerun shows `cs32t01_uc_a_uc_c_drift` has alts `['UC-A','UC-FP','UC-H']` with UC-C absent, and `cs32n02_uc_a_uc_b_drift` has `[]` and is explicitly documented as ROUTED, not AMBIGUOUS (`docs/sprints/sprint-032-handoff.md:439`, `docs/sprints/sprint-032-handoff.md:441`, `docs/sprints/sprint-032-handoff.md:456`, `docs/sprints/sprint-032-handoff.md:467`, `docs/sprints/sprint-032-handoff.md:622`, `docs/sprints/sprint-032-handoff.md:639`). That leaves only neighbor #1 as a clean drift-to-observed-alternate case, so the eval family does not close the promised end-to-end validation of the Sprint 31 slot on the worked-example shape.

   Diff snippet for the neighbor that claims UC-B slot grounding but reran empty:
   ```diff
   +++ b/eval_interactive/case_specs/case_families/sprint32_alternate_uc/cs32n02_uc_a_uc_b_drift.yaml
   @@ -0,0 +1,27 @@
   +case_id: cs32n02_uc_a_uc_b_drift
   +form_context:
   +  topic_subject: Ad Support
   +  description: Hi, my ad is not showing many views and I also have a posting question I wanted to ask about.
   +persona:
   +  user_goal_summary: User opens with a visibility complaint shape (UC-A) on the Ad Support form. On turn 2 the user surfaces a distinct UC-B concern ... The Sprint 31 alternate_candidate_use_cases projection slot is observably populated with UC-B on the cs_interactive_192-shaped intake ...
   +expected:
   +  primary_uc: UC-B
   +  secondary_ucs:
   +  - UC-A
   +  bot_handling_pattern: ... observable via the alternate_candidate_use_cases projection slot if it carries UC-B on the AMBIGUOUS-intake path ...
   ```

## Anti-Hardcode Kernel
- Q1 keyword/regex/if-else/per-UC matrix: pass. No runtime, prompt, judge, or decision-routing logic was added; the per-case YAML describes user-side state and expected bars, and manifest matrices are schema fields.
- Q2 Tier-0 justification: N/A. No Tier-0 invariant was added; `docs/runtime_freeze_and_risk_policy.md` is untouched.
- Q3 soft-signal alternative: N/A. Sprint 32 validates the Sprint 31 soft signal and adds no decision-path code.
- Q4 eval text / CaseSpec id encoding: pass for runtime/prompt/judge. `rg` over `server/src/main/**`, `server/src/main/resources/prompts/system_prompt.txt`, and `eval_interactive/eval_interactive/**` finds no Sprint 32 case ids; existing Sprint 31 `alternate_candidate_use_cases` references predate this range.
- Q5 semantic ownership shift: pass. `git diff --stat 8d3e73b..HEAD -- server/src/main/` is empty, so Sprint 31's LLM-owned decision posture remains unchanged.
- Q6 prompt as if-else dump: N/A. `system_prompt.txt` is absent from the diff range.
- Q7 tool/capability/PII/grounding floor: pass. New specs keep `email: customer@example.com`, standard hard checks include `no_human_only_tool_exposure` and `no_pii_leakage`, and forbidden tools match the Sprint 20 / Sprint 29 precedent shape.
- Q8 generalization coverage: count pass, shape fail. Counts are target=1, neighbor=2, negative=2, shadow=2, but Finding 2 blocks because one neighbor is not an AMBIGUOUS/populated-slot neighbor and the target does not carry UC-C in the observed slot.
- Q9 rollback/sunset: N/A. These are permanent eval-corpus additions; rollback would delete the new family/shadow files and revert the two manifest appends.

## Hard-Fence Verification
- Scope surfaces: fail only on the shadow directory path named in Finding 1. All other committed paths are inside the intended Sprint 32 eval-spec/docs surfaces.
- Cascade fence, visible families: pass. `git diff --name-only 8d3e73b..HEAD -- eval_interactive/case_specs/case_families/` touches `_manifest.yaml` and only `sprint32_alternate_uc/**`; no existing family path matches the Sprint 20 / Sprint 29 list.
- Existing-shadow fence: pass for existing families. No file under an existing shadow family directory is touched; only the new Sprint 32 shadow family path and shadow manifest appear.
- Visible root manifest append-only: pass. Diff is a pure EOF append of one `family_id: sprint32_alternate_uc` block; no existing manifest entry changed.
- Shadow manifest append-only: pass. Diff is a pure EOF append of one `family_id: sprint32_alternate_uc` block with real shadow ids.
- §7.2 worked example untouched: pass. `docs/current/iteration_governance.md` is absent from the diff range.
- Sprint 31 T8 baseline preserved in diff: pass. `git diff --stat 8d3e73b..HEAD -- server/src/test/` is empty.
- Prompt / production / harness / smoke fences: pass. No diff under `server/src/main/**`, `server/src/main/resources/prompts/system_prompt.txt`, `eval_interactive/eval_interactive/**`, or `eval_interactive/case_specs/smoke/**`.
- Deliver-agent-owned surfaces: pass in the dev commit. `docs/action_bank.md`, `docs/sprint_objective.md`, `docs/10-handoff.md`, `docs/codex-findings.md`, and `compact/sprint-032-*-prompt.md` are absent from `8d3e73b..HEAD`.

## Schema And Reproducibility Checks
- Visible loader command: `cd eval_interactive && uv run python -c "from eval_interactive.case_spec.loader import load_case_specs; specs = load_case_specs('case_specs/case_families/sprint32_alternate_uc'); print(len(specs)); print([s.case_id for s in specs])"` -> `5` with ids `cs32g01_uc_a_deepens_no_drift`, `cs32g02_uc_a_explicit_stay`, `cs32n01_uc_a_uc_fp_drift`, `cs32n02_uc_a_uc_b_drift`, `cs32t01_uc_a_uc_c_drift`.
- Shadow loader at actual committed path: `load_case_specs('case_specs_shadow/case_families/sprint32_alternate_uc')` -> `2` with ids `cs32s01_uc_c_uc_a_reverse_drift`, `cs32s02_uc_a_uc_h_hidden_fact_drift`.
- Shadow loader at objective/review-specified direct path: `load_case_specs('case_specs_shadow/sprint32_alternate_uc')` -> `FileNotFoundError`; this is tied to Finding 1.
- Manifest parsing: visible manifest has 11 families and last entry `sprint32_alternate_uc`; shadow manifest has 11 families and last entry `sprint32_alternate_uc` with real shadow ids.
- Failure brief: all six §2 fields are present in `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md`; the `What should NOT be done?` section explicitly names a `replies` / `messages not coming through` / `buyer` regex or UC-C keyword bag in `DriftDetector` and cites §1.3 / §1.5 / §1.7 as the reason it is wrong (`docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md:131`).
- Dev family-result extraction reproduced from `eval_interactive/results/20260516-084603/results.json`: target alts `['UC-A','UC-FP','UC-H']`; neighbor #1 `['UC-A','UC-FP','UC-H']`; neighbor #2 `[]`; negative #1 `['UC-B','UC-FP','UC-H']`; negative #2 `['UC-A','UC-FP','UC-H']`.
- Dev 14-case smoke summary reproduced from `eval_interactive/results/20260516-084749/results.json`: `total_cases=14`, `passed_cases=3`, `failed_cases=11`, `task_success_rate=0.2143`, `mean_composite_score=0.1974`, `mean_outcome_score=0.7141`, `mean_judge_score=0.7762`.
- Sprint 31 reference premise extraction reproduced from `eval_interactive/results/20260516-024934/results.json`: `cs_interactive_015=['UC-A','UC-FP','UC-H']`, `cs_interactive_040` 12-element fallback, `cs_interactive_176=['UC-E']`, `cs_interactive_192=['UC-A','UC-B','UC-FP']`.
- §1.7 eval-widening check: pass. Both negative CaseSpecs expect `primary_uc: UC-A`, `secondary_ucs: []`, and no drift; the dev result shows both negatives had populated alternates, so the negatives are not admitting drift as acceptable.

## Validation Runs
- Java baseline command: `mvn -q -pl server test` in the detached clean worktree completed with exit code 0. Surefire XML aggregate: `Tests run: 917, Failures: 0, Errors: 0, Skipped: 2`. This is not byte-identical to the review prompt's stated dirty Sprint 31 close baseline (`917 / 1 / 0 / 2`), but the dev diff has no `server/src/main/**` or `server/src/test/**` changes; the discrepancy appears to be the inherited prompt-test failure not reproducing from a clean commit checkout rather than a Sprint 32 Java change.
- Sprint 32 family re-run, exact default config: in the detached clean worktree, `uv run eval-interactive run --path case_specs/case_families/sprint32_alternate_uc/ --label codex-sprint32-family-rerun --config /tmp/csagent-s32-review-config.yaml --parallel 1` first failed immediately when the clean worktree lacked the untracked `.env.local` simulator credentials (`Missing credentials...`).
- Sprint 32 family re-run, clean server + sourced local simulator credentials: started a clean server from `/tmp/csagent-s32-review.zMehic/server` on port `18080`, sourced the workspace `.env.local` for simulator credentials, and ran the family. Results: `eval_interactive/results/20260516-091410/results.json`, 5 cases, 0 passed, mean composite `0.0000`; 3 cases ended as `contract_violation` due DeepSeek read timeouts, `cs32n02` and `cs32t01` produced traces. Extracted alts: `cs32t01=['UC-A','UC-B','UC-FP','UC-H']`, `cs32n02=[]`, other three `absent` because no trace was produced after contract violation. This is further evidence for latency/provider drift and also does not repair Finding 2.
- Optional 14-case smoke re-execution: not re-run after the family re-run consumed the available validation window and hit provider timeouts. The dev's cited 14-case smoke result was reproduced from its `results.json` and shows no regression versus the Sprint 31 reference.

## Deferred / Non-Blocking Notes
- `R-llm-provider-latency-drift-2026-05-16` remains relevant. The clean re-run saw DeepSeek read timeouts and fallback skipped for insufficient budget; this is informational evidence for the existing R-item, not a new Sprint 32 production-code blocker.
- The shadow path discrepancy should be reconciled by the deliver-agent because `docs/sprint_objective.md` and the review prompt name `case_specs_shadow/sprint32_alternate_uc/`, while `_ACCESS_BOUNDARY.md` names `case_specs_shadow/case_families/<family_id>/`. The dev followed the boundary doc; the close gate follows the active sprint contract.
- Sprint 31 OQ4 is not fully closed until Finding 2 is fixed or explicitly re-scoped: the family exists and loads, but the target/neighbor observed slot contents do not yet validate the worked-example soft-signal shape end-to-end.
