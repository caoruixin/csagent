# Current Handoff

Date: 2026-05-08
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 10 — Runtime Re-route MVP (in flight; awaiting Codex review).

Latest closed sprint:
Sprint 9 / 9.1 — Tool Contract and Trace Observability Fidelity
(archived under `docs/sprints/sprint-009-*`).

## 2. Sprint 10 goal

Sprint 10 implements the first runtime alignment workstream: pre-plan
re-route and soft-shift handling before `PhaseEvaluator.plan(...)`.

The kernel (`ControlKernel.processMessage`) now decides — between hard
guards / explicit-human / distress checks and the planner — whether
the user message:

- continues the active issue (CONFIRM rebound to RESOLVE);
- shifts to a new low-risk UC (soft shift);
- enters a high-risk UC's intake / handover path; or
- escalates immediately (existing precedence preserved).

Scope is exactly the three Sprint 10 actions L0 / L1 / L2; nothing
outside the do-not-implement list was opened.

## 3. Sprint 10 implementation

### L0 — Internal `RuntimeIntentClassifier` + `RerouteDecision`

`server/src/main/java/com/gumtree/csagent/model/IntentClassification.java` (new)
- Record `IntentClassification(predictedUseCase, confidence, relation,
  taskType, primaryEntityType, primaryEntityValue)` with
  `IntentRelation` enum
  (`SAME_ISSUE / SAME_UC_NEW_TASK / NEW_LOW_RISK_UC / NEW_HIGH_RISK_UC
  / HUMAN_REQUEST / CRITICAL_ESCALATION / UNKNOWN`).

`server/src/main/java/com/gumtree/csagent/model/RerouteDecision.java` (new)
- Record `RerouteDecision(action, targetUseCase, targetPhase,
  transitionReason, classification)` with `RerouteAction` enum
  (`CONTINUE_CURRENT / SOFT_SHIFT_TO_DISCOVER / RISK_SHIFT_TO_INTAKE
  / REBOUND_TO_RESOLVE / ESCALATE_IMMEDIATELY`).

`server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java` (new)
- Runtime-internal `@Service` (NOT registered with the agent-visible
  tool surface). Reuses `EscalationReasonResolver` for
  distress / explicit-human and adds Sprint-10 MVP regex shapes:
  - `UC_C_NEW_REPLIES_PATTERN` ("I haven't got replies", "no replies");
  - `UC_A_SAME_ISSUE_PATTERN` ("I still can't see my ad",
    "still not showing");
  - `UC_A_FOLLOWUP_DURATION_PATTERN`
    ("how long is it active for", "when does it expire");
  - `UC_J_RISK_SHIFT_PATTERN`
    ("I was scammed", "fraud", "harassed");
  - `PAYMENT_AMBIGUITY_AD_VISIBILITY_PATTERN`
    ("paid for Top Ad … not showing") — the negative guard that keeps
    UC-A and refuses UC-I drift.
- Reads `form_context.ad_id` (when present) into the classification's
  `primaryEntityType / primaryEntityValue` slots.
- Falls back to `DriftDetector.HARD_SHIFT` results for refund / GDPR /
  ad-removed signals so the legacy hard-shift keyword set still flows
  through the new reroute path (refund → UC-I `NEW_LOW_RISK_UC`,
  GDPR → UC-G / scam → UC-J `NEW_HIGH_RISK_UC`, ad removed → UC-H).
- Returns `IntentRelation.UNKNOWN` on every other message; the kernel
  then leaves the session untouched.

`server/src/main/java/com/gumtree/csagent/service/runtime/RerouteDecider.java` (new)
- Maps `(IntentClassification, current_phase, current_uc)` to the
  Sprint-10 action matrix:

