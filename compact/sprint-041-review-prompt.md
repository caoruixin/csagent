Paste the content below this line into a fresh Codex session after the Sprint 41 dev commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for Sprint 41 — the FIFTH and **LAST** sub-sprint of **NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization, LLM-led Policy-bounded)** per `docs/milestone_objective.md`. Sprint 37 (M2 sub-sprint 1 design freeze) closed **A — Clean PASS** 2026-05-17 at commit `51c327c`. Sprint 38 (M2 sub-sprint 2 SkillRegistry-core + 4 simpler phase Skills migration) closed **B-fix-iterated** 2026-05-17 across main-dev commit `bd9d3f5` + fix iteration #1 commit `5787806`. Sprint 39 (M2 sub-sprint 3 RESOLVE Skill migration + predicate migration + S1/S2 + unified dispatcher) closed **A — Clean PASS** 2026-05-18 at commit `2f412b6`. Sprint 40 (M2 sub-sprint 4 teaching extraction from system_prompt.txt) closed **A — Clean PASS** 2026-05-18 at commit `9130abc` per `docs/sprints/sprint-040-codex-review.md` (Codex `pass / blocking_count: 0` on first pass, single round).

Sprint 41 is the **architecturally most distinct** M2 implementation sub-sprint — multi-fence convergence shape similar to Sprint 39: NEW Spring `@Component` class + 6 Skill YAML EDITs + NEW projection slot + `PhaseEvaluator` integration. Sprint 41 implements **Sprint 37 freeze decision (i) §10** as runtime code: session-level `SkillStateBus.java` mediates state across Skill boundaries on UC switch; per-Skill `state_inheritance` declarations on all 6 Skill YAMLs name what state the Skill inherits, resets, or sees as soft signal; NEW `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` surfaces continuity to the LLM as soft signal.

The dev landed single commit `8cd0a10` (parent `9130abc`) with 46 files per `git show --stat 8cd0a10`:

- **NEW** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`: 203 lines (per numstat `203/0`). Spring `@Component`; public methods `applyOnSkillSwitch(Skill priorSkill, Skill newSkill, BotSession session)` + `inheritanceFor(Skill skill)` per design doc §10.3. Registry-driven `applyIntakeFieldsIntersection(BotSession)` private method (no per-UC-pair branch). Defensive no-ops on null inputs, same-Skill turns, null intake_fields.
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`: 1161 → 1341 lines (+180; per numstat `182/2`). NEW `prior_use_case_carry` projection slot at the `discover_disambiguation_signals` sibling emission site. NEW private helper `buildPriorUseCaseCarryNode(BotSession, List<BotTurn>)`. Imports `JsonNode`/`NullNode`/`Skill`/`SkillRegistry`. NEW constants `PRIOR_USE_CASE_CARRY_AGING_TURNS = 4` + `PRIOR_USE_CASE_CARRY_CITATION_CAP = 3`. Ctor extended (+1 arg `SkillRegistry`).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`: 1427 → 1482 lines (+55; per numstat `59/3`). NEW `maybeApplyStateBusOnSwitch(History, BotSession, Skill newSkill)` private helper. Invocation inserted in `plan(...)` between `SkillRegistry.select` and `composeSkillPhasePlan`. Ctor extended (+1 arg `SkillStateBus`). Imports `SkillStateBus`.
- **EDIT** × 6 Skill YAMLs at `server/src/main/resources/skills/`:
  - `discover_triage.yaml`: 30 → 32 (+2; per numstat `4/1`). `state_inheritance` extended per §10.5 row 1.
  - `confirm.yaml`: 29 → 30 (+1; per numstat `1/0`). `state_inheritance` per §10.5 row 2.
  - `resolve_faq_grounded_answer.yaml`: 52 → 53 (+1; per numstat `3/1`). `state_inheritance` extended (Sprint 39 forward-compat placeholder consumed) per §10.5 row 3.
  - `resolve_intake_collect_and_handover.yaml`: 37 → 38 (+1; per numstat `1/0`). `state_inheritance` extended per §10.5 row 4.
  - `escalate.yaml`: 28 → 29 (+1; per numstat `3/1`). `state_inheritance` per §10.5 row 5.
  - `terminal.yaml`: 25 → 26 (+1; per numstat `3/1`). `state_inheritance` per §10.5 row 6.
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillStateBusTest.java`: 306 lines (per numstat `306/0`); 17 unit tests.
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/PriorUseCaseCarryProjectionTest.java`: 279 lines (per numstat `279/0`); 13 unit tests.
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/UcSwitchStateInheritanceTest.java`: 246 lines (per numstat `246/0`); 9 integration tests.
- **EDIT** × 33 existing test files at `server/src/test/java/com/gumtree/csagent/service/runtime/[...]`: ctor-signature updates only (passing `null` as the NEW dependency arg — `SkillStateBus` for `PhaseEvaluator`, `SkillRegistry` for `ContextProjectionBuilder`). Most diffs are `2/1` (single ctor site); 1 file `6/3` (Sprint11ProgressiveResolveTest, 3 ctor sites); 2 files `4/2` (Sprint7CandidateUseCasesProjectionTest + Sprint7IntakeStateTest, 2 ctor sites each). Defensive null-checks in `maybeApplyStateBusOnSwitch` + `buildPriorUseCaseCarryNode` preserve behavioural equivalence. **NOTE — numbers-cite drift to verify:** dev handoff §4 D-f frames this as "20 PhaseEvaluator + 16 ContextProjectionBuilder = 36 existing tests". Reality per `git show --numstat 8cd0a10`: 33 distinct files. The "20 + 16 = 36" may be a count of ctor call-sites (37 by enumeration); the "36" framing is therefore ambiguous (call-sites vs files). See drift item §7-d below.
- **NEW** `docs/sprints/sprint-041-handoff.md`: 414-line 12-section dev archive.

Total per `git show --stat 8cd0a10`: **46 files; +1734 / −44**.

Per `iteration_governance.md` §4.3, Sprint 41 fires per-sub-sprint Codex review per **trigger #3** (session state model touch across Skill boundary — load-bearing for cross-Skill state preservation; AND C3 Tier-0 candidate evidence surface — the `SkillStateBus.applyOnSkillSwitch` enforcement is the FIRST observed evidence surface for the C3 candidate from Sprint 37 §12). Sprint 37 + Sprint 38 + Sprint 38-fix + Sprint 39 + Sprint 40 all received per-sub-sprint Codex review; Sprint 41 follows the M2 implementation-sub-sprint cadence; Sprint 41 is the LAST per-sub-sprint review before M2 milestone close planning round.

**Sprint 37 freeze decision (i) §10 — what Sprint 41 implements (the in-scope subset, §10.1-§10.11 row-by-row):**

