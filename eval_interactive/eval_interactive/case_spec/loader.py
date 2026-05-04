"""YAML loader for CaseSpec files.

Loads CaseSpec definitions from individual YAML files or an entire directory.
"""

from __future__ import annotations

from pathlib import Path

import yaml

from .schema import (
    CaseSpec,
    Expected,
    FormContext,
    HiddenFact,
    Persona,
    ScoringConfig,
)


def _parse_case_spec(raw: dict) -> CaseSpec:
    """Parse a raw dict (from YAML) into a CaseSpec dataclass."""
    fc_raw = raw["form_context"]
    form_context = FormContext(
        first_name=fc_raw["first_name"],
        email=fc_raw["email"],
        topic_subject=fc_raw["topic_subject"],
        ad_id=fc_raw.get("ad_id", ""),
        description=fc_raw.get("description", ""),
    )

    p_raw = raw["persona"]
    hidden_facts = [
        HiddenFact(fact=hf["fact"], disclose_when=hf["disclose_when"])
        for hf in p_raw.get("hidden_facts", [])
    ]
    # Wave A3: prefer the new ``user_goal_summary`` field; fall back to the
    # pre-A1.1 ``goal_summary`` for any legacy yaml that still uses the old
    # name. Pass via the new constructor kwarg either way.
    if "user_goal_summary" in p_raw:
        user_goal_summary = p_raw["user_goal_summary"]
    elif "goal_summary" in p_raw:
        user_goal_summary = p_raw["goal_summary"]
    else:
        raise KeyError(
            "persona missing required field 'user_goal_summary' (or legacy 'goal_summary')"
        )
    persona = Persona(
        user_goal_summary=user_goal_summary,
        frustration_level=p_raw["frustration_level"],
        verbosity=p_raw["verbosity"],
        drift_behavior=p_raw["drift_behavior"],
        seed_messages=p_raw.get("seed_messages", []),
        hidden_facts=hidden_facts,
        will_request_human_if=p_raw.get("will_request_human_if", ""),
    )

    e_raw = raw["expected"]
    # Wave A3: ``allow_bot_resolution`` and ``bot_handling_pattern`` are
    # required fields on the new schema; legacy yaml will not have them, so
    # fall back to safe defaults. ``escalation_trigger`` is now an enum that
    # rejects empty strings -- normalise empty/missing to None.
    raw_trigger = e_raw.get("escalation_trigger", None)
    if isinstance(raw_trigger, str) and not raw_trigger.strip():
        raw_trigger = None
    expected = Expected(
        outcome_class=e_raw["outcome_class"],
        primary_uc=e_raw["primary_uc"],
        secondary_ucs=e_raw.get("secondary_ucs", []),
        should_escalate=e_raw["should_escalate"],
        allow_bot_resolution=e_raw.get("allow_bot_resolution", "false"),
        bot_handling_pattern=e_raw.get("bot_handling_pattern", "(legacy spec - bot_handling_pattern not specified)"),
        escalation_trigger=raw_trigger,
        risk_level=e_raw.get("risk_level", "low"),
        expected_tool_sequence=e_raw.get("expected_tool_sequence", []),
        forbidden_tools=e_raw.get("forbidden_tools", []),
        grounding_mode=e_raw.get("grounding_mode", "faq_source_backed"),
        answer_must_not_contain=e_raw.get("answer_must_not_contain", []),
        max_turns=int(e_raw.get("max_turns", 15)),
        acceptable_outcomes=list(e_raw.get("acceptable_outcomes", []) or []),
    )

    s_raw = raw["scoring"]
    scoring = ScoringConfig(
        hard_checks=s_raw.get("hard_checks", []),
        outcome_checks=s_raw.get("outcome_checks", []),
        llm_judge_dimensions=s_raw.get("llm_judge_dimensions", []),
    )

    return CaseSpec(
        case_id=raw["case_id"],
        source_session_id=raw["source_session_id"],
        source_dataset=raw["source_dataset"],
        form_context=form_context,
        persona=persona,
        expected=expected,
        scoring=scoring,
    )


def load_case_spec(path: str | Path) -> CaseSpec:
    """Load a single CaseSpec from a YAML file.

    Args:
        path: Path to a .yaml or .yml file.

    Returns:
        Parsed CaseSpec instance.

    Raises:
        FileNotFoundError: If the file does not exist.
        KeyError: If required fields are missing.
    """
    path = Path(path)
    if not path.exists():
        raise FileNotFoundError(f"CaseSpec file not found: {path}")

    with open(path, "r", encoding="utf-8") as f:
        raw = yaml.safe_load(f)

    return _parse_case_spec(raw)


def load_case_specs(directory: str | Path) -> list[CaseSpec]:
    """Load all CaseSpec YAML files from a directory.

    Args:
        directory: Path to a directory containing .yaml/.yml files.

    Returns:
        List of parsed CaseSpec instances, sorted by case_id.

    Raises:
        FileNotFoundError: If the directory does not exist.
    """
    directory = Path(directory)
    if not directory.exists():
        raise FileNotFoundError(f"CaseSpec directory not found: {directory}")
    if not directory.is_dir():
        raise NotADirectoryError(f"Expected a directory: {directory}")

    specs: list[CaseSpec] = []
    for yaml_file in sorted(directory.glob("*.yaml")):
        specs.append(load_case_spec(yaml_file))
    for yml_file in sorted(directory.glob("*.yml")):
        specs.append(load_case_spec(yml_file))

    specs.sort(key=lambda s: s.case_id)
    return specs
