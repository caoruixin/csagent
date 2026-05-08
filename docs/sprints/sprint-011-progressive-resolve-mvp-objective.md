# Sprint Objective

Date: 2026-05-08

## Sprint name

Progressive Resolve MVP Sprint 11

## Goal

Implement same-UC progressive resolve for multi-turn, multi-subtask customer-service flows.

Sprint 11 builds on Sprint 10 Runtime Re-route MVP. Sprint 10 decides whether a user turn stays in the current issue, soft-shifts to a new UC, risk-shifts to intake, or escalates before `PhaseEvaluator.plan(...)`.

Sprint 11 focuses only on the same-UC continuation path: when the user remains inside the same UC but asks a new related subtask, provides a slot, asks for a listing-specific follow-up, or asks for human help after multiple resolve steps.

Sprint 11 must not introduce a full Issue Ledger, per-issue budgets, an all-UC task taxonomy, a full skill runtime framework, or a handover payload rewrite.

## Baseline

Use the latest accepted Sprint 10 baseline and handoff evidence documented in:

- `docs/10-handoff.md`
- `docs/current_eval_baseline.md`
- `docs/sprints/sprint-010-handoff.md`
- `docs/sprints/sprint-010-codex-review.md`

Sprint 10 closed with:

- Codex decision: pass
- blocking_count: 0
- L0 runtime-internal classification implemented
- L1 pre-plan reroute before `PhaseEvaluator.plan(...)` implemented
- L2 minimal projection / trace evidence implemented
- `mvn -pl server test` green
- `pytest eval_interactive/tests/` green
- `L1:escalation_reason_consistency = 0`
- `CONTRACT_VIOLATION:active_use_case = 0`

## Implement exactly these 3 actions

### M0. Minimal same-UC task/entity state

Add or complete minimal task state for progressive same-UC resolve.

Required state fields, where applicable:

- `current_task_type`
- `task_status`
- `primary_entity`
- `last_entity_context_ref` if already available
- `issue_status_summary`

Sprint 11 target task types:

- UC-A `how_to_find_listing`
- UC-A `listing_visibility_diagnostic`
- UC-A `listing_expiry_or_status_followup`
- UC-C `messaging_replies_followup`

Required behaviour:

- Same-UC follow-up should preserve active UC.
- Listing entity values such as `ad_id` should be reused across same-UC follow-up turns when already known.
- A user-provided entity such as an advert ID should update `primary_entity`.
- Current task should advance without opening a new issue model.
- Projection should expose enough state for the LLM to avoid re-asking already-known information.

Required constraints:

- Do not create full `issues[]` ledger.
- Do not introduce per-issue budgets.
- Do not build an all-UC task taxonomy.
- Do not rewrite handover payload.
- Reuse Sprint 10 minimal state and projection patterns where possible.

### M1. `ResolveDisposition` and transition guard

Introduce a runtime-visible disposition or equivalent checkpoint so that factual answers, slot requests, and progressive subtasks do not all collapse into `CONFIRM`.

Suggested shape:

```java
enum ResolveDisposition {
  CONTINUE_RESOLVE,
  ASKED_FOR_SLOT,
  ANSWERED_SUBTASK,
  READY_TO_CONFIRM,
  ESCALATE
}

Equivalent implementation is acceptable if the same behaviour is pinned by tests.

Required behaviour:

RESOLVE + ASKED_FOR_SLOT -> stay RESOLVE.
RESOLVE + ANSWERED_SUBTASK -> usually stay RESOLVE.
RESOLVE + READY_TO_CONFIRM -> transition to CONFIRM.
RESOLVE + ESCALATE -> transition to ESCALATE.
CONFIRM + same-UC follow-up -> return to RESOLVE.
CONFIRM + human request remains ESCALATE with user_requested.
CONFIRM + new-UC question remains handled by Sprint 10 reroute.

Required guard:

No unconditional FINAL_ANSWER -> CONFIRM.
No unconditional record_outcome(resolve) immediately after a single factual answer.
record_outcome(resolve) should happen only when:
the user confirms resolution,
the close phase is reached,
or a deterministic terminal condition is satisfied.
A soft next-step answer such as “Here is how to find your ad; send the advert ID if you want me to check it” should remain in RESOLVE, not hard-confirm the whole issue.

### M2. Progressive UC-A / UC-C regression suite

Add focused regression coverage for same-UC progressive resolve.

Required UC-A progressive flow test:

User asks: “How do I find my ad?”
Bot gives grounded answer or soft next step.
Runtime remains in RESOLVE or equivalent non-terminal state.
User asks: “Why can’t I find my ad?”
Bot asks for ad_id or uses available listing entity.
User provides ad_id.
Runtime stores / reuses primary_entity.
User asks: “How long is it active for?”
Bot stays UC-A and reuses listing entity where possible.
User asks for human help.
Bot escalates with escalation_reason=user_requested.

Required UC-C follow-up test:

User is in UC-C replies / messaging context.
User asks a same-UC follow-up.
Runtime stays UC-C and returns / remains in RESOLVE.
No unnecessary soft shift or generic handover.

Required record-outcome guard test:

A single factual answer does not immediately call record_outcome(resolve).
record_outcome(resolve) appears only after user confirmation, close phase, or deterministic terminal condition.

Required projection snapshot test:

Projected context includes current task / entity state needed for progressive resolve:
current_task_type
task_status
primary_entity
issue_status_summary
existing Sprint 10 fields remain intact.

## Regression guards
Sprint 10 reroute tests remain green.
RuntimeIntentClassifier remains runtime-internal.
Sprint 10 soft/risk-shift guards remain green:
UC-A -> UC-C soft shift
UC-A same issue dissatisfaction
UC-A same-UC follow-up from CONFIRM
UC-A -> UC-J risk shift
payment ambiguity does not blindly UC-I
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

## Do not implement
Cross-UC router expansion beyond Sprint 10
full Issue Ledger
issues[]
per-issue budgets
all-UC task taxonomy
full skill runtime framework
full handover payload rewrite
Salesforce / external payload contract changes
FAQ corpus changes
product policy changes
judge calibration
CaseSpec churn
CaseSpec override changes
anchor / exploration / promotion hard-gate expansion
Eval Governance docs
broad prompt rewrite
broad Java guard
broad routing taxonomy rewrite

## Success metrics

Primary:

Sprint 11 implements exactly M0 / M1 / M2.
Progressive UC-A flow stays in RESOLVE across related same-UC subtasks.
Known entity such as ad_id can be stored and reused across follow-up turns.
A single factual answer does not immediately hard-confirm or record_outcome(resolve).
Explicit human request still escalates with user_requested.
UC-C same-UC follow-up remains UC-C.
L1:escalation_reason_consistency = 0.
CONTRACT_VIOLATION:active_use_case = 0.

Secondary:

Smoke pass rate may improve or remain stable, but targeted progressive-resolve blocker reduction matters more.
Remaining failures should be classified rather than fixed outside Sprint 11 scope.

## Review rule

Codex must review only whether Sprint 11 same-UC progressive resolve was implemented and whether scope remained exactly M0 / M1 / M2.

Codex should not request full Issue Ledger, per-issue budgets, all-UC task taxonomy, judge calibration, CaseSpec churn, FAQ corpus work, Eval Governance, or broad runtime rewrite unless Sprint 11 directly regresses a hard invariant.