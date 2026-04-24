# QA Report - Wave 1: D11 Iteration 1 ("Bot变聪明") Features

**Generated**: 2026-04-23 15:30
**Scope**: D11.1 (Auto-trigger get_customer_context), D11.2 (Context Projection Enhancement), D11.3 (create_case_controlled Integration)
**Tech Stack**: Java 17, Spring Boot 3.2.5, JUnit 5 + Mockito, Maven
**Server**: http://localhost:8080 (running, health=UP)

---

## 1. Code Review Findings

### Summary

| # | Severity | File | Line | Issue | Suggestion |
|---|----------|------|------|-------|------------|
| 1 | HIGH | DriftDetector.java | 62 | Drift detector triggers HARD_SHIFT for intake UCs even when `activeUseCase` is null (DISCOVER phase), bypassing the intake flow and case creation | Add null-UC guard: when activeUseCase is null, do not treat keyword match as drift -- let the routing/discover flow identify the UC first |
| 2 | HIGH | ControlKernel.java | 166 | `forceEscalate()` does not call `createCaseIfAllowed()`, so intake UCs (UC-H/J/K) that escalate via drift or budget bypass never get a case created | Add case creation logic to `forceEscalate()` for intake UCs before escalation |
| 3 | MEDIUM | ControlKernel.java | 211-257 | `recordTurn()` never populates `projectedContext` field on BotTurn, making the trace endpoint always return null for this field | Pass the projection string from PhaseEvaluator result and store it in the turn record |
| 4 | MEDIUM | ChatController.java | 33 | Request body uses `Map<String, String>` which loses type safety; API contract docs say `topicSubject` but code expects `topic_subject` | Document the actual snake_case contract or use a typed DTO with `@JsonProperty` |
| 5 | MEDIUM | SessionManager.java | 99-106 | When routing fails with exception, the session proceeds with AMBIGUOUS result using empty candidate list, but no event is emitted for the routing failure | Emit a ROUTING_FAILED event for observability |
| 6 | MEDIUM | PhaseEvaluator.java | 413-463 | `createCaseIfAllowed()` catches all exceptions silently, which could mask persistent failures (e.g., DB connection issue) across multiple sessions | At minimum, emit a CASE_CREATION_FAILED event so failures are observable |
| 7 | LOW | FormContextIngestionService.java | 48-52 | `sanitize()` is called twice for all form fields: once in `SessionManager.createSession()` (line 69-73) and again in `FormContextIngestionService.ingest()` | Remove the duplicate sanitization in one location |
| 8 | LOW | GetCustomerContextTool.java | 65-78 | `sanitizeAccount()` always includes `account_type`, `creation_date` etc. even when they are null, resulting in a map with explicit null values | Filter out null entries or use `Map.of()` for cleaner JSON output |

### Details

#### Issue 1: DriftDetector Incorrectly Fires for Sessions in DISCOVER Phase with No Active UC

- **File**: `DriftDetector.java:62`
- **Severity**: HIGH
- **Description**: When a session has `activeUseCase == null` (common when routing was AMBIGUOUS for weak-prior topics like "Ad Support"), the DriftDetector condition `if (activeUc == null || !dk.targetUc.equals(activeUc))` is always true. This means ANY hard-shift keyword in the user's first message triggers a HARD_SHIFT drift, which forces escalation via `ControlKernel.forceEscalate()` -- completely bypassing the normal intake flow where `createCaseIfAllowed()` would be called. This is the root cause of why UC-H sessions from "Ad Support" topic never get a case created.
- **Code**:
  ```java
  // DriftDetector.java line 58-69
  for (DriftKeyword dk : HARD_SHIFT_KEYWORDS) {
      for (String keyword : dk.keywords) {
          if (lowerMessage.contains(keyword)) {
              if (activeUc == null || !dk.targetUc.equals(activeUc)) {
                  // This triggers even when activeUc is null (DISCOVER phase)
                  return DriftResult.builder()
                          .type(DriftType.HARD_SHIFT)
                          .newUseCase(dk.targetUc)
                          .build();
              }
          }
      }
  }
  ```
- **Suggestion**: When `activeUseCase` is null, treat the keyword match as a routing hint (set the activeUseCase and transition to RESOLVE) instead of a hard drift. Alternatively, skip drift detection entirely when `activeUseCase` is null.

#### Issue 2: forceEscalate Bypasses Case Creation for Intake UCs

