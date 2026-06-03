"""Post-eval anti-gaming checks.

S-Auto-4 deliverable. Seven structural checks that run AFTER the
tier evaluator produces its verdict but BEFORE the iteration record
is appended to the experiments log. All checks are
**observation-only in v1** — flags are persisted into the iteration
record and surfaced by `autoloop audit`, but the loop verdict
(`keep` / `discard`) is NOT altered by gaming flags. Promotion of
any ERROR-severity check to a gating role is a M-Auto-1B decision
after calibration evidence accumulates.

The seven checks (per `docs/proposals/autoloop_design.md` §8 plus
the NEW `tier2_measurement_contract_change_attempt`):

1. `anomalous_metric_movement` — Layer 3 improvement out of
   proportion to diff size. WARN.
2. `identical_eval_traces_across_different_hypotheses` — distinct
   fingerprints produce byte-identical results.json. ERROR
   (suggests eval did not re-run).
3. `suspect_baseline_manipulation` — `config.fitness.baseline_dir`
   changed since last `main` commit without a `notes` entry. WARN.
4. `eval_time_gaming_via_timeout_skip` — skipped / timed-out case
   rate ≥ 2x baseline. WARN.
5. `scoring_code_drift` — autoloop scoring code SHA changed since
   the configured baseline SHA. ERROR if baseline SHA known and
   diverged; WARN `baseline_missing` when baseline SHA is unset
   (D3 — never guess or silently use the current SHA as baseline).
6. `shadow_set_leakage` — shadow leak signatures appear in any
   non-audit surface (proposer prompt context, iteration_record
   fields). ERROR. Detector is config-driven (D3 — never reads
   `eval_interactive/case_specs_shadow/` content).
7. `tier2_measurement_contract_change_attempt` — a critical_step
   that previously FAILed now scores N/A or skipped. ERROR.
"""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable, Literal


_REPO_ROOT = Path(__file__).resolve().parents[3]

# Files whose SHA is rolled into `scoring_code_drift`. Order matters
# only for determinism of the resulting hash.
# S-Auto-16: widened to FIVE — `aggregate.py` produces the
# `majority_passed` per-case verdict the fitness gate consumes, so it MUST
# live inside the scoring-code drift coverage (leaving it out would be a
# scoring-code drift hole). Append-only (aggregate.py last) to keep the
# diff localized; the hash recomputes regardless of order, but a stable
# order documents intent.
_SCORING_CODE_FILES = (
    "autoloop/autoloop/scoring/tier_evaluator.py",
    "autoloop/autoloop/scoring/eval_runner.py",
    "autoloop/autoloop/scoring/baseline_loader.py",
    "autoloop/autoloop/scoring/gaming.py",
    "autoloop/autoloop/scoring/aggregate.py",
)


@dataclass
class GamingFlag:
    """One observation-only anti-gaming flag.

    The flag carries the rule id, severity, a short detail string,
    and a rule-specific `evidence` dict for audit. The evidence dict
    NEVER carries raw shadow case content — D3 invariant enforced
    by the per-rule code paths below.
    """

    rule_id: str
    severity: Literal["WARN", "ERROR"]
    detail: str
    evidence: dict[str, Any] = field(default_factory=dict)


def detect(
    iteration_record: dict[str, Any],
    recent_records: Iterable[dict[str, Any]],
    eval_artefacts: dict[str, Any],
    *,
    config: dict[str, Any] | None = None,
) -> list[GamingFlag]:
    """Run all seven anti-gaming checks against the current iteration.

    Arguments:
        iteration_record: the about-to-be-persisted iteration row
            (hypothesis, verdict, sandbox_verdict, etc.).
        recent_records: prior iteration rows (any iterable; the
            checks materialize them lazily).
        eval_artefacts: dict carrying `results_root` (Path to per-
            iteration eval dir), `verdict` (LexicographicVerdict),
            `baseline_dir` (Path | None), and `current_config_yaml`
            (raw config dict). Caller may pass additional fields;
            unknown keys are tolerated.
        config: the autoloop config (gaming behaviour is config-driven).
    """
    if not (config or {}).get("gaming", {}).get("enabled", True):
        return []

    flags: list[GamingFlag] = []
    recent_list = list(recent_records or [])

    flags.extend(_check_anomalous_metric_movement(
        iteration_record, config=config,
    ))
    flags.extend(_check_identical_eval_traces(
        iteration_record, recent_list, eval_artefacts,
    ))
    flags.extend(_check_suspect_baseline_manipulation(
        iteration_record, eval_artefacts, config=config,
    ))
    flags.extend(_check_eval_time_gaming_via_timeout_skip(
        eval_artefacts, config=config,
    ))
    flags.extend(_check_scoring_code_drift(config=config))
    flags.extend(_check_shadow_set_leakage(
        iteration_record, recent_list, eval_artefacts, config=config,
    ))
    flags.extend(_check_tier2_measurement_contract_change_attempt(
        eval_artefacts,
    ))
    return flags


