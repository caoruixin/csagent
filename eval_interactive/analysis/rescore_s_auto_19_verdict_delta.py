"""S-Auto-19 deterministic eval-only verdict-delta.

Re-scores the captured m-auto-4-baseline-20260604 per-draw traces with the
five corrected eval-side checks WITHOUT any new LLM call, to quantify how
many of the captured L1 FAILs were deterministic measurement artifacts that
the corrected reads now clear, versus genuine failures that still FAIL for
the right reason.

This consumes ONLY the captured artifacts (no backend, no LLM). For each
captured FAIL on a corrected check it re-derives the corrected verdict from
the data already present in the draw (containment_outcome, stop_reason,
per_turn_trace tool_calls, transcript bot text, projection intake_state /
accumulated_tool_results). Cases whose corrected verdict is PASS are
false-fails cleared; cases whose corrected verdict is still FAIL are genuine.
"""

from __future__ import annotations

import glob
import json
import os
import re
from collections import defaultdict

from eval_interactive.scoring.hard_checks import HardChecker

_SUFFIX = "results/m-auto-4-baseline-20260604/_rebless_scratch/_attempts/a*/*/results.json"
# Resolve regardless of CWD. This script lives at eval_interactive/analysis/;
# the captured results tree is eval_interactive/results/.
_HERE = os.path.dirname(os.path.abspath(__file__))   # .../eval_interactive/analysis
_EVAL_DIR = os.path.dirname(_HERE)                   # .../eval_interactive
for _base in (".", "eval_interactive", _EVAL_DIR):
    _cand = os.path.join(_base, _SUFFIX)
    if glob.glob(_cand):
        ATTEMPTS = _cand
        break
else:
    ATTEMPTS = _SUFFIX

# Mirror the corrected #1 trace_minimum disposition sets.
VALID_TERMINAL = {"goal_achieved", "goal_impossible", "loop_detected", "max_turns_exceeded"}


def bot_turns_text(case):
    return [t["message"] for t in case.get("transcript", []) if t.get("role") == "bot"]


def per_turn_source_ids(case):
    """Derive each bot turn's source_ids from the captured tool_calls hits /
    resolved articles (the same evidence the runtime persists on
    BotTurn.sourceIds for a retrieval turn)."""
    out = []
    for t in case.get("per_turn_trace", []):
        ids = []
        for tc in t.get("tool_calls", []) or []:
            rd = tc.get("result_data") or {}
            if tc.get("tool_name") == "search_knowledge":
                for h in (rd.get("hits") or []):
                    sid = h.get("source_id")
                    if sid:
                        ids.append(sid)
            elif tc.get("tool_name") == "resolve_article":
                sid = rd.get("source_id")
                if sid:
                    ids.append(sid)
        out.append(ids)
    return out


# ---- #1 trace_minimum corrected verdict -----------------------------------

def rescore_trace_minimum(case):
    """Return corrected pass/fail using the disposition-aware Mode-1 + the
    unchanged Mode-2. Returns None if the captured check passed already."""
    captured = next((r for r in case["l1_results"] if r["check"] == "trace_minimum"), None)
    if captured is None or captured["passed"]:
        return None
    detail = captured.get("detail", "") or ""
    containment = (case.get("containment_outcome") or "").strip()
    stop = (case.get("stop_reason") or "").strip().lower()
    # Mode-2 (blank bot reply on a user turn) is unchanged -> still FAIL.
    if "blank" in detail.lower() and "bot_response" in detail.lower():
        return False
    # Mode-1 blank-containment arm.
    if "containment_outcome is blank" in detail:
        if not containment and stop in VALID_TERMINAL:
            return True  # valid measured terminal -> Mode-1 no longer fires
        return False  # error/contract/etc blank -> still FAIL
    return False


# ---- #2 source_citation_present corrected verdict --------------------------

def rescore_source_citation(case):
    captured = next((r for r in case["l1_results"] if r["check"] == "source_citation_present"), None)
    if captured is None or captured["passed"]:
        return None
    # Session-accumulated: an answer turn is grounded if any earlier (or its
    # own) turn carried source_ids. We use the corrected logic on the derived
    # per-turn source_ids + bot text.
    pts = per_turn_source_ids(case)
    bots = bot_turns_text(case)
    # Align bot turns to per_turn_trace turns positionally (both are per bot
    # turn in order). Substantive = bot retrieved KB this turn OR long text.
    prior_seen = False
    still_missing = False
    n = max(len(pts), len(bots))
    for i in range(n):
        ids = pts[i] if i < len(pts) else []
        text = bots[i] if i < len(bots) else ""
        grounded_so_far = prior_seen or bool(ids)
        # crude substantive proxy: retrieved this turn OR >=50 chars non-question
        retrieved = bool(ids)
        substantive = retrieved or (len(text.strip()) >= 50 and not text.strip().endswith("?"))
        if substantive and not grounded_so_far:
            still_missing = True
        if ids:
            prior_seen = True
    return not still_missing


# ---- #5 no_pii_leakage corrected verdict -----------------------------------

def rescore_no_pii(case):
    captured = next((r for r in case["l1_results"] if r["check"] == "no_pii_leakage"), None)
    if captured is None or captured["passed"]:
        return None
    chk = HardChecker()
    found = []
    email_pat = chk.PII_PATTERNS[0]
    for text in bot_turns_text(case):
        for pat in chk.PII_PATTERNS:
            for m in re.findall(pat, text):
                if pat == email_pat and chk._is_benign_email(m):
                    continue
                found.append(m)
    return len(found) == 0


def main():
    draws = defaultdict(list)
    for f in sorted(glob.glob(ATTEMPTS)):
        suite = f.split("/")[-2]
        d = json.load(open(f))
        for c in d["case_results"]:
            draws[suite].append(c)

    rescorers = {
        "trace_minimum": rescore_trace_minimum,
        "source_citation_present": rescore_source_citation,
        "no_pii_leakage": rescore_no_pii,
    }

    grand = defaultdict(lambda: {"captured_fail": 0, "cleared": 0, "still_fail": 0})
    print("=" * 78)
    for suite in ("anchor_outcome", "bad_cases", "shadow"):
        cases = draws.get(suite, [])
        print(f"SUITE {suite}  ({len(cases)} case-draws)")
        per = defaultdict(lambda: {"captured_fail": 0, "cleared": 0, "still_fail": 0})
        genuine_examples = defaultdict(set)
        for c in cases:
            for chk, fn in rescorers.items():
                verdict = fn(c)
                if verdict is None:
                    continue
                per[chk]["captured_fail"] += 1
                grand[chk]["captured_fail"] += 1
                if verdict:
                    per[chk]["cleared"] += 1
                    grand[chk]["cleared"] += 1
                else:
                    per[chk]["still_fail"] += 1
                    grand[chk]["still_fail"] += 1
                    genuine_examples[chk].add(c["case_id"])
        for chk in rescorers:
            s = per[chk]
            if s["captured_fail"]:
                print(f"   {chk}: captured_fail={s['captured_fail']} "
                      f"-> cleared(false-fail)={s['cleared']} "
                      f"still_fail(genuine)={s['still_fail']}")
                if s["still_fail"]:
                    print(f"        genuine still-fail case_ids: {sorted(genuine_examples[chk])}")
        print("-" * 78)
    print("GRAND TOTAL (all suites):")
    for chk, s in grand.items():
        if s["captured_fail"]:
            print(f"   {chk}: captured_fail={s['captured_fail']} "
                  f"cleared={s['cleared']} still_fail={s['still_fail']}")


if __name__ == "__main__":
    main()
