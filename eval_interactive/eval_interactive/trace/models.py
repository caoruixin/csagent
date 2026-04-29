"""Trace data models for capturing session execution details."""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class TurnTrace:
    """Detailed trace of a single conversation turn."""

    turn_index: int
    user_message: str
    bot_response: str
    action_selected: str
    tool_calls: list[dict]  # [{tool_name, latency_ms, status, result_count, faq_miss}]
    source_ids: list[str]
    phase_before: str
    phase_after: str
    active_use_case: str
    latency_ms: int
    projected_context: dict


@dataclass
class SessionState:
    """Snapshot of session state at the end of a run."""

    session_id: str
    active_use_case: str
    candidate_use_cases: list[str]
    containment_outcome: str  # resolved / escalated / abandoned
    escalation_reason: str
    total_bot_turns: int
    clarification_count: int
    faq_miss_count: int
    form_context: dict
    customer_context: dict
    articles_shown: list[str]
    current_phase: str


@dataclass
class EventEntry:
    """A single event recorded during the session."""

    event_type: str
    turn_index: int
    payload: dict
    created_at: str


@dataclass
class HandoverData:
    """Data captured when a session is handed over to a human agent."""

    log_id: str
    session_id: str
    handover_payload: dict
    customer_message: str
    transcript: list[dict]
    transfer_result: str


@dataclass
class TraceData:
    """Complete trace data for one evaluation session.

    ``contract_warnings`` is populated only when the collector runs in
    ``lenient`` contract mode (see ``trace.collector``). In ``strict``
    mode (default) a contract violation raises ``TraceContractError``
    instead and this list stays empty. Each warning is a one-line
    string of the form ``"trace_contract_<field>: phase=...
    reason=... available_keys=[...]"``.
    """

    session_state: SessionState
    turns: list[TurnTrace]
    events: list[EventEntry]
    handover: HandoverData | None
    contract_warnings: list[str] = field(default_factory=list)
