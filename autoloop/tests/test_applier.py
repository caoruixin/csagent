"""Tests for the applier — git operations + YAML field patch + Spring spawn.

All git operations run in a tmp_path-bound git repo. Spring spawn is
mocked: we patch subprocess.Popen + the health probe so no real
backend is brought up.
"""

from __future__ import annotations

import re
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from autoloop.meta_agent.proposer import Hypothesis, fingerprint_hypothesis
from autoloop.sandbox import applier as _applier
from autoloop.sandbox.applier import (
    AppliedExperiment,
    ApplyError,
    BeforeValueMismatchError,
    CrossFileError,
    SanityCheckRejectError,
    SpringStartupTimeoutError,
    apply,
    cleanup,
)


_ALLOWED_FILES = [
    "server/src/main/resources/skills/discover_triage.yaml",
    "server/src/main/resources/skills/resolve_faq_grounded_answer.yaml",
    "server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml",
    "server/src/main/resources/skills/confirm.yaml",
    "server/src/main/resources/skills/escalate.yaml",
    "server/src/main/resources/skills/terminal.yaml",
]
_ALLOWED_PATHS = [
    "$.procedure",
    "$.grounding_instruction",
    "$.escalation_policy",
    "$.critical_steps[*].desc",
]
_CONFIG = {
    "mutable_surface": {
        "allowed_skill_files": _ALLOWED_FILES,
        "allowed_field_paths": _ALLOWED_PATHS,
    }
}


_FIXTURE_YAML = """\
name: discover_triage
description: triage incoming case
applicable_phases: [discover]
applicable_use_cases: [UC-A]
tools_required:
  - search_knowledge
required_context_keys: []
max_tool_steps: 5
allow_interim_message: false
valid_terminal_outcomes: [resolved]
procedure: |
  step one
  step two
critical_steps:
  - id: s1
    trace_check: tool_called
    mandatory_for: []
    severity: advisory
    desc: original step one desc
  - id: s2
    trace_check: tool_called
    mandatory_for: []
    severity: advisory
    desc: original step two desc
grounding_instruction: original grounding
escalation_policy: original escalation
guardrails: []
state_inheritance: ""
"""


def _make_git_repo(tmp_path: Path) -> Path:
    """Init a git repo + commit a fake Skill YAML + dummy server/pom.xml."""
    repo = tmp_path / "repo"
    repo.mkdir()
    subprocess.run(["git", "init"], cwd=repo, check=True, capture_output=True)
    subprocess.run(
        ["git", "-c", "user.email=t@t", "-c", "user.name=T", "config", "user.email", "t@t"],
        cwd=repo, check=True, capture_output=True,
    )
    subprocess.run(
        ["git", "-c", "user.email=t@t", "-c", "user.name=T", "config", "user.name", "T"],
        cwd=repo, check=True, capture_output=True,
    )
    skill_path = repo / "server/src/main/resources/skills/discover_triage.yaml"
    skill_path.parent.mkdir(parents=True, exist_ok=True)
    skill_path.write_text(_FIXTURE_YAML, encoding="utf-8")
    (repo / "server").mkdir(exist_ok=True)
    (repo / "server" / "pom.xml").write_text("<project/>", encoding="utf-8")
    subprocess.run(["git", "add", "."], cwd=repo, check=True, capture_output=True)
    subprocess.run(
        ["git", "commit", "-m", "init"],
        cwd=repo, check=True, capture_output=True,
    )
    return repo


def _hypothesis_for_procedure(after: str = "step one revised\nstep two") -> Hypothesis:
    hyp = Hypothesis(
        target_skill_file="server/src/main/resources/skills/discover_triage.yaml",
        target_field_path="$.procedure",
        before_value="step one\nstep two\n",
        after_value=after,
        rationale="address drift carry-over",
    )
    hyp.fingerprint = fingerprint_hypothesis(hyp)
    return hyp


# --- apply happy path ------------------------------------------------


