"""CI-style integration test: run the linter against the regenerated corpus.

The post-A3 anchor / promotion / exploration buckets must lint clean
(zero error-severity violations). The smoke/ bucket is a curated SUBSET
of the parent buckets (Wave A3.5) and is also expected to lint clean
when run against just smoke/. When the linter runs against the full
case_specs/ tree, the cross-bucket duplicate source_session_ids between
smoke and its parents need to be opted in via ``--subset-of``.
"""

from __future__ import annotations

import os
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


def _venv_python() -> str:
    """Resolve the interpreter that can import the ``eval_interactive``
    package for the linter subprocess.

    Sprint 071 / S-Auto-15 (B3): ``sys.executable`` is NOT reliable here.
    Under ``uv run pytest`` on this machine the pytest process reports
    ``sys.executable``/``sys.prefix`` as the conda interpreter
    (``~/miniconda3/bin/python``) even though ``uv run`` execs
    ``.venv/bin/pytest`` — so a ``sys.executable`` subprocess hits
    ``ModuleNotFoundError: No module named 'eval_interactive.case_spec'``
    (the editable install lives only in the uv venv, not conda). The
    ``VIRTUAL_ENV`` env var that ``uv run`` exports IS reliable, so prefer
    ``$VIRTUAL_ENV/bin/python`` when it exists. Fall back to
    ``sys.executable`` for non-uv invocations (e.g. a plain venv that runs
    pytest with its own interpreter). Test-infra only; the corpus itself
    lints clean under the resolved interpreter.
    """
    venv = os.environ.get("VIRTUAL_ENV")
    if venv:
        candidate = Path(venv) / "bin" / "python"
        if candidate.exists():
            return str(candidate)
    return sys.executable


def _run_linter(
    target_dir: Path,
    subset_of: str | None = None,
) -> subprocess.CompletedProcess:
    """Invoke the linter as a subprocess against ``target_dir``."""
    cmd = [
        _venv_python(),
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


def test_canonical_corpus_lints_clean_with_smoke_subset_flag(tmp_path):
    """The canonical golden-conformant corpus (anchor / promotion /
    exploration / smoke) must lint clean as a combined tree when the
    smoke-as-subset relationship is opted in via ``--subset-of``.

    Without the flag, R13 source_session_no_duplication legitimately
    fires because smoke shares source_sessions with its parent buckets;
    this test confirms the cross-bucket R13 opt-in still resolves clean.

    Sprint 071 / S-Auto-15 (B4): scoped from "the ENTIRE case_specs/
    tree" to the four canonical golden buckets. The original test linted
    ``CASE_SPECS_ROOT`` outright, but ``case_specs/`` has since grown
    intentionally-NON-golden buckets — ``bad_cases/`` (the §5.6 curated
    failure-shape suite, by design encodes escalate-where-policy-says-
    resolve etc.), ``anchor_outcome/``, ``case_families/``, ``probe/``.
    The linter's R1/R2/R9 golden-policy rules (the source of truth, NOT
    weakened here) legitimately flag those buckets, so a whole-tree clean
    assertion can never hold for them. This test had in fact been FAILING
    all along, masked by an interpreter bug (it ran under the conda
    interpreter and exited 1 on ModuleNotFoundError, never reaching the
    corpus); the B3 ``_venv_python`` fix exposed the real linter output.
    The corpus is NOT edited and the golden-policy rules are NOT relaxed;
    the test is scoped to the buckets it was designed to cover (the same
    set the per-bucket test above already lints clean). The non-golden
    buckets carry their own lifecycles (e.g. bad_cases §5.6 human-judgment
    gate) and are out of scope for golden-policy lint.
    """
    if not CASE_SPECS_ROOT.exists():
        pytest.skip(f"case_specs root not present at {CASE_SPECS_ROOT}")

    # Stage the canonical buckets into a combined tree so the cross-bucket
    # R13 smoke-subset relationship is exercised exactly as it would be on
    # the real tree, without dragging in the non-golden buckets.
    import shutil

    staged_root = tmp_path / "case_specs"
    staged_root.mkdir()
    present_buckets = []
    for bucket in CORPUS_BUCKETS:
        src = CASE_SPECS_ROOT / bucket
        if src.exists():
            shutil.copytree(src, staged_root / bucket)
            present_buckets.append(bucket)
    if not present_buckets:
        pytest.skip("no canonical corpus buckets present to lint")

    result = _run_linter(staged_root, subset_of=SUBSET_OF_FLAG)

    assert result.returncode == 0, (
        f"linter exited with {result.returncode} on the canonical corpus "
        f"({present_buckets!r}) with --subset-of {SUBSET_OF_FLAG!r} "
        f"(expected 0 = no errors).\n"
        f"--- stdout ---\n{result.stdout}\n"
        f"--- stderr ---\n{result.stderr}\n"
    )
    assert "Errors: **0**" in result.stdout, (
        f"linter report for the canonical corpus did not advertise zero "
        f"errors; stdout was:\n{result.stdout}"
    )
