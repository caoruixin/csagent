## Sprint Review Decision
decision: pass
blocking_count: 0
final_verdict: APPROVE_S_AUTO_25
summary: Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 is approved. R7 adds an LLM-invoked, free-form intake-field persistence tool without moving field selection, use-case classification, or handover-completeness decisions into Runtime; R2.a#5-ext narrowly corrects an already-fired budget's control-plane label using the existing intake registry and existing escalation reason. The Option-A capability waiver is acceptable and confined to capability registration. No forbidden semantic, eval, scoring, baseline, validator, registry, or Tier-0 surface changed. Targeted tests passed 100/0/0/0, and the full Java suite reproduced the handoff at 1327/1/0/2 with the sole inherited failure.

## §1 Per-Change Verdicts

**#1 — `UpdateIntakeFieldsTool` class: approve.** The tool name is declared at `UpdateIntakeFieldsTool.java:42-45`; execution reads only `parameters.fields`, rejects null/non-map/empty payloads, delegates the merge, and returns structural confirmation at `:48-73`. It never reads active UC, user message, accumulated results, or validator state. No LLM-owned semantic decision is encoded.

**#2 — standard dispatch path: approve.** The tool is a normal Spring `@Component implements Tool` at `UpdateIntakeFieldsTool.java:32-34`; generic registration and policy enforcement remain in `ToolDispatcher.java:65-75` and `:86-103`. The real-dispatch smoke verifies intake-UC allow and UC-A deny at `UpdateIntakeFieldsDispatchSmokeTest.java:48-57` and `:69-102`.

**#3 — path-β `IntakeFieldsMerger` extraction: approve.** The helper sequences registry parse, merge, canonical-name reporting, and conditional persist only at `IntakeFieldsMerger.java:55-76`; the request-handover path preserves its `intake_fields` extraction/null-empty guard and delegates at `AgentRunLoopImpl.java:999-1014`. Characterization coverage pins union, overwrite, alias, blank-drop, no-op, and round-trip behavior at `IntakeFieldsMergerCharacterizationTest.java:46-147`.

**#4 — projection schema and capability registration: approve.** `ContextProjectionBuilder.java:145-157` registers the tool and points its description to `required_intake_fields_for_active_uc`; `:213-227` declares required `fields` as a free-form object with string-valued `additionalProperties`, with no per-UC properties. The referenced registry-driven slot is emitted at `:465-478`, and schemas are filtered by `plan.allowedTools()` at `:998-1015`.

**#5 — R2.a#5-ext mapping: approve.** The sole production call passes `session.getActiveUseCase()` at `ControlKernel.java:313-315`. The 4-arg mapping at `:782-793` requires `max-repeated-same-action` AND a free-text action AND either DISCOVER or RESOLVE plus `IntakeFieldsRegistry.isIntakeUseCase`; it falls back to the unchanged base mapper. Repository search found no vestigial 3-arg overload or second production call site.

**#6 — anti-误杀 tests: approve.** Real-dispatcher validator rejection/pass are covered at `UpdateIntakeFieldsToolTest.java:133-173`; malformed/no-auto-derivation negatives at `:175-227`; R2.a#5-ext positives and negatives at `MapBudgetToClarificationLabelTest.java:58-207`. Targeted review run: 100 tests, 0 failures/errors/skips.

**#7 — rebuild/integration smoke evidence: approve.** The dispatch smoke uses a real `ToolPolicyEnforcer` loading production policy at `UpdateIntakeFieldsDispatchSmokeTest.java:48-57`, with positive/negative dispatch at `:69-102`. Full `mvn -o test` reproduced `1327 / 1 / 0 / 2`; the sole failure is the inherited `SystemPromptUserRequestedTiebreakerTest`.

**Capability-wiring waiver: approve.** The skill YAML changes only `tools_required` at `resolve_intake_collect_and_handover.yaml:11-16`; procedure/objective/grounding/escalation remain untouched at `:25-28`. Tool policy adds only the new AGENT_VISIBLE intake-UC entry at `tool-policy.yaml:14-19`. `SkillLoader.VALID_TOOL_NAMES` adds one known-tool entry at `SkillLoader.java:95-109`; its unchanged validation semantics reject unknown `tools_required` names at `:256-263`.

