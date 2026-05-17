---
title: Sprint 34 objective — Intake field prefill UC-G/H/I/J (M1 sub-sprint 2)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-033-objective.md]
superseded_by: docs/sprints/sprint-035-objective.md
notes: >
  Sprint 34 is the second sub-sprint of Milestone M1 (DISCOVER + Intake)
  per `docs/milestone_objective.md`. Layer `skill_state` per
  `docs/current/iteration_governance.md` §3.2 Q4 (multi-tool / multi-turn
  flow state — intake fields losing state across the form-context → first
  turn boundary on UC-G/H/I/J today). §7 stanza REQUIRED; Codex review
  deferred to M1 milestone-shared close per §4.3 default (no Tier-0
  candidate, no §1.7 red line, no hard-fence violation expected). Sprint
  34 ships the Sprint 7 §J0 UC-K extractor pattern to UC-G / UC-H / UC-I /
  UC-J, consuming the alias entries already defined in
  `IntakeFieldsRegistry.java:75-105`. Note the reframing in §2: Sprint 33
  closed Alice's D1 dimension on closure-criterion (a) with a single
  real-LLM run; Sprint 34 still ships for D2 regression-depth coverage
  across the broader UC-G/H/I/J intake surface, NOT as an Alice-gated
  fix.
---

# Sprint 34 — Intake field prefill UC-G/H/I/J

**Milestone:** M1 (DISCOVER + Intake) sub-sprint 2 of 3-4 per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Implementation sub-sprint, single track, semantic-touching.** Layer: `skill_state` per `docs/current/iteration_governance.md` §3.2 Q4 (multi-turn / multi-tool flow state is being lost — `IntakeFieldExtractor.java:21-35` today supports UC-K only, so a UC-G/H/I/J session re-asks the user for `ad_id_or_listing_url` / `registered_email` / `report_target` / `transaction_reference` fields the form context already supplied).

**§7 stanza REQUIRED** (see §9 below).
**Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. No per-sub-sprint Codex trigger expected.

## 2. Goal + reframing note

**Goal.** Extend `IntakeFieldExtractor` from UC-K-only to also handle UC-G / UC-H / UC-I / UC-J, consuming the form-context aliases already defined in `IntakeFieldsRegistry.java:75-105`. When intake IS the correct path, the bot does NOT re-ask the user for fields the form context already supplied (`form_context.ad_id` → `ad_id_or_listing_url`, `form_context.email` → `registered_email`, and where applicable `form_context.description` → narrow seed for `stated_reason_or_context` / `description` / `dispute_reason`).

**Reframing note (Sprint 33 close 2026-05-16).** Sprint 33's real-LLM rerun against the Alice bad case PASSED closure-criterion (a) (`docs/sprints/sprint-033-handoff.md` §5). The original M1 plan predicted Sprint 33 alone would be IMPROVING on Alice with Sprint 34 needed to close D2; in fact the bot never entered intake, so Sprint 34 is NOT gated on Alice's D2 dimension. **Sprint 34 still ships** for three reasons:

1. **Single-run evidence is thin.** Alice PASS came from one real-LLM run; the M1 milestone-close rerun across multiple traces is the durable evidence layer. If LLM variance shifts the bot toward UC-H on a re-roll, the intake-prefill is the regression guard.
2. **D2 surface is broader than Alice.** UC-G (GDPR / data deletion), UC-I (refund / payment dispute), UC-J (trust & safety report) are independent intake paths whose form-context-supplied fields are also re-asked today. The closure case is "any UC-G/H/I/J session with intake path that has form_context-supplied fields does not re-ask for them" — Alice is one of many.
3. **Soft-signal architecture coherence.** The Sprint 33 `discover_disambiguation_signals` slot tells the LLM the surface IS ambiguous; without Sprint 34, even a correctly-classified UC-H session re-asks for fields the form already supplied. The two slots are complementary, not redundant.

Sprint 34 closes the D2 dimension of the Alice bad case **as a side benefit**, not as the primary acceptance bar.

## 3. Non-goals (explicit)

