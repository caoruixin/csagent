# Shadow-class case-spec access boundary

Sprint 20 Track A v0. Source-of-truth: this file plus
`docs/current/iteration_governance.md` §5.1 (acceptance bars) and
`docs/sprint_objective.md` §"Tracks (in scope) Track A — Shadow-split
mechanism".

This directory holds the **shadow class** of case-family CaseSpecs per
`iteration_governance.md` §5.1: cases that exercise the same surface as
a target / neighbor / negative family member but are **held out from
the dev agent during development**. They exist so that a future
remediation sprint that ships a fix against a visible target can be
independently checked for shadow regression by the human and the
review agent.

## Who may read what

| Role | May read `case_specs/case_families/**` | May read `case_specs_shadow/**` |
| --- | --- | --- |
| Dev agent (this sprint, future remediation sprints) | yes | **NO** |
| Human reviewer | yes | yes |
| Review agent (Codex, Anti-Hardcode reviewer) | yes | yes |
| Eval-harness runner (default invocation) | yes | no |

The dev agent **must not** load, sample, or glob this directory during
development of any sprint, including Sprint 20 itself. Sprint 20 IS
the sprint that authors the shadow class; Sprint 20 does not consume
it.

## Enforcement (v0)

The boundary above is enforced by three lightweight measures, none of
which is cryptographic isolation. These are the v0 mechanisms that
actually shipped in Sprint 20 (see also
`docs/sprints/sprint-020-handoff.md` lines 443–448):

1. **Directory boundary.** Shadow CaseSpecs live in
   `eval_interactive/case_specs_shadow/case_families/`, **not** under
   `eval_interactive/case_specs/`. The existing
   `CaseSetManager` (`eval_interactive/eval_interactive/batch/sets.py`,
   `__init__(base_dir="case_specs")`) does not see this directory
   under any `--set` flag (`anchor`, `promotion`, `exploration`,
   `smoke`, `all`).

2. **Custom-path-only loading.** Reading shadow CaseSpecs requires an
   explicit `CaseSetManager.load_custom(path)` call with a path under
   `eval_interactive/case_specs_shadow/`. No `--set` value resolves to
   that directory; no default-invocation surface walks it. Human and
   review-agent workflows that need to read shadow content invoke
   `load_custom(path)` deliberately.

3. **Documented self-restraint.** This file. The dev agent reads
   `case_specs_shadow/_ACCESS_BOUNDARY.md` if and only if it stumbles
   into the directory in error, and reads no further. The line "do not
   read shadow CaseSpecs during development" is reinforced in the
   per-task reading list at
   `docs/current/agent_context_guide.md` (added in the next governance
   fold-back, not this sprint).

## Known v0 gaps / v1 hardening

- The directory is checked into the git working tree. A future agent
  that runs an unconstrained `find` / `glob` over the repo will see
  the file paths. The shadow class id index is in `_manifest.yaml`
  (this directory), but the visible manifest at
  `case_specs/case_families/_manifest.yaml` lists each family's
  `shadow_case_ids` as `[REDACTED — see case_specs_shadow/_manifest.yaml]`
  to limit accidental cross-reading.
- **`--include-shadow` runner flag (v1 hardening direction).** Sprint
  20 did **not** add a `--include-shadow` CLI flag; the runner-gate
  surface is documented here as a v1 hardening direction the next
  eval-governance sprint may add (tracked as
  `R-shadow-include-flag-runner-gate` per
  `docs/sprints/sprint-020-handoff.md:927`). If/when added, the flag
  would gate `eval-interactive run` so that shadow CaseSpecs are not
  loaded by default and the human / review agent passes
  `--include-shadow` to opt in. The flag would be a developer-facing
  flag, not cryptographic isolation: a dev agent that ran the flag
  deliberately could still observe shadow results. The flag's purpose
  would be to make the human's and the review agent's review easy and
  to harden the default-off semantics, not to defeat a determined
  dev agent.
- A v1 hardened mechanism (gitignore + separate committed branch /
  out-of-band delivery) is deferred. Sprint 20 records the gap; the
  next eval-governance sprint may revisit if the v0 directory
  boundary + custom-path-only loading + self-restraint prove
  insufficient in practice.

## Adding to or amending the shadow class

- New shadow CaseSpecs go under
  `case_specs_shadow/case_families/<family_id>/` with the same YAML
  shape as visible CaseSpecs.
- `case_specs_shadow/_manifest.yaml` lists every shadow case id per
  family. The visible manifest at
  `case_specs/case_families/_manifest.yaml` carries the family but
  redacts the shadow case ids.
- Edits to this access boundary itself belong in a governance sprint,
  not a remediation sprint. Sprint 20 v0 is the baseline; tighter
  enforcement is folded back on cadence.
