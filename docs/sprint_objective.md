# Sprint Objective

Date: 2026-05-08

## Sprint name

Runtime Alignment Hardening and Validation Sprint 12

## Goal

Harden and validate the combined Sprint 10 Runtime Re-route MVP and Sprint 11 Progressive Resolve MVP behaviours.

Sprint 12 should improve observability, targeted validation, and residual failure classification. It must not introduce a new broad runtime feature, full Issue Ledger, per-issue budgets, all-UC task taxonomy, judge calibration, FAQ corpus changes, or CaseSpec churn.

Sprint 10 closed with:
- runtime-internal classification
- pre-plan reroute before `PhaseEvaluator.plan(...)`
- soft/risk shift and CONFIRM rebound
- minimal reroute projection / trace evidence

Sprint 11 / 11.1 closed with:
- minimal same-UC task/entity state
- progressive resolve flow
- ResolveDisposition terminal-evidence guard
- non-slot UC-A same-UC answers without terminal evidence stay RESOLVE as ANSWERED_SUBTASK
- no unconditional `FINAL_ANSWER -> CONFIRM`
- no premature `record_outcome(resolve)` without terminal evidence

## Baseline

Use the latest accepted Sprint 11 / 11.1 baseline and handoff evidence documented in:

- `docs/10-handoff.md`
- `docs/current_eval_baseline.md`
- `docs/sprints/sprint-011-handoff.md`
- `docs/sprints/sprint-011-codex-review.md`

Do not promote a new canonical baseline unless Sprint 12 produces clean validation evidence and the handoff explicitly justifies replacing the current baseline.

## Implement exactly these 3 actions

### N0. Drift/task/phase observability hardening

Ensure runtime traces expose enough information to audit Sprint 10 / 11 decisions.

Required observability fields or equivalent:

- `drift_history`
- `task_history`
- `phase_transition_reason`
- `reroute_action`
- `intent_relation`
- `predicted_use_case`
- `previous_active_use_case`
- `active_use_case`
- `current_task_type`
- `task_status`
- `primary_entity`
- `resolve_disposition`
- `terminal_evidence`
- `record_outcome_guard_result`

Required behaviour:

- A reviewer should be able to inspect one trace and answer:
  - why did the bot stay in current UC?
  - why did the bot soft-shift?
  - why did the bot risk-shift?
  - why did the bot stay RESOLVE instead of CONFIRM?
  - why did the bot allow or reject `record_outcome(resolve)`?
- Preserve existing trace / event formats where possible.
- Add only backward-compatible fields.
- Do not rewrite handover payload.
- Do not add full `issues[]` ledger.
- Do not add per-issue budgets.

### N1. Targeted runtime alignment validation suite

Create or consolidate a targeted validation suite for Sprint 10 / 11 runtime behaviours.

Required coverage:

1. UC-A → UC-C soft shift:
   - current UC-A
   - user says “I haven’t got replies”
   - expected UC-C / DISCOVER or RESOLVE
   - no generic handover

2. UC-A same-issue dissatisfaction:
   - current UC-A / CONFIRM
   - user says “I still can’t see my ad”
   - expected UC-A / RESOLVE

3. UC-A same-UC follow-up:
   - current UC-A / CONFIRM
   - user asks “how long is it active for?”
   - expected UC-A / RESOLVE

4. UC-A progressive listing diagnostic:
   - how to find ad
   - why can’t I find my ad
   - user provides ad_id
   - expiry/status follow-up
   - human request
   - expected entity reuse and final `user_requested`

5. UC-C messaging follow-up:
   - same UC follow-up remains UC-C / RESOLVE

6. Risk shift to UC-J:
   - user says “I was scammed”
   - expected UC-J intake/handover path, not FAQ

7. Explicit human request:
   - expected `user_requested`

8. Payment ambiguity negative guard:
   - “I paid for Top Ad but it’s not showing”
   - must not blindly route UC-I

