# Handover Orchestrator Design — Exactly-Once Contract

Date: 2026-05-09
Sprint: Sprint 16 — Handover Exactly-Once Contract and Repro
Status: **design freeze (docs only); no runtime change in Sprint 16**

## 0. Purpose

This document freezes the contract for handover side-effects before
any runtime refactor. The handover surface today has two
independently-wired writers in the LLM-driven path; this document
names the issue, separates three distinct contracts that have been
historically conflated, and pre-specifies the future
`HandoverOrchestrator` invariant so a later runtime sprint can land
without re-arguing the design.

Sprint 16 does **not** add `HandoverOrchestrator`. It does:

1. document the issue and the future invariant (this file),
2. add a known-unspecced-surface entry in
   `docs/runtime_freeze_and_risk_policy.md`,
3. add a release-gate blocker in `docs/release_gate.md`,
4. add an action-bank item, and
5. add characterization tests demonstrating the dual local
   persistence shape today.

## 1. Current dual-path shape (LLM-driven `request_handover`)

The LLM-driven path produces **two independent local handover
side-effects** for the same `session_id`:

| Step | Caller | Writer | Sink (local profile) | Schema |
|------|--------|--------|----------------------|--------|
| 1    | `AgentRunLoopImpl` dispatches `request_handover` | `RequestHandoverTool.execute` → `SalesforceService.requestHandover(sessionId, payload)` | `MockSalesforceService` writes `mock_handover_log` row #1 | Phase 3 §3.6.2 v1.0, built inline in the tool |
| 2    | `SessionManager.processMessage` observes phase=`ESCALATE` after the loop returns | `SessionManager.recordHandover(session)` writes `MockHandoverLog` row #2 directly via `handoverLogRepository.save(...)` | `mock_handover_log` row #2 | v1.1, built by `HandoverPayloadAssembler` |

Both rows reference the same `session_id`. Neither writer is aware of
the other. The two payloads can disagree on:

- `version` (`"1.0"` vs `"1.1"`),
- `summary` shape (LLM-supplied / RequestHandoverTool fallback vs
  `HandoverPayloadAssembler.buildSummary`),
- `unresolved_question`, `partial_answer_or_blocker`,
  `status_checks_performed`, `knowledge_tools_invoked`
  (only the assembler emits these),
