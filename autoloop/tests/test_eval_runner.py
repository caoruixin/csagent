"""Tests for `autoloop.scoring.eval_runner`.

All subprocess invocations are mocked. No live `eval-interactive run`
is invoked from these tests. Hard-fence enforcement is verified by
grepping the eval_runner source for forbidden tokens.
"""

from __future__ import annotations

import os
import re
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from autoloop.scoring import (
    EvalRunnerTimeoutError,
    SuiteRunResult,
    SuiteRunSpec,
    run_suite,
    run_v1_fitness_suite,
)
from autoloop.scoring import eval_runner as _eval_runner_module
from autoloop.scoring.eval_runner import EvalRunnerConfigError


DEFAULT_CONFIG = {
    "fitness": {
        "suites": [
            {
                "name": "bad_cases",
                "path": "eval_interactive/case_specs/bad_cases/",
                "parallel": 1,
            },
            {
                "name": "anchor_outcome",
                "path": "eval_interactive/case_specs/anchor_outcome/",
                "parallel": 4,
            },
            {
                "name": "shadow",
                "path": "eval_interactive/case_specs_shadow/",
                "parallel": 4,
            },
        ],
        "eval_suite_timeout_seconds": 1800,
        "parallel_suites": False,
    }
}


# --- SuiteRunSpec basics ---------------------------------------------


def test_suite_run_spec_v1_defaults_per_suite():
    """v1 fitness has 3 suites with specific parallel defaults:
    bad_cases=1 (flakiness mitigation), others=4.
    """
    suites = DEFAULT_CONFIG["fitness"]["suites"]
    by_name = {s["name"]: s for s in suites}
    assert by_name["bad_cases"]["parallel"] == 1
    assert by_name["anchor_outcome"]["parallel"] == 4
    assert by_name["shadow"]["parallel"] == 4


# --- run_suite happy path --------------------------------------------


def test_run_suite_mocked_success(tmp_path: Path):
    """Subprocess returns 0 → SuiteRunResult.exit_code==0, results_json
    points to <results_root>/<suite>/results.json.
    """
    results_root = tmp_path / "results"
    spec = SuiteRunSpec(
        name="bad_cases",
        path=Path("eval_interactive/case_specs/bad_cases/"),
        parallel=1,
    )

    fake_proc = MagicMock()
    fake_proc.returncode = 0
    fake_proc.stderr = ""

    with patch("autoloop.scoring.eval_runner.subprocess.run", return_value=fake_proc) as mock_run:
        result = run_suite(
            spec,
            results_root=results_root,
            config=DEFAULT_CONFIG,
            timeout_seconds=10,
        )

    assert result.suite_name == "bad_cases"
    assert result.exit_code == 0
    assert result.results_json == results_root / "bad_cases" / "results.json"
    assert result.results_dir == results_root / "bad_cases"
    assert result.error_tail is None

    # Confirm subprocess was invoked with the expected CLI shape.
    # S-Auto-7 Blocker B fix path (b): the `--output-dir` flag was
    # REMOVED — eval-interactive auto-timestamps and eval_runner
    # symlinks the result. Asserting absence prevents regression.
    call_args = mock_run.call_args
    cmd = call_args[0][0]
    assert cmd[:4] == ["uv", "run", "eval-interactive", "run"]
    assert "--path" in cmd
    assert "--parallel" in cmd
    assert "--output-dir" not in cmd


def test_run_suite_timeout_raises(tmp_path: Path):
    """subprocess.TimeoutExpired → EvalRunnerTimeoutError with suite + elapsed."""
    spec = SuiteRunSpec(
        name="anchor_outcome",
        path=Path("x"),
        parallel=4,
    )

    def _raise_timeout(*args, **kwargs):
        raise subprocess.TimeoutExpired(cmd=args[0], timeout=5)

    with patch("autoloop.scoring.eval_runner.subprocess.run", side_effect=_raise_timeout):
        with pytest.raises(EvalRunnerTimeoutError) as exc:
            run_suite(
                spec,
                results_root=tmp_path,
                config=DEFAULT_CONFIG,
                timeout_seconds=5,
            )
    assert exc.value.suite == "anchor_outcome"
    assert exc.value.elapsed >= 0.0


