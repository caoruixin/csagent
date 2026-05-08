# Current Handoff

Date: 2026-05-08
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 12 — Runtime Alignment Hardening and Validation
(in flight; awaiting Codex review).

Latest closed sprint:
Sprint 11 / 11.1 — Progressive Resolve MVP + terminal-evidence
closure (archived under `docs/sprints/sprint-011-*`).

## 2. Sprint 12 goal

Sprint 12 is a hardening + validation sprint on top of:

- Sprint 10 — Runtime Re-route MVP (cross-UC soft / risk shift +
  CONFIRM rebound before `PhaseEvaluator.plan`).
- Sprint 11 — Progressive Resolve MVP (same-UC task / entity state
  + `ResolveDisposition` + record-outcome guard).
- Sprint 11.1 — Terminal-evidence closure (non-slot UC-A same-UC
  answers without successful `record_outcome` stay RESOLVE as
  `ANSWERED_SUBTASK`).

Sprint 12 explicitly does NOT introduce a new broad runtime feature,
full Issue Ledger, `issues[]`, per-issue budgets, all-UC task
taxonomy, full skill runtime framework, handover payload rewrite,
FAQ corpus changes, judge calibration, CaseSpec churn, anchor /
exploration / promotion hard-gate expansion, Eval Governance docs,
or Release Candidate docs.

Scope is exactly the three Sprint 12 actions N0 / N1 / N2.

## 3. Sprint 12 implementation

### N0 — Drift / task / phase observability hardening

`server/src/main/java/com/gumtree/csagent/model/BotSession.java`
- Eight NEW `@Transient` Sprint 12 §N0 observability slots appended
  AFTER the Sprint 10 §L2 + Sprint 11 §M0 slots:
  - `predictedUseCase` — latest classifier `predicted_use_case`.
  - `intentRelation` — latest classifier `IntentRelation` token.
  - `rerouteAction` — latest decider `RerouteAction` token.
  - `phaseTransitionReason` — latest canonical
    `phase_transition_reason` (set on reroute and on
    `mapFinalAnswer` disposition decisions).
  - `resolveDisposition` — latest `ResolveDisposition` enum name
    (companion to `taskStatus`, which carries the projection-token
    form).
  - `recordOutcomeAttempted` / `recordOutcomeSucceeded` — terminal
    evidence summary from the most recent agent run.
  - `recordOutcomeGuardResult` — `none / allowed / rejected:<reason>`
    from the §M1 record-outcome guard.

`server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
- `applyRerouteDecision` now stamps the new §N0 slots
  (`predictedUseCase`, `intentRelation`, `rerouteAction`,
  `phaseTransitionReason`) on every classifier turn so the
  projection / trace evidence captures the latest decision even when
  the action is `CONTINUE_CURRENT`.
- The existing `REROUTE_DECISION` event payload is enriched with
  backward-compat alias keys
  (`intent_relation`, `reroute_action`, `phase_transition_reason`,
  `previous_active_use_case`, `active_use_case`, `drift_type`,
  `current_task_type`, `task_status`, `primary_entity`) alongside
  the existing Sprint 10 keys (`relation`, `action`,
  `transition_reason`, `previous_use_case`, `new_use_case`, etc.).
  Existing keys are preserved verbatim.
- New post-loop event emissions:
  - `RESOLVE_DISPOSITION` — payload `{resolve_disposition,
    task_status, phase_transition_reason, transition_reason,
    previous_phase, new_phase, active_use_case, current_task_type,
    primary_entity, terminal_evidence: {record_outcome_attempted,
    record_outcome_succeeded, record_outcome_success}}`. Emitted
    only on RESOLVE / FAQ plans so a reviewer can audit "why did the
    bot stay RESOLVE instead of CONFIRM".
  - `RECORD_OUTCOME_GUARD` — payload `{record_outcome_guard_result,
    plan_phase, plan_use_case, current_phase, active_use_case,
    terminal_evidence}`. Emitted only when the §M1 guard observed a
    `record_outcome` call during the turn.
- New helpers `buildPrimaryEntityPayload`,
  `emitResolveDispositionEvent`, `emitRecordOutcomeGuardEvent`,
  `buildTerminalEvidencePayload`.

`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
- `mapFinalAnswer` RESOLVE branch additionally stamps
  `session.resolveDisposition` (enum name) and
  `session.phaseTransitionReason` (`progressive_resolve_stay /
  agent_escalated / answer_provided`) so the post-loop kernel
  emission and the projection see the canonical values.

