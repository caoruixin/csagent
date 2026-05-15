# QA Report - D14 Fixes (Parallel Rerank, Query Enrichment, LLM Call Log)

**Generated**: 2026-04-26 07:45
**Scope**: D14.1 Parallel Rerank, D14.2 Form Description Query Enrichment, D14.3 LLM Call Log
**Tech Stack**: Java 17, Spring Boot 3.2.x, JUnit 5 + Mockito, Maven
**Branch**: design-v1-without-human-review

---

## 1. Build Status

| Stage          | Result   | Details                                    |
|----------------|----------|--------------------------------------------|
| `mvn compile`  | PASS     | 92 source files compiled, zero errors      |
| `mvn test`     | PASS     | 284 tests, 0 failures, 0 errors, 0 skipped |
| `mvn verify`   | PASS     | Exit code 0, full lifecycle clean           |

---

## 2. Code Review Findings

### Summary

| # | Severity | File | Line | Issue | Suggestion |
|---|----------|------|------|-------|------------|
| 1 | MEDIUM | RerankService.java | 44 | Static `ExecutorService` with fixed thread pool never shuts down | Add `@PreDestroy` shutdown hook or use Spring-managed `TaskExecutor` |
| 2 | MEDIUM | RerankService.java | 44 | Hardcoded pool size `8` -- not configurable | Externalize to `LlmProperties` or a dedicated config property |
| 3 | LOW | RerankService.java | 99 | Double `join()` -- `CompletableFuture.allOf(...).join()` then `futures.stream().map(CF::join)` | First `join()` is sufficient but the second is harmless (already completed). Minor redundancy. |
| 4 | MEDIUM | PhaseEvaluator.java | 504 | Boundary condition: `userMessage.length() >= 20 && turnIndex > 1` -- message length 19 chars at turn 5 still enriches | This is by design (short messages always enrich), but could surprise. Consider documenting threshold. |
| 5 | LOW | PhaseEvaluator.java | 509 | Description truncated to 200 chars with hard cutoff -- may cut mid-word | Use word-boundary truncation consistent with `KnowledgeSearchService.truncateSnippet()` |
| 6 | LOW | LlmCallLogger.java | 40-57 | `OffsetDateTime.now()` is called inside the `try` block -- if clock is wrong, logging still succeeds but timestamp is misleading | Not actionable, just a note |
| 7 | LOW | LlmCallLog.java | 57 | `@Builder.Default private Boolean success = true` uses boxed `Boolean` -- could be `boolean` for null safety | Use primitive `boolean` if the DB column is `NOT NULL` |
| 8 | LOW | DemoInspectionController.java | 95-97 | No pagination on `findBySessionIdOrderByCreatedAt` -- sessions with many rerank calls could return very large lists | Add optional `?limit=N` query param for production |
| 9 | LOW | LlmInvocationService.java | 138 | `invokeRouting()` passes `null` for `turnIndex` via `Integer` type -- consistent with `LlmCallLogger.log(String, Integer, ...)` but callers must remember to box | Document nullability with `@Nullable` annotation |

### Details

#### 1. Static ExecutorService Never Shut Down (MEDIUM)

- **File**: `RerankService.java:44`
- **Severity**: MEDIUM
- **Description**: `RERANK_EXECUTOR` is a `static final ExecutorService` created via `Executors.newFixedThreadPool(8)`. It is never shut down on application context close. In production this causes a clean shutdown delay or thread leak. Spring Boot's `TaskExecutor` would integrate with the lifecycle automatically.
- **Code**: `private static final ExecutorService RERANK_EXECUTOR = Executors.newFixedThreadPool(8);`
- **Suggestion**: Either add a `@PreDestroy` method in `RerankService` that calls `RERANK_EXECUTOR.shutdown()`, or inject a Spring `TaskExecutor` bean configured with a thread pool.

#### 2. Hardcoded Thread Pool Size (MEDIUM)

- **File**: `RerankService.java:44`
- **Severity**: MEDIUM
- **Description**: The pool size of 8 is hardcoded. In environments with fewer cores or during load testing, this may need tuning. The value should be externalized to application config.
- **Suggestion**: Add a `rerankThreadPoolSize` property to `LlmProperties` or create a separate config class.

#### 3. Enrichment Boundary Semantics (MEDIUM)

- **File**: `PhaseEvaluator.java:504`
- **Severity**: MEDIUM
- **Description**: The enrichment logic activates when `userMessage.length() < 20 || turnIndex <= 1`. This means a 19-character message on turn 10 will still be enriched, and a 100-character message on turn 0 will also be enriched. The "OR" condition is intentional per the spec but the interaction between the two conditions should be clearly documented, as it may cause unexpected behavior when a user sends a short follow-up on a later turn and the old form description gets concatenated.
- **Suggestion**: Add a code comment explaining the rationale: short messages likely indicate the user hasn't restated their problem; early turns are before the user has given enough context.