- **(§10.1 Decision statement)** `SkillStateBus.java` created at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`. Per-Skill `state_inheritance` declarations on all 6 Skill YAMLs. UC-switch detection rides on existing M1 Sprint 32 + Sprint 33 projections (no new classifier; M2 §6 #5 fence preserved).
- **(§10.2 schema)** 3 state keys (`customer_context`, `accumulated_tool_results`, `intake_fields_partial`) + 3 projection slots (`alternate_candidate_use_cases`, `discover_disambiguation_signals`, `prior_use_case_carry`) — enforced at `SkillLoader.java:53-69` allowlists (wired in Sprint 38-fix). `SkillLoader.validate(...)` at lines 262-280 exercises both allowlists.
- **(§10.3 SkillStateBus semantics)** `applyOnSkillSwitch(Skill, Skill, BotSession)` + `inheritanceFor(Skill)` public methods. Registry-driven `applyIntakeFieldsIntersection` per design doc Java code at lines 2773-2792 + pseudo-code at 2799-2806. Skill-switch detection in `PhaseEvaluator.maybeApplyStateBusOnSwitch(...)` per design doc trigger conditions at lines 2812-2818.
- **(§10.4 NEW prior_use_case_carry slot)** Slot shape matches design doc example at lines 2828-2840 verbatim (`prior_citations[]` of `{source_id, from_use_case, turn_index}` + `prior_active_use_case` + `prior_skill_name` (or null) + `ages_out_after_turns: 4`). Citation cap 3 + aging window 4 per §10.4 + OLD Sprint 36 OQ 7.7. Null on no-prior-switch / aged-out / empty-history per §10.4 bullets at lines 2846-2851.
- **(§10.5 UC-switching invariant matrix)** 5 dimensions × 5 outcomes mapped to per-Skill declarations:
  - Row 1: `customer_context` always survives → all 6 inherit.
  - Row 2: `accumulated_tool_results` survives ("All resolve-side Skills" per design doc reading; dev extended to all 6 — see OQ-S41.4 below for §10.5 row 1 reading divergence).
  - Row 3: `intake_fields_partial` survives only for INTAKE-flow continuation → `confirm` + `resolve_intake_collect_and_handover` inherit (with registry intersection); other 4 reset.
  - Row 4: `prior_use_case_carry` surfaces on resolve-side → `resolve_faq_grounded_answer` + `resolve_intake_collect_and_handover` declare it.
  - Row 5: Skill terminal predicate state intentionally NOT declared (per-dispatch, not session-state).
- **(§10.6 Rationale)** declarative per-Skill posture (testable, reviewable); narrow schema; registry-driven intersection; soft-signal projection mirror of Sprint 31/33 pattern; UC switch on existing surfaces; SkillStateBus in `service/runtime/skill/` parallels other M2 NEW classes.
- **(§10.7 Alternatives REJECTED)**:
  - A REJECTED (monolithic "all state survives") — verify `intake_fields_partial` is reset for non-INTAKE Skills via registry intersection.
  - B REJECTED (per-UC-pair tables) — verify bus body has ZERO per-UC-pair branches; `applyIntakeFieldsIntersection` is a SINGLE registry lookup.
  - C REJECTED (Runtime-floor on declarations) — verify bus applies declarations as written; no over-constraint.
  - D REJECTED (pure-projection design) — verify hybrid: `intake_fields_partial` deterministic enforcement in bus; soft signals via projection.
  - E REJECTED (Java method per Skill) — verify declarations are YAML-side, schema-validated at load.
- **(§10.8 §1.7 boundary check)** principle-level per-Skill declarations; registry-driven bus body; registry-driven slot construction with SINGLE integer constants (4 aging + 3 cap).
- **(§10.9 §1.3 / §1.4 boundary check)** LLM owns `prior_use_case_carry` read; bus is capability-floor persistence enforcement; no semantic decision moved to Java.
- **(§10.10 Tier-0 candidate question)** C3 candidate evidence surfaces (the bus's `applyOnSkillSwitch` enforcement; the unit + integration tests demonstrating LLM CANNOT override `state_inheritance` declarations). Dev does NOT pre-elevate per `feedback_constitution_discipline_vs_planning_anticipation.md`. **Codex's independent verdict on C3 elevation timing is REQUIRED — see §8 below; this is the LOAD-BEARING OQ for Sprint 41 close.**
- **(§10.11 Downstream sub-sprint reference)** all 6 artefacts shipped: `Skill.java` `state_inheritance` field (verified, no edit); 6 Skill YAMLs (EDITed); `SkillStateBus.java` (NEW); `ContextProjectionBuilder` prior_use_case_carry slot (EDITed); `PhaseEvaluator.java` integration (EDITed); unit + integration tests (NEW × 3 + EDITed × 33 for ctor signature).

**Sprint 37 freeze decisions Sprint 41 does NOT implement (out-of-scope; preserved for downstream M3):**

- **(h §9.9) C2 Tier-0 candidate elevation** — Sprint 39 dispatcher landing surfaced FIRST observed C2 evidence; Sprint 41 ships nothing new C2-relevant (no dispatcher edits). C2 DEFER continues.
- **(g §8.2.x) any Sprint 6/7/11/S1/S2 predicate change** — Sprint 39 landing preserved. `SkillGuardrailDispatcher.java` UNCHANGED at 416 lines. `ResolveDispositionEvaluator.java` UNCHANGED at 205 lines.
- **D-g optional orchestration-shell teaching** — SKIPPED per dev judgement (see OQ-S41.1 below). `system_prompt.txt` UNCHANGED at 80 lines from Sprint 40.

**M2 §5 acceptance recalibration governs Sprint 41 close** per `docs/milestone_objective.md` §5 (2026-05-17): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION ONLY (NOT hard gate) for THIS milestone; primary gate = functional review + Java tests pass + Sprint 37 freeze decisions honored across implementation sub-sprints + per-sub-sprint Codex review verdict `approve`. **Codex's review prompt MUST NOT re-import the `iteration_governance.md` §5.6 bad-case-suite-primary default for Sprint 41**; per-Q walk + behavioural-equivalence verification + Sprint 37 freeze §10 fidelity + Sprint 11/11.1/12 frozen surface preservation + M1 functional-surface preservation + Sprint 38/39/40 surfaces preservation + Java baseline preservation are the load-bearing surfaces. Same recalibration Sprint 37/38/38-fix/39/40 honored.

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip; the prompt's later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / §4.3 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — the NEW M2 milestone north star. Especially: §3 Sprint 41 row; §5 acceptance recalibration (Alice + eval = OBSERVATION, NOT gate); §6 hard fences (22 items; esp. #1 no per-UC-branch if-else, #5 no `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` touch, #6 INTAKE_UCS unchanged, #7 escalation_reason enum unchanged, #8 no new Tier-0 invariant without human review, #20 no OLD design deletion, #21 M1 functional-surface preservation); §11 cross-milestone sequencing (M3-A / M3-B / M3-C / M3-D / M3-Latency / M3-Skill-Tuning).
3. **`docs/sprint_objective.md`** — the Sprint 41 contract (~460 lines). Read: §1 sub-sprint class (`skill_state` + `prompt_projection`; semantic-touching multi-layer; §7 stanza REQUIRED); §2 Goal — 7 deliverables D-a through D-f + 1 optional D-g; §3 Non-goals (explicit list including no M3-A/B/D scope, no Tier-0 elevation, no M3 pre-decisions); §4 Premise check (15 items dev re-verified at session start per handoff §3); §5 Files in scope (~10-15 files); §6 Hard fences (33 items); §7 Bundle policy; §8 §7 stanza (`skill_state` + `prompt_projection`; no Tier-0; no semantic hardcode; full generalization coverage); §9 Success metrics (HARD GATES + observations; Java baseline target ~1140-1155); §10 Stop conditions (20 items); §11 Handoff §11 12-section contract; §12 M2 milestone context (LAST M2 sub-sprint; M2 close is next planning round).
4. **`docs/sprints/sprint-041-handoff.md`** — the dev's archive (414 lines). Compare §9 (Files changed) against §5 of the objective; verify no scope creep beyond what OQ-S41.3 + drift items §7 surface. §1 Context Pack + §2 sub-sprint-objective recap. §3 walks the 15 §4 premises (15/15 PASS). §4 implementation walkthrough across D-a/b/c/d/e/f + D-g SKIPPED. §5 Sprint 37 freeze §10 fidelity row-by-row mapping (§10.1-§10.11). §6 §4.1 self-walk verdict `approve` per Q1-Q9. §7 6 OQs (OQ-S41.1 through OQ-S41.6 — surface for INDEPENDENT verdict per §6 below). §8 anti-hardcode rollup (cross-references §6). §9 files changed table. §10 layer-classification self-walk. §11 §5 acceptance bars adapted per M2 §5 recalibration. §12 closure verdict placeholder (left for deliver-agent + human + Codex per `feedback_handoff_verdict_section_delegation.md`).
5. **`docs/proposals/skill_registry_design.md`** — Sprint 37 freeze (immutable per `doc_governance.md`); architectural authority for Sprint 41. Read sections referenced by Sprint 41 in scope:
   - **§10.1** decision statement.
   - **§10.2** `state_inheritance` declaration schema.
   - **§10.3** SkillStateBus semantics + Java code block (lines 2773-2792) + intersection pseudo-code (lines 2799-2806) + trigger conditions (lines 2812-2818).
   - **§10.4** NEW `prior_use_case_carry` projection slot + example (lines 2828-2840) + null cases (lines 2846-2851).
   - **§10.5** UC-switching invariant matrix (5 dimensions × 5 outcomes).
   - **§10.6** Rationale.
   - **§10.7** Alternatives A/B/C/D/E (all REJECTED).
   - **§10.8** §1.7 boundary check.
   - **§10.9** §1.3 / §1.4 boundary check.
   - **§10.10** Tier-0 candidate question (C3 candidate; QUALIFIED-DEFER at design freeze; Sprint 41 ships FIRST observed evidence surface; deliver-agent + human + Codex jointly classify elevation at sprint close OR M2 close).
   - **§10.11** Downstream sub-sprint reference (lines 2971-2982).
6. **`docs/sprints/sprint-040-objective.md`** + **`docs/sprints/sprint-040-handoff.md`** + **`docs/sprints/sprint-040-codex-review.md`** — Sprint 40 archives. Read for the Sprint 40 baseline (1105 / 1 inherited / 0 / 2 Java tests Sprint 41 inherits; Sprint 40 surfaces UNCHANGED in Sprint 41).
7. **`docs/sprints/sprint-039-objective.md`** + **`docs/sprints/sprint-039-handoff.md`** + **`docs/sprints/sprint-039-codex-review.md`** — Sprint 39 archives. Read for the RESOLVE Skill landing baseline + the forward-compat `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` placeholder Sprint 41 consumes; Sprint 39 unified dispatcher surface preserved in Sprint 41.
8. **`docs/sprints/sprint-038-objective.md`** + **`docs/sprints/sprint-038-handoff.md`** + **`docs/sprints/sprint-038-fix-handoff.md`** + **`docs/sprints/sprint-038-codex-review.md`** — Sprint 38 archives. Read for the SkillRegistry-core landing baseline; `SkillLoader.java` `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists Sprint 41 consumes (no schema drift surfaced; no Sprint 41 edit to `SkillLoader.java`).
9. **`docs/sprints/sprint-037-objective.md`** + **`docs/sprints/sprint-037-handoff.md`** + **`docs/sprints/sprint-037-codex-review.md`** — Sprint 37 design freeze archives. Read for the freeze decision pre-text + Tier-0 candidate verdicts (C1 REJECTED preserved; C2 QUALIFIED-DEFER continues; **C3 QUALIFIED-DEFER — Sprint 41 ships the FIRST observed evidence surface; elevation independent verdict at Sprint 41 close**); C4 + C5 NOT A CANDIDATE preserved.
10. **`docs/current/iteration_governance.md`** — §1.3 (LLM-owned — verify the `prior_use_case_carry` projection slot is LLM-soft signal; verify LLM continues to own UC hypothesis / drift / escalation posture); §1.4 (Runtime-owned — verify `SkillStateBus` is capability-floor persistence enforcement, NOT semantic-decision enforcement; verify tool schema / capability boundary / PII floor / grounding floor UNCHANGED); §1.7 (Forbidden — verify NO per-UC-pair if-else in `SkillStateBus` body OR `state_inheritance` declarations OR `prior_use_case_carry` slot construction; verify NO eval phrase encoding; verify NO LLM-vs-Java boundary shift); §3.2 (`skill_state` Q4 + `prompt_projection` Q3 layer classification for Sprint 41 §7 stanza); §4.1 nine-question kernel (Codex independently re-walks against the Sprint 41 diff); §4.2 sprint-close header convention; §4.3 trigger #3 (Sprint 41 fires); §5 / §5.5 / §5.6 acceptance bars (Sprint 41 + M2 §5 recalibration: bad-case suite OBSERVATION not gate for THIS milestone); §7 sprint-objective stanza; §8 milestone framework.
11. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set. Verify Sprint 41 dev commit ADDS no Tier-0 invariant (M2 §6 #8 fence preserved; Sprint 41 ships C3 candidate's FIRST observed evidence surface but DOES NOT pre-elevate). **Codex's independent C3 elevation verdict at §8 below is the load-bearing decision.**
12. **`docs/current/faq_grounding_contract.md`** — grounding-floor contract. Verify Sprint 41 does NOT touch the S1 `must_cite_source` predicate landed Sprint 39 (`SkillGuardrailDispatcher.java` UNCHANGED; `resolve_faq_grounded_answer.yaml` `guardrails:` block UNCHANGED — only `state_inheritance` block extended).
13. **`compact/sprint-041-dev-prompt.md`** — what the dev was authorized to do vs what landed (cross-check for in-scope vs out-of-scope edits). Especially the 5 LOAD-BEARING STOP discipline items from pre-launch handoff §4.2: (1) no per-UC-pair branch in SkillStateBus body; (2) no new classifier for UC-switch detection; (3) no per-UC-pair `state_inheritance` declarations; (4) no Tier-0 invariant added; (5) no scope creep into M3 surfaces.
14. **`compact/context-handoff-sprint-041-pre-launch.md`** — the deliver-agent pre-launch handoff. Especially: §4 decision records (multi-fence convergence; strong STOP discipline; M2 §5 recalibration carryforward); §7.4 the per-sub-sprint Codex review trigger framing (this prompt is the deliver-agent's product for §7.4).
15. **Code source files for cited-line spot-checking** at HEAD `8cd0a10` (read on demand during §3 + §5 + §6 verification, NOT end-to-end):
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` (NEW; 203 lines; verify via `wc -l`).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (post-Sprint-41 1341 lines; verify via `wc -l` + visually scan the `prior_use_case_carry` emission site at the `discover_disambiguation_signals` sibling + `buildPriorUseCaseCarryNode` helper body).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (post-Sprint-41 1482 lines; verify via `wc -l` + visually scan `maybeApplyStateBusOnSwitch` helper body + invocation site in `plan(...)`).
    - `server/src/main/resources/skills/{discover_triage,confirm,resolve_faq_grounded_answer,resolve_intake_collect_and_handover,escalate,terminal}.yaml` (each EDITed; verify `state_inheritance` block declaration matches §10.5 matrix row per Skill).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (UNCHANGED at 135 lines; verify via `git diff 9130abc..8cd0a10 -- ...` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` (UNCHANGED at 59 lines; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (UNCHANGED at 313 lines from Sprint 38-fix; verify `VALID_STATE_KEYS` at lines 53-57 + `VALID_PROJECTION_SLOTS` at lines 65-69 + `validate(...)` at lines 262-280).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (UNCHANGED at 416 lines from Sprint 39; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (UNCHANGED at 222 lines; M1 functional surface; verify via `git diff` returns empty; SkillStateBus CONSUMES `requiredFieldsFor(...)` + `canonicalFieldName(...)` but does NOT modify).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (UNCHANGED at 174 lines; verify).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` (UNCHANGED at 205 lines; Sprint 11 / 11.1 / 12 frozen surface; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (UNCHANGED at 638 lines; integration via `PhaseEvaluator.plan(...)`, NOT via `ControlKernel` or `AgentRunLoopImpl`; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` (UNCHANGED; verify via `git diff` returns empty).
    - `server/src/main/resources/prompts/system_prompt.txt` (UNCHANGED at 80 lines from Sprint 40; D-g SKIPPED; verify via `git diff` returns empty + `wc -l`).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillStateBusTest.java` (NEW; 306 lines; 17 tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/PriorUseCaseCarryProjectionTest.java` (NEW; 279 lines; 13 tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/UcSwitchStateInheritanceTest.java` (NEW; 246 lines; 9 integration tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint71PartialIntakePersistenceTest.java` (M1 functional surface; only ctor-signature update in Sprint 41; verify `14/14 PRESERVED` per §10).

## 2. Scope-discipline gate (BLOCKING)

Verify the dev commit at `8cd0a10` touches ONLY the surfaces enumerated in `docs/sprint_objective.md` §5 (Sprint 41 contract files in scope), modulo the 33 ctor-update existing-test files (Sprint 39 + Sprint 40 precedent for "behavioural-equivalence test edits don't count as scope creep"; see drift item §7-b). Run:

```bash
git -C /Users/caoruixin/projects/csagent-latest diff 9130abc..8cd0a10 --stat
git -C /Users/caoruixin/projects/csagent-latest show --numstat 8cd0a10
```

Expected file list (any extra file outside this list is a **BLOCKING scope violation**):

**Main sources (10 files):**
- **NEW** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` (203 lines; +203 / −0).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (+182 / −2).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (+59 / −3).
- **EDIT** × 6 Skill YAMLs (`discover_triage` +4/−1; `confirm` +1/−0; `resolve_faq_grounded_answer` +3/−1; `resolve_intake_collect_and_handover` +1/−0; `escalate` +3/−1; `terminal` +3/−1).

**Test sources (36 files: 3 NEW + 33 EDIT for ctor signature):**
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillStateBusTest.java` (+306 / −0).
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/PriorUseCaseCarryProjectionTest.java` (+279 / −0).
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/UcSwitchStateInheritanceTest.java` (+246 / −0).
- **EDIT** × 33 existing test files at `server/src/test/java/com/gumtree/csagent/service/runtime/[...]`: 9 `AgentRunLoop*` integration tests + 3 other AgentRunLoop-territory integration tests (`Cs014`, `Sprint141FaqGrounding`, `Sprint9TraceObservability`) + 21 unit / phase-evaluator / projection tests + `Sprint71PartialIntakePersistenceTest`. Each `2/1` (single ctor site) modulo `Sprint11ProgressiveResolveTest` `6/3` (3 ctor sites) + `Sprint7CandidateUseCasesProjectionTest` `4/2` + `Sprint7IntakeStateTest` `4/2`.

**Docs (1 file):**
- **NEW** `docs/sprints/sprint-041-handoff.md` (+414 / −0).

Total expected per `git show --stat 8cd0a10`: **46 files; +1734 / −44**.

The 33 existing-test ctor-update files exceed Sprint 41 contract §5's explicit listing but extend the Sprint 39 + Sprint 40 precedent (behavioural-equivalence test edits don't count as scope creep). See drift item §7-b for independent verdict.

**Deliver-agent-owned files NOT staged in dev commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`**: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-040-*.md`, `compact/sprint-041-*.md`, anything under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-040-*` (immutable archives), anything in `docs/milestones/`, anything in `docs/foundational/`, anything in `docs/current/` (governance docs), anything in `docs/proposals/`, anything in `eval_interactive/`. Verify each via `git diff 9130abc..8cd0a10 --name-only | grep -v -E '<expected-prefix>'` returns empty (excluding the 46 expected files). Human bundles deliver-agent files at Sprint 41 close + M2 close.

## 3. Anti-Hardcode kernel (§4.1) independent re-walk

Walk all 9 `iteration_governance.md` §4.1 questions on the Sprint 41 dev diff (46 files). Sprint 41 ships Sprint 37 freeze decision (i) §10 as runtime code. Verify INDEPENDENTLY (dev's self-walk at handoff §6 is dev-authored, not Codex's independent verdict):

1. **Q1** — keyword / regex / if-else / enum / per-UC matrix for a semantic decision? Verify SkillStateBus body uses a `switch` over the 3 allowlisted state keys (dispatch of allowlist labels → kernel actions, NOT semantic if-else). Verify `applyIntakeFieldsIntersection(BotSession)` is a SINGLE registry-driven lookup (`IntakeFieldsRegistry.requiredFieldsFor(activeUc)` + canonicalization), NOT a per-UC-pair table. Verify per-Skill `state_inheritance` declarations are PRINCIPLE-LEVEL (each row names INHERIT / RESET / SOFT_SIGNAL dimensions independent of which prior UC's state is being transitioned). Verify `buildPriorUseCaseCarryNode(BotSession, List<BotTurn>)` uses SINGLE integer constants (aging = 4, cap = 3) — NOT per-UC variation. Verify `maybeApplyStateBusOnSwitch` has a SINGLE guard (`prior_uc != current_uc && prior_skill != new_skill`) — capability-routing, NOT semantic decision.
2. **Q2** — Tier-0 justification? Verify Sprint 41 adds NO Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md` (M2 §6 #8 fence preserved). Sprint 41 ships C3 candidate's FIRST observed evidence surface; independent verdict on elevation timing at §8 below.
3. **Q3** — soft signal achievable instead of hard branch? Verify the `prior_use_case_carry` projection slot IS the soft signal for continuity (LLM owns continuation read per §1.3). Verify the deterministic `intake_fields_partial` enforcement in the bus is justified per design doc §10.7 Alternative D REJECTED ("LLM can't be relied on to filter intake fields correctly per registry requirements; pure projection would weaken capability-floor"). Verify no NEW hard branch is added beyond what design doc §10 mandates.
4. **Q4** — visible-eval / trace phrasing / CaseSpec id encoded? Spot-check `SkillStateBus.java` body + `applyIntakeFieldsIntersection` + `buildPriorUseCaseCarryNode` + per-Skill `state_inheritance` YAML declarations + 3 NEW test files for `alice_*`, `cs_*`, `3772e56b-*`, bad-case-suite text, visible-eval phrase. Verify tests assert on stable architectural shapes (e.g., `prior_use_case_carry` JSON shape; `intake_fields_partial` registry intersection outcomes; same-Skill no-op), NOT on CaseSpec-derived text.
5. **Q5** — semantic ownership shift LLM → Java? Verify §1.3 LLM ownership preserved (LLM owns `prior_use_case_carry` read: continue / ask / ignore — runtime does NOT enforce action on slot value). Verify §1.4 Runtime ownership preserved (`SkillStateBus` is capability-floor persistence enforcement — it physically clears `intake_fields_partial` when a non-INTAKE Skill takes over and the prior UC's intake state is no longer applicable; this is data persistence, NOT semantic decision). Verify NO semantic decision (UC hypothesis, drift handling, escalation posture, follow-up policy) moved to Java.
6. **Q6** — if-else block in prompt instead of principle-level guidance? Verify D-g SKIPPED (no prompt edits in Sprint 41). If Codex believes D-g should NOT have been skipped, surface as OQ-S41.1 verdict (see §6).
7. **Q7** — tool schema / capability / PII / grounding floor preserved? Verify tool schemas UNCHANGED (no tool YAML or `*Tool.java` edits); capability/permission boundary UNCHANGED (`Skill.toolsRequired()` consumption unchanged); PII/safety floor UNCHANGED; grounding floor UNCHANGED (Sprint 39 S1 `must_cite_source` predicate preserved; `resolve_faq_grounded_answer.yaml` `guardrails:` UNCHANGED — only `state_inheritance` extended).
8. **Q8** — generalization coverage (target / neighbor / negative / shadow)? Verify target = 17 `SkillStateBusTest` (§10.5 matrix × inherit / reset / soft_signal + registry intersection + same-Skill no-op + null inputs + EMPTY declaration + diagnostic) + 13 `PriorUseCaseCarryProjectionTest` (slot shape + cap + aging + null cases + cross-UC shape invariance + `prior_skill_name` resolution) + 9 `UcSwitchStateInheritanceTest` (representative Skill switches end-to-end). Neighbor = 33 existing tests updated for ctor signature (defensive null-checks preserve behavioural equivalence; `Sprint71PartialIntakePersistenceTest 14/14 PRESERVED`). Negative = same-Skill turns + fresh sessions + null priorSkill assertions in unit tests. Shadow = N/A per M2 §5 recalibration. Java baseline: 1105 (post-Sprint-40) + 39 = 1144 expected.
9. **Q9** — rollback / sunset? Verify no temporary measure / kill-switch introduced. `git revert 8cd0a10` restores the Sprint 41 surfaces exactly to Sprint 40 close state. The Skill Registry abstraction itself stays (Sprint 37 / 38 / 39 / 40 surfaces preserved; SkillStateBus + state_inheritance + prior_use_case_carry land as permanent runtime infrastructure per M2 milestone scope).

Output: per-Q verdict (`pass` / `concern` / `fail`); a single Q `fail` is a blocking finding.

## 4. §1.7 Boundary check

Walk each `iteration_governance.md` §1.7 forbidden item against the Sprint 41 diff:

- **(a) encoding raw eval phrases into Java or prompt** — verify `SkillStateBus.java` + `buildPriorUseCaseCarryNode` + per-Skill YAML `state_inheritance` declarations carry NO Alice / bad-case-suite / source-session-id text.
- **(b) adding UC-specific hard rules for soft semantic decisions** — verify the bus body uses general "for each declared dimension ... apply registry-driven action" language; no per-UC-pair if-else.
- **(c) widening eval spec to accept a genuine bot mistake** — verify Sprint 41 ships no `eval_interactive/` edits.
- **(d) optimizing visible eval at the cost of shadow/generalization** — N/A per M2 §5 recalibration; verify Sprint 41 ships no smoke composite_score change as primary evidence.
- **(e) using prompt as an if-else rule dump** — verify `system_prompt.txt` UNCHANGED (D-g SKIPPED; verify `git diff 9130abc..8cd0a10 -- server/src/main/resources/prompts/system_prompt.txt` returns empty).

Output: per-item verdict.

## 5. Hard-fence verification (Sprint 41 contract §6 + M2 contract §6)

Verify each hard fence cited in Sprint 41 contract §6 (33 items) + M2 contract §6 (22 items). High-value spot-checks:

- **Sprint 41 §6 #1 + M2 §6 #1**: NO per-UC-branch if-else in `SkillStateBus.java` body OR `state_inheritance` declarations OR `buildPriorUseCaseCarryNode` body (verified in §3 Q1 above).
- **Sprint 41 §6 (no new classifier)**: UC-switch detection rides on existing M1 Sprint 32 + Sprint 33 projections; no new classifier added. Verify `maybeApplyStateBusOnSwitch` reads `session.getActiveUseCase()` + walks `history` for prior turn's `activeUseCase` — these are surfaces that already existed pre-Sprint-41.
- **M2 §6 #4**: Sprint 39 S1 `must_cite_source` bounded surface UNCHANGED in Sprint 41 (verify `SkillGuardrailDispatcher.handleMustCiteSource` UNCHANGED via `git diff 9130abc..8cd0a10 -- server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` returns empty + `resolve_faq_grounded_answer.yaml` `guardrails:` block UNCHANGED).
- **M2 §6 #5**: NO touch to `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` (verify via `git diff 9130abc..8cd0a10 --name-only` returns empty for these files).
- **M2 §6 #6 + #7**: NO edit to `INTAKE_UCS` set; NO widening of `escalation_reason` enum (verify via `git diff` on relevant files returns empty).
- **M2 §6 #8**: NO new Tier-0 invariant (verify `git diff 9130abc..8cd0a10 -- docs/runtime_freeze_and_risk_policy.md` returns empty). C3 elevation independent verdict at §8 below.
- **M2 §6 #20**: NO OLD design deletion (the Sprint 11 / 11.1 / 12 frozen surface stays; `ResolveDispositionEvaluator.java` UNCHANGED).
- **M2 §6 #21**: M1 functional surface preserved (`IntakeFieldsRegistry.java` + `IntakeFieldExtractor.java` + `UseCaseRegistryService.java` UNCHANGED; `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED — note ctor-signature update is the ONE allowed Sprint 41 touch per OQ-S41.3).
- **Sprint 41 §6 (M3-A fence)**: NO touch to `request_handover` schema OR rationale/confidence fields (verify via `git diff` on relevant tool files returns empty).
- **Sprint 41 §6 (M3-B fence)**: NO touch to `D-single-handover-orchestrator` surface OR `D-full-issue-ledger` scope.
- **Sprint 41 §6 (M3-D fence)**: NO touch to Topic↔UC binding logic.
- **Sprint 41 §6 (governance fence)**: NO edit to `docs/proposals/skill_registry_design.md` (immutable Sprint 37 freeze) OR `docs/foundational/*` OR `docs/current/*` OR `docs/sprints/sprint-001-*` through `docs/sprints/sprint-040-*` archives.

Output: per-fence verdict.

## 6. OQ items — INDEPENDENT verdict required

Dev surfaced 6 OQs in handoff §7. The deliver-agent has NOT pre-loaded dispositions; Codex returns INDEPENDENT verdicts on each. The deliver-agent + human will read your verdicts at Sprint 41 close and arbitrate against their own assessment.

### OQ-S41.1 — D-g system_prompt.txt teaching SKIPPED

**The question:** Sprint 41 contract §2.7 + §10 closing paragraph said D-g (one-line orchestration-shell teaching about `prior_use_case_carry` slot in `system_prompt.txt`) is OPTIONAL — Sprint 40 §7.3 pattern (dev judgement at session start). Dev SKIPPED per handoff §4 D-g + §7 OQ-S41.1 reasoning: the new slot is intrinsically self-documenting (`ages_out_after_turns` field names its own aging window; `prior_skill_name` is descriptive; the `prior_active_use_case` + citation list shape mirrors existing Sprint 31 / Sprint 33 slot precedent the LLM already reads). Sprint 40 envelope-mechanics paragraph principle says: the slot's self-documentation + the Skill envelope projection (which surfaces `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` to the LLM as the per-Skill declaration of consumption) is sufficient; adding a `system_prompt.txt` paragraph would duplicate self-evident structure without adding LLM-actionable principle.

**Codex verdict required:** (a) AGREE WITH DEV — slot self-documentation + Skill envelope projection are sufficient; no D-g teaching warranted; OR (b) DISAGREE — confirm a one-line shell pointer SHOULD land at M2 close fold-back (as a separate housekeeping commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`); OR (c) DISAGREE — D-g should have landed in Sprint 41 itself; classify as `fix_required`. Read post-Sprint-41 `system_prompt.txt` (UNCHANGED at 80 lines from Sprint 40) + the `ContextProjectionBuilder` emission site for `prior_use_case_carry` + the Skill envelope projection in `PhaseEvaluator.plan(...)` to assess whether the slot's self-documentation is genuinely LLM-actionable without shell teaching.

### OQ-S41.2 — C3 Tier-0 candidate elevation timing (LOAD-BEARING for Sprint 41 close)

**The question:** Sprint 37 §12 surfaced 5 Tier-0 candidates; C3 (state-bus boundary enforcement — "the LLM CANNOT override `state_inheritance` declarations once SkillStateBus is wired into PhaseEvaluator's Skill-switch boundary") was QUALIFIED-DEFER at design freeze pending FIRST observed evidence surface. Sprint 41 ships the FIRST observed evidence surface: `SkillStateBus.applyOnSkillSwitch(...)` enforcement + 17 unit + 9 integration tests demonstrating the LLM has no path to override declared state inheritance.

Dev does NOT pre-elevate per `feedback_constitution_discipline_vs_planning_anticipation.md` (Tier-0 elevation requires deliver-agent + human + Codex jointly; dev surfaces evidence). Dev handoff §7 OQ-S41.2 surfaces evidence + requests Codex independent verdict.

**Codex verdict required:** (a) DEFER continued — Java tests are necessary but not sufficient production evidence; the load-bearing claim (LLM CANNOT override state inheritance) needs production trace observation across Sprint 41 → M2 close → M3+ window before C3 elevation; deliver-agent + human + Codex re-evaluate at M2 close OR M3+; OR (b) ELEVATE NOW — the deterministic Java tests + Spring `@Component` wiring + 6 YAML declarations + integration tests are collectively sufficient evidence of structural enforcement that production trace evidence is not load-bearing for elevation; route as separate governance commit to `docs/runtime_freeze_and_risk_policy.md` §1.1 at Sprint 41 close per M2 §6 #8 fence EXCEPTION clause (sub-sprint-close / M2-close + human authorization); OR (c) NOT A CANDIDATE — re-classify C3 from Sprint 37 verdict (this would be a major architectural divergence; surface evidence). 

If your verdict is (b) ELEVATE NOW, classify as `out_of_scope_review` (NOT `fix_required`) — Tier-0 elevation is deliver-agent + human authority + requires explicit human-review escalation per `feedback_constitution_discipline_vs_planning_anticipation.md` + Sprint 37/38/39 OQ-S38.5 / OQ-S39.5 precedent. Sprint 41 close is one of two windows (Sprint 41 close OR M2 close) where this decision can be made; Codex's evidence-based verdict is load-bearing for both timing options. Surface the specific evidence + rationale; deliver-agent + human will arbitrate.

### OQ-S41.3 — Existing-test ctor signature `null` argument style

**The question:** Sprint 41 updated 33 existing test files (per `git show --numstat 8cd0a10` enumeration; ~37 ctor call-sites by enumeration; dev handoff §4 D-f frames this as "20 PhaseEvaluator + 16 ContextProjectionBuilder = 36" — see drift item §7-d for the framing divergence). All updates pass `null` for the new dependency arg (`SkillStateBus` for `PhaseEvaluator`; `SkillRegistry` for `ContextProjectionBuilder`). Defensive null-checks in `maybeApplyStateBusOnSwitch` + `buildPriorUseCaseCarryNode` make this safe (the bus invocation early-returns on null bus; the `prior_use_case_carry` slot construction early-returns null `prior_skill_name` on null registry but still emits structurally).

**Dev's defense at handoff §4 D-f + §7 OQ-S41.3:** preserves behavioural equivalence for tests that don't exercise the new surfaces; Sprint 40 ctor-update precedent (Sprint 40 dev passed `null` for new args added by Sprint 40 to existing test ctors; Sprint 40 Codex review accepted). Alternative is a `SkillTestFixtures.productionSkillStateBus()` helper that wires real production fixtures into all 33 test files — bigger diff + carries more risk than the `null` + defensive null-check pattern.

**Codex verdict required:** (a) ACCEPTABLE STYLE per Sprint 40 ctor-update precedent — extend the precedent and approve; OR (b) REQUEST FOLLOW-UP FIX — request a follow-up sub-sprint to wire `SkillTestFixtures.productionSkillStateBus()` (+ analogous SkillRegistry fixture) across the 33 test files; classify as `out_of_scope_review` (this would be a Sprint 42 / M2-close fold-back housekeeping sub-sprint, NOT a Sprint 41 fix-iteration since the current style passes Java baseline and preserves equivalence). Sample 3-5 of the 33 test files (e.g., `Sprint71PartialIntakePersistenceTest`, `PhaseEvaluatorPlanTest`, `Cs014RouteAndLoopHandoverIntegrationTest`) to verify the `null` + defensive null-check pattern is genuinely safe across the diff (and not a latent NPE risk).

### OQ-S41.4 — `accumulated_tool_results` declared `inherit` on all 6 Skills (vs §10.5 row 1 reading)

**The question:** Design doc `docs/proposals/skill_registry_design.md` §10.5 row 1 reads (per dev's handoff §4 D-c quotation) "All resolve-side Skills" inherit `accumulated_tool_results`. Sprint 41 contract §2.3 + dev implementation extended `inherit: [accumulated_tool_results]` to all 6 Skills (including DISCOVER, CONFIRM, ESCALATE, TERMINAL — not just resolve-side).

**Dev's defense at handoff §4 D-c + §7 OQ-S41.4:** `accumulated_tool_results` has no persistent session field; the bus action is a documented no-op regardless of inherit-vs-reset. Declaring inherit universally documents the principle that prior-turn tool results survive into the next Skill's projection via `conversation_history` (the LLM observes them through history projection; the bus has no separate `accumulated_tool_results` to mutate). The §10.5 row 1 reading "All resolve-side Skills" was overly narrow — the principle is universal.

**Codex verdict required:** (a) AGREE WITH DEV — universal inherit declaration is the intended reading of §10.5 row 1 (the design doc text was a narrow framing of a universal principle); OR (b) FLAG AS DESIGN-DOC EDITORIAL DIVERGENCE — Sprint 41 contract + dev implementation extended §10.5 row 1 beyond the design doc's narrow text; route as M2 close editorial fold-back (Sprint 38 OQ-S38.1 + Sprint 39 OQ + Sprint 40 OQ-S40.5 precedent); OR (c) NEEDS CLARIFICATION — the design doc §10.5 row 1 is ambiguous between "all 6 Skills" vs "resolve-side only"; deliver-agent + human + Codex resolve at Sprint 41 close OR M2 close. Verify by reading design doc §10.5 row 1 verbatim + comparing to the 6 YAML declarations.

### OQ-S41.5 — Inherited `SystemPromptUserRequestedTiebreakerTest` persists

**The question:** Sprint 40 close documented this failure as carried since Sprint 24-era working-tree mod (`prompt.contains("Sprint 6")` literal-anchor token absent from live `system_prompt.txt`). Sprint 41 contract §10 (20) stop condition: "do NOT make it a hard gate at Sprint 41 close". Sprint 41 dev handoff §11 + §3 premise 15 reports: PERSISTS unchanged (1 → 1 failure; Java baseline 1105 / 1 → 1144 / 1).

**Codex verdict required:** (a) STATUS QUO — not a Sprint 41 close blocker; defer to M2 close for systematic resolution OR Sprint 42 housekeeping; OR (b) IMPROVING — Sprint 41 partially addressed the drift; defer to M2 close housekeeping; OR (c) ROUTE AS FIX-ITERATION — Sprint 41 should have resolved this and didn't. If (c), propose what specifically the dev should have done within Sprint 41 scope (note: adding back the "Sprint 6" anchor would be a deliberate annotation in the post-Sprint-41 shell, arguably out of Sprint 41 scope per §6 stop condition #20).

### OQ-S41.6 — Editorial divergence between §10 template and implementation

**The question:** Dev handoff §7 OQ-S41.6: "Sprint 41 implementation follows §10.1-§10.11 verbatim; no template-vs-implementation divergence surfaced. If deliver-agent + Codex find any post-hoc, route as OQ for M2 close fold-back per Sprint 38 OQ-S38.1 + Sprint 39 OQ + Sprint 40 OQ-S40.5 precedents."

**Codex verdict required:** (a) NO DIVERGENCE SURFACED — confirm dev's null-finding (re-read design doc §10.1-§10.11 against the dev's implementation; no editorial gap); OR (b) DIVERGENCE FOUND — name the specific §10.x text + the implementation deviation + route as M2 close editorial fold-back. If (b), classify the divergence severity: MINOR (line-number / phrasing) → M2 close fold-back; MAJOR (architectural reading) → escalate to deliver-agent + human as load-bearing finding.

## 7. Additional contract-drift items to classify

### Drift item §7-a — Sprint 39 + Sprint 40 precedent for "behavioural-equivalence test edits don't count as scope creep"

**Background:** Sprint 39's contract §5 listed 14 files; dev shipped 21 (8 EDIT test files via constructor + dispatcher-helper sweep). Sprint 40's contract §5 listed 4 files; dev shipped 5 (OQ-S40.1: PhaseEvaluatorSkillIntegrationTest golden update). Both classified as in-scope behavioural-equivalence updates by Codex (`pass / 0` on first pass for Sprint 39; AGREE WITH DEV — INCLUDE per Sprint 39 precedent for Sprint 40). Sprint 41 contract §5 lists ~10-15 files; dev shipped 46 (3 NEW + 33 EDIT existing test ctor-update). Does the Sprint 39 + Sprint 40 precedent extend?

**Codex verdict required:** reaffirm OR push back on the precedent. If REAFFIRM: classify the 33 ctor-update files as the precedent's continuation (in-scope behavioural-equivalence update; not scope creep). If PUSH BACK: classify as scope creep (the precedent should NOT extend at this magnitude — Sprint 39 had 8 EDIT test files for unified-dispatcher constructor sweep; Sprint 40 had 1; Sprint 41 has 33 — different magnitude). Note: Sprint 41 ctor-update was inevitable because the new arg (`SkillStateBus` to `PhaseEvaluator`; `SkillRegistry` to `ContextProjectionBuilder`) was added to two constructors that are heavily-tested by name in existing tests.

### Drift item §7-b — Java baseline drift (1144 / 1 vs contract target 1140-1155)

**Background:** Sprint 41 contract §9 Java baseline target: ~1140-1155. Dev actual: 1144 / 1 inherited / 0 / 2 per `cd server && mvn test 2>&1 | grep "Tests run:" | tail -1` (39 new = 17 + 13 + 9). Within range.

**Codex verdict required:** independently confirm via `cd server && mvn clean test -q 2>&1 | tail -25`. Expected `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2`. The 1 failure is `SystemPromptUserRequestedTiebreakerTest` (OQ-S41.5). Report any unexpected result; classify as `pass` if 1144/1/0/2 OR within ±5 (within contract observation tolerance).

### Drift item §7-c — line-count target hit / miss

**Background:** Sprint 41 contract §9 file-count targets:
- `SkillStateBus.java` target: ~150-250 lines; actual 203 ✓
- `ContextProjectionBuilder.java` target: ~1191-1211 (was 1161 pre-Sprint-41); actual 1341 (+180) — exceeds target by ~130 lines. Dev rationale: `buildPriorUseCaseCarryNode` helper is ~115 lines; contract underestimated helper scope.
- `PhaseEvaluator.java` target: ~5-15 new lines; actual +55 net (+59 / −3). Exceeds target. Dev rationale: helper + ctor + invocation; contract underestimated helper scope.

**Codex verdict required:** classify as (a) CONTENT-COMPLIANT — line-count target was advisory; the substantive design doc §10 fidelity is the load-bearing surface; line-count tightness target was estimated pre-implementation; OR (b) TIGHTNESS CONCERN — Sprint 41 should have hit the contract targets; propose specific helper-extraction options that could have tightened the diff. Reaffirm Sprint 39 + Sprint 40 precedent (line-count targets are advisory, content-fidelity is load-bearing).

### Drift item §7-d — Deliver-agent / dev handoff numbers-cite framing divergence

**Background:** Dev handoff §4 D-f frames the existing-test ctor-update as "20 PhaseEvaluator + 16 ContextProjectionBuilder = 36 existing tests". Reality per `git show --numstat 8cd0a10`:
- 33 distinct existing test files.
- ~37 ctor call-sites by enumeration (most files have 1 site; 1 file has 3 sites; 2 files have 2 sites; total ≈ 30 × 1 + 1 × 3 + 2 × 2 = 37).
- The "20 + 16 = 36" framing is neither a count of distinct files (33) nor ctor call-sites (37); it's a category-overlap count where a file touching both `PhaseEvaluator` and `ContextProjectionBuilder` constructors gets counted in both sub-totals.

This is a continuation of the Sprint 39 + Sprint 40 deliver-agent `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` lapse pattern: handoff narrative reaches for a category-based mental count rather than running `git show --numstat 8cd0a10 | wc -l`.

**Codex verdict required:** (a) NON-BLOCKING NUMBERS-CITE OBSERVATION — note in §10 of `docs/codex-findings.md` for `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` continuation (third observed instance); the substantive ctor-update work is correct; the framing is a narrative drift; OR (b) ROUTE AS SEPARATE FOLLOW-UP — request deliver-agent / dev to amend the handoff §4 D-f framing at M2 close housekeeping. Reaffirm: numbers-cite should be a discipline at deliver-agent's Sprint 41 review prompt + M2 milestone-shared review prompt + future sub-sprint handoff §9 tables.

## 8. Tier-0 candidate disposition (load-bearing for Sprint 41 close)

Sprint 37 freeze §12 surfaced 5 candidates:

- **C1** REJECTED preserved by construction (Sprint 38 + Sprint 39 + Sprint 40 + Sprint 41 closes preserved).
- **C2** QUALIFIED-DEFER (guardrail refusal non-overridability). Sprint 39 dispatcher landing was FIRST observed evidence surface; Sprint 41 ships nothing new C2-relevant. **DEFER continues.**
- **C3** QUALIFIED-DEFER (state-bus boundary enforcement). **Sprint 41 ships the FIRST observed evidence surface** (`SkillStateBus.applyOnSkillSwitch` enforcement + 17 unit + 9 integration tests). **CODEX INDEPENDENT VERDICT IS THE LOAD-BEARING DECISION FOR SPRINT 41 CLOSE** — see OQ-S41.2 above. Expected paths:
  - (a) DEFER continued — production trace evidence pending; deliver-agent + human + Codex re-evaluate at M2 close OR M3+. **This is the deliver-agent + human pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md`** (deterministic Java tests are necessary but not sufficient production evidence; the bus's load-bearing claim — LLM CANNOT override `state_inheritance` — needs production trace observation).
  - (b) ELEVATE NOW — Codex's evidence-based verdict overrides the pre-decision; route as `out_of_scope_review` per Sprint 37/38/39 OQ-S38.5 / OQ-S39.5 precedent; deliver-agent + human escalate to human-review at planning round + execute SEPARATE governance commit to `docs/runtime_freeze_and_risk_policy.md` §1.1 at Sprint 41 close OR M2 close.
  - (c) NOT A CANDIDATE — re-classify C3 from Sprint 37 verdict; major architectural divergence; surface evidence and route as `out_of_scope_review`.
- **C4** + **C5** NOT A CANDIDATE preserved.

**Codex verdict required:** for C3, name your verdict (a/b/c) + cite specific evidence (which Java test, which YAML declaration, which integration test, which production trace surface if available). For C1, C2, C4, C5: REAFFIRM (default) OR raise alternative.

## 9. Schema and reproducibility checks

Verify the Sprint 41 dev handoff's reproducibility per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`:

- Every line-count claim cites a file path. Spot-check 5-10 claims via `wc -l <path>`.
- Every commit-shape claim is reproducible via `git show --stat 8cd0a10` or `git diff 9130abc..8cd0a10 --stat` or `git diff 9130abc..8cd0a10 --numstat`.
- Every "X UNCHANGED" claim is reproducible via `git diff 9130abc..8cd0a10 -- <path>` returning empty.
- Every Sprint 37/38/38-fix/39/40 baseline citation is reproducible via the named sprint archive at `docs/sprints/sprint-NNN-*.md`.
- Drift item §7-d (the "20 + 16 = 36" framing) is the third observed instance of the deliver-agent + dev handoff numbers-cite lapse pattern.

Surface any non-reproducible claim as a finding (informational unless it changes the verdict).

## 10. Validation runs

Run from a clean working tree:

```bash
cd /Users/caoruixin/projects/csagent-latest/server
mvn clean test -q 2>&1 | tail -25
```

**Expected verdict for Sprint 41 close:**
- `Tests run: 1144, Failures: 1, Errors: 0, Skipped: 2`.
- The sole failure is the documented inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` (OQ-S41.5 PERSISTS).
- Java baseline trajectory: 1105 (post-Sprint-40) → 1144 (post-Sprint-41) = +39 new tests (17 `SkillStateBusTest` + 13 `PriorUseCaseCarryProjectionTest` + 9 `UcSwitchStateInheritanceTest`).

Targeted spot-check runs:

```bash
# New Sprint 41 tests — verify 17 + 13 + 9 = 39 PASS
mvn -pl server test -Dtest=SkillStateBusTest -q
mvn -pl server test -Dtest=PriorUseCaseCarryProjectionTest -q
mvn -pl server test -Dtest=UcSwitchStateInheritanceTest -q

# M1 functional surface — verify UNCHANGED
mvn -pl server test -Dtest=Sprint71PartialIntakePersistenceTest -q

# Sprint 40 tests — verify UNCHANGED
mvn -pl server test -Dtest=SkillTeachingMigrationIntegrationTest -q

# Sprint 39 tests — verify UNCHANGED
mvn -pl server test -Dtest=PhaseEvaluatorResolveSkillIntegrationTest,SkillGuardrailDispatcherTest,ResolveFaqGuardrailsTest,ResolveIntakeGuardrailsTest -q

# Sprint 38 SkillRegistry-core tests — verify UNCHANGED
mvn -pl server test -Dtest=SkillTest,SkillRegistryTest,SkillLoaderTest -q

# Sprint 31/33 projection-slot tests — verify UNCHANGED
mvn -pl server test -Dtest=Sprint7CandidateUseCasesProjectionTest,DiscoverDisambiguationSignalsProjectionTest -q

# Inherited baseline failure
mvn -pl server test -Dtest=SystemPromptUserRequestedTiebreakerTest -q
```

Surface any unexpected result.

## 11. Output format — §4.2 sprint-close header

Write `docs/codex-findings.md` per the scaffold (35 lines). Top of the file:

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph — Codex's verdict on Sprint 41 close>
```

Then the per-section structure (Review Evidence, Blocking Findings, Anti-Hardcode Kernel, §1.7 Boundary Check, Hard-Fence Verification, Schema And Reproducibility Checks, Validation Runs, **Tier-0 Candidate Independent Verification (LOAD-BEARING for Sprint 41 close — C3 elevation verdict)**, **OQ Independent Verification** [populate with 6 OQ verdicts + 4 contract-drift verdicts], Deferred / Non-Blocking Notes).

A single blocking finding → `fix_required` (`blocking_count: 1+`). Zero blocking findings → `pass` (`blocking_count: 0`). Any verdict that would expand the milestone scope or invent a new architectural surface (including ELEVATE NOW on C3 per OQ-S41.2) → `out_of_scope_review` (deliver-agent + human re-direct).

## 12. Anti-patterns to refuse

- Do NOT re-import `iteration_governance.md` §5.6 bad-case-suite-primary default (M2 §5 recalibration governs for THIS milestone).
- Do NOT extrapolate scope from the dev's surfaced OQs (every OQ is independent classification, not a scope-expansion invitation).
- Do NOT silently elevate C3 to Tier-0 in your verdict (Tier-0 elevation is deliver-agent + human authority per `feedback_constitution_discipline_vs_planning_anticipation.md`; if your evidence-based verdict is ELEVATE NOW, route as `out_of_scope_review` and surface the specific evidence + rationale — deliver-agent + human authorize the actual `runtime_freeze_and_risk_policy.md` edit at Sprint 41 close OR M2 close).
- Do NOT propose code fix beyond naming the layer per §3.2 of `iteration_governance.md`.
- Do NOT propose edits to `docs/runtime_freeze_and_risk_policy.md`, `docs/proposals/skill_registry_design.md`, `docs/milestone_objective.md`, OR any `docs/sprints/sprint-NNN-*.md` archive (these are deliver-agent / human authority — out-of-scope for review). C3 elevation is a Tier-0 candidate evaluation, not a runtime-freeze policy edit.
- Do NOT rewrite the dev's `docs/sprints/sprint-041-handoff.md` (it is the immutable dev archive; the §12 closure verdict will be filled by deliver-agent + human at Sprint 41 close).
- Do NOT introduce new Tier-0 candidates in the verdict (Sprint 37 surfaced 5; that surface is closed unless new structural evidence surfaces; Sprint 41 ships C3's FIRST observed evidence surface but does NOT surface new candidates beyond C1-C5).
- Do NOT make M2 milestone-close decisions in this review (M2 close is a separate planning round; this review is Sprint 41 close only; M2 close gets its own milestone-shared review prompt + cumulative commit range Codex pass).

---

End of Sprint 41 review prompt. Return your verdict in `docs/codex-findings.md` per §11 above.
