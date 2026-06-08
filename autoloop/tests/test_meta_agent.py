"""Tests for the meta-agent: analyzer, proposer, lessons_compactor.

All LLM calls are mocked via a fake LLMClient. No network calls.
"""

from __future__ import annotations

import json
import re
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


# --- S-Y1.5: P0-A / P0-B / P0-C / P1 meta-agent input shaping --------


def _recent_iter_with_tier_breakdown() -> dict:
    return {
        "iteration_id": "exp-9",
        "hypothesis": {
            "target_skill_file": "server/src/main/resources/skills/confirm.yaml",
            "target_field_path": "$.escalation_policy",
        },
        "decision": "discard",
        "discard_reason": "tier0_escalation_compliance_failed_on_a_case",
        "verdict": {
            "tier_breakdown": {
                "tier0_safety": {"new_violation_case_marker": "MARKER_CS11S01"},
                "tier1_outcome": {"bad_cases_passed": 4},
            }
        },
    }


def test_recent_iterations_passthrough_includes_tier_breakdown(monkeypatch, tmp_path: Path):
    """P0-A: both the proposer and analyzer serialize each recent
    iteration's `verdict.tier_breakdown`, not just the discard_reason."""
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    recent = [_recent_iter_with_tier_breakdown()]

    pclient = FakeClient([_FIXTURE_HYPOTHESIS_JSON])
    proposer.propose({}, "", recent, client=pclient, config=_DEFAULT_CONFIG)
    p_user = pclient.calls[0][1]
    assert "tier_breakdown" in p_user
    assert "MARKER_CS11S01" in p_user

    aclient = FakeClient([json.dumps({"summary": "ok"})])
    analyzer.analyze({}, "", recent, client=aclient)
    a_user = aclient.calls[0][1]
    assert "tier_breakdown" in a_user
    assert "MARKER_CS11S01" in a_user


def test_candidate_results_passthrough_filters_shadow_per_case():
    """P0-B firewall: the analyzer sees candidate per-suite results, but
    shadow per-case detail is aggregate-only — no shadow case_id, tag, or
    failure_shape may reach the serialized prompt. This test FAILS if the
    shadow branch is removed from analyzer._summarize_suite_cases."""
    raw_candidate = [
        {
            "iteration_id": "exp-66",
            "suites": {
                "shadow": {
                    "results": {
                        "case_results": [
                            {
                                "case_id": "cs59s01_uc_d_empty_form_account_recovery",
                                "case_passed": False,
                                "failure_tags": ["SHADOW_SECRET_TAG"],
                                "failure_shape": "shadow-secret-shape",
                            },
                            {
                                "case_id": "cs11s01_uc_d_two_emails_one_account",
                                "case_passed": True,
                            },
                        ]
                    },
                    "missing": False,
                },
                "bad_cases": {
                    "results": {
                        "case_results": [
                            {
                                "case_id": "cs_uc_a_no_ad_id_ad_specific",
                                "case_passed": False,
                                "primary_uc": "UC-A",
                                "failure_shape": "answers-without-verify",
                            }
                        ]
                    },
                    "missing": False,
                },
            },
        }
    ]
    client = FakeClient([json.dumps({"summary": "ok"})])
    analyzer.analyze({}, "", [], client=client, recent_candidate_results=raw_candidate)
    user_prompt = client.calls[0][1]

    # Shadow per-case detail MUST NOT leak (the firewall):
    assert "cs59s01_uc_d_empty_form_account_recovery" not in user_prompt
    assert "cs11s01_uc_d_two_emails_one_account" not in user_prompt
    assert "SHADOW_SECRET_TAG" not in user_prompt
    assert "shadow-secret-shape" not in user_prompt
    assert "cs59s" not in user_prompt
    assert "case_specs_shadow" not in user_prompt
    # cs<NN>s<NN> shadow-shape regex blacklist — no shadow case-id pattern:
    assert re.search(r"cs\d+s\d+", user_prompt) is None
    # But the candidate block IS present, with shadow aggregate counts + the
    # non-shadow per-case detail:
    assert "CANDIDATE_RESULTS_SUMMARY" in user_prompt
    assert "cs_uc_a_no_ad_id_ad_specific" in user_prompt
    assert '"failed_cases": 1' in user_prompt  # shadow aggregate survives


def _pilot_card() -> dict:
    return {
        "schema_version": 1,
        "active_sprint": "S-Y2",
        "primary_targets": ["cs_uc_a_no_ad_id_ad_specific", "cs_uc_a_loaded_listing"],
        "anti_kill_control": ["cs_uc_a_generic_policy_question"],
        "tier2_neighbors": ["cs_uc_a_lookup_failed"],
        "phase_hint": ["DISCOVER", "RESOLVE"],
        "use_case_hint": ["UC-A"],
    }


def test_pilot_primary_targets_block_serialized_in_propose_prompt(monkeypatch, tmp_path: Path):
    """P0-C: the pilot card + skill phase/UC map reach the propose user
    prompt, and the labels-only directive is present in the system prompt."""
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    skill_map = {
        "server/src/main/resources/skills/resolve_faq_grounded_answer.yaml": {
            "applicable_phases": ["RESOLVE"],
            "applicable_use_cases": ["UC-A"],
        }
    }
    client = FakeClient([_FIXTURE_HYPOTHESIS_JSON])
    proposer.propose(
        {}, "", [],
        client=client, config=_DEFAULT_CONFIG,
        pilot=_pilot_card(), skill_phase_usecase_map=skill_map,
    )
    system_prompt, user_prompt = client.calls[0]
    assert "PILOT_PRIMARY_TARGETS" in user_prompt
    assert "cs_uc_a_no_ad_id_ad_specific" in user_prompt
    assert "SKILL_PHASE_USECASE_MAP" in user_prompt
    # Labels-only directive (verbatim) in the propose.txt system prompt.
    # Normalize whitespace since the sentence wraps across list-item lines.
    normalized_system = " ".join(system_prompt.split())
    assert "evaluation bookkeeping labels" in normalized_system
    assert (
        "Do NOT mention, encode, paraphrase, or create rules around these "
        "IDs or their literal fixture wording in any proposed after_value"
    ) in normalized_system


def test_lessons_md_not_in_propose_user_input_when_disabled(monkeypatch, tmp_path: Path):
    """P1: lessons.enabled=false replaces the LESSONS_MD body with a
    placeholder (NOT empty); enabled (default) injects the real text."""
    monkeypatch.setattr(proposer, "_REPO_ROOT", tmp_path)
    disabled_cfg = {**_DEFAULT_CONFIG, "lessons": {"enabled": False}}
    client = FakeClient([_FIXTURE_HYPOTHESIS_JSON])
    proposer.propose(
        {}, "SECRET_LESSON_TEXT do the wrong thing", [],
        client=client, config=disabled_cfg,
    )
    user_prompt = client.calls[0][1]
    assert "SECRET_LESSON_TEXT" not in user_prompt
    assert "lessons disabled for this run" in user_prompt

    # Control: default (no lessons.enabled key) keeps lessons injection.
    client2 = FakeClient([_FIXTURE_HYPOTHESIS_JSON])
    proposer.propose(
        {}, "SECRET_LESSON_TEXT do the wrong thing", [],
        client=client2, config=_DEFAULT_CONFIG,
    )
    assert "SECRET_LESSON_TEXT" in client2.calls[0][1]


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
