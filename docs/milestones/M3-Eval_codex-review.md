---
title: M3-Eval Codex milestone-shared review archive — Coarse-to-Fine Evaluation Architecture
doc_tier: milestone-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-23
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Cumulative Codex milestone-shared review per `iteration_governance.md`
  §4.3 default at M3-Eval close 2026-05-23. Codex commit range
  `d91bd3d..7562a2d` (10 commits across S-Eval-1 through S-Eval-5).
  Verdict at the time of writing: `decision: fix_required / blocking_count: 1`
  on P0-F1 deliver-agent evidence-package inconsistency (Tier-0 "12 of 12 PASS"
  sentence in `eval_interactive/case_specs/bad_cases/_manifest.md` contradicted
  by cs001 `escalation_compliance` L1 fail). Deliver-agent applied the
  documentation fix in the manifest M3-Eval close section 2026-05-23 per
  human-accepted Path 2 disposition (documentation-only fix; not re-Codex
  round); the fix's accuracy is recorded in `docs/milestone_objective.md`
  §12.5 (archived to `docs/milestones/M3-Eval_objective.md` as part of M3-Eval
  close-out). NO S-Eval-N code change was requested by Codex. Architectural
  axis verdict: PASS (no §1.7 hardcode; 16 OQs disposed; cumulative coherence
  judgment "M3-Eval is architecturally coherent"). Per-sub-sprint S-Eval-3
  Codex review previously archived at `docs/sprints/sprint-044-codex-review.md`
  per `iteration_governance.md` §4.3 trigger #2 (`pass / 0` first pass).
---

## Sprint Review Decision
decision: fix_required
blocking_count: 1
summary: M3-Eval ships as a coherent coarse-to-fine eval architecture: S-Eval-1 demotes schema/scoring means to advisory surfaces, S-Eval-2/S-Eval-3 create and populate the Skill `critical_steps` Tier-2 contract with structural DSL hardcode defenses, S-Eval-4 expands the bad-case human-judgment gate, and S-Eval-5 makes Tier-3 L3 dims advisory while wiring Tier-2 into the production eval executor. I found no new §1.7 semantic hardcode in the 18 `critical_steps[].desc`, the `trace_check` DSL, the L3 rubric updates, `user_goal_achievement`, or the Option A executor wiring. The close package does, however, need one blocking rewrite before archival: `eval_interactive/case_specs/bad_cases/_manifest.md:178` claims Tier-0 safety floor `12 of 12 PASS` with no `escalation_compliance` failures, but `eval_interactive/results/20260522-110537/results.json` contains `cs001_uc_c_mechanical_template_escalate` with `l1_results[].check="escalation_compliance"` and `passed=false`; this is evidence-packaging inconsistency, not a re-judgment of the human-owned bad-case verdicts. The 16 OQs are disposed below; one additional non-blocking hard-fence packaging exception is surfaced for the documented `v0_2` spec supersession deletion.

## Cumulative Scope Claim
- Sprint 42 / S-Eval-1 (`d91bd3d`) stayed in `eval_spec`: schema demotions, scoring severity/advisory demotions, 12 `anchor_outcome` cases, and regression tests; `git show --stat d91bd3d` shows no Java, Skill YAML, or runtime semantic surface.
- Sprint 43 / S-Eval-2 (`69ed77f`) stayed in the declared schema/projection/extractor scope: `CriticalStep.java`, `Skill.java`, `SkillLoader.java`, `ContextProjectionBuilder.java`, `skill_procedure_check.py`, and structural-defense tests; the DSL rejects regex / keyword / message-content by positive grammar and blocklist at `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:139` and `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:260`.
- Sprint 44 / S-Eval-3 (`01770ac`) populated only `critical_steps` content plus tests/handoff; 18 steps across the 6 Skill YAMLs remain row-1 soft procedural narratives, matching the archived per-sub-sprint Codex pass at `docs/sprints/sprint-044-codex-review.md`.
- Sprint 45 / S-Eval-4 (`8b7ff40`) added 11 bad-case YAMLs plus manifest calibration notes; parsed `closure_criterion` strings contain no `case_passed`, `composite_score`, regex, keyword, or visible CaseSpec-id coupling.
- Sprint 46 / S-Eval-5 (`e0cd8aa`, `7562a2d`) stayed in `eval_spec`: L3 advisory demotion, new advisory `user_goal_achievement`, narrative rubric updates, Option A executor wiring, R-item annotations, tests, and a docs-only monotone-rerun follow-up.
- Validation: `UV_CACHE_DIR=/tmp/uv-cache uv run --offline python -m pytest` returned `5 failed, 426 passed in 13.34s`, matching the known post-S-Eval-5 Python baseline under the correct `python -m pytest` runner discipline.

