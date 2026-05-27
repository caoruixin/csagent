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
    call_args = mock_run.call_args
    cmd = call_args[0][0]
    assert cmd[:4] == ["uv", "run", "eval-interactive", "run"]
    assert "--path" in cmd
    assert "--parallel" in cmd
    assert "--output-dir" in cmd


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
        # The --output-dir path's parent name encodes the suite.
        out_idx = cmd.index("--output-dir") + 1
        out = cmd[out_idx]
        suite_name = Path(out).name
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
