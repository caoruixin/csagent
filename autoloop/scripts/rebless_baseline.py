"""S-Auto-17 baseline re-bless tool (M-Auto-4).

Runs each v1 fitness suite `n` times (real-LLM) via the S-Auto-16 n-loop
and writes a per-suite AGGREGATED baseline artifact
(`<out-dir>/<suite>/aggregated.json`) whose per-case verdict is the
MAJORITY over provider-comparable attempts, plus a per-case stability
classification (`stable` / `reducible-flaky` / `near-coinflip` /
`non_comparable`) derived from the majority `pass_rate`.

WHY this exists (S-Auto-17 §3): flipping the live loop to n>1 against a
single-draw baseline is asymmetric (baseline lucky-pass vs candidate
honest-majority-fail → manufactured regressions). The re-bless makes the
baseline a majority-over-n artifact FIRST so both sides are symmetric.

This is a ONE-OFF measurement tool, NOT part of the live scoring path —
it is intentionally OUTSIDE the `gaming._SCORING_CODE_FILES` drift set. It
performs the same provider-comparability + majority computation as the
live loop (it reuses `eval_runner.run_v1_fitness_suite` and
`aggregate.classify_stability`), so the blessed baseline is computed by
the exact code the live gate uses.

Usage (run on a CLEAN COMMITTED tree, backend up):

    cd autoloop && uv run python scripts/rebless_baseline.py \
        --n 5 \
        --out-dir ../eval_interactive/results/m-auto-4-baseline-YYYYMMDD

Fences honoured: never invokes mvn/spring-boot/git-mutating commands;
never edits eval_interactive case_specs; only WRITES aggregated.json under
--out-dir (and scratch eval runs under eval_interactive/results/). It does
NOT move the config.fitness.baseline_dir pointer — that is a separate,
human-authorized config edit recorded in the sprint handoff.
"""

from __future__ import annotations

import argparse
import copy
import json
import subprocess
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import yaml

_PACKAGE_ROOT = Path(__file__).resolve().parents[1]      # autoloop/
_REPO_ROOT = _PACKAGE_ROOT.parent                        # repo root
sys.path.insert(0, str(_PACKAGE_ROOT))

from autoloop.scoring.aggregate import (  # noqa: E402
    DEFAULT_STABILITY_THRESHOLDS,
    classify_stability,
)
from autoloop.scoring.eval_runner import run_v1_fitness_suite  # noqa: E402

_CONFIG_PATH = _PACKAGE_ROOT / "config.yaml"


def _load_env_files() -> None:
    """Load repo-root + autoloop .env.local into os.environ so the
    eval-interactive subprocess inherits backend/simulator/judge creds.
    Silent if python-dotenv or the files are absent.
    """
    try:
        from dotenv import load_dotenv  # type: ignore[import-not-found]
    except ImportError:
        return
    for env_path in (_REPO_ROOT / ".env.local", _PACKAGE_ROOT / ".env.local"):
        if env_path.exists():
            load_dotenv(env_path, override=False)


def _git_head() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=str(_REPO_ROOT),
            capture_output=True,
            text=True,
            timeout=10,
        )
        return out.stdout.strip() if out.returncode == 0 else "<unknown>"
    except (OSError, subprocess.TimeoutExpired):
        return "<unknown>"


def _git_tree_dirty() -> bool:
    try:
        out = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=str(_REPO_ROOT),
            capture_output=True,
            text=True,
            timeout=10,
        )
        return bool((out.stdout or "").strip())
    except (OSError, subprocess.TimeoutExpired):
        return False


def _stability_thresholds(fitness_cfg: dict[str, Any]) -> dict[str, float]:
    t = dict(DEFAULT_STABILITY_THRESHOLDS)
    cfg_t = fitness_cfg.get("stability_thresholds") or {}
    for k, v in cfg_t.items():
        try:
            t[k] = float(v)
        except (TypeError, ValueError):
            pass
    return t


def _valid_attempts(case: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        a for a in (case.get("attempts") or [])
        if a.get("valid") and a.get("case_passed") is not None
    ]


