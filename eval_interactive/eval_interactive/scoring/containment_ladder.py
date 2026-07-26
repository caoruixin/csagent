"""D1 / D2 / D3 containment ladder (Sprint 105 item 4, WS-4).

Business success has been a three-tier ladder since the BRD
(``docs/foundational/PRD_biz_part.md`` §1.5):

===  ============================================================
D1   完全自动结案 — fully automated closure, no human involved: the
     knowledge base plus a self-serve link ends the case.
D2   首解 / 显著减负 — first resolution / material deflection: the
     user gets the correct next step (status explanation, link,
     form), even when a human is still involved downstream. This is
     the 45–55% target band, and it is a business *success*.
D3   Neither — the case was not deflected. The user left without a
     correct next step.
===  ============================================================

Eval, until now, carried only the flat ``resolved / escalated /
abandoned`` triple (``trace/models.py`` ``SessionState.containment_
outcome``) and graded pass/fail on a binary resolve-vs-escalate
comparison. The consequence is the defect this module closes: **"explained
the read-only half, then handed the data-modifying half over with full
context" and "handed over at turn 0 with no attempt at all" are the same
value — ``escalated`` — and score identically.** One is a D2 success; the
other is a D3 failure.

Design constraints (Sprint 105 contract §3.4)
---------------------------------------------

- **Derived in the eval layer only.** Every input is a trace fact the
  harness already records; ``server/**`` emits nothing new.
- **Reproducible from a recorded trace**, so the harness verdict and a
  human spot-check verdict can be compared — and can visibly disagree,
  which is the point of having a ladder at all.
- **No keyword matching on bot wording.** The tier is a function of tool
  calls, retrieval results, the harness's own citation check, the
  handover-payload completeness score and turn indices. If a future
  change needs to read what the bot *said* to pick a tier, that is the
  forbidden shape (§1.7) and belongs in an L3 rubric instead.
- **Additive.** ``containment_outcome`` keeps its three values and its
  meaning; the tier is a second, orthogonal axis. Nothing re-maps.

What the tier deliberately does NOT claim
-----------------------------------------

The ladder measures whether a *correct next step was delivered*, not
whether the advice was *good*. Two answers can both cite a real,
correctly-retrieved article and still differ in quality — the e2e
findings recorded exactly that: two materially different groundings for
the same question, one of them worse advice, both legitimately grounded.
Both land D2 here. Answer quality is the L3 judge's job, and no
trace-derived tier can substitute for it.
"""

from __future__ import annotations

from dataclasses import dataclass, field

# Tool names that constitute an attempt to resolve the user's problem.
_RETRIEVAL_TOOLS = frozenset({"search_knowledge", "resolve_article"})
_HANDOVER_TOOL = "request_handover"

TIER_D1 = "D1"
TIER_D2 = "D2"
TIER_D3 = "D3"
TIER_UNKNOWN = "UNKNOWN"

ALL_TIERS = (TIER_D1, TIER_D2, TIER_D3, TIER_UNKNOWN)


@dataclass
class LadderFacts:
    """The trace facts the ladder reads. All are already recorded."""

    containment_outcome: str = ""
    attempted_retrieval: bool = False
    grounded_artifact: bool = False
    citation_check_passed: bool | None = None
    handover_requested: bool = False
    handover_turn: int = -1
    handover_payload_complete: bool | None = None
    total_turns: int = 0
    observed_turns: int = 0
    trace_available: bool = True


@dataclass
class TierVerdict:
    """A ladder tier plus the facts that produced it."""

    tier: str
    detail: str
    facts: LadderFacts = field(default_factory=LadderFacts)


# ---------------------------------------------------------------------------
# Fact extraction
# ---------------------------------------------------------------------------


def _tool_names(turn: dict) -> list[str]:
    return [
        tc.get("tool_name", "")
        for tc in (turn.get("tool_calls") or [])
        if isinstance(tc, dict)
    ]


def _turn_has_grounded_artifact(turn: dict) -> bool:
    """Did any retrieval call in this turn return a usable article?

    ``search_knowledge`` counts when it returned at least one hit and did
    not self-report a miss; ``resolve_article`` counts when it succeeded
    and produced a URL. Both are structured tool-result facts — no bot
    text is inspected.
    """
    for tc in turn.get("tool_calls") or []:
        if not isinstance(tc, dict):
            continue
        name = tc.get("tool_name", "")
        data = tc.get("result_data") or {}
        if not isinstance(data, dict):
            continue
        if name == "search_knowledge":
            hits = data.get("hits") or []
            missed = bool(data.get("faq_miss")) or bool(data.get("retrieval_miss"))
            if hits and not missed:
                return True
        elif name == "resolve_article":
            if tc.get("success") and (
                data.get("source_url") or data.get("canonical_url")
            ):
                return True
    return False


def extract_facts_from_turn_dicts(
    turns: list[dict],
    l1_results,
    l2_results,
    containment_outcome: str,
    total_turns: int,
) -> LadderFacts:
    """Core extraction. Both the live and offline paths funnel through here.

    ``turns`` is the ``per_turn_trace``-shaped list — one dict per bot
    turn carrying ``tool_calls``. ``executor._build_per_turn_trace``
    copies ``TurnTrace.tool_calls`` verbatim, so the live and recorded
    shapes are identical and the tier is reproducible offline by
    construction.
    """
    attempted = False
    grounded = False
    handover_requested = False
    handover_turn = -1

    for idx, turn in enumerate(turns):
        names = _tool_names(turn)
        if any(n in _RETRIEVAL_TOOLS for n in names):
            attempted = True
            if _turn_has_grounded_artifact(turn):
                grounded = True
        if _HANDOVER_TOOL in names and handover_turn < 0:
            handover_requested = True
            handover_turn = idx

    citation = None
    for r in l1_results or []:
        if r.check_name == "source_citation_present":
            citation = r.passed
            break

    payload_complete = None
    for r in l2_results or []:
        if r.check_name == "handover_completeness":
            payload_complete = r.score >= 1.0 - 1e-9
            break

    return LadderFacts(
        containment_outcome=containment_outcome or "",
        attempted_retrieval=attempted,
        grounded_artifact=grounded,
        citation_check_passed=citation,
        handover_requested=handover_requested,
        handover_turn=handover_turn,
        handover_payload_complete=payload_complete,
        total_turns=total_turns,
        observed_turns=len(turns),
        # A session that produced no per-turn trace at all cannot be
        # placed on the ladder. Distinguishing this from D3 matters: an
        # infra failure is not a bot failure (§3.2 question 1).
        trace_available=bool(turns),
    )


