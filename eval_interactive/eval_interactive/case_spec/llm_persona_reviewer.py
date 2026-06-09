"""Wave A6 LLM persona reviewer (production module).

This module is the canonical home for the L2 persona-review prompt,
validators, and DeepSeek client. The shadow-audit script
(``eval_interactive/scripts/llm_review_specs.py``) imports the same
constants so there is exactly one definition of:

* ``PROMPT_SYSTEM`` -- the system prompt locked by Option A (length-cap
  guidance is part of rule 6; sha256 surfaced in the dispatch report).
* ``render_user_prompt`` -- the per-session user-prompt body (the same
  template the shadow audit used).
* ``ALLOWED_RESPONSE_KEYS`` and ``BOT_EXPECTATION_PATTERNS``.
* The validators (``_verbatim_in_visitor``, ``_word_overlap``,
  ``_shared_identifier``, ``validate_response``).

The production extractor uses :class:`LlmPersonaReviewer` to produce a
``ReviewResult`` for one ``(source_session_id, prompt_hash)`` and
optionally writes the result to the committed cache directory at
``eval_interactive/case_spec_llm_cache/<source_session_id>.yaml``.

Trust boundary recap (see ``docs/interactive_case_spec_generation_plan.md``):

* L1 = deterministic Python.
* L2 = this module. May only fill ``persona.{seed_messages,
  user_goal_summary, hidden_facts, verbosity}``. ``verbosity`` is
  recomputed by the rule extractor's ``_derive_verbosity`` from
  ``accepted_value.seed_messages``; the LLM never proposes verbosity.
* L3 = ``case_spec_overrides.yaml`` (Wave A5). Always applied AFTER L2.
"""

from __future__ import annotations

import datetime as _dt
import hashlib
import json
import logging
import os
import random
import re
import time
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any, Iterable

import httpx
import yaml

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# DeepSeek transport
# ---------------------------------------------------------------------------

DEEPSEEK_ENDPOINT = (
    os.environ.get("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1") + "/chat/completions"
)
DEFAULT_DEEPSEEK_MODEL = os.environ.get("DEEPSEEK_MODEL", "deepseek-v4-flash")

# Mirrors extractor.SOURCE_DATASET_TURNS_FILE so the shadow-audit script can
# share this map without importing the production extractor.
SOURCE_DATASET_TURNS_FILE: dict[str, str] = {
    "golden": "golden_turns.csv",
    "escalation": "escalation_turns.csv",
    "badcase": "badcase_turns.csv",
    "handover": "handover_turns.csv",
    "clarification": "clarification_turns.csv",
    "drift_control": "drift_control_turns.csv",
    "intake_tool_contract": "intake_tool_contract_turns.csv",
}

# ---------------------------------------------------------------------------
# Prompt template (Option A locked: rule 6 enforces 280-char cap)
# ---------------------------------------------------------------------------

PROMPT_SYSTEM = """\
You are a CaseSpec persona reviewer for a customer-service evaluation harness.

Your job: improve the persona free-text fields of one CaseSpec, given the
rule-based draft and the original human transcript. You DO NOT make any
policy, outcome, UC, tool, or scoring decision. The persona drives a user
simulator; it is not a graders' answer key.

You may propose values ONLY for these keys:
  - seed_messages         (1-3 strings)
  - user_goal_summary     (string, plain English, MAXIMUM 280 CHARACTERS — count carefully before responding)
  - hidden_facts          (list of {fact, disclose_when})

You MUST NOT include any of these keys in your response:
  outcome_class, should_escalate, escalation_trigger, primary_uc,
  secondary_ucs, expected_tool_sequence, forbidden_tools, grounding_mode,
  bot_handling_pattern, allow_bot_resolution, risk_level, max_turns,
  answer_must_not_contain, scoring, form_context, frustration_level,
  drift_behavior, will_request_human_if, verbosity.

Hard rules:
  1. Each seed_messages[i] MUST appear verbatim (case-insensitive,
     whitespace-collapsed) as a substring of one VISITOR turn in the
     supplied transcript. Do not paraphrase, do not invent, do not merge
     two turns into one quote.
  2. Pick visitor turns that describe the actual issue, not greetings,
     thanks, sign-offs, or single-word acknowledgements. If only such
     turns exist, return your best 1-2 issue-bearing visitor lines.
  3. user_goal_summary describes ONLY what the USER wants. Never write
     "the bot should...", "expects bot to escalate", "expects bot to
     resolve", or any other bot-side instruction. Keep it factual.
  4. hidden_facts must be grounded in the transcript. Do not invent
     identifiers, prices, dates, or account states. If unsure, omit.
  5. If the transcript does not give you enough signal to confidently
     improve the rule draft, set llm_confidence=low and return the rule
     draft values unchanged.
  6. user_goal_summary MUST be 280 characters or fewer. Count characters before you submit. If your draft is longer, shorten it before responding.

Return strict JSON with this shape and no extra keys:

{
  "seed_messages": [string, ...],
  "user_goal_summary": string,
  "hidden_facts": [{"fact": string, "disclose_when": string}, ...],
  "llm_confidence": "low" | "medium" | "high",
  "llm_rationale": string
}
"""

