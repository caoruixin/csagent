---
title: Milestone M1 — DISCOVER + Intake (model-first DISCOVER classification + intake-state durability)
doc_tier: milestone-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: ad hoc
supersedes: []
superseded_by: docs/milestone_objective.md (M2)
notes: >
  M1 is the first milestone under the §8 milestone framework (2026-05-16
  governance update). It bundles the unshipped halves of the original
  roadmap's Sprint 18 (semantic planner soft-signal projection) and
  Sprint 20 broader (skill-plan-state intake durability), plus the
  Alice bad case D1-D3 dimensions, plus the Sprint 32-surfaced
  R-option-beta-coverage-gap-uc-a-uc-c-shape investigation. Scope
  discipline at the milestone level: M1 does NOT touch
  `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` /
  `ClassifyUseCaseTool` semantic surfaces; M1 does NOT add Tier-0
  invariants without human-review escalation; M1 does NOT widen
  judge rubrics or eval-side overrides.
---

# Milestone M1 — DISCOVER + Intake

## 1. Milestone class

**Multi-layer milestone, 3-4 coordinated sub-sprints.** Sub-sprint layer breakdown:

| Sub-sprint | Layer | §7 stanza | Codex review |
|---|---|---|---|
| Sprint 33 — DISCOVER soft-signal + classification guidance | `prompt_projection` | REQUIRED | Milestone-shared (default) |
| Sprint 34 — Intake field prefill UC-G/H/I/J | `skill_state` | REQUIRED | Milestone-shared (default) |
| Sprint 35 — Option β coverage probe + worked-example re-anchor | `eval_spec` | REQUIRED | Milestone-shared (default) |
| Sprint 36 (conditional) — INTAKE-locked reroute trigger investigation | `human_review_required` | REQUIRED if shipped; investigation-only sprint may be exempt | Per-sub-sprint (Tier-0 candidate per §4.3 trigger) |

Codex review default for M1: **milestone-shared at M1 close** per `iteration_governance.md` §4.3. Per-sub-sprint Codex review triggered only if Sprint 36 surfaces a Tier-0 candidate.

## 2. Goal

Make the Alice bad case (`eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`) resolvable. Concretely:

- The bot recognizes on turn 1 or turn 2 that a REMOVED-listing visibility question is fundamentally a UC-A FAQ-resolvable question, not a UC-H intake-only path commitment. (Sub-sprint 33: DISCOVER soft-signal + classification guidance.)
- When intake IS the correct path (UC-G/H/I/J cases that genuinely need appeal-style intake), the bot does NOT re-ask the user for `ad_id_or_listing_url` / `registered_email` fields that the form context already supplied. (Sub-sprint 34: form-context prefill.)
- The Sprint 32-surfaced architectural finding (`R-option-beta-coverage-gap-uc-a-uc-c-shape`) is empirically characterized: the deliver-agent + dev produce a per-(topic, description-shape) AMBIGUOUS/ROUTED matrix; a documented decision is made on whether to re-anchor the §7.2 worked example to a UC pair that DOES fall within Option β coverage. (Sub-sprint 35.)
- (Conditional, Sprint 36) The "no escape from wrong-UC INTAKE commitment" architectural concern is investigated; the deliver-agent + human decide whether a soft-signal projection (preferred), a runtime reroute trigger (escalation candidate), or a new Tier-0 invariant is the right answer.

## 3. Sub-sprint sequence (preliminary; deliver-agent + human refine at each planning round)

### Sprint 33 — DISCOVER UC-A/FP/H soft-signal + classification guidance

**Layer:** `prompt_projection`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default).

**Scope (3 sentences):** Project a soft signal that REMOVED-listing + Ad Support topic yields multiple plausible UC candidates (UC-A visibility, UC-FP ad-support deletion, UC-H appeal) and surface this to the LLM as observable evidence on the DISCOVER projection. Extend the DISCOVER `systemInstruction` at `PhaseEvaluator.java:411-431` with disambiguation guidance for ad-status UCs (NOT a regex on user content, NOT a per-UC matrix — principle-level prompt teaching: "when a REMOVED listing is observed, the user may want to know WHY it was removed (UC-A FAQ-resolvable) OR appeal the removal (UC-H intake) — if ambiguous, ask one clarifying question before committing"). Add a sibling teaching paragraph to `system_prompt.txt` adjacent to the `already_called` and `alternate_candidate_use_cases` paragraphs.

