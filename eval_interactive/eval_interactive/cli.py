"""Click CLI for the Interactive Evaluation Harness."""

from __future__ import annotations

import asyncio
import logging
import sys

import click


@click.group()
def main():
    """Interactive Evaluation Harness for CS Agent."""
    pass


@main.command()
@click.option("--hr-csv", required=True, help="Path to HR annotations CSV")
@click.option("--turns-dir", required=True, help="Path to eval_datasets directory")
@click.option("--output", default="case_specs/", help="Output directory")
@click.option("--verbose", "-v", is_flag=True, help="Enable verbose logging")
def extract(hr_csv: str, turns_dir: str, output: str, verbose: bool):
    """Extract CaseSpecs from HR annotations + turn data."""
    from pathlib import Path

    from eval_interactive.case_spec.extractor import extract_case_specs

    log_level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format="%(levelname)s: %(message)s",
        stream=sys.stderr,
    )

    hr_path = Path(hr_csv)
    turns_path = Path(turns_dir)
    output_path = Path(output)

    click.echo(f"Extracting CaseSpecs ...")
    click.echo(f"  HR CSV:    {hr_path}")
    click.echo(f"  Turns dir: {turns_path}")
    click.echo(f"  Output:    {output_path}")

    try:
        specs = extract_case_specs(hr_path, turns_path, output_path)
    except FileNotFoundError as e:
        click.echo(f"Error: {e}", err=True)
        raise SystemExit(1)

    # Report results
    click.echo(f"\nExtracted {len(specs)} CaseSpec(s).")

    # Tally by case set subdirectory
    from collections import Counter

    set_dirs = Counter()
    for yaml_file in output_path.rglob("*.yaml"):
        set_dirs[yaml_file.parent.name] += 1
    for set_name in sorted(set_dirs):
        click.echo(f"  {set_name}/: {set_dirs[set_name]} cases")

    # UC distribution
    uc_counts = Counter(s.expected.primary_uc for s in specs)
    click.echo("\nUC distribution:")
    for uc, count in sorted(uc_counts.items()):
        click.echo(f"  {uc}: {count}")

    # Outcome distribution
    outcome_counts = Counter(s.expected.outcome_class for s in specs)
    click.echo("\nOutcome distribution:")
    for oc, count in sorted(outcome_counts.items()):
        click.echo(f"  {oc}: {count}")


@main.command()
@click.option(
    "--set", "case_set", default="anchor",
    help="Case set to run: anchor, promotion, exploration, smoke, or all",
)
@click.option("--path", "custom_path", default=None, help="Custom case spec path (file or dir)")
@click.option("--label", default=None, help="Run label")
@click.option("--parallel", default=None, type=int, help="Parallel sessions (default from config)")
@click.option("--limit", default=None, type=int, help="Max number of cases to run (UC-balanced sampling)")
@click.option("--config", "config_path", default=None, help="Config file path")
@click.option("--verbose", "-v", is_flag=True, help="Enable verbose logging")
def run(
    case_set: str,
    custom_path: str | None,
    label: str | None,
    parallel: int | None,
    limit: int | None,
    config_path: str | None,
    verbose: bool,
):
    """Run interactive evaluation batch."""
    log_level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format="%(levelname)s: %(message)s",
        stream=sys.stderr,
    )

    from eval_interactive.batch.executor import BatchExecutor
    from eval_interactive.batch.sets import CaseSetManager
    from eval_interactive.config import load_config

    # 1. Load config
    config = load_config(config_path)

    # 2. Load cases
    if custom_path:
        manager = CaseSetManager()
        cases = manager.load_custom(custom_path)
        click.echo(f"Loaded {len(cases)} case(s) from custom path: {custom_path}")
    else:
        manager = CaseSetManager()
        cases = manager.load_set(case_set)
        click.echo(f"Loaded {len(cases)} case(s) from set '{case_set}'")

    # 3. Apply --limit with UC-balanced sampling
    if limit is not None and limit < len(cases):
        cases = _balanced_sample(cases, limit)
        click.echo(f"Sampled {len(cases)} case(s) with UC-balanced selection")

    if not cases:
        click.echo("No cases found. Exiting.", err=True)
        raise SystemExit(1)

    # 3. Run batch
    executor = BatchExecutor(config)
    result = asyncio.run(executor.run_batch(cases, label=label, parallel=parallel))

    # 4. Print summary
    s = result.summary
    click.echo("\n--- Summary ---")
    click.echo(f"  Run ID:             {result.run_id}")
    click.echo(f"  Label:              {result.label}")
    click.echo(f"  Total cases:        {s['total_cases']}")
    click.echo(f"  Passed:             {s['passed_cases']}")
    click.echo(f"  Failed:             {s['failed_cases']}")
    click.echo(f"  Success rate:       {s['task_success_rate']:.1%}")
    click.echo(f"  Stall rate:         {s['stall_rate']:.1%}")
    click.echo(f"  Mean composite:     {s['mean_composite_score']:.4f}")
    click.echo(f"  Mean outcome:       {s['mean_outcome_score']:.4f}")
    click.echo(f"  Mean judge:         {s['mean_judge_score']:.4f}")
    click.echo(f"  Escalation correct: {s['escalation_correctness']:.1%}")
    click.echo(f"  Policy compliance:  {s['policy_compliance_rate']:.1%}")
    click.echo(f"  Mean turns:         {s['mean_turns_to_resolution']:.1f}")
    click.echo(f"  Elapsed:            {result.elapsed_ms}ms")

    # Per-UC breakdown
    if s.get("per_uc_breakdown"):
        click.echo("\n  Per-UC breakdown:")
        for uc, data in sorted(s["per_uc_breakdown"].items()):
            click.echo(
                f"    {uc:10s}  "
                f"count={data['count']}  "
                f"passed={data['passed']}  "
                f"failed={data['failed']}  "
                f"mean_composite={data['mean_composite']:.4f}"
            )


