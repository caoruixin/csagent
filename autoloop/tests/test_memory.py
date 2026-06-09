"""Tests for the three-layer memory: experiments_log, iterations_index,
lessons_log.
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from autoloop.memory import (
    experiments_log,
    iterations_index,
    lessons_log,
)
from autoloop.memory.iterations_index import IterationRecord


# --- experiments_log -------------------------------------------------


def test_experiments_log_append_then_read(tmp_path: Path):
    log = tmp_path / "experiments.jsonl"
    experiments_log.append(log, {"iteration_id": "exp-1", "decision": "discard"})
    experiments_log.append(log, {"iteration_id": "exp-2", "decision": "keep"})
    records = experiments_log.read_all(log)
    assert len(records) == 2
    assert records[0]["iteration_id"] == "exp-1"
    assert records[1]["decision"] == "keep"


def test_experiments_log_read_recent(tmp_path: Path):
    log = tmp_path / "experiments.jsonl"
    for i in range(5):
        experiments_log.append(log, {"iteration_id": f"exp-{i}"})
    recent = experiments_log.read_recent(log, 3)
    assert [r["iteration_id"] for r in recent] == ["exp-2", "exp-3", "exp-4"]


def test_experiments_log_missing_file_returns_empty(tmp_path: Path):
    log = tmp_path / "does-not-exist.jsonl"
    assert experiments_log.read_all(log) == []
    assert experiments_log.read_recent(log, 5) == []


def test_experiments_log_tolerates_partial_trailing_line(tmp_path: Path):
    log = tmp_path / "experiments.jsonl"
    experiments_log.append(log, {"iteration_id": "exp-1"})
    # Simulate a crash mid-write by appending a truncated line.
    with log.open("a", encoding="utf-8") as f:
        f.write('{"iteration_id": "exp-2", "decisio')
    records = experiments_log.read_all(log)
    assert len(records) == 1
    assert records[0]["iteration_id"] == "exp-1"


def test_experiments_log_shadow_firewall_no_per_case_key(tmp_path: Path):
    """Sanity: an experiments_log row produced by the loop must not
    contain per-case shadow keys. Here we manually serialize a fake
    record matching the loop's shape and assert the JSON has no
    sentinel keys.
    """
    log = tmp_path / "experiments.jsonl"
    fake_record = {
        "iteration_id": "exp-7",
        "verdict": {
            "tier_breakdown": {
                "shadow_regression": {
                    "regression_detected": False,
                    "drop_pct": 1.2,
                    "baseline_pass_rate": 0.95,
                    "current_pass_rate": 0.94,
                }
            }
        },
        "decision": "keep",
    }
    experiments_log.append(log, fake_record)
    blob = log.read_text(encoding="utf-8")
    assert "per_case_failures" not in blob
    assert "case_id" not in blob


# --- iterations_index (sqlite) --------------------------------------


def test_iterations_index_init_idempotent(tmp_path: Path):
    db = tmp_path / "iter.sqlite"
    iterations_index.init_db(db)
    iterations_index.init_db(db)  # idempotent


def test_iterations_index_insert_and_recent(tmp_path: Path):
    db = tmp_path / "iter.sqlite"
    for i in range(3):
        rec = IterationRecord(
            id=f"exp-{i}",
            ts=f"2026-05-28T00:0{i}:00+00:00",
            target_skill=f"server/src/main/resources/skills/skill_{i}.yaml",
            target_field="$.procedure",
            edit_summary=f"edit {i}",
            hypothesis_fingerprint=f"fp-{i}",
            decision="keep" if i == 2 else "discard",
        )
        iterations_index.insert(db, rec)
    recent = iterations_index.query_recent(db, 2)
    assert len(recent) == 2
    assert recent[0].id == "exp-2"
    assert recent[0].decision == "keep"


def test_iterations_index_query_by_target(tmp_path: Path):
    db = tmp_path / "iter.sqlite"
    for i, field in enumerate(["$.procedure", "$.procedure", "$.escalation_policy"]):
        rec = IterationRecord(
            id=f"exp-{i}",
            ts=f"2026-05-28T00:0{i}:00+00:00",
            target_skill="server/src/main/resources/skills/skill_a.yaml",
            target_field=field,
            edit_summary="x",
            hypothesis_fingerprint=f"fp-{i}",
            decision="discard",
        )
        iterations_index.insert(db, rec)
    proc_rows = iterations_index.query_by_target(
        db, "server/src/main/resources/skills/skill_a.yaml", "$.procedure"
    )
    assert len(proc_rows) == 2


def test_iterations_index_query_by_fingerprint(tmp_path: Path):
    db = tmp_path / "iter.sqlite"
    rec = IterationRecord(
        id="exp-99",
        ts="2026-05-28T00:00:00+00:00",
        target_skill="server/src/main/resources/skills/skill_z.yaml",
        target_field="$.procedure",
        hypothesis_fingerprint="abc123",
        decision="discard",
    )
    iterations_index.insert(db, rec)
    rows = iterations_index.query_by_fingerprint(db, "abc123")
    assert len(rows) == 1
    assert rows[0].id == "exp-99"


def test_iterations_index_round_trip_fitness_delta(tmp_path: Path):
    """fitness_delta dict must round-trip through JSON serialization."""
    db = tmp_path / "iter.sqlite"
    delta = {"layer1": {"bad_cases": {"baseline": 5, "current": 6}}}
    rec = IterationRecord(
        id="exp-1",
        ts="2026-05-28T00:00:00+00:00",
        target_skill="x.yaml",
        target_field="$.procedure",
        hypothesis_fingerprint="fp",
        decision="keep",
        fitness_delta=delta,
    )
    iterations_index.insert(db, rec)
    recent = iterations_index.query_recent(db, 1)
    assert recent[0].fitness_delta == delta


# --- lessons_log -----------------------------------------------------


def test_lessons_log_read_missing_file(tmp_path: Path):
    p = tmp_path / "lessons.md"
    content = lessons_log.read_all(p)
    assert content.startswith("# Lessons")


def test_lessons_log_append_first_lesson(tmp_path: Path):
    p = tmp_path / "lessons.md"
    lesson = "## Lesson L-2026-05-28-001\n\nbody"
    lessons_log.append_lesson(p, lesson)
    text = p.read_text(encoding="utf-8")
    assert "## Lesson L-2026-05-28-001" in text
    assert text.startswith("# Lessons")
    assert lessons_log.count_lessons(p) == 1


def test_lessons_log_append_multiple_with_divider(tmp_path: Path):
    p = tmp_path / "lessons.md"
    lessons_log.append_lesson(p, "## Lesson L-2026-05-28-001\nfoo")
    lessons_log.append_lesson(p, "## Lesson L-2026-05-28-002\nbar")
    text = p.read_text(encoding="utf-8")
    assert "---" in text
    assert lessons_log.count_lessons(p) == 2
