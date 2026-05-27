"""Tests for the top-level loop orchestrator.

All external systems are mocked: subprocess (mvn, git), the LLM
client, eval_runner, baseline_loader.load. We verify the state
machine: dry-run short-circuits at step 5; sandbox-reject
short-circuits earlier; mid-iter crashes route to decision="error"
with cleanup() called; full happy-path produces a verdict + memory
writes.
"""

from __future__ import annotations

import json
import subprocess
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from autoloop import loop as _loop
from autoloop.loop import IterationResult, run_iterations, run_one_iteration
from autoloop.memory import experiments_log, iterations_index
from autoloop.meta_agent.llm_client import LLMResponse
from autoloop.meta_agent.proposer import Hypothesis, fingerprint_hypothesis
from autoloop.sandbox.applier import AppliedExperiment
from autoloop.scoring.baseline_loader import BaselineSnapshot
from autoloop.scoring.tier_evaluator import LayerResult, LexicographicVerdict


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
        },
    }


def _make_repo(tmp_path: Path) -> Path:
    repo = tmp_path / "repo"
    repo.mkdir()
    subprocess.run(["git", "init"], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "config", "user.email", "t@t"], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "config", "user.name", "T"], cwd=repo, check=True, capture_output=True)
    skill = repo / "server/src/main/resources/skills/discover_triage.yaml"
    skill.parent.mkdir(parents=True, exist_ok=True)
    skill.write_text(_FIXTURE_YAML, encoding="utf-8")
    (repo / "autoloop/results").mkdir(parents=True, exist_ok=True)
    subprocess.run(["git", "add", "."], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "commit", "-m", "init"], cwd=repo, check=True, capture_output=True)
    return repo


def _hypothesis() -> Hypothesis:
    hyp = Hypothesis(
        target_skill_file="server/src/main/resources/skills/discover_triage.yaml",
        target_field_path="$.procedure",
        before_value="step one\nstep two\n",
        after_value="step one revised\nstep two",
        rationale="fix drift",
    )
    hyp.fingerprint = fingerprint_hypothesis(hyp)
    return hyp


def _fake_client():
    """A fake LLMClient that the loop accepts via the `client=` kw."""

    class FakeClient:
        def __init__(self):
            self.calls = 0

        def is_configured(self) -> bool:
            return True

        def chat(self, system, user):
            self.calls += 1
            return LLMResponse(text="{}", raw=None)

    return FakeClient()


def _empty_baseline(repo: Path) -> BaselineSnapshot:
    return BaselineSnapshot(
        baseline_run_id="<test>",
        baseline_dir=repo,
        snapshots={},
        tier0_baseline={},
        captured_at="2026-05-28T00:00:00+00:00",
    )


# --- dry-run short-circuit ------------------------------------------


def test_dry_run_short_circuits_before_apply(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)
    fake_apply = MagicMock()

    with patch.object(_loop._analyzer, "analyze", return_value={"summary": "s"}), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop._applier, "apply", side_effect=fake_apply), \
         patch.object(_loop, "_build_baseline_summary", return_value={}):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-1",
            dry_run=True, client=_fake_client(), repo_root=repo,
        )
    assert r.decision == "discard"
    assert r.discard_reason == "dry_run_no_apply"
    fake_apply.assert_not_called()
    # Dry-run record written:
    runs_dir = repo / "autoloop" / "results" / "runs" / "exp-1"
    assert (runs_dir / "dry_run_record.json").exists()
    # No long-term memory writes:
    assert not (repo / "autoloop/results/experiments.jsonl").exists()


# --- sandbox-reject short-circuit ----------------------------------


def test_sandbox_reject_short_circuits_before_apply(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)
    bad_hyp = _hypothesis()
    bad_hyp.target_field_path = "$.applicable_use_cases"

    fake_apply = MagicMock()
    with patch.object(_loop._analyzer, "analyze", return_value={"summary": "s"}), \
         patch.object(_loop._proposer, "propose", return_value=bad_hyp), \
         patch.object(_loop._applier, "apply", side_effect=fake_apply), \
         patch.object(_loop, "_build_baseline_summary", return_value={}):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-2",
            client=_fake_client(), repo_root=repo,
        )
    assert r.decision == "discard"
    assert r.discard_reason.startswith("sandbox_rejected:") or \
           r.discard_reason.startswith("sandbox_rejected")
    fake_apply.assert_not_called()


# --- anti-hardcode placeholder always-PASSes -----------------------


def test_anti_hardcode_placeholder_always_passes(tmp_path: Path):
    """The S-Auto-3 placeholder anti_hardcode_check must return PASS
    so the iteration proceeds to apply. (S-Auto-4 swaps real impl.)
    """
    from autoloop.sandbox.anti_hardcode_check import anti_hardcode_check

    hyp = _hypothesis()
    res = anti_hardcode_check(hyp, config={})
    assert res.decision == "PASS"
    assert res.placeholder is True