## §3 Nine-Question Kernel Results

### Q1 — Semantic hardcode added?
Verdict: PASS.

Yes, M3-Eval adds LLM-visible and judge-visible semantic surfaces, but the additions are soft, declarative, or structurally constrained rather than keyword / regex / if-else hardcodes.

- `critical_steps[].desc`: 18/18 re-walk as proposal §5.3 row 1. Examples: `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:115` anchors UC-FP removal explanations on `get_moderation_review_context`, and `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:94` anchors UC-K handover on canonical intake fields. These are tool/field/process narratives, not message keyword branches.
- DSL structural defense: `69ed77f:eval_interactive/eval_interactive/scoring/skill_procedure_check.py:15` documents exactly six primitives; `69ed77f:eval_interactive/eval_interactive/scoring/skill_procedure_check.py:139` names hardcode-flavored fragments; `69ed77f:eval_interactive/eval_interactive/scoring/skill_procedure_check.py:329` rejects unknown primitives. The negative tests at `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py:36` cover 15 regex / keyword / message-content examples.
- L3 rubric updates: `e0cd8aa:eval_interactive/eval_interactive/scoring/llm_judge.py:137` explicitly frames citation-quality tightening as narrative, not per-UC or keyword enumeration; `e0cd8aa:eval_interactive/eval_interactive/scoring/llm_judge.py:229` does the same for `form_context.first_name` trust.
- `user_goal_achievement`: `e0cd8aa:eval_interactive/eval_interactive/scoring/llm_judge.py:271` anchors the dim on `persona.user_goal_summary` and records it advisory-only; no keyword or UC matrix is introduced.
- Option A executor wiring: `e0cd8aa:eval_interactive/eval_interactive/batch/executor.py:365` iterates loaded Skills and delegates semantic contract evaluation to the DSL-constrained extractor; it does not inspect message text or add runtime branching.

### Q2 — Tier-0 invariant protection?
Verdict: PASS.

M3-Eval adds no new Tier-0 invariant and does not edit `docs/runtime_freeze_and_risk_policy.md`. The milestone objective itself says no new Tier-0 elevation is in scope at `docs/milestone_objective.md:248`; C2/C3 remain deferred. The manifest Tier-0 evidence overclaim is a close-package consistency problem, not a new Tier-0 invariant.

### Q3 — Soft signal replacing hard branch?
Verdict: PASS.

The direction is mostly hard-to-soft: S-Eval-1 demotes hard CaseSpec/scoring means, and S-Eval-5 excludes advisory L3 dims from the gate via `eval_interactive/eval_interactive/scoring/composite.py:229`. Tier-2 is a new hard gate, but it is intentional M3-Eval architecture: `eval_interactive/eval_interactive/scoring/composite.py:206` only gates on failed critical Tier-2, while `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:730` reduces mandatory vs advisory step results under a constrained grammar.

### Q4 — Eval phrase, trace-specific phrasing, or CaseSpec-id encoded?
Verdict: PASS.

I found no `critical_steps[].desc` reference to bad-case IDs, smoke/anchor IDs, persona names, or visible eval phrasing. The S-Eval-5 citation-quality rubric includes example opaque IDs at `eval_interactive/eval_interactive/scoring/llm_judge.py:169`, but the prompt labels them examples of a semantic failure pattern, not regex anchors or acceptance lists.

### Q5 — LLM ownership shrunk?
Verdict: PASS.

No runtime semantic ownership is moved out of the LLM. `ContextProjectionBuilder` renders only `{id, desc}` at `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:889`; it does not expose `trace_check`, `severity`, or `mandatory_for` to the runtime LLM. The LLM still owns the semantic read; eval owns the deterministic check of the trace after the fact.

