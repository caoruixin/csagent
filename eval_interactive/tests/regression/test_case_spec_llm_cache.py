"""Wave A6.2 reproducibility regression tests for the LLM persona cache.

These tests must run in CI without network access. The reviewer's HTTP
client is patched via monkeypatch (or a fake injected through the
``llm_reviewer=`` kwarg) so no real DeepSeek call is ever attempted.
"""

from __future__ import annotations

import csv
import filecmp
import json
import os
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
    DeepSeekClient,
    LlmPersonaReviewer,
    PROMPT_SYSTEM,
    PersonaDraft,
    derive_verbosity,
    validate_response,
)


# ---------------------------------------------------------------------------
# Test fixtures (small, in-tmp synthetic HR row + transcript)
# ---------------------------------------------------------------------------

HR_FIELDS = [
    "session_id",
    "source_dataset",
    "case_id",
    "form_topic_subject",
    "source_primary_uc",
    "primary_uc_corrected",
    "secondary_ucs",
    "outcome_class",
    "should_escalate",
    "escalation_trigger",
    "form_provides_email",
    "form_provides_ad_id",
    "drift_type",
    "has_frustration",
    "frustration_type",
    "risk_level",
    "uc_confidence",
    "expected_tool_sequence",
    "forbidden_tools",
]

TURN_FIELDS = ["conversation_id", "sequence", "role", "speaker", "message_redacted"]


SESSION_A = "test-session-A"
SESSION_B = "test-session-B"


def _hr_row(session_id: str, *, primary_uc: str = "UC-C") -> dict[str, str]:
    row = {field: "" for field in HR_FIELDS}
    row.update(
        {
            "session_id": session_id,
            "source_dataset": "badcase",
            "case_id": session_id,
            "form_topic_subject": "Replies & Messaging",
            "source_primary_uc": primary_uc,
            "primary_uc_corrected": primary_uc,
            "secondary_ucs": "[]",
            "outcome_class": "resolve",
            "should_escalate": "false",
            "form_provides_email": "false",
            "form_provides_ad_id": "false",
            "drift_type": "none",
            "has_frustration": "false",
            "risk_level": "low",
        }
    )
    return row


def _write_csv(path: Path, fields: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        for r in rows:
            writer.writerow(r)


def _build_inputs(tmp_path: Path, sessions: list[str]) -> tuple[Path, Path, Path]:
    """Write synthetic HR CSV + badcase turns CSV. Return (hr_csv,
    turns_dir, output_dir)."""
    hr_csv = tmp_path / "hr.csv"
    turns_dir = tmp_path / "turns"
    output_dir = tmp_path / "case_specs"
    rows = [_hr_row(sid) for sid in sessions]
    _write_csv(hr_csv, HR_FIELDS, rows)

    turn_rows: list[dict[str, str]] = []
    for sid in sessions:
        turn_rows.extend([
            {
                "conversation_id": sid,
                "sequence": "0",
                "role": "visitor",
                "speaker": "[PRE_CHAT_FORM]",
                "message_redacted": (
                    "[Form] Subject: Replies & Messaging | "
                    "Description: Cannot receive messages on my account."
                ),
            },
            {
                "conversation_id": sid,
                "sequence": "1",
                "role": "visitor",
                "speaker": "Customer",
                "message_redacted": (
                    "I cannot receive messages on my account and the issue "
                    "has been ongoing for several days."
                ),
            },
            {
                "conversation_id": sid,
                "sequence": "2",
                "role": "agent",
                "speaker": "Agent",
                "message_redacted": (
                    "Thanks for reaching out — let me check your account."
                ),
            },
            {
                "conversation_id": sid,
                "sequence": "3",
                "role": "visitor",
                "speaker": "Customer",
                "message_redacted": (
                    "Please let me know what to do — this is urgent."
                ),
            },
        ])
    _write_csv(turns_dir / "badcase_turns.csv", TURN_FIELDS, turn_rows)
    return hr_csv, turns_dir, output_dir


# ---------------------------------------------------------------------------
# Fakes
# ---------------------------------------------------------------------------


class _FakeDeepSeekClient:
    """In-memory replacement for ``DeepSeekClient``.

    ``responses`` is a list of values to return on each ``call``. Each
    item may be a dict (returned as-is) OR a callable that takes the
    ``messages`` list and returns a dict.
    """

    def __init__(self, responses: list[Any], *, model: str = "deepseek-v4-pro") -> None:
        self._responses = list(responses)
        self.calls: list[list[dict[str, str]]] = []
        self._model = model

    @property
    def model(self) -> str:
        return self._model

    def call(self, messages: list[dict[str, str]]) -> dict[str, Any]:
        self.calls.append([dict(m) for m in messages])
        if not self._responses:
            raise AssertionError("No more fake responses queued")
        nxt = self._responses.pop(0)
        if callable(nxt):
            return nxt(messages)
        return nxt


class _ExplodingDeepSeekClient:
    """Raises if any call is attempted."""

    def __init__(self, *, model: str = "deepseek-v4-pro") -> None:
        self._model = model

    @property
    def model(self) -> str:
        return self._model

    def call(self, messages: list[dict[str, str]]) -> dict[str, Any]:
        raise AssertionError("DeepSeek client should not be called in this test")


# ---------------------------------------------------------------------------
# Test 1: --no-llm offline mode equals rule_draft and skips network entirely.
# ---------------------------------------------------------------------------


def test_offline_mode_returns_rule_draft(tmp_path: Path, monkeypatch) -> None:
    """`extract_case_specs(..., llm_offline=True)` must not even
    instantiate a DeepSeekClient and must yield rule_draft personas."""
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)

    hr_csv, turns_dir, output_dir = _build_inputs(tmp_path, [SESSION_A])
    cache_dir = tmp_path / "cache"

    # Sanity: ensure that we'd blow up if anyone tried to instantiate a
    # client without a key by patching the constructor.
    def _explode(*a, **kw):  # pragma: no cover -- only fires on regression
        raise AssertionError("DeepSeekClient should not be instantiated in offline mode")

    monkeypatch.setattr(
        "eval_interactive.case_spec.llm_persona_reviewer.DeepSeekClient.__init__",
        _explode,
    )

    specs = extract_case_specs(
        hr_csv,
        turns_dir,
        output_dir,
        llm_offline=True,
        llm_cache_dir=cache_dir,
    )
    assert len(specs) == 1
    persona = specs[0].persona
    # The rule-extractor's seed is the issue-bearing visitor messages.
    assert persona.seed_messages == [
        "I cannot receive messages on my account and the issue has been "
        "ongoing for several days.",
        "Please let me know what to do — this is urgent.",
    ]
    # Cache dir must be untouched.
    assert not cache_dir.exists() or not any(cache_dir.glob("*.yaml"))