def test_run_suite_non_zero_exit_captures_stderr_tail(tmp_path: Path):
    """Non-zero exit + stderr → error_tail contains last 50 lines."""
    spec = SuiteRunSpec(
        name="shadow",
        path=Path("x"),
        parallel=4,
    )

    long_stderr = "\n".join(f"err line {i}" for i in range(100))
    fake_proc = MagicMock()
    fake_proc.returncode = 2
    fake_proc.stderr = long_stderr

    with patch("autoloop.scoring.eval_runner.subprocess.run", return_value=fake_proc):
        result = run_suite(
            spec,
            results_root=tmp_path,
            config=DEFAULT_CONFIG,
            timeout_seconds=10,
        )

    assert result.exit_code == 2
    assert result.error_tail is not None
    tail_lines = result.error_tail.splitlines()
    assert len(tail_lines) == 50
    assert tail_lines[0] == "err line 50"
    assert tail_lines[-1] == "err line 99"


# --- run_v1_fitness_suite --------------------------------------------


def test_run_v1_fitness_suite_runs_all_three_sequentially(tmp_path: Path):
    """3 SuiteRunResult keyed by suite_name; sequential when
    parallel_suites=False (the v1 default).
    """
    call_order: list[str] = []

    def _fake_run(cmd, **kwargs):
        # S-Auto-7 fix path (b): --output-dir was removed; derive the
        # suite from --path's basename instead. bad_cases / anchor_outcome
        # / shadow are uniquely identifiable from the case_specs path.
        path_idx = cmd.index("--path") + 1
        path_str = cmd[path_idx]
        if "bad_cases" in path_str:
            suite_name = "bad_cases"
        elif "anchor_outcome" in path_str:
            suite_name = "anchor_outcome"
        elif "shadow" in path_str:
            suite_name = "shadow"
        else:
            suite_name = Path(path_str).name
        call_order.append(suite_name)
        m = MagicMock()
        m.returncode = 0
        m.stderr = ""
        return m

    with patch("autoloop.scoring.eval_runner.subprocess.run", side_effect=_fake_run):
        results = run_v1_fitness_suite(
            results_root=tmp_path,
            config=DEFAULT_CONFIG,
        )

    assert set(results.keys()) == {"bad_cases", "anchor_outcome", "shadow"}
    assert call_order == ["bad_cases", "anchor_outcome", "shadow"]
    assert all(isinstance(r, SuiteRunResult) for r in results.values())
    assert all(r.exit_code == 0 for r in results.values())


def test_run_v1_fitness_suite_parallel_suites_true_raises(tmp_path: Path):
    """parallel_suites=True is reserved for M-Auto-2; v1 raises."""
    cfg = {
        "fitness": {
            **DEFAULT_CONFIG["fitness"],
            "parallel_suites": True,
        }
    }
    with pytest.raises(NotImplementedError):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)


# --- S-Auto-7 Blocker B fix path (b) tests ---------------------------


def test_eval_runner_removes_output_dir_arg(tmp_path: Path):
    """S-Auto-7 fix path (b): the subprocess command must NOT contain
    `--output-dir`. eval-interactive's CLI does not support that flag;
    eval_runner now adapts to the auto-timestamp convention.
    """
    spec = SuiteRunSpec(name="bad_cases", path=Path("x"), parallel=1)
    fake_proc = MagicMock()
    fake_proc.returncode = 0
    fake_proc.stderr = ""

    with patch(
        "autoloop.scoring.eval_runner.subprocess.run", return_value=fake_proc
    ) as mock_run:
        run_suite(
            spec,
            results_root=tmp_path,
            config=DEFAULT_CONFIG,
            timeout_seconds=10,
        )

    cmd = mock_run.call_args[0][0]
    assert "--output-dir" not in cmd
    # The cmd should END with --parallel <N>; no trailing positional args.
    assert cmd[-2] == "--parallel"
    assert cmd[-1] == str(spec.parallel)


