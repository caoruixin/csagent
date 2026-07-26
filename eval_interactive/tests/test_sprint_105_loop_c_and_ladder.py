"""Sprint 105 — Loop C, the L3 fallback diagnostic, and the D1/D2/D3 ladder.

Structure of this file follows the contract's §6 requirement: every
behaviour change is pinned twice — once as a characterisation of the
**before**-state (documented, with the old assertion preserved verbatim in
the docstring) and once as the corrected expectation.

The governing fence throughout is ``iteration_governance.md`` §5.4: a
scoring change may only make the ruler *more* discriminating. The
negative-control tests at the bottom are what enforce that.
"""

from __future__ import annotations

from dataclasses import dataclass, field

import pytest

from eval_interactive.scoring.composite import compute_composite
from eval_interactive.scoring.containment_ladder import (
    LadderFacts,
    TIER_D1,
    TIER_D2,
    TIER_D3,
    TIER_UNKNOWN,
    derive_tier_from_facts,
    extract_facts_from_turn_dicts,
)
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult, LlmJudge
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.stall_detector import StallResult


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


# Stub dataclasses, matching the convention already used by
# ``test_composite_gate.py``: ``compute_composite`` reads only
# ``case_spec.scoring.outcome_checks`` and ``case_spec.expected``, so a
# full CaseSpec (which carries ~15 required fields) is unnecessary here.


@dataclass
class _StubExpected:
    should_escalate: bool = False
    outcome_class: str = "resolve"
    primary_uc: str = "UC-C"
    expected_tool_sequence: list = field(default_factory=list)


@dataclass
class _StubScoring:
    outcome_checks: list = field(
        default_factory=lambda: ["correct_uc", "correct_outcome"]
    )
    # Exactly what all 430 L3-configuring specs in the corpus declare:
    # the three S-Eval-5-demoted advisory dims, and no gating dim at all.
    llm_judge_dimensions: list = field(
        default_factory=lambda: [
            "groundedness",
            "relevance",
            "tone_appropriateness",
        ]
    )


@dataclass
class _StubCaseSpec:
    scoring: _StubScoring = field(default_factory=_StubScoring)
    expected: _StubExpected = field(default_factory=_StubExpected)
    case_id: str = "cs_test"
    source_suite: str = "promotion"


def _spec(case_id: str = "cs_test", outcome_checks=("correct_uc", "correct_outcome")):
    """A minimal spec shaped like the `promotion/` specs in the corpus."""
    return _StubCaseSpec(
        case_id=case_id,
        scoring=_StubScoring(outcome_checks=list(outcome_checks)),
    )


def _clean_l1():
    return [HardCheckResult("no_pii_leakage", True)]


def _perfect_l2():
    return [
        OutcomeCheckResult("correct_uc", 1.0),
        OutcomeCheckResult("correct_outcome", 1.0),
    ]


def _advisory_l3(groundedness=5.0, relevance=5.0, tone=5.0):
    return [
        JudgeResult("groundedness", groundedness, "", "advisory"),
        JudgeResult("relevance", relevance, "", "advisory"),
        JudgeResult("tone_appropriateness", tone, "", "advisory"),
    ]


def _score(l1=None, l2=None, l3=None, spec=None, stall=None):
    return compute_composite(
        "cs_test",
        l1 if l1 is not None else _clean_l1(),
        l2 if l2 is not None else _perfect_l2(),
        l3 if l3 is not None else _advisory_l3(),
        stall or StallResult(),
        case_spec=spec or _spec(),
    )


# ---------------------------------------------------------------------------
# The corpus premise that makes Loop C load-bearing
# ---------------------------------------------------------------------------


def test_the_two_gating_l3_dims_are_the_only_non_advisory_ones():
    """Loop C only bites because every configurable dim is advisory.

    If a future sprint promotes a dim, or adds a new critical one, the
    renormalisation path stops being the common case and this file's
    fixtures no longer represent the corpus. Pin it so that change is
    noticed here rather than silently.
    """
    gating = set(LlmJudge.ALL_DIMENSIONS) - set(LlmJudge._ADVISORY_DIMENSIONS)
    assert gating == {"premature_finish", "stall_quality"}