PROMPT_TEMPLATE_SHA256 = hashlib.sha256(PROMPT_SYSTEM.encode("utf-8")).hexdigest()


ALLOWED_RESPONSE_KEYS = {
    "seed_messages",
    "user_goal_summary",
    "hidden_facts",
    "llm_confidence",
    "llm_rationale",
}

BOT_EXPECTATION_PATTERNS = [
    re.compile(r"the bot should", re.IGNORECASE),
    re.compile(r"expects bot", re.IGNORECASE),
    re.compile(r"the agent should", re.IGNORECASE),
    re.compile(r"bot must", re.IGNORECASE),
    re.compile(r"agent must", re.IGNORECASE),
]

USER_GOAL_SUMMARY_MAX_CHARS = 280

DEFAULT_HTTP_TIMEOUT_S = 90.0
DEFAULT_MAX_RETRIES = 3


# ---------------------------------------------------------------------------
# User-prompt rendering
# ---------------------------------------------------------------------------


def render_user_prompt(
    *,
    source_session_id: str,
    source_dataset: str,
    primary_uc: str,
    secondary_ucs: list[str] | None,
    drift_type: str,
    has_frustration: bool,
    frustration_type: str,
    topic_subject: str,
    description: str,
    rule_draft: "PersonaDraft",
    turns: list[dict[str, Any]],
    turns_filename: str,
) -> str:
    """Render the per-session user prompt body.

    The function is parameterised so the production extractor (which
    builds inputs from HR rows) and the shadow-audit script (which reads
    them from spec YAMLs) can share one renderer.

    Wave A6.6: ``case_id`` was removed from the rendered body. ``case_id``
    is positional (rebuilt from the HR CSV row index) so any HR-row insert
    or delete used to invalidate every downstream ``prompt_hash``. The
    LLM never used ``case_id`` for persona judgement; dropping it makes
    the prompt — and therefore the cache key — invariant under
    HR-row reordering.
    """
    secondary = list(secondary_ucs or [])
    secondary_str = ", ".join(secondary) if secondary else "(none)"

    drift = (drift_type or "none").strip() or "none"
    frustration_label = (frustration_type or "none").strip() or "none"

    rule_seeds = rule_draft.seed_messages
    rule_user_goal = rule_draft.user_goal_summary or ""
    rule_hidden = rule_draft.hidden_facts

    seed_block_lines: list[str] = []
    for s in rule_seeds:
        seed_block_lines.append(f'    - "{s}"')
    seed_block = "\n".join(seed_block_lines) if seed_block_lines else "    (none)"

    hidden_block_lines: list[str] = []
    for hf in rule_hidden:
        if isinstance(hf, dict):
            f_text = (hf.get("fact") or "").strip()
            d_text = (hf.get("disclose_when") or "").strip()
            hidden_block_lines.append(
                f'    - {{fact: "{f_text}", disclose_when: "{d_text}"}}'
            )
    hidden_block = (
        "\n".join(hidden_block_lines) if hidden_block_lines else "    (none)"
    )

    transcript_lines: list[str] = []
    for t in turns:
        seq_str = str(t.get("sequence", "")).strip()
        if seq_str == "0" and t.get("speaker", "") == "[PRE_CHAT_FORM]":
            continue
        try:
            seq = int(seq_str)
        except ValueError:
            continue
        role = (t.get("role") or "").strip()
        speaker = (t.get("speaker") or "").strip()
        message = (t.get("message_redacted") or "").strip()
        message = re.sub(r"\s+", " ", message)
        if len(message) > 600:
            message = message[:597] + "..."
        transcript_lines.append(f"  [seq {seq}] {role} {speaker}: {message}")
    transcript_block = "\n".join(transcript_lines) if transcript_lines else "  (no turns)"

    return f"""SESSION
  source_session_id: {source_session_id}
  source_dataset:    {source_dataset}
  primary_uc:        {primary_uc}
  secondary_ucs:     {secondary_str}
  drift_type:        {drift}
  has_frustration:   {str(bool(has_frustration)).lower()}
  frustration_type:  {frustration_label}

FORM (pre-chat)
  topic_subject: {topic_subject}
  description:   {description}

RULE DRAFT
  seed_messages:
{seed_block}
  user_goal_summary: "{rule_user_goal}"
  hidden_facts:
{hidden_block}

TRANSCRIPT (selected source: {turns_filename}; sequence-ordered)
{transcript_block}

INSTRUCTIONS
  - Read the transcript and the rule draft.
  - If the rule draft's seed_messages already capture the user's real
    issue, keep them and set llm_confidence=high.
  - If they are greetings/closings/acks (e.g. "Hi Jason", "Thank you"),
    replace them with up to 3 visitor turns that describe the actual
    issue, copied VERBATIM from the transcript above.
  - Refresh user_goal_summary to be a single specific factual sentence
    about what the user wants to happen (no bot-side instructions).
    Append "Drift: {drift}." at the end.
  - Add at most 2 hidden_facts that the simulator can disclose if the
    bot asks - they must be supported by the transcript.
  - Output JSON only. No prose, no markdown, no comments.
"""


