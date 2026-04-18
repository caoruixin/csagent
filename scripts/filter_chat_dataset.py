#!/usr/bin/env python3
"""
Filter valuable chat transcript records from the full 20K+ dataset.

Usage:
    python3 scripts/filter_chat_dataset.py <input_file> [--output-dir <dir>]

Input:  CSV/TSV/XLSX file with columns:
    Id, Body, CaseId, StartTime, EndTime, WaitTime, Platform,
    VisitorMessageCount, Subject, Reason, Origin, Description,
    StellaConnect__Star_Rating__c, ...

Output (written to --output-dir, default: data/filtered/):
    Session-level (one row per conversation, scored + tagged):
    - filtered_all.csv              — all records passing hard filters
    - tier1_self_serve.csv          — self-serve / FAQ resolution candidates
    - tier2_escalation.csv          — escalation pattern records
    - tier3_intake.csv              — intake + identifier collection records
    - tier4_frustration.csv         — bad-case / frustration records
    - tier5_drift.csv               — intent drift / multi-UC records
    - tier6_agent_handoff.csv       — agent handoff records

    Turn-level companions (one row per turn, PII-redacted):
    - <tier>_turns.csv              — columns: conversation_id, case_id,
                                       sequence, relative_time_sec, role,
                                       speaker, message_redacted
                                       (join back to session CSV via
                                       conversation_id = session.Id)

    - filter_report.txt             — summary statistics
"""

import csv
import html
import json
import os
import re
import sys
from datetime import datetime

# ─────────────────────────────────────────────
# 1. USE CASE MAPPING (Reason × Subject → UC)
# ─────────────────────────────────────────────

# Primary mapping: (Reason, Subject) → UC
# Reason is the broader category, Subject is the sub-category
UC_MAP_BY_REASON = {
    # Ad-related
    "Ad Management": {
        "default": "UC-H",  # most ad management in chat = removal/appeal
        "keywords": {
            "removed|deleted|taken down|blocked|suspended": "UC-H",
            "can.?t post|unable to post|won.?t post|posting": "UC-B",
            "not showing|not visible|where is|can.?t find|processing": "UC-A",
            "edit|change|update|modify": "UC-B",
            "policy|rules|violat|pet|breed": "UC-FP",
        }
    },
    "Gumtree Product": {
        "default": "UC-E",
        "keywords": {
            "featured|spotlight|promoted|boost": "UC-A",
            "search|filter|browse|category": "UC-E",
        }
    },
    "Technical Issues": {
        "default": "UC-K",
        "keywords": {
            "password|login|sign.?in|log.?in": "UC-D",
            "message|repl|notification": "UC-C",
            "app|crash|error|bug|glitch": "UC-K",
        }
    },
    "Messages & Replies": {
        "default": "UC-C",
        "keywords": {}
    },
    "Payments": {
        "default": "UC-F",
        "keywords": {
            "refund|dispute|chargeback|not as described|return": "UC-I",
            "cancel|cancelled": "UC-I",
        }
    },
    "Shipping": {
        "default": "UC-I",
        "keywords": {
            "refund|dispute|return|not as described": "UC-I",
        }
    },
    "Account Issues": {
        "default": "UC-D",
        "keywords": {
            "delete.*(account|data)|gdpr|erasure|sar": "UC-G",
            "ban|blacklist|restrict|suspend": "UC-FP",
        }
    },
    "Trust & Safety": {
        "default": "UC-J",
        "keywords": {
            "scam|fraud|fake|unsafe|threat": "UC-J",
            "report|flag": "UC-J",
        }
    },
    "GDPR": {
        "default": "UC-G",
        "keywords": {}
    },
}

# Subject-level overrides (some Subject values are more specific than Reason)
SUBJECT_OVERRIDES = {
    "Ad Support": None,  # too vague, fall through to Reason + keywords
    "Account Support": None,
    "Technical Support": "UC-K",
    "Pay & Ship": "UC-I",
}

