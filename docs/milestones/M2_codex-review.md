---
title: M2 Codex milestone-shared review archive — Skill Registry Abstraction + Wholesale Retroactive Externalization
doc_tier: milestone-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: never
supersedes: []
superseded_by: null
notes: >
  Verbatim archive of `docs/codex-findings.md` as written by Codex at
  M2 milestone close per `iteration_governance.md` §4.3 second
  paragraph (milestone-shared cumulative review against the M2 commit
  range `51c327c~..8cd0a10`; 7 commits, 5 sub-sprints S37 → S41).
  Codex was dispatched from `compact/M2-review-prompt.md` (the
  deliver-agent's milestone-shared review prompt) after Sprint 41
  closed A — Clean PASS 2026-05-18 at dev commit `8cd0a10`. Verdict:
  **decision: pass / blocking_count: 0** on first pass (single round).
  M2 final classification: **A — Clean PASS at the milestone level** —
  first clean A close at the milestone level under the NEW §8
  milestone framework (M1 was
  A-with-Codex-finding-OOSR-classification-and-deliver-agent-finding-2-fix-in-close).
  Codex independently re-walked the §4.1 9-question kernel + §1.7
  boundary check + all 22 M2 §6 hard fences cumulatively + per-sub-
  sprint Codex review consistency + reproducibility checks + targeted
  validation runs (full Java baseline `1144 / 1 / 0 / 2` + Sprint 41
  17+13+9 = 39 new tests + S38/39/40 + M1 + projection preservation).
  All Tier-0 candidate verdicts confirmed: C1 REAFFIRM REJECTED as new
  candidate; C2 DEFER continued; C3 DEFER continued (LOAD-BEARING —
  Codex evidence-based verdict aligned with deliver-agent + human
  pre-decision per
  `feedback_constitution_discipline_vs_planning_anticipation.md`); C4
  + C5 REAFFIRM NOT A CANDIDATE. No new Tier-0 candidates surfaced.
  No `runtime_freeze_and_risk_policy.md` edit warranted per Codex
  explicit verdict. All 6 design-doc editorial fold-back items routed
  correctly to SEPARATE governance commit per `doc_governance.md`
  cadence. 1 NEW non-blocking M3-cleanup candidate surfaced by Codex
  in the Deferred / Non-Blocking Notes section:
  `PhaseEvaluator.substitutePlaceholders(...)` retains legacy
  UC-subset content (e.g., UC-H/J/K case-creation note); this is NOT a
  new per-UC-pair semantic branch but future M3 cleanup could
  centralize the case-creation eligibility source — registered as
  NEW R-item
  `R-substitute-placeholders-uc-case-creation-eligibility-centralization`
  in `docs/action_bank.md` §5 at M2 close.
---

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: M2 passes milestone-close cumulative review for the inclusive range `51c327c~..8cd0a10` (7 commits). The full Skill Registry abstraction is coherent across Sprints 37-41: all six Skills are externalized, `PhaseEvaluator` selects via `SkillRegistry`, Sprint 6/7/11 predicates plus S1/S2 are declarative Skill guardrails enforced by the unified dispatcher, Sprint 23/31/33 teaching moved to the Skill/shell split, and Sprint 41 state inheritance is enforced through the Skill state bus without new classifier/router scope. No blocking scope, anti-hardcode, hard-fence, Tier-0, or validation regression was found; C2 and C3 remain QUALIFIED-DEFER Tier-0 candidates rather than M2-close elevations.

## Review Evidence
- Reviewed the cumulative M2 range with `git diff 51c327c~..8cd0a10 --stat`, `git log --oneline 51c327c~..8cd0a10`, and `git log 51c327c~..8cd0a10 --name-only`; result is 7 commits, 79 files, `13162 insertions(+)`, `927 deletions(-)`.
- Loaded the constitution chain and task context: `AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`, `docs/milestone_objective.md`, all Sprint 37-41 objective/handoff/Codex archives, `docs/proposals/skill_registry_design.md`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/faq_grounding_contract.md`, `docs/action_bank.md`, and the Sprint 37-41 compact review prompts.
- Inspected cumulative source/YAML surfaces at the M2 implementation commit: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/*.java`, `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `ContextProjectionBuilder.java`, `IntakeFieldsRegistry.java`, `ResolveDispositionEvaluator.java`, `UseCaseRegistryService.java`, all six `server/src/main/resources/skills/*.yaml`, and `server/src/main/resources/prompts/system_prompt.txt`.
- Verified `HEAD` advanced after Sprint 41 to docs-only housekeeping commit `82410f4`; `git diff 8cd0a10..HEAD -- server/src` is empty. The reviewed implementation range remains exactly `51c327c~..8cd0a10`, and validation ran against an identical server tree.
- Re-ran full Java baseline and all targeted M2/M1/frozen-surface preservation test sets; all targeted sets passed and the only full-suite failure is the documented inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`.

## Blocking Findings
None.

## Anti-Hardcode Kernel (Cumulative)
- Q1 keyword / regex / if-else / enum / per-UC matrix for semantic decision: pass. `SkillRegistry.select(...)` is exact-then-wildcard structural lookup (`SkillRegistry.java:65`), `Skill.appliesTo(...)` is phase/UC-scope matching (`Skill.java:126`), dispatcher routing is by tool/outcome and guardrail type (`SkillGuardrailDispatcher.java:137`, `SkillGuardrailDispatcher.java:183`), and the state bus switches only on allowlisted state keys (`SkillStateBus.java:99`, `SkillStateBus.java:117`). No per-UC-pair branch table was found.
- Q2 Tier-0 justification: pass. `git diff 51c327c~..8cd0a10 -- docs/runtime_freeze_and_risk_policy.md` is empty; C2 and C3 are evaluated below as candidates, not silently added invariants.
- Q3 soft signal achievable: pass. M2 uses soft projection for Sprint 31 `alternate_candidate_use_cases`, Sprint 33 `discover_disambiguation_signals`, and Sprint 41 `prior_use_case_carry` (`ContextProjectionBuilder.java:459`); hard enforcement is limited to Runtime-owned floors: S1 citation presence, Sprint 7 intake completeness, Sprint 11 premature-resolve, and state-bus persistence boundaries.
- Q4 visible-eval / trace phrasing / CaseSpec id encoded: pass. Diff-addition scan over `server/src/main` and `server/src/test` found no `alice_uc`, `3772e56b`, `bad_cases`, `source_session_id`, visible-eval phrase, `composite_score`, or `cs_NNN` strings added by M2. Prose references remain in docs only.
- Q5 semantic ownership shift LLM -> Java: pass. Java enforces capability, grounding minimum, persistence, and state-bus floors; the LLM still owns user goal, UC hypothesis, drift/topic-shift read, next action, escalation posture, response strategy, and customer-facing wording per `iteration_governance.md` section 1.3.
- Q6 prompt if-else dump: pass. `system_prompt.txt` is an 80-line orchestration shell with universal envelope mechanics and the pre-approved escalation-reason decision tree; Skill procedures are LLM-soft guidance. The DISCOVER examples and `request_handover` tree are not active-UC-pair hard branches.
- Q7 tool schema / capability / PII / grounding floor: pass. No tool schema or `server/src/main/java/com/gumtree/csagent/service/tools/` files changed; `PhasePlan.allowedTools` is composed from `skill.toolsRequired()` (`PhaseEvaluator.java:454`) and still enforced by existing tool dispatch. PII/safety surfaces are untouched; grounding is narrowly extended only by S1 per M2 section 6 #4.
- Q8 generalization coverage: pass. New Java coverage is 161 tests across Skill core (51), RESOLVE/dispatcher/S1/S2 (60), teaching extraction (11), and state-bus/projection/UC-switch (39), with target/neighbor/negative cases for each deterministic layer. M2 correctly treats bad-case/smoke evidence as observation, not a gate.
- Q9 rollback / sunset: pass. The Skill Registry abstraction is permanent infrastructure. The cumulative range is revertible as a bounded architectural slice; no temporary hardcode requires a sunset plan.

## Section 1.7 Boundary Check (Cumulative)
- (a) Raw eval phrases encoded into Java or prompt: pass. Added runtime/prompt/YAML diff lines contain no bad-case IDs, source-session IDs, or visible-eval text.
- (b) UC-specific hard rules for soft semantic decisions: pass. The cumulative implementation scopes Skills by phase/use-case and uses registry-driven lookups; it does not add a per-UC-pair semantic branch table for routing, drift, escalation posture, or response strategy.
- (c) Eval spec widened to accept a bot mistake: pass. `git diff 51c327c~..8cd0a10 -- eval_interactive` is empty, including case families, shadow cases, bad cases, harness, and overrides.
- (d) Visible eval optimized at shadow/generalization cost: pass. M2 review and handoffs honor the `docs/milestone_objective.md` section 5 recalibration; smoke and Alice are observation-only, not programmatic close gates.
- (e) Prompt as if-else rule dump: pass. `system_prompt.txt` contains universal mechanics, `already_called`, and the Sprint 37-approved escalation reason decision walk; Skill YAML procedures remain principle-level and no per-UC-pair prompt table was added.

## Hard-Fence Verification (Cumulative)
- M2 section 6 #1: pass. No per-UC-pair branch exists in Skill YAML procedures, YAML guardrails, `SkillRegistry`, `Skill`, `SkillGuardrailDispatcher`, `SkillStateBus`, `ContextProjectionBuilder.buildPriorUseCaseCarryNode(...)`, or `PhaseEvaluator.maybeApplyStateBusOnSwitch(...)`. Legacy UC-subset data in `applicable_use_cases` and placeholder lookup is scoping/content preservation, not an LLM-owned semantic branch.
- M2 section 6 #2/#3: pass. `SkillGuardrailDispatcher` only enforces Runtime-owned floors, and Skill `procedure` remains soft guidance; `tools_required` is the only hard tool whitelist and flows into `PhasePlan.allowedTools`.
- M2 section 6 #4: pass. S1 `must_cite_source` is bounded to the RESOLVE-FAQ Skill declaration (`resolve_faq_grounded_answer.yaml:34`), `record_outcome` outcome class `resolve` (`SkillGuardrailDispatcher.java:340`), and `source_id` token presence only (`SkillGuardrailDispatcher.java:357`). It does not fire on `class=escalate/abandon`, does not judge citation quality, and does not fan out beyond the Skill scope.
- M2 section 6 #5: pass. No cumulative diff touches `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, or `ClassifyUseCaseTool.java`.
- M2 section 6 #6/#7: pass. `INTAKE_UCS` remains `UC-G` through `UC-K` (`PhaseEvaluator.java:34`), and `EscalationReasonResolver.java` has an empty cumulative diff; no escalation reason enum widening occurred.
- M2 section 6 #8: pass. `docs/runtime_freeze_and_risk_policy.md` is unchanged across the M2 range; no Tier-0 invariant was added by a dev commit.
- M2 section 6 #9/#10/#11/#13/#22 cascade fences: pass. No existing eval case families, shadow case families, bad cases, eval harness, or case-spec overrides were edited.
- M2 section 6 #12/#14/#15/#16/#20 governance/archive fences: pass. No `docs/current/*`, `docs/foundational/*`, `docs/milestones/*`, or Sprint 001-036 archives changed; only `docs/proposals/skill_registry_design.md` was added as the Sprint 37 freeze, and the OLD design was not deleted.
- M2 section 6 #17: pass. M2's close evidence is Java-deterministic tests plus Codex functional review; no mocked-LLM result is used as primary evidence for LLM behavior.
- M2 section 6 #18/#19: pass. Skill procedures do not prescribe exact customer-facing language or per-step argument values; they name envelope, tool whitelist, and recommended order only.
- M2 section 6 #21: pass. `IntakeFieldsRegistry.java`, `IntakeFieldExtractor.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java`, and `ControlKernel.java` are unchanged across the cumulative range, and preservation tests pass.
- Per-sub-sprint hard-fence consistency: pass. S37 docs-only, S38 schema/4-Skill scope, S39 RESOLVE/dispatcher/S1/S2 scope, S40 teaching extraction scope, and S41 state-bus/projection scope are mutually consistent with M2 section 6 and with each per-sub-sprint Codex pass verdict.

## Schema And Reproducibility Checks
- Commit shape is reproducible: `git log --oneline 51c327c~..8cd0a10` returns exactly 7 commits; `git diff 51c327c~..8cd0a10 --name-only | wc -l` returns 79 files.
- Representative line counts reproduced at HEAD server tree: `Skill.java` 135, `Guardrail.java` 56, `StateInheritance.java` 59, `SkillRegistry.java` 123, `SkillLoader.java` 313, `SkillGuardrailDispatcher.java` 416, `SkillStateBus.java` 203, `PhaseEvaluator.java` 1482, `AgentRunLoopImpl.java` 638, `ContextProjectionBuilder.java` 1341, `system_prompt.txt` 80.
- Skill YAML line counts reproduced: `confirm.yaml` 30, `discover_triage.yaml` 32, `escalate.yaml` 29, `resolve_faq_grounded_answer.yaml` 53, `resolve_intake_collect_and_handover.yaml` 38, `terminal.yaml` 26.
- Guardrail/schema allowlists are present in `SkillLoader.java`: `VALID_STATE_KEYS` (`SkillLoader.java:53`), `VALID_PROJECTION_SLOTS` (`SkillLoader.java:65`), `VALID_TOOL_NAMES` (`SkillLoader.java:95`), and `VALID_GUARDRAIL_TYPES` (`SkillLoader.java:117`). Sprint 38 fix-review's schema completeness gap remains closed.
- New test-count claims reproduced via `rg -c '@Test'`: S38 9/11/18/13, S39 14/24/11/11, S40 11, S41 17/13/9.
- Unchanged-claim spot checks pass: empty diffs for `docs/runtime_freeze_and_risk_policy.md`, the classifier/router/drift files, tool schema files, `IntakeFieldsRegistry.java`, `IntakeFieldExtractor.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java`, `ControlKernel.java`, and all `eval_interactive` fenced surfaces.
- Non-blocking reproducibility observation: the per-sub-sprint reviews already recorded prompt/handoff numstat drift in S39/S40/S41. This M2-close review derives cited counts directly from `git diff`, `git show --shortstat`, `wc -l`, or `rg -c '@Test'`.

## Validation Runs
- Full baseline: `cd server && mvn clean test -q 2>&1 | tail -25` produced `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2`. The sole failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`, matching the inherited Sprint 24-era baseline.
- Skill abstraction (S38): `mvn -pl server test -Dtest=SkillTest,SkillRegistryTest,SkillLoaderTest,PhaseEvaluatorSkillIntegrationTest -q` PASS.
- RESOLVE migration + S1/S2 + dispatcher (S39): `mvn -pl server test -Dtest=PhaseEvaluatorResolveSkillIntegrationTest,SkillGuardrailDispatcherTest,ResolveFaqGuardrailsTest,ResolveIntakeGuardrailsTest -q` PASS.
- Teaching extraction (S40): `mvn -pl server test -Dtest=SkillTeachingMigrationIntegrationTest -q` PASS.
- State-bus + slot (S41): `mvn -pl server test -Dtest=SkillStateBusTest,PriorUseCaseCarryProjectionTest,UcSwitchStateInheritanceTest -q` PASS.
- M1 functional surface preservation: `mvn -pl server test -Dtest=Sprint71PartialIntakePersistenceTest -q` PASS.
- Sprint 31/33 projection preservation: `mvn -pl server test -Dtest=Sprint7CandidateUseCasesProjectionTest,DiscoverDisambiguationSignalsProjectionTest -q` PASS.
- Sprint 11/11.1/12 frozen surface: `mvn -pl server test -Dtest=Sprint11ProgressiveResolveTest,Sprint12RuntimeAlignmentValidationTest -q` PASS.
- Inherited baseline check: `mvn -pl server test -Dtest=SystemPromptUserRequestedTiebreakerTest -q` failed as expected with 6 tests / 1 failure on `SystemPromptUserRequestedTiebreakerTest.java:53`.

## Tier-0 Candidate Independent Verification (LOAD-BEARING for M2 close)
- C1 Skill tool-whitelist enforcement: REAFFIRM REJECTED as a new candidate. The hard invariant pre-exists M2 via `PhasePlan.allowedTools` plus `ToolDispatcher.validateAgainstPlan`; M2 only changes the source of the whitelist values to `skill.toolsRequired()`.
- C2 Skill terminal predicate refusal non-overridability: verdict (a) DEFER continued. Tier-0 claim shape: Skill terminal predicate refusal is non-overridable by the LLM; the LLM cannot force-through a rejected `request_handover` or `record_outcome` and must satisfy the guardrail condition or take an allowed alternate path. Evidence is strong structurally: `SkillGuardrailDispatcher` short-circuits on first reject (`SkillGuardrailDispatcher.java:137`, `SkillGuardrailDispatcher.java:183`), `AgentRunLoopImpl` invokes it before `request_handover` and `record_outcome` effects (`AgentRunLoopImpl.java:328`, `AgentRunLoopImpl.java:362`), four guardrail types are allowlisted (`SkillLoader.java:117`), and 24 dispatcher tests plus 22 RESOLVE guardrail tests pass. I still do not elevate at M2 close because the Tier-0 freeze would be an operational governance claim about real trace behavior and future override-path prevention; deterministic Java tests are necessary and compelling but not enough to freeze the invariant before M3+/production observation. No `runtime_freeze_and_risk_policy.md` edit is warranted in this review.
- C3 Skill state-bus boundary enforcement: verdict (a) DEFER continued. Tier-0 claim shape: Skill-declared `state_inheritance` is enforced at the session-state-bus boundary; the LLM has no production path to override what the new Skill declares as inherited/reset. Evidence is strong structurally: `SkillStateBus.applyOnSkillSwitch(...)` applies new-Skill declarations (`SkillStateBus.java:85`), `PhaseEvaluator.maybeApplyStateBusOnSwitch(...)` invokes it at the single detected Skill-switch boundary (`PhaseEvaluator.java:407`, `PhaseEvaluator.java:425`), all six Skill YAMLs declare `state_inheritance`, and 17 unit + 9 integration state tests pass. I still do not elevate at M2 close because this is the first evidence surface and the Tier-0 claim is operationally about real traces having no bypass path; continue observing through M3+ before freezing.
- C4 S1 `must_cite_source` semantics: REAFFIRM NOT A CANDIDATE. The predicate is the narrow M2 section 6 #4 bounded inversion, not a generic Tier-0 citation gate; the generic `D-hard-citation-gate` deferral remains valid.
- C5 S2 `intake_complete_required` semantics: REAFFIRM NOT A CANDIDATE. The Sprint 7 intake-completeness predicate was relocated into Skill guardrails; no new invariant semantics were introduced.
- New Tier-0 candidates: none surfaced. C2 and C3 R-items should remain open for M3+ revisit.

## OQ Disposition Rollup
- Sprint 37 OQs: pass. OQ-7.2 and OQ-7.3 remain C2/C3 DEFER and are re-evaluated above; OQ-7.4 section-12-only enumeration was sufficient; OQ-7.10 supersession remained deliver-agent-owned; OQ-7.11 remains correctly classified as situation-to-enum guidance rather than a per-UC prompt branch.
- Sprint 38 and Sprint 38-fix OQs: pass. OQ-S38.1 template-vs-legacy DISCOVER wording is correctly routed to fold-back; OQ-S38.2/3/4 are informational; OQ-S38.5 correctly continued C2/C3; OQ-FIX1.1 opened the canonical tool-name source centralization R-item; OQ-FIX1.2/3/4 remain informational.
- Sprint 39 OQs: pass. OQ-S39.1-4 align with implementation; OQ-S39.5 C2 elevation remains DEFER; OQ-S39.6 `downgrade_reason` remains deferred; OQ-S39.7 stale Sprint 38 test name is housekeeping/fold-back, not a blocker.
- Sprint 40 OQs: pass. OQ-S40.1 fifth-file golden update, OQ-S40.2 combined envelope paragraph, OQ-S40.3 inherited test status quo, OQ-S40.4 no Sprint 23 rewording, and OQ-S40.5 design-doc typo fold-back remain correctly classified.
- Sprint 41 OQs: pass. OQ-S41.1 remains disagree-in-part but non-blocking and routes to M2-close housekeeping; OQ-S41.2 C3 remains DEFER; OQ-S41.3 ctor-null style is acceptable; OQ-S41.4/6 are design-doc editorial divergence only; OQ-S41.5 inherited test remains status quo.
- Cumulative evidence does not change any sub-sprint OQ from non-blocking/informational to blocking. No cumulative-evidence-changed-since-close finding was found.

## Design-Doc Editorial Fold-Back Queue
- Item 1: confirm routed correctly. Sprint 37 design doc section 7.8 Sprint 31/33 line-number swap typo should be fixed in a separate governance fold-back commit.
- Item 2: confirm routed correctly. Sprint 38 OQ-S38.1 DISCOVER template-vs-legacy wording divergence is code-ahead-of-docs/editorial and belongs in separate fold-back.
- Item 3: confirm routed correctly. Sprint 39 RESOLVE template-vs-legacy wording divergence for design doc sections 6.2.5/6.2.6 is editorial and belongs in separate fold-back.
- Item 4: confirm routed correctly. Stale test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` is housekeeping and not an M2 runtime blocker.
- Item 5: confirm routed correctly. A one-line `system_prompt.txt` pointer for `prior_use_case_carry` is reasonable M2-close housekeeping because the current phase-plan projection does not expose `state_inheritance.soft_signal_via_projection`; this is not a Sprint 41 fix requirement.
- Item 6: confirm routed correctly. The `accumulated_tool_results` inherit-all implementation vs design doc section 10.5 resolve-side wording is editorial because runtime action is no-op for that dimension.
- New fold-back items: none found by this cumulative review beyond the six already queued.

## Deferred / Non-Blocking Notes
- The M2 acceptance recalibration is honored: Alice bad-case behavior and interactive eval smoke/composite metrics are observations, not M2 close gates.
- `PhaseEvaluator.substitutePlaceholders(...)` retains legacy UC-subset content such as the UC-H/J/K case-creation note; this is not a new per-UC-pair semantic branch, but future M3 cleanup could centralize that case-creation eligibility source if desired.
- C2 and C3 should remain visible in `docs/action_bank.md` with the Sprint 39 and Sprint 41 evidence annotations; elevation, if ever chosen, must be a separate deliver-agent + human governance commit.
- The inherited `SystemPromptUserRequestedTiebreakerTest` failure remains the only Java-suite red item and is outside M2 attribution.
