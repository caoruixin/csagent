"""CaseSpec data model.

Defines the data structures for evaluation case specifications,
matching Phase 5 section 3.4 of the design document and the canonical
``request_handover.escalation_reason`` enum from
``docs/customer_service_tool_spec_v0_2.yaml`` (lines 433-457).

Wave A1.1 changes (2026-04-27):
* Added ``Expected.allow_bot_resolution`` (true / false / partial)
* Added ``Expected.bot_handling_pattern`` (plain-English bot pattern)
* Renamed ``Persona.goal_summary`` -> ``Persona.user_goal_summary`` so it is
  unambiguously the *user's* goal rather than the expected bot pattern.
* Promoted ``Expected.escalation_trigger`` to the canonical
  ``EscalationTrigger`` Literal and added a cross-field validator: if
  ``should_escalate`` is True the trigger must be non-empty; if False it
  must be empty / None.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Literal, Optional, get_args

_log = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Canonical enums
# ---------------------------------------------------------------------------

# Source of truth: docs/customer_service_tool_spec_v0_2.yaml lines 433-457
# (request_handover.input_schema.properties.escalation_reason.enum).
# Cross-referenced with phase2_domain_realization_spec.md §2.4 (Escalation
# Matrix, lines 606-625). The tool-spec enum is authoritative because it is
# what the Bot's ``request_handover`` tool actually validates against.
#
# NOTE(policy-review): Wave A1.1 brief asked for "17 values" (per
# phase5_evaluation_design.md §4.1 line 316), but the canonical
# ``request_handover.escalation_reason`` enum currently lists 23 values
# (the five ``intake_complete_for_uc_<g|h|i|j|k>`` values are spelled out
# individually, and ``service_degraded`` / ``turn_budget_exhausted`` /
# ``tool_scope_blocked`` / ``runtime_error_threshold`` are present even
# though §2.4 marks them as infrastructure / guardrail-only). We use the
# canonical 23 here; the scoring layer can decide which subset is
# exercised by the eval suite.
EscalationTrigger = Literal[
    "user_requested",
    "faq_miss_threshold_exceeded",
    "clarification_budget_exhausted",
    "intake_complete_for_uc_g",
    "intake_complete_for_uc_h",
    "intake_complete_for_uc_i",
    "intake_complete_for_uc_j",
    "intake_complete_for_uc_k",
    "incomplete_intake",
    "payment_dispute_detected",
    "appeal_requires_human",
    "user_distress",
    "imminent_harm",
    "incorrect_deletion_appeal",
    "trust_safety_required",
    "account_compliance",
    "gdpr_intake",
    "identity_verification_required",
    "out_of_scope",
    "service_degraded",
    "turn_budget_exhausted",
    "tool_scope_blocked",
    "runtime_error_threshold",
]

ESCALATION_TRIGGER_VALUES: tuple[str, ...] = get_args(EscalationTrigger)


AllowBotResolution = Literal["true", "false", "partial"]


# ---------------------------------------------------------------------------
# Sub-models
# ---------------------------------------------------------------------------


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
    """Behavioural profile for the simulated customer.

    ``user_goal_summary`` describes what the *user* wants and is derived from
    ``form_context`` + ``drift_type``. It is intentionally distinct from
    ``Expected.bot_handling_pattern`` (which describes what the bot is
    expected to do).
    """

    user_goal_summary: str
    frustration_level: str  # none | mild | high
    verbosity: str  # terse | normal | verbose
    drift_behavior: str  # none | minor | soft_shift | hard_shift
    seed_messages: list[str]
    hidden_facts: list[HiddenFact]
    will_request_human_if: str = ""

    # ----- Backwards-compat shim (Wave A2.1) -----
    # Some callers still read ``persona.goal_summary`` (the pre-A1.1 name).
    # Expose a read-only deprecated property that delegates to
    # ``user_goal_summary`` so those callers keep working until they are
    # migrated. Do NOT add a setter -- writes must use the new field name.
    @property
    def goal_summary(self) -> str:  # pragma: no cover - trivial alias
        """Deprecated: use ``user_goal_summary``. Kept for callers that
        still reference the pre-A1.1 attribute name (loader.py, simulator
        prompt template, legacy tests). Will be removed once all callers
        are migrated."""
        return self.user_goal_summary


@dataclass
class Expected:
    """Ground-truth expectations for scoring.

    Wave A1.1 fields:
    * ``allow_bot_resolution`` mirrors the per-UC policy
      (phase2 §2.2 / §2.6) at the case level so the scorer does not have to
      cross-look up the policy table for individual cases.
    * ``bot_handling_pattern`` is a plain-English description of the
      expected bot behaviour, distinct from ``outcome_class``. Example for
      UC-H: "Intake user's case details, create a controlled case, hand
      over to human agent".
    * ``escalation_trigger`` is constrained to the canonical
      ``EscalationTrigger`` enum. The post-init validator enforces the
      ``should_escalate`` <-> ``escalation_trigger`` coupling.
    """

    outcome_class: str  # resolve | escalate | either
    primary_uc: str
    secondary_ucs: list[str]
    should_escalate: bool
    allow_bot_resolution: AllowBotResolution  # true | false | partial (required)
    # S-Eval-1 (M3-Eval): demoted from required str to Optional[str] per the
    # four-tier pyramid -- bot_handling_pattern is procedural narrative, no
    # longer a hard gate. Existing fixtures that carry a string keep their
    # non-empty validation; outcome-only cases may omit it.
    bot_handling_pattern: Optional[str] = None
    escalation_trigger: Optional[EscalationTrigger] = None
    risk_level: str = "low"  # low | medium | high | critical
    expected_tool_sequence: list[str] = field(default_factory=list)
    forbidden_tools: list[str] = field(default_factory=list)
    grounding_mode: str = "faq_source_backed"  # faq_source_backed | fixed_script_only
    answer_must_not_contain: list[str] = field(default_factory=list)
    max_turns: int = 15
    # Codex 2026-05-04 round 4 §"Resolve vs Escalate" / §"Recommended
    # Next Implementation" — when populated, ``correct_outcome`` passes
    # if the actual outcome is in this list. Falls back to the legacy
    # single ``outcome_class`` match when omitted, so existing specs are
    # unaffected. Element values mirror ``outcome_class`` (resolve /
    # escalate / abandoned).
    acceptable_outcomes: list[str] = field(default_factory=list)

    def __post_init__(self) -> None:
        # ----- allow_bot_resolution domain -----
        allowed_resolution = get_args(AllowBotResolution)
        if self.allow_bot_resolution not in allowed_resolution:
            raise ValueError(
                f"allow_bot_resolution must be one of {allowed_resolution!r}; "
                f"got {self.allow_bot_resolution!r}"
            )

        # ----- bot_handling_pattern shape -----
        # S-Eval-1 (M3-Eval): None is now acceptable (outcome-only cases).
        # When a string is supplied, it must still be non-empty so legacy
        # fixtures cannot silently drift to a blank value.
        if self.bot_handling_pattern is not None:
            if not isinstance(self.bot_handling_pattern, str) or not self.bot_handling_pattern.strip():
                raise ValueError(
                    "bot_handling_pattern, when supplied, must be a non-empty string"
                )

        # ----- escalation_trigger / should_escalate coupling -----
        trigger = self.escalation_trigger
        # Treat empty string as null to be tolerant of YAML round-trips.
        if isinstance(trigger, str) and not trigger.strip():
            trigger = None
            self.escalation_trigger = None

        if self.should_escalate:
            if trigger is None:
                # S-Eval-1 (M3-Eval): demoted from hard ValueError to a
                # diagnostic-only warning. Outcome-only cases may declare
                # `should_escalate=true` without committing to a canonical
                # trigger family; the scoring layer treats trigger-absent
                # specs as advisory (escalation_compliance skips the
                # family-match path when trigger is None, and
                # escalation_reason_consistency is tagged advisory in the
                # same condition). The runtime contract is unchanged.
                _log.warning(
                    "Expected.escalation_trigger is None with should_escalate=true; "
                    "family-match scoring will be treated as advisory for this case."
                )
            elif trigger not in ESCALATION_TRIGGER_VALUES:
                raise ValueError(
                    f"escalation_trigger must be one of "
                    f"{ESCALATION_TRIGGER_VALUES!r}; got {trigger!r}"
                )
        else:
            if trigger is not None:
                raise ValueError(
                    "escalation_trigger must be empty/null when should_escalate=false"
                )


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
    # S-Eval-1 (M3-Eval): observable end-state the case is "resolved" against,
    # written in customer-perspective natural language. Used by anchor_outcome
    # and bad-case manual review at sprint / milestone close; not a programmatic
    # gate. Placed at CaseSpec level (not on Expected) because the criterion
    # is the case-level closure target, not a sub-field of the
    # expected-bot-behaviour block. Backward-compatible: None on every existing
    # smoke / anchor / case-family fixture.
    closure_criterion: Optional[str] = None
