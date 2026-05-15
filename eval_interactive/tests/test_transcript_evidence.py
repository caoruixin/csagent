"""Tests for transcript evidence extraction."""

from __future__ import annotations

import csv
from pathlib import Path

from eval_interactive.case_spec.extractor import _build_case_spec
from eval_interactive.case_spec.transcript_evidence import (
    TranscriptEvidence,
    extract_transcript_evidence,
)


REPO_ROOT = Path(__file__).resolve().parents[2]
EVAL_DATASETS_DIR = REPO_ROOT / "data" / "eval_datasets"
HR_CSV = REPO_ROOT / "data" / "human_review_annotations_2026-04-22_golden.csv"


def _load_turns(turns_file: Path, conversation_id: str) -> list[dict[str, str]]:
    with turns_file.open("r", encoding="utf-8", newline="") as f:
        return [
            row
            for row in csv.DictReader(f)
            if row.get("conversation_id") == conversation_id
        ]


def _load_hr_row(session_id: str) -> dict[str, str]:
    with HR_CSV.open("r", encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            if row.get("session_id") == session_id:
                return row
    raise AssertionError(f"HR row not found for session_id={session_id}")


def _labels(evidence_signals) -> set[str]:
    return {signal.label for signal in evidence_signals}


def test_cs_interactive_001_like_evidence_detects_unresolved_investigation() -> None:
    turns_file = EVAL_DATASETS_DIR / "badcase_turns.csv"
    turns = _load_turns(turns_file, "570Q5000008kr6LIAQ")

    evidence = extract_transcript_evidence(
        turns,
        turns_file=turns_file,
        source_dataset="badcase",
    )

    assert isinstance(evidence, TranscriptEvidence)
    assert evidence.source_dataset == "badcase"
    assert evidence.turns_file == "badcase_turns.csv"
    assert evidence.turn_count == 22
    assert evidence.form_issue_summary == "I am unable to receive messages on my account"

    representative = " ".join(evidence.representative_user_messages).lower()
    assert "thank you cortney" not in representative
    assert "[email]" != evidence.representative_user_messages[0].lower()
    assert "confused" in representative
    assert "login" in representative

    unresolved_labels = _labels(evidence.unresolved_user_signals)
    assert "explicit_unresolved" in unresolved_labels
    assert "confusion" in unresolved_labels
    assert "identifier_confusion" in unresolved_labels

    investigation_labels = _labels(evidence.human_investigation_signals)
    assert "investigation_needed" in investigation_labels
    assert "async_update" in investigation_labels
    assert "time_window" in investigation_labels

    identifier_labels = _labels(evidence.identifier_context_signals)
    assert {"email_context", "account_context", "platform_context"}.issubset(
        identifier_labels
    )

    assert evidence.user_requested_human is False
    assert evidence.transcript_indicated_outcome == "escalate"
    assert "unresolved" in evidence.reason
    assert "investigation" in evidence.reason


def test_clean_resolved_case_does_not_over_escalate() -> None:
    turns = [
        {
            "sequence": "0",
            "role": "visitor",
            "speaker": "[PRE_CHAT_FORM]",
            "message_redacted": (
                "[Form] Subject: Replies & Messaging | Description: "
                "I cannot receive messages"
            ),
        },
        {
            "sequence": "1",
            "role": "agent",
            "speaker": "Agent",
            "message_redacted": (
                "Could you confirm your email address and whether you use the app or site?"
            ),
        },
        {
            "sequence": "2",
            "role": "visitor",
            "speaker": "Customer",
            "message_redacted": "I'm using the app with [EMAIL]",
        },
        {
            "sequence": "3",
            "role": "agent",
            "speaker": "Agent",
            "message_redacted": (
                "I can confirm messages are enabled on your account. "
                "Please update the app and you should now receive replies."
            ),
        },
        {
            "sequence": "4",
            "role": "visitor",
            "speaker": "Customer",
            "message_redacted": "Great, that's all thanks",
        },
    ]

    evidence = extract_transcript_evidence(
        turns,
        turns_file="golden_turns.csv",
        source_dataset="golden",
    )

    assert evidence.transcript_indicated_outcome == "resolve"
    assert evidence.reason.startswith("agent transcript contains resolution")
    assert evidence.unresolved_user_signals == []
    assert evidence.human_investigation_signals == []
    assert evidence.handover_or_case_signals == []
    assert evidence.user_requested_human is False
    assert evidence.representative_user_messages == [
        "I'm using the app with [EMAIL]",
        "Great, that's all thanks",
    ]


def test_cs_interactive_004_account_login_guidance_resolves_without_async_false_positive() -> None:
    session_id = "570Q50000090OefIAE"
    turns_file = EVAL_DATASETS_DIR / "badcase_turns.csv"
    turns = _load_turns(turns_file, session_id)

    evidence = extract_transcript_evidence(
        turns,
        turns_file=turns_file,
        source_dataset="badcase",
    )

    assert evidence.transcript_indicated_outcome == "resolve"
    assert evidence.reason.startswith("agent transcript contains resolution")
    assert evidence.unresolved_user_signals == []
    assert evidence.human_investigation_signals == []
    assert evidence.handover_or_case_signals == []

    spec = _build_case_spec(
        _load_hr_row(session_id),
        turns,
        4,
        "badcase_turns.csv",
    )

    assert spec.case_id == "cs_interactive_004"
    assert spec.expected.primary_uc == "UC-D"
    assert spec.expected.outcome_class == "resolve"
    assert spec.expected.should_escalate is False
    assert spec.expected.escalation_trigger is None
    assert "request_handover" not in spec.expected.expected_tool_sequence


def test_user_human_request_is_an_escalation_signal() -> None:
    turns = [
        {
            "sequence": "0",
            "role": "visitor",
            "speaker": "[PRE_CHAT_FORM]",
            "message_redacted": (
                "[Form] Subject: Account Support | Description: I need account help"
            ),
        },
        {
            "sequence": "1",
            "role": "visitor",
            "speaker": "Customer",
            "message_redacted": "Can I speak to a human agent please?",
        },
    ]

    evidence = extract_transcript_evidence(
        turns,
        turns_file="escalation_turns.csv",
        source_dataset="escalation",
    )

    assert evidence.user_requested_human is True
    assert evidence.transcript_indicated_outcome == "escalate"
    assert "human" in evidence.reason
