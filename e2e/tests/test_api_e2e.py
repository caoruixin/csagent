"""Live API E2E checks for the customer-service product contract.

These tests deliberately use the running local stack. They neither start nor
restart services. Every HTTP client has ``trust_env=False`` because the macOS
system proxy must never receive localhost traffic.

The assertion messages name the 2026-07-25 product criterion being checked so
a failure is reviewable without reconstructing the test's intent.

Three assertion defects were corrected on 2026-07-25 after checking each
failure against the real trace. All three were test defects, not product
defects; the notes are kept inline so they are not re-introduced.

* **Assertion timing.** ``POST /v1/chat/sessions`` never calls the LLM
  (``SessionManager.java:121-131``, Sprint 8.1 §M0 / Sprint 8 §D1): it ingests
  the form, routes deterministically, and returns a *static* greeting. UC
  classification and any use of the form's ``ad_id`` / ``description`` happen
  on the **next** message. Any "did the bot use the form context" assertion
  therefore belongs on turn 2 or later.
* **Conversation not driven to completion.** The bot is allowed to disambiguate
  before it commits to a branch. ``PRD_biz_part.md:205`` (F′1) splits
  "why was my ad removed" (self-serve explanation + repost guidance) from
  "I want it reviewed / it was a mistake" (fixed script + G4 human). A single
  "restore my advert" message is ambiguous across that split, so asserting a
  handover after one turn demands the very premature escalation the product
  owner is complaining about. Answer the clarifying question first, then
  assert.
* **Substring matching over the serialized trace.** ``projectedContext``
  contains the tool inventory offered to the LLM, so *every* tool name --
  ``request_handover`` included -- appears in the trace text on every turn
  (measured: 8 occurrences per turn in session
  ``27cc90c5-6322-4fb9-9e0b-c21f8c3a7a65``, which invoked no such tool).
  Assert on the parsed ``toolCalls`` field or on terminal session state, never
  on trace text. There is deliberately no ``_trace_text`` helper in this file.
"""

from __future__ import annotations

import json
import os
import re
import uuid
from pathlib import Path
from typing import Any

import httpx
import pytest


API_BASE_URL = os.environ.get("CSAGENT_API_URL", "http://localhost:8080").rstrip("/")
ARTIFACTS = Path(__file__).resolve().parents[1] / "artifacts"
HTTP_TIMEOUT_SECONDS = 75.0
INTERNAL_SF_ARTICLE_ID = re.compile(r"\bka[A-Za-z0-9]{13}(?:[A-Za-z0-9]{3})?\b")
AD_ID_REQUEST = re.compile(
    r"(?:what|which|share|send|provide|enter|need).{0,45}"
    r"(?:ad(?:vert)?|listing)[ -]?(?:id|number)",
    re.IGNORECASE,
)
# An open-ended request for the customer to restate the problem they already
# typed into the pre-chat form's ``description`` field. Narrow on purpose: it
# matches "tell me more about what you need help with" / "describe your issue",
# not a bare "Hi Riley!" or a *specific* follow-up question (which is
# legitimate intake, not a re-request of supplied data).
RESTATE_PROBLEM_REQUEST = re.compile(
    r"(?:tell|give|share with)\s+me\s+(?:a\s+bit\s+)?more\s+about\s+what"
    r"|what\s+(?:exactly\s+)?(?:do\s+you\s+need\s+help\s+with"
    r"|can\s+i\s+help\s+you\s+with"
    r"|(?:seems\s+to\s+be|is)\s+the\s+(?:problem|issue|matter))"
    r"|(?:describe|explain|outline)\s+(?:your|the)\s+(?:issue|problem|situation)",
    re.IGNORECASE,
)


def _json_or_text(response: httpx.Response) -> Any:
    try:
        return response.json()
    except json.JSONDecodeError:
        return response.text


