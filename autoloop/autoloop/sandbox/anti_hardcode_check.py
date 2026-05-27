"""Anti-hardcode auto-check — S-Auto-3 placeholder; S-Auto-4 implements.

This module ships the **signature** of the propose-stage
anti-hardcode check that runs after the YAML-diff sandbox accepts a
proposal and before the applier writes to disk. S-Auto-3 ships an
always-PASS implementation so the full state machine in `loop.py`
can be exercised end-to-end; S-Auto-4 swaps in the real detector
without changing the signature.

Per `autoloop/program.md` §4 row 3 + `docs/current/iteration_governance.md`
§1.7 forbidden-list:

The S-Auto-4 detector should reject propose diffs that:

1. embed case_ids / session_ids / known eval phrases verbatim;
2. encode UC-specific if-else rules (e.g. `if user.message contains
   'appeal' then UC-H`);
3. use enumerated decision-tree narrations inside `procedure` or
   `critical_steps[].desc`;
4. otherwise cross any §1.7 forbidden-list line.

The placeholder MUST always return PASS so that S-Auto-3 close-gate
"FULL pipeline executes" can be demonstrated; S-Auto-4 dev replaces
the implementation behind this signature.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Literal, TYPE_CHECKING

if TYPE_CHECKING:
    from ..meta_agent.proposer import Hypothesis


@dataclass
class AntiHardcodeResult:
    """The verdict of `anti_hardcode_check`.

    - `decision` is "PASS" or "FAIL".
    - `reason` is one line of human-readable explanation (matched
      rule slug for FAIL; "placeholder_always_pass" for the S-Auto-3
      shim).
    - `matched_rules` lists the §1.7 forbidden-list rule slugs that
      triggered the FAIL; empty when `decision == "PASS"`.
    - `placeholder` flags this verdict as produced by the S-Auto-3
      shim (so handoff / Codex / audit can see at a glance that
      anti-hardcode enforcement is not yet active).
    """

    decision: Literal["PASS", "FAIL"]
    reason: str
    matched_rules: list[str]
    placeholder: bool = False


def anti_hardcode_check(
    hypothesis: "Hypothesis",
    *,
    config: dict[str, Any] | None = None,
) -> AntiHardcodeResult:
    """Placeholder always-PASS check.

    The signature is the contract S-Auto-4 must preserve. The body
    is a deliberate no-op marked as `placeholder=True` so the audit
    surface can tell the difference between "S-Auto-4 said PASS"
    and "S-Auto-3 shim said PASS".

    Do NOT add detection logic here — the entire point of separating
    S-Auto-3 from S-Auto-4 is that the structural defense in the
    `program.md` §4 forbidden-by-construction table can be reviewed
    on its own merits in S-Auto-4. Patching detection into the
    placeholder muddles the surface.
    """
    return AntiHardcodeResult(
        decision="PASS",
        reason="placeholder_always_pass",
        matched_rules=[],
        placeholder=True,
    )
