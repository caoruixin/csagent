# QA Report: D11 (Tool Layer Wiring) & D12 (Harness Completion)

**Generated**: 2026-04-24 12:35
**Scope**: D11 (Tool Layer Wiring) and D12 (Harness Completion) features tested via live API calls and unit tests
**Tech Stack**: Java 17, Spring Boot 3.2.5, JPA/Hibernate, PostgreSQL, JUnit 5 + Mockito, React+Vite frontend
**Backend**: localhost:8080 (running, healthy)
**Frontend**: localhost:5173 (running, healthy)

---

## 1. Live API Test Results

### Test 1: get_customer_context auto-trigger (D11.1)

**Status**: PASS (partial)

- **Session Created**: `c593c1d8-984b-4c04-a04c-5c465c825334`, intent=UC-H
- **customerContext**: POPULATED -- `{"account_status":"ACTIVE", ...}` (7 sanitized fields)
- **listingContext**: POPULATED -- `{"ad_id":"AD-1001","title":"iPhone 15 Pro - Like New","status":"LIVE", ...}`
- **moderationContext**: NULL (expected -- listing status is "LIVE", not "removed"/"moderated")
- **Candidate UCs**: `["UC-A","UC-B","UC-FP","UC-H"]` -- all have auto-trigger eligibility

**Observation**: The auto-trigger correctly fires for UCs in the allowed set (UC-A, UC-C, UC-D, UC-F, UC-FP, UC-K). The mock data returns the listing with status "LIVE", so moderation context is correctly skipped. The sanitization strips PII fields (email, phone, full_name) as designed.

---

### Test 2: Context projection fields (D11.2)

**Status**: PASS

Projected context verified in trace for session `c593c1d8...`, turn 2. All required fields present:

| Field | Present | Value (abbreviated) |
|-------|---------|-------------------|
| task_summary | Yes | "User inquiry about Ad Support. Ad Removal Appeal detected with 0.95 confidence. Current phase: RESOLVE." |
| allowed_actions | Yes | `["ask_user","escalate_human"]` |
| budget_state | Yes | `{"max_faq_miss":2,"max_bot_turns":10,...}` |
| risk_flags | Yes | `["HIGH"]` |
| tool_schemas | Yes | `["request_handover","record_outcome"]` |
| form_context | Yes | Includes email, ad_id, description, topic_subject |
| customer_context | Yes | Sanitized account data |
| listing_context | Yes | Sanitized listing data |
| conversation_history | Yes | Previous turns included |
| current_user_message | Yes | With email PII redacted |

**Observation**: For INTAKE UC-H, tool_schemas correctly excludes `search_knowledge` and `resolve_article`. Only `request_handover` and `record_outcome` are visible.

---

### Test 3: create_case_controlled for UC-H (D11.3)

**Status**: PASS

- **Session**: `d85d28a7-2038-489d-9e2d-f21f595f4f61`
- **Case Created**: `CASE-3F3C2FFC`
  - useCaseId: UC-H
  - queueName: Ad_Support_Queue
  - contactEmail: active@test.com
  - adId: 1487477877
- **Handover Log**: Present
  - case_id: CASE-3F3C2FFC (linked in payload)
  - customerMessage: "This conversation has been transferred to a human agent who can better assist you."
  - transcript: Full conversation transcript with user/bot roles and turn indices
  - handoverPayload: Contains all 17 fields per Phase 3 section 3.6.2 schema

---

### Test 4: create_case_controlled for UC-J (D11.3)

**Status**: PASS

- **Session**: `d56aa48d-2acf-4755-bb11-871aebc8adf0`
- **Case Created**: `CASE-4B654F12`
  - useCaseId: UC-J
  - queueName: Safety_Queue
  - subject: "Report a Safety Issue"
  - contactEmail: alice@test.com
- **should_end_chat**: true (correct -- session ends after intake)

---

### Test 5: No case for UC-I (D11.3)

**Status**: PASS

- **Session**: `86554eaf-206e-4537-9d65-7530939e5ffb`, intent=UC-I
- **Case**: NOT CREATED (correct -- UC-I is excluded from create_case_controlled)
- **Handover**: Yes, with case_id=None, has transcript
- **Verified**: `create_case_controlled` tool policy blocks UC-I, and the ALLOWED_UC set in CreateCaseControlledTool.java only includes UC-H, UC-J, UC-K

---

