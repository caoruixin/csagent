"""Top-level orchestrator for the auto-evolution loop.

S-Auto-3 deliverable; S-Auto-4 added the content_validator (pre-
sandbox) + gaming.detect (post-eval) insertion points. Full state
machine per iteration:

    1.   analyzer.analyze(baseline, lessons, recent)        → FailureTaxonomy
    2.   proposer.propose(taxonomy, lessons, recent, ...)   → Hypothesis
    2.5. sandbox.validate_content(hypothesis)               → ContentValidationResult
    3.   sandbox.validate_skill_yaml_diff(...)              → ValidationResult
    4.   sandbox.anti_hardcode_check(hypothesis)            → AntiHardcodeResult
         # PASS / FAIL / FLAG_FOR_CODEX; FLAG_FOR_CODEX continues.
    5.   dry-run: write artefacts under runs/<id>/, STOP.
    6.   applier.apply(hypothesis)                          → AppliedExperiment
    7.   eval_runner.run_v1_fitness_suite(...)              → dict[suite, SuiteRunResult]
    8.   baseline_loader.load(...)                          → BaselineSnapshot
    9.   tier_evaluator.evaluate(...)                       → LexicographicVerdict
    9.5. gaming.detect(...)                                 → list[GamingFlag]
         # Observation-only; never alters keep/discard.
    10.  memory.experiments_log.append(...)
    11.  memory.iterations_index.insert(...)
    12.  if iter_count % K == 0: lessons_compactor.compact(...)
    13.  git tag autoloop/<keep|discard>-N on the exp-N branch
    14.  applier.cleanup() — always in finally.

A failure at ANY step is caught; `IterationResult.decision` is set
to `"error"` with the exception string; `cleanup()` still runs.
The loop continues to the next iteration so a single bad iter
does not crash a whole run.

Shadow firewall RESPECTED: the verdict serialized to
experiments_log contains only the aggregate Layer 4 metrics (the
default `evaluate(...)` API surface). Per-case shadow detail is
never reached via this code path.
"""

from __future__ import annotations

import json
import os
import subprocess
import time
import traceback
from dataclasses import asdict, dataclass, field, is_dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Literal

from .memory import experiments_log as _experiments_log
from .memory import iterations_index as _iterations_index
from .memory import lessons_log as _lessons_log
from .memory.iterations_index import IterationRecord
from .meta_agent import analyzer as _analyzer
from .meta_agent import lessons_compactor as _lessons_compactor
from .meta_agent import proposer as _proposer
from .meta_agent.llm_client import LLMClient, build_client_from_config
from .meta_agent.proposer import Hypothesis, ProposerInvalidOutputError
from .sandbox import applier as _applier
from .sandbox import anti_hardcode_check as _anti_hardcode_module  # noqa: F401 — re-imported as module below
from .sandbox.anti_hardcode_check import (
    AntiHardcodeResult,
    anti_hardcode_check as _anti_hardcode_check_fn,
)
from .sandbox.applier import AppliedExperiment
from .sandbox.content_validator import (
    ContentValidationResult,
    validate_content as _validate_content_fn,
)
from .sandbox.yaml_diff_validator import (
    ValidationResult,
    validate_skill_yaml_diff,
)
from .scoring import baseline_loader, eval_runner, gaming as _gaming, tier_evaluator
from .scoring.gaming import GamingFlag
from .scoring.tier_evaluator import LexicographicVerdict


_REPO_ROOT = Path(__file__).resolve().parents[2]


@dataclass
class IterationResult:
    """One iteration's outcome.

    `decision` is "keep" / "discard" / "error". `error` is set only
    when decision == "error" — it holds the exception text. All
    other fields are best-effort populated up to the point the
    iteration short-circuited.

    S-Auto-4 added two additive optional fields:
    - `content_validator_verdict` — verdict from the pre-sandbox
      content validator (None on iterations that short-circuited
      earlier).
    - `gaming_flags` — list of observation-only anti-gaming flags
      (post-eval; never alters keep/discard).
    - `anti_hardcode_flag_for_codex` — True when the anti-hardcode
      detector returned FLAG_FOR_CODEX (the iteration continued
      through apply / eval; the flag is for human / Codex review).
    """

    iteration_id: str
    hypothesis: Hypothesis | None = None
    sandbox_verdict: ValidationResult | None = None
    anti_hardcode_verdict: AntiHardcodeResult | None = None
    applied: AppliedExperiment | None = None
    eval_results: dict[str, Any] | None = None
    verdict: LexicographicVerdict | None = None
    decision: Literal["keep", "discard", "error"] = "discard"
    discard_reason: str | None = None
    error: str | None = None
    elapsed_seconds: float = 0.0
    content_validator_verdict: ContentValidationResult | None = None
    gaming_flags: list[dict[str, Any]] = field(default_factory=list)
    anti_hardcode_flag_for_codex: bool = False


