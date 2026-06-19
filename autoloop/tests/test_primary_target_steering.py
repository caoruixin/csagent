"""S-Auto-41 — PRIMARY-target proposer steering (R-autoloop-feedback-loop-thinness
follow-up). Proposer-INPUT enrichment + required causal hypothesis + lightweight
OFF_TARGET pre-check. No keep-gate / conditional / baseline change.
"""

from __future__ import annotations

import pytest

from autoloop.config_validator import (
    OffTargetVerdict,
    off_target_precheck,
    render_primary_target_steering_block,
)
from autoloop.meta_agent.proposer import Hypothesis, _validate_and_build


_RESOLVE = "server/src/main/resources/skills/resolve_faq_grounded_answer.yaml"
_ESCALATE = "server/src/main/resources/skills/escalate.yaml"
_STEER = {
    "enabled": True,
    "primary_relevant_skills": [_RESOLVE],
    "on_target_keywords": ["UC-A", "listing", "entity context"],
}


def _hyp(**kw):
    base = dict(
        target_skill_file=_RESOLVE,
        target_field_path="$.procedure",
        before_value="a",
        after_value="b",
        rationale="r",
        causal_hypothesis="ground the answer in the listing for UC-A",
        expected_trace_change="trace shows listing fields used",
    )
    base.update(kw)
    return Hypothesis(**base)


# --- render block ----------------------------------------------------

def test_render_block_only_when_enabled():
    assert render_primary_target_steering_block({"enabled": False}) == []
    out = render_primary_target_steering_block(
        {"enabled": True, "targets": [{"case_id": "x"}], "failure_clusters": ["c1"]}
    )
    assert out and out[0] == "PRIMARY_TARGET_STEERING:"
    body = "\n".join(out)
    assert "REQUIRED" in body and "pre-loaded" in body and "SUBSTANTIVE" in body


# --- OFF_TARGET pre-check --------------------------------------------

def test_on_target_passes():
    assert off_target_precheck(_hyp(), _STEER) == OffTargetVerdict(True, "on_target")


def test_wrong_skill_is_off_target():
    v = off_target_precheck(_hyp(target_skill_file=_ESCALATE), _STEER)
    assert not v.on_target and v.reason.startswith("skill_not_primary_relevant")


def test_no_keyword_is_off_target():
    v = off_target_precheck(
        _hyp(causal_hypothesis="generic wording tweak", expected_trace_change="x",
             rationale="y"),
        _STEER,
    )
    assert not v.on_target
    assert v.reason == "causal_hypothesis_misses_all_on_target_keywords"


def test_keyword_can_come_from_rationale():
    v = off_target_precheck(
        _hyp(causal_hypothesis="tweak", expected_trace_change="x",
             rationale="targets the UC-A listing flow"),
        _STEER,
    )
    assert v.on_target


def test_disabled_is_noop_on_target():
    v = off_target_precheck(_hyp(target_skill_file=_ESCALATE), {"enabled": False})
    assert v.on_target and v.reason == "steering_disabled"


# --- required causal hypothesis (validation) -------------------------

def _parsed(**kw):
    base = dict(
        target_skill_file=_RESOLVE,
        target_field_path="$.grounding_instruction",
        before_value="a",
        after_value="b",
        rationale="r",
        causal_hypothesis="UC-A listing grounding",
        expected_trace_change="listing fields shown",
        surface_rationale="grounding_instruction is escalation-free, narrower than procedure",
        blast_radius="resolve_faq FAQ UCs only; cannot reach UC-J intake",
        escalation_preservation="did not touch escalation_policy; precedence unchanged",
        no_benchmark_encoding="generic CS prose; no case names or ad IDs",
    )
    base.update(kw)
    return base


def _build(parsed, require_causal):
    return _validate_and_build(
        parsed,
        allowed_files=[_RESOLVE],
        allowed_paths=["$.procedure", "$.grounding_instruction"],
        attempts_used=1,
        raw_response="{}",
        require_causal=require_causal,
    )


@pytest.mark.parametrize(
    "field",
    [
        "causal_hypothesis",
        "expected_trace_change",
        "surface_rationale",
        "blast_radius",
        "escalation_preservation",
        "no_benchmark_encoding",
    ],
)
def test_six_disclosures_required_when_steering_on(field):
    with pytest.raises(ValueError, match=f"missing_field_{field}"):
        _build(_parsed(**{field: ""}), require_causal=True)


def test_causal_fields_optional_when_steering_off():
    # Legacy / pre-pilot path: no causal fields required, prompt unchanged.
    h = _build(_parsed(causal_hypothesis="", expected_trace_change=""), require_causal=False)
    assert h.causal_hypothesis == "" and h.expected_trace_change == ""


def test_causal_fields_passthrough():
    h = _build(_parsed(), require_causal=True)
    assert h.causal_hypothesis == "UC-A listing grounding"
    assert h.expected_trace_change == "listing fields shown"
