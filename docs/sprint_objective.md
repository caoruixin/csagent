---
title: Sprint 46 (NEW M3-Eval sub-sprint 5; LAST M3-Eval sub-sprint; S-Eval-5) — L3 judge repositioning + R-item closure
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file (until S-Eval-5 close, then archived to docs/sprints/sprint-046-objective.md)
last_reviewed: 2026-05-22
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-045-objective.md]
superseded_by: null
notes: >
  Sprint 46 is the FIFTH and LAST sub-sprint of NEW Milestone M3-Eval
  (Coarse-to-Fine Evaluation Architecture; see `docs/milestone_objective.md`).
  After S-Eval-5 close, M3-Eval closes via milestone-shared Codex review
  per `iteration_governance.md` §4.3 default + curated bad-case suite
  manual review as PRIMARY GATE per §5.6.

  S-Eval-4 (Sprint 45) closed Clean PASS 2026-05-22 (dev commit
  `8b7ff40`; Codex deferred to M3-Eval milestone-shared close; archive
  at `docs/sprints/sprint-045-objective.md` + `docs/sprints/sprint-045-handoff.md`).
  11 new bad-case YAMLs landed (12 total in suite incl. Alice);
  fallback path (a) authorized for UC-G/H/I/J gap with new R-item
  `R-bad-case-suite-uc-ghij-seed-from-real-sessions` opened for M4+
  planning. Option A (synthetic) dry-run posture taken at S-Eval-4.

  **S-Eval-5 is the L3 judge repositioning + R-item closure sub-sprint.**
  It (a) demotes 3 current L3 dimensions (`relevance`,
  `tone_appropriateness`, `groundedness`) from composite contributors
  to Tier-3 advisory; (b) adds NEW `user_goal_achievement` L3 dimension
  as Tier-1 supplementary advisory signal; (c) updates rubric prompt
  text to close R-l3-judge-form-context-trust-rubric +
  R-l1-source-citation-quality-rubric; (d) closes R-cs040-l3-review-intake-completion-semantics
  + R-cs038-l3-review-intake-efficiency via the S-Eval-1 schema
  simplification route. Includes a mandatory **monotone-relaxing
  check** (no previously-PASS case flips to FAIL on the new rubric).

  **Codex review plan**: milestone-shared at M3-Eval close per §4.3
  default (no per-sub-sprint trigger fires for S-Eval-5 by default).
  Codex consumes the cumulative S-Eval-1 → S-Eval-5 commit range at
  M3-Eval close.

  **S-Eval-4 carry-overs consumed at S-Eval-5 planning round**:
  - **OQ-S45.5 executor wiring decision (LOAD-BEARING)**: the
    populated `critical_steps` content from S-Eval-3 remains
    structurally INERT in the production eval-harness path because
    `eval_interactive/eval_interactive/batch/executor.py:252` does
    NOT pass `tier2_result=` to `compute_composite(...)`. **S-Eval-5
    planning round MUST decide** whether to broaden S-Eval-5 scope
    to include the wiring fix (small change at `executor.py:252`)
    OR carry to M3-Eval close as part of milestone close artefacts.
    Deliver-agent's read (NOT pre-decision; dev + human decide at
    planning round): broadening S-Eval-5 is the cleanest path
    because the S-Eval-5 monotone-relaxing rerun would then double
    as the first real-LLM Tier-2 surface (consuming OQ-S44.5 +
    OQ-S45.6 simultaneously). Carrying to M3-Eval close is the
    smaller-scope option but means M3-Eval close inherits the
    wiring work + zero real-LLM Tier-2 evidence accumulated through
    S-Eval-5.
  - **OQ-S45.6 UC-FP moderation calibration via real-LLM run**: if
    executor wiring lands in S-Eval-5 per the above decision, the
    monotone-relaxing rerun is the natural calibration surface for
    OQ-S44.3 `consult-moderation-context-on-removal-explanation`
    mandatory step tightness (potential S-Eval-3 fix-iteration if
    a legitimate UC-FP no-context path surfaces). If wiring deferred,
    calibration deferred to M3-Eval close real-LLM pass.
  - **Python baseline runner discipline (OQ-S44.6)** preserved: dev
    MUST use `uv run python -m pytest` (NOT `uv run pytest`) per
    OQ-S44.6 carry-over from S-Eval-3.

  **Bundle policy**: dev ships in ONE bundle commit per
  `feedback_commit_at_end_bundles_deliver_artefacts.md`. Dev does
  NOT stage deliver-agent close-out files (sprint_objective archive,
  10-handoff §1 lead refresh, codex-findings stays scaffold, action_bank
  Sprint 46 row + 4 R-item closures + potential M3-Eval close
  housekeeping).

  **Cross-session continuity**: if a new dev-agent picks this up cold,
  the dev prompt at `compact/sprint-046-dev-prompt.md` is the entry
  point. Read order: AGENTS.md (auto-loaded) → this file →
  docs/milestone_objective.md (M3-Eval context, §3 S-Eval-5 row;
  §6 hard fences; §8 Codex review plan) → proposal §6 S-Eval-5
  (scope detail) → docs/sprints/sprint-045-handoff.md §12 closure
  verdict (carry-over context) → eval_interactive/eval_interactive/scoring/llm_judge.py
  (current L3 dimensions + rubric structure).
