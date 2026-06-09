"""Append-only JSONL log of every loop iteration.

One row per iteration, regardless of outcome (`keep`, `discard`,
`error`). The shadow firewall RESPECTED — per-case shadow info is
never serialized here (Layer 4 metrics carry only aggregate keys, as
produced by `tier_evaluator.evaluate(...)` default API surface).

File layout (one JSON object per line):

    {"iteration_id": "exp-1",
     "timestamp": "2026-05-28T01:23:45+00:00",
     "hypothesis": {...},
     "sandbox_verdict": {...},
     "anti_hardcode_verdict": {...},
     "applied": {...} | null,
     "verdict": {...} | null,
     "decision": "keep" | "discard" | "error",
     "discard_reason": str | null,
     "error": str | null,
     "elapsed_seconds": float}

Writes use append-mode + fsync so that a crash mid-run does not lose
the prior iteration's record. Reads tolerate trailing partial lines
(a write that was interrupted) by skipping them.
"""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any, Iterable


def append(log_path: Path, record: dict[str, Any]) -> None:
    """Append one record as a JSON line + fsync.

    The parent directory is created if it does not exist. The record
    is serialized with ``default=str`` so that Path objects and
    datetimes flow through without raising; callers should still
    normalize values before passing them in.
    """
    log_path = Path(log_path)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    line = json.dumps(record, default=str, ensure_ascii=False)
    with log_path.open("a", encoding="utf-8") as f:
        f.write(line)
        f.write("\n")
        f.flush()
        os.fsync(f.fileno())


def read_all(log_path: Path) -> list[dict[str, Any]]:
    """Read every well-formed JSON line in the log.

    Returns an empty list when the file does not exist (a brand-new
    autoloop install). Partial / malformed trailing lines are
    skipped rather than raising — a crashed mid-write is recoverable.
    """
    log_path = Path(log_path)
    if not log_path.exists():
        return []
    out: list[dict[str, Any]] = []
    with log_path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                out.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return out


def read_recent(log_path: Path, n: int) -> list[dict[str, Any]]:
    """Return the last `n` records (chronological order preserved).

    Convenience for the proposer's `recent_iterations_for_propose`
    window. Reads the entire file (cheap for v1 small-N workloads).
    """
    if n <= 0:
        return []
    all_records = read_all(log_path)
    return all_records[-n:]


def iter_records(log_path: Path) -> Iterable[dict[str, Any]]:
    """Streaming variant of `read_all`, for future large-N cases."""
    log_path = Path(log_path)
    if not log_path.exists():
        return iter(())

    def _gen() -> Iterable[dict[str, Any]]:
        with log_path.open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                try:
                    yield json.loads(line)
                except json.JSONDecodeError:
                    continue

    return _gen()
