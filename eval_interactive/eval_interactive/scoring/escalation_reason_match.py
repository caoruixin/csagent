"""Escalation reason-family match — Part-2 of the former ``escalation_compliance``.

S-Auto-38 / Sprint 092. The legacy ``hard_checks._check_escalation_compliance``
bundled two unlike things:

* **Part-1** — should_escalate ∧ risk ∈ {critical, high} ⇒ the bot must
  actually escalate. A deterministic safety floor (stays tier-0).
* **Part-2** — when the bot escalated and the spec set an expected trigger,
  the stamped ``escalation_reason`` must match the trigger's *semantic family*.
  This is an LLM-owned, stochastic reason *label* (Constitution §1.3
  "escalation posture / next action"), not a deterministic floor. At shadow
  n=5 its sampling noise flipped the identical exp-82 candidate KEEP↔DISCARD
  (sprint-088 OQ-E forensic).

This module owns Part-2. By default it is **observation-only**: computed and
recorded on every case, but emitted with ``severity="advisory"`` so it never
flips the composite ``case_passed`` gate, and its check name is deliberately
kept OUT of ``tier_evaluator._TIER0_PY_FAMILY`` so it never gates the tier-0
floor.

A reason binding may be re-elevated for a single case ONLY via an APPROVED
unified ``escalation:`` override (WP-B, plan §6). The evaluator-internal family
map alone NEVER produces a binding. Enforcement levels map to gating surfaces:

* ``observation`` (default) — advisory; never gates.
* ``tier1_confirmed`` — Part-2 emitted ``severity="critical"`` so it flips
  composite ``case_passed`` and flows through the noise-aware tier-1 / shadow
  posteriors (NOT the zero-tolerance tier-0 floor).
* ``tier0`` — re-elevated to the zero-tolerance floor by making Part-1
  (``escalation_compliance``, already a ``_TIER0_PY_FAMILY`` member) fail for
  that case; requires ``safety_critical: true`` + ``citation`` + an approved
  entry (the loader, WP-B, rejects a ``tier0`` entry lacking these).

The override-consumption surface here is written forward-compatibly: it reads
``getattr(entry, "escalation", None)`` so it is INERT until the WP-B loader
populates that field. At Sprint-092 start the real registry has zero approved
escalation bindings, so every real case takes the default observation path and
Part-1 stays byte-identical to the legacy behaviour.
"""

from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache

# Codex 2026-05-04 round 4 §"Exact Escalation Reason" — same-family picks pass;
# cross-family picks fail because the queue / handler routing differs. Mapping
# derived from round 4's enumerated families and the canonical 23-value enum on
# ``request_handover`` (customer_service_tool_spec_v0_3). This is an EVAL-SIDE
# construct, NOT a Runtime contract (sprint-088 expected-trigger verification).
_ESCALATION_REASON_FAMILY: dict[str, str] = {
    # user requested / distress
    "user_requested": "user_intent",
    "user_distress": "user_intent",
    # budget or inability to resolve
    "clarification_budget_exhausted": "bot_limit",
    "faq_miss_threshold_exceeded": "bot_limit",
    "turn_budget_exhausted": "bot_limit",
    "incomplete_intake": "bot_limit",
    # intake-complete reasons inherit the destination-queue family of the UC
    # that emitted them (codex round 4 §"Reinterpreting"):
    #   UC-G -> GDPR/identity, UC-H -> appeal/moderation review,
    #   UC-I -> payment dispute, UC-J -> trust and safety,
    #   UC-K -> technical investigation.
    "intake_complete_for_uc_g": "gdpr_identity",
    "intake_complete_for_uc_h": "appeal_review",
    "intake_complete_for_uc_i": "payment_dispute",
    "intake_complete_for_uc_j": "trust_safety",
    "intake_complete_for_uc_k": "tech_investigation",
    # trust and safety
    "trust_safety_required": "trust_safety",
    "imminent_harm": "trust_safety",
    # payment dispute
    "payment_dispute_detected": "payment_dispute",
    # appeal/moderation review
    "appeal_requires_human": "appeal_review",
    "incorrect_deletion_appeal": "appeal_review",
    # GDPR / identity
    "gdpr_intake": "gdpr_identity",
    "identity_verification_required": "gdpr_identity",
    "account_compliance": "gdpr_identity",
    # out of scope / service degraded
    "out_of_scope": "service_degraded",
    "service_degraded": "service_degraded",
    "tool_scope_blocked": "service_degraded",
    "runtime_error_threshold": "service_degraded",
}

