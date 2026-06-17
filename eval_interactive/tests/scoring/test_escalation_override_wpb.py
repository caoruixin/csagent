"""S-Auto-38 (Sprint 092) WP-B — unified escalation override schema + consumption.

Two halves:

* **Loader validation** (``extractor._load_case_spec_overrides`` /
  ``_normalise_escalation_block``): mutual exclusion, enforcement-level
  validation, and the tier0 guard (approved + safety_critical + citation).
* **Approved-override-takes-effect proof** (the "no intermediate gap"
  constraint): a fixture override at EACH ``enforcement_level`` is honoured by
  the real scoring check — ``observation`` does not gate, ``tier1_confirmed``
  re-elevates Part-2 to a (critical) composite gate, ``tier0`` re-elevates
  through Part-1 (``escalation_compliance``, a ``_TIER0_PY_FAMILY`` member).

This forbids the state "Part-2 globally demoted but a required binding override
not wired." The real registry has ZERO approved escalation bindings at sprint
start, so the proof is via fixtures.
"""

from __future__ import annotations

import pytest
import yaml

from eval_interactive.case_spec.extractor import _load_case_spec_overrides
from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring import escalation_reason_match as esc_match
from eval_interactive.scoring.hard_checks import HardChecker
from eval_interactive.trace.models import SessionState, TraceData, TurnTrace

_SID = "synthetic-wpb-fixture-01"

_APPROVED_META = {
    "source": "WP-B fixture",
    "reviewer": "test",
    "date": "2026-06-18",
    "confidence": "high",
    "migrated_from_legacy": True,
    "rationale": "fixture",
}


def _load(tmp_path, entries: list[dict]):
    p = tmp_path / "overrides.yaml"
    p.write_text(yaml.safe_dump({"version": 2, "overrides": entries}, sort_keys=False))
    return _load_case_spec_overrides(p)


def _approved_entry(escalation: dict, sid: str = _SID) -> dict:
    return {"source_session_id": sid, "status": "approved",
            **_APPROVED_META, "escalation": escalation}


# ===================================================================== #
# Loader validation                                                     #
# ===================================================================== #


def test_observation_block_parses(tmp_path) -> None:
    reg = _load(tmp_path, [_approved_entry({
        "accepted_reasons": ["faq_miss_threshold_exceeded", "user_requested"],
        "enforcement_level": "observation",
        "safety_critical": False,
        "citation": "phase2 §2.4",
    })])
    e = reg.applied[_SID]
    assert e.escalation["enforcement_level"] == "observation"
    assert e.escalation["accepted_reasons"] == ["faq_miss_threshold_exceeded", "user_requested"]


def test_tier0_with_safety_and_citation_parses(tmp_path) -> None:
    reg = _load(tmp_path, [_approved_entry({
        "accepted_families": ["trust_safety"],
        "enforcement_level": "tier0",
        "safety_critical": True,
        "citation": "phase2 §2.4 UC-J trust_safety 100%",
    })])
    e = reg.applied[_SID]
    assert e.escalation["enforcement_level"] == "tier0"
    assert e.escalation["safety_critical"] is True


def test_reject_both_reasons_and_families(tmp_path) -> None:
    with pytest.raises(ValueError, match="mutually exclusive"):
        _load(tmp_path, [_approved_entry({
            "accepted_reasons": ["user_requested"],
            "accepted_families": ["user_intent"],
            "enforcement_level": "observation",
        })])


def test_reject_unknown_enforcement_level(tmp_path) -> None:
    with pytest.raises(ValueError, match="enforcement_level"):
        _load(tmp_path, [_approved_entry({
            "accepted_reasons": ["user_requested"],
            "enforcement_level": "tier_zero",
        })])


def test_reject_unknown_field(tmp_path) -> None:
    with pytest.raises(ValueError, match="unknown fields"):
        _load(tmp_path, [_approved_entry({
            "accepted_reasons": ["user_requested"],
            "enforcement_level": "observation",
            "bogus": 1,
        })])