---

# Sprint 46 (NEW M3-Eval sub-sprint 5; LAST M3-Eval sub-sprint; S-Eval-5) — L3 judge repositioning + R-item closure

## 1. Sub-sprint class

**Single-track semantic-touching sub-sprint, layer `eval_spec` per `iteration_governance.md` §3.2 Q6 (judge config + rubric narrative).** §7 stanza REQUIRED. Codex review **milestone-shared at M3-Eval close** (default per §4.3; no per-sub-sprint trigger fires for S-Eval-5 by default).

Class breakdown:

- **Primary layer**: `eval_spec` (judge config + rubric) — repositioning 3 existing L3 dims to Tier-3 advisory + wiring NEW `user_goal_achievement` L3 dim as Tier-1 supplementary advisory + 2 rubric prompt text edits + 4 R-item closures.
- **§1.7 forbidden-list adjacency**: **NO** — S-Eval-5 ships judge config + rubric narrative wording; the rubric updates close existing R-items (R-l3-judge-form-context-trust, R-l1-source-citation-quality) via additive trust signals + tighter citation requirements, NOT keyword/regex/per-UC-matrix encoding. The 3 dim demotions + 1 new dim addition are dimension-weighting + wiring changes, NOT semantic hardcodes.
- **Tier-0 invariant claim**: no new Tier-0 invariant added.
- **Monotone-relaxing check REQUIRED**: this is a non-trivial property — running smoke + anchor + anchor_outcome with both old and new scoring code as part of S-Eval-5 acceptance to verify no previously-PASS case flips to FAIL on the new rubric.

## 2. Goal

Ship the L3 judge repositioning per `docs/milestone_objective.md` §3 S-Eval-5 + close 4 M3-Eval R-items:

### 2.1 L3 dimension repositioning (per milestone §3 S-Eval-5 sentence 1)

In `eval_interactive/eval_interactive/scoring/llm_judge.py`:

- **Demote** `relevance` from composite contributor to Tier-3 advisory.
- **Demote** `tone_appropriateness` from composite contributor to Tier-3 advisory.
- **Demote** `groundedness` from composite contributor to Tier-3 advisory.

Demoted dim numeric scores are STILL RECORDED in `case_results[].judge_scores`; they no longer factor into `case_passed` or `composite_score` (per S-Eval-1 D-2.5 `severity` field convention demoting to "advisory" pattern).

### 2.2 NEW L3 dimension `user_goal_achievement` (per milestone §3 S-Eval-5 sentence 2)

Add a NEW L3 dimension `user_goal_achievement` (coarse 1-5 score: did the bot help the user achieve the stated `persona.user_goal_summary`, or appropriately escalate / defer?). Wire as **Tier-1 supplementary advisory signal** (NOT a hard gate; bad-case + anchor_outcome manual review remain primary Tier-1 gates per `iteration_governance.md` §5.6 + milestone §5).

Rubric design: coarse 1-5 with clear anchors (1 = clear failure / wrong UC / unresolved; 3 = adequate / problem named + appropriate next step; 5 = clear success / user goal observably achieved OR appropriate escalation taken). The dim feeds into per-case `judge_scores.user_goal_achievement` + a per-run trend metric; does NOT flip `case_passed`.

### 2.3 Rubric prompt updates (per milestone §3 S-Eval-5 sentence 3; closes 2 R-items)

