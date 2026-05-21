---
title: S-Eval-2 contract pending — post-S-Eval-1-close placeholder
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (until S-Eval-2 contract is drafted, then this placeholder is replaced)
last_reviewed: 2026-05-21
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-042-objective.md]
superseded_by: null
notes: >
  S-Eval-1 (Sprint 42) closed Clean PASS 2026-05-21 (dev commit
  `d91bd3d`; deliver-agent + human classification: A — Clean PASS;
  archive at `docs/sprints/sprint-042-objective.md` + `docs/sprints/sprint-042-handoff.md`).
  S-Eval-2 is the next sub-sprint per `docs/milestone_objective.md` §3
  (Skill `critical_steps` schema + extractor + projection wiring;
  multi-layer `eval_spec` + `prompt_projection`; Codex milestone-shared
  per §4.3 default; estimated 4-5 dev-days).

  This file is a PLACEHOLDER until the deliver-agent + human draft the
  S-Eval-2 sub-sprint contract at the next planning round. At that
  point this file is REPLACED with the full S-Eval-2 contract (12
  sections per Sprint 35 / 41 / 42 shape).

  **S-Eval-1 close carry-over (per `docs/sprints/sprint-042-handoff.md`
  §12.7; LOAD-BEARING for S-Eval-3 / S-Eval-4 planning, not S-Eval-2)**:
  the per-UC anchor distribution observation surfaced systematic
  under-representation of the intake-then-escalate UCs (UC-G / UC-H /
  UC-I / UC-J have ZERO anchor coverage; UC-F has 1 case; UC-B has 5
  cases). At S-Eval-3 planning the deliver-agent + dev should be aware
  that any UC-F or UC-B `critical_step` will exercise against a thin
  corpus. At S-Eval-4 planning, prioritise UC-G / UC-H / UC-I / UC-J
  entries from the 17 approved overrides to compensate for the gap.
  M3-Corpus (parallel-track milestone) is the right home for a
  structural fix; no separate R-item opened at S-Eval-1 close.

  **S-Eval-1 open question routed to M3-Eval milestone-shared Codex
  review**: OQ-S42.3 (D-2.2 demotion interpretation — whether tagging
  `_check_escalation_reason_consistency` advisory when
  `expected.escalation_trigger is None` is the right surface, given
  `_check_escalation_compliance` already short-circuits the family-match
  in that condition) is a confirmation item at M3-Eval close, not a
  blocker for S-Eval-2 start.
---

# S-Eval-2 contract pending — post-S-Eval-1-close placeholder

This file is a placeholder. The deliver-agent + human draft the
S-Eval-2 sub-sprint contract at the next planning round; on draft, this
placeholder is REPLACED with the full S-Eval-2 contract (12-section
template per Sprint 35 / 41 / 42 precedent).

For the M3-Eval milestone context S-Eval-2 sits inside, see
`docs/milestone_objective.md` §3 (sub-sprint sequence, S-Eval-2 row).
