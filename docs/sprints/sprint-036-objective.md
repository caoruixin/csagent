---
title: Sprint 36 objective — Skill foundation + UC-switching continuity design freeze (M2-Skill sub-sprint 1)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-035-objective.md]
superseded_by: docs/sprint_objective.md
notes: >
  Sprint 36 is the first sub-sprint of Milestone M2-Skill (Skill
  Foundation + UC-Switching Continuity, LLM-led Policy-bounded) per
  `docs/milestone_objective.md` §3 Sprint 36 row. Layer: docs/design
  only (NO `server/` code). Sprint 36 is a **design-freeze sub-
  sprint** (Sprint-30-shape) that produces an architectural decision
  doc at `docs/proposals/skill_foundation_design.md` superseding /
  refining `docs/proposals/skill_orchestration_candidates.md` (Sprint
  5 F2 proposal). Output locks the hybrid framing (envelope +
  recommended order via prompt; terminal predicate via Java guard for
  Runtime-owned floor ONLY) for Sprints 37/38 AND the UC-switching
  wide continuity invariant matrix for Sprint 39. Walks the §4.1
  nine-question anti-hardcode kernel against the proposed design
  pre-implementation. §7 stanza REQUIRED (multi-layer prospective per
  `feedback_multi_layer_prospective_stanza.md`: Track A = this design-
  freeze sub-sprint, layer eval_spec/docs; Track B = prospective
  coverage of Sprints 37/38/39 layer breakdown). Codex review:
  per-sub-sprint at Sprint 36 close per `iteration_governance.md`
  §4.3 trigger #1 (possible new Tier-0 candidate from Skill predicate
  semantics) + #2 (§1.7 boundary discussion on hybrid framing).
  Human authorization at M2-Skill approval round (2026-05-17) on
  the hard fence inversion for `D-hard-citation-gate` governs the
  S1 predicate scope; Sprint 36 design freeze MUST honor the
  authorized scope and MUST NOT expand it.

  **Closed PASS 2026-05-17.** Dev commit `ed71031` shipped
  `docs/proposals/skill_foundation_design.md` (~750 lines, 8
  sections, D1-D6 + §4.1 walk-through + Tier-0 candidate write-ups)
  + frontmatter EDIT on `docs/proposals/skill_orchestration_candidates.md`
  (`superseded_by` + `status: superseded`) + 12-section
  `docs/sprints/sprint-036-handoff.md`. Two material findings
  surfaced: (1) premise #8 class-name refinement (`UseCaseDefinition.requiredIntakeFields`
  → `IntakeFieldsRegistry.intakeComplete`; mechanical correction,
  intent preserved); (2) S1+S2 partial predicates already ship as
  Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1 work in
  `AgentRunLoopImpl` — Sprint 37 + 38 reframe accordingly (Sprint
  38 becomes much lighter). 8 OQs surfaced with dev-recommended
  defaults (esp. Tier-0 candidates 7.1 + 7.2 with DEFER); deliver-
  agent + human confirmed all 8 defaults at sub-sprint close. Codex
  per-sub-sprint review at `docs/sprints/sprint-036-codex-review.md`
  returned **`decision: pass / blocking_count: 0`** on first pass;
  Codex independently verified MATERIAL FINDING + premise #8
  refinement; CONFIRMED DEFER for both Tier-0 candidates;
  re-evaluation flagged for M2 close after Sprint 37 traces.
---

# Sprint 36 — Skill foundation + UC-switching continuity design freeze

**Milestone:** M2-Skill (Skill Foundation + UC-Switching Continuity) sub-sprint 1 of 5 per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Design-freeze sub-sprint (Sprint-30-shape), single track, docs/design only.** Layer: `eval_spec` / governance per `docs/current/iteration_governance.md` §3.2 Q6 (the architectural decision doc IS an eval-spec / governance instrument; it sets the contract that Sprints 37/38/39 implement). Sprint 36 is **not a behaviour-change sprint** — no production runtime, no prompt, no judge calibration, no test is edited; the dev session produces an architectural decision doc + walks the §4.1 nine-question anti-hardcode kernel against the proposed design.

**§7 stanza REQUIRED — multi-layer prospective** per `feedback_multi_layer_prospective_stanza.md`:

- **Track A** (this sub-sprint) — Sprint 36 design freeze; layer `eval_spec` / governance; ships zero semantic change; stanza fields fill straightforwardly with "N/A" or "no semantic hardcode introduced."
- **Track B** (prospective for Sprints 37/38/39) — names the layers each downstream sub-sprint will touch + the §7 stanza fields they will need to fill, so Codex Sprint 36 review can verify the prospective alignment at Sprint 36 close. See §8 below.

