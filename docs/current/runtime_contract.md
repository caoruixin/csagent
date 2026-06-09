---
title: Runtime contract (current delivered behavior)
doc_tier: current-runtime
status: current
implementation_status: implemented
source_of_truth: server/src/main/java/com/gumtree/csagent/service/runtime/ + server/src/main/resources/config/
last_reviewed: 2026-05-10
review_cadence: every_3_to_5_sprints
notes: >
  Concise summary of the delivered runtime: components, phase model,
  PhasePlan, tool-use model, projection. For tool argument and return
  schemas see customer_service_tool_spec_v0_3.md.
---

> This document is the current runtime contract. Where it disagrees with
> any older spec (phase0/phase1/phase2/phase3 or
> `customer_service_tool_spec_v0_2.*`), the code under
> `server/src/main/java/com/gumtree/csagent/service/runtime/` and
> `server/src/main/java/com/gumtree/csagent/service/tools/` plus the
> YAML in `server/src/main/resources/config/` is the source of truth.

# Runtime contract

This file is a tight, code-grounded summary of the bot's *delivered*
runtime behavior. Older specs describe broader intent and historical
shape; read those for "why", read this for "what currently runs".

## Component ownership

The single end-to-end orchestration entry point is
`ControlKernel.processMessage(BotSession, String)`
(`server/.../service/runtime/ControlKernel.java`). It owns turn counting,
deterministic distress / explicit-escalation detection, drift, budget
enforcement, phase planning, run-loop invocation, phase transition, and
turn persistence. Other components are subordinate workers:

- **`PhaseEvaluator`** (`runtime/PhaseEvaluator.java`) — produces a
  `PhasePlan` for the current phase + UC via `plan(...)`, and after the
  loop runs maps the `AgentRunResult` to a `PhaseTransitionDecision` via
  `interpretRunResult(...)`. Also owns the canonical 23-value
  `escalation_reason` set and the legacy `evaluate(...)` fallback path.
- **`AgentRunLoop`** / **`AgentRunLoopImpl`** (`runtime/AgentRunLoop*.java`)
  — mechanical model↔tool worker. Builds a plan-aware projection,
  invokes the LLM, parses, validates each requested tool against
  `PhasePlan.allowedTools`, dispatches via `ToolDispatcher`, and loops
  up to `PhasePlan.maxToolSteps`. Owns the in-loop guards
  (S1 FAQ-grounded-resolve guard, intake-complete guard, progressive
  resolve guard, classify-use-case DISCOVER terminal).
- **`ContextProjectionBuilder`** (`runtime/ContextProjectionBuilder.java`)
  — builds the JSON projection the LLM sees. Owns the static
  `tool_schemas` registry (one schema per agent-visible tool) plus
  per-turn projection slots (session, plan, intake state, candidate
  UCs, drift / task history, terminal evidence, accumulated tool
  results).
- **`ToolDispatcher`** (`tools/ToolDispatcher.java`) — central dispatch
  for tool calls issued by the LLM via `AgentRunLoop`. Looks up the
  `Tool` bean, runs `ToolPolicyEnforcer`, applies the
  `request_handover` non-canonical-reason coercion, then executes
  the tool with latency tracking. **Note:** a small number of
  deterministic runtime paths invoke tool beans directly without
  going through `ToolDispatcher`; see "Direct runtime tool
  invocations" below.

The kernel does not invoke the LLM directly — every model call is
made by `AgentRunLoopImpl` (or, on the rollback path,
`PhaseEvaluator.evaluate(...)`). Tool execution, however, has the
direct exceptions listed below.

### Direct runtime tool invocations (bypass `ToolDispatcher`)

A handful of deterministic runtime paths construct a `Tool` bean
directly and call its `execute(...)`. These calls do **not** go
through `ToolDispatcher.dispatch`, so neither
`validateAgainstPlan` nor `ToolPolicyEnforcer` runs on them. They
exist to keep side effects under deterministic Java control rather
than letting the LLM order them via a tool call:

- `ControlKernel.createCaseIfNeeded` → `CreateCaseControlledTool.execute`
  (escalation-time case creation for UC-H/J/K).