def run_one_iteration(
    *,
    config: dict[str, Any],
    iteration_id: str,
    dry_run: bool = False,
    client: LLMClient | None = None,
    repo_root: Path | None = None,
) -> IterationResult:
    """Run one full iteration end-to-end.

    `dry_run=True` short-circuits after the anti-hardcode placeholder:
    no apply, no Spring spawn, no eval, no long-term memory writes.
    Dry-run artefacts go under `autoloop/results/runs/<id>/`.

    The function always returns; exceptions are caught and recorded
    in IterationResult.decision="error".
    """
    started = time.monotonic()
    root = Path(repo_root) if repo_root else _REPO_ROOT
    result = IterationResult(iteration_id=iteration_id)
    applied: AppliedExperiment | None = None

    try:
        # --- 0. LLM client + paths.
        if client is None:
            client = build_client_from_config(config)
        paths = _resolve_paths(config, root)

        # --- 1. Analyzer.
        baseline_summary = _build_baseline_summary(config, root)
        lessons_md = _lessons_log.read_all(paths["lessons"])
        recent_iters_records = _iterations_index.query_recent(
            paths["sqlite"],
            int(((config or {}).get("lessons") or {}).get("recent_iterations_for_propose", 5)),
        )
        recent_iters_dicts = [
            _record_to_dict_for_meta_agent(r) for r in recent_iters_records
        ]
        taxonomy = _analyzer.analyze(
            baseline_summary,
            lessons_md,
            recent_iters_dicts,
            client=client,
        )

        # --- 2. Proposer.
        try:
            hypothesis = _proposer.propose(
                taxonomy,
                lessons_md,
                recent_iters_dicts,
                client=client,
                config=config,
            )
        except ProposerInvalidOutputError as e:
            result.decision = "discard"
            result.discard_reason = "proposer_llm_returned_invalid_json"
            result.error = str(e)
            _persist_iteration(
                result, config, root, dry_run=dry_run, applied=None,
            )
            return _finalize(result, started)
        result.hypothesis = hypothesis

        # --- 2.5. Content validator (S-Auto-4, pre-sandbox).
        cv_verdict = _validate_content_fn(hypothesis, config=config)
        result.content_validator_verdict = cv_verdict
        if cv_verdict.verdict != "PASS":
            result.decision = "discard"
            result.discard_reason = (
                f"content_validator_rejected:{cv_verdict.rule_id}"
            )
            _persist_iteration(result, config, root, dry_run=dry_run, applied=None)
            return _finalize(result, started)

        # --- 3. Sandbox.
        surface_cfg = (config or {}).get("mutable_surface") or {}
        allowed_files = surface_cfg.get("allowed_skill_files") or []
        allowed_paths = surface_cfg.get("allowed_field_paths") or []
        before_yaml, after_yaml = _materialize_before_after_yaml(
            hypothesis, root
        )
        sandbox_verdict = validate_skill_yaml_diff(
            before_yaml=before_yaml,
            after_yaml=after_yaml,
            file_path=hypothesis.target_skill_file,
            allowed_skill_files=allowed_files,
            allowed_field_paths=allowed_paths,
        )
        result.sandbox_verdict = sandbox_verdict
        if sandbox_verdict.decision != "ACCEPT":
            result.decision = "discard"
            result.discard_reason = f"sandbox_rejected:{sandbox_verdict.reason}"
            _persist_iteration(result, config, root, dry_run=dry_run, applied=None)
            return _finalize(result, started)

        # --- 4. Anti-hardcode (S-Auto-4 real detector).
        ah_verdict = _anti_hardcode_check_fn(hypothesis, config=config)
        result.anti_hardcode_verdict = ah_verdict
        if ah_verdict.verdict == "FAIL":
            result.decision = "discard"
            result.discard_reason = (
                f"anti_hardcode_rejected:{ah_verdict.rule_id}"
            )
            _persist_iteration(result, config, root, dry_run=dry_run, applied=None)
            return _finalize(result, started)
        if ah_verdict.verdict == "FLAG_FOR_CODEX":
            # Observation-only: continue the iteration; surface the
            # flag for Codex / human review via the iteration record.
            result.anti_hardcode_flag_for_codex = True

        # --- 5. Dry-run short circuit.
        if dry_run:
            _write_dry_run_artefacts(result, config, root)
            result.decision = "discard"
            result.discard_reason = "dry_run_no_apply"
            return _finalize(result, started)

        # --- 6. Applier.
        applied = _applier.apply(
            hypothesis,
            iteration_id=iteration_id,
            config=config,
            repo_root=root,
        )
        result.applied = applied

        # --- 7. Eval.
        results_root = root / "autoloop" / "results" / "runs" / iteration_id / "eval"
        results_root.mkdir(parents=True, exist_ok=True)
        backend_url = f"http://127.0.0.1:{applied.backend_port}"
        old_url = os.environ.get("CSAGENT_BACKEND_URL")
        os.environ["CSAGENT_BACKEND_URL"] = backend_url
        try:
            suite_run_results = eval_runner.run_v1_fitness_suite(
                results_root=results_root,
                config=config,
            )
        finally:
            # Restore old env var (no leakage into other tests / iters).
            if old_url is None:
                os.environ.pop("CSAGENT_BACKEND_URL", None)
            else:
                os.environ["CSAGENT_BACKEND_URL"] = old_url
        result.eval_results = {
            name: _safe_asdict(r) for name, r in suite_run_results.items()
        }

        # --- 8. Baseline.
        baseline = _load_baseline(config, root)

        # --- 9. Tier evaluator.
        verdict = tier_evaluator.evaluate(
            current_results=results_root,
            baseline=baseline,
            config=config,
            iteration_id=iteration_id,
        )
        result.verdict = verdict
        result.decision = "keep" if verdict.decision == "keep" else "discard"
        result.discard_reason = (
            verdict.discard_reason if verdict.decision == "discard" else None
        )

        # --- 9.5. Anti-gaming checks (observation-only in v1).
        try:
            result.gaming_flags = _run_gaming_detect(
                result, results_root, baseline, config, root,
            )
        except Exception:
            # Gaming detector is observation-only; any internal
            # failure must not block iteration persistence.
            result.gaming_flags = []

        # --- 10/11. Persist memory (Layer A + Layer B).
        _persist_iteration(
            result, config, root, dry_run=dry_run, applied=applied,
        )

        # --- 12. Lessons compaction trigger.
        _maybe_compact_lessons(config, root, client)

        # --- 13. Branch tag.
        try:
            _git_tag(
                root,
                tag=f"autoloop/{result.decision}-{iteration_id.replace('exp-', '')}",
                target=applied.branch_name,
            )
        except Exception:
            # Tagging is best-effort; failure here does not flip
            # the iteration to error.
            pass

        return _finalize(result, started)

    except Exception as e:  # noqa: BLE001 — orchestrator catch-all.
        result.decision = "error"
        result.error = f"{e.__class__.__name__}: {e}\n{traceback.format_exc()}"
        try:
            _persist_iteration(
                result, config, root, dry_run=dry_run, applied=applied,
            )
        except Exception:
            pass
        return _finalize(result, started)
    finally:
        # --- 14. Cleanup is ALWAYS called.
        if applied is not None:
            try:
                _applier.cleanup(applied, repo_root=root)
            except Exception:
                pass


