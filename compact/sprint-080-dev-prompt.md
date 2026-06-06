# Sprint 080 / S-Auto-25 / M-Auto-6 — Sub-sprint C-1 — Dev Prompt (R7 update_intake_fields tool + R2.a#5-ext phase-aware re-map RESOLVE-intake extension)

> **Planning-context artifact**: this dev prompt is drafted at the
> Sub-sprint A close + B open transition (2026-06-06) alongside
> Sub-sprint C-2's `sprint-081-dev-prompt.md` as the planned scope for
> the third sub-sprint of M-Auto-6. The CANONICAL active contract is
> `docs/sprint_objective.md`, which currently holds Sub-sprint B. At
> Sub-sprint B close + visual verification, deliver-agent will replace
> `sprint_objective.md` with this sub-sprint's contract and dev launches
> from here.
>
> C-1 may run in parallel with C-2 per the human 2026-06-06 packaging
> decision (independent surfaces — C-1 = intake/clarification runtime
> contract; C-2 = citation UX + corpus retrieval governance). If both
> are dev-launched in parallel, each lands on its own branch /
> commit-range to keep causal attribution clean for the milestone-shared
> Codex review at M-Auto-6 close.

## Role identity

You are the **dev agent** for **Sprint 080 / S-Auto-25 / M-Auto-6
Sub-sprint C-1**.

Your one-sentence goal: ship R7 — a new `update_intake_fields(fields={...})`
no-side-effect tool that lets the LLM accumulate `session.intakeFields`
across turns without triggering the handover validator — plus
R2.a#5-ext, a narrow phase-aware re-map extension that covers
`RESOLVE + isIntakeUseCase(activeUseCase) + isFreeTextActionKey(lastAction)`
in addition to the existing DISCOVER + free-text path so that c14-style
RESOLVE-phase intake clarification budget hits label correctly as
`clarification_budget_exhausted` instead of `turn_budget_exhausted`.
Zero semantic procedure / wording change; the c14 OBS-S6 (LLM inferring
intake fields from free text) is autoloop work AFTER M-Auto-6 close and
R7 is its runtime enabler.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded via this prompt).
2. **This prompt** (full sub-sprint contract — do NOT read
   `docs/sprint_objective.md` for scope when this prompt is the active
   contract).
3. **Code anchors only as needed** (cited inline in §Scope below).

## Cumulative context (one-page)

- **M-Auto-5 CLOSED 2026-06-05** (Class A; baseline_dir =
  `m-auto-5-baseline-20260604-simfixed-stalledfix`).
- **M-Auto-6 ACTIVE**:
  - Sub-sprint A (R1.a + R2.a + R4.a) dev-side closed 2026-06-06;
    code shipped at commits `a873d18 / 840a5e2 / 247da11 / af44903`;
    Codex APPROVE_S_AUTO_23 at `62b4d7b`; P2 smoke wiring evidence at
    `eval_interactive/results/20260605-105339/`. `baseline_dir` and
    `docs/current_eval_baseline.md` UNCHANGED until milestone close.
  - Sub-sprint B (R3.a + R3.b + R3.c admin observability) currently
    active OR closed by the time this sub-sprint launches.
  - Sub-sprint C-2 (R5 + R6 — citation UX + corpus governance) may be
    running in parallel.
