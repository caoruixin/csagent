# Deliver-agent context handoff — Sprint 40 pre-launch (NEW M2 sub-sprint 4 contract + dev prompt drafted; pending human commit + Sprint 40 dev session)

**Authored:** 2026-05-18 by deliver-agent (Sprint 39 close session)
**For:** the next deliver-agent instance picking up after Sprint 40 dev session has committed (OR helping the human decide pre-commit edits to the Sprint 40 contract + dev prompt)
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD:** `2f412b6` (Sprint 39 dev commit; A — Clean PASS closed 2026-05-18; deliver-agent Sprint 39 close housekeeping bundle + Sprint 40 launch round NOT committed yet at handoff authoring time)

Read order on cold start: this file → `AGENTS.md` (loads `iteration_governance.md` + `doc_governance.md` + `agent_context_guide.md`) → `compact/sprint-deliver-orchestrator.md` (deliver-agent role definition; long-term doctrine — **not duplicated here**) → `docs/milestone_objective.md` (NEW M2 contract) → `docs/sprint_objective.md` (Sprint 40 design freeze contract) → `compact/sprint-040-dev-prompt.md` (the dev brief Claude Code consumes) → `docs/sprints/sprint-039-handoff.md` + `docs/sprints/sprint-039-codex-review.md` (Sprint 39 close archives) → `docs/proposals/skill_registry_design.md` §7 (Sprint 37 freeze decision (f), the architectural source-of-truth Sprint 40 implements).

---

## 1. Background (Why this session happened)

Previous deliver-agent session handoff at `compact/context-handoff-sprint-039-pre-launch.md` (2026-05-17 evening) — that handoff covered the Sprint 39 launch round (Sprint 38 close housekeeping batch 1 + Sprint 39 contract drafting batch 2). Sprint 39 dev session ran across 2026-05-18 morning and committed `2f412b6`:

- 21 files shipped (10 NEW + 11 EDIT; 2621 insertions / 536 deletions) per `git diff --stat 5787806..2f412b6`.
- 2 NEW RESOLVE Skill YAMLs + 1 NEW unified `SkillGuardrailDispatcher` Java class + 2 NEW Java records + PhaseEvaluator + AgentRunLoopImpl migrations + 60 NEW tests + 8 EDIT test files + 12-section handoff.
- Java baseline 1094 / 1 inherited / 0 / 2.
- Sprint 11/11.1/12 + M1 functional surfaces preserved.
- NEW S1 `must_cite_source` bounded per M2 §6 #4 verbatim authorization.
- NEW S2 `intake_complete_required` = Sprint 7 §I2 semantics moved.

Codex per-sub-sprint review per `iteration_governance.md` §4.3 trigger #3 returned `decision: pass / blocking_count: 0` on first pass (single round). Codex independent verdict on OQ-S39.5 (C2 Tier-0 elevation timing given dispatcher landing) aligned with deliver-agent + human pre-decision: **DEFER** — "deterministic Java tests are necessary but not sufficient production evidence; no `runtime_freeze_and_risk_policy.md` edit is warranted now."

Sprint 39 final classification: **A — Clean PASS** (cleanest M2 close outcome to date: S37 PASS A → S38 B-fix-iterated → S39 PASS A; second clean A close under NEW M2; first clean A close on an implementation sub-sprint under NEW M2).

THIS session entered (2026-05-18 afternoon) for **Sprint 39 close housekeeping + Sprint 40 launch round**:

- **Batch 1 (Sprint 39 close housekeeping)** — completed this session:
  - `docs/codex-findings.md` reset to scaffold (34 lines; archived Sprint 39 Codex verdict to `docs/sprints/sprint-039-codex-review.md`).
  - NEW `docs/sprints/sprint-039-codex-review.md` (97 lines + archive frontmatter header; verbatim copy of Sprint 39 Codex output per `feedback_packaging_codex_findings_supersession.md` delete-and-add pattern).
  - NEW `docs/sprints/sprint-039-objective.md` (471 lines; Sprint 39 contract archive; frontmatter `status: archived` + `doc_tier: sprint-archive`).
  - `docs/sprints/sprint-039-handoff.md` §12 closure verdict filled (the placeholder dev left per `feedback_handoff_verdict_section_delegation.md`).
  - `docs/10-handoff.md` §1 lead refreshed (Date → 2026-05-18; Current phase → M2 Sprint 39 closed PASS A / Sprint 40 next pre-planning; Preceding sub-sprint → Sprint 39; Earlier sub-sprint → Sprint 38).
  - `docs/action_bank.md` updates: `R-grounding-discipline-iterative-search-fabrication` flipped to consumed-by-Sprint-39-S1-must-cite-source-declaration; `D-hard-citation-gate` annotated with M2 §6 #4 bounded inversion now SHIPPED in code; `D-skill-runtime-framework` partial-landing annotation extended to Sprint 39 (Sprints 37 + 38 + 39 shipped; Sprints 40 + 41 named as continuation surfaces); C2 R-item `R-skill-guardrail-non-overridability-tier-0` annotated with Sprint 39 dispatcher evidence + Codex DEFER verdict; Sprint 39 close-action index row added to §6.
- **Batch 2 (Sprint 40 launch round)** — completed this session:
  - `docs/sprint_objective.md` REPLACED with Sprint 40 contract (~330 lines).
  - NEW `compact/sprint-040-dev-prompt.md` (the dev brief for Claude Code; ~220 lines).
  - NEW `compact/context-handoff-sprint-040-pre-launch.md` (THIS file).

Per the milestone framework, Sprint 40 is a single sub-sprint with smaller scope than Sprint 39 — purely prompt-side cleanup (system_prompt.txt edit + discover_triage.yaml extension + new test). Strong STOP discipline locked in §10 of contract + §4 of dev prompt:
- No deletion of Sprint 23 `already_called` paragraph (cross-Skill envelope-mechanics teaching).
- No deletion of `request_handover` decision tree (lines 54-101; cross-Skill canonical enum).
- No per-UC-branch if-else in `discover_triage.yaml` post-migration.
- No migration into RESOLVE-FAQ or RESOLVE-INTAKE Skill YAMLs (Alternative C REJECTED per design doc §7.2.2).
- No scope creep into Sprint 41 (`SkillStateBus.java` / `ContextProjectionBuilder.java` / `prior_use_case_carry` slot impl).

---

## 2. Goals

### Long-term

Help the human cycle through the milestone framework: human gives architectural theme / scope → deliver-agent drafts `docs/milestone_objective.md` + per-sub-sprint `docs/sprint_objective.md` + dev/review prompts → human reviews + approves → dev/review agents execute externally → deliver-agent helps human classify close / targeted-fix / OOSR / next-sub-sprint.