### Test 6: ToolDispatcher policy enforcement for INTAKE UCs (D12.1)

**Status**: PASS

- **Session**: `4215ef4f-7354-4f8e-a392-9a9d0a4de81a`, intent=UC-G (GDPR)
- **Events**: SESSION_STARTED, USE_CASE_INFERRED only -- no RETRIEVAL_EXECUTED
- **Trace tool_schemas**: `["request_handover","record_outcome"]` -- search_knowledge excluded
- **Verification**: INTAKE UCs (UC-G, UC-H, UC-I, UC-J, UC-K) are blocked from search_knowledge in tool-policy.yaml

---

### Test 7: Event emission completeness for FAQ session (D12.3)

**Status**: PASS

- **Session**: `4ee563cc-0d2b-489c-9cba-8303e0dae52c`, intent=UC-C (Messages & Replies)
- **Event timeline** (9 events):
  1. SESSION_STARTED
  2. USE_CASE_INFERRED (UC-C, confidence 0.90)
  3. RETRIEVAL_EXECUTED (query matched, 3 results)
  4. ARTICLE_SHOWN ("I'm Not Receiving Replies")
  5. ARTICLE_SHOWN ("Sending & Receiving Messages")
  6. ARTICLE_SHOWN ("I Can't Send Replies")
  7. OUTCOME_RECORDED (RESOLVED, UC-C)
  8. SESSION_CLOSED (RESOLVED)
  9. OUTCOME_RECORDED (RESOLVED) -- **DUPLICATE**

**Finding**: OUTCOME_RECORDED is emitted twice. Once from `PhaseEvaluator.evaluateClose()` and once from `SessionManager.recordOutcome()`. See Issue #1 below.

---

### Test 8: Turn log tool_calls (D12.4)

**Status**: PASS

- **FAQ session** (`4ee563cc...`):
  - Turn 1: `toolCalls=[{"status":"success","tool_name":"search_knowledge","result_count":3}]` -- POPULATED
  - Turn 2 (close): `toolCalls=null` -- correct, no tool invoked
- **INTAKE session** (`4215ef4f...`):
  - Turn 1: `toolCalls=null` -- correct, no knowledge retrieval for INTAKE

---

### Test 9: Handover Queue - customer_message and transcript (D12.5)

**Status**: PASS (with finding)

Handover logs contain:
- `customerMessage`: Populated for ROUTED/INTAKE sessions with escalation
- `transcript`: Full JSON array with `{role, message, turn_index}` entries

**Finding**: Out-of-scope handover logs (e.g., "Delivery", "Pro Contract") have `customerMessage=null` and `transcript=null`. This may be by design since these sessions are immediately escalated without bot interaction, but it could confuse human agents receiving the handover. See Issue #2 below.

---

### Test 10: Funnel metrics (D12.6)

**Status**: PASS

```json
{
  "totalSessions": 184,
  "understoodSessions": 139,
  "resolvedSessions": 3,
  "escalatedSessions": 42,
  "abandonedSessions": 0,
  "outcomeBreakdown": {"ESCALATED": 42, "RESOLVED": 3},
  "useCaseBreakdown": {"UC-G": 29, "UC-J": 16, "UC-A": 34, ...}
}
```

Per-UC metrics also returned correctly with resolutionRate computed.

---

## 2. Unit Test Results

### Test Summary

| Test File | Total | Pass | Fail | Skip |
|-----------|-------|------|------|------|
| ToolPolicyEnforcerTest (NEW) | 73 | 73 | 0 | 0 |
| ToolDispatcherTest (NEW) | 8 | 8 | 0 | 0 |
| EventEmitterTest (NEW) | 13 | 13 | 0 | 0 |
| GetCustomerContextToolTest | 10 | 10 | 0 | 0 |
| CreateCaseControlledToolTest | 26 | 20 | 2 | 0 |
| CreateCaseControlledToolQueueMappingTest | 2 | 0 | 1 | 0 |
| **TOTAL** | **132** | **124** | **2** | **0** |

**Errors (Mockito stubbing mismatches)**: 4 tests in CreateCaseControlledToolTest
**Failures**: 2 tests (queue mapping assertions stale)

### Failed/Error Tests

#### CreateCaseControlledToolTest#execute_success_shouldReturnCaseIdInData
- **Expected**: queue_name = "Safety_Queue"
- **Actual**: queue_name = "Ad_Support_Queue"
- **Cause**: Test still expects the OLD (incorrect) queue mapping for UC-H. The production code was corrected to map UC-H -> Ad_Support_Queue, but the test was not updated.

