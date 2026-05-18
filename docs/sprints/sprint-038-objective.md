---
title: Sprint 38 objective archive — SkillRegistry core + 4 simpler phase Skills migration (M2 sub-sprint 2)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-037-objective.md]
superseded_by: null
notes: >
  Sprint 38 is the SECOND sub-sprint of NEW Milestone M2 (Skill
  Registry Abstraction + Wholesale Retroactive Externalization, per
  docs/milestone_objective.md). Sprint 38 is the FIRST implementation
  sub-sprint after the Sprint 37 design freeze; it lands the
  SkillRegistry core (Skill data class + SkillRegistry + SkillLoader
  Java) + the 4 simpler phase Skills as YAML (DISCOVER + CONFIRM +
  ESCALATE + TERMINAL/CLOSE) + PhaseEvaluator integration for those
  4 phases per Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4).

  Sprint 38 explicitly DOES NOT touch: RESOLVE_FAQ + RESOLVE_INTAKE
  (Sprint 39 scope per design doc §6.2.5-§6.2.6); Sprint 6/7/11
  predicate migration (Sprint 39 scope per design doc §8); NEW S1 +
  S2 predicates (Sprint 39 scope); unified Skill terminal-predicate
  dispatcher (Sprint 39 scope per design doc §9 — depends on Skill
  guardrails fields migrated in Sprint 39); system_prompt.txt
  teaching paragraph extraction (Sprint 40 scope per design doc §7);
  UC-switching state preservation (Sprint 41 scope per design doc
  §10). The MATERIAL FINDING from OLD Sprint 36 §1.2 (reconfirmed
  by NEW Sprint 37 Codex review) — Sprint 6/7/11 partial predicates
  ALREADY ship in AgentRunLoopImpl.java — is honored by Sprint 38
  keeping the predicate dispatch sites at AgentRunLoopImpl.java:317
  / 356 / 413 + method bodies at 628-651 / 690-734 / 745-759
  UNTOUCHED; the migration happens at Sprint 39.

  Layer: prompt_projection (Skill envelope reaches LLM via
  Skill-composed PhasePlan) + skill_state (SkillRegistry holds Skill
  definitions across requests; SkillLoader fail-fast at Spring boot)
  + Runtime-owned PhasePlan composition per §1.4. §7 stanza
  REQUIRED.

  Codex review: per-sub-sprint at Sprint 38 close per
  iteration_governance.md §4.3 trigger #3 (new architectural surface
  — SkillRegistry mediates between LLM prompt context and tool
  dispatch). Codex verifies behavioural equivalence pre/post-migration
  on the 4 migrated phases + no per-UC-branch if-else in any Skill
  YAML body or composed prompt + no scope creep into Sprint 39/40/41
  surfaces. Codex review prompt to be drafted by deliver-agent at
  Sprint 38 close BEFORE Codex dispatch.

  M2 acceptance bar recalibration (per docs/milestone_objective.md
  §5): bad-case suite (Alice) + interactive eval are OBSERVATION
  only for THIS milestone; Sprint 38 contract reflects this — no
  bad-case-rerun success metric; no interactive-eval-pass success
  metric; primary success is "SkillRegistry core lands + 4 phase
  Skill YAMLs migrate + behavioural equivalence preserved on Java
  tests for representative UCs across DISCOVER + CONFIRM + ESCALATE
  + TERMINAL/CLOSE + Codex per-sub-sprint review verdict approve".
---

# Sprint 38 — SkillRegistry core + 4 simpler phase Skills migration

**Milestone:** M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) sub-sprint 2 of 5 per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Implementation sub-sprint, single track (Track A), semantic-touching multi-layer.** Sprint 38 ships the SkillRegistry core (Skill data class + SkillRegistry + SkillLoader Java classes per design doc §2 + §3) + 4 simpler phase Skills as externalized YAML (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE per design doc §6.2.1-§6.2.4) + PhaseEvaluator integration for those 4 phases per design doc §4 (composeSkillPhasePlan helper). Layer: `prompt_projection` (Skill envelope composed into PhasePlan reaches LLM) + `skill_state` (SkillRegistry holds Skill definitions across requests; SkillLoader fail-fast at Spring boot) + Runtime-owned PhasePlan composition per Constitution §1.4 (Runtime-owned trace/eval contract surface).

**§7 stanza REQUIRED** per the M2 §1 sub-sprint layer breakdown (semantic-touching multi-layer).

**Codex review:** per-sub-sprint at Sprint 38 close per `iteration_governance.md` §4.3 trigger #3 (new architectural surface — SkillRegistry is a NEW abstraction layer mediating between LLM prompt context and tool dispatch). Codex verifies: (a) behavioural equivalence pre/post-migration on 4 migrated phases for representative UCs in each phase; (b) no per-UC-branch if-else in any Skill YAML body or composed prompt per §6 #1; (c) no scope creep into Sprint 39 (RESOLVE_FAQ / RESOLVE_INTAKE / predicate migration / unified dispatcher / S1+S2) / Sprint 40 (system_prompt.txt teaching extraction) / Sprint 41 (UC switch + state preservation) surfaces; (d) Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) honored verbatim; (e) M1 functional surfaces preserved (Sprint71PartialIntakePersistenceTest 14/14; IntakeFieldExtractor; Sprint 32/33 projection slots in ContextProjectionBuilder).

## 2. Goal

Implement the Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) as runtime code, landing the SkillRegistry abstraction for 4 of 6 phases. Post-Sprint-38, the 4 migrated phases (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE) compose PhasePlan from externalized Skill YAML via SkillRegistry; the 2 unmigrated phases (RESOLVE_FAQ + RESOLVE_INTAKE) continue to compose PhasePlan from existing Java-string content per the legacy PhaseEvaluator branches (Sprint 39 scope to migrate). Mixed migration window per design doc §4.3 — PhaseEvaluator routes through SkillRegistry for the 4 migrated phases AND directly for the 2 legacy phases until Sprint 39 closes the abstraction.

Concrete deliverables:

- **(D-a) Skill data class** — `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (Java record with the 12 fields per design doc §2.1: `name`, `description`, `applicable_phases`, `applicable_use_cases`, `tools_required`, `required_context_keys`, `max_tool_steps`, `allow_interim_message`, `valid_terminal_outcomes`, `objective`, `procedure`, `grounding_instruction`, `escalation_policy`, `guardrails`, `state_inheritance`). Sprint 38 ships the `guardrails: []` and `state_inheritance: { inherit: [...], reset: [], soft_signal_via_projection: [...] }` fields as schema-defined but the 4 simpler Skills' `guardrails` arrays are EMPTY per design doc §6.2.1-§6.2.4 (the predicates live in Sprint 39 Skills). The `state_inheritance` block IS populated for each Skill per the design doc templates (`inherit: [customer_context]` for DISCOVER; `inherit: [customer_context, accumulated_tool_results]` for CONFIRM + ESCALATE + TERMINAL; `soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]` for DISCOVER only).
- **(D-b) SkillRegistry + SkillLoader Java classes** — `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` with `select(phase, useCase) → Skill` method per design doc §3.2 (exact-match → wildcard `["*"]` match → null fallback semantics; null fallback returns Optional.empty() OR throws — dev picks per design doc §3.2 alternatives; recommended Optional.empty() with caller responsible for falling back to legacy PhaseEvaluator path). `SkillLoader.java` parses YAML files from `server/src/main/resources/skills/` at Spring bootstrap via `@PostConstruct` or `@EventListener(ContextRefreshedEvent.class)` per design doc §3.3; fail-fast on schema validation error (missing required field, invalid `applicable_phases` enum, invalid `applicable_use_cases` enum, invalid `valid_terminal_outcomes` enum, malformed YAML) — Spring fails to start. JSON Schema validation contract per design doc §2.2.
- **(D-c) PhaseEvaluator integration** — modify `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` to add `composeSkillPhasePlan(Skill skill, Phase phase, String useCaseId, BotSession session) → PhasePlan` private helper per design doc §4.1. The helper composes the selected Skill with session state into a PhasePlan with the same data shape as today (backward-compatible per design doc §4.2). The `PhaseEvaluator.evaluate(...)` dispatch switch (currently at `PhaseEvaluator.java:355-360`) queries `skillRegistry.select(phase, useCase)` FIRST; if a Skill is returned (i.e., the 4 migrated phases), `composeSkillPhasePlan(...)` is used; if `Optional.empty()` is returned (i.e., the 2 unmigrated RESOLVE phases), falls through to the existing per-phase branch code (the legacy `if (phase == RESOLVE_FAQ) { ... }` blocks at `PhaseEvaluator.java:552-665` are PRESERVED UNCHANGED for Sprint 38; Sprint 39 migrates these).
- **(D-e §6.2.1-§6.2.4) 4 Skill YAML files** at `server/src/main/resources/skills/`:
  - `discover_triage.yaml` per design doc §6.2.1 template (migrates DISCOVER `systemInstruction` at `PhaseEvaluator.java:411-448` including Sprint 7 §I0 weak-candidate cue lines 418-431 + Sprint 33 ad-status disambiguation cue lines 432-448 into `procedure`; `objective` line 401-402 → `objective`; `groundingInstruction` lines 449-452 → `grounding_instruction`; `escalationPolicy` lines 453-455 → `escalation_policy`; `tools_required: [search_knowledge, classify_use_case]`; `state_inheritance.inherit: [customer_context]`; `state_inheritance.soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]`).
  - `confirm.yaml` per design doc §6.2.2 template (migrates CONFIRM content at `PhaseEvaluator.java:462-487`; `tools_required: [record_outcome, request_handover]`; `state_inheritance.inherit: [customer_context, accumulated_tool_results]`).
  - `escalate.yaml` per design doc §6.2.3 template (migrates ESCALATE content at `PhaseEvaluator.java:513-542`; `tools_required: [request_handover, record_outcome]` — `create_case_controlled` INTENTIONALLY excluded per design doc §6.2.3 note + Codex 1.8 / `customer_service_tool_spec_v0_2.yaml` runtime-only visibility rule per PhaseEvaluator inline comment at lines 514-521; `state_inheritance.inherit: [customer_context, accumulated_tool_results]`).
  - `terminal.yaml` per design doc §6.2.4 template (migrates CLOSE content at `PhaseEvaluator.java:492-509`; `tools_required: [record_outcome]`; `applicable_phases: [CLOSE]` per OQ-7.1 default — keep enum unchanged at Sprint 38; file named `terminal.yaml` carries the architectural intent for the M3+ possible rename; `state_inheritance.inherit: [customer_context, accumulated_tool_results]`).

Per design doc §6.2.5-§6.2.6: RESOLVE_FAQ + RESOLVE_INTAKE Skill YAMLs (`resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml`) are NOT shipped in Sprint 38 — those are Sprint 39 scope. Per design doc §8: Sprint 6/7/11 predicate migration to Skill `guardrails` + NEW S1 / S2 predicates + unified dispatcher are NOT shipped in Sprint 38 — Sprint 39 scope.

Per design doc §6.4: behavioural-equivalence test pattern verifies pre/post-migration `PhasePlan` is observationally identical for representative UCs in each of the 4 migrated phases. Sprint 38 ships the equivalence tests as part of D-c (PhaseEvaluator integration).

## 3. Non-goals (explicit)

- Sprint 38 does NOT migrate RESOLVE_FAQ + RESOLVE_INTAKE Skill YAMLs (Sprint 39 scope per design doc §6.2.5-§6.2.6 + M2 §3 Sprint 39 row). The legacy PhaseEvaluator branches at lines 552-596 (RESOLVE-INTAKE) + 598-665 (RESOLVE-FAQ) stay UNCHANGED.
- Sprint 38 does NOT migrate Sprint 6 `shouldRejectFaqMissHandover` (`AgentRunLoopImpl.java:690-734`) or Sprint 7 `shouldRejectIncompleteIntakeHandover` (`:628-651`) or Sprint 11 `shouldRejectPrematureResolveOutcome` (`:745-759`) Java methods into Skill `guardrails` blocks. Per the MATERIAL FINDING (OLD Sprint 36 §1.2 + NEW Sprint 37 Codex confirmation), these predicates already ship and dispatch from AgentRunLoopImpl.java at lines 413 / 317 / 356. The migration is Sprint 39 scope per design doc §8.2.1-§8.2.3.
- Sprint 38 does NOT add NEW S1 `must_cite_source` predicate or NEW S2 `intake_complete_required` predicate. Both are Sprint 39 scope per design doc §8.2.4 + §8.2.5.
- Sprint 38 does NOT ship the unified Skill terminal-predicate dispatcher (`SkillGuardrailDispatcher.java`) per design doc §9. Sprint 39 scope (the dispatcher depends on Skill `guardrails` declarations being populated, which Sprint 39 ships alongside the predicate migration).
- Sprint 38 does NOT migrate Sprint 23 `already_called` (`system_prompt.txt:23-28`) or Sprint 31 `alternate_candidate_use_cases` (`system_prompt.txt:30-34`) or Sprint 33 `discover_disambiguation_signals` (`system_prompt.txt:36-43`) teaching paragraphs from `system_prompt.txt` into Skill `procedure`. Sprint 40 scope per design doc §7. The `system_prompt.txt` file is UNTOUCHED in Sprint 38.
- Sprint 38 does NOT migrate the Sprint 33 ad-status disambiguation cue's SLOT description at `system_prompt.txt:36-43` into a Skill — Sprint 40 scope. Sprint 38 DOES migrate the cue's CUE BODY at `PhaseEvaluator.java:432-448` into `discover_triage.yaml` `procedure` (per the Sprint 33 cue/slot split — Sprint 38 migrates the cue per decision (e), Sprint 40 migrates the slot per decision (f); both end up in `discover_triage.yaml` `procedure` post-Sprint-40).
- Sprint 38 does NOT implement UC-switching state preservation (Sprint 41 scope per design doc §10). `SkillStateBus.java` is NOT created in Sprint 38; the NEW `prior_use_case_carry` projection slot is NOT added in Sprint 38; per-Skill `state_inheritance` declarations ARE populated in the 4 Skill YAMLs (per D-a above) but they are not yet ENFORCED at session-state-bus boundary (that's Sprint 41's `SkillStateBus.java` implementation).
- Sprint 38 does NOT touch `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` (M2 §6 #5 fence; M3-D Topic↔UC binding loosening deferred).
- Sprint 38 does NOT touch `INTAKE_UCS` set at `PhaseEvaluator.java:30` (M2 §6 #6 fence; M3-B Single Handover Orchestrator deferred).
- Sprint 38 does NOT widen `escalation_reason` enum at `PhaseEvaluator.java:39-63` (M2 §6 #7 fence; M3-A sibling fields deferred).
- Sprint 38 does NOT add a Tier-0 invariant to `docs/runtime_freeze_and_risk_policy.md` §1/§2 (M2 §6 #8 fence + Sprint 37 close pre-decision: C2 + C3 DEFER preserved; no candidate elevated at Sprint 38).
- Sprint 38 does NOT touch `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` (constitution-discipline preserved per `feedback_constitution_discipline_vs_planning_anticipation.md`).
- Sprint 38 does NOT touch `docs/foundational/` or `docs/proposals/skill_registry_design.md` (the Sprint 37 freeze is immutable per `doc_governance.md`).
- Sprint 38 does NOT touch sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-037-*`.
- Sprint 38 does NOT touch milestone archives under `docs/milestones/`.
- Sprint 38 does NOT touch `docs/milestone_objective.md` or `docs/sprint_objective.md` (deliver-agent-owned).
- Sprint 38 does NOT touch `docs/action_bank.md` in the dev session (deliver-agent-owned at sub-sprint close).
- Sprint 38 does NOT touch existing case families under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence carried from M1/M2).
- Sprint 38 does NOT touch shadow CaseSpecs under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
- Sprint 38 does NOT touch `eval_interactive/eval_interactive/` (harness, loader, simulator).
- Sprint 38 does NOT touch `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (M2 §5 acceptance recalibration: Alice is observation not gate; M2 §6 #22).
- Sprint 38 does NOT widen judge rubrics or `eval_interactive/case_spec_overrides.yaml`.
- Sprint 38 does NOT regress M1-shipped FUNCTIONAL surfaces — `IntakeFieldExtractor.java` UC-G/H/I/J/K extension preserved; `Sprint71PartialIntakePersistenceTest` 14/14 baseline preserved; Sprint 32 `alternate_candidate_use_cases` projection slot in `ContextProjectionBuilder` preserved; Sprint 33 `discover_disambiguation_signals` projection slot preserved (per M2 §6 #21).
- Sprint 38 does NOT use mocked-LLM as primary evidence for any LLM-behaviour claim (per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` + M2 §6 #17). Skill data-loading + SkillRegistry Java logic tests MAY mock LLM (those are deterministic Java logic tests); behavioural-equivalence tests on PhasePlan composition MAY use mocked LLM (those verify Java composition logic, not LLM behaviour). Sprint 38 ships no real-LLM eval rerun as primary evidence; Java behavioural-equivalence + Java baseline preservation are the primary evidence.
- Sprint 38 does NOT pre-decide Sprint 39 / 40 / 41 implementation specifics beyond what design doc (e §6.2.5-§6.2.6 / f §7 / g §8 / h §9 / i §10) freezes. Downstream sub-sprint contracts are drafted per deliver-agent + human round AFTER Sprint 38 close.
- Sprint 38 does NOT rename the `CLOSE` phase enum to `TERMINAL` per OQ-7.1 default (decision: keep enum unchanged at Sprint 38; Skill file named `terminal.yaml` carries the architectural intent for the M3+ possible rename). Touching the phase enum would cascade through PhaseEvaluator, AgentRunLoopImpl, all PhasePlan consumers, and the full Java baseline — out of Sprint 38 scope.

