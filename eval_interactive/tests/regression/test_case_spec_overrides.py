"""Wave A6.6 unified override registry — schema v2 regression tests.

Covers loader validation, the three application stages
(classification / expected / persona), pending-review semantics, and the
``--strict-overrides`` switch.
"""

from __future__ import annotations

import re
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
    # 15 approved entries: 9 legacy + 2 merged (cs_interactive_012,
    # cs_interactive_015) + 1 Sprint 2.1 P1 follow-up (cs_interactive_014,
    # source_session_id=570Q5000008u9gjIAA) + 3 Sprint 4 follow-ups
    # (cs_interactive_011, cs_interactive_029, cs_interactive_066).
    assert len(registry.applied) == 15
    assert registry.pending == []
    cs29 = registry.applied["570Q5000008kDiPIAU"]
    assert cs29.case_id_hint == "cs_interactive_029"
    assert cs29.classification == {
        "primary_uc": "UC-D",
        "secondary_ucs": ["UC-C"],
    }
    assert cs29.expected is None
    assert cs29.migrated_from_legacy is False
    assert cs29.supporting_turn_numbers == (3, 4, 6, 10, 43, 47)
    cs66 = registry.applied["570Q5000008fBsXIAU"]
    assert cs66.case_id_hint == "cs_interactive_066"
    assert cs66.classification == {
        "primary_uc": "UC-K",
        "secondary_ucs": ["UC-E"],
    }
    cs11 = registry.applied["570Q5000008NWIjIAO"]
    assert cs11.case_id_hint == "cs_interactive_011"
    assert cs11.classification is None
    assert cs11.expected is not None
    assert cs11.expected["escalation_trigger"] == "faq_miss_threshold_exceeded"
    cs14 = registry.applied["570Q5000008u9gjIAA"]
    assert cs14.case_id_hint == "cs_interactive_014"
    assert cs14.expected is not None
    assert cs14.expected["escalation_trigger"] == "faq_miss_threshold_exceeded"
    assert cs14.classification is None
    assert cs14.migrated_from_legacy is False
    assert cs14.supporting_turn_numbers == (6, 10, 12, 14, 16)
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


# ---------------------------------------------------------------------------
# 17. cs_interactive_029 end-to-end: classification-only override (Sprint 4 §E2)
# ---------------------------------------------------------------------------


def test_cs_interactive_029_override_survives_fresh_extraction(tmp_path: Path) -> None:
    """Sprint 4 §E2: a classification-only override flips primary_uc
    from UC-C to UC-D (account-locked persona, not messaging-blocked).
    Closes the L2 ``correct_uc`` gap (D12) without changing runtime
    behaviour. Semantic escalation reason stays ``user_requested`` via
    the explicit-callback path (priority 1)."""
    out_dir = tmp_path / "case_specs"
    cache_dir = tmp_path / "cache"
    specs = extract_case_specs(
        HR_CSV,
        TURNS_DIR,
        out_dir,
        llm_offline=True,
        llm_cache_dir=cache_dir,
    )
    spec = next(s for s in specs if s.case_id == "cs_interactive_029")
    expected = spec.expected
    assert spec.source_session_id == "570Q5000008kDiPIAU"
    assert expected.primary_uc == "UC-D"
    assert expected.secondary_ucs == ["UC-C"]
    # The classification override flips the policy lookup so UC-D's
    # forbidden_tools list applies. UC-D forbids the message-moderation
    # tool that is allowed only for UC-C.
    assert "get_message_moderation_context" in expected.forbidden_tools
    # Semantic escalation reason still falls out as user_requested
    # via the runtime explicit-callback path; the spec-derived
    # escalation_trigger inherits from the persona/HR-derived
    # transcript outcome resolver.
    assert expected.escalation_trigger == "user_requested"
    assert expected.outcome_class == "escalate"

    audit_path = dump_audit_to(tmp_path / "case-spec-generation-audit.md")
    audit_text = audit_path.read_text(encoding="utf-8")
    assert "## 570Q5000008kDiPIAU (case_id=cs_interactive_029)" in audit_text
    assert "classification override applied: **YES** -> primary=UC-D" in audit_text
    assert "Sprint 4 §E2" in audit_text


# ---------------------------------------------------------------------------
# 18. Sprint 4 §E3: smoke YAMLs must equal the override-pipeline output.
#
# Lightweight regression check that prevents direct hand edits of the
# committed smoke fixtures without a matching approved override entry.
# A spec whose generated `expected.*` block diverges from the on-disk
# smoke YAML is either a missing override or an unauthorised hand edit.
# ---------------------------------------------------------------------------


