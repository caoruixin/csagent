---
title: Milestone framework + milestone-shared Codex review
doc_tier: current-runtime
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-02
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Extracted from docs/current/iteration_governance.md §8 and §4.3 on 2026-06-02
  as part of the Layer A/B always-loaded split. Original section numbering
  preserved. Cite as "milestone-framework §8.N" and "milestone-framework §4.3".
---

# Milestone framework

This process doc receives the milestone framework (§8) and the
milestone-shared Codex review convention (§4.3), moved out of the
always-loaded `iteration_governance.md` on 2026-06-02. Section numbers
are preserved against the original file so existing citations
("milestone-framework §8.4", "§4.3") continue to resolve.

References to sections that stayed in the always-loaded Layer A read
"iteration_governance §X" (e.g., iteration_governance §1.7, §3.2,
§4.1, §7) for unambiguity.

## 4.3 Milestone-shared Codex review (2026-05-16 update)

Per the §8 milestone framework introduced 2026-05-16: sub-sprints
within an active milestone may share a single Codex sprint-close
review at **milestone close** rather than dispatching Codex per
sub-sprint. The iteration_governance §4.1 nine-question kernel and the
iteration_governance §4.2 sprint-close header are written once per
milestone, against the cumulative commit range of all sub-sprints in
that milestone.

**Per-sub-sprint Codex review remains REQUIRED** when the sub-sprint:

1. Introduces a new Tier-0 candidate (a candidate invariant for
   `docs/runtime_freeze_and_risk_policy.md` §1 / §2) — Codex must
   verify the candidate at sprint close before the next sub-sprint
   begins;
2. Crosses a iteration_governance §1.7 forbidden-list red line — Codex
   must verify the justification at sprint close;
3. Touches a hard-fenced surface that the milestone objective
   explicitly named out of scope (e.g., editing an existing case
   family per cascade fence);
4. Closes a sub-sprint with a `fix_required` outcome that needs
   per-sub-sprint re-review before the milestone can continue.

For default sub-sprints (semantic-touching but not Tier-0-adjacent,
not §1.7-adjacent, not hard-fence-violating, not fix-iteration on
prior sub-sprint), Codex is deferred to milestone close. The
deliver-agent surfaces the per-sub-sprint deferral choice in
`docs/milestone_objective.md` and the dev session records it in
each sub-sprint handoff §11 (the dev does NOT dispatch Codex
themselves; the deliver-agent + human dispatch at milestone close).

Sub-sprints exempted from the iteration_governance §7 stanza (pure
infra, docs-only, config-governance, characterization-test) remain
Codex-exempt per iteration_governance §4.1 exemption clause regardless
of milestone framing.

## 8. Milestone framework (2026-05-16 update)

This section introduces the **milestone framework** that groups
sub-sprints into architectural themes. The framework is additive on
top of the existing sprint + governance structure; it does NOT
replace sprints, the iteration_governance §7 stanza, or any
iteration_governance §1 constitutional rule.

### 8.1 Definition

A **milestone** is a coordinated bundle of 3–5 sub-sprints sharing
a single architectural theme. Each milestone has:

- **One milestone objective document** at `docs/milestone_objective.md`
  (active milestone; archived to `docs/milestones/M<N>_objective.md`
  at milestone close).
- **One or more sub-sprint contracts** at `docs/sprint_objective.md`
  (active sub-sprint; archived to `docs/sprints/sprint-NNN-objective.md`
  at sub-sprint close per existing convention).
- **One milestone acceptance bar** derived from the curated
  bad-case suite (`eval_interactive/case_specs/bad_cases/`) per
  badcase-lifecycle §5.6 — typically a named bad case must close or
  improve materially.
- **Codex review at milestone close** per §4.3 (sub-sprints share
  one Codex review unless a per-sub-sprint trigger fires).

A **sub-sprint** within a milestone is a single dev-session unit of
work that ships a coherent slice of the milestone scope. Each
sub-sprint:

- Still has its own iteration_governance §7 stanza if
  semantic-touching.
- Still produces a `docs/sprints/sprint-NNN-handoff.md` dev-authored
  archive at sub-sprint close.
- Still flips relevant R-items in `docs/action_bank.md` per
  existing convention.
- Defers Codex review to milestone close per §4.3 default
  (unless a §4.3 per-sub-sprint trigger fires).

### 8.2 Why milestones (vs single-feature sprints)

Milestone-grained planning cuts deliver-agent and Codex overhead by
bundling 3–5 related sub-sprints under one planning round and one
close review, while preserving iteration_governance §1.7 anti-hardcode
discipline (each sub-sprint still fills the iteration_governance §7
stanza; Codex verifies at milestone close). The framework changes
cadence, not architecture.

### 8.3 Milestone objective document schema

`docs/milestone_objective.md` carries:

```yaml
---
title: Milestone M<N> — <name>
doc_tier: current-runtime
status: current
implementation_status: not_started | partial | implemented
source_of_truth: this file
last_reviewed: <YYYY-MM-DD>
review_cadence: per milestone
notes: >
  free-form context (scope rationale, hard-fenced surfaces, why
  this is one milestone not split across two).
---
```

Body sections (analogous to `sprint_objective.md` shape):

