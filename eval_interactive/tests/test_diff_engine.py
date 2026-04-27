"""Tests for the DiffEngine comparison module."""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from eval_interactive.comparison.diff_engine import DiffEngine, ComparisonResult


def _write_results(path: Path, data: dict) -> None:
    """Write a results.json file."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f)


def _make_results(label: str, cases: list[dict], summary: dict | None = None) -> dict:
    """Build a results dict."""
    return {
        "label": label,
        "summary": summary or {},
        "case_results": cases,
    }


def _make_case(case_id: str, passed: bool, composite: float, tags: list[str] | None = None) -> dict:
    return {
        "case_id": case_id,
        "case_passed": passed,
        "composite_score": composite,
        "failure_tags": tags or [],
    }


class TestDiffEngine:
    def test_all_stable_pass(self, tmp_path: Path):
        baseline = _make_results("baseline", [
            _make_case("c1", True, 0.9),
            _make_case("c2", True, 0.8),
        ])
        current = _make_results("current", [
            _make_case("c1", True, 0.95),
            _make_case("c2", True, 0.85),
        ])
        _write_results(tmp_path / "b" / "results.json", baseline)
        _write_results(tmp_path / "c" / "results.json", current)

        engine = DiffEngine()
        result = engine.compare(str(tmp_path / "b"), str(tmp_path / "c"))

        assert result.total_cases == 2
        assert len(result.stable_pass) == 2
        assert len(result.regressed) == 0
        assert len(result.improved) == 0

    def test_regression_detected(self, tmp_path: Path):
        baseline = _make_results("baseline", [
            _make_case("c1", True, 0.9),
        ])
        current = _make_results("current", [
            _make_case("c1", False, 0.3, ["L1:no_stall"]),
        ])
        _write_results(tmp_path / "b" / "results.json", baseline)
        _write_results(tmp_path / "c" / "results.json", current)

        engine = DiffEngine()
        result = engine.compare(str(tmp_path / "b"), str(tmp_path / "c"))

        assert len(result.regressed) == 1
        assert result.regressed[0]["case_id"] == "c1"
        assert result.regression_rate > 0

    def test_improvement_detected(self, tmp_path: Path):
        baseline = _make_results("baseline", [
            _make_case("c1", False, 0.3),
        ])
        current = _make_results("current", [
            _make_case("c1", True, 0.9),
        ])
        _write_results(tmp_path / "b" / "results.json", baseline)
        _write_results(tmp_path / "c" / "results.json", current)

        engine = DiffEngine()
        result = engine.compare(str(tmp_path / "b"), str(tmp_path / "c"))

        assert len(result.improved) == 1
        assert result.improved[0]["case_id"] == "c1"

    def test_new_and_resolved_failure_tags(self, tmp_path: Path):
        baseline = _make_results("baseline", [
            _make_case("c1", False, 0.3, ["L1:old_tag"]),
        ])
        current = _make_results("current", [
            _make_case("c1", False, 0.3, ["L1:new_tag"]),
        ])
        _write_results(tmp_path / "b" / "results.json", baseline)
        _write_results(tmp_path / "c" / "results.json", current)

        engine = DiffEngine()
        result = engine.compare(str(tmp_path / "b"), str(tmp_path / "c"))

        assert "L1:new_tag" in result.new_failures
        assert "L1:old_tag" in result.resolved_failures

    def test_metric_deltas(self, tmp_path: Path):
        baseline = _make_results("baseline", [], summary={
            "task_success_rate": 0.8,
            "mean_composite_score": 0.7,
        })
        current = _make_results("current", [], summary={
            "task_success_rate": 0.9,
            "mean_composite_score": 0.75,
        })
        _write_results(tmp_path / "b" / "results.json", baseline)
        _write_results(tmp_path / "c" / "results.json", current)

        engine = DiffEngine()
        result = engine.compare(str(tmp_path / "b"), str(tmp_path / "c"))

        assert "task_success_rate" in result.metric_deltas
        delta = result.metric_deltas["task_success_rate"]
        assert delta["delta"] == pytest.approx(0.1, abs=0.001)

    def test_missing_file_raises(self, tmp_path: Path):
        engine = DiffEngine()
        with pytest.raises(FileNotFoundError):
            engine.compare(str(tmp_path / "nonexistent"), str(tmp_path / "also_none"))

    def test_format_report_produces_string(self, tmp_path: Path):
        baseline = _make_results("baseline", [
            _make_case("c1", True, 0.9),
        ])
        current = _make_results("current", [
            _make_case("c1", True, 0.95),
        ])
        _write_results(tmp_path / "b" / "results.json", baseline)
        _write_results(tmp_path / "c" / "results.json", current)

        engine = DiffEngine()
        result = engine.compare(str(tmp_path / "b"), str(tmp_path / "c"))
        report = engine.format_report(result)

        assert isinstance(report, str)
        assert "EVALUATION COMPARISON REPORT" in report
        assert "baseline" in report
        assert "current" in report

    def test_cases_in_only_one_run(self, tmp_path: Path):
        """Cases present in only one run should still be handled."""
        baseline = _make_results("baseline", [
            _make_case("c1", True, 0.9),
        ])
        current = _make_results("current", [
            _make_case("c1", True, 0.9),
            _make_case("c2", True, 0.8),
        ])
        _write_results(tmp_path / "b" / "results.json", baseline)
        _write_results(tmp_path / "c" / "results.json", current)

        engine = DiffEngine()
        result = engine.compare(str(tmp_path / "b"), str(tmp_path / "c"))
        assert result.total_cases == 2
