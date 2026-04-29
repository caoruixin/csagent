"""Resolve CaseSpec expected outcomes from policy, HR hints, and evidence.

The resolver is intentionally independent from the extractor so the case-spec
generation pipeline can feed it any ``TranscriptEvidence``-like object.  The
object only needs to expose the evidence fields named in
``docs/phase5_evaluation_design.md``; dictionaries, dataclasses, and simple
objects are all accepted.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Literal, Mapping, Optional

from .policy_table import UcPolicy
from .schema import ESCALATION_TRIGGER_VALUES, EscalationTrigger


ResolvedOutcomeClass = Literal["resolve", "escalate"]
TranscriptOutcomeHint = Literal["resolve", "escalate", "unclear"]

_VALID_TRIGGERS = set(ESCALATION_TRIGGER_VALUES)
_MANDATORY_ESCALATION_UCS = {"UC-G", "UC-H", "UC-I", "UC-J"}
_HANDOVER_ONLY_TOPICS = {
    "delivery",
    "pro contract",
    "account manager support",
    "ratings reviews",
}


@dataclass(frozen=True)
class CaseOutcomeResolution:
    """Resolved case-level outcome fields for ``Expected``."""

    outcome_class: ResolvedOutcomeClass
    should_escalate: bool
    escalation_trigger: Optional[EscalationTrigger]
    decision_reason: str


# Backwards-friendly alias for callers/tests that prefer shorter naming.
OutcomeResolution = CaseOutcomeResolution


@dataclass(frozen=True)
class _EvidenceSummary:
    transcript_outcome: TranscriptOutcomeHint
    transcript_reason: str
    transcript_trigger: str | None
    user_requested_human: bool
    unresolved_user: bool
    human_investigation: bool
    handover_or_case: bool
    clarification_exhausted: bool

    @property
    def escalation_flags(self) -> tuple[str, ...]:
        flags: list[str] = []
        if self.user_requested_human:
            flags.append("user_requested_human")
        if self.unresolved_user:
            flags.append("unresolved_user_signals")
        if self.human_investigation:
            flags.append("human_investigation_signals")
        if self.handover_or_case:
            flags.append("handover_or_case_signals")
        if self.clarification_exhausted:
            flags.append("clarification_exhaustion_signals")
        return tuple(flags)

    @property
    def has_hard_escalation_evidence(self) -> bool:
        return (
            self.transcript_outcome == "escalate"
            or self.user_requested_human
            or self.handover_or_case
            or self.clarification_exhausted
        )

    @property
    def has_investigation_escalation_evidence(self) -> bool:
        return self.human_investigation and self.transcript_outcome != "resolve"

    @property
    def has_strong_escalation_evidence(self) -> bool:
        return (
            self.has_hard_escalation_evidence
            or self.has_investigation_escalation_evidence
        )


def case_outcome_resolver(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None = None,
    transcript_evidence: Any | None = None,
) -> CaseOutcomeResolution:
    """Compatibility wrapper matching the design-doc wording."""

    return resolve_case_outcome(policy, hr_row, transcript_evidence)


def resolve_case_outcome(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None = None,
    transcript_evidence: Any | None = None,
    *,
    evidence: Any | None = None,
) -> CaseOutcomeResolution:
    """Resolve ``outcome_class``, ``should_escalate`` and trigger.

    Precedence:
    1. Mandatory escalation policies for UC-G/H/I/J, explicit
       ``allow_bot_resolution=false`` policies, and OUT_OF_SCOPE handover
       classes.
    2. For partial-resolution UCs such as UC-K, selected transcript evidence
       chooses resolve vs escalate.
    3. For FAQ-resolvable UCs, strong transcript evidence may escalate.
    4. Weak or unclear evidence falls back to the policy default.
    """

    if transcript_evidence is None:
        transcript_evidence = evidence

    evidence_summary = _summarize_evidence(transcript_evidence)

    if _is_mandatory_escalation(policy, hr_row):
        trigger = _choose_escalation_trigger(policy, hr_row, evidence_summary)
        return CaseOutcomeResolution(
            outcome_class="escalate",
            should_escalate=True,
            escalation_trigger=trigger,
            decision_reason=(
                "mandatory_policy_escalation: "
                f"{_policy_uc(policy)} must hand over; trigger={trigger}"
            ),
        )

    if policy.allow_bot_resolution == "partial":
        return _resolve_partial_policy(policy, hr_row, evidence_summary)

    if policy.allow_bot_resolution == "true":
        return _resolve_faq_policy(policy, hr_row, evidence_summary)

    return _policy_default(policy, hr_row, evidence_summary)


def _resolve_partial_policy(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None,
    evidence: _EvidenceSummary,
) -> CaseOutcomeResolution:
    if evidence.has_hard_escalation_evidence:
        trigger = _choose_escalation_trigger(policy, hr_row, evidence)
        return CaseOutcomeResolution(
            outcome_class="escalate",
            should_escalate=True,
            escalation_trigger=trigger,
            decision_reason=(
                "partial_uc_transcript_escalation: "
                f"{_evidence_reason(evidence)}; trigger={trigger}"
            ),
        )

    if evidence.transcript_outcome == "resolve":
        return CaseOutcomeResolution(
            outcome_class="resolve",
            should_escalate=False,
            escalation_trigger=None,
            decision_reason=(
                "partial_uc_transcript_resolve: transcript evidence indicates "
                f"resolution; {_transcript_reason_suffix(evidence)}"
            ),
        )

    if evidence.has_investigation_escalation_evidence:
        trigger = _choose_escalation_trigger(policy, hr_row, evidence)
        return CaseOutcomeResolution(
            outcome_class="escalate",
            should_escalate=True,
            escalation_trigger=trigger,
            decision_reason=(
                "partial_uc_transcript_escalation: "
                f"{_evidence_reason(evidence)}; trigger={trigger}"
            ),
        )

    return _policy_default(policy, hr_row, evidence)


def _resolve_faq_policy(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None,
    evidence: _EvidenceSummary,
) -> CaseOutcomeResolution:
    if evidence.has_hard_escalation_evidence:
        trigger = _choose_escalation_trigger(policy, hr_row, evidence)
        return CaseOutcomeResolution(
            outcome_class="escalate",
            should_escalate=True,
            escalation_trigger=trigger,
            decision_reason=(
                "faq_transcript_escalation: "
                f"{_evidence_reason(evidence)}; trigger={trigger}"
            ),
        )

    if evidence.transcript_outcome == "resolve":
        return _policy_default(policy, hr_row, evidence)

    if evidence.has_investigation_escalation_evidence:
        trigger = _choose_escalation_trigger(policy, hr_row, evidence)
        return CaseOutcomeResolution(
            outcome_class="escalate",
            should_escalate=True,
            escalation_trigger=trigger,
            decision_reason=(
                "faq_transcript_escalation: "
                f"{_evidence_reason(evidence)}; trigger={trigger}"
            ),
        )

    return _policy_default(policy, hr_row, evidence)


def _policy_default(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None,
    evidence: _EvidenceSummary,
) -> CaseOutcomeResolution:
    should_escalate = bool(policy.should_escalate_default)
    if policy.outcome_class == "escalate":
        should_escalate = True
    if policy.outcome_class == "resolve":
        should_escalate = False

    if should_escalate:
        trigger = _choose_escalation_trigger(policy, hr_row, evidence)
        outcome: ResolvedOutcomeClass = "escalate"
    else:
        trigger = None
        outcome = "resolve"

    return CaseOutcomeResolution(
        outcome_class=outcome,
        should_escalate=should_escalate,
        escalation_trigger=trigger,
        decision_reason=(
            "policy_default: transcript evidence unclear or insufficient; "
            f"policy_outcome={policy.outcome_class}, "
            f"policy_should_escalate={policy.should_escalate_default}"
        ),
    )


def _choose_escalation_trigger(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None,
    evidence: _EvidenceSummary,
) -> EscalationTrigger:
    if _is_out_of_scope(policy, hr_row):
        return "out_of_scope"

    evidence_trigger = _normalise_trigger(evidence.transcript_trigger)
    if evidence_trigger is not None:
        return evidence_trigger

    if evidence.user_requested_human:
        return "user_requested"

    if evidence.clarification_exhausted:
        return "clarification_budget_exhausted"

    hr_trigger = _normalise_trigger(_get_value(hr_row, "escalation_trigger"))
    if hr_trigger is not None:
        return hr_trigger

    policy_trigger = _normalise_trigger(policy.default_escalation_trigger)
    if policy_trigger is not None:
        return policy_trigger

    if policy.allow_bot_resolution == "true":
        return "faq_miss_threshold_exceeded"

    return "incomplete_intake"


def _summarize_evidence(transcript_evidence: Any | None) -> _EvidenceSummary:
    outcome, reason, trigger = _read_transcript_outcome(transcript_evidence)
    return _EvidenceSummary(
        transcript_outcome=outcome,
        transcript_reason=reason,
        transcript_trigger=trigger,
        user_requested_human=_has_signal(
            transcript_evidence,
            "user_requested_human",
            "user_requested_human_signal",
            "human_request_signals",
            "requested_human_signals",
        ),
        unresolved_user=_has_signal(
            transcript_evidence,
            "unresolved_user_signals",
            "unresolved_confusion_signals",
            "unresolved_signals",
            "confusion_signals",
            "account_confusion_signals",
        ),
        human_investigation=_has_signal(
            transcript_evidence,
            "human_investigation_signals",
            "agent_investigation_signals",
            "investigation_signals",
            "human_review_signals",
        ),
        handover_or_case=_has_signal(
            transcript_evidence,
            "handover_or_case_signals",
            "handover_case_signals",
            "handover_signals",
            "case_signals",
        ),
        clarification_exhausted=_has_signal(
            transcript_evidence,
            "clarification_exhaustion_signals",
            "clarification_budget_exhausted",
            "clarification_exhausted",
            "clarification_loop_signals",
            "repeated_clarification_signals",
        ),
    )


def _read_transcript_outcome(
    transcript_evidence: Any | None,
) -> tuple[TranscriptOutcomeHint, str, str | None]:
    raw = _get_value(
        transcript_evidence,
        "transcript_indicated_outcome",
        "indicated_outcome",
        "outcome",
    )
    reason = _get_value(
        transcript_evidence,
        "transcript_indicated_outcome_reason",
        "outcome_reason",
        "decision_reason",
        "reason",
    )
    trigger = _get_value(
        transcript_evidence,
        "transcript_escalation_trigger",
        "escalation_trigger",
        "trigger",
    )

    if isinstance(raw, Mapping):
        reason = raw.get("reason") or raw.get("decision_reason") or reason
        trigger = raw.get("escalation_trigger") or raw.get("trigger") or trigger
        raw = raw.get("outcome") or raw.get("outcome_class") or raw.get("value")
    elif isinstance(raw, (tuple, list)):
        if raw:
            first = raw[0]
            if len(raw) > 1 and not reason:
                reason = raw[1]
            raw = first
    elif raw is not None and not isinstance(raw, str):
        reason = _get_value(raw, "reason", "decision_reason") or reason
        trigger = _get_value(raw, "escalation_trigger", "trigger") or trigger
        raw = _get_value(raw, "outcome", "outcome_class", "value")

    return _normalise_outcome(raw), str(reason or "").strip(), _string_or_none(trigger)


def _is_mandatory_escalation(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None,
) -> bool:
    if _is_out_of_scope(policy, hr_row):
        return True
    if _normalise_uc(_policy_uc(policy)) in _MANDATORY_ESCALATION_UCS:
        return True
    return policy.allow_bot_resolution == "false"


def _is_out_of_scope(
    policy: UcPolicy,
    hr_row: Mapping[str, Any] | Any | None,
) -> bool:
    candidates = [
        _policy_uc(policy),
        _get_value(hr_row, "primary_uc_corrected"),
        _get_value(hr_row, "primary_uc"),
        _get_value(hr_row, "source_primary_uc"),
        _get_value(hr_row, "active_use_case"),
    ]
    if any(_looks_out_of_scope(value) for value in candidates):
        return True

    topic = _string_or_none(_get_value(hr_row, "form_topic_subject", "topic_subject"))
    hr_trigger = _normalise_trigger(_get_value(hr_row, "escalation_trigger"))
    return bool(topic and topic.strip().lower() in _HANDOVER_ONLY_TOPICS and hr_trigger == "out_of_scope")


def _looks_out_of_scope(value: Any) -> bool:
    text = _string_or_none(value)
    if not text:
        return False
    text = text.strip().upper()
    return text.startswith("OUT_OF_SCOPE") or text.startswith("OOS")


def _normalise_uc(value: Any) -> str:
    text = _string_or_none(value)
    if not text:
        return ""
    text = text.strip().upper()
    if text.startswith("UC-") and text.count("-") >= 2:
        head, mid, *_ = text.split("-")
        return f"{head}-{mid}"
    return text


def _policy_uc(policy: UcPolicy) -> str:
    return _string_or_none(getattr(policy, "uc", "")) or ""


def _normalise_outcome(value: Any) -> TranscriptOutcomeHint:
    text = _string_or_none(value)
    if not text:
        return "unclear"
    text = text.strip().lower()
    if text in {"resolve", "resolved", "resolution", "success"}:
        return "resolve"
    if text in {"escalate", "escalated", "handover", "hand_over", "case", "human"}:
        return "escalate"
    return "unclear"


def _normalise_trigger(value: Any) -> EscalationTrigger | None:
    text = _string_or_none(value)
    if not text:
        return None
    text = text.strip()
    if text in _VALID_TRIGGERS:
        return text  # type: ignore[return-value]
    lower = text.lower()
    if lower in _VALID_TRIGGERS:
        return lower  # type: ignore[return-value]
    if lower.startswith("out_of_scope"):
        return "out_of_scope"
    return None


def _has_signal(source: Any | None, *names: str) -> bool:
    return any(_truthy_signal(_get_value(source, name)) for name in names)


def _truthy_signal(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        stripped = value.strip().lower()
        return bool(stripped) and stripped not in {"0", "false", "no", "none", "null", "unclear"}
    if isinstance(value, Mapping):
        return bool(value)
    if isinstance(value, (list, tuple, set, frozenset)):
        return bool(value)
    return bool(value)


def _get_value(source: Any | None, *names: str) -> Any | None:
    if source is None:
        return None
    for name in names:
        value = None
        found = False
        if isinstance(source, Mapping) and name in source:
            value = source[name]
            found = True
        elif hasattr(source, name):
            value = getattr(source, name)
            found = True
        if found and value is not None:
            return value
    return None


def _string_or_none(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return str(value)


def _evidence_reason(evidence: _EvidenceSummary) -> str:
    parts: list[str] = []
    if evidence.transcript_outcome == "escalate":
        parts.append("transcript_indicated_outcome=escalate")
    flags = evidence.escalation_flags
    if flags:
        parts.append(f"flags={','.join(flags)}")
    if evidence.transcript_reason:
        parts.append(f"reason={evidence.transcript_reason}")
    return "; ".join(parts) if parts else "strong escalation evidence"


def _transcript_reason_suffix(evidence: _EvidenceSummary) -> str:
    if evidence.transcript_reason:
        return f"reason={evidence.transcript_reason}"
    return "no escalation evidence found"


__all__ = [
    "CaseOutcomeResolution",
    "OutcomeResolution",
    "case_outcome_resolver",
    "resolve_case_outcome",
]