**Files in scope:** `ContextProjectionBuilder.java` (new projection field for `discover_disambiguation_signals`); `PhaseEvaluator.java` (DISCOVER `systemInstruction` extension); `system_prompt.txt` (new teaching paragraph). Plus regression-test file(s) for the new projection.

**Hard fences:** no `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` / `ClassifyUseCaseTool` touch; no Tier-0 invariant added; no edits to existing case families.

### Sprint 34 — Intake field prefill UC-G/H/I/J

**Layer:** `skill_state`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default).

**Scope (3 sentences):** Extend `IntakeFieldExtractor.java:21-35` from UC-K-only to also handle UC-G / UC-H / UC-I / UC-J using the form-context aliases already defined in `IntakeFieldsRegistry.java:86-103`. For each of the four UCs, document which form-context fields map to which intake field aliases; auto-prefill at session creation or first turn; surface in `intake_state.fields_collected` per existing convention. Add per-UC regression tests mirroring Sprint 7.1 §J0 shape.

**Files in scope:** `IntakeFieldExtractor.java` (new per-UC branches); `AgentRunLoopImpl.java:561-565` (merge gate extension, if needed); regression-test files.

**Hard fences:** no `IntakeFieldsRegistry` schema change (only consume existing alias definitions); no Tier-0 invariant added; no edits to existing intake-complete guards.

### Sprint 35 — Option β coverage probe + worked-example re-anchor decision

**Layer:** `eval_spec`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default).

