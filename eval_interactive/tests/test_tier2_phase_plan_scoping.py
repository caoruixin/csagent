"""Sprint 49 / S-Cleanup-3 (#9) — phase-plan-scoped Tier-2 evaluation.

Covers the misflip the Tier-2 design memo
(``docs/solutions/tier2_skill_traversal_design_memo.md``) reproduced
offline: a resolve-no-escalate session was flipped to ``case_passed=False``
because the escalate Skill's ``escalate-via-request-handover`` step is
``mandatory_for: [all 12 UCs]`` and the previous executor iterated all
loaded Skills regardless of which path the runtime took.

Scoping by ``per_turn_trace[].phase_plan.critical_steps[].id`` —
single-source-of-truth observable state the runtime emits — drops the
escalate step on resolve-path sessions while keeping it on sessions
that traverse ESCALATE. This is the Option (b) fix per the memo §7.

Coverage per ``docs/sprint_objective.md`` §3 stanza:

- target: resolve-no-escalate (escalate step NOT in presented_ids → not
  evaluated → no flip)
- neighbor: escalate session (escalate step IS in presented_ids →
  evaluated)
- negative / multi-phase: a session that legitimately enters BOTH
  resolve and escalate (the fix must not over-narrow)
- defensive: empty / absent phase_plan → inert PASS/advisory (NOT a
  fall-back to all-Skills, which would re-introduce the bug)
- presented_ids helper unit coverage
"""

from __future__ import annotations

from typing import Any

import pytest

from eval_interactive.batch.executor import (
    BatchExecutor,
    _collect_presented_step_ids,
)
from eval_interactive.config import Config
from eval_interactive.scoring.skill_procedure_check import (
    CriticalStep,
    Skill,
    SkillProcedureExtractor,
)


# ---------------------------------------------------------------------------
# Synthetic Skills — mirror the real escalate-vs-resolve mandatory pair that
# §3 of the memo identified as the smoking gun, without depending on the
# Java tree being checked out.
# ---------------------------------------------------------------------------


_ALL_UCS = (
    "UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP",
    "UC-G", "UC-H", "UC-I", "UC-J", "UC-K",
)

_ESCALATE_STEP = CriticalStep(
    id="escalate-via-request-handover",
    desc="Dispatch request_handover to escalate.",
    trace_check="accumulated_tool_results.request_handover",
    mandatory_for=_ALL_UCS,
    severity="mandatory",
)

_RESOLVE_STEP = CriticalStep(
    id="search-knowledge-before-faq-answer",
    desc="search_knowledge before producing an FAQ answer.",
    trace_check="accumulated_tool_results.search_knowledge",
    mandatory_for=("UC-A", "UC-B", "UC-C"),
    severity="mandatory",
)

_TERMINAL_STEP = CriticalStep(
    id="terminal-records-outcome",
    desc="record_outcome at terminal turn.",
    trace_check="accumulated_tool_results.record_outcome",
    mandatory_for=_ALL_UCS,
    severity="mandatory",
)


def _synthetic_skills() -> list[Skill]:
    return [
        Skill(
            name="escalate",
            applicable_use_cases=_ALL_UCS,
            critical_steps=(_ESCALATE_STEP,),
        ),
        Skill(
            name="resolve_faq",
            applicable_use_cases=("UC-A", "UC-B", "UC-C"),
            critical_steps=(_RESOLVE_STEP,),
        ),
        Skill(
            name="terminal",
            applicable_use_cases=_ALL_UCS,
            critical_steps=(_TERMINAL_STEP,),
        ),
    ]


def _executor_with_synthetic_skills() -> BatchExecutor:
    """A ``BatchExecutor`` whose Tier-2 extractor is pre-loaded with the
    synthetic Skills above — no Java tree required.
    """
    config = Config()
    ex = BatchExecutor(config)
    ex._skill_extractor = SkillProcedureExtractor.from_skills(_synthetic_skills())
    return ex


# ---------------------------------------------------------------------------
# Trace helpers
# ---------------------------------------------------------------------------


