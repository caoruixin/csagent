"""Tests for JsonReportGenerator and HtmlReportGenerator."""

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
        },
        {
            "case_id": "case-002",
            "case_passed": False,
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
        assert "PASS" in html
        assert "FAIL" in html

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
