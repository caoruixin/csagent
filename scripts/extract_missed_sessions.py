#!/usr/bin/env python3
"""
Extract missed sessions: chat sessions where the visitor sent messages
but no agent ever responded.

These sessions are excluded from filter_chat_dataset.py via the
"missing_agent_or_visitor" hard filter, but are critical for V1 launch:
- They represent unmet demand (currently no service)
- They are the highest-volume opportunity for Bot first-touch
- Bot resolution = pure incremental value (no agent comparison risk)

Usage:
    python3 scripts/extract_missed_sessions.py [<input_file>]
    Default input: data/bq-results-20260414-csat-not-null.csv

Outputs:
    data/filtered/missed_sessions.csv          — all missed sessions (session-level)
    data/filtered/missed_sessions_messages.csv — visitor turns, PII-redacted
    data/eval_datasets/missed_sessions_dataset.csv — stratified sample (~100-150)
    data/eval_datasets/missed_sessions_turns.csv  — turns for the sample
    data/eval_datasets/missed_sessions_REPORT.md  — extraction report
"""

import csv
import os
import re
import sys
from collections import defaultdict
from datetime import datetime
from pathlib import Path

# Reuse parsing + redaction from main filter
sys.path.insert(0, str(Path(__file__).resolve().parent))
from filter_chat_dataset import (
    parse_transcript, redact_pii, read_input, write_csv,
    classify_use_case, UC_BODY_PATTERNS,
)

random_seed = 42

BASE = Path(__file__).resolve().parent.parent
RAW_DEFAULT = BASE / "data" / "bq-results-20260414-csat-not-null.csv"
FILTERED_OUT = BASE / "data" / "filtered"
EVAL_OUT = BASE / "data" / "eval_datasets"
FILTERED_OUT.mkdir(parents=True, exist_ok=True)
EVAL_OUT.mkdir(parents=True, exist_ok=True)

# ─────────────────────────────────────────────
# Urgency signals (from real Gumtree transcripts)
# ─────────────────────────────────────────────
URGENCY_PATTERNS = [
    re.compile(r"(?i)(urgent|asap|immediately|right now|emergency)"),
    re.compile(r"(?i)(losing money|losing customers|missing out|deadline)"),
    re.compile(r"(?i)(scam|fraud|stolen|threat|safety|harm|danger)"),
    re.compile(r"(?i)(law|legal|sue|court|trading standards|solicitor)"),
    re.compile(r"\?{3,}"),
]

# Business hours: UK 09:00-18:00 Mon-Fri (rough heuristic for off-hours flag)
def is_business_hours(start_time_str):
    if not start_time_str:
        return None
    try:
        s = str(start_time_str).replace("+0000", "+00:00").replace("Z", "+00:00")
        dt = datetime.fromisoformat(s)
        return dt.weekday() < 5 and 9 <= dt.hour < 18
    except Exception:
        return None


def detect_urgency(text):
    if not text:
        return False, []
    matches = []
    for p in URGENCY_PATTERNS:
        m = p.findall(text)
        if m:
            matches.extend(m if isinstance(m[0], str) else [x[0] for x in m])
    return len(matches) > 0, matches[:3]


def classify_first_message_uc(first_message, reason, subject, description):
    """Classify intent from limited signal: first message + Salesforce metadata."""
    primary_uc, all_ucs, confidence = classify_use_case(
        reason, subject, description, first_message or ""
    )
    return primary_uc, "|".join(sorted(all_ucs)) if all_ucs else "", confidence


