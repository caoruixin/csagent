---
title: Sprint 39 handoff — RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 + unified Skill terminal-predicate dispatcher (NEW M2 sub-sprint 3)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 39 (NEW M2 sub-sprint 3) — dev-authored archive of the
  RESOLVE-FAQ + RESOLVE-INTAKE Skill migration + Sprint 6 §G2 + Sprint 7
  §I2 + Sprint 11 §M1 predicate migration + NEW S1 must_cite_source +
  NEW S2 intake_complete_required + unified SkillGuardrailDispatcher
  landing per Sprint 37 freeze decisions (e §6.2.5 + §6.2.6) + (g §8) +
  (h §9). Single dev commit; deliver-agent bundles deliver-owned
  artefacts at sub-sprint close.
---

# Sprint 39 handoff — RESOLVE Skill migration + predicate migration + unified dispatcher

**Sub-sprint:** NEW Milestone M2 sub-sprint 3 of 5 (per `docs/milestone_objective.md`).
**Parent commit:** `5787806` (Sprint 38 fix iteration #1; B-fix-iterated close 2026-05-17).
**Branch:** `refactor/remove-the-shackles`.

## 1. Context Pack

### 1.1 Relevant docs sampled

| doc | tier | status | relevance to Sprint 39 |
|---|---|---|---|
| `docs/sprint_objective.md` | current-runtime | current | active Sprint 39 contract (§1-§12) — binding scope |
| `docs/milestone_objective.md` | current-runtime | current | M2 north star + §6 #4 verbatim S1 authorization |
| `docs/proposals/skill_registry_design.md` | proposal (immutable) | proposal | Sprint 37 freeze — §5.2 + §6.2.5 + §6.2.6 + §6.4 + §8 + §9 are LOAD-BEARING for Sprint 39 implementation |
| `docs/sprints/sprint-038-handoff.md` | sprint-archive | archived | Sprint 38 main-dev archive — SkillRegistry-core landing |
| `docs/sprints/sprint-038-fix-handoff.md` | sprint-archive | archived | Sprint 38 fix #1 archive — SkillLoader schema-validation completeness |
| `docs/sprints/sprint-038-codex-review.md` | sprint-archive | archived | Sprint 38 Codex two-round verdict (pass on `5787806`) |
| `docs/current/iteration_governance.md` | durable-connective | current | §1.3 + §1.4 + §1.7 boundary checks; §4.1 anti-hardcode kernel; §4.3 per-sub-sprint Codex triggers; §5/§5.5/§5.6 acceptance bars |
| `docs/current/doc_governance.md` | durable-connective | current | tier model + code-ahead-of-docs rule (justifies legacy-text-verbatim preservation over freeze-template editorial paraphrasing) |

### 1.2 Verified code shape at HEAD `5787806` (session start)

All §4 premise table values re-verified at session start; see §3 below for the per-premise PASS / drift report.

| file | line count pre-Sprint-39 | landmark verification |
|---|---|---|
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 1529 | INTAKE_UCS @30, UC_TEAM_NAME @115, intakeCompleteTrigger @146, buildIntakeSystemInstruction @210, SkillRegistry field @262, ctor 11-arg @264, plan(...) @393, SkillRegistry-first dispatch @403-407, RESOLVE-INTAKE branch @417-460, RESOLVE-FAQ branch @463-530, composeSkillPhasePlan @548 — all matched contract §4 expected ranges |
| `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` | 787 | HANDOVER_TOOL @62, RECORD_OUTCOME_TOOL @65, PROGRESSIVE_RESOLVE_GUARD_REJECT_REASON @76-77, S1_GUARD_REJECT_REASON @111-112, INTAKE_COMPLETE_GUARD_REJECT_REASON @120-121, intake-complete dispatch @317, premature-resolve dispatch @356, faq-miss dispatch @413, shouldRejectIncompleteIntakeHandover @628-651, shouldRejectFaqMissHandover @690-734, shouldRejectPrematureResolveOutcome @745-759 — all matched |
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` | 222 | `intakeComplete(uc, collected)` @184, `requiredFieldsFor(uc)` @118 — all matched |
| `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` | 174 | `getUseCase(uc)`, `isKnownUseCase(uc)` — matched |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` | 205 | `shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClassArg)` @160 — Sprint 11 / 11.1 / 12 frozen surface PRESERVED unchanged |
| `server/src/main/resources/skills/` (4 Sprint 38 YAMLs) | — | discover_triage.yaml + confirm.yaml + escalate.yaml + terminal.yaml present |
| `SkillLoader.VALID_GUARDRAIL_TYPES` @117 | 4 entries | faq_miss_handover_requires_resolve_attempt + intake_complete_required + premature_resolve_outcome_guard + must_cite_source — load-bearing whitelist for Sprint 39's first concrete guardrails (no extension required) |
| `SkillLoader.VALID_PROJECTION_SLOTS` @65 | 3 entries | alternate_candidate_use_cases + discover_disambiguation_signals + **prior_use_case_carry** (Sprint 38-fix forward-compat slot; Sprint 39 RESOLVE Skills declare it in `state_inheritance.soft_signal_via_projection` — no allowlist extension needed) |
| Java baseline pre-Sprint-39 | 1034 / 1 inherited / 0 / 2 | mvn test from clean — measured at session start |

### 1.3 Feedback memory references

- `feedback_constitution_discipline_vs_planning_anticipation.md` — followed: did NOT pre-elevate C2 (guardrail refusal non-overridability) to Tier-0; Sprint 39 ships the FIRST observed evidence surface (the dispatcher with short-circuit-on-first-reject); re-evaluation deferred to Sprint 39 close OR M2 close.
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — followed: dev commit excludes `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-039-*.md`.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — followed: every line-number citation in this handoff cites a specific file + line at a specific point in time (HEAD `5787806` for pre-Sprint-39; post-Sprint-39 line numbers cited as approximate where the post-edit position shifted).
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — followed: dispatcher unit tests + behavioural-equivalence integration tests use Mockito mocks of `LlmInvocationService` ONLY for deterministic Java-logic dependency wiring (not for proving LLM behaviour); no real-LLM eval rerun is shipped as primary evidence (Sprint 39 is architectural refactoring + bounded predicate landing).
- `feedback_handoff_verdict_section_delegation.md` — followed: §12 closure verdict left for deliver-agent + human + Codex per-sub-sprint review at Sprint 39 close.

### 1.4 Doc-status warnings

None. The Sprint 37 freeze design doc + the Sprint 39 contract + the Sprint 38 archives are all current. The legacy-text-verbatim preservation (the RESOLVE Skill YAMLs carry the byte-for-byte legacy text rather than the design doc's editorially-paraphrased templates) is documented by the contract itself as "an editorial fold-back item per `doc_governance.md` code-ahead-of-docs"; deliver-agent + human handle the design-doc editorial fold-back at M2 close, NOT in this dev session.

### 1.5 Source-of-truth decision

For each Sprint 39 deliverable:

- **YAML Skill content:** the LEGACY `PhaseEvaluator.java` RESOLVE branch text at HEAD `5787806` is the source-of-truth (per contract + `doc_governance.md` code-ahead-of-docs); the design doc §6.2.5 / §6.2.6 templates' editorial paraphrasing is downstream fold-back. Captured as golden strings in `PhaseEvaluatorResolveSkillIntegrationTest`.
- **Dispatcher predicate semantics:** the LEGACY `shouldRejectXxx` Java method bodies at HEAD `5787806` are the source-of-truth; the dispatcher handler implementations preserve them bit-for-bit, including the `ResolveDispositionEvaluator` delegation for `premature_resolve_outcome_guard`. Verified by `Sprint11ProgressiveResolveTest` + `Sprint7IntakeStateTest` + `Sprint34IntakePrefillProjectionAndGuardTest` + `Sprint71PartialIntakePersistenceTest` + `AgentRunLoopS1FaqGroundedResolveGuardTest` (all 5 test files updated to invoke the dispatcher; all pre-existing assertions preserved).
- **NEW S1 `must_cite_source` scope:** the M2 §6 #4 verbatim human authorization (cited in contract §2.6 + freeze §8.2.4) is the source-of-truth for the predicate scope (3 boundaries: phase=RESOLVE_FAQ; tool=record_outcome with class=resolve; check=source_id citation presence). NO expansion beyond the 3 boundaries.

### 1.6 Implementation status

- 6 production Skill YAMLs loaded at boot (4 Sprint 38 + 2 Sprint 39) — `implemented`.
- `SkillGuardrailDispatcher` + `RejectVerdict` + `DispatchContext` (Spring `@Component`, `SkillRegistry`-injected) — `implemented`.
- `PhaseEvaluator.composeSkillPhasePlan(...)` template substitution + legacy RESOLVE branch deletion — `implemented`.
- `AgentRunLoopImpl` constructor extension (6-arg with dispatcher) + 3 dispatch site migrations + 3 `shouldRejectXxx` removals — `implemented`.
- `SkillTestFixtures.productionDispatcher()` helper + 8 test-file updates (5 predicate-migration sweep + 2 baseline assertion updates + 1 Sprint 38 test review) — `implemented`.

### 1.7 Risks before implementation (called out at session start)

1. Constructor sweep across 21 AgentRunLoopImpl test sites — mitigated by adding a backward-compatible 5-arg constructor that delegates to the 6-arg with `null` dispatcher; only 5 tests that exercise predicate semantics needed updates.
2. Behavioural-equivalence preservation on the buildIntakeSystemInstruction output's dynamic `requiredFieldsFor(uc).toString()` interpolation — mitigated by adding a 6th template-substitution placeholder `{intake_required_fields}` (handoff §7 OQ flagged).
3. Sprint 11 / 11.1 / 12 frozen surface preservation — verified by `ResolveDispositionEvaluator.java` UNCHANGED; the dispatcher's `handlePrematureResolveOutcomeGuard` delegates to the same static method the legacy `shouldRejectPrematureResolveOutcome` delegated to.
4. M1 functional surfaces (`Sprint71PartialIntakePersistenceTest` 14/14) — verified by re-running the test file post-migration with the dispatcher helper; all 14 tests pass.
5. Existing Sprint 38 tests (45) — verified by re-running; all pass.

## 2. Sub-sprint-objective recap

Sprint 39 is the third sub-sprint of NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) and the SECOND implementation sub-sprint after Sprint 37 design freeze + Sprint 38 SkillRegistry-core landing. Per the contract: the BIGGEST M2 sub-sprint by scope — multi-fence convergence at one sub-sprint (no silent scope split into Sprint 40+).

Layer: `prompt_projection` (RESOLVE Skill envelopes reach LLM via Skill-composed PhasePlan) + `semantic_planner` (LLM continues to own §1.3 surface) + `skill_state` (SkillRegistry + SkillGuardrailDispatcher) + Runtime-owned grounding floor per §1.4 (S1 `must_cite_source` bounded per M2 §6 #4 verbatim authorization) + Runtime-owned capability floor per §1.4. Semantic-touching multi-layer; §7 stanza REQUIRED.

## 3. Premise re-verification

| premise | contract §4 expected | session-start verified | drift? |
|---|---|---|---|
| 1. M2 milestone approved + Sprint 38 closed | docs/milestone_objective.md status `current`; Sprint 38 closed B-fix-iterated @5787806 | confirmed | PASS |
| 2. Sprint 39 contract approved | docs/sprint_objective.md is binding scope | confirmed | PASS |
| 3. Sprint 37 freeze design doc | proposal/skill_registry_design.md `status: proposal` immutable | confirmed | PASS |
| 4. PhaseEvaluator.java landmarks | INTAKE_UCS@30; UC_TEAM_NAME@112; intakeCompleteTrigger@137-148; buildIntakeSystemInstruction@207-208; SkillRegistry@262; ctor@274; plan@393; SkillRegistry-first@403-407; RESOLVE-INTAKE@417-460; RESOLVE-FAQ@463-530; composeSkillPhasePlan@548 | UC_TEAM_NAME at line 115 (3-line drift); intakeCompleteTrigger @146 (1-line drift); buildIntakeSystemInstruction @210 (3-line drift); rest match | PASS — drift within "~" tolerance |
| 5. PhasePlan.java | 146 lines; UNCHANGED in Sprint 39 | confirmed (not edited) | PASS |
| 6. AgentRunLoopImpl.java landmarks | INTAKE_COMPLETE_GUARD_REJECT_REASON@120; dispatch@317/356/413; shouldRejectXxx@628-651/690-734/745-759 | INTAKE_COMPLETE_GUARD_REJECT_REASON @120-121 (matches exactly per multi-line declaration); dispatch sites and method ranges within ±2 lines | PASS |
| 7. IntakeFieldsRegistry.java | 222 lines UNCHANGED | confirmed (not edited) | PASS |
| 8. UseCaseRegistryService.java | 174 lines UNCHANGED | confirmed (not edited) | PASS |
| 9. ContextProjectionBuilder.java | 1161 lines UNCHANGED in Sprint 39 | confirmed (not edited) | PASS |
| 10. system_prompt.txt | 101 lines UNCHANGED in Sprint 39 | confirmed (not edited) | PASS |
| 11. server/src/main/resources/skills/ | 4 Sprint 38 YAMLs; Sprint 39 ADDS 2 → 6 total | confirmed; Sprint 39 ADDED resolve_faq_grounded_answer.yaml + resolve_intake_collect_and_handover.yaml | PASS |
| 12. server/src/main/java/.../skill/ | 5 Sprint 38 Java files; Sprint 39 ADDS dispatcher + records | confirmed; Sprint 39 ADDED SkillGuardrailDispatcher.java + RejectVerdict.java + DispatchContext.java | PASS |
| 13. ResolveDispositionEvaluator.java | UNCHANGED per Sprint 11 / 11.1 / 12 frozen surface | confirmed (not edited; only invocation site moved to dispatcher) | PASS |
| 14. RecordOutcomeTool.normalizeOutcomeClass | UNCHANGED in Sprint 39 | confirmed (not edited) | PASS |
| 15. Java baseline 1034/1-inherited/0/2 | Sprint 38 close baseline | confirmed via mvn test from clean — exactly 1034/1/0/2 | PASS |

All 15 premises hold within tolerance.

## 4. Implementation walkthrough

### 4.1 Execution phase 1 — RESOLVE Skill YAMLs

Shipped 2 new YAMLs at `server/src/main/resources/skills/`:

- `resolve_faq_grounded_answer.yaml` — RESOLVE-FAQ phase Skill. `applicable_use_cases: [UC-A..UC-FP]` (7 FAQ-path UCs). `tools_required: [get_customer_context, search_knowledge, resolve_article, record_outcome, request_handover]`. `max_tool_steps: 4`. Procedure / grounding / escalation text VERBATIM from the legacy `PhaseEvaluator.java` lines 498-529 (HEAD `5787806`); template substitutes `{uc_name}` in objective. 3 guardrails in declaration order: `faq_miss_handover_requires_resolve_attempt` (Sprint 6 §G2 migration) + `premature_resolve_outcome_guard` (Sprint 11 §M1 migration) + `must_cite_source` (NEW S1 per M2 §6 #4 verbatim). State inheritance: `[customer_context, accumulated_tool_results]` inherit; `prior_use_case_carry` projection slot (Sprint 41 enforcement surface; schema-bound but not yet enforced).
- `resolve_intake_collect_and_handover.yaml` — RESOLVE-INTAKE phase Skill. `applicable_use_cases: [UC-G..UC-K]` (5 INTAKE-path UCs). `tools_required: [request_handover]` (intentionally NO `create_case_controlled` per Codex 1.8 runtime-only visibility). `max_tool_steps: 3`. Procedure text VERBATIM from the legacy `buildIntakeSystemInstruction` output with 6 template placeholders (`{uc_name}`, `{uc_id}`, `{team_name}`, `{intake_complete_trigger}`, `{intake_required_fields}`, `{case_creation_note}`). 1 guardrail: `intake_complete_required` (S2 — Sprint 7 §I2 predicate moved to declarative form). State inheritance: `[customer_context, intake_fields_partial]` inherit.

Both YAMLs validate at boot via Sprint 38-fix `SkillLoader.loadAll()` against the existing `VALID_GUARDRAIL_TYPES` + `VALID_TOOL_NAMES` + `VALID_PROJECTION_SLOTS` allowlists — NO allowlist extension needed.

### 4.2 Execution phase 2 — PhaseEvaluator integration extension

Extended `composeSkillPhasePlan(Skill, String, String)` at `PhaseEvaluator.java:389` (post-Sprint-39) to apply template substitution to `objective`, `procedure` (→ `systemInstruction`), `groundingInstruction`, `escalationPolicy` via the new private helper `substitutePlaceholders(text, activeUc)` at `PhaseEvaluator.java:436`. Six placeholders, each a single registry / map lookup keyed by the active UC; NO per-UC-pair branch logic per §1.7 + M2 §6 #1.

DELETED:
- Legacy RESOLVE-INTAKE branch at `PhaseEvaluator.java:417-461` (45 lines).
- Legacy RESOLVE-FAQ branch at `PhaseEvaluator.java:462-530` (69 lines).
- Legacy `buildIntakeSystemInstruction(uc, ucDef)` private helper at `PhaseEvaluator.java:210-250` (41 lines) — its output is now reconstructed via YAML procedure text + substitutePlaceholders.

Post-edit line count: 1529 → 1427 (102-line reduction; close to the ~120-line target after accounting for the substitutePlaceholders helper that was added).

Preserved verbatim: `INTAKE_UCS` set, `UC_TEAM_NAME` map, `INTAKE_OPENING_TEMPLATES` map, `INTAKE_ESCALATION_TRIGGER` map, `intakeCompleteTrigger(activeUc)` helper, `DEFAULT_SLA_HOURS` constant (still referenced by `vars.put("SLA_HOURS", DEFAULT_SLA_HOURS)` at line 1153 in the script-library substitution path). M2 §6 #7 fence on `INTAKE_UCS` honored — the constant is retained as registry-driven SoT even though `plan(...)` no longer references it.

### 4.3 Execution phase 3 — Sprint 6/7/11 predicate migration

REMOVED from `AgentRunLoopImpl.java`:
- `shouldRejectIncompleteIntakeHandover` (lines 628-651) — 24 lines.
- `shouldRejectFaqMissHandover` (lines 690-734) — 45 lines.
- `shouldRejectPrematureResolveOutcome` (lines 745-759) — 15 lines.
- Local helper `sessionCollected(BotSession)` — dead post-migration.
- Now-dead constants: `SEARCH_TOOL`, `RESOLVE_TOOL`, `FAQ_PATH_UCS`, `FAQ_MISS_REASON`, `S1_GUARD_REJECT_REASON`, `INTAKE_COMPLETE_GUARD_REJECT_REASON`, `PROGRESSIVE_RESOLVE_GUARD_REJECT_REASON` — canonical reject-reason labels relocated to `SkillGuardrailDispatcher` as public constants (`FAQ_MISS_REJECT_REASON`, `INTAKE_INCOMPLETE_REJECT_REASON`, `PROGRESSIVE_RESOLVE_REJECT_REASON` — same string values per byte-for-byte preservation, plus the NEW `S1_CITATION_PRESENCE_REQUIRED` for the must_cite_source predicate).
- Unused imports: `java.util.Collections`, `java.util.Set`.

REPLACED dispatch sites in `AgentRunLoopImpl.java`:
- HANDOVER_TOOL dispatch (covers intake-complete + faq-miss guardrails) — single `skillGuardrailDispatcher.checkBeforeDispatch(plan, call, ctx)` call with null-safe wrapping. Side effects preserved at the dispatch site: `persistInlineIntakeFields(session, call)` runs BEFORE the guardrail check for INTAKE UCs (Sprint 7.1 §J0 inline-fields merge).
- RECORD_OUTCOME_TOOL dispatch (covers premature-resolve + must_cite_source guardrails) — single `skillGuardrailDispatcher.checkBeforeOutcomePersist(plan, outcomeClass, ctx)` call. Side effects preserved: `session.setRecordOutcomeGuardResult("rejected:<predicateName>")` on rejection, `"allowed"` on pass-through (Sprint 12 §N0 observability).

Constructor sweep:
- `AgentRunLoopImpl` gains a 6-arg `@Autowired` constructor with `SkillGuardrailDispatcher` parameter (Spring DI path). The 5-arg constructor is preserved as a backward-compatible test path that delegates with a `null` dispatcher (guardrails disabled — appropriate for the ~16 of 21 existing AgentRunLoopImpl tests that don't exercise predicate semantics).
- 5 test files updated to use the 6-arg constructor OR add a dispatcher field invoked through helper methods:
  - `Sprint7IntakeStateTest` (6 tests rewritten to use `dispatcher.checkBeforeDispatch` via `rejectsIntakeIncomplete` helper)
  - `Sprint11ProgressiveResolveTest` (2 tests rewritten via `dispatcherRejectsPrematureResolve` helper)
  - `Sprint12RuntimeAlignmentValidationTest` (2 tests rewritten via same helper)
  - `Sprint34IntakePrefillProjectionAndGuardTest` (6 tests rewritten via `dispatcherRejectsIntakeIncomplete` helper)
  - `Sprint71PartialIntakePersistenceTest` (2 tests rewritten via same helper) — M1 functional-surface preservation
  - `AgentRunLoopS1FaqGroundedResolveGuardTest` (6 unit tests rewritten via `dispatcherRejectsFaqMiss` helper; 1 constructor updated to 6-arg with `SkillTestFixtures.productionDispatcher()` for the integration tests that exercise the dispatcher in the full run loop).

Two additional test-file updates required by the migration shape:
- `SkillLoaderTest.loadAll_productionSkills_loadsFourSkills` → renamed to `loadAll_productionSkills_loadsAllSkills` with expected count `6` (was `4`) + Sprint 39 RESOLVE-FAQ + RESOLVE-INTAKE guardrails verified.
- `PhaseEvaluatorPlanTest.plan_unknownUcInRegistry_returnsNull` — removed an unused Mockito stub (the post-Sprint-39 `plan(...)` no longer calls `useCaseRegistry.getUseCase(...)` for the RESOLVE branch since composition routes through `SkillRegistry.select(...)`).

Post-edit line count: 787 → 638 (149-line reduction).

### 4.4 Execution phase 4 — Unified `SkillGuardrailDispatcher`

NEW: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (416 lines). Spring `@Component` with `SkillRegistry`-injected. Public surface:

- `checkBeforeDispatch(PhasePlan, ToolCall, DispatchContext)` — convenience overload that resolves the active Skill via `skillRegistry.select(plan.phase(), plan.useCase())` and delegates to the Skill-form primary.
- `checkBeforeDispatch(Skill, ToolCall, DispatchContext)` — primary form per design doc §9.1.
- `checkBeforeOutcomePersist(PhasePlan, String, DispatchContext)` — convenience overload for record_outcome dispatch.
- `checkBeforeOutcomePersist(Skill, String, DispatchContext)` — primary form.

Internal: 4 typed predicate handlers (private):
- `handleFaqMissHandoverRequiresResolveAttempt` — Sprint 6 §G2 semantics preserved bit-for-bit (verified `ToolCall.toolName == request_handover`; `escalation_reason == "faq_miss_threshold_exceeded"`; `accumulated_tool_results.search_knowledge` has viable hits; `accumulated_tool_results.resolve_article` empty).
- `handleIntakeCompleteRequired` — Sprint 7 §I2 semantics preserved (verified `ToolCall.toolName == request_handover`; `escalation_reason startsWith "intake_complete_for_uc_"`; `IntakeFieldsRegistry.intakeComplete(activeUc, collected) == false`).
- `handlePrematureResolveOutcomeGuard` — Sprint 11 §M1 semantics preserved via verbatim DELEGATION to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)` (the same method the legacy `AgentRunLoopImpl.shouldRejectPrematureResolveOutcome` delegated to). `ResolveDispositionEvaluator.java` UNCHANGED — only the invocation site relocates.
- `handleMustCiteSource` — NEW S1 per M2 §6 #4 verbatim authorization. 3-boundary check: (a) Skill scope (implicit via `resolve_faq_grounded_answer.yaml` `applicable_use_cases: [UC-A..UC-FP]`); (b) outcomeClass matches `parameters.outcome_class=resolve` after canonical/RESOLVED-alias normalization; (c) user-facing message text contains the `parameters.cite_token_field=source_id` token. Does NOT judge correctness, relevance, or content quality — presence only.

Composition: walks `activeSkill.guardrails()` in declaration order; short-circuit on first reject (verified by `SkillGuardrailDispatcherTest.shortCircuit_firstGuardrailRejectWinsOnFaqResolveOutcome`).

NEW: `RejectVerdict.java` (26 lines) — record with `predicateName`, `hint`, `trace` (immutable copy on construction).
NEW: `DispatchContext.java` (45 lines) — record with `plan`, `session`, `accumulatedToolResults` (immutable copy), `lastLlmRawResponse`, `parsedUserMessage` (Optional).

### 4.5 Execution phase 5 — NEW S1 + NEW S2 bounded-scope verification

S1 `must_cite_source` 3-boundary check (per M2 §6 #4 verbatim authorization):
- Boundary (a) phase=RESOLVE_FAQ — encoded via Skill scope on `resolve_faq_grounded_answer.yaml` `applicable_use_cases: [UC-A..UC-FP]`. The dispatcher only invokes `handleMustCiteSource` when this Skill is the active Skill; UC-G..UC-K (INTAKE) route to a different Skill that does NOT declare `must_cite_source`.
- Boundary (b) tool=record_outcome with class=resolve — `handleMustCiteSource` is only invoked from `checkBeforeOutcomePersist` (called from the record_outcome dispatch site in `AgentRunLoopImpl.java`). The handler bails out on `outcomeClass != "resolve"` (also accepts legacy "RESOLVED" alias per `RecordOutcomeTool.normalizeOutcomeClass`). Verified by `SkillGuardrailDispatcherTest.mustCiteSource_doesNotFire_for_classEscalate` + `mustCiteSource_doesNotFire_for_classAbandon`.
- Boundary (c) check=source_id citation presence — handler reads `parameters.cite_token_field` (default `source_id`); checks `userMessage.contains(citeToken)`; PRESENCE only (no quality/correctness judgment). Verified by `mustCiteSource_doesNotFire_when_sourceIdPresent` + `mustCiteSource_fires_when_classResolve_andNoSourceId`.

S2 `intake_complete_required` semantic equivalence (Sprint 7 §I2 byte-for-byte): verified by the 12 migrated tests in Sprint7IntakeStateTest + Sprint34IntakePrefillProjectionAndGuardTest + Sprint71PartialIntakePersistenceTest (all preserve `assertTrue` / `assertFalse` assertions; only the invocation surface changes).

### 4.6 Execution phase 6 — 4 new test files

| file | tests | scope |
|---|---|---|
| `PhaseEvaluatorResolveSkillIntegrationTest.java` | 14 | Behavioural-equivalence golden strings for RESOLVE-FAQ × 7 UCs + RESOLVE-INTAKE × 5 UCs + 2 fallback paths. Golden strings inline. |
| `SkillGuardrailDispatcherTest.java` | 24 | 4 typed handlers × fire/no-fire scenarios + short-circuit + RejectVerdict trace immutability + DispatchContext null-safety. |
| `ResolveFaqGuardrailsTest.java` | 11 | Target / neighbor / negative for 3 RESOLVE-FAQ guardrails; includes M2 §6 #4 bounded-scope verification (S1 does NOT fire on class=escalate/abandon, does NOT fire outside resolve_faq_grounded_answer Skill scope). |
| `ResolveIntakeGuardrailsTest.java` | 11 | Target / neighbor / negative for intake_complete_required across 5 INTAKE UCs; verifies user_requested + incomplete_intake reasons pass through unchanged. |

Total: 60 new tests. All pass; baseline 1034 + 60 = 1094 (matches contract §9 hard gate target ~1094-1134).

## 5. Sprint 37 freeze fidelity

For each Sprint 37 freeze decision Sprint 39 implements, the implementation site honoring the decision:

| freeze decision | implementation site | departure (if any) + rationale |
|---|---|---|
| (e §6.2.5) RESOLVE-INTAKE Skill mapping | `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` | YAML text honors LEGACY `PhaseEvaluator.buildIntakeSystemInstruction` output (per contract OQ-S38.1 precedent + `doc_governance.md` code-ahead-of-docs) instead of the design doc's editorially-paraphrased shorter template. Editorial fold-back deferred to M2 close. |
| (e §6.2.6) RESOLVE-FAQ Skill mapping | `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` | YAML text honors LEGACY `PhaseEvaluator.java` lines 498-529 verbatim. Same fold-back note as above. |
| (e §6.4) Behavioural-equivalence test pattern | `PhaseEvaluatorResolveSkillIntegrationTest` 14 tests with golden strings inline | None. |
| (g §8.2.1) Sprint 6 §G2 `shouldRejectFaqMissHandover` → `faq_miss_handover_requires_resolve_attempt` | `SkillGuardrailDispatcher.handleFaqMissHandoverRequiresResolveAttempt` | None — predicate semantics preserved bit-for-bit. |
| (g §8.2.2) Sprint 7 §I2 `shouldRejectIncompleteIntakeHandover` → `intake_complete_required` | `SkillGuardrailDispatcher.handleIntakeCompleteRequired` | None. |
| (g §8.2.3) Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` → `premature_resolve_outcome_guard` | `SkillGuardrailDispatcher.handlePrematureResolveOutcomeGuard` | None — `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` UNCHANGED; only invocation site moves. Sprint 11 / 11.1 / 12 frozen surface preserved per `runtime_freeze_and_risk_policy.md` §1.1 #3. |
| (g §8.2.4) NEW S1 `must_cite_source` | `SkillGuardrailDispatcher.handleMustCiteSource` | Bounded per M2 §6 #4 verbatim authorization (3 boundaries). NO expansion. |
| (g §8.2.5) S2 intake-completeness | Same as (g §8.2.2) — S2 IS the Sprint 7 §I2 predicate moved | None. |
| (h §9.1) public surface `checkBeforeDispatch` + `checkBeforeOutcomePersist` | `SkillGuardrailDispatcher.java` lines 119-181 | Added convenience overloads that take `PhasePlan` instead of `Skill` (handoff §7 OQ flagged). Primary `Skill`-form per design doc §9.1 also exposed. |
| (h §9.2) short-circuit-on-first-reject | `SkillGuardrailDispatcher.checkBeforeDispatch` / `checkBeforeOutcomePersist` switch body | None — verified by `SkillGuardrailDispatcherTest.shortCircuit_firstGuardrailRejectWinsOnFaqResolveOutcome`. |
| (h §9.3) per-tool-call dispatch site routing | `AgentRunLoopImpl.java` HANDOVER_TOOL block + RECORD_OUTCOME_TOOL block | None — types route to appropriate entry point; type-mismatched guardrails are no-ops. |
| (h §9.4) trace shape + LLM-visible hint | `RejectVerdict` record + handler trace assembly | All four trace fields (`predicate_name`, `decision_outcome`, `predicate_input_data`, `reject_reason_label`) plus `skill_name` (NEW M2) present. |
| (h §9.9) C2 Tier-0 candidate DEFER | NOT edited — `runtime_freeze_and_risk_policy.md` unchanged | Sprint 39 ships the FIRST observed evidence surface (the dispatcher with short-circuit semantic). Re-evaluation deferred to Sprint 39 close OR M2 close per `feedback_constitution_discipline_vs_planning_anticipation.md`. |

## 6. §4.1 anti-hardcode self-walk on the Sprint 39 diff

Expected verdict: **`approve`**.

1. **Q1 — Keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** NO. SkillRegistry selection is exact-match-then-wildcard lookup (NOT a branch table). `composeSkillPhasePlan` template substitution is registry-driven single lookup per placeholder (NOT per-UC-pair branch). Dispatcher predicate handlers are registry-driven single-condition checks (e.g., `escalation_reason.startsWith("intake_complete_for_uc_")`, `accumulated_tool_results.search_knowledge.faq_miss == true`). The RESOLVE Skill YAMLs' `applicable_use_cases` are explicit UC enumerations — they are SCOPE declarations matched by SkillRegistry, NOT branching tables.

2. **Q2 — Tier-0 justified?** No new Tier-0. C2 + C3 R-items remain DEFER per Sprint 37 close pre-decision; Sprint 39 ships the FIRST observed evidence for C2 (dispatcher refusal non-overridability) but does not pre-elevate per `feedback_constitution_discipline_vs_planning_anticipation.md`.

3. **Q3 — Soft signal achievable?** N/A. Sprint 39 is architectural refactoring + bounded predicate landing. The migrated predicates were already Runtime-floor enforcement pre-Sprint-39 (Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1); NEW S1 is the M2 §6 #4 verbatim authorized Runtime-floor extension.

4. **Q4 — Visible-eval / trace phrasing / CaseSpec id encoded?** NO. The 2 RESOLVE Skill YAMLs carry the legacy `PhaseEvaluator.java` Java-string content (which Codex M1 / Sprint 37 / Sprint 38 already cleared of eval-text leakage). The dispatcher trace shape extends the existing Sprint 6/7/11 trace pattern (`predicate_name` + `reject_reason_label`) with the NEW `skill_name` field per design doc §9.4.

5. **Q5 — Moves semantic ownership LLM → Java?** NO. The migrated predicates were Java pre-Sprint-39 (Sprint 6/7/11 ALREADY shipped them); the migration relocates the invocation surface (from `AgentRunLoopImpl` to `SkillGuardrailDispatcher`) — no LLM ownership shift. The NEW S1 is bounded per M2 §6 #4 verbatim authorization (grounding-floor minimum-citation-presence is Runtime-owned per §1.4); NOT a shift of LLM-owned next-action / response strategy / customer-facing language.

6. **Q6 — If-else in prompt?** NO. RESOLVE Skill YAMLs' `procedure` / `grounding_instruction` / `escalation_policy` text is principle-level + template-substituted with registry-driven values; no per-UC-pair if-else in prompt text.

7. **Q7 — Tool schema / capability / PII / grounding floor preserved?** YES (preserved + S1 bounded extension). Tool schemas unchanged. `PhasePlan.allowedTools` composition unchanged (per phase Skill's `tools_required`). PII / safety floor untouched. Grounding floor EXTENDED for S1 bounded per M2 §6 #4 verbatim authorization (NOT a generic citation gate; the original `D-hard-citation-gate` deferral preserved for all other phases / UCs / tools).

8. **Q8 — Generalization coverage?** YES. Target = 2 RESOLVE phase Skills × 12 representative UCs (7 FAQ + 5 INTAKE). Neighbor = 4 Sprint 38 phase Skills (unchanged) + existing Sprint 71 / Sprint 7 / Sprint 11 / Sprint 12 / Sprint 34 / Sprint 38 tests preserved. Negative = 4 negatives on NEW S1 (class=escalate, class=abandon, source_id present, Skill scope mismatch) + 3 negatives on S2 (user_requested, incomplete_intake, faq-path UC) + 12 existing SkillLoader negative cases (carried from Sprint 38 unchanged). Shadow = N/A per M2 §5 recalibration.

9. **Q9 — Rollback / sunset?** N/A — SkillRegistry abstraction is permanent. If behavioural equivalence regresses on RESOLVE × representative UCs, `git revert <sprint-39-commit>` restores the legacy branches (the deletion is bounded to the 2 RESOLVE legacy branches; the SkillRegistry-core landing from Sprint 38 stays).

Self-walk verdict: **`approve`**.

## 7. Open questions for deliver-agent + human (handoff §7 OQ)

1. **6th template-substitution placeholder** — beyond the 5 named in contract §2.2 D-c, Sprint 39 adds `{intake_required_fields}` to preserve byte-for-byte equivalence with the legacy `buildIntakeSystemInstruction` output's `IntakeFieldsRegistry.requiredFieldsFor(uc).toString()` interpolation. The contract anticipated this via §2.2 D-e ("dev judgement"); deliver-agent + human + Codex confirm at Sprint 39 close whether this is the right shape or whether the placeholder list should be formalized at 6 in a contract update.

2. **Dispatcher convenience overloads** — Sprint 39 exposes BOTH the design doc §9.1 primary `Skill`-form (`checkBeforeDispatch(Skill, ToolCall, DispatchContext)`) AND a convenience `PhasePlan`-form (`checkBeforeDispatch(PhasePlan, ToolCall, DispatchContext)`) that looks up the active Skill via the injected `SkillRegistry`. The primary form is the design doc fidelity surface; the convenience form is the production caller surface (avoids requiring `AgentRunLoopImpl` to inject `SkillRegistry` separately). Confirm this is acceptable.

3. **`RejectVerdict` + `DispatchContext` placement** — Sprint 39 ships these as separate Java record files at `server/src/main/java/.../skill/RejectVerdict.java` + `.../skill/DispatchContext.java`. Alternative: inner records on `SkillGuardrailDispatcher`. Sprint 39 picked separate files for testability + readability. Confirm.

4. **`INTAKE_COMPLETE_GUARD_REJECT_REASON` constant relocation** — Sprint 39 moved the constant to `SkillGuardrailDispatcher.INTAKE_INCOMPLETE_REJECT_REASON` (public). The value is preserved bit-for-bit (`"intake_required_fields_missing_for_intake_complete"`). Confirm.

5. **C2 Tier-0 candidate (guardrail refusal non-overridability)** — Sprint 39 ships the dispatcher with short-circuit-on-first-reject semantics (the load-bearing claim for the C2 candidate). The FIRST observed evidence surface lands NOW; production traces of the dispatcher have not yet been observed. Per `feedback_constitution_discipline_vs_planning_anticipation.md`, Sprint 39 dev does NOT edit `runtime_freeze_and_risk_policy.md`. Deliver-agent + human re-evaluate at Sprint 39 close OR defer to M2 close.

6. **`on_fail: downgrade_reason` for `intake_complete_required`** — Sprint 39 §6 #36 default is `reject_with_hint`; the dispatcher does NOT use `downgrade_reason`. The `SkillLoader.VALID_ON_FAIL_MODES` allowlist does include `downgrade_reason` for forward-compat. Surface as deferred Sprint 39 → Sprint 40+ OQ; Sprint 39 dev did NOT find a tension that would justify switching.

7. **Sprint 38 `PhaseEvaluatorSkillIntegrationTest.resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`** — the test name is now stale (post-Sprint-39, the resolve_intake path goes through the Skill, not the legacy branch). The test continues to PASS post-migration (the `allowedTools == [request_handover]` assertion still holds via the new Skill). Sprint 39 dev did NOT edit this test (no behaviour-equivalence break); deliver-agent + human MAY rename at M2 close for clarity.

## 8. Anti-hardcode self-walk (cross-reference)

See §6 — duplicate omitted to avoid drift.

## 9. Files changed

| path | change | notes |
|---|---|---|
| `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` | NEW | 50 lines; 3 guardrails (faq_miss_handover_requires_resolve_attempt + premature_resolve_outcome_guard + must_cite_source) |
| `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` | NEW | 32 lines; 1 guardrail (intake_complete_required); 6 template-substitution placeholders |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` | NEW | 416 lines; Spring `@Component`; 4 typed handlers |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/RejectVerdict.java` | NEW | 26 lines; record with predicateName/hint/trace |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/DispatchContext.java` | NEW | 45 lines; record with plan/session/accumulatedToolResults/lastLlmRawResponse/parsedUserMessage |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | EDIT | 1529 → 1427 lines (−102); legacy RESOLVE branches deleted, buildIntakeSystemInstruction removed, composeSkillPhasePlan + new substitutePlaceholders helper added |
| `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` | EDIT | 787 → 638 lines (−149); 3 shouldRejectXxx methods deleted, 7 dead constants deleted, dispatcher injection via 6-arg constructor (5-arg preserved for backward-compat), 3 dispatch sites unified |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java` | NEW | 14 tests; golden-string behavioural equivalence per design doc §6.4 |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java` | NEW | 24 tests; dispatcher unit coverage |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveFaqGuardrailsTest.java` | NEW | 11 tests; 3 RESOLVE-FAQ guardrails target/neighbor/negative |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveIntakeGuardrailsTest.java` | NEW | 11 tests; intake_complete_required target/neighbor/negative across 5 INTAKE UCs |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` | EDIT | `productionDispatcher()` helper added |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` | EDIT | `loadAll_productionSkills_loadsAllSkills` updated to expect 6 Skills (was 4); 2 new assertions verify the RESOLVE Skills' guardrail declarations |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint7IntakeStateTest.java` | EDIT | 6 tests rewritten to use dispatcher via `rejectsIntakeIncomplete` helper |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint11ProgressiveResolveTest.java` | EDIT | 2 tests rewritten via `dispatcherRejectsPrematureResolve` helper |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint12RuntimeAlignmentValidationTest.java` | EDIT | 2 tests rewritten via same helper |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillProjectionAndGuardTest.java` | EDIT | 6 tests rewritten via `dispatcherRejectsIntakeIncomplete` helper |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint71PartialIntakePersistenceTest.java` | EDIT | 2 tests rewritten via same helper (M1 functional-surface protection) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopS1FaqGroundedResolveGuardTest.java` | EDIT | 6 unit tests rewritten via `dispatcherRejectsFaqMiss` helper; constructor updated to 6-arg with productionDispatcher (integration tests now route through dispatcher) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorPlanTest.java` | EDIT | removed unused Mockito stub on `useCaseRegistry.getUseCase("UC-X")` (no longer called by post-Sprint-39 plan()) |
| `docs/sprints/sprint-039-handoff.md` | NEW | this file |

Total: 14 files changed/created (5 NEW main; 2 NEW YAMLs; 4 NEW test files; 8 EDIT test files; 2 EDIT main; 1 NEW handoff).

## 10. Layer-classification self-walk (Sprint 39 §8 stanza)

**Target failure layer:** `prompt_projection` + `semantic_planner` + `skill_state` + Runtime-owned grounding floor + Runtime-owned capability floor per §1.4. Verified: the migration relocates the source of teaching (Java string → externalized YAML) and the source of enforcement (Java method → dispatcher handler) but does NOT shift LLM-owned decisions (next-action / response-strategy / customer-facing language) to Java.

**Tier-0 invariant:** NO new Tier-0. C2 (guardrail refusal non-overridability) + C3 (state-bus boundary enforcement) remain DEFER per Sprint 37 close pre-decision; Sprint 39 dispatcher landing provides the FIRST observed evidence surface for C2 — re-evaluation at Sprint 39 close OR M2 close. Sprint 11 / 11.1 / 12 frozen surface (Tier-0 `runtime_freeze_and_risk_policy.md` §1.1 #3) preserved bit-for-bit.

**Semantic hardcode:** None introduced. The 2 RESOLVE Skill YAML files carry the legacy `PhaseEvaluator.java` Java-string content bit-for-bit (modulo whitespace normalization and 6 template-substitution placeholders, each a single registry / map lookup keyed by the active UC). The 4 Sprint 39 dispatcher handler bodies are registry-driven single-condition checks (no per-UC-pair logic; FAQ_PATH_UCS and INTAKE_UCS scope checks become implicit via `applicable_use_cases` on the Skill). The S1 predicate's 3-boolean condition (phase + tool + citation presence) is bounded per M2 §6 #4 verbatim authorization — NOT a generic citation gate.

**Generalization coverage:** target = 2 RESOLVE phase Skills × 12 representative UCs (`PhaseEvaluatorResolveSkillIntegrationTest` 14 tests). Neighbor = 4 Sprint 38 simpler phase Skills (unchanged) + Sprint 71 / Sprint 34 / Sprint 12 / Sprint 11 / Sprint 7 / Sprint 6 §G2 existing tests all preserved + `SkillGuardrailDispatcherTest` 24 tests. Negative = S1 negatives on `class=escalate` + `class=abandon` + `source_id` present + Skill scope mismatch (`ResolveFaqGuardrailsTest`); S2 negatives on `user_requested` + `incomplete_intake` + faq-path UC + `out_of_scope` reason (`ResolveIntakeGuardrailsTest`). Shadow = N/A per M2 §5 recalibration.

## 11. §5 Eval Acceptance bars (adapted per M2 §5 recalibration)

**Hard gates:**

- ✓ 2 NEW RESOLVE Skill YAMLs load + validate at Spring boot via `SkillLoader.loadAll()` (production count is now 6).
- ✓ NEW `SkillGuardrailDispatcher` compiles + passes 24 `SkillGuardrailDispatcherTest` cases.
- ✓ PhaseEvaluator integration: `composeSkillPhasePlan` extended + legacy RESOLVE branches DELETED; `PhaseEvaluatorResolveSkillIntegrationTest` 14 tests pass golden-string assertions.
- ✓ AgentRunLoopImpl integration: 3 dispatch sites routed through dispatcher; 3 `shouldRejectXxx` methods REMOVED; all 21 existing AgentRunLoopImpl test sites either preserve behaviour via the 5-arg backward-compat constructor or are updated to the 6-arg dispatcher constructor.
- ✓ NEW S1 `must_cite_source` declared + implemented bounded per M2 §6 #4 verbatim authorization; `ResolveFaqGuardrailsTest` 11 tests cover target/neighbor/negative.
- ✓ NEW S2 `intake_complete_required` declared + implemented (semantically identical to Sprint 7 §I2); `ResolveIntakeGuardrailsTest` 11 tests cover target/neighbor/negative.
- ✓ Java baseline preserved: **1094 / 1 inherited / 0 / 2** (1034 baseline + 60 new = 1094; sole failure is the pre-existing inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`).
- ✓ `Sprint71PartialIntakePersistenceTest` 14/14 baseline preserved (M1 functional-surface protection).
- ✓ `IntakeFieldExtractor` + Sprint 32/33 projection slot tests remain green.
- ✓ Sprint 38-shipped tests (`SkillTest` 9/9 + `SkillRegistryTest` 11/11 + `SkillLoaderTest` 18/18 + `PhaseEvaluatorSkillIntegrationTest` 13/13) remain green.
- ✓ No per-UC-branch if-else in any Skill YAML body OR dispatcher Java code OR `composeSkillPhasePlan` template substitution.
- ✓ No scope creep into Sprint 40 (system_prompt.txt UNCHANGED) / Sprint 41 (ContextProjectionBuilder.java UNCHANGED; SkillStateBus.java NOT created; prior_use_case_carry NOT added as projection slot — only declared in Skill YAML per existing Sprint 38-fix allowlist).
- ✓ S1 predicate scope matches M2 §6 #4 verbatim authorization (verified by handoff §4.5 boundary check + `ResolveFaqGuardrailsTest` 4 negatives).
- ✓ Sprint 11 / 11.1 / 12 frozen surface preserved (`ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` UNCHANGED; only invocation site moved).
- ✓ Constitution-compliance: §1.3 LLM ownership preserved (Skill `procedure` is LLM-soft teaching; no Skill prescribes customer-facing language or per-step argument values beyond the registered guardrail's enforced argument scope); §1.4 Runtime ownership preserved (tool schema unchanged; capability/permission boundary unchanged via `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`; PII / safety floor unchanged; grounding floor EXTENDED for S1 per M2 §6 #4 verbatim authorization); §1.7 forbidden-list honored.
- ✓ Handoff §6 walks §4.1 anti-hardcode kernel — verdict `approve`.
- ✓ Reproducibility: every quantitative or code-citation claim in this handoff cites source path + line number per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

**Codex review (per-sub-sprint at Sprint 39 close):** deliver-agent dispatches per `iteration_governance.md` §4.3 trigger #3. Expected verdict `approve` per the self-walk.

**Observations (per M2 §5 recalibration; not gates):**

- Interactive eval smoke composite_score / pass_rate / judge dims — recorded at Sprint 39 close for tracking per §5.5 demotion; MAY regress; does NOT block.
- Bad-case suite (Alice) closure-criterion (a) result + fabrication-condition trigger count — recorded at Sprint 39 close per M2 §5 recalibration; MAY STAY FLAT (Sprint 39 ships the S1 citation-presence predicate which is the architectural mechanism for fabrication discipline; whether Alice's specific trace pattern is caught depends on whether the bot's trace hits `record_outcome(class=resolve)` without `source_id`).
- Architecture-health metrics direction (§6 governance): `new_semantic_hardcode_count` = 0; `soft_signal_conversion_count` += 2 (RESOLVE phase Skills externalized; total = 6 phase Skills + 4 predicate migrations + 1 NEW S1 + 1 NEW S2 = 12 architectural conversions post-Sprint-39); `planner_ownership_ratio` unchanged; `shadow_disagreement_rate` not measured.

## 12. Closure verdict

**Final classification: A — Clean PASS** (no fix iteration). Sprint 39 closed 2026-05-18 with Codex per-sub-sprint review verdict `decision: pass / blocking_count: 0` on `2f412b6` in a single round per `iteration_governance.md` §4.3 trigger #3 (Runtime grounding-floor surface + Runtime capability floor + multi-fence convergence at one sub-sprint).

**Codex independent verification PASSED for:**
- §2 scope discipline (21-file diff; no forbidden-surface touch; `ResolveDispositionEvaluator` / `IntakeFieldsRegistry` / `UseCaseRegistryService` / `system_prompt.txt` / `ContextProjectionBuilder` / `SkillLoader` allowlists / governance docs / proposal docs / sprint archives / milestone archives / deliver-agent-owned files / `eval_interactive/` all UNCHANGED in the dev commit).
- §3 §4.1 nine-question anti-hardcode kernel (Q1-Q9 all PASS; agrees with dev handoff §6 self-walk).
- §4 §1.7 boundary check (2 RESOLVE Skill YAML bodies legacy-verbatim; `applicable_use_cases` are scope declarations matching pre-Sprint-39 FAQ/INTAKE partition; `state_inheritance` state-dimension-keyed; guardrails in declaration order with correct types/on_fail/parameters; dispatcher bounded to 4 typed predicate types — NOT a generic rule engine; `handleMustCiteSource` matches M2 §6 #4 verbatim 3-boundary check; `handleIntakeCompleteRequired` matches Sprint 7 §I2 byte-for-byte; `handlePrematureResolveOutcomeGuard` verbatim delegation to UNCHANGED `ResolveDispositionEvaluator`; `SkillLoader` allowlists UNCHANGED from Sprint 38-fix).
- §5 hard-fence verification (Sprint 39 contract §6 37 items + M2 §6 22 items + behavioural-equivalence golden tests across 14 RESOLVE cases + Sprint 37 freeze fidelity for decisions (e §6.2.5/§6.2.6) + (g §8.2.1-§8.2.5) + (h §9.1-§9.4) + MATERIAL FINDING preservation Sprint 6/7/11 + Sprint 33 cue/slot split + premise re-verification all 15 items via `wc -l`).
- §6 schema + reproducibility checks (RESOLVE Skill YAML schemas per design doc §6.2.5/§6.2.6; dispatcher data shape; record shapes; PhaseEvaluator integration; AgentRunLoopImpl migration with 6-arg + 5-arg backward-compat constructor; test file structure 14+24+11+11=60 new tests; line-count verifications PhaseEvaluator 1427 / AgentRunLoopImpl 638 / SkillGuardrailDispatcher 416 / records 26 + 45 / YAMLs 52 + 37; cited file:line ranges reproduced).
- §7 Java validation (`mvn test -q` returned `Tests run: 1094, Failures: 1, Errors: 0, Skipped: 2` matching the expected inherited baseline; only `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` fails as the documented unchanged dirty-working-tree carry; 60 new tests pass + targeted regression spot-checks all pass).
- §8 Tier-0 candidate continuation (C1 REJECTED preserved by construction; **C2 QUALIFIED-DEFER — Codex independent verdict aligned with deliver-agent + human pre-decision: deterministic Java tests are necessary but not sufficient production evidence**; C3 QUALIFIED-DEFER; C4 + C5 N/A; no NEW candidates surfaced).
- §9 OQ independent verification (OQ-S39.1-7 all confirmed; OQ-S39.5 the load-bearing OQ on C2 elevation timing returns **DEFER** independently — alignment).

**OQ disposition at Sprint 39 close (7 OQs):**
- OQ-S39.1 (6th `{intake_required_fields}` placeholder beyond contract §2.2 D-c 5) → AGREE WITH DEV; anticipated by §2.2 D-e dev judgement clause; Codex confirmed.
- OQ-S39.2 (dispatcher `PhasePlan`-form + `Skill`-form overloads) → AGREE WITH DEV; both walk the same declaration-ordered guardrail list and preserve short-circuit semantics.
- OQ-S39.3 (`RejectVerdict` + `DispatchContext` placement as separate files) → AGREE WITH DEV; testability + readability.
- OQ-S39.4 (constant relocation from `AgentRunLoopImpl` to `SkillGuardrailDispatcher`) → AGREE WITH DEV; reject-reason values preserved bit-for-bit (`s1_resolve_required_before_faq_miss_handover`, `intake_required_fields_missing_for_intake_complete`, `progressive_resolve_record_outcome_premature`; S1 adds `s1_citation_presence_required`).
- OQ-S39.5 (C2 dispatcher Tier-0 elevation timing) → **DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md`; Codex independent verdict aligned. C2 R-item `R-skill-guardrail-non-overridability-tier-0` in `docs/action_bank.md` §5.2 stays open for M3+ revisit after Sprint 39 production trace evidence accumulates.
- OQ-S39.6 (`on_fail: downgrade_reason` not used in Sprint 39) → AGREE with deferral; no behavioural tension found; revisit at Sprint 40+ if surfaced.
- OQ-S39.7 (stale Sprint 38 test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`) → DEFER to M2 close housekeeping per `doc_governance.md` rename-at-fold-back cadence; test continues to PASS as-is.

**Tier-0 candidate disposition at Sprint 39 close:** No new Tier-0. C1 REJECTED preserved by construction (`composeSkillPhasePlan` + `ToolDispatcher.validateAgainstPlan`). C2 + C3 QUALIFIED-DEFER R-items stay open. Sprint 39 ships C2's FIRST observed evidence surface (dispatcher short-circuit semantic + 24 dispatcher unit tests); production trace evidence pending across Sprint 39/40/41 window before re-evaluation at M2 close OR M3+ per `feedback_constitution_discipline_vs_planning_anticipation.md`.

**Codex non-blocking observations (§10):**
- Handoff §9 table cites RESOLVE YAMLs as 50/32 lines and "Total: 14 files changed/created"; `wc -l` and `git diff --stat` show 52/37 YAML lines and 21 files changed. Sprint archive is immutable per `doc_governance.md`; left as-is — informational only.
- Review prompt placeholder insertion/deletion estimate (~2191/539) differed from actual `git diff --stat` (2621/536). Evidence-count correction only; no scope violation. Recorded as a `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse for future review prompt drafting.
- C2 + C3 R-items stay open in `docs/action_bank.md` for Sprint 39 close / M2 close revisit; no `runtime_freeze_and_risk_policy.md` edit.
- Any editorial divergence between Sprint 37 design doc §6.2.5/§6.2.6 templates and legacy-verbatim YAML content should be routed to M2 close fold-back (`R-skill-design-doc-template-fold-back` if not already tracked), not Sprint 39 fix iteration.

**R-item flips at Sprint 39 close** (executed by deliver-agent at sub-sprint close):
- `R-grounding-discipline-iterative-search-fabrication` → annotated as consumed-by-Sprint-39-S1-must-cite-source-declaration (M2 §7 progression).
- `D-hard-citation-gate` → annotated to acknowledge M2 §6 #4 bounded inversion is now SHIPPED in code (general gate remains deferred per the original deferral).
- `D-skill-runtime-framework` → partial-landing annotation extended to Sprint 39 (RESOLVE Skills + predicate migration + dispatcher landed; Sprint 40 + 41 named as continuation surfaces).
- C2 R-item `R-skill-guardrail-non-overridability-tier-0` STAYS OPEN; Sprint 39 dispatcher landing recorded as FIRST observed evidence surface; re-evaluation deferred to M2 close OR M3+.
- C3 R-item `R-skill-state-bus-boundary-enforcement-tier-0` STAYS OPEN (Sprint 41 enforcement surface; no Sprint 39 change).
- Sprint 39 close-action index row added to `docs/action_bank.md` §6.

Full handoff: this file. Codex archive: `docs/sprints/sprint-039-codex-review.md`. Deliver-agent close housekeeping bundle commits separately per `feedback_commit_at_end_bundles_deliver_artefacts.md`.
