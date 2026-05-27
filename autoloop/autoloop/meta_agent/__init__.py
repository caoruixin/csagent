"""autoloop.meta_agent — LLM-driven analyzer + proposer + lessons compactor.

S-Auto-3 deliverable. Three roles, three prompt files, one shared LLM
client. The meta-agent is the only place auto-loop calls an external
LLM during a live iteration; everything else (sandbox, applier,
scoring) is deterministic.

Boundary: meta-agent NEVER sees per-case shadow detail (firewall in
`tier_evaluator.evaluate(...)`). It only sees aggregate Layer 4
`{regression_detected, drop_pct}` keys. This is enforced structurally
in `loop.py` by what it passes through to `proposer.propose(...)`.

The three prompts live in `prompts/{analyze,propose,compact}.txt` —
they are the structural defenses against semantic hardcode in
propose-stage outputs (per `program.md` §5 §1.7 mapping).
"""

from .analyzer import FailureTaxonomy, analyze
from .lessons_compactor import compact
from .llm_client import LLMClient, LLMClientError, build_client_from_config
from .proposer import (
    Hypothesis,
    ProposerInvalidOutputError,
    fingerprint_hypothesis,
    propose,
)

__all__ = [
    "FailureTaxonomy",
    "Hypothesis",
    "LLMClient",
    "LLMClientError",
    "ProposerInvalidOutputError",
    "analyze",
    "build_client_from_config",
    "compact",
    "fingerprint_hypothesis",
    "propose",
]
