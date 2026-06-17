"""S-Auto-38 (Sprint 092) zero-LLM OLD/NEW gate-split replay (plan §7).

Pure-Python re-score of recorded traces — NO backend, NO LLM, NO new draws. For
the baseline + the exp-N candidate runs it recomputes, per recorded draw, the
escalation_compliance split using the REAL split check code
(``HardChecker._check_escalation_compliance`` =Part-1,
``_check_escalation_reason_family_match`` =Part-2), majority-collapses, and
compares the tier-0 verdict OLD vs NEW.

OLD escalation_compliance (the legacy bundled check) is reconstructed as
``Part-1 AND Part-2`` per draw and VALIDATED against the stored
``tier0_majority['escalation_compliance']`` (a self-check that the reconstruction
is faithful to what the OLD code actually produced). NEW tier-0 uses Part-1 only
(Part-2 is observation-only, excluded from the tier-0 family).

Emits: the §7.2 matrix, the blast-radius list, the delta-vs-baseline
flip-elimination (exp-82 orig vs exp82-reval), the negative control, and the
other-invariant (4 deterministic safety checks) byte-identity check.

Run from repo root:
    cd eval_interactive && uv run python analysis/replay_s_auto_38_gate_split.py
"""

from __future__ import annotations

import glob
import json
import os
from collections import Counter

from eval_interactive.case_spec.loader import load_case_specs
from eval_interactive.scoring.hard_checks import HardChecker
from eval_interactive.trace.models import SessionState, TraceData, TurnTrace

_HERE = os.path.dirname(os.path.abspath(__file__))
_EVAL = os.path.dirname(_HERE)
_REPO = os.path.dirname(_EVAL)

# The four deterministic safety checks that must be byte-identical OLD vs NEW
# (Part-1 escalation_compliance is the fifth tier-0 member, tracked separately).
_OTHER_TIER0 = (
    "no_pii_leakage",
    "no_human_only_tool_exposure",
    "no_critical_policy_violation",
    "phase_transition_validity",
)

_SUITES = ("bad_cases", "anchor_outcome", "shadow")
_CHECKER = HardChecker()


# --- CaseSpec index --------------------------------------------------------

def _spec_index() -> dict:
    idx = {}
    for d in ("case_specs", "case_specs_shadow"):
        root = os.path.join(_EVAL, d)
        if not os.path.isdir(root):
            continue
        for spec in load_case_specs(root):
            idx[spec.case_id] = spec
    return idx


# --- per-draw split recompute (uses the REAL check code) -------------------

def _trace(reason: str | None, containment: str) -> TraceData:
    if reason:
        turns = [TurnTrace(
            turn_index=1, user_message="u", bot_response="b",
            tool_calls=[{"tool_name": "request_handover",
                         "arguments": {"escalation_reason": reason}}],
            source_ids=[], phase_before="DISCOVER", phase_after="ESCALATE",
            active_use_case="", latency_ms=0, projected_context={})]
    else:
        turns = []
    return TraceData(
        session_state=SessionState(
            session_id="replay", active_use_case="", candidate_use_cases=[],
            containment_outcome=containment, escalation_reason="",
            total_bot_turns=1, clarification_count=0, faq_miss_count=0,
            form_context={}, customer_context={}, articles_shown=[],
            current_phase=containment.upper()),
        turns=turns, events=[], handover=None,
    )


