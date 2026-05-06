## Sprint Review Decision

decision: fix_required
blocking_count: 2
summary: M0 and M2 are directionally met: create-session now uses deterministic non-blocking routing, persists form_context before returning, and K0 now requires both real tool evidence and non-synthetic LLM evidence. Sprint 8.1 is not passable yet because M1 still has a retry path that can start without enough remaining wall-clock budget, and M3's production deadline path conflates retry budget with total successful LLM calls, preventing the required same-turn DISCOVER -> RESOLVE replan when wall-clock budget remains.

## Blocking Sprint Failures

- severity: P1
- target case or test: M1 production fallback retry budget / missing low-remaining-budget guard
- blocks current sprint goal: yes
- evidence: `ChatController.USER_FACING_LLM_DEADLINE_MS` is now 30_000 (`ChatController.java:41`), while `OpenAiCompatibleLlmClient` uses 3s connect + 12s read and defines a full-attempt floor of 15.2s (`OpenAiCompatibleLlmClient.java:58-60`, `:115`). The inner client checks that floor before same-provider retry (`OpenAiCompatibleLlmClient.java:180-194`, `:200-213`), but the production retry path is the fallback wrapper, and `FallbackLlmClient` only checks `LlmCallContext.isExceeded()` and `LlmCallContext.canAttempt()` before starting fallback (`FallbackLlmClient.java:40-58`). A fast transient primary failure with less than a full attempt remaining can still engage fallback and wait up to the 12s read timeout, violating M1's "check remaining budget before retrying" / "do not start retry if remaining budget is below the minimum attempt budget" contract.
- exact minimal fix: Apply the same full-attempt remaining-budget guard at the fallback boundary before `fallback.chat(request)`, and add a focused production-chain test with a deadline whose remaining wall clock is below the attempt floor proving fallback is skipped with `deadline_exceeded`. If the sprint still requires the nominal 10s user-turn budget, restore/parameterize the controller deadline and per-attempt timeout pair so the configured retry floor fits that budget.

- severity: P1
- target case or test: M3 DISCOVER search + classify same-turn replan under production `LlmCallContext`
- blocks current sprint goal: yes
- evidence: `LlmCallContext.setDeadline` initializes a global attempt budget of 2, and `OpenAiCompatibleLlmClient.chat` consumes one slot before every HTTP call, including successful primary calls (`LlmCallContext.java:55-67`, `OpenAiCompatibleLlmClient.java:145-164`). The M3 bug shape requires two DISCOVER LLM calls, search then classify, as pinned by `Sprint81DiscoverPhaseBoundaryTest`; after those two successful calls, `LlmCallContext.canAttempt()` is false. `ControlKernel` requires `canAttempt()` before the same-turn RESOLVE replan (`ControlKernel.java:299-302`), so the production HTTP path skips the RESOLVE replan even when plenty of wall-clock budget remains. The green `Sprint81DiscoverPhaseBoundaryReplanIntegrationTest` does not set a normal production deadline in the happy-path test, so it misses this.
- exact minimal fix: Split "max one retry across primary/fallback" from "total LLM calls allowed in a user turn". Keep the wall-clock deadline shared across DISCOVER and RESOLVE, but do not spend the retry/fallback budget on successful first-attempt LLM calls; alternatively reset only the per-invocation provider-chain retry counter while preserving the same deadline. Add a production-style M3 test with `LlmCallContext.setDeadline(...)`, two successful DISCOVER LLM calls, sufficient remaining wall-clock budget, and assert the fresh RESOLVE plan actually runs once.

## Non-Blocking Notes

- severity: P2
- target case or test: M0 Ad Support / "where is my ad?" / follow-up "hi" context preservation
- blocks current sprint goal: no
- exact minimal fix, if any: Add a focused end-to-end or projection-capture test that creates an Ad Support session from `description="where is my ad?"`, `ad_id`, email, and first name, then processes follow-up `"hi"` and asserts the projected context still contains the submitted `form_context` plus deterministic listing/customer context. Current code persists and projects `form_context` (`SessionManager.java:117-130`, `FormContextIngestionService.java:58-79`, `ContextProjectionBuilder.java:394-405`), but the focused test only verifies create-time ingestion and fast return.

- severity: P2
- target case or test: M1 retry telemetry exactness
- blocks current sprint goal: no
- exact minimal fix, if any: Normalize retry/fallback log fields so every attempt and terminal retry decision consistently emits `attempt_count`, `provider`, `elapsed_ms`, `failure_class`, `retry_decision`, and `remaining_budget_ms`. Inner-client logs mostly contain the data, but fallback engagement/skips log `remaining_attempts` and exception text without the full required telemetry set.

## Regression Risks

- severity: P2
- target case or test: M3 combined tool-event trace ordering
- blocks current sprint goal: no
- exact minimal fix, if any: `ControlKernel.mergeAgentRunResults` concatenates DISCOVER and RESOLVE `ToolEvent`s without renumbering sequence indices, so the persisted one-turn trace can contain duplicate `sequence_index` values. Renumber merged LLM/tool events monotonically after concatenation if trace consumers rely on sequence ordering.

- severity: P2
- target case or test: legacy `UseCaseRouter.route` / `LlmInvocationService.invokeRouting`
- blocks current sprint goal: no
- exact minimal fix, if any: Leave out of Sprint 8.1 unless a user-facing caller reuses the legacy route path. `createSession` now uses `routeNonBlocking`, but `invokeRouting` still catches all exceptions and returns the synthetic safe escalation response, which would re-open honest-failure masking if the legacy path becomes active again.

## Recommended Next Phase

Narrow Runtime Follow-up
