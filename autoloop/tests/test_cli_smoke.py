"""Smoke tests for `autoloop.cli`.

Each subcommand's `--help` must exit 0 and print help. The `check`
subcommand must exit 0 against the known-good shipped `config.yaml`
and exit 1 against a config that names a non-existent Skill file.
"""

from __future__ import annotations

import os
import subprocess
import sys
import textwrap
from pathlib import Path

import pytest
import yaml

REPO_ROOT = Path(__file__).resolve().parents[2]
AUTOLOOP_DIR = REPO_ROOT / "autoloop"


def _run_cli(args: list[str], cwd: Path | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, "-m", "autoloop", *args],
        cwd=str(cwd or REPO_ROOT),
        capture_output=True,
        text=True,
    )


SUBCOMMANDS = ["check", "dry-run", "run", "report", "apply", "audit"]


@pytest.mark.parametrize("subcommand", SUBCOMMANDS)
def test_subcommand_help_exits_zero(subcommand: str) -> None:
    res = _run_cli([subcommand, "--help"])
    assert res.returncode == 0, (
        f"{subcommand} --help exited {res.returncode}; stderr={res.stderr!r}"
    )
    assert "usage" in res.stdout.lower() or subcommand in res.stdout.lower()


def test_check_with_shipped_config_exits_zero() -> None:
    res = _run_cli(["check"])
    assert res.returncode == 0, (
        f"check exited {res.returncode}; "
        f"stdout={res.stdout!r}; stderr={res.stderr!r}"
    )
    assert "PASS" in res.stdout


def test_check_with_missing_skill_path_exits_one(tmp_path: Path) -> None:
    bad_config = {
        "mutable_surface": {
            "allowed_field_paths": [
                "$.procedure",
                "$.grounding_instruction",
                "$.escalation_policy",
                "$.critical_steps[*].desc",
            ],
            "allowed_skill_files": [
                "server/src/main/resources/skills/discover_triage.yaml",
                "server/src/main/resources/skills/this_file_does_not_exist.yaml",
            ],
        },
        "paths": {
            "experiments_log": "autoloop/results/experiments.jsonl",
            "iterations_index": "autoloop/results/iterations.jsonl",
            "lessons_log": "autoloop/results/lessons.md",
            "runs_dir": "autoloop/results/runs/",
        },
        "lessons": {"compaction_window_k": 10},
    }
    # Place the bad config inside a fake `autoloop/` so the CLI's
    # repo-root resolution (parent of config) still finds the real
    # skill YAMLs under the actual repo. To keep that working we put
    # the bad config beside the real one but with a different name —
    # the --config arg accepts an arbitrary path.
    bad_dir = tmp_path / "autoloop"
    bad_dir.mkdir()
    bad_path = bad_dir / "config_bad.yaml"
    with bad_path.open("w", encoding="utf-8") as f:
        yaml.safe_dump(bad_config, f)

    # Symlink the real repo's server tree under tmp_path so the
    # CLI's `repo_root = config_path.parent.parent` finds the real
    # Skill YAMLs. Only `discover_triage.yaml` is expected to exist
    # under the symlink; the bogus one is the failure target.
    (tmp_path / "server").symlink_to(REPO_ROOT / "server")

    res = _run_cli(["check", "--config", str(bad_path)])
    assert res.returncode == 1, (
        f"check with bad config expected exit 1, got {res.returncode}; "
        f"stdout={res.stdout!r}; stderr={res.stderr!r}"
    )
    assert "this_file_does_not_exist.yaml" in (res.stdout + res.stderr)


def test_top_level_help_lists_all_subcommands() -> None:
    res = _run_cli(["--help"])
    assert res.returncode == 0
    for sub in SUBCOMMANDS:
        assert sub in res.stdout, f"top-level help missing subcommand {sub}"


def test_placeholder_subcommand_exits_nonzero() -> None:
    """The placeholder subcommands are not yet implemented and SHOULD
    exit 1 so that a CI invocation of a placeholder surfaces as a
    failure rather than a silent no-op.
    """
    res = _run_cli(["dry-run"])
    assert res.returncode == 1
    assert "S-Auto-3" in res.stdout
