"""CaseSpec extractor -- generates CaseSpec YAML from HR annotations + turn data.

Reads the human-review annotations CSV and per-dataset turn CSVs,
then produces one CaseSpec YAML per HR row and assigns each to a
case set directory (anchor / promotion / exploration).

Wave A2.1 (2026-04-27) refactor
-------------------------------
The HR CSV is now treated as a HINT, not the source of truth, for any
field whose owner is the per-UC policy (phase2 §2.2 / §2.6 / §2.10).
Concretely:

* UC classification is corrected via the v2 ``case_spec_overrides.yaml``
  ``classification`` stage (Wave A6.6) before any UC-derived value is
  computed. The legacy in-code ``UC_B_RECLASSIFICATION_OVERRIDES`` map
  has been migrated into that file.
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
import os
import re
from collections import defaultdict
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any

import yaml

from .policy_table import (
    UcPolicy,
    get_policy,
    list_human_only_tools,
    list_use_cases,
)
from .case_outcome_resolver import resolve_case_outcome
from .llm_persona_reviewer import (
    LlmPersonaReviewer,
    PersonaDraft,
    ReviewResult,
)
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
# Generation audit -- populated as specs are built (Wave A2.1+ / A4)
# ---------------------------------------------------------------------------

_GENERATION_AUDIT: list[dict[str, Any]] = []

DEFAULT_CASE_SPEC_OVERRIDES_PATH = (
    Path(__file__).resolve().parents[2] / "case_spec_overrides.yaml"
)

LLM_REVIEWER_DEFAULT_CACHE_DIR = (
    Path(__file__).resolve().parents[2] / "case_spec_llm_cache"
)


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
        if entry.get("override_classification_applied") and entry.get("case_level_override"):
            override = entry["case_level_override"]
            cls = override.get("classification") or {}
            if cls:
                lines.append(
                    f"- classification override applied: **YES** -> "
                    f"primary={cls.get('primary_uc')}, "
                    f"secondary={cls.get('secondary_ucs', [])}"
                )
        if entry.get("case_level_override"):
            override = entry["case_level_override"]
            lines.append("- case-level override applied: **YES**")
            lines.append(f"  - override source: {override.get('source', '')}")
            lines.append(f"  - reviewer: {override.get('reviewer', '')}")
            lines.append(f"  - date: {override.get('date', '')}")
            lines.append(f"  - confidence: `{override.get('confidence', '')}`")
            lines.append(
                f"  - supporting turns: `{override.get('supporting_turn_numbers', [])}`"
            )
            lines.append(f"  - rationale: {override.get('rationale', '')}")
            if override.get("status"):
                lines.append(f"  - status: `{override.get('status')}`")
            if override.get("migrated_from_legacy"):
                lines.append("  - migrated_from_legacy: `true`")
            if override.get("case_id_hint"):
                lines.append(f"  - case_id_hint: `{override.get('case_id_hint')}`")
            if override.get("expected_changes"):
                lines.append("  - changed expected fields:")
                for fld, change in override["expected_changes"].items():
                    lines.append(
                        f"    - `{fld}`: `{change.get('before')}` -> "
                        f"`{change.get('after')}`"
                    )
            if override.get("persona_changes"):
                lines.append("  - changed persona fields:")
                for fld, change in override["persona_changes"].items():
                    lines.append(
                        f"    - `{fld}`: `{change.get('before')}` -> "
                        f"`{change.get('after')}`"
                    )
        pending = entry.get("pending_overrides_for_session") or []
        if pending:
            lines.append("- pending_review overrides (NOT applied):")
            for rationale in pending:
                lines.append(f"  - {rationale}")
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
        # Wave A6 LLM persona-review fields
        if "llm_cache_hit" in entry or "llm_acceptance_reason" in entry:
            lines.append(
                f"- llm_cache_hit: `{bool(entry.get('llm_cache_hit', False))}`"
            )
            lines.append(
                f"- llm_offline_fallback: `{bool(entry.get('llm_offline_fallback', False))}`"
            )
            reason = entry.get("llm_acceptance_reason")
            lines.append(
                f"- llm_acceptance_reason: `{reason if reason else 'n/a'}`"
            )
            confidence = entry.get("llm_confidence")
            lines.append(
                f"- llm_confidence: `{confidence if confidence else 'n/a'}`"
            )
            changed = entry.get("llm_persona_changed_fields") or []
            if changed:
                lines.append(f"- llm_persona_changed_fields: `{changed}`")
            notes = entry.get("llm_validation_notes") or []
            if notes:
                lines.append("- llm_validation_notes:")
                for note in notes:
                    lines.append(f"  - {note}")
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
# Approved case-level overrides (Wave A6.6 unified schema v2 registry)
# ---------------------------------------------------------------------------
#
# All case-specific corrections — UC reclassification, expected-field
# overrides, and persona pins — live in a single file
# ``eval_interactive/case_spec_overrides.yaml`` (schema v2). Lookup is by
# ``source_session_id`` only.
#
# Each entry may declare zero or more of three blocks corresponding to
# the three pipeline stages at which L3 may apply:
#
#   * ``classification`` (``primary_uc``, ``secondary_ucs``)
#       applied BEFORE policy lookup, at extractor step (1).
#   * ``expected`` (any subset of allowed expected fields, EXCEPT
#       ``primary_uc``/``secondary_ucs``) applied AFTER L1 derivation,
#       at extractor step (7) — Wave A5 timing.
#   * ``persona`` (``seed_messages``, ``user_goal_summary``,
#       ``hidden_facts``) applied AFTER L2 cache and after rule-based
#       persona construction; verbosity is recomputed and the redundancy
#       filter is reapplied.

OVERRIDES_SCHEMA_VERSION: int = 2

_CASE_OVERRIDE_EXPECTED_FIELDS: frozenset[str] = frozenset({
    "outcome_class",
    "should_escalate",
    "allow_bot_resolution",
    "bot_handling_pattern",
    "escalation_trigger",
    "risk_level",
    "expected_tool_sequence",
    "forbidden_tools",
    "grounding_mode",
    "answer_must_not_contain",
    "max_turns",
})

_CASE_OVERRIDE_PERSONA_FIELDS: frozenset[str] = frozenset({
    "seed_messages",
    "user_goal_summary",
    "hidden_facts",
})

_CASE_OVERRIDE_CLASSIFICATION_FIELDS: frozenset[str] = frozenset({
    "primary_uc",
    "secondary_ucs",
})


@dataclass(frozen=True)
class OverrideEntry:
    """One v2 override entry, partitioned by stage."""

    source_session_id: str
    status: str  # "approved" | "pending_review"
    case_id_hint: str = ""
    source: str = ""
    reviewer: str = ""
    date: str = ""
    confidence: str = ""
    supporting_turn_numbers: tuple[int, ...] = ()
    rationale: str = ""
    migrated_from_legacy: bool = False
    classification: dict[str, Any] | None = None
    expected: dict[str, Any] | None = None
    persona: dict[str, Any] | None = None
    raw: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class OverrideRegistry:
    """Result of :func:`_load_case_spec_overrides`. Approved entries are
    indexed by ``source_session_id``; pending entries are returned as a
    list (a session may have at most one entry total — duplicates are a
    hard error)."""

    applied: dict[str, OverrideEntry]
    pending: list[OverrideEntry]


def _known_use_cases() -> set[str]:
    return set(list_use_cases())


def _normalise_classification_block(
    block: dict[str, Any] | None,
    *,
    session_id: str,
    known_ucs: set[str],
) -> dict[str, Any] | None:
    if block is None:
        return None
    if not isinstance(block, dict):
        raise ValueError(
            f"Override for source_session_id={session_id!r}: "
            f"`classification` must be a mapping, got {type(block).__name__}"
        )
    unknown = sorted(set(block) - _CASE_OVERRIDE_CLASSIFICATION_FIELDS)
    if unknown:
        raise ValueError(
            f"Override for source_session_id={session_id!r}: "
            f"`classification` block has unknown fields: {unknown}. "
            f"Allowed: {sorted(_CASE_OVERRIDE_CLASSIFICATION_FIELDS)}."
        )
    out: dict[str, Any] = {}
    if "primary_uc" in block:
        primary = str(block["primary_uc"]).strip()
        if not primary:
            raise ValueError(
                f"Override for source_session_id={session_id!r}: "
                f"`classification.primary_uc` is empty"
            )
        if primary not in known_ucs:
            raise ValueError(
                f"Override for source_session_id={session_id!r}: "
                f"unknown UC in classification.primary_uc={primary!r}. "
                f"Known: {sorted(known_ucs)}."
            )
        out["primary_uc"] = primary
    if "secondary_ucs" in block:
        secondary_raw = block.get("secondary_ucs") or []
        if not isinstance(secondary_raw, list):
            raise ValueError(
                f"Override for source_session_id={session_id!r}: "
                f"`classification.secondary_ucs` must be a list, "
                f"got {type(secondary_raw).__name__}"
            )
        secondary_clean: list[str] = []
        for item in secondary_raw:
            uc = str(item).strip()
            if not uc:
                continue
            if uc not in known_ucs:
                raise ValueError(
                    f"Override for source_session_id={session_id!r}: "
                    f"unknown UC in classification.secondary_ucs={uc!r}. "
                    f"Known: {sorted(known_ucs)}."
                )
            secondary_clean.append(uc)
        out["secondary_ucs"] = secondary_clean
    return out or None


def _normalise_expected_block(
    block: dict[str, Any] | None,
    *,
    session_id: str,
) -> dict[str, Any] | None:
    if block is None:
        return None
    if not isinstance(block, dict):
        raise ValueError(
            f"Override for source_session_id={session_id!r}: "
            f"`expected` must be a mapping, got {type(block).__name__}"
        )
    keys = set(block)
    if "primary_uc" in keys or "secondary_ucs" in keys:
        raise ValueError(
            f"Override for source_session_id={session_id!r}: "
            f"`expected.primary_uc`/`expected.secondary_ucs` is not allowed; "
            f"use classification.primary_uc instead (Wave A6.6)."
        )
    unknown = sorted(keys - _CASE_OVERRIDE_EXPECTED_FIELDS)
    if unknown:
        raise ValueError(
            f"Override for source_session_id={session_id!r}: "
            f"`expected` block has unknown fields: {unknown}. "
            f"Allowed: {sorted(_CASE_OVERRIDE_EXPECTED_FIELDS)}."
        )
    return dict(block) or None


def _normalise_persona_block(
    block: dict[str, Any] | None,
    *,
    session_id: str,
) -> dict[str, Any] | None:
    if block is None:
        return None
    if not isinstance(block, dict):
        raise ValueError(
            f"Override for source_session_id={session_id!r}: "
            f"`persona` must be a mapping, got {type(block).__name__}"
        )
    unknown = sorted(set(block) - _CASE_OVERRIDE_PERSONA_FIELDS)
    if unknown:
        raise ValueError(
            f"Override for source_session_id={session_id!r}: "
            f"`persona` block has disallowed keys: {unknown}. "
            f"Allowed: {sorted(_CASE_OVERRIDE_PERSONA_FIELDS)}."
        )
    out: dict[str, Any] = {}
    if "seed_messages" in block:
        seeds = block["seed_messages"] or []
        if not isinstance(seeds, list) or not all(isinstance(s, str) for s in seeds):
            raise ValueError(
                f"Override for source_session_id={session_id!r}: "
                f"`persona.seed_messages` must be a list of strings"
            )
        out["seed_messages"] = [s for s in seeds]
    if "user_goal_summary" in block:
        ugs = block["user_goal_summary"]
        if ugs is not None and not isinstance(ugs, str):
            raise ValueError(
                f"Override for source_session_id={session_id!r}: "
                f"`persona.user_goal_summary` must be a string"
            )
        out["user_goal_summary"] = (ugs or "").strip()
    if "hidden_facts" in block:
        facts = block["hidden_facts"] or []
        if not isinstance(facts, list):
            raise ValueError(
                f"Override for source_session_id={session_id!r}: "
                f"`persona.hidden_facts` must be a list"
            )
        clean: list[dict[str, str]] = []
        for hf in facts:
            if not isinstance(hf, dict):
                raise ValueError(
                    f"Override for source_session_id={session_id!r}: "
                    f"`persona.hidden_facts[*]` must be a mapping"
                )
            extra = sorted(set(hf) - {"fact", "disclose_when"})
            if extra:
                raise ValueError(
                    f"Override for source_session_id={session_id!r}: "
                    f"`persona.hidden_facts[*]` has disallowed keys: {extra}"
                )
            clean.append({
                "fact": str(hf.get("fact", "")).strip(),
                "disclose_when": str(hf.get("disclose_when", "")).strip(),
            })
        out["hidden_facts"] = clean
    return out or None


def _load_case_spec_overrides(
    path: str | Path = DEFAULT_CASE_SPEC_OVERRIDES_PATH,
) -> OverrideRegistry:
    """Load approved + pending case-level overrides from a v2 file.

    Returns an :class:`OverrideRegistry` whose ``applied`` mapping is keyed by
    ``source_session_id`` (status=approved) and whose ``pending`` list holds
    every entry with ``status: pending_review``.

    The override file is optional; a missing file yields empty applied/pending.
    A v1 file is rejected with a one-line migration message (see Wave A6.6).
    """
    override_path = Path(path)
    if not override_path.exists():
        logger.info("No case-spec override file found at %s; continuing.", override_path)
        return OverrideRegistry(applied={}, pending=[])

    raw = yaml.safe_load(override_path.read_text(encoding="utf-8")) or {}
    if not isinstance(raw, dict):
        raise ValueError(
            f"{override_path}: case_spec_overrides.yaml must be schema v{OVERRIDES_SCHEMA_VERSION} "
            f"(top-level mapping with `version` and `overrides`). Run migration: "
            f"see docs/interactive_case_spec_generation_plan.md A6.6."
        )

    version = raw.get("version", 1)
    if version != OVERRIDES_SCHEMA_VERSION:
        raise ValueError(
            f"case_spec_overrides.yaml must be schema v{OVERRIDES_SCHEMA_VERSION} "
            f"(was v{version}). Run migration: see "
            f"docs/interactive_case_spec_generation_plan.md A6.6."
        )

    entries = raw.get("overrides", [])
    if not isinstance(entries, list):
        raise ValueError(
            f"{override_path}: `overrides` must be a list, "
            f"got {type(entries).__name__}"
        )

    known_ucs = _known_use_cases()
    applied: dict[str, OverrideEntry] = {}
    pending: list[OverrideEntry] = []
    seen_sessions: set[str] = set()

    for entry in entries:
        if not isinstance(entry, dict):
            raise ValueError(
                f"{override_path}: every override must be a mapping, got {entry!r}"
            )
        sid = str(entry.get("source_session_id", "")).strip()
        if not sid:
            raise ValueError(
                f"{override_path}: override missing `source_session_id`: {entry!r}"
            )
        if sid in seen_sessions:
            raise ValueError(
                f"{override_path}: duplicate source_session_id={sid!r}; "
                f"v2 schema requires exactly one entry per session."
            )
        seen_sessions.add(sid)

        status = str(entry.get("status", "")).strip()
        if status not in {"approved", "pending_review"}:
            raise ValueError(
                f"Override for source_session_id={sid!r}: `status` must be "
                f"`approved` or `pending_review` (got {status!r})."
            )
        migrated = bool(entry.get("migrated_from_legacy", False))
        case_id_hint = str(entry.get("case_id_hint", "")).strip()
        source = str(entry.get("source", "")).strip()
        reviewer = str(entry.get("reviewer", "")).strip()
        date_str = str(entry.get("date", "")).strip()
        confidence = str(entry.get("confidence", "")).strip()
        rationale = str(entry.get("rationale", "")).strip()
        supporting_turns_raw = entry.get("supporting_turn_numbers", []) or []
        if not isinstance(supporting_turns_raw, list):
            raise ValueError(
                f"Override for source_session_id={sid!r}: "
                f"`supporting_turn_numbers` must be a list of ints"
            )
        supporting_turns: tuple[int, ...] = tuple(int(t) for t in supporting_turns_raw)

        if status == "approved":
            missing: list[str] = []
            if not source:
                missing.append("source")
            if not reviewer:
                missing.append("reviewer")
            if not date_str:
                missing.append("date")
            if not confidence:
                missing.append("confidence")
            if not rationale:
                missing.append("rationale")
            if missing:
                raise ValueError(
                    f"Override for source_session_id={sid!r} (status=approved) "
                    f"is missing required metadata: {missing}"
                )
            if not migrated and len(supporting_turns) == 0:
                raise ValueError(
                    f"Override for source_session_id={sid!r} (status=approved) "
                    f"has empty `supporting_turn_numbers`. This is allowed only "
                    f"when `migrated_from_legacy: true`."
                )

        classification = _normalise_classification_block(
            entry.get("classification"),
            session_id=sid,
            known_ucs=known_ucs,
        )
        expected_block = _normalise_expected_block(
            entry.get("expected"),
            session_id=sid,
        )
        persona_block = _normalise_persona_block(
            entry.get("persona"),
            session_id=sid,
        )

        oe = OverrideEntry(
            source_session_id=sid,
            status=status,
            case_id_hint=case_id_hint,
            source=source,
            reviewer=reviewer,
            date=date_str,
            confidence=confidence,
            supporting_turn_numbers=supporting_turns,
            rationale=rationale,
            migrated_from_legacy=migrated,
            classification=classification,
            expected=expected_block,
            persona=persona_block,
            raw=dict(entry),
        )
        if status == "approved":
            applied[sid] = oe
        else:
            pending.append(oe)

    return OverrideRegistry(applied=applied, pending=pending)


def _apply_expected_override(
    spec: CaseSpec,
    expected_block: dict[str, Any] | None,
) -> dict[str, dict[str, Any]]:
    """Apply the ``expected`` block of an approved override to ``spec``.

    Returns a mapping of changed expected-field names to ``{before, after}``
    dicts (empty when the block is None or contained no fields).
    """
    if not expected_block:
        return {}

    before = asdict(spec.expected)
    expected_data = dict(before)
    expected_data.update(expected_block)

    policy = get_policy(expected_data["primary_uc"])
    if "allow_bot_resolution" not in expected_block:
        expected_data["allow_bot_resolution"] = policy.allow_bot_resolution
    if "grounding_mode" not in expected_block:
        expected_data["grounding_mode"] = policy.grounding_mode
    if "forbidden_tools" not in expected_block:
        expected_data["forbidden_tools"] = _resolve_forbidden_tools(policy)[0]
    if "expected_tool_sequence" not in expected_block:
        expected_data["expected_tool_sequence"] = _derive_expected_tool_sequence(
            policy,
            expected_data["should_escalate"],
        )
    if "bot_handling_pattern" not in expected_block:
        expected_data["bot_handling_pattern"] = _derive_bot_handling_pattern(
            policy,
            expected_data["should_escalate"],
            expected_data["escalation_trigger"],
            expected_data["expected_tool_sequence"],
        )
    if "answer_must_not_contain" not in expected_block:
        expected_data["answer_must_not_contain"] = _derive_answer_must_not_contain(
            policy,
            expected_data["outcome_class"],
        )
    if "max_turns" not in expected_block:
        expected_data["max_turns"] = 10 if _is_intake_uc(expected_data["primary_uc"]) else 15

    spec.expected = Expected(**expected_data)
    spec.scoring = _derive_scoring_config(
        spec.expected.primary_uc,
        spec.expected.should_escalate,
    )

    after = asdict(spec.expected)
    return {
        fld: {"before": before.get(fld), "after": after.get(fld)}
        for fld in sorted(after)
        if before.get(fld) != after.get(fld)
    }


def _apply_persona_override(
    spec: CaseSpec,
    persona_block: dict[str, Any] | None,
) -> dict[str, dict[str, Any]]:
    """Apply the ``persona`` block of an approved override to ``spec``.

    Allowed fields: ``seed_messages``, ``user_goal_summary``, ``hidden_facts``.
    Verbosity is recomputed from the final ``seed_messages`` (mirroring
    :func:`_derive_verbosity`); the redundancy filter is reapplied against
    ``spec.form_context``.
    """
    if not persona_block:
        return {}

    before_seeds = list(spec.persona.seed_messages)
    before_ugs = spec.persona.user_goal_summary
    before_hf = [
        {"fact": hf.fact, "disclose_when": hf.disclose_when}
        for hf in spec.persona.hidden_facts
    ]
    before_verbosity = spec.persona.verbosity

    new_seeds = (
        list(persona_block["seed_messages"])
        if "seed_messages" in persona_block
        else before_seeds
    )
    new_ugs = (
        persona_block["user_goal_summary"]
        if "user_goal_summary" in persona_block
        else before_ugs
    )
    new_hf_dicts = (
        [dict(hf) for hf in persona_block["hidden_facts"]]
        if "hidden_facts" in persona_block
        else before_hf
    )

    # Re-apply the redundant-fact filter against form_context, exactly as
    # the rule extractor does for rule_draft and L2 outputs.
    new_hf_dicts, _ = _filter_redundant_hidden_facts(new_hf_dicts, spec.form_context)

    new_verbosity = _derive_verbosity(new_seeds) if new_seeds else before_verbosity

    spec.persona = Persona(
        user_goal_summary=new_ugs,
        frustration_level=spec.persona.frustration_level,
        verbosity=new_verbosity,
        drift_behavior=spec.persona.drift_behavior,
        seed_messages=new_seeds,
        hidden_facts=[
            HiddenFact(fact=hf["fact"], disclose_when=hf["disclose_when"])
            for hf in new_hf_dicts
        ],
        will_request_human_if=spec.persona.will_request_human_if,
    )

    after_hf = [
        {"fact": hf["fact"], "disclose_when": hf["disclose_when"]}
        for hf in new_hf_dicts
    ]
    changes: dict[str, dict[str, Any]] = {}
    if before_seeds != new_seeds:
        changes["seed_messages"] = {"before": before_seeds, "after": new_seeds}
    if before_ugs != new_ugs:
        changes["user_goal_summary"] = {"before": before_ugs, "after": new_ugs}
    if before_hf != after_hf:
        changes["hidden_facts"] = {"before": before_hf, "after": after_hf}
    if before_verbosity != new_verbosity:
        changes["verbosity"] = {"before": before_verbosity, "after": new_verbosity}
    return changes


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

def _diff_persona_fields(
    rule_draft: PersonaDraft,
    accepted: PersonaDraft,
) -> list[str]:
    """Return the list of persona fields that differ between rule_draft
    and the LLM-accepted value. Empty list when L2 was a no-op."""
    changed: list[str] = []
    if list(rule_draft.seed_messages) != list(accepted.seed_messages):
        changed.append("seed_messages")
    if (rule_draft.user_goal_summary or "") != (accepted.user_goal_summary or ""):
        changed.append("user_goal_summary")

    def _hf_key(items: list[dict[str, str]]) -> list[tuple[str, str]]:
        return sorted(
            ((i.get("fact", "").strip(), i.get("disclose_when", "").strip()) for i in items)
        )

    if _hf_key(list(rule_draft.hidden_facts)) != _hf_key(list(accepted.hidden_facts)):
        changed.append("hidden_facts")
    if rule_draft.verbosity != accepted.verbosity:
        changed.append("verbosity")
    return changed


def build_rule_persona(
    row: dict[str, str],
    turns: list[dict],
    *,
    turns_file: str = "",
    applied_override: "OverrideEntry | None" = None,
) -> tuple[PersonaDraft, dict[str, Any], list[dict]]:
    """Build the rule-derived persona draft for one HR row + turns.

    Wave A6.4 helper: re-uses the same logic ``_build_case_spec`` runs in
    its (3)-(5) steps so the cache populator can produce inputs for
    :meth:`LlmPersonaReviewer.review` without duplicating any rule logic.

    Returns
    -------
    rule_draft
        The :class:`PersonaDraft` the LLM reviewer would see.
    hr_context
        The dict the production extractor passes to ``review(hr_context=...)``.
    transcript_turns_for_llm
        The turns list with the pre-chat form turn stripped (same filter
        the production extractor uses).
    """
    source_dataset = row.get("source_dataset", "unknown").strip()

    # Form context (HR-owned)
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

    # Transcript evidence -> seed messages
    transcript_evidence = extract_transcript_evidence(
        turns,
        turns_file=turns_file,
        source_dataset=source_dataset,
    )
    seed_messages = list(transcript_evidence.representative_user_messages)
    if not seed_messages:
        seed_messages = [description] if description else [topic_subject]

    drift_type_raw = row.get("drift_type", "")
    user_goal_summary = _derive_user_goal_summary(
        topic_subject,
        description,
        drift_type_raw,
    )
    verbosity = _derive_verbosity(seed_messages)
    hidden_facts_raw = _derive_hidden_facts_raw(
        row.get("form_provides_email", ""),
        row.get("form_provides_ad_id", ""),
        ad_id,
        email,
    )
    hidden_facts_filtered, _dropped = _filter_redundant_hidden_facts(
        hidden_facts_raw, form_context,
    )

    rule_draft = PersonaDraft(
        seed_messages=list(seed_messages),
        user_goal_summary=user_goal_summary,
        hidden_facts=list(hidden_facts_filtered),
        verbosity=verbosity,
    )

    # Strip the pre-chat form turn before handing to LLM
    transcript_turns_for_llm = [
        t for t in turns
        if not (
            str(t.get("sequence", "")).strip() == "0"
            and t.get("speaker", "") == "[PRE_CHAT_FORM]"
        )
    ]

    # UC resolution (mirrors _build_case_spec step 1, including classification override)
    primary_uc_corrected = row.get("primary_uc_corrected", "").strip()
    source_primary_uc = row.get("source_primary_uc", "").strip()
    primary_uc = primary_uc_corrected if primary_uc_corrected else source_primary_uc
    secondary_ucs = _parse_secondary_ucs(row.get("secondary_ucs", ""))
    if applied_override is not None and applied_override.classification:
        cls = applied_override.classification
        if "primary_uc" in cls:
            primary_uc = cls["primary_uc"]
        if "secondary_ucs" in cls:
            secondary_ucs = list(cls["secondary_ucs"])

    hr_context: dict[str, Any] = {
        "primary_uc": primary_uc,
        "secondary_ucs": secondary_ucs,
        "drift_type": drift_type_raw or "none",
        "has_frustration": _parse_bool(row.get("has_frustration", "")),
        "frustration_type": row.get("frustration_type", "") or "none",
        "topic_subject": topic_subject,
        "description": description,
        "source_dataset": source_dataset,
        "turns_filename": turns_file,
    }

    return rule_draft, hr_context, transcript_turns_for_llm


def _build_case_spec(
    row: dict[str, str],
    turns: list[dict],
    case_index: int,
    turns_file: str = "",
    *,
    applied_override: OverrideEntry | None = None,
    pending_overrides: list[OverrideEntry] | None = None,
    strict_overrides: bool = False,
    llm_reviewer: LlmPersonaReviewer | None = None,
) -> CaseSpec:
    """Build a single CaseSpec from an HR row and its matching turns.

    Wave A6.6 sequencing:
      1. Resolve final UC: HR -> classification override (if any).
      2. Look up policy = get_policy(final_primary_uc).
      3. Build form_context (HR-owned).
      4. Extract TranscriptEvidence from the selected source turns.
      5. Build persona; pass through L2 LLM reviewer (Wave A6).
      6. Apply persona override (Wave A6.6).
      7. Assemble Expected via policy + HR hints + TranscriptEvidence.
      8. Apply expected override (Wave A6.6 / A5).
      9. Compute scoring.
     10. Record audit entry.
    """
    session_id = row["session_id"].strip()
    case_id = f"cs_interactive_{case_index:03d}"
    source_dataset = row.get("source_dataset", "unknown").strip()

    pending_overrides = list(pending_overrides or [])
    pending_for_session = [p for p in pending_overrides if p.source_session_id == session_id]
    if strict_overrides and pending_for_session:
        rationales = "; ".join(p.rationale or "(no rationale)" for p in pending_for_session)
        raise ValueError(
            f"strict_overrides=True: spec for source_session_id={session_id!r} "
            f"has pending_review override(s): {rationales}"
        )

    # --- (1) UC resolution: HR -> classification override ---
    primary_uc_corrected = row.get("primary_uc_corrected", "").strip()
    source_primary_uc = row.get("source_primary_uc", "").strip()
    hr_primary_uc = primary_uc_corrected if primary_uc_corrected else source_primary_uc
    hr_secondary_ucs = _parse_secondary_ucs(row.get("secondary_ucs", ""))

    primary_uc = hr_primary_uc
    secondary_ucs = list(hr_secondary_ucs)
    classification_override_applied = False
    if applied_override is not None and applied_override.classification:
        cls = applied_override.classification
        if "primary_uc" in cls:
            primary_uc = cls["primary_uc"]
        if "secondary_ucs" in cls:
            secondary_ucs = list(cls["secondary_ucs"])
        classification_override_applied = True
        logger.warning(
            "Classification override applied for session %s: HR=%s -> %s (secondary=%s)",
            session_id,
            hr_primary_uc,
            primary_uc,
            secondary_ucs,
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

    # --- (5b) Wave A6 L2 LLM persona review ---
    # Build the rule_draft snapshot the reviewer sees. hidden_facts here
    # are already de-duplicated against form_context; the LLM may add
    # new facts but the redundancy filter is reapplied AFTER L2 below so
    # any LLM-added duplicates are also caught.
    rule_persona_draft = PersonaDraft(
        seed_messages=list(seed_messages),
        user_goal_summary=user_goal_summary,
        hidden_facts=list(hidden_facts_filtered),
        verbosity=verbosity,
    )

    # Skip the pre-chat form turn before handing transcript to the reviewer;
    # the verbatim-seed validator should only see actual visitor / agent turns.
    transcript_turns_for_llm = [
        t for t in turns
        if not (
            str(t.get("sequence", "")).strip() == "0"
            and t.get("speaker", "") == "[PRE_CHAT_FORM]"
        )
    ]

    hr_context = {
        "primary_uc": primary_uc,
        "secondary_ucs": secondary_ucs,
        "drift_type": drift_type_raw or "none",
        "has_frustration": _parse_bool(row.get("has_frustration", "")),
        "frustration_type": row.get("frustration_type", "") or "none",
        "topic_subject": topic_subject,
        "description": description,
        "source_dataset": source_dataset,
        "turns_filename": turns_file,
    }

    review_result: ReviewResult
    if llm_reviewer is not None:
        review_result = llm_reviewer.review(
            source_session_id=session_id,
            rule_draft=rule_persona_draft,
            transcript_turns=transcript_turns_for_llm,
            hr_context=hr_context,
            case_id_hint=case_id,
        )
    else:
        review_result = ReviewResult(
            accepted_value=rule_persona_draft,
            cache_record=None,
            cache_hit=False,
            used_offline_fallback=True,
        )

    accepted_persona = review_result.accepted_value
    accepted_seed_messages = list(accepted_persona.seed_messages)
    accepted_user_goal_summary = accepted_persona.user_goal_summary
    accepted_verbosity = accepted_persona.verbosity

    # Reapply the redundant-fact filter against form_context in case the
    # LLM added a fact that overlaps an existing form field.
    accepted_hidden_facts_dicts, llm_added_dropped_facts = _filter_redundant_hidden_facts(
        list(accepted_persona.hidden_facts), form_context,
    )
    if llm_added_dropped_facts:
        dropped_hidden_facts = list(dropped_hidden_facts) + [
            f"(post-L2) {f}" for f in llm_added_dropped_facts
        ]

    hidden_facts = [
        HiddenFact(fact=hf["fact"], disclose_when=hf["disclose_when"])
        for hf in accepted_hidden_facts_dicts
    ]

    will_request_human_if = _derive_will_request_human_if(
        row.get("has_frustration", ""),
        row.get("escalation_trigger", ""),
    )
    persona = Persona(
        user_goal_summary=accepted_user_goal_summary,
        frustration_level=frustration_level,
        verbosity=accepted_verbosity,
        drift_behavior=drift_behavior,
        seed_messages=accepted_seed_messages,
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

    spec = CaseSpec(
        case_id=case_id,
        source_session_id=session_id,
        source_dataset=source_dataset,
        form_context=form_context,
        persona=persona,
        expected=expected,
        scoring=scoring,
    )

    # --- L3 application ---
    # Persona override runs AFTER L2 cache and after rule-based persona
    # construction. Expected override runs at the same point as Wave A5.
    persona_changes: dict[str, dict[str, Any]] = {}
    expected_changes: dict[str, dict[str, Any]] = {}
    if applied_override is not None:
        persona_changes = _apply_persona_override(spec, applied_override.persona)
        expected_changes = _apply_expected_override(spec, applied_override.expected)
    expected = spec.expected
    # If persona override changed seeds we want the audit to reflect
    # the final persona, not the pre-override one.
    persona = spec.persona

    case_level_override: dict[str, Any] | None = None
    expected_override_applied = bool(expected_changes)
    persona_override_applied = bool(persona_changes)
    if applied_override is not None:
        case_level_override = {
            "source": applied_override.source,
            "reviewer": applied_override.reviewer,
            "date": applied_override.date,
            "confidence": applied_override.confidence,
            "supporting_turn_numbers": list(applied_override.supporting_turn_numbers),
            "rationale": applied_override.rationale,
            "status": applied_override.status,
            "migrated_from_legacy": applied_override.migrated_from_legacy,
            "case_id_hint": applied_override.case_id_hint,
            "classification": dict(applied_override.classification or {}),
            "expected_changes": expected_changes,
            "persona_changes": persona_changes,
        }

    # --- (8) Audit ---
    mismatches: list[str] = []
    hr_seq = _parse_tool_names(row.get("expected_tool_sequence", ""))
    if hr_seq and hr_seq != expected.expected_tool_sequence:
        mismatches.append(
            f"expected_tool_sequence: HR={hr_seq} vs final={expected.expected_tool_sequence}"
        )
    hr_forbidden = _parse_forbidden_tool_names(row.get("forbidden_tools", ""))
    if hr_forbidden:
        hr_forbidden_no_human = [t for t in hr_forbidden if t not in set(list_human_only_tools())]
        if sorted(hr_forbidden_no_human) != sorted(expected.forbidden_tools):
            mismatches.append(
                f"forbidden_tools: HR(non-human-only)={sorted(hr_forbidden_no_human)} "
                f"vs policy={sorted(expected.forbidden_tools)}"
            )
    if hr_outcome and hr_outcome != expected.outcome_class:
        mismatches.append(
            f"outcome_class: HR={hr_outcome!r} vs final={expected.outcome_class!r}"
        )
    if hr_should_escalate != expected.should_escalate:
        mismatches.append(
            f"should_escalate: HR={hr_should_escalate} vs final={expected.should_escalate}"
        )
    if (
        hr_trigger_raw
        and expected.escalation_trigger
        and hr_trigger_raw != expected.escalation_trigger
    ):
        mismatches.append(
            f"escalation_trigger: HR={hr_trigger_raw!r} "
            f"vs final={expected.escalation_trigger!r}"
        )

    # --- LLM persona-review audit fields (Wave A6) ---
    llm_persona_changed_fields = _diff_persona_fields(
        rule_persona_draft, accepted_persona
    )
    llm_acceptance_reason: str | None = None
    llm_confidence: str | None = None
    llm_validation_notes: list[str] = []
    if review_result.cache_record is not None:
        llm_acceptance_reason = review_result.cache_record.get("acceptance_reason")
        llm_confidence = review_result.cache_record.get("llm_confidence") or None
        notes = review_result.cache_record.get("validation_notes") or []
        if isinstance(notes, list):
            llm_validation_notes = [str(n) for n in notes]

    _record_audit({
        "session_id": session_id,
        "case_id": case_id,
        "hr_primary_uc": hr_primary_uc,
        "final_primary_uc": expected.primary_uc,
        "override_classification_applied": classification_override_applied,
        "override_expected_applied": expected_override_applied,
        "override_persona_applied": persona_override_applied,
        "case_level_override": case_level_override,
        "pending_overrides_for_session": [
            p.rationale or "(no rationale)" for p in pending_for_session
        ],
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
        # Wave A6 LLM persona-review fields
        "llm_cache_hit": bool(review_result.cache_hit),
        "llm_offline_fallback": bool(review_result.used_offline_fallback),
        "llm_acceptance_reason": llm_acceptance_reason,
        "llm_confidence": llm_confidence,
        "llm_validation_notes": llm_validation_notes,
        "llm_persona_changed_fields": llm_persona_changed_fields,
    })

    return spec


def extract_case_specs(
    hr_csv_path: str | Path,
    turns_dir: str | Path,
    output_dir: str | Path,
    overrides_path: str | Path = DEFAULT_CASE_SPEC_OVERRIDES_PATH,
    *,
    llm_cache_dir: str | Path = LLM_REVIEWER_DEFAULT_CACHE_DIR,
    llm_offline: bool = False,
    llm_refresh_sessions: frozenset[str] | set[str] | tuple[str, ...] | list[str] = (),
    llm_model: str = os.environ.get("DEEPSEEK_MODEL", "deepseek-v4-flash"),
    llm_reviewer: LlmPersonaReviewer | None = None,
    strict_overrides: bool = False,
) -> list[CaseSpec]:
    """Extract CaseSpecs from HR annotation CSV and raw turn data.

    Wave A2.1: HR CSV is a HINT, not the source of truth, for any field
    whose owner is the per-UC policy. See module docstring for details.

    Wave A6 (L2 LLM persona review): unless ``llm_offline=True``, the
    extractor instantiates an :class:`LlmPersonaReviewer` against
    ``llm_cache_dir`` and uses it to refine ``persona.seed_messages``,
    ``persona.user_goal_summary``, ``persona.hidden_facts``, and
    ``persona.verbosity``. Wave A5 overrides still run AFTER L2 and have
    final word. Pass ``llm_refresh_sessions`` to force a re-call for
    specific source_session_ids; pass ``llm_reviewer`` directly for
    tests / custom transports.

    Wave A6.6 (unified override registry): the override file is keyed by
    ``source_session_id`` alone and may contain ``status: approved`` or
    ``status: pending_review`` entries. Approved entries are applied at
    the appropriate pipeline stage (classification before policy lookup,
    expected post-L1, persona post-L2). Pending entries do NOT apply by
    default; pass ``strict_overrides=True`` to hard-fail on any spec whose
    session matches a pending entry.
    """
    hr_csv_path = Path(hr_csv_path)
    turns_dir = Path(turns_dir)
    output_dir = Path(output_dir)

    if not hr_csv_path.exists():
        raise FileNotFoundError(f"HR CSV not found: {hr_csv_path}")
    if not turns_dir.exists():
        raise FileNotFoundError(f"Turns directory not found: {turns_dir}")

    _reset_audit()
    override_registry = _load_case_spec_overrides(overrides_path)
    logger.info(
        "Loaded %d approved + %d pending case-spec override(s).",
        len(override_registry.applied),
        len(override_registry.pending),
    )

    if llm_reviewer is None:
        llm_reviewer = LlmPersonaReviewer(
            model=llm_model,
            cache_dir=Path(llm_cache_dir),
            offline=bool(llm_offline),
            force_refresh_sessions=frozenset(llm_refresh_sessions),
        )

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

        applied_override = override_registry.applied.get(session_id)
        spec = _build_case_spec(
            row,
            turns,
            idx,
            turns_filename,
            applied_override=applied_override,
            pending_overrides=override_registry.pending,
            strict_overrides=strict_overrides,
            llm_reviewer=llm_reviewer,
        )

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