**Golden-set test churn: approve.** The changes update expected intake-plan tool lists while preserving explicit `create_case_controlled` and `search_knowledge` exclusions at `PhaseEvaluatorPlanTest.java:132-138`, `:148-154`, `:165-171`, and `:182-188`; analogous integration expectations are at `PhaseEvaluatorResolveSkillIntegrationTest.java:249-255` and `PhaseEvaluatorSkillIntegrationTest.java:397-402`. No assertion relaxation was found.

## §2 Nine-Question Kernel Walkthrough

**Q1 — Semantic decision hardcode? Pass.** R7 persists exactly the LLM-supplied map (`UpdateIntakeFieldsTool.java:48-67`) and projects a universal free-form schema (`ContextProjectionBuilder.java:213-227`); it does not decide which fields belong to a UC. R2.a#5-ext labels an already-fired Runtime-owned budget using structural phase/action/registry facts (`ControlKernel.java:782-793`). Capability YAML changes visibility/allowed tools only (`tool-policy.yaml:14-19`; `resolve_intake_collect_and_handover.yaml:11-16`).

**Q2 — Tier-0 invariant justification? Pass / not applicable.** No new Tier-0 invariant is added. R7 is a tool-schema/capability/persistence surface; R2.a#5-ext is control-plane budget labeling; the existing handover validator remains unchanged and reachable only for `request_handover` (`AgentRunLoopImpl.java:504-518`).

**Q3 — Could soft-signal projection suffice? Pass.** R7 is already LLM-invoked: Runtime never derives a call, and the LLM sees registry-driven required fields at `ContextProjectionBuilder.java:465-478`. R2.a#5-ext does not choose the bot's next action or escalation posture; it stamps the accurate reason after `BudgetChecker` has fired (`ControlKernel.java:313-318`).

**Q4 — Eval/case text encoded? Pass.** No executable condition or LLM-facing description matches user-message text, raw CaseSpec phrases, or a case-specific UC subset. The schema description is generic and registry-referential (`ContextProjectionBuilder.java:151-156`); the mapping uses structural facts only (`ControlKernel.java:784-793`). Runtime comments mention c14/UC-J as rationale (`UpdateIntakeFieldsTool.java:20-21`; `ControlKernel.java:309-310`) but do not affect behavior or projected wording.

**Q5 — Semantic ownership moved LLM → Java? Pass.** The LLM decides when to invoke R7 and supplies all names/values; Runtime only persists them (`UpdateIntakeFieldsTool.java:48-67`). The mapping extension changes neither budget firing nor LLM response selection (`ControlKernel.java:313-318`, `:782-793`).

**Q6 — Prompt if-else instead of principles? Pass.** No prompt/procedure/objective edit occurred. The skill change is one capability-list entry (`resolve_intake_collect_and_handover.yaml:11-16`), and the tool description is concise observable-state guidance (`ContextProjectionBuilder.java:151-156`).

**Q7 — Tool schema / safety / grounding floors preserved? Pass.** The schema is a required free-form string map (`ContextProjectionBuilder.java:213-227`). Run-loop guardrail invocation remains gated by `HANDOVER_TOOL` at `AgentRunLoopImpl.java:504-518`; the production validator still checks registry completeness at `SkillGuardrailDispatcher.java:265-297`. R7/R2.a#5-ext touch no retrieval, citation, grounding, PII, or safety-floor code.

**Q8 — Generalization eval coverage? Pass for wiring evidence.** Target/neighbor/negative coverage is substantive: all intake UCs in the mapping positive (`MapBudgetToClarificationLabelTest.java:83-91`), non-intake/null/tool-call/other-budget negatives (`:93-119`, `:175-207`), validator non-bypass and no-auto-derivation negatives (`UpdateIntakeFieldsToolTest.java:133-173`, `:202-220`), and policy-blocked UC-A (`UpdateIntakeFieldsDispatchSmokeTest.java:89-102`). Shadow/outcome evidence is correctly deferred to the milestone-shared real-LLM re-bless.

**Q9 — Temporary hardcode / sunset plan? Pass / not applicable.** R7, the mapping extension, and capability registration are durable Runtime wiring (`UpdateIntakeFieldsTool.java:42-73`; `ControlKernel.java:782-793`; `tool-policy.yaml:14-19`). There is no temporary semantic branch requiring a sunset.