---

## 3. Merge Conflict Check

All three D14 agents modified overlapping files. Review of the current source shows:

| File | D14.1 | D14.2 | D14.3 | Conflict? |
|------|-------|-------|-------|-----------|
| `RerankService.java` | Parallel rerank | -- | Added `LlmCallLogger` | NO - changes compose cleanly |
| `PhaseEvaluator.java` | -- | Added `enrichQueryWithFormContext()` | Added sessionId/turnIndex to `invokeChat()` calls | NO - changes compose cleanly |
| `LlmInvocationService.java` | -- | -- | Added `LlmCallLogger` + sessionId/turnIndex params | NO - single owner |
| `KnowledgeSearchService.java` | sessionId/turnIndex passthrough | -- | -- | NO - single owner |
| `SearchKnowledgeTool.java` | sessionId/turnIndex passthrough | -- | -- | NO - single owner |
| `KnowledgeSearchController.java` | `null, 0` for REST calls | -- | -- | NO - single owner |
| `UseCaseRouter.java` | -- | -- | sessionId passthrough to routing | NO - single owner |

**Verdict**: No merge conflicts detected. All three fixes compose cleanly.

---

## 4. New Tests Written

### C1: RerankServiceTest (12 tests)
**File**: `server/src/test/java/com/gumtree/csagent/service/knowledge/RerankServiceTest.java`

| Test | What it covers |
|------|---------------|
| `rerank_multipleCandidates_shouldReturnSortedByScoreDescending` | Parallel scoring + sort order |
| `rerank_singleCandidate_shouldReturnOneResult` | Single candidate path |
| `rerank_emptyList_shouldReturnEmptyList` | Empty input edge case |
| `rerank_oneFailingCandidate_shouldNotBlockOthers` | Fault isolation -- one throw doesn't block others |
| `rerank_allCandidatesFail_shouldReturnAllWithFallbackScores` | Total failure graceful degradation |
| `rerank_shouldPreserveAllCandidateMetadata` | Metadata passthrough correctness |
| `rerank_shouldLogCalls_viaLlmCallLogger` | Logger integration for success |
| `rerank_failedCandidate_shouldLogFailure` | Logger integration for failure |
| `rerank_nullSessionId_shouldNotThrow` | REST API path (null session) |
| `rerank_llmReturnsNonNumeric_shouldUseFallbackScore` | Score parsing: text response |
| `rerank_llmReturnsEmptyContent_shouldUseFallbackScore` | Score parsing: empty response |
| `rerank_llmReturnsScoreWithText_shouldExtractScore` | Score parsing: number embedded in text |

### C2: PhaseEvaluatorQueryEnrichmentTest (15 tests)
**File**: `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorQueryEnrichmentTest.java`

| Test | What it covers |
|------|---------------|
| `enrichQuery_shortMessage_withFormDescription_shouldEnrich` | Short msg + description = enriched |
| `enrichQuery_shortMessage_laterTurn_shouldEnrich` | Short msg always enriches regardless of turn |
| `enrichQuery_earlyTurn_longMessage_shouldEnrich` | Early turn enriches even with long message |
| `enrichQuery_longMessage_laterTurn_shouldNotEnrich` | Long msg + later turn = no enrichment |
| `enrichQuery_exactly20Chars_laterTurn_shouldNotEnrich` | Boundary: exactly 20 chars at turn > 1 |
| `enrichQuery_nullFormContext_shouldReturnOriginal` | Null formContext guard |
| `enrichQuery_blankFormContext_shouldReturnOriginal` | Blank formContext guard |
| `enrichQuery_emptyFormContext_shouldReturnOriginal` | Empty formContext guard |
| `enrichQuery_formContextWithoutDescription_shouldReturnOriginal` | Missing description field |
| `enrichQuery_formContextWithNullDescription_shouldReturnOriginal` | Null description value |
| `enrichQuery_formContextWithEmptyDescription_shouldReturnOriginal` | Empty description value |
| `enrichQuery_formContextWithBlankDescription_shouldReturnOriginal` | Blank description value |
| `enrichQuery_malformedJson_shouldReturnOriginal` | Invalid JSON resilience |
| `enrichQuery_longDescription_shouldTruncateTo200Chars` | Truncation at 200 chars |
| `enrichQuery_nullHistory_shouldTreatAsTurnZero` | Null history safety |

### C3: LlmCallLoggerTest (11 tests)
**File**: `server/src/test/java/com/gumtree/csagent/service/observability/LlmCallLoggerTest.java`