| current state                    | classifier output              | action                  | post-state               |
|----------------------------------|--------------------------------|-------------------------|--------------------------|
| CONFIRM, UC-A                    | NEW_LOW_RISK_UC -> UC-C        | SOFT_SHIFT_TO_DISCOVER  | UC-C, RESOLVE            |
| RESOLVE, UC-A                    | NEW_LOW_RISK_UC -> UC-C        | SOFT_SHIFT_TO_DISCOVER  | UC-C, RESOLVE (stay)     |
| CONFIRM, UC-A                    | SAME_ISSUE                     | REBOUND_TO_RESOLVE      | UC-A, RESOLVE            |
| CONFIRM, UC-A                    | SAME_UC_NEW_TASK               | REBOUND_TO_RESOLVE      | UC-A, RESOLVE            |
| any, any                         | NEW_HIGH_RISK_UC -> UC-J       | RISK_SHIFT_TO_INTAKE    | UC-J, RESOLVE (intake)   |
| any, any                         | HUMAN_REQUEST                  | ESCALATE_IMMEDIATELY    | (kernel step 2.5 handles)|
| any, any                         | UNKNOWN                        | CONTINUE_CURRENT        | unchanged                |

- An already-committed UC is preserved unless the action explicitly
  switches it (SOFT / RISK / REBOUND on the same UC).
- `RESOLVE -> DISCOVER` is forbidden by the existing
  `control-policy.yaml` transition table; the decider keeps the
  session in RESOLVE and simply switches the UC, so the planner
  re-runs RESOLVE for the new UC. The transition table itself was
  not broadened (per Sprint 10 do-not-implement list).

### L1 — Cross-UC soft / risk shift before `PhaseEvaluator.plan(...)`

`server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
- Constructor extended with `RuntimeIntentClassifier` + `RerouteDecider`
  dependencies. The pre-Sprint-10 13-arg constructor is preserved as a
  backward-compat overload that auto-instantiates the new beans, so
  the 16 existing test fixtures (`Sprint8Cs259ActiveUseCaseHardeningTest,
  ControlKernelB3FallbackUseCaseTest, Cs014RouteAndLoopHandoverIntegrationTest,
  …`) continue to compile and run unchanged. The all-args constructor
  is `@Autowired` so Spring continues to use it for the singleton bean.
- `processMessage` runtime order:
  1. Increment `totalBotTurns`.
  2. (Step 2.4) distress detection — stamps `user_distress` via
     resolver precedence (priority 2). No phase change.
  3. (Step 2.5) explicit-human escalation — terminal forceEscalate
     with `user_requested`.
  4. (Step 3) budget exhaustion — terminal forceEscalate.
  5. (Step 4) `DriftDetector.detect()`. `USER_ESCALATION_REQUEST` is
     still terminal forceEscalate with `user_requested`. The legacy
     `HARD_SHIFT -> immediate forceEscalate(service_degraded)` branch
     was REMOVED — hard-shift signals now flow into
     `RuntimeIntentClassifier` as a `DriftResult.newUseCase` hint and
     the decider picks the appropriate action (intake vs FAQ vs
     continue).
  6. **(NEW) Step 4.5 — `applyRerouteDecision(session, userMessage,
     drift, phaseBefore)`.** Calls
     `RuntimeIntentClassifier.classify(...)` and
     `RerouteDecider.decide(...)`, then mutates
     `(session.activeUseCase, session.currentPhase,
     session.intentConfidence, session.candidateUseCases)` per the
     decision. Phase transitions are validated via
     `ControlPolicyService.isValidTransition` so a forbidden
     transition leaves the session in `phaseBefore`.
  7. `phaseBefore` is reassigned to the post-reroute phase so the
     existing AgentRunLoop block, persisted `bot_turns.phase_before`,
     and `applyTransition` validation use the post-reroute view. The
     pre-reroute UC + phase are preserved on the session via
     `previousActiveUseCase` and the emitted `REROUTE_DECISION`
     event.
  8. (Step 5+) `PhaseEvaluator.plan(session, ...)` and
     `AgentRunLoop.run(...)` proceed unchanged.

- Existing `forceEscalate` / `applyMissingUseCaseFallback` /
  `recordRunResult` paths are untouched. Existing distress and
  explicit-escalation precedence is preserved (they still run BEFORE
  the classifier).

### L2 — Minimal issue-state projection + drift observability

`server/src/main/java/com/gumtree/csagent/model/BotSession.java`
- Five `@Transient` fields added (NOT persisted — populated each turn
  by `applyRerouteDecision` and read by `ContextProjectionBuilder`):
  - `previousActiveUseCase`
  - `driftType` (token: `SOFT_SHIFT / RISK_SHIFT / SAME_ISSUE /
    SAME_UC_NEW_TASK / ESCALATE / null`)
  - `currentTaskType` (e.g. `listing_visibility_diagnostic`,
    `listing_visibility_paid_promotion`,
    `listing_lifecycle_followup`, `messaging_diagnostic`,
    `fraud_or_safety_intake`)
  - `primaryEntityType` (`listing` for ad_id-bearing turns, otherwise
    null)
  - `primaryEntityValue`
  - `issueStatusSummary` (defaults to `"open"`).

`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
- `buildProjection` emits the five §L2 keys after
  `candidate_use_cases` (which is unchanged):
  - `previous_active_use_case`
  - `drift_type`
  - `current_task_type`
  - `primary_entity` (object: `{entity_type, ad_id}` when listing,
    `{entity_type, entity_value}` otherwise; null when neither is
    present).
  - `issue_status_summary`
