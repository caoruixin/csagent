"""Tests for the HardChecker (L1) module."""

from __future__ import annotations

import pytest

from eval_interactive.case_spec.schema import (
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring.hard_checks import HardChecker, HardCheckResult
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.trace.models import (
    EventEntry,
    HandoverData,
    SessionState,
    TraceData,
    TurnTrace,
)


def _make_case_spec(
    hard_checks: list[str] | None = None,
    outcome_class: str = "resolve",
    primary_uc: str = "UC-A",
    should_escalate: bool = False,
    risk_level: str = "low",
    forbidden_tools: list[str] | None = None,
    grounding_mode: str = "faq_source_backed",
    answer_must_not_contain: list[str] | None = None,
    max_turns: int = 10,
) -> CaseSpec:
    if hard_checks is None:
        hard_checks = HardChecker.ALL_CHECKS
    # Wave A1.1: ``Expected`` now requires ``allow_bot_resolution`` and
    # ``bot_handling_pattern``; ``escalation_trigger`` must be a canonical
    # enum value when ``should_escalate=True`` and None otherwise.
    escalation_trigger = "intake_complete_for_uc_k" if should_escalate else None
    return CaseSpec(
        case_id="test-hc-001",
        source_session_id="sess-001",
        source_dataset="test",
        form_context=FormContext(
            first_name="Test",
            email="test@test.com",
            topic_subject="Test topic",
        ),
        persona=Persona(
            user_goal_summary="Test goal",
            frustration_level="none",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["Hello"],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class=outcome_class,
            primary_uc=primary_uc,
            secondary_ucs=[],
            should_escalate=should_escalate,
            allow_bot_resolution="true",
            bot_handling_pattern="<test fixture>",
            escalation_trigger=escalation_trigger,
            risk_level=risk_level,
            forbidden_tools=forbidden_tools or [],
            grounding_mode=grounding_mode,
            answer_must_not_contain=answer_must_not_contain or [],
            max_turns=max_turns,
        ),
        scoring=ScoringConfig(
            hard_checks=hard_checks,
            outcome_checks=[],
            llm_judge_dimensions=[],
        ),
    )


def _make_trace(
    turns: list[TurnTrace] | None = None,
    active_use_case: str = "UC-A",
    containment_outcome: str = "resolved",
    total_bot_turns: int = 3,
    clarification_count: int = 0,
    faq_miss_count: int = 0,
    handover: HandoverData | None = None,
) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="test-session",
            active_use_case=active_use_case,
            candidate_use_cases=[],
            containment_outcome=containment_outcome,
            escalation_reason="",
            total_bot_turns=total_bot_turns,
            clarification_count=clarification_count,
            faq_miss_count=faq_miss_count,
            form_context={},
            customer_context={},
            articles_shown=[],
            current_phase="CLOSE",
        ),
        turns=turns or [],
        events=[],
        handover=handover,
    )


def _make_turn(
    turn_index: int = 1,
    user_message: str = "hello",
    bot_response: str = "I can help",
    tool_calls: list[dict] | None = None,
    source_ids: list[str] | None = None,
    phase_before: str = "DISCOVER",
    phase_after: str = "RESOLVE",
) -> TurnTrace:
    return TurnTrace(
        turn_index=turn_index,
        user_message=user_message,
        bot_response=bot_response,
        tool_calls=tool_calls or [],
        source_ids=source_ids or [],
        phase_before=phase_before,
        phase_after=phase_after,
        active_use_case="UC-A",
        latency_ms=100,
        projected_context={},
    )


