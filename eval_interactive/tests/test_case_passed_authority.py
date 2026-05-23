"""S-Cleanup-2 (M4-Eval-Cleanup): case_passed_authority resolution.

Regression tests for the executor's per-case ``case_passed_authority``
annotation introduced in Sprint 48 / S-Cleanup-2 (Item #2). The
annotation surfaces the ``iteration_governance.md`` §5.6 distinction
between programmatic acceptance gates (anchor / smoke / promotion /
exploration) and human-judgment acceptance gates (``bad_cases`` /
``anchor_outcome``) on every result row without changing the
underlying composite / case_passed computation.

The properties under test:

1. ``is_human_judgment_suite`` returns True iff its argument is
   registered in ``_OPT_IN_SETS`` (currently ``bad_cases`` +
   ``anchor_outcome``); returns False for any other suite name and
   for ``None``.
2. ``_resolve_case_passed_authority`` returns ``"human_review"`` for
   a CaseSpec whose ``source_suite`` matches an opt-in suite and
   ``"programmatic"`` otherwise — including when ``source_suite`` is
   ``None`` (defensive default).
3. ``load_case_spec`` populates ``source_suite`` from the YAML's
   parent directory name when no explicit override is supplied, so
   a fixture at ``case_specs/bad_cases/<x>.yaml`` carries
   ``source_suite="bad_cases"``.
4. ``_build_case_result``, ``_timeout_result``, ``_error_result``,
   and ``_contract_violation_result`` all emit a
   ``case_passed_authority`` field on the returned dict, with value
   determined by the CaseSpec's ``source_suite``.
"""

from __future__ import annotations

from pathlib import Path
from types import SimpleNamespace

from eval_interactive.batch.executor import (
    BatchExecutor,
    _resolve_case_passed_authority,
)
from eval_interactive.batch.sets import (
    _KNOWN_SETS,
    _OPT_IN_SETS,
    is_human_judgment_suite,
)
from eval_interactive.case_spec.loader import load_case_spec
from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.composite import CompositeScore
from eval_interactive.scoring.skill_procedure_check import Tier2Result
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.simulator.session_runner import SessionResult
from eval_interactive.trace.collector import TraceContractError


EVAL_ROOT = Path(__file__).resolve().parents[1]


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


def _build_case(case_id: str, *, source_suite: str | None) -> CaseSpec:
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
            risk_level="low",
            expected_tool_sequence=[],
            forbidden_tools=[],
            grounding_mode="faq_source_backed",
            answer_must_not_contain=[],
            max_turns=1,
        ),
        scoring=ScoringConfig(
            hard_checks=[],
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
        source_suite=source_suite,
    )


class _StubExecutor(BatchExecutor):
    """BatchExecutor that skips Config init (mirrors the pattern in
    ``test_executor_per_turn_trace_enrichment.py``)."""

    def __init__(self) -> None:
        self._config = SimpleNamespace(
            batch=SimpleNamespace(timeout_per_session_seconds=60),
        )
        self._skill_extractor = None


def _make_session_result() -> SessionResult:
    return SessionResult(
        session_id="sess-xyz",
        case_id="ut",
        transcript=[{"role": "user", "content": "hi"}],
        total_turns=1,
        stop_reason="resolved",
        elapsed_ms=1000,
    )


def _make_trace_data():
    return SimpleNamespace(
        session_state=SimpleNamespace(
            session_id="sess-xyz",
            active_use_case="UC-B",
            candidate_use_cases=[],
            containment_outcome="resolved",
            escalation_reason="",
            total_bot_turns=0,
            clarification_count=0,
            faq_miss_count=0,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="CLOSE",
        ),
        turns=[],
        events=[],
        handover=None,
        contract_warnings=[],
    )


def _make_composite_score() -> CompositeScore:
    return CompositeScore(
        case_id="ut",
        case_passed=True,
        composite=0.9,
        outcome_score=0.9,
        judge_score=0.9,
        failure_tags=[],
        l1_results=[],
        l2_results=[],
        l3_results=[],
        stall_result=StallResult(detected=False, failure_tag=""),
        tier2_result=Tier2Result(
            passed=True,
            severity="advisory",
            failed_step_ids=(),
            detail="no critical_steps applied",
            per_step=(),
        ),
    )


# ---------------------------------------------------------------------------
# 1. is_human_judgment_suite membership
# ---------------------------------------------------------------------------


def test_is_human_judgment_suite_recognises_opt_in_suites() -> None:
    for name in _OPT_IN_SETS:
        assert is_human_judgment_suite(name) is True, name


def test_is_human_judgment_suite_rejects_known_programmatic_suites() -> None:
    for name in _KNOWN_SETS:
        assert is_human_judgment_suite(name) is False, name


def test_is_human_judgment_suite_rejects_none_and_unknown() -> None:
    assert is_human_judgment_suite(None) is False
    assert is_human_judgment_suite("") is False
    assert is_human_judgment_suite("custom-suite-not-registered") is False


# ---------------------------------------------------------------------------
# 2. _resolve_case_passed_authority on a CaseSpec
# ---------------------------------------------------------------------------


def test_resolve_authority_returns_human_review_for_bad_cases() -> None:
    case = _build_case("ut-bc", source_suite="bad_cases")
    assert _resolve_case_passed_authority(case) == "human_review"


