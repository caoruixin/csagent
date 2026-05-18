# Current Handoff

Date: 2026-05-18
Branch: `refactor/remove-the-shackles`

## 1. Current phase

Current phase:
**Milestone M2 — Skill Registry Abstraction + Wholesale Retroactive Externalization (LLM-led, Policy-bounded)** (NEW M2 framing; supersedes OLD M2-Skill mid-flight 2026-05-17). **State: NEW Sprint 41 (M2 sub-sprint 5; LAST M2 sub-sprint) CLOSED A — Clean PASS 2026-05-18** (dev commit `8cd0a10`; Codex per-sub-sprint review per `iteration_governance.md` §4.3 trigger #3 (session state model touch across Skill boundary; C3 Tier-0 candidate evidence surface) returned `decision: pass / blocking_count: 0` in a single round; archive: `docs/sprints/sprint-041-objective.md` + `docs/sprints/sprint-041-handoff.md` + `docs/sprints/sprint-041-codex-review.md`). **M2 implementation track CLOSED.** Sprint 41 close + deliver-agent housekeeping bundle in flight (Sprint 41 codex-review archived; Sprint 41 sprint_objective archived to `docs/sprints/sprint-041-objective.md`; codex-findings reset to scaffold; action_bank updated with `D-skill-runtime-framework` FULL LANDING annotation (Sprints 37 + 38 + 38-fix + 39 + 40 + 41 ALL shipped) + Sprint 41 close-action index row + C3 R-item Sprint 41 enforcement evidence + Codex DEFER verdict; 10-handoff §1 lead refreshed; `compact/M2-review-prompt.md` drafted for human dispatch). **Next planning round: M2 milestone close** per `iteration_governance.md` §8.4 — milestone-shared cumulative Codex review against `51c327c..8cd0a10` (NEW M2 sub-sprints 1-5 inclusive); design doc editorial fold-back per `doc_governance.md` cadence (6 fold-back items queued); M2 archive to `docs/milestones/M2_objective.md`; §6.5 closed-milestone index row; 10-handoff §1 lead refresh (M2 → Preceding milestone; M3 → Current); C2 + C3 R-item re-evaluation; M3 candidate selection per `docs/milestone_objective.md` §11. **NEW M2 acceptance bar recalibrated 2026-05-17 per human direction**: bad-case suite (Alice) + interactive eval composite_score are OBSERVATION only (NOT hard gate) — architecture-focused milestone; primary gate = functional review + Java tests + Sprint 37 freeze decisions honored across implementation sub-sprints (S38-S41) + per-sub-sprint Codex review `approve` verdicts. **Sprint 41 close summary:** dev `8cd0a10` shipped 46 files (10 main + 3 NEW test classes + 33 EDIT existing-test ctor-update + 1 NEW handoff; +1734 / -44 per `git show --stat 8cd0a10`): NEW `SkillStateBus.java` (203 lines; Spring `@Component`; design doc §10.3 public surface `applyOnSkillSwitch` + `inheritanceFor`; registry-driven `applyIntakeFieldsIntersection` via `IntakeFieldsRegistry.requiredFieldsFor` + `canonicalFieldName`; switch over 3 schema-allowed state keys — NOT per-UC-pair branch table; defensive no-ops on null inputs, same-Skill turns, null intake_fields); `ContextProjectionBuilder.java` EDIT (1161 → 1341; +180; NEW `prior_use_case_carry` projection slot at the `discover_disambiguation_signals` sibling emission site per design doc §10.4; NEW `buildPriorUseCaseCarryNode(BotSession, List<BotTurn>)` private helper; 2 NEW constants `PRIOR_USE_CASE_CARRY_AGING_TURNS = 4` + `PRIOR_USE_CASE_CARRY_CITATION_CAP = 3` per §10.4 / §10.8 — SINGLE integer constants, NO per-UC variation; ctor +1 arg `SkillRegistry`; slot shape matches §10.4 example verbatim — `prior_citations[]` of {source_id, from_use_case, turn_index} + `prior_active_use_case` + `prior_skill_name` (or null) + `ages_out_after_turns: 4`); `PhaseEvaluator.java` EDIT (1427 → 1482; +55; NEW `maybeApplyStateBusOnSwitch` private helper + SkillStateBus invocation in `plan(...)` between `SkillRegistry.select` and `composeSkillPhasePlan`; Skill-switch detection per design doc §10.3 — prior UC ≠ current UC AND prior Skill ≠ new Skill; detection rides on existing M1 surfaces — NO new classifier; M2 §6 #5 fence preserved; defensive null-checks on missing SkillStateBus test seam + empty history fresh session; ctor +1 arg `SkillStateBus`); 6 Skill YAMLs EXTEND with `state_inheritance` blocks per §10.5 matrix (`discover_triage.yaml` 30→32; `confirm.yaml` 29→30; `resolve_faq_grounded_answer.yaml` 52→53 — Sprint 39 forward-compat placeholder for `prior_use_case_carry` consumed; `resolve_intake_collect_and_handover.yaml` 37→38 — Sprint 39 forward-compat placeholder consumed; `escalate.yaml` 28→29; `terminal.yaml` 25→26); `Skill.java` / `StateInheritance.java` / `SkillLoader.java` schema validation VERIFIED wired for full `state_inheritance` declarations via `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists from Sprint 38-fix (no source edits; no schema drift); 3 NEW test classes (`SkillStateBusTest` 306 lines / 17 tests covering §10.5 matrix × inherit/reset/soft + registry intersection + Skill-switch trigger + same-Skill no-op + null inputs + EMPTY declaration + `inheritanceFor` diagnostic; `PriorUseCaseCarryProjectionTest` 279 lines / 13 tests covering slot shape + cap=3 + aging=4 + null cases + cross-UC shape invariance + `prior_skill_name` resolution; `UcSwitchStateInheritanceTest` 246 lines / 9 integration tests covering representative Skill switches end-to-end through `PhaseEvaluator.plan(...)` → `SkillStateBus.applyOnSkillSwitch(...)`); 33 EDIT existing-test files (ctor-signature update only; passing `null` for new dependency arg — `SkillStateBus` for PhaseEvaluator, `SkillRegistry` for ContextProjectionBuilder; defensive null-checks preserve behavioural equivalence; OQ-S41.3 ACCEPTABLE STYLE per Sprint 40 ctor-update precedent; Drift §7-a REAFFIRM Sprint 39+40 ctor-update precedent extension); 414-line dev handoff; D-g optional `system_prompt.txt` orchestration-shell teaching about `prior_use_case_carry` SKIPPED per dev judgement (slot intrinsically self-documenting; Sprint 40 envelope-mechanics paragraph principle). Java baseline: `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2` (1105 post-S40 + 39 new = 1144; inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure persists unchanged per OQ-S41.5 STATUS QUO). **Codex independent verification** confirmed: scope discipline (46 files match Sprint 41 contract §5 + 33 ctor-update extending Sprint 39 + Sprint 40 precedent per Drift §7-a REAFFIRM); §4.1 9-question kernel `approve` per Q1-Q9 with file:line citations (Q1 `SkillStateBus.java:99/117/175` registry-driven; Q2 `runtime_freeze_and_risk_policy.md` UNCHANGED; Q3 soft-projection slot at `ContextProjectionBuilder.java:459`; Q4 no eval-phrase encoding; Q5 LLM §1.3 ownership preserved; Q6 `system_prompt.txt` UNCHANGED; Q7 tool/capability/PII/grounding floor preserved; Q8 39 NEW tests + 33 existing-test ctor-update preserve behavioural equivalence; Q9 `git revert 8cd0a10` clean); §1.7 boundary check PASS on all 5 items; all 33 Sprint 41 §6 + all 22 M2 §6 hard fences honored; M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved (`Sprint71PartialIntakePersistenceTest` 14/14 + `SkillTeachingMigrationIntegrationTest` 11/11 + `PhaseEvaluatorResolveSkillIntegrationTest` 14/14 + `SkillGuardrailDispatcherTest` 24/24 + `ResolveFaqGuardrailsTest` 11/11 + `ResolveIntakeGuardrailsTest` 11/11 + `SkillTest` 9/9 + `SkillRegistryTest` 11/11 + `SkillLoaderTest` 18/18 + `Sprint7CandidateUseCasesProjectionTest` 7/7 + `DiscoverDisambiguationSignalsProjectionTest` 9/9 all PASS); `Skill.java` 135 + `StateInheritance.java` 59 + `SkillLoader.java` 313 + `SkillGuardrailDispatcher.java` 416 + `IntakeFieldsRegistry.java` 222 + `UseCaseRegistryService.java` 174 + `ResolveDispositionEvaluator.java` 205 + `AgentRunLoopImpl.java` 638 + `ControlKernel.java` + `system_prompt.txt` 80 + `runtime_freeze_and_risk_policy.md` + `skill_registry_design.md` all UNCHANGED via empty `git diff`. **OQ disposition** (6 OQs all resolved with Codex independent verdicts; deliver-agent + human withheld pre-decisions from review prompt per "surface OQs without recommendations" framing): OQ-S41.1 (D-g `system_prompt.txt` teaching SKIPPED) **DISAGREE IN PART, NON-BLOCKING** — slot shape IS self-describing but the current `phase_plan` projection does NOT expose `state_inheritance.soft_signal_via_projection` to the LLM (partial code-untruth in dev's rationale); routes to M2 close one-line shell pointer for `prior_use_case_carry`; NOT Sprint 41 fix-requirement. OQ-S41.2 (C3 Tier-0 candidate elevation timing — LOAD-BEARING) **DEFER continued** — Codex evidence-based verdict aligned with deliver-agent + human pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md`; Codex direct quote: "structural evidence... is necessary and compelling, but I do not elevate C3 now because the Tier-0 claim is operationally about the LLM having no production path to override state inheritance across real traces; observe through Sprint 41/M2 close/M3+ before freezing it"; no `runtime_freeze_and_risk_policy.md` edit. OQ-S41.3 (33 existing-test ctor `null` arg style) **ACCEPTABLE STYLE** per Sprint 40 ctor-update precedent + defensive null-checks + dedicated real-fixture tests in NEW Sprint 41 test classes. OQ-S41.4 (`accumulated_tool_results` declared inherit on all 6 Skills vs design doc §10.5 row 1 "All resolve-side Skills" wording) **FLAG AS DESIGN-DOC EDITORIAL DIVERGENCE** — runtime effect no-op (no persistent session field); routes to M2 close editorial fold-back. OQ-S41.5 (inherited test PERSISTS) **STATUS QUO** — not Sprint 41 blocker per contract §10 #20. OQ-S41.6 (template-vs-implementation divergence) **MINOR** — only OQ-S41.4 surfaced; no major architectural divergence. **4 contract-drift items resolved:** Drift §7-a Sprint 39+40 ctor-update precedent extension to 33 files (REAFFIRM PRECEDENT); §7-b Java baseline 1144 within target 1140-1155 (pass per independent rerun); §7-c line-count target gaps on `ContextProjectionBuilder.java` (+180) + `PhaseEvaluator.java` (+55) — CONTENT-COMPLIANT (advisory targets; helper sizes justified by required null/aging/cap/registry handling); §7-d deliver-agent review prompt numbers-cite drift — Codex independently flagged `ContextProjectionBuilder.java` actual numstat `181/1` vs prompt's `182/2`, `PhaseEvaluator.java` actual `57/2` vs prompt's `59/3`, 9 main-source files vs prompt's "10 files" label (third+ observed instance of `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` pattern). **Tier-0 candidate disposition at Sprint 41 close:** No new Tier-0 invariant added. C1 REJECTED preserved by construction. C2 QUALIFIED-DEFER continued — Sprint 41 ships nothing new C2-relevant. **C3 QUALIFIED-DEFER continued (LOAD-BEARING for Sprint 41 close)** — Sprint 41 ships FIRST observed evidence surface (`SkillStateBus.applyOnSkillSwitch` enforcement + 17 unit + 9 integration tests demonstrating LLM CANNOT override `state_inheritance`); Codex evidence-based verdict aligned with deliver-agent + human pre-decision. R-item `R-skill-state-bus-boundary-enforcement-tier-0` annotated with Sprint 41 enforcement evidence + Codex DEFER verdict; STAYS OPEN; re-evaluation flagged at M2 close OR M3+. C4 + C5 NOT A CANDIDATE preserved. **Codex non-blocking observations:** (a) deliver-agent's Sprint 41 review prompt numstat drift (third+ instance of pattern); next deliver-agent review prompts MUST derive every numstat number from `git show --numstat <commit>` directly before citing. (b) `phase_plan` projection currently omits `state_inheritance.soft_signal_via_projection` — partial code-untruth in dev's D-g skip rationale; M2 close one-line shell pointer for `prior_use_case_carry` recommended. (c) Sprint 41 OQ-S41.4 `accumulated_tool_results` inheritance wording joins M2 close fold-back queue. **R-item flips at Sprint 41 close:** `D-skill-runtime-framework` FULL LANDING annotation (Sprints 37 + 38 + 38-fix + 39 + 40 + 41 ALL shipped; M2 implementation track CLOSED — original deferral fully consumed by NEW M2 milestone). C2 R-item STAYS OPEN. C3 R-item annotated with Sprint 41 enforcement evidence + Codex DEFER verdict; STAYS OPEN; re-evaluation at M2 close OR M3+. Sprint 41 close-action index row added to `docs/action_bank.md` §6. **M2 close fold-back queue at Sprint 41 close (6 items):** Sprint 37 design doc §7.8 typo (OQ-S40.5); Sprint 38 OQ-S38.1 template-vs-legacy DISCOVER divergence; Sprint 39 §10 design doc §6.2.5/§6.2.6 RESOLVE template divergence; Sprint 39 OQ-S39.7 stale test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`; NEW Sprint 41 OQ-S41.1 one-line shell pointer for `prior_use_case_carry`; NEW Sprint 41 OQ-S41.4 `accumulated_tool_results` inheritance wording. **Sprint 41 final classification: A — Clean PASS** (M2 implementation-sub-sprint pattern: S37 PASS A → S38 B-fix-iterated → S39 PASS A → S40 PASS A → S41 PASS A — fourth clean A close under NEW M2; third clean A on an implementation sub-sprint; LAST M2 sub-sprint; M2 implementation track CLOSED). `docs/milestone_objective.md` carries NEW M2 contract (5 sub-sprints S37-S41 ALL DONE; M2 close planning round pending). `docs/sprint_objective.md` post-Sprint-41-close placeholder pending M2 close planning round (deliver-agent + human draft after M2-shared Codex review returns). **After Sprint 41 close → M2 milestone close planning round** (milestone-shared cumulative Codex review per `iteration_governance.md` §4.3 against `51c327c..8cd0a10` via `compact/M2-review-prompt.md` + design doc editorial fold-back per `doc_governance.md` cadence + M2 archive to `docs/milestones/M2_objective.md` + §6.5 closed-milestone index row in action_bank + 10-handoff §1 lead refresh + C2 + C3 R-item re-evaluation + M3 candidate selection per `docs/milestone_objective.md` §11 cross-milestone sequencing — M3-A customer-honesty / M3-B Single Handover Orchestrator / M3-C Sprint 5 S3/S4/S5 skills / M3-D Topic↔UC binding loosening / M3-Latency / M3-Skill-Tuning / M3-Tier-0 re-evaluation).
---

Preceding sub-sprint:
**Sprint 41 (NEW M2 sub-sprint 5; LAST M2 sub-sprint) — UC switch + state preservation across Skill boundary (NEW `SkillStateBus.java` Spring `@Component` mediating session-level state across Skill boundaries on UC switch; per-Skill `state_inheritance` declarations on all 6 Skill YAMLs per §10.2 schema + §10.5 invariant matrix; NEW `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` per §10.4; Skill-switch detection in `PhaseEvaluator.plan(...)` per §10.3 — detection rides on existing M1 surfaces (no new classifier per M2 §6 #5 fence) per Sprint 37 freeze decision (i) §10)** closed **A — Clean PASS** 2026-05-18 (dev commit `8cd0a10`; Codex per-sub-sprint review per `iteration_governance.md` §4.3 trigger #3 (session state model touch across Skill boundary; C3 Tier-0 candidate evidence surface) returned `decision: pass / blocking_count: 0` in a single round; archive: `docs/sprints/sprint-041-objective.md` + `docs/sprints/sprint-041-handoff.md` + `docs/sprints/sprint-041-codex-review.md`). **LAST M2 implementation sub-sprint; M2 implementation track CLOSED.** Fourth implementation sub-sprint under NEW M2 (fifth sub-sprint overall after Sprint 37 design freeze + Sprint 38 SkillRegistry-core landing + Sprint 38-fix iteration + Sprint 39 RESOLVE migration + dispatcher + Sprint 40 teaching extraction). Multi-fence convergence shape (similar to Sprint 39): 46 files; +1734 / -44 per `git show --stat 8cd0a10` (per-file numstat per `git show --numstat 8cd0a10`): NEW `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` (203 lines; Spring `@Component`; design doc §10.3 public surface `applyOnSkillSwitch(Skill, Skill, BotSession)` + `inheritanceFor(Skill)`; private `applyIntakeFieldsIntersection(BotSession)` is REGISTRY-DRIVEN via `IntakeFieldsRegistry.requiredFieldsFor(...)` + `canonicalFieldName(...)` — SINGLE registry lookup, NOT per-UC-pair branch table per §1.7; switch over 3 schema-allowed state keys; defensive no-ops on null inputs, same-Skill turns, null intake_fields); `ContextProjectionBuilder.java` EDIT (1161 → 1341; +180; numstat 181/1; NEW `prior_use_case_carry` projection slot at the `discover_disambiguation_signals` sibling emission site per design doc §10.4; NEW `buildPriorUseCaseCarryNode(BotSession, List<BotTurn>)` private helper ~115 lines; 2 NEW constants `PRIOR_USE_CASE_CARRY_AGING_TURNS = 4` + `PRIOR_USE_CASE_CARRY_CITATION_CAP = 3` per §10.4 / §10.8 — SINGLE integer constants, NO per-UC variation; ctor extended +1 arg `SkillRegistry`; slot shape matches §10.4 example verbatim — `prior_citations[]` of {source_id, from_use_case, turn_index} + `prior_active_use_case` + `prior_skill_name` (or null) + `ages_out_after_turns: 4`); `PhaseEvaluator.java` EDIT (1427 → 1482; +55; numstat 57/2; NEW `maybeApplyStateBusOnSwitch(History, BotSession, Skill newSkill)` private helper; invocation in `plan(...)` between `SkillRegistry.select` and `composeSkillPhasePlan`; Skill-switch detection per §10.3 — prior UC ≠ current UC AND prior Skill ≠ new Skill; detection rides on existing M1 surfaces — NO new classifier per M2 §6 #5 fence; defensive null-checks on missing SkillStateBus test seam + empty history fresh session; ctor extended +1 arg `SkillStateBus`); 6 Skill YAMLs EXTEND with `state_inheritance` blocks per §10.5 matrix (`discover_triage.yaml` 30→32; `confirm.yaml` 29→30; `resolve_faq_grounded_answer.yaml` 52→53 — Sprint 39 forward-compat placeholder for `prior_use_case_carry` consumed; `resolve_intake_collect_and_handover.yaml` 37→38 — Sprint 39 forward-compat placeholder consumed; `escalate.yaml` 28→29; `terminal.yaml` 25→26); `Skill.java` 135 + `StateInheritance.java` 59 + `SkillLoader.java` 313 schema validation VERIFIED wired for full `state_inheritance` declarations via `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists from Sprint 38-fix (no source edits; no schema drift); 3 NEW test classes (`server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillStateBusTest.java` 306 lines / 17 unit tests covering §10.5 matrix × inherit / reset / soft_signal_via_projection + registry intersection × 4 + Skill-switch trigger + same-Skill no-op + null inputs + EMPTY declaration + `inheritanceFor()` diagnostic; `server/src/test/java/com/gumtree/csagent/service/runtime/PriorUseCaseCarryProjectionTest.java` 279 lines / 13 unit tests covering slot shape + cap=3 + aging=4 + null cases + cross-UC shape invariance + `prior_skill_name` resolution; `server/src/test/java/com/gumtree/csagent/service/runtime/UcSwitchStateInheritanceTest.java` 246 lines / 9 integration tests covering representative Skill switches end-to-end through `PhaseEvaluator.plan(...)` → `SkillStateBus.applyOnSkillSwitch(...)`); 33 EDIT existing-test files at `server/src/test/java/com/gumtree/csagent/service/runtime/[...]` (ctor-signature update only; passing `null` for new dependency arg — `SkillStateBus` for PhaseEvaluator, `SkillRegistry` for ContextProjectionBuilder; defensive null-checks preserve behavioural equivalence; OQ-S41.3 ACCEPTABLE STYLE Codex verdict per Sprint 40 ctor-update precedent; Drift §7-a REAFFIRM Sprint 39+40 precedent extension); 414-line dev handoff at `docs/sprints/sprint-041-handoff.md`. D-g optional `system_prompt.txt` orchestration-shell teaching about `prior_use_case_carry` SKIPPED per dev judgement (slot intrinsically self-documenting; Sprint 40 envelope-mechanics paragraph principle); surfaced as OQ-S41.1 — Codex DISAGREED IN PART NON-BLOCKING (slot shape IS self-describing but `phase_plan` projection does NOT expose `state_inheritance.soft_signal_via_projection`; routes to M2 close housekeeping one-line shell pointer; NOT Sprint 41 fix-requirement). Java baseline: `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2` (1105 post-S40 + 39 new = 1144; inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure persists unchanged per OQ-S41.5 STATUS QUO). **Sprint 37 freeze fidelity:** decision (i) §10.1-§10.11 honored row-by-row per dev handoff §5 + Codex Schema And Reproducibility Checks; §10.2 schema (3 state keys + 3 projection slots); §10.3 SkillStateBus public surface + intersection pseudo-code + trigger conditions verbatim; §10.4 prior_use_case_carry slot shape verbatim with cap=3 + aging=4; §10.5 invariant matrix per-Skill declarations honored (5 dimensions × 5 outcomes); §10.7 alternatives A/B/C/D/E REJECTED preserved by construction; §10.8 §1.7 boundary check honored; §10.9 §1.3/§1.4 boundary check honored; §10.10 C3 Tier-0 candidate evidence shipped + Codex independent verdict DEFER continued (LOAD-BEARING — see Tier-0 disposition below). 1 design-doc editorial divergence surfaced as OQ-S41.4 (`accumulated_tool_results` declared inherit on all 6 Skills vs §10.5 row 1 "All resolve-side Skills" wording; runtime effect no-op since no persistent session field; Codex routes to M2 close editorial fold-back). **Codex independent verification** confirmed: scope discipline (46 files match Sprint 41 contract §5 + 33 ctor-update extending Sprint 39 + Sprint 40 precedent per Drift §7-a REAFFIRM); §4.1 9-question kernel `approve` per Q1-Q9 with file:line citations (`SkillStateBus.java:99/117/175` switch-over-allowlist + registry-driven intersection; `runtime_freeze_and_risk_policy.md` UNCHANGED; soft-projection slot at `ContextProjectionBuilder.java:459`; no eval-phrase encoding; LLM §1.3 ownership preserved; `system_prompt.txt` UNCHANGED at 80 lines; tool/capability/PII/grounding floor preserved; 39 NEW tests + 33 existing-test ctor-update preserve behavioural equivalence; `git revert 8cd0a10` clean); §1.7 boundary check PASS on all 5 items; all 33 Sprint 41 §6 + all 22 M2 §6 hard fences honored (no per-UC hardcode in bus body OR `state_inheritance` declarations OR `prior_use_case_carry` slot construction; no new classifier; S1/S2/dispatcher UNCHANGED; runtime freeze/M1 preservation; INTAKE_UCS + escalation_reason enum UNCHANGED; governance/archives UNCHANGED; M3 scope fences UNCHANGED); M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces preserved (`Sprint71PartialIntakePersistenceTest` 14/14 + `SkillTeachingMigrationIntegrationTest` 11/11 + `PhaseEvaluatorResolveSkillIntegrationTest` 14/14 + `SkillGuardrailDispatcherTest` 24/24 + `ResolveFaqGuardrailsTest` 11/11 + `ResolveIntakeGuardrailsTest` 11/11 + `SkillTest` 9/9 + `SkillRegistryTest` 11/11 + `SkillLoaderTest` 18/18 + `Sprint7CandidateUseCasesProjectionTest` 7/7 + `DiscoverDisambiguationSignalsProjectionTest` 9/9 all PASS); `Skill.java` 135 + `StateInheritance.java` 59 + `SkillLoader.java` 313 + `SkillGuardrailDispatcher.java` 416 + `IntakeFieldsRegistry.java` 222 + `UseCaseRegistryService.java` 174 + `ResolveDispositionEvaluator.java` 205 + `AgentRunLoopImpl.java` 638 + `ControlKernel.java` + `system_prompt.txt` 80 + `runtime_freeze_and_risk_policy.md` + `skill_registry_design.md` all UNCHANGED via empty `git diff`. **OQ disposition** (6 OQs all resolved with Codex independent verdicts aligned with deliver-agent + human pre-decisions where applicable; deliver-agent withheld pre-decisions from review prompt per "surface OQs without recommendations" framing): OQ-S41.1 (D-g shell teaching SKIPPED) **DISAGREE IN PART, NON-BLOCKING** — slot self-describing but `phase_plan` projection does NOT expose `state_inheritance.soft_signal_via_projection` (partial code-untruth in dev's rationale); routes to M2 close one-line shell pointer; NOT Sprint 41 fix-requirement. OQ-S41.2 (C3 Tier-0 elevation timing — LOAD-BEARING) **DEFER continued** — Codex evidence-based verdict aligned with deliver-agent + human pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md`; Codex direct quote: "structural evidence... is necessary and compelling, but I do not elevate C3 now because the Tier-0 claim is operationally about the LLM having no production path to override state inheritance across real traces; observe through Sprint 41/M2 close/M3+ before freezing it"; no `runtime_freeze_and_risk_policy.md` edit at Sprint 41 close. OQ-S41.3 (33 existing-test ctor `null` arg style) **ACCEPTABLE STYLE** per Sprint 40 ctor-update precedent + defensive null-checks + dedicated real-fixture tests in NEW Sprint 41 test classes. OQ-S41.4 (`accumulated_tool_results` declared inherit on all 6 Skills vs design doc §10.5 row 1 "All resolve-side Skills") **FLAG AS DESIGN-DOC EDITORIAL DIVERGENCE** — runtime effect no-op; routes to M2 close editorial fold-back. OQ-S41.5 (inherited test PERSISTS) **STATUS QUO** — not Sprint 41 close blocker per contract §10 #20; the "Sprint 6" anchor token has been absent since Sprint 24-era working-tree mod. OQ-S41.6 (template-vs-implementation divergence) **MINOR** — only OQ-S41.4 surfaced; no major architectural divergence. **4 contract-drift items resolved:** Drift §7-a Sprint 39+40 ctor-update precedent extension to 33 files **REAFFIRM PRECEDENT** (behavioural-equivalence fallout from ctor signature change; not scope creep); §7-b Java baseline 1144 within target 1140-1155 **pass** per independent rerun; §7-c line-count target gaps on `ContextProjectionBuilder.java` (+180) + `PhaseEvaluator.java` (+55) **CONTENT-COMPLIANT** — line-count targets were advisory; helper sizes justified by required null/aging/cap/registry handling; §7-d deliver-agent's Sprint 41 review prompt numbers-cite drift **NON-BLOCKING NUMBERS-CITE OBSERVATION** — Codex independently flagged `ContextProjectionBuilder.java` actual numstat `181/1` vs prompt's `182/2`, `PhaseEvaluator.java` actual `57/2` vs prompt's `59/3`, 9 main-source files vs prompt's "10 files" label; third+ observed instance of `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` pattern; next deliver-agent review prompts MUST derive every numstat from `git show --numstat <commit>` directly before citing. **Tier-0 candidate disposition at Sprint 41 close:** No new Tier-0 invariant added. C1 REJECTED preserved by construction. C2 QUALIFIED-DEFER continued — Sprint 41 ships nothing new C2-relevant. **C3 QUALIFIED-DEFER continued (LOAD-BEARING)** — Sprint 41 ships FIRST observed evidence surface (`SkillStateBus.applyOnSkillSwitch` enforcement + 17 unit + 9 integration tests demonstrating LLM CANNOT override `state_inheritance` declarations); Codex evidence-based verdict aligned with deliver-agent + human pre-decision (production trace observation across M2 close → M3+ window required before Tier-0 elevation). R-item `R-skill-state-bus-boundary-enforcement-tier-0` annotated with Sprint 41 enforcement evidence + Codex DEFER verdict; STAYS OPEN; re-evaluation flagged at M2 close OR M3+. C4 + C5 NOT A CANDIDATE preserved. **Codex non-blocking observations:** (a) deliver-agent's Sprint 41 review prompt numstat drift (third+ instance); (b) M2 close one-line shell pointer for `prior_use_case_carry` recommended (OQ-S41.1 partial code-untruth in dev D-g rationale); (c) Sprint 41 OQ-S41.4 `accumulated_tool_results` inheritance wording joins M2 close fold-back queue. **R-item flips at Sprint 41 close:** `D-skill-runtime-framework` FULL LANDING annotation (Sprints 37 + 38 + 38-fix + 39 + 40 + 41 ALL shipped; M2 implementation track CLOSED — original deferral fully consumed by NEW M2 milestone). C2 R-item STAYS OPEN. C3 R-item annotated with Sprint 41 enforcement evidence + Codex DEFER verdict; STAYS OPEN; re-evaluation at M2 close OR M3+. Sprint 41 close-action index row added to `docs/action_bank.md` §6. **M2 close fold-back queue at Sprint 41 close (6 items):** Sprint 37 design doc §7.8 typo (OQ-S40.5); Sprint 38 OQ-S38.1 template-vs-legacy DISCOVER divergence; Sprint 39 §10 design doc §6.2.5/§6.2.6 RESOLVE template divergence; Sprint 39 OQ-S39.7 stale test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39`; NEW Sprint 41 OQ-S41.1 one-line shell pointer for `prior_use_case_carry`; NEW Sprint 41 OQ-S41.4 `accumulated_tool_results` inheritance wording. Full handoff: `docs/sprints/sprint-041-handoff.md`; Codex archive: `docs/sprints/sprint-041-codex-review.md`.

---

Preceding milestone:
**Milestone M1 — DISCOVER + Intake** (closed PASS 2026-05-17 per `docs/milestones/M1_objective.md` §12 closure verdict; archive). Classification **A-with-Codex-finding-OOSR-classification + deliver-agent-finding-2-fix-in-close** (first milestone-shared close under the §8 framework, 2026-05-16 governance upgrade
to milestone framework per `iteration_governance.md` §8 — 3-5
coordinated sub-sprints per milestone, milestone-shared Codex
review at close, smoke composite_score demoted to observation per
§5.5, curated bad-case suite at `eval_interactive/case_specs/bad_cases/`
as new primary acceptance gate). M1 shipped three sub-sprints
(Sprint 33 `prompt_projection` commit `8a22aa6`; Sprint 34
`skill_state` commit `e532f0d`; Sprint 35 `eval_spec` commit
`eb65e2b`) across cumulative range `c9edb37..eb65e2b`. Java baseline
preserved 932 → 983 (+51 new tests; UC-K regression guard preserved
through M1). Codex milestone-shared review (`docs/sprints/M1-codex-review.md`)
returned `fix_required / blocking_count: 2`. Deliver-agent + human
classification at M1 close 2026-05-17: **Finding 1** (Alice
grounding fabrication on Codex independent rerun; layer =
`semantic_planner` per Codex triage) reclassified as
**`out_of_scope_review`** per M1 §6 hard fence #1 (semantic_planner
work is M1-out-of-scope by construction); opened new R-item
`R-grounding-discipline-iterative-search-fabrication` as
high-priority M2 anchor. **Finding 2** (Sprint 35 matrix §5/§6
stale vs close decisions) resolved at M1 close via deliver-agent
commit appending "Close decision update (2026-05-17)" addendum to
`docs/diagnostics/option_beta_coverage_matrix.md` per Decision
B-2a. **Constitution-discipline §7.2 revert** at Sprint 35 close —
planning-anticipated §7.2 worked-example re-anchor was *considered
and dropped* (governance teaches principles; current-implementation
limits belong in the optimization backlog); captured in
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md`.
Sprint 36 (conditional INTAKE-locked reroute investigation per M1
§3 row) **deferred** — the conditional trigger ("Sprint 33+34 do
NOT sufficiently close Alice") did NOT fire. **M2 candidate
selection** per Decision C 2026-05-17 cross-validated by two
parallel research-agents: M2 = M2-A ∪ M2-E.option-3 (grounding
discipline + escalation honesty + customer-honesty); M2-C
parallel-track instrumentation; M2-B orchestrator → M3 unless
Salesforce cutover calendar pressure surfaces; M2-D Topic↔UC
binding loosening → deferred to M3+. M1 final classification:
**A-with-Codex-finding-OOSR-classification + deliver-agent-finding-2-fix-in-close**
(first milestone-shared close under the §8 framework; first
instance of this milestone-close classification pattern). M1
objective archived at `docs/milestones/M1_objective.md` (with §12
closure verdict appended); §6.5 closed-milestone index row added
to `docs/action_bank.md`.

---

Earlier sub-sprint:
**Sprint 40 (NEW M2 sub-sprint 4) — Teaching extraction from `system_prompt.txt` + orchestration shell cleanup** closed **A — Clean PASS** 2026-05-18 (dev commit `9130abc`; Codex per-sub-sprint review per `iteration_governance.md` §4.3 trigger #3 returned `decision: pass / blocking_count: 0` in a single round; archive: `docs/sprints/sprint-040-objective.md` + `docs/sprints/sprint-040-handoff.md` + `docs/sprints/sprint-040-codex-review.md`). Third implementation sub-sprint under NEW M2; SMALLEST M2 sub-sprint by scope — content-relocation only. Sprint 40 shipped 5 files (639 insertions / 25 deletions): `system_prompt.txt` EDIT (101 → 80 lines; Sprint 31 + Sprint 33 SLOT description + DISCOVER phase guidance bulk DELETED; envelope-mechanics paragraph + DISCOVER one-line pointer combined ADDED at post-Sprint-40 lines 30-31 per OQ-S40.2; Sprint 23 `already_called` + `request_handover` decision tree PRESERVED); `discover_triage.yaml` EDIT (procedure quoted string extended ~2900 → ~7900 chars with migrated Sprint 31 + Sprint 33 SLOT + DISCOVER refinements; cue/slot split closed per design doc §7.2.3); NEW `SkillTeachingMigrationIntegrationTest.java` (11 Java-deterministic prompt-composition tests); EDIT `PhaseEvaluatorSkillIntegrationTest.java` `DISCOVER_SYSTEM_INSTRUCTION` golden updated (OQ-S40.1 5th-file Sprint 39 precedent reaffirmed); 312-line dev handoff. Java baseline 1094 → 1105. **Sprint 37 freeze fidelity:** decision (f) §7 honored verbatim across §7.1-§7.8. **Codex independent verification** confirmed: §4.1 kernel `approve`; §1.7 PASS; all 33 Sprint 40 §6 + 22 M2 §6 hard fences honored; M1 + Sprint 38 + Sprint 39 functional surfaces preserved. **OQ disposition** (5 OQs resolved): OQ-S40.1 5th-file inclusion AGREE WITH DEV; OQ-S40.2 envelope-mechanics + DISCOVER pointer combined AGREE WITH DEV; OQ-S40.3 inherited test PERSISTS STATUS QUO; OQ-S40.4 Sprint 23 re-wording declined AGREE WITH DEV; OQ-S40.5 design doc §7.8 typo ROUTE TO M2 CLOSE FOLD-BACK. 2 contract-drift items: line-count target 60-75 missed at 80 (CONTENT-EQUIVALENCE PRESERVED); 5th-file Sprint 39 precedent REAFFIRM. **Tier-0 candidate disposition:** No new Tier-0; C2 + C3 QUALIFIED-DEFER continued (Sprint 40 ships content-relocation only — no fresh C2/C3 evidence). **Codex §10 non-blocking observations:** (a) deliver-agent's Sprint 40 review prompt numstat estimates differed from actuals — continuation of `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse (second observed instance); (b) migrated content "formatting-normalized content equivalence" framing more precise than "byte-for-byte modulo whitespace flattening"; (c) Sprint 37 design doc §7.8 typo joins M2 close fold-back queue. **R-item flips:** `D-skill-runtime-framework` partial-landing extended to Sprint 40; Sprint 41 named as LAST continuation surface. C2 + C3 R-items STAYED OPEN. Sprint 40 close-action index row added to `docs/action_bank.md` §6. Full handoff: `docs/sprints/sprint-040-handoff.md`; Codex archive: `docs/sprints/sprint-040-codex-review.md`.

---

Earlier sprint:
Sprint 32 (alternate_candidate_use_cases case family — Sprint 31
OQ4 deferral implementation, single-track semantic-touching sprint,
layer `eval_spec` per §3.2 Q6) closed on **2026-05-16** as
**PASS — A-with-investigation-finding** (first instance of this
pattern on the `eval_spec` layer; Sprint 29 in-flight downgrade
precedent applied). Dev commit `c9edb37` shipped 10+ files: source
brief at `docs/diagnostics/failure-briefs/sprint32-uc-a-uc-c-alternate-uc-readiness.md`
+ 5 visible CaseSpecs at `eval_interactive/case_specs/case_families/sprint32_alternate_uc/`
+ 2 shadow CaseSpecs at `eval_interactive/case_specs_shadow/case_families/sprint32_alternate_uc/`
(per `_ACCESS_BOUNDARY.md:91` convention) + 1 local manifest + 2
manifest appends + dev handoff. Java baseline preserved
(917 / 1-inherited / 0 / 2; zero Sprint 32 Java change). 14-case
smoke rerun showed no production-code-attributable regression.
**Codex sprint-close returned `fix_required / blocking_count: 2`**
at commit range `8d3e73b..c9edb37`. Deliver-agent + human classified
Finding 1 (shadow path scope mismatch) as **`out_of_scope_review`**
— deliver-agent objective `docs/sprint_objective.md` had wrong
shadow path; dev correctly followed `_ACCESS_BOUNDARY.md:91`;
deliver-agent corrected objective post-facto in close cycle.
Finding 2 (target case observed alternates lack UC-C; neighbor #2
ROUTED not AMBIGUOUS — Sprint 30 Option β coverage prediction of
§7.2 worked-example shape empirically falsified) classified as
**in-flight downgrade to investigation finding**, new R-item
`R-option-beta-coverage-gap-uc-a-uc-c-shape` registered (`eval_spec`
/ `prompt_projection`; proposed; consumed by M1 sub-sprint 2 or 3).
Positive findings tucked inside Finding 2: neighbor #1 DID exercise
populated-slot shape (alternates contained UC-FP); both negative
cases observed populated alternates AND LLM stayed in active UC —
validates §1.7 non-enforcement at LLM-behaviour level (Sprint 31
fix-iteration #2 T8 proved at runtime level; Sprint 32 negatives
extend the proof to LLM-behavior level). Full handoff: `docs/sprints/sprint-032-handoff.md`
(13 sections including the §13 findings-classification narrative);
Codex archive: `docs/sprints/sprint-032-codex-review.md`.

---

Earlier earlier sprint:
Sprint 31 (alternate_candidate_use_cases projection slot — Option β
implementation, single-track semantic-touching sprint, layer
`prompt_projection` per `iteration_governance.md` §3.2 Q3, §7 stanza
filled) closed on **2026-05-16** as **PASS — chained close (path A
+ fix-iteration #2)**
(deliver-agent + human applied; full closure rationale in
`docs/sprints/sprint-031-handoff.md` §12). Two commits on
`refactor/remove-the-shackles`: `2c1fd41` (Sprint 31 dev ship) and
`de47635` (§13 fix-iteration append). Sprint 31 ships the runtime
implementation of the Option β design frozen at
`docs/proposals/alternate_uc_signal_data_source_design.md`:
captures the existing `RoutingResult.AMBIGUOUS` candidate list that
`SessionManager.java:185–189` previously **discarded** (now
preserved), persists it on a new
`BotSession.intakeAmbiguousCandidates: String[]` field with the
Flyway V14 migration at
`server/src/main/resources/db/migration/V14__intake_ambiguous_candidates.sql`,
projects per-turn as `alternate_candidate_use_cases` (minus the
active UC) adjacent to the existing `candidate_use_cases` slot at
`ContextProjectionBuilder.java:380–415`, plus a sibling teaching
paragraph in `server/src/main/resources/prompts/system_prompt.txt`
adjacent to the `already_called` paragraph (lines 23–28). Plus two
new regression-test files: 9-test
`server/src/test/java/com/gumtree/csagent/service/runtime/IntakeAmbiguousCandidatesProjectionTest.java`
(mirror of Sprint 20 `AlreadyCalledProjectionTest`) + 1-test
non-enforcement integration test at
`server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopAlternateCandidateUseCasesNonEnforcementIntegrationTest.java`
(mirror of Sprint 20 `AgentRunLoopAlreadyCalledNonEnforcementIntegrationTest`).
All five OQ1–OQ5 pre-picks adhered to (OQ1 = null on ROUTED;
OQ2 = sibling teaching placement; OQ3 = normal fold-back cadence;
OQ4 = case-family authoring deferred to Sprint 31+1; OQ5 = no
sequencing dependency with `R-uc-cdf-get-customer-context-bot-actual-usage`).
Java test bar: 10/10 new Sprint 31 tests PASS; full server suite
912 / 1-inherited / 0 / 2 (zero new regressions; the inherited
`SystemPromptUserRequestedTiebreakerTest` failure persists from
the Sprint 24-era unauthored `system_prompt.txt:60` mod
unchanged). New slot observable on **4 AMBIGUOUS-intake cases**
(`cs_interactive_015 / 040 / 176 / 192`) carrying non-empty
`alternate_candidate_use_cases` arrays in rerun #1's
`per_turn_trace[].projection` at
`eval_interactive/results/20260516-024934/results.json`. §10 smoke
acceptance bar shows a composite/outcome/judge floor regression vs
Sprint 28 reference (`eval_interactive/results/20260514-181257/results.json`);
the §13 fix iteration (three smoke reruns across rerun #1 / #2 / #3
on differing bot instances) disambiguated this as **external LLM
provider drift** (mean elapsed_ms +84% vs Sprint 28 reference with
zero Sprint 31 latency-config / model / retry change). Both
falsifiable internal hypotheses — cold-start race (§7.1(a) per
dev handoff) and system_prompt teaching paragraph (§13.3) — were
REJECTED by the fix iteration. Codex sprint-close review COMPLETED:
**initial verdict `fix_required / blocking_count: 2`** at
`8908775` (Finding 1 smoke regression, Finding 2 T8 too weak);
deliver-agent + human classified Finding 1 as `out_of_scope_review`
(carried by `R-llm-provider-latency-drift-2026-05-16` per
Constitution §1.6) and Finding 2 as `fix_required (targeted P1)`.
**Fix-iteration #2** at commit **`8d3e73b`** strengthened T8 from 1
happy-path scenario to 6 parameterised variants × 5 invariance bars;
dedicated handoff at `docs/sprints/sprint-031-fix-handoff.md`. **Codex
re-review verdict `pass / blocking_count: 0`** at `8908775..8d3e73b`
(Finding 2 closed; Finding 1 confirmed OOSR carry). Full chronological
review archive at `docs/sprints/sprint-031-codex-review.md`. Final
classification: **A-with-fix-iteration-investigation +
A-with-fix-iteration-#2 (chained close; first instance of the pattern)**.
Follow-on R-items in `docs/action_bank.md`:
**`R-llm-provider-latency-drift-2026-05-16`** (`infra` /
observability — characterize the +84% latency widening per §13.7;
deferred behind Sprint 31+1 per 2026-05-16 planning pick) and
**Sprint 31+1 case-family-authoring sprint** (`eval_spec` — picked
as next current sprint per OQ4 pre-pick + 2026-05-16 deliver-agent +
human decision).

---

Earlier sprint:
Sprint 30 (Alternate-UC signal data-source design — investigation-
only docs-only architectural-design sprint, single deliverable: one
new design proposal doc at `docs/proposals/alternate_uc_signal_data_source_design.md`
plus one R-item registration `R-alternate-uc-signal-data-source` at
`docs/action_bank.md`) closed on **2026-05-16**. Classification
**A-with-Codex-skipped** — third instance of the pattern (Sprint 26
first, Sprint 27 second). Codex intentionally skipped per §4.1
docs-only exemption (human-applied 2026-05-16); no
`docs/sprints/sprint-030-codex-review.md` archive exists (intentional).
The §4.1 verdict that would have been returned is `approve
(exemption: docs-only design freeze; no semantic surface touched)`.
The Sprint 30 dev session re-verified all 5 §4 premise items from
the Sprint 30 objective at HEAD `df8b8cd` with no drift, surfaced
the load-bearing Sprint 31 premise (the AMBIGUOUS-branch discard
at `SessionManager.java:185–189`), evaluated 5 candidate options
(α through ε), and recommended **Option β** with explicit
Constitution citation. Sprint 31 inherited the design freeze
verbatim via its `docs/sprint_objective.md` §6 file table and
shipped the runtime change on commit `2c1fd41`. Full handoff:
`docs/sprints/sprint-030-handoff.md` + design freeze:
`docs/proposals/alternate_uc_signal_data_source_design.md`.

---

Earlier sprint:
Sprint 29 (R-prompt-phase-plan-directive-followship (R2) probe
follow-on — two-track semantic-touching sprint with `eval_spec`
Track A CaseSpec authoring + `infra` Track B (c) phase-derivation
verification, in-flight downgrade clause on Track B per
`feedback_corpus_undecidable_premise_check.md`) closed on
**2026-05-15** with the in-flight downgrade clause firing (Track B
Q2 = NO, no `CONFIRM → CLOSE` turn pair observable in Track A's
authored corpus). Track A landed 4 hand-authored CaseSpecs under
`eval_interactive/case_specs/case_families/sprint29_directive_probe/`
exercising D485.2 / D564.7 / D616.2 preconditions; smoke ran at
`eval_interactive/results/20260514-225419/results.json`. D564.7
followed (1/1); D616.2 violated (1/1; `cs29d616_uc_c` escalated
with `user_distress` instead of producing a grounded RESOLVE
answer); D485.2 undecidable on Track A's corpus (user-simulator
`goal_achieved` early-stop at `eval_interactive/eval_interactive/simulator/session_runner.py:224`
pre-empts the bot's CONFIRM phase plan + a `record_outcome` infra
error on `cs29d485_uc_c`). Track B's in-flight downgrade named (but
did not open) `R-per-turn-phase-transition-dump-for-smoke-harness`
as a proposed follow-on R-item. `R-prompt-phase-plan-directive-followship`
at `docs/action_bank.md:450` was updated with the Sprint 29 finding
(D616.2 n=1 violation; (R1) structural-sprint trigger requires
n ≥ 2 confirmed non-fulfillments and Sprint 29 contributes 1) but
**NOT closed**. Sprint 29 ships zero `server/` / `eval_interactive/`
infra code edits; zero `system_prompt.txt` edits; zero edits to
existing Sprint 20 case families. Closure verdict pending Codex
review per Sprint 29 sprint-close convention. Full handoff:
`docs/sprints/sprint-029-handoff.md`.

---

Earlier sprint:
Sprint 28 (Per-case trace dump for smoke harness — single-track,
single-layer `infra` / eval-harness bundle shipping
`R-per-case-trace-dump-for-smoke-harness` per
`docs/action_bank.md:448`) closed on **2026-05-15** with Codex
sprint-close review verdict **`decision: pass / blocking_count: 0`**
on the first pass (no fix iteration required). Classification:
**A — Clean close** (cleanest A in the run since Sprint 24; no
packaging-rollforward needed, no Codex-skip applied).

Option B (writer-side eval-harness enrichment) chosen per Sprint 25
precedent. The planning-turn premise check established (and the dev
session re-verified) that all four R-item-named fields are already
hydrated into `TraceCollector.collect(session_id)`'s `TurnTrace`
records from the bot's `/v1/demo/sessions/{id}/trace` endpoint; the
eval-harness read them but did not serialise them into
`results.json`. The dev edit: a new
`_build_per_turn_trace(trace_data)` static helper in
`eval_interactive/eval_interactive/batch/executor.py` (+62 LOC) plus
one new additive list field `per_turn_trace` emitted on each of the
four `case_result` writer paths (`_build_case_result` success-path +
`_timeout_result` / `_error_result` / `_contract_violation_result`
placeholder paths, each with `[]` for schema uniformity). New
regression test file
`eval_interactive/tests/test_executor_per_turn_trace_enrichment.py`
(7 tests, all pass) mirrors Sprint 25's 8-test
`test_executor_llm_calls_enrichment.py` shape: populated path /
empty trace / missing `phase_plan` in projection / non-dict /
non-list defensive fallbacks / backwards-compat byte-identity /
JSON-serialisability / all three placeholder paths. Smoke rerun on
2026-05-15 (`eval_interactive/results/20260514-181257/results.json`,
14 cases) — 12 cases populate `per_turn_trace` with ≥ 1 entry; 2
cases hit the defensive `[]` branch by design (`cs_interactive_001`
bot-500 path, `cs_interactive_259` CONTRACT_VIOLATION path; both
expected and covered by tests). Mean case-level `elapsed_ms` =
26437.64 ms vs Sprint 25 reference 26139.43 ms vs Sprint 26 close
28846.64 ms — Sprint 28's run sits inside the run-to-run variance
band, no overhead regression.

**All four R-item-named axes shipped**:

- `tool_calls` per bot turn → new `per_turn_trace[].tool_calls`
- `phase_plan` per bot turn → new `per_turn_trace[].phase_plan`
- `projection` per bot turn (including `intake_state`) → new
  `per_turn_trace[].projection`
- `LlmCallEvents` at case level → preserved via Sprint 25's existing
  `case_results[].llm_calls[]` (NOT re-shipped this sprint to avoid
  the opportunistic-field-duplication anti-pattern)

**`R-per-case-trace-dump-for-smoke-harness`** at
`docs/action_bank.md:448` is **CLOSED** by this Sprint 28 close
commit. The disposition note records the closure with file-axis
mapping; §6 closed-action index appends the Sprint 28 row.

**This sprint unblocks Sprint 27's (R2) follow-on probe.**
`R-prompt-phase-plan-directive-followship` at
`docs/action_bank.md:450` had a gating dependency on this R-item
(named three times in Sprint 27 handoff §6, §10, and §11); that
gating is now removed. The directive-followship workstream
(promoted on n=3 in Sprint 19 from cs_259 + manual-probe + cs_011
T2 evidence; probed in Sprint 27 with recommendation (R2) targeted
probe sprint) can now run with per-turn `phase_plan` + per-turn
`projection.intake_state` evidence on authored CaseSpecs. The line
450 R-item disposition is updated to reflect the gating removal but
remains open (awaiting the (R2) probe sprint).

Codex review-pass evidence (`docs/sprints/sprint-028-codex-review.md`):
§4.1 Anti-Hardcode verdict `approve` (exemption: pure infra /
eval-harness writer-side serialization, no semantic surface
touched). All 5 handoff reproducibility recipes re-extracted by
Codex and returned the cited values verbatim. Backwards-compat
verified: `added=['per_turn_trace']`, `removed=[]`; Sprint 25
`llm_calls[]` 13-key shape preserved. Option B-only verified: no
new bot endpoint, no new HTTP client method, no Java persistence,
no Flyway migration, no DB schema change. Opportunistic-field
check passed (only 3 R-item-named per-turn fields shipped;
`LlmCallEvents` not duplicated). Test runs: new Sprint 28 tests
`7 passed`; full Python `306 passed, 3 failed` (3 inherited from
`test_case_spec_overrides.py` × 2 + `test_corpus_lint.py` × 1 per
Sprint 25 §11); full Java `902 / 1 / 0 / 2` (1 inherited
`SystemPromptUserRequestedTiebreakerTest` from the unauthored
`system_prompt.txt` working-tree mod, same as Sprints 24/25/26/27).

Sprint 28 handoff §7 surfaces a **proposed but NOT opened**
follow-on R-item: `R-per-turn-phase-transition-dump-for-smoke-harness`
— would extend `per_turn_trace[]` with `phase_before`, `phase_after`,
`turn_index` so the Sprint 27 (R2) consumer's transition-confirmation
step on D485.2 / D564.7 / D616.2 preconditions can read phase deltas
without re-deriving them. **Named only by the dev, not opened**;
human decides at next-sprint planning whether to (a) open it
standalone, (b) fold it into the Sprint 27 (R2) probe sprint as a
prerequisite, or (c) defer.

Files committed in the single Sprint 28 dev commit `ca55d8e`
("sprint 28: per-case trace dump surfaces into results.json",
2026-05-15):

- `eval_interactive/eval_interactive/batch/executor.py` (modified,
  +62 LOC) — `_build_per_turn_trace` helper + 4 writer-path
  emissions.
- `eval_interactive/tests/test_executor_per_turn_trace_enrichment.py`
  (NEW, 7 tests, all pass).
- `docs/sprints/sprint-028-handoff.md` (NEW, 12-section archive
  including the closure-verdict §12 placeholder authored by the dev
  with explicit-delegation language per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md`).
- `docs/10-handoff.md` (parent-dev-commit modification to set Sprint
  28 as "Current phase" and demote Sprint 27 to "Previous phase";
  this close commit then refreshes the §1 lead to the close
  summary above and demotes Sprint 27 to "Preceding sprint" full
  paragraph).

Files added at close commit (deliver-agent close-out, not part of
the dev's commit):

- `docs/sprints/sprint-028-objective.md` — archive copy of the
  running `docs/sprint_objective.md` carrying the Sprint 28
  objective. Deliver-agent-owned and untracked at session start;
  plain `mv` (no `git mv` HEAD-content risk per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_git_mv_uses_head_content.md`
  because the source was never tracked). Front-matter updated at
  close (`doc_tier: sprint-archive`, `status: archived`,
  `implementation_status: historical`, `review_cadence: ad hoc`);
  the running file is removed by this close commit.
- `docs/sprints/sprint-028-codex-review.md` — archive copy of
  `docs/codex-findings.md` (untracked at top level prior to this
  commit; staged as new file + deletion of the top-level file per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_packaging_codex_findings_supersession.md` —
  delete+add supersession, not a rename).
- `docs/action_bank.md` — line 448 `R-per-case-trace-dump-for-smoke-harness`
  status flipped to `done (Sprint 28)` with disposition note;
  line 450 `R-prompt-phase-plan-directive-followship` disposition
  appended with the gating-removed note; §6 closed action index
  appended Sprint 28 row.
- `compact/sprint-028-dev-prompt.md`,
  `compact/sprint-028-review-prompt.md` — deliver-agent-owned
  planning prompts authored during this sprint; rolled forward in
  this close commit per
  `.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md`.

This sprint is `docs/current/iteration_governance.md` §7
stanza-**REQUIRED** with target layer `infra` per Sprint 28
objective §6 (the archived `docs/sprints/sprint-028-objective.md`
§6 carries the stanza verbatim: target layer `infra`,
"This sprint adds no Tier-0 invariant", "No semantic hardcode
introduced", generalization coverage = 14 target / 0 neighbor
(eval-harness writer is single-surface) / 14 negative (all 14
cases must preserve existing fields byte-identical) / 0 shadow
(deferred — shadow set is the G2 case-family deliverable not yet
existent)). The §4.1 Anti-Hardcode kernel ran externally; Codex
returned `approve` with the exemption note.

The natural follow-on sprint candidate per Sprint 28's R-item
closure + Sprint 27's (R2) recommendation is now the
**Sprint 27 (R2) targeted probe sprint** — author N≥3 CaseSpecs
exercising D485.2 (CLOSE record_outcome-if-not-already-recorded) /
D564.7 (INTAKE intake-complete handover with structured args) /
D616.2 (RESOLVE FAQ premature-escalation prohibition) preconditions
using Sprint 28's per-turn `phase_plan` + `projection.intake_state`
trace evidence. The directive-followship workstream has now chased
this evidence path for 9 sprints (Sprint 18 G1 → Sprint 19
3-instance promotion → Sprint 27 probe + (R2) recommendation →
Sprint 28 trace dump unblocks → Sprint 29 (R2) probe), and letting
the workstream complete is high-value. Alternative Sprint 29 picks
on the action_bank §5.2 backlog include: open
`R-per-turn-phase-transition-dump-for-smoke-harness` (handoff §7
proposal — small `infra` increment that benefits the (R2) consumer,
could be folded into the (R2) sprint as prerequisite OR run
standalone); `R-uc-k-intake-complete-case-id-binding` (Sprint 19
§3.6; cs_066 UC-K state-loss; `skill_state` layer; deterministic
bug fix, not gated on anything; standing UX-leverage reframe
alignment); `R-uc-cdf-get-customer-context-bot-actual-usage`
(Sprint 22-surfaced behavioural question, investigation-only);
`R-handover-orchestrator-write-side` (Sprint 16 design freeze;
`docs/release_gate.md` §1.1 P1-pre-cutover blocker — awaits cutover
trigger). The Sprint 25 §7 open questions (Q2 / Q3 / Q4), the
Sprint 26 §7 conditional `R-pre-post-f2d4cb2-pass-rate-isolation`
opening, the Sprint 27 §11 Q1–Q5 governance questions, and the
Sprint 28 §7 follow-on R-item proposal all continue to require
human direction to open.

Pattern captured in deliver-agent memory: this is the cleanest
first-pass A close in the run since Sprint 24. The Sprint 28
review-pass was reinforced by the writer-side enrichment pattern
established in Sprint 25 (Option B; mirroring exactly), the
explicit-delegation handoff §12 placeholder convention from
Sprint 22 (per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md`),
and the reproducibility-recipe-as-handoff hygiene introduced by
Sprint 25's fix iteration (per
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`).

---

Preceding sprint:
Sprint 27 (PhasePlan directive-shape probe — investigation-only
docs-only probe sprint, single deliverable: a 12-section decision
handoff) closed on 2026-05-15 (commit `8a7703a`) as the probe
sprint triggered by the planning-turn premise check on
`R-prompt-phase-plan-directive-followship`
(`docs/action_bank.md:450`, Sprint 19 close 3-instance promotion).
Premise check found detection-axis fragmentation across the three
cited instances (cs_259 / manual-probe / cs_011 T2): only the
manual-probe UC-A RESOLVE instance is fully event-shape detectable;
cs_259 has a content-shape precondition; cs_011 T2's directive
source is bot-content (not `phase_plan.systemInstruction`). Per
the `docs/sprints/sprint-018-handoff.md` §8.8 conditional-broadening
rule (n=1 evidence insufficient to ship structural action),
Sprint 27 did NOT ship a slot, did NOT restructure `PhasePlan`,
did NOT author probe scenarios — Sprint 27 enumerated the six
`PhaseEvaluator.plan(...)` `.systemInstruction(...)` string
literals (lines 411 / 458 / 485 / 517 / 564 / 616), classified the
21 identified directives under a 4-question rubric (precondition
shape / action shape / source / turn scope), surveyed two existing
reference smoke runs (`eval_interactive/results/20260514-111724/results.json`
Sprint 25 reference + `…/20260514-114628/results.json` Sprint 26
close), and produced an evidence table + recommendation from a
closed set (R1 / R2 / R3). Findings: 4 of 21 directives classify
as `target` (event-shape precondition AND event-shape action AND
`phase_plan` source) — D485.2 (CLOSE record_outcome-if-not-
already-recorded), D517.2 (ESCALATE tautology recap), D564.7
(INTAKE intake-complete handover), D616.2 (RESOLVE FAQ premature-
escalation prohibition); the remaining 17 are `content-shape-defer`.
Across both reference smokes the LLM emits ZERO `request_handover`
and ZERO `record_outcome` tool calls (138 chat calls combined);
`observed_triggers=0` and `observed_non_fulfillments=0` for every
target row (§9.8 valid-finding: insufficient detection capability
+ insufficient case coverage). (R1) requires n≥2 target directives
with observed non-fulfillment; evidence is 0. (R3) requires
structural incompatibility; population IS structurally compatible
(4 of 21 target-shape). **Recommendation: (R2) targeted probe
sprint follow-on** — author N≥3 CaseSpecs exercising D485.2 /
D564.7 / D616.2 preconditions (skipping D517.2 tautology). The
(R2) follow-on was **gated on `R-per-case-trace-dump-for-smoke-
harness` (`docs/action_bank.md:448`) landing first** — Sprint 28
above closes that gating. **`R-prompt-phase-plan-directive-
followship` at `docs/action_bank.md:450` was UPDATED with the
probe finding but NOT closed**; disposition becomes "open, awaiting
(R2) probe sprint with confirmed observed non-fulfillment ≥ 2".
Sprint 27 also names — but does NOT open — 6 follow-on R-items
(handoff §7: `R-discover-weak-candidate-cue-soft-signal`,
`R-discover-classify-or-clarify-projection`,
`R-confirm-sentiment-projection`,
`R-close-phase-completion-checklist-projection`,
`R-intake-instruction-decomposition`,
`R-resolve-faq-terminal-sequence-projection`). Open governance
question surfaced (handoff §11 Q2): does
`phase_plan.systemInstruction` text containing user-message-shape
detection (D411.3 / D411.4) constitute a §1.7 boundary case
(prompt-side soft-matching vs code-side keyword/regex)? Sprint 27
ships ZERO code, config, prompt, eval-spec, judge, CaseSpec,
override, Tier-0 edit. Single deliverable: 12-section
`docs/sprints/sprint-027-handoff.md`. Closure verdict: PASS
(Codex review intentionally skipped per §4.1 exemption clause for
docs-only investigation/probe sprints, human-applied 2026-05-15;
the §4.1 verdict that would have been returned is `approve
(exemption: docs-only investigation/probe sprint)`).
Classification **A-with-Codex-skipped** — second instance of the
pattern (Sprint 26 was the first). Pattern captured in deliver-
agent memory at
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_close_with_codex_skipped_docs_only_outcome.md`
+ the probe-sprint-shape memory at
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_probe_sprint_shape_for_conditional_broadening.md`.
No `docs/sprints/sprint-027-codex-review.md` archive exists
(intentional). Full handoff: `docs/sprints/sprint-027-handoff.md`.

Earlier sprint (Codex skipped, docs-only outcome — first instance):
Sprint 26 (Latency decision sprint — consumes Sprint 25's per-LLM-
call latency instrumentation) closed on 2026-05-15 (commit
`1f4a1db`) as the investigation+decision sprint that walked the
(A) widen deadline budget / (B) revert pre-`f2d4cb2` model /
(C) accept current latency / (D) change retry-backoff / (E) other
option space against the Sprint 25 reference smoke. Decision:
**(C) Accept current latency.** Classification:
**A-with-Codex-skipped (new variant — first instance)** — Codex
review intentionally skipped per `iteration_governance.md` §4.1
exemption clause for docs-only PRs, human-applied 2026-05-15. The
Sprint 26 dev session re-verified all seven Sprint 26 objective
§3 premise checks (handoff §3; all hold), extracted the decision-
relevant per-call data from the Sprint 25 reference smoke (243/243
LLM-call successes / 0 deadline exceedance / max chat latency
11.547s vs 30s budget), walked the three-hypothesis attribution of
the separate 4/14 pass-rate signal (latency REFUTED; model-quality
PLAUSIBLE-but-CONFOUNDED with concurrent prompt / corpus /
override changes; run-to-run variance PARTIAL), and rejected (A) /
(B) / (D) / (E). Surfaced ONE follow-on conditional R-item:
`R-pre-post-f2d4cb2-pass-rate-isolation` recorded at `§5.2` as
`proposed; deferred` per human direction 2026-05-15. Sprint 26
shipped zero code / config / instrumentation / Sprint 24-landed
or Sprint 25-landed code touched / eval-spec / case-family /
prompt / Tier-0 / rubric edit. Full handoff:
`docs/sprints/sprint-026-handoff.md`.

Earlier sprint (Codex fix re-review pass, single-track infra/eval-harness):
Sprint 25 (Per-LLM-call latency instrumentation — single-track
`R-per-llm-call-latency-instrumentation`, `infra` / eval-harness)
closed on 2026-05-14 after one narrow handoff-edit-only fix
iteration on parent handoff's reproducibility hygiene.
Classification: **A — Clean close** (Codex fix re-review `pass /
blocking_count: 0` against fix commit `c8b8c85`). Track A —
Option B (writer-side eval-harness enrichment) chosen: surfaces
existing `LlmCallLogger`-emitted timing into each `case_results[]`
entry on `results.json` via new `llm_calls` field populated from
`GET /v1/demo/sessions/{id}/llm-calls`. Parent commit `1b54b14`;
worked-example pre/post-`f2d4cb2` chat p95 8.4s → 11.7s
(+3.3s widening). Fix iteration was handoff-edit-only (commit
`c8b8c85`): replaced `ARRAY[…]` placeholders with literal `jq`
extractors + executable mean-computation. Sprint 25 ships zero
decision-action; the latency decision belongs to Sprint 26. The
writer-side enrichment pattern Sprint 25 established is the
precedent Sprint 28 above mirrored exactly. Full handoff:
`docs/sprints/sprint-025-handoff.md`. Fix Codex archive:
`docs/sprints/sprint-025-fix-codex-review.md`. Lessons in
`.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

Earlier sprint (Codex pass, no fix iteration required):
Sprint 24 (Slow-LLM Placeholder Coalesce + Coarse Latency Proxy —
two-track, semantic-touching on Track A; investigation-only on
Track B) closed clean on 2026-05-14 with Codex `decision: pass,
blocking_count: 0` on the first review pass. Track A delivered the
deterministic UX repair on the cross-turn slow-LLM placeholder-loop
surface: new `consecutive_deadline_count` `@Column` +
`@Builder.Default = 0` `Integer` field on `BotSession`; `SessionManager`
builder init `.consecutiveDeadlineCount(0)`; `PhaseEvaluator` reset
hook in the outcome-dispatch prologue; split `DEADLINE_EXCEEDED` /
`LLM_UNAVAILABLE` case-block with threshold-gated honest next-step
emission on the second consecutive deadline; single Flyway V13
migration; behaviour-level regression suite. Trigger is the
event-shape count of consecutive `DEADLINE_EXCEEDED` outcomes, not a
regex / keyword / if-else on user content; runtime owns timeout
fallback emission deterministically (§1.4). Track B documented the
honest coarse-proxy latency baseline and proposed
`R-per-llm-call-latency-instrumentation` (Sprint 25 implemented).
Single dev commit `e21b1b6`. Full handoff:
`docs/sprints/sprint-024-handoff.md`.

Earlier sprint (Codex fix re-review pass, PASS branch):
Sprint 23 (repeated FAQ calls + LLM stall root-cause investigation —
two-track investigation+bundle, semantic-touching) closed on
2026-05-14 after one strict-evidence-gate fix iteration. Parent
dev commit `39cb1b9` landed the `already_called` teaching paragraph
in `server/src/main/resources/prompts/system_prompt.txt`; fix
commit `19ce2ae` ran a real-LLM cs_040 target rerun confirming the
duplicate-`search_knowledge` shape reversed. Track B
investigation-only — proximate cause confirmed; narrow UX-repair
shape proposed as `R-slow-llm-placeholder-coalesce-honest-next-step`
(Sprint 24 landed). Mocked-LLM hard fence honored. Full handoff:
`docs/sprints/sprint-023-handoff.md`.

Earlier sprint (docs-only governance, Codex pass):
Sprint 22 (phase 2 line 358 reconciliation + R-item closure) closed
on 2026-05-14 as a narrow docs-only scope-correction sprint:
reconciled the cross-UC `get_customer_context` policy misread
Sprint 21 carried forward (phase 2 §2.10.1 line 1098 already
permits the tool for UC-C / UC-D / UC-F), closed
`R-generator-get-customer-context-policy-mismatch` and the
routed-to `R-phase2-uc-cdf-customer-context-policy-widen` as
premise-invalidated, and opened the residual behavioural R-item
`R-uc-cdf-get-customer-context-bot-actual-usage`. No runtime,
prompt, CaseSpec, override, judge, FAQ corpus, or case-family
change. Full handoff: `docs/sprints/sprint-022-handoff.md`.

Earlier sprint (single-track eval_spec, Codex A-with-evidence-gap-acknowledgment close):
Sprint 21 (Wave A5/A6 L3 Review Batch — per-case + 1 systematic)
closed on 2026-05-14 as a single-track semantic-touching sprint on
the eval_spec surface. Three approved overrides written to
`eval_interactive/case_spec_overrides.yaml` (cs_001
escalation_trigger flip + bot_handling_pattern rewrite; cs_192
secondary_ucs dedupe; cs_095 UC-A → UC-D classification flip,
supersedes Wave A2.1 legacy entry); two rejected (cs_176
reclassified eval_spec → `semantic_planner`; systematic
`R-generator-get-customer-context-policy-mismatch` reclassified
eval_spec → `product_policy` — Sprint 22 later reframed as
premise-invalid); two deferred (cs_038, cs_040) on missing
override-schema scoring extension. Codex fix re-review returned
`fix_required, blocking_count: 3` on typographical-fidelity grounds
(5-character markdown/punctuation drops on quote blocks); the
human accepted as close-eligible (A-with-evidence-gap-acknowledgment
classification — distinct from Sprint 20's A-with-packaging-note
and from B). No remediation against runtime, prompt, CaseSpec
content, judge, or governance surfaces. Full handoff:
`docs/sprints/sprint-021-handoff.md`.

Earlier sprint (G2 case-family + already_called soft signal, Codex A-with-packaging-note close):
Sprint 20 (G2 Interactive Case Family + Shadow Split + Already-Called
Soft Signal — parallel A + B) closed on 2026-05-13 after a narrow
fix iteration. Track A delivered 10 case families × (≥1 target +
≥2 neighbor + ≥2 negative + ≥2 shadow) = 70 CaseSpec-shaped
entries under `eval_interactive/case_specs/case_families/` and
`eval_interactive/case_specs_shadow/` plus the v0 shadow-split
mechanism (directory boundary + custom-path-only loading +
documented self-restraint). Track B delivered the `already_called:
[{tool, arguments_hash, at_step}]` projection slot in
`ContextProjectionBuilder.build(...)` (observability-only, runtime
non-enforcement). Fix iteration closed Codex's two original
findings (runtime non-enforcement test at `AgentRunLoopImpl.run`
granularity + `_ACCESS_BOUNDARY.md` reconciliation); fix
re-review classified as A-with-packaging-note (substantive
findings closed cleanly per Codex's own evidence; the single
blocker was deliver-agent-owned files bundled into the dev's fix
commit). Full handoff: `docs/sprints/sprint-020-handoff.md`.

Earlier sprint (Codex pass):
Sprint 19 (Smoke Regression Investigation + Orchestrator Tool-Call
De-Dup — parallel A + B) walked `docs/current/iteration_governance.md`
§3.2 per case for the two R-items the Sprint 18 G1 backlog named
as blocking G2: `R-smoke-regression-investigation` (Track A) and
`R-runtime-orchestrator-tool-call-deduplication` (Track B). Outcome
was proposal-only on both tracks: Track A found five of six regressed
cases share a slow-LLM placeholder shape (layer `infra` per §3.2
Q1; deferred remediation as `R-slow-llm-placeholder-coalesce`),
cs_011 lands at `semantic_planner` (failure to escalate on explicit
handover request), cs_066 at `skill_state` (UC-K case_id-binding
loss). Track B re-classified the orchestrator-de-dup R-item from
`infra` to `semantic_planner` and proposed two follow-on items:
`R-prompt-projection-already-called-soft-signal` (read-side soft
signal — delivered by Sprint 20 Track B) and
`R-idempotent-read-tool-short-circuit` (conditional). Full handoff:
`docs/sprints/sprint-019-handoff.md`.

Earlier sprint (no Codex run):
Sprint 18 (Human-led Failure Portfolio — G1) filed 10 representative
real failures as Failure Briefs under
`docs/diagnostics/failure-briefs/` per the Sprint 17 §2 template and
recorded 18 G1-surfaced R-items + 2 open observations under
`docs/action_bank.md` §5.2. No runtime, prompt, FAQ corpus, CaseSpec,
judge, eval harness, test, or script change. G1 declared the §7
stanza **exempt** under the docs-only governance exemption. Full
handoff: `docs/sprints/sprint-018-handoff.md`. Archive:
`docs/sprints/sprint-018-g1-failure-portfolio-objective.md`.

Earlier sprint (Codex pass):
Sprint 17 (Iteration Governance Lite — G0) landed the docs-only
governance bundle (`docs/current/iteration_governance.md` 7 sections
+ `AGENTS.md` constitution chain + `docs/action_bank.md` §5.1
governance-track backlog); Codex `decision: pass, blocking_count: 0`;
archive under `docs/sprints/sprint-017-*`.

Earlier accepted sprint:
Sprint 16 (Handover Exactly-Once Contract and Repro) landed as a
docs + characterization-test sprint; no runtime behaviour change.
Defined the handover exactly-once contract in
`docs/proposals/handover_orchestrator_design.md`, added
`Sprint16HandoverDualPathReproTest`, and opened the
`docs/release_gate.md` §1.1 blocking rule + the **Single Handover
Orchestrator** action item.


## 2. Sprint 14 goal

Sprint 14 upgrades FAQ / KB grounding trustworthiness by making the
knowledge source chain, published safety, canonical URL availability,
and citation observability explicit — without introducing a broad hard
runtime citation gate, a new skill runtime framework, broad routing
rewrites, or additional mechanical escalation.

Scope is exactly the three Sprint 14 actions L0 / L1 / L2.

## 3. Sprint 14 implementation

### L0 — KB canonical URL / Help URL / published safety audit + fix

Code changes:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
  — added `findNearestByEmbeddingPublishedOnly` and
  `findNearestByEmbeddingWithUcTagsPublishedOnly` queries that join
  `kb_articles` with `is_published = true`. Existing legacy methods
  preserved for backward compatibility.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  — routes through the new published-only ANN methods so unpublished
  candidates never enter the rerank pipeline. Added defense-in-depth
  hit-projection filter that drops any post-fetch unpublished article
  even if the ANN returned it via a stale cache. Stamps
  `canonical_url_missing` per hit.
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
  — added `canonicalUrlMissing` boolean (Jackson `canonical_url_missing`).
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
  — projects `hits[*].canonical_url_missing` into the agent-visible
  response so missing-URL data-quality gaps are observable.
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
  — refuses unpublished articles with deterministic
  `article_unpublished_safe_refuse` reject reason
  (`UNPUBLISHED_REJECT_REASON` constant). Exposes `canonical_url`
  alongside legacy `source_url`. Stamps `canonical_url_missing` and
  `safe_to_show` (= published AND non-blank body).
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
  — extracted `buildKbArticleFromJson(...)` package-private helper so
  the FAQ → KbArticle field-preservation contract (including
  `source_url` and `is_published`) is unit-testable.
- `scripts/build_knowledge_base.py` — additive: `mapping_summary_report.md`
  now carries a "URL & Published-Safety Coverage" block enumerating
  total / published / unpublished / with-canonical-URL / missing-URL /
  unsafe-to-show counts. JSON content unchanged.

QA report: `qa-reports/faq-kb-lineage-and-url-audit.md`.

Tests added (5 focused tests of the form Sprint 14 §L0 prescribed):

- `Sprint14KnowledgeIngestionCanonicalUrlTest`
  (4 cases — preservation of `Help_Site_URL__c` → `source_url`, respects
  explicit `published_status=false`, missing URL surfaces null,
  CSV-mapping fallback for `uc_tags`).
- `Sprint14KnowledgeSearchPublishedFilterTest`
  (4 cases — UC-scoped path routes through published-only ANN, no-tag
  path same, post-rerank projection drops still-unpublished article,
  missing-URL hit stamps `canonical_url_missing=true`).
- `Sprint14ResolveArticleSafetyTest`
  (4 cases — refuses unpublished with canonical reject reason,
  exposes `canonical_url` mirroring `search_knowledge`, missing URL
  classified as observable DQ gap, blank body marks `safe_to_show=false`).

### L1 — Separate retrieved / resolved / cited source evidence

Code:

- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java`
  (new) — passive citation extractor. Detects Salesforce KA `source_id`
  pattern, canonical URL pattern, and (conservatively) title fuzzy
  match against retrieved/resolved candidates. Stateless;
  side-effect-free.
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java`
  (new) — record `(retrievedSourceIds, resolvedSourceIds,
  citedSourceIds, citedCanonicalUrls, retrievedCanonicalUrls)` with
  `fromToolEvents(toolEvents, botResponse)` constructor walking
  `search_knowledge` (retrieved) and `resolve_article` (resolved) tool
  events. §L0 contract: a refused unpublished resolve is NOT counted
  as resolved — the article remains in the
  `retrievedButUnresolvedSourceIds` set.
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  — added 4 new `@Transient` slots: `retrievedSourceIds`,
  `resolvedSourceIds`, `citedSourceIds`, `citedCanonicalUrls`. Schema
  unchanged; `bot_turns.source_ids` column preserved verbatim.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` now stamps the new transient slots via the
  new `stampFaqGroundingObservability(...)` helper. The existing
  `bot_turns.source_ids` write path is untouched. Stamping is wrapped
  in try/catch so any unexpected pattern in tool events / bot text
  cannot break record-turn persistence.

Tests:

- `Sprint14SourceEvidenceLineageTest` (7 cases):
  retrieved-only does not imply cited, resolved evidence tracked
  separately, source_id mention detected, URL mention detected and
  mapped back to candidate, third-party URL recorded as free-form
  citation, missing citation observable but non-blocking, refused
  unpublished resolve excluded from resolved set.

### L2 — FAQ grounding contract + soft diagnostics

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative) — defines the
  output class taxonomy, the §L1 evidence lineage construction, the
  §L2 diagnostic state transition table, and the non-blocking
  guarantee. Records future narrow Sprint 16 §S1 hardening candidates
  surfaced by the §L0 audit (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`).