# Body-level keyword patterns for use case detection (fallback)
UC_BODY_PATTERNS = [
    (r"(?i)(gdpr|delete\s+my\s+(account|data)|right\s+to\s+erasure|data\s+deletion|subject\s+access)", "UC-G"),
    (r"(?i)(wrongly\s+removed|incorrect(ly)?\s+delet|didn.?t\s+break|appeal|my\s+ad\s+(was|keep|got)\s+removed)", "UC-H"),
    (r"(?i)(refund|dispute|chargeback|not\s+as\s+described|money\s+back|return\s+(the|my))", "UC-I"),
    (r"(?i)(scam|fraud|fake\s+(listing|seller|buyer)|threat|unsafe|report.*(user|seller|buyer))", "UC-J"),
    (r"(?i)(password|can.?t\s+(log|sign)\s*in|reset|locked\s+out|account\s+access)", "UC-D"),
    (r"(?i)(message|repl(y|ies)|notification|inbox|can.?t\s+(read|see)\s+(my\s+)?message)", "UC-C"),
    (r"(?i)(how\s+(do\s+I|to)\s+post|edit\s+my\s+ad|change.*(category|location|price|image))", "UC-B"),
    (r"(?i)(not\s+showing|not\s+visible|where\s+is\s+my\s+ad|ad\s+review|processing|can.?t\s+find\s+my)", "UC-A"),
    (r"(?i)(communit(y|ies)\s+standard|poli(cy|cies)|violat|removed\s+(for|due)|pet\s+poli|breed|repost)", "UC-FP"),
    (r"(?i)(\bsearch\b.*\b(filter|result)|category\s+(list|guide)|how\s+does\s+gumtree\s+work|product\s+question)", "UC-E"),
    (r"(?i)(\b(payment|billing|invoice)\b|feature\s+(ad|item)|pay\s+&\s+ship)", "UC-F"),
    (r"(?i)(error|bug|crash|glitch|not\s+working|broken|technical)", "UC-K"),
]

# ─────────────────────────────────────────────
# 2a. PII REDACTION
# ─────────────────────────────────────────────
# Conservative — only redact high-confidence PII. Ad IDs (10 digits starting
# with 1) are preserved for debugging; only 11-digit UK phone numbers match.

PII_PATTERNS = [
    (re.compile(r"\b[\w\.\-\+]+@[\w\.\-]+\.\w{2,}\b"), "[EMAIL]"),
    (re.compile(r"\+44\s?\d{10}\b"), "[PHONE]"),
    (re.compile(r"\b0\d{10}\b"), "[PHONE]"),  # UK 11-digit (mobile / landline)
    (re.compile(r"\b[A-Z]{1,2}\d{1,2}[A-Z]?\s?\d[A-Z]{2}\b"), "[POSTCODE]"),
    (re.compile(r"\b\d{13,19}\b"), "[CARDLIKE]"),  # long digit strings
]


def redact_pii(text):
    """Return PII-redacted copy of text. Conservative; preserves ad IDs."""
    if not text:
        return text
    redacted = text
    for pattern, replacement in PII_PATTERNS:
        redacted = pattern.sub(replacement, redacted)
    return redacted


# ─────────────────────────────────────────────
# 2. SIGNAL DETECTION PATTERNS
# ─────────────────────────────────────────────

ESCALATION_SIGNALS = [
    r"(?i)(escalat|pass(ed)?\s+(this|it)\s+to|refer(red)?\s+to|internal\s+team)",
    r"(?i)(get\s+back\s+to\s+you\s+(via|by)\s+email|revert\s+back|email\s+you|within\s+24\s+hours)",
    r"(?i)(I\s+will\s+need\s+to\s+escalate|raise(d)?\s+this|investigate)",
    r"(?i)(connect\s+you\s+to\s+(the\s+team|an?\s+agent)|transfer(red)?)",
]

