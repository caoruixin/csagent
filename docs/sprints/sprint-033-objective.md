---
title: Sprint 33 objective — DISCOVER UC-A/FP/H soft-signal + classification guidance (M1 sub-sprint 1)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-032-objective.md]
superseded_by: docs/sprints/sprint-034-objective.md
notes: >
  Sprint 33 is the first sub-sprint of Milestone M1 (DISCOVER + Intake)
  per `docs/milestone_objective.md`. Layer `prompt_projection` per
  `docs/current/iteration_governance.md` §3.2 Q3. §7 stanza REQUIRED;
  Codex review deferred to M1 milestone-shared close per §4.3 default
  (no Tier-0 candidate, no §1.7 red line, no hard-fence violation
  expected). Sprint 33 addresses the Alice bad case D1 dimension
  (UC-A vs UC-H DISCOVER mis-classification) by projecting a soft
  signal that REMOVED-listing + Ad Support topic yields multiple
  plausible UC candidates, AND extending the DISCOVER
  `systemInstruction` with principle-level disambiguation guidance
  for ad-status UCs (NOT a regex on user content, NOT a per-UC
  matrix). The LLM owns the classification decision; runtime
  surfaces evidence.
---

# Sprint 33 — DISCOVER UC-A/FP/H soft-signal + classification guidance

**Milestone:** M1 (DISCOVER + Intake) sub-sprint 1 of 3-4 per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Implementation sub-sprint, single track, semantic-touching.** Layer: `prompt_projection` per `docs/current/iteration_governance.md` §3.2 Q3 (the LLM is choosing validly within the available UC options at DISCOVER, but the projection currently lacks (a) the diagnostic signal that REMOVED-listing creates UC ambiguity, and (b) the principle-level teaching that REMOVED-listing visibility questions can be UC-A FAQ-resolvable not just UC-H intake commitments).

**§7 stanza REQUIRED** (see §9 below).
**Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. No per-sub-sprint Codex trigger expected.

## 2. Goal

Make the bot recognize that REMOVED-listing visibility questions deserve disambiguation between UC-A (FAQ-resolvable visibility/reason query) and UC-H (intake-path appeal) before committing. Concretely:

- **Projection signal:** add `discover_disambiguation_signals` (or similar) projection field that surfaces "REMOVED-listing observed AND topic_subject is Ad Support" as a soft signal, with the candidate UC pair `[UC-A, UC-H]` (or `[UC-A, UC-FP, UC-H]` per registry) listed for the LLM to read. This is OBSERVABLE evidence the LLM may use; runtime does NOT enforce or branch on it.
- **DISCOVER prompt teaching:** extend `PhaseEvaluator.java:411-431` DISCOVER `systemInstruction` with one principle-level paragraph: "when a REMOVED listing is observed AND the user's question is about visibility / 'why' / 'can't see', the user may want EITHER to know WHY the ad was removed (UC-A FAQ-resolvable, has reason-explanation surface in KB) OR to appeal the removal (UC-H intake-only path). If ambiguous, ask ONE focused clarifying question before classify_use_case commits — for example: 'Do you want to know the reason it was removed, or do you want to appeal the removal?' Treat the user's literal request as the disambiguation signal; do not commit UC-H purely on listing_context.status=REMOVED alone."
- **System prompt teaching paragraph:** sibling to the existing `already_called` (Sprint 23) and `alternate_candidate_use_cases` (Sprint 31) paragraphs in `system_prompt.txt`, add a teaching paragraph naming the new `discover_disambiguation_signals` projection field, its provenance (runtime observation of listing_context + topic_subject), and the soft-signal posture (LLM owns the read decision; runtime does not enforce; empty when no signal fires).

## 3. Non-goals (explicit)

