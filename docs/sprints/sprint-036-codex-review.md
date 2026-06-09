## Sprint Review Decision
decision: pass
blocking_count: 0
summary: Sprint 36 passes the design-freeze close gate. The dev commit `eb65e2b..ed71031` touches only the three authorized docs, the proposed Skill Foundation design preserves the LLM-owned vs Runtime-owned boundary, D3 reproduces the 2026-05-17 human authorization verbatim and keeps the S1 citation predicate within the exact RESOLVE_FAQ / `record_outcome(class=resolve)` / `source_id`-presence scope, and both surfaced Tier-0 candidate elevations are correctly deferred for re-evaluation after downstream trace evidence (especially Sprint 37) is available.

## Review Evidence
- Reviewed Sprint 36 commit range `eb65e2b..ed71031` (`sprint 36: Skill foundation + UC-switching continuity design freeze (M2-Skill sub-sprint 1)`).
- Loaded the governance chain (`AGENTS.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/iteration_governance.md`) plus `docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/sprints/sprint-036-handoff.md`, `docs/proposals/skill_foundation_design.md`, `docs/proposals/skill_orchestration_candidates.md`, `docs/runtime_freeze_and_risk_policy.md`, M1 closure context, and `compact/sprint-036-dev-prompt.md`.
- Scope diff verified: `git diff --name-only HEAD^..HEAD` returns exactly `docs/proposals/skill_foundation_design.md`, `docs/proposals/skill_orchestration_candidates.md`, and `docs/sprints/sprint-036-handoff.md`; a forbidden-path scan over the commit returned an empty set.
- Upstream proposal edit verified: `docs/proposals/skill_orchestration_candidates.md` carries `status: superseded` and `superseded_by: docs/proposals/skill_foundation_design.md`; body hash before/after frontmatter is identical (`85409485b1e5b9dfa1a1accc08a8c6629af75e241b69e09a7a3c54012bb34ffc`).
- D3 authorization verified: the quote in `docs/proposals/skill_foundation_design.md:362` matches `docs/milestone_objective.md` §6 #4 verbatim, including the narrow S1 scope and the preserved generic-gate deferral.
- Material finding independently verified in a clean detached worktree at `ed71031`: `AgentRunLoopImpl.java:690-734` is the Sprint 6 FAQ-miss handover guard, `AgentRunLoopImpl.java:628-651` is the Sprint 7 incomplete-intake handover guard, and `AgentRunLoopImpl.java:745-759` is the Sprint 11 premature-resolve outcome guard.
- Premise #8 refinement independently verified: `UseCaseRegistryService.UseCaseDefinition` is a six-field record at `UseCaseRegistryService.java:166-173` with no `requiredIntakeFields`; the required-field source is `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC` plus `requiredFieldsFor`, `canonicalFieldName`, `fieldsRemaining`, and `intakeComplete` at `IntakeFieldsRegistry.java:53-193`.
- Java validation rerun from a clean detached worktree at `ed71031`: `cd server && mvn test -q` exited 0; surefire totals were `Tests run: 983, Failures: 0, Errors: 0, Skipped: 2`.

## Blocking Findings
None.

## Anti-Hardcode Kernel
1. Q1 keyword / regex / if-else / enum / per-UC matrix: PASS. D1 is principle-level envelope teaching, D2-D4 are Runtime-floor predicates, and D5 uses a registry-level intake-field intersection, not a per-UC-pair branch table.
2. Q2 Tier-0 justification: PASS. D3 and D4 surface Tier-0 candidate questions in §8.3/§8.4 but do not silently codify them; DEFER is sound for both candidates.
3. Q3 soft signal achievable: PASS. D5 is explicitly a soft-signal projection (`prior_use_case_carry`) plus state-preservation rule; D3/D4 are terminal predicates for §1.4 grounding/capability floors where soft-signal-only would not be a predicate.
4. Q4 visible-eval / CaseSpec id encoding: PASS. The design references failure shapes and prospective coverage in prose only; no CaseSpec id, Alice trace phrase, or article slug is fed into runtime, prompt, or judge config as a predicate condition.
5. Q5 semantic ownership shift: PASS. D1 leaves UC hypothesis, next action, escalation posture, response strategy, customer wording, and per-step arguments with the LLM; Java predicates only enforce citation-presence and intake-completeness floors.
6. Q6 prompt as if-else dump: PASS. Proposed Sprint 37/38/39 teaching paragraphs are sibling to `already_called`, `alternate_candidate_use_cases`, and `discover_disambiguation_signals` principle-level guidance, not if/else decision tables.
7. Q7 tool / capability / PII / grounding floor: PASS. Tool schema, permissions, PII, and safety surfaces are unchanged; D3 is the narrow human-authorized grounding-floor extension, and D4 preserves the existing capability-floor pattern.
8. Q8 generalization coverage: PASS for design-freeze shape. Sprint 36 ships no behaviour change; Track B prospectively names target/neighbor/negative/shadow coverage for Sprints 37/38/39, including S1 negative `record_outcome(class=escalate)` traces and S2 legitimate intake-complete negatives.
9. Q9 rollback / sunset: PASS. D1-D5 are permanent architecture surfaces; no temporary workaround is introduced. If Sprint 37 evidence shows the narrow S1 predicate is insufficient, the design itself calls for STOP + human/Codex escalation rather than silent broadening.

