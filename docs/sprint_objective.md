---
title: Sprint objective — TBD (M-Auto-1B closure pending milestone-shared Codex review; M-Auto-1C draft at Phase 3)
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: docs/milestone_objective.md §12 closure verdict draft + this placeholder
last_reviewed: 2026-05-29
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-061-objective.md]
superseded_by: null
notes: >
  Placeholder during the M-Auto-1B close → M-Auto-1C open transition.
  S-Auto-7.1 / Sprint 061 closed 2026-05-29 A — Clean PASS at the
  sub-sprint level; archived to `docs/sprints/sprint-061-objective.md`.
  M-Auto-1B is closing via the human-locked §8.5 split decision (5
  sub-sprints ceiling reached; OQ-S61.1 applier.py:370 mvn module-
  selection bug surfaced as the substrate blocker preventing Goal #4
  smoke iter through Step 9).

  Sequence:
  - **Phase 1 (this commit)**: deliver-agent close-bundle artefacts
    (this placeholder + 10-handoff §0/§1 refresh + action_bank §6
    Sprint 61 close-action row + milestone_objective.md §12 closure
    verdict DRAFT with sub-sprint dispositions + acceptance-bar audit
    + §8.5 split decision rationale; Codex outcome blank).
    Milestone-shared Codex prompt authored at
    `compact/M-Auto-1B-review-prompt.md` (self-contained per §9).
  - **Phase 2 (human + deliver-agent jointly, before Codex dispatch)**:
    restart foreground :8080 backend; run §5.6 bad-case suite +
    shadow regression-safety rerun; record evidence (run-ids + per-
    suite case_passed counts + per-case PASS/FAIL/IMPROVING joint
    judgment) into milestone_objective.md §12 §5.6 + shadow
    sections; small follow-up commit; dispatch
    `compact/M-Auto-1B-review-prompt.md` to Codex.
  - **Phase 3 (after Codex returns)**: finalize milestone_objective
    §12 closure verdict with Codex outcome; archive milestone_objective
    → milestones/M-Auto-1B_objective.md; archive codex-findings →
    milestones/M-Auto-1B_codex-review.md; reset live codex-findings
    to scaffold; write NEW live milestone_objective.md = M-Auto-1C
    "Auto-Evolution Calibration Continuation"; write NEW live
    sprint_objective.md = S-Auto-7.2 / Sprint 062 contract (applier.py:370
    mvn module-selection fix); write `compact/sprint-062-dev-prompt.md`;
    refresh 10-handoff §0/§1/§2 with milestone close lead + archive
    index row; refresh action_bank §6.5 M-Auto-1B closed-milestone
    row + R-item flips (R-S57 close confirmed; R-S58 carry to
    M-Auto-1C / Fix-D candidate).

  M-Auto-1C planned scope (2 sub-sprints; well within §8.5 ceiling):
  - **S-Auto-7.2 / Sprint 062 — applier.py:370 mvn module-selection
    fix** (`infra`; §7 EXEMPT; ~half-day; 1-line diff + 1-2 tests).
    Root cause per S-Auto-7.1 handoff §6: `mvn -q -pl server -am
    spring-boot:run` applies the spring-boot:run goal to csagent-
    parent (packaging=pom, no main class) because `-pl server -am`
    selects parent into the reactor. Fix options enumerated in
    handoff §6: (1) drop `-am`; (2) invoke from server submodule
    cwd; (3) spring-boot plugin scope. Smoke iter completion is
    the close gate.
  - **S-Auto-8 / Sprint 063 — first overnight batch + first human
    review + first cherry-pick to main** (`eval_spec`; §7 REQUIRED;
    ~3-5 days). Original deferred S-Auto-6 scope; reattempted post-
    S-Auto-7.2 with the now-validated substrate. R-S58 zero-width
    bypass surface check (S-Auto-6 Scope #5 deferred) lands here +
    final R-S58 disposition (defer-to-M-Auto-2 OR extend-with-S-Auto-9-
    Fix-D) decided at M-Auto-1C close.
---

# Sprint objective — placeholder during M-Auto-1B close → M-Auto-1C open transition

This file is a placeholder. The S-Auto-7.1 / Sprint 061 contract was archived to `docs/sprints/sprint-061-objective.md` at S-Auto-7.1 close 2026-05-29.

The next live `docs/sprint_objective.md` (M-Auto-1C's first sub-sprint S-Auto-7.2 / Sprint 062) will be authored at **Phase 3** of the M-Auto-1B close-bundle, AFTER milestone-shared Codex review returns. See the notes block above for the full 3-phase sequence and M-Auto-1C planned scope.

## Pointer

- **Closing milestone (M-Auto-1B)**: `docs/milestone_objective.md` (live; will archive in Phase 3).
- **Milestone-shared Codex review prompt**: `compact/M-Auto-1B-review-prompt.md` (Phase 1 deliverable; dispatched to Codex in Phase 2 after §5.6 + shadow rerun evidence collection).
- **Closing sub-sprint (S-Auto-7.1)**: `docs/sprints/sprint-061-objective.md` (archive) + `docs/sprints/sprint-061-handoff.md` (dev handoff committed `19213b1`).
- **Next milestone planning notes**: `docs/milestone_objective.md` §12 closure verdict draft + this `notes:` block above name the M-Auto-1C 2-sub-sprint scope.

If a dev agent paste this file by mistake expecting a contract: STOP. The M-Auto-1C / S-Auto-7.2 dev session prompt will be at `compact/sprint-062-dev-prompt.md` after Phase 3 lands. Do not invent scope.