@main.command()
@click.option("--baseline", default=None, help="Baseline results path (default: registered baseline)")
@click.option("--current", required=True, help="Current results path (file or dir)")
@click.option("--output", "-o", default=None, help="Output HTML report path (default: <current>/comparison.html)")
def compare(baseline: str | None, current: str, output: str | None):
    """Compare two eval runs and report regressions.

    If --baseline is omitted, uses the registered baseline from baseline.json.
    """
    from pathlib import Path

    from eval_interactive.comparison.diff_engine import DiffEngine
    from eval_interactive.report.html_report import HtmlReportGenerator

    if baseline is None:
        baseline = _get_registered_baseline()
        if baseline is None:
            click.echo(
                "Error: No baseline registered. Use 'set-baseline' or pass --baseline explicitly.",
                err=True,
            )
            raise SystemExit(1)
        click.echo(f"Using registered baseline: {baseline}")

    engine = DiffEngine()

    try:
        result = engine.compare(baseline, current)
    except FileNotFoundError as e:
        click.echo(f"Error: {e}", err=True)
        raise SystemExit(1)

    # Text report to stdout
    report = engine.format_report(result)
    click.echo(report)

    # HTML comparison report
    current_data = engine._load_results(current)
    comparison_dict = {
        "baseline_label": result.baseline_label,
        "current_label": result.current_label,
        "overall_baseline_score": result.metric_deltas.get(
            "mean_composite_score", {}
        ).get("baseline", 0),
        "overall_current_score": result.metric_deltas.get(
            "mean_composite_score", {}
        ).get("current", 0),
        "overall_delta": result.metric_deltas.get(
            "mean_composite_score", {}
        ).get("delta", 0),
        "case_diffs": [
            {
                "case_id": r["case_id"],
                "baseline_score": r["baseline_composite"],
                "current_score": r["current_composite"],
                "delta": round(r["current_composite"] - r["baseline_composite"], 4),
                "regressions": r.get("failure_tags", []),
                "improvements": [],
            }
            for r in result.regressed + result.improved
        ],
        "metric_deltas": {
            k: v.get("delta", 0)
            for k, v in result.metric_deltas.items()
        },
    }

    html_gen = HtmlReportGenerator()
    html = html_gen.generate(
        run_id=current_data.get("run_id", "comparison"),
        label=current_data.get("label", "current"),
        case_results=current_data.get("case_results", []),
        summary=current_data.get("summary", {}),
        comparison=comparison_dict,
    )

    if output is None:
        current_path = Path(current)
        if current_path.is_dir():
            output = str(current_path / "comparison.html")
        else:
            output = str(current_path.parent / "comparison.html")

    html_path = html_gen.save(html, output)
    click.echo(f"\nHTML comparison report saved to {html_path}")


# ------------------------------------------------------------------
# Baseline management
# ------------------------------------------------------------------

_BASELINE_FILE = "results/baseline.json"