## 4. Premise check (deliver-agent verified 2026-05-17; dev re-verifies at session start)

Verified at HEAD `51c327c` (post-Sprint-37-close; pre-Sprint-38-dev):

1. **M2 milestone objective approved + Sprint 37 closed.** `docs/milestone_objective.md` carries the NEW M2 contract (status `current`; supersedes OLD M2-Skill); Sprint 37 closed PASS A 2026-05-17 (commit `51c327c`; Codex per-sub-sprint review `pass / 0` at `docs/sprints/sprint-037-codex-review.md`).
2. **Sprint 38 contract approved.** `docs/sprint_objective.md` (this file) is the active sub-sprint contract; approved by human (after Sprint 37 close housekeeping).
3. **Sprint 37 design freeze at `docs/proposals/skill_registry_design.md`** — `status: proposal` (frozen; immutable per `doc_governance.md`). Sprint 38 dev SHALL read sections §2 (decision (a)) + §3 (decision (b)) + §4 (decision (c)) + §6.2.1 (DISCOVER mapping) + §6.2.2 (CONFIRM mapping) + §6.2.3 (ESCALATE mapping) + §6.2.4 (TERMINAL/CLOSE mapping) at session start. Other sections (§5 procedure/guardrails split; §6.2.5-§6.2.6 RESOLVE migration; §7 Sprint 23/31/33 teaching migration; §8 predicate migration; §9 dispatcher; §10 state model; §11 §4.1 walk; §12 Tier-0 candidate enumeration; §13 downstream reference index) are CONTEXTUAL ONLY for Sprint 38 dev — Sprint 38 implements decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4), NOT decisions (d)/(e §6.2.5-§6.2.6)/(f)/(g)/(h)/(i)/(j).
4. **`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`** at HEAD: 1628 lines; `INTAKE_UCS` set at line 30; `escalation_reason` enum at lines 33-63 (do NOT touch); `UC_TEAM_NAME` map at lines 112-; `buildIntakeSystemInstruction` helper at lines 207-208; phase dispatch switch at lines 355-360; per-phase branches: DISCOVER 397-457 (Sprint 38 migrates) / CONFIRM 462-487 (Sprint 38 migrates) / CLOSE 492-509 (Sprint 38 migrates) / ESCALATE 513-542 (Sprint 38 migrates) / RESOLVE-INTAKE 552-596 (PRESERVED for Sprint 39) / RESOLVE-FAQ 598-665 (PRESERVED for Sprint 39). Sprint 38 dev SHALL re-verify line numbers at session start.
5. **`server/src/main/java/com/gumtree/csagent/model/PhasePlan.java`** at HEAD: 146 lines; existing record with 11 fields per design doc §4.2 (backward-compatible). Sprint 38's `composeSkillPhasePlan(...)` produces PhasePlan instances with the same field set as the legacy branches.
6. **`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`** at HEAD: 787 lines; UNCHANGED in Sprint 38. Sprint 6/7/11 predicates at lines 628-651 / 690-734 / 745-759 dispatch sites at 317 / 413 / 356 ALL PRESERVED for Sprint 39 migration.
7. **`server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java`** at HEAD: 222 lines; UNCHANGED in Sprint 38 (the registry is the source of truth for per-UC required intake fields per Sprint 36 premise #8 refinement; Sprint 38 does NOT touch intake — RESOLVE-INTAKE migration is Sprint 39 scope).
8. **`server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java`** at HEAD: 174 lines; `UseCaseDefinition` record at lines 166-173 with 6 fields (NOT `requiredIntakeFields`; that's in `IntakeFieldsRegistry`). Sprint 38 reads `UseCaseRegistryService` for parameterized Skill compose (`ucDef.name()` for `{uc_name}` placeholder substitution); does NOT modify.
9. **`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`** at HEAD: 1161 lines; UNCHANGED in Sprint 38 (NEW `prior_use_case_carry` projection slot is Sprint 41 scope per design doc §10.4).
10. **`server/src/main/resources/prompts/system_prompt.txt`** at HEAD: 101 lines (per Sprint 37 premise #7 refinement); UNCHANGED in Sprint 38 (Sprint 23/31/33 teaching extraction is Sprint 40 scope per design doc §7).
11. **`server/src/main/resources/skills/`** directory at HEAD: does NOT exist. Sprint 38 creates it via the 4 Skill YAML files per D-e §6.2.1-§6.2.4.
12. **Java baseline.** 983 / 1-inherited / 0 / 2 post-Sprint-37-close. Sprint 38 ships ~10-20 new tests (SkillTest + SkillRegistryTest + SkillLoaderTest + PhaseEvaluatorSkillIntegrationTest behavioural-equivalence for 4 phases); the 1 inherited `SystemPromptUserRequestedTiebreakerTest` failure persists (dirty working-tree `system_prompt.txt` baseline unchanged from Sprint 37 close). Sprint71PartialIntakePersistenceTest 14/14 baseline preserved.

Sprint 38 dev SHALL re-verify these at session start by reading the cited files (one read each + one grep for line-number citation per file in handoff §3). If any premise has drifted between 2026-05-17 and dev session start, STOP and surface; do NOT implement under a false premise.

## 5. Files in scope (Sprint 38 dev ships)

| path | change type |
|------|-------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` | **NEW** — Java record per design doc §2.1 (12 fields); JSON Schema reference for validation contract |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` | **NEW** — `select(phase, useCase) → Optional<Skill>` per design doc §3.2; Spring bean; constructor-injected with SkillLoader |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` | **NEW** — YAML parser (Jackson YAML OR SnakeYAML; dev picks per consistency with existing repo dependencies — recommend Jackson YAML if already in use); reads `server/src/main/resources/skills/*.yaml` at `@PostConstruct`; fail-fast schema validation per design doc §3.3 |
| `server/src/main/resources/skills/discover_triage.yaml` | **NEW** — DISCOVER Skill per design doc §6.2.1 template |
| `server/src/main/resources/skills/confirm.yaml` | **NEW** — CONFIRM Skill per design doc §6.2.2 template |
| `server/src/main/resources/skills/escalate.yaml` | **NEW** — ESCALATE Skill per design doc §6.2.3 template |
| `server/src/main/resources/skills/terminal.yaml` | **NEW** — TERMINAL/CLOSE Skill per design doc §6.2.4 template (file name `terminal.yaml`; `applicable_phases: [CLOSE]` per OQ-7.1 default) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | **EDIT** — add `composeSkillPhasePlan(Skill, Phase, String, BotSession) → PhasePlan` private helper per design doc §4.1; modify `evaluate(...)` dispatch switch at lines 355-360 to query `skillRegistry.select(phase, useCase)` FIRST + use `composeSkillPhasePlan(...)` if Skill returned; ELSE fall through to legacy per-phase branch (UNCHANGED for RESOLVE_INTAKE 552-596 + RESOLVE_FAQ 598-665); DELETE legacy per-phase branch code for the 4 migrated phases (DISCOVER 397-457 + CONFIRM 462-487 + CLOSE 492-509 + ESCALATE 513-542) once SkillRegistry-driven composition is verified equivalent OR keep both paths in parallel with a feature-flag-style guard for safer rollback (dev picks; recommend deletion since the migration is the whole point + Java tests cover behavioural equivalence) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` | **NEW** — unit tests for Skill record (field accessors; default `guardrails: []` + `state_inheritance: { inherit: [], reset: [], soft_signal_via_projection: [] }`); ~5-8 tests |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` | **NEW** — unit tests for `select(phase, useCase)` semantics (exact-match per phase + wildcard `["*"]` + null fallback / Optional.empty); ~6-8 tests |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` | **NEW** — unit tests for SkillLoader (loads 4 Skills from `skills/`; fail-fast on schema error: missing field / invalid `applicable_phases` enum / invalid `applicable_use_cases` enum / invalid `valid_terminal_outcomes` enum / malformed YAML; verifies all 4 Skill YAMLs parse + validate at boot); ~10-12 tests |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` | **NEW** — behavioural-equivalence integration test per design doc §6.4 for each of the 4 migrated phases. For each phase: enumerate representative UCs (DISCOVER: any UC since `applicable_use_cases: ["*"]`; CONFIRM: any UC; ESCALATE: any UC; CLOSE: any UC), assert pre-migration PhasePlan (captured from a checkout of the legacy branch code at the start of Sprint 38) and post-migration PhasePlan (from SkillRegistry-driven composition) are field-by-field equal (same `objective` / `allowedTools` / `requiredContextKeys` / `maxToolSteps` / `validTerminalOutcomes` / `systemInstruction` / `groundingInstruction` / `escalationPolicy`). ~12-16 tests (3-4 representative UCs × 4 phases). The pre-migration capture can be hardcoded golden strings checked into the test file (since the pre-migration code is being deleted as part of D-c, the test asserts the new composition matches the golden strings). |
| `docs/sprints/sprint-038-handoff.md` | **NEW** (12-section dev-authored archive per Sprint 31-37 shape). |

### 5.1 Sub-sprint-close artefacts (deliver-agent owned, M2 milestone-scoped)

| path | change type |
|------|-------------|
| `docs/sprints/sprint-038-handoff.md` | NEW (12-section dev-authored archive) |
| `docs/sprint_objective.md` | EDIT at Sprint 38 close (deliver-agent replaces with Sprint 39 contract; archives this contract to `docs/sprints/sprint-038-objective.md`) |
| `docs/10-handoff.md` §1 lead | EDIT at Sprint 38 close (deliver-agent demotes Sprint 38 to "preceding sub-sprint", sets Sprint 39 as current) |
| `docs/action_bank.md` | EDIT at Sprint 38 close (deliver-agent): flip Sprint 5 F2 S1+S2 + Sprint 6/7/11 + S1/S2 migration R-items per Sprint 38 progress; add Sprint 38 close-action index entry; refresh `D-skill-runtime-framework` (line 345) — the Skill Registry abstraction is now landing, so D-deferred status may need an update |
| `docs/codex-findings.md` | EDIT at Sprint 38 close — Codex per-sub-sprint review fires per §4.3 trigger #3; deliver-agent dispatches; Codex writes findings; deliver-agent archives at Sprint 38 close to `docs/sprints/sprint-038-codex-review.md` |
| `compact/sprint-038-dev-prompt.md` | deliver-agent-owned (authored before Sprint 38 dev session; NOT staged by dev) |
| `compact/sprint-038-review-prompt.md` | deliver-agent-owned, drafted at Sprint 38 close BEFORE Codex dispatch (NOT in dev session) |

## 6. Files NOT in scope (hard fences)

1. **No edit to `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` RESOLVE-INTAKE branch (lines 552-596) or RESOLVE-FAQ branch (lines 598-665).** Sprint 39 scope per design doc §6.2.5-§6.2.6.
2. **No edit to `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`.** Sprint 6/7/11 predicates at lines 628-651 / 690-734 / 745-759 + dispatch sites at 317 / 413 / 356 all PRESERVED. Sprint 39 scope per design doc §8.2.1-§8.2.3.
3. **No edit to `server/src/main/resources/prompts/system_prompt.txt`.** Sprint 23/31/33 teaching extraction is Sprint 40 scope per design doc §7.
4. **No NEW S1 `must_cite_source` or NEW S2 `intake_complete_required` predicates.** Sprint 39 scope per design doc §8.2.4 + §8.2.5.
5. **No unified Skill terminal-predicate dispatcher (`SkillGuardrailDispatcher.java`).** Sprint 39 scope per design doc §9 — depends on Skill `guardrails` declarations being populated (Sprint 39).
6. **No `SkillStateBus.java` or NEW `prior_use_case_carry` projection slot or `ContextProjectionBuilder.java` edit.** Sprint 41 scope per design doc §10. The `state_inheritance` block IS populated in the 4 Skill YAMLs per D-a (schema-defined), but it is NOT yet enforced at session-state-bus boundary (Sprint 41's `SkillStateBus.java`).
7. **No touch to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java`** (M2 §6 #5 fence; M3-D Topic↔UC binding loosening deferred).
8. **No edit to `INTAKE_UCS` set at `PhaseEvaluator.java:30`** (M2 §6 #6 fence; M3-B Single Handover Orchestrator deferred).
9. **No `escalation_reason` enum widening at `PhaseEvaluator.java:39-63`** (M2 §6 #7 fence; M3-A sibling rationale/confidence fields deferred).
10. **No Tier-0 invariant added to `docs/runtime_freeze_and_risk_policy.md`** (M2 §6 #8 fence; Sprint 37 close pre-decision preserved — C2 + C3 DEFER; if Sprint 38 dev session surfaces a NEW Tier-0 candidate, STOP and surface in handoff §7 OQ for deliver-agent + human evaluation; do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 38 dev commit).
11. **No edit to `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md`** (constitution-discipline preserved per `feedback_constitution_discipline_vs_planning_anticipation.md`).
12. **No edit to `docs/proposals/skill_registry_design.md`** (Sprint 37 freeze; immutable per `doc_governance.md`).
13. **No edit to other docs under `docs/foundational/` or `docs/current/`** (other than the Sprint 38 handoff).
14. **No edit to sprint archives** under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-037-*`.
15. **No edit to milestone archives** under `docs/milestones/`.
16. **No edit to `docs/milestone_objective.md` or `docs/sprint_objective.md`** (deliver-agent-owned).
17. **No edit to `docs/action_bank.md`** in the dev session (deliver-agent-owned at sub-sprint close).
18. **No edit to `docs/codex-findings.md`** in the dev session (Codex writes at Sprint 38 close; deliver-agent dispatches).
19. **No edit to existing case families** under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence carried from M1 + M2).
20. **No edit to shadow CaseSpecs** under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
21. **No edit to `eval_interactive/eval_interactive/`** (harness, loader, simulator) — irrelevant to Sprint 38 scope.
22. **No edit to `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`** (M2 §5 acceptance recalibration: Alice is observation not gate; M2 §6 #22).
23. **No widening of `eval_interactive/case_spec_overrides.yaml`**.
24. **No mocked-LLM as primary evidence** for any LLM-behaviour claim (per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` + M2 §6 #17). Java composition logic tests MAY mock LLM (those are deterministic Java logic tests); behavioural-equivalence integration tests MAY mock LLM (they verify Java PhasePlan composition, not LLM behaviour).
25. **No per-UC-branch if-else in any Skill YAML body** per Constitution §1.7 + M2 §6 #1. Skill `procedure` text is principle-level; `escalation_policy` text is principle-level. Per-UC variation enters only via `applicable_use_cases` list scope at the Skill level OR via template-substitution placeholders (`{uc_name}` / `{team_name}` / etc.) at compose time per design doc §6.2.5-§6.2.6 — Sprint 38's 4 Skills are non-parameterized (`applicable_use_cases: ["*"]`) so template substitution is not needed for D-e §6.2.1-§6.2.4 except possibly for the DISCOVER Skill where the Sprint 33 cue text mentions specific UCs by id (UC-F / UC-B / UC-C / UC-D); that text is preserved verbatim from `PhaseEvaluator.java:418-431` which itself was reviewed by Codex M1 / NEW Sprint 37 as NOT a per-UC-branch if-else (it's classification guidance, naming UCs as targets of inference, not branching behaviour by active UC).
26. **No Skill that prescribes LLM customer-facing language** (M2 §6 #18). Skill `procedure` is teaching at principle-level + tool envelope + recommended order; customer-facing language is LLM-owned per §1.3.
27. **No Skill that hard-encodes per-step argument values** (M2 §6 #19). Skill `procedure` names tools + recommended order; per-step arguments are LLM-owned.
28. **No regression on Sprint71PartialIntakePersistenceTest 14/14** (M1 functional-surface preservation per M2 §6 #21).
29. **No regression on existing IntakeFieldExtractor tests / Sprint 32 alternate_candidate_use_cases projection slot tests / Sprint 33 discover_disambiguation_signals projection slot tests** (M1 functional-surface preservation per M2 §6 #21).
30. **No CLOSE → TERMINAL phase enum rename** in Sprint 38 (OQ-7.1 default at Sprint 38 planning: keep enum unchanged; Skill file named `terminal.yaml` carries architectural intent; rename considered for M3+ if needed).
31. **No pre-decision of Sprint 39 / 40 / 41 implementation specifics beyond what design doc freezes.** Downstream sub-sprint contracts are deliver-agent-owned at planning round AFTER Sprint 38 close.

## 7. Bundle policy

- **Single dev commit** with all §5 dev-authored files: 3 new Java classes (Skill, SkillRegistry, SkillLoader); 4 new Skill YAML files; PhaseEvaluator.java edit; 4 new test files (~30-40 tests total); `docs/sprints/sprint-038-handoff.md`.
- Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`). Deliver-agent + human bundle those at sub-sprint close.
- **Tests are part of the dev commit**, NOT a separate fix-iteration. Per `iteration_governance.md` §5.1 acceptance bars: tests for the new abstraction + behavioural-equivalence for the 4 migrated phases ship together; Java baseline preservation is the hard gate.

## 8. Layer-classification + anti-hardcode stanza (per `iteration_governance.md` §7; REQUIRED)

**Target failure layer:** `prompt_projection` + `skill_state` + Runtime-owned PhasePlan composition per §1.4. Sprint 38 lands a NEW abstraction (SkillRegistry) that mediates between LLM prompt context and tool dispatch; the LLM observes the same `systemInstruction` / `groundingInstruction` / `escalationPolicy` / `allowedTools` / etc. content post-migration as pre-migration (behavioural equivalence on PhasePlan composition); only the SOURCE of the content moves from hardcoded Java strings to externalized Skill YAML.

**Tier-0 invariant:** This sprint adds NO Tier-0 invariant. Sprint 37 freeze §12 surfaced 5 candidates (C1 REJECTED — existing invariant; C2 + C3 QUALIFIED-DEFER — R-items in action_bank §5.2 for M3+ revisit; C4 + C5 NOT-A-CANDIDATE). Sprint 38 honors the DEFER pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md` — C2 (guardrail refusal non-overridability) is Sprint 39 enforcement surface (no observed evidence yet); C3 (state-bus boundary enforcement) is Sprint 41 enforcement surface (no observed evidence yet). The Skill Registry abstraction itself enforces tool-whitelist non-conditionality by construction per §1.4 (PhasePlan.allowedTools composed from `skill.tools_required` + existing `ToolDispatcher.validateAgainstPlan` enforcement at `ToolDispatcher.java:183-195`); C1 REJECTED verdict preserved.

**Semantic hardcode:** No semantic hardcode introduced. The 4 Skill YAML files migrate existing `PhaseEvaluator.java` Java-string content to externalized YAML; the content is bit-for-bit equivalent (modulo whitespace normalization). The composeSkillPhasePlan(...) helper composes the Skill into PhasePlan without per-UC-branch logic — `applicable_use_cases: ["*"]` for the 4 simpler phase Skills means no UC routing; PhaseEvaluator queries SkillRegistry by (phase, useCase) per design doc §3.2 which is a lookup, NOT a branch table. No keyword / regex / if-else / enum / per-UC matrix added.

**Generalization coverage:** target = 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE) behavioural-equivalence verification per `PhaseEvaluatorSkillIntegrationTest`. Neighbor = the 2 unmigrated RESOLVE phases (RESOLVE-INTAKE 552-596 + RESOLVE-FAQ 598-665) — Sprint 38 does NOT touch their PhasePlan composition, so existing Java tests for those phases (Sprint71PartialIntakePersistenceTest 14/14 + IntakeFieldExtractor tests + Sprint 32/33 projection-slot tests) remain green. Negative = SkillLoader fail-fast on malformed YAML / missing required field / invalid enum value — `SkillLoaderTest` verifies (~10-12 tests). Shadow = N/A for Sprint 38 (behavioural-equivalence on Java PhasePlan composition is fully testable; LLM behaviour observation is OBSERVATION ONLY per M2 §5 — not a Sprint 38 hard gate; deliver-agent + Codex own shadow review at Sprint 39+ close per `_ACCESS_BOUNDARY.md`).

## 9. Success metrics (per `iteration_governance.md` §5; HARD GATES + observations per M2 §5 recalibration)

**Hard gates (must pass for Sprint 38 close):**

- 3 new Java classes (Skill / SkillRegistry / SkillLoader) compile + pass new unit tests (SkillTest + SkillRegistryTest + SkillLoaderTest).
- 4 Skill YAML files load + validate at Spring boot via SkillLoader; `SkillLoaderTest` verifies fail-fast on schema error (missing field, invalid enum, malformed YAML).
- PhaseEvaluator integration: `composeSkillPhasePlan(...)` helper produces PhasePlan instances that are field-by-field equivalent to the pre-migration per-phase branch output for each of the 4 migrated phases (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE) on representative UCs; `PhaseEvaluatorSkillIntegrationTest` covers ~12-16 cases (3-4 representative UCs × 4 phases).
- Java baseline preserved (post-Sprint-37-close baseline 983 + ~30-40 new Sprint 38 tests = ~1013-1023; 0 new failures; 1 inherited `SystemPromptUserRequestedTiebreakerTest` failure persists; 0 new errors; 2 skipped).
- Sprint71PartialIntakePersistenceTest 14/14 baseline preserved (M1 functional-surface protection).
- IntakeFieldExtractor + Sprint 32 alternate_candidate_use_cases projection slot + Sprint 33 discover_disambiguation_signals projection slot tests remain green (M1 functional-surface protection).
- No per-UC-branch if-else in any Skill YAML body (Codex verifies in per-sub-sprint review per M2 §6 #1).
- No scope creep into Sprint 39 (RESOLVE_FAQ / RESOLVE_INTAKE / predicate migration / S1+S2 / unified dispatcher) / Sprint 40 (system_prompt.txt) / Sprint 41 (SkillStateBus / prior_use_case_carry) surfaces.
- Constitution-compliance verified: §1.3 LLM ownership preserved (Skill `procedure` is LLM-soft teaching; no Skill prescribes customer-facing language or per-step arguments); §1.4 Runtime ownership preserved (tool schema unchanged; capability/permission boundary unchanged via existing `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`; PII / safety floor / grounding floor unchanged); §1.7 forbidden-list honored (no per-UC-branch if-else; no eval phrases encoded in YAML; no LLM-vs-Java boundary shift).
- Handoff §8 walks §4.1 kernel on Sprint 38 diff; expected verdict `approve` (no per-UC-branch hardcode; behavioural equivalence preserved; tool-whitelist semantics unchanged).
- Reproducibility: every quantitative or code-citation claim in the design doc + handoff cites source path + line number + extraction recipe per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

**Codex review (per-sub-sprint at Sprint 38 close):**

- §4.1 anti-hardcode kernel verdict: `approve` (the Sprint 38 diff honors Constitution + Sprint 37 freeze; no per-UC-branch if-else; no scope expansion).
- §4.2 sprint-close header: `decision: pass | fix_required | out_of_scope_review` per outcome.
- Codex independently re-walks the §4.1 9 questions on the Sprint 38 diff.
- Codex independently verifies the 4 Skill YAML bodies for per-UC-branch absence (load-bearing per M2 §6 #1).
- Codex independently verifies behavioural equivalence claim on `PhaseEvaluatorSkillIntegrationTest` (re-runs the test from clean checkout; verifies all field-by-field equality assertions pass).
- Codex independently verifies Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) honored verbatim (cross-references design doc + Sprint 38 implementation).
- Codex independently verifies no scope creep (RESOLVE_FAQ / RESOLVE_INTAKE branches at PhaseEvaluator.java:552-665 UNCHANGED; AgentRunLoopImpl.java UNCHANGED; system_prompt.txt UNCHANGED; ContextProjectionBuilder.java UNCHANGED; no NEW Tier-0).
- Codex independently verifies Java baseline preservation (re-runs `mvn test -q`; verifies 1 inherited failure unchanged + 0 new failures + ~30-40 new Sprint 38 tests pass).
- Codex independently verifies M1 functional-surface preservation (Sprint71PartialIntakePersistenceTest 14/14 + IntakeFieldExtractor + Sprint 32/33 projection slots).

**Observations (not gates; per M2 §5 recalibration; MAY regress on future implementation sub-sprints):**

- Interactive eval smoke composite_score / pass_rate / judge dims (re-run at Sprint 38 close for tracking; OBSERVATION per §5.5 demotion + M2 §5 recalibration). MAY REGRESS; does NOT block.
- Bad-case suite (Alice) closure-criterion (a) result + fabrication-condition trigger count (re-run at Sprint 38 close for tracking; OBSERVATION per M2 §5 recalibration). MAY STAY FLAT (Sprint 38 ships no behaviour change beyond architectural refactoring of PhasePlan source); does NOT block.
- Architecture-health metrics direction (§6 governance):
  - `new_semantic_hardcode_count` expected = 0.
  - `soft_signal_conversion_count` expected = 4 (4 phase Skills moved from hardcoded Java strings to externalized Skill YAML).
  - `planner_ownership_ratio` expected unchanged (LLM ownership preserved per §1.3; Runtime ownership preserved per §1.4).
  - `shadow_disagreement_rate` not measured (Sprint 38 is architectural; behavioural equivalence is the gate, not shadow LLM behaviour observation).
- `PhaseEvaluator.java` line-count reduction (target: ~150-200 lines reduction across 4 deleted phase branches; phase content extracted to Skill YAML; PhaseEvaluator becomes thinner; observe at Sprint 38 close; final reduction tracked through Sprint 39 when remaining 2 phases migrate).

## 10. Stop conditions (dev-agent)

STOP and report (do NOT silently work around) when:

1. **Premise drift on §4 items** — any of the 12 premises has changed since 2026-05-17. STOP and surface in handoff §3.
2. **Behavioural equivalence test fails** on any of the 4 migrated phases — pre-migration PhasePlan field NOT field-by-field equal to post-migration PhasePlan field for any representative UC. STOP and surface in handoff §7 OQ; either Skill YAML mapping is wrong (refine per design doc §6.2.1-§6.2.4 template) OR composeSkillPhasePlan(...) helper has a logic bug. Do NOT widen the equivalence assertion to accept the divergence (would defeat the migration's behavioural-equivalence contract per design doc §6.4).
3. **Sprint 6/7/11 predicate test regression** — any of `shouldRejectFaqMissHandover` / `shouldRejectIncompleteIntakeHandover` / `shouldRejectPrematureResolveOutcome` related tests fail (these predicates are PRESERVED in AgentRunLoopImpl.java; Sprint 38 does NOT migrate them; regression would indicate accidental touch). STOP.
4. **Sprint71PartialIntakePersistenceTest 14/14 fails** — M1 functional-surface regression. STOP and diagnose.
5. **§1.7 forbidden-list violation cannot be cleanly resolved** in a Skill YAML body (e.g., the only way to encode a phase's `procedure` is per-UC-branch if-else; or the only way to surface a UC-specific signal is per-UC enum in the YAML). STOP and surface; do NOT silently introduce the §1.7 violation. Deliver-agent + human re-frame; possibly add a template-substitution placeholder OR projection slot instead.
6. **§1.3 boundary violation** — a Skill `procedure` is proposed that prescribes LLM customer-facing language OR per-step argument values. STOP and surface.
7. **§1.4 boundary mismatch** — a Skill field is proposed that enforces a Runtime-owned floor that the abstraction doesn't already enforce by construction (e.g., the SkillRegistry abstraction itself encodes a grounding floor rule for the 4 simpler phase Skills — these phases don't have grounding-floor predicates; that's Sprint 39 scope for RESOLVE_FAQ). STOP and surface.
8. **Tempted to migrate RESOLVE_FAQ / RESOLVE_INTAKE in Sprint 38** — STOP. Sprint 39 scope. The MATERIAL FINDING + design doc §6.2.5-§6.2.6 + decision (g) §8 lock the predicate + complex phase migration to Sprint 39.
9. **Tempted to migrate Sprint 6/7/11 predicates from AgentRunLoopImpl.java to Skill `guardrails` blocks** — STOP. Sprint 39 scope per design doc §8.2.1-§8.2.3. The Sprint 38 Skill YAMLs ship with `guardrails: []` per design doc §6.2.1-§6.2.4 (the 4 simpler phases have no predicates).
10. **Tempted to ship NEW S1 `must_cite_source` or NEW S2 `intake_complete_required` predicates in Sprint 38** — STOP. Sprint 39 scope per design doc §8.2.4 + §8.2.5; S1 predicate ships in `resolve_faq_grounded_answer.yaml` `guardrails` (Sprint 39); S2 predicate ships in `resolve_intake_collect_and_handover.yaml` `guardrails` (Sprint 39).
11. **Tempted to implement the unified Skill terminal-predicate dispatcher (`SkillGuardrailDispatcher.java`) in Sprint 38** — STOP. Sprint 39 scope per design doc §9; the dispatcher depends on Skill `guardrails` declarations being populated (Sprint 39).
12. **Tempted to ship `SkillStateBus.java` or NEW `prior_use_case_carry` projection slot in Sprint 38** — STOP. Sprint 41 scope per design doc §10. The `state_inheritance` block IS populated in the 4 Skill YAMLs (schema-defined per D-a) but is NOT yet enforced at session-state-bus boundary.
13. **Tempted to extract Sprint 23/31/33 teaching paragraphs from system_prompt.txt** — STOP. Sprint 40 scope per design doc §7. `system_prompt.txt` is UNTOUCHED in Sprint 38.
14. **Tempted to rename CLOSE phase to TERMINAL in Java enum** — STOP per Sprint 38 §6 #30. OQ-7.1 default at Sprint 38: keep enum unchanged; Skill file named `terminal.yaml` carries architectural intent; rename is M3+ scope.
15. **Tier-0 candidate surfaced from Sprint 38 implementation** — STOP; surface in handoff §7 OQ for deliver-agent + human evaluation at Sprint 38 close. Do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 38 dev commit; human escalation is required first.
16. **Tempted to edit `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md`** — STOP. Per constitution-discipline + Sprint 38 §6 #11 fence.
17. **Tempted to edit `docs/proposals/skill_registry_design.md`** — STOP. Sprint 37 freeze is immutable per `doc_governance.md`.
18. **Tempted to draft Sprint 39 / 40 / 41 contracts** — STOP. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`; deliver-agent-owned at planning round AFTER Sprint 38 close.
19. **Tempted to modify `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`** — STOP. Per M2 §6 #22 + Sprint 38 §6 #22 + Constitution §1.7 "widening eval spec to accept a genuine bot mistake" forbidden line.
20. **Tempted to use mocked-LLM as primary evidence for any LLM-behaviour claim** — STOP. Per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` + M2 §6 #17 + Sprint 38 §6 #24. Java composition logic tests + behavioural-equivalence tests MAY mock LLM (those are deterministic Java logic, not LLM behaviour); but no real-LLM eval rerun is needed for Sprint 38 (architectural refactoring, behavioural equivalence is the gate).

## 11. Handoff document contract (12 sections per Sprint 31-37 shape)

Standard 12-section shape adapted for implementation sub-sprint:

1. **Context Pack** — the M2 milestone objective + Sprint 38 contract (this file) + Sprint 37 freeze design doc §2 + §3 + §4 + §6.2.1-§6.2.4 + verified code shape at HEAD `51c327c` (PhaseEvaluator + PhasePlan + AgentRunLoopImpl + IntakeFieldsRegistry + UseCaseRegistryService + ContextProjectionBuilder + system_prompt.txt grep with line numbers cited at handoff §1.2 table; verify `server/src/main/resources/skills/` directory does NOT exist at session start) + feedback memory references (`feedback_handoff_verdict_section_delegation.md` for §12 placeholder; `feedback_commit_at_end_bundles_deliver_artefacts.md` for bundle policy; `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` for reproducibility).
2. **Sub-sprint-objective recap** — Sprint 38 SkillRegistry core + 4 simpler phase Skills migration; layer `prompt_projection` + `skill_state` + Runtime-owned PhasePlan composition; semantic-touching multi-layer.
3. **Premise re-verification** — §4 spot-check; 12 premises; one short paragraph per premise citing source.
4. **Implementation walkthrough** — high-level summary of Skill / SkillRegistry / SkillLoader Java classes; PhaseEvaluator integration; 4 Skill YAML files; cross-reference to Sprint 37 design doc sections.
5. **Sprint 37 freeze fidelity** — for each Sprint 37 freeze decision (a)/(b)/(c)/(e §6.2.1-§6.2.4), cite the implementation file + line range that honors the decision; note any departures from the freeze template with rationale.
6. **§4.1 anti-hardcode self-walk on the Sprint 38 diff** — walk all 9 questions; for each "yes" or each concern, paste the diff snippet and the reasoning. Expected verdict `approve` (architectural refactoring; behavioural equivalence; no per-UC-branch hardcode; tool-whitelist semantics unchanged).
7. **Open questions for deliver-agent + human** — any §1.7 boundary case requiring deliver-agent + human + Codex review; any premise drift that surfaced at session start; any tension between Sprint 37 freeze template and actual implementation; any decision that surfaced unexpected tension (e.g., null-fallback semantics on SkillRegistry.select per design doc §3.2 alternatives — Optional.empty() vs throw; dev picks and surfaces).
8. **Anti-hardcode self-walk (§4.1 nine questions) on Sprint 38 itself** — expected verdict `approve`.
9. **Files changed** — table with paths; 3 new Java classes + 4 new Skill YAML files + 1 PhaseEvaluator edit + 4 new test files + handoff = ~13 files.
10. **Layer-classification self-walk** per Sprint 38 §8 stanza — `prompt_projection` + `skill_state` + Runtime-owned PhasePlan composition; behavioural equivalence preserved; tool-whitelist semantics unchanged.
11. **§5 Eval Acceptance bars (adapted for architectural-refactoring sub-sprint + M2 §5 recalibration)** — Sprint 38 hard gates: 3 new Java classes compile + pass tests; 4 Skill YAMLs load + validate at boot; PhaseEvaluator integration; behavioural equivalence on 4 phases; Java baseline preservation 983 + ~30-40 new Sprint 38 tests = ~1013-1023; Sprint71PartialIntakePersistenceTest 14/14; M1 functional-surface preservation; Codex per-sub-sprint review verdict `approve` expected. Interactive eval smoke + bad-case suite are OBSERVATION ONLY per M2 §5 recalibration; record at close for tracking, MAY regress, does NOT block.
12. **Closure verdict placeholder** — leave for deliver-agent + human + Codex per-sub-sprint review per `feedback_handoff_verdict_section_delegation.md`. Sprint 38 closes at sub-sprint close per §4.3 per-sub-sprint Codex trigger #3 (new architectural surface; NOT deferred to M2 milestone close).

## 12. M2 milestone context (cross-reference, not Sprint 38 contract)

Sprint 38 is the SECOND of M2's 5 sub-sprints per `docs/milestone_objective.md`. After Sprint 38 commits:

1. **Deliver-agent + human review** Sprint 38 handoff + behavioural-equivalence test results at sub-sprint close. Validate: 3 new Java classes + 4 Skill YAMLs + PhaseEvaluator integration; behavioural equivalence preserved; Sprint 37 freeze decisions honored; no per-UC-branch hardcode.
2. **Deliver-agent drafts `compact/sprint-038-review-prompt.md`** + dispatches Codex per-sub-sprint review per §4.3 trigger #3.
3. **Codex review verdict.** If `pass`: proceed to Sprint 39 planning round. If `fix_required` with `blocking_count ≥ 1`: fix-iteration sub-sprint per existing convention BEFORE Sprint 39. If `out_of_scope_review`: deliver-agent + human classify per §4.2.
4. **Deliver-agent updates `docs/action_bank.md`** at Sprint 38 close: flip Sprint 5 F2 S1+S2 + Sprint 6/7/11 + S1/S2 migration R-items per Sprint 38 progress; refresh `D-skill-runtime-framework` (line 345) — the Skill Registry abstraction is now partially landing; close Sprint 38 in §6 action index.
5. **Deliver-agent + human housekeeping bundle at Sprint 38 close** (separate from Sprint 38 dev commit): refresh `docs/10-handoff.md` §1 lead; archive Sprint 38 codex review; reset codex-findings.md scaffold.
6. **Deliver-agent drafts Sprint 39 contract** at `docs/sprint_objective.md` (replacing this Sprint 38 contract; archives this contract to `docs/sprints/sprint-038-objective.md`) + `compact/sprint-039-dev-prompt.md`. Sprint 39 = RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + NEW S1 + S2 + unified Skill terminal-predicate dispatcher per design doc §6.2.5-§6.2.6 + §8 + §9. Surfaces to human for review BEFORE Sprint 39 dev session launch.
7. **M2 milestone close** happens after Sprints 37 + 38 + 39 + 40 + 41 all close; deliver-agent + human dispatch milestone-shared cumulative Codex review at that point per `iteration_governance.md` §4.3 (in addition to the per-sub-sprint reviews already done).
