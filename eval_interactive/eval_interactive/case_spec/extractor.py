"""CaseSpec extractor -- generates CaseSpec YAML from HR annotations + turn data.

Reads the human-review annotations CSV and per-dataset turn CSVs,
then produces one CaseSpec YAML per HR row and assigns each to a
case set directory (anchor / promotion / exploration).

Wave A2.1 (2026-04-27) refactor
-------------------------------
The HR CSV is now treated as a HINT, not the source of truth, for any
field whose owner is the per-UC policy (phase2 §2.2 / §2.6 / §2.10).
Concretely:

* UC classification is corrected via ``UC_B_RECLASSIFICATION_OVERRIDES``
  before any UC-derived value is computed.
* ``allow_bot_resolution``, ``expected_tool_sequence``,
  ``forbidden_tools``, ``escalation_trigger`` (default),
  ``grounding_mode`` and ``bot_handling_pattern`` are all driven from
  ``policy_table.get_policy(primary_uc)``.
* Human-only tools (``policy_table.list_human_only_tools()``) are
  stripped from per-case ``forbidden_tools`` -- they are blocked by a
  global L1 check, not per-case.
* ``persona.user_goal_summary`` is built from form_context + drift_type
  (neutral, factual). The HR free-text reasoning fields are no longer
  copied verbatim into persona.
* Hidden facts already present in ``form_context`` (email / phone) are
  dropped from ``persona.hidden_facts`` to avoid duplication.
* ``answer_must_not_contain`` is built from a centralised template
  driven by UC + ``allow_bot_resolution`` (phase2 §2.6 / §2.9).

Wave A4 transcript-evidence generation
--------------------------------------
Each HR row now selects turns from exactly one source file via
``source_dataset``. Those turns are reduced to ``TranscriptEvidence`` and
used for persona seed messages plus outcome / trigger resolution. HR
outcome fields are hints; transcript evidence can override policy defaults
when the human transcript shows unresolved investigation, handover, or
safe runtime resolution.

Side outputs
------------
The module maintains a ``_GENERATION_AUDIT`` list of per-session audit
entries (selected turns file, transcript evidence, overrides applied,
policy/HR mismatches, dropped hidden_facts, stripped human-only tools).
Call ``dump_audit_to(path)`` to write it as markdown for Wave A3/A4 QA.
"""

from __future__ import annotations

import csv
import json
import logging
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

import yaml

from .policy_table import (
    UcPolicy,
    get_policy,
    list_human_only_tools,
)
from .case_outcome_resolver import resolve_case_outcome
from .schema import (
    CaseSpec,
    Expected,
    FormContext,
    HiddenFact,
    Persona,
    ScoringConfig,
)
from .transcript_evidence import TranscriptEvidence, extract_transcript_evidence

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# UC classification helpers
# ---------------------------------------------------------------------------

_FAQ_UCS = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"}
_INTAKE_UCS = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}

SOURCE_DATASET_TURNS_FILE: dict[str, str] = {
    "golden": "golden_turns.csv",
    "escalation": "escalation_turns.csv",
    "badcase": "badcase_turns.csv",
    "handover": "handover_turns.csv",
    "clarification": "clarification_turns.csv",
    "drift_control": "drift_control_turns.csv",
    "intake_tool_contract": "intake_tool_contract_turns.csv",
}


def _is_intake_uc(uc: str) -> bool:
    return uc in _INTAKE_UCS


# ---------------------------------------------------------------------------
# UC-B reclassification override map (Wave A2.1)
# ---------------------------------------------------------------------------
#
# Eleven sessions whose HR reviewers tagged primary_uc=UC-B but whose
# transcripts make clear a more specific UC applies. Applied BEFORE any
# UC-dependent logic so the rest of the extractor sees the corrected UC.
# Each value is ``(primary_uc, secondary_ucs)``.

