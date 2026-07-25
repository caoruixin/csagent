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


# WS-1 item 6: the R1 carve-out that used to silence the
# resolve-policy-UC-stamped-escalate contradiction was REMOVED
# (``linter.py::_r1_uc_outcome_consistent``). 158 specs are simultaneously
# ``should_escalate: true`` and ``allow_bot_resolution: 'true'`` (replan
# §1.1 A1/A2), and R1 now reports them. Making them visible is the point of
# WS-1; REWRITING them is WS-2's scope, so these tests no longer assert a
# clean corpus. They assert instead that:
#
#   1. no rule OTHER than R1 regresses (the property the original tests
#      actually protected), and
#   2. the R1 backlog stays bounded — a guard against the corpus growing
#      NEW contradictions while WS-2 works through the existing ones.
#
# Ratchet these to 0 as WS-2 rewrites each bucket. Measured 2026-07-25
# immediately after the carve-out removal (anchor 74 + promotion 64 +
# smoke 6 = 144 of the 158 contradictory specs; the remaining 14 live in
# case_families/, which is not a CORPUS_BUCKETS member).
R1_RULE_PREFIX = "R1 uc_outcome_consistent"
R1_BACKLOG_MAX: dict[str, int] = {
    "anchor": 74,
    "promotion": 64,
    "exploration": 0,
    "smoke": 6,
}


def _violation_lines(stdout: str) -> list[str]:
    """Report lines that record an ERROR-severity violation."""
    return [ln for ln in stdout.splitlines() if "**ERROR**" in ln]


def _non_r1_errors(stdout: str) -> list[str]:
    return [ln for ln in _violation_lines(stdout) if R1_RULE_PREFIX not in ln]


def _r1_errors(stdout: str) -> list[str]:
    return [ln for ln in _violation_lines(stdout) if R1_RULE_PREFIX in ln]


@pytest.mark.parametrize("bucket", CORPUS_BUCKETS)
def test_regenerated_corpus_bucket_has_no_non_r1_errors(bucket: str):
    """Each regenerated bucket must lint clean of every rule EXCEPT R1.

    R1 is the known WS-2 backlog (see the module comment). Any other
    error-severity rule firing is a genuine regression.
    """
    target = CASE_SPECS_ROOT / bucket
    if not target.exists():
        pytest.skip(f"corpus bucket {bucket!r} not present at {target}")

    result = _run_linter(target)

    non_r1 = _non_r1_errors(result.stdout)
    assert not non_r1, (
        f"case_specs/{bucket} has {len(non_r1)} non-R1 error-severity "
        f"violation(s); only the R1 WS-2 backlog is tolerated.\n"
        + "\n".join(non_r1[:20])
    )


@pytest.mark.parametrize("bucket", CORPUS_BUCKETS)
def test_regenerated_corpus_bucket_r1_backlog_is_bounded(bucket: str):
    """The R1 contradiction backlog must not grow while WS-2 works it down."""
    target = CASE_SPECS_ROOT / bucket
    if not target.exists():
        pytest.skip(f"corpus bucket {bucket!r} not present at {target}")

    result = _run_linter(target)
    r1 = _r1_errors(result.stdout)
    budget = R1_BACKLOG_MAX[bucket]
    assert len(r1) <= budget, (
        f"case_specs/{bucket} R1 backlog grew to {len(r1)} (max {budget}). "
        f"New outcome/policy contradictions must not be added while WS-2 "
        f"rewrites the existing ones."
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

    # WS-1 item 6: R1 is the known WS-2 backlog and no longer gates. What
    # this test still protects is the R13 cross-bucket opt-in: linting the
    # combined tree with --subset-of must not surface duplicate
    # source_session_id errors between smoke and its parent buckets.
    non_r1 = _non_r1_errors(result.stdout)
    assert not non_r1, (
        f"canonical corpus ({present_buckets!r}) with --subset-of "
        f"{SUBSET_OF_FLAG!r} produced {len(non_r1)} non-R1 error-severity "
        f"violation(s); the R13 subset opt-in should resolve clean.\n"
        + "\n".join(non_r1[:20])
    )
    assert not [ln for ln in non_r1 if "R13" in ln], (
        "R13 source_session_no_duplication fired despite --subset-of"
    )