### Q6 — Prompt if-else added?
Verdict: PASS.

No `system_prompt.txt` edits landed. S-Eval-3 did not edit existing Skill `procedure` text; it appended `critical_steps` blocks after existing fields, for example `server/src/main/resources/skills/confirm.yaml:31` and `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:54`. Some descs use ordinary prose like "If during UC-FP resolution..." at `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:130`, but that is semantic procedural guidance, not a `user.message.contains(...) THEN active_use_case := ...` prompt branch.

### Q7 — Tool schema / capability / PII / grounding floor preserved?
Verdict: PASS.

Tool schema and runtime capability surfaces are unchanged in the M3-Eval dev commits; the canonical escalation enum remains 23 values in Python at `eval_interactive/eval_interactive/case_spec/schema.py:47` and Java at `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:43`. Grounding is strengthened on the eval side by the citation-quality rubric at `eval_interactive/eval_interactive/scoring/llm_judge.py:163`, which treats bare internal IDs as non-actionable citations.

### Q8 — Generalization coverage?
Verdict: PASS with future-work notes.

Target coverage includes the 4 demoted L3/R-item surfaces, the new `user_goal_achievement` dim, Option A Tier-2 wiring, and 18 populated Skill steps. Neighbor coverage is the 12-case bad-case rerun through the new scoring path, plus the 185-case smoke / anchor_outcome / anchor monotone rerun documented at `docs/sprints/sprint-046-handoff.md:969`. Negative coverage includes retained critical L3 dims (`stall_quality`, `premature_finish`) at `eval_interactive/eval_interactive/scoring/llm_judge.py:74` and empty-`critical_steps` advisory parity at `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:735`. Shadow fixtures stayed untouched; the fixture gap for L3 dims is real and correctly surfaced as `R-bad-case-fixture-migrate-to-l3-judge-dims` at `docs/action_bank.md:762`.

### Q9 — Rollback / sunset plan?
Verdict: N/A.

M3-Eval is intended as a permanent four-tier evaluation architecture, not a temporary guard. No sunset trigger is required; future work is normal milestone follow-up on R-items and fixture migration.

## §4 Hard-Fence Walk Results