def _write_artifact(name: str, value: Any) -> None:
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    safe_name = re.sub(r"[^A-Za-z0-9_.-]", "-", name)
    (ARTIFACTS / safe_name).write_text(
        json.dumps(value, ensure_ascii=False, indent=2, default=str), encoding="utf-8"
    )


def _capture(name: str, response: httpx.Response) -> Any:
    body = _json_or_text(response)
    _write_artifact(
        name,
        {
            "request": {
                "method": response.request.method,
                "url": str(response.request.url),
                "content": response.request.content.decode("utf-8", errors="replace"),
            },
            "response": {"status_code": response.status_code, "body": body},
        },
    )
    return body


@pytest.fixture()
def api() -> httpx.Client:
    """Supply a live client or honestly skip when this runner cannot reach it."""
    with httpx.Client(base_url=API_BASE_URL, timeout=HTTP_TIMEOUT_SECONDS, trust_env=False) as client:
        try:
            probe = client.get("/actuator/health")
        except httpx.HTTPError as exc:
            pytest.skip(
                f"UNVERIFIED: cannot reach {API_BASE_URL} from this test runner: {exc}. "
                "The test does not start or restart the user-managed backend."
            )
        _capture("api-preflight-health.json", probe)
        yield client


def _suffix() -> str:
    return uuid.uuid4().hex[:8]


def _form(*, topic: str = "Ad Support", ad_id: str = "AD-2002", description: str) -> dict[str, str]:
    token = _suffix()
    return {
        "first_name": "Riley",
        "email": f"riley.e2e.{token}@example.test",
        "topic_subject": topic,
        "ad_id": ad_id,
        "description": description,
    }


def _create(api: httpx.Client, label: str, **kwargs: Any) -> dict[str, Any]:
    response = api.post("/v1/chat/sessions", json=_form(**kwargs))
    body = _capture(f"{label}-create.json", response)
    assert response.status_code == 200, f"session creation must succeed: {body}"
    assert isinstance(body, dict) and body.get("session_id"), f"missing session_id: {body}"
    return body


def _message(api: httpx.Client, session_id: str, label: str, text: str) -> dict[str, Any]:
    response = api.post(f"/v1/chat/sessions/{session_id}/messages", json={"message": text})
    body = _capture(f"{label}-message.json", response)
    assert response.status_code == 200, f"message endpoint must accept a valid session: {body}"
    assert isinstance(body, dict) and body.get("reply_text"), f"bot response missing reply_text: {body}"
    return body


def _session(api: httpx.Client, session_id: str, label: str) -> dict[str, Any]:
    response = api.get(f"/v1/chat/sessions/{session_id}")
    body = _capture(f"{label}-session.json", response)
    assert response.status_code == 200 and isinstance(body, dict), f"session retrieval failed: {body}"
    return body


def _trace(api: httpx.Client, session_id: str, label: str) -> Any:
    response = api.get(f"/v1/demo/sessions/{session_id}/trace")
    body = _capture(f"{label}-trace.json", response)
    assert response.status_code == 200, f"trace endpoint failed: {body}"
    return body


def _handover_payload_for(api: httpx.Client, session_id: str, label: str) -> dict[str, Any]:
    # NOTE: this endpoint returns EVERY handover log with its full transcript
    # (~42 MB on a used database). Filter to this session before writing the
    # artifact; dumping the whole response made the artifact unusable.
    response = api.get("/v1/demo/handover-logs")
    assert response.status_code == 200, f"handover logs unavailable: {response.status_code}"
    logs = _json_or_text(response)
    assert isinstance(logs, list), f"handover logs must be a list: {str(logs)[:400]}"
    matching = [entry for entry in logs if entry.get("sessionId", entry.get("session_id")) == session_id]
    _write_artifact(
        f"{label}-handover-logs.json",
        {"session_id": session_id, "total_logs": len(logs), "matching": matching},
    )
    assert matching, f"no handover log recorded for session {session_id}"
    entry = matching[-1]
    payload = entry.get("handoverPayload", entry.get("handover_payload", entry.get("payload", {})))
    if isinstance(payload, str):
        payload = json.loads(payload)
    assert isinstance(payload, dict), f"handover payload must be an object: {payload}"
    return payload


