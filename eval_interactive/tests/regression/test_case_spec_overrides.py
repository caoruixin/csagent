"""Wave A6.6 unified override registry — schema v2 regression tests.

Covers loader validation, the three application stages
(classification / expected / persona), pending-review semantics, and the
``--strict-overrides`` switch.
"""

from __future__ import annotations

from pathlib import Path

import pytest
import yaml

from eval_interactive.case_spec.extractor import (
    LLM_REVIEWER_DEFAULT_CACHE_DIR,
    OverrideRegistry,
    _apply_expected_override,
    _apply_persona_override,
    _load_case_spec_overrides,
    dump_audit_to,
    extract_case_specs,
)
from eval_interactive.case_spec.llm_persona_reviewer import (
    LlmPersonaReviewer,
    PersonaDraft,
)
from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    HiddenFact,
    Persona,
    ScoringConfig,
)


REPO_ROOT = Path(__file__).resolve().parents[3]
HR_CSV = REPO_ROOT / "data" / "human_review_annotations_2026-04-22_golden.csv"
TURNS_DIR = REPO_ROOT / "data" / "eval_datasets"
PROD_OVERRIDES = REPO_ROOT / "eval_interactive" / "case_spec_overrides.yaml"


# ---------------------------------------------------------------------------
# Helpers for tests that build a synthetic v2 file
# ---------------------------------------------------------------------------


def _write_overrides(tmp_path: Path, doc: dict) -> Path:
    tmp_path.mkdir(parents=True, exist_ok=True)
    p = tmp_path / "overrides.yaml"
    p.write_text(yaml.dump(doc, sort_keys=False), encoding="utf-8")
    return p


def _build_minimal_spec(session_id: str = "sid-x") -> CaseSpec:
    """Build a CaseSpec carrying enough data for _apply_*_override tests."""
    form = FormContext(
        first_name="Customer",
        email="customer@example.com",
        topic_subject="Replies & Messaging",
        ad_id="",
        description="Cannot receive messages on my account.",
    )
    persona = Persona(
        user_goal_summary="User reports replies issue. Drift: none.",
        frustration_level="none",
        verbosity="normal",
        drift_behavior="none",
        seed_messages=[
            "I cannot receive messages on my account and the issue has been ongoing.",
        ],
        hidden_facts=[],
        will_request_human_if="",
    )
    expected = Expected(
        outcome_class="resolve",
        primary_uc="UC-C",
        secondary_ucs=[],
        should_escalate=False,
        allow_bot_resolution="true",
        bot_handling_pattern="dummy pattern",
        escalation_trigger=None,
        risk_level="low",
        expected_tool_sequence=["search_knowledge", "resolve_article", "record_outcome"],
        forbidden_tools=[],
        grounding_mode="kb_only",
        answer_must_not_contain=[],
        max_turns=15,
    )
    scoring = ScoringConfig(
        hard_checks=["phase_transition_validity"],
        outcome_checks=["correct_uc"],
        llm_judge_dimensions=["groundedness"],
    )
    return CaseSpec(
        case_id="cs_interactive_xxx",
        source_session_id=session_id,
        source_dataset="badcase",
        form_context=form,
        persona=persona,
        expected=expected,
        scoring=scoring,
    )


# ---------------------------------------------------------------------------
# 1. v2 schema loads cleanly
# ---------------------------------------------------------------------------


def test_v2_schema_loads_cleanly() -> None:
    registry = _load_case_spec_overrides(PROD_OVERRIDES)
    assert isinstance(registry, OverrideRegistry)
    # 11 approved entries (9 legacy + 2 merged: cs_interactive_012 and
    # cs_interactive_015).
    assert len(registry.applied) == 11
    assert registry.pending == []
    cs12 = registry.applied["570Q5000008hx9tIAA"]
    assert cs12.classification == {
        "primary_uc": "UC-FP",
        "secondary_ucs": ["UC-K"],
    }
    # Expected block carries the Wave A5 reviewed values.
    assert cs12.expected is not None
    assert cs12.expected["outcome_class"] == "resolve"
    assert cs12.expected["should_escalate"] is False
    assert cs12.expected["expected_tool_sequence"] == [
        "get_customer_context",
        "search_knowledge",
        "resolve_article",
        "record_outcome",
    ]
    assert cs12.case_id_hint == "cs_interactive_012"
    assert cs12.migrated_from_legacy is False
    assert cs12.supporting_turn_numbers == (5, 6, 8, 10, 12, 14)


