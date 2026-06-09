"""YAML-diff sandbox for Skill YAML mutations proposed by the auto-loop
meta-agent.

This module is the structural enforcement of the M-Auto-1A mutable
surface contract in `autoloop/program.md` §2. Every proposed diff
flows through `validate_skill_yaml_diff` BEFORE it reaches the applier;
the validator answers ACCEPT or REJECT with a human-readable reason
plus the exact AST path(s) that triggered the decision.

The validator is intentionally narrow:
- It does not interpret YAML semantics beyond "what changed at the
  AST level."
- It does not understand whether a `procedure` edit is semantically
  good — that judgment is the anti-hardcode auto-check (S-Auto-4) +
  the four-tier evaluator (S-Auto-2) + Codex review.
- It does not handle multi-file diffs. Callers (the S-Auto-3 loop
  orchestrator) MUST invoke `validate_skill_yaml_diff` per file
  exactly once; bundle-level cross-file rejection is the caller's
  responsibility per the §3 fence #9 contract documented in
  `applier.py`.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Literal

import yaml


# --- Config loading --------------------------------------------------

_CONFIG_PATH = Path(__file__).resolve().parents[2] / "config.yaml"


def _load_config() -> dict[str, Any]:
    with _CONFIG_PATH.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)


# --- Public API -------------------------------------------------------


@dataclass
class ValidationResult:
    """The verdict of `validate_skill_yaml_diff`.

    - `decision` is "ACCEPT" or "REJECT".
    - `reason` is one line of human-readable explanation.
    - `rejected_paths` lists AST paths that violated the whitelist
      (empty when `decision == "ACCEPT"`).
    - `accepted_paths` lists AST paths that were modified and pass
      the whitelist (populated only when `decision == "ACCEPT"`).
    - `file_path` is echoed back from the caller for audit logs.
    """

    decision: Literal["ACCEPT", "REJECT"]
    reason: str
    rejected_paths: list[str] = field(default_factory=list)
    accepted_paths: list[str] = field(default_factory=list)
    file_path: str = ""


def validate_skill_yaml_diff(
    before_yaml: str,
    after_yaml: str,
    file_path: str,
    *,
    allowed_skill_files: list[str] | None = None,
    allowed_field_paths: list[str] | None = None,
) -> ValidationResult:
    """Validate a single-file Skill YAML diff against the mutable
    surface whitelist.

    Algorithm (matches `autoloop/program.md` §2 and the §3 fence #9
    contract):

    1. File-path check — `file_path` must be in
       `allowed_skill_files`. Otherwise REJECT.
    2. Anchor / alias check — neither `before_yaml` nor `after_yaml`
       may use YAML anchors or aliases. Production Skill YAMLs are
       anchor-free (verified at S-Auto-1 design); an anchor in
       either side is treated as a structural change the validator
       refuses to reason about.
    3. YAML parse — both sides via `yaml.safe_load`. After-side
       parse failure → REJECT (malformed proposal). Before-side
       parse failure → REJECT (baseline malformed; evaluator-side
       error, not meta-agent fault).
    4. AST diff — compute the set of AST node paths that differ
       between before and after. Path notation: `$.procedure`,
       `$.critical_steps[0].desc`, `$.applicable_use_cases[2]`.
       Reorderings, additions, and deletions are all reported as
       node-path changes.
    5. Whitelist match — each changed path must match at least one
       of `allowed_field_paths` (with `[*]` matching any list
       index). A path that matches no pattern is rejected.
    6. Empty diff — if zero paths changed after parsing (e.g.
       comment-only or whitespace-only edit), REJECT with the
       "no whitelisted field changed" reason; the loop must
       produce a real proposal or fail-forward.
    """
    if allowed_skill_files is None or allowed_field_paths is None:
        cfg = _load_config()
        if allowed_skill_files is None:
            allowed_skill_files = cfg["mutable_surface"]["allowed_skill_files"]
        if allowed_field_paths is None:
            allowed_field_paths = cfg["mutable_surface"]["allowed_field_paths"]

    normalized_path = _normalize_file_path(file_path)
    if normalized_path not in set(allowed_skill_files):
        return ValidationResult(
            decision="REJECT",
            reason=f"file outside mutable surface: {file_path}",
            file_path=file_path,
        )

    if _has_yaml_anchor_or_alias(after_yaml):
        return ValidationResult(
            decision="REJECT",
            reason="proposed YAML uses anchors or aliases; structurally rejected",
            file_path=file_path,
        )
    if _has_yaml_anchor_or_alias(before_yaml):
        return ValidationResult(
            decision="REJECT",
            reason="baseline YAML uses anchors or aliases; refusing to compare",
            file_path=file_path,
        )

    try:
        after_parsed = yaml.safe_load(after_yaml)
    except yaml.YAMLError as exc:
        return ValidationResult(
            decision="REJECT",
            reason=f"malformed YAML in proposed edit: {exc.__class__.__name__}",
            file_path=file_path,
        )
    try:
        before_parsed = yaml.safe_load(before_yaml)
    except yaml.YAMLError as exc:
        return ValidationResult(
            decision="REJECT",
            reason=f"baseline YAML is malformed — refusing to compare: {exc.__class__.__name__}",
            file_path=file_path,
        )

    changed_paths = _diff_paths(before_parsed, after_parsed, prefix="$")

    if not changed_paths:
        return ValidationResult(
            decision="REJECT",
            reason="no whitelisted field changed (empty AST diff — comment/whitespace-only or identical)",
            file_path=file_path,
        )

    rejected = [
        p for p in changed_paths
        if not _path_matches_any(p, allowed_field_paths)
    ]
    if rejected:
        return ValidationResult(
            decision="REJECT",
            reason=f"modification to non-mutable field(s): {', '.join(rejected)}",
            rejected_paths=rejected,
            file_path=file_path,
        )

    return ValidationResult(
        decision="ACCEPT",
        reason=f"whitelisted edit to {len(changed_paths)} path(s)",
        accepted_paths=changed_paths,
        file_path=file_path,
    )


# --- Internals --------------------------------------------------------


def _normalize_file_path(file_path: str) -> str:
    """Strip a single leading './' so callers can pass either form.

    The validator does not resolve absolute paths or follow symlinks;
    membership in `allowed_skill_files` is a literal-string match
    on the repo-relative path documented in `config.yaml`.
    """
    if file_path.startswith("./"):
        return file_path[2:]
    return file_path


def _has_yaml_anchor_or_alias(text: str) -> bool:
    """Return True if the YAML text uses any anchor (`&name`) or alias
    (`*name`).

    Implemented via the PyYAML event stream so we do not regex
    natural-language content inside scalar values (where `&` or `*`
    may legitimately appear, e.g. "AT&T").

    A document that itself fails to parse cannot have anchors
    detected here; the parse-failure path is handled separately by
    the caller via `yaml.safe_load`.
    """
    try:
        for event in yaml.parse(text):
            if isinstance(event, yaml.AliasEvent):
                return True
            anchor = getattr(event, "anchor", None)
            if anchor:
                return True
    except yaml.YAMLError:
        return False
    return False


def _diff_paths(before: Any, after: Any, prefix: str) -> list[str]:
    """Recursively diff two YAML-parsed structures and return AST
    paths that differ.

    Path conventions:
      - Root is `$`.
      - Dict keys append `.key`.
      - List indices append `[i]`.

    Type mismatch at the same path counts as a single change at that
    path. List length changes report every index that exists in one
    side but not the other. Reorderings of equal-length lists report
    per-index scalar / sub-tree differences (not a separate "reorder"
    marker — the per-index diffs are sufficient evidence of structural
    change to fail the whitelist).
    """
    if type(before) is not type(after):
        return [prefix]
    if isinstance(before, dict):
        changed: list[str] = []
        all_keys = set(before.keys()) | set(after.keys())
        for k in sorted(all_keys, key=str):
            path = f"{prefix}.{k}"
            if k not in before or k not in after:
                changed.append(path)
            else:
                changed.extend(_diff_paths(before[k], after[k], path))
        return changed
    if isinstance(before, list):
        changed = []
        n_before = len(before)
        n_after = len(after)
        n_max = max(n_before, n_after)
        for i in range(n_max):
            path = f"{prefix}[{i}]"
            if i >= n_before or i >= n_after:
                changed.append(path)
            else:
                changed.extend(_diff_paths(before[i], after[i], path))
        return changed
    # Scalars: equality compare. None == None is fine.
    if before != after:
        return [prefix]
    return []


def _pattern_to_regex(pattern: str) -> re.Pattern[str]:
    """Compile a whitelist pattern (e.g. `$.critical_steps[*].desc`)
    into a regex. `[*]` matches any non-negative integer list index;
    every other character is matched literally.
    """
    parts = pattern.split("[*]")
    regex = r"\[\d+\]".join(re.escape(p) for p in parts)
    return re.compile(f"^{regex}$")


def _path_matches_any(path: str, patterns: list[str]) -> bool:
    for pat in patterns:
        if _pattern_to_regex(pat).match(path):
            return True
    return False