# ---------------------------------------------------------------------------
# Persona-draft + result data classes
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class PersonaDraft:
    """Rule-derived persona snapshot fed into the LLM reviewer.

    ``hidden_facts`` is a list of ``{"fact": str, "disclose_when": str}``
    dicts so the cache file round-trips exactly. ``verbosity`` is
    rule-derived; the LLM never proposes it.
    """

    seed_messages: list[str]
    user_goal_summary: str
    hidden_facts: list[dict[str, str]]
    verbosity: str

    def to_record(self) -> dict[str, Any]:
        return {
            "seed_messages": list(self.seed_messages),
            "user_goal_summary": self.user_goal_summary,
            "hidden_facts": [dict(hf) for hf in self.hidden_facts],
            "verbosity": self.verbosity,
        }


@dataclass(frozen=True)
class ReviewResult:
    """Per-session result returned by :meth:`LlmPersonaReviewer.review`."""

    accepted_value: PersonaDraft
    cache_record: dict[str, Any] | None
    cache_hit: bool
    used_offline_fallback: bool


# ---------------------------------------------------------------------------
# Verbosity (mirrors extractor._derive_verbosity)
# ---------------------------------------------------------------------------


def derive_verbosity(seed_messages: list[str]) -> str:
    """Mirror of ``extractor._derive_verbosity`` so this module is the
    single source of truth for what verbosity the persona ends up with
    when the LLM proposal is auto-accepted."""
    if not seed_messages:
        return "normal"
    avg_len = sum(len(m) for m in seed_messages) / len(seed_messages)
    if avg_len < 20:
        return "terse"
    if avg_len > 100:
        return "verbose"
    return "normal"


# ---------------------------------------------------------------------------
# Validators (shared with shadow-audit script)
# ---------------------------------------------------------------------------

_WS_RE = re.compile(r"\s+")
_IDENTIFIER_RE = re.compile(r"\b[A-Za-z]*\d{4,}[A-Za-z0-9]*\b|\b\d{4,}\b")


def _normalize(s: str) -> str:
    return _WS_RE.sub(" ", s).strip().lower()


def visitor_messages(turns: list[dict[str, Any]]) -> list[str]:
    out: list[str] = []
    for t in turns:
        seq_str = str(t.get("sequence", "")).strip()
        if seq_str == "0" and (t.get("speaker") or "") == "[PRE_CHAT_FORM]":
            continue
        if (t.get("role") or "").strip() != "visitor":
            continue
        msg = (t.get("message_redacted") or "").strip()
        if msg:
            out.append(msg)
    return out


def all_messages(turns: list[dict[str, Any]]) -> list[str]:
    out: list[str] = []
    for t in turns:
        seq_str = str(t.get("sequence", "")).strip()
        if seq_str == "0" and (t.get("speaker") or "") == "[PRE_CHAT_FORM]":
            continue
        msg = (t.get("message_redacted") or "").strip()
        if msg:
            out.append(msg)
    return out


def verbatim_in_visitor(quote: str, visitor_msgs: list[str]) -> bool:
    nq = _normalize(quote)
    if not nq:
        return False
    for v in visitor_msgs:
        if nq in _normalize(v):
            return True
    return False


def shared_identifier(fact: str, all_msgs: list[str]) -> bool:
    fact_ids = set(m.group(0) for m in _IDENTIFIER_RE.finditer(fact))
    if not fact_ids:
        return False
    joined = " ".join(all_msgs)
    for fid in fact_ids:
        if fid in joined:
            return True
    return False


def word_overlap(fact: str, all_msgs: list[str]) -> bool:
    """Strict 4-word contiguous-substring match. (Locked decision.)"""
    fact_norm = _normalize(fact)
    words = fact_norm.split(" ")
    if len(words) < 4:
        if not fact_norm:
            return False
        for m in all_msgs:
            if fact_norm in _normalize(m):
                return True
        return False
    for i in range(0, len(words) - 3):
        window = " ".join(words[i : i + 4])
        for m in all_msgs:
            if window in _normalize(m):
                return True
    return False


