# Release Gate

Date: 2026-05-09
Mode: launch / cutover blocking-rule ledger
Status: docs only; the runtime is currently frozen by Sprint 13

## 0. Purpose

This document records the **blocking rules** that must be true
before a customer-impacting cutover (production Salesforce wire-up,
live customer routing, public release) is allowed. A blocking rule
is binary: every rule must be satisfied or the cutover is held.

This is distinct from the eval gate (anchor / exploration /
promotion thresholds, judge calibration) tracked in
`docs/foundational/phase5_evaluation_design.md` and `docs/current_eval_baseline.md`.
This file is operational launch governance, not eval governance.

## 1. Blocking rules

### 1.1 Handover side-effect must be idempotent by `session_id`

**Rule.** Before a real Salesforce client (production
`SalesforceService` implementation) is wired and customers are
routed through it, the handover side-effect must be **exactly-once
per `session_id`**.

For each `session_id`, **at most one** handover decision with
`transfer_result ∈ { "transferred", "offline_logged" }` may exist
across the full session lifetime. A second handover invocation for
the same `session_id` MUST be a no-op that returns the original
result without retransmitting to Salesforce, without re-persisting
the payload, and without re-emitting `ESCALATION_REQUESTED`.

**Why.** Today the LLM-driven `request_handover` path produces two
independent local handover writers (`RequestHandoverTool` →
`SalesforceService.requestHandover(...)` and
`SessionManager.recordHandover(...)`). In `mock_handover_log` this
is observable as two rows per session. With a real Salesforce
client wired naively into the same dual-path shape, this becomes a
real double transfer. A real double transfer can re-route a single
customer's case twice into Salesforce — re-assignment,
mis-prioritisation, queue duplication — and is user-visible.

**Severity escalation path.**

| Phase | Severity |
|------|----------|
| Today (local / mock) | P2 — duplicated local rows; no live customer impact |
| Pre-cutover | P1 — wire-up away from a real double transfer |
| Production with real double transfer | P0 / P1 |

**Acceptance criteria** (must all be true to lift the block):

1. A `HandoverOrchestrator` exists and is the single owner of the
   four handover side-effects:
   - Salesforce transfer (`SalesforceService.requestHandover`),
   - handover payload persistence,
   - handover decision persistence,
   - `ESCALATION_REQUESTED` event emission.
2. `RequestHandoverTool` no longer calls `SalesforceService`
   directly; it delegates to `HandoverOrchestrator`.
3. `SessionManager.recordHandover` is removed or rewritten to
   delegate to the orchestrator.
4. A deterministic test asserts: for any `session_id`, at most one
   handover row with `transfer_result ∈
   { "transferred", "offline_logged" }` exists across the
   LLM-driven path, the kernel force-escalate path, and the
   hard-OOS path.
5. A deterministic test asserts: a second
   `HandoverOrchestrator.handover(...)` call for the same
   `session_id` returns `deduped=true`, does not call
   `SalesforceService.requestHandover` again, and does not persist
   a second row.
6. The reconcile-on-already-escalated path
   (`SessionManager.reconcileEscalationReasonOnAlreadyEscalatedSession`)
   continues to rewrite the existing handover row in place (preserving
   `log_id`).

**Cross-references.**

- Contract design: `docs/proposals/handover_orchestrator_design.md`.
- Risk record: `docs/runtime_freeze_and_risk_policy.md` §10.1.
- Action item: `docs/action_bank.md` (Single Handover
  Orchestrator).
- Repro / characterization: `Sprint16HandoverDualPathRepro*Test`.

**Status (2026-05-09).** **Not satisfied.** Sprint 16 documents the
contract and adds characterization tests; the runtime fix is
deferred to a future "Single Handover Orchestrator" runtime sprint.
This rule blocks any real Salesforce cutover.

## 2. Soft / pre-existing gates (recorded for cross-reference)

These are tracked elsewhere; this section is an index, not a
re-statement.

- `L1:escalation_reason_consistency = 0` (eval-side hard
  invariant; named regression suite).
- `CONTRACT_VIOLATION:active_use_case = 0` (eval-side hard
  invariant; named regression suite).
- Sprint 6 §G2 FAQ-grounded resolve guard (named regression
  test).
- Sprint 14 §L0 published-safety filter; `ResolveArticleTool`
  unpublished-refusal contract (named regression suite).
- Sprint 14.1 `bot_turns.projected_context.faq_grounding`
  persistence (named regression test).

These gates already pass and are not re-asserted here. A new
blocking rule is added to §1 only when a P1+ launch-readiness
issue is identified.

## 3. Maintenance rule

`docs/release_gate.md` is a current-state ledger.

- Add a rule when a launch-readiness issue is identified at P1 or
  worse. Each rule must specify (a) what is blocked, (b) why,
  (c) acceptance criteria, (d) cross-references.
- Move a rule to "satisfied" when its acceptance criteria are met,
  noting the closing sprint and the test(s) that pin the
  invariant. Do not delete satisfied rules within the same sprint
  cycle; archive them on the next ad-hoc transition under
  `docs/archive/current-docs/`.
- Do not list eval thresholds here. Those belong in
  `docs/foundational/phase5_evaluation_design.md` and
  `docs/current_eval_baseline.md`.

Sprint 16 establishes this file with one blocking rule (§1.1).