# ---------------------------------------------------------------------------
# 2. v1 file raises with the documented migration message
# ---------------------------------------------------------------------------


def test_v1_file_raises_with_migration_message(tmp_path: Path) -> None:
    p = _write_overrides(tmp_path, {"version": 1, "overrides": []})
    with pytest.raises(ValueError) as exc:
        _load_case_spec_overrides(p)
    msg = str(exc.value)
    assert "schema v2" in msg
    assert "A6.6" in msg


# ---------------------------------------------------------------------------
# 3. Approved entry missing reviewer/etc fails validation
# ---------------------------------------------------------------------------


def test_missing_required_metadata_for_approved_fails(tmp_path: Path) -> None:
    p = _write_overrides(tmp_path, {
        "version": 2,
        "overrides": [{
            "source_session_id": "sid-1",
            "status": "approved",
            "source": "x",
            "date": "2026-04-30",
            "confidence": "high",
            "rationale": "anything",
            "supporting_turn_numbers": [1],
            # NOTE: reviewer is intentionally missing.
            "classification": {"primary_uc": "UC-C"},
        }],
    })
    with pytest.raises(ValueError) as exc:
        _load_case_spec_overrides(p)
    assert "reviewer" in str(exc.value)


# ---------------------------------------------------------------------------
# 4. Approved entry empty supporting_turn_numbers fails UNLESS legacy
# ---------------------------------------------------------------------------


def test_missing_supporting_turn_numbers_for_approved_fails_unless_legacy(
    tmp_path: Path,
) -> None:
    base = {
        "source_session_id": "sid-1",
        "status": "approved",
        "source": "x",
        "reviewer": "y",
        "date": "2026-04-30",
        "confidence": "high",
        "rationale": "z",
        "supporting_turn_numbers": [],
        "classification": {"primary_uc": "UC-C"},
    }
    # Without migrated_from_legacy: must fail.
    p = _write_overrides(tmp_path, {"version": 2, "overrides": [base]})
    with pytest.raises(ValueError) as exc:
        _load_case_spec_overrides(p)
    assert "supporting_turn_numbers" in str(exc.value)

    # With migrated_from_legacy: must pass.
    legacy = dict(base)
    legacy["migrated_from_legacy"] = True
    p2 = _write_overrides(tmp_path / "legacy", {"version": 2, "overrides": [legacy]})
    registry = _load_case_spec_overrides(p2)
    assert "sid-1" in registry.applied
    assert registry.applied["sid-1"].migrated_from_legacy is True


# ---------------------------------------------------------------------------
# 5. Duplicate source_session_id fails
# ---------------------------------------------------------------------------


def test_duplicate_source_session_id_fails(tmp_path: Path) -> None:
    p = _write_overrides(tmp_path, {
        "version": 2,
        "overrides": [
            {
                "source_session_id": "dup",
                "status": "approved",
                "source": "x",
                "reviewer": "y",
                "date": "2026-04-30",
                "confidence": "high",
                "rationale": "first",
                "supporting_turn_numbers": [1],
                "classification": {"primary_uc": "UC-C"},
            },
            {
                "source_session_id": "dup",
                "status": "pending_review",
                "rationale": "second",
            },
        ],
    })
    with pytest.raises(ValueError) as exc:
        _load_case_spec_overrides(p)
    assert "duplicate" in str(exc.value).lower()


# ---------------------------------------------------------------------------
# 6. Unknown UC in classification fails
# ---------------------------------------------------------------------------


def test_unknown_uc_in_classification_fails(tmp_path: Path) -> None:
    p = _write_overrides(tmp_path, {
        "version": 2,
        "overrides": [{
            "source_session_id": "sid-1",
            "status": "approved",
            "source": "x",
            "reviewer": "y",
            "date": "2026-04-30",
            "confidence": "high",
            "rationale": "z",
            "supporting_turn_numbers": [1],
            "classification": {"primary_uc": "UC-Z"},  # unknown
        }],
    })
    with pytest.raises(ValueError) as exc:
        _load_case_spec_overrides(p)
    assert "UC-Z" in str(exc.value)


# ---------------------------------------------------------------------------
# 7. expected.primary_uc / secondary_ucs is rejected with hint
# ---------------------------------------------------------------------------


def test_expected_block_with_primary_uc_fails(tmp_path: Path) -> None:
    p = _write_overrides(tmp_path, {
        "version": 2,
        "overrides": [{
            "source_session_id": "sid-1",
            "status": "approved",
            "source": "x",
            "reviewer": "y",
            "date": "2026-04-30",
            "confidence": "high",
            "rationale": "z",
            "supporting_turn_numbers": [1],
            "expected": {"primary_uc": "UC-FP"},
        }],
    })
    with pytest.raises(ValueError) as exc:
        _load_case_spec_overrides(p)
    assert "classification.primary_uc" in str(exc.value)


