#!/usr/bin/env python3
"""
Extract clarification sessions: chats where the agent asked multiple
clarifying questions before resolution (or escalation, or abandonment).

These sessions test the Bot's `max_clarification_rounds=2` budget:
- When should Bot ask vs answer directly?
- Does Bot stop asking and escalate after the budget?
- Does Bot avoid over-clarification (annoying users → drop-off)?

Usage:
    python3 scripts/extract_clarification_sessions.py

Inputs (already produced by filter_chat_dataset.py):
    data/filtered/filtered_all.csv
    data/filtered/filtered_all_turns.csv

Outputs:
    data/filtered/clarification_sessions.csv
    data/filtered/clarification_sessions_turns.csv
    data/eval_datasets/clarification_dataset.csv
    data/eval_datasets/clarification_turns.csv
    data/eval_datasets/clarification_REPORT.md
"""

import csv
import re
import sys
from collections import defaultdict
from datetime import datetime
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
FILTERED = BASE / "data" / "filtered"
EVAL_OUT = BASE / "data" / "eval_datasets"
EVAL_OUT.mkdir(parents=True, exist_ok=True)

SESSIONS_IN = FILTERED / "filtered_all.csv"
TURNS_IN = FILTERED / "filtered_all_turns.csv"

# ─────────────────────────────────────────────
# Clarification detection rules
# ─────────────────────────────────────────────

# A clarification turn is an agent turn that:
#   1. Ends with "?" (or contains "?" + question phrasing)
#   2. Does NOT contain a URL / help link / "https" / "help.gumtree.com"
#   3. Does NOT contain resolution keywords ("you can ...", "please try ...",
#      numbered steps, "click ...", instruction patterns)
#   4. Length < 300 chars (long messages are usually answers, not clarifications)

QUESTION_PHRASES = [
    r"(?i)\b(which|what|when|where|why|how|who)\b",
    r"(?i)\b(do you have|could you|can you|would you|may i|please confirm|please advise|please share|please provide)\b",
    r"(?i)\b(are you|is this|is it|is the|did you|have you)\b",
    r"\?$",
]

RESOLUTION_INDICATORS = [
    r"https?://",
    r"(?i)help\.gumtree\.com",
    r"(?i)\byou (can|may|need to|should)\b",
    r"(?i)\bplease (try|follow|click|go to|navigate|visit|use)\b",
    r"(?i)\b(here.?s how|here are the steps|follow these|step \d|^\d+[\.)])\b",
    r"(?i)(I have escalated|I will escalate|let me escalate|i will get back|investigate)",
    r"(?i)\b(thank you|thanks|appreciate|sorry to hear)\b.*\b(reach|contact|get in touch)\b",
]

GREETING_PATTERN = re.compile(
    r"(?i)^(hello|hi|good (morning|afternoon|evening)|thanks (so much|for) reach)"
)


def is_clarification_turn(text):
    """Return True if this agent turn looks like a clarification question."""
    if not text:
        return False
    text = text.strip()
    if len(text) > 300:
        return False
    if "?" not in text:
        return False
    # Skip greetings (e.g., "How can I help?")
    if GREETING_PATTERN.match(text):
        return False
    # If it has resolution indicators, it's an answer not a clarification
    for p in RESOLUTION_INDICATORS:
        if re.search(p, text):
            return False
    # Must have a question phrase
    for p in QUESTION_PHRASES:
        if re.search(p, text):
            return True
    return False


# Topic categories of clarification (heuristic)
TOPIC_PATTERNS = [
    ("identifier", re.compile(r"(?i)(email address|ad id|listing id|reference|case number|order number|phone)")),
    ("device_or_platform", re.compile(r"(?i)(android|ios|browser|app|web|desktop|mobile|device)")),
    ("reproduction_steps", re.compile(r"(?i)(steps you took|what (did|happened) when|error message|when (did|do) you|exactly happen)")),
    ("location_or_category", re.compile(r"(?i)(location|category|area|region|postcode|town|city)")),
    ("issue_specifics", re.compile(r"(?i)(which (item|ad|listing|account)|specific|exactly|what (kind|type) of)")),
]