Code:

- `FaqOutputClass` enum — six classes: `factual_answer`,
  `clarification`, `empathy_ack`, `handover`, `tool_status`,
  `intake_collection`. Only `factual_answer` requires grounding.
- `FaqOutputClassifier` — heuristic classifier with
  `ClassifierContext(intakeUseCase, handoverDispatched)` so the
  runtime's `handoverDispatched` flag overrides any wording-based
  guess and intake UCs prefer `intake_collection` over
  `clarification` on a question shape.
- `FaqGroundingDiagnostics` — record `(outputClass, faqGroundingState,
  citationPresent, citationMatch, citationDrift, resolvedButUncited,
  retrievedButUnresolved)`. State table: `factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`.
- `BotSession` — added 7 transient slots: `faqOutputClass`,
  `faqGroundingState`, `citationPresent`, `citationMatch`,
  `citationDrift`, `resolvedButUncited`, `retrievedButUnresolved`.
- `ControlKernel.stampFaqGroundingObservability(...)` — single call
  from `recordRunResult` populates §L1 + §L2 slots; observability-only
  with no rejection / rewrite / loop.

Tests:

- `Sprint14FaqGroundingDiagnosticsTest` (14 cases):
  taxonomy exemption check (only factual requires grounding),
  classifier coverage for each shape (clarification / empathy /
  handover-flag override / tool_status / intake / factual default), and
  the full diagnostic state table (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `citation_drift`, all five non-factual classes skip grounding,
  missing citation is observable not blocking).