| Test | What it covers |
|------|---------------|
| `log_shouldPersistEntity` | Full field mapping verification |
| `log_withNullTurnIndex_shouldPersistWithNullTurnIndex` | Routing calls (null turn) |
| `log_withNullSummaries_shouldPersistWithNulls` | Null summary fields |
| `log_shouldTruncateLongRequestSummary` | 500-char truncation |
| `log_shouldTruncateLongResponseSummary` | 500-char truncation |
| `log_whenRepositoryThrows_shouldNotPropagate` | Exception isolation |
| `log_whenRepositoryThrowsNPE_shouldNotPropagate` | NPE isolation |
| `logFailure_shouldPersistWithSuccessFalse` | Failure field mapping |
| `logFailure_shouldTruncateLongErrorMessage` | 1000-char truncation |
| `logFailure_whenRepositoryThrows_shouldNotPropagate` | Exception isolation on failure path |
| `logFailure_withNullTurnIndex_shouldPersist` | Routing failure (null turn) |

### C4: DemoInspectionControllerLlmCallsTest (4 tests)
**File**: `server/src/test/java/com/gumtree/csagent/controller/DemoInspectionControllerLlmCallsTest.java`

| Test | What it covers |
|------|---------------|
| `getSessionLlmCalls_withData_shouldReturnLogs` | Happy path with multiple log entries |
| `getSessionLlmCalls_noData_shouldReturnEmptyList` | Empty result for unknown session |
| `getSessionLlmCalls_withFailedCalls_shouldIncludeThem` | Failed call entries included |
| `getSessionLlmCalls_shouldQueryBySessionId` | Correct repository method called |

---

## 5. Test Execution Results

### Test Summary

| Test File | Total | Pass | Fail | Skip |
|-----------|-------|------|------|------|
| RerankServiceTest | 12 | 12 | 0 | 0 |
| PhaseEvaluatorQueryEnrichmentTest | 15 | 15 | 0 | 0 |
| LlmCallLoggerTest | 11 | 11 | 0 | 0 |
| DemoInspectionControllerLlmCallsTest | 4 | 4 | 0 | 0 |
| *All existing tests (16 classes)* | 242 | 242 | 0 | 0 |
| **TOTAL** | **284** | **284** | **0** | **0** |

### Failed Tests

None. All 284 tests pass.

---

## 6. Test Coverage

### Tested modules
- `RerankService` -- parallel execution, fault tolerance, score parsing, logging
- `PhaseEvaluator.enrichQueryWithFormContext()` -- all enrichment conditions and edge cases
- `LlmCallLogger` -- persistence, truncation, exception isolation
- `DemoInspectionController.getSessionLlmCalls()` -- endpoint behavior

### Untested modules (in scope of D14 changes)
- `KnowledgeSearchService.search()` -- the new `sessionId/turnIndex` parameter passthrough is not directly tested (it flows through to `RerankService.rerank()` which is tested)
- `SearchKnowledgeTool.execute()` -- the sessionId extraction from `session.getSessionId()` and `session.getTotalBotTurns()` is not explicitly tested in a new test (existing `ToolDispatcherTest` covers dispatch)
- `UseCaseRouter.route()` -- the `session.getSessionId()` passthrough to `invokeRouting()` is not directly tested in a new test (existing routing tests don't verify logging)

### Priority modules to add tests
1. `KnowledgeSearchService` integration test with mocked rerank -- would verify the full pipeline including sessionId propagation
2. `UseCaseRouter` -- add a test verifying `invokeRouting()` receives the session ID

---

## 7. Summary

### HIGH Priority (fix immediately)
None.

### MEDIUM Priority (fix soon)
1. **RerankService static ExecutorService never shut down** (RerankService.java:44) -- Add `@PreDestroy` shutdown or use Spring-managed `TaskExecutor` to prevent thread leak on shutdown.
2. **RerankService hardcoded pool size** (RerankService.java:44) -- Externalize the `8` to config for tunability.
3. **Query enrichment boundary semantics** (PhaseEvaluator.java:504) -- The OR condition means short follow-up messages on later turns still get enriched with potentially stale form description. Document the design decision.

### LOW Priority (fix when convenient)
1. Double `join()` in `RerankService.rerank()` (line 99 + 103) -- minor redundancy, no functional impact.
2. Description truncation cuts mid-word (PhaseEvaluator.java:509) -- use word-boundary truncation.
3. `LlmCallLog.success` uses boxed `Boolean` instead of primitive `boolean`.
4. No pagination on `/v1/demo/sessions/{id}/llm-calls` endpoint.
5. Missing `@Nullable` annotation on `turnIndex` parameter in `LlmCallLogger`.

---

## 8. Recommendation

**SHIP** -- All three fixes compile cleanly, compose without merge conflicts, and pass 284 unit tests (42 new + 242 existing). No HIGH-severity issues found. The MEDIUM findings (executor lifecycle, config externalization) are improvement items that do not block release.
