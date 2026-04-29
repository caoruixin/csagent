"""Per-Use-Case policy table.

Single source of truth mapping each V1 Use Case (UC-A .. UC-K) to the
expected bot behaviour the eval harness should grade against. Built from:

* ``docs/phase2_domain_realization_spec.md``
  - §2.2 Use Case definitions (per-UC ``allow_bot_resolution`` /
    ``outcome_class`` blocks, lines ~60-545)
  - §2.6 Control Policy by Use Case (lines ~692-1041) — used to derive
    ``required_intake_fields``
  - §2.10.1 Agent-visible tool matrix (lines ~1155-1163)
  - §2.10.2 Runtime-only tool matrix (lines ~1171-1187)
  - §2.10.3 Human-only tools (lines ~1189-1194)
  - §2.10.4 Per-UC tool sequences (lines ~1200-1213) — used to derive
    ``expected_tool_sequence_template``
* ``server/src/main/resources/config/tool-policy.yaml`` — used to
  cross-check the agent-visible / runtime-only / human-only split and to
  populate ``forbidden_tools_for_bot`` (only policy-reachable tools whose
  ``allowed_ucs`` excludes the current UC; human-only tools are NOT
  listed here because they are blocked globally by an L1 check).

Use ``get_policy(uc)`` to look up an entry and ``list_human_only_tools()``
to fetch the global human-only block-list.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Literal, Optional

from .schema import EscalationTrigger


# ---------------------------------------------------------------------------
# Type aliases
# ---------------------------------------------------------------------------

OutcomeClass = Literal["resolve", "escalate", "either"]
AllowBotResolution = Literal["true", "false", "partial"]
GroundingMode = Literal["faq_source_backed", "fixed_script_only"]


# ---------------------------------------------------------------------------
# Dataclass
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class UcPolicy:
    """Canonical per-UC expectation block."""

    uc: str
    outcome_class: OutcomeClass
    allow_bot_resolution: AllowBotResolution
    should_escalate_default: bool
    expected_tool_sequence_template: list[str]
    forbidden_tools_for_bot: list[str]
    grounding_mode: GroundingMode
    required_intake_fields: list[str] = field(default_factory=list)
    default_escalation_trigger: Optional[EscalationTrigger] = None


# ---------------------------------------------------------------------------
# Human-only tool list (visibility=human_only)
# ---------------------------------------------------------------------------

# Source: phase2 §2.10.3 (lines 1189-1194) and tool_spec_v0_2.yaml.
# tool-policy.yaml currently does NOT enumerate human-only tools (only
# AGENT_VISIBLE / RUNTIME_ONLY are listed) -- this list comes from the
# canonical tool spec. Kept as a module-level constant so a global L1
# check can reference it without rebuilding the policy table.
_HUMAN_ONLY_TOOLS: tuple[str, ...] = (
    "moderation_enforcement_action",
    "send_followup_email_or_async_update",
)


def list_human_only_tools() -> list[str]:
    """Return the list of tools the bot must never call regardless of UC.

    Source: phase2 §2.10.3 (lines 1189-1194) +
    ``customer_service_tool_spec_v0_2.yaml`` ``visibility: human_only``.
    """

    return list(_HUMAN_ONLY_TOOLS)


# ---------------------------------------------------------------------------
# Helpers for forbidden-tool computation
# ---------------------------------------------------------------------------

# Mirrors server/src/main/resources/config/tool-policy.yaml. Listing it
# here (rather than reading the YAML at import time) keeps the policy
# table self-contained and reviewable in code review. A drift test
# elsewhere can compare the two.
_TOOL_ALLOWED_UCS: dict[str, frozenset[str]] = {
    # AGENT_VISIBLE
    "search_knowledge": frozenset({"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"}),
    "resolve_article": frozenset({"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"}),
    "get_customer_context": frozenset({"UC-A", "UC-C", "UC-D", "UC-F", "UC-FP", "UC-K"}),
    "request_handover": frozenset({"ALL"}),
    "record_outcome": frozenset({"ALL"}),
    # RUNTIME_ONLY (model cannot invoke directly, but if it tries the
    # scope check still applies, so we include them in the forbidden
    # computation when allowed_ucs excludes current UC).
    "lookup_customer_account": frozenset({"UC-C", "UC-D", "UC-FP", "UC-K"}),
    "lookup_listing_or_ad": frozenset({"UC-A", "UC-C", "UC-FP", "UC-K"}),
    "get_moderation_review_context": frozenset({"UC-A", "UC-FP"}),
    "get_message_moderation_context": frozenset({"UC-C"}),
    "create_case_controlled": frozenset({"UC-H", "UC-J", "UC-K"}),
}


def _forbidden_for(uc: str) -> list[str]:
    """Return policy-reachable tools the bot must NOT call for ``uc``.

    Excludes:
    * Tools whose allowed-set contains "ALL" (always permitted).
    * Tools whose allowed-set already contains ``uc``.
    * Human-only tools (handled by a global L1 check).
    """

    forbidden: list[str] = []
    for tool, allowed in _TOOL_ALLOWED_UCS.items():
        if "ALL" in allowed:
            continue
        if uc in allowed:
            continue
        forbidden.append(tool)
    return sorted(forbidden)


# ---------------------------------------------------------------------------
# Policy table
# ---------------------------------------------------------------------------

# Each entry cites the phase2 §2.2 line where the UC's
# ``allow_bot_resolution`` and ``outcome_class`` are declared, plus the
# §2.10.4 line where the canonical tool sequence is defined.
_POLICIES: dict[str, UcPolicy] = {
    # ── FAQ-resolvable, low-risk ─────────────────────────────────────────
    "UC-A": UcPolicy(
        uc="UC-A",
        # phase2 §2.2 lines 66, 78
        allow_bot_resolution="true",
        outcome_class="resolve",
        should_escalate_default=False,
        # phase2 §2.10.4 line 1202
        expected_tool_sequence_template=[
            "get_customer_context",
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-A"),
        # phase2 §2.5 line 657 (Help Centre articles + grounded answer)
        grounding_mode="faq_source_backed",
        required_intake_fields=[],
        default_escalation_trigger=None,
    ),
    "UC-B": UcPolicy(
        uc="UC-B",
        # phase2 §2.2 lines 105, 117
        allow_bot_resolution="true",
        outcome_class="resolve",
        should_escalate_default=False,
        # phase2 §2.10.4 line 1203 (no get_customer_context for UC-B)
        expected_tool_sequence_template=[
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-B"),
        grounding_mode="faq_source_backed",
        required_intake_fields=[],
        default_escalation_trigger=None,
    ),
    "UC-C": UcPolicy(
        uc="UC-C",
        # phase2 §2.2 lines 147, 159
        allow_bot_resolution="true",
        outcome_class="resolve",
        should_escalate_default=False,
        # phase2 §2.10.4 line 1204 (clean path)
        expected_tool_sequence_template=[
            "get_customer_context",
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-C"),
        grounding_mode="faq_source_backed",
        required_intake_fields=[],
        default_escalation_trigger=None,
    ),
    "UC-D": UcPolicy(
        uc="UC-D",
        # phase2 §2.2 lines 188, 202
        allow_bot_resolution="true",
        outcome_class="resolve",
        should_escalate_default=False,
        # phase2 §2.10.4 line 1205
        expected_tool_sequence_template=[
            "get_customer_context",
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-D"),
        grounding_mode="faq_source_backed",
        required_intake_fields=[],
        default_escalation_trigger=None,
    ),
    "UC-E": UcPolicy(
        uc="UC-E",
        # phase2 §2.2 lines 227, 239
        allow_bot_resolution="true",
        outcome_class="resolve",
        should_escalate_default=False,
        # phase2 §2.10.4 line 1206 (no get_customer_context for UC-E)
        expected_tool_sequence_template=[
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-E"),
        grounding_mode="faq_source_backed",
        required_intake_fields=[],
        default_escalation_trigger=None,
    ),
    "UC-F": UcPolicy(
        uc="UC-F",
        # phase2 §2.2 lines 265, 279
        allow_bot_resolution="true",
        outcome_class="resolve",
        should_escalate_default=False,
        # phase2 §2.10.4 line 1207
        expected_tool_sequence_template=[
            "get_customer_context",
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-F"),
        grounding_mode="faq_source_backed",
        required_intake_fields=[],
        default_escalation_trigger=None,
    ),
    "UC-FP": UcPolicy(
        uc="UC-FP",
        # phase2 §2.2 lines 310, 324 (allow_bot_resolution=true,
        # outcome_class=resolve at the canonical happy path; appeals are
        # handled by routing to UC-H)
        allow_bot_resolution="true",
        outcome_class="resolve",
        should_escalate_default=False,
        # phase2 §2.10.4 line 1208
        expected_tool_sequence_template=[
            "get_customer_context",
            "search_knowledge",
            "resolve_article",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-FP"),
        grounding_mode="faq_source_backed",
        required_intake_fields=[],
        default_escalation_trigger=None,
    ),
    # ── Intake + Handover ────────────────────────────────────────────────
    "UC-G": UcPolicy(
        uc="UC-G",
        # phase2 §2.2 lines 351, 362
        allow_bot_resolution="false",
        outcome_class="escalate",
        should_escalate_default=True,
        # phase2 §2.10.4 line 1209 (UC-G has no create_case_controlled --
        # it is intake + handover only)
        expected_tool_sequence_template=[
            "request_handover",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-G"),
        grounding_mode="fixed_script_only",
        # phase2 §2.6 lines 905-911 (UC-G bot_collectable_fields).
        # Required = request_type + registered_email; the rest are
        # optional / pre-chat-form-derived. ``problem_description`` is
        # already supplied by the pre-chat form so it does not need to be
        # collected by the bot at intake-time.
        required_intake_fields=[
            "request_type",
            "registered_email",
        ],
        # phase2 §2.4 line 620 + §2.6 line 926 (intake_complete_for_uc_g
        # is the canonical happy-path trigger). ``gdpr_intake`` is the
        # routing trigger; the actual handover trigger after intake is
        # ``intake_complete_for_uc_g``.
        default_escalation_trigger="intake_complete_for_uc_g",
    ),
    "UC-H": UcPolicy(
        uc="UC-H",
        # phase2 §2.2 lines 390, 402
        allow_bot_resolution="false",
        outcome_class="escalate",
        should_escalate_default=True,
        # phase2 §2.10.4 line 1210
        expected_tool_sequence_template=[
            "create_case_controlled",
            "request_handover",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-H"),
        grounding_mode="fixed_script_only",
        # phase2 §2.6 lines 944-949
        required_intake_fields=[
            "ad_id_or_listing_url",
            "registered_email",
        ],
        # phase2 §2.4 line 614 (appeal_requires_human is the routing
        # trigger from UC-FP; once intake is complete the trigger is
        # intake_complete_for_uc_h per §2.4 line 611).
        default_escalation_trigger="intake_complete_for_uc_h",
    ),
    "UC-I": UcPolicy(
        uc="UC-I",
        # phase2 §2.2 lines 441, 451
        allow_bot_resolution="false",
        outcome_class="escalate",
        should_escalate_default=True,
        # phase2 §2.10.4 line 1211 (UC-I does NOT call
        # create_case_controlled -- handover only)
        expected_tool_sequence_template=[
            "request_handover",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-I"),
        grounding_mode="fixed_script_only",
        # phase2 §2.6 lines 974-978
        required_intake_fields=[
            "registered_email",
            "ad_id_or_order_reference",
            "brief_description",
        ],
        # phase2 §2.4 line 613 (payment_dispute_detected is routing /
        # immediate-escalate trigger; intake_complete_for_uc_i is
        # post-intake). UC-I per §2.6 escalates immediately on dispute
        # keywords without waiting for full intake, so the *default*
        # trigger is the dispute trigger rather than the intake one.
        default_escalation_trigger="payment_dispute_detected",
    ),
    "UC-J": UcPolicy(
        uc="UC-J",
        # phase2 §2.2 lines 482, 491
        allow_bot_resolution="false",
        outcome_class="escalate",
        should_escalate_default=True,
        # phase2 §2.10.4 line 1212
        expected_tool_sequence_template=[
            "create_case_controlled",
            "request_handover",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-J"),
        grounding_mode="fixed_script_only",
        # phase2 §2.6 lines 998-1001
        required_intake_fields=[
            "report_target",
            "report_type",
            "description",
        ],
        # phase2 §2.4 line 618 (trust_safety_required is the canonical
        # routing+handover trigger for UC-J).
        default_escalation_trigger="trust_safety_required",
    ),
    "UC-K": UcPolicy(
        uc="UC-K",
        # phase2 §2.2 line 526 (allow_bot_resolution=partial) and
        # line 538 (outcome_class=escalate, but commentary says some
        # cases are bot-resolved). Per Wave A1.1 brief, "either" is the
        # canonical outcome_class for partial-resolution UCs.
        allow_bot_resolution="partial",
        outcome_class="either",
        # TODO(policy-review): UC-K is the only "either" UC. The Wave
        # A1.1 brief specifies should_escalate_default=true vs false is
        # ambiguous when outcome_class=either; we default to True to
        # match phase2 §2.2 line 538 ("大多数 K 类会 escalate"). The
        # case-level should_escalate field is what scoring actually
        # consults -- this default is only used when a CaseSpec author
        # has not specified one.
        should_escalate_default=True,
        # phase2 §2.10.4 line 1213 -- escalate path; resolve path
        # collapses to [get_customer_context, record_outcome]. We use
        # the escalate path as the canonical template because phase2
        # §2.2 line 538 says most UC-K cases escalate.
        expected_tool_sequence_template=[
            "get_customer_context",
            "create_case_controlled",
            "request_handover",
            "record_outcome",
        ],
        forbidden_tools_for_bot=_forbidden_for("UC-K"),
        grounding_mode="fixed_script_only",
        # phase2 §2.6 lines 1025-1028
        required_intake_fields=[
            "platform",
            "repro_steps_or_error_message",
        ],
        # phase2 §2.4 line 611 (intake_complete_for_uc_k).
        default_escalation_trigger="intake_complete_for_uc_k",
    ),
}


# ---------------------------------------------------------------------------
# Public API
# ---------------------------------------------------------------------------


def get_policy(uc: str) -> UcPolicy:
    """Return the policy entry for ``uc``.

    ``uc`` may be either the bare prefix (e.g. ``"UC-A"``) or the full
    Phase 2 use-case-id form (e.g. ``"UC-A-01"``). The trailing
    ``-NN`` suffix is stripped before lookup so callers do not have to
    normalise themselves.
    """

    key = uc.strip()
    if not key:
        raise KeyError("uc must be a non-empty string")
    # Strip the "-01" suffix variant.
    if key.count("-") >= 2:
        head, mid, *_ = key.split("-")
        key = f"{head}-{mid}"
    if key not in _POLICIES:
        raise KeyError(f"No policy entry for use case {uc!r} (normalised to {key!r})")
    return _POLICIES[key]


def list_use_cases() -> list[str]:
    """Return the canonical list of UC keys covered by this table."""

    return list(_POLICIES.keys())