Update the L3 rubric prompts (location embedded in `llm_judge.py` OR a sibling rubric file; dev decides at planning round per scope):

- **Close `R-l3-judge-form-context-trust-rubric`**: update the rubric to explicitly state that `form_context.first_name` (from the case persona / pre-populated form context) is a TRUSTED signal; the bot is allowed to greet by first name without confirmation. The bot greeting "Hi {first_name}" should NOT be penalised under `tone_appropriateness` for "unauthorized familiarity" — `form_context.first_name` is canonical pre-known identity.
- **Close `R-l1-source-citation-quality-rubric`**: tighten the citation check in L1 source-citation evaluation to require `canonical_url` OR article title in the bot's citation; reject bare Salesforce IDs (e.g., `ka44J000000gKxqQAE`) as sole citation evidence. The rubric should explicitly call this out as a failure pattern: "citation by Salesforce ID alone is not user-actionable; require `canonical_url` from the FAQ corpus OR a human-readable article title".

### 2.4 R-item closures (per milestone §3 S-Eval-5 sentence 4)

Close 4 M3-Eval R-items in `docs/action_bank.md` §6 close-action index:

- **`R-l3-judge-form-context-trust-rubric`** — closed via §2.3.
- **`R-l1-source-citation-quality-rubric`** — closed via §2.3.
- **`R-cs040-l3-review-intake-completion-semantics`** — closed via S-Eval-1 schema simplification route (its Sprint 21 schema-block dependency was lifted by S-Eval-1 D-1.x; the demotion of 3 L3 dims to Tier-3 advisory + the new tiered architecture together resolve the original concern: cs_040 intake-completion semantics no longer rely on a hard-gate L3 dim, and the demotion makes the per-case scoring monotone-relaxing).
- **`R-cs038-l3-review-intake-efficiency`** — closed via the same S-Eval-1 schema simplification route (Sprint 21 schema-block lifted; cs_038 intake-efficiency no longer relies on hard-gate L3 dim under the new tiered architecture).

For each R-item closure: append a `succeeded-by` annotation in `docs/action_bank.md` §6 close-action index referencing the Sprint 46 close commit + this S-Eval-5 work; do NOT delete the R-item entries from the action_bank (preserve historical record per §6 convention).

### 2.5 Monotone-relaxing check (LOAD-BEARING)

Verify the L3 repositioning is **monotone-relaxing** — no previously-PASS case flips to FAIL on the new rubric. Run smoke (14 cases) + anchor (159 cases) + anchor_outcome (12 cases) with BOTH old and new scoring code; diff the `case_passed` field across the two runs. Expected: zero `case_passed=true → false` transitions. Record per-case delta in handoff §9 + diff table.

If any `case_passed=true → false` transition surfaces, dev STOPS per §10 #5 and surfaces — deliver-agent + human decide whether the transition is (a) acceptable because the L3 dim was concealing a genuine failure (rare; documented at close); (b) needs fix-iteration to preserve PASS state (typical); (c) needs the dim demotion to revert (very rare).

### 2.6 Executor wiring fix (Option A AUTHORIZED at S-Eval-5 planning round 2026-05-22)

Per `docs/milestone_objective.md` §3 S-Eval-4 carry-over + S-Eval-4 handoff §12.7: the populated `critical_steps` content from S-Eval-3 remains structurally INERT in the production eval-harness path because `eval_interactive/eval_interactive/batch/executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)`. **At S-Eval-5 planning round 2026-05-22, human authorized Option A**: land the executor wiring fix as part of the S-Eval-5 bundle. Rationale: the S-Eval-5 monotone-relaxing rerun doubles as the first real-LLM Tier-2 surface, consuming OQ-S44.5 (real-LLM Tier-2 evidence) + OQ-S44.3+S44.4 (UC-FP moderation calibration) + OQ-S45.5+S45.6 simultaneously, avoiding the degenerate M3-Eval close state where milestone-shared Codex review would have zero real-LLM Tier-2 evidence to evaluate.

**Required change**: `eval_interactive/eval_interactive/batch/executor.py:252` — pass `tier2_result=` to `compute_composite(...)` (one-line edit). Add 1-2 NEW Python tests verifying the wiring (Tier-2 mandatory fail flips production `case_passed`; advisory fail does not; empty `critical_steps` parity preserved).

