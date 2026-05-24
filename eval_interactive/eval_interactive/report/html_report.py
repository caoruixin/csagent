"""HTML report generator -- M3-Eval four-tier verdict surface.

Generates a self-contained HTML file with inline CSS (no external
dependencies or JS frameworks) viewable in any modern browser. The
report layout follows the M3-Eval four-tier evaluation pyramid and the
M4-Eval-Cleanup ``case_passed_authority`` annotation
(``iteration_governance.md`` §5.5, §5.6, ``M3-Eval_objective.md``):

  1. Header (run metadata + ``suite_authority`` badge + the "programmatic
     counts are informational" note for human-judgment suites)
  2. Tier-0 — Safety Floor (the L1 hard checks, labelled as Tier-0)
  3. Tier-1 — Outcome (PRIMARY for human-judgment suites; surfaces the
     informational programmatic ``case_passed`` count + a pointer to
     ``eval_interactive/case_specs/bad_cases/_manifest.md`` for the
     human-review verdict)
  4. Tier-2 — Critical-flow (per-case ``tier2_result`` aggregate +
     ``_render_tier2`` per-case rendering of passed / severity /
     ``failed_step_ids``)
  5. Tier-3 — Polish (advisory L2/L3 dims; clearly marked "never flips
     case_passed")
  6. Programmatic-suite per-UC rollup (kept from the legacy view but
     reframed under the four-tier surface)
  7. Per-case details (tier-labelled badges; one ``<details>`` block
     per case)
  8. Optional regression comparison table

Per Sprint 50 / M5 S1 Alternative B (human-confirmed 2026-05-24), the
pre-M3 Phase-5 §6.9 7-metric dashboard rendering is removed. The
underlying aggregates remain computed by
:func:`batch.executor._compute_summary` and present in ``results.json``
for backward-compat; only the HTML rendering is stripped.
"""

from __future__ import annotations

from datetime import datetime, timezone
from html import escape
from pathlib import Path


# Per-case ``case_passed_authority`` values resolved by
# :func:`batch.executor._resolve_case_passed_authority` from
# ``CaseSpec.source_suite`` via :func:`batch.sets.is_human_judgment_suite`.
_AUTHORITY_HUMAN_REVIEW = "human_review"
_AUTHORITY_PROGRAMMATIC = "programmatic"

# Manifest path surfaced as the canonical human-judgment verdict
# source for human-judgment suites (per ``iteration_governance.md``
# §5.6).
_BAD_CASE_MANIFEST_POINTER = (
    "eval_interactive/case_specs/bad_cases/_manifest.md"
)


