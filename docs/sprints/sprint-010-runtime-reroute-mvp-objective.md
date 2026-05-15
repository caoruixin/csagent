# Sprint Objective

Date: 2026-05-08

## Sprint name

Runtime Re-route MVP Sprint 10

## Goal

Implement the first runtime alignment workstream: pre-plan re-route and soft-shift handling before `PhaseEvaluator.plan(...)`.

Sprint 10 should restore the intended architecture where `ControlKernel` / runtime supervision decides whether the user message continues the current issue, shifts to a new UC, enters intake, or escalates before the planner exposes the next tool surface.

This sprint must stay narrowly focused on cross-UC re-route and CONFIRM/RESOLVE rebound. It must not implement full progressive resolve, full Issue Ledger, per-issue budgets, or a broad routing taxonomy rewrite.

## Baseline

Use the latest accepted Sprint 8 clean canonical / nondeterminism references documented in `docs/current_eval_baseline.md` and `docs/10-handoff.md`.

If a clean Sprint 10 smoke result is produced, promote it only if:
- credentials are clean
- no session-wide infra contamination
- `L1:escalation_reason_consistency = 0`
- `CONTRACT_VIOLATION:active_use_case = 0`
- targeted Sprint 10 reroute blockers are reduced or clearly classified

## Implement exactly these 3 actions

### L0. Internal `RuntimeIntentClassifier` + `RerouteDecision` model

Add an internal runtime classifier used before `PhaseEvaluator.plan(...)`.

Required behaviour:

- The classifier must be runtime-internal, not an agent-visible tool.
- It may reuse existing routing prompt / routing service / deterministic UC matchers, but must not call the agent-visible `classify_use_case` tool.
- It should classify the current user turn relative to the active session state.

Required output shape, or equivalent:

```java
record IntentClassification(
    String predictedUseCase,
    double confidence,
    IntentRelation relation,
    String taskType,
    String primaryEntityType,
    String primaryEntityValue
) {}

Suggested enums:

enum IntentRelation {
  SAME_ISSUE,
  SAME_UC_NEW_TASK,
  NEW_LOW_RISK_UC,
  NEW_HIGH_RISK_UC,
  HUMAN_REQUEST,
  CRITICAL_ESCALATION,
  UNKNOWN
}

enum RerouteAction {
  CONTINUE_CURRENT,
  SOFT_SHIFT_TO_DISCOVER,
  RISK_SHIFT_TO_INTAKE,
  ESCALATE_IMMEDIATELY
}

Required constraints:

Explicit human-help request still wins and produces user_requested.
Existing distress / critical escalation guards remain higher priority.
Do not rewrite escalation reason enum.
Do not broaden Tier-2 policy.
Do not implement a broad all-UC taxonomy.
For Sprint 10, support only the target MVP shapes:
UC-A -> UC-C soft shift: “I haven’t got replies”
UC-A same issue: “I still can’t see my ad”
UC-A same UC follow-up: “how long is it active for?”
UC-A -> UC-J risk shift: “I was scammed”
explicit human request
payment ambiguity negative guard: “I paid for Top Ad but it’s not showing” must not blindly route UC-I
L1. Cross-UC soft/risk shift before PhaseEvaluator.plan(...)

Insert the reroute decision before planner execution.

Required runtime order:

processUserMessage()
  -> hard guards
  -> explicit human / distress / critical-risk checks
  -> RuntimeIntentClassifier
  -> RerouteDecision
  -> mutate phase / UC / minimal issue state
  -> PhaseEvaluator.plan(...)
  -> AgentRunLoop

Required transitions:

CONFIRM + NEW_LOW_RISK_UC -> switch active UC and go to DISCOVER or direct RESOLVE when safe.
RESOLVE + NEW_LOW_RISK_UC -> switch active UC and go to DISCOVER or direct RESOLVE when safe.
CONFIRM + SAME_ISSUE -> return to RESOLVE for current UC.
CONFIRM + SAME_UC_NEW_TASK -> return to RESOLVE for current UC.
CONFIRM + HUMAN_REQUEST -> ESCALATE with user_requested.
NEW_HIGH_RISK_UC -> enter intake / handover path, not generic FAQ.
CRITICAL_ESCALATION -> immediate escalation if existing policy requires it.

Required examples:

Current state	User message	Expected
UC-A / CONFIRM	“I haven’t got replies”	UC-C soft shift; no generic handover
UC-A / CONFIRM	“I still can’t see my ad”	stay UC-A; return RESOLVE
UC-A / CONFIRM	“how long is it active for?”	stay UC-A; RESOLVE follow-up
UC-A / any	“I was scammed”	UC-J intake/handover path
UC-A / any	“I want a human”	user_requested escalation
UC-A / any	“I paid for Top Ad but it’s not showing”	not blindly UC-I
L2. Minimal issue-state projection + drift observability

Add minimal issue/task state needed for Sprint 10, without implementing a full Issue Ledger.

Required projected fields, where available:

{
  "previous_active_use_case": "UC-A",
  "drift_type": "SOFT_SHIFT",
  "current_task_type": "listing_visibility_diagnostic",
  "primary_entity": {
    "entity_type": "listing",
    "ad_id": "123456"
  },
  "issue_status_summary": "open"
}

Required constraints:

Keep existing candidate_use_cases projection.
Do not introduce issues[].
Do not introduce per-issue budgets.
Do not rewrite handover payload.
Do not alter Salesforce / external payload contracts.
Add lightweight trace / event / debug observability for:
predicted UC
relation
reroute action
previous UC
new UC
phase transition reason
Regression guards
L1:escalation_reason_consistency remains 0.
CONTRACT_VIOLATION:active_use_case remains 0.
cs014 remains UC-C.
cs066 remains UC-K.
cs095 remains UC-A / not UC-K / not UC-FP.
cs002 remains UC-C + user_distress.
cs029 remains UC-D + user_requested.
cs176 explicit-human-help -> user_requested focused regression remains green.
Sprint 6 G0 ReadTimeout closure remains intact:
120s create-session timeout widen only
no ReadTimeout retry
Sprint 6 G2 S1 FAQ-grounded-resolve remains intact.
Sprint 7 I2 intake-state persistence remains intact.
Sprint 8 cs259 active-use-case contract hardening remains intact.
Do not implement
Same-UC progressive resolve flow
ResolveDisposition
full Issue Ledger
issues[]
per-issue budgets
full handover multi-issue payload rewrite
full skill framework
all-UC task taxonomy
S3 no-prior-search guard
S5 Tier-2 runtime guard
judge calibration
CaseSpec churn
FAQ corpus changes
broad prompt rewrite
broad routing taxonomy rewrite
Eval Governance docs

## Success metrics

Primary:

Sprint 10 implements exactly L0 / L1 / L2.
UC-A -> UC-C soft shift works from CONFIRM / RESOLVE.
Same-issue dissatisfaction from CONFIRM returns to RESOLVE.
Same-UC follow-up from CONFIRM returns to RESOLVE.
Explicit human request still escalates with user_requested.
Risk shift such as “I was scammed” enters UC-J intake/handover path.
Payment ambiguity is not blindly routed to UC-I.
CONTRACT_VIOLATION:active_use_case = 0.
L1:escalation_reason_consistency = 0.

Secondary:

Smoke pass rate may improve or remain stable; targeted reroute blocker reduction matters more.
Remaining failures should be classified rather than fixed outside scope.
Review rule

Codex must review only whether Sprint 10 runtime re-route MVP was implemented and whether scope remained exactly L0 / L1 / L2.

Codex should not request Progressive Resolve, full Issue Ledger, all-UC task taxonomy, judge calibration, CaseSpec churn, FAQ corpus work, or Eval Governance unless Sprint 10 directly regresses a hard invariant.
