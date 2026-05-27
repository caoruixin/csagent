"""Tests for the meta-agent: analyzer, proposer, lessons_compactor.

All LLM calls are mocked via a fake LLMClient. No network calls.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pytest

from autoloop.meta_agent import analyzer, lessons_compactor, proposer
from autoloop.meta_agent.llm_client import LLMClient, LLMResponse
from autoloop.meta_agent.proposer import (
    Hypothesis,
    ProposerInvalidOutputError,
    fingerprint_hypothesis,
)


_DEFAULT_CONFIG = {
    "mutable_surface": {
        "allowed_skill_files": [
            "server/src/main/resources/skills/discover_triage.yaml",
            "server/src/main/resources/skills/resolve_faq_grounded_answer.yaml",
            "server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml",
            "server/src/main/resources/skills/confirm.yaml",
            "server/src/main/resources/skills/escalate.yaml",
            "server/src/main/resources/skills/terminal.yaml",
        ],
        "allowed_field_paths": [
            "$.procedure",
            "$.grounding_instruction",
            "$.escalation_policy",
            "$.critical_steps[*].desc",
        ],
    },
    "meta_agent": {
        "provider": "anthropic",
        "model": "claude-opus-4-7",
        "temperature": 0.3,
        "max_tokens": 4096,
        "api_key_env": "AUTOLOOP_META_LLM_API_KEY",
        "base_url_env": "AUTOLOOP_META_LLM_BASE_URL",
    },
}


class FakeClient:
    """Fake LLMClient for tests; queues responses and records prompts."""

    def __init__(self, responses: list[str]):
        self._responses = list(responses)
        self.calls: list[tuple[str, str]] = []

    def is_configured(self) -> bool:
        return True

    def chat(self, system: str, user: str) -> LLMResponse:
        if not self._responses:
            raise RuntimeError("FakeClient exhausted")
        text = self._responses.pop(0)
        self.calls.append((system, user))
        return LLMResponse(text=text, raw=None)


# --- analyzer --------------------------------------------------------


def test_analyzer_happy_path():
    client = FakeClient([
        json.dumps({
            "skills_critical_steps_advisory_fail": {
                "discover_triage": {
                    "step-1": {"fail_count": 3, "in_cases": ["bc-1", "bc-2"]}
                }
            },
            "bad_cases_regressing": {
                "bc-7": {"primary_uc": "UC-A", "failure_shape": "uc-carry-over"}
            },
            "anchor_outcome_closure_criterion_fails": {},
            "summary": "drift carry-over recurs on discover_triage"
        })
    ])
    taxonomy = analyzer.analyze(
        baseline_results_summary={"bad_cases": {"passed_cases": 10}},
        lessons_md="",
        recent_iterations=[],
        client=client,
    )
    assert taxonomy["summary"].startswith("drift")
    assert "discover_triage" in taxonomy["skills_critical_steps_advisory_fail"]


def test_analyzer_unparseable_output_falls_back_to_empty():
    client = FakeClient(["not json at all"])
    taxonomy = analyzer.analyze(
        baseline_results_summary={},
        lessons_md="",
        recent_iterations=[],
        client=client,
    )
    assert taxonomy["skills_critical_steps_advisory_fail"] == {}
    assert "fallback" in taxonomy["summary"]


def test_analyzer_summary_scrubs_case_ids():
    """Summary must not contain case_ids even if the LLM leaks them."""
    client = FakeClient([
        json.dumps({
            "skills_critical_steps_advisory_fail": {},
            "bad_cases_regressing": {},
            "anchor_outcome_closure_criterion_fails": {},
            "summary": "case bc-7 and UC-A-002 had drift"
        })
    ])
    taxonomy = analyzer.analyze(
        baseline_results_summary={},
        lessons_md="",
        recent_iterations=[],
        client=client,
    )
    assert "bc-7" not in taxonomy["summary"]
    assert "UC-A-002" not in taxonomy["summary"]


def test_analyzer_strips_markdown_fences():
    client = FakeClient(["```json\n" + json.dumps({"summary": "ok"}) + "\n```"])
    taxonomy = analyzer.analyze({}, "", [], client=client)
    assert taxonomy["summary"] == "ok"


def test_analyzer_build_baseline_summary_filters_shadow_to_aggregate(tmp_path: Path):
    """Shadow per-case detail must NOT appear in baseline_summary."""
    shadow_dir = tmp_path / "shadow"
    shadow_dir.mkdir()
    (shadow_dir / "results.json").write_text(json.dumps({
        "case_results": [
            {"case_id": "shadow-1", "case_passed": True},
            {"case_id": "shadow-2", "case_passed": False, "failure_tags": ["secret"]},
        ]
    }))
    summary = analyzer.build_baseline_summary({"shadow": shadow_dir / "results.json"})
    # Aggregates appear:
    assert summary["shadow"]["passed_cases"] == 1
    assert summary["shadow"]["failed_cases"] == 1
    # Per-case detail does NOT:
    assert "per_case" not in summary["shadow"]
    blob = json.dumps(summary)
    assert "shadow-1" not in blob
    assert "shadow-2" not in blob
    assert "secret" not in blob


# --- proposer --------------------------------------------------------


_FIXTURE_HYPOTHESIS_JSON = json.dumps({
    "target_skill_file": "server/src/main/resources/skills/discover_triage.yaml",
    "target_field_path": "$.procedure",
    "before_value": "old procedure",
    "after_value": "new procedure with revised guidance",
    "rationale": "address drift carry-over per analyzer taxonomy",
})


def test_proposer_happy_path(tmp_path: Path, monkeypatch):
    """Mock the disk read for skill files so proposer can build user prompt."""
    # Patch _REPO_ROOT to tmp_path so propose can find (or not find) files.
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    client = FakeClient([_FIXTURE_HYPOTHESIS_JSON])
    hyp = proposer.propose(
        taxonomy={},
        lessons="",
        recent_iterations=[],
        client=client,
        config=_DEFAULT_CONFIG,
    )
    assert hyp.target_skill_file.endswith("discover_triage.yaml")
    assert hyp.target_field_path == "$.procedure"
    assert hyp.fingerprint
    assert hyp.attempts_used == 1


def test_proposer_retries_on_invalid_json(monkeypatch, tmp_path: Path):
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    client = FakeClient(["not json", "still not json", _FIXTURE_HYPOTHESIS_JSON])
    hyp = proposer.propose(
        taxonomy={}, lessons="", recent_iterations=[],
        client=client, config=_DEFAULT_CONFIG,
    )
    assert hyp.attempts_used == 3


def test_proposer_3_invalid_attempts_raises(monkeypatch, tmp_path: Path):
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    client = FakeClient(["bad1", "bad2", "bad3"])
    with pytest.raises(ProposerInvalidOutputError) as exc:
        proposer.propose(
            taxonomy={}, lessons="", recent_iterations=[],
            client=client, config=_DEFAULT_CONFIG,
        )
    assert exc.value.attempts == 3


def test_proposer_rejects_out_of_surface_target(monkeypatch, tmp_path: Path):
    """A target_skill_file NOT in mutable_surface → retry; if always
    bad, raise.
    """
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    bad = json.dumps({
        "target_skill_file": "server/src/main/java/MyClass.java",
        "target_field_path": "$.procedure",
        "before_value": "x",
        "after_value": "y",
        "rationale": "z",
    })
    client = FakeClient([bad, bad, bad])
    with pytest.raises(ProposerInvalidOutputError):
        proposer.propose(
            taxonomy={}, lessons="", recent_iterations=[],
            client=client, config=_DEFAULT_CONFIG,
        )


def test_proposer_rejects_out_of_surface_field(monkeypatch, tmp_path: Path):
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    bad = json.dumps({
        "target_skill_file": "server/src/main/resources/skills/discover_triage.yaml",
        "target_field_path": "$.applicable_use_cases",
        "before_value": "x",
        "after_value": "y",
        "rationale": "z",
    })
    client = FakeClient([bad, bad, bad])
    with pytest.raises(ProposerInvalidOutputError):
        proposer.propose(
            taxonomy={}, lessons="", recent_iterations=[],
            client=client, config=_DEFAULT_CONFIG,
        )


def test_proposer_accepts_concrete_critical_steps_index(monkeypatch, tmp_path: Path):
    """`$.critical_steps[2].desc` matches `$.critical_steps[*].desc`."""
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    blob = json.dumps({
        "target_skill_file": "server/src/main/resources/skills/discover_triage.yaml",
        "target_field_path": "$.critical_steps[2].desc",
        "before_value": "old desc",
        "after_value": "new desc",
        "rationale": "step 2 was advisory-failing",
    })
    client = FakeClient([blob])
    hyp = proposer.propose(
        taxonomy={}, lessons="", recent_iterations=[],
        client=client, config=_DEFAULT_CONFIG,
    )
    assert hyp.target_field_path == "$.critical_steps[2].desc"


def test_proposer_rejects_identical_before_after(monkeypatch, tmp_path: Path):
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    blob = json.dumps({
        "target_skill_file": "server/src/main/resources/skills/discover_triage.yaml",
        "target_field_path": "$.procedure",
        "before_value": "same",
        "after_value": "same",
        "rationale": "no-op",
    })
    client = FakeClient([blob, blob, blob])
    with pytest.raises(ProposerInvalidOutputError):
        proposer.propose({}, "", [], client=client, config=_DEFAULT_CONFIG)


def test_proposer_fingerprint_deterministic():
    hyp1 = Hypothesis(
        target_skill_file="x.yaml",
        target_field_path="$.procedure",
        before_value="a",
        after_value="b",
        rationale="r",
    )
    hyp2 = Hypothesis(
        target_skill_file="x.yaml",
        target_field_path="$.procedure",
        before_value="DIFFERENT_BEFORE",
        after_value="b",
        rationale="DIFFERENT_RATIONALE",
    )
    fp1 = fingerprint_hypothesis(hyp1)
    fp2 = fingerprint_hypothesis(hyp2)
    assert fp1 == fp2  # before / rationale excluded from fingerprint
    hyp3 = Hypothesis(
        target_skill_file="x.yaml",
        target_field_path="$.procedure",
        before_value="a",
        after_value="DIFFERENT",
        rationale="r",
    )
    assert fingerprint_hypothesis(hyp3) != fp1


# --- lessons_compactor ----------------------------------------------


def test_lessons_compactor_happy_path():
    client = FakeClient(["""## Lesson L-2026-05-28-001

