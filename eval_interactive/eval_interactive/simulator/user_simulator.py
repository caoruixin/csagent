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


# Maximum simulator LLM attempts per turn. A parse failure (the model
# emitted prose or a wrong-shaped object) gets a corrective retry rather
# than a single shot, since the original single retry could repeat the
# same malformed reply.
_MAX_SIMULATOR_ATTEMPTS = 3

# Appended after a parse failure so the next attempt restates the exact
# contract instead of re-emitting the same malformed shape.
_PARSE_RETRY_INSTRUCTION = (
    "Your previous response was malformed and could not be parsed. "
    "Respond with EXACTLY this JSON object and nothing else (no prose, "
    "no markdown fence): "
    '{"message": "<your reply as the customer>", '
    '"goal_status": "in_progress" | "achieved" | "impossible"}'
)


def _try_parse_simulator_response(raw_text: str) -> dict | None:
    """Strict parse: return the dict on success, ``None`` on parse failure.

    Lets the caller distinguish a genuinely malformed response (retry with
    a schema reminder) from a valid one, instead of silently coercing every
    failure into a raw-text message.
    """
    cleaned = raw_text.strip()
    cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned)
    cleaned = re.sub(r"\s*```$", "", cleaned)
    cleaned = cleaned.strip()

    try:
        parsed = json.loads(cleaned)
    except (json.JSONDecodeError, ValueError):
        return None
    if not isinstance(parsed, dict):
        return None
    message = parsed.get("message", "")
    goal_status = parsed.get("goal_status", "in_progress")
    if goal_status not in ("in_progress", "achieved", "impossible"):
        goal_status = "in_progress"
    return {"message": message, "goal_status": goal_status}


def _parse_simulator_response(raw_text: str) -> dict:
    """Lenient parse: JSON if possible, else fall back to raw text.

    Kept as the final fallback after the retry budget is exhausted so a
    persistently malformed response still yields a usable message.
    """
    parsed = _try_parse_simulator_response(raw_text)
    if parsed is not None:
        return parsed
    logger.warning("Failed to parse simulator JSON, using raw text as message")
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
            message = (
                case_spec.form_context.description
                or case_spec.persona.user_goal_summary
                or case_spec.form_context.topic_subject
            )
        # Defensive: turn0 must never be empty. An empty first message gives
        # the bot nothing to route on, so the UC is never stamped and the
        # strict trace contract raises CONTRACT_VIOLATION:active_use_case.
        message = (message or "").strip()
        if not message:
            message = "Hi, I need help with my issue."
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
        """Call the LLM, retrying up to ``_MAX_SIMULATOR_ATTEMPTS`` times.

        Retries cover two distinct failures: a transport error (network /
        provider) and an unparseable response. On a parse failure the next
        attempt appends a corrective instruction restating the JSON schema
        so the model gets a concrete second chance rather than repeating the
        same malformed shape.

        Args:
            messages: The message list to send.

        Returns:
            Parsed dict with "message" and "goal_status".
        """
        working = list(messages)
        last_error: Exception | None = None
        last_raw: str | None = None
        logger.info("LLM [simulator] call: model=%s", self._model)
        for attempt in range(_MAX_SIMULATOR_ATTEMPTS):
            try:
                response = self._client.chat.completions.create(
                    model=self._model,
                    messages=working,
                    temperature=self._temperature,
                )
                raw_text = response.choices[0].message.content or ""
                last_raw = raw_text
                parsed = _try_parse_simulator_response(raw_text)
                if parsed is not None:
                    return parsed
                logger.warning(
                    "Simulator response unparseable (attempt %d/%d); "
                    "retrying with schema reminder",
                    attempt + 1, _MAX_SIMULATOR_ATTEMPTS,
                )
                working = working + [
                    {"role": "assistant", "content": raw_text},
                    {"role": "user", "content": _PARSE_RETRY_INSTRUCTION},
                ]
            except Exception as exc:
                last_error = exc
                logger.warning(
                    "LLM call failed (attempt %d/%d): %s",
                    attempt + 1, _MAX_SIMULATOR_ATTEMPTS, exc,
                )

        # Budget exhausted. If we ever got a (malformed) response, fall back
        # to the lenient parse (raw text as the message) rather than a canned
        # line; only a pure transport failure yields the canned fallback.
        if last_raw is not None:
            logger.error(
                "Simulator response unparseable after %d attempts; "
                "falling back to raw text",
                _MAX_SIMULATOR_ATTEMPTS,
            )
            return _parse_simulator_response(last_raw)
        logger.error(
            "LLM call failed after %d attempts: %s",
            _MAX_SIMULATOR_ATTEMPTS, last_error,
        )
        return {
            "message": "I'm still waiting for help with my issue.",
            "goal_status": "in_progress",
        }
