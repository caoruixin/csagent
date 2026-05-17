---
title: Sprint 38 fix iteration #1 handoff — SkillLoader schema-validation completeness for design doc §2.2 (close Codex Blocking Finding 1)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 38 fix iteration #1 closes Codex Blocking Finding 1 from
  `docs/codex-findings.md` (Codex per-sub-sprint review of Sprint 38 dev
  commit `bd9d3f5`). The finding identified 4 schema-validation sub-gaps
  in `SkillLoader` relative to Sprint 37 freeze design doc §2.2:
  (#1a) the `Skill` record's compact constructor normalized null
  `toolsRequired` to `List.of()`, making the loader's null-check
  unreachable; (#1b) `tools_required` entries were not validated against
  the canonical tool-name set; (#1c) explicit (non-wildcard)
  `applicable_use_cases` entries were not validated against the
  registered UC set; (#1d) `guardrails[].type` entries were not
  validated against the known predicate-type whitelist. The fix is
  bounded to `Skill.java` (1-line compact-constructor + Javadoc edit),
  `SkillLoader.java` (2 new constant Sets + `UseCaseRegistryService`
  constructor injection + 3 new validation surfaces), `SkillLoaderTest.java`
  (5 new test methods + test fixture refactor for the injected dep),
  `SkillTest.java` (1 adjusted assertion for the null-passthrough +
  1 new positive empty-list test), `SkillRegistryTest.java` +
  `SkillTestFixtures.java` (call-site updates for the new constructor
  signature). No other surface touched.

  Classification: B (fix-iteration sub-sprint per
  `iteration_governance.md` §4.3). Codex per-sub-sprint review at fix
  close re-verifies Finding 1 closure on this commit (fix-review surface
  only; other Sprint 38 surfaces NOT re-reviewed). §12 closure verdict
  left as placeholder per `feedback_handoff_verdict_section_delegation.md`.
---

# Sprint 38 fix iteration #1 handoff — SkillLoader schema-validation completeness for design doc §2.2

## 1. Context Pack

### 1.1 Relevant docs (read at session start)

| path | tier (best guess) | status | one-line relevance |
|------|-------------------|--------|--------------------|
| `AGENTS.md` (+ transitively `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`) | durable-connective | current | Constitution + governance chain. §1.4 Runtime-owned floor (load-time schema enforcement is Runtime-owned); §4.1 anti-hardcode kernel; §4.3 per-sub-sprint Codex triggers (fix-iteration #4 trigger). |
| `docs/codex-findings.md` (Codex per-sub-sprint review of `bd9d3f5`) | sprint-archive (pre-archive) | current (review surface for the fix) | Blocking Finding 1 paragraph + §6 Schema And Reproducibility Checks: identifies 4 sub-gaps in `SkillLoader` schema validation relative to design doc §2.2. |
| `docs/sprint_objective.md` | current-runtime | current | Sprint 38 contract: §6 31 hard fences carry forward into the fix iteration; §10 stop conditions; the fix iteration is bounded by the same fences. |
| `docs/milestone_objective.md` | current-runtime | current | NEW M2 milestone north star; §6 22 hard fences at the milestone level carry forward. |
| `docs/proposals/skill_registry_design.md` §2.1 + §2.2 + §5.2 + §8.2 | proposal (Sprint 37 freeze; immutable) | proposal | Architectural authority. §2.1 12-field Skill record; §2.2 schema-validation contract (the load-bearing source for this fix); §5.2 guardrail DSL; §8.2.1-§8.2.4 canonical predicate types (`faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome_guard`, `must_cite_source`). |
| `docs/sprints/sprint-038-handoff.md` | sprint-archive | archived | Sprint 38 dev #1 commit `bd9d3f5` archive. The fix iteration preserves all of Sprint 38's other surfaces UNCHANGED. |
| `docs/sprints/sprint-037-handoff.md` + `docs/sprints/sprint-037-codex-review.md` | sprint-archive | archived | Sprint 37 freeze context; immutable. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` | feedback memory | current | Why the fix iteration does NOT silently edit governance docs; surfaces freeze-wording tensions as §7 OQ, not as edits. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md` | feedback memory | current | Dev does NOT stage deliver-agent-owned files (`docs/codex-findings.md`, `compact/sprint-038-*.md`). |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md` | feedback memory | current | Every code citation in this handoff cites file path + line range reproducible via `wc -l` / `grep -n` / `git show <sha>:<path>`. |
| `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md` | feedback memory | current | §12 closure verdict placeholder convention. |

### 1.2 Code shape verified at session start (parent commit `bd9d3f5`)

| path | total lines (pre-fix) | landmarks re-verified |
|------|----------------------:|-----------------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` | 124 | Compact constructor lines 63-71; line 66 normalized null `toolsRequired` to `List.of()` (target of fix #1a). Record-level Javadoc lines 8-41 (target of Javadoc note update). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` | 228 | Constant Sets at 39-77 (`VALID_PHASES` / `VALID_STATE_KEYS` / `VALID_PROJECTION_SLOTS` / `VALID_ON_FAIL_MODES`); `validate(...)` at lines 147-210; existing null-check on `toolsRequired` at 155-157 (made reachable by fix #1a); existing guardrails loop at 204-209 (extended by fix #1d). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Guardrail.java` | 56 | Record enforces nonblank `type` + `onFail` at lines 38-46; UNCHANGED in fix. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` | 59 | UNCHANGED in fix. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` | 123 | UNCHANGED in fix. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` | 174 | `isKnownUseCase(String)` helper at lines 109-111 (used by fix #1c); `init()` is callable directly without Spring (used by test fixtures); UNCHANGED in fix. |
| `server/src/main/resources/config/use-case-registry.yaml` | (config) | 12 UCs registered: UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP, UC-G, UC-H, UC-I, UC-J, UC-K. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` | 245 | 13 existing tests; line 27 instance-field loader (target of test fixture refactor). |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` | 141 | 8 existing tests; lines 68-82 `skill_compactConstructor_normalizesNullListsToEmpty` asserts the OLD null-normalization behavior for `toolsRequired` (target of fix #2.5#6 adjustment). |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` | 147 | 11 existing tests; lines 24-31 `loaderReturning(...)` anonymous subclass of `SkillLoader` (target of call-site update for new constructor signature). |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` | 22 | `productionRegistry()` at line 18-20 (target of call-site update). |

### 1.3 Doc-status warnings observed

- **Tool-name allowlist source-of-truth**: The canonical tool-name set is not currently declared in a single Java enum / registry — each `Tool` implementation declares its own `getName()` return string under `server/src/main/java/com/gumtree/csagent/service/tools/`. `ToolDispatcher.java:71-74` dynamically populates a `toolRegistry` map from the Spring-injected `List<Tool>` at startup. The 11 canonical tools verified via `grep -B1 -A1 "public String getName"`: `create_case_controlled`, `record_outcome`, `get_customer_context`, `lookup_listing_or_ad`, `search_knowledge`, `resolve_article`, `lookup_customer_account`, `get_moderation_review_context`, `request_handover`, `get_message_moderation_context`, `classify_use_case`. The fix encodes this set as a static `Set<String> VALID_TOOL_NAMES` in `SkillLoader.java`; surfaced as §7 OQ-FIX1.1 for fold-back to a central source of truth.
- **Predicate-type naming**: The fix-iteration prompt §2.4 named the canonical predicate types slightly differently from design doc §5.2 + §8.2 (e.g., prompt said `faq_miss_handover_threshold`, design doc says `faq_miss_handover_requires_resolve_attempt`). The design doc is the immutable Sprint 37 freeze and is authoritative; the implementation uses the design doc names. Surfaced as §7 OQ-FIX1.2.
- **No premise drift on Sprint 38 §4 items.** All premises preserved from `bd9d3f5`.

### 1.4 Source-of-truth decision

For the schema-validation contract: source of truth is Sprint 37 design freeze `docs/proposals/skill_registry_design.md` §2.2 (immutable; `source_of_truth: this file` per its frontmatter). For canonical tool names: source of truth is the runtime `Tool` implementations under `server/src/main/java/com/gumtree/csagent/service/tools/` (code-ahead-of-docs per `doc_governance.md`; the fix-side `VALID_TOOL_NAMES` is a mirror). For registered UC IDs: source of truth is `server/src/main/resources/config/use-case-registry.yaml` via `UseCaseRegistryService.isKnownUseCase(...)`. For canonical guardrail predicate types: source of truth is Sprint 37 design doc §5.2 + §8.2.1-§8.2.4 (4 types; immutable freeze).

### 1.5 Implementation status

`implementation_status: implemented`. The fix iteration ships:

- `Skill.java`: compact constructor line 66 no longer normalizes null `toolsRequired` (1-line change + 6-line Javadoc note).
- `SkillLoader.java`: 2 new constant Sets (`VALID_TOOL_NAMES`, `VALID_GUARDRAIL_TYPES`); 1 new field (`useCaseRegistry`); constructor signature `SkillLoader(UseCaseRegistryService)` (constructor-injected); 3 new validation surfaces in `validate(...)` (explicit-UC registry check; tool-name allowlist; guardrail-type whitelist).
- `SkillLoaderTest.java`: test fixture refactored to `@BeforeAll initLoader()` that constructs a single `UseCaseRegistryService` + `SkillLoader` shared across all tests; 5 new test methods at the end of the file covering all 4 sub-gaps + 1 positive registry test.
- `SkillTest.java`: `skill_compactConstructor_normalizesNullListsToEmpty` adjusted to assert `null toolsRequired` stays null (the new behavior); 1 new positive test `skill_compactConstructor_emptyToolsRequiredIsPreserved` confirming explicit empty `tools_required: []` is preserved as non-null empty list (so Skills with no tool calls remain valid post-validation).
- `SkillRegistryTest.java` + `SkillTestFixtures.java`: call-site updates so the existing `new SkillLoader(...)` invocations supply the now-required `UseCaseRegistryService`.

## 2. Sub-sprint-objective recap

Sprint 38 fix iteration #1 closes **Codex Blocking Finding 1** (the SkillLoader schema-validation completeness gap relative to Sprint 37 freeze design doc §2.2). Quoted verbatim from `docs/codex-findings.md`:

> `SkillLoader` does not enforce the frozen Skill schema contract for required `tools_required` presence and canonical Skill-scope values. Sprint 37 freeze §2.2 requires fail-fast validation for missing required fields including `tools_required`, `applicable_use_cases` values not in the UC registry, `tools_required` values not in the canonical tool-name set, and `guardrails[].type` values not matching a known dispatcher predicate type. The Sprint 38 implementation cannot detect a missing `tools_required` field because `Skill.java:63-70` normalizes a null `toolsRequired` constructor argument to `List.of()` before `SkillLoader.validate(...)` runs, making the null check at `SkillLoader.java:152-157` unreachable. The same validator has no loop over `skill.toolsRequired()` to reject unknown tool names, no registry/allowlist check for explicit non-wildcard `applicable_use_cases`, and no known-type check for `Guardrail.type()` at `SkillLoader.java:204-208` (only `on_fail` is validated). This violates in-scope decision (a) §2.2 / Sprint 38 schema-validation acceptance and can let malformed Skill YAML boot successfully instead of failing fast.

Classification: B (fix-iteration sub-sprint per `iteration_governance.md` §4.3 trigger #4). Scope: bounded + mechanical; no scope expansion beyond the 4 sub-gaps. Layer: `skill_state` (SkillLoader is the Spring-boot fail-fast validator for the SkillRegistry data) + Runtime-owned floor per §1.4 (load-time configuration enforcement is Runtime-owned). Codex review: per-sub-sprint at fix close; re-verifies Finding 1 closure on the fix commit (fix-review surface only).

## 3. Codex Finding 1 sub-gap closure

| sub-gap | freeze §2.2 requirement | code citation closing the gap | test citation verifying closure |
|---------|-------------------------|-------------------------------|----------------------------------|
| **#1a** missing `tools_required` field detection | "Required fields missing (`name`, `description`, `applicable_phases`, `applicable_use_cases`, `tools_required`, `procedure`)." | `Skill.java:67-72` compact constructor line `toolsRequired = toolsRequired == null ? null : List.copyOf(toolsRequired);` (null passthrough; the loader's existing null-check at `SkillLoader.java:160-162` becomes reachable). | `SkillLoaderTest.parseAndValidate_missingToolsRequiredField_failsFast` — YAML omits the `tools_required` key; assertion: throws `IllegalStateException` containing "tools_required". |
| **#1b** unknown tool-name detection | "`tools_required` values not in the canonical tool name set." | `SkillLoader.java`: `VALID_TOOL_NAMES` static Set (11 canonical tool names); validation loop in `validate(...)` after the wildcard-mixing check. | `SkillLoaderTest.parseAndValidate_unknownToolName_failsFast` — YAML has `tools_required: [some_nonexistent_tool]`; assertion: throws `IllegalStateException` containing "some_nonexistent_tool". |
| **#1c** explicit unknown UC detection | "`applicable_use_cases` values not in the UC registry (with `["*"]` sentinel allowed)." | `SkillLoader.java`: `UseCaseRegistryService` constructor-injected; non-wildcard UC validation block in `validate(...)` using `useCaseRegistry.isKnownUseCase(uc)`. | `SkillLoaderTest.parseAndValidate_explicitUnknownUseCase_failsFast` + positive companion `parseAndValidate_explicitKnownUseCase_passes` — first asserts `UC-NONEXISTENT` throws; second asserts `UC-A` (real registered UC) passes. |
| **#1d** unknown guardrail-type detection | "`guardrails[]` entries whose `type` does not match a known dispatcher predicate type." | `SkillLoader.java`: `VALID_GUARDRAIL_TYPES` static Set (4 canonical types per design doc §5.2 + §8.2.1-§8.2.4: `faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome_guard`, `must_cite_source`); type check at the head of the existing guardrails loop. | `SkillLoaderTest.parseAndValidate_unknownGuardrailType_failsFast` — YAML has `guardrails: [{type: nonexistent_predicate, on_fail: reject_with_hint}]`; assertion: throws `IllegalStateException` containing "nonexistent_predicate". |

## 4. Implementation walkthrough

### 4.1 `Skill.java` (1-line compact constructor change + Javadoc note)

- The compact constructor at the record (lines 63-72 post-fix) now reads `toolsRequired = toolsRequired == null ? null : List.copyOf(toolsRequired);` (was `... : List.of() : ...`). All other List fields (`applicablePhases`, `applicableUseCases`, `requiredContextKeys`, `validTerminalOutcomes`, `guardrails`) retain their null → `List.of()` normalization because their presence semantics differ from `tools_required`:
  - `applicable_phases` + `applicable_use_cases` are checked via `requireNonEmpty(...)` in the loader (catches both null + empty).
  - `required_context_keys` may genuinely be empty per design doc §2.1.
  - `valid_terminal_outcomes` is optional per design doc §2.1.
  - `guardrails` is intentionally empty for Sprint 38's 4 simpler phase Skills.
  - Only `tools_required` is named in §2.2 as a required field whose absence must fail-fast.
- Record-level Javadoc updated (lines 26-34 post-fix) to note the null-until-validated semantics: the compact constructor intentionally does NOT normalize null `toolsRequired`; `SkillLoader.validate(...)` catches null; post-validation, `toolsRequired` is guaranteed non-null (may be empty).
- A new inline comment block above the compact constructor (lines 66-71 post-fix) names this exception explicitly so a future reader does not "fix" it back to symmetry.

### 4.2 `SkillLoader.java` (2 new constant Sets + UC-registry injection + 3 new validation surfaces)

- **New `VALID_TOOL_NAMES` constant Set** (11 canonical tool names): mirrors the `getName()` return values of every `Tool` implementation under `server/src/main/java/com/gumtree/csagent/service/tools/`. Inline Javadoc names the source-of-truth (the `Tool` implementations themselves) and the maintenance contract (a new tool added to the runtime must update this set in the same commit). Surfaced as §7 OQ-FIX1.1 for fold-back to a central declaration.
- **New `VALID_GUARDRAIL_TYPES` constant Set** (4 canonical predicate types per design doc §5.2 + §8.2.1-§8.2.4): `faq_miss_handover_requires_resolve_attempt` (Sprint 6 §G2), `intake_complete_required` (Sprint 7 §I2 + NEW S2 per §8.2.5), `premature_resolve_outcome_guard` (Sprint 11 §M1), `must_cite_source` (NEW S1 per §8.2.4).
- **`UseCaseRegistryService` constructor-injected** via the new single-arg constructor `public SkillLoader(UseCaseRegistryService useCaseRegistry)`. The `Objects.requireNonNull(...)` precondition enforces the dependency. No fallback to a stub set: the loader's job is to fail-fast against the live UC registry. Spring's bean graph orders `UseCaseRegistryService` → `SkillLoader` → `SkillRegistry` correctly because `UseCaseRegistryService` has no dependencies (its `@PostConstruct init()` reads from the classpath only). No circular dependency.
- **`validate(...)` extended at 3 sites** (in order):
  - After the wildcard-mixing check, a new `if (!hasWildcard) { for (uc : applicable_use_cases) require useCaseRegistry.isKnownUseCase(uc) }` block (sub-gap #1c).
  - Immediately after, a new `for (tool : tools_required) require VALID_TOOL_NAMES.contains(tool)` loop (sub-gap #1b). The loop is empty-safe; empty `tools_required: []` skips it; null `tools_required` is caught earlier by the existing null-check (sub-gap #1a closure path).
  - Inside the existing guardrails loop, a new `if (!VALID_GUARDRAIL_TYPES.contains(g.type()))` check (sub-gap #1d), prepended before the existing on_fail check.

### 4.3 Test fixture refactor + 5 new negative cases + 1 new positive case

- **`SkillLoaderTest.java`**: replaced instance-field `private final SkillLoader loader = new SkillLoader();` with static `@BeforeAll initLoader()` that constructs one shared `UseCaseRegistryService` + `SkillLoader`. The shared instance is safe because `SkillLoader` is stateless (each `parseAndValidate(...)` call is independent). 5 new tests appended at the end of the file (full Javadoc for each per the §2.5 prompt list). Existing 13 tests preserved unchanged in body; only their loader-field reference is updated.
- **`SkillTest.java`**: `skill_compactConstructor_normalizesNullListsToEmpty` assertion adjusted — `assertEquals(List.of(), skill.toolsRequired())` → `assertNull(skill.toolsRequired())` (with inline comment explaining the new null-passthrough semantics). New positive test `skill_compactConstructor_emptyToolsRequiredIsPreserved` confirms explicit empty `tools_required: []` is preserved as non-null empty list.
- **`SkillRegistryTest.java`**: `loaderReturning(...)` updated to pass `SkillTestFixtures.initializedUseCaseRegistry()` to the anonymous subclass's constructor. The overridden `loadAll()` bypasses validation so the registry instance is unused at runtime; the only purpose is to satisfy the constructor signature.
- **`SkillTestFixtures.java`**: `productionRegistry()` updated to pass the initialized UC registry; new helper `initializedUseCaseRegistry()` exposes the construction pattern so `SkillRegistryTest` can reuse it.

## 5. §4.1 anti-hardcode self-walk on the fix iteration diff

The PR is a fix-iteration sub-sprint touching a semantic-adjacent surface (the SkillLoader schema validator) — §4.1 applies. Walking the 9 questions:

1. **Keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** NO. The 3 new validation surfaces are RUNTIME-FLOOR ENFORCEMENT whitelists per §1.4, not LLM-semantic decision tables. The tool-name allowlist mirrors the existing `ToolDispatcher.validateAgainstPlan` runtime invariant (capability-floor). The UC-registry check mirrors the existing `UseCaseRegistryService.isKnownUseCase(...)` invariant (the `classify_use_case` tool already uses this to prevent the LLM committing an out-of-registry UC). The guardrail-type whitelist mirrors the planned `SkillGuardrailDispatcher` predicate type registry (Sprint 39 scope).
2. **Tier-0 invariant being protected?** N/A. This fix adds no Tier-0 invariant. The schema-validation surface is part of Sprint 38's freeze-fidelity contract (decision (a) §2.2), not a new Tier-0 candidate.
3. **Soft signal achievable instead?** N/A. The schema validation contract is fail-fast at Spring boot; a soft signal to the LLM about a malformed YAML at boot time would be meaningless (the bot has no boot-time LLM call). §2.2 requires fail-fast.
4. **Visible-eval / trace phrasing / CaseSpec id encoded?** NO. The constant Sets contain only canonical tool names and canonical predicate types (sourced from the Sprint 37 freeze + runtime tool classes). No CaseSpec id, no Alice / bad-case text, no trace phrasing.
5. **Semantic ownership shift LLM → Java?** NO. The LLM's surface (Skill `procedure` / `grounding_instruction` / `escalation_policy`) is unchanged. The fix only strengthens the load-time Runtime-floor validation of Skill YAML content. §1.3 LLM ownership preserved verbatim.
6. **Prompt as if-else dump?** N/A. The fix touches no prompt content; `system_prompt.txt` and the 4 Skill YAML files are unchanged.
7. **Tool schema / capability / PII / grounding floor preserved?** YES. Tool schemas unchanged (`Tool` implementations untouched); capability/permission boundary unchanged (`ToolDispatcher.validateAgainstPlan` untouched); PII/safety floor unchanged; grounding floor unchanged.
8. **Generalization coverage?** Target = the 4 sub-gaps closed (5 new negative tests + 1 positive). Neighbor = the 13 existing SkillLoaderTest cases re-verified PASS after the test fixture refactor; behavioural-equivalence preserved on the 4 migrated phases (PhaseEvaluatorSkillIntegrationTest 13/0); Sprint 38 SkillTest 9/0 (8 existing + 1 new); SkillRegistryTest 11/0. Negative = the 5 new fail-fast assertions. Shadow = N/A for a fix-iteration on a load-time validator (no LLM behaviour change).
9. **Rollback / sunset?** N/A. The fix is permanent (it closes a freeze-fidelity gap in Sprint 38's decision (a) §2.2 implementation). The 3 new whitelists' growth path is well-defined: `VALID_TOOL_NAMES` grows with each new `Tool` implementation; `VALID_GUARDRAIL_TYPES` grows with each new predicate type registered in `SkillGuardrailDispatcher` (Sprint 39+); `useCaseRegistry` reflects `use-case-registry.yaml` automatically.

Expected verdict: **`approve`**. No semantic hardcode; 3 new whitelists are Runtime-floor enforcement (§1.4); fix is freeze-fidelity (decision (a) §2.2) and scope-bounded.

## 6. §1.7 boundary check on the fix iteration

- **`VALID_TOOL_NAMES` allowlist**: encodes only canonical tool names — no per-UC restriction, no per-phase restriction, no per-CaseSpec branch. Mirrors an existing runtime invariant.
- **UC-registry check** (`useCaseRegistry.isKnownUseCase(uc)`): encodes only "is this UC registered in the system?" — no per-UC behavior branch, no per-Skill scope restriction. Mirrors the existing `classify_use_case` tool invariant.
- **`VALID_GUARDRAIL_TYPES` whitelist**: encodes only the 4 canonical predicate types frozen at Sprint 37 §5.2 + §8.2.1-§8.2.4 — no per-UC parameter, no per-Skill scope branch.
- **No prompt edit**: `system_prompt.txt` UNCHANGED; the 4 Skill YAMLs UNCHANGED.
- **No LLM-vs-Java boundary shift**: the LLM still owns Skill `procedure` content interpretation; the Runtime still owns load-time fail-fast schema validation per §1.4.

§1.7 forbidden-list honored. The fix encodes no per-UC-pair branch, no eval phrasing, no LLM-semantic decision table.

## 7. Open questions for deliver-agent + human

- **OQ-FIX1.1 (informational)**: The canonical tool-name set is mirrored as a static `Set<String> VALID_TOOL_NAMES` in `SkillLoader.java`. There is no current central declaration of the canonical tool-name set in Java (each `Tool` implementation declares its own `getName()` return). A future R-item could fold the canonical tool-name set into a single source-of-truth (e.g., a `ToolName` enum or a `ToolDispatcher.getRegisteredToolNames()` method) so the SkillLoader's mirror is maintained automatically. **Not blocking the fix.** Surfaced for deliver-agent + human consideration at Sprint 38 close / Sprint 39 planning.
- **OQ-FIX1.2 (informational, design-doc fidelity)**: The fix-iteration prompt §2.4 named the canonical predicate types with slightly different strings than the Sprint 37 freeze design doc §5.2 + §8.2 (e.g., prompt's `faq_miss_handover_threshold` vs freeze's `faq_miss_handover_requires_resolve_attempt`). The implementation uses the freeze names because the freeze is immutable per `doc_governance.md`. The prompt naming was an informal abbreviation, not a contradiction. **Not blocking.**
- **OQ-FIX1.3 (informational, Spring bean graph)**: `UseCaseRegistryService` injection via constructor was verified to not create a circular dependency (UseCaseRegistryService → SkillLoader → SkillRegistry is a linear chain; UseCaseRegistryService has no upstream deps). At application startup, `UseCaseRegistryService.@PostConstruct init()` runs first, then Spring constructs SkillLoader, then SkillRegistry. **Not blocking.**
- **OQ-FIX1.4 (informational, test ergonomics)**: The 5 new test fixtures construct + init a new `UseCaseRegistryService` instance per test class (`SkillLoaderTest`, `SkillRegistryTest`, `SkillTestFixtures`); this is a few-millisecond classpath YAML read per class. If test runtime becomes a concern, a future R-item could share one `UseCaseRegistryService` across all test classes via a static singleton. **Not blocking.**
- **No premise drift on Sprint 38 §4 items.** No new Tier-0 candidate surfaced. No §1.7 forbidden-list concern surfaced. No freeze-wording ambiguity surfaced that would require deliver-agent + human evaluation.

## 8. Anti-hardcode self-walk on the fix diff

Identical to §5 above. Expected Codex fix-review verdict: **`approve`**.

## 9. Files changed

| path | change type | line delta (approx) |
|------|-------------|---------------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` | EDIT | +14 / -3 (compact-constructor 1-line + comment + Javadoc note) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` | EDIT | +80 / -3 (imports + 2 new const Sets + ctor injection + 3 new validation surfaces) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` | EDIT | +115 / -3 (BeforeAll fixture + 5 new tests + 1 imports tweak) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` | EDIT | +19 / -3 (null-passthrough assertion + 1 new positive test) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` | EDIT | +5 / -1 (loaderReturning ctor arg + comment) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` | EDIT | +14 / -2 (initializedUseCaseRegistry helper + productionRegistry update) |
| `docs/sprints/sprint-038-fix-handoff.md` | NEW | this file |

Total: 6 code files modified + 1 new handoff. No deliver-agent-owned file edited.

## 10. Layer-classification self-walk

Layer: **`skill_state`** (SkillLoader is the Spring-boot fail-fast validator for the SkillRegistry data; its job is to refuse malformed Skill YAML at boot so the registry never holds invalid Skills) + **Runtime-owned floor per §1.4** (load-time configuration enforcement is Runtime-owned; the LLM is never reached when validation fails because Spring fails to start).

This matches the Sprint 38 contract §8 stanza target layer (`prompt_projection` + `skill_state` + Runtime-owned PhasePlan composition) — the fix iteration narrows the layer focus to `skill_state` because the prompt-projection surface (Skill content reaching the LLM) is untouched.

No Tier-0 invariant added (Sprint 38 stanza preserved). No semantic hardcode (§1.7 boundary preserved per §6). Generalization coverage: 4 sub-gaps × 5 negative tests + 1 positive test + 13 prior tests re-verified + 13 PhaseEvaluatorSkillIntegrationTest behavioural-equivalence tests re-verified.

## 11. §5 Eval Acceptance bars (adapted for fix iteration)

**Target — 4 Codex Blocking Finding 1 sub-gaps closed:**

- [x] Sub-gap #1a missing `tools_required` field detection: closed via `Skill.java` compact-constructor null-passthrough; verified by `parseAndValidate_missingToolsRequiredField_failsFast`.
- [x] Sub-gap #1b unknown tool-name detection: closed via `SkillLoader.VALID_TOOL_NAMES` + validation loop; verified by `parseAndValidate_unknownToolName_failsFast`.
- [x] Sub-gap #1c explicit unknown UC detection: closed via `UseCaseRegistryService` injection + `isKnownUseCase(...)` check; verified by `parseAndValidate_explicitUnknownUseCase_failsFast` + positive companion `parseAndValidate_explicitKnownUseCase_passes`.
- [x] Sub-gap #1d unknown guardrail-type detection: closed via `SkillLoader.VALID_GUARDRAIL_TYPES` + type check in guardrails loop; verified by `parseAndValidate_unknownGuardrailType_failsFast`.

**Neighbor — Sprint 38's other surfaces UNCHANGED:**

- [x] Sprint 38 dev #1 behavioural-equivalence preserved: `PhaseEvaluatorSkillIntegrationTest` 13/0.
- [x] Sprint 38 `SkillTest` 9/0 (8 prior + 1 new positive empty-list test); `skill_compactConstructor_normalizesNullListsToEmpty` adjusted assertion verified.
- [x] Sprint 38 `SkillRegistryTest` 11/0 (constructor call-site adjusted; semantics unchanged).
- [x] Sprint 38 `SkillLoaderTest` 18/0 (13 prior + 5 new).
- [x] M1 functional-surface preserved: `Sprint71PartialIntakePersistenceTest` 14/0.

**Negative — fail-fast assertions hold:**

- [x] 5 new negative test methods each assert a specific schema-validation error message substring.

**Java baseline:**

- Pre-fix (Sprint 38 dev #1 at `bd9d3f5`): `Tests run: 1028, Failures: 1, Errors: 0, Skipped: 2`.
- Post-fix (this commit): `Tests run: 1034, Failures: 1, Errors: 0, Skipped: 2`. Delta: +6 tests (5 new SkillLoaderTest negatives + 1 new SkillTest empty-list positive). 1 inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure preserved unchanged.

**Safety floor / grounding floor: unchanged** (no Tier-0 invariant added; no PII / safety / grounding code touched).

**Wrong-containment / over-escalation: N/A** for a fix-iteration on a load-time schema validator (no LLM behaviour change).

**Architecture-health metrics (informational):**

- `new_semantic_hardcode_count`: 0 (3 new whitelists are Runtime-floor enforcement per §1.4, not semantic hardcodes).
- `soft_signal_conversion_count`: 0 (this fix is freeze-fidelity, not a soft-signal migration).
- `planner_ownership_ratio`: unchanged (LLM ownership preserved per §1.3).
- `shadow_disagreement_rate`: N/A (no LLM behaviour change to observe).

## 12. Closure verdict placeholder

Left for deliver-agent + human + Codex per-sub-sprint fix-review per `feedback_handoff_verdict_section_delegation.md`. Sprint 38 fix iteration #1 closes after Codex re-dispatches on `compact/sprint-038-fix-review-prompt.md` and the fix-review verdict is `pass / 0`; Sprint 38 then closes B-fix-iterated and Sprint 39 contract drafting follows.
