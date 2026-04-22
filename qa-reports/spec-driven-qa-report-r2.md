# Spec-Driven QA Report - Round 2

**Generated**: 2026-04-22 15:30
**Scope**: Fix verification for 5 issues from Round 1 + regression testing + new issue discovery
**Tech Stack**: Java 17, Spring Boot 3.2.5, PostgreSQL + pgvector, Redis, Flyway, Lombok, JPA/Hibernate. LLM via DashScope (Qwen) / Kimi fallback.
**Backend URL**: http://localhost:8080
**Round 1 Report**: `qa-reports/spec-driven-qa-report-r1.md`

---

## 1. Fix Verification Results

### Fix 1: XSS in first_name and description (was R1 CRITICAL, Issue #1)

**Status: PASS**

| Test | Input | Expected | Actual | Result |
|------|-------|----------|--------|--------|
| 1a | `first_name=<script>alert(1)</script>` | No `<script>` in reply | Script tag stripped, reply uses "there" fallback | PASS |
| 1b | `description=<img src=x onerror=alert(1)>` | No `<img>` in reply | Image tag stripped from description | PASS |
| 1c | `first_name=<b>Bold</b>Name` | Name preserved without HTML | "BoldName" (tags stripped, text preserved) | PASS |
| 1d | `first_name=Bob<img/src=x onerror=alert(1)>` | No `<img>` in reply | "Bob" (SVG/img stripped) | PASS |
| 1e | `first_name=<svg onload=alert(1)>test` | No `<svg>` in reply | "test" (SVG stripped) | PASS |
| 1f | `first_name=&lt;script&gt;alert(1)&lt;/script&gt;` | Entities preserved as text | Entities pass through as harmless text | PASS |
| 1g | `first_name=O'Brien` (normal name with apostrophe) | Name preserved | "O'Brien" preserved correctly | PASS |

**Implementation**: `FormContextIngestionService.sanitize()` uses `Jsoup.clean(input, Safelist.none())` to strip all HTML tags. Called in both `FormContextIngestionService.ingest()` and `SessionManager.createSession()` (double sanitization for defense-in-depth).

**Evidence**: All HTML tags (`<script>`, `<img>`, `<svg>`, `<b>`) are stripped. Text content is preserved. Normal names with special characters (apostrophes) are unaffected.

---

### Fix 2: Escalated sessions reject messages (was R1 HIGH, Issue #3)

**Status: PASS**

| Test | Scenario | Expected | Actual | Result |
|------|----------|----------|--------|--------|
| 2a | Message to OOS-escalated session | should_end_chat=true, transfer message | "transferred to a human agent", should_end_chat=true | PASS |
| 2b | Message to user-escalated session | should_end_chat=true, transfer message | "transferred to a human agent", should_end_chat=true | PASS |
| 2c | Message to FAQ-miss-escalated session | should_end_chat=true, transfer message | "transferred to a human agent", should_end_chat=true | PASS |

**Implementation**: `SessionManager.processMessage()` (lines 204-214) now checks `"ESCALATE".equals(session.getCurrentPhase())`, `"QUEUE_TO_HUMAN".equals(session.getHandlingState())`, and `"HUMAN_HANDLING".equals(session.getHandlingState())` before processing. Returns a clear "transferred to human agent" message with `should_end_chat=true`.

**Evidence**: All three escalation paths (OOS, user-requested, FAQ-miss) correctly block follow-up messages.

---

### Fix 3: Handover payload complete (was R1 HIGH, Issue #4)

**Status: PASS**

| Test | Expected | Actual | Result |
|------|----------|--------|--------|
| Required fields present | 22 required fields | 22/22 present | PASS |
| version field | "1.0" | "1.0" | PASS |
| summary field | Non-empty string | "Customer issue: ... Escalation reason: ..." | PASS |
| identifiers_collected | Contains email | `{"email": "esc@test.com"}` | PASS |
| candidate_use_cases | Array | `[]` (correct for OOS) | PASS |
| handling_duration_seconds | >= 0 | 0 (immediate OOS) | PASS |
| prompt_version | Non-null | "v1.0.0" | PASS |
| model_version | Non-null | "local" | PASS |

**Implementation**: `SessionManager.recordHandover()` (lines 313-363) now builds a complete `LinkedHashMap` with all 22 fields. Helper methods `buildSummary()`, `buildIdentifiers()`, `calculateDuration()`, and `checkMismatch()` populate derived fields.

**Evidence**: Handover payload JSON contains all 22 required fields per spec section 3.6.2. Verified on OOS, user-escalated, and FAQ-miss-escalated sessions.

---

### Fix 4: INTAKE UCs skip knowledge search (was R1 MEDIUM, Issue #9)

**Status: PASS**