class HtmlReportGenerator:
    """Generates HTML evaluation report with the M3-Eval four-tier verdict surface."""

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
            summary: Aggregated summary dict, including ``suite_authority``.
            comparison: Optional comparison/regression dict.

        Returns:
            The full HTML document as a string.
        """
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        suite_authority = self._resolve_suite_authority(summary, case_results)

        parts: list[str] = [
            self._html_head(run_id, label),
            '<body>',
            self._header_section(run_id, label, timestamp, summary, suite_authority),
            self._tier0_section(case_results),
            self._tier1_section(summary, case_results, suite_authority),
            self._tier2_section(case_results),
            self._tier3_section(summary, case_results),
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
    # Suite-authority + tier resolution
    # ------------------------------------------------------------------

    @staticmethod
    def _resolve_suite_authority(summary: dict, case_results: list[dict]) -> str:
        """Resolve the run-level ``suite_authority`` for header rendering.

        Prefers the aggregate field computed once by
        :func:`batch.executor._compute_summary`. Falls back to a
        per-case derivation when an older ``summary`` dict (e.g., one
        constructed by a unit test or a pre-Sprint-50 result file) does
        not carry the field; this fallback keeps the renderer usable
        when re-rendering historical ``results.json`` files.
        """
        explicit = summary.get("suite_authority")
        if explicit in (_AUTHORITY_HUMAN_REVIEW, _AUTHORITY_PROGRAMMATIC, "mixed"):
            return explicit
        if not case_results:
            return _AUTHORITY_PROGRAMMATIC
        authorities = {
            (cr.get("case_passed_authority") or _AUTHORITY_PROGRAMMATIC)
            for cr in case_results
        }
        if authorities == {_AUTHORITY_HUMAN_REVIEW}:
            return _AUTHORITY_HUMAN_REVIEW
        if authorities == {_AUTHORITY_PROGRAMMATIC}:
            return _AUTHORITY_PROGRAMMATIC
        return "mixed"

    @staticmethod
    def _tier0_outcome(cr: dict) -> tuple[str, int, int]:
        """Aggregate Tier-0 (L1 hard checks) outcome for one case.

        Returns ``(label, passed_count, total_count)``. ``label`` is
        ``"PASS"`` when every L1 check passed, ``"FAIL"`` otherwise,
        and ``"N/A"`` when no L1 checks ran (e.g., a contract-violation
        case).
        """
        results = cr.get("l1_results") or []
        if not results:
            return ("N/A", 0, 0)
        total = 0
        passed = 0
        for r in results:
            if isinstance(r, dict):
                ok = bool(r.get("passed", False))
            else:
                ok = bool(getattr(r, "passed", False))
            total += 1
            if ok:
                passed += 1
        return (("PASS" if passed == total else "FAIL"), passed, total)

    @staticmethod
    def _tier1_label(cr: dict) -> str:
        """Per-case Tier-1 label.

        For ``case_passed_authority="human_review"`` cases the
        programmatic ``case_passed`` is informational only — render the
        ``HUMAN_REVIEW`` label. For programmatic cases mirror the
        ``case_passed`` boolean as PASS / FAIL.
        """
        authority = cr.get("case_passed_authority") or _AUTHORITY_PROGRAMMATIC
        if authority == _AUTHORITY_HUMAN_REVIEW:
            return "HUMAN_REVIEW"
        return "PASS" if cr.get("case_passed") else "FAIL"

    @staticmethod
    def _tier2_summary(cr: dict) -> tuple[str, dict]:
        """Per-case Tier-2 summary label + structured tier2_result.

        Returns ``(label, tier2_result_dict)``. ``label`` is one of
        ``"PASS"`` / ``"FAIL"`` / ``"N/A"``. The dict is the raw
        ``tier2_result`` payload (empty dict when absent) for the
        per-case ``_render_tier2`` block.
        """
        t2 = cr.get("tier2_result")
        if not isinstance(t2, dict):
            return ("N/A", {})
        if t2.get("passed") is True:
            return ("PASS", t2)
        if t2.get("passed") is False:
            return ("FAIL", t2)
        return ("N/A", t2)

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
.suite-authority-badge {{
    display: inline-block;
    padding: 0.3rem 0.8rem;
    border-radius: 6px;
    font-weight: 600;
    font-size: 0.85rem;
    margin-top: 0.5rem;
}}
.suite-authority-badge.human-review {{ background: var(--amber-bg); color: #92400e; border: 1px solid var(--amber); }}
.suite-authority-badge.programmatic {{ background: var(--blue-bg); color: #1e40af; border: 1px solid var(--blue); }}
.suite-authority-badge.mixed {{ background: var(--gray-100); color: var(--gray-700); border: 1px solid var(--gray-300); }}
.authority-note {{ font-size: 0.85rem; color: var(--gray-500); margin-top: 0.4rem; }}

/* --- Tier sections --- */
.tier-box {{
    background: white;
    border-radius: 10px;
    padding: 1.25rem 1.5rem;
    box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    margin-bottom: 1.5rem;
    border-left: 4px solid var(--gray-300);
}}
.tier-box.tier0 {{ border-left-color: var(--red); }}
.tier-box.tier1 {{ border-left-color: var(--amber); }}
.tier-box.tier2 {{ border-left-color: var(--blue); }}
.tier-box.tier3 {{ border-left-color: var(--gray-500); }}
.tier-label {{ font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--gray-500); }}
.tier-headline {{ font-size: 1.1rem; font-weight: 600; margin: 0.2rem 0 0.4rem; }}
.tier-note {{ font-size: 0.85rem; color: var(--gray-500); margin-top: 0.3rem; }}
.tier-stat-row {{ display: flex; flex-wrap: wrap; gap: 1.5rem; margin: 0.6rem 0 0.4rem; font-size: 0.9rem; }}
.tier-stat-row strong {{ font-weight: 600; }}

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
    flex-wrap: wrap;
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
.badge.human-review {{ background: var(--amber-bg); color: #92400e; }}
.badge.na {{ background: var(--gray-100); color: var(--gray-700); }}
.badge.stall {{ background: var(--amber-bg); color: #92400e; }}
.badge.tier {{ font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; padding: 0.15rem 0.45rem; }}

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
.check-icon.na   {{ color: var(--gray-500); }}

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

    def _header_section(
        self,
        run_id: str,
        label: str,
        timestamp: str,
        summary: dict,
        suite_authority: str,
    ) -> str:
        total = self._si(summary.get("total_cases"))
        badge_cls = {
            _AUTHORITY_HUMAN_REVIEW: "human-review",
            _AUTHORITY_PROGRAMMATIC: "programmatic",
            "mixed": "mixed",
        }.get(suite_authority, "programmatic")
        badge_text = {
            _AUTHORITY_HUMAN_REVIEW: "HUMAN-JUDGMENT suite (per §5.6) — manual review required",
            _AUTHORITY_PROGRAMMATIC: "PROGRAMMATIC suite",
            "mixed": "MIXED suite (human-judgment + programmatic cases)",
        }.get(suite_authority, "PROGRAMMATIC suite")

        if suite_authority == _AUTHORITY_PROGRAMMATIC:
            note = (
                "Programmatic case_passed gating applies; per-case Tier-1 reflects "
                "<code>case_passed</code> from the composite scoring kernel."
            )
        else:
            note = (
                "Programmatic <code>case_passed</code> counts below are "
                "<strong>informational</strong>, not the gate — see "
                f"<code>{escape(_BAD_CASE_MANIFEST_POINTER)}</code> for the "
                "deliver-agent + human verdict from the milestone close."
            )

        return f"""\