# ---------------------------------------------------------------------------
# 8. persona block disallowed keys hard-fail
# ---------------------------------------------------------------------------


def test_persona_block_with_disallowed_keys_fails(tmp_path: Path) -> None:
    p = _write_overrides(tmp_path, {
        "version": 2,
        "overrides": [{
            "source_session_id": "sid-1",
            "status": "approved",
            "source": "x",
            "reviewer": "y",
            "date": "2026-04-30",
            "confidence": "high",
            "rationale": "z",
            "supporting_turn_numbers": [1],
            "persona": {"verbosity": "verbose"},  # disallowed
        }],
    })
    with pytest.raises(ValueError) as exc:
        _load_case_spec_overrides(p)
    assert "verbosity" in str(exc.value)


# ---------------------------------------------------------------------------
# 9. pending_review override does NOT apply, but appears in audit
# ---------------------------------------------------------------------------


def test_pending_review_does_not_apply(tmp_path: Path) -> None:
    """A pending_review entry is loaded into registry.pending but never
    mutates the spec.

    Use ``570Q5000008l7flIAA`` (cs_interactive_064, UC-C resolve) which is
    NOT in any approved override. A pending_review override that would have
    flipped both classification and outcome_class is loaded into the audit
    but ignored when computing the spec.
    """
    overrides_path = _write_overrides(tmp_path, {
        "version": 2,
        "overrides": [{
            "source_session_id": "570Q5000008l7flIAA",
            "status": "pending_review",
            "rationale": "DRAFT — needs further review before applying",
            "classification": {"primary_uc": "UC-K"},
            "expected": {"outcome_class": "escalate"},
        }],
    })

    out_dir = tmp_path / "specs"
    cache_dir = tmp_path / "cache"
    specs = extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        out_dir,
        overrides_path=overrides_path,
        llm_offline=True,
        llm_cache_dir=cache_dir,
    )
    spec = next(s for s in specs if s.source_session_id == "570Q5000008l7flIAA")
    # Pending override does NOT apply: rule-derived UC-C / resolve survives.
    assert spec.expected.primary_uc == "UC-C"
    assert spec.expected.outcome_class == "resolve"
    audit_path = dump_audit_to(tmp_path / "audit.md")
    audit_text = audit_path.read_text(encoding="utf-8")
    assert "pending_review overrides (NOT applied)" in audit_text
    assert "DRAFT" in audit_text


# ---------------------------------------------------------------------------
# 10. strict_overrides hard-fails on pending
# ---------------------------------------------------------------------------


def test_strict_overrides_hard_fails_on_pending(tmp_path: Path) -> None:
    overrides_path = _write_overrides(tmp_path, {
        "version": 2,
        "overrides": [{
            "source_session_id": "570Q5000008hx9tIAA",
            "status": "pending_review",
            "rationale": "DRAFT — needs further review before applying",
        }],
    })
    out_dir = tmp_path / "specs"
    cache_dir = tmp_path / "cache"
    with pytest.raises(ValueError) as exc:
        extract_case_specs(
            HR_CSV,
            TURNS_DIR,
            out_dir,
            overrides_path=overrides_path,
            llm_offline=True,
            llm_cache_dir=cache_dir,
            strict_overrides=True,
        )
    assert "pending_review" in str(exc.value)
    assert "570Q5000008hx9tIAA" in str(exc.value)


# ---------------------------------------------------------------------------
# 11. classification override applied BEFORE policy lookup
# ---------------------------------------------------------------------------


def test_classification_override_applied_before_policy(tmp_path: Path) -> None:
    """Pin a migrated UC-B legacy session — its primary_uc must be
    replaced before policy lookup, so all policy-derived expected.* fields
    reflect the new UC."""
    out_dir = tmp_path / "specs"
    cache_dir = tmp_path / "cache"
    specs = extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        out_dir,
        llm_offline=True,
        llm_cache_dir=cache_dir,
    )
    # 570Q5000008iwZxIAI ("advert on hold 2nd time, please restore") is
    # migrated to UC-K with UC-FP/UC-B fallback. UC-K policy has
    # allow_bot_resolution=partial — verifies the classification override
    # is applied before the L1 policy lookup.
    spec = next(s for s in specs if s.source_session_id == "570Q5000008iwZxIAI")
    assert spec.expected.primary_uc == "UC-K"
    assert spec.expected.secondary_ucs == ["UC-FP", "UC-B"]
    assert spec.expected.allow_bot_resolution == "partial"