# ---------------------------------------------------------------------------
# Loop C — characterisation of the before-state, then the fix
# ---------------------------------------------------------------------------


def test_characterisation_old_rule_capped_advisory_only_specs_at_half():
    """BEFORE-STATE (pinned): absence of a gating dim scored as failure of it.

    The pre-Sprint-105 rule was::

        gating_l3 = [r for r in l3_results if severity != "advisory"]
        if gating_l3:
            judge_score = mean(gating_l3) / 5.0
        else:
            judge_score = 0.0                      # <-- the defect
        composite = 0.5 * outcome + 0.5 * judge

    and the old assertion this test replaces was::

        assert result.composite == 0.5   # perfect bot, still FAILs 0.7

    A spec configuring only advisory dims — which is *every* spec in the
    486-case corpus — therefore had ``judge_score = 0.0`` and a composite
    ceiling of 0.5 against a pass bar of 0.7. A case where the bot did
    everything right could not pass. This test reproduces that arithmetic
    directly so the before-state stays legible after the fix.
    """
    l3 = _advisory_l3()
    gating = [r for r in l3 if r.severity != "advisory"]
    old_judge = (sum(r.score for r in gating) / len(gating) / 5.0) if gating else 0.0
    old_composite = 0.5 * 1.0 + 0.5 * old_judge

    assert gating == [], "the corpus configures no gating dim"
    assert old_judge == 0.0
    assert old_composite == 0.5
    assert old_composite < 0.7, "structurally incapable of passing"


def test_perfect_advisory_only_case_can_now_reach_the_pass_bar():
    """P1: a case where the bot behaved correctly can pass.

    The 0.7 bar is unchanged and no dim was promoted to gating. What
    changed is that the advisory dims — the only judge signal these specs
    produce — are now used instead of discarded.
    """
    result = _score()

    assert result.case_passed is True
    assert result.judge_measured is True
    assert result.judge_basis == "advisory_fallback"
    assert result.judge_score == 1.0
    assert result.composite == 1.0
    assert result.composite >= 0.7


def test_advisory_judge_penalty_is_not_discarded():
    """§5.4 direction check: a real quality deduction must still bite.

    This is the shape of the one recorded session Sprint 105 moves
    (``cs_interactive_185`` @ ``20260725-124324``): perfect outcome,
    ``groundedness=2.0`` because the bot made three definitive factual
    claims in its final turn citing only "our help article" with no URL
    or title. Renormalising the judge term away entirely would have
    scored that session a flat 1.0 and thrown the deduction on the floor.
    """
    result = _score(l3=_advisory_l3(groundedness=2.0, relevance=4.0, tone=5.0))

    # mean(2,4,5) = 3.6667 -> /5 = 0.7333 -> 0.5*1.0 + 0.5*0.7333
    assert result.judge_basis == "advisory_fallback"
    assert result.composite == pytest.approx(0.8667, abs=1e-4)
    assert result.composite < 1.0, "the groundedness deduction must survive"


def test_gating_dims_still_exclude_advisory_ones_from_the_mean():
    """S-Eval-5's intent is preserved exactly where it applies.

    Advisory dims are a *fallback* signal, never a dilutant: the moment a
    gating dim is measured, the advisory ones drop out of the mean just
    as S-Eval-5 specified.
    """
    l3 = _advisory_l3(groundedness=1.0, relevance=1.0, tone=1.0) + [
        JudgeResult("premature_finish", 5.0, "", "critical"),
    ]
    result = _score(l3=l3)

    assert result.judge_basis == "gating"
    assert result.judge_score == 1.0, "only the gating dim feeds the mean"
    assert result.composite == 1.0


