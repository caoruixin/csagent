Paste the content below this line into a fresh Claude Code session after the human commits the deliver-agent Sprint 39 close housekeeping bundle. The Sprint 40 sub-sprint contract is `docs/sprint_objective.md` at HEAD.

---

You are Claude Code working as the **dev agent** for **Sprint 40 — Teaching extraction from `system_prompt.txt` + orchestration shell cleanup** (the FOURTH sub-sprint of NEW Milestone M2: Skill Registry Abstraction + Wholesale Retroactive Externalization, per `docs/milestone_objective.md`). Sprint 37 (design freeze) closed PASS A at commit `51c327c`. Sprint 38 (SkillRegistry core + 4 simpler phase Skills migration) closed B-fix-iterated across `bd9d3f5` + `5787806`. Sprint 39 (RESOLVE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 + unified dispatcher) closed A — Clean PASS at `2f412b6` per `docs/sprints/sprint-039-codex-review.md`.

Sprint 40 is an **implementation sub-sprint** (content-relocation; smaller scope than Sprint 39 — purely prompt-side cleanup; no Java/code change beyond a NEW behavioural-equivalence integration test). The scope is locked at `docs/sprint_objective.md` §2 with 5 deliverables D-a / D-b / D-c / D-d / D-e + 1 optional D-f.

Sprint 40 implements **Sprint 37 freeze decision (f) §7** (retroactive migration mapping for Sprint 23/31/33 teaching paragraphs):

- **D-a** Sprint 31 `alternate_candidate_use_cases` paragraph (`system_prompt.txt:30-34`) → `discover_triage.yaml` `procedure` per design doc §7.2.2.
- **D-b** Sprint 33 `discover_disambiguation_signals` SLOT description paragraph (`system_prompt.txt:36-43`) → `discover_triage.yaml` `procedure` per design doc §7.2.3 (closes the Sprint 33 cue/slot split — Sprint 38 already migrated the cue body per decision (e) §6.2.1).
- **D-c** DISCOVER phase guidance bulk (`system_prompt.txt:45-52`) → `discover_triage.yaml` `procedure` per design doc §7.3 default recommendation; leave one-line shell pointer in `system_prompt.txt`.
- **D-d** ADD orchestration-shell paragraph about Skill envelope mechanics (cross-Skill universal; placement at dev judgement) per design doc §7.3 table row.
- **D-e** NEW `SkillTeachingMigrationIntegrationTest` — Java-deterministic prompt-composition tests per design doc §7.8.
- **D-f** (OPTIONAL): MAY resolve inherited `SystemPromptUserRequestedTiebreakerTest` failure if the structural change addresses the underlying drift; if not, OK — same baseline inherits.

Sprint 40 explicitly DOES NOT:

- Delete Sprint 23 `already_called` paragraph (`system_prompt.txt:23-28`) — stays per design doc §7.2.1 (cross-Skill envelope-mechanics teaching).
- Delete `request_handover` decision tree (`system_prompt.txt:54-101`) — stays per design doc §7.3 (cross-Skill canonical enum).
- Migrate Sprint 31 or Sprint 33 paragraphs into RESOLVE-FAQ or RESOLVE-INTAKE Skills — Alternative C REJECTED per §7.2.2.
- Touch any Java code (`PhaseEvaluator.java` / `AgentRunLoopImpl.java` / `SkillGuardrailDispatcher.java` / `ResolveDispositionEvaluator.java` / `IntakeFieldsRegistry.java` / `UseCaseRegistryService.java` / `ContextProjectionBuilder.java`) — Sprint 40 is prompt-side cleanup only.
- Touch other Skill YAMLs (`confirm.yaml` / `escalate.yaml` / `terminal.yaml` / `resolve_faq_grounded_answer.yaml` / `resolve_intake_collect_and_handover.yaml`) — only `discover_triage.yaml` extends.
- Add `SkillStateBus.java` or impl NEW `prior_use_case_carry` projection slot — Sprint 41 scope.
- Touch `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` — M2 §6 #5 fence + M3-D Topic↔UC binding deferred.
- Touch `INTAKE_UCS` set or widen `escalation_reason` enum.
- Add Tier-0 invariants without explicit human-review escalation.
- Touch governance docs (`iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md`).
- Touch the Sprint 37 freeze doc (`docs/proposals/skill_registry_design.md`).
- Touch existing case families / shadow / harness / Alice bad case / `case_spec_overrides.yaml`.
- Use mocked-LLM as primary evidence for any LLM-behaviour claim.