# ---------------------------------------------------------------------------
# 12. expected override applied AFTER L1 (Wave A5 timing)
# ---------------------------------------------------------------------------


def test_expected_override_applied_after_l1() -> None:
    """The cs_interactive_012 expected block flips outcome_class /
    should_escalate / escalation_trigger / expected_tool_sequence to the
    reviewed values."""
    spec = _build_minimal_spec("570Q5000008hx9tIAA")
    spec.expected = Expected(
        outcome_class="escalate",
        primary_uc="UC-FP",
        secondary_ucs=["UC-K"],
        should_escalate=True,
        allow_bot_resolution="partial",
        bot_handling_pattern="...",
        escalation_trigger="user_requested",
        risk_level="low",
        expected_tool_sequence=[
            "get_customer_context",
            "search_knowledge",
            "request_handover",
            "record_outcome",
        ],
        forbidden_tools=[],
        grounding_mode="kb_only",
        answer_must_not_contain=[],
        max_turns=15,
    )
    changes = _apply_expected_override(spec, {
        "outcome_class": "resolve",
        "should_escalate": False,
        "escalation_trigger": None,
        "expected_tool_sequence": [
            "get_customer_context",
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
    })
    assert "outcome_class" in changes
    assert spec.expected.outcome_class == "resolve"
    assert spec.expected.should_escalate is False
    assert spec.expected.escalation_trigger is None
    assert "request_handover" not in spec.expected.expected_tool_sequence


# ---------------------------------------------------------------------------
# 13. persona override applied AFTER L2 cache, even when cache hit exists
# ---------------------------------------------------------------------------


def test_persona_override_applied_after_l2_cache() -> None:
    """Apply a persona override to a spec whose L2 already populated a
    persona; assert that the override wins and verbosity is recomputed."""
    spec = _build_minimal_spec("sid-x")
    # Make the pre-override seeds long; verbose category.
    spec.persona = Persona(
        user_goal_summary="L2 produced this summary. Drift: none.",
        frustration_level="none",
        verbosity="verbose",
        drift_behavior="none",
        seed_messages=[
            "x" * 110,
            "y" * 110,
        ],
        hidden_facts=[HiddenFact(fact="ad ID is 12345", disclose_when="if asked")],
        will_request_human_if="",
    )
    changes = _apply_persona_override(spec, {
        "seed_messages": ["short", "tiny"],  # avg=4 -> terse
        "user_goal_summary": "Pinned summary.",
    })
    assert "seed_messages" in changes
    assert spec.persona.seed_messages == ["short", "tiny"]
    assert spec.persona.user_goal_summary == "Pinned summary."
    # Verbosity recomputed from new seeds (avg < 20 chars => terse).
    assert spec.persona.verbosity == "terse"


# ---------------------------------------------------------------------------
# 14. legacy UC-B session picks up override in full extraction
# ---------------------------------------------------------------------------


def test_legacy_uc_b_session_picks_up_override_in_full_extraction(
    tmp_path: Path,
) -> None:
    out_dir = tmp_path / "specs"
    cache_dir = tmp_path / "cache"
    specs = extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        out_dir,
        llm_offline=True,
        llm_cache_dir=cache_dir,
    )
    # 570Q5000008caqfIAA is a legacy UC-B -> UC-D entry with empty secondary.
    spec = next(s for s in specs if s.source_session_id == "570Q5000008caqfIAA")
    assert spec.expected.primary_uc == "UC-D"
    assert spec.expected.secondary_ucs == []


# ---------------------------------------------------------------------------
# 15. cs_interactive_012 end-to-end: classification + expected merged entry
# ---------------------------------------------------------------------------


