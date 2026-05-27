"""Failure analyzer — turns a baseline results.json + lessons + recent
iterations into a structured `FailureTaxonomy` for the proposer.

The analyzer does NOT propose any fix. It produces a sanitized
description of the failure landscape — what is currently failing,
which Skill × step is most often advisory-FAIL, which bad_cases /
anchor_outcome cases are missing their closure_criterion. The
proposer consumes this taxonomy + lessons + recent iterations.

Sanitization rules (enforced in the prompt + post-process):

- `summary` MUST NOT contain case_ids.
- `failure_shape` MUST be a generic one-line description.
- No raw customer message text leaks into any field.
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any, TypedDict

from .llm_client import LLMClient


# Match e.g. "uc-a-002", "bc-13", "ao-007", "shadow-014" — the auto-loop
# case-id shapes. Used to scrub `summary` post-hoc as a defensive sweep
# against the analyzer leaking ids into prose.
_CASE_ID_PATTERN = re.compile(
    r"\b(uc-[a-z]-\d+|bc-\d+|ao-\d+|shadow-\d+|case-\d+|[a-f0-9]{8}-[a-f0-9]{4})\b",
    re.IGNORECASE,
)


class FailureTaxonomy(TypedDict, total=False):
    """The structured failure landscape consumed by the proposer.

    Keys:
        skills_critical_steps_advisory_fail:
            {skill_name: {step_id: {fail_count: int, in_cases: [case_id]}}}
        bad_cases_regressing:
            {case_id: {primary_uc: str, failure_shape: str}}
        anchor_outcome_closure_criterion_fails:
            {case_id: {primary_uc: str, closure_criterion_snippet: str}}
        summary: str  (no case_ids; one paragraph)
    """

    skills_critical_steps_advisory_fail: dict[str, dict[str, dict[str, Any]]]
    bad_cases_regressing: dict[str, dict[str, Any]]
    anchor_outcome_closure_criterion_fails: dict[str, dict[str, Any]]
    summary: str


_PROMPT_PATH = Path(__file__).resolve().parent / "prompts" / "analyze.txt"


def analyze(
    baseline_results_summary: dict[str, Any],
    lessons_md: str,
    recent_iterations: list[dict[str, Any]],
    *,
    client: LLMClient,
) -> FailureTaxonomy:
    """Run the analyzer LLM and parse its JSON output.

    `baseline_results_summary` is a pre-aggregated dict the loop
    builds from the most recent eval run; the analyzer does not
    re-read raw results.json. Keeping the analyzer LLM input
    bounded matters for context-window cost.
    """
    system = _PROMPT_PATH.read_text(encoding="utf-8")
    user = _build_user_input(
        baseline_results_summary, lessons_md, recent_iterations
    )
    response = client.chat(system=system, user=user)
    parsed = _safe_parse_json(response.text)
    if parsed is None:
        # Analyzer is best-effort; an unparseable response should not
        # crash the iteration. Fall back to an empty taxonomy + a
        # one-line summary explaining the parse failure.
        return _empty_taxonomy(
            summary="analyzer_returned_unparseable_json_fallback_to_empty_taxonomy"
        )
    return _sanitize_taxonomy(parsed)


def build_baseline_summary(results_paths: dict[str, Path]) -> dict[str, Any]:
    """Aggregate per-suite results.json into the dict the analyzer reads.

    `results_paths` maps suite_name -> per-suite results.json Path.
    Missing suites are skipped (the analyzer is robust to missing
    keys). Per-case shadow detail is NEVER included in the output —
    the firewall in tier_evaluator already keeps the loop blind to
    that data, and the analyzer must remain on the same side of
    that firewall.
    """
    out: dict[str, Any] = {}
    for suite_name, path in results_paths.items():
        # Shadow suite is special: only aggregate counts flow into the
        # analyzer. Per-case shadow detail is forbidden in this surface.
        if suite_name == "shadow":
            data = _safe_read_json(path) if path.exists() else {}
            cases = (data or {}).get("case_results") or []
            passed = sum(1 for c in cases if c.get("case_passed") is True)
            out[suite_name] = {
                "total_cases": len(cases),
                "passed_cases": passed,
                "failed_cases": len(cases) - passed,
            }
            continue
        if not path.exists():
            continue
        data = _safe_read_json(path)
        if not data:
            continue
        cases = data.get("case_results") or []
        suite_block: dict[str, Any] = {
            "total_cases": len(cases),
            "passed_cases": sum(1 for c in cases if c.get("case_passed") is True),
            "per_case": [
                {
                    "case_id": c.get("case_id"),
                    "primary_uc": c.get("primary_uc"),
                    "case_passed": c.get("case_passed"),
                    "tier2_mandatory_failures": _tier2_mandatory_fail_steps(c),
                    "failure_shape": c.get("failure_shape"),
                }
                for c in cases
                if c.get("case_passed") is not True
            ],
        }
        out[suite_name] = suite_block
    return out


def _tier2_mandatory_fail_steps(case: dict[str, Any]) -> list[str]:
    tier2 = case.get("tier2_result") or {}
    per_step = tier2.get("per_step") or []
    return [
        s.get("step_id", "<unknown>")
        for s in per_step
        if s.get("severity") == "mandatory" and s.get("outcome") == "FAIL"
    ]


def _safe_read_json(path: Path) -> dict[str, Any] | None:
    try:
        with path.open("r", encoding="utf-8") as f:
            return json.load(f)
    except (OSError, json.JSONDecodeError):
        return None


def _build_user_input(
    baseline_summary: dict[str, Any],
    lessons_md: str,
    recent_iterations: list[dict[str, Any]],
) -> str:
    parts = [
        "BASELINE_RESULTS_SUMMARY:",
        json.dumps(baseline_summary, indent=2, default=str),
        "",
        "LESSONS_MD:",
        lessons_md or "<no lessons yet>",
        "",
        "RECENT_ITERATIONS_SUMMARY:",
        json.dumps(
            [
                {
                    "iteration_id": r.get("iteration_id"),
                    "target_skill": _extract_target_skill(r),
                    "target_field": _extract_target_field(r),
                    "decision": r.get("decision"),
                    "discard_reason": r.get("discard_reason"),
                }
                for r in recent_iterations
            ],
            indent=2,
            default=str,
        ),
    ]
    return "\n".join(parts)


def _extract_target_skill(record: dict[str, Any]) -> str | None:
    hyp = record.get("hypothesis") or {}
    return hyp.get("target_skill_file")


def _extract_target_field(record: dict[str, Any]) -> str | None:
    hyp = record.get("hypothesis") or {}
    return hyp.get("target_field_path")


def _safe_parse_json(text: str) -> dict[str, Any] | None:
    """Best-effort JSON parsing — strip ``` fences if the LLM
    accidentally added them, return None on failure.
    """
    stripped = text.strip()
    if stripped.startswith("```"):
        # Strip ```json ... ``` or ``` ... ``` fences.
        stripped = re.sub(r"^```(?:json)?\s*", "", stripped, flags=re.IGNORECASE)
        stripped = re.sub(r"\s*```\s*$", "", stripped)
    try:
        return json.loads(stripped)
    except json.JSONDecodeError:
        return None


def _sanitize_taxonomy(parsed: dict[str, Any]) -> FailureTaxonomy:
    """Post-process LLM output: scrub case_ids from `summary` and
    coerce missing top-level keys to empty dicts.
    """
    summary = parsed.get("summary", "")
    if isinstance(summary, str):
        summary = _CASE_ID_PATTERN.sub("<case>", summary)
    return FailureTaxonomy(
        skills_critical_steps_advisory_fail=parsed.get(
            "skills_critical_steps_advisory_fail", {}
        ) or {},
        bad_cases_regressing=parsed.get("bad_cases_regressing", {}) or {},
        anchor_outcome_closure_criterion_fails=parsed.get(
            "anchor_outcome_closure_criterion_fails", {}
        ) or {},
        summary=summary,
    )


def _empty_taxonomy(*, summary: str) -> FailureTaxonomy:
    return FailureTaxonomy(
        skills_critical_steps_advisory_fail={},
        bad_cases_regressing={},
        anchor_outcome_closure_criterion_fails={},
        summary=summary,
    )
