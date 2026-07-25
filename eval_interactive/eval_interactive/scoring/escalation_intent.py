"""Shared escalation-intent + resolution-effort classifiers (WS-1).

This module exists so the L1 hard checks and the L2 outcome checks answer
two questions the same way:

1. **Did the customer ask to be handed to a human?**
   (``user_demanded_human`` / ``HUMAN_HANDOVER_REQUEST_PATTERNS``)
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
semantically different utterances into one global critical gate. They are
partitioned into three classes, and **each class carries a different role
depending on whether it is being used as a demand or as a waiver** (see
"Waiver vs demand" below):

* **Class 1a — explicit human party** (:data:`EXPLICIT_HUMAN_PARTY_PATTERNS`).
  The customer names a *human party* they want to be connected to ("speak to
  an agent", "I want a real person"). Whether this is a handover request is
  not a judgement call; it is what the words say. Deterministic gate.
* **Class 1b — connection request, object unconstrained**
  (:data:`CONNECTION_REQUEST_PATTERNS`). "connect me", "transfer me", "put
  me through" — a verb of *interpersonal connection applied to the speaker*.
  Waiver-only + observation; see below.
* **Class 2 — contact channel** (:data:`CONTACT_CHANNEL_REQUEST_PATTERNS`).
  The customer asks for a *channel*, usually a phone call ("call me",
  "callback"). Whether that means "escalate me now" or "answer my question,
  and a call is fine too" is a **soft semantic judgement owned by the LLM**
  (§1.3). ``cs012``'s seed message #2 — "I just want to know what happened.
  Can someone explain **or** call me about this?" — is the canonical
  boundary case: the pre-WS-1 gate read it as a handover demand within one
  turn, while that case's own ``closure_criterion`` requires a policy
  explanation *first*. Correct behaviour failed L1; incorrect behaviour
  passed it. So the channel class is **downgraded to an observation
  signal** instead of being disambiguated with more keywords.

Why class 1b exists — the ``cs_interactive_208`` regression
----------------------------------------------------------
Class 1a constrains the *object* of the connection verb to a closed list of
human common nouns (``human|agent|person|representative|someone``). Real
customers frequently name **a specific person** instead:
``cs_interactive_208``'s form description is verbatim "Can u plz kindly
connect me with Monisha". The bot handed over — correct behaviour — and the
class-1a waiver did not fire, so the WS-1 L1 gate ``no_premature_escalation``
failed it in all three experiment arms. That is the **same failure shape as
cs012**, which is the shape WS-1 exists to remove.

The fix does **not** add a name list — personal names are an open set and
enumerating them is precisely the keyword-piling §1.5 / §1.7 forbid.
Instead the *object constraint is removed*: "connect me" / "transfer me" /
"put me through" are read as handover requests **whatever follows**, because
the verb+``me`` construction already carries the request. This is a
structural narrowing of the requirement (one constraint deleted), not a
widened vocabulary.

Two evidence-backed refinements to that removal:

* ``me`` is **mandatory** on the class-1b ``transfer`` pattern. Class 1a
  makes it optional (``transfer (me )?to a human``), which is safe only
  because the object is constrained. Unconstrained, ``transfer to X``
  collides with money transfer — the corpus contains verbatim "transfer to
  Monzo account 83367413, sort code 04-00-03" in a scam-report case, and
  "transfer to a new email address" is an ordinary account-support ask.
  Class 1a is left byte-identical, so no class-1a coverage is lost.
* Class 1b is a **waiver + observation, never a critical demand** — see the
  next section. This is what keeps the residual false-positive shape the
  removal admits ("connect me to the FAQ", where the object is a self-serve
  artifact rather than a party) from becoming a new hard failure. Deciding
  whether the object is a party or a document is animacy semantics, i.e.
  LLM-owned (§1.3), so it is not adjudicated deterministically here.

Waiver vs demand — why the two roles do not share a threshold
-------------------------------------------------------------
The same pattern set is consumed in two opposite directions, and an
over-match costs a different thing in each:

* As a **waiver** (``user_demanded_human`` → ``no_premature_escalation``,
  ``correct_outcome``, ``escalation_timing``) it *releases* the bot from
  "you must retrieve before handing over". An over-match loses one
  premature-escalation detection — recoverable via L2 / L3 / §5.6 human
  review.
* As a **demand** (``is_explicit_human_request`` →
  ``user_requested_escalation``) it *requires* a handover within one turn.
  An over-match hard-fails a bot that answered correctly and makes the case
  unwinnable — unrecoverable, and the exact defect cs012 and
  cs_interactive_208 both are.

Because the costs are asymmetric the thresholds are asymmetric: the waiver
takes the broad reading (1a ∪ 1b), the critical demand keeps the narrow one
(1a only). Corpus measurement over all 488 specs supports this: removing
the object constraint newly matches 5 user-side strings across 4 specs
(``cs_interactive_208``, ``cs_interactive_212``, ``csmp_g02_uc_a_user_requested_specialist``,
``cs01g01_uc_c_user_requested_handover``) and loses nothing.
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

# Class 1a — the customer names a HUMAN PARTY they want to reach. Verbatim
# subset of the pre-WS-1 ``ESCALATION_REQUEST_PATTERNS``; nothing added,
# nothing removed. This is the only class that carries a CRITICAL demand.
EXPLICIT_HUMAN_PARTY_PATTERNS: tuple[str, ...] = (
    r"\btalk\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\bspeak\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\bconnect\s+me\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\btransfer\s+(me\s+)?to\s+(a\s+)?(human|agent|person|representative|someone)\b",
    r"\bi\s+want\s+(a\s+)?(human|real\s+person|agent)\b",
    r"\bget\s+me\s+(a\s+)?(human|agent|person|representative)\b",
)

# Backwards-compatible alias. ``EXPLICIT_HUMAN_REQUEST_PATTERNS`` was the
# original WS-1 name for class 1a and is still what the critical demand gate
# consumes, so the name keeps its exact meaning ("the object of the verb is
# a human party").
EXPLICIT_HUMAN_REQUEST_PATTERNS: tuple[str, ...] = EXPLICIT_HUMAN_PARTY_PATTERNS

# Class 1b — the customer asks to be CONNECTED, object unconstrained:
# "connect me with Monisha", "put me through", "transfer me to Thato",
# "thanks - just connect me". The verb+``me`` construction is the request;
# what follows it does not change the ask, so the object slot carries NO
# constraint (that is the point — see the module docstring on
# ``cs_interactive_208``). ``me`` is mandatory on ``transfer`` because
# unconstrained ``transfer to X`` collides with money transfer.
#
# Waiver + observation only; never a critical demand.
CONNECTION_REQUEST_PATTERNS: tuple[str, ...] = (
    r"\bconnect\s+me\b",
    r"\btransfer\s+me\b",
    r"\bput\s+me\s+through\b",
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

# The WAIVER set: any reading under which the customer asked to be handed to
# a human party. Class 1a ∪ class 1b. Consumed by ``user_demanded_human``.
HUMAN_HANDOVER_REQUEST_PATTERNS: tuple[str, ...] = (
    EXPLICIT_HUMAN_PARTY_PATTERNS + CONNECTION_REQUEST_PATTERNS
)

# Backwards-compatible union of the PRE-WS-1 bundled list, preserved so any
# external reader that imported it keeps working. Deliberately still the
# original 12 patterns (class 1a + class 2): it is a historical artifact, not
# a live set, and no gate consumes it.
ESCALATION_REQUEST_PATTERNS: tuple[str, ...] = (
    EXPLICIT_HUMAN_PARTY_PATTERNS + CONTACT_CHANNEL_REQUEST_PATTERNS
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


def is_explicit_human_party_request(text: str) -> str | None:
    """Class 1a — ``text`` names a human party ("speak to an agent").

    This is the only class that carries a **critical** demand
    (``user_requested_escalation``), so it stays narrow on purpose.
    """
    return _matches_any(text, EXPLICIT_HUMAN_PARTY_PATTERNS)


# Backwards-compatible alias — the critical demand gate's entry point.
is_explicit_human_request = is_explicit_human_party_request


def is_connection_request(text: str) -> str | None:
    """Class 1b — ``text`` asks to be connected, object unconstrained.

    "connect me with Monisha", "put me through", "just connect me". Waiver +
    observation only; see the module docstring on why this class is never a
    critical demand.
    """
    return _matches_any(text, CONNECTION_REQUEST_PATTERNS)


def is_human_handover_request(text: str) -> str | None:
    """The WAIVER predicate — class 1a **or** class 1b matched.

    True under any reading in which the customer asked to be put in touch
    with a human party, whether they named the party generically ("an
    agent") or specifically ("Monisha").
    """
    return _matches_any(text, HUMAN_HANDOVER_REQUEST_PATTERNS)


def is_contact_channel_request(text: str) -> str | None:
    """Return the matched phrase iff ``text`` asks for a contact channel."""
    return _matches_any(text, CONTACT_CHANNEL_REQUEST_PATTERNS)


def user_demanded_human(trace, up_to_position: int | None = None) -> bool:
    """True iff any customer turn asked to be handed to a human.

    Uses the broad WAIVER set (class 1a ∪ class 1b) — see the module
    docstring's "Waiver vs demand" section for why this is broader than the
    critical ``user_requested_escalation`` demand. A customer who named a
    specific person ("connect me with Monisha") asked for a human just as
    plainly as one who said "connect me to an agent", so a bot that handed
    over must not be charged with premature escalation.

    ``up_to_position`` bounds the scan to turns at list positions
    ``[0, up_to_position]`` (inclusive) so callers can ask "had the customer
    demanded a human *by the time* the bot handed over?". ``None`` scans the
    whole session.
    """
    turns = list(trace.turns or [])
    if up_to_position is not None:
        turns = turns[: up_to_position + 1]
    return any(is_human_handover_request(t.user_message or "") for t in turns)


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