@main.command("set-baseline")
@click.argument("run_path")
def set_baseline(run_path: str):
    """Register a run as the current baseline.

    RUN_PATH can be a results directory (e.g. results/20260425-083912)
    or a results.json file.
    """
    import json
    from pathlib import Path

    p = Path(run_path)
    if p.is_dir():
        results_file = p / "results.json"
    else:
        results_file = p
        p = results_file.parent

    if not results_file.exists():
        click.echo(f"Error: {results_file} not found", err=True)
        raise SystemExit(1)

    with open(results_file) as f:
        data = json.load(f)

    baseline_info = {
        "run_id": data.get("run_id", p.name),
        "label": data.get("label", ""),
        "path": str(p),
        "timestamp": data.get("timestamp", ""),
        "summary": {
            "total_cases": data.get("summary", {}).get("total_cases", 0),
            "passed_cases": data.get("summary", {}).get("passed_cases", 0),
            "task_success_rate": data.get("summary", {}).get("task_success_rate", 0),
            "mean_composite_score": data.get("summary", {}).get("mean_composite_score", 0),
        },
    }

    baseline_path = Path(_BASELINE_FILE)
    baseline_path.parent.mkdir(parents=True, exist_ok=True)
    with open(baseline_path, "w") as f:
        json.dump(baseline_info, f, indent=2)

    click.echo(f"Baseline registered:")
    click.echo(f"  Run ID:    {baseline_info['run_id']}")
    click.echo(f"  Label:     {baseline_info['label']}")
    click.echo(f"  Path:      {baseline_info['path']}")
    click.echo(f"  Cases:     {baseline_info['summary']['total_cases']}")
    click.echo(f"  Pass rate: {baseline_info['summary']['task_success_rate']:.1%}")
    click.echo(f"  Composite: {baseline_info['summary']['mean_composite_score']:.4f}")


@main.command("runs")
def list_runs():
    """List all evaluation runs with baseline indicator."""
    import json
    from pathlib import Path

    results_dir = Path("results")
    if not results_dir.exists():
        click.echo("No results directory found.")
        return

    baseline_run_id = None
    baseline_path = Path(_BASELINE_FILE)
    if baseline_path.exists():
        with open(baseline_path) as f:
            baseline_run_id = json.load(f).get("run_id")

    runs: list[dict] = []
    for d in sorted(results_dir.iterdir()):
        rfile = d / "results.json"
        if d.is_dir() and rfile.exists():
            with open(rfile) as f:
                data = json.load(f)
            s = data.get("summary", {})
            runs.append({
                "run_id": data.get("run_id", d.name),
                "label": data.get("label", ""),
                "cases": s.get("total_cases", 0),
                "passed": s.get("passed_cases", 0),
                "rate": s.get("task_success_rate", 0),
                "composite": s.get("mean_composite_score", 0),
                "timestamp": data.get("timestamp", ""),
                "path": str(d),
            })

    if not runs:
        click.echo("No runs found.")
        return

    click.echo(f"{'':3s} {'Run ID':<20s} {'Label':<25s} {'Cases':>5s} {'Pass':>5s} {'Rate':>7s} {'Composite':>10s}")
    click.echo("-" * 82)
    for r in runs:
        is_bl = " * " if r["run_id"] == baseline_run_id else "   "
        click.echo(
            f"{is_bl}{r['run_id']:<20s} {r['label']:<25s} "
            f"{r['cases']:>5d} {r['passed']:>5d} "
            f"{r['rate']:>6.1%} {r['composite']:>10.4f}"
        )

    if baseline_run_id:
        click.echo(f"\n * = current baseline")
    else:
        click.echo(f"\nNo baseline registered. Use 'set-baseline <run_path>' to set one.")


def _get_registered_baseline() -> str | None:
    """Return the registered baseline path, or None if not set."""
    import json
    from pathlib import Path

    p = Path(_BASELINE_FILE)
    if not p.exists():
        return None
    with open(p) as f:
        data = json.load(f)
    return data.get("path")


def _balanced_sample(cases: list, limit: int) -> list:
    """Sample *limit* cases with round-robin across UCs for balanced coverage."""
    from collections import defaultdict

    by_uc: dict[str, list] = defaultdict(list)
    for c in cases:
        by_uc[c.expected.primary_uc].append(c)

    sampled: list = []
    uc_iters = {uc: iter(specs) for uc, specs in sorted(by_uc.items())}

    while len(sampled) < limit and uc_iters:
        exhausted: list[str] = []
        for uc, it in uc_iters.items():
            if len(sampled) >= limit:
                break
            try:
                sampled.append(next(it))
            except StopIteration:
                exhausted.append(uc)
        for uc in exhausted:
            del uc_iters[uc]

    sampled.sort(key=lambda s: s.case_id)
    return sampled


if __name__ == "__main__":
    main()
