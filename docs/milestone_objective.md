---
title: (no active milestone — M-Auto-6 planning)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: docs/milestones/M-Auto-5_objective.md (preceding milestone, CLOSED 2026-06-05)
last_reviewed: 2026-06-05
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty — CLOSED
  2026-06-05 (APPROVE_WITH_NON_BLOCKING_OBSERVATIONS; Codex blocking_count=0;
  §5.9 sweep 0/414; paired-evidence 10 F→P / 0 P→F). Archive:
  `docs/milestones/M-Auto-5_{objective,codex-review}.md`. `baseline_dir`
  flipped to `m-auto-5-baseline-20260604-simfixed-stalledfix`;
  `docs/current_eval_baseline.md` updated.

  M-Auto-6 candidate scope (queued — not yet promoted; awaits human
  authorization to draft milestone_objective contract):

    - **R1** — `request_handover` intake-UC tool schema missing
      `intake_fields` → first-call rejection + retry (layer
      `prompt_projection`).
    - **R2** — `BudgetChecker.maxClarificationRounds` dead-code on the live
      `AgentRunLoopImpl` path → DISCOVER no clarification-budget cap (layer
      `infra` / `skill_state`).
    - **Cluster B** (eval/observability debt from the 2026-06-04 audit):
      B.1 `per_turn_trace` 30–50 % truncation; B.2 `primary_uc` vs
      `active_use_case` 40–60 % mismatch + authority undecided. Bundle
      candidate for 2× parallel research-agent dispatch.
    - **Cluster C** (live UI / runtime infra from the 2026-06-04 audit):
      C.1 cs59s `session_create_failed:400`; C.2 46f5b2e9 500 +
      double-send; C.3 generic-clarifier intake-branch wasting opening
      turn.
    - **R-aggregate-retains-per-attempt-composite-l2** (new R-item opened
      from M-Auto-5 Codex non-blocking observation #3): aggregate attempt
      rows lack `composite_score` + `l2_results`; future §5.9 sweeps and
      close reviews cannot independently reproduce per-attempt predicates
      from compact aggregate alone. Candidate for the Cluster B
      observability sub-sprint.

  Human direction 2026-06-05 (recorded at M-Auto-5 close): M-Auto-6 first
  sub-sprint bundles R1 + R2 (small intake-UC unblock for the falsifiable
  `reducible-flaky` prediction); after R1 + R2 re-evaluate flaky count
  before opening the Cluster B + C 2× research-agent dispatch. Research
  proposal input: `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`.

  M-Auto-4's S-Auto-18 (escalation-family tier reshape) remains DEFERRED
  behind M-Auto-6 (to M-Auto-7 or later).

  Forbidden until M-Auto-6 is promoted: editing this file's `title` away
  from "no active milestone" without the corresponding `sprint_objective.md`
  draft + human approval per `deliver-agent.md` §Milestone 开始前.
---

# (no active milestone)

The last closed milestone is **M-Auto-5 — Eval Verdict Correctness +
Trace-Contract Honesty** (2026-06-05; APPROVE_WITH_NON_BLOCKING_OBSERVATIONS).
See `docs/milestones/M-Auto-5_objective.md` for the closed contract and
`docs/milestones/M-Auto-5_codex-review.md` for the Codex verdict.

M-Auto-6 planning context is recorded in the YAML notes above. The next
action is **human authorization to draft M-Auto-6 milestone_objective +
first sub-sprint contract (R1 + R2 bundle)**.
