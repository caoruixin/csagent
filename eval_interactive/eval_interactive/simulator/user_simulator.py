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

In addition to your reply, report your CURRENT resolution state for THIS turn
in the ``user_state`` field, judged ONLY from your own point of view as the
customer (never from the bot's wording):
- "satisfied": the bot's latest help actually solved YOUR specific problem.
- "unresolved_after_help": the bot already gave you an answer / advice / help,
  but it did NOT solve your specific problem and you still need help. Only use
  this AFTER the bot has actually tried to help you.
- "working": you are still working through the issue and have not yet decided
  whether it is solved (no judgement yet).
- "new_request": you are now raising a different / new request.
Report what is true for you this turn; do not infer it from whether the bot
offered a human or ended the chat.

Forbidden -- you are the customer, NEVER the support agent. As the customer you:
- do NOT apologize to or for the agent (no "I apologize", "I'm sorry to hear", "thanks for your patience", "thanks for the details");
- do NOT offer to help, check, look into, connect, or escalate ("let me check", "let me look into it", "let me connect you", "let me escalate");
- do NOT cite sources (e.g. "(source: kaXXXXXX)") or write help.gumtree.com URLs;
- do NOT speak for "our support team", "our technical team", or "our team";
- do NOT provide solutions, instructions, or policy statements;
- do NOT repeat the bot's previous turn verbatim;
- do NOT emit the meta-instruction "Based on the conversation above, generate your next customer response as JSON.".

Respond with JSON: {"message": "your response", "goal_status": "in_progress|achieved|impossible", "user_state": "satisfied|unresolved_after_help|working|new_request"}\
""")


# --- Phase-1 measurement contract (S-Auto-40 WP1-A) ---------------------
#
# ``user_state`` is a POSITIVE, structured, per-turn customer resolution
# signal emitted by the simulator IN THE SAME ``generate_next`` call that
# produces the customer turn. It disambiguates the otherwise-overloaded
# ``goal_status="in_progress"`` bucket into a positively-asserted
# "unresolved after the bot already helped" state, distinct from neutral
# "working" and from a "new_request". It is recorded to the trace as-is;
# the downstream conditional-outcome evaluator (scoring/conditional_outcome)
# never back-infers it from outcome / handover / reason / absence-of-signal.
#
# Provenance constants written alongside every persisted signal so a
# consumer can prove the signal came from the legitimate same-call source
# and reject anything else.
USER_STATE_SCHEMA_VERSION = 1
USER_STATE_SIGNAL_SOURCE = "simulator_generate_next"
_USER_STATE_VALUES: tuple[str, ...] = (
    "satisfied",
    "unresolved_after_help",
    "working",
    "new_request",
)


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
    '"goal_status": "in_progress" | "achieved" | "impossible", '
    '"user_state": "satisfied" | "unresolved_after_help" | "working" | "new_request"}'
)

# --- Customer-voice drift guard (S-Auto-21) -----------------------------
#
# The simulator must speak as the customer, never the support agent. Even
# with the corrected role map and the negative-form system-prompt rules, a
# turn can still slip into agent voice. This is a measurement guard: the
# marker set below is the eval-framework-and-simulator audit's first-party
# catalogue of agent-voice markers (declarative measurement), NOT a routing
# semantic on the bot side.

# D1 -- agent-voice keyword markers (case-insensitive substring match).
_DRIFT_KEYWORDS = (
    "i apologize",
    "i'm sorry to hear",
    "thanks for your patience",
    "thanks for the details",
    "let me check",
    "let me look into",
    "let me connect",
    "let me escalate",
    "our support team",
    "our technical team",
    "our team",
    "(source:",
    "https://help.gumtree.com",
)

# D3 -- the simulator's own meta-instruction leaking into the customer turn.
_DRIFT_LEAKAGE_PROBES = (
    "based on the conversation above",
    "generate your next customer response as json",
)

# D2 -- verbatim regurgitation of the prior bot turn.
_REGURGITATION_NGRAM = 8
_REGURGITATION_JACCARD_THRESHOLD = 0.8

# Appended after a drift detection so the next attempt is steered back into
# customer voice before the retry budget is spent.
_DRIFT_RETRY_INSTRUCTION = (
    "Your previous response drifted into the support agent's voice. "
    "Respond as the customer (do NOT apologize, do NOT cite sources, do "
    "NOT propose solutions). Respond with the JSON contract."
)


class SimulatorDriftError(RuntimeError):
    """Raised when the simulator cannot produce a customer-voice turn.

    After ``_MAX_SIMULATOR_ATTEMPTS`` consecutive drift detections the guard
    refuses to emit the drifted turn into the transcript. Raising ends the
    session rather than scoring the bot on a fabricated agent-voice "customer"
    turn. The executor's generic exception handler records this as an errored
    case (``stop_reason="error"``), which is NOT a valid terminal in
    ``hard_checks._VALID_TERMINAL_STOP_REASONS`` and therefore fails
    ``trace_minimum`` rather than vacuous-passing.

    NOTE (S-Auto-21 fence): surfacing the literal
    ``stop_reason="simulator_drift_blocked"`` requires a one-line catch in
    ``session_runner.run_session`` around the ``generate_next`` call (plus an
    executor branch). Both files are outside this sub-sprint's edit fence, so
    the literal stop_reason is left as an open question; this exception carries
    ``detector`` / ``snippet`` / ``attempts`` so that follow-up is mechanical.
    """

    def __init__(self, detector: str, snippet: str, attempts: int):
        self.detector = detector
        self.snippet = snippet
        self.attempts = attempts
        super().__init__(
            f"simulator_drift_blocked: detector={detector} "
            f"attempts={attempts} snippet={snippet!r}"
        )


def _whitespace_tokens(text: str) -> list[str]:
    return (text or "").lower().split()


def _ngram_set(tokens: list[str], n: int) -> set[tuple[str, ...]]:
    if len(tokens) < n:
        return set()
    return {tuple(tokens[i:i + n]) for i in range(len(tokens) - n + 1)}


def _is_regurgitation(message: str, prior_bot_reply: str | None) -> bool:
    """D2: does ``message`` re-emit the prior bot turn near-verbatim?

    Uses 8-gram Jaccard similarity over whitespace tokens. When the prior bot
    turn is shorter than the n-gram width there are no 8-grams to compare, so
    we fall back to a whole-message exact match.
    """
    if not prior_bot_reply:
        return False
    prior_tokens = _whitespace_tokens(prior_bot_reply)
    msg_tokens = _whitespace_tokens(message)
    if len(prior_tokens) < _REGURGITATION_NGRAM:
        return bool(msg_tokens) and msg_tokens == prior_tokens
    prior_grams = _ngram_set(prior_tokens, _REGURGITATION_NGRAM)
    msg_grams = _ngram_set(msg_tokens, _REGURGITATION_NGRAM)
    if not prior_grams or not msg_grams:
        return False
    union = prior_grams | msg_grams
    jaccard = len(prior_grams & msg_grams) / len(union)
    return jaccard >= _REGURGITATION_JACCARD_THRESHOLD


def _detect_customer_voice_drift(
    message: str,
    prior_bot_reply: str | None = None,
) -> str | None:
    """Return the firing detector id (``D1``/``D2``/``D3``) or ``None``.

    D1 keyword hit and D3 leakage probe are case-insensitive substring
    matches; D2 is the regurgitation check above. Order is D3, D1, D2 so the
    most specific signal (the simulator's own meta-instruction) is reported
    first; any single hit is sufficient.
    """
    lowered = (message or "").lower()
    if any(probe in lowered for probe in _DRIFT_LEAKAGE_PROBES):
        return "D3"
    if any(kw in lowered for kw in _DRIFT_KEYWORDS):
        return "D1"
    if _is_regurgitation(message, prior_bot_reply):
        return "D2"
    return None


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
    # Phase-1 (S-Auto-40 WP1-A): pass the model's own ``user_state`` through
    # verbatim. An absent or out-of-vocabulary value becomes ``None`` (the
    # signal was not produced) rather than being coerced to a default —
    # downstream this reads as UNKNOWN, never as a back-inferred state.
    raw_user_state = parsed.get("user_state")
    user_state = raw_user_state if raw_user_state in _USER_STATE_VALUES else None
    return {
        "message": message,
        "goal_status": goal_status,
        "user_state": user_state,
    }


def _parse_simulator_response(raw_text: str) -> dict:
    """Lenient parse: JSON if possible, else fall back to raw text.

    Kept as the final fallback after the retry budget is exhausted so a
    persistently malformed response still yields a usable message.
    """
    parsed = _try_parse_simulator_response(raw_text)
    if parsed is not None:
        return parsed
    logger.warning("Failed to parse simulator JSON, using raw text as message")
    # Lenient fallback: no structured ``user_state`` was recoverable, so it
    # stays None (UNKNOWN downstream) — never back-inferred from the prose.
    return {
        "message": raw_text.strip(),
        "goal_status": "in_progress",
        "user_state": None,
    }


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

        # Include transcript turns. Role map is from the SIMULATOR LLM's point
        # of view: the simulated customer's own prior turns (transcript
        # role="user") are this model's own past output -> "assistant"; the CS
        # agent under test (transcript role="bot") is the other party ->
        # "user". Sending it the other way round (the pre-S-Auto-21 bug)
        # conditioned the simulator to act as the helper from turn ~3 on.
        for turn in transcript:
            role = "assistant" if turn.get("role") == "user" else "user"
            messages.append({"role": role, "content": turn.get("message", "")})

        # The latest bot reply (in case it's not yet in the transcript). The
        # bot is the other party, so it is a "user" message to the simulator.
        if not transcript or transcript[-1].get("role") != "bot":
            messages.append({"role": "user", "content": bot_reply})

        # Per-turn persona re-anchor: position-0 persona is one short
        # instruction against N rounds of conversation; re-state the role and
        # goal immediately before the generation prompt so the customer voice
        # does not erode over a long session.
        messages.append({
            "role": "user",
            "content": (
                "Continuing as the customer (NOT the support agent). Your "
                f"goal: {case_spec.persona.goal_summary}. Stay in customer "
                "voice."
            ),
        })

        # Ask for the next user message
        messages.append({
            "role": "user",
            "content": "Based on the conversation above, generate your next customer response as JSON.",
        })

        return self._call_llm(messages, prior_bot_reply=bot_reply)

    def _call_llm(
        self,
        messages: list[dict],
        prior_bot_reply: str | None = None,
    ) -> dict:
        """Call the LLM, retrying up to ``_MAX_SIMULATOR_ATTEMPTS`` times.

        Retries cover three distinct failures: a transport error (network /
        provider), an unparseable response, and customer-voice drift (the
        parse succeeded but the turn is in support-agent voice). On a parse
        failure the next attempt restates the JSON schema; on drift the next
        attempt restates the customer role. Both share the single attempt
        budget so a turn cannot retry forever.

        ``prior_bot_reply`` (the bot turn this customer message answers) feeds
        the D2 verbatim-regurgitation check; it is ``None`` for the parse-only
        retry callers.

        Args:
            messages: The message list to send.
            prior_bot_reply: The immediately-prior bot turn, for D2.

        Returns:
            Parsed dict with "message" and "goal_status".

        Raises:
            SimulatorDriftError: every attempt drifted into agent voice; the
                guard refuses to emit the drifted turn into the transcript.
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
                if parsed is None:
                    logger.warning(
                        "Simulator response unparseable (attempt %d/%d); "
                        "retrying with schema reminder",
                        attempt + 1, _MAX_SIMULATOR_ATTEMPTS,
                    )
                    working = working + [
                        {"role": "assistant", "content": raw_text},
                        {"role": "user", "content": _PARSE_RETRY_INSTRUCTION},
                    ]
                    continue
                drift = _detect_customer_voice_drift(
                    parsed["message"], prior_bot_reply
                )
                if drift is None:
                    return parsed
                logger.warning(
                    "simulator_drift_detected detector=%s attempt=%d/%d "
                    "snippet=%r; retrying in customer voice",
                    drift, attempt + 1, _MAX_SIMULATOR_ATTEMPTS,
                    parsed["message"][:120],
                )
                working = working + [
                    {"role": "assistant", "content": raw_text},
                    {"role": "user", "content": _DRIFT_RETRY_INSTRUCTION},
                ]
            except Exception as exc:
                last_error = exc
                logger.warning(
                    "LLM call failed (attempt %d/%d): %s",
                    attempt + 1, _MAX_SIMULATOR_ATTEMPTS, exc,
                )

        # Budget exhausted. If we ever got a (malformed-or-drifted) response,
        # re-evaluate it once: a turn that still drifts is BLOCKED (never
        # emitted into the transcript) rather than silently accepted; an
        # unparseable-but-clean turn falls back to the lenient raw-text parse;
        # a pure transport failure yields the canned fallback.
        if last_raw is not None:
            final = _parse_simulator_response(last_raw)
            drift = _detect_customer_voice_drift(final["message"], prior_bot_reply)
            if drift is not None:
                snippet = final["message"][:120]
                logger.error(
                    "simulator_drift_blocked detector=%s after %d attempts "
                    "snippet=%r; ending session (drifted turn NOT accepted)",
                    drift, _MAX_SIMULATOR_ATTEMPTS, snippet,
                )
                raise SimulatorDriftError(drift, snippet, _MAX_SIMULATOR_ATTEMPTS)
            logger.error(
                "Simulator response unparseable after %d attempts; "
                "falling back to raw text",
                _MAX_SIMULATOR_ATTEMPTS,
            )
            return final
        logger.error(
            "LLM call failed after %d attempts: %s",
            _MAX_SIMULATOR_ATTEMPTS, last_error,
        )
        return {
            "message": "I'm still waiting for help with my issue.",
            "goal_status": "in_progress",
            "user_state": None,
        }