- Absent / null transient slots produce JSON `null` so the projection
  shape is stable across turns.
- The full `issues[]` ledger, per-issue budgets, and the all-UC task
  taxonomy are explicitly NOT introduced.

### Drift observability

`ControlKernel.applyRerouteDecision`
- `log.info` line per applied reroute with predicted UC, relation,
  action, previous UC, new UC, previous phase, new phase, transition
  reason.
- `BotEvent` of type `REROUTE_DECISION` emitted with payload
  `{predicted_use_case, relation, action, previous_use_case,
  new_use_case, previous_phase, new_phase, transition_reason,
  confidence, task_type}` so the trace UI / eval harness can
  surface the runtime reroute path.
- `CONTINUE_CURRENT` / `UNKNOWN` no-op turns are logged at debug
  only — production logs stay quiet on uneventful turns.

## 4. Files changed (Sprint 10)

Production:
- `server/src/main/java/com/gumtree/csagent/model/IntentClassification.java` (new)
- `server/src/main/java/com/gumtree/csagent/model/RerouteDecision.java` (new)
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java` (added 6
  `@Transient` fields)
- `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/RerouteDecider.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (constructor extended; legacy HARD_SHIFT immediate-escalate branch
  removed; new `applyRerouteDecision` + `deriveDriftTypeToken` helpers;
  `phaseBefore` reassigned post-reroute).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  (5 §L2 projection slots after `candidate_use_cases`).

Tests (new):
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint10RuntimeIntentClassifierTest.java`
  (13 tests, all green) — pins the six Sprint-10 MVP shapes plus
  the UNKNOWN / blank fallback and the regex-level smoke checks.
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint10RerouteDecisionTest.java`
  (9 tests, all green) — pins the eight focused tests from the
  Sprint 10 spec:
  1. UC-A / CONFIRM + "I haven't got replies" → UC-C, no handover
  2. UC-A / CONFIRM + "I still can't see my ad" → UC-A, RESOLVE
  3. UC-A / CONFIRM + "how long is it active for?" → UC-A, RESOLVE
  4. UC-A + "I was scammed" → UC-J intake/handover path, not FAQ
  5. UC-A + "I want a human" → ESCALATE_IMMEDIATELY (user_requested)
  6. UC-A + "I paid for Top Ad but it's not showing" → not blindly
     UC-I (stays UC-A)
  7. Projection snapshot includes the §L2 slots
  8. Already-committed UC is preserved on CONTINUE_CURRENT and on
     same-issue rebound (8a + 8b).

Docs:
- `docs/10-handoff.md` (this file; Sprint 9 closure archived to
  `docs/sprints/sprint-009-handoff-closure.md`).
- `docs/action_bank.md` (Sprint 10 row added; Sprint 9 version
  archived to `docs/sprints/sprint-009-action_bank.md`).
