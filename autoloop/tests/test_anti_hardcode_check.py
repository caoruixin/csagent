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
    # S-Y1.5c: Q6 — an if/then decision tree may be a principle-level
    # narrative the LLM still judges; demoted FAIL → FLAG so Codex adjudicates.
    res = anti_hardcode_check(
        _hyp("if user.message contains 'appeal' then route to UC-H"),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q1.if_then_decision_tree"


def test_q1_or_keyword_enumeration_rejected():
    # S-Y1.5c: Q3 — OR-keyword enumeration can be few-shot illustrative
    # teaching, not a routing rule dump; demoted FAIL → FLAG for Codex.
    res = anti_hardcode_check(
        _hyp("escalate if message contains 'appeal' or 'refund' or 'compensation'"),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q1.enumerated_or_keywords"


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
    # S-Y1.5c: Q6 — "must always" against a §1.3 soft dimension may be
    # narrative emphasis rather than a Tier-0 invention; Codex judges
    # whether it crosses the line. Demoted FAIL → FLAG. (Genuine Tier-0
    # invention is still caught by Q2.tier0_invariant_invention_attempt,
    # which stays FAIL — see test below.)
    res = anti_hardcode_check(
        _hyp("The runtime must always reject any user goal that mentions refunds."),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q2.must_always_against_soft_dimension"


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
    # Post-S-Y1.5c the if/then alone only FLAGs (demoted), but
    # Q4.id_assignment_literal (`case_id:`) stays FAIL and outranks the
    # pending FLAG, so the verdict is FAIL via Q4.
    assert res.verdict == "FAIL"
    assert res.rule_id == "Q4.id_assignment_literal"


def test_q4_clean_text_with_cs_in_word_passes():
    # "cs" appears INSIDE words (no word boundary at start) — must
    # not false-positive on "discuss", "customers", etc.
    res = anti_hardcode_check(
        _hyp("Discuss the customer's case carefully before escalating."),
        config={},
    )
    assert res.verdict == "PASS"


def test_q4_ordinary_cs_words_pass():
    # R-S90.5: a word starting `cs` followed by a LETTER (not digit /
    # underscore) is ordinary language, not a CaseSpec id. These are the
    # exact probe strings Codex flagged as false FAILs in S-Y1.5c.
    for prose in (
        "Use CSAT feedback as an aggregate quality signal.",
        "The csagent should provide grounded answers.",
        "Apply the css and cstring helpers when rendering the reply.",
    ):
        res = anti_hardcode_check(_hyp(prose), config={})
        assert res.verdict == "PASS", (
            f"ordinary cs-word prose must PASS; got {res.verdict} "
            f"({res.rule_id}) for {prose!r}"
        )


def test_q4_underscore_and_shadow_case_ids_still_fail():
    # The discriminator (digit / underscore right after `cs`) still catches
    # underscore-slug ids and `cs<n>s<n>` shadow ids — these MUST stay FAIL.
    for case_id in ("cs_uc_a_no_ad_id", "cs38s01", "cs59s"):
        res = anti_hardcode_check(
            _hyp(f"Apply the documented handling for {case_id} during intake."),
            config={},
        )
        assert res.verdict == "FAIL", (
            f"CaseSpec id {case_id!r} must FAIL; got {res.verdict}"
        )
        assert res.rule_id == "Q4.case_id_literal"


# ---------------------------------------------------------------------
# Q5 — LLM-ownership-shrinking language
# ---------------------------------------------------------------------


def test_q5_force_assistant_to_rejected():
    # S-Y1.5c: Q6 — "force the assistant to ..." is LLM-ownership-shrinking
    # language but a borderline semantic judgment; demoted FAIL → FLAG so
    # Codex decides whether it genuinely strips a §1.3-owned decision.
    res = anti_hardcode_check(
        _hyp("Force the assistant to always emit the canned reply."),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q5.force_assistant_to"


def test_q5_bot_must_always_rejected():
    # S-Y1.5c: Q6 — "the bot must always ..." may be narrative emphasis;
    # demoted FAIL → FLAG for Codex adjudication.
    res = anti_hardcode_check(
        _hyp("The bot must always escalate whenever the user mentions refunds."),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q5.bot_must_always"


def test_q5_do_not_consider_soft_dimension_rejected():
    # S-Y1.5c: Q3/Q6 — "do not consider <soft dimension>" is a semantic
    # judgment about what the LLM may reason over; demoted FAIL → FLAG.
    res = anti_hardcode_check(
        _hyp("Do not consider the user goal — proceed directly to escalation."),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q5.do_not_consider_soft_dimension"


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
    # Full-width "if" should still match after NFKC normalization. The
    # match is the proof of normalization; S-Y1.5c demotes the Q1.if_then
    # verdict FAIL → FLAG (Q6 principle-level narrative).
    res = anti_hardcode_check(
        _hyp("ｉｆ user.message contains 'appeal' then route to UC-H"),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q1.if_then_decision_tree"


def test_multi_line_decomposition_if_then_detected():
    # Multi-line if/then decomposition still detected; S-Y1.5c demotes the
    # Q1.if_then verdict FAIL → FLAG (Q6 — Codex judges the narrative).
    text = (
        "if the user mentions an appeal\n"
        "  - and the case is already open\n"
        "then route to escalation"
    )
    res = anti_hardcode_check(_hyp(text), config={})
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q1.if_then_decision_tree"


def test_synonym_map_arrow_when_enabled():
    # Arrow decision tree (=> normalized to →) detected via the shared
    # Q1.if_then_decision_tree id; S-Y1.5c demotes FAIL → FLAG (Q6).
    cfg = {"anti_hardcode": {"synonym_map_enabled": True}}
    res = anti_hardcode_check(
        _hyp("if user.message contains 'appeal' => route to UC-H"),
        config=cfg,
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q1.if_then_decision_tree"


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
    # Uses a kept-FAIL pattern (Q1.contains_or_matches_literal, surface ==
    # intent) so this remains a FAIL-path result-shape contract test after
    # the S-Y1.5c demote of the if/then family to FLAG.
    res = anti_hardcode_check(
        _hyp("when message.contains(\"appeal\") then escalate"),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "Q1.contains_or_matches_literal"
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


def test_fix_c_step1_codex_axis_b_bypass_now_caught():
    """R-S57 / Codex Axis B: `Whenever <X> => <Y>` previously bypassed
    the detector because " whenever " (space-bounded) did not fire at
    start-of-string. Fix-C step 1 word-boundary regex closes the bypass:
    the shape is still CAUGHT (not silently passed). S-Y1.5c demotes the
    Q1.if_then verdict FAIL → FLAG (Q6), so "caught" now means
    FLAG_FOR_CODEX → routed to Codex, not a propose-stage discard."""
    res = anti_hardcode_check(
        _hyp(
            "Whenever the customer describes an appeal => route to escalation "
            "and skip normal triage."
        ),
        config=_SYNONYM_ENABLED_CFG,
    )
    assert res.verdict == "FLAG_FOR_CODEX", (
        f"Codex Axis B bypass MUST be caught (FLAG post-S-Y1.5c); got "
        f"{res.verdict} ({res.rule_id})"
    )
    assert res.rule_id == "Q1.if_then_decision_tree", (
        f"Expected Q1.if_then_decision_tree (arrow form shares the id); "
        f"got {res.rule_id}"
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


def test_fix_c_step1_multi_line_when_arrow_decomposition_caught():
    """Multi-line `When ...\\n=> ...` shape is caught after Fix-C step 1:
    whitespace-collapse joins the lines, word-boundary regex maps
    'when' → 'if', synonym_map maps '=>' → '→', then Q1 arrow_tree
    matches. S-Y1.5c demotes the Q1.if_then verdict FAIL → FLAG (Q6)."""
    text = (
        "When the customer mentions a refund\n"
        "=> escalate to UC-H without further triage."
    )
    res = anti_hardcode_check(
        _hyp(text),
        config=_SYNONYM_ENABLED_CFG,
    )
    assert res.verdict == "FLAG_FOR_CODEX", (
        f"Multi-line 'When => ' MUST be caught (FLAG post-S-Y1.5c); got "
        f"{res.verdict}"
    )
    assert res.rule_id == "Q1.if_then_decision_tree", (
        f"Expected Q1.if_then_decision_tree (arrow form shares the id); "
        f"got {res.rule_id}"
    )


# ---------------------------------------------------------------------
# S-Auto-35 — baseline-whitelist suppression. A rule match is ignored
# when the substring it matched already exists in the candidate's
# untouched `before_value`. This unblocks pure-additive candidates whose
# baseline prose already trips a rule, without weakening the detector
# against patterns the candidate actually introduces.
# ---------------------------------------------------------------------


# Real exp-70 (before, after) pair captured from the Run-1 tranche
# (`autoloop/results/experiments.jsonl`). Embedded verbatim (JSON-safe
# escaping) so the suppression test exercises the exact false-positive
# that motivated the fix: the baseline `$.procedure` already contains the
# `when available, otherwise its article_id) -> record_outcome` arrow-tree
# that, post-`_normalize(synonym_map_enabled=True)`, matches `Q1` arrow
# tree. The 1478-char pure-additive append introduces no new Q1 match.
_EXP70_BEFORE = "You are a helpful Gumtree customer support agent. Resolve the user's issue using the provided tools. FAQ-path RESOLVE flow (S1): the intended terminal sequence is search_knowledge -> resolve_article -> grounded customer-facing answer (with a display_citation citation — the article's source_url when available, otherwise its article_id) -> record_outcome. Only escalate via request_handover after a valid resolve attempt cannot complete (no viable hit, or resolve_article could not produce a grounded answer)."

_EXP70_AFTER = "You are a helpful Gumtree customer support agent. Resolve the user's issue using the provided tools. FAQ-path RESOLVE flow (S1): the intended terminal sequence is search_knowledge -> resolve_article -> grounded customer-facing answer (with a display_citation citation — the article's source_url when available, otherwise its article_id) -> record_outcome. Only escalate via request_handover after a valid resolve attempt cannot complete (no viable hit, or resolve_article could not produce a grounded answer). Listing-context awareness: when the per-turn projection already carries a concrete listing reference (form_context or listing_context surfaces the customer's specific ad), treat the loaded listing as the case anchor and resolve from that listing's state alongside the grounded knowledge surface — answer the user's specific question against the listing data you already hold rather than asking the customer to restate an identifier the runtime has projected. Ad-specific questions without a listing reference: when the user's question concerns a specific ad's state but no listing reference is present in projected context and the user has not shared one, branch the resolve path on whether the question is answerable generically. Where the user's question has a generic policy or process answer that does not depend on the specific listing, run the normal search_knowledge -> resolve_article sequence and answer from the article surface. Where the answer genuinely requires the specific listing, ask one focused clarifying question for the listing reference before attempting to resolve, rather than guessing or escalating prematurely. Lookup-failure path: if a listing-lookup tool returns no record or surfaces an error, that is a legitimate resolve outcome — explain what the available context does and does not show, offer the generic-policy answer where it applies, and request_handover with an appropriate escalation_reason rather than fabricating listing-specific facts."

# A ≥120-char clean buffer with no if / then / when / arrow tokens. Sits
# between a baseline pattern and the appended clause so a rule's greedy
# trailing capture (`[\s\S]{0,80}` / `{1,120}`) stays inside baseline
# text and the matched span remains a substring of `before_norm`.
_CLEAN_BUFFER = (
    " Otherwise continue with the standard grounded resolution path and "
    "keep the customer informed about all the available options at every "
    "stage of the process here."
)


def test_q1_arrow_tree_baseline_match_suppressed():
    """REAL exp-70 pair: the matched arrow-tree span is 100% baseline
    text; the 1478-char additive clause adds no new Q1 match. With the
    baseline supplied, the false-positive is suppressed → PASS."""
    res = anti_hardcode_check(
        _hyp(_EXP70_AFTER, _EXP70_BEFORE),
        config=_SYNONYM_ENABLED_CFG,
    )
    assert res.verdict == "PASS", (
        f"exp-70 pure-additive candidate MUST PASS under baseline "
        f"suppression; got {res.verdict} ({res.rule_id} / "
        f"{res.matched_substring!r})"
    )


def test_q1_arrow_tree_new_match_still_caught():
    """No-op-when-novel: a candidate that ADDS a fresh `if X -> Y`
    arrow-tree clause on top of clean baseline prose is still caught — the
    new span is absent from `before_norm`, so baseline suppression does
    not fire. S-Y1.5c demotes the Q1.if_then verdict FAIL → FLAG (Q6);
    "caught" now means the rule still triggers (FLAG, not suppressed PASS),
    which is what this suppression test asserts."""
    before = (
        "You resolve the user issue using the available grounded tools "
        "and knowledge surfaces."
    )
    after = (
        before + " if the listing is flagged -> escalate immediately to "
        "a human agent for manual review."
    )
    res = anti_hardcode_check(_hyp(after, before), config=_SYNONYM_ENABLED_CFG)
    assert res.verdict == "FLAG_FOR_CODEX", (
        f"candidate-introduced arrow tree MUST still trigger (FLAG "
        f"post-S-Y1.5c, not suppressed PASS); got {res.verdict}"
    )
    assert res.rule_id == "Q1.if_then_decision_tree", (
        f"expected Q1.if_then_decision_tree; got {res.rule_id}"
    )


def test_q1_if_then_baseline_match_suppressed():
    """Synthetic IF/THEN decision tree present in the untouched baseline;
    the candidate only appends clean prose. The IF/THEN span is in
    `before_norm` → suppressed → PASS. (Same text with an empty baseline
    triggers Q1.if_then — FLAG_FOR_CODEX post-S-Y1.5c, see the existing
    Q1 decision-tree fixtures — so this is a genuine suppression to PASS,
    not a vacuous pass.)"""
    before = (
        "Triage rules: if the user reports a billing error then gather "
        "the order id before proposing next steps for them." + _CLEAN_BUFFER
    )
    after = (
        before + " Additionally, keep the tone warm and concise "
        "throughout the interaction with the customer."
    )
    res = anti_hardcode_check(_hyp(after, before), config=_SYNONYM_ENABLED_CFG)
    assert res.verdict == "PASS", (
        f"pure-additive over IF/THEN baseline MUST PASS; got {res.verdict} "
        f"({res.rule_id})"
    )


def test_q2_q4_q5_baseline_match_suppressed():
    """Suppression is uniform across the Q2 / Q4 / Q5 rule families, not
    just Q1. Each baseline already carries the offending span; the
    candidate only appends clean prose → PASS for every family. Each
    `after` triggers its rule with an empty baseline (verified in the
    matching positive fixtures above: Q4 → FAIL, Q2/Q5 → FLAG_FOR_CODEX
    post-S-Y1.5c), so none of these is a vacuous pass — suppression is
    what turns the trigger into PASS here."""
    q2_before = (
        "The runtime must always reject any user goal that mentions a "
        "competitor product line directly." + _CLEAN_BUFFER
    )
    q4_before = (
        "Apply the documented handling for cs042 and cs101 historical "
        "sessions during the migration window." + _CLEAN_BUFFER
    )
    q5_before = (
        "Force the assistant to always emit the canned closing reply "
        "verbatim at the end of the session." + _CLEAN_BUFFER
    )
    tail = " Keep the closing message brief and friendly for the customer."
    for label, before in (("Q2", q2_before), ("Q4", q4_before), ("Q5", q5_before)):
        res = anti_hardcode_check(
            _hyp(before + tail, before), config=_SYNONYM_ENABLED_CFG
        )
        assert res.verdict == "PASS", (
            f"{label} baseline-match MUST be suppressed; got {res.verdict} "
            f"({res.rule_id})"
        )


def test_double_encoding_bypass_documented():
    """ACCEPTED trade-off (documented): baseline-whitelisting suppresses a
    pattern that the candidate COPIES verbatim to a new position, because
    the copied span is still `in before_norm`. The detector therefore
    PASSes even though the field now carries the pattern twice. This is
    the deliberate limit of substring-based suppression: it cannot tell a
    pure-additive context-carry from a deliberate double-encoding. The
    field still shows the doubled pattern to a human / Codex auditor; the
    propose-stage detector simply does not block on it.

    A `.contains(` literal is used so the matched span is short and
    self-contained (no greedy trailing capture), making the
    span-equality with baseline unambiguous."""
    before = (
        "Use the legacy message.contains( probe in the deprecated triage "
        "branch for now during rollout."
    )
    after = (
        before + " Reminder for maintainers: the legacy "
        "message.contains( probe still appears here in the notes."
    )
    res = anti_hardcode_check(_hyp(after, before), config=_SYNONYM_ENABLED_CFG)
    assert res.verdict == "PASS", (
        f"double-encoded copy is suppressed by design; got {res.verdict} "
        f"({res.rule_id})"
    )


# ---------------------------------------------------------------------
# S-Y1.5c — severity calibration. The semantic-judgment rules are
# demoted FAIL → FLAG_FOR_CODEX (routed to per-sub-sprint Codex review);
# the surface==intent rules stay FAIL. `severity_overrides` is a
# reversibility hatch that re-promotes / demotes any rule per-experiment
# without a code change.
# ---------------------------------------------------------------------


def test_severity_overrides_promote_demoted_rule_back_to_FAIL():
    """Reversibility: an override re-promotes a demoted rule to FAIL on a
    NEW (non-baseline) match of that rule."""
    cfg = {"anti_hardcode": {"severity_overrides": {"Q1.if_then_decision_tree": "FAIL"}}}
    res = anti_hardcode_check(
        _hyp("if the listing is flagged then escalate to a human agent."),
        config=cfg,
    )
    assert res.verdict == "FAIL", f"override to FAIL must FAIL; got {res.verdict}"
    assert res.rule_id == "Q1.if_then_decision_tree"


def test_severity_overrides_demote_fail_to_flag():
    """Override MACHINERY works both directions: a kept-FAIL rule can be
    demoted to FLAG per-experiment (forensic only — the DEFAULT Q4 stays
    FAIL, asserted separately and hard-fenced)."""
    cfg = {"anti_hardcode": {"severity_overrides": {"Q4.case_id_literal": "FLAG_FOR_CODEX"}}}
    res = anti_hardcode_check(
        _hyp("Apply special handling for cs011 sessions."),
        config=cfg,
    )
    assert res.verdict == "FLAG_FOR_CODEX", (
        f"override to FLAG must FLAG; got {res.verdict}"
    )
    assert res.rule_id == "Q4.case_id_literal"


def test_default_severity_demotes_q1_if_then_to_flag():
    """Default config: a NEW if/then decision tree is FLAG, not FAIL."""
    res = anti_hardcode_check(
        _hyp("if the customer disputes a charge then open an appeal."),
        config={},
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q1.if_then_decision_tree"


def test_default_severity_demotes_q1_arrow_tree_to_flag():
    """Default config: a NEW arrow decision tree (if X → Y) demotes via the
    SHARED registry id Q1.if_then_decision_tree — there is no separate
    Q1.arrow_tree rule_id, so flipping the one entry covers both forms."""
    cfg = {"anti_hardcode": {"synonym_map_enabled": True}}
    res = anti_hardcode_check(
        _hyp("if the customer disputes a charge -> open an appeal."),
        config=cfg,
    )
    assert res.verdict == "FLAG_FOR_CODEX"
    assert res.rule_id == "Q1.if_then_decision_tree"


def test_default_severity_keeps_q4_case_id_as_fail():
    """Default config: a raw eval CaseSpec id stays FAIL (surface ==
    constitutional intent). Hard-fenced — Q4 default must not be demoted."""
    res = anti_hardcode_check(
        _hyp("Apply the documented handling for cs_uc_a_no_ad_id during intake."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "Q4.case_id_literal"


def test_default_severity_keeps_q1_contains_matches_as_fail():
    """Default config: a code-style `.contains(...)` literal stays FAIL."""
    res = anti_hardcode_check(
        _hyp('classify the case when message.contains("refund") is true.'),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "Q1.contains_or_matches_literal"


def test_default_severity_keeps_q2_tier0_invention_as_fail():
    """Default config: an attempt to mint a new Tier-0 invariant stays
    FAIL (Tier-0 minting authorization is not the proposer's to grant)."""
    res = anti_hardcode_check(
        _hyp("Add a new tier-0 invariant requiring escalation on every turn."),
        config={},
    )
    assert res.verdict == "FAIL"
    assert res.rule_id == "Q2.tier0_invariant_invention_attempt"


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