def test_eval_runner_locates_auto_timestamped_output_via_set_diff(
    tmp_path: Path, monkeypatch
):
    """Post-subprocess, eval_runner identifies the new
    eval_interactive/results/<ts>/ via before/after set-diff.
    """
    # Build a synthetic repo root with eval_interactive/results/.
    fake_repo = tmp_path / "repo"
    ei_results = fake_repo / "eval_interactive" / "results"
    ei_results.mkdir(parents=True)
    # Pre-existing old dir that must be IGNORED.
    (ei_results / "20260101-000000").mkdir()
    (ei_results / "20260101-000000" / "results.json").write_text("{}")
    monkeypatch.setattr(
        "autoloop.scoring.eval_runner._REPO_ROOT", fake_repo
    )

    new_ts = "20260529-120000"

    def _fake_run(cmd, **kwargs):
        # Simulate eval-interactive creating its new run dir.
        new_dir = ei_results / new_ts
        new_dir.mkdir()
        (new_dir / "results.json").write_text('{"case_results": []}')
        m = MagicMock()
        m.returncode = 0
        m.stderr = ""
        return m

    spec = SuiteRunSpec(name="bad_cases", path=Path("x"), parallel=1)
    results_root = tmp_path / "iter_results"

    with patch(
        "autoloop.scoring.eval_runner.subprocess.run", side_effect=_fake_run
    ):
        result = run_suite(
            spec,
            results_root=results_root,
            config=DEFAULT_CONFIG,
            timeout_seconds=10,
        )

    suite_link = results_root / "bad_cases"
    assert suite_link.is_symlink()
    # Symlink target is the NEW dir, not the pre-existing one.
    assert suite_link.resolve() == (ei_results / new_ts).resolve()
    # Downstream consumers see results.json through the symlink.
    assert result.results_json.exists()
    assert result.results_json.read_text() == '{"case_results": []}'


def test_eval_runner_returns_suiterunresult_with_correct_paths(
    tmp_path: Path,
):
    """SuiteRunResult preserves the historical <results_root>/<suite>/
    contract (the symlink path) for downstream baseline_loader,
    tier_evaluator, and gaming consumers.
    """
    spec = SuiteRunSpec(name="anchor_outcome", path=Path("x"), parallel=4)
    fake_proc = MagicMock()
    fake_proc.returncode = 0
    fake_proc.stderr = ""
    results_root = tmp_path / "iter_results"

    with patch(
        "autoloop.scoring.eval_runner.subprocess.run", return_value=fake_proc
    ):
        result = run_suite(
            spec,
            results_root=results_root,
            config=DEFAULT_CONFIG,
            timeout_seconds=10,
        )

    assert result.results_dir == results_root / "anchor_outcome"
    assert result.results_json == results_root / "anchor_outcome" / "results.json"
    assert isinstance(result, SuiteRunResult)


def test_eval_runner_no_new_dir_leaves_results_path_absent(
    tmp_path: Path, monkeypatch
):
    """If the subprocess does not create a new run dir (e.g. fails
    before writing), the symlink is not created and
    results_json.exists() returns False so callers can detect.
    """
    fake_repo = tmp_path / "repo"
    (fake_repo / "eval_interactive" / "results").mkdir(parents=True)
    monkeypatch.setattr(
        "autoloop.scoring.eval_runner._REPO_ROOT", fake_repo
    )

    fake_proc = MagicMock()
    fake_proc.returncode = 1
    fake_proc.stderr = "boom"
    results_root = tmp_path / "iter_results"
    spec = SuiteRunSpec(name="shadow", path=Path("x"), parallel=4)

    with patch(
        "autoloop.scoring.eval_runner.subprocess.run", return_value=fake_proc
    ):
        result = run_suite(
            spec,
            results_root=results_root,
            config=DEFAULT_CONFIG,
            timeout_seconds=10,
        )

    assert result.exit_code == 1
    suite_link = results_root / "shadow"
    assert not suite_link.exists()
    assert not result.results_json.exists()


