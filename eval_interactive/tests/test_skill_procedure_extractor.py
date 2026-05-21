"""Sprint 43 (S-Eval-2) — happy / miss / N/A tests for the Tier-2
``skill_procedure_followship`` extractor + DSL evaluator.

Covers all six frozen primitives of the ``trace_check`` DSL plus the
two combinators, plus the N/A path that mirrors the contract §5 §
"a case whose `active_skill` does NOT match the step's `mandatory_for`
UC set → step outcome N/A".
"""

from __future__ import annotations

from typing import Any

import pytest

from eval_interactive.scoring.skill_procedure_check import (
    CriticalStep,
    Skill,
    SkillProcedureExtractor,
    Tier2Result,
    TraceView,
    evaluate_trace_check,
    parse_trace_check,
    tier2_results_to_gate,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _turn(
    tool_calls: list[dict[str, Any]] | None = None,
    projection: dict[str, Any] | None = None,
) -> dict[str, Any]:
    return {
        "tool_calls": tool_calls or [],
        "phase_plan": {},
        "projection": projection or {},
    }


def _tool(name: str, seq: int, *, success: bool = True) -> dict[str, Any]:
    return {
        "tool_name": name,
        "sequence_index": seq,
        "success": success,
        "arguments": {},
        "result_data": {},
    }


def _skill_with(
    *steps: CriticalStep,
    name: str = "synthetic",
    ucs: tuple[str, ...] = ("UC-A",),
) -> Skill:
    return Skill(name=name, applicable_use_cases=ucs, critical_steps=tuple(steps))


# ---------------------------------------------------------------------------
# Primitive 1 — accumulated_tool_results.<tool>
# ---------------------------------------------------------------------------


class TestAccumulatedToolResultsPresence:
    def test_present_returns_pass(self):
        trace = [
            _turn(
                projection={
                    "accumulated_tool_results": {
                        "search_knowledge": {"hits": [{"score": 5.0}]}
                    },
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("accumulated_tool_results.search_knowledge")
        assert evaluate_trace_check(ast, view) is True

    def test_absent_returns_fail(self):
        trace = [_turn(projection={"accumulated_tool_results": {}, "session": {}})]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("accumulated_tool_results.search_knowledge")
        assert evaluate_trace_check(ast, view) is False


# ---------------------------------------------------------------------------
# Primitive 2 — tool_event_seq(<a>) < tool_event_seq(<b>)
# ---------------------------------------------------------------------------


class TestToolEventSeqOrder:
    def test_correct_order_returns_pass(self):
        trace = [
            _turn(
                tool_calls=[
                    _tool("search_knowledge", seq=1),
                    _tool("resolve_article", seq=2),
                ]
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)"
        )
        assert evaluate_trace_check(ast, view) is True

    def test_reversed_order_returns_fail(self):
        trace = [
            _turn(
                tool_calls=[
                    _tool("resolve_article", seq=1),
                    _tool("search_knowledge", seq=2),
                ]
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)"
        )
        assert evaluate_trace_check(ast, view) is False

    def test_missing_either_tool_returns_fail(self):
        # Only one of the two tools was dispatched — the order check is
        # undefined and yields FAIL (conservative).
        trace = [_turn(tool_calls=[_tool("search_knowledge", seq=1)])]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)"
        )
        assert evaluate_trace_check(ast, view) is False

    def test_unsuccessful_dispatch_is_ignored(self):
        # A failed dispatch does NOT count toward the order check — only
        # successful events do.
        trace = [
            _turn(
                tool_calls=[
                    _tool("search_knowledge", seq=1, success=False),
                    _tool("resolve_article", seq=2),
                ]
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)"
        )
        # search_knowledge never successfully dispatched → first_seq is None
        # → check returns False.
        assert evaluate_trace_check(ast, view) is False


# ---------------------------------------------------------------------------
# Primitive 3 — intake_state.fields_collected.contains(<field>)
# ---------------------------------------------------------------------------


class TestIntakeFieldCollected:
    def test_field_present_returns_pass(self):
        trace = [
            _turn(
                projection={
                    "intake_state": {
                        "fields_collected": ["ad_id", "appeal_reason"]
                    },
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("intake_state.fields_collected.contains(ad_id)")
        assert evaluate_trace_check(ast, view) is True

    def test_field_missing_returns_fail(self):
        trace = [
            _turn(
                projection={
                    "intake_state": {"fields_collected": ["ad_id"]},
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "intake_state.fields_collected.contains(appeal_reason)"
        )
        assert evaluate_trace_check(ast, view) is False

    def test_quoted_field_argument_accepted(self):
        trace = [
            _turn(
                projection={
                    "intake_state": {"fields_collected": ["ad_id"]},
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "intake_state.fields_collected.contains('ad_id')"
        )
        assert evaluate_trace_check(ast, view) is True

    def test_intake_state_nested_under_session_also_resolved(self):
        # Some trace shapes nest intake_state inside session — extractor
        # should fall through to that location.
        trace = [
            _turn(
                projection={
                    "session": {
                        "session_id": "abc",
                        "intake_state": {"fields_collected": ["ad_id"]},
                    }
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("intake_state.fields_collected.contains(ad_id)")
        assert evaluate_trace_check(ast, view) is True


# ---------------------------------------------------------------------------
# Primitive 4 — session.<flag>_present
# ---------------------------------------------------------------------------


class TestSessionFlagPresent:
    def test_explicit_present_key_truthy(self):
        trace = [
            _turn(
                projection={
                    "session": {"moderation_context_present": True},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("session.moderation_context_present")
        assert evaluate_trace_check(ast, view) is True

    def test_explicit_present_key_falsy(self):
        trace = [
            _turn(projection={"session": {"moderation_context_present": False}})
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("session.moderation_context_present")
        assert evaluate_trace_check(ast, view) is False

    def test_bare_flag_truthy_via_fallback(self):
        # When the session does not carry the explicit ``_present`` key but
        # does carry a truthy value at the bare flag name, the fallback
        # resolves it.
        trace = [_turn(projection={"session": {"moderation_context": "ok"}})]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("session.moderation_context_present")
        assert evaluate_trace_check(ast, view) is True

    def test_missing_flag_returns_fail(self):
        trace = [_turn(projection={"session": {}})]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check("session.moderation_context_present")
        assert evaluate_trace_check(ast, view) is False


# ---------------------------------------------------------------------------
# Combinators — any_of / all_of (nested)
# ---------------------------------------------------------------------------


class TestCombinators:
    def test_any_of_short_circuits_to_pass(self):
        trace = [
            _turn(
                projection={
                    "accumulated_tool_results": {"search_knowledge": {}},
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "any_of(accumulated_tool_results.search_knowledge, "
            "accumulated_tool_results.classify_use_case)"
        )
        assert evaluate_trace_check(ast, view) is True

    def test_any_of_all_miss_returns_fail(self):
        trace = [_turn(projection={"accumulated_tool_results": {}, "session": {}})]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "any_of(accumulated_tool_results.search_knowledge, "
            "accumulated_tool_results.classify_use_case)"
        )
        assert evaluate_trace_check(ast, view) is False

    def test_all_of_all_pass_returns_pass(self):
        trace = [
            _turn(
                projection={
                    "accumulated_tool_results": {
                        "search_knowledge": {},
                        "resolve_article": {},
                    },
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "all_of(accumulated_tool_results.search_knowledge, "
            "accumulated_tool_results.resolve_article)"
        )
        assert evaluate_trace_check(ast, view) is True

    def test_all_of_one_fail_returns_fail(self):
        trace = [
            _turn(
                projection={
                    "accumulated_tool_results": {"search_knowledge": {}},
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "all_of(accumulated_tool_results.search_knowledge, "
            "accumulated_tool_results.resolve_article)"
        )
        assert evaluate_trace_check(ast, view) is False

    def test_nested_combinators_two_levels_deep(self):
        # all_of( any_of(A, B), accumulated_tool_results.C )
        trace = [
            _turn(
                projection={
                    "accumulated_tool_results": {
                        "search_knowledge": {},
                        "get_customer_context": {},
                    },
                    "session": {},
                }
            )
        ]
        view = TraceView(per_turn_trace=trace)
        ast = parse_trace_check(
            "all_of("
            "any_of(accumulated_tool_results.search_knowledge, "
            "accumulated_tool_results.classify_use_case), "
            "accumulated_tool_results.get_customer_context)"
        )
        assert evaluate_trace_check(ast, view) is True


# ---------------------------------------------------------------------------
# Extractor — end-to-end PASS / FAIL / N/A
# ---------------------------------------------------------------------------


class TestSkillProcedureExtractor:
    def test_empty_critical_steps_returns_empty_list(self):
        skill = _skill_with()  # no steps
        ext = SkillProcedureExtractor.from_skills([skill])
        results = ext.extract([_turn()], active_skill=skill, active_use_case="UC-A")
        assert results == []

    def test_mandatory_for_match_evaluates_step(self):
        skill = _skill_with(
            CriticalStep(
                id="s1",
                desc="Retrieve a knowledge article first.",
                trace_check="accumulated_tool_results.search_knowledge",
                mandatory_for=("UC-A",),
                severity="mandatory",
            )
        )
        ext = SkillProcedureExtractor.from_skills([skill])
        trace = [
            _turn(
                projection={
                    "accumulated_tool_results": {"search_knowledge": {}},
                    "session": {},
                }
            )
        ]
        results = ext.extract(trace, active_skill=skill, active_use_case="UC-A")
        assert len(results) == 1
        assert results[0].outcome == "PASS"
        assert results[0].severity == "mandatory"
        assert results[0].step_id == "s1"

    def test_mandatory_for_mismatch_returns_NA(self):
        # Step is mandatory_for UC-A, but the active case is UC-B → N/A
        # (no gate effect either direction).
        skill = _skill_with(
            CriticalStep(
                id="s1",
                desc="UC-A-only check.",
                trace_check="accumulated_tool_results.search_knowledge",
                mandatory_for=("UC-A",),
                severity="mandatory",
            )
        )
        ext = SkillProcedureExtractor.from_skills([skill])
        trace = [_turn()]
        results = ext.extract(trace, active_skill=skill, active_use_case="UC-B")
        assert len(results) == 1
        assert results[0].outcome == "N/A"
        assert "not applicable" in results[0].detail

    def test_resolve_by_skill_name(self):
        skill = _skill_with(
            CriticalStep(
                id="s1",
                desc="Stub.",
                trace_check="accumulated_tool_results.search_knowledge",
                mandatory_for=("UC-A",),
                severity="mandatory",
            ),
            name="resolve_faq_grounded_answer",
        )
        ext = SkillProcedureExtractor.from_skills([skill])
        trace = [
            _turn(
                projection={
                    "accumulated_tool_results": {"search_knowledge": {}},
                    "session": {},
                }
            )
        ]
        # Pass the Skill name (string) instead of the object — extractor
        # resolves via skills_by_name.
        results = ext.extract(
            trace,
            active_skill="resolve_faq_grounded_answer",
            active_use_case="UC-A",
        )
        assert len(results) == 1
        assert results[0].outcome == "PASS"

    def test_unknown_active_skill_returns_empty(self):
        skill = _skill_with(name="known_skill")
        ext = SkillProcedureExtractor.from_skills([skill])
        results = ext.extract([_turn()], active_skill="unknown_skill", active_use_case="UC-A")
        assert results == []


# ---------------------------------------------------------------------------
# tier2_results_to_gate — composite-adapter contract
# ---------------------------------------------------------------------------


class TestTier2GateAdapter:
    def test_empty_results_pass_advisory(self):
        result: Tier2Result = tier2_results_to_gate([])
        assert result.passed is True
        assert result.severity == "advisory"

    def test_mandatory_fail_flips_to_critical(self):
        ext = SkillProcedureExtractor.from_skills([
            _skill_with(
                CriticalStep(
                    id="s1",
                    desc="never matches",
                    trace_check="accumulated_tool_results.never_called",
                    mandatory_for=("UC-A",),
                    severity="mandatory",
                )
            )
        ])
        results = ext.extract([_turn()], "synthetic", "UC-A")
        gate = tier2_results_to_gate(results)
        assert gate.passed is False
        assert gate.severity == "critical"
        assert gate.failed_step_ids == ["s1"]

    def test_advisory_fail_does_not_flip_gate(self):
        ext = SkillProcedureExtractor.from_skills([
            _skill_with(
                CriticalStep(
                    id="adv_only",
                    desc="advisory",
                    trace_check="accumulated_tool_results.never_called",
                    mandatory_for=("UC-A",),
                    severity="advisory",
                )
            )
        ])
        results = ext.extract([_turn()], "synthetic", "UC-A")
        gate = tier2_results_to_gate(results)
        assert gate.passed is True
        assert gate.severity == "advisory"
        assert gate.failed_step_ids == ["adv_only"]

    def test_all_pass_advisory_severity_critical(self):
        ext = SkillProcedureExtractor.from_skills([
            _skill_with(
                CriticalStep(
                    id="ok",
                    desc="ok",
                    trace_check="accumulated_tool_results.search_knowledge",
                    mandatory_for=("UC-A",),
                    severity="mandatory",
                )
            )
        ])
        results = ext.extract(
            [
                _turn(
                    projection={
                        "accumulated_tool_results": {"search_knowledge": {}},
                        "session": {},
                    }
                )
            ],
            "synthetic",
            "UC-A",
        )
        gate = tier2_results_to_gate(results)
        assert gate.passed is True
        assert gate.severity == "critical"
        assert gate.failed_step_ids == []


# ---------------------------------------------------------------------------
# Production-Skill load smoke — all 6 YAMLs load with empty critical_steps.
# ---------------------------------------------------------------------------


class TestProductionSkillLoad:
    # Sprint 44 (S-Eval-3, NEW Milestone M3-Eval sub-sprint 3) update of the
    # S-Eval-2 close checkpoint. S-Eval-2 anchored the empty-default state;
    # S-Eval-3 populates 18 critical_steps across the 6 Skills per the
    # contract §2 per-Skill anticipated distribution table.

    _EXPECTED_PER_SKILL_COUNTS = {
        "discover_triage": 3,
        "confirm": 2,
        "resolve_faq_grounded_answer": 5,
        "resolve_intake_collect_and_handover": 5,
        "escalate": 2,
        "terminal": 1,
    }

    def test_load_all_six_production_skills_populated_critical_steps(self):
        from pathlib import Path

        from eval_interactive.scoring.skill_procedure_check import (
            load_skills_from_dir,
        )

        skills_dir = (
            Path(__file__).resolve().parents[2]
            / "server"
            / "src"
            / "main"
            / "resources"
            / "skills"
        )
        skills = load_skills_from_dir(skills_dir)
        assert len(skills) == 6, (
            f"expected 6 production Skill YAMLs, found {len(skills)} under {skills_dir}"
        )
        total = 0
        for s in skills:
            expected = self._EXPECTED_PER_SKILL_COUNTS.get(s.name)
            assert expected is not None, (
                f"S-Eval-3 expected-count map missing entry for Skill={s.name!r}"
            )
            assert len(s.critical_steps) == expected, (
                f"S-Eval-3 per-Skill critical_steps count for {s.name!r} expected "
                f"{expected}, got {len(s.critical_steps)}"
            )
            for step in s.critical_steps:
                assert step.id and step.desc and step.trace_check
                assert step.mandatory_for, (
                    f"Skill {s.name!r} step {step.id!r} has empty mandatory_for"
                )
                assert step.severity in ("mandatory", "advisory")
            total += len(s.critical_steps)
        assert total == 18, (
            f"S-Eval-3 expected 18 populated critical_steps total across the 6 "
            f"Skills (within contract §2 envelope 16-22 lower-bound; 18-30 outer "
            f"envelope), got {total}"
        )

    def test_extractor_handles_full_production_skill_set_without_crash(self):
        from pathlib import Path

        from eval_interactive.scoring.skill_procedure_check import (
            load_skills_from_dir,
        )

        skills_dir = (
            Path(__file__).resolve().parents[2]
            / "server"
            / "src"
            / "main"
            / "resources"
            / "skills"
        )
        # Extractor construction parses every step's trace_check; the
        # S-Eval-2 DSL parser is the §1.7 structural defence. If any
        # populated trace_check is malformed or contains a forbidden
        # primitive, this construction raises TraceCheckDSLSyntaxError.
        ext = SkillProcedureExtractor.from_skills(
            load_skills_from_dir(skills_dir)
        )
        # Smoke: extract() runs without crash for every Skill name on a
        # trivial empty trace. Per-step outcomes (PASS/FAIL/N/A) are
        # exercised by the cluster-specific tests in this file and by the
        # offline verification recorded in the S-Eval-3 handoff §9. The
        # populated production set returns either applicable per-step
        # results (when the active_use_case is in the step's
        # mandatory_for) or N/A entries.
        for skill_name in [
            "discover_triage",
            "confirm",
            "resolve_faq_grounded_answer",
            "resolve_intake_collect_and_handover",
            "escalate",
            "terminal",
        ]:
            results = ext.extract([_turn()], active_skill=skill_name, active_use_case="UC-A")
            # No exception raised is the smoke pass. Result list shape is
            # determined by each Skill's per-step mandatory_for scoping
            # against UC-A.
            assert isinstance(results, list)
