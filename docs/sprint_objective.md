---
title: Sprint 086 / S-Auto-31 (M-Auto-7 S-Y1) — CS4 readiness: projection infra (Part A) + executable CaseSpecs (Part B)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-08
review_cadence: per sprint
supersedes: docs/sprints/sprint-085-objective.md
superseded_by: null
notes: >
  Third (and last) autoloop launch blocker of M-Auto-7. Makes the CS4
  entity-context autoloop pilot runnable: Part A = human projection infra
  the autoloop will reference; Part B = the authored CaseSpecs the autoloop
  optimizes against. NO skill-procedure text change here — that is the pilot
  (S-Y2). Executed as two sequenced dev sessions under one sub-sprint:
  086a (Part A infra) → 086b (Part B CaseSpecs). candidate-UC names are added
  as a BACKWARD-COMPATIBLE ADDITIVE slot (`candidate_use_cases_named`); the
  existing `candidate_use_cases` string-array shape is unchanged (human
  decision 2026-06-08). Source: CS3/CS4 proposal §3.4 Part A + Part B +
  appendix (2026-06-08 CaseSpec drafts §2-§8). Pre-pilot baseline re-bless
  runs after this sub-sprint closes (batched cadence).
---

# Sprint 086 / S-Auto-31 — CS4 readiness (Part A infra + Part B CaseSpecs)

## Class

- **Layer (§3.2):** prompt_projection (Part A) + eval_spec (Part B).
- **§7 stanza:** REQUIRED (projection slots + CaseSpec authoring). Stanza below.
- **Milestone role:** Phase 1 autoloop launch blocker #3 (last before the
  pilot). Two sequenced dev sessions: 086a (Part A) → 086b (Part B).

## Goal

Stand up the projection inputs the autoloop pilot will cite and the
CaseSpecs it will optimize against, so the CS4 entity-context pilot (S-Y2)
can run on the honest baseline. No procedure-text change — the autoloop
authors that in S-Y2.

## Scope — Part A (dev session 086a; layer prompt_projection)

A1. **`moderation_reason_available` boolean** — add to
   `buildDiscoverDisambiguationSignalsNode` (`ContextProjectionBuilder.java:1146-1177`),
   derived from `session.getModerationContext()` presence (non-null /
   non-blank). Additive field on the existing disambiguation node. Project
   ONLY the boolean; NEVER the raw moderation text.
A2. **candidate-UC human-readable names (BACKWARD-COMPATIBLE ADDITIVE)** —
   add a NEW projection slot `candidate_use_cases_named` (array of
   `{"id": <uc>, "name": <UseCaseRegistryService.getUseCase(uc).name()>}`)
   emitted ALONGSIDE the EXISTING `candidate_use_cases` string array
   (`ContextProjectionBuilder.java:499-508`). **The existing
   `candidate_use_cases` shape is UNCHANGED — do not break it.** Gate the new
   slot via its own skill declaration; null-safe on unknown id; no UC-id
   rename / migration.
A3. **`customer_context_status`** — already live (`:1384-1477`); no work.
   Confirm it is reachable on the relevant phase/UC tuples.
A4. **yaml declarations** — declare the new `candidate_use_cases_named` slot
   (and confirm `discover_disambiguation_signals`) in `discover_triage.yaml`,
   and add the slots to `resolve_faq_grounded_answer.yaml`
   `required_context_keys` / `state_inheritance.soft_signal_via_projection`
   if that skill should see them. **NO procedure-text change.**

## Scope — Part B (dev session 086b; layer eval_spec)

B1. Copy the appendix's **5 NEW** CaseSpecs (`cs_uc_a_no_ad_id_ad_specific`,
   `cs_uc_a_generic_policy_question`, `cs_uc_a_loaded_listing`,
   `cs_uc_fp_loaded_moderation`, `cs_uc_a_lookup_failed`) into
   `eval_interactive/case_specs/bad_cases/` and apply the **2 EXTEND** diffs
   (`alice_uc_a_uc_h_misclass`, `wmkb_uc_a_trader_flag_secondary_uc_h`).
B2. **MANDATORY first** — reconcile every placeholder (`AD-3001`/`AD-3050`/
   `AD-3060`/`AD-3070`/`AD-9999`, `IMAGE_QUALITY`) against real fixtures in
   `data/listings/` / `GumtreeApiService` / `MockGumtreeApiService` /
   moderation-review fixtures: either swap to a fixture that actually resolves
   through the lookup path the CaseSpec exercises, or add a fixture entry. A
   CaseSpec whose ad_id never resolves mis-scores and inverts target/anti-误杀
   intent.
