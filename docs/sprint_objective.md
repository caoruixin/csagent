---
title: Sprint 087 / S-Auto-32 (M-Auto-7 S-Y2) — CS4 entity-context autoloop pilot (CORE GATE) — PLACEHOLDER
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-08
review_cadence: per sprint
supersedes: docs/sprints/sprint-086-objective.md
superseded_by: null
notes: >
  PLACEHOLDER — full S-Y2 contract is drafted AFTER the pre-pilot baseline
  re-bless (= pilot Part C.1) confirms the gap is real (Tier-1 targets FAIL,
  anti-误杀 negative-control PASSES) on the post-CS1/CS3 honest baseline. S-Y2
  is the M-Auto-7 CORE GATE: the autoloop authors a skill-yaml procedure
  candidate against the S-Y1 Part B CaseSpecs; human reviews each under the
  §4.1 nine-question kernel + §1.7; an accepted candidate is merged + re-blessed
  (§3.4 hand-author fallback if no candidate is acceptable). Per-sub-sprint
  Codex REQUIRED (§4.3 trigger #2 — autoloop output is the §1.7 binding gate).
  Predecessor S-Y1 (Sprint 086, Parts A+B) closed dev-side 2026-06-08; archive
  docs/sprints/sprint-086-objective + sprint-086{a,b}-handoff.
---

# Sprint 087 / S-Auto-32 — CS4 entity-context autoloop pilot (CORE GATE)

**PLACEHOLDER — not yet a runnable contract.** The full S-Y2 contract is
authored once the pre-pilot baseline re-bless (pilot Part C.1) returns, because
the pilot's acceptance bar is anchored to the re-bless evidence (which Tier-1
targets fail, by how much, and confirmation the negative-control passes).

## Immediate predecessor state (S-Y1 closed dev-side 2026-06-08)

- **Part A (086a, commit `77b1712`)** — projection slots
  `moderation_reason_available` (boolean) + `candidate_use_cases_named`
  ({id,name}); `customer_context_status` already live. Java `1383/1/0/2`.
- **Part B (086b, commit `0b111fd`)** — 5 NEW + 2 EXTEND CS4 CaseSpecs in
  `eval_interactive/case_specs/bad_cases/`, reconciled to real `mock/` fixtures
  (AD-1001 / AD-2002 / AD-2001+PROHIBITED_ITEM / AD-9999). All 7 schema-compile.

## Next gate before this contract is drafted

**Pre-pilot baseline re-bless (= pilot Part C.1)** — Claude-launched /
human-reviewed real-LLM run (per milestone §8 batched cadence), preceded by the
§5.9 pre-flight go/no-go. Expected: Tier-1 targets (`cs_uc_a_no_ad_id_ad_specific`,
`cs_uc_a_loaded_listing`, `cs_uc_a_lookup_failed`) FAIL the entity-context
closure_criterion; anti-误杀 negative-control (`cs_uc_a_generic_policy_question`)
PASSES; Tier-2 neighbor (`cs_uc_fp_loaded_moderation`) mis-routes. On a confirmed
gap, deliver-agent drafts the full S-Y2 pilot contract (Parts C+D) per milestone
contract §3 row 4 + §5.1.

## Carried open questions (→ S-Y2 / milestone-shared Codex)

- **OQ-S86a.1** — if the pilot procedure needs `moderation_reason_available`
  (or candidate names) inside RESOLVE-FAQ, that requires a deliberate revisit of
  the M5-S4 §3.H audit invariant (update the audit doc +
  `Sprint53SkillDeclarationGatingTest`), NOT Part A wiring.
- **OQ-086b.1** — `cs_uc_a_lookup_failed` `escalation_trigger: null` (the only
  schema-valid form with `should_escalate: false`); intent preserved via
  `acceptable_outcomes` + closure_criterion. Confirm `null` at §5.6 tiering.
- **OQ-086b.2** — appendix/prompt cited the CaseSpec schema at
  `…/specs/schema.py`; real path is `…/case_spec/schema.py` (doc-hygiene; fixed
  in the archived S-Y1 contract B3).