- `docs/sprint_objective.md` archived to
  `docs/sprints/sprint-010-runtime-reroute-mvp-objective.md`
  (current `docs/sprint_objective.md` retained).

## 5. Tests run

- `mvn -pl server test` → **782 / 0 / 0 / 0** (was 760 pre-Sprint-10;
  +22 new Sprint-10 tests).
- Targeted Sprint-7 / 8 / 9 regression sweep
  (`Cs014RouteAndLoopHandoverIntegrationTest,
  Sprint8Cs259EscalateBranchIntegrationTest,
  Cs176ExplicitHumanHelpHandoverIntegrationTest,
  Sprint7CandidateUseCasesProjectionTest, Sprint7IntakeStateTest,
  Sprint71PartialIntakePersistenceTest,
  Sprint8Cs259ActiveUseCaseHardeningTest,
  Cs002AlreadyEscalatedDistressReconcileIntegrationTest,
  Cs001LlmDistressGateIntegrationTest`) → **81 / 0 / 0 / 0**.
- Integration + Sprint sweep (`*Integration*, Sprint*Test,
  Sprint9TerminalToolHonestyTest, Sprint9TraceObservabilityFidelityIntegrationTest`)
  → **156 / 0 / 0 / 0**.
- `python -m pytest -p no:capture eval_interactive/tests/`
  → **294 / 0** (full Python eval test suite, including
  `test_agent_client_session_create_timeout.py` for the Sprint 6 §G0
  ReadTimeout no-retry contract, plus all `regression/`, `scoring/`,
  and `trace/` packages).

Smoke runs were NOT executed for Sprint 10 — the change is a
pre-plan reroute layer that does not affect FAQ corpus, judges, or
LLM credentials. Live smoke is recommended ONLY when Codex
explicitly requests it; otherwise the focused JUnit + Python
regression suite is the canonical Sprint 10 evidence.

## 6. Result paths

No new smoke run was promoted in Sprint 10 (no FAQ / corpus / judge
change). The current canonical eval baseline remains the post-
Sprint-8 r1 run documented in `docs/current_eval_baseline.md`:

- `eval_interactive/results/20260505-234448/results.json` (8/14,
  mean composite 0.4784, `sprint8-r1`).
- `eval_interactive/results/20260505-235231/results.json` (9/14,
  mean composite 0.5255, `sprint8-r2`).

When a clean Sprint-10 smoke run is later captured under clean
Kimi credentials, promote it only if:

- `L1:escalation_reason_consistency = 0`
- `CONTRACT_VIOLATION:active_use_case = 0`
- targeted Sprint 10 reroute blockers (cs014 / cs066 / cs095 /
  cs176 / cs002 / cs029) remain green or stable.

## 7. Target outcomes — before vs after

Pre-Sprint-10 control flow when the user said "I haven't got replies"
on a UC-A / CONFIRM session:
```
DriftDetector            -> NONE (pattern miss; no "scam"/"refund"/etc)
PhaseEvaluator.plan      -> CONFIRM plan (no UC switch)
AgentRunLoop             -> LLM reads CONFIRM systemInstruction
                            -> may emit record_outcome(resolve) or
                               request_handover(user_dissatisfied)
                            depending on per-turn LLM variance.
```
The LLM had no runtime guidance to interpret the message as a
new UC-C intent; the trace would frequently land in CONFIRM →
ESCALATE with `service_degraded` or `user_dissatisfied`.

Post-Sprint-10:
```
DriftDetector            -> NONE
RuntimeIntentClassifier  -> NEW_LOW_RISK_UC, predictedUc=UC-C,
                            taskType=messaging_diagnostic
RerouteDecider           -> SOFT_SHIFT_TO_DISCOVER (UC-A->UC-C, RESOLVE)
ControlKernel            -> session.activeUseCase=UC-C,
                            session.currentPhase=RESOLVE,
                            previousActiveUseCase=UC-A,
                            driftType=SOFT_SHIFT,
                            REROUTE_DECISION event emitted.
PhaseEvaluator.plan      -> RESOLVE_FAQ plan for UC-C
AgentRunLoop             -> grounded resolve sequence on UC-C corpus.
```

