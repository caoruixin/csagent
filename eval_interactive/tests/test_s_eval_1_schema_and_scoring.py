"""Regression tests for Sprint 42 / S-Eval-1 (NEW M3-Eval sub-sprint 1).

Covers the four schema demotions (D-1.x) + three scoring demotions
(D-2.x) + the new ``anchor_outcome`` suite loading + backward-compat
preservation on the existing 14 smoke + 159 anchor + 12 case-family
+ 1 Alice bad case fixtures.

See ``docs/sprint_objective.md`` (Sprint 42) §2 / §5 for the contract
and ``docs/sprints/sprint-042-handoff.md`` §4 / §5 for the per-deliverable
walkthrough.
"""

from __future__ import annotations

import logging
from pathlib import Path

import pytest

from eval_interactive.case_spec.loader import load_case_specs
from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.composite import compute_composite
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.stall_detector import StallResult


# ---------------------------------------------------------------------------
# Fixture paths (anchored at repo root via this file's location)
# ---------------------------------------------------------------------------

_REPO_ROOT = Path(__file__).resolve().parents[2]
_CASE_SPECS_ROOT = _REPO_ROOT / "eval_interactive" / "case_specs"
_SMOKE = _CASE_SPECS_ROOT / "smoke"
_ANCHOR = _CASE_SPECS_ROOT / "anchor"
_FAMILIES_ROOT = _CASE_SPECS_ROOT / "case_families"
_BAD_CASES = _CASE_SPECS_ROOT / "bad_cases"
_ANCHOR_OUTCOME = _CASE_SPECS_ROOT / "anchor_outcome"


# ---------------------------------------------------------------------------
# D-1.x — Schema demotions
# ---------------------------------------------------------------------------


class TestSchemaDemotions:
    """D-1.1 / D-1.2 / D-1.3 acceptance."""

    def test_bot_handling_pattern_accepts_none(self):
        """D-1.1: ``bot_handling_pattern`` is now Optional[str] = None."""
        exp = Expected(
            outcome_class="resolve",
            primary_uc="UC-A",
            secondary_ucs=[],
            should_escalate=False,
            allow_bot_resolution="true",
            # bot_handling_pattern omitted — defaults to None
        )
        assert exp.bot_handling_pattern is None

    def test_bot_handling_pattern_still_rejects_blank_string(self):
        """D-1.1: when a string is supplied, the non-empty check still fires."""
        with pytest.raises(ValueError, match="non-empty string"):
            Expected(
                outcome_class="resolve",
                primary_uc="UC-A",
                secondary_ucs=[],
                should_escalate=False,
                allow_bot_resolution="true",
                bot_handling_pattern="   ",
            )

    def test_bot_handling_pattern_accepts_non_empty_string(self):
        """D-1.1: legacy specs that carry a string keep working."""
        exp = Expected(
            outcome_class="resolve",
            primary_uc="UC-A",
            secondary_ucs=[],
            should_escalate=False,
            allow_bot_resolution="true",
            bot_handling_pattern="Acknowledge and answer the FAQ question.",
        )
        assert exp.bot_handling_pattern == "Acknowledge and answer the FAQ question."

    def test_should_escalate_true_with_trigger_none_no_longer_raises(self, caplog):
        """D-1.2: ``should_escalate=true`` with ``escalation_trigger=None`` now
        logs a warning instead of raising ``ValueError``."""
        with caplog.at_level(logging.WARNING, logger="eval_interactive.case_spec.schema"):
            exp = Expected(
                outcome_class="escalate",
                primary_uc="UC-H",
                secondary_ucs=[],
                should_escalate=True,
                allow_bot_resolution="false",
                escalation_trigger=None,
            )
        assert exp.escalation_trigger is None
        assert any(
            "escalation_trigger is None with should_escalate=true" in rec.message
            for rec in caplog.records
        )

    def test_should_escalate_true_with_invalid_trigger_still_raises(self):
        """D-1.2: the enum guard on non-None triggers is preserved."""
        with pytest.raises(ValueError, match="escalation_trigger must be one of"):
            Expected(
                outcome_class="escalate",
                primary_uc="UC-H",
                secondary_ucs=[],
                should_escalate=True,
                allow_bot_resolution="false",
                escalation_trigger="this_is_not_a_canonical_value",  # type: ignore[arg-type]
            )

    def test_should_escalate_false_with_trigger_still_raises(self):
        """D-1.2: the false/non-None coupling still raises (true inconsistency)."""
        with pytest.raises(ValueError, match="escalation_trigger must be empty/null"):
            Expected(
                outcome_class="resolve",
                primary_uc="UC-A",
                secondary_ucs=[],
                should_escalate=False,
                allow_bot_resolution="true",
                escalation_trigger="user_requested",
            )

    def test_closure_criterion_default_none(self):
        """D-1.3: ``closure_criterion`` defaults to None at the CaseSpec level."""
        spec = CaseSpec(
            case_id="t-1",
            source_session_id="s-1",
            source_dataset="d-1",
            form_context=FormContext(
                first_name="x", email="x@x", topic_subject="t"
            ),
            persona=Persona(
                user_goal_summary="g",
                frustration_level="none",
                verbosity="terse",
                drift_behavior="none",
                seed_messages=[],
                hidden_facts=[],
            ),
            expected=Expected(
                outcome_class="resolve",
                primary_uc="UC-A",
                secondary_ucs=[],
                should_escalate=False,
                allow_bot_resolution="true",
            ),
            scoring=ScoringConfig(
                hard_checks=[], outcome_checks=[], llm_judge_dimensions=[]
            ),
        )
        assert spec.closure_criterion is None

    def test_closure_criterion_round_trips_through_loader(self):
        """D-1.3: top-level ``closure_criterion`` is parsed from YAML.

        Alice's bad case has had a top-level ``closure_criterion`` block
        since 2026-05-16; the loader now parses it into the dataclass.
        """
        bad = load_case_specs(_BAD_CASES)
        alice = next(c for c in bad if c.case_id == "alice_uc_a_uc_h_misclass")
        assert alice.closure_criterion is not None
        assert "multi-turn" in alice.closure_criterion