For M2 specifically: Sprint 37 + Sprint 38 + Sprint 39 are CLOSED; Sprint 40 (teaching extraction; smaller scope than Sprint 39) is the active surface; Sprint 41 is the last M2 sub-sprint (UC switch + state preservation per design doc §10).

### Immediate (this cycle, Sprint 40 launch round)

1. **Wait for human commit** of the deliver-agent batch 1 + batch 2 bundle (see §3.4 for file list + suggested commit message in §9 below).
2. **After commit + Sprint 40 dev session launch:** Sprint 40 dev (Claude Code via `compact/sprint-040-dev-prompt.md`) produces:
   - `server/src/main/resources/prompts/system_prompt.txt` EDIT (101 → ~60-75 lines; Sprint 31 + Sprint 33 SLOT + DISCOVER guidance bulk migrated; envelope-mechanics paragraph added; Sprint 23 + decision tree preserved).
   - `server/src/main/resources/skills/discover_triage.yaml` EDIT (extends `procedure` with migrated content).
   - `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` NEW (~5-10 Java-deterministic prompt-composition tests).
   - `docs/sprints/sprint-040-handoff.md` NEW (12-section dev-authored archive).
3. **After Sprint 40 dev commit:** deliver-agent + human review Sprint 40 handoff + behavioural-equivalence test results. Deliver-agent drafts `compact/sprint-040-review-prompt.md` + dispatches Codex per-sub-sprint review per §4.3 trigger #3 (system_prompt.txt structural change — LLM input contract surface). Deliver-agent + human classify Codex verdict.
4. **After Sprint 40 close:** draft Sprint 41 contract (UC switch + state preservation across Skill boundary; `SkillStateBus.java` + per-Skill `state_inheritance` enforcement + NEW `prior_use_case_carry` projection slot impl in `ContextProjectionBuilder.java` per design doc §10) + Sprint 41 dev prompt; surface to human review.

### Subsequent (NEW M2 progression after Sprint 40 closes)

Per `docs/milestone_objective.md` §3 sub-sprint sequence:

- **Sprint 41** — UC switch + state preservation across Skill boundary (`SkillStateBus.java` + per-Skill `state_inheritance` enforcement + NEW `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` per design doc §10).
- **M2 close** — deliver-agent + human dispatch milestone-shared cumulative Codex review (in addition to per-sub-sprint reviews at S37/S38/S39/S40/S41 close); M2 closure verdict + archive to `docs/milestones/M2_objective.md`; §6.5 row append in `docs/action_bank.md`; editorial fold-back of design doc §6.2.1 / §6.2.5 / §6.2.6 / §7 templates per `doc_governance.md` cadence (OQ-S38.1 + Sprint 39 §10 deferred notes carry forward).

### M3 candidates (informational; not pre-decided)

Per `docs/milestone_objective.md` §11 cross-milestone sequencing context (unchanged from Sprint 39 → Sprint 40 transition):

- M3-A: Customer-honesty surface (sibling rationale/confidence fields on `request_handover`; handover UX rewrite; URL policy).
- M3-B: Lifecycle (`D-single-handover-orchestrator` P0 + scoped `D-full-issue-ledger`).
- M3-C: Sprint 5 S3/S4/S5 skills (rides validated M2 Skill abstraction).
- M3-D: Topic↔UC binding loosening (`R-loosen-topic-uc-binding-llm-owned-drift` requires research-agent investigation).
- M3-Latency: consume `R-llm-provider-latency-drift-2026-05-16` (deferred from M2).
- M3-Skill-Tuning: post-M2 behavioural tuning of individual Skills (e.g., Alice closure-criterion (a) PASS if not achieved organically through abstraction landing).
- M3-Tier-0 re-evaluation: C2 (guardrail refusal non-overridability) + C3 (state-bus boundary enforcement) Tier-0 candidates — Sprint 39 shipped C2's FIRST observed evidence surface; Sprint 41 ships C3's FIRST observed evidence surface; re-evaluate at M2 close OR M3+.

---

## 3. Confirmed facts (code-grounded; verify before relying)

### 3.1 Git state at handoff authorship (2026-05-18 afternoon)

```
2f412b6 sprint 39: RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 + unified dispatcher (NEW M2 sub-sprint 3)
5787806 sprint 38 fix #1: SkillLoader schema-validation completeness for design doc §2.2 (close Codex Blocking Finding 1)
bd9d3f5 sprint 38: SkillRegistry core + 4 simpler phase Skills migration (NEW M2 sub-sprint 2)
649c6c7 sprint 37 close: PASS A + Sprint 38 contract + dev prompt draft (NEW M2 sub-sprint 1→2)
51c327c sprint 37: Skill Registry + state-across-Skill design freeze (NEW M2 sub-sprint 1)
```

Working-tree modifications at handoff authoring time (deliver-agent Sprint 38 + Sprint 39 close housekeeping + Sprint 40 launch bundle pending commit):

**From pre-Sprint-39 (batch 1 + batch 2 of Sprint 39 launch; carry-over since Sprint 38 close housekeeping was not committed before Sprint 39 dev):**

```
M docs/10-handoff.md                                (§1 lead refreshed twice — first Sprint 38 close, now Sprint 39 close)
M docs/action_bank.md                               (multiple updates across Sprint 38 + Sprint 39 close housekeeping)
M docs/codex-findings.md                            (reset to scaffold twice — first Sprint 38 close, now Sprint 39 close)
M docs/sprint_objective.md                          (REPLACED twice — first to Sprint 39 contract; now to Sprint 40 contract)
?? docs/sprints/sprint-038-codex-review.md          (Sprint 38 close; carry-over)
?? docs/sprints/sprint-038-objective.md             (Sprint 38 close; carry-over)
?? compact/sprint-038-fix-dev-prompt.md             (Sprint 38 fix iteration; carry-over)
?? compact/sprint-038-fix-review-prompt.md          (Sprint 38 fix review; carry-over)
?? compact/sprint-038-review-prompt.md              (Sprint 38 initial review; carry-over)
?? compact/sprint-039-dev-prompt.md                 (Sprint 39 launch; carry-over)
?? compact/context-handoff-sprint-039-pre-launch.md (Sprint 39 launch; carry-over)
```

**THIS session added (Sprint 39 close + Sprint 40 launch):**