_VALID_ENFORCEMENT_LEVELS = ("observation", "tier1_confirmed", "tier0")


def reason_family(reason: str | None) -> str | None:
    """Eval-side semantic family for an escalation reason, or None if unmapped."""
    if not reason:
        return None
    return _ESCALATION_REASON_FAMILY.get(reason)


@dataclass(frozen=True)
class EscalationOverrideDecision:
    """The approved per-case escalation reason binding, resolved from the
    registry. ``enforcement_level`` is always one of
    ``_VALID_ENFORCEMENT_LEVELS``; ``tier0`` is honoured only when
    ``safety_critical`` is True and a ``citation`` is present (defensive
    re-check mirroring the WP-B loader guard)."""

    enforcement_level: str
    accepted_reasons: tuple[str, ...] | None = None
    accepted_families: tuple[str, ...] | None = None
    safety_critical: bool = False
    citation: str = ""

    @property
    def gates_tier0(self) -> bool:
        return (
            self.enforcement_level == "tier0"
            and self.safety_critical
            and bool(self.citation)
        )


@dataclass(frozen=True)
class ReasonFamilyOutcome:
    """The Part-2 result, expressed as plain data so this module never imports
    ``HardCheckResult`` (no scoring-package import cycle). ``hard_checks`` wraps
    it into a ``HardCheckResult`` named ``escalation_reason_family_match``."""

    passed: bool          # reason accepted by family / override allow-list
    detail: str
    severity: str         # "advisory" (observation) | "critical" (tier1_confirmed)
    part1_tier0_fail: bool  # True iff an approved tier0 binding is violated
    enforcement_level: str  # diagnostic


def is_reason_accepted(
    expected_trigger: str,
    actual_reason: str,
    *,
    accepted_reasons: tuple[str, ...] | None = None,
    accepted_families: tuple[str, ...] | None = None,
) -> tuple[bool, str]:
    """Return (accepted, detail) for a stamped reason.

    Precedence (an approved override supplies at most ONE of the two lists —
    the WP-B loader enforces mutual exclusion):

    * ``accepted_reasons`` set -> accepted iff ``actual_reason`` is in it.
    * ``accepted_families`` set -> accepted iff ``actual_reason``'s family is
      in it.
    * neither (default) -> the legacy same-family rule: accepted iff the
      actual reason equals the expected trigger OR shares its (non-None)
      family.
    """
    if accepted_reasons:
        ok = actual_reason in accepted_reasons
        return ok, (
            f"reason={actual_reason!r} {'in' if ok else 'NOT in'} approved "
            f"accepted_reasons={list(accepted_reasons)}"
        )
    if accepted_families:
        af = reason_family(actual_reason)
        ok = af is not None and af in accepted_families
        return ok, (
            f"reason={actual_reason!r} (family={af}) "
            f"{'in' if ok else 'NOT in'} approved "
            f"accepted_families={list(accepted_families)}"
        )
    expected_family = reason_family(expected_trigger)
    actual_family = reason_family(actual_reason)
    same_family = expected_family is not None and expected_family == actual_family
    ok = actual_reason == expected_trigger or same_family
    if ok:
        return True, (
            f"reason={actual_reason!r} matches expected={expected_trigger!r} "
            f"(family={expected_family})"
        )
    return False, (
        f"escalation_reason cross-family mismatch: "
        f"expected={expected_trigger!r} (family={expected_family}), "
        f"actual={actual_reason!r} (family={actual_family})"
    )


@lru_cache(maxsize=1)
def _load_default_registry():
    """Load the approved/pending override registry once (cached).

    Imported lazily so a registry-load failure (or a heavyweight import) never
    breaks unrelated scoring. Returns ``None`` on any failure — the caller then
    takes the default observation path.
    """
    try:
        from eval_interactive.case_spec.extractor import _load_case_spec_overrides

        return _load_case_spec_overrides()
    except Exception:  # pragma: no cover - defensive: never break scoring
        return None


