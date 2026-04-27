"""Case set management -- loads named sets of CaseSpecs.

Manages the three canonical case-spec directories (anchor, promotion,
exploration) and supports loading custom paths.
"""

from __future__ import annotations

import logging
from pathlib import Path

from eval_interactive.case_spec.loader import load_case_spec, load_case_specs
from eval_interactive.case_spec.schema import CaseSpec

logger = logging.getLogger(__name__)

_KNOWN_SETS = ("anchor", "promotion", "exploration", "smoke")


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
        """Load a named set: 'anchor', 'promotion', 'exploration', or 'all'.

        'all' loads all three sets combined.

        Args:
            set_name: One of 'anchor', 'promotion', 'exploration', or 'all'.

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

        if set_name not in _KNOWN_SETS:
            raise ValueError(
                f"Unknown set name '{set_name}'. "
                f"Valid names: {', '.join(_KNOWN_SETS)}, all"
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
        for name in _KNOWN_SETS:
            sub_dir = self._base_dir / name
            if sub_dir.is_dir():
                yaml_count = len(list(sub_dir.glob("*.yaml"))) + len(
                    list(sub_dir.glob("*.yml"))
                )
                result[name] = yaml_count
            else:
                result[name] = 0
        return result
