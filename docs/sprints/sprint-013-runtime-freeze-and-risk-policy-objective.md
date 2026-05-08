# Sprint Objective

Date: 2026-05-08

## Sprint name

Runtime Freeze, Risk Policy, and Eval Guardrails Sprint 13

## Goal

Freeze the runtime main flow after Sprint 10 / 11 / 11.1 / 12 and shift the next iteration layer to risk policy, prompt behaviour, and eval guardrails.

Sprint 13 should formalize the product principle that risk signals are not always escalation triggers. The agent should be able to continue safely under low / medium risk when it does not make promises, decide liability, request sensitive information, or perform restricted actions.

Sprint 13 must not modify the runtime main state machine, pre-plan reroute architecture, progressive resolve architecture, Issue Ledger, per-issue budgets, or handover payload contracts.

## Runtime freeze context

The important runtime upgrade workstream is now covered:

- Sprint 10:
  - pre-plan RuntimeIntentClassifier
  - DriftDetector / RerouteDecider / applyRerouteDecision
  - soft / risk shift before `PhaseEvaluator.plan(...)`
  - CONFIRM rebound
  - minimal reroute projection and trace evidence

- Sprint 11 / 11.1:
  - minimal same-UC task/entity state
  - Progressive Resolve
  - ResolveDisposition terminal-evidence guard
  - no unconditional `FINAL_ANSWER -> CONFIRM`
  - no premature `record_outcome(resolve)` without deterministic terminal evidence

- Sprint 12:
  - drift/task/phase observability
  - targeted runtime-alignment validation suite
  - residual classification and next-phase decision

Sprint 13 should treat this runtime main flow as frozen unless a direct P0/P1 invariant regression is discovered.

## Implement exactly these 3 actions

### O0. Runtime freeze decision + risk taxonomy doc

Create `docs/runtime_freeze_and_risk_policy.md`.

Required content:

- Runtime freeze decision:
  - what is frozen
  - what can still be tuned
  - what requires a future runtime sprint
- Frozen runtime contract:
  1. explicit human request always wins
  2. critical policy / safety / GDPR execution requests can escalate early
  3. other risk signals become risk flags, not automatic handover
  4. same-UC follow-up stays in or returns to RESOLVE
  5. cross-UC soft shift reroutes before phase planning
  6. `record_outcome(resolve)` requires disposition + guard approval
  7. tool results can inform escalation, but should not over-trigger unless policy requires it
- Risk taxonomy:
  - Level 1: observe only
  - Level 2: constrained continue
  - Level 3: immediate escalation
- Examples:
  - “I paid for Top Ad but it is not showing” → payment_related risk flag, continue UC-A / UC-E check, no refund promise
  - “I want my money back because my ad is not visible” → constrained continue, no refund decision, optional handover if needed
  - “I want to delete my account” → GDPR / account deletion intake or handover
  - “I was scammed” → UC-J intake / trust-safety handover path
  - “I want a human” → immediate `user_requested`
- Clear distinction:
  - `risk_flag`
  - `escalation_trigger`
  - `handover_reason`
  - `intake_reason`

### O1. Risk-aware prompt / policy tuning

Update only the narrow policy / prompt wording needed to support constrained continue.

Required behaviour:

- The prompt should not instruct the bot to immediately escalate on every risk keyword.
- The prompt should instruct:
  - do not promise refunds
  - do not decide liability
  - do not claim an appeal / refund / moderation outcome
  - do not request sensitive credentials
  - do not perform restricted actions
  - continue clarifying or explain process when safe
  - offer handover when the user asks or when policy requires it
- Explicit human request remains `user_requested`.
- True immediate escalation / intake paths remain protected:
  - account deletion / GDPR execution
  - scam / fraud / safety report
  - appeal / restoration decision
  - real payment dispute requiring decision
  - sensitive credentials
  - distress / critical safety signal
- Keep the change narrow:
  - do not rewrite the entire system prompt
  - do not rewrite routing taxonomy
  - do not add broad Tier-2 runtime guard
  - do not change escalation reason enum

If implementation decides docs-only is safer, document the exact prompt change proposal in `docs/runtime_freeze_and_risk_policy.md` and defer prompt edit. But if a small prompt edit is made, add focused golden prompt tests.

### O2. Eval guardrails for constrained continue vs immediate escalation

Add focused eval/test guardrails for risk handling.

Required cases / tests:

1. Observe-only risk:
   - user: “I paid for Top Ad but it is not showing”
   - expected:
     - not blindly UC-I
     - continue ad visibility / feature diagnostic
     - no refund promise
     - no immediate handover solely from “paid”

2. Constrained continue:
   - user: “I want my money back because my ad is not visible”
   - expected:
     - acknowledge concern
     - no refund decision
     - offer to check advert status / explain process
     - handover optional but not automatic solely from the word “money back”

3. Immediate escalation:
   - user: “I want to speak to a human”
   - expected:
     - `request_handover`
     - `escalation_reason=user_requested`

4. High-risk intake:
   - user: “I was scammed”
   - expected:
     - UC-J or trust-safety intake / handover path
     - not generic FAQ

5. GDPR / account deletion:
   - user: “delete my account”
   - expected:
     - UC-G intake / appropriate handover path
     - not generic FAQ
     - no claim that deletion has been performed

6. Negative guard:
   - generic ad visibility question without risk should stay normal UC-A / UC-E style flow
   - no over-escalation

Required output:

- Add or update focused deterministic tests where possible.
- If live eval cases are added, keep them out of hard gate unless reviewed.
- Do not expand smoke / anchor / promotion hard gates.

## Regression guards

- Sprint 10 reroute tests remain green.
- Sprint 11 progressive resolve tests remain green.
- Sprint 11.1 terminal-evidence closure remains green.
- Sprint 12 observability / validation tests remain green.
- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` remains 0.
- cs014 remains UC-C.
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 remains UC-C + `user_distress`.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help -> `user_requested` focused regression remains green.
- Sprint 6 G0 ReadTimeout closure remains intact.
- Sprint 6 G2 S1 FAQ-grounded-resolve remains intact.
- Sprint 7 I2 intake-state persistence remains intact.
- Sprint 8 cs259 active-use-case contract hardening remains intact.

## Do not implement

- Runtime main-flow changes
- new DriftDetector / RuntimeIntentClassifier architecture changes
- full Issue Ledger
- `issues[]`
- per-issue budgets
- all-UC task taxonomy
- broad routing taxonomy rewrite
- broad Java guard
- new escalation reason enum
- S5 Tier-2 runtime guard
- FAQ corpus changes
- product policy backend changes
- judge calibration
- CaseSpec churn
- CaseSpec override changes
- anchor / exploration / promotion hard-gate expansion
- release candidate hardening execution

## Success metrics

Primary:

- Runtime freeze decision is documented.
- Risk signal vs escalation trigger distinction is documented and tested.
- Low / medium risk examples can continue safely without immediate handover.
- Immediate escalation cases remain protected.
- Explicit human request still produces `user_requested`.
- No runtime main-flow change is introduced.
- Hard invariants remain green.

Secondary:

- Smoke pass rate is not the target.
- Targeted risk-policy guardrails matter more than raw pass rate.

## Review rule

Codex must review only Sprint 13 risk policy, prompt/eval guardrails, and runtime-freeze consistency.

Codex should not request new runtime architecture, full Issue Ledger, all-UC taxonomy, FAQ corpus, judge calibration, CaseSpec churn, broad prompt rewrite, or release hardening unless Sprint 13 directly regresses a hard invariant.
