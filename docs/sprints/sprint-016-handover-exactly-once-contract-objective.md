# Sprint Objective

Date: 2026-05-09

## Sprint name

Handover Exactly-Once Contract and Repro Sprint 16

## Goal

Freeze the handover side-effect contract before implementing a runtime refactor.

This sprint documents the current dual-path handover risk, defines the future exactly-once contract, and adds characterization tests that expose the current duplicated local handover log / payload shape.

This is a docs + characterization-test sprint. It must not refactor runtime behaviour.

## Background

Current reports confirm:

- `request_handover` in the LLM-driven AgentRunLoop path directly reaches `RequestHandoverTool`.
- `RequestHandoverTool` currently performs a handover side-effect through `SalesforceService.requestHandover(...)`.
- In the local/mock implementation, this writes a `mock_handover_log` row.
- After the same turn ends, `SessionManager` lifecycle finalization can also call `recordHandover(session)`, which writes another local handover log / payload.
- Current code does not prove a real double Salesforce transfer because the production Salesforce client is not wired yet.
- The confirmed current bug shape is duplicated mock handover log / payload and split side-effect ownership.
- If a real Salesforce transfer client is wired into both paths later, this becomes a launch-blocking double-transfer risk.

## Implement exactly these 3 actions

### H0. Define handover exactly-once contract

Create or update docs to define the contract:

- `request_handover` is the semantic handover command / trace evidence.
- `record_outcome` is outcome analytics persistence.
- Handover side-effects are:
  - Salesforce transfer
  - handover payload persistence
  - handover log / decision persistence
  - escalation requested event emission
- Handover side-effects must have exactly one owner.
- Future owner should be `HandoverOrchestrator`.
- For each `session_id`, at most one transmitted / offline-logged handover decision may exist.
- Synthetic request_handover evidence may be created for force escalation / hard-OOS / legacy paths, but synthetic evidence must not directly duplicate external side-effects.

Recommended doc targets:

- `docs/runtime_freeze_and_risk_policy.md`
- `docs/release_gate.md`
- `docs/action_bank.md`
- optionally `docs/handover_orchestrator_design.md`

### H1. Add characterization tests for current dual-path behaviour

Add focused tests that characterize the current issue without changing runtime.

Required tests:

- LLM-driven `request_handover` path currently produces two local handover persistence surfaces, or at minimum demonstrates two independent handover side-effect paths.
- Force escalation / hard-OOS / legacy path should not be incorrectly described as double-Salesforce today.
- The test must clearly distinguish:
  - duplicated local log / payload
  - not-yet-proven real Salesforce double transfer
- If the test would currently fail by design, mark it disabled / TODO with a clear reason and reference to the future orchestrator sprint.

Do not make the build red unless this sprint explicitly decides to treat the current duplicate local log as an immediate blocker.

### H2. Add release-gate / action-bank tracking

Update `docs/action_bank.md` and release-gate docs so this is tracked as:

- Current severity: P2 in local/mock environment.
- Launch severity: P1 before real Salesforce cutover.
- Production incident severity: P0/P1 if real double transfer occurs.
- Required before production Salesforce cutover:
  - Single Handover Orchestrator
  - idempotency by `session_id`
  - exactly-one handover decision test
  - no duplicate external transfer attempt

## Do not implement

- HandoverOrchestrator runtime
- Salesforce production client
- payload schema redesign
- HandoverContext redesign
- soft handover
- future LLM evidence enrichment
- new escalation reason enum values
- prompt changes
- routing changes
- FAQ corpus changes
- judge changes
- CaseSpec changes

## Success metrics

- The handover exactly-once contract is documented.
- Current dual-path risk is documented accurately:
  - duplicated local log / payload today
  - real double transfer is a future cutover risk, not currently proven
- Characterization tests exist or are explicitly disabled with TODO references.
- `docs/action_bank.md` contains a clear future runtime sprint item.
- No runtime behaviour changes are introduced.

## Review rule

Codex must review only whether the handover contract and repro characterization are accurate and scoped.

Codex should not request the full orchestrator implementation in this sprint unless the sprint objective is changed.