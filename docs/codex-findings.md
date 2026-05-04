# Blocking Sprint Failures

## P1 - B1 target case `cs_interactive_014` still does not demonstrate distress stability

- severity: P1
- target case or test: `cs_interactive_014` in `eval_interactive/results/20260504-151311/results.json`; focused coverage is missing for the actual cs014 transcript.
- blocks current sprint goal: Yes. Sprint 2 B1 asked whether `cs_interactive_002` and `cs_interactive_014` now fail less because of distress detection. `cs_interactive_002` improved to `escalation_reason=user_distress`, but `cs_interactive_014` still fails `L1:escalation_compliance` with actual `turn_budget_exhausted` against expected `user_distress`; it also drifts to `active_use_case=UC-B`.
- exact minimal fix: Add one focused regression using the actual cs014 form text and follow-up turns, then make the deterministic path satisfy it by preserving the Replies/Messaging route and stamping `user_distress` before budget close-out. If those cs014 utterances are not meant to be distress signals, update the smoke case expected trigger instead and rerun the targeted eval.

# Non-Blocking Notes

## P2 - B0 persisted handover reason normalization is implemented and covered

- severity: P2
- target case or test: `AgentRunLoopHandoverReasonNormalizationIntegrationTest`; latest eval runs `20260504-151311` and `20260504-151839`.
- blocks current sprint goal: No.
- exact minimal fix: No code fix. Keep the regression in the targeted suite; both latest eval runs have zero `L1:escalation_reason_consistency` failures.

## P2 - B1 precedence works for explicit distress and explicit user request paths

- severity: P2
- target case or test: `EscalationReasonResolverTest`, `ControlKernelDistressPrecedenceIntegrationTest`, and `cs_interactive_002`.
- blocks current sprint goal: No, apart from the separate cs014 gap above.
- exact minimal fix: No code fix for the covered paths. The focused tests show `user_distress` beats FAQ/budget reasons and `user_requested` still beats distress; canonical eval shows `cs_interactive_002` moved from `faq_miss_threshold_exceeded` to `user_distress`.

## P2 - B2/B3 target routing behavior moved in the intended direction

- severity: P2
- target case or test: `UseCaseRouterB2BiasTest`, `ControlKernelB3FallbackUseCaseTest`, `cs_interactive_095`, `cs_interactive_066`, and `cs_interactive_029`.
- blocks current sprint goal: No.
- exact minimal fix: No code fix for the canonical Sprint 2 target checks. In `20260504-151311`, `cs_interactive_095` routes to UC-A instead of UC-K, `cs_interactive_066` still routes to UC-K with `intake_complete_for_uc_k`, and `cs_interactive_029` avoids `CONTRACT_VIOLATION:active_use_case` while preserving `user_requested`.

## P2 - Latest canonical eval reduced the targeted blocker counts

- severity: P2
- target case or test: baseline `20260504-100538` to canonical Sprint 2 run `20260504-151311`.
- blocks current sprint goal: No.
- exact minimal fix: No code fix. The targeted blockers improved: `CONTRACT_VIOLATION:active_use_case` 1 -> 0, `L1:escalation_compliance` 2 -> 1, `L1:no_forbidden_tools` 2 -> 0, `L1:no_pii_leakage` 1 -> 0, and pass rate 6/14 -> 7/14.

# Regression Risks

## P2 - Stability run does not reproduce every canonical target win

- severity: P2
- target case or test: stability eval `20260504-151839`; target cases `cs_interactive_002`, `cs_interactive_066`, and `cs_interactive_095`.
- blocks current sprint goal: No, because the canonical run proves the routing/reason fixes, but it weakens confidence in repeatability.
- exact minimal fix: Add a targeted two-run stability gate after LLM/API timeout handling: require `cs_interactive_002` to reach `user_distress`, `cs_interactive_066` to avoid stalls while staying UC-K, and `cs_interactive_095` to avoid UC-K and avoid unnecessary escalation.

## P2 - Current B2/B1 tests are mostly helper-level for cs014

- severity: P2
- target case or test: `UseCaseRouterB2BiasTest`, `ControlKernelDistressPrecedenceIntegrationTest`, and `cs_interactive_014`.
- blocks current sprint goal: No as a separate risk; the blocking cs014 failure is listed above.
- exact minimal fix: Add one route/loop regression that starts from the cs014 smoke form context and replays the observed follow-up turns, asserting the resolved UC and persisted handover reason together rather than testing only phrase helpers.

# Recommended Next Sprint Actions

## P1 - Close cs014 with a case-level deterministic regression

- severity: P1
- target case or test: `cs_interactive_014`.
- blocks current sprint goal: Yes; it is the remaining Sprint 2 target miss.
- exact minimal fix: Implement the smallest case-level fix that makes the existing cs014 smoke transcript land on the intended UC/reason pair, or explicitly revise the case expectation if the transcript should not be considered distress.

## P2 - Stabilize targeted eval repeatability before expanding scope

- severity: P2
- target case or test: `eval_interactive/results/20260504-151839/results.json`.
- blocks current sprint goal: No.
- exact minimal fix: Address the timeout/stall path first, then rerun only the Sprint 2 target set before starting broad routing or outcome work.

## P2 - Preserve current B0/B2/B3 checks as sprint gates

- severity: P2
- target case or test: `AgentRunLoopHandoverReasonNormalizationIntegrationTest`, `UseCaseRouterB2BiasTest`, `ControlKernelB3FallbackUseCaseTest`, `cs_interactive_029`, `cs_interactive_066`, and `cs_interactive_095`.
- blocks current sprint goal: No.
- exact minimal fix: Keep these tests in the required targeted suite and add a lightweight eval assertion that fails on any return of `L1:escalation_reason_consistency`, `CONTRACT_VIOLATION:active_use_case`, `cs095 active_use_case=UC-K`, or `cs066 active_use_case!=UC-K`.
