Paste the content below this line into a fresh Codex session after the Sprint 40 dev commit lands. No PR will be opened; review the commit directly.

---

You are the Anti-Hardcode + Sprint-Close Review Agent for Sprint 40 — the FOURTH sub-sprint of **NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization, LLM-led Policy-bounded)** per `docs/milestone_objective.md`. Sprint 37 (M2 sub-sprint 1 design freeze) closed PASS A on 2026-05-17 at commit `51c327c`. Sprint 38 (M2 sub-sprint 2 SkillRegistry-core + 4 simpler phase Skills migration) closed **B-fix-iterated** 2026-05-17 across main-dev commit `bd9d3f5` + fix iteration #1 commit `5787806`. Sprint 39 (M2 sub-sprint 3 RESOLVE Skill migration + predicate migration + S1/S2 + unified dispatcher) closed **A — Clean PASS** 2026-05-18 at commit `2f412b6` per `docs/sprints/sprint-039-codex-review.md` (Codex `pass / blocking_count: 0` on first pass, single round).

Sprint 40 is the **smallest M2 implementation sub-sprint by scope** — purely prompt-side content relocation per Sprint 37 freeze decision (f) §7. The dev landed single commit `9130abc` (parent `2f412b6`) with 5 files (per `git show --stat 9130abc`):

