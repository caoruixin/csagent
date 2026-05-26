# Deliver-agent context handoff — Sprint 41 pre-launch (NEW M2 sub-sprint 5; LAST M2 sub-sprint contract + dev prompt drafted; pending human commit + Sprint 41 dev session)

**Authored:** 2026-05-18 by deliver-agent (Sprint 40 close session)
**For:** the next deliver-agent instance picking up after Sprint 41 dev session has committed (OR helping the human decide pre-commit edits to the Sprint 41 contract + dev prompt + M2 close planning round)
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD:** `9130abc` (Sprint 40 dev commit; A — Clean PASS closed 2026-05-18; deliver-agent Sprint 40 close housekeeping bundle + Sprint 41 launch round NOT committed yet at handoff authoring time)

Read order on cold start: this file → `AGENTS.md` (loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition; long-term doctrine — **not duplicated here**) → `docs/milestone_objective.md` (NEW M2 contract; §11 cross-milestone sequencing for M3 planning post-M2-close) → `docs/sprint_objective.md` (Sprint 41 design freeze contract) → `compact/sprint-041-dev-prompt.md` (the dev brief Claude Code consumes) → `docs/sprints/sprint-040-handoff.md` + `docs/sprints/sprint-040-codex-review.md` (Sprint 40 close archives) → `docs/proposals/skill_registry_design.md` §10 (Sprint 37 freeze decision (i), the architectural source-of-truth Sprint 41 implements).

---

## 1. Background (Why this session happened)

Previous deliver-agent session handoff at `compact/context-handoff-sprint-040-pre-launch.md` (2026-05-18 afternoon) — that handoff covered the Sprint 40 launch round (Sprint 39 close housekeeping batch 1 + Sprint 40 contract drafting batch 2). Sprint 40 dev session ran across 2026-05-18 same-day and committed `9130abc`:

- 5 files shipped (1 NEW main + 2 EDIT main + 1 NEW test + 1 EDIT test + 1 NEW handoff; 639 insertions / 25 deletions per `git show --stat 9130abc`).
- `system_prompt.txt` EDIT (101 → 80 lines, −21); `discover_triage.yaml` EDIT (30 lines structure unchanged; `procedure` extended ~2900 → ~7900 chars); NEW `SkillTeachingMigrationIntegrationTest.java` (264 lines; 11 tests); EDIT `PhaseEvaluatorSkillIntegrationTest.java` (341 → 400 lines; +59; OQ-S40.1 5th-file Sprint 39 precedent); 312-line dev handoff.
- Per-file numstat: `312/0, 2/23, 1/1, 60/1, 264/0`.
- Java baseline 1105 / 1 inherited / 0 / 2 (1094 post-S39 + 11 new = 1105).
- Sprint 37 freeze decision (f) §7 honored verbatim (Sprint 23 + decision tree PRESERVED; Sprint 31 + Sprint 33 SLOT description + DISCOVER guidance MIGRATED; envelope-mechanics paragraph + DISCOVER pointer COMBINED at post-Sprint-40 lines 30-31 per OQ-S40.2 dev judgment).
- Sprint 33 cue/slot split closed at Sprint 40.

Codex per-sub-sprint review per `iteration_governance.md` §4.3 trigger #3 returned `decision: pass / blocking_count: 0` on first pass (single round). All 5 OQ verdicts (S40.1 through S40.5) + 2 contract-drift verdicts aligned with deliver-agent + human pre-decisions (deliver-agent withheld dispositions from the review prompt per "surface OQs without recommendations" framing). Codex 3 non-blocking observations: (a) per-file numstat estimate lapse (continuation of Sprint 39 `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`); (b) "byte-for-byte" should be "formatting-normalized content equivalence"; (c) Sprint 37 design doc §7.8 typo (OQ-S40.5) joins M2 close fold-back queue.

Sprint 40 final classification: **A — Clean PASS** (M2 implementation-sub-sprint pattern: S37 PASS A → S38 B-fix-iterated → S39 PASS A → S40 PASS A; third clean A close under NEW M2; second clean A on an implementation sub-sprint).

THIS session entered (2026-05-18 evening) for **Sprint 40 close housekeeping + Sprint 41 launch round**:

- **Batch 1 (Sprint 40 close housekeeping)** — completed this session:
  - `docs/codex-findings.md` reset to scaffold (archived Sprint 40 Codex verdict to `docs/sprints/sprint-040-codex-review.md`).
  - NEW `docs/sprints/sprint-040-codex-review.md` (verbatim archive of Sprint 40 Codex output per `feedback_packaging_codex_findings_supersession.md` delete-and-add pattern).
  - NEW `docs/sprints/sprint-040-objective.md` (Sprint 40 contract archive; frontmatter `status: archived` + `doc_tier: sprint-archive`).
  - `docs/sprints/sprint-040-handoff.md` §12 closure verdict filled (the placeholder dev left per `feedback_handoff_verdict_section_delegation.md`).
  - `docs/10-handoff.md` §1 lead refreshed (Date → 2026-05-18; Current → M2 Sprint 40 closed PASS A / Sprint 41 next pre-planning; Preceding → Sprint 40 detailed; Earlier → Sprint 39 compressed; Sprint 38 falls off the §1 lead's 3-sub-sprint slot).
  - `docs/action_bank.md` updates: Sprint 40 close-action row in §6 (inserted between Sprint 39 and Sprint 32 rows); `D-skill-runtime-framework` partial-landing annotation extended to Sprint 40 (Sprints 37 + 38 + 39 + 40 shipped; Sprint 41 named as LAST continuation surface); Sprint 37 design doc §7.8 typo (OQ-S40.5) noted as joining M2 close fold-back queue.
- **Batch 2 (Sprint 41 launch round)** — completed this session:
  - `docs/sprint_objective.md` REPLACED with Sprint 41 contract (~460 lines).
  - NEW `compact/sprint-041-dev-prompt.md` (the dev brief for Claude Code; ~320 lines).
  - NEW `compact/context-handoff-sprint-041-pre-launch.md` (THIS file).

Per the milestone framework, Sprint 41 is back to bigger scope (similar shape to Sprint 39's multi-fence convergence) — multi-file Java + 6 Skill YAML edits + NEW state-bus class + NEW projection slot + Skill-switch integration. Strong STOP discipline locked in §10 of contract + §4 of dev prompt:
- NO per-UC-pair branch in SkillStateBus body (registry-driven intersection only).
- NO new classifier for UC-switch detection (rides on existing M1 Sprint 32 + Sprint 33 projections).
- NO per-UC-pair `state_inheritance` declarations in Skill YAMLs (principle-level per §10.5 matrix).
- NO Tier-0 invariant added (C3 evidence surfaces but DEFER; deliver-agent + human + Codex jointly classify at Sprint 41 close OR M2 close).
- NO scope creep into M3 surfaces (RuntimeIntentClassifier / DriftDetector / UseCaseRouter / INTAKE_UCS / escalation_reason / IntakeFieldsRegistry behaviour all preserved).

---

## 2. Goals

### Long-term

Help the human cycle through the milestone framework: human gives architectural theme / scope → deliver-agent drafts `docs/milestone_objective.md` + per-sub-sprint `docs/sprint_objective.md` + dev/review prompts → human reviews + approves → dev/review agents execute externally → deliver-agent helps human classify close / targeted-fix / OOSR / next-sub-sprint.

For M2 specifically: Sprint 37 + Sprint 38 + Sprint 39 + Sprint 40 are CLOSED; Sprint 41 (UC switch + state preservation; LAST M2 sub-sprint per §10) is the active surface. After Sprint 41 closes → M2 milestone close (milestone-shared cumulative Codex review + design doc fold-back + M2 archive + M3 planning round).

### Immediate (this cycle, Sprint 41 launch round)

1. **Wait for human commit** of the deliver-agent batch 1 + batch 2 bundle (see §3.4 for file list + suggested commit message in §9 below).
2. **After commit + Sprint 41 dev session launch:** Sprint 41 dev (Claude Code via `compact/sprint-041-dev-prompt.md`) produces:
   - NEW `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` (~150-250 lines).
   - 6 Skill YAML EDITs (each declares `state_inheritance` block per §10.5 matrix).
   - `Skill.java` / `StateInheritance.java` / `SkillLoader.java` verification + minor edits if schema drift.
   - `ContextProjectionBuilder.java` EXTEND (NEW `prior_use_case_carry` slot; ~30-50 new lines).
   - `PhaseEvaluator.java` (or `ControlKernel.java`) EDIT (Skill-switch detection + bus invocation).
   - NEW `SkillStateBusTest.java` + `UcSwitchStateInheritanceTest.java` + `PriorUseCaseCarryProjectionTest.java` (~35-50 NEW tests).
   - EDIT `PhaseEvaluatorSkillIntegrationTest.java` if applicable.
   - VERIFY `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED.
   - (Optionally D-g) one-line orchestration-shell teaching about `prior_use_case_carry` in `system_prompt.txt`.
   - `docs/sprints/sprint-041-handoff.md` NEW (12-section dev-authored archive).
3. **After Sprint 41 dev commit:** deliver-agent + human review Sprint 41 handoff + Java test results. Deliver-agent drafts `compact/sprint-041-review-prompt.md` + dispatches Codex per-sub-sprint review per §4.3 trigger #3 (session state model touch across Skill boundary — load-bearing for cross-Skill state preservation; C3 Tier-0 candidate evidence surface).
4. **After Sprint 41 close:** **M2 MILESTONE CLOSE** — deliver-agent + human + Codex (milestone-shared cumulative review per §4.3 second paragraph) jointly close M2. See §5.4 below.

### Subsequent (M3 planning post-M2-close)

Per `docs/milestone_objective.md` §11 cross-milestone sequencing context (unchanged from Sprint 40 → Sprint 41 transition):

- **M3-A**: Customer-honesty surface (sibling rationale/confidence fields on `request_handover`; handover UX rewrite; URL policy).
- **M3-B**: Lifecycle (`D-single-handover-orchestrator` P0 + scoped `D-full-issue-ledger`).
- **M3-C**: Sprint 5 S3/S4/S5 skills (rides validated M2 Skill abstraction).
- **M3-D**: Topic↔UC binding loosening (`R-loosen-topic-uc-binding-llm-owned-drift` requires research-agent investigation).
- **M3-Latency**: consume `R-llm-provider-latency-drift-2026-05-16` (deferred from M2).
- **M3-Skill-Tuning**: post-M2 behavioural tuning of individual Skills (e.g., Alice closure-criterion (a) PASS if not achieved organically through abstraction landing).
- **M3-Tier-0 re-evaluation**: C2 (guardrail refusal non-overridability) + C3 (state-bus boundary enforcement) Tier-0 candidates — Sprint 39 shipped C2's FIRST observed evidence surface; Sprint 41 will ship C3's FIRST observed evidence surface; re-evaluate at M2 close OR M3+.

---

## 3. Confirmed facts (code-grounded; verify before relying)

### 3.1 Git state at handoff authorship (2026-05-18 evening)

```
9130abc sprint 40: Teaching extraction from system_prompt.txt (Sprint 31/33 SLOT + DISCOVER guidance + envelope mechanics) (NEW M2 sub-sprint 4)
2f412b6 sprint 39: RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 + unified dispatcher (NEW M2 sub-sprint 3)
5787806 sprint 38 fix #1: SkillLoader schema-validation completeness for design doc §2.2 (close Codex Blocking Finding 1)
bd9d3f5 sprint 38: SkillRegistry core + 4 simpler phase Skills migration (NEW M2 sub-sprint 2)
649c6c7 sprint 37 close: PASS A + Sprint 38 contract + dev prompt draft (NEW M2 sub-sprint 1→2)
```

Working-tree modifications at handoff authoring time (deliver-agent Sprint 40 close housekeeping + Sprint 41 launch bundle + carry-over pending commit):

**From pre-Sprint-40 (batch 1 + batch 2 of Sprint 40 launch; carry-over since Sprint 39 close housekeeping was not committed before Sprint 40 dev):**

```
M docs/10-handoff.md                                (§1 lead refreshed multiple times across Sprint 39/40 close)
M docs/action_bank.md                               (multiple updates across Sprint 39/40 close housekeeping)
M docs/codex-findings.md                            (reset to scaffold across Sprint 39/40 close)
M docs/sprint_objective.md                          (REPLACED Sprint 39 → Sprint 40 → Sprint 41 contract)
?? docs/sprints/sprint-038-codex-review.md          (Sprint 38 close; carry-over)
?? docs/sprints/sprint-038-objective.md             (Sprint 38 close; carry-over)
?? docs/sprints/sprint-039-codex-review.md          (Sprint 39 close; carry-over)
?? docs/sprints/sprint-039-objective.md             (Sprint 39 close; carry-over)
?? compact/sprint-038-fix-dev-prompt.md             (Sprint 38 fix iteration; carry-over)
?? compact/sprint-038-fix-review-prompt.md          (Sprint 38 fix review; carry-over)
?? compact/sprint-038-review-prompt.md              (Sprint 38 initial review; carry-over)
?? compact/sprint-039-dev-prompt.md                 (Sprint 39 launch; carry-over)
?? compact/sprint-039-review-prompt.md              (Sprint 39 review; carry-over)
?? compact/sprint-040-dev-prompt.md                 (Sprint 40 launch; carry-over)
?? compact/sprint-040-review-prompt.md              (Sprint 40 review; carry-over from this session)
?? compact/context-handoff-sprint-039-pre-launch.md (Sprint 39 launch; carry-over)
?? compact/context-handoff-sprint-040-pre-launch.md (Sprint 40 launch; carry-over)
```

**THIS session added (Sprint 40 close + Sprint 41 launch):**

```
?? docs/sprints/sprint-040-codex-review.md          (NEW; Sprint 40 Codex archive)
?? docs/sprints/sprint-040-objective.md             (NEW; Sprint 40 contract archive)
M docs/sprints/sprint-040-handoff.md                (§12 closure verdict filled; this is the ONE allowed post-commit edit to a sprint archive per `feedback_handoff_verdict_section_delegation.md`)
?? compact/sprint-041-dev-prompt.md                 (NEW; Sprint 41 dev brief)
?? compact/context-handoff-sprint-041-pre-launch.md (NEW; THIS file)
```

### 3.2 Sprint 40 outcome (what shipped before this session)

- **Sprint 40 dev commit `9130abc`** shipped 5 files (per `git show --stat 9130abc`): `system_prompt.txt` EDIT (101 → 80 lines; numstat 2/23) + `discover_triage.yaml` EDIT (30 lines; numstat 1/1) + NEW `SkillTeachingMigrationIntegrationTest.java` (264 lines; numstat 264/0; 11 tests) + EDIT `PhaseEvaluatorSkillIntegrationTest.java` (341 → 400; numstat 60/1) + NEW `docs/sprints/sprint-040-handoff.md` (312 lines; numstat 312/0). Total 639 insertions / 25 deletions.
- **Codex per-sub-sprint review** returned `decision: pass / blocking_count: 0` on first pass (single round). All gates PASS: scope discipline (5 files match Sprint 40 contract §5 + OQ-S40.1 5th-file Sprint 39 precedent); §4.1 9-question kernel `approve` per Q1-Q9; §1.7 boundary checks PASS; all 33 Sprint 40 §6 + 22 M2 §6 hard fences honored; behavioural-equivalence golden test (PhaseEvaluatorSkillIntegrationTest 13/13 with updated DISCOVER golden) + new SkillTeachingMigrationIntegrationTest 11/11 PASS; M1 + Sprint 38 + Sprint 39 functional surfaces preserved; Java baseline 1105 / 1 inherited / 0 / 2 byte-identical.
- **Final Sprint 40 classification: A — Clean PASS** (M2 implementation-sub-sprint pattern: S37 PASS A → S38 B-fix-iterated → S39 PASS A → S40 PASS A — third clean A close under NEW M2; second clean A on an implementation sub-sprint).
- **Java baseline trajectory**: 1094 (post-Sprint-39) → 1105 (post-`9130abc`); inherited `SystemPromptUserRequestedTiebreakerTest` failure persists unchanged.
- **OQ disposition** (5 OQs all resolved with Codex independent verdicts aligned with deliver-agent + human pre-decisions): OQ-S40.1 (5th-file golden update) AGREE WITH DEV — INCLUDE; OQ-S40.2 (envelope-mechanics + DISCOVER pointer combined) AGREE WITH DEV; OQ-S40.3 (inherited test PERSISTS) STATUS QUO; OQ-S40.4 (Sprint 23 paragraph cosmetic re-wording declined) AGREE WITH DEV; OQ-S40.5 (Sprint 37 freeze doc §7.8 typo) ROUTE TO M2 CLOSE FOLD-BACK.
- **2 contract-drift items**: line-count target 60-75 missed at 80 (CONTENT-EQUIVALENCE PRESERVED); 5th-file Sprint 39 precedent (REAFFIRM PRECEDENT).
- **Tier-0 candidate disposition**: No new Tier-0. C1 REJECTED preserved by construction. C2 + C3 QUALIFIED-DEFER continued.
- **Codex §10 non-blocking observations**: (a) deliver-agent's Sprint 40 review prompt per-file numstat estimates differed from actuals (continuation of Sprint 39 `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse; second observed instance); (b) migrated content framing should be **formatting-normalized content equivalence** (not "byte-for-byte modulo whitespace flattening"); (c) Sprint 37 design doc §7.8 typo joins M2 close fold-back queue.

### 3.3 Sprint 41 framing (this session's product)

- `docs/sprint_objective.md` carries Sprint 41 contract (~460 lines). Status `current`; supersedes `docs/sprints/sprint-040-objective.md` per frontmatter.
- 12-section shape per Sprint 31-40 convention.
- §1 Sub-sprint class: implementation sub-sprint, single track (Track A); LAST M2 sub-sprint; semantic-touching multi-layer (`skill_state` + `prompt_projection`).
- §2 Goal: 7 deliverables D-a through D-g (D-g optional).
- §3 Non-goals: explicit list including no M3-D/M3-B/M3-A scope, no Tier-0 elevation, no M3 pre-decisions.
- §4 Premise check: 15 premises verified at HEAD `9130abc`. Sprint 41 dev re-verifies at session start.
- §5 Files in scope: ~10-15 files (NEW SkillStateBus.java + 6 Skill YAML EDITs + ContextProjectionBuilder EDIT + PhaseEvaluator EDIT + (possibly) Skill/StateInheritance/SkillLoader EDITs + 3 NEW test files + EDIT PhaseEvaluatorSkillIntegrationTest + handoff + (optional D-g) system_prompt.txt EDIT).
- §6 Hard fences: 33 fences.
- §7 Bundle policy: single dev commit; dev does NOT stage deliver-agent files.
- §8 §7 stanza: `skill_state` + `prompt_projection`; no Tier-0; no semantic hardcode; full generalization coverage.
- §9 Success metrics: HARD GATES + observations per M2 §5 recalibration. Java baseline target ~1140-1155 (1105 + ~35-50 new).
- §10 Stop conditions: 20 stop conditions.
- §11 Handoff §11 12-section contract.
- §12 M2 milestone context: Sprint 41 closes M2 implementation track; M2 close is the next planning round.

### 3.4 Files modified/created in this deliver-agent session (NOT yet committed)

```
M docs/sprint_objective.md                          (REPLACED with Sprint 41 contract)
M docs/codex-findings.md                            (reset to scaffold post-Sprint-40-Codex)
M docs/10-handoff.md                                (§1 lead refreshed; Sprint 40 close + Sprint 41 next; Sprint 39 demoted to Earlier; Sprint 38 dropped off)
M docs/action_bank.md                               (D-skill-runtime-framework Sprint 40 extension + Sprint 41 LAST + Sprint 40 close-action index row)
M docs/sprints/sprint-040-handoff.md                (§12 closure verdict filled)
?? docs/sprints/sprint-040-codex-review.md          (NEW; Sprint 40 Codex archive)
?? docs/sprints/sprint-040-objective.md             (NEW; Sprint 40 contract archive)
?? compact/sprint-041-dev-prompt.md                 (NEW; Sprint 41 dev brief)
?? compact/context-handoff-sprint-041-pre-launch.md (NEW; THIS file)
```

Combined with Sprint 38/39/40 carry-over (per §3.1):

```
M docs/10-handoff.md
M docs/action_bank.md
M docs/codex-findings.md
M docs/sprint_objective.md
M docs/sprints/sprint-040-handoff.md                (this session edit)
?? docs/sprints/sprint-038-codex-review.md
?? docs/sprints/sprint-038-objective.md
?? docs/sprints/sprint-039-codex-review.md
?? docs/sprints/sprint-039-objective.md
?? compact/sprint-038-fix-dev-prompt.md
?? compact/sprint-038-fix-review-prompt.md
?? compact/sprint-038-review-prompt.md
?? compact/sprint-039-dev-prompt.md
?? compact/sprint-039-review-prompt.md
?? compact/sprint-040-dev-prompt.md
?? compact/sprint-040-review-prompt.md
?? compact/context-handoff-sprint-039-pre-launch.md
?? compact/context-handoff-sprint-040-pre-launch.md
?? docs/sprints/sprint-040-codex-review.md          (this session)
?? docs/sprints/sprint-040-objective.md             (this session)
?? compact/sprint-041-dev-prompt.md                 (this session)
?? compact/context-handoff-sprint-041-pre-launch.md (this session)
```

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage these; human bundles at deliver-agent commit. Suggested commit message in §9 below.

---

## 4. Decision records

### 4.1 Decision — Sprint 41 ships the LAST M2 implementation sub-sprint with multi-fence convergence

Per human direction at Sprint 40 close + per `docs/milestone_objective.md` §3 Sprint 41 row + design doc §10 mapping:

- Sprint 41 scope is back to bigger than Sprint 40 — comparable to Sprint 39's multi-fence convergence (NEW SkillStateBus class + per-Skill state_inheritance enforcement across 6 YAMLs + NEW prior_use_case_carry projection slot + PhaseEvaluator integration). Estimated ~10-15 files + ~35-50 NEW tests.
- 7 deliverables + 1 optional (D-g orchestration-shell teaching).
- This closes the M2 implementation track. After Sprint 41 closes, M2 milestone close (milestone-shared Codex review + design doc fold-back + M2 archive).

Rationale (deliver-agent's interpretation; verify with human if discrepancy):
- M2 was deliberately split into 5 sub-sprints (S37 design freeze + S38 SkillRegistry-core + S39 multi-fence convergence + S40 prompt-side cleanup + S41 state-bus); each sub-sprint had different scope sizes.
- Sprint 41 is the architecturally most distinct sub-sprint: cross-Skill state preservation surface — different from the Skill-bounded predicate enforcement in Sprint 39 OR the prompt-side cleanup in Sprint 40.
- Sprint 41 will ship C3 Tier-0 candidate's FIRST observed evidence surface (the bus's `applyOnSkillSwitch` enforcement); deliver-agent + human + Codex jointly classify C3 elevation at Sprint 41 close OR M2 close.

### 4.2 Decision — Strong STOP discipline (5 LOAD-BEARING + 20 total)

Per human direction (carried forward from Sprint 40):

1. **No per-UC-pair branch in SkillStateBus body** — STOP per §1.7 + design doc §10.7 Alternative B REJECTED.
2. **No new classifier for UC-switch detection** — STOP per design doc §10.3 + M2 §6 #5 fence (detection rides on existing M1 Sprint 32 + Sprint 33 projections).
3. **No per-UC-pair `state_inheritance` declarations** in Skill YAMLs — STOP per §1.7 + design doc §10.7 Alternative B REJECTED.
4. **No Tier-0 invariant added to `docs/runtime_freeze_and_risk_policy.md`** — STOP per M2 §6 #8 fence; C3 candidate evidence surfaces but DEFER; deliver-agent + human + Codex jointly classify at Sprint 41 close OR M2 close.
5. **No scope creep into M3 surfaces** — STOP if tempted to touch `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` / `INTAKE_UCS` / `escalation_reason` enum / `IntakeFieldsRegistry` behaviour.

These STOPs reflect Sprint 37 + Sprint 38 + Sprint 39 + Sprint 40 close discipline carried forward. Codified in dev prompt §4 with specific examples.

### 4.3 Decision — Sprint 41 contract acceptance bar inherits M2 §5 recalibration

Per `docs/milestone_objective.md` §5 recalibration (2026-05-17): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION only for THIS milestone; primary gate = functional review + Java tests + Sprint 37 freeze decisions honored. Sprint 41 contract §9 reflects this:
- HARD GATES: SkillStateBus matches §10.3 surface; state_inheritance declarations match §10.5 matrix; intake_fields_partial intersection registry-driven; prior_use_case_carry slot matches §10.4; Skill-switch detection rides on existing M1 surfaces; M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved; Java baseline ~1140-1155; Codex per-sub-sprint verdict `approve`.
- OBSERVATIONS: interactive eval smoke + Alice + architecture-health metrics; MAY regress; does NOT block.

Same recalibration Sprint 37/38/39/40 contracts honored. No change at Sprint 41.

### 4.4 Constitution-discipline check applied this session

Several proposed actions screened through `feedback_constitution_discipline_vs_planning_anticipation.md` 4-question check:

1. **C3 Tier-0 candidate elevation temptation at Sprint 41 close round** — DEFERRED. Sprint 41 will ship the FIRST observed evidence surface (SkillStateBus's `applyOnSkillSwitch` enforcement) but production trace evidence pending. Codex independent verdict at Sprint 41 close: expected DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md` (deterministic Java tests are necessary but not sufficient production evidence). Deliver-agent + human + Codex jointly re-evaluate at Sprint 41 close OR M2 close. R-item `R-skill-state-bus-boundary-enforcement-tier-0` STAYS OPEN.
2. **M2 close fold-back temptation in Sprint 41** — DEFERRED to M2 close. The fold-back queue (Sprint 37 design doc §6.2.5 + §6.2.6 RESOLVE templates per Sprint 39 §10 + §7 teaching mapping per Sprint 40 §7.8 typo per OQ-S40.5) is M2 close housekeeping, NOT Sprint 41 dev scope. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: SEPARATE governance commit at M2 close.
3. **Sprint 39 OQ-S39.7 stale Sprint 38 test name housekeeping** — DEFERRED to M2 close per Sprint 39 close pre-decision. Sprint 41 dev does NOT rename.

---

## 5. Current tasks (what's in flight right now)

### 5.1 Authored and waiting for human commit

- Sprint 38 close housekeeping carry-over (still uncommitted from Sprint 39 launch):
  - `docs/sprints/sprint-038-objective.md` + `sprint-038-codex-review.md` archives.
  - `compact/sprint-038-fix-dev-prompt.md` + `sprint-038-fix-review-prompt.md` + `sprint-038-review-prompt.md` archives.
- Sprint 39 launch carry-over:
  - `compact/sprint-039-dev-prompt.md` + `sprint-039-review-prompt.md` + `context-handoff-sprint-039-pre-launch.md`.
- Sprint 39 close housekeeping carry-over:
  - `docs/sprints/sprint-039-objective.md` + `sprint-039-codex-review.md` archives.
- Sprint 40 launch carry-over:
  - `compact/sprint-040-dev-prompt.md` + `sprint-040-review-prompt.md` + `context-handoff-sprint-040-pre-launch.md`.
- Sprint 40 close housekeeping (THIS session added):
  - `docs/sprints/sprint-040-objective.md` + `sprint-040-codex-review.md` archives.
  - `docs/sprints/sprint-040-handoff.md` §12 closure verdict filled.
  - `docs/codex-findings.md` reset to scaffold.
  - `docs/10-handoff.md` §1 lead refreshed.
  - `docs/action_bank.md` updates.
- Sprint 41 launch (THIS session added):
  - `docs/sprint_objective.md` REPLACED with Sprint 41 contract.
  - `compact/sprint-041-dev-prompt.md` NEW.
  - `compact/context-handoff-sprint-041-pre-launch.md` NEW (THIS file).

All staged but NOT committed. Human bundles per `feedback_commit_at_end_bundles_deliver_artefacts.md`. Suggested commit message in §9 below.

### 5.2 Pending deliver-agent work (after human commit + Sprint 41 dev session)

- (After human commit + Sprint 41 dev launches via `compact/sprint-041-dev-prompt.md`): no deliver-agent action; wait for dev commit.
- (After Sprint 41 dev commit): read Sprint 41 dev handoff at `docs/sprints/sprint-041-handoff.md` + SkillStateBus.java + 6 Skill YAML diffs + ContextProjectionBuilder.java diff + PhaseEvaluator.java diff + new test files + (possibly) system_prompt.txt diff. Validate: SkillStateBus matches §10.3 surface; state_inheritance declarations match §10.5 matrix; intake_fields_partial registry intersection; prior_use_case_carry slot per §10.4; no new classifier; no Tier-0 elevation; M3 surfaces UNCHANGED; M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved.
- Draft `compact/sprint-041-review-prompt.md` + dispatch Codex per-sub-sprint review per §4.3 trigger #3 (session state model touch across Skill boundary; C3 Tier-0 candidate evidence surface — Codex independent verdict on C3 elevation timing is load-bearing for Sprint 41 close).
- Help human classify Codex verdict at Sprint 41 close.

### 5.3 Pending deliver-agent work (Sprint 41 close housekeeping + M2 close planning)

After Sprint 41 closes:

- **Sprint 41 close housekeeping bundle**: archive Sprint 41 contract → `docs/sprints/sprint-041-objective.md`; reset `docs/codex-findings.md` to scaffold; archive Sprint 41 Codex to `docs/sprints/sprint-041-codex-review.md`; fill `docs/sprints/sprint-041-handoff.md` §12 closure verdict; refresh `docs/10-handoff.md` §1 lead (current = M2 milestone close pending; preceding = Sprint 41 closed; earlier = Sprint 40); update `docs/action_bank.md` — close `D-skill-runtime-framework` (FULL LANDING — Sprints 37 + 38 + 39 + 40 + 41 all shipped); add Sprint 41 close-action index row; flip C3 R-item `R-skill-state-bus-boundary-enforcement-tier-0` with Sprint 41 enforcement evidence + Codex independent verdict on C3 elevation timing.
- **M2 milestone close planning round** (deliver-agent + human + Codex jointly):
  - Draft `compact/M2-review-prompt.md` against cumulative M2 commit range (Sprints 37 + 38 + 38-fix + 39 + 40 + 41 commits, IN ADDITION TO per-sub-sprint reviews already done at S37/S38/S39/S40/S41 close).
  - Human dispatches Codex; Codex returns milestone-shared verdict.
  - Deliver-agent + human classify findings.
  - Append §12 closure verdict to `docs/milestone_objective.md`; archive to `docs/milestones/M2_objective.md`.
  - Append §6.5 closed-milestone index row to `docs/action_bank.md`.
  - Refresh `docs/10-handoff.md` §1 lead (M2 → Preceding milestone; M3 → Current).
  - **M2 close editorial fold-back queue** (SEPARATE governance commit per `doc_governance.md` cadence + `feedback_commit_at_end_bundles_deliver_artefacts.md`):
    - Design doc §6.2.1 DISCOVER template editorial paraphrasing per OQ-S38.1.
    - Design doc §6.2.5 + §6.2.6 RESOLVE templates editorial paraphrasing per Sprint 39 §10 note.
    - Design doc §7.8 Sprint 31/33 line-number swap typo per OQ-S40.5.
    - Design doc §7 teaching mapping per Sprint 40 if any divergence surfaced.
    - Design doc §10 state-bus template per Sprint 41 §10 closing notes if any divergence surfaces (Sprint 41 OQ).
    - Stale Sprint 38 test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` rename per Sprint 39 OQ-S39.7.
  - **C2 + C3 R-item re-evaluation at M2 close**: deliver-agent + human + Codex jointly classify (DEFER continued / ELEVATE NOW / NOT-A-CANDIDATE) based on cumulative M2 evidence. If ELEVATE NOW: SEPARATE governance commit to `docs/runtime_freeze_and_risk_policy.md` §1.1.
  - Help human pick M3 (M3-A / M3-B / M3-C / M3-D / M3-Latency / M3-Skill-Tuning per `docs/milestone_objective.md` §11).

### 5.4 Pending deliver-agent work (M3 planning post-M2-close)

Per `docs/milestone_objective.md` §11:
- M3 candidate selection (deliver-agent + human consult research-agents if scope is novel).
- New `docs/milestone_objective.md` for M3.
- First M3 sub-sprint `docs/sprint_objective.md` + dev prompt + pre-launch handoff.
- Surface to human for review BEFORE M3 sub-sprint 1 dev session launches.

---

## 6. Next steps for the next deliver-agent session

When the next deliver-agent instance starts:

1. **Read this file first.** Then `AGENTS.md` + `compact/sprint-deliver-orchestrator.md`.
2. **Verify git state.** Run `git -C /Users/caoruixin/projects/csagent-latest status` + `git log --oneline -10`. Confirm whether the human has committed the deliver-agent batch 1 + batch 2 (+ carry-over) bundle from §3.4.
3. **Verify Sprint 41 contract state.** Confirm `docs/sprint_objective.md` is Sprint 41 contract (not Sprint 40). If unclear, ask explicitly.
4. **If Sprint 41 dev session has not yet run:** wait for human to launch (via `compact/sprint-041-dev-prompt.md`). No deliver-agent action.
5. **If Sprint 41 dev session has committed:** read Sprint 41 dev handoff + SkillStateBus diff + 6 Skill YAML diffs + ContextProjectionBuilder diff + PhaseEvaluator diff + new test results; help human classify outcome; draft Sprint 41 Codex review prompt + dispatch; help classify Codex verdict (especially C3 Tier-0 elevation independent verdict).
6. **If Sprint 41 closes PASS:** initiate M2 milestone close planning round per §5.3.
7. **If Sprint 41 closes B-fix-iterated:** apply existing fix-iteration close pattern (Sprint 38 precedent); after fix iteration closes, then M2 milestone close.
8. **If Sprint 41 surfaces C3 Tier-0 candidate evidence with Codex verdict ELEVATE NOW:** route per `feedback_constitution_discipline_vs_planning_anticipation.md` + Sprint 37/38/39 OQ-S38.5/OQ-S39.5 precedent — Tier-0 elevation is deliver-agent + human authority; if elevation warranted, SEPARATE governance commit to `runtime_freeze_and_risk_policy.md` §1.1; M2 §6 #8 fence EXCEPTION clause allows this at sub-sprint close / M2 close + human authorization.
9. **Throughout: respect constitution-discipline.** Re-walk the 4-question check in `feedback_constitution_discipline_vs_planning_anticipation.md` BEFORE committing any edit to `docs/current/iteration_governance.md` / `docs/foundational/*` / `docs/runtime_freeze_and_risk_policy.md`.

---

## 7. Cautions / hard constraints / lessons

### 7.1 NEW M2 §5 acceptance recalibration is THIS milestone only

Per `docs/milestone_objective.md` §5: bad-case suite (Alice) + interactive eval are OBSERVATION not gate for NEW M2 (architecture-focused). Future milestones (esp. M3-A customer-honesty OR M3-B Single Handover Orchestrator) may revert to bad-case-suite-primary if behaviour-focused. Per-sub-sprint Codex review prompts (S37/S38/S39/S40/S41) MUST explicitly state this recalibration. The Sprint 41 contract §9 + dev prompt §6 honor this; the Codex review prompt at Sprint 41 close MUST also state this.

### 7.2 Hard fences preserved through Sprint 41

Per Sprint 41 contract §6 (33 items) — load-bearing fences:

- **No per-UC-pair branch in SkillStateBus body**.
- **No new classifier for UC-switch detection** (rides on existing M1 Sprint 32 + Sprint 33 projections).
- **No per-UC-pair `state_inheritance` declarations** in Skill YAMLs.
- **No Tier-0 invariant added** (C3 candidate evidence surfaces; DEFER expected).
- **No touch to `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool`** (M2 §6 #5 fence).
- **No edit to `INTAKE_UCS` set; no widening of `escalation_reason` enum** (M2 §6 #6 + #7 fences).
- **No modification of `IntakeFieldsRegistry.java` / `IntakeFieldExtractor.java` behaviour** (M1 functional surface; consume but don't modify).
- **No modification of `ResolveDispositionEvaluator.java`** (Sprint 11/11.1/12 frozen surface).
- **No modification of `SkillGuardrailDispatcher.java` / `RejectVerdict.java` / `DispatchContext.java`** (Sprint 39 surfaces).
- **No edit to governance docs / freeze docs / sprint archives / milestone archives / deliver-agent-owned files / `eval_interactive/`**.
- **No mocked-LLM as primary evidence** for LLM-behaviour claims.

### 7.3 Sprint 41 dev session expected outputs

Single dev commit with:

- NEW `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` (~150-250 lines).
- 6 Skill YAML EDITs at `server/src/main/resources/skills/`.
- `Skill.java` / `StateInheritance.java` / `SkillLoader.java` verification + minor edits if schema drift.
- `ContextProjectionBuilder.java` EDIT (~30-50 new lines).
- `PhaseEvaluator.java` (or `ControlKernel.java`) EDIT (~5-15 new lines + (possibly) `@Autowired` constructor extension).
- (Optionally) `BotSession.java` EDIT if session-state holder needs adjustment.
- (Optional D-g) `system_prompt.txt` EDIT (1-2 lines).
- NEW `SkillStateBusTest.java` + `UcSwitchStateInheritanceTest.java` + `PriorUseCaseCarryProjectionTest.java` (~35-50 NEW tests).
- EDIT `PhaseEvaluatorSkillIntegrationTest.java` if applicable (1-2 new tests).
- `docs/sprints/sprint-041-handoff.md` NEW (12-section dev archive).

Dev does NOT stage: deliver-agent-owned files per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

### 7.4 Codex per-sub-sprint review trigger for Sprint 41

§4.3 trigger #3 (session state model touch across Skill boundary — load-bearing for cross-Skill state preservation; C3 Tier-0 candidate evidence surface). Codex verifies:

- (a) NEW SkillStateBus class matches design doc §10.3 public surface.
- (b) per-Skill state_inheritance declarations match §10.5 invariant matrix.
- (c) intake_fields_partial intersection is REGISTRY-DRIVEN (NOT per-UC-pair table).
- (d) NEW prior_use_case_carry projection slot matches §10.4 shape.
- (e) Skill-switch detection rides on existing M1 surfaces (no new classifier).
- (f) M3-D Topic↔UC binding fence + M3-B INTAKE_UCS fence + M3-A escalation_reason enum fence preserved.
- (g) M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved.
- (h) **C3 Tier-0 candidate independent verdict** — DEFER expected; deliver-agent + human + Codex jointly classify.
- (i) no per-UC-pair if-else in state-bus body OR state_inheritance declarations OR projection slot construction.
- (j) Sprint 41 closes M2 implementation work.

### 7.5 Key memory files (cross-session) in `.claude/agent-memory/sprint-deliver-orchestrator/`

Load all on cold start (if directory exists — note from Sprint 40 close session: directory may not exist on all machines; patterns are summarized here as fallback):
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev NOT stage deliver-agent files; human bundles.
- `feedback_handoff_verdict_section_delegation.md` — dev §12 closure verdict delegation; deliver-agent fills at close.
- `feedback_out_of_scope_review_packaging_rollforward.md` — OOSR-with-packaging-note pattern.
- `feedback_close_with_codex_skipped_docs_only_outcome.md` — A-with-Codex-skipped for docs-only sprints.
- `feedback_corpus_undecidable_premise_check.md` — in-flight downgrade pattern.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every number cites source + recipe; Sprint 40 review prompt per-file numstat estimates differed from actuals (continuation of Sprint 39 lapse — second observed instance). Next deliver-agent review prompts (Sprint 41 + M2-shared) should cite `git show --numstat <commit>` directly when commit exists OR defer to Codex's verification ("Codex verifies via `git show --numstat`") when drafting BEFORE the dev commit lands.
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM required for prompt-causal evidence; Sprint 41 Java unit + integration tests on bus + slot logic MAY mock LLM (deterministic Java logic) but no real-LLM eval rerun is required.
- `feedback_probe_sprint_shape_for_conditional_broadening.md` — probe sprint shape.
- `feedback_multi_layer_prospective_stanza.md` — two-track stanza shape.
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add supersession for codex-findings at archive.
- `feedback_constitution_discipline_vs_planning_anticipation.md` — planning anticipation ≠ execution-time authorization for governance-tier edits; C3 Tier-0 elevation at Sprint 41 close is the load-bearing application of this pattern.

### 7.6 Lessons from THIS session (additions to deliver-agent doctrine; not yet memory files)

- **Sprint 40 clean A close demonstrates the prompt-side cleanup pattern works** when STOP discipline is codified pre-launch. Sprint 40's 4 execution phases (system_prompt.txt EDIT + discover_triage.yaml EDIT + new test + existing test golden update) all landed cleanly with Codex `pass / 0` on first pass; the strong STOP discipline (5 LOAD-BEARING STOPs + 18 contract stop conditions + 33 contract hard fences) prevented silent scope drift. Sprint 41 inherits this discipline pattern with smaller per-deliverable risk but more deliverables (multi-fence convergence shape).
- **Reproducibility lapse continues** on per-file numstat estimates in deliver-agent review prompts. Sprint 39 review prompt estimated `~2191/539` vs actual `2621/536`; Sprint 40 review prompt §2 estimated `+4/-25, +2/-1, +61/-2` vs actuals `2/23, 1/1, 60/1`. **Pattern**: deliver-agent mentally estimates rather than running `git show --numstat <commit>` and citing directly. **Mitigation in Sprint 41 review prompt**: either cite exact `git show --numstat 9130abc..<sprint-41-commit>` output verbatim when the commit exists, OR defer to Codex's verification when drafting BEFORE dev commit lands.
- **"Formatting-normalized content equivalence"** is the more precise term than "byte-for-byte modulo whitespace flattening" for migration claims. Sprint 40 Codex non-blocking observation (b): bullet markers removed + first post-colon "The" → "the" means the migration is *semantically equivalent + formatting-normalized*, NOT literally byte-identical. Future handoffs / review prompts use the precise term.
- **Sprint 41 is the LAST M2 implementation sub-sprint.** Sprint 41 close → M2 milestone close planning round. M2 close housekeeping is BIGGER than per-sub-sprint close (milestone-shared Codex review + design doc editorial fold-back + M2 archive + C2/C3 R-item re-evaluation + M3 planning round). Deliver-agent allocates more session capacity for M2 close than for Sprint 41 close alone.
- **C3 Tier-0 candidate elevation at Sprint 41 close is the load-bearing OQ.** Sprint 41 ships C3's FIRST observed evidence surface (SkillStateBus's `applyOnSkillSwitch` enforcement). Codex independent verdict on C3 elevation timing is requested at Sprint 41 close per `iteration_governance.md` §4.3. Deliver-agent + human pre-decision: DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md` (deterministic Java tests are necessary but not sufficient production evidence; the bus's load-bearing claim is that LLM CANNOT override state inheritance — that needs production trace observation across Sprint 41 → M2 close → M3+ window before C3 elevation). If Codex's verdict is "elevate NOW": route as `out_of_scope_review` per Sprint 37/38/39 OQ-S38.5/OQ-S39.5 precedent — Tier-0 elevation is deliver-agent + human authority + requires explicit human-review escalation at planning round + `feedback_constitution_discipline_vs_planning_anticipation.md` 4-question check.

---

## 8. Quick verification checklist for the next deliver-agent

On cold start, run these to confirm state:

```bash
# Verify HEAD + commit graph
git -C /Users/caoruixin/projects/csagent-latest log --oneline -5
# Expected: top commit may be 9130abc (deliver-agent bundle not committed) OR a NEW commit if human bundled Sprint 40 close + Sprint 41 launch + Sprint 41 dev OR just close + launch

# Verify deliver-agent-owned files state
git -C /Users/caoruixin/projects/csagent-latest status --short docs/sprint_objective.md docs/milestone_objective.md docs/10-handoff.md docs/action_bank.md docs/codex-findings.md docs/sprints/sprint-038-* docs/sprints/sprint-039-* docs/sprints/sprint-040-* compact/sprint-040-* compact/sprint-041-* compact/context-handoff-*

# Verify NEW M2 content (should mention "Skill Registry Abstraction" in title)
head -3 /Users/caoruixin/projects/csagent-latest/docs/milestone_objective.md

# Verify Sprint 41 content (should mention "UC switch + state preservation" in title)
head -5 /Users/caoruixin/projects/csagent-latest/docs/sprint_objective.md

# Verify Sprint 40 archives exist
ls -la /Users/caoruixin/projects/csagent-latest/docs/sprints/sprint-040-*.md

# Verify Sprint 41 dev prompt exists + mentions §10 mapping
head -10 /Users/caoruixin/projects/csagent-latest/compact/sprint-041-dev-prompt.md
grep -c "§10\|design doc §10\|SkillStateBus\|prior_use_case_carry\|state_inheritance" /Users/caoruixin/projects/csagent-latest/compact/sprint-041-dev-prompt.md
# Expected: multiple matches (the §10 mapping is the core of Sprint 41 scope)

# Verify Sprint 41 dev session has committed OR not
git -C /Users/caoruixin/projects/csagent-latest log --oneline -3 | grep -E "sprint 41|Sprint 41"
# If empty: dev has not committed yet (deliver-agent waits)
# If matches: dev has committed; deliver-agent + human can review

# If dev has committed: verify dev handoff exists + SkillStateBus.java added
test -f /Users/caoruixin/projects/csagent-latest/docs/sprints/sprint-041-handoff.md && echo "handoff present"
test -f /Users/caoruixin/projects/csagent-latest/server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java && echo "SkillStateBus present"
wc -l /Users/caoruixin/projects/csagent-latest/server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java
# Expected post-Sprint-41: ~150-250 lines

# Verify ContextProjectionBuilder extended
wc -l /Users/caoruixin/projects/csagent-latest/server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java
# Expected post-Sprint-41: ~1191-1211 lines (was 1161 pre-Sprint-41)

# Verify Java baseline post-Sprint-41 (if dev committed)
cd /Users/caoruixin/projects/csagent-latest/server && mvn test -q 2>&1 | tail -10
# Expected post-Sprint-41: Tests run: ~1140-1155, Failures: 1 OR 0, Errors: 0, Skipped: 2
```

If anything is unexpected vs this handoff (e.g., HEAD moved unexpectedly, Sprint 41 contract edited post-handoff, dev shipped surface outside contract §5), pause and re-read state before acting.

---

## 9. Suggested commit message (if human hasn't bundled close + launch yet)

```
docs: Sprint 40 close housekeeping + Sprint 41 contract + dev prompt draft (NEW M2 sub-sprint 4→5; LAST M2 sub-sprint)

Sprint 40 (NEW M2 sub-sprint 4) closed A — Clean PASS 2026-05-18
at dev commit 9130abc. Codex per-sub-sprint review per
`iteration_governance.md` §4.3 trigger #3 returned `decision: pass /
blocking_count: 0` on first pass (single round; cleanest M2
implementation-sub-sprint close pattern continues: S37 PASS A →
S38 B-fix-iterated → S39 PASS A → S40 PASS A).

Sprint 40 dev shipped (5 files; 639 insertions / 25 deletions per
`git show --stat 9130abc`; per-file numstat 312/0, 2/23, 1/1, 60/1,
264/0): `system_prompt.txt` EDIT (101 → 80 lines, -21 net; Sprint
31 paragraph + Sprint 33 SLOT description + DISCOVER phase guidance
bulk DELETED; envelope-mechanics paragraph + DISCOVER one-line
pointer combined ADDED at post-Sprint-40 lines 30-31 per OQ-S40.2
dev judgment; Sprint 23 `already_called` paragraph + `request_handover`
decision tree PRESERVED); `discover_triage.yaml` EDIT (30 lines
structure unchanged; `procedure` quoted string extended ~2900 →
~7900 chars with migrated Sprint 31 + Sprint 33 SLOT description +
DISCOVER phase guidance refinements; Sprint 33 cue body from Sprint
38 PRESERVED alongside; cue/slot split closed per design doc §7.2.3);
NEW `SkillTeachingMigrationIntegrationTest.java` (264 lines; 11
Java-deterministic prompt-composition tests); EDIT
`PhaseEvaluatorSkillIntegrationTest.java` (341 → 400 lines; +59;
DISCOVER_SYSTEM_INSTRUCTION golden updated; OQ-S40.1 per Sprint 39
precedent — Codex AGREED); 312-line dev handoff. Java baseline 1094
→ 1105 (1 inherited failure persists unchanged).

Codex independent verification confirmed: scope discipline (5 files
match Sprint 40 contract §5 + OQ-S40.1 5th-file Sprint 39 precedent
reaffirmed); §4.1 9-question kernel `approve` per Q1-Q9; §1.7
boundary checks PASS; all 33 Sprint 40 §6 + all 22 M2 §6 hard
fences honored; M1 + Sprint 38 + Sprint 39 functional surfaces
preserved (`Sprint71PartialIntakePersistenceTest` 14/14 + `SkillTest`
9/9 + `SkillRegistryTest` 11/11 + `SkillLoaderTest` 18/18 +
`PhaseEvaluatorResolveSkillIntegrationTest` 14/14 +
`SkillGuardrailDispatcherTest` 24/24 + `ResolveFaqGuardrailsTest`
11/11 + `ResolveIntakeGuardrailsTest` 11/11 all PASS);
`PhaseEvaluator.java` 1427 + `AgentRunLoopImpl.java` 638 +
`SkillGuardrailDispatcher.java` 416 + `ContextProjectionBuilder.java`
1161 + `ResolveDispositionEvaluator.java` 205 + `IntakeFieldsRegistry.java`
222 all UNCHANGED via empty `git diff`; Sprint 37 design doc §7.8
typo confirmed by Codex; dev implemented CORRECT mapping.

OQ disposition at Sprint 40 close (5 OQs; all aligned with deliver-
agent + human pre-decisions which deliver-agent withheld from
review prompt per "surface OQs without recommendations" framing):
- OQ-S40.1 (5th-file `PhaseEvaluatorSkillIntegrationTest.java`
  golden update; contract §5 listed 4) → **AGREE WITH DEV —
  INCLUDE** per Sprint 39 precedent for behavioural-equivalence
  test-file edits.
- OQ-S40.2 (envelope-mechanics paragraph + DISCOVER pointer combined
  into single shell paragraph rather than design doc §7.3 2-row
  separation) → **AGREE WITH DEV** — respects design §7.3 at
  content level.
- OQ-S40.3 (inherited `SystemPromptUserRequestedTiebreakerTest`
  PERSISTS) → **STATUS QUO** — not a Sprint 40 close blocker.
- OQ-S40.4 (Sprint 23 paragraph cosmetic re-wording declined by
  dev) → **AGREE WITH DEV** — existing phrasing already cross-Skill
  framed.
- OQ-S40.5 (Sprint 37 freeze doc §7.8 Sprint 31/33 line-number
  swap typo at `docs/proposals/skill_registry_design.md:2015`) →
  **ROUTE TO M2 CLOSE FOLD-BACK** per Sprint 38 OQ-S38.1 precedent.

2 contract-drift items at Sprint 40 close: line-count target 60-75
missed at 80 lines (CONTENT-EQUIVALENCE PRESERVED); 5th-file Sprint
39 precedent (REAFFIRM PRECEDENT).

Tier-0 candidate disposition at Sprint 40 close: No new Tier-0.
C1 REJECTED preserved by construction. C2 + C3 QUALIFIED-DEFER
continued — Sprint 40 ships no new dispatcher / state-bus
enforcement surface (content-relocation only); no fresh C2/C3
evidence emerges. R-items stay open in `docs/action_bank.md` §5.2
for M3+ revisit. Sprint 41 will ship C3's FIRST observed evidence
surface (`SkillStateBus.java` Skill-switch enforcement).

Codex §10 non-blocking observations:
- Deliver-agent's Sprint 40 review prompt §2 per-file numstat
  estimates differed from actuals (continuation of Sprint 39
  `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`
  lapse; second observed instance); next deliver-agent review
  prompts should cite `git show --numstat <commit>` directly when
  commit exists OR defer to Codex verification when drafting BEFORE
  dev commit lands.
- Migrated content "byte-for-byte modulo whitespace flattening"
  framing is more precisely **formatting-normalized content
  equivalence** (bullet markers removed; first post-colon "The" →
  "the"); future handoffs / review prompts use the precise term.
- Sprint 37 design doc §7.8 typo joins M2 close fold-back queue
  alongside OQ-S38.1 + Sprint 39 OQ-S39.7 + design doc §6.2.5 /
  §6.2.6 templates per Sprint 39 close §10 notes.

Sprint 40 close housekeeping (this commit's Batch 1):
- docs/codex-findings.md: reset to scaffold (archived combined
  Codex Sprint 40 verdict to docs/sprints/sprint-040-codex-review.md).
- docs/sprints/sprint-040-codex-review.md: NEW (archive frontmatter
  header + verbatim Codex output per
  `feedback_packaging_codex_findings_supersession.md` delete-and-add).
- docs/sprints/sprint-040-objective.md: NEW (Sprint 40 contract
  archive; frontmatter `status: archived`).
- docs/sprints/sprint-040-handoff.md: §12 closure verdict filled
  (`A — Clean PASS`; Codex independent verification details; 5 OQ +
  2 contract-drift dispositions; R-item flip notes).
- docs/10-handoff.md: §1 lead refreshed (Date → 2026-05-18; Current
  → M2 Sprint 40 closed PASS A / Sprint 41 next pre-planning;
  Preceding → Sprint 40 detailed; Earlier → Sprint 39 compressed;
  Sprint 38 dropped off).
- docs/action_bank.md: Sprint 40 close-action row in §6 (between
  Sprint 39 and Sprint 32); `D-skill-runtime-framework` partial-
  landing annotation extended to Sprint 40 (Sprints 37 + 38 + 39 +
  40 shipped; Sprint 41 named as LAST continuation surface); Sprint
  37 design doc §7.8 typo noted as joining M2 close fold-back queue.

Sprint 41 launch round (this commit's Batch 2):
- docs/sprint_objective.md: REPLACED with Sprint 41 contract (~460
  lines; UC switch + state preservation across Skill boundary per
  design doc §10 — `SkillStateBus.java` NEW + per-Skill
  `state_inheritance` enforcement declared on all 6 Skill YAMLs +
  NEW `prior_use_case_carry` projection slot in
  `ContextProjectionBuilder.java`). LAST M2 sub-sprint; multi-fence
  convergence shape similar to Sprint 39. Strong STOP discipline
  (20 stop conditions; 33 hard fences); LOAD-BEARING design doc
  §10.3 + §10.5 matrix + §10.7 Alternative B REJECTED enforcement.
- compact/sprint-041-dev-prompt.md: NEW (~320 lines; the dev brief
  for Claude Code; 7-deliverable walkthrough + STOP discipline +
  hard fences + §3 premise re-verification checklist + bundle
  policy + handoff §11 12-section contract + commit message
  template).
- compact/context-handoff-sprint-041-pre-launch.md: NEW (cross-
  session handoff for next deliver-agent instance).

Sprint 41 design (this batch's product):
- Layer: skill_state + prompt_projection per §1.4.
- 7 deliverables D-a/D-b/D-c/D-d/D-e/D-f + 1 optional D-g
  (orchestration-shell teaching about prior_use_case_carry slot).
- Java baseline target post-Sprint-41: 1105 (post-Sprint-40) +
  ~35-50 new = ~1140-1155; 1 inherited failure may persist or
  resolve organically per D-g.

Carry-forward into Sprint 41 (preserved verbatim where applicable):
M2 §6 #4 verbatim human authorization (Sprint 39 implemented;
Sprint 41 not relevant); all hard fences on RuntimeIntentClassifier
/ DriftDetector / UseCaseRouter / escalation_reason enum /
INTAKE_UCS / D-full-issue-ledger; cascade fence on existing case
families; constitution-discipline; Sprint 11 / 11.1 / 12 frozen
surface (ResolveDispositionEvaluator); M1 functional-surface
preservation (IntakeFieldExtractor + IntakeFieldsRegistry +
Sprint71PartialIntakePersistenceTest 14/14); Sprint 38 + Sprint 39
+ Sprint 40 surfaces preserved.

Sprint 41 will ship C3 Tier-0 candidate's FIRST observed evidence
surface (SkillStateBus's applyOnSkillSwitch enforcement). Codex
independent verdict on C3 elevation timing at Sprint 41 close OR M2
close per feedback_constitution_discipline_vs_planning_anticipation.md;
deliver-agent + human pre-decision: DEFER (production trace
evidence pending across Sprint 41 → M2 close → M3+ window).

After Sprint 41 closes: M2 milestone close planning round (deliver-
agent + human + Codex milestone-shared cumulative review per
iteration_governance.md §4.3; design doc editorial fold-back per
doc_governance.md cadence including OQ-S38.1 + Sprint 39 OQ-S39.7
+ OQ-S40.5 + Sprint 41 OQs if any; C2 + C3 R-item re-evaluation;
M2 archive to docs/milestones/M2_objective.md; §6.5 closed-milestone
index row; 10-handoff §1 lead refresh; M3 planning round).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

---

End of context handoff. The next deliver-agent reads this + the role brief + verifies state, then proceeds per §5.2 (Sprint 41 dev result review + Codex dispatch) OR §5.3 (Sprint 41 close housekeeping + M2 close planning) OR §5.4 (M3 planning post-M2-close) depending on where the dev session + M2 close has reached.
