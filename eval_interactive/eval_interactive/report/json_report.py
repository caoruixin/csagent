"""JSON report generator -- produces machine-readable evaluation results.

Generates a structured JSON report containing run metadata, the 7 key
metrics from Phase 5 section 6.9, per-case drill-down data, and optional
regression comparison results.
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path


class JsonReportGenerator:
    """Generates machine-readable JSON evaluation report."""

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def generate(
        self,
        run_id: str,
        label: str,
        case_results: list[dict],
        summary: dict,
        comparison: dict | None = None,
    ) -> dict:
        """Build the full report structure.

        Args:
            run_id: Unique identifier for the evaluation run (e.g. ``"20260424_143000"``).
            label: Human-readable label (e.g. ``"baseline_v1.0.3"``).
            case_results: List of per-case result dicts.  Each dict is expected
                to contain keys such as ``case_id``, ``composite_score``,
                ``case_passed``, ``l1_results``, ``l2_results``, ``l3_results``,
                ``failure_tags``, ``stall_detected``, etc.
            summary: Aggregated summary dict with the 7 key metrics and any
                additional statistics.
            comparison: Optional comparison/regression dict (from diff engine).
                Pass ``None`` to omit the regression section.

        Returns:
            A fully-formed report dict ready for JSON serialisation.
        """
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")

        report: dict = {
            "run_id": run_id,
            "label": label,
            "timestamp": timestamp,
            "summary": self._build_summary(summary),
            "cases": [self._build_case(cr) for cr in case_results],
            "comparison": comparison,
        }
        return report

    def save(self, report: dict, output_path: str | Path) -> Path:
        """Write *report* dict to a JSON file.

        Creates parent directories if they do not exist.

        Args:
            report: The report dict returned by :meth:`generate`.
            output_path: Filesystem path for the JSON file.

        Returns:
            Resolved ``Path`` to the written file.
        """
        path = Path(output_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as fh:
            json.dump(report, fh, indent=2, ensure_ascii=False, default=str)
        return path.resolve()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _safe_float(value, default=None):
        """Coerce *value* to float, returning *default* on failure."""
        if value is None:
            return default
        try:
            return float(value)
        except (TypeError, ValueError):
            return default

    def _build_summary(self, summary: dict) -> dict:
        """Normalise and fill defaults for the summary block."""
        return {
            "total_cases": summary.get("total_cases", 0),
            "passed_cases": summary.get("passed_cases", 0),
            "failed_cases": summary.get("failed_cases", 0),
            "task_success_rate": self._safe_float(summary.get("task_success_rate"), 0.0),
            "stall_rate": self._safe_float(summary.get("stall_rate"), 0.0),
            "correct_tool_invocation_rate": self._safe_float(
                summary.get("correct_tool_invocation_rate"), 0.0
            ),
            "escalation_correctness_rate": self._safe_float(
                summary.get("escalation_correctness_rate"), 0.0
            ),
            "grounded_final_answer_rate": self._safe_float(
                summary.get("grounded_final_answer_rate"), 0.0
            ),
            "policy_compliance_rate": self._safe_float(
                summary.get("policy_compliance_rate"), 0.0
            ),
            "mean_turns_to_resolution": self._safe_float(
                summary.get("mean_turns_to_resolution"), 0.0
            ),
            "mean_composite_score": self._safe_float(
                summary.get("mean_composite_score"), 0.0
            ),
            "per_uc_breakdown": summary.get("per_uc_breakdown", {}),
        }

    def _build_case(self, cr: dict) -> dict:
        """Normalise a single per-case result dict."""
        return {
            "case_id": cr.get("case_id", "unknown"),
            "source_session_id": cr.get("source_session_id", ""),
            "expected_uc": cr.get("expected_uc", ""),
            "actual_uc": cr.get("actual_uc", ""),
            "expected_outcome": cr.get("expected_outcome", ""),
            "actual_outcome": cr.get("actual_outcome", ""),
            "case_passed": bool(cr.get("case_passed", False)),
            "composite_score": self._safe_float(cr.get("composite_score"), 0.0),
            "outcome_score": self._safe_float(cr.get("outcome_score"), 0.0),
            "judge_score": self._safe_float(cr.get("judge_score"), 0.0),
            "stop_reason": cr.get("stop_reason", ""),
            "total_turns": cr.get("total_turns", 0),
            "l1_results": self._serialise_l1(cr.get("l1_results")),
            "l2_results": self._serialise_l2(cr.get("l2_results")),
            "l3_results": self._serialise_l3(cr.get("l3_results")),
            "failure_tags": list(cr.get("failure_tags") or []),
            "stall_detected": bool(cr.get("stall_detected", False)),
        }

    # -- Layer serialisation helpers --

    @staticmethod
    def _serialise_l1(results) -> list[dict]:
        """Convert L1 HardCheckResult objects (or dicts) to plain dicts."""
        if not results:
            return []
        out: list[dict] = []
        for r in results:
            if isinstance(r, dict):
                out.append(r)
            else:
                # dataclass-like object
                out.append({
                    "check_name": getattr(r, "check_name", ""),
                    "passed": bool(getattr(r, "passed", False)),
                    "detail": getattr(r, "detail", ""),
                })
        return out

    @staticmethod
    def _serialise_l2(results) -> list[dict]:
        """Convert L2 OutcomeCheckResult objects (or dicts) to plain dicts."""
        if not results:
            return []
        out: list[dict] = []
        for r in results:
            if isinstance(r, dict):
                out.append(r)
            else:
                out.append({
                    "check_name": getattr(r, "check_name", ""),
                    "score": float(getattr(r, "score", 0.0)),
                    "detail": getattr(r, "detail", ""),
                })
        return out

    @staticmethod
    def _serialise_l3(results) -> list[dict]:
        """Convert L3 JudgeResult objects (or dicts) to plain dicts."""
        if not results:
            return []
        out: list[dict] = []
        for r in results:
            if isinstance(r, dict):
                out.append(r)
            else:
                out.append({
                    "dimension": getattr(r, "dimension", ""),
                    "score": float(getattr(r, "score", 0.0)),
                    "reasoning": getattr(r, "reasoning", ""),
                })
        return out