# ---------------------------------------------------------------------------
# Test 2: cache hit short-circuits the API.
# ---------------------------------------------------------------------------


def test_cache_hit_skips_api_call(tmp_path: Path) -> None:
    hr_csv, turns_dir, output_dir = _build_inputs(tmp_path, [SESSION_A])
    cache_dir = tmp_path / "cache"

    # First pass: a fake client that returns a high-confidence proposal so
    # a cache file is written.
    seed_msg = "Please let me know what to do — this is urgent."
    first_response = {
        "seed_messages": [seed_msg],
        "user_goal_summary": (
            "User cannot receive messages on their account and asks for "
            "guidance to resolve the ongoing issue. Drift: none."
        ),
        "hidden_facts": [],
        "llm_confidence": "high",
        "llm_rationale": "Direct visitor turn captures the user's request.",
    }

    fake = _FakeDeepSeekClient([first_response])
    reviewer1 = LlmPersonaReviewer(
        api_key="fake-key",
        cache_dir=cache_dir,
        client=fake,
    )
    extract_case_specs(
        hr_csv,
        turns_dir,
        output_dir,
        llm_cache_dir=cache_dir,
        llm_reviewer=reviewer1,
    )
    assert len(fake.calls) == 1
    cache_files = list(cache_dir.glob("*.yaml"))
    assert len(cache_files) == 1
    assert cache_files[0].name == f"{SESSION_A}.yaml"

    # Second pass: a client that explodes if called. Must be a cache hit.
    exploder = _ExplodingDeepSeekClient()
    reviewer2 = LlmPersonaReviewer(
        api_key="fake-key",
        cache_dir=cache_dir,
        client=exploder,
    )
    output_dir2 = tmp_path / "case_specs_2"
    specs = extract_case_specs(
        hr_csv,
        turns_dir,
        output_dir2,
        llm_cache_dir=cache_dir,
        llm_reviewer=reviewer2,
    )
    assert specs[0].persona.seed_messages == [seed_msg]


# ---------------------------------------------------------------------------
# Test 3: stale cache (prompt_hash mismatch) regenerates and overwrites.
# ---------------------------------------------------------------------------


