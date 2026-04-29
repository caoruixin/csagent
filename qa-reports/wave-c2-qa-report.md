# Wave C2 — QA Verification Report

**Generated**: 2026-04-27 19:35 (Asia/Shanghai)
**Scope**: End-to-end verification of the Wave A1.1–A3 eval-pipeline refactor
against the live `csagent` Spring Boot server.
**Test set size**: 6 cases (anchor x4 + exploration x2).

---

## 1. Setup

### 1.1 Server status

* Spring Boot server started from
  `server/target/csagent-server-0.1.0-SNAPSHOT.jar` with
  `-Dspring.profiles.active=local` and `.env.local` exported.
* Postgres 17 + Redis 8 already running (homebrew services).
* Health: `GET /actuator/health` → `200 {"status":"UP", db:UP, redis:UP}`.
* Server log: `qa-reports/server-startup.log` (1.2 MB).
* Admin UI (`localhost:5173/admin`): **NOT running**. Browser smoke is
  therefore **SKIPPED** (see §6).

### 1.2 Eval runner invocation

```
cd eval_interactive
set -a && source ../.env.local && set +a
python -m eval_interactive run \
       --path /tmp/wave_c2_subset \
       --label wave-c2-qa-subset \
       --parallel 2 --verbose
```

* Output dir: `eval_interactive/results/20260427-192226/`
  (`results.json`, `report.html`)
* Captured stdout/stderr: `qa-reports/wave-c2-eval-run.log` (57 KB)
* Total wall time: 28.7 s (every case bailed at the trace-contract step
  before driving turns, so LLM spend was minimal).

### 1.3 Test subset (6 cases, mandated by Wave C2 brief)

| case_id | source set | primary_uc | outcome_class | rationale |
|---|---|---|---|---|
| `cs_interactive_015` | anchor | UC-K | escalate | UC-B → UC-K reclassification (Wave A2.1) |
| `cs_interactive_040` | anchor | UC-K | escalate | UC-K resolve → escalate outcome override (Wave A3) |
| `cs_interactive_192` | anchor | UC-B | resolve   | simple FAQ keeper (UC-B) |
| `cs_interactive_193` | anchor | UC-A | resolve   | simple FAQ keeper (UC-A) |
| `cs_interactive_005` | exploration | UC-H | escalate | UC-H account-suspension appeal |
| `cs_interactive_010` | exploration | UC-G | escalate | UC-G safety/imminent-harm |

Subset assembled at `/tmp/wave_c2_subset/` (the executor accepts a
custom path via `--path`).

---

## 2. Per-case results

| case_id | UC | expected | status | composite | mandatory_l2_passed | active_use_case populated | containment populated | escalation_reason populated |
|---|---|---|---|---|---|---|---|---|
| cs_interactive_005 | UC-H | escalate | **CONTRACT_VIOLATION** | 0.000 | n/a (skipped) | empty (forced by collector) | empty (rejected: `'ESCALATED'` enum_violation) | empty |
| cs_interactive_010 | UC-G | escalate | **CONTRACT_VIOLATION** | 0.000 | n/a | empty (raw payload empty — server 500) | empty | empty |
| cs_interactive_015 | UC-K | escalate | **CONTRACT_VIOLATION** | 0.000 | n/a | empty (raw payload empty — server 500) | empty | empty |
| cs_interactive_040 | UC-K | escalate | **CONTRACT_VIOLATION** | 0.000 | n/a | empty (forced) | empty (`'ESCALATED'` enum_violation) | empty |
| cs_interactive_192 | UC-B | resolve  | **CONTRACT_VIOLATION** | 0.000 | n/a | empty (raw payload empty — server 500) | empty | empty |
| cs_interactive_193 | UC-A | resolve  | **CONTRACT_VIOLATION** | 0.000 | n/a | empty (raw payload empty — server 500) | empty | empty |

* No `PASS`, `FAIL`, `TIMEOUT`, or `ERROR` status emitted — all 6 are
  `CONTRACT_VIOLATION`. No bare-empty `status` strings (Wave B1.4
  contract holds).
