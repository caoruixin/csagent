---
title: Sub-sprint C-1 — S-Auto-25 / Sprint 080 — R7 update_intake_fields tool + R2.a#5-ext phase-aware re-map RESOLVE-intake extension
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md §4.7 (R2.a#5-ext) + §4.8 (R7) + §6.7 (Sub-sprint C-1 packaging) + compact/sprint-080-dev-prompt.md (self-contained executable view)
last_reviewed: 2026-06-06
review_cadence: per sub-sprint
supersedes: docs/sprints/sprint-079-objective.md
superseded_by: null
notes: >
  Sub-sprint C-1 of M-Auto-6 (Runtime substrate hygiene + admin
  observability + intake/clarification contract + UX/corpus governance).
  Activated 2026-06-06 after Sub-sprint B dev-side close (visual-verified
  + §7-EXEMPT — no per-sub-sprint Codex required). Sub-sprint A
  (R1.a + R2.a + R4.a) is dev-side closed and smoke-verified per
  `docs/sprints/sprint-078-objective.md`. Sub-sprint B (R3.a + R3.b +
  R3.c) is dev-side closed and visual-verified per
  `docs/sprints/sprint-079-objective.md`. The outcome-evidence re-bless
  waits for C-1 + C-2 to land — single milestone-shared re-bless at
  M-Auto-6 close.

  Scope (intake/clarification runtime contract; semantic-touching at the
  tool-schema surface via R7):
  - R7 (proposal §4.8) — new `update_intake_fields(fields={...})` tool.
    Free-form `string → string` map argument; reuses existing
    `persistInlineIntakeFields` merge path via a shared helper (R7 #3
    audit decides α direct-call vs β refactor); does NOT trigger the
    handover validator. LLM gains a runtime mechanism to accumulate
    intake state across turns without the "send-or-stall" dilemma.
    Runtime enabler for OBS-S6 (autoloop teaching LLM to infer intake
    fields from free text — deferred until R7 ships).
  - R2.a#5-ext (proposal §4.7) — narrow extension of
    `ControlKernel.mapBudgetToEscalationReason` (3-arg overload shipped
    at S-Auto-23 commit `247da11`) to also fire on `phase=RESOLVE AND
    IntakeFieldsRegistry.isIntakeUseCase(activeUseCase) AND
    isFreeTextActionKey(lastAction)`. Anti-误杀 #12 spirit preserved
    (RESOLVE non-intake UC + any-phase tool-call repeat stay
    `turn_budget_exhausted`). Reuses existing
    `clarification_budget_exhausted` enum value (no new enum).

  Per-sub-sprint Codex review REQUIRED (R7 adds a new LLM-facing tool
  name; semantic-touching per `iteration_governance.md` §7).

  Sub-sprint C-2 (S-Auto-26 / Sprint 081; R5 + R6 — citation display
  token + corpus `bot_visible` filter) is drafted as planning context
  in `compact/sprint-081-dev-prompt.md`; launches after C-1 dev-side
  closes (sequential per human 2026-06-06 decision).

  Forbidden: any semantic procedure / wording change; any new
  `escalation_reason` enum value; any `IntakeFieldsRegistry` content
  change (registry contract iterated as the source of truth); any
  `SkillGuardrailDispatcher` reject-logic change (R7 does NOT bypass
  the validator; the validator remains the last line of defence for
  intake completion); any `BudgetChecker` budget-definition change;
  any auto-derivation of `update_intake_fields` calls from runtime
  state (the tool MUST be LLM-invoked); any UI / yaml / CaseSpec /
  simulator / scoring / autoloop 5-file SHA-locked set touch;
  `baseline_dir` or `docs/current_eval_baseline.md` move (both flip
  at M-Auto-6 milestone close after the milestone-shared re-bless).

  `baseline_dir` UNCHANGED (`m-auto-5-baseline-20260604-simfixed-stalledfix`);
  `docs/current_eval_baseline.md` UNCHANGED. NO outcome-evidence
  re-bless at this sub-sprint close — that runs at the M-Auto-6
  milestone close.
---

# Sub-sprint C-1 — S-Auto-25 / Sprint 080 — R7 + R2.a#5-ext

## Class

**Layer (per `iteration_governance.md` §3.2):**

- **R7** → `skill_state` + `infra` + `prompt_projection`. `prompt_projection`
  because R7 adds a new tool name in the LLM-facing tool list (the
  `tool_schemas` projection); `skill_state` because the tool's effect is
  cross-turn `session.intakeFields` accumulation; `infra` because dispatch
  reuses the existing merge path with no semantic decision added.
- **R2.a#5-ext** → `infra`. Control-plane labeling extension to the
  already-shipped phase-aware re-map function; no new semantic surface;
  no new enum value.

**§7 stanza requirement:** **REQUIRED** (R7 adds a new LLM-facing tool
name; semantic-touching per `iteration_governance.md` §7). Full stanza in
§7 below.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.

- R7's no-side-effect tool only WRITES `session.intakeFields` via the
  existing `persistInlineIntakeFields` merge path. It does NOT bypass
  the handover validator at `SkillGuardrailDispatcher.java:265-298`
  (which IS Tier-0 contract — the validator remains the last line of
  defence for intake completion regardless of which tool persisted the
  fields).
- R2.a#5-ext extends an existing mapping function with one additional
  AND-guarded condition; no new mapping; no new enum value; no removal
  of existing guards.

**Semantic hardcode:** No semantic hardcode introduced.

- R7's `fields` argument is a free-form `string → string` map (mirroring
  the `intake_fields` slot shipped at R1.a). NO per-UC enumeration of
  property names in the tool schema; NO server-side derivation of which
  fields belong to which UC (the per-UC required-fields contract is
  already projected via `required_intake_fields_for_active_uc` from
  R1.a #2). The new tool just persists what the LLM passes.
- R2.a#5-ext's extension is
  `RESOLVE phase + isIntakeUseCase(activeUseCase) + isFreeTextActionKey(lastAction)`.
  `IntakeFieldsRegistry.isIntakeUseCase` is the SAME classification the
  existing R1.a #2 projection uses — zero new per-UC matrix.
- R7 does NOT auto-derive calls from runtime state; the tool MUST be
  LLM-invoked.

## Goal

After this sub-sprint ships:

- **R7 wiring**: a new `update_intake_fields(fields={...})` tool is
  declared in the projection's `tool_schemas` (same level as
  `request_handover`, `search_knowledge`, etc.); the server dispatches
  it via the standard `ToolDispatcher` path; the dispatch handler calls
  the existing `persistInlineIntakeFields(session, call)` merge logic
  (same as the `request_handover` intake-fields persist path) WITHOUT
  routing through the handover validator. The tool returns
  `{"status": "ok", "fields_merged": <count>, "fields_persisted": [...]}`
  and emits a normal `ToolEvent` to the trace.
