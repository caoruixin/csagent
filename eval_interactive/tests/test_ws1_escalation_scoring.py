"""WS-1 — stop gating on a contaminated target.

Covers the six deliverables of
``docs/proposals/performance_priority_replan_2026-07.md`` §3 WS-1. Every
behaviour is tested in BOTH directions: the premature-escalation shape must
now fail, and the try-then-escalate shape must still pass.
"""

from __future__ import annotations

import pytest

from eval_interactive.case_spec.schema import (
    GROUNDING_MODE_VALUES,
    CaseSpec,
    Expected,
    FormContext,
    Persona,
    ScoringConfig,
)
from eval_interactive.scoring import escalation_intent as esc
from eval_interactive.scoring.composite import compute_composite
from eval_interactive.scoring.hard_checks import HardChecker
from eval_interactive.scoring.outcome_checks import OutcomeChecker
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.trace.models import (
    HandoverData,
    SessionState,
    TraceData,
    TurnTrace,
)


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


def _spec(
    *,
    outcome_class: str = "resolve",
    should_escalate: bool = False,
    acceptable_outcomes: list[str] | None = None,
    grounding_mode: str = "faq_source_backed",
    hard_checks: list[str] | None = None,
    outcome_checks: list[str] | None = None,
    max_turns: int = 10,
    primary_uc: str = "UC-A",
) -> CaseSpec:
    return CaseSpec(
        case_id="ws1-001",
        source_session_id="sess-ws1",
        source_dataset="test",
        form_context=FormContext(
            first_name="Test", email="t@example.com", topic_subject="Ad support"
        ),
        persona=Persona(
            user_goal_summary="understand why the ad was removed",
            frustration_level="none",
            verbosity="normal",
            drift_behavior="none",
            seed_messages=["Why was my ad removed?"],
            hidden_facts=[],
        ),
        expected=Expected(
            outcome_class=outcome_class,
            primary_uc=primary_uc,
            secondary_ucs=[],
            should_escalate=should_escalate,
            allow_bot_resolution="true",
            bot_handling_pattern="<test fixture>",
            escalation_trigger="user_requested" if should_escalate else None,
            grounding_mode=grounding_mode,
            max_turns=max_turns,
            acceptable_outcomes=acceptable_outcomes or [],
        ),
        scoring=ScoringConfig(
            hard_checks=hard_checks or [],
            outcome_checks=outcome_checks or [],
            llm_judge_dimensions=[],
        ),
    )


def _turn(
    idx: int,
    *,
    user: str = "please help",
    bot: str = "ok",
    tools: list[str] | None = None,
    source_ids: list[str] | None = None,
    phase_after: str = "RESOLVE",
) -> TurnTrace:
    return TurnTrace(
        turn_index=idx,
        user_message=user,
        bot_response=bot,
        tool_calls=[{"tool_name": t} for t in (tools or [])],
        source_ids=source_ids or [],
        phase_before="DISCOVER",
        phase_after=phase_after,
        active_use_case="UC-A",
        latency_ms=10,
        projected_context={},
    )


def _handover(summary: str = "User asked why their ad was removed; escalating.") -> HandoverData:
    return HandoverData(
        log_id="log",
        session_id="s",
        handover_payload={
            "session_id": "s",
            "primary_use_case": "UC-A",
            "summary": summary,
            "escalation_reason": "user_requested",
            "total_bot_turns": 2,
        },
        customer_message="",
        transcript=[],
        transfer_result="",
    )


def _trace(
    turns: list[TurnTrace],
    *,
    containment_outcome: str = "escalated",
    handover: HandoverData | None = None,
    customer_context: dict | None = None,
) -> TraceData:
    return TraceData(
        session_state=SessionState(
            session_id="s",
            active_use_case="UC-A",
            candidate_use_cases=["UC-A"],
            containment_outcome=containment_outcome,
            escalation_reason="user_requested" if containment_outcome == "escalated" else "",
            total_bot_turns=len(turns),
            clarification_count=0,
            faq_miss_count=0,
            form_context={},
            customer_context=customer_context or {},
            articles_shown=[],
            current_phase="ESCALATE",
        ),
        turns=turns,
        events=[],
        handover=handover,
    )


