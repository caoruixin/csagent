# Action Bank

Date: 2026-05-05 (post Sprint 3)

## Status — Sprint 3 closure

### S3-C0. Kimi endpoint / credential configuration normalization — **DONE** (2026-05-05)

Implemented in:

- `server/src/main/java/com/gumtree/csagent/config/LlmConfigValidator.java`
  *(new)* — fail-fast validator (FATAL primary, WARN fallback) +
  warn diagnostic for the historically-flaky `https://api.moonshot.ai/v1`
  default endpoint + secret-free `describe` summary.
- `server/src/main/java/com/gumtree/csagent/config/LlmClientConfig.java`
  — wires `validateOrThrow` at bean creation + redacted lineup log.
- `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java`
  — dedicated `[chat:auth-error]` log line on 401/403 referencing
  the working / default Kimi endpoints (no secret leakage).
- `.env.local` — flipped `KIMI_BASE_URL` to the working
  `https://api.moonshot.cn/v1` with an inline comment.

12 new tests in `LlmConfigValidatorTest`; existing
`OpenAiCompatibleLlmClientTest` widened to accept the new
non-retryable-401 propagation contract.

Documented working pair (this repo):

```bash
KIMI_API_KEY=$MOONSHOT_API_KEY
KIMI_BASE_URL=$MOONSHOT_API_BASE   # https://api.moonshot.cn/v1
KIMI_MODEL=kimi-k2.6
```

### S3-C1. Bot-side LLM retry / timeout robustness — **DONE** (2026-05-05)

Implemented in:

- `OpenAiCompatibleLlmClient.chat` — explicit retryable-status
  classifier; bounded 2-attempt loop; structured
  `[chat:retry] / [chat:non-retryable-status] / [chat:exhausted]`
  log tags.
- `LlmInvocationService.classifyFailure` — stable trace tags
  (`llm_auth_error / llm_rate_limited / llm_server_error / llm_timeout
  / llm_connect_failed / llm_transport_error / llm_unknown_error`)
  walked from the cause chain; appended to the persisted
  `LlmCallLogger.logFailure` message.
- `FallbackLlmClient` (untouched) continues to engage DeepSeek on
  transient failures.

End-to-end maximum: 2 attempts at primary + 2 at fallback.
Bounded-loop semantics preserved; deterministic tool-scope
violations are NOT retried (the inner loop only retries on 429 /
5xx / transport).

Test coverage:

- 6 new tests in `OpenAiCompatibleLlmClientRetryTest` (embedded
  HttpServer): 429 → retry-and-success, 503 → retry-and-success,
  5xx exhaust at exactly 2 attempts, 401 no retry, 400 no retry,
  blank api-key fails fast without any HTTP call.
- 9 new tests in `LlmInvocationServiceFailureClassificationTest`
  pin every classifier branch.

### S3-C2. Replies/Messaging strong-prior carry-forward into bot-loop — **DONE** (2026-05-05)

Implemented in:

- `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java`
  — new `deriveStrongPriorUc(topic, description, registry)`
  read-only derivation; HTML-encoded alias `Replies &amp; Messaging`
  registered alongside `Replies & Messaging` in `TOPIC_ALIASES`,
  `B2_BIAS_TOPICS`, `UC_K_OVERRIDE_TOPICS`. The encoded alias
  fix was load-bearing — `FormContextIngestionService.sanitize`
  HTML-encodes `&` to `&amp;`, so the live form payload never
  matched the raw alias. Both forms are now registered.
- `server/src/main/java/com/gumtree/csagent/service/tools/ClassifyUseCaseTool.java`
  — `shouldPreserveStrongPrior(session, proposed)` policy:
  refuses LLM-driven UC overwrite when current active UC matches
  the deterministic strong-prior derivation AND proposed UC differs.
  Idempotent re-classification is allowed; hard-shifts (UC-G/I/J)
  via `DriftDetector` mutate `activeUseCase` BEFORE the bot loop
  so the policy automatically releases. Rejected commits return
  `success(committed=false, …, reason=strong_prior_carry_forward)`.

Test coverage:

