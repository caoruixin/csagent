# Sprint Objective

Date: 2026-05-06

## Sprint name

Targeted cs259 Active-Use-Case Contract Hardening Sprint 8

## Goal

Close the single Post-Sprint-7 clean-validation P1 blocker: a non-contaminated `CONTRACT_VIOLATION:active_use_case` on `cs_interactive_259` in clean smoke r2.

Sprint 8 must stay narrowly focused on active-use-case contract stability for the cs259 UNKNOWN-topic / empty-form payment-sale-proceeds path. It must not reopen broad routing, FAQ corpus, judge calibration, CaseSpec churn, or Eval Governance work.

## Baseline

Use Sprint 6 r1 as the current canonical baseline:

`eval_interactive/results/20260505-112736/results.json`

Use Post-Sprint-7 clean validation as diagnostic evidence only:

- clean r1: `eval_interactive/results/20260505-224809/results.json`
- clean r2: `eval_interactive/results/20260505-225708/results.json`

Do not promote the Post-Sprint-7 clean results as canonical until the active-use-case contract violation is resolved.

## Implement exactly this 1 action

### K0. cs259 active-use-case contract hardening

Fix or revalidate the cs259 path so the bot commits a valid `active_use_case` before contract evaluation on the UNKNOWN-topic / empty-form payment-sale-proceeds shape.

Required behaviour:

- Target the exact failure shape:
  - case: `cs_interactive_259`
  - clean smoke r2 result: `eval_interactive/results/20260505-225708/results.json`
  - failure: `CONTRACT_VIOLATION:active_use_case`
  - status: clean, non-contaminated; no auth / endpoint / quota / ReadTimeout / session_create_failed / session-level timeout contamination
- Do not classify this failure as infra contamination.
- Do not report `CONTRACT_VIOLATION:active_use_case` guard as green until clean reruns show 0 contract violations.
- Preserve Sprint 7 I0 behaviour:
  - cs259 should route toward UC-F on the payment / sale-proceeds FAQ shape.
  - `candidate_use_cases` projection remains available.
  - DISCOVER cue remains narrow.
- If the LLM fails to call `classify_use_case` on this shape, add the narrowest runtime hardening needed so a valid active UC is committed before handover / contract evaluation.
- Prefer a deterministic fallback only for the cs259-like payment-sale-proceeds FAQ shape:
  - UNKNOWN or empty form context
  - user asks how to receive payment / get paid / sale proceeds after selling an item
  - no active_use_case committed before escalation or handover
  - fallback should be UC-F
- Do not add a broad routing taxonomy rewrite.
- Do not add S3 no-prior-search guard.
- Do not change FAQ corpus or expected outcome.
- Do not change CaseSpecs or overrides.

Acceptance condition:

- Targeted cs259 run has no `CONTRACT_VIOLATION:active_use_case`.
- Two clean smoke runs have:
  - 0 `CONTRACT_VIOLATION:active_use_case`
  - 0 ReadTimeout / `INFRA:ReadTimeout` / `session_create_failed`
  - cs259 either:
    - commits UC-F and then fails only as FAQ corpus / answerability, or
    - has any residual failure clearly classified as non-contract and non-infra.
- `L1:escalation_reason_consistency` remains 0.

## Regression guards

- `L1:escalation_reason_consistency` remains 0.
- `CONTRACT_VIOLATION:active_use_case` is 0 across two clean smoke runs.
- cs259 targeted path commits a valid UC, preferably UC-F.
- cs014 remains UC-C.
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 remains UC-C + `user_distress`.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` focused regression remains green.
- Sprint 6 G0 remains intact:
  - 120s create-session timeout widen only
  - no ReadTimeout retry
- Sprint 6 G2 S1 remains intact:
  - viable FAQ hits require `resolve_article` before faq-miss handover.
- Sprint 7 I1 / I2 remain intact:
  - cs015 tiebreaker tests remain green
  - cs066 intake-state persistence tests remain green

## Do not implement

- second Sprint 8 action
- cs015 description-keyword moderation cue
- cs066 / cs038 stall detector calibration
- FAQ corpus expansion
- product policy changes
- cs176 UC-I drift fix
- S3 no-prior-search guard
- S5 Tier-2 runtime guard
- broad prompt rewrite
- broad Java guard
- broad routing taxonomy rewrite
- judge calibration
- Eval Governance docs
- CaseSpec override changes
- anchor / exploration / promotion hard-gate expansion

## Success metrics

Primary:

- The clean validation P1 blocker is closed.
- `CONTRACT_VIOLATION:active_use_case` is 0 across targeted cs259 and two clean smoke runs.
- No new P0/P1 regression is introduced.
- No fourth / unrelated scope is introduced.

Secondary:

- Smoke pass rate may stay flat; targeted contract stability matters more.
- If cs259 remains non-passing only because of FAQ corpus / answerability, classify it as such and do not fix it in Sprint 8.

## Review rule

Codex must review only whether K0 closes the cs259 active-use-case contract blocker and whether Sprint 8 stayed single-action scoped.

Codex should not request cs015, cs066 stall calibration, FAQ corpus, product policy, judge calibration, broad routing rewrite, CaseSpec churn, or Eval Governance unless Sprint 8 directly regresses them.