@dataclass
class ValidationOutcome:
    proposal: dict[str, Any]
    status: str
    notes: list[str] = field(default_factory=list)
    user_goal_summary_too_long_chars: int | None = None
    dropped_hidden_facts: list[str] = field(default_factory=list)


def validate_response(
    raw: dict[str, Any],
    rule_draft: PersonaDraft,
    turns: list[dict[str, Any]],
) -> ValidationOutcome:
    """Apply all validators. Always returns a ValidationOutcome; never raises.

    Status values: ``ok``, ``forbidden_key_present``, ``schema_error``,
    ``verbatim_check_failed``, ``user_goal_summary_too_long``,
    ``bot_expectation_phrase_detected``, ``other``.
    """
    notes: list[str] = []
    status = "ok"
    too_long_chars: int | None = None
    dropped_facts: list[str] = []

    if not isinstance(raw, dict):
        return ValidationOutcome(
            proposal=rule_draft.to_record(),
            status="schema_error",
            notes=["response is not a JSON object"],
        )

    extra_keys = sorted(set(raw.keys()) - ALLOWED_RESPONSE_KEYS)
    if extra_keys:
        notes.append(f"forbidden_key_present: {extra_keys}")
        status = "forbidden_key_present"

    proposal: dict[str, Any] = {
        "seed_messages": list(rule_draft.seed_messages),
        "user_goal_summary": rule_draft.user_goal_summary or "",
        "hidden_facts": [dict(hf) for hf in rule_draft.hidden_facts],
        "llm_confidence": "low",
        "llm_rationale": "",
    }

    conf = raw.get("llm_confidence", "")
    if isinstance(conf, str) and conf.strip().lower() in {"low", "medium", "high"}:
        proposal["llm_confidence"] = conf.strip().lower()
    else:
        notes.append(f"invalid_llm_confidence: {conf!r}")
        if status == "ok":
            status = "schema_error"

    rationale = raw.get("llm_rationale", "")
    if isinstance(rationale, str):
        proposal["llm_rationale"] = rationale.strip()
    else:
        notes.append("llm_rationale missing or non-string")

    raw_seeds = raw.get("seed_messages")
    visitors = visitor_messages(turns)
    if (
        isinstance(raw_seeds, list)
        and 1 <= len(raw_seeds) <= 3
        and all(isinstance(s, str) and s.strip() for s in raw_seeds)
    ):
        seeds_clean = [s.strip() for s in raw_seeds]
        bad = [s for s in seeds_clean if not verbatim_in_visitor(s, visitors)]
        if bad:
            notes.append(f"verbatim_check_failed: {bad}")
            status = "verbatim_check_failed"
        else:
            proposal["seed_messages"] = seeds_clean
    else:
        notes.append(
            f"seed_messages schema invalid (got type={type(raw_seeds).__name__}, "
            f"len={len(raw_seeds) if isinstance(raw_seeds, list) else 'n/a'})"
        )
        if status == "ok":
            status = "schema_error"

    raw_ugs = raw.get("user_goal_summary")
    if isinstance(raw_ugs, str):
        cand = raw_ugs.strip()
        if len(cand) > USER_GOAL_SUMMARY_MAX_CHARS:
            too_long_chars = len(cand)
            notes.append(
                f"user_goal_summary too long ({len(cand)} chars, max={USER_GOAL_SUMMARY_MAX_CHARS})"
            )
            if status == "ok":
                status = "user_goal_summary_too_long"
        else:
            bot_hits = [p.pattern for p in BOT_EXPECTATION_PATTERNS if p.search(cand)]
            if bot_hits:
                notes.append(f"bot_expectation_phrase_detected: {bot_hits}")
                status = "bot_expectation_phrase_detected"
            else:
                proposal["user_goal_summary"] = cand
    else:
        notes.append("user_goal_summary missing or non-string")
        if status == "ok":
            status = "schema_error"

    raw_hf = raw.get("hidden_facts", [])
    msgs = all_messages(turns)
    if isinstance(raw_hf, list):
        kept: list[dict[str, str]] = []
        for hf in raw_hf:
            if not isinstance(hf, dict):
                notes.append(f"hidden_fact entry not a dict: {hf!r}")
                continue
            f_text = (hf.get("fact") or "").strip()
            d_text = (hf.get("disclose_when") or "").strip()
            if not f_text or not d_text:
                notes.append(f"hidden_fact missing fact/disclose_when: {hf!r}")
                continue
            if word_overlap(f_text, msgs) or shared_identifier(f_text, msgs):
                kept.append({"fact": f_text, "disclose_when": d_text})
            else:
                notes.append(f"unsupported_hidden_fact_dropped: {f_text!r}")
                dropped_facts.append(f_text)
        proposal["hidden_facts"] = kept
    else:
        notes.append(f"hidden_facts not a list (got {type(raw_hf).__name__})")

    return ValidationOutcome(
        proposal=proposal,
        status=status,
        notes=notes,
        user_goal_summary_too_long_chars=too_long_chars,
        dropped_hidden_facts=dropped_facts,
    )