## 1. Read order on cold start

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — NEW M2 north star. Especially §3 Sprint 40 row (this sub-sprint's scope) + §5 acceptance recalibration (Alice + eval = OBSERVATION, NOT gate) + §6 hard fences (22 items).
3. **`docs/sprint_objective.md`** — Sprint 40 contract. ALL 12 sections binding.
4. **`docs/proposals/skill_registry_design.md`** §7 (decision (f) — Sprint 23/31/33 teaching migration mapping; lines 1932-2128 per `grep -n "^## 7\\.\\|^## 8\\." docs/proposals/skill_registry_design.md`). LOAD-BEARING for Sprint 40. Other sections CONTEXTUAL only.
5. **`docs/sprints/sprint-039-handoff.md`** — Sprint 39 dev archive. Read §1.2 code shape table (line numbers verified at HEAD `5787806` for pre-Sprint-39 + handoff §4 walkthrough for post-Sprint-39). Sprint 40 inherits the post-Sprint-39 code shape.
6. **`docs/sprints/sprint-039-codex-review.md`** — Sprint 39 Codex review archive (`pass / 0` verdict; OQ disposition incl. OQ-S39.5 C2 DEFER; §10 deferred non-blocking notes).
7. **`docs/sprints/sprint-038-handoff.md`** — Sprint 38 dev archive. Especially: the Sprint 33 ad-status disambiguation CUE BODY at `PhaseEvaluator.java:432-448` was migrated into `discover_triage.yaml` `procedure` per design doc §6.2.1 (Sprint 40 closes the Sprint 33 cue/slot split by adding the SLOT description from `system_prompt.txt:36-43` alongside).
8. **`docs/current/iteration_governance.md`** §1.3 (LLM-owned — verify Sprint 40 content-relocation does NOT shift LLM-owned decisions to Java) + §1.4 (Runtime-owned — verify Sprint 40 adds NO new Runtime-floor enforcement) + §1.7 (Forbidden — verify Sprint 40 has NO per-UC-branch if-else in `discover_triage.yaml` post-migration; no eval phrase encoding; no LLM-vs-Java boundary shift) + §3.2 (`prompt_projection` Q3 + `semantic_planner` Q5 layer classification per Sprint 40 stanza) + §4.1 nine-question kernel (you self-walk against the Sprint 40 diff at handoff §6) + §4.2 sprint-close header convention + §4.3 trigger #3 (Sprint 40 fires per-sub-sprint Codex review) + §5 / §5.5 / §5.6 acceptance bars (Sprint 40 + M2 §5 recalibration) + §7 sprint-objective stanza + §8 milestone framework.
9. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — verify NO Tier-0 invariant added at Sprint 40.
10. **Code source files to spot-check at HEAD `2f412b6`** (read on demand during §3 premise re-verification + §4 implementation, NOT end-to-end):
    - `server/src/main/resources/prompts/system_prompt.txt` (101 lines pre-Sprint-40; verify line-by-line content matches premise check §4 item 4 in contract).
    - `server/src/main/resources/skills/discover_triage.yaml` (~30 lines pre-Sprint-40; carries Sprint 33 CUE BODY migrated in Sprint 38).
    - `server/src/main/resources/skills/confirm.yaml` + `escalate.yaml` + `terminal.yaml` + `resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml` (5 OTHER production Skill YAMLs; UNCHANGED in Sprint 40 — verify per premise check).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (1427 lines post-Sprint-39; UNCHANGED in Sprint 40).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (638 lines post-Sprint-39; UNCHANGED).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (416 lines post-Sprint-39; UNCHANGED).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (222 lines; UNCHANGED).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` (205 lines; UNCHANGED; Sprint 11/11.1/12 frozen surface).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (1161 lines; UNCHANGED; Sprint 41 scope for `prior_use_case_carry` slot impl).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (~228 lines; UNCHANGED; allowlists Sprint 38-fix shipped already cover Sprint 40 needs).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` + `Guardrail.java` + `StateInheritance.java` + `SkillRegistry.java` + `RejectVerdict.java` + `DispatchContext.java` (all UNCHANGED).

## 2. Goal (5 + 1 deliverables)

### D-a Sprint 31 `alternate_candidate_use_cases` paragraph migration

REMOVE from `server/src/main/resources/prompts/system_prompt.txt:30-34`; ADD into `server/src/main/resources/skills/discover_triage.yaml` `procedure` (extension; place alongside the Sprint 33 cue migrated in Sprint 38; principle-level teaching at content level). The paragraph teaches the LLM how to read the `alternate_candidate_use_cases` projection slot — UCs the intake router considered plausible at session creation, surfaced as soft evidence the LLM may use to consider a reroute. Phase-specific to DISCOVER. Per design doc §7.2.2 final paragraph: only place the teaching in `discover_triage.yaml`; rely on the projection slot's intrinsic self-documentation via the slot description for resolve-side reads. NO migration into RESOLVE-FAQ or RESOLVE-INTAKE Skills (Alternative C REJECTED per §7.2.2).

### D-b Sprint 33 `discover_disambiguation_signals` SLOT description paragraph migration

REMOVE from `server/src/main/resources/prompts/system_prompt.txt:36-43`; ADD into `server/src/main/resources/skills/discover_triage.yaml` `procedure` (extension; alongside Sprint 33 cue body already there from Sprint 38). The paragraph teaches the LLM how to read the `discover_disambiguation_signals` projection slot — a structured object surfacing ad-status disambiguation signals. The Sprint 33 cue body at `PhaseEvaluator.java:432-448` was migrated into `discover_triage.yaml` `procedure` in Sprint 38 per design doc §6.2.1 (the cue/slot split: cue migrated in Sprint 38; slot description migrates in Sprint 40). Post-Sprint-40, `discover_triage.yaml` `procedure` carries BOTH:
- (i) the slot-reading teaching (Sprint 33 from system_prompt.txt lines 36-43);
- (ii) the cue teaching (Sprint 33 from PhaseEvaluator lines 432-448 migrated in Sprint 38).

### D-c DISCOVER phase guidance bulk migration

REMOVE bulk content from `server/src/main/resources/prompts/system_prompt.txt:45-52`; ADD into `server/src/main/resources/skills/discover_triage.yaml` `procedure` (where it duplicates content already there, dedupe; where it adds new principle-level guidance, append). Per design doc §7.3 default recommendation: migrate the bulk into `discover_triage.yaml`; leave a one-line shell pointer in `system_prompt.txt` (replacing lines 45-52) — exact wording at dev judgement, suggested: "Phase-specific guidance lives in the Skill `procedure`; refer to your current Skill envelope for the active phase's directives."

### D-d Orchestration-shell teaching about Skill envelope mechanics

ADD a new paragraph (1-2 sentences; concise; principle-level) to `server/src/main/resources/prompts/system_prompt.txt`. Placement at dev judgement — suggested options: (a) after the universal rules block at line 21; (b) after the `already_called` paragraph at line 28; (c) before the DISCOVER one-line pointer at the post-Sprint-40 position. Suggested wording per design doc §7.3 table row:

> You operate within the Skill envelope the runtime selects per (phase, active_use_case). The envelope names the tool whitelist (you cannot dispatch a tool outside it), the recommended procedure (LLM-soft guidance, deviate when judgement warrants), the grounding instruction (LLM-soft), and the escalation policy (LLM-soft). Skill-declared guardrails enforce Runtime-floor invariants (e.g., a `record_outcome(class=resolve)` without a source_id citation will be rejected). Respect the envelope; exercise judgment on response strategy per the Constitution.

Exact wording is dev judgement; constraint is content-level (the LLM observes the framing post-Sprint-40), not phrasing-level. The paragraph teaches the LLM about the envelope-projection contract introduced in Sprint 38 + extended in Sprint 39; cross-Skill universal (applies to any Skill the runtime selects).

### D-e Behavioural-equivalence integration test

NEW `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` per design doc §7.8 — Java-deterministic prompt-composition tests. The test verifies the migrated teaching content reaches the LLM via Skill envelope projection equivalently to prior monolithic `system_prompt.txt`. Suggested test surface (5-10 tests):

- Post-Sprint-40 `system_prompt.txt` line-count is materially smaller than 101 lines (target: ~60-75 lines; assert < 90 lines as conservative gate).
- Post-Sprint-40 `system_prompt.txt` CONTAINS Sprint 23 `already_called` content (substring assertion; cross-Skill teaching preserved).
- Post-Sprint-40 `system_prompt.txt` CONTAINS `request_handover` decision tree content (substring assertion on a stable phrase from lines 54-101; cross-Skill canonical enum preserved).
- Post-Sprint-40 `system_prompt.txt` CONTAINS new orchestration-shell envelope-mechanics paragraph (substring assertion on key phrase like "Skill envelope" or "Runtime-floor invariants").
- Post-Sprint-40 `system_prompt.txt` DOES NOT CONTAIN the Sprint 31 paragraph as it was at lines 30-34 (substring negative assertion).
- Post-Sprint-40 `system_prompt.txt` DOES NOT CONTAIN the Sprint 33 SLOT description as it was at lines 36-43 (substring negative assertion).
- Post-Sprint-40 `discover_triage.yaml` `procedure` CONTAINS Sprint 31 teaching content (substring positive assertion).
- Post-Sprint-40 `discover_triage.yaml` `procedure` CONTAINS Sprint 33 SLOT description content (substring positive assertion).
- Post-Sprint-40 `discover_triage.yaml` `procedure` CONTAINS DISCOVER phase guidance migrated from lines 45-52 content (substring positive assertion).
- Post-Sprint-40 projected LLM input for a DISCOVER turn (composed via SkillRegistry-driven `composeSkillPhasePlan(...)` + system_prompt.txt) CONTAINS both the shell content AND the discover_triage Skill envelope — observability of migrated teaching to the LLM. Use Mockito-mocked LLM or simply verify the assembled prompt string (no real LLM invocation — Java composition only per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` evidence rules).

Exact test set is dev judgement; the constraint is the post-migration LLM-input contract is observable + verifiable.

### D-f (OPTIONAL) Inherited test resolution

The `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure has persisted since Sprint 24-era as the documented dirty-working-tree baseline. Sprint 40's structural change to `system_prompt.txt` MAY address the underlying drift if the failure is rooted in `system_prompt.txt` content. If it resolves organically: surface in handoff §7 OQ as informational note. If it persists: also OK; Sprint 40 close inherits the same baseline. **Do NOT make resolving this test a hard gate or expand Sprint 40 scope to chase it.**

## 3. Read order specific to deliverables

- For D-a / D-b / D-c: read `server/src/main/resources/prompts/system_prompt.txt` lines 23-52 (the migration source) + read `server/src/main/resources/skills/discover_triage.yaml` (the migration target; verify Sprint 33 cue body already there from Sprint 38; identify where to extend `procedure`).
- For D-d: read `server/src/main/resources/prompts/system_prompt.txt` lines 1-28 + lines 54-101 (the parts that stay; understand the orchestration shell's content for placement decision).
- For D-e: read `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` (Sprint 38; pattern for Skill-composed PhasePlan tests) + `PhaseEvaluatorResolveSkillIntegrationTest.java` (Sprint 39; golden-string assertions); the Sprint 40 test follows the Java-composition pattern but with substring-level assertions (not byte-for-byte; the migration content is not byte-for-byte preserved — the orchestration shell paragraph re-frames "Skill envelope" mechanics).

## 4. STOP discipline (5 LOAD-BEARING + 13 total per contract §10)

The 5 LOAD-BEARING STOPs (must NOT silently work around):

1. **No deletion of Sprint 23 `already_called` paragraph** (`system_prompt.txt:23-28`) — STAYS per design doc §7.2.1. Sprint 40 MAY slightly re-word to reference "Skill envelope" framing (surface in handoff §7 OQ); content unchanged.
2. **No deletion of `request_handover` decision tree** (`system_prompt.txt:54-101`) — STAYS per design doc §7.3. The 23-value canonical enum is cross-Skill.
3. **No per-UC-branch if-else in `discover_triage.yaml` post-migration** per Constitution §1.7 + M2 §6 #1. The migrated Sprint 31 + Sprint 33 + DISCOVER phase guidance paragraphs are principle-level teaching; if you find yourself needing a per-UC-pair branch table to encode any migrated content, STOP and refine.
4. **No migration into RESOLVE-FAQ or RESOLVE-INTAKE Skill YAMLs** per design doc §7.2.2 Alternative C REJECTED. Cross-Skill copy is a maintenance hazard.
5. **No scope creep into Sprint 41** — STOP if tempted to add `SkillStateBus.java` OR impl `prior_use_case_carry` slot in `ContextProjectionBuilder.java`. Sprint 41 scope per design doc §10.

The other STOPs (per contract §10):

- Premise drift, behavioural equivalence test fails, §1.7 boundary case, §1.3 / §1.4 boundary mismatch, Tier-0 candidate surfaces, tempted to edit governance docs / freeze doc / draft Sprint 41 contract / use mocked-LLM as primary evidence / modify Alice bad case.

If any STOP condition fires: STOP and surface in handoff §7 OQ for deliver-agent + human review.

## 5. Hard fences (per contract §6; 33 items)

All 33 fences enumerated in contract §6 are LOAD-BEARING. The most operationally relevant:

- No edit to ANY Java code in Sprint 40 dev commit.
- No edit to ANY other Skill YAML (only `discover_triage.yaml` extends).
- No edit to `system_prompt.txt:23-28` content beyond cosmetic re-wording (Sprint 23 paragraph STAYS).
- No edit to `system_prompt.txt:54-101` (`request_handover` decision tree STAYS).
- No `SkillStateBus.java` created; no `ContextProjectionBuilder.java` edit; no `prior_use_case_carry` slot impl.
- No touch to `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool`.
- No edit to `INTAKE_UCS` set; no widening of `escalation_reason` enum.
- No edit to `IntakeFieldsRegistry.java` / `IntakeFieldExtractor.java` / `UseCaseRegistryService.java` / `ResolveDispositionEvaluator.java`.
- No Tier-0 invariant added to `docs/runtime_freeze_and_risk_policy.md`.
- No edit to governance docs / `docs/proposals/skill_registry_design.md` / sprint archives / milestone archives / deliver-agent-owned files / `eval_interactive/` surfaces.
- No mocked-LLM as primary evidence for LLM-behaviour claims.
- No per-UC-branch if-else in `discover_triage.yaml` post-migration.
- No regression on `Sprint71PartialIntakePersistenceTest` 14/14 or Sprint 38 + Sprint 39 tests.

## 6. §4.1 anti-hardcode self-walk template

At handoff §6, walk all 9 questions on the Sprint 40 IMPLEMENTATION DIFF. Expected verdict per Q:

- **Q1** (keyword / regex / if-else / enum / per-UC matrix for semantic decision?): NO — the migrated paragraphs are principle-level teaching content; no per-UC-pair logic added.
- **Q2** (Tier-0 justification?): N/A — no Tier-0 added. C2 + C3 R-items stay DEFER per Sprint 37/38/39 close pre-decisions.
- **Q3** (soft signal achievable?): N/A — Sprint 40 is content-relocation. The migrated teaching content remains LLM-soft principle-level guidance (was LLM-soft in `system_prompt.txt`; stays LLM-soft in `discover_triage.yaml` `procedure`).
- **Q4** (visible-eval / trace phrasing / CaseSpec id encoded?): NO — read the migrated content + new envelope-mechanics paragraph; no `alice_*` CaseSpec id, no trace phrasing, no bad-case-suite text.
- **Q5** (semantic ownership shift LLM → Java?): NO — §1.3 LLM ownership preserved (the migrated content is teaching, not decision-making logic); §1.4 Runtime ownership preserved (tool schemas unchanged; capability/permission boundary unchanged; PII/safety floor unchanged; grounding floor unchanged — S1 from Sprint 39 stays).
- **Q6** (if-else in prompt?): NO — the migrated paragraphs are principle-level; the existing `request_handover` decision tree at lines 54-101 was Sprint 37 OQ-7.11 reviewed AGREE WITH DEV (situation-to-enum, not per-UC-pair); Sprint 40 preserves it unchanged.
- **Q7** (tool schema / capability / PII / grounding floor preserved?): YES — tool schemas unchanged; `PhasePlan.allowedTools` composition unchanged; PII/safety floor unchanged; grounding floor unchanged (S1 from Sprint 39 stays; no new gate added).
- **Q8** (generalization coverage?): YES — target = 3 migrated paragraphs + 1 new envelope-mechanics paragraph + Sprint 23 + decision tree preserved; `SkillTeachingMigrationIntegrationTest` 5-10 tests cover post-migration composition observability. Neighbor = Sprint 38 + Sprint 39 surfaces preserved; their tests remain green. Negative = `Sprint71PartialIntakePersistenceTest` 14/14 + M1 functional-surface tests preserved.
- **Q9** (rollback / sunset?): N/A — `git revert <sprint-40-commit>` restores `system_prompt.txt` + `discover_triage.yaml` exactly.

Expected verdict: **`approve`**.

## 7. Handoff §11 12-section contract

Standard 12-section shape. Notable adaptations for Sprint 40:

- §1 Context Pack — cite Sprint 39 close commit `2f412b6` as parent; verify all 15 premises in contract §4 at session start.
- §4 Implementation walkthrough — table mapping each Sprint 40 deliverable (D-a/D-b/D-c/D-d/D-e/D-f) to the implementation file + line range that honors it.
- §5 Sprint 37 freeze fidelity — for each design doc §7 mapping cell, cite implementation site.
- §6 Self-walk §4.1 kernel — per template above; verdict `approve`.
- §7 OQ surfacing — anything material the dev surfaced (e.g., Sprint 23 re-wording decision; D-f outcome on inherited test failure; line-count actual vs target; placement decision for envelope-mechanics paragraph).
- §11 Acceptance bars — adapted per M2 §5 recalibration (Alice + eval = OBSERVATION; primary gate = functional review + Java tests + Sprint 37 freeze §7 honored).
- §12 Closure verdict placeholder — DO NOT FILL; deliver-agent + human + Codex own per `feedback_handoff_verdict_section_delegation.md`.

## 8. Bundle policy

Single dev commit with all dev-authored files:

- `server/src/main/resources/prompts/system_prompt.txt` EDIT
- `server/src/main/resources/skills/discover_triage.yaml` EDIT
- `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` NEW
- `docs/sprints/sprint-040-handoff.md` NEW

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage `docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-040-*.md`. Deliver-agent + human bundle those at sub-sprint close.

Commit message template (adapt):

```
sprint 40: Teaching extraction from system_prompt.txt (Sprint 23/31/33 + DISCOVER guidance + envelope mechanics) (NEW M2 sub-sprint 4)

Sprint 40 is the FOURTH sub-sprint of NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) and the THIRD implementation sub-sprint after Sprint 37 design freeze + Sprint 38 SkillRegistry-core landing + Sprint 39 RESOLVE Skill migration + predicate migration + S1/S2 + unified dispatcher landing. Sprint 40 implements Sprint 37 freeze decision (f) §7 as runtime code.

Ships:

- system_prompt.txt EDIT: DELETE Sprint 31 paragraph (lines 30-34) + Sprint 33 SLOT description (lines 36-43) + DISCOVER phase guidance bulk (lines 45-52); REPLACE lines 45-52 with one-line shell pointer; ADD orchestration-shell paragraph about Skill envelope mechanics; Sprint 23 paragraph (lines 23-28) preserved per §7.2.1; request_handover decision tree (lines 54-101) preserved per §7.3. Post-Sprint-40 line count: 101 → ~XX lines.

- discover_triage.yaml EDIT: EXTEND procedure block with migrated Sprint 31 + Sprint 33 SLOT description + DISCOVER phase guidance bulk content (alongside Sprint 33 cue body from Sprint 38). All other fields UNCHANGED.

- SkillTeachingMigrationIntegrationTest NEW: ~X Java-deterministic prompt-composition tests verifying post-migration LLM input contract observability.

- docs/sprints/sprint-040-handoff.md NEW: 12-section dev archive.

[Sprint 37 freeze fidelity table; OQ surfaces; §4.1 self-walk verdict approve; Java baseline; closure verdict placeholder for deliver-agent + human + Codex.]

Refs: docs/sprint_objective.md (Sprint 40 contract);
docs/milestone_objective.md (NEW M2 milestone);
docs/proposals/skill_registry_design.md §7 (Sprint 37 freeze, immutable).

Co-Authored-By: Claude [your-model] <noreply@anthropic.com>
```

## 9. Self-check before commit

- [ ] All 15 premises in contract §4 re-verified at session start.
- [ ] D-a + D-b + D-c migrations applied to `system_prompt.txt` and `discover_triage.yaml`; no per-UC-branch if-else introduced.
- [ ] D-d envelope-mechanics paragraph ADDED at chosen placement in `system_prompt.txt`.
- [ ] D-e tests pass.
- [ ] D-f inherited test outcome noted (resolves OR persists).
- [ ] Sprint 23 paragraph + `request_handover` decision tree preserved in `system_prompt.txt`.
- [ ] No edit to ANY Java code beyond `SkillTeachingMigrationIntegrationTest.java` NEW.
- [ ] No edit to other Skill YAMLs.
- [ ] No `SkillStateBus.java` created; no `ContextProjectionBuilder.java` edit.
- [ ] No touch to other fenced surfaces per contract §6 (33 items walked).
- [ ] Java baseline preserved (1094 + ~5-10 new = ~1099-1104; 0 new failures; 1 inherited persists OR resolves; 2 skipped).
- [ ] `Sprint71PartialIntakePersistenceTest` 14/14 preserved.
- [ ] Sprint 38 + Sprint 39 tests preserved.
- [ ] §4.1 self-walk verdict `approve` per template §6 above.
- [ ] Handoff §11 12-section shape complete.
- [ ] §12 closure verdict placeholder LEFT EMPTY for deliver-agent + human + Codex.
- [ ] Commit message follows template.
- [ ] Bundle policy honored (dev stages ONLY §5 files; NOT deliver-agent-owned files).