# --- proposer JSON-invalid → discard with reason -------------------


def test_proposer_invalid_json_routes_to_discard_with_reason(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)
    from autoloop.meta_agent.proposer import ProposerInvalidOutputError

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose",
                      side_effect=ProposerInvalidOutputError(
                          "x", attempts=3, last_output="")), \
         patch.object(_loop, "_build_baseline_summary", return_value={}):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-3",
            client=_fake_client(), repo_root=repo,
        )
    assert r.decision == "discard"
    assert r.discard_reason == "proposer_llm_returned_invalid_json"


# --- mid-iter crash → error + cleanup --------------------------------


def test_mid_iter_apply_crash_routes_to_error_and_calls_cleanup(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)

    cleanup_mock = MagicMock()
    fake_applied = AppliedExperiment(
        iteration_id="exp-4",
        branch_name="autoloop/exp-4",
        commit_sha="abc",
        skill_file_path=repo / "x",
        backend_port=18099,
        backend_process=None,
        original_branch="master",
    )

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop._applier, "apply", return_value=fake_applied), \
         patch.object(_loop._applier, "cleanup", cleanup_mock), \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch.object(_loop.eval_runner, "run_v1_fitness_suite",
                      side_effect=RuntimeError("eval explosion")):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-4",
            client=_fake_client(), repo_root=repo,
        )
    assert r.decision == "error"
    assert "eval explosion" in (r.error or "")
    cleanup_mock.assert_called()


def test_loop_continues_after_iteration_error(tmp_path: Path):
    """run_iterations must complete all N iterations even if one
    raises mid-way.
    """
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)

    call_count = {"n": 0}

    def _maybe_explode(*args, **kwargs):
        call_count["n"] += 1
        if call_count["n"] == 1:
            raise RuntimeError("first iter explodes")
        return {"summary": "ok"}

    with patch.object(_loop._analyzer, "analyze", side_effect=_maybe_explode), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop, "_build_baseline_summary", return_value={}):
        rs = run_iterations(
            config=cfg, count=2,
            dry_run=True, client=_fake_client(), repo_root=repo,
        )
    assert len(rs) == 2
    assert rs[0].decision == "error"
    assert rs[1].decision == "discard"  # dry-run discard, not error


# --- happy-path full iteration ---------------------------------------


def test_happy_path_keep_full_pipeline(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)
    cfg["fitness"]["baseline_dir"] = "eval_interactive/results/<PLACEHOLDER>"  # forces empty baseline

    fake_applied = AppliedExperiment(
        iteration_id="exp-5",
        branch_name="autoloop/exp-5",
        commit_sha="abc",
        skill_file_path=repo / "server/src/main/resources/skills/discover_triage.yaml",
        backend_port=18555,
        backend_process=None,
        original_branch="master",
    )
    fake_verdict = LexicographicVerdict(
        decision="keep",
        discard_reason=None,
        layer_results=[
            LayerResult(layer=0, name="tier0_safety", passed=True, reason="ok"),
            LayerResult(layer=1, name="tier1_outcome", passed=True, reason="ok"),
            LayerResult(layer=2, name="tier2_critical_flow", passed=True, reason="ok"),
            LayerResult(layer=3, name="improvement_threshold", passed=True,
                        reason="improvement_threshold_met"),
            LayerResult(layer=4, name="shadow_regression", passed=True,
                        reason="shadow_no_regression",
                        metrics_observed={"drop_pct": 0.0, "regression_detected": False}),
        ],
        tier_breakdown={"shadow_regression": {"regression_detected": False}},
        iteration_id="exp-5",
    )

    with patch.object(_loop._analyzer, "analyze", return_value={"summary": "s"}), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop._applier, "apply", return_value=fake_applied), \
         patch.object(_loop._applier, "cleanup") as mock_cleanup, \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch.object(_loop.eval_runner, "run_v1_fitness_suite",
                      return_value={"bad_cases": MagicMock(), "anchor_outcome": MagicMock(), "shadow": MagicMock()}), \
         patch.object(_loop.tier_evaluator, "evaluate", return_value=fake_verdict):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-5",
            client=_fake_client(), repo_root=repo,
        )
    assert r.decision == "keep"
    assert r.applied is not None
    mock_cleanup.assert_called()
    # Memory layers written:
    log_path = repo / "autoloop/results/experiments.jsonl"
    assert log_path.exists()
    rows = experiments_log.read_all(log_path)
    assert any(row.get("iteration_id") == "exp-5" for row in rows)