* The `failure_tags` field is consistently `["CONTRACT_VIOLATION:<field>"]`.
* `composite_score == 0.0` because the executor never reaches L2/L3.
* The `l1_results` array contains a synthetic `trace_contract_<field>`
  row with `severity=critical`, exactly as designed.

### 2.1 Two distinct contract-violation patterns

**Pattern A (`active_use_case missing`, `available_keys=[]`)**:
4/6 cases. Affects cs_interactive_010 (UC-G), 015 (UC-K), 192 (UC-B),
193 (UC-A). Symptom: the simulator's `agent_client.create_session`
fell back to the synthetic id `error-XXXXXXXX` because the server
returned **HTTP 500** on `POST /v1/chat/sessions`. The collector then
asks the server for a session that doesn't exist; `_safe_call` swallows
the 404 and returns `{}`, so `active_use_case` is missing.
Root cause is server-side (see HIGH-1).

**Pattern B (`containment_outcome enum_violation:value='ESCALATED'`)**:
2/6 cases. Affects cs_interactive_005 (UC-H) and 040 (UC-K). Symptom:
session **was** created and the bot escalated correctly. The server
persists `containmentOutcome="ESCALATED"` (uppercase) but the trace
contract requires the lowercase enum
`{resolved, escalated, abandoned, timeout}`. Root cause is server-side
(see HIGH-2).

### 2.2 Was UC-K policy-allowed `get_customer_context` actually exercised?

For cs_interactive_015 the simulator never reached turn 1 (HTTP 500 on
session creation) — cannot answer. For cs_interactive_040 the bot
*did* reach `phase=ESCALATE` with `escalationReason="intake_complete"`,
but **not** the spec-required
`escalation_reason="intake_complete_for_uc_k"`, and only one tool was
recorded — `request_handover` — without first calling
`get_customer_context` (see HIGH-3 escalation-reason mismatch and
HIGH-4 routing/sequence drift). Note the routing for cs_interactive_040
inferred **UC-C** (not UC-K) on retry — confirmed via log line
`Session 27061a2c... created: phase=ESCALATE, uc=UC-C, routing=ROUTED`.

---

## 3. Findings

### CRITICAL — eval is producing wrong scores or crashing

**(none)** — the eval pipeline behaved exactly as Wave B1.4 intended.
Every case got a deterministic, distinguishable `CONTRACT_VIOLATION`
status with structured failure detail; no crash, no silent coercion of
empties, no spurious `FAIL` for bot misbehaviour. The new
`TraceContractError` path in
`eval_interactive/eval_interactive/batch/executor.py:163-175` and the
synthetic `_contract_violation_result` builder
(`executor.py:364-420`) work as specified.

### HIGH — eval is right, server has a bug

#### HIGH-1 — `articles_shown` NOT NULL constraint violated on first save during ROUTED+FAQ auto-search

* **Layer**: server (Java)
* **File**: `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:139`
  (also reproduces at `:189` for non-FAQ paths but is patched there by `:183-185`)
* **Schema constraint**: `server/src/main/resources/db/migration/V1__create_bot_sessions.sql:21` —
  `articles_shown TEXT[] NOT NULL DEFAULT '{}'`
* **What goes wrong**: `BotSession.builder()` at `SessionManager.java:86-100`
  does not initialise `articlesShown`. For routed FAQ UCs (UC-A, UC-B,
  UC-C, UC-D, UC-E, UC-F, UC-FP), the auto-search path calls
  `sessionRepository.save(session)` at line 139 *before* the defensive
  null-coalesce at lines 183-185 runs. Hibernate maps `null` Java
  field → SQL `null`, bypassing the column DEFAULT, and the INSERT
  fails with
  `ERROR: null value in column "articles_shown" of relation "bot_sessions" violates not-null constraint`.
* **Failing INSERT row** (from server log): `(b2b354e6-..., null, null, bot_v1, BOT_HANDLING, RESOLVE, UC-D, ...)` — second column is `articles_shown`, value is `null`.
* **Cascade**: the JPA transaction is marked rollback-only. All
  subsequent reads in the same request (`BotTurnRepository.findBySessionId`,
  `BotEventRepository.findById`) fail with
  `ERROR: current transaction is aborted, commands ignored until end of transaction block`.
  The 500 returned to the client is `UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only`.