- Sprint 33 does NOT touch `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`. The classifier's per-turn semantic surface stays unchanged; only the projection that feeds it expands.
- Sprint 33 does NOT add a Java decision-path branch on `listing_context.status` or `topic_subject` for routing. The decision-path branch IS the soft signal IS the §1.7 forbidden pattern; Sprint 33 ships projection + prompt teaching only.
- Sprint 33 does NOT widen the DISCOVER `allowedTools` for any UC. UC-H still has `request_handover` only after commitment per `PhaseEvaluator.java:30 INTAKE_UCS`. (The "escape" from a wrong UC-H commitment is the Sprint 36 conditional concern; Sprint 33 prevents the wrong commitment at DISCOVER.)
- Sprint 33 does NOT touch `IntakeFieldExtractor.java`. UC-G/H/I/J intake prefill is Sprint 34 scope.
- Sprint 33 does NOT touch `eval_interactive/eval_interactive/` (harness, loader, simulator).
- Sprint 33 does NOT add CaseSpecs to existing case families (Sprint 20 / Sprint 29 / Sprint 32 cascade fence). Sprint 33 MAY add regression-test CaseSpec(s) under a new directory if needed for end-to-end validation — but per M1 §6 the bad-case suite is the primary acceptance gate, not new case families.
- Sprint 33 does NOT close `R-option-beta-coverage-gap-uc-a-uc-c-shape` (that's Sprint 35 scope).
- Sprint 33 does NOT touch `eval_interactive/case_spec_overrides.yaml`.
- Sprint 33 does NOT add a Tier-0 invariant.

## 4. Premise check (deliver-agent verified 2026-05-16)

Verified at HEAD post-Sprint-32-close:

1. `PhaseEvaluator.java:411-431` carries the DISCOVER `systemInstruction` building block with the Sprint 7 §I0 weak-candidate cue (UC-F payment / UC-B advertising / UC-C messaging / UC-D account-login). NO UC-A vs UC-H disambiguation guidance. NO REMOVED-listing-specific guidance. Confirmed via Read.
2. `ContextProjectionBuilder.java` projection slots: `candidate_use_cases` (existing), `alternate_candidate_use_cases` (Sprint 31), `already_called` (Sprint 20 Track B), `intake_state` (Sprint 7 §I0 + Sprint 10 §L2 + Sprint 11). NO `discover_disambiguation_signals` slot exists.
3. `FormContextIngestionService.java:48-85` writes `form_context`, `listing_context` (via `GetCustomerContextTool` auto-trigger), `moderation_context` to `BotSession`. `listing_context.status` is one of `LIVE / PROCESSING / REMOVED / SUSPENDED / EXPIRED` per `MockGumtreeApiService.java` enum. Confirmed `REMOVED` is the value observed in the Alice bad case (`bad_cases/alice_uc_a_uc_h_misclass.yaml` form_context).
4. `system_prompt.txt:23-28` carries the Sprint 23 `already_called` teaching paragraph; `system_prompt.txt:30-34` carries the Sprint 31 `alternate_candidate_use_cases` teaching paragraph. Sprint 33's new paragraph should be sibling-placed adjacent to these per the Sprint 31 OQ2 pre-pick pattern.
5. `useCaseRegistry` (`UseCaseRegistry.java`) — the topic-subject → candidate-UC mapping. Confirmed "Ad Support" topic maps to `[UC-A, UC-B, UC-FP, UC-H]` candidate list per the Sprint 32 dev's empirical observation in `cs_interactive_015` reference smoke output.

Sprint 33 dev SHALL re-verify these at session start by reading the cited files (one read each; one short paragraph in handoff §3). If any premise has drifted between 2026-05-16 and dev session start, STOP and surface; do NOT code under a false premise.

## 5. Files in scope (Sprint 33 dev ships)

| path | change type |
|------|-------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | NEW projection slot `discover_disambiguation_signals` adjacent to existing `alternate_candidate_use_cases` slot. Built from `session.getListingContext().getStatus()` + `session.getFormTopicSubject()` + `useCaseRegistry.getCandidateUcsForTopic(topic)`. Schema-stable across turns (always-present empty-object shape when no signal fires). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | EDIT DISCOVER `systemInstruction` at lines 411-431 area — append principle-level disambiguation paragraph for ad-status UCs. NO keyword/regex; principle-level teaching only. |
| `server/src/main/resources/prompts/system_prompt.txt` | NEW teaching paragraph sibling to `already_called` (lines 23-28) and `alternate_candidate_use_cases` (lines 30-34). Names the new slot, provenance, soft-signal posture. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/DiscoverDisambiguationSignalsProjectionTest.java` (or similar) | NEW regression test, 4-6 tests, mirroring `IntakeAmbiguousCandidatesProjectionTest` shape from Sprint 31. Asserts: signal fires on REMOVED+Ad-Support combo; signal does not fire on LIVE+Ad-Support; signal does not fire on REMOVED+non-Ad-Support; schema stability across turns; runtime does NOT branch on the signal value (non-enforcement). |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest.java` (or similar) | NEW integration test (parameterized per Sprint 31 fix-iteration #2 T8 precedent). 4-6 variants × 5 invariance bars (TerminalOutcome, llm call count, tool dispatch, final message identity, projection slot value). Proves runtime does NOT gate any dispatch / phase transition / escalation on the new signal. |

### 5.1 Sub-sprint-close artefacts (deliver-agent owned, M1 milestone-scoped)

| path | change type |
|------|-------------|
| `docs/sprints/sprint-033-handoff.md` | NEW (12-section dev-authored archive per Sprint 31 / Sprint 32 shape) |
| `docs/sprint_objective.md` | EDIT after Sprint 33 close (deliver-agent replaces with Sprint 34 contract; archives this contract to `docs/sprints/sprint-033-objective.md`) |
| `docs/10-handoff.md` §1 lead | EDIT (deliver-agent demotes Sprint 33 to "current sub-sprint completed", sets Sprint 34 as current sub-sprint) |
| `docs/action_bank.md` | EDIT only if Sprint 33 surfaces a new R-item or closes an existing one (otherwise unchanged; M1 milestone-close action_bank flips happen at M1 close, not sub-sprint close) |
| `docs/codex-findings.md` | UNCHANGED at Sprint 33 close (Codex deferred to M1 milestone-shared close per §4.3 default) |
| `compact/sprint-033-dev-prompt.md` | deliver-agent-owned (authored 2026-05-16; NOT staged by dev) |
| `compact/M1-review-prompt.md` | deliver-agent-owned, drafted at M1 close (NOT this sub-sprint) |

## 6. Files NOT in scope (hard fences)

1. **No edits to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java`** — DISCOVER classifier surface stays unchanged.
2. **No edits to `IntakeFieldExtractor.java`** — Sprint 34 scope.
3. **No edits to `INTAKE_UCS` set at `PhaseEvaluator.java:30`** — Sprint 36 (conditional) scope.
4. **No edits to `escalation_reason` enum at `PhaseEvaluator.java:39-63`** — M2 candidate.
5. **No edits to `eval_interactive/eval_interactive/`** — harness/loader/simulator stable.
6. **No edits to existing case families** under `eval_interactive/case_specs/case_families/` (cascade fence).
7. **No edits to `eval_interactive/case_specs/smoke/`**.
8. **No edits to `eval_interactive/case_spec_overrides.yaml`**.
9. **No edits to foundational docs** under `docs/foundational/`.
10. **No edits to `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md`**.
11. **No edits to sprint archives** under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-032-*`.
12. **No edits to `docs/milestone_objective.md`** — the M1 contract stays stable across sub-sprints (deliver-agent updates only at M1 close).
13. **No new Tier-0 invariant**. No edits to `docs/runtime_freeze_and_risk_policy.md`.
14. **No `FormContextIngestionService.java:48-85` allow-set widening** for UC-H auto-trigger (Sprint 34 may revisit).
15. **No regex / keyword / per-UC matrix in production code**. Per §1.7 forbidden list, the principle-level prompt teaching is the only "guidance" surface.
16. **No mocked-LLM as primary evidence** for any LLM-behaviour claim per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`. Primary evidence is the Sprint 33 dev run against Alice bad case in real-LLM mode (or M1 milestone-close run).

## 7. Bundle policy

- Single track, single feature (DISCOVER soft signal + DISCOVER prompt teaching + system_prompt teaching paragraph). All §5 file changes land in one dev commit.
- Smoke rerun on the 14-case set IS NOT required at Sprint 33 close (smoke composite_score is observation only per §5.5; M1 milestone close rerun will cover it). However, if the dev opportunistically runs smoke and observes a regression > 10% beyond noise, surface in handoff §7 as informational.
- **Bad-case suite rerun on Alice case IS required at Sprint 33 close** (per `iteration_governance.md` §5.6 new primary gate). Run via `uv run eval-interactive run --path case_specs/bad_cases/`. Manual review the per-turn trace and document closure-criterion progress (PASS / FAIL / IMPROVING per the case's `closure_criterion` field).
- Java test suite SHALL run clean: zero new regressions, only the pre-existing inherited `SystemPromptUserRequestedTiebreakerTest` failure carried since Sprint 24-era. Net delta vs Sprint 32 baseline: +N new tests from Sprint 33 (count per `IntakeAmbiguousCandidatesProjectionTest` Sprint 31 precedent).

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `prompt_projection` per `docs/current/iteration_governance.md` §3.2 Q3. The LLM today commits UC-H at DISCOVER on REMOVED-listing + Ad Support because the projection lacks (a) a diagnostic signal naming the disambiguation surface, and (b) principle-level prompt teaching that REMOVED-listing visibility questions deserve disambiguation. Sprint 33 surfaces both; the LLM owns the resulting classification decision.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The projection surface is governed by Runtime's "trace and eval contract" responsibility per Constitution §1.4; the LLM owns the DISCOVER classification per §1.3.

**Semantic hardcode:** No semantic hardcode introduced. The new `discover_disambiguation_signals` slot is a soft signal built from runtime-observable state (`listing_context.status`, `form_topic_subject`, `useCaseRegistry.getCandidateUcsForTopic(topic)`). No keyword on user content, no regex on bot output, no per-UC matrix in Java. The DISCOVER prompt teaching is principle-level ("when a REMOVED listing is observed AND the user's question is about visibility, consider asking one clarifying question"), not an if-else rule dump. The system_prompt teaching paragraph mirrors the Sprint 23 `already_called` and Sprint 31 `alternate_candidate_use_cases` shape: name the slot, name the provenance, name the soft-signal posture; no UC-specific or tool-specific branches.

**Generalization coverage:**

- **Target:** Alice bad case (`eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`) — UC-A user with REMOVED listing wants visibility info, currently mis-routed to UC-H. Closure-criterion check at Sprint 33 close + M1 milestone close.
- **Neighbor:** other REMOVED-listing + Ad Support cases where the disambiguation surface fires; the dev's regression-test integration test parameterizes ≥ 2 such shapes (Java tests; not new case families per cascade fence).
- **Negative:** LIVE listing + Ad Support (no signal should fire); REMOVED listing + non-Ad-Support topic (no signal should fire). Verified by `DiscoverDisambiguationSignalsProjectionTest` per §5.
- **Shadow:** held-out behaviour validation deferred to M1 milestone-close manual review of `case_specs/bad_cases/` traces. Sprint 33 ships the Java regression coverage; the LLM-behaviour validation is M1-scoped.

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (must pass for Sprint 33 close):**

- All §5 file changes land in one dev commit.
- New `DiscoverDisambiguationSignalsProjectionTest` (4-6 tests) PASS.
- New `AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest` (4-6 variants × 5 invariance bars) PASS.
- Full server suite no new regressions vs Sprint 32 baseline (917 / 1-inherited / 0 / 2 + Sprint 33 new tests).
- Anti-hardcode self-walk: handoff §8 walks the §4.1 nine questions; expected verdict `approve` (no semantic hardcode in production code or prompt).
- Reproducibility: every quantitative claim in handoff cites source path + extraction recipe per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

**Primary gate (M1-scoped, not Sprint-33-blocking but tracked):**

- Alice bad case (`bad_cases/alice_uc_a_uc_h_misclass.yaml`) closure-criterion progress: PASS / FAIL / IMPROVING. Sprint 33 alone may not fully close Alice (D2 intake prefill + possibly D3 INTAKE escape are also needed); IMPROVING is acceptable for Sprint 33; FULL CLOSURE at M1 milestone close.

**Observations (not gates):**

- Smoke composite_score and friends per §5.5 (demoted to observation 2026-05-16).
- Architecture-health metrics direction (§6): `new_semantic_hardcode_count` SHOULD be 0; `soft_signal_conversion_count` should be ≥ 1 (the new DISCOVER signal); `planner_ownership_ratio` should not decrease.

**Codex review:** deferred to M1 milestone close per §4.3 default. Sprint 33 dev does NOT dispatch Codex; deliver-agent + human do at M1 close.

## 10. Stop conditions (dev-agent)

STOP and report (do NOT silently work around) when:

1. **Premise drift on §4 items** — any of the 5 premises has changed since 2026-05-16. STOP and surface in handoff §3.
2. **Tempted to widen `escalation_reason` enum** at `PhaseEvaluator.java:39-63` — STOP. M2 candidate; not Sprint 33 scope.
3. **Tempted to add a Java decision-path branch** on `listing_context.status` for routing (e.g., `if status == REMOVED then force-DISCOVER-question-mode`) — STOP. This IS the §1.7 forbidden pattern; the soft signal + prompt teaching is the design.
4. **Tempted to touch `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` / `ClassifyUseCaseTool`** — STOP. Hard fence per §6.
5. **Tempted to widen DISCOVER `allowedTools`** for UC-H or any UC — STOP. Sprint 36 (conditional) scope.
6. **Java test regression** — surface in handoff §4; do NOT silently adjust the failing test.
7. **Bad-case suite Alice case shows FAIL after Sprint 33 alone** (which is expected; D2 + possibly D3 are also needed) — surface in handoff §7 as IMPROVING progress, document which closure-criterion sub-bullets are now closer to met.
8. **Tempted to author new case families** (target/neighbor/negative/shadow split) — STOP. Per M1 §6 the bad-case suite is the primary acceptance gate. Java integration-test parameterization is the right shape for Sprint 33's regression coverage; new case families are out of Sprint 33 scope.
9. **Tempted to skip the integration test** because "the soft signal is observability-only" — STOP. The integration test proves runtime non-enforcement per the Sprint 31 fix-iteration #2 T8 precedent; the parameterized invariance bars are load-bearing for the §1.7 anti-hardcode posture.

## 11. Handoff document contract (12 sections per Sprint 31 / Sprint 32 shape)

Standard 12-section shape:

1. Context Pack.
2. Sub-sprint-objective recap.
3. Premise re-verification (§4 spot-check).
4. Implementation walkthrough — each §5 file by line range; rationale for the projection field shape; rationale for the DISCOVER prompt-teaching language.
5. Bad-case suite rerun on Alice — result path + per-turn trace extraction + closure-criterion status (PASS / FAIL / IMPROVING).
6. Generalization coverage table per §8 — target / neighbor / negative / shadow counts.
7. Open questions for human — any item surfaced but not acted on. Each becomes a possible M1 milestone-level concern or follow-on R-item.
8. Anti-hardcode self-walk (§4.1 nine questions).
9. Files changed — table with paths + line ranges / test counts.
10. Layer-classification self-walk per §3 (lands on `prompt_projection`).
11. §5 Eval Acceptance bars — each gate line by line with cited evidence.
12. Closure verdict placeholder — leave for M1 milestone-shared decision per §4.3 + `feedback_handoff_verdict_section_delegation.md` (Sprint 33 itself does not close standalone; it contributes to the M1 close evidence).

## 12. M1 milestone context (cross-reference, not Sprint 33 contract)

Sprint 33 is the first of M1's 3-4 sub-sprints per `docs/milestone_objective.md`. After Sprint 33 commits:

- Deliver-agent + human review Sprint 33 handoff at sub-sprint close.
- If Alice closure-criterion is IMPROVING (expected — Sprint 33 alone addresses D1, not D2/D3): deliver-agent drafts Sprint 34 contract (intake field prefill UC-G/H/I/J), replaces `docs/sprint_objective.md`.
- If Alice closure-criterion is FAIL with no IMPROVING signal: deliver-agent + human reassess M1 sub-sprint sequence; may surface a deeper architectural concern.
- If Alice closure-criterion is PASS after Sprint 33 alone (unlikely but possible): consider closing M1 early per `iteration_governance.md` §8.5 (single-sub-sprint milestone is acceptable when acceptance bar is met) OR continue Sprint 34+ for regression guard depth.

M1 milestone close happens after all M1 sub-sprints complete; deliver-agent + human dispatch Codex review at that point per §4.3 default.
