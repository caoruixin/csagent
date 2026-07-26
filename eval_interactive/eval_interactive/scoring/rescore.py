"""Offline re-scoring of an already-recorded evaluation run.

Sprint 105 (item 1). Before this module there was no way to re-run the
scoring layer over a run that had already executed: the CLI offered
``extract`` / ``run`` / ``lint`` / ``compare`` / ``set-baseline`` /
``runs`` and nothing else, so the only way to observe the effect of a
scoring-layer change was to spend a fresh batch of real bot sessions.
That made a scoring fix expensive to validate and, in practice,
un-validated.

Re-scoring is the correct *primary* evidence for a scoring-layer change:
it holds the bot's behaviour fixed (the recorded trace) and varies only
the ruler, which is exactly the attribution a scoring change needs.
``iteration_governance.md`` §5.7's mocked-LLM restriction does not apply
— that rule is about attributing a *behaviour* change to a prompt
change, and no behaviour is produced here.

Scope of the substrate
----------------------

``results.json`` persists the composite layer's full input set —
``l1_results`` / ``l2_results`` / ``l3_results`` / ``tier2_result`` /
``stall_detected`` — so :func:`~eval_interactive.scoring.composite.
compute_composite` can be replayed exactly. It does **not** persist
enough to re-run L1 / L2 / L3 themselves; see :data:`REHYDRATION_GAPS`
for the itemised list. This module therefore re-scores the *aggregation*
layer and leaves the check layers as recorded.

The fidelity oracle
-------------------

Rehydration is only trustworthy if it can be shown to reproduce the
recorded verdict. :func:`legacy_verdict` is a deliberately frozen,
standalone copy of the pre-Sprint-105 aggregation formula whose sole job
is to answer "do the rehydrated inputs reproduce the numbers this run
actually recorded?". It does **not** share code with ``composite.py`` on
purpose: an oracle that imports the implementation it verifies cannot
detect a change in that implementation. It gates nothing and must never
be used to score anything.
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path

from eval_interactive.case_spec.loader import load_case_spec
from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.scoring.composite import CompositeScore, compute_composite
from eval_interactive.scoring.hard_checks import HardCheckResult
from eval_interactive.scoring.llm_judge import JudgeResult
from eval_interactive.scoring.outcome_checks import OutcomeCheckResult
from eval_interactive.scoring.skill_procedure_check import (
    CriticalStepResult,
    Tier2Result,
)
from eval_interactive.scoring.stall_detector import StallResult

# Fields the scoring layers consume that ``results.json`` does NOT carry.
# Recorded verbatim so the feasibility finding is inspectable from code
# rather than only from a handoff document. Each entry is
# ``(owner, field, why it matters)``.
REHYDRATION_GAPS: tuple[tuple[str, str, str], ...] = (
    (
        "TraceData",
        "events",
        "EventEntry list is not serialised at all; L1 checks that read "
        "session events cannot be recomputed.",
    ),
    (
        "TraceData",
        "handover",
        "HandoverData (log_id / handover_payload / transfer_result) is not "
        "serialised; L2 handover_completeness cannot be recomputed (its "
        "recorded score is reused).",
    ),
    (
        "SessionState",
        "candidate_use_cases, clarification_count, faq_miss_count, "
        "form_context, customer_context, articles_shown, current_phase, "
        "total_bot_turns",
        "Only containment_outcome / active_use_case / escalation_reason "
        "survive into results.json.",
    ),
    (
        "TurnTrace",
        "turn_index, user_message, bot_response, source_ids, phase_before, "
        "phase_after, active_use_case, latency_ms",
        "per_turn_trace keeps only tool_calls / phase_plan / projection "
        "(executor._build_per_turn_trace), so 8 of TurnTrace's 10 fields "
        "are dropped. Bot text is recoverable from transcript[]; the phase "
        "pair and source_ids are not.",
    ),
    (
        "HardCheckResult / OutcomeCheckResult",
        "severity",
        "Dropped by executor._build_case_result for runs recorded before "
        "Sprint 105. Rehydration defaults to 'critical'; the fidelity "
        "oracle is what proves this was inert on a given run.",
    ),
)


# ---------------------------------------------------------------------------
# Rehydration
# ---------------------------------------------------------------------------


@dataclass
class RehydratedCase:
    """Scoring-layer inputs recovered from one serialised case result."""

    case_id: str
    l1_results: list[HardCheckResult]
    l2_results: list[OutcomeCheckResult]
    l3_results: list[JudgeResult]
    stall_result: StallResult
    tier2_result: Tier2Result
    # Recorded verdict, for the fidelity oracle.
    recorded_case_passed: bool
    recorded_composite: float
    recorded_outcome: float
    recorded_judge: float
    # Trace facts the ladder reads.
    containment_outcome: str
    active_use_case: str
    escalation_reason: str
    total_turns: int
    stop_reason: str
    case_passed_authority: str
    transcript: list[dict] = field(default_factory=list)
    per_turn_trace: list[dict] = field(default_factory=list)
    raw: dict = field(default_factory=dict)


def rehydrate_case(case: dict) -> RehydratedCase:
    """Rebuild the composite layer's inputs from one serialised case.

    ``severity`` is absent on L1 / L2 entries recorded before Sprint 105;
    the dataclass default (``"critical"``) applies. Whether that default
    distorts a given run is answered empirically by :func:`fidelity_check`,
    not assumed here.
    """
    l1 = [
        HardCheckResult(
            check_name=r["check"],
            passed=bool(r["passed"]),
            detail=r.get("detail", "") or "",
            severity=r.get("severity", "critical"),
        )
        for r in case.get("l1_results") or []
    ]
    l2 = [
        OutcomeCheckResult(
            check_name=r["check"],
            score=float(r["score"]),
            detail=r.get("detail", "") or "",
            severity=r.get("severity", "critical"),
        )
        for r in case.get("l2_results") or []
    ]
    l3 = [
        JudgeResult(
            dimension=r["dimension"],
            score=float(r["score"]),
            reasoning=r.get("reasoning", "") or "",
            severity=r.get("severity", "critical"),
            # Runs recorded before Sprint 105 carry no explicit marker, so
            # a fallback is recovered from the reasoning string the judge
            # itself wrote at the fallback sites. Exact-prefix, not a
            # keyword heuristic: these are literals emitted by
            # ``LlmJudge._call_llm`` / ``_parse_response``.
            fallback_reason=r.get("fallback_reason")
            or _recover_fallback_reason(r.get("reasoning", "") or ""),
        )
        for r in case.get("l3_results") or []
    ]

    t2raw = case.get("tier2_result") or {}
    tier2 = Tier2Result(
        passed=bool(t2raw.get("passed", True)),
        severity=t2raw.get("severity", "advisory"),
        failed_step_ids=list(t2raw.get("failed_step_ids") or []),
        per_step=[
            CriticalStepResult(
                step_id=s.get("step_id", ""),
                desc=s.get("desc", ""),
                outcome=s.get("outcome", "N/A"),
                severity=s.get("severity", "advisory"),
                detail=s.get("detail", "") or "",
            )
            for s in t2raw.get("per_step") or []
        ],
        detail=t2raw.get("detail", "") or "",
    )

    return RehydratedCase(
        case_id=case["case_id"],
        l1_results=l1,
        l2_results=l2,
        l3_results=l3,
        stall_result=StallResult(
            detected=bool(case.get("stall_detected", False)),
            failure_tag=case.get("stall_failure_tag") or "",
        ),
        tier2_result=tier2,
        recorded_case_passed=bool(case.get("case_passed", False)),
        recorded_composite=float(case.get("composite_score", 0.0)),
        recorded_outcome=float(case.get("outcome_score", 0.0)),
        recorded_judge=float(case.get("judge_score", 0.0)),
        containment_outcome=case.get("containment_outcome") or "",
        active_use_case=case.get("active_use_case") or "",
        escalation_reason=case.get("escalation_reason") or "",
        total_turns=int(case.get("total_turns") or 0),
        stop_reason=case.get("stop_reason") or "",
        case_passed_authority=case.get("case_passed_authority") or "programmatic",
        transcript=list(case.get("transcript") or []),
        per_turn_trace=list(case.get("per_turn_trace") or []),
        raw=case,
    )


# Literal reasoning strings written by ``LlmJudge`` when no real score was
# obtained. Matching these recovers the fallback marker on runs recorded
# before ``JudgeResult.fallback_reason`` existed.
_LEGACY_FALLBACK_PREFIXES: tuple[tuple[str, str], ...] = (
    ("LLM call failed; default score applied", "llm_call_failed"),
    ("parse failure; raw:", "parse_failure"),
    ("unexpected fallthrough", "unexpected_fallthrough"),
)


def _recover_fallback_reason(reasoning: str) -> str:
    """Recover a pre-Sprint-105 fallback marker from the reasoning string."""
    for prefix, reason in _LEGACY_FALLBACK_PREFIXES:
        if reasoning.startswith(prefix):
            return reason
    return ""


# ---------------------------------------------------------------------------
# Fidelity oracle — frozen pre-Sprint-105 aggregation
# ---------------------------------------------------------------------------


def legacy_verdict(case: RehydratedCase) -> tuple[float, float, float]:
    """Return ``(outcome_score, judge_score, composite)`` under the OLD rules.

    Frozen copy of the aggregation arithmetic as it stood before Sprint
    105, kept deliberately separate from ``composite.py`` so it can act as
    an oracle for it. The gate itself (``case_passed``) is not
    re-derived here — the recorded value is used — because the gate did
    not change in Sprint 105 and re-deriving it would require the L1 / L2
    severities the substrate does not carry.

    Old rules:
      - ``outcome_score = mean(gating L2 scores)``, else ``0.0``
      - ``judge_score  = mean(gating L3 scores) / 5.0``, else ``0.0``
      - ``composite    = 0.5 * outcome + 0.5 * judge``, or ``0.0`` when
        the case did not pass the gate.
    """
    gating_l2 = [r for r in case.l2_results if r.severity != "advisory"]
    outcome = sum(r.score for r in gating_l2) / len(gating_l2) if gating_l2 else 0.0

    gating_l3 = [r for r in case.l3_results if r.severity != "advisory"]
    judge = (
        (sum(r.score for r in gating_l3) / len(gating_l3)) / 5.0
        if gating_l3
        else 0.0
    )

    composite = (0.5 * outcome + 0.5 * judge) if case.recorded_case_passed else 0.0
    return round(outcome, 4), round(judge, 4), round(composite, 4)


@dataclass
class FidelityResult:
    """Did the rehydrated inputs reproduce the recorded verdict?"""

    case_id: str
    ok: bool
    detail: str = ""


def fidelity_check(case: RehydratedCase, tolerance: float = 1e-6) -> FidelityResult:
    """Verify that rehydration reproduces this case's recorded numbers.

    A case that fails this check has lost information in serialisation
    (most likely an L1 / L2 ``severity`` that the pre-Sprint-105 executor
    dropped) and MUST NOT be quoted as re-scoring evidence.
    """
    outcome, judge, composite = legacy_verdict(case)
    deltas = []
    for name, got, rec in (
        ("outcome_score", outcome, case.recorded_outcome),
        ("judge_score", judge, case.recorded_judge),
        ("composite", composite, case.recorded_composite),
    ):
        if abs(got - rec) > tolerance:
            deltas.append(f"{name}: recomputed={got} recorded={rec}")
    if deltas:
        return FidelityResult(case.case_id, False, "; ".join(deltas))
    return FidelityResult(case.case_id, True, "reproduces recorded verdict")


# ---------------------------------------------------------------------------
# Run-level re-scoring
# ---------------------------------------------------------------------------


@dataclass
class CaseRescore:
    """One case, scored as recorded and scored again under current rules."""

    case_id: str
    case_passed_authority: str
    fidelity: FidelityResult
    recorded_case_passed: bool
    recorded_composite: float
    recorded_outcome: float
    recorded_judge: float
    rescored: CompositeScore | None
    spec_found: bool
    containment_outcome: str = ""
    containment_tier: str = ""
    tier_detail: str = ""
    l3_fallback_calls: int = 0
    l3_total_calls: int = 0
    note: str = ""

    # -- derived views -------------------------------------------------

    @property
    def recorded_status(self) -> str:
        return (
            "PASS"
            if self.recorded_case_passed and self.recorded_composite >= 0.7
            else "FAIL"
        )

    @property
    def gating_l3_collapsed(self) -> bool:
        """Gating L3 dims WERE configured, and every one of them fell back.

        Distinct from "no gating dim configured": the former is a judge
        *infrastructure* failure that voids the verdict, the latter is a
        CaseSpec choice the composite renormalises around. Both leave
        ``judge_measured`` False, which is why the tag — not the flag —
        is what separates them.
        """
        return self.rescored is not None and (
            "L3_UNMEASURED:all_gating_dims_fell_back" in self.rescored.failure_tags
        )

    @property
    def rescored_status(self) -> str:
        if self.rescored is None:
            return "UNSCORED"
        if self.gating_l3_collapsed:
            return "UNMEASURED"
        return (
            "PASS"
            if self.rescored.case_passed and self.rescored.composite >= 0.7
            else "FAIL"
        )

    @property
    def direction(self) -> str:
        """Did the ruler get stricter, looser, or stay put on this case?"""
        if self.rescored is None:
            return "n/a"
        delta = round(self.rescored.composite - self.recorded_composite, 4)
        if delta > 0:
            return f"looser (+{delta})"
        if delta < 0:
            return f"stricter ({delta})"
        return "unchanged"


@dataclass
class RunRescore:
    """Re-scoring report for one recorded run directory."""

    source: str
    run_id: str
    label: str
    cases: list[CaseRescore] = field(default_factory=list)

    @property
    def fidelity_ok(self) -> int:
        return sum(1 for c in self.cases if c.fidelity.ok)

    @property
    def l3_fallback_calls(self) -> int:
        return sum(c.l3_fallback_calls for c in self.cases)

    @property
    def l3_total_calls(self) -> int:
        return sum(c.l3_total_calls for c in self.cases)

    @property
    def l3_layer_collapsed(self) -> bool:
        """Every L3 dimension call in the run fell back to the default score.

        A run in this state carries no L3 signal at all — the failure mode
        observed live on 2026-07-25, where a temperature rejection turned
        the whole layer into the constant 3.0 behind one log line.
        """
        return self.l3_total_calls > 0 and self.l3_fallback_calls == self.l3_total_calls

    @property
    def verdict_usable(self) -> bool:
        """Is this run's verdict meaningful at all?"""
        return not self.l3_layer_collapsed and self.fidelity_ok == len(self.cases)

    @property
    def evidence_clean(self) -> bool:
        """May this run be quoted as evidence for a scoring change?

        Stricter than :attr:`verdict_usable`, per the Sprint 105 contract
        §7.4: *any* L3 fallback call disqualifies the run, not just a
        total collapse. One degraded dimension is enough to mean the judge
        layer was not operating normally when these numbers were recorded.
        """
        return self.l3_fallback_calls == 0 and self.fidelity_ok == len(self.cases)


