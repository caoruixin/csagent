# Current Handoff

Date: 2026-05-08
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 11 — Progressive Resolve MVP (in flight; awaiting Codex review).

Latest closed sprint:
Sprint 10 — Runtime Re-route MVP
(archived under `docs/sprints/sprint-010-*`).

## 2. Sprint 11 goal

Sprint 11 implements same-UC progressive resolve on top of the Sprint 10
runtime reroute layer. Sprint 10 decides whether a user turn stays in
the current issue, soft-shifts to a new UC, risk-shifts to intake, or
escalates BEFORE `PhaseEvaluator.plan(...)`.

Sprint 11 covers exactly the same-UC continuation path: when the user
remains inside the same UC but asks a new related subtask, provides a
slot, asks for a listing-specific follow-up, or asks for human help
after multiple resolve steps.

Scope is exactly the three Sprint 11 actions M0 / M1 / M2; nothing
outside the do-not-implement list was opened (no full Issue Ledger, no
per-issue budgets, no all-UC task taxonomy, no full skill runtime, no
handover payload rewrite, no FAQ / corpus / judge / CaseSpec changes).

## 3. Sprint 11 implementation

### M0 — Minimal same-UC task / entity state

`server/src/main/java/com/gumtree/csagent/model/BotSession.java`
- Two NEW `@Transient` fields appended after the Sprint 10 §L2 slots:
  - `taskStatus` — progressive-resolve checkpoint token
    (`in_progress / asked_for_slot / answered_subtask /
    ready_to_confirm / escalate`).
  - `lastEntityContextRef` — short observability pointer for where
    the live `primary_entity` originated
    (`form_context.ad_id` for cold start, `user_message.ad_id` for a
    Sprint 11 runtime capture).
- Sprint 10 §L2 fields (`previousActiveUseCase`, `driftType`,
  `currentTaskType`, `primaryEntityType`, `primaryEntityValue`,
  `issueStatusSummary`) kept verbatim.

`server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java`
- Added two narrow regex patterns to extract an `ad_id` from the user's
  chat message:
  - `AD_ID_LABELED_PATTERN` for `ad id: 12345`, `advert id 12345`,
    `listing id 12345`, etc.
  - `AD_ID_FROM_USER_MESSAGE_PATTERN` for bare numeric ad IDs
    (`\\d{10,}` — same shape used by `PiiRedactionFilter`) and dashed
    `AD-1234` variants.
- New helper `extractAdIdFromUserMessage(String)` (visible for tests)
  returns the labeled match first, then falls back to the bare
  numeric / dashed shape. Tolerant on null / blank input.
- New public method `captureSameUcAdIdHint(BotSession, String)` —
  returns an ad_id only when the active UC is `UC-A` (the FAQ-class
  UC where progressive resolve currently applies). Used by
  `ControlKernel.applyRerouteDecision` for `CONTINUE_CURRENT` turns.
- `preferAdId(fromForm, fromUserMessage)` helper merges sources so a
  user-supplied ID wins over a stale form value during the matching
  Sprint 10 MVP shapes.

`server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
- `applyRerouteDecision` now:
  1. After the Sprint 10 §L1 mutation, calls
     `runtimeIntentClassifier.captureSameUcAdIdHint(...)` for
     `CONTINUE_CURRENT` turns so a same-UC user reply that is just a
     numeric advert ID still stamps `primary_entity_type=listing /
     primary_entity_value=<ad_id>`.
  2. Promotes the captured ad_id into `taskType` /
     `primaryEntityType` / `primaryEntityValue` when the classifier
     itself returned no entity.
  3. Stamps `lastEntityContextRef = "user_message.ad_id"` on a
     runtime capture and `"form_context.ad_id"` otherwise (null when
     no entity is in scope).
  4. Calls a new helper `persistAdIdIntoFormContext(session, adId)`
     which writes the captured ad_id into `session.formContext` only
     when the form context did not already carry the value. This
     keeps customer-supplied form data authoritative and lets the
     Sprint 10 form-context fast path see the ID on the next turn.
- `deriveDriftTypeToken(action, relation)` now also surfaces
  `SAME_ISSUE` / `SAME_UC_NEW_TASK` on `CONTINUE_CURRENT` turns so the
  progressive same-UC follow-up signal stays visible in the projection
  even when the session is already in RESOLVE (the decider returns
  `CONTINUE_CURRENT` because no phase change is needed).

`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
- Two NEW projection slots emitted right after the Sprint 10 §L2 block:
  - `task_status` (defaults to `"in_progress"` when the session has
    none yet, so the projection shape is stable across turns).
  - `last_entity_context_ref` (JSON `null` when no entity is in
    scope).