B3. Verify each authored CaseSpec compiles against
   `eval_interactive/eval_interactive/specs/schema.py` (Path γ fields only:
   `correct_uc` / `correct_outcome` / `tool_sequence_match` + `forbidden_tools`
   + `answer_must_not_contain` + `closure_criterion` / `bot_handling_pattern`).
B4. §5.6 bad-case suite tiering review (human-gated) + `_manifest.md` update:
   Tier-1 target (`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`,
   `cs_uc_a_lookup_failed`), Tier-1 anti-误杀 negative-control
   (`cs_uc_a_generic_policy_question`), Tier-2 neighbor
   (`cs_uc_fp_loaded_moderation`).

## Hard fences / STOP conditions

- Part A: project ONLY `moderation_reason_available` (boolean) — NOT the raw
  `moderation_reason_text` (PII / grounding-boundary). candidate-UC names are
  an ADDITIVE new slot (`candidate_use_cases_named`); the existing
  `candidate_use_cases` string-array shape is UNCHANGED (backward-compatible).
  No UC-id rename. **No procedure-text change** in either skill yaml (that is
  the S-Y2 pilot). Slots are data-derived (registry + context presence); no
  keyword/regex/if-else/enum; no Java branch on the new slots.
- Part B: do NOT widen any CaseSpec to accept current bot behaviour (§5.4);
  CaseSpec L2 outcome_checks pin trace shape, not user_message keywords; every
  `expected_tool_sequence` references a real tool in `tool-policy.yaml`; every
  target has its paired negative-control (anti-误杀).
- STOP + surface an OQ if a placeholder cannot be reconciled to a fixture
  without inventing platform behaviour — do not author a CaseSpec on a
  non-resolving ad_id.

## Test / eval requirements

- **Part A:** projection unit tests — `moderation_reason_available` true iff
  `moderationContext` present (+ raw-text-not-leaked assertion); new
  `candidate_use_cases_named` emits `{id,name}` with registry names (null-safe);
  the EXISTING `candidate_use_cases` string array is UNCHANGED (backward-compat
  regression); existing `discover_disambiguation_signals` fields +
  `customer_context_status` unchanged; reconcile golden projection snapshots
  additively (new slots appear; existing slots unchanged). Focused Java suite:
  no new regression vs post-S-X baseline.
- **Part B:** CaseSpecs compile against the schema; a baseline run (dry, no
  optimization) shows the Tier-1 targets FAIL and the negative-control PASSES
  (the gap is real) — this doubles as the pilot's Part C.1 input.
- **No real-LLM re-bless inside S-Y1** for Part A wiring; the pre-pilot
  baseline re-bless (= pilot Part C.1) runs after S-Y1 closes (batched cadence).

## §7 stanza

**Target failure layer:** prompt_projection (Part A) + eval_spec (Part B).
**Tier-0 invariant:** none added. Part A extends the §1.4 Runtime-owned
projection contract with OBSERVABLE additive slots (the runtime does not
branch on them); the existing `candidate_use_cases` shape is preserved. Part B
is eval-spec authoring per §5.6.
**Semantic hardcode:** none. `moderation_reason_available` is a presence
boolean off `session.moderationContext`; candidate-UC `name` comes from
`UseCaseRegistryService`. No keyword/regex/enum; no procedure-text change.
CaseSpec outcome_checks pin trace shape, not message keywords.
**Generalization coverage:** target/neighbor/negative/shadow = 3 / 1 / 1 / ≥2
— Tier-1 targets (no_ad_id / loaded_listing / lookup_failed); Tier-2 neighbor
(uc_fp_loaded_moderation); anti-误杀 negative-control (generic_policy_question);
shadow = held-out UC-A entity-context + generic-UC-A variants.

## Codex review plan (§4.3)

Per-sub-sprint Codex deferred to the M-Auto-7 milestone-shared close. No §4.3
trigger: not Tier-0; not §1.7-adjacent (data-derived additive slots + eval
authoring, no procedure text); not hard-fence; not fix-iteration. The §5.6
tiering (Part B) is the human gate. Dev does NOT dispatch Codex.

## Handoff requirements

086a dev → `docs/sprints/sprint-086a-handoff.md` (Part A evidence +
backward-compat confirmation + golden reconcile note). 086b dev →
`docs/sprints/sprint-086b-handoff.md` (CaseSpecs authored, placeholders
reconciled to fixtures, schema-compile, baseline-run
targets-fail/negative-control-pass, §5.6 tiering). Both archived at S-Y1 close.

## Commit discipline

086a stages only `ContextProjectionBuilder.java`, the two skill yamls
(declaration-only edits), + projection test file(s) + reconciled golden test(s).
086b stages only `eval_interactive/case_specs/bad_cases/*` + `_manifest.md` +
any new fixture file(s). No `git add -A`. New commit per part.