def _derive_model_provider(case: dict[str, Any]) -> tuple[str | None, str]:
    """Per-case (model, provider) from the VALID attempts. Valid attempts are
    provider-comparable (primary) by construction, so provider is `primary`
    when every valid attempt had fallback_count==0, else `mixed`.
    """
    valid = _valid_attempts(case)
    if not valid:
        return None, "non_comparable"
    models = Counter(a.get("actual_model") for a in valid if a.get("actual_model"))
    model = models.most_common(1)[0][0] if models else None
    provider = "primary" if all((a.get("fallback_count") or 0) == 0 for a in valid) else "mixed"
    return model, provider


def _l1_from_tier0_majority(case: dict[str, Any]) -> list[dict[str, Any]] | None:
    """Majority-collapsed Tier-0 check list so the baseline Layer-0 delta in
    tier_evaluator reads a MAJORITY verdict (symmetric with the candidate's
    `tier0_majority` stable-reproduction path). Returns None when the suite
    carries no Tier-0 check majority (then the original l1_results is kept).
    """
    tier0_majority = case.get("tier0_majority")
    if isinstance(tier0_majority, dict) and tier0_majority:
        return [
            {"check": cn, "passed": bool(passed), "detail": "majority"}
            for cn, passed in tier0_majority.items()
        ]
    return None


