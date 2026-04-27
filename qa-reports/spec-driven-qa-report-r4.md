# QA Report - R4: Connection Error Investigation & Full Regression

**Generated**: 2026-04-23 14:15
**Scope**: LLM connection error diagnosis, error handling assessment, full API regression
**Tech Stack**: Java 17, Spring Boot 3.2.5, JUnit 5 + Mockito, PostgreSQL + pgvector, Redis, DashScope/Kimi LLM APIs

---

## 1. Connection Error Diagnosis

### Root Cause

The `java.net.ConnectException: Connection refused` originates from `OpenAiCompatibleLlmClient.java:64` when calling the DashScope API at `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`.

**Evidence from logs** (`/tmp/csagent-backend.log`):
```
org.springframework.web.client.ResourceAccessException: I/O error on POST request for
"https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions": Connection refused
Caused by: java.net.ConnectException: Connection refused
```

**Network reachability test results**:
- DashScope API (`https://dashscope.aliyuncs.com/...`): HTTP 400 (reachable but auth required)
- Kimi API (`https://api.moonshot.ai/...`): HTTP 404 (reachable but wrong endpoint without path)
- `DASHSCOPE_API_KEY`: NOT SET in environment
- `KIMI_API_KEY`: NOT SET in environment

**Diagnosis**: The API endpoints are network-reachable (not a firewall/DNS issue). The `Connection refused` error likely stems from the Java process being behind a proxy or VPN configuration that differs from the shell `curl` environment, OR the API key being blank causes the fallback to Kimi (whose base URL resolves differently). When `DASHSCOPE_API_KEY` is blank, the code falls back to Kimi (line 49-53 of `OpenAiCompatibleLlmClient.java`), but Kimi's key is also blank, producing an auth failure that manifests as `Connection refused` under certain network conditions.

---

## 2. Code Review Findings

### Summary

| # | Severity | File | Line | Issue | Suggestion |
|---|----------|------|------|-------|------------|
| 1 | **HIGH** | OpenAiCompatibleLlmClient.java | 35 | No timeout configured on RestTemplate | Add connect/read timeouts |
| 2 | **HIGH** | PhaseEvaluator.java | 116-125 | DISCOVER phase does not handle `escalate_human` from safe fallback | Check parsed action and escalate if action is `escalate_human` |
| 3 | **HIGH** | OpenAiCompatibleLlmClient.java | 35 | No retry/circuit-breaker for LLM calls | Add resilience4j or similar |
| 4 | **MEDIUM** | DashScopeEmbeddingClient.java | 33 | No timeout configured on RestTemplate | Add connect/read timeouts |
| 5 | **MEDIUM** | OpenAiCompatibleLlmClient.java | 49-53 | Silent fallback from DashScope to Kimi with no logging | Log the fallback decision |
| 6 | **MEDIUM** | GlobalExceptionHandler.java | 24-30 | Leaks internal exception messages to HTTP response | Sanitize error messages in production |
| 7 | **MEDIUM** | ControlKernel.java | 100-101 | Turn history queried inside processMessage but session not yet saved | Potential stale data if concurrent requests |
| 8 | **MEDIUM** | RerankService.java | 55 | Sequential LLM calls for N candidates (O(N) API calls) | Batch or parallelize rerank scoring |
| 9 | **LOW** | ChatController.java | 33 | Raw `Map<String, String>` instead of typed DTO | Create a `CreateSessionRequest` DTO |
| 10 | **LOW** | SessionManager.java | 158 | String format in event payload not JSON-escaped | Use ObjectMapper for event payloads |
| 11 | **LOW** | PhaseEvaluator.java | 211-231 | Defense-in-depth retry can double LLM latency | Acceptable tradeoff, but add metric tracking |

### Details

#### 1. [HIGH] No Timeout on RestTemplate -- Indefinite Hang Risk
- **File**: `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java:35`
- **Severity**: HIGH
- **Description**: `new RestTemplate()` creates a client with **no** connect timeout and **no** read timeout. If the LLM API is unreachable or slow, the request will hang indefinitely, blocking a Tomcat thread. Under load, this can cause thread pool exhaustion and total service unavailability.
- **Code**: `this.restTemplate = new RestTemplate();`
- **Suggestion**: Configure `SimpleClientHttpRequestFactory` with `setConnectTimeout(5000)` and `setReadTimeout(30000)`, or use `RestTemplateBuilder` with timeout configuration.