- 14 new tests in `ClassifyUseCaseToolStrongPriorTest`:
  cs014 strong prior refuses drift to UC-B / UC-F / UC-H;
  idempotent commit; hard-shift release; no-strong-prior
  baseline; cs001 / cs002 / cs066 / cs095 regression coverage;
  HTML-encoded topic still carries forward.
- 1 new test in `UseCaseRouterB2BiasTest` pinning the encoded
  alias mapping.

D11 closed: cs014 routes UC-C in 3/3 Sprint 3 runs (targeted +
2 smoke runs) vs 0/3 in Sprint 2.1.

### Sprint 3 accepted state

Latest accepted Sprint 3 references:

- Canonical smoke baseline:
  `eval_interactive/results/20260504-191137/results.json` (7/14, 0.4055)
- Nondeterminism reference:
  `eval_interactive/results/20260504-191541/results.json` (6/14, 0.3589)
- Targeted cs014 reference:
  `results/20260504-191028/results.json` (1/1, composite 0.786, UC-C / `faq_miss_threshold_exceeded`)

Mvn test count: 582 / 582 passed (was 538; +44 sprint-3 tests).
Pytest: 283 / 283.

## Status — Sprint 2.1 closure

### S2.1-P1a. cs014 CaseSpec correction through approved override / audit path — **DONE** (2026-05-05)

Sprint 2 follow-up had corrected `cs_interactive_014.yaml` directly, changing
`expected.escalation_trigger` from `user_distress` to
`faq_miss_threshold_exceeded`. Codex rejected that as a P1 because the change
bypassed the approved CaseSpec override / audit mechanism.

Sprint 2.1 fixed this by moving the correction to the approved Wave A6.6 v2
override path:

- `eval_interactive/case_spec_overrides.yaml` now has an approved entry for
  `source_session_id: 570Q5000008u9gjIAA`.
- The entry has reviewer, date, source, confidence, supporting turn numbers,
  and rationale.
- The entry sets `expected.escalation_trigger: faq_miss_threshold_exceeded`
  and aligns the bot handling pattern.
- `eval_interactive/case_specs/smoke/cs_interactive_014.yaml` was regenerated
  through the CaseSpec generator rather than relying on an untracked hand edit.
- `qa-reports/case-spec-generation-audit.md` records
  `case-level override applied: YES` for cs014.
- `qa-reports/smoke-case-review.md` records cs014 as
  `needs_override (applied)` with the same recommended trigger.

Codex Sprint 2.1 final review:

- `decision: pass`
- `blocking_count: 0`

### S2.1-P1b. cs014 route/loop handover persistence regression — **DONE** (2026-05-05)

Sprint 2 follow-up had added helper-level tests for cs014, but Codex rejected
that as insufficient because it did not exercise the route/loop persistence
path.

Sprint 2.1 added:

`server/src/test/java/com/gumtree/csagent/integration/Cs014RouteAndLoopHandoverIntegrationTest.java`

This focused integration test:

- Starts from cs014's actual form context.
- Commits the Replies/Messaging UC-C path.
- Replays the relevant handover-driving follow-up turn through
  `ControlKernel.processMessage` and `AgentRunLoop`.
- Asserts `active_use_case=UC-C`.
- Asserts `session.escalationReason=faq_miss_threshold_exceeded`.
- Asserts the persisted `request_handover.arguments.escalation_reason`
  matches the session reason.
- Asserts the assembled handover payload carries the same escalation reason
  and `primary_use_case=UC-C`.
- Pairwise-equates session state, persisted tool call, and handover payload so
  the cs014 path cannot reopen the `L1:escalation_reason_consistency` failure
  class.

No production runtime code changed in Sprint 2.1.

### Sprint 2.1 accepted state

Latest accepted Sprint 2.1 references:

- Canonical smoke baseline for next sprint:
  `eval_interactive/results/20260504-172942/results.json` (5/14)
- Nondeterminism reference:
  `eval_interactive/results/20260504-173601/results.json` (5/14)
- Targeted cs014 reference:
  `eval_interactive/results/20260504-172751/results.json`

Known interpretation:

- Sprint 2.1 closed the two Codex P1 blockers.
- Live cs014 evals still show D11 bot-loop UC drift away from UC-C.
- The D11 drift is carried forward as a next-sprint candidate, not a Sprint 2.1 closure blocker.

## Status — Sprint 2 (B0 / B1 / B2 / B3)

### B0. Persisted `request_handover` reason normalization — **DONE** (2026-05-04)

Implemented in
`ControlKernel.normalizeHandoverArgsToSessionReason` (applied in both
`recordRunResult` and the legacy `recordTurn` ESCALATE branch) +
`PhaseEvaluator.canonicalize` extended to mirror the resolver's
legacy-literal mapping.

The persisted `request_handover.arguments.escalation_reason`, the session
`escalationReason`, and the handover payload now all carry the same
resolver-canonical value regardless of what literal the LLM emitted.

Integration test:

- `AgentRunLoopHandoverReasonNormalizationIntegrationTest`
  covers:
  - lower-priority LLM literal vs higher-priority session reason
  - legacy literal canonicalisation

Sprint 2.1 status:

- `L1:escalation_reason_consistency` remains 0 across the accepted
  Sprint 2.1 smoke runs.
- cs014 route/loop persistence is now additionally pinned by
  `Cs014RouteAndLoopHandoverIntegrationTest`.

### B1. Distress / frustration detector — **DONE** (2026-05-04)

Implemented in `EscalationReasonResolver.detectDistressSignal` +
`isAllCapsShout` + new step 2.4 in `ControlKernel.processMessage`.

Patterns cover:

- "you are not helping"
- "your no helping"
- "no one is helping"
- "I followed your so called process"
- "this is ridiculous"
- "How long do I have to wait"
- "since day 1"
- ALL-CAPS shouting (≥ 8 letters, ≥ 70% uppercase)

The resolver's precedence table guarantees:

- `user_distress` beats `faq_miss_threshold_exceeded`
- `user_distress` beats `turn_budget_exhausted`
- `user_distress` beats `clarification_budget_exhausted`
- `user_requested` still beats `user_distress`

Target case status:

- `cs_interactive_002`: stamps `user_distress` when the bot produces a completed turn.
- `cs_interactive_014`: Sprint 2.1 determined the actual cs014 seed messages are not semantic distress. The expected escalation trigger is now correctly `faq_miss_threshold_exceeded` through the approved override path.
- `cs_interactive_029`: `user_requested` still wins per spec.

### B2. Messaging / account / email-sync routing bias — **DONE** (2026-05-04)

Implemented in `UseCaseRouter.normalizeTopicSubject`:

- `"Replies & Messaging"` → `"Replies or Messaging"`

Implemented in `UseCaseRouter.matchAccountMessagingBias`:

- UC-A for ad/listing visibility phrases
- UC-D for account/login/access phrases
- UC-C for messages/notifications/replies/inbox phrases

The B2 bias is inserted between the UC-K override and the LLM classifier.
UC-K override still fires first, preserving cs066.

Target case status:

- `cs_interactive_001`: reaches UC-C through strong-prior alias when not blocked by bot-side LLM flake.
- `cs_interactive_002`: distress reason is stable when the bot completes, but active UC can still drift under LLM noise.
- `cs_interactive_014`: initial route is UC-C via alias-normalised strong prior; live bot-loop drift remains D11.
- `cs_interactive_095`: routes to UC-A / not UC-K when the session completes.
- `cs_interactive_066`: UC-K preserved.

### B3. Soft-OOS UNKNOWN-topic immediate-escalation fallback — **DONE** (2026-05-04)

Implemented in `ControlKernel.inferFallbackUseCase` +
`forceEscalate`.

When a session escalates without any committed UC, runtime fills in a
deterministic fallback `active_use_case` from user message + form description
keywords. Semantic escalation reason is unaffected.

Target case status:

- `cs_interactive_029`: UC-D fallback committed; semantic reason
  `user_requested` preserved.
- `CONTRACT_VIOLATION:active_use_case` no longer fires for the original B3
  target case.