## §1.7 Boundary Check
- D1 envelope: PASS. `docs/proposals/skill_foundation_design.md:155-247` defines the envelope as existing `PhasePlan` data plus principle-level `system_prompt.txt` teaching; it does not encode raw eval phrases or per-UC branch logic.
- D2 predicate framing: PASS. `docs/proposals/skill_foundation_design.md:259-359` places terminal predicates in the Runtime dispatch layer only for §1.4 floors and excludes observation-only pseudo-predicates.
- D3 S1 predicate: PASS. `docs/proposals/skill_foundation_design.md:400-431` exactly scopes the predicate to RESOLVE_FAQ, `record_outcome(class=resolve)`, and `source_id` citation presence; `docs/proposals/skill_foundation_design.md:494-515` explicitly excludes `class=escalate`, content-quality judgement, fan-out beyond RESOLVE_FAQ, generic Java citation gating, and per-trace matching.
- D3 prompt boundary: PASS. Recommended order is taught as guidance; enforcement is a Java guard at the `record_outcome` dispatch site and only for the Runtime-owned floor.
- D4 S2 predicate: PASS. `docs/proposals/skill_foundation_design.md:551-632` uses `IntakeFieldsRegistry.intakeComplete` and either the already-shipped reject-and-hint pattern or the existing canonical `incomplete_intake` reason; it does not widen the `escalation_reason` enum.
- D5 UC-switching continuity: PASS. `docs/proposals/skill_foundation_design.md:648-808` is a five-row principle matrix; per-UC variation enters only through `IntakeFieldsRegistry.canonicalFieldName` + `requiredFieldsFor` intersection, with `prior_use_case_carry` as a soft LLM-readable projection.