UC_B_RECLASSIFICATION_OVERRIDES: dict[str, tuple[str, list[str]]] = {
    "570Q5000008hx9tIAA":  ("UC-FP", ["UC-K"]),          # Ad keeps getting deleted; needs deletion-reason lookup
    "570Q5000008WmXxIAK":  ("UC-K",  ["UC-FP", "UC-B"]), # cs_interactive_015 -- "what happened to my ad"
    "570Q5000008TMmvIAG":  ("UC-FP", ["UC-K"]),          # Paid promotion + temporary hold
    "570Q5000008U5C9IAK":  ("UC-A",  ["UC-D", "UC-K"]),  # Wrong email, no adverts showing
    "570Q5000008wmKbIAI":  ("UC-A",  ["UC-H", "UC-D"]),  # Trader flag wrong on account
    "570Q5000009060DIAQ":  ("UC-A",  ["UC-K"]),          # "Where is my ad"
    "570Q5000008w24rIAA":  ("UC-FP", ["UC-F"]),          # Cancel auto-renewal
    "570Q5000008fG5qIAE":  ("UC-FP", ["UC-K", "UC-C"]),  # Ad breaking rules; phone rejected
    "570Q5000008caqfIAA":  ("UC-D",  []),                # Wrong price on specific ad -- agent asked for ad ID
    "570Q5000008IwKHIA0":  ("UC-FP", ["UC-K"]),          # Paid 30-day Booster expired
    "570Q5000008iwZxIAI":  ("UC-K",  ["UC-FP", "UC-B"]), # Advert on hold 2nd time, please restore
}


# ---------------------------------------------------------------------------
# Generation audit -- populated as specs are built (Wave A2.1+ / A4)
# ---------------------------------------------------------------------------

_GENERATION_AUDIT: list[dict[str, Any]] = []


def _reset_audit() -> None:
    """Clear the audit log -- intended for tests / repeat runs."""
    _GENERATION_AUDIT.clear()


def dump_audit_to(path: str | Path) -> Path:
    """Write the generation audit to ``path`` as markdown.

    Wave A3/A4 consumes this file when building the spec-regeneration QA
    report. Each session contributes one section listing the selected turns
    file, transcript evidence, policy-vs-HR mismatches, dropped hidden-facts
    entries, and human-only tools stripped from forbidden_tools.
    """
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    lines: list[str] = [
        "# CaseSpec generation audit",
        "",
        f"_Sessions audited: **{len(_GENERATION_AUDIT)}**_",
        "",
    ]
    for entry in _GENERATION_AUDIT:
        lines.append(f"## {entry['session_id']} (case_id={entry.get('case_id', '?')})")
        lines.append("")
        lines.append(f"- original primary_uc (HR): `{entry.get('hr_primary_uc', '?')}`")
        lines.append(f"- final primary_uc: `{entry.get('final_primary_uc', '?')}`")
        if entry.get("turns_file"):
            lines.append(f"- turns file: `{entry['turns_file']}`")
            lines.append(f"- turn count: `{entry.get('turn_count', 0)}`")
        if entry.get("transcript_outcome"):
            lines.append(f"- transcript outcome: `{entry['transcript_outcome']}`")
            lines.append(f"- transcript reason: {entry.get('transcript_reason', '')}")
        if entry.get("evidence_signal_counts"):
            lines.append(f"- evidence signal counts: `{entry['evidence_signal_counts']}`")
        if entry.get("representative_user_messages"):
            lines.append("- representative user messages:")
            for msg in entry["representative_user_messages"]:
                lines.append(f"  - {msg}")
        if entry.get("outcome_decision_reason"):
            lines.append(f"- outcome decision: {entry['outcome_decision_reason']}")
        if entry.get("override_applied"):
            lines.append(f"- override applied: **YES** -> "
                         f"primary={entry['override_applied'][0]}, "
                         f"secondary={entry['override_applied'][1]}")
        mismatches: list[str] = entry.get("policy_vs_hr_mismatches", [])
        if mismatches:
            lines.append("- policy-vs-HR mismatches:")
            for m in mismatches:
                lines.append(f"  - {m}")
        dropped_hf: list[str] = entry.get("dropped_hidden_facts", [])
        if dropped_hf:
            lines.append(f"- dropped hidden_facts (already in form_context): {dropped_hf}")
        stripped_tools: list[str] = entry.get("stripped_human_only_tools", [])
        if stripped_tools:
            lines.append(f"- stripped human-only tools from forbidden: {stripped_tools}")
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")
    return path


def _record_audit(entry: dict[str, Any]) -> None:
    _GENERATION_AUDIT.append(entry)


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
    return {"subject": message.strip(), "description": ""}


# ---------------------------------------------------------------------------
# Turns loading
# ---------------------------------------------------------------------------