FRUSTRATION_SIGNALS = [
    r"\?{3,}",  # ??? or more
    r"(?i)(unacceptable|disgraceful|disgusting|ridiculous|appalling|outrageous)",
    r"(?i)(illegal|law|legal|legislation|trading\s+standards|solicitor|lawyer|court|sue)",
    r"(?i)(manager|supervisor|complaint|complain|formal)",
    r"(?i)(fraud(ulent)?|steal|stolen|rip.?off|\bcon(ned)\b)",
    r"(?i)(worst|terrible|horrible|useless|pathetic|joke|waste\s+of\s+time)",
    r"(?i)(are\s+you\s+(still\s+)?there|hello\???\s*$|anyone\s+there)",
    r"(?i)(this\s+is\s+(my|the)\s+\d+(rd|th|st|nd)\s+time)",
]

IDENTIFIER_COLLECTION_SIGNALS = [
    r"(?i)(confirm\s+the\s+email|email\s+address\s+(on|for|linked|registered))",
    r"(?i)(ad\s+id|listing\s+id|reference\s+number|order\s+number|case\s+number)",
    r"(?i)(do\s+you\s+have\s+(the|an?)\s+(email|ad|listing|reference))",
    r"(?i)(phone\s+number|contact\s+number|mobile\s+number)",
    r"(?i)(\w+@\w+\.\w+)",  # actual email provided
]

# ─────────────────────────────────────────────
# 3. TRANSCRIPT PARSING
# ─────────────────────────────────────────────

def parse_transcript(body_html):
    """Parse HTML body into structured turns.

    Returns:
        list of dict: [{role: 'agent'|'visitor'|'system', name: str,
                        timestamp: str, text: str}, ...]
        int: agent_count
        int: visitor_count
        list: agent_names
        bool: has_agent_handoff (multiple Chat Started blocks)
    """
    if not body_html:
        return [], 0, 0, [], False

    # Decode HTML entities
    text = html.unescape(str(body_html))

    # Count "Chat Started" blocks (agent handoff indicator)
    chat_started_count = len(re.findall(r'Chat Started:', text))
    has_agent_handoff = chat_started_count > 1

    # Extract agent names from "Agent {Name}" headers
    agent_names = list(set(re.findall(r'Agent\s+(\w+\s+\w+)', text)))
    if not agent_names:
        agent_names = list(set(re.findall(r'Agent\s+(\w+)', text)))

    # Parse individual turns: ( Xm Ys ) Name: message
    turn_pattern = re.compile(
        r'\(\s*(?:(\d+)m\s+)?(\d+)s?\s*\)\s+'  # timestamp
        r'([^:]+?):\s*'                          # speaker name
        r'(.*?)(?=\(\s*\d+[ms]|\Z|<p\s)',        # message content
        re.DOTALL
    )

    turns = []
    agent_count = 0
    visitor_count = 0
    agent_name_set = set(n.lower().split()[0] for n in agent_names) if agent_names else set()

    for match in turn_pattern.finditer(text):
        minutes = int(match.group(1)) if match.group(1) else 0
        seconds = int(match.group(2))
        speaker = match.group(3).strip()
        message = match.group(4).strip()

        # Clean HTML from message
        message = re.sub(r'<[^>]+>', ' ', message)
        message = re.sub(r'\s+', ' ', message).strip()

        if not message:
            continue

        # Determine role
        speaker_first = speaker.lower().split()[0] if speaker else ""
        if speaker_first in agent_name_set or any(
            speaker.lower().startswith(n.lower().split()[0]) for n in agent_names
        ):
            role = "agent"
            agent_count += 1
        else:
            role = "visitor"
            visitor_count += 1

        turns.append({
            "role": role,
            "name": speaker,
            "timestamp_seconds": minutes * 60 + seconds,
            "text": message,
        })

    return turns, agent_count, visitor_count, agent_names, has_agent_handoff


# ─────────────────────────────────────────────
# 4. USE CASE CLASSIFICATION
# ─────────────────────────────────────────────

