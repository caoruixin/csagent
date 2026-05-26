Paste the content below this line into a fresh Codex session at M4-Eval-Cleanup milestone close, after Sprint 49 / S-Cleanup-3 has closed A — Clean PASS and after the deliver-agent + human have completed the bad-case suite manual review. No PR will be opened; review the milestone-shared cumulative commit range directly.

**Commit-timing note (deliver-agent → human, read before dispatch):** at the time this prompt was drafted the S-Cleanup-3 dev work was still in the working tree (uncommitted; HEAD `8f32dbd`). Before dispatching Codex, the human commits the S-Cleanup-3 dev-scope files as a dev commit (`sprint 49 / S-Cleanup-3: Tier-2 phase-plan-scoped fix (#9) + handover_completeness demotion (#4)`) so the cumulative range is well-defined. The deliver-agent milestone-close bundle (archives + 10-handoff + action_bank + codex-findings archive) lands AFTER this Codex review (it archives the very findings Codex writes). So at review time the range is `4344662..<S3-dev-commit>` and the close bundle is not yet committed.

---

You are the Anti-Hardcode + Milestone-Close Review Agent for **Milestone M4-Eval-Cleanup — Evaluation harness + governance gap cleanup** per `docs/milestone_objective.md`. M4-Eval-Cleanup is the FOURTH milestone planned and executed under the `iteration_governance.md` §8 milestone framework (M1 closed 2026-05-17; M2 closed 2026-05-18; M3-Eval closed 2026-05-23). It uses a milestone-shared cumulative Codex review per §4.3 second-paragraph DEFAULT.

M4-Eval-Cleanup is the **first explicitly cleanup-flavored milestone** under §8 (M1/M2/M3-Eval were implementation / architecture flavored). It consumes a post-M3-Eval cleanup audit (deliver-agent verified 9 of 10 P1 claims + 1 Tier-2 design decision). The shape — a non-bad-case-anchored acceptance bar (suite-stability + Codex pass + per-item verification) plus a multi-sub-sprint cleanup theme — establishes a precedent for future cleanup milestones. **The headline scope-discipline fact: M4-Eval-Cleanup touches ZERO `server/` runtime code across all three sub-sprints. Every change is eval-harness-side (Python under `eval_interactive/`) or eval-side fixtures.**

M4-Eval-Cleanup shipped 3 sub-sprints:

| Sub-sprint | Commit(s) | Outcome | Per-sub-sprint Codex |
|---|---|---|---|
| Sprint 47 (S-Cleanup-1; eval-harness polish + R-item closure) | `1576070` (dev) + `8ccedbf` (close + M4-Eval-Cleanup launch-bundle) | A — Clean PASS | Deferred to milestone-shared close per §4.3 default (infra + char-test + R-item closure; no trigger) |
| Sprint 48 (S-Cleanup-2; bad-case fixture schema unification + executor suite-mode) | `b989833` (dev) + `36ade6e` (close + Tier-2 #9 design memo + S-Cleanup-3 placeholder) | A — Clean PASS | Deferred to milestone-shared close per §4.3 default (eval_spec fixture + infra annotation; no trigger) |
| Sprint 49 (S-Cleanup-3; Tier-2 phase-plan-scoped fix #9 + handover_completeness demotion #4) | `<S3-dev-commit>` (dev) | A — Clean PASS (dev-level; this review + bad-case manual review are the milestone-close gates) | **§4.3 trigger #2 candidate** (touches the `case_passed` Tier-2 gate logic — `judge_calibration` layer). Since S-Cleanup-3 is the LAST sub-sprint, the milestone-shared review covers its commit range; the deliver-agent + human folded the recommended per-sub-sprint review INTO this milestone-shared review rather than dispatching a separate round. **Give #9 the highest scrutiny in this review.** |

Plus one governance-consolidation commit `8f32dbd` (deliver-agent role-doc + governance consolidation; not a sub-sprint dev commit — verify it is docs/compact-only).

**M4-Eval-Cleanup cumulative commit range: `4344662..<S3-dev-commit>`** (= S1 dev `1576070` + S1 close `8ccedbf` + S2 dev `b989833` + S2 close `36ade6e` + governance consolidation `8f32dbd` + S3 dev `<S3-dev-commit>`). Run `git log --oneline 4344662..HEAD` to confirm the exact set at review time. `4344662` ("milestone3 eval governace has been delivered") is the M4-Eval-Cleanup base (exclusive); it is the post-M3-Eval HEAD.

**M4-Eval-Cleanup ships (cumulative artefacts):**

- **Eval-harness polish + R-item closure** (Sprint 47 / S-Cleanup-1; dev `1576070` + close `8ccedbf`):
  - **#1** ENFORCED opt-in for `bad_cases` + `anchor_outcome` via NEW `_OPT_IN_SETS = ("bad_cases", "anchor_outcome")` tuple in `eval_interactive/eval_interactive/batch/sets.py` — `load_set("all")` iterates only `_KNOWN_SETS` (4 implementation-eval sets); the two human-judgment suites (12+12=24 cases) are reachable ONLY via explicit `--set bad_cases` / `--set anchor_outcome`. Per §5.6 these are human-judgment suites that MUST NOT auto-include in `--set all`.
  - **#3** default `parallel` 5→1 at `config.py:85` (dataclass + rationale comment) + `config.py:138` (loader fallback) + `eval_interactive.yaml:29` (yaml align in the close bundle per OQ-S47.1 — the dataclass alone was shadowed by the yaml in typical CLI usage).
  - **#6** `user_goal_achievement` advisory L3 dim seeded on 3 anchor fixtures (cs_interactive_001 escalate UC-C; cs_interactive_004 resolve UC-D; cs_interactive_012 resolve UC-FP).
  - **#7** cs_interactive_095 audit-dispute INVESTIGATION — the audit's "deleted" claim is DISPUTED (file EXISTS at `eval_interactive/case_specs/anchor/cs_interactive_095.yaml`); one genuine docs-only orphan found at `eval_interactive/case_specs/case_families/_manifest.yaml:114` (`target_case_path: smoke/cs_interactive_095.yaml` but the file is at `anchor/`); manifest is documentation-only, not code-consumed (`grep -rn case_families/_manifest --include=*.py` = 0). Surfaced as OQ-S47.2 → NEW R-item.
  - **#8** `test_escalation_enum_sync` migration from the deleted v0_2 spec path to v0_3 markdown-bullet regex extraction of the canonical 23-value `escalation_reason` enum from `docs/current/customer_service_tool_spec_v0_3.md` (sub-helper rename `_load_yaml_enum` → `_load_spec_enum`; test rename `test_yaml_and_*` → `test_spec_and_*`; 3/3 PASS).
  - **#10** compact `--output` cleanup = NO-OP (the active `compact/sprint-deliver-orchestrator.md` is clean; the audit's `--output` finding was in an immutable sprint archive).
  - **R-item flips**: `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` → CLOSED; `R-bad-case-parallel-session-establishment-flakiness` → PARTIAL-CLOSED (parallel default reduction is one mitigation; root-cause investigation deferred). **NEW R-item**: `R-case-families-manifest-cs095-smoke-vs-anchor-orphan` (deferred to M4+).

- **Bad-case fixture schema unification + executor suite-mode** (Sprint 48 / S-Cleanup-2; dev `b989833` + close `36ade6e`):
  - **#5** Alice bad-case fixture schema unification — `alice_uc_a_uc_h_misclass.yaml` `outcome_checks` (7 items → empty) + `llm_judge_dimensions` (3 items → empty) stripped to align with the M3-Eval empty-list schema the other 11 bad cases already carry. Surgical (`git show b989833` = 2 insertions / 12 deletions on the YAML; ONLY the 2 `scoring:` lists touched). **`closure_criterion` + `bad_case_metadata` UNCHANGED** per the §10 STOP-condition preserve-list (these are §5.6 human-judgment-gate inputs). Strip direction (Alice → empty, NOT the 11 cases → grow legacy) was independently verified by dev against 3 archive cells BEFORE the edit; no §10 STOP fired.
  - **#2** executor suite-mode awareness — NEW `CaseSpec.source_suite: Optional[str]` (`schema.py`; backward-compat default None) + `loader.py` parent-dir inference (`path.parent.name`) + NEW `sets.is_human_judgment_suite(suite_name)` helper + NEW module-level `executor._resolve_case_passed_authority(case_spec)` returning `"human_review"` (opt-in suites) / `"programmatic"` (default); `case_passed_authority` emitted on ALL 4 result-builder paths (`_build_case_result` + `_timeout_result` + `_error_result` + `_contract_violation_result`); `run_batch` prints a `Suite type: human_judgment (per §5.6) ...` banner + `_execute_case_sync` renders a per-case `HUMAN_REVIEW {case_id} (programmatic=...)` line for human_review authority. All call sites use `getattr(case_spec, "source_suite", None)` for legacy mock-spec compat. 17 NEW tests (`tests/test_case_passed_authority.py`).
  - **R-item flip**: `R-bad-case-fixture-migrate-to-l3-judge-dims` → CLOSED (Item #5 migrates the LAST legacy fixture; full 12-case suite uniform). **NEW R-items**: `R-bad-case-metadata-field-name-canonicalize` (OQ-S48.1) + `R-eval-report-observability` (merged OQ-S48.2 + OQ-S48.3; deferred).

- **Tier-2 phase-plan-scoped fix + handover_completeness demotion** (Sprint 49 / S-Cleanup-3; dev `<S3-dev-commit>`):
  - **#9** Tier-2 `skill_procedure_followship` phase-plan-scoped evaluation (HIGHEST-SCRUTINY ITEM). Drove from the Tier-2 design memo `docs/solutions/tier2_skill_traversal_design_memo.md` (2026-05-23): the audit's #9 ("Tier-2 traverses all Skills rather than the runtime-selected Skill") was reframed from "document a design choice" to "fix a Tier-2 gate bug" by an OFFLINE reproduction in the memo §3. Human picked **Option (b) phase-plan-scoped evaluation**. NEW module-level `_collect_presented_step_ids(per_turn_trace)` helper in `eval_interactive/eval_interactive/batch/executor.py` returns the union of `phase_plan.critical_steps[].id` across all turns. `BatchExecutor._compute_tier2_result` now (a) computes the presented-id set first, (b) returns the inert `tier2_results_to_gate(())` default when the set is empty (defensive — DO NOT fall back to all-Skills, which would re-introduce the bug), (c) keeps only per-step results whose `step_id` is in the presented set. Per-step N/A-by-`mandatory_for` semantics in `skill_procedure_check.py:extract()` UNCHANGED (the fix is at the executor orchestration layer — which steps are fed to the extractor — not at the per-step evaluator). The step ids come from the runtime's own per-turn projection (the single-source-of-truth `critical_steps` per M3-Eval); the eval side only CONSUMES observable trace state.
  - **#4** `handover_completeness` + `case_id_present` demotion from mandatory-on-escalate to Tier-3 advisory. `composite.py:_conditional_mandatory_l2` now returns `()` for every case (the two dims are no longer added to the mandatory-L2 set on `outcome_class == "escalate"`); the two dead helpers `_CASE_ID_UCS` + `_uc_family` that fed the removed UC-H/J/K branch are deleted (the `outcome_checks.py` copies are intentionally retained — they still COMPUTE the scores for trend reporting). Tier-3 advisory recording is unchanged; the dims are still computed and serialised, just no longer gating. Safety floor unaffected — these were L2 process-completeness checks, never Tier-0 safety (Tier-0 lives in `hard_checks.py`).
  - **Tests**: NEW `eval_interactive/tests/test_tier2_phase_plan_scoping.py` (14 tests, 4 classes: `_collect_presented_step_ids` helper unit ×5; `TestPhasePlanScopedTier2` target/neighbor/negative-multi-phase ×4; `TestDefensiveDefault` empty/absent phase_plan ×4; `TestUcMandatoryForInterplay` ×1). Inverted 2 tests in `test_composite_gate.py` (`test_escalate_does_not_add_handover_completeness` + `test_outcome_class_escalate_handover_completeness_demoted`) + 1 NEW (`test_outcome_class_escalate_case_id_present_demoted`). Renamed 1 test in `test_s_eval_5_l3_repositioning.py` (`..._iterates_all_skills` → `..._aggregates_presented_skills`; seeds both skill ids into `phase_plan.critical_steps`).
  - **#9 empirical (anchor suite, 159 cases, real-LLM, parallel=1)**: before-fix run `20260523-051234` vs after-fix run `20260523-063218` — `escalate-via-request-handover` Tier-2 misflips **119 → 0**; `case_passed=true` count **0 → 11**. The 11 PASS cases have `composite < 0.7` (correctly gated through Tier-2, held back by graded L2/L3). Residual 148 FAILs are legitimate bot-side / contract-trace failure shapes (OQ-S49.1).
  - **R-item**: none closed at S3 (correctness fix + pyramid-intent demotion; neither closes an R-item).

- **Tests + baselines**: Java baseline `1163 / 1-inherited / 0 / 2` UNCHANGED across all three sub-sprints (the 1 inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure since Sprint 24-era persists per OQ-S41.5 STATUS QUO; not M4-attributable; NO `server/` touch in any sub-sprint). Python baseline trajectory: post-M3-Eval `5 failed` (deliver-agent documented env) → S-Cleanup-1 fixes 2 `test_escalation_enum_sync` tests → S-Cleanup-3 `7 failed, 456 passed` (dev env; +15 NEW tests at S3). The 7 dev-env failures are env-specific (2× `test_case_spec_overrides` + 5× `test_corpus_lint` `ModuleNotFoundError: No module named 'eval_interactive.case_spec'`), NOT M4-attributable — see OQ-S47.3 (baseline-framing reconciliation between the milestone objective's "5 fail" and the dev-env "7 fail").

**M4-Eval-Cleanup acceptance bar** (per `docs/milestone_objective.md` §5; cleanup-milestone framing): the §5.6 bad-case suite manual review IS the primary acceptance gate, but M4-Eval-Cleanup is NOT bad-case-driven (no new bad case in scope) — so the bar is **regression-safety**: the post-cleanup rerun at `parallel=1` against milestone-close HEAD should REPRODUCE the M3-Eval close distribution (PASS×5 [cs001, cs014, cs029, cs066, fg5q] + IMPROVING×4 [alice, cs011, cs012, wmkb] + FAIL×3 [cs015, cs095, iwzx] + OOSR×0). A cleanup milestone should NOT change bad-case behaviour; a PASS→FAIL flip is a regression to investigate. **Codex MUST NOT re-interpret §5.6 as a programmatic threshold** — per the 2026-05-17 refinement the bad-case suite is a human-judgment gate where `closure_criterion` is GUIDANCE for human qualitative review, not a binary programmatic match. This milestone-shared review evaluates (a) cumulative scope discipline (all eval-side, zero `server/` touch); (b) the §4.1 nine-question kernel against the cumulative diff (#9 + #4 are the load-bearing items); (c) §1.7 forbidden-list compliance; (d) the M4 §6 hard fences; (e) the R-item flips' correctness — **NOT** a re-judging of the per-case bad-case verdicts (those are deliver-agent + human per §5.6).

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip; later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §1.3 / §1.4 / §1.7 / §3.2 / §4.1 / §4.2 / **§4.3** / §5 / §5.5 / **§5.6** + §5.6.1 + §5.6.2 + §5.6.3 / §7 / **§8**).
2. **`docs/milestone_objective.md`** — the M4-Eval-Cleanup north star. Read end-to-end. Especially: §1 milestone class (S1/S2/S3 layer breakdown); §2 Goal (close the cleanup audit + re-align harness with M3-Eval intent; CLEANUP milestone, no new feature); §3 sub-sprint sequence; §4 non-goals; §5 acceptance bar (8 items); §6 hard fences (11 items); §7 R-items consumed / surfaced; §8 Codex review plan (milestone-shared default; S3 possibly trigger #2); §10 stop conditions; §11 cross-milestone sequencing.
3. **All 3 per-sub-sprint objective + handoff archives** (immutable per `doc_governance.md`):
   - `docs/sprints/sprint-047-objective.md` + `docs/sprints/sprint-047-handoff.md` (S-Cleanup-1).
   - `docs/sprints/sprint-048-objective.md` + `docs/sprints/sprint-048-handoff.md` (S-Cleanup-2).
   - `docs/sprints/sprint-049-objective.md` + `docs/sprints/sprint-049-handoff.md` (S-Cleanup-3; the LIVE dev handoff at milestone close — `docs/sprint_objective.md` is archived to `sprint-049-objective.md` as part of the deliver-agent close-out bundle AFTER this Codex review). The S3 handoff §3 carries the dev's own §4.1 self-walk (clean `approve` per Q1-Q9) + §6 empirical before/after + §10 hard-fence checklist — re-verify, do not merely echo.
4. **`docs/solutions/tier2_skill_traversal_design_memo.md`** — the design memo that drove #9 (LOAD-BEARING). Read end-to-end: §0 TL;DR (the audit's #9 reframed from "document a design choice" to "fix a Tier-2 gate bug"); §3 OFFLINE reproduction of the misflip; the options a/b/c table + the human's Option (b) phase-plan-scoped selection. **The memo is the justification that #9 is correctness-driven (a bug fix), not a discretionary scoring change** — confirm the landed `_compute_tier2_result` change matches Option (b) as the memo describes it.
5. **`docs/current/iteration_governance.md`** — §1 Constitution; §1.3 LLM-owned; §1.4 Runtime-owned; §1.7 Forbidden; §3.2 layer classification; §4.1 nine-question kernel (re-walk against the cumulative diff at §3 below); §4.2 sprint-close header (write per §4.2); §4.3 milestone-shared review provisions + the S3 trigger-#2 analysis; §5 acceptance bars; §5.5 smoke OBSERVATION; **§5.6 bad-case suite primary-gate framing** + §5.6.1 tiering + §5.6.2 per-milestone selection + §5.6.3 downgrade rule; §7 sprint-objective stanza; §8 milestone framework.
6. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set. Verify M4 ADDED no Tier-0 invariant and the #4 demotion did NOT remove a safety floor (the demoted dims are L2 process-completeness, not Tier-0 — confirm against the Tier-0 set + `hard_checks.py`).
7. **`docs/current/faq_grounding_contract.md`** — grounding-floor contract. Verify M4 preserves the FAQ grounding contract (no eval-side change degrades the six output classes / citation diagnostics; the #4 demotion does not touch grounding).
8. **`eval_interactive/case_specs/bad_cases/_manifest.md`** — bad-case suite lifecycle ledger. Read the M3 column verdicts + the NEW "M4-Eval-Cleanup close" section (deliver-agent + human will have added it before this review). **This is the PRIMARY GATE evidence for M4 close per §5.6** — Codex does NOT re-judge per-case PASS/FAIL/IMPROVING but DOES verify the M4-close ledger notes accurately summarize the trace evidence and the verdicts are consistent with what the cited result path contains (spot-check 2-3 cases via `jq` against the bad-case run, suggest `eval_interactive/results/20260523-075141/results.json` or the deliver-agent's milestone-close rerun if a fresher run was used).
9. **`docs/action_bank.md`** — R-item tracker; verify the M4 R-item flips:
   - CLOSED at S-Cleanup-1: `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (#8 markdown-regex migration). PARTIAL-CLOSED: `R-bad-case-parallel-session-establishment-flakiness` (#3 parallel default; root-cause deferred).
   - CLOSED at S-Cleanup-2: `R-bad-case-fixture-migrate-to-l3-judge-dims` (#5 Alice strip — LAST legacy fixture).
   - NEW (opened, deferred): `R-case-families-manifest-cs095-smoke-vs-anchor-orphan` (S1) + `R-bad-case-metadata-field-name-canonicalize` (S2) + `R-eval-report-observability` (S2).
   - Verify each closure annotation references the closing surface + matches the code at HEAD; verify each NEW R-item is correctly scoped + deferred (not smuggling M4+ work into M4).
10. **Code source files for cumulative cited-line spot-checking at HEAD `<S3-dev-commit>`** (read on demand during §3 + §4 + §5; NOT end-to-end):
    - `eval_interactive/eval_interactive/batch/executor.py` (S1 #1-adjacent set wiring + S2 `_resolve_case_passed_authority` + 4 builder paths + **S3 `_collect_presented_step_ids` + `_compute_tier2_result` phase-plan scoping**).
    - `eval_interactive/eval_interactive/batch/sets.py` (S1 `_OPT_IN_SETS` + S2 `is_human_judgment_suite`).
    - `eval_interactive/eval_interactive/scoring/composite.py` (**S3 `_conditional_mandatory_l2` → `()` demotion** + deleted `_CASE_ID_UCS` / `_uc_family`; docstring rationale).
    - `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (`extract()` per-step N/A-by-`mandatory_for` semantics — verify S3 did NOT change this; the fix is executor-orchestration-layer only).
    - `eval_interactive/eval_interactive/scoring/outcome_checks.py` (verify the retained `_CASE_ID_UCS` / `_uc_family` copies still COMPUTE the scores — the demotion is gate-side only, scores still serialised for trend).
    - `eval_interactive/eval_interactive/case_spec/schema.py` + `loader.py` (S2 `source_suite` field + parent-dir inference).
    - `eval_interactive/eval_interactive/config.py` + `eval_interactive/eval_interactive.yaml` (S1 #3 parallel 5→1).
    - `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (S2 #5 strip — verify `closure_criterion` + `bad_case_metadata` intact).
    - `eval_interactive/tests/test_tier2_phase_plan_scoping.py` + `test_composite_gate.py` + `test_case_passed_authority.py` + `test_s_eval_5_l3_repositioning.py` + `test_escalation_enum_sync.py`.

---

## 2. Cumulative scope claim

Walk the cumulative range `4344662..<S3-dev-commit>` and verify each sub-sprint shipped within its declared scope (per the respective `docs/sprints/sprint-NNN-objective.md` archive). For each:

1. **Sprint 47 (S-Cleanup-1)** — `git show 1576070 --stat` + `git show 8ccedbf --stat`; expected: `sets.py` opt-in tuple + `config.py`/`eval_interactive.yaml` parallel default + 3 anchor-fixture `user_goal_achievement` seeds + `test_escalation_enum_sync.py` v0_3 migration + action_bank R-item flips. Verify **no `server/` touch**; no Skill YAML; no runtime.
2. **Sprint 48 (S-Cleanup-2)** — `git show b989833 --stat` + `git show 36ade6e --stat`; expected: Alice fixture strip (2 ins / 12 del; ONLY the 2 `scoring:` lists) + `schema.py`/`loader.py`/`sets.py`/`executor.py` suite-mode wiring + `test_case_passed_authority.py` (17 tests). Verify the Alice strip did NOT touch `closure_criterion` or `bad_case_metadata` (`git show b989833 -- eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`). Verify **no `server/` touch**.
3. **Sprint 49 (S-Cleanup-3)** — `git show <S3-dev-commit> --stat`; expected: `executor.py` (`_collect_presented_step_ids` + `_compute_tier2_result` scoping) + `composite.py` (`_conditional_mandatory_l2` → `()` + dead-helper deletion) + `test_tier2_phase_plan_scoping.py` (NEW, 14 tests) + inverted/renamed tests + the S3 dev handoff. Verify **no `server/` touch**; no Skill YAML `mandatory_for` edit (the fix is in how the eval side CONSUMES `critical_steps`, NOT in the Skill declarations); no `skill_procedure_check.py:extract()` per-step semantics change; no `case_specs/**` fixture edit; no `sets.py` change.
4. **Governance consolidation `8f32dbd`** — `git show 8f32dbd --stat`; expected: docs/compact-only (deliver-agent role doc + governance consolidation). Verify it touched no code and no `iteration_governance.md` §1 / §1.7 / §5.5 / §5.6 wording in a way that crosses the M4 §6 hard fence.

---

## 3. §4.1 nine-question anti-hardcode kernel walk (cumulative)

Walk each of the nine questions across the FULL M4-Eval-Cleanup cumulative range. For each "yes" or each concern, paste the diff snippet (cite commit + path:line) and the reasoning. **#9 (S3 Tier-2 phase-plan-scoping) is the load-bearing item — give it the highest scrutiny.** Particular attention to:

- **Q1 (semantic hardcode added?)**: Is #9's phase-plan-scoping a scope-by-OBSERVABLE-TRACE-STATE change (it keeps only per-step results whose `step_id` is in the union of `phase_plan.critical_steps[].id` the runtime EMITTED), or does it encode a keyword / regex / per-UC matrix? Confirm `_collect_presented_step_ids` reads `phase_plan.critical_steps[].id` (runtime projection, single-source-of-truth per M3-Eval) and contains no UC-name branch, no message-content match, no hardcoded step-id literal that routes by case identity. Is #4 purely a REMOVAL of a mandatory gate (it shrinks the deterministic surface — `_conditional_mandatory_l2` → `()`), and therefore cannot add a hardcode? Is the S1 `_OPT_IN_SETS` tuple a set-membership config (not a semantic decision the LLM owns)?
- **Q2 (Tier-0 invariant protection?)**: did M4 add any new Tier-0 invariant? Expected **NO**. Neither #9 (narrows an existing gate by observable state) nor #4 (demotes existing gates) adds a gate.
- **Q3 (soft signal replacing hard branch?)**: #9 already IS consumption of a soft signal — the runtime's per-turn projection — so the eval side mirrors the LLM-side observable surface rather than duplicating routing. #4 moves two dims from hard gate contributor → advisory (HARD → SOFT direction). Confirm neither introduces a NEW hard branch.
- **Q4 (eval-phrase / trace-specific phrasing / CaseSpec-id encoded?)**: walk #9 + #4 for any reference to a visible-eval case_id, trace-specific phrasing, or CaseSpec id. The `_collect_presented_step_ids` helper should read `phase_plan.critical_steps[].id` generically; verify no case_id literal appears.
- **Q5 (LLM ownership shrunk?)**: does any M4 change shrink what §1.3 says the LLM owns? Expected **NO** — M4 is eval-harness-side; `server/src/main/java/**`, `system_prompt.txt`, and Skill YAMLs are all UNTOUCHED (verify by empty `git diff --stat -- server/` across the range). #9 is eval-side gate correctness; #4 is eval-side gate demotion.
- **Q6 (prompt if-else added?)**: `system_prompt.txt` UNTOUCHED — verify empty diff. No Skill `critical_steps[].desc` edit.
- **Q7 (tool schema / capability / PII / grounding floor preserved?)**: tool schema + capability + PII + grounding floor are all server-side; M4 makes no `server/` change. The #4 demotion only affects the L2 mandatory-set, which sits ABOVE the Tier-0 safety floor on `hard_checks.py` (verify the demoted dims are not in the Tier-0 hard-check set). Grounding floor untouched.
- **Q8 (generalization coverage?)**: target / neighbor / negative / shadow counts. For #9: target = the confirmed UC-A FAQ-resolve misflip (`test_target_resolve_no_escalate_does_not_flip` + the anchor before/after 119→0 delta); neighbor = UC-A escalate session + UC-K resolve session (`TestUcMandatoryForInterplay`); negative = multi-phase session entering BOTH resolve and escalate (`test_negative_multi_phase_session_evaluates_both` + `..._one_failing_step_flips_gate` — proves no over-narrowing); defensive = empty/absent phase_plan (4-test `TestDefensiveDefault`). Shadow = N/A (eval-harness gate logic, not a bot semantic surface). Verify the negative-control tests genuinely prevent over-narrowing.
- **Q9 (rollback / sunset plan?)**: M4 changes are durable (#9 is a correctness fix; #4 is a permanent demotion per M3-Eval pyramid intent). N/A is correct for Q9. `git revert <S3-dev-commit>` reverses #9 + #4 if needed.

---

## 4. M4-Eval-Cleanup §6 hard fences walk

Walk each of the 11 §6 hard fences in `docs/milestone_objective.md` and verify NO violation across the cumulative range. The headline fence is **#1: no `server/src/main/java/**` runtime touch** — verify with `git diff --stat 4344662..<S3-dev-commit> -- server/` (expected: EMPTY).

1. No `server/src/main/java/**` runtime touch (Tier-2 cleanup is eval-side `executor.py` only).
2. No `system_prompt.txt` edit.
3. No Skill YAML edit (`server/src/main/resources/skills/*.yaml` — including no `mandatory_for` edit on the escalate step to "fix" the #9 misflip; the fix is eval-side consumption).
4. No editing of `docs/milestones/M3-Eval_*` (immutable archive).
5. No editing of any `docs/sprints/sprint-NNN-*` files (immutable archives) — only NEW `sprint-047/048/049-*` landed at their respective closes.
6. No new Tier-0 invariant; no `runtime_freeze_and_risk_policy.md` edit.
7. No `iteration_governance.md` §1.7 (Constitution forbidden-list) edit.
8. No `iteration_governance.md` §5.5 / §5.6 wording rewrite EXCEPT a minimal clarification for the #4 handover_completeness Tier-3 status or the Tier-2 design rationale (the dev DEFERRED this to the deliver-agent at close — OQ-S49.2; verify whether the deliver-agent close bundle made a minimal §5.6 edit and, if so, that it is 1-2 sentences and load-bearing, not a rewrite).
9. No new bad cases opened; no `bad_cases/` directory schema-breaking change beyond the S2 Alice unification.
10. No `eval_interactive/case_specs/shadow/` reads (held-out, dev-blind) — verify no S-Cleanup code reads from that path.
11. No M4+ candidate slate work (M3-Eval §12.10 — Single Handover Orchestrator, UC-G/H/I/J seeding, semantic-planner soft-signal, M3-Corpus, Latency/Skill-Tuning/Tier-0) — verify no sub-sprint smuggled M4+ scope.

---

## 5. Bad-case suite manual review verification (regression-safety focus)

Per §5.6 (2026-05-17 refinement) the bad-case suite is a **human-judgment gate**, NOT a programmatic gate. Codex does NOT re-judge per-case PASS/FAIL/IMPROVING. Because M4-Eval-Cleanup is a CLEANUP milestone (not bad-case-driven), the acceptance shape is **regression-safety**: the M4-close distribution should REPRODUCE the M3-Eval close distribution. Codex verifies:

(a) The M4-close verdicts in `eval_interactive/case_specs/bad_cases/_manifest.md` (the NEW M4-Eval-Cleanup-close section the deliver-agent + human add before this review) are consistent with the per-case trace evidence. Spot-check at least 3 cases (suggest 1 PASS + 1 IMPROVING + 1 FAIL — e.g. cs066 PASS / alice IMPROVING / cs015 FAIL) via:
- `jq '.case_results[] | select(.case_id == "<id>") | .per_turn_trace' <bad-case-run>/results.json` (or `.transcript` if present).
- Compare bot reply text against the case YAML `bad_case_metadata.closure_criterion`.
- Verify the deliver-agent's M4-close reasoning accurately summarizes what the trace shows.

(b) **Regression-safety**: confirm no M3-PASS case flipped to FAIL at M4 close. The M3-Eval close distribution was PASS×5 (cs001, cs014, cs029, cs066, fg5q) + IMPROVING×4 (alice, cs011, cs012, wmkb) + FAIL×3 (cs015, cs095, iwzx). A cleanup milestone should not change bad-case behaviour; flag any PASS→FAIL flip as a P0 regression. Note the #9 fix CHANGES the Tier-2 gate logic (which steps are evaluated) — verify via `jq` that no bad case now FAILs on the escalate-step misflip the fix eliminated (`escalate-via-request-handover` should be absent from every bad case's `tier2_result.failed_step_ids`), and that any residual `TIER2:` failures are LEGITIMATE presented-step failures (the runtime DID present the step and the bot did not satisfy it), not new misflips.

(c) The #4 demotion lands operationally on the bad-case suite: spot-check that an escalate bad case with a low `handover_completeness` / absent `case_id_present` does NOT carry a `L2_GATE:handover_completeness` / `L2_GATE_MISSING:case_id_present` failure tag (the dim score is still computed + serialised, just not gating).

If any spot-check surfaces inconsistency between the manifest verdict / claimed evidence and the actual result paths, surface as a P0 finding for re-write before close.

---

## 6. Open question queue routed to M4-Eval-Cleanup-shared close

The S1 and S2 OQs were already disposed at their sub-sprint closes (S1: OQ-S47.1 yaml-align APPROVED + OQ-S47.2 cs095-orphan → R-item + OQ-S47.3 corpus_lint document-only; S2: OQ-S48.1 → R-item + OQ-S48.2/OQ-S48.3 → merged R-item). The OQs still needing milestone-close disposition are the S3 set plus the carry-over baseline-reconciliation:

1. **OQ-S49.1** — Residual 148/159 anchor FAILs post-#9-fix. Source: `docs/sprints/sprint-049-handoff.md` §8. The fix eliminated all 119 escalate-step misflips (119→0) and lifted `case_passed=true` 0→11. The remaining 148 FAILs are legitimate failure shapes (top tags `L2_GATE:correct_outcome` 71×, `L1:source_citation_present` 43×, `CONTRACT_VIOLATION:active_use_case` 42×, `L1:trace_minimum` 33×, `L2_GATE:correct_uc` 32×, `TIER2:search-knowledge-before-faq-answer` 13× down from 20). Codex: confirm these are bot-side / contract-trace issues (NOT eval-side gate bugs) and recommend whether any warrant a follow-on R-item or already track to existing M4+ candidates.
2. **OQ-S49.2** — §5.6 governance text fold-in for the #4 demotion. Source: `docs/sprints/sprint-049-handoff.md` §8. The dev DEFERRED the optional §5.6 clarification (handover_completeness / case_id_present are Tier-3 advisory) to the deliver-agent at milestone close, preferring a handoff note + the code docstring over a §5.6 edit. Codex: assess whether the deliver-agent's chosen disposition (edit vs no-edit) is correct + minimal; if a §5.6 edit was made, verify it is 1-2 sentences and does not cross the §6 #8 fence.
3. **OQ-S49.3** — Composite-threshold-vs-gate reporting. Source: `docs/sprints/sprint-049-handoff.md` §8. The 11 post-fix `case_passed=true` cases all have `composite < 0.7`, so the executor's "Passed: N" stdout label (= `case_passed AND composite >= 0.7`) still reads 0. Codex: confirm this is a reporting-cosmetics observation (correctly gated through Tier-2; held back by graded scores), not a correctness issue, and recommend disposition (likely the existing `R-eval-report-observability` R-item).
4. **OQ-S47.3 (carry-over)** — Python baseline framing reconciliation. Source: `docs/sprints/sprint-047-handoff.md` §7 + S3 handoff §5. The milestone objective §5 bar #4 reads "baseline 5 fail → 4 fail", but the dev-env reality is "7 fail" (the delta = 5× `test_corpus_lint` `ModuleNotFoundError: No module named 'eval_interactive.case_spec'` env-specific failures that surface in both dev + deliver envs). Codex: independently re-run the Python suite at HEAD (`cd eval_interactive && uv run pytest --tb=no -q`) and report the count; recommend whether the milestone-close handoff should reconcile the "5 fail" framing to the env-specific "7 fail" reality (the 5 corpus_lint failures are an environment / packaging issue, not an M4 regression).

For EACH OQ, return:
- Codex assessment (1-3 sentences).
- Recommended disposition: `closed` / `closed-with-followup` / `surface-as-r-item` / `defer-to-future-milestone` / `human-architecture-decision`.

---

## 7. Output format

Write your verdict to `docs/codex-findings.md` at the top of the file (delete-and-add supersession per `feedback_packaging_codex_findings_supersession.md` — the deliver-agent will archive the live file to `docs/milestones/M4-Eval-Cleanup_codex-review.md` after this review). The live `docs/codex-findings.md` is currently the scaffold reset state; you ARE writing the milestone-shared review evidence here. Use the §4.2 4-line sprint-close header convention (milestone-shared review still writes per §4.2):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph — cumulative M4-Eval-Cleanup verdict: scope discipline (zero server/ touch) + §1.7 compliance (#9 scope-by-observable-state, #4 surface-shrink) + §6 hard-fence honor + bad-case regression-safety verification + OQ disposition>
```

Then below the header (the live scaffold lists the full section set — fill the applicable ones):

1. **§3 nine-question kernel results** (one section per question; cite diff snippets for any "yes" or concern; #9 is the load-bearing item).
2. **§4 hard-fence walk results** (one row per fence; PASS / FAIL / N/A; the `git diff --stat -- server/` empty-diff confirmation is the headline).
3. **§5 bad-case regression-safety verification** (per (a)/(b)/(c); spot-check evidence; confirm no PASS→FAIL flip + no new escalate-step misflip).
4. **§6 OQ disposition table** (4 rows; one disposition per OQ).
5. **Schema / reproducibility checks** — spot-check the dev handoff's cited numbers (the 119→0 misflip delta, the 0→11 case_passed delta, the 63-test targeted count, the +15 Python pass-count delta) reproduce from the cited result paths + test runs per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.
6. **Optional Codex-surfaced new findings** (anything not in the OQ queue): surface as P0/P1/P2/P3 with cite to commit:path:line; deliver-agent + human decide disposition post-Codex.
7. **Cumulative architecture coherence judgment** (1-2 paragraphs): does M4-Eval-Cleanup ship as one coherent cleanup (the eval harness re-aligned with M3-Eval intent — opt-in human-judgment suites, uniform bad-case schema, phase-plan-scoped Tier-2, Tier-3 advisory demotion), or are there inter-sub-sprint inconsistencies / dead code / orphaned wiring (e.g., the retained `outcome_checks.py` `_CASE_ID_UCS` / `_uc_family` copies — verify these are intentional score-computation retention, not orphans)?

---

## 8. Anti-pre-decision discipline

Per `feedback_constitution_discipline_vs_planning_anticipation.md`: Codex's job is to assess + surface findings. Do NOT pre-decide whether a finding is M4-blocking vs M4+ deferral — surface the finding + supporting evidence; deliver-agent + human + Codex jointly decide disposition at close. The §4.2 verdict header IS Codex's binding output on whether M4-Eval-Cleanup can close as PASS — but per-finding routing is collaborative.

Per `feedback_packaging_codex_findings_supersession.md`: write the verdict via delete-and-add supersession at the top of `docs/codex-findings.md`.

Do NOT edit any code. Do NOT propose code fixes beyond naming the layer in `docs/current/iteration_governance.md` §3 that the fix should target. Do NOT edit sprint archives (`docs/sprints/*`) or the milestone objective archive that will land at `docs/milestones/M4-Eval-Cleanup_objective.md` (deliver-agent owns that archive at close-out bundle time).
