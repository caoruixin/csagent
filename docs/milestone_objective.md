---
title: Milestone objective — placeholder (M2 closed; M3 candidate selection planning round pending)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: per milestone
supersedes: [docs/milestones/M2_objective.md]
superseded_by: null
notes: >
  Placeholder. NO active milestone at the time of this file. M2
  (Skill Registry Abstraction + Wholesale Retroactive Externalization,
  LLM-led Policy-bounded) closed **A — Clean PASS at the milestone
  level** 2026-05-18 (cumulative commit range `51c327c~..8cd0a10`; 7
  commits; 5 sub-sprints S37 → S41; archive at
  `docs/milestones/M2_objective.md` + `docs/milestones/M2_codex-review.md`).
  First clean A close at the milestone level under the NEW §8
  milestone framework. Milestone-shared cumulative Codex review per
  `iteration_governance.md` §4.3 second paragraph returned
  `decision: pass / blocking_count: 0` on first pass (single round);
  all C1-C5 Tier-0 candidate verdicts confirmed (C1 REJECTED, C2
  DEFER, C3 DEFER, C4+C5 NOT A CANDIDATE); no
  `runtime_freeze_and_risk_policy.md` edit warranted; 6 design-doc
  editorial fold-back items routed correctly to SEPARATE governance
  commit (deferred to deliver-agent + human discretion); 1 NEW
  non-blocking M3-cleanup candidate surfaced
  (`R-substitute-placeholders-uc-case-creation-eligibility-centralization`).

  **Next planning round: M3 candidate selection** per
  `iteration_governance.md` §8.4 + the M2 archive §12.7 cross-
  milestone sequencing options. Deliver-agent + human (and possibly
  research-agents for novel M3 surfaces) decide which M3 candidate
  to pursue. Candidate slate:

  - **M3-A**: Customer-honesty surface (sibling rationale/confidence
    fields on `request_handover`; handover human-message UX rewrite;
    URL policy + factual narrowing extension). Rides on validated
    M2 Skill abstraction.
  - **M3-B**: Lifecycle (`D-single-handover-orchestrator` P0 launch
    blocker + scoped `D-full-issue-ledger` with explicit new-objective
    approval if granted + semantic planner shadow mode if scoped).
  - **M3-C**: Sprint 5 S3/S4/S5 skills (Soft-OOS triage / Account
    login-recovery / Tier-2-reason UC-compat) directly on the
    validated M2 Skill abstraction (new Skill YAML files; minimal
    infra change).
  - **M3-D**: Topic↔UC binding loosening
    (`R-loosen-topic-uc-binding-llm-owned-drift`; requires research-
    agent investigation round before scoping).
  - **M3-Latency** (or M3-Infra): consume
    `R-llm-provider-latency-drift-2026-05-16` deferred from M2.
    Likely milestone-of-one per §8.5.
  - **M3-Skill-Tuning**: post-M2 behavioural tuning of individual
    Skill `procedure` / `guardrails` content (e.g., Alice closure-
    criterion (a) PASS if not achieved organically through Skill
    abstraction landing; UC-switch specific scenarios). Localized
    Skill edits on validated abstraction.
  - **M3-Tier-0 re-evaluation**: C2 + C3 candidate re-evaluation if
    production trace evidence has accumulated post-M2 (Sprint 41 +
    M2-close Codex DEFER verdicts; re-evaluation flagged at M3+).
  - **M3-cleanup**: `R-skill-tool-name-canonical-source-centralization`
    (Sprint 38 fix-iteration #1) + NEW M2-close-surfaced
    `R-substitute-placeholders-uc-case-creation-eligibility-centralization`.

  **Selection criteria** at the M3 planning round:
  - Which M3 surfaces have research-agent investigation completed
    (esp. M3-D Topic↔UC binding loosening which requires research
    before scoping).
  - Salesforce cutover calendar pressure (would flip M3-B to highest
    priority per `docs/release_gate.md` §1.1).
  - Bad-case suite state (Alice observation results post-M2).
  - Human priorities.
  - Whether C2 or C3 production trace evidence has accumulated enough
    for Tier-0 elevation.
  - Whether any Path 2 real-session bad case has surfaced during M2
    close window that warrants emergency triage.

  Deliver-agent does NOT pre-decide M3 here.

  **Until M3 candidate is locked and M3 milestone objective drafted:**
  - Live `docs/milestone_objective.md` carries this M2-close-pending
    placeholder.
  - Live `docs/sprint_objective.md` carries an M3-candidate-selection-
    pending placeholder (no active sub-sprint until first M3 sub-
    sprint drafted).
  - Dev agent should NOT launch from these placeholders (no
    actionable scope; these are for human reference only).

  **Deferred governance commit (separate from this M2-close archive
  bundle)** per `doc_governance.md` cadence — design-doc editorial
  fold-back queue (6 items confirmed by Codex M2-shared review §7):
  1. Sprint 37 design doc §7.8 Sprint 31/33 line-number swap typo at
     `docs/proposals/skill_registry_design.md:2015`.
  2. Sprint 38 OQ-S38.1 template-vs-legacy editorial divergence on
     DISCOVER Skill body.
  3. Sprint 39 §10 design doc §6.2.5/§6.2.6 RESOLVE template
     editorial divergence.
  4. Sprint 39 OQ-S39.7 stale Sprint 38 test name
     `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` rename.
  5. NEW Sprint 41 OQ-S41.1 one-line `system_prompt.txt` shell
     pointer for `prior_use_case_carry` (`phase_plan` projection
     currently omits `state_inheritance.soft_signal_via_projection`).
  6. NEW Sprint 41 OQ-S41.4 `accumulated_tool_results` inheritance
     wording at design doc §10.5 row 1.

  This fold-back is a SEPARATE governance commit; not bundled in the
  M2-close archive commit.
---

# No active milestone — M3 candidate selection planning round pending

**Current state (2026-05-18):**
- NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) closed **A — Clean PASS at the milestone level** 2026-05-18.
- Milestone archive: `docs/milestones/M2_objective.md` (the immutable contract + §12 closure verdict appended at close).
- Codex milestone-shared review archive: `docs/milestones/M2_codex-review.md` (`decision: pass / blocking_count: 0` first pass; cumulative range `51c327c~..8cd0a10`).
- Live `docs/codex-findings.md` reset to scaffold (ready for the next sub-sprint or milestone Codex review pass).
- 5 sub-sprints S37 → S41 all closed; per-sub-sprint Codex review verdicts: A → B-fix-iterated → A → A → A.
- Cumulative metrics: 7 commits + 79 files + 13162 insertions + 927 deletions + 161 new Java tests (983 → 1144).