# ---------------------------------------------------------------------------
# DeepSeek HTTP client
# ---------------------------------------------------------------------------


class DeepSeekClient:
    """Tiny synchronous DeepSeek client. Patchable in tests via
    monkeypatching :meth:`_post`. Never logs the bearer token."""

    def __init__(
        self,
        api_key: str,
        *,
        model: str = DEFAULT_DEEPSEEK_MODEL,
        endpoint: str = DEEPSEEK_ENDPOINT,
        timeout_s: float = DEFAULT_HTTP_TIMEOUT_S,
        max_retries: int = DEFAULT_MAX_RETRIES,
    ) -> None:
        if not api_key:
            raise ValueError(
                "DeepSeek API key is missing. Set DEEPSEEK_API_KEY or pass "
                "api_key=... explicitly. Use offline=True for tests."
            )
        self._api_key = api_key
        self._model = model
        self._endpoint = endpoint
        self._timeout_s = timeout_s
        self._max_retries = max_retries

    @property
    def model(self) -> str:
        return self._model

    def _post(self, body: dict[str, Any]) -> httpx.Response:
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }
        with httpx.Client(timeout=self._timeout_s) as client:
            return client.post(self._endpoint, headers=headers, json=body)

    def call(self, messages: list[dict[str, str]]) -> dict[str, Any]:
        """Send a chat-completions request and return the parsed JSON
        ``message.content`` dict. Retries on 5xx / network errors."""
        body = {
            "model": self._model,
            "messages": messages,
            "temperature": 0,
            "response_format": {"type": "json_object"},
        }
        last_exc: Exception | None = None
        for attempt in range(self._max_retries + 1):
            try:
                resp = self._post(body)
            except (httpx.RequestError, httpx.TransportError) as e:  # pragma: no cover - network path
                last_exc = e
                logger.warning(
                    "DeepSeek request error attempt %d: %s", attempt + 1, e
                )
            else:
                if resp.status_code >= 500:
                    logger.warning(
                        "DeepSeek 5xx attempt %d (status=%s)",
                        attempt + 1, resp.status_code,
                    )
                elif resp.status_code != 200:
                    snippet = resp.text[:300] if resp.text else ""
                    raise RuntimeError(
                        f"DeepSeek non-200 response: {resp.status_code} {snippet}"
                    )
                else:
                    data = resp.json()
                    content = (
                        data.get("choices", [{}])[0]
                        .get("message", {})
                        .get("content", "")
                    )
                    try:
                        return json.loads(content)
                    except json.JSONDecodeError as e:
                        raise RuntimeError(
                            f"DeepSeek returned non-JSON content: {content[:300]}"
                        ) from e
            if attempt < self._max_retries:
                delay = (2 ** attempt) + random.random()
                time.sleep(delay)
        raise RuntimeError(
            f"DeepSeek call failed after {self._max_retries + 1} attempts: {last_exc}"
        )


# ---------------------------------------------------------------------------
# Reviewer
# ---------------------------------------------------------------------------


@dataclass
class _PromptBundle:
    system: str
    user: str
    prompt_hash: str


@dataclass
class _ReviewerInputs:
    source_session_id: str
    source_dataset: str
    primary_uc: str
    secondary_ucs: list[str]
    drift_type: str
    has_frustration: bool
    frustration_type: str
    topic_subject: str
    description: str
    rule_draft: PersonaDraft
    transcript_turns: list[dict[str, Any]]
    turns_filename: str
    # Display-only; written to ``case_id_hint`` in the cache record but
    # NOT included in the rendered prompt body (Wave A6.6).
    case_id_hint: str = ""


def _build_prompt(inputs: _ReviewerInputs) -> _PromptBundle:
    user = render_user_prompt(
        source_session_id=inputs.source_session_id,
        source_dataset=inputs.source_dataset,
        primary_uc=inputs.primary_uc,
        secondary_ucs=inputs.secondary_ucs,
        drift_type=inputs.drift_type,
        has_frustration=inputs.has_frustration,
        frustration_type=inputs.frustration_type,
        topic_subject=inputs.topic_subject,
        description=inputs.description,
        rule_draft=inputs.rule_draft,
        turns=inputs.transcript_turns,
        turns_filename=inputs.turns_filename,
    )
    prompt_hash = hashlib.sha256(
        (PROMPT_SYSTEM + user).encode("utf-8")
    ).hexdigest()
    return _PromptBundle(system=PROMPT_SYSTEM, user=user, prompt_hash=prompt_hash)


