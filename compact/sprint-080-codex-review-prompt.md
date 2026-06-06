# Sprint 080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1 — Codex per-sub-sprint review prompt (Anti-Hardcode + tool-surface honesty + R2.a#5-ext narrowness + capability-wiring fence-waiver acceptance)

## Role identity

You are the **Anti-Hardcode + Per-Sub-Sprint Review Agent** for **Sprint
080 / S-Auto-25 / M-Auto-6 Sub-sprint C-1**.

Your review covers the cumulative commit range
**`be1e733..c031786`** (5 commits: path-β IntakeFieldsMerger extraction +
R7 update_intake_fields tool + capability wiring + R2.a#5-ext
RESOLVE-intake clarification-budget re-map + dev handoff).

This sub-sprint is **semantic-touching** at the tool-surface level
(R7 declares a new LLM-facing tool name). Per
`iteration_governance.md` §7 + `process/milestone-framework.md` §4.3,
per-sub-sprint Codex review is **REQUIRED**.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (self-contained per `prompt-artifact-rules.md` §9.1).
3. `docs/sprints/sprint-080-handoff.md` (dev handoff; produced AFTER
   this prompt was drafted — read it to ground the per-change verdict
   against shipped code).
4. Code anchors only as needed (cited inline in §"Cumulative scope
   claim" below).

## Cumulative scope claim (what was shipped)

Sub-sprint C-1 shipped two coupled changes plus their mandatory
capability-wiring tail:

### R7 — `update_intake_fields` no-side-effect tool

- **New tool class** `server/src/main/java/com/gumtree/csagent/service/tools/UpdateIntakeFieldsTool.java`
  (`@Component implements Tool`). Tool name = `update_intake_fields`.
  Arguments schema = `{"fields": {"type": "object",
  "additionalProperties": {"type": "string"}}}` (free-form string→string
  map; structural validation only at dispatch). Returns
  `{"status": "ok", "fields_merged": <int>, "fields_persisted": [...]}`.
- **Path-β refactor** to a new shared helper
  `server/.../service/runtime/IntakeFieldsMerger.java` extracting the
  parse → merge → persist body from
  `AgentRunLoopImpl.persistInlineIntakeFields`. Both the
  `request_handover` persist path and the new tool dispatch path call
  the helper. Byte-equivalence pinned by
  `IntakeFieldsMergerCharacterizationTest` (9 tests).
- **Projection schema declaration** at
  `server/.../service/runtime/ContextProjectionBuilder.java:213-…` via
  `buildUpdateIntakeFieldsArgsSchema` registered in `initToolSchemas`
  (`:149-157`); tool description references
  `required_intake_fields_for_active_uc` (the R1.a #2 projection slot
  at `ContextProjectionBuilder:440`) and does NOT enumerate any per-UC
  field name.
- **Capability wiring** (deliver-agent fence-waiver, see §"Capability
  YAML exception" below):
  - `server/src/main/resources/config/tool-policy.yaml:17–21` —
    `update_intake_fields: {type: AGENT_VISIBLE, allowed-ucs: [UC-G,
    UC-H, UC-I, UC-J, UC-K]}`. Scoped to the five intake UCs.
  - `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml:16`
    — `update_intake_fields` added to `tools_required`. NO procedure /
    wording / objective / objective-text / escalation / grounding edit
    on the skill (verify by diff).
  - `server/.../service/runtime/skill/SkillLoader.java:102` —
    `update_intake_fields` added to `VALID_TOOL_NAMES`. Known-tools
    registry only; no policy behaviour change.

### R2.a#5-ext — phase-aware mapping narrow extension

- `server/.../service/runtime/ControlKernel.java:782` —
  `mapBudgetToEscalationReason(bucket, currentPhase, lastAction)` 3-arg
  overload changed to a 4-arg
  `mapBudgetToEscalationReason(bucket, currentPhase, lastAction,
  activeUseCase)` (overload choice **A** — single source of truth;
  sole production call site at `:313` updated to pass
  `session.getActiveUseCase()`).
- Mapping condition (post-extension; quoted verbatim from
  `ControlKernel.java`):

  ```java
  if ("max-repeated-same-action".equals(bucket)
          && isFreeTextActionKey(lastAction)
          && (
              "DISCOVER".equalsIgnoreCase(currentPhase)
              || ("RESOLVE".equalsIgnoreCase(currentPhase)
                  && IntakeFieldsRegistry.isIntakeUseCase(activeUseCase))
          )) {
      return "clarification_budget_exhausted";
  }
  return mapBudgetToEscalationReason(bucket);
  ```

- No new `escalation_reason` enum value added. The existing
  `clarification_budget_exhausted` enum value is the re-map target.
- `MapBudgetToClarificationLabelTest` extended from 11 to 21 tests
  with RESOLVE-intake positives across UC-G/H/I/J/K and anti-误杀
  negatives (RESOLVE non-intake UC, RESOLVE null UC, RESOLVE intake +
  tool-call repeat, other budget on intake UC, "no new enum"
  canonical-member assertion, INTAKE-phase-literal repeated action).

### Test deltas

- Java baseline post-S-Auto-23: `1297 / 1 / 0 / 2`.
- Post-S-Auto-25: `1327 / 1 / 0 / 2`. Net **+30 tests**, sole pre-existing
  failure preserved (inherited `SystemPromptUserRequestedTiebreakerTest`,
  OQ-S41.5; dev verified pre-existing via `git stash` on clean HEAD
  `ea8004f`).
- New / extended test files (cumulative):
  - `IntakeFieldsMergerCharacterizationTest` (9 byte-equivalence tests).
  - `UpdateIntakeFieldsToolTest` (10 tests incl. validator non-bypass
    via REAL `SkillGuardrailDispatcher`; full-stash → handover-pass;
    empty/wrong-type/null rejected with session unchanged; no
    auto-derivation; blank-value-only → 0 merged).
  - `UpdateIntakeFieldsDispatchSmokeTest` (2 tests: UC-J dispatch via
    REAL `ToolDispatcher` + REAL `ToolPolicyEnforcer` loading the
    production `tool-policy.yaml`; UC-A policy-blocked negative).
  - `MapBudgetToClarificationLabelTest` (extended +10 net to 21 total
    R2.a#5-ext anti-误杀 coverage).
  - Golden-set churn (mechanical tool-list update, NOT assertion
    relaxation): `PhaseEvaluatorPlanTest` (4 intake-UC assertions:
    `List.of("request_handover")` → `List.of("request_handover",
    "update_intake_fields")` while the
    `assertFalse(plan.allowedTools().contains("create_case_controlled"))`
    and `assertFalse(... .contains("search_knowledge"))` anti-误杀
    invariants are PRESERVED unchanged);
    `PhaseEvaluatorResolveSkillIntegrationTest:252` +
    `PhaseEvaluatorSkillIntegrationTest:400` (analogous tool-list
    updates).

### Eval / autoloop / baseline_dir

- Eval pytest UNCHANGED (zero `eval_interactive/` file touched per
  `git diff ea8004f..HEAD --name-only`).
- Autoloop pytest UNCHANGED.
- `autoloop/config.yaml` `baseline_dir` UNCHANGED at
  `m-auto-5-baseline-20260604-simfixed-stalledfix`.
- `docs/current_eval_baseline.md` UNCHANGED.
- NO real-LLM outcome re-bless launched (deferred to M-Auto-6
  milestone-shared run after Sub-sprint C-2 also lands).

## Capability YAML exception (deliver-agent fence waiver — review this explicitly)

The original Sub-sprint C-1 prompt fence forbade
"Any prompt / yaml / CaseSpec under `server/src/main/resources/`".
During the #4 audit the dev surfaced that the new tool requires FOUR
runtime gates, all driven by capability config the prompt fence (and
one Java validator) covers:

1. `ToolDispatcher.validateAgainstPlan` → `plan.allowedTools()` =
   `skill.toolsRequired()` (`PhaseEvaluator:469`).
2. `ContextProjectionBuilder` `tool_schemas` filter →
   `plan.allowedTools()` (`:960-977`).
3. `ToolPolicyEnforcer.isToolAllowed` → `config/tool-policy.yaml`.
4. **`SkillLoader.VALID_TOOL_NAMES`** (`SkillLoader.java:95`) — a
   hardcoded known-tools set that rejects a `tools_required` entry
   naming an unknown tool. This fourth gate was **not anticipated by
   the prompt** and broke skill loading for every test once the tool
   was added to `tools_required`.

The deliver-agent **explicitly authorized Option A** on 2026-06-06: treat
`tool-policy.yaml`, the intake skill's `tools_required` list, and
`SkillLoader.VALID_TOOL_NAMES` as **Runtime capability config per §1.4
"tool schema / capability boundary"**, NOT as semantic surface — zero
procedure / wording / objective / objective-text / enum / CaseSpec
change is permitted on the same surfaces. The four capability edits
(three lines of YAML across two files + 2 lines in SkillLoader.java)
are the minimum viable wiring set.

Codex MUST verify this fence-waiver assertion in Q1 / Q3 / Q4 / Q6 below:
- The yaml diffs are pure registration (allowed-ucs list + new
  `tools_required` entry, with explanatory comments). No procedure or
  wording edit anywhere in the modified yaml files.
- `SkillLoader.VALID_TOOL_NAMES` add is a single new string entry; it
  is NOT a policy enforcement or grounding change.

If Codex disagrees with the Option-A classification, escalate as a
`needs human architecture decision` verdict and explain WHY the
deliver-agent decision is unsafe; otherwise score Q1/Q3/Q4/Q6 against
the capability-only narrow scope.

## Anti-误杀 invariants (HARD, non-negotiable — these were the dev's contract)

1. R7 does NOT bypass the handover validator. The validator at
   `SkillGuardrailDispatcher.checkBeforeDispatch` is the last line of
   defence for whether intake is complete; R7 makes intake accumulation
   VISIBLE to the validator (via shared `session.intakeFields`) but
   cannot make an incomplete intake pass. `AgentRunLoopImpl` gates
   validator invocations behind `HANDOVER_TOOL.equals(toolName)`
   (verify at `:504 / :513 / :897 / :924`) so the new tool name
   `update_intake_fields` is physically unreachable from those gates.
2. R7 does NOT auto-derive from runtime state. The tool MUST be
   LLM-invoked. The tool reads ONLY `parameters.get("fields")` and
   nothing else; the `noAutoDerivation_onlyActsOnFieldsArg` test
   constructs a parameters map carrying `user_message` +
   `accumulated_tool_results` content (no `fields` key) and asserts
   the tool rejects + persists nothing.
3. R7's `fields` argument is a free-form string→string map. NO per-UC
   enumeration in the tool schema. The per-UC required-fields contract
   remains in `IntakeFieldsRegistry` and is projected via
   `required_intake_fields_for_active_uc` (shipped at R1.a #2).
4. R2.a#5-ext preserves anti-误杀 #12 spirit. RESOLVE non-intake UC +
   any phase + any tool-call repeat MUST remain
   `turn_budget_exhausted`. Verify via the explicit negatives in
   `MapBudgetToClarificationLabelTest`.
5. NO new `escalation_reason` enum value. R2.a#5-ext re-maps to the
   existing `clarification_budget_exhausted`.
6. NO `IntakeFieldsRegistry` content change. R7 + R2.a#5-ext both read
   the existing classification.
7. NO `SkillGuardrailDispatcher` change. Validator semantics preserved.
8. NO yaml / prompt / CaseSpec / simulator / scoring touch BEYOND the
   capability-wiring fence waiver above (which is capability registration
   only — no semantic surface touched).
9. NO new Tier-0 invariant. R7 adds a new tool; tools are not Tier-0
   surface per §1.4.
10. `baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
    UNCHANGED. NO outcome-evidence re-bless launched.

## File-path fence (cumulative)

**Allowed (under the deliver-agent fence waiver)**:

- `server/src/main/java/com/gumtree/csagent/service/tools/UpdateIntakeFieldsTool.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsMerger.java` (new helper)
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (path-β refactor only —
  `persistInlineIntakeFields` body extracted to the new helper; null/empty guard preserved; the only delta is the
  removed WARN log on a practically-unreachable `writeValueAsString` failure of a `Map<String,String>` per handoff §1)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (new tool schema +
  `buildUpdateIntakeFieldsArgsSchema` only; pre-existing R1.a / R4.a projection surfaces UNCHANGED)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` (R2.a#5-ext mapping
  signature 3-arg → 4-arg + sole-call-site update at `:313`)
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (single string add to
  `VALID_TOOL_NAMES` — capability registration per the fence waiver)
- `server/src/main/resources/config/tool-policy.yaml` (capability registration entry + `allowed-ucs` list per the
  fence waiver)
- `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` (`tools_required` list addition per
  the fence waiver — NO procedure / wording / objective edit)
- `server/src/test/java/...` (5 new/extended test files)
- `docs/sprints/sprint-080-handoff.md` (dev handoff)

**Forbidden**:

- `IntakeFieldsRegistry.java` content change.
- `SkillGuardrailDispatcher.java` reject-logic change.
- `BudgetChecker.java`.
- `FormContextIngestionService.java`.
- Any other prompt / yaml / CaseSpec under `server/src/main/resources/` beyond the three capability files above.
- Any UI file (Sub-sprint B's surface; B already dev-side closed 2026-06-06).
- Any `eval_interactive/` file (CaseSpecs, simulator, scoring, harness).
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir` pointer.
- `docs/current_eval_baseline.md`.

## §7 stanza (embedded verbatim from dev handoff §7 — record-only)

```markdown
**Target failure layer:** `skill_state` + `infra` + `prompt_projection`
(R7 new tool — `skill_state` for cross-turn intake-field accumulation;
`infra` for dispatch wiring; `prompt_projection` for the new
`tool_schemas` entry making the tool visible to the LLM) + `infra`
(R2.a#5-ext mapping function signature extension; control-plane label
correctness on the c14-style RESOLVE-phase intake clarification budget
hit).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R7 adds
a new TOOL (not a Tier-0 surface per §1.4); the tool does not bypass
the handover validator (which IS Tier-0); R2.a#5-ext extends an
existing mapping function with one additional AND-guarded condition;
no new enum value.

**Semantic hardcode:** No semantic hardcode introduced.
- R7's tool arguments schema is a free-form `string → string` map; NO
  per-UC enumeration in the tool. The per-UC required-fields contract
  remains in `IntakeFieldsRegistry` and is projected via
  `required_intake_fields_for_active_uc` (shipped at R1.a #2). NO
  server-side semantic decision about which fields belong to which UC.
- R7's persist logic reuses the existing `persistInlineIntakeFields`
  merge path (via the shared `IntakeFieldsMerger` helper extracted at
  #3 path β; behaviour-equivalent to the `request_handover` persist
  path).
- R7 does NOT auto-derive calls from runtime state; the tool MUST be
  LLM-invoked.
- R2.a#5-ext extends the existing mapping function with one additional
  AND-guarded condition (`phase=RESOLVE AND
  IntakeFieldsRegistry.isIntakeUseCase(activeUseCase) AND
  isFreeTextActionKey(lastAction)`). `isIntakeUseCase` is the same
  classification R1.a #2 uses — zero new per-UC matrix.
- No new `escalation_reason` enum value introduced.

**Generalization coverage:** target / neighbor / negative / shadow case
counts: 1 / ~8 / ~10 / 0
- target: c14 (UC-J multi-turn intake state loss + RESOLVE-phase
  clarification budget mislabel).
- neighbor: all intake-UC (UC-G / UC-H / UC-I / UC-J / UC-K) cases
  that go through DISCOVER / RESOLVE intake collection; the new tool
  is universal so adoption can vary by UC but availability is uniform.
- negative: R7 anti-误杀 negatives #1-#5 (validator non-bypass; no
  auto-derivation; empty fields rejected; wrong type rejected;
  full-stash → handover passes); R2.a#5-ext anti-误杀 negatives #1-#5
  (RESOLVE non-intake UC unchanged; RESOLVE no active UC unchanged;
  tool-call repeat unchanged; other budget unchanged; no new enum).
- shadow: not applicable (mocked-LLM tests are wiring evidence; real
  evidence is milestone-shared re-bless).
```

## Five specific focal points the deliver-agent has flagged for your review (please address each in §3 of your verdict)

### F1 — R7 as a semantic-touching tool surface vs. semantic hardcode

R7 declares a NEW LLM-facing tool name in `tool_schemas`. By
`iteration_governance.md` §7, that makes it semantic-touching at the
projection surface and triggers this per-sub-sprint review. But the
tool's payload, behaviour, and projection MUST be semantic-hardcode-free
under §1.5/§1.7. Confirm specifically:

- The tool's `fields` argument is a free-form string→string map (NOT
  a per-UC matrix; NOT enumerated property names; NOT a content-classifier
  shape).
- The tool's description references the registry-driven projection slot
  (`required_intake_fields_for_active_uc`) for the per-UC required-fields
  contract, delegating the field-set decision to the LLM (which sees
  the projection slot every turn).
- The tool's execute method does ONLY structural validation (non-null
  + non-empty + map shape) and a merge — NO inspection of the active
  UC, NO derivation from `user_message`, NO classification step, NO
  semantic decision.

### F2 — Capability YAML exception narrowness

The deliver-agent's Option-A waiver permits three capability edits
(tool-policy.yaml + intake skill `tools_required` +
`SkillLoader.VALID_TOOL_NAMES`). Confirm specifically:

- The intake skill yaml diff is **strictly** an addition to the
  `tools_required` list — NO change to `procedure`, `objective`,
  `applicable_use_cases`, `tools_required` ordering, `escalation`,
  `grounding`, or any other field. Diff size = 4 LOC (3 added + 1
  comment). (Use `git show 2d36e98 -- .../resolve_intake_collect_and_handover.yaml`
  to verify.)
- The tool-policy.yaml diff is **strictly** a new tool entry with
  `type: AGENT_VISIBLE` + `allowed-ucs: [UC-G, UC-H, UC-I, UC-J, UC-K]`
  — NO change to other tools, NO change to default policy semantics.
  Diff size = 6 LOC (4 entry + 2 comment).
- The `SkillLoader.VALID_TOOL_NAMES` add is a single new string entry
  + one comment line. Diff size = 2 LOC.

If you observe any procedure / wording / objective / objective-text /
enum / CaseSpec / scoring change inside these files, FAIL the waiver
and reject the sub-sprint.

### F3 — R7 truly does not bypass the handover validator

The dev handoff and tests claim the tool persists to the same
`session.intakeFields` blob that the validator reads, so an incomplete
intake stays incomplete regardless of WHICH tool persisted the fields.
Confirm specifically:

- `AgentRunLoopImpl` gates the validator invocation behind
  `HANDOVER_TOOL.equals(toolName)` (lines `:504`, `:513`, `:897`,
  `:924`). The new tool name `update_intake_fields` does NOT match
  HANDOVER_TOOL = `"request_handover"`, so the validator is physically
  unreachable from the new tool's dispatch path.
- `UpdateIntakeFieldsToolTest.validatorStillRejects_afterPartialUpdate`
  uses the REAL production `SkillGuardrailDispatcher` (the same one
  the run loop wires up). Partial stash → validator returns
  `INTAKE_INCOMPLETE_REJECT_REASON` as expected.
- `UpdateIntakeFieldsToolTest.validatorPasses_afterFullStashViaTool`
  uses the same REAL dispatcher. Full stash → validator returns
  `Optional.empty()` (passes), proving the persist semantics are
  equivalent to the `request_handover` path but the validator path is
  identical (no bypass; just the same intake-completeness contract
  satisfied through a different tool invocation route).

### F4 — R2.a#5-ext narrowness

The mapping extension adds ONE additional AND-guarded condition
(`RESOLVE phase + IntakeFieldsRegistry.isIntakeUseCase(activeUseCase) +
isFreeTextActionKey(lastAction)`). Confirm specifically:

- The condition is AND-guarded with both the existing free-text-action
  guard (excluding tool-call repeats) AND the existing intake-UC
  classification (excluding RESOLVE non-intake UCs like UC-A / UC-B /
  UC-D / UC-F / UC-FP / UC-C / UC-E).
- The `isIntakeUseCase` source is the existing
  `IntakeFieldsRegistry.isIntakeUseCase(activeUseCase)` method — NOT a
  new per-UC hard-coded list, NOT a new classification.
- No new `escalation_reason` enum value introduced. The mapping target
  is the existing `clarification_budget_exhausted` value used in R2.a
  #5.
- The 4-arg overload (option A) is the SINGLE source of truth. The
  3-arg overload is removed (NOT left as a vestigial code path that
  could drift).
- All production call sites pass `session.getActiveUseCase()` (verify
  there is exactly one — sole prod site at `ControlKernel:313` per
  the dev's audit). NO call site passes a hard-coded UC literal.
- Anti-误杀 negative tests cover RESOLVE non-intake UC, null active
  UC, tool-call repeat, other budget bucket, and the "no new enum"
  canonical-member assertion (all in
  `MapBudgetToClarificationLabelTest`).

### F5 — `SkillLoader.VALID_TOOL_NAMES` add acceptability

The fourth gate the dev surfaced (a hardcoded known-tools set in
`SkillLoader.java:95`) is being treated as capability registration
under the deliver-agent's Option-A fence waiver. Confirm specifically:

- The `VALID_TOOL_NAMES` set is a sanity gate that rejects an UNKNOWN
  tool name appearing in a skill yaml's `tools_required` list — it is
  NOT a policy enforcement layer (that's `ToolPolicyEnforcer`), NOT a
  semantic decision layer.
- The add is a single new string entry (`"update_intake_fields"`) with
  one comment line. The semantics of the gate are unchanged: known
  tools registered → loadable; unknown tools → rejected at load.
- Putting it elsewhere is not viable: leaving it OUT breaks skill
  loading for every test the moment the intake skill's
  `tools_required` includes the new tool name. The dev's choice to
  list it here is the minimum viable wiring set.

If you agree the gate is capability-registration shape (not semantic
surface), the add is acceptable under Option A. If you disagree,
explain WHY and propose an alternative (e.g. extract VALID_TOOL_NAMES
to a property file / register tools via Spring DI so the static set is
not needed — this would be a refactor candidate for a later sub-sprint,
not a blocker for C-1).

## §4.1 Nine-question Anti-Hardcode kernel (embedded verbatim — apply each per the focal-point context above)

### Q1 — Semantic decision hardcode?

Does the change encode a semantic decision the LLM owns (UC hypothesis,
drift / topic-shift detection, next-action, escalation posture,
response strategy, natural customer-facing wording) into Runtime code
(Java guard, prompt if-else, keyword/regex match, per-UC matrix)?

Apply to:
- The tool's execute logic and structural validation.
- The tool's projection schema and description.
- The mapping extension's AND-guard condition.
- The `tool-policy.yaml` capability registration (does it route a
  semantic decision? or just gate visibility?).
- The skill yaml `tools_required` addition (does it modify procedure?
  or just expand the allowed tool set?).

Verdict shape: `pass` / `partial` / `fail` per change; aggregate.

### Q2 — Tier-0 invariant justification?

Does the change add a Tier-0 invariant per `runtime_freeze_and_risk_policy.md`
§1 / §2? If yes, is it independently justified (existing Tier-0 cited;
new invariant proposed via a separate authorization)? If no, confirm
the change is below the Tier-0 line.

Apply to:
- The new tool class (tools are not Tier-0 surface per §1.4 — confirm).
- The mapping extension (control-plane labeling; not a Tier-0
  invariant).
- The capability-wiring edits (capability config; not a Tier-0
  invariant).

### Q3 — Could soft-signal projection suffice instead?

For each Java guard or prompt rule, could a structured projection slot
(observable state the LLM may use) have achieved the same effect
without putting the decision in Runtime?

Apply to:
- The tool itself — is this a soft signal (LLM-invoked, never derived)
  or a hard guard (Runtime decides)? The dev claims LLM-invoked /
  soft-signal shape.
- The mapping extension — is this control-plane labeling (Runtime
  stamps the accurate reason on a budget the LLM did not control) or
  a Runtime decision that the LLM should have made?

### Q4 — Eval/case text encoded?

Does the change encode raw eval phrases, CaseSpec strings, or specific
test fixture content into Runtime code?

Apply to:
- The tool description (does it reference c14 by name? a UC-J
  scenario? no — it references the registry-driven projection slot).
- The mapping condition (does it match on user-message content or
  CaseSpec phrasing? no — structural AND-guard only).
- The capability yaml (does it carry test-specific UC names? the
  allowed-ucs list is the registry-intake-UC set, not a test-specific
  subset).
- The dispatch smoke test (uses UC-J + UC-A as plausible canonical
  representatives of the intake / non-intake split — not test-fixture
  leakage).

### Q5 — Semantic ownership moved LLM → Java?

Does this change take a semantic decision currently owned by the LLM
and move it into Java?

Apply to:
- R7 — does the tool decide WHEN to persist? No; LLM decides when to
  call it. Does the tool decide WHICH fields belong to which UC? No;
  the LLM passes the `fields` map.
- R2.a#5-ext — does the mapping decide how a budget should be
  resolved? It decides the LABEL on an already-fired budget (an
  observability concern); does not influence whether the budget
  fires or how the LLM should respond. The LLM's escalation decision
  is unchanged.

### Q6 — Prompt if-else instead of principles?

Does any prompt edit add an if-else / rule-dump instead of expressing
a principle?

Apply to:
- The intake skill yaml — is the new `tools_required` entry a
  principle-shaped capability declaration or an if-else rule? It is
  a capability list extension, no if-else added; the procedure text
  is byte-untouched per the dev claim.
- The tool description — same shape question; verify no rule-dump.

### Q7 — Tool schema / safety / grounding floors preserved?

Are tool-schema, PII, safety, grounding contracts preserved?

Apply to:
- Tool schema additions (the new schema is well-formed: type/object,
  additionalProperties string, required: fields).
- Safety floor (the handover validator path is NOT modified; R7 does
  NOT bypass it; confirm via the
  `validatorStillRejects_afterPartialUpdate` test using REAL
  `SkillGuardrailDispatcher`).
- Grounding floor (R7 / R2.a#5-ext do not touch retrieval, FAQ,
  citation, or any grounding-floor surface).

### Q8 — Generalization eval coverage?

Are target / neighbor / negative / shadow case families sized and
documented per §5.1 / §7.1?

Apply to the §7 stanza embedded above: counts 1 / ~8 / ~10 / 0.
Confirm the negatives are PROPER negatives (constructed to NOT trigger
the new behaviour and exercising the anti-误杀 path).

### Q9 — Temporary hardcode / sunset plan?

Is the change temporary (with a sunset trigger) or durable?

R7 + R2.a#5-ext are both durable wiring fixes. No interim semantic
branch. The capability-wiring waiver is durable (no rollback planned —
the tool is intended to ship into M-Auto-7+ autoloop work via OBS-S6).

### Aggregate verdict

One of: `approve` / `approve with downgrade-to-signal follow-up` /
`reject as semantic hardcode` / `needs human architecture decision`.

## Output format (required §4.2 header at the TOP of `docs/codex-findings.md`)

After completing the review, write the result to
`docs/codex-findings.md` REPLACING the existing per-sub-sprint review
header (S-Auto-23's `APPROVE_S_AUTO_23` content) with the new
S-Auto-25 header. The header MUST start with:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
final_verdict: APPROVE_S_AUTO_25 | APPROVE_S_AUTO_25_WITH_FIXES | REJECT_S_AUTO_25 | NEEDS_HUMAN_ARCHITECTURE_DECISION
summary: <one paragraph>
```

Then write §1–§5 covering:

- §1 — Per-change verdicts (one per scope item #1–#7 + the
  capability-wiring waiver + the golden-set test churn).
- §2 — Nine-question kernel walkthrough (Q1–Q9 each with cited
  evidence).
- §3 — Five focal-point verdicts (F1–F5; explicit pass / fail / partial
  per focal point).
- §4 — Blocking findings (if any). Each finding cites file:line + the
  specific anti-误杀 invariant or §4.1 question it violates.
- §5 — Non-blocking observations (recommendations, follow-up suggestions,
  downgrade-to-signal candidates).

## Constraints

- Codex MUST NOT edit code. This review writes findings only.
- Codex MUST NOT re-judge any case in
  `eval_interactive/case_specs/bad_cases/` (the §5.6 primary acceptance
  gate is human-judgment-only).
- Codex MAY use `git show <commit>` / `git diff` / `grep` /
  `mvn -o test -Dtest=...` to verify claims.
- Codex MUST cite specific file:line ranges for each verdict and each
  blocking finding.
- Per `process/milestone-framework.md` §4.3 + the deliver-agent's
  `compact/sprint-080-codex-review-prompt.md` artifact, the review
  scope is bounded to the cumulative commit range `be1e733..c031786`.

## Cumulative commit range

`be1e733..c031786` — five commits:

1. `be1e733` — R7 #3 path-β `IntakeFieldsMerger` extraction +
   characterization test (9 tests).
2. `2d36e98` — R7 #1+#2+#4 — `UpdateIntakeFieldsTool` + projection
   schema + capability wiring (tool-policy.yaml + intake skill
   tools_required + `SkillLoader.VALID_TOOL_NAMES`) + tool/dispatch
   smoke tests + golden-set tool-list update.
3. `d0d42d7` — R2.a#5-ext (overload A) `ControlKernel` mapping
   signature 3-arg → 4-arg + sole call-site update +
   `MapBudgetToClarificationLabelTest` extension.
4. `c031786` — dev handoff
   (`docs/sprints/sprint-080-handoff.md`).

(`be1e733` is the dev's `R7 #3 path-β IntakeFieldsMerger extraction`
commit; the deliver-agent's commit `ea8004f` is the baseline for the
range start exclusive.)

## Final note

This is a per-sub-sprint review; the milestone-shared review at
M-Auto-6 close will revisit the cumulative A + B + C-1 + C-2 commit
range. Approve / reject this sub-sprint on its own merits; do not roll
in Sub-sprint B (UI-only, §7-EXEMPT) or Sub-sprint C-2 (R5 + R6,
PLANNING CONTEXT pending C-1 close) into this verdict.
