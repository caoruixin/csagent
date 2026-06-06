---
title: Sprint objective — TBD post M-Auto-6 close
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: docs/milestone_objective.md (M-Auto-6 acceptance bar) + docs/10-handoff.md §0 (cold-start table) + this placeholder
last_reviewed: 2026-06-06
review_cadence: replaced when next sub-sprint or milestone is promoted
supersedes: docs/sprints/sprint-082-objective.md
superseded_by: null
notes: >
  PLACEHOLDER — no active sub-sprint contract.

  Milestone M-Auto-6 (Runtime substrate hygiene + admin observability +
  intake/clarification contract + UX/corpus governance) is **ACTIVE**.
  All 5 sub-sprints are DEV-SIDE CLOSED as of 2026-06-06:
  - A (S-Auto-23 / Sprint 078; R1.a + R2.a + R4.a) — Codex
    `APPROVE_S_AUTO_23 / blocking_count=0`.
  - B (S-Auto-24 / Sprint 079; R3.a + R3.b + R3.c admin trace
    observability) — visual-verified; §7-EXEMPT.
  - C-1 (S-Auto-25 / Sprint 080; R7 + R2.a#5-ext) — Codex
    `APPROVE_S_AUTO_25 / blocking_count=0`; capability-wiring
    Option-A fence-waiver ACCEPTED.
  - C-2a (S-Auto-26 / Sprint 081; R5 citation contract fix) — Codex
    `APPROVE_S_AUTO_26 / blocking_count=0` on targeted re-review.
  - C-2b (S-Auto-27 / Sprint 082; R6 corpus eligibility filter) —
    Codex `APPROVE_S_AUTO_27 / blocking_count=0` on targeted
    re-review (both prior P0 blockers — F2 second direct-resolve
    test + F3 V17 SQL comment forbidden-grep — resolved by fix
    iteration commits `e6aad78` + `d27b824` + `4c8931f`).

  **Next action**: M-Auto-6 milestone-shared §9 real-LLM re-bless
  launches (deliver-agent + human). Paired-evidence against
  `eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix`
  per the M-Auto-6 acceptance bar (`docs/milestone_objective.md` §5).
  The milestone-shared Codex review prompt
  `compact/M-Auto-6-review-prompt.md` is authored at this close
  commit (covering the A + B + C-1 + C-2a + C-2b cumulative range
  per `process/milestone-framework.md` §4.3); dispatched after the
  re-bless evidence is in.

  Next sub-sprint contract is **deferred** until M-Auto-6 closes.
  Candidates per `docs/milestone_objective.md` §3 (S-Auto-28+):
  - Cluster B.2 (`primary_uc` vs `active_use_case` authority decision
    — needs research-agent sub-sprint).
  - Cluster C.1 (cs59s session_create 400 — research-first short).
  - `R-aggregate-retains-per-attempt-composite-l2` (from M-Auto-5
    Codex non-blocking observation #3).
  - Autoloop semantic sub-sprint targeting OBS-S1 (UC-A / UC-H /
    UC-J verify-entity-context) / OBS-S2 (DISCOVER disambiguation) /
    OBS-S6 (intake too literal — gated on R7 ship, now landed) /
    OBS-S7 (escalate-without-summary).
  - Post-M-Auto-6 OBS-only backlog from S-Auto-24 close
    (OQ-S79.2 single-session demo endpoint; OQ-S79.3 per-call
    timestamps).
  - S-Auto-25 / S-Auto-26 non-blocking observations queued for
    S-Auto-28+.
  - `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source`
    (NBO #2 from S-Auto-26 Codex; post-M-Auto-6 docs-only sprint).
  - M-Auto-4 S-Auto-18 (escalation-family tier reshape; deferred
    to M-Auto-7+).

  Decision deferred to the M-Auto-6 close. Do NOT promote a next
  sub-sprint until:
  1. The milestone-shared §9 real-LLM re-bless launches and
     paired-evidence is reviewed.
  2. The milestone-shared Codex review at
     `compact/M-Auto-6-review-prompt.md` returns a verdict.
  3. The human + deliver-agent classify M-Auto-6 close route per
     `docs/milestone_objective.md` §5 routes (a) prediction holds /
     (b) prediction fails / (c) anti-误杀 violation.

  `baseline_dir` UNCHANGED (`m-auto-5-baseline-20260604-simfixed-stalledfix`)
  and `docs/current_eval_baseline.md` UNCHANGED until the
  milestone-shared re-bless lands at M-Auto-6 close.
---

# Sprint objective — placeholder (no active sub-sprint)

See front matter notes for the M-Auto-6 milestone status, the next
action (milestone-shared §9 real-LLM re-bless launch), and the
S-Auto-28+ candidate list deferred to M-Auto-6 close.

Active milestone contract: `docs/milestone_objective.md`.
Cold-start state: `docs/10-handoff.md` §0.
