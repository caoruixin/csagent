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

from eval_interactive.case_spec.extractor import _load_case_spec_overrides
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
# Anchor case: cs_interactive_015 (UC-FP, secondaries [UC-B, UC-K])
# ---------------------------------------------------------------------------


def test_cs_interactive_015_field_values():
    """Pin the Wave A6 expected fields for cs_interactive_015.

    cs_interactive_015's Wave A2.1 UC-K legacy pin was superseded by a
    Wave A6 semantic re-review: the user asks "what happened to my ad?"
    and "how do I change it?", which Phase 2 §2.2:282 places under UC-FP
    (failed posting, edit-and-repost). The historical late escalation is
    failure-path evidence rather than the desired golden behavior.
    """
    spec = load_case_spec(ANCHOR_DIR / "cs_interactive_015.yaml")

    expected = spec.expected
    assert expected.primary_uc == "UC-FP"
    assert expected.secondary_ucs == ["UC-B", "UC-K"]
    assert expected.outcome_class == "resolve"
    assert expected.should_escalate is False

    # The bot SHOULD call get_customer_context as part of the safe-summary
    # intake step, so it must be in the expected sequence and never in the
    # forbidden list.
    assert "get_customer_context" in expected.expected_tool_sequence
    assert "get_customer_context" not in expected.forbidden_tools

    # No escalation trigger since should_escalate=False (resolve-first).
    assert expected.escalation_trigger is None


# ---------------------------------------------------------------------------
# Migrated UC-B reclassification entries in case_spec_overrides.yaml v2
# (Wave A6.6). Replaces the legacy in-code UC_B_RECLASSIFICATION_OVERRIDES
# constant; same eleven session-ids, plus the merged cs_interactive_012
# entry which carries both classification + expected blocks.
# ---------------------------------------------------------------------------


def test_uc_b_reclassification_override_map():
    """The legacy UC-B reclassification map must survive in
    ``case_spec_overrides.yaml`` v2: every original session id has a
    ``classification.primary_uc`` entry, and each UC value is recognised."""
    valid_uc_prefixes = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP",
                        "UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}

    legacy_session_ids = {
        "570Q5000008hx9tIAA",
        "570Q5000008WmXxIAK",
        "570Q5000008TMmvIAG",
        "570Q5000008U5C9IAK",
        "570Q5000008wmKbIAI",
        "570Q5000009060DIAQ",
        "570Q5000008w24rIAA",
        "570Q5000008fG5qIAE",
        "570Q5000008caqfIAA",
        "570Q5000008IwKHIA0",
        "570Q5000008iwZxIAI",
    }

    registry = _load_case_spec_overrides()

    for sid in legacy_session_ids:
        assert sid in registry.applied, (
            f"missing legacy UC-B reclassification entry for {sid!r}"
        )
        entry = registry.applied[sid]
        cls = entry.classification or {}
        assert "primary_uc" in cls and cls["primary_uc"] in valid_uc_prefixes, (
            f"override primary_uc for {sid!r} invalid: {cls!r}"
        )
        for sec in cls.get("secondary_ucs") or []:
            assert sec in valid_uc_prefixes, (
                f"override secondary entry for {sid!r} invalid: {sec!r}"
            )

    # Spot-check the cs_interactive_015 source session id (Wave A6
    # re-review: classification flipped from UC-K to UC-FP).
    cs15 = registry.applied["570Q5000008WmXxIAK"].classification
    assert cs15 == {"primary_uc": "UC-FP", "secondary_ucs": ["UC-B", "UC-K"]}
