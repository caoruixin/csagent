"""S-Auto-38 (Sprint 092) pre-dev zero-LLM sweep.

Confirms the 13 ``faq_miss``-flagged candidate cases (gate-reclassification
plan rev 2 §4) using ONLY existing baseline / historical run traces — NO new
LLM eval. For each case it emits:

  * persona ``will_request_human_if`` (drives the priority-1 user_requested path)
  * runtime / persona-allowed escalation paths
  * current ``expected.escalation_trigger`` and its eval-side reason family
  * the baseline per-attempt ``escalation_reason`` distribution (when traces exist)
  * a classification in {genuine_faq_miss, dual_path_conflict, unknown}

`unknown` is emitted whenever the evidence is insufficient (no baseline /
historical draws for the case) — we do NOT infer and do NOT auto-write any
override. This sweep confirms the override schema + replay blast radius; it is
NOT a per-case-override campaign.

Read-only: consumes only committed CaseSpec YAML + the gitignored baseline run
dir. Writes nothing. Run from repo root:

    python3 eval_interactive/analysis/sweep_s_auto_38_faq_miss_dualpath.py
"""

from __future__ import annotations

import json
import os
from collections import Counter

import yaml

# Eval-side reason-family map (mirror of
# hard_checks._ESCALATION_REASON_FAMILY — kept inline so the sweep does not
# import the scoring module it is auditing). Only the values the sweep needs.
_FAMILY = {
    "user_requested": "user_intent",
    "user_distress": "user_intent",
    "clarification_budget_exhausted": "bot_limit",
    "faq_miss_threshold_exceeded": "bot_limit",
    "turn_budget_exhausted": "bot_limit",
    "incomplete_intake": "bot_limit",
    "intake_complete_for_uc_g": "gdpr_identity",
    "intake_complete_for_uc_h": "appeal_review",
    "intake_complete_for_uc_i": "payment_dispute",
    "intake_complete_for_uc_j": "trust_safety",
    "intake_complete_for_uc_k": "tech_investigation",
    "trust_safety_required": "trust_safety",
    "imminent_harm": "trust_safety",
    "payment_dispute_detected": "payment_dispute",
    "appeal_requires_human": "appeal_review",
    "incorrect_deletion_appeal": "appeal_review",
    "gdpr_intake": "gdpr_identity",
    "identity_verification_required": "gdpr_identity",
    "account_compliance": "gdpr_identity",
    "out_of_scope": "service_degraded",
    "service_degraded": "service_degraded",
    "tool_scope_blocked": "service_degraded",
    "runtime_error_threshold": "service_degraded",
}

_HERE = os.path.dirname(os.path.abspath(__file__))
_EVAL = os.path.dirname(_HERE)                       # eval_interactive/
_REPO = os.path.dirname(_EVAL)                       # repo root
_BASELINE = os.path.join(
    _EVAL, "results", "m-auto-7-prepilot-baseline-20260608"
)