def classify_use_case(reason, subject, description, body_text):
    """Map a record to V1 use case(s). Returns (primary_uc, all_ucs, confidence)."""
    all_ucs = set()
    combined_text = f"{subject or ''} {description or ''} {body_text or ''}"

    # Step 1: Try Reason-based mapping with keyword refinement
    primary_uc = None
    reason_clean = (reason or "").strip()

    if reason_clean in UC_MAP_BY_REASON:
        mapping = UC_MAP_BY_REASON[reason_clean]
        # Try keyword refinement using Description + Body
        for pattern, uc in mapping.get("keywords", {}).items():
            if re.search(pattern, combined_text, re.IGNORECASE):
                if primary_uc is None:
                    primary_uc = uc
                all_ucs.add(uc)
        if primary_uc is None:
            primary_uc = mapping["default"]
            all_ucs.add(primary_uc)

    # Step 2: Subject-level override
    subject_clean = (subject or "").strip()
    if subject_clean in SUBJECT_OVERRIDES and SUBJECT_OVERRIDES[subject_clean]:
        override_uc = SUBJECT_OVERRIDES[subject_clean]
        if primary_uc is None:
            primary_uc = override_uc
        all_ucs.add(override_uc)

    # Step 3: Body-level keyword detection (catch additional UCs / drift)
    for pattern, uc in UC_BODY_PATTERNS:
        if re.search(pattern, combined_text):
            all_ucs.add(uc)
            if primary_uc is None:
                primary_uc = uc

    # Confidence
    if primary_uc is None:
        return "UNMAPPED", set(), "none"

    confidence = "high" if len(all_ucs) == 1 else "medium" if len(all_ucs) <= 2 else "low"
    return primary_uc, all_ucs, confidence


def detect_signals(body_text):
    """Detect escalation, frustration, and identifier collection signals."""
    signals = {
        "escalation": False,
        "escalation_matches": [],
        "frustration": False,
        "frustration_matches": [],
        "identifier_collection": False,
        "identifier_matches": [],
    }

    for pattern in ESCALATION_SIGNALS:
        matches = re.findall(pattern, body_text or "")
        if matches:
            signals["escalation"] = True
            signals["escalation_matches"].extend(
                [m if isinstance(m, str) else m[0] for m in matches[:2]]
            )

    for pattern in FRUSTRATION_SIGNALS:
        matches = re.findall(pattern, body_text or "")
        if matches:
            signals["frustration"] = True
            signals["frustration_matches"].extend(
                [m if isinstance(m, str) else m[0] for m in matches[:2]]
            )

    for pattern in IDENTIFIER_COLLECTION_SIGNALS:
        matches = re.findall(pattern, body_text or "")
        if matches:
            signals["identifier_collection"] = True
            signals["identifier_matches"].extend(
                [m if isinstance(m, str) else m[0] for m in matches[:2]]
            )

    return signals


# ─────────────────────────────────────────────
# 5. QUALITY SCORING (0–100)
# ─────────────────────────────────────────────

def compute_quality_score(total_turns, uc_confidence, has_csat, signals, has_handoff):
    """Score a record's value for the CS agent project (0–100)."""
    score = 0

    # Turn richness (max 25)
    if total_turns >= 16:
        score += 25
    elif total_turns >= 9:
        score += 20
    elif total_turns >= 4:
        score += 10

    # UC mappability (max 20)
    if uc_confidence == "high":
        score += 20
    elif uc_confidence == "medium":
        score += 15  # multi-UC = drift, still valuable
    elif uc_confidence == "low":
        score += 5

    # CSAT (max 10)
    if has_csat:
        score += 10

    # Identifier collection (max 15)
    if signals["identifier_collection"]:
        score += 15

    # Escalation signal (max 15)
    if signals["escalation"]:
        score += 15

    # Frustration signal (max 15)
    if signals["frustration"]:
        score += 15

    # Agent handoff bonus (max 5 — rare, interesting pattern)
    if has_handoff:
        score += 5

    return min(score, 100)


# ─────────────────────────────────────────────
# 6. DESIGN BUCKET TAGGING
# ─────────────────────────────────────────────

FAQ_RESOLVABLE_UCS = {"UC-A", "UC-B", "UC-C", "UC-D", "UC-E", "UC-F", "UC-FP"}
INTAKE_UCS = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}