def _build_aggregated_suite(
    suite_name: str,
    results_json: Path,
    *,
    n: int,
    thresholds: dict[str, float],
    primary_model: str | None,
    git_commit: str,
    min_valid: int,
) -> dict[str, Any]:
    payload = json.loads(results_json.read_text(encoding="utf-8"))
    cases_in = payload.get("case_results") or []
    cases_out: list[dict[str, Any]] = []
    stability_counter: Counter[str] = Counter()

    for case in cases_in:
        pass_rate = case.get("pass_rate")
        stability_class = classify_stability(pass_rate, thresholds)
        stability_counter[stability_class] += 1
        model, provider = _derive_model_provider(case)
        out_case: dict[str, Any] = {
            "case_id": case.get("case_id", "<unknown>"),
            "primary_uc": case.get("primary_uc"),
            "majority_passed": case.get("majority_passed"),
            "pass_rate": pass_rate,
            "valid_attempts": case.get("valid_attempts"),
            "total_attempts": case.get("total_attempts"),
            "comparable": case.get("comparable"),
            "flaky": case.get("flaky"),
            "aggregate_status": case.get("aggregate_status"),
            "stability_class": stability_class,
            "model": model,
            "provider": provider,
            "tier2_result_majority": case.get("tier2_result_majority") or {"per_step": []},
            "attempts": case.get("attempts") or [],
        }
        # Majority-collapsed Tier-0 check list (falls back to the raw
        # single-draw l1_results when no Tier-0 majority is present).
        l1 = _l1_from_tier0_majority(case)
        out_case["l1_results"] = l1 if l1 is not None else (case.get("l1_results") or [])
        if "tier0_majority" in case:
            out_case["tier0_majority"] = case["tier0_majority"]
        cases_out.append(out_case)

    return {
        "schema": "autoloop.baseline.aggregated.v1",
        "suite": suite_name,
        "git_commit": git_commit,
        "captured_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "n": n,
        "samples_per_case": n,
        "primary_model": primary_model,
        "stability_thresholds": thresholds,
        "_aggregation": payload.get("_aggregation")
        or {"method": "majority", "min_valid_attempts": min_valid, "suite": suite_name},
        "stability_summary": dict(stability_counter),
        "case_results": cases_out,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Re-bless the v1 fitness baseline as an aggregated artifact.")
    parser.add_argument("--n", type=int, default=5, help="samples per case for the re-bless (>=5).")
    parser.add_argument("--out-dir", required=True, help="Dated baseline output dir (e.g. eval_interactive/results/m-auto-4-baseline-YYYYMMDD).")
    parser.add_argument("--config", default=str(_CONFIG_PATH), help="Path to autoloop config.yaml.")
    parser.add_argument("--scratch-dir", default=None, help="Scratch results_root for the n-loop (default: <out-dir>/_rebless_scratch).")
    parser.add_argument("--allow-dirty", action="store_true", help="Proceed even if the git tree is dirty (NOT recommended).")
    args = parser.parse_args()

    if args.n < 5:
        print(f"[rebless] WARNING: n={args.n} < 5; the re-bless contract calls for n>=5.", file=sys.stderr)

    if _git_tree_dirty() and not args.allow_dirty:
        print(
            "[rebless] ABORT: git tree is dirty. Run the re-bless on a CLEAN "
            "COMMITTED tree so git_commit is meaningful and the dirty-index "
            "hazard cannot poison anything. Use --allow-dirty to override.",
            file=sys.stderr,
        )
        return 2

    _load_env_files()

    config_path = Path(args.config)
    config = yaml.safe_load(config_path.read_text(encoding="utf-8"))
    fitness_cfg = config.get("fitness") or {}
    thresholds = _stability_thresholds(fitness_cfg)
    primary_model = ((fitness_cfg.get("provider_policy") or {}).get("primary_model"))
    min_valid = int((fitness_cfg.get("aggregation") or {}).get("min_valid_attempts", 3))
    git_commit = _git_head()

    # Resolve a relative --out-dir against the CURRENT working directory
    # (the intuitive behaviour when invoked as `cd autoloop && ... --out-dir
    # ../eval_interactive/...`), NOT against the repo root.
    out_dir = Path(args.out_dir)
    if not out_dir.is_absolute():
        out_dir = (Path.cwd() / out_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    scratch = Path(args.scratch_dir) if args.scratch_dir else (out_dir / "_rebless_scratch")
    scratch.mkdir(parents=True, exist_ok=True)

    # Re-bless config: override samples_per_case to n; everything else
    # (suites, parallel, provider_policy, aggregation) inherited as-is.
    rebless_cfg = copy.deepcopy(config)
    rebless_cfg.setdefault("fitness", {})["samples_per_case"] = args.n

    print(f"[rebless] git_commit={git_commit}")
    print(f"[rebless] n={args.n}  primary_model={primary_model!r}  out_dir={out_dir}")
    print(f"[rebless] running n-loop into scratch={scratch} ...")

    suite_results = run_v1_fitness_suite(results_root=scratch, config=rebless_cfg)

    report: dict[str, Any] = {}
    for suite_name, sr in suite_results.items():
        rj = Path(sr.results_json)
        if not rj.exists():
            print(f"[rebless] WARNING: suite {suite_name} produced no results.json (exit={sr.exit_code})", file=sys.stderr)
            continue
        agg = _build_aggregated_suite(
            suite_name,
            rj,
            n=args.n,
            thresholds=thresholds,
            primary_model=primary_model,
            git_commit=git_commit,
            min_valid=min_valid,
        )
        suite_out = out_dir / suite_name
        suite_out.mkdir(parents=True, exist_ok=True)
        (suite_out / "aggregated.json").write_text(json.dumps(agg, indent=2, default=str), encoding="utf-8")
        report[suite_name] = {
            "n_cases": len(agg["case_results"]),
            "stability_summary": agg["stability_summary"],
            "cases": {
                c["case_id"]: {
                    "majority_passed": c["majority_passed"],
                    "pass_rate": c["pass_rate"],
                    "stability_class": c["stability_class"],
                }
                for c in agg["case_results"]
            },
        }
        print(f"[rebless] wrote {suite_out / 'aggregated.json'}  "
              f"({len(agg['case_results'])} cases, stability={agg['stability_summary']})")

    (out_dir / "_rebless_report.json").write_text(json.dumps(report, indent=2, default=str), encoding="utf-8")
    print(f"[rebless] DONE. report={out_dir / '_rebless_report.json'}")
    print("[rebless] NOTE: baseline_dir pointer NOT moved — that is a separate, "
          "human-authorized config edit (recorded in the sprint handoff).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
