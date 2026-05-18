---
title: Sprint 39 Codex per-sub-sprint review archive — RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 + unified Skill terminal-predicate dispatcher (NEW M2 sub-sprint 3)
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 39 fired per-sub-sprint Codex review per `iteration_governance.md` §4.3 trigger #3 (Runtime grounding-floor surface — S1 `must_cite_source` NEW Runtime-floor enforcement bounded per M2 §6 #4 verbatim authorization; Runtime capability floor — unified `SkillGuardrailDispatcher` NEW architectural surface; multi-fence convergence at one sub-sprint). The review returned `decision: pass / blocking_count: 0` on `2f412b6` in a single round. Sprint 39 closes **A — Clean PASS** (no fix iteration; cleanest M2 close outcome to date — Sprint 37 PASS A → Sprint 38 B-fix-iterated → Sprint 39 PASS A). Codex independent verdict on OQ-S39.5 (C2 Tier-0 elevation timing) aligned with the deliver-agent + human pre-decision: **DEFER** — deterministic Java tests are necessary but not sufficient production evidence; no `runtime_freeze_and_risk_policy.md` edit warranted. C2 + C3 R-items in `docs/action_bank.md` §5.2 stay open for M3+ revisit. Archive copies the live `docs/codex-findings.md` content verbatim at Sprint 39 close per `feedback_packaging_codex_findings_supersession.md` delete-and-add supersession pattern.
---

# Sprint 39 Codex per-sub-sprint review archive

Sprint 39 closed **A — Clean PASS** 2026-05-18. The Codex per-sub-sprint review fired in a single round (`2f412b6` initial review → `decision: pass / blocking_count: 0`). Full verdict preserved verbatim below.

## Round 1 — initial review of `2f412b6` (Sprint 39 dev commit)

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 39 passes Codex anti-hardcode + sprint-close review. The dev commit `2f412b6` stays within the Sprint 39 scope, externalizes both RESOLVE Skills, migrates the Sprint 6/7/11 predicates into a bounded four-type `SkillGuardrailDispatcher`, implements S1 only within the M2 §6 #4 authorization, preserves Sprint 11/11.1/12 and M1 functional surfaces, and preserves the Java baseline at `1094 / 1 inherited / 0 / 2`. Codex independently recommends continuing to DEFER C2 Tier-0 elevation pending production trace evidence; no blocking findings were found.

## Review Evidence
- Reviewed Sprint 39 commit range `5787806..2f412b6` at HEAD `2f412b6`; `git diff --stat` reports 21 files changed, 2621 insertions, 536 deletions.
- Loaded the governance chain (`AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`) plus `docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/sprints/sprint-039-handoff.md`, `docs/proposals/skill_registry_design.md`, Sprint 37/38 archives, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/faq_grounding_contract.md`, and the Sprint 39 compact prompts/handoff.
- Verified scope discipline with `git diff 5787806..2f412b6 --name-only` and `git show --stat 2f412b6`; touched files are only the 2 RESOLVE Skill YAMLs, 3 new Skill-dispatch Java files, `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, 4 new test files, 8 edited test/helper files, and `docs/sprints/sprint-039-handoff.md`.
- Verified forbidden-surface diffs are empty for `ResolveDispositionEvaluator.java`, `IntakeFieldsRegistry.java`, `IntakeFieldExtractor.java`, `UseCaseRegistryService.java`, `ContextProjectionBuilder.java`, `system_prompt.txt`, `SkillLoader.java`, Skill model/registry classes, tool specs, `RecordOutcomeTool.java`, governance docs, proposal docs, `docs/foundational/`, `docs/milestones/`, and `eval_interactive/`; no `SkillStateBus.java` exists.
- Spot-checked code surfaces: `PhaseEvaluator.java:389-465`, `AgentRunLoopImpl.java:106-138`, `AgentRunLoopImpl.java:319-400`, `SkillGuardrailDispatcher.java:137-203`, `SkillGuardrailDispatcher.java:217-384`, `DispatchContext.java:32-44`, `RejectVerdict.java:18-25`, `SkillLoader.java:65-122`, and both RESOLVE YAML guardrail blocks.
- Re-ran Java validation: full `mvn test -q` preserved the expected inherited baseline (`Tests run: 1094, Failures: 1, Errors: 0, Skipped: 2`; only `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`), 60 new Sprint 39 tests passed, and targeted regression spot-checks passed.
- Independently verified Sprint 37 freeze fidelity for decisions (e §6.2.5/§6.2.6), (g §8.2.1-§8.2.5), and (h §9.1-§9.4); behavioural-equivalence golden tests cover RESOLVE-FAQ × 7 UCs, RESOLVE-INTAKE × 5 UCs, and 2 fallback paths.

