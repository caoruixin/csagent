#!/usr/bin/env python3
"""
Build eval datasets from filtered chat tiers.

Produces stratified samples aligned with workbook §1.5.1 / §6.5 targets:
  1. Golden Dataset        (≥150)  — from tier1_self_serve, quality >= 40
  2. Bad-case Bank         (≥80)   — from tier4_frustration, diversified
  3. Drift/Control Dataset (≥20)   — from tier5_drift, graded complexity
  4. Intake/Tool Contract  (≥40)   — from tier3_intake, UC-G/H/I/J/K
  5. Handover Dataset      (≥60)   — from tier2_escalation, per-reason coverage
  6. Escalation Dataset    (≥120)  — from tier2_escalation, broader sample
  7. Human Review Queue             — quality >= 60 across all tiers
"""

import csv
import os
import random
import sys
from collections import defaultdict
from pathlib import Path

random.seed(42)

BASE = Path(__file__).resolve().parent.parent / "data"
FILTERED = BASE / "filtered"
OUT = BASE / "eval_datasets"
OUT.mkdir(exist_ok=True)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def load_csv(name: str) -> list[dict]:
    path = FILTERED / name
    with open(path, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def score(row: dict) -> float:
    try:
        return float(row.get("quality_score", 0) or 0)
    except (ValueError, TypeError):
        return 0.0


def write_csv(rows: list[dict], filename: str, fieldnames: list[str] | None = None):
    if not rows:
        return
    path = OUT / filename
    fns = fieldnames or list(rows[0].keys())
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fns, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)
    print(f"  ✓ {filename}: {len(rows)} rows")


def stratified_sample(rows: list[dict], key_field: str, target: int,
                      min_per_group: int = 2, quality_min: float = 0.0) -> list[dict]:
    """Stratified sample ensuring min_per_group from each group, then fill proportionally."""
    eligible = [r for r in rows if score(r) >= quality_min]
    groups = defaultdict(list)
    for r in eligible:
        groups[r.get(key_field, "UNKNOWN")].append(r)

    # Sort each group by quality descending
    for g in groups:
        groups[g].sort(key=lambda r: score(r), reverse=True)

    selected = []
    remaining_budget = target

    # Phase 1: guarantee minimum per group
    for g, members in groups.items():
        take = min(min_per_group, len(members))
        selected.extend(members[:take])
        groups[g] = members[take:]
        remaining_budget -= take

    # Phase 2: fill proportionally from remaining
    if remaining_budget > 0:
        pool = []
        for g, members in groups.items():
            pool.extend(members)
        pool.sort(key=lambda r: score(r), reverse=True)
        selected.extend(pool[:remaining_budget])

    return selected[:target]


# ---------------------------------------------------------------------------
# Summary fields for output datasets (session-level, no transcript)
# ---------------------------------------------------------------------------

SESSION_FIELDS = [
    "Id", "CaseId", "StartTime", "EndTime", "Platform",
    "Subject", "Reason", "CSAT",
    "total_turns", "agent_turns", "visitor_turns",
    "primary_uc", "all_ucs", "uc_confidence",
    "has_escalation_signal", "has_frustration_signal", "has_identifier_collection",
    "escalation_evidence", "frustration_evidence", "identifier_evidence",
    "quality_score", "tags",
]

# Extra fields for specific datasets
GOLDEN_EXTRA = ["eval_tier", "eval_priority"]
BADCASE_EXTRA = ["eval_tier", "frustration_type"]
DRIFT_EXTRA = ["eval_tier", "drift_complexity", "uc_count"]
INTAKE_EXTRA = ["eval_tier"]
HANDOVER_EXTRA = ["eval_tier", "escalation_reason_group"]
ESCALATION_EXTRA = ["eval_tier", "escalation_reason_group"]


# ---------------------------------------------------------------------------
# 1. Golden Dataset
# ---------------------------------------------------------------------------

