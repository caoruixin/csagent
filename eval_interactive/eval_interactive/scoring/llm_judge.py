"""LLM Judge -- L3 semantic quality scoring via LLM (1-5 scale)."""

from __future__ import annotations

import json
import logging
import re
from dataclasses import dataclass

from openai import OpenAI

from eval_interactive.case_spec.schema import CaseSpec
from eval_interactive.config import Config
from eval_interactive.trace.models import TraceData

logger = logging.getLogger(__name__)

_DEFAULT_SCORE = 3.0
_MAX_RETRIES = 1


@dataclass
class JudgeResult:
    """Score for a single LLM-judged dimension."""

    dimension: str
    score: float  # 1 - 5
    reasoning: str = ""


class LlmJudge:
    """L3 LLM Judge -- semantic quality scoring via LLM."""

    ALL_DIMENSIONS = [
        "groundedness",
        "relevance",
        "tone_appropriateness",
        "premature_finish",
        "stall_quality",
    ]

    def __init__(self, config: Config):
        """Initialize with OpenAI-compatible client.

        Args:
            config: Application config with llm section.
        """
        self._model = config.llm.model
        self._temperature = config.llm.temperature  # 0.0 for grading
        self._client = OpenAI(
            base_url=config.llm.base_url or None,
            api_key=config.llm.api_key or "not-set",
        )
        logger.info("LlmJudge initialized: model=%s, base_url=%s", self._model, config.llm.base_url)

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

        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate how well the bot's responses are grounded in retrieved knowledge sources.

IMPORTANT: If the bot's response is purely procedural or an escalation message (e.g.,
"transferring you to a human agent", "let me connect you with a specialist") with no
factual claims about the user's issue, score 5 — there are no claims that require
grounding. Only score low when the bot makes factual claims without citing sources.

TRANSCRIPT:
{self._format_transcript(transcript)}

SOURCE CITATIONS PER TURN:
{sources_block}

SCORING (1-5):
5 = Every factual claim is backed by a cited source, OR no factual claims were made (e.g., escalation/procedural messages)
4 = Most claims are grounded, minor unsourced details
3 = Some claims are grounded but notable gaps exist
2 = Few claims are grounded; bot invents information
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
        prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate the bot's tone and professionalism.

CONTEXT: The customer's frustration level is "{frustration}".

TRANSCRIPT:
{self._format_transcript(transcript)}

SCORING (1-5):
5 = Professional, empathetic, appropriately warm; matches the customer's emotional state
4 = Generally professional with minor tone issues
3 = Adequate but somewhat mechanical or slightly dismissive
2 = Tone is inappropriate (overly casual, condescending, or ignoring frustration)
1 = Rude, dismissive, or completely tone-deaf

Respond with ONLY a JSON object: {{"score": <1-5>, "reasoning": "<brief explanation>"}}"""

        return self._call_llm("tone_appropriateness", prompt)

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
        """Send prompt to LLM, parse score.  Retry once on parse failure."""
        logger.info("LLM [judge:%s] call: model=%s", dimension, self._model)
        for attempt in range(_MAX_RETRIES + 1):
            try:
                response = self._client.chat.completions.create(
                    model=self._model,
                    temperature=self._temperature,
                    messages=[{"role": "user", "content": prompt}],
                )
                content = response.choices[0].message.content or ""
                return self._parse_response(dimension, content)
            except Exception:
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
                )

        # Should not reach here, but be safe
        return JudgeResult(dimension=dimension, score=_DEFAULT_SCORE, reasoning="unexpected fallthrough")

    @staticmethod
    def _parse_response(dimension: str, content: str) -> JudgeResult:
        """Extract score and reasoning from LLM response JSON."""
        # Try to find JSON in the response
        json_match = re.search(r"\{[^}]+\}", content, re.DOTALL)
        if json_match:
            try:
                data = json.loads(json_match.group())
                score = float(data.get("score", _DEFAULT_SCORE))
                score = max(1.0, min(5.0, score))  # clamp to 1-5
                reasoning = str(data.get("reasoning", ""))
                return JudgeResult(dimension=dimension, score=score, reasoning=reasoning)
            except (json.JSONDecodeError, ValueError, TypeError):
                pass

        # Fallback: try to find a bare number
        num_match = re.search(r"\b([1-5])\b", content)
        if num_match:
            return JudgeResult(
                dimension=dimension,
                score=float(num_match.group(1)),
                reasoning=f"parsed bare score from: {content[:200]}",
            )

        logger.warning("LlmJudge: could not parse response for %s: %s", dimension, content[:200])
        return JudgeResult(
            dimension=dimension,
            score=_DEFAULT_SCORE,
            reasoning=f"parse failure; raw: {content[:200]}",
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
