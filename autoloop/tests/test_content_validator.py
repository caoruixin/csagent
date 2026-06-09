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
    # The 5x relative rule still fires when both bounds are crossed.
    # Default absolute_ceiling=1000; the short-`before` case below is
    # held to the ceiling, so we explicitly disable it via config to
    # exercise the 5x relative rule in isolation.
    before = "step one"
    after = "x" * (5 * len(before) + 5)
    cfg = {"content_validator": {"length_overflow_absolute_ceiling": 0}}
    res = validate_content(_hyp(after, before), config=cfg)
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
    cfg = {
        "content_validator": {
            "length_overflow_ratio": 2.0,
            # Disable absolute_ceiling so the tightened relative rule
            # is the dominant constraint for this small-field case.
            "length_overflow_absolute_ceiling": 0,
        }
    }
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
# length_overflow absolute_ceiling (S-Auto-7.2, OQ-S62.1)
# ---------------------------------------------------------------------


def test_length_overflow_absolute_ceiling_short_before_passes_under_ceiling():
    """A short `before_value` can grow up to `absolute_ceiling`
    (default 1000) chars without firing overflow, even if the ratio
    exceeds `overflow_ratio` (5.0). Models the exp-8 / exp-9
    pattern: short policy fields gaining coherent structural detail.
    """
    before = "x" * 168
    after = "x" * 974  # 5.80x, the exp-8 shape
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "PASS"


def test_length_overflow_exp9_shape_passes():
    """Concrete regression-pin: exp-9 168 → 852 chars (5.07x) PASSES
    under default absolute_ceiling=1000.
    """
    before = "x" * 168
    after = "x" * 852
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "PASS"


def test_length_overflow_short_before_above_ceiling_fails():
    """Short `before` + after above the absolute_ceiling still FAILS,
    preventing gross expansion of small fields into multi-thousand-
    char prose.
    """
    before = "x" * 168
    after = "x" * 1500  # over the 1000 ceiling
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.length_overflow"


def test_length_overflow_long_before_relative_rule_dominates():
    """When 5x `before_value` exceeds the absolute_ceiling, the
    relative rule dominates and an above-5x expansion still FAILS.
    """
    before = "x" * 500  # 5x = 2500 > 1000 ceiling, so 5x dominates
    after = "x" * 3000  # 6x, above the relative cap
    res = validate_content(_hyp(after, before), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.length_overflow"


def test_length_overflow_absolute_ceiling_tunable_via_config():
    """The `length_overflow_absolute_ceiling` knob is config-tunable;
    a lower ceiling enforces a stricter bound on short-field expansion.
    """
    before = "x" * 100
    after = "x" * 600
    cfg = {"content_validator": {"length_overflow_absolute_ceiling": 500}}
    res = validate_content(_hyp(after, before), config=cfg)
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.length_overflow"


def test_length_overflow_absolute_ceiling_zero_disables_floor():
    """Setting `length_overflow_absolute_ceiling: 0` restores
    pre-S-Auto-7.2 5x-only behavior (for tests that want to isolate
    the relative rule).
    """
    before = "x" * 100  # 5x = 500
    after = "x" * 600  # 6x, above relative cap
    cfg = {"content_validator": {"length_overflow_absolute_ceiling": 0}}
    res = validate_content(_hyp(after, before), config=cfg)
    assert res.verdict == "FAIL"
    assert res.rule_id == "content_validator.length_overflow"


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