def _turn(
    *,
    phase_plan_step_ids: list[str] | None,
    accumulated_tool_results: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Build a per-turn trace dict matching the eval harness shape.

    ``phase_plan_step_ids = None`` means the turn does not carry a
    ``phase_plan`` at all (older / error turns).
    """
    turn: dict[str, Any] = {
        "tool_calls": [],
        "projection": {
            "accumulated_tool_results": accumulated_tool_results or {},
            "session": {},
        },
    }
    if phase_plan_step_ids is not None:
        turn["phase_plan"] = {
            "critical_steps": [
                {"id": sid, "desc": sid} for sid in phase_plan_step_ids
            ],
        }
    return turn


# ---------------------------------------------------------------------------
# 1. presented_ids helper
# ---------------------------------------------------------------------------


class TestCollectPresentedStepIds:
    def test_unions_step_ids_across_turns(self):
        trace = [
            _turn(phase_plan_step_ids=["a", "b"]),
            _turn(phase_plan_step_ids=["b", "c"]),
        ]
        assert _collect_presented_step_ids(trace) == {"a", "b", "c"}

    def test_empty_when_no_phase_plan(self):
        trace = [_turn(phase_plan_step_ids=None) for _ in range(3)]
        assert _collect_presented_step_ids(trace) == set()

    def test_empty_when_phase_plan_has_empty_critical_steps(self):
        trace = [_turn(phase_plan_step_ids=[])]
        assert _collect_presented_step_ids(trace) == set()

    def test_empty_trace(self):
        assert _collect_presented_step_ids([]) == set()

    def test_ignores_steps_without_id(self):
        trace = [
            {
                "tool_calls": [],
                "projection": {"accumulated_tool_results": {}, "session": {}},
                "phase_plan": {"critical_steps": [{"desc": "no id"}, {"id": "ok"}]},
            }
        ]
        assert _collect_presented_step_ids(trace) == {"ok"}


# ---------------------------------------------------------------------------
# 2. Phase-plan-scoped Tier-2 evaluation — target / neighbor / negative
# ---------------------------------------------------------------------------


class TestPhasePlanScopedTier2:
    def test_target_resolve_no_escalate_does_not_flip(self):
        """Target case (the memo §3 misflip):

        A UC-A FAQ-resolve session that called ``search_knowledge`` and
        ``record_outcome`` but NOT ``request_handover``. Pre-fix, the
        escalate Skill's mandatory step flipped Tier-2 to critical
        FAIL. Post-fix, that step's id is NOT in any turn's
        ``phase_plan.critical_steps``, so it is not evaluated.
        """
        ex = _executor_with_synthetic_skills()
        trace = [
            _turn(
                phase_plan_step_ids=[
                    "search-knowledge-before-faq-answer",
                    "terminal-records-outcome",
                ],
                accumulated_tool_results={
                    "search_knowledge": {"hits": [{"score": 5.0}]},
                    "record_outcome": {"outcome": "RESOLVED"},
                },
            )
        ]

        result = ex._compute_tier2_result(trace, active_use_case="UC-A")

        assert result.passed is True
        assert result.severity in ("critical", "advisory")
        assert "escalate-via-request-handover" not in result.failed_step_ids
        evaluated_ids = {r.step_id for r in result.per_step}
        assert evaluated_ids <= {
            "search-knowledge-before-faq-answer",
            "terminal-records-outcome",
        }

    def test_neighbor_escalate_session_evaluates_escalate_step(self):
        """Neighbor: an escalate session whose phase_plan presented the
        escalate step → the step IS evaluated. With request_handover
        dispatched, it PASSes. With it absent, it FAILs (and the gate
        flips). Both halves prove the step is in-scope, not over-
        narrowed away.
        """
        ex = _executor_with_synthetic_skills()

        passing_trace = [
            _turn(
                phase_plan_step_ids=["escalate-via-request-handover"],
                accumulated_tool_results={
                    "request_handover": {"escalation_reason": "user_requested"},
                },
            )
        ]
        passing = ex._compute_tier2_result(passing_trace, active_use_case="UC-A")
        assert passing.passed is True
        evaluated_ids = {r.step_id for r in passing.per_step}
        assert "escalate-via-request-handover" in evaluated_ids

        failing_trace = [
            _turn(
                phase_plan_step_ids=["escalate-via-request-handover"],
                accumulated_tool_results={},
            )
        ]
        failing = ex._compute_tier2_result(failing_trace, active_use_case="UC-A")
        assert failing.passed is False
        assert failing.severity == "critical"
        assert "escalate-via-request-handover" in failing.failed_step_ids

    def test_negative_multi_phase_session_evaluates_both(self):
        """Negative-control: a legitimately multi-phase session that
        traversed BOTH a resolve phase and an escalate phase must
        evaluate BOTH Skills' presented steps. The fix must not over-
        narrow to a single Skill.
        """
        ex = _executor_with_synthetic_skills()
        trace = [
            _turn(
                phase_plan_step_ids=["search-knowledge-before-faq-answer"],
                accumulated_tool_results={
                    "search_knowledge": {"hits": []},
                },
            ),
            _turn(
                phase_plan_step_ids=["escalate-via-request-handover"],
                accumulated_tool_results={
                    "search_knowledge": {"hits": []},
                    "request_handover": {
                        "escalation_reason": "faq_miss_threshold_exceeded"
                    },
                },
            ),
        ]

        result = ex._compute_tier2_result(trace, active_use_case="UC-A")

        evaluated_ids = {r.step_id for r in result.per_step}
        assert "search-knowledge-before-faq-answer" in evaluated_ids
        assert "escalate-via-request-handover" in evaluated_ids
        # Both PASS — search_knowledge present + request_handover present.
        assert result.passed is True

    def test_negative_multi_phase_one_failing_step_flips_gate(self):
        """Variant of negative-control: a multi-phase session where the
        resolve step was presented but its trace_check fails (no
        search_knowledge dispatch). The Tier-2 gate must flip.
        """
        ex = _executor_with_synthetic_skills()
        trace = [
            _turn(
                phase_plan_step_ids=["search-knowledge-before-faq-answer"],
                accumulated_tool_results={},
            ),
            _turn(
                phase_plan_step_ids=["escalate-via-request-handover"],
                accumulated_tool_results={
                    "request_handover": {"escalation_reason": "out_of_scope"},
                },
            ),
        ]

        result = ex._compute_tier2_result(trace, active_use_case="UC-A")

        assert result.passed is False
        assert result.severity == "critical"
        assert "search-knowledge-before-faq-answer" in result.failed_step_ids
        # The escalate step PASSed (request_handover was present) so it
        # must not appear in failed_step_ids.
        assert "escalate-via-request-handover" not in result.failed_step_ids


# ---------------------------------------------------------------------------
# 3. Defensive default — empty / absent phase_plan
# ---------------------------------------------------------------------------


class TestDefensiveDefault:
    def test_no_phase_plan_anywhere_returns_inert(self):
        """If no turn carries phase_plan.critical_steps (older traces,
        error turns), the gate must evaluate NOTHING → inert PASS /
        advisory. It MUST NOT fall back to all-Skills (that would
        re-introduce the bug).
        """
        ex = _executor_with_synthetic_skills()
        trace = [
            _turn(
                phase_plan_step_ids=None,
                accumulated_tool_results={},
            )
            for _ in range(3)
        ]

        result = ex._compute_tier2_result(trace, active_use_case="UC-A")

        # Inert: PASS, advisory severity, no per-step results, no failed ids.
        assert result.passed is True
        assert result.severity == "advisory"
        assert result.failed_step_ids == []
        assert result.per_step == []
        # Crucial: the escalate step (mandatory_for all 12 UCs) was NOT
        # evaluated despite UC-A matching its mandatory_for list. This
        # is the contract that prevents the misflip from coming back via
        # the empty-phase_plan path.
        evaluated_ids = {r.step_id for r in result.per_step}
        assert "escalate-via-request-handover" not in evaluated_ids

    def test_empty_trace_returns_inert(self):
        ex = _executor_with_synthetic_skills()
        result = ex._compute_tier2_result([], active_use_case="UC-A")
        assert result.passed is True
        assert result.severity == "advisory"
        assert result.per_step == []

    def test_phase_plan_with_empty_critical_steps_returns_inert(self):
        ex = _executor_with_synthetic_skills()
        trace = [_turn(phase_plan_step_ids=[], accumulated_tool_results={})]
        result = ex._compute_tier2_result(trace, active_use_case="UC-A")
        assert result.passed is True
        assert result.severity == "advisory"
        assert result.per_step == []

    def test_skill_extractor_missing_returns_inert(self):
        """If the Skill YAML dir is missing entirely (eval_interactive
        checkout without the Java tree), the executor returns the
        inert default regardless of phase_plan content. Preserves the
        S-Eval-2 backward-compat default.
        """
        config = Config()
        ex = BatchExecutor(config)
        ex._skill_extractor = None
        # Force the lazy loader to fail by pointing it at a missing dir.
        BatchExecutor._SKILLS_DIR = type(BatchExecutor._SKILLS_DIR)(
            "/nonexistent/skills/dir"
        )
        try:
            trace = [
                _turn(
                    phase_plan_step_ids=["escalate-via-request-handover"],
                    accumulated_tool_results={},
                )
            ]
            result = ex._compute_tier2_result(trace, active_use_case="UC-A")
            assert result.passed is True
            assert result.severity == "advisory"
        finally:
            # Restore for any other tests in the suite that read it.
            from pathlib import Path

            BatchExecutor._SKILLS_DIR = (
                Path(__file__).resolve().parents[2]
                / "server"
                / "src"
                / "main"
                / "resources"
                / "skills"
            )


# ---------------------------------------------------------------------------
# 4. UC mandatory_for interplay — the per-step N/A semantics in extract()
#    are unchanged; the executor only narrows WHICH steps are kept.
# ---------------------------------------------------------------------------


class TestUcMandatoryForInterplay:
    def test_uc_not_in_mandatory_for_yields_na_not_fail(self):
        """A presented step whose ``mandatory_for`` does not include
        ``active_use_case`` is N/A (extractor semantics) and does not
        flip the gate even when its trace_check would fail.
        """
        ex = _executor_with_synthetic_skills()
        # The resolve step is mandatory_for UC-A/B/C only. Run as UC-K
        # with the step presented → the extractor returns N/A for it,
        # so even though search_knowledge wasn't dispatched the gate
        # stays PASS.
        trace = [
            _turn(
                phase_plan_step_ids=["search-knowledge-before-faq-answer"],
                accumulated_tool_results={},
            )
        ]
        result = ex._compute_tier2_result(trace, active_use_case="UC-K")
        assert result.passed is True