## Blocking Findings (if any)
None.

## Anti-Hardcode Kernel (§3)
1. **Q1 keyword / regex / if-else / enum / per-UC matrix:** Pass. `SkillRegistry.select(...)` remains exact-match-then-wildcard lookup (`SkillRegistry.java:65-84`), RESOLVE `applicable_use_cases` lists are scope declarations, `PhaseEvaluator.substitutePlaceholders(...)` uses registry/map/set lookups keyed by active UC (`PhaseEvaluator.java:436-464`), and dispatcher handlers are bounded predicate checks rather than LLM-owned semantic branches.
2. **Q2 Tier-0 justification:** Pass. Sprint 39 adds no Tier-0 invariant; `docs/runtime_freeze_and_risk_policy.md` is unchanged, and `ResolveDispositionEvaluator.java` is unchanged. C2/C3 remain candidate R-items, not dev-commit edits.
3. **Q3 soft signal achievable:** N/A / pass. The three migrated predicates were already runtime guards; S1 is the explicitly authorized runtime grounding-floor minimum-citation-presence guard; S2 is Sprint 7 §I2 moved into declarative form.
4. **Q4 eval text / CaseSpec id leakage:** Pass. No `alice_*`, `cs_*`, bad-case-suite text, source session id, or visible-eval phrase is present in the new YAMLs, dispatcher, `PhaseEvaluator`, or `AgentRunLoopImpl` surfaces checked.
5. **Q5 semantic ownership shift:** Pass. Skill procedure/grounding/escalation text is LLM-soft teaching; Java only enforces runtime-owned floors. The new S1 guard is bounded to M2 §6 #4 and does not take over response strategy or customer-facing wording.
6. **Q6 if-else prompt dump:** Pass. The two RESOLVE Skill YAML text bodies are legacy-verbatim principle-level instruction plus template placeholders, not per-UC if/else rule dumps. `system_prompt.txt` is untouched.
7. **Q7 tool schema / capability / PII / grounding floor:** Pass. Tool schemas and `RecordOutcomeTool.normalizeOutcomeClass(...)` are unchanged; `PhasePlan.allowedTools` is still composed from `skill.toolsRequired()` (`PhaseEvaluator.java:395-407`) and enforced by `ToolDispatcher.validateAgainstPlan(...)`; S1 is a bounded grounding-floor extension only.
8. **Q8 generalization coverage:** Pass. Target coverage is 14 RESOLVE behavioural-equivalence tests; dispatcher/guardrail coverage is 24 + 11 + 11 tests; neighbor regression suites for Sprint 38 Skills and M1/Sprint 7/11/12/34/71 surfaces passed. Shadow / interactive eval remain observation-only for M2 per milestone §5.
9. **Q9 rollback / sunset:** Pass / N/A. The SkillRegistry abstraction is permanent; rollback for Sprint 39 behaviour is bounded to reverting `2f412b6`, which restores the legacy RESOLVE branches and scattered predicate methods.

Codex agrees with the dev handoff §6 self-walk verdict: `approve`.