- `PhaseEvaluator.createCaseIfAllowed` → `CreateCaseControlledTool.execute`
  (legacy intake-complete path on the rollback `evaluate(...)` flow).
- `FormContextIngestionService` → `GetCustomerContextTool.execute`
  (auto-trigger at session create when the pre-chat form provides
  an email and the candidate UC matches the per-UC allow set; see
  "Session create and form ingestion" below).

These callers are responsible for their own argument shape and per-UC
gating; they intentionally do not project a phase plan or emit
`TOOL_SCOPE_BLOCKED`.

### Run-loop routing (live config)

`server/src/main/resources/application.yml` sets
`agent.run-loop.enabled-phases: [RESOLVE_FAQ, RESOLVE_INTAKE, DISCOVER,
CONFIRM, CLOSE, ESCALATE]`. The kernel resolves the route key per turn
via `ControlKernel.computeRouteKey(...)` and routes through the agent
run loop **only** for configured, recognized post-INIT route keys:

- DISCOVER, CONFIRM, CLOSE, ESCALATE map directly to their phase name.
- RESOLVE maps to `RESOLVE_FAQ` when `activeUseCase ∈ {UC-A, UC-B,
  UC-C, UC-D, UC-E, UC-F, UC-FP}`, to `RESOLVE_INTAKE` when
  `activeUseCase ∈ {UC-G, UC-H, UC-I, UC-J, UC-K}`, and to `null`
  otherwise (e.g. `activeUseCase` is null or not in either set).
- A `null` route key, or one not present in `enabled-phases`, falls
  back to the legacy `PhaseEvaluator.evaluate(...)` path. The same
  fallback applies when `PhaseEvaluator.plan(...)` itself returns
  `null` for an unrecognized phase or unknown UC.

The legacy `evaluate(...)` path is therefore not dead code; it is the
documented rollback for unconfigured / unknown phase-UC combinations
and for shrinking `enabled-phases` if a regression demands it.

## Phase model

Phases and the allowed transition graph are loaded from
`server/src/main/resources/config/control-policy.yaml`:

```
INIT      -> DISCOVER
DISCOVER  -> RESOLVE | ESCALATE
RESOLVE   -> CONFIRM | ESCALATE
CONFIRM   -> CLOSE | RESOLVE | DISCOVER | ESCALATE
ESCALATE  -> CLOSE
CLOSE     -> (terminal)
```

Budgets are also loaded from this file: `max-clarification-rounds: 2`,
`max-faq-miss: 2`, `max-bot-turns-faq: 15`, `max-bot-turns-intake: 10`,
`max-repeated-same-action: 2`, `max-total-bot-turns: 25`. Drift
thresholds (`minor-drift-similarity: 0.85`,
`soft-shift-confidence: 0.6`) live alongside.

`ControlPolicyService` loads this YAML and exposes it to the kernel and
projection builder.

## Current PhasePlan concept

`PhasePlan` is a Java record (`server/.../model/PhasePlan.java`) emitted
by `PhaseEvaluator.plan(...)`. It carries:

- `phase`, `useCase` (may be null in DISCOVER),
- `objective` (one-line mission statement),
- `allowedTools` — hard whitelist for the loop,
- `requiredContextKeys` — projection sections the plan expects,
- `maxToolSteps` — server-side loop bound (NOT projected),
- `allowInterimMessage`,
- `validTerminalOutcomes` — `TerminalOutcome` enum subset,
- `systemInstruction`, `groundingInstruction`, `escalationPolicy`
  (free text, projected verbatim).

