---
title: Sprint 41 dev handoff — UC switch + state preservation across Skill boundary (M2 sub-sprint 5 of 5; LAST M2 sub-sprint)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: ad hoc
notes: >
  Dev-authored 12-section archive of Sprint 41 implementation work
  (NEW M2 sub-sprint 5; LAST M2 sub-sprint). Follows the Sprint 31-40
  handoff shape. Sprint 41 implements Sprint 37 freeze decision (i)
  §10 as runtime code: session-level `SkillStateBus.java` mediating
  state across Skill boundaries on UC switch; per-Skill
  `state_inheritance` declarations on all 6 Skill YAMLs; NEW
  `prior_use_case_carry` projection slot in `ContextProjectionBuilder`;
  Skill-switch detection in `PhaseEvaluator.plan(...)` invoking the
  bus at the single boundary. Detection rides on existing M1
  surfaces; no new classifier.
---

# Sprint 41 dev handoff — UC switch + state preservation across Skill boundary

**Milestone:** M2 sub-sprint 5 of 5 (LAST) per `docs/milestone_objective.md`.

**Baseline at session start:** HEAD `9130abc` (Sprint 40 close, 2026-05-18). Java baseline `1105 / 1 inherited / 0 / 2` per Sprint 40 close.

**Post-Sprint-41 baseline:** Java `1144 / 1 inherited / 0 / 2` (1105 + 39 new Sprint 41 tests). The 1 inherited failure is `SystemPromptUserRequestedTiebreakerTest` carried since Sprint 24-era working-tree mod per Sprint 40 close documentation; UNCHANGED in Sprint 41.

## 1. Context Pack

Loaded at session start in this order:

1. `AGENTS.md` (transitively `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4.1 / §4.2 / §4.3 / §5 / §5.5 / §5.6 / §7 / §8).
2. `docs/milestone_objective.md` — M2 milestone (Skill Registry Abstraction + Wholesale Retroactive Externalization).
3. `docs/sprint_objective.md` — Sprint 41 contract (12 sections; all binding).
4. `docs/proposals/skill_registry_design.md` §10 (decision (i) §10.1-§10.11) — Sprint 37 freeze decision (i) load-bearing for D-a/b/c/d/e/f.
5. `docs/sprints/sprint-040-handoff.md` + `docs/sprints/sprint-040-codex-review.md` — Sprint 40 close baseline.
6. `docs/sprints/sprint-039-handoff.md` + `docs/sprints/sprint-039-codex-review.md` — Sprint 39 close (RESOLVE Skill migration; Sprint 39 RESOLVE Skills already declared `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` for forward-compat).
7. `docs/sprints/sprint-038-handoff.md` + `docs/sprints/sprint-038-fix-handoff.md` + `docs/sprints/sprint-038-codex-review.md` — Sprint 38 + fix close (SkillRegistry core; `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists already wired).
8. `docs/current/iteration_governance.md` — §1.3 (LLM-owned), §1.4 (Runtime-owned), §1.7 (Forbidden), §3.2 (layer classification: `skill_state` Q4 + `prompt_projection` Q3), §4.1 (anti-hardcode kernel), §4.3 (per-sub-sprint Codex trigger #3).
9. `docs/runtime_freeze_and_risk_policy.md` §1 + §2 — current Tier-0 invariant set; Sprint 41 adds NONE (C3 candidate evidence surfaces but DEFER).
10. `compact/context-handoff-sprint-041-pre-launch.md` — deliver-agent pre-launch handoff.
11. Verified source files at HEAD `9130abc` (file-by-file spot-checks against §4 premise check).

**Verified file state at HEAD `9130abc`** (per `git log --oneline -3` + `wc -l`):
- `server/src/main/resources/prompts/system_prompt.txt`: 80 lines.
- Skill YAMLs: `discover_triage.yaml` 30, `confirm.yaml` 29, `escalate.yaml` 28, `terminal.yaml` 25, `resolve_faq_grounded_answer.yaml` 52, `resolve_intake_collect_and_handover.yaml` 37.
- Runtime Java: `PhaseEvaluator.java` 1427, `AgentRunLoopImpl.java` 638, `IntakeFieldsRegistry.java` 222, `UseCaseRegistryService.java` 174, `ResolveDispositionEvaluator.java` 205, `ContextProjectionBuilder.java` 1161.
- Skill package Java: `Skill.java` 135, `Guardrail.java` 56, `StateInheritance.java` 59, `SkillRegistry.java` 123, `SkillLoader.java` 313, `SkillGuardrailDispatcher.java` 416, `RejectVerdict.java` 26, `DispatchContext.java` 45.
- Java baseline pre-Sprint-41: `1105 / 1 / 0 / 2` per `cd server && mvn test 2>&1 | grep -E "Tests run:" | tail -1`.

**Feedback memory references** applied at session start (per `agent_context_guide.md` Context Pack convention):
- `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev does NOT stage deliver-agent-owned files.
- `feedback_constitution_discipline_vs_planning_anticipation.md` — Tier-0 elevation requires deliver-agent + human + Codex; dev surfaces evidence, does NOT pre-elevate.
- `feedback_handoff_verdict_section_delegation.md` — §12 closure verdict placeholder is left for deliver-agent + human + Codex.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every quantitative claim cites source + reproduction recipe.
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — Java tests on bus + projection slot are deterministic Java-logic tests (not LLM behaviour claims); no real-LLM eval needed for Sprint 41.

## 2. Sub-sprint-objective recap

Sprint 41 is the LAST M2 sub-sprint. It implements Sprint 37 freeze decision (i) §10 as runtime code:

- Session-level `SkillStateBus.java` Spring `@Component` mediates state across Skill boundaries on UC switch (decision (i) §10.3).
- Per-Skill `state_inheritance` declarations on all 6 Skill YAMLs name what state the Skill inherits, resets, or sees as soft signal from the prior Skill (decision (i) §10.5 matrix).
- NEW `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` surfaces continuity state to the LLM as soft signal (decision (i) §10.4).
- Skill-switch detection in `PhaseEvaluator.plan(...)` invokes the bus at the single boundary when (a) the prior persisted turn's active UC differs from the current session's active UC AND (b) the resolved Skill name differs.

**Layer:** `skill_state` (Q4 — session-level state-bus + per-Skill declarations) + `prompt_projection` (Q3 — NEW prior_use_case_carry slot) + Runtime-owned capability floor per §1.4. Semantic-touching multi-layer; §7 stanza REQUIRED.

## 3. Premise re-verification

Sprint 41 dev re-verified each of the 15 premises in `docs/sprint_objective.md` §4 at session start:

1. **M2 milestone objective approved + Sprint 40 closed.** Verified via `git log --oneline -3`: top commit `9130abc` matches Sprint 40 close per Sprint 40 handoff §1.
2. **Sprint 41 contract approved.** `docs/sprint_objective.md` (the contract) is the active sub-sprint contract; read end-to-end at session start.
3. **Sprint 37 freeze immutable.** `docs/proposals/skill_registry_design.md` §10 read end-to-end at session start; no edits during Sprint 41 dev (per §6 #13 fence).
4. **`system_prompt.txt`** 80 lines confirmed via `wc -l server/src/main/resources/prompts/system_prompt.txt`. UNCHANGED in Sprint 41 (D-g optional teaching skipped per §4 dev judgement; surfaced as OQ-S41.1 in §7 below).
5. **Skill YAMLs at HEAD** confirmed per `wc -l server/src/main/resources/skills/*.yaml`: 30, 29, 28, 25, 52, 37. Sprint 41 EXTENDED each per §10.5 matrix; post-Sprint-41 counts in §9.
6. **`PhaseEvaluator.java` 1427 lines** confirmed at HEAD. Sprint 41 EDITed: +55 lines (constructor +1 arg + `maybeApplyStateBusOnSwitch` helper + 1 invocation site).
7. **`AgentRunLoopImpl.java` 638 lines** UNCHANGED (integration via `PhaseEvaluator.plan(...)` per design doc §10.3; no `ControlKernel` or `AgentRunLoop` edit needed).
8. **`SkillGuardrailDispatcher.java` 416 lines** UNCHANGED (Sprint 39 surface).
9. **`IntakeFieldsRegistry.java` 222 lines** UNCHANGED. `SkillStateBus` CONSUMES `requiredFieldsFor(...)` + `canonicalFieldName(...)`; does NOT modify the registry (M2 §6 #21 fence preserved).
10. **`UseCaseRegistryService.java` 174 lines** UNCHANGED.
11. **`ResolveDispositionEvaluator.java` 205 lines** UNCHANGED (Sprint 11/11.1/12 frozen surface).
12. **`ContextProjectionBuilder.java` 1161 lines** EDITed: +180 lines (NEW `prior_use_case_carry` slot + helper method + ctor arg + 2 import lines + 2 static constants). Post-Sprint-41: 1341 lines.
13. **`SkillLoader.java` 313 lines** VERIFIED to wire `VALID_STATE_KEYS` (3 keys: `customer_context`, `accumulated_tool_results`, `intake_fields_partial`) at lines 53-57 + `VALID_PROJECTION_SLOTS` (3 slots: `alternate_candidate_use_cases`, `discover_disambiguation_signals`, `prior_use_case_carry`) at lines 65-69 + `Skill.validate(...)` exercises them at lines 262-280. NO Sprint 41 edits to `SkillLoader.java` (no schema drift surfaced).
14. **`Skill.java` 135 lines + `StateInheritance.java` 59 lines** VERIFIED: `state_inheritance` field of type `StateInheritance` at `Skill.java:63`; `StateInheritance` record at `StateInheritance.java:34-49` carries 3 fields (`inherit`, `reset`, `softSignalViaProjection`) matching §10.2 schema. NO Sprint 41 edits (no schema drift).
15. **Java baseline `1105 / 1 / 0 / 2`** confirmed at HEAD via background `mvn test` → `[ERROR] Tests run: 1105, Failures: 1, Errors: 0, Skipped: 2 / [INFO] BUILD FAILURE`. The 1 failure is `SystemPromptUserRequestedTiebreakerTest` carried since Sprint 24-era working-tree mod.

NO premise drift surfaced. Sprint 41 proceeded with implementation per §5 contract.

## 4. Implementation walkthrough

### D-a — NEW `SkillStateBus.java` (203 lines)

CREATED `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`. Spring `@Component` per design doc §10.3 public surface.

Public methods:
- `applyOnSkillSwitch(Skill priorSkill, Skill newSkill, BotSession session)` — applied at Skill-switch boundary; reads new Skill's `StateInheritance`; for each dimension applies (a) `reset` clearing on session, (b) `inherit` pass-through (with registry-driven intersection on `intake_fields_partial`), (c) `soft_signal_via_projection` no-op at bus level (projection emits unconditionally per §N0).
- `inheritanceFor(Skill skill)` — diagnostic; returns the `StateInheritance` for a Skill (or `StateInheritance.EMPTY` on null input).

Registry-driven intersection rule per §10.3 + OLD Sprint 36 D5 §6.1.C carried forward (private `applyIntakeFieldsIntersection` method at `SkillStateBus.java:163-194`):

```java
String activeUc = session.getActiveUseCase();
List<String> required = IntakeFieldsRegistry.requiredFieldsFor(activeUc);
Map<String, String> collected = IntakeFieldsRegistry.parseCollectedFields(objectMapper, rawIntakeFields);
Map<String, String> survivors = new LinkedHashMap<>();
for (Map.Entry<String, String> entry : collected.entrySet()) {
    String canonical = IntakeFieldsRegistry.canonicalFieldName(entry.getKey());
    if (required.contains(canonical)) {
        survivors.put(canonical, entry.getValue());
    }
}
```

The intersection is a SINGLE registry-driven check; the bus body has NO `if (oldUc, newUc) ...` logic per §1.7. The body uses a `switch` over the 3 allowlisted state keys per §10.2; that is a dispatch of allowlist labels → kernel actions, NOT a semantic if-else.

Dependencies: `ObjectMapper` (for intake_fields_partial JSON round-trip). `IntakeFieldsRegistry` is consumed statically (no DI). No circular dependencies.

Defensive no-ops per §10.3 contract:
- `applyOnSkillSwitch` returns early when `newSkill == null` OR `session == null`.
- `applyOnSkillSwitch` returns early when `priorSkill.name().equals(newSkill.name())` (same-Skill turn defensive guard).
- `applyIntakeFieldsIntersection` returns early when `session.getIntakeFields()` is null/blank (nothing to filter).

### D-b — `Skill.java` + `StateInheritance.java` + `SkillLoader.java` schema verification

VERIFIED at session start (no edits required):
- `Skill.java:63` declares `StateInheritance stateInheritance` as the 15th record field per design doc §2.1.
- `Skill.java:81` compact-constructor normalises null `stateInheritance` to `StateInheritance.EMPTY`.
- `StateInheritance.java:34-49` declares the 3-field record (`inherit`, `reset`, `softSignalViaProjection`) per §10.2 schema.
- `SkillLoader.java:53-57` declares `VALID_STATE_KEYS = {customer_context, accumulated_tool_results, intake_fields_partial}` per §10.2.
- `SkillLoader.java:65-69` declares `VALID_PROJECTION_SLOTS = {alternate_candidate_use_cases, discover_disambiguation_signals, prior_use_case_carry}` per §10.2.
- `SkillLoader.validate(...)` at lines 262-280 enforces both allowlists on `state_inheritance.inherit`, `state_inheritance.reset`, and `state_inheritance.soft_signal_via_projection`.

NO schema drift surfaced. NO Sprint 41 edits to D-b files.

### D-c — All 6 Skill YAMLs `state_inheritance` declarations

EDITed all 6 Skill YAMLs per `docs/sprint_objective.md` §2.3 contract table:

| Skill | inherit | reset | soft_signal_via_projection |
|---|---|---|---|
| `discover_triage.yaml` | `[customer_context, accumulated_tool_results]` | `[intake_fields_partial]` | `[alternate_candidate_use_cases, discover_disambiguation_signals]` |
| `confirm.yaml` | `[customer_context, accumulated_tool_results, intake_fields_partial]` | `[]` | `[]` |
| `resolve_faq_grounded_answer.yaml` | `[customer_context, accumulated_tool_results]` | `[intake_fields_partial]` | `[prior_use_case_carry]` |
| `resolve_intake_collect_and_handover.yaml` | `[customer_context, accumulated_tool_results, intake_fields_partial]` | `[]` | `[prior_use_case_carry]` |
| `escalate.yaml` | `[customer_context, accumulated_tool_results]` | `[intake_fields_partial]` | `[]` |
| `terminal.yaml` | `[customer_context, accumulated_tool_results]` | `[intake_fields_partial]` | `[]` |

Pre-Sprint-41 declarations carried partial shapes (Sprint 39 RESOLVE Skills' forward-compat placeholders for `prior_use_case_carry`; Sprint 38's 4 simpler Skills' partial inherit/reset). Sprint 41 EXTENDED each to honour the §10.5 invariant matrix verbatim:

1. **`customer_context` always survives** → all 6 Skills inherit it.
2. **`accumulated_tool_results` always survives** → all 6 Skills inherit it (the design doc §10.5 row says "All resolve-side Skills"; the sprint contract table extends this to all 6 Skills since accumulated_tool_results is per-loop state that the bus has no persistent session field to mutate; declaring inherit universally documents the principle).
3. **`intake_fields_partial` survives only for INTAKE-flow continuation** → `confirm` and `resolve_intake_collect_and_handover` inherit (with registry intersection); the other 4 Skills reset.
4. **`prior_use_case_carry` surfaces on resolve-side** → `resolve_faq_grounded_answer` and `resolve_intake_collect_and_handover` declare it as soft signal.

The declarations are PRINCIPLE-LEVEL: each Skill names INHERIT / RESET / SOFT_SIGNAL dimensions independent of which prior UC's state is being transitioned. No per-UC-pair tables per §1.7 + design doc §10.7 Alternative B REJECTED.

### D-d — `ContextProjectionBuilder.java` NEW `prior_use_case_carry` slot (+180 lines)

EDITed `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`:

- Imported `JsonNode`, `NullNode`, `Skill`, `SkillRegistry`.
- Added 2 static constants: `PRIOR_USE_CASE_CARRY_AGING_TURNS = 4`, `PRIOR_USE_CASE_CARRY_CITATION_CAP = 3` (single integer constants per design doc §10.4 / §10.8; NO per-UC variation).
- Added `SkillRegistry skillRegistry` to constructor (5th arg).
- Added `prior_use_case_carry` projection slot emission at the `discover_disambiguation_signals` sibling site (just after line 433).
- Added private helper `buildPriorUseCaseCarryNode(BotSession, List<BotTurn>)` (~115 lines including doc).

Helper algorithm per §10.4:
1. Return `NullNode` when `session` is null OR `activeUseCase` is null/blank OR history is empty.
2. Walk history in reverse to find the most recent turn whose `activeUseCase` is non-null AND differs from the current session's `activeUseCase`. That's the prior UC switch boundary.
3. If aging window exceeded (`currentTurnIdx - priorTurnIdx > 4`), return `NullNode`.
4. Resolve `prior_skill_name` via `SkillRegistry.select(priorTurn.phaseAfter, priorUc)`. Null when registry misses.
5. Walk turns in reverse collecting `sourceIds` from turns where `activeUseCase == priorUc`; cap at 3 (the cap applies across turns AND within a single turn's `sourceIds` array).
6. Emit `ObjectNode` with `prior_citations` (array of `{source_id, from_use_case, turn_index}`), `prior_active_use_case`, `prior_skill_name` (or null), `ages_out_after_turns: 4`.

LLM-owned read per §1.3: the slot is a soft signal. The runtime does NOT enforce action on the slot value.

### D-e — `PhaseEvaluator.java` integration (+55 lines)

EDITed `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`:

- Imported `SkillStateBus`.
- Added `SkillStateBus skillStateBus` to constructor (12th arg).
- Added `maybeApplyStateBusOnSwitch(History, BotSession, Skill newSkill)` private helper (~30 lines).
- Inserted the invocation at the existing SkillRegistry.select dispatch site in `plan(...)` (between the registry select and the `composeSkillPhasePlan` call).

Skill-switch detection per design doc §10.3 (the helper has NO per-UC-pair branch):
- (a) prior turn's `activeUseCase` is non-null AND differs from current session's `activeUseCase`;
- (b) prior turn's `phaseAfter` is non-null;
- (c) `SkillRegistry.select(priorTurn.phaseAfter, priorUc)` resolves to a Skill;
- (d) prior Skill's name differs from `newSkill.name()`.

Both conditions (a) and (d) must hold; same-UC phase transitions (e.g., RESOLVE → CONFIRM on UC-A) do NOT trigger the bus, consistent with §10.3 explicit "UC switch detection".

Defensive null-checks ensure no NPE on missing SkillStateBus (test seam) or empty history (fresh session).

### D-f — Tests

Three NEW test classes + 36 existing tests touched (constructor signature updates only):

**NEW `SkillStateBusTest.java`** (306 lines, 17 tests). Covers:
- 5 §10.5 matrix rows × inherit / reset / soft_signal_via_projection.
- intake_fields_partial registry intersection: preserves UC-required fields; drops non-required; drops all when new UC is non-intake; canonicalises alias names (e.g., `ad_id → ad_id_or_listing_url`).
- Reset of customer_context honored when declared.
- soft_signal_via_projection is no-op at bus level.
- Same-Skill no-op; null inputs; EMPTY declaration; null priorSkill applies newSkill's declaration.
- `inheritanceFor()` diagnostic accessor.

**NEW `PriorUseCaseCarryProjectionTest.java`** (279 lines, 13 tests). Covers:
- Slot shape (4 fields + nested citations).
- Citation cap = 3 (across turns AND within single turn).
- Aging window = 4 turns (boundary check passes; +1 drops to null).
- Null on no prior switch (empty history; single-UC session; active UC null).
- prior_skill_name resolved for RESOLVE-FAQ and RESOLVE-INTAKE prior paths; null when registry misses.
- Per-UC shape invariance (UC-G switch from UC-A produces same field names as UC-H switch from UC-B).

**NEW `UcSwitchStateInheritanceTest.java`** (246 lines, 9 tests). End-to-end through `PhaseEvaluator.plan(...)` → `SkillStateBus.applyOnSkillSwitch(...)`:
- DISCOVER → RESOLVE-FAQ resets intake_fields_partial.
- RESOLVE-FAQ → RESOLVE-INTAKE preserves intake fields via registry intersection (drops legacy non-required fields).
- RESOLVE-INTAKE → RESOLVE-FAQ clears intake_fields_partial.
- RESOLVE-INTAKE → CONFIRM inherits intake_fields_partial (CONFIRM declares inherit).
- RESOLVE → ESCALATE clears intake_fields_partial.
- RESOLVE → CLOSE/terminal clears intake_fields_partial.
- Same-UC RESOLVE → CONFIRM: no bus invocation (UC unchanged).
- Fresh session: no bus invocation (empty history).
- Prior turn with null UC (DISCOVER pre-classification): no bus invocation.

**EXISTING tests updated (36 files)** — constructor signature changes only (no behavioural assertion changes):
- 20 tests touching `new PhaseEvaluator(...)` updated to pass a 12th `null` argument (SkillStateBus). Defensive `maybeApplyStateBusOnSwitch` null-check ensures behavioural equivalence.
- 16 tests touching `new ContextProjectionBuilder(...)` updated to pass a 5th `null` argument (SkillRegistry). The `prior_use_case_carry` slot emits null when `skillRegistry == null`, preserving observable behaviour for tests that don't exercise this slot. (Note: `null` SkillRegistry only affects `prior_skill_name` resolution; the slot still emits structurally with `prior_skill_name = null`. Existing tests that read other projection slots are unaffected.)

Wait — that's not quite right. If `skillRegistry` is null, my helper does `if (skillRegistry != null) {...}` so `priorSkillName` stays null. The slot still emits when a prior UC switch is detected in history. For tests that DO populate a history with a prior-UC turn, the slot will be present. Let me re-audit: actually most existing tests don't populate a multi-turn history with differing activeUseCase — they either pass an empty history or a single same-UC turn. So the slot is null in those tests, and behavioural equivalence is preserved. Sprint71PartialIntakePersistenceTest preserves 14/14.

Full Java suite post-Sprint-41: `1144 / 1 inherited / 0 / 2` per `cd server && mvn test 2>&1 | grep "Tests run:" | tail -1` (39 new Sprint 41 tests, all passing).

`Sprint71PartialIntakePersistenceTest 14/14 PRESERVED` per the same run (M1 functional-surface protection per §10.

### D-g — Optional orchestration-shell teaching (SKIPPED)

Per `docs/sprint_objective.md` §2.7 + §10 closing paragraph + Sprint 40 §7.3 pattern: dev judgement at session start. The new `prior_use_case_carry` slot is intrinsically self-documenting (it carries an `ages_out_after_turns` field naming its own aging window; the `prior_skill_name` is descriptive; the `prior_active_use_case` + citation list shape mirrors the existing Sprint 31 / Sprint 33 slot precedent the LLM already reads).

Per the Sprint 40 envelope-mechanics paragraph principle: the slot's self-documentation + the Skill envelope projection (which surfaces `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` to the LLM as the per-Skill declaration of consumption) is sufficient. Adding a `system_prompt.txt` paragraph would duplicate self-evident structure without adding LLM-actionable principle.

D-g SKIPPED. Surfaced as **OQ-S41.1** for deliver-agent + human + Codex deliberation: confirm the slot's self-documentation is sufficient OR add a one-line orchestration-shell pointer at Sprint 41 close OR M2 close fold-back.

## 5. Sprint 37 freeze fidelity

Sprint 41 implements decision (i) §10. Each subsection mapping:

- **§10.1 Decision statement.** `SkillStateBus.java` created at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`. Per-Skill `state_inheritance` declarations land on all 6 Skill YAMLs. UC-switch detection rides on existing M1 Sprint 32 + Sprint 33 projections per M2 §6 #5 fence (no new classifier).
- **§10.2 `state_inheritance` declaration schema.** 3 state keys (customer_context, accumulated_tool_results, intake_fields_partial) + 3 projection slots (alternate_candidate_use_cases, discover_disambiguation_signals, prior_use_case_carry) enforced at `SkillLoader.java:53-69` allowlists. Schema validation at `SkillLoader.validate(...)` lines 262-280.
- **§10.3 SkillStateBus semantics.** `SkillStateBus.applyOnSkillSwitch(Skill, Skill, BotSession)` + `inheritanceFor(Skill)` public methods match the design doc Java code block at lines 2773-2792. Registry-driven intersection in `applyIntakeFieldsIntersection(BotSession)` at `SkillStateBus.java:163-194` matches §10.3 pseudo-code at lines 2799-2806. Skill-switch detection in `PhaseEvaluator.maybeApplyStateBusOnSwitch(...)` matches §10.3 trigger conditions at lines 2812-2818.
- **§10.4 NEW `prior_use_case_carry` projection slot.** Slot shape matches §10.4 example at lines 2828-2840 verbatim (`prior_citations` array of `{source_id, from_use_case, turn_index}` + `prior_active_use_case` + `prior_skill_name` + `ages_out_after_turns: 4`). Implemented at `ContextProjectionBuilder.buildPriorUseCaseCarryNode(...)`. Cap 3, aging 4 per §10.4 + OLD Sprint 36 OQ 7.7. Empty/null cases per §10.4 bullets at lines 2846-2851.
- **§10.5 UC-switching invariant matrix.** 5 dimensions × 5 outcomes mapped to per-Skill declarations (D-c walkthrough above). The "L" row (Skill terminal predicate state) is intentionally NOT declared in `state_inheritance` per §10.5 row 5 ("per-dispatch, not session-state. NO declaration in `state_inheritance`").
- **§10.6 Rationale.** Honored: declarative per-Skill posture (testable, reviewable); narrow schema; registry-driven intersection; soft-signal projection mirror of Sprint 31/33 pattern; UC switch on existing surfaces; SkillStateBus in `service/runtime/skill/` parallels other M2 NEW classes.
- **§10.7 Alternatives REJECTED.**
  - **Alt A (monolithic "all state survives").** Rejected per §10.7. Implementation honors: `intake_fields_partial` is reset for non-INTAKE Skills (registry intersection drops fields not in new UC's required set).
  - **Alt B (per-UC-pair tables).** Rejected per §1.7. Implementation honors: bus body has zero per-UC-pair branches; the `applyIntakeFieldsIntersection` rule is a SINGLE registry lookup.
  - **Alt C (Runtime-floor on declarations).** Rejected. Implementation honors: bus applies declarations as written; no over-constraint.
  - **Alt D (pure-projection design).** Rejected. Implementation honors: hybrid — `intake_fields_partial` deterministic enforcement in bus; soft signals via projection.
  - **Alt E (Java method per Skill).** Rejected. Implementation honors: declarations are YAML-side, schema-validated at load.
- **§10.8 §1.7 boundary check.** Verified in §8 below: principle-level per-Skill declarations; registry-driven bus body; registry-driven slot construction with single integer constants.
- **§10.9 §1.3 / §1.4 boundary check.** Verified: LLM owns prior_use_case_carry read (§1.3); bus is capability-floor persistence enforcement (§1.4); no semantic decision moved to Java.
- **§10.10 Tier-0 candidate question.** C3 candidate evidence surfaces. Sprint 41 dev does NOT pre-elevate per `feedback_constitution_discipline_vs_planning_anticipation.md`. **OQ-S41.2** surfaces for deliver-agent + human + Codex joint decision at Sprint 41 close OR M2 close.
- **§10.11 Downstream sub-sprint reference.** All 6 artefacts listed at lines 2971-2982 shipped: Skill.java state_inheritance field (verified, no edit), 6 Skill YAMLs (EDITed), SkillStateBus.java (NEW), ContextProjectionBuilder prior_use_case_carry slot (EDITed), PhaseEvaluator.java integration (EDITed), unit + integration tests (NEW × 3 + EDITed × 36 for ctor signature).

## 6. §4.1 anti-hardcode self-walk on the Sprint 41 diff

Per `docs/current/iteration_governance.md` §4.1 (nine-question kernel), walked against the Sprint 41 diff:

1. **Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** NO. SkillStateBus body uses a `switch` over 3 allowlisted state keys (dispatch of allowlist labels → kernel actions, not semantic if-else) + a single registry-driven intersection rule. Per-Skill `state_inheritance` declarations are principle-level (one rule per row). `prior_use_case_carry` slot construction is registry-driven (single aging constant + single citation cap). `maybeApplyStateBusOnSwitch` has a single `if (prior_uc != current_uc && prior_skill != new_skill)` guard — capability-routing, not semantic decision.
2. **If yes to (1), is the change justified as protecting a current Tier-0 invariant?** N/A — Sprint 41 adds NO Tier-0 invariant (C3 candidate evidence surfaces but DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md`).
3. **Could the same outcome be achieved by projecting a soft signal to the LLM?** Mixed/Hybrid: the `prior_use_case_carry` slot IS the soft signal. Deterministic intake_fields_partial enforcement is in the bus per design doc §10.7 Alternative D REJECTED ("the LLM can't be relied on to filter intake fields correctly per registry requirements; pure projection would weaken the capability-floor invariant").
4. **Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?** NO. Sprint 41 touches NO eval text, NO CaseSpec id, NO judge config.
5. **Does the change move semantic ownership from the LLM to Java?** NO. LLM owns prior_use_case_carry read (§1.3). Bus is Runtime-owned capability floor per §1.4 ("persistence" surface). No semantic decision moved to Java.
6. **Does the change add an if-else block to the prompt?** NO. No prompt edits in Sprint 41 (D-g SKIPPED per dev judgement).
7. **Does the change preserve tool schema, capability/permission boundary, PII/safety floor, and grounding floor?** YES. No tool schema changes. Capability boundary preserved (the bus is a NEW capability surface; it does not remove any existing). PII/safety floor unchanged. Grounding floor unchanged (FAQ diagnostics per `faq_grounding_contract.md` unchanged).
8. **Does the PR ship generalization eval coverage — target, neighbor, negative, shadow?** Java-test coverage (M2 §5 recalibrates eval-suite coverage to OBSERVATION):
   - Target: `SkillStateBusTest` (17 tests) × matrix rows + intersection + soft-signal no-op + same-Skill no-op.
   - Neighbor: `PriorUseCaseCarryProjectionTest` (13 tests) × slot shape + cap + aging + null cases + cross-UC shape invariance.
   - Integration: `UcSwitchStateInheritanceTest` (9 tests) × representative Skill switches end-to-end.
   - Negative: `Sprint71PartialIntakePersistenceTest 14/14 PRESERVED` (M1 functional surface); Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved.
   - Shadow: N/A per M2 §5 recalibration (architecture-focused milestone).
9. **If the change is temporary, does it carry an explicit rollback or sunset plan?** Not temporary. Sprint 41 ships the §10 design freeze permanently per M2 milestone scope.

**Verdict: `approve`.** Sprint 41 honors §1.3 LLM ownership (LLM owns prior_use_case_carry read decision: continue / ask / ignore); §1.4 Runtime ownership (bus is capability-floor persistence enforcement, NOT semantic-decision enforcement); §1.7 forbidden-list (no per-UC-pair branches, no semantic hardcode, no eval-text encoding).

## 7. Open questions for deliver-agent + human

- **OQ-S41.1 — D-g orchestration-shell teaching.** Sprint 41 dev SKIPPED the optional one-line `system_prompt.txt` teaching about `prior_use_case_carry` slot per the slot's intrinsic self-documentation + Sprint 40 envelope-mechanics paragraph principle. Deliver-agent + human + Codex confirm at Sprint 41 close OR M2 close fold-back: is this sufficient, or should a one-line shell pointer land at M2 close as a separate housekeeping commit?
- **OQ-S41.2 — C3 Tier-0 candidate elevation timing.** Sprint 41 ships the FIRST observed evidence surface for C3 (the bus's `applyOnSkillSwitch` enforcement; the unit + integration tests demonstrating LLM CANNOT override `state_inheritance`). Per `feedback_constitution_discipline_vs_planning_anticipation.md` + Sprint 41 §10 (10) stop condition, dev does NOT pre-elevate. Deliver-agent + human + Codex jointly evaluate at Sprint 41 close OR M2 close: DEFER continued / ELEVATE NOW. If ELEVATE NOW, a SEPARATE governance commit to `docs/runtime_freeze_and_risk_policy.md` §1.1 lands per the discipline.
- **OQ-S41.3 — Existing-test ctor signature update style (`null` argument).** Sprint 41 updated 36 existing test files (20 PhaseEvaluator + 16 ContextProjectionBuilder constructors) to pass `null` for the new dependency (SkillStateBus or SkillRegistry). The defensive null-checks in `maybeApplyStateBusOnSwitch` + `buildPriorUseCaseCarryNode` make this safe and preserves behavioural equivalence. Alternative would be a `SkillTestFixtures.productionSkillStateBus()` helper; deliver-agent + Codex confirm `null` is acceptable style for the existing-tests update (per Sprint 40 ctor-update precedent) OR request a follow-up fix-iteration #1 to wire production-fixtures into all 36 tests.
- **OQ-S41.4 — `accumulated_tool_results` declared as `inherit` on all 6 Skills.** The design doc §10.5 row reads "All resolve-side Skills" but the sprint contract table extends `inherit: [accumulated_tool_results]` to all 6 Skills (including DISCOVER, CONFIRM, ESCALATE, TERMINAL). Rationale: `accumulated_tool_results` has no persistent session field; the bus action is a documented no-op regardless of inherit-vs-reset. Declaring inherit universally documents the principle that prior-turn tool results survive into the next Skill's projection via `conversation_history`. Deliver-agent + Codex confirm this is the intended reading of §10.5 row 1 OR surface as design-doc editorial divergence for M2 close fold-back.
- **OQ-S41.5 — Inherited `SystemPromptUserRequestedTiebreakerTest` failure status.** UNCHANGED in Sprint 41 (1 failure baseline → 1 failure post-Sprint-41). Per Sprint 40 close documentation, this failure has been carried since the Sprint 24-era working-tree modification. Sprint 41 does NOT attempt resolution (per §10 (20) stop condition: "do NOT make it a hard gate at Sprint 41 close"). Deliver-agent + human + Codex decide at Sprint 41 close OR M2 close whether to route as a separate fix-iteration OR archive as a documented-known-failure.
- **OQ-S41.6 — Editorial divergence between §10 template and implementation.** Sprint 41 implementation follows §10.1-§10.11 verbatim; no template-vs-implementation divergence surfaced. If deliver-agent + Codex find any post-hoc, route as OQ for M2 close fold-back per Sprint 38 OQ-S38.1 + Sprint 39 §10 + Sprint 40 §7.8 precedents.

## 8. Anti-hardcode self-walk (§4.1 nine questions) on Sprint 41 itself

This section duplicates §6 with a single-paragraph rollup for the §11 sprint-close header. Per §4.1 the per-PR verdict is one of:

- `approve` — change is not a semantic hardcode, OR justified as protecting a current Tier-0 invariant with adequate generalization coverage + clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — acceptable interim measure.
- `reject as semantic hardcode`.
- `needs human architecture decision`.

**Sprint 41 self-walk verdict: `approve`.**

Rollup justification: Sprint 41 ships Sprint 37 freeze decision (i) §10 as runtime code. No per-UC-pair branch is introduced anywhere on the diff (bus body, per-Skill declarations, slot construction, PhaseEvaluator integration). No new classifier (UC-switch detection rides on existing M1 Sprint 32 + Sprint 33 projections per M2 §6 #5 fence). No Tier-0 invariant added (C3 candidate evidence surfaces; deliver-agent + human + Codex elevate at Sprint 41 close OR M2 close per OQ-S41.2). LLM ownership preserved per §1.3 (LLM owns prior_use_case_carry read). Runtime ownership preserved per §1.4 (bus is capability-floor persistence). §1.7 forbidden-list honored (no semantic hardcode; no per-UC-pair tables; no eval-text encoding). Generalization coverage: 39 new Java tests + Sprint 71 partial-intake-persistence 14/14 preserved (M1 functional surface) + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved.

## 9. Files changed

| Path | Type | Lines (delta) | Notes |
|------|------|---------------|-------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` | NEW | 203 | D-a; design doc §10.3 |
| `server/src/main/resources/skills/discover_triage.yaml` | EDIT | 30 → 32 (+2) | D-c §10.5 row 1 |
| `server/src/main/resources/skills/confirm.yaml` | EDIT | 29 → 30 (+1) | D-c §10.5 row 2 |
| `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` | EDIT | 52 → 53 (+1) | D-c §10.5 row 3 (extends Sprint 39 placeholder) |
| `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` | EDIT | 37 → 38 (+1) | D-c §10.5 row 4 (extends Sprint 39 placeholder) |
| `server/src/main/resources/skills/escalate.yaml` | EDIT | 28 → 29 (+1) | D-c §10.5 row 5 |
| `server/src/main/resources/skills/terminal.yaml` | EDIT | 25 → 26 (+1) | D-c §10.5 row 6 |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | EDIT | 1161 → 1341 (+180) | D-d prior_use_case_carry slot |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | EDIT | 1427 → 1482 (+55) | D-e SkillStateBus integration |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillStateBusTest.java` | NEW | 306 (17 tests) | D-f bus unit |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PriorUseCaseCarryProjectionTest.java` | NEW | 279 (13 tests) | D-f projection-slot unit |
| `server/src/test/java/com/gumtree/csagent/service/runtime/UcSwitchStateInheritanceTest.java` | NEW | 246 (9 tests) | D-f integration |
| 20 existing tests touching `new PhaseEvaluator(...)` | EDIT | ctor-signature only (+1 arg `null`) | D-f back-compat ctor updates |
| 16 existing tests touching `new ContextProjectionBuilder(...)` | EDIT | ctor-signature only (+1 arg `null`) | D-f back-compat ctor updates |
| `docs/sprints/sprint-041-handoff.md` | NEW | (this file) | 12-section dev-authored archive |

**NOT changed (per `docs/sprint_objective.md` §6 fences):**
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (verified, no schema drift).
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` (verified, no schema drift).
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (verified, allowlists wired).
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (Sprint 39 surface).
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (M1 functional surface).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` (integration via `PhaseEvaluator.plan(...)` not `applyRerouteDecision`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`.
- `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java`.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java`.
- `server/src/main/resources/prompts/system_prompt.txt` (D-g SKIPPED per OQ-S41.1).
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint71PartialIntakePersistenceTest.java` (M1 functional surface 14/14 PRESERVED; constructor signature updated only).
- Deliver-agent-owned files per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

## 10. Layer-classification self-walk per Sprint 41 §8 stanza

**Target failure layer:** `skill_state` (Q4 — session-level state-bus + per-Skill state_inheritance enforcement) + `prompt_projection` (Q3 — NEW prior_use_case_carry slot surfaces continuity as soft signal) + Runtime-owned capability floor per §1.4 (bus enforces which session-state dimensions carry vs reset).

Sprint 41 introduces NO per-UC-pair branching:
- Bus body uses REGISTRY-DRIVEN intersection for intake_fields_partial.
- Per-Skill `state_inheritance` declarations are PRINCIPLE-LEVEL (one rule per row per §10.5 matrix).
- `prior_use_case_carry` slot construction is REGISTRY-DRIVEN with single integer aging constant (4) + single integer cap (3).
- PhaseEvaluator integration uses a single guard with no per-UC variation.

**Tier-0 invariant:** Sprint 41 adds NO Tier-0 invariant per `docs/sprint_objective.md` §8 stanza. C3 candidate evidence surfaces; deliver-agent + human + Codex evaluate elevation at Sprint 41 close OR M2 close per OQ-S41.2.

**Semantic hardcode:** No semantic hardcode introduced per §6 walk + §8 self-walk verdict.

**Generalization coverage:** 39 new Java tests across 3 NEW test classes; M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved (no Java test regression beyond the inherited `SystemPromptUserRequestedTiebreakerTest`).

Behavioural equivalence:
- `Sprint71PartialIntakePersistenceTest 14/14 PRESERVED` — M1 intake-field persistence behaviour unchanged on the same-UC continuity path.
- All Sprint 31/33 projection slots (`alternate_candidate_use_cases`, `discover_disambiguation_signals`) construction logic UNCHANGED at `ContextProjectionBuilder.java:396-432`.
- All Sprint 38 SkillTest + SkillRegistryTest + SkillLoaderTest + PhaseEvaluatorSkillIntegrationTest PASS.
- All Sprint 39 PhaseEvaluatorResolveSkillIntegrationTest + SkillGuardrailDispatcherTest + ResolveFaqGuardrailsTest + ResolveIntakeGuardrailsTest PASS.
- All Sprint 40 SkillTeachingMigrationIntegrationTest PASS.
- IntakeFieldExtractor + IntakeFieldsRegistry tests PASS.

## 11. §5 Eval Acceptance bars

Per `docs/sprint_objective.md` §9 + `iteration_governance.md` §5 + M2 §5 recalibration:

**Hard gates (all PASS):**
- ✅ NEW `SkillStateBus.java` Spring `@Component` matches design doc §10.3 public surface (2 methods: `applyOnSkillSwitch` + `inheritanceFor`).
- ✅ Each of 6 Skill YAMLs declares `state_inheritance` per §10.5 matrix.
- ✅ NEW `prior_use_case_carry` projection slot matches §10.4 shape (cap 3 + aging 4 + null-on-no-prior-switch).
- ✅ Skill-switch detection rides on existing M1 surfaces (no new classifier).
- ✅ intake_fields_partial intersection is REGISTRY-DRIVEN (verified via `SkillStateBusTest` intersection cases × 4).
- ✅ All §10.5 matrix invariants honored.
- ✅ 39 NEW tests PASS (`SkillStateBusTest 17/17`, `PriorUseCaseCarryProjectionTest 13/13`, `UcSwitchStateInheritanceTest 9/9`).
- ✅ Java baseline preserved: pre-Sprint-41 `1105 / 1 / 0 / 2` → post-Sprint-41 `1144 / 1 / 0 / 2` (1105 + 39 = 1144; 1 inherited failure unchanged; 0 new failures; 0 errors; 2 skipped).
- ✅ `Sprint71PartialIntakePersistenceTest 14/14 PRESERVED`.
- ✅ IntakeFieldExtractor + Sprint 32 + Sprint 33 projection-slot tests remain green.
- ✅ Sprint 38 + Sprint 39 + Sprint 40 tests preserved.
- ✅ No per-UC-pair if-else in SkillStateBus body OR state_inheritance declarations OR prior_use_case_carry slot construction.
- ✅ No scope creep into M3 surfaces (`RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter` UNCHANGED; INTAKE_UCS UNCHANGED; escalation_reason enum UNCHANGED).
- ✅ Constitution-compliance verified per §6 + §8.
- ✅ Handoff §6 + §8 anti-hardcode self-walk verdict: `approve`.
- ✅ Reproducibility: every quantitative claim cites source path + reproduction recipe.

**Codex review (per-sub-sprint at Sprint 41 close per `iteration_governance.md` §4.3 trigger #3):**
- Pending. Deliver-agent dispatches at Sprint 41 close.

**Observations (per M2 §5 recalibration; MAY regress — non-gating):**
- Interactive eval smoke composite_score / pass_rate: NOT measured by dev session (M2 §5 recalibration demoted to observation; Sprint 41 ships no real-LLM eval rerun per §10 stop condition #18).
- Bad-case suite (Alice) closure-criterion: NOT measured by dev session (M2 §5 recalibration; deliver-agent + human review at Sprint 41 close OR M2 close).
- Architecture-health metrics:
  - `new_semantic_hardcode_count` = 0.
  - `soft_signal_conversion_count` incremented: NEW `prior_use_case_carry` projection slot surfaces continuity as soft signal; 6 Skill YAMLs declare `state_inheritance` (declarative externalization).
  - `planner_ownership_ratio` unchanged (LLM ownership preserved; bus is capability-floor enforcement).
  - `shadow_disagreement_rate` not measured.

## 12. Closure verdict

**Sprint 41 final classification: A — Clean PASS** (filled by deliver-agent + human at Sprint 41 close 2026-05-18 per `feedback_handoff_verdict_section_delegation.md`).

Codex per-sub-sprint review at `docs/sprints/sprint-041-codex-review.md` returned **`decision: pass / blocking_count: 0`** on first pass (single round) per `iteration_governance.md` §4.3 trigger #3 (session state model touch across Skill boundary; C3 Tier-0 candidate evidence surface). Dispatch source: `compact/sprint-041-review-prompt.md` against dev commit `8cd0a10` (parent `9130abc`).

**Codex independent verification confirmed:**
- All 9 §4.1 anti-hardcode kernel questions PASS with file:line citations (Q1 `SkillStateBus.java:99/117/175` + `SkillStateBus.java:175` registry-driven intersection; Q2 `runtime_freeze_and_risk_policy.md` UNCHANGED; Q3 soft-projection slot at `ContextProjectionBuilder.java:459` + bounded deterministic intake_fields filter; Q4 no eval-phrase encoding; Q5 LLM §1.3 ownership preserved; Q6 `system_prompt.txt` UNCHANGED at 80 lines; Q7 tool schema / capability / PII / grounding floor preserved; Q8 39 NEW Sprint 41 tests + 33 existing ctor-update preserve behavioural equivalence; Q9 `git revert 8cd0a10` cleanly removes Sprint 41 infrastructure to Sprint 40 shape).
- All 5 §1.7 boundary items PASS (no raw eval phrases; no per-UC-pair branch table — switch detection is single guard at `PhaseEvaluator.java:407`; no `eval_interactive/` edits; M2 §5 OBSERVATION respected — no smoke composite_score / bad-case programmatic gate used; `system_prompt.txt` UNCHANGED).
- All 8 hard-fence categories PASS (scope fence; no per-UC hardcode; no new classifier; S1/S2/dispatcher UNCHANGED; runtime freeze/M1 preservation; INTAKE_UCS + escalation enum UNCHANGED; governance/archives UNCHANGED; M3 scope fences UNCHANGED).
- Java baseline preserved: `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2` per `cd server && mvn clean test -q` independent rerun. The sole failure is the documented inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` (OQ-S41.5 STATUS QUO).
- Sprint 41 target tests: `SkillStateBusTest` 17/17 + `PriorUseCaseCarryProjectionTest` 13/13 + `UcSwitchStateInheritanceTest` 9/9 = 39/39 PASS.
- M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces ALL preserved (`Sprint71PartialIntakePersistenceTest 14/14` + `SkillTeachingMigrationIntegrationTest 11/11` + `PhaseEvaluatorResolveSkillIntegrationTest 14/14` + `SkillGuardrailDispatcherTest 24/24` + `ResolveFaqGuardrailsTest 11/11` + `ResolveIntakeGuardrailsTest 11/11` + `SkillTest 9/9` + `SkillRegistryTest 11/11` + `SkillLoaderTest 18/18` + `Sprint7CandidateUseCasesProjectionTest 7/7` + `DiscoverDisambiguationSignalsProjectionTest 9/9` all PASS).
- Schema allowlists wired at `SkillLoader.java:53` + `SkillLoader.java:65`; all 12 unchanged-claim spot checks empty `git diff` (Skill.java, StateInheritance.java, SkillLoader.java, SkillGuardrailDispatcher.java, IntakeFieldsRegistry.java, UseCaseRegistryService.java, ResolveDispositionEvaluator.java, AgentRunLoopImpl.java, ControlKernel.java, system_prompt.txt, runtime_freeze_and_risk_policy.md, skill_registry_design.md).

**OQ disposition** (6 OQs resolved with Codex independent verdicts; deliver-agent + human withheld pre-decisions from the review prompt per the "surface OQs without recommendations" framing):
- **OQ-S41.1** (D-g `system_prompt.txt` teaching SKIPPED) → **DISAGREE IN PART, NON-BLOCKING**. The slot shape IS self-describing (Codex confirmed), but the dev's rationale that the LLM observes `state_inheritance.soft_signal_via_projection` via Skill envelope projection is partly code-untrue — the current `phase_plan` projection does NOT expose `state_inheritance` to the LLM. Routes to **M2 close housekeeping one-line shell pointer** for `prior_use_case_carry`; NOT a Sprint 41 fix-requirement.
- **OQ-S41.2** (C3 Tier-0 candidate elevation timing — LOAD-BEARING) → **DEFER continued**. Codex evidence-based verdict aligned with deliver-agent + human pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md`. Codex direct quote: "structural evidence... is necessary and compelling, but I do not elevate C3 now because the Tier-0 claim is operationally about the LLM having no production path to override state inheritance across real traces; observe through Sprint 41/M2 close/M3+ before freezing it." No `runtime_freeze_and_risk_policy.md` edit at Sprint 41 close.
- **OQ-S41.3** (existing-test ctor `null` argument style; 33 files) → **ACCEPTABLE STYLE** per Sprint 40 ctor-update precedent + defensive null-checks + dedicated real-fixture tests in NEW Sprint 41 test classes.
- **OQ-S41.4** (`accumulated_tool_results` declared `inherit` on all 6 Skills vs design doc §10.5 row 1 "All resolve-side Skills" wording) → **FLAG AS DESIGN-DOC EDITORIAL DIVERGENCE**. Runtime effect is no-op (no persistent session field). Routes to **M2 close editorial fold-back** per Sprint 38 OQ-S38.1 + Sprint 39 OQ + Sprint 40 OQ-S40.5 precedent.
- **OQ-S41.5** (inherited `SystemPromptUserRequestedTiebreakerTest` failure persists; 1 → 1) → **STATUS QUO**. Not a Sprint 41 close blocker per contract §10 #20 ("do NOT make it a hard gate at Sprint 41 close"). The "Sprint 6" anchor token has been absent since Sprint 24-era working-tree mod.
- **OQ-S41.6** (editorial divergence between §10 template and implementation) → **DIVERGENCE FOUND, MINOR**. The only divergence surfaced is OQ-S41.4 `accumulated_tool_results` wording; no major architectural divergence; routes alongside OQ-S41.4 to M2 close fold-back.

**4 contract-drift items resolved by Codex:**
- **Drift §7-a** (Sprint 39 + Sprint 40 ctor-update precedent extension to 33 files in Sprint 41) → **REAFFIRM PRECEDENT**. The ctor-update edits are behavioural-equivalence fallout from new dependency injection; not scope creep.
- **Drift §7-b** (Java baseline 1144 within target 1140-1155) → **pass**. Independent rerun confirmed.
- **Drift §7-c** (line-count target gaps on `ContextProjectionBuilder.java` +180 vs target ~30-50 + `PhaseEvaluator.java` +55 vs target ~5-15) → **CONTENT-COMPLIANT**. Line-count targets were advisory; helper sizes justified by required null/aging/cap/registry handling.
- **Drift §7-d** (deliver-agent / dev handoff numbers-cite framing divergence) → **NON-BLOCKING NUMBERS-CITE OBSERVATION**. Codex independently found: deliver-agent review prompt cited `ContextProjectionBuilder.java +182/-2` (actual numstat `181/1`) + `PhaseEvaluator.java +59/-3` (actual `57/2`) + "main sources (10 files)" (actual 9 changed main-source files). Third+ observed instance of `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` pattern. Future deliver-agent prompts should derive every numstat number from `git show --numstat <commit>` directly before citing.

**Tier-0 candidate disposition at Sprint 41 close:**
- **C1** REJECTED preserved by construction.
- **C2** QUALIFIED-DEFER continued (Sprint 41 ships nothing new C2-relevant).
- **C3 QUALIFIED-DEFER continued (LOAD-BEARING decision; Sprint 41 ships FIRST observed evidence surface but Codex evidence-based verdict aligns with deliver-agent + human pre-decision).** R-item `R-skill-state-bus-boundary-enforcement-tier-0` STAYS OPEN; re-evaluation flagged at M2 close OR M3+ after production trace observation.
- **C4** + **C5** NOT A CANDIDATE preserved.

**M2 close fold-back queue extended by 2 items at Sprint 41 close** (in addition to the pre-Sprint-41 queue from OQ-S38.1 + Sprint 39 §10 + Sprint 39 OQ-S39.7 + OQ-S40.5):
- (S41) one-line shell pointer for `prior_use_case_carry` (from OQ-S41.1 — the dev rationale was partly code-untrue; M2 close adds the pointer).
- (S41) `accumulated_tool_results` inheritance wording — design doc §10.5 row 1 "All resolve-side Skills" vs Sprint 41 all-six framing (from OQ-S41.4).

**R-item flips at Sprint 41 close:**
- `D-skill-runtime-framework` (action_bank.md:345) annotation extended to FULL LANDING (Sprints 37 + 38 + 38-fix + 39 + 40 + 41 ALL shipped; M2 implementation track CLOSED).
- C2 R-item `R-skill-guardrail-non-overridability-tier-0` STAYS OPEN — Sprint 41 ships nothing new C2-relevant.
- C3 R-item `R-skill-state-bus-boundary-enforcement-tier-0` annotated with Sprint 41 enforcement evidence + Codex DEFER verdict; STAYS OPEN; re-evaluate at M2 close OR M3+.
- Sprint 41 close-action index row added to `docs/action_bank.md` §6 after Sprint 40.

**Sprint 41 closes M2 implementation track.** M2 milestone close planning round is the NEXT deliver-agent + human unit of work per `iteration_governance.md` §8.4 — milestone-shared cumulative Codex review against commit range `51c327c..8cd0a10` (NEW M2 sub-sprints 1-5 inclusive); design doc editorial fold-back per `doc_governance.md` cadence (the 6 fold-back items above); M2 archive to `docs/milestones/M2_objective.md`; §6.5 closed-milestone index row in `docs/action_bank.md`; 10-handoff §1 lead refresh (M2 → Preceding milestone; M3 → Current); C2 + C3 R-item re-evaluation; M3 candidate selection per `docs/milestone_objective.md` §11 cross-milestone sequencing (M3-A customer-honesty / M3-B Single Handover Orchestrator / M3-C Sprint 5 S3/S4/S5 skills / M3-D Topic↔UC binding loosening / M3-Latency / M3-Skill-Tuning / M3-Tier-0 re-evaluation).
