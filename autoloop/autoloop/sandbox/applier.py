"""Applier — skeleton (S-Auto-1 stub; S-Auto-3 implements git logic).

The applier is the caller layer that consumes
`autoloop/sandbox/yaml_diff_validator.py`. It is the only place where
the cross-file fence (program.md §3.A #9 / §3.B #6) is enforced —
the validator itself is per-file.

Contract for S-Auto-3:

    A meta-agent propose bundle MAY name at most ONE target Skill
    YAML. The applier receives `(file_path, before_yaml, after_yaml)`
    for that one file; if the propose bundle names more than one
    file, the applier MUST short-circuit REJECT before invoking
    `validate_skill_yaml_diff` at all. There is no API path that
    invokes the validator with multiple file paths.

    Conceptually:

        def apply_proposal(proposal: ProposalBundle) -> ApplyResult:
            if len(proposal.changes) != 1:
                return ApplyResult.reject(
                    "cross-file diff: only single-file edits allowed"
                )
            change = proposal.changes[0]
            verdict = validate_skill_yaml_diff(
                before_yaml=change.before,
                after_yaml=change.after,
                file_path=change.file_path,
            )
            if verdict.decision == "REJECT":
                return ApplyResult.reject(verdict.reason)
            # ... write to `autoloop/exp-N` branch via git ...
            # ... NEVER to main; main is human-driven via the `apply`
            # subcommand operating on a kept iteration's keep-{N}
            # branch ...

S-Auto-1 ships only this docstring + a placeholder. S-Auto-3 fills
in the git logic. The single-file invariant is documented here so
that any future contributor reading the validator first knows where
the cross-file fence lives.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ApplyResult:
    """Placeholder for the S-Auto-3 ApplyResult dataclass."""

    decision: str
    reason: str


def apply_proposal(*args, **kwargs):  # pragma: no cover - skeleton only
    """Placeholder; S-Auto-3 will implement git commit + branch logic.

    Raising NotImplementedError makes the placeholder load-bearing —
    any accidental invocation from S-Auto-1 surfaces immediately.
    """
    raise NotImplementedError(
        "apply_proposal is an S-Auto-3 deliverable; S-Auto-1 ships only "
        "the validator and the contract docstring."
    )
