"""Basic tests for CaseSpec schema and YAML serialization/deserialization."""

from __future__ import annotations

import tempfile
from pathlib import Path

import pytest
import yaml

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    HiddenFact,
    Persona,
    ScoringConfig,
)
from eval_interactive.case_spec.loader import load_case_spec, load_case_specs


def _make_sample_case_spec() -> CaseSpec:
    """Create a sample CaseSpec for testing."""
    return CaseSpec(
        case_id="test-001",
        source_session_id="sess-abc-123",
        source_dataset="eval_v1",
        form_context=FormContext(
            first_name="Alice",
            email="alice@example.com",
            topic_subject="Billing issue",
            ad_id="AD-001",
            description="I was charged twice for my subscription.",
        ),
        persona=Persona(
            user_goal_summary="Resolve a double-charge billing issue",
            frustration_level="mild",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["Hi, I was charged twice for my subscription."],
            hidden_facts=[
                HiddenFact(
                    fact="The charge appeared on both Visa and Mastercard",
                    disclose_when="if asked",
                ),
            ],
            will_request_human_if="bot cannot locate the duplicate charge",
        ),
        expected=Expected(
            outcome_class="resolve",
            primary_uc="billing_inquiry",
            secondary_ucs=["refund_request"],
            should_escalate=False,
            allow_bot_resolution="true",
            bot_handling_pattern="<test fixture>",
            escalation_trigger=None,
            risk_level="medium",
            expected_tool_sequence=["search_faq", "lookup_order"],
            forbidden_tools=["delete_account"],
            grounding_mode="faq_source_backed",
            answer_must_not_contain=["cancel your account"],
            max_turns=10,
        ),
        scoring=ScoringConfig(
            hard_checks=["correct_uc_detected", "no_forbidden_tools"],
            outcome_checks=["containment_match", "turn_budget"],
            llm_judge_dimensions=["empathy", "clarity", "accuracy"],
        ),
    )


def _case_spec_to_dict(spec: CaseSpec) -> dict:
    """Convert a CaseSpec to a plain dict suitable for YAML serialization."""
    return {
        "case_id": spec.case_id,
        "source_session_id": spec.source_session_id,
        "source_dataset": spec.source_dataset,
        "form_context": {
            "first_name": spec.form_context.first_name,
            "email": spec.form_context.email,
            "topic_subject": spec.form_context.topic_subject,
            "ad_id": spec.form_context.ad_id,
            "description": spec.form_context.description,
        },
        "persona": {
            "user_goal_summary": spec.persona.user_goal_summary,
            "frustration_level": spec.persona.frustration_level,
            "verbosity": spec.persona.verbosity,
            "drift_behavior": spec.persona.drift_behavior,
            "seed_messages": spec.persona.seed_messages,
            "hidden_facts": [
                {"fact": hf.fact, "disclose_when": hf.disclose_when}
                for hf in spec.persona.hidden_facts
            ],
            "will_request_human_if": spec.persona.will_request_human_if,
        },
        "expected": {
            "outcome_class": spec.expected.outcome_class,
            "primary_uc": spec.expected.primary_uc,
            "secondary_ucs": spec.expected.secondary_ucs,
            "should_escalate": spec.expected.should_escalate,
            "allow_bot_resolution": spec.expected.allow_bot_resolution,
            "bot_handling_pattern": spec.expected.bot_handling_pattern,
            "escalation_trigger": spec.expected.escalation_trigger,
            "risk_level": spec.expected.risk_level,
            "expected_tool_sequence": spec.expected.expected_tool_sequence,
            "forbidden_tools": spec.expected.forbidden_tools,
            "grounding_mode": spec.expected.grounding_mode,
            "answer_must_not_contain": spec.expected.answer_must_not_contain,
            "max_turns": spec.expected.max_turns,
        },
        "scoring": {
            "hard_checks": spec.scoring.hard_checks,
            "outcome_checks": spec.scoring.outcome_checks,
            "llm_judge_dimensions": spec.scoring.llm_judge_dimensions,
        },
    }


