"""Declarative three-state conditional outcome acceptance (S-Auto-40 WP1-A).

Encodes a product decision as a *declarative, condition-bound* outcome rule:
for a UC-A bad case where the bot gives closure-qualified grounded help, the
terminal is accepted as **resolve when the customer is satisfied** and is only
**eligible for escalate-acceptance when the customer remains positively
unresolved after that help** — and even then it is never an automatic PASS:
it is ``CONDITIONAL_ELIGIBLE`` / ``REVIEW_REQUIRED`` until a versioned,
auditable adjudication artifact keyed by the trace says otherwise.

Design constraints (binding):
- **Generic, not case-id-keyed.** This evaluator activates only when a
  CaseSpec carries the declarative ``conditional_outcome_acceptance`` block;
  it reads that block, never a hard-coded case id, and never customer
  free text.
- **Reads the Phase-1 signal only.** The customer's resolution stance comes
  from the simulator's own per-turn ``user_state`` series
  (``trace.user_state_signals``). It is never back-inferred from the chosen
  outcome, the handover, the escalation reason, or the *absence* of a
  satisfaction signal.
- **Closure quality is a precondition, not proof.** The structural marker
  (a bot turn with tool calls + ``source_ids`` + a grounded answer) proves
  only that grounded help was *attempted*, never that closure was *met*. On
  the marker alone the verdict is ``CONDITIONAL_ELIGIBLE``, never auto-PASS.
- **Adjudication, not runtime judgment.** A ``CONDITIONAL_ELIGIBLE`` trace
  becomes PASS only when a registry entry keyed by the trace id (and matched
  to the current closure-criterion version) records a human ``accept``
  verdict. The evaluator only *reads* that artifact; it never calls human
  judgment at runtime.
"""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

import yaml

logger = logging.getLogger(__name__)


# --- Verdicts -----------------------------------------------------------
PASS = "PASS"
FAIL = "FAIL"
CONDITIONAL_ELIGIBLE = "CONDITIONAL_ELIGIBLE"  # == REVIEW_REQUIRED until adjudicated

# --- Three-state customer stance ---------------------------------------
SATISFIED = "SATISFIED"
UNRESOLVED = "UNRESOLVED"
UNKNOWN = "UNKNOWN"

# Outcome normalisation (mirrors outcome_checks._OUTCOME_MAP).
_OUTCOME_MAP = {"resolved": "resolve", "escalated": "escalate", "abandoned": "abandoned"}

# Default committed registry of per-trace adjudications.
_DEFAULT_ADJUDICATION_PATH = (
    Path(__file__).resolve().parents[2]
    / "case_specs"
    / "conditional_outcome_adjudications.yaml"
)

_ACCEPT_VERDICT = "accept_escalation"


@dataclass(frozen=True)
class ConditionalOutcomeAcceptance:
    """Parsed declarative acceptance block from a CaseSpec.

    ``satisfied_outcome`` — the outcome a SATISFIED customer should receive
    (e.g. ``resolve``).
    ``unresolved_accept_outcome`` — the outcome that becomes
    ``CONDITIONAL_ELIGIBLE`` when the customer is positively UNRESOLVED after
    closure-qualified grounded help (e.g. ``escalate``).
    ``require_closure_precondition`` — when True, the escalate-acceptance
    branch additionally requires the grounded-help structural marker.
    """

    schema_version: int
    satisfied_outcome: str
    unresolved_accept_outcome: str
    require_closure_precondition: bool = True


@dataclass(frozen=True)
class ConditionalOutcomeResult:
    """Outcome of the conditional evaluator for one trace."""

    verdict: str          # PASS | FAIL | CONDITIONAL_ELIGIBLE
    score: float          # 1.0 only on PASS; 0.0 otherwise (never auto-pass)
    user_state: str       # SATISFIED | UNRESOLVED | UNKNOWN
    closure_present: bool
    adjudicated: bool
    detail: str


def parse_conditional_outcome_acceptance(
    raw: Optional[dict],
) -> Optional[ConditionalOutcomeAcceptance]:
    """Parse the declarative block (or return None when absent)."""
    if not raw:
        return None
    return ConditionalOutcomeAcceptance(
        schema_version=int(raw.get("schema_version", 1)),
        satisfied_outcome=str(raw["satisfied_outcome"]).lower(),
        unresolved_accept_outcome=str(raw["unresolved_accept_outcome"]).lower(),
        require_closure_precondition=bool(raw.get("require_closure_precondition", True)),
    )


