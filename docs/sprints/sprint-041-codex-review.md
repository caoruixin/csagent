---
title: Sprint 41 Codex per-sub-sprint review archive — UC switch + state preservation across Skill boundary (NEW M2 sub-sprint 5; LAST M2 sub-sprint)
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: never
supersedes: []
superseded_by: null
notes: >
  Verbatim archive of `docs/codex-findings.md` as written by Codex per
  `iteration_governance.md` §4.3 trigger #3 (session state model touch
  across Skill boundary — load-bearing for cross-Skill state preservation;
  C3 Tier-0 candidate evidence surface) at Sprint 41 close 2026-05-18.
  Codex was dispatched from `compact/sprint-041-review-prompt.md` (the
  deliver-agent's per-sub-sprint review prompt) against the Sprint 41 dev
  commit `8cd0a10` (parent `9130abc`; branch `refactor/remove-the-shackles`).
  Verdict: **decision: pass / blocking_count: 0** on first pass (single
  round). Codex independently re-walked the §4.1 9-question kernel + §1.7
  boundary check + all 33 Sprint 41 §6 + 22 M2 §6 hard fences + reproducibility
  checks + targeted validation runs (full Java baseline `1144 / 1 / 0 / 2`
  + Sprint 41 17+13+9 = 39 new tests all PASS + M1 + Sprint 38/39/40
  surfaces preserved + Sprint 31/33 projection preservation). All 6 OQs
  resolved with Codex independent verdicts: OQ-S41.1 DISAGREE IN PART
  NON-BLOCKING (slot self-documentation rationale partly code-untrue —
  `phase_plan` projection does NOT expose `state_inheritance` to LLM;
  routes to M2 close housekeeping one-liner pointer); OQ-S41.2 DEFER
  continued for C3 Tier-0 (structural Java evidence necessary and
  compelling but operationally about production trace observation across
  M2 close → M3+ window before freezing); OQ-S41.3 ACCEPTABLE STYLE
  (Sprint 40 ctor-update precedent extended); OQ-S41.4 FLAG AS DESIGN-DOC
  EDITORIAL DIVERGENCE (`accumulated_tool_results` inherit-all vs §10.5
  row 1 "All resolve-side Skills" wording; routes to M2 close fold-back);
  OQ-S41.5 STATUS QUO (inherited test failure persists; not Sprint 41
  blocker); OQ-S41.6 MINOR (= OQ-S41.4 only; no major architectural
  divergence). 4 contract-drift items: §7-a REAFFIRM Sprint 39+40 ctor-
  update precedent extension to 33 files; §7-b pass on Java baseline 1144
  within target; §7-c CONTENT-COMPLIANT on line-count target gap (advisory);
  §7-d NON-BLOCKING NUMBERS-CITE OBSERVATION (deliver-agent prompt cited
  `ContextProjectionBuilder.java` 182/2 + `PhaseEvaluator.java` 59/3 +
  10 main-source files; actual numstat 181/1 + 57/2 + 9 main-source files;
  third+ observed instance of `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`
  pattern). Tier-0 verdicts: C1 REJECTED reaffirmed; C2 DEFER continued;
  C3 DEFER continued (Sprint 41 ships FIRST observed evidence surface but
  Codex evidence-based verdict aligns with deliver-agent + human pre-decision
  — Tier-0 elevation is operationally about LLM having no production path
  to override state inheritance across real traces); C4 + C5 NOT A CANDIDATE
  reaffirmed. The 1 inherited Java failure `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`
  persists unchanged per OQ-S41.5 STATUS QUO. Sprint 41 closes M2
  implementation track — M2 milestone close planning round is the NEXT
  major deliver-agent + human unit of work; milestone-shared cumulative
  Codex review against `51c327c..8cd0a10` per `iteration_governance.md`
  §4.3 will be dispatched at M2 close. M2 close fold-back queue at this
  archive (4 items): (1) Sprint 37 design doc §7.8 Sprint 31/33 line-number
  swap typo per OQ-S40.5; (2) Sprint 38 OQ-S38.1 template-vs-legacy editorial
  divergence on DISCOVER Skill body; (3) Sprint 39 §10 design doc §6.2.5/§6.2.6
  RESOLVE template editorial divergence; (4) Sprint 39 OQ-S39.7 stale test
  name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`; (5) NEW
  Sprint 41 OQ-S41.1 one-line shell pointer for `prior_use_case_carry`
  (code-untrue dev rationale that `phase_plan` projection exposes
  `state_inheritance` — the projection currently omits it); (6) NEW Sprint
  41 OQ-S41.4 `accumulated_tool_results` inheritance wording — resolve-side-only
  vs all-six framing in design doc §10.5 row 1.
---

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 41 passes Codex anti-hardcode + sprint-close review for commit `8cd0a10` over parent `9130abc`. The implementation stays within the authorized Skill-state/projection surfaces, ships `SkillStateBus`, the six `state_inheritance` YAML declarations, `prior_use_case_carry`, and `PhaseEvaluator` integration without per-UC-pair branching or a new classifier, preserves the frozen M1/Sprint 38/Sprint 39/Sprint 40 surfaces, and validates at `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2` with the sole failure being the documented inherited `SystemPromptUserRequestedTiebreakerTest` drift.

## Review Evidence
- Loaded governance/objective context: `AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`, `docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/sprints/sprint-041-handoff.md`, `docs/proposals/skill_registry_design.md`, `docs/runtime_freeze_and_risk_policy.md`, and `docs/current/faq_grounding_contract.md`.
- Sampled prior M2 closure records: `docs/sprints/sprint-037-codex-review.md`, `docs/sprints/sprint-038-codex-review.md`, `docs/sprints/sprint-039-codex-review.md`, and `docs/sprints/sprint-040-codex-review.md`.
- Verified commit shape with `git diff 9130abc..8cd0a10 --stat`, `git show --numstat 8cd0a10`, and `git diff 9130abc..8cd0a10 --name-only`: 46 files, 1734 insertions, 44 deletions.
- Reviewed source/YAML/test surfaces around `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java:85`, `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:351`, `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:459`, and all six `server/src/main/resources/skills/*.yaml` state declarations.
- Re-ran the full Java baseline plus targeted Sprint 41, M1, Sprint 38, Sprint 39, Sprint 40, and projection preservation test sets.

## Blocking Findings
None.

## Anti-Hardcode Kernel (per `iteration_governance.md` §4.1)
- Q1: pass. `SkillStateBus` switches only over the three schema-allowed state keys at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java:99` and `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java:117`; `applyIntakeFieldsIntersection` is registry-driven via `IntakeFieldsRegistry.requiredFieldsFor` at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java:175`.
- Q2: pass. `docs/runtime_freeze_and_risk_policy.md` is unchanged in the commit; C3 remains a candidate disposition question, not a silently added Tier-0 invariant.
- Q3: pass. `prior_use_case_carry` is a soft projection slot at `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:459`; deterministic filtering is limited to the runtime-owned `intake_fields_partial` persistence boundary.
- Q4: pass. Targeted search found no Alice bad-case session id, CaseSpec id, visible-eval phrase, or bad-case-suite text encoded in the new Java/YAML/test surfaces.
- Q5: pass. LLM ownership of UC hypothesis, drift, escalation posture, and response strategy remains intact; Java only manages persistence/reset state at the Skill boundary.
- Q6: pass. `server/src/main/resources/prompts/system_prompt.txt` is unchanged at 80 lines; no prompt if-else block was added.
- Q7: pass. Tool schema, capability/permission boundaries, PII/safety floor, and FAQ grounding floor surfaces are unchanged; `SkillGuardrailDispatcher` and RESOLVE guardrail blocks are unchanged except for `state_inheritance` edits.
- Q8: pass. New coverage is 17 `SkillStateBusTest`, 13 `PriorUseCaseCarryProjectionTest`, and 9 `UcSwitchStateInheritanceTest` tests; existing behavioral-equivalence tests were updated only for constructor signatures.
- Q9: pass. No temporary kill switch or sunset path was introduced; `git revert 8cd0a10` cleanly removes the Sprint 41 infrastructure and returns the code surfaces to Sprint 40 shape.

## §1.7 Boundary Check
- (a) pass: no raw eval phrases or source-session ids were added to Java, YAML, prompt, or new tests.
- (b) pass: no UC-pair branch table was added; switch detection is a single prior-UC/current-UC + prior-Skill/new-Skill guard at `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:407`.
- (c) pass: no `eval_interactive/` files were edited.
- (d) pass: Sprint 41 does not use smoke composite score or bad-case programmatic pass rate as gating evidence, consistent with M2 §5 recalibration.
- (e) pass: `server/src/main/resources/prompts/system_prompt.txt` is unchanged; no prompt rule dump landed.

## Hard-Fence Verification
- Scope fence: pass. Changed files are exactly the Sprint 41 main surfaces, 3 new Sprint 41 test classes, 33 existing test constructor-signature updates, and `docs/sprints/sprint-041-handoff.md`.
- No per-UC hardcode fence: pass. `SkillStateBus`, YAML declarations, `buildPriorUseCaseCarryNode`, and `maybeApplyStateBusOnSwitch` contain no per-UC-pair branch logic.
- No new classifier fence: pass. No `RuntimeIntentClassifier`, `IntentClassification`, `DriftResult`, `DriftDetector`, `UseCaseRouter`, or `ClassifyUseCaseTool` files changed.
- S1/S2/dispatcher fence: pass. `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` is unchanged; `resolve_faq_grounded_answer.yaml` guardrails are unchanged and still include the bounded `must_cite_source` predicate.
- Runtime freeze/M1 preservation fence: pass. `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java`, `AgentRunLoopImpl.java`, and `ControlKernel.java` are unchanged.
- INTAKE_UCS and escalation enum fence: pass. `INTAKE_UCS` and `CANONICAL_ESCALATION_REASONS` in `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` were not edited by the diff.
- Governance/archives fence: pass. No `docs/current/*`, `docs/foundational/*`, `docs/proposals/*`, `docs/runtime_freeze_and_risk_policy.md`, `docs/milestones/*`, or prior sprint archives were edited.
- M3 scope fences: pass. No request-handover schema/rationale/confidence fields, single-handover orchestrator, full issue ledger, or Topic<->UC binding surfaces were changed.

## Schema And Reproducibility Checks
- Line-count spot checks pass: `SkillStateBus.java` 203 lines, `ContextProjectionBuilder.java` 1341 lines, `PhaseEvaluator.java` 1482 lines, `system_prompt.txt` 80 lines, new test classes 306/279/246 lines, and `docs/sprints/sprint-041-handoff.md` 414 lines.
- Schema allowlists pass: `SkillLoader` still validates `customer_context`, `accumulated_tool_results`, `intake_fields_partial`, `alternate_candidate_use_cases`, `discover_disambiguation_signals`, and `prior_use_case_carry` at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:53` and `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:65`.
- Unchanged-claim spot checks pass: `Skill.java`, `StateInheritance.java`, `SkillLoader.java`, `SkillGuardrailDispatcher.java`, `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java`, `AgentRunLoopImpl.java`, `ControlKernel.java`, `system_prompt.txt`, `docs/runtime_freeze_and_risk_policy.md`, and `docs/proposals/skill_registry_design.md` all have empty diffs across `9130abc..8cd0a10`.
- Non-blocking reproducibility drift: the handoff/review narrative has count drift. Actual numstat is `ContextProjectionBuilder.java` 181/1 and `PhaseEvaluator.java` 57/2; actual distinct existing test files are 33; the "20 + 16 = 36" phrasing is a category-overlap count; and the prompt's "main sources (10 files)" list enumerates 9 changed main-source files.

## Validation Runs
- Full baseline: `cd server && mvn clean test -q 2>&1 | tail -25` produced `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2`; the sole failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`.
- Sprint 41 target tests: `SkillStateBusTest` 17/17 pass, `PriorUseCaseCarryProjectionTest` 13/13 pass, `UcSwitchStateInheritanceTest` 9/9 pass.
- M1 preservation: `Sprint71PartialIntakePersistenceTest` 14/14 pass.
- Sprint 40 preservation: `SkillTeachingMigrationIntegrationTest` 11/11 pass.
- Sprint 39 preservation: `PhaseEvaluatorResolveSkillIntegrationTest` 14/14, `SkillGuardrailDispatcherTest` 24/24, `ResolveFaqGuardrailsTest` 11/11, and `ResolveIntakeGuardrailsTest` 11/11 pass.
- Sprint 38 preservation: `SkillTest` 9/9, `SkillRegistryTest` 11/11, and `SkillLoaderTest` 18/18 pass.
- Sprint 31/33 projection preservation: `Sprint7CandidateUseCasesProjectionTest` 7/7 and `DiscoverDisambiguationSignalsProjectionTest` 9/9 pass.

## Tier-0 Candidate Independent Verification (LOAD-BEARING for Sprint 41 close)
- C1: reaffirm REJECTED as a new candidate. Tool whitelist enforcement is existing runtime behavior and Sprint 41 does not change it.
- C2: reaffirm QUALIFIED-DEFER continued. Sprint 41 does not touch the Sprint 39 dispatcher surface, so no new C2 evidence lands here.
- C3: DEFER continued. Sprint 41 provides strong structural evidence: bus enforcement at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java:85`, integration at `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:363`, all six YAML declarations, unit coverage at `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillStateBusTest.java:116`, and integration coverage at `server/src/test/java/com/gumtree/csagent/service/runtime/UcSwitchStateInheritanceTest.java:97`. That is necessary and compelling, but I do not elevate C3 now because the Tier-0 claim is operationally about the LLM having no production path to override state inheritance across real traces; observe through Sprint 41/M2 close/M3+ before freezing it.
- C4: reaffirm NOT A CANDIDATE. The S1 citation predicate remains the bounded M2 §6 #4 exception, not a generic Tier-0 citation invariant.
- C5: reaffirm NOT A CANDIDATE. The S2 intake-completeness predicate is pre-existing Sprint 7 behavior relocated under Skill guardrails in Sprint 39, not a new invariant.

## OQ Independent Verification
- OQ-S41.1: DISAGREE in part, non-blocking. The slot shape is self-describing, but the current `phase_plan` projection does not expose `state_inheritance.soft_signal_via_projection`, so that part of the dev rationale is not code-true. I recommend a one-line M2-close housekeeping pointer to `prior_use_case_carry`; this is not a Sprint 41 fix requirement.
- OQ-S41.2: DEFER continued for C3. See Tier-0 section; route no governance edit in Sprint 41.
- OQ-S41.3: ACCEPTABLE STYLE. Passing `null` in legacy constructor tests is consistent with the Sprint 40 precedent and safe because new surfaces have defensive null checks plus dedicated real-fixture tests.
- OQ-S41.4: FLAG AS DESIGN-DOC EDITORIAL DIVERGENCE. Implementation declares `accumulated_tool_results` inherit on all six Skills while design doc §10.5 says "All resolve-side Skills"; runtime effect is no-op, so this is an M2-close editorial fold-back, not a code blocker.
- OQ-S41.5: STATUS QUO. The inherited `SystemPromptUserRequestedTiebreakerTest` failure persists unchanged and is not a Sprint 41 close blocker under the sprint contract.
- OQ-S41.6: DIVERGENCE FOUND, MINOR. The only implementation-vs-§10 divergence surfaced is the `accumulated_tool_results` resolve-side/all-Skills wording in OQ-S41.4; no major architectural divergence surfaced.
- Drift §7-a: REAFFIRM precedent. The 33 existing-test constructor edits are behavioral-equivalence fallout from constructor signature changes and are not scope creep.
- Drift §7-b: pass. Baseline is within the target range at 1144/1/0/2.
- Drift §7-c: CONTENT-COMPLIANT. The line-count estimates were advisory; the additional helper size is justified by the required null/aging/cap/registry handling.
- Drift §7-d: NON-BLOCKING NUMBERS-CITE OBSERVATION. Record as another instance of reproducibility-count drift; substantive implementation and test updates are correct.

## Deferred / Non-Blocking Notes
- Consider M2-close housekeeping to add a concise shell pointer for `prior_use_case_carry`, since `phase_plan` currently omits `state_inheritance` and the LLM only sees the slot shape itself.
- Fold back the design-doc wording for `accumulated_tool_results` inheritance, or explicitly document that Sprint 41 intentionally treats it as universal no-op documentation.
- Continue tracking the inherited `SystemPromptUserRequestedTiebreakerTest` failure separately from Sprint 41 close.
- Tighten future handoff/review-prompt count discipline by deriving file counts and numstat values directly from `git show --numstat` before citing them.