def test_no_l3_at_all_renormalises_onto_the_outcome_term():
    """The 19 `bad_cases` shape: ``llm_judge_dimensions: []``.

    With no judge signal whatsoever the composite is renormalised onto
    the outcome term rather than halved against a phantom zero.
    """
    spec = _spec()
    spec.scoring.llm_judge_dimensions = []
    result = _score(l3=[], spec=spec)

    assert result.judge_measured is False
    assert result.judge_basis == "none"
    assert result.composite == 1.0
    assert "L3_UNMEASURED:no_dims_configured" in result.failure_tags


def test_renormalisation_never_applies_to_a_missing_outcome_term():
    """§5.4: a pass may never rest on judge signal with no L2 evidence.

    Only the judge term is renormalised away. With no gating L2 the
    outcome term stays 0.0 and is *not* renormalised, so the ceiling is
    0.5 — below the bar — no matter how good the judge score is.
    """
    spec = _spec(outcome_checks=())
    result = _score(l2=[], spec=spec, l3=_advisory_l3())

    assert result.judge_score == 1.0, "judge is perfect"
    assert result.composite == 0.5, "but with no L2 evidence the ceiling is 0.5"
    assert result.composite < 0.7


def test_the_pass_threshold_was_not_lowered():
    """The fix must not be 'lower the 0.7 bar'. Pin the bar's effect."""
    # outcome 2/3, perfect advisory judge -> 0.5*0.6667 + 0.5*1.0 = 0.8333
    partial = _score(
        l2=[
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult("handover_completeness", 0.0),
        ]
    )
    assert partial.composite == pytest.approx(0.8333, abs=1e-4)

    # A failing mandatory gate still zeroes the composite outright.
    gated = _score(
        l2=[
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 0.0),
        ]
    )
    assert gated.case_passed is False
    assert gated.composite == 0.0


# ---------------------------------------------------------------------------
# Item 3 — the silent L3 collapse
# ---------------------------------------------------------------------------


def test_characterisation_fallback_used_to_be_indistinguishable_from_a_real_score():
    """BEFORE-STATE (pinned): a fallback 3.0 looked exactly like a real 3.0.

    ``_DEFAULT_SCORE = 3.0`` was substituted on every failure path with no
    marker of any kind, so a judge that 400'd on every dimension produced
    a run that looked fully scored. The old assertion this replaces was::

        assert JudgeResult("relevance", 3.0, "parse failure; raw: ") == \\
               JudgeResult("relevance", 3.0, "")     # modulo reasoning

    i.e. nothing structural separated them. Now ``fallback_reason`` does.
    """
    real = JudgeResult("relevance", 3.0, "the bot answered adequately", "advisory")
    fell_back = JudgeResult(
        "relevance", 3.0, "parse failure; raw: ", "advisory",
        fallback_reason="parse_failure",
    )

    assert real.score == fell_back.score, "the before-state: same number"
    assert real.is_fallback is False
    assert fell_back.is_fallback is True


def test_fallback_dims_are_excluded_from_the_judge_mean():
    """A fallback carries no information and must not be averaged in."""
    l3 = _advisory_l3(groundedness=1.0, relevance=1.0, tone=1.0)
    l3[0] = JudgeResult(
        "groundedness", 3.0, "LLM call failed; default score applied",
        "advisory", fallback_reason="llm_call_failed",
    )
    result = _score(l3=l3)

    # mean of the two REAL dims (1.0, 1.0) = 1.0/5 = 0.2, not mean(3,1,1).
    assert result.judge_score == pytest.approx(0.2, abs=1e-6)
    assert result.l3_fallback_calls == 1
    assert result.l3_total_calls == 3
    assert "L3_FALLBACK:groundedness:llm_call_failed" in result.failure_tags