def build_golden():
    print("\n═══ 1. Golden Dataset (target ≥ 150) ═══")
    rows = load_csv("tier1_self_serve.csv")
    print(f"  Pool: {len(rows)} sessions")

    # All UCs present in self-serve: UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP
    ucs = sorted(set(r["primary_uc"] for r in rows))
    print(f"  UCs: {ucs}")

    # Tier A: quality >= 60 (high priority for human review)
    tier_a = [r for r in rows if score(r) >= 60]
    # Tier B: quality 40-59
    tier_b = [r for r in rows if 40 <= score(r) < 60]

    print(f"  Tier A (≥60): {len(tier_a)}")
    print(f"  Tier B (40-59): {len(tier_b)}")

    # Strategy: take ALL tier A (298), then fill to 150 from tier B if needed
    # But we want balanced UC coverage, so stratify

    # Target per UC: at least 10 per UC from tier A, rest proportional
    target = 150
    min_per_uc = max(5, target // (len(ucs) * 2))  # at least 10 per UC

    selected = stratified_sample(tier_a, "primary_uc", target, min_per_group=min_per_uc, quality_min=60)

    # If under target, backfill from tier B
    selected_ids = {r["Id"] for r in selected}
    if len(selected) < target:
        backfill = [r for r in tier_b if r["Id"] not in selected_ids]
        needed = target - len(selected)
        backfill_sample = stratified_sample(backfill, "primary_uc", needed, min_per_group=3, quality_min=40)
        selected.extend(backfill_sample)

    # Annotate
    for r in selected:
        r["eval_tier"] = "golden"
        r["eval_priority"] = "high" if score(r) >= 60 else "medium"

    # Report UC coverage
    uc_counts = defaultdict(int)
    for r in selected:
        uc_counts[r["primary_uc"]] += 1
    print(f"  Selected: {len(selected)}")
    for uc in sorted(uc_counts):
        print(f"    {uc}: {uc_counts[uc]}")

    write_csv(selected, "golden_dataset.csv", SESSION_FIELDS + GOLDEN_EXTRA)
    return selected


# ---------------------------------------------------------------------------
# 2. Bad-case Bank
# ---------------------------------------------------------------------------

def classify_frustration(evidence: str) -> str:
    """Classify frustration type from evidence string."""
    ev = (evidence or "").lower()
    if "sue" in ev or "law" in ev or "legal" in ev:
        return "litigation_threat"
    if "still there" in ev or "are you there" in ev or "hello" in ev:
        return "abandonment_wait"
    if "fraud" in ev:
        return "fraud_concern"
    if "manager" in ev or "supervisor" in ev or "escalat" in ev:
        return "escalation_demand"
    if "refund" in ev or "money" in ev:
        return "financial_dispute"
    if "complaint" in ev or "disgust" in ev or "rubbish" in ev or "useless" in ev:
        return "general_complaint"
    return "other"


def build_badcase():
    print("\n═══ 2. Bad-case Bank (target ≥ 80) ═══")
    rows = load_csv("tier4_frustration.csv")
    print(f"  Pool: {len(rows)} sessions")

    # Classify frustration types
    for r in rows:
        r["frustration_type"] = classify_frustration(r.get("frustration_evidence", ""))

    type_dist = defaultdict(int)
    for r in rows:
        type_dist[r["frustration_type"]] += 1
    print("  Frustration type distribution:")
    for t, c in sorted(type_dist.items(), key=lambda x: -x[1]):
        print(f"    {t}: {c}")

    # Target: 80+ with diversified frustration types AND UC coverage
    target = 100  # overshoot slightly for richer bank

    # Two-level stratification: first by frustration_type, then by UC
    # Minimum 5 per frustration type, minimum 2 per UC within type
    type_groups = defaultdict(list)
    for r in rows:
        if score(r) >= 40:  # minimum quality threshold
            type_groups[r["frustration_type"]].append(r)

    for g in type_groups:
        type_groups[g].sort(key=lambda r: score(r), reverse=True)

    selected = []
    per_type_target = max(8, target // len(type_groups))

    for ftype, members in type_groups.items():
        # Within each frustration type, stratify by UC
        take = min(per_type_target, len(members))
        type_sample = stratified_sample(members, "primary_uc", take, min_per_group=1, quality_min=40)
        selected.extend(type_sample)

    # Deduplicate and trim
    seen = set()
    deduped = []
    for r in selected:
        if r["Id"] not in seen:
            seen.add(r["Id"])
            deduped.append(r)
    selected = deduped

    # Sort by quality descending, take top target
    selected.sort(key=lambda r: score(r), reverse=True)
    selected = selected[:target]

    for r in selected:
        r["eval_tier"] = "badcase"

    # Report
    uc_counts = defaultdict(int)
    type_counts = defaultdict(int)
    for r in selected:
        uc_counts[r["primary_uc"]] += 1
        type_counts[r["frustration_type"]] += 1
    print(f"  Selected: {len(selected)}")
    print("  By frustration type:")
    for t in sorted(type_counts):
        print(f"    {t}: {type_counts[t]}")
    print("  By UC:")
    for uc in sorted(uc_counts):
        print(f"    {uc}: {uc_counts[uc]}")

    write_csv(selected, "badcase_bank.csv", SESSION_FIELDS + BADCASE_EXTRA)
    return selected


# ---------------------------------------------------------------------------
# 3. Drift / Control Test Dataset
# ---------------------------------------------------------------------------

def build_drift():
    print("\n═══ 3. Drift/Control Dataset (target ≥ 20) ═══")
    rows = load_csv("tier5_drift.csv")
    print(f"  Pool: {len(rows)} sessions")

    # Classify drift complexity by UC count
    for r in rows:
        ucs = (r.get("all_ucs") or "").split("|")
        uc_count = len([u for u in ucs if u.strip()])
        r["uc_count"] = str(uc_count)
        if uc_count <= 2:
            r["drift_complexity"] = "simple"
        elif uc_count == 3:
            r["drift_complexity"] = "medium"
        else:
            r["drift_complexity"] = "complex"

    complexity_dist = defaultdict(int)
    for r in rows:
        complexity_dist[r["drift_complexity"]] += 1
    print("  Complexity distribution:")
    for c, n in sorted(complexity_dist.items()):
        print(f"    {c}: {n}")

    # Target: 30 (overshoot 20 target), balanced across complexity levels
    target = 30
    selected = []

    for complexity in ["simple", "medium", "complex"]:
        pool = [r for r in rows if r["drift_complexity"] == complexity and score(r) >= 50]
        pool.sort(key=lambda r: score(r), reverse=True)
        take = target // 3
        if complexity == "complex":
            take = target - len(selected)  # fill remainder
        sample = stratified_sample(pool, "primary_uc", take, min_per_group=1, quality_min=50)
        selected.extend(sample)

    for r in selected:
        r["eval_tier"] = "drift_control"

    # Report
    comp_counts = defaultdict(int)
    uc_counts = defaultdict(int)
    for r in selected:
        comp_counts[r["drift_complexity"]] += 1
        uc_counts[r["primary_uc"]] += 1
    print(f"  Selected: {len(selected)}")
    print("  By complexity:")
    for c in ["simple", "medium", "complex"]:
        print(f"    {c}: {comp_counts.get(c, 0)}")
    print("  By primary UC:")
    for uc in sorted(uc_counts):
        print(f"    {uc}: {uc_counts[uc]}")

    write_csv(selected, "drift_control_dataset.csv", SESSION_FIELDS + DRIFT_EXTRA)
    return selected


# ---------------------------------------------------------------------------
# 4. Intake / Tool Contract Dataset
# ---------------------------------------------------------------------------

def build_intake():
    print("\n═══ 4. Intake/Tool Contract Dataset (target ≥ 40) ═══")
    rows = load_csv("tier3_intake.csv")
    print(f"  Pool: {len(rows)} sessions")

    # Only UC-G/H/I/J/K are intake-then-handover
    intake_ucs = {"UC-G", "UC-H", "UC-I", "UC-J", "UC-K"}
    eligible = [r for r in rows if r.get("primary_uc") in intake_ucs]
    print(f"  Eligible (intake UCs only): {len(eligible)}")

    # Target: 50 (overshoot), minimum 5 per UC
    target = 50
    selected = stratified_sample(eligible, "primary_uc", target, min_per_group=5, quality_min=50)

    for r in selected:
        r["eval_tier"] = "intake_tool_contract"

    uc_counts = defaultdict(int)
    for r in selected:
        uc_counts[r["primary_uc"]] += 1
    print(f"  Selected: {len(selected)}")
    for uc in sorted(uc_counts):
        print(f"    {uc}: {uc_counts[uc]}")

    write_csv(selected, "intake_tool_contract_dataset.csv", SESSION_FIELDS + INTAKE_EXTRA)
    return selected


# ---------------------------------------------------------------------------
# 5. Handover Dataset
# ---------------------------------------------------------------------------

def normalize_reason(reason: str) -> str:
    """Normalize Reason field for grouping."""
    r = (reason or "").strip()
    if not r:
        return "Unknown"
    # Fix known inconsistencies
    if r in ("Technical Issues", "Technical Issue"):
        return "Technical Issues"
    return r


def build_handover():
    print("\n═══ 5. Handover Dataset (target ≥ 60) ═══")
    rows = load_csv("tier2_escalation.csv")
    print(f"  Pool: {len(rows)} sessions")

    for r in rows:
        r["escalation_reason_group"] = normalize_reason(r.get("Reason", ""))

    reason_dist = defaultdict(int)
    for r in rows:
        reason_dist[r["escalation_reason_group"]] += 1
    print("  Reason distribution:")
    for rg, c in sorted(reason_dist.items(), key=lambda x: -x[1]):
        print(f"    {rg}: {c}")

    # Target: 5-10 per reason group, ≥60 total
    target = 80
    reasons = sorted(reason_dist.keys())
    per_reason = max(5, target // len(reasons))

    selected = []
    for reason in reasons:
        pool = [r for r in rows if r["escalation_reason_group"] == reason and score(r) >= 40]
        pool.sort(key=lambda r: score(r), reverse=True)
        take = min(per_reason, len(pool))
        selected.extend(pool[:take])

    # Sort by quality, trim
    selected.sort(key=lambda r: score(r), reverse=True)
    if len(selected) > target:
        # Keep at least min_per_reason, trim the rest
        selected = selected[:target]

    for r in selected:
        r["eval_tier"] = "handover"

    reason_counts = defaultdict(int)
    for r in selected:
        reason_counts[r["escalation_reason_group"]] += 1
    print(f"  Selected: {len(selected)}")
    for rg in sorted(reason_counts):
        print(f"    {rg}: {reason_counts[rg]}")

    write_csv(selected, "handover_dataset.csv", SESSION_FIELDS + HANDOVER_EXTRA)
    return selected


# ---------------------------------------------------------------------------
# 6. Escalation Dataset (broader sample)
# ---------------------------------------------------------------------------

def build_escalation():
    print("\n═══ 6. Escalation Dataset (target ≥ 120) ═══")
    rows = load_csv("tier2_escalation.csv")
    print(f"  Pool: {len(rows)} sessions")

    for r in rows:
        r["escalation_reason_group"] = normalize_reason(r.get("Reason", ""))

    target = 150
    selected = stratified_sample(rows, "primary_uc", target, min_per_group=5, quality_min=40)

    for r in selected:
        r["eval_tier"] = "escalation"
        r["escalation_reason_group"] = normalize_reason(r.get("Reason", ""))

    uc_counts = defaultdict(int)
    reason_counts = defaultdict(int)
    for r in selected:
        uc_counts[r["primary_uc"]] += 1
        reason_counts[r["escalation_reason_group"]] += 1
    print(f"  Selected: {len(selected)}")
    print("  By UC:")
    for uc in sorted(uc_counts):
        print(f"    {uc}: {uc_counts[uc]}")
    print("  By Reason:")
    for rg in sorted(reason_counts):
        print(f"    {rg}: {reason_counts[rg]}")

    write_csv(selected, "escalation_dataset.csv", SESSION_FIELDS + ESCALATION_EXTRA)
    return selected


# ---------------------------------------------------------------------------
# 7. Human Review Queue (quality >= 60 across all tiers)
# ---------------------------------------------------------------------------

def build_human_review_queue(all_selected: dict[str, list[dict]]):
    print("\n═══ 7. Human Review Queue (quality ≥ 60) ═══")

    # Collect all unique high-quality sessions across datasets
    seen = set()
    queue = []
    for dataset_name, rows in all_selected.items():
        for r in rows:
            if score(r) >= 60 and r["Id"] not in seen:
                seen.add(r["Id"])
                r_copy = dict(r)
                r_copy["source_dataset"] = dataset_name
                queue.append(r_copy)

    queue.sort(key=lambda r: score(r), reverse=True)

    uc_counts = defaultdict(int)
    for r in queue:
        uc_counts[r["primary_uc"]] += 1
    print(f"  Total unique sessions for human review: {len(queue)}")
    for uc in sorted(uc_counts):
        print(f"    {uc}: {uc_counts[uc]}")

    fields = SESSION_FIELDS + ["source_dataset"]
    write_csv(queue, "human_review_queue.csv", fields)
    return queue


# ---------------------------------------------------------------------------
# 8. Dataset cross-reference: extract turns for selected sessions
# ---------------------------------------------------------------------------

def extract_turns_for_dataset(dataset_name: str, session_ids: set[str], turns_file: str):
    """Extract turn-level data for selected sessions."""
    path = FILTERED / turns_file
    if not path.exists():
        print(f"  ⚠ Turns file not found: {turns_file}")
        return

    selected_turns = []
    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = reader.fieldnames
        for row in reader:
            if row.get("conversation_id") in session_ids:
                selected_turns.append(row)

    if selected_turns:
        write_csv(selected_turns, f"{dataset_name}_turns.csv", fieldnames)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    print("=" * 60)
    print("EVAL DATASET BUILDER")
    print(f"Input:  {FILTERED}")
    print(f"Output: {OUT}")
    print("=" * 60)

    # Build all datasets
    golden = build_golden()
    badcase = build_badcase()
    drift = build_drift()
    intake = build_intake()
    handover = build_handover()
    escalation = build_escalation()

    all_selected = {
        "golden": golden,
        "badcase": badcase,
        "drift_control": drift,
        "intake_tool_contract": intake,
        "handover": handover,
        "escalation": escalation,
    }

    review_queue = build_human_review_queue(all_selected)

    # Extract turns for each dataset
    print("\n═══ Extracting turns for selected sessions ═══")
    tier_turns_map = {
        "golden": "tier1_self_serve_turns.csv",
        "badcase": "tier4_frustration_turns.csv",
        "drift_control": "tier5_drift_turns.csv",
        "intake_tool_contract": "tier3_intake_turns.csv",
        "handover": "tier2_escalation_turns.csv",
        "escalation": "tier2_escalation_turns.csv",
    }

    for ds_name, rows in all_selected.items():
        ids = {r["Id"] for r in rows}
        extract_turns_for_dataset(ds_name, ids, tier_turns_map[ds_name])

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"  Golden Dataset:           {len(golden):>4} sessions  (target ≥ 150)")
    print(f"  Bad-case Bank:            {len(badcase):>4} sessions  (target ≥ 80)")
    print(f"  Drift/Control Dataset:    {len(drift):>4} sessions  (target ≥ 20)")
    print(f"  Intake/Tool Contract:     {len(intake):>4} sessions  (target ≥ 40)")
    print(f"  Handover Dataset:         {len(handover):>4} sessions  (target ≥ 60)")
    print(f"  Escalation Dataset:       {len(escalation):>4} sessions  (target ≥ 120)")
    print(f"  Human Review Queue:       {len(review_queue):>4} sessions  (quality ≥ 60)")
    print(f"\n  Output directory: {OUT}")


if __name__ == "__main__":
    main()