def test_locate_new_results_dir_picks_newest_by_mtime_on_multi(
    tmp_path: Path,
):
    """When multiple new directories appear (defensive against
    concurrent runs), `_locate_new_results_dir` returns the newest by
    mtime.
    """
    from autoloop.scoring.eval_runner import _locate_new_results_dir
    import time as _time

    ei_results = tmp_path / "results"
    ei_results.mkdir()
    before = {p for p in ei_results.iterdir() if p.is_dir()}

    older = ei_results / "20260529-100000"
    older.mkdir()
    _time.sleep(0.05)
    newer = ei_results / "20260529-110000"
    newer.mkdir()

    found = _locate_new_results_dir(ei_results, before)
    assert found == newer


# --- Hard-fence grep enforcement -------------------------------------


def test_eval_runner_source_does_not_invoke_forbidden_tools():
    """Hard fence: eval_runner.py must never invoke `mvn`,
    `spring-boot`, or `git`. We grep the source.
    """
    src = Path(_eval_runner_module.__file__).read_text(encoding="utf-8")

    # Strip out docstrings / comments to avoid false-positives on
    # the "no mvn" prose in the module docstring.
    cleaned_lines = []
    in_docstring = False
    for line in src.splitlines():
        stripped = line.strip()
        if stripped.startswith('"""') or stripped.startswith("'''"):
            in_docstring = not in_docstring
            if stripped.count('"""') == 2 or stripped.count("'''") == 2:
                in_docstring = False  # single-line docstring
            continue
        if in_docstring:
            continue
        if stripped.startswith("#"):
            continue
        cleaned_lines.append(line)
    cleaned = "\n".join(cleaned_lines).lower()

    forbidden_invocations = ["mvn ", '"mvn"', "spring-boot", "git "]
    for needle in forbidden_invocations:
        assert needle not in cleaned, (
            f"forbidden token {needle!r} detected in eval_runner.py source "
            f"(non-comment, non-docstring) — hard fence violated"
        )


def test_eval_runner_subprocess_cwd_is_eval_interactive(tmp_path: Path):
    """Per the S-Auto-2 contract: cwd must be `<repo>/eval_interactive`."""
    spec = SuiteRunSpec(name="bad_cases", path=Path("x"), parallel=1)
    fake_proc = MagicMock()
    fake_proc.returncode = 0
    fake_proc.stderr = ""
    with patch(
        "autoloop.scoring.eval_runner.subprocess.run", return_value=fake_proc
    ) as mock_run:
        run_suite(spec, results_root=tmp_path, config=DEFAULT_CONFIG, timeout_seconds=10)
    cwd_arg = mock_run.call_args.kwargs.get("cwd")
    assert cwd_arg is not None
    assert Path(cwd_arg).name == "eval_interactive"


# --- S-Auto-16: k-of-n majority pass ---------------------------------

import json as _json


def _mk_case(
    cid: str,
    passed: bool,
    *,
    models: list[str] | None = None,
    status: str = "OK",
    escalation_reason: str | None = None,
    failure_tags: list | None = None,
    l1_results: list[dict] | None = None,
    tier2_mandatory_fail_steps: list[str] | None = None,
) -> dict:
    calls = [{"model": m} for m in (models or ["deepseek-v4-flash"])]
    per_step = [
        {"step_id": sid, "severity": "mandatory", "outcome": "FAIL"}
        for sid in (tier2_mandatory_fail_steps or [])
    ]
    return {
        "case_id": cid,
        "primary_uc": "UC-A",
        "case_passed": passed,
        "status": status,
        "escalation_reason": escalation_reason,
        "failure_tags": failure_tags or [],
        "stop_reason": "bot_ended",
        "llm_calls": calls,
        "l1_results": l1_results or [],
        "tier2_result": {"per_step": per_step},
    }


