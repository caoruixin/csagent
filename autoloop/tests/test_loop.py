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
from types import SimpleNamespace
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


# --- anti-hardcode detector — clean propose PASSes ------------------


def test_anti_hardcode_real_detector_passes_clean_propose(tmp_path: Path):
    """S-Auto-4 real detector. A clean soft-narrative edit (no
    forbidden structural pattern) returns PASS with placeholder=False.
    """
    from autoloop.sandbox.anti_hardcode_check import anti_hardcode_check

    hyp = _hypothesis()
    res = anti_hardcode_check(hyp, config={})
    assert res.verdict == "PASS"
    assert res.placeholder is False
    assert res.rule_id is None


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


# --- adversarial fixture for S-Auto-4 — real detector rejects --------


def test_adversarial_fixture_fails_real_detection(tmp_path: Path):
    """Load-bearing placeholder → real transition regression target.
    The §1.7 red-line content (an IF-THEN decision tree + a
    `.contains(...)` literal + a UC-by-name reference) MUST be
    rejected by the S-Auto-4 detector.
    """
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)

    adversarial = Hypothesis(
        target_skill_file="server/src/main/resources/skills/discover_triage.yaml",
        target_field_path="$.procedure",
        before_value="step one\nstep two\n",
        after_value=(
            "if user.message.contains('appeal') then route to UC-H "
            "else UC-A"
        ),
        rationale="adversarial fixture for S-Auto-4",
    )

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=adversarial), \
         patch.object(_loop, "_build_baseline_summary", return_value={}):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-adv",
            client=_fake_client(), repo_root=repo,
        )

    assert r.decision == "discard"
    assert r.discard_reason is not None
    assert r.discard_reason.startswith("anti_hardcode_rejected:")


# --- S-Auto-11: eval trace persistence + infra-error detection -------


def _write_results_json(path: Path, cases: list[dict]) -> None:
    """Write a minimal eval-interactive results.json with the given cases."""
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "run_id": path.parent.name,
        "label": "test",
        "timestamp": "2026-06-01T00:00:00+00:00",
        "summary": {"total_cases": len(cases)},
        "case_results": cases,
    }
    path.write_text(json.dumps(payload), encoding="utf-8")


def _suite(results_json: Path | str | None, *, exit_code: int = 0, error_tail=None):
    """A SuiteRunResult-shaped stand-in (loop helpers use getattr)."""
    return SimpleNamespace(
        suite_name="bad_cases",
        results_dir=None,
        results_json=results_json,
        elapsed_seconds=1.0,
        exit_code=exit_code,
        error_tail=error_tail,
    )


def _ok_case(cid="c_ok"):
    return {"case_id": cid, "status": "FAIL", "stop_reason": "bot_ended",
            "escalation_reason": "user_requested", "failure_tags": [],
            "per_turn_trace": [{"turn_index": 1, "phase": "DISCOVER"}]}


def _degraded_case(cid="c_deg", kind="service_degraded"):
    if kind == "service_degraded":
        return {"case_id": cid, "status": "FAIL", "escalation_reason": "service_degraded",
                "failure_tags": [], "per_turn_trace": []}
    if kind == "contract_violation":
        return {"case_id": cid, "status": "CONTRACT_VIOLATION", "stop_reason": "contract_violation",
                "contract_violation": {"field": "active_use_case", "reason": "missing_after_turns"},
                "failure_tags": ["CONTRACT_VIOLATION:active_use_case"]}
    if kind == "error":
        return {"case_id": cid, "status": "ERROR", "failure_tags": ["ReadTimeout"]}
    raise ValueError(kind)


# ---- _case_is_infra_degraded ----


def test_case_is_infra_degraded_signals():
    assert _loop._case_is_infra_degraded(_degraded_case(kind="service_degraded"))
    assert _loop._case_is_infra_degraded(_degraded_case(kind="contract_violation"))
    assert _loop._case_is_infra_degraded(_degraded_case(kind="error"))
    assert _loop._case_is_infra_degraded(
        {"case_id": "x", "status": "FAIL", "failure_tags": ["LlmDeadlineExceeded"]}
    )
    # Negative control: a genuine fitness FAIL is NOT infra.
    assert not _loop._case_is_infra_degraded(_ok_case())
    assert not _loop._case_is_infra_degraded(
        {"case_id": "x", "status": "CONTRACT_VIOLATION",
         "contract_violation": {"field": "current_phase"}}
    )


# ---- _assess_infra_error ----


def test_assess_infra_error_empty_and_mock_inputs_not_infra():
    # Empty suite map (degenerate / config-less) and bare MagicMocks must
    # not be flagged infra-error (preserves the normal fitness path).
    assert _loop._assess_infra_error({}, {}) == (False, None)
    assert _loop._assess_infra_error({"bad_cases": MagicMock()}, {}) == (False, None)


def test_assess_infra_error_failed_suite_nonzero_exit():
    is_infra, reason = _loop._assess_infra_error(
        {"bad_cases": _suite("/does/not/matter", exit_code=2)}, {}
    )
    assert is_infra
    assert "exit=2" in reason


def test_assess_infra_error_missing_results_json_on_real_suite(tmp_path):
    missing = tmp_path / "nope.json"
    is_infra, reason = _loop._assess_infra_error(
        {"bad_cases": _suite(missing, exit_code=0)}, {}
    )
    assert is_infra
    assert "missing_results_json" in reason