#### 2. [HIGH] Safe Escalation Response Not Acted Upon -- Session Stuck in DISCOVER
- **File**: `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:116-125`
- **Severity**: HIGH
- **Description**: When the LLM fails, `LlmInvocationService.invokeChat()` returns a `SAFE_ESCALATION_RESPONSE` containing JSON with `"action":"escalate_human"`. The `ActionParser` correctly parses this into a `ParsedAction` with `action="escalate_human"`. However, the `evaluateDiscover()` method in `PhaseEvaluator` returns `PhaseResult.respond(...)` **without checking** if the parsed action is `escalate_human`. The `ControlKernel` then sets `shouldEndChat` based on whether the phase is `CLOSE` or `ESCALATE`, but since the phase never transitions, `shouldEndChat` remains `false`. The user sees "Let me connect you with a human agent" but the session stays open and they can send more messages -- receiving the same error message in an infinite loop.
- **Reproduction**: Create a session when the LLM API is down. Send any message. Response says "technical issue...connect with human agent" but `should_end_chat=false`. Send another message. Same response. Infinite loop.
- **Suggestion**: In `evaluateDiscover()` (and `evaluateResolve()`), check `if ("escalate_human".equals(action.getAction()))` and return `PhaseResult.escalate(...)` instead of `PhaseResult.respond(...)`.

#### 3. [HIGH] No Retry or Circuit Breaker for LLM API
- **File**: `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java`
- **Severity**: HIGH
- **Description**: Transient network errors (DNS hiccup, TCP reset) cause immediate failure with no retry attempt. There is also no circuit breaker to prevent cascading failures when the LLM API is persistently down.
- **Suggestion**: Add at least one retry with exponential backoff for transient errors (ConnectException, SocketTimeoutException). Consider resilience4j CircuitBreaker for production.

#### 4. [MEDIUM] No Timeout on Embedding Client RestTemplate
- **File**: `server/src/main/java/com/gumtree/csagent/service/embedding/DashScopeEmbeddingClient.java:33`
- **Severity**: MEDIUM
- **Description**: Same issue as #1 -- `new RestTemplate()` with no timeouts. Embedding API calls can hang indefinitely.
- **Suggestion**: Same fix as #1.

#### 5. [MEDIUM] Silent DashScope-to-Kimi Fallback
- **File**: `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java:49-53`
- **Severity**: MEDIUM
- **Description**: When `DASHSCOPE_API_KEY` is blank, the code silently falls back to Kimi with no log message. This makes debugging connection failures harder -- the error message shows a DashScope URL but the actual call may be to Kimi.
- **Suggestion**: Add `log.info("DashScope API key not configured, falling back to Kimi")` when switching providers.

#### 6. [MEDIUM] Internal Exception Message Leaked in HTTP Response
- **File**: `server/src/main/java/com/gumtree/csagent/controller/GlobalExceptionHandler.java:24-30`
- **Severity**: MEDIUM
- **Description**: `ex.getMessage()` is returned directly in the HTTP 500 response body. This can expose internal details (class names, SQL errors, file paths) to external callers.
- **Suggestion**: Return a generic error message in production. Log the full exception server-side.

#### 7. [MEDIUM] Potential Stale Conversation History
- **File**: `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:100`
- **Severity**: MEDIUM
- **Description**: `turnRepository.findBySessionIdOrderByTurnIndex()` is called before the current turn is saved. If two requests arrive concurrently for the same session, both will get the same stale history. The `@Transactional` on `SessionManager.processMessage()` provides some protection, but there is no row-level lock on the session.
- **Suggestion**: Add `SELECT ... FOR UPDATE` or optimistic locking (`@Version`) on `BotSession` to prevent concurrent turn processing.

