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

from . import config_validator as _config_validator
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
    # S-Auto-11: per-iter eval traces persisted for §5.6 / §11 review
    # (R-overnight-eval-traces-not-persisted). Path to the consolidated
    # eval-results.json, or None if persistence failed / no eval ran.
    eval_traces_path: str | None = None
    # S-Auto-11 (OQ-S65.7/8): an iteration whose eval evidence is
    # infra-degraded (failed/empty suite OR pervasive LLM-deadline /
    # service_degraded prevalence) is marked infra-error. It is reported
    # with decision="error" (the non-fitness bucket) so it is NEVER scored
    # as a Tier-0 fitness regression; `infra_error_reason` makes it
    # distinct from a generic code-exception error in the iter row.
    infra_error: bool = False
    infra_error_reason: str | None = None
    # S-Y1.5 (#5): forensic per-iteration snapshot of the active pilot
    # card (target case-id lists + UC/phase hints + 16-char block_sha256)
    # and the `lessons.enabled` flag in force this iteration. Embedded in
    # the experiments.jsonl row + dry-run hypothesis.json so an auditor
    # never has to reconstruct which target card was live. Observation-only.
    pilot_snapshot: dict[str, Any] | None = None
    lessons_enabled: bool = True


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

        # --- 0.5. Pilot card (P0-C) — validated (rejects shadow case_id
        # collisions), the lazy per-skill phase/UC map, the lessons opt-out
        # flag, and the forensic snapshot. Computed once per iteration.
        surface_cfg = (config or {}).get("mutable_surface") or {}
        allowed_skill_files = surface_cfg.get("allowed_skill_files") or []
        pilot = _config_validator.validate_pilot_config(config, root)
        skill_phase_usecase_map = _config_validator.build_skill_phase_usecase_map(
            allowed_skill_files, root
        )
        result.lessons_enabled = bool(
            ((config or {}).get("lessons") or {}).get("enabled", True)
        )
        result.pilot_snapshot = _config_validator.build_pilot_snapshot(
            pilot, result.lessons_enabled
        )

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
        # P0-B: the last K candidate iterations' persisted eval results, so the
        # analyzer's failure taxonomy reflects candidate-introduced regressions,
        # not only the static baseline. Shadow-firewalled inside `analyze`.
        recent_candidate_results = _read_recent_candidate_results(config, root)
        taxonomy = _analyzer.analyze(
            baseline_summary,
            lessons_md,
            recent_iters_dicts,
            client=client,
            recent_candidate_results=recent_candidate_results,
            pilot=pilot,
            skill_phase_usecase_map=skill_phase_usecase_map,
        )

        # --- 2. Proposer.
        try:
            hypothesis = _proposer.propose(
                taxonomy,
                lessons_md,
                recent_iters_dicts,
                client=client,
                config=config,
                pilot=pilot,
                skill_phase_usecase_map=skill_phase_usecase_map,
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
        except eval_runner.EvalRunnerTimeoutError as e:
            # A suite timeout is infra degradation, not a fitness verdict.
            # Mark infra-error so it is never scored as a Tier-0 regression
            # (OQ-S65.7/8) — distinct from a generic code-exception error.
            result.infra_error = True
            result.infra_error_reason = f"eval_suite_timeout:{e.suite}"
            result.decision = "error"
            result.error = f"infra-error: {result.infra_error_reason}"
            _persist_iteration(
                result, config, root, dry_run=dry_run, applied=applied,
            )
            return _finalize(result, started)
        finally:
            # Restore old env var (no leakage into other tests / iters).
            if old_url is None:
                os.environ.pop("CSAGENT_BACKEND_URL", None)
            else:
                os.environ["CSAGENT_BACKEND_URL"] = old_url
        result.eval_results = {
            name: _safe_asdict(r) for name, r in suite_run_results.items()
        }

        # --- 7a. Persist per-iter eval traces (R-overnight-eval-traces-not-
        # persisted). eval_runner symlinks each suite to the volatile
        # eval_interactive/results/<ts>/ dir; copy the per-case traces into a
        # real file under the iter dir so §5.6 / §11 review survives applier
        # cleanup. Best-effort — never crashes the iteration.
        result.eval_traces_path = _persist_eval_traces(
            results_root, suite_run_results, iteration_id
        )

        # --- 7b. Infra-error detection (OQ-S65.7/8). A failed/empty suite or
        # pervasive LLM-deadline / service_degraded prevalence means the eval
        # evidence is infra-degraded, not a fitness signal. Mark infra-error
        # and skip Tier-0 scoring so degradation never masquerades as a
        # regression. Only transport/deadline/first-turn-abort signals trigger
        # this — a genuine Tier-0 FAIL still reaches the tier evaluator.
        is_infra, infra_reason = _assess_infra_error(suite_run_results, config)
        if is_infra:
            result.infra_error = True
            result.infra_error_reason = infra_reason
            result.decision = "error"
            result.error = f"infra-error: {infra_reason}"
            _persist_iteration(
                result, config, root, dry_run=dry_run, applied=applied,
            )
            return _finalize(result, started)

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


def _read_recent_candidate_results(
    config: dict[str, Any], root: Path
) -> list[dict[str, Any]]:
    """Load the last K candidate iterations' persisted `eval-results.json`
    payloads (P0-B).

    K comes from `meta_agent.recent_candidate_results_k` (default 3). The
    payloads are the raw per-suite results written by
    `_persist_eval_traces`; the shadow firewall is applied downstream in
    `analyzer.summarize_candidate_results`. Best-effort: missing files /
    rows without an `eval_traces_path` are skipped. Most-recent first.
    """
    meta_cfg = (config or {}).get("meta_agent") or {}
    try:
        k = int(meta_cfg.get("recent_candidate_results_k", 3))
    except (TypeError, ValueError):
        k = 3
    if k <= 0:
        return []

    paths = _resolve_paths(config, root)
    # Over-read a window then filter to the K most recent rows that
    # actually persisted eval traces (keep / discard iters that reached
    # eval; error / short-circuit iters have no traces).
    records = _experiments_log.read_recent(paths["experiments_log"], max(k * 8, 24))
    out: list[dict[str, Any]] = []
    for rec in reversed(records):
        traces_path = rec.get("eval_traces_path")
        if not traces_path:
            continue
        p = Path(traces_path)
        if not p.is_absolute():
            p = root / traces_path
        if not p.exists():
            continue
        try:
            with p.open("r", encoding="utf-8") as f:
                payload = json.load(f)
        except (OSError, json.JSONDecodeError):
            continue
        if isinstance(payload, dict):
            out.append(payload)
        if len(out) >= k:
            break
    return out


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
        "eval_traces_path": result.eval_traces_path,
        "infra_error": result.infra_error,
        "infra_error_reason": result.infra_error_reason,
        # S-Y1.5 (#5): forensic pilot snapshot + lessons-enabled flag.
        "pilot_snapshot": result.pilot_snapshot,
        "lessons_enabled": result.lessons_enabled,
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
        hyp_payload = _safe_asdict(result.hypothesis)
        if isinstance(hyp_payload, dict):
            # S-Y1.5 (#5): embed the forensic pilot snapshot + lessons flag
            # alongside the hypothesis so a dry-run audit reconstructs the
            # active target card without inference.
            hyp_payload["pilot_snapshot"] = result.pilot_snapshot
            hyp_payload["lessons_enabled"] = result.lessons_enabled
        with (runs_dir / "hypothesis.json").open("w", encoding="utf-8") as f:
            json.dump(hyp_payload, f, indent=2, default=str)
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


# --- S-Auto-11: eval trace persistence + infra-error detection -------

# Raw `escalation_reason` values the runtime coerces when it gives up on a
# transport/deadline failure (see eval_interactive hard_checks
# `_ESCALATION_REASON_FAMILY` "service_degraded" family +
# ChatController LlmDeadlineExceededException give-up). These are infra
# signals, NOT semantic escalations — `out_of_scope` / `tool_scope_blocked`
# are deliberately excluded because they are legitimate semantic outcomes.
_INFRA_ESCALATION_REASONS = frozenset({"service_degraded", "runtime_error_threshold"})

_INFRA_FAILURE_TAG_SUBSTRINGS = ("ReadTimeout", "Timeout", "Deadline", "service_degraded")


def _read_suite_results_json(sr: Any) -> dict[str, Any] | None:
    """Load a SuiteRunResult's results.json, or None if unreadable.

    Tolerates non-SuiteRunResult inputs (e.g. test MagicMocks): a
    `results_json` that is not a real str/Path yields None rather than
    raising.
    """
    results_json = getattr(sr, "results_json", None)
    if not isinstance(results_json, (str, Path)):
        return None
    rj = Path(results_json)
    if not rj.exists():
        return None
    try:
        with rj.open(encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return None


def _persist_eval_traces(
    results_root: Path,
    suite_run_results: dict[str, Any],
    iteration_id: str,
) -> str | None:
    """Copy each suite's per-case eval results (incl. per_turn_trace) into a
    real file so they survive the volatile eval_interactive/results/<ts>/
    symlink and applier cleanup.

    Writes ``<runs>/<id>/eval-results.json`` (sibling of the ``eval/``
    symlink dir). Best-effort: any failure returns None without raising, so
    trace persistence never crashes an iteration.
    """
    try:
        out_path = Path(results_root).parent / "eval-results.json"
        payload: dict[str, Any] = {
            "iteration_id": iteration_id,
            "generated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "suites": {},
        }
        for name, sr in (suite_run_results or {}).items():
            results_json = getattr(sr, "results_json", None)
            exit_code = getattr(sr, "exit_code", None)
            error_tail = getattr(sr, "error_tail", None)
            data = _read_suite_results_json(sr)
            entry: dict[str, Any] = {
                "exit_code": exit_code if isinstance(exit_code, int) else None,
                "error_tail": error_tail if isinstance(error_tail, str) else None,
                "results_json": (
                    str(results_json) if isinstance(results_json, (str, Path)) else None
                ),
                "source_dir": None,
                "results": data,
                "missing": data is None,
            }
            if isinstance(results_json, (str, Path)):
                try:
                    entry["source_dir"] = str(Path(results_json).resolve().parent)
                except Exception:
                    pass
            payload["suites"][name] = entry
        out_path.parent.mkdir(parents=True, exist_ok=True)
        with out_path.open("w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2, default=str)
        return str(out_path)
    except Exception:
        return None


def _case_is_infra_degraded(case: dict[str, Any]) -> bool:
    """True if a single case shows a transport / deadline / first-turn-abort
    signal.

    A generic fitness FAIL is deliberately NOT an infra signal — it must
    still reach the tier evaluator as a real regression (negative control
    for the OQ-S65.7/8 masquerade fence).
    """
    if not isinstance(case, dict):
        return False
    status = case.get("status")
    if status == "ERROR":
        # Executor-level error (session-create failure, ReadTimeout, etc.).
        return True
    if status == "CONTRACT_VIOLATION":
        cv = case.get("contract_violation") or {}
        # active_use_case missing after a ~0-turn session is the first-turn
        # LLM-deadline / give-up masquerade (S-Auto-11 cs015/fg5q finding).
        if isinstance(cv, dict) and cv.get("field") == "active_use_case":
            return True
    if case.get("escalation_reason") in _INFRA_ESCALATION_REASONS:
        return True
    for tag in case.get("failure_tags") or []:
        tag_s = str(tag)
        if any(sub in tag_s for sub in _INFRA_FAILURE_TAG_SUBSTRINGS):
            return True
    return False


def _assess_infra_error(
    suite_run_results: dict[str, Any],
    config: dict[str, Any],
) -> tuple[bool, str | None]:
    """Detect whether the eval evidence is infra-degraded rather than a
    fitness signal (OQ-S65.7/8).

    Two independent triggers:
      1. A failed / empty suite — a real SuiteRunResult with a non-zero
         exit code OR a missing results.json (the eval produced no
         evidence).
      2. Pervasive LLM-deadline / service_degraded prevalence across the
         cases that did run (>= ``fitness.infra_error_degraded_fraction``,
         default 0.5).

    Returns ``(is_infra_error, reason)``. Inputs that are not real
    SuiteRunResults (e.g. test MagicMocks, or an empty dict) yield
    ``(False, None)`` so the normal fitness path is preserved.
    """
    fitness_cfg = (config or {}).get("fitness") or {}
    try:
        threshold = float(fitness_cfg.get("infra_error_degraded_fraction", 0.5))
    except (TypeError, ValueError):
        threshold = 0.5

    failed_suites: list[str] = []
    total_cases = 0
    degraded_cases = 0

    for name, sr in (suite_run_results or {}).items():
        exit_code = getattr(sr, "exit_code", None)
        is_real_suite = isinstance(exit_code, int)
        if is_real_suite and exit_code != 0:
            failed_suites.append(f"{name}:exit={exit_code}")
            continue
        data = _read_suite_results_json(sr)
        if data is None:
            if is_real_suite:
                # A real suite that exited 0 but wrote no readable
                # results.json = empty eval evidence.
                failed_suites.append(f"{name}:missing_results_json")
            # Non-real suite (mock / unexpected shape): contribute nothing.
            continue
        for case in data.get("case_results") or []:
            total_cases += 1
            if _case_is_infra_degraded(case):
                degraded_cases += 1

    if failed_suites:
        return True, "eval_suite_failed:" + ",".join(failed_suites)

    if total_cases > 0:
        fraction = degraded_cases / total_cases
        if fraction >= threshold:
            return True, (
                f"infra_degradation_prevalence:{degraded_cases}/{total_cases}"
                f"={fraction:.2f}>=threshold:{threshold:.2f}"
            )

    return False, None
