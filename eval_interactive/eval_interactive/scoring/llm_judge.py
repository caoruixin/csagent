"""LLM Judge -- L3 semantic quality scoring via LLM (1-5 scale)."""

from __future__ import annotations

import json
import logging
import re
from dataclasses import dataclass

from openai import OpenAI

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.config import Config, resolve_judge_config
from eval_interactive.trace.models import TraceData

logger = logging.getLogger(__name__)

_DEFAULT_SCORE = 3.0
_MAX_RETRIES = 1


# Models observed (this process) to reject an explicit ``temperature``.
# The executor builds a fresh ``LlmJudge`` per case, so without a
# process-level memo every case would pay one doomed 400 before falling
# back — 486 wasted calls on a full-corpus run.
_TEMPERATURE_PINNED_MODELS: set[str] = set()


def _is_temperature_rejection(exc: Exception) -> bool:
    """Does ``exc`` look like "this model does not accept that temperature"?

    Provider-agnostic: a 400 whose message mentions ``temperature``.
    Deliberately narrow — any other 400 (bad model id, oversized prompt)
    must still surface through the normal retry / default-score path so a
    real failure is not silently swallowed.
    """
    status = getattr(exc, "status_code", None) or getattr(exc, "code", None)
    if status not in (400, "400"):
        return False
    return "temperature" in str(exc).lower()


@dataclass
class JudgeResult:
    """Score for a single LLM-judged dimension.

    ``severity`` mirrors the S-Eval-1 D-2.5 convention on
    :class:`~eval_interactive.scoring.hard_checks.HardCheckResult` /
    :class:`~eval_interactive.scoring.outcome_checks.OutcomeCheckResult`.
    A ``"critical"`` result contributes to the composite ``judge_score``
    mean; an ``"advisory"`` result is recorded for diagnostics but does
    not factor into ``judge_score`` or ``composite_score``.

    S-Eval-5 (M3-Eval): the three legacy L3 dims (``groundedness``,
    ``relevance``, ``tone_appropriateness``) are demoted from
    composite contributors to ``severity="advisory"`` (Tier-3 advisory
    per the four-tier pyramid in ``docs/milestone_objective.md`` §2).
    The new ``user_goal_achievement`` dim is also ``"advisory"``: it is
    a Tier-1 *supplementary* signal (NOT a hard gate; bad-case +
    anchor_outcome manual review remain the primary Tier-1 gates per
    ``iteration_governance.md`` §5.6 + milestone §5). ``premature_finish``
    and ``stall_quality`` keep their existing default-``"critical"``
    severity so that historical callers that don't yet know about
    severity see no change in behaviour.
    """

    dimension: str
    score: float  # 1 - 5
    reasoning: str = ""
    severity: str = "critical"
    # Sprint 105 (item 3). Empty when ``score`` came from the judge LLM.
    # Otherwise names why no real score was obtained and ``_DEFAULT_SCORE``
    # was substituted: ``llm_call_failed`` / ``parse_failure`` /
    # ``unexpected_fallthrough``.
    #
    # Why this field exists: on 2026-07-25 the judge was pointed at a model
    # that rejects an explicit ``temperature``. Every dimension 400'd twice
    # and fell back to the mid-scale constant 3.0, so the entire L3 layer
    # became a constant behind one WARNING log line while every run kept
    # reporting scored-looking numbers. A fallback carries no information
    # in either direction; conflating it with a real 3.0 is the defect.
    # ``composite.py`` excludes fallbacks from the gating judge mean so
    # the layer cannot silently re-become a constant.
    fallback_reason: str = ""

    @property
    def is_fallback(self) -> bool:
        """True when no real judge score was obtained for this dimension."""
        return bool(self.fallback_reason)