**§4.1 aggregate verdict:** `approve`

## §3 Five Focal-Point Verdicts

**F1 — R7 semantic-touching surface vs semantic hardcode: pass.** The new LLM-facing name makes review mandatory, but its schema is free-form rather than a per-UC matrix (`ContextProjectionBuilder.java:213-227`), its description delegates required-field knowledge to the registry-driven projection slot (`:151-156`, `:465-478`), and execution performs only shape validation plus merge (`UpdateIntakeFieldsTool.java:48-67`).

**F2 — Capability YAML exception narrowness: pass.** The skill diff is exactly four added lines under `tools_required` (`resolve_intake_collect_and_handover.yaml:11-16`); tool policy is exactly six added lines for the new entry (`tool-policy.yaml:14-19`); `SkillLoader` is one comment plus one string (`SkillLoader.java:101-102`). No procedure, objective, grounding, escalation, enum, CaseSpec, or scoring edit occurred. The prompt's comment/config split description for tool policy is numerically imprecise, but the six-line total and capability-only scope are correct.

**F3 — R7 does not bypass the handover validator: pass.** The run loop persists inline fields and invokes the dispatcher only when `HANDOVER_TOOL.equals(toolName)` (`AgentRunLoopImpl.java:504-518`), while successful-handover terminal handling is likewise handover-name-gated (`:897-908`). The real production dispatcher fixture is constructed at `SkillTestFixtures.java:30-44`; partial stash rejects and full stash passes at `UpdateIntakeFieldsToolTest.java:133-173`. The prompt's reference to `AgentRunLoopImpl:924` is an unrelated `classify_use_case` gate, not a validator invocation; this does not affect the invariant.

**F4 — R2.a#5-ext narrowness: pass.** The extension reuses `IntakeFieldsRegistry.isIntakeUseCase`, retains the free-text guard, adds no enum, removes the old 3-arg overload, and has one production call passing session state (`ControlKernel.java:313-315`, `:736-750`, `:782-803`). Required anti-误杀 negatives and canonical-member assertion are present at `MapBudgetToClarificationLabelTest.java:93-119`, `:175-207`.

**F5 — `SkillLoader.VALID_TOOL_NAMES` acceptability: pass.** The set is explicitly a canonical-name sanity registry (`SkillLoader.java:82-109`), and unchanged validation rejects unknown YAML tool names at `:256-263`. It does not enforce per-UC policy or semantic routing. Adding the one string is the minimum viable wiring under Option A; DI-backed registration remains a reasonable later refactor, not a C-1 blocker.

## §4 Blocking Findings

None.

## §5 Non-Blocking Observations

1. **Review range notation is inconsistent.** Literal `be1e733..c031786` contains three commits and excludes `be1e733`; the stated intended cumulative scope contains four commits and is expressed by `be1e733^..c031786` (or baseline `ea8004f..c031786`). This review audited the intended four-commit scope so the merger extraction was not omitted.

2. **Pin the new projected schema directly.** No new test asserts that the projected `update_intake_fields` schema contains required `fields` plus `additionalProperties.type=string`. Current tests prove plan wiring/dispatch, but a focused projection-schema test would protect the core F1 contract at `ContextProjectionBuilder.java:149-157` and `:213-227`.

3. **Runtime value coercion is broader than the projected schema.** The schema advertises string values, but `IntakeFieldsRegistry.mergeFields` converts arbitrary non-null values with `toString()` at `IntakeFieldsRegistry.java:200-213`; R7 intentionally reuses that pre-existing request-handover contract. This is not semantic hardcode or validator bypass, but a later contract-hardening sprint should decide whether dispatch must reject non-string field values.

4. **Capability registries can drift.** Intake applicability is represented in `IntakeFieldsRegistry`, the skill YAML, and `tool-policy.yaml`, while known tool names are mirrored in `SkillLoader`. The current edits match and are acceptable; a later infra refactor or consistency test could reduce manual-registration drift.

5. **Outcome evidence remains deferred.** The mocked/unit/integration evidence is sufficient for C-1 wiring review, but it does not prove real-LLM adoption of `update_intake_fields`; the milestone-shared re-bless remains required after C-2.
