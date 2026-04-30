"""Wave A6.4 concurrent LLM-cache populator.

Hits DeepSeek concurrently to fill the production
``eval_interactive/case_spec_llm_cache/`` directory for every unique
``source_session_id`` in the HR CSV. Existing valid cache files are
respected (cache-hits, no API call).

Why this exists: ``regenerate_case_specs.py`` calls the L2 reviewer
sequentially -- 367 sessions × ~80 s per call ≈ 8 hours. This script runs
the same canonical reviewer with concurrency=5 (asyncio + a semaphore +
``asyncio.to_thread`` since ``LlmPersonaReviewer.review`` is sync).
Wall-clock for a clean run: ~10 min.

After this script completes, run ``regenerate_case_specs.py --clean`` --
all sessions will be cache-hits and the regen finishes in 1-3 min.

Usage::

    python -m eval_interactive.scripts.populate_llm_cache
    python -m eval_interactive.scripts.populate_llm_cache --limit 5
    python -m eval_interactive.scripts.populate_llm_cache --concurrency 8
"""

from __future__ import annotations

import argparse
import asyncio
import csv
import logging
import sys
import time
from collections import Counter
from pathlib import Path

_REPO_ROOT = Path(__file__).resolve().parents[2]
if str(_REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(_REPO_ROOT))

from eval_interactive.case_spec.extractor import (  # noqa: E402
    LLM_REVIEWER_DEFAULT_CACHE_DIR,
    SOURCE_DATASET_TURNS_FILE,
    _load_case_spec_overrides,
    _load_source_turn_indexes,
    build_rule_persona,
)
from eval_interactive.case_spec.llm_persona_reviewer import (  # noqa: E402
    DEFAULT_DEEPSEEK_MODEL,
    LlmPersonaReviewer,
    ReviewResult,
)

LOG = logging.getLogger("populate_llm_cache")

DEFAULT_HR_CSV = (
    _REPO_ROOT / "data" / "human_review_annotations_2026-04-22_golden.csv"
)
DEFAULT_TURNS_DIR = _REPO_ROOT / "data" / "eval_datasets"
DEFAULT_CONCURRENCY = 5
HARD_CALL_BUDGET = 500


def _parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--hr-csv", type=Path, default=DEFAULT_HR_CSV)
    p.add_argument("--turns-dir", type=Path, default=DEFAULT_TURNS_DIR)
    p.add_argument("--cache-dir", type=Path, default=LLM_REVIEWER_DEFAULT_CACHE_DIR)
    p.add_argument("--model", default=DEFAULT_DEEPSEEK_MODEL)
    p.add_argument("--limit", type=int, default=0,
                   help="Process at most N unique sessions (smoke).")
    p.add_argument("--concurrency", type=int, default=DEFAULT_CONCURRENCY)
    p.add_argument("--dry-run", action="store_true",
                   help="Skip API calls; report cache-hit rate only.")
    p.add_argument("--verbose", action="store_true")
    return p.parse_args()


