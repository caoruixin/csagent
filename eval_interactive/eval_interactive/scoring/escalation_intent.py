"""Shared escalation-intent + resolution-effort classifiers (WS-1).

This module exists so the L1 hard checks and the L2 outcome checks answer
two questions the same way:

1. **Did the customer explicitly demand a human?**
   (``user_demanded_human`` / ``EXPLICIT_HUMAN_REQUEST_PATTERNS``)
2. **Did the bot actually try to resolve before handing over?**
   (``has_resolution_attempt`` / ``has_resolution_attempt_before``)

Before WS-1 these two questions were answered inconsistently across the
scoring layer, which is what let a turn-0 handover with a 10-character
summary clear the mandatory L2 gate while a genuine self-resolution had to
produce cited sources (see
``docs/proposals/performance_priority_replan_2026-07.md`` §1.2 B2/B3/B4/B6).

Anti-hardcode posture (Constitution §1.5 / §1.7)
------------------------------------------------
The pattern sets below **narrow** the deterministic surface rather than
widen it. The pre-WS-1 ``HardChecker.ESCALATION_REQUEST_PATTERNS`` bundled
two semantically different utterances into one global critical gate:

* **Explicit human request** — the customer names a *human party* they want
  to be connected to ("speak to an agent", "I want a real person"). Whether
  this is a handover request is not a judgement call; it is what the words
  say. This stays a deterministic gate.
* **Contact-channel request** — the customer asks for a *channel*, usually a
  phone call ("call me", "callback"). Whether that means "escalate me now"
  or "answer my question, and a call is fine too" is a **soft semantic
  judgement owned by the LLM** (§1.3). ``cs012``'s seed message #2 — "I just
  want to know what happened. Can someone explain **or** call me about
  this?" — is the canonical boundary case: the pre-WS-1 gate read it as a
  handover demand within one turn, while that case's own
  ``closure_criterion`` requires a policy explanation *first*. Correct
  behaviour failed L1; incorrect behaviour passed it.

So the channel class is **downgraded to an observation signal** instead of
being disambiguated with more keywords. No new keyword was added to either
list — the pre-WS-1 patterns were partitioned, not extended.
"""

from __future__ import annotations

import re
from typing import Iterable

# Knowledge-retrieval tools. Mirrors ``HardChecker.KNOWLEDGE_TOOLS`` and the
# ``search_knowledge`` / ``resolve_article`` entries in
# ``case_spec.policy_table._TOOL_ALLOWED_UCS``.
KNOWLEDGE_TOOLS: frozenset[str] = frozenset({"search_knowledge", "resolve_article"})


# ---------------------------------------------------------------------------
# Escalation-intent patterns
# ---------------------------------------------------------------------------

# Class 1 — the customer names a HUMAN PARTY they want to reach. Verbatim
# subset of the pre-WS-1 ``ESCALATION_REQUEST_PATTERNS``; nothing added.
EXPLICIT_HUMAN_REQUEST_PATTERNS: tuple[str, ...] = (
    r"\btalk\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\bspeak\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\bconnect\s+me\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\btransfer\s+(me\s+)?to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\bi\s+want\s+(a\s+)?(human|real\s+person|agent)\b",
    r"\bget\s+me\s+(a\s+)?(human|agent|person|representative)\b",
)

# Class 2 — the customer asks for a CONTACT CHANNEL (typically a phone
# call). Verbatim subset of the pre-WS-1 ``ESCALATION_REQUEST_PATTERNS``;
# nothing added. Observation-only: see the module docstring.
CONTACT_CHANNEL_REQUEST_PATTERNS: tuple[str, ...] = (
    r"\b(please\s+)?call\s+me\b",
    r"\bgive\s+me\s+a\s+call\b",
    r"\b(ring|phone)\s+me\b",
    r"\bcall(\s+me)?\s+(back|now)\b",
    r"\bcallback\b",
    r"\bcan\s+(someone|anyone)\s+(call|phone|ring)\s+me\b",
)

