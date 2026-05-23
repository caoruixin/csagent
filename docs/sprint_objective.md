---
title: Sprint planning placeholder — S-Cleanup-3 pending Tier-2 design memo decision
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (placeholder; replaced when S-Cleanup-3 contract is authored)
last_reviewed: 2026-05-23
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-048-objective.md]
superseded_by: null
notes: >
  S-Cleanup-2 (Sprint 48) CLOSED 2026-05-23 A — Clean PASS (dev commit
  `b989833`; deliver-agent + human spot-check). Archived to
  `docs/sprints/sprint-048-objective.md` + `docs/sprints/sprint-048-handoff.md`.

  This file is a placeholder pending the S-Cleanup-3 contract. S-Cleanup-3
  is the THIRD (and per current milestone plan, LAST) M4-Eval-Cleanup
  sub-sprint: governance alignment — #4 handover_completeness /
  case_id_present demotion + #9 Tier-2 design decision. Per deliver-agent
  + human direction 2026-05-23, the #9 Tier-2 design decision (full-Skill
  traversal vs runtime-selected vs hybrid) requires a design memo at
  `docs/solutions/tier2_skill_traversal_design_memo.md` and a human a/b/c
  decision BEFORE the S-Cleanup-3 contract is drafted. The memo enumerates
  the options + trade-offs; the human decides; the deliver-agent then
  replaces this placeholder with the full S-Cleanup-3 contract (the
  Sprint 47/48 12-section shape) with the locked #9 decision + #4 scope.
---

# Sprint planning placeholder — S-Cleanup-3 pending Tier-2 design memo decision

This file is intentionally a placeholder between S-Cleanup-2 close and
S-Cleanup-3 launch.

**Status**: no active sub-sprint contract. S-Cleanup-2 CLOSED 2026-05-23
A — Clean PASS. S-Cleanup-3 contract pending:

1. **Tier-2 design memo** at `docs/solutions/tier2_skill_traversal_design_memo.md`
   (deliver-agent authored; enumerates #9 options a/b/c + trade-offs).
2. **Human a/b/c decision** on the Tier-2 wiring (keep full-Skill
   traversal + document / switch to runtime-selected Skill / hybrid).
3. **Deliver-agent drafts S-Cleanup-3 contract** here (REPLACES this
   placeholder) with the locked #9 decision + #4 handover_completeness /
   case_id_present demotion scope.

## S-Cleanup-3 preliminary scope (per `docs/milestone_objective.md` §3 S3)

- **#4 handover_completeness / case_id_present demotion** — from
  mandatory-on-escalate (`composite.py:81,84` in `_conditional_mandatory_l2`)
  to advisory Tier-3 per M3-Eval four-tier pyramid intent. Code change +
  possibly a §5.6 governance text clarification (Codex must verify if
  governance text is touched). Layer: `eval_spec` + possibly governance-
  touch.
- **#9 Tier-2 design decision** — `executor.py:_compute_tier2_result`
  iterates `ext.skills_by_name` (all loaded Skills) rather than the
  runtime-selected Skill, relying on `mandatory_for` UC matching to filter.
  Options (per `docs/milestone_objective.md` §3 S3): (a) keep current
  full-Skill traversal + document design rationale, (b) switch to runtime-
  selected Skill (out-of-scope for cleanup if it requires runtime touches),
  (c) hybrid (current traversal + warn if no runtime Skill matches). Layer:
  `judge_calibration`. **NOT LOCKED — pending memo + human decision.**

## Reference

- S-Cleanup-2 archive: `docs/sprints/sprint-048-objective.md` + `docs/sprints/sprint-048-handoff.md`.
- Milestone: `docs/milestone_objective.md` (M4-Eval-Cleanup) §3 S3 paragraph + §6 hard fences + §10 stop conditions.
- Tier-2 memo (when authored): `docs/solutions/tier2_skill_traversal_design_memo.md`.
- Action bank: `docs/action_bank.md` §5 + §6 (Sprint 48 row).
