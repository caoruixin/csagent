"""Proposer — turns a `FailureTaxonomy` + lessons + recent iterations
into a single concrete `Hypothesis` for the applier to act on.

Discipline (enforced both in `prompts/propose.txt` AND in the
post-parse validator below):

- `target_skill_file` MUST be one of the six allowed Skill YAMLs.
- `target_field_path` MUST match one of the four allowed field
  patterns (with `[*]` matching a concrete integer index).
- `before_value` and `after_value` MUST be different (an empty
  diff is rejected by the sandbox anyway).
- The proposer retries up to 3 times on invalid JSON; if all
  three retries fail, `ProposerInvalidOutputError` is raised so
  the orchestrator can record `discard_reason
  = proposer_llm_returned_invalid_json` and move on.
"""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from .. import config_validator as _config_validator
from .analyzer import FailureTaxonomy
from .llm_client import LLMClient, LLMClientError


_PROMPT_PATH = Path(__file__).resolve().parent / "prompts" / "propose.txt"
_REPO_ROOT = Path(__file__).resolve().parents[3]

# P1 (S-Y1.5): when `lessons.enabled` is false, the LESSONS_MD block is
# replaced with this placeholder — NOT an empty string — so the proposer
# is told the lessons were intentionally withheld for this run rather
# than being silently absent. Preserves the file; only the projection
# into the propose prompt changes.
_LESSONS_DISABLED_PLACEHOLDER = (
    "<lessons disabled for this run — historical lessons may not reflect "
    "the active pilot's targets>"
)


class ProposerInvalidOutputError(Exception):
    """Raised after exhausting the JSON-parse retry budget."""

    def __init__(self, message: str, *, attempts: int, last_output: str):
        super().__init__(message)
        self.attempts = attempts
        self.last_output = last_output


@dataclass
class Hypothesis:
    """One proposed Skill YAML edit.

    Fields:
        target_skill_file — repo-relative path to one of the six
            allowed Skill YAMLs.
        target_field_path — JSONPath-ish AST path (e.g. `$.procedure`,
            `$.critical_steps[2].desc`).
        before_value — the EXACT current text of that field, copied
            from the live file. Used by the applier to verify the
            file hasn't drifted between propose and apply.
        after_value — the proposed new text.
        rationale — 1–3 sentence justification.
        fingerprint — deterministic hash for anti-repeat detection.
        attempts_used — number of LLM round-trips the proposer
            consumed to produce this hypothesis (1 if first try
            parsed; 2 or 3 on retries).
    """

    target_skill_file: str
    target_field_path: str
    before_value: str
    after_value: str
    rationale: str
    # S-Auto-41 (R-autoloop-feedback-loop-thinness follow-up): proposer-
    # steering fields. REQUIRED only when `primary_target_steering.enabled`
    # (the active PRIMARY-target pilot); empty + unvalidated otherwise so
    # legacy / pre-pilot runs are byte-for-byte unchanged.
    #   causal_hypothesis — the causal chain connecting THIS edit to a target
    #     PRIMARY failure mechanism (e.g. UC-A→UC-B misclass / superficial
    #     listing use / budget escalation before grounded resolution).
    #   expected_trace_change — the expected trace-level behaviour change if
    #     the edit works (what a passing trace would now show).
    causal_hypothesis: str = ""
    expected_trace_change: str = ""
    fingerprint: str = ""
    attempts_used: int = 1
    raw_llm_response: str = field(default="", repr=False)


