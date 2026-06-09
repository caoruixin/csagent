"""Integration tests for S-Auto-4 wiring in loop.py.

Covers the three new insertion points:

A. pre-sandbox content_validator runs BEFORE sandbox.validate_skill_yaml_diff;
   FAIL short-circuits with the right discard_reason and never invokes
   the sandbox.
B. FLAG_FOR_CODEX from anti_hardcode_check does NOT discard; the
   iteration continues through apply/eval and the experiments_log row
   carries `anti_hardcode_flag_for_codex: true`.
C. post-eval gaming.detect runs after the verdict; flags are persisted
   into experiments_log and surface via `audit`.

Plus the renamed S-Auto-3 adversarial-fixture test (now in test_loop.py
under the new name `test_adversarial_fixture_fails_real_detection`).
"""

from __future__ import annotations

import json
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from autoloop import loop as _loop
from autoloop.loop import IterationResult, run_one_iteration
from autoloop.memory import experiments_log
from autoloop.meta_agent.llm_client import LLMResponse
from autoloop.meta_agent.proposer import Hypothesis, fingerprint_hypothesis
from autoloop.sandbox.applier import AppliedExperiment
from autoloop.scoring.tier_evaluator import LayerResult, LexicographicVerdict


# ---------------------------------------------------------------------
# Fixtures (mirrors test_loop.py's helpers)
# ---------------------------------------------------------------------


_FIXTURE_YAML = """\
name: discover_triage
description: x
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
    desc: original step desc
grounding_instruction: original grounding
escalation_policy: original escalation
guardrails: []
state_inheritance: ""
"""

_ALLOWED_FILES = [
    "server/src/main/resources/skills/discover_triage.yaml",
]
_ALLOWED_PATHS = [
    "$.procedure",
    "$.grounding_instruction",
    "$.escalation_policy",
    "$.critical_steps[*].desc",
]


def _config_for(tmp_repo: Path) -> dict:
    return {
        "mutable_surface": {
            "allowed_skill_files": _ALLOWED_FILES,
            "allowed_field_paths": _ALLOWED_PATHS,
        },
        "meta_agent": {
            "provider": "anthropic",
            "model": "x",
            "temperature": 0.0,
            "max_tokens": 100,
            "api_key_env": "AUTOLOOP_META_LLM_API_KEY",
            "base_url_env": "AUTOLOOP_META_LLM_BASE_URL",
        },
        "paths": {
            "experiments_log": "autoloop/results/experiments.jsonl",
            "lessons_log": "autoloop/results/lessons.md",
            "runs_dir": "autoloop/results/runs/",
        },
        "lessons": {
            "compaction_window_k": 10,
            "recent_iterations_for_propose": 5,
        },
        "fitness": {
            "baseline_dir": "eval_interactive/results/<PLACEHOLDER>",
            "suites": [
                {"name": "bad_cases", "path": "x", "parallel": 1},
                {"name": "anchor_outcome", "path": "x", "parallel": 4},
                {"name": "shadow", "path": "x", "parallel": 4},
            ],
            "shadow_max_drop_pct": 3.0,
            "anchor_outcome_max_drop_cases": 0,
            "improvement_threshold_mode": "case_count",
            "improvement_min_cases": 1,
            "eval_suite_timeout_seconds": 1800,
            "scoring_code_baseline_sha": None,
        },
    }


def _make_repo(tmp_path: Path) -> Path:
    repo = tmp_path / "repo"
    repo.mkdir()
    subprocess.run(["git", "init"], cwd=repo, check=True, capture_output=True)
    subprocess.run(
        ["git", "config", "user.email", "t@t"],
        cwd=repo, check=True, capture_output=True,
    )
    subprocess.run(
        ["git", "config", "user.name", "T"],
        cwd=repo, check=True, capture_output=True,
    )
    skill = repo / "server/src/main/resources/skills/discover_triage.yaml"
    skill.parent.mkdir(parents=True, exist_ok=True)
    skill.write_text(_FIXTURE_YAML, encoding="utf-8")
    (repo / "autoloop/results").mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "add", "."], cwd=repo, check=True, capture_output=True)
    subprocess.run(
        ["git", "commit", "-m", "init"],
        cwd=repo, check=True, capture_output=True,
    )
    return repo


def _hypothesis(after: str = "step one revised\nstep two") -> Hypothesis:
    hyp = Hypothesis(
        target_skill_file="server/src/main/resources/skills/discover_triage.yaml",
        target_field_path="$.procedure",
        before_value="step one\nstep two\n",
        after_value=after,
        rationale="fix drift",
    )
    hyp.fingerprint = fingerprint_hypothesis(hyp)
    return hyp


def _fake_client():
    class FakeClient:
        def __init__(self):
            self.calls = 0

        def is_configured(self) -> bool:
            return True

        def chat(self, system, user):
            self.calls += 1
            return LLMResponse(text="{}", raw=None)
    return FakeClient()


# ---------------------------------------------------------------------
# Insertion A — content_validator FAIL discards BEFORE sandbox
# ---------------------------------------------------------------------


def test_content_validator_failure_discards_before_sandbox(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)
    # Empty after_value triggers content_validator.zero_length.
    bad_hyp = _hypothesis(after="")

    sandbox_mock = MagicMock(name="validate_skill_yaml_diff")

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=bad_hyp), \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch("autoloop.loop.validate_skill_yaml_diff", sandbox_mock):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-cv-1",
            client=_fake_client(), repo_root=repo,
        )

    assert r.decision == "discard"
    assert r.discard_reason is not None
    assert r.discard_reason.startswith("content_validator_rejected:")
    sandbox_mock.assert_not_called()