def _turn_records(trace: Any) -> list[dict[str, Any]]:
    """Normalise the trace endpoint's payload to a list of turn records.

    ``GET /v1/demo/sessions/{id}/trace`` returns a bare JSON array of turn
    records today; tolerate a ``{"turns": [...]}`` envelope so a future
    endpoint change surfaces as an assertion failure rather than a silently
    empty tool list.
    """
    if isinstance(trace, list):
        return [t for t in trace if isinstance(t, dict)]
    if isinstance(trace, dict):
        turns = trace.get("turns") or trace.get("turn_traces") or []
        return [t for t in turns if isinstance(t, dict)]
    return []


def _invoked_tools(trace: Any) -> list[str]:
    """Tool names the runtime ACTUALLY invoked, in turn order.

    Reads the per-turn ``toolCalls`` field (a JSON-encoded string on this
    endpoint) rather than matching substrings over the serialized trace.
    ``projectedContext`` embeds the tool inventory handed to the LLM, so a
    substring search for any tool name -- ``request_handover`` most damagingly
    -- matches on every turn regardless of what ran. See the module docstring.
    """
    names: list[str] = []
    for turn in _turn_records(trace):
        calls = turn.get("toolCalls", turn.get("tool_calls")) or []
        if isinstance(calls, str):
            try:
                calls = json.loads(calls)
            except json.JSONDecodeError:
                calls = []
        if not isinstance(calls, list):
            continue
        for call in calls:
            if not isinstance(call, dict):
                continue
            name = call.get("tool_name") or call.get("toolName") or ""
            if name:
                names.append(str(name))
    return names


def _trace_digest(trace: Any) -> list[str]:
    """Compact per-turn summary for assertion messages.

    The raw trace is ~250 KB (``projectedContext`` + ``llmRawResponse`` +
    ``llmCalls`` per turn); interpolating it into a failure message makes the
    failure unreadable. The digest keeps what a reviewer needs.
    """
    digest: list[str] = []
    for turn in _turn_records(trace):
        calls = turn.get("toolCalls", turn.get("tool_calls")) or []
        if isinstance(calls, str):
            try:
                calls = json.loads(calls)
            except json.JSONDecodeError:
                calls = []
        tools = [
            c.get("tool_name") or c.get("toolName")
            for c in calls
            if isinstance(c, dict)
        ]
        digest.append(
            "turn {idx}: {before}->{after} uc={uc} tools={tools}".format(
                idx=turn.get("turnIndex", turn.get("turn_index")),
                before=turn.get("phaseBefore", turn.get("phase_before")),
                after=turn.get("phaseAfter", turn.get("phase_after")),
                uc=turn.get("activeUseCase", turn.get("active_use_case")),
                tools=[t for t in tools if t],
            )
        )
    return digest


def _state_value(session: dict[str, Any], snake: str, camel: str) -> str:
    value = session.get(snake, session.get(camel, ""))
    return "" if value is None else str(value)


def test_01_health_and_invalid_request_contracts(api: httpx.Client) -> None:
    """Scenario 6: missing topic and absent session are 4xx."""
    health = api.get("/actuator/health")
    health_body = _capture("01-health.json", health)
    assert health.status_code == 200 and health_body.get("status") == "UP", health_body

    missing_topic = api.post("/v1/chat/sessions", json={"first_name": "Riley"})
    missing_body = _capture("01-missing-topic.json", missing_topic)
    assert missing_topic.status_code == 400, (
        "Controller contract: topic_subject is required and must be a client 4xx; "
        f"got {missing_topic.status_code}: {missing_body}"
    )

    unknown = api.post("/v1/chat/sessions/not-a-real-session/messages", json={"message": "hello"})
    unknown_body = _capture("01-unknown-session.json", unknown)
    assert unknown.status_code == 404, (
        "Controller contract: a nonexistent session id must be 404; "
        f"got {unknown.status_code}: {unknown_body}"
    )


