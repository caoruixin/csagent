"""CLI integration tests — every subcommand exits cleanly on a
mocked / fixture-backed environment.
"""

from __future__ import annotations

import json
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from autoloop import cli as _cli
from autoloop.cli import main
from autoloop.memory import experiments_log


def test_cli_check_help_clean():
    """argparse `--help` raises SystemExit(0). Smoke test the parser."""
    with pytest.raises(SystemExit) as exc:
        main(["check", "--help"])
    assert exc.value.code == 0


@pytest.mark.parametrize("name", ["dry-run", "run", "report", "apply", "audit"])
def test_cli_subcommand_help_exits_zero(name: str):
    """`autoloop <name> --help` must exit 0 (argparse convention)."""
    with pytest.raises(SystemExit) as exc:
        main([name, "--help"])
    assert exc.value.code == 0


# --- dry-run / run subcommands -------------------------------------


def _config_at_autoloop_dir(tmp_path: Path) -> Path:
    """Place config at <tmp>/autoloop/config.yaml so the CLI's
    repo_root = config_path.parent.parent resolves to <tmp>.
    """
    autoloop_dir = tmp_path / "autoloop"
    autoloop_dir.mkdir(parents=True, exist_ok=True)
    config_path = autoloop_dir / "config.yaml"
    config_path.write_text(_minimal_config_yaml(), encoding="utf-8")
    return config_path


def test_cli_dry_run_invokes_loop(tmp_path: Path):
    """`autoloop dry-run --experiments 1` constructs a config + calls
    run_iterations(dry_run=True).
    """
    config_path = _config_at_autoloop_dir(tmp_path)

    fake_result = MagicMock()
    fake_result.iteration_id = "exp-1"
    fake_result.decision = "discard"
    fake_result.discard_reason = "dry_run_no_apply"
    fake_result.elapsed_seconds = 0.1

    with patch("autoloop.loop.run_iterations", return_value=[fake_result]):
        rc = main(["dry-run", "--config", str(config_path), "--experiments", "1"])
    assert rc == 0


def test_cli_run_invokes_loop_with_dry_run_false(tmp_path: Path):
    config_path = _config_at_autoloop_dir(tmp_path)

    fake_result = MagicMock()
    fake_result.iteration_id = "exp-1"
    fake_result.decision = "discard"
    fake_result.discard_reason = "x"
    fake_result.elapsed_seconds = 0.1

    captured_kwargs: dict = {}

    def _capture(*args, **kwargs):
        captured_kwargs.update(kwargs)
        return [fake_result]

    # S-Auto-7.1 — the new pre-flight gate refuses a minimal placeholder
    # config (no blessed baseline_dir on disk). This test verifies loop
    # invocation, not pre-flight integration (which has dedicated coverage
    # in test_preflight.py). `--skip-preflight` bypasses the gate as designed.
    with patch("autoloop.loop.run_iterations", side_effect=_capture):
        rc = main([
            "run", "--config", str(config_path),
            "--experiments", "1", "--skip-preflight",
        ])
    assert rc == 0
    assert captured_kwargs.get("dry_run") is False


# --- report subcommand ----------------------------------------------


def test_cli_report_renders_html(tmp_path: Path):
    config_path = _config_at_autoloop_dir(tmp_path)
    log_path = tmp_path / "autoloop/results/experiments.jsonl"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    experiments_log.append(log_path, {
        "iteration_id": "exp-1",
        "decision": "keep",
        "hypothesis": {"target_skill_file": "x.yaml", "target_field_path": "$.procedure"},
        "verdict": {"tier_breakdown": {}},
        "discard_reason": None,
    })
    rc = main(["report", "--config", str(config_path)])
    assert rc == 0
    out = tmp_path / "autoloop/results/report.html"
    assert out.exists()
    html = out.read_text(encoding="utf-8")
    assert "<table" in html
    assert "exp-1" in html


# --- apply subcommand (Hybrid; cherry-pick + patch + NO auto-commit) -


def test_cli_apply_does_not_auto_commit(tmp_path: Path):
    """`autoloop apply --experiment exp-N`:
       1. verifies autoloop/exp-N branch exists
       2. cherry-picks
       3. emits proposed config.yaml patch
       4. does NOT auto-commit anything

    Setup: a tmp git repo with an autoloop/exp-1 branch that has a
    one-line file change relative to main.
    """
    repo = tmp_path / "repo"
    repo.mkdir()
    subprocess.run(["git", "init", "-q", "-b", "main"], cwd=repo, check=True)
    subprocess.run(["git", "config", "user.email", "t@t"], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "config", "user.name", "T"], cwd=repo, check=True, capture_output=True)
    (repo / "autoloop").mkdir()
    (repo / "autoloop" / "README.md").write_text("init", encoding="utf-8")
    (repo / "autoloop" / "config.yaml").write_text(_minimal_config_yaml(), encoding="utf-8")
    subprocess.run(["git", "add", "."], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "commit", "-q", "-m", "init"], cwd=repo, check=True)
    # Create autoloop/exp-1 branch with one extra commit.
    subprocess.run(["git", "checkout", "-q", "-b", "autoloop/exp-1"], cwd=repo, check=True)
    (repo / "autoloop" / "README.md").write_text("exp-1 change", encoding="utf-8")
    subprocess.run(["git", "add", "."], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "commit", "-q", "-m", "exp-1 edit"], cwd=repo, check=True)
    # Switch back to main for the apply cherry-pick.
    subprocess.run(["git", "checkout", "-q", "main"], cwd=repo, check=True)

    config_path = repo / "autoloop" / "config.yaml"

    rc = main(["apply", "--config", str(config_path), "--experiment", "exp-1"])
    assert rc == 0
    # Cherry-pick produces a commit on main:
    log = subprocess.run(
        ["git", "log", "--format=%s", "-n", "5"],
        cwd=repo, capture_output=True, text=True,
    )
    assert "exp-1 edit" in log.stdout
    # Patch file was written but NO further commit was created
    # (the cherry-pick itself is one commit; we verify no `--amend`
    # was issued AND the working tree is clean after the cherry-pick).
    runs_dir = repo / "autoloop" / "results" / "runs" / "exp-1"
    patch_file = runs_dir / "proposed-baseline-update.patch"
    assert patch_file.exists()
    status = subprocess.run(
        ["git", "status", "--porcelain"], cwd=repo, capture_output=True, text=True,
    )
    # The patch file is untracked but the tree is otherwise clean.
    assert "M " not in status.stdout  # no modified-staged
    # The cherry-pick commit is the latest:
    head_msg = subprocess.run(
        ["git", "log", "-1", "--format=%s"], cwd=repo, capture_output=True, text=True,
    )
    assert "exp-1 edit" in head_msg.stdout