def test_apply_writes_yaml_and_creates_branch_and_commit(tmp_path: Path):
    repo = _make_git_repo(tmp_path)
    hyp = _hypothesis_for_procedure()

    fake_proc = MagicMock()
    fake_proc.poll.return_value = None
    fake_proc.pid = 99999

    with patch.object(_applier, "_spawn_spring", return_value=fake_proc), \
         patch.object(_applier, "_probe_url_is_up", return_value=True), \
         patch.object(_applier, "_find_free_port", return_value=18080), \
         patch.object(_applier, "_terminate_process") as mock_term:
        applied = apply(
            hyp,
            iteration_id="exp-1",
            config=_CONFIG,
            repo_root=repo,
            health_probe_interval_seconds=0.0,
        )
        assert isinstance(applied, AppliedExperiment)
        assert applied.branch_name == "autoloop/exp-1"
        assert applied.backend_port == 18080
        # Branch exists in git:
        proc = subprocess.run(
            ["git", "rev-parse", "--verify", "autoloop/exp-1"],
            cwd=repo, capture_output=True, text=True,
        )
        assert proc.returncode == 0
        # File on disk has revised content (round-tripped YAML):
        text = (repo / hyp.target_skill_file).read_text(encoding="utf-8")
        assert "step one revised" in text
        cleanup(applied, repo_root=repo)
        mock_term.assert_called()


# --- cross-file rejection -------------------------------------------


def test_apply_cross_file_rejected_before_git(tmp_path: Path):
    repo = _make_git_repo(tmp_path)
    hyp = _hypothesis_for_procedure()
    hyp.target_skill_file = "server/src/main/java/Foo.java"
    with pytest.raises(CrossFileError):
        apply(hyp, iteration_id="exp-9", config=_CONFIG, repo_root=repo)
    # No autoloop/exp-9 branch created:
    proc = subprocess.run(
        ["git", "rev-parse", "--verify", "autoloop/exp-9"],
        cwd=repo, capture_output=True, text=True,
    )
    assert proc.returncode != 0


# --- before_value mismatch ------------------------------------------


def test_apply_before_value_mismatch_raises(tmp_path: Path):
    repo = _make_git_repo(tmp_path)
    hyp = _hypothesis_for_procedure()
    hyp.before_value = "completely different content that's not in the file"
    with pytest.raises(BeforeValueMismatchError):
        apply(
            hyp, iteration_id="exp-2",
            config=_CONFIG, repo_root=repo,
            health_probe_interval_seconds=0.0,
        )


# --- Spring startup timeout -----------------------------------------


def test_apply_spring_timeout_propagates_cleanly(tmp_path: Path):
    repo = _make_git_repo(tmp_path)
    hyp = _hypothesis_for_procedure()

    fake_proc = MagicMock()
    fake_proc.poll.return_value = None
    fake_proc.pid = 12345

    with patch.object(_applier, "_spawn_spring", return_value=fake_proc), \
         patch.object(_applier, "_probe_url_is_up", return_value=False), \
         patch.object(_applier, "_find_free_port", return_value=18081), \
         patch.object(_applier, "_terminate_process") as mock_term:
        with pytest.raises(SpringStartupTimeoutError):
            apply(
                hyp,
                iteration_id="exp-3",
                config=_CONFIG,
                repo_root=repo,
                health_probe_timeout_seconds=1,
                health_probe_interval_seconds=0.0,
            )
        # On timeout the process is terminated:
        mock_term.assert_called()


# --- cleanup() is reentrant -----------------------------------------


def test_cleanup_with_no_process_is_noop(tmp_path: Path):
    repo = _make_git_repo(tmp_path)
    applied = AppliedExperiment(
        iteration_id="exp-x",
        branch_name="autoloop/exp-x",
        commit_sha="abc",
        skill_file_path=repo / "x",
        backend_port=0,
        backend_process=None,
        original_branch="master",
    )
    cleanup(applied, repo_root=repo)  # no exception


# --- Hard fence grep -------------------------------------------------


def test_applier_source_does_not_invoke_eval_interactive():
    """applier.py must not call eval-interactive; eval is the
    eval_runner's job.
    """
    src = Path(_applier.__file__).read_text(encoding="utf-8")
    # Filter docstrings + comments before searching.
    stripped_lines = []
    in_docstring = False
    quote = None
    for line in src.splitlines():
        s = line.strip()
        if not in_docstring and (s.startswith('"""') or s.startswith("'''")):
            quote = s[:3]
            in_docstring = not s.endswith(quote) or len(s) <= 3
            continue
        if in_docstring:
            if s.endswith(quote):
                in_docstring = False
            continue
        if s.startswith("#"):
            continue
        stripped_lines.append(line)
    cleaned = "\n".join(stripped_lines).lower()
    assert "eval-interactive" not in cleaned
    assert "eval_interactive" not in cleaned


