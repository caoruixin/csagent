## Sprint Review Decision

decision: fix_required
blocking_count: 2
summary: M0 is directionally correct: session create now uses deterministic non-blocking routing, persists the session before returning, and `ContextProjectionBuilder` projects stored `form_context` on follow-up turns. Sprint 8.1 is not passable yet because M1 still permits more than one total retry through the production fallback provider chain, and M2's K0 evidence gate can still fire on an LLM-only escalation with no successful tool work.

## Blocking Sprint Failures

- severity: P1
- target case or test: M1 production LLM client chain / missing fallback-wrapper regression
- blocks current sprint goal: yes
- evidence: `LlmClientConfig` wires production `LlmClient` as `new FallbackLlmClient(kimiLlmClient, deepseekLlmClient, ...)`. `OpenAiCompatibleLlmClient.chat` loops `attempt <= 2`, and its own comment still documents "2 attempts at this provider plus 2 attempts at the fallback". `FallbackLlmClient.chat` then calls `fallback.chat(request)` after transient primary exhaustion. Therefore a fast repeated 503/429 can consume primary attempt 1, primary retry, fallback attempt 1, and fallback retry. That violates Sprint 8.1 M1's max-one-retry / no-third-attempt contract. The committed `Sprint81BudgetedRetryTest` covers only the inner `OpenAiCompatibleLlmClient`, not the production fallback wrapper.
- exact minimal fix: Enforce one global retry budget across the provider chain. Either make the fallback provider the single retry (primary one attempt, fallback one attempt) or allow primary's one retry and suppress fallback after that retry is consumed. Carry a shared attempt counter / retry budget in `LlmCallContext` or pass explicit retry state through `FallbackLlmClient`, keep the same wall-clock deadline check, and add a focused production-chain test proving total provider HTTP calls never exceed 2 and 401/403 still do not retry or fallback.

- severity: P1
- target case or test: M2 K0 fallback evidence gate / `ControlKernel.agentRunResultHasEvidence`
- blocks current sprint goal: yes
- evidence: Sprint 8.1 M2 says K0 must not fire when there are no real tool events, and the legitimate cs259 path is specifically a reasoned LLM/tool path with successful `search_knowledge` work. Current code computes `hasRealTool` and `hasRealLlm`, but returns `hasRealTool || hasRealLlm`. For `TerminalOutcome.ESCALATE` with a non-synthetic LLM event and zero successful tool events, this returns true and allows `applyMissingUseCaseFallback` to stamp UC-A/UC-F from regex text. Existing tests cover empty events, ERROR with LLM-only events, synthetic events, and MAX_STEPS with rejected tools, but not ESCALATE with a real LLM event and no successful tool event.
- exact minimal fix: Tighten the gate to require both at least one non-synthetic LLM event and at least one successful tool event for K0 fallback after the infra/outcome suppressions. Add a focused regression where `AgentRunResult.escalate("faq_miss_threshold_exceeded", one real LlmCallEvent, no ToolEvents, ...)` leaves `activeUseCase` null, and keep the cs259 test green by continuing to model its two successful `search_knowledge` events plus real LLM events.

## Non-Blocking Notes

- severity: P2
- target case or test: M0 Ad Support / "where is my ad?" / follow-up "hi" context preservation
- blocks current sprint goal: no
- exact minimal fix, if any: Add a focused end-to-end or projection-capture test that creates an Ad Support session with `description="where is my ad?"`, `ad_id`, email, and first name using real form ingestion, then processes follow-up `"hi"` and asserts the captured `ContextProjectionBuilder` projection contains the original `form_context` and deterministic customer/listing context. Current tests verify `routeNonBlocking`, `formIngestion.ingest`, and fast return, while production code does save the session and project `form_context`.

- severity: P2
- target case or test: M1 retry telemetry exactness
- blocks current sprint goal: no
- exact minimal fix, if any: Normalize retry log fields so every attempt and terminal retry decision consistently emits `attempt_count`, `provider`, `elapsed_ms`, `failure_class`, `retry_decision`, and `remaining_budget_ms`. Current retry logs mostly expose the data but use `attempt` rather than `attempt_count`, and non-retryable/exhausted branches do not consistently include all required fields.

## Regression Risks

- severity: P2
- target case or test: focused validation coverage / production fallback wrapper
- blocks current sprint goal: no
- exact minimal fix, if any: Add a `FallbackLlmClient` or Spring-wired `LlmClient` test for fast transient primary failures. The current focused suite passed, but it cannot catch the global attempt-count violation because it tests the inner provider client in isolation.

- severity: P2
- target case or test: legacy `UseCaseRouter.route` / `LlmInvocationService.invokeRouting`
- blocks current sprint goal: no
- exact minimal fix, if any: Leave out of Sprint 8.1 unless the legacy route path is reintroduced into a user-facing flow. `createSession` now uses `routeNonBlocking`, but `invokeRouting` still catches all exceptions and returns synthetic safe escalation; a future user-facing caller could re-open an honest-failure masking path.

## Recommended Next Phase

Narrow Runtime Follow-up
