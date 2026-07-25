"""Sprint 46 / S-Eval-5 (NEW Milestone M3-Eval sub-sprint 5; LAST M3-Eval
sub-sprint) regression suite — L3 judge repositioning + R-item closure
+ executor.py:252 Tier-2 wiring (Option A AUTHORIZED at planning round
2026-05-22 per ``docs/sprint_objective.md`` §2.6).

Covered surfaces
================

1. **L3 dim demotion** — the three legacy L3 dims (``groundedness``,
   ``relevance``, ``tone_appropriateness``) are demoted to Tier-3
   advisory: numeric scores still recorded in
   ``case_results[].l3_results[]`` but no longer factor into
   ``judge_score`` or ``composite_score`` (per
   ``docs/milestone_objective.md`` §2 four-tier pyramid and §3 S-Eval-5
   sentence 1).
2. **NEW ``user_goal_achievement`` dim** — Tier-1 *supplementary*
   advisory signal anchored on ``persona.user_goal_summary``; does NOT
   flip ``case_passed`` (bad-case + ``anchor_outcome`` manual review
   remain the primary Tier-1 gates per ``iteration_governance.md``
   §5.6 and milestone §5).
3. **Rubric prompt updates** —
   - ``R-l3-judge-form-context-trust-rubric`` closed: the
     ``_judge_tone_appropriateness`` prompt explicitly states
     ``form_context.first_name`` is a trusted signal (greet by first
     name without confirmation; do NOT penalise for "unauthorized
     familiarity").
   - ``R-l1-source-citation-quality-rubric`` closed: the
     ``_judge_groundedness`` prompt requires citations to surface a
     canonical URL OR article title; a bare Salesforce knowledge-
     article ID (e.g., ``ka44J000000gKxqQAE``) alone is NOT
     user-actionable and SHOULD be scored as a citation failure.
4. **Monotone-relaxing property (structural proof)** — under the
   S-Eval-5 demotion, no case whose pre-S-Eval-5 ``case_passed`` was
   True can flip to False as a result of the demotion alone (the
   demotion only changes ``judge_score`` mean composition, which does
   NOT participate in the ``case_passed`` gate; the gate depends on
   L1, mandatory L2, and Tier-2 only). Per
   ``docs/sprint_objective.md`` §2.5: the real-LLM rerun across smoke
   + anchor + anchor_outcome remains the operational verification
   surface, deferred to M3-Eval close per OQ-S45.5 / OQ-S45.6 carry-
   over (no API key available in the dev session); the structural
   proof here is the binding mathematical fact for the demotion
   axis (the Tier-2 wiring axis can intentionally flip true→false
   per §2.6 calibration follow-on — that is a separate signal, not
   a monotone-relaxing violation of the L3 demotion).
5. **Option A executor wiring** — ``executor.py`` builds the
   ``SkillProcedureExtractor`` lazily, iterates every loaded Skill,
   aggregates per-step ``critical_steps`` outcomes via
   ``tier2_results_to_gate``, and passes ``tier2_result=`` to
   ``compute_composite``. The wiring is the first production surface
   that makes the S-Eval-3 populated ``critical_steps`` content
   non-inert. Verified end-to-end: mandatory Tier-2 fail flips
   production ``case_passed``; advisory Tier-2 fail does NOT;
   empty-``critical_steps`` parity preserved.

These tests do NOT exercise the real LLM (the LlmJudge ``_call_llm``
network path is exercised in production runs at M3-Eval close).
They are pure-Python unit tests with synthetic JudgeResult / trace
fixtures; they pin the structural contract that the M3-Eval close
manual review and Codex milestone-shared review consume.
"""

from __future__ import annotations

import sys
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from eval_interactive.scoring.composite import compute_composite
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult, LlmJudge
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.skill_procedure_check import (
    CriticalStep,
    CriticalStepResult,
    Skill,
    SkillProcedureExtractor,
    Tier2Result,
    tier2_results_to_gate,
)
from eval_interactive.scoring.stall_detector import StallResult


# ---------------------------------------------------------------------------
# Test 1 — L3 dim demotion: advisory dims do not factor into judge_score
# ---------------------------------------------------------------------------


