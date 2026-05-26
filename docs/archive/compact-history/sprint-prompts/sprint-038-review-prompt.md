Paste the content below this line into a fresh Codex session after the Sprint 38 dev commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for Sprint 38 — the SECOND sub-sprint of **NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization, LLM-led Policy-bounded)** per `docs/milestone_objective.md`. Sprint 37 (the M2 sub-sprint 1 design freeze) closed PASS A on 2026-05-17 at commit `51c327c` per `docs/sprints/sprint-037-codex-review.md` (`decision: pass / blocking_count: 0`).

Sprint 38 is an **implementation sub-sprint** (the FIRST after the Sprint 37 design freeze; Sprint-25 / Sprint-32 shape; Java + YAML + tests). The dev landed commit `bd9d3f5` with: 5 new Java classes under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` (Skill record + Guardrail record + StateInheritance record + SkillRegistry Spring component + SkillLoader Spring component); 4 new Skill YAML files at `server/src/main/resources/skills/` (`discover_triage.yaml`, `confirm.yaml`, `escalate.yaml`, `terminal.yaml`); a `PhaseEvaluator.java` edit (constructor extended 10→11 args; new `composeSkillPhasePlan(...)` private helper; new SkillRegistry-first dispatch at `plan(...)`; 4 legacy phase branches DELETED — DISCOVER + CONFIRM + CLOSE + ESCALATE; RESOLVE-INTAKE + RESOLVE-FAQ branches PRESERVED for Sprint 39); 4 new test files at `server/src/test/java/com/gumtree/csagent/service/runtime/skill/` (SkillTest + SkillRegistryTest + SkillLoaderTest + SkillTestFixtures) and 1 new behavioural-equivalence integration test at `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java`; 19 existing PhaseEvaluator constructor call sites updated to inject `SkillTestFixtures.productionRegistry()`; the 12-section handoff at `docs/sprints/sprint-038-handoff.md`.

Per `iteration_governance.md` §4.3, Sprint 38 fires per-sub-sprint Codex review per **trigger #3 (new architectural surface — SkillRegistry mediates between LLM prompt context and tool dispatch)**. The Sprint 38 commit ships actual Java + YAML semantic surface, NOT docs-only — the §4.1 nine-question kernel applies to the IMPLEMENTATION DIFF (not a proposed design as in Sprint 37). The Sprint 37 design freeze at `docs/proposals/skill_registry_design.md` is the **architectural authority**; Codex's job is to verify the implementation honors the freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) verbatim and introduces no scope creep.

**Sprint 37 freeze decisions Sprint 38 implements (the in-scope subset):**

- **(a) §2 Skill data model** — 12-field record per design doc §2.1 + JSON Schema validation contract per §2.2.
- **(b) §3 SkillRegistry shape** — `select(phase, useCase) → Optional<Skill>` per §3.2 + SkillLoader fail-fast at Spring boot per §3.3.
- **(c) §4 PhaseEvaluator integration** — `composeSkillPhasePlan(...)` private helper per §4.1 + backward-compatible `PhasePlan` data shape per §4.2 + mixed Sprint 38/39 migration window per §4.3.
- **(e §6.2.1-§6.2.4) 4 simpler phase Skill YAML templates** — DISCOVER + CONFIRM + ESCALATE + CLOSE/TERMINAL.

**Sprint 37 freeze decisions Sprint 38 does NOT implement (out-of-scope; preserved for downstream sub-sprints):**

- **(d) §5 procedure-vs-guardrails responsibility split** — schema-defined; Sprint 38's 4 Skills all carry `guardrails: []` (the Guardrail record is exercised only by the SkillLoader negative-test surface). Sprint 39 populates concrete guardrail instances.
- **(e §6.2.5-§6.2.6) RESOLVE_FAQ + RESOLVE_INTAKE Skill templates** — Sprint 39 scope.
- **(f) §7 Sprint 23/31/33 teaching paragraph migration** from `system_prompt.txt` — Sprint 40 scope.
- **(g) §8 Sprint 6/7/11 + NEW S1 + NEW S2 predicate migration** — Sprint 39 scope (the MATERIAL FINDING: predicates already ship in `AgentRunLoopImpl.java` UNCHANGED in Sprint 38).
- **(h) §9 unified Skill terminal-predicate dispatcher** — Sprint 39 scope (depends on Skill `guardrails` populated).
- **(i) §10 session-level state model + state_inheritance enforcement** — Sprint 41 scope. `state_inheritance` field IS populated per design doc templates in the 4 Skill YAMLs but NOT yet enforced at session-state-bus boundary (no `SkillStateBus.java`; `ContextProjectionBuilder.java` UNCHANGED).
- **(j) §11 §4.1 nine-question walk on PROPOSED design** — contextual only; informs Codex's per-Q walk on the Sprint 38 diff.

**M2 §5 acceptance recalibration governs Sprint 38 close** per `docs/milestone_objective.md` §5 (2026-05-17, human-judgment recalibration of `iteration_governance.md` §5.6 primary-gate framing for THIS milestone): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION ONLY (NOT hard gate) for M2; primary gate = functional review + Java tests pass + Sprint 37 freeze decisions honored across implementation sub-sprints + per-sub-sprint Codex review verdict `approve`. **Codex's review prompt MUST NOT re-import the §5.6 bad-case-suite-primary default for Sprint 38**; per-Q walk + behavioural-equivalence verification + Sprint 37 freeze fidelity + Java baseline preservation are the load-bearing surfaces.

**Sprint-38-close pre-decisions (deliver-agent + human, 2026-05-17 post-handoff):** Before Codex dispatch, deliver-agent + human pre-decided the five OQs the dev surfaced in handoff §7. Codex INDEPENDENTLY verifies each pre-decision is sound where called out (see §9 below):

- **OQ-S38.1 (Sprint 37 design freeze §6.2.1 DISCOVER template vs legacy `PhaseEvaluator.java:411-448` editorial divergence; Sprint 38 chose LEGACY-verbatim):** **AGREE WITH DEV** (legacy-verbatim) per `doc_governance.md` "code ahead of docs" rule — the freeze template at §6.2.1 carries editorial paraphrasing (paragraph headings normalized; objective trailing period added) that does NOT represent a behaviour-change decision the freeze locked. The §6.4 behavioural-equivalence hard gate REQUIRES legacy-verbatim text (golden-string assertion would fail otherwise). The freeze template's intent is honored at field-shape level + the design doc itself is the editorial source that needs a fold-back update to match the legacy text at the next governance cadence; this is NOT a Sprint 38 implementation defect. Codex INDEPENDENT verdict REQUESTED — see §9 below for the verification surface. **If Codex disagrees** (e.g., the dev should have re-authored the golden strings to match the template's editorial rephrasing), the classification is **`out_of_scope_review` + recommend a future deliver-agent housekeeping commit that updates `docs/proposals/skill_registry_design.md` §6.2.1 template wording to match the legacy text** (or, alternatively, surfaces the discrepancy as an R-item for M2 close fold-back) — NOT `fix_required`. Rationale: the design doc is `status: proposal` (immutable per `doc_governance.md` once locked at Sprint 37 close); the divergence is an editorial drift in the FROZEN doc, not a substantive freeze contradiction; resolving it would require either editing the immutable freeze (governance violation) OR re-authoring 4 Skill YAMLs + 13 golden-string assertions to a template that the freeze itself did not justify substantively (scope expansion beyond Sprint 38 contract).
- **OQ-S38.2 (composer placement — `composeSkillPhasePlan(...)` inside PhaseEvaluator vs separate utility class):** **AGREE WITH DEV** (keep inside PhaseEvaluator for Sprint 38 per design doc §4.1 illustrative snippet; revisit if Sprint 39 grows the helper materially). **Informational** for Codex; not blocking; not pre-routed to OOSR.
- **OQ-S38.3 (`maxToolSteps` literal-2 default in `composeSkillPhasePlan(...)`):** **DEFER to Sprint 39 planning** — the 4 Sprint 38 Skills all set `max_tool_steps: 2` explicitly so the default never fires; Sprint 39 Skills (RESOLVE-FAQ at 4, RESOLVE-INTAKE at 3) will set explicit values. **Informational** for Codex; not blocking.
- **OQ-S38.4 (`SkillTestFixtures.productionRegistry()` coupling to production YAMLs):** **AGREE WITH DEV** — coupling to production YAMLs is the same shape as existing tests' coupling to `UseCaseRegistryService.getUseCase(...)` registry data; preserves behavioural equivalence "for free". **Informational** for Codex; not blocking.
- **OQ-S38.5 (C2 + C3 Tier-0 R-items continuation):** **CONFIRM** — both R-items carry forward from Sprint 37 close per `feedback_constitution_discipline_vs_planning_anticipation.md`; Sprint 38 dev surfaced NO new evidence to re-evaluate; C2 (guardrail refusal non-overridability) enforcement surface is Sprint 39; C3 (state-bus boundary enforcement) enforcement surface is Sprint 41; both DEFER recommendations preserved. **Informational** for Codex; if Codex DISAGREES (e.g., Sprint 38's `SkillRegistry.select` + `composeSkillPhasePlan` paths already constitute structural Tier-0 territory), route as `out_of_scope_review` per Sprint 37 §8 precedent (Tier-0 elevation / deferral is deliver-agent + human authority per M2 §10 stop condition #1).

**OQ-S38.1 disagreement-routing rationale (encoded above):** the freeze doc at `docs/proposals/skill_registry_design.md` is `status: proposal` and frozen at Sprint 37 close per `doc_governance.md`; editing it now (to either re-author the template or annotate the divergence) requires a SEPARATE deliver-agent governance round, not a Sprint 38 dev fix-iteration. The Sprint 38 implementation's choice to honor LEGACY-verbatim is consistent with `doc_governance.md` "code ahead of docs" decision rule (the running, reviewed code at HEAD `51c327c` is the source of truth; the freeze template's editorial paraphrasing is doc drift that gets folded back at the normal cadence, not a Sprint 38 dev surface).

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip; the prompt's later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / §4.3 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — the NEW M2 milestone north star (status `current`; supersedes OLD M2-Skill). Read: §1 5-sub-sprint layer breakdown (Sprint 38 layer = `prompt_projection` + `skill_state` + Runtime-owned PhasePlan composition); §2 goal (Skill Registry abstraction + wholesale retroactive externalization); §3 Sprint 38 row (SkillRegistry core + 4 simpler phase Skills migration); §5 acceptance recalibration (Alice + eval = OBSERVATION, NOT gate); §6 hard fences (22 items; esp. #1 no per-UC-branch if-else in Skill body, #4 verbatim S1 authorization carry-forward for Sprint 39, #20 no OLD design deletion, #21 M1 functional-surface preservation, #22 Alice observation).
3. **`docs/sprint_objective.md`** — the Sprint 38 contract. Read: §1 sub-sprint class (implementation, semantic-touching multi-layer); §2 Goal (4 deliverables D-a/D-b/D-c/D-e enumerated); §3 Non-goals (explicit list of what Sprint 38 does NOT touch); §4 Premise check (12 items deliver-agent verified; dev re-verified at session start per handoff §3); §5 Files in scope (single-commit list); §6 Hard fences (31 items); §7 Bundle policy; §8 §7 stanza; §9 Success metrics (hard gates + observations); §10 Stop conditions (20 items); §11 Handoff contract (12 sections).
4. **`docs/sprints/sprint-038-handoff.md`** — the dev's archive. Compare §9 (Files changed) against §5 of the objective; verify no scope creep. §1.2 code-shape table verified at HEAD `51c327c` carries line numbers + post-Sprint-38 PhaseEvaluator line-count claim. §3 walks the 12 §4 premises (12/12 PASS reported). §4 implementation walkthrough (Skill / Guardrail / StateInheritance records + SkillRegistry + SkillLoader Spring components + `composeSkillPhasePlan(...)` helper + 4 legacy phase branches DELETED + 4 Skill YAMLs verbatim from legacy `PhaseEvaluator.java` content + 4 new test files + 19 existing PhaseEvaluator constructor call sites updated). §5 Sprint 37 freeze fidelity (table cross-referencing each freeze decision to the implementation file + line range). §6 §4.1 self-walk verdict `approve` per Q1-Q9. §7 5 OQs (OQ-S38.1 is the load-bearing one; OQ-S38.2/3/4/5 informational). §8 §4.1 self-walk on Sprint 38 diff (duplicate of §6 — same verdict). §9 files changed table. §10 layer-classification self-walk per §8 stanza. §11 §5 acceptance bars adapted per M2 §5 recalibration. §12 closure verdict placeholder.
5. **`docs/proposals/skill_registry_design.md`** — Sprint 37 freeze (immutable per `doc_governance.md`); architectural authority for Sprint 38. Read sections referenced by Sprint 38 in scope:
   - §2 (decision (a) — Skill data model) — verify Sprint 38 `Skill.java` record fields match §2.1; verify SkillLoader schema validation per §2.2.
   - §3 (decision (b) — SkillRegistry shape) — verify Sprint 38 `SkillRegistry.java` `select(phase, useCase) → Optional<Skill>` exact-match-then-wildcard-then-Optional.empty() semantics per §3.2; verify SkillLoader fail-fast at Spring boot per §3.3.
   - §4 (decision (c) — PhaseEvaluator integration) — verify Sprint 38 `composeSkillPhasePlan(...)` private helper per §4.1; verify `plan(...)` dispatch order (SkillRegistry first; legacy fall-through for RESOLVE branches) per §4.3 mixed Sprint 38/39 migration window.
   - §6.2.1 (DISCOVER template) — verify Sprint 38 `discover_triage.yaml` against §6.2.1 template (note: dev chose LEGACY-verbatim per OQ-S38.1; the §6.2.1 template carries editorial paraphrasing — see §9 below for OQ-S38.1 verification).
   - §6.2.2 (CONFIRM template) — verify Sprint 38 `confirm.yaml` against §6.2.2 template + legacy `PhaseEvaluator.java:462-487`.
   - §6.2.3 (ESCALATE template) — verify Sprint 38 `escalate.yaml` against §6.2.3 template + legacy `PhaseEvaluator.java:513-542`; verify `create_case_controlled` INTENTIONALLY excluded per §6.2.3 note + Codex 1.8 / `customer_service_tool_spec_v0_2.yaml` runtime-only visibility rule per PhaseEvaluator inline comment at legacy lines 514-521.
   - §6.2.4 (CLOSE template) — verify Sprint 38 `terminal.yaml` against §6.2.4 template + legacy `PhaseEvaluator.java:492-509`; verify `applicable_phases: [CLOSE]` per OQ-7.1 default (keep phase enum unchanged at Sprint 38; file name `terminal.yaml` carries M3+ rename intent).
   - §6.4 behavioural-equivalence test pattern — verify Sprint 38 `PhaseEvaluatorSkillIntegrationTest.java` implements per §6.4.
   - **§6.2.5 + §6.2.6 (RESOLVE-INTAKE + RESOLVE-FAQ templates) — CONTEXTUAL only; verify NOT shipped in Sprint 38 (Sprint 39 scope).**
   - §5 (decision (d)) — schema-defined; verify Sprint 38 `Guardrail.java` record matches §5.2 DSL + Skill `guardrails` field present in `Skill.java` + the 4 Sprint 38 Skill YAMLs all declare `guardrails: []` (the negative-control test surface in `SkillLoaderTest` exercises Guardrail validation).
   - §10 (decision (i)) — schema-defined; verify Sprint 38 `StateInheritance.java` record matches §10.2 schema + 4 Skill YAMLs populate `state_inheritance` per §6.2.1-§6.2.4 templates. Enforcement NOT in Sprint 38 (Sprint 41 scope per `SkillStateBus.java`).
   - **§7 / §8 / §9 — CONTEXTUAL only; verify NOT touched in Sprint 38 (Sprint 39 / Sprint 40 / Sprint 41 scopes).**
   - §11 (§4.1 nine-question walk on PROPOSED design) — contextual; informs Codex's own §4.1 walk on the Sprint 38 diff.
   - §12 (5 Tier-0 candidates) — contextual; C1 REJECTED / C2 + C3 QUALIFIED-DEFER (R-items in `action_bank.md` §5.2 from Sprint 37 close) / C4 + C5 NOT-A-CANDIDATE. Sprint 38 confirms continuation (OQ-S38.5 in §9 below).
6. **`docs/sprints/sprint-037-objective.md`** — Sprint 37 contract (archived). Read for §6 hard fences carried forward (esp. the 19-item list); for §11 handoff contract that Sprint 38 inherits the shape of.
7. **`docs/sprints/sprint-037-handoff.md`** — Sprint 37 dev's archive. Read §1.2 code shape table (line numbers verified at HEAD `51c327c`; Sprint 38 inherits and re-verifies); §3 premise re-verification (10 items + 1 refinement on `system_prompt.txt` 101-line shape — Sprint 38 inherits); §6 Tier-0 candidate outcome (5 candidates; C2 + C3 QUALIFIED-DEFER → R-items opened); §7 11 OQs (esp. OQ-7.1 CLOSE/TERMINAL enum kept unchanged at Sprint 38 default; OQ-7.11 `request_handover` decision tree AGREE WITH DEV → preserved through Sprint 38).
8. **`docs/sprints/sprint-037-codex-review.md`** — Codex per-sub-sprint review of Sprint 37 (`decision: pass / blocking_count: 0` at commit `51c327c`); §10 deferred / non-blocking notes (esp. MATERIAL FINDING preservation through Sprint 38 + Sprint 33 cue/slot split preservation + premise #7 informing Sprint 40 not Sprint 38 + C2/C3 R-items for M3+ revisit).
9. **`docs/current/iteration_governance.md`** — §1.3 (LLM-owned: user goal / issue relation / UC hypothesis / drift / topic shift / next action / escalation posture / response strategy / natural customer-facing wording — verify the 4 Sprint 38 Skill `procedure` bodies are LLM-soft teaching; verify no Skill prescribes customer-facing language or per-step argument values); §1.4 (Runtime-owned: tool schema / capability / permission / PII / safety floor / grounding floor / budget / timeout / idempotency / persistence / trace and eval contract — verify Sprint 38 `PhasePlan.allowedTools` composed from `skill.toolsRequired` still enforced by existing `ToolDispatcher.validateAgainstPlan`); §1.7 (Forbidden — verify Sprint 38 has NO per-UC-branch if-else in any Skill YAML body, no eval phrase encoding, no LLM-vs-Java boundary shift); §3.2 (`prompt_projection` Q3 + `skill_state` Q4 layer classification per Sprint 38 stanza); §4.1 nine-question kernel (Codex independently re-walks against the Sprint 38 diff); §4.2 sprint-close header convention; §4.3 trigger #3 (new architectural surface — Sprint 38 fires; verify the SkillRegistry abstraction qualifies); §5 / §5.5 / §5.6 acceptance bars (Sprint 38 + M2 §5 recalibration: bad-case suite OBSERVATION not gate for THIS milestone — verify Codex does NOT re-import the §5.6 default); §7 sprint-objective stanza (verify Sprint 38 §8 stanza per the objective); §8 milestone framework.
10. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set. Verify Sprint 38 dev commit ADDS no Tier-0 invariant (M2 §6 #8 fence preserved; C2 + C3 R-items not elevated; Sprint 38 architectural refactor does not introduce structural absolute beyond what existing `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan` enforcement already provides per C1 REJECTED).
11. **`docs/current/faq_grounding_contract.md`** — grounding-floor contract. Verify Sprint 38 does NOT extend the grounding floor (S1 `must_cite_source` is Sprint 39 scope per design doc §8.2.4 + M2 §6 #4 verbatim authorization preserved for Sprint 39).
12. **`compact/sprint-038-dev-prompt.md`** — what the dev was authorized to do vs what landed (cross-check for in-scope vs out-of-scope edits). The dev prompt §2 walks 4 deliverables D-a / D-b / D-c / D-e; §3 the §4.1 self-walk; §4 31 hard fences; §5 20 stop conditions; §6 bundle policy; §7 Java baseline note (post-Sprint-37 baseline 983; expected post-Sprint-38 ~1013-1023 — actual at HEAD `bd9d3f5` is 1028 per handoff §1.6).
13. **`docs/sprints/sprint-037-objective.md`** (already read at step 6 above) is sufficient for Sprint 37 context cross-reference; do not re-read.
14. **Code source files for cited-line spot-checking** at HEAD `bd9d3f5` (read on demand during §3 + §4 + §5 + §6 verification, NOT end-to-end):
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (post-Sprint-38 ~1500-1530 lines per Sprint 38 handoff §1.6 / verify with `wc -l`; Sprint 38 handoff §4.3 claims 1500; pre-Sprint-38 1628 per Sprint 38 handoff §1.2 / Sprint 37 close commit `51c327c`. NOTE: deliver-agent's git-shape spot check post-commit measured 1529 — the handoff §4.3 "1500" claim is approximate. Verify via `wc -l`; surface as minor reproducibility note if material).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (NEW; ~124 lines; record with 15 fields per design doc §2.1 + `@JsonCreator` snake_case mapping + `appliesTo(phase, useCase)` selection helper).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Guardrail.java` (NEW; ~56 lines; typed record per design doc §5.2 DSL: `type` + `onFail` + `parameters`).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` (NEW; ~59 lines; record per design doc §10.2: `inherit` / `reset` / `softSignalViaProjection`).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` (NEW; ~123 lines; Spring `@Component`; `select(phase, useCase) → Optional<Skill>` per design doc §3.2).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (NEW; ~228 lines; Spring `@Component`; loads `classpath:/skills/*.yaml`; fail-fast schema validation per design doc §3.3 / §2.2).
    - `server/src/main/resources/skills/discover_triage.yaml` (NEW; ~30 lines; DISCOVER Skill — `applicable_use_cases: ["*"]`; `procedure` carries legacy `PhaseEvaluator.java:411-448` content INCLUDING Sprint 7 §I0 weak-candidate cue + Sprint 33 ad-status disambiguation cue per the cue-side of the Sprint 33 cue/slot split — slot-side stays at `system_prompt.txt:36-43` for Sprint 40).
    - `server/src/main/resources/skills/confirm.yaml` (NEW; ~29 lines; CONFIRM Skill — `applicable_use_cases: ["*"]`).
    - `server/src/main/resources/skills/escalate.yaml` (NEW; ~28 lines; ESCALATE Skill — `applicable_use_cases: ["*"]`; `tools_required: [request_handover, record_outcome]` — `create_case_controlled` INTENTIONALLY excluded).
    - `server/src/main/resources/skills/terminal.yaml` (NEW; ~25 lines; CLOSE Skill — `applicable_phases: [CLOSE]`; `applicable_use_cases: ["*"]`; `tools_required: [record_outcome]`).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTest.java` (NEW; ~141 lines; 8 unit tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillRegistryTest.java` (NEW; ~146 lines; 11 unit tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillLoaderTest.java` (NEW; ~245 lines; 13 unit tests — 1 happy + 12 negative).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` (NEW; ~21 lines; `productionRegistry()` helper).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` (NEW; ~341 lines; 13 behavioural-equivalence tests pinned to GOLDEN pre-migration legacy strings; 3 representative UCs × 4 migrated phases + 1 legacy-RESOLVE-INTAKE preservation check).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (UNCHANGED in Sprint 38; 787 lines; Sprint 6/7/11 predicates at 628-651 / 690-734 / 745-759; dispatch sites at 317 / 356 / 413. **Sprint 39 migrates.**).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (UNCHANGED; 222 lines).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (UNCHANGED; 174 lines; `UseCaseDefinition` record at 166-173 = 6 fields).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (UNCHANGED; 1161 lines; Sprint 41 adds NEW `prior_use_case_carry` slot).
    - `server/src/main/resources/prompts/system_prompt.txt` (UNCHANGED; 101 lines; Sprint 40 extracts teaching paragraphs).
    - `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` (UNCHANGED; 146 lines; 11-field record).
    - `server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java` — read to verify `validateAgainstPlan(...)` continues to enforce `PhasePlan.allowedTools` whitelist post-Sprint-38 (C1 REJECTED preserved).

## 2. Scope-discipline gate (BLOCKING)

Verify the dev commit at `bd9d3f5` touches ONLY the surfaces enumerated in `docs/sprint_objective.md` §5 (Sprint 38 contract files in scope). Run:

```bash
git -C /Users/caoruixin/projects/csagent-latest diff 51c327c..bd9d3f5 --stat
```

Expected file list (any extra file outside this list is a **BLOCKING scope violation**):

- **NEW** Java records under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/`: `Skill.java` (124 lines), `Guardrail.java` (56 lines), `StateInheritance.java` (59 lines).
- **NEW** Spring components under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/`: `SkillRegistry.java` (123 lines), `SkillLoader.java` (228 lines).
- **NEW** Skill YAMLs under `server/src/main/resources/skills/`: `discover_triage.yaml` (30 lines), `confirm.yaml` (29 lines), `escalate.yaml` (28 lines), `terminal.yaml` (25 lines).
- **EDIT** `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (1628 → ~1500-1530 lines; constructor extended 10→11 args; `composeSkillPhasePlan(...)` helper added; `plan(...)` dispatch updated to query `skillRegistry.select(...)` first; 4 legacy phase branches DELETED — DISCOVER 397-457 + CONFIRM 462-487 + CLOSE 492-509 + ESCALATE 513-542; RESOLVE-INTAKE 552-596 + RESOLVE-FAQ 598-665 PRESERVED).
- **NEW** test files under `server/src/test/java/com/gumtree/csagent/service/runtime/skill/`: `SkillTest.java` (141 lines), `SkillRegistryTest.java` (146 lines), `SkillLoaderTest.java` (245 lines), `SkillTestFixtures.java` (21 lines).
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` (341 lines; 13 tests pinned to GOLDEN pre-migration legacy strings).
- **EDIT** 19 existing test files (constructor-injection update for the new `SkillRegistry` arg): listed verbatim in the dev handoff §9 table — verify each diff is one-or-two-line constructor delta + the `SkillTestFixtures` import where needed; no semantic test logic changes.
- **NEW** `docs/sprints/sprint-038-handoff.md` (324 lines; 12-section dev archive).

**BLOCKING scope violations** (any presence in the dev commit at `bd9d3f5`):

- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` RESOLVE-INTAKE branch (legacy 552-596) or RESOLVE-FAQ branch (legacy 598-665). Sprint 39 scope per Sprint 38 §6 #1.
- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`. Sprint 6/7/11 predicates UNCHANGED per Sprint 38 §6 #2; Sprint 39 migrates.
- ANY edit to `server/src/main/resources/prompts/system_prompt.txt`. Sprint 40 scope per Sprint 38 §6 #3.
- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`. Sprint 41 scope per Sprint 38 §6 #6.
- ANY edit to `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` OR `UseCaseRegistryService.java`. UNCHANGED per Sprint 38 §6 (M1 functional-surface preservation).
- ANY edit to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java`. M2 §6 #5 fence + Sprint 38 §6 #7.
- ANY edit to `INTAKE_UCS` set at legacy `PhaseEvaluator.java:30` OR `escalation_reason` enum at legacy 33-63. M2 §6 #6 + #7 + Sprint 38 §6 #8 + #9.
- ANY edit to `docs/runtime_freeze_and_risk_policy.md`. M2 §6 #8 + Sprint 38 §6 #10 fence.
- ANY edit to `docs/current/iteration_governance.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md`. Constitution-discipline; Sprint 38 §6 #11 fence.
- ANY edit to `docs/proposals/skill_registry_design.md`. Sprint 37 freeze IMMUTABLE per `doc_governance.md` + Sprint 38 §6 #12 fence.
- ANY edit to other docs under `docs/foundational/` or `docs/current/` (other than the Sprint 38 handoff at `docs/sprints/sprint-038-handoff.md`). Sprint 38 §6 #13.
- ANY edit to sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-037-*`. Sprint 38 §6 #14.
- ANY edit to milestone archives under `docs/milestones/`. Sprint 38 §6 #15.
- ANY edit to deliver-agent-owned files: `docs/milestone_objective.md`, `docs/sprint_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-038-*.md`. Sprint 38 §6 #16 + #17 + #18.
- ANY edit to `eval_interactive/case_specs/case_families/<existing>/` or `eval_interactive/case_specs_shadow/case_families/<existing>/` or `eval_interactive/eval_interactive/` or `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` or `eval_interactive/case_spec_overrides.yaml`. Sprint 38 §6 #19-#23.

**Run** `git -C /Users/caoruixin/projects/csagent-latest show --stat bd9d3f5` to enumerate the touched files; any scope violation = Finding #1 with the file path + line range quoted.

## 3. §4.1 Anti-Hardcode kernel walk (against the Sprint 38 diff)

Run the §4.1 9-question kernel verbatim as loaded from `iteration_governance.md` §4.1. **Sprint 38 is NOT exempt** (semantic-touching: introduces a NEW architectural surface — SkillRegistry mediates between LLM prompt context and tool dispatch per §4.3 trigger #3).

Walk each of the nine questions against the **Sprint 38 IMPLEMENTATION DIFF** (not against the Sprint 37 design doc — the dev handoff §6 walks against the diff; Codex independently re-walks). Per-Q load-bearing surfaces to inspect:

- **Q1 keyword / regex / if-else / enum / per-UC matrix for semantic decision?** Walk all Sprint 38 surfaces:
  - **`SkillRegistry.select(phase, useCase)`** is a (phase, useCase) lookup with exact-match-then-wildcard semantics, NOT a branch table. Verify: 4 Sprint 38 Skills all declare `applicable_use_cases: ["*"]` so no per-UC routing exists; no Sprint 38 Skill carries a per-UC `applicable_use_cases` list with multiple specific UCs that would constitute a per-UC matrix.
  - **`composeSkillPhasePlan(...)`** composes Skill fields into PhasePlan without per-UC-branch logic; verify the helper has zero per-UC enum reference or per-UC if-else.
  - **The 4 Skill YAML `procedure` texts** are principle-level CONTENT migrations from legacy `PhaseEvaluator.java:397-542` (CONFIRM = interpret-satisfaction; ESCALATE = finalize-handover; CLOSE = polite-closing-+-record-outcome; DISCOVER carries Sprint 7 §I0 weak-candidate cue + Sprint 33 ad-status disambiguation cue verbatim from `PhaseEvaluator.java:418-431` + `:432-448`). Codex M1 (Sprint 34 close) and Sprint 37 (Sprint 37 Codex review §11.1) independently verified the DISCOVER cue content is **classification guidance** (naming UCs as INFERENCE TARGETS for the LLM — UC-F / UC-B / UC-C / UC-D), NOT per-UC-branch routing by ACTIVE UC. Sprint 38 preserves this exactly. Verify: read `server/src/main/resources/skills/discover_triage.yaml` `procedure` text; cross-reference to legacy `PhaseEvaluator.java:411-448` at parent commit `51c327c` (via `git show 51c327c:server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java | sed -n '397,457p'`); confirm content equivalence; confirm classification-guidance framing preserved.
  - **The 4 Skill YAML `state_inheritance` blocks** populate `inherit` / `reset` / `soft_signal_via_projection` at STATE-DIMENSION granularity (e.g., `inherit: [customer_context]` for DISCOVER; `inherit: [customer_context, accumulated_tool_results]` for CONFIRM / ESCALATE / CLOSE; `soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]` for DISCOVER), NOT per-UC-pair granularity. Verify: no per-UC-pair declaration (e.g., `if prior_uc == UC-A and new_uc == UC-C then inherit X`) exists in any Sprint 38 Skill YAML.
  - **The 4 Skill YAML `tools_required` lists** are tool-set declarations (e.g., `[search_knowledge, classify_use_case]` for DISCOVER; `[record_outcome, request_handover]` for CONFIRM; `[request_handover, record_outcome]` for ESCALATE; `[record_outcome]` for CLOSE), NOT per-UC tool-set branches. Verify.
  - **The 4 Skill YAML `valid_terminal_outcomes` lists** are terminal-state declarations (e.g., `[CLARIFICATION_NEEDED, FINAL_ANSWER, ESCALATE]` for DISCOVER), NOT per-UC terminal-state branches. Verify.
  - **`composeSkillPhasePlan(...)`'s `maxToolSteps` literal-2 fallback** (per OQ-S38.3): the literal 2 is a value DEFAULT, NOT a per-UC-branch decision. Verify the helper does NOT branch on UC to pick a different `maxToolSteps` default; it just falls back to literal-2 if the Skill field is null.
  - Expected: NO per-UC-branch if-else in any Sprint 38 surface. Dev handoff §6 Q1 verdict `NO`. Codex independently confirms or surfaces dissent with the diff snippet quoted.
- **Q2 Tier-0 justification?** N/A — Sprint 38 adds no Tier-0 invariant; C2 + C3 R-items (Sprint 37 close DEFER) carry forward unchanged per OQ-S38.5. Verify: `docs/runtime_freeze_and_risk_policy.md` §1 + §2 unchanged at HEAD `bd9d3f5` (run `git diff 51c327c..bd9d3f5 -- docs/runtime_freeze_and_risk_policy.md` returns empty).
- **Q3 soft signal achievable?** N/A — Sprint 38 is architectural refactoring; existing M1 Sprint 32 `alternate_candidate_use_cases` slot + Sprint 33 `discover_disambiguation_signals` slot continue to operate (preserved in `ContextProjectionBuilder.java` UNCHANGED); Sprint 38's DISCOVER Skill `state_inheritance.soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]` DECLARES the existing slots at the Skill level for Sprint 41's eventual state-bus enforcement. NEW `prior_use_case_carry` slot is Sprint 41 scope.
- **Q4 visible-eval / trace phrasing / CaseSpec id encoded into runtime / prompt / judge?** Read the 4 Skill YAML bodies. Verify: NO `alice_*` CaseSpec id, NO `cs_*` CaseSpec id, NO trace phrasing, NO bad-case-suite reference text encoded in any Skill YAML. The 4 Skill YAML bodies carry ONLY legacy `PhaseEvaluator.java` Java-string content + state-inheritance metadata.
- **Q5 semantic ownership shift LLM → Java?** Verify: §1.3 LLM ownership preserved (Skill `procedure` is LLM-soft teaching at principle-level; no Skill prescribes customer-facing language or per-step argument values); §1.4 Runtime ownership preserved (`PhasePlan.allowedTools` continues to be enforced by `ToolDispatcher.validateAgainstPlan` post-migration — read `server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java` to confirm the enforcement path); tool schema unchanged; capability / permission boundary unchanged.
- **Q6 if-else in prompt?** Read each of the 4 Skill `procedure` texts. Verify: principle-level teaching (CONFIRM interprets customer satisfaction; ESCALATE finalizes handover; CLOSE closes politely + records outcome; DISCOVER carries Sprint 7 §I0 + Sprint 33 cues that are classification-guidance, not active-UC if-else). The `request_handover` decision tree at `system_prompt.txt:54-101` is UNCHANGED in Sprint 38 (Sprint 38 §6 #3 fence; the tree was Sprint 37 OQ-7.11 reviewed and AGREE WITH DEV — situation-to-enum, not per-UC-pair); Sprint 38 inherits the OQ-7.11 verdict.
- **Q7 tool schema / capability / PII / grounding floor preserved?** Verify: tool schema unchanged (no `customer_service_tool_spec_v0_2.yaml` edit; the 4 Sprint 38 Skills' `tools_required` lists name only existing tools); PII / safety floor unchanged (Tier-0 invariants in `runtime_freeze_and_risk_policy.md` §1 / §2 unchanged); grounding floor NARROWLY unchanged in Sprint 38 (S1 `must_cite_source` Skill-bounded extension is Sprint 39 scope; M2 §6 #4 verbatim authorization carried forward).
- **Q8 generalization coverage?** Per Sprint 38 handoff §10 (target / neighbor / negative / shadow). Verify against actual Sprint 38 test files:
  - **Target**: 4 migrated phases × 3 representative UCs = 12 behavioural-equivalence cases in `PhaseEvaluatorSkillIntegrationTest.java` pinned to GOLDEN pre-migration legacy strings (read the file; verify the test assertions reference inline String constants extracted from legacy `PhaseEvaluator.java:397-542` at parent `51c327c`).
  - **Neighbor**: 2 unmigrated RESOLVE branches (RESOLVE-INTAKE 552-596 + RESOLVE-FAQ 598-665) PRESERVED verbatim. Run targeted regression tests to verify: `Sprint71PartialIntakePersistenceTest`, `PhaseEvaluatorFaqMissFallbackTest`, `PhaseEvaluatorPlanTest`, `Sprint11ProgressiveResolveTest`, `Sprint7IntakeStateTest`, `Sprint7CandidateUseCasesProjectionTest` — all green per handoff §11 row "Neighbor cases no regression". Codex independently re-runs from a clean checkout.
  - **Negative**: 12 negative `SkillLoaderTest.java` cases covering schema fail-fast paths (missing/blank name; missing procedure; unknown phase; empty UC list; wildcard-mixed-with-explicit; unknown terminal outcome; unknown state key; unknown projection slot; unknown on_fail mode; malformed YAML). Codex spot-checks 3-5 negative cases at random.
  - **Shadow**: N/A per M2 §5 acceptance recalibration (Sprint 38 architectural; LLM-behaviour observation OBSERVATION ONLY).
- **Q9 rollback / sunset?** N/A — Skill Registry abstraction is permanent. If Sprint 38 evidence shows the abstraction wrong, legacy branches can be restored via `git revert bd9d3f5` (the 4 deleted branches are recoverable from the parent commit `51c327c`).

**Per `feedback_review_prompt_kernel_inline_vs_loader.md`**: the 9 questions + 4 verdicts are loaded verbatim from `iteration_governance.md` §4.1; this section §3 carries only the per-Q sprint-specific surfaces to inspect for Sprint 38. Codex's verdict pulls from the canonical kernel, not a paraphrase.

**Expected verdict on the §4.1 kernel walked against the Sprint 38 diff: `approve`** (architectural refactoring; behavioural equivalence preserved; no per-UC-branch hardcode introduced; tool-whitelist semantics unchanged; LLM-vs-Java boundary preserved). Codex may surface per-Q informational dissent if any; raise as BLOCKING only if Q1 / Q5 / Q6 / Q7 yields a substantive boundary violation in the implementation diff.

## 4. §1.7 boundary check on the Sprint 38 implementation (BLOCKING if a violation is found)

§1.7 forbidden list applies to the Sprint 38 IMPLEMENTATION DIFF. Specifically verify:

- **The 4 Skill YAML `procedure` bodies** do NOT encode "raw eval phrases into Java or prompt" — read each verbatim from the YAML file; confirm no CaseSpec id, no trace phrasing, no `alice_*` reference, no bad-case-suite text. The 4 procedure texts are CONTENT migrations from legacy `PhaseEvaluator.java:397-542`; verify content equivalence + content boundary.
- **The 4 Skill YAML `applicable_use_cases` lists** are all `["*"]` (wildcard) per design doc §6.2.1-§6.2.4 templates — no per-UC enumeration. Read each YAML; verify `applicable_use_cases: ["*"]` for all 4 Skills. Any specific-UC enumeration would be a §1.7 violation.
- **The 4 Skill YAML `state_inheritance` blocks** are STATE-DIMENSION-keyed, NOT per-UC-pair-keyed. Read each; verify `inherit` / `reset` / `soft_signal_via_projection` lists name state slots (e.g., `customer_context`, `accumulated_tool_results`, `alternate_candidate_use_cases`, `discover_disambiguation_signals`), NOT UCs.
- **The 4 Skill YAML `guardrails` blocks** are all empty (`guardrails: []`) per design doc §6.2.1-§6.2.4 templates — Sprint 38's 4 simpler phase Skills have no predicates; Sprint 39 populates concrete instances in the RESOLVE Skills. Verify `guardrails: []` for all 4.
- **`composeSkillPhasePlan(...)`** is principle-level: maps Skill fields to PhasePlan fields without per-UC logic. Read the helper at `PhaseEvaluator.java` (post-Sprint-38, around the location named in handoff §4.3); confirm no per-UC enum branch, no per-UC string match, no per-UC template substitution applied (the 4 Sprint 38 Skills are non-parameterized — `applicable_use_cases: ["*"]`; template-substitution surface is reserved for Sprint 39 RESOLVE Skills).
- **`SkillRegistry.select(phase, useCase)`** is a lookup with `exact-UC-then-wildcard-then-Optional.empty()` semantics — NOT a per-UC enum routing. Read `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java`; verify the selection logic implements design doc §3.2 semantics exactly.
- **`Skill.appliesTo(phase, useCase)` selection helper** (per dev handoff §4.1) is principle-level: returns true if (a) phase is in `applicablePhases` AND (b) `useCase` is in `applicableUseCases` OR `applicableUseCases` contains `"*"`. Read `Skill.java`; verify no per-UC enum branch.
- **Sprint 38's `Skill.java` 15-field record** preserves the design doc §2.1 field set without expanding the LLM-vs-Java boundary (per §1.3 + §1.4). Verify: no field encodes a Runtime-floor enforcement beyond what the existing `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan` already enforce; no field prescribes customer-facing language; no field hard-encodes per-step argument values.
- **`SkillLoader.validate(...)` schema enforcement** at `SkillLoader.java` (per dev handoff §4.2) covers required field presence + enum-subset checks; the validation is at LOAD time (fail-fast at Spring boot), not at runtime — Runtime-owned floor is preserved per §1.4 (the LLM cannot bypass the schema at runtime; the abstraction enforces by construction).
- **The 4 deleted legacy phase branches** (DISCOVER 397-457 + CONFIRM 462-487 + CLOSE 492-509 + ESCALATE 513-542) are RELOCATED to YAML, not REDESIGNED. Verify by comparing post-Sprint-38 Skill YAML body to parent `51c327c` legacy branch content (use `git show 51c327c:server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java | sed -n '<range>p'`) for each migrated phase. Spot-check at least one phase verbatim.
- **The 2 preserved legacy RESOLVE branches** at post-Sprint-38 line ranges (handoff §4.3 claims renumbered to ~417-460 for RESOLVE-INTAKE and ~463-530 for RESOLVE-FAQ) match parent `51c327c` byte-for-byte modulo line numbers shifting from the 4 deleted branches above. Verify via `git diff 51c327c..bd9d3f5 -- server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` — the diff should show only deletions for the 4 migrated phases + constructor / dispatch / helper additions; no edits to the 2 RESOLVE branch bodies. ANY non-trivial RESOLVE branch edit is a BLOCKING fence violation (Sprint 39 scope).

If ANY Sprint 38 implementation surface encodes a §1.7 violation, raise as a **BLOCKING finding** and recommend `fix_required`.

## 5. Hard-fence verification

- **Sprint 38 contract §6 hard fences (31 items):** verify each is honored at HEAD `bd9d3f5`. Walk in order:
  - #1 No edit to `PhaseEvaluator.java` RESOLVE-INTAKE (legacy 552-596) or RESOLVE-FAQ (legacy 598-665) branches — verify via `git diff` per §4 above.
  - #2 No edit to `AgentRunLoopImpl.java` — verify via `git diff 51c327c..bd9d3f5 -- server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (returns empty).
  - #3 No edit to `system_prompt.txt` — verify via `git diff 51c327c..bd9d3f5 -- server/src/main/resources/prompts/system_prompt.txt` (returns empty).
  - #4 No NEW S1 `must_cite_source` or NEW S2 `intake_complete_required` predicates — verify: the 4 Sprint 38 Skill YAMLs all declare `guardrails: []`; the Guardrail record exists but is exercised only by SkillLoader negative-test surface; no S1 / S2 predicate logic added in Java.
  - #5 No unified Skill terminal-predicate dispatcher (`SkillGuardrailDispatcher.java`) — verify: no file at `server/src/main/java/com/gumtree/csagent/service/runtime/SkillGuardrailDispatcher.java` post-Sprint-38; the 4 Sprint 38 Skill YAMLs all `guardrails: []` so no dispatcher is needed yet.
  - #6 No `SkillStateBus.java` / NEW `prior_use_case_carry` projection slot / `ContextProjectionBuilder.java` edit — verify: no file at `server/src/main/java/com/gumtree/csagent/service/runtime/SkillStateBus.java`; `ContextProjectionBuilder.java` unchanged.
  - #7 No touch to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` — verify via `git diff 51c327c..bd9d3f5 -- server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java server/src/main/java/com/gumtree/csagent/service/runtime/IntentClassification.java [...]` (all return empty).
  - #8 No edit to `INTAKE_UCS` set at legacy `PhaseEvaluator.java:30` — verify by reading the post-Sprint-38 file; the set is unchanged.
  - #9 No `escalation_reason` enum widening at legacy `PhaseEvaluator.java:33-63` — verify by reading the post-Sprint-38 file; the enum is unchanged (23 values).
  - #10 No Tier-0 invariant added to `docs/runtime_freeze_and_risk_policy.md` — verify via `git diff 51c327c..bd9d3f5 -- docs/runtime_freeze_and_risk_policy.md` (returns empty).
  - #11 No edit to `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` — verify via `git diff` on each path (all return empty).
  - #12 No edit to `docs/proposals/skill_registry_design.md` — verify (Sprint 37 freeze IMMUTABLE).
  - #13 No edit to other docs under `docs/foundational/` or `docs/current/` — verify.
  - #14 No edit to sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-037-*` — verify (the Sprint 37 archive files at `docs/sprints/sprint-037-*.md` are present at HEAD per deliver-agent's Sprint 37 close commit `649c6c7`, NOT modified at `bd9d3f5`; verify via `git log --oneline -- docs/sprints/sprint-037-handoff.md` — last modification SHA should be `649c6c7` or earlier, NOT `bd9d3f5`).
  - #15 No edit to milestone archives under `docs/milestones/` — verify.
  - #16-#18 No edit to deliver-agent-owned files (`docs/milestone_objective.md` / `docs/sprint_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-038-*.md`) — verify via `git diff 51c327c..bd9d3f5 --stat | grep -E "(milestone_objective|sprint_objective|10-handoff|action_bank|codex-findings|sprint-038-dev-prompt|sprint-038-review-prompt)"` (returns empty — the deliver-agent's housekeeping bundle is at a DIFFERENT commit than the dev commit `bd9d3f5`; the Sprint 37 close commit `649c6c7` carries the prior housekeeping).
  - #19-#23 No edit to eval surfaces (existing case families / shadow CaseSpecs / harness / Alice bad case / overrides) — verify via `git diff 51c327c..bd9d3f5 -- eval_interactive/` (returns empty).
  - #24 No mocked-LLM as primary evidence — Sprint 38 ships Java behavioural-equivalence + Java baseline as primary evidence; no real-LLM eval rerun; Java composition logic tests mock LLM but those are deterministic Java logic tests, not LLM behaviour claims. Sprint 38 §11 acceptance bars row "Wrong-containment rate unchanged or down" and "Over-escalation rate unchanged or down" mark N/A per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`. Verify.
  - #25 No per-UC-branch if-else in any Skill YAML body — verified by §3 Q1 + §4 above; load-bearing per M2 §6 #1.
  - #26 No Skill that prescribes LLM customer-facing language — verified by §3 Q5 + §4.
  - #27 No Skill that hard-encodes per-step argument values — verified by §3 Q5 + §4.
  - #28 No regression on `Sprint71PartialIntakePersistenceTest` 14/14 — verified by §7 Java baseline re-run below.
  - #29 No regression on `IntakeFieldExtractor` tests + Sprint 32 alternate_candidate_use_cases projection slot tests + Sprint 33 discover_disambiguation_signals projection slot tests — verified by §7 Java baseline re-run.
  - #30 No CLOSE → TERMINAL phase enum rename — verify by reading the post-Sprint-38 file at `PhaseEvaluator.java`; the `Phase` enum is unchanged (still has `CLOSE`, not `TERMINAL`); `terminal.yaml` declares `applicable_phases: [CLOSE]` per OQ-7.1 default.
  - #31 No pre-decision of Sprint 39 / 40 / 41 implementation specifics beyond freeze — verify: no Sprint 38 dev commit edit anticipates Sprint 39 RESOLVE migration semantics, Sprint 40 system_prompt.txt extraction semantics, or Sprint 41 SkillStateBus semantics.

- **M2 milestone-level hard fences (22 items per `docs/milestone_objective.md` §6):** verify each is honored at HEAD `bd9d3f5`. Especially:
  - #1 No per-UC-branch if-else in any Skill body (YAML or Java or prompt) — verified by §3 + §4.
  - #2 Skill terminal predicate is Java guard ONLY for Runtime-owned floor per §1.4 — N/A for Sprint 38 (no predicate migration; Sprint 39 scope).
  - #3 Skill recommended order is soft prompt guidance, NOT hard enforcement — verify the 4 Sprint 38 Skill `procedure` texts are LLM-soft (CONFIRM = interpret; ESCALATE = finalize; CLOSE = close politely; DISCOVER = triage). No hard enforcement of step ordering.
  - #4 HARD FENCE INVERSION on `D-hard-citation-gate` (verbatim S1 authorization) — N/A for Sprint 38 (S1 predicate is Sprint 39 scope); the authorization is preserved verbatim in `docs/milestone_objective.md` §6 #4 and in `docs/proposals/skill_registry_design.md` §8.2.4 ready for Sprint 39 implementation.
  - #5 No `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` touch — verified by Sprint 38 §6 #7.
  - #6 No edit to `INTAKE_UCS` set — verified by Sprint 38 §6 #8.
  - #7 No `escalation_reason` enum widening — verified by Sprint 38 §6 #9.
  - #8 No Tier-0 invariant added without Sprint 37 freeze pre-authorization + explicit human-review escalation — verified by §8 below (C2 + C3 R-items continuation; no new candidate).
  - #9-#11 No edits to existing case families / shadow case families / harness — verified by Sprint 38 §6 #19-#21.
  - #12 No `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` edit during M2 — verified by Sprint 38 §6 #11.
  - #20 No deletion of OLD `docs/proposals/skill_foundation_design.md` — verify the OLD design doc still exists at HEAD `bd9d3f5` with `status: superseded` frontmatter preserved (the supersession was applied by deliver-agent at commit `fd7396a`; verify the frontmatter persists through Sprint 38 dev commit `bd9d3f5`).
  - #21 No reset / migration of M1-shipped FUNCTIONAL surfaces (`IntakeFieldExtractor`, `Sprint71PartialIntakePersistenceTest`, Sprint 32/33 projections in `ContextProjectionBuilder.java`) — verified by Sprint 38 §6 #28 + #29 + the unchanged `ContextProjectionBuilder.java`.
  - #22 No deletion or relocation of `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — Sprint 38 ships zero eval surface change so by construction PASS.

- **Behavioural equivalence verification (load-bearing per Sprint 37 freeze §6.4):** the §6.4 hard gate is "Skill-composed PhasePlan field-by-field equal to pre-migration golden". Codex independently verifies:
  1. The `PhaseEvaluatorSkillIntegrationTest.java` test file contains 12 behavioural-equivalence cases (3 representative UCs × 4 migrated phases) + 1 legacy-RESOLVE-INTAKE regression check = 13 tests total per handoff §1.6.
  2. Each test pins the Skill-composed PhasePlan to GOLDEN pre-migration legacy string captured inline from `PhaseEvaluator.java:397-542` at parent commit `51c327c`. Codex spot-checks at least one phase's golden assertion by:
     - Running `git show 51c327c:server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java | sed -n '397,457p'` (DISCOVER legacy branch).
     - Reading the corresponding test assertion in `PhaseEvaluatorSkillIntegrationTest.java`.
     - Confirming the test asserts character-by-character equivalence (modulo whitespace normalization Java string concatenation performed).
  3. The test passes via `mvn test -Dtest=PhaseEvaluatorSkillIntegrationTest -q` from a clean checkout of `bd9d3f5`.

- **Sprint 37 freeze fidelity verification (per Sprint 38 handoff §5 table):**
  - Decision (a) §2.1 12-field Skill data model → `Skill.java` 15-field record (the dev added 3 supporting types — Guardrail, StateInheritance, plus the 12 named fields + 3 sub-objects — verify field count + JsonCreator snake_case mapping).
  - Decision (b) §3.1 SkillRegistry shape → `SkillRegistry.java` `select(phase, useCase) → Optional<Skill>` + `allSkills()` + `skillsForPhase()`. Verify the selection logic implements §3.2 exact-match-then-wildcard-then-Optional.empty() semantics.
  - Decision (c) §4.1 PhaseEvaluator integration → `composeSkillPhasePlan(...)` helper + `plan(...)` dispatch updated. Verify the helper exists at the dev-handoff-cited location (handoff §4.3 says "around line 533, immediately before `interpretRunResult`"); verify the dispatch ordering (SkillRegistry first; legacy fall-through).
  - Decision (e §6.2.1) DISCOVER → `discover_triage.yaml`. Verify legacy-verbatim content (OQ-S38.1 default).
  - Decision (e §6.2.2) CONFIRM → `confirm.yaml`. Verify legacy-verbatim.
  - Decision (e §6.2.3) ESCALATE → `escalate.yaml`. Verify `create_case_controlled` INTENTIONALLY excluded per design doc §6.2.3 note + Codex 1.8 rule (the legacy `PhaseEvaluator.java` inline comment at 514-521 carries the rationale).
  - Decision (e §6.2.4) CLOSE → `terminal.yaml`. Verify `applicable_phases: [CLOSE]` per OQ-7.1 default; file name `terminal.yaml` carries M3+ rename intent.

- **MATERIAL FINDING preservation through Sprint 38** (cross-reference Sprint 37 close MATERIAL FINDING + Sprint 38 §6 #2):
  - Sprint 6 §G2 `shouldRejectFaqMissHandover` at legacy `AgentRunLoopImpl.java:690-734` — verify UNCHANGED at HEAD `bd9d3f5` via `git diff 51c327c..bd9d3f5 -- server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (returns empty).
  - Sprint 7 §I2 `shouldRejectIncompleteIntakeHandover` at legacy `:628-651` — UNCHANGED.
  - Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` at legacy `:745-759` — UNCHANGED.
  - Dispatch sites at lines 317 / 356 / 413 — UNCHANGED.
  
  Sprint 39 migrates these per design doc §8.2.1-§8.2.3; Sprint 38 leaves them intact. Verify by reading the post-Sprint-38 file content matches parent `51c327c` byte-for-byte.

- **Sprint 33 cue/slot split preservation through Sprint 38:**
  - Cue at legacy `PhaseEvaluator.java:432-448` (the DISCOVER `systemInstruction` Sprint 33 ad-status disambiguation cue text) → migrated INTO `discover_triage.yaml` `procedure` per Sprint 38 decision (e §6.2.1). Verify the migrated text in `discover_triage.yaml` matches legacy 432-448 byte-for-byte modulo whitespace.
  - Slot at `system_prompt.txt:36-43` (the Sprint 33 slot description teaching block in the system prompt) → UNCHANGED in Sprint 38 (Sprint 40 migrates). Verify via `git diff 51c327c..bd9d3f5 -- server/src/main/resources/prompts/system_prompt.txt` (returns empty).

- **Premise re-verification (Sprint 37 close + Sprint 38 dev) preservation:**
  - `system_prompt.txt` is **101 lines** (Sprint 37 premise #7 refinement) — verify at HEAD `bd9d3f5` via `wc -l server/src/main/resources/prompts/system_prompt.txt` (returns 101).
  - `UseCaseDefinition` record at `UseCaseRegistryService.java:166-173` has **6 fields** (Sprint 37 premise #8 refinement; NO `requiredIntakeFields`) — verify by reading lines 166-173 of `UseCaseRegistryService.java`.
  - `IntakeFieldsRegistry.java` is **222 lines** with `REQUIRED_FIELDS_BY_UC` at 53+, `canonicalFieldName` at 127-131, `requiredFieldsFor` at 118-121, `intakeComplete` at 184-193 — verify (Sprint 7 §I2 source of truth for per-UC intake fields).

## 6. Schema and reproducibility checks

- **Skill data model schema (design doc §2.1 / §2.2):**
  - 12 named fields + supporting types (Guardrail + StateInheritance + the 3 sub-objects).
  - JSON Schema validation contract per design doc §2.2 → Sprint 38 dev chose **manual Java validation in `SkillLoader.validate(...)`** (per handoff §5 / OQ note + design doc §2.2 "exact JSON Schema is Sprint 38 dev"). Verify: `SkillLoader.validate(skill, filename)` covers required field presence + enum-subset checks + wildcard-mixed-with-explicit-UC rejection + state-key whitelist + projection-slot whitelist + `guardrail.on_fail` whitelist (`reject_with_hint` / `downgrade_reason`; `observe_only` excluded per OLD Sprint 36 D2 §3.2 verbatim).
  - The validation surface is the same as design doc §2.2; the editorial path (manual Java vs JSON Schema file) is a dev-judgement permitted by the freeze.
- **SkillRegistry shape (design doc §3.1 / §3.2 / §3.3):**
  - `select(phase, useCase) → Optional<Skill>` (the dev's Optional return type matches §3.2 "null fallback returns Optional.empty() semantically").
  - `allSkills()` + `skillsForPhase()` per design doc §3.1.
  - SkillLoader fail-fast at boot via Spring `@Component` constructor + `loadAll()` per §3.3.
  - Eager indexing into `wildcardByPhase` + `exactByPhaseUc` per §3.1.
  - Collision detection on intra-phase (phase, useCase) duplicates per §3.1.
- **PhaseEvaluator integration (design doc §4.1 / §4.2 / §4.3):**
  - Constructor extended 10 → 11 args (new `SkillRegistry skillRegistry` parameter).
  - `composeSkillPhasePlan(Skill, String phase, String activeUc) → PhasePlan` private helper.
  - `plan(...)` dispatch: SkillRegistry first; legacy fall-through for unmigrated phases.
  - 4 legacy phase branches DELETED (DISCOVER 397-457 + CONFIRM 462-487 + CLOSE 492-509 + ESCALATE 513-542 at parent `51c327c`).
  - 2 legacy RESOLVE branches PRESERVED verbatim (RESOLVE-INTAKE legacy 552-596 + RESOLVE-FAQ legacy 598-665).
  - Backward-compatible PhasePlan data shape per §4.2 (no PhasePlan record edit; `wc -l server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` returns 146).
- **4 Skill YAML schemas:** read each YAML; verify all required fields present (`name`, `description`, `applicable_phases`, `applicable_use_cases`, `tools_required`, `procedure`); verify `applicable_phases` values in {`DISCOVER`, `CONFIRM`, `RESOLVE`, `ESCALATE`, `CLOSE`}; verify `applicable_use_cases` is `["*"]` for all 4; verify `valid_terminal_outcomes` values in `TerminalOutcome` enum (`FINAL_ANSWER`, `CLARIFICATION_NEEDED`, `ESCALATE`); verify `state_inheritance` blocks populate per design doc §6.2.1-§6.2.4 templates; verify `guardrails: []` for all 4; verify `tools_required` lists name only existing tools (cross-reference `customer_service_tool_spec_v0_2.yaml`).
- **Test file structure:**
  - `SkillTest.java` (~141 lines; 8 unit tests per handoff §4.5 — verify count).
  - `SkillRegistryTest.java` (~146 lines; 11 unit tests).
  - `SkillLoaderTest.java` (~245 lines; 13 unit tests = 1 happy + 12 negative).
  - `SkillTestFixtures.java` (~21 lines; `productionRegistry()` helper).
  - `PhaseEvaluatorSkillIntegrationTest.java` (~341 lines; 13 behavioural-equivalence tests + 1 legacy-RESOLVE preservation check).
- **PhaseEvaluator post-Sprint-38 line count:** dev handoff §1.6 + §4.3 + §11 claims **1500 lines** (1628 → 1500 = −128). Deliver-agent post-commit spot-measure was **1529 lines** (−99). Codex independently verifies via `wc -l server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` at HEAD `bd9d3f5`. If the actual line count is materially different from the handoff claim, surface as a minor REPRODUCIBILITY note (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`) — NOT a BLOCKING finding. The substantive deletion (4 legacy phase branches removed; helper + dispatch added; constructor extended) is the behaviour-bearing change; the exact line count is downstream of formatting / blank-line conventions.
- **Reproducibility (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`):** every claim in the dev handoff about current code shape cites file path + line range. Codex spot-checks 5-6 cited ranges; confirms they exist and match the handoff claim:
  - `PhaseEvaluator.java:30` INTAKE_UCS — verify by `git show bd9d3f5:server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java | sed -n '28,35p'`.
  - `PhaseEvaluator.java` post-Sprint-38 line count — verify via `wc -l`.
  - `AgentRunLoopImpl.java:628-651` Sprint 7 `shouldRejectIncompleteIntakeHandover` — verify UNCHANGED.
  - `AgentRunLoopImpl.java:690-734` Sprint 6 `shouldRejectFaqMissHandover` — verify UNCHANGED.
  - `AgentRunLoopImpl.java:745-759` Sprint 11 `shouldRejectPrematureResolveOutcome` — verify UNCHANGED.
  - `system_prompt.txt:54-101` `request_handover` decision tree — verify UNCHANGED (preserved from Sprint 37 OQ-7.11 verdict).
- **Handoff structure:** verify 12-section shape per Sprint 31-37 + Sprint 38 §11 contract. §12 closure verdict placeholder per `feedback_handoff_verdict_section_delegation.md` (NOT filled by dev).

## 7. Validation runs (you re-execute)

From a clean checkout of the dev commit `bd9d3f5`:

- **Java test baseline preservation:**

  ```bash
  cd server && mvn test -q
  ```

  Expected: `Tests run: 1028, Failures: 1, Errors: 0, Skipped: 2` (post-Sprint-37-close baseline 983 + 45 new Sprint 38 tests = 1028; the 1 inherited failure is `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` attributed to the dirty working-tree `system_prompt.txt` baseline carrying since Sprint 24-era per Sprint 36 / 37 close notes; persists UNCHANGED through Sprint 38 since `system_prompt.txt` is UNTOUCHED). Any new failure delta is a **BLOCKING finding** (Sprint 38's architectural refactoring must preserve behavioural equivalence).

  Spot-check the 45 new Sprint 38 tests pass:
  - `SkillTest` — 8 tests pass.
  - `SkillRegistryTest` — 11 tests pass.
  - `SkillLoaderTest` — 13 tests pass (1 happy + 12 negative).
  - `PhaseEvaluatorSkillIntegrationTest` — 13 tests pass.
  
  Verify via `mvn test -Dtest=SkillTest -q` + `mvn test -Dtest=SkillRegistryTest -q` + `mvn test -Dtest=SkillLoaderTest -q` + `mvn test -Dtest=PhaseEvaluatorSkillIntegrationTest -q`.

- **Targeted regression spot-checks** (per Sprint 38 §6 #28 + #29 hard fences):
  - `Sprint71PartialIntakePersistenceTest` 14/14 — verify via `mvn test -Dtest=Sprint71PartialIntakePersistenceTest -q`.
  - `Sprint11ProgressiveResolveTest` — verify (Sprint 11 §M1 dispatch site UNCHANGED).
  - `Sprint7IntakeStateTest` + `Sprint7CandidateUseCasesProjectionTest` — verify (Sprint 7 §I2 + Sprint 32 alternate_candidate_use_cases projection UNCHANGED).
  - `PhaseEvaluatorPlanTest` + `PhaseEvaluatorFaqMissFallbackTest` + `PhaseEvaluatorMaxStepsResolverTest` + `PhaseEvaluatorQueryEnrichmentTest` — verify (legacy RESOLVE branches + max-tool-steps semantics UNCHANGED).
  - `Sprint9TerminalToolHonestyTest` + `Sprint9TraceObservabilityFidelityIntegrationTest` — verify (Sprint 9 terminal + trace observability UNCHANGED).
  - `Sprint24DeadlinePlaceholderCoalesceTest` — verify (Sprint 24 deadline-placeholder UNCHANGED).
  - `Sprint81PhaseEvaluatorUseCaseIdentifiedTest` — verify (Sprint 81 UC-identified semantics UNCHANGED).

- **No eval run required** at Sprint 38 close. Per M2 §5 acceptance recalibration, interactive eval + bad-case suite are OBSERVATION ONLY for THIS milestone; not a hard gate. If Codex chooses to spot-check OPTIONALLY, results are informational and MAY regress; do NOT block on regression.

- **No real-LLM behavioural rerun required** at Sprint 38 close. Sprint 38 is architectural refactoring; behavioural equivalence is testable in Java (the `PhaseEvaluatorSkillIntegrationTest` is the gate). The §6.4 contract is "Skill-composed PhasePlan field-by-field equal to pre-migration golden" — that's Java-deterministic, not LLM-stochastic.

## 8. Tier-0 candidate continuation (Codex-substantive verification)

Sprint 37 close pre-decided C2 + C3 QUALIFIED-DEFER and opened R-items in `docs/action_bank.md` §5.2 (`R-skill-guardrail-non-overridability-tier-0` + `R-skill-state-bus-boundary-enforcement-tier-0`) for M3+ revisit. Sprint 38 honors the DEFER pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md`:

| Candidate | Statement | Sprint 38 implementation evidence | Recommendation |
|---|---|---|---|
| **C1** | Skill tool-whitelist enforcement unconditional | Sprint 38 `composeSkillPhasePlan(...)` composes `skill.toolsRequired` into `PhasePlan.allowedTools`; existing `ToolDispatcher.validateAgainstPlan` at `ToolDispatcher.java:183-195` continues to enforce the whitelist (read post-Sprint-38 to confirm). C1 REJECTED preserved by construction. | Codex confirms — no action |
| **C2** | Skill terminal predicate refusal non-overridable by LLM | Sprint 38 ships `guardrails: []` for all 4 Skills (no terminal predicate enforcement yet); the unified Skill terminal-predicate dispatcher is Sprint 39 scope. No observed Sprint 38 runtime evidence on dispatcher refusal semantics. DEFER preserved; R-item `R-skill-guardrail-non-overridability-tier-0` in `action_bank.md` §5.2 ready for M3+ revisit after Sprint 39 evidence. | Codex independently re-evaluates: DEFER is sound (Sprint 39 hasn't shipped). If Codex DISAGREES (e.g., `composeSkillPhasePlan` + `Skill.toolsRequired` already structurally Tier-0), route as `out_of_scope_review` per Sprint 37 §8 precedent |
| **C3** | Skill `state_inheritance` enforced at session-state-bus boundary | Sprint 38 ships `state_inheritance` populated per design doc §6.2.1-§6.2.4 templates but NOT enforced (no `SkillStateBus.java`; `ContextProjectionBuilder.java` UNCHANGED). Sprint 41 scope. DEFER preserved; R-item `R-skill-state-bus-boundary-enforcement-tier-0` ready for M3+ revisit after Sprint 41 evidence. | Codex independently re-evaluates: DEFER is sound. If Codex DISAGREES, route as `out_of_scope_review` per Sprint 37 §8 precedent |
| **C4** | S1 `must_cite_source` predicate semantics | N/A for Sprint 38 (Sprint 39 scope per M2 §6 #4 verbatim authorization); the authorization is preserved verbatim in `docs/milestone_objective.md` §6 #4 and `docs/proposals/skill_registry_design.md` §8.2.4. NOT A CANDIDATE per Sprint 37 close. | Codex confirms — no action |
| **C5** | S2 `intake_complete_required` predicate semantics | N/A for Sprint 38 (Sprint 39 scope per Sprint 7 §I2 precedent). NOT A CANDIDATE per Sprint 37 close. | Codex confirms — no action |

**Critical: Sprint 38 dev surfaced NO new Tier-0 candidate from implementation.** OQ-S38.5 explicitly confirms — both C2 + C3 R-items carry forward unchanged. If Codex SURFACES a new Tier-0 candidate from the Sprint 38 implementation diff (e.g., `SkillRegistry.select` should be Tier-0 territory; `SkillLoader.validate` schema enforcement should be Tier-0 territory; the post-Sprint-38 `plan(...)` dispatch order should be Tier-0 territory), raise as a NEW Finding for deliver-agent + human Tier-0 evaluation at Sprint 38 close. Do NOT block Sprint 38 close on a NEW Tier-0 candidate elevation — per `feedback_constitution_discipline_vs_planning_anticipation.md` + M2 §10 stop condition #1, Tier-0 elevation requires explicit human-review escalation; Codex's finding is informational input to the human's deliberation, not a Sprint 38 dev blocker.

If Codex DISAGREES with the C2 + C3 continuation (e.g., Sprint 38's `composeSkillPhasePlan` + `Skill.toolsRequired` already constitute structural Tier-0 territory that should be elevated NOW): route as `out_of_scope_review` per Sprint 37 §8 precedent (Tier-0 elevation / deferral is deliver-agent + human authority).

## 9. Open question independent verification

Sprint 38 dev surfaced 5 OQs in handoff §7:

| OQ | Subject | Deliver-agent + human pre-decision | Codex action |
|---|---|---|---|
| **OQ-S38.1** | Sprint 37 design freeze §6.2.1 DISCOVER YAML template re-phrases legacy `PhaseEvaluator.java:411-448` systemInstruction in places (paragraph headings normalized; objective trailing period added); Sprint 38 chose LEGACY-verbatim to satisfy §6.4 behavioural-equivalence hard gate. | **AGREE WITH DEV** (legacy-verbatim) per `doc_governance.md` "code ahead of docs" rule. See header. | **Codex INDEPENDENT verdict REQUIRED**. Read `docs/proposals/skill_registry_design.md` §6.2.1 DISCOVER template + read `server/src/main/resources/skills/discover_triage.yaml` `procedure` text + read legacy `PhaseEvaluator.java:411-448` at parent commit `51c327c`. Judge: (a) is the divergence editorial paraphrasing in the freeze template the dev correctly resolved by honoring code-as-source-of-truth per `doc_governance.md`, OR (b) is it a freeze template intent change the implementation should adopt (which would require updating the 13 golden-string assertions in `PhaseEvaluatorSkillIntegrationTest.java` to match the template wording)? Expected verdict: (a) — honor legacy verbatim; behavioural equivalence is the §6.4 contract; freeze template's intent is honored at field-shape level; template wording is descriptive paraphrasing, not a behaviour-change decision the freeze locked. **If Codex DISAGREES** (verdict (b)): route as `out_of_scope_review` per the header — fix would require editing the immutable Sprint 37 freeze design doc OR re-authoring 4 Skill YAMLs + 13 golden-string assertions; both are scope expansion beyond Sprint 38 contract. Open R-item like `R-skill-design-doc-template-fold-back` for M2 close fold-back per `doc_governance.md` fold-back cadence. NOT `fix_required` |
| **OQ-S38.2** | `composeSkillPhasePlan(...)` placement — private helper inside `PhaseEvaluator.java` vs separate utility class `SkillPhasePlanComposer` in the `skill` package | **AGREE WITH DEV** (keep inside PhaseEvaluator per design doc §4.1 illustrative snippet; revisit if Sprint 39 grows the helper materially) | Codex may note any disagreement as informational; not blocking; revisit at Sprint 39 planning round |
| **OQ-S38.3** | `maxToolSteps` literal-2 fallback in `composeSkillPhasePlan(...)` when Skill field is null; Sprint 38's 4 Skills all set `max_tool_steps: 2` explicitly so the fallback never fires; Sprint 39 RESOLVE Skills (4 / 3) will set explicit values | **DEFER to Sprint 39 planning** (the literal-2 default is dev-judgement, not a freeze decision; Sprint 39 Skills will set explicit values) | Codex may note any disagreement as informational; not blocking; revisit at Sprint 39 planning round |
| **OQ-S38.4** | `SkillTestFixtures.productionRegistry()` used by 19 existing PhaseEvaluator constructor call sites; couples tests to production Skill YAMLs (vs Mockito stub + `@MockBean` pattern) | **AGREE WITH DEV** — coupling to production YAMLs is the same shape as existing tests' coupling to `UseCaseRegistryService.getUseCase(...)` registry data | Codex may note any disagreement as informational; not blocking |
| **OQ-S38.5** | C2 + C3 R-items (`R-skill-guardrail-non-overridability-tier-0` + `R-skill-state-bus-boundary-enforcement-tier-0`) carry forward unchanged; Sprint 38 dev surfaced NO new evidence to re-evaluate | **CONFIRM** (continuation per Sprint 37 close pre-decision) | Codex confirms per §8 above; if Codex DISAGREES, route per §8 disagreement-routing (`out_of_scope_review`) |

OQ-S38.1 is the **load-bearing** OQ for Sprint 38 Codex review — Codex's verdict adjudicates the template-vs-legacy editorial divergence and routes the disagreement appropriately. The other 4 OQs are informational.

## 10. Deferred / non-blocking observations

- **PhaseEvaluator post-Sprint-38 line count discrepancy** — handoff §1.6 + §4.3 + §11 cite **1500 lines** (1628 → 1500 = −128); deliver-agent's post-commit spot-measure was **1529 lines** (−99). Codex re-runs `wc -l server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` at HEAD `bd9d3f5`. If actual differs from handoff claim, note as a REPRODUCIBILITY observation (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`) — informational; not blocking. The substantive deletion (4 legacy phase branches removed; helper added; constructor extended; dispatch updated) is the behaviour-bearing change and is unambiguously present.
- **Skill record 15 vs 12 fields** — dev handoff §4.1 describes Skill.java as 15 fields (12 named + 3 sub-objects: `guardrails`, `stateInheritance`, plus `objective` / `maxToolSteps` / `allowInterimMessage` / `validTerminalOutcomes` / `requiredContextKeys` from the optional set in design doc §2.1). The design doc §2.1 names 12 PRIMARY fields + 3 supporting types. The dev's framing as "15 fields" is a presentation choice that captures the full record signature. Codex notes the framing as informational; not a substantive freeze deviation.
- **Behavioural-equivalence test scope** — 13 tests pinned to GOLDEN pre-migration legacy strings cover 4 migrated phases × 3 representative UCs + 1 legacy-RESOLVE-INTAKE regression check. Adequate coverage per design doc §6.4 contract. If Codex SURFACES a gap (e.g., a representative UC for a migrated phase that should have been pinned but wasn't), note as informational + open R-item for Sprint 39 / M2 close revisit; not blocking for Sprint 38 (the §6.4 gate is "field-by-field equal for representative UCs"; 3 UCs per phase is the dev-judgement minimum that the freeze permits).
- **C2 + C3 R-items in `action_bank.md` §5.2** stay open through Sprint 38 close; revisit at M3+ after Sprint 39 (C2 dispatcher evidence) + Sprint 41 (C3 state-bus evidence). Codex may note any disagreement with the deferral framing as informational input to the human's M3+ planning round; not blocking for Sprint 38.
- **Sprint 38 contract §11 row "Wrong-containment rate unchanged or down" + "Over-escalation rate unchanged or down"** marked N/A per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`. Sprint 38 is architectural; behavioural equivalence on Java composition is the gate; LLM behaviour observation OBSERVATION ONLY per M2 §5 recalibration. Codex confirms N/A is correct.
- **Sprint 38 contract §11 row "Architecture-health metrics not regressed"** YES per `new_semantic_hardcode_count` = 0; `soft_signal_conversion_count` = 4 (4 phase Skills moved from hardcoded Java strings to externalized Skill YAML); `planner_ownership_ratio` unchanged; `shadow_disagreement_rate` not measured (Sprint 38 architectural). Codex confirms; the §6 governance metrics remain `not_started` on collection per `iteration_governance.md` §6 — Sprint 38 reports counts for tracking but no programmatic gate.

## 11. Output format (write to `docs/codex-findings.md`)

Replace the file content with the standard §4.2 sprint-close header followed by the structured sections below. Per `feedback_packaging_codex_findings_supersession.md`: delete-and-add the prior content (if any) to ensure no editorial drift across sprints. Per the deliver-agent close convention: the deliver-agent will archive this file to `docs/sprints/sprint-038-codex-review.md` at Sprint 38 close.

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph>

## Review Evidence
<bullet list — review scope (Sprint 38 commit bd9d3f5 against pre-dev HEAD 51c327c; ~3535 insertions / 398 deletions across 43 files; 5 new Skill Java classes + 4 Skill YAMLs + PhaseEvaluator integration + 4 new test files + SkillTestFixtures + 19 existing PhaseEvaluator test constructor edits + dev handoff); what you re-ran (Java baseline `mvn test -q`; targeted regression spot-checks per §7; cited file:line ranges spot-checked); what passed; what was independently verified (Sprint 37 freeze fidelity per §5 + §6; behavioural-equivalence golden-string preservation per §5; Sprint 6/7/11 predicate UNCHANGED per §5; Sprint 33 cue/slot split preservation per §5; premise re-verification per §5)>

## Blocking Findings (if any)
<numbered list; each entry quotes the diff snippet OR Java file:line / Skill YAML body + cited reference; references back to §2 scope-discipline / §4 §1.7 boundary / §5 hard-fence / §8 Tier-0 / §9 OQ-S38.1>

## Anti-Hardcode Kernel (§3)
<nine-question walk against the Sprint 38 IMPLEMENTATION DIFF; each Q with one-line verdict; cross-reference dev handoff §6 self-walk; Codex agreement or dissent per Q with diff snippet quoted where dissent>

## §1.7 Boundary Check (§4)
<the §4 implementation boundary checks; pass/fail per surface: 4 Skill YAML `procedure` bodies legacy-verbatim + no eval-phrase encoding; 4 Skill YAML `applicable_use_cases` all `["*"]`; 4 Skill YAML `state_inheritance` state-dimension-keyed; 4 Skill YAML `guardrails: []`; `composeSkillPhasePlan` no per-UC branch; `SkillRegistry.select` lookup-not-branch-table; `Skill.appliesTo` no per-UC enum; `SkillLoader.validate` schema-enforcement at load not runtime; 4 deleted legacy phase branches relocated-not-redesigned; 2 preserved legacy RESOLVE branches byte-for-byte equivalent>

## Hard-Fence Verification (§5)
<Sprint 38 contract §6 31 fences + M2 §6 22 fences + behavioural equivalence + Sprint 37 freeze fidelity table + MATERIAL FINDING preservation + Sprint 33 cue/slot split preservation + premise re-verification; pass/fail per fence>

## Schema And Reproducibility Checks (§6)
<Skill / Guardrail / StateInheritance data models per design doc §2 + §5 + §10; SkillRegistry + SkillLoader shape per design doc §3; PhaseEvaluator integration per design doc §4; 4 Skill YAML schemas per design doc §6.2.1-§6.2.4; test file structure (8 + 11 + 13 + 13 = 45 new tests); PhaseEvaluator line-count verification (handoff claim 1500 vs actual via `wc -l` — note discrepancy if material); cited file:line spot-checks (5-6 ranges) reproduced>

## Validation Runs (§7)
<the §7 results; `mvn test -q` output; baseline preservation 1028/1-inherited/0/2; 45 new Sprint 38 tests pass; targeted regression spot-checks Sprint71PartialIntakePersistenceTest 14/14 + Sprint11 + Sprint7 + PhaseEvaluatorPlanTest + PhaseEvaluatorFaqMissFallbackTest + Sprint9 + Sprint24 + Sprint81>

## Tier-0 Candidate Continuation (§8)
<the §8 walk; CONFIRM or DISAGREE per candidate (C1 REJECTED preserved by `composeSkillPhasePlan` + `ToolDispatcher.validateAgainstPlan` enforcement; C2 + C3 QUALIFIED-DEFER continuation per Sprint 37 close pre-decision; C4 + C5 NOT A CANDIDATE N/A for Sprint 38); flag any NEW Tier-0 candidate Codex surfaces from Sprint 38 implementation diff as informational input to deliver-agent + human for M3+ revisit>

## OQ Independent Verification (§9)
<the §9 walk; OQ-S38.1 INDEPENDENT verdict (agree with dev legacy-verbatim OR disagree → route to `out_of_scope_review` per header); OQ-S38.2-S38.5 informational notes if any>

## Deferred / Non-Blocking Notes (§10)
<the §10 items; line-count discrepancy + 15-vs-12 Skill field framing + behavioural-equivalence test scope + C2/C3 R-items + N/A acceptance bar rows + architecture-health metric counts>
```

## 12. Expected verdict shape

If all gates pass + Sprint 38 honors Sprint 37 freeze decisions (a)/(b)/(c)/(e §6.2.1-§6.2.4) verbatim + behavioural equivalence preserved per §6.4 + no scope creep + Java baseline 1028/1-inherited/0/2 holds + Codex's OQ-S38.1 verdict aligns with dev (legacy-verbatim): **`decision: pass / blocking_count: 0`**. The cleanest outcome for an architectural-refactoring sub-sprint where the dev shipped per spec + behavioural equivalence verified.

If §2 scope-discipline fails (e.g., dev commit touched `AgentRunLoopImpl.java`, `system_prompt.txt`, or a deliver-agent-owned doc): **`decision: fix_required`** with the violating file path + line range quoted as Finding #1.

If §4 §1.7 boundary check fails (e.g., a per-UC-branch if-else in a Skill YAML body OR `composeSkillPhasePlan` introduces per-UC logic OR a Skill prescribes customer-facing language): **`decision: fix_required`** with the violating diff snippet / YAML body quoted; cite the relevant §1.7 forbidden-list line + M2 §6 #1 / #25 / #26 / #27 fences.

If §5 behavioural-equivalence verification fails (e.g., `PhaseEvaluatorSkillIntegrationTest` golden-string assertion fails on any of 12 cases OR the legacy RESOLVE branches show edits inconsistent with line-renumbering): **`decision: fix_required`** with the failing test name + diff snippet quoted.

If §5 hard-fence verification fails (e.g., Sprint 6/7/11 predicates modified at `AgentRunLoopImpl.java`; `system_prompt.txt` modified; Sprint 33 cue migration drops content): **`decision: fix_required`** with the violating fence + file path quoted.

If §7 Java baseline regression (e.g., `mvn test -q` returns `Tests run: ≠1028 OR Failures: >1` OR a new failure delta beyond the 1 inherited): **`decision: fix_required`** with the failing test names quoted.

If §6 schema check fails (e.g., a Skill YAML lacks a required field; a state-inheritance block names an unknown state slot; a `tools_required` list names a non-existent tool): **`decision: fix_required`** with the violating YAML body quoted.

If Codex's OQ-S38.1 INDEPENDENT verdict comes back DISAGREEING with the dev (template-vs-legacy editorial divergence; Codex believes the implementation should adopt the template wording): **`decision: out_of_scope_review`** with the template paragraph + legacy paragraph + Skill YAML body quoted; recommend a future deliver-agent housekeeping commit updating the design doc §6.2.1 template wording to match legacy text (preferred per `doc_governance.md` "code ahead of docs" rule) OR open R-item `R-skill-design-doc-template-fold-back` for M2 close fold-back. **Do NOT classify as `fix_required`** per the header pre-decision: fix would require editing the immutable Sprint 37 freeze design doc OR re-authoring 4 Skill YAMLs + 13 golden-string assertions; both are scope expansion beyond Sprint 38 contract.

If Codex DISAGREES with the Tier-0 candidate continuation per §8 (e.g., believes Sprint 38's `composeSkillPhasePlan` + `Skill.toolsRequired` already constitute structural Tier-0 territory that should be elevated NOW): **`decision: out_of_scope_review`** — the Tier-0 elevation / deferral is deliver-agent + human authority per M2 §10 stop condition #1 + Sprint 37 §8 precedent; Codex's disagreement is informational input to the human's deliberation, not a Sprint 38 dev blocker.

If Codex SURFACES a NEW Tier-0 candidate from the Sprint 38 implementation diff (not C1-C5): **`decision: out_of_scope_review`** with the candidate + surface quoted; deliver-agent + human evaluate at Sprint 38 close per Sprint 38 §10 stop condition #15. Do NOT block Sprint 38 close on a NEW Tier-0 candidate elevation; the elevation requires human-review escalation.

If a substance concern is found that is NOT in scope for Sprint 38 (e.g., a critique of the Sprint 37 design freeze itself — the freeze is approved + immutable; a critique of the M2 milestone scope — the milestone is approved; a critique of a pre-existing surface in `system_prompt.txt:54-101` — Sprint 37 OQ-7.11 verdict AGREE WITH DEV; reading the decision tree as if-else-in-prompt now would be Sprint 40+ scope per Sprint 37 OQ-pre-existing-surface routing precedent): **`decision: out_of_scope_review`** with the concern named and the milestone-contract / freeze / Sprint-37-precedent reference cited.

## 13. Self-check before submitting

- [ ] §2 scope-discipline gate walked; every disallowed surface checked against `git diff 51c327c..bd9d3f5 --stat`.
- [ ] §3 §4.1 nine-question kernel walked against the Sprint 38 IMPLEMENTATION DIFF per the canonical kernel loaded from `iteration_governance.md` §4.1; per-Q verdict aligned with or independently dissenting from dev handoff §6 self-walk.
- [ ] §4 §1.7 boundary check walked against 4 Skill YAML bodies + `composeSkillPhasePlan` + `SkillRegistry.select` + `Skill.appliesTo` + `SkillLoader.validate` + 4 deleted legacy phase branches + 2 preserved legacy RESOLVE branches.
- [ ] §5 hard-fence verification (Sprint 38 contract §6 31 items + M2 §6 22 items + behavioural equivalence golden-string preservation + Sprint 37 freeze fidelity + MATERIAL FINDING preservation + Sprint 33 cue/slot split preservation + premise re-verification).
- [ ] §6 schema + reproducibility (Skill / Guardrail / StateInheritance data models + SkillRegistry + SkillLoader shape + PhaseEvaluator integration + 4 Skill YAML schemas + test file structure + PhaseEvaluator line-count + cited file:line spot-checks).
- [ ] §7 Java baseline re-run from clean checkout; 1028/1-inherited/0/2 byte-identical; targeted regression spot-checks pass.
- [ ] §8 Tier-0 candidate continuation (C1 REJECTED by construction + C2 + C3 QUALIFIED-DEFER continuation + C4 + C5 NOT A CANDIDATE N/A); no NEW Tier-0 candidate surfaced (or surfaced + flagged for human escalation).
- [ ] §9 OQ independent verification (OQ-S38.1 INDEPENDENT verdict on template-vs-legacy editorial divergence; OQ-S38.2-S38.5 informational).
- [ ] §10 deferred items noted as non-blocking.
- [ ] `docs/codex-findings.md` written per §11 format.
- [ ] Verdict per §12 expected shape.