- Sprint 34 does NOT touch `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`, `PhaseEvaluator.java` semantic surfaces.
- Sprint 34 does NOT touch `INTAKE_UCS` set at `PhaseEvaluator.java:30` (Sprint 36 conditional scope).
- Sprint 34 does NOT widen `escalation_reason` enum at `PhaseEvaluator.java:39-63` (M2 candidate).
- Sprint 34 does NOT change the `IntakeFieldsRegistry` schema: the required-fields list per UC at `IntakeFieldsRegistry.java:53-67` and the alias map at `IntakeFieldsRegistry.java:78-104` stay byte-identical. Sprint 34 only CONSUMES the existing alias definitions.
- Sprint 34 does NOT touch the existing UC-K extraction code path (`extractUcKFields`, `capturePlatform`, `captureRegressionText` helpers, `PLATFORM_TOKEN_PATTERN`, `REGRESSION_MARKER_PATTERN`). Existing behaviour MUST remain byte-identical (verified via the existing `Sprint71PartialIntakePersistenceTest` continuing to pass unchanged).
- Sprint 34 does NOT touch `AgentRunLoopImpl.persistInlineIntakeFields` (the LLM-supplied path) or the `intake_complete` guard predicate at `AgentRunLoopImpl.java:611+`. The LLM-supplied path still wins over the extractor path per the `mergeForUc` "skip if existing" rule at `IntakeFieldExtractor.java:204-209`.
- Sprint 34 does NOT touch `eval_interactive/eval_interactive/` (harness, loader, simulator).
- Sprint 34 does NOT add CaseSpecs to existing case families (Sprint 20 / Sprint 29 / Sprint 32 cascade fence) and does NOT edit `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` or any existing bad case.
- Sprint 34 does NOT touch `eval_interactive/case_spec_overrides.yaml`.
- Sprint 34 does NOT add a Tier-0 invariant.
- Sprint 34 does NOT add a regex / keyword / per-UC if-else matrix on user content for routing decisions. Per-UC extraction branches that read FORM-CONTEXT JSON fields by canonical key (form_context.ad_id, form_context.email, form_context.description) are NOT semantic hardcodes — they are field-mapping plumbing per §1.4 (Runtime owns persistence / trace contract). See §8 stanza for the explicit anti-hardcode walk.
- Sprint 34 does NOT close OQ1 / OQ3 from `docs/sprints/sprint-033-handoff.md` §7 (those are M2 candidates).

## 4. Premise check (deliver-agent verified 2026-05-16)

Verified at HEAD post-Sprint-33-close (cumulative commit range includes Sprint 33 commit; deliver-agent will verify Sprint 33 has been committed before dev session starts):

1. **`IntakeFieldExtractor.java:85-99`** — `extractFromTurn` currently has a single `if ("UC-K".equals(uc))` branch. `handlesUc` at lines 220-222 returns true only for UC-K. Sprint 34 extends both.
2. **`IntakeFieldsRegistry.java:53-67`** — required-fields map covers UC-G `[registered_email, data_request_type]`, UC-H `[ad_id_or_listing_url, registered_email, stated_reason_or_context]`, UC-I `[transaction_reference, dispute_reason]`, UC-J `[report_target, report_type, description]`, UC-K `[platform, repro_steps_or_error_message]`.
3. **`IntakeFieldsRegistry.java:78-104`** — alias map already defines: UC-H `ad_id → ad_id_or_listing_url`, `listing_url → ad_id_or_listing_url`, `email → registered_email`, `appeal_reason → stated_reason_or_context`, `reason → stated_reason_or_context`; UC-J `target → report_target`, `report_about → report_target`, `type → report_type`, `issue_type → report_type`; UC-I `transaction_id → transaction_reference`, `payment_reference → transaction_reference`, `dispute_description → dispute_reason`; UC-G `request_type → data_request_type`, `data_action → data_request_type`.
4. **`FormContextIngestionService.java:48-85`** — form_context JSON fields written at session start: `first_name`, `email`, `topic_subject`, `description`, optionally `ad_id`. These are the SOURCE-OF-TRUTH fields the extractor reads.
5. **`AgentRunLoopImpl.java:557-577` (`mergePartialIntakeFromContext`)** — the merge gate at line 561 (`if (!IntakeFieldExtractor.handlesUc(uc)) return;`) is the SINGLE plumbing point Sprint 34 needs the extractor to unlock for UC-G/H/I/J. Once `handlesUc` returns true for those UCs, the existing run-loop plumbing fires automatically. Per §5 the merge gate edit is ONE-LINE and the existing test `Sprint71PartialIntakePersistenceTest` must continue to pass unchanged.
6. **`Sprint71PartialIntakePersistenceTest.java`** — UC-K reference test pattern: extractor unit contracts + run-loop merge tests + projection-flip tests + intake-complete guard tests. Sprint 34's regression test mirrors this shape per-UC.
7. **Alice bad case D2 dimension** — `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` `bad_case_metadata.related_dimensions: D2 (intake prefill gap)` confirms Alice's form_context carries `ad_id: AD-2001` and `email: alice.removed@example.com`; these are the fields that today would be re-asked if the bot committed UC-H.

