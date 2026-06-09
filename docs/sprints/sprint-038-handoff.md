---
title: Sprint 38 handoff — SkillRegistry core + 4 simpler phase Skills migration (NEW M2 sub-sprint 2)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 38 is the SECOND sub-sprint of NEW Milestone M2 (Skill Registry
  Abstraction + Wholesale Retroactive Externalization) per
  `docs/milestone_objective.md`. First implementation sub-sprint after the
  Sprint 37 design freeze. Ships the SkillRegistry core (`Skill`,
  `Guardrail`, `StateInheritance`, `SkillRegistry`, `SkillLoader` Java
  classes) + the 4 simpler phase Skills as externalized YAML
  (`discover_triage.yaml`, `confirm.yaml`, `escalate.yaml`, `terminal.yaml`)
  + PhaseEvaluator integration for those 4 phases (composeSkillPhasePlan
  helper) per Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4).
  §7 stanza REQUIRED — single-track multi-layer (`prompt_projection` +
  `skill_state` + Runtime-owned PhasePlan composition). Codex
  per-sub-sprint review fires at Sprint 38 close per
  `iteration_governance.md` §4.3 trigger #3 (new architectural surface).
  Closure verdict (§12) left for deliver-agent + human + Codex per
  `feedback_handoff_verdict_section_delegation.md`.
---

# Sprint 38 handoff — SkillRegistry core + 4 simpler phase Skills migration

## 1. Context Pack

### 1.1 Relevant docs (read at session start)