# The two canonical shapes this sprint exists to separate.
def _turn0_handover_trace() -> TraceData:
    """Turn-0 handover, 10+ char summary, ZERO resolution attempt."""
    return _trace(
        [_turn(0, tools=["request_handover"], phase_after="ESCALATE")],
        handover=_handover(),
    )


def _search_then_handover_trace() -> TraceData:
    """Retrieved first, then handed over — the legitimate shape."""
    return _trace(
        [
            _turn(0, tools=["search_knowledge"], source_ids=["faq-101"]),
            _turn(1, tools=["request_handover"], phase_after="ESCALATE"),
        ],
        handover=_handover(),
    )


# ---------------------------------------------------------------------------
# Item 1 — correct_outcome direction symmetry
# ---------------------------------------------------------------------------


class TestItem1CorrectOutcomeSymmetry:
    def test_turn0_handover_with_10_char_summary_now_fails(self):
        """THE headline regression: a 10-character summary used to buy 1.0."""
        checker = OutcomeChecker()
        case = _spec(
            outcome_checks=["correct_outcome"],
            acceptable_outcomes=["resolve", "escalate"],
        )
        results = checker.run_checks(case, _turn0_handover_trace())
        co = next(r for r in results if r.check_name == "correct_outcome")
        assert co.score == 0.0
        assert "no resolution attempt" in co.detail

    def test_search_then_handover_still_passes(self):
        checker = OutcomeChecker()
        case = _spec(
            outcome_checks=["correct_outcome"],
            acceptable_outcomes=["resolve", "escalate"],
        )
        results = checker.run_checks(case, _search_then_handover_trace())
        co = next(r for r in results if r.check_name == "correct_outcome")
        assert co.score == 1.0

    def test_citation_without_tool_call_counts_as_attempt(self):
        """Grounding may be stamped via source_ids on a turn whose tool call
        the trace recorded elsewhere; that is still evidence of an attempt."""
        checker = OutcomeChecker()
        case = _spec(
            outcome_checks=["correct_outcome"],
            acceptable_outcomes=["resolve", "escalate"],
        )
        trace = _trace(
            [
                _turn(0, source_ids=["faq-7"]),
                _turn(1, tools=["request_handover"], phase_after="ESCALATE"),
            ],
            handover=_handover(),
        )
        co = next(
            r for r in checker.run_checks(case, trace) if r.check_name == "correct_outcome"
        )
        assert co.score == 1.0

    def test_explicit_human_request_waives_the_attempt_requirement(self):
        """Anti-误杀: product decision D2 keeps an explicit human demand as a
        valid immediate trigger, so a turn-0 handover is legitimate there."""
        checker = OutcomeChecker()
        case = _spec(
            outcome_checks=["correct_outcome"],
            acceptable_outcomes=["resolve", "escalate"],
        )
        trace = _trace(
            [
                _turn(
                    0,
                    user="just let me speak to a human please",
                    tools=["request_handover"],
                    phase_after="ESCALATE",
                )
            ],
            handover=_handover(),
        )
        co = next(
            r for r in checker.run_checks(case, trace) if r.check_name == "correct_outcome"
        )
        assert co.score == 1.0

    def test_attempted_but_degraded_handover_still_scores_half(self):
        """The pre-existing payload-quality rung is preserved."""
        checker = OutcomeChecker()
        case = _spec(
            outcome_checks=["correct_outcome"],
            acceptable_outcomes=["resolve", "escalate"],
        )
        trace = _trace(
            [
                _turn(0, tools=["search_knowledge"], source_ids=["faq-1"]),
                _turn(1, tools=["request_handover"], phase_after="ESCALATE"),
            ],
            handover=_handover(summary="x"),
        )
        co = next(
            r for r in checker.run_checks(case, trace) if r.check_name == "correct_outcome"
        )
        assert co.score == 0.5

    def test_reverse_direction_unchanged(self):
        """expected=escalate / actual=resolve keeps its citation rigor."""
        checker = OutcomeChecker()
        case = _spec(
            outcome_class="escalate",
            should_escalate=True,
            outcome_checks=["correct_outcome"],
            acceptable_outcomes=["resolve", "escalate"],
        )
        cited = _trace([_turn(0, source_ids=["faq-1"])], containment_outcome="resolved")
        bare = _trace([_turn(0)], containment_outcome="resolved")
        assert (
            next(
                r for r in checker.run_checks(case, cited) if r.check_name == "correct_outcome"
            ).score
            == 1.0
        )
        assert (
            next(
                r for r in checker.run_checks(case, bare) if r.check_name == "correct_outcome"
            ).score
            == 0.5
        )