1. **Milestone class** (semantic-touching layer breakdown across
   sub-sprints; iteration_governance §7 stanza coverage at the
   milestone level).
2. **Goal** — the architectural outcome the milestone targets,
   expressed as user-facing or bad-case-suite-anchored behaviour
   change (not as code paths).
3. **Sub-sprint sequence** — preliminary list of 3-5 sub-sprints
   with class, layer, scope (3 sentences each), and dependency
   relationships. Deliver-agent + human may refine at each sub-
   sprint planning round; the milestone objective is updated
   in-place.
4. **Non-goals** (explicit; what the milestone does NOT cover,
   including which Alice / bad-case dimensions are deferred to
   later milestones).
5. **Milestone acceptance bar** — one or more bad cases (per
   badcase-lifecycle §5.6) that the milestone is expected to close or
   improve; per-bad-case closure criterion.
6. **Hard fences** at the milestone level (no edits to existing
   case families per cascade fence; no Tier-0 invention without
   human review; etc.).
7. **R-items consumed / surfaced** — which `action_bank.md`
   R-items the milestone is expected to consume; which new R-items
   the deliver-agent expects to surface.
8. **Codex review plan** per §4.3 — default milestone-shared OR
   per-sub-sprint triggers expected.
9. **Estimated milestone duration** (calendar weeks) — informational,
   not a gate.

### 8.4 Milestone close artefacts (deliver-agent owned)

At milestone close, the deliver-agent + human produce:

- Update `docs/milestone_objective.md` closure verdict (analogous
  to sprint handoff §12 — pass / fix_required / out-of-scope-review
  + classification + per-sub-sprint disposition).
- Archive the milestone objective to `docs/milestones/M<N>_objective.md`.
- Append the milestone's closed rows to `docs/action_bank_archive.md`
  per the `docs/action_bank.md` §7.1 retention sweep: closed per-sprint
  rows → §A, the closed-milestone row → §B, newly-closed R-item rows →
  §C. (`docs/action_bank.md` keeps only open / active / deferred items.)
- Refresh `docs/10-handoff.md` §0 table + §1 lead (demote current
  milestone to Preceding milestone; truncate §1 content older than
  the preceding milestone per `doc_governance.md` retention rule;
  add row to §2 archive index).
- Reset `docs/sprint_objective.md` to the first sub-sprint of the
  next milestone (or to a planning placeholder if no next milestone
  is locked).
- Optionally start a new `docs/milestone_objective.md` for the next
  milestone.

The deliver-agent's existing close-out artefacts (per
`feedback_commit_at_end_bundles_deliver_artefacts.md`) move from
per-sprint to per-milestone cadence; per-sub-sprint dev handoff
files still ship per sub-sprint close.

### 8.5 When to break milestone framing

The framework is not mandatory. A single high-risk feature (e.g.,
the Single Handover Orchestrator P0 launch blocker per
`docs/release_gate.md` §1.1) may be its own "milestone of one
sub-sprint" if that better matches the scope discipline. The
deliver-agent + human decide at planning round.

A milestone that exceeds 5 sub-sprints is a signal that the
milestone scope is too large; the deliver-agent SHALL split it at
the next milestone planning round.

A sub-sprint that crosses an unrelated architectural surface is a
signal that the sub-sprint belongs to a different milestone; the
deliver-agent SHALL surface this at sub-sprint planning round
rather than smuggle the scope across milestones.

### 8.6 Sprint vs milestone vs R-item relationship

```
docs/action_bank.md  (backlog, cross-milestone persistent;
                     R-items flow in from research / bad cases /
                     sprint findings, flow out on close)
       ↓ (deliver-agent picks 3-5 related R-items into a milestone)
docs/milestone_objective.md  (current milestone north star;
                              names sub-sprints + acceptance bar;
                              archived to docs/milestones/M<N>_*.md
                              at close)
       ↓ (deliver-agent picks one sub-sprint contract from milestone)
docs/sprint_objective.md  (current sub-sprint dev/review contract;
                           archived to docs/sprints/sprint-NNN-objective.md
                           at sub-sprint close)
```

R-items are the persistent backlog. Milestones are the planning
horizon. Sub-sprints are the execution unit. The dev session
consumes the sub-sprint contract; the review session consumes
either the sub-sprint or the milestone (per §4.3); the deliver-
agent + human consume all three layers.

### 8.7 Backwards compatibility

Pre-2026-05-16 sprints (Sprint 1 through Sprint 32) were planned
under the single-feature cadence and do not retroactively become
milestones. They remain in `docs/sprints/sprint-NNN-*` archives
unchanged. The milestone framework applies prospectively from
Milestone M1 onward (2026-05-16+).

A sprint started without an explicit milestone (e.g., a
single-feature follow-on between milestones) is allowed; it
defaults to "milestone-of-one" framing per §8.5 and follows
existing per-sprint conventions for Codex review, deliver-agent
close-out, etc.

**Commit-at-end bundling**: in commit-at-end workflows, dev working
trees accumulate uncommitted deliver-agent-owned files. Dev should stage
only authorized-scope files (not `git add -A`); deliver-agent files are
bundled by human at close commit. If bundled anyway, classify per
`docs/current/deliver_close_taxonomy.md` A-with-packaging-note.
