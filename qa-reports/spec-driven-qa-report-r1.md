# Spec-Driven QA Report - Round 1

**Generated**: 2026-04-22 10:30
**Scope**: Full spec-driven test plan covering Session Creation, UC Routing, Multi-Turn Conversation, Escalation Paths, Demo Inspection, Knowledge Search, Mock Integration, Error Handling, Event Tracking, and Health/Actuator endpoints.
**Tech Stack**: Java 17, Spring Boot 3.2.5, PostgreSQL + pgvector, Redis, Flyway, Lombok, JPA/Hibernate. LLM via DashScope (Qwen) / Kimi fallback. Frontend at :5173 (Vite).
**Backend URL**: http://localhost:8080
**Frontend URL**: http://localhost:5173

---

## 1. Test Results Table

### A. Session Creation & UC Routing

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| A1a | Strong Prior: "Delete My Account or Data" -> UC-G | PASS | intent=UC-G, should_end_chat=false | Correctly routed with confidence 0.90 |
| A1b | Strong Prior: "Report a Safety Issue" -> UC-J | PASS | intent=UC-J, should_end_chat=false | Correctly routed |
| A1c | Strong Prior: "Replies or Messaging" -> UC-C | PASS | intent=UC-C, should_end_chat=false | Correctly routed |
| A2a | Weak Prior: "Ad Support" + ad visibility description -> UC-A | PASS | intent=UC-A, confidence=0.95 | LLM correctly classified |
| A2b | Weak Prior: "Payments" + inquiry description -> UC-F | PASS | intent=UC-F | LLM correctly classified |
| A2c | Weak Prior: "Technical Support" + crash description -> UC-K | PASS | intent=UC-K | LLM correctly classified as technical intake |
| A3a | OOS: "Delivery" -> immediate handover | PASS | should_end_chat=true, no intent | Correct OOS handling |
| A3b | OOS: "Pro Contract" -> immediate handover | PASS | should_end_chat=true, no intent | Correct OOS handling |
| A3c | OOS: "Ratings Reviews" -> immediate handover | PASS | should_end_chat=true, no intent | Correct OOS handling |
| A3d | OOS: "Account Manager Support" -> immediate handover | PASS | should_end_chat=true, no intent | Correct OOS handling |
| A4a | Validation: Missing topic_subject -> 400 | PASS | HTTP 400, reply_text="topic_subject is required" | Correct validation |
| A4b | Validation: Missing email -> should still work | PASS | HTTP 200, intent=UC-H | Works but routing to UC-H for "help" description is questionable |

### B. Multi-Turn Conversation Flow

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| B1-T1 | UC-A turn 1: ad not showing | PASS | "I couldn't find a specific answer..." faqMissCount=1 | FAQ miss due to empty KB |
| B1-T2 | UC-A turn 2: detailed description | FAIL | Escalated after 2nd FAQ miss | Escalated too quickly; max-faq-miss=2 reached in only 2 turns |
| B2-T1 | UC-C turn 1: messages blocked | PASS | "I couldn't find a specific answer..." | FAQ miss #1 |
| B2-T2 | UC-C turn 2: different wording | FAIL | Escalated after 2nd FAQ miss | Same pattern - always escalates at turn 2 due to empty KB |
| B-State | Session state after multi-turn | PASS | Phase transitions RESOLVE->ESCALATE are valid | State machine transitions comply with control-policy.yaml |

### C. Escalation Paths

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| C1 | User-requested escalation ("talk to a real person") | PASS | should_end_chat=true, escalationReason=drift_hard_shift | Works but reason is "drift_hard_shift" not "user_requested" |
| C2a | UC-G GDPR session turn 1 | PASS | Bot collecting intake info, should_end_chat=false | Intake flow working |
| C2b | UC-G GDPR session turn 2 | FAIL | Bot still collecting, lastAction="retrieve_knowledge" | INTAKE path should not search knowledge base |
| C3 | Handover logs verification | FAIL | Missing 15+ required fields per spec section 3.6.2 | See Issue #4 below |

