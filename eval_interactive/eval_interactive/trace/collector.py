"""Trace collector -- fetches full trace data from CS Agent API endpoints.

Wave B1.4: contract validation.

Background: prior to this wave the collector silently coerced missing
telemetry fields (e.g. ``active_use_case``, ``containment_outcome``,
``escalation_reason``) into empty strings. Downstream scorers then read
those empties and produced low scores indistinguishable from real bot
misbehaviour. CRITICAL-1/2/3 in qa-d13-interactive-eval-report.md
documented the camelCase/snake_case half of the problem; this wave adds
the second half: required fields are now a *contract*. Missing values
either raise ``TraceContractError`` (strict mode -- the default) or
record a warning on ``TraceData.contract_warnings`` (lenient mode).

The executor catches ``TraceContractError`` and produces a result with
``status="CONTRACT_VIOLATION"`` so contract failures surface as their
own failure mode in reports rather than being conflated with TIMEOUT or
ERROR. See ``batch/executor.py``.
"""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass, field
from typing import Literal

from eval_interactive.case_spec.schema import ESCALATION_TRIGGER_VALUES
from eval_interactive.simulator.agent_client import AgentClient
from eval_interactive.trace.models import (
    EventEntry,
    HandoverData,
    SessionState,
    TraceData,
    TurnTrace,
)

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Contract enforcement
# ---------------------------------------------------------------------------


ContractMode = Literal["strict", "lenient"]


# Allowed values for ``containment_outcome`` per the design contract.
CONTAINMENT_OUTCOME_VALUES: frozenset[str] = frozenset(
    {"resolved", "escalated", "abandoned", "timeout"}
)


class TraceContractError(Exception):
    """Raised when a required telemetry field is missing or invalid.

    Carries enough structured context that the executor can synthesise a
    deterministic per-case failure result without re-discovering what
    went wrong.

    Attributes:
        field: Name of the missing/invalid field (snake_case).
        phase: Logical phase / stage where the field is required.
              One of: ``"session"``, ``"turn"``, ``"tool_call"``.
        session_id: The session whose trace failed validation.
        available_keys: Keys that *were* present in the offending payload,
                       useful for debugging case-spec / schema drift.
        reason: Optional short tag (e.g. ``"missing"``, ``"empty"``,
                ``"enum_violation"``) for telemetry classification.
    """

    def __init__(
        self,
        *,
        field: str,
        phase: str,
        session_id: str,
        available_keys: list[str] | None = None,
        reason: str = "missing",
        message: str | None = None,
    ) -> None:
        self.field = field
        self.phase = phase
        self.session_id = session_id
        self.available_keys = list(available_keys or [])
        self.reason = reason
        self.message = message or (
            f"Trace contract violation in phase={phase!r}: "
            f"required field {field!r} is {reason} "
            f"(session_id={session_id!r}, available_keys={self.available_keys!r})"
        )
        super().__init__(self.message)


# Required-field contract per phase. Kept as module-level constants so
# they can be imported by tests and report layers.
#
#   session             -- fields the bot populates from session creation
#                          onward (always required)
#   conditional session -- fields the bot only populates once the session
#                          has progressed (post-routing / post-termination).
#                          Enforced by ``_enforce_conditional_session_contracts``
#                          using turn count + current_phase + handover presence.
#   turn                -- fields populated for every assistant turn
#   tool_call           -- fields populated for every recorded tool call

REQUIRED_SESSION_FIELDS: tuple[str, ...] = (
    "current_phase",
)

# Fields that are only required once a precondition holds:
#   active_use_case      -> required after at least one bot turn (routing
#                           happens on the first user message; pre-routing
#                           the field is legitimately empty).
#   containment_outcome  -> required only when the session has actually
#                           terminated (current_phase in TERMINAL_PHASES or
#                           a handover record was produced). Mid-conversation
#                           sessions legitimately have no outcome yet.
CONDITIONAL_SESSION_FIELDS: tuple[str, ...] = (
    "active_use_case",
    "containment_outcome",
)

