"""S-Auto-38 (Sprint 092) zero-LLM tier-0 re-bless under the split checks.

Re-scores the EXISTING ``m-auto-7-prepilot-baseline-20260608`` baseline run dir
under the WP-A split checks (NO LLM, NO new draws) and writes a NEW DATED dir
with the OLD and NEW tier-0 classification maps side-by-side + metadata. The old
baseline dir is left immutable; the canonical ``config.fitness.baseline_dir`` /
``current_eval_baseline.md`` pointer is NOT flipped (deferred to milestone close).

Per the binding-gate order this RUN is the human's post-review step (gate 6,
after Codex §4.1 + human blast-radius sign-off). This script PREPARES that step
and can be run with ``--preview`` to validate against a labelled scratch dir
without claiming the canonical re-bless.

Usage (from repo root):
    cd eval_interactive && uv run python analysis/rebless_s_auto_38_tier0_split.py --preview
    # canonical (post-review):
    cd eval_interactive && uv run python analysis/rebless_s_auto_38_tier0_split.py
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
from datetime import date

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from replay_s_auto_38_gate_split import (  # noqa: E402
    _spec_index,
    load_run,
    replay_case,
)

_HERE = os.path.dirname(os.path.abspath(__file__))
_EVAL = os.path.dirname(_HERE)
_REPO = os.path.dirname(_EVAL)
_SOURCE_BASELINE = "m-auto-7-prepilot-baseline-20260608"
_GATE_DEFINITION_VERSION = "tier0_escalation_split_s_auto_38_v1"

# The scoring files whose identity this re-bless is bound to.
_SCORING_FILES = (
    "eval_interactive/eval_interactive/scoring/hard_checks.py",
    "eval_interactive/eval_interactive/scoring/escalation_reason_match.py",
    "autoloop/autoloop/scoring/tier_evaluator.py",
)


def _scoring_code_sha() -> str:
    h = hashlib.sha256()
    for rel in _SCORING_FILES:
        with open(os.path.join(_REPO, rel), "rb") as f:
            h.update(f.read())
    return h.hexdigest()[:16]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", action="store_true",
                    help="write to a labelled scratch dir (pre-review validation)")
    ap.add_argument("--out-dir", default=None)
    args = ap.parse_args()

    today = date.today().isoformat().replace("-", "")
    default_name = (f"{_SOURCE_BASELINE.split('-2026')[0]}-{today}-s_auto_38_split"
                    + ("-preview" if args.preview else ""))
    out_dir = args.out_dir or os.path.join(_EVAL, "results", default_name)
    os.makedirs(out_dir, exist_ok=True)

    idx = _spec_index()
    baseline = load_run(os.path.join(_EVAL, "results", _SOURCE_BASELINE))

    suites: dict[str, dict] = {}
    flips = []
    for cid, (suite, case) in baseline.items():
        if "escalation_compliance" not in (case.get("tier0_majority") or {}):
            continue
        spec = idx.get(cid)
        if spec is None:
            continue
        r = replay_case(spec, case)
        if r is None:
            continue
        suites.setdefault(suite, {})[cid] = {
            "old_escalation_compliance": r["old_escompl_stored"],
            "new_escalation_compliance_part1": r["new_part1"],
            "new_escalation_reason_family_match_obs": r["new_part2_obs"],
            "others_pass": r["others_pass"],
            "tier0_old": r["tier0_old"],
            "tier0_new": r["tier0_new"],
            "changed": r["delta"],
        }
        if r["delta"]:
            flips.append(f"{suite}:{cid}")

    for suite, m in suites.items():
        with open(os.path.join(out_dir, f"{suite}_tier0_old_new.json"), "w") as f:
            json.dump(m, f, indent=1)

    meta = {
        "scoring_code_sha": _scoring_code_sha(),
        "source_baseline": _SOURCE_BASELINE,
        "generated_at": date.today().isoformat(),
        "gate_definition_version": _GATE_DEFINITION_VERSION,
        "scoring_files": list(_SCORING_FILES),
        "status": "pre_review_preview" if args.preview else "canonical_pending_human_signoff",
        "note": ("Zero-LLM re-score of the baseline run under the WP-A split "
                 "checks. Old baseline dir is immutable; canonical "
                 "config.fitness.baseline_dir / current_eval_baseline.md pointer "
                 "flip is DEFERRED to milestone close (separate human decision)."),
        "tier0_changed_cases": sorted(flips),
        "tier0_changed_count": len(flips),
        "suites": {s: len(m) for s, m in suites.items()},
    }
    with open(os.path.join(out_dir, "_rebless_metadata.json"), "w") as f:
        json.dump(meta, f, indent=1)

    print(f"re-bless written to: {out_dir}")
    print(json.dumps(meta, indent=1))


if __name__ == "__main__":
    main()