def _load_specs(spec_root: Path) -> dict[str, CaseSpec]:
    """Index every loadable CaseSpec under ``spec_root`` by ``case_id``."""
    specs: dict[str, CaseSpec] = {}
    for path in sorted(spec_root.rglob("*.yaml")):
        if path.name.startswith("_"):
            continue
        try:
            spec = load_case_spec(path)
        except Exception:  # noqa: BLE001 — a malformed spec must not abort re-scoring
            continue
        specs.setdefault(spec.case_id, spec)
    return specs


def resolve_results_path(run_path: str | Path) -> Path:
    """Accept either a run directory or a ``results.json`` file."""
    p = Path(run_path)
    if p.is_dir():
        return p / "results.json"
    return p


def rescore_run(
    run_path: str | Path,
    spec_root: str | Path,
    *,
    ladder: bool = True,
) -> RunRescore:
    """Re-run the aggregation layer over a recorded run. No bot call."""
    results_path = resolve_results_path(run_path)
    if not results_path.exists():
        raise FileNotFoundError(f"no results.json at {results_path}")

    data = json.loads(results_path.read_text(encoding="utf-8"))
    specs = _load_specs(Path(spec_root))

    report = RunRescore(
        source=str(results_path),
        run_id=data.get("run_id", ""),
        label=data.get("label", "") or "",
    )

    for raw_case in data.get("case_results") or []:
        case = rehydrate_case(raw_case)
        fidelity = fidelity_check(case)
        spec = specs.get(case.case_id)

        fallbacks = [r for r in case.l3_results if r.fallback_reason]

        entry = CaseRescore(
            case_id=case.case_id,
            case_passed_authority=case.case_passed_authority,
            fidelity=fidelity,
            recorded_case_passed=case.recorded_case_passed,
            recorded_composite=case.recorded_composite,
            recorded_outcome=case.recorded_outcome,
            recorded_judge=case.recorded_judge,
            rescored=None,
            spec_found=spec is not None,
            containment_outcome=case.containment_outcome,
            l3_fallback_calls=len(fallbacks),
            l3_total_calls=len(case.l3_results),
        )

        if spec is None:
            entry.note = "CaseSpec not found under the spec root; not re-scored"
            report.cases.append(entry)
            continue

        entry.rescored = compute_composite(
            case.case_id,
            case.l1_results,
            case.l2_results,
            case.l3_results,
            case.stall_result,
            case_spec=spec,
            tier2_result=case.tier2_result,
        )

        if ladder:
            # Imported lazily so the aggregation path stays usable if the
            # ladder module is being edited.
            from eval_interactive.scoring.containment_ladder import derive_tier

            tier = derive_tier(case)
            entry.containment_tier = tier.tier
            entry.tier_detail = tier.detail

        report.cases.append(entry)

    return report


