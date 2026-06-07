---
title: Milestone objective — TBD post-M-Auto-6 close (M-Auto-7 candidate selection pending)
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: docs/action_bank.md §5 (M-Auto-7 follow-up ledger) + docs/10-handoff.md §0 (current phase) + this placeholder
last_reviewed: 2026-06-07
review_cadence: replaced when next milestone is promoted
supersedes: docs/milestones/M-Auto-6_objective.md
superseded_by: null
notes: >
  PLACEHOLDER — no active milestone contract.

  M-Auto-6 closed 2026-06-07 (route-(b) accept-with-known-regression;
  Codex milestone-shared §4.3 verdict APPROVE_M_AUTO_6_WITH_NON_BLOCKING_OBSERVATIONS,
  blocking_count=0; archived at `docs/milestones/M-Auto-6_objective.md`
  + `docs/milestones/M-Auto-6_codex-review.md`). Baseline pointer
  flipped 2026-06-07 to
  `eval_interactive/results/m-auto-6-baseline-shared-20260607/`
  (`autoloop/config.yaml:baseline_dir` + `docs/current_eval_baseline.md`
  canonical section).

  M-Auto-7 scope is undecided. The candidate ledger lives at
  `docs/action_bank.md` §5 (M-Auto-7 candidates carried from M-Auto-6
  close + Codex NBOs). Top candidates by surface:

  - **Semantic OQ — `OQ-M6.uc-fp-resolve-vs-escalate-boundary`**
    (UC-FP resolve-vs-escalate; promotes OBS-S1 + OBS-S2; solution
    doc at `docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md`
    §3; layer `prompt_projection` + soft `semantic_planner`).
  - **Measurement-honesty — `R-controlkernel-default-resolved-on-close-anti误杀`**
    (CS1; ControlKernel.java:575-579 Path B default-stamps `resolved`
    on phase=CLOSE without grounding; small certain runtime fix;
    solution doc at `docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md`
    §3.1).
  - **Future enhancement — `R-user-role-projection-slot-from-listing-ownership`**
    (CS2 user_role projection slot; layer `prompt_projection`;
    same solution doc §3.2).
  - **Infra brief — `OQ-M6.empty-trace-and-session-start-flake`**
    (CS2-new admin UX + CS3 DISCOVER null-turn counter gating; layer
    `infra` + admin observability; Codex NBO #2 fences this at infra +
    soft projection guidance, NOT content/keyword detection).
  - **Grounding strengthening — `R-r5-citation-result-binding-grounding-strengthening`**
    (NEW from M-Auto-6 Codex NBO #1; bind R5 citation acceptance to
    the active `resolve_article` result; layer `infra` + grounding
    contract).
  - **Infra-test pickup — `R-standalone-reconcile-entry-gate-test`**
    (from S-Auto-28 NBO #1).
  - **Docs-only — `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source`**
    (from S-Auto-26 NBO #2).
  - **Carry-overs from M-Auto-5**: `OQ-S77.stall-detector-window`;
    `OQ-S77.goal-impossible-resolved-evidence`;
    `R-aggregate-retains-per-attempt-composite-l2`;
    `R-eval-interactive-judge-score-never-populated` (chronic LOW).
  - **Deferred**: M-Auto-4 S-Auto-18 escalation-family tier reshape;
    Cluster B.2 (`primary_uc` vs `active_use_case` authority) +
    Cluster C.1 (cs59s 400 session-create) — research-first sprints.

  **NEXT ACTION (human + deliver-agent)**: select M-Auto-7 scope
  (typically 3-5 candidates from above bundled around a shared
  acceptance bar per `process/milestone-framework.md` §8.1-§8.3),
  draft M-Auto-7 milestone contract here + first sub-sprint contract
  at `docs/sprint_objective.md`, generate dev/review prompts under
  `compact/`.

  `baseline_dir` = `eval_interactive/results/m-auto-6-baseline-shared-20260607`
  (flipped at M-Auto-6 close); `docs/current_eval_baseline.md` =
  M-Auto-6 canonical (M-Auto-5 demoted to "Previous canonical
  baseline"); Java baseline at close = `1358 / 1 / 0 / 2` (sole failure
  = inherited OQ-S41.5, provably uncoupled); UI baseline at close =
  `10 passed / 0 failed / 0 skipped`.
---

# Milestone objective — placeholder (M-Auto-7 candidate selection pending)

See front matter notes for the M-Auto-7 candidate ledger pointer + the
post-M-Auto-6 close state.

Active milestone contract: none (this placeholder).
Cold-start state: `docs/10-handoff.md` §0.
Candidate ledger: `docs/action_bank.md` §5.
Last closed milestone archive: `docs/milestones/M-Auto-6_objective.md`
+ `docs/milestones/M-Auto-6_codex-review.md`.
