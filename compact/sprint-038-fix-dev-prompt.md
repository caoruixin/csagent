Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

# Sprint 38 fix iteration #1 dev prompt (Claude Code session) — close Codex Blocking Finding 1: `SkillLoader` schema-validation completeness for design doc §2.2

**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**Parent commit:** `bd9d3f5` (Sprint 38 dev session 1).
**Codex review of `bd9d3f5`:** `decision: fix_required / blocking_count: 1` at `docs/codex-findings.md`; Blocking Finding 1 = `SkillLoader` does not fully implement Sprint 37 freeze §2.2 schema validation (4 sub-gaps named below).
**Classification:** B — fix-iteration sub-sprint per `iteration_governance.md` §4. The finding is in scope for Sprint 38 D-a §2.2 schema validation; fix is bounded + mechanical; no scope expansion beyond closing the 4 sub-gaps.
**Codex review:** per-sub-sprint at fix close per `iteration_governance.md` §4.3 — deliver-agent re-dispatches Codex on `compact/sprint-038-fix-review-prompt.md` after this fix commit lands.

You are dev-agent (Claude Code) for **Sprint 38 fix iteration #1**. Your task is to close Codex Blocking Finding 1 (the `SkillLoader` schema-validation completeness gap relative to Sprint 37 freeze design doc §2.2) without expanding scope into any other Sprint 38 / 39 / 40 / 41 surface.

> **Important orientation:** Sprint 38 (NEW M2 sub-sprint 2) shipped at `bd9d3f5` with a behaviourally-correct SkillRegistry core + 4 simpler phase Skills + behavioural-equivalence preserved on the 4 migrated phases. Codex's per-sub-sprint review verified all other gates PASS (scope discipline; §4.1 9-question kernel; §1.7 boundary; M2 + Sprint 38 hard fences; MATERIAL FINDING preservation; Sprint 33 cue/slot split; premise re-verification; Java baseline 1028/1-inherited/0/2; 13 behavioural-equivalence golden assertions + 12 SkillLoader negative cases pass). The ONLY blocker is the 4-sub-gap schema-validation completeness issue in `SkillLoader`. This fix iteration MUST stay narrow.

---

## 1. Read order on session start

Read these in order before doing anything else. Cite each file in your fix-handoff §1 Context Pack as you read it.

1. `AGENTS.md` (auto-loaded transitively: `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md`).
2. **`docs/codex-findings.md`** (Codex per-sub-sprint review of `bd9d3f5`) — read end-to-end. The Blocking Finding 1 paragraph + the Schema And Reproducibility Checks (§6) section are the substantive review surface for this fix iteration.
3. `docs/sprint_objective.md` — Sprint 38 contract. Re-read §2 D-a + §6 hard fences (31 items) + §10 stop conditions. The fix iteration honors all §6 fences from the original Sprint 38 contract.
4. `docs/milestone_objective.md` — NEW M2 contract (context). Especially §6 hard fences (22 items at the milestone level).
5. **`docs/proposals/skill_registry_design.md` §2.1 + §2.2 + §5.2 + §8.2** — the freeze's schema-validation contract.
   - §2.1: Skill data model 12-field record (re-read for field semantics + required vs optional).
   - **§2.2 (THE LOAD-BEARING SECTION FOR THIS FIX)**: JSON Schema validation contract — what must fail-fast at boot. Re-read with care. The Codex finding cites:
     - Missing required fields including `tools_required` MUST fail.
     - `applicable_use_cases` values not in the UC registry MUST fail (when explicit, i.e., non-wildcard).
     - `tools_required` values not in the canonical tool-name set MUST fail.
     - `guardrails[].type` values not matching a known dispatcher predicate type MUST fail.
   - §5.2: Guardrail DSL (`type` + `on_fail` + `parameters`).
   - §8.2.1-§8.2.5: planned predicate types for Sprint 39 — `faq_miss_handover_threshold` (Sprint 6 §G2), `intake_complete_required` (NEW S2), `premature_resolve_outcome` (Sprint 11 §M1), `must_cite_source` (NEW S1), plus the existing Sprint 7 §I2 predicate naming convention. **These are the canonical `Guardrail.type` whitelist values your fix must encode.**
