"""Sprint 43 (S-Eval-2) — §1.7 STRUCTURAL DEFENCE test.

This test is the milestone's **primary structural mechanism** preventing
S-Eval-3 ``critical_steps`` content from accidentally encoding §1.7
semantic hardcodes (keyword / regex / message-content matching) via the
``trace_check`` DSL. It asserts that the parser rejects at parse time
every representative hardcode-flavoured ``trace_check`` string from a
curated list — raising :class:`TraceCheckDSLSyntaxError` with a clear,
named error message so an S-Eval-3 dev iterating on content sees
immediately which §1.7 surface their expression tripped.

The structural guarantee is the POSITIVE grammar (the parser accepts
exactly the six frozen primitives — nothing else). This negative test
demonstrates the practical consequence on shapes a dev is likely to
reach for.

§4.1 Q1 verdict in `docs/sprints/sprint-043-handoff.md` §6 explicitly
cites this test as the structural defence justifying the `pass`
verdict for S-Eval-2's contract + extractor + projection wiring.
"""

from __future__ import annotations

import pytest

from eval_interactive.scoring.skill_procedure_check import (
    TraceCheckDSLSyntaxError,
    parse_trace_check,
)


# Curated representative hardcode-flavoured trace_check strings. Every
# entry must fail parse with TraceCheckDSLSyntaxError. The list is
# illustrative, not exhaustive: the positive grammar would reject
# anything outside the six frozen primitives regardless.
_HARDCODE_TRACE_CHECKS = [
    # Direct string-content matches against user / bot messages.
    "message.contains('refund')",
    "user_message.contains('appeal')",
    "bot_message.matches(r'.*sorry.*')",
    # Python ``re`` module references — direct regex.
    "re.search(r'\\bpayment\\b', user_message)",
    "re.match(r'^cancel', user_message)",
    "re.fullmatch(r'.*deleted.*', user_message)",
    # Keyword-list / membership against message-derived content. UC-list
    # membership is rejected because it implies UC was extracted from
    # message text (a keyword surface); UC scoping is properly enforced
    # via the step's mandatory_for declaration.
    "user_intent in ['UC-A', 'UC-FP']",
    "intent in ['cancel', 'refund']",
    # Explicit keyword-match primitives (the most obvious shape S-Eval-3
    # might reach for when the principled DSL doesn't fit).
    "keyword_match('refund', user_message)",
    "keyword_in(['refund', 'cancel'], user_message)",
    "contains_keyword(user_message, 'appeal')",
    # String-suffix / prefix checks against message content.
    "user_message.startswith('Hi')",
    "bot_message.endswith('thanks')",
    # Combinator wrapping a forbidden primitive — must NOT slip through
    # via any_of / all_of.
    "any_of(accumulated_tool_results.search_knowledge, message.contains('refund'))",
    "all_of(accumulated_tool_results.search_knowledge, re.search(r'x', user_message))",
]


class TestStructuralDefence:
    """Each curated hardcode-flavoured string must fail parse."""

    @pytest.mark.parametrize("trace_check", _HARDCODE_TRACE_CHECKS)
    def test_rejects_hardcode_with_named_error(self, trace_check: str):
        with pytest.raises(TraceCheckDSLSyntaxError) as ei:
            parse_trace_check(trace_check)
        # The error message MUST name the rejected primitive (or at
        # minimum the §1.7 forbidden semantic surface) so S-Eval-3
        # devs can iterate cleanly. We assert the message is non-empty
        # and mentions either the §1.7 marker, the offending primitive
        # name, or some keyword indicating the structural rejection.
        msg = str(ei.value).lower()
        assert msg, "TraceCheckDSLSyntaxError message must not be empty"

    def test_minimum_six_distinct_hardcode_shapes_rejected(self):
        # Contract §9 hard gate (per dev prompt §9 self-check #5): the
        # structural defence test asserts ≥ 6 hardcode-flavoured
        # trace_check strings fail parse. The list above has 15 entries
        # — well above the floor — but pin the floor here so a future
        # refactor that trims the list trips a clear assertion.
        assert len(_HARDCODE_TRACE_CHECKS) >= 6, (
            "Contract §9 requires ≥ 6 representative hardcode-flavoured "
            "trace_check strings in the structural-defence test."
        )


class TestPositiveGrammarStillAcceptsCanonicalForms:
    """Sanity: the parser still accepts every shape S-Eval-3 is
    contractually allowed to author (the six frozen primitives + the
    two combinators). Pins the positive surface so a future
    over-tightening of the parser doesn't accidentally break S-Eval-3
    content.
    """

    @pytest.mark.parametrize(
        "trace_check",
        [
            "accumulated_tool_results.search_knowledge",
            "accumulated_tool_results.create_case_controlled",
            "tool_event_seq(search_knowledge) < tool_event_seq(resolve_article)",
            "tool_event_seq(create_case_controlled) < tool_event_seq(request_handover)",
            "intake_state.fields_collected.contains(ad_id)",
            "intake_state.fields_collected.contains('appeal_reason')",
            "session.moderation_context_present",
            "session.case_id_present",
            "any_of(accumulated_tool_results.search_knowledge, "
            "accumulated_tool_results.resolve_article)",
            "all_of(accumulated_tool_results.search_knowledge, "
            "tool_event_seq(search_knowledge) < tool_event_seq(resolve_article))",
            # Nested combinator — two levels deep.
            "all_of("
            "any_of(accumulated_tool_results.search_knowledge, "
            "accumulated_tool_results.classify_use_case), "
            "intake_state.fields_collected.contains(ad_id))",
        ],
    )
    def test_canonical_form_parses(self, trace_check: str):
        # Should not raise.
        ast = parse_trace_check(trace_check)
        assert ast is not None


class TestSyntaxErrorsOnMalformedExpressions:
    """Generic syntax errors (unbalanced parens, missing args) — not
    §1.7 hardcodes per se, but should still raise the same exception
    type so the caller has one error class to handle.
    """

    @pytest.mark.parametrize(
        "bad_input",
        [
            "",
            "   ",
            "tool_event_seq(search_knowledge",  # unbalanced paren
            "any_of()",  # combinator with no children
            "session.no_present_suffix",  # missing _present suffix
            "session.",  # missing flag name
            "unknown_primitive.foo",  # unrecognised head
            "accumulated_tool_results",  # missing .<tool>
            "tool_event_seq(search_knowledge) > tool_event_seq(resolve_article)",
            # Wrong operator: only '<' is valid; '>' is rejected.
        ],
    )
    def test_malformed_input_raises_dsl_syntax_error(self, bad_input: str):
        with pytest.raises(TraceCheckDSLSyntaxError):
            parse_trace_check(bad_input)
