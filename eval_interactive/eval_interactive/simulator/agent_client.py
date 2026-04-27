"""HTTP client for the CS Agent under test.

Wraps all API endpoints needed to create sessions, send messages,
and retrieve traces/events/handover logs.
"""

from __future__ import annotations

import httpx


class AgentClient:
    """HTTP client for the CS Agent under test."""

    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")
        self.client = httpx.Client(
            timeout=30.0,
            transport=httpx.HTTPTransport(proxy=None),
        )

    def create_session(self, form_context: dict) -> dict:
        """Create a new chat session with the given form context.

        POST /v1/chat/sessions

        Args:
            form_context: Dict with keys like first_name, email,
                          topic_subject, ad_id, description.

        Returns:
            Dict with session_id and reply_text.
        """
        url = f"{self.base_url}/v1/chat/sessions"
        response = self.client.post(url, json=form_context)
        response.raise_for_status()
        return response.json()

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