### D. Demo Inspection Endpoints

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| D1 | GET /v1/demo/sessions | PASS | HTTP 200, returns session list | Working |
| D2 | GET /v1/demo/cases | PASS | HTTP 200, returns case list | Working |
| D3 | GET /v1/demo/handover-logs | PASS | HTTP 200, returns handover log list | Working |
| D4 | GET /v1/demo/metrics/funnel | PASS | HTTP 200, returns funnel metrics | Data consistency issue (see #5) |
| D5 | GET /v1/demo/metrics/per-uc | PASS | HTTP 200, returns per-UC metrics | useCaseName uses UC ID not human name |
| D6 | GET /v1/demo/mock-data/accounts | PASS | HTTP 200, returns account fixtures | Working |
| D7 | GET /v1/demo/mock-data/listings | PASS | HTTP 200, returns listing fixtures | Working |
| D8 | GET /v1/demo/sessions/{id}/trace | PASS | HTTP 200, returns turn trace | Working |
| D9 | GET /v1/demo/sessions/{id}/events | PASS | HTTP 200, returns event list | Working |

### E. Knowledge Search

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| E1 | Basic search: "how to post an ad on gumtree" | FAIL | faq_miss=true, retrieval_miss=true, hits=[] | No results for a legitimate query |
| E2 | Search with UC filter: "how to post an ad" + UC-B | FAIL | faq_miss=true, retrieval_miss=true, hits=[] | Same - no results |
| E3 | Gibberish search: "asdfghjkl" | PASS | faq_miss=true, retrieval_miss=true, hits=[] | Expected empty results |

### F. Mock Integration

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| F1 | Toggle business_hours to false | PASS | HTTP 200, business_hours=false confirmed | Working |
| F2 | Toggle business_hours back to true | PASS | HTTP 200, business_hours=true confirmed | Working |

### G. Error Handling

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| G1 | GET invalid session ID -> 404 | PASS | HTTP 404, empty body | Correct |
| G2 | POST message to invalid session -> 404 | PASS | HTTP 404, empty body | Correct |
| G3 | POST empty message -> 400 | PASS | HTTP 400, "message is required" | Correct |
| G4 | POST message to ESCALATED session | FAIL | HTTP 200, still accepts messages | Should reject; only CLOSED/CLOSE are blocked |

### H. Event Tracking

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| H1 | OOS (Delivery) session events | PASS | SESSION_STARTED + OUT_OF_SCOPE_HANDOVER + OUTCOME_RECORDED | All expected events present |
| H2 | User-escalated session events | PASS | SESSION_STARTED + USE_CASE_INFERRED + ESCALATION_REQUESTED + OUTCOME_RECORDED | All expected events present |
| H3 | UC-G strong prior session events | PASS | SESSION_STARTED + USE_CASE_INFERRED | Correct for non-escalated session |

### I. Health & Actuator

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| I1 | /internal/health | PASS | HTTP 200, status=UP, db=UP, redis=UP | Working |
| I2 | /internal/health/liveness | PASS | HTTP 200, status=UP | Working |
| I3 | /internal/health/readiness | PASS | HTTP 200, status=UP | Working |
| I4 | /actuator/metrics | PASS | HTTP 200 | Metrics endpoint exposed |

### Security Tests

| ID | Test Name | Status | Actual Response Summary | Notes |
|----|-----------|--------|------------------------|-------|
| S1 | XSS payload in first_name | FAIL | Script tag reflected in reply_text | Reflected XSS vulnerability |
| S2 | SQL injection in description | PASS | HTTP 200, no crash | JPA parameterized queries protect |
| S3 | 100KB payload in description | FAIL | HTTP 200, accepted | No input size validation |

---

## 2. Issues Found

### Issue #1: Reflected XSS in Bot Response (CRITICAL)

- **File**: `SessionManager.java:237-240` (buildGreeting method)
- **Severity**: CRITICAL
- **Description**: The `first_name` field from user input is directly interpolated into the `reply_text` response without any HTML sanitization. When the response is rendered in a browser (frontend at :5173), malicious scripts injected via `first_name` will execute.
- **Reproduction**:
  ```bash
  curl -X POST http://localhost:8080/v1/chat/sessions \
    -H "Content-Type: application/json" \
    -d '{"first_name":"<script>alert(1)</script>","email":"t@t.com","topic_subject":"Ad Support","description":"test"}'
  ```
- **Actual**: `reply_text` contains `Hi <script>alert(1)</script>! I'm here to help...`
- **Expected**: Input should be sanitized or escaped before inclusion in response text
- **Suggestion**: Sanitize `firstName` in the greeting builders using HTML entity encoding or a library like OWASP Java Encoder. The `Jsoup` dependency already exists in pom.xml and can be used for this purpose.

### Issue #2: Knowledge Base Returns Empty Results for All Queries (HIGH)

- **File**: `KnowledgeSearchService.java`, `KnowledgeIngestionRunner.java`
- **Severity**: HIGH
- **Description**: All FAQ search queries return `retrieval_miss=true` with empty hits, including legitimate queries like "how to post an ad on gumtree". This means the FAQ-path UCs (UC-A through UC-FP) can never resolve -- every conversation will escalate after 2 turns due to `max-faq-miss: 2`.
- **Reproduction**:
  ```bash
  curl -X POST http://localhost:8080/v1/faq/search \
    -H "Content-Type: application/json" \
    -d '{"query":"how to post an ad on gumtree"}'
  ```
- **Actual**: `{"hits":[],"faq_miss":true,"retrieval_miss":true,"answer_miss":false}`
- **Expected**: Should return relevant FAQ articles with snippets
- **Likely Cause**: The knowledge base (kb_articles / kb_chunks tables) may be empty because the ingestion runner has not been executed, or the embedding service is returning null/empty embeddings so the pgvector ANN search returns no results.
- **Suggestion**: Verify `make ingest` was run to populate the KB. Check logs for embedding failures. Consider adding a `/v1/demo/kb-stats` endpoint to surface KB health.

### Issue #3: Escalated Sessions Still Accept Messages (HIGH)

- **File**: `SessionManager.java:189`
- **Severity**: HIGH
- **Description**: The `processMessage` method only blocks messages to sessions with `handlingState="CLOSED"` or `currentPhase="CLOSE"`. Sessions in `ESCALATE` phase or `QUEUE_TO_HUMAN` state still accept new messages and process them through the control kernel.
- **Reproduction**:
  ```bash
  # After a session escalates:
  curl -X POST http://localhost:8080/v1/chat/sessions/{escalated_session_id}/messages \
    -H "Content-Type: application/json" \
    -d '{"message":"hello again"}'
  ```
- **Actual**: HTTP 200, processes the message and returns a response
- **Expected**: Should return an appropriate error or "session has ended" message without processing through the control kernel
- **Suggestion**: Add `"ESCALATE".equals(session.getCurrentPhase())` or `"QUEUE_TO_HUMAN".equals(session.getHandlingState())` to the closed-session check at line 189.

### Issue #4: Handover Payload Missing Required Fields per Spec Section 3.6.2 (HIGH)

- **File**: `SessionManager.java:296-318` (recordHandover method)
- **Severity**: HIGH
- **Description**: The handover payload written to `mock_handover_log` only contains 5 fields: `session_id`, `use_case`, `escalation_reason`, `total_turns`, `form_topic_subject`. The spec (Phase 3, Section 3.6.2) requires 20+ fields including: `version`, `bot_session_id`, `primary_use_case`, `candidate_use_cases`, `current_status`, `summary`, `intent_confidence`, `clarification_count`, `faq_miss_count`, `articles_shown`, `transcript_ref`, `case_id`, `identifiers_collected`, `intake_fields`, `handling_duration_seconds`, `topic_uc_mismatch`, `prompt_version`, `model_version`.
- **Actual payload**:
  ```json
  {"use_case":"UC-A","session_id":"...","total_turns":2,"escalation_reason":"faq_miss_exceeded","form_topic_subject":"Ad Support"}
  ```
- **Expected payload**: Should include all fields from spec section 3.6.2
- **Suggestion**: Expand `recordHandover` to include all session fields. Add `version: "1.0"`, `primary_use_case` (aliased from `use_case`), pull `candidate_use_cases`, `intent_confidence`, `clarification_count`, `faq_miss_count`, `articles_shown`, `prompt_version`, `model_version` from the BotSession object.

### Issue #5: Metrics Discrepancy Between Funnel and Per-UC (MEDIUM)

- **File**: `LocalEventStore.java:53-138`
- **Severity**: MEDIUM
- **Description**: Funnel metrics count all sessions with `containmentOutcome=ESCALATED` including OOS sessions (which have null `activeUseCase`). Per-UC metrics only count sessions that have a non-null `activeUseCase`. This creates a discrepancy: funnel shows 7 escalated but per-UC sums to only 2 escalated.
- **Actual**: Funnel: escalatedSessions=7. Per-UC sum: escalated=2 (UC-A:1 + UC-D:1). Missing 5 are OOS sessions + sessions where containmentOutcome was not set.
- **Expected**: Both metrics should be consistent. OOS sessions should appear in a separate "OOS" bucket in per-UC, or funnel should clearly distinguish OOS from UC-routed escalations.
- **Suggestion**: Add an "OUT_OF_SCOPE" category to per-UC metrics, or add `oosEscalatedSessions` to funnel metrics. Also, sessions that escalate via faq_miss should have their `containmentOutcome` consistently set.

### Issue #6: Per-UC Metrics Use UC IDs Instead of Human-Readable Names (MEDIUM)

- **File**: `LocalEventStore.java:107-115`
- **Severity**: MEDIUM
- **Description**: The `useCaseName` field in per-UC metrics is set to the UC ID (e.g., "UC-A") rather than the human-readable name from the registry (e.g., "Ad Status & Visibility"). This makes the metrics less useful for demo stakeholders.
- **Actual**: `{"useCaseId":"UC-A","useCaseName":"UC-A",...}`
- **Expected**: `{"useCaseId":"UC-A","useCaseName":"Ad Status & Visibility",...}`
- **Suggestion**: Inject `UseCaseRegistryService` into `LocalEventStore` and look up `ucDef.name()` for each UC ID.

### Issue #7: User Escalation Request Classified as "drift_hard_shift" Not "user_requested" (MEDIUM)

- **File**: `DriftDetector.java:46-52`, `ControlKernel.java:82-89`
- **Severity**: MEDIUM
- **Description**: When a user says "I want to talk to a real person", the DriftDetector correctly detects the escalation pattern, but the ControlKernel records `escalationReason="drift_hard_shift"` regardless of whether it was a topic drift or an explicit user request. This makes it impossible to distinguish user-requested escalations from actual topic drift in analytics.
- **Actual**: `escalationReason: "drift_hard_shift"` for both actual drift and user escalation requests
- **Expected**: `escalationReason: "user_requested_escalation"` when the ESCALATION_PATTERN matches; `"drift_hard_shift"` only for keyword-based UC shifts
- **Suggestion**: Have `DriftResult` include a sub-type or reason field that distinguishes `USER_ESCALATION_REQUEST` from `KEYWORD_HARD_SHIFT`. Map to distinct escalation reasons in ControlKernel.

### Issue #8: Inconsistent containmentOutcome for FAQ-Miss Escalations (MEDIUM)

- **File**: `SessionManager.java:200-210`, `PhaseEvaluator.java:124-128`
- **Severity**: MEDIUM
- **Description**: When a session escalates via `faq_miss_exceeded` in `PhaseEvaluator.resolveFaq()`, the escalation is done via `PhaseResult.escalate()` which sets `session.setEscalationReason()`. However, examining session state after such escalation shows `containmentOutcome=null` and `handlingState=BOT_HANDLING` rather than the expected `ESCALATED` and `QUEUE_TO_HUMAN`.
- **Actual**: After FAQ-miss escalation: `handlingState: "BOT_HANDLING"`, `containmentOutcome: null`
- **Expected**: `handlingState: "QUEUE_TO_HUMAN"`, `containmentOutcome: "ESCALATED"`
- **Suggestion**: The `ControlKernel` should check `phaseResult.shouldEscalate()` and if true, update `handlingState` and `containmentOutcome` before saving, similar to `forceEscalate()`. Or the `SessionManager.processMessage()` should detect `ESCALATE` phase and update these fields.

### Issue #9: INTAKE Path Sessions Use Knowledge Search Instead of Pure Intake Flow (MEDIUM)

- **File**: `PhaseEvaluator.java:172-185`, UC-G session trace
- **Severity**: MEDIUM
- **Description**: For INTAKE path UCs (UC-G, UC-H, UC-I, UC-J, UC-K), the LLM sometimes selects `retrieve_knowledge` as its action. The trace for UC-G session shows `lastAction="retrieve_knowledge"` after 2 turns. INTAKE sessions should focus on collecting information for handover, not searching the FAQ knowledge base.
- **Actual**: UC-G session at turn 2: `lastAction: "retrieve_knowledge"`, `faqMissCount: 0`
- **Expected**: INTAKE sessions should only use ask_user, create_case, and escalate_human actions
- **Suggestion**: The system prompt or ControlKernel should restrict the action set for INTAKE path UCs. The ToolPolicyEnforcer correctly limits tool access per UC, but the LLM action selection in the prompt is not similarly constrained.

### Issue #10: No Input Size/Length Validation (MEDIUM)

- **File**: `ChatController.java:32-55`
- **Severity**: MEDIUM
- **Description**: The API accepts arbitrarily large payloads. A 100KB description was accepted without error. This could lead to resource exhaustion, excessive LLM token consumption, and potential DoS.
- **Reproduction**: Send a 100,000-character description in the POST body
- **Actual**: HTTP 200, accepted and processed
- **Expected**: Should enforce reasonable limits (e.g., first_name: 100 chars, email: 255 chars, description: 5000 chars)
- **Suggestion**: Add `@Size` annotations or manual length checks in the controller. Consider using a proper request DTO with `@Valid` instead of `Map<String, String>`.

### Issue #11: GlobalExceptionHandler Maps All IllegalArgumentException to 400 (LOW)

- **File**: `GlobalExceptionHandler.java:16`
- **Severity**: LOW
- **Description**: The global exception handler catches all `IllegalArgumentException` and returns 400 Bad Request. However, some uses of this exception (e.g., "Session not found") should return 404 Not Found. The ChatController manually catches `IllegalArgumentException` for session-not-found and returns 404, but if any other code path throws `IllegalArgumentException` for a not-found scenario, it would incorrectly return 400.
- **Suggestion**: Use a custom `SessionNotFoundException` (extending `RuntimeException`) for session lookups, with a dedicated handler returning 404.

### Issue #12: No Request Body Validation Using Spring Validation (LOW)

- **File**: `ChatController.java`
- **Severity**: LOW
- **Description**: The controller accepts `Map<String, String>` instead of a typed DTO with Jakarta Validation annotations. The `spring-boot-starter-validation` dependency is included in pom.xml but unused. This means all validation is ad-hoc string null checks.
- **Suggestion**: Create `CreateSessionRequest` and `SendMessageRequest` DTOs with `@NotBlank`, `@Email`, `@Size` annotations. Use `@Valid @RequestBody` in the controller.

### Issue #13: Static SAFE_ESCALATION_RESPONSE Object is Shared (LOW)

- **File**: `LlmInvocationService.java:27-31`
- **Severity**: LOW
- **Description**: The `SAFE_ESCALATION_RESPONSE` is a static final `LlmResponse` object. Since `LlmResponse` uses `@Data` (Lombok), it has setters and is mutable. If any downstream code accidentally modifies this object, it would corrupt the shared fallback for all subsequent failures.
- **Suggestion**: Make `LlmResponse` immutable (use `@Value` instead of `@Data`, or use a record), or create a new instance each time in the catch block.

### Issue #14: formContext Stores Email in Plaintext JSON (LOW)

- **File**: `FormContextIngestionService.java:44`
- **Severity**: LOW
- **Description**: The customer's email address is stored in plaintext in the `formContext` JSON column. While the PII redaction filter exists, it is not applied to the formContext before persistence. The email is visible in the session GET endpoint and demo session list.
- **Actual**: `formContext: "{\"email\": \"jane@example.com\", ...}"`
- **Suggestion**: Apply `PiiRedactionFilter.redactForLogs()` to the email before storing in formContext, or encrypt the field. Keep the raw email only for internal use (not exposed via API).

### Issue #15: CORS Allows Only localhost:5173 (LOW)

- **File**: `WebConfig.java:12`
- **Severity**: LOW
- **Description**: CORS is hardcoded to `http://localhost:5173`. This is appropriate for local development but would need updating for any deployment.
- **Suggestion**: Make the CORS origin configurable via application properties.

---

## 3. Test Execution Results

### Test Summary

| Category | Total | Pass | Fail | Skip |
|----------|-------|------|------|------|
| A. Session Creation & Routing | 12 | 12 | 0 | 0 |
| B. Multi-Turn Conversation | 5 | 3 | 2 | 0 |
| C. Escalation Paths | 4 | 2 | 2 | 0 |
| D. Demo Inspection | 9 | 9 | 0 | 0 |
| E. Knowledge Search | 3 | 1 | 2 | 0 |
| F. Mock Integration | 2 | 2 | 0 | 0 |
| G. Error Handling | 4 | 3 | 1 | 0 |
| H. Event Tracking | 3 | 3 | 0 | 0 |
| I. Health & Actuator | 4 | 4 | 0 | 0 |
| Security | 3 | 1 | 2 | 0 |
| **TOTAL** | **49** | **40** | **9** | **0** |

**Pass Rate: 81.6% (40/49)**

### Existing Unit Tests

No unit tests exist in `server/src/test/`. The test directory `src/test/java/com/gumtree/csagent/` is empty.

---

## 4. Test Coverage Assessment

### Tested Modules
- ChatController (session creation, message processing, session retrieval)
- DemoInspectionController (all endpoints)
- HealthController (all health endpoints)
- KnowledgeSearchController (FAQ search)
- SessionManager (session lifecycle)
- UseCaseRouter (strong priors, weak priors, OOS routing)
- ControlKernel (budget checking, drift detection, phase evaluation)
- PhaseEvaluator (RESOLVE for FAQ and INTAKE paths)
- DriftDetector (escalation pattern, hard shift keywords)
- BudgetChecker (FAQ miss budget)
- FormContextIngestionService (form context ingestion)
- LocalEventStore (funnel and per-UC metrics)

### Untested Modules (no unit tests, limited E2E coverage)
- ForbiddenPhraseDetector (guardrails)
- PiiRedactionFilter (PII redaction)
- ToolPolicyEnforcer (tool access control)
- ToolDispatcher (tool execution)
- All individual tools (GetCustomerContextTool, LookupListingTool, etc.)
- ScriptLibraryService (fixed scripts)
- DashScopeEmbeddingClient (embedding)
- RerankService (reranking)
- ChunkingService (knowledge chunking)
- KnowledgeIngestionRunner (KB ingestion)
- MockDataInitializer (mock data setup)
- ActionParser (LLM response parsing)
- ContextProjectionBuilder (context projection)

### Priority Modules to Add Tests
1. **UseCaseRouter** - Core routing logic, handles strong/weak/OOS classification
2. **ControlKernel** - Main orchestration engine, multiple code paths
3. **PhaseEvaluator** - Per-phase logic with complex branching
4. **BudgetChecker** - Budget enforcement, multiple budget types
5. **DriftDetector** - Pattern matching, keyword detection
6. **ForbiddenPhraseDetector** - Guardrail enforcement (safety-critical)
7. **PiiRedactionFilter** - PII protection (security-critical)
8. **ActionParser** - LLM response parsing, error handling
9. **ToolPolicyEnforcer** - Tool access control (security-critical)

---

## 5. Summary

### CRITICAL Priority (fix immediately)
1. **[Issue #1]** Reflected XSS: User input (`first_name`) is echoed in bot responses without sanitization. The `<script>` tags pass through to the frontend.

### HIGH Priority (fix soon)
2. **[Issue #2]** Knowledge Base empty: All FAQ searches return retrieval_miss=true. No FAQ-path UC can ever self-resolve, forcing 100% escalation for FAQ cases.
3. **[Issue #3]** Escalated sessions accept new messages: Sessions in ESCALATE phase still process messages through the control kernel.
4. **[Issue #4]** Handover payload missing 15+ required fields per spec section 3.6.2. Only 5 of 20+ specified fields are present.

### MEDIUM Priority (fix when possible)
5. **[Issue #5]** Funnel vs per-UC metrics discrepancy (OOS sessions counted in funnel but not per-UC).
6. **[Issue #6]** Per-UC metrics show UC IDs as names instead of human-readable names.
7. **[Issue #7]** User escalation requests logged as "drift_hard_shift" rather than "user_requested_escalation".
8. **[Issue #8]** FAQ-miss escalations don't set containmentOutcome or handlingState correctly.
9. **[Issue #9]** INTAKE path sessions incorrectly use knowledge search (retrieve_knowledge action).
10. **[Issue #10]** No input size validation; 100KB payloads accepted.

### LOW Priority (fix when convenient)
11. **[Issue #11]** All IllegalArgumentException mapped to 400 (some should be 404).
12. **[Issue #12]** No typed request DTOs with Spring Validation annotations.
13. **[Issue #13]** Mutable static SAFE_ESCALATION_RESPONSE could be corrupted.
14. **[Issue #14]** Email stored in plaintext in formContext JSON column.
15. **[Issue #15]** CORS origin hardcoded to localhost:5173.