class TestCaseSpecCreation:
    """Test that CaseSpec dataclasses can be created correctly."""

    def test_create_full_case_spec(self):
        spec = _make_sample_case_spec()
        assert spec.case_id == "test-001"
        assert spec.form_context.first_name == "Alice"
        assert spec.persona.frustration_level == "mild"
        assert spec.expected.outcome_class == "resolve"
        assert len(spec.scoring.hard_checks) == 2

    def test_form_context_defaults(self):
        fc = FormContext(
            first_name="Bob",
            email="bob@example.com",
            topic_subject="General question",
        )
        assert fc.ad_id == ""
        assert fc.description == ""

    def test_expected_defaults(self):
        exp = Expected(
            outcome_class="escalate",
            primary_uc="complaint",
            secondary_ucs=[],
            should_escalate=True,
            allow_bot_resolution="true",
            bot_handling_pattern="<test fixture>",
            escalation_trigger="intake_complete_for_uc_k",
        )
        assert exp.risk_level == "low"
        assert exp.max_turns == 15
        assert exp.expected_tool_sequence == []
        assert exp.forbidden_tools == []
        assert exp.grounding_mode == "faq_source_backed"
        assert exp.answer_must_not_contain == []

    def test_hidden_fact(self):
        hf = HiddenFact(fact="secret info", disclose_when="if asked")
        assert hf.fact == "secret info"
        assert hf.disclose_when == "if asked"

    def test_persona_with_no_optional_fields(self):
        persona = Persona(
            user_goal_summary="Ask about pricing",
            frustration_level="none",
            verbosity="terse",
            drift_behavior="none",
            seed_messages=["How much does it cost?"],
            hidden_facts=[],
        )
        assert persona.will_request_human_if == ""


class TestCaseSpecYAML:
    """Test YAML round-trip serialization/deserialization."""

    def test_yaml_round_trip_single_file(self, tmp_path: Path):
        spec = _make_sample_case_spec()
        data = _case_spec_to_dict(spec)

        yaml_file = tmp_path / "test-001.yaml"
        with open(yaml_file, "w") as f:
            yaml.dump(data, f, default_flow_style=False)

        loaded = load_case_spec(yaml_file)

        assert loaded.case_id == spec.case_id
        assert loaded.source_session_id == spec.source_session_id
        assert loaded.form_context.first_name == spec.form_context.first_name
        assert loaded.form_context.email == spec.form_context.email
        assert loaded.persona.goal_summary == spec.persona.goal_summary
        assert loaded.persona.frustration_level == spec.persona.frustration_level
        assert len(loaded.persona.hidden_facts) == len(spec.persona.hidden_facts)
        assert loaded.persona.hidden_facts[0].fact == spec.persona.hidden_facts[0].fact
        assert loaded.expected.outcome_class == spec.expected.outcome_class
        assert loaded.expected.primary_uc == spec.expected.primary_uc
        assert loaded.expected.should_escalate == spec.expected.should_escalate
        assert loaded.expected.max_turns == spec.expected.max_turns
        assert loaded.scoring.hard_checks == spec.scoring.hard_checks

    def test_load_directory(self, tmp_path: Path):
        for i in range(3):
            spec = _make_sample_case_spec()
            spec_dict = _case_spec_to_dict(spec)
            spec_dict["case_id"] = f"test-{i:03d}"
            yaml_file = tmp_path / f"case-{i:03d}.yaml"
            with open(yaml_file, "w") as f:
                yaml.dump(spec_dict, f, default_flow_style=False)

        specs = load_case_specs(tmp_path)
        assert len(specs) == 3
        assert specs[0].case_id == "test-000"
        assert specs[1].case_id == "test-001"
        assert specs[2].case_id == "test-002"

    def test_load_nonexistent_file_raises(self):
        with pytest.raises(FileNotFoundError):
            load_case_spec("/nonexistent/path.yaml")

    def test_load_nonexistent_directory_raises(self):
        with pytest.raises(FileNotFoundError):
            load_case_specs("/nonexistent/dir/")

    def test_load_empty_directory(self, tmp_path: Path):
        specs = load_case_specs(tmp_path)
        assert specs == []
