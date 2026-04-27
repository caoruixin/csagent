"""CaseSpec extractor -- generates CaseSpec YAML from HR annotations + turn data.

Reads the human-review annotations CSV and per-dataset turn CSVs,
then produces one CaseSpec YAML per HR row and assigns each to a
case set directory (anchor / promotion / exploration).
"""

from __future__ import annotations

import csv
import json
import logging
import re
from collections import defaultdict
from dataclasses import asdict
from pathlib import Path
from typing import Any

import yaml

from .schema import (
    CaseSpec,
    Expected,
    FormContext,
    HiddenFact,
    Persona,
    ScoringConfig,
)

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# UC classification helpers
# ---------------------------------------------------------------------------

_FAQ_UCS = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"}
_INTAKE_UCS = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}
_FIXED_SCRIPT_UCS = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}


def _is_faq_uc(uc: str) -> bool:
    return uc in _FAQ_UCS


def _is_intake_uc(uc: str) -> bool:
    return uc in _INTAKE_UCS


# ---------------------------------------------------------------------------
# Parsing helpers
# ---------------------------------------------------------------------------

def _parse_bool(value: str) -> bool:
    """Parse a boolean string from CSV (true/false/empty)."""
    return value.strip().lower() in ("true", "1", "yes")


def _parse_json_list(value: str) -> list:
    """Parse a JSON array string, returning [] on failure."""
    value = value.strip()
    if not value:
        return []
    try:
        parsed = json.loads(value)
        if isinstance(parsed, list):
            return parsed
        return []
    except (json.JSONDecodeError, TypeError):
        return []


def _parse_secondary_ucs(value: str) -> list[str]:
    """Parse secondary UCs from pipe-separated or JSON array format."""
    value = value.strip()
    if not value:
        return []
    # Try JSON array first: ["UC-D", "UC-K"]
    try:
        parsed = json.loads(value)
        if isinstance(parsed, list):
            return [str(item).strip() for item in parsed if str(item).strip()]
        return []
    except (json.JSONDecodeError, TypeError):
        pass
    # Try pipe-separated: UC-D|UC-K
    if "|" in value:
        return [s.strip() for s in value.split("|") if s.strip()]
    # Single value
    return [value] if value else []


def _parse_tool_names(tool_sequence_json: str) -> list[str]:
    """Extract just tool names from the expected_tool_sequence JSON."""
    items = _parse_json_list(tool_sequence_json)
    names: list[str] = []
    for item in items:
        if isinstance(item, dict) and "tool" in item:
            names.append(item["tool"])
        elif isinstance(item, str):
            names.append(item)
    return names


def _parse_forbidden_tool_names(forbidden_json: str) -> list[str]:
    """Extract just tool names from the forbidden_tools JSON."""
    items = _parse_json_list(forbidden_json)
    names: list[str] = []
    for item in items:
        if isinstance(item, dict) and "tool" in item:
            names.append(item["tool"])
        elif isinstance(item, str):
            names.append(item)
    return names


def _parse_answer_must_not_contain(value: str) -> list[str]:
    """Parse the answer_must_not_contain field (JSON array of strings)."""
    items = _parse_json_list(value)
    return [str(s) for s in items if s]


# ---------------------------------------------------------------------------
# Form context parsing
# ---------------------------------------------------------------------------

_FORM_PATTERN = re.compile(
    r"\[Form\]\s*Subject:\s*(?P<subject>.+?)\s*\|\s*Description:\s*(?P<description>.*)",
    re.IGNORECASE,
)


def _parse_form_message(message: str) -> dict[str, str]:
    """Parse the pre-chat form message into subject and description."""
    m = _FORM_PATTERN.match(message.strip())
    if m:
        return {
            "subject": m.group("subject").strip(),
            "description": m.group("description").strip(),
        }
    # Fallback: try without [Form] prefix
    fallback = re.match(
        r"Subject:\s*(?P<subject>.+?)\s*\|\s*Description:\s*(?P<description>.*)",
        message.strip(),
        re.IGNORECASE,
    )
    if fallback:
        return {
            "subject": fallback.group("subject").strip(),
            "description": fallback.group("description").strip(),
        }
    # Last resort
    return {"subject": message.strip(), "description": ""}


# ---------------------------------------------------------------------------
# Turns loading
# ---------------------------------------------------------------------------

