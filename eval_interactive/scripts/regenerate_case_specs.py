"""Wave A3 regenerator: rebuild CaseSpec yamls from HR + turn data.

Driver wrapper around ``case_spec.extractor.extract_case_specs``. Run from
the repo root:

    python -m eval_interactive.scripts.regenerate_case_specs

Defaults to the canonical paths used by the rest of the eval pipeline.
Pass ``--clean`` to wipe the anchor / promotion / exploration directories
before regenerating (recommended -- otherwise stale yamls from a previous
regen with a different case_id distribution will linger).
"""

from __future__ import annotations

import argparse
import logging
import shutil
import sys
from pathlib import Path

# Make this script runnable as both ``python -m eval_interactive.scripts...``
# (when CWD is repo root with the outer eval_interactive treated as a
# namespace package) and as ``python path/to/regenerate_case_specs.py``.
_REPO_ROOT = Path(__file__).resolve().parents[2]
if str(_REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(_REPO_ROOT))

try:
    # Pattern that matches the linter CLI invocation in the Wave A2.2 brief:
    # ``python -m eval_interactive.eval_interactive.case_spec.linter``.
    from eval_interactive.eval_interactive.case_spec.extractor import (
        LLM_REVIEWER_DEFAULT_CACHE_DIR,
        dump_audit_to,
        extract_case_specs,
    )
    from eval_interactive.eval_interactive.case_spec.smoke_curator import (
        select_smoke_case_ids,
        write_smoke_corpus,
    )
except ModuleNotFoundError:
    # Fall back to the editable-install package layout where the inner
    # ``eval_interactive`` package is itself the import root.
    from eval_interactive.case_spec.extractor import (
        LLM_REVIEWER_DEFAULT_CACHE_DIR,
        dump_audit_to,
        extract_case_specs,
    )
    from eval_interactive.case_spec.smoke_curator import (
        select_smoke_case_ids,
        write_smoke_corpus,
    )


REPO_ROOT = _REPO_ROOT
DEFAULT_HR_CSV = REPO_ROOT / "data" / "human_review_annotations_2026-04-22_golden.csv"
DEFAULT_TURNS_DIR = REPO_ROOT / "data" / "eval_datasets"
DEFAULT_OUTPUT_DIR = REPO_ROOT / "eval_interactive" / "case_specs"
DEFAULT_AUDIT_PATH = REPO_ROOT / "qa-reports" / "case-spec-generation-audit.md"

REGEN_DIRS = ("anchor", "promotion", "exploration")
SMOKE_DIR_NAME = "smoke"
DEFAULT_SMOKE_TARGET_COUNT = 14