| path | tier (best guess) | status | one-line relevance |
|------|-------------------|--------|--------------------|
| `AGENTS.md` (+ transitively `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`) | durable-connective | current | Constitution + governance chain. §1.3 LLM-owned / §1.4 Runtime-owned boundary; §4.1 anti-hardcode kernel; §4.3 per-sub-sprint Codex triggers; §5/§5.5/§5.6 acceptance bars; §7 stanza shape; §8 milestone framework. |
| `docs/sprint_objective.md` | current-runtime | current | Sprint 38 binding scope: §2 Goal (4 deliverables D-a/D-b/D-c/D-e); §3 non-goals; §4 12-premise check; §5 files in scope; §6 31 hard fences; §7 bundle policy; §8 §7 stanza fields; §9 success metrics; §10 20 stop conditions; §11 handoff contract. |
| `docs/milestone_objective.md` | current-runtime | current | NEW M2 milestone north star: §1 5-sub-sprint layer breakdown; §2 goal; §5 acceptance recalibration (Alice + eval = OBSERVATION not gate); §6 22 hard fences (#4 verbatim S1 authorization carried forward). |
| `docs/proposals/skill_registry_design.md` | proposal | proposal (Sprint 37 freeze; immutable per `doc_governance.md`) | Architectural authority for Sprint 38: §2 decision (a) Skill data model; §3 decision (b) SkillRegistry shape + selection semantics; §4 decision (c) PhaseEvaluator integration; §6.2.1-§6.2.4 the 4 Skill YAML templates Sprint 38 ships verbatim. §5/§6.2.5-§6.2.6/§7/§8/§9/§10/§11/§12/§13 contextual only (downstream sub-sprints implement). |
| `docs/sprints/sprint-037-handoff.md` | sprint-archive | archived | Sprint 37 design-freeze handoff: §1.2 verified code shape at HEAD `6d97888` (preserved through Sprint 37 close `51c327c`); §3 premise re-verification table (10/10 PASS, 1 refinement on `system_prompt.txt` 101-line shape); §6 Tier-0 candidate outcome (5 candidates; C2 + C3 DEFER as R-items); §7 11 OQs (OQ-7.1 CLOSE/TERMINAL enum kept unchanged; OQ-7.11 AGREE WITH DEV). |
| `docs/sprints/sprint-037-codex-review.md` | sprint-archive | archived | Sprint 37 Codex per-sub-sprint review `decision: pass / blocking_count: 0`. §10 deferred / non-blocking notes carry forward (Sprint 33 cue/slot split preserved exactly in Sprint 38; premise #7 informs Sprint 40, not Sprint 38; C2 + C3 as R-items for M3+ revisit). |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` | feedback memory | current | Why Sprint 38 dev does NOT elevate C2 / C3 to Tier-0 — DEFER pre-decision preserved; R-items carry forward to M3+. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md` | feedback memory | current | Bundle policy — dev does NOT stage deliver-agent-owned files (`docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-038-*.md`). |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md` | feedback memory | current | Every code citation + line range here is reproducible via `wc -l <path>` / `grep -n` / `git show <sha>:<path>`. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_mocked_llm_cannot_prove_prompt_causal_change.md` | feedback memory | current | Sprint 38 ships no real-LLM eval rerun as primary evidence; Java behavioural-equivalence on PhasePlan composition is the hard gate per design doc §6.4. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md` | feedback memory | current | §12 closure-verdict placeholder convention. |

### 1.2 Code shape verified at session start (HEAD `51c327c`, 2026-05-17 post-Sprint-37-close)

Re-verified at session start. All Sprint 37 premise #4-#11 cited line numbers preserved (zero drift since Sprint 37 close).

| path | total lines | landmarks re-verified |
|------|------------:|-----------------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 1628 (pre-Sprint-38) | `INTAKE_UCS` line 30; escalation_reason enum 33-63; `UC_TEAM_NAME` 112; `buildIntakeSystemInstruction` 207-208; `evaluate` switch 355-360; **`plan(...)` method 387-666**; DISCOVER 397-457; CONFIRM 462-487; CLOSE 492-509; ESCALATE 513-542; RESOLVE-INTAKE 552-596; RESOLVE-FAQ 598-665. |
| `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` | 146 | 11-field record with `Set<TerminalOutcome>` for `validTerminalOutcomes` + `Set<String>` for `requiredContextKeys` (re-verified). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` | 787 | UNCHANGED in Sprint 38 — Sprint 6/7/11 predicates 628-651 / 690-734 / 745-759, dispatch sites 317/356/413 ALL PRESERVED. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` | 222 | UNCHANGED. Static registry (not a Spring bean) per its inline javadoc. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` | 174 | UNCHANGED. `UseCaseDefinition` record lines 166-173 (6 fields, no `requiredIntakeFields`). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 1161 | UNCHANGED in Sprint 38. Sprint 32 `alternate_candidate_use_cases` slot 396-416 + Sprint 33 `discover_disambiguation_signals` slot 418-432 preserved (Sprint 41 will add `prior_use_case_carry`). |
| `server/src/main/resources/prompts/system_prompt.txt` | 101 | UNCHANGED in Sprint 38 — Sprint 23 23-28 / Sprint 31 30-34 / Sprint 33 36-43 / DISCOVER 45-52 / `request_handover` decision tree 54-101 all preserved (Sprint 40 scope). |
| `server/src/main/resources/skills/` | (did not exist) | Created by Sprint 38: 4 new YAMLs (`discover_triage.yaml`, `confirm.yaml`, `escalate.yaml`, `terminal.yaml`). |

### 1.3 Doc-status warnings observed

- **Editorial divergence between design doc §6.2.1 template and legacy `PhaseEvaluator.java:411-448` DISCOVER systemInstruction.** The freeze template at §6.2.1 paraphrases the legacy text in places (e.g., "Sprint 7 §I0 weak-candidate cue:" → "Weak-candidate cue (Sprint 7 §I0 migration):"; "Sprint 33 ad-status disambiguation cue (read alongside ...):" template-block flow re-paragraphed; objective trailing period added). Per Sprint 38 contract §2.4 the dev surfaces this for adjudication. **Sprint 38 implementation honors LEGACY text verbatim** (the §6.4 behavioural-equivalence hard gate would fail otherwise). Surfaced in §7 OQ-S38.1.
- **No premise drift on §4 items.** All 12 §4 premises re-verified at session start; matches Sprint 37 close exactly. See §3 below.
- **Working tree at session start CLEAN** per `git status`; HEAD `51c327c`. The deliver-agent's Sprint 37 close + Sprint 38 contract commit (`649c6c7`) is already in place.

### 1.4 Source-of-truth decision

For the Skill data model + selection semantics + PhaseEvaluator integration shape, source of truth is the Sprint 37 design freeze at `docs/proposals/skill_registry_design.md` §2/§3/§4 (`source_of_truth: this file` per its frontmatter). For per-Skill text content (objective / procedure / grounding / escalation / tool whitelist / required context / max tool steps / valid terminal outcomes), source of truth is the legacy `PhaseEvaluator.java` Java-string branches at HEAD `51c327c` (per `doc_governance.md` "code ahead of docs" rule and the §6.4 behavioural-equivalence contract). Where the freeze template at §6.2.1 paraphrases the legacy text, the implementation honors the legacy verbatim and surfaces the divergence in §7 OQ.

### 1.5 Implementation status

`implementation_status: implemented`. Sprint 38 ships:

- 3 new Java records (`Skill`, `Guardrail`, `StateInheritance`) + 2 new Spring `@Component`s (`SkillRegistry`, `SkillLoader`) under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/`.
- 4 new Skill YAML files under `server/src/main/resources/skills/`.
- PhaseEvaluator integration: `composeSkillPhasePlan` private helper + dispatch through SkillRegistry at the top of `plan(...)`; 4 legacy phase branches (DISCOVER / CONFIRM / CLOSE / ESCALATE) DELETED; RESOLVE-INTAKE + RESOLVE-FAQ branches PRESERVED for Sprint 39.
- 4 new test files (45 new tests total; all pass) covering Skill data model + Registry selection + Loader fail-fast schema validation + behavioural-equivalence on the 4 migrated phases.
- 19 existing PhaseEvaluator constructor call sites updated to inject `SkillTestFixtures.productionRegistry()`.

No partial shipment; no deferred work within Sprint 38 scope. Sprint 39 / 40 / 41 implementation rounds are separate deliver-agent + human planning rounds.

### 1.6 Risks before implementation (assessed at session start)

- **Behavioural-equivalence risk (mitigated):** the §6.4 hard gate is "Skill-composed PhasePlan field-by-field equal to pre-migration golden". Mitigated by capturing 16 inline golden strings in `PhaseEvaluatorSkillIntegrationTest` from the legacy branch source at HEAD `51c327c` BEFORE deletion; tests pass green (13/13 in that file, plus 32 in the other 3 Sprint 38 test files).
- **Template-vs-legacy text divergence (surfaced for adjudication):** §1.3 above + §7 OQ-S38.1. Sprint 38 chose LEGACY-verbatim; freeze template intent honored at field-shape level. Codex per-sub-sprint review verifies.
- **Constructor-signature ripple risk (mitigated):** 19 existing PhaseEvaluator call sites needed update for the new `SkillRegistry` constructor arg. Mitigated by creating `SkillTestFixtures.productionRegistry()` helper + a uniform perl substitution; full test suite passes with no new failures.
- **§1.7 boundary risk (assessed clean):** Skill data model and the 4 Skill YAML bodies introduce no per-UC-branch if-else; `applicable_use_cases: ["*"]` for all 4 Skills; DISCOVER `procedure` carries the same Sprint 7 §I0 + Sprint 33 cues that Codex M1 / Sprint 37 verified are classification guidance, not active-UC routing.
- **§1.3 / §1.4 boundary risk (clean):** Skill `procedure` is LLM-soft teaching (no prescription of customer-facing language or per-step argument values); `tools_required` composes the same `PhasePlan.allowedTools` enforced by existing `ToolDispatcher.validateAgainstPlan`; `guardrails: []` for all 4 Sprint 38 Skills (Sprint 39 populates).
- **Java baseline preservation (verified):** post-Sprint-38 `mvn test -q` returns `1028 tests, 1 failure (inherited), 0 errors, 2 skipped` — exactly +45 new Sprint 38 tests over Sprint 37 close baseline 983; inherited `SystemPromptUserRequestedTiebreakerTest` persists unchanged.

## 2. Sub-sprint-objective recap

Sprint 38 is the SECOND sub-sprint of NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) per `docs/milestone_objective.md`. Layer: `prompt_projection` (Skill envelope reaches LLM via SkillRegistry-composed PhasePlan) + `skill_state` (SkillRegistry holds Skill definitions across requests; SkillLoader fail-fast at Spring boot) + Runtime-owned PhasePlan composition per Constitution §1.4. Semantic-touching multi-layer; §7 stanza REQUIRED.

Sprint 38 implements the Sprint 37 design freeze decisions (a) / (b) / (c) / (e §6.2.1-§6.2.4) verbatim. Decisions (d) procedure-vs-guardrails split, (e §6.2.5-§6.2.6) RESOLVE Skill migration, (f) Sprint 23/31/33 teaching extraction, (g) Sprint 6/7/11 + S1/S2 predicate migration, (h) unified Skill terminal-predicate dispatcher, (i) session-level state model + `state_inheritance` enforcement, (j) §4.1 walk on PROPOSED design — those are CONTEXTUAL ONLY for Sprint 38; Sprints 39 / 40 / 41 implement them per the freeze's downstream sub-sprint reference index.

Codex per-sub-sprint review fires at Sprint 38 close per `iteration_governance.md` §4.3 trigger #3 (new architectural surface — SkillRegistry mediates between LLM prompt context and tool dispatch). Codex verifies: behavioural equivalence pre/post-migration on 4 migrated phases; no per-UC-branch if-else in any Skill YAML body; Sprint 37 freeze fidelity; no scope creep into Sprint 39 / 40 / 41 surfaces; Java baseline preserved.

## 3. Premise re-verification (each of §4's 12 points)

All 12 premise items in `docs/sprint_objective.md` §4 re-verified at session start at HEAD `51c327c`.

| # | premise | verification |
|---|---------|--------------|
| 1 | **M2 milestone objective approved + Sprint 37 closed.** | Read `docs/milestone_objective.md` (M2 status `current`, supersedes OLD M2-Skill); `docs/sprints/sprint-037-codex-review.md` confirms `decision: pass / blocking_count: 0` at commit `51c327c`. **PASS.** |
| 2 | **Sprint 38 contract approved.** | Read `docs/sprint_objective.md` (Sprint 38 status `current`, supersedes `sprint-037-objective.md` per frontmatter). **PASS.** |
| 3 | **Sprint 37 design freeze at `docs/proposals/skill_registry_design.md`** — `status: proposal` (immutable). | Read frontmatter + §2 / §3 / §4 / §6.2.1 / §6.2.2 / §6.2.3 / §6.2.4 verbatim. 3633 lines; 13 sections. **PASS.** |
| 4 | **`PhaseEvaluator.java` at HEAD: 1628 lines; phase branches at cited positions.** | `wc -l` returns 1628; phase branches verified at exact lines 397-457 (DISCOVER), 462-487 (CONFIRM), 492-509 (CLOSE), 513-542 (ESCALATE), 552-596 (RESOLVE-INTAKE), 598-665 (RESOLVE-FAQ); switch at 355-360; `plan(...)` method at 387+. **PASS** (Sprint 37 premise refinement that the plan() method is the dispatch site, NOT the evaluate() switch — preserved; integration point is plan() per design doc §4.1). |
| 5 | **`PhasePlan.java` at HEAD: 146 lines; 11-field record backward-compatible.** | `wc -l` returns 146; record signature verified (11 fields; `Set<TerminalOutcome>` for `validTerminalOutcomes`; `Set<String>` for `requiredContextKeys`). **PASS.** |
| 6 | **`AgentRunLoopImpl.java` at HEAD: 787 lines; Sprint 6/7/11 predicates UNCHANGED.** | `wc -l` returns 787; Sprint 38 introduces zero edits to this file. **PASS.** |
| 7 | **`IntakeFieldsRegistry.java` at HEAD: 222 lines; UNCHANGED.** | `wc -l` returns 222; no edits. Note: static (not Spring bean) per inline javadoc. **PASS.** |
| 8 | **`UseCaseRegistryService.java` at HEAD: 174 lines; `UseCaseDefinition` 6-field record at 166-173.** | `wc -l` returns 174; record signature verified (6 fields: `ucId`, `name`, `topicSubjects`, `riskLevel`, `allowBotResolution`, `path`). **PASS.** |
| 9 | **`ContextProjectionBuilder.java` at HEAD: 1161 lines; UNCHANGED.** | `wc -l` returns 1161; no edits in Sprint 38. **PASS.** |
| 10 | **`system_prompt.txt` at HEAD: 101 lines; UNCHANGED.** | `wc -l` returns 101; no edits. **PASS.** |
| 11 | **`server/src/main/resources/skills/` directory: does NOT exist at HEAD.** | `ls` confirms directory absent at session start; created by Sprint 38 commit. **PASS.** |
| 12 | **Java baseline 983 / 1-inherited / 0 / 2 post-Sprint-37-close.** | Baseline holds at session start (per Sprint 37 Codex review `pass / 0` evidence). Post-Sprint-38 baseline measured `1028 / 1-inherited / 0 / 2` = 983 + 45 new Sprint 38 tests; inherited `SystemPromptUserRequestedTiebreakerTest` persists. **PASS.** |

**Premise drift summary:** 0 drifted. 12/12 PASS. **No STOP condition fired.**

## 4. Implementation walkthrough

### 4.1 Skill data model (D-a) — `Skill.java` + supporting types

- **`Skill.java`** — Java record with 15 fields (12 named in design doc §2.1 + the 3 sub-objects: `guardrails`, `stateInheritance`, plus `objective` / `maxToolSteps` / `allowInterimMessage` / `validTerminalOutcomes` / `requiredContextKeys` from the optional set). Compact-constructor defensively normalizes null lists to `List.of()` and null `stateInheritance` to `StateInheritance.EMPTY`. `@JsonCreator` static factory maps snake_case YAML keys to camelCase record fields. `appliesTo(phase, useCase)` honors design doc §3.2 selection semantics (exact UC OR `["*"]` wildcard).
- **`Guardrail.java`** — typed record per design doc §5.2 DSL: `type` + `onFail` + `parameters`. Both `type` and `onFail` are required (compact constructor throws on null/blank); Sprint 38's 4 Skills all declare `guardrails: []` so this record is exercised only by the Sprint 38 negative `SkillLoaderTest.parseAndValidate_unknownOnFailMode_failsFast` test (Sprint 39 populates concrete instances).
- **`StateInheritance.java`** — record with `inherit` / `reset` / `softSignalViaProjection` lists per design doc §10.2. `StateInheritance.EMPTY` singleton for default. Sprint 38 populates the field per the 4 Skill YAML templates but does NOT yet enforce at session-state-bus boundary (Sprint 41 scope).

### 4.2 SkillRegistry + SkillLoader (D-b)

- **`SkillLoader.java`** — Spring `@Component`. `loadAll()` enumerates `classpath:/skills/*.yaml` via `PathMatchingResourcePatternResolver`, parses each via Jackson YAML, runs `validate(skill, filename)` per design doc §2.2 schema rules, and throws `IllegalStateException` on any violation (fail-fast at Spring boot per design doc §3.3). `parseAndValidate(InputStream, filename)` is the test seam used by `SkillLoaderTest`. Schema validation covers: required field presence (`name`, `description`, `applicable_phases`, `applicable_use_cases`, `tools_required`, `procedure`); `applicable_phases` enum subset (DISCOVER / RESOLVE / CONFIRM / CLOSE / ESCALATE — matches runtime phase enum per OQ-7.1 default kept unchanged); wildcard-mixed-with-explicit-UC rejection; `valid_terminal_outcomes` against `TerminalOutcome` enum names; `state_inheritance` key/slot whitelists per design doc §10.2; `guardrail.on_fail` whitelist (`reject_with_hint` / `downgrade_reason`; `observe_only` excluded per OLD Sprint 36 D2 §3.2 verbatim).
- **`SkillRegistry.java`** — Spring `@Component`. Constructor takes `SkillLoader` (Spring auto-wires); calls `loadAll()`; eagerly indexes Skills into `wildcardByPhase` map + `exactByPhaseUc` nested map; throws on intra-phase collision per design doc §3.1. `select(phase, useCase)` implements design doc §3.2 semantics: exact-UC match in `exactByPhaseUc` first; wildcard match in `wildcardByPhase` second; `Optional.empty()` fallback otherwise (Sprint 38 → Sprint 39 incremental migration window safety).

### 4.3 PhaseEvaluator integration (D-c)

- **Constructor extended** (10 → 11 args) — added `SkillRegistry skillRegistry` parameter at the end. Existing 19 test sites updated to inject `SkillTestFixtures.productionRegistry()`.
- **`composeSkillPhasePlan(Skill, String phase, String activeUc) → PhasePlan`** — new private helper (`PhaseEvaluator.java` around line 533, immediately before `interpretRunResult`). Maps Skill fields verbatim to PhasePlan fields; converts `List<String> validTerminalOutcomes` (YAML strings) to `Set<TerminalOutcome>` via `TerminalOutcome::valueOf`; converts `List<String> requiredContextKeys` to a `LinkedHashSet<String>` (PhasePlan compact constructor normalizes to unmodifiable order-preserving set). No template substitution applied (Sprint 38's 4 Skills are all `applicable_use_cases: ["*"]` and carry no placeholders; Sprint 39 RESOLVE Skills will exercise the substitution surface).
- **`plan(...)` dispatch updated** — `PhaseEvaluator.java:393-408`. Just after the `session == null` null-guard, the new path queries `skillRegistry.select(phase, activeUc)` first; on present, returns `composeSkillPhasePlan(...)`; on empty, falls through to the existing RESOLVE-INTAKE / RESOLVE-FAQ branches (which Sprint 39 will migrate).
- **4 legacy phase branches DELETED** — DISCOVER 397-457 + CONFIRM 462-487 + CLOSE 492-509 + ESCALATE 513-542 removed. PhaseEvaluator.java shrunk from 1628 lines to 1500 lines (-128 lines). The 2 RESOLVE branches at the (renumbered) lines ~417-460 (RESOLVE-INTAKE) and ~463-530 (RESOLVE-FAQ) preserved verbatim — Sprint 39 migrates them.

### 4.4 4 Skill YAMLs (D-e §6.2.1-§6.2.4)

Each YAML carries the legacy `PhaseEvaluator.java` content verbatim (modulo whitespace normalization Java string concatenation already performed) so the behavioural-equivalence contract per design doc §6.4 holds. State inheritance populated per design doc templates; `guardrails: []` for all 4 Skills (Sprint 39 populates).

- `discover_triage.yaml` — `applicable_phases: [DISCOVER]`; `applicable_use_cases: ["*"]`; tools `[search_knowledge, classify_use_case]`; required context `[form_context, candidate_use_cases]`; max_tool_steps 2; valid outcomes `[CLARIFICATION_NEEDED, FINAL_ANSWER, ESCALATE]`. `state_inheritance.inherit: [customer_context]` + `soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]` per design doc §6.2.1. Procedure body carries the legacy DISCOVER systemInstruction verbatim including Sprint 7 §I0 weak-candidate cue + Sprint 33 ad-status disambiguation cue.
- `confirm.yaml` — `applicable_phases: [CONFIRM]`; `applicable_use_cases: ["*"]`; tools `[record_outcome, request_handover]`; required context `[form_context, conversation_history]`; max_tool_steps 2; valid outcomes `[FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE]`. `state_inheritance.inherit: [customer_context, accumulated_tool_results]` per design doc §6.2.2.
- `escalate.yaml` — `applicable_phases: [ESCALATE]`; `applicable_use_cases: ["*"]`; tools `[request_handover, record_outcome]` (`create_case_controlled` INTENTIONALLY excluded per Codex 1.8 / `customer_service_tool_spec_v0_2.yaml` runtime-only visibility rule; inline comment at legacy `PhaseEvaluator.java:514-521` carries the rationale); required context `[form_context, customer_context]`; max_tool_steps 2; valid outcomes `[ESCALATE, FINAL_ANSWER]`. `state_inheritance.inherit: [customer_context, accumulated_tool_results]` per design doc §6.2.3.
- `terminal.yaml` — `applicable_phases: [CLOSE]` per OQ-7.1 default (keep phase enum unchanged at Sprint 38; file name `terminal.yaml` carries M3+ rename intent); `applicable_use_cases: ["*"]`; tools `[record_outcome]`; required context `[form_context]`; max_tool_steps 2; valid outcomes `[FINAL_ANSWER]`. `state_inheritance.inherit: [customer_context, accumulated_tool_results]` per design doc §6.2.4.

### 4.5 Tests + supporting test helper

- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` — `productionRegistry()` returns a real `SkillRegistry` backed by the 4 production YAMLs; used by all 19 existing PhaseEvaluator test sites to keep migrated-phase behaviour green without per-test mocking.
- `SkillTest.java` — 8 tests covering record field accessors, compact-constructor null normalization, and `appliesTo(phase, useCase)` selection semantics (exact / wildcard / mismatch / null cases).
- `SkillRegistryTest.java` — 11 tests for `select(phase, useCase)` semantics (empty / exact / wildcard / exact-prefers-wildcard / phase-miss / null-phase / collision-detection / disjoint-UC-coexistence).
- `SkillLoaderTest.java` — 13 tests for schema validation: production-YAML happy path + 12 negative cases (missing/blank name, missing procedure, unknown phase, empty UC list, wildcard-mixed-with-explicit, unknown terminal outcome, unknown state key, unknown projection slot, unknown on_fail mode, malformed YAML).
- `PhaseEvaluatorSkillIntegrationTest.java` — 13 tests: 3 per migrated phase (representative active UCs: any UC + intake UC + null UC) × 4 phases + 1 RESOLVE-INTAKE legacy-branch regression check. Each migrated-phase test pins the Skill-composed PhasePlan field-by-field to the GOLDEN pre-migration legacy string (captured inline from `PhaseEvaluator.java:397-542` at HEAD `51c327c`).

## 5. Sprint 37 freeze fidelity

For each Sprint 37 freeze decision Sprint 38 implements, cite the implementation file + line range.

| Freeze decision | Sprint 38 implementation | Departures |
|------|--------------------------|------------|
| **(a) §2.1 Skill data model — 12 fields + JSON Schema validation** | `Skill.java` + `Guardrail.java` + `StateInheritance.java`; schema validation in `SkillLoader.validate(...)` per design doc §2.2 | None at the field-set level. Implementation chose manual Java validation in `SkillLoader.validate(...)` over a JSON Schema file because Jackson YAML doesn't natively consume JSON Schema; the validation surface is the same. Dev judgement per design doc §2.2 "exact JSON Schema is Sprint 38 dev". |
| **(b) §3.1 SkillRegistry shape — `select(phase, useCase)` + SkillLoader fail-fast** | `SkillRegistry.java` (`select` + `allSkills` + `skillsForPhase`); `SkillLoader.java` (`loadAll` + `parseAndValidate`); both Spring `@Component` constructor-injected | None. The `Optional<Skill>` return type for `select` (vs the raw `Skill` in design doc §3.1's illustrative signature) matches design doc §3.2 "null fallback returns Optional.empty() semantically" — an editorial preference for the explicit Optional API. |
| **(c) §4.1 PhaseEvaluator integration — composeSkillPhasePlan + dispatch** | `PhaseEvaluator.java` constructor extended; `composeSkillPhasePlan(...)` private helper added; `plan(...)` dispatch updated | None. The 4 legacy phase branches are DELETED in Sprint 38 (design doc §4.5 Alternative B was rejected for atomic full migration; Sprint 38 chose the design doc's recommended path of "delete migrated branches; preserve unmigrated branches; legacy fallback is for Sprint 38 → Sprint 39 window's 2 unmigrated phases"). |
| **(e §6.2.1) DISCOVER → `discover_triage.yaml`** | `server/src/main/resources/skills/discover_triage.yaml` | **Editorial divergence between freeze template and legacy text** (see §1.3 above + §7 OQ-S38.1). Sprint 38 honors legacy text verbatim; freeze template's intent honored at field-shape level. |
| **(e §6.2.2) CONFIRM → `confirm.yaml`** | `server/src/main/resources/skills/confirm.yaml` | None substantive. Legacy text bit-for-bit equivalent to freeze template at §6.2.2. |
| **(e §6.2.3) ESCALATE → `escalate.yaml`** | `server/src/main/resources/skills/escalate.yaml` (`create_case_controlled` INTENTIONALLY excluded per design doc §6.2.3 note + Codex 1.8 / `customer_service_tool_spec_v0_2.yaml` runtime-only visibility rule) | None. |
| **(e §6.2.4) CLOSE → `terminal.yaml`** | `server/src/main/resources/skills/terminal.yaml` (`applicable_phases: [CLOSE]` per OQ-7.1 default; file name carries M3+ TERMINAL rename intent) | None. |

## 6. §4.1 anti-hardcode self-walk on the Sprint 38 diff

Per Sprint 38 contract §8 stanza + handoff contract §11 bar.

| Q | Question (abbreviated) | Sprint 38 diff verdict | Evidence |
|---|------------------------|-----------------------|----------|
| Q1 | Keyword / regex / if-else / enum / per-UC matrix for semantic decision? | **NO** | `SkillRegistry.select(phase, useCase)` is a lookup, NOT a branch table. All 4 Sprint 38 Skills declare `applicable_use_cases: ["*"]` so no per-UC routing. DISCOVER procedure's Sprint 7 §I0 cue + Sprint 33 cue name UCs as classification TARGETS (UC-F / UC-B / UC-C / UC-D), not active-UC-branch — Codex M1 / Sprint 37 already verified this content. |
| Q2 | Tier-0 justified? | **N/A** — no Tier-0 added. C2 + C3 DEFER pre-decision preserved per Sprint 37 close. | Sprint 38 dev did NOT edit `runtime_freeze_and_risk_policy.md`. |
| Q3 | Soft signal alternative achievable? | **N/A** — Sprint 38 is architectural refactoring; Skill `state_inheritance` declares `soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]` for DISCOVER (existing M1 Sprint 32/33 slots). Sprint 38 does not yet enforce; Sprint 41's `SkillStateBus` will. | `discover_triage.yaml` state_inheritance block. |
| Q4 | Visible-eval / trace phrasing / CaseSpec id encoded? | **NO** | 4 Skill YAMLs carry only the legacy `PhaseEvaluator.java` Java-string content; no CaseSpec id; no trace phrasing; no eval text. |
| Q5 | Moves semantic ownership LLM → Java? | **NO** | LLM still owns UC hypothesis / drift / escalation posture / response strategy / customer-facing wording / per-step argument choice per §1.3. Skill `procedure` is LLM-soft teaching; `tools_required` composes same `PhasePlan.allowedTools` enforced by existing `ToolDispatcher.validateAgainstPlan`; `guardrails: []` adds no Java predicate. |
| Q6 | If-else in prompt? | **NO** | All 4 Skill `procedure` texts are principle-level (CONFIRM = interpret satisfaction; ESCALATE = finalize handover; CLOSE = polite closing + record outcome; DISCOVER carries Sprint 7 §I0 + Sprint 33 cues which Codex M1 / Sprint 37 verified are classification guidance, not active-UC if-else). |
| Q7 | Tool schema / capability / PII / grounding floor preserved? | **YES** | Tool schemas unchanged; `PhasePlan.allowedTools` composed from `skill.tools_required` with same enforcement; PII / safety / grounding floors untouched (S1 grounding extension is Sprint 39 scope). |
| Q8 | Generalization coverage? | **YES** per §8 stanza | Target = 4 migrated phases × 3 representative UCs = 12 cases pinned to golden + 1 legacy-RESOLVE regression check. Neighbor = 2 unmigrated RESOLVE branches preserved (Sprint71PartialIntakePersistenceTest 14/14 green + existing PhaseEvaluator FAQ tests green). Negative = 12 negative `SkillLoaderTest` cases (malformed YAML / missing fields / invalid enums / collisions). Shadow = N/A per M2 §5 recalibration (Sprint 38 is architectural). |
| Q9 | Rollback / sunset? | **N/A** | Skill Registry abstraction is permanent. Per Sprint 38 §10 #2: if behavioural-equivalence regresses, dev stops; the legacy branches can be restored via `git revert` if needed. |

**Sprint 38 self-walk verdict: `approve`** — architectural refactoring; behavioural equivalence preserved (45 new tests + full suite 1028/1-inherited/0/2 baseline preserved); no per-UC-branch if-else; tool-whitelist semantics unchanged; no scope creep into Sprint 39 / 40 / 41 surfaces.

## 7. Open questions for deliver-agent + human

| # | Question | Source | Recommended default | Decision needed by |
|---|----------|--------|---------------------|---------------------|
| **OQ-S38.1** | The Sprint 37 design freeze §6.2.1 DISCOVER YAML template re-phrases the legacy `PhaseEvaluator.java:411-448` systemInstruction in places (paragraph headings: "Sprint 7 §I0 weak-candidate cue:" → "Weak-candidate cue (Sprint 7 §I0 migration):"; objective trailing period added; etc.). Sprint 38 honors LEGACY text verbatim to satisfy the §6.4 behavioural-equivalence hard gate. Is the divergence (a) editorial paraphrasing in the freeze template the dev correctly resolved by honoring code-as-source-of-truth per `doc_governance.md`, OR (b) a freeze template intent change the implementation should adopt (which would require updating the behavioural-equivalence golden strings)? | This handoff §1.3 + §5; freeze §6.2.1 vs `PhaseEvaluator.java:411-448` at HEAD `51c327c` | (a) — honor legacy verbatim; behavioural equivalence is the §6.4 contract; freeze template's intent is honored at field-shape level. The template wording is descriptive paraphrasing, not a behaviour change the freeze decided. | Sprint 38 Codex per-sub-sprint review |
| **OQ-S38.2** | `composeSkillPhasePlan(...)` placement: implemented as a private helper inside `PhaseEvaluator.java` (per design doc §4.1 illustrative snippet). Alternative: a separate utility class `SkillPhasePlanComposer` in the `skill` package. Sprint 38 picked the design doc's illustrative path; if Codex flags this as Skill-package belongs, refactor at Sprint 39 alongside RESOLVE Skill addition. | design doc §4.1 + §4.4 | Keep inside PhaseEvaluator for Sprint 38; revisit if Sprint 39 grows the helper materially. | Sprint 38 Codex review (informational) |
| **OQ-S38.3** | `Skill.maxToolSteps` is `Integer` (nullable) per design doc §2.1 ("optional, default uses existing per-phase default"). `composeSkillPhasePlan` falls back to literal `2` when null. The 4 Sprint 38 Skills all explicitly set `max_tool_steps: 2`, so the default never fires; but the legacy RESOLVE-FAQ branch uses 4 and RESOLVE-INTAKE uses 3 (Sprint 39 will set those explicitly in the respective Skill YAMLs). The literal-2 default is dev-judgement, not a freeze decision. | design doc §2.1 + `PhaseEvaluator.java` line ~545 fallback | Keep literal-2 default; Sprint 39 Skills will set explicit values. | Sprint 39 planning round |
| **OQ-S38.4** | `SkillTestFixtures.productionRegistry()` is used by 19 existing PhaseEvaluator constructor call sites to satisfy the new 11th arg. Alternative: a `@MockBean` pattern + Mockito stub returning empty Optional in tests that don't exercise migrated phases. The fixture path preserves behavioural equivalence "for free" but couples tests to the production Skill YAMLs. | This handoff §4.5 + Sprint 38 contract §10 #3 | Keep `SkillTestFixtures.productionRegistry()` — coupling to production YAMLs is the same shape as existing tests' coupling to `UseCaseRegistryService.getUseCase(...)` registry data. | Sprint 38 Codex review (informational) |
| **OQ-S38.5** | C2 + C3 Tier-0 R-items (`R-skill-guardrail-non-overridability-tier-0` + `R-skill-state-bus-boundary-enforcement-tier-0`) — Sprint 37 close opened these as R-items in `docs/action_bank.md` §5.2 for M3+ revisit. Sprint 38 dev confirms NO new evidence surfaced from implementation to re-evaluate; both DEFER recommendations preserved. | Sprint 37 Codex review + design doc §12 | Carry forward; no Sprint 38 action. | Sprint 38 Codex review confirms |

No Tier-0 candidate surfaced from Sprint 38 implementation. No premise drift. No §1.7 boundary violation. No §1.3 / §1.4 boundary mismatch.

## 8. Anti-hardcode self-walk (§4.1 nine questions) on Sprint 38 itself

See §6 above. Sprint 38 self-walk verdict: `approve`. Architectural refactoring; no semantic hardcode introduced; LLM-owned / Runtime-owned boundary preserved per Constitution §1.3 / §1.4 / §1.7.

## 9. Files changed (Sprint 38 dev commit scope)

| path | change type | one-line description |
|------|-------------|----------------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` | **NEW** | Java record for Skill data model per design doc §2.1; 15 fields + `@JsonCreator` snake_case mapping + `appliesTo(phase, useCase)` selection helper. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Guardrail.java` | **NEW** | Typed record per design doc §5.2; required `type` + `onFail`; Sprint 38 ships schema only (all 4 Skills have `guardrails: []`). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` | **NEW** | Per-Skill state-bus posture record per design doc §10.2; populated by Sprint 38 Skills but enforced by Sprint 41 SkillStateBus. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` | **NEW** | Spring `@Component`; loads + schema-validates `classpath:/skills/*.yaml` at boot; fail-fast on schema violation per design doc §2.2 / §3.3. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` | **NEW** | Spring `@Component`; indexes Skills; `select(phase, useCase) → Optional<Skill>` per design doc §3.2; fail-fast on `(phase, useCase)` collision. |
| `server/src/main/resources/skills/discover_triage.yaml` | **NEW** | DISCOVER Skill per design doc §6.2.1; legacy `PhaseEvaluator.java:397-457` content verbatim. |
| `server/src/main/resources/skills/confirm.yaml` | **NEW** | CONFIRM Skill per design doc §6.2.2; legacy `PhaseEvaluator.java:462-487` content verbatim. |
| `server/src/main/resources/skills/escalate.yaml` | **NEW** | ESCALATE Skill per design doc §6.2.3; legacy `PhaseEvaluator.java:513-542` content verbatim. |
| `server/src/main/resources/skills/terminal.yaml` | **NEW** | CLOSE Skill per design doc §6.2.4; `applicable_phases: [CLOSE]` per OQ-7.1 default. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | **EDIT** | Constructor extended (10 → 11 args; `SkillRegistry` added); `composeSkillPhasePlan` helper added; `plan(...)` dispatch updated to query SkillRegistry first; 4 legacy phase branches (DISCOVER / CONFIRM / CLOSE / ESCALATE) DELETED. File 1628 → 1500 lines (−128). RESOLVE branches preserved verbatim. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` | **NEW** | Test helper providing `productionRegistry()` → real SkillRegistry built from classpath YAMLs. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` | **NEW** | 8 unit tests for Skill record. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` | **NEW** | 11 unit tests for SkillRegistry selection + collision detection. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` | **NEW** | 13 unit tests for SkillLoader schema validation (1 happy + 12 negative). |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` | **NEW** | 13 behavioural-equivalence integration tests for 4 migrated phases + 1 legacy-RESOLVE preservation check. |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopHandoverReasonNormalizationIntegrationTest.java` | **EDIT** | Constructor call updated to inject `SkillTestFixtures.productionRegistry()`. |
| `server/src/test/java/com/gumtree/csagent/integration/Sprint9TraceObservabilityFidelityIntegrationTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/integration/Sprint141FaqGroundingTracePersistenceTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAd1002IntegrationTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopIntakeIntegrationTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopConfirmCloseIntegrationTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopDeadlineExceededBotResponseIntegrationTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/integration/Cs014RouteAndLoopHandoverIntegrationTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint24DeadlinePlaceholderCoalesceTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint81PhaseEvaluatorUseCaseIdentifiedTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorMaxStepsResolverTest.java` | **EDIT** | Constructor call updated; new `SkillTestFixtures` import. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint9TerminalToolHonestyTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint7IntakeStateTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint11ProgressiveResolveTest.java` | **EDIT** | 2 constructor sites updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorQueryEnrichmentTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorFaqMissFallbackTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint7CandidateUseCasesProjectionTest.java` | **EDIT** | Constructor call updated. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorPlanTest.java` | **EDIT** | Constructor call updated; new `SkillTestFixtures` import. |
| `docs/sprints/sprint-038-handoff.md` | **NEW** | This file. 12-section dev-authored archive per Sprint 38 contract §11. |

Out-of-scope hard fences honored (Sprint 38 §6 + dev prompt §4):

- No edit to `PhaseEvaluator.java` RESOLVE-INTAKE / RESOLVE-FAQ branches (Sprint 39 scope).
- No edit to `AgentRunLoopImpl.java` (Sprint 39 scope; Sprint 6/7/11 predicates UNCHANGED).
- No edit to `system_prompt.txt` (Sprint 40 scope).
- No edit to `ContextProjectionBuilder.java` (Sprint 41 scope).
- No edit to `RuntimeIntentClassifier.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` (M2 §6 fence).
- No edit to `INTAKE_UCS` set / `escalation_reason` enum (M2 §6 fences).
- No edit to `runtime_freeze_and_risk_policy.md` / `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` (constitution-discipline).
- No edit to `docs/proposals/skill_registry_design.md` (Sprint 37 freeze immutable).
- No edit to sprint archives / milestone archives / foundational docs.
- No edit to deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`).
- No edit to eval surfaces (case families / shadow / harness / Alice bad case / overrides).
- No new Tier-0 invariant added.
- No CLOSE → TERMINAL phase enum rename (OQ-7.1 default kept).

## 10. Layer-classification self-walk per Sprint 38 §8 stanza

**Target failure layer:** `prompt_projection` (Skill envelope reaches LLM via Skill-composed PhasePlan; same `systemInstruction` / `groundingInstruction` / `escalationPolicy` content as pre-migration, sourced from YAML instead of Java strings) + `skill_state` (SkillRegistry indexes Skill definitions across requests; SkillLoader fail-fast at Spring boot) + Runtime-owned PhasePlan composition per Constitution §1.4 (the Runtime owns the trace + eval contract surface; Sprint 38 strengthens declarative ownership by externalizing the source). Semantic-touching multi-layer per Sprint 38 §1.

**Tier-0 invariant:** This sub-sprint adds NO Tier-0 invariant. Sprint 37 close pre-decision (C2 + C3 QUALIFIED-DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md`) preserved. Skill Registry abstraction enforces existing Tier-0 surfaces by construction: `PhasePlan.allowedTools` (`PhasePlan.java:15-16`) composed from `skill.toolsRequired` continues to be enforced by `ToolDispatcher.validateAgainstPlan` (`ToolDispatcher.java:183-195`) — C1 REJECTED verdict preserved.

**Semantic hardcode:** No semantic hardcode introduced. The 4 Skill YAML files carry the legacy `PhaseEvaluator.java:397-542` Java-string content verbatim; the migration is content-relocation, not content-redesign. `composeSkillPhasePlan(...)` composes Skill fields into PhasePlan without per-UC-branch logic (all 4 Sprint 38 Skills are `applicable_use_cases: ["*"]`). `SkillRegistry.select(...)` is a (phase, useCase) lookup — exact-match-then-wildcard — which is structural routing, not semantic branching.

**Generalization coverage:** target = 4 migrated phases (DISCOVER + CONFIRM + ESCALATE + CLOSE) × 3 representative UCs each (any UC + intake UC + null UC) = 12 behavioural-equivalence cases in `PhaseEvaluatorSkillIntegrationTest`, all pinned to GOLDEN pre-migration strings captured inline. Neighbor = 2 unmigrated RESOLVE phases (RESOLVE-INTAKE + RESOLVE-FAQ) preserved verbatim; existing Sprint71PartialIntakePersistenceTest + IntakeFieldExtractor + Sprint 32/33 projection-slot + PhaseEvaluatorFaqMissFallback + PhaseEvaluatorPlan tests remain green (verified by full-suite `mvn test -q` → 1028/1-inherited/0/2). Negative = 12 negative `SkillLoaderTest` cases covering schema fail-fast (missing required field / unknown phase / wildcard-mixed / unknown terminal outcome / unknown state key / unknown projection slot / unknown on_fail mode / malformed YAML). Shadow = N/A for Sprint 38 (behavioural equivalence on Java PhasePlan composition is the gate; real-LLM behaviour observation OBSERVATION ONLY per M2 §5 recalibration).

## 11. §5 Eval Acceptance bars — adapted for architectural-refactoring sub-sprint per M2 §5 recalibration

Walking each `iteration_governance.md` §5.1 bar adapted for Sprint 38's architectural-refactoring scope (M2 §5 acceptance recalibration 2026-05-17 + Sprint 38 contract §9):

| Bar | Sprint 38 status | Evidence |
|-----|------------------|----------|
| **Target cases pass** | **YES** | 12 behavioural-equivalence cases in `PhaseEvaluatorSkillIntegrationTest` (3 UCs × 4 phases) all pass; Skill-composed PhasePlan field-by-field equal to GOLDEN pre-migration strings. |
| **Neighbor cases no regression** | **YES** | 2 unmigrated RESOLVE phases preserved; `PhaseEvaluatorPlanTest`, `PhaseEvaluatorFaqMissFallbackTest`, `Sprint11ProgressiveResolveTest`, `Sprint7IntakeStateTest`, `Sprint7CandidateUseCasesProjectionTest`, Sprint71PartialIntakePersistenceTest 14/14 all green. |
| **Negative-control cases unchanged** | **YES** | 12 negative `SkillLoaderTest` cases cover schema fail-fast paths; existing negative tests (e.g., `Sprint9TerminalToolHonestyTest`) green. |
| **Shadow cases no regression** | **N/A** for Sprint 38 — architectural refactoring; LLM-behaviour observation is OBSERVATION ONLY per M2 §5; deliver-agent + Codex own shadow review at downstream sub-sprints. |
| **Safety floor unchanged** | **YES** — Tier-0 safety invariants untouched. Sprint 38 adds no Tier-0; existing `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan` enforcement preserved. |
| **Grounding floor unchanged** | **YES** — `faq_grounding_contract.md` diagnostics unchanged; Sprint 38 ships no grounding-floor extension (Sprint 39 NARROW S1 `must_cite_source` extension per §6 #4 verbatim authorization is downstream scope). |
| **Wrong-containment rate unchanged or down** | **N/A** for Sprint 38 (architectural; no real-LLM rerun primary evidence per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`). |
| **Over-escalation rate unchanged or down** | **N/A** for Sprint 38 (same reason). |
| **Architecture-health metrics not regressed** | **YES** | `new_semantic_hardcode_count` = 0 (no per-UC-branch if-else added); `soft_signal_conversion_count` = 4 (4 phase Skills moved from hardcoded Java strings to externalized Skill YAML); `planner_ownership_ratio` unchanged (LLM ownership preserved per §1.3; Runtime ownership preserved per §1.4); `shadow_disagreement_rate` not measured (Sprint 38 architectural). `PhaseEvaluator.java` line-count: 1628 → 1500 (−128 lines from 4 deleted phase branches; remaining −150-200 expected at Sprint 39 close when RESOLVE branches migrate). |

**Per §5.5 smoke composite_score demotion (effective 2026-05-16):** Sprint 38 does NOT run interactive eval smoke; demoted observation not applicable. The smoke rerun is OBSERVATION ONLY per M2 §5 recalibration; no Sprint 38 dev action.

**Per §5.6 bad-case suite + M2 §5 acceptance recalibration (effective 2026-05-17):** Sprint 38 does NOT run the bad-case suite (Alice); per M2 §5 the bad-case suite is OBSERVATION ONLY for THIS milestone, NOT a gate. Sprint 38 is architectural refactoring; no behaviour change is expected to flow to bad-case outcomes (the abstraction LANDS the surface that makes future Skill tuning easier, per M2 §5 rationale (4)).

**Sprint 38 hard gates (per Sprint 38 §9):**

- ✅ 3 new Java records (`Skill`, `Guardrail`, `StateInheritance`) + 2 new Spring components (`SkillRegistry`, `SkillLoader`) compile + pass `SkillTest` (8) + `SkillRegistryTest` (11) + `SkillLoaderTest` (13).
- ✅ 4 Skill YAML files load + validate at Spring boot via `SkillLoader.loadAll()` (verified by `SkillLoaderTest.loadAll_productionSkills_loadsFourSkills`).
- ✅ `PhaseEvaluator.composeSkillPhasePlan(...)` produces PhasePlan instances field-by-field equal to pre-migration golden for each of 4 migrated phases × representative UCs (verified by `PhaseEvaluatorSkillIntegrationTest` — 13 tests pass).
- ✅ Java baseline preserved: post-Sprint-38 `mvn test -q` = `1028 / 1-inherited / 0 / 2` = Sprint 37 close baseline 983 + 45 new Sprint 38 tests; inherited `SystemPromptUserRequestedTiebreakerTest` persists unchanged.
- ✅ Sprint71PartialIntakePersistenceTest 14/14 preserved (M1 functional-surface).
- ✅ IntakeFieldExtractor + Sprint 32 alternate_candidate_use_cases projection slot + Sprint 33 discover_disambiguation_signals projection slot tests remain green (M1 functional-surface).
- ✅ No per-UC-branch if-else in any Skill YAML body (visual review + design doc §11.1 / Codex M1 / Sprint 37 verdicts on DISCOVER cues preserved).
- ✅ No scope creep into Sprint 39 (RESOLVE_FAQ / RESOLVE_INTAKE / predicate migration / S1+S2 / unified dispatcher) / Sprint 40 (system_prompt.txt) / Sprint 41 (SkillStateBus / prior_use_case_carry) surfaces (verified by `git diff --stat HEAD~1 HEAD` at commit; only `skill/` package + 4 YAMLs + PhaseEvaluator.java edit + 19 test-constructor updates + sprint-038-handoff.md).
- ✅ Constitution-compliance verified: §1.3 LLM ownership preserved (Skill `procedure` is LLM-soft; no Skill prescribes customer-facing language or per-step arguments); §1.4 Runtime ownership preserved (tool whitelist via `PhasePlan.allowedTools` composed from `skill.tools_required` + existing `ToolDispatcher.validateAgainstPlan`); §1.7 forbidden-list honored (no per-UC-branch if-else; no eval phrases; no LLM-vs-Java boundary shift).
- ✅ Handoff §6 walks §4.1 kernel on Sprint 38 diff; verdict `approve`.
- ✅ Reproducibility: every code-citation in §4 / §5 / §9 cites file path + line numbers verifiable via `wc -l <path>` / `grep -n <pattern> <path>` / `git show <sha>:<path>`.

**Codex per-sub-sprint review at Sprint 38 close** (per Sprint 38 §9 Codex review section + §4.3 trigger #3): expected verdict `approve`. Codex independently re-walks the §4.1 9 questions, verifies the 4 Skill YAML bodies for per-UC-branch absence (M2 §6 #1 load-bearing), verifies behavioural equivalence (re-runs `PhaseEvaluatorSkillIntegrationTest` and inspects the golden-string assertions), verifies Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) honored verbatim, verifies no scope creep, verifies Java baseline preservation (re-runs `mvn test -q`), and adjudicates OQ-S38.1 (template-vs-legacy editorial divergence).

## 12. Closure verdict placeholder

Per `feedback_handoff_verdict_section_delegation.md`, this section is a placeholder. The deliver-agent + human + Codex per-sub-sprint review fill it in at Sprint 38 close per Sprint 38 §11 + §12 + `iteration_governance.md` §4.3 trigger #3.

The deliver-agent drafts `compact/sprint-038-review-prompt.md` + dispatches Codex per-sub-sprint review at Sprint 38 close per Sprint 38 §5.1 sub-sprint-close artefacts (deliver-agent owns `compact/sprint-038-review-prompt.md` drafting + Codex dispatch). Codex review verdict (per `iteration_governance.md` §4.1 + `docs/codex-findings.md` sprint-close header per §4.2) is written to `docs/codex-findings.md` at Sprint 38 close; deliver-agent archives to `docs/sprints/sprint-038-codex-review.md` at sub-sprint close.

| field | value |
|-------|-------|
| status | _(deliver-agent + human + Codex per-sub-sprint review at Sprint 38 close)_ |
| classification | _(A / A-with-Codex-finding-OOSR / B-fix-required / C-OOSR / D-human-escalation per `iteration_governance.md` §4 close decision rules)_ |
| Codex outcome | _(decision: pass | fix_required | out_of_scope_review / blocking_count: N)_ |
| Tier-0 candidate disposition | _(Sprint 37's C2 + C3 R-items carried forward unchanged unless Sprint 38 Codex review surfaces NEW evidence)_ |
| Open question disposition | _(OQ-S38.1 template-vs-legacy editorial divergence; OQ-S38.2 composer placement; OQ-S38.3 maxToolSteps default; OQ-S38.4 SkillTestFixtures fixture; OQ-S38.5 C2/C3 R-items unchanged)_ |
| Premise refinement disposition | _(none; 12/12 §4 premises PASS)_ |
| date | _(Sprint 38 close date)_ |