# ---------------------------------------------------------------------------
# D-2.x — Scoring demotions
# ---------------------------------------------------------------------------


class TestScoringDemotions:
    """D-2.1 / D-2.2 / D-2.3 / D-2.4 / D-2.5 acceptance."""

    def _make_spec(
        self,
        outcome_checks: list[str],
        outcome_class: str = "resolve",
        primary_uc: str = "UC-A",
        should_escalate: bool = False,
    ) -> CaseSpec:
        return CaseSpec(
            case_id="t",
            source_session_id="s",
            source_dataset="d",
            form_context=FormContext(first_name="x", email="x@x", topic_subject="t"),
            persona=Persona(
                user_goal_summary="g",
                frustration_level="none",
                verbosity="terse",
                drift_behavior="none",
                seed_messages=[],
                hidden_facts=[],
            ),
            expected=Expected(
                outcome_class=outcome_class,
                primary_uc=primary_uc,
                secondary_ucs=[],
                should_escalate=should_escalate,
                allow_bot_resolution="true" if not should_escalate else "false",
            ),
            scoring=ScoringConfig(
                hard_checks=[], outcome_checks=outcome_checks, llm_judge_dimensions=[]
            ),
        )

    def test_advisory_l1_failure_does_not_flip_gate(self):
        """D-2.1: advisory L1 result with passed=False does not gate."""
        spec = self._make_spec(outcome_checks=["correct_uc", "correct_outcome"])
        l1 = [
            HardCheckResult("no_pii_leakage", True),
            HardCheckResult(
                "no_forbidden_tools",
                False,
                "forbidden tools invoked",
                severity="advisory",
            ),
        ]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite("c", l1, l2, [], StallResult(detected=False), spec)
        assert result.case_passed is True

    def test_critical_l1_failure_still_flips_gate(self):
        """D-2.1 inverse: critical-severity L1 failure still flips the gate."""
        spec = self._make_spec(outcome_checks=["correct_uc", "correct_outcome"])
        l1 = [
            HardCheckResult("no_pii_leakage", False, "leaked"),
        ]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite("c", l1, l2, [], StallResult(detected=False), spec)
        assert result.case_passed is False

    def test_advisory_l2_excluded_from_outcome_score(self):
        """D-2.3 / D-2.5: advisory L2 results do not contribute to outcome_score."""
        spec = self._make_spec(outcome_checks=["correct_uc", "correct_outcome"])
        l1 = [HardCheckResult("no_pii_leakage", True)]
        l2 = [
            OutcomeCheckResult("correct_uc", 1.0),
            OutcomeCheckResult("correct_outcome", 1.0),
            OutcomeCheckResult(
                "tool_sequence_match", 0.0, "demoted", severity="advisory"
            ),
        ]
        result = compute_composite("c", l1, l2, [], StallResult(detected=False), spec)
        # Mean of the two critical L2 dims is 1.0; the advisory tool_sequence_match
        # is excluded so the score does not get dragged down by 0.0.
        assert result.outcome_score == pytest.approx(1.0)

    def test_empty_scoring_outcome_checks_opts_out_of_l2_gate(self):
        """D-2.4: empty ``scoring.outcome_checks`` skips the mandatory-L2 gate.

        OQ-S77 (S-Auto-22) update: the original form of this test asserted
        ``case_passed is True`` on a case with empty outcome_checks AND no
        L2/L3 evidence at all. That is exactly the zero-positive-evidence
        vacuous pass the OQ-S77 #3 gate now refuses, so the case is given a
        positive L3 judge signal here to isolate the D-2.4 contract (the
        mandatory-L2 gate does not fail-close on empty outcome_checks) from
        the new zero-evidence gate. The dedicated zero-evidence behaviour is
        covered by ``test_oq_s77_*`` in ``test_oq_s77_false_positive_gates``.
        """
        spec = self._make_spec(outcome_checks=[])
        l1 = [HardCheckResult("no_pii_leakage", True)]
        # No L2 results (anchor_outcome cases skip L2 entirely), but a passing
        # L3 judge dim gives composite > 0 so the verdict rests on real
        # evidence rather than a vacuous stamp.
        l3 = [JudgeResult("premature_finish", 5.0)]
        result = compute_composite("c", l1, [], l3, StallResult(detected=False), spec)
        assert result.case_passed is True
        assert result.mandatory_l2_passed is True
        assert result.mandatory_l2_failures == []

    def test_non_empty_scoring_outcome_checks_still_gates(self):
        """D-2.4: when the spec opts in, the gate still fires."""
        spec = self._make_spec(outcome_checks=["correct_uc", "correct_outcome"])
        l1 = [HardCheckResult("no_pii_leakage", True)]
        l2 = [
            OutcomeCheckResult("correct_uc", 0.5),  # below 1.0 threshold
            OutcomeCheckResult("correct_outcome", 1.0),
        ]
        result = compute_composite("c", l1, l2, [], StallResult(detected=False), spec)
        assert result.case_passed is False
        assert "correct_uc" in result.mandatory_l2_failures


