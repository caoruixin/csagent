"""Case set management -- loads named sets of CaseSpecs.

Manages the canonical case-spec directories and supports loading custom
paths. Sets are split into two registries: `_KNOWN_SETS` participate in
`--set all` iteration; `_OPT_IN_SETS` are opt-in human-judgment suites
(per `docs/current/iteration_governance.md` §5.6) that must be selected
explicitly to avoid treating their programmatic PASS/FAIL as hard gates.
"""

from __future__ import annotations

import logging
from pathlib import Path

from eval_interactive.case_spec.loader import load_case_spec, load_case_specs
from eval_interactive.case_spec.schema import CaseSpec

logger = logging.getLogger(__name__)

_KNOWN_SETS = ("anchor", "promotion", "exploration", "smoke")
# Opt-in human-judgment suites excluded from `--set all` per
# `iteration_governance.md` §5.6 (programmatic PASS/FAIL is NOT a hard gate
# for these suites; manual review is the acceptance gate).
_OPT_IN_SETS = ("bad_cases", "anchor_outcome")
_ALL_SETS = _KNOWN_SETS + _OPT_IN_SETS


def is_human_judgment_suite(suite_name: str | None) -> bool:
    """Return True iff ``suite_name`` is an opt-in human-judgment suite.

    Per ``iteration_governance.md`` §5.6, the bad-case and
    anchor-outcome suites are evaluated by manual human review of the
    per-case ``closure_criterion`` against ``per_turn_trace``;
    programmatic ``case_passed`` PASS/FAIL is observation-only. This
    helper centralises the suite-name check so the executor and report
    code do not duplicate the membership test.

    Args:
        suite_name: A suite directory name (e.g., "bad_cases",
            "anchor_outcome", "anchor"), or ``None`` when the loader
            could not determine the suite (e.g., a CaseSpec
            instantiated directly in unit tests, or loaded from a
            non-set path that does not match any registered suite).

    Returns:
        True when ``suite_name`` matches an entry in ``_OPT_IN_SETS``;
        False otherwise (including for ``None``).
    """
    if suite_name is None:
        return False
    return suite_name in _OPT_IN_SETS


class CaseSetManager:
    """Manages case spec sets (anchor/promotion/exploration)."""

    def __init__(self, base_dir: str = "case_specs"):
        """Initialize with a base directory containing set subdirectories.

        Args:
            base_dir: Root directory that contains anchor/, promotion/,
                      and exploration/ subdirectories.
        """
        self._base_dir = Path(base_dir)

    def load_set(self, set_name: str) -> list[CaseSpec]:
        """Load a named set.

        Valid set names: anchor, promotion, exploration, smoke (participate
        in 'all'); bad_cases, anchor_outcome (opt-in human-judgment suites,
        explicit selection only — excluded from 'all' per
        `iteration_governance.md` §5.6); 'all' (combines `_KNOWN_SETS` only).

        Args:
            set_name: A registered set name or 'all'.

        Returns:
            List of CaseSpec instances, sorted by case_id.

        Raises:
            ValueError: If set_name is not recognised.
            FileNotFoundError: If the set directory does not exist.
        """
        set_name = set_name.strip().lower()

        if set_name == "all":
            specs: list[CaseSpec] = []
            for name in _KNOWN_SETS:
                sub_dir = self._base_dir / name
                if sub_dir.is_dir():
                    specs.extend(load_case_specs(sub_dir))
                else:
                    logger.warning("Set directory not found, skipping: %s", sub_dir)
            specs.sort(key=lambda s: s.case_id)
            return specs

        if set_name not in _ALL_SETS:
            raise ValueError(
                f"Unknown set name '{set_name}'. "
                f"Valid names: {', '.join(_KNOWN_SETS)}, "
                f"{', '.join(_OPT_IN_SETS)} (opt-in), all"
            )

        sub_dir = self._base_dir / set_name
        if not sub_dir.exists():
            raise FileNotFoundError(f"Case set directory not found: {sub_dir}")

        return load_case_specs(sub_dir)

    def load_custom(self, path: str) -> list[CaseSpec]:
        """Load CaseSpecs from a custom path (file or directory).

        Args:
            path: Path to a single YAML file or a directory of YAML files.

        Returns:
            List of CaseSpec instances, sorted by case_id.

        Raises:
            FileNotFoundError: If the path does not exist.
        """
        p = Path(path)
        if not p.exists():
            raise FileNotFoundError(f"Custom path not found: {p}")

        if p.is_file():
            return [load_case_spec(p)]

        return load_case_specs(p)

    def list_sets(self) -> dict[str, int]:
        """Return {set_name: count} for each available set.

        Scans each known subdirectory and counts YAML files.

        Returns:
            Dict mapping set name to number of case specs found.
        """
        result: dict[str, int] = {}
        for name in _ALL_SETS:
            sub_dir = self._base_dir / name
            if sub_dir.is_dir():
                yaml_count = len(list(sub_dir.glob("*.yaml"))) + len(
                    list(sub_dir.glob("*.yml"))
                )
                result[name] = yaml_count
            else:
                result[name] = 0
        return result
