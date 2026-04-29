"""Transcript evidence extraction for interactive CaseSpec generation.

The extractor reduces selected source turns to bounded, auditable signals.
It intentionally does not replay the human transcript; later generation
steps can use these fields for persona seeds, outcome resolution, and audit
records.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
import re
from typing import Any, Literal, Mapping, Sequence


TranscriptOutcome = Literal["resolve", "escalate", "unclear"]


@dataclass
class TranscriptSignal:
    """A labeled transcript signal with turn provenance."""

    category: str
    label: str
    sequence: int | None
    role: str
    speaker: str
    text: str

    def as_dict(self) -> dict[str, Any]:
        return {
            "category": self.category,
            "label": self.label,
            "sequence": self.sequence,
            "role": self.role,
            "speaker": self.speaker,
            "text": self.text,
        }


@dataclass
class TranscriptEvidence:
    """Structured evidence derived from one selected source transcript."""

    source_dataset: str
    turns_file: str
    turn_count: int
    form_issue_summary: str
    representative_user_messages: list[str]
    unresolved_user_signals: list[TranscriptSignal] = field(default_factory=list)
    human_investigation_signals: list[TranscriptSignal] = field(default_factory=list)
    handover_or_case_signals: list[TranscriptSignal] = field(default_factory=list)
    identifier_context_signals: list[TranscriptSignal] = field(default_factory=list)
    user_requested_human: bool = False
    transcript_indicated_outcome: TranscriptOutcome = "unclear"
    reason: str = ""

    def as_dict(self) -> dict[str, Any]:
        """Return an audit-friendly dictionary representation."""
        return {
            "source_dataset": self.source_dataset,
            "turns_file": self.turns_file,
            "turn_count": self.turn_count,
            "form_issue_summary": self.form_issue_summary,
            "representative_user_messages": list(self.representative_user_messages),
            "unresolved_user_signals": [
                signal.as_dict() for signal in self.unresolved_user_signals
            ],
            "human_investigation_signals": [
                signal.as_dict() for signal in self.human_investigation_signals
            ],
            "handover_or_case_signals": [
                signal.as_dict() for signal in self.handover_or_case_signals
            ],
            "identifier_context_signals": [
                signal.as_dict() for signal in self.identifier_context_signals
            ],
            "user_requested_human": self.user_requested_human,
            "transcript_indicated_outcome": self.transcript_indicated_outcome,
            "reason": self.reason,
        }


_FORM_PATTERN = re.compile(
    r"\[Form\]\s*Subject:\s*(?P<subject>.+?)\s*\|\s*Description:\s*(?P<description>.*)",
    re.IGNORECASE,
)
_FALLBACK_FORM_PATTERN = re.compile(
    r"Subject:\s*(?P<subject>.+?)\s*\|\s*Description:\s*(?P<description>.*)",
    re.IGNORECASE,
)

_UNRESOLVED_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    (
        "explicit_unresolved",
        re.compile(
            r"\bno\s+i\s+(?:have\s+)?not\b"
            r"|\bnot\s+(?:resolved|fixed|sorted|working|receiving|able)\b"
            r"|\b(?:isn't|isnt|not)\s+working\b",
            re.IGNORECASE,
        ),
    ),
    (
        "still_unable",
        re.compile(
            r"\bstill\b.{0,50}\b(?:can't|cant|cannot|unable|not|issue|problem|missing|blocked)\b"
            r"|\b(?:can't|cant|cannot|unable)\b.{0,50}\b(?:receive|access|login|log in|message|reply|post|find)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "confusion",
        re.compile(
            r"\bconfus(?:ed|ing|ion)\b"
            r"|\b(?:don't|do not)\s+understand\b"
            r"|\bnot\s+sure\b",
            re.IGNORECASE,
        ),
    ),
    (
        "identifier_confusion",
        re.compile(
            r"\b(?:both|two|multiple|different)\b.{0,50}\b(?:email|account)s?\b"
            r"|\b(?:login|contact)\s+email\b"
            r"|\bwhich\s+account\b",
            re.IGNORECASE,
        ),
    ),
    (
        "paid_service_impact",
        re.compile(
            r"\b(?:paid|payment|charged|charge|refund|money|booster|featured|buyer protection)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "frustration",
        re.compile(
            r"\b(?:frustrat(?:ed|ing|ion)|annoyed|angry|ridiculous|disappointed|unacceptable)\b",
            re.IGNORECASE,
        ),
    ),
)

_HUMAN_INVESTIGATION_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    (
        "investigation_needed",
        re.compile(
            r"\binvestigat(?:e|ed|ing|ion)\b"
            r"|\blook\s+into\b"
            r"|\blooked\s+into\b"
            r"|\blet\s+me\s+(?:have\s+a\s+)?look\b"
            r"|\bthoroughly\s+checked\b",
            re.IGNORECASE,
        ),
    ),
    (
        "internal_team",
        re.compile(
            r"\b(?:internal|tech|technical|development|dispute|support|relevant)\s+team\b"
            r"|\brelevant\s+department\b",
            re.IGNORECASE,
        ),
    ),
    (
        "async_update",
        re.compile(
            r"\b(?:get\s+back|contact|follow\s+up|feedback)\b.{0,80}\b(?:you|via\s+email|within|as\s+soon)"
            r"|\b(?:update\s+you|keep\s+you\s+updated)\b"
            r"|\bsend\s+you\s+an\s+email\b"
            r"|\b(?:(?:i|we)'?ll|(?:i|we)\s+will)\b.{0,60}\b(?:update|get\s+back|contact|email|follow\s+up)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "time_window",
        re.compile(
            r"\b\d+\s*(?:-|to)?\s*\d*\s*(?:hours|hrs|minutes|mins)\b"
            r"|\b(?:24|48|72|96)\s*(?:-|to)\s*(?:24|48|72|96)\s*(?:hours|hrs)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "raise_case_for_investigation",
        re.compile(
            r"\b(?:raise|raised|create|created|open|opened|log|logged)\b.{0,40}\bcase\b",
            re.IGNORECASE,
        ),
    ),
)

_HANDOVER_OR_CASE_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    (
        "handover",
        re.compile(
            r"\bhand\s*over\b"
            r"|\btransfer\b"
            r"|\bpass\b.{0,30}\b(?:to|on)\b"
            r"|\bhuman\s+agent\b"
            r"|\breal\s+person\b",
            re.IGNORECASE,
        ),
    ),
    (
        "escalation",
        re.compile(r"\bescalat(?:e|ed|ing|ion)\b", re.IGNORECASE),
    ),
    (
        "case_reference",
        re.compile(
            r"\b(?:raise|raised|create|created|open|opened|log|logged)\b.{0,40}\bcase\b"
            r"|\bcase\s+(?:number|reference|id)\b"
            r"|\byour\s+reference\b",
            re.IGNORECASE,
        ),
    ),
    ("manager", re.compile(r"\bmanager\b", re.IGNORECASE)),
)

_IDENTIFIER_CONTEXT_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    (
        "email_context",
        re.compile(r"\bemail(?:\s+address)?\b|\[EMAIL\]", re.IGNORECASE),
    ),
    (
        "account_context",
        re.compile(
            r"\baccount\b|\blog(?:ged|ging)?\s*in\b|\bsign(?:ed|ing)?\s*in\b|\blogin\b",
            re.IGNORECASE,
        ),
    ),
    (
        "ad_context",
        re.compile(r"\bad\s*id\b|\badvert\b|\bad\b|\blisting\b", re.IGNORECASE),
    ),
    (
        "moderation_context",
        re.compile(
            r"\b(?:removed|deleted|on\s+hold|hold|rules?|moderation|blocked|banned|rejected)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "platform_context",
        re.compile(r"\b(?:app|site|browser|laptop|desktop|mobile|web)\b", re.IGNORECASE),
    ),
)

_USER_REQUESTED_HUMAN_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    (
        "request_human",
        re.compile(
            r"\b(?:speak|talk|chat|connect|transfer|pass|escalate)\b.{0,50}\b(?:human|person|agent|representative|manager|supervisor|someone)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "request_real_person",
        re.compile(
            r"\b(?:real\s+person|human\s+agent|human\s+being|live\s+agent)\b",
            re.IGNORECASE,
        ),
    ),
    (
        "request_manager",
        re.compile(
            r"\bi\s+(?:want|need|would\s+like)\b.{0,50}\b(?:manager|supervisor|agent|human)\b",
            re.IGNORECASE,
        ),
    ),
)

_AGENT_RESOLUTION_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(
        r"\b(?:i\s+can\s+confirm|confirmed|has\s+been\s+fixed|is\s+fixed|now\s+fixed|now\s+resolved|has\s+been\s+resolved)\b",
        re.IGNORECASE,
    ),
    re.compile(
        r"\b(?:you\s+should\s+now|you\s+will\s+now|you\s+can\s+now)\b.{0,80}\b(?:receive|access|reply|message|post|see)\b",
        re.IGNORECASE,
    ),
    re.compile(
        r"\b(?:your\s+ad\s+is\s+live|ad\s+is\s+live|account\s+is\s+active|messages\s+are\s+enabled)\b",
        re.IGNORECASE,
    ),
    re.compile(r"\bi\s+hope\s+(?:the\s+above\s+)?information\s+helps\b", re.IGNORECASE),
)

_VISITOR_POSITIVE_CLOSURE = re.compile(
    r"\b(?:thanks|thank\s+you|that'?s\s+all|perfect|great|brilliant|cheers)\b",
    re.IGNORECASE,
)
_PLACEHOLDER_ONLY = re.compile(r"^\[[A-Z_]+\]$")
_ACK_ONLY = re.compile(
    r"^(?:hi|hello|hey|ok|okay|yes|yep|no|nope|cheers|bye|gotcha"
    r"|thanks(?:\s+\w+)?|thank\s+you(?:\s+\w+)?)[.! ]*$",
    re.IGNORECASE,
)
_NEGATIVE_SHORT = re.compile(
    r"^(?:no|nope|not\s+yet|no\s+i\s+have\s+not|i\s+have\s+not)[.! ]*$",
    re.IGNORECASE,
)
_RESOLUTION_QUESTION = re.compile(
    r"\b(?:resolved?|fixed|sorted|working|help(?:ed)?)\b", re.IGNORECASE
)


def extract_transcript_evidence(
    selected_source_turns: Sequence[Mapping[str, Any]],
    *,
    turns_file: str | Path,
    source_dataset: str,
    max_representative_messages: int = 3,
) -> TranscriptEvidence:
    """Extract structured evidence from one selected source transcript.

    Args:
        selected_source_turns: Turns already selected from the source dataset
            for a single conversation/session. The function never loads or
            merges other datasets.
        turns_file: Metadata for the selected turns file.
        source_dataset: HR/source dataset key used to select ``turns_file``.
        max_representative_messages: Upper bound for persona seed messages.
    """
    turns = sorted(selected_source_turns, key=_turn_sort_key)
    form_issue_summary = _extract_form_issue_summary(turns)
    visitor_turns = [turn for turn in turns if _is_visitor_turn(turn)]
    agent_turns = [turn for turn in turns if _is_agent_turn(turn)]

    unresolved_user_signals = _extract_unresolved_user_signals(visitor_turns, turns)
    human_investigation_signals = _extract_signals(
        agent_turns,
        _HUMAN_INVESTIGATION_PATTERNS,
        category="human_investigation",
    )
    handover_or_case_signals = _extract_signals(
        agent_turns,
        _HANDOVER_OR_CASE_PATTERNS,
        category="handover_or_case",
    )
    identifier_context_signals = _extract_signals(
        [turn for turn in turns if not _is_form_turn(turn)],
        _IDENTIFIER_CONTEXT_PATTERNS,
        category="identifier_context",
    )
    user_requested_human_signals = _extract_signals(
        visitor_turns,
        _USER_REQUESTED_HUMAN_PATTERNS,
        category="user_requested_human",
    )
    representative_user_messages = _select_representative_user_messages(
        visitor_turns,
        unresolved_user_signals,
        max_messages=max_representative_messages,
        fallback=form_issue_summary,
    )
    outcome, reason = _resolve_transcript_outcome(
        turns=turns,
        unresolved_user_signals=unresolved_user_signals,
        human_investigation_signals=human_investigation_signals,
        handover_or_case_signals=handover_or_case_signals,
        user_requested_human=bool(user_requested_human_signals),
    )

    return TranscriptEvidence(
        source_dataset=source_dataset,
        turns_file=Path(turns_file).name,
        turn_count=len(turns),
        form_issue_summary=form_issue_summary,
        representative_user_messages=representative_user_messages,
        unresolved_user_signals=unresolved_user_signals,
        human_investigation_signals=human_investigation_signals,
        handover_or_case_signals=handover_or_case_signals,
        identifier_context_signals=identifier_context_signals,
        user_requested_human=bool(user_requested_human_signals),
        transcript_indicated_outcome=outcome,
        reason=reason,
    )


def build_transcript_evidence(
    selected_source_turns: Sequence[Mapping[str, Any]],
    turns_file: str | Path,
    source_dataset: str,
) -> TranscriptEvidence:
    """Compatibility wrapper with positional metadata arguments."""
    return extract_transcript_evidence(
        selected_source_turns,
        turns_file=turns_file,
        source_dataset=source_dataset,
    )


def _extract_unresolved_user_signals(
    visitor_turns: Sequence[Mapping[str, Any]],
    all_turns: Sequence[Mapping[str, Any]],
) -> list[TranscriptSignal]:
    signals = _extract_signals(
        visitor_turns,
        _UNRESOLVED_PATTERNS,
        category="unresolved_user",
    )
    existing = {(signal.sequence, signal.label) for signal in signals}
    for turn in visitor_turns:
        message = _message(turn)
        if not _NEGATIVE_SHORT.match(message):
            continue
        previous_agent = _previous_agent_turn(all_turns, _safe_sequence(turn))
        if previous_agent is None:
            continue
        if not _RESOLUTION_QUESTION.search(_message(previous_agent)):
            continue
        key = (_safe_sequence(turn), "explicit_unresolved")
        if key in existing:
            continue
        signals.append(
            _signal(
                turn,
                category="unresolved_user",
                label="explicit_unresolved",
            )
        )
        existing.add(key)
    return sorted(signals, key=lambda signal: (signal.sequence is None, signal.sequence or 0))


def _extract_signals(
    turns: Sequence[Mapping[str, Any]],
    patterns: Sequence[tuple[str, re.Pattern[str]]],
    *,
    category: str,
) -> list[TranscriptSignal]:
    signals: list[TranscriptSignal] = []
    seen: set[tuple[int | None, str, str]] = set()
    for turn in turns:
        message = _message(turn)
        if not message:
            continue
        for label, pattern in patterns:
            if not pattern.search(message):
                continue
            key = (_safe_sequence(turn), label, message)
            if key in seen:
                continue
            signals.append(_signal(turn, category=category, label=label))
            seen.add(key)
    return signals


def _select_representative_user_messages(
    visitor_turns: Sequence[Mapping[str, Any]],
    unresolved_user_signals: Sequence[TranscriptSignal],
    *,
    max_messages: int,
    fallback: str,
) -> list[str]:
    if max_messages <= 0:
        return []

    substantive = [
        turn for turn in visitor_turns if _is_representative_candidate(_message(turn))
    ]
    selected: list[str] = []

    def add(text: str) -> None:
        cleaned = _clean_text(text)
        if cleaned and cleaned not in selected and len(selected) < max_messages:
            selected.append(cleaned)

    if substantive:
        add(_message(substantive[0]))

    best_unresolved = _best_unresolved_message(unresolved_user_signals)
    if best_unresolved:
        add(best_unresolved)

    for turn in reversed(substantive):
        add(_message(turn))
        if len(selected) >= max_messages:
            break

    if len(selected) < max_messages:
        scored = sorted(
            substantive,
            key=lambda turn: _representative_score(turn, unresolved_user_signals),
            reverse=True,
        )
        for turn in scored:
            add(_message(turn))
            if len(selected) >= max_messages:
                break

    if not selected and fallback:
        selected.append(fallback)
    return selected


def _best_unresolved_message(signals: Sequence[TranscriptSignal]) -> str:
    if not signals:
        return ""
    label_weight = {
        "confusion": 40,
        "identifier_confusion": 35,
        "explicit_unresolved": 30,
        "still_unable": 30,
        "paid_service_impact": 20,
        "frustration": 20,
    }
    best = max(
        signals,
        key=lambda signal: (label_weight.get(signal.label, 0), len(signal.text)),
    )
    return best.text


def _representative_score(
    turn: Mapping[str, Any],
    unresolved_user_signals: Sequence[TranscriptSignal],
) -> int:
    seq = _safe_sequence(turn)
    score = min(len(_message(turn)), 120)
    if any(signal.sequence == seq for signal in unresolved_user_signals):
        score += 100
    if any(pattern.search(_message(turn)) for _, pattern in _IDENTIFIER_CONTEXT_PATTERNS):
        score += 25
    return score


def _resolve_transcript_outcome(
    *,
    turns: Sequence[Mapping[str, Any]],
    unresolved_user_signals: Sequence[TranscriptSignal],
    human_investigation_signals: Sequence[TranscriptSignal],
    handover_or_case_signals: Sequence[TranscriptSignal],
    user_requested_human: bool,
) -> tuple[TranscriptOutcome, str]:
    if user_requested_human:
        return "escalate", "visitor explicitly requested a human or manager"

    if handover_or_case_signals:
        labels = _label_summary(handover_or_case_signals)
        return "escalate", f"agent transcript includes handover/case signal(s): {labels}"

    strong_investigation = [
        signal
        for signal in human_investigation_signals
        if signal.label
        in {
            "investigation_needed",
            "internal_team",
            "async_update",
            "time_window",
            "raise_case_for_investigation",
        }
    ]
    if unresolved_user_signals and strong_investigation:
        return (
            "escalate",
            "unresolved user signal(s) combined with agent investigation/follow-up language",
        )

    if len(unresolved_user_signals) >= 2 and _has_identifier_confusion(unresolved_user_signals):
        return (
            "escalate",
            "repeated unresolved/account-confusion signal(s) in visitor turns",
        )

    if strong_investigation and not _has_resolution_evidence(turns):
        labels = _label_summary(strong_investigation)
        return "escalate", f"agent required investigation or async follow-up: {labels}"

    if _has_resolution_evidence(turns) and not unresolved_user_signals:
        return "resolve", "agent transcript contains resolution/answer confirmation and no unresolved user signal"

    if unresolved_user_signals:
        labels = _label_summary(unresolved_user_signals)
        return "unclear", f"unresolved user signal(s) present without clear handover evidence: {labels}"

    return "unclear", "no strong transcript outcome signal detected"


def _has_identifier_confusion(signals: Sequence[TranscriptSignal]) -> bool:
    return any(signal.label in {"identifier_confusion", "confusion"} for signal in signals)


def _has_resolution_evidence(turns: Sequence[Mapping[str, Any]]) -> bool:
    for turn in turns:
        if not _is_agent_turn(turn):
            continue
        message = _message(turn)
        if any(pattern.search(message) for pattern in _AGENT_RESOLUTION_PATTERNS):
            return True

    last_visitor = next((turn for turn in reversed(turns) if _is_visitor_turn(turn)), None)
    if last_visitor is None:
        return False
    if not _VISITOR_POSITIVE_CLOSURE.search(_message(last_visitor)):
        return False
    return any(
        _is_agent_turn(turn)
        and (
            "anything else" in _message(turn).lower()
            or any(pattern.search(_message(turn)) for pattern in _AGENT_RESOLUTION_PATTERNS)
        )
        for turn in turns
    )


def _extract_form_issue_summary(turns: Sequence[Mapping[str, Any]]) -> str:
    form_turn = next((turn for turn in turns if _is_form_turn(turn)), None)
    if form_turn is None:
        return ""
    message = _message(form_turn)
    for pattern in (_FORM_PATTERN, _FALLBACK_FORM_PATTERN):
        match = pattern.match(message)
        if match:
            description = _clean_text(match.group("description"))
            subject = _clean_text(match.group("subject"))
            return description or subject
    return message


def _previous_agent_turn(
    turns: Sequence[Mapping[str, Any]], sequence: int | None
) -> Mapping[str, Any] | None:
    if sequence is None:
        return None
    candidates = [
        turn
        for turn in turns
        if _is_agent_turn(turn)
        and _safe_sequence(turn) is not None
        and (_safe_sequence(turn) or 0) < sequence
    ]
    if not candidates:
        return None
    return max(candidates, key=lambda turn: _safe_sequence(turn) or -1)


def _signal(turn: Mapping[str, Any], *, category: str, label: str) -> TranscriptSignal:
    return TranscriptSignal(
        category=category,
        label=label,
        sequence=_safe_sequence(turn),
        role=str(turn.get("role", "")).strip(),
        speaker=str(turn.get("speaker", "")).strip(),
        text=_message(turn),
    )


def _is_representative_candidate(message: str) -> bool:
    cleaned = _clean_text(message)
    if not cleaned:
        return False
    if _PLACEHOLDER_ONLY.match(cleaned):
        return False
    if _ACK_ONLY.match(cleaned):
        return False
    return True


def _label_summary(signals: Sequence[TranscriptSignal]) -> str:
    labels: list[str] = []
    for signal in signals:
        if signal.label not in labels:
            labels.append(signal.label)
    return ", ".join(labels)


def _is_form_turn(turn: Mapping[str, Any]) -> bool:
    return _safe_sequence(turn) == 0 and str(turn.get("speaker", "")).strip() == "[PRE_CHAT_FORM]"


def _is_visitor_turn(turn: Mapping[str, Any]) -> bool:
    if _is_form_turn(turn):
        return False
    return str(turn.get("role", "")).strip().lower() in {"visitor", "user", "customer"}


def _is_agent_turn(turn: Mapping[str, Any]) -> bool:
    return str(turn.get("role", "")).strip().lower() in {"agent", "assistant", "bot"}


def _message(turn: Mapping[str, Any]) -> str:
    for key in ("message_redacted", "message", "text", "content"):
        value = turn.get(key)
        if value is not None:
            return _clean_text(str(value))
    return ""


def _clean_text(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def _turn_sort_key(turn: Mapping[str, Any]) -> tuple[int, str]:
    sequence = _safe_sequence(turn)
    if sequence is None:
        return (10**9, _message(turn))
    return (sequence, _message(turn))


def _safe_sequence(turn: Mapping[str, Any]) -> int | None:
    try:
        return int(str(turn.get("sequence", "")).strip())
    except (TypeError, ValueError):
        return None