## §1.7 Boundary Check (§4)
- **RESOLVE Skill bodies:** Pass. `resolve_faq_grounded_answer.yaml:30-33` and `resolve_intake_collect_and_handover.yaml:21-24` carry legacy RESOLVE branch text with placeholders; no eval phrase encoding, no CaseSpec id, and no per-UC-pair branch table.
- **Applicable UC scope:** Pass. FAQ scope is `[UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]` (`resolve_faq_grounded_answer.yaml:5-12`); INTAKE scope is `[UC-G, UC-H, UC-I, UC-J, UC-K]` (`resolve_intake_collect_and_handover.yaml:5-10`). This matches the pre-Sprint-39 FAQ/INTAKE partition and is declarative Skill selection scope, not a new semantic branching matrix.
- **State inheritance:** Pass. FAQ inherits `[customer_context, accumulated_tool_results]` and INTAKE inherits `[customer_context, intake_fields_partial]`; both reset `[]` and soft-signal via `[prior_use_case_carry]` (`resolve_faq_grounded_answer.yaml:46-52`, `resolve_intake_collect_and_handover.yaml:31-37`). Slots are state-dimension-keyed and loader-allowlisted.
- **Guardrail declarations:** Pass. FAQ guardrails appear in declaration order `faq_miss_handover_requires_resolve_attempt`, `premature_resolve_outcome_guard`, `must_cite_source` with `on_fail: reject_with_hint` and S1 parameters `outcome_class: resolve`, `cite_token_field: source_id` (`resolve_faq_grounded_answer.yaml:34-45`). INTAKE declares only `intake_complete_required` with expected parameters (`resolve_intake_collect_and_handover.yaml:25-30`).
- **Dispatcher boundedness:** Pass. `SkillGuardrailDispatcher` exposes two primary `Skill`-form methods plus two `PhasePlan` convenience overloads and switches only over the four allowlisted guardrail types (`SkillGuardrailDispatcher.java:137-203`). There is no runtime-injected `Map<String, Function<...>>` or generic external predicate engine.
- **S1 `must_cite_source`:** Pass. `handleMustCiteSource(...)` normalizes only `resolve` / legacy `resolved`, bows out on configured `outcome_class` mismatch, uses `cite_token_field` default `source_id`, checks user-facing text presence only, and does not judge citation correctness/relevance/content quality (`SkillGuardrailDispatcher.java:340-384`). Tool boundary is the `record_outcome` dispatch site (`AgentRunLoopImpl.java:362-400`); Skill scope is the RESOLVE-FAQ YAML.
- **S2 `intake_complete_required`:** Pass. `handleIntakeCompleteRequired(...)` only evaluates `request_handover`, requires `escalation_reason` prefix `intake_complete_for_uc_`, verifies active UC through `IntakeFieldsRegistry.isIntakeUseCase(...)`, delegates completeness to `IntakeFieldsRegistry.intakeComplete(...)`, and preserves the reject reason `intake_required_fields_missing_for_intake_complete` (`SkillGuardrailDispatcher.java:265-298`).
- **Sprint 11 delegation:** Pass. `handlePrematureResolveOutcomeGuard(...)` delegates directly to unchanged `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)` with no predicate reimplementation (`SkillGuardrailDispatcher.java:307-327`).
- **SkillLoader allowlists:** Pass. `VALID_PROJECTION_SLOTS` includes `prior_use_case_carry`, `VALID_ON_FAIL_MODES` remains `reject_with_hint` / `downgrade_reason`, `VALID_TOOL_NAMES` remains 11 canonical tools, and `VALID_GUARDRAIL_TYPES` remains exactly the four Sprint 39 types (`SkillLoader.java:65-122`); `git diff` confirms `SkillLoader.java` is unchanged in Sprint 39.
- **Ownership boundary:** Pass. No Skill prescribes exact customer-facing language or arbitrary per-step argument values beyond declared runtime guardrail scopes; LLM-owned §1.3 surfaces remain LLM-owned.

