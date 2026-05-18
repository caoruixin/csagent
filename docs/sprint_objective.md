---
title: Sprint objective — placeholder (M2 milestone close planning round pending; no active sub-sprint)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: per round
supersedes: [docs/sprints/sprint-041-objective.md]
superseded_by: null
notes: >
  Placeholder. NO active sub-sprint at the time of this file. Sprint 41
  (NEW M2 sub-sprint 5; LAST M2 implementation sub-sprint) closed
  A — Clean PASS 2026-05-18 (dev commit `8cd0a10`; Codex per-sub-sprint
  review `pass / blocking_count: 0` first pass; archive at
  `docs/sprints/sprint-041-objective.md` + `docs/sprints/sprint-041-handoff.md`
  + `docs/sprints/sprint-041-codex-review.md`). Sprint 41 close closes
  the M2 implementation track.

  **Next planning round: M2 milestone close** per
  `iteration_governance.md` §8.4. Deliver-agent + human dispatch the
  milestone-shared cumulative Codex review per §4.3 second paragraph
  via `compact/M2-review-prompt.md` against the M2 commit range
  `51c327c..8cd0a10` (NEW M2 sub-sprints 1-5 inclusive; 7 commits).

  After Codex M2-shared review returns:
  (a) Deliver-agent + human classify the M2-shared verdict (PASS / FIX
      REQUIRED / OOSR).
  (b) Design doc editorial fold-back per `doc_governance.md` cadence
      (6 fold-back items queued at Sprint 41 close):
      1. Sprint 37 design doc §7.8 Sprint 31/33 line-number swap typo
         (OQ-S40.5).
      2. Sprint 38 OQ-S38.1 template-vs-legacy DISCOVER divergence.
      3. Sprint 39 §10 design doc §6.2.5/§6.2.6 RESOLVE template
         divergence.
      4. Sprint 39 OQ-S39.7 stale test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`.
      5. NEW Sprint 41 OQ-S41.1 one-line shell pointer for
         `prior_use_case_carry` in `system_prompt.txt` (Codex
         DISAGREE-IN-PART NON-BLOCKING; partial code-untruth in dev's
         D-g skip rationale).
      6. NEW Sprint 41 OQ-S41.4 `accumulated_tool_results` inheritance
         wording (design doc §10.5 row 1 "All resolve-side Skills" vs
         Sprint 41 all-six framing).
  (c) C2 + C3 Tier-0 candidate independent re-evaluation with full M2
      evidence per `feedback_constitution_discipline_vs_planning_anticipation.md`
      (Codex independent verdict + deliver-agent + human authorization
      if elevation warranted).
  (d) Update `docs/milestone_objective.md` closure verdict per §8.4;
      archive to `docs/milestones/M2_objective.md` (status: archived).
  (e) Append §6.5 "Closed milestone index" row to `docs/action_bank.md`.
  (f) Refresh `docs/10-handoff.md` §1 lead (M2 → Preceding milestone;
      M3 → Current).
  (g) Help human pick M3 candidate per `docs/milestone_objective.md` §11
      cross-milestone sequencing:
      - M3-A: Customer-honesty surface (sibling rationale/confidence
        fields on `request_handover`; handover UX rewrite; URL policy).
      - M3-B: Lifecycle (`D-single-handover-orchestrator` P0 + scoped
        `D-full-issue-ledger`).
      - M3-C: Sprint 5 S3/S4/S5 skills (rides validated M2 Skill
        abstraction).
      - M3-D: Topic↔UC binding loosening (`R-loosen-topic-uc-binding-llm-owned-drift`
        requires research-agent investigation).
      - M3-Latency: consume `R-llm-provider-latency-drift-2026-05-16`.
      - M3-Skill-Tuning: post-M2 behavioural tuning of individual
        Skills (e.g., Alice closure-criterion if not achieved organically
        through abstraction landing).
      - M3-Tier-0 re-evaluation: C2 + C3 candidate re-evaluation if
        deferred at M2 close.
  (h) Once M3 candidate locked, draft `docs/milestone_objective.md` for
      M3 + first M3 sub-sprint `docs/sprint_objective.md` (the latter
      REPLACES this placeholder file).

  Until M2 close completes and an M3 sub-sprint is drafted: deliver-agent
  + human are between sub-sprints. Dev agent should NOT launch from this
  file (no actionable scope; this placeholder is for human reference
  only).
---

# No active sub-sprint — M2 milestone close planning round pending

**Current state (2026-05-18):**
- NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) implementation track CLOSED at Sprint 41 close.
- 5 sub-sprints (S37 → S41) all closed; per-sub-sprint Codex review verdicts: A → B-fix-iterated → A → A → A (S38 closed B-fix-iterated on a single SkillLoader schema-completeness blocking finding; S37/S39/S40/S41 closed A — Clean PASS first pass).
- M2 milestone-shared cumulative Codex review prompt drafted at `compact/M2-review-prompt.md` against commit range `51c327c..8cd0a10` (7 commits).
- Deliver-agent + human pending: human dispatches Codex via `compact/M2-review-prompt.md`; Codex writes to `docs/codex-findings.md` per §4.2 sprint-close header convention.

## What this file is

A placeholder for the live `docs/sprint_objective.md`. No active sub-sprint scope until deliver-agent + human draft the first M3 sub-sprint contract at M3 planning round (which is gated on M2 close per §8.4).

## Next deliver-agent unit of work

Per the workflow inputs framing in the deliver-agent role definition:
- **Path 1 input** (research-driven): human gives M3 candidate + research-agent proposal; deliver-agent drafts M3 `docs/milestone_objective.md` + first M3 `docs/sprint_objective.md`.
- **Path 2 input** (bad-case-driven): a real-session bad case surfaces during/after M2 close; deliver-agent + human triage per `iteration_governance.md` §5.6 + 4-route fit decision; routes to existing M3 candidate OR new M3 milestone OR Tier-0 emergency.

## Reading order on cold start

If a new deliver-agent instance picks up after M2 close has progressed:
1. Verify `docs/codex-findings.md` state (scaffold vs filled by M2-shared Codex).
2. Read `docs/milestone_objective.md` §12 closure verdict (filled after M2 close decision).
3. Read `compact/M2-review-prompt.md` for the dispatch reference.
4. Read this file (placeholder; no scope).
5. Read `docs/sprints/sprint-041-handoff.md` §12 closure verdict (Sprint 41 close = A — Clean PASS; M2 implementation track CLOSED).
