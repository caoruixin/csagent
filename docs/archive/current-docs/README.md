# docs/archive/current-docs/

This directory holds dated snapshots of the *current working*
documents that the project intentionally treats as
overwrite-current-state files:

- `docs/10-handoff.md`
- `docs/codex-findings.md`
- `docs/sprint_objective.md`
- `docs/action_bank.md`

These files must not become append-only historical logs. They
each represent the *latest* current handoff, latest Codex
review decision, latest sprint objective, and current action
ledger respectively. `docs/action_bank.md` is a *current
ledger* (active / next actions, open deferred backlog, compact
closed-action index) — not a full sprint-by-sprint history.
When one of these files is about to be replaced for a new
sprint, a closure review, a post-sprint validation pass, or
any major rewrite, the previous version is archived first so
the historical content is not lost.

## What belongs here

- Ad hoc transition snapshots — e.g. a pre-Sprint-9 compaction
  copy of `docs/10-handoff.md` after it has accumulated
  multiple post-sprint validation appendices but before a new
  sprint starts.
- Pre-overwrite copies of `docs/codex-findings.md` or
  `docs/sprint_objective.md` taken when they were not
  already saved as part of a sprint closure.
- Pre-rewrite copies of `docs/action_bank.md` taken before
  major compaction or restructuring.
- Any snapshot that is preserved purely for historical
  reference and is not the canonical record of a sprint
  closure.

## What does NOT belong here

- Sprint closure handoffs / objectives / Codex reviews. Those
  belong under `docs/sprints/` using the existing
  `sprint-NNN-...` naming convention. Sprint closure archives
  remain the canonical historical record per sprint.
- Eval result JSON files (`eval_interactive/results/`).
- QA reports (`qa-reports/`).

## Naming convention

`YYYY-MM-DD-<short-tag>-<source-filename>.md`, e.g.
`2026-05-06-pre-sprint9-10-handoff.md`.

The date is the date of archiving, the tag identifies the
transition, and the source filename matches the working file
this snapshot was taken from.

## Working files are latest-only

`docs/10-handoff.md`, `docs/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
latest-only working files. `docs/action_bank.md` in
particular is a current ledger, not a full history — it
carries active / next actions, the open deferred backlog,
and a compact closed-action index, with sprint-by-sprint
implementation detail living under `docs/sprints/`. Before
any of these files are overwritten or undergo a major
rewrite:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or here if it is an ad hoc
   transition / major rewrite;
2. then overwrite the working file with the latest current
   version;
3. keep only actionable current state in the working file.
