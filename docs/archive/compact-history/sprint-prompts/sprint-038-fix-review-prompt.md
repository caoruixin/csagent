Paste the content below this line into a fresh Codex session after the Sprint 38 fix iteration #1 commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for **Sprint 38 fix iteration #1** — closing your own prior `decision: fix_required / blocking_count: 1` verdict on Sprint 38 (commit `bd9d3f5`) by re-reviewing the targeted fix commit at `5787806`.

The prior Codex review of `bd9d3f5` (archived at `docs/codex-findings.md` in its pre-supersession state — read the file at HEAD `5787806` for the verdict your prior round issued; the file will be overwritten by THIS fix review) identified **Blocking Finding 1**: `SkillLoader` does not fully implement Sprint 37 freeze design doc §2.2 schema validation — four sub-gaps: (a) `tools_required` presence cannot be detected after `Skill` normalizes null lists to `List.of()`; (b) no `tools_required` tool-name allowlist; (c) no explicit `applicable_use_cases` UC registry check; (d) no `Guardrail.type` known-type check.

The deliver-agent + human classified per `iteration_governance.md` §4 as **B — fix-iteration sub-sprint**. The fix iteration's authorized scope per `compact/sprint-038-fix-dev-prompt.md` was strictly: 4 sub-gap closures + ~5 new negative tests + (conditional) SkillTest adjustment + fix handoff. No other Sprint 38 surface should change.

**Your job (THIS round)**: re-review ONLY the diff `bd9d3f5..5787806` for Finding 1 closure correctness + scope discipline. **Do NOT re-walk the full §4.1 9-question kernel on the surfaces that did NOT change in this fix iteration** (PhaseEvaluator integration, 4 Skill YAML bodies, SkillRegistry.select semantics, behavioural-equivalence golden assertions, Sprint 6/7/11 predicate preservation, Sprint 33 cue/slot split, MATERIAL FINDING preservation, premise re-verification) — those passed your prior verdict and are unchanged. The §4.1 + §1.7 walks in THIS round target ONLY the fix surfaces (Skill.java line ~66 + SkillLoader.java new validation surfaces + SkillLoaderTest new negative cases + SkillTest assertion adjustment + SkillRegistryTest + SkillTestFixtures constructor-signature updates).

**Per `iteration_governance.md` §4.3**: fix iterations on per-sub-sprint-Codex sub-sprints continue to fire per-sub-sprint Codex review at fix close. Sprint 38 fix #1's review is THIS round.

**Sprint-38-fix-close pre-decisions (deliver-agent + human, 2026-05-17):**

- **Classification target**: `pass / 0` is the expected outcome if all 4 sub-gaps are correctly closed + no scope drift + Java baseline preserved.
- **If Codex finds the 4 sub-gaps are NOT all closed correctly**: `fix_required` with the failing sub-gap quoted + the dev re-iterates ONLY on that sub-gap. The dev gets ONE more fix round.
- **If Codex finds a NEW concern on the fix surfaces** (e.g., the new VALID_TOOL_NAMES allowlist is over-broad or under-broad; the explicit-UC check has a logic bug): route per severity. If the concern means a sub-gap is mis-closed, `fix_required`. If the concern is a Sprint 39+ design question (e.g., "the canonical tool-name set should be centralized"), route as `out_of_scope_review` + R-item suggestion + Sprint 38 fix close as `pass / 0` separately.
- **If Codex finds a concern on the OLD surfaces** (PhaseEvaluator, 4 Skill YAMLs, behavioural-equivalence test, etc. — unchanged in this fix): route as `out_of_scope_review` per `feedback_oq_on_pre_existing_surface_oosr_only_routing.md` — those surfaces passed the prior review at `bd9d3f5`; revisiting them now would broaden scope beyond the fix iteration. Open R-item for M2 close or Sprint 39 planning round consideration.

---

## 1. Loader

