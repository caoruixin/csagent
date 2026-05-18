---
title: Sprint 40 handoff — Teaching extraction from system_prompt.txt + orchestration shell cleanup (M2 sub-sprint 4)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-18
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Dev-authored archive of Sprint 40 (NEW Milestone M2 sub-sprint 4).
  Implements Sprint 37 freeze decision (f) §7 as runtime code:
  Sprint 31 + Sprint 33 SLOT description + DISCOVER phase-guidance
  bulk teaching paragraphs migrated from `system_prompt.txt` into
  `discover_triage.yaml` `procedure`; new orchestration-shell
  paragraph teaching Skill envelope mechanics ADDED; Sprint 23
  `already_called` paragraph + `request_handover` decision tree
  PRESERVED in shell per design doc §7.2.1 + §7.3. Behavioural
  equivalence verified via new `SkillTeachingMigrationIntegrationTest`
  (11 tests) + existing `PhaseEvaluatorSkillIntegrationTest` (13 tests,
  golden DISCOVER systemInstruction updated to match extended
  procedure). Java baseline preserved: 1105 / 1 inherited / 0 / 2
  (= 1094 post-Sprint-39 + 11 new). The §12 closure verdict is left
  for deliver-agent + human + Codex per-sub-sprint review per
  `feedback_handoff_verdict_section_delegation.md`.
---

# Sprint 40 handoff — Teaching extraction from system_prompt.txt + orchestration shell cleanup (NEW M2 sub-sprint 4)