- **Source-of-truth proposal**:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  §4.7 (R2.a#5-ext) + §4.8 (R7) + §6.7 (Sub-sprint C-1 packaging).
- **Trigger case**: c14 — UC-J intake. Bot collects `listing_id`
  (turn 1), then `report_type` (turn 2), then asks for `listing_id`
  again (turn 3, state loss because nothing persisted), then
  `turn_budget_exhausted` (turn 4, mislabeled). Two root causes that
  R7 + R2.a#5-ext together address:
  1. `persistInlineIntakeFields(session, call)` at
     `AgentRunLoopImpl.java:487-494` only fires on `HANDOVER_TOOL` calls,
     so the LLM cannot accumulate partial intake state across turns
     without "send-or-stall" dilemma.
  2. `ControlKernel.mapBudgetToEscalationReason(bucket, phase, lastAction)`
     at `:763-771` (shipped at S-Auto-23 commit `247da11`) is correctly
     restricted to DISCOVER per anti-误杀 #12; the RESOLVE-phase intake
     UC free-text repeat still mislabels.

## Class (layer classification)

**Layer (per `iteration_governance.md` §3.2):**

- **R7** → `skill_state` + `infra` + `prompt_projection`. New tool name
  declared in the LLM-facing tool list (projection surface — triggers
  §7 stanza). `skill_state` because the tool's effect is cross-turn
  accumulation of `session.intakeFields`. `infra` because the tool
  reuses existing `persistInlineIntakeFields` merge logic at a new
  dispatch point without server-side semantic decision.
- **R2.a#5-ext** → `infra`. Control-plane labeling extension to the
  already-shipped phase-aware re-map; no new semantic surface; no new
  enum value.

**§7 stanza requirement:** **REQUIRED** (R7 adds a new LLM-facing tool
name; semantic-touching per `iteration_governance.md` §7).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.

- R7's no-side-effect tool only WRITES `session.intakeFields` via the
  existing merge path. It does NOT bypass the handover validator (which
  remains the last line of defence for whether intake is complete).
- R2.a#5-ext extends an existing mapping function with one additional
  AND-guarded condition; no new mapping; no new enum value; no removal
  of existing guards.

**Semantic hardcode:** No semantic hardcode introduced.

- R7's `fields` argument is a free-form `string → string` map
  (mirroring the `intake_fields` slot shipped at R1.a). NO per-UC
  enumeration of property names; NO server-side derivation of which
  fields belong to which UC (the per-UC required-fields contract is
  already projected via `required_intake_fields_for_active_uc` from
  R1.a #2). The new tool just persists what the LLM passes.
- R2.a#5-ext's extension is `RESOLVE phase + isIntakeUseCase(activeUseCase)
  + isFreeTextActionKey(lastAction)`. `IntakeFieldsRegistry.isIntakeUseCase`
  is the SAME classification the existing R1.a #2 projection uses —
  zero new per-UC matrix.

## Goal

After this sub-sprint ships:

- **R7 wiring**: a new `update_intake_fields(fields={...})` tool is
  declared in the projection's `tool_schemas` (same level as
  `request_handover`, `search_knowledge`, etc.); the server dispatches
  it via the standard `ToolDispatcher` path; the dispatch handler calls
  the existing `persistInlineIntakeFields(session, call)` merge logic
  (same as the `request_handover` intake-fields persist path) WITHOUT
  routing through the handover validator. The tool returns
  `{"status": "ok", "fields_merged": <count>}` and emits a normal
  `ToolEvent` to the trace.
- **R2.a#5-ext mapping**: the existing 3-arg
  `mapBudgetToEscalationReason(bucket, phase, lastAction)` overload at
  `ControlKernel.java:763-771` is extended (or wrapped in a 4-arg
  overload — see §Scope #5 for the choice rationale) to also fire on
  `phase=RESOLVE AND isIntakeUseCase(activeUseCase) AND isFreeTextActionKey(lastAction)`.
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
- Changing skill yaml procedures (OBS-S6 is autoloop work after
  M-Auto-6 close).
- Any LLM-side semantic decision (the LLM decides WHEN to call
  `update_intake_fields` and what fields to pass; the runtime does NOT
  derive the call from message content).

## Scope (executable, #1–#7)

### #1 — R7 step 1: `UpdateIntakeFieldsTool.java` new tool class

**Anchor (new file):** `server/src/main/java/.../tools/UpdateIntakeFieldsTool.java`.

**Change:** New tool class implementing the project's standard `Tool`
interface (mirror the structure of an existing simple no-side-effect
tool — e.g. the read-only `get_customer_context` or `lookup_listing_or_ad`).

Tool spec:
- Name: `update_intake_fields`.
- Arguments schema: `{"fields": {"type": "object", "additionalProperties": {"type": "string"}}}` (free-form string→string map; mirror the `intake_fields` slot shipped at R1.a).
- Required arguments: `fields` (non-empty validation at the dispatch
  layer — the tool returns an error result if `fields` is null or
  empty; this is structural validation, not semantic).
- Description (LLM-facing): "Persist partial intake fields collected
  from the user so they survive across turns. Use this when you have
  identified one or more intake fields the user has provided but you
  do not yet have a complete set to call `request_handover`. Does NOT
  trigger handover. See `required_intake_fields_for_active_uc` in the
  projection for the active UC's required-fields list."
- Tool result body: `{"status": "ok", "fields_merged": <int>,
  "fields_persisted": [<list of merged field names>]}` (no semantic
  payload; structural confirmation only).
- Trace event: standard `ToolEvent` (same shape as other tools).

**Why:** c14 root cause #1 — LLM cannot accumulate intake state across
turns. R1.a shipped the "send everything at once" path; R7 ships the
"send incrementally" path. Together they cover both LLM strategies.

### #2 — R7 step 2: dispatch handler reuses `persistInlineIntakeFields`

**Anchor:** the tool dispatch path that currently handles existing tool
calls. The exact location depends on how `ToolDispatcher` routes — for
the new tool the handler should:
- Validate `arguments.fields` is a non-empty `Map<String, String>`
  (structural; reject with the standard tool-validation error shape if
  empty or wrong type).
- Invoke the existing `persistInlineIntakeFields(session, call)` merge
  logic (currently at `AgentRunLoopImpl.java` per S-Auto-23) — OR
  refactor that method to a shared helper that both the
  `request_handover` path and the `update_intake_fields` path call.
  Pre-fix audit REQUIRED before choosing: see §3.

**Why:** reuse keeps the merge semantics identical; no new code path
that could diverge from `request_handover`'s persist behaviour.

### #3 — R7 step 3: PRE-FIX `persistInlineIntakeFields` reuse audit

**Audit task (REQUIRED before #2 wiring):** read
`AgentRunLoopImpl.java:487-494` + the
`persistInlineIntakeFields(session, call)` method body. Confirm:

- (a) Whether `persistInlineIntakeFields` is currently `private` to
  `AgentRunLoopImpl` and tightly coupled to the `request_handover`
  dispatch path, OR is already structured as a reusable helper.
- (b) Whether the merge logic depends on any `call` field other than
  `call.arguments.intake_fields` (e.g. inspecting
  `call.arguments.escalation_reason` or `call.toolName`).
- (c) Whether the merge logic gates on `IntakeFieldsRegistry.isIntakeUseCase(activeUseCase)`
  before persisting, and what happens for non-intake UCs.

Two outcomes:

- **(α) Already reusable** — invoke directly from the new tool handler.
  Document in handoff §1.
- **(β) Tightly coupled** — refactor to a shared helper (e.g.
  `IntakeFieldsMerger.merge(session, fieldsMap)`); the
  `request_handover` path now calls the helper too. The refactor MUST
  be byte-equivalent in behaviour on the `request_handover` path
  (verified by a characterization test that compares before/after on a
  fixed input). Document the refactor diff in handoff §1.

**Hard fence:** the audit MUST NOT change the merge semantics. If the
audit surfaces that the current `request_handover` persist path has a
subtle behaviour the new tool would break (e.g. an off-by-one or a
phase-dependent guard), STOP and surface to deliver-agent.

### #4 — R7 step 4: tool registration + projection schema declaration

**Anchors:**
- The tool registry / `@Configuration` class where existing tools are
  registered (likely `ToolPolicyEnforcer` or `ToolDispatcher` constructor;
  pre-fix audit needed to find the canonical registration point).
- `ContextProjectionBuilder.java` `tool_schemas` emission section
  (where `request_handover` schema is built — same area as R1.a #1
  shipped at `:262-281`).

**Changes:**
- Register `UpdateIntakeFieldsTool` in the tool registry so
  `ToolDispatcher` can route to it.
- Add the tool's arguments-schema projection to `tool_schemas` so the
  LLM sees the tool's existence + description + arguments contract on
  every turn.

**Hard fence:** the projection MUST include the tool's description
referencing `required_intake_fields_for_active_uc` so the LLM knows
which fields to populate. The tool description MUST NOT enumerate
per-UC field names (those come from the registry-driven projection).

### #5 — R2.a#5-ext: extend phase-aware mapping to RESOLVE-intake free-text

**Anchor:** `ControlKernel.java:763-771` (the 3-arg
`mapBudgetToEscalationReason(bucket, phase, lastAction)` overload
shipped at S-Auto-23 commit `247da11`).

**Two design choices** (proposal §4.7 + §6.7 outline):

**(A) Extend the 3-arg overload signature to take `activeUseCase`**:
the existing 3-arg becomes a 4-arg `mapBudgetToEscalationReason(bucket,
phase, lastAction, activeUseCase)`. All call sites updated. Mapping
condition expands:

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

**(B) Add a 4-arg overload alongside the 3-arg one**: the 3-arg
overload stays as-is (delegates to the new 4-arg with `activeUseCase=null`);
the 4-arg overload adds the RESOLVE-intake branch.

**Choice recommendation**: **(A)** — single source of truth for the
mapping; cleaner signature; the only production call site is at
`ControlKernel.java:305-313` per Codex §1 R2.a #5 verdict, so updating
all call sites is bounded. Document the choice in handoff §1.

**Hard constraint** — Anti-误杀 #12 spirit:
- RESOLVE + non-intake UC (UC-A / UC-B / UC-D / UC-F / UC-FP / etc.) +
  any action → `turn_budget_exhausted` (existing behaviour preserved).
- Any phase + any tool-call repeat (action is NOT free-text) →
  `turn_budget_exhausted` (existing behaviour preserved).
- DISCOVER + intake UC + free-text → `clarification_budget_exhausted`
  (existing R2.a #5 behaviour preserved).
- RESOLVE + intake UC + free-text → `clarification_budget_exhausted`
  (NEW R2.a#5-ext behaviour).

### #6 — Anti-误杀 test suite (R7 + R2.a#5-ext)

**Anchor (new test classes):**

- `server/src/test/java/.../UpdateIntakeFieldsToolTest.java`
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java`
  (existing — extend with RESOLVE-intake positive + RESOLVE non-intake
  negative + tool-call negative).
- If #3 chose path (β), a characterization test that compares the
  refactored helper's output to the pre-refactor inline merge on a
  fixed input.

**Tests** (positive + negative):

R7:
- **R7 positive**: `update_intake_fields(fields={"report_type": "scam"})`
  invoked on a UC-J session → `session.intakeFields` contains
  `{"report_type": "scam"}` AND no handover validator fires.
- **R7 positive (multi-call accumulation)**: two consecutive
  `update_intake_fields` calls with different fields → `session.intakeFields`
  contains the union. (Mirrors c14's intended path.)
- **R7 anti-误杀 negative #1 (validator non-bypass)**: after one or more
  `update_intake_fields` calls that populate ONLY a subset of the active
  UC's required-fields, a subsequent `request_handover` call with NO
  `intake_fields` arg → handover validator STILL rejects with
  `intake_required_fields_missing_for_intake_complete` (R7 does NOT
  bypass the validator; the only way intake completes is when ALL
  required fields are present, regardless of which tool persisted them).
- **R7 anti-误杀 negative #2 (full-stash → handover passes)**: after
  `update_intake_fields` calls that populate ALL required fields,
  `request_handover(escalation_reason="intake_complete_for_uc_j")` → the
  handover succeeds (no validator rejection). This proves R7's persist
  semantics are equivalent to the R1.a path.
- **R7 anti-误杀 negative #3 (empty fields rejected at dispatch)**:
  `update_intake_fields(fields={})` → tool returns the standard
  structural-validation error; `session.intakeFields` UNCHANGED.
- **R7 anti-误杀 negative #4 (wrong type rejected)**:
  `update_intake_fields(fields="not-a-map")` → tool returns validation
  error; `session.intakeFields` UNCHANGED.
- **R7 anti-误杀 negative #5 (no auto-derivation)**: NEVER auto-call
  `update_intake_fields` from runtime. The tool MUST be LLM-invoked.
  A test asserts no runtime call path derives the call from
  `accumulated_tool_results`, `user_message`, or any other channel.

R2.a#5-ext:
- **Positive #1**: `phase=RESOLVE, activeUseCase=UC-J,
  lastAction="answer", bucket="max-repeated-same-action"` →
  `clarification_budget_exhausted`.
- **Positive #2**: `phase=RESOLVE, activeUseCase=UC-J,
  lastAction="clarify", bucket="max-repeated-same-action"` →
  `clarification_budget_exhausted`.
- **Positive #3** (preserve existing R2.a #5 behaviour):
  `phase=DISCOVER, activeUseCase=null, lastAction="answer",
  bucket="max-repeated-same-action"` → `clarification_budget_exhausted`.
- **Negative anti-误杀 #1** (RESOLVE non-intake UC):
  `phase=RESOLVE, activeUseCase=UC-A, lastAction="answer",
  bucket="max-repeated-same-action"` → `turn_budget_exhausted`.
- **Negative anti-误杀 #2** (RESOLVE non-intake UC, no active UC):
  `phase=RESOLVE, activeUseCase=null, lastAction="answer",
  bucket="max-repeated-same-action"` → `turn_budget_exhausted`.
- **Negative anti-误杀 #3** (tool-call repeat, any phase, any UC):
  `phase=RESOLVE, activeUseCase=UC-J, lastAction="search_knowledge",
  bucket="max-repeated-same-action"` → `turn_budget_exhausted`.
- **Negative anti-误杀 #4** (other budget bucket):
  `phase=RESOLVE, activeUseCase=UC-J, lastAction="answer",
  bucket="max-faq-miss"` → existing `max-faq-miss` mapping
  (`faq_miss_threshold_exceeded` per the existing single-arg overload)
  — NOT relabeled.
- **Negative anti-误杀 #5** (no new enum): test asserts the return
  value is one of the existing 23 enum values, never a new one.

### #7 — Backend rebuild check + integration smoke (no real-LLM run)

**Task:** after #1-#6 land, rebuild the backend (`cd server && mvn -o
-DskipTests package`), restart, hit
`POST /v1/chat/sessions/<id>/messages` with a synthetic tool-result
turn that invokes `update_intake_fields`, and confirm:

- The tool dispatches successfully (no 404 / 5xx).
- `session.intakeFields` updates as expected.
- The trace captures a `ToolEvent` for the call.
- A subsequent `request_handover` call with intake complete passes the
  validator.

This is dispatch-wiring evidence, NOT outcome evidence. The outcome
re-bless is the M-Auto-6 milestone-shared run. Document the integration
smoke in handoff §1 with the request/response shapes.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **R7 does NOT bypass the handover validator.** The validator at
   `SkillGuardrailDispatcher.java:265-298` is the last line of defence
   for whether intake is complete; R7 just makes intake accumulation
   visible to it. Any test that verifies handover behaviour with
   partial intake MUST still see the validator reject.
2. **R7 does NOT auto-derive from runtime.** The tool MUST be LLM-invoked.
   Runtime never calls `update_intake_fields` itself; never derives
   from `accumulated_tool_results` / `user_message` / `form_context`
   / etc.
3. **R7's `fields` argument is a free-form string→string map.** NO
   per-UC enumeration of property names in the tool schema; NO server-side
   validation that fields belong to the active UC (the validator does
   that on the handover path).
4. **R2.a#5-ext preserves anti-误杀 #12 spirit.** RESOLVE non-intake UC
   + any phase + any tool-call repeat MUST remain
   `turn_budget_exhausted`. Test coverage for each negative branch is
   non-negotiable.
5. **No new `escalation_reason` enum value.** R2.a#5-ext only re-maps
   to existing `clarification_budget_exhausted`.
6. **No `IntakeFieldsRegistry` content change.** R7 reads the existing
   classification; R2.a#5-ext reads the existing classification. Both
   iterate the registry as the source of truth.
7. **No `SkillGuardrailDispatcher` change.** Validator semantics
   preserved.
8. **No yaml / prompt / CaseSpec / simulator / scoring touch.** OBS-S6
   (autoloop teaching LLM to use the new tool) is M-Auto-6+ work.
9. **No new Tier-0 invariant.** R7 is a new tool; tools are not Tier-0
   surface per `iteration_governance.md` §1.4.
10. **`baseline_dir` UNCHANGED. `docs/current_eval_baseline.md`
    UNCHANGED.** Both flip at milestone close.

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `server/src/main/java/.../tools/UpdateIntakeFieldsTool.java` (new)
- `server/src/main/java/.../ContextProjectionBuilder.java` (add tool
  schema)
- `server/src/main/java/.../ControlKernel.java` (R2.a#5-ext mapping)
- The tool registry / dispatch wiring file (whatever it is per the #3
  audit — likely `ToolPolicyEnforcer` or `ToolDispatcher`)
- `server/src/main/java/.../AgentRunLoopImpl.java` IF the #3 audit
  chose path (β) and a helper extraction is needed for
  `persistInlineIntakeFields`
- `server/src/test/java/.../UpdateIntakeFieldsToolTest.java` (new)
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java`
  (existing — extend with R2.a#5-ext cases)
- (If #3 path β) characterization test class for the
  `persistInlineIntakeFields` refactor
- `docs/sprints/sprint-080-handoff.md` (your dev handoff)

**Files FORBIDDEN to edit**:

- `IntakeFieldsRegistry.java` (content unchanged).
- `SkillGuardrailDispatcher.java` (reject logic unchanged).
- `BudgetChecker.java`.
- `FormContextIngestionService.java`.
- Any prompt / yaml / CaseSpec under `server/src/main/resources/`.
- Any UI file (R3.* is Sub-sprint B's surface).
- Any `eval_interactive/` file (CaseSpecs / simulator / scoring /
  harness).
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir`.
- `docs/current_eval_baseline.md`.

**STOP conditions**:

- #3 audit surfaces that `persistInlineIntakeFields` has phase-dependent
  or UC-dependent behaviour the new tool would break → STOP and surface
  to deliver-agent.
- #5 audit surfaces a SECOND production call site to
  `mapBudgetToEscalationReason` beyond the documented `:305-313` → STOP
  (the verification needs widening before signature change is safe).
- Tool registration discovery requires more than ~10 LOC changes
  outside the registry constructor → STOP and surface (tool dispatch
  may have an unfamiliar dependency injection shape).
- Any test in #6 fails in a way that suggests the validator is being
  bypassed → STOP, this is an anti-误杀 #1 violation.

## Test / eval requirements

- All new tests in #6 GREEN.
- Existing test baselines unchanged: Java `1244 / 1 / 0 / 2` baseline +
  the new R7 + extended R2.a#5-ext tests (delta: +~15-20 tests across
  the two suites). Sole pre-existing failure (`OQ-S41.5`) preserved.
- Eval pytest `553 passed, 0 failed` UNCHANGED (no eval-side change in
  this sub-sprint).
- Autoloop pytest UNCHANGED.
- Backend integration smoke at #7 documented in handoff §1.
- **No real-LLM re-bless at this sub-sprint close.** Outcome evidence
  is the M-Auto-6 milestone-shared re-bless after C-1 + C-2 land.
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
- R7's persist logic reuses the existing
  `persistInlineIntakeFields` merge path (either invoked directly or
  via a shared helper extracted by the #3 audit — behaviour-equivalent
  to the `request_handover` persist path).
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
  `required_intake_fields_for_active_uc` (delegates field choice to the
  registry-driven projection); confirm R2.a#5-ext's mapping condition
  preserves anti-误杀 #12 spirit.
- Q4: confirm R7's persist semantics are byte-equivalent to the
  `request_handover` persist path on a fixed input.
- Q5: confirm no semantic decision moved from LLM to Java
  (LLM owns when to call the new tool and what to pass; runtime owns
  schema validation + merge logic + correct label-mapping).
- Q7: confirm safety floor unchanged (the handover validator remains
  unmodified; R7 does not bypass it; grounding floor unchanged because
  R7 does not touch retrieval / FAQ paths).
- Q8: confirm generalization coverage matches the §7 stanza counts.
- Q9: no temporary hardcode; all changes are durable.

## Handoff requirements (you author `docs/sprints/sprint-080-handoff.md`)

§1 of your handoff must include:

- For each of #1-#5: file:line ranges + rationale + the test name(s)
  that gate it.
- **#3 audit outcome**: which path (α or β) was chosen, with cited
  evidence. If β, the characterization test name and a description of
  the refactor (before/after diff bullets).
- **#5 design choice**: which overload approach (A or B), with
  rationale; cited evidence of all production call sites.
- **#7 integration smoke**: request shape + response body + trace event
  shape for one `update_intake_fields` invocation, plus the follow-up
  `request_handover` call that demonstrates the validator path
  unchanged.
- Java test results (full numeric: passed / failed / skipped / errors).
- Eval pytest / autoloop pytest results (UNCHANGED).
- STOP confirmations:
  - File fence respected; no forbidden file touched.
  - No `IntakeFieldsRegistry` content changed.
  - No `SkillGuardrailDispatcher` change.
  - No new `escalation_reason` enum value added.
  - No yaml / prompt / CaseSpec / simulator / scoring touched.
  - `baseline_dir` UNCHANGED; `docs/current_eval_baseline.md`
    UNCHANGED.
  - No real-LLM outcome re-bless launched (deferred to milestone close).
- A clear "wiring evidence" vs "outcome evidence" separator per §5.7
  mocked-LLM gate.

## Commit discipline

Recommended commit split (per `prompt-artifact-rules.md` §9 + the
S-Auto-23 pattern):

1. **Commit 1 — R7 step 3 (#3) PRE-FIX audit refactor (only if path β
   chosen)**: `persistInlineIntakeFields` extraction to shared helper
   + characterization test confirming `request_handover` path is
   byte-equivalent.
2. **Commit 2 — R7 #1 + #2 + #4**: `UpdateIntakeFieldsTool.java` +
   dispatch wiring + tool registration + projection schema declaration
   + `UpdateIntakeFieldsToolTest`.
3. **Commit 3 — R2.a#5-ext (#5)**: `ControlKernel.java` mapping signature
   extension + `MapBudgetToClarificationLabelTest` extension (incl. all
   negatives).
4. **Commit 4 — Integration smoke evidence (#7)**: capture in handoff
   only; no code change (or +1 LOC if a logging adjustment helps).
5. **Commit 5 — Dev handoff**: `docs/sprints/sprint-080-handoff.md`.

Each commit message follows the established convention (see sprint-077
+ sprint-078 commits as reference). All commits are by the dev agent;
deliver-agent commits its own close-archive artifacts separately at
sub-sprint close.

## Self-check checklist (complete BEFORE claiming done)

- [ ] Each of #1-#5 implemented with file:line ranges captured in
      handoff §1.
- [ ] #3 audit outcome documented (path α or β); if β, characterization
      test green.
- [ ] #5 design choice documented (overload A or B); all call sites
      to `mapBudgetToEscalationReason` updated consistently.
- [ ] Each anti-误杀 test in #6 GREEN — R7 #1-#5 + R2.a#5-ext #1-#5
      negatives all pass.
- [ ] R7 validator non-bypass test (#1 anti-误杀): handover STILL
      rejects with partial intake even after `update_intake_fields`
      populated a subset.
- [ ] R7 no-auto-derivation invariant verified (#5 anti-误杀): no
      runtime call path derives the tool call.
- [ ] R2.a#5-ext RESOLVE non-intake UC negative: `turn_budget_exhausted`
      preserved (anti-误杀 #12 spirit).
- [ ] R2.a#5-ext tool-call negative: `turn_budget_exhausted` preserved
      regardless of phase / UC.
- [ ] No new `escalation_reason` enum value added.
- [ ] No `IntakeFieldsRegistry` content changed (only iterated).
- [ ] No `SkillGuardrailDispatcher` reject-logic change.
- [ ] Java baseline `1244 / 1 / 0 / 2` + new tests, no regressions.
- [ ] Eval pytest `553` unchanged.
- [ ] No file outside the file fence has been touched.
- [ ] Integration smoke at #7 documented in handoff §1.
- [ ] §7 stanza copied verbatim into the handoff.
- [ ] `baseline_dir` UNCHANGED.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.
- [ ] No outcome-evidence re-bless launched at this sub-sprint close
      (the milestone-shared re-bless runs after C-1 + C-2 both land).

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + Sub-sprint C-2 status check (if not
parallel-launched).
