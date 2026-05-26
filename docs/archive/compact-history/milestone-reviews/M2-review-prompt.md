Paste the content below this line into a fresh Codex session at M2 milestone close, after Sprint 41 has closed A — Clean PASS. No PR will be opened; review the milestone-shared cumulative commit range directly.

---

You are the Anti-Hardcode + Milestone-Close Review Agent for **NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization, LLM-led Policy-bounded)** per `docs/milestone_objective.md`. M2 is the FIRST milestone planned and executed under the `iteration_governance.md` §8 milestone framework (2026-05-16 governance upgrade) using a milestone-shared cumulative Codex review per §4.3 second paragraph. M2 supersedes the OLD M2-Skill milestone mid-flight on 2026-05-17 per human direction; the OLD minimum-surface freeze preserved the scattered Zhang-Sanfeng pattern that M2 was supposed to fix, so NEW M2 promotes Skill to a first-class abstraction with externalized YAML/JSON definitions + SkillRegistry + retroactive migration of ALL 6 phase content + Sprint 23/31/33 teaching paragraphs + Sprint 6/7/11 predicates + the cross-Skill state preservation surface.

M2 shipped 5 sub-sprints across 7 commits over 2 calendar days (2026-05-17 → 2026-05-18):

| Sub-sprint | Commit(s) | Outcome | Per-sub-sprint Codex |
|---|---|---|---|
| Sprint 37 (design freeze) | `51c327c` (dev) + `649c6c7` (close) | A — Clean close | `pass / 0` first pass per `docs/sprints/sprint-037-codex-review.md` |
| Sprint 38 (SkillRegistry-core + 4 simpler phase Skills) | `bd9d3f5` (main) + `5787806` (fix #1) | B-fix-iterated | round 1 `fix_required / 1` on SkillLoader §2.2 schema-validation completeness; round 2 fix-review `pass / 0` per `docs/sprints/sprint-038-codex-review.md` |
| Sprint 39 (RESOLVE migration + Sprint 6/7/11 predicate migration + S1 + S2 + unified dispatcher) | `2f412b6` | A — Clean PASS | `pass / 0` first pass per `docs/sprints/sprint-039-codex-review.md` |
| Sprint 40 (teaching extraction from system_prompt.txt + orchestration shell cleanup) | `9130abc` | A — Clean PASS | `pass / 0` first pass per `docs/sprints/sprint-040-codex-review.md` |
| Sprint 41 (UC switch + state preservation across Skill boundary; LAST M2 sub-sprint) | `8cd0a10` | A — Clean PASS | `pass / 0` first pass per `docs/sprints/sprint-041-codex-review.md` |

**M2 cumulative commit range: `51c327c..8cd0a10` (7 commits).**

This **milestone-shared review** is the CUMULATIVE check across the full M2 range. Each sub-sprint already received per-sub-sprint Codex review per `iteration_governance.md` §4.3 trigger #3 (design freeze touches; SkillLoader schema completeness; dispatcher landing + state model touch; system_prompt.txt structural change; cross-Skill state preservation + C3 Tier-0 evidence surface). This M2 review re-walks the §4.1 nine-question kernel + §4.2 sprint-close header against the FULL M2 commit range to verify (a) milestone-level scope discipline across the 5 sub-sprints; (b) cumulative architectural coherence (the 5 sub-sprints work together as one Skill Registry Abstraction); (c) M2 §6 hard fences honored across the full range; (d) **C2 + C3 Tier-0 candidate re-evaluation with FULL milestone evidence** (load-bearing M2-close decision); (e) M2 acceptance bar recalibration honored (smoke + bad-case = OBSERVATION not gate per `iteration_governance.md` §5.5 + `docs/milestone_objective.md` §5); (f) design doc editorial fold-back queue surfaced for SEPARATE governance commit per `doc_governance.md` cadence.

**M2 ships (cumulative artefacts):**

- **Skill abstraction** (Sprint 37 design freeze; Sprint 38 implementation): `Skill.java` (135 lines; 15-field record per design doc §2.1; `state_inheritance` field), `Guardrail.java` (56 lines; per §5.2), `StateInheritance.java` (59 lines; per §10.2), `SkillRegistry.java` (123 lines; Spring `@Component`; `select(phase, useCase) → Skill` per §3.2), `SkillLoader.java` (313 lines; Spring `@Component`; `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` + `VALID_TOOL_NAMES` + `VALID_GUARDRAIL_TYPES` allowlists per §2.2 + Sprint 38-fix). All under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/`.
- **6 Skill YAMLs** at `server/src/main/resources/skills/`: `discover_triage.yaml` (32 lines post-S41; Sprint 38 + Sprint 40 + Sprint 41 contributions), `confirm.yaml` (30 lines), `escalate.yaml` (29 lines), `terminal.yaml` (26 lines), `resolve_faq_grounded_answer.yaml` (53 lines; Sprint 39 NEW + Sprint 41 state_inheritance extension), `resolve_intake_collect_and_handover.yaml` (38 lines; Sprint 39 NEW + Sprint 41 state_inheritance extension).
- **PhaseEvaluator integration** (`PhaseEvaluator.java` 1628 → 1482 lines; −146 net across S38+S39+S41): SkillRegistry-first dispatch via `composeSkillPhasePlan(...)` helper (Sprint 38); template substitution for RESOLVE Skills via `substitutePlaceholders(text, activeUc)` (Sprint 39); `maybeApplyStateBusOnSwitch(History, BotSession, Skill newSkill)` helper + SkillStateBus invocation (Sprint 41); 4 legacy phase branches DELETED at Sprint 38 (DISCOVER + CONFIRM + CLOSE + ESCALATE) + 2 more DELETED at Sprint 39 (RESOLVE-INTAKE + RESOLVE-FAQ + legacy `buildIntakeSystemInstruction(...)` helper); ctor 10→11 (S38) → 12 (S41) args.
- **AgentRunLoopImpl migration** (787 → 638 lines at S39; −149): 3 dispatch sites at HANDOVER_TOOL + RECORD_OUTCOME_TOOL routed through `SkillGuardrailDispatcher`; 3 `shouldRejectXxx` methods + 1 helper + 7 dead constants REMOVED; new `@Autowired` 6-arg constructor.
- **Unified Skill terminal-predicate dispatcher** (`SkillGuardrailDispatcher.java` 416 lines NEW at S39; Spring `@Component`; 4 typed handlers — `handleFaqMissHandoverRequiresResolveAttempt` (Sprint 6 §G2 migration), `handleIntakeCompleteRequired` (Sprint 7 §I2 migration), `handlePrematureResolveOutcomeGuard` (Sprint 11 §M1 migration via verbatim delegation to UNCHANGED `ResolveDispositionEvaluator`), `handleMustCiteSource` (NEW S1 bounded per M2 §6 #4 verbatim 3-boundary check); short-circuit-on-first-reject per design doc §9.2; bounded to 4 typed predicate types in `SkillLoader.VALID_GUARDRAIL_TYPES`); 2 NEW Java records (`RejectVerdict.java` + `DispatchContext.java`).
- **NEW S1 `must_cite_source` predicate** (Sprint 39; declared on `resolve_faq_grounded_answer.yaml:34-45` + implemented in `handleMustCiteSource` lines 340-384; verbatim M2 §6 #4 3-boundary check — phase=RESOLVE_FAQ via Skill `applicable_use_cases: [UC-A..UC-FP]` scope; tool=`record_outcome` with `class=resolve`; check=`source_id` citation presence only, NO content-quality / relevance / correctness judgment).
- **NEW S2 `intake_complete_required` predicate** (Sprint 39; declared on `resolve_intake_collect_and_handover.yaml:25-30` + implemented in `handleIntakeCompleteRequired` lines 265-298; Sprint 7 §I2 semantics moved bit-for-bit; delegates to UNCHANGED `IntakeFieldsRegistry`).
- **Teaching extraction** (Sprint 40): `system_prompt.txt` 101 → 80 lines (Sprint 31 + Sprint 33 SLOT description + DISCOVER phase guidance bulk migrated to `discover_triage.yaml` `procedure`; Sprint 23 `already_called` + `request_handover` decision tree PRESERVED in shell; NEW envelope-mechanics paragraph + DISCOVER one-line pointer combined at post-Sprint-40 lines 30-31 per OQ-S40.2 dev judgement Codex AGREED). `discover_triage.yaml` `procedure` extended ~2900 → ~7900 chars with migrated content + Sprint 33 cue body preserved alongside (cue/slot split closed per design doc §7.2.3).
- **Cross-Skill state preservation** (Sprint 41): NEW `SkillStateBus.java` (203 lines; Spring `@Component`; design doc §10.3 public surface `applyOnSkillSwitch(Skill, Skill, BotSession)` + `inheritanceFor(Skill)`; registry-driven `applyIntakeFieldsIntersection(BotSession)` via `IntakeFieldsRegistry.requiredFieldsFor(...)` + `canonicalFieldName(...)` — SINGLE registry lookup, NOT per-UC-pair branch table per §1.7). NEW `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` (1161 → 1341; +180; per §10.4; cap=3 + aging=4; sibling to Sprint 31 + Sprint 33 slots). Skill-switch detection in `PhaseEvaluator.maybeApplyStateBusOnSwitch(...)` invoking bus at the single boundary (per §10.3; detection rides on existing M1 Sprint 32 + Sprint 33 projections — NO new classifier per M2 §6 #5 fence). All 6 Skill YAMLs declare `state_inheritance` per §10.5 invariant matrix.
- **Tests**: ~150 new Java tests across all M2 sub-sprints (S38 51 tests: SkillTest 9 + SkillRegistryTest 11 + SkillLoaderTest 18 + PhaseEvaluatorSkillIntegrationTest 13; S39 60 tests: PhaseEvaluatorResolveSkillIntegrationTest 14 + SkillGuardrailDispatcherTest 24 + ResolveFaqGuardrailsTest 11 + ResolveIntakeGuardrailsTest 11; S40 11 tests: SkillTeachingMigrationIntegrationTest 11; S41 39 tests: SkillStateBusTest 17 + PriorUseCaseCarryProjectionTest 13 + UcSwitchStateInheritanceTest 9). Java baseline trajectory: 983 (pre-M2) → 1028 (post-S38-main) → 1034 (post-S38-fix) → 1094 (post-S39) → 1105 (post-S40) → 1144 (post-S41). The 1 inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure since Sprint 24-era working-tree mod persists unchanged across all M2 sub-sprints.

**M2 acceptance bar recalibration** (per `docs/milestone_objective.md` §5; 2026-05-17 human-judgment recalibration of `iteration_governance.md` §5.6 primary-gate framing for THIS milestone): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION ONLY (NOT hard gate) — M2 is an architecture-focused milestone; gating Alice or smoke would re-import the §5.5 confounding sources (external LLM provider drift + judge calibration variance + mocked-LLM gap + eval rubric instability). **Primary M2 acceptance gate** = functional review (this milestone-shared review) + Java tests pass (1144 / 1 inherited / 0 / 2 at M2 close) + Sprint 37 freeze decisions (a)-(j) honored across implementation sub-sprints S38-S41 + each per-sub-sprint Codex review `approve` verdict (S37 PASS A; S38 B-fix-iterated; S39 PASS A; S40 PASS A; S41 PASS A). **Codex MUST NOT re-import `iteration_governance.md` §5.6 bad-case-suite-primary default at M2 close**; this milestone-shared review evaluates the architecture, not behavioural-suite pass rates. The same recalibration was honored across all 5 per-sub-sprint Codex reviews.

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip; the prompt's later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / **§4.3** (milestone-shared review provisions) / §5 / §5.5 / §5.6 / §7 / **§8** (milestone framework)).
2. **`docs/milestone_objective.md`** — the NEW M2 milestone north star. Read end-to-end. Especially: §1 milestone class + §7 stanza coverage across S38-S41; §2 Goal — the architectural outcome (Skill Registry Abstraction + Wholesale Retroactive Externalization; LLM-led Policy-bounded); §3 sub-sprint sequence (S37 → S41); §4 non-goals; §5 acceptance bar recalibration (bad-case suite + interactive eval = OBSERVATION); §6 hard fences (22 items); §7 R-items consumed / surfaced; §8 Codex review plan (milestone-shared); §11 cross-milestone sequencing (M3-A/B/C/D/Latency/Skill-Tuning/Tier-0 re-eval candidates).
3. **All 5 per-sub-sprint objective archives** (immutable per `doc_governance.md`):
   - `docs/sprints/sprint-037-objective.md` + `docs/sprints/sprint-037-handoff.md` + `docs/sprints/sprint-037-codex-review.md` (design freeze; 10 decisions (a)-(j) locked; 5 Tier-0 candidates surfaced; Codex `pass/0`).
   - `docs/sprints/sprint-038-objective.md` + `docs/sprints/sprint-038-handoff.md` + `docs/sprints/sprint-038-fix-handoff.md` + `docs/sprints/sprint-038-codex-review.md` (SkillRegistry-core + 4 simpler Skills; B-fix-iterated; Sprint 38-fix completed `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` + `VALID_TOOL_NAMES` + `VALID_GUARDRAIL_TYPES` allowlists per §2.2 schema completeness).
   - `docs/sprints/sprint-039-objective.md` + `docs/sprints/sprint-039-handoff.md` + `docs/sprints/sprint-039-codex-review.md` (RESOLVE migration + predicate migration + S1/S2 + unified dispatcher; Codex `pass/0`).
   - `docs/sprints/sprint-040-objective.md` + `docs/sprints/sprint-040-handoff.md` + `docs/sprints/sprint-040-codex-review.md` (teaching extraction + envelope-mechanics; Codex `pass/0`).
   - `docs/sprints/sprint-041-objective.md` + `docs/sprints/sprint-041-handoff.md` + `docs/sprints/sprint-041-codex-review.md` (state-bus + state_inheritance + prior_use_case_carry; Codex `pass/0`).
4. **`docs/proposals/skill_registry_design.md`** — Sprint 37 freeze (immutable per `doc_governance.md`); the architectural authority for M2. Read end-to-end:
   - §1 — context + scope.
   - §2.1-§2.2 — Skill data model + schema.
   - §3.1-§3.3 — SkillRegistry + SkillLoader shape.
   - §4.1-§4.3 — PhaseEvaluator-as-Skill-Selector integration.
   - §5.1-§5.3 — procedure vs guardrails responsibility split.
   - §6.2.1-§6.2.6 — 6 phase YAML migration mapping (load-bearing for S38 + S39 verification).
   - §7.1-§7.8 — Sprint 23/31/33 teaching paragraph migration (load-bearing for S40 verification; OQ-S40.5 typo at §7.8).
   - §8.2.1-§8.2.5 — Sprint 6/7/11 + S1/S2 predicate migration (load-bearing for S39 verification).
   - §9.1-§9.4 — unified Skill terminal-predicate dispatcher (S39 surface).
   - §9.9 — C2 Tier-0 candidate question.
   - §10.1-§10.11 — session-level state model + state_inheritance + prior_use_case_carry (Sprint 41 surface).
   - §10.5 — UC-switching invariant matrix.
   - §10.10 — C3 Tier-0 candidate question.
   - §11.1-§11.10 — §4.1 nine-question walk on PROPOSED design.
   - §12 — 5 Tier-0 candidate enumeration (C1 REJECTED / C2 QUALIFIED-DEFER / C3 QUALIFIED-DEFER / C4 + C5 NOT A CANDIDATE).
5. **`docs/current/iteration_governance.md`** — §1 Constitution; §1.3 LLM-owned; §1.4 Runtime-owned; §1.7 Forbidden; §3.2 layer classification (Q3/Q4 for `prompt_projection` + `skill_state`); §4.1 nine-question kernel (Codex independently re-walks against the cumulative M2 diff at §3 below); §4.2 sprint-close header convention (this milestone-shared review writes per §4.2 at M2 close); §4.3 milestone-shared review provisions (this review is the operationalization); §5 acceptance bars; §5.5 smoke OBSERVATION; §5.6 bad-case suite primary-gate framing (M2 RECALIBRATED — OBSERVATION not gate); §7 sprint-objective stanza; §8 milestone framework.
6. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set at M2 close. Verify M2 dev commits across the cumulative range ADDED no Tier-0 invariant (M2 §6 #8 fence preserved across all 5 sub-sprints). **C3 elevation independent verdict at §8 below is the LOAD-BEARING M2 close decision.**
7. **`docs/current/faq_grounding_contract.md`** — grounding-floor contract. Verify M2 preserves the FAQ grounding contract; M2 §6 #4 bounded inversion (S1 `must_cite_source` predicate) ships in code at Sprint 39 per `SkillGuardrailDispatcher.handleMustCiteSource` (`SkillGuardrailDispatcher.java:340-384`) + `resolve_faq_grounded_answer.yaml:34-45`; verify bounded scope preserved.
8. **`docs/action_bank.md`** — R-item tracker; pay attention to:
   - Line 345 `D-skill-runtime-framework` row (M2 FULL LANDING annotation post-Sprint-41 close).
   - Line 348 `D-hard-citation-gate` row (M2 §6 #4 bounded inversion shipped at Sprint 39; general gate remains deferred).
   - §5.2 R-items `R-skill-guardrail-non-overridability-tier-0` (C2 candidate; load-bearing for M2 close re-evaluation) + `R-skill-state-bus-boundary-enforcement-tier-0` (C3 candidate; **LOAD-BEARING for M2 close re-evaluation** — Sprint 41 shipped FIRST observed evidence surface).
   - §5.2 `R-grounding-discipline-iterative-search-fabrication` (consumed by Sprint 39 S1 declaration).
   - §6 closed-action index Sprint 41 row (added at Sprint 41 close).
9. **`compact/sprint-037-review-prompt.md`** through **`compact/sprint-041-review-prompt.md`** — the 5 per-sub-sprint review prompts that drove the per-sub-sprint Codex reviews. Read each to cross-check the per-sub-sprint scope-discipline framing was internally consistent across the milestone.
10. **Code source files for cumulative cited-line spot-checking at HEAD `8cd0a10`** (read on demand during §3 + §5 + §8 verification; NOT end-to-end):
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/{Skill,Guardrail,StateInheritance,SkillRegistry,SkillLoader,SkillGuardrailDispatcher,SkillStateBus,RejectVerdict,DispatchContext}.java` (all M2 NEW or extensively-modified).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (post-M2 1482 lines; per-S38-S39-S40-S41 evolution).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (post-S39 638 lines; UNCHANGED since S39).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (post-S41 1341 lines; Sprint 31/33 projection slots PRESERVED + NEW prior_use_case_carry slot at S41).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (UNCHANGED at 222 lines across all M2; M1 functional surface preserved per M2 §6 #21 fence).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` (UNCHANGED at 205 lines; Sprint 11/11.1/12 frozen surface per `runtime_freeze_and_risk_policy.md` §1.1 #3).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (UNCHANGED at 174 lines).
    - `server/src/main/resources/skills/*.yaml` (6 files; all NEW or M2-EDITed).
    - `server/src/main/resources/prompts/system_prompt.txt` (post-S40 80 lines; UNCHANGED at S41).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` (UNCHANGED).
    - All M2 NEW test files: SkillTest + SkillRegistryTest + SkillLoaderTest + PhaseEvaluatorSkillIntegrationTest (S38) + PhaseEvaluatorResolveSkillIntegrationTest + SkillGuardrailDispatcherTest + ResolveFaqGuardrailsTest + ResolveIntakeGuardrailsTest (S39) + SkillTeachingMigrationIntegrationTest (S40) + SkillStateBusTest + PriorUseCaseCarryProjectionTest + UcSwitchStateInheritanceTest (S41).

## 2. Cumulative scope-discipline gate (BLOCKING)

Verify the M2 cumulative commit range `51c327c..8cd0a10` touches ONLY the surfaces enumerated in `docs/milestone_objective.md` §3 sub-sprint sequence + each per-sub-sprint contract §5 + the explicit M2-precedent extensions (S39+S40 ctor-update precedent extended to S41 33 files per Drift §7-a REAFFIRM):

```bash
git -C /Users/caoruixin/projects/csagent-latest diff 51c327c~..8cd0a10 --stat
git -C /Users/caoruixin/projects/csagent-latest log --oneline 51c327c~..8cd0a10
git -C /Users/caoruixin/projects/csagent-latest log 51c327c~..8cd0a10 --name-only
```

Expected commit count: **7** (`51c327c` + `649c6c7` + `bd9d3f5` + `5787806` + `2f412b6` + `9130abc` + `8cd0a10`).

Spot-check NO surfaces outside M2 contract were touched cumulatively:
- `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` — M2 §6 #5 fence; verify `git diff 51c327c~..8cd0a10 --name-only | grep -E "(RuntimeIntentClassifier|IntentClassification|DriftResult|DriftDetector|UseCaseRouter|ClassifyUseCaseTool)"` returns empty.
- `INTAKE_UCS` set + `escalation_reason` enum — M2 §6 #6 + #7 fences; verify `PhaseEvaluator.java:30` `INTAKE_UCS` + `EscalationReasonResolver.java` `CANONICAL_ESCALATION_REASONS` UNCHANGED via `git diff` on these lines.
- `docs/runtime_freeze_and_risk_policy.md` — M2 §6 #8 fence; verify `git diff 51c327c~..8cd0a10 -- docs/runtime_freeze_and_risk_policy.md` returns empty (NO new Tier-0 invariant added during M2; C2 + C3 candidate elevation deferred per per-sub-sprint Codex verdicts; final elevation independent verdict in §8 below).
- `IntakeFieldsRegistry.java` / `IntakeFieldExtractor.java` — M2 §6 #21 M1 functional-surface fence; verify UNCHANGED across `51c327c..8cd0a10`.
- `ResolveDispositionEvaluator.java` — Sprint 11/11.1/12 frozen surface per `runtime_freeze_and_risk_policy.md` §1.1 #3; verify UNCHANGED.
- Existing case families under `eval_interactive/case_specs/case_families/<existing>/` + shadow CaseSpecs under `eval_interactive/case_specs_shadow/case_families/<existing>/` + bad case suite at `eval_interactive/case_specs/bad_cases/` — M2 §6 cascade fence; verify NO M2 sub-sprint touched existing eval surfaces.
- `docs/current/*` + `docs/foundational/*` + `docs/proposals/*` (except the Sprint 37 design doc) + `docs/sprints/sprint-001-*` through `docs/sprints/sprint-036-*` archives — governance + archive fences; verify UNCHANGED.

**Deliver-agent-owned files** are excluded from the dev commits per `feedback_commit_at_end_bundles_deliver_artefacts.md`; the human bundles them at sub-sprint close + M2 close in SEPARATE commits.

A single surface outside M2 contract scope is a **BLOCKING scope-discipline finding**. Surface as M2 milestone-close blocker.

## 3. §4.1 Anti-Hardcode kernel — independent cumulative re-walk

Walk all 9 `iteration_governance.md` §4.1 questions against the **CUMULATIVE M2 diff** (`51c327c..8cd0a10`). Each per-sub-sprint Codex review (S37 / S38 / S38-fix / S39 / S40 / S41) already walked the kernel against the sub-sprint diff and returned `pass` per Q1-Q9. This milestone-shared walk verifies the cumulative pattern: no Q's cumulative diff introduces a hardcode that the per-sub-sprint walks individually missed.

1. **Q1** — keyword / regex / if-else / enum / per-UC matrix for a semantic decision? Cumulative verification: walk the post-M2 `SkillStateBus.java` body + `SkillGuardrailDispatcher.java` body + `SkillRegistry.java` `select(...)` body + `Skill.appliesTo(...)` body + all 6 Skill YAML `procedure` + `guardrails` + `state_inheritance` declarations + `ContextProjectionBuilder.buildPriorUseCaseCarryNode(...)` + `PhaseEvaluator.maybeApplyStateBusOnSwitch(...)` for any per-UC-pair branch table. Expected: ZERO per-UC-pair branches across M2; all dispatch is registry-driven (SkillRegistry exact-match → wildcard → null; `IntakeFieldsRegistry.requiredFieldsFor(...)` for intake field intersection; bus body uses switch over 3 allowlisted state keys; dispatcher routes by tool/outcome NOT by UC). Surface ANY cumulative pattern of `if (uc == "UC-X" && newUc == "UC-Y")` shape as Q1 BLOCKING finding.
2. **Q2** — Tier-0 justification? Verify cumulatively that M2 adds NO Tier-0 invariant to `docs/runtime_freeze_and_risk_policy.md` (empty diff across `51c327c..8cd0a10`). C2 + C3 elevation independent verdict at §8 below is the load-bearing decision.
3. **Q3** — soft signal achievable instead of hard branch? Verify M2 cumulatively prefers soft-signal projection slots where possible: Sprint 31 `alternate_candidate_use_cases` + Sprint 33 `discover_disambiguation_signals` + Sprint 41 `prior_use_case_carry` are all LLM-owned soft signals; deterministic enforcement is bounded to (a) S1 `must_cite_source` per M2 §6 #4 verbatim 3-boundary; (b) S2 `intake_complete_required` Sprint 7 §I2 semantics preserved; (c) `SkillStateBus.applyIntakeFieldsIntersection` per design doc §10.3 Alternative D REJECTED rationale.
4. **Q4** — visible-eval / trace phrasing / CaseSpec id encoded? Cumulative spot-check across `51c327c..8cd0a10` for `alice_*`, `cs_*`, source-session ids (`3772e56b-*` etc.), bad-case-suite text, visible-eval phrase. Verify NONE encoded in any M2 Java / YAML / test / prompt surface.
5. **Q5** — semantic ownership shift LLM → Java? Cumulatively verify §1.3 LLM ownership preserved (LLM continues to own UC hypothesis, drift, escalation posture, response strategy, customer-facing language); §1.4 Runtime ownership scoped correctly (the dispatcher + state-bus are capability-floor enforcement, NOT semantic-decision enforcement); the Sprint 40 teaching extraction is content-RELOCATION not ownership-SHIFT (LLM still observes the same teaching, just via Skill envelope projection instead of monolithic shell).
6. **Q6** — if-else block in prompt instead of principle-level guidance? Cumulative verify post-M2 `system_prompt.txt` (80 lines; UNCHANGED at S41) carries no per-UC-branch table; post-M2 6 Skill YAML `procedure` bodies use general principle-level language ("When ... is populated ...", "Confidence guidance: >= 0.7 ...", "Always ground your answer in retrieved knowledge ..."). No per-UC-pair if-else.
7. **Q7** — tool schema / capability / PII / grounding floor preserved? Cumulatively verify: tool schemas UNCHANGED (no `*Tool.java` edits across M2); capability/permission boundary preserved (`PhasePlan.allowedTools` still pulls from `skill.toolsRequired()` post-S38; `Skill.toolsRequired()` validated against `VALID_TOOL_NAMES` allowlist at SkillLoader); PII/safety floor unchanged; grounding floor unchanged (Sprint 14 `faq_grounding_contract.md` diagnostics preserved; M2 §6 #4 bounded inversion ships per S1 declaration).
8. **Q8** — generalization eval coverage? Java test coverage trajectory: 983 → 1144 (+161 across M2). Per layer: SkillRegistry-core (51); RESOLVE migration + S1/S2 + dispatcher (60); teaching extraction (11); state-bus + slot (39). Eval-suite coverage demoted to OBSERVATION per M2 §5 recalibration; bad-case suite (Alice) not gated. Cumulative coverage spans target / neighbor / negative for each per-sub-sprint scope. Shadow class N/A per M2 §5 recalibration. Independent assessment: is the Java test coverage adequate for a multi-layer architectural milestone of this scope?
9. **Q9** — rollback / sunset? Cumulative verify `git revert 51c327c..8cd0a10` would cleanly remove the entire Skill Registry abstraction back to pre-M2 state. The Skill Registry lands as permanent runtime infrastructure (no sunset plan; M2 milestone scope).

Output: per-Q cumulative verdict (`pass` / `concern` / `fail`); a single Q `fail` is a M2 milestone-close BLOCKING finding.

## 4. §1.7 Boundary check — cumulative

Walk each `iteration_governance.md` §1.7 forbidden item against the **CUMULATIVE M2 diff**:

- **(a) encoding raw eval phrases into Java or prompt** — cumulative spot-check NO Alice / bad-case-suite / source-session-id text encoded.
- **(b) adding UC-specific hard rules for soft semantic decisions** — cumulative verify dispatcher routes by tool/outcome (NOT by UC); state-bus body switches over allowlisted state keys (NOT per-UC-pair); per-Skill `state_inheritance` declarations are principle-level per §10.5 matrix.
- **(c) widening eval spec to accept a genuine bot mistake** — verify M2 ships NO `eval_interactive/` edits across the cumulative range.
- **(d) optimizing visible eval at the cost of shadow/generalization** — M2 §5 recalibration; verify M2 ships NO smoke composite_score change as primary evidence; verify NO bad-case-suite-programmatic-pass-rate framing used as gate.
- **(e) using prompt as an if-else rule dump** — cumulative verify post-M2 `system_prompt.txt` (80 lines) carries no per-UC-branch table; verify post-M2 `discover_triage.yaml` `procedure` (~7900 chars) carries no per-UC-pair table; verify all 6 Skill YAML procedures use principle-level language.

Output: per-item cumulative verdict.

## 5. M2 Hard-fence verification — cumulative

Verify each of the 22 M2 §6 hard fences cited in `docs/milestone_objective.md` §6 against the cumulative `51c327c..8cd0a10` range:

- **M2 §6 #1**: NO per-UC-branch if-else in any M2 Skill YAML procedure OR dispatcher OR state-bus body (verified in §3 Q1 above).
- **M2 §6 #4**: S1 `must_cite_source` bounded per verbatim 3-boundary check (phase=RESOLVE_FAQ + tool=`record_outcome(class=resolve)` + check=`source_id` citation presence ONLY); verify `SkillGuardrailDispatcher.handleMustCiteSource` lines 340-384 + `resolve_faq_grounded_answer.yaml:34-45` match the verbatim authorization.
- **M2 §6 #5**: NO touch to `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` (cumulative `git diff` returns empty for these files).
- **M2 §6 #6 + #7**: NO edit to `INTAKE_UCS` set; NO widening of `escalation_reason` enum (verify cumulatively).
- **M2 §6 #8**: NO new Tier-0 invariant (cumulative `git diff 51c327c~..8cd0a10 -- docs/runtime_freeze_and_risk_policy.md` returns empty). C2 + C3 elevation independent verdict at §8 below.
- **M2 §6 #20**: NO OLD design deletion (Sprint 11 / 11.1 / 12 frozen surface stays; `ResolveDispositionEvaluator.java` UNCHANGED at 205 lines).
- **M2 §6 #21**: M1 functional surface preserved cumulatively (`IntakeFieldsRegistry.java` 222 + `IntakeFieldExtractor.java` + `UseCaseRegistryService.java` 174 all UNCHANGED; `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED across M2).
- **M2 cascade fence**: NO edits to existing case families under `eval_interactive/case_specs/case_families/<existing>/` OR shadow CaseSpecs OR bad case suite (verify cumulatively).

Cross-check each per-sub-sprint §6 fence (S37 19 items + S38 31 items + S38-fix 7 items + S39 37 items + S40 33 items + S41 33 items) against M2 §6 — all per-sub-sprint fences should be subsumed by or consistent with M2 §6.

Output: per-fence cumulative verdict.

## 6. OQ disposition rollup — cumulative

Each per-sub-sprint Codex review independently classified its sub-sprint's OQs. This M2-shared review verifies the cumulative OQ state at M2 close:

- **Sprint 37 close OQs** (5): OQ-7.2 + OQ-7.3 C2/C3 DEFER (rolled into §8 below for M2-close re-evaluation); OQ-7.4 §12-only sufficient; OQ-7.10 supersession applied; OQ-7.11 `request_handover` decision tree NOT per-UC-pair (Codex confirmed). 6 downstream-planning OQs (OQ-7.1/5/6/7/8/9) consumed at S38/S39/S40/S41 planning rounds.
- **Sprint 38 + Sprint 38-fix OQs** (5 main + 4 fix): OQ-S38.1 template-vs-legacy editorial divergence on DISCOVER (deferred to M2 close fold-back per Sprint 38 OQ-S38.1 precedent — surface in §7 below); OQ-S38.2/3/4 informational; OQ-S38.5 C2 + C3 R-items continuation CONFIRM. Fix-iteration OQ-FIX1.1 R-item opened (`R-skill-tool-name-canonical-source-centralization`); OQ-FIX1.2/3/4 informational.
- **Sprint 39 OQs** (7): OQ-S39.5 C2 dispatcher Tier-0 elevation Codex DEFER (rolled into §8 below); OQ-S39.1/2/3/4 AGREE WITH DEV; OQ-S39.6 `on_fail: downgrade_reason` deferred S40+; OQ-S39.7 stale Sprint 38 test name `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` deferred to M2 close housekeeping (surface in §7 below).
- **Sprint 40 OQs** (5): OQ-S40.1 5th-file inclusion AGREE WITH DEV per Sprint 39 precedent; OQ-S40.2 envelope-mechanics + DISCOVER pointer combined AGREE WITH DEV; OQ-S40.3 inherited test STATUS QUO; OQ-S40.4 Sprint 23 re-wording declined AGREE WITH DEV; OQ-S40.5 design doc §7.8 Sprint 31/33 line-number swap typo ROUTE TO M2 CLOSE FOLD-BACK (surface in §7 below).
- **Sprint 41 OQs** (6): OQ-S41.1 D-g shell teaching SKIPPED DISAGREE IN PART NON-BLOCKING — routes to M2 close housekeeping one-line shell pointer (surface in §7 below); OQ-S41.2 C3 Tier-0 elevation Codex DEFER (rolled into §8 below for M2-close re-evaluation); OQ-S41.3 33-file ctor null arg style ACCEPTABLE STYLE; OQ-S41.4 `accumulated_tool_results` inherit-all vs §10.5 row 1 wording FLAG AS DESIGN-DOC EDITORIAL DIVERGENCE — routes to M2 close fold-back (surface in §7 below); OQ-S41.5 inherited test STATUS QUO; OQ-S41.6 template-vs-implementation divergence MINOR (= OQ-S41.4).

**Codex M2-close verdict required:** confirm no OQ disposition was incorrectly classified at sub-sprint close (cumulative re-check); surface any cumulative-evidence-changed-since-sub-sprint-close findings.

## 7. Design-doc editorial fold-back queue — M2-close routing

The following 6 items are queued for SEPARATE governance commit at M2 close per `doc_governance.md` cadence + `feedback_commit_at_end_bundles_deliver_artefacts.md` (deliver-agent + human commit the fold-back AFTER M2-shared Codex review returns; NOT in any sub-sprint dev commit):

1. **Sprint 37 design doc §7.8 Sprint 31/33 line-number swap typo** (OQ-S40.5; confirmed by Codex at `docs/proposals/skill_registry_design.md:2015`; dev implemented the CORRECT mapping — Sprint 33 from lines 36-43; Sprint 31 from lines 30-34).
2. **Sprint 38 OQ-S38.1 template-vs-legacy editorial divergence** on DISCOVER Skill body (`discover_triage.yaml` carries legacy `PhaseEvaluator.java:397-542` content verbatim; design doc §6.2.1 template has editorial paraphrasing; resolved per `doc_governance.md` code-ahead-of-docs cadence).
3. **Sprint 39 §10 design doc §6.2.5 + §6.2.6 RESOLVE template editorial divergence** (RESOLVE Skill YAMLs carry legacy `PhaseEvaluator.java` RESOLVE branch content verbatim; design doc templates have editorial paraphrasing).
4. **Sprint 39 OQ-S39.7 stale Sprint 38 test name** `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` (was forward-looking placeholder when Sprint 38 shipped; Sprint 39 migration consumed the surface; test name no longer reflects content).
5. **NEW Sprint 41 OQ-S41.1 one-line shell pointer for `prior_use_case_carry`** (per Codex DISAGREE IN PART NON-BLOCKING verdict; dev's D-g skip rationale was partly code-untrue — `phase_plan` projection does NOT currently expose `state_inheritance.soft_signal_via_projection` to the LLM; adding a brief one-line pointer in `system_prompt.txt` would close the gap without requiring a full teaching paragraph).
6. **NEW Sprint 41 OQ-S41.4 `accumulated_tool_results` inheritance wording** (design doc §10.5 row 1 reads "All resolve-side Skills" but Sprint 41 implementation declares inherit on all 6 Skills; runtime effect is no-op; fold-back resolves the wording — either narrow the implementation OR widen the design doc text).

**Codex M2-close verdict required:** (a) confirm each fold-back item is routed correctly (SEPARATE governance commit at M2 close); (b) flag any cumulative-evidence-changed-since-sub-sprint-close item; (c) surface any NEW fold-back item the milestone-shared review independently uncovers.

## 8. Tier-0 candidate independent verification — LOAD-BEARING for M2 close

Sprint 37 design freeze §12 surfaced 5 Tier-0 candidates with per-sub-sprint Codex verdicts:

- **C1** REJECTED preserved by construction (tool-whitelist enforcement via `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`; verify the M2 cumulative range PRESERVES this — Sprint 38 SkillLoader added `VALID_TOOL_NAMES` allowlist which is downstream of (not replacement for) the dispatcher invariant; Sprint 38-fix tightened this).
- **C2 (guardrail refusal non-overridability)** QUALIFIED-DEFER per Sprint 37 design freeze + Sprint 38/38-fix/39/40/41 per-sub-sprint Codex re-confirmations. Sprint 39 shipped FIRST observed evidence surface (`SkillGuardrailDispatcher` short-circuit-on-first-reject + 24 `SkillGuardrailDispatcherTest` unit tests covering 4 handler types × fire/no-fire × short-circuit). Sprint 40 + Sprint 41 added no new C2 evidence (S40 content-relocation only; S41 state-bus surface — neither is dispatcher-relevant). **M2-close LOAD-BEARING re-evaluation for C2**: with full M2 evidence (Sprint 39 dispatcher landing + 4 handlers fully wired across S1/S2/G2/M1 predicates), is C2 elevation NOW warranted? Codex independent verdict required: (a) DEFER continued (Sprint 39 deterministic Java tests necessary but not sufficient production evidence; observe through M3+ window); (b) ELEVATE NOW (cumulative milestone evidence + 4-handler integration crosses the threshold from "structural evidence" to "operational Tier-0 invariant"); (c) NOT A CANDIDATE re-classification. If (b), classify as `out_of_scope_review` (NOT `fix_required`) — Tier-0 elevation is deliver-agent + human authority per `feedback_constitution_discipline_vs_planning_anticipation.md`; deliver-agent + human authorize the `runtime_freeze_and_risk_policy.md` §1.1 edit as SEPARATE governance commit.
- **C3 (state-bus boundary enforcement)** QUALIFIED-DEFER per Sprint 37 design freeze + Sprint 41 per-sub-sprint Codex re-confirmation. **Sprint 41 shipped FIRST observed evidence surface** (`SkillStateBus.applyOnSkillSwitch` enforcement at `SkillStateBus.java:85` + integration at `PhaseEvaluator.java:363` + all 6 YAML declarations per §10.5 invariant matrix + 17 unit + 9 integration tests demonstrating LLM CANNOT override declared `state_inheritance`). Sprint 41 Codex independent verdict at `docs/sprints/sprint-041-codex-review.md` "Tier-0 Candidate Independent Verification" C3 row: "Sprint 41 provides strong structural evidence... That is necessary and compelling, but I do not elevate C3 now because the Tier-0 claim is operationally about the LLM having no production path to override state inheritance across real traces; observe through Sprint 41/M2 close/M3+ before freezing it." **M2-close LOAD-BEARING re-evaluation for C3**: with full M2 evidence (Sprint 41 state-bus + 39 tests + integration at the single boundary), is C3 elevation NOW warranted at M2 close? Codex independent verdict required: (a) DEFER continued (deliver-agent + human pre-decision at Sprint 41 close was DEFER; M2-shared view is one step closer to production trace observation but still pre-M3+); (b) ELEVATE NOW; (c) NOT A CANDIDATE re-classification. Same routing as C2 if (b): classify as `out_of_scope_review` and surface evidence.
- **C4** + **C5** NOT A CANDIDATE preserved across all 5 per-sub-sprint Codex reviews; cumulative re-confirmation expected.

**Codex M2-close verdict required for C2 + C3 specifically**: for each, name your verdict (a/b/c) + cite specific cumulative evidence + name the Tier-0-claim shape (e.g., for C2: "Skill terminal predicate refusal is non-overridable by LLM"; for C3: "Skill-declared `state_inheritance` is enforced at session-state-bus boundary; LLM has no production path to override"). For C1, C4, C5: REAFFIRM (default) OR raise alternative.

If your verdict on C2 OR C3 is ELEVATE NOW, this is the SINGLE most load-bearing M2 milestone-close finding; surface evidence + rationale + propose specific `runtime_freeze_and_risk_policy.md` §1.1 invariant text.

## 9. Schema and reproducibility checks — cumulative

Verify the M2 cumulative reproducibility per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`:

- Every line-count claim in the per-sub-sprint handoffs cites a file path. Spot-check 10-15 cumulative claims via `wc -l <path>` at HEAD `8cd0a10`.
- Every commit-shape claim is reproducible via `git show --stat <commit>` and `git diff <range> --numstat`.
- Every "X UNCHANGED" claim cumulatively is reproducible via `git diff 51c327c~..8cd0a10 -- <path>` returning empty.
- **Cumulative numbers-cite discipline check**: per-sub-sprint reviews surfaced 3+ deliver-agent prompt numbers-cite drift instances (Sprint 39 + Sprint 40 + Sprint 41). This M2-shared review must not introduce additional numbers-cite drift; derive every numstat from `git show --numstat <commit>` OR `git diff 51c327c~..8cd0a10 --stat` directly.

Surface any non-reproducible claim as a finding (informational unless it changes the verdict).

## 10. Validation runs — cumulative

Run from a clean working tree:

```bash
cd /Users/caoruixin/projects/csagent-latest/server
mvn clean test -q 2>&1 | tail -25
```

**Expected verdict for M2 close:**
- `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2`.
- The sole failure is the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` (persisted since Sprint 24-era working-tree mod; NOT M2-attributable).
- Java baseline trajectory across M2: 983 → 1028 → 1034 → 1094 → 1105 → 1144 (+161 net across M2).

Targeted cumulative validation runs:

```bash
# Skill abstraction (S38)
mvn -pl server test -Dtest=SkillTest,SkillRegistryTest,SkillLoaderTest,PhaseEvaluatorSkillIntegrationTest -q

# RESOLVE migration + S1/S2 + dispatcher (S39)
mvn -pl server test -Dtest=PhaseEvaluatorResolveSkillIntegrationTest,SkillGuardrailDispatcherTest,ResolveFaqGuardrailsTest,ResolveIntakeGuardrailsTest -q

# Teaching extraction (S40)
mvn -pl server test -Dtest=SkillTeachingMigrationIntegrationTest -q

# State-bus + slot (S41)
mvn -pl server test -Dtest=SkillStateBusTest,PriorUseCaseCarryProjectionTest,UcSwitchStateInheritanceTest -q

# M1 functional surface preservation
mvn -pl server test -Dtest=Sprint71PartialIntakePersistenceTest -q

# Sprint 31/33 projection preservation
mvn -pl server test -Dtest=Sprint7CandidateUseCasesProjectionTest,DiscoverDisambiguationSignalsProjectionTest -q

# Sprint 11/11.1/12 frozen surface (ResolveDispositionEvaluator delegation)
mvn -pl server test -Dtest=Sprint11ProgressiveResolveTest,Sprint12RuntimeAlignmentValidationTest -q

# Inherited baseline failure (unchanged)
mvn -pl server test -Dtest=SystemPromptUserRequestedTiebreakerTest -q
```

Surface any unexpected result.

## 11. Output format — §4.2 sprint-close header

Write `docs/codex-findings.md` per the scaffold. Top of the file:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph — Codex's verdict on M2 milestone close>
```

Then the per-section structure (Review Evidence, Blocking Findings, Anti-Hardcode Kernel (Cumulative), §1.7 Boundary Check (Cumulative), Hard-Fence Verification (Cumulative), Schema And Reproducibility Checks, Validation Runs, **Tier-0 Candidate Independent Verification (LOAD-BEARING for M2 close — C2 + C3 elevation verdicts)**, **OQ Disposition Rollup**, **Design-Doc Editorial Fold-Back Queue**, Deferred / Non-Blocking Notes).

A single blocking finding → `fix_required` (`blocking_count: 1+`; deliver-agent + human address as M2-close fix-iteration sub-sprint OR add to M3 scope). Zero blocking findings → `pass` (`blocking_count: 0`; deliver-agent + human proceed to M2 close housekeeping per `iteration_governance.md` §8.4 — milestone archive + 10-handoff refresh + M3 planning). Any verdict that would expand M2 scope or invent a new architectural surface (including ELEVATE NOW on C2 or C3 per §8) → `out_of_scope_review` (deliver-agent + human re-direct; Tier-0 elevation is human authority; M3 candidate selection is planning-round decision).

## 12. Anti-patterns to refuse

- Do NOT re-import `iteration_governance.md` §5.6 bad-case-suite-primary default at M2 close (M2 §5 recalibration governs for THIS milestone; bad-case suite = OBSERVATION).
- Do NOT extrapolate scope from cumulative findings beyond what each sub-sprint contract authorized (each sub-sprint had its own §5 + §6; M2 close summarizes — does not reopen — sub-sprint scope decisions).
- Do NOT silently elevate C2 OR C3 to Tier-0 in your verdict (Tier-0 elevation is deliver-agent + human authority per `feedback_constitution_discipline_vs_planning_anticipation.md`; if your evidence-based verdict is ELEVATE NOW, route as `out_of_scope_review` and surface the specific evidence + rationale + propose `runtime_freeze_and_risk_policy.md` §1.1 invariant text — deliver-agent + human authorize the actual edit as SEPARATE governance commit at M2 close OR M3 planning round).
- Do NOT propose code fix beyond naming the layer per §3.2 of `iteration_governance.md`.
- Do NOT propose edits to `docs/runtime_freeze_and_risk_policy.md`, `docs/proposals/skill_registry_design.md`, `docs/milestone_objective.md`, OR any `docs/sprints/sprint-NNN-*.md` archive (these are deliver-agent / human authority — out-of-scope for review).
- Do NOT rewrite any of the 5 per-sub-sprint dev handoffs (`sprint-037-handoff.md` through `sprint-041-handoff.md`); they are immutable archives.
- Do NOT introduce new Tier-0 candidates beyond C1-C5 (the Sprint 37 surface is closed; if new structural evidence surfaces independently in your M2 review, surface as `out_of_scope_review` and propose for M3+ design freeze discussion).
- Do NOT make M3 scope decisions in this M2-close review (M3 candidate selection is a separate planning round AFTER M2 close housekeeping; this review is M2 close only).

---

End of M2 milestone-shared review prompt. Return your verdict in `docs/codex-findings.md` per §11 above.