# ---------------------------------------------------------------------------
# Outcome 3 — anchor_outcome suite loads through the existing loader
# ---------------------------------------------------------------------------


class TestAnchorOutcomeSuite:
    def test_anchor_outcome_loads(self):
        specs = load_case_specs(_ANCHOR_OUTCOME)
        assert 10 <= len(specs) <= 15, (
            f"anchor_outcome should carry 10-15 cases per S-Eval-1 §2 Outcome 3; "
            f"found {len(specs)}"
        )

    def test_anchor_outcome_each_case_is_outcome_only(self):
        specs = load_case_specs(_ANCHOR_OUTCOME)
        for spec in specs:
            assert spec.expected.bot_handling_pattern is None, (
                f"{spec.case_id} carries a bot_handling_pattern; anchor_outcome "
                f"must declare only outcome + persona + closure_criterion"
            )
            assert spec.expected.expected_tool_sequence == [], (
                f"{spec.case_id} declares an expected_tool_sequence; not allowed in anchor_outcome"
            )
            assert spec.expected.forbidden_tools == [], (
                f"{spec.case_id} declares forbidden_tools; not allowed in anchor_outcome"
            )
            assert spec.expected.escalation_trigger is None, (
                f"{spec.case_id} declares an escalation_trigger; not allowed in anchor_outcome"
            )
            assert spec.closure_criterion is not None and spec.closure_criterion.strip(), (
                f"{spec.case_id} missing closure_criterion (case-level)"
            )
            assert spec.scoring.outcome_checks == [], (
                f"{spec.case_id} should opt out of L2 gating via empty scoring.outcome_checks"
            )

    def test_anchor_outcome_per_uc_coverage(self):
        """Sanity: per-UC coverage spans the canonical 12 UCs."""
        specs = load_case_specs(_ANCHOR_OUTCOME)
        ucs = {s.expected.primary_uc for s in specs}
        canonical = {
            "UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F",
            "UC-FP", "UC-G", "UC-H", "UC-I", "UC-J", "UC-K",
        }
        missing = canonical - ucs
        assert not missing, f"anchor_outcome missing per-UC coverage for: {missing}"