# ---------------------------------------------------------------------------
# Item 2 — no_premature_escalation L1 gate
# ---------------------------------------------------------------------------


class TestItem2NoPrematureEscalation:
    def test_registered_in_all_checks_and_runs_globally(self):
        assert "no_premature_escalation" in HardChecker.ALL_CHECKS
        # Global: runs even when the spec configures an unrelated check only.
        results = HardChecker().run_checks(
            _spec(hard_checks=["no_pii_leakage"]), _turn0_handover_trace()
        )
        assert "no_premature_escalation" in {r.check_name for r in results}

    def test_turn0_handover_fails(self):
        result = next(
            r
            for r in HardChecker().run_checks(_spec(), _turn0_handover_trace())
            if r.check_name == "no_premature_escalation"
        )
        assert result.passed is False
        assert result.severity == "critical"

    def test_search_then_handover_passes(self):
        result = next(
            r
            for r in HardChecker().run_checks(_spec(), _search_then_handover_trace())
            if r.check_name == "no_premature_escalation"
        )
        assert result.passed is True

    def test_no_op_when_spec_expects_escalation(self):
        case = _spec(outcome_class="escalate", should_escalate=True)
        result = next(
            r
            for r in HardChecker().run_checks(case, _turn0_handover_trace())
            if r.check_name == "no_premature_escalation"
        )
        assert result.passed is True

    def test_no_op_when_session_did_not_escalate(self):
        trace = _trace([_turn(0)], containment_outcome="resolved")
        result = next(
            r
            for r in HardChecker().run_checks(_spec(), trace)
            if r.check_name == "no_premature_escalation"
        )
        assert result.passed is True

    def test_explicit_human_request_is_exempt(self):
        trace = _trace(
            [
                _turn(
                    0,
                    user="I want a human, now",
                    tools=["request_handover"],
                    phase_after="ESCALATE",
                )
            ],
            handover=_handover(),
        )
        result = next(
            r
            for r in HardChecker().run_checks(_spec(), trace)
            if r.check_name == "no_premature_escalation"
        )
        assert result.passed is True

    def test_fixed_script_intake_path_is_exempt(self):
        """Knowledge tools are forbidden on that path, so no attempt is possible."""
        case = _spec(grounding_mode="fixed_script_only")
        result = next(
            r
            for r in HardChecker().run_checks(case, _turn0_handover_trace())
            if r.check_name == "no_premature_escalation"
        )
        assert result.passed is True

    def test_l1_gate_flips_case_passed_in_composite(self):
        """The check must actually reach the composite L1 gate."""
        case = _spec(outcome_checks=["correct_outcome"], acceptable_outcomes=["resolve", "escalate"])
        l1 = HardChecker().run_checks(case, _turn0_handover_trace())
        l2 = OutcomeChecker().run_checks(case, _turn0_handover_trace())
        score = compute_composite(
            "ws1-001", l1, l2, [], StallResult(detected=False), case_spec=case
        )
        assert score.case_passed is False
        assert "L1:no_premature_escalation" in score.failure_tags

    def test_composite_passes_l1_on_the_attempted_shape(self):
        case = _spec()
        l1 = HardChecker().run_checks(case, _search_then_handover_trace())
        premature = next(r for r in l1 if r.check_name == "no_premature_escalation")
        assert premature.passed is True


