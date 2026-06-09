"""Tests for the S-Auto-4 anti_hardcode_check detector.

Covers:

- §4.1 kernel coverage: Q1, Q2, Q4, Q5 — ≥2 positive auto-rejected
  examples per category + clean-negative examples + borderline
  FLAG_FOR_CODEX cases.
- Unicode NFKC normalization regression.
- Multi-line decomposition (if-then split across newlines).
- Detector self-discipline (D2): the rule source MUST NOT contain
  any specific eval case_id / user utterance / expected assistant
  answer / case-status label literal.
"""

from __future__ import annotations

from pathlib import Path

from autoloop.meta_agent.proposer import Hypothesis
from autoloop.sandbox.anti_hardcode_check import (
    AntiHardcodeResult,
    anti_hardcode_check,
)


_ANTI_HARDCODE_SOURCE = (
    Path(__file__).resolve().parents[1]
    / "autoloop" / "sandbox" / "anti_hardcode_check.py"
).read_text(encoding="utf-8")


def _hyp(after: str, before: str = "x") -> Hypothesis:
    return Hypothesis(
        target_skill_file="server/src/main/resources/skills/discover_triage.yaml",
        target_field_path="$.procedure",
        before_value=before,
        after_value=after,
        rationale="t",
    )


# ---------------------------------------------------------------------
# Q1 — semantic hardcode patterns
# ---------------------------------------------------------------------