<div class="header">
  <h1>Evaluation Report</h1>
  <div class="header-meta">
    <span><strong>Run ID:</strong> {escape(run_id)}</span>
    <span><strong>Label:</strong> {escape(label)}</span>
    <span><strong>Timestamp:</strong> {escape(timestamp)}</span>
    <span><strong>Total Cases:</strong> {total}</span>
  </div>
  <div class="suite-authority-badge {badge_cls}">Suite authority: {escape(suite_authority)} &mdash; {badge_text}</div>
  <div class="authority-note">{note}</div>
</div>"""

    # -- Tier-0 (L1 Safety Floor) --
    def _tier0_section(self, case_results: list[dict]) -> str:
        if not case_results:
            return """\
<h2>Tier-0 Safety Floor</h2>
<div class="tier-box tier0">
  <div class="tier-label">Tier-0 &mdash; Safety floor</div>
  <div class="tier-headline">No case data available.</div>
</div>"""

        # Aggregate per-check pass counts across cases.
        check_totals: dict[str, dict[str, int]] = {}
        cases_with_any_fail = 0
        for cr in case_results:
            results = cr.get("l1_results") or []
            any_fail = False
            for r in results:
                if isinstance(r, dict):
                    name = str(r.get("check") or r.get("check_name") or "")
                    ok = bool(r.get("passed", False))
                else:
                    name = str(getattr(r, "check_name", "") or getattr(r, "check", ""))
                    ok = bool(getattr(r, "passed", False))
                if not name:
                    continue
                bucket = check_totals.setdefault(name, {"passed": 0, "total": 0})
                bucket["total"] += 1
                if ok:
                    bucket["passed"] += 1
                else:
                    any_fail = True
            if any_fail:
                cases_with_any_fail += 1

        total = len(case_results)
        clean_cases = total - cases_with_any_fail
        headline_pass = cases_with_any_fail == 0

        rows = "".join(
            f"<tr>"
            f"<td><code>{escape(name)}</code></td>"
            f"<td>{b['passed']}/{b['total']}</td>"
            f"</tr>"
            for name, b in sorted(check_totals.items())
        )
        rows_html = (
            f"<table><thead><tr><th>L1 Check</th><th>Passed / Total</th></tr></thead>"
            f"<tbody>{rows}</tbody></table>"
            if rows
            else "<p class=\"tier-note\">No L1 hard-check results recorded.</p>"
        )

        return f"""\
<h2>Tier-0 Safety Floor</h2>
<div class="tier-box tier0">
  <div class="tier-label">Tier-0 &mdash; Safety floor (L1 hard checks)</div>
  <div class="tier-headline">{'All cases pass Tier-0.' if headline_pass else f'{clean_cases}/{total} cases pass Tier-0.'}</div>
  <div class="tier-note">PII / safety / identity / forbidden-phrase / escalation-compliance / phase-transition-validity. Sourced from <code>runtime_freeze_and_risk_policy.md</code> §1/§2 (M3-Eval).</div>
  <div class="tier-stat-row"><span><strong>Cases clean:</strong> {clean_cases}/{total}</span><span><strong>Cases with at least one L1 fail:</strong> {cases_with_any_fail}/{total}</span></div>
  {rows_html}
