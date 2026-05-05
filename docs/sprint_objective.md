# Sprint Objective

Date: 2026-05-06

## Sprint name

Targeted Routing Projection and Intake-State Orchestration Sprint 7

## Goal

Implement the exactly-three-action Sprint 7 scope selected after Sprint 6 closure.

Sprint 7 should improve routing/context projection and intake-state orchestration for the remaining targeted smoke blockers without reopening broad prompt rewrite, broad Java guard, CaseSpec churn, judge calibration, or eval expansion.

Sprint 6 closed with:
- G0 normalized to exactly one ReadTimeout mitigation: 120s create-session timeout widen only
- G1 cs176 explicit-human-help → `user_requested` focused runtime evidence
- G2 S1 FAQ-grounded-resolve PhasePlan / guard implemented
- Codex decision: pass
- blocking_count: 0

## Baseline

Use the Sprint 6 canonical smoke reference:

`eval_interactive/results/20260505-112736/results.json`

Use the Sprint 6 nondeterminism reference:

`eval_interactive/results/20260505-113845/results.json`

Sprint 7 should compare against these references unless credentials or session-level timeouts contaminate the run. If a result is contaminated, do not promote it as canonical; classify the contamination separately.

## Implement exactly these 3 actions

### I0. C5 `candidate_use_cases` projection + DISCOVER cue for `cs_interactive_259`

Implement the deferred C5 projection / DISCOVER cue now that Sprint 6 S1 FAQ-grounded-resolve has landed.

Required behaviour:

- Surface `candidate_use_cases` in the projected context sent to the bot loop.
- Use the existing session / routing state where available; do not create a broad new routing taxonomy.
- Add a narrow DISCOVER-phase instruction cue so that when:
  - `candidate_use_cases` is empty or weak,
  - form context is empty / UNKNOWN-topic,
  - and the current user message is FAQ-shaped or payment-sale-proceeds-shaped,
  the bot should avoid premature generic handover and should gather enough evidence to classify toward the appropriate FAQ-path UC.
- Anchor the cue on the cs259 shape:
  - empty / weak form context
  - user asks how to receive payment when selling an item
  - expected direction is UC-F payment / sale-proceeds FAQ path, not UC-B / UC-J / UC-E drift.
- Add a focused regression test that pins the cs259-shape routing / projection behaviour.
- Preserve Sprint 6 S1:
  - cs259 primary Sprint 6 fix remains S1 FAQ-grounded-resolve
  - S3 no-prior-search guard remains deferred
  - do not reframe cs259 as “no prior search”

Target case:

- `cs_interactive_259`

Acceptance condition:

- cs259 no longer drifts to unrelated UC-B / UC-J / UC-E merely because form context is empty.
- If cs259 still fails due to knowledge-corpus no-hit or answerability, document that separately from routing.
- `CONTRACT_VIOLATION:active_use_case` remains 0.

### I1. C2 UC-FP vs UC-A routing tiebreaker for `cs_interactive_015`

Implement the deferred C2 routing/context tiebreaker for short ad-rejection forms.

Required behaviour:

- First verify whether `customer_context.moderation_status` or equivalent ad moderation / rejection signal is visible to the routing surface.
- If the signal is already visible, add a narrow routing prompt or routing-context cue:
  - short “what happened to my ad?” / “why was my ad removed/rejected?” forms
  - with rejected / removed / disapproved / moderation status
  - should route to UC-FP rather than generic UC-A.
- If the signal is not visible, add the smallest projection needed to make that moderation signal available to routing.
- Preserve negative routing guards:
  - cs095 remains UC-A / not UC-K / not UC-FP when it is an ad visibility or account-email product gap, not a rejected-ad appeal.
  - cs014 remains UC-C.
  - cs066 remains UC-K.
- Do not implement a broad moderation or appeal policy suite.
- Do not change CaseSpecs unless a direct P0 evidence issue is discovered.

Target case:

- `cs_interactive_015`