# Phase values that indicate the session has reached a terminal state.
# Keep in sync with server-side ControlKernel / SessionManager phase
# transitions (see ControlKernel.java).
TERMINAL_PHASES: frozenset[str] = frozenset({"CLOSE", "ESCALATE"})

# Phase values in which the session has NOT yet committed to a use case.
# The server's UseCaseRouter sets active_use_case on *exit* from the
# discovery/triage phase, and discovery can span several bot turns during
# which active_use_case is legitimately empty -- the server documents this
# directly (PhaseEvaluator.java: ".useCase(activeUc) // may be null in
# DISCOVER while still discovering"). A session captured mid-DISCOVER
# (e.g. a turn-1 latency/timeout or an early-terminated conversation) thus
# has >=1 recorded turn but a telemetry-faithful empty active_use_case, and
# must NOT raise a contract violation. Turn-count alone is the wrong proxy
# for "routing has happened"; the phase is. Keep in sync with server-side
# ControlKernel / PhaseEvaluator phase names.
PRE_ROUTING_PHASES: frozenset[str] = frozenset({"INIT", "DISCOVER"})

REQUIRED_TURN_FIELDS: tuple[str, ...] = (
    "phase_after",
)

REQUIRED_TOOL_CALL_FIELDS: tuple[str, ...] = (
    "tool_name",
    "arguments",
)

# Per-tool extra requirements. The arguments dict is inspected for the
# named keys; an empty string / missing key is a violation.
TOOL_SPECIFIC_REQUIRED_ARGS: dict[str, tuple[str, ...]] = {
    "request_handover": ("escalation_reason",),
    "record_outcome": ("outcome_class",),
}


# ---------------------------------------------------------------------------
# Collector
# ---------------------------------------------------------------------------