@pytest.mark.xfail(
    strict=False,
    reason=(
        "KNOWN PENDING (2026-07-25): malformed JSON currently returns 500 "
        '("JSON parse error: Unexpected end-of-input"). A 500->400 handler is '
        "being added on the server side by a parallel change and needs a "
        "backend restart to take effect. The assertion below is the target "
        "state and is deliberately NOT weakened; it will report XPASS once the "
        "restarted backend serves the fix."
    ),
)
def test_01b_malformed_json_body_is_client_error(api: httpx.Client) -> None:
    """Scenario 6 (cont.): a truncated JSON body is client input, not a 5xx.

    Split out of ``test_01`` so the pending server-side fix shows up as XFAIL /
    XPASS instead of turning the whole invalid-request contract red.
    """
    malformed = api.post(
        "/v1/chat/sessions",
        content=b'{"topic_subject":',
        headers={"Content-Type": "application/json"},
    )
    malformed_body = _capture("01-malformed-json.json", malformed)
    assert 400 <= malformed.status_code < 500, (
        "Product criterion / HTTP contract: malformed JSON is invalid client input and must return 4xx, "
        f"not a user-visible 5xx; got {malformed.status_code}: {malformed_body}"
    )


def test_02_faq_is_grounded_and_self_served(api: httpx.Client) -> None:
    """Scenario 1: FAQ asks must be answered with a customer-visible source, not handed over."""
    created = _create(
        api,
        "02-faq",
        description="I need to know how to relist my advert after it expired.",
    )
    session_id = created["session_id"]
    reply = _message(api, session_id, "02-faq", "How do I relist my advert after it expired?")
    session = _session(api, session_id, "02-faq")
    trace = _trace(api, session_id, "02-faq")

    assert not reply.get("should_end_chat"), (
        "Product criterion: pure FAQ explanation must be self-served, not escalated. "
        f"reply={reply} session={session}"
    )
    assert _state_value(session, "handling_state", "handlingState") not in {"QUEUE_TO_HUMAN", "HUMAN_HANDLING"}, (
        "Product criterion: pure FAQ explanation must not be handed over."
    )
    customer_source = bool(re.search(r"https?://", str(reply.get("reply_text", "")))) or bool(
        (reply.get("additional_data") or {}).get("articles")
    )
    assert customer_source, (
        "Product criterion: a factual FAQ answer needs a customer-visible source citation/link. "
        f"reply={reply}; trace={trace}"
    )
    assert not INTERNAL_SF_ARTICLE_ID.search(str(reply.get("reply_text", ""))), (
        "P0 criterion: a Salesforce internal Knowledge id (ka...) must never be exposed to the customer."
    )


