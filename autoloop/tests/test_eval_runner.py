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