def test_cli_apply_missing_branch_errors(tmp_path: Path):
    repo = tmp_path / "repo"
    repo.mkdir()
    subprocess.run(["git", "init", "-q", "-b", "main"], cwd=repo, check=True)
    subprocess.run(["git", "config", "user.email", "t@t"], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "config", "user.name", "T"], cwd=repo, check=True, capture_output=True)
    (repo / "autoloop").mkdir()
    (repo / "autoloop" / "config.yaml").write_text(_minimal_config_yaml(), encoding="utf-8")
    (repo / "init.txt").write_text("x", encoding="utf-8")
    subprocess.run(["git", "add", "."], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "commit", "-q", "-m", "init"], cwd=repo, check=True)

    config_path = repo / "autoloop" / "config.yaml"
    rc = main(["apply", "--config", str(config_path), "--experiment", "exp-999"])
    assert rc == 1


# --- audit subcommand (firewall RESPECT by default) ----------------


def test_cli_audit_default_respects_shadow_firewall(tmp_path: Path, capsys):
    """`autoloop audit --experiment exp-1` (no flag) outputs JSON
    without per_case_failures / failure_tags.
    """
    config_path = _config_at_autoloop_dir(tmp_path)
    log_path = tmp_path / "autoloop/results/experiments.jsonl"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    experiments_log.append(log_path, {
        "iteration_id": "exp-1",
        "decision": "keep",
        "hypothesis": {"target_skill_file": "x.yaml", "target_field_path": "$.procedure"},
        "verdict": {"tier_breakdown": {
            "shadow_regression": {"drop_pct": 0.0, "regression_detected": False}
        }},
        "discard_reason": None,
    })
    rc = main(["audit", "--config", str(config_path), "--experiment", "exp-1"])
    assert rc == 0
    captured = capsys.readouterr().out
    assert "exp-1" in captured
    assert "per_case_failures" not in captured
    assert "failure_tags" not in captured


def test_cli_audit_include_shadow_detail_flag_opens_per_case(tmp_path: Path, capsys):
    """`--include-shadow-detail` opens the human-only path; per_case_failures
    appears in the output.
    """
    config_path = _config_at_autoloop_dir(tmp_path)
    log_path = tmp_path / "autoloop/results/experiments.jsonl"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    experiments_log.append(log_path, {
        "iteration_id": "exp-1",
        "decision": "keep",
        "hypothesis": {},
        "verdict": {},
        "discard_reason": None,
    })

    eval_dir = tmp_path / "autoloop/results/runs/exp-1/eval/shadow"
    eval_dir.mkdir(parents=True, exist_ok=True)
    (eval_dir / "results.json").write_text(json.dumps({
        "case_results": [
            {"case_id": "shadow-1", "case_passed": False,
             "primary_uc": "UC-A", "failure_tags": ["drift"]},
        ]
    }))
    rc = main([
        "audit", "--config", str(config_path),
        "--experiment", "exp-1", "--include-shadow-detail",
    ])
    assert rc == 0
    captured = capsys.readouterr().out
    assert "per_case_failures" in captured
    assert "shadow-1" in captured


# --- helper -----------------------------------------------------------


def _minimal_config_yaml() -> str:
    return """\
mutable_surface:
  allowed_skill_files:
    - server/src/main/resources/skills/discover_triage.yaml
  allowed_field_paths:
    - "$.procedure"

paths:
  experiments_log: "autoloop/results/experiments.jsonl"
  lessons_log: "autoloop/results/lessons.md"
  runs_dir: "autoloop/results/runs/"

lessons:
  compaction_window_k: 10
  recent_iterations_for_propose: 5

meta_agent:
  provider: anthropic
  model: claude-opus-4-7
  temperature: 0.3
  max_tokens: 4096
  api_key_env: AUTOLOOP_META_LLM_API_KEY
  base_url_env: AUTOLOOP_META_LLM_BASE_URL
  request_timeout_seconds: 120

fitness:
  suites:
    - name: bad_cases
      path: eval_interactive/case_specs/bad_cases/
      parallel: 1
  improvement_threshold_mode: case_count
  improvement_min_cases: 1
  shadow_max_drop_pct: 3.0
  anchor_outcome_max_drop_cases: 0
  baseline_dir: eval_interactive/results/<PLACEHOLDER>
  eval_suite_timeout_seconds: 1800
  parallel_suites: false
"""