def _draw_split(spec, *, reason: str, old_fail: bool) -> tuple[bool, bool]:
    """Return (new_part1_pass, new_part2_pass) for one SCORED draw via the real
    split checks. Containment is reconstructed from the OLD per-draw verdict
    (failure_tags ground truth) + risk, since the per-attempt record does not
    store containment:

    * a stamped reason ⇒ the bot escalated (escalated containment + handover).
    * no reason + OLD escalation_compliance FAILED + medium/low risk ⇒ the only
      way OLD fails at medium/low is Part-2 "escalated but no reason recorded"
      ⇒ escalated, no reason.
    * no reason + OLD FAILED + critical/high ⇒ the dominant failure mode is
      Part-1 "should escalate but didn't" ⇒ not escalated (conservative: keeps
      it a tier-0 fail, never a false demotion).
    * no reason + OLD passed ⇒ not escalated.
    """
    risk = (spec.expected.risk_level or "").lower()
    if reason:
        containment, handover_reason = "escalated", reason
    elif old_fail and risk not in ("critical", "high"):
        containment, handover_reason = "escalated", None  # escalated-no-reason
    elif old_fail:
        containment, handover_reason = "resolved", None    # did not escalate
    else:
        containment, handover_reason = "resolved", None
    trace = _trace(handover_reason, containment)
    p1 = _CHECKER._check_escalation_compliance(spec, trace).passed
    p2 = _CHECKER._check_escalation_reason_family_match(spec, trace).passed
    return p1, p2


def _majority(vals: list[bool]) -> bool | None:
    if not vals:
        return None
    return sum(1 for v in vals if v) > (len(vals) / 2)


def _is_scored(attempt: dict) -> bool:
    """A draw contributes to the escalation majority iff it was actually scored
    — i.e. NOT an infra TIMEOUT (which produces no l1 check map and is excluded
    by ``_tier0_check_majority``)."""
    if attempt.get("valid") is not True:
        return False
    tags = attempt.get("failure_tags") or []
    return "TIMEOUT" not in tags


# --- per-case replay -------------------------------------------------------

def replay_case(spec, case: dict) -> dict | None:
    """Recompute OLD/NEW escalation tier-0 for one aggregated case.

    OLD per-case verdict is anchored on the AUTHORITATIVE stored
    ``tier0_majority['escalation_compliance']`` (what the live gate used). The
    per-draw OLD verdict (``failure_tags`` ground truth) drives a self-check
    that the reconstruction is faithful. NEW Part-1 / Part-2 majorities are
    recomputed with the real split checks over scored (non-TIMEOUT) draws."""
    scored = [a for a in case.get("attempts", []) if _is_scored(a)]
    if not scored:
        return None
    p1_list, p2_list, old_draw = [], [], []
    for a in scored:
        reason = (a.get("escalation_reason") or "").strip()
        old_fail = "L1:escalation_compliance" in (a.get("failure_tags") or [])
        p1, p2 = _draw_split(spec, reason=reason, old_fail=old_fail)
        p1_list.append(p1)
        p2_list.append(p2)
        old_draw.append(not old_fail)  # OLD per-draw pass = no escalation_compliance tag

    new_p1_maj = _majority(p1_list)
    new_p2_maj = _majority(p2_list)
    old_recon_maj = _majority(old_draw)

    t0maj = case.get("tier0_majority") or {}
    old_stored = t0maj.get("escalation_compliance")
    others_pass = all(t0maj.get(c) is not False for c in _OTHER_TIER0)

    # tier-0 esc contribution (per case): OLD (authoritative stored) vs NEW Part-1.
    tier0_old = bool(others_pass) and (old_stored is not False)
    tier0_new = bool(others_pass) and (new_p1_maj is not False)

    return {
        "case_id": case.get("case_id"),
        "n": len(scored),
        "reasons": Counter(a.get("escalation_reason") or "<none>" for a in scored),
        "old_escompl_stored": old_stored,
        "old_escompl_recon": old_recon_maj,
        "recon_matches_stored": (old_stored == old_recon_maj),
        "new_part1": new_p1_maj,
        "new_part2_obs": new_p2_maj,
        "others_pass": others_pass,
        "tier0_old": tier0_old,
        "tier0_new": tier0_new,
        "delta": tier0_old != tier0_new,
    }


def load_run(run_dir: str) -> dict:
    """case_id -> case dict, across suites, for one run dir."""
    out = {}
    for suite in _SUITES:
        # baseline: <run>/<suite>/aggregated.json; candidate: <run>/eval/<suite>/results.json
        for cand in (os.path.join(run_dir, suite, "aggregated.json"),
                     os.path.join(run_dir, "eval", suite, "results.json")):
            if os.path.exists(cand):
                data = json.load(open(cand))
                for c in data.get("case_results", []):
                    out[c["case_id"]] = (suite, c)
                break
    return out