6. `docs/sprints/sprint-038-handoff.md` — Sprint 38 dev's archive from `bd9d3f5`. Re-read §4 implementation walkthrough + §5 freeze fidelity + §9 files changed + §10 stanza self-walk. The fix preserves all of Sprint 38's other surfaces UNCHANGED.
7. `docs/sprints/sprint-037-handoff.md` + `docs/sprints/sprint-037-codex-review.md` — Sprint 37 freeze context.
8. Code source files at HEAD `bd9d3f5` (the parent of this fix commit):
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (124 lines) — **TARGET of fix sub-gap #1a**: the compact constructor at lines 63-71 normalizes null collections; line 66 specifically normalizes null `toolsRequired` to `List.of()` which makes the loader's null-check unreachable.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (228 lines) — **TARGET of fix sub-gaps #1b + #1c + #1d**: the `validate(...)` method at lines 147-210 covers required-field presence + phase enum + wildcard-mixing + terminal-outcome enum + state-key + projection-slot + guardrail `on_fail` — but does NOT cover (b) tool-name allowlist, (c) explicit-UC registry, (d) guardrail-type whitelist.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Guardrail.java` (~56 lines) — read for the record shape (`type` + `onFail` + `parameters`); the record itself enforces nonblank `type` + `onFail`, but `SkillLoader.validate(...)` does not check `type` against a known whitelist.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` (~59 lines) — read for context; UNCHANGED in this fix.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` (~123 lines) — read for context; UNCHANGED in this fix.
   - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` (~141 lines; 8 tests) — verify whether ANY test asserts the OLD `null toolsRequired → List.of()` compact-constructor behavior specifically; if so, that test's expectation must change.
   - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` (~245 lines; 13 tests) — **TARGET for new negative cases**.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (174 lines) — read `UseCaseDefinition` record + the UC enumeration method (`getUseCase(...)` or `allUseCases()` or equivalent) for the explicit-UC registry-check fix #1c.
   - `server/src/main/resources/customer_service_tool_spec_v0_2.yaml` — read for the canonical tool-name set used in fix #1b. Note the file path may be elsewhere if the tool spec moved; grep for it: `find server/src/main/resources -name "customer_service_tool_spec*.yaml"`.
   - `server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java` — if the canonical tool-name set is also declared in Java (e.g., as an enum or registry), prefer the Java source over the YAML for the allowlist's source of truth. Grep `validateAgainstPlan` for the dispatch surface.
   - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (~1529 lines) — UNCHANGED in this fix. Only context; no edit.
