---
title: Milestone planning placeholder — M4+ candidate selection pending
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (placeholder; replaced when the next milestone objective is authored)
last_reviewed: 2026-05-23
review_cadence: per milestone
supersedes: [docs/milestones/M3-Eval_objective.md]
superseded_by: null
notes: >
  M3-Eval CLOSED 2026-05-23 (A — Clean PASS with deliver-agent documentation-only
  fix-iteration on P0-F1). This file is a placeholder pending the next M4+
  milestone planning round.

  When the next milestone is scoped by deliver-agent + human, this file is
  REPLACED with the full milestone objective per `iteration_governance.md` §8.3
  schema (12 sections covering milestone class + §7 stanza coverage; goal;
  sub-sprint sequence preliminary; non-goals; acceptance bar; hard fences;
  R-items consumed / surfaced; Codex review plan per §4.3; estimated duration;
  stop conditions; cross-milestone sequencing context; §12 closure verdict
  deferred to milestone close).

  See `docs/milestones/M3-Eval_objective.md` §12.10 for the M4+ candidate slate
  enumerated at M3-Eval close.
---

# Milestone planning placeholder — M4+ candidate selection pending

This file is intentionally a placeholder between milestone closes.

**Status**: no active milestone scope. M3-Eval CLOSED 2026-05-23
A — Clean PASS (with deliver-agent documentation-only fix-iteration on
P0-F1 evidence-package inconsistency in the bad-case manifest).

## M4+ candidate slate (per M3-Eval archive §12.10; ordered roughly by release-gate proximity)

1. **M3-B Single Handover Orchestrator** — `docs/release_gate.md` §1.1 release-gate-blocker; consume `D-single-handover-orchestrator` + related R-items from `docs/action_bank.md` §3 / §4. Pick if Salesforce cutover calendar pressure surfaces.
2. **M4 Bad-case fixture migration + parallel-session flakiness fix** — consume `R-bad-case-fixture-migrate-to-l3-judge-dims` + `R-bad-case-parallel-session-establishment-flakiness` (both M3-Eval close surfaced 2026-05-23). Smaller scope; cleanup-flavored; reduces eval-harness debt.
3. **M4 UC-G/H/I/J bad-case seeding** — consume `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close surfaced 2026-05-22). Requires real-session source material; mock-account-experiment-driven (Alice precedent).
4. **M4 Semantic-planner soft-signal extension** — consume `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (M3-Eval close surfaced 2026-05-23) + related per-UC moderation-context projection enhancements. Semantic_planner layer; LLM-first soft-signal expansion path.
5. **M4 M3-Corpus** — separate parallel-track milestone consuming `R-corpus-coverage-audit-per-uc` (preceding M3-Eval R-item; runtime-corpus-coverage scope; per-UC FAQ corpus audit).
6. **M4 Latency / Skill-Tuning / Tier-0 re-evaluation** — preceding M2-close candidates; remain in slate.

## What happens next

1. **Deliver-agent + human pick M4+ candidate** at the next planning round.
2. **Path 1 vs Path 2 decision** per `compact/sprint-deliver-orchestrator.md` Workflow inputs — research-driven (forward-looking architecture) vs bad-case-driven (backward-looking from real failure). M3-Eval was Path 1; M4+ candidates 2/3/4 are bad-case-driven (Path 2), M3-B is bad-case-blocked (release-gate), 5/6 are research-driven.
3. **Research-agent or directly-scope decision** — Path 2 candidates require a research-agent investigation before deliver-agent scopes the milestone; Path 1 candidates can go directly to scoping if the proposal already exists.
4. **Deliver-agent drafts** the full milestone objective here (REPLACES this placeholder; per `iteration_governance.md` §8.3 schema).
5. **Deliver-agent + human review** the milestone objective before launching the first sub-sprint contract at `docs/sprint_objective.md`.

## Reference

- M3-Eval archive: `docs/milestones/M3-Eval_objective.md` (objective + §12 closure verdict) + `docs/milestones/M3-Eval_codex-review.md` (Codex milestone-shared review).
- Cross-milestone history: `docs/milestones/M2_objective.md` + `docs/milestones/M2_codex-review.md` (M2 closed 2026-05-18 A — Clean PASS) + `docs/milestones/M1_objective.md` (M1 closed 2026-05-17 A-with-Codex-finding-OOSR + deliver-agent-finding-2-fix-in-close) + `docs/milestones/M2-Skill_objective.md` (M2-Skill superseded mid-flight 2026-05-17 by NEW M2).
- Active R-items: `docs/action_bank.md` §3 + §4 (current accepted + active/next) + §5 (proposed; including the 5 M3-Eval-era opened: stale-test enum-sync v0_2-to-v0_3 + bad-case suite UC-G/H/I/J seeding + iwzx UC-K-vs-UC-H spurious distress + bad-case parallel session establishment flakiness + bad-case fixture migrate to L3 judge dims).
- Closed milestone index: `docs/action_bank.md` §6.5.
- Post-close context handoff: `compact/context-handoff-M3-Eval-post-close.md` (cross-session continuity for the next deliver-agent instance).