| # | Result | Evidence |
|---:|---|---|
| 1 | PASS | Empty diff for `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, and `ClassifyUseCaseTool.java` in `git diff --name-only d91bd3d^..7562a2d`; fence source `docs/milestone_objective.md:247`. |
| 2 | PASS | No `docs/runtime_freeze_and_risk_policy.md` edit; no Tier-0 elevation; M3 objective prohibits new Tier-0 invariants at `docs/milestone_objective.md:248`. |
| 3 | PASS | Empty diff for `eval_interactive/case_specs/case_families/<existing>/`; fence source `docs/milestone_objective.md:249`. |
| 4 | PASS | Empty diff for `eval_interactive/case_specs_shadow/case_families/<existing>/`; fence source `docs/milestone_objective.md:250`. |
| 5 | PASS | Empty committed-range diff for existing `eval_interactive/case_specs/smoke/` and `eval_interactive/case_specs/anchor/`; current dirty worktree changes in those dirs are outside the reviewed commits. |
| 6 | PASS | `alice_uc_a_uc_h_misclass.yaml` is not touched in `d91bd3d^..7562a2d`; Alice is rerun only. |
| 7 | PASS | `eval_interactive/case_spec_overrides.yaml` has empty committed-range diff; S-Eval-4 only reads it. |
| 8 | PASS | No edits under `eval_interactive/eval_interactive/loader/` or `eval_interactive/eval_interactive/simulator/`; S-Eval-1 edits `case_spec/loader.py`, which is not this fenced path. |
| 9 | FAIL, documented exception | `db19a47` deletes `docs/customer_service_tool_spec_v0_2.yaml` despite fence `docs/milestone_objective.md:255`. S-Eval-2 close records this as human-directed v0_2 supersession landing at `docs/sprints/sprint-043-handoff.md:546`, and `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` explicitly says not to restore v0_2 at `docs/action_bank.md:746`. Surface as non-blocking packaging exception P2-F2 below. |
| 10 | PASS | Empty diff for sprint archives `sprint-001-*` through `sprint-041-*` and milestone archives named in `docs/milestone_objective.md:256`. |
| 11 | PASS | Escalation enum remains canonical 23 values in Python at `eval_interactive/eval_interactive/case_spec/schema.py:47` and Java at `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:43`; no widening found. |
| 12 | PASS | `iteration_governance.md` diff is only the S-Eval-1 `anchor_outcome` §5.5 sentence, matching the allowed exception; see `docs/current/iteration_governance.md:498`. |
| 13 | PASS | No `SkillRegistry.select`, `SkillStateBus.applyOnSkillSwitch`, or `SkillGuardrailDispatcher.dispatch` edit. Skill YAML changes add `critical_steps` only; existing `procedure`, `guardrails`, and `state_inheritance` content are not modified. |
| 14 | PASS | The DSL has no regex / keyword / message-content primitive: blocklist at `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:139`, positive grammar at `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:317`, and negative tests at `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py:36`. |
| 15 | PASS | 18 `critical_steps[].desc` entries remain soft procedural narratives; representative starts at `server/src/main/resources/skills/discover_triage.yaml:34`, `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:55`, `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:40`, and the archived S-Eval-3 Codex verdict says 18 row-1 / 0 row-3 / 0 row-4 in `docs/sprints/sprint-044-codex-review.md`. |

## §5 Bad-Case Suite Verification Results

### (a) Manifest verdicts vs trace evidence
- PASS spot-check, `cs001_uc_c_mechanical_template_escalate`: the human verdict that the primary mechanical-template anti-pattern is avoided is consistent with the transcript in `eval_interactive/results/20260522-110537/results.json`; T0/T1 engage the Replies/Messaging issue instead of opening with the old "I'm having difficulty resolving this" template. Blocking caveat: the same result has `case_passed=false` and `l1_results[].check="escalation_compliance"`, `passed=false`, contradicting the manifest's blanket Tier-0 statement at `eval_interactive/case_specs/bad_cases/_manifest.md:178`.
- IMPROVING spot-check, `cs012_uc_fp_late_phone_failure_path`: isolated rerun `eval_interactive/results/20260522-162847/results.json` reaches a multi-turn UC-FP-flavored guidance path with a help URL, while the late phone/callback branch is not exercised. The manifest reasoning at `eval_interactive/case_specs/bad_cases/_manifest.md:161` is consistent.
- FAIL spot-check, `cs015_uc_fp_appeal_edit_repost`: main run `eval_interactive/results/20260522-110537/results.json` shows UC-A misroute and mechanical handover without policy explanation; manifest FAIL reasoning at `eval_interactive/case_specs/bad_cases/_manifest.md:163` is consistent.
- FAIL/R-item spot-check, `iwzx_uc_k_advert_on_hold_restore`: isolated rerun `eval_interactive/results/20260522-162850/results.json` shows `active_use_case=UC-H`, `escalation_reason=user_distress`, and Tier-2 failures including `uc-h-intake-complete-before-handover`; this matches the manifest reasoning at `eval_interactive/case_specs/bad_cases/_manifest.md:168` and action-bank R-item at `docs/action_bank.md:760`.

### (b) New R-item scope checks
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress`: correctly scoped as `semantic_planner`; the failure is the LLM's UC/action commitment, not a schema/tool capability change. Evidence: `docs/action_bank.md:760` and isolated rerun `eval_interactive/results/20260522-162850/results.json`.
- `R-bad-case-parallel-session-establishment-flakiness`: correctly scoped as `infra`; main parallel=4 run has truncated/OOSR shapes for `cs012`, `fg5q`, `iwzx`, while parallel=1 isolated reruns complete multi-turn sessions. Evidence: `docs/action_bank.md:761`, `eval_interactive/results/20260522-110537/results.json`, `eval_interactive/results/20260522-162847/results.json`, `eval_interactive/results/20260522-162848/results.json`, `eval_interactive/results/20260522-162850/results.json`.
- `R-bad-case-fixture-migrate-to-l3-judge-dims`: correctly scoped as `eval_spec` fixture migration. `rg -l "llm_judge_dimensions: []" eval_interactive/case_specs/bad_cases/*.yaml` returns 11 files; only Alice opts into L3 dims at `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml:154`. Evidence: `docs/action_bank.md:762`.

