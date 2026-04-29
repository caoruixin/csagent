"""CI-style integration test: run the linter against the regenerated corpus.

The post-A3 anchor / promotion / exploration buckets must lint clean
(zero error-severity violations). The smoke/ bucket is a curated SUBSET
of the parent buckets (Wave A3.5) and is also expected to lint clean
when run against just smoke/. When the linter runs against the full
case_specs/ tree, the cross-bucket duplicate source_session_ids between
smoke and its parents need to be opted in via ``--subset-of``.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest


# eval_interactive/tests/regression/test_corpus_lint.py -> the inner
# ``eval_interactive`` package root (two levels up). The case_specs/
# corpus lives next to it as a sibling directory.
EVAL_INTERACTIVE_ROOT = Path(__file__).resolve().parents[2]
CASE_SPECS_ROOT = EVAL_INTERACTIVE_ROOT / "case_specs"

CORPUS_BUCKETS = ("anchor", "promotion", "exploration", "smoke")
SUBSET_OF_FLAG = "smoke:anchor,smoke:promotion,smoke:exploration"


def _run_linter(
    target_dir: Path,
    subset_of: str | None = None,
) -> subprocess.CompletedProcess:
    """Invoke the linter as a subprocess against ``target_dir``."""
    cmd = [
        sys.executable,
        "-m",
        "eval_interactive.case_spec.linter",
        str(target_dir),
    ]
    if subset_of:
        cmd.extend(["--subset-of", subset_of])
    return subprocess.run(
        cmd,
        cwd=str(EVAL_INTERACTIVE_ROOT.parent),  # run from repo root so the
        # ``eval_interactive`` package import works regardless of the test
        # runner's cwd.
        capture_output=True,
        text=True,
        timeout=120,
    )


@pytest.mark.parametrize("bucket", CORPUS_BUCKETS)
def test_regenerated_corpus_bucket_lints_clean(bucket: str):
    """Each regenerated bucket must lint with zero error-severity rules.

    Linter exit codes (see ``case_spec/linter.py:main``):
    * 0 -> no errors (warnings allowed)
    * 1 -> at least one error
    * 2 -> path does not exist
    """
    target = CASE_SPECS_ROOT / bucket
    if not target.exists():
        pytest.skip(f"corpus bucket {bucket!r} not present at {target}")

    result = _run_linter(target)

    assert result.returncode == 0, (
        f"linter exited with {result.returncode} on case_specs/{bucket} "
        f"(expected 0 = no errors).\n"
        f"--- stdout ---\n{result.stdout}\n"
        f"--- stderr ---\n{result.stderr}\n"
    )

    # Defensive: the markdown report should explicitly say "Errors: 0".
    assert "Errors: **0**" in result.stdout, (
        f"linter report for case_specs/{bucket} did not advertise zero "
        f"errors; stdout was:\n{result.stdout}"
    )


def test_full_corpus_lints_clean_with_smoke_subset_flag():
    """Linting the entire case_specs/ tree must be clean when the
    smoke-as-subset relationship is opted in via ``--subset-of``.

    Without the flag, R13 source_session_no_duplication legitimately
    fires because smoke shares source_sessions with its parent buckets.
    """
    if not CASE_SPECS_ROOT.exists():
        pytest.skip(f"case_specs root not present at {CASE_SPECS_ROOT}")

    result = _run_linter(CASE_SPECS_ROOT, subset_of=SUBSET_OF_FLAG)

    assert result.returncode == 0, (
        f"linter exited with {result.returncode} on full case_specs/ tree "
        f"with --subset-of {SUBSET_OF_FLAG!r} (expected 0 = no errors).\n"
        f"--- stdout ---\n{result.stdout}\n"
        f"--- stderr ---\n{result.stderr}\n"
    )
    assert "Errors: **0**" in result.stdout, (
        f"linter report for full case_specs/ tree did not advertise zero "
        f"errors; stdout was:\n{result.stdout}"
    )