def closure_qualified_help_turn(trace) -> Optional[int]:
    """Earliest bot turn that is a closure-qualified grounded-help marker.

    Structural marker = a turn with tool calls AND non-empty ``source_ids``
    AND a non-empty grounded answer (``bot_response``). This proves only the
    *precondition* that grounded help was attempted — never that the user's
    closure criterion was met.
    """
    best: Optional[int] = None
    for t in getattr(trace, "turns", []) or []:
        tool_calls = getattr(t, "tool_calls", None) or []
        source_ids = getattr(t, "source_ids", None) or []
        answer = (getattr(t, "bot_response", "") or "").strip()
        if tool_calls and source_ids and answer:
            ti = getattr(t, "turn_index", None)
            if ti is None:
                continue
            if best is None or ti < best:
                best = ti
    return best


def reduce_user_state(signals, closure_turn: Optional[int]) -> str:
    """Reduce the Phase-1 per-turn signal series to one terminal stance.

    Uses the LATEST positively-asserted resolution signal as the customer's
    terminal stance:

    - ``user_state == "satisfied"`` or ``goal_status == "achieved"`` →
      a positive SATISFIED assertion.
    - ``user_state == "unresolved_after_help"`` → a positive UNRESOLVED
      assertion, but ONLY when it occurred at/after the closure marker
      (post-help alignment). A pre-help unresolved assertion is not a
      qualifying stance (it is not yet "after the bot helped").
    - everything else (``working`` / ``new_request`` / missing /
      ``goal_status == "impossible"``) is neutral and never maps to
      UNRESOLVED.

    No signal is carried forward: each is judged on its own ``turn_id``.
    Absent any qualifying positive assertion → UNKNOWN.
    """
    terminal: Optional[str] = None
    for s in signals or []:
        us = s.get("user_state")
        gs = s.get("goal_status")
        ts = s.get("turn_id")
        if us == "satisfied" or gs == "achieved":
            terminal = SATISFIED
        elif us == "unresolved_after_help":
            if (
                closure_turn is not None
                and ts is not None
                and ts >= closure_turn
            ):
                terminal = UNRESOLVED
            # pre-help unresolved → not a qualifying stance; leave terminal as-is
    return terminal or UNKNOWN


def closure_criterion_version(case_spec) -> str:
    """Short content hash of the CaseSpec closure criterion.

    Recorded in each adjudication so a closure-criterion change invalidates a
    stale adjudication (the trace falls back to REVIEW_REQUIRED).
    """
    text = (getattr(case_spec, "closure_criterion", None) or "").strip()
    return hashlib.sha256(text.encode("utf-8")).hexdigest()[:12]


def load_adjudications(path: Optional[Path] = None) -> dict:
    """Load the per-trace adjudication registry, keyed by ``trace_id``.

    Missing / empty registry → empty dict (no trace is adjudicated, so every
    eligible escalation stays REVIEW_REQUIRED). The registry is the only
    artifact that can flip ``CONDITIONAL_ELIGIBLE`` to PASS; it is committed
    and human-authored.
    """
    p = path or _DEFAULT_ADJUDICATION_PATH
    try:
        with open(p, "r", encoding="utf-8") as f:
            raw = yaml.safe_load(f) or {}
    except FileNotFoundError:
        return {}
    out: dict = {}
    for entry in raw.get("adjudications", []) or []:
        tid = entry.get("trace_id")
        if tid:
            out[str(tid)] = entry
    return out


def _is_adjudicated(case_spec, trace, adjudications: dict) -> bool:
    """True iff a committed registry entry accepts THIS trace's escalation.

    Requires: trace id match, ``verdict == accept_escalation``, case id
    match, and the recorded ``closure_criterion_version`` matching the
    current CaseSpec closure criterion (stale adjudications do not apply).
    """
    trace_id = getattr(getattr(trace, "session_state", None), "session_id", "") or ""
    entry = adjudications.get(str(trace_id))
    if not entry:
        return False
    if entry.get("verdict") != _ACCEPT_VERDICT:
        return False
    if entry.get("case_id") not in (None, getattr(case_spec, "case_id", None)):
        return False
    recorded_version = entry.get("closure_criterion_version")
    if recorded_version is not None and recorded_version != closure_criterion_version(
        case_spec
    ):
        logger.warning(
            "conditional_outcome: adjudication for trace_id=%s is stale "
            "(closure_criterion_version mismatch); staying REVIEW_REQUIRED",
            trace_id,
        )
        return False
    return True