def run_iterations(
    *,
    config: dict[str, Any],
    count: int,
    dry_run: bool = False,
    client: LLMClient | None = None,
    repo_root: Path | None = None,
    start_index: int | None = None,
) -> list[IterationResult]:
    """Run `count` iterations sequentially.

    Iteration ids are generated as `exp-<N>` where N continues from
    the highest existing iteration in the experiments log, OR starts
    at `start_index` if provided.
    """
    root = Path(repo_root) if repo_root else _REPO_ROOT
    if start_index is None:
        start_index = _next_iteration_index(config, root)
    if client is None:
        client = build_client_from_config(config)

    out: list[IterationResult] = []
    for i in range(count):
        iteration_id = f"exp-{start_index + i}"
        r = run_one_iteration(
            config=config,
            iteration_id=iteration_id,
            dry_run=dry_run,
            client=client,
            repo_root=root,
        )
        out.append(r)
    return out


# --- Internals --------------------------------------------------------


def _resolve_paths(config: dict[str, Any], root: Path) -> dict[str, Path]:
    paths_cfg = (config or {}).get("paths") or {}
    return {
        "experiments_log": root / paths_cfg.get(
            "experiments_log", "autoloop/results/experiments.jsonl"
        ),
        "sqlite": root / "autoloop" / "results" / "iterations.sqlite",
        "lessons": root / paths_cfg.get(
            "lessons_log", "autoloop/results/lessons.md"
        ),
        "runs_dir": root / paths_cfg.get(
            "runs_dir", "autoloop/results/runs/"
        ),
    }