# --- branch tag --------------------------------------------------------


def test_branch_tag_keep_format(tmp_path: Path):
    """When a kept iteration completes the loop calls git tag
    autoloop/keep-N. We capture the subprocess call via the helper.
    """
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)

    fake_applied = AppliedExperiment(
        iteration_id="exp-7",
        branch_name="autoloop/exp-7",
        commit_sha="abc",
        skill_file_path=repo / "x",
        backend_port=18077,
        backend_process=None,
        original_branch="master",
    )
    fake_verdict = LexicographicVerdict(
        decision="keep",
        discard_reason=None,
        layer_results=[],
        tier_breakdown={},
        iteration_id="exp-7",
    )
    git_calls: list[list[str]] = []

    def _capture_git_tag(*args, **kwargs):
        if "args" in kwargs:
            git_calls.append(kwargs["args"])
        elif args:
            git_calls.append(args[0] if isinstance(args[0], list) else list(args))

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop._applier, "apply", return_value=fake_applied), \
         patch.object(_loop._applier, "cleanup"), \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch.object(_loop.eval_runner, "run_v1_fitness_suite", return_value={}), \
         patch.object(_loop.tier_evaluator, "evaluate", return_value=fake_verdict), \
         patch("autoloop.loop.subprocess.run", side_effect=_capture_git_tag):
        run_one_iteration(
            config=cfg, iteration_id="exp-7",
            client=_fake_client(), repo_root=repo,
        )
    flat = [a for call in git_calls for a in (call if isinstance(call, list) else [])]
    joined = " ".join(str(a) for a in flat)
    assert "autoloop/keep-7" in joined


# --- shadow firewall regression test in serialized log -------------


def test_shadow_firewall_holds_in_experiments_log(tmp_path: Path):
    """The serialized iteration record in experiments_log must NOT
    contain per-case shadow keys. We run the full pipeline with a
    verdict whose tier_breakdown carries only aggregate Layer 4 keys
    (as the default `evaluate` API surface guarantees).
    """
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)

    fake_applied = AppliedExperiment(
        iteration_id="exp-9",
        branch_name="autoloop/exp-9",
        commit_sha="abc",
        skill_file_path=repo / "x",
        backend_port=18099,
        backend_process=None,
        original_branch="master",
    )
    fake_verdict = LexicographicVerdict(
        decision="keep",
        discard_reason=None,
        layer_results=[
            LayerResult(
                layer=4, name="shadow_regression", passed=True,
                reason="shadow_no_regression",
                metrics_observed={
                    "drop_pct": 0.5, "regression_detected": False,
                    "baseline_pass_rate": 0.95, "current_pass_rate": 0.945,
                },
            ),
        ],
        tier_breakdown={
            "shadow_regression": {
                "drop_pct": 0.5,
                "regression_detected": False,
                "baseline_pass_rate": 0.95,
                "current_pass_rate": 0.945,
            },
        },
        iteration_id="exp-9",
    )

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop._applier, "apply", return_value=fake_applied), \
         patch.object(_loop._applier, "cleanup"), \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch.object(_loop.eval_runner, "run_v1_fitness_suite", return_value={}), \
         patch.object(_loop.tier_evaluator, "evaluate", return_value=fake_verdict):
        run_one_iteration(
            config=cfg, iteration_id="exp-9",
            client=_fake_client(), repo_root=repo,
        )
    log_text = (repo / "autoloop/results/experiments.jsonl").read_text(encoding="utf-8")
    # Sentinel keys that would indicate per-case shadow leakage:
    assert "per_case_failures" not in log_text
    assert "failure_tags" not in log_text


# --- adversarial fixture for S-Auto-4 (kept for future use) ---------


def test_adversarial_fixture_passes_placeholder_anti_hardcode(tmp_path: Path):
    """Fixture: an adversarial hypothesis with §1.7 red-line content
    (an IF-THEN decision tree referencing a UC by name). The
    S-Auto-3 placeholder always-PASS hook lets it through; S-Auto-4
    real impl SHOULD reject it. This test asserts placeholder
    behavior (so S-Auto-4 dev knows what to make change).
    """
    from autoloop.sandbox.anti_hardcode_check import anti_hardcode_check

    adversarial = Hypothesis(
        target_skill_file="server/src/main/resources/skills/discover_triage.yaml",
        target_field_path="$.procedure",
        before_value="x",
        after_value="if user.message contains 'appeal' then route to UC-H else UC-A",
        rationale="adversarial fixture for S-Auto-4",
    )
    res = anti_hardcode_check(adversarial, config={})
    # S-Auto-3 placeholder: PASS. S-Auto-4 should change this to FAIL.
    assert res.decision == "PASS"
    assert res.placeholder is True