def classify_clarification_topic(text):
    if not text:
        return "other"
    for label, pat in TOPIC_PATTERNS:
        if pat.search(text):
            return label
    return "other"


# ─────────────────────────────────────────────
# Load + index turns by conversation
# ─────────────────────────────────────────────
def load_turns():
    print(f"Loading turns from {TURNS_IN.name}...")
    turns_by_conv = defaultdict(list)
    with open(TURNS_IN, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            turns_by_conv[row["conversation_id"]].append(row)
    # Sort each conversation by sequence
    for conv_id in turns_by_conv:
        turns_by_conv[conv_id].sort(key=lambda t: int(t.get("sequence", 0) or 0))
    print(f"  Loaded {sum(len(v) for v in turns_by_conv.values())} turns "
          f"across {len(turns_by_conv)} conversations")
    return turns_by_conv


def load_sessions():
    print(f"Loading sessions from {SESSIONS_IN.name}...")
    sessions = []
    with open(SESSIONS_IN, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            sessions.append(row)
    print(f"  Loaded {len(sessions)} sessions")
    return sessions


# ─────────────────────────────────────────────
# Per-session clarification analysis
# ─────────────────────────────────────────────
def analyze_session(session, turns):
    """Identify clarification pattern in a session."""
    clar_indices = []
    clar_topics = []

    for i, t in enumerate(turns):
        if t.get("role") == "agent" and is_clarification_turn(t.get("message_redacted", "")):
            clar_indices.append(i)
            clar_topics.append(
                classify_clarification_topic(t.get("message_redacted", ""))
            )

    clar_count = len(clar_indices)
    if clar_count < 2:
        return None  # not a clarification candidate

    # Determine outcome from last few turns
    last_turns = turns[-3:] if len(turns) >= 3 else turns
    last_text = " ".join(t.get("message_redacted", "") for t in last_turns)

    has_escalation = bool(re.search(
        r"(?i)(escalat|email you|24 hours|investigate|refer to|internal team)",
        last_text
    ))
    has_thanks = bool(re.search(
        r"(?i)(thank you|thanks|that.?s great|perfect|all good|sorted)",
        last_text
    ))
    has_user_silence = (
        len(turns) >= 2
        and turns[-1].get("role") == "agent"
        and turns[-2].get("role") == "agent"
    )  # agent talking to itself = user left

    if has_escalation:
        outcome = "escalated"
    elif has_thanks:
        outcome = "resolved"
    elif has_user_silence:
        outcome = "abandoned"
    else:
        outcome = "unclear"

    # Quality heuristic:
    # GOOD: clar_count == 2, outcome == resolved, topics are diverse (not repeated)
    # BAD: clar_count >= 3, outcome in (abandoned, escalated)
    over_clarified = clar_count > 2
    diverse_topics = len(set(clar_topics)) >= max(2, clar_count // 2)

    if outcome == "resolved" and clar_count <= 3 and diverse_topics:
        quality = "good"
    elif over_clarified and outcome in ("abandoned", "escalated"):
        quality = "bad"
    elif outcome == "unclear":
        quality = "unclear"
    else:
        quality = "mixed"

    # Review priority:
    # high: bad quality (clear failure pattern) OR clar_count >= 4
    # medium: good quality with 2+ clarifications (positive baseline)
    # low: unclear
    if quality == "bad" or clar_count >= 4:
        review_priority = "high"
    elif quality == "good":
        review_priority = "medium"
    else:
        review_priority = "low"

    return {
        "clarification_count": clar_count,
        "clarification_topics": "|".join(clar_topics),
        "clarification_topic_unique_count": len(set(clar_topics)),
        "clarification_outcome": outcome,
        "clarification_quality": quality,
        "over_clarified": over_clarified,
        "review_priority": review_priority,
    }


# ─────────────────────────────────────────────
# Stratified sample
# ─────────────────────────────────────────────
def stratified_sample(rows, target):
    """Stratify by (clarification_quality × primary_uc), prioritize 'bad' quality."""
    quality_priority = {"bad": 0, "good": 1, "mixed": 2, "unclear": 3}
    rows_sorted = sorted(rows, key=lambda r: (
        quality_priority.get(r["clarification_quality"], 9),
        -int(r["clarification_count"]),
    ))

    # Quotas: bad 40%, good 30%, mixed 20%, unclear 10%
    quotas = {
        "bad": int(target * 0.40),
        "good": int(target * 0.30),
        "mixed": int(target * 0.20),
        "unclear": int(target * 0.10),
    }

    by_quality = defaultdict(list)
    for r in rows_sorted:
        by_quality[r["clarification_quality"]].append(r)

    selected = []
    for q, quota in quotas.items():
        pool = by_quality.get(q, [])
        # Within each quality, do mini-stratification by primary_uc
        by_uc = defaultdict(list)
        for r in pool:
            by_uc[r.get("primary_uc", "UNKNOWN")].append(r)
        # Round-robin pick from UCs
        picked = []
        while len(picked) < quota and any(by_uc.values()):
            for uc in list(by_uc.keys()):
                if by_uc[uc] and len(picked) < quota:
                    picked.append(by_uc[uc].pop(0))
        selected.extend(picked)

    return selected[:target]


# ─────────────────────────────────────────────
# Output schemas
# ─────────────────────────────────────────────
SESSION_FIELDS = [
    # Inherited from filtered_all.csv
    "Id", "CaseId", "StartTime", "EndTime", "Platform",
    "Subject", "Reason", "CSAT",
    "total_turns", "agent_turns", "visitor_turns",
    "primary_uc", "all_ucs", "uc_confidence",
    "has_escalation_signal", "has_frustration_signal", "has_identifier_collection",
    "quality_score", "tags",
    # Clarification-specific
    "clarification_count", "clarification_topics", "clarification_topic_unique_count",
    "clarification_outcome", "clarification_quality", "over_clarified",
    "review_priority",
]
EVAL_EXTRA = ["eval_tier"]
TURN_FIELDS = [
    "conversation_id", "case_id", "sequence", "relative_time_sec",
    "role", "speaker", "message_redacted",
]


def write_csv(rows, path, fields):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)


# ─────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────
def main():
    sessions = load_sessions()
    turns_by_conv = load_turns()

    print("\nAnalyzing clarification patterns...")
    candidates = []
    for s in sessions:
        conv_id = s["Id"]
        turns = turns_by_conv.get(conv_id, [])
        if not turns:
            continue
        result = analyze_session(s, turns)
        if result:
            merged = dict(s)
            merged.update(result)
            candidates.append(merged)

    print(f"  Found {len(candidates)} sessions with clarification_count ≥ 2")

    # ── Distribution stats ──
    quality_dist = defaultdict(int)
    outcome_dist = defaultdict(int)
    count_dist = defaultdict(int)
    uc_dist = defaultdict(int)
    for r in candidates:
        quality_dist[r["clarification_quality"]] += 1
        outcome_dist[r["clarification_outcome"]] += 1
        count_dist[min(r["clarification_count"], 5)] += 1  # cap at 5+
        uc_dist[r.get("primary_uc", "UNKNOWN")] += 1

    print(f"\n── QUALITY DISTRIBUTION ──")
    for q in ("bad", "good", "mixed", "unclear"):
        n = quality_dist.get(q, 0)
        pct = 100 * n / max(len(candidates), 1)
        print(f"  {q:<10} {n:>5}  ({pct:>5.1f}%)")

    print(f"\n── OUTCOME DISTRIBUTION ──")
    for o, n in sorted(outcome_dist.items(), key=lambda x: -x[1]):
        pct = 100 * n / max(len(candidates), 1)
        print(f"  {o:<12} {n:>5}  ({pct:>5.1f}%)")

    print(f"\n── CLARIFICATION COUNT DISTRIBUTION ──")
    for c in sorted(count_dist.keys()):
        label = f"{c}+" if c == 5 else str(c)
        n = count_dist[c]
        pct = 100 * n / max(len(candidates), 1)
        print(f"  count={label:<3} {n:>5}  ({pct:>5.1f}%)")

    # ── Write filtered outputs ──
    print(f"\n── WRITING FILTERED OUTPUTS ──")
    write_csv(candidates, str(FILTERED / "clarification_sessions.csv"), SESSION_FIELDS)
    print(f"  → data/filtered/clarification_sessions.csv ({len(candidates)} sessions)")

    candidate_ids = {r["Id"] for r in candidates}
    all_clar_turns = []
    for cid in candidate_ids:
        all_clar_turns.extend(turns_by_conv.get(cid, []))
    write_csv(all_clar_turns, str(FILTERED / "clarification_sessions_turns.csv"), TURN_FIELDS)
    print(f"  → data/filtered/clarification_sessions_turns.csv ({len(all_clar_turns)} turns)")

    # ── Build eval dataset ──
    print(f"\n── BUILDING EVAL DATASET ──")
    target = 60
    sample = stratified_sample(candidates, target)
    for r in sample:
        r["eval_tier"] = "clarification"

    write_csv(sample, str(EVAL_OUT / "clarification_dataset.csv"),
              SESSION_FIELDS + EVAL_EXTRA)
    print(f"  → data/eval_datasets/clarification_dataset.csv ({len(sample)} sessions)")

    sample_ids = {r["Id"] for r in sample}
    sample_turns = [t for t in all_clar_turns if t["conversation_id"] in sample_ids]
    write_csv(sample_turns, str(EVAL_OUT / "clarification_turns.csv"), TURN_FIELDS)
    print(f"  → data/eval_datasets/clarification_turns.csv ({len(sample_turns)} turns)")

    # ── Eval sample distribution ──
    s_quality = defaultdict(int)
    s_outcome = defaultdict(int)
    s_uc = defaultdict(int)
    for r in sample:
        s_quality[r["clarification_quality"]] += 1
        s_outcome[r["clarification_outcome"]] += 1
        s_uc[r.get("primary_uc", "UNKNOWN")] += 1

    # ── Write report ──
    report_lines = [
        "# Clarification Sessions Extraction Report",
        "",
        f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        f"Source: `data/filtered/filtered_all.csv` + `filtered_all_turns.csv`",
        f"Script: `scripts/extract_clarification_sessions.py`",
        "",
        "## Definition",
        "",
        "**Clarification turn** = agent message that asks a question without offering an answer or link.",
        "Detection rules:",
        "- Contains `?`",
        "- Length < 300 chars",
        "- Does not contain URL / help.gumtree.com / numbered steps / 'you can'-style instruction",
        "- Contains a question phrase ('which', 'do you have', 'please confirm', etc.)",
        "- Excludes greetings ('How can I help?')",
        "",
        "**Clarification session** = session with ≥ 2 clarification turns.",
        "",
        "## Why these matter for V1",
        "",
        "- Validates `max_clarification_rounds=2` budget (tech_spec §11.5)",
        "- Detects over-clarification → user abandonment (BRD §6.3 禁用'反复 sorry')",
        "- Trains clarification trigger logic: when to ask vs answer directly",
        "- Maps clarification topic taxonomy (identifier, device, reproduction, etc.)",
        "",
        "## Extraction stats",
        "",
        f"| Metric | Count |",
        f"|--------|-------|",
        f"| Source sessions | {len(sessions)} |",
        f"| Sessions with clarification ≥ 2 | **{len(candidates)}** |",
        f"| Clarification turns total | {sum(int(r['clarification_count']) for r in candidates)} |",
        "",
        "## Quality distribution",
        "",
        "| Quality | Count | % | Definition |",
        "|---------|-------|---|------------|",
    ]
    for q, defn in [
        ("bad", "over_clarified=true AND outcome in (abandoned/escalated)"),
        ("good", "clar_count ≤ 3 AND outcome=resolved AND diverse topics"),
        ("mixed", "neither clearly good nor bad"),
        ("unclear", "outcome=unclear"),
    ]:
        n = quality_dist.get(q, 0)
        pct = 100 * n / max(len(candidates), 1)
        report_lines.append(f"| {q} | {n} | {pct:.1f}% | {defn} |")

    report_lines += [
        "",
        "## Outcome distribution",
        "",
        "| Outcome | Count |",
        "|---------|-------|",
    ]
    for o, n in sorted(outcome_dist.items(), key=lambda x: -x[1]):
        report_lines.append(f"| {o} | {n} |")

    report_lines += [
        "",
        "## Clarification count distribution",
        "",
        "| Count | Sessions |",
        "|-------|----------|",
    ]
    for c in sorted(count_dist.keys()):
        label = f"{c}+" if c == 5 else str(c)
        report_lines.append(f"| {label} | {count_dist[c]} |")

    report_lines += [
        "",
        f"## Eval dataset sample ({len(sample)} sessions)",
        "",
        "Stratified quotas: bad 40% / good 30% / mixed 20% / unclear 10%.",
        "Within each quality bucket, round-robin across `primary_uc`.",
        "",
        "### By quality",
        "",
        "| Quality | Count |",
        "|---------|-------|",
    ]
    for q in ("bad", "good", "mixed", "unclear"):
        report_lines.append(f"| {q} | {s_quality.get(q, 0)} |")

    report_lines += [
        "",
        "### By outcome",
        "",
        "| Outcome | Count |",
        "|---------|-------|",
    ]
    for o in sorted(s_outcome.keys()):
        report_lines.append(f"| {o} | {s_outcome[o]} |")

    report_lines += [
        "",
        "### By UC",
        "",
        "| UC | Count |",
        "|----|-------|",
    ]
    for uc in sorted(s_uc.keys()):
        report_lines.append(f"| {uc} | {s_uc[uc]} |")

    report_lines += [
        "",
        "## How to review",
        "",
        "Each session has a clarification pattern. Reviewer task (~8 min/session):",
        "",
        "1. Read full transcript (use `clarification_turns.csv` filter by conversation_id)",
        "2. Confirm or correct `clarification_quality` (good/bad/mixed)",
        "3. For each clarification turn, mark `was_clarification_needed` (true/false)",
        "4. Mark `expected_bot_clarification_count` (0/1/2): how many times *should* Bot ask?",
        "5. If `over_clarified=true`: mark `should_escalate_at_turn` (turn # where Bot should give up)",
        "6. Mark `clarification_replaceable_by_get_customer_context` (true/false): could the Bot have skipped asking by calling get_customer_context?",
        "",
        "## Use of this dataset",
        "",
        "- **Control Sanity Suite**: validates `max_clarification_rounds=2` enforcement",
        "- **Grader: clarification trigger correctness**: was each clarification justified?",
        "- **Grader: over-clarification detector**: did Bot keep asking when it should have escalated?",
        "- **Tool sequence design**: when can `get_customer_context` replace user asking?",
        "- **Discover policy override**: per-UC clarification budgets (some UCs may need stricter limits)",
        "",
        "## Output files",
        "",
        f"- `data/filtered/clarification_sessions.csv` — all {len(candidates)} sessions",
        f"- `data/filtered/clarification_sessions_turns.csv` — {len(all_clar_turns)} turns",
        f"- `data/eval_datasets/clarification_dataset.csv` — {len(sample)} stratified sample",
        f"- `data/eval_datasets/clarification_turns.csv` — {len(sample_turns)} sample turns",
    ]

    with open(EVAL_OUT / "clarification_REPORT.md", "w") as f:
        f.write("\n".join(report_lines))
    print(f"  → data/eval_datasets/clarification_REPORT.md")

    print(f"\nDone.")


if __name__ == "__main__":
    main()
