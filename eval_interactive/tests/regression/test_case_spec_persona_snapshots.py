"""Wave A6.5 persona snapshot regression tests.

These tests pin ``persona.seed_messages``, ``persona.user_goal_summary``,
and ``persona.hidden_facts`` for the three smoke sessions whose LLM
review cache files are committed under ``eval_interactive/case_spec_llm_cache/``.

Locking goals (per Wave A6.5 acceptance):

* If a future DeepSeek behaviour change silently mutates the cached
  ``accepted_value``, these tests fail loudly so a human reviews the diff
  before merging.
* The Wave A5 ``case_spec_overrides.yaml`` decision must keep winning over
  L2 for ``cs_interactive_012``'s ``expected.*`` (UC-FP / resolve / no
  request_handover).
* The system prompt sha256 is locked so any prompt edit must update both
  the constant AND this snapshot test, surfacing prompt drift in CI.

The tests run fully offline. They never call DeepSeek. The reviewer's
HTTP transport is replaced either by a fake client that hands back the
cache file's ``llm_proposal`` (proving the validator accepts it) or by
``llm_offline=True`` (proving the rule_draft path).

Why a fake client instead of a true cache hit? The committed cache
files were generated when the snapshot sessions occupied row positions
1/2/3 of the HR CSV (so ``case_id_hint`` is ``cs_interactive_001`` etc.),
but the production HR CSV places them at positions 12/55/64. Since
``case_id`` is part of the rendered prompt body, ``prompt_hash`` does not
match the production prompt and the production cache lookup misses for
these sessions today. We therefore drive the snapshot through a fake
client; the maintainer is asked to decide separately whether the cache
key should switch from ``(source_session_id, prompt_hash)`` to
``source_session_id`` alone, or whether ``case_id`` should be removed
from the prompt body.
"""

from __future__ import annotations

import csv
from dataclasses import asdict
from pathlib import Path
from typing import Any

import pytest
import yaml

from eval_interactive.case_spec.extractor import (
    LLM_REVIEWER_DEFAULT_CACHE_DIR,
    extract_case_specs,
)
from eval_interactive.case_spec.llm_persona_reviewer import (
    LlmPersonaReviewer,
    PROMPT_TEMPLATE_SHA256,
)


# ---------------------------------------------------------------------------
# Repo paths
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).resolve().parents[3]
HR_CSV = REPO_ROOT / "data" / "human_review_annotations_2026-04-22_golden.csv"
TURNS_DIR = REPO_ROOT / "data" / "eval_datasets"
PROD_CACHE_DIR = LLM_REVIEWER_DEFAULT_CACHE_DIR

LOCKED_PROMPT_SHA256 = (
    "61617a17cfd68a14ef0a7496724c2141e0896b6e0b92c34394668f858e9c0364"
)


# ---------------------------------------------------------------------------
# Fake DeepSeek client: hands back a cache file's llm_proposal.
# ---------------------------------------------------------------------------


class _ScriptedDeepSeekClient:
    """Returns one canned response per ``(source_session_id)`` lookup.

    The reviewer uses ``call(messages)``; we extract the session id from
    the rendered user prompt so the fake works without coordinating with
    the production extractor's row ordering.
    """

    def __init__(self, responses_by_session: dict[str, dict[str, Any]]) -> None:
        self._responses = dict(responses_by_session)
        self.calls: list[str] = []

    @property
    def model(self) -> str:
        return "deepseek-v4-pro"

    def call(self, messages: list[dict[str, str]]) -> dict[str, Any]:
        # The user prompt body always begins with "SESSION\n  source_session_id: <sid>\n..."
        body = messages[-1]["content"] if messages else ""
        sid = ""
        for line in body.splitlines():
            line = line.strip()
            if line.startswith("source_session_id:"):
                sid = line.split(":", 1)[1].strip()
                break
        self.calls.append(sid)
        if sid not in self._responses:
            raise AssertionError(
                f"_ScriptedDeepSeekClient got unexpected session id {sid!r}; "
                f"only have responses for {sorted(self._responses)}"
            )
        return dict(self._responses[sid])