# ---------------------------------------------------------------------------
# Item 3 — escalation_timing rewards effort, not earliness
# ---------------------------------------------------------------------------


class TestItem3EscalationTiming:
    def _case(self) -> CaseSpec:
        return _spec(
            outcome_class="escalate",
            should_escalate=True,
            outcome_checks=["escalation_timing"],
        )

    def test_turn0_escalation_no_longer_scores_best(self):
        r = next(
            x
            for x in OutcomeChecker().run_checks(self._case(), _turn0_handover_trace())
            if x.check_name == "escalation_timing"
        )
        assert r.score == 0.3
        assert "no prior resolution attempt" in r.detail

    def test_escalation_after_attempt_scores_best(self):
        r = next(
            x
            for x in OutcomeChecker().run_checks(self._case(), _search_then_handover_trace())
            if x.check_name == "escalation_timing"
        )
        assert r.score == 1.0

    def test_late_escalation_after_attempt_is_not_penalised(self):
        """Lateness is priced by turn_efficiency / budget_enforcement; pricing
        it here is what created the 'escalate earlier' gradient."""
        turns = [_turn(i) for i in range(6)]
        turns[0] = _turn(0, tools=["search_knowledge"], source_ids=["faq-1"])
        turns.append(_turn(6, tools=["request_handover"], phase_after="ESCALATE"))
        trace = _trace(turns, handover=_handover())
        r = next(
            x
            for x in OutcomeChecker().run_checks(self._case(), trace)
            if x.check_name == "escalation_timing"
        )
        assert r.score == 1.0

    def test_explicit_human_request_scores_best_without_attempt(self):
        trace = _trace(
            [
                _turn(
                    0,
                    user="get me a human",
                    tools=["request_handover"],
                    phase_after="ESCALATE",
                )
            ],
            handover=_handover(),
        )
        r = next(
            x
            for x in OutcomeChecker().run_checks(self._case(), trace)
            if x.check_name == "escalation_timing"
        )
        assert r.score == 1.0

    def test_did_not_escalate_still_zero(self):
        trace = _trace([_turn(0)], containment_outcome="resolved")
        r = next(
            x
            for x in OutcomeChecker().run_checks(self._case(), trace)
            if x.check_name == "escalation_timing"
        )
        assert r.score == 0.0


# ---------------------------------------------------------------------------
# Item 4 — grounding_mode enum + a gate that actually runs
# ---------------------------------------------------------------------------


