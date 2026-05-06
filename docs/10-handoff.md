# Current Handoff

Date: 2026-05-06
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Pre-Sprint-9 handoff compaction complete / Sprint 9 Eval Governance
next.

Latest closed sprint:
Sprint 8 — Targeted cs259 Active-Use-Case Contract Hardening.

Latest Codex decision:
- decision: pass
- blocking_count: 0

## 2. Current authoritative baseline

Canonical baseline:
`eval_interactive/results/20260505-234448/results.json`
(post-Sprint-8 r1, `sprint8-r1`, 8/14, mean composite 0.4784,
0 `CONTRACT_VIOLATION:active_use_case`,
0 `L1:escalation_reason_consistency`,
0 ReadTimeout / 0 `INFRA:ReadTimeout` /
0 `session_create_failed`).

Nondeterminism reference:
`eval_interactive/results/20260505-235231/results.json`
(post-Sprint-8 r2, `sprint8-r2`, 9/14, mean composite 0.5255,
same 0 / 0 / 0 contract + reason + ReadTimeout numbers).

Do not promote:
- contaminated Sprint 7 auth-failure runs
- diagnostic-only runs
- any run explicitly marked non-canonical

## 3. Current accepted state

Summary of latest accepted runtime state only. Historical
sprint-by-sprint detail lives in `docs/sprints/`.

Sprint 6 G0 closed:
- exactly one ReadTimeout mitigation
- 120 s create-session timeout widen only
- no ReadTimeout retry

Sprint 6 G1 closed:
- explicit human-help → `user_requested` focused-evidence
  regression on cs_interactive_176

Sprint 6 G2 closed:
- FAQ-grounded-resolve S1 flow / guard
  (`shouldRejectFaqMissHandover`)

Sprint 7 + 7.1 closed:
- `candidate_use_cases` projection + DISCOVER cue (I0)
- UC-FP vs UC-A routing tiebreaker wiring (I1)
- `intake_state` projection + UC-G/H/I/J/K intake-complete
  guard (I2)
- partial intake-field persistence across clarification turns
  (J0)

Sprint 8 closed:
- cs259 active_use_case contract hardened
  (`applyMissingUseCaseFallback` reused at the AgentRunLoop
  ESCALATE branch + UC-F regex sale-proceeds vocabulary)
- targeted cs259 and clean smoke runs commit UC-F
- `CONTRACT_VIOLATION:active_use_case = 0` in accepted clean
  runs

## 4. Current open work

Next planned phase:
Sprint 9 — Eval Governance and Release Gate Definition.

Sprint 9 is docs-only.

Expected deliverables:
- `docs/eval_convergence_policy.md`
- `docs/case_label_confidence.md`
- `docs/release_gate.md`
- `qa-reports/post-sprint8-residual-failure-triage.md`

## 5. Remaining residual failures

| case   | classification                                     | owner          | blocks runtime sprint |
|--------|----------------------------------------------------|----------------|-----------------------|
| cs259  | FAQ corpus / answerability residual after UC-F contract fix | Eval Governance | no |
| cs015  | possible moderation-signal projection follow-up, deferred   | Eval Governance | no |
| cs066  | stall detector / persona variance                  | Eval Governance | no |
| cs038  | stall detector / persona variance                  | Eval Governance | no |
| cs095  | product policy / FAQ corpus gap                    | Eval Governance | no |
| cs176  | deferred UC-I drift                                | Eval Governance | no |
| cs192  | FAQ corpus / answerability                         | Eval Governance | no |
| L3 relevance / tone | judge volatility                      | Eval Governance | no |

## 6. Regression guards

Only currently-active guards:

- `L1:escalation_reason_consistency = 0`
- `CONTRACT_VIOLATION:active_use_case = 0`
- cs014 remains UC-C
- cs066 remains UC-K
- cs095 remains not UC-K / not UC-FP
- cs002 remains UC-C + `user_distress`
- cs029 remains UC-D + `user_requested`
- cs176 explicit-human-help → `user_requested` focused
  regression remains green
- Sprint 6 G0 no ReadTimeout retry remains green
- Sprint 6 G2 FAQ-grounded-resolve guard remains green
- Sprint 7 `intake_state` persistence tests remain green
- Sprint 8 cs259 UC-F contract hardening remains green

## 7. Current-doc maintenance rule

The current working docs are overwrite-current-state files:

- `docs/10-handoff.md`
- `docs/codex-findings.md`
- `docs/sprint_objective.md`

They must not become append-only historical logs.

Before replacing one of these files for a new sprint, closure
review, or post-sprint validation:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file with the latest current
   version;
3. keep only actionable current state in the working file.

Historical details belong in:

- `docs/sprints/`
- `docs/archive/current-docs/`
- `eval_interactive/results/`
- `qa-reports/`

## 8. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- runtime sprint unless a new P0/P1 runtime blocker is found

## 9. Next action

Next action:
Define / execute Sprint 9 Eval Governance docs-only sprint.

Do not start another runtime sprint unless Sprint 9 triage
finds a new P0/P1 runtime blocker.
