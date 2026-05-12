---
title: Codex findings (latest sprint review)
doc_tier: diagnostic
status: diagnostic
implementation_status: unknown
runtime_contract: false
last_reviewed: 2026-05-13
review_cadence: on_reactivation
notes: >
  Point-in-time sprint review findings (most recent: Sprint 19 A+B
  smoke regression investigation + orchestrator tool-call de-dup).
  Body references specific sections of iteration_governance.md,
  sprint_objective.md, and the Sprint 19 handoff that may move on.
  Use as a historical review record, not as a live list of P2 follow-ups.
---

## Sprint Review Decision

decision: pass
blocking_count: 0
summary: Sprint 19 A+B passes review as a proposal-only / docs-only close. The reviewed sprint surface contains the active Sprint 19 objective, the new Sprint 19 handoff, and the `docs/10-handoff.md` lead refresh; it ships no runtime, prompt, eval, judge, FAQ-corpus, CaseSpec, or bundled-fix change. All six Track A cases plus Track B have recorded §3.2 walks and bundle/defer decisions, no `java_guard` or `human_review_required` finding was silently bundled, the target / neighbor / negative / shadow coverage table is present with negatives N/A because no fix was bundled, and the per-PR §4.1 verdict is `approve` under the docs-only governance exemption.

## Blocking Findings

None.

## Per-PR Anti-Hardcode Verdict

verdict: `approve`

Scope exemption: docs-only governance. The Sprint 19 diff describes investigation results and future remediation proposals, but adds no runtime branch, prompt rule, judge rubric, CaseSpec override, enum, keyword, regex, or per-UC matrix.

- Q1 / Q4 / Q5 / Q6: no semantic hardcode is introduced; case ids and trace excerpts appear only in the diagnostic handoff, not in runtime, prompt, or judge config.
- Q7: tool schema, capability / permission boundary, PII / safety floor, and grounding floor are unchanged because no executable surface changed.
- Q8: `docs/sprints/sprint-019-handoff.md:902` contains the target / neighbor / negative / shadow table; `docs/sprints/sprint-019-handoff.md:906` names the 6 regressed target cases and 8 smoke neighbors, `docs/sprints/sprint-019-handoff.md:907` names the manual-probe target, and shadow is explicitly deferred to G2.
- Q9: no temporary runtime change shipped, so no rollback / sunset plan is required.

## Sprint Gate Checks

- §3.2 walk coverage: pass. The handoff records walks for cs_002, cs_011, cs_014, cs_038, cs_040, cs_066, and manual-probe Track B at `docs/sprints/sprint-019-handoff.md:836`.
- Bundle / regression-test gate: pass. No fix was bundled, so the regression-test and named-negative-control requirements do not fire.
- Java-guard / Tier-0 gate: pass. The handoff records no `java_guard` classification and no silent bundle of a `human_review_required` shape at `docs/sprints/sprint-019-handoff.md:846`.
- Coverage gate: pass. Target and neighbor cases are named; negatives are N/A per no bundled fix; shadow is deferred to G2 as allowed by the sprint objective.
- Out-of-scope gate: pass. The remaining 16 G1 R-items, Wave A5 / A6, FAQ corpus, and G2 design remain outside Sprint 19; the handoff records them as deferrals under `docs/sprints/sprint-019-handoff.md:1134` and the existing backlog remains in `docs/action_bank.md:377`.

## Non-Blocking Findings

- PR file: `docs/sprints/sprint-019-handoff.md:627`
- diff snippet:

```text
The manual-probe trace itself is not saved as a file in this checkout; the brief is the only narrative.
```

- failing §4.1 question: none; this is an evidence-confidence note, not a semantic-hardcode failure.
- §3.1 layer: `semantic_planner` primary, `prompt_projection` secondary as classified in `docs/sprints/sprint-019-handoff.md:787`.
- severity: non-blocking
- reasoning: Track B's `semantic_planner` classification relies on resolving the brief-vs-code contradiction by inference (`docs/sprints/sprint-019-handoff.md:680` through `docs/sprints/sprint-019-handoff.md:703`) because the full per-step manual-probe trace is absent. The handoff explicitly records the uncertainty and does not bundle a fix, so this does not trip the Sprint 19 `fix_required` gates.

## Review Scope Notes

- Reviewed against local `HEAD` (`18a96ed`) plus the uncommitted Sprint 19 docs diff: `docs/sprint_objective.md`, `docs/10-handoff.md`, and new `docs/sprints/sprint-019-handoff.md`.
- Untracked sidecar files under `.claude/`, `compact/`, and `csagent_system_design_review.md` were treated as local agent/workspace artefacts, not Sprint 19 deliverables; keep them out of the PR unless separately scoped and reviewed.