def propose(
    taxonomy: FailureTaxonomy,
    lessons: str,
    recent_iterations: list[dict[str, Any]],
    *,
    client: LLMClient,
    config: dict[str, Any],
    max_retries: int = 3,
    pilot: dict[str, Any] | None = None,
    skill_phase_usecase_map: dict[str, dict[str, list[str]]] | None = None,
) -> Hypothesis:
    """Build a `Hypothesis` from the analyzer's taxonomy.

    Retries up to `max_retries` times if the LLM output cannot be
    parsed OR fails the structural-validation rules (target outside
    surface; missing fields; empty diff). Each retry tightens the
    "respond in JSON" reminder.

    `pilot` + `skill_phase_usecase_map` (S-Y1.5) add the active pilot's
    PRIMARY TARGETS + per-skill phase/UC declarations to the prompt so
    the proposer can bias toward a phase/UC-correct skill. Both default
    to empty, preserving the pre-pilot prompt byte-for-byte.

    Raises ProposerInvalidOutputError if all retries fail.
    """
    surface_cfg = (config or {}).get("mutable_surface") or {}
    allowed_files: list[str] = surface_cfg.get("allowed_skill_files") or []
    allowed_paths: list[str] = surface_cfg.get("allowed_field_paths") or []

    # P1 (S-Y1.5): lessons opt-out. When false, withhold the lessons body
    # behind a placeholder (not empty) — the file + compactor are untouched.
    lessons_cfg = (config or {}).get("lessons") or {}
    lessons_enabled = bool(lessons_cfg.get("enabled", True))
    effective_lessons = lessons if lessons_enabled else _LESSONS_DISABLED_PLACEHOLDER

    # S-Auto-41: PRIMARY-target proposer steering. When enabled, inject the
    # target baseline evidence + observed failure clusters into the prompt and
    # require a causal hypothesis + expected trace change.
    steering = (config or {}).get("primary_target_steering") or {}
    require_causal = bool(steering.get("enabled"))

    system = _PROMPT_PATH.read_text(encoding="utf-8")
    base_user = _build_user_input(
        taxonomy,
        effective_lessons,
        recent_iterations,
        allowed_files,
        pilot=pilot or {},
        skill_phase_usecase_map=skill_phase_usecase_map or {},
        steering=steering,
    )

    last_text = ""
    last_error = ""
    for attempt in range(1, max_retries + 1):
        prompt_user = base_user
        if attempt > 1:
            prompt_user = (
                f"PRIOR ATTEMPT FAILED with: {last_error}\n"
                "RESPOND with raw JSON matching the schema. No prose. No "
                "markdown fences.\n\n" + base_user
            )
        try:
            response = client.chat(system=system, user=prompt_user)
        except LLMClientError as e:
            last_error = f"llm_client_error: {e}"
            last_text = ""
            continue
        last_text = response.text
        parsed = _safe_parse_json(last_text)
        if parsed is None:
            last_error = "llm_returned_unparseable_json"
            continue
        try:
            hyp = _validate_and_build(
                parsed,
                allowed_files=allowed_files,
                allowed_paths=allowed_paths,
                attempts_used=attempt,
                raw_response=last_text,
                require_causal=require_causal,
            )
        except ValueError as e:
            last_error = str(e)
            continue
        return hyp

    raise ProposerInvalidOutputError(
        "proposer_llm_returned_invalid_json",
        attempts=max_retries,
        last_output=last_text,
    )


def fingerprint_hypothesis(hyp: Hypothesis) -> str:
    """Deterministic hash of (target_skill, target_field, after_value).

    `before_value` is excluded so that the same intended edit
    against a slightly-different baseline still fingerprints
    identically — the proposer cares about "where + what" not
    "what was there before".
    """
    normalized = "|".join([
        hyp.target_skill_file.strip(),
        hyp.target_field_path.strip(),
        hyp.after_value.strip(),
    ]).encode("utf-8")
    return hashlib.sha256(normalized).hexdigest()[:16]


def _build_user_input(
    taxonomy: FailureTaxonomy,
    lessons: str,
    recent_iterations: list[dict[str, Any]],
    allowed_files: list[str],
    *,
    pilot: dict[str, Any] | None = None,
    skill_phase_usecase_map: dict[str, dict[str, list[str]]] | None = None,
    steering: dict[str, Any] | None = None,
) -> str:
    """Bundle the taxonomy + lessons + recent iters + current YAML.

    Reads the six allowed Skill YAML files from disk so the LLM can
    write a verbatim `before_value`. Keeping the YAML inline in the
    propose-prompt is acceptable for v1 (total < 30K tokens).
    """
    skill_content_blocks: list[str] = []
    for rel in allowed_files:
        abs_path = _REPO_ROOT / rel
        if not abs_path.exists():
            continue
        try:
            text = abs_path.read_text(encoding="utf-8")
        except OSError:
            continue
        skill_content_blocks.append(f"### FILE: {rel}\n```yaml\n{text}\n```")

    parts = [
        "FAILURE_TAXONOMY:",
        json.dumps(taxonomy, indent=2, default=str),
        "",
        "LESSONS_MD:",
        lessons or "<no lessons yet>",
        "",
        "RECENT_ITERATIONS:",
        json.dumps(
            [
                {
                    "iteration_id": r.get("iteration_id"),
                    "target_skill": _extract_target_skill(r),
                    "target_field": _extract_target_field(r),
                    "decision": r.get("decision"),
                    "discard_reason": r.get("discard_reason"),
                    # P0-A (S-Y1.5): pass the per-layer tier_breakdown so the
                    # proposer can see WHICH cases a prior candidate regressed,
                    # not just the one-line discard_reason string.
                    "tier_breakdown": (r.get("verdict") or {}).get(
                        "tier_breakdown", {}
                    ),
                }
                for r in recent_iterations
            ],
            indent=2,
            default=str,
        ),
        "",
    ]
    # P0-C (S-Y1.5): pilot PRIMARY TARGETS + per-skill phase/UC map. Rendered
    # only when present, so a pre-pilot run's prompt is unchanged.
    parts.extend(
        _config_validator.render_pilot_input_blocks(
            pilot or {}, skill_phase_usecase_map or {}
        )
    )
    # S-Auto-41: PRIMARY-target steering block (baseline evidence + observed
    # failure clusters + the causal-hypothesis requirement). Rendered only
    # when the steering config is present, so non-pilot prompts are unchanged.
    parts.extend(
        _config_validator.render_primary_target_steering_block(steering or {})
    )
    parts.extend(
        [
            "TARGET_SKILL_FILES_CONTENT:",
            "\n\n".join(skill_content_blocks) or "<no skill files found on disk>",
        ]
    )
    return "\n".join(parts)