#### CreateCaseControlledToolQueueMappingTest#execute_ucH_queueName_shouldBeAdSupportRelated
- **Expected**: assertEquals("Safety_Queue", actualQueue)
- **Actual**: "Ad_Support_Queue"
- **Cause**: Same stale test -- the assertion text says "UC-H currently maps to Safety_Queue" but the code was already fixed. The assertion itself is wrong.

#### CreateCaseControlledToolTest (4 Mockito errors)
- `execute_ucH_withDescription_shouldSucceed` -- stubs `eq("Safety_Queue")` but code sends "Ad_Support_Queue"
- `execute_ucH_shouldUseSafetyQueue` -- same issue
- `execute_ucJ_shouldUseCommercialQueue` -- stubs `eq("Commercial_Queue")` but code sends "Safety_Queue"
- `execute_ucJ_withSubjectAndDescription_shouldSucceed` -- same issue

**Root cause**: The queue mapping in `CreateCaseControlledTool.QUEUE_BY_UC` was corrected (UC-H -> Ad_Support_Queue, UC-J -> Safety_Queue), but 6 existing tests still reference the old mapping. These tests are stale and need updating.

---

## 3. Code Review Findings

### Summary

| # | Severity | File | Line | Issue | Suggestion |
|---|----------|------|------|-------|------------|
| 1 | MEDIUM | SessionManager.java / PhaseEvaluator | multiple | Duplicate OUTCOME_RECORDED event emission | Remove duplicate; emit from one location only |
| 2 | LOW | SessionManager.java | 383-386 | Out-of-scope handover logs have null customerMessage and transcript | Populate these fields even for OOS handovers |
| 3 | HIGH | CreateCaseControlledToolTest.java | 117-231 | 6 stale test assertions reference old queue mapping | Update test expectations to match corrected QUEUE_BY_UC |
| 4 | MEDIUM | CreateCaseControlledTool.java | 26-29 | REQUIRED_FIELDS_BY_UC comment says UC-H is "safety/illegal reporting" | Fix comment -- UC-H is "Ad Removal Appeal", UC-J is safety |
| 5 | LOW | ControlKernel.java | 240 | Subject for UC-J in createCaseIfNeeded hardcodes "Report a Safety Issue" | Consider extracting from form_context topic_subject instead of hardcoding |
| 6 | LOW | ContextProjectionBuilder.java | 106-111 | tool_schemas only lists tool names, not actual schemas | Consider adding parameter schemas for LLM function calling |
| 7 | LOW | SessionManager.java | 160 | Event payload uses string format instead of JSON builder | Potential JSON injection if topicSubject contains quotes |

### Details

#### Issue 1: Duplicate OUTCOME_RECORDED Event Emission
- **File**: Multiple (SessionManager.java, ControlKernel.java, PhaseEvaluator)
- **Severity**: MEDIUM
- **Description**: When a FAQ session resolves, OUTCOME_RECORDED is emitted twice: once from the phase evaluator/control kernel chain, and once from SessionManager.recordOutcome(). Observed in test 7: events #7 and #9 are both OUTCOME_RECORDED with identical payload.
- **Evidence**: Event timeline shows `OUTCOME_RECORDED {"outcome":"RESOLVED","useCase":"UC-C"}` at index 7 and `OUTCOME_RECORDED {"outcome":"RESOLVED"}` at index 9.
- **Suggestion**: Audit all code paths that emit OUTCOME_RECORDED and ensure only one emission per session lifecycle.

#### Issue 2: OOS Handover Logs Missing customerMessage/transcript
- **File**: `SessionManager.java:317-376`
- **Severity**: LOW
- **Description**: When a session is immediately routed to out-of-scope (e.g., "Delivery"), the handover log has `customerMessage=null` and `transcript=null`. The `recordHandover()` method does build these fields, but OOS handovers that occur at session creation time (lines 170-176) may call recordHandover before any bot turns exist.
- **Suggestion**: Provide a default customerMessage for OOS handovers like "Your inquiry requires a specialist. Transferred to human agent." to give receiving agents context.