class TraceCollector:
    """Fetches trace data from CS Agent demo inspection endpoints.

    Args:
        agent_client: HTTP client wrapping the CS Agent demo endpoints.
        contract_mode: How to react to missing required fields.
            * ``"strict"`` (default) -- raise ``TraceContractError``
              immediately. The executor catches this and produces a
              ``CONTRACT_VIOLATION`` result.
            * ``"lenient"`` -- coerce to the legacy default value (empty
              string / 0 / []) but record a human-readable warning on
              ``TraceData.contract_warnings``.
    """

    def __init__(
        self,
        agent_client: AgentClient,
        contract_mode: ContractMode = "strict",
    ):
        if contract_mode not in ("strict", "lenient"):
            raise ValueError(
                f"contract_mode must be 'strict' or 'lenient', got {contract_mode!r}"
            )
        self.agent_client = agent_client
        self.contract_mode = contract_mode

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def collect(self, session_id: str) -> TraceData:
        """Fetch all trace data for a completed session.

        Calls four endpoints via the AgentClient:
        1. GET /v1/chat/sessions/{id}          -> session state
        2. GET /v1/demo/sessions/{id}/trace     -> per-turn trace
        3. GET /v1/demo/sessions/{id}/events    -> event timeline
        4. GET /v1/demo/handover-logs           -> handover payloads

        Returns:
            Fully-populated TraceData instance. In ``lenient`` mode,
            ``TraceData.contract_warnings`` may be non-empty.

        Raises:
            TraceContractError: in ``strict`` mode when a required
                telemetry field is missing or invalid.
        """
        session_raw = self._safe_call(
            lambda: self.agent_client.get_session(session_id),
            "get_session",
            default={},
        )
        trace_raw = self._safe_call(
            lambda: self.agent_client.get_trace(session_id),
            "get_trace",
            default=[],
        )
        events_raw = self._safe_call(
            lambda: self.agent_client.get_events(session_id),
            "get_events",
            default=[],
        )
        handover_logs_raw = self._safe_call(
            lambda: self.agent_client.get_handover_logs(),
            "get_handover_logs",
            default=[],
        )

        warnings: list[str] = []

        session_state = self._build_session_state(
            session_id, session_raw, warnings
        )
        turns = self._build_turns(session_id, trace_raw, warnings)
        events = self._build_events(events_raw)
        handover = self._build_handover(session_id, handover_logs_raw)

        # Conditional session-state checks run last so they can use
        # turn count + handover presence to decide whether a missing
        # field is a real violation or legitimately not-yet-set.
        self._enforce_conditional_session_contracts(
            session_state=session_state,
            turns=turns,
            handover=handover,
            session_raw=session_raw,
            warnings=warnings,
        )

        return TraceData(
            session_state=session_state,
            turns=turns,
            events=events,
            handover=handover,
            contract_warnings=warnings,
        )

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _safe_call(fn, label: str, default):
        """Call *fn*, returning *default* on any exception."""
        try:
            return fn()
        except Exception:
            logger.warning("TraceCollector: %s call failed, using default", label, exc_info=True)
            return default

    @staticmethod
    def _g(d: dict, *keys, default=""):
        """Get first non-None value from dict using multiple key variants (camelCase/snake_case)."""
        if not isinstance(d, dict):
            return default
        for k in keys:
            v = d.get(k)
            if v is not None:
                return v
        return default

    # ------------------------------------------------------------------
    # Contract enforcement helpers
    # ------------------------------------------------------------------

    def _violate(
        self,
        *,
        field: str,
        phase: str,
        session_id: str,
        raw: dict | None,
        reason: str,
        warnings: list[str],
    ) -> None:
        """Either raise (strict) or log a warning (lenient)."""
        available = sorted((raw or {}).keys()) if isinstance(raw, dict) else []
        msg = (
            f"trace_contract_{field}: phase={phase} reason={reason} "
            f"available_keys={available}"
        )
        if self.contract_mode == "strict":
            raise TraceContractError(
                field=field,
                phase=phase,
                session_id=session_id,
                available_keys=available,
                reason=reason,
            )
        # lenient: record and continue with the legacy default
        warnings.append(msg)
        logger.info("Trace contract warning (lenient mode): %s", msg)

    def _require_session_field(
        self,
        *,
        field: str,
        value,
        session_id: str,
        raw: dict,
        warnings: list[str],
        allowed: frozenset[str] | None = None,
    ) -> str:
        """Validate a required session-state string field.

        Returns the value if valid, or "" (lenient) after recording a
        warning. Raises TraceContractError in strict mode.
        """
        if value is None or value == "":
            self._violate(
                field=field,
                phase="session",
                session_id=session_id,
                raw=raw,
                reason="missing",
                warnings=warnings,
            )
            return ""
        if allowed is not None and value not in allowed:
            self._violate(
                field=field,
                phase="session",
                session_id=session_id,
                raw=raw,
                reason=f"enum_violation:value={value!r}_not_in_{sorted(allowed)!r}",
                warnings=warnings,
            )
            return ""
        return value

    def _enforce_conditional_session_contracts(
        self,
        *,
        session_state: SessionState,
        turns: list[TurnTrace],
        handover: HandoverData | None,
        session_raw: dict,
        warnings: list[str],
    ) -> None:
        """Validate session-state fields whose requirement depends on
        whether the session has actually progressed.

        - ``active_use_case`` is set when the session routes, which happens
          on *exit* from the pre-routing phases (``INIT`` / ``DISCOVER``).
          Discovery can span several bot turns, so a session still in a
          pre-routing phase legitimately has no UC even after one or more
          turns (see ``PRE_ROUTING_PHASES``) -- turn-count alone is the
          wrong proxy for "routing has happened". A session that ended
          before routing therefore has no UC, and that is not a contract
          violation. Out-of-scope escalations (escalation_reason ==
          "out_of_scope") likewise legitimately have no UC even when a
          synthetic handover-evidence turn was persisted by the server's
          SessionManager hard-OOS path.
        - ``containment_outcome`` is set when the session terminates
          (CLOSE / ESCALATE) or a handover is recorded. Mid-conversation
          sessions legitimately have an empty value.
        """
        is_oos_escalation = (
            session_state.containment_outcome == "escalated"
            and session_state.escalation_reason == "out_of_scope"
        )
        # active_use_case is only required once the session has progressed
        # past the pre-routing phases. A session still in INIT/DISCOVER has
        # legitimately not routed yet even after one or more bot turns
        # (see PRE_ROUTING_PHASES), so an empty active_use_case there is
        # telemetry-faithful, not a contract violation. The genuine contract
        # is preserved for every routed phase (CONFIRM / RESOLVE / CLOSE /
        # non-out-of-scope ESCALATE).
        in_pre_routing_phase = session_state.current_phase in PRE_ROUTING_PHASES
        if (
            turns
            and not session_state.active_use_case
            and not is_oos_escalation
            and not in_pre_routing_phase
        ):
            self._violate(
                field="active_use_case",
                phase="session",
                session_id=session_state.session_id,
                raw=session_raw,
                reason="missing_after_turns",
                warnings=warnings,
            )

        is_terminal = (
            session_state.current_phase in TERMINAL_PHASES
            or handover is not None
        )
        if is_terminal:
            outcome = session_state.containment_outcome
            if not outcome:
                self._violate(
                    field="containment_outcome",
                    phase="session",
                    session_id=session_state.session_id,
                    raw=session_raw,
                    reason="missing_at_terminal_phase",
                    warnings=warnings,
                )
            elif outcome not in CONTAINMENT_OUTCOME_VALUES:
                self._violate(
                    field="containment_outcome",
                    phase="session",
                    session_id=session_state.session_id,
                    raw=session_raw,
                    reason=(
                        f"enum_violation:value={outcome!r}_not_in_"
                        f"{sorted(CONTAINMENT_OUTCOME_VALUES)!r}"
                    ),
                    warnings=warnings,
                )

    def _build_session_state(
        self,
        session_id: str,
        raw: dict,
        warnings: list[str],
    ) -> SessionState:
        g = self._g

        # active_use_case and containment_outcome are read unconditionally
        # here; they are validated later by _enforce_conditional_session_contracts
        # which has access to turn count and terminal-phase signals.
        active_uc = g(raw, "activeUseCase", "active_use_case") or ""
        containment = g(raw, "containmentOutcome", "containment_outcome") or ""

        current_phase = g(raw, "currentPhase", "current_phase")
        current_phase = self._require_session_field(
            field="current_phase",
            value=current_phase,
            session_id=session_id,
            raw=raw,
            warnings=warnings,
        )

        # form_context / customer_context arrive as JSONB-encoded strings
        # from the server (BotSession entity persists them as String with
        # @Column(columnDefinition="jsonb")). Parse here so downstream
        # consumers see proper dicts. Forward-compatible with a future
        # server-side DTO fix that returns objects directly.
        form_context_raw = g(raw, "formContext", "form_context", default={})
        form_context = self._parse_jsonb_field(
            raw=form_context_raw,
            field_name="form_context",
            session_id=session_id,
            fallback={},
        )
        customer_context_raw = g(raw, "customerContext", "customer_context", default={})
        customer_context = self._parse_jsonb_field(
            raw=customer_context_raw,
            field_name="customer_context",
            session_id=session_id,
            fallback={},
        )

        return SessionState(
            session_id=session_id,
            active_use_case=active_uc,
            candidate_use_cases=g(raw, "candidateUseCases", "candidate_use_cases", default=[]) or [],
            containment_outcome=containment,
            escalation_reason=g(raw, "escalationReason", "escalation_reason"),
            total_bot_turns=int(g(raw, "totalBotTurns", "total_bot_turns", default=0)),
            clarification_count=int(g(raw, "clarificationCount", "clarification_count", default=0)),
            faq_miss_count=int(g(raw, "faqMissCount", "faq_miss_count", default=0)),
            form_context=form_context,
            customer_context=customer_context,
            articles_shown=g(raw, "articlesShown", "articles_shown", default=[]) or [],
            current_phase=current_phase,
        )

    def _parse_jsonb_field(
        self,
        *,
        raw,
        field_name: str,
        session_id: str,
        fallback,
    ):
        """Parse a JSONB column that the server returns as a JSON-encoded
        string (BotTurn / BotSession persist these as Java String with
        @Column(columnDefinition="jsonb")).

        Forward-compatible: also accepts already-decoded list/dict so a
        future server-side DTO fix won't require a harness change.
        """
        if isinstance(raw, str):
            try:
                return json.loads(raw)
            except (json.JSONDecodeError, TypeError):
                logger.warning(
                    "TraceCollector: failed to json.loads %s for session=%s; "
                    "falling back to %r",
                    field_name,
                    session_id,
                    fallback,
                )
                return fallback
        if isinstance(raw, (list, dict)):
            return raw
        return fallback

    def _build_turns(
        self,
        session_id: str,
        raw_list: list[dict],
        warnings: list[str],
    ) -> list[TurnTrace]:
        g = self._g
        turns: list[TurnTrace] = []
        for entry in raw_list:
            phase_after = g(entry, "phaseAfter", "phase_after")
            if phase_after is None or phase_after == "":
                self._violate(
                    field="phase_after",
                    phase="turn",
                    session_id=session_id,
                    raw=entry,
                    reason="missing",
                    warnings=warnings,
                )
                phase_after = ""

            # tool_calls / projected_context arrive as JSONB-encoded
            # strings from the server. Parse before downstream use so
            # _validate_tool_calls (isinstance(list) check) and scorers
            # see real Python objects.
            tool_calls_raw = self._parse_jsonb_field(
                raw=g(entry, "toolCalls", "tool_calls", default=[]),
                field_name="tool_calls",
                session_id=session_id,
                fallback=[],
            )
            tool_calls = self._validate_tool_calls(
                session_id=session_id,
                tool_calls_raw=tool_calls_raw,
                warnings=warnings,
            )

            projected_context = self._parse_jsonb_field(
                raw=g(entry, "projectedContext", "projected_context", default={}),
                field_name="projected_context",
                session_id=session_id,
                fallback={},
            )

            turns.append(
                TurnTrace(
                    turn_index=int(g(entry, "turnIndex", "turn_index", default=len(turns))),
                    user_message=g(entry, "userMessage", "user_message"),
                    bot_response=g(entry, "botResponse", "bot_response"),
                    tool_calls=tool_calls,
                    source_ids=g(entry, "sourceIds", "source_ids", default=[]) or [],
                    phase_before=g(entry, "phaseBefore", "phase_before"),
                    phase_after=phase_after,
                    active_use_case=g(entry, "activeUseCase", "active_use_case"),
                    latency_ms=int(g(entry, "latencyMs", "latency_ms", default=0)),
                    projected_context=projected_context,
                )
            )
        return turns

    def _validate_tool_calls(
        self,
        *,
        session_id: str,
        tool_calls_raw: list,
        warnings: list[str],
    ) -> list[dict]:
        """Validate tool-call telemetry; coerce to canonical snake_case dicts.

        Required for every tool call: ``tool_name`` and ``arguments``
        (the key must exist; an empty dict is fine). Tool-specific
        requirements live in ``TOOL_SPECIFIC_REQUIRED_ARGS``.
        """
        g = self._g
        validated: list[dict] = []
        if not isinstance(tool_calls_raw, list):
            return validated

        for tc in tool_calls_raw:
            if not isinstance(tc, dict):
                # We can't validate a non-dict tool call -- treat as
                # malformed at the structural level.
                self._violate(
                    field="tool_call_shape",
                    phase="tool_call",
                    session_id=session_id,
                    raw={},
                    reason=f"non_dict:type={type(tc).__name__}",
                    warnings=warnings,
                )
                continue

            tool_name = g(tc, "toolName", "tool_name")
            if tool_name is None or tool_name == "":
                self._violate(
                    field="tool_name",
                    phase="tool_call",
                    session_id=session_id,
                    raw=tc,
                    reason="missing",
                    warnings=warnings,
                )
                tool_name = ""

            # ``arguments`` -- the *key* must exist (we accept {} as a
            # valid empty value, but a totally absent key is a
            # contract violation).
            has_args = "arguments" in tc or "args" in tc
            if not has_args:
                self._violate(
                    field="arguments",
                    phase="tool_call",
                    session_id=session_id,
                    raw=tc,
                    reason="missing",
                    warnings=warnings,
                )
                arguments: dict = {}
            else:
                arguments = g(tc, "arguments", "args", default={}) or {}
                if not isinstance(arguments, dict):
                    arguments = {"_raw": arguments}

            # Tool-specific extra arg requirements.
            extra_required = TOOL_SPECIFIC_REQUIRED_ARGS.get(tool_name, ())
            for arg_name in extra_required:
                arg_value = arguments.get(arg_name) if isinstance(arguments, dict) else None
                if arg_value is None or arg_value == "":
                    self._violate(
                        field=arg_name,
                        phase="tool_call",
                        session_id=session_id,
                        raw=tc,
                        reason=f"missing_for_tool:{tool_name}",
                        warnings=warnings,
                    )
                elif arg_name == "escalation_reason" and tool_name == "request_handover":
                    if arg_value not in ESCALATION_TRIGGER_VALUES:
                        self._violate(
                            field="escalation_reason",
                            phase="tool_call",
                            session_id=session_id,
                            raw=tc,
                            reason=f"enum_violation:value={arg_value!r}",
                            warnings=warnings,
                        )

            # Carry the rest of the tool-call payload through unchanged
            # so downstream consumers (hard_checks etc.) keep working.
            normalised = dict(tc)
            normalised["tool_name"] = tool_name
            normalised["arguments"] = arguments
            validated.append(normalised)

        return validated

    def _build_events(self, raw_list: list[dict]) -> list[EventEntry]:
        g = self._g
        events: list[EventEntry] = []
        for entry in raw_list:
            payload = g(entry, "payload", default={}) or {}
            # payload may be a JSON string -- parse it
            if isinstance(payload, str):
                import json as _json
                try:
                    payload = _json.loads(payload)
                except (ValueError, TypeError):
                    payload = {"raw": payload}
            events.append(
                EventEntry(
                    event_type=g(entry, "eventType", "event_type"),
                    turn_index=int(g(entry, "turnIndex", "turn_index", default=0)),
                    payload=payload,
                    created_at=g(entry, "createdAt", "created_at"),
                )
            )
        return events

    def _build_handover(self, session_id: str, logs: list[dict]) -> HandoverData | None:
        """Find the handover log matching *session_id*, if any."""
        g = self._g
        import json as _json
        for log in logs:
            raw_payload = g(log, "handoverPayload", "handover_payload", default=None)
            # handover_payload may be a JSON string
            if isinstance(raw_payload, str):
                try:
                    payload = _json.loads(raw_payload)
                except (ValueError, TypeError):
                    payload = {}
            elif raw_payload is not None:
                payload = raw_payload
            else:
                payload = {}

            log_session = (
                g(log, "sessionId", "session_id")
                or payload.get("session_id", "")
            )
            if log_session == session_id:
                transcript = g(log, "transcript", default=[]) or []
                if isinstance(transcript, str):
                    try:
                        transcript = _json.loads(transcript)
                    except (ValueError, TypeError):
                        transcript = []
                return HandoverData(
                    log_id=g(log, "logId", "log_id"),
                    session_id=session_id,
                    handover_payload=payload,
                    customer_message=g(log, "customerMessage", "customer_message"),
                    transcript=transcript,
                    transfer_result=g(log, "transferResult", "transfer_result"),
                )
        return None