Read in this order on a fresh Codex session.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / §4.3 / §5 / §7).
2. **`docs/codex-findings.md`** at HEAD `5787806` — read the prior Codex review of `bd9d3f5` end-to-end. The **Blocking Finding 1 paragraph** is the contract for this fix review. Note: this file will be OVERWRITTEN by your output this round (per `feedback_packaging_codex_findings_supersession.md` delete-and-add pattern at archive time).
3. **`docs/sprints/sprint-038-fix-handoff.md`** — the dev's fix-iteration archive (~241 lines / 31.8K). Read §1 Context Pack + §2 fix recap + §3 sub-gap closure table + §4 implementation walkthrough + §5 §4.1 self-walk + §6 §1.7 self-walk + §7 OQs (4 informational items: OQ-FIX1.1 VALID_TOOL_NAMES sourcing; OQ-FIX1.2 freeze-canonical predicate names; OQ-FIX1.3 UseCaseRegistryService injection no circular dependency; OQ-FIX1.4 each test class re-inits UseCaseRegistryService) + §11 acceptance bars + §12 closure verdict PLACEHOLDER.
4. **`docs/sprints/sprint-038-handoff.md`** — Sprint 38 main-dev handoff (commit `bd9d3f5`); contextual.
5. **`docs/proposals/skill_registry_design.md` §2.2 + §5.2 + §8.2** — the freeze contract for the schema validation surface. Re-read §2.2 (JSON Schema validation contract); §5.2 (Guardrail DSL); §8.2.1-§8.2.5 (canonical predicate-type plan for Sprint 39).
6. **`docs/sprint_objective.md`** §6 hard fences (31 items) — re-read; the fix iteration honors all of them.
7. **`compact/sprint-038-fix-dev-prompt.md`** — what the dev was authorized to do vs what landed. The dev prompt §2 enumerates 5 sub-tasks (#1a/#1b/#1c/#1d/#1e tests + conditional SkillTest adjustment); §3 lists allowed surfaces (Skill.java + SkillLoader.java + SkillLoaderTest.java + conditional SkillTest.java + fix handoff). Verify diff stays within this allowlist.
8. **Code source files at HEAD `5787806`** (read on demand during verification):
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (~140 lines post-fix; verify the compact constructor at lines ~63-72 — `toolsRequired` is NO LONGER null-normalized; other List fields preserve normalization).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (313 lines post-fix; 228 pre-fix; verify the new constants `VALID_TOOL_NAMES` + `VALID_GUARDRAIL_TYPES`; the new `UseCaseRegistryService` constructor injection; the 3 new validation surfaces in `validate(...)`).
   - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` (366 lines post-fix; 245 pre-fix; verify 5 new negative tests + 1 positive UC companion = 6 new test methods → 13 + 5 = 18 tests total).
   - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` (~160 lines post-fix; verify the existing test that asserted `null toolsRequired → List.of()` has been adjusted to `assertNull(...)`; +1 new positive empty-list test).
   - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` (~152 lines post-fix; 6-line constructor-signature update only).
   - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` (~37 lines post-fix; 16-line constructor-signature update only).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (174 lines; UNCHANGED in fix iteration; read for the API SkillLoader injects — `getUseCase(...)` / `isKnownUseCase(...)` / equivalent).
   - **Code source files UNCHANGED in fix iteration** (do NOT re-walk; trust prior review at `bd9d3f5`):
     - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
     - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java`
     - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Guardrail.java`
     - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java`
     - All 4 Skill YAMLs (`discover_triage.yaml`, `confirm.yaml`, `escalate.yaml`, `terminal.yaml`)
     - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
     - `server/src/main/resources/prompts/system_prompt.txt`
     - `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java`
9. `docs/current/iteration_governance.md` §1.4 (Runtime-owned floor — the 3 new allowlists are load-time Runtime configuration enforcement per §1.4); §1.7 (Forbidden — verify the 3 new allowlists do NOT encode per-UC-pair branch / eval phrasing / LLM-vs-Java boundary shift); §3.2 (the fix iteration layer is `skill_state` + Runtime-owned floor per §1.4).

## 2. Scope-discipline gate (BLOCKING)

Verify the fix commit at `5787806` touches ONLY the surfaces enumerated in `compact/sprint-038-fix-dev-prompt.md` §3. Run:

```bash
git -C /Users/caoruixin/projects/csagent-latest diff bd9d3f5..5787806 --stat
```

Expected exactly **7 files changed**:

- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (~15 lines; compact-constructor `toolsRequired` null-passthrough + Javadoc note).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (~91 lines added; new VALID_TOOL_NAMES + VALID_GUARDRAIL_TYPES + UseCaseRegistryService injection + 3 new validation surfaces).
- **EDIT** `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` (~123 lines added; +5 new negative tests + 1 positive UC companion).
- **EDIT** `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` (~19 lines; null-passthrough assertion adjustment + 1 new positive empty-list test).
- **EDIT** `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` (~6 lines; constructor-signature update for the new SkillLoader UseCaseRegistryService injection).
- **EDIT** `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` (~16 lines; constructor-signature update).
- **NEW** `docs/sprints/sprint-038-fix-handoff.md` (241 lines; 12-section fix-iteration archive).

**BLOCKING scope violations** if ANY of these surfaces appear in `5787806`:
- ANY file under `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`.
- ANY of the 4 Skill YAMLs (`server/src/main/resources/skills/*.yaml`).
- ANY file under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` other than `Skill.java` + `SkillLoader.java` (i.e., NO edit to `SkillRegistry.java` / `Guardrail.java` / `StateInheritance.java`).
- `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java`.
- ANY of the 19 PhaseEvaluator constructor-call-site test files updated in Sprint 38 main-dev commit (`bd9d3f5`).
- `AgentRunLoopImpl.java` / `system_prompt.txt` / `ContextProjectionBuilder.java` / `IntakeFieldsRegistry.java` / `UseCaseRegistryService.java`.
- `docs/runtime_freeze_and_risk_policy.md` / `docs/current/*` / `docs/foundational/*` / `docs/proposals/skill_registry_design.md` / sprint archives / milestone archives.
- Deliver-agent-owned files (`docs/milestone_objective.md` / `docs/sprint_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`).
- ANY eval surface (`eval_interactive/**`).

Any scope violation = Finding #1 with the file path + line range quoted.

## 3. Sub-gap closure verification (substantive review surface)

For each of the 4 sub-gaps in your prior Blocking Finding 1, verify closure by reading the implementation + the new negative test that exercises it.

### 3.1 Sub-gap #1a — `tools_required` presence detection

**Closure expectation**: a YAML missing the `tools_required` key entirely now fails at boot.

Verify:

- **Skill.java compact constructor** at lines ~63-72: `toolsRequired = toolsRequired == null ? null : List.copyOf(toolsRequired);` (no longer normalizes to `List.of()`). The Javadoc comment inside the constructor explicitly cites Finding 1 sub-gap #1a as the rationale. The other List<String> fields (`applicablePhases`, `applicableUseCases`, `requiredContextKeys`, `validTerminalOutcomes`, `guardrails`) STILL normalize null to `List.of()` (no regression on the defensive immutability pattern for non-`toolsRequired` fields).
- **SkillLoader.java validate(...)** at the previously-cited lines (now around 155-157, or wherever the null-check moved post-fix): the `if (skill.toolsRequired() == null) { throw schema(filename, "tools_required is required (may be empty list)"); }` block is unchanged structurally but now REACHABLE (because `null` is no longer normalized away upstream).
- **SkillLoaderTest new negative test** `parseAndValidate_missingToolsRequiredField_failsFast` (or equivalent name): builds a YAML string omitting `tools_required`; asserts throws `IllegalStateException` with message containing "tools_required is required". Re-run `mvn test -Dtest=SkillLoaderTest#parseAndValidate_missingToolsRequiredField_failsFast -q`; verify pass.
- **SkillTest** verifies the new null-passthrough behavior. Read the test that previously asserted `Skill(..., null, ...).toolsRequired()` equals `List.of()`; verify it now asserts `assertNull(skill.toolsRequired())` for the null case. Verify the new positive empty-list test asserts `Skill(..., List.of(), ...).toolsRequired()` equals `List.of()` (empty list preserved when explicitly passed).

If the sub-gap is correctly closed AND no Skill compact-constructor regression on other fields: PASS.

### 3.2 Sub-gap #1b — tool-name allowlist

**Closure expectation**: a YAML with a tool name not in the canonical set fails at boot.

Verify:

- **SkillLoader.java VALID_TOOL_NAMES** Set declaration. Per dev OQ-FIX1.1, the set was sourced from Java Tool implementations (mirroring `Tool.getName()` returns), NOT parsed from `customer_service_tool_spec_v0_2.yaml`. Expected 11 tools (or whatever Tool.getName() returns at HEAD `5787806`).
  - Independently verify the canonical set by grepping Java Tool implementations:
    ```bash
    grep -rn "public String getName" server/src/main/java/com/gumtree/csagent/service/tools/ | head -20
    ```
    Cross-reference each `getName()` return value with the VALID_TOOL_NAMES Set in SkillLoader.java. If a Tool exists in Java but is NOT in VALID_TOOL_NAMES (or vice versa), surface as a finding (sub-gap not fully closed).
- **SkillLoader.java validate(...)** new loop: iterates `skill.toolsRequired()` and throws schema error on unknown tool names. Verify the loop runs AFTER the new null-check (so empty list passes the loop; null list fails earlier).
- **SkillLoaderTest new negative test** `parseAndValidate_unknownToolName_failsFast` (or equivalent name): YAML has `tools_required: [some_nonexistent_tool]`; asserts throws with message containing the unknown tool name + the valid set. Re-run; verify pass.

If the allowlist is comprehensive (all real tools included) AND the test covers the closure: PASS. Note OQ-FIX1.1 informationally (future R-item to centralize the canonical tool-name source); not blocking.

### 3.3 Sub-gap #1c — explicit `applicable_use_cases` UC registry check

**Closure expectation**: a YAML with `applicable_use_cases: [UC-NONEXISTENT]` (non-wildcard, unknown UC) fails at boot.

Verify:

- **SkillLoader.java constructor signature**: now `public SkillLoader(UseCaseRegistryService useCaseRegistry)` (or equivalent injection pattern). Per OQ-FIX1.3, no circular dependency was introduced.
- **SkillLoader.java validate(...)** new surface: for each non-wildcard value in `applicableUseCases`, queries `useCaseRegistry.getUseCase(...)` (or `isKnownUseCase(...)`) and throws on null/false return.
- **SkillLoaderTest new negative test** `parseAndValidate_explicitUnknownUseCase_failsFast`: YAML has explicit unknown UC; asserts throws with message containing the unknown UC name + a meaningful diagnostic.
- **SkillLoaderTest new positive test** (per OQ-FIX1.4): YAML has a real UC from the registry (e.g., UC-A or UC-B); asserts passes without exception. Confirms the registry check accepts valid UCs and isn't accidentally rejecting all explicit values.
- **SkillTestFixtures + SkillRegistryTest** constructor-signature updates to pass `UseCaseRegistryService` instance.

If the registry check works AND the positive test confirms valid UCs pass: PASS. Note OQ-FIX1.4 informationally (future R-item if test runtime becomes a concern); not blocking.

### 3.4 Sub-gap #1d — guardrail-type whitelist

**Closure expectation**: a YAML with a guardrail entry whose `type` is not in the canonical Sprint 39 predicate-type set fails at boot.

Verify:

- **SkillLoader.java VALID_GUARDRAIL_TYPES** Set declaration. Per dev OQ-FIX1.2, the set was sourced from Sprint 37 freeze §5.2 + §8.2 canonical predicate names (NOT my prompt's informal abbreviations). Expected 4 canonical types per design doc §8.2.1-§8.2.5:
  - Read `docs/proposals/skill_registry_design.md` §8.2.1 through §8.2.5 verbatim; extract each canonical predicate name (e.g., `faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome`, `must_cite_source` — verify exact names at the freeze).
  - Cross-reference VALID_GUARDRAIL_TYPES Set in SkillLoader.java; verify each canonical name is present + no extra names not in the freeze.
- **SkillLoader.java validate(...)** new check inside the existing guardrails loop: validates `g.type()` against VALID_GUARDRAIL_TYPES BEFORE the existing `on_fail` check.
- **SkillLoaderTest new negative test** `parseAndValidate_unknownGuardrailType_failsFast`: YAML has `guardrails: [{type: "nonexistent_predicate", on_fail: "reject_with_hint"}]`; asserts throws with message containing the unknown type name + the valid set.

If the whitelist matches the freeze §8.2 canonical names AND the test covers the closure: PASS. Note OQ-FIX1.2 informationally (dev chose freeze-canonical over informal abbreviations — the correct call); not blocking.

## 4. §1.7 boundary check on the fix surfaces (NARROW; only the 3 new allowlists + Skill compact-constructor edit)

§1.7 forbidden list applies to the FIX iteration diff. Verify:

- **VALID_TOOL_NAMES** Set encodes a Runtime-floor tool-name allowlist sourced from Java Tool implementations. NO per-UC encoding (the set is global; doesn't vary by UC). NO eval phrasing. NO LLM-vs-Java boundary shift — this is `§1.4 tool schema` enforcement at load time, parallel to existing `ToolDispatcher.validateAgainstPlan` at runtime.
- **UseCaseRegistryService registry check** encodes a Runtime-floor UC-membership check sourced from the existing `UseCaseRegistryService`. NO per-phase encoding (the check is uniform across phases — explicit UC must be registered, regardless of phase). NO eval phrasing.
- **VALID_GUARDRAIL_TYPES** Set encodes a Runtime-floor predicate-type whitelist sourced from Sprint 37 freeze §8.2 canonical predicate names. NO per-UC encoding. NO per-phase encoding. NO LLM-semantic decision encoded — `guardrail.type` names structural predicates (e.g., `must_cite_source`, `intake_complete_required`) that enforce §1.4 Runtime-owned floor surfaces; the LLM does NOT branch on these.
- **Skill.java compact-constructor null-passthrough for `toolsRequired` ONLY**: surgical edit. Other List<String> fields preserve normalization. No §1.3 / §1.4 boundary shift; the null-passthrough is a presence-detection enabling pattern, not a semantic surface change.

If ANY of the 3 new allowlists encodes a per-UC / per-phase / per-pair branch table (instead of a flat membership Set), raise as a §1.7 violation. Expected: PASS — all 3 allowlists are flat membership Sets, structurally lookup-not-branch.

## 5. Validation runs (you re-execute)

From a clean checkout of the fix commit `5787806`:

- **Java test baseline preservation:**

  ```bash
  cd server && mvn test -q
  ```

  Expected: `Tests run: 1034, Failures: 1, Errors: 0, Skipped: 2` (post-Sprint-38-main-dev baseline 1028 + 5 new SkillLoaderTest negatives + 1 new SkillLoaderTest positive UC companion + 1 new SkillTest positive empty-list test − some test count delta if SkillTest assertion adjustment reshapes existing tests). The 1 inherited `SystemPromptUserRequestedTiebreakerTest` failure persists UNCHANGED. **Any new-failure delta beyond 1 inherited is a BLOCKING finding**.

  Note: the dev reported `1034 / 1-inherited / 0 / 2` (1028 + 6 = 1034). Verify; if your re-run gets a different count, investigate the delta.

- **Sub-gap-targeted spot-checks:**

  ```bash
  mvn test -Dtest=SkillLoaderTest -q
  mvn test -Dtest=SkillTest -q
  mvn test -Dtest=SkillRegistryTest -q
  ```

  Expected: SkillLoaderTest 18/0, SkillTest 9/0, SkillRegistryTest 11/0.

- **Behavioural-equivalence preservation:**

  ```bash
  mvn test -Dtest=PhaseEvaluatorSkillIntegrationTest -q
  ```

  Expected: 13/0 (UNCHANGED from Sprint 38 main-dev). The fix iteration did not touch the behavioural-equivalence test or the 4 Skill YAMLs, so this is a paranoid check confirming no ripple from the SkillLoader / Skill changes.

- **M1 functional-surface preservation:**

  ```bash
  mvn test -Dtest=Sprint71PartialIntakePersistenceTest -q
  ```

  Expected: 14/0.

- **No eval run required.** Per M2 §5 acceptance recalibration + the fix-iteration narrow scope.

## 6. OQ verification (4 informational items surfaced by dev)

Per fix handoff §7:

| OQ | Subject | Dev surfacing | Codex action |
|---|---|---|---|
| **OQ-FIX1.1** | VALID_TOOL_NAMES sourced from Java Tool implementations (`Tool.getName()`) instead of parsed YAML | Dev choice; cleaner Java-source-of-truth pattern. Future R-item to centralize the canonical tool-name source if it grows. | Codex confirms the sourcing is consistent + comprehensive; flags if any Tool implementation has a `getName()` return NOT in VALID_TOOL_NAMES (or vice versa). Otherwise informational only |
| **OQ-FIX1.2** | VALID_GUARDRAIL_TYPES uses Sprint 37 freeze §8.2 canonical names (e.g., `faq_miss_handover_requires_resolve_attempt`) over the prompt's informal abbreviations (e.g., `faq_miss_handover_threshold`) | Dev chose the freeze-canonical names — the correct authority. | Codex confirms the canonical names match design doc §8.2.1-§8.2.5 verbatim; if any whitelist entry doesn't match a freeze canonical name, surface as a finding (sub-gap #1d closure incomplete or incorrect). Otherwise informational only |
| **OQ-FIX1.3** | `UseCaseRegistryService` injection via SkillLoader constructor — no circular dependency | Verified at dev time; Spring bean graph clean | Codex spot-checks by reading SkillLoader constructor + verifying Spring `@Component` annotation pattern; informational only if clean |
| **OQ-FIX1.4** | Each test class re-inits a fresh `UseCaseRegistryService` (for test isolation); future R-item if test runtime becomes a concern | Acceptable for now; ~6 tests x ~ms re-init cost is negligible | Codex confirms the test pattern doesn't introduce flakiness; informational only |

If Codex CONFIRMS all 4 OQ resolutions: note in verdict header that fix iteration OQs are resolved. If Codex DISAGREES with any OQ resolution on substantive grounds (e.g., a Tool implementation is missing from VALID_TOOL_NAMES; a guardrail type is mis-named relative to the freeze): raise as a finding. Per §1 disagreement-routing precedent: only finding-on-fix-closure-correctness is `fix_required`; finding-on-OQ-housekeeping-or-future-improvement is `out_of_scope_review` + R-item.

## 7. Deferred / non-blocking observations

- The fix iteration did not touch PhaseEvaluator / 4 Skill YAML bodies / SkillRegistry / behavioural-equivalence integration test / Sprint 6/7/11 predicates / system_prompt.txt — all of these were reviewed in your prior round at `bd9d3f5` and confirmed PASS. **DO NOT re-walk these surfaces** in this fix-review round. If you have a concern on any of them, route as `out_of_scope_review` per `feedback_oq_on_pre_existing_surface_oosr_only_routing.md` — those passed prior review; re-litigating now would broaden scope beyond the fix iteration.
- C2 + C3 Tier-0 R-items (`R-skill-guardrail-non-overridability-tier-0` + `R-skill-state-bus-boundary-enforcement-tier-0`) remain open through Sprint 38 close; revisit at M3+ after Sprint 39 + Sprint 41 evidence. The fix iteration introduces no new Tier-0 candidate.
- The PhaseEvaluator post-Sprint-38 line-count discrepancy (handoff §1.6 claimed ~1500; actual via `wc -l` is 1529) noted in the prior review's Deferred / Non-Blocking Notes (§10) is unchanged in this fix iteration (PhaseEvaluator unchanged). Informational; not re-litigated.

## 8. Output format (write to `docs/codex-findings.md`)

Per `feedback_packaging_codex_findings_supersession.md`: delete-and-add the prior content (the `bd9d3f5` review verdict) to ensure no editorial drift. The deliver-agent will archive THIS file's content to `docs/sprints/sprint-038-codex-review.md` at Sprint 38 close (combining the `bd9d3f5` review + this fix review into one archive).

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph; Sprint 38 fix iteration #1 review at commit 5787806 closing Codex Blocking Finding 1 sub-gaps a-d>

## Review Evidence
<bullet list — review scope (fix commit 5787806 against bd9d3f5; 7 files / ~502 insertions / ~9 deletions); what you re-ran (Java baseline `mvn test -q`; SkillLoaderTest 18/0; SkillTest 9/0; SkillRegistryTest 11/0; PhaseEvaluatorSkillIntegrationTest 13/0 paranoid check; Sprint71PartialIntakePersistenceTest 14/0); what passed; what was independently verified (the 4 sub-gap closures + the 5+1 new tests + the SkillTest assertion adjustment + the Skill.java compact-constructor surgical edit + scope discipline)>

## Blocking Findings (if any)
<numbered list; each entry quotes the failing diff snippet + cited reference back to §3.1-§3.4 sub-gap verification, §2 scope-discipline, OR §4 §1.7 boundary>

## Sub-gap Closure Verification (§3)
<the §3 walk; PASS or FAIL per sub-gap (#1a tools_required presence + #1b tool-name allowlist + #1c explicit-UC registry check + #1d guardrail-type whitelist); cite test name + Skill.java/SkillLoader.java line range that closes each sub-gap>

## §1.7 Boundary Check (§4)
<the §4 walk; PASS or FAIL per surface (VALID_TOOL_NAMES + UseCaseRegistryService check + VALID_GUARDRAIL_TYPES + Skill.java compact-constructor surgical edit); flag any per-UC or per-phase branch table>

## Scope Discipline (§2)
<the §2 walk; PASS or FAIL; cite the 7-file expected list against `git diff bd9d3f5..5787806 --stat`>

## Validation Runs (§5)
<the §5 results; `mvn test -q` output 1034/1-inherited/0/2; targeted spot-checks per sub-gap>

## OQ Independent Verification (§6)
<the §6 walk; OQ-FIX1.1 + OQ-FIX1.2 + OQ-FIX1.3 + OQ-FIX1.4 confirmation or dissent>

## Deferred / Non-Blocking Notes (§7)
<the §7 items; pre-existing-surface routing reminder>
```

## 9. Expected verdict shape

If all 4 sub-gaps are correctly closed + scope discipline holds + Java baseline 1034/1-inherited/0/2 + OQ resolutions confirmed: **`decision: pass / blocking_count: 0`**. Sprint 38 fix iteration #1 close = Sprint 38 closes B-fix-iterated. Deliver-agent housekeeping bundle follows (archive both Codex reviews + flip R-items + draft Sprint 39 contract).

If §2 scope-discipline fails (e.g., fix touched PhaseEvaluator, an unmodified Skill YAML, or any unmodified `skill/` Java class): **`decision: fix_required`** with the violating file path quoted as Finding #1.

If any of the 4 sub-gaps is NOT correctly closed (e.g., VALID_TOOL_NAMES is missing a real Tool entry; UC registry check has a logic bug; guardrail-type whitelist uses non-freeze names; Skill.java compact-constructor regression on a non-`toolsRequired` field): **`decision: fix_required`** with the failing sub-gap + diff snippet quoted.

If §5 Java baseline regresses beyond 1 inherited: **`decision: fix_required`** with the failing test name(s) quoted.

If Codex DISAGREES with an OQ resolution on substantive grounds (e.g., a guardrail type is mis-named): treat as a sub-gap closure failure → **`decision: fix_required`**. Otherwise (e.g., suggesting a future centralization R-item): **`decision: out_of_scope_review`** + open R-item, BUT Sprint 38 still closes B-fix-iterated separately if the 4 sub-gaps are otherwise correctly closed (use the OOSR-with-packaging-note pattern per `feedback_out_of_scope_review_packaging_rollforward.md`).

If Codex SURFACES a concern on the OLD surfaces (PhaseEvaluator, Skill YAMLs, behavioural-equivalence integration test, etc. — unchanged in this fix iteration): **`decision: out_of_scope_review`** per `feedback_oq_on_pre_existing_surface_oosr_only_routing.md` + open R-item for M2 close fold-back or Sprint 39 planning round consideration. **Do NOT classify as `fix_required`** — those surfaces passed prior review at `bd9d3f5`; re-litigating now would expand fix-iteration scope.

## 10. Self-check before submitting

- [ ] §2 scope-discipline gate walked; 7 files verified against `git diff bd9d3f5..5787806 --stat`.
- [ ] §3 sub-gap closure verification per #1a / #1b / #1c / #1d (4 sub-gaps); cited test name + implementation file:line for each closure.
- [ ] §4 §1.7 boundary check on the 3 new allowlists + the Skill.java surgical edit; verified flat-membership Sets / not branch tables.
- [ ] §5 Java baseline re-run from clean checkout; 1034/1-inherited/0/2 byte-identical; targeted spot-checks pass.
- [ ] §6 OQ independent verification (OQ-FIX1.1-OQ-FIX1.4); confirmation or dissent per OQ.
- [ ] §7 deferred items noted as non-blocking; pre-existing-surface routing reminder honored.
- [ ] `docs/codex-findings.md` written per §8 format (delete-and-add supersession of prior `bd9d3f5` review verdict).
- [ ] Verdict per §9 expected shape.
- [ ] **Did NOT re-walk the full §4.1 9-question kernel on surfaces that did NOT change in this fix iteration** (PhaseEvaluator integration, 4 Skill YAML bodies, SkillRegistry.select semantics, behavioural-equivalence golden assertions, Sprint 6/7/11 predicate preservation, Sprint 33 cue/slot split, MATERIAL FINDING preservation, premise re-verification) — those passed prior review at `bd9d3f5` and remain valid.
