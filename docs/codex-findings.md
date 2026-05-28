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

- Last archived: `docs/milestones/M-Auto-1A_codex-review.md` (M-Auto-1A — Auto-Evolution Build close 2026-05-28; cumulative `1fb2062..b6b627b`; per-sub-sprint S-Auto-4 + milestone-shared review both `pass / 0`, sub-classified `approve with downgrade-to-signal follow-up` with only follow-up trigger = `R-S57-anti-hardcode-whenever-arrow-synonym-bypass`).
- Next review target: TBD (M-Auto-1B candidate selection pending; deliver-agent + human Path 1 research-driven planning round per `docs/teams/deliver-agent.md`).

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