def test_03_listing_status_uses_prefilled_ad_context(api: httpx.Client) -> None:
    """Scenario 2: AD-2002 form context powers a concrete, no-repeat read-only answer.

    ASSERTION TIMING (corrected 2026-07-25). The previous version asserted the
    form context was used in the *create-session* reply. That is unreachable by
    design: ``SessionManager.java:121-131`` routes deterministically and returns
    a static greeting without an LLM call (Sprint 8.1 §M0), and the comment at
    ``:144-150`` states the create-time auto-search branch was deliberately
    removed because it blew the user-facing wait budget. The form context is
    ingested at step 1 and consumed on the next message, so every
    "did it use the form?" assertion now runs against turn 2.

    The greeting has its own weaker, reachable obligation; see
    ``test_03b_static_greeting_does_not_re_ask_form_supplied_information``.
    """
    created = _create(
        api,
        "03-listing",
        description="My ad isn't performing well. Not many views.",
    )
    session_id = created["session_id"]
    reply = _message(
        api,
        session_id,
        "03-listing",
        "Please check why my ad has few views. The ad ID and description are already in my form.",
    )
    session = _session(api, session_id, "03-listing")
    trace = _trace(api, session_id, "03-listing")
    answer = str(reply.get("reply_text", ""))
    answer_lower = answer.lower()
    listing_context = str(session.get("listing_context", session.get("listingContext", ""))).lower()

    assert "ad-2002" in listing_context, (
        "Product criterion: a read-only listing-status request with form ad_id must query/retain that listing context. "
        f"session={session}; trace={_trace_digest(trace)}"
    )
    assert any(fact in answer_lower for fact in ("ad-2002", "handmade wooden desk", "live", "120", "edinburgh", "12")), (
        "Product criterion: read-only status response must cite concrete information about AD-2002 "
        "(fixture: Handmade Wooden Desk, LIVE, 120 views, 12 replies, Edinburgh), not give generic advice. "
        f"answer={answer!r}; trace={_trace_digest(trace)}"
    )
    assert not AD_ID_REQUEST.search(answer), (
        "Product criterion: the bot must not request ad_id already supplied in the pre-chat form. "
        f"answer={answer!r}"
    )
    assert not reply.get("should_end_chat"), (
        "Product criterion: a read-only listing status query is self-serve and must not escalate."
    )


@pytest.mark.xfail(
    strict=False,
    reason=(
        "KNOWN PENDING (2026-07-25): the static greeting currently ends with "
        '"Could you tell me a bit more about what you need help with?" even '
        "though the pre-chat form already supplied topic_subject, ad_id and "
        "description. ``buildGreeting`` is being corrected by a parallel "
        "server-side change and needs a backend restart to take effect. This "
        "is the weaker but REACHABLE obligation on the create-session path "
        "(which makes no LLM call), and is deliberately NOT weakened further; "
        "it will report XPASS once the restarted backend serves the fix."
    ),
)
def test_03b_static_greeting_does_not_re_ask_form_supplied_information(api: httpx.Client) -> None:
    """Scenario 2 (cont.): the create-session greeting must not re-ask the form.

    The greeting is static and LLM-free, so it cannot be expected to cite
    AD-2002's status. It CAN be expected not to ask the customer to retype what
    they already submitted -- that is the genuine defect the original
    first-turn assertion was reaching for.
    """
    created = _create(
        api,
        "03b-greeting",
        description="My ad isn't performing well. Not many views.",
    )
    initial = str(created.get("reply_text", ""))

    assert not RESTATE_PROBLEM_REQUEST.search(initial), (
        "Product criterion: the pre-chat form supplied topic_subject='Ad Support', "
        "ad_id='AD-2002' and a description; the static greeting must acknowledge that "
        "context instead of asking the customer to restate the problem. "
        f"greeting={initial!r}"
    )
    assert not AD_ID_REQUEST.search(initial), (
        "Product criterion: the greeting must not request an ad_id the form already carries. "
        f"greeting={initial!r}"
    )


# Ordered replies the test gives while driving scenario 3 to a terminal state.
# Each answers the question the bot is expected to ask at that point: first the
# F'1 branch split, then the appeal's reason. The list is a bound, not a
# script -- the loop stops the moment the session ends.
MUTATION_FOLLOW_UPS = [
    "Please restore my advert. I need you to change it back.",
    "I want to appeal it - I think it was removed by mistake.",
    "The ad is AD-2002, a Handmade Wooden Desk. It did not break any rules.",
]


