"""autoloop CLI — subcommand router.

Subcommands (S-Auto-7.1 adds `preflight`; S-Auto-3 wires the prior five):

    check       Validate config + run sandbox self-test (UNCHANGED from S-Auto-1).
    preflight   Dev-env discovery (foreground :8080 / postgres / redis /
                API key / working tree / baseline_dir). Standalone surface.
    dry-run     Run N iterations in dry-run mode (no apply, no eval).
    run         Run N live iterations end-to-end. Pre-flight runs FIRST
                unless `--skip-preflight`; `--auto-reboot` kills a foreground
                :8080 listener that blocks the applier alt-port Spring spawn.
    report      Render `autoloop/results/report.html` audit timeline.
    apply       Hybrid: cherry-pick an exp-N branch, emit baseline patch, NO
                auto-commit (OQ-S55.1 disposition).
    audit       Inspect one iteration. Default RESPECTS shadow firewall;
                `--include-shadow-detail` opens the human-only surface.

Entry points:
    autoloop <subcommand>          (via pyproject.toml [project.scripts])
    python -m autoloop <subcommand>
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Sequence

import yaml

from . import config_validator as _config_validator
from . import preflight as _preflight
from .memory import experiments_log as _experiments_log
from .memory import iterations_index as _iterations_index
from .memory import lessons_log as _lessons_log
from .sandbox.yaml_diff_validator import validate_skill_yaml_diff


_PACKAGE_ROOT = Path(__file__).resolve().parents[1]
_CONFIG_PATH = _PACKAGE_ROOT / "config.yaml"
_ENV_LOCAL_PATH = _PACKAGE_ROOT / ".env.local"


def _auto_load_env_local() -> None:
    """OQ-S60.10 ergonomics — auto-load `autoloop/.env.local` into
    `os.environ` so `AUTOLOOP_META_LLM_API_KEY` is available to the
    meta-agent client without requiring `set -a; source .env.local;
    set +a` first. Silent if the file is absent OR python-dotenv is
    unavailable — pre-flight `check_meta_llm_api_key` will then
    surface the missing-key fail with actionable remediation.
    """
    if not _ENV_LOCAL_PATH.exists():
        return
    try:
        from dotenv import load_dotenv  # type: ignore[import-not-found]
    except ImportError:
        return
    load_dotenv(_ENV_LOCAL_PATH, override=False)


# --- Shared config loader --------------------------------------------


def _load_config(config_path: Path) -> dict[str, Any]:
    with config_path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def _resolve_repo_root(config_path: Path) -> Path:
    return config_path.resolve().parent.parent


# --- Subcommand: check ------------------------------------------------


def _cmd_check(args: argparse.Namespace) -> int:
    config_path = Path(args.config) if args.config else _CONFIG_PATH
    if not config_path.exists():
        print(f"[check] FAIL: config not found at {config_path}", file=sys.stderr)
        return 1

    cfg = _load_config(config_path)
    repo_root = _resolve_repo_root(config_path)
    surface = cfg.get("mutable_surface", {})
    allowed_files = surface.get("allowed_skill_files") or []
    allowed_fields = surface.get("allowed_field_paths") or []

    print(f"[check] config: {config_path}")
    print(f"[check] repo root: {repo_root}")
    print(f"[check] allowed_field_paths: {len(allowed_fields)}")
    print(f"[check] allowed_skill_files: {len(allowed_files)}")

    missing: list[str] = []
    for rel in allowed_files:
        abs_path = repo_root / rel
        if not abs_path.exists():
            missing.append(rel)
    if missing:
        for m in missing:
            print(f"[check] FAIL: allowed_skill_files entry missing on disk: {m}", file=sys.stderr)
        return 1
    print(f"[check] PASS: all {len(allowed_files)} Skill YAML paths exist on disk")

    if not _self_test_sandbox(allowed_files, allowed_fields):
        return 1
    print("[check] PASS: sandbox self-test (1 positive + 1 negative)")
    return 0


def _self_test_sandbox(
    allowed_files: list[str], allowed_fields: list[str]
) -> bool:
    if not allowed_files:
        print("[check] FAIL: no allowed_skill_files; cannot self-test", file=sys.stderr)
        return False
    pos_target = allowed_files[0]

    before = (
        "name: discover_triage\n"
        "tools_required:\n  - search_knowledge\n"
        "procedure: \"step one\"\n"
    )
    after_positive = (
        "name: discover_triage\n"
        "tools_required:\n  - search_knowledge\n"
        "procedure: \"step one revised\"\n"
    )
    after_negative = (
        "name: discover_triage\n"
        "tools_required:\n  - search_knowledge\n  - classify_use_case\n"
        "procedure: \"step one\"\n"
    )

    pos = validate_skill_yaml_diff(
        before_yaml=before,
        after_yaml=after_positive,
        file_path=pos_target,
        allowed_skill_files=allowed_files,
        allowed_field_paths=allowed_fields,
    )
    if pos.decision != "ACCEPT":
        print(
            f"[check] FAIL: self-test positive expected ACCEPT, got {pos.decision} ({pos.reason})",
            file=sys.stderr,
        )
        return False

    neg = validate_skill_yaml_diff(
        before_yaml=before,
        after_yaml=after_negative,
        file_path=pos_target,
        allowed_skill_files=allowed_files,
        allowed_field_paths=allowed_fields,
    )
    if neg.decision != "REJECT":
        print(
            f"[check] FAIL: self-test negative expected REJECT, got {neg.decision} ({neg.reason})",
            file=sys.stderr,
        )
        return False
    return True


# --- Subcommand: preflight (S-Auto-7.1) ------------------------------


def _cmd_preflight(args: argparse.Namespace) -> int:
    """Standalone surface for `python -m autoloop preflight`.

    Exit 0 if all checks are ok-or-warn; 1 if any check returned
    `fail`. The same checks run inside `_cmd_run` before the loop
    starts (gated by `--skip-preflight`).
    """
    config_path = Path(args.config) if args.config else _CONFIG_PATH
    if not config_path.exists():
        print(f"[preflight] FAIL: config not found at {config_path}", file=sys.stderr)
        return 1
    cfg = _load_config(config_path)
    repo_root = _resolve_repo_root(config_path)

    _auto_load_env_local()
    results = _preflight.run_preflight(cfg, repo_root=repo_root)
    print(_preflight.format_report(results))
    return 1 if any(r.status == "fail" for r in results) else 0


def _run_preflight_gate(
    cfg: dict[str, Any],
    repo_root: Path,
    *,
    auto_reboot: bool,
    skip_preflight: bool,
) -> int:
    """Pre-flight gate for `_cmd_run`. Returns 0 to proceed; non-zero
    to refuse to start. On `--auto-reboot`, attempts to kill the
    foreground :8080 listener and re-runs pre-flight once.
    """
    if skip_preflight:
        print(
            "[run] WARN: --skip-preflight set; bypassing env discovery (DEV-ONLY)",
            file=sys.stderr,
        )
        return 0

    results = _preflight.run_preflight(cfg, repo_root=repo_root)
    print(_preflight.format_report(results))
    fails = [r for r in results if r.status == "fail"]
    if not fails:
        return 0

    if not auto_reboot:
        print(
            "[run] FAIL: pre-flight reported %d failing check(s); "
            "address remediation above OR re-run with --skip-preflight "
            "(dev-only)" % len(fails),
            file=sys.stderr,
        )
        return 1

    fg = next(
        (r for r in fails if r.name == "foreground_backend"), None
    )
    if fg is None or not (fg.details and fg.details.get("pids")):
        print(
            "[run] FAIL: --auto-reboot only recovers `foreground_backend` "
            "fails; %d other check(s) failing — address remediation above"
            % len(fails),
            file=sys.stderr,
        )
        return 1

    print(
        "[run] --auto-reboot: killing foreground :%s listener(s) pid=%s"
        % (fg.details.get("port"), fg.details.get("pids"))
    )
    for pid in fg.details["pids"]:
        ok = _preflight.auto_reboot_foreground_backend(int(pid))
        print("[run] auto-reboot pid=%d → %s" % (pid, "stopped" if ok else "FAILED"))
        if not ok:
            print(
                "[run] FAIL: could not stop pid=%d; check permissions" % pid,
                file=sys.stderr,
            )
            return 1

    results = _preflight.run_preflight(cfg, repo_root=repo_root)
    print(_preflight.format_report(results))
    fails = [r for r in results if r.status == "fail"]
    if fails:
        print(
            "[run] FAIL: pre-flight still reports %d failing check(s) after "
            "auto-reboot — address remediation above" % len(fails),
            file=sys.stderr,
        )
        return 1
    return 0


# --- Subcommand: run / dry-run ---------------------------------------


def _cmd_run(args: argparse.Namespace) -> int:
    config_path = Path(args.config) if args.config else _CONFIG_PATH
    if not config_path.exists():
        print(f"[run] FAIL: config not found at {config_path}", file=sys.stderr)
        return 1
    cfg = _load_config(config_path)
    repo_root = _resolve_repo_root(config_path)

    # OQ-S60.10 — auto-load autoloop/.env.local so AUTOLOOP_META_LLM_API_KEY
    # is available without `set -a; source .env.local; set +a`.
    _auto_load_env_local()

    count = int(args.experiments)
    dry_run = bool(args.dry_run) or args._invoked_as == "dry-run"

    if count <= 0:
        print("[run] FAIL: --experiments must be >= 1", file=sys.stderr)
        return 1

    # S-Auto-7.1 — pre-flight env discovery before live runs. dry-run
    # skips pre-flight by construction (no apply / no eval / no LLM
    # call, so substrate state is irrelevant).
    if not dry_run:
        gate = _run_preflight_gate(
            cfg,
            repo_root,
            auto_reboot=bool(getattr(args, "auto_reboot", False)),
            skip_preflight=bool(getattr(args, "skip_preflight", False)),
        )
        if gate != 0:
            return gate

    # Lazy-import the loop so `autoloop check` doesn't pay the
    # import cost on a fresh checkout.
    from .loop import run_iterations

    print(f"[run] starting {count} iteration(s), dry_run={dry_run}")
    results = run_iterations(
        config=cfg,
        count=count,
        dry_run=dry_run,
        repo_root=repo_root,
    )

    n_keep = sum(1 for r in results if r.decision == "keep")
    n_discard = sum(1 for r in results if r.decision == "discard")
    n_error = sum(1 for r in results if r.decision == "error")
    print(
        f"[run] done: keep={n_keep} discard={n_discard} error={n_error} "
        f"(of {len(results)} total)"
    )
    for r in results:
        print(
            f"  {r.iteration_id}: decision={r.decision} "
            f"discard_reason={r.discard_reason or '-'} "
            f"elapsed={r.elapsed_seconds:.1f}s"
        )

    # Exit 0 if no iteration errored; exit non-zero on error
    # (errors are infra / unrecoverable, NOT keep/discard verdict).
    return 1 if n_error > 0 else 0


# --- Subcommand: report ----------------------------------------------


def _cmd_report(args: argparse.Namespace) -> int:
    config_path = Path(args.config) if args.config else _CONFIG_PATH
    if not config_path.exists():
        print(f"[report] FAIL: config not found at {config_path}", file=sys.stderr)
        return 1
    cfg = _load_config(config_path)
    repo_root = _resolve_repo_root(config_path)

    paths_cfg = cfg.get("paths") or {}
    experiments_log = repo_root / paths_cfg.get(
        "experiments_log", "autoloop/results/experiments.jsonl"
    )
    sqlite_path = repo_root / "autoloop/results/iterations.sqlite"
    lessons_path = repo_root / paths_cfg.get(
        "lessons_log", "autoloop/results/lessons.md"
    )
    out_path = repo_root / "autoloop/results/report.html"
    out_path.parent.mkdir(parents=True, exist_ok=True)

    records = _experiments_log.read_all(experiments_log)
    lessons_md = _lessons_log.read_all(lessons_path)
    hit_rates = _pilot_hit_rates(cfg, repo_root, records)
    html = _render_report_html(records, lessons_md, sqlite_path, hit_rates)
    out_path.write_text(html, encoding="utf-8")
    print(f"[report] wrote {out_path} ({len(records)} iterations)")
    return 0


def _pilot_hit_rates(
    cfg: dict[str, Any], repo_root: Path, records: list[dict[str, Any]]
) -> dict[str, Any]:
    """Compute the 4-layer pilot hit-rate audit over the iteration log.

    Forensic / observation-only — touches no gate or baseline. The pilot
    card + per-skill phase/UC map are read fresh; a config with no pilot
    block yields a degenerate (all-`general`) decomposition.
    """
    surface = cfg.get("mutable_surface") or {}
    allowed_files = surface.get("allowed_skill_files") or []
    try:
        pilot = _config_validator.validate_pilot_config(cfg, repo_root)
    except _config_validator.PilotConfigError:
        pilot = {}
    skill_map = _config_validator.build_skill_phase_usecase_map(
        allowed_files, repo_root
    )
    return _config_validator.compute_pilot_hit_rates(records, skill_map, pilot)


def _format_hit_rate(value: Any) -> str:
    return "n/a" if value is None else f"{value * 100:.1f}%"


def _render_pilot_hit_rate_html(hit_rates: dict[str, Any]) -> str:
    """Render the 4-layer pilot hit-rate audit block (forensic)."""
    if not hit_rates or hit_rates.get("scored_iterations", 0) == 0:
        return "<h2>Pilot hit-rate audit</h2><p>no scored iterations yet</p>"
    venn = hit_rates.get("partial_hit_breakdown") or {}
    venn_rows = "\n".join(
        f"<tr><td>{_html_escape(k)}</td><td>{v}</td></tr>"
        for k, v in sorted(venn.items(), key=lambda kv: (-kv[1], kv[0]))
    )
    return f"""<h2>Pilot hit-rate audit (forensic; {hit_rates['scored_iterations']} scored iters)</h2>