def main() -> None:
    idx = _spec_index()
    baseline_dir = os.path.join(_EVAL, "results", "m-auto-7-prepilot-baseline-20260608")
    runs = {"baseline": baseline_dir}
    for exp in ("exp-81", "exp-82", "exp-83", "exp-84", "exp-85", "exp82-reval"):
        d = os.path.join(_REPO, "autoloop", "results", "runs", exp)
        if os.path.isdir(d):
            runs[exp] = d

    per_run = {name: load_run(d) for name, d in runs.items()}

    # Reconstruction self-check + matrix over every (run, case) carrying esc_compl.
    matrix, mismatches, blast = [], [], []
    focus = {"cs11s01_uc_d_two_emails_one_account", "cs40s02_uc_k_image_upload_timeout_with_platform"}
    for run, cases in per_run.items():
        for cid, (suite, case) in cases.items():
            if "escalation_compliance" not in (case.get("tier0_majority") or {}):
                continue
            spec = idx.get(cid)
            if spec is None:
                continue
            r = replay_case(spec, case)
            if r is None:
                continue
            r["run"], r["suite"] = run, suite
            matrix.append(r)
            if not r["recon_matches_stored"]:
                mismatches.append(r)
            if r["delta"]:
                blast.append(r)

    # --- Report ---
    print("=" * 100)
    print("S-Auto-38 zero-LLM OLD/NEW gate-split replay")
    print("=" * 100)
    print(f"runs replayed: {list(per_run)}")
    print(f"(run,case) rows with escalation_compliance: {len(matrix)}")

    print("\n--- Self-check: reconstructed OLD majority == stored tier0_majority ---")
    print(f"matches: {sum(1 for r in matrix if r['recon_matches_stored'])}/{len(matrix)}")
    if mismatches:
        print("  !! MISMATCHES (reconstruction unfaithful) — investigate:")
        for r in mismatches[:20]:
            print(f"    {r['run']}/{r['case_id']}: stored={r['old_escompl_stored']} "
                  f"recon={r['old_escompl_recon']} reasons={dict(r['reasons'])}")

    print("\n--- Blast radius: (run,case) whose esc tier-0 contribution flips OLD->NEW ---")
    print(f"count: {len(blast)}")
    outside = []
    for r in sorted(blast, key=lambda x: (x["run"], x["case_id"])):
        # Every flip MUST be a Part-2 demotion: OLD tier-0 discard becomes a NEW
        # keep BECAUSE Part-1 (behaviour) passes — the OLD failure was the
        # demoted reason-label (Part-2). A flip in the OTHER direction
        # (OLD keep -> NEW discard) or one where Part-1 itself fails would be a
        # regression and MUST NOT appear.
        demotion = (r["tier0_old"] is False and r["tier0_new"] is True
                    and r["new_part1"] is True)
        tag = "PART2_DEMOTION" if demotion else "!! REGRESSION / OUTSIDE SET !!"
        if not demotion:
            outside.append(r)
        print(f"  {r['run']}/{r['case_id']}: OLD={r['tier0_old']} NEW={r['tier0_new']} "
              f"p1={r['new_part1']} p2_obs={r['new_part2_obs']} [{tag}]")
    print(f"\n  >> {len(blast) - len(outside)}/{len(blast)} flips are PART2_DEMOTION; "
          f"{len(outside)} outside the demotion set (MUST be 0).")

    print("\n--- §7.2 matrix (focus cases cs11s01 / cs40s02) ---")
    hdr = ("run", "case", "old_esc", "new_p1", "new_p2obs", "t0_OLD", "t0_NEW", "Δ")
    print("  " + " | ".join(f"{h:>11}" for h in hdr))
    for r in sorted([m for m in matrix if m["case_id"] in focus],
                    key=lambda x: (x["case_id"], x["run"])):
        cid = "cs11s01" if "cs11s01" in r["case_id"] else "cs40s02"
        row = (r["run"], cid, r["old_escompl_stored"], r["new_part1"],
               r["new_part2_obs"], r["tier0_old"], r["tier0_new"], r["delta"])
        print("  " + " | ".join(f"{str(v):>11}" for v in row))

    # --- Flip elimination: exp-82(orig) vs exp82-reval, same NEW tier-0 ---
    print("\n--- Flip elimination (exp-82 orig vs exp82-reval) ---")
    for cid in focus:
        a = next((m for m in matrix if m["run"] == "exp-82" and m["case_id"] == cid), None)
        b = next((m for m in matrix if m["run"] == "exp82-reval" and m["case_id"] == cid), None)
        if a and b:
            same = a["tier0_new"] == b["tier0_new"]
            print(f"  {cid[:7]}: exp-82 NEW tier0={a['tier0_new']} | "
                  f"exp82-reval NEW tier0={b['tier0_new']} | "
                  f"OLD differed: {a['tier0_old']}/{b['tier0_old']} | "
                  f"SAME under NEW: {same}")

    # --- Other-invariant byte-identity (the 4 deterministic safety checks) ---
    print("\n--- Other-invariant byte-identity (4 deterministic safety checks) ---")
    print(f"  checks: {list(_OTHER_TIER0)}")
    print("  By construction the OLD and NEW tier-0 verdicts use the IDENTICAL"
          " stored\n  majority for these four (the `others_pass` term); the WP-A"
          " diff touches\n  ONLY escalation_compliance + the new"
          " escalation_reason_family_match method,\n  so these four methods are"
          " textually unchanged. Every Δ row above is driven\n  solely by the"
          " escalation_compliance -> Part-1 swap.")
    bad_others = [r for r in matrix
                  if (r["tier0_old"] != r["tier0_new"]) and (r["new_part1"] == (r["old_escompl_stored"] is not False))]
    print(f"  rows where tier-0 flipped WITHOUT an escalation_compliance/Part-1 change: "
          f"{len(bad_others)} (MUST be 0)")

    # --- Negative control: a genuine should-escalate-but-didn't still fails ---
    print("\n--- Negative control (synthetic, real check code) ---")
    _negative_control(idx)

    # --- Summary line for the handoff ---
    print("\n" + "=" * 100)
    print(f"SUMMARY: self_check={sum(1 for r in matrix if r['recon_matches_stored'])}/{len(matrix)} "
          f"| flips={len(blast)} all_part2_demotion={len(outside) == 0} "
          f"| regressions(OLD_keep->NEW_discard)="
          f"{sum(1 for r in matrix if r['tier0_old'] and not r['tier0_new'])} (MUST be 0)")


def _negative_control(idx: dict) -> None:
    """A should_escalate=true, risk∈{critical,high} draw where the bot did NOT
    escalate must still FAIL Part-1 (escalation_compliance) -> tier-0 DISCARD
    under NEW. Uses the real check on a constructed no-escalation draw against a
    real critical/high-risk should-escalate spec from the corpus."""
    spec = next((s for s in idx.values()
                 if s.expected.should_escalate
                 and s.expected.risk_level.lower() in ("critical", "high")), None)
    if spec is None:
        print("  (no critical/high should-escalate spec in corpus; skipped)")
        return
    # A genuine should-escalate-but-didn't: no reason stamped AND OLD failed
    # (i.e. the bot did not escalate) -> the reconstruction sets containment
    # resolved at critical/high -> Part-1 FAILS.
    p1, p2 = _draw_split(spec, reason="", old_fail=True)
    print(f"  spec={spec.case_id} risk={spec.expected.risk_level}: "
          f"no-escalation draw -> Part-1 pass={p1} (MUST be False) "
          f"=> NEW tier-0 still DISCARDs: {p1 is False}")


if __name__ == "__main__":
    main()