- L2 `correct_uc` still fails because runtime UC-D fallback differs from spec
  primary UC-C; this is tracked as D12.

## Status — Sprint 1 (A1 / A2 / A3) — preserved

### A1. Deterministic EscalationReasonResolver — **DONE** (2026-05-04)

Implemented in `EscalationReasonResolver` and integrated into
`ControlKernel` and `PhaseEvaluator`.

Capabilities:

- canonical escalation reason enum
- precedence table
- explicit user / callback request beats budget reasons
- lower-priority reasons such as `turn_budget_exhausted` and
  `faq_miss_threshold_exceeded` cannot overwrite higher-priority semantic reasons
- tool call, session state, and handover payload agree

Target case status:

- `cs_interactive_002`: B1 now handles distress stamping when the bot completes.
- `cs_interactive_014`: Sprint 2.1 corrected the expected trigger to
  `faq_miss_threshold_exceeded`; D11 bot-loop drift remains separate.
- `cs_interactive_029`: semantic reason remains `user_requested`; B3 fixed the missing active UC contract violation for the target path.

### A2. UC-K technical regression routing — **DONE** (2026-05-04)

Implemented in `UseCaseRouter.matchUcKTechnicalRegression`.

Pre-LLM deterministic override fires on technical regression wording such as:

- disappeared
- used to work
- greyed out
- app crashes
- error when
- "not getting THE option"

UC-E generic FAQ remains preserved for general product/contact-option questions.

Target case status:

- `cs_interactive_066`: remains the UC-K regression guard.
- Sprint 2 / 2.1 guards require cs066 to stay UC-K.
- Negative guard: cs095 should not route to UC-K.

### A3. Server-side handover payload assembler — **DONE** (2026-05-04)

Implemented in `HandoverPayloadAssembler`.

Required-field coverage:

- user issue
- detected UC
- escalation reason
- collected identifiers
- relevant tools / sources / status checks
- partial answer or clear blocker
- unresolved question
- issue-specific summary
- terminal close reason when applicable

Also added:

- `recordHandover` is invoked when the auto-search path itself escalates at session-create time.

Target case status:

- `cs_interactive_066`: handover completeness is preserved.
- `cs_interactive_095`: payload assembly is not the blocker; remaining mismatch is routing / outcome related.
- `cs_interactive_259`: handover persists; remaining failures are routing / outcome / stall related.

## Carry-over deferrals

These remain deferred unless explicitly selected into a future sprint objective:

- D1. Full trace / transcript alignment gate
- D2. Turn-0 factual claim classifier
- D3. Full service-outcome taxonomy
- D4. Large production smoke case expansion
- D5. Tool error / timeout cases
- D6. Full semantic groundedness classifier
- D7. Complete GDPR / moderation / payment / scam / out-of-scope eval suite

## Resolved in Sprint 2 (formerly D8 / D9 / D10)

- ~~D8. Distress / frustration detector.~~ Done as B1.
- ~~D9. Pre-LLM routing bias for messaging / login keywords.~~ Done as B2.
- ~~D10. Soft-OOS + early-escalation interaction.~~ Done as B3.

## New deferred items emerging from Sprint 2 / 2.1

### D11. cs014 / Replies-Messaging cross-turn UC drift — **CLOSED** (2026-05-05, Sprint 3 §C2)

Initial route is UC-C via the alias-normalised Replies/Messaging strong prior;
the bot-turn AgentRunLoop used to drift to UC-B / UC-F / UC-H on the same
issue.

Closure: Sprint 3 §C2 implemented the strong-prior carry-forward policy in
`ClassifyUseCaseTool.shouldPreserveStrongPrior`. cs014 routes UC-C in 3/3
Sprint 3 runs (targeted + 2 smoke). The supporting alias-encoding fix
(`Replies &amp; Messaging` registered alongside `Replies & Messaging`)
was load-bearing — without it, the §B2 alias path was a dead code path
on the live `FormContextIngestionService.sanitize` output. Hard-shift
exits (UC-G / UC-I / UC-J) still work via `DriftDetector` mutating
`activeUseCase` before the bot loop runs.

### D12. cs029 outcome lift — **OPEN**