`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
- §M1 record-outcome guard now stamps
  `session.recordOutcomeGuardResult` to
  `rejected:progressive_resolve_record_outcome_premature` on
  rejection and to `allowed` on a passing call. Sticky semantics:
  the first rejection wins so a later allowed call cannot erase the
  audit signal in the same loop.
- Tool-dispatch path stamps `session.recordOutcomeAttempted` /
  `session.recordOutcomeSucceeded` whenever a `record_outcome` call
  lands. Every run-loop exit (final answer, escalate, max-steps,
  error) leaves the trace evidence consistent so the kernel post-
  loop helpers and the next turn's projection see the same view.

`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
- Surfaces the new Sprint 12 §N0 fields on every projection right
  after the Sprint 11 §M0 block:
  - `predicted_use_case`
  - `intent_relation`
  - `reroute_action`
  - `phase_transition_reason`
  - `resolve_disposition`
  - `record_outcome_guard_result`
  - `terminal_evidence: {record_outcome_attempted,
    record_outcome_succeeded, record_outcome_success}`
- Also emits two new aggregate observability fields:
  - `drift_history` — array of prior-turn entries with
    `{turn_index, drift_type, intent_relation, reroute_action,
    predicted_use_case, active_use_case, previous_active_use_case,
    phase_transition_reason}`.
  - `task_history` — array of prior-turn entries with
    `{turn_index, current_task_type, task_status,
    resolve_disposition, record_outcome_guard_result,
    issue_status_summary}`.
- Both aggregates are reconstructed from the conversation-history
  `BotTurn.projectedContext` JSON within the existing 10-turn window
  used for `conversation_history`. Cheap, deterministic, no new DB
  queries; gracefully skips legacy turns whose `projectedContext`
  predates these fields.
- All §N0 additions are backward-compatible: existing keys are
  preserved verbatim and the JSON shape stays stable across turns
  (absent values become JSON `null`).

A reviewer reading any single turn's `bot_turns.projected_context`
plus the per-turn `bot_events` rows can answer:
- why did the bot stay in current UC? — `intent_relation` /
  `reroute_action` / `drift_type` on the projection plus the
  `REROUTE_DECISION` event payload.
- why did the bot soft-shift? — `intent_relation=NEW_LOW_RISK_UC`,
  `reroute_action=SOFT_SHIFT_TO_DISCOVER`,
  `phase_transition_reason=soft_shift_to_<uc>`.
- why did the bot risk-shift? — `intent_relation=NEW_HIGH_RISK_UC`,
  `reroute_action=RISK_SHIFT_TO_INTAKE`,
  `phase_transition_reason=risk_shift_to_<uc>`.
- why did the bot stay RESOLVE instead of CONFIRM? —
  `RESOLVE_DISPOSITION` event +
  `resolve_disposition=ASKED_FOR_SLOT/ANSWERED_SUBTASK/CONTINUE_RESOLVE`,
  `terminal_evidence.record_outcome_succeeded=false`.
- why did `record_outcome(resolve)` get allowed or rejected? —
  `RECORD_OUTCOME_GUARD` event +
  `record_outcome_guard_result=allowed` or
  `rejected:progressive_resolve_record_outcome_premature`.

### N1 — Targeted runtime alignment validation suite

`server/src/test/java/com/gumtree/csagent/service/runtime/Sprint12RuntimeAlignmentValidationTest.java` (new)
- 13 deterministic regression tests covering the 10 Sprint 12 spec
  scenarios (each scenario gets one or two focused tests; entity
  reuse / multi-turn flows roll into a single end-to-end test). No
  live LLM dependence; reuses the existing Sprint 10 / 11 fixture
  shape.