### M1 — `ResolveDisposition` and transition guard

`server/src/main/java/com/gumtree/csagent/model/ResolveDisposition.java` (new)
- Enum with the exact five values pinned by the Sprint 11 spec:
  `CONTINUE_RESOLVE / ASKED_FOR_SLOT / ANSWERED_SUBTASK /
  READY_TO_CONFIRM / ESCALATE`.
- Helper `toTaskStatusToken()` maps each value to the canonical
  `task_status` projection token.

`server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` (new)
- Pure helper used by `PhaseEvaluator.mapFinalAnswer` and the
  `AgentRunLoopImpl` record-outcome guard. Tolerant on null / blank
  inputs (defaults to `CONTINUE_RESOLVE`). RESOLVE / FAQ plans only —
  INTAKE plans are excluded.
- `evaluate(plan, AgentRunResult)` order:
  1. `ESCALATE` outcome → `ESCALATE`.
  2. `CLARIFICATION_NEEDED` outcome → `ASKED_FOR_SLOT`.
  3. `FINAL_ANSWER` outcome:
     - bot text is a `?`-suffix question, matches the clarifying
       phrase pattern, or matches the new
       `SOFT_NEXT_STEP_PATTERN` ("send the advert ID", "share the ad
       id", "if you can share", "let me know", etc.) →
       `ASKED_FOR_SLOT`.
     - otherwise default → `READY_TO_CONFIRM` (preserves the legacy
       FAQ FINAL_ANSWER → CONFIRM behaviour for normal grounded
       answers).
  4. Anything else → `CONTINUE_RESOLVE`.
- `shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)`
  — true only when the plan is RESOLVE / FAQ, the outcome class is
  `resolve` / `resolved`, and the phase is neither `CONFIRM` nor
  `CLOSE`. Other outcome classes / CONFIRM / CLOSE plans pass through.

`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
- `mapFinalAnswer` RESOLVE branch now consults
  `ResolveDispositionEvaluator.evaluate(plan, result)`:
  - `ASKED_FOR_SLOT` / `ANSWERED_SUBTASK` / `CONTINUE_RESOLVE` →
    stay in `RESOLVE` with `transitionReason="progressive_resolve_stay"`.
  - `ESCALATE` → `ESCALATE` with `service_degraded`.
  - `READY_TO_CONFIRM` → `CONFIRM` with `answer_provided`
    (legacy contract for normal FAQ-grounded answers).
- Sprint 9 §O1 `recordOutcomeAttemptedAndFailed(...)` retry guard runs
  BEFORE the disposition lookup — failed `record_outcome` dispatches
  still keep the session in RESOLVE with
  `transitionReason="record_outcome_failed_retry"`.
- Sprint 11 §M0 — populates `session.taskStatus` with
  `disposition.toTaskStatusToken()` so the projection / trace
  observability surfaces the progressive-resolve checkpoint.

`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
- New constant `RECORD_OUTCOME_TOOL = "record_outcome"` and
  `PROGRESSIVE_RESOLVE_GUARD_REJECT_REASON =
  "progressive_resolve_record_outcome_premature"`.
- New step `6a''` in the dispatch loop, BEFORE the Sprint 6 §G2 S1
  guard: refuses `record_outcome(outcome_class=resolve)` on a RESOLVE /
  FAQ plan when the deterministic terminal condition is not satisfied.
  Records a rejection `ToolEvent` and surfaces a hint in
  `accumulated_tool_results.record_outcome` so the next LLM iteration
  can either ask the user to confirm or call `request_handover` /
  another tool. Other outcome classes (escalate / abandon) and CONFIRM
  / CLOSE plans pass through.
- New helper `shouldRejectPrematureResolveOutcome(plan, session, call)`
  delegates to `ResolveDispositionEvaluator`.

### M2 — Progressive UC-A / UC-C regression suite

`server/src/test/java/com/gumtree/csagent/service/runtime/Sprint11ProgressiveResolveTest.java` (new)
- 11 focused tests covering the four Sprint 11 spec groups:
  1. **Progressive UC-A flow.**
     - `test1a` — soft FAQ-grounded UC-A answer asking for the advert
       ID maps to `ASKED_FOR_SLOT` (stays RESOLVE).
     - `test1b` — user replies with a bare ad_id; the kernel stamps
       `primary_entity_type=listing` /
       `primary_entity_value=<ad_id>` /
       `last_entity_context_ref=user_message.ad_id` AND persists the
       ad_id into `form_context` for cross-turn reuse.
     - `test1c` — same-UC follow-up "How long is it active for?"
       reuses the form-context ad_id, stays UC-A / RESOLVE,
       `current_task_type=listing_lifecycle_followup`,
       `drift_type=SAME_UC_NEW_TASK`.
     - `test1d` — explicit human request still surfaces
       `IntentRelation.HUMAN_REQUEST` and the resolver continues to
       flag explicit-human so kernel step 2.5 fires
       `user_requested` before the planner runs.
  2. **UC-C same-UC follow-up.**
     - `test2` — UC-C / RESOLVE + "I haven't got replies" stays
       UC-C / RESOLVE; no soft shift, no generic handover.
  3. **Record-outcome guard.**
     - `test3a` — RESOLVE / FAQ + `record_outcome(resolve)` is
       rejected when the session is not in CONFIRM / CLOSE.
     - `test3b` — CONFIRM plan + `record_outcome(resolve)` passes
       through.
     - `test3c` — successful `record_outcome` dispatch on this run
       earns `READY_TO_CONFIRM` (deterministic terminal condition).
     - `test3d` — soft "send the advert ID" answer maps to
       `ASKED_FOR_SLOT` / `ANSWERED_SUBTASK`, never
       `READY_TO_CONFIRM`.
  4. **Projection snapshot.**
     - `test4` — Sprint 10 §L2 fields remain intact AND Sprint 11
       §M0 fields (`task_status`, `last_entity_context_ref`) are
       surfaced.
  5. **PhaseEvaluator wiring regression.**
     - `test5` — `interpretRunResult(plan, softAnswer, session)`
       returns `nextPhase=RESOLVE` for a soft progressive-resolve
       answer and writes `session.taskStatus` via the disposition
       evaluator.

## 4. Files changed (Sprint 11)

Production:
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  (+ `taskStatus`, `lastEntityContextRef` `@Transient` fields).
- `server/src/main/java/com/gumtree/csagent/model/ResolveDisposition.java` (new).
- `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java`
  (+ ad_id user-message extraction, `captureSameUcAdIdHint`,
  `preferAdId`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java`
  (new).
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  (RESOLVE FINAL_ANSWER consults the disposition evaluator and
  populates `session.taskStatus`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  (+ record-outcome guard at dispatch step 6a'').
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (same-UC ad_id capture + `persistAdIdIntoFormContext` helper +
  `deriveDriftTypeToken` extension for `CONTINUE_CURRENT` same-UC
  relations).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  (+ `task_status` and `last_entity_context_ref` projection slots).

Tests (new):
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint11ProgressiveResolveTest.java`
  (11 tests, all green).

Docs:
- `docs/10-handoff.md` (this file; Sprint 10 closure archived to
  `docs/sprints/sprint-010-handoff-closure.md`).
- `docs/action_bank.md` (Sprint 11 row added; Sprint 10 entry moved
  to closed-action index).
- `docs/sprint_objective.md` archived to
  `docs/sprints/sprint-011-progressive-resolve-mvp-objective.md`
  (current `docs/sprint_objective.md` retained).

## 5. Tests run

- `mvn -pl server test` → **793 / 0 / 0 / 0** (was 782 pre-Sprint-11;
  +11 new Sprint-11 tests).
- Targeted Sprint-7 / 8 / 9 / 10 / 11 + Cs014/Cs066/Cs095/Cs002/
  Cs029/Cs176/Cs001 regression sweep
  (`Sprint7*Test, Sprint8*Test, Sprint9*Test, Sprint10*Test,
  Sprint11*Test, Sprint71*Test, Sprint81*Test,
  Cs014RouteAndDistressRegressionTest,
  Cs014RouteAndLoopHandoverIntegrationTest,
  Cs176ExplicitHumanHelpHandoverIntegrationTest,
  Cs002AlreadyEscalatedDistressReconcileIntegrationTest,
  Cs001LlmDistressGateIntegrationTest,
  EscalationReason*Test`) → **218 / 0 / 0 / 0**.
- `python -m pytest -p no:capture eval_interactive/tests/`
  → **294 / 0** (full Python eval test suite, including
  `test_agent_client_session_create_timeout.py` for the Sprint 6 §G0
  ReadTimeout no-retry contract, plus all `regression/`, `scoring/`,
  and `trace/` packages).

Smoke runs were NOT executed for Sprint 11 — the change is a
state-projection + transition-guard layer that does not affect FAQ
corpus, judges, CaseSpec, or LLM credentials. Live smoke is
recommended ONLY when Codex explicitly requests it; otherwise the
focused JUnit + Python regression suite is the canonical Sprint 11
evidence.

## 6. Result paths

No new smoke run was promoted in Sprint 11 (no FAQ / corpus / judge
change). The current canonical eval baseline remains the post-
Sprint-8 r1 run documented in `docs/current_eval_baseline.md`:

- `eval_interactive/results/20260505-234448/results.json` (8/14,
  mean composite 0.4784, `sprint8-r1`).
- `eval_interactive/results/20260505-235231/results.json` (9/14,
  mean composite 0.5255, `sprint8-r2`).

When a clean Sprint-11 smoke run is later captured under clean
Kimi credentials, promote it only if:

- `L1:escalation_reason_consistency = 0`
- `CONTRACT_VIOLATION:active_use_case = 0`
- the Sprint 10 reroute blockers (cs014 / cs066 / cs095 / cs176 /
  cs002 / cs029) remain green or stable.
- the new Sprint 11 progressive-resolve guards (no premature
  record_outcome, soft answers stay in RESOLVE) do not regress.

## 7. Progressive UC-A / UC-C outcomes — before vs after

**Pre-Sprint-11 progressive UC-A flow.**
```
Turn 1: "How do I find my ad?"
DriftDetector            -> NONE
RuntimeIntentClassifier  -> UNKNOWN
RerouteDecider           -> CONTINUE_CURRENT
PhaseEvaluator.plan      -> RESOLVE_FAQ for UC-A
AgentRunLoop             -> grounded answer "Here is how to find your ad;
                            send the advert ID if you want me to check it."
PhaseEvaluator           -> mapFinalAnswer(RESOLVE) -> CONFIRM
                            (unconditional FAQ-FINAL_ANSWER -> CONFIRM)
ControlKernel            -> session.currentPhase=CONFIRM,
                            transitionReason=answer_provided
                            -> next user turn might emit
                            record_outcome(resolve) without the user ever
                            confirming the soft answer.
```

**Post-Sprint-11 progressive UC-A flow.**
```
Turn 1: "How do I find my ad?"
DriftDetector            -> NONE
RuntimeIntentClassifier  -> UNKNOWN
RerouteDecider           -> CONTINUE_CURRENT
applyRerouteDecision     -> taskStatus=null (set by disposition evaluator
                            after the run loop), driftType=null,
                            primary_entity null (no ad_id yet).
PhaseEvaluator.plan      -> RESOLVE_FAQ for UC-A
AgentRunLoop             -> grounded answer "Here is how to find your ad;
                            send the advert ID if you want me to check it."
ResolveDispositionEval   -> ASKED_FOR_SLOT (soft next-step pattern matched)
PhaseEvaluator           -> mapFinalAnswer(RESOLVE) -> RESOLVE
                            (transitionReason=progressive_resolve_stay)
ControlKernel            -> session.currentPhase=RESOLVE,
                            session.taskStatus="asked_for_slot".

Turn 2: "1234567890"
RuntimeIntentClassifier  -> UNKNOWN (no MVP shape)
RerouteDecider           -> CONTINUE_CURRENT
applyRerouteDecision     -> captureSameUcAdIdHint stamps
                            primary_entity_type=listing,
                            primary_entity_value=1234567890,
                            last_entity_context_ref=user_message.ad_id,
                            current_task_type=listing_visibility_diagnostic.
                            Form context updated with ad_id.

Turn 3: "How long is it active for?"
RuntimeIntentClassifier  -> SAME_UC_NEW_TASK, predictedUc=UC-A,
                            taskType=listing_lifecycle_followup,
                            primary_entity reused from form_context.
RerouteDecider           -> CONTINUE_CURRENT (already in RESOLVE)
applyRerouteDecision     -> drift_type=SAME_UC_NEW_TASK,
                            current_task_type=listing_lifecycle_followup,
                            primary_entity preserved.
PhaseEvaluator.plan      -> RESOLVE_FAQ for UC-A (ad_id available in
                            projection so the LLM does not re-ask).

Turn 4: "I want a human"
EscalationReasonResolver -> detectExplicitUserEscalation == true
ControlKernel step 2.5   -> applyEscalationReason("user_requested");
                            forceEscalate. Terminal ESCALATE with
                            user_requested.
```

**UC-C same-UC follow-up.** When the session is already on UC-C and
the user says "I haven't got replies" again, the classifier returns
`SAME_ISSUE` and the decider's `CONTINUE_CURRENT` (already in
RESOLVE) keeps the session UC-C / RESOLVE. The Sprint 11
`drift_type=SAME_ISSUE` projection slot now stays visible across
those turns instead of becoming `null`.

## 8. Record-outcome guard outcome

- `record_outcome(outcome_class=resolve)` on a RESOLVE / FAQ plan is
  rejected when the session is not in CONFIRM / CLOSE. The rejection
  is recorded as a tool event with reason
  `progressive_resolve_record_outcome_premature` and a hint surfaces
  in `accumulated_tool_results.record_outcome.error` so the next LLM
  iteration can adjust.
- Other outcome classes (escalate / abandon) and CONFIRM / CLOSE
  plans pass through unchanged.
- The legacy Sprint 9 §O1 record-outcome failure-retry path is
  preserved: a failed `record_outcome` dispatch in RESOLVE keeps the
  session in RESOLVE via
  `PhaseEvaluator.recordOutcomeAttemptedAndFailed`.
- A successful `record_outcome` dispatch within the same run is
  treated as the deterministic terminal condition by
  `ResolveDispositionEvaluator.evaluate(...)` and earns
  `READY_TO_CONFIRM` → CONFIRM.

## 9. Regression guard outcomes

Active guards (all green in the full Sprint-11 server run):

- `L1:escalation_reason_consistency` = 0 (never re-introduced)
- `CONTRACT_VIOLATION:active_use_case` = 0
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest` green;
  `Cs014RouteAndDistressRegressionTest` green)
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
  (`Sprint7CandidateUseCasesProjectionTest, Sprint7IntakeStateTest`
  green)
- Sprint 7.1 §J0 partial intake persistence
  (`Sprint71PartialIntakePersistenceTest` green)
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
- Sprint 10 §L0 / §L1 / §L2 reroute MVP
  (`Sprint10RuntimeIntentClassifierTest` 13 / 0,
  `Sprint10RerouteDecisionTest` 9 / 0).
- `RuntimeIntentClassifier` remains runtime-internal (NOT registered
  with the agent-visible tool surface; never exposed via
  `classify_use_case`).

New Sprint 11 guards:

- Sprint 11 §M0 — minimal same-UC task / entity state surfaces
  `current_task_type`, `task_status`, `primary_entity`,
  `issue_status_summary`, `last_entity_context_ref` alongside the
  Sprint 10 §L2 slots (`Sprint11ProgressiveResolveTest.test4`).
- Sprint 11 §M0 — same-UC ad_id capture: a bare numeric ad_id reply
  on a UC-A / RESOLVE turn stamps `primary_entity` AND persists the
  ad_id into `form_context` for cross-turn reuse
  (`Sprint11ProgressiveResolveTest.test1b / test1c`).
- Sprint 11 §M1 — `ResolveDisposition` runtime checkpoint:
  `ASKED_FOR_SLOT` / `ANSWERED_SUBTASK` keep RESOLVE,
  `READY_TO_CONFIRM` (successful `record_outcome` dispatch) earns
  CONFIRM, `ESCALATE` escalates
  (`Sprint11ProgressiveResolveTest.test1a / test3c / test3d`).
- Sprint 11 §M1 — record-outcome guard refuses
  `record_outcome(resolve)` on RESOLVE / FAQ when the deterministic
  terminal condition is unmet
  (`Sprint11ProgressiveResolveTest.test3a / test3b`).
- Sprint 11 §M2 — UC-C same-UC follow-up stays UC-C / RESOLVE; no
  unnecessary soft shift, no generic handover
  (`Sprint11ProgressiveResolveTest.test2`).
- Sprint 11 §M2 — `PhaseEvaluator.interpretRunResult` consults the
  disposition evaluator on RESOLVE / FAQ FINAL_ANSWER and writes
  `session.taskStatus`
  (`Sprint11ProgressiveResolveTest.test5`).

## 10. Remaining P0 / P1 blockers

- **None opened by Sprint 11.** The change is a state-projection +
  transition-guard layer; it does not introduce new tool surfaces,
  schema changes, or external contract changes.
- The Eval Governance backlog (cs015 / cs066 / cs176 deferrals,
  L3 judge volatility, FAQ corpus answerability, advert-link
  product policy, rerank fallback diagnostics) is unchanged and
  remains under `docs/action_bank.md` §4 / §5 as deferred /
  governance work.
- Live re-probe of the Sprint-11 progressive-resolve flow against a
  Kimi-backed deploy is recommended once Codex passes Sprint 11 — any
  residual must be classified as "downstream LLM tool-use variance"
  rather than a Sprint 11 regression.

## 11. Was Sprint 11 objective met?

Yes:

- M0 minimal same-UC task / entity state implemented. The Sprint 10
  §L2 transient slots are reused; Sprint 11 adds `taskStatus` and
  `lastEntityContextRef`. `primary_entity` is captured from the user
  message on UC-A turns and persisted into `form_context` for
  cross-turn reuse. Active UC is preserved on same-UC follow-up. No
  full `issues[]` ledger, per-issue budgets, or all-UC task taxonomy
  was opened.
- M1 `ResolveDisposition` enum + `ResolveDispositionEvaluator`
  implemented. RESOLVE / FAQ FINAL_ANSWER no longer unconditionally
  transitions to CONFIRM; a soft next-step / clarifying answer stays
  in RESOLVE. `record_outcome(resolve)` on a single factual answer is
  rejected before the close phase / user confirmation. Existing
  CONFIRM / CLOSE / human-request transitions are preserved.
- M2 progressive UC-A / UC-C regression suite added (11 focused
  tests, all green). Sprint 10 reroute regression tests remain green
  (`Sprint10RuntimeIntentClassifierTest` 13 / 0,
  `Sprint10RerouteDecisionTest` 9 / 0). `mvn -pl server test` =
  793 / 0 / 0 / 0 (was 782 pre-Sprint-11). `python -m pytest -p
  no:capture eval_interactive/tests/` = 294 / 0.

Out-of-scope items (cross-UC router expansion beyond Sprint 10, full
Issue Ledger, `issues[]`, per-issue budgets, all-UC task taxonomy,
full skill runtime framework, handover payload rewrite, FAQ corpus
changes, judge calibration, CaseSpec churn, broad prompt rewrite,
broad routing taxonomy rewrite, Eval Governance docs) were NOT
touched.

## 12. Next recommended phase

If Codex passes Sprint 11 with `decision: pass` /
`blocking_count: 0`, the recommended next phase is:

**closure or Eval Governance docs sprint.**

The Sprint 11 progressive-resolve MVP closes the same-UC continuation
gap that Sprint 10 explicitly carved out. Further runtime sprints
(full Issue Ledger, per-issue budgets, all-UC task taxonomy, full
skill runtime framework, handover payload rewrite) remain on the
deferred / avoid list under `docs/action_bank.md` §4 unless a new
P0/P1 runtime blocker is found. Eval Governance docs-only work
remains a parallel option.

Do not start another runtime sprint unless triage finds a new
P0/P1 runtime blocker (none identified at Sprint 11 close).

## 13. Current-doc maintenance rule

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

## 14. Do not reopen

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
- runtime sprint unless a new P0/P1 runtime blocker is found