* **Repro recipe**:
  ```
  /usr/bin/curl -s -X POST http://localhost:8080/v1/chat/sessions \
    -H "Content-Type: application/json" \
    -d '{"first_name":"X","email":"x@x.com","topic_subject":"Ad Support","ad_id":"","description":"I want to give away free items.  Can I do this on your site?"}'
  ```
  → `{"error":"Transaction silently rolled back...","status":500}`
* **Suggested direction**: either initialise `articlesShown(new String[0])`
  in the builder at `SessionManager.java:86-100`, or move the
  null-coalesce block (lines 183-185) before the first `save(session)`
  call at line 139. (Same fix should be applied to
  `candidateUseCases` for symmetry, even though it is currently
  populated by the router.)
* **Observed scope**: 4/6 of our cases hit this. In a full corpus this
  will fail every routed FAQ UC.

#### HIGH-2 — `containment_outcome` persisted as uppercase enum, contract requires lowercase

* **Layer**: server (Java) vs. eval contract
* **Files** (uppercase writes):
  - `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:163` — `setContainmentOutcome("ESCALATED")` (OOS path)
  - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:659` — `setContainmentOutcome("RESOLVED")`
  - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:670` — `setContainmentOutcome("ESCALATED")`
  - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:736` — `setContainmentOutcome("ESCALATED")`
  - `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java:183` — `setContainmentOutcome("ESCALATED")`
* **Contract**: `eval_interactive/eval_interactive/trace/collector.py:49-51`
  ```
  CONTAINMENT_OUTCOME_VALUES: frozenset[str] = frozenset(
      {"resolved", "escalated", "abandoned", "timeout"}
  )
  ```
* **Live evidence**:
  ```
  /usr/bin/curl -s http://localhost:8080/v1/chat/sessions/5d10af70-bc72-405b-8b6c-53b94619e42f
  → {"containmentOutcome":"ESCALATED", "handlingState":"QUEUE_TO_HUMAN", ...}
  ```
* **Reference**: phase3 doc `docs/phase3_detailed_technical_design.md:260`
  comments the column as `RESOLVED/ESCALATED/ABANDONED` (uppercase) — the
  *server* is internally consistent with the old design. The Wave B1.4
  contract switched to lowercase; the server side was never updated.
* **Suggested direction**: pick one and align. Options:
  (a) lowercase the five Java write-sites and migrate any historical rows
      (`UPDATE bot_sessions SET containment_outcome = LOWER(containment_outcome)`),
  (b) extend `CONTAINMENT_OUTCOME_VALUES` to accept both cases and
      normalise to lowercase in `_build_session_state`. Recommendation:
      (a), because the case-spec `outcome_class` is already lowercase
      (`escalate`/`resolve`) and downstream `correct_outcome` mapping
      currently does `{"resolved":"resolve", "escalated":"escalate"}`
      (`batch/executor.py:501-505`) — the lowercase wire format is the
      "real" contract.
* **Observed scope**: 2/6 in our subset; will hit every successful
  session in the full corpus.

#### HIGH-3 — `escalation_reason` emits free-form `intake_complete`, not the enum trigger

* **Layer**: server (Java) vs. eval contract
* **Files**:
  - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:392` — `PhaseResult.escalate(session, msg, "intake_complete")`
  - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:421` — same string
* **Contract**: `eval_interactive/eval_interactive/case_spec/schema.py:43-66`
  enumerates 22 valid `EscalationTrigger` values including
  `intake_complete_for_uc_g`, `intake_complete_for_uc_h`,
  `intake_complete_for_uc_i`, `intake_complete_for_uc_j`,
  `intake_complete_for_uc_k`, and `appeal_requires_human`. Bare
  `"intake_complete"` is **not** a valid value.
* **Live evidence** (cs_interactive_005, UC-H, expected
  `appeal_requires_human`):
  `escalationReason: 'intake_complete'`.
* **Trace contract impact**: when `request_handover` is recorded with
  `escalation_reason="intake_complete"` it triggers the per-tool
  `enum_violation` path in `trace/collector.py:480-489`, becoming a
  *third* possible `CONTRACT_VIOLATION` mode the eval will now surface.
  In our 6-case run this didn't fire because Pattern A/B aborted first,
  but it will fire in the full corpus.
* **Suggested direction**: in `PhaseEvaluator.evaluateResolve` /
  `PhaseEvaluator.escalate(...)`, derive the trigger from the active UC
  (UC-G→`intake_complete_for_uc_g`, UC-H→`appeal_requires_human` per
  spec, UC-I→`intake_complete_for_uc_i`, UC-J→`intake_complete_for_uc_j`,
  UC-K→`intake_complete_for_uc_k`, etc.) instead of hard-coding
  `"intake_complete"`. Same fix for
  `SessionManager.java:352-353` (`getEscalationReason().startsWith("intake_complete")`)
  if it gates anything.

#### HIGH-4 — UC-K request mis-routed to UC-C (routing accuracy)

* **Layer**: server (LLM router prompt or thresholds)
* **File**: `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRouter.java`
  (entry point `route(...)`)
* **Symptom**: cs_interactive_040 description "My ad isn't allowing
  customers to send requests. There is a flaw on the app" should
  classify as **UC-K** (runtime/state issue with the user's own ad)
  per Wave A3 case spec (after the explicit
  `OUTCOME_OVERRIDES["570Q5000008pMbOIAU"]` extractor entry). Live
  routing repeatedly returns **UC-C** with confidence 0.85, then
  triggers the auto-search FAQ path, then 500s on HIGH-1.
  Server log line:
  `Session 27061a2c-...: created: phase=ESCALATE, uc=UC-C, routing=ROUTED`.
* **Suggested direction**: this is an LLM-router behaviour issue, not
  an eval-pipeline issue, but the eval will keep flagging UC-K cases
  as `correct_uc=fail` until the router prompt or training set is
  tuned. Out of scope for the fix-issues wave that addresses HIGH-1/2/3,
  but worth a separate ticket.

### MEDIUM — test infra noise

#### MEDIUM-1 — Simulator's `error-XXXXXXXX` synthetic session id leaks into the trace endpoints

* **Layer**: eval (Python)
* **File**: `eval_interactive/eval_interactive/simulator/agent_client.py`
  + `eval_interactive/eval_interactive/simulator/session_runner.py`
  (the path that swallows a 500 on create_session and substitutes
  `error-XXXXXXXX`)
* **Symptom**: the resulting `error-...` id is then used to GET
  `/v1/chat/sessions/error-...` (404), `/v1/demo/sessions/error-.../trace`
  (200, empty list), and `/v1/demo/handover-logs` (200). The `_safe_call`
  fallbacks return `{}` / `[]` so the contract violation we see is
  `available_keys=[]` rather than a more informative
  `bot_create_session_failed`.
* **Impact**: the failure surfaces correctly as
  `CONTRACT_VIOLATION`, but a developer reading the report has to
  cross-reference the server log to discover that the *real* upstream
  cause is a 500 on session creation. A dedicated synthetic
  `BOT_API_ERROR` status in the executor would short-circuit this
  cleanly.
* **Suggested direction**: in `_execute_case_sync`, detect the
  `error-` prefix on `session_result.session_id` (or, better, surface
  the real `httpx.HTTPStatusError` from `agent_client.create_session`)
  and produce a distinct `status="BOT_API_ERROR"` result mirroring
  `_contract_violation_result`. Note: explicitly allowed to be
  out-of-scope for the fix wave; HIGH-1 will remove the symptom
  anyway.

### LOW — cosmetic / quality

#### LOW-1 — `intake_fields_collected` outcome-check naming inconsistency

* **Files**: `eval_interactive/case_specs/anchor/cs_interactive_040.yaml:62`
  and `cs_interactive_015.yaml:64` both list
  `outcome_checks: [..., intake_fields_collected]` — but the
  corresponding intake fields list isn't surfaced anywhere in the spec
  (only the natural-language `bot_handling_pattern` mentions
  `platform, repro_steps_or_error_message`). When the eval can finally
  run a full session for these cases, the `intake_fields_collected`
  L2 check has no machine-readable expected list to compare against.
  Cosmetic for now; relevant once HIGH-1/2/3 unblock real runs.

#### LOW-2 — Server log noisy with full repeated stack traces for the same INSERT failure

* The single root cause (HIGH-1) prints ~150-line stack traces 4x
  consecutively per request, mixing `DataIntegrityViolationException`
  + `current transaction is aborted` echoes from the dispatcher's
  catch sites. Once HIGH-1 is fixed this disappears, but in the
  meantime it makes log triage tedious. Suggest adding a session-id
  dedupe in the controller's exception handler.

---

## 4. Trace contract verification

* **Were any contract violations produced?** Yes — 6/6, exactly as
  intended. Two distinct field+reason pairs covered (`active_use_case
  missing` and `containment_outcome enum_violation`). The third pair
  (`escalation_reason enum_violation` at the tool-call phase) didn't
  fire because no case got far enough to record a `request_handover`
  tool call after the contract was tightened.
* **Are blank-field results gone?** Yes. Inspect any
  `case_results[i].status` in `results.json` — every value is one of
  `PASS`, `FAIL`, `TIMEOUT`, `ERROR`, `CONTRACT_VIOLATION` (no `""`).
  The synthetic `l1_results[0]` row carries
  `check="trace_contract_<field>", passed=false, severity=critical`
  with structured `available_keys`.
* **Are mandatory L2 gates respected on contract violations?** The
  contract path bypasses `compute_composite()` entirely (executor
  returns the synthetic dict at `_contract_violation_result()`), so
  `mandatory_l2_passed` / `mandatory_l2_failures` are not present in
  contract-violation results. This is the documented design and
  matches the executor source (`executor.py:241-308` vs. `:364-420`).
* **`available_keys` provides good blame data** — for Pattern B we
  can see the exact 29-key snake/camel inventory the server returned,
  which makes pinpointing the casing bug straightforward.

---

## 5. API smoke (manual)

Mirroring cs_interactive_015 and cs_interactive_040.

### 5.1 cs_interactive_015 (UC-K, UC-B → UC-K reclassified)

```
/usr/bin/curl -s -X POST http://localhost:8080/v1/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{"first_name":"Adam","email":"customer@example.com",
       "topic_subject":"Ad Support","ad_id":"",
       "description":"Hi - can you tell me what happened to my ad?"}'