| # | Scenario | Test |
|---|---|---|
| 1 | UC-A → UC-C soft shift on "I haven't got replies" | `scenario1_ucA_to_ucC_softShift_noGenericHandover` |
| 2 | UC-A same-issue dissatisfaction "I still can't see my ad" | `scenario2_ucA_sameIssue_stillCantSeeMyAd_reboundsToResolve` |
| 3 | UC-A same-UC follow-up "how long is it active for?" | `scenario3_ucA_sameUcFollowup_howLongIsItActive_reboundsToResolve` |
| 4 | UC-A progressive listing diagnostic — multi-turn entity reuse + final `user_requested` | `scenario4_progressiveListingDiagnostic_entityReuse_finalUserRequested` |
| 5 | UC-C messaging follow-up stays UC-C / RESOLVE | `scenario5_ucC_messagingFollowup_staysUcC_resolve` |
| 6 | Risk shift to UC-J on "I was scammed" | `scenario6_iWasScammed_riskShiftsToUcJ_intakePath_notFaq` |
| 7 | Explicit human request → `user_requested` | `scenario7_explicitHumanRequest_userRequested` |
| 8 | Payment ambiguity negative guard ("paid for Top Ad but it's not showing") | `scenario8_paymentAmbiguity_paidForTopAd_doesNotBlindlyRouteToUcI` |
| 9 | `record_outcome(resolve)` guard rejects premature close | `scenario9_recordOutcomeGuard_rejectsPrematureResolveBeforeConfirm` |
| 10a | Non-slot UC-A same-UC answer without terminal evidence stays RESOLVE | `scenario10a_nonSlotSameUcAnswer_withoutTerminalEvidence_staysResolve` |
| 10b | Valid terminal evidence may lead to READY_TO_CONFIRM | `scenario10b_validTerminalEvidence_mayLeadToReadyToConfirm` |

Plus two §N0 observability assertions:
- `observability_projectionSurfacesAllSprint12Fields` — pins that
  every Sprint 12 §N0 field
  (`drift_history, task_history, phase_transition_reason,
  reroute_action, intent_relation, predicted_use_case,
  previous_active_use_case, active_use_case, current_task_type,
  task_status, primary_entity, resolve_disposition,
  terminal_evidence, record_outcome_guard_result`) is present in the
  projection JSON with meaningful values for a soft-shift turn.
- `observability_projectionDriftHistory_aggregatesAcrossTurns` —
  pins that `drift_history` aggregates SOFT_SHIFT entries from prior
  turns' persisted `projectedContext` so a reviewer reading the
  latest turn sees the trajectory.

### N2 — Residual classification and next-phase decision

See §6 (residual classification) and §7 (next-phase recommendation)
below.

## 4. Files changed (Sprint 12)