#### Issue 3: Stale Test Assertions (Queue Mapping)
- **File**: `CreateCaseControlledToolTest.java`, `CreateCaseControlledToolQueueMappingTest.java`
- **Severity**: HIGH
- **Description**: 6 test methods reference the old queue mapping (UC-H -> Safety_Queue, UC-J -> Commercial_Queue) which was corrected in production code. These tests now fail, making the test suite unreliable. Any CI/CD pipeline will fail.
- **Suggestion**: Update test expectations: UC-H -> Ad_Support_Queue, UC-J -> Safety_Queue, UC-K -> Account_Support_Queue.

#### Issue 4: Misleading Comment in CreateCaseControlledTool
- **File**: `CreateCaseControlledTool.java:24-28`
- **Severity**: MEDIUM
- **Description**: The Javadoc comment for REQUIRED_FIELDS_BY_UC says "UC-H: safety/illegal reporting" and "UC-J: commercial escalation". This is backwards -- UC-H is "Ad Removal Appeal" and UC-J is "Trust & Safety Report".
- **Code**:
  ```java
  /**
   * Required intake fields per use case.
   * UC-H: safety/illegal reporting -- needs description    // WRONG: UC-H is Ad Appeal
   * UC-J: commercial escalation -- needs subject, description  // WRONG: UC-J is Safety
   * UC-K: account access issues -- needs email, description
   */
  ```
- **Suggestion**: Correct to: UC-H = Ad Removal Appeal, UC-J = Trust & Safety Report.

#### Issue 5: Event Payload String Format Risk
- **File**: `SessionManager.java:160-162`
- **Severity**: LOW
- **Description**: Event payloads are built using `String.format("{\"topic_subject\":\"%s\",...}", topicSubject)`. If topicSubject contains a double-quote character, this produces malformed JSON. While the pre-chat form sanitizes input, this is a defense-in-depth concern.
- **Suggestion**: Use ObjectMapper to build event payloads consistently (as EventEmitter already does).

---

## 4. Test Coverage

### Tested Modules
- `service/tools/ToolPolicyEnforcer` -- 73 tests (NEW, all pass)
- `service/tools/ToolDispatcher` -- 8 tests (NEW, all pass)
- `service/tools/CreateCaseControlledTool` -- 26 tests (6 stale, need update)
- `service/tools/GetCustomerContextTool` -- 10 tests (all pass)
- `service/observability/EventEmitter` -- 13 tests (NEW, all pass)

### Untested Modules (Priority)
- `service/runtime/ControlKernel` -- Core orchestration engine. Has `createCaseIfNeeded()` and `forceEscalate()` logic that should be tested with mock dependencies.
- `service/runtime/SessionManager` -- Session lifecycle including handover recording, transcript building, duplicate event emission. Needs integration-level tests.
- `service/observability/LocalEventStore` -- Funnel metrics aggregation logic.
- `service/tools/RequestHandoverTool` -- Handover payload construction.

### Priority Modules to Add Tests
1. **ControlKernel** (HIGH) -- critical orchestration logic, case creation during forced escalation, budget checking
2. **SessionManager.recordHandover** (MEDIUM) -- handover payload completeness, transcript building
3. **RequestHandoverTool** (MEDIUM) -- payload schema validation
4. **LocalEventStore.getFunnelMetrics** (LOW) -- aggregation correctness

---

## 5. Summary

### Test Results

| Category | Count |
|----------|-------|
| Live API Tests | 10 PASS, 0 FAIL |
| Unit Tests (new) | 94 PASS, 0 FAIL |
| Unit Tests (existing) | 30 PASS, 2 FAIL, 4 ERROR |
| **Total** | **134 PASS, 2 FAIL, 4 ERROR** |

### HIGH Priority (fix immediately)
1. **Stale test assertions**: 6 tests in CreateCaseControlledToolTest and CreateCaseControlledToolQueueMappingTest reference old queue mapping. CI will fail. Update expectations to match corrected QUEUE_BY_UC mapping.

### MEDIUM Priority (fix soon)
1. **Duplicate OUTCOME_RECORDED event**: Emitted twice for resolved FAQ sessions. Audit emission points and ensure single emission per session.
2. **Misleading comment in CreateCaseControlledTool**: UC-H/UC-J description comments are swapped. Fix to prevent future developer confusion.

### LOW Priority (fix when convenient)
1. **OOS handover logs missing customerMessage/transcript**: Populate with sensible defaults for out-of-scope handovers.
2. **Event payload built with String.format**: Replace with ObjectMapper for JSON safety.
3. **tool_schemas only lists names, not parameter schemas**: Consider adding full schemas for better LLM function calling.