# 13 faq_miss-flagged cases (plan §4) → CaseSpec path (relative to repo root).
CASES = {
    "cs01n02_uc_c": "eval_interactive/case_specs/case_families/cs001_uc_c_template_escalate/neighbor/cs01n02_uc_c_messaging_general.yaml",
    "cs01s01_uc_c": "eval_interactive/case_specs_shadow/case_families/cs001_uc_c_template_escalate/cs01s01_uc_c_notification_lag_detailed.yaml",
    "cs01s02_uc_c": "eval_interactive/case_specs_shadow/case_families/cs001_uc_c_template_escalate/cs01s02_uc_c_inbox_empty_after_form.yaml",
    "cs11n01_uc_d": "eval_interactive/case_specs/case_families/cs011_uc_d_description_ignored/neighbor/cs11n01_uc_d_2fa_loop_verbose.yaml",
    "cs11s01_uc_d": "eval_interactive/case_specs_shadow/case_families/cs011_uc_d_description_ignored/cs11s01_uc_d_two_emails_one_account.yaml",
    "cs11s02_uc_d": "eval_interactive/case_specs_shadow/case_families/cs011_uc_d_description_ignored/cs11s02_uc_d_password_change_loop_high_distress.yaml",
    "cs59g01_uc_f": "eval_interactive/case_specs/case_families/cs259_uc_f_payment_question/negative/cs59g01_uc_f_obscure_payment_taxation.yaml",
    "cs92g01_uc_b": "eval_interactive/case_specs/case_families/cs192_uc_b_giveaway/negative/cs92g01_uc_b_obscure_policy_genuine_faq_miss.yaml",
    "cs95n01_uc_d": "eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/neighbor/cs95n01_uc_d_app_login_email_mismatch.yaml",
    "cs95n02_uc_d": "eval_interactive/case_specs/case_families/cs095_uc_classification_account_aware/neighbor/cs95n02_uc_d_no_ads_visible_state.yaml",
    "cs95s01_uc_d": "eval_interactive/case_specs_shadow/case_families/cs095_uc_classification_account_aware/cs95s01_uc_d_phone_changed_no_messages.yaml",
    "cs95s02_uc_d": "eval_interactive/case_specs_shadow/case_families/cs095_uc_classification_account_aware/cs95s02_uc_d_logged_in_different_browser_no_ads.yaml",
    "csmp_g01_uc_a": "eval_interactive/case_specs/case_families/manual_probe_uc_a_resolve_must/negative/csmp_g01_uc_a_genuine_faq_miss_obscure.yaml",
}

_PRIORITY1_FAMILY = "user_intent"  # user_requested / user_distress


def load_baseline_reason_dist() -> dict[str, Counter]:
    """case_id -> Counter of per-attempt escalation_reason across the baseline
    run (all three suites). Empty string => no handover that draw."""
    out: dict[str, Counter] = {}
    for suite in ("bad_cases", "anchor_outcome", "shadow"):
        p = os.path.join(_BASELINE, suite, "aggregated.json")
        if not os.path.exists(p):
            continue
        data = json.load(open(p))
        for c in data.get("case_results", []):
            cid = c.get("case_id", "")
            ctr: Counter = Counter()
            for a in c.get("attempts", []):
                if a.get("valid") is not True:
                    continue
                r = (a.get("escalation_reason") or "").strip() or "<none>"
                ctr[r] += 1
            if ctr:
                out[cid] = ctr
    return out