- **R2.a#5-ext mapping**: the existing 3-arg
  `mapBudgetToEscalationReason(bucket, phase, lastAction)` overload at
  `ControlKernel.java:763-771` is extended (signature recommendation:
  add `activeUseCase` parameter; alternative: 4-arg overload alongside
  the 3-arg) to also fire on
  `phase=RESOLVE AND IntakeFieldsRegistry.isIntakeUseCase(activeUseCase) AND isFreeTextActionKey(lastAction)`.
  Anti-误杀 #12 spirit preserved: RESOLVE non-intake UC + any phase +
  any tool-call repeat remain `turn_budget_exhausted`.

NOT a goal:

- Modifying `IntakeFieldsRegistry` content or contract.
- Modifying `SkillGuardrailDispatcher` reject logic (handover validator
  unchanged — c14's intake completion is still gated by the validator).
- Modifying `BudgetChecker` budget definitions.
- Adding new `escalation_reason` enum values.
- Routing the new tool through the handover validator (the whole point
  of R7 is that it does NOT trigger the validator — that path remains
  `request_handover`-only).
- Changing skill yaml procedures (OBS-S6 — autoloop teaching LLM to
  call the new tool — is autoloop work after M-Auto-6 close).
- Any LLM-side semantic decision (the LLM decides WHEN to call
  `update_intake_fields` and what fields to pass; the runtime does NOT
  derive the call from message content).

## Scope (executable, #1–#7)

The dev prompt at `compact/sprint-080-dev-prompt.md` is the
self-contained executable view of this contract; sync invariant per
`prompt-artifact-rules.md` §9.3. The scope steps below are the canonical
version; the prompt mirrors them with one-page cumulative context +
read-order wrappers added.

### #1 — R7 step 1: `UpdateIntakeFieldsTool.java` new tool class

Standard `Tool` interface implementation. Tool name `update_intake_fields`;
arguments schema `{"fields": {"type": "object", "additionalProperties":
{"type": "string"}}}`; required arguments `fields` (non-empty structural
validation at dispatch); description references
`required_intake_fields_for_active_uc` projection field (R1.a #2). Tool
result body: `{"status": "ok", "fields_merged": <int>,
"fields_persisted": [...]}`. Trace event: standard `ToolEvent`.

### #2 — R7 step 2: dispatch handler reuses `persistInlineIntakeFields`

Dispatch handler validates `arguments.fields` is a non-empty
`Map<String, String>` (structural; rejects with standard
tool-validation error shape if empty or wrong type), then invokes the
existing `persistInlineIntakeFields(session, call)` merge logic — OR
the shared helper extracted by the #3 audit. Behaviour-equivalent to
the `request_handover` persist path on a fixed input.

### #3 — R7 step 3: PRE-FIX `persistInlineIntakeFields` reuse audit

Mandatory audit before #2 wiring: read
`AgentRunLoopImpl.java:487-494` + the method body. Confirm
(a) whether the method is private and tightly coupled to
`request_handover`, OR already structured as a reusable helper;
(b) whether it depends on any `call` field other than
`call.arguments.intake_fields`; (c) whether it gates on
`IntakeFieldsRegistry.isIntakeUseCase(activeUseCase)` before persisting.

Two outcomes:

- **(α) Already reusable** — invoke directly. Document in handoff §1.
- **(β) Tightly coupled** — refactor to a shared helper (e.g.
  `IntakeFieldsMerger.merge(session, fieldsMap)`); the
  `request_handover` path now calls the helper too. The refactor MUST
  be byte-equivalent on the `request_handover` path (characterization
  test required).

**Hard fence:** the audit MUST NOT change the merge semantics. If it
surfaces that the current path has a subtle behaviour the new tool
would break, STOP and surface to deliver-agent.

### #4 — R7 step 4: tool registration + projection schema declaration

Register `UpdateIntakeFieldsTool` in the tool registry / `ToolDispatcher`
constructor; add the tool's arguments-schema projection to
`ContextProjectionBuilder.java` `tool_schemas` emission (where
`request_handover` schema is built — R1.a #1 shipped at `:262-281`).

**Hard fence:** the projection MUST include the tool's description
referencing `required_intake_fields_for_active_uc` so the LLM knows
which fields to populate. The tool description MUST NOT enumerate
per-UC field names (those come from the registry-driven projection).

### #5 — R2.a#5-ext: extend phase-aware mapping to RESOLVE-intake free-text

Anchor: `ControlKernel.java:763-771` (3-arg overload shipped at
S-Auto-23 commit `247da11`).

Design choice **(A) recommended**: extend the 3-arg signature to take
`activeUseCase`; update all call sites (the only production call site
per Codex §1 R2.a #5 verdict is at `:305-313`). Alternative **(B)**:
4-arg overload alongside 3-arg (delegate 3-arg → 4-arg with `null`
activeUseCase).

Mapping condition (post-extension):

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

**Anti-误杀 #12 spirit preserved:**
- RESOLVE + non-intake UC + any action → `turn_budget_exhausted`.
- Any phase + any tool-call repeat → `turn_budget_exhausted`.
- DISCOVER + free-text → `clarification_budget_exhausted` (existing).
- RESOLVE + intake UC + free-text → `clarification_budget_exhausted`
  (NEW).

### #6 — Anti-误杀 test suite (R7 + R2.a#5-ext)

New test classes:

- `server/src/test/java/.../UpdateIntakeFieldsToolTest.java`
- Extend `MapBudgetToClarificationLabelTest.java` (existing) with
  RESOLVE intake positive + RESOLVE non-intake negative + tool-call
  negative + other-budget negative + no-new-enum assertion.
- If #3 chose path (β): characterization test comparing refactored
  helper output to pre-refactor inline merge on a fixed input.

**R7 tests** (positive + negative):
- R7 #1 positive: `update_intake_fields(fields={"report_type": "scam"})`
  on UC-J session → `session.intakeFields` contains
  `{"report_type": "scam"}` AND no handover validator fires.
- R7 #2 positive (multi-call accumulation): two consecutive calls with
  different fields → union persisted.
- R7 anti-误杀 #1 (validator non-bypass): after `update_intake_fields`
  partial-population, `request_handover` with no `intake_fields` arg →
  validator STILL rejects with
  `intake_required_fields_missing_for_intake_complete`.
- R7 anti-误杀 #2 (full-stash → handover passes): after
  `update_intake_fields` populates all required fields,
  `request_handover` succeeds.
- R7 anti-误杀 #3 (empty fields rejected at dispatch).
- R7 anti-误杀 #4 (wrong type rejected).
- R7 anti-误杀 #5 (no auto-derivation): test asserts no runtime call
  path derives the tool call from `accumulated_tool_results`,
  `user_message`, or any other channel.

**R2.a#5-ext tests** (positive + negative):
- Positive: `phase=RESOLVE, activeUseCase=UC-J, lastAction="answer",
  bucket="max-repeated-same-action"` → `clarification_budget_exhausted`.
- Positive: same with `lastAction="clarify"` → same.
- Positive (preserve existing): `phase=DISCOVER, activeUseCase=null,
  lastAction="answer", bucket="max-repeated-same-action"` →
  `clarification_budget_exhausted`.
- Negative anti-误杀 #1 (RESOLVE non-intake UC):
  `phase=RESOLVE, activeUseCase=UC-A` → `turn_budget_exhausted`.
- Negative anti-误杀 #2 (no active UC):
  `phase=RESOLVE, activeUseCase=null` → `turn_budget_exhausted`.
- Negative anti-误杀 #3 (tool-call repeat):
  `lastAction="search_knowledge"` → `turn_budget_exhausted`.
- Negative anti-误杀 #4 (other budget): `bucket="max-faq-miss"` → existing
  `faq_miss_threshold_exceeded` (unchanged).
- Negative anti-误杀 #5: assert return value is one of existing 23
  enum values (no new enum).

### #7 — Backend rebuild + integration smoke (no real-LLM run)

After #1-#6: rebuild (`cd server && mvn -o -DskipTests package`),
restart, hit
`POST /v1/chat/sessions/<id>/messages` with a synthetic tool-result
turn that invokes `update_intake_fields`. Confirm:
- Tool dispatches (no 404 / 5xx).
- `session.intakeFields` updates as expected.
- Trace captures a `ToolEvent` for the call.
- Subsequent `request_handover` with intake complete passes the
  validator.

Dispatch-wiring evidence only; the outcome re-bless is the
milestone-shared run at M-Auto-6 close. Document the integration smoke
in handoff §1 with request/response shapes.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **R7 does NOT bypass the handover validator.**
2. **R7 does NOT auto-derive from runtime.** LLM-invoked only.
3. **R7's `fields` is a free-form string→string map.** No per-UC schema.
4. **R2.a#5-ext preserves anti-误杀 #12 spirit.**
5. **No new `escalation_reason` enum value.**
6. **No `IntakeFieldsRegistry` content change.**
7. **No `SkillGuardrailDispatcher` change.**
8. **No yaml / prompt / CaseSpec / simulator / scoring touch.**
9. **No new Tier-0 invariant.**
10. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
    UNCHANGED.**

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `server/src/main/java/.../tools/UpdateIntakeFieldsTool.java` (new)
- `server/src/main/java/.../ContextProjectionBuilder.java` (tool schema
  projection)
- `server/src/main/java/.../ControlKernel.java` (R2.a#5-ext mapping)
- Tool registry / dispatch wiring file (per #3 audit; likely
  `ToolPolicyEnforcer` or `ToolDispatcher`)
- `server/src/main/java/.../AgentRunLoopImpl.java` IF #3 chose path
  (β) and helper extraction is needed
- `server/src/test/java/.../UpdateIntakeFieldsToolTest.java` (new)
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java`
  (extend)
- (If #3 path β) characterization test class
- `docs/sprints/sprint-080-handoff.md` (dev handoff)

**Files FORBIDDEN to edit**:

- `IntakeFieldsRegistry.java` (content unchanged).
- `SkillGuardrailDispatcher.java`.
- `BudgetChecker.java`.
- `FormContextIngestionService.java`.
- Any prompt / yaml / CaseSpec under `server/src/main/resources/`.
- Any UI file (B's surface; B closed).
- Any `eval_interactive/` file.
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- #3 audit surfaces phase / UC-dependent behaviour the new tool would
  break → STOP, surface to deliver-agent.
- #5 audit surfaces a SECOND production call site to
  `mapBudgetToEscalationReason` beyond `:305-313` → STOP (signature
  change needs widened verification).
- Tool registration requires > ~10 LOC outside the registry constructor
  → STOP (unfamiliar DI shape).
- Any test in #6 fails in a way that suggests the validator is being
  bypassed → STOP, anti-误杀 #1 violation.

## Test / eval requirements

- All new tests in #6 GREEN.
- Existing Java baseline `1297 / 1 / 0 / 2` preserved + R7 +
  R2.a#5-ext additions (~+15-20 tests). Sole pre-existing failure
  (`OQ-S41.5`) preserved.
- Eval pytest `553` UNCHANGED (no eval-side change).
- Autoloop pytest UNCHANGED.
- Backend integration smoke at #7 documented in handoff §1.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence
  is the M-Auto-6 milestone-shared re-bless after C-1 + C-2 both land.
- Mocked-LLM tests for the projection surface (R7 tool schema + R7
  description visibility) are wiring evidence per §5.7; real evidence
  is the milestone re-bless.

## §7 Layer-classification + anti-hardcode stanza

```markdown
## Layer-classification + anti-hardcode stanza

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
  merge path (either invoked directly or via a shared helper extracted
  by the #3 audit — behaviour-equivalent to the `request_handover`
  persist path).
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

## Codex review plan (per `process/milestone-framework.md` §4.3)

**Per-sub-sprint Codex review REQUIRED** because R7 adds an LLM-facing
tool name (semantic-touching per `iteration_governance.md` §7 +
`process/milestone-framework.md` §4.3). R2.a#5-ext alone would be
exempt as pure infra, but bundled with R7 triggers semantic-touching
review.

Codex prompt artifact: `compact/sprint-080-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close, embeds §4.1 nine-question
kernel + §7 stanza + file-path fence + anti-误杀 invariants +
generalization coverage).

**Focus points for Codex** (per §4.3):

- Q1: confirm R7's `fields` schema is a free-form string→string map
  with NO per-UC enumeration; confirm R2.a#5-ext's new RESOLVE-intake
  branch uses `IntakeFieldsRegistry.isIntakeUseCase(activeUseCase)`
  and NOT a hard-coded UC list.
- Q3: confirm R7 surfaces the tool description with reference to
  `required_intake_fields_for_active_uc` (delegates field choice to
  the registry-driven projection); confirm R2.a#5-ext's mapping
  condition preserves anti-误杀 #12 spirit.
- Q4: confirm R7's persist semantics are byte-equivalent to the
  `request_handover` persist path on a fixed input.
- Q5: confirm no semantic decision moved from LLM to Java.
- Q7: confirm safety floor unchanged (validator unmodified; grounding
  unaffected).
- Q8: confirm generalization coverage matches the §7 stanza counts.
- Q9: no temporary hardcode; all changes are durable.

## Handoff requirements (dev authors `docs/sprints/sprint-080-handoff.md`)

§1 of the handoff must include:

- For each of #1-#5: file:line ranges + rationale + the test name(s)
  that gate it.
- **#3 audit outcome**: which path (α or β) was chosen; if β, the
  characterization test name + refactor diff bullets.
- **#5 design choice**: which overload approach (A or B); cited
  evidence of all production call sites.
- **#7 integration smoke**: request shape + response body + trace event
  shape for one `update_intake_fields` invocation, plus the follow-up
  `request_handover` call that demonstrates the validator path
  unchanged.
- Java test results (full numeric).
- Eval pytest / autoloop pytest results (UNCHANGED).
- STOP confirmations:
  - File fence respected.
  - No `IntakeFieldsRegistry` content changed.
  - No `SkillGuardrailDispatcher` change.
  - No new `escalation_reason` enum value added.
  - No yaml / prompt / CaseSpec / simulator / scoring touched.
  - `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md` UNCHANGED.
  - No real-LLM outcome re-bless launched (deferred to milestone close).
- A clear "wiring evidence" vs "outcome evidence" separator per §5.7.

## Commit discipline

Recommended commit split:

1. **Commit 1 — R7 #3 PRE-FIX audit refactor (path β only)**:
   `persistInlineIntakeFields` extraction + characterization test.
2. **Commit 2 — R7 #1 + #2 + #4**: `UpdateIntakeFieldsTool.java` +
   dispatch + tool registration + projection schema +
   `UpdateIntakeFieldsToolTest`.
3. **Commit 3 — R2.a#5-ext (#5)**: `ControlKernel.java` mapping
   signature extension + `MapBudgetToClarificationLabelTest` extension.
4. **Commit 4 — Integration smoke evidence (#7)**: handoff only (or
   +1 LOC logging if needed).
5. **Commit 5 — Dev handoff**: `docs/sprints/sprint-080-handoff.md`.

## Self-check checklist (dev completes before claiming done)

- [ ] Each of #1-#5 implemented with file:line ranges in handoff §1.
- [ ] #3 audit outcome documented; if β, characterization test green.
- [ ] #5 design choice documented; all call sites consistent.
- [ ] All R7 anti-误杀 #1-#5 + R2.a#5-ext anti-误杀 #1-#5 GREEN.
- [ ] R7 validator non-bypass verified.
- [ ] R7 no-auto-derivation verified.
- [ ] R2.a#5-ext RESOLVE non-intake UC negative: `turn_budget_exhausted`
      preserved.
- [ ] R2.a#5-ext tool-call negative: `turn_budget_exhausted` preserved.
- [ ] No new `escalation_reason` enum value.
- [ ] No `IntakeFieldsRegistry` content changed.
- [ ] No `SkillGuardrailDispatcher` reject-logic change.
- [ ] Java baseline `1297/1/0/2` + new tests, no regressions.
- [ ] Eval pytest `553` unchanged.
- [ ] No file outside the file fence touched.
- [ ] Integration smoke at #7 documented.
- [ ] §7 stanza copied verbatim into handoff.
- [ ] `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
      UNCHANGED.
- [ ] No outcome-evidence re-bless launched.

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + Sub-sprint C-2 launch.