def _load_cache_file(session_id: str) -> dict[str, Any]:
    path = PROD_CACHE_DIR / f"{session_id}.yaml"
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def _proposal_to_response(cache: dict[str, Any]) -> dict[str, Any]:
    """Build a fake DeepSeek response payload from a cache record."""
    proposal = cache["llm_proposal"]
    return {
        "seed_messages": list(proposal["seed_messages"]),
        "user_goal_summary": proposal["user_goal_summary"],
        "hidden_facts": [dict(hf) for hf in (proposal.get("hidden_facts") or [])],
        "llm_confidence": "high",
        "llm_rationale": cache.get("llm_rationale", ""),
    }


def _build_reviewer_with_scripted_responses(
    *,
    cache_dir: Path,
    sessions_to_responses: dict[str, dict[str, Any]],
) -> LlmPersonaReviewer:
    fake_client = _ScriptedDeepSeekClient(sessions_to_responses)
    return LlmPersonaReviewer(
        api_key="fake-key",
        cache_dir=cache_dir,
        client=fake_client,
    )


def _extract_with_fake_llm(
    output_dir: Path,
    *,
    sessions_to_responses: dict[str, dict[str, Any]],
    cache_dir: Path,
):
    reviewer = _build_reviewer_with_scripted_responses(
        cache_dir=cache_dir,
        sessions_to_responses=sessions_to_responses,
    )
    return extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        output_dir,
        llm_cache_dir=cache_dir,
        llm_reviewer=reviewer,
    )


def _extract_offline(output_dir: Path, *, cache_dir: Path):
    return extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        output_dir,
        llm_cache_dir=cache_dir,
        llm_offline=True,
    )


# ---------------------------------------------------------------------------
# Test 1: cs_interactive_012 (badcase / UC-FP via Wave A5 override)
# ---------------------------------------------------------------------------


def test_snapshot_cs_interactive_012_persona(tmp_path: Path) -> None:
    """Pin persona.seed/ugs/hidden_facts to the cache file for
    570Q5000008hx9tIAA, and prove the L3 override still wins on
    expected.*"""
    cache = _load_cache_file("570Q5000008hx9tIAA")
    accepted = cache["accepted_value"]

    specs = _extract_with_fake_llm(
        tmp_path / "specs",
        cache_dir=tmp_path / "cache",
        sessions_to_responses={
            "570Q5000008hx9tIAA": _proposal_to_response(cache),
        },
    )

    spec = next(s for s in specs if s.source_session_id == "570Q5000008hx9tIAA")
    assert spec.case_id == "cs_interactive_012"

    persona = spec.persona
    assert persona.seed_messages == accepted["seed_messages"], (
        "persona.seed_messages drifted from the committed Wave A6 cache for "
        "cs_interactive_012. If this is intentional, regenerate the cache "
        "(--refresh-llm-session 570Q5000008hx9tIAA) and update this test."
    )
    assert persona.user_goal_summary == accepted["user_goal_summary"]
    cached_hidden = [
        (hf["fact"], hf["disclose_when"]) for hf in (accepted.get("hidden_facts") or [])
    ]
    spec_hidden = [(hf.fact, hf.disclose_when) for hf in persona.hidden_facts]
    assert spec_hidden == cached_hidden

    # L3 override (Wave A5) must still win.
    expected = spec.expected
    assert expected.outcome_class == "resolve"
    assert expected.primary_uc == "UC-FP"
    assert expected.secondary_ucs == ["UC-K"]
    assert expected.should_escalate is False
    assert expected.escalation_trigger is None
    assert "request_handover" not in expected.expected_tool_sequence


# ---------------------------------------------------------------------------
# Test 2: offline mode -> rule_draft persona, but L3 override still wins.
# ---------------------------------------------------------------------------