def _coerce_persona_from_record(
    record: dict[str, Any], rule_draft: PersonaDraft
) -> PersonaDraft:
    """Build a ``PersonaDraft`` from an ``accepted_value`` cache block.

    ``verbosity`` in the record is informational only -- the production
    code recomputes it from the seed_messages so it always matches the
    rule extractor's logic on whatever seeds we end up using.
    """
    seeds = list(record.get("seed_messages") or [])
    ugs = (record.get("user_goal_summary") or "").strip()
    raw_hidden = record.get("hidden_facts") or []
    hidden: list[dict[str, str]] = []
    for hf in raw_hidden:
        if isinstance(hf, dict):
            hidden.append({
                "fact": (hf.get("fact") or "").strip(),
                "disclose_when": (hf.get("disclose_when") or "").strip(),
            })
    return PersonaDraft(
        seed_messages=seeds,
        user_goal_summary=ugs,
        hidden_facts=hidden,
        verbosity=derive_verbosity(seeds) if seeds else rule_draft.verbosity,
    )


def _now_isoformat() -> str:
    return _dt.datetime.now(_dt.timezone.utc).isoformat()


_VALID_CACHE_KEYS = {
    "source_session_id",
    "case_id_hint",
    "llm_model",
    "prompt_hash",
    "generated_at",
    "llm_confidence",
    "llm_rationale",
    "rule_draft",
    "llm_proposal",
    "accepted_value",
    "acceptance_reason",
    "validation_notes",
}


def _validate_cache_record(record: dict[str, Any], session_id: str) -> bool:
    if not isinstance(record, dict):
        return False
    if (record.get("source_session_id") or "").strip() != session_id:
        return False
    if "accepted_value" not in record or not isinstance(
        record["accepted_value"], dict
    ):
        return False
    accepted = record["accepted_value"]
    if "seed_messages" not in accepted or "user_goal_summary" not in accepted:
        return False
    return True


