---
title: Milestone objective — next milestone TBD (M5+ candidate selection pending)
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-24
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Placeholder. Milestone M4-Eval-Cleanup closed 2026-05-24 (A — Clean PASS);
  archived to docs/milestones/M4-Eval-Cleanup_objective.md (with the §12 closure
  verdict + the M5+ candidate slate at §12.10). No active milestone. The next
  milestone is selected by the deliver-agent + human at the next planning round.
---

# Milestone objective — next milestone TBD

**No active milestone.** Milestone M4-Eval-Cleanup closed 2026-05-24 (A — Clean
PASS); see `docs/milestones/M4-Eval-Cleanup_objective.md` §12 for the closure
verdict and `docs/10-handoff.md` §0/§1 for current state.

The next milestone is chosen by the deliver-agent + human at the next planning
round from the M5+ candidate slate (`docs/milestones/M4-Eval-Cleanup_objective.md`
§12.10, carried from M3-Eval §12.10):

1. **M3-B Single Handover Orchestrator** — `docs/release_gate.md` §1.1 release-gate blocker.
2. **UC-G/H/I/J bad-case seeding** — `R-bad-case-suite-uc-ghij-seed-from-real-sessions`; requires real-session source material.
3. **Semantic-planner soft-signal extension** — `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` + per-UC moderation-context projection.
4. **M3-Corpus** — separate parallel-track; `R-corpus-coverage-audit-per-uc`.
5. **Latency / Skill-Tuning / Tier-0 re-evaluation** — preceding candidates remain in slate.

Per the `iteration_governance.md` §8 framework, milestone planning assembles 3-5
related R-items from `docs/action_bank.md` into a single architectural theme with
a bad-case-anchored (or, for cleanup milestones, regression-safety) acceptance
bar, then the deliver-agent drafts the first sub-sprint contract in
`docs/sprint_objective.md`.
