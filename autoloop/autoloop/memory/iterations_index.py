"""sqlite-backed index for fast iteration lookups.

Stdlib `sqlite3` only — no new dependency. The DB is one table with
indexes on the columns the proposer / audit consult most often:
fingerprint (anti-repeat), target_skill (per-Skill failure pattern),
decision (kept vs discarded ratio over a window), and ts
(recent-K queries for the proposer's context window).

Schema is created idempotently on first call to `init_db`. Existing
deployments survive new rows being added because the layer C
(lessons_log) and Layer A (experiments_log) carry the same data and
can rebuild the sqlite index on demand.
"""

from __future__ import annotations

import json
import sqlite3
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any


_SCHEMA = """
CREATE TABLE IF NOT EXISTS iterations(
    id TEXT PRIMARY KEY,
    ts TEXT NOT NULL,
    target_skill TEXT NOT NULL,
    target_field TEXT NOT NULL,
    edit_summary TEXT,
    hypothesis_fingerprint TEXT NOT NULL,
    decision TEXT NOT NULL,
    discard_reason TEXT,
    fitness_delta_json TEXT,
    parent_iteration_id TEXT,
    notes TEXT
);
CREATE INDEX IF NOT EXISTS idx_target_skill ON iterations(target_skill);
CREATE INDEX IF NOT EXISTS idx_fingerprint ON iterations(hypothesis_fingerprint);
CREATE INDEX IF NOT EXISTS idx_decision ON iterations(decision);
CREATE INDEX IF NOT EXISTS idx_ts ON iterations(ts);
"""


@dataclass
class IterationRecord:
    """Row shape for the iterations table.

    `fitness_delta` is stored as a JSON blob — the per-tier delta
    summary the verdict produces. `parent_iteration_id` references
    the iteration this one was a follow-up to (for the proposer's
    anti-repeat reasoning).
    """

    id: str
    ts: str
    target_skill: str
    target_field: str
    edit_summary: str | None = None
    hypothesis_fingerprint: str = ""
    decision: str = "discard"
    discard_reason: str | None = None
    fitness_delta: dict[str, Any] = field(default_factory=dict)
    parent_iteration_id: str | None = None
    notes: str | None = None


def init_db(db_path: Path) -> None:
    """Create the iterations table + indexes if absent.

    Idempotent. Callers may invoke this on every loop start without
    worrying about double-creation; sqlite's IF NOT EXISTS handles it.
    """
    db_path = Path(db_path)
    db_path.parent.mkdir(parents=True, exist_ok=True)
    with sqlite3.connect(str(db_path)) as conn:
        conn.executescript(_SCHEMA)
        conn.commit()


def insert(db_path: Path, record: IterationRecord) -> None:
    """Insert one row. Raises sqlite3.IntegrityError on duplicate id.

    The auto-loop never produces duplicate ids in normal operation;
    a duplicate would indicate the orchestrator restarted on an old
    iteration_id (the human should rename it).
    """
    init_db(db_path)
    fitness_blob = json.dumps(record.fitness_delta, default=str)
    with sqlite3.connect(str(db_path)) as conn:
        conn.execute(
            """
            INSERT INTO iterations(
                id, ts, target_skill, target_field, edit_summary,
                hypothesis_fingerprint, decision, discard_reason,
                fitness_delta_json, parent_iteration_id, notes
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                record.id,
                record.ts,
                record.target_skill,
                record.target_field,
                record.edit_summary,
                record.hypothesis_fingerprint,
                record.decision,
                record.discard_reason,
                fitness_blob,
                record.parent_iteration_id,
                record.notes,
            ),
        )
        conn.commit()


def query_recent(db_path: Path, n: int) -> list[IterationRecord]:
    """Return the most recent N iterations ordered by ts DESC.

    Used by the proposer to build the "recent iterations summary"
    context block — see proposer.propose's `recent_iterations` input.
    """
    init_db(db_path)
    with sqlite3.connect(str(db_path)) as conn:
        conn.row_factory = sqlite3.Row
        rows = conn.execute(
            "SELECT * FROM iterations ORDER BY ts DESC LIMIT ?",
            (n,),
        ).fetchall()
    return [_row_to_record(r) for r in rows]


def query_by_target(
    db_path: Path,
    target_skill: str,
    target_field: str | None = None,
) -> list[IterationRecord]:
    """All iterations against a given skill (+ optional field).

    Used by analyzer to surface per-skill discard rates and by
    proposer to avoid same-target repetition when discards pile up.
    """
    init_db(db_path)
    with sqlite3.connect(str(db_path)) as conn:
        conn.row_factory = sqlite3.Row
        if target_field is None:
            rows = conn.execute(
                "SELECT * FROM iterations WHERE target_skill = ? ORDER BY ts DESC",
                (target_skill,),
            ).fetchall()
        else:
            rows = conn.execute(
                "SELECT * FROM iterations WHERE target_skill = ? "
                "AND target_field = ? ORDER BY ts DESC",
                (target_skill, target_field),
            ).fetchall()
    return [_row_to_record(r) for r in rows]


def query_by_fingerprint(
    db_path: Path, fingerprint: str
) -> list[IterationRecord]:
    """Hypothesis-fingerprint match — exact-repeat detection.

    The proposer hashes (target_skill, target_field, after_value) into
    a fingerprint; if a future propose collides, that's an exact
    repeat which the proposer should reject before applying.
    """
    init_db(db_path)
    with sqlite3.connect(str(db_path)) as conn:
        conn.row_factory = sqlite3.Row
        rows = conn.execute(
            "SELECT * FROM iterations WHERE hypothesis_fingerprint = ? "
            "ORDER BY ts DESC",
            (fingerprint,),
        ).fetchall()
    return [_row_to_record(r) for r in rows]


def _row_to_record(row: sqlite3.Row) -> IterationRecord:
    fitness_delta: dict[str, Any] = {}
    if row["fitness_delta_json"]:
        try:
            fitness_delta = json.loads(row["fitness_delta_json"])
        except json.JSONDecodeError:
            fitness_delta = {}
    return IterationRecord(
        id=row["id"],
        ts=row["ts"],
        target_skill=row["target_skill"],
        target_field=row["target_field"],
        edit_summary=row["edit_summary"],
        hypothesis_fingerprint=row["hypothesis_fingerprint"],
        decision=row["decision"],
        discard_reason=row["discard_reason"],
        fitness_delta=fitness_delta,
        parent_iteration_id=row["parent_iteration_id"],
        notes=row["notes"],
    )


def record_to_dict(record: IterationRecord) -> dict[str, Any]:
    """Convenience for serializing a record into the proposer prompt."""
    return asdict(record)