Sprint 34 dev SHALL re-verify these at session start by reading the cited files (one read each; one short paragraph in handoff §3). If any premise has drifted between 2026-05-16 and dev session start, STOP and surface; do NOT code under a false premise.

## 5. Files in scope (Sprint 34 dev ships)

| path | change type |
|------|-------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldExtractor.java` | EDIT (1) extend `extractFromTurn` with per-UC branches for UC-G / UC-H / UC-I / UC-J (one `else if` per UC, each calling a new per-UC `extractUcXFields` helper); (2) add four helper methods: `extractUcGFields`, `extractUcHFields`, `extractUcIFields`, `extractUcJFields`; (3) add a new helper `extractFormContextField(formContextJson, fieldName, objectMapper)` that pulls an arbitrary field (e.g., `ad_id`, `email`) from form_context JSON (mirrors the existing `extractFormDescription` helper at lines 169-183); (4) extend `handlesUc` to return true for UC-G/H/I/J/K. NO touch to existing UC-K helpers / patterns. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` | EDIT (if needed): the merge-gate plumbing at `mergePartialIntakeFromContext` (lines 557-577) likely needs zero change — once `handlesUc(uc)` returns true for UC-G/H/I/J the existing code path fires. The dev SHALL verify by running the new per-UC integration tests; if a non-obvious wiring issue surfaces, document it in handoff §4 and surface the diff. Default expectation: NO change to this file. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillExtractorTest.java` | NEW unit-test file. Per-UC test groups (G/H/I/J × ~4-6 fire/no-fire variants per UC). Mirrors `Sprint71PartialIntakePersistenceTest` extractor-unit-contract shape (e.g., `extractor_capturesPlatformFromUserReply` per UC equivalent). Target: ~20-30 unit tests total. Asserts: per-UC fire condition with form_context-supplied fields; per-UC no-fire when form_context lacks the field; canonical-name normalisation via existing alias map (no schema-edit assertion); UC-K behaviour byte-identical (one assertion calling the existing UC-K extractor path with the canonical fixture to prove no regression). |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillProjectionAndGuardTest.java` | NEW unit-test file. Per-UC projection-flip tests: form_context-seeded fields appear in `intake_state.fields_collected` after one merge call, `fields_remaining` shrinks by the corresponding count. Plus per-UC `intake_complete_for_uc_X` guard tests: when extractor seeds enough fields (UC-G: email + data_request_type if user supplies the type on turn 1; UC-H: ad_id + email seeded, user supplies stated_reason on turn 1; etc.), the guard predicate flips true. Mirrors `Sprint71PartialIntakePersistenceTest` projection-flip + guard shape. Target: ~12-16 tests. |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopUcGHIJIntakePrefillIntegrationTest.java` | NEW integration test (parameterized per Sprint 31 fix-iteration #2 T8 / Sprint 33 non-enforcement integration test precedent). 4-8 parameterised variants × ~5 invariance bars (form-context seeded → first user turn → next-turn projection shows fields_collected populated → bot does NOT ask for the prefilled fields). Asserts: TerminalOutcome unchanged on negative-control UC-A/B/C/D/F (no extractor change for FAQ-path UCs); intake_state projection populates fields_collected as expected per UC; no regression on UC-K (existing reference case still passes). |

### 5.1 Sub-sprint-close artefacts (deliver-agent owned, M1 milestone-scoped)

| path | change type |
|------|-------------|
| `docs/sprints/sprint-034-handoff.md` | NEW (12-section dev-authored archive per Sprint 31 / Sprint 32 / Sprint 33 shape) |
| `docs/sprint_objective.md` | EDIT after Sprint 34 close (deliver-agent replaces with Sprint 35 contract; archives this contract to `docs/sprints/sprint-034-objective.md`) |
| `docs/10-handoff.md` §1 lead | EDIT (deliver-agent demotes Sprint 34 to "current sub-sprint completed", sets Sprint 35 as current sub-sprint) |
| `docs/action_bank.md` | EDIT only if Sprint 34 surfaces a new R-item or closes an existing one (otherwise unchanged; M1 milestone-close action_bank flips happen at M1 close, not sub-sprint close) |
| `docs/codex-findings.md` | UNCHANGED at Sprint 34 close (Codex deferred to M1 milestone-shared close per §4.3 default) |
| `compact/sprint-034-dev-prompt.md` | deliver-agent-owned (authored 2026-05-16; NOT staged by dev) |
| `compact/M1-review-prompt.md` | deliver-agent-owned, drafted at M1 close (NOT this sub-sprint) |

## 6. Files NOT in scope (hard fences)

1. **No edits to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` / `PhaseEvaluator.java`** — DISCOVER classifier surface and phase machine stay unchanged.
2. **No edits to `INTAKE_UCS` set at `PhaseEvaluator.java:30`** — Sprint 36 (conditional) scope.
3. **No edits to `escalation_reason` enum at `PhaseEvaluator.java:39-63`** — M2 candidate.
4. **No edits to `IntakeFieldsRegistry.java`** — required-fields map and alias map are stable; Sprint 34 consumes them, does not edit them.
5. **No edits to the existing UC-K extractor path** (`extractUcKFields`, `capturePlatform`, `captureRegressionText`, `PLATFORM_TOKEN_PATTERN`, `REGRESSION_MARKER_PATTERN`). Existing UC-K test suite must continue to pass byte-identical.
6. **No edits to `Sprint71PartialIntakePersistenceTest.java`** — the existing UC-K test is the regression guard for "Sprint 34 did not break UC-K".
7. **No edits to `ContextProjectionBuilder.java`** — Sprint 33 shipped its only Sprint-33-attributable touch (`discover_disambiguation_signals`). The `intake_state` projection slot already consumes `IntakeFieldsRegistry.parseCollectedFields` + `IntakeFieldsRegistry.fieldsRemaining`, which automatically reflect Sprint 34's new prefilled fields with zero projection-builder change.
8. **No edits to `FormContextIngestionService.java`** — the form_context JSON schema (first_name, email, topic_subject, description, optionally ad_id) is the source-of-truth Sprint 34 reads from. No schema change.
9. **No edits to `system_prompt.txt`** — Sprint 34 changes Java extraction plumbing, not LLM-facing teaching surfaces.
10. **No edits to `eval_interactive/eval_interactive/`** — harness/loader/simulator stable.
11. **No edits to existing case families** under `eval_interactive/case_specs/case_families/` (cascade fence).
12. **No edits to `eval_interactive/case_specs/smoke/`** and **no edits to `eval_interactive/case_specs/bad_cases/`** (the bad-case suite is governance-tracked; Sprint 34 contributes evidence to M1 close manual review, does not edit suite contents).
13. **No edits to `eval_interactive/case_spec_overrides.yaml`**.
14. **No edits to foundational docs** under `docs/foundational/`.
15. **No edits to `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md`**.
16. **No edits to sprint archives** under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-033-*`.
17. **No edits to `docs/milestone_objective.md`** — the M1 contract stays stable across sub-sprints (deliver-agent updates only at M1 close).
18. **No new Tier-0 invariant**. No edits to `docs/runtime_freeze_and_risk_policy.md`.
19. **No regex / keyword / per-UC matrix on user content for routing decisions**. Per-UC FORM-CONTEXT field mapping (form_context.ad_id → ad_id_or_listing_url) is field-mapping plumbing, NOT a semantic hardcode; see §8 stanza for the explicit anti-hardcode walk and §4.1 nine-question pre-walk.
20. **No mocked-LLM as primary evidence** for any LLM-behaviour claim per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`. Sprint 34's primary evidence is Java unit + integration tests (extractor is Java-deterministic plumbing, not LLM-facing); a Sprint 34-attributable Alice rerun is OPTIONAL informational evidence only (because Alice closed on (a) without intake, Sprint 34 may not exercise the prefill path on Alice — UC-G/I/J synthetic traces are the regression surface).

