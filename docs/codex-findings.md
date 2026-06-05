## Sprint Review Decision
decision: pass
blocking_count: 0
final_verdict: APPROVE_S_AUTO_23
human_rebless_gate: PROCEED
summary: Sprint 078 Sub-sprint A (R1.a + R2.a + R4.a, commits `084dd8b..af44903`) ships seven scoped runtime/projection repairs with zero forbidden-surface edits. R1.a declares an optional `intake_fields` schema slot and projects `required_intake_fields_for_active_uc` by iterating `IntakeFieldsRegistry` (single source of truth, no replicated per-UC matrix). R2.a wires the dead-code DISCOVER clarification counter on structural criteria only, projects `budgets.clarification` as a cardinality soft signal, and phase-guards the `max-repeated-same-action` → `clarification_budget_exhausted` re-map without adding enum values. R4.a surfaces `customer_context_status` and `ad_reference` from form_context + lookup tool events only (zero user-message content matching; load-bearing priority order and `missing`≠`skipped` verified). Fifty-three new characterization tests pass locally; file fence is clean; anti-误杀 invariants preserved by design. No semantic hardcode under §4.1; human may launch §6 real-LLM re-bless.

## §1 Per-Change Verdicts

**R1.a #1 (schema)**: approve — Adds OPTIONAL `intake_fields` as a free-form `object` with no per-UC `properties` enumeration; remains absent from `required[]` (only `escalation_reason` required). Makes the existing validator/`persistInlineIntakeFields` contract explicit without forcing non-intake UCs. Evidence: `ContextProjectionBuilder.java:262-281`; `IntakeFieldsProjectionTest.requestHandoverSchema_declaresOptionalIntakeFieldsObject`.

**R1.a #2 (per-UC required projection)**: approve — Emits `required_intake_fields_for_active_uc` by iterating `IntakeFieldsRegistry.requiredFieldsFor(activeUc)` inside `isIntakeUseCase(activeUc)` guard; omits the field entirely for null/non-intake UC (not an empty list). The guard delegates to the registry's existing `REQUIRED_FIELDS_BY_UC.containsKey` classification — structural applicability, not a replicated per-UC decision matrix. Evidence: `ContextProjectionBuilder.java:427-441`; `IntakeFieldsRegistry.java:53-67` (unchanged); `IntakeFieldsProjectionTest` positive/negative suite.

**R2.a #3 (counter wiring)**: approve — Live-path increment at no-tool-calls branch uses four AND-ed structural criteria (DISCOVER phase, zero tool calls, no UC commit vs run-start snapshot, non-empty bot reply). `userMsg = action.getUserMessage()` at `:410` is the bot's outgoing field, not the customer `run()` parameter. Predicate `isDiscoverFreeTextClarification` at `:1207-1215` takes only `botReply`. Zero content/NLP. Evidence: `AgentRunLoopImpl.java:410-458`, `:1207-1215`; `DiscoverClarificationCounterTest` (8 tests).

**R2.a #4 (budgets projection)**: approve — `budgets.clarification: {used, max}` emitted only when `session.getCurrentPhase()` is DISCOVER; cardinality from `session.getClarificationCount()` and `controlPolicy.getMaxClarificationRounds()`. LLM retains next-action ownership (§1.3). Evidence: `ContextProjectionBuilder.java:702-716`; phase-independence verified in `CustomerContextStatusProjectionTest.enumIndependentOfOtherProjectionFields`.

**R2.a #5 (phase-aware mapping)**: approve — New 3-arg overload at `ControlKernel.java:763-771` re-maps `max-repeated-same-action` → existing `clarification_budget_exhausted` ONLY when `phase==DISCOVER` AND `lastAction` is `"answer"` or `"clarify"` (`isFreeTextActionKey` at `:779-781`). All other combinations delegate to unchanged single-arg overload (`:733-749`). Sole production call site: `:305-313`. No new enum value. Evidence: `MapBudgetToClarificationLabelTest` (12 tests) + `ControlKernelEscalationReasonTest` regression lock.

**R4.a #6 (customer_context_status)**: approve — Enum derived from `formField(email)`, `formField(ad_id)`, and `deriveListingLookupState` over `lookup_listing_or_ad` tool event. Priority order first-match-wins: `missing_email` > `missing_ad_id` > `lookup_failed` > `lookup_skipped` > `loaded`. c7 verified: email present + null ad_id + lookup OK → `missing_ad_id` (not `loaded`). Zero user-message parsing in helpers. Evidence: `ContextProjectionBuilder.java:1383-1401`, `:1426-1440`; `CustomerContextStatusProjectionTest` (12 tests incl. `priority_loadedCustomerContextWithNullAdId_missingAdIdWins`, `priority_noKeywordLeakage_userMessagePhraseDoesNotShiftEnum`).

