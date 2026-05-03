"""Hard checks -- L1 deterministic pass/fail checks on trace data.

Every hard check must pass for a case to be considered valid.
"""

from __future__ import annotations

import re
from dataclasses import dataclass

from eval_interactive.case_spec.policy_table import list_human_only_tools
from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.scoring.stall_detector import StallResult
from eval_interactive.trace.models import TraceData


@dataclass
class HardCheckResult:
    """Result of a single hard check."""

    check_name: str
    passed: bool
    detail: str = ""
    severity: str = "critical"


def _has_handover_tool_call(turn) -> bool:
    """Return True if this turn invoked ``request_handover`` as a tool call.

    Replaces the legacy ``action_selected in {escalate, handover, transfer}``
    check after the abstract action layer was dropped in favor of OpenAI-style
    single-layer tool-use.
    """
    return any(
        (tc.get("tool_name", "") if isinstance(tc, dict) else "").lower()
        == "request_handover"
        for tc in (turn.tool_calls or [])
    )


def _is_answer_turn(turn) -> bool:
    """A turn is an answer if the bot produced user-facing text without escalating.

    Replaces the legacy ``action_selected in {answer, answer_grounded}`` check
    after the abstract action layer was dropped in favor of OpenAI-style
    single-layer tool-use.
    """
    has_text = bool((turn.bot_response or "").strip())
    return has_text and not _has_handover_tool_call(turn)


