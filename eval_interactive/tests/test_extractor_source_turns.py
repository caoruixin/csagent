"""Tests for CaseSpec extractor source-dataset turn provenance."""

from __future__ import annotations

import csv
from pathlib import Path

import yaml

from eval_interactive.case_spec.extractor import extract_case_specs


HR_FIELDS = [
    "session_id",
    "source_dataset",
    "case_id",
    "form_topic_subject",
    "source_primary_uc",
    "source_all_ucs",
    "primary_uc_corrected",
    "secondary_ucs",
    "uc_confidence",
    "topic_uc_routing_correct",
    "outcome_class",
    "outcome_reasoning",
    "form_provides_email",
    "form_provides_ad_id",
    "form_topic_matches_uc",
    "bot_can_skip_identifier_ask",
    "expected_tool_sequence",
    "forbidden_tools",
    "should_escalate",
    "escalation_trigger",
    "escalation_turn",
    "grounding_required",
    "grounding_source",
    "expected_knowledge_scope",
    "answer_must_not_contain",
    "drift_type",
    "drift_handling",
    "has_frustration",
    "frustration_type",
    "risk_level",
    "guardrail_violations_in_human_transcript",
    "quality_notes",
    "transcript_summary",
]

TURN_FIELDS = ["conversation_id", "sequence", "role", "speaker", "message_redacted"]


def _write_csv(path: Path, fields: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def test_extract_case_specs_uses_only_source_dataset_turn_file(tmp_path: Path) -> None:
    """A duplicated conversation_id in another dataset must not bleed in."""
    session_id = "dup-session-001"
    hr_csv = tmp_path / "hr.csv"
    turns_dir = tmp_path / "turns"
    output_dir = tmp_path / "case_specs"

    row = {field: "" for field in HR_FIELDS}
    row.update(
        {
            "session_id": session_id,
            "source_dataset": "badcase",
            "case_id": "500-test",
            "form_topic_subject": "Replies & Messaging",
            "source_primary_uc": "UC-C",
            "primary_uc_corrected": "UC-C",
            "secondary_ucs": "[]",
            "outcome_class": "resolve",
            "should_escalate": "false",
            "form_provides_email": "true",
            "form_provides_ad_id": "false",
            "drift_type": "none",
            "has_frustration": "false",
            "risk_level": "low",
        }
    )
    _write_csv(hr_csv, HR_FIELDS, [row])

    _write_csv(
        turns_dir / "badcase_turns.csv",
        TURN_FIELDS,
        [
            {
                "conversation_id": session_id,
                "sequence": "0",
                "role": "visitor",
                "speaker": "[PRE_CHAT_FORM]",
                "message_redacted": "[Form] Subject: Replies & Messaging | Description: BADCASE description",
            },
            {
                "conversation_id": session_id,
                "sequence": "1",
                "role": "visitor",
                "speaker": "Badcase Name",
                "message_redacted": "badcase visitor message",
            },
        ],
    )
    _write_csv(
        turns_dir / "escalation_turns.csv",
        TURN_FIELDS,
        [
            {
                "conversation_id": session_id,
                "sequence": "0",
                "role": "visitor",
                "speaker": "[PRE_CHAT_FORM]",
                "message_redacted": "[Form] Subject: Replies & Messaging | Description: ESCALATION description",
            },
            {
                "conversation_id": session_id,
                "sequence": "1",
                "role": "visitor",
                "speaker": "Escalation Name",
                "message_redacted": "escalation visitor message",
            },
        ],
    )

    specs = extract_case_specs(hr_csv, turns_dir, output_dir)

    assert len(specs) == 1
    spec = specs[0]
    assert spec.form_context.description == "BADCASE description"
    assert spec.form_context.first_name == "Badcase Name"
    assert spec.persona.seed_messages == ["badcase visitor message"]

    generated = yaml.safe_load(
        (output_dir / "anchor" / "cs_interactive_001.yaml").read_text(encoding="utf-8")
    )
    assert generated["form_context"]["description"] == "BADCASE description"
    assert generated["persona"]["seed_messages"] == ["badcase visitor message"]


def test_extract_case_specs_uses_transcript_evidence_for_outcome_and_seeds(
    tmp_path: Path,
) -> None:
    """Evidence from selected turns can override a shallow HR resolve hint."""
    session_id = "evidence-session-001"
    hr_csv = tmp_path / "hr.csv"
    turns_dir = tmp_path / "turns"
    output_dir = tmp_path / "case_specs"

    row = {field: "" for field in HR_FIELDS}
    row.update(
        {
            "session_id": session_id,
            "source_dataset": "badcase",
            "case_id": "500-evidence",
            "form_topic_subject": "Replies & Messaging",
            "source_primary_uc": "UC-C",
            "primary_uc_corrected": "UC-C",
            "secondary_ucs": "[]",
            "outcome_class": "resolve",
            "should_escalate": "false",
            "form_provides_email": "false",
            "form_provides_ad_id": "false",
            "drift_type": "none",
            "has_frustration": "false",
            "risk_level": "low",
        }
    )
    _write_csv(hr_csv, HR_FIELDS, [row])

    _write_csv(
        turns_dir / "badcase_turns.csv",
        TURN_FIELDS,
        [
            {
                "conversation_id": session_id,
                "sequence": "0",
                "role": "visitor",
                "speaker": "[PRE_CHAT_FORM]",
                "message_redacted": (
                    "[Form] Subject: Replies & Messaging | Description: "
                    "I am unable to receive messages"
                ),
            },
            {
                "conversation_id": session_id,
                "sequence": "1",
                "role": "visitor",
                "speaker": "Customer",
                "message_redacted": (
                    "I still can't receive messages and I'm confused which "
                    "login email is correct"
                ),
            },
            {
                "conversation_id": session_id,
                "sequence": "2",
                "role": "agent",
                "speaker": "Agent",
                "message_redacted": (
                    "I need to investigate this with the technical team and "
                    "will get back to you within 48 hours."
                ),
            },
        ],
    )

    specs = extract_case_specs(hr_csv, turns_dir, output_dir)

    assert len(specs) == 1
    expected = specs[0].expected
    assert expected.outcome_class == "escalate"
    assert expected.should_escalate is True
    assert expected.escalation_trigger == "faq_miss_threshold_exceeded"
    assert "request_handover" in expected.expected_tool_sequence
    assert specs[0].persona.seed_messages == [
        "I still can't receive messages and I'm confused which login email is correct"
    ]

    generated = yaml.safe_load(
        (output_dir / "anchor" / "cs_interactive_001.yaml").read_text(encoding="utf-8")
    )
    assert generated["expected"]["outcome_class"] == "escalate"
    assert "request_handover" in generated["expected"]["expected_tool_sequence"]
    assert generated["persona"]["seed_messages"] == specs[0].persona.seed_messages