def report_to_dict(report: RunRescore) -> dict:
    """JSON-serialisable form of a re-scoring report."""
    return {
        "source": report.source,
        "run_id": report.run_id,
        "label": report.label,
        "verdict_usable": report.verdict_usable,
        "evidence_clean": report.evidence_clean,
        "fidelity": {
            "ok": report.fidelity_ok,
            "total": len(report.cases),
            "mismatches": [
                {"case_id": c.case_id, "detail": c.fidelity.detail}
                for c in report.cases
                if not c.fidelity.ok
            ],
        },
        "l3": {
            "fallback_calls": report.l3_fallback_calls,
            "total_calls": report.l3_total_calls,
            "layer_collapsed": report.l3_layer_collapsed,
        },
        "cases": [
            {
                "case_id": c.case_id,
                "authority": c.case_passed_authority,
                "fidelity_ok": c.fidelity.ok,
                "recorded": {
                    "status": c.recorded_status,
                    "composite": c.recorded_composite,
                    "outcome": c.recorded_outcome,
                    "judge": c.recorded_judge,
                    "case_passed": c.recorded_case_passed,
                },
                "rescored": (
                    None
                    if c.rescored is None
                    else {
                        "status": c.rescored_status,
                        "composite": c.rescored.composite,
                        "outcome": c.rescored.outcome_score,
                        "judge": c.rescored.judge_score,
                        "judge_measured": c.rescored.judge_measured,
                        "judge_basis": c.rescored.judge_basis,
                        "case_passed": c.rescored.case_passed,
                        "failure_tags": c.rescored.failure_tags,
                    }
                ),
                "direction": c.direction,
                "containment_outcome": c.containment_outcome,
                "containment_tier": c.containment_tier,
                "tier_detail": c.tier_detail,
                "l3_fallback_calls": c.l3_fallback_calls,
                "l3_total_calls": c.l3_total_calls,
                "note": c.note,
            }
            for c in report.cases
        ],
    }