**Codex review:** per-sub-sprint at Sprint 36 close per `iteration_governance.md` §4.3 trigger #1 (possible new Tier-0 candidate from Skill predicate semantics — the S1 citation predicate or any UC-switching invariant may qualify as Tier-0 territory; Sprint 36 SHALL surface the candidate question explicitly) + trigger #2 (§1.7 boundary discussion on hybrid framing — Codex review verifies the proposed design does NOT smuggle per-UC-branch if-else into skill bodies). Per-sub-sprint Codex review fires BEFORE Sprint 37 begins; per M2-Skill §10 stop condition #1, any surfaced Tier-0 candidate requires explicit human-review escalation BEFORE Sprint 37 dev starts.

## 2-12 [body unchanged from sprint_objective.md as committed at sub-sprint open; closure verdict captured in front-matter notes]

The §2-§12 body is the deliver-agent + human authored sub-sprint contract per the 12-section shape. For the full body of §2 (Goal — D1-D6 sub-decisions), §3 (Non-goals), §4 (Premise check — 10 items), §5 (Files in scope + §5.1 design doc structure + §5.2 sub-sprint-close artefacts), §6 (Files NOT in scope — 19 hard fences), §7 (Bundle policy), §8 (§7 stanza Track A + Track B), §9 (Success metrics), §10 (Stop conditions — 11 items), §11 (Handoff document contract), §12 (M2-Skill milestone context cross-reference), see the dev handoff at `docs/sprints/sprint-036-handoff.md` which mirrors and elaborates each section per `docs/sprint_objective.md` §11.

The contract as committed at Sprint 36 close is the one preserved here at archival. The Sprint 36 dev session verified all 10 premise items (1 refinement on #8; 9 PASS as-written), shipped all required files (3 files: design doc + frontmatter EDIT + handoff), honored all 19 hard fences (per Codex independent verification), and surfaced all OQs to deliver-agent + human per §11 §11 of the contract.

## Closure verdict (captured at sub-sprint close, 2026-05-17)

| field | value |
|---|---|
| status | **PASS** (Codex per-sub-sprint review `decision: pass / blocking_count: 0` on first pass) |
| classification | **A — Clean close** (design-freeze sub-sprint per spec; Codex pass on first review; no fix iteration) |
| Codex outcome | `pass / 0` per `docs/sprints/sprint-036-codex-review.md` |
| §4.1 anti-hardcode kernel | `approve` walked against the PROPOSED DESIGN (D1-D6); each Q PASS; verdict matches dev §8 Track A self-walk |
| Hard-fence verification | ALL 19 Sprint 36 §6 fences + ALL 19 M2-Skill §6 fences PASS per Codex independent verification |
| MATERIAL FINDING verification | Codex independently verified S1+S2 partial predicates ship at cited Sprint 6/7/11 line ranges in `AgentRunLoopImpl` |
| Premise #8 refinement verification | Codex independently confirmed `UseCaseDefinition` has no `requiredIntakeFields` field; `IntakeFieldsRegistry` is the actual source; D4 §5.2 cites correctly |
| Tier-0 candidate disposition | OQ 7.1 (S1 citation predicate): DEFER CONFIRMED by Codex; OQ 7.2 (S2 intake-completeness): DEFER CONFIRMED by Codex; re-evaluate at M2 close after Sprint 37 traces observed |
| 6 non-Tier-0 OQs disposition (7.3-7.8) | All 6 dev-recommended defaults confirmed by deliver-agent + human at sub-sprint close; Sprint 37/38/39 planning rounds carry the per-sub-sprint picks |
| Java baseline | 983 / 0 / 0 / 2 in Codex's CLEAN detached worktree (cleaner than dirty-working-tree 983/1/0/2 baseline; inherited `SystemPromptUserRequestedTiebreakerTest` failure attributed to dirty `system_prompt.txt`, NOT Sprint 36-attributable) |
| Open R-item flips at close | `R-grounding-discipline-iterative-search-fabrication` → `Sprint-36-frozen-pending-Sprint-37-implementation` (S1 predicate freeze locked; Sprint 37 implements); Sprint 5 F2 S1 + S2 candidates promoted from `not_started` to `Sprint-36-frozen` via the design doc superseding the proposal; `D-hard-citation-gate` gets a status-note pointer to M2-Skill §6 #4 bounded inversion (deferral preserved for generic gate; narrow Skill-bounded exception for S1 authorized) |
| Residual risk noted | Codex §10 deferred note: S1 predicate is minimum citation-presence floor only; does NOT judge citation relevance or factual content quality. If Sprint 37 traces still fabricate while carrying a `source_id`, that's a new Sprint 37/M2-close evidence item for human/Codex review (NOT a reason to broaden Sprint 36's frozen scope silently) |
| date | 2026-05-17 |