For the same UC-A / CONFIRM session saying "I was scammed":
```
Pre-Sprint-10:
DriftDetector            -> HARD_SHIFT to UC-J
ControlKernel            -> setActiveUseCase(UC-J);
                            applyEscalationReason("service_degraded");
                            forceEscalate (terminal).
                            Persisted: ESCALATE / service_degraded
                            (intake fields never collected).

Post-Sprint-10:
DriftDetector            -> HARD_SHIFT (still flags UC-J)
RuntimeIntentClassifier  -> NEW_HIGH_RISK_UC, predictedUc=UC-J
RerouteDecider           -> RISK_SHIFT_TO_INTAKE (UC-J, RESOLVE)
ControlKernel            -> session.activeUseCase=UC-J,
                            session.currentPhase=RESOLVE,
                            driftType=RISK_SHIFT.
PhaseEvaluator.plan      -> RESOLVE_INTAKE plan for UC-J.
AgentRunLoop             -> intake collection -> request_handover
                            (intake_complete_for_uc_j).
                            Persisted: ESCALATE /
                            intake_complete_for_uc_j (correct
                            intake-class reason).
```

For "I paid for Top Ad but it's not showing" on UC-A / CONFIRM:
```
Pre-Sprint-10:
DriftDetector            -> HARD_SHIFT to UC-I (refund/payment keyword
                            "paid")
ControlKernel            -> forceEscalate UC-I, service_degraded.

Post-Sprint-10:
PAYMENT_AMBIGUITY_AD_VIS -> matches; predictedUc=UC-A, SAME_ISSUE
RerouteDecider           -> REBOUND_TO_RESOLVE (UC-A, RESOLVE).
                            currentTaskType=
                            listing_visibility_paid_promotion.
PhaseEvaluator.plan      -> RESOLVE_FAQ plan for UC-A (correct
                            intent: ad visibility, not refund).
```

## 8. Regression guards

Active guards (all green in the full Sprint-10 server run):