def assign_tags(primary_uc, all_ucs, signals, csat_rating):
    """Assign design bucket tags to a record."""
    tags = []

    # Self-serve candidate
    if primary_uc in FAQ_RESOLVABLE_UCS and not signals["escalation"]:
        tags.append("self_serve_candidate")

    # Escalation design
    if signals["escalation"] or primary_uc in INTAKE_UCS:
        tags.append("escalation_design")

    # Intake pattern
    if primary_uc in INTAKE_UCS and signals["identifier_collection"]:
        tags.append("intake_pattern")

    # Frustration / bad-case
    if signals["frustration"] or (csat_rating is not None and csat_rating <= 2):
        tags.append("frustration_case")

    # Intent drift
    if len(all_ucs) >= 2:
        tags.append("intent_drift")

    return tags


# ─────────────────────────────────────────────
# 7. FILE I/O
# ─────────────────────────────────────────────

def read_input(filepath):
    """Read CSV/TSV/XLSX input file. Returns list of dicts."""
    ext = os.path.splitext(filepath)[1].lower()

    if ext in (".xlsx", ".xls"):
        import openpyxl
        wb = openpyxl.load_workbook(filepath, read_only=True, data_only=True)
        ws = wb.active
        rows = list(ws.iter_rows(values_only=True))
        if not rows:
            return []
        headers = [str(h).strip() if h else f"col_{i}" for i, h in enumerate(rows[0])]
        records = []
        for row in rows[1:]:
            record = {}
            for i, val in enumerate(row):
                if i < len(headers):
                    record[headers[i]] = val
            records.append(record)
        wb.close()
        return records

    elif ext in (".csv", ".tsv"):
        delimiter = "\t" if ext == ".tsv" else ","
        # Auto-detect delimiter by checking first line
        with open(filepath, "r", encoding="utf-8-sig") as f:
            first_line = f.readline()
            if "\t" in first_line and first_line.count("\t") > first_line.count(","):
                delimiter = "\t"

        records = []
        with open(filepath, "r", encoding="utf-8-sig") as f:
            reader = csv.DictReader(f, delimiter=delimiter)
            for row in reader:
                records.append(dict(row))
        return records

    else:
        print(f"ERROR: Unsupported file format '{ext}'. Use .csv, .tsv, or .xlsx")
        sys.exit(1)


def write_csv(records, filepath, fieldnames):
    """Write list of dicts to CSV."""
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for r in records:
            writer.writerow(r)


# ─────────────────────────────────────────────
# 8. MAIN PIPELINE
# ─────────────────────────────────────────────

OUTPUT_FIELDS = [
    "Id", "CaseId", "StartTime", "EndTime", "WaitTime", "Platform",
    "VisitorMessageCount", "Subject", "Reason", "Origin", "Description",
    "CSAT",
    # Computed fields
    "total_turns", "agent_turns", "visitor_turns", "agent_names",
    "has_agent_handoff", "duration_seconds",
    "primary_uc", "all_ucs", "uc_confidence",
    "has_escalation_signal", "has_frustration_signal", "has_identifier_collection",
    "escalation_evidence", "frustration_evidence", "identifier_evidence",
    "quality_score", "tags",
]

TURN_FIELDS = [
    "conversation_id", "case_id", "sequence", "relative_time_sec",
    "role", "speaker", "message_redacted",
]


def build_form_turn(conversation_id, case_id, subject, description):
    """Build a synthetic turn 0 from pre-chat form data (Subject + Description).

    The pre-chat form is submitted by the user BEFORE the chat session starts.
    In the current system, this creates a Case and the agent sees the form data
    in their console before greeting the user. For the Bot, this is the first
    input it receives — effectively the user's turn 0.
    """
    parts = []
    if subject:
        parts.append(f"Subject: {subject}")
    if description:
        parts.append(f"Description: {description}")
    if not parts:
        return None
    return {
        "conversation_id": conversation_id,
        "case_id": case_id,
        "sequence": 0,
        "relative_time_sec": 0,
        "role": "visitor",
        "speaker": "[PRE_CHAT_FORM]",
        "message_redacted": redact_pii("[Form] " + " | ".join(parts)),
    }