| Test | UC | Expected | Actual | Result |
|------|----|---------|----|--------|
| 4a | UC-G (GDPR) | lastAction != retrieve_knowledge, faqMissCount=0 | lastAction=ask_user, faqMissCount=0 | PASS |
| 4b | UC-J (Safety) | lastAction != retrieve_knowledge, faqMissCount=0 | lastAction=ask_user, faqMissCount=0 | PASS |
| 4c | UC-I (Payments) | lastAction != retrieve_knowledge, faqMissCount=0 | lastAction=ask_user, faqMissCount=0 | PASS |

**Implementation**: `PhaseEvaluator.evaluateResolve()` (line 131) checks `"INTAKE".equals(ucDef.path()) || INTAKE_UCS.contains(activeUc)` and routes to `resolveIntake()` instead of `resolveFaq()`. The `INTAKE_UCS` set contains UC-G/H/I/J/K. `resolveIntake()` uses fixed script templates and LLM for intake field collection, never calling `knowledgeSearchService.search()`.

**Evidence**: All three tested INTAKE UCs show `lastAction=ask_user` and `faqMissCount=0` after message processing.

---

### Fix 5: User escalation distinct from drift (was R1 MEDIUM, Issue #7)

**Status: PASS**

| Test | Input | Expected | Actual | Result |
|------|-------|----------|--------|--------|
| 5a | "I want to talk to a real person" | escalationReason=user_requested_escalation | escalationReason=user_requested_escalation | PASS |
| 5b | Handover log for user-escalated session | escalation_reason=user_requested_escalation | escalation_reason=user_requested_escalation | PASS |

**Implementation**: `DriftResult.DriftType` enum now includes `USER_ESCALATION_REQUEST` (distinct from `HARD_SHIFT`). `DriftDetector.detect()` checks `ESCALATION_PATTERN` first and returns `DriftType.USER_ESCALATION_REQUEST` with `escalationRequested=true`. `ControlKernel.processMessage()` (lines 82-88) checks `drift.getType() == DriftType.USER_ESCALATION_REQUEST` and sets `escalationReason="user_requested_escalation"` (not "drift_hard_shift").

**Evidence**: Session escalationReason is "user_requested_escalation" and handover payload matches.

---

## 2. Regression Test Results

### Routing Regression

| ID | Test | Expected | Actual | Result |
|----|------|----------|--------|--------|
| R1 | Strong Prior: "Delete My Account or Data" -> UC-G | intent=UC-G | intent=UC-G | PASS |
| R2 | Strong Prior: "Report a Safety Issue" -> UC-J | intent=UC-J | intent=UC-J | PASS |
| R3 | Weak Prior: "Ad Support" + ad_id -> UC-A | intent non-null, should_end_chat=false | intent=UC-A, should_end_chat=false | PASS |
| R4 | OOS: "Delivery" -> immediate handover | should_end_chat=true | should_end_chat=true | PASS |
| R5 | OOS: "Pro Contract" -> immediate handover | should_end_chat=true | should_end_chat=true | PASS |

### Infrastructure Regression

| ID | Test | Expected | Actual | Result |
|----|------|----------|--------|--------|
| R6 | Health check `/internal/health` | status=UP | status=UP, db=UP, redis=UP | PASS |
| R7 | Demo sessions list | Non-empty list | 61 sessions | PASS |
| R8 | Funnel metrics | Contains totalSessions | totalSessions=61 | PASS |
| R9 | FAQ search graceful return | Returns faq_miss field | faq_miss=true, hits=[] | PASS |

### Error Handling Regression

| ID | Test | Expected | Actual | Result |
|----|------|----------|--------|--------|
| R10 | Missing topic_subject -> 400 | HTTP 400 | HTTP 400 | PASS |
| R11 | Invalid session ID -> 404 | HTTP 404 | HTTP 404 | PASS |
| R12 | Empty message -> 400 | HTTP 400 | HTTP 400 | PASS |

### Edge Case Tests