class TestNoForbiddenTools:
    def test_pass_no_forbidden_tools_configured(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_forbidden_tools"], forbidden_tools=[])
        trace = _make_trace(turns=[_make_turn()])
        results = checker.run_checks(case, trace)
        # Wave B1.2 added ``no_human_only_tool_exposure`` as a global L1 check
        # that always runs, so the result list also contains it.
        nft = next(r for r in results if r.check_name == "no_forbidden_tools")
        assert nft.passed is True

    def test_pass_no_forbidden_tools_invoked(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["no_forbidden_tools"],
            forbidden_tools=["delete_account"],
        )
        turn = _make_turn(tool_calls=[{"tool_name": "search_knowledge"}])
        trace = _make_trace(turns=[turn])
        results = checker.run_checks(case, trace)
        nft = next(r for r in results if r.check_name == "no_forbidden_tools")
        assert nft.passed is True

    def test_fail_forbidden_tool_invoked(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["no_forbidden_tools"],
            forbidden_tools=["delete_account"],
        )
        turn = _make_turn(tool_calls=[{"tool_name": "delete_account"}])
        trace = _make_trace(turns=[turn])
        results = checker.run_checks(case, trace)
        nft = next(r for r in results if r.check_name == "no_forbidden_tools")
        assert nft.passed is False
        assert "delete_account" in nft.detail


class TestBudgetEnforcement:
    def test_pass_within_budget(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"], max_turns=10)
        trace = _make_trace(total_bot_turns=5, clarification_count=1, faq_miss_count=1)
        results = checker.run_checks(case, trace)
        be = next(r for r in results if r.check_name == "budget_enforcement")
        assert be.passed is True

    def test_fail_too_many_clarifications(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"])
        trace = _make_trace(clarification_count=3)
        results = checker.run_checks(case, trace)
        be = next(r for r in results if r.check_name == "budget_enforcement")
        assert be.passed is False
        assert "clarification_count" in be.detail

    def test_fail_too_many_faq_misses(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"])
        trace = _make_trace(faq_miss_count=3)
        results = checker.run_checks(case, trace)
        be = next(r for r in results if r.check_name == "budget_enforcement")
        assert be.passed is False
        assert "faq_miss_count" in be.detail

    def test_fail_exceeds_max_turns(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["budget_enforcement"], max_turns=5)
        trace = _make_trace(total_bot_turns=6)
        results = checker.run_checks(case, trace)
        be = next(r for r in results if r.check_name == "budget_enforcement")
        assert be.passed is False
        assert "total_bot_turns" in be.detail


class TestPhaseTransitionValidity:
    def test_pass_valid_transitions(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["phase_transition_validity"])
        turns = [
            _make_turn(turn_index=1, phase_before="INIT", phase_after="DISCOVER"),
            _make_turn(turn_index=2, phase_before="DISCOVER", phase_after="RESOLVE"),
            _make_turn(turn_index=3, phase_before="RESOLVE", phase_after="CONFIRM"),
            _make_turn(turn_index=4, phase_before="CONFIRM", phase_after="CLOSE"),
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        ptv = next(r for r in results if r.check_name == "phase_transition_validity")
        assert ptv.passed is True

    def test_pass_same_phase_stays(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["phase_transition_validity"])
        turns = [
            _make_turn(turn_index=1, phase_before="DISCOVER", phase_after="DISCOVER"),
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        ptv = next(r for r in results if r.check_name == "phase_transition_validity")
        assert ptv.passed is True

    def test_fail_invalid_transition(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["phase_transition_validity"])
        turns = [
            _make_turn(turn_index=1, phase_before="INIT", phase_after="CLOSE"),
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        ptv = next(r for r in results if r.check_name == "phase_transition_validity")
        assert ptv.passed is False
        assert "INIT->CLOSE" in ptv.detail


class TestNoCriticalPolicyViolation:
    def test_pass_clean_responses(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_critical_policy_violation"])
        turns = [_make_turn(bot_response="I can help you with that.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        cpv = next(r for r in results if r.check_name == "no_critical_policy_violation")
        assert cpv.passed is True

    def test_fail_forbidden_phrase(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_critical_policy_violation"])
        turns = [_make_turn(bot_response="As an AI language model, I cannot do that.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        cpv = next(r for r in results if r.check_name == "no_critical_policy_violation")
        assert cpv.passed is False

    def test_fail_answer_must_not_contain(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["no_critical_policy_violation"],
            answer_must_not_contain=["cancel your account"],
        )
        turns = [_make_turn(bot_response="You should cancel your account to fix this.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        cpv = next(r for r in results if r.check_name == "no_critical_policy_violation")
        assert cpv.passed is False
        assert "cancel your account" in cpv.detail


class TestNoPiiLeakage:
    def test_pass_no_pii(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [_make_turn(bot_response="Your ad has been updated.")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        npl = next(r for r in results if r.check_name == "no_pii_leakage")
        assert npl.passed is True

    def test_fail_email_in_response(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        # S-Auto-19 (#5): use a real third-party domain. ``example.com`` is
        # now treated as a benign RFC 2606 reserved documentation domain, so
        # this test (which asserts a genuine PII leak FAILs) must use a real
        # mailbox like gmail.com to stay a meaningful failure case.
        turns = [_make_turn(bot_response="Your email is user@gmail.com")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        npl = next(r for r in results if r.check_name == "no_pii_leakage")
        assert npl.passed is False

    def test_fail_phone_in_response(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [_make_turn(bot_response="Your phone number is 07123456789")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        npl = next(r for r in results if r.check_name == "no_pii_leakage")
        assert npl.passed is False


class TestEscalationCompliance:
    def test_pass_escalation_not_required(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["escalation_compliance"],
            should_escalate=False,
        )
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        ec = next(r for r in results if r.check_name == "escalation_compliance")
        assert ec.passed is True

    def test_pass_escalation_required_and_done(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["escalation_compliance"],
            should_escalate=True,
            risk_level="critical",
        )
        # Wave B1.3: escalation_compliance now also asserts the bot's
        # request_handover ``escalation_reason`` matches the spec's
        # ``expected.escalation_trigger``. The fixture default trigger is
        # ``intake_complete_for_uc_k`` (see ``_make_case_spec``), so the
        # trace must record a matching request_handover tool call.
        handover_turn = _make_turn(
            tool_calls=[
                {
                    "tool_name": "request_handover",
                    "arguments": {"escalation_reason": "intake_complete_for_uc_k"},
                }
            ]
        )
        trace = _make_trace(containment_outcome="escalated", turns=[handover_turn])
        results = checker.run_checks(case, trace)
        ec = next(r for r in results if r.check_name == "escalation_compliance")
        assert ec.passed is True

    def test_fail_escalation_required_but_not_done(self):
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["escalation_compliance"],
            should_escalate=True,
            risk_level="high",
        )
        trace = _make_trace(containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        ec = next(r for r in results if r.check_name == "escalation_compliance")
        assert ec.passed is False


class TestNoStall:
    def test_pass_no_stall_detected(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_stall"])
        trace = _make_trace()
        stall = StallResult(detected=False)
        results = checker.run_checks(case, trace, stall)
        ns = next(r for r in results if r.check_name == "no_stall")
        assert ns.passed is True

    def test_fail_stall_detected(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_stall"])
        trace = _make_trace()
        stall = StallResult(
            detected=True,
            turn_index=3,
            failure_tag="STALL_AFTER_TOOL_INTENT",
        )
        results = checker.run_checks(case, trace, stall)
        ns = next(r for r in results if r.check_name == "no_stall")
        assert ns.passed is False
        assert "STALL_AFTER_TOOL_INTENT" in ns.detail

    def test_pass_no_stall_result_provided(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_stall"])
        trace = _make_trace()
        results = checker.run_checks(case, trace, stall_result=None)
        ns = next(r for r in results if r.check_name == "no_stall")
        assert ns.passed is True


class TestRunChecksFiltering:
    # Global L1 checks always run regardless of per-case configuration.
    # Kept in sync with ``HardChecker.run_checks``'s ``global_checks`` set.
    # Codex 2026-05-04 round 5 §P1 promoted ``required_escalation`` and
    # ``trace_minimum`` to global so they cannot be silently dropped by a
    # per-case scoring config.
    GLOBAL_L1 = {
        "no_human_only_tool_exposure",
        "escalation_compliance",
        # S-Auto-38 (Sprint 092): Part-2 reason-family match is also global
        # (observation-only / advisory; not in _TIER0_PY_FAMILY).
        "escalation_reason_family_match",
        "required_escalation",
        "escalation_reason_consistency",
        "user_requested_escalation",
        # WS-1 item 2: the mirror image of ``required_escalation``. Global so
        # a spec that omits it cannot re-open the over-escalation hole.
        "no_premature_escalation",
        "trace_minimum",
    }

    def test_only_configured_checks_run(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage", "budget_enforcement"])
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        # Globals ALWAYS run regardless of per-case configuration. Filter
        # them out before asserting on the configured-only set.
        configured_names = sorted(
            r.check_name for r in results if r.check_name not in self.GLOBAL_L1
        )
        assert configured_names == ["budget_enforcement", "no_pii_leakage"]

    def test_empty_hard_checks_returns_nothing(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace()
        results = checker.run_checks(case, trace)
        # Only the global L1 checks remain when no per-case checks are
        # configured.
        names = sorted(r.check_name for r in results)
        assert names == sorted(self.GLOBAL_L1)


# ---------------------------------------------------------------------------
# Codex 2026-05-04 round 5 §P1 regressions
# ---------------------------------------------------------------------------


def _make_required_escalation_case(allow_bot_resolution: str = "false") -> CaseSpec:
    """Spec wired for ``required_escalation`` -- escalate is required and the
    bot is not allowed to self-resolve regardless of risk level."""
    return CaseSpec(
        case_id="test-req-esc",
        source_session_id="sess-req",
        source_dataset="test",
        form_context=FormContext(first_name="T", email="a@b.c", topic_subject="t"),
        persona=Persona(
            user_goal_summary="g",
            frustration_level="none",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["hi"],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class="escalate",
            primary_uc="UC-J",
            secondary_ucs=[],
            should_escalate=True,
            allow_bot_resolution=allow_bot_resolution,
            bot_handling_pattern="<test fixture>",
            escalation_trigger="trust_safety_required",
            risk_level="low",  # deliberately low to bypass escalation_compliance
        ),
        scoring=ScoringConfig(hard_checks=[], outcome_checks=[], llm_judge_dimensions=[]),
    )


class TestRequiredEscalation:
    def test_fail_when_resolved_but_handover_required(self):
        """allow_bot_resolution=false + outcome=resolved + no handover → fail.

        ``escalation_compliance`` would let this through because risk is low,
        but ``required_escalation`` is independent of risk per round 5 §P1.
        """
        checker = HardChecker()
        case = _make_required_escalation_case(allow_bot_resolution="false")
        trace = _make_trace(containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        rq = next(r for r in results if r.check_name == "required_escalation")
        assert rq.passed is False
        assert "allow_bot_resolution=false" in rq.detail

    def test_pass_when_escalated(self):
        checker = HardChecker()
        case = _make_required_escalation_case(allow_bot_resolution="false")
        turn = _make_turn(
            tool_calls=[
                {
                    "tool_name": "request_handover",
                    "arguments": {"escalation_reason": "trust_safety_required"},
                }
            ],
        )
        trace = _make_trace(turns=[turn], containment_outcome="escalated")
        results = checker.run_checks(case, trace)
        rq = next(r for r in results if r.check_name == "required_escalation")
        assert rq.passed is True

    def test_pass_when_bot_resolution_allowed(self):
        """allow_bot_resolution != "false" → check is a no-op (handled by
        escalation_compliance)."""
        checker = HardChecker()
        case = _make_required_escalation_case(allow_bot_resolution="true")
        trace = _make_trace(containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        rq = next(r for r in results if r.check_name == "required_escalation")
        assert rq.passed is True


class TestTraceMinimum:
    def test_fail_blank_containment(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(containment_outcome="")
        results = checker.run_checks(case, trace)
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is False
        assert "containment_outcome" in tm.detail

    def test_fail_user_turn_without_bot_response(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        bad_turn = _make_turn(
            user_message="hello?", bot_response="", tool_calls=[]
        )
        trace = _make_trace(turns=[bad_turn], containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is False
        assert "blank" in tm.detail.lower()

    def test_pass_user_turn_with_handover_only(self):
        """An empty bot_response is OK if the runtime escalated on this turn
        (handover stop messages can live in the CLOSE phase / bot_greeting)."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        handover_turn = _make_turn(
            user_message="please escalate",
            bot_response="",
            tool_calls=[
                {
                    "tool_name": "request_handover",
                    "arguments": {"escalation_reason": "user_requested"},
                }
            ],
        )
        trace = _make_trace(turns=[handover_turn], containment_outcome="escalated")
        results = checker.run_checks(case, trace)
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is True

    def test_pass_normal_trace(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(turns=[_make_turn()], containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is True


class TestTraceMinimumTerminalDisposition:
    """S-Auto-19 (#1): trace_minimum Mode-1 is terminal-disposition-aware.

    A blank ``containment_outcome`` only fails when WHY it is blank is a
    genuine partial / errored instrumentation. A simulator that ended a
    fully-measured session before the runtime reached CLOSE (goal_achieved
    etc.) is a valid measured terminal, not a Mode-1 failure.
    """

    # ---- characterization: blank + valid terminal -> Mode-1 does NOT fire

    def test_pass_blank_containment_goal_achieved(self):
        """cs095-style one-shot goal_achieved with a delivered grounded answer
        but blank containment_outcome -> trace_minimum PASSes (so L2/judge run)."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        answer = _make_turn(
            user_message="why was my ad removed?",
            bot_response="Your ad was removed because it breached our posting rules.",
            source_ids=["ka44J000000gKv5QAE"],
        )
        trace = _make_trace(turns=[answer], containment_outcome="")
        results = checker.run_checks(
            case, trace, stop_reason="goal_achieved"
        )
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is True

    def test_pass_blank_containment_goal_impossible(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(turns=[_make_turn()], containment_outcome="")
        results = checker.run_checks(
            case, trace, stop_reason="goal_impossible"
        )
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is True

    def test_pass_blank_containment_max_turns(self):
        # ``max_turns_exceeded`` remains a valid measured terminal (the
        # corpus exercises zero such draws; not speculatively narrowed).
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(turns=[_make_turn()], containment_outcome="")
        results = checker.run_checks(
            case, trace, stop_reason="max_turns_exceeded"
        )
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is True, "max_turns_exceeded should not Mode-1-fail"

    # ---- S-Auto-20 (#2): loop_detected is a GENUINE FAILURE, not a valid terminal

    def test_fail_blank_containment_loop_detected(self):
        """S-Auto-20 (#2): a ``loop_detected`` terminal (the bot repeated an
        identical reply) with blank containment is a genuine bot failure and
        must now FAIL trace_minimum -- it can no longer vacuous-pass."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(turns=[_make_turn()], containment_outcome="")
        results = checker.run_checks(case, trace, stop_reason="loop_detected")
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is False, "loop_detected blank-containment must FAIL"
        assert "blank" in tm.detail.lower()
        assert "loop_detected" in tm.detail.lower()

    def test_loop_detected_not_in_valid_terminal_set(self):
        """S-Auto-20 (#2): loop_detected removed from the valid-terminal set;
        the named non-resolved terminals that remain are explicit."""
        assert "loop_detected" not in HardChecker._VALID_TERMINAL_STOP_REASONS
        assert "goal_achieved" in HardChecker._VALID_TERMINAL_STOP_REASONS
        assert "goal_impossible" in HardChecker._VALID_TERMINAL_STOP_REASONS
        assert "max_turns_exceeded" in HardChecker._VALID_TERMINAL_STOP_REASONS

    # ---- anti-误杀 counter-tests: a genuine same-shape failure still FAILs

    def test_fail_blank_containment_error_stop_reason(self):
        """COUNTER-TEST: blank + stop_reason=error is genuine partial
        instrumentation and still FAILs."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(turns=[_make_turn()], containment_outcome="")
        results = checker.run_checks(case, trace, stop_reason="error")
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is False
        assert "blank" in tm.detail.lower()

    def test_fail_blank_containment_contract_violation(self):
        """COUNTER-TEST: blank + stop_reason=contract_violation still FAILs."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(turns=[_make_turn()], containment_outcome="")
        results = checker.run_checks(
            case, trace, stop_reason="contract_violation"
        )
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is False

    def test_fail_blank_containment_no_stop_reason_strict_legacy(self):
        """COUNTER-TEST: with no stop_reason threaded, the strict legacy
        behaviour is preserved -- any blank outcome fails."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        trace = _make_trace(turns=[_make_turn()], containment_outcome="")
        results = checker.run_checks(case, trace)  # no stop_reason
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is False

    def test_fail_mode2_blank_bot_reply_even_on_goal_achieved(self):
        """COUNTER-TEST: Mode-2 (non-empty user turn, blank bot reply, no
        handover) is unchanged -- it still FAILs even with a valid
        goal_achieved terminal disposition."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=[])
        bad_turn = _make_turn(
            user_message="hello?", bot_response="", tool_calls=[]
        )
        trace = _make_trace(turns=[bad_turn], containment_outcome="resolved")
        results = checker.run_checks(
            case, trace, stop_reason="goal_achieved"
        )
        tm = next(r for r in results if r.check_name == "trace_minimum")
        assert tm.passed is False


class TestSourceCitationSessionAccumulated:
    """S-Auto-19 (#2): source_citation_present is session-accumulated.

    A substantive answer turn is grounded if its own source_ids is non-empty
    OR any earlier turn in the session carried source_ids.
    """

    def _faq_case(self):
        return _make_case_spec(
            hard_checks=["source_citation_present"],
            grounding_mode="faq_source_backed",
        )

    # ---- characterization: alice-style retrieve-early answer-later PASSes

    def test_pass_retrieve_early_answer_later(self):
        checker = HardChecker()
        case = self._faq_case()
        retrieval_turn = _make_turn(
            turn_index=1,
            user_message="why was my ad removed?",
            bot_response="Let me look into that for you.",
            tool_calls=[{"tool_name": "search_knowledge"}],
            source_ids=["ka44J000000gKv5QAE"],
        )
        # The answer turn retrieves nothing itself; its own source_ids is
        # empty (the prompt answers from accumulated hits on a later turn).
        answer_turn = _make_turn(
            turn_index=2,
            user_message="ok",
            bot_response=(
                "Your ad was removed because it did not follow our posting "
                "rules. You can review the rules and repost a compliant ad."
            ),
            tool_calls=[],
            source_ids=[],
        )
        trace = _make_trace(
            turns=[retrieval_turn, answer_turn], containment_outcome="resolved"
        )
        results = checker.run_checks(case, trace)
        sc = next(r for r in results if r.check_name == "source_citation_present")
        assert sc.passed is True

    def test_pass_per_turn_sources_still_pass(self):
        """An answer turn that carries its own source_ids still PASSes
        (no regression of the per-turn-grounded path)."""
        checker = HardChecker()
        case = self._faq_case()
        answer_turn = _make_turn(
            bot_response=(
                "Your ad was removed because it did not follow our posting "
                "rules. Please review and repost a compliant advert."
            ),
            tool_calls=[{"tool_name": "resolve_article"}],
            source_ids=["ka44J000000gKv5QAE"],
        )
        trace = _make_trace(turns=[answer_turn], containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        sc = next(r for r in results if r.check_name == "source_citation_present")
        assert sc.passed is True

    # ---- anti-误杀 counter-test: never-retrieved substantive answer FAILs

    def test_fail_never_searched_substantive_answer(self):
        """COUNTER-TEST: a session that retrieves on NO turn but emits a
        substantive factual answer is genuinely ungrounded and still FAILs."""
        checker = HardChecker()
        case = self._faq_case()
        answer_turn = _make_turn(
            bot_response=(
                "Your ad was definitely removed because of a payment problem "
                "on your account and you owe an outstanding balance of forty "
                "pounds that must be settled before reposting."
            ),
            tool_calls=[],
            source_ids=[],
        )
        trace = _make_trace(turns=[answer_turn], containment_outcome="resolved")
        results = checker.run_checks(case, trace)
        sc = next(r for r in results if r.check_name == "source_citation_present")
        assert sc.passed is False
        assert "without sources" in sc.detail


class TestNoPiiLeakageBenignRelaxation:
    """S-Auto-19 (#5): the email PII gate no longer flags benign first-party
    system addresses or RFC 2606 reserved documentation domains; real
    user / third-party emails and all phone numbers still flag."""

    def test_pass_first_party_noreply_address(self):
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [
            _make_turn(
                bot_response=(
                    "You'll receive a confirmation email from "
                    "noreply@gumtree.com shortly."
                )
            )
        ]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        npl = next(r for r in results if r.check_name == "no_pii_leakage")
        assert npl.passed is True

    def test_fail_real_third_party_email_still_flags(self):
        """SANITY COUNTER-TEST: a real user / third-party email still FAILs."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [_make_turn(bot_response="The seller's email is someone@gmail.com")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        npl = next(r for r in results if r.check_name == "no_pii_leakage")
        assert npl.passed is False

    def test_phone_still_flags(self):
        """Phone numbers are not allowlisted and still FAIL."""
        checker = HardChecker()
        case = _make_case_spec(hard_checks=["no_pii_leakage"])
        turns = [_make_turn(bot_response="Call them on 07123456789")]
        trace = _make_trace(turns=turns)
        results = checker.run_checks(case, trace)
        npl = next(r for r in results if r.check_name == "no_pii_leakage")
        assert npl.passed is False


class TestIntakeNoKnowledgeToolUcGating:
    """WS-3 / A3 (2026-07-25): ``intake_no_knowledge_tool`` is gated on the
    UC being intake-only, not on ``grounding_mode`` alone.

    UC-K (technical fault) was reclassified ``path: INTAKE`` -> ``path:
    PARTIAL`` on the server: ``use-case-registry.yaml`` UC-K is now
    ``partial``, ``tool-policy.yaml`` lists UC-K under ``search_knowledge``
    and ``resolve_article``, and
    ``skills/resolve_technical_diagnose_or_intake.yaml`` carries a critical
    step whose ``mandatory_for`` is ``(UC-K,)`` and which orders
    ``search_knowledge`` before ``resolve_article``. A UC-K session that
    consults the help centre is therefore doing what the runtime requires,
    and must not be scored as an intake violation.

    The 23 already-generated UC-K specs still declare ``grounding_mode:
    fixed_script_only`` (a frozen snapshot of the pre-reclassification
    ``policy_table``), so the exemption cannot key on ``grounding_mode`` —
    it keys on :attr:`HardChecker.INTAKE_UCS`.
    """

    # The real UC-K specs configure the ``fixed_script_adherence`` alias, not
    # the canonical name; both dispatch to the same implementation, so both
    # are exercised.
    INTAKE_CHECK_NAMES = ["intake_no_knowledge_tool", "fixed_script_adherence"]

    def test_intake_ucs_set_excludes_uc_k(self):
        """Pin the set. Re-adding UC-K here silently re-breaks 23 specs."""
        assert HardChecker.INTAKE_UCS == {"UC-G", "UC-H", "UC-I", "UC-J"}

    @pytest.mark.parametrize("check_name", INTAKE_CHECK_NAMES)
    @pytest.mark.parametrize("tool_name", ["search_knowledge", "resolve_article"])
    def test_pass_uc_k_may_call_knowledge_tools(self, check_name, tool_name):
        """THE FIX: UC-K + knowledge tool is no longer an intake violation,
        even though the spec still says ``grounding_mode:
        fixed_script_only``."""
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=[check_name],
            primary_uc="UC-K",
            grounding_mode="fixed_script_only",
        )
        trace = _make_trace(
            turns=[_make_turn(tool_calls=[{"tool_name": tool_name}])],
            active_use_case="UC-K",
        )
        results = checker.run_checks(case, trace)
        inkt = next(r for r in results if r.check_name == "intake_no_knowledge_tool")
        assert inkt.passed is True, inkt.detail
        assert "UC-K" in inkt.detail

    @pytest.mark.parametrize("check_name", INTAKE_CHECK_NAMES)
    @pytest.mark.parametrize("intake_uc", ["UC-G", "UC-H", "UC-I", "UC-J"])
    @pytest.mark.parametrize("tool_name", ["search_knowledge", "resolve_article"])
    def test_fail_true_intake_ucs_still_gated(self, check_name, intake_uc, tool_name):
        """SANITY COUNTER-TEST: the check is not disabled wholesale. The four
        UCs that really are script-only still FAIL on a knowledge call."""
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=[check_name],
            primary_uc=intake_uc,
            grounding_mode="fixed_script_only",
        )
        trace = _make_trace(
            turns=[_make_turn(tool_calls=[{"tool_name": tool_name}])],
            active_use_case=intake_uc,
        )
        results = checker.run_checks(case, trace)
        inkt = next(r for r in results if r.check_name == "intake_no_knowledge_tool")
        assert inkt.passed is False, inkt.detail
        assert tool_name in inkt.detail

    def test_pass_intake_uc_without_knowledge_call(self):
        """No false positive: a clean UC-J intake run still passes."""
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["intake_no_knowledge_tool"],
            primary_uc="UC-J",
            grounding_mode="fixed_script_only",
        )
        trace = _make_trace(
            turns=[_make_turn(tool_calls=[{"tool_name": "create_case_controlled"}])],
            active_use_case="UC-J",
        )
        results = checker.run_checks(case, trace)
        inkt = next(r for r in results if r.check_name == "intake_no_knowledge_tool")
        assert inkt.passed is True, inkt.detail

    def test_grounding_mode_gate_still_short_circuits_first(self):
        """An intake UC on a non-script grounding_mode is still exempt via
        the original ``grounding_mode`` short-circuit, unchanged."""
        checker = HardChecker()
        case = _make_case_spec(
            hard_checks=["intake_no_knowledge_tool"],
            primary_uc="UC-G",
            grounding_mode="faq_source_backed",
        )
        trace = _make_trace(
            turns=[_make_turn(tool_calls=[{"tool_name": "search_knowledge"}])],
            active_use_case="UC-G",
        )
        results = checker.run_checks(case, trace)
        inkt = next(r for r in results if r.check_name == "intake_no_knowledge_tool")
        assert inkt.passed is True
        assert "grounding_mode not fixed_script_only" in inkt.detail