# --- YAML field read/write helpers ----------------------------------


def test_parse_field_path_simple():
    tokens = _applier._parse_field_path("$.procedure")
    assert tokens == ["procedure"]


def test_parse_field_path_with_index():
    tokens = _applier._parse_field_path("$.critical_steps[2].desc")
    assert tokens == ["critical_steps", 2, "desc"]


def test_write_field_to_yaml_round_trip_changes_only_one_path():
    """After writing one field, the YAML AST diff should contain only
    that path — the sandbox should ACCEPT.
    """
    from autoloop.sandbox.yaml_diff_validator import validate_skill_yaml_diff

    after = _applier._write_field_to_yaml(
        _FIXTURE_YAML, "$.procedure", "step one revised\nstep two"
    )
    v = validate_skill_yaml_diff(
        before_yaml=_FIXTURE_YAML,
        after_yaml=after,
        file_path="server/src/main/resources/skills/discover_triage.yaml",
        allowed_skill_files=_ALLOWED_FILES,
        allowed_field_paths=_ALLOWED_PATHS,
    )
    assert v.decision == "ACCEPT", v.reason


def test_write_field_to_yaml_critical_steps_desc_round_trip():
    after = _applier._write_field_to_yaml(
        _FIXTURE_YAML, "$.critical_steps[0].desc", "revised step one desc"
    )
    assert "revised step one desc" in after


def test_read_field_value_from_yaml_returns_string():
    val = _applier._read_field_from_yaml(_FIXTURE_YAML, "$.grounding_instruction")
    assert val == "original grounding"


# --- OQ-S62.2: idempotent branch creation -----------------------------


def test_git_create_branch_idempotent_on_collision(tmp_path: Path):
    """A stale `autoloop/exp-N` branch from a prior killed iteration
    must not break the next `apply()` invocation. The applier should
    switch to the existing branch without raising.
    """
    repo = _make_git_repo(tmp_path)
    # Pre-create the autoloop/exp-X branch (simulates a prior killed
    # iter that left the branch behind).
    subprocess.run(
        ["git", "branch", "autoloop/exp-X"],
        cwd=repo, check=True, capture_output=True,
    )
    # Sanity: the branch should exist + we should be on the main
    # branch (whichever the repo's default is).
    assert _applier._branch_exists(repo, "autoloop/exp-X")
    pre_branch = _applier._git_current_branch(repo)
    assert pre_branch != "autoloop/exp-X"

    # Calling _git_create_branch on the existing branch should
    # silently switch to it, not raise.
    _applier._git_create_branch(repo, "autoloop/exp-X")

    assert _applier._git_current_branch(repo) == "autoloop/exp-X"


def test_git_create_branch_creates_fresh_branch(tmp_path: Path):
    """The non-collision happy path is unchanged: a fresh branch name
    is created + switched to.
    """
    repo = _make_git_repo(tmp_path)
    assert not _applier._branch_exists(repo, "autoloop/exp-fresh")
    pre_branch = _applier._git_current_branch(repo)
    assert pre_branch != "autoloop/exp-fresh"

    _applier._git_create_branch(repo, "autoloop/exp-fresh")

    assert _applier._branch_exists(repo, "autoloop/exp-fresh")
    assert _applier._git_current_branch(repo) == "autoloop/exp-fresh"


def test_branch_exists_helper(tmp_path: Path):
    """`_branch_exists` returns True iff the named ref is in the
    local heads tree.
    """
    repo = _make_git_repo(tmp_path)
    assert not _applier._branch_exists(repo, "autoloop/does-not-exist")
    subprocess.run(
        ["git", "branch", "autoloop/created-by-test"],
        cwd=repo, check=True, capture_output=True,
    )
    assert _applier._branch_exists(repo, "autoloop/created-by-test")