def test_smoke_yaml_matches_override_pipeline_output(tmp_path: Path) -> None:
    """Sprint 4 §E3 guard: every committed smoke YAML must equal what
    `extract_case_specs` produces with the production override file.

    Running the extractor in offline mode (`llm_offline=True`) bypasses
    the L2 LLM persona reviewer and uses the rule_draft persona. To keep
    the comparison stable when L2 has cached a richer persona, this
    guard only diffs the `expected.*` block — which is the sprint-4
    contract surface — and requires the smoke YAML's `case_id`,
    `source_session_id`, `source_dataset`, and `expected.*` fields to
    match the regenerated spec exactly. Persona drift between rule_draft
    and the cached LLM proposal is allowed; spec-expected drift is not,
    because expected-field changes must flow through
    `case_spec_overrides.yaml`.
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
    by_case_id = {s.case_id: s for s in specs}

    smoke_dir = REPO_ROOT / "eval_interactive" / "case_specs" / "smoke"
    smoke_files = sorted(smoke_dir.glob("cs_interactive_*.yaml"))
    assert smoke_files, "smoke fixture directory is empty"

    mismatches: list[str] = []
    for smoke_path in smoke_files:
        on_disk = yaml.safe_load(smoke_path.read_text(encoding="utf-8"))
        case_id = on_disk["case_id"]
        regenerated = by_case_id.get(case_id)
        if regenerated is None:
            mismatches.append(
                f"{smoke_path.name}: on-disk smoke YAML's case_id={case_id} "
                f"is not produced by the extractor — regenerate the smoke "
                f"corpus or add the missing source row."
            )
            continue

        regen_expected = {
            "outcome_class": regenerated.expected.outcome_class,
            "primary_uc": regenerated.expected.primary_uc,
            "secondary_ucs": list(regenerated.expected.secondary_ucs),
            "should_escalate": regenerated.expected.should_escalate,
            "allow_bot_resolution": regenerated.expected.allow_bot_resolution,
            "bot_handling_pattern": regenerated.expected.bot_handling_pattern,
            "escalation_trigger": regenerated.expected.escalation_trigger,
            "risk_level": regenerated.expected.risk_level,
            "expected_tool_sequence": list(regenerated.expected.expected_tool_sequence),
            "forbidden_tools": list(regenerated.expected.forbidden_tools),
            "grounding_mode": regenerated.expected.grounding_mode,
            "answer_must_not_contain": list(regenerated.expected.answer_must_not_contain),
            "max_turns": regenerated.expected.max_turns,
        }
        on_disk_expected = on_disk.get("expected") or {}
        # Normalise list-valued fields the same way the extractor does
        # so YAML's flow vs. block style differences do not register as
        # diffs.
        on_disk_expected_norm = {
            "outcome_class": on_disk_expected.get("outcome_class"),
            "primary_uc": on_disk_expected.get("primary_uc"),
            "secondary_ucs": list(on_disk_expected.get("secondary_ucs") or []),
            "should_escalate": on_disk_expected.get("should_escalate"),
            "allow_bot_resolution": on_disk_expected.get("allow_bot_resolution"),
            "bot_handling_pattern": on_disk_expected.get("bot_handling_pattern"),
            "escalation_trigger": on_disk_expected.get("escalation_trigger"),
            "risk_level": on_disk_expected.get("risk_level"),
            "expected_tool_sequence": list(
                on_disk_expected.get("expected_tool_sequence") or []
            ),
            "forbidden_tools": list(on_disk_expected.get("forbidden_tools") or []),
            "grounding_mode": on_disk_expected.get("grounding_mode"),
            "answer_must_not_contain": list(
                on_disk_expected.get("answer_must_not_contain") or []
            ),
            "max_turns": on_disk_expected.get("max_turns"),
        }
        if regen_expected != on_disk_expected_norm:
            differing = sorted(
                key for key in regen_expected
                if regen_expected[key] != on_disk_expected_norm.get(key)
            )
            mismatches.append(
                f"{smoke_path.name}: expected.* differs from override-pipeline "
                f"output on fields {differing}. Run "
                f"`python -m eval_interactive.scripts.regenerate_case_specs` "
                f"and route any reviewed change through "
                f"eval_interactive/case_spec_overrides.yaml — direct hand "
                f"edits to generated smoke YAML are not allowed."
            )

    assert not mismatches, (
        "Sprint 4 §E3: smoke YAML expected-field guard. The committed "
        "smoke fixtures must equal the output of "
        "`extract_case_specs` with the production override registry. "
        "Each mismatch below is either a missing override entry or an "
        "unauthorised hand edit:\n  - " + "\n  - ".join(mismatches)
    )


# ---------------------------------------------------------------------------
# 19. Sprint 4.1 §E3: qa-reports/smoke-case-review.md tracks the smoke set.
#
# Lightweight consistency guard between the on-disk smoke fixtures and the
# committed smoke review report. Catches three classes of staleness without
# rewriting the report generator:
#   - stale report rows after a case leaves smoke (e.g. cs_interactive_004)
#   - missing rows after a case joins smoke    (e.g. cs_interactive_176)
#   - stale recommended outcome lines for cases with approved overrides
#     (the fields the override registry actively pins).
# ---------------------------------------------------------------------------


SMOKE_REVIEW_REPORT = REPO_ROOT / "qa-reports" / "smoke-case-review.md"

_HEADING_RE = re.compile(r"^### (cs_interactive_\d+)\s*$", re.MULTILINE)
_OUTCOME_RE = re.compile(
    r"Recommended outcome:\s*(?P<uc>UC-[A-Z]+)\s+"
    r"(?P<outcome>resolve|escalate)"
    r"(?:,\s*(?P<trigger>`[^`]+`|no escalation trigger))?",
)


def _parse_smoke_review_sections(text: str) -> dict[str, dict[str, str | None]]:
    """Return {case_id: {primary_uc, outcome_class, escalation_trigger}}.

    Only parses the leading ``Recommended outcome`` line of each
    ``### cs_interactive_xxx`` section. Anything that cannot be parsed is
    returned with values None so the test surfaces it as a mismatch.
    """
    result: dict[str, dict[str, str | None]] = {}
    matches = list(_HEADING_RE.finditer(text))
    for idx, m in enumerate(matches):
        case_id = m.group(1)
        start = m.end()
        end = matches[idx + 1].start() if idx + 1 < len(matches) else len(text)
        section = text[start:end]
        outcome_match = _OUTCOME_RE.search(section)
        if not outcome_match:
            result[case_id] = {
                "primary_uc": None,
                "outcome_class": None,
                "escalation_trigger": None,
            }
            continue
        trigger_raw = outcome_match.group("trigger")
        if trigger_raw is None or trigger_raw == "no escalation trigger":
            trigger: str | None = None
        else:
            trigger = trigger_raw.strip("`")
        result[case_id] = {
            "primary_uc": outcome_match.group("uc"),
            "outcome_class": outcome_match.group("outcome"),
            "escalation_trigger": trigger,
        }
    return result


def test_smoke_review_report_tracks_smoke_set_and_overrides() -> None:
    """The committed smoke review report must:

    1. Have one ``### cs_interactive_xxx`` heading per current smoke YAML
       (catches missing rows after a case joins smoke).
    2. Have no headings for cases no longer in the smoke directory
       (catches stale rows after a case leaves smoke).
    3. For each case with an approved expected-field override, the report's
       ``Recommended outcome`` line must reflect the override-pipeline
       fields (catches stale recommended-outcome lines).

    The check is intentionally narrow: it only inspects headings and the
    leading recommended-outcome line. It does not rewrite the report
    generator.
    """
    text = SMOKE_REVIEW_REPORT.read_text(encoding="utf-8")
    sections = _parse_smoke_review_sections(text)

    smoke_dir = REPO_ROOT / "eval_interactive" / "case_specs" / "smoke"
    smoke_yaml_by_case_id: dict[str, dict] = {}
    for path in sorted(smoke_dir.glob("cs_interactive_*.yaml")):
        loaded = yaml.safe_load(path.read_text(encoding="utf-8"))
        smoke_yaml_by_case_id[loaded["case_id"]] = loaded
    assert smoke_yaml_by_case_id, "smoke fixture directory is empty"

    smoke_ids = set(smoke_yaml_by_case_id)
    report_ids = set(sections)

    missing_in_report = sorted(smoke_ids - report_ids)
    stale_in_report = sorted(report_ids - smoke_ids)

    problems: list[str] = []
    for case_id in missing_in_report:
        problems.append(
            f"{case_id}: present in smoke fixtures but missing a "
            f"`### {case_id}` section in qa-reports/smoke-case-review.md"
        )
    for case_id in stale_in_report:
        problems.append(
            f"{case_id}: has a `### {case_id}` section in "
            f"qa-reports/smoke-case-review.md but is not in the current "
            f"smoke fixture set"
        )

    # For overridden cases that are in smoke, the recommended outcome line
    # must match the override-pipeline expected fields.
    registry = _load_case_spec_overrides(PROD_OVERRIDES)
    for case_id, on_disk in smoke_yaml_by_case_id.items():
        session_id = on_disk["source_session_id"]
        override = registry.applied.get(session_id)
        if override is None:
            continue
        if override.classification is None and override.expected is None:
            continue
        section = sections.get(case_id)
        if section is None:
            continue  # already reported above
        expected = on_disk["expected"]
        report_uc = section["primary_uc"]
        report_outcome = section["outcome_class"]
        report_trigger = section["escalation_trigger"]
        spec_uc = expected["primary_uc"]
        spec_outcome = expected["outcome_class"]
        spec_trigger = expected.get("escalation_trigger")
        if (
            report_uc != spec_uc
            or report_outcome != spec_outcome
            or report_trigger != spec_trigger
        ):
            problems.append(
                f"{case_id}: smoke-case-review recommended outcome "
                f"({report_uc} {report_outcome}, "
                f"{report_trigger or 'no escalation trigger'}) does not "
                f"match the override-pipeline smoke YAML "
                f"({spec_uc} {spec_outcome}, "
                f"{spec_trigger or 'no escalation trigger'}). Update the "
                f"`### {case_id}` section to reflect the approved override."
            )

    assert not problems, (
        "Sprint 4.1 §E3: qa-reports/smoke-case-review.md must track the "
        "current smoke fixture set and approved overrides:\n  - "
        + "\n  - ".join(problems)
    )
