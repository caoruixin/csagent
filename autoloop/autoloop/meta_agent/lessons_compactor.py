"""Lessons compactor — every K iterations, produces one new lesson
section to append to `autoloop/results/lessons.md`.

The compactor reads the last K iteration records + the current
lessons file, asks the LLM to summarize patterns, and emits a single
`## Lesson L-<date>-<counter>` markdown section. The compactor never
edits prior sections — corrections are appended as new sections.

Discipline:

- No case_ids in the lesson body (enforced both in the prompt and
  in a defensive scrub here).
- No raw eval phrases (enforced in the prompt only — the scrub
  here is conservative since legitimate prose may share words with
  eval phrases).
"""

from __future__ import annotations

import json
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from .llm_client import LLMClient


_PROMPT_PATH = Path(__file__).resolve().parent / "prompts" / "compact.txt"

_CASE_ID_PATTERN = re.compile(
    r"\b(uc-[a-z]-\d+|bc-\d+|ao-\d+|shadow-\d+|case-\d+)\b",
    re.IGNORECASE,
)


def compact(
    recent_iterations: list[dict[str, Any]],
    current_lessons: str,
    *,
    client: LLMClient,
    next_counter: int | None = None,
) -> str:
    """Run the compactor LLM and return the new lesson section.

    `next_counter` overrides the counter portion of the lesson id;
    if None, derived from the count of existing `## Lesson` H2
    headers in `current_lessons` + 1.
    """
    if next_counter is None:
        next_counter = _count_lessons(current_lessons) + 1

    system = _PROMPT_PATH.read_text(encoding="utf-8")
    user = _build_user_input(recent_iterations, current_lessons)
    response = client.chat(system=system, user=user)
    section = _normalize_lesson_section(
        response.text, next_counter=next_counter
    )
    return section


def _build_user_input(
    recent_iterations: list[dict[str, Any]], current_lessons: str
) -> str:
    parts = [
        "WINDOW_ITERATIONS:",
        json.dumps(
            [
                {
                    "iteration_id": r.get("iteration_id"),
                    "target_skill": _extract_target_skill(r),
                    "target_field": _extract_target_field(r),
                    "decision": r.get("decision"),
                    "discard_reason": r.get("discard_reason"),
                    "fitness_delta": (
                        (r.get("verdict") or {}).get("tier_breakdown")
                    ),
                }
                for r in recent_iterations
            ],
            indent=2,
            default=str,
        ),
        "",
        "CURRENT_LESSONS_MD:",
        current_lessons or "<no lessons yet>",
    ]
    return "\n".join(parts)


def _normalize_lesson_section(text: str, *, next_counter: int) -> str:
    """Strip code fences and rewrite the L-id with the correct counter.

    The LLM may produce L-2026-05-28-001 even when the counter
    should be 047 — we rewrite to whichever value the caller passed.
    """
    stripped = text.strip()
    if stripped.startswith("```"):
        stripped = re.sub(r"^```(?:markdown)?\s*", "", stripped, flags=re.IGNORECASE)
        stripped = re.sub(r"\s*```\s*$", "", stripped)

    if not stripped.startswith("## Lesson"):
        # Defensive fallback: wrap whatever came back in a minimal
        # lesson section so the file remains parseable.
        stripped = (
            f"## Lesson L-{datetime.now(timezone.utc).strftime('%Y-%m-%d')}-{next_counter:03d}\n\n"
            f"**Window**: unknown (compactor returned non-section text)\n"
            f"**Target observation**: <unparseable compactor output>\n"
            f"**Pattern**: <unparseable compactor output>\n"
            f"**Heuristic for future propose**: <unparseable compactor output>\n"
            f"**Affected Skill × field**: unknown\n"
        )
        return stripped

    # Replace L-... line with the correct counter.
    stripped = re.sub(
        r"^## Lesson L-\d{4}-\d{2}-\d{2}-\d+",
        f"## Lesson L-{datetime.now(timezone.utc).strftime('%Y-%m-%d')}-{next_counter:03d}",
        stripped,
        count=1,
        flags=re.MULTILINE,
    )

    # Defensive case_id scrub on the body (separately from the prompt
    # discipline that asks the LLM to avoid case_ids).
    stripped = _CASE_ID_PATTERN.sub("<case>", stripped)
    return stripped


def _count_lessons(lessons_md: str) -> int:
    n = 0
    for line in lessons_md.splitlines():
        if line.startswith("## Lesson "):
            n += 1
    return n


def _extract_target_skill(record: dict[str, Any]) -> str | None:
    hyp = record.get("hypothesis") or {}
    return hyp.get("target_skill_file")


def _extract_target_field(record: dict[str, Any]) -> str | None:
    hyp = record.get("hypothesis") or {}
    return hyp.get("target_field_path")
