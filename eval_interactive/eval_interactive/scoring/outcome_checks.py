"""Outcome checks -- L2 result-level scoring (0.0 to 1.0 per check)."""

from __future__ import annotations

import re
from dataclasses import dataclass

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.trace.models import TraceData


@dataclass
class OutcomeCheckResult:
    """Result of a single outcome check."""

    check_name: str
    score: float  # 0.0 to 1.0
    detail: str = ""


# Use-case families that require case_id in handover payload
_CASE_ID_UCS = {"UC-H", "UC-J", "UC-K"}

# Map containment_outcome values to expected outcome_class values
_OUTCOME_MAP = {
    "resolved": "resolve",
    "escalated": "escalate",
    "abandoned": "abandoned",
}


class OutcomeChecker:
    """L2 Outcome Checks -- result-level scoring."""

    ALL_CHECKS = [
        "correct_uc",
        "correct_outcome",
        "tool_sequence_match",
        "turn_efficiency",
        "handover_completeness",
        "case_id_present",
        "escalation_timing",
        "issue_preservation",
    ]

    # Map alias names to their canonical check names so that duplicate
    # scoring is avoided when a CaseSpec configures both forms.
    _ALIAS_TO_CANONICAL = {
        "answer_accuracy": "correct_outcome",
        "escalation_triggered": "escalation_timing",
        "intake_fields_collected": "handover_completeness",
        "resolution_achieved": "correct_outcome",
    }

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def run_checks(self, case_spec: CaseSpec, trace: TraceData) -> list[OutcomeCheckResult]:
        """Run all applicable outcome checks.

        Runs every check listed in ``case_spec.scoring.outcome_checks`` plus
        any conditionally-mandatory check the composite scorer needs (Codex
        finding 1.4): for ``outcome_class == "escalate"`` we always run
        ``handover_completeness`` so the L2 gate has a real result to grade
        instead of fail-closing on ``L2_GATE_MISSING:handover_completeness``.

        Aliases are deduplicated against their canonical check so the same
        underlying scorer never contributes to the L2 mean more than once.
        """
        configured = set(case_spec.scoring.outcome_checks)
        # Auto-include conditionally-mandatory checks so the L2 gate logic in
        # composite.py never has to fail-closed on a missing-but-configured
        # check. Mirrors composite._conditional_mandatory_l2.
        if case_spec.expected.outcome_class == "escalate":
            configured.add("handover_completeness")
        results: list[OutcomeCheckResult] = []

        dispatch = {
            "correct_uc": lambda: self._check_correct_uc(case_spec, trace),
            "correct_outcome": lambda: self._check_correct_outcome(case_spec, trace),
            "tool_sequence_match": lambda: self._check_tool_sequence_match(case_spec, trace),
            "turn_efficiency": lambda: self._check_turn_efficiency(case_spec, trace),
            "handover_completeness": lambda: self._check_handover_completeness(case_spec, trace),
            "case_id_present": lambda: self._check_case_id_present(case_spec, trace),
            "escalation_timing": lambda: self._check_escalation_timing(case_spec, trace),
            "issue_preservation": lambda: self._check_issue_preservation(case_spec, trace),
            # Aliases used by CaseSpec extractor
            "answer_accuracy": lambda: self._check_correct_outcome(case_spec, trace),
            "escalation_triggered": lambda: self._check_escalation_timing(case_spec, trace),
            "intake_fields_collected": lambda: self._check_handover_completeness(case_spec, trace),
            "resolution_achieved": lambda: self._check_correct_outcome(case_spec, trace),
        }

        executed_canonical: set[str] = set()

        for name in configured:
            canonical = self._ALIAS_TO_CANONICAL.get(name, name)
            if canonical in executed_canonical:
                continue
            executed_canonical.add(canonical)
            if name in dispatch:
                results.append(dispatch[name]())

        return results

    # ------------------------------------------------------------------
    # Individual checks
    # ------------------------------------------------------------------

    def _check_correct_uc(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """1.0 if active_use_case matches expected primary_uc."""
        expected = self._normalize_uc(case_spec.expected.primary_uc)
        actual = self._normalize_uc(trace.session_state.active_use_case)

        if expected == actual:
            return OutcomeCheckResult("correct_uc", 1.0)
        return OutcomeCheckResult(
            "correct_uc", 0.0, f"expected={expected}, actual={actual}"
        )

    def _check_correct_outcome(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """1.0 if containment_outcome matches expected outcome_class."""
        expected = case_spec.expected.outcome_class.lower()
        raw_outcome = trace.session_state.containment_outcome.lower()
        actual = _OUTCOME_MAP.get(raw_outcome, raw_outcome)

        if expected == actual:
            return OutcomeCheckResult("correct_outcome", 1.0)
        return OutcomeCheckResult(
            "correct_outcome", 0.0, f"expected={expected}, actual={actual}"
        )

    def _check_tool_sequence_match(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """Compare actual tool calls against expected_tool_sequence.

        1.0 for exact match.  Partial credit for longest common subsequence.
        """
        expected_seq = case_spec.expected.expected_tool_sequence
        if not expected_seq:
            return OutcomeCheckResult("tool_sequence_match", 1.0, "no expected sequence")

        actual_seq = [
            tc.get("tool_name", "") if isinstance(tc, dict) else ""
            for turn in trace.turns
            for tc in turn.tool_calls
        ]
        actual_seq = [t for t in actual_seq if t]  # drop empties

        if actual_seq == expected_seq:
            return OutcomeCheckResult("tool_sequence_match", 1.0, "exact match")

        # Longest common subsequence for partial credit
        lcs_len = self._lcs_length(expected_seq, actual_seq)
        score = lcs_len / len(expected_seq) if expected_seq else 0.0
        return OutcomeCheckResult(
            "tool_sequence_match",
            round(score, 3),
            f"lcs={lcs_len}/{len(expected_seq)}, actual={actual_seq}",
        )

    def _check_turn_efficiency(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """1.0 if total_turns <= max_turns.  Linear decay to 0.0 at 2x max_turns."""
        max_turns = case_spec.expected.max_turns
        total = trace.session_state.total_bot_turns

        if max_turns <= 0:
            return OutcomeCheckResult("turn_efficiency", 1.0, "max_turns not set")

        if total <= max_turns:
            return OutcomeCheckResult("turn_efficiency", 1.0)

        # Linear decay from 1.0 at max_turns to 0.0 at 2*max_turns
        overshoot = total - max_turns
        score = max(0.0, 1.0 - overshoot / max_turns)
        return OutcomeCheckResult(
            "turn_efficiency",
            round(score, 3),
            f"total={total}, max={max_turns}",
        )

    def _check_handover_completeness(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """If escalated: handover payload must have required fields."""
        outcome = trace.session_state.containment_outcome.lower()
        if outcome != "escalated":
            return OutcomeCheckResult("handover_completeness", 1.0, "not escalated")

        if trace.handover is None:
            return OutcomeCheckResult("handover_completeness", 0.0, "no handover data")

        payload = trace.handover.handover_payload or {}
        required = ["session_id", "primary_use_case", "summary", "escalation_reason", "total_bot_turns"]
        missing = [f for f in required if not payload.get(f)]

        if missing:
            score = (len(required) - len(missing)) / len(required)
            return OutcomeCheckResult(
                "handover_completeness",
                round(score, 3),
                f"missing fields: {missing}",
            )
        return OutcomeCheckResult("handover_completeness", 1.0)

    def _check_case_id_present(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """If UC in {H, J, K} and escalated: case_id must be in handover payload."""
        uc = self._normalize_uc(case_spec.expected.primary_uc)
        outcome = trace.session_state.containment_outcome.lower()

        # Only applies if UC family is in the required set AND session was escalated
        uc_family = uc.split("-")[0] + "-" + uc.split("-")[1] if "-" in uc else uc
        if uc_family not in _CASE_ID_UCS:
            return OutcomeCheckResult("case_id_present", 1.0, "UC not in case_id-required set")
        if outcome != "escalated":
            return OutcomeCheckResult("case_id_present", 1.0, "not escalated")

        if trace.handover is None:
            return OutcomeCheckResult("case_id_present", 0.0, "no handover data")

        payload = trace.handover.handover_payload or {}
        if payload.get("case_id"):
            return OutcomeCheckResult("case_id_present", 1.0)
        return OutcomeCheckResult("case_id_present", 0.0, "case_id missing from handover payload")

    def _check_escalation_timing(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """If escalation required: check it occurred within reasonable turns.

        Score 1.0 if within max_turns/2, decaying to 0.0 at max_turns.
        """
        if not case_spec.expected.should_escalate:
            return OutcomeCheckResult("escalation_timing", 1.0, "escalation not required")

        outcome = trace.session_state.containment_outcome.lower()
        if outcome != "escalated":
            return OutcomeCheckResult("escalation_timing", 0.0, "expected escalation but did not escalate")

        # Find escalation turn
        esc_turn = None
        for turn in trace.turns:
            if turn.phase_after.upper() == "ESCALATE":
                esc_turn = turn.turn_index
                break

        if esc_turn is None:
            # Escalated at some unknown point -- give partial credit
            return OutcomeCheckResult("escalation_timing", 0.5, "escalated but turn unknown")

        max_t = case_spec.expected.max_turns
        threshold = max(1, max_t // 2)

        if esc_turn <= threshold:
            return OutcomeCheckResult("escalation_timing", 1.0)

        overshoot = esc_turn - threshold
        remaining = max_t - threshold
        score = max(0.0, 1.0 - overshoot / remaining) if remaining > 0 else 0.0
        return OutcomeCheckResult(
            "escalation_timing",
            round(score, 3),
            f"escalated at turn {esc_turn}, threshold={threshold}",
        )

    def _check_issue_preservation(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """After soft shift: original UC still in candidate_use_cases."""
        candidates = [c.upper() for c in trace.session_state.candidate_use_cases]
        expected = case_spec.expected.primary_uc.upper()
        # Strip suffixes for comparison
        expected_base = re.sub(r"-\d+$", "", expected)
        candidate_bases = [re.sub(r"-\d+$", "", c) for c in candidates]

        if expected_base in candidate_bases:
            return OutcomeCheckResult("issue_preservation", 1.0)

        # If there are secondary UCs, check at least one is preserved
        for sec in case_spec.expected.secondary_ucs:
            sec_base = re.sub(r"-\d+$", "", sec.upper())
            if sec_base in candidate_bases:
                return OutcomeCheckResult(
                    "issue_preservation", 0.5, f"primary UC lost, secondary {sec} preserved"
                )

        if not candidates:
            return OutcomeCheckResult("issue_preservation", 1.0, "no candidates to check")

        return OutcomeCheckResult(
            "issue_preservation",
            0.0,
            f"primary UC {expected_base} not in candidates {candidate_bases}",
        )

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _normalize_uc(uc: str) -> str:
        """Normalize use-case identifier: upper-case, strip trailing -NN suffixes."""
        uc = uc.strip().upper()
        return re.sub(r"-\d+$", "", uc)

    @staticmethod
    def _lcs_length(a: list[str], b: list[str]) -> int:
        """Compute the length of the longest common subsequence."""
        m, n = len(a), len(b)
        if m == 0 or n == 0:
            return 0
        dp = [[0] * (n + 1) for _ in range(m + 1)]
        for i in range(1, m + 1):
            for j in range(1, n + 1):
                if a[i - 1] == b[j - 1]:
                    dp[i][j] = dp[i - 1][j - 1] + 1
                else:
                    dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
        return dp[m][n]