def _extract_target_skill(record: dict[str, Any]) -> str | None:
    hyp = record.get("hypothesis") or {}
    return hyp.get("target_skill_file")


def _extract_target_field(record: dict[str, Any]) -> str | None:
    hyp = record.get("hypothesis") or {}
    return hyp.get("target_field_path")


def _safe_parse_json(text: str) -> dict[str, Any] | None:
    stripped = text.strip()
    if stripped.startswith("```"):
        stripped = re.sub(r"^```(?:json)?\s*", "", stripped, flags=re.IGNORECASE)
        stripped = re.sub(r"\s*```\s*$", "", stripped)
    if not stripped:
        return None
    try:
        return json.loads(stripped)
    except json.JSONDecodeError:
        return None


def _validate_and_build(
    parsed: dict[str, Any],
    *,
    allowed_files: list[str],
    allowed_paths: list[str],
    attempts_used: int,
    raw_response: str,
    require_causal: bool = False,
) -> Hypothesis:
    """Structural validation of the LLM JSON output.

    ``require_causal`` (S-Auto-41) enforces the steering fields
    ``causal_hypothesis`` + ``expected_trace_change`` when the active
    PRIMARY-target pilot is on; otherwise they are optional pass-throughs.
    """
    if not isinstance(parsed, dict):
        raise ValueError("output_is_not_an_object")

    required = (
        "target_skill_file",
        "target_field_path",
        "before_value",
        "after_value",
        "rationale",
    )
    for k in required:
        if k not in parsed:
            raise ValueError(f"missing_field_{k}")
        if not isinstance(parsed[k], str):
            raise ValueError(f"field_{k}_must_be_string")

    causal_hypothesis = str(parsed.get("causal_hypothesis", "") or "").strip()
    expected_trace_change = str(parsed.get("expected_trace_change", "") or "").strip()
    if require_causal:
        if not causal_hypothesis:
            raise ValueError("missing_field_causal_hypothesis")
        if not expected_trace_change:
            raise ValueError("missing_field_expected_trace_change")

    target_skill = parsed["target_skill_file"].strip()
    target_field = parsed["target_field_path"].strip()
    before_value = parsed["before_value"]
    after_value = parsed["after_value"]
    rationale = parsed["rationale"].strip()

    if target_skill not in allowed_files:
        raise ValueError(
            f"target_skill_file_outside_mutable_surface:{target_skill}"
        )

    if not _path_matches_any(target_field, allowed_paths):
        raise ValueError(
            f"target_field_path_outside_mutable_surface:{target_field}"
        )

    if before_value == after_value:
        raise ValueError("before_and_after_values_identical")

    hyp = Hypothesis(
        target_skill_file=target_skill,
        target_field_path=target_field,
        before_value=before_value,
        after_value=after_value,
        rationale=rationale,
        causal_hypothesis=causal_hypothesis,
        expected_trace_change=expected_trace_change,
        attempts_used=attempts_used,
        raw_llm_response=raw_response,
    )
    hyp.fingerprint = fingerprint_hypothesis(hyp)
    return hyp


def _path_matches_any(path: str, patterns: list[str]) -> bool:
    """Concrete `$.critical_steps[2].desc` matches `$.critical_steps[*].desc`."""
    for pat in patterns:
        regex_parts = pat.split("[*]")
        regex = r"\[\d+\]".join(re.escape(p) for p in regex_parts)
        if re.match(f"^{regex}$", path):
            return True
    return False