def _read_hr_rows(hr_csv: Path) -> list[dict[str, str]]:
    with open(hr_csv, "r", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def _dedupe_by_session(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    seen: set[str] = set()
    out: list[dict[str, str]] = []
    for row in rows:
        sid = (row.get("session_id") or "").strip()
        if not sid or sid in seen:
            continue
        seen.add(sid)
        out.append(row)
    return out


async def _populate_one(
    *,
    semaphore: asyncio.Semaphore,
    reviewer: LlmPersonaReviewer,
    work_index: int,
    total: int,
    row: dict[str, str],
    turns: list[dict],
    turns_file: str,
    applied_overrides: dict,
    case_id_hint: str,
) -> tuple[str, ReviewResult | None, str]:
    session_id = row["session_id"].strip()
    override = applied_overrides.get(session_id)
    rule_draft, hr_context, transcript_turns = build_rule_persona(
        row, turns, turns_file=turns_file, applied_override=override,
    )
    async with semaphore:
        try:
            result = await asyncio.to_thread(
                reviewer.review,
                source_session_id=session_id,
                rule_draft=rule_draft,
                transcript_turns=transcript_turns,
                hr_context=hr_context,
                case_id_hint=case_id_hint,
            )
        except Exception as e:  # noqa: BLE001 - keep one bad call from killing the run
            print(
                f"[{work_index}/{total}] {session_id} ERROR {type(e).__name__}: {e}",
                flush=True,
            )
            return session_id, None, "error"

    tag = "hit" if result.cache_hit else (
        "offline" if result.used_offline_fallback else "miss"
    )
    confidence = ""
    if result.cache_record:
        confidence = result.cache_record.get("llm_confidence", "")
    print(
        f"[{work_index}/{total}] {session_id} {tag} confidence={confidence}",
        flush=True,
    )
    return session_id, result, tag


async def _run(args: argparse.Namespace) -> int:
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
    )

    hr_csv = args.hr_csv
    turns_dir = args.turns_dir
    cache_dir = args.cache_dir
    cache_dir.mkdir(parents=True, exist_ok=True)

    LOG.info("HR CSV: %s", hr_csv)
    LOG.info("Turns dir: %s", turns_dir)
    LOG.info("Cache dir: %s", cache_dir)
    LOG.info("Model: %s | concurrency: %d", args.model, args.concurrency)

    if not hr_csv.exists():
        LOG.error("HR CSV not found: %s", hr_csv)
        return 2

    rows = _dedupe_by_session(_read_hr_rows(hr_csv))
    LOG.info("Unique sessions in HR CSV: %d", len(rows))

    turn_indexes = _load_source_turn_indexes(turns_dir)
    overrides_registry = _load_case_spec_overrides()
    applied = {
        sid: entry for sid, entry in overrides_registry.applied.items()
    } if hasattr(overrides_registry, "applied") else dict(overrides_registry or {})
    LOG.info("Applied overrides loaded: %d", len(applied))

    work: list[tuple[int, dict[str, str], list[dict], str]] = []
    skipped_no_turns = 0
    skipped_unknown_dataset = 0
    for row in rows:
        session_id = row["session_id"].strip()
        source_dataset = (row.get("source_dataset") or "").strip()
        if source_dataset not in SOURCE_DATASET_TURNS_FILE:
            skipped_unknown_dataset += 1
            continue
        turns_filename = SOURCE_DATASET_TURNS_FILE[source_dataset]
        turns = turn_indexes.get(source_dataset, {}).get(session_id)
        if not turns:
            skipped_no_turns += 1
            continue
        work.append((len(work) + 1, row, turns, turns_filename))

    LOG.info(
        "Work items: %d (skipped: no_turns=%d unknown_dataset=%d)",
        len(work), skipped_no_turns, skipped_unknown_dataset,
    )

    if args.limit > 0:
        work = work[: args.limit]
        LOG.info("--limit %d applied; %d items remain.", args.limit, len(work))

    if len(work) > HARD_CALL_BUDGET:
        LOG.error("Refusing to dispatch %d > %d call budget.", len(work), HARD_CALL_BUDGET)
        return 3

    if args.dry_run:
        LOG.info("--dry-run: not calling DeepSeek.")
        return 0

    reviewer = LlmPersonaReviewer(
        model=args.model,
        cache_dir=cache_dir,
        offline=False,
    )

    semaphore = asyncio.Semaphore(args.concurrency)
    started = time.monotonic()
    results = await asyncio.gather(
        *[
            _populate_one(
                semaphore=semaphore,
                reviewer=reviewer,
                work_index=idx,
                total=len(work),
                row=row,
                turns=turns,
                turns_file=turns_filename,
                applied_overrides=applied,
                case_id_hint=f"row_{idx}",
            )
            for (idx, row, turns, turns_filename) in work
        ],
        return_exceptions=False,
    )
    elapsed = time.monotonic() - started

    tag_counts = Counter(tag for _sid, _r, tag in results)
    confidence_counts = Counter()
    acceptance_counts = Counter()
    for _sid, r, tag in results:
        if r and r.cache_record:
            confidence_counts[r.cache_record.get("llm_confidence", "")] += 1
            acceptance_counts[r.cache_record.get("acceptance_reason", "")] += 1

    LOG.info("Done in %.1fs.", elapsed)
    LOG.info("Tag counts: %s", dict(tag_counts))
    LOG.info("Confidence: %s", dict(confidence_counts))
    LOG.info("Acceptance: %s", dict(acceptance_counts))
    return 0 if tag_counts.get("error", 0) == 0 else 1


def main() -> int:
    args = _parse_args()
    return asyncio.run(_run(args))


if __name__ == "__main__":
    sys.exit(main())