**Commit:** to be assigned at handoff bundle (single dev commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`).
**Parent:** `2f412b6` (Sprint 39 close — A Clean PASS).
**Branch:** `refactor/remove-the-shackles`.

## 1. Context Pack

### 1.1 Relevant docs sampled

- `AGENTS.md` — constitution chain entry; loads `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`.
- `docs/current/iteration_governance.md` — §1.3 LLM-owned + §1.4 Runtime-owned + §1.7 forbidden-list + §3.2 layer classification + §4.1 anti-hardcode kernel (self-walked on the Sprint 40 diff at §6) + §4.2 sprint-close header + §4.3 trigger #3 (Sprint 40 fires per-sub-sprint Codex review) + §5 / §5.5 / §5.6 acceptance (Sprint 40 + M2 §5 recalibration) + §7 sprint-objective stanza + §8 milestone framework.
- `docs/milestone_objective.md` — NEW M2 milestone. §3 Sprint 40 row + §5 recalibration (Alice + eval = observation NOT gate) + §6 hard fences (22 items) + §8 Codex review plan (per-sub-sprint).
- `docs/sprint_objective.md` — Sprint 40 contract. All 12 sections binding.
- `docs/proposals/skill_registry_design.md` §7 (decision (f) §7.1-§7.8 — Sprint 23/31/33 teaching migration mapping; load-bearing).
- `docs/sprints/sprint-039-handoff.md` §1.2 (code shape at HEAD `5787806` for pre-Sprint-39) + §4 (post-Sprint-39 surfaces).
- `docs/sprints/sprint-039-codex-review.md` — Sprint 39 Codex verdict `pass / 0` confirms the Sprint 39 surfaces Sprint 40 inherits.
- `docs/sprints/sprint-038-handoff.md` — Sprint 38 archive (DISCOVER Skill `procedure` already carries the Sprint 33 ad-status disambiguation CUE BODY per decision (e) §6.2.1; Sprint 40 closes the cue/slot split).
- `docs/runtime_freeze_and_risk_policy.md` §1 + §2 — verified NO Tier-0 invariant added at Sprint 40.

### 1.2 Verified code shape at HEAD `2f412b6` (session start)

All 15 §4 premises re-verified at session start; see §3 below for the per-premise PASS / drift report.

| file | line count pre-Sprint-40 | landmark verification |
|---|---|---|
| `server/src/main/resources/prompts/system_prompt.txt` | 101 | Lines 23-28 Sprint 23 `already_called` (STAYS); 30-34 Sprint 31 `alternate_candidate_use_cases` (MIGRATED); 36-43 Sprint 33 `discover_disambiguation_signals` SLOT description (MIGRATED); 45-52 DISCOVER phase guidance (MIGRATED + one-line shell pointer remains); 54-101 `request_handover` decision tree (STAYS) — all matched contract §4 premise #4 |
| `server/src/main/resources/skills/discover_triage.yaml` | 30 | Existing `procedure` already carries Sprint 33 ad-status disambiguation CUE BODY from Sprint 38 per decision (e) §6.2.1; Sprint 40 EXTENDS `procedure` with Sprint 31 + Sprint 33 SLOT + DISCOVER guidance — all matched contract §4 premise #5 |
| `server/src/main/resources/skills/{confirm,escalate,terminal,resolve_faq_grounded_answer,resolve_intake_collect_and_handover}.yaml` | 29 / 28 / 25 / 52 / 37 | UNCHANGED in Sprint 40 (only `discover_triage.yaml` edits) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 1427 | UNCHANGED (Sprint 39 post-edit; Sprint 40 is prompt-side cleanup only) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` | 638 | UNCHANGED |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` | 416 | UNCHANGED |
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` | 222 | UNCHANGED (M1 functional surface) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java` | 174 | UNCHANGED |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java` | 205 | UNCHANGED (Sprint 11 / 11.1 / 12 frozen surface) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 1161 | UNCHANGED (Sprint 41 scope for `prior_use_case_carry` slot impl) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` | 313 | UNCHANGED; `VALID_PROJECTION_SLOTS` carries 3 entries already (`alternate_candidate_use_cases`, `discover_disambiguation_signals`, `prior_use_case_carry`) — no allowlist extension required |
| Java baseline pre-Sprint-40 | 1094 / 1 inherited / 0 / 2 | mvn test full-run confirmed at Sprint 39 close per codex-review §7 + reproduced at Sprint 40 session start |

### 1.3 Feedback memory references

- `feedback_commit_at_end_bundles_deliver_artefacts.md` — followed: dev commit excludes `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-040-*.md`. Sprint 40 dev stages ONLY the 4 §5 dev-authored files + the one ancillary update to `PhaseEvaluatorSkillIntegrationTest.java` per §7 OQ-S40.1 below.
- `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — followed: every line-number citation in this handoff cites a specific file + line / range at HEAD `2f412b6` (pre-Sprint-40) or in the post-Sprint-40 dev commit; the test count delta (1094 → 1105 = +11 new) is reproduced by `mvn test` (handoff §6 + §11).
- `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — followed: `SkillTeachingMigrationIntegrationTest` is a Java-deterministic prompt-composition assertion (classpath read of system_prompt.txt + production SkillRegistry lookup) — NOT an LLM-behaviour claim. No mocked LLM is used as primary evidence; no real-LLM eval rerun is shipped as primary evidence. Per M2 §5 recalibration, Alice + interactive eval are observations not gates for Sprint 40 close.
- `feedback_handoff_verdict_section_delegation.md` — followed: §12 closure verdict left for deliver-agent + human + Codex per-sub-sprint review at Sprint 40 close.
- `feedback_constitution_discipline_vs_planning_anticipation.md` — followed: did NOT pre-elevate C2 (guardrail refusal non-overridability) or C3 (state-bus boundary enforcement) to Tier-0; Sprint 40 ships content-relocation only — no new dispatcher / state-bus enforcement surface emerges; C2/C3 stay DEFER per Sprint 37/38/39 close pre-decisions.

### 1.4 Doc-status warnings

None new in Sprint 40. The Sprint 37 freeze design doc §7 template and the legacy text in `system_prompt.txt` lines 30-34 / 36-43 / 45-52 are byte-identical to the migrated content placed into `discover_triage.yaml` `procedure` (modulo whitespace flattening — YAML single-line string format collapses paragraph breaks). No editorial divergence between freeze template and migrated content. The Sprint 39-handoff-internal reproducibility drift noted by Codex (file counts) is unaffected by Sprint 40.

### 1.5 Source-of-truth decision

For each Sprint 40 deliverable:

- **D-a Sprint 31 paragraph migration:** the LEGACY content at `system_prompt.txt:30-34` pre-Sprint-40 is the source-of-truth; the Sprint 40 `discover_triage.yaml` `procedure` extension copies it byte-for-byte (whitespace flattened to YAML quoted-string).
- **D-b Sprint 33 SLOT description migration:** the LEGACY content at `system_prompt.txt:36-43` pre-Sprint-40 is the source-of-truth; the Sprint 40 `discover_triage.yaml` `procedure` extension copies it byte-for-byte.
- **D-c DISCOVER phase guidance bulk migration:** the LEGACY content at `system_prompt.txt:45-52` pre-Sprint-40 is the source-of-truth for the NEW principle-level content (confidence guidance + `reasoning` string + escalate clauses); the existing `discover_triage.yaml` `procedure` already covers the high-level "identify use case, call classify_use_case, otherwise ask clarifying question, escalate if OOS" content from Sprint 38, so D-c adds the refinement principles without re-stating the lead-in (light dedupe per contract §2.3).
- **D-d envelope-mechanics paragraph:** the suggested wording from design doc §7.3 table row is the source-of-truth; dev judgment per contract §2.4 picked the wording verbatim from the design doc with one minor adjustment (added the DISCOVER one-line pointer "Phase-specific guidance lives in the Skill `procedure`; refer to your current Skill envelope for the active phase's directives" inline in the same paragraph rather than as a separate compressed paragraph — combined for shell brevity; surface in §7 OQ-S40.2).
- **D-e behavioural-equivalence test:** the Java composition surface (classpath read + production SkillRegistry) is the source-of-truth for the assertion shape; assertions are substring-based on stable phrases per contract §2.5.

### 1.6 Implementation status

- `system_prompt.txt` post-Sprint-40 at 80 lines (was 101; −21 lines / ~21% reduction) — `implemented`.
- `discover_triage.yaml` `procedure` extension (same 30 YAML lines; the procedure string itself grew ~3× in character count) — `implemented`.
- `SkillTeachingMigrationIntegrationTest` 11 tests — `implemented`; all PASS.
- `PhaseEvaluatorSkillIntegrationTest` DISCOVER_SYSTEM_INSTRUCTION golden updated to match extended `procedure`; 13/13 tests still PASS — `implemented`.
- Inherited `SystemPromptUserRequestedTiebreakerTest` failure — **PERSISTS** unchanged (same baseline; the inherited test asserts a literal "Sprint 6" anchor token in `system_prompt.txt`; Sprint 40 does NOT touch the `ACTIVE-UC TIEBREAKER` block at pre-Sprint-40 lines 75-76 / post-Sprint-40 lines 54-55, so the anchor's absence is unchanged). D-f outcome surfaced as OQ-S40.3.

### 1.7 Risks called out at session start

1. **`PhaseEvaluatorSkillIntegrationTest.DISCOVER_SYSTEM_INSTRUCTION` golden update tension with contract §5.** The contract §5 lists only 4 files in scope; updating an existing test's golden string is not enumerated. But contract §6 #32 says "no regression on PhaseEvaluatorSkillIntegrationTest" — meaning the test must keep passing. Sprint 39 precedent (Sprint 39 dev updated multiple existing test files to match the migrated content) governs. Mitigation: included the golden update in the Sprint 40 dev commit; surfaced as OQ-S40.1.
2. **DISCOVER `procedure` YAML string grew ~3×.** Risk: the SkillLoader / Jackson YAML parsing surface might choke on a long quoted string. Mitigation: verified post-edit by running `PhaseEvaluatorSkillIntegrationTest` (13/13 PASS), `SkillLoaderTest` (18/18 PASS), `SkillRegistryTest` (11/11 PASS); the SnakeYAML parser handles arbitrarily long quoted strings.
3. **D-c dedupe vs full append decision.** Lines 45-52 cover content partially overlapping with existing `procedure` (the lead-in "identify use case, call classify_use_case" + escalate principle is already in `procedure`). Risk: full append duplicates content; aggressive dedupe loses principle-level guidance. Mitigation: kept ONLY the NEW principle-level content (the `reasoning` string requirement; 3-tier confidence guidance; the explicit "ask one focused clarifying question (user_message ending with ?)" clause). The high-level lead-in is dropped because it was already in the existing procedure pre-Sprint-40. Result: `procedure` carries each principle once.
4. **D-d placement choice.** Contract §2.4 lists 3 options for the envelope-mechanics paragraph placement: (a) after universal rules at line 21; (b) after `already_called` paragraph at line 28; (c) before the DISCOVER one-line pointer. Dev judgment per §1.5 above: placement (b) — combined the envelope-mechanics paragraph + the DISCOVER one-line pointer into a single shell paragraph immediately after `already_called`. Mitigation: surface in OQ-S40.2 for Codex / human review at Sprint 40 close.

## 2. Sub-sprint-objective recap

Sprint 40 is the FOURTH sub-sprint of NEW Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) and the THIRD implementation sub-sprint after Sprint 37 design freeze + Sprint 38 SkillRegistry-core landing + Sprint 39 RESOLVE Skill migration + predicate migration + S1/S2 + unified dispatcher landing. Per the contract: smaller scope than Sprint 39 — content-relocation only; no Java code change beyond the new behavioural-equivalence test + the existing test's golden update.

Layer: `prompt_projection` (system_prompt.txt structure change — the LLM input contract shifts from monolithic prompt to Skill envelope projection for DISCOVER-specific teaching) + `semantic_planner` (LLM continues to own §1.3 surface; the migration is surface-relocation, NOT LLM-ownership shift). Semantic-touching multi-layer; §7 stanza REQUIRED.

## 3. Premise re-verification

| premise | contract §4 expected | session-start verified | drift? |
|---|---|---|---|
| 1. M2 milestone approved + Sprint 39 closed | docs/milestone_objective.md `current`; Sprint 39 closed A Clean PASS @2f412b6; Codex `pass / 0` | confirmed | PASS |
| 2. Sprint 40 contract approved | docs/sprint_objective.md is binding scope | confirmed | PASS |
| 3. Sprint 37 freeze design doc | docs/proposals/skill_registry_design.md `status: proposal` immutable; §7 §7.1-§7.8 load-bearing | confirmed | PASS |
| 4. system_prompt.txt structure | 101 lines; landmark line ranges per contract §4 #4 | confirmed (`wc -l` = 101; landmark lines 23-28 / 30-34 / 36-43 / 45-52 / 54-101 match) | PASS |
| 5. discover_triage.yaml | ~30 lines; carries Sprint 33 cue body from Sprint 38 | confirmed (30 lines; existing `procedure` includes "Sprint 33 ad-status disambiguation cue" body) | PASS |
| 6. PhaseEvaluator.java | 1427 lines UNCHANGED | confirmed (not edited in Sprint 40) | PASS |
| 7. AgentRunLoopImpl.java | 638 lines UNCHANGED | confirmed (not edited) | PASS |
| 8. SkillGuardrailDispatcher.java | 416 lines UNCHANGED | confirmed (not edited) | PASS |
| 9. IntakeFieldsRegistry.java | 222 lines UNCHANGED | confirmed (not edited) | PASS |
| 10. UseCaseRegistryService.java | 174 lines UNCHANGED | confirmed (not edited) | PASS |
| 11. ResolveDispositionEvaluator.java | 205 lines UNCHANGED | confirmed (not edited) | PASS |
| 12. ContextProjectionBuilder.java | 1161 lines UNCHANGED | confirmed (not edited) | PASS |
| 13. 6 production Skill YAMLs present | discover/confirm/escalate/terminal + resolve_faq/resolve_intake | confirmed; only `discover_triage.yaml` edited in Sprint 40 | PASS |
| 14. SkillLoader.VALID_PROJECTION_SLOTS = 3 entries | alternate_candidate_use_cases + discover_disambiguation_signals + prior_use_case_carry | confirmed at `SkillLoader.java:65-69` | PASS |
| 15. Java baseline 1094/1-inherited/0/2 | Sprint 39 close baseline | confirmed via mvn test from clean at session start | PASS |

All 15 premises hold within tolerance. No drift triggers contract §10 STOP condition #1.

## 4. Implementation walkthrough

Sprint 40 is content-relocation only. The dev commit touches 4 files (system_prompt.txt + discover_triage.yaml + 1 new test file + 1 existing test golden update + the handoff). No Java code change beyond test files. No new Java surface; no new Skill YAML.

### 4.1 D-a + D-b + D-c — discover_triage.yaml `procedure` extension (single edit)

The pre-Sprint-40 `procedure` (line 20 of `discover_triage.yaml`) carried:

- DISCOVER lead-in ("You are in the DISCOVER phase. Your goal is to identify which Use Case applies to the customer...").
- Sprint 7 §I0 weak-candidate cue.
- Sprint 33 ad-status disambiguation cue (migrated in Sprint 38 per design doc §6.2.1).

Sprint 40 EXTENDS `procedure` (single edit; same YAML line 20; the string content grew from ~2900 chars to ~7900 chars) by appending:

- **D-a:** Sprint 31 `alternate_candidate_use_cases` paragraph (~830 chars), byte-identical to `system_prompt.txt:30-34` pre-Sprint-40 modulo newline → space flattening.
- **D-b:** Sprint 33 `discover_disambiguation_signals` SLOT description (~1500 chars), byte-identical to `system_prompt.txt:36-43` pre-Sprint-40 modulo newline → space flattening.
- **D-c:** DISCOVER phase guidance refinements (~720 chars; only NEW content from pre-Sprint-40 lines 45-52: `reasoning` string + 3-tier confidence guidance + explicit "?" clarification rule + escalate clauses). The DISCOVER lead-in ("Your job is to identify which use case...") was already in `procedure` from Sprint 38 — dropped from D-c per dedupe rule.

All other fields in `discover_triage.yaml` (name / description / applicable_phases / applicable_use_cases / tools_required / required_context_keys / max_tool_steps / allow_interim_message / valid_terminal_outcomes / objective / grounding_instruction / escalation_policy / guardrails / state_inheritance) are UNCHANGED.

### 4.2 D-d — system_prompt.txt edit (single replacement block)

The pre-Sprint-40 `system_prompt.txt:23-52` (Sprint 23 paragraph + Sprint 31 paragraph + Sprint 33 SLOT description + DISCOVER phase guidance bulk) was replaced by:

- **PRESERVED:** Sprint 23 `already_called` paragraph (post-Sprint-40 lines 23-28; cross-Skill envelope-mechanics teaching per design doc §7.2.1). The contract §6 #1 permits cosmetic re-wording to reference "Skill envelope" framing; dev judgment was NO re-wording (the paragraph's existing "every tool" phrasing already reads cross-Skill; surface in OQ-S40.4).
- **DELETED:** Sprint 31 paragraph (was lines 30-34).
- **DELETED:** Sprint 33 SLOT description (was lines 36-43).
- **DELETED:** DISCOVER phase guidance bulk (was lines 45-52).
- **ADDED (D-d):** new orchestration-shell paragraph teaching Skill envelope mechanics + the one-line DISCOVER pointer combined (post-Sprint-40 lines 30-31). The paragraph reads:

  > Skill envelope mechanics:
  > You operate within the Skill envelope the runtime selects per (phase, active_use_case). The envelope names the tool whitelist (you cannot dispatch a tool outside it), the recommended procedure (LLM-soft guidance, deviate when judgement warrants), the grounding instruction (LLM-soft), and the escalation policy (LLM-soft). Skill-declared guardrails enforce Runtime-floor invariants (e.g., a `record_outcome(class=resolve)` without a `source_id` citation will be rejected). Respect the envelope; exercise judgment on response strategy per the Constitution. Phase-specific guidance lives in the Skill `procedure`; refer to your current Skill envelope for the active phase's directives.

- **PRESERVED:** `request_handover` decision tree (post-Sprint-40 lines 33-80 = pre-Sprint-40 lines 54-101 shifted by −21; cross-Skill canonical enum per design doc §7.3 + contract §6 #2). Content byte-identical to pre-Sprint-40.

Result: `system_prompt.txt` post-Sprint-40 = 80 lines (was 101; −21 lines; ~21% reduction).

### 4.3 D-e — NEW SkillTeachingMigrationIntegrationTest (11 tests)

`server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` (264 lines; 11 tests). Java-deterministic prompt-composition assertions per design doc §7.8. Test surface (one test per assertion family):

1. `systemPrompt_postSprint40_lineCountIsMateriallyReduced` — < 90 lines (actual: 80).
2. `systemPrompt_postSprint40_preservesSprint23AlreadyCalledTeaching` — `already_called` header + principle line PRESENT in shell.
3. `systemPrompt_postSprint40_preservesRequestHandoverDecisionTree` — header + ACTIVE-UC TIEBREAKER block + INTAKE COMPLETION block + `service_degraded` tail enum PRESENT in shell.
4. `systemPrompt_postSprint40_addsEnvelopeMechanicsParagraph` — "Skill envelope" + "tool whitelist" + "Runtime-floor invariants" + DISCOVER pointer phrase PRESENT.
5. `systemPrompt_postSprint40_removesSprint31AlternateCandidateHeader` — Sprint 31 header line + trailing principle line ABSENT from shell.
6. `discoverSkill_postSprint40_carriesMigratedSprint31Teaching` — Sprint 31 header + principle line PRESENT in `procedure`.
7. `systemPrompt_postSprint40_removesSprint33DisambiguationSignalsSlotDescription` — Sprint 33 SLOT description header + sub-field description ABSENT from shell.
8. `discoverSkill_postSprint40_carriesMigratedSprint33SlotDescription` — Sprint 33 SLOT description header + sub-field PRESENT in `procedure` + Sprint 33 ad-status disambiguation CUE BODY (migrated by Sprint 38) STILL PRESENT alongside.
9. `systemPrompt_postSprint40_removesDiscoverPhaseGuidanceBulk` — DISCOVER phase guidance header + lead-in + escalate line ABSENT from shell.
10. `discoverSkill_postSprint40_carriesMigratedDiscoverPhaseGuidance` — "DISCOVER phase guidance refinements" + confidence guidance + `<` 0.5 clarification rule PRESENT in `procedure`.
11. `llmInputForDiscoverTurn_containsBothShellAndDiscoverSkillEnvelope` — composition observability: shell carries cross-Skill content; DISCOVER Skill envelope carries DISCOVER-specific content. Both reach the LLM via the Sprint 38 SkillRegistry-driven `composeSkillPhasePlan(...)` path.

Per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`: no mocked LLM used (the test reads classpath resources + invokes the production `SkillRegistry.select(...)` directly; no `LlmInvocationService` involved). The test is a Java-composition observability assertion, NOT an LLM-behaviour claim.

### 4.4 PhaseEvaluatorSkillIntegrationTest golden update (existing test, OQ-S40.1)

The Sprint 38 `PhaseEvaluatorSkillIntegrationTest.DISCOVER_SYSTEM_INSTRUCTION` (lines 82-119 pre-Sprint-40) was a byte-identical golden assertion against `discover_triage.yaml` `procedure` content. Sprint 40 extends `procedure`; the golden was extended to match (lines 82-180 post-Sprint-40; 13/13 tests PASS).

This is a content-equivalent update (the post-Sprint-40 golden carries the pre-Sprint-40 Sprint 38 base content + appended Sprint 31 + Sprint 33 SLOT description + DISCOVER guidance refinements content). The other 12 tests in the file (CONFIRM × 3, CLOSE × 3, ESCALATE × 3, resolve_intake_stillFollowsLegacyBranchUntilSprint39 × 3) are UNCHANGED.

### 4.5 D-f — Inherited SystemPromptUserRequestedTiebreakerTest outcome

Per contract §2.6: the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` MAY resolve organically if Sprint 40's structural change addresses the underlying drift.

**Outcome: PERSISTS unchanged.** The inherited test asserts `prompt.contains("Sprint 6")` (a literal anchor token) alongside `prompt.contains("ACTIVE-UC TIEBREAKER")`. The Sprint 40 dev commit does NOT touch the `ACTIVE-UC TIEBREAKER` block (pre-Sprint-40 lines 75-76 / post-Sprint-40 lines 54-55; per contract §6 #2, the `request_handover` decision tree at lines 54-101 STAYS). The "Sprint 6" anchor token has been absent from the live `system_prompt.txt` since Sprint 24-era working-tree mod (per Sprint 39 codex review §7 + this Sprint 40 verification). Sprint 40 close inherits the same baseline.

Surfaced as OQ-S40.3 for deliver-agent + human classification (informational; not a blocker per contract §10 stop condition #18 — "do NOT make it a hard gate at Sprint 40 close").

## 5. Sprint 37 freeze fidelity

Per design doc §7 (decision (f) — Sprint 23/31/33 teaching migration mapping):

| Freeze decision | Site of honor in Sprint 40 commit | Departures |
|---|---|---|
| §7.1 mapping table row 1 (Sprint 23 `already_called` → orchestration shell, STAYS) | `system_prompt.txt:23-28` UNCHANGED (post-Sprint-40 line numbers same as pre-Sprint-40 for lines 1-28) | None. Dev judgment chose NO cosmetic re-wording (existing "every tool" phrasing already reads cross-Skill); surfaced as OQ-S40.4 |
| §7.1 mapping table row 2 (Sprint 31 `alternate_candidate_use_cases` → `discover_triage.yaml` `procedure`) | `discover_triage.yaml:20` `procedure` extension; `system_prompt.txt:30-34` DELETED | None. Byte-for-byte content (modulo whitespace flattening) |
| §7.1 mapping table row 3 (Sprint 33 `discover_disambiguation_signals` → `discover_triage.yaml` `procedure`) | `discover_triage.yaml:20` `procedure` extension (alongside Sprint 33 CUE BODY migrated in Sprint 38 per §6.2.1); `system_prompt.txt:36-43` DELETED | None. Cue/slot split closed per design doc §7.2.3 |
| §7.2.1 (Sprint 23 stays in shell — cross-Skill universal) | `system_prompt.txt:23-28` UNCHANGED | None |
| §7.2.2 (Sprint 31 migrates to DISCOVER Skill; rely on slot's intrinsic self-documentation for resolve-side reads; Alternative C REJECTED) | `discover_triage.yaml:20` `procedure` carries the teaching; no copy in RESOLVE-FAQ or RESOLVE-INTAKE YAMLs (UNCHANGED) | None |
| §7.2.3 (Sprint 33 SLOT migrates to DISCOVER Skill alongside Sprint 33 cue body from Sprint 38) | `discover_triage.yaml:20` `procedure` carries BOTH the slot-reading teaching (D-b Sprint 40) AND the cue body (Sprint 38) | None |
| §7.3 orchestration-shell content rows: Bot identity (lines 1-9) | `system_prompt.txt:1-9` UNCHANGED | None |
| §7.3 row: Empty/non-empty tool_calls (lines 10-12) | `system_prompt.txt:10-12` UNCHANGED | None |
| §7.3 row: Universal rules (lines 14-21) | `system_prompt.txt:14-21` UNCHANGED | None |
| §7.3 row: `already_called` slot teaching | `system_prompt.txt:23-28` UNCHANGED | None |
| §7.3 row: NEW envelope-mechanics paragraph + DISCOVER pointer | `system_prompt.txt:30-31` ADDED (combined into single paragraph) | Minor: dev judgment combined the envelope-mechanics paragraph and the DISCOVER one-line pointer into a single paragraph block rather than separating them. Reason: shell brevity + single block reads more naturally. Surfaced as OQ-S40.2 for Codex / human review at sprint close |
| §7.3 row: DISCOVER phase guidance (bulk migrated; compressed one-line pointer remaining) | `system_prompt.txt:30-31` (combined with envelope mechanics — see above row) | Same minor deviation as above |
| §7.3 row: `request_handover` decision tree (lines 54-101 → post-Sprint-40 lines 33-80) | `system_prompt.txt:33-80` UNCHANGED content (line numbers shifted by −21 because preceding paragraphs deleted) | None |
| §7.4 rationale (Sprint 23 universally relevant; Sprint 31/33 phase-specific; shell shrinkage is reviewability benefit not token-cost benefit; decision tree stays cross-Skill) | Honored in design + implementation | None |
| §7.5 alternatives (A REJECTED — Alternative A migrate all teaching: would duplicate; B REJECTED — keep all in shell: defeats M2 goal; C REJECTED — migrate Sprint 31 cross-Skill into RESOLVE Skills: maintenance hazard; D REJECTED — `cross_skill_includes` field: complexity) | Honored: no cross-Skill copy; only `discover_triage.yaml` extended | None |
| §7.6 §1.7 boundary check (migrated teaching is principle-level; no per-UC-pair if-else) | Verified at §6 below; `discover_triage.yaml` `procedure` post-Sprint-40 carries no per-UC-pair branch table | None |
| §7.7 §1.3 / §1.4 boundary check (migrated teaching LLM-soft; no new Runtime enforcement; surface-relocation) | Verified at §6 below; no `record_outcome` / `request_handover` predicate change; no `PhaseEvaluator` / `AgentRunLoopImpl` / `SkillGuardrailDispatcher` edit | None |
| §7.8 downstream sub-sprint reference (Sprint 40 ships system_prompt.txt EDIT + discover_triage.yaml EDIT + behavioural-equivalence test) | All three artefacts shipped in this dev commit | None — design doc §7.8 references "Sprint 31 from system_prompt.txt lines 36-43" which is a TYPO in the freeze doc (should say "Sprint 33 from system_prompt.txt lines 36-43" — Sprint 31 was lines 30-34). Sprint 40 dev implements the CORRECT mapping (Sprint 33 from lines 36-43; Sprint 31 from lines 30-34). The freeze-doc typo is an editorial fold-back item per `doc_governance.md` cadence; deliver-agent + human handle at M2 close per the Sprint 38 OQ-S38.1 precedent. Surfaced as OQ-S40.5 |

All Sprint 37 freeze §7 decisions are honored. The two minor deviations (D-d placement combined with DISCOVER pointer; freeze-doc typo on Sprint 31/33 line numbers) are surfaced as OQs for Codex / human classification.

## 6. §4.1 anti-hardcode self-walk on the Sprint 40 diff

Walking all 9 `iteration_governance.md` §4.1 questions on the Sprint 40 dev diff (5 files: system_prompt.txt EDIT + discover_triage.yaml EDIT + SkillTeachingMigrationIntegrationTest.java NEW + PhaseEvaluatorSkillIntegrationTest.java EDIT + this handoff):

1. **Q1 — keyword / regex / if-else / enum / per-UC matrix for a semantic decision?** **PASS.** No new keyword / regex / if-else / enum / per-UC matrix added. The migrated Sprint 31 + Sprint 33 + DISCOVER phase guidance paragraphs are principle-level teaching content moved from monolithic shell to Skill envelope; the content is bit-for-bit equivalent (modulo whitespace flattening). The post-Sprint-40 `discover_triage.yaml` `procedure` carries no per-UC-pair branch table; the migrated content uses general principles ("When ... is populated ...", "When a later user turn surfaces evidence ...", "Confidence guidance: >= 0.7 ... >= 0.5 ... below 0.5 ..."). No `if UC == X then ...` logic anywhere. The new envelope-mechanics shell paragraph is also principle-level (cross-Skill envelope teaching).
2. **Q2 — Tier-0 justification?** **N/A.** Sprint 40 adds NO Tier-0 invariant. C2 (guardrail refusal non-overridability) + C3 (state-bus boundary enforcement) stay DEFER per Sprint 37/38/39 close pre-decisions. `docs/runtime_freeze_and_risk_policy.md` is UNCHANGED in this dev commit.
3. **Q3 — soft signal achievable instead of hard branch?** **N/A.** Sprint 40 is content-relocation; no new hard branch (in Java OR prompt) is introduced. The migrated teaching content stays LLM-soft (was LLM-soft in `system_prompt.txt`; stays LLM-soft in `discover_triage.yaml` `procedure`). The runtime continues to surface the `alternate_candidate_use_cases` + `discover_disambiguation_signals` projection slots; the LLM continues to own the read judgment.
4. **Q4 — visible-eval / trace phrasing / CaseSpec id encoded?** **PASS.** Inspected the post-Sprint-40 `discover_triage.yaml` `procedure` + `system_prompt.txt` envelope-mechanics paragraph + the new test file: no `alice_*`, no `cs_*`, no source session id (`3772e56b-*` etc.), no bad-case-suite text, no visible-eval phrase. The test file asserts substring matches on stable architectural phrases ("`already_called` projection slot", "Skill envelope", "Confidence guidance: >= 0.7"), NOT on CaseSpec-derived text.
5. **Q5 — semantic ownership shift LLM → Java?** **PASS.** §1.3 LLM ownership preserved. The migrated teaching is LLM-soft teaching content (was LLM-soft in shell; stays LLM-soft in Skill `procedure`); no semantic decision is shifted to Java. The runtime continues to surface projection slots; the LLM continues to own UC hypothesis, drift/topic-shift handling, escalation posture, response strategy, customer-facing wording. §1.4 Runtime ownership also preserved: tool schemas UNCHANGED; `PhasePlan.allowedTools` composition UNCHANGED; PII/safety floor UNCHANGED; grounding floor UNCHANGED (S1 from Sprint 39 stays — no new guardrail in Sprint 40); idempotency UNCHANGED; persistence UNCHANGED. No new Runtime-floor enforcement.
6. **Q6 — if-else block in prompt instead of principle-level guidance?** **PASS.** Inspected the post-Sprint-40 `discover_triage.yaml` `procedure`: every migrated paragraph reads at principle level ("When ... AND ... is true, ..." OR "When a later user turn surfaces evidence ..." OR "Confidence guidance: >= 0.7 means ..."). No `if active_use_case == "UC-A" then dispatch tool X else dispatch tool Y` table. The Sprint 37 OQ-7.11 reviewed AGREE WITH DEV that the `request_handover` decision tree at pre-Sprint-40 lines 54-101 is situation-to-enum (not per-UC-pair) — Sprint 40 preserves it unchanged at post-Sprint-40 lines 33-80.
7. **Q7 — tool schema / capability / PII / grounding floor preserved?** **PASS.** Tool schemas: UNCHANGED (no tool YAML or `*Tool.java` edits in Sprint 40). Capability/permission boundary: UNCHANGED (`PhasePlan.allowedTools` composition still pulls from `skill.toolsRequired()`; `discover_triage.yaml` `tools_required` UNCHANGED at `[search_knowledge, classify_use_case]`). PII/safety floor: UNCHANGED (`AgentRunLoopImpl` UNCHANGED; no new safety guard). Grounding floor: UNCHANGED (S1 `must_cite_source` predicate from Sprint 39 stays in `resolve_faq_grounded_answer.yaml`; Sprint 40 does NOT extend it OR add a new grounding gate; the migrated content is teaching, not enforcement).
8. **Q8 — generalization coverage (target / neighbor / negative / shadow)?** **PASS.** Target = 3 migrated paragraphs + 1 new envelope-mechanics paragraph + Sprint 23 + decision tree preserved; covered by 11 `SkillTeachingMigrationIntegrationTest` tests (positive AND negative substring assertions). Neighbor = the existing 13 `PhaseEvaluatorSkillIntegrationTest` cases (DISCOVER × 3 with updated golden + CONFIRM × 3 + CLOSE × 3 + ESCALATE × 3 + legacy-RESOLVE × 1) plus the 14 `PhaseEvaluatorResolveSkillIntegrationTest` cases from Sprint 39 + the Sprint 38 `SkillLoaderTest` (18) + `SkillRegistryTest` (11) + `SkillTest` (9) — all GREEN post-Sprint-40. Negative = `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED (M1 functional surface); other M1 / M2 functional tests preserved (`SkillGuardrailDispatcherTest` 24/24; `ResolveFaqGuardrailsTest` 11/11; `ResolveIntakeGuardrailsTest` 11/11). Shadow = N/A per M2 §5 recalibration (architecture-focused milestone).
9. **Q9 — rollback / sunset?** **N/A.** No temporary measure / kill-switch introduced. `git revert <sprint-40-commit>` restores `system_prompt.txt` + `discover_triage.yaml` exactly to pre-Sprint-40 (HEAD `2f412b6`) state. The Skill Registry abstraction itself stays (Sprint 38 / Sprint 39 surfaces).

**Verdict: `approve`.** Sprint 40 introduces no semantic hardcode; no Tier-0 candidate; no §1.3 / §1.4 / §1.7 boundary violation; full generalization coverage on the content-relocation surface; rollback is trivial.

## 7. Open questions for deliver-agent + human (handoff §7 OQ)

- **OQ-S40.1 — Existing test golden update included in dev commit.** Contract §5 lists 4 files in scope (system_prompt.txt + discover_triage.yaml + new test + handoff); Sprint 40 dev commit ALSO updates `PhaseEvaluatorSkillIntegrationTest.java` `DISCOVER_SYSTEM_INSTRUCTION` golden to match the extended `discover_triage.yaml` `procedure`. Without this update, the existing test fails (contract §6 #32 says "no regression on PhaseEvaluatorSkillIntegrationTest"). Sprint 39 precedent: dev updated 8 existing test files for the migration to be observable. Dev judgment per `feedback_commit_at_end_bundles_deliver_artefacts.md`: include the golden update in the Sprint 40 dev commit since it's a behavioural-equivalence update (the new golden carries the pre-Sprint-40 base content + appended migrated content; the assertion is still byte-for-byte against the YAML); other 12 tests in the file UNCHANGED. **Disposition recommendation: AGREE WITH DEV — INCLUDE in dev commit; classify as behavioural-equivalence update per Sprint 39 precedent.**
- **OQ-S40.2 — D-d envelope-mechanics paragraph + DISCOVER pointer placement / combination.** Contract §2.4 lists 3 placement options (after universal rules / after `already_called` / before DISCOVER pointer) and notes "Exact wording at dev judgement; the constraint is content-level not phrasing-level". Design doc §7.3 lists the envelope-mechanics paragraph and the DISCOVER one-line pointer as TWO separate rows in the orchestration-shell content table. Dev judgment combined them into a single shell paragraph block for brevity (single paragraph reads more naturally; the DISCOVER pointer is a one-liner that naturally fits as the closing sentence of the envelope-mechanics paragraph). Verified `SkillTeachingMigrationIntegrationTest.systemPrompt_postSprint40_addsEnvelopeMechanicsParagraph` asserts both phrases. **Disposition recommendation: AGREE WITH DEV unless Codex flags the combination as a §7.3 mapping deviation; if flagged, refactor to two separate paragraphs at sprint close.**
- **OQ-S40.3 — Inherited SystemPromptUserRequestedTiebreakerTest D-f outcome.** PERSISTS unchanged. The inherited test asserts `prompt.contains("Sprint 6")` literal anchor token alongside ACTIVE-UC TIEBREAKER block. The "Sprint 6" anchor has been absent from `system_prompt.txt` since Sprint 24-era working-tree mod (documented baseline). Sprint 40 does NOT touch the ACTIVE-UC TIEBREAKER block (post-Sprint-40 lines 54-55; per contract §6 #2 the decision tree stays). **Disposition recommendation: STATUS QUO — inherited baseline persists; not a Sprint 40 close blocker per contract §10 stop condition #18.** Resolving this test would require adding back the "Sprint 6" anchor (a Sprint 24+ cleanup or a deliberate annotation in the post-Sprint-40 shell); out of Sprint 40 scope.
- **OQ-S40.4 — Sprint 23 `already_called` paragraph cosmetic re-wording option.** Contract §6 #1 permits cosmetic re-wording of the Sprint 23 paragraph to reference "Skill envelope" framing instead of implicit phase/plan framing. Dev judgment: NO re-wording chosen. The existing Sprint 23 paragraph reads "This guidance applies to every tool — there is no tool-name or use-case branching" which already presents as cross-Skill envelope-mechanics teaching. Adding "Skill envelope" framing would be redundant; the new envelope-mechanics paragraph (D-d, immediately following) establishes the Skill envelope framing for the rest of the shell. **Disposition recommendation: AGREE WITH DEV unless Codex / human prefers the re-wording for consistency.**
- **OQ-S40.5 — Sprint 37 freeze doc §7.8 typo.** Design doc §7.8 says "the slot-reading teaching (Sprint 31 from system_prompt.txt lines 36-43)" — this is a typo; lines 36-43 carried the Sprint 33 SLOT description (not Sprint 31). Sprint 31 was lines 30-34. Sprint 40 dev implements the CORRECT mapping (Sprint 33 SLOT → from lines 36-43; Sprint 31 → from lines 30-34). The freeze doc itself is immutable per `doc_governance.md` (contract §6 #16). **Disposition recommendation: ROUTE to M2 close editorial fold-back per the Sprint 38 OQ-S38.1 precedent — deliver-agent + human at M2 close fix the typo in `docs/proposals/skill_registry_design.md` §7.8 as a SEPARATE governance commit. NO Sprint 40 fix iteration warranted.**

## 8. Anti-hardcode self-walk (cross-reference)

See §6 above for the full §4.1 nine-question kernel walk. Verdict: **`approve`**.

## 9. Files changed

| path | change | notes |
|---|---|---|
| `server/src/main/resources/prompts/system_prompt.txt` | EDIT | 101 → 80 lines (−21); Sprint 31 + Sprint 33 SLOT description + DISCOVER phase guidance bulk DELETED; envelope-mechanics paragraph + DISCOVER pointer ADDED; Sprint 23 `already_called` paragraph + `request_handover` decision tree PRESERVED unchanged |
| `server/src/main/resources/skills/discover_triage.yaml` | EDIT | 30 lines unchanged (single-line YAML quoted string); `procedure` content extended (~2900 → ~7900 chars) with Sprint 31 paragraph + Sprint 33 SLOT description + DISCOVER phase guidance refinements (light dedupe on lead-in) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/SkillTeachingMigrationIntegrationTest.java` | NEW | 264 lines; 11 Java-deterministic prompt-composition tests per design doc §7.8 |
| `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorSkillIntegrationTest.java` | EDIT | 341 → 400 lines (+59); `DISCOVER_SYSTEM_INSTRUCTION` golden updated to match extended `discover_triage.yaml` `procedure`; other 12 tests UNCHANGED; surfaced as OQ-S40.1 |
| `docs/sprints/sprint-040-handoff.md` | NEW | this file |

Total: 5 files changed/created in dev commit (3 EDIT main resources + 2 test files [1 NEW + 1 EDIT] + 1 NEW handoff). Per `feedback_commit_at_end_bundles_deliver_artefacts.md`, deliver-agent-owned files (`docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-040-*.md`) are NOT staged in this dev commit; deliver-agent + human bundle at Sprint 40 close.

## 10. Layer-classification self-walk (Sprint 40 §8 stanza)

**Target failure layer:** `prompt_projection` + `semantic_planner` per §1.4 (LLM input contract change — system_prompt.txt structure shifts from monolithic prompt to Skill envelope projection for DISCOVER-specific teaching) + (LLM continues to own §1.3 surface — the migration is surface-relocation, NOT LLM-ownership shift). Verified: the LLM observes the same teaching content post-Sprint-40 (Sprint 31 + Sprint 33 SLOT description + DISCOVER phase guidance + universal norms + envelope mechanics + decision tree), but partitioned by Skill envelope projection vs orchestration shell vs canonical enum block. No LLM-owned decision is shifted to Java.

**Tier-0 invariant:** NO new Tier-0 invariant. Sprint 37 freeze §12 surfaced 5 candidates (C1 REJECTED — existing invariant; C2 + C3 QUALIFIED-DEFER — R-items in action_bank §5.2 for M3+ revisit; C4 + C5 NOT-A-CANDIDATE). Sprint 40 honors the DEFER pre-decision per `feedback_constitution_discipline_vs_planning_anticipation.md` — C2 (Sprint 39 dispatcher landing shipped, production trace evidence still pending) and C3 (Sprint 41 enforcement surface, not yet shipped) both stay open. Sprint 40 is content-relocation only and does NOT generate a NEW Tier-0 candidate.

**Semantic hardcode:** None introduced. The migrated Sprint 31 + Sprint 33 + DISCOVER phase guidance paragraphs are principle-level teaching content moved from monolithic `system_prompt.txt` to `discover_triage.yaml` `procedure`; the content is bit-for-bit equivalent (modulo whitespace flattening). The post-Sprint-40 `system_prompt.txt` is a thin orchestration shell carrying only cross-Skill universal content per design doc §7.3 mapping; no per-UC-branch logic added; no LLM-owned decision shifted to Java. The new envelope-mechanics paragraph (D-d) is principle-level cross-Skill teaching about how to read the envelope; no `if Skill.name == "..." then ...` logic.

**Generalization coverage:** target = the 3 migrated teaching content + the kept cross-Skill content + the new envelope-mechanics paragraph; `SkillTeachingMigrationIntegrationTest` 11 tests cover post-migration composition observability (positive PRESERVED + negative REMOVED + positive ADDED assertions). Neighbor = the 4 simpler phase Skills (Sprint 38; `PhaseEvaluatorSkillIntegrationTest` 13/13 GREEN post-Sprint-40 with updated DISCOVER golden) + 2 RESOLVE Skills (Sprint 39; `PhaseEvaluatorResolveSkillIntegrationTest` 14/14 GREEN); Sprint 33 ad-status disambiguation CUE BODY already in DISCOVER Skill from Sprint 38 — STAYS in Sprint 40 (Sprint 40 SLOT description landing closes the cue/slot split). Negative = `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED (M1 functional surface); `SkillGuardrailDispatcherTest` 24/24 + `ResolveFaqGuardrailsTest` 11/11 + `ResolveIntakeGuardrailsTest` 11/11 PRESERVED (Sprint 39 functional surface). Shadow = N/A per M2 §5 recalibration (architecture-focused milestone).

## 11. §5 Eval Acceptance bars (adapted for content-relocation sub-sprint + M2 §5 recalibration)

**Hard gates (must pass for Sprint 40 close):**

- ✓ `system_prompt.txt` post-Sprint-40: Sprint 23 paragraph PRESERVED in shell (assertion #2 of new test) + `request_handover` decision tree at post-Sprint-40 lines 33-80 PRESERVED (assertion #3) + Sprint 31 paragraph DELETED (assertion #5) + Sprint 33 SLOT description DELETED (assertion #7) + DISCOVER phase guidance bulk REPLACED with the envelope-mechanics paragraph's one-line shell pointer (assertion #9 + #4) + envelope-mechanics paragraph ADDED (assertion #4). Line count 80 < 90 conservative gate (assertion #1).
- ✓ `discover_triage.yaml` `procedure` post-Sprint-40 contains migrated Sprint 31 content (assertion #6) + Sprint 33 SLOT description (assertion #8) + DISCOVER phase guidance refinements (assertion #10); alongside Sprint 33 ad-status disambiguation CUE BODY from Sprint 38 (assertion #8); dedupe applied where lead-in already there from Sprint 38.
- ✓ `SkillTeachingMigrationIntegrationTest` 11 tests PASS verifying post-Sprint-40 composition observability.
- ✓ Java baseline preserved: **1105 / 1 inherited / 0 / 2** (= 1094 post-Sprint-39 + 11 new Sprint 40 tests). Sole failure is the documented inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` per OQ-S40.3.
- ✓ `Sprint71PartialIntakePersistenceTest` 14/14 PRESERVED (M1 functional-surface protection).
- ✓ Sprint 38 + Sprint 39 tests PRESERVED (`SkillTest` 9/9 + `SkillRegistryTest` 11/11 + `SkillLoaderTest` 18/18 + `PhaseEvaluatorSkillIntegrationTest` 13/13 with DISCOVER golden updated per §4.4 + `PhaseEvaluatorResolveSkillIntegrationTest` 14/14 + `SkillGuardrailDispatcherTest` 24/24 + `ResolveFaqGuardrailsTest` 11/11 + `ResolveIntakeGuardrailsTest` 11/11). All 136 of these tests re-run together and PASS.
- ✓ No per-UC-branch if-else introduced in `discover_triage.yaml` post-Sprint-40 (verified §6 Q1 + Q6).
- ✓ No scope creep into Sprint 41 surfaces (`SkillStateBus.java` NOT created; `ContextProjectionBuilder.java` UNCHANGED; `prior_use_case_carry` projection slot impl NOT added — slot is declared in Sprint 39 RESOLVE Skill YAML `state_inheritance.soft_signal_via_projection` only for forward-compat).
- ✓ No edit to ANY Java code beyond `SkillTeachingMigrationIntegrationTest.java` NEW + `PhaseEvaluatorSkillIntegrationTest.java` golden update (OQ-S40.1).
- ✓ No edit to other Skill YAMLs (`confirm.yaml` / `escalate.yaml` / `terminal.yaml` / `resolve_faq_grounded_answer.yaml` / `resolve_intake_collect_and_handover.yaml` UNCHANGED).
- ✓ Constitution-compliance verified: §1.3 LLM ownership preserved; §1.4 Runtime ownership preserved; §1.7 forbidden-list honored (no per-UC matrix; no eval phrase encoding).
- ✓ Handoff §6 walks §4.1 anti-hardcode kernel — verdict `approve`.
- ✓ Reproducibility: every quantitative or code-citation claim in this handoff cites source path + line number per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

**Codex review (per-sub-sprint at Sprint 40 close):** deliver-agent dispatches per `iteration_governance.md` §4.3 trigger #3 (system_prompt.txt structural change — LLM input contract surface). Expected verdict `approve` per the self-walk.

**Observations (per M2 §5 recalibration; not gates):**

- Interactive eval smoke composite_score / pass_rate / judge dims — NOT measured in this dev session per M2 §5 recalibration + contract §10 stop condition #16; deliver-agent + human MAY record at Sprint 40 close for tracking per §5.5 demotion; MAY regress; does NOT block.
- Bad-case suite (Alice) closure-criterion (a) result + fabrication-condition trigger count — NOT measured in this dev session per M2 §5 recalibration; MAY STAY FLAT (Sprint 40 is content-relocation; the LLM observes the same teaching content via Skill envelope projection); does NOT block.
- Architecture-health metrics direction (§6 governance):
  - `new_semantic_hardcode_count` = 0 (Sprint 40 introduces NO new per-UC-branch if-else).
  - `soft_signal_conversion_count` += 3 (Sprint 31 paragraph + Sprint 33 SLOT description + DISCOVER phase guidance bulk migrated from monolithic prompt to Skill envelope); cumulative M2 post-Sprint-40 = Sprint 38's 4 phase Skills + Sprint 39's 2 RESOLVE Skills + 3 predicate migrations + 1 NEW S1 + 1 NEW S2 + Sprint 40's 3 teaching paragraphs = 14 architectural conversions.
  - `planner_ownership_ratio` unchanged (LLM ownership preserved; no decision shifted to Java).
  - `shadow_disagreement_rate` not measured.
- `system_prompt.txt` line-count: 101 → 80 (−21 / ~21% reduction). Below the contract §9 expected range of 60-75 (Sprint 40 dev kept content equivalence over aggressive compression; the envelope-mechanics paragraph + DISCOVER one-line pointer combined is shorter than separating them; the existing 7-line Sprint 23 paragraph + 47-line `request_handover` decision tree are byte-identical to pre-Sprint-40).

## 12. Closure verdict

**Closure verdict placeholder.** Per `feedback_handoff_verdict_section_delegation.md`: this section is left for deliver-agent + human + Codex per-sub-sprint review at Sprint 40 close. Sprint 40 closes at sub-sprint close per `iteration_governance.md` §4.3 per-sub-sprint Codex trigger #3.