def _build_baseline_summary(config: dict[str, Any], root: Path) -> dict[str, Any]:
    fitness_cfg = (config or {}).get("fitness") or {}
    baseline_dir_str = fitness_cfg.get("baseline_dir", "")
    if not baseline_dir_str or "<PLACEHOLDER" in baseline_dir_str:
        # No real baseline yet — the analyzer still runs but gets
        # an empty summary.
        return {}
    baseline_dir = root / baseline_dir_str
    if not baseline_dir.exists():
        return {}
    results_paths: dict[str, Path] = {}
    for entry in fitness_cfg.get("suites") or []:
        if not isinstance(entry, dict):
            continue
        name = entry.get("name")
        if not name:
            continue
        per_suite = baseline_dir / name / "results.json"
        if per_suite.exists():
            results_paths[name] = per_suite
        else:
            flat = baseline_dir / "results.json"
            if flat.exists():
                results_paths[name] = flat
    return _analyzer.build_baseline_summary(results_paths)


def _materialize_before_after_yaml(
    hypothesis: Hypothesis, root: Path
) -> tuple[str, str]:
    """Read current YAML on disk + produce the post-edit YAML using
    the same patch helper the applier uses (so the sandbox check
    matches what would land if we did apply).
    """
    skill_path = root / hypothesis.target_skill_file
    before = skill_path.read_text(encoding="utf-8") if skill_path.exists() else ""
    after = _applier._write_field_to_yaml(  # type: ignore[attr-defined]
        before, hypothesis.target_field_path, hypothesis.after_value
    )
    return before, after


def _load_baseline(config: dict[str, Any], root: Path):
    fitness_cfg = (config or {}).get("fitness") or {}
    baseline_dir_str = fitness_cfg.get("baseline_dir", "")
    if not baseline_dir_str or "<PLACEHOLDER" in baseline_dir_str:
        # No baseline configured — use empty BaselineSnapshot.
        return baseline_loader.BaselineSnapshot(
            baseline_run_id="<no-baseline>",
            baseline_dir=root,
            snapshots={},
            tier0_baseline={},
            captured_at=datetime.now(timezone.utc).isoformat(timespec="seconds"),
        )
    baseline_dir = root / baseline_dir_str
    return baseline_loader.load(baseline_dir, config=config)


def _persist_iteration(
    result: IterationResult,
    config: dict[str, Any],
    root: Path,
    *,
    dry_run: bool,
    applied: AppliedExperiment | None,
) -> None:
    """Append to experiments_log + iterations_index, except in dry-run."""
    record = _build_record_dict(result, applied)

    if dry_run:
        # Dry-run writes a dedicated dry-run log under runs/<id>/.
        runs_dir = root / "autoloop" / "results" / "runs" / result.iteration_id
        runs_dir.mkdir(parents=True, exist_ok=True)
        with (runs_dir / "dry_run_record.json").open("w", encoding="utf-8") as f:
            json.dump(record, f, indent=2, default=str)
        return

    paths = _resolve_paths(config, root)
    _experiments_log.append(paths["experiments_log"], record)
    _iterations_index.insert(
        paths["sqlite"], _build_index_record(result, applied)
    )


