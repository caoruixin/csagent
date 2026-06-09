# Sprint 37 Codex review — archive

**Archived from `docs/codex-findings.md` at Sprint 37 close (2026-05-17) per `feedback_packaging_codex_findings_supersession.md` delete-and-add pattern.**

**Sprint 37 commit range reviewed:** `6d97888..51c327c` (pre-dev HEAD → Sprint 37 dev commit `51c327c` — Skill Registry + state-across-Skill design freeze; NEW M2 sub-sprint 1; supersedes OLD M2-Skill Sprint 36 freeze).

**Codex review prompt:** `compact/sprint-037-review-prompt.md` (451 lines; deliver-agent-drafted; 13 sections + Sprint-37-close pre-decisions header encoding deliver-agent + human positions on OQ-7.2 / 7.3 / 7.4 / 7.10 / 7.11).

**Sprint 37 close classification:** **A — clean PASS** per `iteration_governance.md` §4 close decision rule A. Matches Sprint 36's first-pass `pass / 0` precedent for the OLD M2-Skill design freeze (second instance of design-freeze sub-sprint clean-close pattern).

---

## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 37 passes as a docs-only design-freeze sub-sprint. The dev commit `51c327c` adds only `docs/proposals/skill_registry_design.md` and `docs/sprints/sprint-037-handoff.md`; the proposed Skill Registry design preserves the LLM-owned / Runtime-owned boundary, reproduces the S1 `must_cite_source` authorization verbatim, does not expand the predicate beyond `RESOLVE_FAQ` + `record_outcome(class=resolve)` + `source_id` presence, and does not introduce per-UC-branch if-else in Skill bodies, guardrails, dispatcher, or state-inheritance semantics. Tier-0 candidates C1-C5 are correctly classified, with C2 and C3 qualified but deferred for M3+ re-evaluation after Sprint 39 and Sprint 41 evidence exists; OQ-7.11 aligns with the dev verdict that `system_prompt.txt:54-101` is situation-to-enum teaching, not a forbidden per-UC prompt if-else dump.

