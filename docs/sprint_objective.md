# Sprint Objective

Date: 2026-05-04

## Sprint name

Targeted Routing and Escalation Stability Sprint 2

## Goal

Stabilize the remaining high-impact runtime failures after Sprint 1.

Sprint 1 fixed escalation reason consistency, UC-K technical-regression routing, and deterministic handover payload assembly.

Sprint 2 should improve routing and escalation stability without expanding the eval scope.

## Baseline

Use this as the current sprint baseline:

`eval_interactive/results/20260504-100538/results.json`

Use these only as nondeterminism references:

- `eval_interactive/results/20260504-101453/results.json`
- `eval_interactive/results/20260504-102131/results.json`

Use this only as pre-Sprint-1 reference:

`eval_interactive/results/20260504-085942/results.json`

## Implement only

### B0. Request-handover persisted reason normalization

Before persisting AgentRunLoop `request_handover` tool-call arguments, rewrite every `request_handover.arguments.escalation_reason` to the resolved canonical `session.escalationReason`.

Required behavior:

- LLM-emitted non-canonical reasons such as `user_requested_escalation` must persist as canonical `user_requested`.
- LLM-emitted lower-priority reasons must not disagree with resolved session reason.
- Trace tool call, session state, and handover payload must remain consistent.

### B1. Distress / frustration detector

Add a deterministic detector that stamps `user_distress` before FAQ-miss or budget reasons win.

Target signals:

- repeated complaints
- ALL-CAPS frustration
- "you are not helping"
- "no one is helping"
- "I followed your process"
- "this is ridiculous"
- strong frustration after repeated failed resolution

Required behavior:

- `user_distress` beats `faq_miss_threshold_exceeded`
- `user_distress` beats `turn_budget_exhausted`
- Should target `cs_interactive_002` and `cs_interactive_014`

### B2. Messaging / account / email routing stability

Add deterministic pre-LLM routing bias for account, login, message, notification, and email-sync phrases.

Target routing:

- account locked / login / cannot access account → UC-D
- notification not arriving / messages not received / replies and messaging issue → UC-C or UC-D according to the domain spec
- email/app/account-sync issue should not drift to UC-K unless there is a true technical-regression signal

Required behavior:

- `cs_interactive_095` must not route to UC-K.
- UC-K regression override from Sprint 1 must still pass for `cs_interactive_066`.
- Generic contact-option FAQ must still avoid UC-K.

### B3. Soft-OOS UNKNOWN topic + immediate escalation

Fix the case where an UNKNOWN-topic session receives an immediate user-requested escalation before any active UC is committed.

Target case:

- `cs_interactive_029`

Acceptable approaches:

- commit a safe default active UC before handover, if the issue family can be inferred
- or allow one DISCOVER / classify turn before honoring immediate escalation
- or explicitly support UNKNOWN-topic escalation with a valid active_use_case fallback that satisfies the trace contract

Required behavior:

- no `CONTRACT_VIOLATION:active_use_case`
- semantic escalation reason remains `user_requested`
- `turn_budget_exhausted` must not become the semantic reason

## Do not implement

- full claim classifier
- full trace / transcript alignment
- large new smoke suite expansion
- full service-outcome taxonomy
- broad rubric rewrite
- production GDPR / moderation / payment / scam / OOS suite expansion
- LLM judge stabilization
- broad eval redesign

## Target cases

- `cs_interactive_001`
- `cs_interactive_002`
- `cs_interactive_014`
- `cs_interactive_029`
- `cs_interactive_095`
- `cs_interactive_066` as regression guard

## Success metrics

Primary metrics:

- `L1:escalation_reason_consistency` remains 0
- `cs_interactive_002` and `cs_interactive_014` stamp `user_distress` when distress signals are present
- `cs_interactive_029` no longer fails `CONTRACT_VIOLATION:active_use_case`
- `cs_interactive_095` does not route to UC-K
- `cs_interactive_066` still routes to UC-K and passes

Secondary metrics:

- smoke pass rate should stabilize closer to 6–8 / 14 across multiple runs
- if raw pass count varies, targeted blocker counts are more important

## Review rule

The next Codex review must only check whether Sprint 2 objective was met.

Codex should not perform a broad review of missing production cases, future groundedness work, service-outcome taxonomy, or production policy expansion unless it directly blocks B0, B1, B2, or B3.