"""autoloop CLI — subcommand router.

S-Auto-1 implements only `check` (config validation + sandbox
self-test). All other subcommands print "S-Auto-3 territory" and
exit 1, but their `--help` still works so that `python -m autoloop
<name> --help` returns clean help at exit 0.

Entry points:
    autoloop <subcommand>          (via pyproject.toml [project.scripts])
    python -m autoloop <subcommand>
"""

from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path
from typing import Sequence

import yaml

from .sandbox.yaml_diff_validator import validate_skill_yaml_diff


_PACKAGE_ROOT = Path(__file__).resolve().parents[1]
_CONFIG_PATH = _PACKAGE_ROOT / "config.yaml"


# --- Subcommand: check ------------------------------------------------


def _cmd_check(args: argparse.Namespace) -> int:
    """Validate config + run sandbox self-test.

    Returns 0 on success, 1 on any failure (file missing, sandbox
    misbehaves on the canned positive / negative case).
    """
    config_path = Path(args.config) if args.config else _CONFIG_PATH
    if not config_path.exists():
        print(f"[check] FAIL: config not found at {config_path}", file=sys.stderr)
        return 1

    with config_path.open("r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)

    repo_root = _resolve_repo_root(config_path)
    surface = cfg.get("mutable_surface", {})
    allowed_files = surface.get("allowed_skill_files") or []
    allowed_fields = surface.get("allowed_field_paths") or []

    print(f"[check] config: {config_path}")
    print(f"[check] repo root: {repo_root}")
    print(f"[check] allowed_field_paths: {len(allowed_fields)}")
    print(f"[check] allowed_skill_files: {len(allowed_files)}")

    missing: list[str] = []
    for rel in allowed_files:
        abs_path = repo_root / rel
        if not abs_path.exists():
            missing.append(rel)
    if missing:
        for m in missing:
            print(f"[check] FAIL: allowed_skill_files entry missing on disk: {m}", file=sys.stderr)
        return 1
    print(f"[check] PASS: all {len(allowed_files)} Skill YAML paths exist on disk")

    if not _self_test_sandbox(allowed_files, allowed_fields):
        return 1
    print("[check] PASS: sandbox self-test (1 positive + 1 negative)")
    return 0


def _resolve_repo_root(config_path: Path) -> Path:
    """The config holds repo-relative paths; resolve them against the
    config file's grandparent (autoloop/config.yaml → repo root).
    """
    return config_path.resolve().parent.parent


def _self_test_sandbox(
    allowed_files: list[str], allowed_fields: list[str]
) -> bool:
    """Run a positive (procedure-only edit) and a negative
    (tools_required edit) through the validator; verify the decisions.
    """
    if not allowed_files:
        print("[check] FAIL: no allowed_skill_files; cannot self-test", file=sys.stderr)
        return False
    pos_target = allowed_files[0]

    before = (
        "name: discover_triage\n"
        "tools_required:\n  - search_knowledge\n"
        "procedure: \"step one\"\n"
    )
    after_positive = (
        "name: discover_triage\n"
        "tools_required:\n  - search_knowledge\n"
        "procedure: \"step one revised\"\n"
    )
    after_negative = (
        "name: discover_triage\n"
        "tools_required:\n  - search_knowledge\n  - classify_use_case\n"
        "procedure: \"step one\"\n"
    )

    pos = validate_skill_yaml_diff(
        before_yaml=before,
        after_yaml=after_positive,
        file_path=pos_target,
        allowed_skill_files=allowed_files,
        allowed_field_paths=allowed_fields,
    )
    if pos.decision != "ACCEPT":
        print(
            f"[check] FAIL: self-test positive expected ACCEPT, got {pos.decision} ({pos.reason})",
            file=sys.stderr,
        )
        return False

    neg = validate_skill_yaml_diff(
        before_yaml=before,
        after_yaml=after_negative,
        file_path=pos_target,
        allowed_skill_files=allowed_files,
        allowed_field_paths=allowed_fields,
    )
    if neg.decision != "REJECT":
        print(
            f"[check] FAIL: self-test negative expected REJECT, got {neg.decision} ({neg.reason})",
            file=sys.stderr,
        )
        return False
    return True


# --- Placeholder subcommands -----------------------------------------


def _cmd_placeholder(name: str):
    def _run(_args: argparse.Namespace) -> int:
        print(f"[{name}] S-Auto-3 territory — not implemented in S-Auto-1.")
        return 1
    return _run


# --- Argument parser --------------------------------------------------


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="autoloop",
        description="Auto-evolution loop subsystem (M-Auto-1A). See autoloop/program.md.",
    )
    sub = parser.add_subparsers(dest="subcommand", metavar="<subcommand>")
    sub.required = True

    p_check = sub.add_parser("check", help="Validate config + run sandbox self-test.")
    p_check.add_argument("--config", help="Path to autoloop config.yaml (default: package config).")
    p_check.set_defaults(func=_cmd_check)

    p_dry = sub.add_parser("dry-run", help="(S-Auto-3 placeholder) Single iteration without commit.")
    p_dry.set_defaults(func=_cmd_placeholder("dry-run"))

    p_run = sub.add_parser("run", help="(S-Auto-3 placeholder) Live iteration loop.")
    p_run.set_defaults(func=_cmd_placeholder("run"))

    p_report = sub.add_parser("report", help="(S-Auto-3 placeholder) Render per-iteration audit report.")
    p_report.set_defaults(func=_cmd_placeholder("report"))

    p_apply = sub.add_parser("apply", help="(S-Auto-3 placeholder) Cherry-pick a kept iteration to main.")
    p_apply.add_argument("--experiment", help="Experiment id to apply.")
    p_apply.set_defaults(func=_cmd_placeholder("apply"))

    p_audit = sub.add_parser("audit", help="(S-Auto-3 placeholder) Inspect a finished iteration.")
    p_audit.add_argument("--experiment", help="Experiment id to audit.")
    p_audit.set_defaults(func=_cmd_placeholder("audit"))

    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = _build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())