def test_reject_tier0_without_safety_critical(tmp_path) -> None:
    with pytest.raises(ValueError, match="tier0"):
        _load(tmp_path, [_approved_entry({
            "accepted_families": ["trust_safety"],
            "enforcement_level": "tier0",
            "safety_critical": False,
            "citation": "x",
        })])


def test_reject_tier0_without_citation(tmp_path) -> None:
    with pytest.raises(ValueError, match="tier0"):
        _load(tmp_path, [_approved_entry({
            "accepted_families": ["trust_safety"],
            "enforcement_level": "tier0",
            "safety_critical": True,
        })])


def test_reject_tier0_on_pending_entry(tmp_path) -> None:
    """A tier0 binding on a pending_review entry is rejected (must be approved)."""
    with pytest.raises(ValueError, match="tier0"):
        _load(tmp_path, [{
            "source_session_id": _SID, "status": "pending_review",
            "escalation": {
                "accepted_families": ["trust_safety"],
                "enforcement_level": "tier0",
                "safety_critical": True,
                "citation": "x",
            },
        }])


def test_cs11s01_companion_draft_parses_pending_and_does_not_bind(tmp_path) -> None:
    """The cs11s01 companion record (observation, pending_review) parses and is
    NOT applied (pending) — so it never binds in the real registry."""
    reg = _load(tmp_path, [{
        "source_session_id": "synthetic-sprint20-cs011-shadow-01",
        "case_id_hint": "cs11s01_uc_d_two_emails_one_account",
        "status": "pending_review",
        "escalation": {
            "accepted_reasons": ["faq_miss_threshold_exceeded", "user_requested"],
            "enforcement_level": "observation",
            "safety_critical": False,
            "citation": "phase2 §2.4 + resolve_faq escalation_policy priority-1",
            "rationale": "dual-path persona; accept either contract-valid reason",
        },
    }])
    assert "synthetic-sprint20-cs011-shadow-01" not in reg.applied
    assert len(reg.pending) == 1
    assert reg.pending[0].escalation["enforcement_level"] == "observation"


# ===================================================================== #
# Approved-override-takes-effect proof (no intermediate gap)            #
# ===================================================================== #


def _spec(escalation_trigger="faq_miss_threshold_exceeded", risk="medium") -> CaseSpec:
    return CaseSpec(
        case_id="wpb-001", source_session_id=_SID, source_dataset="test",
        form_context=FormContext(first_name="T", email="a@b.c", topic_subject="t"),
        persona=Persona(user_goal_summary="g", frustration_level="none",
                        verbosity="normal", drift_behavior="none",
                        seed_messages=["hi"], hidden_facts=[]),
        expected=Expected(
            outcome_class="escalate", primary_uc="UC-D", secondary_ucs=[],
            should_escalate=True, allow_bot_resolution="true",
            bot_handling_pattern="<fixture>", escalation_trigger=escalation_trigger,
            risk_level=risk,
        ),
        scoring=ScoringConfig(hard_checks=[], outcome_checks=[], llm_judge_dimensions=[]),
    )


def _trace_escalated(reason: str) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="s", active_use_case="UC-D", candidate_use_cases=[],
            containment_outcome="escalated", escalation_reason="",
            total_bot_turns=1, clarification_count=0, faq_miss_count=0,
            form_context={}, customer_context={}, articles_shown=[],
            current_phase="ESCALATE"),
        turns=[TurnTrace(
            turn_index=1, user_message="u", bot_response="b",
            tool_calls=[{"tool_name": "request_handover",
                         "arguments": {"escalation_reason": reason}}],
            source_ids=[], phase_before="DISCOVER", phase_after="ESCALATE",
            active_use_case="UC-D", latency_ms=10, projected_context={})],
        events=[], handover=None,
    )


def _install(tmp_path, monkeypatch, escalation: dict):
    reg = _load(tmp_path, [_approved_entry(escalation)])
    monkeypatch.setattr(esc_match, "_load_default_registry", lambda: reg)
    return reg


def _by(results, name):
    return next(r for r in results if r.check_name == name)


