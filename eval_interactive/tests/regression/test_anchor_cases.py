"""Regression tests pinning the post-A3 field values for the anchor cases.

These tests guard against silent regressions in the regenerated corpus
(Wave A3) and the UC reclassification override map (Wave A2.1). They also
serve as living documentation of the two anchor cases that the
phase5 evaluation design explicitly calls out (cs_interactive_040 and
cs_interactive_015).
"""

from __future__ import annotations

from pathlib import Path

import pytest

from eval_interactive.case_spec.extractor import UC_B_RECLASSIFICATION_OVERRIDES
from eval_interactive.case_spec.loader import load_case_spec


# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------

# eval_interactive/tests/regression/test_anchor_cases.py -> repo's
# ``eval_interactive`` package root (the inner one, two levels up from this
# file). The case_specs corpus lives next to it as a sibling directory.
EVAL_INTERACTIVE_ROOT = Path(__file__).resolve().parents[2]
ANCHOR_DIR = EVAL_INTERACTIVE_ROOT / "case_specs" / "anchor"


# ---------------------------------------------------------------------------
# Anchor case: cs_interactive_040 (UC-K, escalate, intake_complete_for_uc_k)
# ---------------------------------------------------------------------------


def test_cs_interactive_040_field_values():
    """Pin the post-A3 expected fields for cs_interactive_040.

    cs_interactive_040 is the canonical UC-K intake-then-escalate anchor.
    The transcript-evidence resolver flips its legacy HR outcome_class from
    "resolve" to "escalate" because the bot cannot resolve a tooling defect
    on its own; only intake + handover is permitted.
    """
    spec = load_case_spec(ANCHOR_DIR / "cs_interactive_040.yaml")

    expected = spec.expected
    assert expected.primary_uc == "UC-K"
    assert expected.outcome_class == "escalate"
    assert expected.allow_bot_resolution == "partial"
    assert expected.should_escalate is True
    assert expected.escalation_trigger == "intake_complete_for_uc_k"
    assert expected.bot_handling_pattern  # non-empty
    assert isinstance(expected.bot_handling_pattern, str)
    assert expected.bot_handling_pattern.strip() != ""

    # Forbidden tools must NOT include the human-only async-update tool nor
    # moderation_enforcement_action -- those names should never appear in any
    # bot-facing list. (Wave A2 forbidden-tool curation removed them.)
    assert "send_followup_email_or_async_update" not in expected.forbidden_tools
    assert "moderation_enforcement_action" not in expected.forbidden_tools


# ---------------------------------------------------------------------------
# Anchor case: cs_interactive_015 (UC-K, secondaries [UC-FP, UC-B])
# ---------------------------------------------------------------------------


def test_cs_interactive_015_field_values():
    """Pin the post-A3 expected fields for cs_interactive_015.

    cs_interactive_015 is the multi-UC reclassification anchor: the source
    session looked like a UC-B (status query) but the override map promotes
    it to UC-K with secondaries UC-FP and UC-B (Wave A2.1).
    """
    spec = load_case_spec(ANCHOR_DIR / "cs_interactive_015.yaml")

    expected = spec.expected
    assert expected.primary_uc == "UC-K"
    assert expected.secondary_ucs == ["UC-FP", "UC-B"]

    # The bot SHOULD call get_customer_context as part of the safe-summary
    # intake step, so it must be in the expected sequence and never in the
    # forbidden list.
    assert "get_customer_context" in expected.expected_tool_sequence
    assert "get_customer_context" not in expected.forbidden_tools

    # An escalation trigger must be set since should_escalate=True.
    assert expected.escalation_trigger is not None


# ---------------------------------------------------------------------------
# UC-B reclassification override map sanity (Wave A2.1)
# ---------------------------------------------------------------------------


def test_uc_b_reclassification_override_map():
    """The override map should have exactly 11 entries; each value must be a
    ``(str, list[str])`` tuple where the primary UC is a recognised
    ``UC-<letter>``-style identifier.
    """
    valid_uc_prefixes = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP",
                        "UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}

    assert len(UC_B_RECLASSIFICATION_OVERRIDES) == 11

    for session_id, override in UC_B_RECLASSIFICATION_OVERRIDES.items():
        assert isinstance(session_id, str) and session_id, (
            f"override key must be non-empty str; got {session_id!r}"
        )
        assert isinstance(override, tuple) and len(override) == 2, (
            f"override value for {session_id!r} must be a 2-tuple; got {override!r}"
        )
        primary, secondary = override
        assert isinstance(primary, str) and primary in valid_uc_prefixes, (
            f"override primary for {session_id!r} must be a valid UC; got {primary!r}"
        )
        assert isinstance(secondary, list), (
            f"override secondary for {session_id!r} must be a list; got {type(secondary).__name__}"
        )
        for sec in secondary:
            assert isinstance(sec, str) and sec in valid_uc_prefixes, (
                f"override secondary entry for {session_id!r} invalid: {sec!r}"
            )

    # Spot-check the cs_interactive_015 source session id.
    assert UC_B_RECLASSIFICATION_OVERRIDES["570Q5000008WmXxIAK"] == (
        "UC-K",
        ["UC-FP", "UC-B"],
    )