class LlmJudge:
    """L3 LLM Judge -- semantic quality scoring via LLM."""

    # S-Eval-5 (M3-Eval): dims tagged ``advisory`` are recorded for
    # diagnostics but do not contribute to ``composite_score`` (their
    # numeric scores are excluded from the L3 judge_score mean in
    # ``composite.py``). The three demoted dims (``groundedness``,
    # ``relevance``, ``tone_appropriateness``) become Tier-3 advisory.
    # The new ``user_goal_achievement`` dim is wired as Tier-1
    # *supplementary* advisory — recorded per-case, never gates.
    _ADVISORY_DIMENSIONS = frozenset({
        "groundedness",
        "relevance",
        "tone_appropriateness",
        "user_goal_achievement",
    })

    ALL_DIMENSIONS = [
        "groundedness",
        "relevance",
        "tone_appropriateness",
        "user_goal_achievement",
        "premature_finish",
        "stall_quality",
    ]

    def __init__(self, config: Config):
        """Initialize with OpenAI-compatible client.

        Args:
            config: Application config with llm section.
        """
        # WS-5 item 3: the judge reads its OWN section so it is not forced
        # onto the user simulator's model/provider. ``resolve_judge_config``
        # falls back field-by-field to ``config.llm`` (with a WARNING) when
        # the JUDGE_* env vars are unset, so environments that predate the
        # split behave exactly as before.
        judge_cfg = resolve_judge_config(config)
        self._model = judge_cfg.model
        self._temperature = judge_cfg.temperature  # 0.0 for grading
        # Set on the first provider-side rejection of an explicit
        # temperature; see ``_is_temperature_rejection``. Seeded from the
        # process-level memo so later cases in the same run skip the probe.
        self._temperature_unsupported = self._model in _TEMPERATURE_PINNED_MODELS
        self._client = OpenAI(
            base_url=judge_cfg.base_url or None,
            api_key=judge_cfg.api_key or "not-set",
        )
        logger.info(
            "LlmJudge initialized: model=%s, base_url=%s (simulator model=%s)",
            self._model, judge_cfg.base_url, config.llm.model,
        )

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def judge(
        self,
        case_spec: CaseSpec,
        trace: TraceData,
        transcript: list[dict],
    ) -> list[JudgeResult]:
        """Run all LLM judge dimensions listed in case_spec.scoring.llm_judge_dimensions."""
        configured = set(case_spec.scoring.llm_judge_dimensions)
        results: list[JudgeResult] = []

        dispatch = {
            "groundedness": lambda: self._judge_groundedness(trace, transcript),
            "relevance": lambda: self._judge_relevance(trace, transcript),
            "tone_appropriateness": lambda: self._judge_tone_appropriateness(case_spec, trace, transcript),
            "user_goal_achievement": lambda: self._judge_user_goal_achievement(case_spec, trace, transcript),
            "premature_finish": lambda: self._judge_premature_finish(trace, transcript),
            "stall_quality": lambda: self._judge_stall_quality(trace, transcript),
        }

        for dim in self.ALL_DIMENSIONS:
            if dim in configured and dim in dispatch:
                results.append(dispatch[dim]())

        return results

    # ------------------------------------------------------------------
    # Individual judge methods
    # ------------------------------------------------------------------

    def _judge_groundedness(self, trace: TraceData, transcript: list[dict]) -> JudgeResult:
        """Is the bot response supported by retrieved knowledge sources?"""
        # Collect source_ids from turns that answered
        sources_info = []
        for turn in trace.turns:
            if turn.source_ids:
                sources_info.append(
                    f"Turn {turn.turn_index}: sources={turn.source_ids}"
                )

        sources_block = "\n".join(sources_info) if sources_info else "No source IDs found in any turn."

        # S-Eval-5 (M3-Eval): close R-l1-source-citation-quality-rubric by
        # tightening citation-quality expectations. The L3 groundedness
        # rubric is the right surface (L1 hard_checks is fenced per
        # S-Eval-5 §6 and `no_pii_leakage` / `no_critical_policy_violation`
        # do not evaluate citation user-actionability). The new clause
        # treats a bare Salesforce knowledge-article ID (e.g.,
        # ``ka44J000000gKxqQAE``) as NOT user-actionable when surfaced
        # alone in the customer-facing bot reply: the user cannot click
        # an opaque ID to read the article. A `canonical_url` (e.g.,
        # ``https://help.example.com/articles/repost-an-advert``) OR a
        # human-readable article title (e.g., "How to repost an
        # advert") is required for the citation to count as
        # user-actionable. This is a *narrative* tightening of the
        # rubric, not a per-UC matrix or keyword enumeration; the
        # rubric describes a *failure pattern* (ID-alone citation) the
        # judge should recognize semantically.
        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate how well the bot's responses are grounded in retrieved knowledge sources.