# ─────────────────────────────────────────────
# Process a single record under "missed" criteria
# ─────────────────────────────────────────────
def process_missed(record):
    body = record.get("Body", "") or ""
    origin = (record.get("Origin", "") or "").strip()

    if origin and origin.lower() != "chat":
        return None, None, "origin_not_chat"
    if not body or len(body.strip()) < 30:
        return None, None, "empty_body"

    turns, agent_count, visitor_count, agent_names, _ = parse_transcript(body)

    # NEW HARD FILTER: visitor messages exist, NO agent response
    if visitor_count < 1:
        return None, None, "no_visitor"
    if agent_count > 0:
        return None, None, "has_agent"  # not "missed"

    # Build session record
    visitor_turns = [t for t in turns if t["role"] == "visitor"]
    first_message = visitor_turns[0]["text"] if visitor_turns else ""
    all_visitor_text = " | ".join(t["text"] for t in visitor_turns)

    reason = (record.get("Reason", "") or "").strip()
    subject = (record.get("Subject", "") or "").strip()
    description = (record.get("Description", "") or "").strip()

    primary_uc, all_ucs, uc_confidence = classify_first_message_uc(
        first_message + " " + all_visitor_text, reason, subject, description
    )

    has_urgency, urgency_evidence = detect_urgency(
        first_message + " " + all_visitor_text
    )
    biz_hours = is_business_hours(record.get("StartTime"))

    # Time to abandon = wait time + duration of visitor messages
    duration = None
    try:
        s = str(record.get("StartTime", "")).replace("+0000", "+00:00").replace("Z", "+00:00")
        e = str(record.get("EndTime", "")).replace("+0000", "+00:00").replace("Z", "+00:00")
        duration = int((datetime.fromisoformat(e) - datetime.fromisoformat(s)).total_seconds())
    except Exception:
        pass

    wait_time_raw = record.get("WaitTime", "")
    try:
        wait_time = int(wait_time_raw) if wait_time_raw not in ("", None) else None
    except (ValueError, TypeError):
        wait_time = None

    csat_raw = record.get("StellaConnect__Star_Rating__c")
    csat = None
    if csat_raw:
        try:
            csat = int(float(csat_raw))
        except (ValueError, TypeError):
            csat = None

    visitor_persistence = visitor_count  # how many times user pinged
    intent_clarity = (
        "high" if uc_confidence == "high" and len(first_message.split()) >= 5
        else "medium" if uc_confidence in ("high", "medium")
        else "low"
    )

    review_priority = (
        "high" if intent_clarity == "high" and (has_urgency or visitor_persistence >= 3)
        else "medium" if intent_clarity in ("high", "medium")
        else "low"
    )

    out = {
        "Id": record.get("Id", ""),
        "CaseId": record.get("CaseId", ""),
        "StartTime": record.get("StartTime", ""),
        "EndTime": record.get("EndTime", ""),
        "WaitTime": wait_time,
        "Platform": record.get("Platform", ""),
        "Subject": subject,
        "Reason": reason,
        "Description": description[:200] if description else "",
        "CSAT": csat,
        "visitor_message_count": visitor_count,
        "agent_message_count": 0,
        "duration_seconds": duration,
        "first_message": redact_pii(first_message)[:300],
        "all_visitor_text": redact_pii(all_visitor_text)[:1000],
        "inferred_uc": primary_uc,
        "all_ucs": all_ucs,
        "inferred_uc_confidence": uc_confidence,
        "intent_clarity": intent_clarity,
        "is_business_hours": biz_hours,
        "has_urgency_signal": has_urgency,
        "urgency_evidence": "|".join(urgency_evidence),
        "visitor_persistence": visitor_persistence,
        "time_to_abandon_seconds": duration,
        "review_priority": review_priority,
    }

    # Build turn-level rows (visitor only, since no agent)
    turn_rows = []
    for seq, t in enumerate(visitor_turns, start=1):
        turn_rows.append({
            "conversation_id": out["Id"],
            "case_id": out["CaseId"],
            "sequence": seq,
            "relative_time_sec": t["timestamp_seconds"],
            "role": "visitor",
            "speaker": t["name"],
            "message_redacted": redact_pii(t["text"]),
        })

    return out, turn_rows, "pass"


# ─────────────────────────────────────────────
# Stratified sample for eval dataset
# ─────────────────────────────────────────────
def stratified_sample(rows, key, target, min_per_group=3):
    """Stratified sampling, prioritizing high review_priority within each group."""
    priority_rank = {"high": 0, "medium": 1, "low": 2}

    groups = defaultdict(list)
    for r in rows:
        groups[r.get(key, "UNKNOWN")].append(r)
    for g in groups:
        groups[g].sort(key=lambda r: (priority_rank.get(r.get("review_priority"), 9),
                                       -(r.get("visitor_persistence") or 0)))

    selected = []
    remaining = target
    for g, members in groups.items():
        take = min(min_per_group, len(members))
        selected.extend(members[:take])
        groups[g] = members[take:]
        remaining -= take

    if remaining > 0:
        pool = []
        for g, members in groups.items():
            pool.extend(members)
        pool.sort(key=lambda r: (priority_rank.get(r.get("review_priority"), 9),
                                  -(r.get("visitor_persistence") or 0)))
        selected.extend(pool[:remaining])

    return selected[:target]