def _build_record_dict(
    result: IterationResult, applied: AppliedExperiment | None
) -> dict[str, Any]:
    """Serialize an IterationResult into the experiments_log row shape."""
    return {
        "iteration_id": result.iteration_id,
        "timestamp": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "hypothesis": _safe_asdict(result.hypothesis),
        "sandbox_verdict": _safe_asdict(result.sandbox_verdict),
        "content_validator_verdict": _safe_asdict(result.content_validator_verdict),
        "anti_hardcode_verdict": _safe_asdict(result.anti_hardcode_verdict),
        "anti_hardcode_flag_for_codex": result.anti_hardcode_flag_for_codex,
        "applied": _serialize_applied(applied),
        "verdict": _serialize_verdict(result.verdict),
        "gaming_flags": list(result.gaming_flags or []),
        "decision": result.decision,
        "discard_reason": result.discard_reason,
        "error": result.error,
        "elapsed_seconds": result.elapsed_seconds,
    }


def _run_gaming_detect(
    result: IterationResult,
    results_root: Path,
    baseline,
    config: dict[str, Any],
    root: Path,
) -> list[dict[str, Any]]:
    """Build the gaming.detect arguments + return serialized flags."""
    iteration_record = _build_record_dict(result, applied=result.applied)
    paths = _resolve_paths(config, root)
    recent_records = _experiments_log.read_recent(paths["experiments_log"], 20)
    baseline_dir_str = (config or {}).get("fitness", {}).get("baseline_dir") or ""
    baseline_dir = (
        root / baseline_dir_str
        if baseline_dir_str and "<PLACEHOLDER" not in baseline_dir_str
        else None
    )
    eval_artefacts = {
        "results_root": results_root,
        "verdict": result.verdict,
        "baseline_dir": baseline_dir,
        "baseline_snapshot": baseline,
    }
    flags = _gaming.detect(
        iteration_record,
        recent_records,
        eval_artefacts,
        config=config,
    )
    return [asdict(f) for f in flags]


def _summarize_gaming_flags(
    flags: list[dict[str, Any]] | None,
) -> list[dict[str, Any]]:
    """Compact gaming-flag view for the `audit` subcommand."""
    if not flags:
        return []
    return [
        {
            "rule_id": f.get("rule_id"),
            "severity": f.get("severity"),
            "detail": f.get("detail"),
        }
        for f in flags
    ]


def _build_index_record(
    result: IterationResult, applied: AppliedExperiment | None
) -> IterationRecord:
    hyp = result.hypothesis
    fitness_delta: dict[str, Any] = {}
    if result.verdict is not None:
        fitness_delta = {"tier_breakdown": result.verdict.tier_breakdown}
    return IterationRecord(
        id=result.iteration_id,
        ts=datetime.now(timezone.utc).isoformat(timespec="seconds"),
        target_skill=hyp.target_skill_file if hyp else "<none>",
        target_field=hyp.target_field_path if hyp else "<none>",
        edit_summary=(hyp.rationale[:120] if hyp else None),
        hypothesis_fingerprint=(hyp.fingerprint if hyp else ""),
        decision=result.decision,
        discard_reason=result.discard_reason,
        fitness_delta=fitness_delta,
        parent_iteration_id=None,
        notes=(result.error[:200] if result.error else None),
    )


def _serialize_applied(applied: AppliedExperiment | None) -> dict[str, Any] | None:
    if applied is None:
        return None
    return {
        "iteration_id": applied.iteration_id,
        "branch_name": applied.branch_name,
        "commit_sha": applied.commit_sha,
        "skill_file_path": str(applied.skill_file_path),
        "backend_port": applied.backend_port,
        "original_branch": applied.original_branch,
    }


def _serialize_verdict(
    verdict: LexicographicVerdict | None,
) -> dict[str, Any] | None:
    if verdict is None:
        return None
    return {
        "decision": verdict.decision,
        "discard_reason": verdict.discard_reason,
        "tier_breakdown": verdict.tier_breakdown,
        "layer_results": [_safe_asdict(lr) for lr in verdict.layer_results],
        "iteration_id": verdict.iteration_id,
    }


