"""Trace collector -- fetches full trace data from CS Agent API endpoints."""

from __future__ import annotations

import logging

from eval_interactive.simulator.agent_client import AgentClient
from eval_interactive.trace.models import (
    EventEntry,
    HandoverData,
    SessionState,
    TraceData,
    TurnTrace,
)

logger = logging.getLogger(__name__)


class TraceCollector:
    """Fetches trace data from CS Agent demo inspection endpoints."""

    def __init__(self, agent_client: AgentClient):
        self.agent_client = agent_client

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
            Fully-populated TraceData instance.
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

        session_state = self._build_session_state(session_id, session_raw)
        turns = self._build_turns(trace_raw)
        events = self._build_events(events_raw)
        handover = self._build_handover(session_id, handover_logs_raw)

        return TraceData(
            session_state=session_state,
            turns=turns,
            events=events,
            handover=handover,
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
        for k in keys:
            v = d.get(k)
            if v is not None:
                return v
        return default

    def _build_session_state(self, session_id: str, raw: dict) -> SessionState:
        g = self._g
        return SessionState(
            session_id=session_id,
            active_use_case=g(raw, "activeUseCase", "active_use_case"),
            candidate_use_cases=g(raw, "candidateUseCases", "candidate_use_cases", default=[]) or [],
            containment_outcome=g(raw, "containmentOutcome", "containment_outcome"),
            escalation_reason=g(raw, "escalationReason", "escalation_reason"),
            total_bot_turns=int(g(raw, "totalBotTurns", "total_bot_turns", default=0)),
            clarification_count=int(g(raw, "clarificationCount", "clarification_count", default=0)),
            faq_miss_count=int(g(raw, "faqMissCount", "faq_miss_count", default=0)),
            form_context=g(raw, "formContext", "form_context", default={}) or {},
            customer_context=g(raw, "customerContext", "customer_context", default={}) or {},
            articles_shown=g(raw, "articlesShown", "articles_shown", default=[]) or [],
            current_phase=g(raw, "currentPhase", "current_phase"),
        )

    def _build_turns(self, raw_list: list[dict]) -> list[TurnTrace]:
        g = self._g
        turns: list[TurnTrace] = []
        for entry in raw_list:
            turns.append(
                TurnTrace(
                    turn_index=int(g(entry, "turnIndex", "turn_index", default=len(turns))),
                    user_message=g(entry, "userMessage", "user_message"),
                    bot_response=g(entry, "botResponse", "bot_response"),
                    action_selected=g(entry, "actionSelected", "action_selected"),
                    tool_calls=g(entry, "toolCalls", "tool_calls", default=[]) or [],
                    source_ids=g(entry, "sourceIds", "source_ids", default=[]) or [],
                    phase_before=g(entry, "phaseBefore", "phase_before"),
                    phase_after=g(entry, "phaseAfter", "phase_after"),
                    active_use_case=g(entry, "activeUseCase", "active_use_case"),
                    latency_ms=int(g(entry, "latencyMs", "latency_ms", default=0)),
                    projected_context=g(entry, "projectedContext", "projected_context", default={}) or {},
                )
            )
        return turns

    def _build_events(self, raw_list: list[dict]) -> list[EventEntry]:
        g = self._g
        events: list[EventEntry] = []
        for entry in raw_list:
            payload = g(entry, "payload", default={}) or {}
            # payload may be a JSON string — parse it
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