<table><tbody>
<tr><th>phase_usecase_hit_rate</th><td>{_format_hit_rate(hit_rates.get('phase_usecase_hit_rate'))}</td></tr>
<tr><th>skill_hit_rate (resolve_faq_grounded_answer.yaml)</th><td>{_format_hit_rate(hit_rates.get('skill_hit_rate'))}</td></tr>
<tr><th>field_family_hit_rate</th><td>{_format_hit_rate(hit_rates.get('field_family_hit_rate'))}</td></tr>
<tr><th>full_on_gap_hit_rate (S-Y2 primary success metric)</th><td><b>{_format_hit_rate(hit_rates.get('full_on_gap_hit_rate'))}</b></td></tr>
</tbody></table>
<h3>partial_hit_breakdown</h3>
<table><thead><tr><th>layers hit</th><th>count</th></tr></thead><tbody>
{venn_rows or "<tr><td colspan='2'>-</td></tr>"}
</tbody></table>"""


def _render_report_html(
    records: list[dict[str, Any]],
    lessons_md: str,
    sqlite_path: Path,
    hit_rates: dict[str, Any] | None = None,
) -> str:
    """Minimal HTML report. v1 is a flat table; richer surfaces are
    M-Auto-2 concerns."""
    rows = []
    for r in records:
        hyp = r.get("hypothesis") or {}
        verdict = r.get("verdict") or {}
        tier_breakdown = verdict.get("tier_breakdown") or {}
        decision = r.get("decision", "?")
        rows.append(
            f"<tr><td>{r.get('iteration_id','?')}</td>"
            f"<td>{_html_escape(hyp.get('target_skill_file','?'))}</td>"
            f"<td>{_html_escape(hyp.get('target_field_path','?'))}</td>"
            f"<td class='dec-{decision}'>{decision}</td>"
            f"<td>{_html_escape(r.get('discard_reason') or '-')}</td>"
            f"<td><pre>{_html_escape(json.dumps(tier_breakdown, indent=2)[:400])}</pre></td>"
            f"<td>autoloop/results/runs/{r.get('iteration_id','?')}/</td>"
            f"</tr>"
        )
    body = "\n".join(rows) if rows else "<tr><td colspan='7'>no iterations yet</td></tr>"
    return f"""<!doctype html>
