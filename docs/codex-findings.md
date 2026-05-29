# Codex Findings — Live Scaffold

This file is the live working surface for the **current** Codex review.
Per `docs/current/iteration_governance.md` §4.2 and §4.3, it carries
either a per-sub-sprint or a milestone-shared review depending on the
active milestone's Codex review plan.

At milestone close, `docs/teams/deliver-agent.md` "Close 时维护操作"
prescribes `git mv docs/codex-findings.md → docs/milestones/M<N>_codex-review.md`
and a reset of this file back to the scaffold form below. The previously
archived milestone-shared Codex review lives at the destination path; do
not edit it after the archive.

---

## Status

**No active Codex review.**

- Last archived: `docs/milestones/M-Auto-1B_codex-review.md` (M-Auto-1B — Auto-Evolution Calibration close 2026-05-30; cumulative `b6b627b..<M-Auto-1B Phase 3 close commit>`; bundles all 3 reviews per Codex Axis M10 trigger #2 close-package consolidation — per-sub-sprint S-Auto-5 2026-05-28 + per-sub-sprint S-Auto-7 2026-05-29 + milestone-shared M-Auto-1B 2026-05-30. All 3 verdicts `decision: pass / blocking_count: 0` sub-classified `approve with downgrade-to-signal follow-up`. M-Auto-1B closure verdict A-with-acceptance-bar-revision; 3 non-blocking downgrade triggers dispositioned at `docs/milestones/M-Auto-1B_objective.md` §12.14.).
- Next review target: M-Auto-1C — Auto-Evolution Calibration Continuation (`docs/milestone_objective.md` live; 2 sub-sprints S-Auto-7.2 / Sprint 062 applier.py:370 mvn module-selection fix + S-Auto-8 / Sprint 063 first overnight + first cherry-pick to main on the now-validated substrate). S-Auto-7.2 Codex plan = DEFAULT milestone-shared at M-Auto-1C close (no §4.3 trigger expected — 1-line applier change isn't fenced); S-Auto-8 conditional per-sub-sprint per §4.3 trigger if cherry-pick candidate borderline-§5.3 surfaces.

## Expected output format

When a per-sub-sprint or milestone-shared review begins, the review
agent (Codex) appends a verdict header at the top of this file using
the §4.2 four-line convention:

```
## Sprint Review Decision     # or "Milestone-Shared Review Decision"
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>
```

…followed by the §4.1 nine-question kernel walk + per-axis evidence
+ verdict per the §4.1 verdict set (`approve` / `approve with
downgrade-to-signal follow-up` / `reject as semantic hardcode` /
`needs human architecture decision`).

Per-sub-sprint review prompts live at `compact/sprint-NNN-codex-review-prompt.md`;
milestone-shared review prompts live at `compact/M<N>-review-prompt.md`.
Both prompts are self-contained per `iteration_governance.md` §9
invariant and are dispatched by the human to a fresh Codex session.