Production:
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  (+ 8 Sprint 12 §N0 `@Transient` slots).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (REROUTE_DECISION payload alias keys; new RESOLVE_DISPOSITION +
  RECORD_OUTCOME_GUARD events; new helpers
  `buildPrimaryEntityPayload`, `emitResolveDispositionEvent`,
  `emitRecordOutcomeGuardEvent`, `buildTerminalEvidencePayload`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  (mapFinalAnswer stamps `session.resolveDisposition` +
  `session.phaseTransitionReason`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  (record-outcome guard stamps `recordOutcomeGuardResult`;
  successful / failed `record_outcome` dispatches stamp
  `recordOutcomeAttempted` / `recordOutcomeSucceeded`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  (+ §N0 single-value slots; + `drift_history` and `task_history`
  aggregates; new helpers `putNullableString`,
  `buildDriftAndTaskHistory`, `copyTextField`).

Tests (new):
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint12RuntimeAlignmentValidationTest.java`
  (13 tests, all green).

Docs:
- `docs/10-handoff.md` (this file; previous Sprint 11 / 11.1
  handoff already mirrored to `docs/sprints/sprint-011-handoff.md`).
- `docs/action_bank.md` (Sprint 12 row added; Sprint 11 entry
  moved to closed-action index).
- `docs/sprint_objective.md` retained — already contains the
  Sprint 12 objective.

No FAQ corpus, CaseSpec, judge, broad routing taxonomy, handover
payload, Salesforce contract, prompt rewrite, or Eval Governance
file was touched.

## 5. Tests run

- `mvn -pl server test` → **809 / 0 / 0 / 0** (was 796 pre-Sprint-12;
  +13 new Sprint-12 §N1 regressions).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  PhaseEvaluatorPlanTest,Sprint12*Test'` → **80 / 0 / 0 / 0**
  (Sprint 10 reroute MVP, Sprint 11 progressive-resolve MVP,
  Sprint 11.1 terminal-evidence closure, PhaseEvaluator plan
  regression, Sprint 12 §N1 validation suite).
- `python -m pytest eval_interactive/tests/` → **294 / 0** (full
  Python eval test suite, including
  `test_agent_client_session_create_timeout.py` for the Sprint 6
  §G0 ReadTimeout no-retry contract, plus all `regression/`,
  `scoring/`, `trace/` packages).

Smoke runs were NOT executed for Sprint 12. The change is a
state-projection + observability hardening + targeted regression
sprint that does not touch FAQ corpus, judges, CaseSpec, prompts,
or LLM credentials. Live smoke remains optional and is recommended
only when Codex explicitly requests runtime evidence for the
next-phase recommendation; the focused JUnit + Python regression
suite is the canonical Sprint 12 evidence.

## 6. Targeted validation results

Sprint 12 §N1 regressions (all green, deterministic):

```
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] Time elapsed: 0.86 s -- in
       com.gumtree.csagent.service.runtime.Sprint12RuntimeAlignmentValidationTest
```

Combined Sprint 10 / 11 / 11.1 + PhaseEvaluator + Sprint 12 sweep:

```
[INFO] Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
       (Sprint10*Test, Sprint11*Test, PhaseEvaluatorPlanTest,
        Sprint12*Test)
```

Smoke result paths: none promoted in Sprint 12. The current
canonical eval baseline remains the post-Sprint-8 r1 / r2 runs
documented in `docs/current_eval_baseline.md`:

- `eval_interactive/results/20260505-234448/results.json` (8/14,
  mean composite 0.4784, `sprint8-r1`).
- `eval_interactive/results/20260505-235231/results.json` (9/14,
  mean composite 0.5255, `sprint8-r2`).

Hard invariant outcomes:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0** (Sprint 8 §K0
  hardening guard remains green; Sprint 10 reroute does not stamp
  null UCs; Sprint 11 progressive resolve preserves the active UC
  on same-UC follow-ups).

Regression guard outcomes (all green in the full Sprint 12 server
run):

- Sprint 6 §G0 no ReadTimeout retry
  (`test_agent_client_session_create_timeout.py` 8 / 0).
- Sprint 6 §G2 FAQ-grounded-resolve guard
  (`AgentRunLoopS1FaqGroundedResolveGuardTest` green).
- Sprint 7 / 7.1 candidate_use_cases + intake-state persistence
  (`Sprint7CandidateUseCasesProjectionTest`,
  `Sprint7IntakeStateTest`,
  `Sprint71PartialIntakePersistenceTest` green).
- Sprint 8 §K0 cs259 active-use-case contract hardening
  (`Sprint8Cs259ActiveUseCaseHardeningTest`,
  `Sprint8Cs259EscalateBranchIntegrationTest` green).
- Sprint 8.1 §M3 DISCOVER → RESOLVE phase boundary green.
- Sprint 8.2 §M0a / §M0b green.
- Sprint 9 / 9.1 trace observability + record-outcome honesty
  (`Sprint9TerminalToolHonestyTest`,
  `Sprint9TraceObservabilityFidelityIntegrationTest`,
  `ToolCallTraceSanitizerTest` green).
- Sprint 10 reroute MVP
  (`Sprint10RuntimeIntentClassifierTest` 13 / 0,
  `Sprint10RerouteDecisionTest` 9 / 0).
- Sprint 11 progressive resolve MVP + 11.1 terminal-evidence closure
  (`Sprint11ProgressiveResolveTest` 14 / 0).
- cs014 remains UC-C (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 remains UC-C + `user_distress`.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested`
  (`Cs176ExplicitHumanHelpHandoverIntegrationTest`).
- `RuntimeIntentClassifier` remains runtime-internal (NOT registered
  with the agent-visible tool surface; never exposed via
  `classify_use_case`).

## 7. Residual P0 / P1 blockers and classification

**No new P0 / P1 blockers opened by Sprint 12.** The change is a
state-projection + trace-event hardening + targeted regression
suite; no new tool surfaces, schema changes, or external contract
changes were introduced. Hard invariants remain green.

Residual items, classified per Sprint 12 §N2 contract:

| ID | Item | Classification | Owner | Notes |
|---|---|---|---|---|
| R-cs015-description-keyword | description-keyword moderation cue for forms with no `ad_id` (cs015 / cs095 boundary) | `deferred_scope` | runtime routing | Deferred; preserve cs095 negative guard. Reopen only if a P0 trace appears. |
| R-cs176-UC-I-drift | unjustified UC-I drift on cs_176 r2 | `judge_volatility` / `deferred_scope` | runtime routing | Explicit-human-help → `user_requested` regression already green; the drift is independent and was deferred at Sprint 11 close. |
| R-S3-no-prior-search-guard | refuse `request_handover(faq_miss_threshold_exceeded)` without a prior `search_knowledge` | `deferred_scope` | runtime/tool-use | Not needed for cs259 closure; only reconsider with new evidence. |
| R-S5-Tier2-runtime-guard | runtime guard for Tier-2 policy reasoning | `product_policy_gap` / `deferred_scope` | policy/runtime | Do not implement unless prompt path proves insufficient. |
| R-stall-detector-calibration | cs066 / cs038 stall detector + persona pacing variance | `judge_volatility` | Eval Governance | `STALL_AFTER_TOOL_INTENT` fires on legitimate intake clarification turns. |
| R-L3-relevance-tone | L3 `relevance` and `tone_appropriateness` judges | `judge_volatility` | Eval Governance | flips across runs even on passing cases. |
| R-FAQ-corpus-answerability | cs259 / cs192 / cs095 answerability | `faq_corpus_gap` / `product_policy_gap` | Eval Governance / corpus audit | No resolve-grade article for the user's intent. |
| R-advert-link-product-decision | direct advert URL for `tool_scope_blocked` follow-up | `product_policy_gap` | Product / Policy | Runtime currently routes to handover; awaiting policy. |
| R-rerank-fallback-diagnostics | distinguish `rerank_llm` from `rerank_fallback` score | `runtime_bug` (diagnostic-only) / `deferred_scope` | runtime/observability | Honest rerank attribution; revisit only with a corpus-level rerank investigation. |
| R-task-type-token-naming | `listing_lifecycle_followup` vs `listing_expiry_or_status_followup` | `label_disagreement` | runtime routing | Sprint 11 P2 note carried forward; rename / alias only if downstream consumers require the exact spec token. |
| R-record-outcome-loop | FAQ RESOLVE prompt/tool loop on repeated rejected `record_outcome(resolve)` calls | `runtime_bug` (P2) | runtime | Already covered by `MAX_STEPS → ESCALATE / clarification_budget_exhausted`; reopen only if real traffic shows a budget-exhaustion loop. |
| R-clean-baseline-promote | promote a Sprint-11.x or Sprint-12 smoke baseline once Kimi credentials are clean | `infra` | infra / eval | Pending; no clean run captured this sprint. |
| R-full-issue-ledger | full per-issue ledger / `issues[]` / per-issue budgets / all-UC task taxonomy | `deferred_scope` | none | Sprint 11 carved this out; not reopened. |
| R-skill-runtime-framework | full skill runtime framework + handover payload rewrite | `deferred_scope` | none | Not needed for Sprint 11 / 12; reopen only with a new objective doc. |

Failure-classification distribution for Sprint 12 residuals:

- `runtime_bug` — 2 (R-rerank-fallback-diagnostics,
  R-record-outcome-loop) — both P2; both have non-blocking
  workarounds in place (rerank fallback path is honest if not
  attributed; max-steps guards the record-outcome loop).
- `label_disagreement` — 1 (R-task-type-token-naming) — P2; renames
  are downstream-consumer-driven.
- `faq_corpus_gap` — 1 (subset of R-FAQ-corpus-answerability).
- `product_policy_gap` — 2 (R-FAQ-corpus-answerability,
  R-advert-link-product-decision, R-S5-Tier2-runtime-guard).
- `judge_volatility` — 3 (R-cs176-UC-I-drift,
  R-stall-detector-calibration, R-L3-relevance-tone).
- `persona_drift` — 0 in active backlog.
- `infra` — 1 (R-clean-baseline-promote).
- `deferred_scope` — 7 (R-cs015-description-keyword,
  R-cs176-UC-I-drift secondary, R-S3-no-prior-search-guard,
  R-S5-Tier2-runtime-guard secondary, R-rerank-fallback-diagnostics
  secondary, R-full-issue-ledger, R-skill-runtime-framework).

## 8. Next-phase recommendation

Recommended next phase:

**Eval Governance docs sprint** (or equivalent governance-only
work).

Justification:

- Sprint 10 / 11 / 11.1 closed the runtime-alignment workstream
  for cross-UC reroute and same-UC progressive resolve. No new P0
  / P1 runtime blocker is open at Sprint 12 close.
- Sprint 12 §N0 hardens trace observability so the residual
  judge-volatility / corpus-gap / product-policy items can be
  audited from one trace evidence pass — the canonical Sprint 12
  fields (`predicted_use_case`, `intent_relation`, `reroute_action`,
  `phase_transition_reason`, `resolve_disposition`,
  `terminal_evidence`, `record_outcome_guard_result`,
  `drift_history`, `task_history`) make Eval Governance review
  cheaper without expanding hard gates.
- The largest residual category is `judge_volatility` /
  `faq_corpus_gap` / `product_policy_gap`, all of which belong to
  Eval Governance, not runtime.
- A clean live smoke run under uncontested Kimi credentials should
  be captured during Eval Governance (R-clean-baseline-promote)
  so a Sprint-12-era canonical baseline can replace the
  post-Sprint-8 r1 / r2 runs in `docs/current_eval_baseline.md`
  when it is clean.

Alternative phases ranked:

1. **Eval Governance** — primary recommendation (above).
2. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are blocking the baseline promote; runs
   alongside Eval Governance work.
3. **Release Candidate Hardening** — premature; depends on a
   clean canonical baseline that has not yet been promoted.
4. **Narrow Runtime Follow-up** — only if a real-traffic case
   surfaces a new P0 / P1 runtime blocker (none today).

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 12 explicitly does NOT promote
a new canonical eval baseline; reopen `docs/current_eval_baseline.md`
only when an Eval Governance smoke run produces clean evidence.

## 9. Was Sprint 12 objective met?

Yes:

- N0 drift / task / phase observability hardening implemented as a
  set of backward-compatible `BotSession` transient slots,
  projection fields, and trace-event payload extensions. A reviewer
  reading any single turn's trace evidence can now answer the five
  audit questions enumerated in the objective (stay in current UC,
  soft-shift, risk-shift, stay RESOLVE, allow / reject
  `record_outcome(resolve)`).
- N1 targeted runtime alignment validation suite added — 13
  deterministic tests covering the 10 Sprint 12 spec scenarios plus
  two §N0 projection-surface assertions. No live LLM dependence.
  No CaseSpec / smoke / anchor / promotion gate change.
- N2 residual classification + next-phase decision recorded above
  (this section + §7 + §8). The next-phase recommendation is
  **Eval Governance** with a justified alternatives ranking.

Out-of-scope items (full Issue Ledger, `issues[]`, per-issue
budgets, all-UC task taxonomy, full skill runtime framework,
handover payload rewrite, FAQ corpus changes, judge calibration,
CaseSpec churn, broad prompt rewrite, broad routing taxonomy
rewrite, Eval Governance docs, Release Candidate docs) were NOT
touched.

## 10. Current-doc maintenance rule

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

Sprint 11 / 11.1 archives are at `docs/sprints/sprint-011-*`;
Sprint 12 archives will land at `docs/sprints/sprint-012-*` on
closure.

## 11. Do not reopen

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
- full Issue Ledger / `issues[]` / per-issue budgets / all-UC task
  taxonomy / full skill runtime framework / handover payload rewrite
- runtime sprint unless a new P0 / P1 runtime blocker is found