### (c) S-Eval-5 rubric operational spot-check
- Alice L3 result in `eval_interactive/results/20260522-110537/results.json` matches the manifest's operational claim at `eval_interactive/case_specs/bad_cases/_manifest.md:182`: `tone_appropriateness=5.0`, `groundedness=3.0`, both `severity="advisory"`.
- The observed scores validate both narrative rubric updates without making them hard gates: advisory severity is defined at `eval_interactive/eval_interactive/scoring/llm_judge.py:62` and filtered out of the judge-score gate at `eval_interactive/eval_interactive/scoring/composite.py:240`.

## §6 OQ Disposition Table

| OQ | Codex assessment | Recommended disposition |
|---|---|---|
| OQ-S42.3 | The demotion is correct: when `escalation_trigger` is absent, family-level reason matching is a soft signal, while hard outcome/gate checks should remain on canonical Tier-1/Tier-2 surfaces. This preserves trace diagnostics without forcing brittle exact-match family assertions. | closed |
| OQ-S43.1 | The positive grammar is the real blocklist: unknown primitives are rejected even if not named in `_HARDCODE_BLOCKLIST`. Keep future additions limited to trace-structure primitives; do not add any `conversation`, `transcript`, `message`, `regex`, or semantic-intent primitive. | closed-with-followup |
| OQ-S43.4 | HEAD does not count `success=false` dispatches: `TraceView.successful_tool_events` filters `call.get("success", False)` at `eval_interactive/eval_interactive/scoring/skill_procedure_check.py:463`, and the regression test at `eval_interactive/tests/test_skill_procedure_extractor.py:136` pins that behavior. This is the right design. | closed |
| OQ-S44.6 | Confirmed. With sandbox cache workaround, `UV_CACHE_DIR=/tmp/uv-cache uv run --offline python -m pytest` returns `5 failed, 426 passed`; failures are the known v0_2 supersession/override/corpus-lint baseline, not S-Eval-5 regressions. | closed |
| OQ-S45.1 | The M3 manual ledger is consistent for the three legacy-migrated cases: `wmkb` IMPROVING, `iwzx` FAIL with new R-item, `fg5q` PASS after isolated rerun. | closed |
| OQ-S45.3 | Dual-encoding `570Q5000008U5C9IAK` as `cs095` bad-case FAILURE and `anchor_outcome_uc_a_visibility` SUCCESS is coherent: they are different eval artefacts for different desired reads of the same source session. Keep both unless future dedupe tooling cannot tolerate source-session reuse. | human-architecture-decision |
| OQ-S45.4 | The proposed wording is better for future authoring: "filler reply that does not name a concrete next step or trigger a tool call" is less example-bound than "placeholder". Do not rewrite M3 cases now unless the close package is already being edited for P0-F1. | closed-with-followup |
| OQ-S45.5 | Consumed by S-Eval-5 Option A. `executor.py` computes Tier-2 at `eval_interactive/eval_interactive/batch/executor.py:292` and passes `tier2_result=` into `compute_composite` at `eval_interactive/eval_interactive/batch/executor.py:301`. | closed |
| OQ-S45.6 | Consumed by S-Eval-5 §13: `consult-moderation-context-on-removal-explanation` does not appear in the anchor POST failed-step list, per `docs/sprints/sprint-046-handoff.md:1035`. No S-Eval-3 content fix is indicated. | closed |
| OQ-S46.1 | Resolved by `7562a2d` docs and local result-path spot-check: smoke, anchor_outcome, and anchor runs all had 0 PASS before and after, so true→false transitions are 0. This is vacuous on L3 but still confirms no Tier-2 wiring surprise. | closed |
| OQ-S46.2 | Iterating every Skill is acceptable as eval-side broad coverage because `mandatory_for` produces N/A outside active UC, and `executor.py:373` explicitly avoids duplicating Java `SkillRegistry.select` semantics. If trend noise grows, revisit with an expected-active-Skill trace field. | human-architecture-decision |
| OQ-S46.3 | Double `_build_per_turn_trace` is a minor efficiency/readability smell, not architectural risk. | defer-to-future-milestone |
| OQ-S46.4 | The 30-test file is acceptable: it pins L3 demotion, advisory filtering, rubric updates, executor wiring, R-item annotations, and monotone proof. Consolidation would mostly reduce useful specification density. | closed |
| OQ-S46.5 | Per-advisory-dim means would improve trend visibility, especially now L3 no longer gates. This is reporting-layer follow-up, not M3 architecture debt. | defer-to-future-milestone |
| OQ-S46.6 | `stall_quality` may become redundant with L1 stall detection, but it remains one of the two critical L3 dims after S-Eval-5 and should be demoted only after a focused M4+ signal-consolidation review. | surface-as-r-item |
| OQ-S46.7 | Generalization is correct: smoke/anchor/anchor_outcome do not opt into `user_goal_achievement`, and bad cases also leave L3 dims empty except Alice. `R-bad-case-fixture-migrate-to-l3-judge-dims` captures the bad-case side; a broader fixture-migration R-item may be needed for smoke/anchor/anchor_outcome. | surface-as-r-item |