def test_04_mutation_request_escalates_with_structured_handover(api: httpx.Client) -> None:
    """Scenario 3: an ad-restoration APPEAL is a data mutation and routes to a human.

    CONVERSATION COMPLETION (corrected 2026-07-25). The previous version sent
    one "Please restore my advert" message and immediately demanded a handover.
    The real trace (session ``edaeab4e-b162-47dd-a87e-cbec2ffa3052``) shows the
    bot asked "are you looking to understand why it was removed, or do you want
    to appeal the removal to get it reinstated?" and stopped at DISCOVER, 1
    turn, 0 tool calls. That is CORRECT: ``PRD_biz_part.md:205`` (F'1) routes
    "why was my ad removed" + compliant sub-class to a self-serve explanation
    plus repost guidance, and only "I want it reviewed / it was removed by
    mistake" to the fixed script + G4 human. "Restore my advert" straddles that
    split. Requiring a handover *before* the split is resolved is precisely the
    premature escalation the product owner is complaining about, so the test now
    answers the question and asserts on the terminal state instead.

    The over-escalation guard is kept as an upper bound on *clarifications*
    (``clarification_count``), which the runtime tracks separately from
    structured intake questions -- a UC-H intake prompt does not increment it.
    """
    created = _create(
        api,
        "04-mutation",
        description="Please restore my removed advert.",
    )
    session_id = created["session_id"]

    replies: list[str] = []
    ended = False
    turns_used = 0
    for index, text in enumerate(MUTATION_FOLLOW_UPS, start=1):
        turns_used = index
        reply = _message(api, session_id, f"04-mutation-turn-{index}", text)
        replies.append(str(reply.get("reply_text", "")).strip())
        if reply.get("should_end_chat"):
            ended = True
            break

    session = _session(api, session_id, "04-mutation")
    clarifications = _state_value(session, "clarification_count", "clarificationCount")

    # Upper bound on clarification: disambiguating the F'1 branch once is
    # correct; looping on clarifying questions is not.
    assert clarifications.isdigit() and int(clarifications) <= 1, (
        "Product criterion: the bot may ask AT MOST ONE clarifying question to resolve the "
        "PRD_biz_part.md:205 (F'1) explanation-vs-appeal split; it must not keep clarifying. "
        f"clarification_count={clarifications!r}; replies={replies}"
    )
    assert len(set(replies)) == len(replies), (
        "Product criterion: the bot must not repeat the same question while collecting the appeal. "
        f"replies={replies}"
    )

    assert ended, (
        "Product criterion: once the customer confirms they are APPEALING the removal "
        "(a data mutation the bot has no tool for), the session must escalate to a human "
        f"within {len(MUTATION_FOLLOW_UPS)} turns. used={turns_used}; replies={replies}; session={session}"
    )
    assert _state_value(session, "handling_state", "handlingState") in {"QUEUE_TO_HUMAN", "HUMAN_HANDLING"}, session

    payload = _handover_payload_for(api, session_id, "04-mutation")
    assert str(payload.get("summary", "")).strip(), f"handover summary must be non-empty: {payload}"
    assert str(payload.get("escalation_reason", "")).strip(), f"handover escalation_reason must be non-empty: {payload}"


def test_05_explicit_human_request_escalates_immediately(api: httpx.Client) -> None:
    """Scenario 4: a direct human request takes priority and terminates bot handling."""
    created = _create(api, "05-human", description="I have a question about my advert.")
    session_id = created["session_id"]
    reply = _message(api, session_id, "05-human", "Let me talk to a human, please.")
    session = _session(api, session_id, "05-human")

    assert reply.get("should_end_chat"), (
        "Product criterion: when the user explicitly asks for a human, escalate immediately."
    )
    assert _state_value(session, "handling_state", "handlingState") in {"QUEUE_TO_HUMAN", "HUMAN_HANDLING"}, session
    assert _state_value(session, "escalation_reason", "escalationReason") == "user_requested", session


