---
title: "E2E findings — 2026-07-25"
doc_tier: diagnostic
status: diagnostic
implementation_status: unknown
source_of_truth: "live service evidence where available; otherwise cited runtime source"
last_reviewed: 2026-07-25
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Authored by the suite author from a network-isolated runner (§"Original
  authoring run"). SUPERSEDED IN PART on 2026-07-25 by a live execution from
  the orchestrating session, which could reach the loopback services — see
  §"Live execution" and §"Triage". Three of the four initial live failures were
  adjudicated as TEST defects, not product defects. One new product defect
  (P1-04) was surfaced only by live execution. Browser scenarios 7–8 remain
  unverified.
---

# csagent E2E findings — 2026-07-25

## Live execution (authoritative)

Executed from the orchestrating session against the user-managed services
(backend `localhost:8080` with Postgres on port **5442**, Redis up, UI on
`localhost:5173`). Dependencies installed into `e2e/.venv` via `uv`.

**Run 1 — before any fix:** 4 failed / 2 passed (78 s).
**Run 2 — after the P1-01/02/03 fixes and a backend restart:** **6 passed /
2 xpassed** (46 s). The two `xpass` entries are the deliberately-not-weakened
assertions for P1-01 and P1-03, which flipped once the fixes were served.

Browser scenarios 7–8 (chat UI, admin trace) remain **unverified** — driving a
real Chrome requires the operator to approve a browser connection, which did
not occur in this session. Unverified is not a pass.

## P0

**None.** Confirmed by live execution, not by inference. The Salesforce
Knowledge-id (`ka…`) leak assertion did run after the P1-02 fix and found no
leak in the assembled article payload.

## Triage of the four initial live failures

Each failure was adjudicated against the real trace pulled from postgres
(`bot_turns.tool_calls`) before any code was changed. **Three were test
defects.**

| Test | Verdict | Basis |
|---|---|---|
| `test_01` malformed JSON → 500 | **Product defect** (P1-01) | Confirmed; fixed. |
| `test_03` first reply omits form context | **Product defect, mis-framed assertion** (P1-03) | The create-session path deliberately makes **no LLM call** (`SessionManager.java:121-131`, Sprint 8.1 §M0). The assertion wrongly demanded turn-1 semantic use of form context; the *real* defect is narrower — the static greeting re-asked for information the form already supplied. Assertion re-scoped to turn 2+, greeting obligation split out; fixed. |
| `test_04` mutation ask → no handover | **TEST defect** | The bot asked one clarifying question separating "understand why it was removed" from "appeal to get it reinstated" — exactly the branch `PRD_biz_part.md:205` (F′1) prescribes. The test sent one message and asserted handover without ever answering the question. Demanding escalation *before* disambiguation is the mirror image of the over-escalation problem this whole effort targets. Test now drives the conversation to completion. |
| `test_06` read-only dialogue → `request_handover` in trace | **TEST defect** — *but see P1-04* | The assertion substring-matched the whole serialised trace, which includes the **tool schema projected to the LLM**; `request_handover` appears ~8× per turn in `projectedContext` on sessions that never invoke it. The cited session ended `containmentOutcome=resolved`, `escalationReason=None`. Replaced with real `toolCalls` parsing plus terminal-state assertions. |

## P1-04 — identical repeated `search_knowledge` exhausts the step budget and forces a handover

Surfaced only after `test_06` was corrected — the naive assertion had been
masking it behind a false positive.

**Reproduction:** ~20 % (1 failure in 5 runs of the corrected `test_06`).

**Evidence** — session `81a08aeb-01fd-446e-9eb1-f1519ee45327`, read directly
from `bot_turns.tool_calls`:

```
turn 1  DISCOVER->RESOLVE uc=UC-A
        classify_use_case, get_customer_context,
        search_knowledge("ad visibility low visibility how to improve"),
        search_knowledge("ad visibility low visibility tips"),
        resolve_article, record_outcome
turn 2  RESOLVE->RESOLVE
        search_knowledge("what does LIVE status mean for my advert"), resolve_article
turn 3  RESOLVE->ESCALATE
        search_knowledge("improve ad visibility tips")   x4  <- byte-identical
        resolve_article, resolve_article,
        request_handover(escalation_reason=turn_budget_exhausted)
```

The four turn-3 queries are **byte-identical**, not paraphrases, so the A1
per-run identity cache (`AgentRunLoopImpl.java:613-651`, keyed on
`toolName + canonicalArgumentsHash`) should have suppressed them. They still
consumed the 6-step budget, after which the deterministic backstop stamped
`turn_budget_exhausted` and handed over — on a **pure read-only UC-A status
query the bot had already answered in turns 1–2**.

**Layer:** `infra` / `skill_state`, not `semantic_planner`. Idempotency is
Runtime-owned under Constitution §1.4, so a deterministic in-turn backstop is
legitimate here and is not a §1.7 semantic hardcode. Precedent:
`AgentRunLoopImpl.java:674-682` records that the soft-signal approach was
empirically falsified and that the backstop must be "the deterministic
substrate the LLM cannot route around".

**Significance beyond this bug:** M-Auto-11 characterised MAX_STEPS
over-escalation on satisfiable flows as **k=0/11, non-reproducing**
(`docs/sprints/sprint-101-handoff.md`). Live E2E reproduces it at ~20 %. That
is a material update to a closed characterisation, and it is the concrete
mechanism behind the product owner's "it escalates when it shouldn't".

## Original authoring run (network-isolated, superseded)

The suite author's runner could see the host listeners (java on `*:8080`, node
on `127.0.0.1:5173`) but could not connect to either loopback stack; PyPI and
the npm registry did not resolve, so dependencies could not be installed and no
scenario executed. That run reported 0 passed / 0 failed / 8 unverified, and
its three P1 findings below were derived from code paths rather than live
behaviour. All three were subsequently confirmed by live execution and fixed.
No service was restarted by that runner.

