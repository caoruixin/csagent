"""CaseSpec data model.

Defines the data structures for evaluation case specifications,
matching Phase 5 section 3.4 of the design document.
"""

from __future__ import annotations

from dataclasses import dataclass, field


@dataclass
class FormContext:
    """Customer-submitted form fields that seed the session."""

    first_name: str
    email: str
    topic_subject: str
    ad_id: str = ""
    description: str = ""


@dataclass
class HiddenFact:
    """A fact the simulated customer knows but only reveals conditionally."""

    fact: str
    disclose_when: str  # "if asked", "after bot acknowledges issue", "proactively"


@dataclass
class Persona:
    """Behavioural profile for the simulated customer."""

    goal_summary: str
    frustration_level: str  # none | mild | high
    verbosity: str  # terse | normal | verbose
    drift_behavior: str  # none | minor | soft_shift | hard_shift
    seed_messages: list[str]
    hidden_facts: list[HiddenFact]
    will_request_human_if: str = ""


@dataclass
class Expected:
    """Ground-truth expectations for scoring."""

    outcome_class: str  # resolve | escalate
    primary_uc: str
    secondary_ucs: list[str]
    should_escalate: bool
    escalation_trigger: str = ""
    risk_level: str = "low"  # low | medium | high | critical
    expected_tool_sequence: list[str] = field(default_factory=list)
    forbidden_tools: list[str] = field(default_factory=list)
    grounding_mode: str = "faq_source_backed"  # faq_source_backed | fixed_script_only
    answer_must_not_contain: list[str] = field(default_factory=list)
    max_turns: int = 15


@dataclass
class ScoringConfig:
    """Which scoring dimensions to evaluate for this case."""

    hard_checks: list[str]
    outcome_checks: list[str]
    llm_judge_dimensions: list[str]


@dataclass
class CaseSpec:
    """Complete specification for a single evaluation case."""

    case_id: str
    source_session_id: str
    source_dataset: str
    form_context: FormContext
    persona: Persona
    expected: Expected
    scoring: ScoringConfig
