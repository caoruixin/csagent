# Sprint Objective

Date: 2026-05-05

## Sprint name

Targeted Runtime Reliability and Bot-Loop UC Stability Sprint 3

## Goal

Stabilize the highest-impact runtime nondeterminism remaining after Sprint 2.1 without expanding the eval scope.

Sprint 2 / 2.1 closed:
- B0 request_handover persisted reason normalization
- B1 distress / frustration detector
- B2 messaging/account/email routing bias
- B3 soft-OOS active_use_case fallback
- cs014 CaseSpec override/audit correction
- cs014 route/loop handover persistence regression

Sprint 3 should improve repeatability and bot-loop UC stability for the existing smoke target surface.

## Baseline

Use this as the current sprint baseline:

`eval_interactive/results/20260504-172942/results.json`

Use this as nondeterminism reference:

`eval_interactive/results/20260504-173601/results.json`

Use this as targeted cs014 reference:

`eval_interactive/results/20260504-172751/results.json`

Use this only as pre-Sprint-3 / post-Sprint-2 reference:

`eval_interactive/results/20260504-151311/results.json`

## Implement only

### C0. Kimi endpoint / credential configuration normalization

The bot-side Kimi LLM path is sensitive to endpoint and key configuration.

Required behavior:

- Document the working endpoint / env-var pair used by local and CI eval.
- Fail fast with a clear diagnostic if the configured endpoint/key pair is invalid.
- Avoid silent 401 / auth failures becoming ambiguous eval flakes.
- Do not log secrets.

### C1. Bot-side LLM retry / timeout robustness

Add a bounded mitigation for transient bot-side LLM failures.

Required behavior:

- Add at most one retry for transient LLM failures such as 401 due to wrong endpoint config, rate-limit, timeout, or transport errors where retry is appropriate.
- Do not retry deterministic policy/tool-scope violations.
- Preserve bounded-loop semantics.
- Preserve traceability: retry attempts must be visible in logs or trace metadata without leaking secrets.
- Reduce TIMEOUT / session_create_failed / trace_minimum flakes on cs001 / cs002 / cs014.

### C2. Replies/Messaging strong-prior carry-forward into bot-loop

Fix the D11 failure mode where the initial route is UC-C via Replies/Messaging strong prior, but the bot-turn agent loop later drifts active_use_case to UC-B / UC-F / UC-H without a true new issue.

Required behavior:

- Preserve UC-C for ongoing Replies/Messaging issues unless the user clearly introduces a new issue.
- Carry the strong-prior route basis into the bot-turn AgentRunLoop / context projection / drift logic, whichever is the minimal correct layer.
- Do not hard-lock the UC forever: legitimate hard shift or high-risk issue should still be able to route away.
- Add focused regression coverage using cs014-style context.
- Keep the Sprint 2.1 handover consistency contract green.

## Do not implement

- cs029 outcome lift
- broad anchor / exploration / promotion expansion
- full trace / transcript alignment
- full claim classifier
- full service-outcome taxonomy
- broad rubric rewrite
- production GDPR / moderation / payment / scam / OOS expansion
- L3 judge stabilization
- broad eval redesign

## Target cases

- `cs_interactive_001`
- `cs_interactive_002`
- `cs_interactive_014`
- `cs_interactive_066` as UC-K regression guard
- `cs_interactive_095` as not-UC-K regression guard
- `cs_interactive_029` as B3 regression guard

## Success metrics

Primary metrics:

- `L1:escalation_reason_consistency` remains 0.
- `cs_interactive_014` live bot-loop no longer drifts away from UC-C for the same Replies/Messaging issue.
- `cs_interactive_001`, `cs_interactive_002`, and `cs_interactive_014` have fewer TIMEOUT / session_create_failed / trace_minimum failures across two smoke runs.
- `cs_interactive_002` still stamps `user_distress` when distress signals exist.
- `cs_interactive_066` still routes to UC-K.
- `cs_interactive_095` still does not route to UC-K.
- `cs_interactive_029` still avoids target-case `CONTRACT_VIOLATION:active_use_case` and preserves semantic `user_requested`.

Secondary metrics:

- Smoke pass rate should improve or stabilize, but targeted blocker reduction matters more.
- Eval must distinguish infrastructure/runtime flake from semantic failures where possible.

## Review rule

The next Codex review must only check whether Sprint 3 objective was met.

Codex should not perform a broad review of missing production cases, future groundedness work, service-outcome taxonomy, large eval expansion, or judge stabilization unless it directly blocks C0, C1, or C2.