## Hard-Fence Verification
- Sprint 36 §6 #1 PASS: no `server/src/main/**` change in `eb65e2b..ed71031`.
- Sprint 36 §6 #2 PASS: no `server/src/test/**` change.
- Sprint 36 §6 #3 PASS: no `server/src/main/resources/prompts/system_prompt.txt` change.
- Sprint 36 §6 #4 PASS: no `eval_interactive/eval_interactive/**` harness/loader/simulator change.
- Sprint 36 §6 #5 PASS: no existing case-family change under `eval_interactive/case_specs/case_families/**`.
- Sprint 36 §6 #6 PASS: no shadow case-family change.
- Sprint 36 §6 #7 PASS: no smoke CaseSpec change.
- Sprint 36 §6 #8 PASS: no bad-case CaseSpec change.
- Sprint 36 §6 #9 PASS: no probe CaseSpec change.
- Sprint 36 §6 #10 PASS: no `eval_interactive/case_spec_overrides.yaml` change.
- Sprint 36 §6 #11 PASS: no edits to `docs/current/iteration_governance.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, or `docs/runtime_freeze_and_risk_policy.md`.
- Sprint 36 §6 #12 PASS: no `docs/foundational/**` change.
- Sprint 36 §6 #13 PASS: no archive edits under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-035-*`.
- Sprint 36 §6 #14 PASS: no `docs/milestones/**` change.
- Sprint 36 §6 #15 PASS: no `docs/milestone_objective.md` change.
- Sprint 36 §6 #16 PASS: no `docs/action_bank.md` change.
- Sprint 36 §6 #17 PASS: no Tier-0 invariant added; candidate write-ups live only in `skill_foundation_design.md` §8.
- Sprint 36 §6 #18 PASS: no mocked-LLM evidence used for design decisions; code-shape claims were verified by reading Java source.
- Sprint 36 §6 #19 PASS: no S1 predicate expansion beyond the human-authorized scope.
- M2-Skill §6 #1 PASS: the design contains no per-UC-branch if-else in skill bodies; D5 rejects per-UC-pair continuity tables.
- M2-Skill §6 #2 PASS: terminal predicates are Java guards only for Runtime-owned grounding/capability floors.
- M2-Skill §6 #3 PASS: recommended order remains soft prompt guidance; the LLM owns deviation.
- M2-Skill §6 #4 PASS: the HARD FENCE INVERSION is reproduced verbatim and bounded to Phase = RESOLVE_FAQ, tool call = `record_outcome(class=resolve)`, check = `source_id` citation present.
- M2-Skill §6 #5 PASS: no `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, or `ClassifyUseCaseTool.java` touch.
- M2-Skill §6 #6 PASS: no edit to `PhaseEvaluator.INTAKE_UCS` at `PhaseEvaluator.java:30`.
- M2-Skill §6 #7 PASS: no `escalation_reason` enum widening; `incomplete_intake` already exists in `EscalationReasonResolver.java:51-67` and priority table `:84-103`.
- M2-Skill §6 #8 PASS: no Tier-0 invariant added without authorization; §8.3/§8.4 are candidate write-ups only.
- M2-Skill §6 #9 PASS: no existing case-family edit under `eval_interactive/case_specs/case_families/**`.
- M2-Skill §6 #10 PASS: no existing shadow CaseSpec edit.
- M2-Skill §6 #11 PASS: no `eval_interactive/eval_interactive/**` harness/loader/simulator edit.
- M2-Skill §6 #12 PASS: no `docs/current/iteration_governance.md` edit.
- M2-Skill §6 #13 PASS: no `eval_interactive/case_spec_overrides.yaml` widening.
- M2-Skill §6 #14 PASS: no sprint archive edit under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-035-*`.
- M2-Skill §6 #15 PASS: no `docs/foundational/**` edit.
- M2-Skill §6 #16 PASS: no `docs/milestones/M1_objective.md` edit.
- M2-Skill §6 #17 PASS: no mocked-LLM primary evidence used for an LLM-behaviour claim.
- M2-Skill §6 #18 PASS: no Skill prescribes customer-facing language; wording remains LLM-owned.
- M2-Skill §6 #19 PASS: no Skill hard-encodes per-step argument values; tools/order are described, arguments stay LLM-owned.
- Material finding PASS: S1/S2 partial predicate surfaces already exist in `AgentRunLoopImpl`; Sprint 36 accurately frames Sprint 37/38 as formalization + narrow extension rather than from-scratch predicate work.
- Premise #8 refinement PASS: the dev correction from `UseCaseDefinition.requiredIntakeFields` to `IntakeFieldsRegistry` is correct, and D4 §5.2 cites the correct source.

## Schema And Reproducibility Checks
- Design doc structure PASS: `docs/proposals/skill_foundation_design.md` has top-level sections §1-§8 and covers all six sub-decisions D1-D6.
- Design doc §7 PASS: all nine §4.1 questions Q1-Q9 are present and answered.
- Design doc §8 PASS: Tier-0 candidate write-ups for OQ 7.1/S1 and OQ 7.2/S2 include explicit human-review escalation paths and recommended DEFER defaults.
- Upstream proposal frontmatter PASS: `docs/proposals/skill_orchestration_candidates.md` has `status: superseded` and `superseded_by: docs/proposals/skill_foundation_design.md`; body unchanged byte-for-byte after frontmatter removal. The additional supersession note stays within frontmatter and is consistent with the authorized frontmatter-only edit.
- Handoff structure PASS: `docs/sprints/sprint-036-handoff.md` has sections §1-§12, §9 lists only the three authorized files, and §12 is a closure-verdict placeholder rather than a dev-filled verdict.
- Cited line spot-check PASS: `PhaseEvaluator.java:618-665` contains RESOLVE-FAQ S1 sequence teaching and citation instruction; `PhaseEvaluator.java:552-596` contains RESOLVE-INTAKE plan construction; `PhasePlan.java:21-33` contains the 11 existing record fields reused by D1.
- Cited line spot-check PASS: `RecordOutcomeTool.java:42-94` dispatches `record_outcome`, and `RecordOutcomeTool.java:110-121` normalizes `resolve/escalate/abandon` with legacy aliases.
- Cited line spot-check PASS: `ContextProjectionBuilder.java:346-377`, `:396-416`, and `:418-432` contain the existing `intake_state`, `alternate_candidate_use_cases`, and `discover_disambiguation_signals` projection-slot precedents.
- Cited line spot-check PASS: `system_prompt.txt:23-28`, `:30-33`, and `:36-40` contain the existing principle-level teaching paragraph precedents.