## What this file is

A placeholder for the live `docs/milestone_objective.md`. No active milestone scope until deliver-agent + human draft the M3 milestone objective at the M3 planning round (which is the next deliver-agent unit of work).

## Next deliver-agent unit of work

**M3 candidate selection planning round.** Per the workflow inputs framing in the deliver-agent role definition:
- **Path 1** (research-driven): human picks M3 candidate from the slate above + provides research-agent proposal; deliver-agent drafts NEW `docs/milestone_objective.md` for M3 + first M3 `docs/sprint_objective.md`.
- **Path 2** (bad-case-driven): a real-session bad case surfaces during/after M2 close; deliver-agent + human triage per `iteration_governance.md` §5.6 + 4-route fit decision; routes to existing M3 candidate OR new M3 milestone OR Tier-0 emergency.

## Reading order on cold start

If a new deliver-agent instance picks up after M3 planning has progressed:
1. Verify `docs/codex-findings.md` state (scaffold vs filled).
2. Read this file (placeholder; no scope).
3. Read `docs/milestones/M2_objective.md` §12 closure verdict (M2 final classification + Tier-0 dispositions + R-item flips + design-doc fold-back queue + M3 candidate context).
4. Read `docs/milestones/M2_codex-review.md` (verbatim Codex M2-shared verdict).
5. Read `docs/action_bank.md` §6.5 closed-milestone index for the M2 close-action row + §5 deferred candidates (including NEW M2-close-surfaced R-items).
6. Read `docs/10-handoff.md` §1 lead for the current state (M2 → Preceding milestone; M3 → Current placeholder).