**Calibration follow-on**: if the monotone-relaxing rerun (per §2.5) surfaces a legitimate UC-FP no-context path that escalates or defers instead of answering (contradicting the S-Eval-3 mandatory step `consult-moderation-context-on-removal-explanation` per OQ-S44.3 / OQ-S45.6 carry-over), dev STOPS per §10 #2 and surfaces — deliver-agent + human decide whether to trigger S-Eval-3 fix-iteration (loosen `trace_check` OR downgrade severity to advisory).

## 3. Non-goals (explicit)

S-Eval-5 does NOT:

1. **Change L3 model temperature or provider** (deferred per Sprint 4; M3-Eval scope).
2. **Retrain or replace the judge model** (out of M3-Eval scope).
3. **Modify L3 prompt structure beyond rubric wording for the 4 R-items** (additive trust signal + tighter citation requirement; NO structural rewrite).
4. **Modify any Tier-0 / Tier-1 hard-gate code** (those are S-Eval-1 territory; cascade fence).
5. **Modify the populated `critical_steps` content in 6 Skill YAMLs** (S-Eval-3 territory; cascade fence).
6. **Modify any case fixture** under smoke / anchor / anchor_outcome / case-families / bad-case / shadow / `case_spec_overrides.yaml` (cascade fence).
7. **Modify Skill abstraction** (M2 + S-Eval-2 + S-Eval-3 Java + `ContextProjectionBuilder.java`; M3-Eval §6 #13 inherits).
8. **Modify runtime semantic surfaces** (`RuntimeIntentClassifier.java`, etc.; M2 §6 #5 + M3-Eval §6 #1 inherits).
9. **Add new Tier-0 invariants.** Milestone-level §6 #2 hard fence.
10. **Add any runtime if-else / keyword matrix / per-UC enumeration / regex anywhere in rubric updates.** §1.7 red line. Rubric updates use additive trust signals (`form_context.first_name` trusted) + tighter requirement (citation needs canonical_url OR title), NOT keyword enumeration matrices or per-UC branches.
11. **Touch governance docs** (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`), foundational docs, or sprint / milestone archives.
12. **Touch `docs/codex-findings.md`** (review-agent territory; stays scaffold during S-Eval-5; Codex milestone-shared at M3-Eval close writes here).
13. **Touch `docs/milestone_objective.md`** (deliver-agent territory).
14. **Address `R-bad-case-suite-uc-ghij-seed-from-real-sessions`** (NEW R-item opened at S-Eval-4 close; deferred to M4+ planning per §5 governance-track backlog).
15. **Reset or modify the bad-case suite expansion from S-Eval-4** (cascade fence; bad cases re-run as regression guards at M3-Eval close).
16. **(STRUCK at planning round 2026-05-22 per §2.6)** ~~Modify the executor wiring at `executor.py:252`~~ — **Option A AUTHORIZED**; `executor.py:252` IS in scope per §5 (one-line edit + 1-2 NEW tests).

## 4. Premise check (deliver-agent verified 2026-05-22)

- ✅ `eval_interactive/eval_interactive/scoring/llm_judge.py` exists at HEAD `8b7ff40` and carries 3 L3 dimensions (`relevance`, `tone_appropriateness`, `groundedness`) as composite contributors. Dev verifies the dimension list + scoring path at planning round.
- ✅ Rubric prompt text location: TBD by dev (likely embedded in `llm_judge.py` constants OR a sibling rubric module). Dev locates at planning round.
- ✅ S-Eval-1 D-2.5 `severity` field convention established (S-Eval-1 demoted `hard_checks.no_forbidden_tools` + `outcome_checks.tool_sequence_match` + `escalation_reason_consistency` to advisory via `severity="advisory"` field on `HardCheckResult` / `OutcomeCheckResult`). S-Eval-5 reuses this pattern for L3 dim demotion.
- ✅ The 4 M3-Eval R-items (`R-l3-judge-form-context-trust-rubric` / `R-l1-source-citation-quality-rubric` / `R-cs040-l3-review-intake-completion-semantics` / `R-cs038-l3-review-intake-efficiency`) exist in `docs/action_bank.md` and are flagged for closure at S-Eval-5.
- ✅ Monotone-relaxing check infrastructure: existing smoke + anchor + anchor_outcome + case_families + bad_cases all load through `case_set_manager`; dev can run with old vs new scoring code side-by-side.
- ✅ Executor wiring carry-over verified: `eval_interactive/eval_interactive/batch/executor.py:252` does NOT pass `tier2_result=` to `compute_composite(...)` (S-Eval-3 §7 OQ-S44.1 reproduction; S-Eval-4 OQ-S45.5 confirmation).

## 5. Files in scope (Sprint 46 dev ships)

**L3 judge repositioning + new dim:**

- `eval_interactive/eval_interactive/scoring/llm_judge.py` EDIT: demote 3 existing dims to Tier-3 advisory + add NEW `user_goal_achievement` dim wiring + update rubric prompts for R-l3-judge-form-context-trust + R-l1-source-citation-quality.
- Optional sibling rubric file edit (if rubric prompts live separately).
- `eval_interactive/eval_interactive/scoring/composite.py` EDIT (if needed; dim demotion may require composite-side adjustment per S-Eval-1 `severity` pattern).

**Regression tests:**

- NEW Python tests for the demoted dims (still recorded; advisory severity; not flipping `case_passed` or `composite_score`).
- NEW Python tests for the `user_goal_achievement` dim (Tier-1 supplementary advisory wiring; 1-5 score range; persona.user_goal_summary anchor).
- NEW Python test for the monotone-relaxing property (run smoke + anchor with old vs new scoring code; assert zero `case_passed=true → false` transitions).
- NEW Python tests for the rubric prompt updates (R-l3-judge-form-context-trust trust signal; R-l1-source-citation-quality canonical_url-or-title requirement).

**R-item closures:**

- `docs/action_bank.md` EDIT — for each of the 4 M3-Eval R-items, append a `succeeded-by: <S-Eval-5 close commit>` annotation in §6 close-action index (do NOT delete entries; preserve historical record).

**Executor wiring fix (Option A AUTHORIZED at planning round 2026-05-22 per §2.6):**

- `eval_interactive/eval_interactive/batch/executor.py` line 252 EDIT (1-line: pass `tier2_result=` to `compute_composite(...)`).
- 1-2 NEW Python tests for the wiring fix (verify Tier-2 mandatory results flow through to production `case_passed`; advisory fails do not flip; empty `critical_steps` parity preserved).

**Handoff document:**

- `docs/sprints/sprint-046-handoff.md` (dev-authored at sub-sprint close); follows 12-section template per Sprint 35 / 41 / 42 / 43 / 44 / 45 precedent.

## 6. Files NOT in scope (hard fences)

**1. Tier-0 / Tier-1 hard-gate code — NO touch:**

- `eval_interactive/eval_interactive/scoring/hard_checks.py` (Tier-0; S-Eval-1 territory).
- `eval_interactive/eval_interactive/scoring/outcome_checks.py` (Tier-1 / Tier-3; S-Eval-1 territory).
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (Tier-2; S-Eval-2 territory).

**2. CaseSpec schema + loader — NO touch (S-Eval-1 territory):**

- `eval_interactive/eval_interactive/case_spec/schema.py`.
- `eval_interactive/eval_interactive/case_spec/loader.py`.

**3. Skill abstraction (M2 + S-Eval-2 + S-Eval-3) — NO touch:**

- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` (all Java Skill files).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`.
- `server/src/main/resources/skills/` (all 6 Skill YAMLs; S-Eval-3 populated content UNCHANGED).
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java`.

**4. Runtime semantic surfaces — NO touch (M2 §6 #5 + M3-Eval §6 #1 inherits):**

- `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`.
- `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `ControlKernel.java`.
- `system_prompt.txt`, `tool-policy.yaml`.

**5. Eval case fixtures — NO touch (cascade fence per milestone §6 #3-#7):**

- Any case under `eval_interactive/case_specs/smoke/` (14 cases).
- Any case under `eval_interactive/case_specs/anchor/` (159 cases).
- Any case under `eval_interactive/case_specs/anchor_outcome/` (12 cases).
- Any case under `eval_interactive/case_specs/case_families/<existing>/` (12 directories).
- Any case under `eval_interactive/case_specs/bad_cases/` (12 cases: 1 Alice + 11 S-Eval-4 new).
- Shadow CaseSpecs under `eval_interactive/case_specs_shadow/`.
- `eval_interactive/case_spec_overrides.yaml` (read-only).

**6. Eval harness / loader / simulator — NO touch EXCEPT `executor.py:252` per Option A AUTHORIZED §2.6:**

- `eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`.
- `eval_interactive/eval_interactive/batch/executor.py` — line 252 IS in scope per Option A authorization 2026-05-22 (one-line `tier2_result=` pass-through + 1-2 NEW tests). NO other touch to this file or to `batch/` siblings.

**7. Governance and archives — NO touch:**

- `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/customer_service_tool_spec_v0_3.md`.
- `docs/current/iteration_governance.md` (one exception per milestone §6 #12: if S-Eval-5 surfaces a §5 acceptance-bar refinement need that's narrowly scoped, deliver-agent + human may discuss separately; default NO touch).
- `docs/sprints/sprint-001-*` through `docs/sprints/sprint-045-*`.
- `docs/milestones/M1_objective.md`, `M2_objective.md`, `M2-Skill_objective.md`, `M2_codex-review.md`.
- `docs/milestone_objective.md` (M3-Eval; deliver-agent territory).
- `docs/codex-findings.md` (review-agent territory; stays scaffold during S-Eval-5).

**8. Action bank — limited touch:**

- `docs/action_bank.md` §6 close-action index: dev appends 4 R-item closures (`succeeded-by` annotations) per §2.4. Dev MAY also append a Sprint 46 close-action row in §6 at sub-sprint close per existing convention.
- `docs/action_bank.md` §5 governance-track backlog: NO touch on `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (NEW R-item opened at S-Eval-4 close; deferred to M4+).

## 7. Bundle policy

S-Eval-5 dev ships in **ONE bundle commit** containing:

- `llm_judge.py` EDIT + optional rubric file edit + optional `composite.py` EDIT.
- NEW Python regression tests for demoted dims + new dim + monotone-relaxing + rubric updates.
- `docs/action_bank.md` EDIT (4 R-item closures + optional Sprint 46 row).
- **`executor.py:252` EDIT + 1-2 NEW tests (REQUIRED per Option A §2.6 authorization 2026-05-22).**
- Dev handoff `docs/sprints/sprint-046-handoff.md`.

Expected: 6-10 files in the bundle (smaller than S-Eval-4's 14; one file added vs original 5-9 estimate to reflect Option A executor wiring).

Deliver-agent + human bundle (separately, post-close):

- This sub-sprint contract archive (`docs/sprints/sprint-046-objective.md`).
- Codex review at M3-Eval close (milestone-shared per §4.3 default).
- Live `docs/sprint_objective.md` replaced with M3-Eval close planning placeholder OR next-milestone (M4+) contract.
- `docs/10-handoff.md` §1 lead refresh.
- `docs/codex-findings.md` stays scaffold until M3-Eval close (when Codex milestone-shared writes here; then delete-and-add archive to `docs/milestones/M3-Eval_codex-review.md`).
- `docs/action_bank.md` Sprint 46 close-action row (if dev did not already append) + M3-Eval close artefacts (§6.5 closed-milestone index row).
- M3-Eval objective archive to `docs/milestones/M3-Eval_objective.md` (at M3-Eval close).

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent close-out files; human bundles at deliver-agent's commit.

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `eval_spec` (judge config + rubric narrative).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The L3 dim repositioning is a weight adjustment; the new `user_goal_achievement` dim is a Tier-1 supplementary advisory signal (NOT a Tier-0 invariant).

**Semantic hardcode:** **No semantic hardcode introduced.** The 3 dim demotions + 1 new dim addition are dimension-weighting changes (per S-Eval-1 `severity` field convention); the 2 rubric updates are narrative wording changes (additive trust signal + tighter requirement), NOT keyword/regex/per-UC-matrix encoding.

**Sunset plan**: N/A — L3 repositioning is intended permanent (the new tiered architecture is the M3-Eval north star per `docs/milestone_objective.md` §2 Tier-0/Tier-1/Tier-2/Tier-3 pyramid).

**Generalization coverage:**

- **Target**: 3 demoted L3 dims + 1 NEW `user_goal_achievement` dim + 2 rubric updates + 4 R-item closures + monotone-relaxing check verified across smoke (14) + anchor (159) + anchor_outcome (12).
- **Neighbor**: 12 bad cases (1 Alice + 11 S-Eval-4 new) re-run as regression guards under new rubric; expected unchanged `case_passed` per monotone-relaxing property.
- **Negative**: a non-affected case (e.g., a smoke case where L3 dims didn't dominate) must not have its `case_passed` flip under new rubric; verified by monotone-relaxing check.
- **Shadow**: 3-5 cases held out of dev visibility per `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md`; S-Eval-5 dev does NOT read shadow; deliver-agent + review-agent run shadow as part of M3-Eval close monotone-relaxing verification.

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (sub-sprint close PASS only if all clear):**

- [ ] 3 L3 dims (`relevance`, `tone_appropriateness`, `groundedness`) demoted to Tier-3 advisory; numeric scores still recorded; do NOT flip `case_passed` or `composite_score`.
- [ ] NEW `user_goal_achievement` L3 dim landed with 1-5 score range + persona.user_goal_summary anchor; wired as Tier-1 supplementary advisory (does NOT flip `case_passed`).
- [ ] Rubric prompt updates: R-l3-judge-form-context-trust trust signal + R-l1-source-citation-quality canonical_url-or-title requirement landed.
- [ ] 4 M3-Eval R-items closed in `docs/action_bank.md` §6 close-action index (`succeeded-by` annotations).
- [ ] **Monotone-relaxing check PASS**: 0 `case_passed=true → false` transitions when smoke + anchor + anchor_outcome run with old vs new scoring code.
- [ ] Java baseline preservation: pre-S-Eval-5 baseline `1163 / 1-inherited / 0 / 2` (S-Eval-4 close). S-Eval-5 adds 0 Java code; expected baseline UNCHANGED.
- [ ] Python baseline preservation: pre-S-Eval-5 baseline `5 failed / 396 passed` via `uv run python -m pytest` (S-Eval-4 close per OQ-S44.6 runner discipline). S-Eval-5 expected to add 7-17 NEW tests (demoted dims + new dim + monotone-relaxing + rubric updates + REQUIRED executor wiring per §2.6 Option A); NO new Python test failures introduced.
- [ ] **Executor wiring fix landed (Option A §2.6)**: `eval_interactive/eval_interactive/batch/executor.py:252` passes `tier2_result=` to `compute_composite(...)`; 1-2 NEW Python tests verify Tier-2 mandatory results flow through to production `case_passed`; empty `critical_steps` parity preserved.
- [ ] Numbers-cite discipline (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`): every count in handoff (file count, test count, line count, R-item closures, monotone-relaxing transitions) reproducible from `git show --numstat <commit>` / `wc -l` / `grep -c` / test-run direct output. Python baseline citations MUST use `uv run python -m pytest`.

**Observation-only (recorded; does not gate close):**

- Smoke composite_score / pass-rate / judge dimensions deltas pre/post rubric (recorded for M3-Eval close cumulative tracking).
- Architecture-health metric direction: `new_semantic_hardcode_count` = 0; `soft_signal_conversion_count` UNCHANGED at +18 from S-Eval-3 close; `planner_ownership_ratio` not decreased.
- Per-dim score distribution shifts under new rubric (informational only).
- Option A executor wiring decision outcome (taken / not taken at planning round; recorded with rationale).

## 10. Stop conditions (dev-agent)

Dev STOPS and surfaces to deliver-agent + human (instead of pressing on) if any of:

1. **(STRUCK at planning round 2026-05-22 per §2.6)** ~~Option A executor wiring fix authorisation needed~~ — **Option A AUTHORIZED**; the wiring fix is in scope per §5; no additional authorization needed. (Stop signal preserved for future-history reference only; dev does NOT need to surface this STOP.)
2. **Rubric prompt structure rewrite needed** (e.g., the existing rubric structure cannot accommodate the new trust signal OR new dim without a structural rewrite). §3 #3 says rubric updates are narrative-only; structural rewrite is out of scope. Surface; deliver-agent + human decide whether to authorize structural rewrite OR rephrase the rubric updates within current structure.
3. **L3 model temperature / provider / model change needed** to make the new rubric land. §3 #1+#2 says these are out of scope. Surface.
4. **A populated `critical_steps` content edit needed** in any of the 6 Skill YAMLs to make the new rubric land. §3 #5 hard fence. Surface.
5. **Monotone-relaxing check FAILS** (any `case_passed=true → false` transition surfaces). Halt + surface; deliver-agent + human decide per §2.5 (a/b/c options).
6. **Any case fixture edit needed** to make tests pass. §3 #6 hard fence. Surface.
7. **Java or Python baseline regresses** (any new test failure introduced by S-Eval-5 beyond the inherited `SystemPromptUserRequestedTiebreakerTest:53` and the 5 pre-existing Python failures per OQ-S44.6 baseline). Halt and surface; do NOT commit.
8. **Any file under "Files NOT in scope" §6** needs touching. (`executor.py:252` per Option A §2.6 is in scope per §5; not a hard fence violation. Any OTHER file under §6 needing touch IS a hard fence violation; halt and surface.)
9. **R-item closure path unclear** for any of the 4 M3-Eval R-items (e.g., closing `R-cs040-l3-review-intake-completion-semantics` requires more than the S-Eval-1 schema simplification route per §2.4). Surface; deliver-agent + human decide whether to defer the closure to M3-Eval close OR a follow-on R-item.

## 11. Handoff document contract (12 sections per Sprint 35 / 41 / 42 / 43 / 44 / 45 shape)

`docs/sprints/sprint-046-handoff.md` must include:

§1 Sprint identity (Sprint 46 / S-Eval-5 / M3-Eval sub-sprint 5; LAST M3-Eval sub-sprint).
§2 Summary (one paragraph).
§3 Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`). **Per OQ-S44.6 carry-over**: validate sub-tally arithmetic by re-summing per-file numstats; the cumulative "Bundle total" count should match `git show --stat` file count exactly.
§4 Per-change content map (3 dim demotions + 1 new dim + 2 rubric updates + 4 R-item closures + optional Option A executor wiring).
§5 Rubric prompt update content (the actual narrative wording for R-l3-judge-form-context-trust + R-l1-source-citation-quality; surface for deliver-agent + human + Codex review).
§6 §4.1 anti-hardcode self-walk verdicts (Q1-Q9 with one-line justification each).
§7 Open questions (OQ-S46.N format; non-blocking decisions for deliver-agent + human at close).
§8 Generalization coverage filled (target / neighbor / negative / shadow counts per §8 stanza).
§9 Validation runs:
- Java test suite (`mvn test`): baseline vs pre-S-Eval-5 (1163).
- **Python test suite (`uv run python -m pytest`)** per OQ-S44.6 carry-over: baseline vs pre-S-Eval-5 (5/396).
- **Monotone-relaxing check**: smoke (14) + anchor (159) + anchor_outcome (12) run with old vs new scoring code; diff table of `case_passed` transitions (expected 0 `true → false`).
- Per-dim score distribution pre/post (informational).
- If Option A executor wiring taken: Tier-2 results flow through to composite verified; UC-FP moderation calibration outcome.
§10 Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention).
§11 Bundle policy honored (dev shipped one commit; deliver-agent close-out files NOT staged).
§12 Closure verdict (LEFT EMPTY by dev per `feedback_handoff_verdict_section_delegation.md`).

## 12. M3-Eval milestone context (cross-reference, not Sprint 46 contract)

- `docs/milestone_objective.md` is the live M3-Eval milestone objective. Dev SHOULD load it on cold start. ESPECIALLY §3 S-Eval-5 row + §5 milestone acceptance bar + §6 hard fences + §7 R-items consumed (4 R-items S-Eval-5 closes) + §8 Codex review plan (milestone-shared at M3-Eval close).
- `docs/solutions/m3_eval_milestone_proposal.md` is the research-agent proposal source. **LOAD-BEARING sections for S-Eval-5**: §6 S-Eval-5 (scope detail) + §2 four-tier pyramid (L3 dim repositioning context) + §5 decision-5 deep-dive on the LLM-visible content (informs whether rubric updates can stay within §1.7-compliant narrative).
- `docs/sprints/sprint-045-handoff.md` is the S-Eval-4 dev archive. LOAD-BEARING sections: §12.7 closure verdict carry-overs (executor wiring decision LOAD-BEARING; UC-FP moderation calibration; Python baseline runner discipline).
- `eval_interactive/eval_interactive/scoring/llm_judge.py` — current L3 judge surface (dimensions, rubric prompts location TBD).
- `eval_interactive/eval_interactive/scoring/composite.py` — composite scorer (may need dim-weighting adjustment per S-Eval-1 `severity` pattern).
- After S-Eval-5 close, M3-Eval closes via milestone-shared Codex review per §4.3 default + curated bad-case suite manual review as PRIMARY GATE per §5.6.
