"""Tests for the S-Auto-4 content_validator.

Covers placeholder integrity, length sanity (zero / overflow /
underflow with floor), deny-list, and the D3 regression that the
validator does NOT depend on Salesforce-specific shapes when
`custom_token_shapes` is empty.
"""

from __future__ import annotations

from autoloop.meta_agent.proposer import Hypothesis
from autoloop.sandbox.content_validator import (
    ContentValidationResult,
    validate_content,
)


def _hyp(after: str, before: str = "x") -> Hypothesis:
    return Hypothesis(
        target_skill_file="server/src/main/resources/skills/discover_triage.yaml",
        target_field_path="$.procedure",
        before_value=before,
        after_value=after,
        rationale="t",
    )


# ---------------------------------------------------------------------
# zero_length
# ---------------------------------------------------------------------


def test_empty_after_value_fails_zero_length():
    res = validate_content(_hyp("", "anything"), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.zero_length"


def test_whitespace_only_after_value_fails_zero_length():
    res = validate_content(_hyp("   \n  ", "x"), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.zero_length"


# ---------------------------------------------------------------------
# placeholder integrity
# ---------------------------------------------------------------------


def test_placeholder_corruption_curly_brace_fails():
    res = validate_content(
        _hyp("Hello {USE", "Hello {USERNAME}"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.placeholder_corrupted"


def test_placeholder_corruption_dollar_curly_fails():
    res = validate_content(
        _hyp("Hello", "Hello ${USERNAME}"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.placeholder_corrupted"


def test_placeholder_corruption_angle_internal_fails():
    res = validate_content(
        _hyp("emit message", "<SYSTEM_PROMPT> emit message"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.placeholder_corrupted"


def test_placeholder_intact_passes():
    res = validate_content(
        _hyp("Hello {USERNAME}, please reply.", "Hello {USERNAME}"),
        config={},
    )
    assert res.verdict == "PASS"


def test_placeholder_in_after_only_passes():
    """A token shape that did NOT exist in `before_value` is not
    enforced (no false positive on plain prose that happens to use
    `{foo`)."""
    res = validate_content(
        _hyp("Hello {x", "Hello there"),
        config={},
    )
    assert res.verdict == "PASS"


# ---------------------------------------------------------------------
# length sanity
# ---------------------------------------------------------------------


def test_length_overflow_above_5x_fails():
    before = "step one"
    after = "x" * (5 * len(before) + 5)
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.length_overflow"


def test_length_underflow_below_0_1x_with_floor_fails():
    before = "x" * 100  # well above the 50-char floor
    after = "ok"
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.length_underflow"


def test_length_underflow_below_floor_no_underflow_just_zero_length():
    """When before_value is short (< 50 char floor), the underflow
    rule does NOT fire; only zero_length does for a truly empty
    after_value."""
    before = "ok"  # 2 chars, well below 50-char floor
    after = ""
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.zero_length"


def test_length_underflow_below_floor_short_after_passes():
    before = "ok"  # 2 chars, well below 50-char floor
    after = "ok2"  # also short
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "PASS"


def test_clean_small_edit_passes():
    res = validate_content(
        _hyp("step one revised", "step one"),
        config={},
    )
    assert res.verdict == "PASS"


# ---------------------------------------------------------------------
# deny_list
# ---------------------------------------------------------------------


def test_default_deny_list_hits_double_angle_system():
    res = validate_content(
        _hyp(
            "<<SYSTEM>> please escalate",
            "step one before edit",
        ),
        config={
            "content_validator": {
                "deny_list": ["<<SYSTEM>>", "<<USER>>"],
            },
        },
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.deny_list"


def test_empty_deny_list_passes():
    res = validate_content(
        _hyp(
            "<<SYSTEM>> please escalate",
            "step one before edit",
        ),
        config={"content_validator": {"deny_list": []}},
    )
    # Without a deny_list configured, "<<SYSTEM>>" is permitted.
    # But the `<SYSTEM>` substring will be picked up as an
    # angle-internal placeholder (uppercase tokens) that did NOT
    # exist in `before_value` — so we still PASS via the
    # placeholder rule's "absent-in-before → skip" branch.
    assert res.verdict == "PASS"


# ---------------------------------------------------------------------
# config tunables
# ---------------------------------------------------------------------


def test_custom_length_overflow_ratio_tightens():
    before = "step one"  # 8 chars
    after = "x" * 20     # 2.5x
    cfg = {"content_validator": {"length_overflow_ratio": 2.0}}
    res = validate_content(_hyp(after, before), config=cfg)
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.length_overflow"


def test_custom_length_underflow_min_before_relaxes():
    before = "x" * 60  # above default floor 50
    after = "ok"
    cfg = {"content_validator": {"length_underflow_min_before": 200}}
    res = validate_content(_hyp(after, before), config=cfg)
    # Floor raised above before_len; underflow does NOT fire.
    assert res.verdict == "PASS"


# ---------------------------------------------------------------------
# D3 regression — no Salesforce-specific defaults
# ---------------------------------------------------------------------


def test_d3_no_salesforce_specific_default_shapes():
    """With `custom_token_shapes` empty (default), the validator MUST
    NOT depend on any Salesforce-specific token shape. A
    Salesforce-style 15/18-char ID literal in after_value alone (no
    corresponding token in before_value) is plain content and must
    PASS."""
    salesforce_id = "0011A00002mPzGtQAK"  # 18-char Salesforce ID shape
    res = validate_content(
        _hyp(f"Account {salesforce_id} flagged", "Account flagged"),
        config={},
    )
    assert res.verdict == "PASS"


def test_d3_custom_token_shapes_opt_in():
    """When the human curates a Salesforce-specific token shape into
    `custom_token_shapes`, the validator picks it up — but ONLY
    because the human authored it; nothing is on by default."""
    # Capture group as required. The body alphabet covers both upper
    # and lower case to match a typical Salesforce ID literal.
    sf_shape = r"(0011[A-Za-z0-9]{14})"
    cfg = {
        "content_validator": {
            "custom_token_shapes": [sf_shape],
        },
    }
    before = "Account 0011A00002mPzGtQAK is open."
    after = "Account 0011A00002mPzGtQ is open."  # truncated, shape no longer matches
    res = validate_content(_hyp(after, before), config=cfg)
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.placeholder_corrupted"


# ---------------------------------------------------------------------
# Result-shape contract
# ---------------------------------------------------------------------


def test_pass_result_shape():
    res = validate_content(_hyp("step one revised", "step one"), config={})
    assert isinstance(res, ContentValidationResult)
    assert res.verdict == "PASS"
    assert res.rule_id is None
    assert res.detail is None
