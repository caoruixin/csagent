"""Batch executor -- runs multiple evaluation sessions with configurable parallelism.

Orchestrates SessionRunner, scoring layers, and trace collection for each
case spec, producing a consolidated RunResult with per-case detail and
aggregate metrics.
"""

from __future__ import annotations

import asyncio
import json
import logging
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path

import click

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.config import Config
from eval_interactive.scoring.composite import CompositeScore, compute_composite
from eval_interactive.scoring.hard_checks import HardChecker, HardCheckResult
from eval_interactive.scoring.llm_judge import LlmJudge
from eval_interactive.scoring.outcome_checks import OutcomeChecker
from eval_interactive.scoring.stall_detector import StallDetector
from eval_interactive.simulator.agent_client import AgentClient
from eval_interactive.simulator.session_runner import SessionResult, SessionRunner
from eval_interactive.simulator.user_simulator import UserSimulator
from eval_interactive.trace.collector import TraceCollector, TraceContractError

logger = logging.getLogger(__name__)


@dataclass
class RunResult:
    """Consolidated output of a batch evaluation run."""

    run_id: str
    label: str
    timestamp: str
    case_results: list[dict]  # per-case dicts (serialisable)
    summary: dict  # aggregate metrics
    elapsed_ms: int


class BatchExecutor:
    """Runs multiple evaluation sessions with configurable parallelism."""

    def __init__(self, config: Config):
        """Initialize with the application config.

        Args:
            config: Application config containing bot URL, LLM settings,
                    parallelism, timeout, and output directory.
        """
        self._config = config

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    async def run_batch(
        self,
        cases: list[CaseSpec],
        label: str | None = None,
        parallel: int | None = None,
    ) -> RunResult:
        """Run all cases with bounded parallelism.

        For each case the pipeline is:
        1. Create per-case AgentClient, UserSimulator, StallDetector, SessionRunner
        2. Run session via SessionRunner.run_session (synchronous, run in thread)
        3. Collect trace via TraceCollector
        4. Run stall detection
        5. Run hard checks (L1)
        6. Run outcome checks (L2)
        7. Run LLM judge (L3)
        8. Compute composite score

        Args:
            cases: List of CaseSpec to evaluate.
            label: Human-readable label for this run.
            parallel: Max concurrent sessions. Defaults to config value.

        Returns:
            A RunResult with all per-case detail and summary metrics.
        """
        start_ns = time.monotonic_ns()
        now = datetime.now(timezone.utc)
        run_id = now.strftime("%Y%m%d-%H%M%S")
        timestamp = now.isoformat()
        label = label or f"run-{run_id}"
        parallel = parallel or self._config.batch.parallel

        semaphore = asyncio.Semaphore(parallel)
        case_results: list[dict] = []

        click.echo(
            f"Starting batch run '{label}' -- {len(cases)} case(s), "
            f"parallel={parallel}"
        )

        tasks = [
            self._run_one(case, semaphore)
            for case in cases
        ]
        completed = await asyncio.gather(*tasks, return_exceptions=True)

        for item in completed:
            if isinstance(item, Exception):
                logger.error("Unexpected gather exception: %s", item)
                continue
            if item is not None:
                case_results.append(item)

        summary = self._compute_summary(case_results)
        elapsed_ms = int((time.monotonic_ns() - start_ns) / 1_000_000)

        result = RunResult(
            run_id=run_id,
            label=label,
            timestamp=timestamp,
            case_results=case_results,
            summary=summary,
            elapsed_ms=elapsed_ms,
        )

        # Persist results
        self._save_results(result)

        click.echo(f"\nBatch run '{label}' complete in {elapsed_ms}ms")
        click.echo(f"  Total: {summary['total_cases']}  "
                    f"Passed: {summary['passed_cases']}  "
                    f"Failed: {summary['failed_cases']}  "
                    f"Success rate: {summary['task_success_rate']:.1%}")

        return result

    # ------------------------------------------------------------------
    # Per-case pipeline
    # ------------------------------------------------------------------

    async def _run_one(
        self,
        case_spec: CaseSpec,
        semaphore: asyncio.Semaphore,
    ) -> dict | None:
        """Run the full pipeline for a single case with semaphore gating."""
        async with semaphore:
            timeout_s = self._config.batch.timeout_per_session_seconds
            try:
                return await asyncio.wait_for(
                    self._execute_case(case_spec),
                    timeout=timeout_s,
                )
            except asyncio.TimeoutError:
                logger.error(
                    "Case %s timed out after %ds", case_spec.case_id, timeout_s
                )
                click.echo(f"  TIMEOUT  {case_spec.case_id}")
                return self._timeout_result(case_spec)
            except TraceContractError as exc:
                # Wave B1.4: trace telemetry is missing or malformed.
                # Distinguish from TIMEOUT / ERROR so reports can blame
                # the right layer (instrumentation vs. bot behaviour).
                logger.error(
                    "Case %s contract violation: field=%s phase=%s reason=%s",
                    case_spec.case_id, exc.field, exc.phase, exc.reason,
                )
                click.echo(
                    f"  CONTRACT {case_spec.case_id}: "
                    f"{exc.field} ({exc.reason})"
                )
                return self._contract_violation_result(case_spec, exc)
            except Exception as exc:
                logger.error(
                    "Case %s failed: %s", case_spec.case_id, exc, exc_info=True
                )
                click.echo(f"  ERROR    {case_spec.case_id}: {exc}")
                return self._error_result(case_spec, str(exc))

    async def _execute_case(self, case_spec: CaseSpec) -> dict:
        """Execute the full evaluation pipeline for one case in a thread."""
        return await asyncio.to_thread(self._execute_case_sync, case_spec)

    def _execute_case_sync(self, case_spec: CaseSpec) -> dict:
        """Synchronous pipeline for one case (runs inside a worker thread)."""
        agent_client = AgentClient(self._config.bot.base_url)
        user_simulator = UserSimulator(self._config)
        stall_detector = StallDetector(self._config)
        hard_checker = HardChecker()
        outcome_checker = OutcomeChecker()
        llm_judge = LlmJudge(self._config)

        try:
            # 1. Run session
            session_runner = SessionRunner(agent_client, user_simulator, stall_detector)
            session_result: SessionResult = session_runner.run_session(case_spec)

            # Fail fast: if session creation itself failed, the synthetic
            # "error-<hex>" id has no backend record. Skip trace collection
            # to avoid masking infrastructure failure as a contract violation.
            if session_result.creation_error is not None:
                msg = (
                    f"session_create_failed: {session_result.creation_error}. "
                    f"Backend at {self._config.bot.base_url} unreachable or "
                    f"returned an error during POST /v1/chat/sessions."
                )
                click.echo(f"  ERROR    {case_spec.case_id}: {msg}")
                # Sprint 6 §G0: surface ReadTimeout vs semantic-failure
                # distinction in the failure_tag so handoff / aggregation
                # downstream can separate upstream latency from a real
                # bot-side failure.
                if "ReadTimeout" in (session_result.creation_error or ""):
                    return self._error_result(
                        case_spec, msg, failure_kind="ReadTimeout"
                    )
                return self._error_result(case_spec, msg)

            # 2. Collect trace
            trace_collector = TraceCollector(agent_client)
            trace_data = trace_collector.collect(session_result.session_id)

            # 3. Build turn_traces list for stall detector
            turn_traces = [
                {
                    "turn_index": t.turn_index,
                    "tool_calls": t.tool_calls,
                }
                for t in trace_data.turns
            ]

            # 4. Stall detection
            stall_result = stall_detector.detect(
                session_result.transcript, turn_traces
            )

            # 5. Hard checks (L1)
            l1_results = hard_checker.run_checks(case_spec, trace_data, stall_result)

            # 6. Outcome checks (L2)
            l2_results = outcome_checker.run_checks(case_spec, trace_data)

            # 7. LLM judge (L3)
            l3_results = llm_judge.judge(
                case_spec, trace_data, session_result.transcript
            )

            # 8. Composite score (Wave B1.1: pass case_spec so the composite
            # scorer can apply mandatory-L2 gates in addition to L1 gates).
            composite_score = compute_composite(
                case_spec.case_id,
                l1_results,
                l2_results,
                l3_results,
                stall_result,
                case_spec=case_spec,
            )

            # Sprint 25 (R-per-llm-call-latency-instrumentation): fetch the
            # per-LLM-call log from the bot. The endpoint reads from the
            # ``llm_call_log`` table that ``LlmCallLogger`` already
            # populates on every chat/routing/rerank invocation; this is
            # the writer-side enrichment surface (Option B). Best-effort —
            # an endpoint failure must not fail the case.
            llm_calls = self._fetch_llm_calls(
                agent_client, session_result.session_id, case_spec.case_id
            )

            case_result = self._build_case_result(
                case_spec, session_result, trace_data, composite_score, llm_calls
            )

            status = "PASS" if composite_score.case_passed and composite_score.composite >= 0.7 else "FAIL"
            click.echo(
                f"  {status:7s} {case_spec.case_id}  "
                f"composite={composite_score.composite:.3f}  "
                f"turns={session_result.total_turns}  "
                f"stop={session_result.stop_reason}"
            )

            return case_result

        finally:
            agent_client.close()

    # ------------------------------------------------------------------
    # Serialisation helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _fetch_llm_calls(
        agent_client: AgentClient, session_id: str, case_id: str
    ) -> list[dict]:
        """Sprint 25: best-effort fetch of the per-LLM-call log.

        Returns the raw ``LlmCallLog`` rows for ``session_id`` from the bot's
        ``GET /v1/demo/sessions/{id}/llm-calls`` endpoint. Failures are
        swallowed and logged — the eval contract still produces a result
        for the case; the ``llm_calls`` field just stays empty.
        """
        if not session_id:
            return []
        try:
            return agent_client.get_llm_calls(session_id)
        except Exception as exc:  # noqa: BLE001 — best-effort enrichment
            logger.warning(
                "Sprint 25: failed to fetch llm_calls for case %s session %s: %s",
                case_id, session_id, exc,
            )
            return []

    @staticmethod
    def _build_per_turn_trace(trace_data) -> list[dict]:
        """Sprint 28 (R-per-case-trace-dump-for-smoke-harness): per-turn
        trace dump for the smoke harness.

        Serialises the three R-item-named per-turn fields from the already-
        collected ``trace_data.turns`` (each a ``TurnTrace`` populated by
        ``TraceCollector`` from the bot's
        ``GET /v1/demo/sessions/{id}/trace`` endpoint, which itself reads
        the ``bot_turns`` table V2 ``tool_calls`` + ``projected_context``
        JSONB columns):

        - ``tool_calls`` — verbatim from ``TurnTrace.tool_calls`` (already
          a list of dicts; ``TraceCollector`` parses the JSONB column).
        - ``phase_plan`` — the ``phase_plan`` sub-object inside the
          per-turn projection (populated by ``ContextProjectionBuilder``
          when a ``PhasePlan`` is present; ``None`` otherwise).
        - ``projection`` — the full per-turn projected_context dict (which
          also re-contains ``phase_plan`` — the duplication is intentional
          so consumers can read either path).

        The fourth R-item-named field, ``LlmCallEvents``, is **already**
        carried on each case result by the Sprint 25
        ``case_results[].llm_calls[]`` enrichment; Sprint 28 does not
        re-ship that axis.

        Defensive: returns ``[]`` when ``trace_data`` has no turns or the
        per-turn fields are missing / malformed, so the schema stays
        uniform across populated / placeholder / error paths.
        """
        turns = getattr(trace_data, "turns", None) or []
        out: list[dict] = []
        for turn in turns:
            projection = getattr(turn, "projected_context", None)
            if not isinstance(projection, dict):
                projection = {}
            phase_plan = projection.get("phase_plan")
            tool_calls = getattr(turn, "tool_calls", None)
            if not isinstance(tool_calls, list):
                tool_calls = []
            out.append(
                {
                    "tool_calls": list(tool_calls),
                    "phase_plan": phase_plan,
                    "projection": dict(projection),
                }
            )
        return out

    def _build_case_result(
        self,
        case_spec: CaseSpec,
        session_result: SessionResult,
        trace_data,
        composite_score: CompositeScore,
        llm_calls: list[dict] | None = None,
    ) -> dict:
        """Serialize a single case's results to a dict for JSON output."""
        return {
            "case_id": case_spec.case_id,
            "primary_uc": case_spec.expected.primary_uc,
            "expected_outcome": case_spec.expected.outcome_class,
            "session_id": session_result.session_id,
            "total_turns": session_result.total_turns,
            "stop_reason": session_result.stop_reason,
            "elapsed_ms": session_result.elapsed_ms,
            "case_passed": composite_score.case_passed,
            "composite_score": composite_score.composite,
            "outcome_score": composite_score.outcome_score,
            "judge_score": composite_score.judge_score,
            "failure_tags": composite_score.failure_tags,
            "stall_detected": composite_score.stall_result.detected,
            "stall_failure_tag": composite_score.stall_result.failure_tag,
            "containment_outcome": trace_data.session_state.containment_outcome,
            "active_use_case": trace_data.session_state.active_use_case,
            "escalation_reason": trace_data.session_state.escalation_reason,
            "l1_results": [
                {"check": r.check_name, "passed": r.passed, "detail": r.detail}
                for r in composite_score.l1_results
            ],
            "l2_results": [
                {"check": r.check_name, "score": r.score, "detail": r.detail}
                for r in composite_score.l2_results
            ],
            "l3_results": [
                {"dimension": r.dimension, "score": r.score, "reasoning": r.reasoning}
                for r in composite_score.l3_results
            ],
            "transcript": session_result.transcript,
            # Codex (latest review) §1.1: per-case status must use the same
            # gate as the summary pass count — case_passed AND composite>=0.7
            # — so a low-composite case can never be serialised as "PASS"
            # while the summary counts it as failed.
            "status": (
                "PASS"
                if composite_score.case_passed and composite_score.composite >= 0.7
                else "FAIL"
            ),
            # Wave B1.4: surface lenient-mode contract warnings so they
            # show up in the per-case detail in reports without being
            # confused with bot misbehaviour.
            "contract_warnings": list(
                getattr(trace_data, "contract_warnings", []) or []
            ),
            # Sprint 25 (R-per-llm-call-latency-instrumentation): per-
            # LLM-call timing rows from the bot's ``llm_call_log`` table
            # (chat / routing / rerank), surfaced unchanged so downstream
            # analysis can compute p50/p95 by ``call_type`` without
            # conflating tool dispatch or persistence overhead. May be an
            # empty list when the endpoint is unreachable; this is
            # logged at fetch time and does not fail the case.
            "llm_calls": list(llm_calls or []),
            # Sprint 28 (R-per-case-trace-dump-for-smoke-harness): per-
            # turn trace dump — one entry per bot turn carrying
            # ``tool_calls`` + ``phase_plan`` + full per-turn
            # ``projection``. Sourced from the already-collected
            # ``trace_data.turns[]`` (TraceCollector hydrates from the
            # bot's ``/v1/demo/sessions/{id}/trace`` endpoint, which
            # reads the V2 ``bot_turns`` JSONB columns). The fourth
            # R-item-named field, ``LlmCallEvents``, is already shipped
            # at case level via ``llm_calls`` above — not re-shipped.
            "per_turn_trace": self._build_per_turn_trace(trace_data),
        }

    def _timeout_result(self, case_spec: CaseSpec) -> dict:
        """Build a placeholder result for a timed-out case."""
        return {
            "case_id": case_spec.case_id,
            "primary_uc": case_spec.expected.primary_uc,
            "expected_outcome": case_spec.expected.outcome_class,
            "session_id": "",
            "total_turns": 0,
            "stop_reason": "timeout",
            "elapsed_ms": self._config.batch.timeout_per_session_seconds * 1000,
            "case_passed": False,
            "composite_score": 0.0,
            "outcome_score": 0.0,
            "judge_score": 0.0,
            "failure_tags": ["TIMEOUT"],
            "stall_detected": False,
            "stall_failure_tag": "",
            "containment_outcome": "",
            "active_use_case": "",
            "escalation_reason": "",
            "l1_results": [],
            "l2_results": [],
            "l3_results": [],
            "transcript": [],
            "status": "TIMEOUT",
            "llm_calls": [],
            "per_turn_trace": [],
        }

    def _error_result(
        self,
        case_spec: CaseSpec,
        error_msg: str,
        failure_kind: str | None = None,
    ) -> dict:
        """Build a placeholder result for a case that raised an exception.

        Sprint 6 §G0: when ``failure_kind`` is set (e.g. ``ReadTimeout``),
        emit an extra structured failure tag alongside the legacy
        ``ERROR:...`` tag so the post-Sprint-6 handoff / aggregation can
        separate upstream latency from a real bot-side failure.
        """
        failure_tags = [f"ERROR:{error_msg[:200]}"]
        if failure_kind:
            failure_tags.insert(0, f"INFRA:{failure_kind}")
        return {
            "case_id": case_spec.case_id,
            "primary_uc": case_spec.expected.primary_uc,
            "expected_outcome": case_spec.expected.outcome_class,
            "session_id": "",
            "total_turns": 0,
            "stop_reason": "error",
            "elapsed_ms": 0,
            "case_passed": False,
            "composite_score": 0.0,
            "outcome_score": 0.0,
            "judge_score": 0.0,
            "failure_tags": failure_tags,
            "stall_detected": False,
            "stall_failure_tag": "",
            "containment_outcome": "",
            "active_use_case": "",
            "escalation_reason": "",
            "l1_results": [],
            "l2_results": [],
            "l3_results": [],
            "transcript": [],
            "status": "ERROR",
            "llm_calls": [],
            "per_turn_trace": [],
        }

    def _contract_violation_result(
        self,
        case_spec: CaseSpec,
        exc: TraceContractError,
    ) -> dict:
        """Build a deterministic result for a trace-contract violation.

        Mirrors the timeout/error result shape but with a distinct
        ``status`` and a synthetic L1 entry so the cause is visible in
        per-case reports without needing to dig into logs.
        """
        synthetic = HardCheckResult(
            check_name=f"trace_contract_{exc.field}",
            passed=False,
            detail=(
                f"Required telemetry field {exc.field!r} missing/invalid "
                f"in phase {exc.phase!r} (reason={exc.reason}). "
                f"available_keys={exc.available_keys}"
            ),
            severity="critical",
        )
        return {
            "case_id": case_spec.case_id,
            "primary_uc": case_spec.expected.primary_uc,
            "expected_outcome": case_spec.expected.outcome_class,
            "session_id": exc.session_id,
            "total_turns": 0,
            "stop_reason": "contract_violation",
            "elapsed_ms": 0,
            "case_passed": False,
            "composite_score": 0.0,
            "outcome_score": 0.0,
            "judge_score": 0.0,
            "failure_tags": [f"CONTRACT_VIOLATION:{exc.field}"],
            "stall_detected": False,
            "stall_failure_tag": "",
            "containment_outcome": "",
            "active_use_case": "",
            "escalation_reason": "",
            "l1_results": [
                {
                    "check": synthetic.check_name,
                    "passed": synthetic.passed,
                    "detail": synthetic.detail,
                }
            ],
            "l2_results": [],
            "l3_results": [],
            "transcript": [],
            "status": "CONTRACT_VIOLATION",
            "contract_violation": {
                "field": exc.field,
                "phase": exc.phase,
                "reason": exc.reason,
                "available_keys": exc.available_keys,
            },
            "llm_calls": [],
            "per_turn_trace": [],
        }

    # ------------------------------------------------------------------
    # Summary computation
    # ------------------------------------------------------------------

    def _compute_summary(self, case_results: list[dict]) -> dict:
        """Compute aggregate metrics across all cases.

        Returns a dict with overall rates, per-UC breakdown,
        escalation correctness, and policy compliance.
        """
        total = len(case_results)
        if total == 0:
            return {
                "total_cases": 0,
                "passed_cases": 0,
                "failed_cases": 0,
                "task_success_rate": 0.0,
                "stall_rate": 0.0,
                "mean_composite_score": 0.0,
                "mean_outcome_score": 0.0,
                "mean_judge_score": 0.0,
                "per_uc_breakdown": {},
                "escalation_correctness": 0.0,
                "policy_compliance_rate": 0.0,
                "mean_turns_to_resolution": 0.0,
            }

        passed = sum(
            1
            for r in case_results
            if r.get("case_passed") and r.get("composite_score", 0) >= 0.7
        )
        failed = total - passed

        stall_count = sum(1 for r in case_results if r.get("stall_detected"))

        composites = [r.get("composite_score", 0.0) for r in case_results]
        outcomes = [r.get("outcome_score", 0.0) for r in case_results]
        judges = [r.get("judge_score", 0.0) for r in case_results]
        turns = [r.get("total_turns", 0) for r in case_results]

        # Per-UC breakdown
        per_uc: dict[str, dict] = {}
        for r in case_results:
            uc = r.get("primary_uc", "unknown")
            if uc not in per_uc:
                per_uc[uc] = {"count": 0, "passed": 0, "failed": 0, "composites": []}
            per_uc[uc]["count"] += 1
            is_pass = r.get("case_passed") and r.get("composite_score", 0) >= 0.7
            if is_pass:
                per_uc[uc]["passed"] += 1
            else:
                per_uc[uc]["failed"] += 1
            per_uc[uc]["composites"].append(r.get("composite_score", 0.0))

        per_uc_summary = {}
        for uc, data in per_uc.items():
            per_uc_summary[uc] = {
                "count": data["count"],
                "passed": data["passed"],
                "failed": data["failed"],
                "mean_composite": (
                    round(sum(data["composites"]) / len(data["composites"]), 4)
                    if data["composites"]
                    else 0.0
                ),
            }

        # Escalation correctness: for cases where escalation was expected
        # or an escalation actually occurred, check if the decision was correct
        escalation_cases = [
            r
            for r in case_results
            if r.get("expected_outcome") in ("escalate", "resolve")
        ]
        correct_escalations = 0
        for r in escalation_cases:
            expected = r.get("expected_outcome", "")
            actual = r.get("containment_outcome", "").lower()
            actual_mapped = {
                "resolved": "resolve",
                "escalated": "escalate",
            }.get(actual, actual)
            if expected == actual_mapped:
                correct_escalations += 1
        escalation_correctness = (
            correct_escalations / len(escalation_cases)
            if escalation_cases
            else 0.0
        )

        # Policy compliance: cases with zero policy-violation failure tags
        policy_tags = {"L1:no_critical_policy_violation", "L1:no_pii_leakage"}
        compliant = sum(
            1
            for r in case_results
            if not (set(r.get("failure_tags", [])) & policy_tags)
        )
        policy_compliance_rate = compliant / total if total else 0.0

        return {
            "total_cases": total,
            "passed_cases": passed,
            "failed_cases": failed,
            "task_success_rate": round(passed / total, 4) if total else 0.0,
            "stall_rate": round(stall_count / total, 4) if total else 0.0,
            "mean_composite_score": round(sum(composites) / total, 4),
            "mean_outcome_score": round(sum(outcomes) / total, 4),
            "mean_judge_score": round(sum(judges) / total, 4),
            "per_uc_breakdown": per_uc_summary,
            "escalation_correctness": round(escalation_correctness, 4),
            "policy_compliance_rate": round(policy_compliance_rate, 4),
            "mean_turns_to_resolution": (
                round(sum(turns) / total, 2) if total else 0.0
            ),
        }

    # ------------------------------------------------------------------
    # Persistence
    # ------------------------------------------------------------------

    def _save_results(self, result: RunResult) -> None:
        """Write RunResult to JSON and HTML in the configured output directory."""
        output_dir = Path(self._config.report.output_dir) / result.run_id
        output_dir.mkdir(parents=True, exist_ok=True)

        # --- JSON ---
        output_path = output_dir / "results.json"
        payload = {
            "run_id": result.run_id,
            "label": result.label,
            "timestamp": result.timestamp,
            "elapsed_ms": result.elapsed_ms,
            "summary": result.summary,
            "case_results": result.case_results,
        }
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2, ensure_ascii=False, default=str)

        click.echo(f"Results saved to {output_path}")

        # --- HTML report ---
        from eval_interactive.report.html_report import HtmlReportGenerator

        html_gen = HtmlReportGenerator()
        html = html_gen.generate(
            run_id=result.run_id,
            label=result.label,
            case_results=result.case_results,
            summary=result.summary,
        )
        html_path = html_gen.save(html, output_dir / "report.html")
        click.echo(f"HTML report saved to {html_path}")
