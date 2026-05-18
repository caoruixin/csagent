# Deliver-agent context handoff — Sprint 39 pre-launch (NEW M2 sub-sprint 3 contract + dev prompt drafted; pending human commit + dev session)

**Authored:** 2026-05-17 by deliver-agent
**For:** the next deliver-agent instance picking up after Sprint 39 dev session has committed (OR helping the human decide pre-commit edits to the Sprint 39 contract + dev prompt)
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD:** `5787806` (Sprint 38 fix iteration #1; B-fix-iterated close 2026-05-17; deliver-agent Sprint 38 close housekeeping bundle + Sprint 39 contract bundle NOT committed yet at handoff authoring time)

Read order on cold start: this file → `AGENTS.md` (loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition; long-term doctrine — **not duplicated here**) → `docs/milestone_objective.md` (NEW M2 contract; supersedes OLD M2-Skill) → `docs/sprint_objective.md` (Sprint 39 design freeze contract) → `compact/sprint-039-dev-prompt.md` (the dev brief Claude Code consumes) → `docs/sprints/sprint-038-handoff.md` + `docs/sprints/sprint-038-fix-handoff.md` + `docs/sprints/sprint-038-codex-review.md` (Sprint 38 close archives) → `docs/proposals/skill_registry_design.md` §5.2 + §6.2.5 + §6.2.6 + §6.4 + §8 + §9 (Sprint 37 freeze, the architectural source-of-truth Sprint 39 implements).

---

## 1. Background (Why this session happened)

Previous deliver-agent session handoff at `compact/context-handoff-m2-skill-supersede-new-m2-skillreg.md` (2026-05-17 afternoon) — that handoff covered the OLD M2-Skill → NEW M2 supersession + Sprint 37 design freeze contract draft. Sprint 37 closed clean PASS A on 2026-05-17 (commit `51c327c`).

Sprint 38 closed B-fix-iterated 2026-05-17:
- Main-dev commit `bd9d3f5` shipped the SkillRegistry core abstraction + 4 simpler phase Skills migration (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE) + 45 new tests.
- Codex per-sub-sprint review round 1 returned `decision: fix_required / blocking_count: 1` — Codex Blocking Finding 1: `SkillLoader` schema-validation completeness for design doc §2.2 (4 sub-gaps: missing `tools_required` presence detection unreachable due to `Skill.java:66` compact-constructor null-normalization; no tool-name allowlist; no explicit `applicable_use_cases` UC registry check; no `Guardrail.type` known-type whitelist).
- Fix iteration #1 commit `5787806` closed all 4 sub-gaps surgically (7-file surface).
- Codex per-sub-sprint review round 2 returned `decision: pass / blocking_count: 0`.
- Combined two-round Codex archive at `docs/sprints/sprint-038-codex-review.md`.

THIS session entered (2026-05-17 evening) for **Sprint 38 close housekeeping + Sprint 39 contract drafting**:

- **Batch 1 (Sprint 38 close housekeeping)** — completed pre-session (per user description in prompt):
  - `docs/10-handoff.md` §1 lead refreshed (Current phase → M2 Sprint 38 closed / Sprint 39 next; Preceding sub-sprint → Sprint 38; Earlier sub-sprint → Sprint 37).
  - `docs/action_bank.md` updated: `D-skill-runtime-framework` (line 345) partial-landing annotation (Sprint 38 + Sprint 37 progress noted; Sprint 39 + 40 + 41 named as continuation surfaces); NEW R-item `R-skill-tool-name-canonical-source-centralization` opened in §5.2 from OQ-FIX1.1; Sprint 38 close-action index row added to §6.
  - `docs/codex-findings.md` reset to scaffold (34 lines; archived combined two-round content to `docs/sprints/sprint-038-codex-review.md`).
  - NEW `docs/sprints/sprint-038-codex-review.md` (215 lines; combined two-round Codex archive per `feedback_packaging_codex_findings_supersession.md` delete-and-add pattern).
  - NEW `docs/sprints/sprint-038-objective.md` (311 lines; Sprint 38 contract archived; frontmatter `status: archived`).
- **Batch 2 (Sprint 39 contract + dev prompt + cross-session handoff)** — THIS session completed:
  - `docs/sprint_objective.md` REPLACED with Sprint 39 contract (~715 lines).
  - NEW `compact/sprint-039-dev-prompt.md` (the dev brief for Claude Code; ~750 lines).
  - NEW `compact/context-handoff-sprint-039-pre-launch.md` (THIS file).

Per the human's batch 2 framing, Sprint 39 stays as a single sub-sprint with 6 internal execution phases (no silent scope split into Sprint 40+; the milestone framework allows replanning at human review round, NOT silent scope expansion / contraction by dev). Strong STOP discipline locked in §10 of contract + §5 of dev prompt:
- No §6 #4 authorization expansion (S1 stays bounded to phase=RESOLVE_FAQ, tool=record_outcome with class=resolve, check=source_id citation presence in user-facing message).
- No generic rule-engine dispatcher (the dispatcher is bounded to 4 typed predicate types declared in Sprint 37 freeze §5.2 + §8.2).
- No touch to `UseCaseRouter` / `escalation_reason` enum / `system_prompt.txt` (M3-D / M3-A / Sprint 40 scope).
- No silent scope split.
- STOP if RESOLVE behavioural equivalence cannot be preserved.

---

## 2. Goals

### Long-term

Help the human cycle through the milestone framework: human gives architectural theme / scope → deliver-agent drafts `docs/milestone_objective.md` + per-sub-sprint `docs/sprint_objective.md` + dev/review prompts → human reviews + approves → dev/review agents execute externally → deliver-agent helps human classify close / targeted-fix / OOSR / next-sub-sprint.

For M2 specifically: Sprint 37 + Sprint 38 are CLOSED; Sprint 39 (the biggest sub-sprint by scope per multi-fence convergence) is the active surface; Sprint 40 + Sprint 41 are downstream-planned per M2 milestone objective §3.

### Immediate (this cycle, Sprint 39 launch round)

1. **Wait for human commit** of the deliver-agent batch 1 + batch 2 bundle (see §3.4 for file list + suggested commit message in §9 below).
2. **After commit + dev session launch:** Sprint 39 dev (Claude Code via `compact/sprint-039-dev-prompt.md`) produces:
   - `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml` (NEW).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (NEW; + optionally `RejectVerdict.java` + `DispatchContext.java` as separate records).
   - `PhaseEvaluator.java` extension (composeSkillPhasePlan template substitution + DELETE legacy RESOLVE branches).
   - `AgentRunLoopImpl.java` migration (3 dispatch sites → dispatcher; 3 static methods REMOVED).
   - `~60-100 new tests across 4 new test files (PhaseEvaluatorResolveSkillIntegrationTest + SkillGuardrailDispatcherTest + ResolveFaqGuardrailsTest + ResolveIntakeGuardrailsTest)`.
   - `docs/sprints/sprint-039-handoff.md` (NEW; 12-section dev-authored archive).
3. **After Sprint 39 dev commit:** deliver-agent + human review Sprint 39 handoff + behavioural-equivalence test results + 4-typed-predicate dispatcher coverage. Deliver-agent drafts `compact/sprint-039-review-prompt.md` + dispatches Codex per-sub-sprint review per §4.3 trigger #3. Deliver-agent + human classify Codex verdict.
4. **After Sprint 39 close:** draft Sprint 40 contract (teaching extraction from `system_prompt.txt` Sprint 23/31/33 paragraphs + orchestration shell cleanup per design doc §7) + Sprint 40 dev prompt; surface to human review.

### Subsequent (NEW M2 progression after Sprint 39 closes)

Per `docs/milestone_objective.md` §3 sub-sprint sequence:

- **Sprint 40** — Teaching extraction from `system_prompt.txt` (Sprint 23 `already_called` + Sprint 31 `alternate_candidate_use_cases` + Sprint 33 `discover_disambiguation_signals` SLOT description) into corresponding Skill YAML files per design doc §7; `system_prompt.txt` shrunk to orchestration shell.
- **Sprint 41** — UC switch + state preservation across Skill boundary (session-level state-bus `SkillStateBus.java` + per-Skill `state_inheritance` enforcement + new `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` per design doc §10).
- **M2 close** — deliver-agent + human dispatch milestone-shared cumulative Codex review (in addition to per-sub-sprint reviews already done at S37/S38/S39/S40/S41 close); M2 closure verdict + archive to `docs/milestones/M2_objective.md`; §6.5 row append in `docs/action_bank.md`.

### M3 candidates (informational; not pre-decided)

Per `docs/milestone_objective.md` §11 cross-milestone sequencing context (unchanged from Sprint 37 → Sprint 38 → Sprint 39 transition):

- M3-A: Customer-honesty surface (sibling rationale/confidence fields on `request_handover`; handover UX rewrite; URL policy).
- M3-B: Lifecycle (`D-single-handover-orchestrator` P0 + scoped `D-full-issue-ledger`).
- M3-C: Sprint 5 S3/S4/S5 skills (rides validated M2 Skill abstraction).
- M3-D: Topic↔UC binding loosening (`R-loosen-topic-uc-binding-llm-owned-drift` requires research-agent investigation).
- M3-Latency: consume `R-llm-provider-latency-drift-2026-05-16` (deferred from M2).
- M3-Skill-Tuning: post-M2 behavioural tuning of individual Skills (e.g., Alice closure-criterion (a) PASS if not achieved organically through abstraction landing).
- M3-Tier-0 re-evaluation: C2 (guardrail refusal non-overridability) + C3 (state-bus boundary enforcement) Tier-0 candidates — Sprint 39 ships C2's FIRST observed evidence surface (the dispatcher); Sprint 41 ships C3's FIRST observed evidence surface (the state-bus); re-evaluate at M2 close OR M3+.

---

## 3. Confirmed facts (code-grounded; verify before relying)

### 3.1 Git state at handoff authorship (2026-05-17 evening)

```
5787806 sprint 38 fix #1: SkillLoader schema-validation completeness for design doc §2.2 (close Codex Blocking Finding 1)
bd9d3f5 sprint 38: SkillRegistry core + 4 simpler phase Skills migration (NEW M2 sub-sprint 2)
649c6c7 sprint 37 close: PASS A + Sprint 38 contract + dev prompt draft (NEW M2 sub-sprint 1→2)
51c327c sprint 37: Skill Registry + state-across-Skill design freeze (NEW M2 sub-sprint 1)
6d97888 commit the mock data updates
```

Working-tree modifications at handoff authoring time (deliver-agent batch 1 + batch 2 bundle pending commit):

**Batch 1 (Sprint 38 close housekeeping)** — already in working tree per user description:

```
M docs/10-handoff.md                                (§1 lead refreshed)
M docs/action_bank.md                               (D-skill-runtime-framework partial annotation + R-skill-tool-name-canonical-source-centralization new R-item + Sprint 38 close-action index row)
M docs/codex-findings.md                            (reset to scaffold; 34 lines)
?? docs/sprints/sprint-038-codex-review.md          (NEW; 215 lines; combined two-round Codex archive)
?? docs/sprints/sprint-038-objective.md             (NEW; 311 lines; Sprint 38 contract archive)
?? compact/sprint-038-fix-dev-prompt.md             (NEW; pre-Sprint-38-fix dev prompt; archived)
?? compact/sprint-038-fix-review-prompt.md          (NEW; Sprint 38 fix-review prompt; archived)
?? compact/sprint-038-review-prompt.md              (NEW; Sprint 38 initial review prompt; archived)
```

**Batch 2 (Sprint 39 contract + dev prompt + handoff)** — THIS session added:

```
M docs/sprint_objective.md                          (REPLACED with Sprint 39 contract; OLD Sprint 38 contract already archived per batch 1)
?? compact/sprint-039-dev-prompt.md                 (NEW; Sprint 39 dev brief for Claude Code)
?? compact/context-handoff-sprint-039-pre-launch.md (THIS file)
```

### 3.2 Sprint 38 outcome (what shipped before this session)

- **Main-dev commit `bd9d3f5`** shipped the SkillRegistry core abstraction + 4 simpler phase Skills migration (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE); 5 Skill Java classes; 4 Skill YAMLs (legacy-verbatim text from `PhaseEvaluator.java:397-542`); PhaseEvaluator integration (constructor 10→11 args; SkillRegistry-first dispatch at `plan(...)` lines 403-407; new `composeSkillPhasePlan(...)` helper at line 548; 4 legacy phase branches DELETED; RESOLVE-INTAKE + RESOLVE-FAQ branches PRESERVED for Sprint 39 at lines ~417-460 + ~463-530 post-Sprint-38); 4 new test files + SkillTestFixtures + 19 existing PhaseEvaluator constructor-call-site updates. Behavioural equivalence preserved (13 golden-string assertions in `PhaseEvaluatorSkillIntegrationTest`).
- **Codex per-sub-sprint review round 1** returned `decision: fix_required / blocking_count: 1`. Codex Blocking Finding 1: `SkillLoader` schema-validation completeness for Sprint 37 freeze §2.2 — 4 sub-gaps (1a `tools_required` presence detection unreachable due to `Skill.java:66` compact-constructor null-normalization; 1b no tool-name allowlist check; 1c no explicit `applicable_use_cases` UC registry check; 1d no `Guardrail.type` known-type whitelist).
- **Fix iteration #1 commit `5787806`** closed all 4 sub-gaps surgically (7-file surface: `Skill.java` compact-constructor null-passthrough for `toolsRequired` only; `SkillLoader.java` extensions — `VALID_TOOL_NAMES` Set with 11 canonical tool names mirrored from Java `Tool.getName()` implementations, explicit-UC registry check via `UseCaseRegistryService.isKnownUseCase(...)`, `VALID_GUARDRAIL_TYPES` Set with 4 canonical predicate types per Sprint 37 freeze §5.2 + §8.2; 5 new negative tests + 1 positive UC companion in `SkillLoaderTest`; `SkillTest` assertion adjusted for null-passthrough; `SkillRegistryTest` + `SkillTestFixtures` constructor-call-site updates).
- **Codex per-sub-sprint review round 2** returned `decision: pass / blocking_count: 0` on `5787806`; all 4 sub-gap closures verified independently.
- **Combined two-round Codex archive** at `docs/sprints/sprint-038-codex-review.md` (215 lines) per `feedback_packaging_codex_findings_supersession.md` delete-and-add supersession pattern.
- **Final Sprint 38 classification: A — B-fix-iterated** (equivalent to Sprint 23/25 fix-iteration close pattern; first implementation sub-sprint under NEW M2 — closed with single targeted fix iteration on freeze-fidelity).
- **Java baseline trajectory**: 983 (post-Sprint-37) → 1028 (post-`bd9d3f5`) → 1034 (post-`5787806`); inherited `SystemPromptUserRequestedTiebreakerTest` failure persists unchanged.
- **OQ disposition**: 5 main-dev OQs (S38.1-S38.5) + 4 fix-iteration OQs (FIX1.1-FIX1.4) all resolved. OQ-S38.1 (template-vs-legacy editorial divergence on DISCOVER YAML) → AGREE WITH DEV per `doc_governance.md` code-ahead-of-docs; editorial fold-back at M2 close. OQ-FIX1.1 (centralize canonical tool-name source) → opened as NEW R-item `R-skill-tool-name-canonical-source-centralization` in §5.2 for M3+ revisit.
- **Tier-0 candidate disposition**: No new Tier-0 from Sprint 38. C1 REJECTED preserved by construction; C2 + C3 QUALIFIED-DEFER continued.

### 3.3 Sprint 39 framing (this session's product)

- `docs/sprint_objective.md` carries Sprint 39 contract (~715 lines). Status `current`; supersedes `docs/sprints/sprint-038-objective.md` per frontmatter.
- 12-section shape per Sprint 31-38 convention.
- §1 Sub-sprint class: implementation sub-sprint, single track (Track A), semantic-touching multi-layer with multi-fence convergence.
- §2 Goal: 6 execution phases (D-a through D-v) covering RESOLVE Skill YAMLs + PhaseEvaluator integration extension + Sprint 6/7/11 predicate migration + unified `SkillGuardrailDispatcher` + NEW S1 + NEW S2 + behavioural-equivalence test pattern.
- §2.6 reproduces M2 §6 #4 verbatim S1 authorization quote (LOAD-BEARING for Sprint 39).
- §3 Non-goals: explicit list including system_prompt.txt UNTOUCHED (Sprint 40 scope); SkillStateBus.java NOT created (Sprint 41 scope); no Tier-0 added; no S1 expansion; no generic rule-engine dispatcher; no on_fail: observe_only / downgrade_reason; etc.
- §4 Premise check: 15 premises verified at HEAD `5787806` (PhaseEvaluator 1529 lines post-Sprint-38; AgentRunLoopImpl 787 lines UNCHANGED; IntakeFieldsRegistry 222 lines UNCHANGED; etc.). Sprint 39 dev re-verifies at session start.
- §5 Files in scope: 14-20 files. The 6 execution phases enumerated in detail.
- §6 Hard fences: 37 fences (M2 §6 fences carried + Sprint 39-specific fences on S1 scope + dispatcher boundedness + Sprint 11 / 11.1 / 12 frozen surface preservation + M1 functional-surface preservation + scope-creep prevention).
- §7 Bundle policy: single dev commit; dev does NOT stage deliver-agent files.
- §8 §7 stanza: `prompt_projection` + `semantic_planner` + `skill_state` + Runtime-owned grounding floor + Runtime-owned capability floor per §1.4; no Tier-0 added; no semantic hardcode; generalization coverage target/neighbor/negative/N-A-for-shadow.
- §9 Success metrics: HARD GATES + observations per M2 §5 recalibration. Java baseline target 1094-1134 (1034 + ~60-100 new).
- §10 Stop conditions: 28 stop conditions; strong STOP discipline.
- §11 Handoff §11 12-section contract.
- §12 M2 milestone context.

### 3.4 Files modified/created in this deliver-agent session (NOT yet committed)

```
M docs/sprint_objective.md                          (REPLACED with Sprint 39 contract)
?? compact/sprint-039-dev-prompt.md                 (NEW; ~750 lines)
?? compact/context-handoff-sprint-039-pre-launch.md (NEW; THIS file)
```

Combined with batch 1 (already in working tree per user description):

```
M docs/10-handoff.md
M docs/action_bank.md
M docs/codex-findings.md
M docs/sprint_objective.md                          (batch 2 EDIT)
?? docs/sprints/sprint-038-codex-review.md          (batch 1 NEW)
?? docs/sprints/sprint-038-objective.md             (batch 1 NEW)
?? compact/sprint-038-fix-dev-prompt.md             (batch 1 NEW)
?? compact/sprint-038-fix-review-prompt.md          (batch 1 NEW)
?? compact/sprint-038-review-prompt.md              (batch 1 NEW)
?? compact/sprint-039-dev-prompt.md                 (batch 2 NEW)
?? compact/context-handoff-sprint-039-pre-launch.md (batch 2 NEW; THIS file)
```

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage these; human bundles at deliver-agent commit. Suggested commit message in §9 below.

---

## 4. Decision records

### 4.1 Decision — Sprint 39 stays single sub-sprint with 6 internal execution phases (no silent scope split)

Per human direction at this session start ("proceed with batch 2 now" + the framing summary listing 6 internal execution phases). Rationale (deliver-agent's interpretation; verify with human if discrepancy):

- The 5 sub-sprint sequence in `docs/milestone_objective.md` §3 lists Sprint 39 as a single sub-sprint covering RESOLVE Skill migration + Sprint 6/7/11 predicate migration + NEW S1 + NEW S2 + unified dispatcher.
- Splitting Sprint 39 into multiple sub-sprints would increase deliver-agent + Codex overhead (more planning rounds, more per-sub-sprint reviews) without architectural benefit — the 4 typed predicates + 2 RESOLVE YAMLs + dispatcher are mutually load-bearing (predicate migrations require the dispatcher; YAML guardrail declarations require the type allowlist; behavioural equivalence requires the integrated path).
- Per `iteration_governance.md` §8.5: "A sub-sprint that crosses an unrelated architectural surface is a signal that the sub-sprint belongs to a different milestone; the deliver-agent SHALL surface this at sub-sprint planning round rather than smuggle the scope across milestones." Sprint 39's scope is internally coherent (the surfaces are mutually load-bearing); no smuggle.
- The strong STOP discipline (5 in dev prompt §5; 28 in contract §10) prevents the dev from silently expanding OR contracting scope — if anything goes wrong, dev STOPs and surfaces in handoff §7 OQ for deliver-agent + human review.

### 4.2 Decision — Strong STOP discipline (5 LOAD-BEARING + 28 total)

Per human direction. The LOAD-BEARING STOPs:

1. **No §6 #4 authorization expansion** — S1 stays bounded per the 3-boundary contract (a/b/c). Verified in contract §2.6 + dev prompt §7.
2. **No generic rule-engine dispatcher** — 4 typed predicate types only; the `SkillLoader.VALID_GUARDRAIL_TYPES` allowlist is the load-bearing whitelist (Sprint 38-fix shipped).
3. **No touch to `UseCaseRouter` / `escalation_reason` enum / `system_prompt.txt`** — M3-D / M3-A / Sprint 40 scope.
4. **No silent scope split** — replanning happens at human review round, NOT silent dev expansion / contraction.
5. **STOP if RESOLVE behavioural equivalence cannot be preserved** — don't widen the equivalence assertion to accept divergence; surface the failure as STOP.

These STOPs reflect lessons from OLD M2-Skill supersession (Sprint 36 era) — silent scope drift + minimum-surface-incrementalism preserved the scattered Zhang-Sanfeng pattern Sprint 37's freeze was explicitly designed to break.

### 4.3 Decision — Sprint 39 contract acceptance bar inherits M2 §5 recalibration

Per `docs/milestone_objective.md` §5 recalibration (2026-05-17): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION only for THIS milestone; primary gate = functional review + Java tests + Sprint 37 freeze decisions honored. Sprint 39 contract §9 reflects this:
- HARD GATES: 2 RESOLVE YAMLs load; dispatcher compiles + 4 typed predicates pass tests; PhaseEvaluator integration; AgentRunLoopImpl migration; behavioural equivalence on RESOLVE × 12 representative UCs; Java baseline 1034 + ~60-100 new = ~1094-1134; M1 functional-surface preservation; Sprint 38 tests preserved; Sprint 11 / 11.1 / 12 frozen surface preserved; Codex per-sub-sprint verdict `approve`.
- OBSERVATIONS: interactive eval smoke + Alice + architecture-health metrics; MAY regress; does NOT block.

This is the same recalibration Sprint 38 contract honored. No change at Sprint 39.

### 4.4 Constitution-discipline check applied this session

Several proposed actions screened through `feedback_constitution_discipline_vs_planning_anticipation.md` 4-question check:

1. **Tier-0 candidate C2 (guardrail refusal non-overridability) elevation temptation** — REJECTED at this planning round. Sprint 39 ships the FIRST observed evidence surface (the dispatcher); deliver-agent + human re-evaluate at Sprint 39 close OR M2 close per the feedback rule. Contract §3 + dev prompt §5 STOP discipline + dev prompt §9 Q2 explicitly call out no Tier-0 elevation in Sprint 39 dev commit.
2. **`prior_use_case_carry` projection slot allowlist edit temptation** — DEFERRED to dev judgement at session start. The Sprint 38-fix `SkillLoader.VALID_PROJECTION_SLOTS` allowlist may or may not include `prior_use_case_carry` (Sprint 41 scope). Dev prompt §4.1 phase 1 stop check names 2 options: extend allowlist (recommended; future-naming acceptable) OR omit from Sprint 39 YAMLs (add in Sprint 41 alongside slot impl). Either is acceptable; dev surfaces as handoff §7 OQ.
3. **OQ-S38.1 (template-vs-legacy editorial divergence) editorial fold-back temptation** — DEFERRED to M2 close per Sprint 38 codex round 1 OQ disposition. Sprint 39 dev honors LEGACY text from the current `PhaseEvaluator.java` RESOLVE branches (NOT the design doc §6.2.5 / §6.2.6 template's editorial paraphrasing). Editorial fold-back is M2 close work.

---

## 5. Current tasks (what's in flight right now)

### 5.1 Authored and waiting for human commit

- Batch 1 (Sprint 38 close housekeeping; already in working tree):
  - `docs/10-handoff.md` §1 lead refresh.
  - `docs/action_bank.md` updates.
  - `docs/codex-findings.md` scaffold reset.
  - `docs/sprints/sprint-038-codex-review.md` NEW.
  - `docs/sprints/sprint-038-objective.md` NEW (archive).
  - `compact/sprint-038-fix-dev-prompt.md` + `compact/sprint-038-fix-review-prompt.md` + `compact/sprint-038-review-prompt.md` (Sprint 38 review prompts archived).
- Batch 2 (Sprint 39 launch round; THIS session added):
  - `docs/sprint_objective.md` REPLACED with Sprint 39 contract.
  - `compact/sprint-039-dev-prompt.md` NEW.
  - `compact/context-handoff-sprint-039-pre-launch.md` NEW (THIS file).

All staged but NOT committed. Human bundles per `feedback_commit_at_end_bundles_deliver_artefacts.md`. Suggested commit message in §9 below.

### 5.2 Pending deliver-agent work (after human commit + Sprint 39 dev session)

- (After human commit + Sprint 39 dev launches via `compact/sprint-039-dev-prompt.md`): no deliver-agent action; wait for dev commit.
- (After Sprint 39 dev commit): read Sprint 39 dev handoff at `docs/sprints/sprint-039-handoff.md` + 2 RESOLVE YAMLs + dispatcher Java class. Validate: behavioural equivalence preserved (12 representative UCs); Sprint 37 freeze decisions (e §6.2.5 + §6.2.6 + g §8 + h §9) honored; S1 bounded per M2 §6 #4 verbatim (3-boundary check); dispatcher bounded to 4 typed predicates; no per-UC-branch hardcode; no scope creep into Sprint 40/41 surfaces; Sprint 11 / 11.1 / 12 frozen surface preserved (ResolveDispositionEvaluator UNCHANGED); M1 functional-surface preserved.
- Draft `compact/sprint-039-review-prompt.md` + dispatch Codex per-sub-sprint review per §4.3 trigger #3 (Runtime grounding-floor surface + Runtime capability floor + multi-fence convergence at one sub-sprint).
- Help human classify Codex verdict at Sprint 39 close.

### 5.3 Pending deliver-agent work (after Sprint 39 closes)

- Draft Sprint 40 contract at `docs/sprint_objective.md` (replacing Sprint 39 contract; archive Sprint 39 contract to `docs/sprints/sprint-039-objective.md`). Sprint 40 = teaching extraction from `system_prompt.txt` (Sprint 23 `already_called` + Sprint 31 `alternate_candidate_use_cases` + Sprint 33 `discover_disambiguation_signals` SLOT description) into corresponding Skill YAML files per design doc §7 + M2 §3 Sprint 40 row.
- Draft `compact/sprint-040-dev-prompt.md`.
- Update `docs/action_bank.md`:
  - Flip `R-grounding-discipline-iterative-search-fabrication` to `consumed-by-Sprint-39-S1-must-cite-source-declaration` (Sprint 39 ships S1 predicate; R-item closure per M2 §7).
  - Flip `D-hard-citation-gate` to acknowledge M2 §6 #4 bounded inversion is now SHIPPED in code.
  - Refresh `D-skill-runtime-framework` (Sprint 38 partial annotation extended to Sprint 39 — RESOLVE + predicate migration + dispatcher landing).
  - Re-evaluate C2 R-item (`R-skill-guardrail-non-overridability-tier-0`) — Sprint 39 dispatcher is the FIRST observed evidence; deliver-agent + human jointly decide (defer to M2 close OR open Tier-0 candidate write-up).
  - Add Sprint 39 close-action index row.
- Surface for human review BEFORE Sprint 40 dev session launches.

### 5.4 Pending deliver-agent work (M2 close)

- Draft `compact/M2-review-prompt.md` against cumulative M2 commit range (Sprints 37 + 38 + 38-fix + 39 + 40 + 41 commits, IN ADDITION TO per-sub-sprint reviews already done).
- Help human dispatch Codex; help classify findings.
- Append §12 closure verdict to M2 milestone objective; archive to `docs/milestones/M2_objective.md`.
- Append §6.5 row for M2 in `docs/action_bank.md`.
- Refresh `docs/10-handoff.md` §1 lead.
- M2 close editorial fold-back of design doc §6.2.1 (DISCOVER template's editorial paraphrasing per OQ-S38.1) — per Sprint 38 codex round 1 disposition; M2-close fold-back per `doc_governance.md` cadence.
- Help human pick M3.

---

## 6. Next steps for the next deliver-agent session

When the next deliver-agent instance starts:

1. **Read this file first.** Then `AGENTS.md` + `compact/sprint-deliver-orchestrator.md`.
2. **Verify git state.** Run `git -C /Users/caoruixin/projects/csagent-latest status` + `git log --oneline -10`. Confirm whether the human has committed the deliver-agent batch 1 + batch 2 bundle from §3.4.
3. **Verify Sprint 39 contract state.** Confirm `docs/sprint_objective.md` is Sprint 39 contract (not Sprint 38). If unclear, ask explicitly.
4. **If Sprint 39 dev session has not yet run:** wait for human to launch (via `compact/sprint-039-dev-prompt.md`). No deliver-agent action.
5. **If Sprint 39 dev session has committed:** read Sprint 39 dev handoff + 2 RESOLVE YAMLs + dispatcher Java class + behavioural-equivalence test results; help human classify outcome; draft Sprint 39 Codex review prompt + dispatch; help classify Codex verdict.
6. **If Sprint 39 closes PASS:** draft Sprint 40 contract per `docs/milestone_objective.md` §3 Sprint 40 row + dev prompt; surface for human review.
7. **If Sprint 39 closes B-fix-iterated:** apply existing fix-iteration close pattern (per Sprint 38's close: targeted fix dev commit + fix-review Codex round + combined two-round archive at `docs/sprints/sprint-039-codex-review.md`).
8. **If Sprint 39 surfaces C2 Tier-0 candidate evidence authorized at close:** commit `runtime_freeze_and_risk_policy.md` §1/§2 addition as SEPARATE deliver-agent commit per `feedback_commit_at_end_bundles_deliver_artefacts.md` BEFORE drafting Sprint 40 contract.
9. **Throughout: respect constitution-discipline.** Re-walk the 4-question check in `feedback_constitution_discipline_vs_planning_anticipation.md` BEFORE committing any edit to `docs/current/iteration_governance.md` / `docs/foundational/*` / `docs/runtime_freeze_and_risk_policy.md`. NEW M2 §6 #12 EXCEPTION clause allows `runtime_freeze_and_risk_policy.md` edit ONLY at Sprint close / M2 close + human authorization.

---

## 7. Cautions / hard constraints / lessons

### 7.1 NEW M2 §5 acceptance recalibration is THIS milestone only

Per `docs/milestone_objective.md` §5: bad-case suite (Alice) + interactive eval are OBSERVATION not gate for NEW M2 (architecture-focused). Future milestones (esp. M3-A customer-honesty OR M3-B Single Handover Orchestrator) may revert to bad-case-suite-primary if behaviour-focused. Per-sub-sprint Codex review prompts (S37/S38/S39/S40/S41) MUST explicitly state this recalibration to avoid Codex re-importing the §5.6 default. The Sprint 39 contract §9 + dev prompt §11 honor this; the Codex review prompt at Sprint 39 close MUST also state this.

### 7.2 Hard fences preserved through Sprint 39

Per Sprint 39 contract §6 (37 items) — load-bearing fences:

- **§6 #4 verbatim authorization for S1 `must_cite_source`** — carried forward from M2 §6 #4 verbatim; bounded scope: RESOLVE_FAQ + `record_outcome(class=resolve)` + `source_id` citation presence; expansion requires new human authorization.
- **No `escalation_reason` enum widening** (`D-new-escalation-reason-enum` deferral preserved; M3-A sibling rationale/confidence fields deferred).
- **No `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` touch** (M3-D Topic↔UC binding deferred to M3+).
- **No `D-single-handover-orchestrator`** (M3-B → M3 unless Salesforce cutover surfaces).
- **No `D-full-issue-ledger`** (hard-deferred per `action_bank.md`).
- **No Tier-0 invariant added** without Sprint 37 freeze pre-authorization + human-review escalation. Sprint 39 dispatcher provides FIRST observed evidence for C2; deliver-agent + human re-evaluate at Sprint 39 close OR M2 close.
- **No edits to existing case families** under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence from M1).
- **No `iteration_governance.md` edit during M2** (constitution-discipline preserved).
- **No mocked-LLM as primary evidence** for LLM-behaviour claims.
- **No Skill prescribes customer-facing language** (LLM-owned per §1.3).
- **No Skill hard-encodes per-step argument values** beyond registered guardrail's enforced argument scope (LLM-owned per §1.3).
- **No deletion of `docs/proposals/skill_foundation_design.md`** (OLD Sprint 36 freeze) — supersession pattern only.
- **No reset/migration of M1-shipped FUNCTIONAL surfaces** (IntakeFieldExtractor + Sprint71 + Sprint 32/33 projections) beyond explicit retroactive scope. Individual case behaviour MAY shift per §5 recalibration; FUNCTIONAL behaviour preserved.
- **No `ResolveDispositionEvaluator.java` edit** (Sprint 11 / 11.1 / 12 frozen surface per `runtime_freeze_and_risk_policy.md` §1.1 #3) — only invocation site relocates.
- **No `IntakeFieldsRegistry.java` or `IntakeFieldExtractor.java` edit** (M1 Sprint 34 functional surface preserved).
- **No NEW outcome class enum** (no `resolve_no_citation`; reject-and-hint per design doc §9.5).
- **No `on_fail: observe_only` or `on_fail: downgrade_reason`** (only `reject_with_hint` in Sprint 39; `downgrade_reason` deferred per design doc §9.6 OQ).
- **No silent scope split** — replanning happens at human review round.

### 7.3 Sprint 39 dev session expected outputs

Single dev commit with:

- 2 NEW Skill YAMLs (`resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml`) under `server/src/main/resources/skills/`.
- 1 NEW Java class `SkillGuardrailDispatcher.java` (+ optionally `RejectVerdict.java` + `DispatchContext.java` as separate records).
- `PhaseEvaluator.java` EDIT (extension + DELETE legacy RESOLVE branches).
- `AgentRunLoopImpl.java` EDIT (migration + REMOVE 3 static predicate methods).
- 4 NEW test files (~60-100 tests) under `server/src/test/java/com/gumtree/csagent/`.
- Any constructor-call-site updates in existing tests (AgentRunLoopImpl + SkillTestFixtures extensions).
- `docs/sprints/sprint-039-handoff.md` (NEW; 12-section dev-authored archive).

Dev does NOT stage: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-039-*.md`, anything under `eval_interactive/`, anything in `docs/sprints/sprint-001-*` through `docs/sprints/sprint-038-*` (immutable archives), anything in `docs/milestones/`, anything in `docs/foundational/` or `docs/current/`, anything in `docs/proposals/`.

### 7.4 Codex per-sub-sprint review trigger for Sprint 39

§4.3 trigger #3 (Runtime grounding-floor surface — S1 `must_cite_source` is NEW Runtime-floor enforcement bounded per M2 §6 #4 verbatim authorization; Runtime capability floor — unified dispatcher is NEW architectural surface; multi-fence convergence at one sub-sprint requires per-sub-sprint Codex review to verify each fence + cumulative anti-hardcode kernel).

Codex verifies: (a) behavioural equivalence pre/post-migration on RESOLVE_FAQ + RESOLVE_INTAKE phases for representative UCs; (b) S1 predicate scope matches M2 §6 #4 verbatim authorization with NO expansion; (c) S2 predicate is the existing Sprint 7 §I2 semantics moved; (d) dispatcher is bounded to 4 typed predicate types; (e) no scope creep into Sprint 40/41 surfaces; (f) no per-UC-branch if-else; (g) M1 functional surfaces preserved; (h) Sprint 11 / 11.1 / 12 frozen surface preserved.

### 7.5 Key memory files (cross-session) in `.claude/agent-memory/sprint-deliver-orchestrator/`

Load all on cold start:
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev NOT stage deliver-agent files; human bundles.
- `feedback_handoff_verdict_section_delegation.md` — dev §12 closure verdict delegation.
- `feedback_out_of_scope_review_packaging_rollforward.md` — OOSR-with-packaging-note pattern.
- `feedback_close_with_codex_skipped_docs_only_outcome.md` — A-with-Codex-skipped for docs-only sprints.
- `feedback_corpus_undecidable_premise_check.md` — in-flight downgrade pattern.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every number cites source + recipe.
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM required for prompt-causal evidence (Sprint 39 dispatcher Java logic tests + behavioural-equivalence tests MAY mock LLM).
- `feedback_probe_sprint_shape_for_conditional_broadening.md` — probe sprint shape.
- `feedback_multi_layer_prospective_stanza.md` — two-track stanza shape.
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add supersession for codex-findings.
- `feedback_constitution_discipline_vs_planning_anticipation.md` — planning anticipation ≠ execution-time authorization for governance-tier edits.

### 7.6 Lessons from THIS session (additions to deliver-agent doctrine; not yet memory files)

- **Multi-fence convergence at one sub-sprint is acceptable when the surfaces are mutually load-bearing.** Sprint 39's 6 execution phases (RESOLVE YAMLs + integration extension + predicate migration + dispatcher + S1/S2 + tests) are mutually load-bearing — splitting would increase overhead without architectural benefit. Strong STOP discipline prevents silent scope drift; the contract + dev prompt make scope explicit.
- **Strong STOP discipline replaces silent scope-split prevention.** Instead of building artificial sub-sprint boundaries to prevent dev from over-reaching, codify STOP conditions in the contract + dev prompt with specific examples (e.g., "no §6 #4 authorization expansion"; "no generic rule-engine dispatcher"). When the dev hits a STOP condition, surfacing is the expected behaviour; silent work-around is the violation.
- **Reproduce LOAD-BEARING authorization quotes verbatim in the contract + dev prompt.** The M2 §6 #4 verbatim S1 authorization quote appears in: `docs/milestone_objective.md` §6 #4 + Sprint 37 freeze §8.2.4 + Sprint 39 contract §2.6 + Sprint 39 dev prompt §7. Quadruple-redundancy is intentional — the authorization is the most-likely-to-be-misread surface; reproducing prevents drift.
- **Sprint 38's OQ-S38.1 (template-vs-legacy editorial divergence) sets the precedent for Sprint 39 RESOLVE migration.** Per `doc_governance.md` code-ahead-of-docs: when the design doc template and the legacy code disagree on text wording, honor LEGACY text in the migration; the freeze template's editorial paraphrasing is fold-back work at M2 close. Sprint 39 dev prompt §3 names this explicitly so the dev doesn't waste cycles "fixing" the migration to match the design doc.

---

## 8. Quick verification checklist for the next deliver-agent

On cold start, run these to confirm state:

```bash
# Verify HEAD + commit graph
git -C /Users/caoruixin/projects/csagent-latest log --oneline -5
# Expected: top commit may be 5787806 (deliver-agent bundle not committed) OR a NEW commit if human bundled batch 1 + batch 2 + Sprint 39 dev OR just batch 1 + batch 2

# Verify deliver-agent-owned files state
git -C /Users/caoruixin/projects/csagent-latest status --short docs/sprint_objective.md docs/milestone_objective.md docs/10-handoff.md docs/action_bank.md docs/codex-findings.md docs/sprints/sprint-038-* compact/sprint-038-* compact/sprint-039-* compact/context-handoff-*

# Verify NEW M2 content (should mention "Skill Registry Abstraction" in title)
head -3 /Users/caoruixin/projects/csagent-latest/docs/milestone_objective.md

# Verify Sprint 39 content (should mention "RESOLVE_FAQ + RESOLVE_INTAKE Skill migration" in title)
head -5 /Users/caoruixin/projects/csagent-latest/docs/sprint_objective.md

# Verify Sprint 38 archives exist
ls -la /Users/caoruixin/projects/csagent-latest/docs/sprints/sprint-038-*.md

# Verify Sprint 39 dev prompt exists + mentions M2 §6 #4 verbatim authorization
head -10 /Users/caoruixin/projects/csagent-latest/compact/sprint-039-dev-prompt.md
grep -c "§6 #4 verbatim\|must_cite_source" /Users/caoruixin/projects/csagent-latest/compact/sprint-039-dev-prompt.md
# Expected: multiple matches (the quote appears ~5+ times)

# Verify Sprint 39 dev session has committed OR not
git -C /Users/caoruixin/projects/csagent-latest log --oneline -3 | grep -E "sprint 39|Sprint 39"
# If empty: dev has not committed yet (deliver-agent waits)
# If matches: dev has committed; deliver-agent + human can review

# If dev has committed: verify dev handoff exists + RESOLVE YAMLs land
test -f /Users/caoruixin/projects/csagent-latest/docs/sprints/sprint-039-handoff.md && echo "handoff present"
ls -la /Users/caoruixin/projects/csagent-latest/server/src/main/resources/skills/resolve_*.yaml
ls -la /Users/caoruixin/projects/csagent-latest/server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java

# Verify Java baseline post-Sprint-39 (if dev committed)
cd /Users/caoruixin/projects/csagent-latest/server && mvn test -q 2>&1 | tail -10
# Expected post-Sprint-39: Tests run: ~1094-1134, Failures: 1, Errors: 0, Skipped: 2
```

If anything is unexpected vs this handoff (e.g., HEAD moved unexpectedly, Sprint 39 contract edited post-handoff, dev shipped surface outside contract §5), pause and re-read state before acting.

---

## 9. Suggested commit message (if human hasn't bundled batch 1 + batch 2 yet)

```
docs: Sprint 38 close housekeeping + Sprint 39 contract + dev prompt draft (NEW M2 sub-sprint 2→3)

Sprint 38 (NEW M2 sub-sprint 2) closed B-fix-iterated 2026-05-17
across main-dev commit bd9d3f5 + fix iteration #1 commit 5787806.
Codex per-sub-sprint review fired across two rounds per
iteration_governance.md §4.3 trigger #3 — round 1 `fix_required /
blocking_count: 1` on bd9d3f5 (SkillLoader schema-validation
completeness for design doc §2.2); round 2 `pass / 0` on 5787806
confirming all 4 sub-gap closures. Final classification: A —
B-fix-iterated (first implementation sub-sprint under NEW M2;
single targeted fix iteration on freeze-fidelity).

Batch 1 (Sprint 38 close housekeeping):
- docs/codex-findings.md: reset to scaffold (34 lines; archived combined
  two-round content to docs/sprints/sprint-038-codex-review.md).
- docs/sprints/sprint-038-codex-review.md: NEW (215 lines; combined
  two-round Codex archive per `feedback_packaging_codex_findings_supersession.md`
  delete-and-add pattern).
- docs/sprints/sprint-038-objective.md: NEW (311 lines; Sprint 38 contract
  archive; frontmatter `status: archived`).
- docs/10-handoff.md: §1 lead refreshed (Current → M2 Sprint 38 closed /
  Sprint 39 next; Preceding sub-sprint → Sprint 38; Earlier sub-sprint →
  Sprint 37).
- docs/action_bank.md: D-skill-runtime-framework (line 345) partial-landing
  annotation extended to Sprint 38; NEW R-item
  R-skill-tool-name-canonical-source-centralization in §5.2 (from
  OQ-FIX1.1 for M3+ revisit); Sprint 38 close-action index row added to §6.
- compact/sprint-038-review-prompt.md + compact/sprint-038-fix-review-prompt.md
  + compact/sprint-038-fix-dev-prompt.md: archived Sprint 38 review/fix
  prompts.

Batch 2 (Sprint 39 launch round):
- docs/sprint_objective.md: REPLACED with Sprint 39 contract (~715 lines;
  RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate
  migration + NEW S1 + NEW S2 + unified Skill terminal-predicate
  dispatcher per docs/proposals/skill_registry_design.md §5.2 + §6.2.5 +
  §6.2.6 + §6.4 + §8 + §9). Single sub-sprint with 6 internal execution
  phases (no silent scope split; multi-fence convergence per human direction).
  Strong STOP discipline (28 stop conditions; 37 hard fences); LOAD-BEARING
  M2 §6 #4 verbatim S1 authorization quote reproduced in §2.6.
- compact/sprint-039-dev-prompt.md: NEW (~750 lines; the dev brief for
  Claude Code; 6-execution-phase walkthrough + STOP discipline + hard fences
  + M2 §6 #4 verbatim quote + §4.1 anti-hardcode self-walk template +
  bundle policy + handoff §11 12-section contract + commit message template).
- compact/context-handoff-sprint-039-pre-launch.md: NEW (cross-session
  handoff for next deliver-agent instance).

OQ disposition at Sprint 38 close (5 main-dev + 4 fix-iteration OQs):
- OQ-S38.1 (DISCOVER template vs legacy text editorial divergence) →
  AGREE WITH DEV per `doc_governance.md` code-ahead-of-docs; editorial
  fold-back at M2 close. Codex CONFIRMED in round 1.
- OQ-S38.2 (composer placement) → informational.
- OQ-S38.3 (maxToolSteps literal-2 default) → deferred to Sprint 39
  (RESOLVE Skills will set explicit max_tool_steps: 4 + 3 values).
- OQ-S38.4 (SkillTestFixtures.productionRegistry() coupling) → informational.
- OQ-S38.5 (C2 + C3 R-items continuation) → CONFIRM; Sprint 39 ships C2's
  first observed evidence surface (dispatcher); re-evaluate at Sprint 39
  close OR M2 close.
- OQ-FIX1.1 (centralize canonical tool-name source) → opened as NEW R-item
  R-skill-tool-name-canonical-source-centralization in §5.2 for M3+ revisit.
- OQ-FIX1.2 (freeze-canonical predicate names over informal abbreviations) →
  CONFIRM.
- OQ-FIX1.3 (UseCaseRegistryService injection no circular dependency) →
  CONFIRM.
- OQ-FIX1.4 (test runtime concern on UseCaseRegistryService re-init) →
  informational; NOT opened as R-item (micro-optimization; not load-bearing).

Tier-0 candidate disposition at Sprint 38 close: No new Tier-0. C1 REJECTED
preserved by construction (composeSkillPhasePlan + ToolDispatcher.validateAgainstPlan).
C2 + C3 QUALIFIED-DEFER continued. Sprint 39 dispatcher will provide FIRST
observed evidence for C2 (guardrail refusal non-overridability); deliver-agent
+ human re-evaluate at Sprint 39 close OR M2 close per
`feedback_constitution_discipline_vs_planning_anticipation.md`.

Sprint 39 design (this batch's product):
- Layer: prompt_projection + semantic_planner + skill_state + Runtime-owned
  grounding floor + Runtime-owned capability floor per §1.4.
- 6 execution phases (dev-internal structure): (1) RESOLVE Skill YAMLs;
  (2) PhaseEvaluator/SkillRegistry integration extension + DELETE legacy
  RESOLVE branches; (3) Sprint 6/7/11 predicate migration (REMOVE 3 static
  methods + REPLACE 3 dispatch sites); (4) unified SkillGuardrailDispatcher
  (4 typed predicates; short-circuit-on-first-reject); (5) NEW S1
  (bounded per M2 §6 #4 verbatim) + NEW S2 (existing Sprint 7 §I2 semantics
  moved); (6) behavioural-equivalence + negative + resolve-flow evidence
  (~60-100 new tests across 4 new test files).
- Java baseline target post-Sprint-39: 1034 (post-Sprint-38-fix) + ~60-100
  new = ~1094-1134/1-inherited/0/2.

Carry-forward into Sprint 39 (preserved verbatim where applicable):
M2 §6 #4 verbatim human authorization for S1 must_cite_source bounded
inversion of D-hard-citation-gate; all hard fences on RuntimeIntentClassifier
/ DriftDetector / UseCaseRouter / escalation_reason enum / INTAKE_UCS /
D-full-issue-ledger; cascade fence on existing case families;
constitution-discipline; Sprint 11 / 11.1 / 12 frozen surface
(ResolveDispositionEvaluator); M1 functional-surface preservation.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

---

End of context handoff. The next deliver-agent reads this + the role brief + verifies state, then proceeds per §5.2 (Sprint 39 dev result review + Codex dispatch) OR §5.3 (Sprint 40 contract drafting) OR §5.4 (M2 close) depending on where the dev session has reached.
