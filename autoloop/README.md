# csagent autoloop

Auto-evolution loop subsystem for the csagent. The loop is a Skill-driven
hill-climbing meta-agent constrained to a tightly-locked **mutable
surface** — 6 Skill YAMLs × 4 LLM-soft field classes. Everything else is
structurally rejected by the sandbox.

This package is the M-Auto-1A scaffold. Sub-sprint S-Auto-1 (Sprint 54)
ships the directory skeleton + the human-readable contract
(`program.md`) + the YAML-diff sandbox. The loop orchestrator,
tier-evaluator, and meta-agent ship in S-Auto-2 / S-Auto-3 / S-Auto-4.

## CLI synopsis

| Subcommand | Status | Purpose |
|---|---|---|
| `check`     | S-Auto-1 | Validate `config.yaml`, verify the 6 allowed Skill YAML paths exist on disk, self-test the sandbox on one positive + one negative fixture. |
| `dry-run`   | S-Auto-3 (placeholder; fitness evaluator delivered by S-Auto-2, loop wiring lands in S-Auto-3) | Single iteration without committing — propose, evaluate, write report, discard. |
| `run`       | S-Auto-3 (placeholder; fitness evaluator delivered by S-Auto-2, loop wiring lands in S-Auto-3) | Live iteration loop (creates `autoloop/exp-N` test branches). |
| `report`    | S-Auto-3 (placeholder) | Render per-iteration human audit report. |
| `apply`     | S-Auto-3 (placeholder) | Human-driven cherry-pick of a kept iteration to `main`. |
| `audit`     | S-Auto-3 (placeholder); S-Auto-4 surfaces `gaming_flags` + `anti_hardcode_flag_for_codex` | Inspect a finished iteration's evidence + meta-agent reasoning. |

All subcommands are exposed via the `autoloop` entry point installed by
`pyproject.toml` and additionally as `python -m autoloop <subcommand>`.

## Pointers

- `program.md` — the full human-readable contract. Read it BEFORE
  running anything that could mutate a Skill YAML. The contract is
  immutable during M-Auto-1A execution.
- `docs/milestone_objective.md` — milestone scope (M-Auto-1A).
- `docs/solutions/auto_evolution_skill_driven_v1.md` — design rationale,
  alternatives, §1.7 mapping, risk table.

## Install (dev)

```
cd autoloop
uv pip install -e .
uv pip install pytest
uv run pytest -q
```

## Status

S-Auto-1 deliverable: directory skeleton + `program.md` + sandbox.
No live LLM calls. No subprocess invocations of `mvn` / `spring-boot`
/ `eval-interactive`. No writes to any Skill YAML. The sandbox
**validates** diffs; the **applier** is a skeleton that documents the
single-file contract S-Auto-3 will consume.