def _clean_regen_dirs(output_dir: Path) -> None:
    """Wipe yaml files from anchor/promotion/exploration before regen.

    The smoke/ directory is wiped+rewritten by the smoke curator step
    further down (see ``write_smoke_corpus``); we don't touch it here
    so that ``--skip-smoke`` runs leave the existing smoke set alone.
    We also keep .gitkeep files.
    """
    for sub in REGEN_DIRS:
        d = output_dir / sub
        if not d.exists():
            continue
        for path in d.iterdir():
            if path.is_file() and path.suffix in (".yaml", ".yml"):
                path.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--hr-csv", type=Path, default=DEFAULT_HR_CSV)
    parser.add_argument("--turns-dir", type=Path, default=DEFAULT_TURNS_DIR)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--audit-path", type=Path, default=DEFAULT_AUDIT_PATH)
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Wipe anchor/promotion/exploration yaml files before regenerating.",
    )
    parser.add_argument(
        "--skip-smoke",
        action="store_true",
        help=(
            "Skip the smoke-curator step. Useful when only the regenerated "
            "anchor/promotion/exploration buckets are needed (e.g. during "
            "extractor debugging)."
        ),
    )
    parser.add_argument(
        "--smoke-target-count",
        type=int,
        default=DEFAULT_SMOKE_TARGET_COUNT,
        help=(
            "Target number of specs in the smoke fixture (default 14). "
            "The curator may produce slightly fewer if there are not "
            "enough safe candidates per UC."
        ),
    )
    # ---- Wave A6 LLM persona reviewer flags ----
    parser.add_argument(
        "--no-llm",
        action="store_true",
        help=(
            "Skip the Wave A6 L2 persona reviewer entirely. Persona blocks "
            "fall back to rule_draft. Used by offline CI / unit tests."
        ),
    )
    parser.add_argument(
        "--refresh-llm-session",
        type=str,
        action="append",
        default=None,
        metavar="SESSION_ID",
        help=(
            "Force a re-call for a single source_session_id, overriding "
            "the cache hit. May be passed multiple times."
        ),
    )
    parser.add_argument(
        "--llm-model",
        type=str,
        default="deepseek-v4-pro",
        help="DeepSeek model name (default deepseek-v4-pro).",
    )
    parser.add_argument(
        "--llm-cache-dir",
        type=Path,
        default=LLM_REVIEWER_DEFAULT_CACHE_DIR,
        help=(
            "Directory where committed L2 cache files live "
            "(<source_session_id>.yaml per session)."
        ),
    )
    parser.add_argument(
        "--strict-overrides",
        action="store_true",
        help=(
            "Hard-fail extraction on any spec whose source_session_id "
            "matches a `status: pending_review` entry in "
            "case_spec_overrides.yaml."
        ),
    )
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
    )
    log = logging.getLogger("regenerate_case_specs")

    if args.clean:
        log.info("Cleaning %s subdirs before regen ...", args.output_dir)
        _clean_regen_dirs(args.output_dir)

    log.info("Running extractor ...")
    refresh_sessions = frozenset(args.refresh_llm_session or ())
    specs = extract_case_specs(
        hr_csv_path=args.hr_csv,
        turns_dir=args.turns_dir,
        output_dir=args.output_dir,
        llm_cache_dir=args.llm_cache_dir,
        llm_offline=bool(args.no_llm),
        llm_refresh_sessions=refresh_sessions,
        llm_model=args.llm_model,
        strict_overrides=bool(args.strict_overrides),
    )
    log.info("Extractor produced %d specs.", len(specs))

    audit_path = dump_audit_to(args.audit_path)
    log.info("Wrote audit to %s.", audit_path)

    # Per-set tally for the report.
    tally: dict[str, int] = {}
    for sub in REGEN_DIRS:
        d = args.output_dir / sub
        if d.exists():
            tally[sub] = sum(1 for p in d.iterdir() if p.suffix in (".yaml", ".yml"))
        else:
            tally[sub] = 0
    log.info("Per-set yaml counts: %s", tally)

    # Smoke curation (Wave A3.5). Run AFTER the buckets are written so
    # the curator sees the latest specs.
    if not args.skip_smoke:
        anchor_dir = args.output_dir / "anchor"
        promotion_dir = args.output_dir / "promotion"
        exploration_dir = args.output_dir / "exploration"
        smoke_dir = args.output_dir / SMOKE_DIR_NAME
        log.info("Curating smoke fixture (target_count=%d) ...", args.smoke_target_count)
        smoke_sources = select_smoke_case_ids(
            anchor_dir=anchor_dir,
            promotion_dir=promotion_dir,
            exploration_dir=exploration_dir,
            target_count=args.smoke_target_count,
        )
        # Tally each source by parent bucket for the summary line.
        per_bucket: dict[str, int] = {"anchor": 0, "promotion": 0, "exploration": 0}
        for src in smoke_sources:
            parent = src.parent.name
            if parent in per_bucket:
                per_bucket[parent] += 1
        written = write_smoke_corpus(smoke_sources, smoke_dir)
        log.info(
            "wrote %d smoke specs from buckets: anchor=%d promotion=%d exploration=%d",
            len(written),
            per_bucket["anchor"],
            per_bucket["promotion"],
            per_bucket["exploration"],
        )
        tally[SMOKE_DIR_NAME] = len(written)
    else:
        log.info("--skip-smoke set; smoke fixture not regenerated.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
