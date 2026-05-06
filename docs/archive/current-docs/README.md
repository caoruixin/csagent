# docs/archive/current-docs/

This directory holds dated snapshots of the *current working*
documents that the project intentionally treats as
overwrite-current-state files:

- `docs/10-handoff.md`
- `docs/codex-findings.md`
- `docs/sprint_objective.md`

Those three files must not become append-only historical logs.
They each represent the *latest* current handoff, latest Codex
review decision, and latest sprint objective respectively. When
one of them is about to be replaced for a new sprint, a closure
review, or any post-sprint validation pass, the previous
version is archived first so the historical content is not
lost.

## What belongs here

- Ad hoc transition snapshots — e.g. a pre-Sprint-9 compaction
  copy of `docs/10-handoff.md` after it has accumulated
  multiple post-sprint validation appendices but before a new
  sprint starts.
- Pre-overwrite copies of `docs/codex-findings.md` or
  `docs/sprint_objective.md` taken when they were not
  already saved as part of a sprint closure.
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

`docs/10-handoff.md`, `docs/codex-findings.md`, and
`docs/sprint_objective.md` are latest-only working files.
Before they are overwritten:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or here if it is an ad hoc
   transition;
2. then overwrite the working file with the latest current
   version;
3. keep only actionable current state in the working file.