<html><head><meta charset="utf-8"><title>autoloop report</title>
<style>
body {{ font-family: -apple-system, sans-serif; margin: 2em; }}
table {{ border-collapse: collapse; width: 100%; }}
th, td {{ border: 1px solid #ccc; padding: 6px; text-align: left; vertical-align: top; }}
th {{ background: #f5f5f5; }}
.dec-keep {{ color: green; font-weight: bold; }}
.dec-discard {{ color: gray; }}
.dec-error {{ color: red; font-weight: bold; }}
pre {{ font-size: 11px; margin: 0; max-width: 400px; overflow: auto; }}
</style></head>
<body>
<h1>autoloop iteration timeline</h1>
<p>sqlite index: {sqlite_path}</p>
<table><thead><tr>
<th>iter</th><th>target_skill</th><th>target_field</th><th>decision</th>
<th>discard_reason</th><th>tier_breakdown</th><th>per-iter dir</th>
</tr></thead><tbody>
{body}
</tbody></table>
<hr>
{_render_pilot_hit_rate_html(hit_rates or {})}
<hr>
<h2>Lessons</h2>
<pre>{_html_escape(lessons_md)}</pre>
</body></html>
"""


def _html_escape(s: Any) -> str:
    if s is None:
        return ""
    s = str(s)
    return (
        s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


# --- Subcommand: apply (Hybrid; OQ-S55.1) ----------------------------


def _cmd_apply(args: argparse.Namespace) -> int:
    config_path = Path(args.config) if args.config else _CONFIG_PATH
    if not config_path.exists():
        print(f"[apply] FAIL: config not found at {config_path}", file=sys.stderr)
        return 1
    repo_root = _resolve_repo_root(config_path)

    if not args.experiment:
        print("[apply] FAIL: --experiment <exp-N> is required", file=sys.stderr)
        return 1
    exp_id = args.experiment
    branch = f"autoloop/{exp_id}"

    # 1. Verify the branch exists.
    proc = subprocess.run(
        ["git", "rev-parse", "--verify", branch],
        cwd=str(repo_root),
        capture_output=True,
        text=True,
    )
    if proc.returncode != 0:
        print(
            f"[apply] FAIL: branch '{branch}' does not exist locally",
            file=sys.stderr,
        )
        return 1

    # 2. Cherry-pick the branch tip onto the current branch.
    cp = subprocess.run(
        ["git", "cherry-pick", branch],
        cwd=str(repo_root),
        capture_output=True,
        text=True,
    )
    if cp.returncode != 0:
        print(
            f"[apply] FAIL: git cherry-pick {branch} failed:\n{cp.stderr}",
            file=sys.stderr,
        )
        return 1
    print(f"[apply] cherry-pick of {branch} applied to current branch")

    # 3. Compute baseline-patch text + write to per-iteration dir.
    runs_dir = repo_root / "autoloop" / "results" / "runs" / exp_id
    runs_dir.mkdir(parents=True, exist_ok=True)
    proposed_baseline = f"autoloop/results/runs/{exp_id}/eval"
    patch_text = (
        "--- a/autoloop/config.yaml\n"
        "+++ b/autoloop/config.yaml\n"
        f"@@ baseline_dir update for {exp_id} @@\n"
        f"-  baseline_dir: <previous>\n"
        f"+  baseline_dir: {proposed_baseline}\n"
    )
    patch_path = runs_dir / "proposed-baseline-update.patch"
    patch_path.write_text(patch_text, encoding="utf-8")
    print("---- proposed config.yaml patch ----")
    print(patch_text)
    print("------------------------------------")
    print(
        f"[apply] Cherry-pick applied. Review the proposed config.yaml patch above; "
        f"if accepting, apply it manually, then `git add autoloop/config.yaml && "
        f"git commit --amend --no-edit`. If rejecting baseline advance, just keep "
        f"the cherry-pick as-is (do NOT auto-commit anything via this tool)."
    )
    print(f"[apply] Patch file written to: {patch_path}")
    print(
        f"[apply] NO auto-commit performed (OQ-S55.1 Hybrid disposition; "
        f"human review + commit required)."
    )
    return 0


# --- Subcommand: audit -----------------------------------------------


def _cmd_audit(args: argparse.Namespace) -> int:
    config_path = Path(args.config) if args.config else _CONFIG_PATH
    if not config_path.exists():
        print(f"[audit] FAIL: config not found at {config_path}", file=sys.stderr)
        return 1
    cfg = _load_config(config_path)
    repo_root = _resolve_repo_root(config_path)

    if not args.experiment:
        print("[audit] FAIL: --experiment <exp-N> is required", file=sys.stderr)
        return 1
    exp_id = args.experiment

    paths_cfg = cfg.get("paths") or {}
    experiments_log = repo_root / paths_cfg.get(
        "experiments_log", "autoloop/results/experiments.jsonl"
    )
    sqlite_path = repo_root / "autoloop/results/iterations.sqlite"

    records = _experiments_log.read_all(experiments_log)
    match = next(
        (r for r in records if r.get("iteration_id") == exp_id), None
    )
    if match is None:
        print(f"[audit] FAIL: no record for {exp_id} in {experiments_log}", file=sys.stderr)
        return 1
    sqlite_matches = _iterations_index.query_recent(sqlite_path, 9999)
    sqlite_match = next((r for r in sqlite_matches if r.id == exp_id), None)

    # Default mode RESPECTS the shadow firewall: the persisted record
    # already carries only aggregate Layer 4 metrics. We do not
    # re-open a per-case shadow lookup.
    include_shadow_detail = bool(getattr(args, "include_shadow_detail", False))

    # S-Auto-4 surfaces — extracted from the persisted record for a
    # compact summary in addition to the full record JSON.
    gaming_flags_summary = [
        {
            "rule_id": (f or {}).get("rule_id"),
            "severity": (f or {}).get("severity"),
            "detail": (f or {}).get("detail"),
        }
        for f in (match.get("gaming_flags") or [])
    ]
    anti_hardcode_flag_for_codex = bool(
        match.get("anti_hardcode_flag_for_codex", False)
    )

    # S-Y1.5 (#5): per-iteration pilot hit decomposition (forensic). The
    # running aggregate over the whole log is included for context.
    pilot_hit_rates = _pilot_hit_rates(cfg, repo_root, records)
    surface = cfg.get("mutable_surface") or {}
    try:
        pilot_card = _config_validator.validate_pilot_config(cfg, repo_root)
    except _config_validator.PilotConfigError:
        pilot_card = {}
    skill_map = _config_validator.build_skill_phase_usecase_map(
        surface.get("allowed_skill_files") or [], repo_root
    )
    iteration_hits = _config_validator.iteration_hits(match, skill_map, pilot_card)

    rendered = {
        "iteration_id": exp_id,
        "experiments_log_record": match,
        "iterations_index_record": (
            _iterations_index.record_to_dict(sqlite_match)
            if sqlite_match else None
        ),
        "anti_hardcode_flag_for_codex": anti_hardcode_flag_for_codex,
        "gaming_flags_summary": gaming_flags_summary,
        "pilot_iteration_hits": iteration_hits,
        "pilot_hit_rate_aggregate": pilot_hit_rates,
    }

    if include_shadow_detail:
        # Human-only path. Re-run the tier evaluator with audit=True
        # against the per-iter eval results, if available.
        eval_dir = repo_root / "autoloop" / "results" / "runs" / exp_id / "eval"
        if eval_dir.exists():
            shadow_detail = _resolve_shadow_detail(eval_dir, cfg, repo_root)
            rendered["shadow_audit_detail"] = shadow_detail
        else:
            rendered["shadow_audit_detail"] = {
                "error": f"per-iter eval dir not found at {eval_dir}"
            }

    print(json.dumps(rendered, indent=2, default=str))
    return 0


def _resolve_shadow_detail(
    eval_dir: Path, cfg: dict[str, Any], repo_root: Path
) -> dict[str, Any]:
    """Re-load the shadow results.json and produce a human-facing
    per-case summary. Reached ONLY via `--include-shadow-detail`.
    """
    shadow_results = eval_dir / "shadow" / "results.json"
    if not shadow_results.exists():
        return {"error": f"shadow results.json not found at {shadow_results}"}
    try:
        with shadow_results.open("r", encoding="utf-8") as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        return {"error": f"cannot read shadow results: {e}"}
    cases = data.get("case_results") or []
    per_case_failures = [
        {
            "case_id": c.get("case_id"),
            "primary_uc": c.get("primary_uc"),
            "failure_tags": c.get("failure_tags"),
        }
        for c in cases
        if c.get("case_passed") is not True
    ]
    return {
        "per_case_failures": per_case_failures,
        "total_cases": len(cases),
        "passed": sum(1 for c in cases if c.get("case_passed") is True),
    }


# --- Argument parser --------------------------------------------------


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="autoloop",
        description="Auto-evolution loop subsystem (M-Auto-1A). See autoloop/program.md.",
    )
    sub = parser.add_subparsers(dest="subcommand", metavar="<subcommand>")
    sub.required = True

    # check
    p_check = sub.add_parser("check", help="Validate config + run sandbox self-test.")
    p_check.add_argument("--config", help="Path to autoloop config.yaml (default: package config).")
    p_check.set_defaults(func=_cmd_check)

    # preflight (S-Auto-7.1)
    p_preflight = sub.add_parser(
        "preflight",
        help=(
            "Dev-env discovery (foreground :8080 / postgres / redis / "
            "API key / working tree / baseline_dir). Exit 1 on any fail."
        ),
    )
    p_preflight.add_argument("--config", help="Path to autoloop config.yaml.")
    p_preflight.set_defaults(func=_cmd_preflight)

    # dry-run (alias for run --dry-run)
    p_dry = sub.add_parser(
        "dry-run",
        help="Single-or-multi iteration in dry-run mode (no apply, no eval, no memory writes).",
    )
    p_dry.add_argument("--config", help="Path to autoloop config.yaml.")
    p_dry.add_argument("--experiments", "-n", type=int, default=1, help="How many iterations (default 1).")
    p_dry.set_defaults(func=_cmd_run, dry_run=True, _invoked_as="dry-run")

    # run
    p_run = sub.add_parser(
        "run", help="Live iteration loop end-to-end (apply + eval + memory)."
    )
    p_run.add_argument("--config", help="Path to autoloop config.yaml.")
    p_run.add_argument("--experiments", "-n", type=int, default=1, help="How many iterations (default 1).")
    p_run.add_argument(
        "--dry-run",
        action="store_true",
        help="No apply / no eval / no long-term memory writes.",
    )
    p_run.add_argument(
        "--auto-reboot",
        action="store_true",
        help=(
            "On pre-flight `foreground_backend` fail, kill the listening "
            "PID(s) and re-run pre-flight. DEV-ONLY ergonomics; does NOT "
            "recover other pre-flight fail modes."
        ),
    )
    p_run.add_argument(
        "--skip-preflight",
        action="store_true",
        help=(
            "DEV ESCAPE HATCH: skip pre-flight env discovery entirely. "
            "Use only when you've already verified env state manually."
        ),
    )
    p_run.set_defaults(func=_cmd_run, _invoked_as="run")

    # report
    p_report = sub.add_parser("report", help="Render per-iteration audit HTML report.")
    p_report.add_argument("--config", help="Path to autoloop config.yaml.")
    p_report.set_defaults(func=_cmd_report)

    # apply (Hybrid)
    p_apply = sub.add_parser(
        "apply",
        help="Cherry-pick autoloop/<exp-N> + emit baseline patch. NO auto-commit.",
    )
    p_apply.add_argument("--config", help="Path to autoloop config.yaml.")
    p_apply.add_argument(
        "--experiment",
        required=True,
        help="Iteration id (e.g. exp-7) whose autoloop/<id> branch should be picked.",
    )
    p_apply.set_defaults(func=_cmd_apply)

    # audit
    p_audit = sub.add_parser(
        "audit",
        help="Inspect one iteration. Default RESPECTS shadow firewall.",
    )
    p_audit.add_argument("--config", help="Path to autoloop config.yaml.")
    p_audit.add_argument(
        "--experiment",
        required=True,
        help="Iteration id (e.g. exp-7) to inspect.",
    )
    p_audit.add_argument(
        "--include-shadow-detail",
        action="store_true",
        help=(
            "Open the human-only per-case shadow surface. The loop / "
            "meta-agent NEVER takes this path; only humans auditing a "
            "candidate before merge."
        ),
    )
    p_audit.set_defaults(func=_cmd_audit)

    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    # Ensure both `dry-run` and `run --dry-run` produce dry_run=True.
    if not hasattr(args, "dry_run"):
        args.dry_run = False
    if not hasattr(args, "_invoked_as"):
        args._invoked_as = args.subcommand
    return args.func(args)


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