def classify(will_human: str, expected_trigger: str | None,
             dist: Counter | None) -> tuple[str, str]:
    """Return (classification, evidence_note).

    The decisive signal is the eval-side Part-2 *family-match rate*: the
    fraction of stamped (escalated) draws whose reason shares the expected
    trigger's family. This mirrors the real check semantics — within-family
    scatter (e.g. ``clarification_budget_exhausted`` vs expected
    ``faq_miss_threshold_exceeded``, both ``bot_limit``) PASSES Part-2, so it
    is NOT a conflict; only cross-family stamping (the priority-1
    ``user_intent`` path competing with a ``bot_limit`` expectation) is.
    """
    exp_fam = _FAMILY.get(expected_trigger or "")
    has_user_request_path = bool((will_human or "").strip())

    if dist is None:
        return "unknown", "no baseline/historical draws for this case"

    # Stamped (escalated) draws only — a <none> draw didn't escalate so Part-2
    # is not evaluated on it (Part-1 passes at medium risk).
    stamped = {r: n for r, n in dist.items() if r != "<none>"}
    n_stamped = sum(stamped.values())
    if n_stamped == 0:
        return "unknown", f"no escalated draws to evaluate Part-2 ({dict(dist)})"

    matched = sum(n for r, n in stamped.items()
                  if exp_fam is not None and _FAMILY.get(r) == exp_fam)
    user_intent = sum(n for r, n in stamped.items()
                      if _FAMILY.get(r) == _PRIORITY1_FAMILY)
    match_rate = matched / n_stamped

    # genuine_faq_miss: Part-2 family-match passes in the clear majority —
    # the reasons stay predominantly in the expected family even if a late
    # user-request fallback exists in the persona. Threshold 0.6.
    if match_rate >= 0.6:
        return ("genuine_faq_miss",
                f"family-match rate {matched}/{n_stamped}={match_rate:.2f} "
                f"(>=0.60); reasons predominantly in expected family={exp_fam} "
                f"({dict(stamped)})")

    # dual_path_conflict: family-match is split/low AND the priority-1
    # user_intent path (driven by will_request_human_if) materially drives the
    # cross-family stamping — the cs11s01 shape.
    if (has_user_request_path and exp_fam is not None
            and exp_fam != _PRIORITY1_FAMILY and user_intent > 0):
        return ("dual_path_conflict",
                f"family-match rate {matched}/{n_stamped}={match_rate:.2f} "
                f"(<0.60); will_request_human live and priority-1 user_intent "
                f"stamped on {user_intent}/{n_stamped} draws vs expected "
                f"family={exp_fam} ({dict(stamped)})")

    # Low family-match but not driven by the user-request path: insufficient
    # to call a dual-path conflict — record as unknown rather than infer.
    return ("unknown",
            f"family-match rate {matched}/{n_stamped}={match_rate:.2f} but no "
            f"user-request-driven cross-family scatter ({dict(stamped)}); "
            f"expected family={exp_fam}")


def main() -> None:
    dists = load_baseline_reason_dist()
    rows = []
    for cid_hint, relpath in CASES.items():
        path = os.path.join(_REPO, relpath)
        raw = yaml.safe_load(open(path))
        persona = raw.get("persona", {}) or {}
        expected = raw.get("expected", {}) or {}
        will_human = persona.get("will_request_human_if", "") or ""
        exp_trig = expected.get("escalation_trigger")
        should_esc = expected.get("should_escalate")
        risk = expected.get("risk_level")
        allow_bot = expected.get("allow_bot_resolution")
        primary_uc = expected.get("primary_uc")

        # Find the baseline dist by matching the full case_id.
        full_cid = raw.get("case_id", "")
        dist = dists.get(full_cid)

        cls, note = classify(will_human, exp_trig, dist)
        rows.append({
            "case": full_cid or cid_hint,
            "primary_uc": primary_uc,
            "should_escalate": should_esc,
            "risk": risk,
            "allow_bot_resolution": allow_bot,
            "expected_trigger": exp_trig,
            "expected_family": _FAMILY.get(exp_trig or ""),
            "will_request_human_if": will_human,
            "baseline_reason_dist": dict(dist) if dist else None,
            "classification": cls,
            "evidence": note,
        })

    print("=" * 100)
    print("S-Auto-38 pre-dev zero-LLM sweep — 13 faq_miss-flagged cases")
    print("=" * 100)
    for r in rows:
        print(f"\n### {r['case']}  [{r['primary_uc']}]  -> {r['classification'].upper()}")
        print(f"    should_escalate={r['should_escalate']} risk={r['risk']} "
              f"allow_bot_resolution={r['allow_bot_resolution']}")
        print(f"    expected_trigger={r['expected_trigger']} "
              f"(family={r['expected_family']})")
        print(f"    will_request_human_if={r['will_request_human_if']!r}")
        print(f"    baseline_reason_dist={r['baseline_reason_dist']}")
        print(f"    evidence: {r['evidence']}")

    print("\n" + "=" * 100)
    counts = Counter(r["classification"] for r in rows)
    print("CLASSIFICATION TOTALS:", dict(counts))
    # Emit machine-readable JSON too.
    print("\nJSON:")
    print(json.dumps(rows, indent=1, default=str))


if __name__ == "__main__":
    main()