9. Memory feedbacks (load from `.claude/agent-memory/sprint-deliver-orchestrator/`):
   - `feedback_constitution_discipline_vs_planning_anticipation.md` — do NOT silently edit governance docs.
   - `feedback_commit_at_end_bundles_deliver_artefacts.md` — dev does NOT stage deliver-agent-owned files.
   - `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every code-citation cites file path + line numbers.
   - `feedback_handoff_verdict_section_delegation.md` — leave fix-handoff §12 closure verdict as PLACEHOLDER.
   - `feedback_fix_iteration_objective_shape.md` (if present) — fix iteration shape guidance.

---

## 2. Task — close Codex Blocking Finding 1 (4 sub-gaps)

### 2.1 Fix #1a — `tools_required` presence detection (Option A: drop null-normalization in Skill record)

Per deliver-agent + human decision: **the surgical fix is to NOT normalize null `toolsRequired` to `List.of()` in the Skill record's compact constructor.** This makes the validator's null-check at `SkillLoader.java:155-157` reachable.

Edit `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java`:

- **Line 66** (current): `toolsRequired = toolsRequired == null ? List.of() : List.copyOf(toolsRequired);`
- **Change to**: `toolsRequired = toolsRequired == null ? null : List.copyOf(toolsRequired);`

Rationale:
- The compact constructor's job is immutability + defensive copy, NOT presence-checking.
- If YAML omits the `tools_required` key, Jackson sets the field to null; the loader's `validate(...)` catches null at line 155-157 and throws a schema error.
- If YAML provides `tools_required: []` explicitly, the compact constructor copies to `List.of()` (immutable); the loader's null-check passes (because the field IS present, just empty); downstream `composeSkillPhasePlan(...)` receives the empty list and composes `PhasePlan.allowedTools = Set.of()` which the existing `ToolDispatcher.validateAgainstPlan` enforces (rejects any tool call).
- **Empty `tools_required: []` is preserved as acceptable** — for future Skills that intentionally have no tool calls (e.g., a pure observation Skill). The dev's inline comment at `SkillLoader.java:152-154` rationale is preserved; only the gap of "field missing from YAML entirely" is closed.

**Update the Javadoc** in `Skill.java` (right above the compact constructor at line 63 OR at the record-level Javadoc) noting: `toolsRequired` is `null` until validated; downstream consumers (`SkillLoader.validate(...)` runs after deserialization) catch null and throw; post-validation, `toolsRequired` is guaranteed non-null (may be empty).

**Defensive note**: do NOT change the null-normalization for the OTHER List<String> fields (`applicablePhases`, `applicableUseCases`, `requiredContextKeys`, `validTerminalOutcomes`, `guardrails`) at lines 64-65 + 67-69. Those fields have different presence semantics:
- `applicablePhases` + `applicableUseCases` are checked via `requireNonEmpty(...)` in the loader (catches both null + empty); preserving the normalization keeps downstream code's iteration safe even if accidentally constructed via direct `new Skill(...)`.
- `requiredContextKeys` may genuinely be empty for some Skills (per design doc §2.1 optional set).
- `validTerminalOutcomes` is optional per §2.1.
- `guardrails` is intentionally empty for Sprint 38's 4 simpler phase Skills.

**Only `toolsRequired` gets the null-passthrough treatment** because §2.2 explicitly names it as a required field whose absence must fail-fast.

### 2.2 Fix #1b — tool-name allowlist in `SkillLoader.validate(...)`

Add a new private static Set<String> `VALID_TOOL_NAMES` in `SkillLoader.java` (alongside the existing `VALID_PHASES` / `VALID_STATE_KEYS` / `VALID_PROJECTION_SLOTS` / `VALID_ON_FAIL_MODES` blocks at lines 39-77).

**Source the canonical tool-name list**: read `server/src/main/resources/customer_service_tool_spec_v0_2.yaml` (or wherever the canonical spec lives) at session start; verify the tool names present at HEAD `bd9d3f5`. Expected canonical set (verify):

- `search_knowledge`
- `classify_use_case`
- `record_outcome`
- `request_handover`
- `create_case_controlled` (runtime-only; NOT in any Sprint 38 Skill `tools_required` but valid as a tool name)
- (any other tools present in the spec at HEAD)

**Alternative**: if the canonical tool-name set is declared in Java (e.g., as an enum in `ToolDispatcher` or `customer_service_tool_spec_v0_2` parsing code), prefer the Java source as the SkillLoader's import. Grep:

```bash
grep -rn "search_knowledge\|classify_use_case\|record_outcome\|request_handover\|create_case_controlled" server/src/main/java/ | head -20
```

If a Java enum / registry exists, import from it; else hardcode the Set in SkillLoader for now (and note in handoff §7 that future tool-spec edits should fold into this allowlist via a central source).

Add to `validate(...)` after the existing `requireNonEmpty(skill.applicableUseCases(), ...)` check, alongside the existing phase loop:

```java
for (String tool : skill.toolsRequired()) {
    if (!VALID_TOOL_NAMES.contains(tool)) {
        throw schema(filename, "tools_required contains unknown tool '" + tool
                + "'; valid tools are " + VALID_TOOL_NAMES);
    }
}
```

(The loop is empty-safe — if `toolsRequired` is empty, no iterations happen; the fix #1a null-passthrough is caught earlier by the null-check; non-empty lists with unknown names fail.)

### 2.3 Fix #1c — explicit `applicable_use_cases` UC registry check

Sprint 38's 4 Skills all use `applicable_use_cases: ["*"]`, so this gap doesn't manifest in production today. But Sprint 39 ships RESOLVE Skills with parameterized UCs (per design doc §6.2.5 + §6.2.6); the loader must catch unknown UC names BEFORE Sprint 39 ships.

Two implementation options:

- **Option A (recommended)**: inject `UseCaseRegistryService` into `SkillLoader` via constructor. Spring will auto-wire. Verify that `UseCaseRegistryService` is `@Component` / `@Service` (read `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java`); if so, simply add a constructor parameter:

  ```java
  private final UseCaseRegistryService useCaseRegistry;

  public SkillLoader(UseCaseRegistryService useCaseRegistry) {
      this.yamlMapper = new ObjectMapper(new YAMLFactory());
      this.useCaseRegistry = useCaseRegistry;
  }
  ```

  In `validate(...)`, after the existing wildcard-mixing check:

  ```java
  if (!hasWildcard) {
      for (String uc : skill.applicableUseCases()) {
          if (useCaseRegistry.getUseCase(uc) == null) {  // OR equivalent registry lookup method
              throw schema(filename, "applicable_use_cases contains unknown UC '" + uc
                      + "'; not registered in UseCaseRegistryService");
          }
      }
  }
  ```

  Verify the actual `UseCaseRegistryService` API at HEAD (`getUseCase(String) → UseCaseDefinition` OR `containsUseCase(String) → boolean` OR similar); adapt the lookup call accordingly.

- **Option B (fallback if Option A has Spring ordering issues with the bean graph)**: hardcode the set of registered UC IDs from `UseCaseRegistryService` enumeration into `SkillLoader` as a static `VALID_USE_CASES` Set. Less ideal (couples SkillLoader to a snapshot of the UC registry) but simpler. Add a TODO note in `SkillLoader` Javadoc pointing to the central registry as the long-term source of truth.

**Prefer Option A**; fall back to Option B if Spring bean-graph ordering / circular-dependency issues block injection.

### 2.4 Fix #1d — guardrail `type` whitelist in `SkillLoader.validate(...)`

Add a new private static Set<String> `VALID_GUARDRAIL_TYPES` in `SkillLoader.java` (alongside the existing constant Sets).

**Source the canonical predicate-type list** from Sprint 37 freeze §8.2.1-§8.2.5:

- `faq_miss_handover_threshold` — Sprint 6 §G2 predicate (migrated in Sprint 39).
- `intake_complete_required` — NEW S2 (Sprint 39 scope).
- `premature_resolve_outcome` — Sprint 11 §M1 (migrated in Sprint 39).
- `must_cite_source` — NEW S1 (Sprint 39 scope per M2 §6 #4 verbatim authorization).
- `incomplete_intake_handover` — Sprint 7 §I2 predicate (the existing `shouldRejectIncompleteIntakeHandover` Java method; migrated in Sprint 39).

Re-read design doc §8.2 to verify the exact canonical names. The Skill 4 Sprint 38 Skills all ship `guardrails: []` so the loop is empty for them today; the whitelist becomes load-bearing for Sprint 39's first concrete guardrails.

Add to `validate(...)` inside the existing guardrails loop at lines 204-209 (extend the existing `on_fail` check):

```java
for (Guardrail g : skill.guardrails()) {
    if (!VALID_GUARDRAIL_TYPES.contains(g.type())) {
        throw schema(filename, "guardrail type '" + g.type()
                + "' is not a known predicate type; valid types are " + VALID_GUARDRAIL_TYPES);
    }
    if (!VALID_ON_FAIL_MODES.contains(g.onFail())) {
        throw schema(filename, "guardrail type=" + g.type() + " has invalid on_fail '"
                + g.onFail() + "'; valid modes are " + VALID_ON_FAIL_MODES);
    }
}
```

(The existing `on_fail` check at lines 205-208 already validates `onFail`; just prepend the new `type` check.)

### 2.5 New negative test cases in `SkillLoaderTest.java`

Add ~5 new negative test cases at the end of `SkillLoaderTest.java`. Each test follows the existing pattern: build a malformed YAML string + parse via `parseAndValidate(InputStream, filename)` + assertThrows `IllegalStateException` with a meaningful error message substring.

Required new tests:

1. `parseAndValidate_missingToolsRequiredField_failsFast()` — YAML omits the `tools_required` key entirely (other required fields present). Assert: throws `IllegalStateException` containing "tools_required is required".
2. `parseAndValidate_unknownToolName_failsFast()` — YAML has `tools_required: [some_nonexistent_tool]`. Assert: throws containing "tools_required contains unknown tool 'some_nonexistent_tool'".
3. `parseAndValidate_explicitUnknownUseCase_failsFast()` — YAML has `applicable_use_cases: [UC-NONEXISTENT]` (non-wildcard, unknown UC). Assert: throws containing "applicable_use_cases contains unknown UC 'UC-NONEXISTENT'".
4. `parseAndValidate_explicitKnownUseCase_passes()` — YAML has `applicable_use_cases: [<real UC>]` (non-wildcard, known UC). Assert: passes without exception. (Positive test confirming the registry check accepts valid UCs.) Pick a real UC from `UseCaseRegistryService` (e.g., `UC-A`, `UC-B`, `UC-C`, `UC-D`, `UC-F` — verify by reading the registry at session start).
5. `parseAndValidate_unknownGuardrailType_failsFast()` — YAML has `guardrails: [{type: "nonexistent_predicate", on_fail: "reject_with_hint"}]`. Assert: throws containing "guardrail type 'nonexistent_predicate' is not a known predicate type".

Optional (if Option A null-passthrough in Skill record breaks an existing SkillTest assertion):

6. **Adjust** any existing `SkillTest` assertion that explicitly asserts `null toolsRequired → List.of()` compact-constructor behavior. The new behavior: `null toolsRequired` stays null; the validator catches. Existing SkillTest cases that construct Skill records with explicit non-null `toolsRequired` (most likely all of them) are unaffected.

### 2.6 What NOT to touch in this fix iteration

- **DO NOT** edit any of the 4 Skill YAML files (`discover_triage.yaml` / `confirm.yaml` / `escalate.yaml` / `terminal.yaml`). All 4 production Skills have valid `tools_required` + `applicable_use_cases: ["*"]` + `guardrails: []`; they pass the strengthened validator unchanged.
- **DO NOT** edit `PhaseEvaluator.java` (the integration is correct at `bd9d3f5`; the fix is in SkillLoader only).
- **DO NOT** edit `PhaseEvaluatorSkillIntegrationTest.java` (the 13 behavioural-equivalence golden assertions are correct).
- **DO NOT** edit `SkillRegistry.java` or `StateInheritance.java` or `Guardrail.java` (the record shape is correct; the gap is in the LOADER, not the records).
- **DO NOT** edit any of the 19 existing PhaseEvaluator test constructor sites (the `SkillTestFixtures.productionRegistry()` injection is correct).
- **DO NOT** add S1 `must_cite_source` predicate or any concrete Skill guardrail logic (Sprint 39 scope).
- **DO NOT** add a unified `SkillGuardrailDispatcher.java` (Sprint 39 scope).
- **DO NOT** touch `AgentRunLoopImpl.java` / `system_prompt.txt` / `ContextProjectionBuilder.java` / `IntakeFieldsRegistry.java` / `runtime_freeze_and_risk_policy.md` / `iteration_governance.md` / `docs/proposals/skill_registry_design.md`.
- **DO NOT** edit `eval_interactive/**` (M2 §6 fences + Sprint 38 §6 #19-#23).
- **DO NOT** edit deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`).
- **DO NOT** rename CLOSE → TERMINAL phase enum (Sprint 38 §6 #30 fence preserved).

---

## 3. Hard fences (inherited from Sprint 38 contract §6 + M2 §6)

All 31 Sprint 38 hard fences from `docs/sprint_objective.md` §6 + all 22 M2 hard fences from `docs/milestone_objective.md` §6 carry forward UNCHANGED. The fix iteration's allowed surface is strictly:

- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (1-line compact constructor change for `toolsRequired` + Javadoc note per §2.1 above).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (add 2 new constant Sets + 1 constructor injection of `UseCaseRegistryService` + 3 new validation surfaces in `validate(...)` per §2.2 + §2.3 + §2.4).
- **EDIT** `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` (add ~5 new negative test methods per §2.5).
- **EDIT** (CONDITIONAL) `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` — ONLY if an existing test asserts `null toolsRequired → List.of()` behavior; in that case, update the assertion to reflect the new null-passthrough semantics.
- **NEW** `docs/sprints/sprint-038-fix-handoff.md` (12-section dev-authored archive per fix-iteration shape; see §6 below).

Any other file edit is a **BLOCKING scope violation** for this fix iteration.

---

## 4. Stop conditions

STOP and report (do NOT silently work around) when:

1. **The canonical tool-name set cannot be located** in `customer_service_tool_spec_v0_2.yaml` or Java source (the fix #1b allowlist source is missing) — STOP + surface in handoff §7 OQ for deliver-agent + human guidance.
2. **`UseCaseRegistryService` injection causes Spring bean-graph circular dependency** — fall back to Option B (hardcoded `VALID_USE_CASES` Set); note the fallback choice in handoff §7 OQ.
3. **A SkillTest case fails after the Skill.java null-passthrough change** — examine the failing test; if it asserts the OLD null-normalization, UPDATE the assertion (per §2.5 #6); if it asserts something else and the change breaks an unrelated invariant, STOP + surface in handoff §7 OQ.
4. **A SkillLoaderTest existing case fails after the strengthened validator** — STOP + investigate. Possible causes: an existing negative test was relying on a different schema-failure error message; OR the existing test was constructing a Skill with valid surface that the new validation incorrectly rejects. Fix the test message expectation OR fix the validator OR surface in §7 OQ. **No existing SkillLoaderTest case should suddenly start passing or failing in a way that contradicts §2.5's intent**.
5. **An existing PhaseEvaluator or behavioural-equivalence test fails** — STOP. The fix should not regress any PhaseEvaluator surface. If a test fails, the fix introduced an unintended ripple; back out + diagnose.
6. **`Sprint71PartialIntakePersistenceTest` 14/14 fails** — M1 functional-surface regression; STOP + diagnose.
7. **Any §1.7 forbidden-list concern surfaces** (e.g., the tool-name allowlist hardcodes a per-UC tool restriction; the UC-registry check encodes a per-phase UC list) — STOP + surface.
8. **Java baseline regresses beyond expectations** — expected post-fix baseline is `1028 + ~5 (new negative tests) = 1033 / 1-inherited / 0 / 2`. Any new failure beyond the 1 inherited is a STOP signal. Re-run + diagnose.
9. **Tempted to broaden scope** (e.g., "while I'm in SkillLoader, let me also pre-validate something Sprint 39 will need"; "let me adjust a Skill YAML body since it's right there") — STOP. The fix is the 4 schema gaps only.
10. **Tempted to edit Sprint 38 freeze design doc** to clarify §2.2 — STOP. The freeze is immutable. Surface as informational OQ for M2 close fold-back if the freeze wording itself is ambiguous; do not edit.

---

## 5. Bundle policy

- **Single dev commit** with all §3 allowed surfaces: `Skill.java` 1-line edit + Javadoc; `SkillLoader.java` constructor + 2 new Sets + 3 new validation surfaces; `SkillLoaderTest.java` ~5 new test methods; (conditional) `SkillTest.java` assertion adjustment; `docs/sprints/sprint-038-fix-handoff.md` NEW.
- Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent-owned files.
- Commit message format (per recent precedent):

```
sprint 38 fix #1: SkillLoader schema-validation completeness for design doc §2.2 (close Codex Blocking Finding 1)
```

(With or without a longer body; deliver-agent doesn't require a specific body shape but a 2-3 sentence summary of the 4 sub-fixes is helpful for the fix-review Codex round.)

---

## 6. Java baseline expectation

Post-fix expected `mvn test -q` result: `Tests run: ~1033, Failures: 1, Errors: 0, Skipped: 2` (Sprint 38 baseline 1028 + ~5 new negative tests in SkillLoaderTest). The 1 inherited `SystemPromptUserRequestedTiebreakerTest` failure persists unchanged. Run from clean checkout before committing; verify counts; surface any unexpected delta.

If the conditional SkillTest assertion adjustment is needed (per §2.5 #6 + §4 #3), the total count may shift slightly but the new-failure count must stay at 0.

---

## 7. Handoff document structure (12 sections per Sprint 31-38 fix-iteration shape)

Author `docs/sprints/sprint-038-fix-handoff.md` with 12 sections following the fix-iteration shape (adapted from the standard 12-section per `feedback_fix_iteration_objective_shape.md` if present; otherwise mirror the Sprint 38 dev handoff at `docs/sprints/sprint-038-handoff.md` adapted for fix scope):

1. **Context Pack** — what was read at session start; cite each file (Sprint 38 dev handoff, Sprint 37 freeze §2.2 + §8.2, codex-findings.md, Skill.java + SkillLoader.java + Guardrail.java + SkillLoaderTest.java + UseCaseRegistryService.java + customer_service_tool_spec_v0_2.yaml); risks assessed at session start.
2. **Fix iteration recap** — Sprint 38 fix #1 closes Codex Blocking Finding 1 (4 sub-gaps in SkillLoader schema validation per design doc §2.2). Cite the codex-findings.md paragraph verbatim.
3. **Codex Finding 1 sub-gap closure** — for each of the 4 sub-gaps (#1a tools_required null detection + #1b tool-name allowlist + #1c explicit-UC registry check + #1d guardrail-type whitelist): cite the implementation file + line range that closes the sub-gap; cite the new SkillLoaderTest negative case that verifies closure.
4. **Implementation walkthrough** — high-level summary of: Skill.java line 66 surgical edit; SkillLoader.java new constants + constructor injection + 3 new validation surfaces; SkillLoaderTest.java ~5 new negative tests.
5. **§4.1 anti-hardcode self-walk on the fix iteration diff** — walk all 9 questions; expected verdict `approve` (no per-UC-branch hardcode; the 3 new allowlists are RUNTIME-ENFORCEMENT whitelists per §1.4, not LLM-semantic decision tables; tool-name allowlist mirrors existing `ToolDispatcher.validateAgainstPlan` invariant; UC-registry check mirrors existing `UseCaseRegistryService.getUseCase(...)` invariant; guardrail-type whitelist mirrors planned Sprint 39 predicate types per design doc §8.2).
6. **§1.7 boundary check on the fix iteration** — verify the 3 new allowlists encode no per-UC-pair branch, no eval phrasing, no LLM-vs-Java boundary shift; the tool-name + UC-registry + guardrail-type whitelists are Runtime-floor enforcement per §1.4.
7. **Open questions for deliver-agent + human** — any §1.7 boundary concern; any premise drift; any tension between freeze §2.2 wording and implementation choice; any Spring bean-graph issue forcing Option B fallback on fix #1c; any SkillTest assertion adjustment required.
8. **Anti-hardcode self-walk on the fix diff** — duplicate of §5; same verdict `approve`.
9. **Files changed** — table with paths; 2 production edits (`Skill.java`, `SkillLoader.java`) + 1 test edit (`SkillLoaderTest.java`) + (conditional) `SkillTest.java` + handoff = 4 or 5 files.
10. **Layer-classification self-walk** — fix iteration layer = `skill_state` (SkillLoader schema enforcement) + Runtime-owned floor per §1.4 (load-time validation; not LLM-semantic surface).
11. **§5 Eval Acceptance bars** — adapted for fix iteration: target = 4 sub-gaps closed; neighbor = behavioural equivalence on 4 migrated phases preserved (re-run PhaseEvaluatorSkillIntegrationTest 13/0); negative = ~5 new SkillLoaderTest negative cases; Java baseline `~1033/1-inherited/0/2`.
12. **Closure verdict placeholder** — leave for deliver-agent + human + Codex fix-review per `feedback_handoff_verdict_section_delegation.md`.

---

## 8. M2 milestone context recap (quick reference)

| Sub-sprint | Status | Scope |
|---|---|---|
| Sprint 37 | **CLOSED PASS A 2026-05-17** (commit `51c327c`) | Skill Registry design freeze |
| Sprint 38 (dev #1) | Codex returned `fix_required / blocking_count: 1` (commit `bd9d3f5`) | SkillRegistry core + 4 simpler phase Skills |
| **Sprint 38 fix #1** | **Current fix iteration** | Close Codex Blocking Finding 1 — SkillLoader schema-validation completeness |
| Sprint 39 | Future | RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + NEW S1 + S2 + unified Skill terminal-predicate dispatcher |
| Sprint 40 | Future | Sprint 23/31/33 teaching paragraph extraction from `system_prompt.txt` + orchestration shell cleanup |
| Sprint 41 | Future | UC switch + state preservation (`SkillStateBus.java` + per-Skill `state_inheritance` enforcement + NEW `prior_use_case_carry` projection slot) |

After this fix commit lands: deliver-agent drafts `compact/sprint-038-fix-review-prompt.md` + re-dispatches Codex on the fix-review surface (Finding 1 closure only, no other surface re-reviewed). Expected fix-review outcome: `pass / 0` → Sprint 38 closes B-fix-iterated. Sprint 39 contract drafting follows.

---

## 9. Self-check before committing

- [ ] All §1 read order completed; cited in handoff §1 Context Pack.
- [ ] §2.1 Fix #1a: `Skill.java:66` toolsRequired null-passthrough; Javadoc updated.
- [ ] §2.2 Fix #1b: `SkillLoader.java` VALID_TOOL_NAMES Set added; validation loop in `validate(...)`.
- [ ] §2.3 Fix #1c: `SkillLoader.java` UseCaseRegistryService constructor injection (or Option B fallback with TODO note); validation surface for non-wildcard UCs.
- [ ] §2.4 Fix #1d: `SkillLoader.java` VALID_GUARDRAIL_TYPES Set added; `type` check added inside guardrails loop.
- [ ] §2.5 New negative tests in `SkillLoaderTest.java` cover all 4 sub-gaps + 1 positive registry test.
- [ ] §2.5 #6 conditional: SkillTest assertion adjustment if any existing test asserted old null-normalization behavior.
- [ ] §3 hard fences honored: only Skill.java + SkillLoader.java + SkillLoaderTest.java (+ optional SkillTest.java) + handoff edited.
- [ ] §4 stop conditions evaluated; none fired (or surfaced as OQ).
- [ ] §5 bundle policy: single fix commit; deliver-agent-owned files NOT staged.
- [ ] §6 Java baseline `~1033/1-inherited/0/2`; no new-failure delta beyond 1 inherited.
- [ ] §7 handoff structure 12 sections complete; §12 closure verdict left as PLACEHOLDER.
- [ ] No edit to deliver-agent-owned files.
- [ ] No edit to `docs/proposals/skill_registry_design.md` (Sprint 37 freeze immutable).
- [ ] No edit to PhaseEvaluator.java, AgentRunLoopImpl.java, system_prompt.txt, ContextProjectionBuilder.java, IntakeFieldsRegistry.java, runtime_freeze_and_risk_policy.md.
- [ ] No edit to existing Skill YAMLs, behavioural-equivalence integration test, SkillRegistry.java, StateInheritance.java, Guardrail.java.
- [ ] Commit message: `sprint 38 fix #1: SkillLoader schema-validation completeness for design doc §2.2 (close Codex Blocking Finding 1)`.

Once committed, surface the commit SHA + a brief summary (~3-5 sentences on what shipped + any §7 OQs surfaced) to the human. The deliver-agent will draft `compact/sprint-038-fix-review-prompt.md` + re-dispatch Codex on the fix-review surface (Finding 1 closure only).