## Hard-Fence Verification (§5)
- **Scope discipline:** Pass. Only Sprint 39 §5 dev-authored files appear in `git diff 5787806..2f412b6 --name-only`; no deliver-agent-owned docs except the dev handoff are in the dev commit range.
- **Sprint 39 contract §6 fences:** Pass. No edits to `system_prompt.txt`, `ContextProjectionBuilder.java`, `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`, `ResolveDispositionEvaluator.java`, `IntakeFieldsRegistry.java`, `IntakeFieldExtractor.java`, `RecordOutcomeTool.java`, tool specs, governance docs, proposal docs, eval harness/cases, milestone archives, or Sprint 1-38 archives. No `SkillStateBus.java`; no new outcome class; no `observe_only` or `downgrade_reason` in Sprint 39 guardrails.
- **M2 §6 fences:** Pass. No per-UC-branch if/else in Skill bodies or dispatcher; Skill terminal predicates are runtime-owned floor enforcement only; Skill procedures remain soft guidance; S1 matches the bounded inversion of `D-hard-citation-gate`; no Tier-0 invariant was added; M1 functional surfaces are preserved.
- **Behavioural equivalence:** Pass. `PhaseEvaluatorResolveSkillIntegrationTest.java` has 14 tests; FAQ golden strings at `PhaseEvaluatorResolveSkillIntegrationTest.java:105-160` match parent `5787806` RESOLVE-FAQ branch lines 498-529, and INTAKE golden construction at `PhaseEvaluatorResolveSkillIntegrationTest.java:200-257` matches parent `buildIntakeSystemInstruction(...)` plus legacy grounding/escalation text. Targeted test command passed.
- **Sprint 37 freeze fidelity:** Pass. RESOLVE-FAQ and RESOLVE-INTAKE YAMLs implement design doc §6.2.6/§6.2.5; predicate migrations implement §8.2.1-§8.2.5; dispatcher implements §9.1-§9.4 public surface, declaration-order short-circuit, dispatch-site routing, and trace/hint shape with `skill_name`.
- **Material finding preservation:** Pass. Sprint 6 §G2 FAQ-miss semantics are preserved in `handleFaqMissHandoverRequiresResolveAttempt(...)` (`SkillGuardrailDispatcher.java:217-257`); Sprint 7 §I2 intake completeness delegates to unchanged `IntakeFieldsRegistry`; Sprint 11 §M1 delegates to unchanged `ResolveDispositionEvaluator`.
- **Sprint 33 cue/slot split:** Pass. Sprint 38 `discover_triage.yaml` was not touched; `system_prompt.txt:36-43` slot teaching is unchanged for Sprint 40.
- **Premise re-verification:** Pass. `wc -l` confirms `system_prompt.txt` 101, `IntakeFieldsRegistry.java` 222, `UseCaseRegistryService.java` 174, `ContextProjectionBuilder.java` 1161, `ResolveDispositionEvaluator.java` 205, `PhaseEvaluator.java` 1427, and `AgentRunLoopImpl.java` 638.

## Schema And Reproducibility Checks (§6)
- **RESOLVE-FAQ schema:** Pass. `resolve_faq_grounded_answer.yaml` includes required name/description/phase/use-case/tool/context/max-step/interim/terminal/text/guardrail/state fields; tools are the five canonical FAQ-path tools; terminal outcomes are valid; guardrails are in declaration order with expected S1 parameters.
- **RESOLVE-INTAKE schema:** Pass. `resolve_intake_collect_and_handover.yaml` includes required fields; `tools_required` is only `[request_handover]`; terminal outcomes are valid; the six placeholders are resolved by `PhaseEvaluator.substitutePlaceholders(...)`; guardrail parameters match the Sprint 39 contract.
- **Dispatcher data shape:** Pass. `SkillGuardrailDispatcher` is a Spring `@Component` with constructor injection for `SkillRegistry` + `ObjectMapper`, four type constants, four reject-reason constants, four public entry points, and four private handlers.
- **Records:** Pass. `RejectVerdict` has `predicateName`, `hint`, immutable-copy `trace` (`RejectVerdict.java:18-25`); `DispatchContext` has `plan`, `session`, immutable-copy `accumulatedToolResults`, `lastLlmRawResponse`, and null-safe `parsedUserMessage` (`DispatchContext.java:32-44`).
- **PhaseEvaluator integration:** Pass. Constructor shape is unchanged; `composeSkillPhasePlan(...)` now substitutes placeholders; legacy RESOLVE branches and `buildIntakeSystemInstruction(...)` are deleted; `INTAKE_UCS`, `UC_TEAM_NAME`, and `intakeCompleteTrigger(...)` are preserved.
- **AgentRunLoop integration:** Pass. Six-arg Spring constructor with dispatcher is present (`AgentRunLoopImpl.java:106-119`); legacy five-arg constructor delegates with null dispatcher for non-predicate tests (`AgentRunLoopImpl.java:131-138`); request_handover and record_outcome dispatch sites route through the dispatcher (`AgentRunLoopImpl.java:328-400`).
- **Test structure:** Pass. New tests count 14 + 24 + 11 + 11 = 60 via `rg -c '@Test'`; edited test files are within the constructor/dispatcher-helper sweep described by the handoff.
- **Line counts:** Pass for load-bearing cited counts: `PhaseEvaluator.java` 1427, `AgentRunLoopImpl.java` 638, `SkillGuardrailDispatcher.java` 416, `RejectVerdict.java` 26, `DispatchContext.java` 45, FAQ YAML 52, INTAKE YAML 37, new test files 321/445/201/182.
- **Cited ranges reproduced:** Pass. Handler ranges `SkillGuardrailDispatcher.java:217-257`, `:265-298`, `:307-327`, and `:340-384` exist and match the handoff claims; YAML guardrail blocks are at `resolve_faq_grounded_answer.yaml:34-45` and `resolve_intake_collect_and_handover.yaml:25-30`.
- **Handoff structure:** Pass. `docs/sprints/sprint-039-handoff.md` has the required 12-section shape and leaves §12 as a closure-verdict placeholder.