## Optional Codex-Surfaced New Findings

### P0-F1 — Bad-case manifest Tier-0 blanket contradicts `cs001` result evidence
- Evidence: `eval_interactive/case_specs/bad_cases/_manifest.md:178` says `Tier-0 safety floor: 12 of 12 PASS` with no `escalation_compliance` fails.
- Counter-evidence: `jq '.case_results[] | select(.case_id == "cs001_uc_c_mechanical_template_escalate") | .l1_results[] | select(.check == "escalation_compliance")' eval_interactive/results/20260522-110537/results.json` returns `passed=false` and detail `expected='faq_miss_threshold_exceeded'`, `actual='user_requested'`.
- Why this blocks archival: this is not a re-judgment of the human-owned `cs001` PASS verdict; the primary closure anti-pattern can still be considered avoided. The blocker is that the close evidence asserts a Tier-0 all-pass fact that the cited result file disproves. Rewrite or qualify the Tier-0 sentence before archiving the M3-Eval close package.

### P2-F2 — Hard-fence #9 has a documented close-out exception that should be explicit in the close package
- Evidence: hard fence #9 at `docs/milestone_objective.md:255` says no edits to `docs/customer_service_tool_spec_v0_2.yaml`, but `db19a47` deletes `docs/customer_service_tool_spec_v0_2.yaml` and `docs/customer_service_tool_spec_v0_2.md`.
- Mitigating context: S-Eval-2 close records the deletion as human-directed supersession landing at `docs/sprints/sprint-043-handoff.md:546`; `docs/action_bank.md:746` opens the stale enum-sync migration R-item and explicitly says not to restore v0_2 to make tests pass.
- Recommendation: do not restore the superseded v0_2 files. Instead, make the M3 close package explicitly label this as a documented hard-fence exception / inherited supersession housekeeping item so the §6 walk does not claim a literal zero-exception pass.

## Cumulative Architecture Coherence Judgment

M3-Eval is architecturally coherent. The five sub-sprints compose into the intended four-tier pyramid: Tier-0 stays unchanged; Tier-1 gains outcome-only anchors and human-owned bad-case manual review; Tier-2 is derived from Skill `critical_steps` with a single-source-of-truth `desc` consumed by both runtime projection and eval extraction; Tier-3 judge/scoring polish is preserved as advisory trend data. The strongest coherence point is the S-Eval-2/S-Eval-3/S-Eval-5 chain: Java renders only `{id, desc}` to the LLM at `ContextProjectionBuilder.java:889`, Python evaluates only constrained `trace_check` forms at `skill_procedure_check.py:15`, and the production executor passes the resulting `tier2_result` into `compute_composite` at `executor.py:301`.

I did not find dead code or orphaned wiring in the cumulative architecture. The remaining issues are close-package evidence hygiene and future coverage work, not architectural hardcode: the manifest Tier-0 overclaim needs correction before archival, the v0_2 deletion needs explicit hard-fence-exception packaging, advisory L3 fixture adoption remains thin outside Alice, and `stall_quality` vs L1 stall detection deserves future signal-consolidation review. None of those changes requires touching runtime semantic code for M3-Eval.
