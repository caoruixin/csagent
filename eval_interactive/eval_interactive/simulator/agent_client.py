"""HTTP client for the CS Agent under test.

Wraps all API endpoints needed to create sessions, send messages,
and retrieve traces/events/handover logs.
"""

from __future__ import annotations

import logging
import time

import httpx


logger = logging.getLogger(__name__)


# Sprint 6 §G0: widen the create-session read timeout to 120s. The Kimi
# auto-search path during session-create chains 4-6 LLM calls (8-15s
# each); under nominal latency the whole path fits inside the original
# 60s budget, but a single slow Kimi call routinely pushes total
# session-create above 60s. A 120s ceiling keeps the bounded-retry
# semantics intact while letting nominal sessions through. The
# `transient_error_max_retries=1` accept-and-retry below covers the
# residual ReadTimeout cases without unbounded retry.
SESSION_CREATE_READ_TIMEOUT_SECONDS = 120.0
SESSION_CREATE_CONNECT_TIMEOUT_SECONDS = 10.0
DEFAULT_READ_TIMEOUT_SECONDS = 60.0
DEFAULT_CONNECT_TIMEOUT_SECONDS = 10.0


class AgentClient:
    """HTTP client for the CS Agent under test."""

    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        # The default per-request timeout covers send_message / get_trace /
        # get_session etc. — these endpoints don't run the auto-search path
        # and 60s is plenty.
        self.client = httpx.Client(
            timeout=httpx.Timeout(
                DEFAULT_READ_TIMEOUT_SECONDS,
                connect=DEFAULT_CONNECT_TIMEOUT_SECONDS,
            ),
            transport=httpx.HTTPTransport(proxy=None),
        )

    def create_session(self, form_context: dict) -> dict:
        """Create a new chat session with the given form context.

        POST /v1/chat/sessions

        Sprint 6 §G0: uses a wider 120s read timeout for session-create
        and accepts-and-retries exactly once on httpx.ReadTimeout. All
        other transport / HTTP failures (incl. 4xx / 5xx) propagate
        immediately so semantic failures stay non-retryable. Auth
        failures (401 / 403) are surfaced as raise_for_status without
        any retry.

        Args:
            form_context: Dict with keys like first_name, email,
                          topic_subject, ad_id, description.

        Returns:
            Dict with session_id and reply_text.
        """
        url = f"{self.base_url}/v1/chat/sessions"
        per_request_timeout = httpx.Timeout(
            SESSION_CREATE_READ_TIMEOUT_SECONDS,
            connect=SESSION_CREATE_CONNECT_TIMEOUT_SECONDS,
        )
        attempts = 0
        max_attempts = 2  # 1 initial + 1 retry on ReadTimeout
        last_read_timeout: httpx.ReadTimeout | None = None
        while attempts < max_attempts:
            attempts += 1
            try:
                response = self.client.post(
                    url, json=form_context, timeout=per_request_timeout
                )
            except httpx.ReadTimeout as exc:
                # Tag the ReadTimeout so SessionResult.creation_error /
                # the executor's failure-tag plumbing can distinguish a
                # session-create read timeout from a semantic failure
                # (4xx / 5xx). The exception text already starts with
                # "timed out"; we re-emit it as a ReadTimeout to keep
                # repr() classifiable downstream.
                logger.warning(
                    "create_session ReadTimeout on attempt %d/%d (read=%.1fs)",
                    attempts,
                    max_attempts,
                    SESSION_CREATE_READ_TIMEOUT_SECONDS,
                )
                last_read_timeout = exc
                if attempts >= max_attempts:
                    raise
                # Brief pause before retry so a backend that just slow-pathed
                # the auto-search has a moment to settle.
                time.sleep(1.0)
                continue
            response.raise_for_status()
            return response.json()
        # Defensive — only reachable if the loop exhausts without raising.
        if last_read_timeout is not None:
            raise last_read_timeout
        raise RuntimeError("create_session retry loop exited without a response")

    def send_message(self, session_id: str, message: str) -> dict:
        """Send a user message to an existing session.

        POST /v1/chat/sessions/{id}/messages

        Args:
            session_id: The session identifier.
            message: User message text.

        Returns:
            Dict with reply_text, intent, should_end_chat, additional_data.
        """
        url = f"{self.base_url}/v1/chat/sessions/{session_id}/messages"
        response = self.client.post(url, json={"message": message})
        response.raise_for_status()
        return response.json()

    def get_session(self, session_id: str) -> dict:
        """Retrieve full session state.

        GET /v1/chat/sessions/{id}

        Args:
            session_id: The session identifier.

        Returns:
            Full session state dict.
        """
        url = f"{self.base_url}/v1/chat/sessions/{session_id}"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()

    def get_trace(self, session_id: str) -> list[dict]:
        """Retrieve the trace for a session.

        GET /v1/demo/sessions/{id}/trace

        Args:
            session_id: The session identifier.

        Returns:
            List of trace entry dicts.
        """
        url = f"{self.base_url}/v1/demo/sessions/{session_id}/trace"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()

    def get_events(self, session_id: str) -> list[dict]:
        """Retrieve events for a session.

        GET /v1/demo/sessions/{id}/events

        Args:
            session_id: The session identifier.

        Returns:
            List of event dicts.
        """
        url = f"{self.base_url}/v1/demo/sessions/{session_id}/events"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()

    def get_handover_logs(self) -> list[dict]:
        """Retrieve all handover logs.

        GET /v1/demo/handover-logs

        Returns:
            List of handover log dicts.
        """
        url = f"{self.base_url}/v1/demo/handover-logs"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()

    def close(self) -> None:
        """Close the underlying HTTP client."""
        self.client.close()

    def __enter__(self) -> "AgentClient":
        return self

    def __exit__(self, *args: object) -> None:
        self.close()