def build_turn_rows(conversation_id, case_id, turns, subject="", description=""):
    """Build turn-level rows with PII redaction and sequence numbering.

    Injects a synthetic sequence=0 turn from pre-chat form data when available.
    """
    rows = []
    form_turn = build_form_turn(conversation_id, case_id, subject, description)
    if form_turn:
        rows.append(form_turn)
    for seq, t in enumerate(turns, start=1):
        rows.append({
            "conversation_id": conversation_id,
            "case_id": case_id,
            "sequence": seq,
            "relative_time_sec": t["timestamp_seconds"],
            "role": t["role"],
            "speaker": t["name"],
            "message_redacted": redact_pii(t["text"]),
        })
    return rows


def process_record(record):
    """Process a single record through the full pipeline.

    Returns:
        (session_dict, turn_rows_list, reason_str) — session_dict/turn_rows are
        None when hard-filtered out.
    """
    body = record.get("Body", "") or ""
    origin = (record.get("Origin", "") or "").strip()
    visitor_msg_count_raw = record.get("VisitorMessageCount")

    try:
        visitor_msg_count = int(visitor_msg_count_raw) if visitor_msg_count_raw else 0
    except (ValueError, TypeError):
        visitor_msg_count = 0

    # ── Hard Filter 1: Origin must be Chat ──
    if origin and origin.lower() != "chat":
        return None, None, "origin_not_chat"

    # ── Hard Filter 2: Body must be non-empty ──
    if not body or len(body.strip()) < 50:
        return None, None, "empty_body"

    # ── Parse transcript ──
    turns, agent_count, visitor_count, agent_names, has_handoff = parse_transcript(body)
    total_turns = agent_count + visitor_count

    # ── Hard Filter 3: Must have agent + visitor messages ──
    if agent_count < 1 or visitor_count < 1:
        return None, None, "missing_agent_or_visitor"

    # ── Hard Filter 4: Total turns >= 4 ──
    if total_turns < 4:
        return None, None, "too_few_turns"

    # ── Classify use case ──
    body_text = " ".join(t["text"] for t in turns)
    reason = (record.get("Reason", "") or "").strip()
    subject = (record.get("Subject", "") or "").strip()
    description = (record.get("Description", "") or "").strip()

    primary_uc, all_ucs, uc_confidence = classify_use_case(
        reason, subject, description, body_text
    )

    # ── Detect signals ──
    signals = detect_signals(body_text)

    # ── CSAT ──
    csat_raw = record.get("StellaConnect__Star_Rating__c")
    csat = None
    if csat_raw:
        try:
            csat = int(float(csat_raw))
        except (ValueError, TypeError):
            csat = None

    # ── Quality score ──
    quality_score = compute_quality_score(
        total_turns, uc_confidence, csat is not None, signals, has_handoff
    )

    # ── Tags ──
    tags = assign_tags(primary_uc, all_ucs, signals, csat)

    # ── Duration ──
    duration = None
    try:
        start = record.get("StartTime", "")
        end = record.get("EndTime", "")
        if start and end:
            # Handle both string and datetime objects
            if isinstance(start, datetime):
                start_dt = start
            else:
                start_dt = datetime.fromisoformat(str(start).replace("+0000", "+00:00").replace("Z", "+00:00"))
            if isinstance(end, datetime):
                end_dt = end
            else:
                end_dt = datetime.fromisoformat(str(end).replace("+0000", "+00:00").replace("Z", "+00:00"))
            duration = int((end_dt - start_dt).total_seconds())
    except Exception:
        duration = None

    # ── Build output record ──
    out = {
        "Id": record.get("Id", ""),
        "CaseId": record.get("CaseId", ""),
        "StartTime": record.get("StartTime", ""),
        "EndTime": record.get("EndTime", ""),
        "WaitTime": record.get("WaitTime", ""),
        "Platform": record.get("Platform", ""),
        "VisitorMessageCount": visitor_msg_count,
        "Subject": subject,
        "Reason": reason,
        "Origin": origin,
        "Description": description[:200] if description else "",
        "CSAT": csat,
        "total_turns": total_turns,
        "agent_turns": agent_count,
        "visitor_turns": visitor_count,
        "agent_names": "|".join(agent_names),
        "has_agent_handoff": has_handoff,
        "duration_seconds": duration,
        "primary_uc": primary_uc,
        "all_ucs": "|".join(sorted(all_ucs)) if all_ucs else "",
        "uc_confidence": uc_confidence,
        "has_escalation_signal": signals["escalation"],
        "has_frustration_signal": signals["frustration"],
        "has_identifier_collection": signals["identifier_collection"],
        "escalation_evidence": "|".join(signals["escalation_matches"][:3]),
        "frustration_evidence": "|".join(signals["frustration_matches"][:3]),
        "identifier_evidence": "|".join(signals["identifier_matches"][:3]),
        "quality_score": quality_score,
        "tags": "|".join(tags),
    }

    turn_rows = build_turn_rows(out["Id"], out["CaseId"], turns,
                                subject=subject, description=description)
    return out, turn_rows, "pass"


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    input_file = sys.argv[1]
    output_dir = "data/filtered"

    # Parse --output-dir
    for i, arg in enumerate(sys.argv):
        if arg == "--output-dir" and i + 1 < len(sys.argv):
            output_dir = sys.argv[i + 1]

    if not os.path.exists(input_file):
        print(f"ERROR: Input file not found: {input_file}")
        sys.exit(1)

    print(f"Reading: {input_file}")
    records = read_input(input_file)
    print(f"Total records: {len(records)}")

    # ── Process all records ──
    passed = []
    filter_stats = {
        "total": len(records),
        "origin_not_chat": 0,
        "empty_body": 0,
        "missing_agent_or_visitor": 0,
        "too_few_turns": 0,
        "pass": 0,
    }

    turns_by_id = {}
    for record in records:
        result, turn_rows, reason = process_record(record)
        filter_stats[reason] = filter_stats.get(reason, 0) + 1
        if result:
            passed.append(result)
            turns_by_id[result["Id"]] = turn_rows

    # Sort by quality score descending
    passed.sort(key=lambda x: x["quality_score"], reverse=True)

    # ── Write outputs ──
    print(f"\nPassed hard filters: {len(passed)}/{len(records)}")

    def write_tier(subset, filename):
        """Write session-level CSV and its turn-level companion."""
        write_csv(subset, os.path.join(output_dir, filename), OUTPUT_FIELDS)
        turns_filename = filename.replace(".csv", "_turns.csv")
        tier_turns = []
        subset_ids = {r["Id"] for r in subset}
        # Preserve session ordering (by quality score) when emitting turns
        for r in subset:
            tier_turns.extend(turns_by_id.get(r["Id"], []))
        write_csv(tier_turns, os.path.join(output_dir, turns_filename), TURN_FIELDS)
        return len(tier_turns)

    all_turn_count = write_tier(passed, "filtered_all.csv")
    print(f"  → {output_dir}/filtered_all.csv  (+ _turns.csv, {all_turn_count} turns)")

    # Split by tags
    tag_files = {
        "self_serve_candidate": "tier1_self_serve.csv",
        "escalation_design": "tier2_escalation.csv",
        "intake_pattern": "tier3_intake.csv",
        "frustration_case": "tier4_frustration.csv",
        "intent_drift": "tier5_drift.csv",
    }

    tag_counts = {}
    for tag, filename in tag_files.items():
        subset = [r for r in passed if tag in (r.get("tags") or "")]
        tag_counts[tag] = len(subset)
        turn_count = write_tier(subset, filename)
        print(f"  → {output_dir}/{filename} ({len(subset)} sessions, {turn_count} turns)")

    # Agent handoff
    handoff_subset = [r for r in passed if r.get("has_agent_handoff")]
    tag_counts["agent_handoff"] = len(handoff_subset)
    turn_count = write_tier(handoff_subset, "tier6_agent_handoff.csv")
    print(f"  → {output_dir}/tier6_agent_handoff.csv ({len(handoff_subset)} sessions, {turn_count} turns)")

    # ── UC distribution ──
    uc_dist = {}
    for r in passed:
        uc = r["primary_uc"]
        uc_dist[uc] = uc_dist.get(uc, 0) + 1

    # ── Score distribution ──
    score_buckets = {"0-19": 0, "20-39": 0, "40-59": 0, "60-79": 0, "80-100": 0}
    for r in passed:
        s = r["quality_score"]
        if s >= 80:
            score_buckets["80-100"] += 1
        elif s >= 60:
            score_buckets["60-79"] += 1
        elif s >= 40:
            score_buckets["40-59"] += 1
        elif s >= 20:
            score_buckets["20-39"] += 1
        else:
            score_buckets["0-19"] += 1

    # ── Write report ──
    report_lines = [
        "=" * 60,
        "CHAT DATASET FILTER REPORT",
        f"Input: {input_file}",
        f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M')}",
        "=" * 60,
        "",
        "── HARD FILTER RESULTS ──",
        f"Total input records:        {filter_stats['total']:>6}",
        f"  Excluded (non-Chat):      {filter_stats.get('origin_not_chat', 0):>6}",
        f"  Excluded (empty body):    {filter_stats.get('empty_body', 0):>6}",
        f"  Excluded (no agent/user): {filter_stats.get('missing_agent_or_visitor', 0):>6}",
        f"  Excluded (< 4 turns):     {filter_stats.get('too_few_turns', 0):>6}",
        f"  ── Passed:                {len(passed):>6}  "
        f"({100*len(passed)/max(filter_stats['total'],1):.1f}%)",
        "",
        "── USE CASE DISTRIBUTION ──",
    ]
    for uc in sorted(uc_dist.keys()):
        pct = 100 * uc_dist[uc] / max(len(passed), 1)
        report_lines.append(f"  {uc:<12} {uc_dist[uc]:>5}  ({pct:>5.1f}%)")

    report_lines += [
        "",
        "── QUALITY SCORE DISTRIBUTION ──",
    ]
    for bucket, count in score_buckets.items():
        pct = 100 * count / max(len(passed), 1)
        report_lines.append(f"  {bucket:<8}  {count:>5}  ({pct:>5.1f}%)")

    report_lines += [
        "",
        "── TAG DISTRIBUTION ──",
    ]
    for tag, count in sorted(tag_counts.items(), key=lambda x: -x[1]):
        pct = 100 * count / max(len(passed), 1)
        report_lines.append(f"  {tag:<25} {count:>5}  ({pct:>5.1f}%)")

    report_lines += [
        "",
        "── CSAT COVERAGE ──",
        f"  With CSAT rating: {sum(1 for r in passed if r['CSAT'] is not None):>5}  "
        f"({100*sum(1 for r in passed if r['CSAT'] is not None)/max(len(passed),1):.1f}%)",
    ]

    # CSAT distribution
    csat_dist = {}
    for r in passed:
        if r["CSAT"] is not None:
            csat_dist[r["CSAT"]] = csat_dist.get(r["CSAT"], 0) + 1
    for rating in sorted(csat_dist.keys()):
        report_lines.append(f"    {rating}★: {csat_dist[rating]:>5}")

    report_lines += [
        "",
        "── RECOMMENDED NEXT STEPS ──",
        "1. Review tier1_self_serve.csv (score >= 40) → Golden Dataset candidates",
        "2. Review tier4_frustration.csv → Bad-case bank",
        "3. Review tier5_drift.csv → Control/drift test cases",
        "4. Review tier3_intake.csv → Tool contract test cases",
        "5. Sample from tier2_escalation.csv → Handover dataset",
        "6. Records with quality_score >= 60 are high-priority for human review",
        "",
        f"Total output files in: {output_dir}/",
    ]

    report_text = "\n".join(report_lines)
    report_path = os.path.join(output_dir, "filter_report.txt")
    os.makedirs(output_dir, exist_ok=True)
    with open(report_path, "w") as f:
        f.write(report_text)

    print(f"\n{report_text}")


if __name__ == "__main__":
    main()
