"""Outcome checks -- L2 result-level scoring (0.0 to 1.0 per check)."""

from __future__ import annotations

import re
from dataclasses import dataclass

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.scoring import escalation_intent as _esc_intent
from eval_interactive.trace.models import TraceData


@dataclass
class OutcomeCheckResult:
    """Result of a single outcome check.

    ``severity`` controls whether the result contributes to the composite
    case-pass gate or is purely diagnostic. Defaults to ``"critical"`` so
    historical callers keep their gate-contribution semantics. S-Eval-1
    (M3-Eval) demotes ``tool_sequence_match`` to ``"advisory"``; the
    composite scorer excludes advisory results from the outcome-score mean
    and the mandatory-L2 gate.
    """

    check_name: str
    score: float  # 0.0 to 1.0
    detail: str = ""
    severity: str = "critical"


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
        expected = case_spec.expected
        uc_family = self._uc_family(expected.primary_uc)
        if expected.outcome_class == "escalate":
            configured.add("handover_completeness")
            # Codex 2026-05-03 round 3 / 2026-05-04 round 4: case_id_present
            # remains mandatory for UC-H/J/K escalations because losing it
            # makes the handover unusable (round 4 §H4). Auto-include so
            # the gate has a real result. ``tool_sequence_match`` is now a
            # diagnostic only (round 4 §"What Should Become Soft or
            # Diagnostic") -- it is graded when the spec lists it but no
            # longer auto-included or gated.
            if uc_family in _CASE_ID_UCS:
                configured.add("case_id_present")
        # Codex 2026-05-03 round 3 §3.3: also exercise handover_completeness
        # whenever the bot ACTUALLY escalated, even if the spec expected
        # resolve. Production safety wants over-escalations to still produce
        # a complete payload. The check itself short-circuits to 1.0 when
        # the session did not escalate, so this is a no-op for resolve runs.
        if trace.session_state.containment_outcome.lower() == "escalated":
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
        """Drift-aware match with preservation evidence.

        Codex 2026-05-04 round 4 §"Exact Primary UC" demoted exact-UC
        mismatch from a hard fail; codex 2026-05-04 round 5 §"correct_uc
        gives full credit for any secondary UC" tightens the relaxation
        so secondary-UC credit requires evidence of real user drift, not
        just the case spec listing the UC as secondary. Concretely:

        - primary UC match → 1.0.
        - actual UC is in ``secondary_ucs`` AND the primary UC family is
          also preserved in ``candidate_use_cases`` (drift evidence) →
          1.0.
        - actual UC is in ``secondary_ucs`` but the primary UC family
          dropped from ``candidate_use_cases`` (lost the original issue)
          → 0.5 (partial credit; not enough to clear the mandatory L2
          gate, which requires >=1.0).
        - otherwise → 0.0.
        """
        expected = self._normalize_uc(case_spec.expected.primary_uc)
        actual = self._normalize_uc(trace.session_state.active_use_case)

        if expected == actual:
            return OutcomeCheckResult("correct_uc", 1.0)

        secondary = {self._normalize_uc(s) for s in case_spec.expected.secondary_ucs if s}
        if actual and actual in secondary:
            candidate_families = {
                self._normalize_uc(c)
                for c in trace.session_state.candidate_use_cases
                if c
            }
            primary_preserved = expected in candidate_families
            handover_mentions_primary = self._handover_summary_mentions(
                trace, expected, case_spec.form_context.description
            )
            # Codex 2026-05-04 round 6 §P1: candidate-list presence alone
            # is gameable (cs_interactive_095 kept UC-A in candidates while
            # the bot escalated UC-D without answering the original
            # question). Full credit now requires *either* the primary UC
            # appears in the handover summary text (real handoff
            # preservation) *or* the primary UC is preserved in
            # candidate_use_cases. Failing both → 0.5.
            if primary_preserved or handover_mentions_primary:
                detail = (
                    f"matched secondary UC {actual}; primary {expected} "
                    f"preserved (candidates={primary_preserved}, "
                    f"handover_mentions={handover_mentions_primary})"
                )
                return OutcomeCheckResult("correct_uc", 1.0, detail)
            return OutcomeCheckResult(
                "correct_uc",
                0.5,
                f"matched secondary UC {actual} but primary {expected} "
                f"dropped from candidates {sorted(candidate_families)} and "
                f"not referenced in handover summary",
            )

        return OutcomeCheckResult(
            "correct_uc", 0.0, f"expected={expected}, actual={actual}"
        )

    def _check_correct_outcome(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """Honor ``Expected.acceptable_outcomes`` with a quality guard.

        Codex 2026-05-04 round 4 §"Resolve vs Escalate" — outcome should
        be reframed as acceptable outcome. Codex 2026-05-04 round 5 §P0
        adds a quality guard: when the actual outcome differs from the
        primary ``outcome_class`` (i.e. the case fell back to a listed
        alternative), the alternative path must still meet a minimum
        service-quality bar. Otherwise a non-answer escalation could pass
        a resolve-expected case just because the spec listed
        ``[resolve, escalate]``.

        Quality gate per cross-class fallback:
        - primary=resolve, actual=escalate: handover payload must be
          present with a non-trivial summary (>= 10 chars after strip)
          and an ``escalation_reason``. Otherwise the fallback scores
          0.5 (partial credit so the outcome still beats a true
          mismatch but does not pass the L2 mandatory gate).
        - primary=escalate, actual=resolve: bot must have produced a
          source-cited answer turn (``trace.turns`` containing a
          turn with non-empty ``source_ids``). Otherwise 0.5.

        Falls back to the strict ``outcome_class`` match when
        ``acceptable_outcomes`` is empty, preserving legacy behaviour.

        S-Auto-40 (WP1-A): when the CaseSpec carries a declarative
        ``conditional_outcome_acceptance`` block, the verdict is delegated to
        the generic conditional-outcome evaluator instead. That evaluator
        reads the Phase-1 ``user_state`` signal and a closure-quality marker
        and returns PASS only for a SATISFIED→resolve path or an
        adjudicated UNRESOLVED→escalate path; a positively-UNRESOLVED
        escalation without an adjudication artifact scores 0.0
        (``CONDITIONAL_ELIGIBLE`` / ``REVIEW_REQUIRED``), so it never
        auto-passes the mandatory gate. This is a §5.4 product-authorized
        ``eval_spec`` acceptance, NOT an unconditional ``resolve OR escalate``
        widen.
        """
        if getattr(case_spec, "conditional_outcome_acceptance", None):
            from eval_interactive.scoring.conditional_outcome import (
                evaluate_conditional_outcome,
            )

            cr = evaluate_conditional_outcome(case_spec, trace)
            return OutcomeCheckResult("correct_outcome", cr.score, cr.detail)

        expected = case_spec.expected.outcome_class.lower()
        raw_outcome = trace.session_state.containment_outcome.lower()
        actual = _OUTCOME_MAP.get(raw_outcome, raw_outcome)

        acceptable = {o.lower() for o in case_spec.expected.acceptable_outcomes}
        if not acceptable:
            acceptable = {expected}

        if actual not in acceptable:
            return OutcomeCheckResult(
                "correct_outcome", 0.0,
                f"actual={actual} not in acceptable={sorted(acceptable)}",
            )

        # Same-class match — full credit, no quality guard needed.
        if actual == expected:
            return OutcomeCheckResult(
                "correct_outcome",
                1.0,
                f"actual={actual} in acceptable={sorted(acceptable)}",
            )

        # Cross-class fallback — apply the round-5 quality guard.
        if actual == "escalate" and expected == "resolve":
            # WS-1 item 1 (replan §3 WS-1.1): this branch used to award 1.0
            # for a >=10-character summary plus any reason, while the
            # mirror-image branch below demanded real retrieval evidence.
            # Escalating cost 10 characters; resolving cost a cited answer —
            # so the ruler paid more for giving up than for solving. The two
            # directions are now symmetric: each requires evidence that the
            # chosen path was actually WORKED, not merely stamped.
            #
            # Evidence of work on the escalate path = the bot tried to
            # resolve first (a retrieval or citation turn at or before the
            # handover), which is the same predicate the L1
            # ``no_premature_escalation`` gate uses.
            #
            # Anti-误杀 exemptions (both are cases where "search first" is
            # not the correct behaviour, so requiring it would build a NEW
            # contradiction into the ruler — the exact defect WS-1 exists to
            # remove):
            #   * the customer asked to be handed to a human (product
            #     decision D2 keeps explicit human requests as a valid
            #     trigger). Uses the broad ``user_demanded_human`` waiver set
            #     (``escalation_intent`` class 1a ∪ 1b), so naming a specific
            #     person ("connect me with Monisha") counts — see the
            #     cs_interactive_208 note on
            #     ``HardChecker._check_no_premature_escalation``, or
            #   * the case runs the fixed-script intake path, where knowledge
            #     tools are forbidden outright
            #     (``intake_no_knowledge_tool``).
            # KNOWN STALENESS (WS-3 / A3, 2026-07-25): 23 UC-K specs still
            # declare ``grounding_mode: fixed_script_only`` from before UC-K
            # moved INTAKE -> PARTIAL, so they take this waiver without
            # earning it. See the matching note on
            # ``HardChecker._check_no_premature_escalation``; the fix is an
            # ``eval_spec`` grounding_mode correction, not a scoring change.
            payload = (
                trace.handover.handover_payload
                if trace.handover is not None
                else {}
            ) or {}
            summary = (payload.get("summary") or "").strip()
            reason = (payload.get("escalation_reason") or "").strip()
            handover_usable = len(summary) >= 10 and bool(reason)

            esc_pos = _esc_intent.first_escalation_position(trace)
            attempted = _esc_intent.has_resolution_attempt_before(trace, esc_pos)
            demanded_human = _esc_intent.user_demanded_human(trace, esc_pos)
            script_only = case_spec.expected.grounding_mode == "fixed_script_only"
            attempt_required = not (demanded_human or script_only)

            if attempt_required and not attempted:
                # Premature handover on a case the bot was expected to
                # resolve, with no evidence it tried. This is a total outcome
                # failure, not a partial one: scoring it 0.0 (rather than the
                # degraded 0.5) also emits the ``L2:correct_outcome`` tag so
                # the failure is visible in reports, not just in the gate.
                return OutcomeCheckResult(
                    "correct_outcome",
                    0.0,
                    "cross-class fallback REJECTED: actual=escalate on a "
                    "resolve-expected case with no resolution attempt "
                    "(no search_knowledge / resolve_article / cited turn "
                    f"before handover; handover summary={len(summary)} chars, "
                    f"reason={reason!r})",
                )

            if handover_usable:
                exempt = (
                    " (attempt waived: user explicitly requested a human)"
                    if attempt_required is False and demanded_human
                    else (
                        " (attempt waived: fixed_script_only intake path)"
                        if attempt_required is False
                        else ""
                    )
                )
                return OutcomeCheckResult(
                    "correct_outcome",
                    1.0,
                    f"cross-class fallback OK: actual=escalate after a genuine "
                    f"resolution attempt w/ useful handover (summary="
                    f"{len(summary)} chars, reason={reason}){exempt}",
                )
            return OutcomeCheckResult(
                "correct_outcome",
                0.5,
                f"cross-class fallback degraded: actual=escalate but handover "
                f"summary={len(summary)} chars, reason={reason!r}",
            )

        if actual == "resolve" and expected == "escalate":
            cited = any(getattr(t, "source_ids", None) for t in trace.turns)
            if cited:
                return OutcomeCheckResult(
                    "correct_outcome",
                    1.0,
                    "cross-class fallback OK: actual=resolve w/ source-cited answer",
                )
            return OutcomeCheckResult(
                "correct_outcome",
                0.5,
                "cross-class fallback degraded: actual=resolve but no source-cited answer turn",
            )

        # Other cross-class combos (abandoned etc.) keep simple full
        # credit since no quality rubric is defined for them yet.
        return OutcomeCheckResult(
            "correct_outcome",
            1.0,
            f"actual={actual} in acceptable={sorted(acceptable)}",
        )

    def _check_tool_sequence_match(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """Compare actual tool calls against expected_tool_sequence.

        1.0 for exact match.  Partial credit for longest common subsequence.

        S-Eval-1 (M3-Eval): demoted from L2 LCS gate-contributor to
        Tier-3 diagnostic. The score is still computed and recorded so
        per-case trace reports continue to show LCS alignment, but the
        composite scorer treats the result as advisory -- it does not
        contribute to the outcome-score mean and is not eligible for
        the mandatory-L2 gate.
        """
        expected_seq = case_spec.expected.expected_tool_sequence
        if not expected_seq:
            return OutcomeCheckResult(
                "tool_sequence_match", 1.0, "no expected sequence", severity="advisory"
            )

        actual_seq = [
            tc.get("tool_name", "") if isinstance(tc, dict) else ""
            for turn in trace.turns
            for tc in turn.tool_calls
        ]
        actual_seq = [t for t in actual_seq if t]  # drop empties

        if actual_seq == expected_seq:
            return OutcomeCheckResult(
                "tool_sequence_match", 1.0, "exact match", severity="advisory"
            )

        # Longest common subsequence for partial credit
        lcs_len = self._lcs_length(expected_seq, actual_seq)
        score = lcs_len / len(expected_seq) if expected_seq else 0.0
        return OutcomeCheckResult(
            "tool_sequence_match",
            round(score, 3),
            f"lcs={lcs_len}/{len(expected_seq)}, actual={actual_seq}",
            severity="advisory",
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

    # WS-1 item 3: score awarded to an expected escalation that was NOT
    # preceded by any resolution attempt. Low enough to be a clear signal,
    # non-zero because the escalation itself was the expected outcome — only
    # its timing is wrong.
    _UNATTEMPTED_ESCALATION_SCORE = 0.3

    def _check_escalation_timing(self, case_spec: CaseSpec, trace: TraceData) -> OutcomeCheckResult:
        """If escalation required: did it follow a genuine resolution attempt?

        WS-1 item 3 (replan §3 WS-1.3). The pre-WS-1 rubric scored 1.0 for
        any escalation at or before ``max_turns // 2`` and decayed linearly
        after, which means **a turn-0 handover scored best** — the dimension
        literally rewarded escalating sooner. Combined with the
        ``correct_outcome`` asymmetry it taught the agent that giving up
        early is the cheapest way to score.

        The rubric is now effort-ordered rather than clock-ordered:

        * escalation **after** a retrieval / citation turn -> 1.0
        * escalation with **no** attempt at all -> 0.3

        Lateness is deliberately no longer penalised here. ``turn_efficiency``
        and the ``budget_enforcement`` L1 check already price excess turns;
        re-pricing them in this dimension is what re-imported the
        "escalate earlier" gradient.

        Anti-误杀 exemptions (identical to ``correct_outcome`` and the L1
        ``no_premature_escalation`` gate, so the three never disagree):
        a customer request to be handed to a human (the broad
        ``user_demanded_human`` waiver set — ``escalation_intent`` class
        1a ∪ 1b, so "connect me with <named person>" counts; see the
        cs_interactive_208 note on
        ``HardChecker._check_no_premature_escalation``), and the
        ``fixed_script_only`` intake path where knowledge tools are
        forbidden. On both, an immediate handover is the correct behaviour.

        KNOWN STALENESS (WS-3 / A3, 2026-07-25): the ``fixed_script_only``
        waiver over-fires on the 23 UC-K specs whose grounding_mode predates
        UC-K's INTAKE -> PARTIAL move. See
        ``HardChecker._check_no_premature_escalation``.
        """
        if not case_spec.expected.should_escalate:
            return OutcomeCheckResult("escalation_timing", 1.0, "escalation not required")

        outcome = trace.session_state.containment_outcome.lower()
        if outcome != "escalated":
            return OutcomeCheckResult("escalation_timing", 0.0, "expected escalation but did not escalate")

        esc_pos = _esc_intent.first_escalation_position(trace)
        esc_turn = (
            trace.turns[esc_pos].turn_index if esc_pos is not None else None
        )

        if _esc_intent.has_resolution_attempt_before(trace, esc_pos):
            return OutcomeCheckResult(
                "escalation_timing",
                1.0,
                f"escalated at turn {esc_turn} after a resolution attempt",
            )

        if case_spec.expected.grounding_mode == "fixed_script_only":
            return OutcomeCheckResult(
                "escalation_timing",
                1.0,
                "fixed_script_only intake path: knowledge retrieval is "
                "forbidden, so a direct handover is the expected shape",
            )

        if _esc_intent.user_demanded_human(trace, esc_pos):
            return OutcomeCheckResult(
                "escalation_timing",
                1.0,
                f"escalated at turn {esc_turn} on an explicit customer "
                f"request for a human",
            )

        return OutcomeCheckResult(
            "escalation_timing",
            self._UNATTEMPTED_ESCALATION_SCORE,
            f"escalated at turn {esc_turn} with no prior resolution attempt "
            f"(no search_knowledge / resolve_article / cited turn) and no "
            f"explicit human request",
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
    def _handover_summary_mentions(
        trace: TraceData, primary_uc: str, form_description: str
    ) -> bool:
        """Return True if the handover summary references the primary UC's
        domain or the form-description issue text.

        Codex 2026-05-04 round 6 §P1: secondary-UC drift credit needs
        evidence the original issue was actually preserved on handoff,
        not just that it was once in the candidate list. We accept either
        a UC family token (e.g. ``UC-A``) or a content-bearing token from
        the form description appearing in the handover summary.
        """
        if trace.handover is None:
            return False
        payload = trace.handover.handover_payload or {}
        summary = (payload.get("summary") or "").lower()
        if not summary:
            return False
        if primary_uc and primary_uc.lower() in summary:
            return True
        # Content-bearing tokens from the form description (skip stop-ish
        # short words). 4-char minimum filters "the", "and", "for", etc.
        tokens = re.findall(r"[a-zA-Z]{4,}", (form_description or "").lower())
        for tok in tokens:
            if tok in summary:
                return True
        return False

    @staticmethod
    def _normalize_uc(uc: str) -> str:
        """Normalize use-case identifier: upper-case, strip trailing -NN suffixes."""
        uc = uc.strip().upper()
        return re.sub(r"-\d+$", "", uc)

    @staticmethod
    def _uc_family(uc: str | None) -> str:
        """Return the ``UC-X`` family stub of a use-case identifier (or "")."""
        if not uc:
            return ""
        normalized = OutcomeChecker._normalize_uc(uc)
        parts = normalized.split("-")
        if len(parts) >= 2:
            return f"{parts[0]}-{parts[1]}"
        return normalized

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