**R4.a #7 (ad_reference)**: approve — Struct `{form_ad_id, listing_lookup}` from same runtime facts. `deriveListingLookupState` at `:1364-1374` distinguishes `MISSING` (triggered+succeeded+!found) from `SKIPPED` (!triggered). `listingLookupToken` maps to `ok`/`missing`/`failed`/`skipped`. Emitted every live turn via `addAdContextPremiseProjection` before early-return (`:913-920`). Evidence: `ContextProjectionBuilder.java:1404-1449`; `AdReferenceProjectionTest` (12 tests incl. `missingIsDistinctFromSkipped`).

## §2 Nine-Question Kernel Walkthrough

**Q1 — Semantic decision hardcode?** Partial surface (enums, if-else) present, but none encode LLM-owned semantic decisions:
- R1: schema + registry iteration — no per-UC matrix in projection builder.
- R2: structural cardinality + control-plane label re-map on existing enum.
- R4: enum labels name observable runtime facts (field presence, tool outcome), not user-intent classification.

```java
// R4 — structural state only, not user-message content
if (email == null || email.isBlank()) return "missing_email";
if (adId == null || adId.isBlank()) return "missing_ad_id";
```

**Verdict for Q1:** No — changes are `prompt_projection` observability + `infra`/`skill_state` wiring per §3.2.

**Q2 — Tier-0 invariant justification?** N/A — dev correctly claims no new Tier-0 invariant; changes project existing registry/budget contracts.

**Q3 — Could soft-signal projection suffice?** Yes, and that is what shipped: `intake_fields` schema declaration, `required_intake_fields_for_active_uc` hint, `budgets.clarification`, `customer_context_status`, `ad_reference`. R2 #5 re-map reuses an existing enum value with phase/action structural guards — preferred pattern per §1.5/§1.7 over enum widening.

**Q4 — Eval/case text encoded?** No — cumulative diff touches only `ContextProjectionBuilder`, `AgentRunLoopImpl`, `ControlKernel`, five test classes, and handoff. Zero `eval_interactive/**`, `autoloop/**`, yaml, CaseSpec, or scoring edits (`git diff 084dd8b..af44903 --name-only` verified).

**Q5 — Semantic ownership moved LLM→Java?** No — projection expands observable state the LLM may use; FAQ-grounded resolve gate, UC routing, and escalation posture unchanged. R2 counter/mapping are control-plane state tracking and accurate stamping, not bot wording or UC-selection decisions.

**Q6 — Prompt if-else instead of principles?** No — zero yaml/skill/prompt artifact edits; changes are JSON projection slots and Java wiring only.

**Q7 — Tool schema / safety / grounding floors preserved?** Yes — `SkillGuardrailDispatcher` reject logic untouched (not in diff); `IntakeFieldsRegistry` field definitions unchanged (`:53-67`); `BudgetChecker` unchanged; `FormContextIngestionService` unchanged; `intake_fields` optional in schema so validator is not weakened for partial intake on non-intake UCs.

**Q8 — Generalization eval coverage?** Wiring-tier coverage adequate for §5.7 gate: 53 mocked tests span target (c1/c5/c6/c3/c9/c7), neighbor (all intake UCs G–K, DISCOVER/RESOLVE phases), negative (non-intake UC omission, tool-call/UC-commit counter negatives, with-ad_id `loaded`, keyword non-leakage), and structural shadow guards (UC-independence, phase-independence). Real-LLM re-bless remains the §6 behaviour-evidence gate — appropriately deferred to human launch.

**Q9 — Temporary hardcode / sunset plan?** N/A — durable wiring fixes with no interim semantic branch; no rollback sprint required.

**§4.1 aggregate verdict:** `approve`

## §3 Specific-Focus-Point Verdicts