**Scope (3 sentences):** Run a probe corpus covering every (`topic_subject`, `description`-shape) tuple that the intake router exposes; extract whether each produces AMBIGUOUS (with what candidate list) or ROUTED (to which UC). Produce a per-(topic, description-shape) AMBIGUOUS/ROUTED matrix. Decide (deliver-agent + human at sub-sprint close): either (a) re-anchor the `iteration_governance.md` §7.2 worked example to a UC pair that DOES fall within Option β coverage (Sprint 32 neighbor #1 evidence: UC-A↔UC-FP works), OR (b) open a follow-on R-item proposing Option γ live `AlternateUseCaseSurveyor` for the post-routing drift shape.

**Files in scope:** probe CaseSpecs under `eval_interactive/case_specs/probe/option_beta_coverage/` (NEW directory; flat layout); a probe-result analysis document at `docs/diagnostics/option_beta_coverage_matrix.md`; if (a) chosen, an `iteration_governance.md` §7.2 fold-back edit (otherwise deferred to normal cadence).

**Hard fences:** no `UseCaseRouter` edit; no `IntentClassification` edit; no production code touch.

### Sprint 36 (conditional) — INTAKE-locked reroute trigger investigation

**Layer:** `human_review_required` per `iteration_governance.md` §3.2 Q2 (Java-guard territory but no current Tier-0 invariant covers it; the design decision belongs to the human, not invented in the sprint).

**Scope (3 sentences):** Investigate whether the cross-turn reroute mechanism (Sprint 10 `RuntimeIntentClassifier.applyRerouteDecision`) should fire when an INTAKE-locked session shows persistent "user cannot supply mandatory intake field" evidence, indicating a likely upstream mis-classification at DISCOVER. Evaluate three answer shapes: (a) soft signal — project `intake_field_stall_count` to the LLM and let `classify_use_case` propose a reroute while in INTAKE; (b) deterministic trigger — runtime reroute after N consecutive turns without progress on the same intake field; (c) new Tier-0 invariant — INTAKE-locked sessions that cannot collect required fields must trigger a reroute candidacy check, with human-review-required to authorize. Document each option's §1.7 forbidden-list risk + §1.4 Runtime ownership implications.

**Files in scope:** investigation document at `docs/proposals/intake_locked_reroute_design.md`; NO production code change in Sprint 36 (this is investigation-only; implementation is a separate later sprint per `human_review_required` discipline).

**Conditional trigger:** Sprint 36 fires only if Sprint 33 + Sprint 34 do NOT sufficiently close the Alice bad case at M1 acceptance review (i.e., if the closure_criterion is still FAIL after Sprint 33 + Sprint 34, then Sprint 36 investigation is needed before M2 can scope a structural fix). If Sprint 33 + Sprint 34 alone meet the closure_criterion, Sprint 36 is deferred to a later milestone.

## 4. Non-goals (explicit)

- M1 does NOT touch `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` semantic surfaces. The DISCOVER fix in Sprint 33 is a prompt-projection-only soft signal; no runtime classifier behaviour change.
- M1 does NOT widen judge rubrics or `eval_interactive/case_spec_overrides.yaml`.
- M1 does NOT edit existing case families (Sprint 20 / Sprint 29 / Sprint 32 cascade fence).
- M1 does NOT touch `eval_interactive/eval_interactive/` (harness, loader, simulator).
- M1 does NOT address D5 (Trace UI observability gap) — that is a separate parallel-track UI sprint per the deliver-agent investigation 2026-05-16; can ship at any time independently.
- M1 does NOT consume `R-llm-provider-latency-drift-2026-05-16` (deferred to M2 or a separate diagnostic sprint).
- M1 does NOT consume `D-single-handover-orchestrator` (P0 launch blocker; separate triggered sprint when Salesforce cutover or real-traffic double-transfer occurs).
- M1 does NOT consume `D-full-issue-ledger` (hard-deferred per `action_bank.md:344`; requires explicit new-objective approval).
- M1 does NOT pre-decide Sprint 36 (conditional; fires only if Sprint 33+34 don't close Alice).

## 5. Milestone acceptance bar

**Primary gate (per §5.6):** Alice bad case (`eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`) closure criterion met under manual review of M1 close rerun trace. Closure criterion per the CaseSpec:

> Bot reaches one of these end-states without entering a multi-turn dead loop (≥ 3 consecutive turns asking the same intake field with no progress):
> (a) Bot routes to UC-A handling + grounded FAQ answer about removal reason / appeal path.
> (b) Bot asks ONE focused clarifying question on turn 2 and routes per user's answer.
> (c) Bot acknowledges UC-H but recognizes the user cannot supply stated_reason_or_context and gracefully escalates with reason=user_requested or reason=ambiguous_intent.

**Secondary observations (informational, not gates):**

- Sprint 32-surfaced `R-option-beta-coverage-gap-uc-a-uc-c-shape` matrix produced and reviewed at Sprint 35 close.
- Architecture-health metrics direction (§6): `new_semantic_hardcode_count` = 0 (M1 ships no Java decision-path hardcode); `soft_signal_conversion_count` ≥ 2 (DISCOVER signal + intake-prefill source-of-truth signal); `planner_ownership_ratio` not decreased.

**Java baseline:** preserved (917 / 1-inherited / 0 / 2 from Sprint 31 fix-iteration #2 close + Sprint 33 / 34 / 35 new tests appended).

**Smoke composite_score:** observation only per §5.5; tracked but does not gate close.

## 6. Hard fences (milestone-level)

1. No edits to `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`.
2. No Tier-0 invariant added without human-review escalation (Sprint 36 explicitly surfaces this as a candidate; M1 does NOT auto-promote).
3. No edits to existing case families under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence).
4. No edits to existing shadow CaseSpecs under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
5. No edits to `eval_interactive/eval_interactive/` (harness, loader, simulator) — these are stable per Sprint 28 / Sprint 32 precedent.
6. No `iteration_governance.md` edit during M1 EXCEPT the Sprint 35 §7.2 worked-example fold-back (and only if Sprint 35 (a) is chosen).
7. No widening of `eval_interactive/case_spec_overrides.yaml`.
8. No edits to sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-032-*`.
9. No edits to `docs/foundational/`.
10. No widening of `escalation_reason` enum at `PhaseEvaluator.java:39-63`.

## 7. R-items consumed / surfaced

**Consumed by M1:**

- `R-alternate-uc-signal-shadow-caseset` (Sprint 31 OQ4 informal; closed by Sprint 32 substance with investigation finding — M1 does not re-litigate).
- (Implicit) Original roadmap Sprint 18 soft-signal projection unshipped half — M1 sub-sprint 33 partially ships via DISCOVER soft-signal extension.
- (Implicit) Original roadmap Sprint 20 broader Skill Plan-state durability unshipped half — M1 sub-sprint 34 ships intake-prefill durability.
- `R-option-beta-coverage-gap-uc-a-uc-c-shape` (Sprint 32 surfaced; consumed by M1 sub-sprint 35).
- (Conditional) `R-intake-instruction-decomposition` (Sprint 27 named-but-not-opened) — if Sprint 36 fires, this R-item is also consumed.

**Surfaced by M1 (expected):**

- If Sprint 35 chooses option (b): new R-item `R-option-gamma-alternate-uc-surveyor-design` for post-routing drift shape coverage.
- If Sprint 36 surfaces a Tier-0 candidate: deliver-agent + human escalation; no R-item auto-registered without human approval.
- Architecture-health metric collection candidates (§6) may surface as side R-items if M1 sub-sprints reveal natural collection points.

**NOT consumed by M1 (deferred):**

- `R-llm-provider-latency-drift-2026-05-16` (deferred to M2 or separate diagnostic sprint).
- `R-uc-cdf-get-customer-context-bot-actual-usage` (open; unrelated to M1 scope).
- `R-prompt-phase-plan-directive-followship` (open; n=1 from Sprint 29; needs n+1 confirmation before structural action — not part of M1 scope).
- `D-single-handover-orchestrator` (P0 launch blocker; separate triggered sprint).
- `D-full-issue-ledger` (hard-deferred per `action_bank.md:344`).
- D5 (Trace UI observability gap) — parallel-track UI sprint, no §7 stanza.

## 8. Codex review plan (per §4.3)

**Default: milestone-shared Codex review at M1 close.** Single review against the cumulative commit range of Sprint 33 + Sprint 34 + Sprint 35 (+ Sprint 36 if shipped).

**Per-sub-sprint Codex triggers expected:**

- **Sprint 36 ONLY**: this sub-sprint is `human_review_required` layer per §3.2 Q2; the investigation document may surface a Tier-0 candidate. Per §4.3 trigger #1 (new Tier-0 candidate), Codex reviews Sprint 36 at sub-sprint close before M2 can scope a structural fix. If Sprint 36 does NOT surface a Tier-0 candidate (e.g., concludes (a) soft signal is sufficient), the per-sub-sprint Codex review is downgraded to milestone-shared.

**Codex review prompt** for M1 close: deliver-agent drafts at `compact/M1-review-prompt.md` once Sprint 33 + Sprint 34 + Sprint 35 are committed. Per-sub-sprint dev prompts are at `compact/sprint-NNN-dev-prompt.md` per existing convention.

## 9. Estimated milestone duration

Informational, not a gate. Estimated 1-2 weeks for Sprint 33 + Sprint 34 + Sprint 35 (3-5 days each), plus 3-5 days for Sprint 36 if needed, plus deliver-agent + human planning rounds + milestone close. Total estimate: 2-3 weeks.

This is the first milestone under the §8 framework; the deliver-agent will record actual duration at M1 close as a calibration baseline for M2 planning.

## 10. Stop conditions (milestone-level)

STOP and re-plan the milestone when:

1. **Sprint 33 ships a regression on the Sprint 31 baseline** (e.g., the new DISCOVER projection field breaks `alternate_candidate_use_cases` slot population, or the new prompt teaching paragraph triggers smoke composite_score collapse beyond observation noise).
2. **Sprint 34 breaks `IntakeFieldExtractor` UC-K behaviour** (the existing UC-K path must remain byte-identical; extension MUST be additive).
3. **Sprint 35 reveals that the §7.2 worked example IS truly impossible under Option β** AND no alternative re-anchor UC pair is viable (a deeper architectural shift is needed; surface to M2 planning).
4. **Sprint 36 fires AND surfaces a Tier-0 candidate AND human declines to promote** (the gap stays open; M2 must address with a different design shape).
5. **Alice closure criterion still FAIL after Sprint 33 + Sprint 34 + Sprint 35 + Sprint 36** (M1 closes with investigation finding; M2 scopes a structural attempt).
6. **Any sub-sprint surfaces a §1.7 forbidden-list violation** (immediate STOP; deliver-agent + human address before continuing).

## 11. Cross-milestone sequencing context

M1 is the first milestone. M2 candidates per the 2026-05-16 deliver-agent + human governance round:

- **M2-A**: Resolution + Escalation (Sprint 21 escalation_reason canonical+rationale+confidence + handover human UX + Sprint 23 URL policy + factual narrowing).
- **M2-B**: Lifecycle (`D-single-handover-orchestrator` + `D-full-issue-ledger` + semantic planner shadow mode).
- **M2-C**: Diagnostic (`R-llm-provider-latency-drift-2026-05-16` + per-LLM-call latency characterization).

M2 selection happens at M1 close based on (a) which M1 sub-sprints fired, (b) which R-items M1 surfaced, (c) bad-case suite state, and (d) human priorities. The deliver-agent does NOT pre-decide M2 here.

---

## 12. M1 closure verdict — appended at milestone close 2026-05-17

### 12.1 Milestone close header (per `iteration_governance.md` §4.2 adapted for milestone-shared close)

```
## Milestone Review Decision
milestone: M1 — DISCOVER + Intake
commit_range: c9edb37..eb65e2b
sub_sprints_reviewed: Sprint 33, Sprint 34, Sprint 35
codex_verdict_raw: fix_required (blocking_count: 2)
deliver_agent_human_verdict: PASS — A-with-Codex-finding-OOSR-classification-and-deliver-agent-finding-2-fix-in-close
decision_date: 2026-05-17
summary: M1 closed PASS after deliver-agent + human classified Codex
  Finding 1 (Alice grounding fabrication on Codex independent rerun;
  layer = semantic_planner per Codex triage) as out_of_scope_review
  per M1 §6 hard fence #1 (semantic_planner work is M1-out-of-scope
  by construction). Finding 1 carry: new R-item
  `R-grounding-discipline-iterative-search-fabrication` opened in
  `docs/action_bank.md` §5.2 as high-priority M2 anchor. Codex Finding
  2 (Sprint 35 matrix §5/§6 stale vs close decisions) resolved at M1
  close via deliver-agent commit appending "Close decision update
  (2026-05-17)" addendum to `docs/diagnostics/option_beta_coverage_matrix.md`
  per Decision B-2a. All three M1 sub-sprint contracts shipped per
  contract; cumulative Java baseline 932 → 983 (+51 new tests; UC-K
  regression preserved); Alice closure-criterion (a) multi-trace
  evidence 2-of-3 PASS (1-of-3 FAIL on fabrication-condition captured
  in the new R-item).
```

### 12.2 §5 milestone-acceptance-bar evaluation

Per §5 of this archived contract, the M1 primary gate is Alice bad-case closure-criterion. Multi-trace evidence at HEAD `eb65e2b`:

| trace | tool | active_use_case | closure outcome |
|---|---|---|---|
| Sprint 33 single-run (commit `8a22aa6` HEAD-effective) | `eval_interactive/results/20260516-110928/results.json` | UC-A | (a) PASS |
| Deliver-agent M1-close rerun | `eval_interactive/results/20260516-232938/results.json` | UC-A | (a) PASS — graceful `faq_miss_threshold_exceeded` escalation, no fabrication |
| Codex independent rerun (port 18080 clean detached HEAD) | `eval_interactive/results/20260516-235235/results.json` | UC-A | **FAIL** — bot fabricated specific takedown reason quoting generic article `ka41r000000LIEJAA4` |

2-of-3 PASS / 1-of-3 FAIL. The FAIL is a load-bearing fabrication condition per the CaseSpec `closure_criterion.FAIL conditions`. Codex (correctly) called the gate `fix_required`.

Deliver-agent + human disposition: the FAIL trace's root cause (semantic_planner / grounding-answer discipline; LLM-variance on iterative search) is **outside M1's scope by construction** per the §6 hard fence #1 ("no edits to RuntimeIntentClassifier / IntentClassification / DriftResult / DriftDetector / UseCaseRouter / ClassifyUseCaseTool"). Sprint 33/34/35 individually did not scope grounding-discipline; M1 milestone did not promise to fix it. The right disposition is `out_of_scope_review` with explicit M2 anchor carry, NOT M1 fix-iteration that would smuggle in scope creep.

### 12.3 §6 milestone-level hard fence verification (per Codex M1 review)

All 10 milestone-level hard fences per §6 above held across the cumulative commit range. Per `docs/sprints/M1-codex-review.md`:

> Milestone-level hard fences: PASS. Requested cumulative forbidden-path stat scan over `c9edb37..eb65e2b` returned empty output; no escalation_reason enum widening at `PhaseEvaluator.java:39-63`.

Plus the constitution-discipline §7.2 fold-back (which was planning-anticipated in §3 Sprint 35 row) was *considered and dropped* at Sprint 35 close per the discipline review captured in `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md`. `docs/current/iteration_governance.md` §7.2 stayed at UC-A↔UC-C as the principled teaching example.

### 12.4 R-items consumed / surfaced (per §7 above)

Consumed by M1 (closed):
- `R-alternate-uc-signal-shadow-caseset` — closed by Sprint 32 substance (recorded in M1 §7).
- (Implicit) Sprint 18 soft-signal-projection unshipped half — partially shipped via Sprint 33 DISCOVER soft signal.
- (Implicit) Sprint 20 broader skill-plan-state durability unshipped half — shipped via Sprint 34 intake prefill.
- `R-option-beta-coverage-gap-uc-a-uc-c-shape` — **CLOSED** by Sprint 35 matrix + (b) `R-loosen-topic-uc-binding-llm-owned-drift` opened succeeding it.

Surfaced by M1 (new):
- `R-loosen-topic-uc-binding-llm-owned-drift` — opened at Sprint 35 close; deferred to M3+ per M2 candidate selection 2026-05-17.
- `R-grounding-discipline-iterative-search-fabrication` — opened at M1 close 2026-05-17; selected as M2 sub-sprint 1 anchor.

NOT consumed by M1 (still deferred per original M1 §7):
- `R-llm-provider-latency-drift-2026-05-16` — proposed; pickup at M2-C parallel-track (M2 sub-sprint C1).
- `D-single-handover-orchestrator` — P0 launch blocker; M2-B candidate deferred to M3 unless Salesforce cutover calendar pressure surfaces.
- `D-full-issue-ledger` — hard-deferred per `action_bank.md:344`; requires explicit new-objective approval (not granted at M1 close).
- Sprint 36 (conditional INTAKE-locked reroute investigation) — **deferred** per the conditional trigger not firing.

### 12.5 M2 candidate selection (per §11 above + 2026-05-17 cross-validated research-agent round)

Per Decision C 2026-05-17 (cross-validated by two parallel research-agents at M1 close):

- **M2 (now)**: M2-A ∪ M2-E.option-3 — Resolution + Escalation + Grounding Discipline (`semantic_planner` + `prompt_projection`). Anchored on `R-grounding-discipline-iterative-search-fabrication` (Codex Finding 1 carry) + Sprint 21 escalation rationale + Sprint 23 URL policy + factual narrowing. **Scope discipline**: NO `escalation_reason` enum widening; sibling fields only; preserve `D-new-escalation-reason-enum` deferral.
- **M2 parallel-track**: M2-C — per-LLM-call latency characterization (`R-llm-provider-latency-drift-2026-05-16`).
- **M2-B → M3** unless Salesforce cutover calendar pressure surfaces.
- **M2-D → deferred to M3+** — `R-loosen-topic-uc-binding-llm-owned-drift` requires research-agent investigation before scoping; the constitution-discipline §7.2-drop decision (2026-05-17) is too fresh to reverse.

### 12.6 Calibration baseline for M2 planning (per §9)

Actual M1 duration: planning round 2026-05-16 → M1 close 2026-05-17 (less than 2 days). Three sub-sprints shipped in this window; the milestone framework's cadence overhead reduction was as advertised (one milestone-shared Codex review at close vs three per-sub-sprint reviews). The 2-3 week estimate in §9 above was conservative — actual was significantly faster, driven by (a) clean per-sub-sprint scope discipline, (b) the constitution-discipline §7.2 revert avoiding scope drift, (c) cross-validated research-agent round resolving M2 selection in one round.

M2 planning estimate: 1-2 weeks for M2-A∪M2-E sub-sprints (4-5 sub-sprints; first is the grounding-discipline anchor; subsequent sub-sprints layered per M2 milestone objective) + M2-C parallel-track (~2-3 days infra work).