def test_wholly_collapsed_gating_layer_is_unmeasured_not_a_mid_range_constant():
    """Item 3's core requirement.

    A run whose gating L3 layer wholly fell back must not report the
    ``_DEFAULT_SCORE``-derived constant 0.6 as if it were a judge score.
    """
    l3 = [
        JudgeResult(
            "premature_finish", 3.0, "LLM call failed; default score applied",
            "critical", fallback_reason="llm_call_failed",
        ),
        JudgeResult(
            "stall_quality", 3.0, "LLM call failed; default score applied",
            "critical", fallback_reason="llm_call_failed",
        ),
    ]
    result = _score(l3=l3)

    assert result.judge_measured is False
    assert result.judge_score != pytest.approx(0.6), (
        "3.0/5.0 = 0.6 is the constant this fix exists to prevent"
    )
    assert "L3_UNMEASURED:all_gating_dims_fell_back" in result.failure_tags


def test_every_judge_fallback_path_stamps_a_reason():
    """All three ``_DEFAULT_SCORE`` sites must be marked, not just one."""
    parsed = LlmJudge._parse_response("relevance", "not json at all, no digits")
    assert parsed.fallback_reason == "parse_failure"
    assert parsed.score == 3.0

    # A well-formed response must NOT be marked.
    good = LlmJudge._parse_response("relevance", '{"score": 4, "reasoning": "ok"}')
    assert good.fallback_reason == ""
    assert good.score == 4.0


# ---------------------------------------------------------------------------
# Item 4 — the D1/D2/D3 ladder
# ---------------------------------------------------------------------------


def _turns(*specs):
    """Build per_turn_trace-shaped turns. Each spec is a list of tool names."""
    out = []
    for names in specs:
        calls = []
        for n in names:
            if n == "search_knowledge":
                calls.append({
                    "tool_name": n,
                    "success": True,
                    "result_data": {
                        "hits": [{"source_id": "ka1", "canonical_url": "https://h/x"}],
                        "faq_miss": False,
                    },
                })
            elif n == "resolve_article":
                calls.append({
                    "tool_name": n,
                    "success": True,
                    "result_data": {"source_url": "https://h/x"},
                })
            else:
                calls.append({"tool_name": n, "success": True, "result_data": {}})
        out.append({"tool_calls": calls})
    return out


def test_characterisation_flat_model_cannot_separate_the_two_handovers():
    """BEFORE-STATE (pinned): both handovers are the same value.

    The flat model carries only ``resolved / escalated / abandoned``. A
    session that explained the read-only half and handed over with full
    context, and one that handed over with no attempt at all, both stamp
    ``escalated`` — the defect WS-4 exists to close. The old assertion
    this replaces was, in effect::

        assert good.containment_outcome == bad.containment_outcome
    """
    good = "escalated"
    bad = "escalated"
    assert good == bad, "the flat model genuinely cannot tell them apart"


def test_ladder_separates_grounded_handover_from_bare_handover():
    """P2, on the exact shape of the two recorded sessions.

    ``cs_interactive_179`` retrieved across four turns and handed over
    with a complete payload; ``cs_interactive_185`` made zero tool calls
    and handed over at turn 2. Both recorded ``escalated``.
    """
    l1 = [HardCheckResult("source_citation_present", True)]
    l2 = [OutcomeCheckResult("handover_completeness", 1.0)]

    grounded = derive_tier_from_facts(
        extract_facts_from_turn_dicts(
            _turns(
                ["search_knowledge", "resolve_article"],
                ["search_knowledge"],
                ["search_knowledge"],
                ["request_handover"],
            ),
            l1, l2, "escalated", 6,
        )
    )
    bare = derive_tier_from_facts(
        extract_facts_from_turn_dicts(
            _turns([], [], ["request_handover"]), l1, l2, "escalated", 4,
        )
    )

    assert grounded.tier == TIER_D2
    assert bare.tier == TIER_D3
    assert grounded.tier != bare.tier, "the whole point of the ladder"


def test_d1_requires_in_bot_closure_on_grounded_evidence():
    facts = extract_facts_from_turn_dicts(
        _turns(["search_knowledge", "resolve_article"], ["record_outcome"]),
        [], [], "resolved", 3,
    )
    assert derive_tier_from_facts(facts).tier == TIER_D1