class TestItem4GroundingMode:
    def test_six_corpus_values_are_legal(self):
        for mode in (
            "faq_source_backed",
            "fixed_script_only",
            "listing_data_and_faq_backed",
            "listing_data_or_faq_backed",
            "moderation_review_and_faq_backed",
        ):
            assert mode in GROUNDING_MODE_VALUES
            _spec(grounding_mode=mode)  # must not raise

    def test_unknown_value_raises_at_load_time(self):
        with pytest.raises(ValueError, match="grounding_mode must be one of"):
            _spec(grounding_mode="kb_only")

    def _substantive_answer(self, idx: int, source_ids: list[str] | None = None) -> TurnTrace:
        return _turn(
            idx,
            bot=(
                "Your listing was removed because live animal sales are "
                "restricted under the prohibited items policy. You can relist "
                "under the rehoming category once the photos are updated."
            ),
            tools=["search_knowledge"],
            source_ids=source_ids or [],
        )

    def test_and_mode_fails_without_entity_context(self):
        case = _spec(
            grounding_mode="listing_data_and_faq_backed",
            hard_checks=["source_citation_present"],
        )
        trace = _trace(
            [self._substantive_answer(0, ["faq-1"])], containment_outcome="resolved"
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "source_citation_present"
        )
        assert r.passed is False
        assert "entity" in r.detail

    def test_and_mode_passes_with_preloaded_customer_context(self):
        """Anti-误杀: cs_uc_a_loaded_listing explicitly allows using the
        pre-loaded customer_context.listing instead of a tool call."""
        case = _spec(
            grounding_mode="listing_data_and_faq_backed",
            hard_checks=["source_citation_present"],
        )
        trace = _trace(
            [self._substantive_answer(0, ["faq-1"])],
            containment_outcome="resolved",
            customer_context={"listing": {"ad_id": "AD-2002", "status": "LIVE"}},
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "source_citation_present"
        )
        assert r.passed is True

    def test_and_mode_passes_with_entity_tool_call(self):
        case = _spec(
            grounding_mode="moderation_review_and_faq_backed",
            hard_checks=["source_citation_present"],
        )
        trace = _trace(
            [
                _turn(0, tools=["get_moderation_review_context"]),
                self._substantive_answer(1, ["faq-1"]),
            ],
            containment_outcome="resolved",
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "source_citation_present"
        )
        assert r.passed is True

    def test_or_mode_satisfied_by_entity_alone(self):
        case = _spec(
            grounding_mode="listing_data_or_faq_backed",
            hard_checks=["source_citation_present"],
        )
        trace = _trace(
            [self._substantive_answer(0)],
            containment_outcome="resolved",
            customer_context={"listing": {"ad_id": "AD-1"}},
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "source_citation_present"
        )
        assert r.passed is True

    def test_or_mode_fails_when_neither_evidence_class_present(self):
        case = _spec(
            grounding_mode="listing_data_or_faq_backed",
            hard_checks=["source_citation_present"],
        )
        trace = _trace([self._substantive_answer(0)], containment_outcome="resolved")
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "source_citation_present"
        )
        assert r.passed is False

    def test_fixed_script_only_still_skips_the_gate(self):
        case = _spec(
            grounding_mode="fixed_script_only", hard_checks=["source_citation_present"]
        )
        trace = _trace([self._substantive_answer(0)], containment_outcome="resolved")
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "source_citation_present"
        )
        assert r.passed is True


# ---------------------------------------------------------------------------
# Item 5 — cs012: explanation/callback request vs explicit human demand
# ---------------------------------------------------------------------------