# ---------------------------------------------------------------------
# Check 1 — anomalous_metric_movement
# ---------------------------------------------------------------------


def _check_anomalous_metric_movement(
    iteration_record: dict[str, Any],
    *,
    config: dict[str, Any] | None,
) -> list[GamingFlag]:
    threshold = float(
        ((config or {}).get("gaming") or {}).get(
            "anomalous_metric_movement_threshold", 0.05
        )
    )
    verdict = iteration_record.get("verdict") or {}
    tier_breakdown = verdict.get("tier_breakdown") or {}
    layer3 = tier_breakdown.get("improvement_threshold") or {}
    bc_delta = int(layer3.get("bad_cases_passed_delta") or 0)
    ao_delta = int(layer3.get("anchor_outcome_passed_delta") or 0)
    tier2_red = int(layer3.get("tier2_mandatory_failure_reduction") or 0)
    improvement_count = max(bc_delta, ao_delta, tier2_red, 0)
    if improvement_count <= 0:
        return []

    hyp = iteration_record.get("hypothesis") or {}
    after_value = hyp.get("after_value") or ""
    before_value = hyp.get("before_value") or ""
    diff_chars = abs(len(after_value) - len(before_value)) or len(after_value)
    if diff_chars <= 0:
        diff_chars = 1
    ratio = improvement_count / diff_chars
    if ratio <= threshold:
        return []
    return [GamingFlag(
        rule_id="anomalous_metric_movement",
        severity="WARN",
        detail=(
            f"improvement_count={improvement_count} vs diff_chars="
            f"{diff_chars} ratio={ratio:.4f} > threshold={threshold:.4f}"
        ),
        evidence={
            "improvement_count": improvement_count,
            "diff_chars": diff_chars,
            "ratio": ratio,
            "threshold": threshold,
        },
    )]


# ---------------------------------------------------------------------
# Check 2 — identical_eval_traces_across_different_hypotheses
# ---------------------------------------------------------------------


def _check_identical_eval_traces(
    iteration_record: dict[str, Any],
    recent_records: list[dict[str, Any]],
    eval_artefacts: dict[str, Any],
) -> list[GamingFlag]:
    cur_hash = _hash_results_dir(eval_artefacts.get("results_root"))
    if cur_hash is None:
        return []
    cur_fingerprint = (
        (iteration_record.get("hypothesis") or {}).get("fingerprint") or ""
    )
    matches: list[dict[str, str]] = []
    for prev in recent_records:
        prev_hash = prev.get("_results_hash") or prev.get(
            "results_dir_hash"
        )
        if not prev_hash:
            continue
        prev_fingerprint = (
            (prev.get("hypothesis") or {}).get("fingerprint") or ""
        )
        if not prev_fingerprint or prev_fingerprint == cur_fingerprint:
            continue
        if prev_hash == cur_hash:
            matches.append({
                "iteration_id": prev.get("iteration_id", "<unknown>"),
                "fingerprint": prev_fingerprint,
            })
    if not matches:
        return []
    return [GamingFlag(
        rule_id="identical_eval_traces_across_different_hypotheses",
        severity="ERROR",
        detail=(
            f"current results hash {cur_hash[:12]} matches "
            f"{len(matches)} prior iteration(s) with distinct fingerprints"
        ),
        evidence={
            "current_hash": cur_hash,
            "current_fingerprint": cur_fingerprint,
            "matches": matches,
        },
    )]