</div>"""

    # -- Tier-1 (Outcome / human-judgment vs programmatic) --
    def _tier1_section(
        self,
        summary: dict,
        case_results: list[dict],
        suite_authority: str,
    ) -> str:
        total = len(case_results)
        passed_programmatic = sum(
            1 for cr in case_results if cr.get("case_passed")
        )
        human_review_n = sum(
            1
            for cr in case_results
            if cr.get("case_passed_authority") == _AUTHORITY_HUMAN_REVIEW
        )
        programmatic_n = sum(
            1
            for cr in case_results
            if cr.get("case_passed_authority") == _AUTHORITY_PROGRAMMATIC
        )
        legacy_n = total - human_review_n - programmatic_n

        if suite_authority == _AUTHORITY_HUMAN_REVIEW:
            headline = (
                "Human-judgment suite &mdash; human review of "
                "<code>closure_criterion</code> against <code>per_turn_trace</code> "
                "is the primary gate."
            )
            primary_line = (
                f"<strong>Programmatic case_passed (informational):</strong> "
                f"{passed_programmatic}/{total}"
            )
            pointer_line = (
                f"&rarr; Verdict source: <code>{escape(_BAD_CASE_MANIFEST_POINTER)}</code> "
                f"(deliver-agent + human verdicts from milestone close)."
            )
        elif suite_authority == _AUTHORITY_PROGRAMMATIC:
            headline = (
                "Programmatic suite &mdash; <code>case_passed</code> is the gate."
            )
            primary_line = (
                f"<strong>case_passed:</strong> "
                f"{passed_programmatic}/{total}"
            )
            pointer_line = (
                "Programmatic gate: L1 (Tier-0 floor) &and; mandatory L2 "
                "&and; Tier-2 not critical-failed."
            )
        else:
            headline = (
                "Mixed suite &mdash; human-judgment + programmatic cases. "
                "Verdict varies per case authority (see badges below)."
            )
            primary_line = (
                f"<strong>case_passed (mixed):</strong> "
                f"{passed_programmatic}/{total} "
                f"(<em>informational</em> for human-judgment cases; "
                f"<em>gating</em> for programmatic cases)"
            )
            pointer_line = (
                f"&rarr; For human-judgment cases see "
                f"<code>{escape(_BAD_CASE_MANIFEST_POINTER)}</code>."
            )

        authority_split = (
            f"<div class=\"tier-stat-row\">"
            f"<span><strong>human_review:</strong> {human_review_n}/{total}</span>"
            f"<span><strong>programmatic:</strong> {programmatic_n}/{total}</span>"
            + (
                f"<span><strong>unannotated (legacy):</strong> {legacy_n}/{total}</span>"
                if legacy_n
                else ""
            )
            + "</div>"
        )

        return f"""\
<h2>Tier-1 Outcome</h2>
<div class="tier-box tier1">
  <div class="tier-label">Tier-1 &mdash; Outcome (PRIMARY for human-judgment suites)</div>
  <div class="tier-headline">{headline}</div>
  <div class="tier-stat-row"><span>{primary_line}</span></div>
  {authority_split}
  <div class="tier-note">{pointer_line}</div>
</div>"""

    # -- Tier-2 (Critical-flow / skill_procedure_followship) --
    def _tier2_section(self, case_results: list[dict]) -> str:
        total = len(case_results)
        critical_fails: list[tuple[str, list[str]]] = []
        advisory_fails: list[tuple[str, list[str]]] = []
        pass_cnt = 0
        na_cnt = 0

        for cr in case_results:
            label, t2 = self._tier2_summary(cr)
            cid = str(cr.get("case_id", "unknown"))
            if label == "PASS":
                pass_cnt += 1
            elif label == "FAIL":
                severity = str(t2.get("severity") or "").lower()
                failed_ids = [str(s) for s in (t2.get("failed_step_ids") or [])]
                if severity == "critical":
                    critical_fails.append((cid, failed_ids))
                else:
                    advisory_fails.append((cid, failed_ids))
            else:
                na_cnt += 1

        def _fmt_fail_rows(items: list[tuple[str, list[str]]]) -> str:
            if not items:
                return "<p class=\"tier-note\">None.</p>"
            rows = "".join(
                f"<tr><td><code>{escape(cid)}</code></td>"
                f"<td>{escape(', '.join(steps)) if steps else '&mdash;'}</td></tr>"
                for cid, steps in items
            )
            return (
                "<table><thead><tr><th>Case</th><th>failed_step_ids</th></tr></thead>"
                f"<tbody>{rows}</tbody></table>"
            )

        return f"""\
