"""HTML report generator -- produces a self-contained evaluation dashboard.

Generates an HTML file with inline CSS (no external dependencies or JS
frameworks) that is viewable in any modern browser.  Includes:

  1. Header with run metadata
  2. Summary dashboard with the 7 key metrics from Phase 5 section 6.9
  3. Pass/fail overview with composite distribution bar
  4. Per-UC breakdown table
  5. Per-case expandable details (using ``<details>/<summary>``)
  6. Optional regression comparison table
"""

from __future__ import annotations

from datetime import datetime, timezone
from html import escape
from pathlib import Path


class HtmlReportGenerator:
    """Generates HTML evaluation report with dashboard and per-case drill-down."""

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
    ) -> str:
        """Generate a complete, self-contained HTML report string.

        Args:
            run_id: Unique identifier for the evaluation run.
            label: Human-readable label for the run.
            case_results: List of per-case result dicts.
            summary: Aggregated summary dict with the 7 key metrics.
            comparison: Optional comparison/regression dict.

        Returns:
            The full HTML document as a string.
        """
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

        parts: list[str] = [
            self._html_head(run_id, label),
            '<body>',
            self._header_section(run_id, label, timestamp, summary),
            self._metrics_dashboard(summary),
            self._pass_fail_overview(summary, case_results),
            self._per_uc_table(summary),
            self._per_case_details(case_results),
        ]

        if comparison:
            parts.append(self._regression_section(comparison))

        parts.append(self._html_footer())
        parts.append('</body></html>')

        return "\n".join(parts)

    def save(self, html: str, output_path: str | Path) -> Path:
        """Write HTML string to file.

        Creates parent directories if they do not exist.

        Args:
            html: The HTML string returned by :meth:`generate`.
            output_path: Filesystem path for the HTML file.

        Returns:
            Resolved ``Path`` to the written file.
        """
        path = Path(output_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(html)
        return path.resolve()

    # ------------------------------------------------------------------
    # Safe value helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _sf(value, default=0.0):
        """Safe float coercion."""
        if value is None:
            return default
        try:
            return float(value)
        except (TypeError, ValueError):
            return default

    @staticmethod
    def _si(value, default=0):
        """Safe int coercion."""
        if value is None:
            return default
        try:
            return int(value)
        except (TypeError, ValueError):
            return default

    def _fmt_pct(self, value) -> str:
        """Format a 0-1 float as a percentage string (1 decimal place)."""
        v = self._sf(value)
        return f"{v * 100:.1f}%"

    def _fmt_score(self, value) -> str:
        """Format a score to 2 decimal places."""
        v = self._sf(value)
        return f"{v:.2f}"

    # ------------------------------------------------------------------
    # HTML skeleton
    # ------------------------------------------------------------------

    @staticmethod
    def _html_head(run_id: str, label: str) -> str:
        title = escape(f"Eval Report - {label} ({run_id})")
        return f"""\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{title}</title>
<style>
:root {{
    --green: #22c55e;
    --green-bg: #f0fdf4;
    --amber: #f59e0b;
    --amber-bg: #fffbeb;
    --red: #ef4444;
    --red-bg: #fef2f2;
    --blue: #3b82f6;
    --blue-bg: #eff6ff;
    --gray-50: #f9fafb;
    --gray-100: #f3f4f6;
    --gray-200: #e5e7eb;
    --gray-300: #d1d5db;
    --gray-500: #6b7280;
    --gray-700: #374151;
    --gray-900: #111827;
}}

* {{ box-sizing: border-box; margin: 0; padding: 0; }}

body {{
    font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    color: var(--gray-900);
    background: var(--gray-50);
    line-height: 1.6;
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
}}

h1 {{ font-size: 1.75rem; margin-bottom: 0.25rem; }}
h2 {{ font-size: 1.35rem; margin: 2rem 0 1rem; border-bottom: 2px solid var(--gray-200); padding-bottom: 0.5rem; }}
h3 {{ font-size: 1.1rem; margin: 1rem 0 0.5rem; }}

.header {{
    background: white;
    padding: 1.5rem 2rem;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    margin-bottom: 2rem;
}}
.header-meta {{ color: var(--gray-500); font-size: 0.9rem; }}
.header-meta span {{ margin-right: 2rem; }}

/* --- Metric cards --- */
.cards {{
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 1rem;
    margin-bottom: 2rem;
}}
.card {{
    background: white;
    border-radius: 10px;
    padding: 1.25rem;
    box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    border-left: 4px solid var(--gray-300);
}}
.card.green  {{ border-left-color: var(--green);  background: var(--green-bg); }}
.card.amber  {{ border-left-color: var(--amber);  background: var(--amber-bg); }}
.card.red    {{ border-left-color: var(--red);    background: var(--red-bg); }}
.card-label  {{ font-size: 0.8rem; color: var(--gray-500); text-transform: uppercase; letter-spacing: 0.05em; }}
.card-value  {{ font-size: 1.6rem; font-weight: 700; margin: 0.25rem 0; }}
.card-target {{ font-size: 0.75rem; color: var(--gray-500); }}

/* --- Overview bar --- */
.overview-box {{
    background: white;
    border-radius: 10px;
    padding: 1.5rem;
    box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    margin-bottom: 2rem;
}}
.bar-container {{ display: flex; height: 28px; border-radius: 6px; overflow: hidden; margin: 0.75rem 0; }}
.bar-pass {{ background: var(--green); }}
.bar-fail {{ background: var(--red); }}
.overview-stats {{ display: flex; gap: 2rem; font-size: 0.95rem; }}
.overview-stats span {{ display: inline-flex; align-items: center; gap: 0.35rem; }}
.dot {{ width: 10px; height: 10px; border-radius: 50%; display: inline-block; }}
.dot.green {{ background: var(--green); }}
.dot.red   {{ background: var(--red); }}

/* --- Tables --- */
table {{ width: 100%; border-collapse: collapse; margin-bottom: 1rem; }}
th, td {{ padding: 0.6rem 0.75rem; text-align: left; border-bottom: 1px solid var(--gray-200); }}
th {{ background: var(--gray-100); font-weight: 600; font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.04em; }}
td {{ font-size: 0.9rem; }}
tr:hover td {{ background: var(--gray-50); }}

/* --- Per-case details --- */
details {{
    background: white;
    border-radius: 8px;
    box-shadow: 0 1px 2px rgba(0,0,0,0.06);
    margin-bottom: 0.75rem;
    border: 1px solid var(--gray-200);
}}
details[open] {{ border-color: var(--blue); }}
summary {{
    padding: 0.75rem 1rem;
    cursor: pointer;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 0.75rem;
    user-select: none;
}}
summary::-webkit-details-marker {{ display: none; }}
summary::before {{ content: "\\25B6"; font-size: 0.7rem; transition: transform 0.15s; }}
details[open] > summary::before {{ transform: rotate(90deg); }}
.case-body {{ padding: 0.5rem 1rem 1rem; }}

.badge {{
    display: inline-block;
    padding: 0.15rem 0.55rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
}}
.badge.pass {{ background: var(--green-bg); color: #166534; }}
.badge.fail {{ background: var(--red-bg); color: #991b1b; }}
.badge.stall {{ background: var(--amber-bg); color: #92400e; }}

.tag {{
    display: inline-block;
    padding: 0.1rem 0.45rem;
    border-radius: 4px;
    font-size: 0.75rem;
    background: var(--red-bg);
    color: #991b1b;
    margin-right: 0.3rem;
    margin-bottom: 0.2rem;
}}

.kv-grid {{
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.35rem 1.5rem;
    font-size: 0.88rem;
    margin-bottom: 0.75rem;
}}
.kv-grid dt {{ color: var(--gray-500); }}
.kv-grid dd {{ font-weight: 500; }}

.check-row {{ display: flex; align-items: center; gap: 0.5rem; font-size: 0.85rem; padding: 0.2rem 0; }}
.check-icon {{ font-weight: 700; width: 1.2em; text-align: center; }}
.check-icon.pass {{ color: var(--green); }}
.check-icon.fail {{ color: var(--red); }}

/* --- Regression section --- */
.regression-improved td {{ background: var(--green-bg); }}
.regression-regressed td {{ background: var(--red-bg); }}

.delta {{ font-weight: 600; }}
.delta.pos {{ color: #166534; }}
.delta.neg {{ color: #991b1b; }}

.footer {{
    text-align: center;
    color: var(--gray-500);
    font-size: 0.8rem;
    padding: 2rem 0 1rem;
}}
</style>
</head>"""

    # ------------------------------------------------------------------
    # Sections
    # ------------------------------------------------------------------

    def _header_section(self, run_id: str, label: str, timestamp: str, summary: dict) -> str:
        total = self._si(summary.get("total_cases"))
        return f"""\
<div class="header">
  <h1>Evaluation Report</h1>
  <div class="header-meta">
    <span><strong>Run ID:</strong> {escape(run_id)}</span>
    <span><strong>Label:</strong> {escape(label)}</span>
    <span><strong>Timestamp:</strong> {escape(timestamp)}</span>
    <span><strong>Total Cases:</strong> {total}</span>
  </div>
</div>"""

    def _metrics_dashboard(self, summary: dict) -> str:
        metrics = [
            ("Task Success Rate",              summary.get("task_success_rate"),              0.80, True),
            ("Stall Rate",                     summary.get("stall_rate"),                     0.00, False),
            ("Correct Tool Invocation Rate",   summary.get("correct_tool_invocation_rate"),   0.90, True),
            ("Escalation Correctness Rate",    summary.get("escalation_correctness_rate"),    0.95, True),
            ("Grounded Final Answer Rate",     summary.get("grounded_final_answer_rate"),     0.98, True),
            ("Policy Compliance Rate",         summary.get("policy_compliance_rate"),          1.00, True),
            ("Turns to Resolution",            summary.get("mean_turns_to_resolution"),       6.0,  None),
        ]

        cards_html: list[str] = []
        for name, value, target, higher_is_better in metrics:
            css_class = self._metric_color(value, target, higher_is_better)

            if higher_is_better is None:
                # "Turns to Resolution" -- lower is better, target is max
                display_value = self._fmt_score(value) if value is not None else "N/A"
                target_label = f"target: &le; {target:.0f}"
            elif name == "Stall Rate":
                display_value = self._fmt_pct(value) if value is not None else "N/A"
                target_label = "target: 0%"
            else:
                display_value = self._fmt_pct(value) if value is not None else "N/A"
                target_label = f"target: &ge; {target * 100:.0f}%"

            cards_html.append(f"""\
<div class="card {css_class}">
  <div class="card-label">{escape(name)}</div>
  <div class="card-value">{display_value}</div>
  <div class="card-target">{target_label}</div>
</div>""")

        return f"""\
<h2>Key Metrics Dashboard</h2>
<div class="cards">
{''.join(cards_html)}
</div>"""

    def _pass_fail_overview(self, summary: dict, case_results: list[dict]) -> str:
        total = max(self._si(summary.get("total_cases")), 1)
        passed = self._si(summary.get("passed_cases"))
        failed = self._si(summary.get("failed_cases"))
        mean_comp = self._fmt_score(summary.get("mean_composite_score"))

        pass_pct = passed / total * 100
        fail_pct = failed / total * 100

        # Distribution buckets
        buckets = {"0.0-0.2": 0, "0.2-0.4": 0, "0.4-0.6": 0, "0.6-0.8": 0, "0.8-1.0": 0}
        for cr in case_results:
            score = self._sf(cr.get("composite_score"))
            if score < 0.2:
                buckets["0.0-0.2"] += 1
            elif score < 0.4:
                buckets["0.2-0.4"] += 1
            elif score < 0.6:
                buckets["0.4-0.6"] += 1
            elif score < 0.8:
                buckets["0.6-0.8"] += 1
            else:
                buckets["0.8-1.0"] += 1

        dist_rows = "".join(
            f"<tr><td>{k}</td><td>{v}</td></tr>" for k, v in buckets.items()
        )

        return f"""\
<h2>Pass / Fail Overview</h2>
<div class="overview-box">
  <div class="overview-stats">
    <span><span class="dot green"></span> Passed: {passed}</span>
    <span><span class="dot red"></span> Failed: {failed}</span>
    <span>Mean Composite: {mean_comp}</span>
  </div>
  <div class="bar-container">
    <div class="bar-pass" style="width:{pass_pct:.1f}%"></div>
    <div class="bar-fail" style="width:{fail_pct:.1f}%"></div>
  </div>
  <h3>Composite Score Distribution</h3>
  <table>
    <thead><tr><th>Range</th><th>Count</th></tr></thead>
    <tbody>{dist_rows}</tbody>
  </table>
</div>"""

    def _per_uc_table(self, summary: dict) -> str:
        breakdown = summary.get("per_uc_breakdown")
        if not breakdown:
            return '<h2>Per Use-Case Breakdown</h2><p>No per-UC data available.</p>'

        rows: list[str] = []
        for uc, data in sorted(breakdown.items()):
            if isinstance(data, dict):
                count = self._si(data.get("count"))
                passed_uc = self._si(data.get("passed"))
                failed_uc = self._si(data.get("failed"))
                mean_c = self._fmt_score(data.get("mean_composite"))
                tsr = self._fmt_pct(data.get("task_success_rate"))
            else:
                count = passed_uc = failed_uc = 0
                mean_c = "N/A"
                tsr = "N/A"

            rows.append(
                f"<tr><td>{escape(str(uc))}</td><td>{count}</td>"
                f"<td>{passed_uc}</td><td>{failed_uc}</td>"
                f"<td>{mean_c}</td><td>{tsr}</td></tr>"
            )

        return f"""\
<h2>Per Use-Case Breakdown</h2>
<table>
  <thead>
    <tr><th>Use Case</th><th>Count</th><th>Passed</th><th>Failed</th><th>Mean Composite</th><th>Task Success Rate</th></tr>
  </thead>
  <tbody>
    {''.join(rows)}
  </tbody>
</table>"""

    def _per_case_details(self, case_results: list[dict]) -> str:
        if not case_results:
            return '<h2>Per-Case Details</h2><p>No case data available.</p>'

        items: list[str] = []
        for cr in case_results:
            items.append(self._render_case(cr))

        return f"""\
<h2>Per-Case Details</h2>
{''.join(items)}"""

    def _render_case(self, cr: dict) -> str:
        case_id = escape(str(cr.get("case_id", "unknown")))
        passed = bool(cr.get("case_passed", False))
        badge = '<span class="badge pass">PASS</span>' if passed else '<span class="badge fail">FAIL</span>'
        composite = self._fmt_score(cr.get("composite_score"))

        stall = bool(cr.get("stall_detected", False))
        stall_badge = ' <span class="badge stall">STALL</span>' if stall else ""

        # Key-value pairs
        kv = {
            "Expected UC": cr.get("primary_uc", "N/A"),
            "Actual UC": cr.get("active_use_case", "N/A"),
            "Expected Outcome": cr.get("expected_outcome", "N/A"),
            "Actual Outcome": cr.get("containment_outcome", "N/A"),
            "Composite Score": composite,
            "Outcome Score (L2)": self._fmt_score(cr.get("outcome_score")),
            "Judge Score (L3)": self._fmt_score(cr.get("judge_score")),
            "Stop Reason": cr.get("stop_reason", "N/A"),
            "Total Turns": str(cr.get("total_turns", "N/A")),
            "Session ID": cr.get("session_id", "N/A"),
        }
        kv_html = "".join(
            f"<dt>{escape(k)}</dt><dd>{escape(str(v))}</dd>" for k, v in kv.items()
        )

        # L1 hard checks
        l1_html = self._render_l1(cr.get("l1_results"))

        # L2 outcome checks
        l2_html = self._render_l2(cr.get("l2_results"))

        # L3 LLM judge
        l3_html = self._render_l3(cr.get("l3_results"))

        # Failure tags
        tags = cr.get("failure_tags") or []
        tags_html = "".join(f'<span class="tag">{escape(str(t))}</span>' for t in tags) if tags else '<span style="color:var(--gray-500);font-size:0.85rem;">None</span>'

        return f"""\
<details>
  <summary>{case_id} {badge}{stall_badge} &mdash; composite {composite}</summary>
  <div class="case-body">
    <dl class="kv-grid">{kv_html}</dl>

    <h3>L1 Hard Checks</h3>
    {l1_html}

    <h3>L2 Outcome Scores</h3>
    {l2_html}

    <h3>L3 LLM Judge Scores</h3>
    {l3_html}

    <h3>Failure Tags</h3>
    <div>{tags_html}</div>
  </div>
</details>"""

    def _render_l1(self, results) -> str:
        if not results:
            return '<p style="color:var(--gray-500);font-size:0.85rem;">No L1 checks.</p>'
        lines: list[str] = []
        for r in results:
            if isinstance(r, dict):
                name = r.get("check", "")
                passed = r.get("passed", False)
                detail = r.get("detail", "")
            else:
                name = getattr(r, "check_name", "")
                passed = getattr(r, "passed", False)
                detail = getattr(r, "detail", "")

            icon_cls = "pass" if passed else "fail"
            icon_char = "&#10003;" if passed else "&#10007;"
            detail_str = f' <span style="color:var(--gray-500);">({escape(str(detail))})</span>' if detail else ""
            lines.append(
                f'<div class="check-row"><span class="check-icon {icon_cls}">{icon_char}</span>'
                f'{escape(str(name))}{detail_str}</div>'
            )
        return "".join(lines)

    def _render_l2(self, results) -> str:
        if not results:
            return '<p style="color:var(--gray-500);font-size:0.85rem;">No L2 checks.</p>'
        rows: list[str] = []
        for r in results:
            if isinstance(r, dict):
                name = r.get("check", "")
                score = self._sf(r.get("score"))
                detail = r.get("detail", "")
            else:
                name = getattr(r, "check_name", "")
                score = self._sf(getattr(r, "score", 0.0))
                detail = getattr(r, "detail", "")

            color = "var(--green)" if score >= 0.8 else ("var(--amber)" if score >= 0.5 else "var(--red)")
            detail_str = escape(str(detail)) if detail else ""
            rows.append(
                f"<tr><td>{escape(str(name))}</td>"
                f'<td style="color:{color};font-weight:600;">{score:.2f}</td>'
                f"<td style=\"font-size:0.82rem;color:var(--gray-500);\">{detail_str}</td></tr>"
            )
        return f"""\
<table>
  <thead><tr><th>Check</th><th>Score</th><th>Detail</th></tr></thead>
  <tbody>{''.join(rows)}</tbody>
</table>"""

    def _render_l3(self, results) -> str:
        if not results:
            return '<p style="color:var(--gray-500);font-size:0.85rem;">No L3 judge scores.</p>'
        rows: list[str] = []
        for r in results:
            if isinstance(r, dict):
                dim = r.get("dimension", "")
                score = self._sf(r.get("score"))
                reasoning = r.get("reasoning", "")
            else:
                dim = getattr(r, "dimension", "")
                score = self._sf(getattr(r, "score", 0.0))
                reasoning = getattr(r, "reasoning", "")

            color = "var(--green)" if score >= 4.0 else ("var(--amber)" if score >= 3.0 else "var(--red)")
            reasoning_str = escape(str(reasoning)) if reasoning else ""
            rows.append(
                f"<tr><td>{escape(str(dim))}</td>"
                f'<td style="color:{color};font-weight:600;">{score:.1f}/5</td>'
                f"<td style=\"font-size:0.82rem;color:var(--gray-500);\">{reasoning_str}</td></tr>"
            )
        return f"""\
<table>
  <thead><tr><th>Dimension</th><th>Score</th><th>Reasoning</th></tr></thead>
  <tbody>{''.join(rows)}</tbody>
</table>"""

    def _regression_section(self, comparison: dict) -> str:
        """Render the optional regression comparison section."""
        baseline_label = escape(str(comparison.get("baseline_label", "baseline")))
        current_label = escape(str(comparison.get("current_label", "current")))
        overall_baseline = self._sf(comparison.get("overall_baseline_score"))
        overall_current = self._sf(comparison.get("overall_current_score"))
        overall_delta = self._sf(comparison.get("overall_delta"))

        delta_cls = "pos" if overall_delta >= 0 else "neg"
        delta_sign = "+" if overall_delta >= 0 else ""

        case_diffs = comparison.get("case_diffs", [])
        rows: list[str] = []
        for cd in case_diffs:
            if isinstance(cd, dict):
                cid = cd.get("case_id", "")
                b_score = self._sf(cd.get("baseline_score"))
                c_score = self._sf(cd.get("current_score"))
                delta = self._sf(cd.get("delta"))
                regressions = cd.get("regressions", [])
                improvements = cd.get("improvements", [])
            else:
                cid = getattr(cd, "case_id", "")
                b_score = self._sf(getattr(cd, "baseline_score", 0.0))
                c_score = self._sf(getattr(cd, "current_score", 0.0))
                delta = self._sf(getattr(cd, "delta", 0.0))
                regressions = getattr(cd, "regressions", [])
                improvements = getattr(cd, "improvements", [])

            if delta > 0.001:
                row_cls = "regression-improved"
            elif delta < -0.001:
                row_cls = "regression-regressed"
            else:
                row_cls = ""

            d_cls = "pos" if delta >= 0 else "neg"
            d_sign = "+" if delta >= 0 else ""

            details_items: list[str] = []
            for imp in improvements:
                details_items.append(f'<span style="color:var(--green);">+ {escape(str(imp))}</span>')
            for reg in regressions:
                details_items.append(f'<span style="color:var(--red);">- {escape(str(reg))}</span>')
            details_str = "<br>".join(details_items) if details_items else ""

            rows.append(
                f'<tr class="{row_cls}">'
                f"<td>{escape(str(cid))}</td>"
                f"<td>{b_score:.2f}</td>"
                f"<td>{c_score:.2f}</td>"
                f'<td class="delta {d_cls}">{d_sign}{delta:.2f}</td>'
                f"<td>{details_str}</td></tr>"
            )

        # Metric deltas (if provided)
        metric_deltas = comparison.get("metric_deltas", {})
        metric_rows = ""
        if metric_deltas:
            m_items: list[str] = []
            for mname, mval in sorted(metric_deltas.items()):
                mv = self._sf(mval)
                mc = "pos" if mv >= 0 else "neg"
                ms = "+" if mv >= 0 else ""
                m_items.append(
                    f'<tr><td>{escape(str(mname))}</td>'
                    f'<td class="delta {mc}">{ms}{mv:.4f}</td></tr>'
                )
            metric_rows = f"""\
<h3>Metric Deltas</h3>
<table>
  <thead><tr><th>Metric</th><th>Delta</th></tr></thead>
  <tbody>{''.join(m_items)}</tbody>
</table>"""

        return f"""\
<h2>Regression Comparison</h2>
<div class="overview-box">
  <div class="overview-stats">
    <span>Baseline: <strong>{baseline_label}</strong> ({overall_baseline:.2f})</span>
    <span>Current: <strong>{current_label}</strong> ({overall_current:.2f})</span>
    <span>Overall Delta: <span class="delta {delta_cls}">{delta_sign}{overall_delta:.2f}</span></span>
  </div>
</div>

<table>
  <thead>
    <tr><th>Case ID</th><th>Baseline</th><th>Current</th><th>Delta</th><th>Details</th></tr>
  </thead>
  <tbody>
    {''.join(rows)}
  </tbody>
</table>

{metric_rows}"""

    # ------------------------------------------------------------------
    # Color helpers
    # ------------------------------------------------------------------

    def _metric_color(self, value, target: float, higher_is_better: bool | None) -> str:
        """Return CSS class (green / amber / red) for a metric card."""
        if value is None:
            return ""
        v = self._sf(value)

        if higher_is_better is None:
            # "Turns to Resolution": lower is better, target is ceiling
            if v <= target:
                return "green"
            elif v <= target * 1.25:
                return "amber"
            else:
                return "red"

        if higher_is_better is False:
            # Stall Rate: lower is better, target is 0
            if v <= target:
                return "green"
            elif v <= target + 0.05:
                return "amber"
            else:
                return "red"

        # Higher is better
        if v >= target:
            return "green"
        elif v >= target * 0.9:
            return "amber"
        else:
            return "red"

    @staticmethod
    def _html_footer() -> str:
        return """\
<div class="footer">
  Generated by Interactive Eval Harness
</div>"""