def extract_facts(case) -> LadderFacts:
    """Offline path: build facts from a rehydrated recorded case.

    ``case`` is a
    :class:`~eval_interactive.scoring.rescore.RehydratedCase`; typed
    structurally rather than by import to keep the ladder free of a
    circular dependency on the re-scoring module.
    """
    return extract_facts_from_turn_dicts(
        case.per_turn_trace or [],
        case.l1_results,
        case.l2_results,
        case.containment_outcome,
        case.total_turns,
    )


def extract_facts_from_trace(
    trace_data, l1_results, l2_results, total_turns: int
) -> LadderFacts:
    """Live path: build facts from a freshly-collected ``TraceData``.

    Reads the same ``tool_calls`` payload the offline path reads, so a
    tier computed during a run and a tier recomputed later from that
    run's ``results.json`` agree by construction.
    """
    turns = [
        {"tool_calls": list(getattr(t, "tool_calls", None) or [])}
        for t in (getattr(trace_data, "turns", None) or [])
    ]
    state = getattr(trace_data, "session_state", None)
    return extract_facts_from_turn_dicts(
        turns,
        l1_results,
        l2_results,
        getattr(state, "containment_outcome", "") or "",
        total_turns,
    )


# ---------------------------------------------------------------------------
# Tier derivation
# ---------------------------------------------------------------------------


def derive_tier_from_facts(f: LadderFacts) -> TierVerdict:
    """Place one session on the D1 / D2 / D3 ladder. First match wins.

    The ordering encodes the business definition, not a score:

    1. No trace, or no measured terminal → ``UNKNOWN``. An unmeasured
       session must never be reported as a D3 failure; that would charge
       an infra defect to the bot.
    2. Resolved in-bot, no handover, grounded → ``D1``.
    3. A correct next step was delivered — retrieval attempted AND a
       grounded artifact surfaced AND (if handed over) the payload was
       complete → ``D2``. This is the "explained, then handed over with
       context" case the flat model could not distinguish.
    4. Everything else → ``D3``.
    """
    if not f.trace_available:
        return TierVerdict(
            TIER_UNKNOWN,
            "no per-turn trace recorded (infra failure or unmeasured run); "
            "not placed on the ladder",
            f,
        )
    if not f.containment_outcome:
        return TierVerdict(
            TIER_UNKNOWN,
            f"no containment_outcome stamped (observed_turns={f.observed_turns}); "
            "terminal not measured",
            f,
        )

    resolved = f.containment_outcome == "resolved"

    # -- D1: the bot closed it alone, on grounded evidence ---------------
    if resolved and not f.handover_requested and f.grounded_artifact:
        return TierVerdict(
            TIER_D1,
            "resolved in-bot with no handover and a grounded retrieval "
            "artifact surfaced",
            f,
        )

    # -- D2: a correct next step was delivered ---------------------------
    if f.attempted_retrieval and f.grounded_artifact:
        if f.handover_requested:
            if f.handover_payload_complete is False:
                return TierVerdict(
                    TIER_D3,
                    f"grounded next step delivered but the handover payload was "
                    f"incomplete (handover_completeness < 1.0, turn "
                    f"{f.handover_turn}); the human receives no usable context",
                    f,
                )
            return TierVerdict(
                TIER_D2,
                f"grounded next step delivered before handover at turn "
                f"{f.handover_turn} of {f.observed_turns}; handover payload "
                f"complete — material deflection with context preserved",
                f,
            )
        return TierVerdict(
            TIER_D2,
            f"grounded next step delivered (containment_outcome="
            f"{f.containment_outcome}) without a handover, but the session did "
            f"not close as resolved",
            f,
        )

    if resolved:
        # Resolved without any grounded artifact. Not a full automated
        # closure — there is no evidence the user was given a correct next
        # step — but a resolve stamp is more than a bare handover.
        return TierVerdict(
            TIER_D2,
            "resolved but with no grounded retrieval artifact; deflection "
            "claimed without retrieval evidence",
            f,
        )

    # -- D3: no correct next step reached the user -----------------------
    if f.handover_requested and not f.attempted_retrieval:
        return TierVerdict(
            TIER_D3,
            f"handover at turn {f.handover_turn} with no retrieval attempted "
            f"in any turn — no next step was delivered",
            f,
        )
    if f.attempted_retrieval and not f.grounded_artifact:
        return TierVerdict(
            TIER_D3,
            "retrieval attempted but no grounded artifact surfaced; the user "
            "left without an actionable next step",
            f,
        )
    return TierVerdict(
        TIER_D3,
        f"containment_outcome={f.containment_outcome} with no retrieval "
        f"attempt and no grounded artifact",
        f,
    )


def derive_tier(case) -> TierVerdict:
    """Convenience wrapper: extract facts from a rehydrated case, then tier."""
    return derive_tier_from_facts(extract_facts(case))
