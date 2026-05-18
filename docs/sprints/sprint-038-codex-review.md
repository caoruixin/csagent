---
title: Sprint 38 Codex per-sub-sprint review archive — SkillRegistry core + 4 simpler phase Skills migration (NEW M2 sub-sprint 2) + fix iteration #1 (SkillLoader schema-validation completeness)
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 38 fired per-sub-sprint Codex review per `iteration_governance.md` §4.3 trigger #3 (new architectural surface — SkillRegistry mediates between LLM prompt context and tool dispatch). The review ran across TWO rounds per the B-fix-iteration close pattern: an initial round against the Sprint 38 main-dev commit `bd9d3f5` (verdict: `fix_required / blocking_count: 1` — Codex Blocking Finding 1 on `SkillLoader` schema-validation completeness for design doc §2.2) and a fix-review round against the Sprint 38 fix iteration #1 commit `5787806` (verdict: `pass / blocking_count: 0` — Finding 1's 4 sub-gaps all closed). This archive carries BOTH rounds verbatim per `feedback_packaging_codex_findings_supersession.md` delete-and-add supersession pattern. Sprint 38 final classification: **A — Sprint 38 closes B-fix-iterated** (equivalent to Sprint 23/25 fix-iteration close pattern; second instance under the NEW M2 framing after Sprint 37's clean A close; first implementation sub-sprint of NEW M2 — closes with a single targeted fix iteration on freeze-fidelity).
---

# Sprint 38 Codex per-sub-sprint review archive

Sprint 38 closes **B-fix-iterated** 2026-05-17. The Codex per-sub-sprint review fired in two rounds (`bd9d3f5` initial review → `5787806` fix-review). Both round verdicts preserved verbatim below.

## Round 1 — initial review of `bd9d3f5` (Sprint 38 main-dev commit)