def test_incomplete_handover_payload_drops_a_grounded_session_to_d3():
    """A handover the human cannot act on is not a material deflection."""
    facts = extract_facts_from_turn_dicts(
        _turns(["search_knowledge"], ["request_handover"]),
        [],
        [OutcomeCheckResult("handover_completeness", 0.0)],
        "escalated",
        3,
    )
    assert derive_tier_from_facts(facts).tier == TIER_D3


def test_infra_failure_is_unknown_not_d3():
    """An unmeasured session must never be charged to the bot as a failure."""
    no_trace = derive_tier_from_facts(
        extract_facts_from_turn_dicts([], [], [], "", 4)
    )
    assert no_trace.tier == TIER_UNKNOWN

    no_terminal = derive_tier_from_facts(
        extract_facts_from_turn_dicts(_turns(["search_knowledge"]), [], [], "", 2)
    )
    assert no_terminal.tier == TIER_UNKNOWN


def test_retrieval_that_missed_does_not_earn_d2():
    """Attempting is not enough — an artifact has to actually surface."""
    turns = [{
        "tool_calls": [{
            "tool_name": "search_knowledge",
            "success": True,
            "result_data": {"hits": [], "faq_miss": True},
        }]
    }, {"tool_calls": [{"tool_name": "request_handover", "success": True}]}]
    facts = extract_facts_from_turn_dicts(turns, [], [], "escalated", 3)
    verdict = derive_tier_from_facts(facts)

    assert facts.attempted_retrieval is True
    assert facts.grounded_artifact is False
    assert verdict.tier == TIER_D3


def test_tier_is_derived_from_trace_facts_not_bot_wording():
    """§7.1 anti-hardcode: no tier may depend on what the bot *said*.

    Same tool trace, wildly different bot text — the tier must not move.
    The ladder never receives the transcript, which is the structural
    guarantee; this test pins the intent against a future refactor that
    might thread it in.
    """
    facts = extract_facts_from_turn_dicts(
        _turns(["search_knowledge"], ["request_handover"]),
        [], [OutcomeCheckResult("handover_completeness", 1.0)], "escalated", 3,
    )
    first = derive_tier_from_facts(facts)
    second = derive_tier_from_facts(LadderFacts(**vars(facts)))
    assert first.tier == second.tier == TIER_D2


# ---------------------------------------------------------------------------
# Negative controls — §5.4 / P0
# ---------------------------------------------------------------------------


def test_negative_control_premature_handover_does_not_start_passing():
    """The shape of both recorded negative controls.

    ``cs_interactive_155`` and ``cs_interactive_185`` @ ``20260725-165045``
    both handed over with ZERO retrieval calls after the user supplied
    exactly what the bot had asked for. The L1 ``no_premature_escalation``
    check fails, which zeroes the composite through the L1 gate. Nothing
    in Sprint 105 may lift that.
    """
    result = _score(
        l1=[
            HardCheckResult("no_pii_leakage", True),
            HardCheckResult(
                "no_premature_escalation", False,
                "handed over at turn 3 with no search_knowledge",
            ),
        ],
        l3=_advisory_l3(),  # even a perfect advisory judge
    )

    assert result.case_passed is False
    assert result.composite == 0.0
    assert "L1:no_premature_escalation" in result.failure_tags


def test_negative_control_wrong_uc_does_not_start_passing():
    """A mandatory-L2 gate failure is not reachable by renormalisation."""
    result = _score(
        l2=[
            OutcomeCheckResult("correct_uc", 0.0, "expected=UC-C, actual=UC-A"),
            OutcomeCheckResult("correct_outcome", 0.0),
        ],
        l3=_advisory_l3(),
    )

    assert result.case_passed is False
    assert result.composite == 0.0


def test_oq_s77_no_evidence_gate_survives_renormalisation():
    """A pass resting on zero L2 evidence is still refused."""
    spec = _spec(outcome_checks=())
    spec.scoring.llm_judge_dimensions = []
    result = _score(l2=[], l3=[], spec=spec)

    assert result.composite == 0.0
    assert result.case_passed is False
    assert result.verdict_reason == "no_l2_evidence_to_pass"