# ─────────────────────────────────────────────
# Output schemas
# ─────────────────────────────────────────────
SESSION_FIELDS = [
    "Id", "CaseId", "StartTime", "EndTime", "WaitTime", "Platform",
    "Subject", "Reason", "Description", "CSAT",
    "visitor_message_count", "agent_message_count", "duration_seconds",
    "first_message", "all_visitor_text",
    "inferred_uc", "all_ucs", "inferred_uc_confidence", "intent_clarity",
    "is_business_hours", "has_urgency_signal", "urgency_evidence",
    "visitor_persistence", "time_to_abandon_seconds", "review_priority",
]
EVAL_EXTRA = ["eval_tier"]
TURN_FIELDS = [
    "conversation_id", "case_id", "sequence", "relative_time_sec",
    "role", "speaker", "message_redacted",
]


# ─────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────
def main():
    input_file = sys.argv[1] if len(sys.argv) > 1 else str(RAW_DEFAULT)
    if not os.path.exists(input_file):
        print(f"ERROR: Input file not found: {input_file}")
        sys.exit(1)

    print(f"Reading: {input_file}")
    records = read_input(input_file)
    print(f"Total records: {len(records)}")

    passed = []
    turns_by_id = {}
    stats = defaultdict(int)
    stats["total"] = len(records)

    for record in records:
        result, turn_rows, reason = process_missed(record)
        stats[reason] += 1
        if result:
            passed.append(result)
            turns_by_id[result["Id"]] = turn_rows

    print(f"\n── EXTRACTION RESULTS ──")
    print(f"  Total:                {stats['total']:>6}")
    print(f"  Excluded (non-Chat):  {stats.get('origin_not_chat', 0):>6}")
    print(f"  Excluded (empty body):{stats.get('empty_body', 0):>6}")
    print(f"  Excluded (no visitor):{stats.get('no_visitor', 0):>6}")
    print(f"  Excluded (has agent): {stats.get('has_agent', 0):>6}  ← these became filtered_all.csv")
    print(f"  ── Missed sessions:    {len(passed):>6}")

    # ── Write filtered outputs ──
    print(f"\n── WRITING FILTERED OUTPUTS ──")
    write_csv(passed, str(FILTERED_OUT / "missed_sessions.csv"), SESSION_FIELDS)
    print(f"  → data/filtered/missed_sessions.csv ({len(passed)} sessions)")

    all_turns = []
    for r in passed:
        all_turns.extend(turns_by_id.get(r["Id"], []))
    write_csv(all_turns, str(FILTERED_OUT / "missed_sessions_messages.csv"), TURN_FIELDS)
    print(f"  → data/filtered/missed_sessions_messages.csv ({len(all_turns)} visitor turns)")

    # ── UC distribution ──
    uc_dist = defaultdict(int)
    biz_dist = defaultdict(int)
    priority_dist = defaultdict(int)
    for r in passed:
        uc_dist[r["inferred_uc"]] += 1
        biz_dist["business_hours" if r["is_business_hours"] else "off_hours" if r["is_business_hours"] is False else "unknown"] += 1
        priority_dist[r["review_priority"]] += 1

    print(f"\n── UC DISTRIBUTION (inferred from first message) ──")
    for uc in sorted(uc_dist.keys()):
        pct = 100 * uc_dist[uc] / max(len(passed), 1)
        print(f"  {uc:<12} {uc_dist[uc]:>5}  ({pct:>5.1f}%)")

    print(f"\n── BUSINESS HOURS DISTRIBUTION ──")
    for k, v in biz_dist.items():
        pct = 100 * v / max(len(passed), 1)
        print(f"  {k:<18} {v:>5}  ({pct:>5.1f}%)")

    # ── Build eval dataset ──
    print(f"\n── BUILDING EVAL DATASET ──")
    target = 120  # mid-range between Bad-case (95) and Escalation (150)
    sample = stratified_sample(passed, "inferred_uc", target, min_per_group=5)

    for r in sample:
        r["eval_tier"] = "missed_session"

    write_csv(sample, str(EVAL_OUT / "missed_sessions_dataset.csv"),
              SESSION_FIELDS + EVAL_EXTRA)
    print(f"  → data/eval_datasets/missed_sessions_dataset.csv ({len(sample)} sessions)")

    sample_ids = {r["Id"] for r in sample}
    sample_turns = [t for t in all_turns if t["conversation_id"] in sample_ids]
    write_csv(sample_turns, str(EVAL_OUT / "missed_sessions_turns.csv"), TURN_FIELDS)
    print(f"  → data/eval_datasets/missed_sessions_turns.csv ({len(sample_turns)} turns)")

    # ── UC distribution in eval sample ──
    sample_uc_dist = defaultdict(int)
    sample_priority_dist = defaultdict(int)
    for r in sample:
        sample_uc_dist[r["inferred_uc"]] += 1
        sample_priority_dist[r["review_priority"]] += 1

    # ── Write report ──
    report_lines = [
        "# Missed Sessions Extraction Report",
        "",
        f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        f"Source: `{input_file}`",
        f"Script: `scripts/extract_missed_sessions.py`",
        "",
        "## Definition",
        "",
        "**Missed session** = chat session where visitor sent ≥ 1 message but agent never responded.",
        "Excluded from main `filter_chat_dataset.py` via the `missing_agent_or_visitor` hard filter.",
        "",
        "## Why these matter for V1",
        "",
        "- **Unmet demand**: currently no service, so any Bot resolution = pure win",
        "- **Highest-volume opportunity** for Bot first-touch",
        "- **Off-hours coverage**: most missed sessions occur outside business hours",
        "- **No agent comparison risk**: no 'agent did better' counterfactual",
        "",
        "## Extraction stats",
        "",
        f"| Metric | Count |",
        f"|--------|-------|",
        f"| Total raw records | {stats['total']} |",
        f"| Excluded (non-Chat) | {stats.get('origin_not_chat', 0)} |",
        f"| Excluded (empty body) | {stats.get('empty_body', 0)} |",
        f"| Excluded (no visitor message) | {stats.get('no_visitor', 0)} |",
        f"| Excluded (had agent response) | {stats.get('has_agent', 0)} |",
        f"| **Missed sessions extracted** | **{len(passed)}** |",
        "",
        "## UC distribution (inferred)",
        "",
        "| UC | Count | % |",
        "|----|-------|---|",
    ]
    for uc in sorted(uc_dist.keys()):
        pct = 100 * uc_dist[uc] / max(len(passed), 1)
        report_lines.append(f"| {uc} | {uc_dist[uc]} | {pct:.1f}% |")

    report_lines += [
        "",
        "## Business hours distribution",
        "",
        "| Window | Count | % |",
        "|--------|-------|---|",
    ]
    for k, v in biz_dist.items():
        pct = 100 * v / max(len(passed), 1)
        report_lines.append(f"| {k} | {v} | {pct:.1f}% |")

    report_lines += [
        "",
        f"## Eval dataset sample ({len(sample)} sessions)",
        "",
        "Stratified by `inferred_uc`, prioritizing sessions with high `review_priority`",
        "(clear intent + urgency signal or persistence ≥ 3).",
        "",
        "| UC | Sample Count |",
        "|----|--------------|",
    ]
    for uc in sorted(sample_uc_dist.keys()):
        report_lines.append(f"| {uc} | {sample_uc_dist[uc]} |")

    report_lines += [
        "",
        "## Review priority distribution (sample)",
        "",
        "| Priority | Count |",
        "|----------|-------|",
    ]
    for p in ("high", "medium", "low"):
        report_lines.append(f"| {p} | {sample_priority_dist.get(p, 0)} |")

    report_lines += [
        "",
        "## How to review",
        "",
        "Each session has only the visitor's first message + any follow-ups (no agent context).",
        "Reviewer task is **lightweight** (~5 min/session):",
        "",
        "1. Read `first_message` and `all_visitor_text`",
        "2. Confirm or correct `inferred_uc`",
        "3. Mark `bot_could_help` (true/false): could V1 Bot likely resolve this?",
        "4. If `bot_could_help=true`, mark `expected_outcome`: resolve / intake_handover",
        "5. If user message is too vague: mark `requires_clarification` (true/false)",
        "",
        "## Use of this dataset",
        "",
        "- **Containment ceiling estimation**: ratio of `bot_could_help=true` × volume = upper bound for V1 containment",
        "- **First-touch intent classifier eval**: tests if classifier handles cold-start (no agent context)",
        "- **Cold-start UX validation**: tests Bot's opening turn quality without agent priming",
        "- **Off-hours flow validation**: tests `is_business_hours=false` branch",
        "",
        f"## Output files",
        "",
        f"- `data/filtered/missed_sessions.csv` — all {len(passed)} sessions (session-level)",
        f"- `data/filtered/missed_sessions_messages.csv` — all {len(all_turns)} visitor turns",
        f"- `data/eval_datasets/missed_sessions_dataset.csv` — {len(sample)} stratified sample",
        f"- `data/eval_datasets/missed_sessions_turns.csv` — {len(sample_turns)} sample turns",
    ]

    report_path = EVAL_OUT / "missed_sessions_REPORT.md"
    with open(report_path, "w") as f:
        f.write("\n".join(report_lines))
    print(f"  → data/eval_datasets/missed_sessions_REPORT.md")

    print(f"\nDone.")


if __name__ == "__main__":
    main()