- **File**: `ControlKernel.java:166-201`
- **Severity**: HIGH
- **Description**: `forceEscalate()` handles budget-exceeded and drift-based escalations. It transitions directly to ESCALATE phase and records the turn, but never calls `PhaseEvaluator.createCaseIfAllowed()`. For intake UCs (UC-H, UC-J, UC-K), this means cases are never created when escalation happens through the drift or budget path. Only escalations through the normal `PhaseEvaluator.resolveIntake()` flow trigger case creation.
- **Suggestion**: Add a `createCaseIfAllowed()` call in `forceEscalate()` when the session's activeUseCase is an intake UC (UC-H, UC-J, UC-K), or refactor case creation into a shared method callable from both paths.

#### Issue 3: projectedContext Never Stored in Turn Trace

- **File**: `ControlKernel.java:211-257`
- **Severity**: MEDIUM
- **Description**: The `recordTurn()` method builds a `BotTurn` record but never sets the `projectedContext` field, even though the field exists on the `BotTurn` entity and is exposed through the `/v1/demo/sessions/{id}/trace` endpoint. The `ContextProjectionBuilder.buildProjection()` is called inside `PhaseEvaluator` but the result is not propagated back through `PhaseResult`. This makes D11.2's new projection fields (task_summary, allowed_actions, risk_flags, budget_state, tool_schemas) unverifiable through the trace endpoint.
- **Suggestion**: Add the projected context string to `PhaseResult` and populate it in `ControlKernel.recordTurn()`. Alternatively, use the `TraceWriter` service which already has a `projectedContext` parameter.

---

## 2. Test Execution Results

### 2a. Unit Test Results

| Test File | Total | Pass | Fail | Skip |
|-----------|-------|------|------|------|
| ContextProjectionBuilderTest | 14 | 14 | 0 | 0 |
| CreateCaseControlledToolTest | 26 | 26 | 0 | 0 |
| GetCustomerContextToolTest | 10 | 10 | 0 | 0 |
| DriftDetectorTest (existing) | 18 | 18 | 0 | 0 |
| FormContextIngestionServiceTest (existing) | 9 | 9 | 0 | 0 |
| ActionParserTest (existing) | 14 | 14 | 0 | 0 |
| LlmInvocationServiceTest (existing) | 9 | 9 | 0 | 0 |
| OpenAiCompatibleLlmClientTest (existing) | 6 | 6 | 0 | 0 |
| PiiRedactionFilterTest (existing) | 18 | 18 | 0 | 0 |
| ForbiddenPhraseDetectorTest (existing) | 18 | 18 | 0 | 0 |
| **Total** | **142** | **142** | **0** | **0** |

### 2b. Integration / API Test Results

| Test ID | Feature | Description | Result | Notes |
|---------|---------|-------------|--------|-------|
| Test 1 | D11.1 | Customer context auto-trigger with email + Ad Support topic | **PASS** | customerContext populated with account data; listingContext populated with live_ad fixture; moderationContext=null (listing status=LIVE, correctly skipped) |
| Test 2 | D11.1 | Customer context skipped for UC-G (not in allowed set) | **PASS** | UC-G strong prior routed directly; customerContext=null as expected |
| Test 3 | D11.1 | No email = no auto-trigger | **PASS** | Empty email correctly prevents auto-trigger; all context fields null |
| Test 4 | D11.2 | Context projection 5 new fields in trace | **FAIL** | projectedContext is always null in trace -- ControlKernel.recordTurn() never populates it. Code review confirms 5 fields (task_summary, allowed_actions, risk_flags, budget_state, tool_schemas) exist in ContextProjectionBuilder but are not observable |
| Test 5 | D11.3 | Case creation for UC-H via Ad Support | **FAIL** | UC-H detected via DriftDetector HARD_SHIFT -> forceEscalate bypasses case creation. caseId=null. No CASE_CREATED event |
| Test 5-alt | D11.3 | Case creation for UC-J via strong prior | **PASS** | UC-J ("Report a Safety Issue" strong prior) -> normal intake flow -> case created (CASE-73576211). CASE_CREATED event emitted. Handover log contains case_id |
| Test 6 | D11.3 | No case creation for UC-I (Payments) | **PASS** | UC-I correctly has no case. However, this was via drift_hard_shift path, not the intentional UC-I exclusion logic in createCaseIfAllowed. The correct behavior happens for the wrong reason |
| Test 7 | D11.3 | Case ID in handover log | **PARTIAL PASS** | UC-J handover log contains case_id=CASE-73576211. UC-H handover logs have case_id=null (no case was created due to Issue 1+2) |

---

## 3. Test Coverage

### Tested modules (new tests written)