class HardChecker:
    """L1 Hard Checks -- deterministic, zero tolerance."""

    ALL_CHECKS = [
        "no_forbidden_tools",
        "no_human_only_tool_exposure",
        "budget_enforcement",
        "phase_transition_validity",
        "no_critical_policy_violation",
        "no_pii_leakage",
        "escalation_compliance",
        "user_requested_escalation",
        "source_citation_present",
        "intake_no_knowledge_tool",
        "no_stall",
    ]

    # Patterns for self-promises of human-only capabilities.
    # Tuned to match first-person commitments while leaving handover phrasing
    # like "I'll transfer you to a human agent who can email you" untouched
    # (the verb after "I" is "transfer", not "send/email/ban/...").
    HUMAN_ONLY_PROMISE_PATTERNS = [
        r"\bI(?:'ll| will)\s+(?:send|email)\s+(?:you\s+)?an?\s+email",
        r"\bI(?:'ve| have)\s+(?:sent|emailed)\s+(?:you\s+)?an?\s+email",
        r"\b(?:I|we)\s+(?:will\s+)?(?:ban|suspend|delete|remove|restore)\s+(?:the|your|this)\s+(?:account|ad|listing|user)\b",
        r"\bI(?:'ve| have)\s+(?:banned|suspended|deleted|removed|restored)\b",
    ]

    FORBIDDEN_PHRASES = [
        r"Hello, my name is",
        r"You are speaking to a human",
        r"I am a real human",
        r"I've fixed that",
        r"I've removed the ad",
        r"I've restricted their account",
        r"Your restriction is lifted",
        r"The ads have now been removed",
        r"I'll process your refund",
        r"Your refund has been issued",
        r"I can guarantee",
        r"I understand how you feel",
        r"As an AI language model",
        r"As an AI",
        r"I have deleted your data",
        r"Your account has been erased",
    ]

    PII_PATTERNS = [
        r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b",
        r"\b0\d{10}\b",
        r"\b\+44\d{10}\b",
    ]

    INTAKE_UCS = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}

    ALLOWED_TRANSITIONS = {
        ("INIT", "DISCOVER"),
        ("DISCOVER", "RESOLVE"),
        ("DISCOVER", "ESCALATE"),
        ("RESOLVE", "CONFIRM"),
        ("RESOLVE", "ESCALATE"),
        ("CONFIRM", "CLOSE"),
        ("CONFIRM", "RESOLVE"),
        ("CONFIRM", "DISCOVER"),
        ("CONFIRM", "ESCALATE"),
        ("ESCALATE", "CLOSE"),
    }

    ESCALATION_REQUEST_PATTERNS = [
        r"\btalk\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
        r"\bspeak\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
        r"\bconnect\s+me\s+to\s+(a\s+)?(human|agent|person|representative|someone)\b",
        r"\btransfer\s+(me\s+)?to\s+(a\s+)?(human|agent|person|representative|someone)\b",
        r"\bi\s+want\s+(a\s+)?(human|real\s+person|agent)\b",
        r"\bget\s+me\s+(a\s+)?(human|agent|person|representative)\b",
    ]

    KNOWLEDGE_TOOLS = {"search_knowledge", "resolve_article"}

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def run_checks(
        self,
        case_spec: CaseSpec,
        trace: TraceData,
        stall_result: StallResult | None = None,
    ) -> list[HardCheckResult]:
        """Run all applicable hard checks.

        Only runs checks listed in ``case_spec.scoring.hard_checks``.
        Returns a list of HardCheckResult -- one per check.
        """
        configured = set(case_spec.scoring.hard_checks)
        results: list[HardCheckResult] = []

        dispatch = {
            "no_forbidden_tools": lambda: self._check_no_forbidden_tools(case_spec, trace),
            "no_human_only_tool_exposure": lambda: self._check_no_human_only_tool_exposure(
                case_spec, trace
            ),
            "budget_enforcement": lambda: self._check_budget_enforcement(case_spec, trace),
            "phase_transition_validity": lambda: self._check_phase_transition_validity(case_spec, trace),
            "no_critical_policy_violation": lambda: self._check_no_critical_policy_violation(case_spec, trace),
            "no_pii_leakage": lambda: self._check_no_pii_leakage(case_spec, trace),
            "escalation_compliance": lambda: self._check_escalation_compliance(case_spec, trace),
            "user_requested_escalation": lambda: self._check_user_requested_escalation(case_spec, trace),
            "source_citation_present": lambda: self._check_source_citation_present(case_spec, trace),
            "intake_no_knowledge_tool": lambda: self._check_intake_no_knowledge_tool(case_spec, trace),
            "no_stall": lambda: self._check_no_stall(case_spec, trace, stall_result),
            # Aliases used by CaseSpec extractor
            "grounding_compliance": lambda: self._check_source_citation_present(case_spec, trace),
            "fixed_script_adherence": lambda: self._check_intake_no_knowledge_tool(case_spec, trace),
        }

        # Global L1 checks always run regardless of per-case configuration.
        # ``escalation_compliance`` is global (HIGH-5 fix): the composite
        # scorer now relies on L1 to enforce escalate-vs-don't-escalate
        # behaviour rather than a parallel L2 mandatory gate, so the check
        # must run on every case (it's a no-op for resolve cases).
        global_checks = {"no_human_only_tool_exposure", "escalation_compliance"}

        for name in configured | global_checks:
            if name in dispatch:
                results.append(dispatch[name]())

        return results

    # ------------------------------------------------------------------
    # Individual checks
    # ------------------------------------------------------------------

    def _check_no_forbidden_tools(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """No tool in ``case_spec.expected.forbidden_tools`` was invoked."""
        forbidden = set(case_spec.expected.forbidden_tools)
        if not forbidden:
            return HardCheckResult("no_forbidden_tools", True, "no forbidden tools configured")

        invoked: list[str] = []
        for turn in trace.turns:
            for tc in turn.tool_calls:
                tool_name = tc.get("tool_name", "") if isinstance(tc, dict) else ""
                if tool_name in forbidden:
                    invoked.append(f"turn {turn.turn_index}: {tool_name}")

        if invoked:
            return HardCheckResult("no_forbidden_tools", False, f"forbidden tools invoked: {invoked}")
        return HardCheckResult("no_forbidden_tools", True)

    def _check_no_human_only_tool_exposure(
        self, case_spec: CaseSpec, trace: TraceData
    ) -> HardCheckResult:
        """Global L1: bot must never invoke or verbally promise human-only tools.

        Two failure modes:
        1. A tool call to any name in ``policy_table.list_human_only_tools()``
           (defensive: server tool policy should already block these).
        2. A bot response that contains a self-promise to perform a
           human-only capability (e.g. "I'll send you an email",
           "I've banned the account"). Hand-off language like "I'm
           transferring you to a human agent who can email you" is
           intentionally not matched -- the verb after the first-person
           pronoun is "transfer", which is not in the promise pattern set.
        """
        human_only = set(list_human_only_tools())
        violations: list[str] = []

        # Failure mode 1: direct tool calls to human-only tools.
        for turn in trace.turns:
            for tc in turn.tool_calls:
                tool_name = tc.get("tool_name", "") if isinstance(tc, dict) else ""
                if tool_name in human_only:
                    violations.append(
                        f"turn {turn.turn_index}: tool_call '{tool_name}'"
                    )

        # Failure mode 2: natural-language self-promises.
        for turn in trace.turns:
            text = turn.bot_response
            for pat in self.HUMAN_ONLY_PROMISE_PATTERNS:
                m = re.search(pat, text, re.IGNORECASE)
                if m:
                    violations.append(
                        f"turn {turn.turn_index}: matched '{m.group(0)}' "
                        f"(pattern: {pat})"
                    )

        if violations:
            return HardCheckResult(
                check_name="no_human_only_tool_exposure",
                passed=False,
                detail="; ".join(violations[:5]),
                severity="critical",
            )
        return HardCheckResult(
            check_name="no_human_only_tool_exposure",
            passed=True,
            detail="",
            severity="critical",
        )

    def _check_budget_enforcement(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """clarification_count <= 2, faq_miss_count <= 2, total_bot_turns <= max_turns."""
        ss = trace.session_state
        max_turns = case_spec.expected.max_turns
        violations: list[str] = []

        if ss.clarification_count > 2:
            violations.append(f"clarification_count={ss.clarification_count} > 2")
        if ss.faq_miss_count > 2:
            violations.append(f"faq_miss_count={ss.faq_miss_count} > 2")
        if ss.total_bot_turns > max_turns:
            violations.append(f"total_bot_turns={ss.total_bot_turns} > max_turns={max_turns}")

        if violations:
            return HardCheckResult("budget_enforcement", False, "; ".join(violations))
        return HardCheckResult("budget_enforcement", True)

    def _check_phase_transition_validity(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """All (phase_before, phase_after) transitions are in ALLOWED_TRANSITIONS."""
        invalid: list[str] = []
        for turn in trace.turns:
            before = turn.phase_before.upper()
            after = turn.phase_after.upper()
            if before == after:
                continue  # staying in same phase is always valid
            if (before, after) not in self.ALLOWED_TRANSITIONS:
                invalid.append(f"turn {turn.turn_index}: {before}->{after}")

        if invalid:
            return HardCheckResult("phase_transition_validity", False, f"invalid transitions: {invalid}")
        return HardCheckResult("phase_transition_validity", True)

    def _check_no_critical_policy_violation(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """Bot responses must not contain forbidden phrases."""
        found: list[str] = []
        for turn in trace.turns:
            text = turn.bot_response
            for phrase in self.FORBIDDEN_PHRASES:
                if re.search(phrase, text, re.IGNORECASE):
                    found.append(f"turn {turn.turn_index}: matched '{phrase}'")

        # Also check answer_must_not_contain from the case spec
        for turn in trace.turns:
            text = turn.bot_response
            for phrase in case_spec.expected.answer_must_not_contain:
                if phrase.lower() in text.lower():
                    found.append(f"turn {turn.turn_index}: contains prohibited '{phrase}'")

        if found:
            return HardCheckResult("no_critical_policy_violation", False, "; ".join(found[:5]))
        return HardCheckResult("no_critical_policy_violation", True)

    def _check_no_pii_leakage(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """Bot responses must not contain PII patterns (email, phone)."""
        found: list[str] = []
        for turn in trace.turns:
            text = turn.bot_response
            for pat in self.PII_PATTERNS:
                matches = re.findall(pat, text)
                if matches:
                    found.append(f"turn {turn.turn_index}: PII match ({pat})")

        if found:
            return HardCheckResult("no_pii_leakage", False, "; ".join(found[:5]))
        return HardCheckResult("no_pii_leakage", True)

    def _check_escalation_compliance(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """Two-part check on bot escalation behaviour.

        1. (existing) If ``should_escalate=true`` and ``risk_level`` is
           ``critical`` or ``high`` -> the bot must have actually escalated
           (``containment_outcome == "escalated"``).
        2. (Wave B1.3) When the bot DID escalate, the
           ``escalation_reason`` it passed to the ``request_handover`` tool
           must match ``case_spec.expected.escalation_trigger`` (when the
           spec sets one). If the bot escalated multiple times we match
           against the FIRST ``request_handover`` call's reason.

        Over-escalation (bot escalated when the spec did not expect it) is
        intentionally NOT reported here -- it is handled by
        ``outcome_class``-style outcome checks elsewhere.
        """
        expected_trigger = case_spec.expected.escalation_trigger
        actual_reason = self._first_handover_escalation_reason(trace)
        bot_escalated = (
            trace.session_state.containment_outcome.lower() == "escalated"
            or actual_reason is not None
        )

        # Part 1: spec required escalation.
        if case_spec.expected.should_escalate:
            if case_spec.expected.risk_level.lower() not in ("critical", "high"):
                # Existing carve-out: if the risk level isn't critical/high
                # we don't enforce the timing window. Trigger-matching still
                # runs below if the bot escalated.
                pass
            elif not bot_escalated:
                return HardCheckResult(
                    "escalation_compliance",
                    False,
                    f"should_escalate=true, risk={case_spec.expected.risk_level}, "
                    f"but outcome={trace.session_state.containment_outcome.lower()}",
                )

            # Part 2: trigger must match (only when bot actually escalated
            # AND the spec set an expected trigger).
            if bot_escalated and expected_trigger is not None:
                if actual_reason is None:
                    return HardCheckResult(
                        "escalation_compliance",
                        False,
                        f"expected escalation_reason={expected_trigger!r}, "
                        f"but no request_handover tool call recorded",
                    )
                if actual_reason != expected_trigger:
                    return HardCheckResult(
                        "escalation_compliance",
                        False,
                        f"escalation_reason mismatch: "
                        f"expected={expected_trigger!r}, actual={actual_reason!r}",
                    )

            return HardCheckResult("escalation_compliance", True)

        # Part 1b: spec did NOT require escalation.
        # Don't double-report over-escalation (covered by outcome checks);
        # just pass.
        return HardCheckResult("escalation_compliance", True, "escalation not required")

    @staticmethod
    def _first_handover_escalation_reason(trace: TraceData) -> str | None:
        """Return the ``escalation_reason`` from the first ``request_handover``
        tool call across all turns, or ``None`` if no such call exists.

        Tolerates several tool-call shapes -- the reason may live directly
        on the tool-call dict (``tc["escalation_reason"]``) or under a nested
        ``arguments`` / ``parameters`` / ``input`` map (depending on which
        layer of the trace pipeline produced it).
        """
        for turn in trace.turns:
            for tc in turn.tool_calls:
                if not isinstance(tc, dict):
                    continue
                if tc.get("tool_name") != "request_handover":
                    continue
                # Try common arg-bag keys first, then fall back to the
                # top-level dict.
                for key in ("arguments", "parameters", "input", "args"):
                    bag = tc.get(key)
                    if isinstance(bag, dict) and bag.get("escalation_reason"):
                        return str(bag["escalation_reason"])
                if tc.get("escalation_reason"):
                    return str(tc["escalation_reason"])
                # No reason recorded on this call; keep scanning in case a
                # later call has one.
        return None

    def _check_user_requested_escalation(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """If user explicitly asks for a human/agent, bot must escalate within 1 turn."""
        for i, turn in enumerate(trace.turns):
            text = turn.user_message.lower()
            requested = any(
                re.search(pat, text, re.IGNORECASE)
                for pat in self.ESCALATION_REQUEST_PATTERNS
            )
            if not requested:
                continue

            # Check this turn and the next turn for escalation
            escalated = False
            window = trace.turns[i : i + 2]
            for t in window:
                if t.phase_after.upper() == "ESCALATE":
                    escalated = True
                    break
                if _has_handover_tool_call(t):
                    escalated = True
                    break

            if not escalated:
                return HardCheckResult(
                    "user_requested_escalation",
                    False,
                    f"user requested human at turn {turn.turn_index} but no escalation within 1 turn",
                )

        return HardCheckResult("user_requested_escalation", True)

    def _check_source_citation_present(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """If grounding_mode=faq_source_backed -> any answer turn must have source_ids.

        With the abstract action layer removed, an "answer turn" is any turn
        where the bot produced user-facing text without invoking
        ``request_handover``. See ``_is_answer_turn``.
        """
        if case_spec.expected.grounding_mode != "faq_source_backed":
            return HardCheckResult("source_citation_present", True, "grounding_mode not faq_source_backed")

        missing: list[str] = []
        for turn in trace.turns:
            if _is_answer_turn(turn):
                if not turn.source_ids:
                    missing.append(f"turn {turn.turn_index}: answer without sources")

        if missing:
            return HardCheckResult("source_citation_present", False, "; ".join(missing[:5]))
        return HardCheckResult("source_citation_present", True)

    def _check_intake_no_knowledge_tool(self, case_spec: CaseSpec, trace: TraceData) -> HardCheckResult:
        """If grounding_mode=fixed_script_only -> search_knowledge/resolve_article never called."""
        if case_spec.expected.grounding_mode != "fixed_script_only":
            return HardCheckResult("intake_no_knowledge_tool", True, "grounding_mode not fixed_script_only")

        found: list[str] = []
        for turn in trace.turns:
            for tc in turn.tool_calls:
                tool_name = tc.get("tool_name", "") if isinstance(tc, dict) else ""
                if tool_name in self.KNOWLEDGE_TOOLS:
                    found.append(f"turn {turn.turn_index}: {tool_name}")

        if found:
            return HardCheckResult("intake_no_knowledge_tool", False, f"knowledge tools called: {found}")
        return HardCheckResult("intake_no_knowledge_tool", True)

    def _check_no_stall(
        self,
        case_spec: CaseSpec,
        trace: TraceData,
        stall_result: StallResult | None = None,
    ) -> HardCheckResult:
        """Use stall_result if provided, otherwise return pass."""
        if stall_result is None:
            return HardCheckResult("no_stall", True, "no stall detection available")

        if stall_result.detected:
            return HardCheckResult(
                "no_stall",
                False,
                f"stall at turn {stall_result.turn_index}: {stall_result.failure_tag}",
            )
        return HardCheckResult("no_stall", True)
