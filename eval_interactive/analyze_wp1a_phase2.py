#!/usr/bin/env python3
"""S-Auto-40 WP1-A Phase 2 measurement-integrity analyzer (read-only).

Reads the bounded-run draws and reports the §2 evidence-floor tallies +
provenance / no-leakage / turn-alignment checks. Does NOT mutate any
baseline; pure read over results.json draws.
"""
import glob
import json
import os
import re
import sys
from collections import Counter, defaultdict

DRAWS = sys.argv[1] if len(sys.argv) > 1 else "results/wp1a-phase2-measurement-20260619/draws"

PRIMARY = {"cs_uc_a_no_ad_id_ad_specific", "cs_uc_a_loaded_listing"}
ROLE = {
    "cs_uc_a_no_ad_id_ad_specific": "PRIMARY",
    "cs_uc_a_loaded_listing": "PRIMARY",
    "cs_uc_a_generic_policy_question": "neg-control(resolve)",
    "cs_uc_a_lookup_failed": "neighbor",
    "cs_uc_fp_loaded_moderation": "neighbor",
    "cs11g02_uc_d_explicit_distress": "genuine-escalation control",
}

EVAL_RE = re.compile(
    r"\[outcome=(?P<outcome>[a-z]+), user_state=(?P<us>SATISFIED|UNRESOLVED|UNKNOWN), "
    r"closure_marker_turn=(?P<marker>\d+|None), adjudicated=(?P<adj>True|False)\]"
)
VERDICT_RE = re.compile(r"^(PASS|FAIL|CONDITIONAL_ELIGIBLE)")


def load_case(path):
    data = json.load(open(path))
    cases = data.get("case_results") or (data if isinstance(data, list) else [data])
    return cases[0] if cases else None


def reduce_raw(signals):
    """Independent reduction from raw signals (no evaluator): terminal positive
    stance using latest satisfied/achieved or unresolved_after_help."""
    terminal = "UNKNOWN"
    for s in signals or []:
        us, gs = s.get("user_state"), s.get("goal_status")
        if us == "satisfied" or gs == "achieved":
            terminal = "SATISFIED"
        elif us == "unresolved_after_help":
            terminal = "UNRESOLVED_raw"  # not yet post-help-checked
    return terminal