- **EDIT** `server/src/main/resources/prompts/system_prompt.txt`: 101 → 80 lines (−21 net; +4 / −25 per git numstat). DELETED Sprint 31 `alternate_candidate_use_cases` paragraph (was pre-Sprint-40 lines 30-34) + Sprint 33 `discover_disambiguation_signals` SLOT description (was 36-43) + DISCOVER phase guidance bulk (was 45-52). ADDED orchestration-shell paragraph teaching Skill envelope mechanics + DISCOVER one-line pointer COMBINED into a single shell paragraph (post-Sprint-40 lines 30-31). PRESERVED Sprint 23 `already_called` paragraph (lines 23-28, content unchanged) + `request_handover` decision tree (post-Sprint-40 lines 33-80 = pre-Sprint-40 lines 54-101 shifted −21, content byte-identical).
- **EDIT** `server/src/main/resources/skills/discover_triage.yaml`: YAML structure UNCHANGED at 30 lines; `procedure` block (line 20, single-line YAML quoted string) extended from ~2900 chars to ~7900 chars. Appended: Sprint 31 paragraph (~830 chars, byte-identical to pre-Sprint-40 `system_prompt.txt:30-34` modulo whitespace→space flattening); Sprint 33 SLOT description (~1500 chars, byte-identical to pre-Sprint-40 `system_prompt.txt:36-43` modulo whitespace flattening); DISCOVER phase guidance refinements (~720 chars; only the NEW principle-level content from pre-Sprint-40 lines 45-52 — `reasoning` string requirement + 3-tier confidence guidance + explicit "?" clarification rule + escalate clauses; the pre-Sprint-40 DISCOVER lead-in already in `procedure` from Sprint 38 was DEDUPED per dev judgment, per handoff §4.1 + §1.7 risk #3). Sprint 33 ad-status disambiguation CUE BODY (migrated by Sprint 38 per design doc §6.2.1) PRESERVED alongside; the cue/slot split closes at Sprint 40 per design doc §7.2.3.
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java`: 264 lines; 11 Java-deterministic prompt-composition tests per design doc §7.8. Asserts: line-count reduction (#1); Sprint 23 `already_called` PRESERVED (#2); `request_handover` decision tree PRESERVED (#3); envelope-mechanics paragraph + DISCOVER pointer phrases ADDED (#4); Sprint 31 header ABSENT in shell (#5) + PRESENT in `procedure` (#6); Sprint 33 SLOT description ABSENT in shell (#7) + PRESENT in `procedure` alongside Sprint 38 cue body (#8); DISCOVER phase guidance bulk ABSENT in shell (#9) + PRESENT in `procedure` (#10); combined composition observability via production `SkillRegistry.select(...)` path (#11). Classpath read + production SkillRegistry lookup; no mocked LLM (`feedback_mocked_llm_cannot_prove_prompt_causal_change.md` honored — this is Java-composition observability, not LLM-behaviour evidence).
- **EDIT** `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java`: 341 → 400 lines (+59 net; +61 / −2 per git numstat). The `DISCOVER_SYSTEM_INSTRUCTION` golden assertion (Sprint 38) was a byte-identical golden against `discover_triage.yaml` `procedure` content; Sprint 40 extends `procedure`; the golden was extended to match. Other 12 tests in the file (CONFIRM × 3, CLOSE × 3, ESCALATE × 3, legacy-RESOLVE × 3 incl. the `resolve_intakeUc_stillFollowsLegacyBranchUntilSprint39` Sprint 38 OQ-S39.7 stale-name placeholder) UNCHANGED. Dev surfaces inclusion as OQ-S40.1; see §6 below.
- **NEW** `docs/sprints/sprint-040-handoff.md`: 312-line 12-section dev archive.

Total: 639 insertions / 25 deletions / 5 files per `git show --stat 9130abc`.

Per `iteration_governance.md` §4.3, Sprint 40 fires per-sub-sprint Codex review per **trigger #3** (system_prompt.txt structural change — the LLM input contract surface; the shell shifts from carrying DISCOVER-specific teaching content to projecting that content via the DISCOVER Skill envelope; the load-bearing claim is behavioural equivalence — the LLM observes the same teaching content via Skill envelope projection post-migration). Sprint 37 + Sprint 38 + Sprint 38-fix + Sprint 39 all received per-sub-sprint Codex review; Sprint 40 follows the same M2 implementation-sub-sprint cadence.

**Sprint 37 freeze decisions Sprint 40 implements (the in-scope subset):**

- **(f §7.1 mapping table row 2)** Sprint 31 `alternate_candidate_use_cases` teaching → `discover_triage.yaml` `procedure`. Byte-for-byte content (modulo whitespace flattening).
- **(f §7.1 mapping table row 3)** Sprint 33 `discover_disambiguation_signals` SLOT description → `discover_triage.yaml` `procedure`. Byte-for-byte content; alongside Sprint 33 ad-status disambiguation CUE BODY already migrated in Sprint 38 per (e) §6.2.1; the cue/slot split closes at Sprint 40 per §7.2.3.
- **(f §7.2.1)** Sprint 23 `already_called` STAYS in orchestration shell (cross-Skill envelope-mechanics teaching; applies to ANY Skill on ANY tool on ANY UC). `system_prompt.txt:23-28` content UNCHANGED.
- **(f §7.2.2)** Sprint 31 migrates to DISCOVER Skill; Alternative C REJECTED (no copy in RESOLVE-FAQ or RESOLVE-INTAKE YAMLs). Verify `resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml` UNCHANGED.
- **(f §7.2.3)** Sprint 33 SLOT migrates to DISCOVER Skill alongside Sprint 33 cue body from Sprint 38.
- **(f §7.3 orchestration-shell content rows)**:
  - Bot identity (lines 1-9) UNCHANGED.
  - Empty/non-empty tool_calls (lines 10-12) UNCHANGED.
  - Universal rules (lines 14-21) UNCHANGED.
  - `already_called` slot teaching (lines 23-28) UNCHANGED.
  - NEW envelope-mechanics paragraph + DISCOVER pointer (lines 30-31). Dev combined into single paragraph rather than separating per design doc §7.3 table's 2 rows — surface as OQ-S40.2 (see §6 below).
  - `request_handover` decision tree at post-Sprint-40 lines 33-80 (= pre-Sprint-40 lines 54-101 shifted −21) UNCHANGED in content.
- **(f §7.4 rationale)** Sprint 23 universally relevant; Sprint 31/33 phase-specific; shell shrinkage is reviewability benefit; decision tree stays cross-Skill.
- **(f §7.5 alternatives)** A REJECTED (full migration) + B REJECTED (keep all in shell) + C REJECTED (cross-Skill copy) + D REJECTED (`cross_skill_includes` field) — verify no Sprint 40 evidence to the contrary.
- **(f §7.6 §1.7 boundary check)** Migrated teaching is principle-level; no per-UC-pair if-else in `discover_triage.yaml` `procedure` post-Sprint-40.
- **(f §7.7 §1.3 / §1.4 boundary check)** Migrated teaching LLM-soft; no new Runtime enforcement; surface-relocation only.
- **(f §7.8 downstream sub-sprint reference)** Sprint 40 ships system_prompt.txt EDIT + discover_triage.yaml EDIT + behavioural-equivalence test. NOTE: §7.8 text reads "the slot-reading teaching (Sprint 31 from system_prompt.txt lines 36-43)" — this is a typo (lines 36-43 carried Sprint 33; Sprint 31 was lines 30-34). Dev implemented the CORRECT mapping. Surface as OQ-S40.5.

**Sprint 37 freeze decisions Sprint 40 does NOT implement (out-of-scope; preserved for downstream):**

- **(i §10) Session-level state model + `state_inheritance` enforcement** — Sprint 41 scope. `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]` was declared on Sprint 39 RESOLVE Skill YAMLs but NOT enforced (no `SkillStateBus.java`; `ContextProjectionBuilder.java` UNCHANGED at 1161 lines). Sprint 40 does NOT touch this. Verify via `git diff 2f412b6..9130abc -- server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` returns empty.
- **(h §9.9) C2 Tier-0 candidate elevation** — already deferred Sprint 37/38/39 close pre-decisions per `feedback_constitution_discipline_vs_planning_anticipation.md`. Sprint 40 ships content-relocation only, NOT a dispatcher / state-bus enforcement surface; no NEW C2 evidence surface expected.
- **(g §8.2.x) any Sprint 6/7/11/S1/S2 predicate change** — Sprint 39 landing preserved. `SkillGuardrailDispatcher.java` UNCHANGED at 416 lines. `PhaseEvaluator.java` UNCHANGED at 1427 lines. `AgentRunLoopImpl.java` UNCHANGED at 638 lines. Verify each via `git diff` returns empty.

**M2 §5 acceptance recalibration governs Sprint 40 close** per `docs/milestone_objective.md` §5 (2026-05-17, human-judgment recalibration of `iteration_governance.md` §5.6 primary-gate framing for THIS milestone): bad-case suite (Alice) + interactive eval composite_score are OBSERVATION ONLY (NOT hard gate) for M2; primary gate = functional review + Java tests pass + Sprint 37 freeze decisions honored across implementation sub-sprints + per-sub-sprint Codex review verdict `approve`. **Codex's review prompt MUST NOT re-import the §5.6 bad-case-suite-primary default for Sprint 40**; per-Q walk + behavioural-equivalence verification + Sprint 37 freeze §7 fidelity + Sprint 11/11.1/12 frozen surface preservation + M1 functional-surface preservation + Sprint 38/39 surfaces preservation + Java baseline preservation are the load-bearing surfaces. Same recalibration Sprint 37/38/38-fix/39 honored.

---

## 1. Loader

Read in this order on a fresh Codex session. Do NOT skip; the prompt's later sections assume each is loaded.

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §4.1 / §4.2 / §4.3 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — the NEW M2 milestone north star. Especially: §3 Sprint 40 row (Sprint 40 scope per the sub-sprint sequence); §5 acceptance recalibration (Alice + eval = OBSERVATION, NOT gate); §6 hard fences (22 items; esp. #1 no per-UC-branch if-else, #5 no `RuntimeIntentClassifier` / `DriftDetector` / etc touch, #8 no new Tier-0 invariant, #20 no OLD design deletion, #21 M1 functional-surface preservation).
3. **`docs/sprint_objective.md`** — the Sprint 40 contract (~330 lines). Read: §1 sub-sprint class (`prompt_projection` + `semantic_planner`); §2 Goal — 5 deliverables D-a/D-b/D-c/D-d/D-e + 1 OPTIONAL D-f (inherited test resolution; PERSISTS per dev §4.5); §3 Non-goals (explicit list); §4 Premise check (15 items dev re-verified at session start per handoff §3); §5 Files in scope (~4 files — 1 system_prompt.txt EDIT + 1 discover_triage.yaml EDIT + 1 NEW test file + handoff; dev shipped 5th file as PhaseEvaluatorSkillIntegrationTest golden update — see OQ-S40.1 in §6 below); §6 Hard fences (33 items); §7 Bundle policy; §8 §7 stanza (`prompt_projection` + `semantic_planner`); §9 Success metrics (HARD GATES + observations; Java baseline target 1099-1104; system_prompt.txt line-count target 60-75 — dev actual is 80, see §6 contract-drift item below); §10 Stop conditions (18 items); §11 Handoff §11 12-section contract; §12 M2 milestone context.
4. **`docs/sprints/sprint-040-handoff.md`** — the dev's archive (312 lines). Compare §9 (Files changed) against §5 of the objective; verify no scope creep beyond what OQ-S40.1 surfaces. §1.2 code-shape table verified at HEAD `2f412b6` (pre-Sprint-40). §3 walks the 15 §4 premises (15/15 PASS). §4 implementation walkthrough across D-a/D-b/D-c (single `procedure` edit) + D-d (system_prompt.txt replacement block) + D-e (new test) + golden update + D-f outcome (PERSISTS unchanged). §5 Sprint 37 freeze §7 fidelity table (15 rows; 2 minor deviations surfaced as OQ-S40.2 + OQ-S40.5). §6 §4.1 self-walk verdict `approve` per Q1-Q9. §7 5 OQs (OQ-S40.1 through OQ-S40.5; surface for INDEPENDENT verdict per §6 below). §8 anti-hardcode duplicate (cross-references §6). §9 files changed table. §10 layer-classification self-walk. §11 §5 acceptance bars adapted per M2 §5 recalibration. §12 closure verdict placeholder.
5. **`docs/proposals/skill_registry_design.md`** — Sprint 37 freeze (immutable per `doc_governance.md`); architectural authority for Sprint 40. Read sections referenced by Sprint 40 in scope:
   - **§7.1** mapping table (all 3 rows: Sprint 23 STAYS row 1; Sprint 31 → DISCOVER row 2; Sprint 33 → DISCOVER row 3).
   - **§7.2.1** rationale for Sprint 23 staying in shell.
   - **§7.2.2** rationale for Sprint 31 migrating + Alternative C REJECTED.
   - **§7.2.3** rationale for Sprint 33 SLOT migrating + cue/slot split with Sprint 38.
   - **§7.3** orchestration-shell content rows table (post-migration shell composition; the envelope-mechanics paragraph + DISCOVER one-line pointer are listed as 2 SEPARATE rows — dev combined them in implementation per OQ-S40.2).
   - **§7.4** rationale (Sprint 23 universally relevant; Sprint 31/33 phase-specific; shell shrinkage benefit).
   - **§7.5** alternatives (A/B/C/D all REJECTED).
   - **§7.6** §1.7 boundary check (principle-level migration; no per-UC-pair if-else).
   - **§7.7** §1.3 / §1.4 boundary check (LLM-soft preserved).
   - **§7.8** downstream sub-sprint reference — NOTE typo (Sprint 31 vs Sprint 33 line numbers swapped). OQ-S40.5.
6. **`docs/sprints/sprint-039-objective.md`** + **`docs/sprints/sprint-039-handoff.md`** + **`docs/sprints/sprint-039-codex-review.md`** — Sprint 39 archives. Read for the Sprint 39 RESOLVE Skill landing baseline (1094 / 1 inherited / 0 / 2 Java tests Sprint 40 inherits; Sprint 39 surfaces UNCHANGED in Sprint 40).
7. **`docs/sprints/sprint-038-objective.md`** + **`docs/sprints/sprint-038-handoff.md`** + **`docs/sprints/sprint-038-fix-handoff.md`** + **`docs/sprints/sprint-038-codex-review.md`** — Sprint 38 archives. Read for the SkillRegistry-core landing baseline + the existing `discover_triage.yaml` `procedure` body Sprint 40 EXTENDS (the Sprint 33 ad-status disambiguation CUE BODY migrated in Sprint 38 per decision (e) §6.2.1; Sprint 40 must preserve it alongside the SLOT description).
8. **`docs/sprints/sprint-037-objective.md`** + **`docs/sprints/sprint-037-handoff.md`** + **`docs/sprints/sprint-037-codex-review.md`** — Sprint 37 design freeze archives. Read for the freeze decision pre-text + Tier-0 candidate verdicts (C1 REJECTED preserved by construction; C2 + C3 QUALIFIED-DEFER; C4 + C5 NOT A CANDIDATE — Sprint 40 honors all; no new Tier-0 candidate expected from content-relocation).
9. **`docs/current/iteration_governance.md`** — §1.3 (LLM-owned — verify Sprint 40's `discover_triage.yaml` `procedure` post-migration is LLM-soft teaching at principle-level; verify NO per-UC-pair branching in the migrated paragraphs); §1.4 (Runtime-owned — verify Sprint 40 introduces no NEW Runtime enforcement; the migrated teaching content stays LLM-soft, NOT a guardrail); §1.7 (Forbidden — verify Sprint 40 has NO per-UC-branch if-else in `discover_triage.yaml`; no eval phrase encoding; no LLM-vs-Java boundary shift); §3.2 (`prompt_projection` Q3 + `semantic_planner` Q5 layer classification for Sprint 40 §7 stanza); §4.1 nine-question kernel (Codex independently re-walks against the Sprint 40 diff); §4.2 sprint-close header convention; §4.3 trigger #3 (Sprint 40 fires); §5 / §5.5 / §5.6 acceptance bars (Sprint 40 + M2 §5 recalibration: bad-case suite OBSERVATION not gate for THIS milestone); §7 sprint-objective stanza; §8 milestone framework.
10. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — current Tier-0 invariant set. Verify Sprint 40 dev commit ADDS no Tier-0 invariant (M2 §6 #8 fence preserved; C2 + C3 R-items not elevated; Sprint 40 is content-relocation only and ships no NEW dispatcher / state-bus enforcement surface — no fresh C2/C3 evidence expected).
11. **`docs/current/faq_grounding_contract.md`** — grounding-floor contract. Verify Sprint 40 does NOT touch the S1 `must_cite_source` predicate landed Sprint 39 (`SkillGuardrailDispatcher.java` UNCHANGED; `resolve_faq_grounded_answer.yaml` UNCHANGED).
12. **`compact/sprint-040-dev-prompt.md`** — what the dev was authorized to do vs what landed (cross-check for in-scope vs out-of-scope edits). Especially the 5 LOAD-BEARING STOP discipline items (Sprint 23 STAYS; decision tree STAYS; no per-UC if-else; no migration to RESOLVE Skills; no scope creep into Sprint 41).
13. **`compact/context-handoff-sprint-040-pre-launch.md`** — the deliver-agent pre-launch handoff. Especially: §4 decision records (single sub-sprint, content-relocation only; strong STOP discipline; M2 §5 recalibration carryforward); §7.4 the per-sub-sprint Codex review trigger framing (this prompt is the deliver-agent's product for §7.4).
14. **Code source files for cited-line spot-checking** at HEAD `9130abc` (read on demand during §3 + §4 + §5 verification, NOT end-to-end):
    - `server/src/main/resources/prompts/system_prompt.txt` (post-Sprint-40 80 lines; verify via `wc -l`).
    - `server/src/main/resources/skills/discover_triage.yaml` (post-Sprint-40 30 lines structure unchanged; `procedure` quoted string grew; verify via `wc -l` + visually scan the `procedure` block).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (UNCHANGED at 1427 lines from Sprint 39; verify via `git diff 2f412b6..9130abc -- server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (UNCHANGED at 638 lines from Sprint 39; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (UNCHANGED at 416 lines from Sprint 39; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (UNCHANGED at 222 lines; M1 functional surface; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` (UNCHANGED at 174 lines; verify).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` (UNCHANGED at 205 lines; Sprint 11 / 11.1 / 12 frozen surface; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (UNCHANGED at 1161 lines; Sprint 41 scope; verify via `git diff` returns empty).
    - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (UNCHANGED at 313 lines from Sprint 38-fix; verify via `git diff` returns empty).
    - `server/src/main/resources/skills/{confirm,escalate,terminal,resolve_faq_grounded_answer,resolve_intake_collect_and_handover}.yaml` (UNCHANGED; verify each via `git diff` returns empty).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` (NEW; 264 lines; 11 tests).
    - `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` (EDIT; 341 → 400 lines; +59; only the `DISCOVER_SYSTEM_INSTRUCTION` golden + the 3 DISCOVER tests' assertions touched; other 12 tests UNCHANGED).

## 2. Scope-discipline gate (BLOCKING)

Verify the dev commit at `9130abc` touches ONLY the surfaces enumerated in `docs/sprint_objective.md` §5 (Sprint 40 contract files in scope), with the EXCEPTION surfaced as OQ-S40.1 (see §6 below). Run:

```bash
git -C /Users/caoruixin/projects/csagent-latest diff 2f412b6..9130abc --stat
```

Expected file list (any extra file outside this list is a **BLOCKING scope violation**):

- **EDIT** `server/src/main/resources/prompts/system_prompt.txt` (101 → 80 lines; +4 / −25 per git numstat).
- **EDIT** `server/src/main/resources/skills/discover_triage.yaml` (30 lines structure unchanged; `procedure` quoted string grew; +2 / −1 per git numstat since the change is in-line within a single YAML quoted string).
- **NEW** `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` (264 lines; +264 / −0).
- **EDIT** `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` (341 → 400 lines; +61 / −2 per git numstat — `DISCOVER_SYSTEM_INSTRUCTION` golden update; surfaced as OQ-S40.1).
- **NEW** `docs/sprints/sprint-040-handoff.md` (312 lines; +312 / −0).

Total expected per `git show --stat 9130abc`: **5 files; +639 / −25**.

The 5th file (`PhaseEvaluatorSkillIntegrationTest.java` EDIT) is OQ-S40.1 — see §6 OQ verification.

**Deliver-agent-owned files NOT staged in dev commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`**: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-040-*.md`, anything under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-039-*` (immutable archives), anything in `docs/milestones/`, anything in `docs/foundational/`, anything in `docs/current/` (governance docs), anything in `docs/proposals/`, anything in `eval_interactive/`. Verify each via `git diff 2f412b6..9130abc --name-only | grep -v -E '<expected-prefix>'` returns empty. Human bundles deliver-agent files at Sprint 40 close.

## 3. Anti-Hardcode kernel (§4.1) independent re-walk

Walk all 9 `iteration_governance.md` §4.1 questions on the Sprint 40 dev diff (5 files). Sprint 40 is content-relocation; the migrated teaching content moves from monolithic `system_prompt.txt` shell to `discover_triage.yaml` `procedure` envelope projection. Verify INDEPENDENTLY (dev's self-walk at handoff §6 is dev-authored, not Codex's independent verdict):

1. **Q1** — keyword / regex / if-else / enum / per-UC matrix for a semantic decision? Verify the migrated content in `discover_triage.yaml` `procedure` (post-Sprint-40) carries NO per-UC-pair branch table; the Sprint 31 paragraph uses general "When ... is populated ..." language; the Sprint 33 SLOT description uses "When a later user turn surfaces evidence ..." language; the DISCOVER phase guidance refinements use "Confidence guidance: >= 0.7 ... >= 0.5 ... below 0.5 ..." language. Verify the new envelope-mechanics shell paragraph is principle-level cross-Skill teaching, NOT `if Skill.name == "..." then ...` logic.
2. **Q2** — Tier-0 justification? Verify Sprint 40 adds NO Tier-0 invariant. `docs/runtime_freeze_and_risk_policy.md` UNCHANGED. C2 (guardrail refusal non-overridability) + C3 (state-bus boundary enforcement) stay DEFER per Sprint 37/38/39 close pre-decisions; Sprint 40 ships NO new dispatcher / state-bus surface that would change this.
3. **Q3** — soft signal achievable instead of hard branch? Verify Sprint 40 introduces no new hard branch (in Java OR prompt). The migrated teaching content stays LLM-soft (was LLM-soft in `system_prompt.txt`; stays LLM-soft in `discover_triage.yaml` `procedure`).
4. **Q4** — visible-eval / trace phrasing / CaseSpec id encoded? Spot-check the migrated `procedure` body + the envelope-mechanics paragraph + the new test file for `alice_*`, `cs_*`, source session id (`3772e56b-*` etc.), bad-case-suite text, visible-eval phrase. Verify the test file asserts substring matches on STABLE architectural phrases ("`already_called` projection slot", "Skill envelope", "Confidence guidance: >= 0.7"), NOT on CaseSpec-derived text.
5. **Q5** — semantic ownership shift LLM → Java? Verify §1.3 LLM ownership preserved. The migrated teaching is LLM-soft teaching content (was LLM-soft in shell; stays LLM-soft in Skill `procedure`); no semantic decision is shifted to Java. The runtime continues to surface projection slots (`alternate_candidate_use_cases` + `discover_disambiguation_signals`); the LLM continues to own UC hypothesis, drift handling, escalation posture. §1.4 Runtime ownership also preserved: tool schemas UNCHANGED; `PhasePlan.allowedTools` composition UNCHANGED; PII/safety floor UNCHANGED; grounding floor UNCHANGED (Sprint 39 S1 stays — no Sprint 40 extension).
6. **Q6** — if-else block in prompt instead of principle-level guidance? Inspect the post-Sprint-40 `discover_triage.yaml` `procedure` body: every migrated paragraph reads at principle level. Spot-check the migrated Sprint 31 + Sprint 33 SLOT description + DISCOVER phase guidance refinements against the pre-Sprint-40 `system_prompt.txt:30-34/36-43/45-52` for byte-for-byte equivalence modulo whitespace flattening. The `request_handover` decision tree at post-Sprint-40 lines 33-80 is the situation-to-enum mapping per Sprint 37 OQ-7.11 AGREE WITH DEV verdict (NOT per-UC-pair).
7. **Q7** — tool schema / capability / PII / grounding floor preserved? Verify tool schemas UNCHANGED (no tool YAML or `*Tool.java` edits in Sprint 40); capability/permission boundary UNCHANGED (`PhasePlan.allowedTools` still pulls from `skill.toolsRequired()`; `discover_triage.yaml` `tools_required` UNCHANGED at `[search_knowledge, classify_use_case]`); PII/safety floor UNCHANGED (`AgentRunLoopImpl` UNCHANGED); grounding floor UNCHANGED.
8. **Q8** — generalization coverage (target / neighbor / negative / shadow)? Verify target = 3 migrated paragraphs + 1 new envelope-mechanics paragraph + Sprint 23 + decision tree preserved (11 `SkillTeachingMigrationIntegrationTest` tests; positive PRESERVED + negative REMOVED + positive ADDED assertions). Neighbor = existing 13 `PhaseEvaluatorSkillIntegrationTest` cases (DISCOVER × 3 with updated golden + CONFIRM × 3 + CLOSE × 3 + ESCALATE × 3 + legacy-RESOLVE × 1) + 14 `PhaseEvaluatorResolveSkillIntegrationTest` from Sprint 39 + Sprint 38 `SkillLoaderTest` (18) + `SkillRegistryTest` (11) + `SkillTest` (9) — all GREEN post-Sprint-40 per dev handoff §11. Negative = `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED (M1 functional surface). Shadow = N/A per M2 §5 recalibration (architecture-focused milestone).
9. **Q9** — rollback / sunset? Verify no temporary measure / kill-switch introduced. `git revert 9130abc` restores `system_prompt.txt` + `discover_triage.yaml` exactly to pre-Sprint-40 state. The Skill Registry abstraction itself stays (Sprint 38 / Sprint 39 surfaces preserved).

Output: per-Q verdict (`pass` / `concern` / `fail`); a single Q `fail` is a blocking finding.

## 4. §1.7 Boundary check

Walk each `iteration_governance.md` §1.7 forbidden item against the Sprint 40 diff:

- **(a) encoding raw eval phrases into Java or prompt** — verify the migrated `procedure` body + envelope-mechanics paragraph carry NO Alice / bad-case-suite / source-session-id text.
- **(b) adding UC-specific hard rules for soft semantic decisions** — verify the migrated paragraphs use general "When ... condition ... then ..." language; no per-UC-pair if-else.
- **(c) widening eval spec to accept a genuine bot mistake** — verify Sprint 40 ships no `eval_interactive/` edits.
- **(d) optimizing visible eval at the cost of shadow/generalization** — N/A per M2 §5 recalibration; verify Sprint 40 ships no smoke composite_score change as primary evidence.
- **(e) using prompt as an if-else rule dump** — verify the post-Sprint-40 `system_prompt.txt` (80 lines) carries no per-UC-branch table; verify the migrated `discover_triage.yaml` `procedure` (~7900 chars) carries no per-UC-pair table.

Output: per-item verdict.

## 5. Hard-fence verification (Sprint 40 contract §6 + M2 contract §6)

Verify each hard fence cited in Sprint 40 contract §6 (33 items) + M2 contract §6 (22 items). High-value spot-checks:

- **Sprint 40 §6 #1 + M2 §6 #1**: NO per-UC-branch if-else in `discover_triage.yaml` post-Sprint-40 (verified in §3 Q1/Q6 above).
- **Sprint 40 §6 #2**: `request_handover` decision tree at post-Sprint-40 lines 33-80 PRESERVED unchanged (`git diff 2f412b6..9130abc -- server/src/main/resources/prompts/system_prompt.txt` shows the decision-tree block UNCHANGED in content; line range shifted −21 due to deletions above).
- **Sprint 40 §6 #3**: Sprint 23 `already_called` paragraph at `system_prompt.txt:23-28` UNCHANGED content (verify via diff).
- **M2 §6 #4**: Sprint 39 S1 `must_cite_source` bounded surface UNCHANGED in Sprint 40 (verify `resolve_faq_grounded_answer.yaml` + `SkillGuardrailDispatcher.handleMustCiteSource` UNCHANGED via `git diff` returns empty).
- **M2 §6 #5**: NO touch to `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` (verify via `git diff 2f412b6..9130abc --name-only` returns empty for these files).
- **M2 §6 #6 + #7**: NO edit to `INTAKE_UCS` set; NO widening of `escalation_reason` enum (verify via `git diff` on relevant files returns empty).
- **M2 §6 #8**: NO new Tier-0 invariant (verify `git diff 2f412b6..9130abc -- docs/runtime_freeze_and_risk_policy.md` returns empty).
- **M2 §6 #20**: NO OLD design deletion (the Sprint 11 / 11.1 / 12 frozen surface stays; `ResolveDispositionEvaluator.java` UNCHANGED).
- **M2 §6 #21**: M1 functional surface preserved (`IntakeFieldsRegistry.java` + `IntakeFieldExtractor.java` + `UseCaseRegistryService.java` UNCHANGED; `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED).
- **Sprint 40 §6 #29-#33** (or analogous): NO `SkillStateBus.java` created; NO `ContextProjectionBuilder.java` edit; NO `prior_use_case_carry` slot impl (Sprint 41 scope).

Output: per-fence verdict.

## 6. OQ items — INDEPENDENT verdict required

Dev surfaced 5 OQs in handoff §7. The deliver-agent has NOT pre-loaded dispositions; Codex returns INDEPENDENT verdicts on each. The deliver-agent + human will read your verdicts at Sprint 40 close and arbitrate against their own assessment.

### OQ-S40.1 — Existing test golden update included in dev commit (5th file)

**The question:** Sprint 40 contract `docs/sprint_objective.md` §5 listed 4 files in scope (`system_prompt.txt` EDIT + `discover_triage.yaml` EDIT + `SkillTeachingMigrationIntegrationTest.java` NEW + `sprint-040-handoff.md` NEW). The dev commit `9130abc` ALSO includes a 5th file: `PhaseEvaluatorSkillIntegrationTest.java` EDIT (`DISCOVER_SYSTEM_INSTRUCTION` golden update; +61 / −2 per git numstat; other 12 tests UNCHANGED).

**Dev's defense at handoff §1.7 risk #1 + §4.4 + §7 OQ-S40.1:** without this update, contract §6 #32 ("no regression on PhaseEvaluatorSkillIntegrationTest") would fail. The Sprint 38 `DISCOVER_SYSTEM_INSTRUCTION` golden was a byte-identical assertion against `discover_triage.yaml` `procedure`; Sprint 40 extends `procedure`; the golden was extended to match — a content-equivalence update, NOT a behaviour change. Sprint 39 precedent: dev updated 8 EDIT test files for the migration to be observable; same pattern.

**Codex verdict required:** (a) is this in scope per Sprint 39 precedent + `feedback_commit_at_end_bundles_deliver_artefacts.md` interpretation? OR (b) is this a contract §5 scope deviation that merits fix iteration? Verify the golden update IS content-equivalent (the post-Sprint-40 golden = pre-Sprint-40 base content + appended migrated content; the assertion shape — byte-for-byte against YAML — unchanged) by reading the diff at `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java`. If (b), classify as `fix_required` (split out the golden update into a separate housekeeping commit, OR amend Sprint 40 contract retroactively, OR ...).

### OQ-S40.2 — D-d envelope-mechanics paragraph + DISCOVER pointer placement / combination

**The question:** Sprint 40 contract §2.4 listed 3 placement options for D-d (after universal rules at line 21; OR after `already_called` paragraph at line 28; OR before the DISCOVER one-line pointer; "Exact wording at dev judgement; the constraint is content-level not phrasing-level"). Design doc `docs/proposals/skill_registry_design.md` §7.3 listed the envelope-mechanics paragraph + the DISCOVER one-line pointer as 2 SEPARATE rows in the orchestration-shell content table.

**Dev's choice at handoff §1.5 + §4.2 + §5 row "NEW envelope-mechanics paragraph + DISCOVER pointer" + §7 OQ-S40.2:** combined into a single shell paragraph block (post-Sprint-40 lines 30-31) for brevity; the DISCOVER pointer is a one-liner that naturally fits as the closing sentence of the envelope-mechanics paragraph. The `SkillTeachingMigrationIntegrationTest.systemPrompt_postSprint40_addsEnvelopeMechanicsParagraph` test (#4) asserts both content phrases.

**Codex verdict required:** (a) AGREE WITH DEV — the combination respects the design doc §7.3 mapping at content level (both phrases present; LLM observes both); OR (b) DISAGREE — the combination is a §7.3 mapping deviation that should be refactored to 2 separate paragraphs at Sprint 40 close. Read the post-Sprint-40 `system_prompt.txt:30-31` content + compare to design doc §7.3 mapping table rows. If (b), classify as `fix_required` and propose the refactor scope.

### OQ-S40.3 — Inherited SystemPromptUserRequestedTiebreakerTest persists (D-f outcome)

**The question:** Sprint 40 contract §2.6 said the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` MAY resolve organically if Sprint 40's structural change addresses the underlying drift. The inherited test asserts `prompt.contains("Sprint 6")` (literal anchor token) alongside `prompt.contains("ACTIVE-UC TIEBREAKER")`. Sprint 40 dev handoff §4.5 reports: PERSISTS unchanged. Sprint 40 does NOT touch the `ACTIVE-UC TIEBREAKER` block (at post-Sprint-40 lines 54-55 = pre-Sprint-40 lines 75-76) per contract §6 #2 the decision tree stays. The "Sprint 6" anchor token has been absent from the live `system_prompt.txt` since a Sprint 24-era working-tree mod (documented baseline; Sprint 39 codex review §7 + Sprint 40 verification confirm).

**Codex verdict required:** classify as (a) STATUS QUO — not a Sprint 40 close blocker per contract §10 #18 ("do NOT make it a hard gate at Sprint 40 close"); OR (b) IMPROVING — Sprint 40 partially addressed the drift, defer to M2 close housekeeping; OR (c) BLOCKING — Sprint 40 should have resolved this and didn't. If (c), propose what specifically the dev should have done within Sprint 40 scope (note: adding back the "Sprint 6" anchor would be a deliberate annotation in the post-Sprint-40 shell, arguably out of Sprint 40 scope per §6 #2).

### OQ-S40.4 — Sprint 23 `already_called` cosmetic re-wording

**The question:** Sprint 40 contract §6 #1 permitted cosmetic re-wording of the Sprint 23 paragraph to reference "Skill envelope" framing instead of implicit phase/plan framing. Dev's choice at handoff §1.5 + §4.2 + §7 OQ-S40.4: NO re-wording. Reason: existing Sprint 23 paragraph reads "This guidance applies to every tool — there is no tool-name or use-case branching" which already presents as cross-Skill envelope-mechanics teaching; the new envelope-mechanics paragraph (D-d, immediately following) establishes the Skill envelope framing for the rest of the shell. Smaller diff.

**Codex verdict required:** (a) AGREE WITH DEV — no re-wording is correct (existing phrasing already cross-Skill framed); OR (b) DISAGREE — the re-wording would have been preferable for consistency with the new envelope-mechanics framing. Read post-Sprint-40 `system_prompt.txt:23-28` and compare to design doc §7.2.1. Note: §6 #1 PERMITS re-wording but does NOT mandate; (b) verdict should classify as INFORMATIONAL not `fix_required`.

### OQ-S40.5 — Sprint 37 freeze doc §7.8 typo (Sprint 31 / Sprint 33 line-number swap)

**The question:** `docs/proposals/skill_registry_design.md` §7.8 text reads (approximately) "the slot-reading teaching (Sprint 31 from system_prompt.txt lines 36-43)" — this is a typo; lines 36-43 in pre-Sprint-40 `system_prompt.txt` carried the Sprint 33 SLOT description (not Sprint 31). Sprint 31 was lines 30-34. Sprint 40 dev implemented the CORRECT mapping (Sprint 33 from lines 36-43; Sprint 31 from lines 30-34) per handoff §5 freeze fidelity table last row + §7 OQ-S40.5. The freeze doc itself is immutable per `doc_governance.md` (Sprint 40 contract §6 #16 + Sprint 38 OQ-S38.1 precedent for design-doc-vs-implementation divergence routes to governance fold-back).

**Codex verdict required:** (a) ROUTE to M2 close editorial fold-back per the Sprint 38 OQ-S38.1 precedent — deliver-agent + human at M2 close fix the typo in `docs/proposals/skill_registry_design.md` §7.8 as a SEPARATE governance commit; NO Sprint 40 fix iteration warranted; OR (b) the typo is material enough to fix earlier (e.g., immediately as a docs-only side-commit). Verify the typo exists in `docs/proposals/skill_registry_design.md` §7.8 by reading the file; confirm Sprint 40 dev implemented the CORRECT line-number mapping (NOT the typo'd one).

## 7. Two additional contract-drift items to classify

### Drift item 1 — system_prompt.txt line-count target missed

**Background:** Sprint 40 contract §9 target was 60-75 lines post-Sprint-40. Dev actual is 80 lines per `wc -l server/src/main/resources/prompts/system_prompt.txt`. Dev's explanation per handoff §11 "Observations" + §1.7 risk #4: the combined envelope-mechanics + DISCOVER pointer paragraph (OQ-S40.2) is shorter than the contract-anticipated separation; the existing 7-line Sprint 23 paragraph + 47-line `request_handover` decision tree are byte-identical to pre-Sprint-40 (preserved per §6 #1 + #2).

**Codex verdict required:** classify as (a) CONTENT-EQUIVALENCE PRESERVED — line-count is an observation not a hard gate; tightness target was advisory; the substantive content equivalence + Sprint 23 + decision tree preservation are the load-bearing surfaces; OR (b) TIGHTNESS CONCERN — Sprint 40 should have hit the contract target; propose the specific 5+ lines that could have been collapsed without losing teaching. If (b), classify as `fix_required` ONLY if the gap reflects a content issue, not as `fix_required` if it is merely a tightness preference.

### Drift item 2 — Sprint 39 precedent for "behavioural-equivalence test edits don't count as scope creep"

**Background:** Sprint 39's contract §5 listed 14 files; dev shipped 21 files (8 EDIT test files via constructor + dispatcher-helper sweep). Sprint 39 Codex review classified those as in-scope behavioural-equivalence updates (`pass / 0`). Sprint 40 contract §5 lists 4 files; dev shipped 5 (OQ-S40.1). Does the Sprint 39 precedent extend?

**Codex verdict required:** reaffirm OR push back on the precedent. If REAFFIRM: classify OQ-S40.1 as the precedent's continuation (in-scope behavioural-equivalence update; not scope creep). If PUSH BACK: classify OQ-S40.1 as scope creep (the precedent should not extend — Sprint 39 had 8 EDIT test files because of unified-dispatcher constructor sweep across many test fixtures; Sprint 40 has 1 EDIT test file for a golden update — different shape).

## 8. Tier-0 candidate disposition (informational; reaffirm)

Sprint 37 freeze §12 surfaced 5 candidates:

- **C1** REJECTED preserved by construction (Sprint 39 close confirmed; Sprint 40 ships nothing that would change this).
- **C2** QUALIFIED-DEFER (guardrail refusal non-overridability). Sprint 39 dispatcher landing recorded as FIRST observed evidence surface but production trace evidence pending. Sprint 40 ships content-relocation only — NO new dispatcher / state-bus enforcement surface; no fresh C2 evidence expected. **DEFER continues.**
- **C3** QUALIFIED-DEFER (state-bus boundary enforcement). Sprint 41 ships the FIRST observed evidence surface (not yet shipped). Sprint 40 ships nothing C3-relevant. **DEFER continues.**
- **C4** + **C5** NOT A CANDIDATE preserved.

**Codex verdict required:** REAFFIRM (no Sprint 40 evidence to change disposition) OR raise alternative. Reaffirm is the expected verdict.

## 9. Schema and reproducibility checks

Verify the Sprint 40 dev handoff's reproducibility per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`:

- Every line-count claim cites a file path. Spot-check 5-10 claims via `wc -l <path>`.
- Every commit-shape claim is reproducible via `git show --stat 9130abc` or `git diff 2f412b6..9130abc --stat` or `git diff 2f412b6..9130abc --numstat`.
- Every "X UNCHANGED" claim is reproducible via `git diff 2f412b6..9130abc -- <path>` returning empty.
- Every Sprint 37/38/38-fix/39 baseline citation is reproducible via the named sprint archive at `docs/sprints/sprint-NNN-*.md`.

Surface any non-reproducible claim as a finding (informational unless it changes the verdict).

## 10. Validation runs

Run from a clean working tree:

```bash
cd /Users/caoruixin/projects/csagent-latest/server
mvn clean test -q 2>&1 | tail -25
```

**Expected verdict for Sprint 40 close:**
- `Tests run: 1105, Failures: 1, Errors: 0, Skipped: 2`.
- The sole failure is the documented inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` (D-f outcome — OQ-S40.3 PERSISTS).
- Java baseline trajectory: 1094 (post-Sprint-39) → 1105 (post-Sprint-40) = +11 new tests from `SkillTeachingMigrationIntegrationTest`.

Targeted spot-check runs:

```bash
# New Sprint 40 tests — verify 11/11 PASS
mvn -pl server test -Dtest=SkillTeachingMigrationIntegrationTest -q

# Existing Sprint 38 test with golden update — verify 13/13 PASS
mvn -pl server test -Dtest=PhaseEvaluatorSkillIntegrationTest -q

# Sprint 39 tests — verify UNCHANGED
mvn -pl server test -Dtest=PhaseEvaluatorResolveSkillIntegrationTest,SkillGuardrailDispatcherTest,ResolveFaqGuardrailsTest,ResolveIntakeGuardrailsTest -q

# Sprint 38 SkillRegistry-core tests — verify UNCHANGED
mvn -pl server test -Dtest=SkillTest,SkillRegistryTest,SkillLoaderTest -q

# M1 functional surface — verify UNCHANGED
mvn -pl server test -Dtest=Sprint71PartialIntakePersistenceTest -q

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
summary: <one paragraph — Codex's verdict on Sprint 40 close>
```

Then the per-section structure (Review Evidence, Blocking Findings, Anti-Hardcode Kernel, §1.7 Boundary Check, Hard-Fence Verification, Schema And Reproducibility Checks, Validation Runs, Tier-0 Candidate Independent Verification, **OQ Independent Verification** [populate with 5 OQ verdicts + 2 contract-drift verdicts], Deferred / Non-Blocking Notes).

A single blocking finding → `fix_required` (`blocking_count: 1+`). Zero blocking findings → `pass` (`blocking_count: 0`). Any verdict that would expand the milestone scope or invent a new architectural surface → `out_of_scope_review` (deliver-agent + human re-direct).

## 12. Anti-patterns to refuse

- Do NOT re-import `iteration_governance.md` §5.6 bad-case-suite-primary default (M2 §5 recalibration governs for THIS milestone).
- Do NOT extrapolate scope from the dev's surfaced OQs (every OQ is independent classification, not a scope-expansion invitation).
- Do NOT elevate C2 or C3 to Tier-0 in your verdict (Tier-0 elevation is deliver-agent + human authority per `feedback_constitution_discipline_vs_planning_anticipation.md` + M2 §10 stop conditions; if you believe elevation is warranted, route as `out_of_scope_review` and surface the evidence).
- Do NOT propose code fix beyond naming the layer per §3.2 of `iteration_governance.md`.
- Do NOT propose edits to `docs/runtime_freeze_and_risk_policy.md`, `docs/proposals/skill_registry_design.md`, `docs/milestone_objective.md`, OR any `docs/sprints/sprint-NNN-*.md` archive (these are deliver-agent / human authority — out-of-scope for review).
- Do NOT rewrite the dev's `docs/sprints/sprint-040-handoff.md` (it is the immutable dev archive; the §12 closure verdict will be filled by deliver-agent + human at Sprint 40 close).
- Do NOT introduce new Tier-0 candidates in the verdict (Sprint 37 surfaced 5; that surface is closed unless new structural evidence surfaces; Sprint 40 is content-relocation and ships no fresh enforcement surface).

---

End of Sprint 40 review prompt. Return your verdict in `docs/codex-findings.md` per §11 above.