def test_resolve_authority_returns_human_review_for_anchor_outcome() -> None:
    case = _build_case("ut-ao", source_suite="anchor_outcome")
    assert _resolve_case_passed_authority(case) == "human_review"


def test_resolve_authority_returns_programmatic_for_anchor() -> None:
    case = _build_case("ut-anchor", source_suite="anchor")
    assert _resolve_case_passed_authority(case) == "programmatic"


def test_resolve_authority_defaults_to_programmatic_when_suite_is_none() -> None:
    """Defensive default: unknown / None suite must NOT silently
    surface human_review (which would lift programmatic gates)."""
    case = _build_case("ut-no-suite", source_suite=None)
    assert _resolve_case_passed_authority(case) == "programmatic"


def test_resolve_authority_defaults_to_programmatic_for_unknown_suite() -> None:
    """Same defensive default for suite names that don't match any
    registered opt-in suite (covers custom paths the loader couldn't
    classify)."""
    case = _build_case("ut-custom", source_suite="custom-suite-x")
    assert _resolve_case_passed_authority(case) == "programmatic"


# ---------------------------------------------------------------------------
# 3. load_case_spec populates source_suite from parent directory
# ---------------------------------------------------------------------------


def test_load_case_spec_infers_source_suite_for_bad_cases() -> None:
    path = (
        EVAL_ROOT
        / "case_specs"
        / "bad_cases"
        / "alice_uc_a_uc_h_misclass.yaml"
    )
    cs = load_case_spec(path)
    assert cs.source_suite == "bad_cases"
    assert _resolve_case_passed_authority(cs) == "human_review"


def test_load_case_spec_infers_source_suite_for_anchor_outcome() -> None:
    anchor_outcome_dir = EVAL_ROOT / "case_specs" / "anchor_outcome"
    yaml_files = sorted(anchor_outcome_dir.glob("*.yaml"))
    assert yaml_files, "anchor_outcome dir must carry at least one fixture"
    cs = load_case_spec(yaml_files[0])
    assert cs.source_suite == "anchor_outcome"
    assert _resolve_case_passed_authority(cs) == "human_review"


def test_load_case_spec_infers_source_suite_for_programmatic_anchor() -> None:
    anchor_dir = EVAL_ROOT / "case_specs" / "anchor"
    yaml_files = sorted(anchor_dir.glob("*.yaml"))
    assert yaml_files, "anchor dir must carry at least one fixture"
    cs = load_case_spec(yaml_files[0])
    assert cs.source_suite == "anchor"
    assert _resolve_case_passed_authority(cs) == "programmatic"


def test_load_case_spec_explicit_override_wins(tmp_path: Path) -> None:
    """An explicit ``source_suite`` arg overrides the parent-dir
    inference (the override path is used by ``CaseSetManager`` if it
    needs to relabel a CaseSpec without moving the file)."""
    src = (
        EVAL_ROOT
        / "case_specs"
        / "anchor"
    )
    yaml_files = sorted(src.glob("*.yaml"))
    cs = load_case_spec(yaml_files[0], source_suite="bad_cases")
    assert cs.source_suite == "bad_cases"
    assert _resolve_case_passed_authority(cs) == "human_review"


# ---------------------------------------------------------------------------
# 4. _build_case_result / placeholder builders emit authority
# ---------------------------------------------------------------------------


def test_build_case_result_emits_human_review_for_bad_cases() -> None:
    executor = _StubExecutor()
    case = _build_case("ut-bc", source_suite="bad_cases")
    result = executor._build_case_result(
        case,
        _make_session_result(),
        _make_trace_data(),
        _make_composite_score(),
        llm_calls=[],
    )
    assert result["case_passed_authority"] == "human_review"
    # Underlying programmatic verdict is still surfaced; the authority
    # field annotates it, it does not replace it.
    assert "case_passed" in result
    assert "composite_score" in result


def test_build_case_result_emits_programmatic_for_anchor() -> None:
    executor = _StubExecutor()
    case = _build_case("ut-anchor", source_suite="anchor")
    result = executor._build_case_result(
        case,
        _make_session_result(),
        _make_trace_data(),
        _make_composite_score(),
        llm_calls=[],
    )
    assert result["case_passed_authority"] == "programmatic"


def test_timeout_result_emits_authority() -> None:
    executor = _StubExecutor()
    case = _build_case("ut-bc-timeout", source_suite="bad_cases")
    result = executor._timeout_result(case)
    assert result["case_passed_authority"] == "human_review"
    assert result["status"] == "TIMEOUT"


def test_error_result_emits_authority() -> None:
    executor = _StubExecutor()
    case = _build_case("ut-anchor-error", source_suite="anchor")
    result = executor._error_result(case, "boom")
    assert result["case_passed_authority"] == "programmatic"
    assert result["status"] == "ERROR"


def test_contract_violation_result_emits_authority() -> None:
    executor = _StubExecutor()
    case = _build_case("ut-ao-violation", source_suite="anchor_outcome")
    exc = TraceContractError(
        field="phase_plan",
        phase="DISCOVER",
        reason="missing",
        available_keys=["projection"],
        session_id="sess-violation",
    )
    result = executor._contract_violation_result(case, exc)
    assert result["case_passed_authority"] == "human_review"
    assert result["status"] == "CONTRACT_VIOLATION"