def decide(
    acceptance: ConditionalOutcomeAcceptance,
    actual_outcome: str,
    user_state: str,
    closure_present: bool,
    adjudicated: bool,
) -> tuple[str, float, str]:
    """Pure three-state decision. Returns (verdict, score, detail).

    Score is 1.0 ONLY on a final PASS; every non-PASS verdict scores 0.0 so
    the mandatory-L2 gate (threshold 1.0) never auto-passes a
    ``CONDITIONAL_ELIGIBLE`` / ``FAIL`` trace.
    """
    actual = (actual_outcome or "").lower()
    sat = acceptance.satisfied_outcome
    accept = acceptance.unresolved_accept_outcome

    if actual == sat:
        if user_state == UNRESOLVED:
            return FAIL, 0.0, "false resolve: customer positively UNRESOLVED after help"
        return PASS, 1.0, f"satisfied-path outcome={actual}, user_state={user_state}"

    if actual == accept:
        if user_state == SATISFIED:
            return FAIL, 0.0, "escalation after the customer was SATISFIED"
        if user_state == UNRESOLVED:
            if acceptance.require_closure_precondition and not closure_present:
                return FAIL, 0.0, (
                    "escalation after UNRESOLVED but NO closure-qualified "
                    "grounded-help marker (incomplete / non-grounded)"
                )
            if adjudicated:
                return PASS, 1.0, (
                    "escalate accepted: UNRESOLVED after closure-qualified help, "
                    "adjudicated"
                )
            return CONDITIONAL_ELIGIBLE, 0.0, (
                "UNRESOLVED after closure-qualified help → CONDITIONAL_ELIGIBLE / "
                "REVIEW_REQUIRED (no adjudication artifact for this trace)"
            )
        # UNKNOWN / neutral → not accepted (early / lazy escalation, neutral
        # reply, new-goal, or handover-caused missing signal).
        return FAIL, 0.0, (
            f"escalation not accepted: user_state={user_state} "
            "(no positive post-help UNRESOLVED signal)"
        )

    # Any other actual outcome (abandoned / unexpected) is not accepted.
    return FAIL, 0.0, f"outcome={actual} not covered by conditional acceptance"


def evaluate_conditional_outcome(
    case_spec, trace, adjudications: Optional[dict] = None
) -> ConditionalOutcomeResult:
    """Evaluate the declarative conditional acceptance for one trace."""
    acceptance = parse_conditional_outcome_acceptance(
        getattr(case_spec, "conditional_outcome_acceptance", None)
    )
    if acceptance is None:
        raise ValueError(
            "evaluate_conditional_outcome called on a CaseSpec without a "
            "conditional_outcome_acceptance block"
        )

    raw_outcome = (
        getattr(getattr(trace, "session_state", None), "containment_outcome", "")
        or ""
    ).lower()
    actual_outcome = _OUTCOME_MAP.get(raw_outcome, raw_outcome)

    closure_turn = closure_qualified_help_turn(trace)
    closure_present = closure_turn is not None
    user_state = reduce_user_state(
        getattr(trace, "user_state_signals", None), closure_turn
    )

    if adjudications is None:
        adjudications = load_adjudications()
    adjudicated = _is_adjudicated(case_spec, trace, adjudications)

    verdict, score, detail = decide(
        acceptance, actual_outcome, user_state, closure_present, adjudicated
    )
    return ConditionalOutcomeResult(
        verdict=verdict,
        score=score,
        user_state=user_state,
        closure_present=closure_present,
        adjudicated=adjudicated,
        detail=(
            f"{verdict} [outcome={actual_outcome}, user_state={user_state}, "
            f"closure_marker_turn={closure_turn}, adjudicated={adjudicated}]: {detail}"
        ),
    )