- `L1:escalation_reason_consistency` = 0 (never re-introduced)
- `CONTRACT_VIOLATION:active_use_case` = 0
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest` green)
- cs066 remains UC-K
- cs095 remains UC-A / not UC-K / not UC-FP
- cs002 remains UC-C + `user_distress`
- cs029 remains UC-D + `user_requested`
- cs176 explicit-human-help → `user_requested` focused regression
  (`Cs176ExplicitHumanHelpHandoverIntegrationTest` green)
- Sprint 6 §G0 no ReadTimeout retry
  (`test_agent_client_session_create_timeout.py` 8 / 0)
- Sprint 6 §G2 FAQ-grounded-resolve guard
  (`AgentRunLoopS1FaqGroundedResolveGuardTest` green)
- Sprint 7 §I0 candidate_use_cases projection
- Sprint 7 §I2 intake_state persistence
- Sprint 7.1 §J0 partial intake persistence
- Sprint 8 §K0 cs259 active_use_case contract
  (`Sprint8Cs259ActiveUseCaseHardeningTest` green;
  `Sprint8Cs259EscalateBranchIntegrationTest` green)
- Sprint 8.1 §M3 DISCOVER → RESOLVE phase boundary
  (`Sprint81DiscoverPhaseBoundaryTest` green;
  `Sprint81DiscoverPhaseBoundaryReplanIntegrationTest` green)
- Sprint 8.2 §M0a / §M0b resolve_article + max-steps raw response
- Sprint 9 §O0 / §O1 / §O2 + Sprint 9.1 sanitization
  (`Sprint9TerminalToolHonestyTest, ToolCallTraceSanitizerTest,
  Sprint9TraceObservabilityFidelityIntegrationTest` green)

New Sprint 10 guards:

- Sprint 10 §L0 — `RuntimeIntentClassifier` deterministic shape
  pinning for the six MVP cases, including the payment-ambiguity
  negative guard
  (`Sprint10RuntimeIntentClassifierTest`).
- Sprint 10 §L1 — `RerouteDecider` action matrix and
  `ControlKernel.applyRerouteDecision` state mutation;
  CONTINUE_CURRENT preserves the committed UC; same-issue rebound
  preserves the UC and only changes the phase
  (`Sprint10RerouteDecisionTest`).
- Sprint 10 §L2 — projection emits
  `previous_active_use_case / drift_type / current_task_type /
  primary_entity / issue_status_summary` after `candidate_use_cases`
  (`Sprint10RerouteDecisionTest.test7`).
- Hard-shift signals no longer terminal-escalate immediately;
  scam → UC-J flows through the RESOLVE_INTAKE plan instead of
  forceEscalate.
- Constructor backward-compat: the pre-Sprint-10 13-arg
  `ControlKernel` constructor remains usable for the 16 existing
  test fixtures.

## 9. Remaining P0 / P1 blockers

- **None opened by Sprint 10.** The runtime change is a pre-plan
  layer; it does not introduce new tool surfaces, schema changes, or
  external contract changes.
- The Eval Governance backlog (cs015 / cs066 / cs176 deferrals,
  L3 judge volatility, FAQ corpus answerability, advert-link
  product policy, rerank fallback diagnostics) is unchanged and
  remains under `docs/action_bank.md` §4 / §5 as deferred /
  governance work.
- Live re-probe of the Sprint-10 reroute path against a Kimi-backed
  deploy is recommended once Codex passes Sprint 10 — any residual
  must be classified as "downstream LLM tool variance" rather than a
  reroute regression.

## 10. Was Sprint 10 objective met?

Yes:

- L0 `RuntimeIntentClassifier` + `RerouteDecision` model implemented
  as a runtime-internal service (NOT the agent-visible
  `classify_use_case` tool). Six MVP shapes covered with the
  payment-ambiguity negative guard.
- L1 reroute decision applied before `PhaseEvaluator.plan(...)` per
  the required order
  (hard guards → explicit-human / distress / critical → classifier
  → decider → state mutation → plan → run-loop). CONFIRM-rebound,
  soft-shift, risk-shift-to-intake, and human-request transitions
  all wired. Existing distress / explicit-escalation precedence
  preserved.
- L2 minimal projection slots emitted; `candidate_use_cases`
  retained; `issues[]`, per-issue budgets, full handover payload
  rewrite are all explicitly NOT introduced.
- 8 focused tests + 13 classifier shape tests pass; 782 / 0 / 0 / 0
  full server run; 294 / 0 Python eval run; all Sprint 6 / 7 / 7.1 /
  8 / 8.1 / 8.2 / 9 / 9.1 regression guards green.

Out-of-scope items (Progressive Resolve, ResolveDisposition, full
Issue Ledger, per-issue budgets, all-UC task taxonomy, full
handover payload rewrite, S3 no-prior-search guard, S5 Tier-2
runtime guard, judge calibration, CaseSpec churn, FAQ corpus
changes, broad prompt rewrite, broad routing taxonomy rewrite,
Eval Governance docs) were NOT touched.

## 11. Next recommended phase

If Codex passes Sprint 10 with `decision: pass` /
`blocking_count: 0`, the recommended next phase is:

**Sprint 11 — Progressive Resolve MVP (or closure fix).**

Progressive Resolve would extend the Sprint-10 reroute layer with
same-UC progressive-resolve disposition handling (the Sprint 10
do-not-implement list explicitly carved this out). Until then, the
current handoff covers exactly the L0 / L1 / L2 reroute MVP scope.

Do not start another runtime sprint unless triage finds a new
P0/P1 runtime blocker (none identified at Sprint 10 close).

## 12. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

## 13. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- advert-link generator / direct listing URL tool
- Progressive Resolve / ResolveDisposition / full Issue Ledger /
  issues[] / per-issue budgets / all-UC task taxonomy
- runtime sprint unless a new P0/P1 runtime blocker is found