```
* **Result**: HTTP 500 — `{"error":"Transaction silently rolled back ...","status":500}`
* **Shape verdict**: **FAIL** (no normal payload returned).
* **Root cause**: HIGH-1.

### 5.2 cs_interactive_040 (UC-K, escalate)

```
/usr/bin/curl -s -X POST http://localhost:8080/v1/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{"first_name":"Mo","email":"customer@example.com",
       "topic_subject":"Account Support","ad_id":"",
       "description":"My ad isnt allowing customers to send requests. There is a flaw on the app"}'
```
* **Result**: HTTP 500 — same rollback.
* **Reason**: routing inferred UC-C (not UC-K — see HIGH-4) → routed to
  the FAQ auto-search path → HIGH-1 fires.
* **Shape verdict**: **FAIL**.

### 5.3 Sanity check on a UC-H success path (intake UC, skips auto-search)

```
RESP=$(/usr/bin/curl -s -X POST http://localhost:8080/v1/chat/sessions \
  -d '{"first_name":"Kenny2","email":"customer@example.com",
       "topic_subject":"Account Support","ad_id":"",
       "description":"Why have my paid for gumtree ads been removed?"}'
  -H "Content-Type: application/json")
# → {"intent":"UC-H","session_id":"20161e44-...","reply_text":"Hi Kenny2! ...","should_end_chat":false}
SID=20161e44-74f1-4ec3-b167-5dfd0e89b108
/usr/bin/curl -s http://localhost:8080/v1/chat/sessions/$SID
# → {"sessionId":"20161e44-...","activeUseCase":"UC-H","containmentOutcome":null, ...}
/usr/bin/curl -s -X POST .../$SID/messages -d '{"message":"*Nursan"}'
# → {"intent":"UC-H","session_id":"...","reply_text":"...","should_end_chat":false,"additional_data":{"latency_ms":5833}}
/usr/bin/curl -s .../trace
# → list of {"turnId","sessionId","turnIndex","userMessage","botResponse","actionSelected","toolCalls","phaseBefore","phaseAfter","activeUseCase","latencyMs",...}
/usr/bin/curl -s .../events
# → [{"eventId","sessionId","eventType","turnIndex","payload":"<JSON-string>","createdAt"}]
```
* **Shape verdict for a successful UC-H session**: **PASS** for the
  field set the trace collector reads
  (`activeUseCase`, `containmentOutcome`, `currentPhase`,
  `escalationReason`, plus per-turn `phaseAfter`, `toolCalls`, etc.).
  Casing is camelCase, which the collector accepts via the `_g()`
  helper. **Caveat**: when this same session reaches escalation it
  will write `containmentOutcome="ESCALATED"` and trip HIGH-2.

### 5.4 Endpoint-shape conclusions

* All four read endpoints used by the trace collector
  (`GET /v1/chat/sessions/{id}`, `GET /v1/demo/sessions/{id}/trace`,
  `GET /v1/demo/sessions/{id}/events`, `GET /v1/demo/handover-logs`)
  return the structurally correct payloads when reachable.
* The contract failures in §2 are **value-level**, not shape-level.
* Create-session POST returns `session_id` (snake) but GET-session
  returns `sessionId` (camel) — the simulator handles both, and the
  collector handles both via `_g()`. Recommend documenting this
  mixed-casing publicly so future agents don't accidentally rely on
  one or the other.

---

## 6. Browser smoke (admin UI)

* **Status**: **SKIPPED**
* **Reason**: the Vite dev server at `localhost:5173/admin` is not
  running (HTTP 000 / connection refused). The Wave C2 brief
  explicitly permits skipping if browser tooling is unavailable, and
  the `claude-in-chrome` MCP tools were not loaded in this agent
  session either.
* **Action for next agent**: in a follow-up wave, start the UI with
  `cd ui && npm run dev` (per repo `Makefile`) and verify the
  Handover Queue table renders with at least the sessions surfaced
  by `GET /v1/demo/handover-logs`.

---

## 7. Recommendations for the fix-issues wave

Priority order:

1. **Fix HIGH-1** (`articles_shown` NOT NULL on first save). One-line
   builder fix in `SessionManager.java:86-100`. Without this, ~70 %
   of the corpus (every routed FAQ UC) cannot run a single turn.
2. **Fix HIGH-2** (`containmentOutcome` casing). Five Java write-sites,
   plus a one-shot `UPDATE bot_sessions SET containment_outcome =
   LOWER(...)` for any pre-existing rows in dev DBs.
3. **Fix HIGH-3** (`escalation_reason` enum). Per-UC mapping in
   `PhaseEvaluator.escalate(...)` so the value is one of the 22
   allowed `EscalationTrigger` literals.
4. **Re-run this Wave-C2 6-case subset** end-to-end. Expect:
   - 0 `CONTRACT_VIOLATION` results
   - 4–6 cases reaching L2/L3 with composite scores between 0.4 and 0.9
   - cs_interactive_040 and cs_interactive_015 should at minimum
     trigger `correct_outcome=true` (escalate path completes).
5. **Then** investigate HIGH-4 (UC-K mis-routing) as a separate
   router-tuning ticket. The eval is now able to *measure* this
   regression cleanly; the router fix itself is policy/prompt work.
6. **Defer** MEDIUM-1 / LOW-1 / LOW-2 unless they keep biting in the
   re-run.

---

## 8. Artefacts

* `qa-reports/wave-c2-eval-run.log` — full eval-runner stdout/stderr
* `qa-reports/server-startup.log` — Spring Boot log including the
  full failing INSERT row and stack traces
* `eval_interactive/results/20260427-192226/results.json` —
  machine-readable per-case results
* `eval_interactive/results/20260427-192226/report.html` — HTML view
* `/tmp/wave_c2_subset/` — the 6-case test set (re-creatable from
  the canonical `eval_interactive/case_specs/{anchor,exploration}/`)