def _load_all_turns(turns_dir: Path) -> dict[str, list[dict]]:
    """Load all turns CSVs and index by conversation_id (session_id).

    Returns:
        Mapping from conversation_id to a sorted list of turn dicts.
    """
    index: dict[str, list[dict]] = defaultdict(list)
    for csv_path in sorted(turns_dir.glob("*_turns.csv")):
        with open(csv_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                cid = row.get("conversation_id", "").strip()
                if cid:
                    index[cid].append(row)
    # Sort each conversation's turns by sequence
    for cid in index:
        index[cid].sort(key=lambda r: int(r.get("sequence", 0)))
    return dict(index)


def _get_form_turn(turns: list[dict]) -> dict | None:
    """Return the sequence=0 PRE_CHAT_FORM turn, or None."""
    for t in turns:
        if (
            t.get("sequence", "") == "0"
            and t.get("speaker", "") == "[PRE_CHAT_FORM]"
        ):
            return t
    return None


def _get_visitor_turns(turns: list[dict]) -> list[dict]:
    """Return visitor turns with sequence > 0, sorted by sequence."""
    return [
        t
        for t in turns
        if int(t.get("sequence", 0)) > 0 and t.get("role", "") == "visitor"
    ]


def _get_visitor_name(turns: list[dict]) -> str:
    """Extract the visitor's first name from the first visitor turn."""
    for t in turns:
        if (
            int(t.get("sequence", 0)) > 0
            and t.get("role", "") == "visitor"
            and t.get("speaker", "")
            and t["speaker"] != "[PRE_CHAT_FORM]"
        ):
            return t["speaker"].strip()
    return "Customer"


# ---------------------------------------------------------------------------
# Persona derivation
# ---------------------------------------------------------------------------

def _derive_frustration_level(has_frustration: str, frustration_type: str) -> str:
    """Map HR frustration fields to persona frustration level."""
    if not _parse_bool(has_frustration):
        return "none"
    ft = frustration_type.lower()
    if any(kw in ft for kw in ("litigation", "threat", "abusive", "legal")):
        return "high"
    if any(kw in ft for kw in ("general", "impatient", "wait", "confusion")):
        return "mild"
    # Default non-none frustration to mild
    return "mild" if ft else "none"


def _derive_drift_behavior(drift_type: str) -> str:
    """Normalise drift_type to persona drift_behavior values."""
    dt = drift_type.strip().lower()
    mapping = {
        "none": "none",
        "minor_drift": "minor",
        "minor": "minor",
        "soft_shift": "soft_shift",
        "hard_shift": "hard_shift",
    }
    return mapping.get(dt, "none")


def _derive_verbosity(seed_messages: list[str]) -> str:
    """Infer verbosity from seed message lengths."""
    if not seed_messages:
        return "normal"
    avg_len = sum(len(m) for m in seed_messages) / len(seed_messages)
    if avg_len < 20:
        return "terse"
    if avg_len > 100:
        return "verbose"
    return "normal"


def _derive_goal_summary(
    topic_subject: str,
    outcome_class: str,
    primary_uc: str,
    description: str,
) -> str:
    """Generate a goal summary from available fields."""
    # Start with the topic
    if description:
        goal = f"Get help with: {description}."
    else:
        goal = f"Get help with {topic_subject} issue."

    # Add outcome expectation
    if outcome_class == "escalate":
        goal += " Expects bot to escalate to human agent."
    else:
        goal += " Expects bot to resolve the issue directly."

    return goal


def _derive_hidden_facts(
    form_provides_email: str,
    form_provides_ad_id: str,
    ad_id: str,
    email: str,
) -> list[dict[str, str]]:
    """Build hidden facts from form-provided identifiers."""
    facts: list[dict[str, str]] = []
    if _parse_bool(form_provides_email) and email:
        facts.append({
            "fact": f"email is {email}",
            "disclose_when": "if asked",
        })
    if _parse_bool(form_provides_ad_id) and ad_id:
        facts.append({
            "fact": f"ad ID is {ad_id}",
            "disclose_when": "if asked",
        })
    return facts


def _derive_will_request_human_if(
    has_frustration: str,
    escalation_trigger: str,
) -> str:
    """Determine when the simulated user will request a human."""
    if _parse_bool(has_frustration) and escalation_trigger.strip().lower() == "user_requested":
        return "bot cannot resolve after 2 attempts"
    return ""


# ---------------------------------------------------------------------------
# Scoring config derivation
# ---------------------------------------------------------------------------

def _derive_scoring_config(primary_uc: str, should_escalate: bool) -> ScoringConfig:
    """Build standard ScoringConfig based on UC type."""
    hard_checks = ["no_forbidden_tools", "budget_enforcement"]
    outcome_checks = ["correct_uc", "correct_outcome"]
    llm_judge_dimensions = ["groundedness", "relevance", "tone_appropriateness"]

    if should_escalate:
        outcome_checks.append("escalation_triggered")
    else:
        outcome_checks.append("resolution_achieved")

    if _is_intake_uc(primary_uc):
        hard_checks.append("fixed_script_adherence")
        outcome_checks.append("intake_fields_collected")
    else:
        hard_checks.append("grounding_compliance")
        outcome_checks.append("answer_accuracy")

    return ScoringConfig(
        hard_checks=hard_checks,
        outcome_checks=outcome_checks,
        llm_judge_dimensions=llm_judge_dimensions,
    )


# ---------------------------------------------------------------------------
# Grounding mode
# ---------------------------------------------------------------------------

def _derive_grounding_mode(grounding_required: str, primary_uc: str) -> str:
    """Determine grounding mode from HR fields."""
    if primary_uc in _FIXED_SCRIPT_UCS:
        return "fixed_script_only"
    if _parse_bool(grounding_required):
        return "faq_source_backed"
    return "faq_source_backed"


# ---------------------------------------------------------------------------
# Case set assignment
# ---------------------------------------------------------------------------

def _assign_case_set(
    uc_confidence: str,
    outcome_class: str,
    has_frustration: str,
    drift_type: str,
    risk_level: str,
) -> str:
    """Assign a case to anchor / promotion / exploration set.

    Tiered heuristic designed for datasets where frustration and drift
    are prevalent.  We score each case and use score thresholds:

    - anchor  (score 0-1): high confidence, low risk, at most soft_shift
    - promotion (score 2-3): medium complexity
    - exploration (score >= 4): hard edge cases
    """
    conf = uc_confidence.strip().lower()
    drift = drift_type.strip().lower()
    risk = risk_level.strip().lower()

    score = 0

    # Risk contribution
    if risk in ("critical",):
        score += 4
    elif risk in ("high",):
        score += 3
    elif risk in ("medium",):
        score += 1

    # Drift contribution
    if drift == "hard_shift":
        score += 2
    elif drift == "soft_shift":
        score += 1
    elif drift == "minor_drift":
        score += 0  # minor drift is low complexity

    # Confidence penalty (low confidence = harder)
    if conf == "medium":
        score += 1

    if score >= 4:
        return "exploration"
    if score >= 2:
        return "promotion"
    return "anchor"


# ---------------------------------------------------------------------------
# YAML serialisation
# ---------------------------------------------------------------------------

def _casespec_to_dict(spec: CaseSpec) -> dict[str, Any]:
    """Convert a CaseSpec to an ordered dict suitable for YAML output."""
    d: dict[str, Any] = {}
    d["case_id"] = spec.case_id
    d["source_session_id"] = spec.source_session_id
    d["source_dataset"] = spec.source_dataset
    d["form_context"] = {
        "first_name": spec.form_context.first_name,
        "email": spec.form_context.email,
        "topic_subject": spec.form_context.topic_subject,
        "ad_id": spec.form_context.ad_id,
        "description": spec.form_context.description,
    }
    d["persona"] = {
        "goal_summary": spec.persona.goal_summary,
        "frustration_level": spec.persona.frustration_level,
        "verbosity": spec.persona.verbosity,
        "drift_behavior": spec.persona.drift_behavior,
        "seed_messages": spec.persona.seed_messages,
        "hidden_facts": [
            {"fact": hf.fact, "disclose_when": hf.disclose_when}
            for hf in spec.persona.hidden_facts
        ],
        "will_request_human_if": spec.persona.will_request_human_if,
    }
    d["expected"] = {
        "outcome_class": spec.expected.outcome_class,
        "primary_uc": spec.expected.primary_uc,
        "secondary_ucs": spec.expected.secondary_ucs,
        "should_escalate": spec.expected.should_escalate,
        "escalation_trigger": spec.expected.escalation_trigger,
        "risk_level": spec.expected.risk_level,
        "expected_tool_sequence": spec.expected.expected_tool_sequence,
        "forbidden_tools": spec.expected.forbidden_tools,
        "grounding_mode": spec.expected.grounding_mode,
        "answer_must_not_contain": spec.expected.answer_must_not_contain,
        "max_turns": spec.expected.max_turns,
    }
    d["scoring"] = {
        "hard_checks": spec.scoring.hard_checks,
        "outcome_checks": spec.scoring.outcome_checks,
        "llm_judge_dimensions": spec.scoring.llm_judge_dimensions,
    }
    return d


def _write_yaml(spec: CaseSpec, output_path: Path) -> None:
    """Write a single CaseSpec to a YAML file."""
    output_path.parent.mkdir(parents=True, exist_ok=True)
    data = _casespec_to_dict(spec)
    with open(output_path, "w", encoding="utf-8") as f:
        yaml.dump(
            data,
            f,
            default_flow_style=False,
            allow_unicode=True,
            sort_keys=False,
            width=120,
        )


# ---------------------------------------------------------------------------
# Main extraction
# ---------------------------------------------------------------------------

def _build_case_spec(
    row: dict[str, str],
    turns: list[dict],
    case_index: int,
) -> CaseSpec:
    """Build a single CaseSpec from an HR row and its matching turns."""
    session_id = row["session_id"].strip()
    source_dataset = row.get("source_dataset", "unknown").strip()

    # --- Primary UC ---
    primary_uc_corrected = row.get("primary_uc_corrected", "").strip()
    source_primary_uc = row.get("source_primary_uc", "").strip()
    primary_uc = primary_uc_corrected if primary_uc_corrected else source_primary_uc

    # --- Form context ---
    form_turn = _get_form_turn(turns)
    if form_turn:
        form_data = _parse_form_message(form_turn.get("message_redacted", ""))
    else:
        form_data = {
            "subject": row.get("form_topic_subject", ""),
            "description": "",
        }

    # Use HR form_topic_subject as authoritative topic_subject
    topic_subject = row.get("form_topic_subject", "").strip()
    if not topic_subject:
        topic_subject = form_data.get("subject", "")
    description = form_data.get("description", "")

    # Visitor name from turns
    first_name = _get_visitor_name(turns)

    # Email / ad_id: these are indicated as available via form but redacted in turns
    # We use placeholders since actual values are [EMAIL] / [AD_ID] in transcripts
    email = "customer@example.com" if _parse_bool(row.get("form_provides_email", "")) else ""
    ad_id = "REDACTED_AD_ID" if _parse_bool(row.get("form_provides_ad_id", "")) else ""

    form_context = FormContext(
        first_name=first_name,
        email=email,
        topic_subject=topic_subject,
        ad_id=ad_id,
        description=description,
    )

    # --- Seed messages (first 1-3 unique visitor turns) ---
    visitor_turns = _get_visitor_turns(turns)
    seed_messages: list[str] = []
    seen_msgs: set[str] = set()
    for t in visitor_turns:
        msg = t.get("message_redacted", "").strip()
        if msg and msg not in seen_msgs:
            seed_messages.append(msg)
            seen_msgs.add(msg)
            if len(seed_messages) >= 3:
                break
    if not seed_messages:
        # Fallback: use description from form
        if description:
            seed_messages = [description]
        else:
            seed_messages = [topic_subject]

    # --- Persona ---
    frustration_level = _derive_frustration_level(
        row.get("has_frustration", ""),
        row.get("frustration_type", ""),
    )
    drift_behavior = _derive_drift_behavior(row.get("drift_type", ""))
    verbosity = _derive_verbosity(seed_messages)
    goal_summary = _derive_goal_summary(
        topic_subject,
        row.get("outcome_class", "").strip(),
        primary_uc,
        description,
    )
    hidden_facts_raw = _derive_hidden_facts(
        row.get("form_provides_email", ""),
        row.get("form_provides_ad_id", ""),
        ad_id,
        email,
    )
    hidden_facts = [
        HiddenFact(fact=hf["fact"], disclose_when=hf["disclose_when"])
        for hf in hidden_facts_raw
    ]
    will_request_human_if = _derive_will_request_human_if(
        row.get("has_frustration", ""),
        row.get("escalation_trigger", ""),
    )

    persona = Persona(
        goal_summary=goal_summary,
        frustration_level=frustration_level,
        verbosity=verbosity,
        drift_behavior=drift_behavior,
        seed_messages=seed_messages,
        hidden_facts=hidden_facts,
        will_request_human_if=will_request_human_if,
    )

    # --- Expected ---
    outcome_class = row.get("outcome_class", "resolve").strip()
    should_escalate = _parse_bool(row.get("should_escalate", ""))
    escalation_trigger = row.get("escalation_trigger", "").strip()
    risk_level = row.get("risk_level", "low").strip() or "low"
    secondary_ucs = _parse_secondary_ucs(row.get("secondary_ucs", ""))
    expected_tool_sequence = _parse_tool_names(row.get("expected_tool_sequence", ""))
    forbidden_tools = _parse_forbidden_tool_names(row.get("forbidden_tools", ""))
    grounding_mode = _derive_grounding_mode(
        row.get("grounding_required", ""),
        primary_uc,
    )
    answer_must_not_contain = _parse_answer_must_not_contain(
        row.get("answer_must_not_contain", "")
    )
    max_turns = 10 if _is_intake_uc(primary_uc) else 15

    expected = Expected(
        outcome_class=outcome_class,
        primary_uc=primary_uc,
        secondary_ucs=secondary_ucs,
        should_escalate=should_escalate,
        escalation_trigger=escalation_trigger,
        risk_level=risk_level,
        expected_tool_sequence=expected_tool_sequence,
        forbidden_tools=forbidden_tools,
        grounding_mode=grounding_mode,
        answer_must_not_contain=answer_must_not_contain,
        max_turns=max_turns,
    )

    # --- Scoring ---
    scoring = _derive_scoring_config(primary_uc, should_escalate)

    # --- Case ID ---
    case_id = f"cs_interactive_{case_index:03d}"

    return CaseSpec(
        case_id=case_id,
        source_session_id=session_id,
        source_dataset=source_dataset,
        form_context=form_context,
        persona=persona,
        expected=expected,
        scoring=scoring,
    )


def extract_case_specs(
    hr_csv_path: str | Path,
    turns_dir: str | Path,
    output_dir: str | Path,
) -> list[CaseSpec]:
    """Extract CaseSpecs from HR annotation CSV and raw turn data.

    Reads every row from the HR CSV, matches it to the corresponding
    conversation turns, builds a CaseSpec, assigns it to a case set
    (anchor / promotion / exploration), and writes one YAML file per case
    into the appropriate subdirectory.

    Args:
        hr_csv_path: Path to the HR annotations CSV file.
        turns_dir: Path to the eval_datasets directory with turn CSVs.
        output_dir: Directory where generated CaseSpec YAMLs are written.
                    Subdirectories anchor/, promotion/, exploration/ are
                    created automatically.

    Returns:
        List of generated CaseSpec instances.
    """
    hr_csv_path = Path(hr_csv_path)
    turns_dir = Path(turns_dir)
    output_dir = Path(output_dir)

    if not hr_csv_path.exists():
        raise FileNotFoundError(f"HR CSV not found: {hr_csv_path}")
    if not turns_dir.exists():
        raise FileNotFoundError(f"Turns directory not found: {turns_dir}")

    # Load all turns indexed by conversation_id
    logger.info("Loading turns from %s ...", turns_dir)
    all_turns = _load_all_turns(turns_dir)
    logger.info("Loaded turns for %d conversations.", len(all_turns))

    # Read HR CSV
    logger.info("Reading HR annotations from %s ...", hr_csv_path)
    with open(hr_csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        hr_rows = list(reader)
    logger.info("Read %d HR rows.", len(hr_rows))

    # Deduplicate by session_id (keep first occurrence)
    seen_sessions: set[str] = set()
    unique_rows: list[dict[str, str]] = []
    for row in hr_rows:
        sid = row.get("session_id", "").strip()
        if sid and sid not in seen_sessions:
            seen_sessions.add(sid)
            unique_rows.append(row)
        elif sid in seen_sessions:
            logger.warning("Duplicate session_id %s -- skipping.", sid)

    # Build CaseSpecs
    specs: list[CaseSpec] = []
    set_counts: dict[str, int] = defaultdict(int)
    skipped = 0

    for idx, row in enumerate(unique_rows, start=1):
        session_id = row.get("session_id", "").strip()

        # Find matching turns
        turns = all_turns.get(session_id)
        if not turns:
            logger.warning(
                "No matching turns for session_id=%s (case_id=%s) -- skipping.",
                session_id,
                row.get("case_id", ""),
            )
            skipped += 1
            continue

        spec = _build_case_spec(row, turns, idx)

        # Assign to case set
        case_set = _assign_case_set(
            row.get("uc_confidence", ""),
            row.get("outcome_class", ""),
            row.get("has_frustration", ""),
            row.get("drift_type", ""),
            row.get("risk_level", ""),
        )
        set_counts[case_set] += 1

        # Write YAML
        yaml_filename = f"{spec.case_id}.yaml"
        yaml_path = output_dir / case_set / yaml_filename
        _write_yaml(spec, yaml_path)

        specs.append(spec)

    logger.info(
        "Extraction complete: %d specs generated, %d skipped. Sets: %s",
        len(specs),
        skipped,
        dict(set_counts),
    )

    return specs