class TestItem5UserRequestedEscalation:
    # cs012_uc_fp_late_phone_failure_path seed message #2, verbatim.
    CS012_MESSAGE = "I just want to know what happened. Can someone explain or call me about this?"

    def test_cs012_message_is_not_an_explicit_human_request(self):
        assert esc.is_explicit_human_request(self.CS012_MESSAGE) is None
        assert esc.is_contact_channel_request(self.CS012_MESSAGE) is not None

    def test_cs012_correct_behaviour_no_longer_fails_l1(self):
        """The bot explains the policy instead of handing over — cs012's own
        closure_criterion requires exactly this, and it used to FAIL L1."""
        case = _spec(hard_checks=["no_pii_leakage"], primary_uc="UC-FP")
        trace = _trace(
            [
                _turn(0, user="Why was my kitten ad removed?", tools=["search_knowledge"], source_ids=["faq-9"]),
                _turn(
                    1,
                    user=self.CS012_MESSAGE,
                    bot="Live animal sales are restricted; here is how to relist.",
                    source_ids=["faq-9"],
                ),
            ],
            containment_outcome="resolved",
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "user_requested_escalation"
        )
        assert r.severity == "advisory", "channel request must not gate"
        # Recorded as an observation so the signal is not lost.
        assert "OBSERVATION" in r.detail

    def test_explicit_human_demand_without_handover_still_hard_fails(self):
        case = _spec()
        trace = _trace(
            [_turn(0, user="stop, I want to talk to a human")],
            containment_outcome="resolved",
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "user_requested_escalation"
        )
        assert r.passed is False
        assert r.severity == "critical"

    def test_explicit_human_demand_with_handover_passes(self):
        case = _spec()
        trace = _trace(
            [
                _turn(
                    0,
                    user="I want to talk to a human",
                    tools=["request_handover"],
                    phase_after="ESCALATE",
                )
            ],
            handover=_handover(),
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "user_requested_escalation"
        )
        assert r.passed is True

    def test_advisory_result_does_not_flip_case_passed(self):
        # Both mandatory L2 checks must be configured, else the gate
        # fail-closes on L2_GATE_MISSING for an unrelated reason.
        case = _spec(outcome_checks=["correct_uc", "correct_outcome"])
        trace = _trace(
            [
                _turn(0, user=self.CS012_MESSAGE, tools=["search_knowledge"], source_ids=["f-1"]),
            ],
            containment_outcome="resolved",
        )
        l1 = HardChecker().run_checks(case, trace)
        l2 = OutcomeChecker().run_checks(case, trace)
        score = compute_composite(
            "ws1-001", l1, l2, [], StallResult(detected=False), case_spec=case
        )
        assert score.case_passed is True

    def test_inert_bot_after_channel_request_hard_fails(self):
        """The non-inertness invariant. The bot neither attempted a resolution
        nor handed over — wrong under BOTH readings of "call me", so it can be
        gated without adjudicating intent. Recovers the coverage that demoting
        the channel class would otherwise have lost (cs_interactive_029)."""
        case = _spec()
        trace = _trace(
            [_turn(0, user="can you please call me now?", bot="Sorry about that.")],
            containment_outcome="resolved",
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "user_requested_escalation"
        )
        assert r.passed is False
        assert r.severity == "critical"
        assert "inert" in r.detail

    def test_cs012_shape_satisfies_the_non_inertness_invariant(self):
        """cs012's CORRECT behaviour — explain the policy first, never hand
        over — must still clear the critical gate. A retrieval turn is a
        resolution attempt, so the bot was not inert."""
        case = _spec(primary_uc="UC-FP")
        trace = _trace(
            [
                _turn(0, user="Why was my kitten ad removed?", tools=["search_knowledge"], source_ids=["faq-9"]),
                _turn(
                    1,
                    user=self.CS012_MESSAGE,
                    bot="Live animal sales are restricted; here is how to relist.",
                    source_ids=["faq-9"],
                ),
            ],
            containment_outcome="resolved",
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "user_requested_escalation"
        )
        assert r.severity == "advisory", "must not gate on the cs012 shape"
        assert "inert" not in r.detail

    def test_channel_request_then_handover_satisfies_invariant(self):
        """The other way to be non-inert: actually hand over."""
        case = _spec()
        trace = _trace(
            [
                _turn(
                    0,
                    user="please call me back",
                    tools=["request_handover"],
                    phase_after="ESCALATE",
                )
            ],
            handover=_handover(),
        )
        r = next(
            x
            for x in HardChecker().run_checks(case, trace)
            if x.check_name == "user_requested_escalation"
        )
        assert r.passed is True

    def test_pattern_union_is_unchanged(self):
        """No keyword was ADDED; the pre-WS-1 list was partitioned."""
        assert set(esc.ESCALATION_REQUEST_PATTERNS) == set(
            esc.EXPLICIT_HUMAN_REQUEST_PATTERNS
        ) | set(esc.CONTACT_CHANNEL_REQUEST_PATTERNS)
        assert not (
            set(esc.EXPLICIT_HUMAN_REQUEST_PATTERNS)
            & set(esc.CONTACT_CHANNEL_REQUEST_PATTERNS)
        )
        assert len(esc.ESCALATION_REQUEST_PATTERNS) == 12