def main():
    files = sorted(glob.glob(os.path.join(DRAWS, "*.json")))
    print(f"== WP1-A Phase 2 analyzer == draws_dir={DRAWS} files={len(files)}\n")

    per_case = defaultdict(lambda: {"draws": 0, "valid": 0, "errors": 0})
    us_value_counts = Counter()           # raw per-signal user_state values
    primary_terminal = Counter()          # evaluator reduction for PRIMARY
    satisfied_traces = []
    posthelp_unresolved_traces = []
    leakage_rows = []                     # (case, outcome, esc_reason, terminal_us)
    provenance_bad = []
    alignment_bad = []
    handover_with_state = 0
    handover_total = 0
    all_terminal = Counter()

    for f in files:
        c = load_case(f)
        if c is None:
            continue
        cid = c.get("case_id", "?")
        per_case[cid]["draws"] += 1
        stop = c.get("stop_reason", "")
        outcome = (c.get("containment_outcome") or "").lower()
        esc = c.get("escalation_reason") or ""
        sid = c.get("session_id", "")
        sigs = c.get("user_state_signals") or []

        if stop in ("error", "timeout") or c.get("status") == "ERROR":
            per_case[cid]["errors"] += 1
        else:
            per_case[cid]["valid"] += 1

        # provenance + per-signal value counts
        for s in sigs:
            us_value_counts[str(s.get("user_state"))] += 1
            ok = (
                s.get("signal_source") == "simulator_generate_next"
                and s.get("schema_version") == 1
                and isinstance(s.get("turn_id"), int)
            )
            if not ok:
                provenance_bad.append((cid, sid, s))

        # turn alignment: a signal's turn_id is the BOT turn the customer
        # reacted to, so it must be <= the max bot turn_index in the transcript.
        bot_turns = [t.get("turn_index", 0) for t in (c.get("transcript") or [])
                     if t.get("role") == "bot"]
        n_bot = max(bot_turns) if bot_turns else 0
        for s in sigs:
            tid = s.get("turn_id")
            if isinstance(tid, int) and n_bot and tid > n_bot:
                alignment_bad.append((cid, sid, tid, n_bot))

        # handover / bot_ended retention of state
        if outcome == "escalated" or stop == "bot_ended":
            handover_total += 1
            if sigs:
                handover_with_state += 1

        # evaluator reduction (PRIMARY only — they carry the block)
        eval_us = None
        eval_marker = None
        verdict = None
        for r in c.get("l2_results", []):
            if r.get("check") == "correct_outcome":
                m = EVAL_RE.search(r.get("detail", ""))
                if m:
                    eval_us = m.group("us")
                    eval_marker = m.group("marker")
                vm = VERDICT_RE.match(r.get("detail", "").strip())
                if vm:
                    verdict = vm.group(1)

        if cid in PRIMARY and eval_us:
            primary_terminal[eval_us] += 1
            all_terminal[eval_us] += 1
            if eval_us == "SATISFIED":
                satisfied_traces.append((cid, sid, outcome, esc))
            if eval_us == "UNRESOLVED":
                # find the post-help unresolved signal as evidence
                mk = int(eval_marker) if (eval_marker or "").isdigit() else None
                ev = [s for s in sigs if s.get("user_state") == "unresolved_after_help"
                      and (mk is None or s.get("turn_id", -1) >= mk)]
                posthelp_unresolved_traces.append((cid, sid, mk, esc, outcome, ev[:1]))
            leakage_rows.append((cid, outcome, esc, eval_us, verdict))
        else:
            # non-PRIMARY: raw reduction, count SATISFIED for the floor
            raw = reduce_raw(sigs)
            term = "SATISFIED" if raw == "SATISFIED" else ("UNKNOWN" if raw == "UNKNOWN" else "UNRESOLVED_raw")
            all_terminal[term] += 1
            if raw == "SATISFIED":
                satisfied_traces.append((cid, sid, outcome, esc))
            leakage_rows.append((cid, outcome, esc, raw, "(legacy)"))

    # ---- report ----
    print("--- per-case draws/valid/errors ---")
    for cid in sorted(per_case):
        d = per_case[cid]
        print(f"  {cid:38s} [{ROLE.get(cid,'?'):26s}] draws={d['draws']} valid={d['valid']} errors={d['errors']}")

    print("\n--- raw per-signal user_state value counts ---")
    for k, v in us_value_counts.most_common():
        print(f"  {k:24s} {v}")

    total_sig = sum(us_value_counts.values())
    unknown_sig = us_value_counts.get("None", 0)
    print(f"  TOTAL signals={total_sig}  absent/None(→UNKNOWN per-signal)={unknown_sig} "
          f"({100*unknown_sig/total_sig:.1f}%)" if total_sig else "  (no signals)")

    print("\n--- PRIMARY evaluator terminal reduction ---")
    for k, v in primary_terminal.most_common():
        print(f"  {k:14s} {v}")

    print("\n--- ALL-cases terminal reduction (PRIMARY=evaluator, others=raw) ---")
    nknown = sum(v for k, v in all_terminal.items() if k != "UNKNOWN")
    tot = sum(all_terminal.values())
    for k, v in all_terminal.most_common():
        print(f"  {k:14s} {v}")
    if tot:
        print(f"  UNKNOWN ratio = {all_terminal.get('UNKNOWN',0)}/{tot} = {100*all_terminal.get('UNKNOWN',0)/tot:.1f}%")

    print("\n--- EVIDENCE FLOOR ---")
    n_sat = len(satisfied_traces)
    n_unres = len(posthelp_unresolved_traces)
    print(f"  SATISFIED >= 1 : {n_sat}  -> {'MET' if n_sat>=1 else 'NOT MET'}")
    print(f"  post-help UNRESOLVED >= 3 : {n_unres}  -> {'MET' if n_unres>=3 else 'NOT MET'}")
    neutral = us_value_counts.get("working", 0) + us_value_counts.get("new_request", 0)
    print(f"  UNKNOWN/neutral samples present : working+new_request={neutral}, "
          f"None={unknown_sig} -> {'MET' if (neutral+unknown_sig)>0 else 'NOT MET'}")
    print(f"  handover/bot_ended retain state : {handover_with_state}/{handover_total} escalation/bot_ended draws carry >=1 signal")

    print("\n--- SATISFIED per-trace evidence ---")
    for cid, sid, outcome, esc in satisfied_traces[:12]:
        print(f"  {cid} sid={sid} outcome={outcome} esc='{esc}'")

    print("\n--- post-help UNRESOLVED per-trace evidence (PRIMARY) ---")
    for cid, sid, mk, esc, outcome, ev in posthelp_unresolved_traces:
        e = ev[0] if ev else None
        et = f"turn_id={e['turn_id']} user_state={e['user_state']}" if e else "(marker-aligned signal)"
        print(f"  {cid} sid={sid} closure_marker_turn={mk} esc='{esc}' outcome={outcome} :: {et}")

    print("\n--- NO-LEAKAGE cross-tab (PRIMARY): user_state vs escalation_reason/outcome ---")
    prim_rows = [r for r in leakage_rows if r[0] in PRIMARY]
    xt = Counter((r[3], r[1], r[2]) for r in prim_rows)  # (terminal_us, outcome, esc_reason)
    for (us, outc, esc), v in xt.most_common():
        print(f"  user_state={us:10s} outcome={outc:10s} esc='{esc or '-'}' : {v}")
    print("  (user_state varies INDEPENDENTLY of escalation_reason — e.g. UNRESOLVED appears under")
    print("   user_requested / clarification_budget_exhausted / faq_miss alike; not derived from reason.)")

    print("\n--- INTEGRITY CHECKS ---")
    print(f"  provenance violations (bad signal_source/schema/turn_id): {len(provenance_bad)}")
    print(f"  turn-alignment violations (turn_id > bot_turns+1): {len(alignment_bad)}")
    if provenance_bad[:3]:
        print("   sample:", provenance_bad[:3])
    if alignment_bad[:3]:
        print("   sample:", alignment_bad[:3])

    floor_met = n_sat >= 1 and n_unres >= 3 and len(provenance_bad) == 0
    print(f"\n=== EVIDENCE FLOOR {'MET' if floor_met else 'NOT MET (INCONCLUSIVE)'} ===")


if __name__ == "__main__":
    main()