- `ContextProjectionBuilder` -- 14 unit tests covering all 5 new fields, PII redaction, knowledge instruction, customer/listing context inclusion
- `CreateCaseControlledTool` -- 26 unit tests covering UC access control, required field validation per UC, queue mapping, UC-I exclusion, blank field handling
- `GetCustomerContextTool` -- 10 unit tests covering email lookup, ad lookup, moderation review conditional fetch, PII sanitization, error cases

### Tested modules (existing tests)

- `DriftDetector`, `FormContextIngestionService`, `ActionParser`, `LlmInvocationService`, `OpenAiCompatibleLlmClient`, `PiiRedactionFilter`, `ForbiddenPhraseDetector`

### Untested modules (no unit tests)

- `ControlKernel` -- complex orchestration with many dependencies; needs integration testing
- `PhaseEvaluator` -- core phase logic including `createCaseIfAllowed()`; would require mocking LLM, knowledge search, script library
- `SessionManager` -- session lifecycle; would require Spring Boot Test context or extensive mocking
- `UseCaseRouter` -- routing logic; depends on LLM client
- `BudgetChecker` -- budget enforcement

### Priority modules to add tests

1. **PhaseEvaluator.createCaseIfAllowed()** -- CRITICAL: this is where case creation happens and should be tested to verify the UC-H/J/K vs UC-I/G split
2. **ControlKernel** -- HIGH: the forceEscalate path needs testing to verify it interacts correctly with case creation
3. **DriftDetector** -- MEDIUM: existing tests do not cover the null-activeUseCase scenario that causes Issue 1

---

## 4. Summary

### HIGH Priority (fix before next wave)

1. **DriftDetector fires HARD_SHIFT for null-activeUseCase sessions** (Issue 1). This causes intake UCs (especially UC-H via "Ad Support") to be escalated through the drift path instead of the normal intake flow. Root cause of case creation failure for UC-H.

2. **ControlKernel.forceEscalate() skips case creation** (Issue 2). When escalation happens via drift or budget exceeded, `createCaseIfAllowed()` is never called, leaving intake UCs without a Salesforce case. This breaks the D11.3 feature for all UCs that reach escalation through the drift path.

### MEDIUM Priority (fix soon)

3. **projectedContext never stored in trace** (Issue 3). The D11.2 context projection enhancement works correctly in code (all 5 fields are built), but is unverifiable through the trace endpoint because `ControlKernel.recordTurn()` does not populate the `projectedContext` field on `BotTurn`.

4. **API contract mismatch** (Issue 4). The task description uses `topicSubject` / `firstName` (camelCase) but the actual API expects `topic_subject` / `first_name` (snake_case). This should be clarified in documentation.

5. **No observability for routing failures** (Issue 5). Silent error handling in routing means failures are only visible in logs, not in the event stream.

6. **Silent case creation failures** (Issue 6). `createCaseIfAllowed()` catches all exceptions without emitting an event, making failures invisible to monitoring.

### LOW Priority (fix when convenient)

7. **Double sanitization of form fields** (Issue 7). `FormContextIngestionService.sanitize()` is called redundantly in both `SessionManager.createSession()` and `FormContextIngestionService.ingest()`.

8. **Null values in sanitized account data** (Issue 8). `GetCustomerContextTool.sanitizeAccount()` includes keys with null values in the output map.

---

## 5. Feature Verification Matrix

| Feature | Spec | Implementation | Unit Tests | Integration Tests | Verdict |
|---------|------|----------------|------------|-------------------|---------|
| D11.1: Auto-trigger get_customer_context | Email + allowed UC candidates -> auto-fetch | Correctly implemented in FormContextIngestionService | 10 tests (PASS) | 3 API tests (ALL PASS) | **PASS** |
| D11.2: Context Projection 5 new fields | task_summary, allowed_actions, risk_flags, budget_state, tool_schemas | Correctly implemented in ContextProjectionBuilder | 14 tests (PASS) | Trace endpoint cannot verify (projectedContext not persisted) | **PARTIAL PASS** (code correct, observability gap) |
| D11.3: create_case_controlled for UC-H/J/K | Case created before escalation for intake UCs (not UC-I) | Correctly implemented in CreateCaseControlledTool + PhaseEvaluator.createCaseIfAllowed() | 26 tests (PASS) | UC-J: PASS. UC-H: FAIL (drift path bypasses). UC-I: PASS (correct exclusion, wrong reason) | **PARTIAL PASS** (works for strong-prior UCs, fails for weak-prior UCs) |