def _single_suite_config(
    n: int,
    *,
    min_valid: int = 3,
    retry_cap: int = 2,
    primary_model: str | None = "deepseek-v4-flash",
) -> dict:
    cfg: dict = {
        "fitness": {
            "suites": [
                {"name": "bad_cases", "path": "eval_interactive/case_specs/bad_cases/",
                 "parallel": 1},
            ],
            "eval_suite_timeout_seconds": 1800,
            "parallel_suites": False,
            "samples_per_case": n,
            "aggregation": {
                "method": "majority",
                "min_valid_attempts": min_valid,
                "attempt_retry_cap": retry_cap,
            },
        }
    }
    # S-Auto-17 (OQ-S72.1): provider comparability anchors on the configured
    # primary chat model. Omitting it (primary_model=None) exercises the
    # fail-safe (EvalRunnerConfigError).
    if primary_model is not None:
        cfg["fitness"]["provider_policy"] = {"primary_model": primary_model}
    return cfg


def _fake_run_suite_factory(scripts: dict[str, list[list[dict]]]):
    """Returns a fake `run_suite` that writes the scripted per-attempt
    case_results to a real results.json under results_root/<suite>/ and
    returns a SuiteRunResult. Attempt index is tracked per suite; indices
    beyond the script clamp to the last scripted attempt.
    """
    counters: dict[str, int] = {}

    def _fake(spec, *, results_root, config, timeout_seconds):
        idx = counters.get(spec.name, 0)
        counters[spec.name] = idx + 1
        attempts = scripts[spec.name]
        cases = attempts[min(idx, len(attempts) - 1)]
        suite_dir = Path(results_root) / spec.name
        suite_dir.mkdir(parents=True, exist_ok=True)
        rj = suite_dir / "results.json"
        rj.write_text(
            _json.dumps({"run_id": f"{spec.name}-{idx}", "case_results": cases,
                         "summary": {}}),
            encoding="utf-8",
        )
        return SuiteRunResult(
            suite_name=spec.name,
            results_dir=suite_dir,
            results_json=rj,
            elapsed_seconds=1.0,
            exit_code=0,
        )

    return _fake, counters


def _read_aggregated(results_root: Path, suite: str = "bad_cases") -> dict:
    return _json.loads(
        (results_root / suite / "results.json").read_text(encoding="utf-8")
    )


def test_majority_n_loop_runs_n_attempts_and_aggregates(tmp_path: Path):
    cfg = _single_suite_config(3)
    scripts = {"bad_cases": [
        [_mk_case("c1", True)],
        [_mk_case("c1", True)],
        [_mk_case("c1", True)],
    ]}
    fake, counters = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        results = run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    assert counters["bad_cases"] == 3
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["case_id"] == "c1"
    assert agg["majority_passed"] is True
    assert agg["valid_attempts"] == 3
    assert agg["comparable"] is True
    assert len(agg["attempts"]) == 3
    # original fields preserved
    assert agg["primary_uc"] == "UC-A"
    # SuiteRunResult carries the per-attempt list.
    assert results["bad_cases"].attempts is not None
    assert len(results["bad_cases"].attempts) == 3


def test_majority_provider_mixed_dropped(tmp_path: Path):
    # attempt 2 is fallback-served (kimi minority among deepseek) → dropped.
    cfg = _single_suite_config(3, retry_cap=0)
    # attempt 2: 2 deepseek + 1 kimi → modal=deepseek, kimi counts as fallback.
    scripts = {"bad_cases": [
        [_mk_case("c1", True, models=["deepseek-v4-flash"])],
        [_mk_case("c1", True, models=["deepseek-v4-flash"])],
        [_mk_case("c1", False,
                  models=["deepseek-v4-flash", "deepseek-v4-flash", "kimi-k2"])],
    ]}
    fake, counters = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["valid_attempts"] == 2  # mixed attempt dropped
    assert agg["comparable"] is False  # 2 < min_valid 3
    assert agg["majority_passed"] is None
    assert agg["aggregate_status"] == "non_comparable"
    mixed = [a for a in agg["attempts"] if a["invalid_reason"] == "provider_mixed"]
    assert len(mixed) == 1
    assert mixed[0]["fallback_count"] == 1


