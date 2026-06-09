"""Smoke tests for `autoloop.cli`.

Each subcommand's `--help` must exit 0 and print help. The `check`
subcommand must exit 0 against the known-good shipped `config.yaml`
and exit 1 against a config that names a non-existent Skill file.
"""

from __future__ import annotations

import json
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


def test_dry_run_subcommand_invokes_loop() -> None:
    """S-Auto-3 wires dry-run to the loop orchestrator. Without
    AUTOLOOP_META_LLM_API_KEY set, the first iteration short-circuits
    to `decision=error` (the LLM client can't be built), but the
    output proves the orchestrator was invoked — distinguishing
    S-Auto-3's real wiring from the S-Auto-1 placeholder banner.
    """
    env = os.environ.copy()
    # Force LLM unconfigured so we don't hit the network in CI.
    env.pop("AUTOLOOP_META_LLM_API_KEY", None)
    res = subprocess.run(
        [sys.executable, "-m", "autoloop", "dry-run"],
        cwd=str(REPO_ROOT),
        capture_output=True,
        text=True,
        env=env,
    )
    # The S-Auto-1 placeholder printed "S-Auto-3 territory" and exited 1.
    # The S-Auto-3 wiring prints the iteration banner.
    assert "S-Auto-3 territory" not in res.stdout
    assert "starting" in res.stdout or "iteration" in res.stdout.lower()


# --- S-Y1.5 (P0-C): dry-run threads the pilot card to the proposer ---


_RESOLVE_FAQ_YAML = """\
name: resolve_faq_grounded_answer
description: x
applicable_phases: [RESOLVE]
applicable_use_cases: [UC-A]
tools_required:
  - search_knowledge
required_context_keys: []
max_tool_steps: 5
allow_interim_message: false
valid_terminal_outcomes: [resolved]
procedure: |
  Search the knowledge base and answer with grounded evidence.
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


class _FakeClient:
    """Queued-response fake LLMClient capturing (system, user) prompts."""

    def __init__(self, responses):
        from autoloop.meta_agent.llm_client import LLMResponse

        self._responses = list(responses)
        self._LLMResponse = LLMResponse
        self.calls = []

    def is_configured(self) -> bool:
        return True

    def chat(self, system, user):
        text = self._responses.pop(0)
        self.calls.append((system, user))
        return self._LLMResponse(text=text, raw=None)


def test_dry_run_with_pilot_config_threads_targets_and_picks_phase_correct_skill(tmp_path):
    """`run --dry-run` under a populated pilot config serializes
    PILOT_PRIMARY_TARGETS into the propose prompt and lets the proposer
    pick a phase-correct skill (resolve_faq_grounded_answer.yaml).

    In-process (a real LLM is unavailable in CI): drives the real
    analyzer + proposer with a fake client so the proposer prompt and the
    picked skill are asserted deterministically. The real-LLM `--dry-run
    -n 2` close checklist covers the rationale-quality dimension.
    """
    from autoloop import loop as _loop
    from autoloop.loop import run_one_iteration
    from autoloop.meta_agent import proposer as _proposer

    repo = tmp_path / "repo"
    repo.mkdir()
    subprocess.run(["git", "init"], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "config", "user.email", "t@t"], cwd=repo, check=True, capture_output=True)
    subprocess.run(["git", "config", "user.name", "T"], cwd=repo, check=True, capture_output=True)
    skill_rel = "server/src/main/resources/skills/resolve_faq_grounded_answer.yaml"
    skill = repo / skill_rel
    skill.parent.mkdir(parents=True, exist_ok=True)
    skill.write_text(_RESOLVE_FAQ_YAML, encoding="utf-8")
    (repo / "autoloop/results").mkdir(parents=True, exist_ok=True)

    cfg = {
        "mutable_surface": {
            "allowed_skill_files": [skill_rel],
            "allowed_field_paths": [
                "$.procedure",
                "$.grounding_instruction",
                "$.escalation_policy",
                "$.critical_steps[*].desc",
            ],
        },
        "meta_agent": {"provider": "anthropic", "model": "x", "temperature": 0.0,
                       "max_tokens": 100, "api_key_env": "AUTOLOOP_META_LLM_API_KEY",
                       "base_url_env": "AUTOLOOP_META_LLM_BASE_URL"},
        "paths": {"experiments_log": "autoloop/results/experiments.jsonl",
                  "lessons_log": "autoloop/results/lessons.md",
                  "runs_dir": "autoloop/results/runs/"},
        "lessons": {"compaction_window_k": 10, "recent_iterations_for_propose": 5,
                    "enabled": False},
        "fitness": {"baseline_dir": "eval_interactive/results/<PLACEHOLDER>",
                    "suites": [{"name": "bad_cases", "path": "x", "parallel": 1}]},
        "pilot": {
            "schema_version": 1, "active_sprint": "S-Y2",
            "primary_targets": ["cs_uc_a_no_ad_id_ad_specific", "cs_uc_a_loaded_listing"],
            "anti_kill_control": ["cs_uc_a_generic_policy_question"],
            "tier2_neighbors": ["cs_uc_a_lookup_failed"],
            "phase_hint": ["DISCOVER", "RESOLVE"], "use_case_hint": ["UC-A"],
        },
    }

    analyzer_resp = json.dumps({"summary": "s"})
    proposer_resp = json.dumps({
        "target_skill_file": skill_rel,
        "target_field_path": "$.procedure",
        "before_value": "Search the knowledge base and answer with grounded evidence.\n",
        "after_value": "Search the knowledge base and confirm the entity context, then answer with grounded evidence.",
        "rationale": "Guide the agent to verify entity context before answering UC-A FAQs.",
    })
    fake = _FakeClient([analyzer_resp, proposer_resp])

    # Proposer reads skill YAML from its module-global repo root; point it
    # at the fake repo so before/after materialization is self-contained.
    monkeypatch_repo = _proposer._REPO_ROOT
    _proposer._REPO_ROOT = repo
    try:
        r = run_one_iteration(
            config=cfg, iteration_id="exp-1",
            dry_run=True, client=fake, repo_root=repo,
        )
    finally:
        _proposer._REPO_ROOT = monkeypatch_repo

    # Proposer picked the phase-correct skill:
    assert r.hypothesis is not None
    assert r.hypothesis.target_skill_file.endswith("resolve_faq_grounded_answer.yaml")
    # The pilot card reached the propose prompt (2nd chat == proposer):
    proposer_user_prompt = fake.calls[1][1]
    assert "PILOT_PRIMARY_TARGETS" in proposer_user_prompt
    assert "cs_uc_a_no_ad_id_ad_specific" in proposer_user_prompt
    assert "SKILL_PHASE_USECASE_MAP" in proposer_user_prompt
    # Dry-run hypothesis.json carries the forensic pilot snapshot:
    hyp_json = json.loads(
        (repo / "autoloop/results/runs/exp-1/hypothesis.json").read_text(encoding="utf-8")
    )
    assert hyp_json["pilot_snapshot"]["schema_version"] == 1
    assert hyp_json["lessons_enabled"] is False