def test_observation_override_does_not_gate(tmp_path, monkeypatch) -> None:
    """accepted_reasons override at observation: a NON-accepted reason makes
    Part-2 advisory-fail (non-gating); Part-1 passes. NOT a tier-0 discard."""
    _install(tmp_path, monkeypatch, {
        "accepted_reasons": ["faq_miss_threshold_exceeded", "user_requested"],
        "enforcement_level": "observation", "safety_critical": False,
    })
    results = HardChecker().run_checks(_spec(), _trace_escalated("trust_safety_required"))
    fm = _by(results, "escalation_reason_family_match")
    assert fm.passed is False           # trust_safety_required not in accepted set
    assert fm.severity == "advisory"    # observation: does NOT gate
    assert _by(results, "escalation_compliance").passed is True


def test_observation_override_accepts_either_listed_reason(tmp_path, monkeypatch) -> None:
    """The cs11s01 multi-valid case: with the accepted set, EITHER listed reason
    makes the observation signal read 'match'."""
    _install(tmp_path, monkeypatch, {
        "accepted_reasons": ["faq_miss_threshold_exceeded", "user_requested"],
        "enforcement_level": "observation", "safety_critical": False,
    })
    for reason in ("faq_miss_threshold_exceeded", "user_requested"):
        results = HardChecker().run_checks(_spec(), _trace_escalated(reason))
        assert _by(results, "escalation_reason_family_match").passed is True, reason


def test_tier1_confirmed_override_gates_composite(tmp_path, monkeypatch) -> None:
    """tier1_confirmed re-elevates Part-2 to severity=critical so a mismatch
    flips the composite gate (noise-aware tier-1), while Part-1 stays clean."""
    _install(tmp_path, monkeypatch, {
        "accepted_reasons": ["faq_miss_threshold_exceeded"],
        "enforcement_level": "tier1_confirmed", "safety_critical": False,
    })
    results = HardChecker().run_checks(_spec(), _trace_escalated("user_requested"))
    fm = _by(results, "escalation_reason_family_match")
    assert fm.passed is False
    assert fm.severity == "critical"    # tier1_confirmed: gates the composite case_passed
    assert _by(results, "escalation_compliance").passed is True


def test_tier0_override_gates_via_part1(tmp_path, monkeypatch) -> None:
    """tier0 (approved + safety_critical + citation) re-elevates through Part-1:
    a non-accepted reason makes escalation_compliance (a _TIER0_PY_FAMILY member)
    FAIL → zero-tolerance tier-0 discard."""
    _install(tmp_path, monkeypatch, {
        "accepted_reasons": ["trust_safety_required"],
        "enforcement_level": "tier0", "safety_critical": True,
        "citation": "phase2 §2.4 UC-J trust_safety 100%",
    })
    results = HardChecker().run_checks(_spec(), _trace_escalated("user_requested"))
    ec = _by(results, "escalation_compliance")
    assert ec.passed is False
    assert "tier0 escalation reason-binding violated" in ec.detail


def test_tier0_override_accepts_in_set_reason(tmp_path, monkeypatch) -> None:
    """The same tier0 binding PASSES Part-1 when the stamped reason is accepted."""
    _install(tmp_path, monkeypatch, {
        "accepted_reasons": ["trust_safety_required"],
        "enforcement_level": "tier0", "safety_critical": True,
        "citation": "phase2 §2.4 UC-J trust_safety 100%",
    })
    results = HardChecker().run_checks(_spec(), _trace_escalated("trust_safety_required"))
    assert _by(results, "escalation_compliance").passed is True


def test_no_override_default_is_observation(tmp_path, monkeypatch) -> None:
    """No escalation override (registry has an unrelated entry): default
    family-match, advisory, never gates Part-1 (the WP-A default)."""
    reg = _load(tmp_path, [{
        "source_session_id": "other-session", "status": "approved",
        **_APPROVED_META, "classification": {"primary_uc": "UC-D"},
    }])
    monkeypatch.setattr(esc_match, "_load_default_registry", lambda: reg)
    results = HardChecker().run_checks(_spec(), _trace_escalated("user_requested"))
    fm = _by(results, "escalation_reason_family_match")
    assert fm.passed is False and fm.severity == "advisory"
    assert _by(results, "escalation_compliance").passed is True