9. `record_outcome(resolve)` guard:
   - no premature resolve outcome before confirmation / terminal evidence

10. ResolveDisposition terminal evidence:
   - non-slot UC-A same-UC answer without terminal evidence stays RESOLVE
   - valid terminal evidence may lead to READY_TO_CONFIRM

Required constraints:

- Prefer deterministic Java regression / simulator fixtures over live LLM dependence.
- Do not expand smoke / anchor / promotion hard gates.
- Do not change CaseSpecs unless a direct P0 evidence issue is discovered.
- Keep tests targeted to Sprint 10 / 11 behaviours.

### N2. Residual runtime blocker classification and next-phase decision

Update handoff / action bank with residual classification after Sprint 12 validation.

Required classification categories:

- `runtime_bug`
- `label_disagreement`
- `faq_corpus_gap`
- `product_policy_gap`
- `judge_volatility`
- `persona_drift`
- `infra`
- `deferred_scope`

Required output in `docs/10-handoff.md`:

- Targeted validation results.
- Any smoke result paths if smoke is run.
- Whether hard invariants remain green:
  - `L1:escalation_reason_consistency = 0`
  - `CONTRACT_VIOLATION:active_use_case = 0`
- Residual P0/P1 blockers, if any.
- Recommendation for next phase:
  - Eval Governance
  - Release Candidate Hardening
  - Narrow Runtime Follow-up
  - Re-run validation after infra cleanup

Required constraints:

- Do not fix residuals outside Sprint 12 scope.
- Do not start Eval Governance docs in Sprint 12.
- Do not start release hardening in Sprint 12.
- Do not promote a baseline unless results are clean and explicitly accepted.

## Regression guards

- Sprint 10 reroute tests remain green.
- Sprint 11 progressive resolve tests remain green.
- Sprint 11.1 terminal-evidence closure remains green.
- RuntimeIntentClassifier remains runtime-internal.
- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` remains 0.
- cs014 remains UC-C.
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 remains UC-C + `user_distress`.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help -> `user_requested` focused regression remains green.
- Sprint 6 G0 ReadTimeout closure remains intact:
  - 120s create-session timeout widen only
  - no ReadTimeout retry
- Sprint 6 G2 S1 FAQ-grounded-resolve remains intact.
- Sprint 7 I2 intake-state persistence remains intact.
- Sprint 8 cs259 active-use-case contract hardening remains intact.

## Do not implement

- New broad runtime feature
- full Issue Ledger
- `issues[]`
- per-issue budgets
- all-UC task taxonomy
- full skill runtime framework
- handover payload rewrite
- Salesforce / external payload contract changes
- FAQ corpus changes
- product policy changes
- judge calibration
- CaseSpec churn
- CaseSpec override changes
- anchor / exploration / promotion hard-gate expansion
- Eval Governance docs
- Release Candidate docs
- broad prompt rewrite
- broad Java guard
- broad routing taxonomy rewrite

## Success metrics

Primary:

- Sprint 12 implements exactly N0 / N1 / N2.
- Sprint 10 and Sprint 11 behaviours are observable in trace / debug evidence.
- Targeted validation suite covers reroute + progressive resolve + record_outcome guard.
- Residual failures are classified with owners.
- Hard invariants remain green:
  - `L1:escalation_reason_consistency = 0`
  - `CONTRACT_VIOLATION:active_use_case = 0`

Secondary:

- Smoke pass rate may improve or remain stable, but targeted runtime-alignment evidence matters more.
- The next-phase recommendation is explicit and justified.

## Review rule

Codex must review only whether Sprint 12 observability, validation, and residual classification are complete and whether scope remained exactly N0 / N1 / N2.

Codex should not request full Issue Ledger, per-issue budgets, all-UC taxonomy, FAQ corpus changes, judge calibration, CaseSpec churn, Eval Governance, Release Candidate work, or broad runtime rewrite unless Sprint 12 directly regresses a hard invariant.