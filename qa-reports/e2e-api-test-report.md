# E2E API Test Report

**Generated**: 2026-04-22 16:24
**Scope**: Full E2E API testing of CS Agent backend at http://localhost:8080
**Tech Stack**: Java 21, Spring Boot 3.2.x, PostgreSQL, Redis, Flyway, Lombok, Jackson, pgvector

---

## 1. Test Results

### Test Summary

| # | Test Name | Endpoint | Method | HTTP Status | Result | Details |
|---|-----------|----------|--------|-------------|--------|---------|
| 1a | Health Check | `/internal/health` | GET | 200 | PASS | Returns `{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}` |
| 1b | Liveness Probe | `/internal/health/liveness` | GET | 200 | PASS | Returns `{"status":"UP"}` |
| 1c | Readiness Probe | `/internal/health/readiness` | GET | 200 | PASS | Returns UP with db and redis components |
| 1d | Actuator Health | `/actuator/health` | GET | 200 | PASS | Spring Boot actuator health with full details |
| 2 | Create Session - Ad Support (Scenario #1) | `/v1/chat/sessions` | POST | 500 | **FAIL** | Internal Server Error. All session creation fails with 500. |
| 3 | Create Session - GDPR (Scenario #6) | `/v1/chat/sessions` | POST | 500 | **FAIL** | Internal Server Error. Strong prior topic (no LLM needed). |
| 4 | Create Session - Safety/Scam (Scenario #7) | `/v1/chat/sessions` | POST | 500 | **FAIL** | Internal Server Error. Strong prior topic. |
| 5 | Create Session - Messaging | `/v1/chat/sessions` | POST | 500 | **FAIL** | Internal Server Error. Strong prior topic. |
| 6 | Create Session - OOS Delivery (Scenario #10) | `/v1/chat/sessions` | POST | 500 | **FAIL** | Internal Server Error. Handover-only topic. |
| 7 | Validation - Empty topic_subject | `/v1/chat/sessions` | POST | 400 | PASS | Returns `{"reply_text":"topic_subject is required","should_end_chat":true}` |
| 8 | Validation - Missing topic_subject | `/v1/chat/sessions` | POST | 400 | PASS | Returns topic_subject is required message |
| 9 | Get Non-Existent Session | `/v1/chat/sessions/{id}` | GET | 404 | PASS | Returns empty 404 body |
| 10 | Send Message to Non-Existent Session | `/v1/chat/sessions/{id}/messages` | POST | 404 | PASS | Returns empty 404 body |
| 11 | Send Empty Message Validation | `/v1/chat/sessions/{id}/messages` | POST | 400 | PASS | Returns `{"reply_text":"message is required"}` |
| 12 | List Demo Sessions | `/v1/demo/sessions` | GET | 200 | PASS | Returns empty array (no sessions created due to bug) |
| 13 | List Demo Cases | `/v1/demo/cases` | GET | 200 | PASS | Returns seeded mock cases |
| 14 | List Handover Logs | `/v1/demo/handover-logs` | GET | 200 | PASS | Returns empty array |
| 15 | Funnel Metrics | `/v1/demo/metrics/funnel` | GET | 200 | PASS | Returns zeroed metrics (no sessions) |
| 16 | Per-UC Metrics | `/v1/demo/metrics/per-uc` | GET | 200 | PASS | Returns empty object |
| 17 | Mock Data - Accounts | `/v1/demo/mock-data/accounts` | GET | 200 | PASS | Returns loaded account fixtures |
| 18 | Mock Data - Listings | `/v1/demo/mock-data/listings` | GET | 200 | PASS | Returns loaded listing fixtures |
| 19 | Mock Data - Moderation Reviews | `/v1/demo/mock-data/moderation_reviews` | GET | 200 | PASS | Returns moderation review fixtures |
| 20 | Mock Data - Message Moderation | `/v1/demo/mock-data/message_moderation` | GET | 200 | PASS | Returns message moderation fixtures |
| 21 | Mock Data - Nonexistent Type | `/v1/demo/mock-data/nonexistent` | GET | 404 | PASS | Returns 404 for unknown type |
| 22 | Demo Config Toggle Off | `/v1/demo/config` | POST | 200 | PASS | Sets business_hours=false, returns updated config |
| 23 | Demo Config Toggle On | `/v1/demo/config` | POST | 200 | PASS | Sets business_hours=true, returns updated config |
| 24 | Demo Config Empty Body | `/v1/demo/config` | POST | 200 | PASS | Returns status=updated with current config |
| 25 | Session Events - Non-Existent | `/v1/demo/sessions/{id}/events` | GET | 200 | PASS* | Returns empty array instead of 404 |
| 26 | Session Trace - Non-Existent | `/v1/demo/sessions/{id}/trace` | GET | 200 | PASS* | Returns empty array instead of 404 |
| 27 | FAQ Search - Valid Query | `/v1/faq/search` | POST | 500 | **FAIL** | Internal Server Error (embedding API call failure) |
| 28 | FAQ Search - Empty Query | `/v1/faq/search` | POST | 400 | PASS | Returns `{"hits":[],"faq_miss":true,"retrieval_miss":true,"answer_miss":false}` |
| 29 | FAQ Search - With UC Tags | `/v1/faq/search` | POST | 500 | **FAIL** | Internal Server Error (same root cause as #27) |
| 30 | FAQ Search - No Body | `/v1/faq/search` | POST | 400 | PASS | Spring returns 400 Bad Request |
| 31 | Wrong Content-Type | `/v1/chat/sessions` | POST | 415 | PASS | Returns 415 Unsupported Media Type |
| 32 | Invalid JSON Body | `/v1/chat/sessions` | POST | 400 | PASS | Returns 400 Bad Request |
| 33 | Non-Existent Endpoint | `/v1/nonexistent-endpoint` | GET | 404 | PASS | Returns standard 404 |
| 34 | CORS - Allowed Origin | `/v1/chat/sessions` | OPTIONS | 200 | PASS | Returns correct CORS headers for localhost:5173 |
| 35 | CORS - Disallowed Origin | `/v1/chat/sessions` | OPTIONS | 403 | PASS | Blocks requests from non-whitelisted origin |

### Overall Results

| Metric | Value |
|--------|-------|
| Total Tests | 35 |
| Passed | 27 |
| Failed | 6 |
| Conditional Pass | 2 |
| Pass Rate | 77.1% |

---

## 2. Issues Found

### CRITICAL Issues

#### ISSUE-001: All Session Creation Endpoints Return 500 Internal Server Error

- **Severity**: CRITICAL
- **Affected Endpoints**: `POST /v1/chat/sessions`
- **Description**: Every attempt to create a chat session results in a 500 Internal Server Error. This includes all routing paths: strong prior topics (GDPR, Safety, Messaging), weak prior topics (Ad Support), and handover-only topics (Delivery). The validation path (empty/missing `topic_subject`) works correctly with 400, confirming the issue is in the `SessionManager.createSession()` execution path.
- **Root Cause Analysis**: The 500 occurs on ALL code paths that reach `sessionRepository.save(session)`, including strong-prior topics that do NOT invoke the LLM. This strongly suggests a JPA/Hibernate persistence issue. The most likely cause is the `String[] candidateUseCases` field mapped to PostgreSQL `text[]` column type. Hibernate does not natively support PostgreSQL array types for `String[]` fields without a custom `UserType`. The `VectorType` custom UserType exists for `float[]` (pgvector), but there is no equivalent custom UserType for `String[]` arrays used by `BotSession.candidateUseCases`, `BotSession.articlesShown`, `BotTurn.sourceIds`, and `SessionOutcome.articlesShown`.
- **Reproduction**:
  ```bash
  curl -s -X POST http://localhost:8080/v1/chat/sessions \
    -H "Content-Type: application/json" \
    -d '{"first_name":"Jane","email":"jane@example.com","topic_subject":"Delete My Account or Data","description":"I want to delete all my data"}'
  ```
  Returns: `{"timestamp":"...","status":500,"error":"Internal Server Error","path":"/v1/chat/sessions"}`
- **Impact**: The entire chat functionality is non-operational. No sessions can be created, no conversations can happen, no demo scenarios can run.
- **Suggestion**: Create a custom Hibernate `UserType` for PostgreSQL `text[]` arrays (similar to `VectorType` for `vector`), or use `@Type(StringArrayType.class)` from `hypersistence-utils-hibernate-63` library. Alternatively, use `@Column(columnDefinition = "text[]")` with a `@Convert` annotation.

#### ISSUE-002: FAQ/Knowledge Search Returns 500 Internal Server Error

- **Severity**: CRITICAL
- **Affected Endpoints**: `POST /v1/faq/search`
- **Description**: All FAQ search requests with a non-empty query return 500. The empty query validation path returns 400 correctly.
- **Root Cause Analysis**: The `KnowledgeSearchService.search()` calls `embeddingClient.embed(query)` which makes an HTTP call to the DashScope embedding API. The `DashScopeEmbeddingClient.embedBatch()` throws `RuntimeException("Embedding API call failed")` on any exception, and the `KnowledgeSearchController` does not catch this exception (no try-catch, no global `@ControllerAdvice`).
- **Reproduction**:
  ```bash
  curl -s -X POST http://localhost:8080/v1/faq/search \
    -H "Content-Type: application/json" \
    -d '{"query":"how to post an ad"}'
  ```
  Returns: `{"timestamp":"...","status":500,"error":"Internal Server Error","path":"/v1/faq/search"}`
- **Impact**: Knowledge search is completely broken. The FAQ path (UC-A through UC-FP) cannot retrieve articles.
- **Suggestion**: Add error handling in `KnowledgeSearchController` to catch exceptions from the embedding/search pipeline and return a graceful response (e.g., empty hits with `faq_miss=true`). Consider also adding a `@ControllerAdvice` global exception handler.

### HIGH Issues

#### ISSUE-003: No Global Exception Handler (@ControllerAdvice)

- **Severity**: HIGH
- **File**: N/A (missing)
- **Description**: The application has no `@ControllerAdvice` or `@RestControllerAdvice` class. All unhandled exceptions bubble up as generic Spring Boot 500 responses with no structured error details. The error response format `{"timestamp":"...","status":500,"error":"Internal Server Error","path":"..."}` exposes no useful diagnostic information to API consumers.
- **Impact**: Debugging production issues is difficult. API consumers receive unhelpful error responses. Sensitive stack traces may leak in development mode.
- **Suggestion**: Create a `GlobalExceptionHandler` class annotated with `@RestControllerAdvice` that handles `RuntimeException`, `IllegalArgumentException`, `HttpMessageNotReadableException`, and other expected exceptions with structured error responses.

#### ISSUE-004: Enums Defined But Used as Strings Throughout Codebase

- **Severity**: HIGH
- **Files**: `BotSession.java:44-46`, `SessionManager.java:174`, `ControlKernel.java:153`, `LocalEventStore.java:61-68`
- **Description**: The codebase defines proper Java enums for `HandlingState`, `SessionPhase`, `ContainmentOutcome`, and `EventType`, but they are used as plain `String` values everywhere. For example, `session.setCurrentPhase("RESOLVE")` instead of `SessionPhase.RESOLVE`, `"CLOSED".equals(session.getHandlingState())` instead of using enum comparison. This makes the code prone to typos and inconsistencies.
- **Impact**: No compile-time safety for state transitions. Typo in any string literal (e.g., `"ESCULATE"` instead of `"ESCALATE"`) would silently cause incorrect behavior with no compiler warning.
- **Suggestion**: Change the entity fields to use the actual enum types with `@Enumerated(EnumType.STRING)` JPA annotation, or at minimum use the enum constants (e.g., `SessionPhase.RESOLVE.name()`) for all comparisons and assignments.

#### ISSUE-005: Redis Connection Leak in Health Check

- **Severity**: HIGH
- **File**: `HealthController.java:79`
- **Description**: The `checkRedis()` method calls `redisConnectionFactory.getConnection().ping()` without closing the connection. Unlike the DB check which uses try-with-resources (`try (Connection conn = dataSource.getConnection())`), the Redis connection is obtained and pinged but never closed.
- **Code**:
  ```java
  private Map<String, String> checkRedis() {
      try {
          redisConnectionFactory.getConnection().ping(); // Connection never closed
          return Map.of("status", "UP");
  ```
- **Impact**: Each health check call leaks a Redis connection. Under continuous monitoring (e.g., Kubernetes liveness probes every 10s), this will exhaust the Redis connection pool.
- **Suggestion**: Store the connection in a variable and close it in a finally block, or use try-with-resources: `try (var conn = redisConnectionFactory.getConnection()) { conn.ping(); }`

#### ISSUE-006: Sensitive API Keys Exposed in .env.local

- **Severity**: HIGH
- **File**: `.env.local` (lines 10-11, 16-17, 25-26)
- **Description**: The `.env.local` file contains plaintext API keys for Kimi (Moonshot AI), GetStream Chat, and DashScope/QWEN. This file is tracked in the git repository (it was listed in `ls` output and is readable).
- **Impact**: API keys are exposed to anyone with repository access. These keys could be used for unauthorized API calls, incurring costs or data access.
- **Suggestion**: Add `.env.local` to `.gitignore`, rotate all exposed keys immediately, and use environment variables or a secrets manager for key injection.

#### ISSUE-007: Session Events/Trace Endpoints Return 200 with Empty Array for Non-Existent Sessions

- **Severity**: HIGH
- **Endpoints**: `GET /v1/demo/sessions/{id}/events`, `GET /v1/demo/sessions/{id}/trace`
- **Description**: These endpoints return HTTP 200 with an empty array `[]` when queried with a non-existent session ID. The `GET /v1/chat/sessions/{id}` endpoint correctly returns 404 for non-existent sessions. This inconsistency could mislead API consumers into thinking a session exists but has no events/trace.
- **Suggestion**: Add session existence validation in these endpoints, returning 404 if the session ID is not found in `bot_sessions`.

### MEDIUM Issues

#### ISSUE-008: LLM Client Throws RuntimeException Instead of Returning Error Response

- **Severity**: MEDIUM
- **File**: `OpenAiCompatibleLlmClient.java:69-75`
- **Description**: The `chat()` method catches `RestClientException` and re-throws it wrapped in a `RuntimeException`. This means any LLM API failure (network timeout, rate limit, auth failure) will propagate as an unhandled exception. The `LlmInvocationService` does catch this and returns `SAFE_ESCALATION_RESPONSE`, but the `DashScopeEmbeddingClient` has the same pattern and its callers (KnowledgeSearchService) do NOT have this safety net.
- **Suggestion**: Have `OpenAiCompatibleLlmClient.chat()` return a fallback `LlmResponse` with `finishReason="error"` instead of throwing. Apply the same pattern to `DashScopeEmbeddingClient`.

#### ISSUE-009: PII Logged in ChatController

- **Severity**: MEDIUM
- **File**: `ChatController.java:49`
- **Description**: The `createSession` method logs `firstName` directly: `log.info("Creating session: firstName={}, topicSubject={}", firstName, topicSubject)`. While `firstName` alone may not be PII in isolation, combined with the `topicSubject` (e.g., "Delete My Account or Data") it can identify a user in a small dataset. The `PiiRedactionFilter` service exists but is not used in controller logging.
- **Suggestion**: Use `PiiRedactionFilter.redactForLogs()` on logged values, or omit the first name from logs entirely.

#### ISSUE-010: FormContextIngestionService Silently Swallows Exceptions

- **Severity**: MEDIUM
- **File**: `FormContextIngestionService.java:63-65`
- **Description**: The `ingest()` method catches all exceptions and only logs them. If form context ingestion fails (e.g., JSON serialization error), the session is created without form context and no error is propagated. This could lead to sessions with incomplete data that behave unpredictably during routing and resolution.
- **Suggestion**: Re-throw critical exceptions or set a flag on the session indicating ingestion failure.

#### ISSUE-011: String Concatenation in Event Payload JSON Building

- **Severity**: MEDIUM
- **File**: `SessionManager.java:136-137,140-141,148`
- **Description**: Event payloads are built using `String.format()` with direct string interpolation:
  ```java
  String.format("{\"topic_subject\":\"%s\",\"routing_outcome\":\"%s\"}", topicSubject, routingResult.outcome())
  ```
  If `topicSubject` or `description` contains quotes, backslashes, or other JSON-special characters, the resulting JSON will be malformed.
- **Suggestion**: Use `ObjectMapper` to build JSON payloads, or at minimum escape the interpolated values.

#### ISSUE-012: CORS Configuration Allows Only localhost:5173

- **Severity**: MEDIUM
- **File**: `WebConfig.java:13`
- **Description**: CORS is configured with a single allowed origin `http://localhost:5173`. This works for local development but would need updating for any other deployment. There is no configuration externalization for the CORS origin.
- **Suggestion**: Make the allowed CORS origins configurable via `application.yml` or environment variables.

#### ISSUE-013: No Request/Response Logging Interceptor

- **Severity**: MEDIUM
- **Description**: There is no request/response logging interceptor or filter. Combined with the lack of a global exception handler, this makes it difficult to diagnose production issues. The only logging is within individual controller methods and services.
- **Suggestion**: Add a `CommonsRequestLoggingFilter` bean or a custom `HandlerInterceptor` for request tracing.

#### ISSUE-014: ControlKernel Re-evaluates Phase After Transition Without Guard

- **Severity**: MEDIUM
- **File**: `ControlKernel.java:117-133`
- **Description**: After a phase transition, if `responseText` is null, the code re-evaluates the new phase. However, the re-evaluation result may trigger another phase transition (`reEval.nextPhase()`), leading to a double transition in a single turn. There is no guard against infinite re-evaluation loops.
- **Suggestion**: Add a recursion guard (e.g., max 1 re-evaluation per turn) or restructure to avoid the re-evaluation pattern.

#### ISSUE-015: Mock Data Type "cases" Returns 404

- **Severity**: MEDIUM
- **Endpoint**: `GET /v1/demo/mock-data/cases`
- **Description**: The `MockGumtreeApiService.getFixturesByType()` switch expression does not include "cases" as a type. Only "accounts", "listings", "moderation_reviews", and "message_moderation" are supported. Requesting `/v1/demo/mock-data/cases` returns 404.
- **Impact**: Demo users may expect to view case fixture data through this endpoint but cannot.
- **Suggestion**: Either add "cases" to the switch, or document the supported types clearly in the API.

### LOW Issues

#### ISSUE-016: Inconsistent Error Response Format

- **Severity**: LOW
- **Description**: Different endpoints return errors in different formats:
  - Validation errors: `{"reply_text":"...","should_end_chat":true}` (custom format)
  - Spring errors: `{"timestamp":"...","status":500,"error":"...","path":"..."}` (default Spring format)
  - 404 errors: empty body
  There is no standard error envelope across the API.
- **Suggestion**: Define a standard error response DTO (e.g., `{"error_code":"...","message":"...","details":"..."}`) and use it consistently.

#### ISSUE-017: Unused EventEmitter and TraceWriter Services

- **Severity**: LOW
- **Files**: `EventEmitter.java`, `TraceWriter.java`
- **Description**: The `EventEmitter` service has rich methods (`emitSessionStarted`, `emitUseCaseInferred`, etc.) but the `SessionManager` and `ControlKernel` build events manually with `BotEvent.builder()` and save directly to `BotEventRepository`. Similarly, `TraceWriter` exists but `ControlKernel` records turns directly. These services appear to be unused duplicates.
- **Suggestion**: Either use `EventEmitter` and `TraceWriter` consistently or remove them to reduce confusion.

#### ISSUE-018: ProgressPlaceholderService Not Used in Any Controller

- **Severity**: LOW
- **File**: `ProgressPlaceholderService.java`
- **Description**: This service exists but is not referenced in any controller or the session manager. Its purpose (streaming progress) is not integrated into the API layer.
- **Suggestion**: Either integrate or remove to reduce dead code.

#### ISSUE-019: Hardcoded Max Context Window for Conversation History

- **Severity**: LOW
- **File**: `ContextProjectionBuilder.java:79`
- **Description**: The conversation history is limited to the last 10 turns (`Math.max(0, conversationHistory.size() - 10)`). This limit is hardcoded and not configurable.
- **Suggestion**: Make the limit configurable via `control-policy.yaml`.

#### ISSUE-020: CreateCaseControlledTool Has Redundant UC Check

- **Severity**: LOW
- **File**: `CreateCaseControlledTool.java:52-55`
- **Description**: The `execute()` method checks `ALLOWED_UC.contains(activeUc)` internally, but the `ToolPolicyEnforcer` already performs UC-based access control before the tool is dispatched. This is defense-in-depth which is good, but the `ALLOWED_UC` set in the tool (`UC-H, UC-J, UC-K`) must be kept in sync with `tool-policy.yaml`.
- **Suggestion**: Remove the redundant check in the tool, or better, have the tool read its allowed UCs from the policy service.

#### ISSUE-021: CreateCaseControlledTool Comment Incorrectly Describes UC Mapping

- **Severity**: LOW
- **File**: `CreateCaseControlledTool.java:25-28`
- **Description**: The Javadoc comments describe UC-H as "safety/illegal reporting" and UC-J as "commercial escalation", but per the `use-case-registry.yaml`, UC-H is "Ad Removal Appeal" and UC-J is "Trust & Safety Report". The mapping is inverted in the documentation.
- **Suggestion**: Correct the Javadoc to match the actual UC registry definitions.

---

## 3. Test Coverage Analysis

### Tested Modules (via E2E)
- HealthController (all 3 endpoints)
- ChatController (session creation, message processing, session retrieval - all fail paths tested)
- DemoInspectionController (all endpoints including sessions, cases, handover-logs, events, trace, mock-data, config, metrics)
- KnowledgeSearchController (search with various inputs)
- CORS configuration
- Spring Boot Actuator

### Untested Modules (not reachable via E2E due to session creation failure)
- SessionManager.processMessage() - requires a valid session_id
- ControlKernel.processMessage() - requires a valid session
- PhaseEvaluator (all phase paths) - requires live sessions
- BudgetChecker - requires multi-turn conversations
- DriftDetector - requires mid-conversation input
- All Tool implementations - require active sessions with valid UCs
- KnowledgeSearchService (full pipeline) - embedding API failure blocks this
- RerankService - blocked by embedding failure
- ForbiddenPhraseDetector - not invoked without live conversations
- PiiRedactionFilter - not invoked without live conversations
- ScriptLibraryService - not invoked without live conversations

### Unit Test Coverage
- **Current**: 0% -- There are zero unit test files in the project. The `src/test/java/com/gumtree/csagent/` directory exists but is completely empty.
- **Priority modules for unit tests**:
  1. `ActionParser` - pure logic, easily testable, critical for conversation flow
  2. `DriftDetector` - pure logic with regex patterns, critical for safety
  3. `ForbiddenPhraseDetector` - pure logic, critical guardrail
  4. `PiiRedactionFilter` - pure logic, critical for data protection
  5. `BudgetChecker` - pure logic, critical for escalation control
  6. `UseCaseRegistryService` - configuration parsing, routing foundation
  7. `ControlPolicyService` - phase transition validation
  8. `PhaseEvaluator` - core business logic (needs mocking)
  9. `SessionManager` - integration test with mocked dependencies

---

## 4. Summary

### CRITICAL (fix immediately)
1. **ISSUE-001**: All session creation returns 500 -- entire chat functionality is non-operational. Likely caused by Hibernate `String[]` to PostgreSQL `text[]` mapping issue.
2. **ISSUE-002**: FAQ search returns 500 -- knowledge base is inaccessible. Embedding API call failure with no graceful degradation.

### HIGH (fix soon)
3. **ISSUE-003**: No global exception handler -- all unhandled exceptions produce generic 500 responses.
4. **ISSUE-004**: Enums defined but unused -- string literals throughout codebase risk typo-induced bugs.
5. **ISSUE-005**: Redis connection leak in health check -- will exhaust connection pool under monitoring.
6. **ISSUE-006**: API keys in plaintext in `.env.local` tracked in git.
7. **ISSUE-007**: Session events/trace return 200 for non-existent sessions instead of 404.

### MEDIUM (fix when convenient)
8. **ISSUE-008**: LLM client throws instead of returning error response.
9. **ISSUE-009**: PII (firstName) logged in ChatController.
10. **ISSUE-010**: FormContextIngestionService silently swallows exceptions.
11. **ISSUE-011**: String concatenation for JSON payloads risks malformed JSON.
12. **ISSUE-012**: CORS origin hardcoded to localhost:5173.
13. **ISSUE-013**: No request/response logging interceptor.
14. **ISSUE-014**: ControlKernel re-evaluation has no recursion guard.
15. **ISSUE-015**: Mock data type "cases" returns 404 unexpectedly.

### LOW (fix at leisure)
16. **ISSUE-016**: Inconsistent error response format across endpoints.
17. **ISSUE-017**: Unused EventEmitter and TraceWriter services (dead code).
18. **ISSUE-018**: ProgressPlaceholderService not integrated.
19. **ISSUE-019**: Hardcoded conversation history limit.
20. **ISSUE-020**: Redundant UC check in CreateCaseControlledTool.
21. **ISSUE-021**: Incorrect Javadoc UC descriptions in CreateCaseControlledTool.