def test_snapshot_offline_mode_falls_back_to_rule_draft(tmp_path: Path) -> None:
    specs = _extract_offline(
        tmp_path / "specs",
        cache_dir=tmp_path / "cache",
    )
    spec = next(s for s in specs if s.source_session_id == "570Q5000008hx9tIAA")
    assert spec.case_id == "cs_interactive_012"

    # Rule-draft persona = greetings/closings, NOT the LLM-improved seeds.
    assert spec.persona.seed_messages[0].startswith("Hi Jason")
    assert "Thank you" in spec.persona.seed_messages[1]

    # Wave A5 override still applies on top of rule_draft.
    assert spec.expected.outcome_class == "resolve"
    assert spec.expected.primary_uc == "UC-FP"
    assert spec.expected.secondary_ucs == ["UC-K"]
    assert spec.expected.should_escalate is False
    assert spec.expected.escalation_trigger is None
    assert "request_handover" not in spec.expected.expected_tool_sequence


# ---------------------------------------------------------------------------
# Test 3: cs_interactive_055 (escalation / UC-FP via UC-B reclassification)
# ---------------------------------------------------------------------------


def test_snapshot_cs_interactive_055_persona(tmp_path: Path) -> None:
    """Session 570Q5000008TMmvIAG. UC-B in HR; reclassified to UC-FP via
    extractor.UC_B_RECLASSIFICATION_OVERRIDES (NOT case_spec_overrides)."""
    cache = _load_cache_file("570Q5000008TMmvIAG")
    accepted = cache["accepted_value"]

    specs = _extract_with_fake_llm(
        tmp_path / "specs",
        cache_dir=tmp_path / "cache",
        sessions_to_responses={
            "570Q5000008TMmvIAG": _proposal_to_response(cache),
        },
    )

    spec = next(s for s in specs if s.source_session_id == "570Q5000008TMmvIAG")
    assert spec.case_id == "cs_interactive_055"

    persona = spec.persona
    assert persona.seed_messages == accepted["seed_messages"]
    assert persona.user_goal_summary == accepted["user_goal_summary"]
    spec_hidden = [(hf.fact, hf.disclose_when) for hf in persona.hidden_facts]
    cached_hidden = [
        (hf["fact"], hf["disclose_when"]) for hf in (accepted.get("hidden_facts") or [])
    ]
    assert spec_hidden == cached_hidden

    # UC-FP via reclassification map; no Wave A5 override for this case.
    assert spec.expected.primary_uc == "UC-FP"
    assert spec.expected.secondary_ucs == ["UC-K"]


# ---------------------------------------------------------------------------
# Test 4: cs_interactive_064 (golden / UC-C, no override)
# ---------------------------------------------------------------------------


def test_snapshot_cs_interactive_064_persona(tmp_path: Path) -> None:
    """Session 570Q5000008l7flIAA. HR primary_uc_corrected=UC-C; no override."""
    cache = _load_cache_file("570Q5000008l7flIAA")
    accepted = cache["accepted_value"]

    specs = _extract_with_fake_llm(
        tmp_path / "specs",
        cache_dir=tmp_path / "cache",
        sessions_to_responses={
            "570Q5000008l7flIAA": _proposal_to_response(cache),
        },
    )

    spec = next(s for s in specs if s.source_session_id == "570Q5000008l7flIAA")
    assert spec.case_id == "cs_interactive_064"

    persona = spec.persona
    assert persona.seed_messages == accepted["seed_messages"]
    assert persona.user_goal_summary == accepted["user_goal_summary"]
    # Cache file has no hidden_facts.
    assert persona.hidden_facts == []

    assert spec.expected.primary_uc == "UC-C"


# ---------------------------------------------------------------------------
# Test 5: prompt sha256 lock-in.
# ---------------------------------------------------------------------------


def test_snapshot_prompt_sha256_locked() -> None:
    """If the prompt template ever changes, this test fails loudly so the
    maintainer must regenerate the persona cache (and the
    ``LOCKED_PROMPT_SHA256`` constant) on purpose."""
    assert PROMPT_TEMPLATE_SHA256 == LOCKED_PROMPT_SHA256, (
        "PROMPT_SYSTEM has drifted. Either the change is intentional (then "
        "regenerate the LLM persona cache and update LOCKED_PROMPT_SHA256 "
        "in this test), or it is a typo / merge conflict (revert it)."
    )