| ID | Test | Expected | Actual | Result |
|----|------|----------|--------|--------|
| E1 | User-escalated session blocks follow-up | should_end_chat=true | should_end_chat=true, "transferred" message | PASS |
| E2 | FAQ-miss-escalated session blocks follow-up | should_end_chat=true | should_end_chat=true, "transferred" message | PASS |
| E3 | Normal name (O'Brien) not mangled | Name preserved | "O'Brien" in reply | PASS |
| E4 | Handover payload for user-escalated has correct reason | user_requested_escalation | user_requested_escalation in payload | PASS |

---

## 3. New Issues Found

### NEW Issue #1: INTAKE Script Templates Have Unresolved Variables (MEDIUM)

- **File**: `PhaseEvaluator.java:213-216`, `ScriptLibraryService.java:73-87`
- **Severity**: MEDIUM
- **Description**: The INTAKE path in `PhaseEvaluator.resolveIntake()` calls `scriptLibrary.getTemplate("g_process_explanation")` which returns the raw pattern with `{SLA_HOURS}` unsubstituted. The `ScriptLibraryService` has a `renderTemplate(category, variables)` method specifically for variable substitution, but it is not used. All INTAKE UCs that reference templates with variables will show raw `{SLA_HOURS}`, `{CASE_NUMBER}`, `{TEAM_NAME}`, `{PRIVACY_EMAIL}` etc. in customer-facing responses.
- **Reproduction**: Create a UC-G session, send a message. The reply contains literal `{SLA_HOURS}`.
- **Actual**: `"...you'll hear back by email within {SLA_HOURS} hours."`
- **Expected**: `"...you'll hear back by email within 72 hours."` (or whatever the configured SLA is)
- **Affected templates**: `g_process_explanation`, `h_empathy`, `i_disclaimer`, `generic_case_created`, and others with `{SLA_HOURS}`.
- **Suggestion**: Change `PhaseEvaluator.resolveIntake()` to use `scriptLibrary.renderTemplate(key, Map.of("SLA_HOURS", "72"))` instead of `scriptLibrary.getTemplate(key)`. Define SLA values in configuration.

---

## 4. Remaining Issues from Round 1 (Not Targeted for Fix)

The following R1 issues were NOT in scope for this fix round and remain open:

| R1 Issue | Severity | Status | Notes |
|----------|----------|--------|-------|
| #2 Knowledge Base empty | HIGH | OPEN | FAQ search still returns empty results. All FAQ-path sessions escalate after 2 FAQ misses. |
| #5 Funnel vs per-UC metrics discrepancy | MEDIUM | OPEN | Funnel shows 18 escalated; per-UC sum is 6. OOS sessions not counted in per-UC. |
| #6 Per-UC names use UC IDs | MEDIUM | OPEN | `useCaseName: "UC-A"` instead of `"Ad Status & Visibility"` |
| #8 FAQ-miss escalation state inconsistency | MEDIUM | OPEN | Phase transitions to ESCALATE but handlingState stays BOT_HANDLING and containmentOutcome stays null. (Follow-up messages ARE correctly blocked via the phase check from Fix 2, so functional impact is reduced.) |
| #10 No input size validation | MEDIUM | OPEN | 100KB payloads accepted without error. |
| #11 IllegalArgumentException -> 400 | LOW | OPEN | |
| #12 No typed request DTOs | LOW | OPEN | |
| #13 Mutable static SAFE_ESCALATION_RESPONSE | LOW | OPEN | |
| #14 Email in plaintext formContext | LOW | OPEN | |
| #15 CORS hardcoded | LOW | OPEN | |

---

## 5. Test Execution Summary

### Overall Results

| Category | Total | Pass | Fail | Skip |
|----------|-------|------|------|------|
| Fix 1: XSS Sanitization | 7 | 7 | 0 | 0 |
| Fix 2: Escalated Session Rejection | 3 | 3 | 0 | 0 |
| Fix 3: Handover Payload | 8 | 8 | 0 | 0 |
| Fix 4: INTAKE Skip Knowledge | 3 | 3 | 0 | 0 |
| Fix 5: User Escalation Reason | 2 | 2 | 0 | 0 |
| Routing Regression | 5 | 5 | 0 | 0 |
| Infrastructure Regression | 4 | 4 | 0 | 0 |
| Error Handling Regression | 3 | 3 | 0 | 0 |
| Edge Cases | 4 | 4 | 0 | 0 |
| **TOTAL** | **39** | **39** | **0** | **0** |

**Pass Rate: 100% (39/39)**

---

## 6. Verdict

### PASS (conditional)

All 5 targeted fixes are verified as correctly implemented. No regressions detected across 17 regression and edge-case tests. The fixes are well-implemented with defense-in-depth (double sanitization for XSS, multiple checks for escalation blocking).

### Remaining Blockers for Production

**HIGH** (1 remaining from R1):
1. **R1 Issue #2**: Knowledge base returns empty results for all queries -- 100% escalation rate for FAQ-path UCs.

**MEDIUM** (6 remaining: 5 from R1 + 1 new):
1. **R1 Issue #5**: Funnel vs per-UC metrics discrepancy
2. **R1 Issue #6**: Per-UC metrics show UC IDs instead of human-readable names
3. **R1 Issue #8**: FAQ-miss escalation does not update handlingState/containmentOutcome (functional impact mitigated by Fix 2)
4. **R1 Issue #10**: No input size validation (100KB accepted)
5. **NEW Issue #1**: INTAKE script templates have unresolved `{SLA_HOURS}` and other variables in customer-facing responses

**LOW** (5 remaining from R1):
- Issues #11, #12, #13, #14, #15 -- no functional impact

### Priority Recommendation for Next Fix Round

1. **R1 Issue #2 (HIGH)**: Populate knowledge base -- the single biggest gap preventing FAQ self-resolution
2. **NEW Issue #1 (MEDIUM)**: Fix template variable substitution in `PhaseEvaluator.resolveIntake()` -- visible to customers
3. **R1 Issue #8 (MEDIUM)**: Set handlingState and containmentOutcome when PhaseEvaluator escalates -- data consistency