**A. R1/R4 prompt_projection purity**: verified — R1 schema is universal-optional object without per-UC properties; R1 #2 iterates `IntakeFieldsRegistry` under `isIntakeUseCase` (registry classification, not replicated lists); omission for non-intake UC is a structural shape distinction (field absent vs empty array). R4 reads only `formField(session,"email"|"ad_id")`, `lastToolEvent(..., "lookup_listing_or_ad")`, and `toolResultResolvedListing` (`found` boolean) — no user-message parameters in `addAdContextPremiseProjection` / `computeCustomerContextStatus` / `deriveListingLookupState` / `listingLookupToken`. Priority order and c7 case confirmed at `:1383-1401`. `missing`≠`skipped` confirmed at `:1364-1374`.

**B. R2 infra/skill_state repair**: verified — Counter uses phase/tool-call/UC-commit/bot-reply emptiness only; `userMsg` is `action.getUserMessage()` (bot outgoing). Budget projection is DISCOVER-only cardinality. Phase-aware mapping is AND-guarded; single-arg overload unchanged; no new enum. Pre-fix audit outcome (b) correctly implemented.

**C. Phase-aware mapping guard correctness**: correct — All three conditions AND-ed at `ControlKernel.java:765-768`; `isFreeTextActionKey` limited to `"answer"`/`"clarify"`; fallback `return mapBudgetToEscalationReason(bucket)` at `:770`; grep shows sole production 3-arg call at `:311` (tests at `MapBudgetToClarificationLabelTest` only). RESOLVE+`search_knowledge` and INTAKE+`answer` stay `turn_budget_exhausted`.

**D. Anti-误杀 floor preservation**: preserved — No validator/scoring/simulator weakening; anchor/shadow cases cannot be masked by these changes (projection-only + counter wiring). Design cannot make uc_g/h/i/j or cs38s* pass without real behaviour change. Tests lock: non-intake omission, registry verbatim match, UC-independence for R4, phase guard for R2 #5, field-confusion guard for counter. Note: no dedicated `SkillGuardrailDispatcher` reject-path regression test in the 53-test suite — acceptable because dispatcher is file-fence untouched and schema keeps `intake_fields` optional (non-weakening).

**E. 4-commit split acceptability**: ACCEPTABLE-WITH-NOTE — Consolidating R1+R2#4+R4 in `a873d18` is reviewable as one projection bundle sharing `objectMapper`/`buildProjection` plumbing; runtime-behaviour commits (`840a5e2` counter, `247da11` mapping) are cleanly isolated for revert. Interaction risk is low (R4 emit is independent of R1/R2 budget blocks). Recommend per-scope-item commits in future sub-sprints when projection file churn is surgical, but consolidation is not blocking.

**F. Human re-bless gate**: PROCEED — All seven scope items pass anti-hardcode review; file fence clean; mocked wiring evidence sufficient per §5.7. Human may launch §6 re-bless to `eval_interactive/results/m-auto-6-baseline-r1r2r4-20260605` with backend rebuild and anti-误杀 abort floor (anchor uc_g/h/i/j + shadow cs38s* at 0.000 stable). R4 bot-behaviour shift is explicitly NOT a close gate (OBS-S1 deferred post M-Auto-6).

## §4 Blocking Findings

None.

## §5 Non-Blocking Observations

1. **Inherited Java failure (OQ-S41.5)** — `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` remains the sole failure; this sub-sprint's edits are additive projection slots/schema and do not touch system-prompt tiebreaker text. Accept dev's pre-existing-on-clean-main claim; no evidence this sub-sprint caused the failure.

2. **Validator reject regression test gap** — Handoff references `intakeValidatorRejectBehaviour_unchangedWhenIntakeFieldsAbsent` "or equivalent"; the shipped suite proves schema optionality and registry iteration but does not invoke `SkillGuardrailDispatcher` directly. Low risk given file-fence confirmation; consider a thin dispatcher characterization in a future infra sprint.

3. **R4 `lookup_skipped` vs `loaded` nuance** — When email+ad_id present but lookup never ran, `customer_context_status=lookup_skipped` while `ad_reference.listing_lookup=skipped`; handoff wiring table documents this. OBS-S1 yaml procedure step remains autoloop work after M-Auto-6 close.

4. **Commit hygiene note** — Future sub-sprints touching `ContextProjectionBuilder` with multiple independent projection items should prefer the dev-prompt's 6-commit split when revert granularity outweighs plumbing convenience.

5. **Re-bless observables to watch (non-blocking)** — Post-fix expect `clarification_budget_exhausted` to appear where pre-fix showed 0; `TIER2:uc-h-intake-complete-before-handover` tag count should drop. R4 projection samples (≥3 no-ad_id, ≥3 with-ad_id) are wiring observables only.