def _safe_asdict(obj: Any) -> Any:
    """asdict() that tolerates non-dataclass objects (raw dicts pass through)."""
    if obj is None:
        return None
    if is_dataclass(obj) and not isinstance(obj, type):
        return asdict(obj)
    if isinstance(obj, dict):
        return obj
    return str(obj)


def _record_to_dict_for_meta_agent(record: IterationRecord) -> dict[str, Any]:
    """Translate a sqlite row into the dict shape the meta-agent reads.

    The meta-agent reads the record's hypothesis (from the row's
    target_skill / target_field) plus the decision / discard_reason.
    """
    return {
        "iteration_id": record.id,
        "hypothesis": {
            "target_skill_file": record.target_skill,
            "target_field_path": record.target_field,
        },
        "decision": record.decision,
        "discard_reason": record.discard_reason,
        "verdict": {"tier_breakdown": record.fitness_delta.get("tier_breakdown", {})},
    }


def _maybe_compact_lessons(
    config: dict[str, Any], root: Path, client: LLMClient
) -> None:
    """Trigger lessons compaction every K iterations."""
    lessons_cfg = (config or {}).get("lessons") or {}
    k = int(lessons_cfg.get("compaction_window_k", 10))
    if k <= 0:
        return

    paths = _resolve_paths(config, root)
    all_records = _experiments_log.read_all(paths["experiments_log"])
    total = len(all_records)
    if total == 0 or total % k != 0:
        return

    window = all_records[-k:]
    current_lessons = _lessons_log.read_all(paths["lessons"])
    try:
        new_section = _lessons_compactor.compact(
            window, current_lessons, client=client
        )
    except Exception:
        return
    _lessons_log.append_lesson(paths["lessons"], new_section)


def _write_dry_run_artefacts(
    result: IterationResult, config: dict[str, Any], root: Path
) -> None:
    """Per-iteration dry-run artefacts under runs/<id>/."""
    runs_dir = root / "autoloop" / "results" / "runs" / result.iteration_id
    runs_dir.mkdir(parents=True, exist_ok=True)

    if result.hypothesis is not None:
        with (runs_dir / "hypothesis.json").open("w", encoding="utf-8") as f:
            json.dump(_safe_asdict(result.hypothesis), f, indent=2, default=str)
    if result.content_validator_verdict is not None:
        with (runs_dir / "content_validator_verdict.json").open(
            "w", encoding="utf-8"
        ) as f:
            json.dump(
                _safe_asdict(result.content_validator_verdict),
                f, indent=2, default=str,
            )
    if result.sandbox_verdict is not None:
        with (runs_dir / "sandbox_verdict.json").open("w", encoding="utf-8") as f:
            json.dump(_safe_asdict(result.sandbox_verdict), f, indent=2, default=str)
    if result.anti_hardcode_verdict is not None:
        with (runs_dir / "anti_hardcode_verdict.json").open(
            "w", encoding="utf-8"
        ) as f:
            json.dump(
                _safe_asdict(result.anti_hardcode_verdict), f, indent=2, default=str
            )

    record = _build_record_dict(result, applied=None)
    with (runs_dir / "dry_run_record.json").open("w", encoding="utf-8") as f:
        json.dump(record, f, indent=2, default=str)


def _next_iteration_index(config: dict[str, Any], root: Path) -> int:
    """Compute the next iteration index from the experiments log.

    Highest existing exp-<N> + 1; defaults to 1 if log is empty.
    """
    paths = _resolve_paths(config, root)
    records = _experiments_log.read_all(paths["experiments_log"])
    if not records:
        return 1
    max_idx = 0
    for r in records:
        iid = r.get("iteration_id") or ""
        if iid.startswith("exp-"):
            try:
                n = int(iid[len("exp-"):])
                if n > max_idx:
                    max_idx = n
            except ValueError:
                continue
    return max_idx + 1


def _git_tag(root: Path, *, tag: str, target: str) -> None:
    subprocess.run(
        ["git", "tag", "-f", tag, target],
        cwd=str(root),
        capture_output=True,
        text=True,
        check=False,
    )


def _finalize(result: IterationResult, started: float) -> IterationResult:
    result.elapsed_seconds = time.monotonic() - started
    return result