```
?? docs/sprints/sprint-039-codex-review.md          (NEW; Sprint 39 Codex archive)
?? docs/sprints/sprint-039-objective.md             (NEW; Sprint 39 contract archive)
M docs/sprints/sprint-039-handoff.md                (§12 closure verdict filled; this is the ONE allowed post-commit edit to a sprint archive per `feedback_handoff_verdict_section_delegation.md`)
?? compact/sprint-039-review-prompt.md              (NEW; Sprint 39 review prompt; from previous session — note: still pending commit)
?? compact/sprint-040-dev-prompt.md                 (NEW; Sprint 40 dev brief)
?? compact/context-handoff-sprint-040-pre-launch.md (NEW; THIS file)
```

### 3.2 Sprint 39 outcome (what shipped before this session)

- **Sprint 39 dev commit `2f412b6`** shipped 21 files (10 NEW + 11 EDIT; 2621 insertions / 536 deletions per `git diff --stat`): 2 NEW RESOLVE Skill YAMLs (`resolve_faq_grounded_answer.yaml` 52 lines + `resolve_intake_collect_and_handover.yaml` 37 lines) + 1 NEW unified Java dispatcher `SkillGuardrailDispatcher.java` (416 lines; Spring `@Component`; 4 typed handlers; short-circuit-on-first-reject) + 2 NEW Java records (`RejectVerdict.java` 26 lines + `DispatchContext.java` 45 lines); PhaseEvaluator.java integration extension (`composeSkillPhasePlan(...)` extended with `substitutePlaceholders(text, activeUc)` helper; legacy RESOLVE branches + `buildIntakeSystemInstruction` DELETED; 1529 → 1427 lines, −102); AgentRunLoopImpl.java migration (3 dispatch sites routed through dispatcher; 3 `shouldRejectXxx` methods + 7 dead constants REMOVED; new `@Autowired` 6-arg constructor with 5-arg backward-compat preserved; 787 → 638 lines, −149); NEW S1 `must_cite_source` predicate declared on `resolve_faq_grounded_answer.yaml` + implemented in `handleMustCiteSource` bounded per M2 §6 #4 verbatim 3-boundary check; NEW S2 `intake_complete_required` predicate declared on `resolve_intake_collect_and_handover.yaml` + implemented in `handleIntakeCompleteRequired` (Sprint 7 §I2 semantics moved bit-for-bit); Sprint 11/11.1/12 frozen surface preserved (`ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` UNCHANGED; only invocation site moves via verbatim delegation); 60 new tests across 4 NEW test files; 8 EDIT test files for constructor + dispatcher-helper sweep; 350-line dev handoff.
- **Codex per-sub-sprint review** returned `decision: pass / blocking_count: 0` on first pass (single round). All gates PASS: scope discipline (21 files match Sprint 39 contract §5); §4.1 9-question kernel `approve` per Q1-Q9; §1.7 boundary checks PASS (dispatcher bounded to 4 typed predicates NOT generic rule engine; handleMustCiteSource matches M2 §6 #4 verbatim 3-boundary; handleIntakeCompleteRequired matches Sprint 7 §I2 byte-for-byte; handlePrematureResolveOutcomeGuard verbatim delegation to UNCHANGED ResolveDispositionEvaluator; SkillLoader allowlists UNCHANGED from Sprint 38-fix); all 37 Sprint 39 §6 + all 22 M2 §6 hard fences honored; behavioural-equivalence golden tests `PhaseEvaluatorResolveSkillIntegrationTest` 14/14 PASS; MATERIAL FINDING preservation; Sprint 33 cue/slot split preservation; premise re-verification via `wc -l` confirms all line counts; Java baseline `Tests run: 1094, Failures: 1, Errors: 0, Skipped: 2` byte-identical.
- **Final Sprint 39 classification: A — Clean PASS** (cleanest M2 close outcome to date — second clean A close under NEW M2 after Sprint 37; first clean A close on an implementation sub-sprint under NEW M2).
- **Java baseline trajectory**: 1034 (post-Sprint-38-fix) → 1094 (post-`2f412b6`); inherited `SystemPromptUserRequestedTiebreakerTest` failure persists unchanged.
- **OQ disposition** (7 OQs all resolved): OQ-S39.5 (load-bearing — C2 Tier-0 elevation timing given dispatcher landing) → **Codex independent verdict aligned with deliver-agent + human pre-decision: DEFER**; OQ-S39.1 (6th `{intake_required_fields}` placeholder) AGREE; OQ-S39.2 (dispatcher convenience overloads) AGREE; OQ-S39.3 (separate record files) AGREE; OQ-S39.4 (constant relocation bit-for-bit) AGREE; OQ-S39.6 (`on_fail: downgrade_reason` not used) deferred to Sprint 40+; OQ-S39.7 (stale Sprint 38 test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`) deferred to M2 close housekeeping.
- **Tier-0 candidate disposition**: No new Tier-0. C1 REJECTED preserved by construction. C2 + C3 QUALIFIED-DEFER continued; Sprint 39 dispatcher landing recorded as C2's FIRST observed evidence surface but production trace evidence pending across Sprint 40 + Sprint 41 window before re-evaluation at M2 close OR M3+.
- **Codex §10 non-blocking observations**: (a) Sprint 39 handoff §9 table cites RESOLVE YAMLs as 50/32 lines and "Total: 14 files" but `wc -l` and `git diff --stat` show 52/37 and 21 files — informational drift; sprint archive immutable; left as-is. (b) Deliver-agent's review prompt insertion/deletion estimate (~2191/539) differed from actual `git diff --stat` (2621/536) — evidence-count correction; recorded as `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse. (c) Editorial divergence between Sprint 37 design doc §6.2.5/§6.2.6 templates and legacy-verbatim YAML routed to M2 close fold-back per `doc_governance.md` code-ahead-of-docs.

### 3.3 Sprint 40 framing (this session's product)

- `docs/sprint_objective.md` carries Sprint 40 contract (~330 lines). Status `current`; supersedes `docs/sprints/sprint-039-objective.md` per frontmatter.
- 12-section shape per Sprint 31-39 convention.
- §1 Sub-sprint class: implementation sub-sprint, single track (Track A), semantic-touching multi-layer (`prompt_projection` + `semantic_planner`).
- §2 Goal: 5 deliverables D-a/D-b/D-c/D-d/D-e + 1 OPTIONAL D-f (inherited test resolution).
- §3 Non-goals: explicit list including Sprint 23 paragraph STAYS, `request_handover` decision tree STAYS, NO migration to RESOLVE Skills (Alt C rejected), Sprint 41 surfaces UNTOUCHED, etc.
- §4 Premise check: 15 premises verified at HEAD `2f412b6`. Sprint 40 dev re-verifies at session start.
- §5 Files in scope: ~4 files (1 system_prompt.txt EDIT + 1 discover_triage.yaml EDIT + 1 NEW test file + handoff).
- §6 Hard fences: 33 fences.
- §7 Bundle policy: single dev commit; dev does NOT stage deliver-agent files.
- §8 §7 stanza: `prompt_projection` + `semantic_planner`; no Tier-0; no semantic hardcode; generalization coverage target/neighbor/negative/N-A-for-shadow.
- §9 Success metrics: HARD GATES + observations per M2 §5 recalibration. Java baseline target 1099-1104 (1094 + ~5-10 new).
- §10 Stop conditions: 18 stop conditions.
- §11 Handoff §11 12-section contract.
- §12 M2 milestone context.

### 3.4 Files modified/created in this deliver-agent session (NOT yet committed)

```
M docs/sprint_objective.md                          (REPLACED with Sprint 40 contract)
M docs/codex-findings.md                            (reset to scaffold post-Sprint-39-Codex)
M docs/10-handoff.md                                (§1 lead refreshed)
M docs/action_bank.md                               (R-grounding consumed + D-hard-citation-gate SHIPPED + D-skill-runtime-framework Sprint 39 + C2 R-item Sprint 39 evidence + Sprint 39 close-action row)
M docs/sprints/sprint-039-handoff.md                (§12 closure verdict filled)
?? docs/sprints/sprint-039-codex-review.md          (NEW; Sprint 39 Codex archive)
?? docs/sprints/sprint-039-objective.md             (NEW; Sprint 39 contract archive)
?? compact/sprint-040-dev-prompt.md                 (NEW; Sprint 40 dev brief)
?? compact/context-handoff-sprint-040-pre-launch.md (NEW; THIS file)
```

Combined with Sprint 38 + Sprint 39 carry-over (per §3.1):

```
M docs/10-handoff.md
M docs/action_bank.md
M docs/codex-findings.md
M docs/sprint_objective.md
M docs/sprints/sprint-039-handoff.md                (this session edit)
?? docs/sprints/sprint-038-codex-review.md
?? docs/sprints/sprint-038-objective.md
?? compact/sprint-038-fix-dev-prompt.md
?? compact/sprint-038-fix-review-prompt.md
?? compact/sprint-038-review-prompt.md
?? compact/sprint-039-dev-prompt.md
?? compact/context-handoff-sprint-039-pre-launch.md
?? docs/sprints/sprint-039-codex-review.md          (this session)
?? docs/sprints/sprint-039-objective.md             (this session)
?? compact/sprint-039-review-prompt.md
?? compact/sprint-040-dev-prompt.md                 (this session)
?? compact/context-handoff-sprint-040-pre-launch.md (this session)
```

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage these; human bundles at deliver-agent commit. Suggested commit message in §9 below.

---

## 4. Decision records

### 4.1 Decision — Sprint 40 keeps simpler scope (content-relocation only)

Per human direction at Sprint 39 close + per `docs/milestone_objective.md` §3 Sprint 40 row + design doc §7 mapping:

- Sprint 40 scope is bounded to teaching extraction from `system_prompt.txt` into `discover_triage.yaml`. No Java code change. No other Skill YAML edits.
- 5 deliverables + 1 optional (D-f inherited test resolution).
- The Sprint 33 cue/slot split closes at Sprint 40 (cue migrated in Sprint 38; SLOT description migrates in Sprint 40).

Rationale (deliver-agent's interpretation; verify with human if discrepancy):
- Sprint 39 was the multi-fence convergence sub-sprint by design (the biggest M2 sub-sprint); Sprint 40 + Sprint 41 are deliberately smaller (Sprint 40 = prompt-side cleanup; Sprint 41 = state-bus impl).
- Sprint 40 inherits the LLM-input-contract refactor surface; the smaller scope means less risk of cross-fence interactions; Codex review at close stays focused on §7 mapping fidelity.

### 4.2 Decision — Strong STOP discipline (5 LOAD-BEARING + 18 total)

Per human direction (carried forward from Sprint 39):

1. **No deletion of Sprint 23 `already_called` paragraph** — STAYS per design doc §7.2.1 (cross-Skill envelope-mechanics teaching).
2. **No deletion of `request_handover` decision tree** at `system_prompt.txt:54-101` — STAYS per design doc §7.3 (cross-Skill canonical enum).
3. **No per-UC-branch if-else in `discover_triage.yaml` post-migration** per Constitution §1.7 + M2 §6 #1.
4. **No migration into RESOLVE-FAQ or RESOLVE-INTAKE Skill YAMLs** per design doc §7.2.2 Alternative C REJECTED.
5. **No scope creep into Sprint 41** — `SkillStateBus.java` / `ContextProjectionBuilder.java` / `prior_use_case_carry` slot impl are Sprint 41 scope.

These STOPs reflect Sprint 37 + Sprint 38 + Sprint 39 close discipline carried forward. Codify in dev prompt §4 with specific examples.

### 4.3 Decision — Sprint 40 contract acceptance bar inherits M2 §5 recalibration

Per `docs/milestone_objective.md` §5 recalibration (2026-05-17): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION only for THIS milestone; primary gate = functional review + Java tests + Sprint 37 freeze decisions honored. Sprint 40 contract §9 reflects this:
- HARD GATES: 3 migrations applied to system_prompt.txt + discover_triage.yaml; envelope-mechanics paragraph added; Sprint 23 + decision tree preserved; behavioural-equivalence integration test passes; Java baseline ~1099-1104; M1 + Sprint 38 + Sprint 39 functional-surface preservation; Codex per-sub-sprint verdict `approve`.
- OBSERVATIONS: interactive eval smoke + Alice + architecture-health metrics; MAY regress; does NOT block.

This is the same recalibration Sprint 38 + Sprint 39 contracts honored. No change at Sprint 40.

### 4.4 Constitution-discipline check applied this session

Several proposed actions screened through `feedback_constitution_discipline_vs_planning_anticipation.md` 4-question check:

1. **Tier-0 candidate C2 (guardrail refusal non-overridability) elevation temptation at Sprint 39 close** — REJECTED at this close round per Codex independent verdict. C2 R-item stays open in `docs/action_bank.md` §5.2 for M2 close OR M3+ revisit. Sprint 39 close housekeeping annotated C2 with Sprint 39 dispatcher landing as FIRST observed evidence surface but DEFER preserved.
2. **`prior_use_case_carry` projection slot impl temptation in Sprint 40** — DEFERRED to Sprint 41. The slot is declared in Sprint 39 RESOLVE Skill YAMLs `state_inheritance.soft_signal_via_projection` (forward-compat per Sprint 38-fix allowlist) but NOT enforced until Sprint 41's `SkillStateBus.java` + `ContextProjectionBuilder.java` impl. Sprint 40 explicitly does NOT touch this.
3. **OQ-S38.1 (template-vs-legacy editorial divergence) M2 close fold-back** — DEFERRED to M2 close per `doc_governance.md` cadence. Sprint 38 + Sprint 39 close added the design doc §6.2.5/§6.2.6 + §7 template paraphrasing vs implementation-verbatim divergence to the M2 close fold-back queue. Sprint 40 dev honors LEGACY text per code-ahead-of-docs (the same precedent applies if Sprint 40 surfaces additional editorial divergence on §7 template text vs migrated content).

---

## 5. Current tasks (what's in flight right now)

### 5.1 Authored and waiting for human commit

- Sprint 38 close housekeeping carry-over (still uncommitted from Sprint 39 launch):
  - `docs/sprints/sprint-038-objective.md` + `sprint-038-codex-review.md` archives.
  - `compact/sprint-038-fix-dev-prompt.md` + `sprint-038-fix-review-prompt.md` + `sprint-038-review-prompt.md` archives.
- Sprint 39 launch carry-over:
  - `compact/sprint-039-dev-prompt.md` + `context-handoff-sprint-039-pre-launch.md` + `sprint-039-review-prompt.md`.
- Sprint 39 close housekeeping (THIS session added):
  - `docs/sprints/sprint-039-objective.md` + `sprint-039-codex-review.md` archives.
  - `docs/sprints/sprint-039-handoff.md` §12 closure verdict filled.
  - `docs/codex-findings.md` reset to scaffold.
  - `docs/10-handoff.md` §1 lead refreshed.
  - `docs/action_bank.md` updates.
- Sprint 40 launch (THIS session added):
  - `docs/sprint_objective.md` REPLACED with Sprint 40 contract.
  - `compact/sprint-040-dev-prompt.md` NEW.
  - `compact/context-handoff-sprint-040-pre-launch.md` NEW (THIS file).

All staged but NOT committed. Human bundles per `feedback_commit_at_end_bundles_deliver_artefacts.md`. Suggested commit message in §9 below.

### 5.2 Pending deliver-agent work (after human commit + Sprint 40 dev session)

- (After human commit + Sprint 40 dev launches via `compact/sprint-040-dev-prompt.md`): no deliver-agent action; wait for dev commit.
- (After Sprint 40 dev commit): read Sprint 40 dev handoff at `docs/sprints/sprint-040-handoff.md` + system_prompt.txt + discover_triage.yaml diffs + new test file. Validate: 3 migrations applied; Sprint 23 paragraph + decision tree preserved; envelope-mechanics paragraph added; no per-UC-branch hardcode in `discover_triage.yaml`; no scope creep into Sprint 41 surfaces; M1 + Sprint 38 + Sprint 39 tests preserved.
- Draft `compact/sprint-040-review-prompt.md` + dispatch Codex per-sub-sprint review per §4.3 trigger #3 (system_prompt.txt structural change — LLM input contract surface).
- Help human classify Codex verdict at Sprint 40 close.

### 5.3 Pending deliver-agent work (after Sprint 40 closes)

- Draft Sprint 41 contract at `docs/sprint_objective.md` (replacing Sprint 40 contract; archive Sprint 40 contract to `docs/sprints/sprint-040-objective.md`). Sprint 41 = UC switch + state preservation across Skill boundary per `docs/proposals/skill_registry_design.md` §10 + M2 §3 Sprint 41 row.
- Draft `compact/sprint-041-dev-prompt.md`.
- Update `docs/action_bank.md`:
  - Refresh `D-skill-runtime-framework` (Sprint 40 progress; Sprint 41 named as last continuation surface).
  - Add Sprint 40 close-action index row.
- Surface for human review BEFORE Sprint 41 dev session launches.

### 5.4 Pending deliver-agent work (M2 close)

- Draft `compact/M2-review-prompt.md` against cumulative M2 commit range (Sprints 37 + 38 + 38-fix + 39 + 40 + 41 commits, IN ADDITION TO per-sub-sprint reviews already done).
- Help human dispatch Codex; help classify findings.
- Append §12 closure verdict to M2 milestone objective; archive to `docs/milestones/M2_objective.md`.
- Append §6.5 row for M2 in `docs/action_bank.md`.
- Refresh `docs/10-handoff.md` §1 lead.
- M2 close editorial fold-back: design doc §6.2.1 (DISCOVER template per OQ-S38.1) + §6.2.5 + §6.2.6 (RESOLVE templates per Sprint 39 §10 note) + §7 (teaching mapping per Sprint 40 if any divergence surfaced) editorial paraphrasing per `doc_governance.md` cadence. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: SEPARATE governance commit (not bundled with milestone close housekeeping).
- C2 + C3 R-item re-evaluation at M2 close (potential elevation OR continued DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md`).
- Help human pick M3.

---

## 6. Next steps for the next deliver-agent session

When the next deliver-agent instance starts:

1. **Read this file first.** Then `AGENTS.md` + `compact/sprint-deliver-orchestrator.md`.
2. **Verify git state.** Run `git -C /Users/caoruixin/projects/csagent-latest status` + `git log --oneline -10`. Confirm whether the human has committed the deliver-agent batch 1 + batch 2 (+ carry-over) bundle from §3.4.
3. **Verify Sprint 40 contract state.** Confirm `docs/sprint_objective.md` is Sprint 40 contract (not Sprint 39). If unclear, ask explicitly.
4. **If Sprint 40 dev session has not yet run:** wait for human to launch (via `compact/sprint-040-dev-prompt.md`). No deliver-agent action.
5. **If Sprint 40 dev session has committed:** read Sprint 40 dev handoff + system_prompt.txt diff + discover_triage.yaml diff + new test results; help human classify outcome; draft Sprint 40 Codex review prompt + dispatch; help classify Codex verdict.
6. **If Sprint 40 closes PASS:** draft Sprint 41 contract per `docs/milestone_objective.md` §3 Sprint 41 row + dev prompt; surface for human review.
7. **If Sprint 40 closes B-fix-iterated:** apply existing fix-iteration close pattern (Sprint 38 precedent).
8. **If Sprint 40 surfaces C2 Tier-0 candidate evidence authorized at close (unlikely; Sprint 40 is content-relocation, not dispatcher work):** treat per `feedback_constitution_discipline_vs_planning_anticipation.md`.
9. **Throughout: respect constitution-discipline.** Re-walk the 4-question check in `feedback_constitution_discipline_vs_planning_anticipation.md` BEFORE committing any edit to `docs/current/iteration_governance.md` / `docs/foundational/*` / `docs/runtime_freeze_and_risk_policy.md`. NEW M2 §6 #12 EXCEPTION clause allows `runtime_freeze_and_risk_policy.md` edit ONLY at Sprint close / M2 close + human authorization.

---

## 7. Cautions / hard constraints / lessons

### 7.1 NEW M2 §5 acceptance recalibration is THIS milestone only

Per `docs/milestone_objective.md` §5: bad-case suite (Alice) + interactive eval are OBSERVATION not gate for NEW M2 (architecture-focused). Future milestones (esp. M3-A customer-honesty OR M3-B Single Handover Orchestrator) may revert to bad-case-suite-primary if behaviour-focused. Per-sub-sprint Codex review prompts (S37/S38/S39/S40/S41) MUST explicitly state this recalibration to avoid Codex re-importing the §5.6 default. The Sprint 40 contract §9 + dev prompt §6 honor this; the Codex review prompt at Sprint 40 close MUST also state this.

### 7.2 Hard fences preserved through Sprint 40

Per Sprint 40 contract §6 (33 items) — load-bearing fences:

- **No deletion of Sprint 23 `already_called` paragraph** (`system_prompt.txt:23-28`) — STAYS per design doc §7.2.1.
- **No deletion of `request_handover` decision tree** (`system_prompt.txt:54-101`) — STAYS per design doc §7.3.
- **No edit to any Java code** in Sprint 40 dev commit (Sprint 40 is prompt-side cleanup; only NEW test file is the Java surface).
- **No edit to other Skill YAMLs** (only `discover_triage.yaml` extends).
- **No `SkillStateBus.java` created; no `ContextProjectionBuilder.java` edit; no `prior_use_case_carry` slot impl** — Sprint 41 scope.
- **No touch to `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool`** (M2 §6 #5 fence).
- **No edit to `INTAKE_UCS` set; no widening of `escalation_reason` enum** (M2 §6 #6 + #7 fences).
- **No edit to `IntakeFieldsRegistry.java` / `IntakeFieldExtractor.java` / `UseCaseRegistryService.java` / `ResolveDispositionEvaluator.java`** (M1 functional surface + Sprint 11/11.1/12 frozen surface).
- **No Tier-0 invariant added** to `docs/runtime_freeze_and_risk_policy.md` (M2 §6 #8 fence).
- **No edit to governance docs / freeze docs / sprint archives / milestone archives / deliver-agent-owned files / `eval_interactive/`**.
- **No mocked-LLM as primary evidence** for LLM-behaviour claims.
- **No per-UC-branch if-else in `discover_triage.yaml` post-migration** per Constitution §1.7 + M2 §6 #1.

### 7.3 Sprint 40 dev session expected outputs

Single dev commit with:

- `server/src/main/resources/prompts/system_prompt.txt` EDIT (101 → ~60-75 lines; Sprint 31 + Sprint 33 SLOT + DISCOVER guidance bulk migrated; envelope-mechanics paragraph added).
- `server/src/main/resources/skills/discover_triage.yaml` EDIT (extends `procedure` with migrated content).
- `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` NEW (~5-10 tests).
- `docs/sprints/sprint-040-handoff.md` NEW (12-section dev archive).

Dev does NOT stage: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-040-*.md`, anything under `eval_interactive/`, anything in `docs/sprints/sprint-001-*` through `docs/sprints/sprint-039-*` (immutable archives), anything in `docs/milestones/`, anything in `docs/foundational/` or `docs/current/`, anything in `docs/proposals/`.

### 7.4 Codex per-sub-sprint review trigger for Sprint 40

§4.3 trigger #3 (system_prompt.txt structural change — LLM input contract surface). Codex verifies:

- (a) behavioural equivalence on representative DISCOVER + RESOLVE-FAQ + RESOLVE-INTAKE traces (LLM observes the same teaching content via Skill envelope projection post-migration).
- (b) Sprint 23 `already_called` paragraph STAYS in shell per §7.2.1.
- (c) `request_handover` decision tree at lines 54-101 STAYS in shell per §7.3.
- (d) Sprint 31 + Sprint 33 paragraphs DELETED from shell + migrated to `discover_triage.yaml`.
- (e) DISCOVER phase guidance bulk migrated with one-line shell pointer remaining.
- (f) orchestration-shell paragraph about Skill envelope mechanics ADDED.
- (g) no scope creep into Sprint 41 surfaces.
- (h) no per-UC-branch if-else in `discover_triage.yaml` post-migration.
- (i) M1 + Sprint 38 + Sprint 39 functional surfaces preserved.
- (j) no Tier-0 invariant added.

### 7.5 Key memory files (cross-session) in `.claude/agent-memory/sprint-deliver-orchestrator/`

Load all on cold start:
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev NOT stage deliver-agent files; human bundles.
- `feedback_handoff_verdict_section_delegation.md` — dev §12 closure verdict delegation; deliver-agent fills at close.
- `feedback_out_of_scope_review_packaging_rollforward.md` — OOSR-with-packaging-note pattern.
- `feedback_close_with_codex_skipped_docs_only_outcome.md` — A-with-Codex-skipped for docs-only sprints (not applicable to Sprint 40; included for completeness).
- `feedback_corpus_undecidable_premise_check.md` — in-flight downgrade pattern.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every number cites source + recipe; Sprint 39 review prompt's `~2191/539` vs actual 2621/536 is a documented lapse to avoid.
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — real-LLM required for prompt-causal evidence; Sprint 40 Java prompt-composition tests MAY mock LLM (deterministic Java logic) but no real-LLM eval rerun is required.
- `feedback_probe_sprint_shape_for_conditional_broadening.md` — probe sprint shape.
- `feedback_multi_layer_prospective_stanza.md` — two-track stanza shape.
- `feedback_packaging_codex_findings_supersession.md` — delete-and-add supersession for codex-findings at archive.
- `feedback_constitution_discipline_vs_planning_anticipation.md` — planning anticipation ≠ execution-time authorization for governance-tier edits.

### 7.6 Lessons from THIS session (additions to deliver-agent doctrine; not yet memory files)

- **Sprint 39 clean A close demonstrates the multi-fence convergence pattern works when STOP discipline is codified pre-launch.** Sprint 39's 6 execution phases (RESOLVE YAMLs + integration extension + predicate migration + dispatcher + S1/S2 + tests) all landed cleanly with Codex `pass / 0` on first pass; the strong STOP discipline (5 LOAD-BEARING STOPs + 28 contract stop conditions + 37 contract hard fences) prevented silent scope drift. Sprint 40 inherits this discipline pattern but with smaller scope.
- **Reproducibility lapse on the Sprint 39 review prompt insertion/deletion estimate (~2191/539 vs actual 2621/536)** is recorded as a `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse. Next deliver-agent (THIS session's product Sprint 40 review prompt; AND future review prompts): cite insertion/deletion counts from `git show --numstat <commit>` or `git diff --stat <range>` directly, NOT from mental estimation; if no commit exists at review-prompt-drafting time (because the review prompt is drafted BEFORE the dev commit), state explicitly "insertion/deletion counts will be verified by Codex via `git diff --stat`" instead of pre-estimating.
- **Sprint 33 cue/slot split closes at Sprint 40.** Sprint 38 migrated the cue body (PhaseEvaluator.java:432-448 → discover_triage.yaml procedure per decision (e) §6.2.1). Sprint 40 migrates the slot description (system_prompt.txt:36-43 → discover_triage.yaml procedure per decision (f) §7.2.3). Post-Sprint-40, the Sprint 33 disambiguation teaching is fully consolidated in `discover_triage.yaml`. This was anticipated in Sprint 37 freeze §7.2.3 and is a 2-step migration spanning Sprint 38 + Sprint 40.

---

## 8. Quick verification checklist for the next deliver-agent

On cold start, run these to confirm state:

```bash
# Verify HEAD + commit graph
git -C /Users/caoruixin/projects/csagent-latest log --oneline -5
# Expected: top commit may be 2f412b6 (deliver-agent bundle not committed) OR a NEW commit if human bundled Sprint 39 close + Sprint 40 launch + Sprint 40 dev OR just close + launch

# Verify deliver-agent-owned files state
git -C /Users/caoruixin/projects/csagent-latest status --short docs/sprint_objective.md docs/milestone_objective.md docs/10-handoff.md docs/action_bank.md docs/codex-findings.md docs/sprints/sprint-038-* docs/sprints/sprint-039-* compact/sprint-039-* compact/sprint-040-* compact/context-handoff-*

# Verify NEW M2 content (should mention "Skill Registry Abstraction" in title)
head -3 /Users/caoruixin/projects/csagent-latest/docs/milestone_objective.md

# Verify Sprint 40 content (should mention "Teaching extraction from system_prompt.txt" in title)
head -5 /Users/caoruixin/projects/csagent-latest/docs/sprint_objective.md

# Verify Sprint 39 archives exist
ls -la /Users/caoruixin/projects/csagent-latest/docs/sprints/sprint-039-*.md

# Verify Sprint 40 dev prompt exists + mentions §7 mapping
head -10 /Users/caoruixin/projects/csagent-latest/compact/sprint-040-dev-prompt.md
grep -c "§7\|design doc §7\|Sprint 23\|Sprint 31\|Sprint 33" /Users/caoruixin/projects/csagent-latest/compact/sprint-040-dev-prompt.md
# Expected: multiple matches (the §7 mapping is the core of Sprint 40 scope)

# Verify Sprint 40 dev session has committed OR not
git -C /Users/caoruixin/projects/csagent-latest log --oneline -3 | grep -E "sprint 40|Sprint 40"
# If empty: dev has not committed yet (deliver-agent waits)
# If matches: dev has committed; deliver-agent + human can review

# If dev has committed: verify dev handoff exists + system_prompt.txt shrank
test -f /Users/caoruixin/projects/csagent-latest/docs/sprints/sprint-040-handoff.md && echo "handoff present"
wc -l /Users/caoruixin/projects/csagent-latest/server/src/main/resources/prompts/system_prompt.txt
# Expected post-Sprint-40: ~60-75 lines (was 101 pre-Sprint-40)

# Verify discover_triage.yaml extended
wc -l /Users/caoruixin/projects/csagent-latest/server/src/main/resources/skills/discover_triage.yaml
# Expected post-Sprint-40: ~50-60 lines (was ~30 pre-Sprint-40)

# Verify Java baseline post-Sprint-40 (if dev committed)
cd /Users/caoruixin/projects/csagent-latest/server && mvn test -q 2>&1 | tail -10
# Expected post-Sprint-40: Tests run: ~1099-1104, Failures: 1 OR 0, Errors: 0, Skipped: 2 (the 1 inherited may resolve organically per D-f; if it does, Failures: 0)
```

If anything is unexpected vs this handoff (e.g., HEAD moved unexpectedly, Sprint 40 contract edited post-handoff, dev shipped surface outside contract §5), pause and re-read state before acting.

---

## 9. Suggested commit message (if human hasn't bundled close + launch yet)

```
docs: Sprint 39 close housekeeping + Sprint 40 contract + dev prompt draft (NEW M2 sub-sprint 3→4)

Sprint 39 (NEW M2 sub-sprint 3) closed A — Clean PASS 2026-05-18
at dev commit 2f412b6. Codex per-sub-sprint review per
`iteration_governance.md` §4.3 trigger #3 returned `decision: pass /
blocking_count: 0` on first pass (single round; cleanest M2 close
outcome to date: S37 PASS A → S38 B-fix-iterated → S39 PASS A).

Sprint 39 dev shipped (21 files; 2621 insertions / 536 deletions):
2 NEW RESOLVE Skill YAMLs (`resolve_faq_grounded_answer.yaml` 52
lines with 3 guardrails in declaration order; `resolve_intake_collect_and_handover.yaml`
37 lines with 1 guardrail + 6 template-substitution placeholders);
1 NEW unified Java dispatcher `SkillGuardrailDispatcher.java` (416
lines; Spring @Component; 4 typed handlers; short-circuit-on-first-reject
per design doc §9.2); 2 NEW Java records (`RejectVerdict.java` +
`DispatchContext.java`); PhaseEvaluator.java integration extension
(`composeSkillPhasePlan(...)` + `substitutePlaceholders(text, activeUc)`;
legacy RESOLVE branches + legacy `buildIntakeSystemInstruction(...)`
DELETED; 1529 → 1427 lines, −102); AgentRunLoopImpl.java migration
(3 dispatch sites routed through dispatcher; 3 `shouldRejectXxx`
static methods + 7 dead constants REMOVED; new @Autowired 6-arg
constructor + backward-compat 5-arg preserved; 787 → 638 lines,
−149); NEW S1 `must_cite_source` bounded per M2 §6 #4 verbatim
3-boundary check; NEW S2 `intake_complete_required` (Sprint 7 §I2
semantics moved bit-for-bit); Sprint 11/11.1/12 frozen surface
preserved (ResolveDispositionEvaluator UNCHANGED); 60 new tests
across 4 NEW test files; 8 EDIT test files for constructor +
dispatcher-helper sweep; 350-line dev handoff. Java baseline 1034
→ 1094 (1 inherited failure persists unchanged).

Codex independent verification confirmed: scope discipline (21
files match Sprint 39 contract §5; ResolveDispositionEvaluator /
IntakeFieldsRegistry / UseCaseRegistryService / system_prompt.txt /
ContextProjectionBuilder / SkillLoader allowlists / governance docs
/ proposal docs / sprint archives / milestone archives /
deliver-agent-owned files / eval_interactive/ all UNCHANGED in dev
commit); §4.1 9-question kernel `approve`; §1.7 boundary checks
PASS (dispatcher bounded to 4 typed predicates NOT generic rule
engine; handleMustCiteSource matches M2 §6 #4 verbatim 3-boundary;
handleIntakeCompleteRequired matches Sprint 7 §I2 byte-for-byte;
handlePrematureResolveOutcomeGuard verbatim delegation to UNCHANGED
ResolveDispositionEvaluator); all 37 Sprint 39 §6 + all 22 M2 §6
hard fences honored; behavioural-equivalence golden tests 14/14
PASS; MATERIAL FINDING + Sprint 33 cue/slot split + premise
re-verification all PASS; Java baseline 1094/1-inherited/0/2
byte-identical.

OQ disposition at Sprint 39 close (7 OQs):
- OQ-S39.5 (load-bearing — C2 Tier-0 elevation timing given
  dispatcher landing) → Codex independent verdict aligned with
  deliver-agent + human pre-decision: **DEFER** ("deterministic Java
  tests are necessary but not sufficient production evidence;
  no runtime_freeze_and_risk_policy.md edit warranted"). C2 R-item
  stays open for M2 close OR M3+ revisit.
- OQ-S39.1 (6th `{intake_required_fields}` placeholder) AGREE.
- OQ-S39.2 (dispatcher convenience overloads) AGREE.
- OQ-S39.3 (separate record files) AGREE.
- OQ-S39.4 (constant relocation bit-for-bit) AGREE.
- OQ-S39.6 (`on_fail: downgrade_reason` not used) deferred Sprint 40+.
- OQ-S39.7 (stale Sprint 38 test name) deferred M2 close housekeeping.

Tier-0 candidate disposition at Sprint 39 close: No new Tier-0.
C1 REJECTED preserved by construction. C2 + C3 QUALIFIED-DEFER
continued — Sprint 39 dispatcher landing recorded as C2's FIRST
observed evidence surface but production trace evidence pending
across Sprint 40 + Sprint 41 window before re-evaluation at M2
close OR M3+ per
`feedback_constitution_discipline_vs_planning_anticipation.md`.

Sprint 39 close housekeeping (this commit's Batch 1):
- docs/codex-findings.md: reset to scaffold (archived combined Codex
  Sprint 39 verdict to docs/sprints/sprint-039-codex-review.md).
- docs/sprints/sprint-039-codex-review.md: NEW (archive frontmatter
  header + verbatim Codex output per
  `feedback_packaging_codex_findings_supersession.md` delete-and-add).
- docs/sprints/sprint-039-objective.md: NEW (Sprint 39 contract
  archive; frontmatter `status: archived`).
- docs/sprints/sprint-039-handoff.md: §12 closure verdict filled
  (`A — Clean PASS`; Codex independent verification details; OQ
  disposition; R-item flip notes).
- docs/10-handoff.md: §1 lead refreshed (Date → 2026-05-18; Current →
  M2 Sprint 39 closed PASS A / Sprint 40 next pre-planning; Preceding
  sub-sprint → Sprint 39; Earlier sub-sprint → Sprint 38).
- docs/action_bank.md: R-grounding-discipline-iterative-search-fabrication
  flipped to consumed-by-Sprint-39-S1-must-cite-source-declaration;
  D-hard-citation-gate annotated with M2 §6 #4 bounded inversion now
  SHIPPED in code at SkillGuardrailDispatcher.handleMustCiteSource
  + resolve_faq_grounded_answer.yaml guardrails (general gate
  deferral preserved); D-skill-runtime-framework partial-landing
  annotation extended to Sprint 39 (Sprints 37 + 38 + 39 shipped;
  Sprints 40 + 41 named as continuation surfaces); C2 R-item
  annotated with Sprint 39 dispatcher landing as FIRST observed
  evidence + Codex DEFER verdict (R-item stays open); Sprint 39
  close-action index row added to §6.

Sprint 40 launch round (this commit's Batch 2):
- docs/sprint_objective.md: REPLACED with Sprint 40 contract (~330
  lines; teaching extraction from system_prompt.txt per design doc
  §7 — Sprint 31 + Sprint 33 SLOT description + DISCOVER phase
  guidance bulk migrate to discover_triage.yaml `procedure`; Sprint 23
  `already_called` paragraph + `request_handover` decision tree at
  lines 54-101 PRESERVED in orchestration shell; envelope-mechanics
  paragraph ADDED; behavioural-equivalence integration test). Single
  sub-sprint, content-relocation only (no Java code change beyond
  NEW test file). Strong STOP discipline (18 stop conditions; 33 hard
  fences); LOAD-BEARING design doc §7.2.1 + §7.3 + §7.2.2 Alternative
  C REJECTED enforcement.
- compact/sprint-040-dev-prompt.md: NEW (~220 lines; the dev brief
  for Claude Code; 6-deliverable walkthrough + STOP discipline + hard
  fences + §4.1 self-walk template + bundle policy + handoff §11
  12-section contract + commit message template).
- compact/context-handoff-sprint-040-pre-launch.md: NEW (cross-session
  handoff for next deliver-agent instance).

Sprint 40 design (this batch's product):
- Layer: prompt_projection + semantic_planner per §1.4.
- 5 deliverables D-a/D-b/D-c/D-d/D-e + 1 optional D-f (inherited test
  resolution if structural change addresses underlying drift).
- Java baseline target post-Sprint-40: 1094 (post-Sprint-39) + ~5-10
  new = ~1099-1104; 1 inherited failure may resolve organically per
  D-f.

Carry-forward into Sprint 40 (preserved verbatim where applicable):
M2 §6 #4 verbatim human authorization (Sprint 39 implemented; Sprint
40 not relevant); all hard fences on RuntimeIntentClassifier /
DriftDetector / UseCaseRouter / escalation_reason enum / INTAKE_UCS
/ D-full-issue-ledger; cascade fence on existing case families;
constitution-discipline; Sprint 11 / 11.1 / 12 frozen surface
(ResolveDispositionEvaluator); M1 functional-surface preservation
(IntakeFieldExtractor + IntakeFieldsRegistry + Sprint71PartialIntakePersistenceTest
14/14); Sprint 38 + Sprint 39 surfaces preserved.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

---

End of context handoff. The next deliver-agent reads this + the role brief + verifies state, then proceeds per §5.2 (Sprint 40 dev result review + Codex dispatch) OR §5.3 (Sprint 41 contract drafting) OR §5.4 (M2 close) depending on where the dev session has reached.