def _load_turns_file(turns_path: Path) -> dict[str, list[dict]]:
    """Load one turns CSV and index it by conversation_id (session_id)."""
    index: dict[str, list[dict]] = defaultdict(list)
    with open(turns_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            cid = row.get("conversation_id", "").strip()
            if cid:
                index[cid].append(row)
    for cid in index:
        index[cid].sort(key=lambda r: int(r.get("sequence", 0)))
    return dict(index)


def _load_source_turn_indexes(turns_dir: Path) -> dict[str, dict[str, list[dict]]]:
    """Load each known source-dataset turns file independently.

    Wave A4 provenance rule: a CaseSpec row may only use turns from its
    own source dataset file. We therefore keep a per-source index instead
    of merging all ``*_turns.csv`` files by conversation_id.
    """
    indexes: dict[str, dict[str, list[dict]]] = {}
    for source_dataset, filename in SOURCE_DATASET_TURNS_FILE.items():
        turns_path = turns_dir / filename
        if not turns_path.exists():
            logger.warning(
                "Turns file for source_dataset=%s is missing: %s",
                source_dataset,
                turns_path,
            )
            indexes[source_dataset] = {}
            continue
        indexes[source_dataset] = _load_turns_file(turns_path)
    return indexes


def _turns_file_for_source(source_dataset: str) -> str:
    return SOURCE_DATASET_TURNS_FILE.get(source_dataset.strip(), "")


def _get_form_turn(turns: list[dict]) -> dict | None:
    for t in turns:
        if (
            t.get("sequence", "") == "0"
            and t.get("speaker", "") == "[PRE_CHAT_FORM]"
        ):
            return t
    return None


def _get_visitor_turns(turns: list[dict]) -> list[dict]:
    return [
        t
        for t in turns
        if int(t.get("sequence", 0)) > 0 and t.get("role", "") == "visitor"
    ]


def _get_visitor_name(turns: list[dict]) -> str:
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
# UC override resolution (Wave A2.1)
# ---------------------------------------------------------------------------

def _apply_uc_override(
    session_id: str,
    hr_primary_uc: str,
    hr_secondary_ucs: list[str],
) -> tuple[str, list[str], tuple[str, list[str]] | None]:
    """Resolve final (primary_uc, secondary_ucs) for ``session_id``.

    If the session is in ``UC_B_RECLASSIFICATION_OVERRIDES`` the override
    wins. Returns the final values plus the override tuple (or None) so
    the caller can record it in the audit log.
    """
    if session_id in UC_B_RECLASSIFICATION_OVERRIDES:
        primary, secondary = UC_B_RECLASSIFICATION_OVERRIDES[session_id]
        logger.warning(
            "UC override applied for session %s: HR=%s -> override=%s (secondary=%s)",
            session_id,
            hr_primary_uc,
            primary,
            secondary,
        )
        return primary, list(secondary), (primary, list(secondary))
    return hr_primary_uc, hr_secondary_ucs, None


# ---------------------------------------------------------------------------
# Persona derivation
# ---------------------------------------------------------------------------

def _derive_frustration_level(has_frustration: str, frustration_type: str) -> str:
    if not _parse_bool(has_frustration):
        return "none"
    ft = frustration_type.lower()
    if any(kw in ft for kw in ("litigation", "threat", "abusive", "legal")):
        return "high"
    if any(kw in ft for kw in ("general", "impatient", "wait", "confusion")):
        return "mild"
    return "mild" if ft else "none"


def _derive_drift_behavior(drift_type: str) -> str:
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
    if not seed_messages:
        return "normal"
    avg_len = sum(len(m) for m in seed_messages) / len(seed_messages)
    if avg_len < 20:
        return "terse"
    if avg_len > 100:
        return "verbose"
    return "normal"


def _summarize_description(description: str) -> str:
    """One-sentence factual summary of the form description.

    Trims to a single sentence; collapses internal whitespace; never
    quotes any "expects bot to resolve / escalate" guidance.
    """
    desc = re.sub(r"\s+", " ", description.strip())
    if not desc:
        return ""
    # First sentence (up to the first ., ! or ?), capped at 220 chars.
    m = re.match(r"(.+?[.!?])(\s|$)", desc)
    sentence = m.group(1) if m else desc
    if len(sentence) > 220:
        sentence = sentence[:217].rstrip() + "..."
    return sentence


def _derive_user_goal_summary(
    topic: str,
    description: str,
    drift_type: str,
) -> str:
    """Build the persona user_goal_summary from form_context + drift_type.

    Wave A2.1 contract: the summary is *neutral and factual*. It must NOT
    contain any "expects bot to resolve/escalate" phrasing -- that is the
    job of ``Expected.bot_handling_pattern`` which lives on the bot side.
    """
    topic_clean = topic.strip() or "an unspecified topic"
    summary_sentence = _summarize_description(description)
    drift_clean = (drift_type or "none").strip().lower() or "none"
    if summary_sentence:
        body = f"User reports {topic_clean}: {summary_sentence}"
    else:
        body = f"User reports {topic_clean}."
    if not body.endswith("."):
        body += "."
    return f"{body} Drift: {drift_clean}."


def _derive_hidden_facts_raw(
    form_provides_email: str,
    form_provides_ad_id: str,
    ad_id: str,
    email: str,
) -> list[dict[str, str]]:
    """Build hidden facts from form-provided identifiers (HR-driven).

    NOTE: Wave A2.1 will post-filter this list against form_context to
    drop entries that duplicate fields already exposed there.
    """
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


_DUPLICATE_HIDDEN_FACT_PATTERNS = (
    re.compile(r"^email\s+is\b", re.IGNORECASE),
    re.compile(r"^phone(\s+number)?\s+is\b", re.IGNORECASE),
    re.compile(r"^ad\s+id\s+is\b", re.IGNORECASE),
)


def _filter_redundant_hidden_facts(
    facts: list[dict[str, str]],
    form_context: FormContext,
) -> tuple[list[dict[str, str]], list[str]]:
    """Drop hidden_facts that duplicate form_context fields.

    A fact is considered redundant if it begins with "email is ..." (when
    ``form_context.email`` is set), "ad id is ..." (when
    ``form_context.ad_id`` is set), or "phone is ..." (always -- phone
    isn't a FormContext field today, but if HR adds one we don't want it
    duplicated either). Returns the filtered list plus the list of
    dropped fact strings for the audit log.
    """
    kept: list[dict[str, str]] = []
    dropped: list[str] = []
    has_email = bool(form_context.email)
    has_ad_id = bool(form_context.ad_id)
    for f in facts:
        text = f.get("fact", "").strip()
        is_email_fact = _DUPLICATE_HIDDEN_FACT_PATTERNS[0].match(text) is not None
        is_phone_fact = _DUPLICATE_HIDDEN_FACT_PATTERNS[1].match(text) is not None
        is_ad_id_fact = _DUPLICATE_HIDDEN_FACT_PATTERNS[2].match(text) is not None
        if is_email_fact and has_email:
            dropped.append(text)
            continue
        if is_phone_fact:
            # No form_context.phone today; treat any phone-fact as
            # duplicate-by-policy because it should be a form field, not
            # a hidden behavioural state.
            dropped.append(text)
            continue
        if is_ad_id_fact and has_ad_id:
            dropped.append(text)
            continue
        kept.append(f)
    return kept, dropped


def _derive_will_request_human_if(
    has_frustration: str,
    escalation_trigger: str,
) -> str:
    if _parse_bool(has_frustration) and escalation_trigger.strip().lower() == "user_requested":
        return "bot cannot resolve after 2 attempts"
    return ""


# ---------------------------------------------------------------------------
# Expected.* derivation -- POLICY + transcript evidence (Wave A4)
# ---------------------------------------------------------------------------

def _derive_expected_tool_sequence(
    policy: UcPolicy,
    final_should_escalate: bool,
) -> list[str]:
    """Derive the tool sequence for the resolved outcome path.

    The policy table stores each UC's canonical path. Wave A4 lets
    transcript evidence flip a case between FAQ-resolve and handover, so
    the sequence must follow the resolved outcome, not only the policy
    default.
    """
    if not final_should_escalate:
        if policy.allow_bot_resolution == "partial":
            return ["get_customer_context", "record_outcome"]
        return [
            tool
            for tool in policy.expected_tool_sequence_template
            if tool not in {"create_case_controlled", "request_handover"}
        ]

    sequence = list(policy.expected_tool_sequence_template)
    if "request_handover" not in sequence:
        sequence = _insert_before_record(sequence, "request_handover")
    if "record_outcome" not in sequence:
        sequence.append("record_outcome")
    return sequence


def _insert_before_record(sequence: list[str], tool: str) -> list[str]:
    """Insert ``tool`` before ``record_outcome`` while preserving order."""
    if tool in sequence:
        return sequence
    if "record_outcome" in sequence:
        idx = sequence.index("record_outcome")
        return sequence[:idx] + [tool] + sequence[idx:]
    return sequence + [tool]


def _resolve_forbidden_tools(
    policy: UcPolicy,
) -> tuple[list[str], list[str]]:
    """Per-case forbidden_tools = policy table forbidden, minus human-only.

    Human-only tools (``moderation_enforcement_action``,
    ``send_followup_email_or_async_update``) are blocked by a global L1
    check -- they should NOT be repeated per-case. Returns the filtered
    list plus the list of tools stripped (for audit).
    """
    human_only = set(list_human_only_tools())
    kept: list[str] = []
    stripped: list[str] = []
    for tool in policy.forbidden_tools_for_bot:
        if tool in human_only:
            stripped.append(tool)
            continue
        kept.append(tool)
    return kept, stripped


# ---------------------------------------------------------------------------
# Expected.bot_handling_pattern -- DERIVED template per resolved path (Wave A4)
# ---------------------------------------------------------------------------

def _derive_bot_handling_pattern(
    policy: UcPolicy,
    final_should_escalate: bool,
    final_trigger: str | None,
    expected_tool_sequence: list[str],
) -> str:
    """One-sentence plain-English description of the expected bot pattern.

    The pattern is built from the policy's tool sequence template + the
    escalation trigger. Distinct from ``Persona.user_goal_summary``
    (which describes what the *user* wants).
    """
    seq_str = ", ".join(expected_tool_sequence) if expected_tool_sequence else "no tools"
    if final_should_escalate:
        trigger = final_trigger or policy.default_escalation_trigger or "incomplete_intake"
        if "create_case_controlled" in expected_tool_sequence:
            return (
                f"Acknowledge the issue, collect required intake fields "
                f"({', '.join(policy.required_intake_fields) or 'none'}), "
                f"run policy-mandated tools ({seq_str}), create a controlled "
                f"case where permitted, and hand over with reason {trigger}."
            )
        return (
            f"Acknowledge the issue, run policy-mandated tools ({seq_str}), "
            f"collect required intake fields "
            f"({', '.join(policy.required_intake_fields) or 'none'}), and "
            f"hand over with reason {trigger}."
        )

    if policy.allow_bot_resolution == "partial":
        return (
            f"Acknowledge the issue, run get_customer_context for "
            f"safe_summary, and resolve from runtime state if possible "
            f"({seq_str})."
        )

    return (
        f"Acknowledge the issue, run policy-mandated tools "
        f"({seq_str}), and answer from the knowledge base. Do not escalate."
    )


# ---------------------------------------------------------------------------
# Expected.answer_must_not_contain -- TEMPLATE (Wave A2.1)
# ---------------------------------------------------------------------------
#
# Centralised template per phase2 §2.6 (no-direct-action phrases) and
# §2.9 (escalation honesty). The bot must never claim to have performed
# an action it cannot actually take, and must never claim to resolve a
# case that the policy says it must escalate.
_BANNED_PHRASES_NEVER_RESOLVE = [
    "I've fixed",
    "I have sent you an email",
    "I can restore/delete/ban this directly",
]

_BANNED_PHRASES_ESCALATE_OUTCOME = [
    "I'll resolve this for you",
]


def _derive_answer_must_not_contain(
    policy: UcPolicy,
    final_outcome_class: str,
) -> list[str]:
    """Build ``answer_must_not_contain`` from a policy-driven template.

    - When ``allow_bot_resolution != 'true'`` (i.e. partial / false):
      ban "I've fixed", "I have sent you an email",
      "I can restore/delete/ban this directly" (phase2 §2.6).
    - When ``outcome_class == 'escalate'``: also ban
      "I'll resolve this for you" (phase2 §2.9 -- the bot must be honest
      about handing over).
    """
    banned: list[str] = []
    if policy.allow_bot_resolution != "true" or final_outcome_class == "escalate":
        banned.extend(_BANNED_PHRASES_NEVER_RESOLVE)
    if final_outcome_class == "escalate":
        banned.extend(_BANNED_PHRASES_ESCALATE_OUTCOME)
    # De-dupe while preserving order
    seen: set[str] = set()
    result: list[str] = []
    for p in banned:
        if p not in seen:
            seen.add(p)
            result.append(p)
    return result


# ---------------------------------------------------------------------------
# Scoring config derivation
# ---------------------------------------------------------------------------

# Wave A2.1: the scoring layer (Wave B1.x) will add several new L1
# checks. We pre-list their canonical names here so freshly-generated
# specs already reference them; the scoring layer can no-op any name it
# doesn't yet implement. These names are tracked in
# phase5_evaluation_design §4.2 (Hard checks).
_REQUIRED_L1_HARD_CHECKS = (
    "phase_transition_validity",
    "no_critical_policy_violation",
    "no_pii_leakage",
    "no_stall",
    "no_human_only_tool_exposure",  # added by Wave B1.2
)


def _derive_scoring_config(primary_uc: str, should_escalate: bool) -> ScoringConfig:
    """Build standard ScoringConfig based on UC type.

    Wave A2.1 keeps the existing UC-shaped logic, but ALWAYS prepends the
    required L1 hard-check names (see ``_REQUIRED_L1_HARD_CHECKS``) so
    every spec references them.
    """
    hard_checks: list[str] = list(_REQUIRED_L1_HARD_CHECKS) + [
        "no_forbidden_tools",
        "budget_enforcement",
    ]
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


def _transcript_signal_count_summary(
    evidence: TranscriptEvidence,
) -> dict[str, int | bool]:
    """Compact transcript-evidence summary for audit output."""
    return {
        "unresolved_user": len(evidence.unresolved_user_signals),
        "human_investigation": len(evidence.human_investigation_signals),
        "handover_or_case": len(evidence.handover_or_case_signals),
        "identifier_context": len(evidence.identifier_context_signals),
        "user_requested_human": evidence.user_requested_human,
    }


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
    """Assign a case to anchor / promotion / exploration set."""
    conf = uc_confidence.strip().lower()
    drift = drift_type.strip().lower()
    risk = risk_level.strip().lower()

    score = 0
    if risk in ("critical",):
        score += 4
    elif risk in ("high",):
        score += 3
    elif risk in ("medium",):
        score += 1

    if drift == "hard_shift":
        score += 2
    elif drift == "soft_shift":
        score += 1
    elif drift == "minor_drift":
        score += 0

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
    }
    d["expected"] = {
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
    turns_file: str = "",
) -> CaseSpec:
    """Build a single CaseSpec from an HR row and its matching turns.

    Wave A4 sequencing:
      1. Resolve final UC (HR + override map).
      2. Look up policy = get_policy(final_primary_uc).
      3. Derive UC-owned fields from ``policy``; HR is a hint, not the
         authority for outcome when transcript evidence is stronger.
      4. Build form_context (HR-owned).
      5. Extract TranscriptEvidence from the selected source turns.
      6. Build persona (HR-owned, but seed_messages and outcome evidence
         come from TranscriptEvidence; hidden_facts are filtered).
      7. Assemble Expected via policy + HR hints + TranscriptEvidence.
      8. Compute scoring.
      9. Record audit entry.
    """
    session_id = row["session_id"].strip()
    case_id = f"cs_interactive_{case_index:03d}"
    source_dataset = row.get("source_dataset", "unknown").strip()

    # --- (1) UC resolution: HR -> override ---
    primary_uc_corrected = row.get("primary_uc_corrected", "").strip()
    source_primary_uc = row.get("source_primary_uc", "").strip()
    hr_primary_uc = primary_uc_corrected if primary_uc_corrected else source_primary_uc
    hr_secondary_ucs = _parse_secondary_ucs(row.get("secondary_ucs", ""))

    primary_uc, secondary_ucs, override_applied = _apply_uc_override(
        session_id,
        hr_primary_uc,
        hr_secondary_ucs,
    )

    # --- (2) Policy lookup ---
    try:
        policy = get_policy(primary_uc)
    except KeyError:
        # Defensive: unknown UC -- fall back to UC-A so we don't crash a
        # 367-row run on a single typo. The audit log will flag it.
        logger.warning("Unknown UC %r for session %s; falling back to UC-A.",
                       primary_uc, session_id)
        primary_uc = "UC-A"
        policy = get_policy("UC-A")

    # --- (3) Form context (HR-owned) ---
    form_turn = _get_form_turn(turns)
    if form_turn:
        form_data = _parse_form_message(form_turn.get("message_redacted", ""))
    else:
        form_data = {
            "subject": row.get("form_topic_subject", ""),
            "description": "",
        }
    topic_subject = row.get("form_topic_subject", "").strip() or form_data.get("subject", "")
    description = form_data.get("description", "")
    first_name = _get_visitor_name(turns)
    email = "customer@example.com" if _parse_bool(row.get("form_provides_email", "")) else ""
    ad_id = "REDACTED_AD_ID" if _parse_bool(row.get("form_provides_ad_id", "")) else ""
    form_context = FormContext(
        first_name=first_name,
        email=email,
        topic_subject=topic_subject,
        ad_id=ad_id,
        description=description,
    )

    # --- (4) Transcript evidence + seed messages ---
    transcript_evidence = extract_transcript_evidence(
        turns,
        turns_file=turns_file,
        source_dataset=source_dataset,
    )
    seed_messages = list(transcript_evidence.representative_user_messages)
    if not seed_messages:
        seed_messages = [description] if description else [topic_subject]

    # --- (5) Persona ---
    drift_type_raw = row.get("drift_type", "")
    user_goal_summary = _derive_user_goal_summary(
        topic_subject,
        description,
        drift_type_raw,
    )
    frustration_level = _derive_frustration_level(
        row.get("has_frustration", ""),
        row.get("frustration_type", ""),
    )
    drift_behavior = _derive_drift_behavior(drift_type_raw)
    verbosity = _derive_verbosity(seed_messages)
    hidden_facts_raw = _derive_hidden_facts_raw(
        row.get("form_provides_email", ""),
        row.get("form_provides_ad_id", ""),
        ad_id,
        email,
    )
    hidden_facts_filtered, dropped_hidden_facts = _filter_redundant_hidden_facts(
        hidden_facts_raw, form_context,
    )
    hidden_facts = [
        HiddenFact(fact=hf["fact"], disclose_when=hf["disclose_when"])
        for hf in hidden_facts_filtered
    ]
    will_request_human_if = _derive_will_request_human_if(
        row.get("has_frustration", ""),
        row.get("escalation_trigger", ""),
    )
    persona = Persona(
        user_goal_summary=user_goal_summary,
        frustration_level=frustration_level,
        verbosity=verbosity,
        drift_behavior=drift_behavior,
        seed_messages=seed_messages,
        hidden_facts=hidden_facts,
        will_request_human_if=will_request_human_if,
    )

    # --- (6) Expected (POLICY-OWNED, with HR mismatch tracking) ---
    hr_outcome = row.get("outcome_class", "resolve").strip()
    hr_should_escalate = _parse_bool(row.get("should_escalate", ""))
    hr_trigger_raw = row.get("escalation_trigger", "").strip()
    risk_level = row.get("risk_level", "low").strip() or "low"

    outcome_resolution = resolve_case_outcome(policy, row, transcript_evidence)
    final_outcome_class = outcome_resolution.outcome_class
    final_should_escalate = outcome_resolution.should_escalate
    final_trigger = outcome_resolution.escalation_trigger

    forbidden_tools, stripped_human_only = _resolve_forbidden_tools(policy)
    expected_tool_sequence = _derive_expected_tool_sequence(policy, final_should_escalate)
    bot_handling_pattern = _derive_bot_handling_pattern(
        policy,
        final_should_escalate,
        final_trigger,
        expected_tool_sequence,
    )
    answer_must_not_contain = _derive_answer_must_not_contain(policy, final_outcome_class)
    max_turns = 10 if _is_intake_uc(primary_uc) else 15

    expected = Expected(
        outcome_class=final_outcome_class,
        primary_uc=primary_uc,
        secondary_ucs=secondary_ucs,
        should_escalate=final_should_escalate,
        allow_bot_resolution=policy.allow_bot_resolution,
        bot_handling_pattern=bot_handling_pattern,
        escalation_trigger=final_trigger,
        risk_level=risk_level,
        expected_tool_sequence=expected_tool_sequence,
        forbidden_tools=forbidden_tools,
        grounding_mode=policy.grounding_mode,
        answer_must_not_contain=answer_must_not_contain,
        max_turns=max_turns,
    )

    # --- (7) Scoring ---
    scoring = _derive_scoring_config(primary_uc, final_should_escalate)

    # --- (8) Audit ---
    mismatches: list[str] = []
    hr_seq = _parse_tool_names(row.get("expected_tool_sequence", ""))
    if hr_seq and hr_seq != expected_tool_sequence:
        mismatches.append(
            f"expected_tool_sequence: HR={hr_seq} vs final={expected_tool_sequence}"
        )
    hr_forbidden = _parse_forbidden_tool_names(row.get("forbidden_tools", ""))
    if hr_forbidden:
        hr_forbidden_no_human = [t for t in hr_forbidden if t not in set(list_human_only_tools())]
        if sorted(hr_forbidden_no_human) != sorted(forbidden_tools):
            mismatches.append(
                f"forbidden_tools: HR(non-human-only)={sorted(hr_forbidden_no_human)} "
                f"vs policy={sorted(forbidden_tools)}"
            )
    if hr_outcome and hr_outcome != final_outcome_class:
        mismatches.append(
            f"outcome_class: HR={hr_outcome!r} vs final={final_outcome_class!r}"
        )
    if hr_should_escalate != final_should_escalate:
        mismatches.append(
            f"should_escalate: HR={hr_should_escalate} vs final={final_should_escalate}"
        )
    if hr_trigger_raw and final_trigger and hr_trigger_raw != final_trigger:
        mismatches.append(
            f"escalation_trigger: HR={hr_trigger_raw!r} vs final={final_trigger!r}"
        )

    _record_audit({
        "session_id": session_id,
        "case_id": case_id,
        "hr_primary_uc": hr_primary_uc,
        "final_primary_uc": primary_uc,
        "override_applied": override_applied,
        "turns_file": turns_file,
        "turn_count": len(turns),
        "transcript_outcome": transcript_evidence.transcript_indicated_outcome,
        "transcript_reason": transcript_evidence.reason,
        "representative_user_messages": transcript_evidence.representative_user_messages,
        "evidence_signal_counts": _transcript_signal_count_summary(transcript_evidence),
        "outcome_decision_reason": outcome_resolution.decision_reason,
        "policy_vs_hr_mismatches": mismatches,
        "dropped_hidden_facts": dropped_hidden_facts,
        "stripped_human_only_tools": stripped_human_only,
    })

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

    Wave A2.1: HR CSV is a HINT, not the source of truth, for any field
    whose owner is the per-UC policy. See module docstring for details.
    """
    hr_csv_path = Path(hr_csv_path)
    turns_dir = Path(turns_dir)
    output_dir = Path(output_dir)

    if not hr_csv_path.exists():
        raise FileNotFoundError(f"HR CSV not found: {hr_csv_path}")
    if not turns_dir.exists():
        raise FileNotFoundError(f"Turns directory not found: {turns_dir}")

    _reset_audit()

    logger.info("Loading source-dataset turns from %s ...", turns_dir)
    source_turn_indexes = _load_source_turn_indexes(turns_dir)
    logger.info(
        "Loaded source turn indexes: %s",
        {source: len(index) for source, index in source_turn_indexes.items()},
    )

    logger.info("Reading HR annotations from %s ...", hr_csv_path)
    with open(hr_csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        hr_rows = list(reader)
    logger.info("Read %d HR rows.", len(hr_rows))

    seen_sessions: set[str] = set()
    unique_rows: list[dict[str, str]] = []
    for row in hr_rows:
        sid = row.get("session_id", "").strip()
        if sid and sid not in seen_sessions:
            seen_sessions.add(sid)
            unique_rows.append(row)
        elif sid in seen_sessions:
            logger.warning("Duplicate session_id %s -- skipping.", sid)

    specs: list[CaseSpec] = []
    set_counts: dict[str, int] = defaultdict(int)
    skipped = 0

    for idx, row in enumerate(unique_rows, start=1):
        session_id = row.get("session_id", "").strip()
        source_dataset = row.get("source_dataset", "").strip()
        turns_filename = _turns_file_for_source(source_dataset)
        if not turns_filename:
            logger.warning(
                "Unknown source_dataset=%s for session_id=%s (case_id=%s) -- skipping.",
                source_dataset,
                session_id,
                row.get("case_id", ""),
            )
            skipped += 1
            continue
        turns = source_turn_indexes.get(source_dataset, {}).get(session_id)
        if not turns:
            logger.warning(
                "No matching turns in %s for session_id=%s (case_id=%s) -- skipping.",
                turns_filename,
                session_id,
                row.get("case_id", ""),
            )
            skipped += 1
            continue

        spec = _build_case_spec(row, turns, idx, turns_filename)

        case_set = _assign_case_set(
            row.get("uc_confidence", ""),
            row.get("outcome_class", ""),
            row.get("has_frustration", ""),
            row.get("drift_type", ""),
            row.get("risk_level", ""),
        )
        set_counts[case_set] += 1

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