def test_assess_infra_error_high_deadline_prevalence(tmp_path):
    rj = tmp_path / "results.json"
    _write_results_json(rj, [
        _degraded_case("a", "service_degraded"),
        _degraded_case("b", "contract_violation"),
        _degraded_case("c", "error"),
        _ok_case("d"),
    ])
    is_infra, reason = _loop._assess_infra_error({"bad_cases": _suite(rj)}, {})
    assert is_infra
    assert reason.startswith("infra_degradation_prevalence:3/4")


def test_assess_infra_error_genuine_fails_not_infra(tmp_path):
    # NEGATIVE CONTROL (OQ-S65.7/8 fence): a suite full of genuine fitness
    # FAILs must NOT be masked as infra-error — it must reach tier eval.
    rj = tmp_path / "results.json"
    _write_results_json(rj, [_ok_case(f"c{i}") for i in range(6)])
    assert _loop._assess_infra_error({"bad_cases": _suite(rj)}, {}) == (False, None)


def test_assess_infra_error_threshold_configurable(tmp_path):
    rj = tmp_path / "results.json"
    _write_results_json(rj, [
        _degraded_case("a", "service_degraded"), _ok_case("b"),
        _ok_case("c"), _ok_case("d"),
    ])  # 1/4 = 0.25
    assert _loop._assess_infra_error({"bad_cases": _suite(rj)}, {}) == (False, None)
    cfg = {"fitness": {"infra_error_degraded_fraction": 0.2}}
    is_infra, reason = _loop._assess_infra_error({"bad_cases": _suite(rj)}, cfg)
    assert is_infra


# ---- _persist_eval_traces ----


def test_persist_eval_traces_roundtrip(tmp_path):
    results_root = tmp_path / "runs" / "exp-9" / "eval"
    suite_dir = results_root / "bad_cases"
    rj = suite_dir / "results.json"
    case = _ok_case("c1")
    case["per_turn_trace"] = [{"turn_index": 1, "phase": "DISCOVER", "tool_calls": ["x"]}]
    _write_results_json(rj, [case])

    out = _loop._persist_eval_traces(results_root, {"bad_cases": _suite(rj)}, "exp-9")
    assert out is not None
    out_path = Path(out)
    assert out_path == results_root.parent / "eval-results.json"
    assert out_path.exists()

    persisted = json.loads(out_path.read_text(encoding="utf-8"))
    assert persisted["iteration_id"] == "exp-9"
    suite = persisted["suites"]["bad_cases"]
    assert suite["missing"] is False
    # The per-turn trace survives as a real copy (not a volatile symlink).
    ptt = suite["results"]["case_results"][0]["per_turn_trace"]
    assert ptt[0]["phase"] == "DISCOVER"


def test_persist_eval_traces_marks_missing_suite(tmp_path):
    results_root = tmp_path / "runs" / "exp-10" / "eval"
    results_root.mkdir(parents=True, exist_ok=True)
    out = _loop._persist_eval_traces(
        results_root, {"bad_cases": _suite(tmp_path / "gone.json")}, "exp-10"
    )
    persisted = json.loads(Path(out).read_text(encoding="utf-8"))
    assert persisted["suites"]["bad_cases"]["missing"] is True
    assert persisted["suites"]["bad_cases"]["results"] is None


# ---- integration: infra-error iter is decision=error, skips tier eval ----


def test_infra_error_iter_marked_error_and_skips_tier_eval(tmp_path: Path):
    repo = _make_repo(tmp_path)
    cfg = _config_for(repo)

    # All three suites point at a results.json dominated by service_degraded
    # / first-turn-abort cases -> infra-degraded eval evidence.
    rj = repo / "autoloop" / "results" / "degraded.json"
    _write_results_json(rj, [
        _degraded_case("a", "service_degraded"),
        _degraded_case("b", "contract_violation"),
        _degraded_case("c", "error"),
    ])
    suite_map = {
        "bad_cases": _suite(rj),
        "anchor_outcome": _suite(rj),
        "shadow": _suite(rj),
    }

    fake_applied = AppliedExperiment(
        iteration_id="exp-7",
        branch_name="autoloop/exp-7",
        commit_sha="abc",
        skill_file_path=repo / "server/src/main/resources/skills/discover_triage.yaml",
        backend_port=18777,
        backend_process=None,
        original_branch="master",
    )
    tier_eval_mock = MagicMock()

    with patch.object(_loop._analyzer, "analyze", return_value={}), \
         patch.object(_loop._proposer, "propose", return_value=_hypothesis()), \
         patch.object(_loop._applier, "apply", return_value=fake_applied), \
         patch.object(_loop._applier, "cleanup") as mock_cleanup, \
         patch.object(_loop, "_build_baseline_summary", return_value={}), \
         patch.object(_loop.eval_runner, "run_v1_fitness_suite", return_value=suite_map), \
         patch.object(_loop.tier_evaluator, "evaluate", tier_eval_mock):
        r = run_one_iteration(
            config=cfg, iteration_id="exp-7",
            client=_fake_client(), repo_root=repo,
        )

    assert r.decision == "error"
    assert r.infra_error is True
    assert r.infra_error_reason and r.infra_error_reason.startswith(
        "infra_degradation_prevalence:"
    )
    # The masquerade fence: a degraded iter is NEVER scored by the tier
    # evaluator (so it cannot register as a Tier-0 fitness regression).
    tier_eval_mock.assert_not_called()
    mock_cleanup.assert_called()

    # The infra-error reason is persisted in the experiments_log row.
    rows = experiments_log.read_all(repo / "autoloop/results/experiments.jsonl")
    row = next(r for r in rows if r.get("iteration_id") == "exp-7")
    assert row["infra_error"] is True
    assert row["decision"] == "error"