## P1 findings

### P1-01 — Malformed JSON is deterministically converted to HTTP 500

Reproduction:

    curl --noproxy '*' -i http://localhost:8080/v1/chat/sessions \
      -H 'Content-Type: application/json' \
      --data-binary '{"topic_subject":'

Or run test_01_health_and_invalid_request_contracts.

Actual: POST /v1/chat/sessions binds the request to Map<String, String>
(server/src/main/java/com/gumtree/csagent/controller/ChatController.java:60-66).
A JSON parse/bind failure has no specific handler. The catch-all
Exception handler creates status 500 and returns HTTP 500
(server/src/main/java/com/gumtree/csagent/controller/GlobalExceptionHandler.java:23-29).
The supplied backend log independently records HttpMessageNotReadableException
as Unhandled exception.

Expected: malformed JSON is client input and must be a suitable 4xx (normally
400), per scenario 6 and the P1 definition “status code inappropriate.”

Evidence: e2e/tests/test_api_e2e.py::test_01_health_and_invalid_request_contracts;
ChatController.java:60-66; GlobalExceptionHandler.java:23-29. A live HTTP
response artifact was not generated because this runner's loopback access was
blocked. The exact 500 result is nevertheless deterministic from the handler.

### P1-02 — Chat UI cannot render FAQ ArticleCard/source links from normal bot replies

Reproduction: run scenario 7 in e2e/tests/chat-ui.spec.ts after installing the
e2e dependencies.

Actual: SessionManager constructs a normal post-message response with
additionalData containing only latency_ms
(server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:377-382).
ChatWidget forwards that value unchanged
(ui/src/components/chat/ChatWidget.tsx:67-77). MessageBubble renders article
cards only from additional_data.articles
(ui/src/components/chat/MessageBubble.tsx:15,51-53), and ArticleCard is the
component which emits the source anchor link
(ui/src/components/chat/ArticleCard.tsx:7-12).

Consequently the required ArticleCard path cannot be reached by the current API
contract, even if retrieval found a source. An inline URL in reply_text would
not repair the required ArticleCard/source-link acceptance.

Expected: pure FAQ self-service needs a source citation, and the requested
browser test requires the source article card/link to render. This is a key
UI-flow/grounding-contract violation.

Evidence: browser assertion in e2e/tests/chat-ui.spec.ts; source paths above.
Expected screenshot e2e/artifacts/07-chat-e2e.png was not generated because the
browser service and loopback access were unavailable.

### P1-03 — First reply after pre-chat omits supplied ad_id and description

Reproduction:

    curl --noproxy '*' -i http://localhost:8080/v1/chat/sessions \
      -H 'Content-Type: application/json' \
      -d '{"first_name":"Riley","email":"riley@example.com","topic_subject":"Ad Support","ad_id":"AD-2002","description":"My ad is not performing; not many views."}'

Or run test_03_listing_status_uses_prefilled_ad_context.

Actual: the create-session path deliberately sends a static greeting and
defers the first normal/LLM message turn
(server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java:142-157).
It returns that greeting with empty additional_data
(SessionManager.java:294-300). buildGreeting accepts only name, topic, and
active UC, so it cannot include ad_id or description
(SessionManager.java:396-400). When routing is ambiguous, buildAmbiguousGreeting
instead asks the customer to “tell me a bit more”
(SessionManager.java:410-414), despite a completed description field.

This is stable by construction: neither greeting-builder signature accepts the
form ad id or description. The live text for Ad Support could not be captured
from this sandbox.

Expected: for a read-only status request with AD-2002 and a description already
in the form, the agent should use that context and not ask the customer to
restate it. This directly matches the stated product criterion and validation
lead.

Evidence: e2e/tests/test_api_e2e.py::test_03_listing_status_uses_prefilled_ad_context;
SessionManager.java:118-119,142-157,294-300,396-414. No live response, trace,
or screenshot artifact was generated due to runner isolation.

## Dynamic scenarios not verified

The following are implemented as real assertions but have no invented result:

1. FAQ self-service versus handover and customer-visible source.
2. AD-2002 lookup, a concrete status response, and no repeated ad-id request.
3. Restore-ad mutation escalation with non-empty summary and escalation_reason.
4. Explicit “talk to a human” escalation with user_requested.
5. Three-turn state/use-case progression with no repeated response or ad-id request.
6. Missing topic_subject, malformed JSON, and absent session status contracts.
7. Browser chat screenshot e2e/artifacts/07-chat-e2e.png.
8. Admin list/trace screenshot e2e/artifacts/08-admin-trace.png.

## Product-criterion caveat

“All data modifications must transfer to a human” is directionally safe but
too broad as a durable rule. It should distinguish a customer asking whether or
how a refund, deletion, or restoration works (an FAQ/read-only explanation)
from an authenticated request to perform that change. Account-data deletion
also carries privacy-right and identity-verification concerns: human handover
may be the correct current policy, but that should be explicit rather than
inferred solely from the verb “delete.” The suite uses the action form “Please
restore my advert,” so it does not blur these intents.

