"""Diff engine -- compares two evaluation runs to detect regressions.

Loads two result JSON files, joins them by case_id, classifies each
case as improved / stable_pass / stable_fail / regressed, and
computes metric deltas.
"""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass, field
from pathlib import Path

logger = logging.getLogger(__name__)

_PASS_THRESHOLD = 0.7


@dataclass
class ComparisonResult:
    """Full comparison between a baseline and a current eval run."""

    baseline_label: str
    current_label: str
    total_cases: int
    improved: list[dict]  # [{case_id, baseline_composite, current_composite}]
    stable_pass: list[dict]
    stable_fail: list[dict]
    regressed: list[dict]  # [{case_id, baseline_composite, current_composite, failure_tags}]
    regression_rate: float  # regressed / total
    metric_deltas: dict  # {metric_name: {baseline, current, delta}}
    new_failures: list[str]  # failure tags in current but not baseline
    resolved_failures: list[str]  # failure tags in baseline but not current


class DiffEngine:
    """Compares two eval runs to detect regressions."""

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def compare(self, baseline_path: str, current_path: str) -> ComparisonResult:
        """Load two result JSON files, join by case_id, classify each case.

        Classification:
        - Improved: failed in baseline, passed in current
        - Stable pass: passed in both
        - Stable fail: failed in both
        - Regressed: passed in baseline, failed in current

        Args:
            baseline_path: Path to the baseline results.json.
            current_path: Path to the current results.json.

        Returns:
            ComparisonResult with per-case classifications and deltas.

        Raises:
            FileNotFoundError: If either path does not exist.
        """
        baseline_data = self._load_results(baseline_path)
        current_data = self._load_results(current_path)

        baseline_label = baseline_data.get("label", "baseline")
        current_label = current_data.get("label", "current")

        # Index case results by case_id
        baseline_by_id = {
            r["case_id"]: r for r in baseline_data.get("case_results", [])
        }
        current_by_id = {
            r["case_id"]: r for r in current_data.get("case_results", [])
        }

        # Union of all case_ids
        all_ids = sorted(set(baseline_by_id.keys()) | set(current_by_id.keys()))

        improved: list[dict] = []
        stable_pass: list[dict] = []
        stable_fail: list[dict] = []
        regressed: list[dict] = []

        baseline_failure_tags: set[str] = set()
        current_failure_tags: set[str] = set()

        for case_id in all_ids:
            b = baseline_by_id.get(case_id)
            c = current_by_id.get(case_id)

            b_composite = b.get("composite_score", 0.0) if b else 0.0
            c_composite = c.get("composite_score", 0.0) if c else 0.0
            b_passed = self._is_pass(b) if b else False
            c_passed = self._is_pass(c) if c else False

            if b:
                baseline_failure_tags.update(b.get("failure_tags", []))
            if c:
                current_failure_tags.update(c.get("failure_tags", []))

            entry = {
                "case_id": case_id,
                "baseline_composite": round(b_composite, 4),
                "current_composite": round(c_composite, 4),
            }

            if b_passed and c_passed:
                stable_pass.append(entry)
            elif not b_passed and c_passed:
                improved.append(entry)
            elif not b_passed and not c_passed:
                stable_fail.append(entry)
            else:
                # Regressed: passed in baseline, failed in current
                entry["failure_tags"] = c.get("failure_tags", []) if c else []
                regressed.append(entry)

        total = len(all_ids)
        regression_rate = len(regressed) / total if total else 0.0

        # Metric deltas from summary sections
        baseline_summary = baseline_data.get("summary", {})
        current_summary = current_data.get("summary", {})
        metric_deltas = self._compute_metric_deltas(
            baseline_summary, current_summary
        )

        # Failure tag changes
        new_failures = sorted(current_failure_tags - baseline_failure_tags)
        resolved_failures = sorted(baseline_failure_tags - current_failure_tags)

        return ComparisonResult(
            baseline_label=baseline_label,
            current_label=current_label,
            total_cases=total,
            improved=improved,
            stable_pass=stable_pass,
            stable_fail=stable_fail,
            regressed=regressed,
            regression_rate=round(regression_rate, 4),
            metric_deltas=metric_deltas,
            new_failures=new_failures,
            resolved_failures=resolved_failures,
        )

    def format_report(self, result: ComparisonResult) -> str:
        """Format a human-readable comparison report string.

        Args:
            result: The ComparisonResult to format.

        Returns:
            Multi-line report string.
        """
        lines: list[str] = []

        lines.append("=" * 60)
        lines.append("EVALUATION COMPARISON REPORT")
        lines.append("=" * 60)
        lines.append(f"Baseline: {result.baseline_label}")
        lines.append(f"Current:  {result.current_label}")
        lines.append(f"Total cases compared: {result.total_cases}")
        lines.append("")

        # Summary counts
        lines.append("--- Classification ---")
        lines.append(f"  Improved:    {len(result.improved)}")
        lines.append(f"  Stable pass: {len(result.stable_pass)}")
        lines.append(f"  Stable fail: {len(result.stable_fail)}")
        lines.append(f"  Regressed:   {len(result.regressed)}")
        lines.append(f"  Regression rate: {result.regression_rate:.1%}")
        lines.append("")

        # Metric deltas
        if result.metric_deltas:
            lines.append("--- Metric Deltas ---")
            for metric, values in sorted(result.metric_deltas.items()):
                baseline_val = values.get("baseline", 0)
                current_val = values.get("current", 0)
                delta = values.get("delta", 0)
                sign = "+" if delta > 0 else ""
                lines.append(
                    f"  {metric:30s}  "
                    f"{baseline_val:>8.4f} -> {current_val:>8.4f}  "
                    f"({sign}{delta:.4f})"
                )
            lines.append("")

        # Regressions detail
        if result.regressed:
            lines.append("--- Regressions ---")
            for r in result.regressed:
                tags = ", ".join(r.get("failure_tags", []))
                lines.append(
                    f"  {r['case_id']:30s}  "
                    f"{r['baseline_composite']:.3f} -> {r['current_composite']:.3f}"
                    f"  tags: {tags}"
                )
            lines.append("")

        # Improvements detail
        if result.improved:
            lines.append("--- Improvements ---")
            for r in result.improved:
                lines.append(
                    f"  {r['case_id']:30s}  "
                    f"{r['baseline_composite']:.3f} -> {r['current_composite']:.3f}"
                )
            lines.append("")

        # Failure tag changes
        if result.new_failures:
            lines.append("--- New Failure Tags ---")
            for tag in result.new_failures:
                lines.append(f"  + {tag}")
            lines.append("")

        if result.resolved_failures:
            lines.append("--- Resolved Failure Tags ---")
            for tag in result.resolved_failures:
                lines.append(f"  - {tag}")
            lines.append("")

        lines.append("=" * 60)
        return "\n".join(lines)

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _load_results(path: str) -> dict:
        """Load a results.json file.

        If path is a directory, appends /results.json.
        """
        p = Path(path)
        if p.is_dir():
            p = p / "results.json"
        if not p.exists():
            raise FileNotFoundError(f"Results file not found: {p}")
        with open(p, "r", encoding="utf-8") as f:
            return json.load(f)

    @staticmethod
    def _is_pass(case_result: dict) -> bool:
        """Determine if a case result counts as a pass."""
        return (
            case_result.get("case_passed", False)
            and case_result.get("composite_score", 0.0) >= _PASS_THRESHOLD
        )

    @staticmethod
    def _compute_metric_deltas(
        baseline_summary: dict,
        current_summary: dict,
    ) -> dict:
        """Compute deltas for all numeric summary metrics.

        Returns {metric_name: {baseline, current, delta}}.
        """
        numeric_keys = [
            "task_success_rate",
            "stall_rate",
            "mean_composite_score",
            "mean_outcome_score",
            "mean_judge_score",
            "escalation_correctness",
            "policy_compliance_rate",
            "mean_turns_to_resolution",
        ]
        deltas: dict = {}
        for key in numeric_keys:
            b_val = baseline_summary.get(key, 0.0)
            c_val = current_summary.get(key, 0.0)
            if isinstance(b_val, (int, float)) and isinstance(c_val, (int, float)):
                deltas[key] = {
                    "baseline": round(float(b_val), 4),
                    "current": round(float(c_val), 4),
                    "delta": round(float(c_val) - float(b_val), 4),
                }
        return deltas