def test_q1_if_then_decision_tree_rejected():
    res = anti_hardcode_check(
        _hyp("if user.message contains 'appeal' then route to UC-H"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q1.")


def test_q1_or_keyword_enumeration_rejected():
    res = anti_hardcode_check(
        _hyp("escalate if message contains 'appeal' or 'refund' or 'compensation'"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q1.")


def test_q1_contains_literal_rejected():
    res = anti_hardcode_check(
        _hyp("when message.contains(\"refund\") classify as priority"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "Q1.contains_or_matches_literal"


def test_q1_matches_literal_rejected():
    res = anti_hardcode_check(
        _hyp("when message.matches(/refund/i) flag the case"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "Q1.contains_or_matches_literal"


def test_q1_clean_soft_narrative_passes():
    res = anti_hardcode_check(
        _hyp(
            "When the user describes a billing concern, gather the "
            "relevant intake fields before proposing next steps."
        ),
        config={},
    )
    assert res.verdict == "PASS"


# ---------------------------------------------------------------------
# Q2 — Tier-0 invariant invention attempts
# ---------------------------------------------------------------------


def test_q2_runtime_must_always_on_soft_dimension_rejected():
    res = anti_hardcode_check(
        _hyp("The runtime must always reject any user goal that mentions refunds."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q2.")


def test_q2_tier0_invention_attempt_rejected():
    res = anti_hardcode_check(
        _hyp("Add a new Tier-0 invariant that all messages require escalation."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q2.")


def test_q2_clean_clarifying_question_passes():
    res = anti_hardcode_check(
        _hyp(
            "If the user's intent is unclear, ask one focused clarifying "
            "question about their goal before continuing."
        ),
        config={},
    )
    # NOTE: this content uses "if" + "before" but no THEN/ELSE structural
    # decision tree. We expect PASS or FLAG_FOR_CODEX, not FAIL.
    assert res.verdict in ("PASS", "FLAG_FOR_CODEX")


# ---------------------------------------------------------------------
# Q4 — case_id / session_id / iteration_id literals
# ---------------------------------------------------------------------


def test_q4_case_id_token_rejected():
    res = anti_hardcode_check(
        _hyp("Apply special handling for cs042 and cs101 sessions."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q4.")


def test_q4_session_id_assignment_rejected():
    res = anti_hardcode_check(
        _hyp("Set session_id = abc-123 in the trace header."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q4.")


def test_q4_case_id_in_assignment_rejected():
    res = anti_hardcode_check(
        _hyp("If case_id: 9999 then continue intake."),
        config={},
    )
    # The if/then alone would already fire Q1; we accept either Q1
    # or Q4 here as long as the verdict is FAIL.
    assert res.verdict == "FAIL"


def test_q4_clean_text_with_cs_in_word_passes():
    # "cs" appears INSIDE words (no word boundary at start) — must
    # not false-positive on "discuss", "customers", etc.
    res = anti_hardcode_check(
        _hyp("Discuss the customer's case carefully before escalating."),
        config={},
    )
    assert res.verdict == "PASS"


# ---------------------------------------------------------------------
# Q5 — LLM-ownership-shrinking language
# ---------------------------------------------------------------------


def test_q5_force_assistant_to_rejected():
    res = anti_hardcode_check(
        _hyp("Force the assistant to always emit the canned reply."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q5.")


def test_q5_bot_must_always_rejected():
    res = anti_hardcode_check(
        _hyp("The bot must always escalate whenever the user mentions refunds."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q5.")


def test_q5_do_not_consider_soft_dimension_rejected():
    res = anti_hardcode_check(
        _hyp("Do not consider the user goal — proceed directly to escalation."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q5.")


def test_q5_standalone_must_borderline_flag_for_codex():
    # Standalone "MUST always escalate" without a Runtime-like subject:
    # not a high-confidence FAIL (no §1.3 dimension named) → FLAG.
    res = anti_hardcode_check(
        _hyp("Operators must always escalate suspicious payment activity."),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q5.standalone_must_borderline"


def test_q5_borderline_standalone_must_emit_flag():
    # Second FLAG_FOR_CODEX case for calibration.
    res = anti_hardcode_check(
        _hyp("Never reject a verified document without supervisor review."),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"


# ---------------------------------------------------------------------
# Normalization regressions
# ---------------------------------------------------------------------


def test_unicode_nfkc_full_width_if_detected():
    # Full-width "if" should still match after NFKC normalization.
    res = anti_hardcode_check(
        _hyp("ｉｆ user.message contains 'appeal' then route to UC-H"),
        config={},
    )
    assert res.verdict == "FAIL"


def test_multi_line_decomposition_if_then_detected():
    text = (
        "if the user mentions an appeal\n"
        "  - and the case is already open\n"
        "then route to escalation"
    )
    res = anti_hardcode_check(_hyp(text), config={})
    assert res.verdict == "FAIL"
    assert res.rule_id.startswith("Q1.")


def test_synonym_map_arrow_when_enabled():
    cfg = {"anti_hardcode": {"synonym_map_enabled": True}}
    res = anti_hardcode_check(
        _hyp("if user.message contains 'appeal' => route to UC-H"),
        config=cfg,
    )
    assert res.verdict == "FAIL"


# ---------------------------------------------------------------------
# Empty / trivial input
# ---------------------------------------------------------------------


def test_empty_after_value_returns_pass():
    res = anti_hardcode_check(_hyp(""), config={})
    assert res.verdict == "PASS"


def test_whitespace_after_value_returns_pass():
    res = anti_hardcode_check(_hyp("   \n  "), config={})
    assert res.verdict == "PASS"


# ---------------------------------------------------------------------
# Result-shape contract
# ---------------------------------------------------------------------


def test_result_placeholder_is_false():
    res = anti_hardcode_check(_hyp("clean soft-narrative content"), config={})
    assert isinstance(res, AntiHardcodeResult)
    assert res.placeholder is False


def test_fail_carries_matched_substring():
    res = anti_hardcode_check(
        _hyp("if user.message contains 'appeal' then escalate"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.matched_substring is not None
    assert len(res.matched_substring) > 0
    assert len(res.matched_substring) <= 200


# ---------------------------------------------------------------------
# Fix-C step 1 (S-Auto-5) — word-boundary "whenever"/"when" → "if".
# Closes the Codex Axis B bypass shape `Whenever ... =>`. Active only
# when `anti_hardcode.synonym_map_enabled: true` (the toggle final
# state is decided by §5 batch-calibration evidence; see handoff §5).
# ---------------------------------------------------------------------


_SYNONYM_ENABLED_CFG = {"anti_hardcode": {"synonym_map_enabled": True}}


def test_fix_c_step1_codex_axis_b_bypass_now_fails():
    """R-S57 / Codex Axis B: `Whenever <X> => <Y>` previously bypassed
    the detector because " whenever " (space-bounded) did not fire at
    start-of-string. Fix-C step 1 word-boundary regex closes the bypass."""
    res = anti_hardcode_check(
        _hyp(
            "Whenever the customer describes an appeal => route to escalation "
            "and skip normal triage."
        ),
        config=_SYNONYM_ENABLED_CFG,
    )
    assert res.verdict == "FAIL", (
        f"Codex Axis B bypass MUST FAIL after Fix-C step 1; got "
        f"{res.verdict} ({res.rule_id})"
    )
    assert res.rule_id.startswith("Q1."), (
        f"Expected Q1 family rule; got {res.rule_id}"
    )


def test_fix_c_step1_clean_prose_when_user_describes_still_passes():
    """Fix-C step 1 must not false-positive on common 'when the user
    describes ...' clean intake prose (no decision-tree arrow / then)."""
    res = anti_hardcode_check(
        _hyp(
            "When the user describes their issue, gather the relevant "
            "intake fields before proposing next steps."
        ),
        config=_SYNONYM_ENABLED_CFG,
    )
    assert res.verdict == "PASS", (
        f"Clean 'when' prose MUST PASS; got {res.verdict} ({res.rule_id})"
    )


def test_fix_c_step1_clean_prose_whenever_subordinate_clause_still_passes():
    """`Whenever possible, prefer concrete examples...` — `whenever` as a
    subordinate-clause adverb introducing context, NOT a decision tree.
    Must PASS (no arrow / no then token)."""
    res = anti_hardcode_check(
        _hyp(
            "Whenever possible, prefer concrete examples over abstract "
            "policy text in the response."
        ),
        config=_SYNONYM_ENABLED_CFG,
    )
    assert res.verdict == "PASS", (
        f"Subordinate-clause 'whenever' MUST PASS; got "
        f"{res.verdict} ({res.rule_id})"
    )


def test_fix_c_step1_multi_line_when_arrow_decomposition_fails():
    """Multi-line `When ...\\n=> ...` shape must FAIL after Fix-C step 1:
    whitespace-collapse joins the lines, word-boundary regex maps
    'when' → 'if', synonym_map maps '=>' → '→', then Q1 arrow_tree
    matches."""
    text = (
        "When the customer mentions a refund\n"
        "=> escalate to UC-H without further triage."
    )
    res = anti_hardcode_check(
        _hyp(text),
        config=_SYNONYM_ENABLED_CFG,
    )
    assert res.verdict == "FAIL", (
        f"Multi-line 'When => ' MUST FAIL; got {res.verdict}"
    )
    assert res.rule_id.startswith("Q1."), (
        f"Expected Q1 family rule; got {res.rule_id}"
    )


# ---------------------------------------------------------------------
# Detector self-discipline regression (D2)
# ---------------------------------------------------------------------


def test_detector_source_does_not_hardcode_eval_case_ids():
    """The rule source MUST encode generic structural patterns only —
    no specific eval CaseSpec literal. Codex re-checks this at Sprint
    close; the test exists so a future PR cannot quietly slip in a
    case-id-literal rule."""
    forbidden_literals = (
        "cs011", "cs015", "cs042", "cs101", "cs59s",
    )
    for lit in forbidden_literals:
        # The literal `cs59s` may appear in the gaming.py shadow-leak
        # signatures (NOT this file). Here we are inspecting
        # anti_hardcode_check.py only.
        assert lit not in _ANTI_HARDCODE_SOURCE, (
            f"forbidden literal {lit!r} found in anti_hardcode_check.py"
        )


def test_detector_source_does_not_hardcode_user_utterance_literals():
    """Rule source MUST NOT contain literal user-message phrases or
    expected-answer wording from the eval suite. Sample list of
    things that would be high-signal violations if present."""
    forbidden_phrases = (
        "my account is locked",
        "closure_criterion",
        "expected_behavior",
        "source_session_id",
        # Common eval-CaseSpec phrasing fragments — none of these
        # should appear as a rule literal.
        "primary_uc",
        "failure_tags",
    )
    for phrase in forbidden_phrases:
        assert phrase not in _ANTI_HARDCODE_SOURCE, (
            f"forbidden phrase {phrase!r} found in anti_hardcode_check.py"
        )


def test_detector_source_rule_count_bounded():
    """The contract caps the rule set at ~30 rules. If we grow past
    that, STOP-and-surface for a re-design pass."""
    # `from .anti_hardcode_check import anti_hardcode_check` in
    # sandbox/__init__.py rebinds the package attribute to the
    # function. Use importlib to be sure we reach the module.
    import importlib
    mod = importlib.import_module("autoloop.sandbox.anti_hardcode_check")
    assert len(mod._RULES) <= 30, (
        f"rule count {len(mod._RULES)} exceeds 30; redesign required"
    )
