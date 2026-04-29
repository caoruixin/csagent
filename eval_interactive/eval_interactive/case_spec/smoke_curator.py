"""Smoke set curator (Wave A3.5).

Builds the ``smoke/`` corpus as a deterministic SUBSET of the regenerated
``anchor/`` + ``promotion/`` + ``exploration/`` buckets so smoke and its
parent buckets always agree on every field. Smoke is a fast-feedback
fixture for CI: small (~12-16 specs), broad UC coverage, mixed
outcome_class, no risky drift.

Selection rationale
-------------------
* Strongly prefer Option (a) -- smoke = literal subset of A/P/E. The
  alternative (independent regeneration) would let smoke and its parents
  drift, costing us a single source of truth.
* Must include ``cs_interactive_015`` and ``cs_interactive_040`` -- both
  are UC-K cases that Wave C1 regression tests pin against.
* Cover at least one case per primary_uc that exists in the regenerated
  corpus (UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP, UC-G, UC-H, UC-I,
  UC-J, UC-K). UCs with zero cases in any bucket are dropped silently
  with an info-level log line; they are not required for fast feedback.
* Mix of outcome_classes: aim for ~1/3 resolve, ~2/3 escalate. The
  escalate-heavy ratio matches the post-A3 corpus distribution (intake
  UCs dominate the escalate side).
* Avoid hard_shift drift cases -- their judge dimensions are unstable
  and smoke is meant to be a stable signal.

Output ordering and selection are deterministic (sorted by case_id; the
first match per criterion wins). No randomness anywhere.

Public API
----------
* ``select_smoke_case_ids(anchor_dir, promotion_dir, exploration_dir,
  target_count=14)`` -- returns the absolute paths of the selected yaml
  files in the source buckets.
* ``write_smoke_corpus(case_files, smoke_dir)`` -- copies them as-is
  (byte-for-byte via ``shutil.copy2``) to ``smoke_dir``.
"""

from __future__ import annotations

import logging
import shutil
from pathlib import Path

from .loader import load_case_spec
from .schema import CaseSpec

logger = logging.getLogger(__name__)


# Wave C1 regression tests pin against these specific case_ids; smoke
# must always include them so the regression remains visible in the
# fast-feedback fixture.
_REQUIRED_CASE_IDS: tuple[str, ...] = (
    "cs_interactive_015",
    "cs_interactive_040",
)


# Target UCs for smoke coverage. UC-E and UC-F appear in anchor/promotion;
# UC-I appears in exploration (and possibly promotion). UCs not present
# in any bucket are silently skipped at runtime.
_TARGET_UCS: tuple[str, ...] = (
    "UC-A",
    "UC-B",
    "UC-C",
    "UC-D",
    "UC-E",
    "UC-F",
    "UC-FP",
    "UC-G",
    "UC-H",
    "UC-I",
    "UC-J",
    "UC-K",
)


# Drift behaviors we want to AVOID for smoke (judge dimensions are
# unstable). ``minor`` and ``soft_shift`` are OK; ``hard_shift`` is
# excluded.
_AVOIDED_DRIFTS: frozenset[str] = frozenset({"hard_shift"})


def _load_bucket(directory: Path) -> list[tuple[CaseSpec, Path]]:
    """Load every yaml in ``directory`` and return (spec, path) pairs."""

    out: list[tuple[CaseSpec, Path]] = []
    if not directory.exists():
        return out
    for yaml_path in sorted(directory.glob("*.yaml")):
        try:
            spec = load_case_spec(yaml_path)
        except Exception as exc:
            logger.warning(
                "smoke_curator: skipping %s -- failed to load (%s)",
                yaml_path,
                exc,
            )
            continue
        out.append((spec, yaml_path))
    return out


def _is_safe_for_smoke(spec: CaseSpec) -> bool:
    """Return True if the spec is acceptable for the smoke fixture."""

    drift = (spec.persona.drift_behavior or "").strip().lower()
    if drift in _AVOIDED_DRIFTS:
        return False
    return True


def _find_first_for_uc(
    candidates: list[tuple[CaseSpec, Path]],
    uc: str,
    already_selected: set[str],
) -> tuple[CaseSpec, Path] | None:
    """Pick the first safe candidate matching ``uc`` not yet selected.

    Sorted-by-case_id ordering already comes from ``_load_bucket``.
    """

    for spec, path in candidates:
        if spec.case_id in already_selected:
            continue
        if spec.expected.primary_uc != uc:
            continue
        if not _is_safe_for_smoke(spec):
            continue
        return spec, path
    return None


def _classify_outcomes(
    selected: list[tuple[CaseSpec, Path]],
) -> tuple[int, int]:
    """Return (resolve_count, escalate_count) for the selected list."""

    r = sum(1 for s, _ in selected if s.expected.outcome_class == "resolve")
    e = sum(1 for s, _ in selected if s.expected.outcome_class == "escalate")
    return r, e


