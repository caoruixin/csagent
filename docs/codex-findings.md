---
title: Codex findings (latest sprint review)
doc_tier: diagnostic
status: diagnostic
implementation_status: unknown
runtime_contract: false
last_reviewed: 2026-05-11
review_cadence: on_reactivation
notes: >
  Point-in-time sprint review findings (most recent: Sprint 17 G0
  iteration governance lite). Body references specific sections of
  iteration_governance.md, AGENTS.md, and action_bank.md that may
  move on. Use as a historical review record, not as a live list of
  P2 follow-ups.
---

## Sprint Review Decision

decision: pass
blocking_count: 0
summary: Sprint 17 G0 lands the requested docs-only governance scaffold without a blocking scope or consistency failure. The sprint objective is promoted to Sprint 17, `iteration_governance.md` carries the full governance bundle plus the Section 7 sprint-objective stanza, the layer-classification and anti-hardcode self-walks match this review's independent walk, `AGENTS.md` now transitively loads the three governance docs through the existing `CLAUDE.md` include, and `action_bank.md` records Sprint 17 plus the deferred G1 / G2 backlog.

## Blocking Sprint Failures

None.

## Non-Blocking Notes

- severity: informational
- target doc: `docs/current/iteration_governance.md`
- blocks-current-sprint: no
- exact-minimal-fix: None. Q1 verdict: Sections 1-7 are internally consistent; the front matter in `docs/current/iteration_governance.md:1` uses allowed values from `docs/current/doc_governance.md:67`; Section 1 preserves the expected Constitution clauses and order at `docs/current/iteration_governance.md:40`; and the cross-references to `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` at `docs/current/iteration_governance.md:34` do not contradict either governed doc.

- severity: informational
- target doc: `docs/current/iteration_governance.md`
- blocks-current-sprint: no
- exact-minimal-fix: None. Q2 verdict: the five required scenarios resolve to exactly one layer each and match the handoff's self-walk at `docs/sprints/sprint-017-handoff.md:234`: (1) no-prior-search handover -> `semantic_planner` via `docs/current/iteration_governance.md:229`; (2) old `active_use_case` after UC shift -> `prompt_projection` via `docs/current/iteration_governance.md:222`; (3) rerun flip -> `judge_calibration` via `docs/current/iteration_governance.md:243`; (4) FINAL_ANSWER over `retrieved_but_unresolved` -> `semantic_planner` via `docs/current/iteration_governance.md:229`; (5) single-CaseSpec new escalation reason -> `eval_spec` via `docs/current/iteration_governance.md:234`.

- severity: informational
- target doc: `docs/current/iteration_governance.md`
- blocks-current-sprint: no
- exact-minimal-fix: None. Q3 verdict: Section 4's anti-hardcode prompt catches the regex PR scenario reliably because questions 1-5 / 8-9 at `docs/current/iteration_governance.md:294` identify keyword regexes for drift routing with no Tier-0 claim, no generalization coverage, and no sunset plan, and the verdict definition at `docs/current/iteration_governance.md:326` returns `reject as semantic hardcode`.

- severity: informational
- target doc: `AGENTS.md` and `CLAUDE.md`
- blocks-current-sprint: no
- exact-minimal-fix: None. Q4 verdict: Option A is complete: `AGENTS.md` is non-empty, explains the repo constitution, and includes `@docs/current/doc_governance.md`, `@docs/current/agent_context_guide.md`, and `@docs/current/iteration_governance.md` at `AGENTS.md:34`; `CLAUDE.md:1` remains the existing `@AGENTS.md` include and was not edited for Option B.

- severity: informational
- target doc: `docs/current/iteration_governance.md`
- blocks-current-sprint: no
- exact-minimal-fix: None. Q5 verdict: the Section 7 stanza is mechanically usable: it defines the semantic-touching trigger and exemption set at `docs/current/iteration_governance.md:432`, requires target layer / Tier-0 invariant / semantic hardcode / generalization coverage fields at `docs/current/iteration_governance.md:448`, and the handoff's hypothetical Sprint 18 stanza at `docs/sprints/sprint-017-handoff.md:295` is derivable from the template without inventing extra structure.

- severity: informational
- target doc: `docs/action_bank.md`
- blocks-current-sprint: no
- exact-minimal-fix: None. Q6 verdict: `docs/action_bank.md` records Sprint 17 as the current G0 phase at `docs/action_bank.md:9`, carries G0.1 / G0.2 / G0.3 / G0.4 rows under Section 3 at `docs/action_bank.md:273`, preserves the Sprint 14 / 15 / 16 historical rows, records the G1 / G2 governance backlog at `docs/action_bank.md:366`, and keeps the Sprint 16 row in the closed action index at `docs/action_bank.md:402` without rewriting its narrative.

- severity: informational
- target doc: sprint diff / working tree
- blocks-current-sprint: no
- exact-minimal-fix: None. Q7 verdict: the reviewed sprint deliverable diff `d928c52..HEAD -- docs/current/iteration_governance.md AGENTS.md docs/action_bank.md` is limited to those three declared docs; the full committed delta also adds the required Sprint 17 handoff. `git status` before this review showed `docs/sprint_objective.md` modified as the human's Sprint 16 -> Sprint 17 promotion and `docs/codex-findings.md` as the review output, matching the handoff caveat at `docs/sprints/sprint-017-handoff.md:132` rather than a runtime, prompt, eval, code, or config change.

## Regression Risks

- severity: P3
- target doc: `docs/runtime_freeze_and_risk_policy.md`
- blocks-current-sprint: no
- exact-minimal-fix: None for Sprint 17. `docs/current/iteration_governance.md:216` correctly points to `docs/runtime_freeze_and_risk_policy.md` Section 1 / Section 2 for the frozen invariants and contract, but that target doc uses "hard invariants" / "frozen runtime contract" rather than the literal label "Tier-0"; a future fold-back can make the terminology literal if it becomes confusing.

## Recommended Next Sprint Actions

- Close Sprint 17 G0 as a pass.
- Open Sprint 18 G1 Human-led Failure Portfolio and populate 10-20 representative Failure Briefs using `docs/current/iteration_governance.md` Section 2.
- Keep G2 Interactive Eval Case Family + Shadow Split deferred until after G1, as recorded in `docs/action_bank.md` Section 5.1.
