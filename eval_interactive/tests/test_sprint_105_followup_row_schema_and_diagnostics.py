"""Sprint 105 follow-up: placeholder-row schema + spec-load diagnostics.

Covers the two cheap framework defects recorded in
``docs/sprints/sprint-105-handoff.md`` §7 as found-but-not-fixed:

* **item 6** — ``contract_warnings`` was absent from the timeout /
  cancelled / error / contract-violation rows, so a consumer doing
  ``case["contract_warnings"]`` — which the populated row has always
  supported — raised ``KeyError`` on exactly the runs where something
  went wrong. The key is now present and empty on every row shape.
  Empty is the honest value: those rows never reached trace collection,
  so no lenient-mode warning was ever recorded for them.

* **item 7** — a corpus load emitted five identical copies of the
  ``escalation_trigger is None with should_escalate=true`` advisory with
  no case id attached, which made them unactionable. The diagnostic now
  names the spec that produced it.

The last test is the important one for the corpus: ``spec_context`` is a
diagnostic label, and it must never round-trip into a written CaseSpec
YAML, or every re-extraction would churn the corpus.
"""

from __future__ import annotations

import logging
from types import SimpleNamespace

import pytest

from eval_interactive.batch.executor import BatchExecutor
from eval_interactive.case_spec.extractor import _casespec_to_dict
from eval_interactive.case_spec.loader import _parse_case_spec
from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.trace.collector import TraceContractError


class _StubExecutor(BatchExecutor):
    """BatchExecutor with Config init skipped (same pattern as
    ``test_case_passed_authority.py``)."""

    def __init__(self) -> None:
        self._config = SimpleNamespace(
            batch=SimpleNamespace(timeout_per_session_seconds=60),
        )
        self._skill_extractor = None


def _case(case_id: str = "ut-followup") -> CaseSpec:
    return CaseSpec(
        case_id=case_id,
        source_session_id=case_id,
        source_dataset="ut",
        form_context=FormContext(
            first_name="U",
            email="u@example.com",
            topic_subject="Ad Support",
            ad_id="",
            description="test",
        ),
        persona=Persona(
            user_goal_summary="ut",
            frustration_level="none",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["hi"],
            hidden_facts=[],
            will_request_human_if="",
        ),
        expected=Expected(
            outcome_class="resolve",
            primary_uc="UC-B",
            secondary_ucs=[],
            should_escalate=False,
            allow_bot_resolution="true",
            bot_handling_pattern="ut",
            escalation_trigger=None,
        ),
        scoring=ScoringConfig(
            hard_checks=[],
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
        source_suite="anchor",
    )


def _violation() -> TraceContractError:
    return TraceContractError(
        field="phase_plan",
        phase="DISCOVER",
        reason="missing",
        available_keys=["projection"],
        session_id="sess-violation",
    )


# ---------------------------------------------------------------------------
# item 6 — uniform contract_warnings key
# ---------------------------------------------------------------------------


@pytest.mark.parametrize(
    "builder",
    [
        pytest.param(lambda ex, c: ex._timeout_result(c), id="timeout"),
        pytest.param(lambda ex, c: ex._cancelled_result(c, "deadline"), id="cancelled"),
        pytest.param(lambda ex, c: ex._error_result(c, "boom"), id="error"),
        pytest.param(
            lambda ex, c: ex._contract_violation_result(c, _violation()),
            id="contract_violation",
        ),
    ],
)
def test_every_placeholder_row_carries_contract_warnings(builder) -> None:
    """§7 item 6: subscripting the key must not raise on any row shape."""
    row = builder(_StubExecutor(), _case())
    # Subscript, not .get() — the point is that the KeyError is gone.
    assert row["contract_warnings"] == []


def test_contract_violation_l1_row_carries_severity() -> None:
    """The populated path serialises ``severity`` on every L1 row because
    ``composite.py`` filters advisory results out of the L1 gate. The
    synthetic row dropped it, forcing a consumer to branch on status."""
    row = _StubExecutor()._contract_violation_result(_case(), _violation())
    assert row["l1_results"][0]["severity"] == "critical"


# ---------------------------------------------------------------------------
# item 7 — the load-time advisory names its spec
# ---------------------------------------------------------------------------


def _raw_spec(case_id: str) -> dict:
    """Minimal raw YAML body whose Expected trips the advisory warning."""
    return {
        "case_id": case_id,
        "source_session_id": "sess-ut",
        "source_dataset": "ut",
        "form_context": {
            "first_name": "U",
            "email": "u@example.com",
            "topic_subject": "Ad Support",
        },
        "persona": {
            "user_goal_summary": "ut",
            "frustration_level": "none",
            "verbosity": "normal",
            "drift_behavior": "none",
        },
        "expected": {
            "outcome_class": "escalate",
            "primary_uc": "UC-H",
            "should_escalate": True,
            # No escalation_trigger — this is what emits the advisory.
        },
        "scoring": {},
    }


def test_spec_load_advisory_warning_names_the_case_and_path(caplog) -> None:
    with caplog.at_level(logging.WARNING, logger="eval_interactive.case_spec.schema"):
        _parse_case_spec(_raw_spec("cs_ut_advisory"), source_path="case_specs/ut/x.yaml")

    advisories = [
        rec.getMessage()
        for rec in caplog.records
        if "escalation_trigger is None with should_escalate=true" in rec.getMessage()
    ]
    assert len(advisories) == 1
    assert "cs_ut_advisory" in advisories[0]
    assert "case_specs/ut/x.yaml" in advisories[0]


def test_two_specs_produce_two_distinguishable_advisories(caplog) -> None:
    """Five identical un-attributed copies were the actual defect."""
    with caplog.at_level(logging.WARNING, logger="eval_interactive.case_spec.schema"):
        _parse_case_spec(_raw_spec("cs_ut_one"), source_path="a.yaml")
        _parse_case_spec(_raw_spec("cs_ut_two"), source_path="b.yaml")

    advisories = [
        rec.getMessage()
        for rec in caplog.records
        if "escalation_trigger is None with should_escalate=true" in rec.getMessage()
    ]
    assert len(advisories) == 2
    assert len(set(advisories)) == 2


def test_direct_construction_still_warns_without_a_context(caplog) -> None:
    """The schema-level guard is unchanged for callers that build an
    ``Expected`` directly (fixtures, tests); it simply has no id to name."""
    with caplog.at_level(logging.WARNING, logger="eval_interactive.case_spec.schema"):
        exp = Expected(
            outcome_class="escalate",
            primary_uc="UC-H",
            secondary_ucs=[],
            should_escalate=True,
            allow_bot_resolution="false",
            escalation_trigger=None,
        )
    assert exp.spec_context is None
    assert any(
        "escalation_trigger is None with should_escalate=true" in rec.getMessage()
        for rec in caplog.records
    )


def test_spec_context_never_round_trips_into_a_written_spec(tmp_path) -> None:
    """``spec_context`` is a diagnostic label, not ground truth. If it ever
    reached ``_casespec_to_dict`` the next re-extraction would rewrite every
    spec in the corpus with a machine-local path in it."""
    spec = _parse_case_spec(_raw_spec("cs_ut_roundtrip"), source_path="x.yaml")
    assert spec.expected.spec_context == "cs_ut_roundtrip (x.yaml)"

    written = _casespec_to_dict(spec)
    assert "spec_context" not in written["expected"]
    assert "spec_context" not in written