#### 8. [MEDIUM] Sequential LLM Calls for Reranking
- **File**: `server/src/main/java/com/gumtree/csagent/service/knowledge/RerankService.java:55`
- **Severity**: MEDIUM
- **Description**: The `rerank()` method makes N sequential LLM calls (one per candidate chunk, default up to 8). Each call has full LLM latency (100-2000ms), making reranking 800-16000ms total.
- **Suggestion**: Parallelize using `CompletableFuture.supplyAsync()` or batch the scoring into a single LLM call.

#### 9. [LOW] Raw Map Used as Request Body
- **File**: `server/src/main/java/com/gumtree/csagent/controller/ChatController.java:33`
- **Severity**: LOW
- **Description**: `@RequestBody Map<String, String>` loses type safety and documentation. Request validation relies on manual null checks.
- **Suggestion**: Create `CreateSessionRequest` and `SendMessageRequest` DTOs with `@NotBlank` annotations.

#### 10. [LOW] String Format in Event Payload Not JSON-Escaped
- **File**: `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:158`
- **Severity**: LOW
- **Description**: `String.format("{\"topic_subject\":\"%s\",...}")` does not escape the `topicSubject` string. If the topic contains quotes or backslashes, the JSON becomes invalid.
- **Suggestion**: Use `ObjectMapper.writeValueAsString()` to build event payloads.

---

## 3. Test Execution Results

### Unit Test Summary

| Test File | Total | Pass | Fail | Skip |
|-----------|-------|------|------|------|
| ActionParserTest | 14 | 14 | 0 | 0 |
| DriftDetectorTest | 18 | 18 | 0 | 0 |
| ForbiddenPhraseDetectorTest | 19 | 19 | 0 | 0 |
| PiiRedactionFilterTest | 16 | 16 | 0 | 0 |
| FormContextIngestionServiceTest | 9 | 9 | 0 | 0 |
| LlmInvocationServiceTest | 9 | 9 | 0 | 0 |
| OpenAiCompatibleLlmClientTest | 7 | 7 | 0 | 0 |
| **Total** | **92** | **92** | **0** | **0** |

### API Regression Test Summary

| Test # | Scenario | Result | Notes |
|--------|----------|--------|-------|
| T-1 | Create session (Ad Support) | PASS | Session created, ambiguous greeting |
| T-2 | Send message (exercises LLM) | **FAIL** | Graceful fallback shown but shouldEndChat=false (BUG) |
| T-3 | Strong prior routing (UC-G) | PASS | intent=UC-G, correct |
| T-4 | OOS routing (Delivery) | PASS | shouldEndChat=true, correct |
| T-5 | Knowledge base search | **FAIL** | 0 hits, faq_miss=true (embedding API also failing) |
| T-6a | Intake multi-turn (turn 1) | **FAIL** | Shows "technical issue" instead of intake template |
| T-6b | Intake multi-turn (turn 2) | **FAIL** | Shows "technical issue", session not escalated |
| T-7 | User escalation request | PASS | shouldEndChat=true, correct |
| T-8 | Handover payload completeness | PASS | 22 fields per payload |
| T-9 | Missing required field | PASS | Returns "topic_subject is required" |
| T-10 | Empty message | PASS | Returns "message is required" |
| T-11 | Non-existent session | PASS | HTTP 404 |
| T-12 | UC-G intake session | PASS | intent=UC-G, correct greeting |
| T-13 | Already-escalated session | PASS | shouldEndChat=true |
| T-14 | UC-G intake follow-up | PASS | Template-based response (no LLM needed) |
| T-15 | XSS injection | PASS | Script tags stripped |
| T-16 | SQL injection | PASS | Session created normally |
| T-17 | Drift detection (scam -> UC-J) | PASS | shouldEndChat=true |
| T-18 | Session GET endpoint | PASS | Returns correct state |
| T-20 | Very long message (10K chars) | PASS | Session created |
| T-21 | Unicode handling | PASS | Session created |

**Pass Rate**: 17/21 (81.0%)

### Failed Tests Details