- `transfer_result` (set by `MockSalesforceService` to
  `transferred` / `offline_logged` for row #1; set to
  `mock_transfer` for row #2).

Other handover paths are **not dual** today:

- **Hard-OOS in `SessionManager.createSession`.** Synthesises a
  `request_handover` entry into `bot_turns.tool_calls` and then
  calls `recordHandover(session)` once. No `RequestHandoverTool`
  execution. **Single local write.**
- **Step 2.5 forceEscalate in `ControlKernel`.** Synthesises a
  `request_handover` entry into `bot_turns.tool_calls` and the
  loop ends with `phase=ESCALATE`. `SessionManager.processMessage`
  calls `recordHandover(session)` once. No `RequestHandoverTool`
  execution. **Single local write.**

## 2. What is confirmed vs not confirmed

**Confirmed today (local / mock):**

- The LLM-driven path produces two rows in `mock_handover_log` for a
  single `session_id` in a single ESCALATE turn.
- The two rows are written by different code paths with different
  payload builders. They disagree in shape and version.

**Not confirmed today:**

- A real **double Salesforce transfer**. The production Salesforce
  client is not wired. Today only `MockSalesforceService` implements
  `SalesforceService`, and `SessionManager.recordHandover` does not
  go through `SalesforceService` at all — it writes directly to the
  same `mock_handover_log` repository.
- Whether `HandoverPayloadAssembler`'s payload would be the canonical
  one used by Salesforce in production; today it is unused outside
  the mock flow and the reconcile-on-already-escalated path.

## 3. Three separate contracts (the freeze)

The historical confusion comes from three concepts being conflated.
Sprint 16 separates them.

### 3.1 Contract A — trace evidence: `request_handover`

**What.** The semantic handover decision and trace evidence on the
turn the bot decided to hand over.

**Where.** Persisted as one entry in `bot_turns.tool_calls` JSON for
the ESCALATE turn:

```
{
  "tool_name": "request_handover",
  "arguments": { "escalation_reason": "<canonical>", ... },
  "result": { ... }
}
```

**Producer.** Either:

1. the LLM action (parsed by `ActionParser`, dispatched by
   `ToolDispatcher` → `RequestHandoverTool`), or
2. the kernel (`ControlKernel.synthesizeHandoverToolCall(...)` for
   force-escalate / hard-OOS / Step 2.5).

**Invariant.**

- **Synthetic** evidence (kernel-built) MUST exist on the persisted
  ESCALATE turn so eval can verify the L1 sequence contract.
- **LLM** evidence MAY exist when the LLM drove the handover.
- Synthetic evidence MUST NOT itself trigger a Salesforce transfer
  or a duplicate handover side-effect. It is a trace-only
  representation of "the system handed over here, for this reason".
- The persisted `arguments.escalation_reason` MUST equal
  `session.escalationReason` after canonicalisation
  (`L1:escalation_reason_consistency = 0`; pinned by
  `Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs176ExplicitHumanHelpHandoverIntegrationTest`,
  `AgentRunLoopHandoverReasonNormalizationIntegrationTest`).

**Out of scope for this contract.** Sending anything to Salesforce.
Writing to `mock_handover_log`. Computing the durable handover
payload.

### 3.2 Contract B — outcome persistence: `record_outcome`

**What.** The terminal session outcome (resolved / escalated /
abandoned), the analytics row recording how the session ended.

**Where.** `session_outcomes` table (`SessionOutcome`), written by
`SessionManager.recordOutcome(session)`.

**Producer.** `SessionManager.processMessage` when the session
reaches a terminal phase (`ESCALATE` or `CLOSE`).

**Invariant.**

- Exactly one `session_outcomes` row per `session_id`.
- Independent of Contract A and Contract C — `record_outcome` is
  analytics, not a handover side-effect.
- An `outcome=escalated` row does NOT itself trigger a Salesforce
  transfer. It records that escalation happened.
- The `record_outcome` LLM tool (the agent-visible
  `outcome_class={resolve|escalate|abandon}` tool) is the
  agent-facing trace evidence that the bot believes the session is
  ending; the runtime `recordOutcome(...)` is the persistence layer.
  The tool's `outcome_class` and the persisted
  `session_outcomes.outcome` MUST stay aligned (Sprint 9 §O0
  contract; preserved).

**Out of scope for this contract.** Sending anything to Salesforce.
Writing handover payloads.

### 3.3 Contract C — handover side-effect: `HandoverOrchestrator`

**What.** The single, exactly-once external + durable side-effect
that constitutes "this conversation is now handed over to a human".

This is the contract the future `HandoverOrchestrator` owns.

**Side-effects covered (a single owner for all four):**

1. **Salesforce transfer** — the actual `requestHandover` call
   against the production Salesforce client (a real transfer).
2. **Handover payload persistence** — the durable record of the
   payload that was transmitted (today: `mock_handover_log.handover_payload`;
   in production: whatever durable store is wired alongside or in
   place of Salesforce).
3. **Handover decision persistence** — the durable record that the
   handover happened, including the canonical `escalation_reason`,
   the `transfer_result` (`transferred` / `offline_logged`), the
   `customer_message`, the `transcript` reference.
4. **`ESCALATION_REQUESTED` event emission** — exactly one
   `bot_events` row of type `ESCALATION_REQUESTED` per ESCALATE
   transition.

**Invariant.**

- For each `session_id`, **at most one** handover decision with
  `transfer_result ∈ { "transferred", "offline_logged" }` may
  exist across the full session lifetime.
- The handover side-effect is **idempotent by `session_id`**:
  re-invoking the orchestrator for the same `session_id` MUST be
  a no-op that returns the original `transfer_result` without
  retransmitting to Salesforce, without re-persisting the payload,
  and without re-emitting `ESCALATION_REQUESTED`.
- The orchestrator is the **only** writer to the handover
  side-effect surface. `RequestHandoverTool` and
  `SessionManager.recordHandover` MUST NOT both reach Salesforce or
  the durable handover store directly. (Today they both write
  locally; the future orchestrator removes that split.)
- The handover payload schema (Phase 3 §3.6.2) is owned by the
  orchestrator's payload builder. `HandoverPayloadAssembler` is the
  candidate today; the orchestrator is the future single caller.
- The reconcile-on-already-escalated path
  (`SessionManager.reconcileEscalationReasonOnAlreadyEscalatedSession`)
  **rewrites the existing handover row in place** (preserving
  `log_id`); this path is compatible with idempotency by `session_id`
  and MUST be preserved by the orchestrator.

**Out of scope for this contract.** Producing the agent-visible
trace evidence (Contract A) or the analytics outcome row (Contract
B).

## 4. Future `HandoverOrchestrator` shape (target, not Sprint 16 scope)

A later runtime sprint owns the implementation. The shape this
document freezes:

```
HandoverOrchestrator.handover(session, escalationReason, source)
  → HandoverResult { transferResult, logId, deduped: boolean }
```

- **Single entry point.** `RequestHandoverTool` no longer calls
  `SalesforceService` directly; it calls
  `HandoverOrchestrator.handover(...)`. `SessionManager` no longer
  calls `recordHandover(...)` directly; it also goes through the
  orchestrator.
- **Idempotency.** Internally keyed by `session_id`. A second call
  for the same `session_id` returns the prior `HandoverResult` with
  `deduped=true`; no second Salesforce call, no second payload
  persist, no second event.
- **Payload ownership.** The orchestrator is the sole caller of the
  payload builder (`HandoverPayloadAssembler` or its successor).
  `RequestHandoverTool` no longer builds an inline v1.0 payload.
- **`source` argument.** Distinguishes
  `LLM_REQUEST_HANDOVER`, `KERNEL_FORCE_ESCALATE`,
  `KERNEL_HARD_OOS`, `RECONCILE_ALREADY_ESCALATED` purely for
  observability; idempotency does not depend on source.
- **`ESCALATION_REQUESTED` emission.** Exactly once per
  `session_id`, at the orchestrator boundary.
- **Reason canonicalisation.** Continues to flow through
  `EscalationReasonResolver` before the orchestrator persists.
- **Reconcile path.** Treated as an in-place update of the existing
  handover row, not a second handover. Today's
  `MockHandoverLog`-by-`log_id` rewrite shape is preserved.

The orchestrator is the natural place to wire the production
Salesforce client. The exactly-once contract MUST land BEFORE that
wire-up to avoid a real double-transfer at cutover.

## 5. Why Sprint 16 freezes the contract before refactoring

Sprint 16 is intentionally docs + characterization only.

1. The dual-path issue is currently **P2** — duplicated local
   persistence in `mock_handover_log`, no proven real Salesforce
   double transfer.
2. The launch-readiness severity is **P1** before real Salesforce
   cutover. Wiring a real client into the current dual-path shape
   would risk an immediate production double transfer.
3. The production-incident severity of an actual double transfer is
   **P0/P1**: a customer's case re-routed twice into Salesforce can
   be re-assigned, mis-prioritised, or duplicated in queue, and is
   directly user-visible.
4. The runtime is currently **frozen** by Sprint 13. A handover
   refactor cuts across `RequestHandoverTool`, `SessionManager`,
   `HandoverPayloadAssembler`, `MockSalesforceService`,
   `EventEmitter`, and the reconcile path. It is a runtime sprint,
   not a docs-governance sprint.
5. Freezing the contract first makes it possible to (a) repro the
   issue in characterization tests today and (b) accept the future
   orchestrator with pre-defined acceptance criteria rather than
   rediscovering them mid-sprint.

## 6. Acceptance criteria for the future runtime sprint

A future "Single Handover Orchestrator" runtime sprint closes when:

- A `HandoverOrchestrator` exists and is the **only** caller of
  `SalesforceService.requestHandover(...)` and the **only** writer
  to the durable handover-decision store.
- `RequestHandoverTool` no longer reaches `SalesforceService`
  directly; it delegates to the orchestrator.
- `SessionManager.recordHandover` is removed or rewritten to
  delegate to the orchestrator.
- A deterministic test asserts: for any `session_id`, at most one
  `mock_handover_log` row with `transfer_result ∈
  { "transferred", "offline_logged" }` exists across the full
  LLM-driven path, the kernel force-escalate path, and the hard-OOS
  path.
- A deterministic test asserts: a second
  `HandoverOrchestrator.handover(...)` call for the same
  `session_id` returns `deduped=true`, does not call
  `SalesforceService.requestHandover` again, and does not persist a
  second row.
- The reconcile-on-already-escalated path still rewrites the
  existing row in place.
- `L1:escalation_reason_consistency = 0` and the cs014 / cs176 /
  cs002 / cs029 / cs176 / cs066 / cs095 regression suite remain
  green.

The Sprint 16 characterization tests are the failing-by-design
trigger that flips to passing the moment the orchestrator lands.

## 7. References

- `docs/sprint_objective.md` — Sprint 16 objective.
- `docs/runtime_freeze_and_risk_policy.md` — known-unspecced-surface
  entry for the handover dual-path.
- `docs/release_gate.md` — release-gate blocker before real
  Salesforce cutover.
- `docs/action_bank.md` — Single Handover Orchestrator action item.
- `docs/phase3_detailed_technical_design.md` — handover payload
  schema (§3.6.2).
- `docs/customer_service_tool_spec_v0_2.yaml` — `request_handover`
  tool schema; canonical `escalation_reason` enum.
- `docs/salesforce-part-spec.md` — current Salesforce part spec
  (case creation, queues, off-hours behaviour).
- `server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java`
  — current LLM-driven writer (Contract A producer + side-effect).
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  — current second writer (`recordHandover` + reconcile path).
- `server/src/main/java/com/gumtree/csagent/service/runtime/HandoverPayloadAssembler.java`
  — current payload builder (Contract C candidate).
- `server/src/main/java/com/gumtree/csagent/service/mock/MockSalesforceService.java`
  — current local-profile sink.
- `server/src/main/java/com/gumtree/csagent/service/observability/EventEmitter.java`
  — current `ESCALATION_REQUESTED` emitter.
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint16HandoverDualPathRepro*Test.java`
  (new in Sprint 16) — characterization tests demonstrating the
  dual local persistence shape today.