def test_cache_hash_mismatch_triggers_call(tmp_path: Path) -> None:
    hr_csv, turns_dir, output_dir = _build_inputs(tmp_path, [SESSION_A])
    cache_dir = tmp_path / "cache"
    cache_dir.mkdir()

    # Pre-populate a stale cache file with bogus prompt_hash.
    stale_path = cache_dir / f"{SESSION_A}.yaml"
    stale_doc = {
        "source_session_id": SESSION_A,
        "case_id_hint": "stale_case",
        "llm_model": "deepseek-v4-pro",
        "prompt_hash": "0" * 64,
        "generated_at": "2026-01-01T00:00:00+00:00",
        "llm_confidence": "high",
        "llm_rationale": "stale entry",
        "rule_draft": {
            "seed_messages": ["stale rule seed"],
            "user_goal_summary": "stale ugs",
            "hidden_facts": [],
            "verbosity": "normal",
        },
        "llm_proposal": None,
        "accepted_value": {
            "seed_messages": ["STALE ACCEPTED SEED"],
            "user_goal_summary": "STALE ACCEPTED UGS",
            "hidden_facts": [],
            "verbosity": "normal",
        },
        "acceptance_reason": "auto_accept_high_confidence",
        "validation_notes": [],
    }
    with stale_path.open("w", encoding="utf-8") as f:
        yaml.dump(stale_doc, f, sort_keys=False)
    pre_mtime = stale_path.stat().st_mtime_ns

    new_seed = "Please let me know what to do — this is urgent."
    fresh_response = {
        "seed_messages": [new_seed],
        "user_goal_summary": "Refreshed user goal. Drift: none.",
        "hidden_facts": [],
        "llm_confidence": "high",
        "llm_rationale": "fresh",
    }
    fake = _FakeDeepSeekClient([fresh_response])
    reviewer = LlmPersonaReviewer(
        api_key="fake-key",
        cache_dir=cache_dir,
        client=fake,
    )

    specs = extract_case_specs(
        hr_csv,
        turns_dir,
        output_dir,
        llm_cache_dir=cache_dir,
        llm_reviewer=reviewer,
    )
    assert len(fake.calls) == 1, "Stale prompt_hash must trigger a fresh call"
    assert specs[0].persona.seed_messages == [new_seed]

    rewritten = yaml.safe_load(stale_path.read_text(encoding="utf-8"))
    assert rewritten["accepted_value"]["seed_messages"] == [new_seed]
    assert rewritten["prompt_hash"] != "0" * 64
    assert stale_path.stat().st_mtime_ns >= pre_mtime  # rewritten


# ---------------------------------------------------------------------------
# Test 4: forbidden key in response -> validation rejection -> rule fallback.
# ---------------------------------------------------------------------------


def test_forbidden_key_in_response_falls_back(tmp_path: Path) -> None:
    hr_csv, turns_dir, output_dir = _build_inputs(tmp_path, [SESSION_A])
    cache_dir = tmp_path / "cache"

    bad_response = {
        "seed_messages": [
            "I cannot receive messages on my account and the issue has been "
            "ongoing for several days."
        ],
        "user_goal_summary": "User wants their messages working again. Drift: none.",
        "hidden_facts": [],
        "llm_confidence": "high",
        "llm_rationale": "trying to overstep",
        # FORBIDDEN
        "outcome_class": "escalate",
    }

    fake = _FakeDeepSeekClient([bad_response])
    reviewer = LlmPersonaReviewer(
        api_key="fake-key",
        cache_dir=cache_dir,
        client=fake,
    )
    specs = extract_case_specs(
        hr_csv,
        turns_dir,
        output_dir,
        llm_cache_dir=cache_dir,
        llm_reviewer=reviewer,
    )
    persona = specs[0].persona

    # Persona must equal the rule_draft (greetings/issue lines from extractor).
    assert persona.seed_messages == [
        "I cannot receive messages on my account and the issue has been "
        "ongoing for several days.",
        "Please let me know what to do — this is urgent.",
    ]
    # Outcome must be unaffected by the forbidden LLM key.
    assert specs[0].expected.outcome_class == "resolve"

    cache_doc = yaml.safe_load(
        (cache_dir / f"{SESSION_A}.yaml").read_text(encoding="utf-8")
    )
    assert cache_doc["acceptance_reason"] == "rule_fallback_validation_failed"
    notes_blob = " ".join(cache_doc.get("validation_notes") or [])
    assert "forbidden_key_present" in notes_blob
    assert "outcome_class" in notes_blob


# ---------------------------------------------------------------------------
# Test 5: fabricated seed_messages (not in any visitor turn) -> fallback.
# ---------------------------------------------------------------------------