Acceptance condition:

- cs015 routes toward UC-FP when moderation/rejection context is available.
- The fix must be gated enough not to over-route generic ad visibility / listing visibility questions to UC-FP.

### I2. S2 / C4 intake-state projection + UC-G/H/I/J/K intake skill for `cs_interactive_066`

Implement the deferred intake-state projection and intake PhasePlan / skill-like branch.

Required behaviour:

- Surface an `intake_state` projection for intake-path UCs:
  - `fields_collected`
  - `fields_remaining`
  - required fields for the active UC
  - whether the intake is complete
- Implement the flow as a PhasePlan / skill-like branch inside the existing `PhaseEvaluator` / `AgentRunLoop` architecture.
- Do not introduce a new skill runtime framework.
- For UC-G/H/I/J/K intake phases:
  - ask only for missing required fields
  - avoid repeating already-collected fields
  - do not stamp `intake_complete_for_uc_X` before all required fields are present
  - once all required fields are present, handover with the correct `intake_complete_for_uc_X` reason is allowed
- Preserve allowed-tool enforcement and bounded `maxToolSteps`.
- Add focused regression tests for:
  - cs066 UC-K required-field flow
  - no premature `intake_complete_for_uc_k`
  - completion after required fields are present
  - non-intake FAQ-path UCs unaffected

Target case:

- `cs_interactive_066`

Acceptance condition:

- cs066 should avoid stall / turn-budget variance caused by opaque intake state.
- cs066 remains UC-K.
- `intake_complete_for_uc_k` should only appear when required fields are actually complete.

## Regression guards

- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` remains 0.
- cs014 remains UC-C and approved cs014 override path remains intact.
- cs066 remains UC-K.
- cs095 remains not UC-K and not incorrectly UC-FP.
- cs002 already-escalated distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit human-help → `user_requested` focused regression remains green.
- Sprint 6 G0 closure remains intact:
  - one mitigation only
  - 120s create-session timeout widen
  - no ReadTimeout retry
  - escaped ReadTimeout classified as `INFRA:ReadTimeout`
- Sprint 6 G2 S1 remains intact:
  - FAQ-path viable hits require `resolve_article` before faq-miss handover
  - S3 no-prior-search guard remains deferred

## Do not implement

- Fourth Sprint 7 action
- S3 no-prior-search guard
- S5 Tier-2 runtime guard
- cs176 UC-I / UC-K drift fix unless directly caused by Sprint 7 changes
- broad prompt rewrite
- broad Java guard
- broad routing taxonomy rewrite
- broad moderation / appeal / payment policy suite
- L3 judge calibration
- broad eval expansion
- anchor / exploration / promotion hard-gate expansion
- CaseSpec override changes unless a direct P0 evidence issue is discovered
- qa-report regeneration unrelated to I0 / I1 / I2

## Success metrics

Primary:

- Sprint 7 implements exactly I0 / I1 / I2 and no fourth action.
- cs259 routing improves toward UC-F on the empty-form payment-sale-proceeds FAQ shape, or any remaining failure is clearly classified as knowledge-corpus / answerability rather than routing drift.
- cs015 routes toward UC-FP when moderation/rejection context is visible, without over-routing cs095.
- cs066 intake flow uses projected intake state and avoids premature or missing `intake_complete_for_uc_k`.
- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` remains 0.

Secondary:

- Smoke pass rate should improve or remain stable, but targeted blocker reduction matters more.
- If credentials or upstream LLM latency contaminate smoke, do not promote contaminated smoke as canonical; report targeted results and classify contamination.

## Review rule

Codex must review only whether I0 / I1 / I2 were implemented and whether Sprint 7 remained exactly-three-action scoped.

Codex should not request S3, S5, cs176 drift fix, judge calibration, broad prompt rewrite, broad Java guard, broad routing taxonomy rewrite, broad eval expansion, CaseSpec churn, or QA-report churn unless there is a direct P0/P1 regression caused by Sprint 7.