**Window**: iterations exp-1 through exp-10 (K=10)
**Target observation**: 8/10 on discover_triage.procedure discarded
**Pattern**: enumeration-style edits rejected
**Heuristic for future propose**: prefer narrative procedural prose
**Affected Skill × field**: discover_triage.procedure
"""])
    section = lessons_compactor.compact(
        recent_iterations=[
            {"iteration_id": f"exp-{i}", "decision": "discard"}
            for i in range(10)
        ],
        current_lessons="",
        client=client,
    )
    assert section.startswith("## Lesson L-")
    assert "narrative procedural" in section


def test_lessons_compactor_scrubs_case_ids():
    """The defensive scrub removes case_ids the LLM may leak into the body."""
    client = FakeClient(["""## Lesson L-2026-05-28-001

**Window**: iterations exp-1 through exp-10 (K=10)
**Target observation**: bc-7 and UC-A-002 regressed
**Pattern**: ...
**Heuristic for future propose**: ...
**Affected Skill × field**: discover_triage.procedure
"""])
    section = lessons_compactor.compact(
        recent_iterations=[],
        current_lessons="",
        client=client,
    )
    assert "bc-7" not in section
    assert "UC-A-002" not in section


def test_lessons_compactor_renumbers_counter():
    """If counter override is given, the L-... line is rewritten."""
    client = FakeClient(["""## Lesson L-2026-01-01-999

**Window**: ...
**Target observation**: ...
**Pattern**: ...
**Heuristic for future propose**: ...
**Affected Skill × field**: x
"""])
    section = lessons_compactor.compact(
        recent_iterations=[], current_lessons="",
        client=client, next_counter=7,
    )
    assert "-007" in section
    assert "-999" not in section


def test_lessons_compactor_non_section_response_fallback():
    client = FakeClient(["here is some prose that is not a lesson section"])
    section = lessons_compactor.compact(
        recent_iterations=[], current_lessons="",
        client=client, next_counter=1,
    )
    assert section.startswith("## Lesson L-")
    assert "unparseable" in section
