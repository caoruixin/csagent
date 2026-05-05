# Sprint Objective

Date: 2026-05-05

## Sprint name

Targeted Runtime Latency and FAQ-Resolve Orchestration Sprint 6

## Goal

Implement the exactly-three-action Sprint 6 scope produced by the Sprint 5 fix-layer diagnostic.

Sprint 6 should improve runtime comparability and FAQ-resolution behaviour without reopening deferred prompt/context, CaseSpec, judge, or broad routing work.

Sprint 5 closed with:
- fix-layer taxonomy complete
- prompt/context projection audit complete
- skill orchestration candidates identified
- Java guard / prompt / skill / eval responsibility split documented
- Codex decision: pass
- blocking_count: 0

## Baseline

Use the post-Sprint-4 canonical baseline:

`eval_interactive/results/20260504-221916/results.json`

Use this nondeterminism reference:

`eval_interactive/results/20260504-223153/results.json`

These remain authoritative because Sprint 5 was diagnostic-only.

## Implement exactly these 3 actions

### G0. Kimi `session_create_failed: ReadTimeout` mitigation

Reduce smoke-side session-create ReadTimeout failures without changing semantic runtime behaviour.

Required behaviour:

- Pick one narrow mitigation:
  - widen eval-client timeout to 120s, OR
  - pre-warm the first Kimi call, OR
  - async pre-fetch FAQ snapshots, OR
  - accept-and-retry on ReadTimeout.
- Keep the change scoped to runtime latency / eval-client reliability.
- Preserve existing C0/C1 auth and retry semantics:
  - auth failures such as 401 / 403 remain non-retryable
  - transient timeout / transport / 429 / 5xx handling remains bounded
  - no secret logging
- Make ReadTimeout evidence easier to separate from semantic failures.

Target cases:

- `cs_interactive_002`
- `cs_interactive_014`
- `cs_interactive_015`
- `cs_interactive_192`
- `cs_interactive_259`

### G1. Corrected C1 for `cs_interactive_176`, targeting `user_requested`

Add a narrow active_use_case-aware `request_handover` prompt change.

Required behaviour:

- Update the system prompt so that when the user explicitly asks for human help, the bot preserves / produces `escalation_reason=user_requested`.
- This is required even if the same conversation contains payment-keyword phrasing.
- `faq_miss_threshold_exceeded`, `intake_complete_for_uc_k`, `service_degraded`, and `payment_dispute_detected` are NOT acceptable substitutes for `user_requested`.
- The prompt must not suppress genuine payment dispute / chargeback / GDPR / identity / appeal cases.
- Add a golden prompt snapshot test or equivalent minimal regression check.
- Add targeted runtime/eval evidence for cs176.

Target case:

- `cs_interactive_176`

Acceptance condition:

- cs176 should not fail by stamping `payment_dispute_detected` or `service_degraded` when the transcript contains explicit human-help request evidence.
- Sprint 6 must either:
  - prevent unjustified `active_use_case=UC-I` drift on cs176, OR
  - explicitly document UC-I drift as deferred residual risk in `docs/10-handoff.md`.

### G2. S1 FAQ-grounded-resolve PhasePlan / skill

Implement the S1 FAQ-grounded-resolve flow as a parametrized PhasePlan / skill-like branch. Do not introduce a new skill runtime framework.

Required behaviour:

- For FAQ-path UCs in RESOLVE, the bot must not complete a factual customer-facing answer without grounded knowledge evidence.
- Support both failure shapes:
  1. cs192 shape: answer emitted without citation / resolve sequence incomplete.
  2. cs259 shape: `search_knowledge` happened, but `resolve_article` / grounded answer / `record_outcome` did not complete.
- The intended terminal sequence is:

  `search_knowledge → resolve_article → grounded customer-facing answer → record_outcome`

  or explicit handover only after a valid resolve attempt cannot complete.

- If `search_knowledge` returns no viable hit, handover with `faq_miss_threshold_exceeded` is allowed.
- If `search_knowledge` returns viable evidence, do not short-circuit directly to `request_handover(faq_miss_threshold_exceeded)` before attempting `resolve_article`.
- Preserve allowed-tool enforcement and bounded maxToolSteps.
- Add focused regression tests for both cs192-style and cs259-style flows.

Target cases:

- `cs_interactive_192`
- `cs_interactive_259`

## Regression guards

- `L1:escalation_reason_consistency` remains 0.
- cs014 remains UC-C and approved cs014 override path remains intact.
- cs066 remains UC-K.
- cs095 remains not UC-K.
- cs002 already-escalated distress reconciliation remains green.
- cs029 remains UC-D + `user_requested` and avoids the original active_use_case contract violation.
- Sprint 5 diagnostic guidance remains intact:
  - C5 is deferred
  - S3 no-prior-search guard is deferred
  - cs259 primary fix is S1, not C5/S3

## Do not implement

- C5 `candidate_use_cases` projection + DISCOVER cue
- S3 no-prior-search guard
- cs015 UC-FP / UC-A tiebreaker
- S2 / C4 intake-state projection + intake skill
- S5 Tier-2 runtime guard
- L3 judge calibration
- broad prompt rewrite
- broad Java guard
- broad routing taxonomy rewrite
- broad eval expansion
- anchor / exploration / promotion hard-gate expansion
- CaseSpec override changes unless a P0 evidence issue is discovered
- qa-report regeneration unrelated to these three actions

## Success metrics

Primary:

- Sprint 6 implements exactly G0 / G1 / G2 and no fourth action.
- ReadTimeout / session_create_failed incidence improves on targeted cases or is clearly classified as external credential/quota contamination.
- cs176 preserves / produces `user_requested` when the user explicitly asks for human help.
- cs192 no longer emits an uncited factual FAQ answer.
- cs259 no longer stops after `search_knowledge` without completing `resolve_article` / grounded answer / `record_outcome`, unless a valid no-answer handover path is reached.
- `L1:escalation_reason_consistency` remains 0.

Secondary:

- Smoke pass rate should improve or remain stable, but targeted blocker reduction matters more.
- If smoke credentials are contaminated, do not use contaminated smoke as canonical; report targeted results and classify contamination.

## Review rule

Codex must review only whether G0 / G1 / G2 were implemented and whether Sprint 6 remained exactly-three-action scoped.

Codex should not request C5, S3, cs015, S2, S5, judge calibration, broad prompt rewrite, broad Java guard, broad eval expansion, or CaseSpec churn unless there is a direct P0/P1 regression caused by Sprint 6.