def _hash_results_dir(results_root: Any) -> str | None:
    if results_root is None:
        return None
    root = Path(results_root)
    if not root.exists():
        return None
    h = hashlib.sha256()
    saw_any = False
    for suite_dir in sorted(p for p in root.iterdir() if p.is_dir()):
        rj = suite_dir / "results.json"
        if not rj.exists():
            continue
        try:
            h.update(suite_dir.name.encode("utf-8"))
            h.update(b"\0")
            h.update(rj.read_bytes())
            h.update(b"\0")
            saw_any = True
        except OSError:
            continue
    return h.hexdigest() if saw_any else None


# ---------------------------------------------------------------------
# Check 3 — suspect_baseline_manipulation
# ---------------------------------------------------------------------


def _check_suspect_baseline_manipulation(
    iteration_record: dict[str, Any],
    eval_artefacts: dict[str, Any],
    *,
    config: dict[str, Any] | None,
) -> list[GamingFlag]:
    current_value = (
        (config or {}).get("fitness", {}) or {}
    ).get("baseline_dir")
    if not current_value:
        return []
    try:
        log = subprocess.run(
            [
                "git", "log", "--format=%H", "main",
                "--", "autoloop/config.yaml",
            ],
            cwd=str(_REPO_ROOT),
            capture_output=True,
            text=True,
            timeout=10,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        return [GamingFlag(
            rule_id="suspect_baseline_manipulation.git_lookup_failed",
            severity="WARN",
            detail=f"git log lookup failed: {exc.__class__.__name__}",
            evidence={"error": str(exc)},
        )]
    if log.returncode != 0:
        return [GamingFlag(
            rule_id="suspect_baseline_manipulation.git_lookup_failed",
            severity="WARN",
            detail="git log on main for autoloop/config.yaml returned nonzero",
            evidence={"stderr": (log.stderr or "")[-200:]},
        )]
    shas = [s for s in (log.stdout or "").splitlines() if s.strip()]
    if not shas:
        return [GamingFlag(
            rule_id="suspect_baseline_manipulation.git_lookup_failed",
            severity="WARN",
            detail="git log produced no commits for autoloop/config.yaml",
            evidence={},
        )]
    last_main_sha = shas[0]
    try:
        show = subprocess.run(
            ["git", "show", f"{last_main_sha}:autoloop/config.yaml"],
            cwd=str(_REPO_ROOT),
            capture_output=True,
            text=True,
            timeout=10,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        return [GamingFlag(
            rule_id="suspect_baseline_manipulation.git_lookup_failed",
            severity="WARN",
            detail=f"git show failed: {exc.__class__.__name__}",
            evidence={"error": str(exc)},
        )]
    if show.returncode != 0:
        return [GamingFlag(
            rule_id="suspect_baseline_manipulation.git_lookup_failed",
            severity="WARN",
            detail=f"git show {last_main_sha}:autoloop/config.yaml failed",
            evidence={"stderr": (show.stderr or "")[-200:]},
        )]
    prev_value = _extract_baseline_dir_value(show.stdout)
    if prev_value is None or prev_value == current_value:
        return []
    if _baseline_change_recorded_in_notes(iteration_record, prev_value, current_value):
        return []
    return [GamingFlag(
        rule_id="suspect_baseline_manipulation",
        severity="WARN",
        detail=(
            f"baseline_dir changed from {prev_value!r} to {current_value!r}"
            f" since {last_main_sha[:12]} on main with no notes record"
        ),
        evidence={
            "previous_baseline_dir": prev_value,
            "current_baseline_dir": current_value,
            "last_main_sha": last_main_sha,
        },
    )]


_BASELINE_DIR_RE = re.compile(
    r"^\s*baseline_dir\s*:\s*(\S+)\s*$", re.MULTILINE
)


def _extract_baseline_dir_value(yaml_text: str) -> str | None:
    m = _BASELINE_DIR_RE.search(yaml_text or "")
    return m.group(1).strip() if m else None


def _baseline_change_recorded_in_notes(
    iteration_record: dict[str, Any],
    prev_value: str,
    current_value: str,
) -> bool:
    notes_blob = json.dumps(iteration_record, default=str)
    return prev_value in notes_blob and current_value in notes_blob and "baseline_dir" in notes_blob


# ---------------------------------------------------------------------
# Check 4 — eval_time_gaming_via_timeout_skip
# ---------------------------------------------------------------------


def _check_eval_time_gaming_via_timeout_skip(
    eval_artefacts: dict[str, Any],
    *,
    config: dict[str, Any] | None,
) -> list[GamingFlag]:
    cur_rates = _suite_timeout_skip_rates(eval_artefacts.get("results_root"))
    base_rates = _suite_timeout_skip_rates(eval_artefacts.get("baseline_dir"))
    if not cur_rates:
        return []
    flags: list[GamingFlag] = []
    for suite_name, cur_rate in cur_rates.items():
        base_rate = base_rates.get(suite_name)
        if base_rate is None:
            # Fall back to a sane absolute floor when no baseline:
            # any skip > 10% is suspicious enough to WARN on.
            if cur_rate > 0.10:
                flags.append(GamingFlag(
                    rule_id="eval_time_gaming_via_timeout_skip",
                    severity="WARN",
                    detail=(
                        f"suite {suite_name!r} skip/timeout rate "
                        f"{cur_rate:.2%} above 10% absolute floor "
                        f"(no baseline rate available)"
                    ),
                    evidence={
                        "suite": suite_name,
                        "current_rate": cur_rate,
                        "baseline_rate": None,
                    },
                ))
            continue
        if base_rate <= 0:
            if cur_rate > 0.10:
                flags.append(GamingFlag(
                    rule_id="eval_time_gaming_via_timeout_skip",
                    severity="WARN",
                    detail=(
                        f"suite {suite_name!r} skip/timeout rate "
                        f"{cur_rate:.2%}; baseline rate 0%"
                    ),
                    evidence={
                        "suite": suite_name,
                        "current_rate": cur_rate,
                        "baseline_rate": base_rate,
                    },
                ))
            continue
        if cur_rate >= 2.0 * base_rate:
            flags.append(GamingFlag(
                rule_id="eval_time_gaming_via_timeout_skip",
                severity="WARN",
                detail=(
                    f"suite {suite_name!r} skip/timeout rate "
                    f"{cur_rate:.2%} >= 2x baseline {base_rate:.2%}"
                ),
                evidence={
                    "suite": suite_name,
                    "current_rate": cur_rate,
                    "baseline_rate": base_rate,
                },
            ))
    return flags


def _suite_timeout_skip_rates(root: Any) -> dict[str, float]:
    if root is None:
        return {}
    root_path = Path(root)
    if not root_path.exists() or not root_path.is_dir():
        return {}
    rates: dict[str, float] = {}
    for suite_dir in (p for p in root_path.iterdir() if p.is_dir()):
        rj = suite_dir / "results.json"
        if not rj.exists():
            continue
        try:
            with rj.open("r", encoding="utf-8") as f:
                data = json.load(f)
        except (OSError, json.JSONDecodeError):
            continue
        cases = data.get("case_results") or []
        total = len(cases)
        if total <= 0:
            continue
        bad = 0
        for c in cases:
            outcome = (c.get("terminal_outcome") or "").lower()
            if outcome in {"skipped", "skip", "timeout", "timed_out", "error"}:
                bad += 1
        rates[suite_dir.name] = bad / total
    return rates


# ---------------------------------------------------------------------
# Check 5 — scoring_code_drift
# ---------------------------------------------------------------------


def _check_scoring_code_drift(
    *,
    config: dict[str, Any] | None,
) -> list[GamingFlag]:
    baseline_sha = (
        ((config or {}).get("fitness") or {}).get("scoring_code_baseline_sha")
    )
    current_sha = _compute_scoring_code_sha()
    if baseline_sha in (None, "", "null"):
        return [GamingFlag(
            rule_id="scoring_code_drift.baseline_missing",
            severity="WARN",
            detail=(
                "scoring_code_baseline_sha not set in config; cannot "
                "verify drift"
            ),
            evidence={
                "current_sha": current_sha,
                "baseline_sha": None,
            },
        )]
    if current_sha != baseline_sha:
        return [GamingFlag(
            rule_id="scoring_code_drift.sha_changed",
            severity="ERROR",
            detail=(
                f"scoring code SHA {current_sha[:12]} differs from "
                f"baseline SHA {baseline_sha[:12]}"
            ),
            evidence={
                "current_sha": current_sha,
                "baseline_sha": baseline_sha,
                "files_hashed": list(_SCORING_CODE_FILES),
            },
        )]
    return []


def _compute_scoring_code_sha() -> str:
    h = hashlib.sha256()
    for rel in _SCORING_CODE_FILES:
        abs_path = _REPO_ROOT / rel
        h.update(rel.encode("utf-8"))
        h.update(b"\0")
        try:
            h.update(abs_path.read_bytes())
        except OSError:
            h.update(b"<missing>")
        h.update(b"\0")
    return h.hexdigest()


# ---------------------------------------------------------------------
# Check 6 — shadow_set_leakage
# ---------------------------------------------------------------------


def _check_shadow_set_leakage(
    iteration_record: dict[str, Any],
    recent_records: list[dict[str, Any]],
    eval_artefacts: dict[str, Any],
    *,
    config: dict[str, Any] | None,
) -> list[GamingFlag]:
    signatures = (
        ((config or {}).get("gaming") or {}).get("shadow_leak_signatures")
    )
    if not signatures:
        return []
    surfaces: dict[str, str] = {
        "iteration_record": json.dumps(iteration_record, default=str),
    }
    proposer_prompt = eval_artefacts.get("proposer_prompt")
    if proposer_prompt:
        surfaces["proposer_prompt"] = str(proposer_prompt)
    for i, prev in enumerate(recent_records[:5]):
        surfaces[f"recent_record_{i}"] = json.dumps(prev, default=str)

    flags: list[GamingFlag] = []
    for surface_name, payload in surfaces.items():
        for sig in signatures:
            if not isinstance(sig, str) or not sig:
                continue
            if sig in payload:
                flags.append(GamingFlag(
                    rule_id="shadow_set_leakage",
                    severity="ERROR",
                    detail=(
                        f"shadow leak signature {sig!r} found on "
                        f"non-audit surface {surface_name!r}"
                    ),
                    evidence={
                        "surface": surface_name,
                        "signature": sig,
                    },
                ))
                break  # one flag per surface is sufficient.
    return flags


# ---------------------------------------------------------------------
# Check 7 — tier2_measurement_contract_change_attempt
# ---------------------------------------------------------------------


def _check_tier2_measurement_contract_change_attempt(
    eval_artefacts: dict[str, Any],
) -> list[GamingFlag]:
    current_steps = _load_tier2_per_step(eval_artefacts.get("results_root"))
    baseline_steps = _load_tier2_per_step(eval_artefacts.get("baseline_dir"))
    if not current_steps or not baseline_steps:
        return []
    flags: list[GamingFlag] = []
    for (suite, step_id), base_outcome in baseline_steps.items():
        cur_outcome = current_steps.get((suite, step_id))
        if cur_outcome is None:
            continue
        if base_outcome == "FAIL" and cur_outcome in {"N/A", "skipped", "NA"}:
            flags.append(GamingFlag(
                rule_id="tier2_measurement_contract_change_attempt",
                severity="ERROR",
                detail=(
                    f"critical_step {step_id!r} in suite {suite!r} flipped "
                    f"from baseline FAIL to current {cur_outcome!r}; "
                    f"measurement contract may have changed"
                ),
                evidence={
                    "suite": suite,
                    "step_id": step_id,
                    "baseline_outcome": base_outcome,
                    "current_outcome": cur_outcome,
                },
            ))
    return flags


def _load_tier2_per_step(root: Any) -> dict[tuple[str, str], str]:
    """Read tier2_result.per_step from every suite's results.json.

    Returns {(suite_name, step_id): outcome}. Missing per-step data
    is silently skipped — this check is best-effort across the
    possible shapes eval-interactive may produce.
    """
    if root is None:
        return {}
    root_path = Path(root)
    if not root_path.exists() or not root_path.is_dir():
        return {}
    out: dict[tuple[str, str], str] = {}
    for suite_dir in (p for p in root_path.iterdir() if p.is_dir()):
        rj = suite_dir / "results.json"
        if not rj.exists():
            continue
        try:
            with rj.open("r", encoding="utf-8") as f:
                data = json.load(f)
        except (OSError, json.JSONDecodeError):
            continue
        cases = data.get("case_results") or []
        for case in cases:
            tier2 = case.get("tier2_result") or {}
            for step in (tier2.get("per_step") or []):
                step_id = step.get("step_id") or step.get("id")
                outcome = step.get("outcome")
                if step_id and isinstance(outcome, str):
                    out[(suite_dir.name, str(step_id))] = outcome
    return out