def test_06_three_turn_dialogue_progresses_without_repeating_question(api: httpx.Client) -> None:
    """Scenario 5: three messages retain a UC/phase and do not re-ask the same question."""
    created = _create(
        api,
        "06-multiturn",
        description="My AD-2002 advert is live but has low visibility. Please explain the status.",
    )
    session_id = created["session_id"]
    turns = [
        "Can you check the status and visibility of AD-2002?",
        "What does that status mean for my advert?",
        "What can I do next to improve its visibility?",
    ]
    replies: list[str] = []
    observed_states: list[tuple[str, str]] = []
    for index, message in enumerate(turns, start=1):
        response = _message(api, session_id, f"06-multiturn-turn-{index}", message)
        replies.append(str(response["reply_text"]).strip().lower())
        state = _session(api, session_id, f"06-multiturn-turn-{index}")
        observed_states.append(
            (
                _state_value(state, "current_phase", "currentPhase"),
                _state_value(state, "active_use_case", "activeUseCase"),
            )
        )

    trace = _trace(api, session_id, "06-multiturn")
    digest = _trace_digest(trace)
    final_phase, final_uc = observed_states[-1]
    assert final_phase and final_phase != "INIT", (
        "Product criterion: multi-turn conversation must advance from INIT into an actionable phase. "
        f"states={observed_states}; trace={digest}"
    )
    assert final_uc, (
        "Product criterion: after three coherent listing-status turns the agent must retain/identify an active use case. "
        f"states={observed_states}; trace={digest}"
    )
    # ASSERTION ORDER MATTERS. The no-handover assertions run BEFORE the
    # repeated-reply assertion. Once a session escalates, every later turn
    # returns the same canned "this conversation has been transferred to a
    # human agent" string, so the repeat check fires too and would misreport a
    # premature escalation as a "repeated question" defect. Observed on
    # 2026-07-25: turn 1 spun search_knowledge x6 on one query, hit the 6-step
    # budget, and the backstop stamped escalation_reason=turn_budget_exhausted.
    #
    # NO-HANDOVER (corrected 2026-07-25). The previous version asserted
    # `"request_handover" not in json.dumps(trace)`. That is a guaranteed false
    # positive: `projectedContext` carries the tool inventory offered to the
    # LLM, so `request_handover` appears 8 times per turn in the trace text of
    # a session that never invoked it (measured on
    # `27cc90c5-6322-4fb9-9e0b-c21f8c3a7a65`, whose real sequence was
    # classify_use_case -> search_knowledge -> resolve_article on turn 1 and
    # search_knowledge -> resolve_article on turns 2-3, ending
    # containmentOutcome=resolved / phase=CONFIRM / escalationReason=None).
    # Assert on what actually ran and on the terminal state.
    invoked = _invoked_tools(trace)
    assert invoked, (
        "Instrumentation guard: the trace must expose parsed tool calls, otherwise the "
        f"no-handover assertion below is vacuous. trace={digest}"
    )
    assert "request_handover" not in invoked, (
        "Product criterion: a normal read-only status dialogue should continue self-service "
        f"rather than hand over. invoked_tools={invoked}; trace={digest}"
    )

    final_session = _session(api, session_id, "06-multiturn-final")
    assert _state_value(final_session, "handling_state", "handlingState") == "BOT_HANDLING", (
        "Product criterion: a self-served read-only dialogue must stay in bot handling. "
        f"session={final_session}"
    )
    assert not _state_value(final_session, "escalation_reason", "escalationReason"), (
        "Product criterion: no escalation reason may be stamped on a self-served dialogue. "
        f"session={final_session}"
    )
    assert _state_value(final_session, "containment_outcome", "containmentOutcome") != "escalated", (
        "Product criterion: the session must not be recorded as escalated. "
        f"session={final_session}"
    )

    assert len(set(replies)) == len(replies), (
        "Product criterion: the bot must not repeat the same question/response across a progressing dialogue. "
        f"replies={replies}"
    )
    assert sum(bool(AD_ID_REQUEST.search(reply)) for reply in replies) == 0, (
        "Product criterion: form-provided ad_id must not be re-requested on later turns. "
        f"replies={replies}"
    )