#### T-2: Send Message -- Safe Escalation Not Ending Session
- **Expected**: `should_end_chat=true` when LLM returns safe escalation
- **Actual**: `should_end_chat=false`, session stays in DISCOVER, user sees "technical issue" text
- **Likely cause**: `PhaseEvaluator.evaluateDiscover()` does not check for `escalate_human` action

#### T-5: Knowledge Base Search Returns 0 Hits
- **Expected**: Non-zero hits for "how to post an ad"
- **Actual**: 0 hits, faq_miss=true
- **Likely cause**: Embedding API (DashScope) also fails with ConnectException, causing retrieval_miss

#### T-6a/T-6b: Intake Multi-Turn Shows Error Instead of Template
- **Expected**: Intake template response on first turn, LLM-guided intake on second
- **Actual**: Both turns show "I'm experiencing a technical issue"
- **Likely cause**: Session has no activeUseCase (LLM routing failed), so it enters DISCOVER phase which invokes LLM (which fails)

---

## 4. Test Coverage

### Tested Modules (Unit Tests)
- `ActionParser` -- JSON parsing, markdown stripping, fallback
- `DriftDetector` -- escalation detection, hard shift keywords
- `ForbiddenPhraseDetector` -- all categories, sanitization
- `PiiRedactionFilter` -- email, phone, postcode, ad_id across all layers
- `FormContextIngestionService` -- XSS sanitization
- `LlmInvocationService` -- graceful degradation on LLM failure
- `OpenAiCompatibleLlmClient` -- connection errors, key fallback

### Untested Modules
- `ControlKernel` -- requires full Spring context with repositories
- `PhaseEvaluator` -- requires mocking of 7 dependencies
- `SessionManager` -- requires DB/repositories
- `UseCaseRouter` -- requires registry + LLM mocking
- `BudgetChecker` -- requires ControlPolicyService + UseCaseRegistryService
- `ContextProjectionBuilder` -- straightforward but untested
- `KnowledgeSearchService` -- requires embedding client + repositories
- `RerankService` -- requires LLM client mocking
- `ToolDispatcher` / `ToolPolicyEnforcer` -- tool routing logic
- All `Tool` implementations

### Priority Modules to Add Tests
1. **PhaseEvaluator** -- highest risk, contains the discovered stuck-session bug
2. **ControlKernel** -- core orchestration, budget checking, drift handling
3. **UseCaseRouter** -- LLM fallback path, single-candidate handling
4. **KnowledgeSearchService** -- retrieval gate, answer gate, rerank pipeline

---

## 5. Summary

### HIGH Priority (fix immediately)

1. **Session stuck in infinite loop when LLM fails** (PhaseEvaluator.java:116-125) -- User sees "connecting to human agent" but session never ends. The `evaluateDiscover()` method returns the safe escalation message as a normal response without transitioning to ESCALATE phase. Same issue likely affects `resolveIntake()` and `resolveFaq()` when the LLM safe fallback fires.

2. **No timeout on RestTemplate** (OpenAiCompatibleLlmClient.java:35, DashScopeEmbeddingClient.java:33) -- Requests to unresponsive LLM/embedding APIs can hang indefinitely, eventually exhausting the Tomcat thread pool and making the entire backend unresponsive.

3. **No retry for transient LLM failures** (OpenAiCompatibleLlmClient.java) -- A single TCP reset or DNS timeout immediately fails the request with no retry, even though the LLM API may be available 100ms later.

### MEDIUM Priority (fix soon)

4. Silent DashScope-to-Kimi fallback makes debugging harder
5. Internal exception messages leaked in HTTP 500 responses (security)
6. No concurrency protection on session message processing
7. Sequential rerank scoring causes O(N) latency multiplier

### LOW Priority (fix when convenient)

8. Raw `Map` request bodies instead of typed DTOs
9. String-format JSON payloads not properly escaped
10. Defense-in-depth retry strategy adds latency (already acceptable tradeoff)

### Metrics
- **Issues found**: 3 HIGH, 4 MEDIUM, 3 LOW
- **Unit test pass rate**: 92/92 (100%)
- **API regression pass rate**: 17/21 (81.0%)
- **Critical finding**: Session stuck in infinite error loop when LLM API is down