class TestL3DemotionAdvisoryDoesNotGateJudgeScore:
    """Advisory L3 dims must NOT contribute to ``judge_score`` mean.

    Pre-S-Eval-5: ``judge_score = mean(l3_scores) / 5.0`` across ALL
    L3 results.

    Post-S-Eval-5: advisory L3 results are filtered out of the mean
    (per S-Eval-1 D-2.5 severity convention applied to L3 in
    composite.py).
    """

    def test_advisory_groundedness_excluded_from_judge_score(self):
        """A 5.0 advisory groundedness + 1.0 critical premature_finish
        → judge_score = 1.0/5.0 = 0.2, NOT mean(5.0, 1.0)/5.0 = 0.6.
        """
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [
            JudgeResult("groundedness", 5.0, severity="advisory"),
            JudgeResult("premature_finish", 1.0, severity="critical"),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-s-eval-5-1a", l1, l2, l3, stall)
        # judge_score considers only the critical dim (premature_finish 1.0):
        # 1.0 / 5.0 = 0.2
        assert result.judge_score == pytest.approx(0.2)
        # All three demoted dims AND user_goal_achievement should be
        # filtered out of the gate mean; only critical dims contribute.
        # The full l3_results list still carries every result (advisory
        # dims are recorded for trend / report consumption).
        assert len(result.l3_results) == 2

    def test_all_advisory_l3_used_as_fallback_signal(self):
        """Sprint 105 (item 2): advisory dims are a fallback, not a dilutant.

        BEFORE-STATE, preserved verbatim as this test's prior name and
        expectation — ``test_all_advisory_l3_yields_zero_judge_score``::

            assert result.judge_score == 0.0
            # composite = 0.5 * outcome + 0.5 * judge = 0.5*1.0 + 0.5*0.0 = 0.5
            assert result.composite == pytest.approx(0.5)

        S-Eval-5 demoted these dims so they could not *skew* a judge mean
        that already contained gating dims. That intent is unchanged and
        is still pinned by
        ``test_critical_l3_only_contributes_to_judge_score`` above: the
        moment a gating dim is present, advisory dims drop out of the
        mean. What S-Eval-5 did not contemplate is the case where the
        advisory dims are the *only* judge signal — which is every one of
        the 486 specs in the corpus, none of which configures a gating
        dim. Discarding the only measurement available is strictly less
        discriminating than using it, so when no gating dim is measured
        the advisory dims now supply ``judge_score``.
        """
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [
            JudgeResult("groundedness", 5.0, severity="advisory"),
            JudgeResult("relevance", 4.0, severity="advisory"),
            JudgeResult("tone_appropriateness", 3.0, severity="advisory"),
            JudgeResult("user_goal_achievement", 5.0, severity="advisory"),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-s-eval-5-1b", l1, l2, l3, stall)
        assert result.judge_measured is True
        assert result.judge_basis == "advisory_fallback"
        # mean(5, 4, 3, 5) = 4.25 -> / 5.0 = 0.85
        assert result.judge_score == pytest.approx(0.85)
        # composite = 0.5 * 1.0 + 0.5 * 0.85 = 0.925 — and the imperfect
        # tone_appropriateness=3.0 still costs the case real score,
        # rather than being thrown away.
        assert result.composite == pytest.approx(0.925)
        assert result.composite < 1.0

    def test_advisory_l3_does_not_flip_case_passed(self):
        """A failing advisory L3 (e.g., groundedness=1.0 advisory) must
        NEVER flip ``case_passed``. The L3 gate is the
        ``judge_score`` mean; ``case_passed`` depends only on L1 +
        mandatory L2 + Tier-2.
        """
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [
            JudgeResult("groundedness", 1.0, severity="advisory"),  # bad
            JudgeResult("relevance", 1.0, severity="advisory"),  # bad
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-s-eval-5-1c", l1, l2, l3, stall)
        # Both L3 dims are advisory and bad — but case_passed remains
        # True (the gate is L1 + L2_mandatory + Tier-2; none of which
        # consult the L3 score).
        assert result.case_passed is True

    def test_advisory_l3_failure_tag_uses_distinct_prefix(self):
        """Sub-3.0 advisory L3 result → ``L3_ADVISORY:<dim>`` tag,
        NOT the legacy ``L3:<dim>`` (parity with the
        ``TIER2_ADVISORY:`` convention from Sprint 43 / S-Eval-2)."""
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [
            JudgeResult("groundedness", 2.0, severity="advisory"),
            JudgeResult("premature_finish", 1.0, severity="critical"),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-s-eval-5-1d", l1, l2, l3, stall)
        assert "L3_ADVISORY:groundedness" in result.failure_tags
        assert "L3:premature_finish" in result.failure_tags
        # Critical premature_finish keeps the legacy ``L3:`` prefix —
        # the split is severity-driven, not name-driven.
        assert "L3:groundedness" not in result.failure_tags

    def test_advisory_l3_scores_still_recorded(self):
        """Demoted dims still appear in ``l3_results`` for trend
        reporting; only their gate effect is removed."""
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [
            JudgeResult("groundedness", 4.2, "doc retrieval ok", severity="advisory"),
            JudgeResult("relevance", 3.8, "slight tangent", severity="advisory"),
            JudgeResult("tone_appropriateness", 4.5, "warm", severity="advisory"),
            JudgeResult("user_goal_achievement", 4.0, "goal met", severity="advisory"),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-s-eval-5-1e", l1, l2, l3, stall)
        # All four dims are present on the composite result; consumers
        # (reports / handoff / trend metrics) can read severity to
        # distinguish gate-contributing vs advisory.
        dim_names = {r.dimension for r in result.l3_results}
        assert dim_names == {
            "groundedness",
            "relevance",
            "tone_appropriateness",
            "user_goal_achievement",
        }
        # All four results carry advisory severity.
        assert all(r.severity == "advisory" for r in result.l3_results)


# ---------------------------------------------------------------------------
# Test 2 — LlmJudge severity assignment per dim
# ---------------------------------------------------------------------------


class TestLlmJudgeSeverityAssignment:
    """``LlmJudge._severity_for`` returns advisory iff the dim is in
    ``_ADVISORY_DIMENSIONS``."""

    def test_demoted_dims_are_advisory(self):
        for dim in ("groundedness", "relevance", "tone_appropriateness"):
            assert LlmJudge._severity_for(dim) == "advisory", (
                f"S-Eval-5 demoted dim {dim!r} must be advisory"
            )

    def test_new_user_goal_achievement_is_advisory(self):
        """NEW dim is Tier-1 supplementary advisory per
        milestone §3 S-Eval-5 sentence 2."""
        assert LlmJudge._severity_for("user_goal_achievement") == "advisory"

    def test_critical_dims_unchanged(self):
        """``premature_finish`` / ``stall_quality`` keep critical
        severity for backward compatibility with pre-S-Eval-5
        callers."""
        for dim in ("premature_finish", "stall_quality"):
            assert LlmJudge._severity_for(dim) == "critical", (
                f"S-Eval-5 does NOT demote {dim!r}; severity must stay critical"
            )

    def test_unknown_dim_defaults_to_critical(self):
        """A dim that is not in ``_ADVISORY_DIMENSIONS`` defaults to
        critical (fail-safe default — if a new dim is added without an
        advisory mapping, it gates by default)."""
        assert LlmJudge._severity_for("some_new_dim") == "critical"

    def test_parse_response_applies_severity(self):
        """``_parse_response`` stamps the returned JudgeResult with
        the dim's tier severity (so every JudgeResult coming out of
        the dispatch chain carries the right gate signal)."""
        # Advisory dim
        r = LlmJudge._parse_response(
            "groundedness", '{"score": 4, "reasoning": "ok"}', severity=None
        )
        assert r.severity == "advisory"
        # Critical dim
        r = LlmJudge._parse_response(
            "premature_finish", '{"score": 5, "reasoning": "done"}', severity=None
        )
        assert r.severity == "critical"


# ---------------------------------------------------------------------------
# Test 3 — user_goal_achievement dim wiring (NEW)
# ---------------------------------------------------------------------------


class TestUserGoalAchievementDim:
    """NEW Tier-1 supplementary advisory L3 dim
    ``user_goal_achievement`` is dispatched, anchors on
    ``persona.user_goal_summary``, and never gates ``case_passed``.
    """

    def test_user_goal_achievement_is_in_all_dimensions(self):
        """``LlmJudge.ALL_DIMENSIONS`` advertises the new dim so it
        is dispatched when a CaseSpec configures it via
        ``scoring.llm_judge_dimensions``."""
        assert "user_goal_achievement" in LlmJudge.ALL_DIMENSIONS

    def test_user_goal_achievement_does_not_gate_case_passed(self):
        """Advisory severity is enforced; even a score=1.0
        ``user_goal_achievement`` MUST NOT flip ``case_passed``."""
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        l3 = [
            JudgeResult("user_goal_achievement", 1.0, severity="advisory"),
            JudgeResult("premature_finish", 5.0, severity="critical"),
        ]
        stall = StallResult(detected=False)
        result = compute_composite("case-s-eval-5-3a", l1, l2, l3, stall)
        assert result.case_passed is True
        # Bad user_goal_achievement still surfaces in failure_tags via
        # the L3_ADVISORY prefix.
        assert "L3_ADVISORY:user_goal_achievement" in result.failure_tags

    def test_user_goal_achievement_records_score_via_dispatch(self):
        """Verify the dispatch path actually exercises
        ``_judge_user_goal_achievement`` and returns an advisory
        JudgeResult anchored on ``persona.user_goal_summary``."""
        # Build a minimal stub case spec + trace.
        case_spec = MagicMock()
        case_spec.scoring.llm_judge_dimensions = ["user_goal_achievement"]
        case_spec.persona.user_goal_summary = "Restore my advert listing"
        case_spec.persona.frustration_level = "medium"
        trace = MagicMock()
        trace.session_state.containment_outcome = "resolved"
        trace.turns = []

        config = MagicMock()
        config.llm.model = "test-model"
        config.llm.temperature = 0.0
        config.llm.base_url = "http://localhost:0"
        config.llm.api_key = "not-set"

        with patch("eval_interactive.scoring.llm_judge.OpenAI") as mock_openai:
            judge = LlmJudge(config)
            mock_completion = MagicMock()
            mock_completion.choices = [MagicMock()]
            mock_completion.choices[0].message.content = (
                '{"score": 4, "reasoning": "goal substantively addressed"}'
            )
            mock_openai.return_value.chat.completions.create.return_value = (
                mock_completion
            )

            # Need to re-bind the mock client since LlmJudge already
            # captured the patched OpenAI()-returned instance.
            judge._client = mock_openai.return_value

            results = judge.judge(case_spec, trace, transcript=[])

        assert len(results) == 1
        r = results[0]
        assert r.dimension == "user_goal_achievement"
        assert r.score == 4.0
        assert r.severity == "advisory"
        # Verify the prompt actually included the user_goal_summary
        # narrative anchor (the user-perspective string from persona).
        call_kwargs = mock_openai.return_value.chat.completions.create.call_args.kwargs
        prompt_text = call_kwargs["messages"][0]["content"]
        assert "Restore my advert listing" in prompt_text


# ---------------------------------------------------------------------------
# Test 4 — Rubric prompt updates (R-l3-form-context-trust + R-l1-source-citation)
# ---------------------------------------------------------------------------


def _stub_case_spec_with_first_name(first_name: str | None):
    case_spec = MagicMock()
    case_spec.scoring.llm_judge_dimensions = ["tone_appropriateness"]
    case_spec.form_context.first_name = first_name
    case_spec.persona.frustration_level = "calm"
    return case_spec


def _stub_trace_with_sources(sources_per_turn: list[list[str]]):
    trace = MagicMock()
    trace.turns = []
    for i, s in enumerate(sources_per_turn):
        turn = MagicMock()
        turn.turn_index = i
        turn.source_ids = list(s)
        trace.turns.append(turn)
    trace.session_state.containment_outcome = "resolved"
    return trace


class TestRubricUpdateFormContextTrust:
    """``R-l3-judge-form-context-trust-rubric`` close: the
    ``tone_appropriateness`` rubric explicitly trusts
    ``form_context.first_name`` and tells the judge NOT to penalise a
    first-name greeting for "unauthorized familiarity"."""

    def test_first_name_clause_renders_when_populated(self):
        config = MagicMock()
        config.llm.model = "m"
        config.llm.temperature = 0.0
        config.llm.base_url = "http://x"
        config.llm.api_key = "k"
        case_spec = _stub_case_spec_with_first_name("Alex")
        trace = _stub_trace_with_sources([[]])

        with patch("eval_interactive.scoring.llm_judge.OpenAI") as mock_openai:
            judge = LlmJudge(config)
            mock_completion = MagicMock()
            mock_completion.choices = [MagicMock()]
            mock_completion.choices[0].message.content = (
                '{"score": 5, "reasoning": "tone ok"}'
            )
            mock_openai.return_value.chat.completions.create.return_value = (
                mock_completion
            )
            judge._client = mock_openai.return_value
            judge.judge(case_spec, trace, transcript=[])

        prompt = mock_openai.return_value.chat.completions.create.call_args.kwargs[
            "messages"
        ][0]["content"]
        # Trust signal must be present, with the first name surfaced
        # so the judge can recognise the "Hi Alex" greeting as
        # appropriate.
        assert "form_context" in prompt
        assert "first_name" in prompt
        assert "Alex" in prompt
        assert "unauthorized familiarity" in prompt
        assert "R-l3-judge-form-context-trust-rubric" in prompt

    def test_first_name_clause_renders_when_absent(self):
        """When ``form_context.first_name`` is absent, the rubric
        still surfaces the trust statement (so the judge knows the
        trust signal exists in general) but notes none is
        pre-populated for this case."""
        config = MagicMock()
        config.llm.model = "m"
        config.llm.temperature = 0.0
        config.llm.base_url = "http://x"
        config.llm.api_key = "k"
        case_spec = _stub_case_spec_with_first_name(None)
        trace = _stub_trace_with_sources([[]])

        with patch("eval_interactive.scoring.llm_judge.OpenAI") as mock_openai:
            judge = LlmJudge(config)
            mock_completion = MagicMock()
            mock_completion.choices = [MagicMock()]
            mock_completion.choices[0].message.content = (
                '{"score": 5, "reasoning": "tone ok"}'
            )
            mock_openai.return_value.chat.completions.create.return_value = (
                mock_completion
            )
            judge._client = mock_openai.return_value
            judge.judge(case_spec, trace, transcript=[])

        prompt = mock_openai.return_value.chat.completions.create.call_args.kwargs[
            "messages"
        ][0]["content"]
        assert "No ``form_context.first_name`` is pre-populated" in prompt


class TestRubricUpdateSourceCitationQuality:
    """``R-l1-source-citation-quality-rubric`` close: the
    ``groundedness`` rubric requires citations to surface a canonical
    URL OR article title; a bare Salesforce ID alone is NOT
    user-actionable."""

    def test_citation_quality_clause_renders_in_groundedness_prompt(self):
        config = MagicMock()
        config.llm.model = "m"
        config.llm.temperature = 0.0
        config.llm.base_url = "http://x"
        config.llm.api_key = "k"
        case_spec = MagicMock()
        case_spec.scoring.llm_judge_dimensions = ["groundedness"]
        trace = _stub_trace_with_sources([["ka44J000000gKxqQAE"]])

        with patch("eval_interactive.scoring.llm_judge.OpenAI") as mock_openai:
            judge = LlmJudge(config)
            mock_completion = MagicMock()
            mock_completion.choices = [MagicMock()]
            mock_completion.choices[0].message.content = (
                '{"score": 2, "reasoning": "ID-only citation"}'
            )
            mock_openai.return_value.chat.completions.create.return_value = (
                mock_completion
            )
            judge._client = mock_openai.return_value
            judge.judge(case_spec, trace, transcript=[])

        prompt = mock_openai.return_value.chat.completions.create.call_args.kwargs[
            "messages"
        ][0]["content"]
        # Rubric must surface the new citation-quality clause + the
        # Salesforce-ID-alone failure pattern.
        assert "R-l1-source-citation-quality-rubric" in prompt
        assert "canonical URL" in prompt
        assert "article title" in prompt
        # Both example shapes (concrete ID + opaque alphanumeric) are
        # surfaced so the judge can recognise the failure pattern.
        assert "ka44J000000gKxqQAE" in prompt
        assert "opaque alphanumeric handle" in prompt
        # The CITATION QUALITY scoring band on the 1-5 anchors must
        # mention "bare Salesforce ID" so the judge can map the
        # observed pattern to a score band.
        assert "bare Salesforce ID" in prompt


# ---------------------------------------------------------------------------
# Test 5 — Monotone-relaxing property (structural proof for the L3 axis)
# ---------------------------------------------------------------------------


class TestMonotoneRelaxingL3Demotion:
    """The L3 demotion is monotone-relaxing on ``case_passed`` BY
    CONSTRUCTION: ``case_passed = L1 ∧ L2_mandatory ∧ ¬Tier2_critical``
    (per ``composite.py`` line ~217). None of these gates consult
    ``judge_score`` or any L3 dim, so changing the L3 mean
    composition cannot change ``case_passed``.

    The operational verification (real-LLM rerun across smoke + anchor
    + anchor_outcome) is deferred to M3-Eval close per
    ``docs/sprint_objective.md`` §2.5 + OQ-S45.5 / OQ-S45.6 carry-
    overs (no API key available in the dev session). This test pins
    the structural fact so the deferred rerun is verifying a property
    that is logically forced.

    NOTE: the Tier-2 wiring axis (Option A) can intentionally flip
    ``case_passed`` true→false when a mandatory critical_step on the
    active Skill fails (that is the whole point of the wiring); a
    Tier-2 flip is NOT a monotone-relaxing violation — it is the
    Tier-2 gate doing its job. See ``test_tier2_wiring_flips_case_passed``
    below for the intentional flip path.
    """

    @staticmethod
    def _build_composite(
        l3_severities: list[str],
        l3_scores: list[float],
        outcome_score: float = 1.0,
    ):
        """Construct a composite_score given a list of L3 severities
        + scores. L1 + mandatory L2 always pass; only the L3 dim
        composition varies. Returns ``(case_passed, judge_score)``.
        """
        l1 = [HardCheckResult("no_pii_leakage", True)]
        l2 = [OutcomeCheckResult("correct_uc", outcome_score)]
        l3 = [
            JudgeResult(f"dim_{i}", score, severity=sev)
            for i, (sev, score) in enumerate(zip(l3_severities, l3_scores))
        ]
        stall = StallResult(detected=False)
        c = compute_composite("synthetic", l1, l2, l3, stall)
        return c.case_passed, c.judge_score

    def test_all_critical_vs_all_advisory_same_case_passed(self):
        """A fixture where all L3 dims score = 5.0 critical (pre-
        S-Eval-5 mental model) yields the same ``case_passed`` as
        the same fixture where every L3 dim is demoted to advisory
        (post-S-Eval-5)."""
        pre_passed, _ = self._build_composite(
            ["critical", "critical", "critical"], [5.0, 5.0, 5.0]
        )
        post_passed, _ = self._build_composite(
            ["advisory", "advisory", "advisory"], [5.0, 5.0, 5.0]
        )
        assert pre_passed == post_passed == True  # noqa: E712

    def test_low_advisory_l3_does_not_flip_case_passed(self):
        """Same fixture; lower L3 scores post-demotion. Case must
        remain PASS (the demotion cannot flip true→false on the L3
        axis alone)."""
        pre_passed, pre_score = self._build_composite(
            ["critical", "critical", "critical"], [3.0, 3.0, 3.0]
        )
        post_passed, post_score = self._build_composite(
            ["advisory", "advisory", "advisory"], [1.5, 1.5, 1.5]
        )
        # Pre-passes, post-passes: the demotion is monotone-relaxing
        # on case_passed. The judge_score changes (it always does
        # when L3 composition changes), but case_passed does not.
        assert pre_passed is True
        assert post_passed is True

    @pytest.mark.parametrize(
        "scores", [[1.0, 1.0, 1.0], [2.5, 3.5, 4.5], [5.0, 5.0, 5.0]]
    )
    def test_no_post_demotion_l3_score_combination_flips_case_passed(self, scores):
        """For any L3 score combination on advisory severity,
        ``case_passed`` remains True so long as L1 + mandatory L2 +
        Tier-2 pass. This is the binding invariant."""
        post_passed, _ = self._build_composite(
            ["advisory", "advisory", "advisory"], scores
        )
        assert post_passed is True


# ---------------------------------------------------------------------------
# Test 6 — Tier-2 executor wiring (Option A AUTHORIZED)
# ---------------------------------------------------------------------------


class TestTier2ExecutorWiring:
    """Option A AUTHORIZED 2026-05-22 per
    ``docs/sprint_objective.md`` §2.6: ``executor.py`` builds a
    SkillProcedureExtractor lazily and passes ``tier2_result=`` to
    ``compute_composite``. With this wiring landed, the populated
    ``critical_steps`` content from S-Eval-3 is non-inert in the
    production eval-harness path.
    """

    @staticmethod
    def _trace_with_tool_results(tool_results: dict, intake_fields: list[str] | None = None):
        """Build a single-turn synthetic trace exposing the projection
        shape SkillProcedureExtractor expects."""
        session = {}
        if intake_fields is not None:
            session["intake_state"] = {"fields_collected": list(intake_fields)}
        return [
            {
                "tool_calls": [],
                "phase_plan": None,
                "projection": {
                    "accumulated_tool_results": dict(tool_results),
                    "session": session,
                },
            }
        ]

    def test_mandatory_tier2_failure_flips_case_passed(self):
        """A mandatory critical_step FAIL on the active UC flows
        through ``tier2_result`` into ``compute_composite`` and flips
        ``case_passed`` to False. This is the load-bearing assertion
        for the Option A wiring — without it the S-Eval-3 populated
        content stays inert in production."""
        skill = Skill(
            name="resolve_faq_grounded_answer",
            applicable_use_cases=("UC-A",),
            critical_steps=(
                CriticalStep(
                    id="retrieve-before-answer",
                    desc="Retrieve a knowledge article before answering UC-A.",
                    trace_check="accumulated_tool_results.search_knowledge",
                    mandatory_for=("UC-A",),
                    severity="mandatory",
                ),
            ),
        )
        ext = SkillProcedureExtractor.from_skills([skill])
        # Trace shows NO search_knowledge tool result — the mandatory
        # step FAILS.
        trace = self._trace_with_tool_results({})
        results = ext.extract(trace, active_skill=skill, active_use_case="UC-A")
        gate = tier2_results_to_gate(results)
        assert gate.passed is False
        assert gate.severity == "critical"
        # Now feed gate into compute_composite (the executor wiring
        # path). The case must flip to False.
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        stall = StallResult(detected=False)
        c = compute_composite(
            "case-t2-mandatory-fail",
            l1,
            l2,
            [],
            stall,
            tier2_result=gate,
        )
        assert c.case_passed is False
        assert any(t.startswith("TIER2:") for t in c.failure_tags)

    def test_advisory_tier2_failure_does_not_flip_case_passed(self):
        """An advisory-severity Tier-2 fail must NOT flip
        ``case_passed`` (parity with the L3 advisory pattern)."""
        skill = Skill(
            name="resolve_faq_grounded_answer",
            applicable_use_cases=("UC-A",),
            critical_steps=(
                CriticalStep(
                    id="cite-source-on-answer",
                    desc="Cite a source on factual answers.",
                    trace_check="accumulated_tool_results.resolve_article",
                    mandatory_for=("UC-A",),
                    severity="advisory",
                ),
            ),
        )
        ext = SkillProcedureExtractor.from_skills([skill])
        trace = self._trace_with_tool_results({})  # advisory step fails
        results = ext.extract(trace, active_skill=skill, active_use_case="UC-A")
        gate = tier2_results_to_gate(results)
        assert gate.passed is True  # advisory-only failure → gate stays PASS
        assert gate.severity == "advisory"
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        stall = StallResult(detected=False)
        c = compute_composite(
            "case-t2-advisory-fail",
            l1,
            l2,
            [],
            stall,
            tier2_result=gate,
        )
        assert c.case_passed is True
        # Advisory-only Tier-2 fail surfaces as a TIER2_ADVISORY tag.
        assert any(t.startswith("TIER2_ADVISORY:") for t in c.failure_tags)

    def test_empty_critical_steps_preserves_pre_s_eval_2_parity(self):
        """A Skill with empty ``critical_steps`` (the S-Eval-2
        default before S-Eval-3 populated content) yields no gate
        effect — ``case_passed`` is decided purely by L1 + L2."""
        skill = Skill(
            name="terminal", applicable_use_cases=("*",), critical_steps=()
        )
        ext = SkillProcedureExtractor.from_skills([skill])
        trace = self._trace_with_tool_results({})
        results = ext.extract(trace, active_skill=skill, active_use_case="UC-A")
        gate = tier2_results_to_gate(results)
        assert gate.passed is True
        assert gate.severity == "advisory"
        assert gate.failed_step_ids == []
        l1 = [HardCheckResult("no_stall", True)]
        l2 = [OutcomeCheckResult("correct_uc", 1.0)]
        stall = StallResult(detected=False)
        c = compute_composite(
            "case-t2-empty",
            l1,
            l2,
            [],
            stall,
            tier2_result=gate,
        )
        assert c.case_passed is True
        # No TIER2:* nor TIER2_ADVISORY:* tags should be present.
        assert not any(t.startswith("TIER2") for t in c.failure_tags)

    def test_executor_lazy_extractor_load_pattern(self):
        """``BatchExecutor`` lazy-loads the SkillProcedureExtractor
        from the canonical Skill YAML directory; absent directory is
        gracefully degraded to inert Tier-2 (PASS / advisory)."""
        # Avoid pulling in the full BatchExecutor.__init__ path (which
        # would resolve Config). Instead exercise the loader path
        # directly via the same calculation BatchExecutor uses.
        from eval_interactive.batch.executor import BatchExecutor

        skills_dir = BatchExecutor._SKILLS_DIR
        # Sanity: the path resolution lands on the real Skill YAML
        # directory in a normal checkout.
        assert skills_dir.is_dir(), (
            f"Expected canonical Skill YAML dir at {skills_dir} for "
            "S-Eval-5 Option A wiring; if the Java tree is intentionally "
            "absent in this checkout, the lazy loader returns None and "
            "Tier-2 stays inert — but the assertion here pins the "
            "happy-path resolution for the dev session."
        )

    def test_executor_compute_tier2_result_aggregates_presented_skills(self):
        """S-Cleanup-3 (#9): ``_compute_tier2_result`` aggregates
        per-step outcomes across every Skill whose presented step ids
        appear in ``per_turn_trace[].phase_plan.critical_steps``. A
        mandatory FAIL on any presented step flips the gate to critical.

        Pre-S-Cleanup-3 this test asserted "iterates every loaded
        Skill" regardless of phase_plan content; that semantic
        produced the memo §3 misflip and was replaced by phase-plan-
        scoped evaluation. The remaining contract (a mandatory FAIL
        on a *presented* step flips Tier-2) is unchanged.
        """
        skill_pass = Skill(
            name="confirm",
            applicable_use_cases=("*",),
            critical_steps=(
                CriticalStep(
                    id="confirm-step",
                    desc="Confirm step.",
                    trace_check="accumulated_tool_results.record_outcome",
                    mandatory_for=("UC-A",),
                    severity="mandatory",
                ),
            ),
        )
        skill_fail = Skill(
            name="resolve_faq_grounded_answer",
            applicable_use_cases=("UC-A",),
            critical_steps=(
                CriticalStep(
                    id="search-before-answer",
                    desc="Retrieve before answering.",
                    trace_check="accumulated_tool_results.search_knowledge",
                    mandatory_for=("UC-A",),
                    severity="mandatory",
                ),
            ),
        )

        ext = SkillProcedureExtractor.from_skills([skill_pass, skill_fail])

        from eval_interactive.batch.executor import BatchExecutor

        executor = BatchExecutor.__new__(BatchExecutor)
        executor._skill_extractor = ext

        # Both steps' ids must appear in phase_plan.critical_steps so
        # the post-S-Cleanup-3 scoping treats them as presented.
        trace = self._trace_with_tool_results({"record_outcome": {}})
        trace[0]["phase_plan"] = {
            "critical_steps": [
                {"id": "confirm-step", "desc": "Confirm step."},
                {"id": "search-before-answer", "desc": "Retrieve before answering."},
            ]
        }
        gate = executor._compute_tier2_result(trace, active_use_case="UC-A")
        assert gate.passed is False
        assert gate.severity == "critical"
        assert "search-before-answer" in gate.failed_step_ids


# ---------------------------------------------------------------------------
# Test 7 — R-item closure annotations recorded in action_bank
# ---------------------------------------------------------------------------


class TestRItemClosuresRecordedInActionBank:
    """The 4 M3-Eval R-items closed at S-Eval-5 carry ``succeeded-by``
    annotations in ``docs/action_bank.md`` §6 close-action index per
    ``docs/sprint_objective.md`` §2.4."""

    @pytest.fixture
    def action_bank_text(self) -> str:
        # Sprint 071 / S-Auto-15 (B2): the 4 M3-Eval R-item closure rows
        # were moved from ``docs/action_bank.md`` §6 into the new
        # ``docs/action_bank_archive.md`` by the 2026-06-01 ledger/archive
        # split (commit 0323457): the live ledger keeps only OPEN items,
        # closed items live in the archive. Read BOTH files so this
        # regression guard tracks the closure annotation wherever it
        # currently lives (live ledger or archive) without reverting the
        # split. Doc-governance/test-infra only; no agent behaviour masked.
        docs_dir = Path(__file__).resolve().parents[2] / "docs"
        parts: list[str] = []
        for name in ("action_bank.md", "action_bank_archive.md"):
            path = docs_dir / name
            if path.exists():
                parts.append(path.read_text(encoding="utf-8"))
        return "\n".join(parts)

    @pytest.mark.parametrize(
        "r_item_id",
        [
            "R-l3-judge-form-context-trust-rubric",
            "R-l1-source-citation-quality-rubric",
            "R-cs040-l3-review-intake-completion-semantics",
            "R-cs038-l3-review-intake-efficiency",
        ],
    )
    def test_r_item_closure_annotated(self, action_bank_text: str, r_item_id: str):
        """Each of the 4 M3-Eval R-items SHALL appear in the close-
        action index with a Sprint 46 / S-Eval-5 close annotation.
        The pattern matched is permissive (Sprint 46 OR S-Eval-5 OR
        "succeeded-by" near the R-item id) so deliver-agent close
        wording variants (per the convention established at S-Eval-1
        / S-Eval-3 close) do not break this regression guard.
        """
        # Locate the R-item near a closure annotation. The deliver-
        # agent close housekeeping appends the row in §6; this test
        # confirms the dev-side append (per dev contract §5 file in
        # scope: ``docs/action_bank.md``).
        assert r_item_id in action_bank_text, (
            f"R-item {r_item_id!r} must appear in docs/action_bank.md or "
            "docs/action_bank_archive.md for S-Eval-5 close per "
            "docs/sprint_objective.md §2.4 + §5"
        )
        # Sprint 071 / S-Auto-15 (B2): scan EVERY occurrence of the R-item
        # for a closure marker in the ±400-char window, not just the first.
        # An R-item id can also appear in a non-closure context (e.g. the
        # Sprint 20 "next sprint should pick the L3 review batch"
        # recommendation list in action_bank.md mentions
        # R-cs038-/R-cs040- as future work); the closure annotation itself
        # lives in the archive table row after the ledger/archive split.
        # Requiring the marker near the FIRST occurrence would spuriously
        # fail on that earlier mention.
        closure_markers = [
            "Sprint 46",
            "S-Eval-5",
            "succeeded-by",
            "M3-Eval close",
        ]
        annotated = False
        search_from = 0
        while True:
            idx = action_bank_text.find(r_item_id, search_from)
            if idx == -1:
                break
            window = action_bank_text[max(0, idx - 50) : idx + 400]
            if any(marker in window for marker in closure_markers):
                annotated = True
                break
            search_from = idx + len(r_item_id)
        assert annotated, (
            f"R-item {r_item_id!r} must carry a close annotation in "
            f"docs/action_bank.md §6 or docs/action_bank_archive.md "
            f"(one of {closure_markers!r} near the entry)"
        )