<h2>Tier-2 Critical-flow</h2>
<div class="tier-box tier2">
  <div class="tier-label">Tier-2 &mdash; Critical-flow (<code>skill_procedure_followship</code>; M3-Eval)</div>
  <div class="tier-headline">{pass_cnt}/{total} cases pass Tier-2.</div>
  <div class="tier-stat-row">
    <span><strong>PASS:</strong> {pass_cnt}</span>
    <span><strong>FAIL (critical):</strong> {len(critical_fails)}</span>
    <span><strong>FAIL (advisory):</strong> {len(advisory_fails)}</span>
    <span><strong>N/A:</strong> {na_cnt}</span>
  </div>
  <h3>Critical Tier-2 failures</h3>
  {_fmt_fail_rows(critical_fails)}
  <h3>Advisory Tier-2 failures (informational)</h3>
  {_fmt_fail_rows(advisory_fails)}
  <div class="tier-note">Per-case <code>failed_step_ids</code> + step detail in the per-case drawer below. Critical Tier-2 failures contribute to <code>case_passed=False</code> on programmatic suites; advisory Tier-2 failures are informational.</div>
</div>"""

    # -- Tier-3 (Polish / advisory L2 + L3 dims) --
    def _tier3_section(self, summary: dict, case_results: list[dict]) -> str:
        mean_outcome = self._fmt_score(summary.get("mean_outcome_score"))
        mean_judge = self._fmt_score(summary.get("mean_judge_score"))
        stall_rate = self._fmt_pct(summary.get("stall_rate"))

        # Aggregate L3 dimensions, if present.
        dim_totals: dict[str, list[float]] = {}
        for cr in case_results:
            for r in (cr.get("l3_results") or []):
                if isinstance(r, dict):
                    dim = str(r.get("dimension", ""))
                    score = self._sf(r.get("score"))
                else:
                    dim = str(getattr(r, "dimension", ""))
                    score = self._sf(getattr(r, "score", 0.0))
                if dim:
                    dim_totals.setdefault(dim, []).append(score)

        if dim_totals:
            rows = "".join(
                f"<tr><td><code>{escape(name)}</code></td>"
                f"<td>{sum(scores) / len(scores):.2f}</td>"
                f"<td>{len(scores)}</td></tr>"
                for name, scores in sorted(dim_totals.items())
            )
            l3_table = (
                "<table><thead><tr><th>L3 dimension</th><th>Mean</th><th>N</th></tr></thead>"
                f"<tbody>{rows}</tbody></table>"
            )
        else:
            l3_table = "<p class=\"tier-note\">No L3 advisory dimensions configured.</p>"

        return f"""\
<h2>Tier-3 Polish (advisory)</h2>
<div class="tier-box tier3">
  <div class="tier-label">Tier-3 &mdash; Polish (advisory; <strong>never flips <code>case_passed</code></strong>)</div>
  <div class="tier-headline">Advisory dimensions &mdash; trend signal only.</div>
  <div class="tier-stat-row">
    <span><strong>Mean outcome (L2):</strong> {mean_outcome}</span>
    <span><strong>Mean judge (L3):</strong> {mean_judge}</span>
    <span><strong>Stall rate:</strong> {stall_rate}</span>
  </div>
  <h3>L3 advisory dimensions</h3>
  {l3_table}
  <div class="tier-note">Includes legacy L3 dimensions (<code>relevance</code>, <code>tone_appropriateness</code>, <code>groundedness</code>; demoted S-Eval-5) and the L2 advisory dimensions demoted in S-Cleanup-3 (<code>handover_completeness</code>, <code>case_id_present</code>). The L3 <code>user_goal_achievement</code> dimension is a Tier-1 supplementary advisory per M3-Eval. None of these flip <code>case_passed</code>.</div>