### Audit findings (factual-answer bypass — Sprint 16 candidate)

Per Sprint 14 §L2, if the audit found a current factual-answer bypass
of search/resolve, document it as a future narrow Sprint 16 candidate.
The audit recorded **R-faq-grounded-resolve-bypass** in
`docs/current/faq_grounding_contract.md` §6: the existing Sprint 6 §G2 guard
refuses the `request_handover(faq_miss_threshold_exceeded)` shape, but
does NOT refuse a FINAL_ANSWER shape that paraphrases an unresolved
hit. This is observable today as `retrieved_but_unresolved=true` on a
factual-answer turn. Sprint 14 explicitly does NOT close it — the
guidance is for a future narrow Sprint 16 §S1 hardening if real-traffic
evidence motivates it.

## 4. Files changed (Sprint 14)

Production code:

- `server/src/main/java/com/gumtree/csagent/repository/KbChunkRepository.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeIngestionRunner.java`
- `server/src/main/java/com/gumtree/csagent/service/knowledge/CitationExtractor.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/SourceEvidenceLineage.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClass.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqOutputClassifier.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/FaqGroundingDiagnostics.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (added §L1/§L2 stamping helper; preserved all existing logic)
- `server/src/main/java/com/gumtree/csagent/service/tools/SearchKnowledgeTool.java`
- `server/src/main/java/com/gumtree/csagent/service/tools/ResolveArticleTool.java`
- `server/src/main/java/com/gumtree/csagent/model/KnowledgeHit.java`
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  (added 11 `@Transient` slots; no schema change)

Tests (new):

- `Sprint14KnowledgeIngestionCanonicalUrlTest` (4 cases)
- `Sprint14KnowledgeSearchPublishedFilterTest` (4 cases)
- `Sprint14ResolveArticleSafetyTest` (4 cases)
- `Sprint14SourceEvidenceLineageTest` (7 cases)
- `Sprint14FaqGroundingDiagnosticsTest` (14 cases)

Docs:

- `docs/current/faq_grounding_contract.md` (new, normative)
- `qa-reports/faq-kb-lineage-and-url-audit.md` (new — §L0 audit + repair)
- `docs/10-handoff.md` (this file)
- `docs/action_bank.md` (Sprint 14 row added)
- `docs/sprint_objective.md` retained — already contains the Sprint 14
  objective.

Scripts:

- `scripts/build_knowledge_base.py` — additive URL & Published-Safety
  Coverage block in the auto-generated mapping report.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, or system prompt was
touched.

No DB schema migration. The `bot_turns.source_ids` column is
preserved verbatim; new lineage / diagnostics fields are
session-transient observability material consumed via projection /
trace evidence.

## 5. Tests run

- `mvn -pl server test -Dtest='Sprint14*'`
  → **33 / 0 / 0 / 0** (Sprint 14 §L0/§L1/§L2 focused tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint 14 regression guards).
- `mvn -pl server test`
  → **854 / 0 / 0 / 0** at the Sprint 14 milestone (was 821
  pre-Sprint-14; +33 new Sprint 14 §L0/§L1/§L2 tests). Post Sprint
  14.1 closure the suite is **859 / 0 / 0 / 0** (+5 trace-persistence
  tests; see §15).
- `pytest eval_interactive/tests/`
  → **294 / 0** (full Python eval test suite).
- Smoke runs were NOT executed for Sprint 14. Sprint 14 introduces no
  FAQ corpus content change, no judge change, no CaseSpec change, no
  prompt change, no escalation enum change, no routing taxonomy
  change, and no eval-output schema change. The current canonical
  baseline (`docs/current_eval_baseline.md`) is preserved
  (post-Sprint-8 r1 / r2 runs).

## 6. KB URL / published audit result

| Metric | Value |
|--------|-------|
| Total articles | 218 |
| Articles published (`published_status=true`) | 218 |
| Articles unpublished (`published_status=false`) | 0 |
| Articles with `source_url` (canonical URL) | 180 |
| Articles missing `source_url` (canonical-URL DQ gap) | 38 |
| Articles unsafe to show (unpublished OR empty body) | 0 |

The 38 articles missing a canonical URL come from rows the Salesforce
export shipped without `Help_Site_URL__c`. They remain searchable but
the §L0 fix surfaces the gap as `canonical_url_missing=true` on every
search-hit and resolve-article response so a reviewer can classify the
case as a corpus-side curation task rather than a runtime fix. The
broader curation work is out of Sprint 14 scope and queued for the Eval
Governance / corpus-audit owner alongside `G-FAQ-corpus-answerability`.

Per `qa-reports/faq-kb-lineage-and-url-audit.md` §3, nine concrete §L0
fixes landed this sprint (published-safety filter at SQL layer + at hit
projection, refusal of unpublished `resolve_article`, exposure of
`canonical_url` alongside `source_url`, observable
`canonical_url_missing` flag on both tools, and the `safe_to_show`
conjunctive predicate on `resolve_article`).

## 7. Retrieved / resolved / cited evidence contract

Sprint 14 §L1 splits the historically-overloaded `sourceIds` concept
into four observably-distinct dimensions, all populated each turn and
durably persisted on `bot_turns.projected_context.faq_grounding`
(Sprint 14.1 closure) and mirrored onto in-memory `BotSession`
transient slots:

- `retrievedSourceIds` — IDs from successful `search_knowledge` events
  (post §L0 published-safety filter). De-duplicated; insertion order
  preserved.
- `resolvedSourceIds` — IDs that successfully passed through
  `resolve_article`. A refused unpublished resolve is NOT counted.
- `citedSourceIds` — IDs detected in the customer-visible reply by
  the passive `CitationExtractor` (source_id mention, URL mention
  mapped back to candidate, conservative title match).
- `citedCanonicalUrls` — URLs in the reply that did NOT correspond to
  any candidate `canonical_url`. Typically third-party (e.g. gov.uk).

The `bot_turns.source_ids` column is preserved verbatim as the
backward-compatible aggregate. Sprint 14 §L1 explicitly does NOT use
the diff between the four dimensions to gate / rewrite / loop the bot
response. The output is observability material — see
`docs/current/faq_grounding_contract.md` §3 for the canonical contract.

## 8. Soft diagnostic fields

Sprint 14 §L2 surfaces seven additional fields per turn under
`bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure;
`docs/current/faq_grounding_contract.md` §4) and mirrors the same values onto
`BotSession` transient slots for in-process readers:

- `faqOutputClass` — taxonomy token (`factual_answer`, `clarification`,
  `empathy_ack`, `handover`, `tool_status`, `intake_collection`).
- `faqGroundingState` — coarse state (`factual_grounded`,
  `factual_uncited`, `factual_unresolved`, `factual_unretrieved`,
  `non_factual`, `unknown`).
- `citationPresent`, `citationMatch`, `citationDrift` — citation
  observability triplet.
- `resolvedButUncited`, `retrievedButUnresolved` — evidence-diff
  observability pair.

All seven are observability-only. Sprint 14 §L2 does NOT block, rewrite,
re-loop, or escalate based on any of them. The §G2 FAQ-grounded-resolve
guard from Sprint 6 remains the only enforced runtime check on this
surface.

## 9. Regression guard outcomes

All Sprint 14 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Existing FAQ S1 guard tests remain green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout closure intact
  (`test_agent_client_session_create_timeout.py`).
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green (`Cs176ExplicitHumanHelpHandoverIntegrationTest`).
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 / 12 /
  13 hard invariants remain green
  (named regression suite: 269 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks` schemas
  unchanged.

New Sprint 14 §L0 / §L1 / §L2 guards (33 deterministic JUnit tests):

- §L0 — `Sprint14KnowledgeIngestionCanonicalUrlTest`,
  `Sprint14KnowledgeSearchPublishedFilterTest`,
  `Sprint14ResolveArticleSafetyTest`.
- §L1 — `Sprint14SourceEvidenceLineageTest`.
- §L2 — `Sprint14FaqGroundingDiagnosticsTest`.

## 10. Remaining risks

**No new P0 / P1 blockers opened by Sprint 14.** The change is additive
observability + a narrow §L0 published-safety / canonical-URL fix; no
runtime main-flow architecture file was modified.

Sprint 14 §L0 audit surfaced four narrow follow-ups (recorded in
`docs/current/faq_grounding_contract.md` §6 — all deferred):

- **R-faq-grounded-resolve-bypass** — factual answers paraphrasing
  retrieved-but-unresolved hits. Observable today as
  `retrieved_but_unresolved=true` on a factual-answer turn. Future
  narrow Sprint 16 §S1 hardening candidate IF real-traffic shows
  reproducible cases.
- **R-cited-but-unresolved** — `citation_drift=true` with cited
  source_id never retrieved/resolved. Hallucination signal candidate;
  Eval Governance owns until reproduced.
- **R-resolved-but-uncited-rate** — corpus-level rate of uncited
  factual answers. Eval Governance.
- **R-canonical-url-missing-rate** — 38 articles missing
  `Help_Site_URL__c`. Corpus curation, not runtime.

Residuals carried forward from Sprint 13 §8 (unchanged):

- `R-cs015-description-keyword`, `R-cs176-UC-I-drift`,
  `R-S3-no-prior-search-guard`, `R-S5-Tier2-runtime-guard`,
  `R-stall-detector-calibration`, `R-L3-relevance-tone`,
  `R-FAQ-corpus-answerability`, `R-advert-link-product-decision`,
  `R-rerank-fallback-diagnostics`, `R-task-type-token-naming`,
  `R-record-outcome-loop`, `R-clean-baseline-promote`,
  `R-full-issue-ledger`, `R-skill-runtime-framework`,
  `R-prompt-risk-signal-handling`.

The current canonical eval baseline remains the post-Sprint-8 r1 / r2
runs documented in `docs/current_eval_baseline.md`. Sprint 14
explicitly does NOT promote a new canonical eval baseline.

## 11. Recommendation for Sprint 15 or Sprint 16

Recommended next phase:

**Eval Governance docs sprint** (or equivalent governance-only work)
remains the primary recommendation. Sprint 14 + 14.1 closed the FAQ /
KB evidence lineage and safety workstream (including the persistence
gap Codex flagged) that Sprint 13 §8 / §9 had open; the largest
residual category is still `judge_volatility` / `faq_corpus_gap` /
`product_policy_gap`, all governance-owned.

(The Sprint 14.1 Codex review surfaced **Script / Policy Config
Governance Sprint** as an alternative recommended next phase. Either
phase is reasonable; choose by prioritisation, not by Sprint 14 / 14.1
state.)

Alternative phases ranked:

1. **Eval Governance** — primary recommendation (above).
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic evidence
   demonstrates `R-faq-grounded-resolve-bypass` reproducibly affects
   answer correctness on a high-traffic UC. The Sprint 14 §L1 / §L2
   diagnostics surface (`retrieved_but_unresolved=true` on a
   `factual_answer` turn) is the trigger to look for. Until then, the
   §G2 prompt-side nudge already handles the dominant case.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.
5. **Release Candidate Hardening** — premature; depends on a
   clean canonical baseline that has not yet been promoted.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 14 explicitly does NOT promote a new
canonical eval baseline.

## 12. Was Sprint 14 objective met?

Sprint 14 alone was **not** met under its initial Codex review: the
review correctly identified that the §L1 / §L2 diagnostics were stamped
only onto `@Transient` `BotSession` fields after `BotTurn.save(...)`,
so a normal save / reload trace could not observe them. Sprint 14.1
(§15 below) closed that single blocking gap. The Sprint 14.1 closure
review returned `decision: pass, blocking_count: 0`. Sprint 14 + 14.1
together are accepted:

- L0 KB canonical URL / Help URL / published safety audit + fix landed.
  Source chain traced (CSV → build script → JSON → DB → service →
  tools); `Help_Site_URL__c` confirmed preserved; published-safety
  filter added at SQL + projection layer; `resolve_article` refuses
  unpublished with deterministic reject reason; missing canonical URL
  classified as observable DQ gap; QA report
  `qa-reports/faq-kb-lineage-and-url-audit.md` shipped; 12 focused
  tests.
- L1 separate retrieved / resolved / cited source evidence landed.
  `SourceEvidenceLineage` value object splits the historically-
  overloaded `sourceIds` concept; `CitationExtractor` provides passive
  citation extraction (source_id pattern, URL pattern, conservative
  title match); existing `bot_turns.source_ids` write path preserved
  for back-compat; 7 focused tests confirm retrieved-only does not
  imply cited, resolved tracked separately, citation extractor
  patterns work, missing citation observable not blocking.
- L2 FAQ grounding contract + soft diagnostics landed.
  `docs/current/faq_grounding_contract.md` defines the output class taxonomy,
  the §L1 evidence lineage construction, the §L2 diagnostic state
  table, and the non-blocking guarantee. `FaqOutputClass`,
  `FaqOutputClassifier`, `FaqGroundingDiagnostics` services compute
  the seven soft diagnostic fields per turn. 14 focused tests.
- Diagnostics are durably observable on
  `bot_turns.projected_context.faq_grounding` (Sprint 14.1 closure);
  the in-memory `BotSession` transient slots are mirrored from the
  same computed values for in-process readers / tests. No DB schema
  migration; no broad runtime / prompt / routing scope; no hard
  citation gate; existing §G2 guard preserved verbatim.
- Full server suite 859 / 0 (post Sprint 14.1; was 854 pre-closure);
  named Sprint 14 regression suite 269 / 0; full Python eval 294 / 0;
  `L1:escalation_reason_consistency = 0`;
  `CONTRACT_VIOLATION:active_use_case = 0`; cs014 / cs066 / cs095 /
  cs002 / cs029 / cs176 regressions all green.

Out-of-scope items (broad hard citation gate, new skill runtime
framework, broad S1 rewrite, broad routing taxonomy rewrite,
escalation enum changes, CaseSpec churn, FAQ corpus content rewrite,
judge calibration, eval expansion, broad prompt rewrite, mechanical
risk-keyword escalation) were NOT touched.

## 13. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/diagnostics/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

Sprint 13 archives are at `docs/sprints/sprint-013-*`;
Sprint 14 will archive to `docs/sprints/sprint-014-*` on closure.

## 14. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- advert-link generator / direct listing URL tool
- full Issue Ledger / `issues[]` / per-issue budgets / all-UC task
  taxonomy / full skill runtime framework / handover payload rewrite
- new escalation reason enum value
- runtime sprint unless a new P0 / P1 runtime blocker is found
- broad system prompt rewrite — narrow risk-signal-handling proposal
  in `docs/runtime_freeze_and_risk_policy.md` §6.2 only on Eval
  Governance trigger
- broad FAQ corpus rewrite — narrow corpus curation for the 38
  `Help_Site_URL__c` gap rows is a queued Eval Governance / corpus
  audit task
- hard runtime citation gate — Sprint 14 §L2 explicitly carved this
  out; only consider after real-traffic evidence escalates the
  `citation_drift` / `retrieved_but_unresolved` signals

## 15. Sprint 14.1 closure (FAQ grounding observability persistence)

Status: **closed (Codex pass)**. The Sprint 14.1 closure review
returned `decision: pass, blocking_count: 0`, accepting Sprint 14 +
14.1 as a unit.

The initial Sprint 14 Codex review had returned `decision:
fix_required, blocking_count: 1` against the Sprint 14 diff. The
single blocker was:

> Sprint 14 L1/L2 lineage + grounding diagnostics are computed but not
> durably observable. They are stamped onto `@Transient` `BotSession`
> fields after `BotTurn` is saved, so a normal save/reload trace cannot
> see `retrieved_source_ids`, `resolved_source_ids`, `cited_source_ids`,
> `faq_grounding_state`, `citation_present`, `citation_match`,
> `citation_drift`, `resolved_but_uncited`, `retrieved_but_unresolved`.

### Closure summary

- **Blocker fixed:** the §L1 `SourceEvidenceLineage`, §L2
  `FaqOutputClass`, and §L2 `FaqGroundingDiagnostics` are now computed
  in `ControlKernel.recordRunResult(...)` BEFORE
  `turnRepository.save(turn)`, and the snake_case payload is merged
  into the existing `bot_turns.projected_context` JSONB column under a
  new top-level `faq_grounding` key.
- **Trace surface used:**
  `bot_turns.projected_context.faq_grounding` (JSONB; no migration —
  the column already exists and is opaque JSON).
- **Fields persisted under `faq_grounding`:**
  - `retrieved_source_ids` (array of source_id strings),
  - `resolved_source_ids` (array of source_id strings),
  - `cited_source_ids` (array of source_id strings),
  - `cited_canonical_urls` (array of free-form URLs that did NOT
    correspond to any candidate `canonical_url`),
  - `output_class` (one of `factual_answer`, `clarification`,
    `empathy_ack`, `handover`, `tool_status`, `intake_collection`, or
    `null` when classification was skipped),
  - `faq_grounding_state` (`factual_grounded` / `factual_uncited` /
    `factual_unresolved` / `factual_unretrieved` / `non_factual` /
    `unknown`),
  - `citation_present`, `citation_match`, `citation_drift`,
  - `resolved_but_uncited`, `retrieved_but_unresolved`.
- **Backwards compatibility:** the existing
  `bot_turns.source_ids` `text[]` column is preserved verbatim; its
  write path in `recordRunResult` is unchanged. The `BotSession`
  `@Transient` slots are still populated (mirroring the durable
  values) so any in-memory reader / projection / test that already
  consumes them keeps working.
- **Out of scope (carved out, per Sprint 14.1 task):**
  - no DB migration (uses the existing JSONB column);
  - no hard citation gate (no rejection / rewrite / re-loop on
    missing citation);
  - no new skill runtime framework;
  - no broad S1 hardening;
  - no FAQ corpus / CaseSpec / judge / prompt / routing /
    escalation-enum / risk-policy edit.

### Files changed (Sprint 14.1)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  — `recordRunResult(...)` reordered to compute lineage / output class
  / diagnostics BEFORE saving the turn; new
  `mergeFaqGroundingIntoProjection(...)`,
  `buildFaqGroundingPayload(...)`, and
  `stampFaqGroundingObservabilityFromComputed(...)` helpers replace
  the old post-save `stampFaqGroundingObservability(...)` call site.

Tests added:

- `server/src/test/java/com/gumtree/csagent/integration/Sprint141FaqGroundingTracePersistenceTest.java`
  (5 cases — retrieved-only, resolved-only, cited-by-source-id,
  cited-by-canonical-url, missing-citation-non-blocking).

Docs:

- `docs/10-handoff.md` (this section + corrections to §1, §7, §8,
  §12 — Sprint 14 was NOT met before Codex review).
- `docs/current/faq_grounding_contract.md` §5 — wiring narrative updated to
  name the durable trace surface
  (`bot_turns.projected_context.faq_grounding`).
- `docs/action_bank.md` — Sprint 14 status reflects the §14.1 closure.

### Tests run

- `mvn -pl server test -Dtest='Sprint14*,Sprint141*'`
  → **38 / 0 / 0 / 0** (33 Sprint 14 + 5 Sprint 14.1 closure tests).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test'`
  → **269 / 0 / 0 / 0** (named Sprint regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions).
- `mvn -pl server test`
  → **859 / 0 / 0 / 0** (was 854; +5 new Sprint 14.1 closure tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 14.1 changes
  no eval-output schema and no Python parsing path.
- Smoke runs not required — Sprint 14.1 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema.

### What Sprint 14.1 explicitly does NOT do

- No DB migration. The trace surface is the existing `jsonb`
  `bot_turns.projected_context` column — `faq_grounding` rides as an
  additive top-level key. Old rows simply won't have the key.
- No hard citation gate. Missing citation remains observable
  (`citation_present=false`, optionally `resolved_but_uncited=true` /
  `retrieved_but_unresolved=true`) but is never used to reject,
  rewrite, or re-loop the bot reply.
- No broad S1 hardening. The Sprint 16 §S1 candidates documented in
  `docs/current/faq_grounding_contract.md` §6 (`R-faq-grounded-resolve-bypass`,
  `R-cited-but-unresolved`, `R-resolved-but-uncited-rate`,
  `R-canonical-url-missing-rate`) remain deferred.
- No new skill runtime framework, no new escalation reason value, no
  CaseSpec edits, no FAQ corpus content edit, no judge / prompt /
  routing / risk-policy change.

## 16. Sprint 15 — Script / Policy Config Governance

Status: **accepted / ready to archive**. The Sprint 15 Codex review
returned `decision: pass, blocking_count: 0`, accepting Sprint 15
as inside the config-governance scope (no prompt, routing, judge,
corpus, CaseSpec, hard citation gate, hot reload, dashboard, or
broad runtime work). Sprint 15 is the latest accepted sprint and
introduced no runtime semantic change. Diff stays narrow: three
actions only.

### 16.1 Implemented actions

#### M0 — Externalize DriftDetector / risk keywords to YAML

What landed:

- New config: `server/src/main/resources/config/risk-keywords.yaml`
  (`version: 1`). Carries the exact same five hard-shift groups
  (UC-J / UC-G / UC-I / UC-J / UC-H, in the original declaration
  order) with the exact same keyword strings as the previous
  hardcoded `DriftDetector.HARD_SHIFT_KEYWORDS` list, plus the same
  escalation regex under `escalation-pattern`.
- New loader: `RiskKeywordsConfig` (Spring `@Service`) loads the YAML
  at startup, compiles the escalation pattern, and validates:
  required fields, non-empty groups, non-blank keywords, no
  cross-group keyword duplicates (which would be unreachable under
  first-match-wins), and a parseable regex. Fail-fast on any
  violation.
- `DriftDetector` constructor-injects `RiskKeywordsConfig`. The
  matching contract is unchanged — same lower-case substring check,
  same first-match-wins precedence within and across groups, same
  same-UC suppression, same `DriftResult` outputs.
- A no-arg test convenience constructor on `DriftDetector` loads the
  bundled default classpath resource so legacy unit tests
  (`DriftDetectorTest`) keep working without behavioural change.

Sprint 15 §M0 explicitly does NOT add new risk semantics, new risk
levels, new escalation reasons, or auto-handover for Level 1 / Level
2 risk signals. Future risk-policy reviews are expected to land as
config diff rather than Java source edits.

Tests added:

- `Sprint15RiskKeywordsConfigTest` (11 cases) — config validation
  parity (default loads, group / keyword set parity vs the hardcoded
  reference, version pin), six structural-validation negative tests
  (missing escalation pattern, empty group list, empty keywords,
  blank target UC, duplicate keyword, invalid regex), the full
  parity matrix across escalation phrases / hard-shift groups /
  same-UC suppression / escalation-precedence / no-drift baseline,
  the cross-group first-match-wins precedence assertion, and a
  keyword-uniqueness sanity guard.

#### M1 — Script library version pin and docs ↔ YAML consistency check

What landed:

- `server/src/main/resources/scripts/templates.yaml` now declares
  `library_version: "v1.1"` and `library_version_date: "2026-04-21"`,
  matching the latest entry of the `## 11. Version` table in
  `docs/fixed_script_library_v1.md`.
- `ScriptLibraryService` reads and exposes the version pin
  (`getLibraryVersion()`, `getLibraryVersionDate()`) and fails fast
  at startup if `library_version` is missing.
- New build-time check:
  `Sprint15ScriptLibraryConsistencyTest` parses both surfaces and
  fails the build when:
  - `library_version` / `library_version_date` is absent or blank,
  - the YAML version pin diverges from the latest doc version row,
  - any required template ID listed in the test's reference manifest
    is missing from the YAML,
  - a required template's `variables:` declaration drifts from the
    doc's variable contract,
  - a top-level template id is duplicated.

Sprint 15 §M1 explicitly does NOT rewrite script copy, change tone /
escalation wording, alter forbidden-phrase rules, or introduce a new
template engine. ScriptLibraryService substitution semantics are
unchanged except for additive metadata validation.

Tests added:

- `Sprint15ScriptLibraryConsistencyTest` (5 cases) — version pin
  presence, version + date parity vs the doc's §11 Version table,
  required-template-IDs presence, required-variables contract
  parity for every template that takes parameters, and duplicate
  top-level id detection.

#### M2 — Retrieval / answer gate / rerank fallback thresholds + diagnostics

What landed:

- New `@ConfigurationProperties("knowledge.retrieval")` bean
  `KnowledgeRetrievalProperties` carrying the previously hardcoded
  thresholds with **defaults unchanged**:
  - `ann-limit: 20`
  - `retrieval-gate-threshold: 0.3`
  - `answer-gate-threshold: 3.5`
  - `rerank-candidates: 8`
  - `top-results: 3`
  - `rerank-fallback-score: 2.5`
- Range validation runs in a `@PostConstruct` `validate()` step:
  `ann-limit ≥ rerank-candidates ≥ top-results ≥ 1`, retrieval gate
  ∈ [0.0, 1.0], answer gate ∈ [1.0, 5.0], fallback score ∈ [1.0,
  5.0], and `rerank-fallback-score < answer-gate-threshold` so a
  fallback-score result can never satisfy the answer gate.
- `application.yml` adds an explicit `knowledge.retrieval` block
  whose values match the defaults verbatim — future tuning becomes
  an auditable config diff rather than a hidden Java edit.
- `KnowledgeSearchService` no longer holds `static final`
  thresholds; it reads them per-call from the injected properties
  bean. Pipeline behaviour (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  unchanged.
- `RerankService` now sources the neutral fallback score from
  `KnowledgeRetrievalProperties.rerankFallbackScore()` (default 2.5,
  unchanged) for both the parse-fallback path and the LLM-failure /
  future-failure paths.
- Diagnostics added (observability-only, no behaviour change):
  - `KnowledgeSearchService` logs `retrieval_miss` /
    `answer_miss` with `reason=retrieval_gate` or `reason=answer_gate`
    and the threshold value; on `answer_miss` it also logs
    `top_is_fallback_score=true|false` so a parse-failure /
    call-failure attribution is visible.
  - `RerankService` logs `Rerank parse fallback` with
    `reason=empty_response | unparseable` plus the fallback score
    used, and `Rerank LLM call failed` /
    `Rerank future failed` with the fallback score on the failure
    paths.

Sprint 15 §M2 explicitly does NOT tune thresholds, change FAQ
answerability semantics, change corpus content, or change eval
expected outcomes.

Tests added:

- `Sprint15KnowledgeRetrievalConfigTest` (9 cases) — defaults parity
  (each default pinned by literal value), defaults pass validation,
  and seven structural-validation negative tests (ann-limit < 1,
  ann-limit < rerank-candidates, rerank-candidates < top-results,
  retrieval gate out of range, answer gate out of range,
  fallback ≥ answer gate, fallback out of range).

Updates to existing tests for constructor signature changes:

- `RerankServiceTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor; the
  pre-existing parse-failure / call-failure cases remain green and
  continue to assert the 2.5 fallback score (now sourced from the
  default config).
- `Sprint14KnowledgeSearchPublishedFilterTest` — passes the default
  `KnowledgeRetrievalProperties` instance into the constructor;
  Sprint 14 §L0 published-only filter / projection contract is
  preserved and remains green.

### 16.2 Files changed (Sprint 15)

Production code:

- `server/src/main/java/com/gumtree/csagent/service/runtime/RiskKeywordsConfig.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java`
  (constructor-injects `RiskKeywordsConfig`; matching contract
  unchanged)
- `server/src/main/java/com/gumtree/csagent/service/guardrails/ScriptLibraryService.java`
  (reads `library_version` / `library_version_date`; fail-fast on
  missing pin; substitution semantics unchanged)
- `server/src/main/java/com/gumtree/csagent/config/KnowledgeRetrievalProperties.java` (new)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/KnowledgeSearchService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  thresholds from config; adds threshold-gate diagnostics; pipeline
  behaviour unchanged)
- `server/src/main/java/com/gumtree/csagent/service/knowledge/RerankService.java`
  (constructor-injects `KnowledgeRetrievalProperties`; reads
  fallback score from config; adds parse / call / future failure
  diagnostics; scoring contract unchanged)

Config / resources:

- `server/src/main/resources/config/risk-keywords.yaml` (new)
- `server/src/main/resources/scripts/templates.yaml`
  (additive `library_version` + `library_version_date` keys; no
  template copy change)
- `server/src/main/resources/application.yml`
  (additive `knowledge.retrieval.*` block matching the previous
  hardcoded defaults verbatim)

Tests (new):

- `Sprint15RiskKeywordsConfigTest` (11 cases — config validation,
  group / keyword parity, full DriftDetector behavioural parity
  matrix, first-match-wins precedence)
- `Sprint15ScriptLibraryConsistencyTest` (5 cases — version pin
  presence, doc-vs-YAML version + date parity, required template
  IDs, required variables contract, duplicate-id guard)
- `Sprint15KnowledgeRetrievalConfigTest` (9 cases — defaults parity,
  validation positive + 7 negatives)

Tests (updated for constructor signature):

- `RerankServiceTest`
- `Sprint14KnowledgeSearchPublishedFilterTest`

Docs:

- `docs/10-handoff.md` (this section + §1 update).
- `docs/action_bank.md` (Sprint 15 deliverables added).
- `docs/sprint_objective.md` retained — already contains the
  Sprint 15 objective.
- No edit to `docs/current_eval_baseline.md` — Sprint 15 changes no
  eval expected outcome; baseline remains the post-Sprint-8 r1 / r2
  runs.

No FAQ corpus content, CaseSpec, judge, broad routing taxonomy,
handover payload contract, Salesforce backend contract, full Issue
Ledger, all-UC task taxonomy, escalation enum, system prompt, DB
schema migration, runtime main-flow architecture file, or hard
citation gate was touched.

### 16.3 Tests run

- `mvn -pl server test -Dtest='Sprint15*,DriftDetectorTest,RerankServiceTest'`
  → **55 / 0 / 0 / 0** (25 Sprint 15 §M0 / §M1 / §M2 focused tests +
  18 legacy `DriftDetectorTest` cases + 12 `RerankServiceTest`
  cases, confirming parity under the new wiring).
- `mvn -pl server test -Dtest='Sprint10*Test,Sprint11*Test,
  Sprint12*Test,Sprint13*Test,PhaseEvaluatorPlanTest,Cs014*,
  Cs066*,Cs095*,Cs002*,Cs029*,Cs176*,Cs001*,EscalationReason*Test,
  Sprint6*,Sprint7*Test,Sprint8*Test,Sprint9*Test,Sprint14*,
  Sprint141*,Sprint15*'`
  → **332 / 0 / 0 / 0** (extended named regression suite — including
  Sprint 6 §G2 FAQ S1 guard, cs014 / cs066 / cs095 / cs002 / cs029 /
  cs176 regressions, all Sprint 14 §L0 / §L1 / §L2 + §14.1 trace
  persistence, plus the new Sprint 15 §M0 / §M1 / §M2 focused
  suite).
- `mvn -pl server test`
  → **884 / 0 / 0 / 0** (was 859 pre-Sprint-15; +25 new Sprint 15
  §M0 / §M1 / §M2 tests).
- `pytest eval_interactive/tests/` — not re-run; Sprint 15 changes
  no Python parsing path and no eval-output schema.
- Smoke runs not required — Sprint 15 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema. The current canonical baseline
  (`docs/current_eval_baseline.md`) is preserved (post-Sprint-8 r1 /
  r2 runs).

### 16.4 Behaviour-preservation evidence

- §M0 — `Sprint15RiskKeywordsConfigTest.detect_parityMatrix_matchesHardcodedReference`
  exercises every escalation phrase, every hard-shift keyword group,
  the same-UC suppression rule, the escalation-precedence rule, and
  the no-drift baseline against the previously hardcoded
  expectations. The legacy `DriftDetectorTest` (18 cases) keeps
  passing under the new YAML-loaded path. The
  cross-group-first-match-wins assertion locks the precedence
  ordering. No DriftResult shape, type, or field has changed.
- §M1 — `ScriptLibraryService.getTemplate(...)` /
  `renderTemplate(...)` substitution semantics are unchanged. The
  approved template copy (50+ templates / 14 categories) is
  bit-for-bit identical to v1.1 of the doc; the consistency test
  enforces no silent copy drift in the variables contract.
- §M2 — `Sprint15KnowledgeRetrievalConfigTest.defaults_matchPreviouslyHardcodedConstants`
  pins each default to the literal value of the prior `static
  final` constant. `Sprint14KnowledgeSearchPublishedFilterTest`
  (Sprint 14 §L0 contract) and `RerankServiceTest` (parse / call
  failure → 2.5 fallback) keep passing under the new wiring. The
  full retrieval pipeline (ANN limit, retrieval gate test, dedup,
  rerank window, answer gate test, top-results truncation) is
  observable-equivalent under default config; the validator
  forbids any threshold combination that would change semantic
  ordering.

### 16.5 Config files added / changed

- `server/src/main/resources/config/risk-keywords.yaml` (added) —
  source of truth for `DriftDetector` escalation pattern + hard-shift
  groups.
- `server/src/main/resources/scripts/templates.yaml` (changed —
  additive `library_version`, `library_version_date` only).
- `server/src/main/resources/application.yml` (changed — additive
  `knowledge.retrieval.*` block; defaults match prior hardcoded
  constants verbatim).

### 16.6 Regression guard outcomes

All Sprint 15 active guards pass:

- `L1:escalation_reason_consistency` = **0** (never re-introduced).
- `CONTRACT_VIOLATION:active_use_case` = **0**.
- Sprint 14 `bot_turns.projected_context.faq_grounding` persistence
  trace remains intact (`Sprint141FaqGroundingTracePersistenceTest`,
  5 cases).
- Sprint 14 published-only KB search remains intact
  (`Sprint14KnowledgeSearchPublishedFilterTest`, 4 cases).
- Existing FAQ S1 guard remains green
  (`AgentRunLoopS1FaqGroundedResolveGuardTest`).
- Sprint 6 §G0 ReadTimeout no-retry closure intact.
- cs014 remains UC-C
  (`Cs014RouteAndLoopHandoverIntegrationTest`,
  `Cs014RouteAndDistressRegressionTest`).
- cs066 remains UC-K.
- cs095 remains UC-A / not UC-K / not UC-FP.
- cs002 distress reconciliation remains green.
- cs029 remains UC-D + `user_requested`.
- cs176 explicit-human-help → `user_requested` regression remains
  green.
- All Sprint 7 / 7.1 / 8 / 8.1 / 8.2 / 9 / 9.1 / 10 / 11 / 11.1 /
  12 / 13 hard invariants remain green
  (named regression suite: 332 / 0).
- `RuntimeIntentClassifier` remains runtime-internal.
- `bot_turns.source_ids` write path unchanged.
- `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.

### 16.7 Remaining risks

**No new P0 / P1 blockers opened by Sprint 15.** The change is
config-governance + diagnostics; no runtime main-flow architecture
file gained new semantics, no DriftResult / KnowledgeSearchResult /
ScoredCandidate shape changed.

Carry-over notes:

- The `library_version` pin must be updated whenever
  `docs/fixed_script_library_v1.md` ships a new approved version
  row — `Sprint15ScriptLibraryConsistencyTest` will fail the build
  if the two surfaces diverge. This is the intended invariant.
- Future threshold tuning must land as a `knowledge.retrieval.*`
  config diff. The validator constraints
  (`fallback < answer-gate`, `ann ≥ rerank ≥ top`) are the floor,
  not a tuned recommendation.
- `RiskKeywordsConfig` is loaded once at startup. Hot-reload remains
  out of scope; Sprint 15 explicitly does NOT introduce ops-owned
  runtime config. A risk-keyword change still requires a redeploy.

Residuals carried forward from Sprint 14 §10 remain unchanged
(`R-faq-grounded-resolve-bypass`, `R-cited-but-unresolved`,
`R-resolved-but-uncited-rate`, `R-canonical-url-missing-rate`, plus
the older Sprint 13 §8 list).

### 16.8 Recommended next phase

The Sprint 15 closure surfaces no runtime regression. The previously
listed alternatives remain viable:

1. **Eval Governance docs sprint** — primary recommendation. The
   largest residual category is still `judge_volatility` /
   `faq_corpus_gap` / `product_policy_gap`, all governance-owned.
2. **Narrow Sprint 16 §S1 hardening** — only if real-traffic
   evidence demonstrates `R-faq-grounded-resolve-bypass`
   reproducibly affects answer correctness on a high-traffic UC.
3. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows. Owner: Eval Governance / corpus audit.
4. **Re-run validation after infra cleanup** — viable if Kimi /
   smoke credentials are available; runs alongside Eval Governance.

Do not start another runtime sprint unless triage finds a new
P0 / P1 runtime blocker. Sprint 15 explicitly does NOT promote a
new canonical eval baseline.

### 16.9 What Sprint 15 explicitly does NOT do

- No new risk semantics, no new escalation reasons, no new risk
  levels, no auto-handover for Level 1 / Level 2 risk signals.
- No new hard Java guard.
- No prompt rewrite, no system prompt edit, no broad routing
  rewrite.
- No hard runtime citation gate.
- No FAQ corpus rewrite.
- No judge calibration, no CaseSpec changes, no eval expansion.
- No anchor / exploration / promotion hard-gate expansion.
- No hot reload / ops-owned runtime config.
- No dashboard work, no full TraceViewer redesign.
- No new skill runtime framework.
- No broad S1 rewrite.
- No DB migration. No schema change. No bot_turns / bot_sessions /
  kb_articles / kb_chunks edit.
- No threshold tuning. Defaults are pinned to the previous
  hardcoded values verbatim.

## 17. Sprint 16 — Handover Exactly-Once Contract and Repro

Status: **landed (docs + characterization-test sprint, no runtime
change)**. Sprint 16 freezes the handover exactly-once contract and
adds the characterization tests that demonstrate the current
LLM-driven dual local-persistence shape, without modifying any
runtime behaviour. It does not yet implement
`HandoverOrchestrator`; that is the explicitly deferred future
runtime sprint trigger.

### 17.1 Exact docs / tests changed

Docs (new + edited):

- `docs/proposals/handover_orchestrator_design.md` (new) — defines the
  exactly-once contract. §1 names the current dual-path shape
  (writer #1 = `RequestHandoverTool` →
  `SalesforceService.requestHandover` →
  `MockSalesforceService` → `mock_handover_log` row #1; writer #2
  = `SessionManager.recordHandover` → direct
  `handoverLogRepository.save(...)` → `mock_handover_log` row
  #2). §2 distinguishes confirmed local persistence duplication
  from unproven real Salesforce double transfer. §3 freezes three
  contracts: trace evidence (`request_handover`), outcome
  persistence (`record_outcome`), and the future
  `HandoverOrchestrator` handover side-effect (idempotent by
  `session_id`). §4 sketches the future orchestrator shape. §5
  records why Sprint 16 is docs + characterization only. §6
  pre-specifies acceptance criteria for the future runtime sprint.
- `docs/runtime_freeze_and_risk_policy.md` (edited) — added §10
  "Known unspecced surfaces" with §10.1 entry covering the
  handover dual-path. Renumbered References to §11; added
  cross-reference links to the Sprint 16 design / release_gate /
  source files.
- `docs/release_gate.md` (new) — opens the release-gate ledger.
  §1.1 blocking rule: before real Salesforce cutover, the
  handover side-effect must be idempotent by `session_id`. The
  rule is **not satisfied** at Sprint 16 close.
- `docs/action_bank.md` (edited) — Sprint 16 row added to §3
  active deliverables (H0 / H1 / H2); new
  D-single-handover-orchestrator entry in §4 deferred runtime
  candidates ("Single Handover Orchestrator: exactly-once
  side-effect owner before Salesforce production cutover");
  Sprint 16 row added to §6 closed-action index.
- `docs/sprint_objective.md` retained — already contains the
  Sprint 16 objective.
- `docs/10-handoff.md` (this file) — §1 and new §17 (this
  section).

Tests (new):

- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint16HandoverDualPathReproTest.java`
  (3 cases): two passing characterization cases plus one
  `@Disabled` future-invariant case.
  - `dualLocalPersistence_llmDrivenRequestHandoverPath_producesTwoMockHandoverLogWrites_currentBehaviour`
    (passing) — exercises `RequestHandoverTool.execute(...)` with
    a real `MockSalesforceService` backed by a mocked
    `MockHandoverLogRepository`, then replays the
    `SessionManager.recordHandover` surface (real
    `HandoverPayloadAssembler` → direct `repo.save(...)`).
    Asserts two saves on the same repository for the same
    `session_id`; asserts the two payloads carry distinct
    `version` (`"1.0"` vs `"1.1"`) and distinct `transfer_result`
    (`MockSalesforceService` set vs `mock_transfer` literal).
  - `distinguishesLocalPersistenceDuplication_fromUnprovenRealSalesforceDoubleTransfer`
    (passing) — pins that today the only `SalesforceService`
    implementation is `MockSalesforceService`. Designed to break
    on first wire-up of a real Salesforce client so the reviewer
    is forced to re-read the §10.1 contract before passing the
    release gate.
  - `futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands`
    (`@Disabled`, TODO) — encodes the future invariant: at most
    one transmitted / `offline_logged` handover decision per
    `session_id`. Would fail today by design (the LLM-driven
    path writes twice). Removing the `@Disabled` annotation is
    listed as one of the acceptance criteria for the future
    "Single Handover Orchestrator" runtime sprint
    (`docs/release_gate.md` §1.1 #4-#5).

Source comments (new, no behaviour change):

- `server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java`
  — class-level Javadoc carries a Sprint 16 §H0
  known-unspecced-surface marker pointing to
  `docs/proposals/handover_orchestrator_design.md` and the future-invariant
  test.
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  — `recordHandover(...)` carries the matching marker on its
  Javadoc.

### 17.2 Current confirmed risk

- **Duplicated local persistence (P2 today, mock / local).** On
  the LLM-driven `request_handover` path two
  `mock_handover_log` rows are written for the same `session_id`
  on the same ESCALATE turn. The two writers are unaware of each
  other and persist payloads with different `version` and
  `transfer_result` shapes.
- **P1 launch-readiness severity.** The dual-path shape is one
  wire-up away from a real double transfer at the moment a
  production `SalesforceService` implementation lands.
- **P0 / P1 production-incident severity** if a real double
  transfer occurs after cutover (case re-routed twice in
  Salesforce: re-assignment, mis-prioritisation, queue
  duplication; user-visible).
- The hard-OOS path (`SessionManager.createSession` → synthetic
  `request_handover` evidence + single `recordHandover`) and the
  kernel force-escalate / Step 2.5 path remain single-write today
  (pinned by Sprint 16 §H0 contract; not opened or weakened by
  Sprint 16).

### 17.3 What is not confirmed

- A real **double Salesforce transfer** is not currently proven.
  The production Salesforce client is not wired; only
  `MockSalesforceService` (`@Profile("local")`) implements
  `SalesforceService`, and `SessionManager.recordHandover` does
  not flow through `SalesforceService` at all (it writes directly
  to `mock_handover_log`).
- `HandoverPayloadAssembler` is the candidate single payload
  builder for the future orchestrator. Sprint 16 does not commit
  to it as the final shape; the orchestrator design doc names it
  as the candidate but leaves payload-schema decisions to the
  runtime sprint.
- No production case routing is currently affected. The
  characterization test runs against an in-memory mocked
  repository.

### 17.4 Future runtime sprint trigger

A future "Single Handover Orchestrator" runtime sprint must start
when either:

1. a real Salesforce client is staged for cutover (the launch-time
   wire-up forces the orchestrator to land first to satisfy
   `docs/release_gate.md` §1.1), or
2. a real-traffic case shows duplicated handover routing in
   Salesforce (would be the first P0 / P1 confirmation that
   moves the issue out of P2 territory).

Until then, `Sprint16HandoverDualPathReproTest` is the standing
guard that the dual-path shape has not been silently fixed (the
two passing cases would break) and that the future invariant has
not been silently re-introduced as a hard gate (the `@Disabled`
case would need to be re-enabled deliberately).

The runtime sprint's acceptance criteria are pre-specified in
`docs/proposals/handover_orchestrator_design.md` §6 and
`docs/release_gate.md` §1.1.

### 17.5 Tests run

- `mvn -pl server test
  -Dtest='Sprint16*,RequestHandoverToolTest,HandoverPayloadAssemblerTest,Cs014RouteAndLoopHandoverIntegrationTest,Cs176ExplicitHumanHelpHandoverIntegrationTest,AgentRunLoopHandoverReasonNormalizationIntegrationTest'`
  → **22 / 0 / 0 / 1** (Sprint 16 §H1 focused suite + handover
  regression suite; the 1 skipped is the `@Disabled`
  future-invariant case).
- `mvn -pl server test`
  → **887 / 0 / 0 / 1** (was 884 pre-Sprint-16; +2 new passing
  Sprint 16 tests + 1 new disabled future-invariant test). No
  regression in any Sprint 6 / 7 / 7.1 / 8 / 8.2 / 9 / 9.1 / 10
  / 11 / 11.1 / 12 / 13 / 14 / 14.1 / 15 invariant. The cs014 /
  cs066 / cs095 / cs002 / cs029 / cs176 / cs001 regression suite
  remains green. `L1:escalation_reason_consistency = 0` and
  `CONTRACT_VIOLATION:active_use_case = 0` are preserved.
- `pytest eval_interactive/tests/` — not re-run; Sprint 16
  changes no Python file, no eval-output schema, no judge, no
  CaseSpec, no FAQ corpus. The Sprint 16 diff is two Markdown
  files (new), three Markdown files (edited), one new Java test,
  and two Javadoc-only edits on existing Java source.
- Smoke runs not required — Sprint 16 changes no FAQ corpus, no
  prompt, no judge, no CaseSpec, no escalation enum, no routing
  taxonomy, no eval-output schema, no DB schema. The current
  canonical baseline (`docs/current_eval_baseline.md`) is
  preserved (post-Sprint-8 r1 / r2 runs).

### 17.6 Next recommended action

Recommended next phase: **defer**. Sprint 16 is intentionally a
docs + characterization sprint; the runtime fix
("Single Handover Orchestrator") is queued as a deferred runtime
candidate (`docs/action_bank.md` §4 row
`D-single-handover-orchestrator`) and gated by the real-Salesforce
cutover plan (`docs/release_gate.md` §1.1).

In priority order:

1. **Eval Governance Follow-up** — primary recommendation
   carried over from Sprint 15. Largest residual category is
   still `judge_volatility` / `faq_corpus_gap` /
   `product_policy_gap`.
2. **Single Handover Orchestrator runtime sprint** — start ONLY
   when (a) a real Salesforce client is staged for cutover, or
   (b) a real-traffic case shows duplicated handover routing.
   Acceptance criteria pre-specified in
   `docs/proposals/handover_orchestrator_design.md` §6 and
   `docs/release_gate.md` §1.1; the `@Disabled` future-invariant
   test is the closing artifact.
3. **Narrow Sprint 16 §S1 hardening** (FAQ-grounded resolve
   bypass) — only on real-traffic evidence per
   `docs/current/faq_grounding_contract.md` §6.
4. **Narrow Corpus Curation** — fill the 38 missing
   `Help_Site_URL__c` rows.

Do not start the Single Handover Orchestrator runtime sprint
opportunistically. Sprint 13's runtime freeze remains in force; a
new runtime sprint requires a P0 / P1 trigger or the real
Salesforce cutover plan.

### 17.7 What Sprint 16 explicitly does NOT do

- No `HandoverOrchestrator` implementation. Sprint 16 is design
  + characterization only; the runtime fix is the future
  "Single Handover Orchestrator" sprint.
- No production Salesforce client. The release-gate rule
  (`docs/release_gate.md` §1.1) blocks that wire-up until the
  orchestrator lands.
- No change to `RequestHandoverTool` behaviour. Class-level
  Javadoc adds a §H0 marker only.
- No change to `SessionManager.recordHandover` behaviour. Method
  Javadoc adds a §H0 marker only.
- No change to `MockSalesforceService` behaviour or the
  `SalesforceService` interface.
- No change to the Phase 3 §3.6.2 handover payload schema.
- No change to `HandoverPayloadAssembler` behaviour.
- No prompt edit. No `system_prompt.txt` change.
- No routing change. No new escalation reason. No CaseSpec
  change. No FAQ corpus change. No judge calibration change. No
  eval expected-outcome change.
- No DB migration. No schema change. `mock_handover_log` /
  `bot_turns` / `bot_sessions` / `kb_articles` / `kb_chunks`
  schemas unchanged.
- No new live smoke baseline. No anchor / exploration /
  promotion hard-gate change.