def test_fabricated_seed_falls_back(tmp_path: Path) -> None:
    hr_csv, turns_dir, output_dir = _build_inputs(tmp_path, [SESSION_A])
    cache_dir = tmp_path / "cache"

    fabricated_response = {
        "seed_messages": [
            "I never said this string anywhere in the transcript",
        ],
        "user_goal_summary": "Made-up summary. Drift: none.",
        "hidden_facts": [],
        "llm_confidence": "high",
        "llm_rationale": "claiming visitor said this",
    }
    fake = _FakeDeepSeekClient([fabricated_response])
    reviewer = LlmPersonaReviewer(
        api_key="fake-key",
        cache_dir=cache_dir,
        client=fake,
    )

    specs = extract_case_specs(
        hr_csv,
        turns_dir,
        output_dir,
        llm_cache_dir=cache_dir,
        llm_reviewer=reviewer,
    )
    persona = specs[0].persona
    # Fall back to rule-extractor seeds.
    assert persona.seed_messages == [
        "I cannot receive messages on my account and the issue has been "
        "ongoing for several days.",
        "Please let me know what to do — this is urgent.",
    ]
    cache_doc = yaml.safe_load(
        (cache_dir / f"{SESSION_A}.yaml").read_text(encoding="utf-8")
    )
    assert cache_doc["acceptance_reason"] == "rule_fallback_validation_failed"
    assert any("verbatim_check_failed" in n for n in cache_doc["validation_notes"])


# ---------------------------------------------------------------------------
# Test 6: Option A -- one retry on user_goal_summary too long, then accept.
# ---------------------------------------------------------------------------


def test_user_goal_summary_length_retry(tmp_path: Path) -> None:
    hr_csv, turns_dir, output_dir = _build_inputs(tmp_path, [SESSION_A])
    cache_dir = tmp_path / "cache"

    long_summary = "x" * 320
    short_summary = (
        "User cannot receive messages on the account and wants the issue "
        "resolved promptly. Drift: none."
    )
    assert len(short_summary) <= 280

    seed_msg = (
        "I cannot receive messages on my account and the issue has been "
        "ongoing for several days."
    )

    first_response = {
        "seed_messages": [seed_msg],
        "user_goal_summary": long_summary,
        "hidden_facts": [],
        "llm_confidence": "high",
        "llm_rationale": "first attempt was too long",
    }
    second_response = {
        "seed_messages": [seed_msg],
        "user_goal_summary": short_summary,
        "hidden_facts": [],
        "llm_confidence": "high",
        "llm_rationale": "shortened",
    }
    fake = _FakeDeepSeekClient([first_response, second_response])
    reviewer = LlmPersonaReviewer(
        api_key="fake-key",
        cache_dir=cache_dir,
        client=fake,
    )
    specs = extract_case_specs(
        hr_csv,
        turns_dir,
        output_dir,
        llm_cache_dir=cache_dir,
        llm_reviewer=reviewer,
    )
    assert len(fake.calls) == 2, "Length failure must trigger exactly one retry"
    persona = specs[0].persona
    assert persona.user_goal_summary == short_summary
    cache_doc = yaml.safe_load(
        (cache_dir / f"{SESSION_A}.yaml").read_text(encoding="utf-8")
    )
    assert cache_doc["acceptance_reason"] == "auto_accept_high_confidence"
    notes_blob = "\n".join(cache_doc["validation_notes"])
    assert "first_attempt: user_goal_summary too long" in notes_blob

    # Sanity: the retry message body must reference the prior length.
    retry_messages = fake.calls[1]
    assert any("320 characters" in m["content"] for m in retry_messages)


# ---------------------------------------------------------------------------
# Test 7: byte-identical regeneration in offline mode.
# ---------------------------------------------------------------------------


def test_byte_identical_regeneration(tmp_path: Path) -> None:
    hr_csv, turns_dir, _ = _build_inputs(tmp_path, [SESSION_A, SESSION_B])
    out1 = tmp_path / "out1"
    out2 = tmp_path / "out2"
    cache_dir = tmp_path / "cache"

    extract_case_specs(
        hr_csv, turns_dir, out1, llm_offline=True, llm_cache_dir=cache_dir,
    )
    extract_case_specs(
        hr_csv, turns_dir, out2, llm_offline=True, llm_cache_dir=cache_dir,
    )

    yaml_files = sorted(p.relative_to(out1) for p in out1.rglob("*.yaml"))
    assert yaml_files, "Expected at least one generated spec"
    for rel in yaml_files:
        assert filecmp.cmp(out1 / rel, out2 / rel, shallow=False), (
            f"YAML byte mismatch between runs for {rel}"
        )