For every configured / recognized live route key (see "Run-loop
routing" above), `PhaseEvaluator.plan(...)` returns a non-null plan
matching the table below. Outside the configured set — e.g. when
`computeRouteKey(...)` returns `null` (no committed UC, or a UC that
is in neither the FAQ nor the intake set) or when the phase is one
the evaluator does not recognise — `plan(...)` may return `null` and
the kernel falls back to `PhaseEvaluator.evaluate(...)`:

| Phase | UC scope | `allowedTools` | `maxToolSteps` |
|---|---|---|---|
| DISCOVER | any (UC may be null) | `search_knowledge`, `classify_use_case` | 2 |
| RESOLVE (FAQ UCs A/B/C/D/E/F/FP) | per UC | `get_customer_context`, `search_knowledge`, `resolve_article`, `record_outcome`, `request_handover` | 4 |
| RESOLVE (INTAKE UCs G/H/I/J/K) | per UC | `request_handover` only | 3 |
| CONFIRM | any | `record_outcome`, `request_handover` | 2 |
| CLOSE | any | `record_outcome` | 2 |
| ESCALATE | any | `request_handover`, `record_outcome` | 2 |

Source: `PhaseEvaluator.plan(...)`,
`server/.../service/runtime/PhaseEvaluator.java`.

`PhasePlan.allowedTools` is the canonical per-phase tool whitelist
seen at the run loop, and the projection's `tool_schemas` array is
filtered to exactly those tools (see "Allowed-tool enforcement" below).

## Session create and form ingestion

Before the first `ControlKernel.processMessage(...)` runs for a
session, `SessionManager.createSession(...)` invokes
`FormContextIngestionService` against the inbound pre-chat form. When
the form carries an `email` and **at least one of the session's
initial candidate UCs** is in the per-UC allow set for
`get_customer_context` (i.e. an `anyMatch` over the candidate list
captured at session create — see `FormContextIngestionService`), the
ingestion service calls `GetCustomerContextTool.execute(session, ...)`
directly (without `ToolDispatcher`) and stores the sanitised result
on the session as `customerContext`, optionally `listingContext`, and
(when the listing status is `removed` / `moderated`) the moderation
review on `session.moderationContext`. The first two populate the
`customer_context` / `listing_context` projection slots described
below before the LLM is ever invoked; `session.moderationContext` is
read by `RequestHandoverTool` for handover identifiers but is not
currently surfaced as a projection slot — see TODO_DECISION
"Moderation context: written but not projected".

This is the only observed mechanism in the live runtime that
populates those projection slots. An LLM-issued
`get_customer_context` call does not copy back into
`session.customerContext` / `session.listingContext` /
`session.moderationContext`; its return value reaches the next
projection through `accumulated_tool_results.get_customer_context`
instead (see "Session vs. loop-local tool results" below). The
gate is evaluated **once at session create** against the initial
candidate UC list, so a session whose `activeUseCase` later commits
to a UC outside the `get_customer_context` allow set can still
already carry auto-populated customer context — the auto-trigger
fires (or not) before any classification has happened. Conversely,
sessions whose initial candidate list contained no allowed UC get
no auto-populated context, even if the active UC is later promoted
into the allow set.

## Current tool-use model

The LLM speaks a single-layer JSON contract: each model response is
parsed (`runtime/ActionParser.java`) into a `ParsedAction` of the form
`{ tool_calls: [...], user_message: "..." }`. There is no separate
"action" enum or routing/decision layer; every control-flow decision
is derived from these two fields plus session state. The legacy
five-action switch has been removed (see `PhaseEvaluator` class
javadoc; phase0 §0.6, phase3 §3.3.3 still describe this contract at
intent level).

Loop semantics (`AgentRunLoopImpl.run`):

1. Build a plan-aware projection.
2. Invoke the LLM (with deadline + unavailability handling).
3. Parse → `ParsedAction`.
4. If no tool calls and the message is a clarification: return
   `CLARIFICATION_NEEDED`. Otherwise return `FINAL_ANSWER`.
5. Otherwise, for each tool call: validate against
   `PhasePlan.allowedTools`, run in-loop guards (intake-complete,
   S1 FAQ-grounded-resolve, progressive resolve), dispatch via
   `ToolDispatcher`, accumulate results.
6. A **successful** `request_handover` dispatch short-circuits
   **further LLM iterations** — the loop sets a flag and returns
   `ESCALATE` only after the current model response's remaining
   `tool_calls` batch has finished dispatching (so any other tools
   the LLM emitted in the same response, e.g. `record_outcome`,
   still execute). A failed `request_handover` (e.g. malformed
   payload, `SalesforceService` failure) is recorded as an error in
   `accumulated_tool_results` and does not set the flag; the loop
   continues until a later step succeeds or `maxToolSteps` is
   exhausted (Sprint 9 §O1 terminal-state honesty; see
   `AgentRunLoopImpl` step 6d and the post-batch `if (handoverRequested)`
   return). A successful `classify_use_case` inside a DISCOVER plan
   that commits a non-blank `activeUseCase` short-circuits to
   `USE_CASE_IDENTIFIED`.
7. Repeat until terminal or `maxToolSteps` is exhausted.

Sequence numbers are monotonic across `LlmCallEvent` and `ToolEvent`
so traces can be replayed in order.

## Same-turn DISCOVER → RESOLVE replan

When the run loop returns `TerminalOutcome.USE_CASE_IDENTIFIED` from
a DISCOVER plan (i.e. a successful `classify_use_case` committed an
`activeUseCase`), `ControlKernel.processMessage(...)` performs **at
most one** bounded same-turn replan into RESOLVE before the kernel
hands the response back to the user (Sprint 8.1 §M3, see the §M3
block in `ControlKernel.processMessage`):

1. The kernel applies the deterministic DISCOVER → RESOLVE phase
   transition on the session (`session.setCurrentPhase("RESOLVE")`)
   if `controlPolicy.isValidTransition(...)` allows it, so the
   second `PhaseEvaluator.plan(...)` reflects the new phase. The
   pre-replan phase tracked for transition logging is also promoted
   to `RESOLVE` so the post-replan transition (e.g. RESOLVE →
   CONFIRM on a FAQ FINAL_ANSWER) stays inside the policy graph.
2. A **wall-clock budget gate** decides whether the same-turn
   RESOLVE attempt can fit. The shared `LlmCallContext` budget
   (used by both DISCOVER and the would-be RESOLVE call) must have
   at least `MIN_RESOLVE_REPLAN_BUDGET_MS` remaining. If not, the
   replan is skipped: the session stays in RESOLVE, and the kernel
   returns the DISCOVER result as a transitional response so the
   next user turn runs RESOLVE fresh. The per-invocation
   HTTP-attempt budget is re-armed at `FallbackLlmClient.chat`
   entry, so two successful DISCOVER calls do not poison the
   replan's first attempt — only wall-clock bounds whether the
   replan can fit.
3. If the budget is sufficient and `phaseEvaluator.plan(...)`
   returns a non-null RESOLVE plan, the kernel runs
   `agentRunLoop.run(resolvePlan, ...)` once more on the same user
   message and history, then **merges** the DISCOVER and RESOLVE
   `AgentRunResult`s via `mergeAgentRunResults(...)` so the
   persisted bot turn captures the full DISCOVER tool chain
   (`search_knowledge`, `classify_use_case`) alongside the RESOLVE
   tool chain. The merged result becomes the turn's outcome and
   feeds the post-loop phase transition.
4. If the second `plan(...)` returns null, the kernel falls back to
   the DISCOVER `USE_CASE_IDENTIFIED` transitional response.

This same-turn replan is bounded to a single attempt: there is no
recursion into further phase replans within one user turn, and the
flag is not used outside the DISCOVER `USE_CASE_IDENTIFIED` exit
condition.

## Current allowed-tool enforcement

There are two enforcement points, both run on every dispatch
**through `ToolDispatcher`** (i.e. on every LLM-issued tool call
inside `AgentRunLoop`). Direct runtime tool invocations (see
"Direct runtime tool invocations" above) bypass both filters and
must do their own gating in their caller.

1. **Plan whitelist** — `ToolDispatcher.validateAgainstPlan(plan, name)`
   checks that the tool name is in `PhasePlan.allowedTools`. Failure
   becomes a rejected `ToolEvent` with reason `tool_not_in_plan: ...`
   and the loop continues so the LLM can self-correct.
2. **Per-UC policy** — `ToolPolicyEnforcer` loads
   `server/src/main/resources/config/tool-policy.yaml` at startup.
   Each tool has `type` (`AGENT_VISIBLE` or `RUNTIME_ONLY`) and an
   `allowed-ucs` list (`[ALL]` is the wildcard). On a deny the
   dispatcher emits a `TOOL_SCOPE_BLOCKED` `BotEvent` and returns an
   error.

The plan-side filter is also applied to the projection: the
`tool_schemas` array surfaced to the LLM is filtered to the plan's
`allowedTools` (Sprint 8.1 §M3 in
`ContextProjectionBuilder.build(...)`), so the model never sees tools
outside the current `PhasePlan`. Per-UC policy may still reject a
plan-exposed tool at dispatch time — the two filters are independent
and `tool_schemas` is **not** the intersection of plan and policy.
For example, the FAQ RESOLVE plan exposes `get_customer_context` for
all FAQ UCs (`PhaseEvaluator.plan(...)` RESOLVE/FAQ branch), but
`tool-policy.yaml` only allows that tool for UC-A, UC-C, UC-D, UC-F,
UC-FP, UC-K, so on UC-B / UC-E the schema is projected and an actual
call would be denied with `TOOL_SCOPE_BLOCKED`.

`request_handover` carries an extra coercion: any non-canonical
`escalation_reason` is logged and rewritten to `service_degraded`
inside `ToolDispatcher.dispatch` before the tool runs (see
`ToolDispatcher.CANONICAL_ESCALATION_REASONS`).

## Current projection fields (high level)

`ContextProjectionBuilder.buildProjection(...)` (called by
`build(...)` for run-loop turns) emits a stable JSON shape. The keys
that ship today, grouped:

- **Session**: `session.session_id`, `session.current_phase`,
  `session.active_use_case`, `session.total_bot_turns`,
  `session.clarification_count`, `session.faq_miss_count`,
  `session.handling_state`.
- **Routing & state**: `task_summary`, `risk_flags`,
  `candidate_use_cases`, `previous_active_use_case`, `drift_type`,
  `current_task_type`, `primary_entity`, `issue_status_summary`,
  `task_status`, `last_entity_context_ref`.
- **Runtime alignment** (Sprint 12 §N0): `predicted_use_case`,
  `intent_relation`, `reroute_action`, `phase_transition_reason`,
  `resolve_disposition`, `record_outcome_guard_result`.
- **Terminal evidence**: `terminal_evidence.record_outcome_attempted`,
  `record_outcome_succeeded`, `record_outcome_success`.
- **History**: `drift_history`, `task_history` (reconstructed from the
  prior turns' persisted projections, last-10 window).
- **Budget**: a `budget_state` sub-object with the configured caps and
  current counters (`ContextProjectionBuilder.buildProjection` —
  `projection.set("budget_state", ...)`).
- **Per-turn inputs**: `form_context` (parsed pre-chat form, when
  the session has one), `customer_context`, `listing_context`
  (populated **only** by `FormContextIngestionService` at session
  create — see "Session create and form ingestion" above; the
  builder reads `customer_context` from `session.getCustomerContext()`
  and `listing_context` from `session.getListingContext()`, and an
  LLM-issued `get_customer_context` call does **not** mutate those
  session fields — its result instead surfaces in the next turn's
  projection under `accumulated_tool_results.get_customer_context`,
  see "Session vs. loop-local tool results" below),
  `conversation_history` (last 10 turns, with PII redacted on
  `user_message`), and the redacted `current_user_message` for the
  turn being processed. There is currently no `moderation_context`
  projection slot even when `session.moderationContext` is set; see
  TODO_DECISION "Moderation context: written but not projected".
- **Legacy non-loop knowledge slot**: when `buildProjection` is
  called with a non-empty pre-loaded `knowledgeHits` list (the
  legacy fallback path), the projection also carries
  `knowledge_hits` and a `knowledge_instruction` string telling the
  LLM not to call `search_knowledge` again. On the run-loop path,
  `build(...)` passes `null` for this argument and knowledge
  results instead arrive via `accumulated_tool_results.search_knowledge`.
- **Plan-aware additions** (`build(...)` after `buildProjection`):
  `phase_plan` (phase, use_case, objective, allowed_tools, plan
  instructions, valid_terminal_outcomes), filtered `tool_schemas`
  (replaces the per-UC `tool_schemas` set by the legacy projection),
  and `accumulated_tool_results` keyed by tool name.
- **Intake**: an `intake_state` object with `required_fields`,
  `fields_collected`, `fields_remaining`, and `intake_complete` —
  emitted only when the active UC is in the intake set
  (`IntakeFieldsRegistry.isIntakeUseCase`).

The exact field names and order are governed by code; this list is a
high-level index, not a schema.

## Session vs. loop-local tool results

`get_customer_context` is the only agent-visible tool today whose
return value semantically overlaps with a session-level projection
slot, so it is worth being explicit about how data flows:

- **Session-level slots** — `customer_context` in the projection is
  read from `session.getCustomerContext()` and `listing_context`
  from `session.getListingContext()`. The only current writer of
  those session fields (and of `session.moderationContext`, which
  has no corresponding projection slot today — see TODO_DECISION
  "Moderation context: written but not projected") is
  `FormContextIngestionService` at session create (see "Session
  create and form ingestion"). They persist across turns until the
  session ends.
- **Loop-local tool results** — `AgentRunLoopImpl` records each
  successful LLM-issued tool call into the run loop's
  `accumulatedToolResults` map and projects the latest entry per
  tool under `accumulated_tool_results.<tool_name>` on the next
  iteration of the same turn. There is no code path today that
  copies any tool's return value back into the session-level slots
  above; in particular an LLM-issued
  `get_customer_context(...)` populates
  `accumulated_tool_results.get_customer_context` only.

If the LLM needs the customer / listing context in a later turn
beyond the current run-loop iteration, today it must either rely
on the form-ingestion auto-trigger having already populated the
session slots or call `get_customer_context` again. Cross-turn
persistence of tool returns is not implemented for this surface.

## Current known deviations from older specs

Items where the code has moved on from older docs and the code is
authoritative:

- **`classify_use_case` is now an agent-visible tool** (added
  2026-05-02 per `tool-policy.yaml` and
  `ContextProjectionBuilder.initToolSchemas`). It is exposed only in
  the DISCOVER plan and unlocks DISCOVER → RESOLVE.
- **`record_outcome` canonical arg renamed `outcome_class`** with
  lowercase enum `resolve | escalate | abandon`. Legacy `outcome`
  arg and uppercase values (`RESOLVED / ESCALATED / ABANDONED`) are
  still accepted as back-compat aliases (Sprint 9 §O0,
  `RecordOutcomeTool`).
- **`resolve_article` canonical arg `source_id`** with `article_id`
  accepted as a legacy alias (Sprint 8.2 §M0a, `ResolveArticleTool`).
  Unpublished articles are refused with the deterministic reason
  `article_unpublished_safe_refuse` (Sprint 14 §L0).
- **`request_handover.summary` is optional**, derived deterministically
  by `RequestHandoverTool.deriveFallbackSummary` when absent
  (Sprint 9 §O0). The 23-value canonical `escalation_reason` enum is
  enforced both at the schema layer and via `ToolDispatcher` coercion.
- **`create_case_controlled` is `RUNTIME_ONLY`**, not agent-visible.
  The deterministic `PhaseEvaluator.createCaseIfAllowed` /
  `ControlKernel.createCaseIfNeeded` paths create UC-H/J/K cases
  before handover; the LLM has no schema for this tool.
- **Tool surface filtered per phase** — projected `tool_schemas` is
  exactly `PhasePlan.allowedTools`. Earlier docs implied the per-UC
  policy alone determined visibility.
- **Single-layer tool-use contract** — no separate routing decision
  layer; all control flow derives from `tool_calls` + `user_message`.
- **In-loop guards** owned by `AgentRunLoopImpl`: S1
  FAQ-grounded-resolve guard, intake-complete guard, progressive
  resolve `record_outcome` guard, DISCOVER `classify_use_case`
  terminal short-circuit.

## What is NOT current runtime behavior

The following are described in proposal / design docs but are not
delivered today. Treat anything in them as forward-looking:

- The **autoloop** described in `docs/proposals/autoloop_design.md`
  is a separate Python CLI design; no autoloop code is wired into
  the server runtime.
- A unified **HandoverOrchestrator** that owns handover side-effects
  and is idempotent by `session_id` is referenced as a future
  invariant (see `RequestHandoverTool` javadoc and
  `docs/proposals/handover_orchestrator_design.md`). Today, two paths can
  independently produce a local/mock handover write
  (`RequestHandoverTool` → `SalesforceService.requestHandover`, and
  `SessionManager.recordHandover`). `Sprint16HandoverDualPathReproTest`
  contains active characterization tests for the current dual-path
  behavior; only the future-invariant method
  `futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands`
  is `@Disabled` until the orchestrator lands.
- A **skill / plan-template framework** beyond `PhasePlan` — see
  `docs/proposals/skill_orchestration_candidates.md` — is not
  implemented.
  The current "skill" granularity is the `PhasePlan` itself.
- A **single-handover idempotency gate** keyed on `session_id` is
  not yet wired (see `docs/release_gate.md` for the cutover blocker).
- A dedicated **HUMAN_ONLY tool type** is not represented in
  `tool-policy.yaml`. The current type set is `AGENT_VISIBLE` and
  `RUNTIME_ONLY` only; sensitive write actions described in v0.2
  as `human_only` are simply not exposed as `Tool` beans.

## TODO_DECISION

- **Dormant runtime-only tool beans.** Four tools are registered as
  `RUNTIME_ONLY` in `tool-policy.yaml` and have `Tool` bean
  implementations, but no live deterministic call site in the
  current runtime has been identified for them:
  `lookup_customer_account`, `lookup_listing_or_ad`,
  `get_moderation_review_context`, `get_message_moderation_context`.
  The names also appear as labels in handover status checks. Decide
  whether to (a) wire deterministic callers, (b) retire the beans
  + policy entries, or (c) document a specific owner / planned
  caller for each. Until decided, treat them as registered
  runtime-only beans with no current dispatch path; the LLM has no
  schema for them and `ToolDispatcher` would only execute them if
  invoked by name from a non-existent caller.
- **Plan-vs-policy mismatch on projected `tool_schemas`.** The
  per-phase plan filter and the per-UC `tool-policy.yaml` matrix are
  independent; the projection currently surfaces tools that
  `ToolPolicyEnforcer` will deny on certain UCs (e.g.
  `get_customer_context` projected on the FAQ RESOLVE plan for UC-B
  and UC-E, then blocked at dispatch with `TOOL_SCOPE_BLOCKED`).
  Decide whether (a) the projection should additionally intersect
  with `tool-policy.yaml` `allowed-ucs`, (b) the policy matrix
  should be widened to match the plan exposure, or (c) the
  divergence is intentional and should be documented as expected
  behavior. Until decided, do not treat "tool schema projected" as
  equivalent to "tool dispatch allowed" — the two filters are
  independent and either can deny.
- **Moderation context: session-internal / handover-only (resolved
  M5-S3 via option (b)).** `session.moderationContext` is written by
  `FormContextIngestionService` (when the form-driven
  `get_customer_context` returns a moderation review) and read by
  `RequestHandoverTool` (when assembling handover identifiers), but it
  is **not** part of the LLM-visible projection:
  - `ContextProjectionBuilder` does **not** emit a `moderation_context`
    slot; only `customer_context` and `listing_context` are projected,
    via `session.getCustomerContext()` / `session.getListingContext()`.
  - The FAQ RESOLVE Skill (`resolve_faq_grounded_answer.yaml`) formerly
    declared `moderation_context` in `required_context_keys` — a
    dangling declaration with no emission consumer. M5-S3 (Sprint 52,
    C2 #3) took **option (b)**: dropped `moderation_context` from that
    declaration so `PhasePlan.requiredContextKeys` now reflects what is
    actually projected (`form_context`, `customer_context`,
    `listing_context`). The M5-S3 C1 consumption map
    (`docs/diagnostics/m5-s3-projection-consumption-map.md`) confirmed
    no projection / eval-trace-contract / drift-reconstruction consumer
    depended on the declaration.

  `moderation_context` is therefore **not present in the LLM-visible
  projection**; `session.moderationContext` remains session-internal /
  handover-payload-only by design.