</div>"""

    def _per_uc_table(self, summary: dict) -> str:
        """Programmatic per-UC rollup -- kept from the legacy view and
        reframed under the four-tier surface (sprint 50 / M5 S1 #2).

        Useful on programmatic suites; on human-judgment suites the
        counts are derived from the same programmatic
        ``case_passed`` and are informational only (see the
        ``suite_authority`` header note).
        """
        breakdown = summary.get("per_uc_breakdown")
        if not breakdown:
            return (
                "<h2>Per Use-Case Breakdown (programmatic-suite rollup)</h2>"
                "<p>No per-UC data available.</p>"
            )

        rows: list[str] = []
        for uc, data in sorted(breakdown.items()):
            if isinstance(data, dict):
                count = self._si(data.get("count"))
                passed_uc = self._si(data.get("passed"))
                failed_uc = self._si(data.get("failed"))
                mean_c = self._fmt_score(data.get("mean_composite"))
            else:
                count = passed_uc = failed_uc = 0
                mean_c = "N/A"

            rows.append(
                f"<tr><td>{escape(str(uc))}</td><td>{count}</td>"
                f"<td>{passed_uc}</td><td>{failed_uc}</td>"
                f"<td>{mean_c}</td></tr>"
            )

        return f"""\
<h2>Per Use-Case Breakdown (programmatic-suite rollup)</h2>
<p class="tier-note">Programmatic <code>case_passed</code> per UC. Informational only when <code>suite_authority</code> is <code>human_review</code> (see the manifest for the human-judgment verdict).</p>
<table>
  <thead>
    <tr><th>Use Case</th><th>Count</th><th>Passed</th><th>Failed</th><th>Mean Composite</th></tr>
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
        composite = self._fmt_score(cr.get("composite_score"))

        # Tier-labelled badge.
        tier0_label, _t0_passed, _t0_total = self._tier0_outcome(cr)
        tier1_label = self._tier1_label(cr)
        tier2_label, t2 = self._tier2_summary(cr)

        def _badge(label: str) -> str:
            cls_map = {
                "PASS": "pass",
                "FAIL": "fail",
                "HUMAN_REVIEW": "human-review",
                "N/A": "na",
            }
            cls = cls_map.get(label, "na")
            return f'<span class="badge tier {cls}">{escape(label)}</span>'

        tier_badges = (
            f'<span class="badge tier">T0</span> {_badge(tier0_label)} '
            f'<span class="badge tier">T1</span> {_badge(tier1_label)} '
            f'<span class="badge tier">T2</span> {_badge(tier2_label)}'
        )

        stall = bool(cr.get("stall_detected", False))
        stall_badge = ' <span class="badge stall">STALL</span>' if stall else ""

        # Key-value pairs
        authority = cr.get("case_passed_authority") or _AUTHORITY_PROGRAMMATIC
        kv = {
            "Expected UC": cr.get("primary_uc", "N/A"),
            "Actual UC": cr.get("active_use_case", "N/A"),
            "Expected Outcome": cr.get("expected_outcome", "N/A"),
            "Actual Outcome": cr.get("containment_outcome", "N/A"),
            "case_passed_authority": authority,
            "case_passed (programmatic)": str(bool(cr.get("case_passed", False))),
            "Composite Score": composite,
            "Outcome Score (advisory)": self._fmt_score(cr.get("outcome_score")),
            "Judge Score (advisory)": self._fmt_score(cr.get("judge_score")),
            "Stop Reason": cr.get("stop_reason", "N/A"),
            "Total Turns": str(cr.get("total_turns", "N/A")),
            "Session ID": cr.get("session_id", "N/A"),
        }
        kv_html = "".join(
            f"<dt>{escape(k)}</dt><dd>{escape(str(v))}</dd>" for k, v in kv.items()
        )

        # L1 hard checks (Tier-0 floor)
        l1_html = self._render_l1(cr.get("l1_results"))

        # Tier-2 critical-flow detail
        tier2_html = self._render_tier2(t2)

        # L2 outcome checks (Tier-3 advisory)
        l2_html = self._render_l2(cr.get("l2_results"))

        # L3 LLM judge (Tier-3 advisory)
        l3_html = self._render_l3(cr.get("l3_results"))

        # Failure tags
        tags = cr.get("failure_tags") or []
        tags_html = "".join(f'<span class="tag">{escape(str(t))}</span>' for t in tags) if tags else '<span style="color:var(--gray-500);font-size:0.85rem;">None</span>'

        return f"""\
<details>
  <summary>{case_id} &mdash; {tier_badges}{stall_badge} &mdash; composite {composite}</summary>
  <div class="case-body">
    <dl class="kv-grid">{kv_html}</dl>

    <h3>Tier-0 &mdash; L1 Hard Checks</h3>
    {l1_html}

    <h3>Tier-2 &mdash; Critical-flow (<code>tier2_result</code>)</h3>
    {tier2_html}

    <h3>Tier-3 &mdash; L2 Advisory Outcome Scores</h3>
    {l2_html}

    <h3>Tier-3 &mdash; L3 Advisory Judge Scores</h3>
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
                name = r.get("check", "") or r.get("check_name", "")
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

    def _render_tier2(self, tier2_result: dict | None) -> str:
        """Render the per-case ``tier2_result`` payload.

        Surfaces ``passed`` / ``severity`` / ``failed_step_ids`` and the
        per-step outcomes from
        :class:`scoring.skill_procedure_check.Tier2Result`. Prior to
        Sprint 50 / M5 S1, the field was populated by the executor on
        every case dict but never rendered.
        """
        if not tier2_result:
            return '<p style="color:var(--gray-500);font-size:0.85rem;">No Tier-2 result (no Skill critical-steps applicable).</p>'

        passed = tier2_result.get("passed")
        severity = str(tier2_result.get("severity") or "")
        failed_ids = [str(s) for s in (tier2_result.get("failed_step_ids") or [])]
        detail = str(tier2_result.get("detail") or "")

        if passed is True:
            header_badge = '<span class="badge pass">PASS</span>'
        elif passed is False:
            header_badge = '<span class="badge fail">FAIL</span>'
        else:
            header_badge = '<span class="badge na">N/A</span>'

        per_step = tier2_result.get("per_step") or []
        if per_step:
            rows: list[str] = []
            for step in per_step:
                if not isinstance(step, dict):
                    continue
                step_id = escape(str(step.get("step_id", "")))
                outcome = str(step.get("outcome", "")).upper()
                if outcome == "PASS":
                    icon_cls = "pass"
                    icon_char = "&#10003;"
                elif outcome == "FAIL":
                    icon_cls = "fail"
                    icon_char = "&#10007;"
                else:
                    icon_cls = "na"
                    icon_char = "&mdash;"
                step_severity = escape(str(step.get("severity", "")))
                step_detail = escape(str(step.get("detail", "")))
                rows.append(
                    f"<tr>"
                    f'<td><span class="check-icon {icon_cls}">{icon_char}</span> <code>{step_id}</code></td>'
                    f"<td>{escape(outcome) if outcome else '&mdash;'}</td>"
                    f"<td>{step_severity}</td>"
                    f"<td style=\"font-size:0.82rem;color:var(--gray-500);\">{step_detail}</td>"
                    f"</tr>"
                )
            per_step_html = (
                "<table><thead><tr><th>Step</th><th>Outcome</th><th>Severity</th><th>Detail</th></tr></thead>"
                f"<tbody>{''.join(rows)}</tbody></table>"
            )
        else:
            per_step_html = '<p style="color:var(--gray-500);font-size:0.85rem;">No per-step detail.</p>'

        failed_block = (
            f"<p style=\"font-size:0.85rem;\"><strong>failed_step_ids:</strong> "
            f"{escape(', '.join(failed_ids))}</p>"
            if failed_ids
            else ""
        )

        return f"""\
<p style="font-size:0.9rem;">
  {header_badge}
  <span style="margin-left:0.5rem;"><strong>severity:</strong> {escape(severity) if severity else '&mdash;'}</span>
  {(' <span style="margin-left:0.5rem;color:var(--gray-500);">' + escape(detail) + '</span>') if detail else ''}
</p>
{failed_block}
{per_step_html}"""

    def _render_l2(self, results) -> str:
        if not results:
            return '<p style="color:var(--gray-500);font-size:0.85rem;">No L2 advisory dimensions.</p>'
        rows: list[str] = []
        for r in results:
            if isinstance(r, dict):
                name = r.get("check", "") or r.get("check_name", "")
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
            return '<p style="color:var(--gray-500);font-size:0.85rem;">No L3 advisory judge scores.</p>'
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
<div class="tier-box">
  <div class="tier-stat-row">
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

    @staticmethod
    def _html_footer() -> str:
        return """\
<div class="footer">
  Generated by Interactive Eval Harness &mdash; M3-Eval four-tier verdict surface (Sprint 50 / M5 S1)
</div>"""