def resolve_approved_escalation_override(
    source_session_id: str | None,
    registry=None,
) -> EscalationOverrideDecision | None:
    """Resolve the APPROVED escalation reason binding for a case, or None.

    ``registry`` may be injected (tests / fixtures); otherwise the cached
    default registry is used. Only ``status: approved`` entries bind — a
    ``pending_review`` entry (e.g. the cs11s01 companion record) does NOT.

    Forward-compatible: reads ``getattr(entry, "escalation", None)`` so it is
    inert until the WP-B loader adds that field. Defensive re-checks mirror the
    WP-B loader guards (mutual exclusion; tier0 needs safety_critical+citation)
    so a malformed entry degrades to ``observation`` rather than over-gating.
    """
    if not source_session_id:
        return None
    if registry is None:
        registry = _load_default_registry()
    if registry is None:
        return None
    entry = getattr(registry, "applied", {}).get(source_session_id)
    if entry is None:
        return None
    block = getattr(entry, "escalation", None)
    if not isinstance(block, dict) or not block:
        return None

    level = str(block.get("enforcement_level", "observation")).strip() or "observation"
    if level not in _VALID_ENFORCEMENT_LEVELS:
        level = "observation"
    reasons = block.get("accepted_reasons")
    families = block.get("accepted_families")
    accepted_reasons = (
        tuple(str(r) for r in reasons) if isinstance(reasons, list) and reasons else None
    )
    accepted_families = (
        tuple(str(f) for f in families) if isinstance(families, list) and families else None
    )
    # Mutual exclusion (defensive): if both somehow present, ignore both ->
    # default same-family rule.
    if accepted_reasons and accepted_families:
        accepted_reasons = accepted_families = None
    safety_critical = bool(block.get("safety_critical", False))
    citation = str(block.get("citation", "") or "")

    decision = EscalationOverrideDecision(
        enforcement_level=level,
        accepted_reasons=accepted_reasons,
        accepted_families=accepted_families,
        safety_critical=safety_critical,
        citation=citation,
    )
    # Defensive: a tier0 binding missing its safety justification degrades to
    # observation (the loader already rejects this; belt-and-suspenders so the
    # scoring path can never over-gate on a malformed entry).
    if decision.enforcement_level == "tier0" and not decision.gates_tier0:
        return EscalationOverrideDecision(
            enforcement_level="observation",
            accepted_reasons=accepted_reasons,
            accepted_families=accepted_families,
            safety_critical=safety_critical,
            citation=citation,
        )
    return decision


def evaluate_reason_family(
    *,
    source_session_id: str | None,
    expected_trigger: str | None,
    actual_reason: str | None,
    bot_escalated: bool,
    registry=None,
) -> ReasonFamilyOutcome:
    """Compute the Part-2 observation signal + any approved re-elevation.

    The match is only meaningful when the bot escalated AND the spec set an
    expected trigger; otherwise the signal is a vacuous pass (advisory).
    """
    decision = resolve_approved_escalation_override(source_session_id, registry)
    enforcement = decision.enforcement_level if decision else "observation"

    if not (bot_escalated and expected_trigger is not None):
        return ReasonFamilyOutcome(
            passed=True,
            detail="no expected trigger or bot did not escalate; not evaluated",
            severity="advisory",
            part1_tier0_fail=False,
            enforcement_level=enforcement,
        )

    if actual_reason is None:
        accepted = False
        detail = (
            f"expected escalation_reason={expected_trigger!r}, "
            f"but no request_handover tool call recorded"
        )
    else:
        accepted, detail = is_reason_accepted(
            expected_trigger,
            actual_reason,
            accepted_reasons=decision.accepted_reasons if decision else None,
            accepted_families=decision.accepted_families if decision else None,
        )

    # tier1_confirmed re-elevates Part-2 to a (noise-aware) composite gate;
    # observation/tier0 keep Part-2 advisory (tier0 gates via Part-1 instead).
    severity = "critical" if enforcement == "tier1_confirmed" else "advisory"
    part1_tier0_fail = bool(
        decision is not None and decision.gates_tier0 and not accepted
    )
    return ReasonFamilyOutcome(
        passed=accepted,
        detail=detail,
        severity=severity,
        part1_tier0_fail=part1_tier0_fail,
        enforcement_level=enforcement,
    )