## Validation Runs
- Created a clean detached worktree at `/tmp/csagent-s36-review.wW1mfc/wt` for validation so the primary dirty working tree did not affect Java results.
- Command: `cd /tmp/csagent-s36-review.wW1mfc/wt/server && mvn test -q`.
- Result: exit code 0; surefire XML totals `Tests run: 983, Failures: 0, Errors: 0, Skipped: 2`.
- Interpretation: no Sprint 36 Java regression. This is stricter than the dirty-working-tree `983 / 1 / 0 / 2` baseline noted in the prompt; the inherited `SystemPromptUserRequestedTiebreakerTest` failure is tied to unauthored dirty `system_prompt.txt` state, not the clean dev commit.
- No design-doc YAML loader check run: `skill_foundation_design.md` is markdown prose, not a YAML CaseSpec.
- No eval run required: Sprint 36 ships zero runtime, prompt, judge, or eval-surface behaviour change.

## Tier-0 Candidate DEFER Rationale Review
- OQ 7.1 / S1 citation predicate: CONFIRM DEFER. The predicate is narrow and Skill-bounded by explicit human authorization; Sprint 37 has not yet produced implementation traces; and the generic `D-hard-citation-gate` deferral remains valid. Elevating exact wording into `docs/runtime_freeze_and_risk_policy.md` before observing Sprint 37 traces would risk freezing a suboptimal phrase or scope.
- OQ 7.2 / S2 intake-completeness predicate: CONFIRM DEFER. The predicate has shipped since Sprint 7 via `shouldRejectIncompleteIntakeHandover`; Tier-0 elevation now would add documentation weight but no new architectural protection or code safety. It can be reconsidered at normal fold-back cadence or M2 close.
- Re-evaluation flag: revisit both candidates at M2 close, especially after Sprint 37 evidence shows whether the narrow S1 citation-presence predicate is sufficient as a minimum grounding-floor guard or whether a new human-authorized design round is needed.

## Deferred / Non-Blocking Notes
- D3.1 remains a Sprint 37 planning pick: default reject-and-hint for citation-missing `record_outcome(class=resolve)` is sound; the alternative `resolve_no_citation` outcome class would be a larger persistence/eval surface and is not recommended by default.
- D4.1 remains a Sprint 38 planning pick: keeping the already-shipped reject-and-hint pattern is sound; canonical downgrade to existing `incomplete_intake` remains an optional design choice, not a Sprint 36 blocker.
- D5.1 / D5.2 remain Sprint 39 planning picks: dropped-intake-fields projection as a soft signal, plus `prior_use_case_carry` cap/aging defaults (cap=3, aging=4), are reasonable but non-blocking.
- D1.1 remains a Sprint 37 planning pick: no explicit `skill_envelope` projection field by default is sound unless observed traces show the LLM needs one.
- Premise #8 correction is mechanical and should be baked into Sprint 38's contract draft: cite `IntakeFieldsRegistry.requiredFieldsFor` / `intakeComplete`, not `UseCaseDefinition.requiredIntakeFields`.
- Residual risk to monitor after Sprint 37: the authorized S1 predicate is a minimum citation-presence floor only; it intentionally does not judge citation relevance or factual content quality. If Alice-style traces still fabricate content while carrying a `source_id`, that is a new Sprint 37/M2-close evidence item for human/Codex review, not a reason to broaden Sprint 36's frozen scope silently.