# ---------------------------------------------------------------------------
# Backward compatibility — existing fixtures still load
# ---------------------------------------------------------------------------


class TestBackwardCompatLoad:
    def test_smoke_fixtures_load_unchanged(self):
        specs = load_case_specs(_SMOKE)
        assert len(specs) == 14, (
            f"smoke fixture count must be 14 (HEAD baseline); found {len(specs)}"
        )

    def test_anchor_fixtures_load_unchanged(self):
        specs = load_case_specs(_ANCHOR)
        assert len(specs) == 159, (
            f"anchor fixture count must be 159 (HEAD baseline); found {len(specs)}"
        )

    def test_alice_bad_case_loads_unchanged(self):
        specs = load_case_specs(_BAD_CASES)
        ids = {s.case_id for s in specs}
        # Alice remains the regression guard for the UC-A/UC-H mis-classification
        # shape across all M3-Eval sub-sprints; cascade fence prevents edits to
        # the Alice YAML (S-Eval-4 §6 #5 / milestone §6 #6).
        assert "alice_uc_a_uc_h_misclass" in ids
        # S-Eval-4 (Sprint 45) expansion: 11 new bad cases sourced from the 17
        # approved `case_spec_overrides.yaml` entries landed alongside Alice
        # (1 Alice + 11 = 12).
        # M-Auto-7 (Sprint 086b) entity-context expansion: +5 UC-A/UC-FP
        # entity-context cases (cs_uc_a_no_ad_id_ad_specific,
        # cs_uc_a_generic_policy_question, cs_uc_a_loaded_listing,
        # cs_uc_fp_loaded_moderation, cs_uc_a_lookup_failed) → 17.
        # Sprint 097 / S-Auto-45 (M-Auto-9 WP2): +1 satisfiable companion
        # `cs_uc_a_loaded_listing_resolvable` → 18. (This anchor was stale at
        # 12 from the M-Auto-7 close, which did not update it; corrected to the
        # current count alongside the WP2 add.)
        # Sprint 101 / S-Auto-49 (M-Auto-11 WP1): +1 characterization instrument
        # `cs_uc_a_viable_hit_loop_nonconvergence` (the viable-hit loop-
        # nonconvergence TARGET) → 19. Count-anchor maintenance only; this
        # characterization sub-sprint adds one bad case and changes no eval/
        # scoring/simulator logic.
        assert len(specs) == 19, (
            f"bad_cases should carry exactly 19 cases (12 S-Eval-4 + 5 M-Auto-7 "
            f"entity-context + 1 M-Auto-9 WP2 companion + 1 M-Auto-11 WP1 "
            f"characterization instrument); found {len(specs)}"
        )

    def test_case_family_fixtures_load_unchanged(self):
        # 12 family directories per HEAD inventory; each holds one or more YAML
        # files. Loading each directory through the standard loader is enough
        # to certify schema compatibility.
        family_dirs = [p for p in _FAMILIES_ROOT.iterdir() if p.is_dir()]
        assert len(family_dirs) >= 10, (
            f"expected >= 10 case-family directories; found {len(family_dirs)}"
        )
        total = 0
        for d in family_dirs:
            specs = load_case_specs(d)
            total += len(specs)
        assert total > 0