IMPORTANT: If the bot's response is purely procedural or an escalation message (e.g.,
"transferring you to a human agent", "let me connect you with a specialist") with no
factual claims about the user's issue, score 5 — there are no claims that require
grounding. Only score low when the bot makes factual claims without citing sources.

CITATION QUALITY (R-l1-source-citation-quality-rubric, closed via
S-Eval-5 / M3-Eval):
A citation in the customer-facing bot reply counts as user-actionable
ONLY when it surfaces either (a) a canonical URL the user can open
(e.g., ``https://help.example.com/articles/repost-an-advert``) OR
(b) a human-readable article title the user can search for (e.g.,
"How to repost an advert"). A bare Salesforce knowledge-article ID
shown alone — e.g., ``ka44J000000gKxqQAE``, ``ka0xx00000xxxxxAAB``,
or any opaque alphanumeric handle from the internal knowledge corpus —
is NOT user-actionable on its own: the user cannot click or look up
an opaque ID. When the bot makes factual claims and the only
"citation" surfaced is a bare Salesforce ID, treat this as a citation
failure (score ≤ 2) even if the bot did retrieve a source internally.

TRANSCRIPT:
{self._format_transcript(transcript)}

SOURCE CITATIONS PER TURN:
{sources_block}

SCORING (1-5):
5 = Every factual claim is backed by a cited source that surfaces a canonical URL or article title to the user, OR no factual claims were made (e.g., escalation/procedural messages)
4 = Most claims are grounded with user-actionable citations, minor unsourced details
3 = Some claims are grounded but notable gaps exist (including citations that surface only a bare Salesforce ID)
2 = Few claims are grounded; bot invents information OR cites only by opaque Salesforce ID alone
1 = Bot fabricates factual answers without any source backing

Respond with ONLY a JSON object: {{"score": <1-5>, "reasoning": "<brief explanation>"}}"""

        return self._call_llm("groundedness", prompt)

    def _judge_relevance(self, trace: TraceData, transcript: list[dict]) -> JudgeResult:
        """Does the bot response address the user's actual question?"""
        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate whether the bot's responses directly address the user's actual questions and concerns.

TRANSCRIPT:
{self._format_transcript(transcript)}

SCORING (1-5):
5 = Every response precisely addresses what the user asked
4 = Mostly relevant, occasional tangential information
3 = Partially relevant but misses key aspects of the query
2 = Mostly irrelevant; bot talks past the user
1 = Completely irrelevant; bot ignores the user's question

Respond with ONLY a JSON object: {{"score": <1-5>, "reasoning": "<brief explanation>"}}"""

        return self._call_llm("relevance", prompt)

    def _judge_tone_appropriateness(
        self,
        case_spec: CaseSpec,
        trace: TraceData,
        transcript: list[dict],
    ) -> JudgeResult:
        """Is the bot professional, empathetic, not dismissive?"""
        frustration = case_spec.persona.frustration_level
        first_name = getattr(case_spec.form_context, "first_name", None) or ""
        first_name_clause = (
            f'The pre-populated ``form_context.first_name`` for this case is "{first_name}".'
            if first_name
            else "No ``form_context.first_name`` is pre-populated for this case."
        )
        # S-Eval-5 (M3-Eval): close R-l3-judge-form-context-trust-rubric.
        # The bot greeting the user by ``form_context.first_name`` (e.g.,
        # "Hi Alex, how can I help?") was historically penalised under
        # earlier tone rubric interpretations as "unauthorized
        # familiarity". This is wrong — ``form_context.first_name`` is
        # canonical pre-known identity surfaced by the platform's
        # account-bound chat session, NOT user-entered free text. The
        # rubric now explicitly states this as a *trusted signal*; the
        # bot is allowed to greet by first name without confirmation.
        # This is a narrative trust-signal addition, NOT a per-UC matrix
        # or keyword branch.
        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate the bot's tone and professionalism.

CONTEXT: The customer's frustration level is "{frustration}".

TRUSTED PERSONA CONTEXT (R-l3-judge-form-context-trust-rubric, closed
via S-Eval-5 / M3-Eval): {first_name_clause} The ``form_context``
fields are canonical pre-known identity surfaced by the platform's
account-bound chat session (NOT user-entered free text). The bot is
allowed to address the customer by ``first_name`` without a
confirmation turn; greeting "Hi {{first_name}}, ..." is appropriate
and SHOULD NOT be penalised for "unauthorized familiarity" or
"assuming identity". A familiar greeting against a populated
``form_context.first_name`` is acceptable warmth, not over-reach.

TRANSCRIPT:
{self._format_transcript(transcript)}

SCORING (1-5):
5 = Professional, empathetic, appropriately warm; matches the customer's emotional state; greeting-by-first-name (when populated) is acceptable
4 = Generally professional with minor tone issues
3 = Adequate but somewhat mechanical or slightly dismissive
2 = Tone is inappropriate (overly casual, condescending, or ignoring frustration)
1 = Rude, dismissive, or completely tone-deaf

Respond with ONLY a JSON object: {{"score": <1-5>, "reasoning": "<brief explanation>"}}"""

        return self._call_llm("tone_appropriateness", prompt)

    def _judge_user_goal_achievement(
        self,
        case_spec: CaseSpec,
        trace: TraceData,
        transcript: list[dict],
    ) -> JudgeResult:
        """S-Eval-5 (M3-Eval): NEW Tier-1 supplementary advisory L3 dim.

        Did the bot help the customer achieve the stated
        ``persona.user_goal_summary``, or appropriately escalate / defer?
        Wired as Tier-1 *supplementary* advisory (``severity="advisory"``):
        the dim is recorded per-case for trend tracking, but it does
        NOT flip ``case_passed`` or factor into ``composite_score``.
        Bad-case suite + ``anchor_outcome`` manual review remain the
        primary Tier-1 gates per ``iteration_governance.md`` §5.6 and
        ``docs/milestone_objective.md`` §5.

        Anchored on the customer-perspective ``user_goal_summary`` field
        from the CaseSpec persona (see ``case_spec/schema.py`` Persona).
        Score range coarse 1-5: 1 = clear failure / wrong UC /
        unresolved; 3 = adequate / problem named + appropriate next
        step; 5 = clear success / user goal observably achieved OR
        appropriate escalation taken.
        """
        user_goal = (
            (case_spec.persona.user_goal_summary or "").strip()
            or "(user_goal_summary not provided)"
        )
        outcome = trace.session_state.containment_outcome
        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate whether the bot helped the customer achieve the stated
USER GOAL, or appropriately escalated / deferred when the goal could
not be solved within the bot's authority.

The USER GOAL is the customer-perspective summary of what the
customer wants out of this conversation (derived from the source
session and curated into the case persona). The bot is "successful"
on this dim when the customer's goal is either resolved in-bot OR
appropriately escalated to a human with the right context handed
over.

USER GOAL: {user_goal}

SESSION OUTCOME: {outcome}

TRANSCRIPT:
{self._format_transcript(transcript)}

SCORING (1-5):
5 = Clear success — the user's goal was observably achieved, OR a fully appropriate escalation was taken with the right context handed over
4 = Mostly successful — goal substantively addressed with minor gaps; or appropriate escalation with small handover gaps
3 = Adequate — the bot named the problem and offered an appropriate next step, even if the goal was not fully resolved
2 = Insufficient — the bot engaged but missed the goal substantially (wrong UC, partial resolution, off-target advice)
1 = Clear failure — wrong UC, no resolution, no appropriate escalation, or the bot talked past the user's actual goal entirely

Respond with ONLY a JSON object: {{"score": <1-5>, "reasoning": "<brief explanation>"}}"""

        return self._call_llm("user_goal_achievement", prompt)

    def _judge_premature_finish(self, trace: TraceData, transcript: list[dict]) -> JudgeResult:
        """Did bot end conversation before issue was genuinely resolved?"""
        outcome = trace.session_state.containment_outcome
        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Determine whether the bot ended the conversation prematurely, before the user's issue was genuinely resolved.

SESSION OUTCOME: {outcome}

TRANSCRIPT:
{self._format_transcript(transcript)}

SCORING:
5 = The conversation ended naturally after the issue was fully resolved or properly escalated
1 = The bot ended the conversation prematurely; the user's issue was NOT resolved

This is a binary judgment: respond with score 5 if resolved appropriately, 1 if premature.

Respond with ONLY a JSON object: {{"score": <1 or 5>, "reasoning": "<brief explanation>"}}"""

        return self._call_llm("premature_finish", prompt)

    def _judge_stall_quality(self, trace: TraceData, transcript: list[dict]) -> JudgeResult:
        """Did bot move conversation forward at each turn?"""
        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate whether the bot moves the conversation forward productively at each turn, or if it stalls, repeats itself, or goes in circles.

TRANSCRIPT:
{self._format_transcript(transcript)}

SCORING (1-5):
5 = Every turn adds new information or meaningfully advances the conversation
4 = Mostly forward progress with minor repetition
3 = Some stalling or repetition but conversation eventually progresses
2 = Significant stalling; bot repeats the same information or asks the same questions
1 = Bot is stuck in a loop; no forward progress

Respond with ONLY a JSON object: {{"score": <1-5>, "reasoning": "<brief explanation>"}}"""

        return self._call_llm("stall_quality", prompt)

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _call_llm(self, dimension: str, prompt: str) -> JudgeResult:
        """Send prompt to LLM, parse score.  Retry once on parse failure.

        S-Eval-5: every JudgeResult is stamped with the dim's tier
        ``severity`` (advisory for the four demoted / supplementary
        dims, critical for ``premature_finish`` / ``stall_quality``)
        so ``composite.py`` can filter the L3 mean.
        """
        severity = self._severity_for(dimension)
        logger.info("LLM [judge:%s] call: model=%s", dimension, self._model)
        for attempt in range(_MAX_RETRIES + 1):
            try:
                kwargs: dict = {
                    "model": self._model,
                    "messages": [{"role": "user", "content": prompt}],
                }
                # Some providers pin the sampling temperature for a given
                # model and 400 on any explicit value (observed on
                # moonshot ``kimi-k2.6``: "invalid temperature: only 1 is
                # allowed for this model"). Once seen, this judge omits the
                # parameter for the rest of the run rather than degrading
                # every L3 dimension to the default score.
                if not self._temperature_unsupported:
                    kwargs["temperature"] = self._temperature
                response = self._client.chat.completions.create(**kwargs)
                content = response.choices[0].message.content or ""
                return self._parse_response(dimension, content, severity)
            except Exception as exc:
                if _is_temperature_rejection(exc) and not self._temperature_unsupported:
                    self._temperature_unsupported = True
                    _TEMPERATURE_PINNED_MODELS.add(self._model)
                    logger.warning(
                        "LlmJudge: model=%s rejected temperature=%s (%s); "
                        "re-issuing without the temperature parameter for the "
                        "rest of this run. Grading determinism is now the "
                        "provider's default, not %s.",
                        self._model, self._temperature, exc, self._temperature,
                    )
                    # Not a real failure: does not consume the retry budget.
                    try:
                        response = self._client.chat.completions.create(
                            model=self._model,
                            messages=[{"role": "user", "content": prompt}],
                        )
                        content = response.choices[0].message.content or ""
                        return self._parse_response(dimension, content, severity)
                    except Exception:
                        logger.warning(
                            "LlmJudge: retry without temperature also failed "
                            "for %s", dimension, exc_info=True,
                        )
                if attempt < _MAX_RETRIES:
                    logger.warning(
                        "LlmJudge: attempt %d for %s failed, retrying",
                        attempt + 1,
                        dimension,
                        exc_info=True,
                    )
                    continue
                logger.error(
                    "LlmJudge: all attempts for %s failed, using default score",
                    dimension,
                    exc_info=True,
                )
                return JudgeResult(
                    dimension=dimension,
                    score=_DEFAULT_SCORE,
                    reasoning="LLM call failed; default score applied",
                    severity=severity,
                    fallback_reason="llm_call_failed",
                )

        # Should not reach here, but be safe
        return JudgeResult(
            dimension=dimension,
            score=_DEFAULT_SCORE,
            reasoning="unexpected fallthrough",
            severity=severity,
            fallback_reason="unexpected_fallthrough",
        )

    @classmethod
    def _severity_for(cls, dimension: str) -> str:
        """Return the gate-severity for a given L3 dim.

        S-Eval-5 (M3-Eval): the three demoted dims plus the new
        ``user_goal_achievement`` are ``advisory``; everything else
        (``premature_finish`` / ``stall_quality``) keeps the legacy
        ``critical`` default so the gate-vs-advisory split is
        backward-compatible.
        """
        return "advisory" if dimension in cls._ADVISORY_DIMENSIONS else "critical"

    @classmethod
    def _parse_response(cls, dimension: str, content: str, severity: str | None = None) -> JudgeResult:
        """Extract score and reasoning from LLM response JSON.

        ``severity`` defaults to the dim's tier per
        :meth:`_severity_for` so the caller does not have to remember
        to pass it in every code path.
        """
        if severity is None:
            severity = cls._severity_for(dimension)
        # Try to find JSON in the response
        json_match = re.search(r"\{[^}]+\}", content, re.DOTALL)
        if json_match:
            try:
                data = json.loads(json_match.group())
                score = float(data.get("score", _DEFAULT_SCORE))
                score = max(1.0, min(5.0, score))  # clamp to 1-5
                reasoning = str(data.get("reasoning", ""))
                return JudgeResult(
                    dimension=dimension,
                    score=score,
                    reasoning=reasoning,
                    severity=severity,
                )
            except (json.JSONDecodeError, ValueError, TypeError):
                pass

        # Fallback: try to find a bare number
        num_match = re.search(r"\b([1-5])\b", content)
        if num_match:
            return JudgeResult(
                dimension=dimension,
                score=float(num_match.group(1)),
                reasoning=f"parsed bare score from: {content[:200]}",
                severity=severity,
            )

        logger.warning("LlmJudge: could not parse response for %s: %s", dimension, content[:200])
        return JudgeResult(
            dimension=dimension,
            score=_DEFAULT_SCORE,
            reasoning=f"parse failure; raw: {content[:200]}",
            severity=severity,
            fallback_reason="parse_failure",
        )

    @staticmethod
    def _format_transcript(transcript: list[dict]) -> str:
        """Format transcript into readable text for LLM prompts."""
        lines: list[str] = []
        for entry in transcript:
            role = entry.get("role", "unknown").upper()
            message = entry.get("message", "")
            turn_idx = entry.get("turn_index", "")
            prefix = f"[Turn {turn_idx}] " if turn_idx != "" else ""
            lines.append(f"{prefix}{role}: {message}")
        return "\n".join(lines) if lines else "(empty transcript)"
