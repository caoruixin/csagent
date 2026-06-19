"""Pilot-config surface for the auto-evolution loop (S-Y1.5 / M-Auto-7).

This module is the single home for the pure helpers that operationalize
the `pilot:` block in `autoloop/config.yaml`:

- `validate_pilot_config` — load the `pilot` block and REJECT any pilot
  case_id that collides with a shadow case_spec filename. The shadow
  firewall is honored: filenames only are read, never file contents.
- `build_skill_phase_usecase_map` — lazily read (NO cache) the
  `applicable_phases` / `applicable_use_cases` keys of the six allowed
  Skill YAMLs, for phase/UC-correct skill-selection steering.
- `build_pilot_snapshot` — a forensic per-iteration snapshot of the
  active pilot targets + a 16-char content hash, embedded in each
  experiments.jsonl row + dry-run hypothesis.json so an auditor never
  has to reconstruct which target card was live.
- `compute_pilot_hit_rates` / `iteration_hits` — the 4-layer hit-rate
  decomposition (phase/UC, skill, field-family, full-on-gap) surfaced
  by `autoloop report` / `audit`. Forensic / observation-only; touches
  no gate or baseline.

Everything here is stdlib + PyYAML only, so both `loop.py` and `cli.py`
can import it without paying the heavy import cost of the loop module.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import yaml


# The CS4 entity-context guidance most plausibly lands in
# resolve_faq_grounded_answer.yaml (UC-A / RESOLVE). The skill-hit layer
# of the 4-layer hit-rate audit scores against this skill basename.
_PILOT_SUCCESS_SKILL_BASENAME = "resolve_faq_grounded_answer.yaml"

# Field-family the CS4 entity-context guidance would land in. NOT
# $.escalation_policy (an edit there cannot help a RESOLVE-phase success
# path). Measured, never prescribed (no field-level steering in prompts).
_PILOT_FIELD_FAMILY = frozenset(
    {"$.procedure", "$.grounding_instruction", "$.critical_steps[*].desc"}
)


class PilotConfigError(Exception):
    """Raised when the `pilot` block is structurally invalid — currently
    only when a pilot case_id collides with a shadow case_spec filename
    (the shadow-firewall fence)."""


def shadow_case_ids(repo_root: Path) -> set[str]:
    """Return the set of shadow case-id stems under
    `eval_interactive/case_specs_shadow/`.

    FILENAMES ONLY — the file *contents* are never opened (shadow
    firewall). `_*.yaml` manifest / access-boundary files are excluded.
    The stem (filename without the `.yaml` suffix) is used as the
    case-id, since the loop is forbidden from reading the case_spec body
    to learn its declared id.
    """
    shadow_dir = Path(repo_root) / "eval_interactive" / "case_specs_shadow"
    out: set[str] = set()
    if not shadow_dir.exists():
        return out
    for p in shadow_dir.rglob("*.yaml"):
        if p.name.startswith("_"):
            continue
        out.add(p.stem)
    return out


def validate_pilot_config(
    config: dict[str, Any], repo_root: Path
) -> dict[str, Any]:
    """Load + validate the `pilot` block.

    Returns the pilot block dict, or `{}` when no `pilot` block is
    present (pre-S-Y1.5 behaviour — fully backward compatible; an absent
    block means empty PILOT_PRIMARY_TARGETS downstream).

    Raises `PilotConfigError` if any case_id listed in
    `primary_targets` / `anti_kill_control` / `tier2_neighbors` collides
    with a shadow case_spec filename (filenames only — never contents).
    """
    pilot = (config or {}).get("pilot") or {}
    if not pilot:
        return {}
    shadow = shadow_case_ids(repo_root)
    collisions: list[str] = []
    for key in ("primary_targets", "anti_kill_control", "tier2_neighbors"):
        for cid in pilot.get(key) or []:
            if cid in shadow:
                collisions.append(f"{key}:{cid}")
    if collisions:
        raise PilotConfigError(
            "pilot case_id(s) collide with shadow case_specs (firewall): "
            + ", ".join(collisions)
        )
    return pilot


def build_skill_phase_usecase_map(
    allowed_skill_files: list[str], repo_root: Path
) -> dict[str, dict[str, list[str]]]:
    """Lazily read the `applicable_phases` / `applicable_use_cases` keys
    of each allowed Skill YAML.

    NO cache — re-read fresh every call so a mid-run edit to a Skill's
    phase/UC declaration is reflected immediately. Only the two keys are
    extracted; the rest of the YAML is ignored. Missing / unreadable
    files contribute an empty entry rather than raising.
    """
    out: dict[str, dict[str, list[str]]] = {}
    for rel in allowed_skill_files or []:
        abs_path = Path(repo_root) / rel
        phases: list[str] = []
        use_cases: list[str] = []
        if abs_path.exists():
            try:
                data = yaml.safe_load(abs_path.read_text(encoding="utf-8")) or {}
            except (OSError, yaml.YAMLError):
                data = {}
            if isinstance(data, dict):
                phases = [str(x) for x in (data.get("applicable_phases") or [])]
                use_cases = [str(x) for x in (data.get("applicable_use_cases") or [])]
        out[rel] = {"applicable_phases": phases, "applicable_use_cases": use_cases}
    return out


def build_pilot_snapshot(
    pilot: dict[str, Any], lessons_enabled: bool
) -> dict[str, Any]:
    """Build a forensic per-iteration snapshot of the active pilot card.

    Contains the schema_version, the three case-id lists, the UC / phase
    hints, the `lessons_enabled` flag, and a 16-char `block_sha256` over
    the normalized JSON of the content (excluding the hash itself).
    Forensic-only — never read by the gate / baseline.
    """
    content = {
        "schema_version": (pilot or {}).get("schema_version"),
        "active_sprint": (pilot or {}).get("active_sprint"),
        "primary_targets": list((pilot or {}).get("primary_targets") or []),
        "anti_kill_control": list((pilot or {}).get("anti_kill_control") or []),
        "tier2_neighbors": list((pilot or {}).get("tier2_neighbors") or []),
        "phase_hint": list((pilot or {}).get("phase_hint") or []),
        "use_case_hint": list((pilot or {}).get("use_case_hint") or []),
        "lessons_enabled": bool(lessons_enabled),
    }
    normalized = json.dumps(content, sort_keys=True, separators=(",", ":"))
    block_sha = hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]
    return {**content, "block_sha256": block_sha}


def render_pilot_input_blocks(
    pilot: dict[str, Any],
    skill_map: dict[str, dict[str, list[str]]],
) -> list[str]:
    """Render the `PILOT_PRIMARY_TARGETS` + `SKILL_PHASE_USECASE_MAP`
    prompt blocks shared by the analyzer + proposer user inputs.

    Each block is rendered ONLY when its source dict is non-empty, so a
    pre-S-Y1.5 run (no `pilot` block) produces a byte-identical prompt to
    the legacy path. Returns a list of lines to splice into `parts`.
    """
    blocks: list[str] = []
    if pilot:
        pilot_ctx = {
            "schema_version": pilot.get("schema_version"),
            "active_sprint": pilot.get("active_sprint"),
            "primary_targets": list(pilot.get("primary_targets") or []),
            "anti_kill_control": list(pilot.get("anti_kill_control") or []),
            "tier2_neighbors": list(pilot.get("tier2_neighbors") or []),
            "phase_hint": list(pilot.get("phase_hint") or []),
            "use_case_hint": list(pilot.get("use_case_hint") or []),
        }
        blocks.append("PILOT_PRIMARY_TARGETS:")
        blocks.append(json.dumps(pilot_ctx, indent=2, default=str))
        blocks.append("")
    if skill_map:
        blocks.append("SKILL_PHASE_USECASE_MAP:")
        blocks.append(json.dumps(skill_map, indent=2, default=str))
        blocks.append("")
    return blocks


# --- S-Auto-41 PRIMARY-target proposer steering -----------------------
#
# Narrowly-scoped follow-up under R-autoloop-feedback-loop-thinness: enrich
# the proposer's INPUT only (target baseline evidence + observed failure
# clusters + a required causal hypothesis), and add a lightweight OFF_TARGET
# pre-check before the expensive fitness eval. Does NOT touch the keep gate,
# conditional rules, baseline, or canonical pointer. Proposer-side only.


@dataclass
class OffTargetVerdict:
    """Result of the lightweight pre-eval steering check."""

    on_target: bool
    reason: str


def render_primary_target_steering_block(steering: dict[str, Any]) -> list[str]:
    """Render the ``PRIMARY_TARGET_STEERING`` prompt block.

    Rendered only when ``steering.enabled`` is truthy, so a non-pilot run's
    prompt is byte-identical to the legacy path. Carries (1) the target
    PRIMARY cases + their current baseline evidence + closure intent, (2) the
    observed failure clusters, and tells the proposer (3) a causal hypothesis
    and (4) an expected trace-level change are REQUIRED.
    """
    if not (steering or {}).get("enabled"):
        return []
    ctx = {
        "instruction": (
            "You are steering toward the PRIMARY targets below. Your edit MUST "
            "be causally connected to one of the observed failure mechanisms. "
            "Output `causal_hypothesis` (how this edit changes the failure "
            "mechanism) and `expected_trace_change` (what a passing trace would "
            "now show) — both REQUIRED. Treat tool-returned AND pre-loaded "
            "customer context as equally valid provenance; require SUBSTANTIVE "
            "use of listing-specific information rather than any specific tool "
            "call."
        ),
        "primary_targets": steering.get("targets") or [],
        "observed_failure_clusters": steering.get("failure_clusters") or [],
        "primary_relevant_skills": steering.get("primary_relevant_skills") or [],
    }
    return [
        "PRIMARY_TARGET_STEERING:",
        json.dumps(ctx, indent=2, default=str),
        "",
    ]


def off_target_precheck(
    hypothesis: Any, steering: dict[str, Any]
) -> OffTargetVerdict:
    """Lightweight, LLM-free pre-eval steering check.

    A proposal is ON_TARGET only when BOTH hold:
      (a) it edits a PRIMARY-relevant skill file (declared in the steering
          config), AND
      (b) its causal hypothesis (or rationale) references at least one of the
          declared on-target keywords (target case ids / UC tokens / failure-
          mechanism terms — config-driven, not hard-coded in code).

    Anything else is OFF_TARGET → the orchestrator discards it cheaply,
    before the expensive apply + fitness eval. When steering is disabled this
    is a no-op (always ON_TARGET), preserving legacy behaviour.
    """
    if not (steering or {}).get("enabled"):
        return OffTargetVerdict(True, "steering_disabled")

    relevant = set(steering.get("primary_relevant_skills") or [])
    keywords = [str(k).lower() for k in (steering.get("on_target_keywords") or [])]
    target_skill = (getattr(hypothesis, "target_skill_file", "") or "").strip()
    if relevant and target_skill not in relevant:
        return OffTargetVerdict(
            False, f"skill_not_primary_relevant:{target_skill}"
        )

    haystack = " ".join(
        [
            getattr(hypothesis, "causal_hypothesis", "") or "",
            getattr(hypothesis, "expected_trace_change", "") or "",
            getattr(hypothesis, "rationale", "") or "",
        ]
    ).lower()
    if keywords and not any(kw in haystack for kw in keywords):
        return OffTargetVerdict(
            False, "causal_hypothesis_misses_all_on_target_keywords"
        )
    return OffTargetVerdict(True, "on_target")


# --- 4-layer hit-rate audit (forensic / observation-only) ------------


def _field_family_match(target_field: str) -> bool:
    """True if `target_field` is in the pilot field-family (procedure /
    grounding_instruction / critical_steps[*].desc), matching a concrete
    `critical_steps[N].desc` against the `[*]` pattern."""
    if target_field in _PILOT_FIELD_FAMILY:
        return True
    # `$.critical_steps[2].desc` collapses to the `[*]` family member.
    import re

    if re.match(r"^\$\.critical_steps\[\d+\]\.desc$", target_field or ""):
        return True
    return False


def _phase_usecase_match(
    target_skill: str,
    skill_map: dict[str, dict[str, list[str]]],
    pilot: dict[str, Any],
) -> bool:
    """True if the targeted skill's declared phases / use-cases intersect
    the pilot phase_hint / use_case_hint. A `*` wildcard in the skill's
    declaration matches any hint."""
    entry = (skill_map or {}).get(target_skill) or {}
    skill_phases = set(entry.get("applicable_phases") or [])
    skill_ucs = set(entry.get("applicable_use_cases") or [])
    phase_hint = set((pilot or {}).get("phase_hint") or [])
    uc_hint = set((pilot or {}).get("use_case_hint") or [])

    phase_ok = (
        not phase_hint
        or "*" in skill_phases
        or bool(skill_phases & phase_hint)
    )
    uc_ok = (
        not uc_hint
        or "*" in skill_ucs
        or bool(skill_ucs & uc_hint)
    )
    return phase_ok and uc_ok


def iteration_hits(
    record: dict[str, Any],
    skill_map: dict[str, dict[str, list[str]]],
    pilot: dict[str, Any],
) -> dict[str, bool]:
    """Decompose one iteration record into the 4 hit-layers.

    A single `primary_target_hit` boolean is a known false-positive trap
    (an edit can hit the right skill on the wrong field). The four
    independent layers + `full_on_gap` (all three substantive layers)
    are scored separately; `full_on_gap` is the S-Y2 primary success
    metric.
    """
    hyp = record.get("hypothesis") or {}
    target_skill = hyp.get("target_skill_file") or ""
    target_field = hyp.get("target_field_path") or ""

    phase_usecase = _phase_usecase_match(target_skill, skill_map, pilot)
    skill = target_skill.endswith(_PILOT_SUCCESS_SKILL_BASENAME)
    field_family = _field_family_match(target_field)
    return {
        "phase_usecase_hit": phase_usecase,
        "skill_hit": skill,
        "field_family_hit": field_family,
        "full_on_gap_hit": bool(phase_usecase and skill and field_family),
    }


def compute_pilot_hit_rates(
    records: list[dict[str, Any]],
    skill_map: dict[str, dict[str, list[str]]],
    pilot: dict[str, Any],
) -> dict[str, Any]:
    """Aggregate the 4-layer hit-rate over all records that carry a
    `hypothesis` (records that never reached the proposer are skipped).

    Returns the four rates + a venn-style `partial_hit_breakdown`
    (counts of records matching each subset of the three substantive
    layers). Forensic-only; touches no gate.
    """
    scored = [r for r in records if (r.get("hypothesis") or {}).get("target_skill_file")]
    n = len(scored)
    if n == 0:
        return {
            "scored_iterations": 0,
            "phase_usecase_hit_rate": None,
            "skill_hit_rate": None,
            "field_family_hit_rate": None,
            "full_on_gap_hit_rate": None,
            "partial_hit_breakdown": {},
        }

    counts = {
        "phase_usecase": 0,
        "skill": 0,
        "field_family": 0,
        "full_on_gap": 0,
    }
    venn: dict[str, int] = {}
    for r in scored:
        h = iteration_hits(r, skill_map, pilot)
        if h["phase_usecase_hit"]:
            counts["phase_usecase"] += 1
        if h["skill_hit"]:
            counts["skill"] += 1
        if h["field_family_hit"]:
            counts["field_family"] += 1
        if h["full_on_gap_hit"]:
            counts["full_on_gap"] += 1
        key = "+".join(
            name
            for name, flag in (
                ("phase_usecase", h["phase_usecase_hit"]),
                ("skill", h["skill_hit"]),
                ("field_family", h["field_family_hit"]),
            )
            if flag
        ) or "none"
        venn[key] = venn.get(key, 0) + 1

    return {
        "scored_iterations": n,
        "phase_usecase_hit_rate": counts["phase_usecase"] / n,
        "skill_hit_rate": counts["skill"] / n,
        "field_family_hit_rate": counts["field_family"] / n,
        "full_on_gap_hit_rate": counts["full_on_gap"] / n,
        "partial_hit_breakdown": venn,
    }
