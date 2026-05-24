"""Tests for JsonReportGenerator and HtmlReportGenerator.

Sprint 50 / Milestone M5 — Observability Coherence S1 extended this
suite with assertions for the M3-Eval four-tier verdict surface
(``suite_authority`` header, Tier-0/1/2/3 sections, ``_render_tier2``)
and a regression that the pre-M3 Phase-5 §6.9 7-metric dashboard
*rendering* is gone (its aggregates remain in ``results.json``).
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from eval_interactive.report.json_report import JsonReportGenerator
from eval_interactive.report.html_report import HtmlReportGenerator


def _make_case_results() -> list[dict]:
    return [
        {
            "case_id": "case-001",
            "case_passed": True,
            "case_passed_authority": "programmatic",
            "composite_score": 0.85,
            "outcome_score": 0.9,
            "judge_score": 0.8,
            "stop_reason": "goal_achieved",
            "total_turns": 4,
            "failure_tags": [],
            "stall_detected": False,
            "l1_results": [{"check_name": "no_stall", "passed": True, "detail": ""}],
            "l2_results": [{"check_name": "correct_uc", "score": 1.0, "detail": ""}],
            "l3_results": [{"dimension": "groundedness", "score": 4.5, "reasoning": "good"}],
            "tier2_result": {
                "passed": True,
                "severity": "critical",
                "failed_step_ids": [],
                "detail": "all applicable mandatory critical steps passed",
                "per_step": [
                    {
                        "step_id": "search-knowledge-before-faq-answer",
                        "outcome": "PASS",
                        "severity": "mandatory",
                        "detail": "accumulated_tool_results.search_knowledge",
                    }
                ],
            },
        },
        {
            "case_id": "case-002",
            "case_passed": False,
            "case_passed_authority": "programmatic",
            "composite_score": 0.3,
            "outcome_score": 0.0,
            "judge_score": 0.6,
            "stop_reason": "max_turns_exceeded",
            "total_turns": 15,
            "failure_tags": ["L2:correct_uc"],
            "stall_detected": True,
            "l1_results": [],
            "l2_results": [{"check_name": "correct_uc", "score": 0.0, "detail": "wrong UC"}],
            "l3_results": [],
            "tier2_result": {
                "passed": False,
                "severity": "critical",
                "failed_step_ids": ["uc-h-intake-complete-before-handover"],
                "detail": "critical step missing",
                "per_step": [],
            },
        },
    ]


def _make_summary() -> dict:
    return {
        "total_cases": 2,
        "passed_cases": 1,
        "failed_cases": 1,
        "task_success_rate": 0.5,
        "stall_rate": 0.5,
        "mean_composite_score": 0.575,
        "mean_outcome_score": 0.45,
        "mean_judge_score": 0.7,
        "per_uc_breakdown": {
            "UC-A": {"count": 1, "passed": 1, "failed": 0, "mean_composite": 0.85},
            "UC-B": {"count": 1, "passed": 0, "failed": 1, "mean_composite": 0.3},
        },
        "policy_compliance_rate": 1.0,
        "mean_turns_to_resolution": 9.5,
        "suite_authority": "programmatic",
    }


def _make_human_review_cases() -> list[dict]:
    """Bad-case-suite-shape cases (all ``case_passed_authority="human_review"``)."""
    return [
        {
            "case_id": "alice_uc_a_uc_h_misclass",
            "case_passed": True,
            "case_passed_authority": "human_review",
            "composite_score": 0.5,
            "outcome_score": 1.0,
            "judge_score": 0.0,
            "stop_reason": "goal_achieved",
            "total_turns": 6,
            "failure_tags": [],
            "stall_detected": False,
            "l1_results": [
                {"check_name": "no_pii_leakage", "passed": True, "detail": ""},
                {"check_name": "no_critical_policy_violation", "passed": True, "detail": ""},
            ],
            "l2_results": [],
            "l3_results": [],
            "tier2_result": {
                "passed": True,
                "severity": "critical",
                "failed_step_ids": [],
                "detail": "passed",
                "per_step": [],
            },
        },
        {
            "case_id": "cs012_uc_fp_late_phone_failure_path",
            "case_passed": False,
            "case_passed_authority": "human_review",
            "composite_score": 0.0,
            "outcome_score": 0.0,
            "judge_score": 0.0,
            "stop_reason": "max_turns_exceeded",
            "total_turns": 8,
            "failure_tags": ["TIER2:uc-h-intake-complete-before-handover"],
            "stall_detected": False,
            "l1_results": [
                {"check_name": "no_pii_leakage", "passed": False, "detail": "kitten.seller@example.com"},
            ],
            "l2_results": [],
            "l3_results": [],
            "tier2_result": {
                "passed": False,
                "severity": "critical",
                "failed_step_ids": ["uc-h-intake-complete-before-handover"],
                "detail": "critical step missing",
                "per_step": [
                    {
                        "step_id": "uc-h-intake-complete-before-handover",
                        "outcome": "FAIL",
                        "severity": "mandatory",
                        "detail": "intake fields missing at handover",
                    }
                ],
            },
        },
    ]


def _make_human_review_summary() -> dict:
    return {
        "total_cases": 2,
        "passed_cases": 0,
        "failed_cases": 2,
        "task_success_rate": 0.0,
        "stall_rate": 0.0,
        "mean_composite_score": 0.25,
        "mean_outcome_score": 0.5,
        "mean_judge_score": 0.0,
        "per_uc_breakdown": {
            "UC-A": {"count": 1, "passed": 0, "failed": 1, "mean_composite": 0.5},
            "UC-FP": {"count": 1, "passed": 0, "failed": 1, "mean_composite": 0.0},
        },
        "policy_compliance_rate": 0.5,
        "mean_turns_to_resolution": 7.0,
        "suite_authority": "human_review",
    }


class TestJsonReportGenerator:
    def test_generate_returns_dict(self):
        gen = JsonReportGenerator()
        report = gen.generate("run-001", "test_label", _make_case_results(), _make_summary())
        assert isinstance(report, dict)
        assert report["run_id"] == "run-001"
        assert report["label"] == "test_label"
        assert len(report["cases"]) == 2

    def test_save_creates_file(self, tmp_path: Path):
        gen = JsonReportGenerator()
        report = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        output = tmp_path / "report.json"
        gen.save(report, output)
        assert output.exists()
        with open(output) as f:
            loaded = json.load(f)
        assert loaded["run_id"] == "run-001"

    def test_summary_has_required_fields(self):
        gen = JsonReportGenerator()
        report = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        summary = report["summary"]
        assert "total_cases" in summary
        assert "task_success_rate" in summary
        assert "mean_composite_score" in summary

    def test_case_normalization(self):
        gen = JsonReportGenerator()
        report = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        case = report["cases"][0]
        assert "case_id" in case
        assert "case_passed" in case
        assert "composite_score" in case
        assert "l1_results" in case

    def test_comparison_included_when_provided(self):
        gen = JsonReportGenerator()
        comparison = {"baseline_label": "v1", "current_label": "v2"}
        report = gen.generate("run-001", "test", [], {}, comparison=comparison)
        assert report["comparison"] == comparison

    def test_comparison_none_when_not_provided(self):
        gen = JsonReportGenerator()
        report = gen.generate("run-001", "test", [], {})
        assert report["comparison"] is None


class TestHtmlReportGenerator:
    def test_generate_returns_html_string(self):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test_label", _make_case_results(), _make_summary())
        assert isinstance(html, str)
        assert "<!DOCTYPE html>" in html
        assert "</html>" in html

    def test_html_contains_run_info(self):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test_label", _make_case_results(), _make_summary())
        assert "run-001" in html
        assert "test_label" in html

    def test_html_contains_case_ids(self):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        assert "case-001" in html
        assert "case-002" in html

    def test_html_contains_pass_fail_badges(self):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        # Per-case tier-labelled badges replaced the binary PASS/FAIL
        # badge (Sprint 50 / M5 S1); the words still appear inside the
        # tier-labelled badge spans.
        assert ">PASS<" in html
        assert ">FAIL<" in html

    def test_html_contains_stall_badge(self):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        assert "STALL" in html

    def test_save_creates_file(self, tmp_path: Path):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        output = tmp_path / "report.html"
        gen.save(html, output)
        assert output.exists()
        content = output.read_text()
        assert "<!DOCTYPE html>" in content

    def test_html_with_empty_cases(self):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test", [], _make_summary())
        assert "No case data available" in html

    def test_html_escapes_special_characters(self):
        gen = HtmlReportGenerator()
        cases = [{
            "case_id": "<script>alert('xss')</script>",
            "case_passed": True,
            "composite_score": 0.8,
            "stall_detected": False,
            "failure_tags": [],
            "l1_results": [],
            "l2_results": [],
            "l3_results": [],
        }]
        html = gen.generate("run-001", "test", cases, _make_summary())
        assert "<script>" not in html
        assert "&lt;script&gt;" in html

    def test_html_with_comparison(self):
        gen = HtmlReportGenerator()
        comparison = {
            "baseline_label": "v1",
            "current_label": "v2",
            "overall_baseline_score": 0.7,
            "overall_current_score": 0.8,
            "overall_delta": 0.1,
            "case_diffs": [],
            "metric_deltas": {},
        }
        html = gen.generate("run-001", "test", [], _make_summary(), comparison=comparison)
        assert "Regression Comparison" in html


# ---------------------------------------------------------------------------
# Sprint 50 / M5 S1 — four-tier verdict surface
# ---------------------------------------------------------------------------


class TestFourTierVerdictSurface:
    """Assertions on the M3-Eval four-tier HTML verdict surface."""

    def test_html_contains_all_four_tier_sections(self):
        gen = HtmlReportGenerator()
        html = gen.generate(
            "run-001", "bad_cases_run", _make_human_review_cases(), _make_human_review_summary()
        )
        assert "Tier-0 Safety Floor" in html
        assert "Tier-1 Outcome" in html
        assert "Tier-2 Critical-flow" in html
        # Tier-3 advisory section heading
        assert "Tier-3 Polish" in html

    def test_html_header_renders_suite_authority_badge_human_review(self):
        gen = HtmlReportGenerator()
        html = gen.generate(
            "run-001", "bad_cases_run", _make_human_review_cases(), _make_human_review_summary()
        )
        # Authority badge surfaces the resolved value + the
        # human-judgment guidance line.
        assert "suite-authority-badge" in html
        assert "human_review" in html
        assert "HUMAN-JUDGMENT" in html
        # Pointer to the bad-case manifest (the human-judgment verdict
        # source-of-truth per §5.6) appears.
        assert "eval_interactive/case_specs/bad_cases/_manifest.md" in html

    def test_html_header_renders_suite_authority_badge_programmatic(self):
        gen = HtmlReportGenerator()
        html = gen.generate(
            "run-001", "programmatic_run", _make_case_results(), _make_summary()
        )
        assert "suite-authority-badge programmatic" in html
        assert "PROGRAMMATIC suite" in html

    def test_html_per_case_tier_labelled_badges(self):
        gen = HtmlReportGenerator()
        html = gen.generate(
            "run-001", "bad_cases_run", _make_human_review_cases(), _make_human_review_summary()
        )
        # Tier-labelled badges replace the single binary PASS/FAIL
        # badge (Sprint 50 / M5 S1).
        assert "badge tier" in html
        # Per-case Tier-1 label for a human_review case is
        # HUMAN_REVIEW, not PASS/FAIL.
        assert "HUMAN_REVIEW" in html

    def test_html_human_review_renders_informational_note(self):
        gen = HtmlReportGenerator()
        html = gen.generate(
            "run-001", "bad_cases_run", _make_human_review_cases(), _make_human_review_summary()
        )
        # The four-tier header annotates that programmatic counts are
        # informational, not the gate, for human-judgment suites.
        assert "informational" in html

    def test_html_renders_tier2_critical_failure_case_ids(self):
        gen = HtmlReportGenerator()
        html = gen.generate(
            "run-001", "bad_cases_run", _make_human_review_cases(), _make_human_review_summary()
        )
        # cs012 is the critical-Tier-2 failure in the fixture; its
        # case_id should appear in the Tier-2 failures table.
        assert "cs012_uc_fp_late_phone_failure_path" in html
        # The failed step id surfaces too.
        assert "uc-h-intake-complete-before-handover" in html

    def test_html_per_uc_table_kept_and_reframed(self):
        gen = HtmlReportGenerator()
        html = gen.generate(
            "run-001", "test", _make_case_results(), _make_summary()
        )
        # The legacy per-UC rollup is kept but reframed as a
        # programmatic-suite rollup (Sprint 50 / M5 S1 #2).
        assert "Per Use-Case Breakdown" in html
        assert "programmatic-suite rollup" in html


class TestSuiteAuthorityResolution:
    """Direct assertions on the suite_authority resolution path."""

    def test_resolve_human_review_when_all_cases_human_review(self):
        gen = HtmlReportGenerator()
        authority = gen._resolve_suite_authority(
            _make_human_review_summary(), _make_human_review_cases()
        )
        assert authority == "human_review"

    def test_resolve_programmatic_when_all_cases_programmatic(self):
        gen = HtmlReportGenerator()
        authority = gen._resolve_suite_authority(
            _make_summary(), _make_case_results()
        )
        assert authority == "programmatic"

    def test_resolve_mixed_when_both_authorities_present(self):
        gen = HtmlReportGenerator()
        cases = _make_case_results() + _make_human_review_cases()
        # Summary missing suite_authority forces a per-case fallback
        # derivation — exercises the mixed branch.
        summary = {k: v for k, v in _make_summary().items() if k != "suite_authority"}
        authority = gen._resolve_suite_authority(summary, cases)
        assert authority == "mixed"

    def test_resolve_prefers_summary_field_over_per_case_derivation(self):
        gen = HtmlReportGenerator()
        # Per-case authority says human_review, summary says
        # programmatic; aggregate field wins (single source of truth).
        summary = dict(_make_human_review_summary())
        summary["suite_authority"] = "programmatic"
        authority = gen._resolve_suite_authority(
            summary, _make_human_review_cases()
        )
        assert authority == "programmatic"

    def test_resolve_falls_back_to_programmatic_on_empty(self):
        gen = HtmlReportGenerator()
        authority = gen._resolve_suite_authority({}, [])
        assert authority == "programmatic"


class TestRenderTier2:
    """Assertions on the new ``_render_tier2`` per-case block."""

    def test_render_tier2_pass_surfaces_pass_badge(self):
        gen = HtmlReportGenerator()
        html = gen._render_tier2(
            {
                "passed": True,
                "severity": "critical",
                "failed_step_ids": [],
                "detail": "all applicable mandatory critical steps passed",
                "per_step": [
                    {
                        "step_id": "search-knowledge-before-faq-answer",
                        "outcome": "PASS",
                        "severity": "mandatory",
                        "detail": "ok",
                    }
                ],
            }
        )
        assert "PASS" in html
        assert "search-knowledge-before-faq-answer" in html
        assert "severity" in html

    def test_render_tier2_fail_surfaces_failed_step_ids(self):
        gen = HtmlReportGenerator()
        html = gen._render_tier2(
            {
                "passed": False,
                "severity": "critical",
                "failed_step_ids": ["uc-h-intake-complete-before-handover"],
                "detail": "critical step missing",
                "per_step": [
                    {
                        "step_id": "uc-h-intake-complete-before-handover",
                        "outcome": "FAIL",
                        "severity": "mandatory",
                        "detail": "missing",
                    }
                ],
            }
        )
        assert "FAIL" in html
        assert "uc-h-intake-complete-before-handover" in html
        assert "failed_step_ids" in html
        assert "critical" in html

    def test_render_tier2_empty_returns_placeholder(self):
        gen = HtmlReportGenerator()
        html = gen._render_tier2(None)
        assert "No Tier-2 result" in html


class TestPhase5DashboardRemoved:
    """Regression: the pre-M3 Phase-5 §6.9 7-metric dashboard rendering
    must not return.

    Sprint 50 / M5 S1 removed the HTML *rendering* of the 7-card
    dashboard (Alternative B). The underlying aggregates remain
    computed by ``batch.executor._compute_summary`` and are still
    present in ``results.json`` (backward-compat); only the HTML
    rendering is stripped.
    """

    def test_html_no_key_metrics_dashboard_headline(self):
        gen = HtmlReportGenerator()
        html = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        # The pre-M3 dashboard headline + card titles must be absent.
        assert "Key Metrics Dashboard" not in html
        # Several Phase-5 §6.9 card labels removed by the four-tier
        # rewrite (they were the visible-eval card labels in the
        # pre-Sprint-50 renderer):
        assert "Correct Tool Invocation Rate" not in html
        assert "Escalation Correctness Rate" not in html
        assert "Grounded Final Answer Rate" not in html
        assert "Task Success Rate" not in html
        # The misleading "0/12 passed" framing came from a
        # "<h2>Pass / Fail Overview</h2>" block; it must be gone.
        assert "Pass / Fail Overview" not in html

    def test_executor_summary_aggregates_still_present_for_backward_compat(self):
        """The data side stays — only the HTML rendering is removed."""
        # Smoke-test the JSON renderer: the legacy 7-metric aggregates
        # must still serialise so downstream consumers of
        # results.json do not break.
        gen = JsonReportGenerator()
        report = gen.generate("run-001", "test", _make_case_results(), _make_summary())
        summary = report["summary"]
        assert "task_success_rate" in summary
        assert "policy_compliance_rate" in summary
        assert "mean_turns_to_resolution" in summary
        # And the new suite_authority field is surfaced.
        assert summary["suite_authority"] == "programmatic"


class TestJsonReportSuiteAuthority:
    """JSON renderer surfaces ``suite_authority`` so the two renderers do not drift."""

    def test_json_summary_emits_suite_authority_human_review(self):
        gen = JsonReportGenerator()
        report = gen.generate(
            "run-001", "bad_cases_run", _make_human_review_cases(), _make_human_review_summary()
        )
        assert report["summary"]["suite_authority"] == "human_review"

    def test_json_summary_emits_suite_authority_programmatic_default(self):
        gen = JsonReportGenerator()
        # Summary without an explicit suite_authority — fall back to
        # the safety default (matches the per-case resolver).
        summary = {k: v for k, v in _make_summary().items() if k != "suite_authority"}
        report = gen.generate("run-001", "test", _make_case_results(), summary)
        assert report["summary"]["suite_authority"] == "programmatic"

    def test_json_case_payload_includes_authority_and_tier2(self):
        gen = JsonReportGenerator()
        report = gen.generate(
            "run-001", "bad_cases_run", _make_human_review_cases(), _make_human_review_summary()
        )
        case0 = report["cases"][0]
        assert case0["case_passed_authority"] == "human_review"
        assert "tier2_result" in case0
        assert case0["tier2_result"]["passed"] is True


# ---------------------------------------------------------------------------
# Executor _compute_summary suite_authority (single source of truth)
# ---------------------------------------------------------------------------


class TestComputeSummarySuiteAuthority:
    """Assertions on :func:`batch.executor.BatchExecutor._compute_summary`
    emitting ``suite_authority`` once per run."""

    @staticmethod
    def _executor():
        from eval_interactive.batch.executor import BatchExecutor

        # _compute_summary is a method but does not touch instance
        # state; construct via __new__ to skip __init__ wiring.
        return BatchExecutor.__new__(BatchExecutor)

    def test_compute_summary_all_human_review(self):
        ex = self._executor()
        cases = [
            {"case_passed_authority": "human_review", "case_passed": False, "composite_score": 0.0, "outcome_score": 0.0, "judge_score": 0.0, "stall_detected": False, "total_turns": 0, "primary_uc": "UC-A", "expected_outcome": "resolve", "containment_outcome": "resolved", "failure_tags": []},
            {"case_passed_authority": "human_review", "case_passed": True, "composite_score": 0.85, "outcome_score": 1.0, "judge_score": 0.7, "stall_detected": False, "total_turns": 4, "primary_uc": "UC-A", "expected_outcome": "resolve", "containment_outcome": "resolved", "failure_tags": []},
        ]
        summary = ex._compute_summary(cases)
        assert summary["suite_authority"] == "human_review"

    def test_compute_summary_all_programmatic(self):
        ex = self._executor()
        cases = [
            {"case_passed_authority": "programmatic", "case_passed": True, "composite_score": 0.85, "outcome_score": 1.0, "judge_score": 0.7, "stall_detected": False, "total_turns": 4, "primary_uc": "UC-A", "expected_outcome": "resolve", "containment_outcome": "resolved", "failure_tags": []},
        ]
        summary = ex._compute_summary(cases)
        assert summary["suite_authority"] == "programmatic"

    def test_compute_summary_mixed(self):
        ex = self._executor()
        cases = [
            {"case_passed_authority": "human_review", "case_passed": False, "composite_score": 0.0, "outcome_score": 0.0, "judge_score": 0.0, "stall_detected": False, "total_turns": 0, "primary_uc": "UC-A", "expected_outcome": "resolve", "containment_outcome": "resolved", "failure_tags": []},
            {"case_passed_authority": "programmatic", "case_passed": True, "composite_score": 0.85, "outcome_score": 1.0, "judge_score": 0.7, "stall_detected": False, "total_turns": 4, "primary_uc": "UC-A", "expected_outcome": "resolve", "containment_outcome": "resolved", "failure_tags": []},
        ]
        summary = ex._compute_summary(cases)
        assert summary["suite_authority"] == "mixed"

    def test_compute_summary_empty_defaults_to_programmatic(self):
        ex = self._executor()
        summary = ex._compute_summary([])
        assert summary["suite_authority"] == "programmatic"

    def test_compute_summary_unannotated_coalesces_to_programmatic(self):
        """A case missing ``case_passed_authority`` (e.g., pre-Sprint-48
        result file) coalesces to programmatic via the safety default,
        matching :func:`_resolve_case_passed_authority`."""
        ex = self._executor()
        cases = [
            {"case_passed": True, "composite_score": 0.85, "outcome_score": 1.0, "judge_score": 0.7, "stall_detected": False, "total_turns": 4, "primary_uc": "UC-A", "expected_outcome": "resolve", "containment_outcome": "resolved", "failure_tags": []},
        ]
        summary = ex._compute_summary(cases)
        assert summary["suite_authority"] == "programmatic"

    def test_compute_summary_preserves_existing_aggregates(self):
        """Sprint 50 / M5 S1 hard fence: existing summary fields stay;
        only ``suite_authority`` is added."""
        ex = self._executor()
        cases = [
            {"case_passed_authority": "programmatic", "case_passed": True, "composite_score": 0.85, "outcome_score": 1.0, "judge_score": 0.7, "stall_detected": False, "total_turns": 4, "primary_uc": "UC-A", "expected_outcome": "resolve", "containment_outcome": "resolved", "failure_tags": []},
        ]
        summary = ex._compute_summary(cases)
        # Existing aggregates remain (the Phase-5 §6.9 *aggregates*
        # stay; only the HTML *rendering* is removed).
        assert "task_success_rate" in summary
        assert "stall_rate" in summary
        assert "mean_composite_score" in summary
        assert "mean_outcome_score" in summary
        assert "mean_judge_score" in summary
        assert "per_uc_breakdown" in summary
        assert "policy_compliance_rate" in summary
        assert "mean_turns_to_resolution" in summary