# Backwards-compatible union, preserved so any external reader that imported
# the bundled list keeps working. The GATE no longer uses the union.
ESCALATION_REQUEST_PATTERNS: tuple[str, ...] = (
    EXPLICIT_HUMAN_REQUEST_PATTERNS + CONTACT_CHANNEL_REQUEST_PATTERNS
)


def _matches_any(text: str, patterns: Iterable[str]) -> str | None:
    """Return the matched substring, or ``None``."""
    if not text:
        return None
    for pat in patterns:
        m = re.search(pat, text, re.IGNORECASE)
        if m:
            return m.group(0)
    return None


def is_explicit_human_request(text: str) -> str | None:
    """Return the matched phrase iff ``text`` explicitly demands a human."""
    return _matches_any(text, EXPLICIT_HUMAN_REQUEST_PATTERNS)


def is_contact_channel_request(text: str) -> str | None:
    """Return the matched phrase iff ``text`` asks for a contact channel."""
    return _matches_any(text, CONTACT_CHANNEL_REQUEST_PATTERNS)


def user_demanded_human(trace, up_to_position: int | None = None) -> bool:
    """True iff any customer turn explicitly asked for a human.

    ``up_to_position`` bounds the scan to turns at list positions
    ``[0, up_to_position]`` (inclusive) so callers can ask "had the customer
    demanded a human *by the time* the bot handed over?". ``None`` scans the
    whole session.
    """
    turns = list(trace.turns or [])
    if up_to_position is not None:
        turns = turns[: up_to_position + 1]
    return any(is_explicit_human_request(t.user_message or "") for t in turns)


# ---------------------------------------------------------------------------
# Resolution-effort evidence
# ---------------------------------------------------------------------------


def turn_retrieved_knowledge(turn) -> bool:
    """True iff this turn invoked ``search_knowledge`` / ``resolve_article``."""
    return any(
        (tc.get("tool_name", "") if isinstance(tc, dict) else "").lower()
        in KNOWLEDGE_TOOLS
        for tc in (turn.tool_calls or [])
    )


def turn_is_resolution_attempt(turn) -> bool:
    """True iff this turn is a retrieval turn or a citation turn.

    Two independent forms of evidence that the bot tried to answer rather
    than deflect:

    * a knowledge-tool call on this turn (retrieval), or
    * a non-empty ``source_ids`` on this turn (citation) — grounding may be
      stamped on a turn whose tool call the trace recorded elsewhere.
    """
    return turn_retrieved_knowledge(turn) or bool(turn.source_ids)


def has_resolution_attempt(trace) -> bool:
    """True iff the session contains any retrieval or citation turn."""
    return any(turn_is_resolution_attempt(t) for t in (trace.turns or []))


def has_resolution_attempt_before(trace, position: int | None) -> bool:
    """True iff a retrieval / citation turn occurs at or before ``position``.

    ``position`` is a **list position** in ``trace.turns`` (not a
    ``turn_index``). ``None`` means the escalation point is unknown, in which
    case the whole session is scanned — deliberately lenient, because
    penalising a case whose escalation turn the trace failed to record would
    be a measurement artifact rather than a bot failure (anti-误杀).

    The attempt turn itself may be the escalation turn: a bot that searched
    and *then* handed over on the same turn did try first.
    """
    if position is None:
        return has_resolution_attempt(trace)
    turns = list(trace.turns or [])[: position + 1]
    return any(turn_is_resolution_attempt(t) for t in turns)


def first_escalation_position(trace) -> int | None:
    """List position of the first turn that handed over, or ``None``.

    A turn counts as the escalation point when it invokes
    ``request_handover`` or transitions into the ``ESCALATE`` phase.
    """
    for pos, turn in enumerate(trace.turns or []):
        handed_over = any(
            (tc.get("tool_name", "") if isinstance(tc, dict) else "").lower()
            == "request_handover"
            for tc in (turn.tool_calls or [])
        )
        if handed_over or (turn.phase_after or "").upper() == "ESCALATE":
            return pos
    return None


def session_escalated(trace) -> bool:
    """True iff the session handed over, by trace evidence or by outcome stamp."""
    if (trace.session_state.containment_outcome or "").strip().lower() == "escalated":
        return True
    return first_escalation_position(trace) is not None