# ---------------------------------------------------------------------
# Insertion B — FLAG_FOR_CODEX does NOT discard
# ---------------------------------------------------------------------


def test_anti_hardcode_flag_for_codex_does_not_discard(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)
    # Use a borderline FLAG_FOR_CODEX hypothesis: standalone MUST
    # without Runtime-like subject.
    hyp = _hypothesis(
        after="step one revised\nOperators must always escalate suspicious cases",
    )

    fake_applied = AppliedExperiment(
        iteration_id="exp-flag",
        branch_name="autoloop/exp-flag",
        commit_sha="abc",
        skill_file_path=repo / "server/src/main/resources/skills/discover_triage.yaml",
        backend_port=18601,
        backend_process=None,
        original_branch="master",
    )
    fake_verdict = LexicographicVerdict(
        decision="keep",
        discard_reason=None,
        layer_results=[
            LayerResult(layer=4, name="shadow_regression", passed=True,
                        reason="shadow_no_regression",
                        metrics_observed={"drop_pct": 0.0, "regression_detected": False}),
        ],
        tier_breakdown={"shadow_regression": {"regression_detected": False}},
        iteration_id="exp-flag",
    )

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=hyp), \
         patch.object(_loop._applier, "apply", return_value=fake_applied), \
         patch.object(_loop._applier, "cleanup"), \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch.object(
             _loop.eval_runner, "run_v1_fitness_suite",
             return_value={
                 "bad_cases": MagicMock(),
                 "anchor_outcome": MagicMock(),
                 "shadow": MagicMock(),
             },
         ), \
         patch.object(_loop.tier_evaluator, "evaluate", return_value=fake_verdict):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-flag",
            client=_fake_client(), repo_root=repo,
        )

    assert r.decision == "keep"
    assert r.anti_hardcode_flag_for_codex is True

    # The persisted record carries the flag.
    log_path = repo / "autoloop/results/experiments.jsonl"
    rows = experiments_log.read_all(log_path)
    match = next(r for r in rows if r["iteration_id"] == "exp-flag")
    assert match.get("anti_hardcode_flag_for_codex") is True


# ---------------------------------------------------------------------
# Insertion C — gaming.detect post-eval persistence + audit surface
# ---------------------------------------------------------------------


def test_gaming_flags_persisted_to_experiments_log_and_visible_in_audit(
    tmp_path: Path,
):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)
    # No scoring_code_baseline_sha → expect a `baseline_missing` WARN.
    cfg["fitness"]["scoring_code_baseline_sha"] = None

    fake_applied = AppliedExperiment(
        iteration_id="exp-gaming",
        branch_name="autoloop/exp-gaming",
        commit_sha="abc",
        skill_file_path=repo / "server/src/main/resources/skills/discover_triage.yaml",
        backend_port=18602,
        backend_process=None,
        original_branch="master",
    )
    fake_verdict = LexicographicVerdict(
        decision="keep",
        discard_reason=None,
        layer_results=[
            LayerResult(layer=4, name="shadow_regression", passed=True,
                        reason="shadow_no_regression",
                        metrics_observed={"drop_pct": 0.0, "regression_detected": False}),
        ],
        tier_breakdown={"shadow_regression": {"regression_detected": False}},
        iteration_id="exp-gaming",
    )

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop._applier, "apply", return_value=fake_applied), \
         patch.object(_loop._applier, "cleanup"), \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch.object(
             _loop.eval_runner, "run_v1_fitness_suite",
             return_value={
                 "bad_cases": MagicMock(),
                 "anchor_outcome": MagicMock(),
                 "shadow": MagicMock(),
             },
         ), \
         patch.object(_loop.tier_evaluator, "evaluate", return_value=fake_verdict):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-gaming",
            client=_fake_client(), repo_root=repo,
        )

    assert r.decision == "keep"
    assert r.gaming_flags  # non-empty
    rule_ids = [f.get("rule_id") for f in r.gaming_flags]
    assert "scoring_code_drift.baseline_missing" in rule_ids

    # Persisted in experiments_log:
    log_path = repo / "autoloop/results/experiments.jsonl"
    rows = experiments_log.read_all(log_path)
    match = next(row for row in rows if row["iteration_id"] == "exp-gaming")
    assert match.get("gaming_flags")
    persisted_rule_ids = [f.get("rule_id") for f in match["gaming_flags"]]
    assert "scoring_code_drift.baseline_missing" in persisted_rule_ids


# ---------------------------------------------------------------------
# Backward-compat regression — S-Auto-3 IterationResult constructors
# ---------------------------------------------------------------------


def test_iteration_result_constructible_without_new_fields():
    """S-Auto-3 tests construct IterationResult(iteration_id=...) without
    the new additive fields. The new defaults (`gaming_flags=[]`,
    `anti_hardcode_flag_for_codex=False`, `content_validator_verdict=None`)
    keep that call form valid."""
    r = IterationResult(iteration_id="exp-x")
    assert r.gaming_flags == []
    assert r.anti_hardcode_flag_for_codex is False
    assert r.content_validator_verdict is None