## 7. Bundle policy

- Single track, single feature (per-UC extractor extension for UC-G/H/I/J + one-line `handlesUc` extension + per-UC tests + integration test). All §5 file changes land in one dev commit.
- Smoke rerun on the 14-case set IS NOT required at Sprint 34 close (smoke composite_score is observation only per §5.5; M1 milestone close rerun will cover it). However, if the dev opportunistically runs smoke and observes a regression > 10% beyond noise, surface in handoff §7 as informational.
- **Bad-case suite rerun on Alice case IS recommended (not required) at Sprint 34 close** as informational evidence. Alice already PASSED on Sprint 33; a re-run after Sprint 34 should remain PASS (regression guard) AND, if the LLM happens to commit UC-H this time, the intake prefill should fire (alice's form_context.ad_id=AD-2001 + email=alice.removed@example.com should populate as fields_collected). If LLM variance keeps Alice on UC-A FAQ path again, that's also fine — the Java unit+integration tests are the load-bearing evidence for Sprint 34.
- Java test suite SHALL run clean: zero new regressions, only the pre-existing inherited `SystemPromptUserRequestedTiebreakerTest` failure carried since Sprint 24-era. Net delta vs Sprint 33 baseline (932 / 1-inherited / 0 / 2): +M new tests from Sprint 34 (count per §5 expected ~32-46 new tests).
- **Existing `Sprint71PartialIntakePersistenceTest` MUST continue to PASS unchanged** — this is the regression guard for "UC-K behaviour is byte-identical after Sprint 34".

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `skill_state` per `docs/current/iteration_governance.md` §3.2 Q4. A multi-tool / multi-turn flow is losing state across the form-context → first-turn boundary on UC-G/H/I/J: the user supplied `ad_id` + `email` in the form, the LLM committed UC-H, but `IntakeFieldExtractor` today only seeds UC-K's `platform` + `repro_steps_or_error_message` — so the next-turn `intake_state.fields_remaining` projection shows all UC-H fields remaining, and the LLM asks the user to retype data the form already collected. Sprint 34 closes this state durability gap.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The intake-state durability is governed by Runtime's "persistence + trace and eval contract" responsibility per Constitution §1.4; the LLM still owns the classify_use_case decision per §1.3.

**Semantic hardcode:** No semantic hardcode introduced. Sprint 34 ships per-UC FORM-CONTEXT field mapping (e.g., for UC-H: read `form_context.ad_id` → canonical `ad_id_or_listing_url`; read `form_context.email` → canonical `registered_email`). This is field-mapping plumbing, not semantic decision-making:

- The MAPPING SOURCE is the `IntakeFieldsRegistry.FIELD_ALIASES` map at `IntakeFieldsRegistry.java:78-104`, which already defines `ad_id → ad_id_or_listing_url` and `email → registered_email`. Sprint 34 CONSUMES the existing alias definitions; it does not add new aliases.
- No regex on user-typed content. The existing UC-K helpers `capturePlatform` / `captureRegressionText` (which DO regex user text) stay UC-K-scoped per §6 hard fence #5. UC-G/H/I/J extractors read form_context JSON fields by canonical key, never inspecting user-typed words.
- No per-UC if-else for routing or escalation. The UC selection (`if ("UC-H".equals(uc))`) is plumbing — it tells the extractor which form_context keys to read for THAT UC. It does NOT make a semantic classification decision; classify_use_case (Sprint 33 + earlier) has already made that decision before `mergePartialIntakeFromContext` fires.
- The §4.1 nine-question walk verdict (pre-walked by deliver-agent): `approve`. Q1 (keyword/regex/if-else for semantic decision?) — NO; per-UC plumbing branches map form_context keys to canonical names. Q5 (move semantic ownership from LLM to Java?) — NO; LLM still owns the classification. Q6 (if-else block in prompt?) — N/A; Sprint 34 ships no prompt change.

**Generalization coverage:**

- **Target:** intake-path UC sessions (UC-G/H/I/J) whose form_context supplies one or more canonical-alias fields. Closure check at Sprint 34 close: per-UC unit + integration tests PASS; the next-turn `intake_state.fields_collected` reflects the prefilled fields.
- **Neighbor:** UC-K (existing) regression-guarded by `Sprint71PartialIntakePersistenceTest` continuing to pass unchanged. UC-G/H/I/J cross-pollination by parameterising the integration test across all four UCs.
- **Negative:** FAQ-path UCs (UC-A/B/C/D/F/FP) where `mergePartialIntakeFromContext` SHALL no-op per the existing `IntakeFieldsRegistry.isIntakeUseCase` gate at `AgentRunLoopImpl.java:560`. Verified by integration test parameterising a UC-A session and asserting no `intake_state.fields_collected` writes.
- **Shadow:** held-out LLM-behaviour validation deferred to M1 milestone-close manual review of `case_specs/bad_cases/` traces (including Alice). Sprint 34 ships the Java regression coverage; the LLM-behaviour validation is M1-scoped.

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (must pass for Sprint 34 close):**

- All §5 file changes land in one dev commit.
- New per-UC `Sprint34IntakePrefillExtractorTest` (~20-30 tests) PASS.
- New per-UC `Sprint34IntakePrefillProjectionAndGuardTest` (~12-16 tests) PASS.
- New `AgentRunLoopUcGHIJIntakePrefillIntegrationTest` (4-8 parameterised variants) PASS.
- Existing `Sprint71PartialIntakePersistenceTest` continues to PASS unchanged (UC-K regression guard).
- Full server suite no new regressions vs Sprint 33 baseline (932 / 1-inherited / 0 / 2 + Sprint 34 new tests).
- Anti-hardcode self-walk: handoff §8 walks the §4.1 nine questions; expected verdict `approve` (no semantic hardcode; per-UC plumbing, not per-UC routing).
- Reproducibility: every quantitative claim in handoff cites source path + extraction recipe per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

**Primary gate (M1-scoped, not Sprint-34-blocking but tracked):**

- Sprint 34 contributes to the M1 milestone-close bad-case suite rerun evidence pool. Alice case is expected to remain PASS on (a) (Sprint 33 carryover); if LLM variance flips the bot toward UC-H, Sprint 34's prefill should fire and Alice should land on closure-criterion (c) (graceful escalation with `reason=user_requested` / `ambiguous_intent` instead of `intake_complete_for_uc_h` lie). Either path is M1-acceptable.

**Observations (not gates):**

- Smoke composite_score and friends per §5.5 (demoted to observation 2026-05-16).
- Architecture-health metrics direction (§6): `new_semantic_hardcode_count` SHOULD be 0; `soft_signal_conversion_count` should be ≥ 0 (Sprint 34 is plumbing, not soft-signal conversion); `planner_ownership_ratio` should not decrease.

**Codex review:** deferred to M1 milestone close per §4.3 default. Sprint 34 dev does NOT dispatch Codex; deliver-agent + human do at M1 close.

## 10. Stop conditions (dev-agent)

STOP and report (do NOT silently work around) when:

1. **Premise drift on §4 items** — any of the 7 premises has changed since 2026-05-16. STOP and surface in handoff §3.
2. **Tempted to edit `IntakeFieldsRegistry`** schema (add a new alias, change the required-fields list, add a new UC) — STOP. Out of Sprint 34 scope; if the dev session surfaces a missing alias that blocks a Sprint 34 test fixture, document it as an OQ for M1 close (deliver-agent + human decide).
3. **Tempted to widen `escalation_reason` enum** at `PhaseEvaluator.java:39-63` — STOP. M2 candidate; not Sprint 34 scope.
4. **Tempted to add a regex on user-typed content** for UC-G/H/I/J extraction (e.g., mining the user's first message for an ad ID or email) — STOP. The form_context is the source of truth; if the user typed an ad ID inline (not in the form), the LLM owns surfacing it via `intake_fields` on `request_handover` per `persistInlineIntakeFields` plumbing. Inline user-content extraction is a §1.7-adjacent decision that needs deliver-agent + human review.
5. **Tempted to touch `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` / `ClassifyUseCaseTool` / `PhaseEvaluator`** — STOP. Hard fence per §6.
6. **Tempted to widen DISCOVER `allowedTools`** or `INTAKE_UCS` — STOP. Sprint 36 (conditional) scope.
7. **Java test regression** — surface in handoff §4; do NOT silently adjust the failing test. The existing `Sprint71PartialIntakePersistenceTest` is the load-bearing UC-K regression guard.
8. **`Sprint71PartialIntakePersistenceTest` shows ANY new failure** after Sprint 34 — STOP. This means a Sprint 34 helper inadvertently changed UC-K extraction behaviour. Roll back to UC-G/H/I/J-only changes and re-verify.
9. **Tempted to author new case families** (target/neighbor/negative/shadow split) — STOP. Per M1 §6 the bad-case suite is the primary acceptance gate. Java integration-test parameterization is the right shape for Sprint 34's regression coverage; new case families are out of Sprint 34 scope.
10. **Tempted to skip the integration test** because "the extractor is plumbing-only" — STOP. The integration test proves the end-to-end wiring (form ingestion → session.formContext → extractor → session.intakeFields → next-turn projection) survives the extension; the Java unit tests prove only the per-UC branches.

## 11. Handoff document contract (12 sections per Sprint 31 / Sprint 32 / Sprint 33 shape)

Standard 12-section shape:

1. Context Pack.
2. Sub-sprint-objective recap.
3. Premise re-verification (§4 spot-check; 7 premises).
4. Implementation walkthrough — each §5 file by line range; rationale for the per-UC extraction branches; explicit note on which form_context fields each UC reads.
5. Bad-case suite Alice case rerun (recommended, informational) — result path + per-turn trace + closure-criterion status. If Alice stays on UC-A FAQ path (LLM variance carries Sprint 33 outcome forward), document and confirm prefill plumbing did not regress UC-A. If Alice flips to UC-H, document whether prefill fired and which closure-criterion sub-bullet was met.
6. Generalization coverage table per §8 — target / neighbor / negative / shadow counts (Java layer).
7. Open questions for human — any item surfaced but not acted on. Each becomes a possible M1 milestone-level concern or follow-on R-item.
8. Anti-hardcode self-walk (§4.1 nine questions).
9. Files changed — table with paths + line ranges / test counts.
10. Layer-classification self-walk per §3 (lands on `skill_state`).
11. §5 Eval Acceptance bars — each gate line by line with cited evidence.
12. Closure verdict placeholder — leave for M1 milestone-shared decision per §4.3 + `feedback_handoff_verdict_section_delegation.md` (Sprint 34 itself does not close standalone; it contributes to the M1 close evidence).

## 12. M1 milestone context (cross-reference, not Sprint 34 contract)

Sprint 34 is the second of M1's 3-4 sub-sprints per `docs/milestone_objective.md`. After Sprint 34 commits:

- Deliver-agent + human review Sprint 34 handoff at sub-sprint close.
- Default next step: deliver-agent drafts Sprint 35 contract (Option β coverage probe + worked-example re-anchor decision; layer `eval_spec`), replaces `docs/sprint_objective.md`.
- If Sprint 34 surfaces a §1.7 concern (e.g., inline user-content regex temptation from Stop Condition #4) → deliver-agent + human reassess M1 sub-sprint sequence.
- If Sprint 34 reveals an `IntakeFieldsRegistry` schema gap blocking UC-G/I extraction → deliver-agent + human decide whether Sprint 35 expands to cover registry schema work or whether a separate sub-sprint is needed.
- M1 milestone close happens after all M1 sub-sprints (33 + 34 + 35 + optionally 36) complete; deliver-agent + human dispatch milestone-shared Codex review at that point per §4.3 default.