def select_smoke_case_ids(
    anchor_dir: Path,
    promotion_dir: Path,
    exploration_dir: Path,
    target_count: int = 14,
) -> list[Path]:
    """Select ``target_count`` yaml file paths for the smoke fixture.

    The returned list is the source-of-truth paths in the parent buckets
    (NOT the smoke directory). The caller passes them to
    ``write_smoke_corpus`` to materialise the smoke directory.

    Selection algorithm:
      1. Always include the ``_REQUIRED_CASE_IDS`` (Wave C1 regression
         pins) if they exist in any bucket.
      2. For each UC in ``_TARGET_UCS`` not yet covered, pick the first
         safe candidate from the union of buckets (anchor preferred,
         then promotion, then exploration -- matches the corpus weight
         distribution).
      3. Top up to ``target_count`` with additional safe cases from
         anchor/promotion/exploration (in that order), preferring cases
         whose outcome_class is under-represented relative to a
         ~1/3-resolve / ~2/3-escalate target.

    Returns absolute paths sorted by case_id for stable downstream
    behaviour.
    """

    anchor = _load_bucket(Path(anchor_dir))
    promotion = _load_bucket(Path(promotion_dir))
    exploration = _load_bucket(Path(exploration_dir))

    # Path -> (spec, path) and case_id -> (spec, path)
    by_case_id: dict[str, tuple[CaseSpec, Path]] = {}
    for bucket in (anchor, promotion, exploration):
        for spec, path in bucket:
            # First-write wins so anchor wins over promotion wins over
            # exploration when the same case_id appears in multiple
            # buckets (in practice case_ids are unique per bucket).
            if spec.case_id not in by_case_id:
                by_case_id[spec.case_id] = (spec, path)

    selected: list[tuple[CaseSpec, Path]] = []
    selected_ids: set[str] = set()

    # (1) Required case_ids
    for cid in _REQUIRED_CASE_IDS:
        entry = by_case_id.get(cid)
        if entry is None:
            logger.warning(
                "smoke_curator: required case_id %r not found in any bucket",
                cid,
            )
            continue
        if not _is_safe_for_smoke(entry[0]):
            logger.warning(
                "smoke_curator: required case_id %r has unsafe drift "
                "(%s); including anyway as it is on the must-include list",
                cid,
                entry[0].persona.drift_behavior,
            )
        selected.append(entry)
        selected_ids.add(cid)

    # (2) UC coverage
    bucket_order = [anchor, promotion, exploration]
    for uc in _TARGET_UCS:
        # Skip if a required case already covers this UC.
        if any(s.expected.primary_uc == uc for s, _ in selected):
            continue
        for bucket in bucket_order:
            entry = _find_first_for_uc(bucket, uc, selected_ids)
            if entry is not None:
                selected.append(entry)
                selected_ids.add(entry[0].case_id)
                break
        else:
            logger.info(
                "smoke_curator: no safe case for UC %s in any bucket -- "
                "smoke set will under-represent this UC",
                uc,
            )

    # (3) Top up to target_count with outcome-balance preference.
    if len(selected) < target_count:
        # Build a single deterministic candidate stream: anchor, then
        # promotion, then exploration, each already sorted by case_id.
        stream: list[tuple[CaseSpec, Path]] = []
        for bucket in bucket_order:
            for entry in bucket:
                if entry[0].case_id in selected_ids:
                    continue
                if not _is_safe_for_smoke(entry[0]):
                    continue
                stream.append(entry)

        while len(selected) < target_count and stream:
            r, e = _classify_outcomes(selected)
            # Target ratio: roughly 1/3 resolve, 2/3 escalate.
            total_target = target_count
            want_resolve = total_target // 3  # e.g. 4 of 14
            prefer_resolve = r < want_resolve

            picked_idx = -1
            for i, entry in enumerate(stream):
                outcome = entry[0].expected.outcome_class
                if prefer_resolve and outcome == "resolve":
                    picked_idx = i
                    break
                if (not prefer_resolve) and outcome == "escalate":
                    picked_idx = i
                    break
            if picked_idx == -1:
                # No matching outcome left; just take the next one.
                picked_idx = 0

            entry = stream.pop(picked_idx)
            selected.append(entry)
            selected_ids.add(entry[0].case_id)

    selected.sort(key=lambda sp: sp[0].case_id)
    return [path for _, path in selected]


def write_smoke_corpus(case_files: list[Path], smoke_dir: Path) -> list[Path]:
    """Copy each yaml in ``case_files`` to ``smoke_dir`` byte-for-byte.

    Wipes any existing ``*.yaml`` / ``*.yml`` files in ``smoke_dir``
    first (preserves ``.gitkeep`` and any non-yaml siblings). Uses
    ``shutil.copy2`` so the copy preserves metadata and -- crucially --
    avoids any pyyaml round-trip that might reorder keys or lose
    trailing whitespace.

    Returns the list of newly-written paths inside ``smoke_dir``.
    """

    smoke_dir = Path(smoke_dir)
    smoke_dir.mkdir(parents=True, exist_ok=True)

    # Wipe pre-existing yaml files. Keep .gitkeep / non-yaml files.
    for path in smoke_dir.iterdir():
        if path.is_file() and path.suffix in (".yaml", ".yml"):
            path.unlink()

    written: list[Path] = []
    for src in case_files:
        dst = smoke_dir / src.name
        shutil.copy2(src, dst)
        written.append(dst)
    return written
