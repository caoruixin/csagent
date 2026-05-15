"""User simulator -- LLM-driven simulated customer.

Uses an OpenAI-compatible LLM to generate realistic customer messages
based on a CaseSpec persona, conversation history, and hidden facts.
"""

from __future__ import annotations

import json
import logging
import re

from jinja2 import Template
from openai import OpenAI

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.config import Config

logger = logging.getLogger(__name__)

_SYSTEM_PROMPT_TEMPLATE = Template("""\
You are a customer contacting Gumtree support.

Your persona:
- Goal: {{ persona.goal_summary }}
- Frustration level: {{ persona.frustration_level }}
- Style: {{ persona.verbosity }}

You submitted a form with:
- Topic: {{ form_context.topic_subject }}
- Description: {{ form_context.description }}

Facts you know (reveal naturally when relevant):
{% for fact in persona.hidden_facts %}
- {{ fact.fact }} (reveal: {{ fact.disclose_when }})
{% endfor %}

{% if persona.will_request_human_if %}
If the bot {{ persona.will_request_human_if }}, ask to speak with a human agent.
{% endif %}

Rules:
- Respond as a real customer would. Do NOT reveal you are an AI.
- If the bot has resolved your issue, say something like "thank you, that helps."
- If the bot is clearly unable to help after multiple attempts, say "can I speak to someone?"
- Keep responses concise (1-3 sentences).

Respond with JSON: {"message": "your response", "goal_status": "in_progress|achieved|impossible"}\
""")


def _parse_simulator_response(raw_text: str) -> dict:
    """Extract JSON from LLM response, handling markdown fences and fallbacks."""
    # Strip markdown code fences if present
    cleaned = raw_text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    cleaned = cleaned.strip()

    try:
        parsed = json.loads(cleaned)
        message = parsed.get("message", "")
        goal_status = parsed.get("goal_status", "in_progress")
        if goal_status not in ("in_progress", "achieved", "impossible"):
            goal_status = "in_progress"
        return {"message": message, "goal_status": goal_status}
    except (json.JSONDecodeError, AttributeError):
        logger.warning("Failed to parse simulator JSON, using raw text as message")
        # Fall back: use the entire text as the message
        return {"message": raw_text.strip(), "goal_status": "in_progress"}


class UserSimulator:
    """Generates simulated customer messages using an LLM.

    Given a CaseSpec (with persona, form_context, hidden facts) and the
    conversation history, produces the next customer message that follows
    the persona's goals, frustration level, and verbosity.
    """

    def __init__(self, config: Config):
        """Initialize the simulator with an OpenAI-compatible client.

        Args:
            config: Application config containing LLM connection details.
        """
        self._client = OpenAI(
            base_url=config.llm.base_url,
            api_key=config.llm.api_key,
        )
        self._model = config.llm.model
        self._temperature = config.llm.simulator_temperature
        logger.info("UserSimulator initialized: model=%s, base_url=%s", self._model, config.llm.base_url)

    def generate_first_message(self, case_spec: CaseSpec) -> dict:
        """Return the first user message for a session.

        If the persona has seed_messages, the first one is used.
        Otherwise the form description is used.

        Args:
            case_spec: The case specification.

        Returns:
            Dict with "message" (str) and "goal_status" ("in_progress").
        """
        if case_spec.persona.seed_messages:
            message = case_spec.persona.seed_messages[0]
        else:
            message = case_spec.form_context.description or case_spec.persona.goal_summary
        return {"message": message, "goal_status": "in_progress"}

    def generate_next(
        self,
        case_spec: CaseSpec,
        transcript: list[dict],
        bot_reply: str,
    ) -> dict:
        """Generate the next user message based on conversation history.

        Calls the LLM with a persona-aware system prompt and the full
        conversation transcript so far.

        Args:
            case_spec: The case specification (persona, form_context, etc.).
            transcript: List of turn dicts [{role, message, turn_index}].
            bot_reply: The latest bot reply text.

        Returns:
            Dict with "message" (str) and
            "goal_status" ("in_progress" | "achieved" | "impossible").
        """
        system_prompt = _SYSTEM_PROMPT_TEMPLATE.render(
            persona=case_spec.persona,
            form_context=case_spec.form_context,
        )

        # Build conversation history for the LLM
        messages: list[dict] = [{"role": "system", "content": system_prompt}]

        # Include transcript turns
        for turn in transcript:
            role = "user" if turn.get("role") == "user" else "assistant"
            messages.append({"role": role, "content": turn.get("message", "")})

        # The latest bot reply (in case it's not yet in the transcript)
        if not transcript or transcript[-1].get("role") != "bot":
            messages.append({"role": "assistant", "content": bot_reply})

        # Ask for the next user message
        messages.append({
            "role": "user",
            "content": "Based on the conversation above, generate your next customer response as JSON.",
        })

        return self._call_llm(messages)

    def _call_llm(self, messages: list[dict]) -> dict:
        """Call the LLM with retry-once on failure.

        Args:
            messages: The message list to send.

        Returns:
            Parsed dict with "message" and "goal_status".
        """
        last_error: Exception | None = None
        logger.info("LLM [simulator] call: model=%s", self._model)
        for attempt in range(2):
            try:
                response = self._client.chat.completions.create(
                    model=self._model,
                    messages=messages,
                    temperature=self._temperature,
                )
                raw_text = response.choices[0].message.content or ""
                return _parse_simulator_response(raw_text)
            except Exception as exc:
                last_error = exc
                logger.warning(
                    "LLM call failed (attempt %d/2): %s", attempt + 1, exc
                )

        # Both attempts failed -- return fallback
        logger.error("LLM call failed after 2 attempts: %s", last_error)
        return {
            "message": "I'm still waiting for help with my issue.",
            "goal_status": "in_progress",
        }