```
## Sprint Review Decision
decision: fix_required
blocking_count: 1
summary: Sprint 38 is correctly scoped as a SkillRegistry implementation wave and the Java/YAML migration itself preserves behavioural equivalence for the four migrated phases, with no §1.7 semantic hardcode found in Skill bodies, registry selection, or PhaseEvaluator composition. However, `SkillLoader` does not fully implement the Sprint 37 freeze's fail-fast schema validation contract for the Skill data model: required `tools_required` presence cannot be detected after `Skill` normalizes null lists to `List.of()`, and the loader does not validate `tools_required` tool names, explicit `applicable_use_cases`, or guardrail `type` values against the frozen schema. Because decision (a) §2.2 is one of Sprint 38's in-scope freeze decisions, this is a blocking schema/freeze-fidelity finding requiring a fix before Sprint 38 closes.

## Review Evidence
- Reviewed Sprint 38 implementation commit `bd9d3f5` with parent `649c6c7`; `git show --stat bd9d3f5` / `git diff 649c6c7..bd9d3f5 --stat` show the dev commit touches the expected 34 files: 5 new Skill Java classes, 4 new Skill YAMLs, `PhaseEvaluator.java`, 4 new Skill tests plus `SkillTestFixtures`, `PhaseEvaluatorSkillIntegrationTest.java`, 19 constructor-only test updates, and `docs/sprints/sprint-038-handoff.md`.
- Also ran the prompt-requested `git diff 51c327c..bd9d3f5 --stat`; that wider range includes the Sprint 37 close / Sprint 38 planning bundle at `649c6c7` (`docs/sprint_objective.md`, `docs/action_bank.md`, `docs/sprints/sprint-037-*`, `compact/sprint-038-dev-prompt.md`, etc.). Those files are deliver-agent-owned in `649c6c7`, not part of the dev commit `bd9d3f5`, so scope discipline was assessed against the dev commit parent.
- Loaded the governance chain (`AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`) plus NEW M2 `docs/milestone_objective.md`, Sprint 38 `docs/sprint_objective.md`, `docs/sprints/sprint-038-handoff.md`, Sprint 37 freeze `docs/proposals/skill_registry_design.md`, Sprint 37 objective/handoff/Codex review, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/faq_grounding_contract.md`, `docs/action_bank.md`, and `compact/sprint-038-dev-prompt.md`.
- Inspected Sprint 38 implementation surfaces: `Skill.java`, `Guardrail.java`, `StateInheritance.java`, `SkillLoader.java`, `SkillRegistry.java`, all four Skill YAMLs, `PhaseEvaluator.java`, `PhaseEvaluatorSkillIntegrationTest.java`, the Skill unit tests, `AgentRunLoopImpl.java`, `ToolDispatcher.java`, `PhasePlan.java`, and representative constructor-call-site diffs.
- Re-ran the full Java suite with `cd server && mvn test -q`; surefire XML totals are `Tests run: 1028, Failures: 1, Errors: 0, Skipped: 2`, with only the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` failure.
- Re-ran Sprint 38 targeted tests: `SkillTest` 8/0, `SkillRegistryTest` 11/0, `SkillLoaderTest` 13/0, and `PhaseEvaluatorSkillIntegrationTest` 13/0.
- Re-ran targeted regressions for Sprint 38 hard fences: `Sprint71PartialIntakePersistenceTest` 14/0, `Sprint11ProgressiveResolveTest` 14/0, `Sprint7IntakeStateTest` 16/0, `Sprint7CandidateUseCasesProjectionTest` 7/0, `PhaseEvaluatorPlanTest` 31/0, `PhaseEvaluatorFaqMissFallbackTest` 10/0, `PhaseEvaluatorMaxStepsResolverTest` 6/0, `PhaseEvaluatorQueryEnrichmentTest` 15/0, `Sprint9TerminalToolHonestyTest` 4/0, `Sprint9TraceObservabilityFidelityIntegrationTest` 3/0, `Sprint24DeadlinePlaceholderCoalesceTest` 3/0, and `Sprint81PhaseEvaluatorUseCaseIdentifiedTest` 2/0.
- Independently verified Sprint 37 freeze fidelity for decisions (b), (c), and (e §6.2.1-§6.2.4), behavioural-equivalence golden-string preservation for the migrated phases, unchanged Sprint 6/7/11 predicates in `AgentRunLoopImpl.java`, Sprint 33 cue/slot split preservation, and premise re-verification (`system_prompt.txt` 101 lines, `IntakeFieldsRegistry.java` 222 lines, `UseCaseDefinition` still 6 fields).

## Blocking Findings
1. `SkillLoader` does not enforce the frozen Skill schema contract for required `tools_required` presence and canonical Skill-scope values. Sprint 37 freeze §2.2 requires fail-fast validation for missing required fields including `tools_required`, `applicable_use_cases` values not in the UC registry, `tools_required` values not in the canonical tool-name set, and `guardrails[].type` values not matching a known dispatcher predicate type. The Sprint 38 implementation cannot detect a missing `tools_required` field because `Skill.java:63-70` normalizes a null `toolsRequired` constructor argument to `List.of()` before `SkillLoader.validate(...)` runs, making the null check at `SkillLoader.java:152-157` unreachable. The same validator has no loop over `skill.toolsRequired()` to reject unknown tool names, no registry/allowlist check for explicit non-wildcard `applicable_use_cases`, and no known-type check for `Guardrail.type()` at `SkillLoader.java:204-208` (only `on_fail` is validated). This violates in-scope decision (a) §2.2 / Sprint 38 schema-validation acceptance and can let malformed Skill YAML boot successfully instead of failing fast.

## Anti-Hardcode Kernel (§3)
- Q1 keyword / regex / if-else / enum / per-UC matrix: PASS; agree with handoff §6. `SkillRegistry.select(...)` is exact-then-wildcard lookup, `composeSkillPhasePlan(...)` projects fields without UC branching, all four Sprint 38 YAMLs use `applicable_use_cases: ["*"]`, and `state_inheritance` is state-dimension-keyed.
- Q2 Tier-0 justification: N/A; Sprint 38 adds no Tier-0 invariant and `docs/runtime_freeze_and_risk_policy.md` is unchanged. C2/C3 remain deferred R-items.
- Q3 soft signal achievable: N/A / PASS; Sprint 38 is a refactor and preserves existing Sprint 31/33 projection slots. `state_inheritance.soft_signal_via_projection` declares existing slots for future Sprint 41 state-bus work but does not enforce new semantic logic.
- Q4 visible eval / trace phrasing / CaseSpec id: PASS; no Skill YAML contains Alice, CaseSpec ids, bad-case text, or trace-specific wording. The procedure text is legacy PhaseEvaluator content.
- Q5 semantic ownership shift LLM to Java: PASS; Skill `procedure`, `grounding_instruction`, and `escalation_policy` remain LLM-soft, while `tools_required` maps to the existing Runtime-owned `PhasePlan.allowedTools` boundary. Blocking Finding 1 is schema fail-fast fidelity, not an LLM-ownership shift.
- Q6 prompt as if-else dump: PASS; the four Skill procedure bodies are principle-level migrated phase teaching. `system_prompt.txt:54-101` is unchanged and inherits the Sprint 37 OQ-7.11 verdict.
- Q7 tool schema / capability / PII / grounding floor: PASS for production runtime semantics; existing `ToolDispatcher.validateAgainstPlan` still enforces `PhasePlan.allowedTools`, tool schemas/PII/safety/grounding surfaces are unchanged, and S1/S2 predicates are Sprint 39 scope. The loader schema-validation gap is captured separately as Blocking Finding 1.
- Q8 generalization coverage: PASS; 12 migrated-phase behavioural-equivalence tests plus one RESOLVE-INTAKE preservation test pass, SkillLoader has 12 negative cases, and targeted neighbor regressions pass. Shadow/eval runs remain observation-only per M2 §5.
- Q9 rollback / sunset: N/A; Skill Registry is permanent. A bad Sprint 38 migration remains revertible via `git revert bd9d3f5`, but the intended abstraction has no sunset.

## §1.7 Boundary Check (§4)
- 4 Skill YAML `procedure` bodies: PASS. `discover_triage.yaml`, `confirm.yaml`, `escalate.yaml`, and `terminal.yaml` contain legacy phase instructions, no raw eval phrases, no CaseSpec ids, and no bad-case text.
- `applicable_use_cases`: PASS. All four Sprint 38 YAMLs declare wildcard `"*"`; no specific-UC list or per-UC branch table ships in Sprint 38.
- `state_inheritance`: PASS. Blocks use `inherit`, `reset`, and `soft_signal_via_projection` with state/projection slot names only; no prior-UC/new-UC pair branch appears.
- `guardrails`: PASS. All four Sprint 38 YAMLs declare `guardrails: []`; concrete guardrails and dispatcher enforcement are Sprint 39 scope.
- `composeSkillPhasePlan(...)`: PASS. `PhaseEvaluator.java:548-566` directly maps Skill fields into `PhasePlan` fields and uses only a null-coalescing `maxToolSteps` default of 2; no UC enum branch or template substitution exists.
- `SkillRegistry.select(...)`: PASS. `SkillRegistry.java:65-84` implements exact UC lookup, wildcard phase lookup, then `Optional.empty()` fallback; this is structural lookup, not semantic branching.
- `Skill.appliesTo(...)`: PASS. `Skill.java:115-122` checks phase membership plus exact UC or wildcard membership; no per-UC enum routing.
- `SkillLoader.validate(...)`: PASS for §1.7 content boundary, but FAILS schema/freeze fidelity per Blocking Finding 1. Validation is load-time Runtime configuration enforcement, not an LLM semantic hardcode.
- Deleted legacy branches: PASS. The four migrated DISCOVER / CONFIRM / CLOSE / ESCALATE branches were relocated to YAML and pinned by golden tests; content is not redesigned.
- Preserved RESOLVE branches: PASS. `git diff --unified=0 51c327c..bd9d3f5 -- PhaseEvaluator.java` shows constructor/import/dispatch/helper additions and deletions of the four migrated branches only; the RESOLVE-INTAKE and RESOLVE-FAQ branch bodies are not edited.

## Hard-Fence Verification (§5)
- Scope discipline: PASS for dev commit `bd9d3f5`. The dev-only diff (`649c6c7..bd9d3f5`) matches Sprint 38 file scope: Skill package, 4 Skill YAMLs, PhaseEvaluator integration, tests, and handoff only. The wider `51c327c..bd9d3f5` range includes deliver-agent commit `649c6c7`; those deliver-agent-owned files are not attributable to the Sprint 38 dev commit.
- Sprint 38 §6 fences #1-#3: PASS. `AgentRunLoopImpl.java`, `system_prompt.txt`, and the RESOLVE branch bodies are unchanged; Sprint 39/40 surfaces are not touched.
- Sprint 38 §6 fences #4-#6: PASS. No concrete S1/S2 predicates, no `SkillGuardrailDispatcher.java`, no `SkillStateBus.java`, and no `ContextProjectionBuilder.java` edit. `prior_use_case_carry` appears only as an allowed future projection-slot token in the `state_inheritance` schema comments/validation set, not as a new projected slot.
- Sprint 38 §6 fences #7-#13: PASS. No `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` touch; no `INTAKE_UCS` or escalation enum change; no Tier-0/governance/foundational/current-doc/freeze-doc edit.
- Sprint 38 §6 fences #14-#23: PASS. No sprint-001 through sprint-037 archive edit in the dev commit; no milestone archive, deliver-agent-owned doc, `docs/action_bank.md`, `docs/codex-findings.md`, compact prompt, or eval surface edit in `bd9d3f5`.
- Sprint 38 §6 fences #24-#31: PASS except the schema issue captured separately. Java tests, not mocked-LLM eval, are primary evidence; no per-UC YAML branch, no prescribed customer language, no hardcoded per-step argument values, no M1 functional-surface regression, no CLOSE→TERMINAL enum rename, and no Sprint 39/40/41 pre-implementation.
- M2 §6 fences: PASS. No per-UC-branch Skill body, no predicate migration, no S1 grounding-floor extension in Sprint 38, no drift/router/classifier touch, no `INTAKE_UCS` edit, no escalation enum widening, no Tier-0 policy edit, no eval/governance/foundational/archive deletion, and OLD `docs/proposals/skill_foundation_design.md` remains superseded in-place.
- Behavioural equivalence: PASS. `PhaseEvaluatorSkillIntegrationTest.java` has 12 migrated-phase golden assertions plus one RESOLVE-INTAKE legacy check; the target test class passes 13/0. The DISCOVER golden text matches the parent `51c327c` branch, including Sprint 7 weak-candidate and Sprint 33 ad-status cues.
- Sprint 37 freeze fidelity: PASS for decisions (b), (c), and (e §6.2.1-§6.2.4); FAIL for decision (a) §2.2 schema-validation completeness per Blocking Finding 1.
- MATERIAL FINDING preservation: PASS. `AgentRunLoopImpl.java` is unchanged; Sprint 7 `shouldRejectIncompleteIntakeHandover`, Sprint 6 `shouldRejectFaqMissHandover`, Sprint 11 `shouldRejectPrematureResolveOutcome`, and their dispatch sites remain intact for Sprint 39 migration.
- Sprint 33 cue/slot split: PASS. The cue from legacy `PhaseEvaluator.java:432-448` is migrated into `discover_triage.yaml`; the `system_prompt.txt:36-43` slot teaching block is unchanged for Sprint 40.
- Premise re-verification: PASS. `system_prompt.txt` is 101 lines; `UseCaseDefinition` still has 6 fields and no `requiredIntakeFields`; `IntakeFieldsRegistry.java` is 222 lines and remains the required-fields source of truth.

## Schema And Reproducibility Checks (§6)
- Skill data model: PARTIAL / FIX REQUIRED. `Skill.java` provides the expected 15-field record signature (12 primary fields plus supporting records / optional PhasePlan fields) with snake_case `@JsonCreator` mapping and immutable collection normalization, but that normalization prevents `SkillLoader` from detecting absent `tools_required` as required by design doc §2.2.
- Guardrail model: PARTIAL / FIX REQUIRED. `Guardrail.java` enforces nonblank `type` and `on_fail`, and `SkillLoader` validates `on_fail`, but `SkillLoader` does not validate `type` against known predicate types as required by design doc §2.2 / §5.2.
- StateInheritance model: PASS. `StateInheritance.java` matches `inherit` / `reset` / `soft_signal_via_projection` schema and `SkillLoader` validates state keys and projection-slot names.
- SkillRegistry shape: PASS. `SkillRegistry.java` is a Spring component, indexes wildcard and exact maps, exposes `select(...)`, `allSkills()`, and `skillsForPhase()`, and returns `Optional.empty()` on miss for the Sprint 38→39 migration window.
- SkillLoader shape: PARTIAL / FIX REQUIRED. It loads `classpath:/skills/*.yaml` and validates phases, wildcard mixing, terminal outcomes, state keys, projection slots, and guardrail `on_fail`; it does not fully satisfy the frozen §2.2 validation contract for required `tools_required`, tool names, explicit UC names, or guardrail types.
- PhaseEvaluator integration: PASS. Constructor is 10→11 args, SkillRegistry-first dispatch happens at `PhaseEvaluator.java:398-407`, legacy RESOLVE fallback remains, and `composeSkillPhasePlan(...)` exists at `PhaseEvaluator.java:548-566`.
- 4 Skill YAML schemas: PASS for production files. Each has required field content, wildcard use-case scope, valid phase values, valid terminal outcomes, existing tool names, `guardrails: []`, and state-inheritance blocks matching Sprint 38 templates.
- Test file structure: PASS. New test counts are `SkillTest` 8, `SkillRegistryTest` 11, `SkillLoaderTest` 13, and `PhaseEvaluatorSkillIntegrationTest` 13 = 45 new Sprint 38 tests.
- PhaseEvaluator line count: REPRODUCIBILITY NOTE. Actual `wc -l` is 1529, not the handoff's approximate 1500; this is non-blocking because the substantive diff (four branch deletions + dispatch/helper additions) is verified.
- Cited file:line spot-checks: PASS. Reproduced `PhaseEvaluator.java:30-66`, `AgentRunLoopImpl.java:628-651`, `AgentRunLoopImpl.java:690-734`, `AgentRunLoopImpl.java:745-759`, `system_prompt.txt:54-101`, `ToolDispatcher.java:183-195`, and `PhasePlan.java:21-32`.
- Handoff structure: PASS. `docs/sprints/sprint-038-handoff.md` has the contracted 12 sections and leaves §12 as the closure-verdict placeholder.

## Validation Runs (§7)
- `cd server && mvn test -q` exited with the documented inherited failure only.
- Surefire aggregate from `server/target/surefire-reports/TEST-*.xml`: `1028` tests, `1` failure, `0` errors, `2` skipped.
- The failing test is `com.gumtree.csagent.service.runtime.SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`, matching the Sprint 37/38 inherited baseline.
- Sprint 38 new tests pass: `SkillTest` 8/0, `SkillRegistryTest` 11/0, `SkillLoaderTest` 13/0, `PhaseEvaluatorSkillIntegrationTest` 13/0.
- Targeted regressions pass: `Sprint71PartialIntakePersistenceTest` 14/0, `Sprint11ProgressiveResolveTest` 14/0, `Sprint7IntakeStateTest` 16/0, `Sprint7CandidateUseCasesProjectionTest` 7/0, `PhaseEvaluatorPlanTest` 31/0, `PhaseEvaluatorFaqMissFallbackTest` 10/0, `PhaseEvaluatorMaxStepsResolverTest` 6/0, `PhaseEvaluatorQueryEnrichmentTest` 15/0, `Sprint9TerminalToolHonestyTest` 4/0, `Sprint9TraceObservabilityFidelityIntegrationTest` 3/0, `Sprint24DeadlinePlaceholderCoalesceTest` 3/0, and `Sprint81PhaseEvaluatorUseCaseIdentifiedTest` 2/0.
- No eval or bad-case suite run was required; M2 §5 makes those observations, not Sprint 38 hard gates.

## Tier-0 Candidate Continuation (§8)
- C1 Skill tool-whitelist enforcement unconditional: CONFIRM `REJECTED as new candidate`. `composeSkillPhasePlan(...)` maps `skill.toolsRequired()` into `PhasePlan.allowedTools`, and `AgentRunLoopImpl.java:293-304` still calls `ToolDispatcher.validateAgainstPlan(...)` at `ToolDispatcher.java:183-195` before dispatch. Blocking Finding 1 is about malformed YAML fail-fast, not a new Tier-0 invariant.
- C2 Skill terminal predicate refusal non-overridable by LLM: CONFIRM `QUALIFIED -> DEFER`. Sprint 38 ships `guardrails: []`; the dispatcher evidence surface is Sprint 39, so the Sprint 37 deferral remains sound. Keep `R-skill-guardrail-non-overridability-tier-0` open for M3+ / M2-close re-evaluation after Sprint 39 evidence.
- C3 Skill `state_inheritance` enforced at session-state-bus boundary: CONFIRM `QUALIFIED -> DEFER`. Sprint 38 populates declarations only; no `SkillStateBus.java` or projection-slot enforcement ships. Keep `R-skill-state-bus-boundary-enforcement-tier-0` open until Sprint 41 evidence.
- C4 S1 `must_cite_source` predicate semantics: CONFIRM `NOT A CANDIDATE` / N/A for Sprint 38. S1 remains Sprint 39 scope under the bounded M2 §6 #4 authorization.
- C5 S2 `intake_complete_required` predicate semantics: CONFIRM `NOT A CANDIDATE` / N/A for Sprint 38. S2 predicate semantics remain the existing Sprint 7 surface until Sprint 39 relocation.
- No new Tier-0 candidate is surfaced from the Sprint 38 implementation diff. The schema validation gap is a Sprint 38 freeze-fidelity bug, not a Tier-0 elevation question.

## OQ Independent Verification (§9)
- OQ-S38.1 DISCOVER template vs legacy text: AGREE WITH DEV. The implementation correctly chose legacy-verbatim text from parent `51c327c` to satisfy design doc §6.4 behavioural equivalence; the design doc §6.2.1 wording differences are editorial template drift, not a behaviour-change decision. Per `doc_governance.md` code-ahead-of-docs, any fold-back should be a future governance housekeeping item, not a Sprint 38 code rewrite.
- OQ-S38.2 composer placement: informational agreement. Keeping `composeSkillPhasePlan(...)` private inside `PhaseEvaluator` matches design doc §4.1 and is reasonable for the current helper size.
- OQ-S38.3 `maxToolSteps` literal 2 fallback: informational agreement with deferral. All Sprint 38 Skills set `max_tool_steps: 2`, so the fallback does not affect shipped behaviour; Sprint 39 should keep explicit values for RESOLVE Skills.
- OQ-S38.4 `SkillTestFixtures.productionRegistry()` coupling: informational agreement. Using production YAMLs in existing tests is acceptable for behavioural equivalence and mirrors existing registry-data coupling patterns.
- OQ-S38.5 C2/C3 continuation: CONFIRM. Sprint 38 adds no dispatcher or state-bus evidence that would justify re-evaluation.

## Deferred / Non-Blocking Notes (§10)
- PhaseEvaluator line-count discrepancy is non-blocking: handoff says approximately 1500, actual is 1529. The code-bearing changes are independently verified.
- Skill record 15-vs-12 field framing is non-blocking: the implementation signature includes the frozen primary fields plus optional PhasePlan/supporting fields; the schema-validation bug is the only field-model blocker.
- Behavioural-equivalence coverage is adequate for Sprint 38: 3 representative UC states across each migrated phase plus a RESOLVE-INTAKE preservation check.
- C2 and C3 R-items remain open through Sprint 38; re-evaluate after Sprint 39 dispatcher and Sprint 41 state-bus evidence.
- Sprint 38 acceptance rows for wrong-containment and over-escalation are correctly N/A; Java behavioural equivalence is the hard gate and real-LLM eval remains observation-only for M2.
- Architecture-health tracking is positive aside from the schema bug: `new_semantic_hardcode_count = 0`, `soft_signal_conversion_count = 4`, planner ownership is unchanged, and no shadow measurement is required for this architectural sub-sprint.
```

## Round 2 — fix-review of `5787806` (Sprint 38 fix iteration #1)

```
## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 38 fix iteration #1 at commit `5787806` correctly closes Codex Blocking Finding 1 from `bd9d3f5`: the missing `tools_required` presence gap is now reachable via the surgical `Skill` null-passthrough, `SkillLoader` now rejects unknown tool names, explicit unknown UCs, and unknown guardrail predicate types, and the fix remains inside the authorized 7-file scope. Java baseline is preserved at `1034 / 1 inherited failure / 0 errors / 2 skipped`; targeted Skill and preservation checks pass; fix iteration OQs OQ-FIX1.1 through OQ-FIX1.4 are resolved as informational only.

## Review Evidence
- Reviewed only the fix-iteration diff `bd9d3f5..5787806`; `git diff bd9d3f5..5787806 --stat` is exactly 7 files, `502 insertions(+)`, `9 deletions(-)`, matching the authorized fix surfaces.
- Re-read the prior `docs/codex-findings.md` Blocking Finding 1 contract, `docs/sprints/sprint-038-fix-handoff.md`, Sprint 38 context, `docs/proposals/skill_registry_design.md` §2.2 / §5.2 / §8.2, `docs/sprint_objective.md` §6 fences, and `compact/sprint-038-fix-dev-prompt.md`.
- Verified sub-gap #1a in `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java:29` and `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java:69`: only `toolsRequired` now preserves null until loader validation; other list fields still normalize defensively.
- Verified sub-gaps #1b/#1c/#1d in `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:95`, `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:136`, `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:229`, `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:241`, and `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:282`.
- Verified test coverage: `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java:269`, `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java:288`, `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java:309`, `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java:329`, and `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java:348` cover the four sub-gaps plus the positive UC companion.
- Verified `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java:70` now asserts null-passthrough for missing `toolsRequired`, and `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java:90` confirms explicit empty `tools_required: []` remains a non-null empty list.
- Re-ran validation: `cd server && mvn test -q` produced `Tests run: 1034, Failures: 1, Errors: 0, Skipped: 2`, with only inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`; targeted checks were `SkillLoaderTest` 18/0, `SkillTest` 9/0, `SkillRegistryTest` 11/0, `PhaseEvaluatorSkillIntegrationTest` 13/0, and `Sprint71PartialIntakePersistenceTest` 14/0.

## Blocking Findings (if any)
None.

## Sub-gap Closure Verification (§3)
- PASS #1a — `tools_required` presence detection. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java:69` keeps `toolsRequired == null` as null while preserving null-to-empty normalization for `applicablePhases`, `applicableUseCases`, `requiredContextKeys`, `validTerminalOutcomes`, and `guardrails`. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:203` rejects null with `tools_required is required (may be empty list)`. `parseAndValidate_missingToolsRequiredField_failsFast` passes, and `SkillTest` confirms null-passthrough plus explicit-empty-list preservation.
- PASS #1b — tool-name allowlist. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:95` lists the 11 Java `Tool.getName()` values: `search_knowledge`, `resolve_article`, `classify_use_case`, `record_outcome`, `request_handover`, `create_case_controlled`, `get_customer_context`, `lookup_listing_or_ad`, `lookup_customer_account`, `get_moderation_review_context`, and `get_message_moderation_context`. Independent grep of `server/src/main/java/com/gumtree/csagent/service/tools/*Tool.java` found the same 11 names and no extras. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:241` validates after the null check, so empty lists pass and unknown names fail. `parseAndValidate_unknownToolName_failsFast` passes.
- PASS #1c — explicit `applicable_use_cases` UC registry check. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:136` constructor-injects `UseCaseRegistryService`, whose `isKnownUseCase(...)` API is at `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java:109`. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:229` validates every non-wildcard UC uniformly against the registry. `parseAndValidate_explicitUnknownUseCase_failsFast` rejects `UC-NONEXISTENT`, and `parseAndValidate_explicitKnownUseCase_passes` accepts `UC-A`.
- PASS #1d — guardrail-type whitelist. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:117` matches the Sprint 37 freeze §5.2 / §8.2 canonical predicate names exactly: `faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome_guard`, and `must_cite_source`. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:282` validates `Guardrail.type()` before the existing `on_fail` check. `parseAndValidate_unknownGuardrailType_failsFast` passes.

## §1.7 Boundary Check (§4)
- PASS `VALID_TOOL_NAMES`: the set is a flat runtime-floor membership allowlist sourced from Java `Tool.getName()` implementations; it does not vary by UC, phase, eval phrasing, or customer scenario.
- PASS `UseCaseRegistryService` check: explicit UC validation is a uniform registry-membership check using the existing service; it does not encode phase-specific behavior or per-UC semantic routing.
- PASS `VALID_GUARDRAIL_TYPES`: the set is a flat predicate-type whitelist from the Sprint 37 freeze; it names structural Runtime-owned floor predicates and does not encode per-UC-pair or per-phase branch logic.
- PASS `Skill.java` surgical edit: null-passthrough applies only to `toolsRequired` to enable required-field detection; no LLM-owned semantic decision moved into Java, and no prompt or Skill YAML content changed.

## Scope Discipline (§2)
- PASS. `git diff bd9d3f5..5787806 --stat` shows exactly the expected 7 files: `docs/sprints/sprint-038-fix-handoff.md`, `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java`, `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java`, `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java`, `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java`, `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java`, and `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java`.
- No prohibited old surface appears in the fix diff: no `PhaseEvaluator.java`, no 4 Skill YAMLs, no `SkillRegistry.java` / `Guardrail.java` / `StateInheritance.java`, no `PhaseEvaluatorSkillIntegrationTest.java`, no PhaseEvaluator constructor-call-site sweep, no `AgentRunLoopImpl.java`, no `system_prompt.txt`, no `ContextProjectionBuilder.java`, no `IntakeFieldsRegistry.java`, no `UseCaseRegistryService.java`, no governance/foundational/proposal docs, no deliver-agent-owned active docs, and no `eval_interactive/**` files.

## Validation Runs (§5)
- `cd server && mvn test -q`: expected non-zero exit from the inherited failure only; surefire aggregate is `1034` tests, `1` failure, `0` errors, `2` skipped. The only failing test is `com.gumtree.csagent.service.runtime.SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`.
- `mvn test -Dtest=SkillLoaderTest#parseAndValidate_missingToolsRequiredField_failsFast -q`: pass.
- `mvn test -Dtest=SkillLoaderTest#parseAndValidate_unknownToolName_failsFast -q`: pass.
- `mvn test -Dtest=SkillLoaderTest#parseAndValidate_explicitUnknownUseCase_failsFast -q`: pass.
- `mvn test -Dtest=SkillLoaderTest#parseAndValidate_unknownGuardrailType_failsFast -q`: pass.
- `mvn test -Dtest=SkillLoaderTest -q`: `18` tests, `0` failures, `0` errors, `0` skipped.
- `mvn test -Dtest=SkillTest -q`: `9` tests, `0` failures, `0` errors, `0` skipped.
- `mvn test -Dtest=SkillRegistryTest -q`: `11` tests, `0` failures, `0` errors, `0` skipped.
- `mvn test -Dtest=PhaseEvaluatorSkillIntegrationTest -q`: `13` tests, `0` failures, `0` errors, `0` skipped.
- `mvn test -Dtest=Sprint71PartialIntakePersistenceTest -q`: `14` tests, `0` failures, `0` errors, `0` skipped.
- No eval run was required for this narrow load-time schema-validator fix iteration.

## OQ Independent Verification (§6)
- OQ-FIX1.1 — resolved informationally. `VALID_TOOL_NAMES` mirrors all 11 Java `Tool.getName()` return values and has no missing or extra entries. Future centralization of the canonical tool-name source remains a reasonable R-item, but it is not a Sprint 38 blocker.
- OQ-FIX1.2 — resolved informationally. `VALID_GUARDRAIL_TYPES` uses the freeze-canonical §8.2 names, including `premature_resolve_outcome_guard`, rather than informal prompt abbreviations. No whitelist entry is outside the freeze set.
- OQ-FIX1.3 — resolved informationally. `SkillLoader` remains a Spring `@Component`, `UseCaseRegistryService` is a Spring `@Service` with no dependency on `SkillLoader` or `SkillRegistry`, and the graph is linear: `UseCaseRegistryService` → `SkillLoader` → `SkillRegistry`.
- OQ-FIX1.4 — resolved informationally. Test fixtures construct initialized `UseCaseRegistryService` instances in `SkillLoaderTest`, `SkillRegistryTest`, and `SkillTestFixtures`; this is deterministic classpath YAML initialization and introduces no observed flakiness in the targeted runs.

## Deferred / Non-Blocking Notes (§7)
- I did not re-walk the full §4.1 nine-question kernel on old Sprint 38 surfaces that did not change in this fix iteration. PhaseEvaluator integration, the 4 Skill YAML bodies, SkillRegistry selection semantics, behavioural-equivalence golden assertions, Sprint 6/7/11 predicate preservation, Sprint 33 cue/slot split, MATERIAL FINDING preservation, and premise re-verification remain governed by the prior `bd9d3f5` review.
- Any future concern on those old surfaces should route as `out_of_scope_review` with an R-item for M2 close or Sprint 39 planning, not as this fix iteration's `fix_required`.
- C2 and C3 Tier-0 R-items remain open through Sprint 38 close for re-evaluation after Sprint 39 dispatcher evidence and Sprint 41 state-bus evidence; this fix introduces no new Tier-0 candidate.
- The prior PhaseEvaluator line-count discrepancy note is unchanged and not re-litigated because `PhaseEvaluator.java` is outside the fix diff.
```

---

## Cumulative close summary (deliver-agent + human, 2026-05-17)

**Classification:** A — Sprint 38 closes B-fix-iterated. Single fix iteration on the SkillLoader schema-validation completeness gap; clean fix-review PASS verdict at `5787806`. Sprint 38 ships the SkillRegistry core abstraction + 4 simpler phase Skills externalized + behavioural-equivalence preserved + full §2.2 schema validation post-fix.

**Java baseline trajectory:** 983 (post-Sprint-37) → 1028 (post-Sprint-38 main-dev `bd9d3f5`) → 1034 (post-Sprint-38 fix `5787806`). Inherited `SystemPromptUserRequestedTiebreakerTest` failure persists unchanged through both commits.

**OQs disposed:**
- OQ-S38.1 (template-vs-legacy editorial divergence): AGREE WITH DEV (legacy-verbatim per `doc_governance.md` code-ahead-of-docs). Codex CONFIRMED. Editorial fold-back at M2 close per `doc_governance.md` cadence.
- OQ-S38.2 (composer placement): informational.
- OQ-S38.3 (`maxToolSteps` literal-2 default): deferred to Sprint 39 planning (RESOLVE Skills will set explicit values).
- OQ-S38.4 (`SkillTestFixtures.productionRegistry()` coupling): informational.
- OQ-S38.5 (C2 + C3 R-items continuation): CONFIRM; carry forward unchanged.
- OQ-FIX1.1 (centralize canonical tool-name source): opened as new R-item `R-skill-tool-name-canonical-source-centralization` in `docs/action_bank.md` §5.2 for M3+ revisit.
- OQ-FIX1.2 (freeze-canonical predicate names over informal abbreviations): CONFIRM.
- OQ-FIX1.3 (UseCaseRegistryService injection no circular dependency): CONFIRM.
- OQ-FIX1.4 (test runtime concern on UseCaseRegistryService re-init): informational; not opened as R-item per deliver-agent + human 2026-05-17 close decision (micro-optimization; not load-bearing).

**Tier-0 candidate disposition:** No new Tier-0 candidate surfaced from Sprint 38. C1 REJECTED preserved by construction (`composeSkillPhasePlan` + `ToolDispatcher.validateAgainstPlan`). C2 + C3 QUALIFIED-DEFER continued — `R-skill-guardrail-non-overridability-tier-0` for M3+ revisit after Sprint 39 dispatcher evidence; `R-skill-state-bus-boundary-enforcement-tier-0` for M3+ revisit after Sprint 41 state-bus evidence. C4 + C5 NOT A CANDIDATE.

**R-item flips at Sprint 38 close:**
- `D-skill-runtime-framework` (action_bank line 345): annotated with Sprint 38 partial-landing note (Skill Registry core + 4 simpler phase Skills shipped; RESOLVE migration + predicate migration + dispatcher + S1/S2 remain Sprint 39; teaching extraction Sprint 40; state-bus Sprint 41).
- `R-grounding-discipline-iterative-search-fabrication`: status preserved (`NEW-Sprint-37-frozen-pending-NEW-Sprint-39-implementation-under-Skill-Registry-abstraction`); Sprint 38 ships no S1 predicate code (Sprint 39 scope per design doc §8.2.4 + M2 §6 #4 verbatim authorization).
- `D-hard-citation-gate`: status preserved (Sprint 39 will implement S1 `must_cite_source` as Skill `guardrails` declaration per the M2 §6 #4 bounded inversion).
- NEW R-item opened: `R-skill-tool-name-canonical-source-centralization` (action_bank §5.2; M3+ revisit; surfaced by Sprint 38 fix iteration OQ-FIX1.1).

**Cross-references:**
- Dev archives: `docs/sprints/sprint-038-handoff.md` (Sprint 38 main-dev archive; commit `bd9d3f5`) + `docs/sprints/sprint-038-fix-handoff.md` (Sprint 38 fix iteration #1 archive; commit `5787806`).
- Sprint 38 contract archive: `docs/sprints/sprint-038-objective.md`.
- Codex review prompts: `compact/sprint-038-review-prompt.md` (initial) + `compact/sprint-038-fix-review-prompt.md` (fix-review).
- Next sub-sprint: Sprint 39 (RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + NEW S1 + NEW S2 + unified Skill terminal-predicate dispatcher) per design doc §6.2.5-§6.2.6 + §7 + §8 + §9.
