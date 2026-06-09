Paste the content below this line into a fresh Claude Code session at the start of Sprint 41 dev work.

---

You are Claude Code working on Sprint 41 — the FIFTH and LAST sub-sprint of NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization). Sprint 40 (M2 sub-sprint 4) closed PASS A on 2026-05-18 at commit `9130abc` with Codex per-sub-sprint review `pass / 0` on first pass. Your job is to implement Sprint 37 freeze decision (i) §10 as runtime code: session-level state-bus mediating cross-Skill state preservation on UC switch.

## 1. Loader

Read in this order on a fresh session. Do NOT skip; later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / §4.3 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — NEW M2 milestone north star. §3 Sprint 41 row + §5 acceptance recalibration (Alice + eval = OBSERVATION not gate) + §6 hard fences (22 items) + §8 Codex review plan + §11 cross-milestone sequencing.
3. **`docs/sprint_objective.md`** — Sprint 41 contract (THIS file you're working from is its instantiation; read the contract verbatim). All 12 sections binding. Especially: §2 (7 deliverables D-a/b/c/d/e/f/g + 1 optional D-g); §3 (Non-goals; explicit list); §4 (Premise check; 15 items dev SHALL re-verify at session start); §5 (Files in scope); §6 (Hard fences; 33 items); §7 (Bundle policy); §8 §7 stanza; §9 Success metrics; §10 Stop conditions (20 items); §11 Handoff §11 12-section contract.
4. **`docs/proposals/skill_registry_design.md`** §10 — Sprint 37 freeze decision (i) (LOAD-BEARING; ~280 lines from §10.1 to §10.11). Read each subsection: §10.1 Decision statement; §10.2 `state_inheritance` schema (3 keys + 3 projection slots); §10.3 SkillStateBus semantics (`applyOnSkillSwitch` + `inheritanceFor`); §10.4 NEW `prior_use_case_carry` slot (cap 3 + aging 4); §10.5 UC-switching invariant matrix (5 rows; carried from OLD Sprint 36 D5); §10.6 rationale; §10.7 alternatives REJECTED (A monolithic / B per-UC-pair / C state-bus over-enforcement / D pure-projection / E Java method per Skill); §10.8 §1.7 boundary check; §10.9 §1.3 / §1.4 boundary check; §10.10 C3 Tier-0 candidate (DEFER expected); §10.11 downstream sub-sprint reference (Sprint 41 ships 6 artefacts).
5. **`docs/sprints/sprint-040-handoff.md`** + **`docs/sprints/sprint-040-codex-review.md`** — Sprint 40 close baseline (1105 / 1 inherited / 0 / 2 Java tests; Sprint 40 surfaces UNCHANGED in Sprint 41).
6. **`docs/sprints/sprint-039-handoff.md`** + **`docs/sprints/sprint-039-codex-review.md`** — Sprint 39 archive. Especially: `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` declarations on Sprint 39 RESOLVE Skill YAMLs (forward-compat placeholders Sprint 41 EXTENDS with full §10.5 matrix shape).
7. **`docs/sprints/sprint-038-handoff.md`** + **`docs/sprints/sprint-038-fix-handoff.md`** + **`docs/sprints/sprint-038-codex-review.md`** — Sprint 38 archive. Especially: `SkillLoader.VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists from Sprint 38-fix (both already include the 3 keys + 3 slots per §10.2 forward-compat).
8. **`docs/current/iteration_governance.md`** — §1.3 (LLM-owned — verify the NEW `prior_use_case_carry` slot is LLM-soft signal; LLM owns continue/ask/ignore read); §1.4 (Runtime-owned — verify SkillStateBus is capability-floor enforcement, NOT semantic-decision enforcement); §1.7 (Forbidden — verify SkillStateBus body uses REGISTRY-DRIVEN intersection NOT per-UC-pair branch; verify per-Skill state_inheritance declarations are principle-level NOT per-UC-pair tables; verify prior_use_case_carry slot construction has no per-UC variation); §3.2 (`skill_state` Q4 + `prompt_projection` Q3 layer classification for Sprint 41 §7 stanza); §4.1 nine-question kernel.
9. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set. Verify Sprint 41 dev commit ADDS no Tier-0 invariant. C3 candidate evidence surfaces from Sprint 41 dispatcher landing but DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md`; deliver-agent + human + Codex jointly classify at Sprint 41 close OR M2 close.
10. **`compact/context-handoff-sprint-041-pre-launch.md`** — deliver-agent pre-launch handoff with strategic context.
11. **Source files for spot-checking at HEAD `9130abc`** (read on demand during implementation, NOT end-to-end):
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (verify `state_inheritance` field shape matches §10.2).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` (verify 3-field record matches §10.2).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (313 lines from Sprint 38-fix; verify `VALID_STATE_KEYS` at ~line 100 + `VALID_PROJECTION_SLOTS` at ~line 65-69 wire `Skill.validate(...)` for full state_inheritance declarations).
    - `server/src/main/resources/skills/discover_triage.yaml` + `confirm.yaml` + `escalate.yaml` + `terminal.yaml` + `resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml` (verify current state_inheritance blocks; Sprint 39 RESOLVE Skills have forward-compat placeholders).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (1161 lines; verify Sprint 31 `alternate_candidate_use_cases` slot at lines 396-416 + Sprint 33 `discover_disambiguation_signals` slot at lines 418-432 as SHAPE reference for the NEW `prior_use_case_carry` slot).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (1427 lines; verify `plan(...)` Skill selection logic for Skill-switch detection integration point).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` (verify alternative integration site per design doc §10.3).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/BotSession.java` (verify session-state holder structure for state-bus integration).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (222 lines; M1 functional surface; verify `requiredFieldsFor(uc)` + `canonicalFieldName(key)` public methods for registry-driven intersection per §10.3).

## 2. Mission

Implement Sprint 37 freeze decision (i) §10 as runtime code. Concrete deliverables per `docs/sprint_objective.md` §2:

**D-a** NEW `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` Spring `@Component` per §10.3 (2 public methods: `applyOnSkillSwitch(priorSkill, newSkill, session)` + `inheritanceFor(skill)`; registry-driven `intake_fields_partial` intersection using `IntakeFieldsRegistry`; no per-UC-pair branching).

**D-b** `Skill.java` EXTEND (verify `state_inheritance` field shape matches §10.2; minor EDIT if schema drift). `StateInheritance.java` (already exists from Sprint 38) verify 3-field record matches §10.2. `SkillLoader.java` `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists verify in place.

**D-c** All 6 Skill YAMLs EXTEND with `state_inheritance` block per §10.5 matrix:
- `discover_triage.yaml`: `inherit: [customer_context, accumulated_tool_results]` + `reset: [intake_fields_partial]` + `soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]`.
- `confirm.yaml`: `inherit: [customer_context, accumulated_tool_results, intake_fields_partial]` + `reset: []` + `soft_signal_via_projection: []`.
- `resolve_faq_grounded_answer.yaml`: EXTEND Sprint 39 placeholder; `inherit: [customer_context, accumulated_tool_results]` + `reset: [intake_fields_partial]` + `soft_signal_via_projection: [prior_use_case_carry]`.
- `resolve_intake_collect_and_handover.yaml`: EXTEND Sprint 39 placeholder; `inherit: [customer_context, accumulated_tool_results, intake_fields_partial]` + `reset: []` + `soft_signal_via_projection: [prior_use_case_carry]`.
- `escalate.yaml`: `inherit: [customer_context, accumulated_tool_results]` + `reset: [intake_fields_partial]` + `soft_signal_via_projection: []`.
- `terminal.yaml`: `inherit: [customer_context, accumulated_tool_results]` + `reset: [intake_fields_partial]` + `soft_signal_via_projection: []`.

(Exact shapes at dev judgement per §10.5 matrix; constraint is invariant matrix honored: accumulated_tool_results + customer_context always survive; intake_fields_partial survives only for INTAKE-flow continuation; prior_use_case_carry on resolve-side.)

**D-d** `ContextProjectionBuilder.java` EXTEND: NEW `prior_use_case_carry` projection slot per §10.4 shape (cap 3 most recent citations; aging 4 turns; null on no prior switch; sibling to existing Sprint 31 + Sprint 33 slots at lines 396-432). Construction REGISTRY-DRIVEN; no per-UC variation. Estimated ~30-50 new lines.

**D-e** `PhaseEvaluator.java` (or `ControlKernel.java` per dev judgement) integration: invoke `SkillStateBus.applyOnSkillSwitch(...)` when `plan(...)` (or `applyRerouteDecision(...)`) detects (a) `session.getActiveUseCase()` changed since prior turn AND (b) `select(phase, newUc)` returns different Skill from prior turn's active Skill. Detection rides on existing M1 surfaces (Sprint 32 + Sprint 33 projections); NO new classifier (M2 §6 #5 fence).

**D-f** Tests:
- NEW `SkillStateBusTest.java` (~15-25 tests): 5 invariant matrix rows × inherit / reset / soft_signal_via_projection; intake_fields_partial registry intersection; Skill-switch trigger detection; no-op on same-Skill turn.
- NEW `UcSwitchStateInheritanceTest.java` (~10-15 integration tests): representative Skill switches (DISCOVER → RESOLVE-FAQ, RESOLVE-FAQ → RESOLVE-INTAKE, RESOLVE-INTAKE → CONFIRM, etc.).
- NEW `PriorUseCaseCarryProjectionTest.java` (~10 tests): slot shape; cap; aging; null on no prior switch; no per-UC variation.
- EDIT existing `PhaseEvaluatorSkillIntegrationTest.java` if needed (1-2 new tests for Skill-switch integration).
- VERIFY `Sprint71PartialIntakePersistenceTest.java` 14/14 PRESERVED (M1 functional surface).

**D-g** (Optional) Orchestration-shell teaching about `prior_use_case_carry` slot — dev judgement; if added, principle-level + concise (1-2 sentences); MAY add to `system_prompt.txt`. If skipped: the slot's intrinsic self-documentation + Skill envelope projection per Sprint 40 is sufficient. Surface as OQ if added.

## 3. Premise re-verification at session start

Run these spot-checks at session start. STOP and surface if any premise has drifted from `docs/sprint_objective.md` §4:

```bash
git log --oneline -3
# Expected: top commit 9130abc (Sprint 40 close)

wc -l server/src/main/resources/prompts/system_prompt.txt
# Expected: 80 lines (post-Sprint-40)

wc -l server/src/main/resources/skills/*.yaml
# Expected: discover_triage.yaml 30, confirm.yaml 29, escalate.yaml 28, terminal.yaml 25, resolve_faq_grounded_answer.yaml 52, resolve_intake_collect_and_handover.yaml 37

wc -l server/src/main/java/com/gumtree/csagent/service/runtime/{PhaseEvaluator,AgentRunLoopImpl,IntakeFieldsRegistry,UseCaseRegistryService,ResolveDispositionEvaluator,ContextProjectionBuilder}.java
# Expected: 1427, 638, 222, 174, 205, 1161

wc -l server/src/main/java/com/gumtree/csagent/service/runtime/skill/{Skill,Guardrail,StateInheritance,SkillRegistry,SkillLoader,SkillGuardrailDispatcher,RejectVerdict,DispatchContext}.java
# Expected (approximate): Skill.java ~110, Guardrail.java ~40, StateInheritance.java ~35, SkillRegistry.java ~80, SkillLoader.java 313, SkillGuardrailDispatcher.java 416, RejectVerdict.java 26, DispatchContext.java 45

cd server && mvn test -q 2>&1 | tail -5
# Expected: Tests run: 1105, Failures: 1, Errors: 0, Skipped: 2
```

If any of these fails: STOP. Re-read the relevant docs to understand the drift. Surface in handoff §3.

## 4. STOP discipline (LOAD-BEARING per `docs/sprint_objective.md` §10)

20 STOP conditions in §10. The 5 LOAD-BEARING ones:

1. **NO per-UC-pair branch in SkillStateBus body** — STOP per §1.7 + design doc §10.7 Alternative B REJECTED. The bus uses REGISTRY-DRIVEN intersection (`IntakeFieldsRegistry.requiredFieldsFor(newUc).contains(canonicalFieldName(key))`); the bus body has NO `if (oldUc, newUc) ...` logic.
2. **NO new classifier for UC-switch detection** — STOP per design doc §10.3 + M2 §6 #5 fence. Detection rides on existing M1 Sprint 32 `alternate_candidate_use_cases` + Sprint 33 `discover_disambiguation_signals` projections; do NOT introduce `UcSwitchDetector.java` or analogous.
3. **NO per-UC-pair `state_inheritance` declarations** in Skill YAMLs — STOP per §1.7 + design doc §10.7 Alternative B REJECTED. Each Skill declares INHERIT / RESET / SOFT_SIGNAL dimensions independent of which prior UC's state is being transitioned.
4. **NO Tier-0 invariant added to `docs/runtime_freeze_and_risk_policy.md`** — STOP per M2 §6 #8 fence. C3 candidate evidence surfaces from Sprint 41 dispatcher landing; deliver-agent + human + Codex jointly classify at Sprint 41 close OR M2 close. Sprint 41 dev surfaces evidence; does NOT pre-elevate.
5. **NO scope creep into M3 surfaces** — STOP if tempted to touch `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` (M2 §6 #5); `INTAKE_UCS` set (M2 §6 #6); `escalation_reason` enum (M2 §6 #7); `IntakeFieldsRegistry` / `IntakeFieldExtractor` behaviour (M2 §6 #21; consume but don't modify).

Other STOPs cover: §1.3 / §1.4 boundary violations; tempted to elevate C3; tempted to edit governance / freeze / proposal / sprint-archive docs; tempted to use mocked-LLM as primary evidence for LLM-behaviour claim; tempted to draft M2 close housekeeping or M3 contract; tempted to modify `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`.

## 5. Execution plan (suggested order)

1. **Premise re-verification** (§3 commands above) → fill handoff §3.
2. **D-b verification + minor edits** (`Skill.java` + `StateInheritance.java` + `SkillLoader.java` allowlists) — confirm schema validation is wired for full `state_inheritance` declarations. If gaps exist, surgical edits.
3. **D-c Skill YAML declarations** (all 6 YAMLs EXTEND per §10.5 matrix) — verify against design doc §10.5 row-by-row.
4. **D-a `SkillStateBus.java` NEW** — implement public surface per §10.3; registry-driven intersection per §10.3 + §10.5 row C; injection dependencies `IntakeFieldsRegistry` + `ContextProjectionBuilder`. Spring graph linear.
5. **D-d `ContextProjectionBuilder.java` EXTEND** — new `prior_use_case_carry` slot per §10.4 (cap 3 + aging 4 + null-on-no-prior-switch); use Sprint 31/33 slots at lines 396-432 as SHAPE reference.
6. **D-e Integration** — `PhaseEvaluator.java` (or `ControlKernel.java`) Skill-switch detection + bus invocation. Constructor + `@Autowired` extension if needed.
7. **D-f Tests** — NEW SkillStateBusTest + UcSwitchStateInheritanceTest + PriorUseCaseCarryProjectionTest; EDIT PhaseEvaluatorSkillIntegrationTest if needed. Verify `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED.
8. **D-g Optional** — orchestration-shell teaching about `prior_use_case_carry` slot if dev judgement warrants. Surface as OQ.
9. **Full test suite** — `cd server && mvn test -q` — verify baseline ~1140-1155 / 1 inherited (or 0 if D-g organically resolves) / 0 / 2.
10. **§4.1 9-question self-walk** — walk anti-hardcode kernel on the Sprint 41 diff; fill handoff §6.
11. **Handoff §11 12-section archive** — author `docs/sprints/sprint-041-handoff.md` per §11 contract.
12. **Stage + commit** — single dev commit with §5 dev-authored files ONLY (deliver-agent files NOT staged; human bundles at sub-sprint close).

## 6. Handoff §11 12-section contract

Author `docs/sprints/sprint-041-handoff.md` per `docs/sprint_objective.md` §11 contract:

1. Context Pack — M2 milestone objective + Sprint 41 contract + Sprint 37 freeze design doc §10 + verified code shape at HEAD `9130abc` + feedback memory references.
2. Sub-sprint-objective recap.
3. Premise re-verification (15 premises spot-check; one paragraph per premise citing source).
4. Implementation walkthrough (D-a through D-g; cross-reference to design doc §10 mapping).
5. Sprint 37 freeze fidelity (§10.1-§10.11 row-by-row implementation cite).
6. §4.1 anti-hardcode self-walk on the Sprint 41 diff (9 questions; per-Q verdict; expected `approve`).
7. Open questions for deliver-agent + human (any §1.7 boundary case; any premise drift; any §10 template-vs-implementation editorial divergence; D-g optional teaching outcome; inherited test status; C3 Tier-0 candidate elevation timing question).
8. Anti-hardcode self-walk on Sprint 41 itself (expected `approve`).
9. Files changed table.
10. Layer-classification self-walk per Sprint 41 §8 stanza.
11. §5 Eval Acceptance bars (Sprint 41 hard gates; interactive eval smoke + bad-case suite OBSERVATION ONLY).
12. **Closure verdict placeholder** — leave for deliver-agent + human + Codex per `feedback_handoff_verdict_section_delegation.md`.

## 7. Reproducibility discipline

Every quantitative or code-citation claim in the handoff cites source path + line number + reproduction recipe per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`. Examples:
- "SkillStateBus.java post-Sprint-41 ~180 lines per `wc -l server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`".
- "Java baseline post-Sprint-41 = 1148 / 1 inherited / 0 / 2 per `cd server && mvn test -q 2>&1 | tail -5`".

Per Sprint 40 Codex non-blocking note (b): use **formatting-normalized content equivalence** (NOT "byte-for-byte modulo whitespace flattening") when describing migrated content; the precise term is verifiable.

## 8. Commit message template

When ready to commit (single dev commit with all §5 files):

```
sprint 41: UC switch + state preservation across Skill boundary (SkillStateBus + state_inheritance + prior_use_case_carry slot) (NEW M2 sub-sprint 5; LAST M2 sub-sprint)

Sprint 41 is the FIFTH and LAST sub-sprint of NEW Milestone M2 (Skill
Registry Abstraction + Wholesale Retroactive Externalization) and
the FOURTH implementation sub-sprint after Sprint 37 design freeze +
Sprint 38 SkillRegistry-core landing + Sprint 39 RESOLVE migration +
predicate migration + S1/S2 + unified dispatcher + Sprint 40 teaching
extraction. Sprint 41 implements Sprint 37 freeze decision (i) §10
as runtime code.

Ships:

- NEW server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java
  per design doc §10.3 public surface (applyOnSkillSwitch + inheritanceFor).
  Registry-driven intake_fields_partial intersection per §10.3 (no
  per-UC-pair branch). Spring @Component; injects IntakeFieldsRegistry +
  ContextProjectionBuilder.

- 6 Skill YAMLs EDIT (state_inheritance block per §10.5 matrix):
  discover_triage.yaml / confirm.yaml / resolve_faq_grounded_answer.yaml /
  resolve_intake_collect_and_handover.yaml / escalate.yaml / terminal.yaml.
  Each declares inherit + reset + soft_signal_via_projection per matrix.
  Sprint 39 RESOLVE Skills' forward-compat placeholders extended with
  full §10.5 shape.

- ContextProjectionBuilder.java EDIT (NEW prior_use_case_carry slot
  per §10.4; cap 3 + aging 4 + null-on-no-prior-switch; sibling to
  Sprint 31 + Sprint 33 slots).

- PhaseEvaluator.java EDIT (Skill-switch detection + SkillStateBus
  invocation when activeUseCase changes AND select(phase, newUc)
  returns different Skill). Detection rides on existing M1 Sprint 32 +
  Sprint 33 projections; no new classifier (M2 §6 #5 fence preserved).

- Skill.java / StateInheritance.java / SkillLoader.java VERIFIED
  schema validation wired for full state_inheritance declarations
  via VALID_STATE_KEYS + VALID_PROJECTION_SLOTS allowlists from
  Sprint 38-fix.

- NEW SkillStateBusTest (~15-25 tests; 5 invariant matrix rows ×
  inherit / reset / soft_signal_via_projection; intake_fields_partial
  registry intersection; Skill-switch trigger detection; no-op on
  same-Skill turn).

- NEW UcSwitchStateInheritanceTest (~10-15 integration tests;
  representative Skill switches with assertion of inherit / reset /
  soft-signal outcomes).

- NEW PriorUseCaseCarryProjectionTest (~10 tests; slot shape + cap +
  aging + null + no-per-UC-variation).

- EDIT PhaseEvaluatorSkillIntegrationTest (1-2 new tests for
  Skill-switch integration if applicable).

- Java baseline: 1105 → ~1140-1155 (~35-50 new Sprint 41 tests).
  Sprint71PartialIntakePersistenceTest 14/14 PRESERVED.

- 12-section dev handoff at docs/sprints/sprint-041-handoff.md.

Sprint 37 freeze fidelity: decision (i) §10 (§10.1-§10.11) honored
per handoff §5 row-by-row mapping. C3 Tier-0 candidate evidence
surfaces from SkillStateBus enforcement landing; deliver-agent +
human + Codex jointly classify at Sprint 41 close OR M2 close per
feedback_constitution_discipline_vs_planning_anticipation.md.

Constitution-compliance:
- §1.3 LLM ownership preserved (LLM owns prior_use_case_carry read
  decision: continue / ask / ignore).
- §1.4 Runtime ownership preserved (bus is capability-floor
  enforcement, NOT semantic-decision enforcement).
- §1.7 forbidden-list honored (no per-UC-pair branch in bus body;
  no per-UC-pair state_inheritance declarations; no per-UC
  variation in prior_use_case_carry slot construction).

M2 acceptance recalibration: bad-case suite + interactive eval are
observation only per docs/milestone_objective.md §5. Sprint 41 hard
gates: NEW SkillStateBus matches §10.3 surface; per-Skill
state_inheritance declarations match §10.5 matrix; intake_fields_partial
intersection is registry-driven (not per-UC-pair); prior_use_case_carry
slot matches §10.4; Skill-switch detection rides on existing M1
surfaces; M1 + Sprint 38 + Sprint 39 + Sprint 40 functional surfaces
preserved; Codex per-sub-sprint review verdict approve.

Sprint 41 closes M2 implementation track. M2 milestone close is the
next planning round (deliver-agent + human + Codex milestone-shared
cumulative review per iteration_governance.md §4.3).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

## 9. Bundle policy

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage these:

- `docs/sprint_objective.md` (deliver-agent owned)
- `docs/milestone_objective.md` (deliver-agent owned)
- `docs/10-handoff.md` (deliver-agent owned)
- `docs/action_bank.md` (deliver-agent owned)
- `docs/codex-findings.md` (Codex writes at sub-sprint close)
- `compact/sprint-041-*.md` (deliver-agent owned)
- Anything under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-040-*` (immutable archives)
- Anything in `docs/milestones/`
- Anything in `docs/foundational/` or `docs/current/` (governance)
- Anything in `docs/proposals/`
- Anything in `eval_interactive/`

Dev stages ONLY the §5 dev-authored files (~10-15 source/test files + 6 Skill YAMLs + ContextProjectionBuilder edit + PhaseEvaluator edit + 3 NEW test files + handoff). Deliver-agent + human bundle the rest at Sprint 41 close.

---

End of Sprint 41 dev prompt. Begin with §3 premise re-verification, then §5 execution plan in order. Surface any STOP-condition trigger immediately via handoff §7 before silently working around.