## Review Evidence
- Reviewed Sprint 37 commit `51c327c` against pre-dev HEAD `6d97888`; `git diff 6d97888..51c327c --stat` shows exactly two added files and `4216` insertions: `docs/proposals/skill_registry_design.md` (`3633` lines) and `docs/sprints/sprint-037-handoff.md` (`583` lines).
- Loaded the governance chain (`AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`) plus NEW M2 `docs/milestone_objective.md`, Sprint 37 `docs/sprint_objective.md`, the Sprint 37 handoff, the design doc, OLD `skill_foundation_design.md`, Sprint 5 `skill_orchestration_candidates.md`, M1 close context, `runtime_freeze_and_risk_policy.md`, and `faq_grounding_contract.md`.
- Re-ran the Java baseline with `cd server && mvn test -q`; surefire XML totals are `Tests run: 983, Failures: 1, Errors: 0, Skipped: 2`, matching the expected inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` failure.
- Spot-checked cited file:line ranges at HEAD `51c327c`: `PhaseEvaluator.java:30`, `PhaseEvaluator.java:397-665`, `AgentRunLoopImpl.java:317`, `AgentRunLoopImpl.java:356`, `AgentRunLoopImpl.java:413`, `AgentRunLoopImpl.java:628-651`, `AgentRunLoopImpl.java:690-734`, `AgentRunLoopImpl.java:745-759`, `IntakeFieldsRegistry.java:53`, `IntakeFieldsRegistry.java:118-131`, `IntakeFieldsRegistry.java:184-193`, `UseCaseRegistryService.java:166-173`, `system_prompt.txt:23-101`, `RecordOutcomeTool.java:36-94`, `RecordOutcomeTool.java:110-121`, `EscalationReasonResolver.java:84-103`, and `ToolDispatcher.java:183-195`.
- Independently verified the MATERIAL FINDING that Sprint 6 / 7 / 11 partial predicates already ship in `AgentRunLoopImpl.java`, the premise #7 refinement that `system_prompt.txt` is 101 lines with Sprint 23/31/33 teaching plus lines 45-52 and 54-101, the premise #8 correction that `IntakeFieldsRegistry` is the required-fields source of truth, and the Sprint 33 cue/slot split between `PhaseEvaluator.java:432-448` and `system_prompt.txt:36-43`.
- Verified no eval, bad-case, shadow, prompt, server, test, governance, foundational, milestone archive, action-bank, or Tier-0 policy file changed in the Sprint 37 dev commit.

## Blocking Findings
None.

## Anti-Hardcode Kernel
- Q1 keyword / regex / if-else / enum / per-UC matrix: PASS; agree with design doc §11.1. `applicable_use_cases` is registry scope, Skill procedures are principle-level, predicates are typed guardrails, dispatcher routing is by tool/outcome, and `state_inheritance` is by state dimension rather than per-UC-pair branch.
- Q2 Tier-0 justification: N/A for Q1=NO; agree with design doc §11.2. Candidate evaluation belongs to §12 / this review's Tier-0 section.
- Q3 soft signal achievable: PASS with mixed verdict; agree with design doc §11.3. `prior_use_case_carry` is correctly modeled as soft projection, while S1/S2/Sprint 6/7/11 terminal predicates are Runtime-floor hard surfaces under §1.4.
- Q4 visible eval / trace phrasing / CaseSpec id: PASS; agree with design doc §11.4. Alice and M1 Finding 1 appear only as explanatory prose, not as runtime, prompt, judge, or Skill matching logic.
- Q5 semantic ownership shift: PASS; agree with design doc §11.5. The LLM retains user-goal, UC hypothesis, drift, next-action, escalation posture, response strategy, customer wording, and per-step argument choice; Java enforces only tool whitelist, grounding/capability floor, persistence, trace, and state boundaries.
- Q6 prompt as if-else dump: PASS; agree with design doc §11.6 and OQ-7.11. `system_prompt.txt:54-101` is a universal `escalation_reason` situation-to-enum decision walk, not active-UC-pair branching; keeping it in the shell vs moving it to a Skill does not create a §1.7 violation.
- Q7 tool schema / capability / PII / grounding floor: PASS; agree with design doc §11.7. Tool schemas and `PhasePlan.allowedTools` enforcement remain unchanged; PII/safety floors are untouched; grounding is narrowly extended only by the S1 predicate authorized in M2 §6 #4.
- Q8 generalization coverage: PASS for design-freeze shape; agree with design doc §11.8. Sprint 37 has no behavior change, and the prospective coverage for Sprints 38-41 is internally consistent: phase equivalence first, RESOLVE/predicates next, prompt teaching extraction next, state/UC-switch evidence last.
- Q9 rollback / sunset: PASS; agree with design doc §11.9. The Skill Registry abstraction is permanent; future Skill tuning belongs to M3+ if evidence shows a tuning need.

## §1.7 Boundary Check
- D1 envelope / Skill `procedure`: PASS. Skill procedures in §6 are LLM-soft, principle-level migrations of existing phase teaching; they do not encode raw eval phrases, CaseSpec ids, or active-UC if-else branches. The DISCOVER topic-to-UC examples are inherited classification hints from `PhaseEvaluator.java:418-430`, not a new hard branch keyed on active UC.
- D2 / D3 / D4 predicates: PASS. `guardrails[]` is a structured typed DSL consumed by Java; no free-form per-UC code is embedded in `procedure` text or a guardrail declaration body.
- D3 NEW S1 `must_cite_source`: PASS. Design doc §8.2.4 reproduces the M2 §6 #4 human authorization verbatim and scopes the predicate exactly to logical `RESOLVE_FAQ`, `record_outcome(class=resolve)`, and `source_id` citation presence. It explicitly does not fire on `class=escalate/abandon`, non-RESOLVE_FAQ phases, intake UCs, or citation quality/relevance judgments.
- D4 NEW S2 `intake_complete_required`: PASS. The design uses existing canonical `intake_complete_for_uc_*` / `incomplete_intake` vocabulary and does not widen the `escalation_reason` enum at `EscalationReasonResolver.java:84-103`; the Sprint 39 `on_fail` mode remains a planning OQ, not a Sprint 37 blocker.
- D-existing Sprint 6/7/11 migration: PASS. The mapping in §8.2.1-§8.2.3 is mechanical relocation: Sprint 6 FAQ-miss guard remains FAQ-path only, Sprint 7 intake completeness remains `IntakeFieldsRegistry`-driven, and Sprint 11 premature resolve still delegates to `ResolveDispositionEvaluator`.
- D5 UC-switching continuity matrix: PASS. §10.5 preserves one principle-level row per state dimension and uses `IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor(newUc)` for intersection; no per-UC-pair table is proposed.
- D-state_inheritance declarations: PASS. §10.2 allows `inherit`, `reset`, and `soft_signal_via_projection` by state/projection dimension only; examples contain no `prior_uc/new_uc` branch.
- D-dispatcher composition: PASS. §9.2-§9.3 dispatches by active Skill guardrail declarations and tool/outcome sites, not by UC-specific routing.
- D-no per-UC YAML in 6 Skill bodies: PASS. The six YAML mappings in §6.2 are phase/path scoped and use registry/template substitution for UC names, team names, required fields, and intake triggers; no Skill body contains `if UC == X then do Y` logic.

## Hard-Fence Verification
- Scope discipline: PASS. `git diff 6d97888..51c327c --stat` shows exactly two new docs, zero modified/deleted files, and no production/test/prompt/eval/governance/foundational/archive/action-bank surfaces.
- Sprint 37 §6 hard fences: PASS. Fences #1-#16 and #19 are satisfied by the two-file docs-only diff; #17 is satisfied by the §1.7 boundary review above; #18 is satisfied because downstream implementation details are frozen only at decision-contract level and are left to Sprints 38-41 planning.
- M2 §6 hard fences: PASS. No per-UC-branch Skill body; predicates are Runtime-floor only; `procedure` remains soft; S1 bounded inversion is preserved; no `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` touch; no `INTAKE_UCS` edit; no enum widening; no Tier-0 policy edit; no case-family, shadow, harness, governance, foundational, milestone archive, M1 functional-surface, or Alice bad-case edit.
- Supersession pattern: PASS. `docs/proposals/skill_foundation_design.md` frontmatter at HEAD has `status: superseded` and `superseded_by: docs/proposals/skill_registry_design.md`; `git log --diff-filter=M -- docs/proposals/skill_foundation_design.md` shows the direct registry supersession edit at deliver-agent commit `fd7396a`, not `51c327c`. `docs/proposals/skill_orchestration_candidates.md` has `status: superseded` and the chained pointer to `docs/proposals/skill_foundation_design.md`; that pointer is present at HEAD and untouched by `51c327c` (git history shows it was established before Sprint 37, at `ed71031`, and remains the correct chain through the now-superseded Sprint 36 design).
- MATERIAL FINDING: PASS. `AgentRunLoopImpl.java:690-734` implements Sprint 6 `shouldRejectFaqMissHandover` and is dispatched at `AgentRunLoopImpl.java:413`; `AgentRunLoopImpl.java:628-651` implements Sprint 7 `shouldRejectIncompleteIntakeHandover` and is dispatched at `AgentRunLoopImpl.java:317`; `AgentRunLoopImpl.java:745-759` implements Sprint 11 `shouldRejectPrematureResolveOutcome` and is dispatched at `AgentRunLoopImpl.java:356`.
- Premise #7 refinement: PASS. `system_prompt.txt` is 101 lines; Sprint 23 `already_called` is at lines 23-28, Sprint 31 `alternate_candidate_use_cases` at lines 30-34, Sprint 33 `discover_disambiguation_signals` at lines 36-43, DISCOVER guidance at lines 45-52, and `request_handover` decision tree at lines 54-101.
- Premise #8 / IntakeFieldsRegistry: PASS. `UseCaseRegistryService.UseCaseDefinition` at lines 166-173 has only `ucId`, `name`, `topicSubjects`, `riskLevel`, `allowBotResolution`, and `path`; required intake fields live in `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`, with `requiredFieldsFor`, `canonicalFieldName`, `fieldsRemaining`, and `intakeComplete` verified at the cited lines.
- Sprint 33 cue/slot split: PASS. The ad-status disambiguation cue is in `PhaseEvaluator.java:432-448`; the projection-slot teaching is in `system_prompt.txt:36-43`; the design correctly routes the cue via Sprint 38 and the slot teaching via Sprint 40 into `discover_triage.yaml`.

## Schema And Reproducibility Checks
- Design doc structure: PASS. `docs/proposals/skill_registry_design.md` has 13 top-level sections and covers decisions (a)-(j), §11 Q1-Q9 plus §11.10 verdict, §12 C1-C5 plus §12.6 aggregate, and §13 downstream index / refinements / OQs / self-walk.
- Decision-section structure: PASS. Each decision section (§2-§10) includes a decision statement, rationale, alternatives considered, §1.7 boundary check, §1.3/§1.4 boundary check, and downstream sub-sprint reference; §2 additionally carries schema validation, §5 guardrail schema, §9 and §10 Tier-0 candidate questions.
- Design doc frontmatter: PASS. It has `doc_tier: proposal`, `status: proposal`, `implementation_status: not_started`, `source_of_truth: this file`, `last_reviewed: 2026-05-17`, `supersedes` with both predecessor docs, and `superseded_by: null`.
- Predecessor frontmatter: PASS. `skill_foundation_design.md` is directly superseded by `skill_registry_design.md`; `skill_orchestration_candidates.md` remains the chained predecessor through `skill_foundation_design.md`; neither file is in the Sprint 37 dev diff.
- Handoff structure: PASS. `docs/sprints/sprint-037-handoff.md` has the contracted 12 sections and leaves §12 as a closure-verdict placeholder.
- Cited line reproduction: PASS. Reproduced representative citations for `PhaseEvaluator.java:30`, `PhaseEvaluator.java:598-665`, `AgentRunLoopImpl.java:690-734`, `AgentRunLoopImpl.java:745-759`, `IntakeFieldsRegistry.java:184-193`, `system_prompt.txt:54-101`, `EscalationReasonResolver.java:84-103`, and `ToolDispatcher.java:183-195`.
- S1 authorization reproduction: PASS. Normalizing whitespace, the exact quote from `docs/milestone_objective.md` §6 #4 appears in design doc §8.2.4.

## Validation Runs
- `cd server && mvn test -q` exited with the documented inherited failure only.
- Surefire aggregate from `server/target/surefire-reports/TEST-*.xml`: `983` tests, `1` failure, `0` errors, `2` skipped.
- The failing test is `com.gumtree.csagent.service.runtime.SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`, matching the expected baseline named in the Sprint 37 review prompt.
- No schema validation, eval, smoke, or bad-case suite run was required for Sprint 37 because the sprint is docs-only and M2 §5 marks Alice/eval as observation-only for this milestone.

## Tier-0 Candidate Independent Verification
- C1 Skill tool-whitelist enforcement unconditional: CONFIRM `REJECTED as new candidate`. `PhasePlan.allowedTools` is already a hard whitelist (`PhasePlan.java:15-16`) enforced before every tool dispatch by `AgentRunLoopImpl.java:293-304` calling `ToolDispatcher.validateAgainstPlan` (`ToolDispatcher.java:183-195`). Skill changes only the source of the list, not the enforcement invariant.
- C2 Skill terminal predicate refusal non-overridable by LLM: CONFIRM `QUALIFIED -> DEFER`. The non-overridability claim is Tier-0-shaped, but the dispatcher is Sprint 39 future code; deferring avoids premature governance freeze before observed implementation evidence. Carry `R-skill-guardrail-non-overridability-tier-0` for M3+ revisit after Sprint 39 evidence.
- C3 Skill `state_inheritance` enforced at session-state-bus boundary: CONFIRM `QUALIFIED -> DEFER`. The state bus is Sprint 41 future code and the registry-intersection behavior has not been observed; deferral is sound. Carry `R-skill-state-bus-boundary-enforcement-tier-0` for M3+ revisit after Sprint 41 evidence.
- C4 S1 `must_cite_source` predicate semantics: CONFIRM `NOT A CANDIDATE`. The predicate is a narrow M2 §6 #4 bounded inversion of `D-hard-citation-gate`, not a generic structural Tier-0 invariant; the generic Java citation-gate deferral remains valid.
- C5 S2 `intake_complete_required` predicate semantics: CONFIRM `NOT A CANDIDATE`. The predicate already exists since Sprint 7 and M2 relocates it into Skill guardrails without new semantics; Tier-0 codification now would not add load-bearing protection.
- Aggregate: CONFIRM. No separate Tier-0 write-up file is required for Sprint 37 because no candidate is QUALIFIED-AND-ELEVATE; C2 and C3 should be re-evaluated at M2 close after the relevant implementation evidence exists.

## OQ Independent Verification
- OQ-7.11 `request_handover` decision tree: AGREE WITH DEV. Reading `system_prompt.txt:54-101` directly, the block maps observable situations to canonical `escalation_reason` enum values and enforces priority among reasons (especially explicit human request), but it does not encode active-UC-pair routing logic such as `if UC == X then reason Y`. The UC-specific mentions at lines 86 and 88-89 are compatibility/exclusion guidance for canonical reason selection, not a hidden Skill-body branch table. Keeping the decision tree in the orchestration shell is consistent because the 23-value enum is universal across Skills that allow `request_handover`; moving it into individual Skills would increase duplication without improving §4.1 Q6 compliance.
- OQ-7.2 / OQ-7.3: CONFIRM DEFER via the Tier-0 verification above; any elevation should wait for Sprint 39 / Sprint 41 runtime evidence.
- OQ-7.4: CONFIRM the design doc §12 enumeration is sufficient for QUALIFIED-DEFER candidates; no separate write-up file is needed at Sprint 37 close under the Sprint 36 precedent.
- OQ-7.10: CONFIRM no dev revisit. Supersession housekeeping is not part of `51c327c`; the direct predecessor supersession is already in `fd7396a`, and the upstream chained pointer is present and untouched.
- Other OQs remain downstream planning items; no informational disagreement is raised.

## Deferred / Non-Blocking Notes
- The MATERIAL FINDING that Sprint 6/7/11 predicates already ship is honest and useful; it should shape Sprint 39 contract drafting but is not a Sprint 37 blocker.
- The premise #7 `system_prompt.txt` 101-line refinement is accurate and should inform Sprint 40 planning, especially the treatment of lines 45-52 and preservation of lines 54-101.
- The Sprint 33 cue/slot split is accurate and should be preserved exactly through Sprint 38 and Sprint 40 migrations.
- C2 and C3 are real architecture-health questions; carrying them as R-items for M3+ is the right governance posture under `feedback_constitution_discipline_vs_planning_anticipation.md`.
- M2 §5's acceptance recalibration means Alice / bad-case / interactive eval observations are intentionally not Sprint 37 gates; no eval rerun is expected from this review.
