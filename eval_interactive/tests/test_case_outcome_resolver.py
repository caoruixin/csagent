from __future__ import annotations

from types import SimpleNamespace

from eval_interactive.case_spec.case_outcome_resolver import resolve_case_outcome
from eval_interactive.case_spec.policy_table import get_policy


def test_mandatory_uc_escalation_wins_over_resolved_transcript_hint() -> None:
    result = resolve_case_outcome(
        get_policy("UC-G"),
        {"escalation_trigger": "gdpr_intake"},
        SimpleNamespace(transcript_indicated_outcome="resolve"),
    )

    assert result.outcome_class == "escalate"
    assert result.should_escalate is True
    assert result.escalation_trigger == "gdpr_intake"
    assert "mandatory_policy_escalation" in result.decision_reason


def test_uc_k_partial_escalates_from_transcript_evidence() -> None:
    result = resolve_case_outcome(
        get_policy("UC-K"),
        {},
        SimpleNamespace(
            transcript_indicated_outcome="unclear",
            handover_or_case_signals=["agent opened a support case"],
        ),
    )

    assert result.outcome_class == "escalate"
    assert result.should_escalate is True
    assert result.escalation_trigger == "intake_complete_for_uc_k"
    assert "partial_uc_transcript_escalation" in result.decision_reason


def test_uc_k_partial_resolves_when_transcript_indicates_resolution() -> None:
    result = resolve_case_outcome(
        get_policy("UC-K"),
        {"outcome_class": "escalate", "should_escalate": "true"},
        SimpleNamespace(
            transcript_indicated_outcome={
                "outcome": "resolve",
                "reason": "runtime state answered the status question",
            },
        ),
    )

    assert result.outcome_class == "resolve"
    assert result.should_escalate is False
    assert result.escalation_trigger is None
    assert "partial_uc_transcript_resolve" in result.decision_reason


def test_uc_k_unclear_evidence_falls_back_to_policy_default() -> None:
    result = resolve_case_outcome(
        get_policy("UC-K"),
        {"outcome_class": "resolve", "should_escalate": "false"},
        SimpleNamespace(transcript_indicated_outcome="unclear"),
    )

    assert result.outcome_class == "escalate"
    assert result.should_escalate is True
    assert result.escalation_trigger == "intake_complete_for_uc_k"
    assert "policy_default" in result.decision_reason


def test_faq_uc_escalates_on_strong_unresolved_and_investigation_evidence() -> None:
    result = resolve_case_outcome(
        get_policy("UC-C"),
        {},
        SimpleNamespace(
            unresolved_user_signals=["user says the account still does not make sense"],
            human_investigation_signals=["agent says internal team must investigate"],
        ),
    )

    assert result.outcome_class == "escalate"
    assert result.should_escalate is True
    assert result.escalation_trigger == "faq_miss_threshold_exceeded"
    assert "faq_transcript_escalation" in result.decision_reason


def test_faq_uc_clean_or_unclear_evidence_uses_policy_default_resolution() -> None:
    result = resolve_case_outcome(
        get_policy("UC-C"),
        {"should_escalate": "true", "escalation_trigger": "user_requested"},
        SimpleNamespace(transcript_indicated_outcome="resolve"),
    )

    assert result.outcome_class == "resolve"
    assert result.should_escalate is False
    assert result.escalation_trigger is None
    assert "policy_default" in result.decision_reason


def test_faq_uc_resolved_transcript_beats_weak_investigation_signal() -> None:
    result = resolve_case_outcome(
        get_policy("UC-D"),
        {"should_escalate": "true", "escalation_trigger": "faq_miss_threshold_exceeded"},
        SimpleNamespace(
            transcript_indicated_outcome="resolve",
            human_investigation_signals=[
                "instructional login guidance mentioning the email you need to sign in with"
            ],
        ),
    )

    assert result.outcome_class == "resolve"
    assert result.should_escalate is False
    assert result.escalation_trigger is None


def test_faq_uc_hard_handover_signal_wins_over_resolved_transcript_hint() -> None:
    result = resolve_case_outcome(
        get_policy("UC-D"),
        {},
        SimpleNamespace(
            transcript_indicated_outcome="resolve",
            handover_or_case_signals=["agent opened a support case"],
        ),
    )

    assert result.outcome_class == "escalate"
    assert result.should_escalate is True
    assert result.escalation_trigger == "faq_miss_threshold_exceeded"


def test_faq_uc_unresolved_signal_alone_does_not_force_escalation() -> None:
    result = resolve_case_outcome(
        get_policy("UC-C"),
        {},
        SimpleNamespace(
            transcript_indicated_outcome="unclear",
            unresolved_user_signals=["user says they are not sure yet"],
        ),
    )

    assert result.outcome_class == "resolve"
    assert result.should_escalate is False
    assert result.escalation_trigger is None


def test_faq_uc_clarification_exhaustion_uses_canonical_trigger() -> None:
    result = resolve_case_outcome(
        get_policy("UC-B"),
        {},
        SimpleNamespace(clarification_budget_exhausted=True),
    )

    assert result.outcome_class == "escalate"
    assert result.should_escalate is True
    assert result.escalation_trigger == "clarification_budget_exhausted"


def test_out_of_scope_row_escalates_even_with_fallback_policy() -> None:
    result = resolve_case_outcome(
        get_policy("UC-A"),
        {
            "primary_uc_corrected": "OUT_OF_SCOPE_DELIVERY",
            "escalation_trigger": "out_of_scope",
        },
        SimpleNamespace(transcript_indicated_outcome="resolve"),
    )

    assert result.outcome_class == "escalate"
    assert result.should_escalate is True
    assert result.escalation_trigger == "out_of_scope"
    assert "mandatory_policy_escalation" in result.decision_reason