class LlmPersonaReviewer:
    """Production L2 reviewer.

    Cache lifecycle (committed under ``case_spec_llm_cache/``):

    * On call, the reviewer computes ``prompt_hash`` over the full
      ``system + user`` prompt (which itself bakes in rule_draft and the
      transcript turns).
    * If the cache file for ``source_session_id`` exists AND
      ``prompt_hash`` matches AND the session is not in
      ``force_refresh_sessions``, the reviewer reconstitutes the persona
      from ``accepted_value`` and returns ``cache_hit=True``.
    * Otherwise it calls DeepSeek (unless ``offline=True``), validates
      the response, applies the Option A retry on length failures, and
      writes a fresh cache file.

    Set ``dry_cache=True`` to suppress cache writes entirely (used by the
    Wave A6.1 shadow audit script).
    """

    def __init__(
        self,
        *,
        api_key: str | None = None,
        model: str = DEFAULT_DEEPSEEK_MODEL,
        cache_dir: Path,
        offline: bool = False,
        force_refresh_sessions: Iterable[str] = (),
        timeout_s: float = DEFAULT_HTTP_TIMEOUT_S,
        max_retries: int = DEFAULT_MAX_RETRIES,
        dry_cache: bool = False,
        client: DeepSeekClient | None = None,
    ) -> None:
        self._cache_dir = Path(cache_dir)
        self._offline = bool(offline)
        self._force_refresh = frozenset(force_refresh_sessions)
        self._dry_cache = bool(dry_cache)
        self._model = model
        self._client_override = client
        self._timeout_s = timeout_s
        self._max_retries = max_retries
        self._api_key_arg = api_key
        # In offline mode we never need credentials.
        self._client: DeepSeekClient | None = None

    def _get_client(self) -> DeepSeekClient:
        if self._client_override is not None:
            return self._client_override
        if self._client is None:
            api_key = self._api_key_arg or os.environ.get("DEEPSEEK_API_KEY", "")
            self._client = DeepSeekClient(
                api_key,
                model=self._model,
                timeout_s=self._timeout_s,
                max_retries=self._max_retries,
            )
        return self._client

    @property
    def cache_dir(self) -> Path:
        return self._cache_dir

    @property
    def model(self) -> str:
        return self._model

    @property
    def offline(self) -> bool:
        return self._offline

    @property
    def dry_cache(self) -> bool:
        return self._dry_cache

    def _cache_path(self, session_id: str) -> Path:
        return self._cache_dir / f"{session_id}.yaml"

    def _read_cache(self, session_id: str) -> dict[str, Any] | None:
        path = self._cache_path(session_id)
        if not path.exists():
            return None
        try:
            with path.open("r", encoding="utf-8") as f:
                doc = yaml.safe_load(f) or {}
        except Exception as e:  # pragma: no cover - defensive
            logger.warning("Failed to read cache file %s: %s", path, e)
            return None
        if not _validate_cache_record(doc, session_id):
            logger.warning(
                "Cache file %s failed structural validation; will regenerate.", path
            )
            return None
        return doc

    def _write_cache(self, session_id: str, record: dict[str, Any]) -> None:
        if self._dry_cache:
            return
        self._cache_dir.mkdir(parents=True, exist_ok=True)
        path = self._cache_path(session_id)
        with path.open("w", encoding="utf-8") as f:
            yaml.dump(record, f, sort_keys=False, allow_unicode=True, width=120)

    def review(
        self,
        *,
        source_session_id: str,
        rule_draft: PersonaDraft,
        transcript_turns: list[dict[str, Any]],
        hr_context: dict[str, Any],
        case_id_hint: str = "",
    ) -> ReviewResult:
        if self._offline:
            return ReviewResult(
                accepted_value=rule_draft,
                cache_record=None,
                cache_hit=False,
                used_offline_fallback=True,
            )

        inputs = _ReviewerInputs(
            source_session_id=source_session_id,
            source_dataset=str(hr_context.get("source_dataset", "")).strip(),
            primary_uc=str(hr_context.get("primary_uc", "")).strip(),
            secondary_ucs=list(hr_context.get("secondary_ucs") or []),
            drift_type=str(hr_context.get("drift_type", "none") or "none").strip(),
            has_frustration=bool(hr_context.get("has_frustration", False)),
            frustration_type=str(hr_context.get("frustration_type", "none") or "none").strip(),
            topic_subject=str(hr_context.get("topic_subject", "") or "").strip(),
            description=str(hr_context.get("description", "") or "").strip(),
            rule_draft=rule_draft,
            transcript_turns=list(transcript_turns),
            turns_filename=str(hr_context.get("turns_filename", "") or "").strip(),
            case_id_hint=case_id_hint or "",
        )
        prompt = _build_prompt(inputs)

        # ---- cache lookup ----
        if source_session_id not in self._force_refresh:
            cache_doc = self._read_cache(source_session_id)
            if cache_doc and (cache_doc.get("prompt_hash") or "") == prompt.prompt_hash:
                accepted_record = cache_doc["accepted_value"]
                accepted = _coerce_persona_from_record(accepted_record, rule_draft)
                return ReviewResult(
                    accepted_value=accepted,
                    cache_record=cache_doc,
                    cache_hit=True,
                    used_offline_fallback=False,
                )

        # ---- cache miss / hash mismatch / force refresh ----
        client = self._get_client()
        messages = [
            {"role": "system", "content": prompt.system},
            {"role": "user", "content": prompt.user},
        ]

        try:
            raw_response = client.call(messages)
        except Exception as e:
            logger.warning(
                "DeepSeek call failed for session %s: %s. Falling back to rule_draft.",
                source_session_id, e,
            )
            record = self._build_cache_record(
                inputs=inputs,
                prompt_hash=prompt.prompt_hash,
                rule_draft=rule_draft,
                llm_proposal=None,
                accepted_value=rule_draft,
                acceptance_reason="rule_fallback_validation_failed",
                llm_confidence="",
                llm_rationale="",
                validation_notes=[f"api_error: {type(e).__name__}: {e}"],
            )
            self._write_cache(source_session_id, record)
            return ReviewResult(
                accepted_value=rule_draft,
                cache_record=record,
                cache_hit=False,
                used_offline_fallback=False,
            )

        outcome = validate_response(raw_response, rule_draft, transcript_turns)

        # ---- Option A: one retry on user_goal_summary too long ----
        if outcome.status == "user_goal_summary_too_long":
            length_n = outcome.user_goal_summary_too_long_chars or 0
            retry_user = (
                f"Your previous user_goal_summary was {length_n} characters. "
                f"The MAXIMUM is {USER_GOAL_SUMMARY_MAX_CHARS} characters. "
                f"Reduce it and resubmit the entire JSON object with the same "
                f"schema, keeping seed_messages and hidden_facts unchanged."
            )
            retry_messages = [
                {"role": "system", "content": prompt.system},
                {"role": "user", "content": prompt.user},
                {"role": "assistant", "content": json.dumps(raw_response)},
                {"role": "user", "content": retry_user},
            ]
            try:
                raw_retry = client.call(retry_messages)
            except Exception as e:
                logger.warning(
                    "DeepSeek retry call failed for session %s: %s. Falling back.",
                    source_session_id, e,
                )
                record = self._build_cache_record(
                    inputs=inputs,
                    prompt_hash=prompt.prompt_hash,
                    rule_draft=rule_draft,
                    llm_proposal=outcome.proposal,
                    accepted_value=rule_draft,
                    acceptance_reason="rule_fallback_validation_failed",
                    llm_confidence=outcome.proposal.get("llm_confidence", ""),
                    llm_rationale=outcome.proposal.get("llm_rationale", ""),
                    validation_notes=outcome.notes
                    + [f"retry_api_error: {type(e).__name__}: {e}"],
                )
                self._write_cache(source_session_id, record)
                return ReviewResult(
                    accepted_value=rule_draft,
                    cache_record=record,
                    cache_hit=False,
                    used_offline_fallback=False,
                )
            retry_outcome = validate_response(raw_retry, rule_draft, transcript_turns)
            # Merge note trail: keep the original "too long" note plus retry notes
            merged_notes = (
                [f"first_attempt: {n}" for n in outcome.notes]
                + [f"retry_attempt: {n}" for n in retry_outcome.notes]
            )
            outcome = ValidationOutcome(
                proposal=retry_outcome.proposal,
                status=retry_outcome.status,
                notes=merged_notes,
                user_goal_summary_too_long_chars=retry_outcome.user_goal_summary_too_long_chars,
                dropped_hidden_facts=retry_outcome.dropped_hidden_facts,
            )

        proposal = outcome.proposal
        confidence = (proposal.get("llm_confidence") or "").lower()

        if outcome.status == "ok" and confidence == "high":
            accepted = PersonaDraft(
                seed_messages=list(proposal["seed_messages"]),
                user_goal_summary=proposal["user_goal_summary"],
                hidden_facts=[dict(hf) for hf in proposal["hidden_facts"]],
                verbosity=derive_verbosity(list(proposal["seed_messages"])),
            )
            acceptance_reason = "auto_accept_high_confidence"
        elif outcome.status == "ok":
            accepted = rule_draft
            acceptance_reason = "rule_fallback_low_confidence"
        else:
            accepted = rule_draft
            acceptance_reason = "rule_fallback_validation_failed"

        record = self._build_cache_record(
            inputs=inputs,
            prompt_hash=prompt.prompt_hash,
            rule_draft=rule_draft,
            llm_proposal=proposal,
            accepted_value=accepted,
            acceptance_reason=acceptance_reason,
            llm_confidence=confidence,
            llm_rationale=proposal.get("llm_rationale", ""),
            validation_notes=outcome.notes,
        )
        self._write_cache(source_session_id, record)

        return ReviewResult(
            accepted_value=accepted,
            cache_record=record,
            cache_hit=False,
            used_offline_fallback=False,
        )

    # ------------------------------------------------------------------
    # Cache record construction
    # ------------------------------------------------------------------

    def _build_cache_record(
        self,
        *,
        inputs: _ReviewerInputs,
        prompt_hash: str,
        rule_draft: PersonaDraft,
        llm_proposal: dict[str, Any] | None,
        accepted_value: PersonaDraft,
        acceptance_reason: str,
        llm_confidence: str,
        llm_rationale: str,
        validation_notes: list[str],
    ) -> dict[str, Any]:
        record: dict[str, Any] = {
            "source_session_id": inputs.source_session_id,
            "case_id_hint": inputs.case_id_hint,
            "llm_model": self._model,
            "prompt_hash": prompt_hash,
            "generated_at": _now_isoformat(),
            "llm_confidence": llm_confidence or "",
            "llm_rationale": llm_rationale or "",
            "rule_draft": rule_draft.to_record(),
            "llm_proposal": (
                {
                    "seed_messages": list(llm_proposal.get("seed_messages") or []),
                    "user_goal_summary": llm_proposal.get("user_goal_summary", "") or "",
                    "hidden_facts": [
                        dict(hf) for hf in (llm_proposal.get("hidden_facts") or [])
                    ],
                }
                if llm_proposal is not None
                else None
            ),
            "accepted_value": accepted_value.to_record(),
            "acceptance_reason": acceptance_reason,
            "validation_notes": list(validation_notes),
        }
        return record


__all__ = [
    "PersonaDraft",
    "ReviewResult",
    "LlmPersonaReviewer",
    "DeepSeekClient",
    "PROMPT_SYSTEM",
    "PROMPT_TEMPLATE_SHA256",
    "ALLOWED_RESPONSE_KEYS",
    "BOT_EXPECTATION_PATTERNS",
    "USER_GOAL_SUMMARY_MAX_CHARS",
    "SOURCE_DATASET_TURNS_FILE",
    "render_user_prompt",
    "validate_response",
    "ValidationOutcome",
    "verbatim_in_visitor",
    "shared_identifier",
    "word_overlap",
    "visitor_messages",
    "all_messages",
    "derive_verbosity",
    "DEFAULT_DEEPSEEK_MODEL",
]
