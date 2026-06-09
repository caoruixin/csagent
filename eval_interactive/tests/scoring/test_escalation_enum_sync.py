"""Codex 2026-05-04 round 6 §R-9 — automated cross-source enum sync test.

Walks the three sources of truth for the canonical 23-value
``request_handover.escalation_reason`` enum and asserts they are
identical (order-independent):

1. ``docs/current/customer_service_tool_spec_v0_3.md`` — the markdown
   tool spec that production schema is derived from. The canonical
   enum is embedded as a backtick-quoted comma-separated list inside
   the ``**Canonical `escalation_reason` enum (23 values)**:`` bullet
   under ``### `request_handover```. (S-Cleanup-1 / Sprint 47
   migration from deleted ``customer_service_tool_spec_v0_2.yaml``.)
2. ``eval_interactive.case_spec.schema.ESCALATION_TRIGGER_VALUES`` —
   the eval-side typed enum used in CaseSpec validation.
3. ``server/src/main/java/.../PhaseEvaluator.java`` —
   ``CANONICAL_ESCALATION_REASONS`` constant used at runtime to
   coerce non-canonical values to ``service_degraded``.

Drift in any of these masks real failures: a CaseSpec can declare an
escalation_trigger that the spec doesn't validate, the runtime can
emit a value the eval rejects, or the spec can advertise a value that
the runtime never produces.
"""

from __future__ import annotations

import re
from pathlib import Path

from eval_interactive.case_spec.schema import ESCALATION_TRIGGER_VALUES

REPO_ROOT = Path(__file__).resolve().parents[3]
TOOL_SPEC_MD = (
    REPO_ROOT / "docs" / "current" / "customer_service_tool_spec_v0_3.md"
)
PHASE_EVALUATOR_JAVA = (
    REPO_ROOT
    / "server"
    / "src"
    / "main"
    / "java"
    / "com"
    / "gumtree"
    / "csagent"
    / "service"
    / "runtime"
    / "PhaseEvaluator.java"
)


def _load_spec_enum() -> set[str]:
    text = TOOL_SPEC_MD.read_text(encoding="utf-8")
    match = re.search(
        r"\*\*Canonical\s+`escalation_reason`\s+enum\s+\(23\s+values\)\*\*:\s*"
        r"(.*?)\.\s*The same set",
        text,
        re.DOTALL,
    )
    assert match, (
        "Canonical `escalation_reason` enum section not found in "
        f"{TOOL_SPEC_MD.name}"
    )
    body = match.group(1)
    return set(re.findall(r"`([a-z_]+)`", body))


def _load_runtime_enum() -> set[str]:
    text = PHASE_EVALUATOR_JAVA.read_text(encoding="utf-8")
    match = re.search(
        r"CANONICAL_ESCALATION_REASONS\s*=\s*Set\.of\((.*?)\);",
        text,
        re.DOTALL,
    )
    assert match, "CANONICAL_ESCALATION_REASONS literal not found"
    body = match.group(1)
    return set(re.findall(r'"([a-z_]+)"', body))


def test_spec_and_eval_schema_enums_match() -> None:
    spec_enum = _load_spec_enum()
    schema_enum = set(ESCALATION_TRIGGER_VALUES)
    assert spec_enum == schema_enum, (
        f"spec/schema drift: only_spec={sorted(spec_enum - schema_enum)}, "
        f"only_schema={sorted(schema_enum - spec_enum)}"
    )


def test_runtime_and_eval_schema_enums_match() -> None:
    runtime_enum = _load_runtime_enum()
    schema_enum = set(ESCALATION_TRIGGER_VALUES)
    assert runtime_enum == schema_enum, (
        f"runtime/schema drift: only_runtime={sorted(runtime_enum - schema_enum)}, "
        f"only_schema={sorted(schema_enum - runtime_enum)}"
    )


def test_spec_and_runtime_enums_match() -> None:
    """Transitive — if both other tests pass this is redundant, but a
    direct assertion gives a cleaner failure message when only one of
    the two upstream sources has drifted."""
    spec_enum = _load_spec_enum()
    runtime_enum = _load_runtime_enum()
    assert spec_enum == runtime_enum, (
        f"spec/runtime drift: only_spec={sorted(spec_enum - runtime_enum)}, "
        f"only_runtime={sorted(runtime_enum - spec_enum)}"
    )
