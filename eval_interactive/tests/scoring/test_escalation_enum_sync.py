"""Codex 2026-05-04 round 6 §R-9 — automated cross-source enum sync test.

Walks the three sources of truth for the canonical 23-value
``request_handover.escalation_reason`` enum and asserts they are
identical (order-independent):

1. ``docs/customer_service_tool_spec_v0_2.yaml`` — the YAML tool spec
   that production schema is derived from.
2. ``eval_interactive.case_spec.schema.ESCALATION_TRIGGER_VALUES`` —
   the eval-side typed enum used in CaseSpec validation.
3. ``server/src/main/java/.../PhaseEvaluator.java`` —
   ``CANONICAL_ESCALATION_REASONS`` constant used at runtime to
   coerce non-canonical values to ``service_degraded``.

Drift in any of these masks real failures: a CaseSpec can declare an
escalation_trigger that the YAML doesn't validate, the runtime can
emit a value the eval rejects, or the YAML can advertise a value that
the runtime never produces.
"""

from __future__ import annotations

import re
from pathlib import Path

import yaml

from eval_interactive.case_spec.schema import ESCALATION_TRIGGER_VALUES

REPO_ROOT = Path(__file__).resolve().parents[3]
TOOL_SPEC_YAML = REPO_ROOT / "docs" / "customer_service_tool_spec_v0_2.yaml"
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


def _load_yaml_enum() -> set[str]:
    with TOOL_SPEC_YAML.open("r", encoding="utf-8") as fh:
        spec = yaml.safe_load(fh)
    for tool in spec.get("tools", []):
        if tool.get("tool_name") != "request_handover":
            continue
        return set(
            tool["input_schema"]["properties"]["escalation_reason"]["enum"]
        )
    raise AssertionError("request_handover not found in tool spec YAML")


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


def test_yaml_and_eval_schema_enums_match() -> None:
    yaml_enum = _load_yaml_enum()
    schema_enum = set(ESCALATION_TRIGGER_VALUES)
    assert yaml_enum == schema_enum, (
        f"YAML/schema drift: only_yaml={sorted(yaml_enum - schema_enum)}, "
        f"only_schema={sorted(schema_enum - yaml_enum)}"
    )


def test_runtime_and_eval_schema_enums_match() -> None:
    runtime_enum = _load_runtime_enum()
    schema_enum = set(ESCALATION_TRIGGER_VALUES)
    assert runtime_enum == schema_enum, (
        f"runtime/schema drift: only_runtime={sorted(runtime_enum - schema_enum)}, "
        f"only_schema={sorted(schema_enum - runtime_enum)}"
    )


def test_yaml_and_runtime_enums_match() -> None:
    """Transitive — if both other tests pass this is redundant, but a
    direct assertion gives a cleaner failure message when only one of
    the two upstream sources has drifted."""
    yaml_enum = _load_yaml_enum()
    runtime_enum = _load_runtime_enum()
    assert yaml_enum == runtime_enum, (
        f"YAML/runtime drift: only_yaml={sorted(yaml_enum - runtime_enum)}, "
        f"only_runtime={sorted(runtime_enum - yaml_enum)}"
    )