## Validation Runs (§7)
- `cd server && mvn test -q` completed with the expected inherited failure: `Tests run: 1094, Failures: 1, Errors: 0, Skipped: 2`; the single failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`, matching the documented unchanged baseline.
- `mvn test -Dtest=PhaseEvaluatorResolveSkillIntegrationTest,SkillGuardrailDispatcherTest,ResolveFaqGuardrailsTest,ResolveIntakeGuardrailsTest -q` passed; this covers all 60 new Sprint 39 tests.
- Targeted regression command passed for `Sprint71PartialIntakePersistenceTest`, `Sprint11ProgressiveResolveTest`, `Sprint12RuntimeAlignmentValidationTest`, `Sprint7IntakeStateTest`, `Sprint34IntakePrefillProjectionAndGuardTest`, `AgentRunLoopS1FaqGroundedResolveGuardTest`, `PhaseEvaluatorSkillIntegrationTest`, `SkillTest`, `SkillRegistryTest`, `SkillLoaderTest`, `PhaseEvaluatorPlanTest`, `PhaseEvaluatorFaqMissFallbackTest`, `PhaseEvaluatorMaxStepsResolverTest`, and `PhaseEvaluatorQueryEnrichmentTest`.
- `mvn test -Dtest=Sprint34IntakePrefillExtractorTest -q` passed as the M1 intake-extractor spot-check.
- No interactive eval or real-LLM rerun was performed; this matches M2 §5 recalibration and Sprint 39's Java-deterministic evidence contract.

## Tier-0 Candidate Continuation (§8)
- **C1 Skill tool-whitelist enforcement:** Confirm REJECTED preserved by construction. Sprint 39 composes `skill.toolsRequired()` into `PhasePlan.allowedTools`; existing `ToolDispatcher.validateAgainstPlan(...)` remains the hard capability boundary.
- **C2 Skill terminal predicate refusal non-overridability:** Codex independent verdict = **DEFER**. Sprint 39 provides the first structural evidence surface (`SkillGuardrailDispatcher` short-circuit-on-first-reject plus tests), but the Tier-0 claim is runtime non-overridability under real traces. Deterministic Java tests are necessary but not sufficient production evidence; no `runtime_freeze_and_risk_policy.md` edit is warranted now.
- **C3 Skill `state_inheritance` session-state-bus enforcement:** Confirm QUALIFIED-DEFER. Sprint 39 declares `prior_use_case_carry` in both RESOLVE Skills but does not implement `SkillStateBus.java` or modify `ContextProjectionBuilder.java`; Sprint 41 remains the enforcement surface.
- **C4 S1 `must_cite_source` semantics:** Confirm not a separate Tier-0 candidate for Sprint 39 close. The implementation is the freeze-decision-bounded runtime-floor predicate; the elevation question, if any, is covered by C2 non-overridability.
- **C5 S2 `intake_complete_required` semantics:** Confirm not a candidate. S2 is the existing Sprint 7 §I2 predicate moved into declarative Skill guardrails.
- **New candidates:** None surfaced by Codex. The dispatcher short-circuit semantic remains an informational evidence surface for C2, not a Sprint 39 blocker.

## OQ Independent Verification (§9)
- **OQ-S39.1 `{intake_required_fields}` placeholder:** Agree with dev / human pre-decision. The sixth placeholder preserves legacy-verbatim INTAKE system-instruction output and is resolved via `IntakeFieldsRegistry.requiredFieldsFor(activeUc).toString()`; no per-UC-pair branch is introduced.
- **OQ-S39.2 dispatcher overloads:** Agree. Both primary `Skill`-form and convenience `PhasePlan`-form walk the same declaration-ordered guardrail list and preserve short-circuit semantics.
- **OQ-S39.3 separate record files:** Agree. `RejectVerdict.java` and `DispatchContext.java` are small, test-friendly, and immutable/null-safe where required.
- **OQ-S39.4 constant relocation:** Agree. Reject-reason values are preserved byte-for-byte: `s1_resolve_required_before_faq_miss_handover`, `intake_required_fields_missing_for_intake_complete`, and `progressive_resolve_record_outcome_premature`; S1 adds `s1_citation_presence_required`.
- **OQ-S39.5 C2 Tier-0 elevation timing:** Codex independent verdict = **DEFER**. Production trace evidence is still pending; do not route to `out_of_scope_review` and do not block Sprint 39 close.
- **OQ-S39.6 `downgrade_reason`:** Agree with deferral. All four Sprint 39 guardrails use `reject_with_hint`; no behavioural tension was found that requires `downgrade_reason` in this sub-sprint.
- **OQ-S39.7 stale Sprint 38 test name:** Confirm informational only. `PhaseEvaluatorSkillIntegrationTest.resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` remains stale but passing; rename can wait for M2 close housekeeping.

## Deferred / Non-Blocking Notes (§10)
- Handoff-internal reproducibility drift is informational: `docs/sprints/sprint-039-handoff.md` §9 table says the RESOLVE YAMLs are 50/32 lines and later says "Total: 14 files changed/created"; `wc -l` and `git diff --stat` show 52/37 YAML lines and 21 files changed. The user-provided review prompt and actual commit scope are correct; no code fix is required.
- `git diff --stat` reports 2621 insertions / 536 deletions, not the approximate insertion/deletion counts in the review prompt placeholder. This is an evidence-count correction only, not a scope violation.
- Behavioural-equivalence scope is acceptable for Sprint 39: all 12 RESOLVE UCs plus 2 fallback paths are covered; further edge-case expansion can be considered at M2 close without blocking this sub-sprint.
- `SkillTestFixtures.productionDispatcher()` couples tests to production YAMLs in the same acceptable pattern as Sprint 38 `productionRegistry()`; no action.
- C2 and C3 R-items should remain open in `docs/action_bank.md` for Sprint 39 close / M2 close human review; no Tier-0 policy file edit should be made by the Sprint 39 dev commit.
- Sprint 39 acceptance rows for wrong-containment, over-escalation, shadow, Alice, and interactive eval are correctly N/A or observation-only under M2 §5 recalibration.
- Architecture-health direction is positive: `new_semantic_hardcode_count = 0`; Sprint 39 converts 2 RESOLVE phase Skill surfaces, 3 migrated predicates, and 1 new bounded S1 predicate into the Skill/guardrail architecture; planner ownership is not reduced.
- Any editorial divergence between Sprint 37 design templates and legacy-verbatim YAML content should be routed to M2 close fold-back (`R-skill-design-doc-template-fold-back` if not already tracked), not Sprint 39 fix iteration.