def test_cs_interactive_012_override_survives_fresh_extraction(tmp_path: Path) -> None:
    """A fresh offline extraction keeps reviewed cs_interactive_012
    resolve-first via the merged classification + expected override."""
    out_dir = tmp_path / "case_specs"
    cache_dir = tmp_path / "cache"
    specs = extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        out_dir,
        llm_offline=True,
        llm_cache_dir=cache_dir,
    )
    spec = next(s for s in specs if s.case_id == "cs_interactive_012")
    expected = spec.expected
    assert spec.source_session_id == "570Q5000008hx9tIAA"
    assert expected.primary_uc == "UC-FP"
    assert expected.secondary_ucs == ["UC-K"]
    assert expected.outcome_class == "resolve"
    assert expected.should_escalate is False
    assert expected.escalation_trigger is None
    assert expected.expected_tool_sequence == [
        "get_customer_context",
        "search_knowledge",
        "resolve_article",
        "record_outcome",
    ]
    assert "request_handover" not in expected.expected_tool_sequence
    assert "resolution_achieved" in spec.scoring.outcome_checks
    assert "escalation_triggered" not in spec.scoring.outcome_checks

    generated = yaml.safe_load(
        (out_dir / "anchor" / "cs_interactive_012.yaml").read_text(encoding="utf-8")
    )
    generated_expected = generated["expected"]
    assert generated_expected["outcome_class"] == "resolve"
    assert generated_expected["should_escalate"] is False
    assert generated_expected["escalation_trigger"] is None
    assert "request_handover" not in generated_expected["expected_tool_sequence"]

    audit_path = dump_audit_to(tmp_path / "case-spec-generation-audit.md")
    audit_text = audit_path.read_text(encoding="utf-8")
    assert "## 570Q5000008hx9tIAA (case_id=cs_interactive_012)" in audit_text
    assert "case-level override applied: **YES**" in audit_text
    assert "Late phone/human request is a failure-path signal" in audit_text
    # Both stages should be flagged in the audit.
    assert "classification override applied: **YES**" in audit_text
    assert "changed expected fields:" in audit_text


# ---------------------------------------------------------------------------
# 16. cs_interactive_015 end-to-end: Wave A6 resolve-first override
# ---------------------------------------------------------------------------


def test_cs_interactive_015_override_survives_fresh_extraction(tmp_path: Path) -> None:
    """A fresh offline extraction keeps reviewed cs_interactive_015
    resolve-first via the merged classification + expected override.

    Phase 2 §2.2 line 282 places "why was my ad deleted?" + reposting under
    UC-FP; the historical late escalation is failure-path evidence and not
    the desired golden behavior. Mirrors cs_interactive_012's pattern.
    """
    out_dir = tmp_path / "case_specs"
    cache_dir = tmp_path / "cache"
    specs = extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        out_dir,
        llm_offline=True,
        llm_cache_dir=cache_dir,
    )
    spec = next(s for s in specs if s.case_id == "cs_interactive_015")
    expected = spec.expected
    assert spec.source_session_id == "570Q5000008WmXxIAK"
    assert expected.primary_uc == "UC-FP"
    assert expected.secondary_ucs == ["UC-B", "UC-K"]
    assert expected.outcome_class == "resolve"
    assert expected.should_escalate is False
    assert expected.escalation_trigger is None
    assert expected.expected_tool_sequence == [
        "get_customer_context",
        "search_knowledge",
        "resolve_article",
        "record_outcome",
    ]
    assert "request_handover" not in expected.expected_tool_sequence
    # UC-FP allows search_knowledge / resolve_article / get_moderation_review_context
    # so they must NOT be forbidden after reclassification.
    assert "search_knowledge" not in expected.forbidden_tools
    assert "resolve_article" not in expected.forbidden_tools
    assert "get_moderation_review_context" not in expected.forbidden_tools
    # get_message_moderation_context (allowed only for UC-C) remains forbidden.
    assert "get_message_moderation_context" in expected.forbidden_tools
    # Custom bot_handling_pattern is pinned by the override.
    assert "edit/repost/change" in expected.bot_handling_pattern
    assert "resolution_achieved" in spec.scoring.outcome_checks
    assert "escalation_triggered" not in spec.scoring.outcome_checks

    generated = yaml.safe_load(
        (out_dir / "anchor" / "cs_interactive_015.yaml").read_text(encoding="utf-8")
    )
    generated_expected = generated["expected"]
    assert generated_expected["primary_uc"] == "UC-FP"
    assert generated_expected["outcome_class"] == "resolve"
    assert generated_expected["should_escalate"] is False
    assert generated_expected["escalation_trigger"] is None
    assert "request_handover" not in generated_expected["expected_tool_sequence"]

    audit_path = dump_audit_to(tmp_path / "case-spec-generation-audit.md")
    audit_text = audit_path.read_text(encoding="utf-8")
    assert "## 570Q5000008WmXxIAK (case_id=cs_interactive_015)" in audit_text
    assert "case-level override applied: **YES**" in audit_text
    assert "failure-path" in audit_text
    # Both stages should be flagged in the audit.
    assert "classification override applied: **YES**" in audit_text
    assert "changed expected fields:" in audit_text