def test_majority_infra_error_dropped(tmp_path: Path):
    cfg = _single_suite_config(3, retry_cap=0)
    scripts = {"bad_cases": [
        [_mk_case("c1", True)],
        [_mk_case("c1", False, status="ERROR")],
        [_mk_case("c1", False, escalation_reason="service_degraded")],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["valid_attempts"] == 1
    assert agg["comparable"] is False
    assert agg["aggregate_status"] == "infra_error"
    infra = [a for a in agg["attempts"] if a["invalid_reason"] == "infra_error"]
    assert len(infra) == 2


def test_majority_retry_to_cap(tmp_path: Path):
    # n=3 with attempt 2 mixed → 2 valid < 3 → retry one extra valid pass.
    cfg = _single_suite_config(3, retry_cap=2)
    scripts = {"bad_cases": [
        [_mk_case("c1", True)],
        [_mk_case("c1", True)],
        [_mk_case("c1", False,
                  models=["deepseek-v4-flash", "deepseek-v4-flash", "kimi-k2"])],
        [_mk_case("c1", True)],  # retry attempt (valid)
    ]}
    fake, counters = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    assert counters["bad_cases"] == 4  # 3 initial + 1 retry
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["valid_attempts"] == 3
    assert agg["comparable"] is True
    assert agg["majority_passed"] is True


def test_majority_insufficient_non_comparable(tmp_path: Path):
    cfg = _single_suite_config(2, min_valid=3, retry_cap=0)
    scripts = {"bad_cases": [
        [_mk_case("c1", True)],
        [_mk_case("c1", True)],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["valid_attempts"] == 2
    assert agg["majority_passed"] is None
    assert agg["aggregate_status"] == "non_comparable"


def test_majority_persistence_shape(tmp_path: Path):
    cfg = _single_suite_config(3)
    scripts = {"bad_cases": [
        [_mk_case("c1", True)],
        [_mk_case("c1", True)],
        [_mk_case("c1", False)],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    payload = _read_aggregated(tmp_path)
    assert payload["_aggregation"]["method"] == "majority"
    assert payload["_aggregation"]["min_valid_attempts"] == 3
    agg = payload["case_results"][0]
    for key in ("majority_passed", "pass_rate", "valid_attempts", "total_attempts",
                "comparable", "flaky", "aggregate_status", "attempts",
                "tier0_majority", "tier2_result_majority"):
        assert key in agg, f"missing aggregated key {key}"
    assert agg["majority_passed"] is True  # 2/3
    assert agg["flaky"] is True


def test_majority_tier0_check_majority(tmp_path: Path):
    # PII check fails in MINORITY (1 of 3) → tier0_majority keeps it passed.
    pii_fail = [{"check": "no_pii_leakage", "passed": False}]
    pii_ok = [{"check": "no_pii_leakage", "passed": True}]
    cfg = _single_suite_config(3)
    scripts = {"bad_cases": [
        [_mk_case("c1", True, l1_results=pii_ok)],
        [_mk_case("c1", True, l1_results=pii_ok)],
        [_mk_case("c1", False, l1_results=pii_fail)],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["tier0_majority"]["no_pii_leakage"] is True  # minority fail → still passed


def test_majority_non_chat_calltype_not_counted_as_fallback(tmp_path: Path):
    """A different model on a NON-chat callType (rerank/embedding) is NOT a
    fallback — provider comparability is scoped to the agent `chat` call.
    Regression guard for the modal-over-all-calltypes bug (S-Auto-16 §0:
    rerank=kimi swamped the mode and misflagged every deepseek chat call)."""
    def _mixed_calltype_case(passed: bool) -> dict:
        c = _mk_case("c1", passed)
        # 1 chat (deepseek, primary) + 2 rerank (kimi, by design — NOT fallback)
        c["llm_calls"] = [
            {"callType": "chat", "model": "deepseek-v4-flash"},
            {"callType": "rerank", "model": "kimi-k2.6"},
            {"callType": "rerank", "model": "kimi-k2.6"},
        ]
        return c
    cfg = _single_suite_config(3)
    scripts = {"bad_cases": [
        [_mixed_calltype_case(True)],
        [_mixed_calltype_case(True)],
        [_mixed_calltype_case(False)],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    # all 3 attempts valid (no provider_mixed) → comparable majority.
    assert agg["valid_attempts"] == 3
    assert agg["comparable"] is True
    assert agg["majority_passed"] is True  # 2/3
    for a in agg["attempts"]:
        assert a["invalid_reason"] is None
        assert a["fallback_count"] == 0
        assert a["actual_model"] == "deepseek-v4-flash"


def test_majority_tier2_step_majority(tmp_path: Path):
    # mandatory step s1 fails in MAJORITY (2 of 3) → appears in majority per_step.
    cfg = _single_suite_config(3)
    scripts = {"bad_cases": [
        [_mk_case("c1", False, tier2_mandatory_fail_steps=["s1"])],
        [_mk_case("c1", False, tier2_mandatory_fail_steps=["s1"])],
        [_mk_case("c1", True, tier2_mandatory_fail_steps=[])],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    steps = agg["tier2_result_majority"]["per_step"]
    assert any(s["step_id"] == "s1" and s["outcome"] == "FAIL" for s in steps)


# --- S-Auto-17 (OQ-S72.1): configured-primary provider-comparability -----


def test_configured_primary_detects_fallback_attempt(tmp_path: Path):
    """A `chat` attempt whose model != the CONFIGURED primary is a fallback
    (provider_mixed → dropped), even though it is the only deviating draw.
    Anchored on config.fitness.provider_policy.primary_model, NOT the modal
    model of the run.
    """
    cfg = _single_suite_config(3, retry_cap=0, primary_model="deepseek-v4-flash")
    scripts = {"bad_cases": [
        [_mk_case("c1", True, models=["deepseek-v4-flash"])],
        [_mk_case("c1", True, models=["deepseek-v4-flash"])],
        # a single fallback-served chat draw on a different model.
        [_mk_case("c1", False, models=["deepseek-v3-legacy"])],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["valid_attempts"] == 2  # the fallback draw dropped
    mixed = [a for a in agg["attempts"] if a["invalid_reason"] == "provider_mixed"]
    assert len(mixed) == 1
    assert mixed[0]["actual_model"] == "deepseek-v3-legacy"
    assert mixed[0]["fallback_count"] == 1


def test_configured_primary_whole_run_fallback_flagged(tmp_path: Path):
    """The bug the modal heuristic MISSED: when EVERY chat draw across the
    whole run is the fallback model, the empirical modal would call the
    fallback 'primary' and treat all attempts as comparable. Anchored on the
    configured primary, every draw is provider_mixed → non-comparable.
    """
    cfg = _single_suite_config(3, retry_cap=0, primary_model="deepseek-v4-flash")
    scripts = {"bad_cases": [
        [_mk_case("c1", True, models=["kimi-k2-fallback"])],
        [_mk_case("c1", True, models=["kimi-k2-fallback"])],
        [_mk_case("c1", True, models=["kimi-k2-fallback"])],
    ]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    agg = _read_aggregated(tmp_path)["case_results"][0]
    assert agg["valid_attempts"] == 0  # all 3 dropped as provider_mixed
    assert agg["comparable"] is False
    assert agg["majority_passed"] is None
    assert all(a["invalid_reason"] == "provider_mixed" for a in agg["attempts"])
    assert all(a["fallback_count"] == 1 for a in agg["attempts"])


def test_unset_primary_model_fail_safe_raises(tmp_path: Path):
    """Fail-safe (OQ-S72.1): with samples_per_case>1 and no configured
    primary_model, the run RAISES rather than degrading to the empirical
    modal model.
    """
    cfg = _single_suite_config(3, primary_model=None)
    scripts = {"bad_cases": [[_mk_case("c1", True)]]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        with pytest.raises(EvalRunnerConfigError):
            run_v1_fitness_suite(results_root=tmp_path, config=cfg)


def test_unset_primary_model_n1_single_pass_unaffected(tmp_path: Path):
    """The fail-safe only guards the n>1 majority path. At n=1 (single pass)
    there is no provider-comparability vote, so an absent primary_model does
    NOT raise — byte-identical to the pre-sprint single-draw path.
    """
    cfg = _single_suite_config(1, primary_model=None)
    scripts = {"bad_cases": [[_mk_case("c1", True)]]}
    fake, _ = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        results = run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    assert results["bad_cases"].exit_code == 0


# --- S-Y1.7 P0.4: primary-target oversampling ------------------------

# A real bad_cases case_spec (filename == case_id) so the plan resolver
# finds it on disk via the repo-wide naming convention.
_REAL_PRIMARY = "cs_uc_a_no_ad_id_ad_specific"


def test_primary_target_oversample_lifts_only_primaries(tmp_path: Path):
    """`fitness.pilot.primary_targets_samples` drives EXTRA primary-ONLY
    passes: the primary reaches the oversample n (5) while every other case
    stays at `samples_per_case` (2). Regression guard for the S-Y1.7 P0.4
    unwired-knob gap (the knob was previously read nowhere in eval_runner).
    """
    # min_valid=2 so the 2 base passes satisfy the floor and the retry loop
    # does NOT fire (it would otherwise confound the attempt count).
    cfg = _single_suite_config(2, min_valid=2)
    cfg["pilot"] = {"primary_targets": [_REAL_PRIMARY]}
    cfg["fitness"]["pilot"] = {"primary_targets_samples": 5}
    scripts = {
        "bad_cases": [
            [_mk_case(_REAL_PRIMARY, False), _mk_case("other_case", True)],
            [_mk_case(_REAL_PRIMARY, False), _mk_case("other_case", True)],
        ],
        # The oversample pass exercises ONLY the primary (mini-suite of 1).
        "bad_cases__primary_oversample": [
            [_mk_case(_REAL_PRIMARY, True)],
            [_mk_case(_REAL_PRIMARY, True)],
            [_mk_case(_REAL_PRIMARY, True)],
        ],
    }
    fake, counters = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    # 2 base passes + (5 - 2) = 3 oversample passes.
    assert counters["bad_cases"] == 2
    assert counters["bad_cases__primary_oversample"] == 3
    agg = {c["case_id"]: c for c in _read_aggregated(tmp_path)["case_results"]}
    assert len(agg[_REAL_PRIMARY]["attempts"]) == 5  # oversampled to n=5
    assert len(agg["other_case"]["attempts"]) == 2  # untouched at n=2


def test_primary_target_oversample_noop_without_pilot_targets(tmp_path: Path):
    """No pilot.primary_targets → the pre-pilot general-hill-climber path:
    NO oversample passes are run (back-compat)."""
    cfg = _single_suite_config(2, min_valid=2)
    # fitness.pilot.primary_targets_samples present but no pilot block → no-op.
    cfg["fitness"]["pilot"] = {"primary_targets_samples": 5}
    scripts = {"bad_cases": [[_mk_case("c1", True)], [_mk_case("c1", True)]]}
    fake, counters = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    assert counters["bad_cases"] == 2
    assert "bad_cases__primary_oversample" not in counters


def test_primary_target_oversample_noop_when_target_not_above_n(tmp_path: Path):
    """`primary_targets_samples <= samples_per_case` → no extra passes (the
    primary already gets at least that many from the uniform passes)."""
    cfg = _single_suite_config(3, min_valid=3)
    cfg["pilot"] = {"primary_targets": [_REAL_PRIMARY]}
    cfg["fitness"]["pilot"] = {"primary_targets_samples": 3}  # == n
    scripts = {
        "bad_cases": [
            [_mk_case(_REAL_PRIMARY, True)],
            [_mk_case(_REAL_PRIMARY, True)],
            [_mk_case(_REAL_PRIMARY, True)],
        ],
    }
    fake, counters = _fake_run_suite_factory(scripts)
    with patch("autoloop.scoring.eval_runner.run_suite", side_effect=fake):
        run_v1_fitness_suite(results_root=tmp_path, config=cfg)
    assert counters["bad_cases"] == 3
    assert "bad_cases__primary_oversample" not in counters