B3 fixes the original contract violation, but L2 `correct_uc` still fails because
the deterministic UC-D fallback differs from the spec primary UC-C.

Potential fixes:

- Widen the spec to accept UC-D as a secondary if transcript evidence supports account fallback.
- Or add a stronger heuristic that picks UC-C when "messages / replies / inbox"
  appear in the soft-OOS user message.

Recommended handling:

- Do not include in Sprint 3 unless explicitly scoped.
- This is a spec-vs-fallback alignment issue, not a runtime reliability issue.

### D13. Bot-side Kimi LLM credentials / rate-limit / timeout robustness — **CLOSED** (2026-05-05, Sprint 3 §C0 + §C1)

Closure:

- §C0: `LlmConfigValidator` runs at bean creation, fails fast on
  blank / placeholder / malformed primary config; emits a WARN
  diagnostic for the historically-flaky `https://api.moonshot.ai/v1`
  default endpoint. Documented working endpoint / env-var pair.
  Secrets are never logged; the lineup describe string only
  reports `present|placeholder|blank`.
- §C1: bounded 2-attempt retry classifier on 429 / 5xx / transport;
  401 / 403 surface immediately to the fallback layer. Failure
  classifier emits stable trace tags
  (`llm_auth_error / llm_rate_limited / llm_server_error
  / llm_timeout / llm_connect_failed / llm_transport_error`) that
  post-hoc analysis can use to separate infra flakes from semantic
  failures.

Evidence: 0 TIMEOUT and 0 session_create_failed in both Sprint 3
smoke runs (was 2 / 0 in Sprint 2.1). cs014 / cs002 reach the bot
loop without timing out across all Sprint 3 runs.

### D14. cs002 UC drift while distress fires — **CLOSED** (2026-05-05, Sprint 3 §C2 indirect)

Closure: §C2 carry-forward applies to cs002's "Replies or Messaging"
form context too. Sprint 3 r1 cs002 routed UC-C / `user_distress` /
composite 0.771. Sprint 3 r2 cs002 stamped
`faq_miss_threshold_exceeded` instead of `user_distress` because
the persona simulator emitted a non-distress message that turn —
the detector / resolver / escalation path are deterministic and
remain pinned by Java tests; the per-run reason variation is
upstream LLM nondeterminism, not a runtime drift.

### D15. L3 relevance / tone judge volatility — **OPEN / DEFERRED**

L3 `relevance` and `tone_appropriateness` continue to flip across runs.

Recommended handling:

- Keep deferred.
- Do not include in Sprint 3.
- Revisit only in a future judge calibration or eval-design sprint.

## Recommended next sprint

### Sprint 4 candidate

`Spec-vs-runtime alignment for cs001 / cs002 / cs029`

After Sprint 3 closed D11 / D13 / D14, the remaining smoke
instability is no longer runtime-side — it is a spec-vs-runtime
alignment question. Recommended actions (keep narrow):

- E1. cs001 / cs002 escalation-reason alignment. Decide whether
  the case specs accept `user_distress` as a valid expected reason
  for personas that use distress phrasing. If yes, apply via the
  approved CaseSpec override / audit path (Wave A6.6 v2).
- E2. cs029 outcome lift (D12). Either widen the spec to accept
  UC-D as a secondary or add a soft-OOS sub-detector that picks
  UC-C when "messages / replies / inbox" appear.
- E3. cs259 routing / stall stabilisation if Sprint 3 r2's
  `trace_minimum` repeats.

Do not include in Sprint 4 unless explicitly scoped:

- broad anchor / exploration / promotion expansion
- full trace / transcript alignment
- full claim classifier
- full service-outcome taxonomy
- L3 judge stabilization
- production GDPR / moderation / payment / scam / OOS expansion

## Rule (carry-over)

If a finding is not directly related to an active sprint contract or one of the
explicitly named deferred items above, do not implement it without an updated
sprint scope.

Avoid returning to broad full-review → fix → full-review loops. Each sprint
must name 3–4 accepted actions, define target cases, and close only when Codex
reports no blocking sprint failures or only non-blocking P2 notes remain.