# Sprint 39 dev prompt — RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + NEW S1 + NEW S2 + unified Skill terminal-predicate dispatcher (NEW M2 sub-sprint 3)

**Authored:** 2026-05-17 by deliver-agent (post-Sprint-38-fix-close housekeeping; pre-Sprint-39 dev launch)
**For:** the Claude Code dev-agent session that consumes this prompt to execute Sprint 39
**Branch:** `refactor/remove-the-shackles`
**Repo:** `/Users/caoruixin/projects/csagent-latest`
**HEAD (parent commit):** `5787806` (Sprint 38 fix iteration #1; B-fix-iterated close 2026-05-17)
**Sub-sprint contract:** `docs/sprint_objective.md` (active Sprint 39 contract; supersedes archived `docs/sprints/sprint-038-objective.md`)

---

## 1. Mission briefing (read first)

You are Claude Code, the dev-agent for Sprint 39 — the **third sub-sprint of NEW Milestone M2** (Skill Registry Abstraction + Wholesale Retroactive Externalization). Sprint 39 is the **biggest M2 sub-sprint by scope** — multi-fence convergence at one sub-sprint per human direction (no silent scope split into Sprint 40+).

**What Sprint 38 shipped** (preceding sub-sprint, closed B-fix-iterated 2026-05-17 across commits `bd9d3f5` + `5787806`):

- 5 new Skill Java classes under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` — `Skill` record + `Guardrail` record + `StateInheritance` record + `SkillRegistry` Spring `@Component` + `SkillLoader` Spring `@Component`.
- 4 new Skill YAMLs at `server/src/main/resources/skills/` — `discover_triage.yaml`, `confirm.yaml`, `escalate.yaml`, `terminal.yaml` (the 4 SIMPLER phase Skills; all carry `guardrails: []` empty per Sprint 38 contract; legacy-verbatim content from `PhaseEvaluator.java:397-542` preserved).
- `PhaseEvaluator.java` integration: constructor 10→11 args (added `SkillRegistry`); new `composeSkillPhasePlan(...)` private helper at line 548; SkillRegistry-first dispatch at lines 403-407 in `plan(...)`; 4 legacy phase branches (DISCOVER 397-457 + CONFIRM 462-487 + CLOSE 492-509 + ESCALATE 513-542) DELETED; RESOLVE-INTAKE 552-596 + RESOLVE-FAQ 598-665 branches PRESERVED for Sprint 39 (now at lines 417-460 + 463-530 post-Sprint-38 — re-verify at session start).
- 4 new test files (45 new tests; all pass): `SkillTest.java` 9 tests + `SkillRegistryTest.java` 11 + `SkillLoaderTest.java` 18 + `PhaseEvaluatorSkillIntegrationTest.java` 13.
- `SkillLoader` fail-fast schema validation per design doc §2.2 — including the Sprint 38-fix-iteration additions: `VALID_TOOL_NAMES` allowlist (11 canonical tool names mirrored from Java `Tool.getName()` implementations), explicit `applicable_use_cases` UC registry check via `UseCaseRegistryService.isKnownUseCase(...)`, `VALID_GUARDRAIL_TYPES` allowlist (4 canonical predicate types per Sprint 37 freeze §5.2 + §8.2: `faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome_guard`, `must_cite_source`).
- Behavioural equivalence preserved on the 4 migrated phases (golden strings pinned inline; 13 tests in `PhaseEvaluatorSkillIntegrationTest`).
- Java baseline: 1034 / 1-inherited / 0 / 2 post-Sprint-38-fix-close.

**What Sprint 39 ships** (this dev session — see contract §2 for canonical scope):

1. **2 RESOLVE Skill YAMLs** at `server/src/main/resources/skills/` — `resolve_faq_grounded_answer.yaml` (UC-A/B/C/D/E/F/FP; 3 guardrails) + `resolve_intake_collect_and_handover.yaml` (UC-G/H/I/J/K; 1 guardrail; template substitution placeholders).
2. **1 NEW Java class** `SkillGuardrailDispatcher` (+ optionally 2 supporting records `RejectVerdict` + `DispatchContext` as separate files or inner records) implementing 4 typed predicate types: `faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome_guard`, `must_cite_source`.
3. **PhaseEvaluator integration extension**: `composeSkillPhasePlan(...)` extended for template substitution (`{uc_name}`, `{uc_id}`, `{team_name}`, `{intake_complete_trigger}`, `{case_creation_note}`); legacy RESOLVE-INTAKE + RESOLVE-FAQ branches DELETED.
4. **AgentRunLoopImpl migration**: 3 dispatch sites (lines 317, 356, 413) routed through dispatcher; 3 static predicate methods (`shouldRejectFaqMissHandover` 690-734; `shouldRejectIncompleteIntakeHandover` 628-651; `shouldRejectPrematureResolveOutcome` 745-759) REMOVED.
5. **NEW S1 `must_cite_source` predicate** declared in `resolve_faq_grounded_answer.yaml` `guardrails` + implemented in dispatcher, bounded per M2 §6 #4 verbatim authorization (phase=RESOLVE_FAQ, tool=record_outcome with class=resolve, check=source_id citation presence). **NO expansion beyond authorization scope.**
6. **NEW S2 `intake_complete_required` predicate** declared in `resolve_intake_collect_and_handover.yaml` `guardrails` + implemented in dispatcher with the existing Sprint 7 §I2 semantics moved (NOT new semantics; same `IntakeFieldsRegistry`-driven required-fields check).
7. **4 new test files** (~60-100 tests): `PhaseEvaluatorResolveSkillIntegrationTest`, `SkillGuardrailDispatcherTest`, `ResolveFaqGuardrailsTest`, `ResolveIntakeGuardrailsTest`.
8. **`docs/sprints/sprint-039-handoff.md`** (NEW; 12-section dev-authored archive per Sprint 31-38 shape).

**Post-Sprint-39 state**: `PhaseEvaluator.java` carries ZERO hardcoded `systemInstruction` / `groundingInstruction` / `escalationPolicy` for any of the 6 phases; `AgentRunLoopImpl.java` carries ZERO `shouldRejectXxx` Java methods; all 6 phase Skills are externalized YAML; the SkillRegistry abstraction fully covers the runtime per-turn dispatch surface.

Your job:

1. Re-verify §4 premises in the contract by reading the cited files at session start.
2. Read the Sprint 37 freeze design doc sections (§5.2 + §6.2.5 + §6.2.6 + §6.4 + §8 + §9) as the architectural source-of-truth — these sections were Codex-approved at Sprint 37 close + Sprint 38 didn't touch them.
3. Execute the 6 execution phases (§4 below) with stop checks at each phase exit.
4. Write `docs/sprints/sprint-039-handoff.md` covering all 12 sections.
5. Run full `mvn test -q` from clean state at completion; preserve baseline 1034 / 1-inherited / 0 / 2 → ~1094-1134 / 1-inherited / 0 / 2.
6. Commit the single dev commit per §10 below.

You do NOT stage deliver-agent files (`docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/10-handoff.md`, `docs/action_bank.md`, `docs/codex-findings.md`, `compact/sprint-039-*.md`). You do NOT draft Sprint 40 / 41 contracts. You do NOT fill the handoff §12 closure verdict (that's deliver-agent + human + Codex at Sprint 39 close).

---

## 2. Read order (cold start)

Load these in order — governance first, then active contract, then design source-of-truth, then Sprint 38 archives for context, then verify current code shape.

### 2.1 Governance chain (auto-loaded via `AGENTS.md` transitive include)

- `AGENTS.md` — repo constitution.
- `docs/current/doc_governance.md` — tier model + decision rules.
- `docs/current/agent_context_guide.md` — per-task reading lists; the "Runtime, phase machine, drift" + "Tool schema, tool policy" reading lists are most relevant.
- `docs/current/iteration_governance.md` — Constitution §1 (LLM-owned §1.3 / Runtime-owned §1.4 / forbidden §1.7) + Failure Brief §2 + Fix Layer §3 + **§4.1 anti-hardcode kernel** (you'll self-walk this on the Sprint 39 diff at handoff §6) + §4.3 per-sub-sprint Codex triggers + §5/§5.5/§5.6 acceptance bars (with M2 §5 recalibration) + §7 stanza shape + §8 milestone framework.

### 2.2 Active sub-sprint contract + milestone north star

- `docs/sprint_objective.md` — Sprint 39 contract (this is the binding scope; read end-to-end). §1 layer breakdown; §2 goal with 6 execution phases (§2.1-§2.7); §3 non-goals; §4 15-premise check; §5 files in scope; §6 37 hard fences; §7 bundle policy; §8 §7 stanza; §9 success metrics with HARD GATES + observations; §10 28 stop conditions; §11 handoff §11 12-section contract; §12 M2 milestone context.
- `docs/milestone_objective.md` — NEW M2 milestone objective. Particularly §3 Sprint 39 row + §5 acceptance recalibration + **§6 #4 verbatim S1 authorization** (load-bearing for Sprint 39) + §6 22 milestone-level hard fences.

### 2.3 Sprint 37 freeze design doc (architectural source-of-truth)

`docs/proposals/skill_registry_design.md` — `status: proposal` (immutable per `doc_governance.md`). 3633 lines / 13 sections. **Read these sections for Sprint 39 — they are LOAD-BEARING for execution phases 1-5:**

- **§5.2 Guardrail declaration schema (declarative DSL)** — the typed object schema with `type` / `on_fail` / `parameters` fields; canonical predicate types Sprint 39 ships.
- **§6.2.5 RESOLVE-INTAKE → `skills/resolve_intake_collect_and_handover.yaml`** — verbatim YAML template; current source mapping at `PhaseEvaluator.java:552-596` (Sprint 37 era; **re-verify line range at HEAD `5787806`** — should be ~410-461 post-Sprint-38); template substitution placeholders `{uc_name}` / `{uc_id}` / `{team_name}` / `{intake_complete_trigger}` / `{case_creation_note}` + their substitution sources (registry / map lookups).
- **§6.2.6 RESOLVE-FAQ → `skills/resolve_faq_grounded_answer.yaml`** — verbatim YAML template; current source mapping at `PhaseEvaluator.java:598-665` (Sprint 37 era; **re-verify at ~463-530 post-Sprint-38**); 3 guardrails entries in `guardrails[]` array.
- **§6.4 Behavioural equivalence test pattern** — the test contract Sprint 39 honors per design doc decision (c) §4.3.
- **§8 Decision (g) — retroactive migration mapping for Sprint 6/7/11 predicates** — §8.1 mapping table; §8.2.1-§8.2.3 per-predicate detail (migration of `shouldRejectFaqMissHandover` / `shouldRejectIncompleteIntakeHandover` / `shouldRejectPrematureResolveOutcome`); **§8.2.4 NEW S1 `must_cite_source` on `resolve_faq_grounded_answer.yaml`** (LOAD-BEARING — quotes the M2 §6 #4 verbatim authorization + names the 3 boundary conditions a/b/c); §8.2.5 S2 intake-completeness; §8.5 §1.7 boundary check; §8.6 §1.3 / §1.4 boundary check.
- **§9 Decision (h) — unified Skill terminal-predicate dispatcher** — §9.1 public surface (`checkBeforeDispatch` + `checkBeforeOutcomePersist`; `RejectVerdict` + `DispatchContext` records); §9.2 short-circuit-on-first-reject semantics; §9.3 per-tool-call dispatch site routing (the table mapping 4 guardrail types to dispatch sites); §9.4 failure-mode trace + LLM-visible message shape; §9.7 §1.7 boundary check; §9.8 §1.3 / §1.4 boundary check; §9.9 Tier-0 candidate question (guardrail refusal non-overridability) — DEFER preserved through Sprint 39 close.

Other sections (§1-§4 + §6.2.1-§6.2.4 + §7 + §10 + §11 + §12 + §13) are CONTEXTUAL ONLY for Sprint 39 dev — you may skim, but do not need to study end-to-end.

### 2.4 Sprint 38 archives (context)

- `docs/sprints/sprint-038-objective.md` — the just-archived Sprint 38 contract; useful for confirming what Sprint 38 was scoped to ship vs what Sprint 39 picks up.
- `docs/sprints/sprint-038-handoff.md` — Sprint 38 main-dev archive (commit `bd9d3f5`). Read §1.2 (Sprint 38's code-shape table at HEAD `51c327c`) and §4 (implementation walkthrough). The 4 simpler phase Skill YAML templates Sprint 38 shipped are the shape pattern for Sprint 39's 2 RESOLVE YAMLs.
- `docs/sprints/sprint-038-fix-handoff.md` — Sprint 38 fix iteration #1 archive (commit `5787806`). Read §4.2 (SkillLoader extensions — `VALID_TOOL_NAMES` allowlist + `VALID_GUARDRAIL_TYPES` allowlist + `UseCaseRegistryService` injection); this is the schema-validation foundation Sprint 39 builds on (Sprint 39's 2 new RESOLVE YAMLs + their guardrail declarations will validate against the Sprint 38-fix-shipped allowlists at boot).
- `docs/sprints/sprint-038-codex-review.md` — combined two-round Codex archive. Read the Round 2 §3 "Sub-gap Closure Verification" and §6 "OQ Independent Verification" — confirms the SkillLoader schema-validation foundation Sprint 39 builds on is sound.

### 2.5 Feedback memory (deliver-agent doctrine)

- `.claude/agent-memory/sprint-deliver-orchestrator/feedback_constitution_discipline_vs_planning_anticipation.md` — why Sprint 39 dev does NOT pre-elevate C2 (guardrail refusal non-overridability) to Tier-0; Sprint 39 ships the dispatcher (FIRST observed evidence surface for C2); re-evaluation at Sprint 39 close OR M2 close per the feedback rule. Do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 39 dev commit.
- `.claude/agent-memory/sprint-deliver-orchestrator/feedback_commit_at_end_bundles_deliver_artefacts.md` — bundle policy. Dev does NOT stage deliver-agent-owned files.
- `.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every quantitative or code-citation claim cites source path + line number + extraction recipe.
- `.claude/agent-memory/sprint-deliver-orchestrator/feedback_mocked_llm_cannot_prove_prompt_causal_change.md` — Sprint 39 dispatcher Java logic tests + behavioural-equivalence integration tests MAY mock LLM (those are deterministic Java logic, not LLM behaviour); no real-LLM eval rerun is needed for Sprint 39 (architectural refactoring + bounded predicate landing).
- `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md` — handoff §12 is a placeholder; dev does NOT fill the closure verdict.

### 2.6 Code shape re-verification at session start

Run these reads + greps (one per cited file) to confirm the §4 premise table in the contract is still accurate at session start. If any line numbers drift, STOP and surface in handoff §3.

```bash
# PhaseEvaluator.java
wc -l server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java
# Expected: 1529 lines
grep -n "INTAKE_UCS\|escalation_reason\|UC_TEAM_NAME\|intakeCompleteTrigger\|buildIntakeSystemInstruction\|skillRegistry\|composeSkillPhasePlan\|RESOLVE\|case DISCOVER" server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java | head -40

# AgentRunLoopImpl.java
wc -l server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java
# Expected: 787 lines
grep -n "shouldRejectFaqMissHandover\|shouldRejectIncompleteIntakeHandover\|shouldRejectPrematureResolveOutcome\|INTAKE_COMPLETE_GUARD_REJECT_REASON" server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java

# IntakeFieldsRegistry.java — verify intakeComplete(uc, collected) helper exists (exact method name)
wc -l server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java
# Expected: 222 lines
grep -n "intakeComplete\|requiredFieldsFor\|public " server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java | head -20

# UseCaseRegistryService.java — confirm getUseCase + isKnownUseCase + UseCaseDefinition record
wc -l server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java
# Expected: 174 lines
grep -n "public " server/src/main/java/com/gumtree/csagent/service/runtime/UseCaseRegistryService.java

# ResolveDispositionEvaluator.java — confirm shouldRejectPrematureResolveOutcome static method (Sprint 11 frozen surface)
wc -l server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java
grep -n "shouldRejectPrematureResolveOutcome\|public " server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java

# RecordOutcomeTool.java — confirm normalizeOutcomeClass at lines 110-121
grep -n "normalizeOutcomeClass\|public " server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java

# Sprint 38-shipped Skill package
ls -la server/src/main/java/com/gumtree/csagent/service/runtime/skill/
# Expected: Skill.java, Guardrail.java, StateInheritance.java, SkillRegistry.java, SkillLoader.java

# Sprint 38-shipped YAMLs
ls -la server/src/main/resources/skills/
# Expected: discover_triage.yaml, confirm.yaml, escalate.yaml, terminal.yaml

# Sprint 38-fix VALID_GUARDRAIL_TYPES + VALID_TOOL_NAMES
grep -n "VALID_TOOL_NAMES\|VALID_GUARDRAIL_TYPES\|VALID_PHASES" server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java

# Baseline Java test count (BEFORE you write Sprint 39 tests)
cd server && mvn test -q 2>&1 | tail -20
# Expected: Tests run: 1034, Failures: 1 (SystemPromptUserRequestedTiebreakerTest inherited), Errors: 0, Skipped: 2
```

Spot-check the contract §4 premise table against the grep output. If any line range has drifted (e.g., RESOLVE-INTAKE branch is at lines 417-460 in your grep, contract says ~410-461 → that's within the "~" tolerance; but if it's at lines 500-540, premise drift is real and you STOP).

---

## 3. Architectural sources (cross-references; do NOT re-derive)

The Sprint 37 freeze locked all architectural decisions for Sprint 39. You implement; you do NOT re-design. Where the freeze says "Sprint 39 dev judgement", that's the bounded judgement scope.

**Frozen at Sprint 37 (immutable):**

- Skill data model — `Skill.java` (already shipped Sprint 38; do NOT modify the record fields).
- SkillRegistry shape — `SkillRegistry.java` (already shipped Sprint 38; do NOT modify the public API).
- PhaseEvaluator integration shape — `composeSkillPhasePlan(...)` signature (already shipped Sprint 38; Sprint 39 EXTENDS the body for template substitution, NOT the signature).
- 2 RESOLVE Skill YAML templates (design doc §6.2.5 + §6.2.6) — Sprint 39 ships verbatim modulo legacy-text-verbatim adaptation per OQ-S38.1 precedent (Codex-approved at Sprint 38; **honor LEGACY text from the current `PhaseEvaluator.java` RESOLVE branches; the freeze template's editorial paraphrasing is an editorial fold-back item per `doc_governance.md` code-ahead-of-docs**; deliver-agent + human handle editorial fold-back at M2 close, NOT Sprint 39 dev).
- 4 typed predicate types (design doc §5.2 + §8.2) — `faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome_guard`, `must_cite_source`. Sprint 38-fix `VALID_GUARDRAIL_TYPES` allowlist locks these names verbatim.
- Dispatcher public surface (design doc §9.1) — `checkBeforeDispatch(...)` + `checkBeforeOutcomePersist(...)`; `RejectVerdict` + `DispatchContext` record shapes.
- Short-circuit-on-first-reject (design doc §9.2) — Sprint 39 implements; no all-must-pass alternative.
- Per-tool-call dispatch site routing (design doc §9.3) — 4 guardrails × 2 dispatch sites table; you implement; no alternative routing.
- M2 §6 #4 verbatim S1 authorization scope (3-boundary check: a/b/c) — IMMUTABLE.

**Dev judgement (sprint-internal):**

- Placement of `RejectVerdict` + `DispatchContext` — separate files at `server/src/main/java/com/gumtree/csagent/service/runtime/skill/` OR inner records on `SkillGuardrailDispatcher`. Either is acceptable.
- Placement of `INTAKE_COMPLETE_GUARD_REJECT_REASON` constant — preserved at `AgentRunLoopImpl.java:120` OR moved to `SkillGuardrailDispatcher` as a public constant. Either is acceptable.
- Exact method names on dispatcher handlers (`handleFaqMissHandoverRequiresResolveAttempt`, `handleMustCiteSource`, etc.) — your choice; standard Java naming applies.
- Whether `buildIntakeSystemInstruction(...)` at `PhaseEvaluator.java:207-208` is inlined into `composeSkillPhasePlan` OR preserved as a private helper called from `composeSkillPhasePlan`. Either is acceptable. **Do NOT delete it.**
- Glob-pattern implementation for `intake_complete_required.parameters.escalation_reason_pattern: "intake_complete_for_uc_*"` — use the simplest reliable implementation (e.g., `startsWith("intake_complete_for_uc_")` is equivalent to the wildcard in this case; the wildcard token in the YAML parameter is the declarative form). Match what the existing Sprint 7 §I2 predicate does for backward-compatibility.
- Exact `source_id` token parsing in the S1 must_cite_source handler — your choice on the simplest reliable check that mirrors how the Sprint 6 §G2 grounding-instruction teaches the LLM to cite (search for `"source_id"` substring; OR `source_id:` prefix; OR a structured citation marker if the prompt teaches one). Surface as handoff §7 OQ if any ambiguity.
- Whether to delete `INTAKE_UCS` and similar sets after migration — **NO. Preserve them verbatim. They become the registry-driven source-of-truth referenced by `applicable_use_cases` on the RESOLVE YAMLs; consumption surfaces may not be fully audited; M3+ deletion if at all.**

---

## 4. 6-execution-phase walkthrough

The 6 execution phases below are DEV-INTERNAL execution structure (NOT separate Codex review surfaces — Codex reviews the whole sub-sprint at close per `iteration_governance.md` §4.3 trigger #3). Each phase has its own stop checks; you surface stop conditions if any phase cannot complete cleanly.

You may execute phases sequentially OR interleave (e.g., write the dispatcher before the YAMLs to validate the YAML loading paths). The phases below describe LOGICAL groupings, not strict ordering.

### 4.1 Execution phase 1 — RESOLVE Skill YAMLs

**Goal**: ship `resolve_faq_grounded_answer.yaml` + `resolve_intake_collect_and_handover.yaml` at `server/src/main/resources/skills/` per design doc §6.2.5 + §6.2.6, with content adapted from the current `PhaseEvaluator.java` legacy RESOLVE branches.

**Inputs**:

- Current RESOLVE-INTAKE branch in `PhaseEvaluator.java` at lines ~417-460 (re-verify at session start).
- Current RESOLVE-FAQ branch in `PhaseEvaluator.java` at lines ~463-530 (re-verify).
- Design doc §6.2.5 (RESOLVE-INTAKE template) + §6.2.6 (RESOLVE-FAQ template).

**Outputs**:

- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`:
  - `name: resolve_faq_grounded_answer`
  - `description`: short one-line description.
  - `applicable_phases: [RESOLVE]` (string from the runtime phase name).
  - `applicable_use_cases: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]` (explicit FAQ-path UCs).
  - `tools_required: [get_customer_context, search_knowledge, resolve_article, record_outcome, request_handover]`.
  - `required_context_keys: [form_context, customer_context, listing_context, moderation_context]`.
  - `max_tool_steps: 4`.
  - `allow_interim_message: false`.
  - `valid_terminal_outcomes: [FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE]`.
  - `objective`: legacy text from `PhaseEvaluator.java` RESOLVE-FAQ branch `objective` line (~486-487; "Determine the customer's issue and provide a grounded, helpful answer for " + `ucDef.name()` → translates to `"Determine the customer's issue and provide a grounded, helpful answer for {uc_name}."`).
  - `procedure`: legacy text from RESOLVE-FAQ branch `systemInstruction` lines ~498-506 (the "FAQ-path RESOLVE flow (S1)" passage).
  - `grounding_instruction`: legacy text from RESOLVE-FAQ branch `groundingInstruction` lines ~507-518.
  - `escalation_policy`: legacy text from RESOLVE-FAQ branch `escalationPolicy` lines ~519-529.
  - `guardrails`: array with 3 entries (in declaration order — matters for short-circuit semantics per design doc §9.2):
    ```yaml
    guardrails:
      - type: faq_miss_handover_requires_resolve_attempt
        on_fail: reject_with_hint
        parameters: {}
      - type: premature_resolve_outcome_guard
        on_fail: reject_with_hint
        parameters: {}
      - type: must_cite_source
        on_fail: reject_with_hint
        parameters:
          outcome_class: resolve
          cite_token_field: source_id
    ```
  - `state_inheritance`:
    ```yaml
    state_inheritance:
      inherit: [customer_context, accumulated_tool_results]
      reset: []
      soft_signal_via_projection: [prior_use_case_carry]
    ```

- `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml`:
  - `name: resolve_intake_collect_and_handover`
  - `description`: short one-line description.
  - `applicable_phases: [RESOLVE]`.
  - `applicable_use_cases: [UC-G, UC-H, UC-I, UC-J, UC-K]` (explicit INTAKE-path UCs; must match `INTAKE_UCS` set at `PhaseEvaluator.java:30`).
  - `tools_required: [request_handover]` (per Codex 1.8 / `customer_service_tool_spec_v0_2.yaml` runtime-only visibility rule, `create_case_controlled` INTENTIONALLY excluded — see inline comment at current `PhaseEvaluator.java:425-432`).
  - `required_context_keys: [form_context, customer_context]`.
  - `max_tool_steps: 3`.
  - `allow_interim_message: false`.
  - `valid_terminal_outcomes: [CLARIFICATION_NEEDED, ESCALATE]`.
  - `objective`: template `"Collect required intake details for {uc_name} and hand over to the {team_name} team."`.
  - `procedure`: legacy text from RESOLVE-INTAKE branch `systemInstruction` line ~446 (`buildIntakeSystemInstruction(activeUc, ucDef)` body — verify at session start; the per-UC composition uses team name + required-fields list + intake_complete_trigger). Use template substitution placeholders `{uc_id}` / `{uc_name}` / `{team_name}` / `{intake_complete_trigger}` / `{case_creation_note}`.
  - `grounding_instruction`: legacy text from RESOLVE-INTAKE branch `groundingInstruction` lines ~447-455 (the "Use fixed-script templates and standard intake questions. Do NOT cite knowledge articles." passage; uses `{team_name}` placeholder and `{case_creation_note}` placeholder).
  - `escalation_policy`: legacy text from RESOLVE-INTAKE branch `escalationPolicy` lines ~456-459 (uses `{intake_complete_trigger}` placeholder).
  - `guardrails`:
    ```yaml
    guardrails:
      - type: intake_complete_required
        on_fail: reject_with_hint
        parameters:
          escalation_reason_pattern: "intake_complete_for_uc_*"
          required_fields_source: IntakeFieldsRegistry
    ```
  - `state_inheritance`:
    ```yaml
    state_inheritance:
      inherit: [customer_context, intake_fields_partial]
      reset: []
      soft_signal_via_projection: [prior_use_case_carry]
    ```

**Stop checks at phase 1 exit**:

- The 2 new YAMLs validate at Spring boot via the existing Sprint 38-shipped `SkillLoader.loadAll()` happy path. The 4 typed guardrail types are in the Sprint 38-fix `VALID_GUARDRAIL_TYPES` allowlist — no allowlist extension needed. All 5 tool names are in the Sprint 38-fix `VALID_TOOL_NAMES` allowlist — no allowlist extension needed. All UCs are in the `UseCaseRegistryService` registry — `isKnownUseCase(...)` check passes.
- The Skill YAML state inheritance block uses `prior_use_case_carry` as a `soft_signal_via_projection` slot name. **CAVEAT**: the Sprint 38-fix `SkillLoader.VALID_PROJECTION_SLOTS` set may NOT include `prior_use_case_carry` (since Sprint 41 adds the slot; Sprint 39 only DECLARES it in YAML, not yet enforced). Re-verify at session start by grepping `SkillLoader.java:39-77` for `VALID_PROJECTION_SLOTS`. **If `prior_use_case_carry` is NOT in the allowlist, you have 2 options**: (i) extend the allowlist to include `prior_use_case_carry` as a future-bound slot name (justified by Sprint 41 scope; surface as handoff §7 OQ); OR (ii) omit `prior_use_case_carry` from `soft_signal_via_projection` in Sprint 39 YAMLs and add it in Sprint 41 alongside the projection slot implementation. **Option (i) is recommended** — the declaration is schema-bound but not enforcement-bound; future-naming is acceptable per design doc §10.4 + Sprint 38 codex's confirmation that "`prior_use_case_carry` appears only as an allowed future projection-slot token in the `state_inheritance` schema comments/validation set, not as a new projected slot" (Sprint 38 codex round 1 §5 hard-fence verification). If the allowlist already includes it, no action needed.

### 4.2 Execution phase 2 — SkillRegistry / PhaseEvaluator integration extension

**Goal**: extend `PhaseEvaluator.composeSkillPhasePlan(...)` for RESOLVE Skills' template substitution, and DELETE the legacy RESOLVE-INTAKE + RESOLVE-FAQ branches.

**Inputs**:

- Current `composeSkillPhasePlan(...)` at `PhaseEvaluator.java:548` (re-verify line at session start).
- Current legacy RESOLVE-INTAKE branch at `PhaseEvaluator.java` ~417-460.
- Current legacy RESOLVE-FAQ branch at `PhaseEvaluator.java` ~463-530.
- `UC_TEAM_NAME` map at `PhaseEvaluator.java:112`; `intakeCompleteTrigger(...)` helper at lines 137-148; `buildIntakeSystemInstruction(...)` helper at lines 207-208.
- `UseCaseRegistryService.getUseCase(uc)` for `{uc_name}`.

**Outputs**:

- Extended `composeSkillPhasePlan(Skill skill, String phase, String activeUc)` body:
  - For each Skill field that may contain template substitution placeholders (`objective`, `procedure`, `grounding_instruction`, `escalation_policy`): walk the text, replace `{uc_name}` with `useCaseRegistry.getUseCase(activeUc).name()`, replace `{uc_id}` with `activeUc`, replace `{team_name}` with `UC_TEAM_NAME.getOrDefault(activeUc, "specialist")`, replace `{intake_complete_trigger}` with `intakeCompleteTrigger(activeUc)`, replace `{case_creation_note}` with the case-creation-note string if `Set.of("UC-H", "UC-J", "UC-K").contains(activeUc)` else empty string.
  - **No per-UC-pair branch logic** in the substitution body — each placeholder is a single registry / map lookup.
  - The substitution preserves the field-shape contract from Sprint 38 (`composeSkillPhasePlan` maps Skill fields to PhasePlan fields without changing the PhasePlan record shape).
- Legacy RESOLVE-INTAKE branch DELETED (lines 417-460).
- Legacy RESOLVE-FAQ branch DELETED (lines 463-530).
- `PhaseEvaluator.plan(...)` body post-Sprint-39 reads approximately:
  ```java
  public PhasePlan plan(BotSession session, String userMessage, List<BotTurn> history) {
      if (session == null) return null;
      String phase = session.getCurrentPhase();
      String activeUc = session.getActiveUseCase();

      if (skillRegistry != null) {
          Optional<Skill> selected = skillRegistry.select(phase, activeUc);
          if (selected.isPresent()) {
              return composeSkillPhasePlan(selected.get(), phase, activeUc);
          }
      }
      // Unknown phase or unmapped (phase, useCase) tuple — fall back to null
      // (existing behaviour).
      return null;
  }
  ```
  (Roughly — preserve any defensive null-checks the existing code has; don't accidentally remove a guard that other callers rely on.)
- `buildIntakeSystemInstruction(...)` helper preserved (either inlined into `composeSkillPhasePlan` substitution flow, or called from substitution; dev judgement).
- `intakeCompleteTrigger(...)` helper preserved.
- `UC_TEAM_NAME` map preserved.
- `INTAKE_UCS` set preserved (becomes unreferenced from `plan(...)` body but kept as registry-driven SoT; do NOT delete).

**Stop checks at phase 2 exit**:

- Run `PhaseEvaluatorSkillIntegrationTest` (Sprint 38-shipped 13 tests) — all pass. The 4 simpler phase Skills' behavioural equivalence is preserved.
- Run a quick smoke test: invoke `phaseEvaluator.plan(session, ...)` for a UC-A FAQ session + a UC-G INTAKE session at simulated session state — verify the returned PhasePlan has the same `objective` / `allowedTools` / etc. as the pre-deletion legacy branch produced. (Inline this as a dev-internal smoke; the behavioural-equivalence integration test in phase 6 is the formal verification.)
- Verify `plan(...)` no longer references `INTAKE_UCS.contains(...)` OR `useCaseRegistry.getUseCase(...)` for branch routing — all routing is via `SkillRegistry.select(...)`.
- Verify `composeSkillPhasePlan(...)` substitution logic has NO per-UC-pair branch.

### 4.3 Execution phase 3 — Sprint 6/7/11 predicate migration

**Goal**: REMOVE the 3 static predicate methods from `AgentRunLoopImpl.java` + REPLACE their dispatch sites with unified `SkillGuardrailDispatcher` calls.

**Inputs**:

- `AgentRunLoopImpl.java` predicate method bodies at lines 628-651 (Sprint 7 `shouldRejectIncompleteIntakeHandover`), 690-734 (Sprint 6 `shouldRejectFaqMissHandover`), 745-759 (Sprint 11 `shouldRejectPrematureResolveOutcome`).
- `AgentRunLoopImpl.java` dispatch sites at lines 317, 356, 413.
- `INTAKE_COMPLETE_GUARD_REJECT_REASON` constant at line 120.
- Design doc §8.2.1 / §8.2.2 / §8.2.3 (per-predicate migration mapping) + §9.3 (dispatch site routing).

**Outputs**:

- REMOVE `shouldRejectIncompleteIntakeHandover` static method body (lines 628-651). The semantic moves to `SkillGuardrailDispatcher.handleIntakeCompleteRequired(...)`.
- REMOVE `shouldRejectFaqMissHandover` static method body (lines 690-734). The semantic moves to `SkillGuardrailDispatcher.handleFaqMissHandoverRequiresResolveAttempt(...)`.
- REMOVE `shouldRejectPrematureResolveOutcome` static method body (lines 745-759). The semantic moves to `SkillGuardrailDispatcher.handlePrematureResolveOutcomeGuard(...)` — which DELEGATES to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)` (the Sprint 11 / 11.1 / 12 frozen surface — DO NOT MODIFY `ResolveDispositionEvaluator`; only relocate the invocation site).
- REPLACE dispatch site at line 317 (current `shouldRejectIncompleteIntakeHandover` call) with `dispatcher.checkBeforeDispatch(activeSkill, call, dispatchContext)` per design doc §9.3.
- REPLACE dispatch site at line 356 (current `shouldRejectPrematureResolveOutcome` call) with `dispatcher.checkBeforeOutcomePersist(activeSkill, outcomeClass, dispatchContext)` per design doc §9.3 (this dispatch site fires on outcome persistence, NOT tool call dispatch — see §9.1 public surface).
- REPLACE dispatch site at line 413 (current `shouldRejectFaqMissHandover` call) with `dispatcher.checkBeforeDispatch(activeSkill, call, dispatchContext)` per design doc §9.3.
- For each dispatch site: build `DispatchContext` from `plan`, `session`, `accumulatedToolResults`, `lastLlmRawResponse` (per `AgentRunLoopImpl.java:156`), parsed user_message (via existing `actionParser` integration; OR `Optional.empty()` if not available at the dispatch site). If `dispatcher.checkBeforeDispatch(...)` returns non-empty `Optional<RejectVerdict>`:
  - Emit reject hint to `accumulated_tool_results.<tool>.error` (using the verdict's `hint` text) — same shape as pre-migration `shouldRejectXxx` reject pattern (the existing code paths show how `INTAKE_COMPLETE_GUARD_REJECT_REASON` etc. flow into `accumulated_tool_results`; preserve the shape).
  - Log trace per `verdict.trace()` fields (same trace pattern as pre-migration; add `skill_name` field per design doc §9.4 if not already present in the existing trace shape).
  - Short-circuit return.
- `INTAKE_COMPLETE_GUARD_REJECT_REASON` constant: preserved at `AgentRunLoopImpl.java:120` (or moved to dispatcher as public constant; dev judgement). It MUST be accessible to the dispatcher's `handleIntakeCompleteRequired` to use as the reject reason label.
- `AgentRunLoopImpl` constructor: ADD `SkillGuardrailDispatcher` parameter (likely Spring-injected); update any test sites that build `AgentRunLoopImpl` directly. Note: this is a constructor-call-site sweep similar to Sprint 38's PhaseEvaluator constructor sweep (19 existing test sites); the count for AgentRunLoopImpl call sites may be smaller — grep at session start. Use a fixture pattern similar to `SkillTestFixtures.productionRegistry()` (e.g., `SkillTestFixtures.productionDispatcher()` — recommend extending the existing fixture file rather than creating a new one).

**Stop checks at phase 3 exit**:

- All existing tests that exercise Sprint 6 / Sprint 7 / Sprint 11 predicate semantics via integration (e.g., `Sprint7IntakeStateTest`, `Sprint11ProgressiveResolveTest`, `Sprint71PartialIntakePersistenceTest`, any `AgentRunLoop*` integration tests) remain green. Survey at session start by grepping for `shouldReject*` direct references (likely few; the predicates are mostly tested via behaviour).
- Sprint 38-shipped tests (45 tests) all remain green.
- `mvn test -q` shows 1034 + N tests, 1 failure (inherited), 0 errors (where N is the count of any new tests Sprint 39 wrote so far in phases 1-3).
- The 3 `shouldRejectXxx` Java methods are GONE from `AgentRunLoopImpl.java`. `grep -c "shouldReject" AgentRunLoopImpl.java` returns 0.

### 4.4 Execution phase 4 — Unified `SkillGuardrailDispatcher`

**Goal**: ship the dispatcher Java class implementing 4 typed predicate handlers per design doc §9.

**Inputs**:

- Design doc §9 (full section).
- Design doc §8.2.1-§8.2.5 (per-predicate semantics).
- The semantics from the just-deleted `shouldRejectXxx` Java methods (the bodies you removed in phase 3 are the source-of-truth for the migrated predicates' check logic).

**Outputs**:

- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`:
  ```java
  @Component
  public class SkillGuardrailDispatcher {

      public Optional<RejectVerdict> checkBeforeDispatch(
              Skill activeSkill,
              ToolCall call,
              DispatchContext context) {
          // Walk activeSkill.guardrails() in declaration order; for each guardrail
          // entry, dispatch to the appropriate handler based on guardrail.type();
          // short-circuit on first reject.
      }

      public Optional<RejectVerdict> checkBeforeOutcomePersist(
              Skill activeSkill,
              String outcomeClass,
              DispatchContext context) {
          // Walk activeSkill.guardrails() in declaration order; for each guardrail
          // entry that fires on outcome persistence (premature_resolve_outcome_guard
          // + must_cite_source), dispatch to the appropriate handler;
          // short-circuit on first reject.
      }

      // 4 per-handler private methods:
      private Optional<RejectVerdict> handleFaqMissHandoverRequiresResolveAttempt(
              Skill skill, Guardrail guardrail, ToolCall call, DispatchContext context) {...}
      private Optional<RejectVerdict> handleIntakeCompleteRequired(
              Skill skill, Guardrail guardrail, ToolCall call, DispatchContext context) {...}
      private Optional<RejectVerdict> handlePrematureResolveOutcomeGuard(
              Skill skill, Guardrail guardrail, String outcomeClass, DispatchContext context) {...}
      private Optional<RejectVerdict> handleMustCiteSource(
              Skill skill, Guardrail guardrail, String outcomeClass, DispatchContext context) {...}
  }
  ```
- Each handler reads its declared `parameters` from the Skill guardrail declaration AND the `DispatchContext` data surface. **Each handler is a registry-driven single-condition check** — no per-UC-pair branch logic.
- `RejectVerdict` record per design doc §9.1:
  ```java
  public record RejectVerdict(
      String predicateName,    // canonical label (e.g., "s1_citation_presence_required")
      String hint,             // LLM-visible message text
      Map<String, Object> trace // trace fields (predicate input data surface)
  ) {}
  ```
- `DispatchContext` record per design doc §9.1:
  ```java
  public record DispatchContext(
      PhasePlan plan,
      BotSession session,
      Map<String, Object> accumulatedToolResults,
      String lastLlmRawResponse,
      Optional<String> parsedUserMessage
  ) {}
  ```
  (Records may be inner records on `SkillGuardrailDispatcher` or separate files — dev judgement.)

**Per-handler semantic mapping** (you implement the bodies; the design doc references are the SoT):

- `handleFaqMissHandoverRequiresResolveAttempt`: implements Sprint 6 §G2 semantics per the just-deleted `shouldRejectFaqMissHandover(plan, call, accumulatedToolResults)` method body at deleted lines 690-734. Check: `call.toolName == "request_handover"`; `call.args.escalation_reason == "faq_miss_threshold_exceeded"`; `accumulatedToolResults.get("search_knowledge")` non-empty (viable hit surfaced); `accumulatedToolResults.get("resolve_article")` empty (no resolve attempt yet). If all match, return `Optional.of(new RejectVerdict("faq_miss_handover_requires_resolve_attempt", <hint>, <trace>))`. The `FAQ_PATH_UCS` check from the deleted method becomes implicit via the active Skill's `applicable_use_cases: [UC-A, ...UC-FP]`. **Hint text**: reuse the existing reject reason label from pre-migration code (grep `AgentRunLoopImpl` for any constants used at line 413's reject path; preserve the label).
- `handleIntakeCompleteRequired`: implements Sprint 7 §I2 semantics per the just-deleted `shouldRejectIncompleteIntakeHandover(plan, call, session)` method body at deleted lines 628-651. Check: `call.toolName == "request_handover"`; `call.args.escalation_reason` matches `guardrail.parameters.escalation_reason_pattern` glob (`intake_complete_for_uc_*`); `IntakeFieldsRegistry.intakeComplete(plan.useCase(), session.getCollectedIntakeFields())` returns false (or equivalent — verify exact method signature at session start). If all match, return `Optional.of(new RejectVerdict("intake_required_fields_missing_for_intake_complete", <hint>, <trace>))`. Reuse `INTAKE_COMPLETE_GUARD_REJECT_REASON` as the canonical label (preserved from `AgentRunLoopImpl.java:120` or moved to dispatcher).
- `handlePrematureResolveOutcomeGuard`: implements Sprint 11 §M1 semantics per the just-deleted `shouldRejectPrematureResolveOutcome(plan, session, call)` method body at deleted lines 745-759 (which DELEGATES to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)`). The dispatcher impl preserves the delegation verbatim — call `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(...)` with the same args as pre-migration; if it returns true, return `Optional.of(new RejectVerdict("progressive_resolve_record_outcome_premature", <hint>, <trace>))`. **DO NOT modify `ResolveDispositionEvaluator.java`** (Sprint 11 / 11.1 / 12 frozen surface per `runtime_freeze_and_risk_policy.md` §1.1 #3).
- `handleMustCiteSource`: implements NEW S1 semantics bounded per M2 §6 #4 verbatim authorization. Read `guardrail.parameters.outcome_class` (= `resolve` per Sprint 39 YAML). Read `guardrail.parameters.cite_token_field` (= `source_id` per Sprint 39 YAML). Check: `outcomeClass` matches `parameters.outcome_class` AFTER `RecordOutcomeTool.normalizeOutcomeClass(...)` resolution. If not match, return `Optional.empty()` (predicate doesn't fire for non-`resolve` outcomes). If matches `resolve`, inspect `context.parsedUserMessage` OR the user_message from `context.lastLlmRawResponse` parsed via the same parser used elsewhere in `AgentRunLoopImpl`. Check for `source_id` token presence (e.g., `userMessage.contains("source_id")` — surface the exact check in handoff §7 OQ if any ambiguity on what "source_id" presence means; recommend the simplest reliable check). If absent, return `Optional.of(new RejectVerdict("s1_citation_presence_required", <hint per design doc §9.4>, <trace>))`. **DO NOT** judge content quality; presence only.

**Stop checks at phase 4 exit**:

- `SkillGuardrailDispatcher` compiles + the 4 handler methods are bounded to 4 typed predicate types (no `if (type.equals("unknown_type"))` fall-through behaviour; the dispatch logic should be a switch / map lookup over the 4 known types, with an explicit exception for unknown types — though Sprint 38-fix `VALID_GUARDRAIL_TYPES` allowlist already prevents unknown types from loading at boot, so this is defensive).
- The dispatcher delegates to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(...)` for the `premature_resolve_outcome_guard` handler — verify by grep `ResolveDispositionEvaluator` in the dispatcher file.
- `SkillGuardrailDispatcherTest` (next phase) covers each handler's fire / no-fire scenarios.

### 4.5 Execution phase 5 — NEW S1 + NEW S2 (declarations + dispatcher impl + edge cases)

**Goal**: verify S1 + S2 predicate scopes are bounded correctly + write the target / neighbor / negative tests.

This phase OVERLAPS phases 1 (YAML declarations) + 4 (dispatcher impl). The phase 5 framing is for verifying the bounded-scope contract — that you have NOT silently expanded the predicates beyond authorization.

**S1 `must_cite_source` bounded-scope re-verification (3 boundaries):**

- (a) Phase = RESOLVE_FAQ — implicit via `applicable_phases: [RESOLVE]` + `applicable_use_cases: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]` on `resolve_faq_grounded_answer.yaml`. The dispatcher only fires the guardrail when the active Skill is `resolve_faq_grounded_answer`. The `handleMustCiteSource` handler does NOT add an extra phase / UC check (the Skill scope check is the single source-of-truth).
- (b) Tool call = `record_outcome` with `class=resolve` — the handler reads `guardrail.parameters.outcome_class` (= `resolve`); the handler is invoked from `checkBeforeOutcomePersist(activeSkill, outcomeClass, context)` which is called from `AgentRunLoopImpl` at the `record_outcome` dispatch site (NOT the `request_handover` site; NOT other tools); the handler resolves `outcomeClass` via `RecordOutcomeTool.normalizeOutcomeClass(...)` and matches against `parameters.outcome_class`. If `outcomeClass` is not `resolve` (e.g., `escalate`, `abandon`), the handler returns `Optional.empty()` — does NOT fire.
- (c) Check = `source_id` citation presence — the handler reads `guardrail.parameters.cite_token_field` (= `source_id`); inspects user-facing message text; checks PRESENCE only; does NOT judge correctness, relevance, or content quality.

**S2 `intake_complete_required` semantics — same as existing Sprint 7 §I2 predicate:**

- Reason pattern check: `escalation_reason` matches `intake_complete_for_uc_*` glob (equivalent to startsWith `intake_complete_for_uc_`).
- Required-fields check: `IntakeFieldsRegistry.intakeComplete(uc, collected)` returns false.
- The migration ONLY changes the invocation site; the semantics are byte-for-byte preserved from pre-migration `shouldRejectIncompleteIntakeHandover` body.

**Tests for S1 + S2 in `ResolveFaqGuardrailsTest` + `ResolveIntakeGuardrailsTest` (the tests' coverage matrix is described in §2.7 D-s + D-t of the contract; see also phase 6 below):**

S1 coverage:
- **Target**: `record_outcome(class=resolve)` in `resolve_faq_grounded_answer` Skill scope without `source_id` in user message → handler returns non-empty `RejectVerdict` with `predicateName="s1_citation_presence_required"`.
- **Neighbor (positive)**: same call WITH `source_id` in user message → handler returns `Optional.empty()`.
- **Negative-1**: `record_outcome(class=escalate)` without `source_id` → handler returns `Optional.empty()` (S1 bounded to `class=resolve`).
- **Negative-2**: `record_outcome(class=abandon)` without `source_id` → handler returns `Optional.empty()` (S1 bounded to `class=resolve`).
- **Negative-3 (Skill scope)**: simulate DISCOVER phase OR `discover_triage` Skill — no `must_cite_source` guardrail declared; dispatcher does NOT invoke `handleMustCiteSource`; no reject regardless of user message text. (This is a structural test — the dispatcher walks `activeSkill.guardrails()`; if `must_cite_source` is not in the list, the handler is never called.)

S2 coverage:
- **Target**: `request_handover(escalation_reason=intake_complete_for_uc_H)` in `resolve_intake_collect_and_handover` Skill scope with required fields missing → handler returns non-empty `RejectVerdict`.
- **Neighbor (positive)**: same call with required fields complete → handler returns `Optional.empty()`.
- **Negative-1**: `request_handover(escalation_reason=user_requested)` regardless of intake state → handler returns `Optional.empty()` (S2 bounded to `intake_complete_for_uc_*` reason).
- **Negative-2 (Skill scope)**: simulate UC-A FAQ session → `resolve_intake_collect_and_handover` is not the active Skill; dispatcher does NOT invoke `handleIntakeCompleteRequired`; no reject regardless of intake state.

**Stop checks at phase 5 exit**:

- S1 implementation matches the 3-boundary contract (a/b/c).
- S2 implementation matches the existing Sprint 7 §I2 semantics (verifiable by comparing to the deleted method body in phase 3).
- All target / neighbor / negative tests written + passing.
- `ResolveFaqGuardrailsTest` + `ResolveIntakeGuardrailsTest` use mocked LLM (deterministic Java logic tests; per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` this is correct usage — the tests verify Java enforcement logic, NOT LLM behaviour).

### 4.6 Execution phase 6 — Behavioural-equivalence + negative + resolve-flow evidence

**Goal**: write the behavioural-equivalence + dispatcher unit + integration tests.

**Outputs**:

- `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java` — behavioural-equivalence integration tests per design doc §6.4:
  - For RESOLVE_FAQ × UC-A (and UC-B / UC-C / UC-D / UC-E / UC-F / UC-FP representative UCs): assert pre-migration PhasePlan (golden strings captured inline from the just-deleted RESOLVE-FAQ branch) field-by-field equal to post-migration PhasePlan (from `SkillRegistry.select("RESOLVE", "UC-A")` → `composeSkillPhasePlan(...)` with template substitution).
  - For RESOLVE_INTAKE × UC-G (and UC-H / UC-I / UC-J / UC-K): similar golden-string assertions.
  - Golden strings: capture at the START of Sprint 39 BEFORE phase 2 deletes the legacy branches. Inline them in the test file as `private static final String EXPECTED_RESOLVE_FAQ_UC_A_OBJECTIVE = "..."` etc.
  - ~15-25 tests total (representative UC + state scenarios).
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java` — unit tests for dispatcher (~25-40 tests):
  - `faq_miss_handover_requires_resolve_attempt`: target (rejects when conditions met) + no-fire scenarios (search empty, resolve_article non-empty, different reason, different tool).
  - `intake_complete_required`: target + no-fire scenarios.
  - `premature_resolve_outcome_guard`: target + no-fire (delegate to ResolveDispositionEvaluator).
  - `must_cite_source`: target + no-fire scenarios (4 negatives: class=escalate, class=abandon, source_id present, Skill scope mismatch).
  - Short-circuit-on-first-reject: declare 2 guardrails on a synthetic Skill; first rejects; verify the second's handler is NOT invoked.
  - `RejectVerdict` shape verification: predicateName / hint / trace fields populated correctly.
  - `DispatchContext` null-safety: handlers handle missing fields (e.g., `accumulatedToolResults` empty, `lastLlmRawResponse` null, `parsedUserMessage` empty Optional).
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveFaqGuardrailsTest.java` — integration tests verifying the 3 RESOLVE_FAQ guardrails fire correctly at dispatch sites with full dispatcher + AgentRunLoopImpl integration (~15-20 tests; see §4.5 coverage matrix).
- `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveIntakeGuardrailsTest.java` — integration tests verifying the 1 RESOLVE_INTAKE guardrail fires correctly (~8-12 tests; see §4.5).

**Stop checks at phase 6 exit**:

- All ~60-100 new Sprint 39 tests pass.
- Full `mvn test -q` shows baseline 1034 + N new tests = 1094-1134; 1 inherited failure persists; 0 new failures; 0 errors.
- Sprint 38-shipped tests (45) remain green.
- Sprint71PartialIntakePersistenceTest 14/14 remains green.
- Existing tests for Sprint 6 / 7 / 11 predicate semantics via integration remain green (the dispatcher impl preserves the semantics).

---

## 5. STOP discipline (reproduced from contract §10 with emphasis)

If you encounter any of these conditions, STOP and surface in handoff §7 OQ. Do NOT silently work around. The full list is in contract §10; the LOAD-BEARING conditions for Sprint 39 are:

1. **§6 #4 authorization expansion temptation** — if any implementation tension surfaces requiring expansion of S1 beyond (a) phase=RESOLVE_FAQ; (b) tool=record_outcome with class=resolve; (c) check=source_id presence in user-facing message — STOP. Examples of expansion that would trigger STOP:
   - Enforcing citation on `class=escalate` or `class=abandon`.
   - Fanning out S1 to RESOLVE_INTAKE or DISCOVER or other phases.
   - Judging content quality (correctness, relevance, factual accuracy) of the citation.
   - Enforcing citation in non-`record_outcome` tools.
2. **Generic rule-engine dispatcher temptation** — if you find yourself building a dispatcher that loads predicate definitions from arbitrary external config beyond the 4 typed predicate types declared in Sprint 37 freeze §5.2 + §8.2 — STOP. The dispatcher is bounded to 4 typed predicates: `faq_miss_handover_requires_resolve_attempt`, `intake_complete_required`, `premature_resolve_outcome_guard`, `must_cite_source`. The `SkillLoader.VALID_GUARDRAIL_TYPES` allowlist (post-Sprint-38-fix at `SkillLoader.java:117`) is the load-bearing whitelist; the dispatcher mirrors it.
3. **Touch to `UseCaseRouter` / `escalation_reason` enum / `system_prompt.txt`** — STOP per contract §6 #6 + #8 + #1. Sprint 40+ scope.
4. **Silent scope split temptation** — if any execution phase (§4.1-§4.6) cannot complete cleanly within Sprint 39 — STOP. Surface in handoff §7 OQ for deliver-agent + human review. The milestone framework allows replanning at human review round, NOT silent scope expansion (more in Sprint 39) or contraction (peel off into Sprint 40+).
5. **RESOLVE behavioural equivalence cannot be preserved** — if the `PhaseEvaluatorResolveSkillIntegrationTest` golden-string assertions fail and you cannot bring them to pass without widening the equivalence check — STOP. The migration is invalid per the freeze contract per design doc §6.4. Do NOT widen the equivalence assertion to accept divergence. Do NOT silently widen the M2 §6 fences. **If equivalence cannot be preserved, STOP the entire sub-sprint** and surface in handoff §7 OQ.
6. **Tier-0 candidate temptation** — if you observe that the dispatcher's short-circuit-on-first-reject semantics (the C2 guardrail-refusal-non-overridability claim) should be a Tier-0 invariant — STOP. Sprint 39 ships the FIRST observed evidence surface; deliver-agent + human re-evaluate at Sprint 39 close OR M2 close. Do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 39 dev commit.
7. **Sprint 11 / 11.1 / 12 frozen surface modification temptation** — if you find yourself modifying `ResolveDispositionEvaluator.java` — STOP. Sprint 11 / 11.1 / 12 frozen per `runtime_freeze_and_risk_policy.md` §1.1 #3. Only the invocation site moves from `AgentRunLoopImpl` to `SkillGuardrailDispatcher.handlePrematureResolveOutcomeGuard`.
8. **M1 functional surface modification temptation** — if you find yourself modifying `IntakeFieldsRegistry.java` or `IntakeFieldExtractor.java` — STOP per contract §6 #34.
9. **NEW outcome class enum temptation** — if you find yourself adding `resolve_no_citation` to `RecordOutcomeTool.normalizeOutcomeClass` — STOP per contract §6 #9. Reject-and-hint is the M2-scope `on_fail` mode.

---

## 6. Hard fences (reproduced from contract §6 with emphasis)

The full list is in contract §6. The LOAD-BEARING fences for Sprint 39 are:

- **No edit to `system_prompt.txt`** (Sprint 40 scope).
- **No edit to `ContextProjectionBuilder.java`** (Sprint 41 scope; NEW `prior_use_case_carry` projection slot Sprint 41 adds).
- **No NEW `SkillStateBus.java`** (Sprint 41 scope).
- **No expansion of S1 `must_cite_source` predicate beyond M2 §6 #4 verbatim authorization scope** (3 boundaries; see §7 below).
- **No generic rule-engine dispatcher** (4 typed predicate types only).
- **No touch to `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool`** (M3-D deferred).
- **No edit to `INTAKE_UCS` set OR `FAQ_PATH_UCS` set as branching surface** — preserve as registry-driven SoT; do NOT delete the constants even if they become structurally unreferenced.
- **No `escalation_reason` enum widening** (M3-A deferred).
- **No `RecordOutcomeTool.normalizeOutcomeClass` enum widening** (no NEW outcome class like `resolve_no_citation`).
- **No Tier-0 invariant added** (do NOT edit `runtime_freeze_and_risk_policy.md` in dev commit).
- **No edit to `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md`** (constitution-discipline preserved).
- **No edit to `docs/proposals/skill_registry_design.md`** (Sprint 37 freeze immutable).
- **No edit to sprint archives** under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-038-*`.
- **No edit to milestone archives** under `docs/milestones/`.
- **No edit to `docs/milestone_objective.md` or `docs/sprint_objective.md`** (deliver-agent-owned).
- **No edit to `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-039-*.md`** (deliver-agent-owned).
- **No edit to existing case families** under `eval_interactive/case_specs/case_families/<existing>/`.
- **No edit to shadow CaseSpecs** or `eval_interactive/eval_interactive/`.
- **No edit to bad-case YAMLs** including Alice (M2 §5 acceptance recalibration).
- **No mocked-LLM as primary evidence** for LLM-behaviour claims; dispatcher Java logic tests + behavioural-equivalence integration tests MAY mock LLM (those are Java logic, not LLM behaviour).
- **No per-UC-branch if-else** in any Skill YAML body OR dispatcher Java code OR `composeSkillPhasePlan(...)` extension per Constitution §1.7 + M2 §6 #1.
- **No Skill that prescribes LLM customer-facing language** (M2 §6 #18; §1.3 preserved).
- **No Skill that hard-encodes per-step argument values** beyond the registered guardrail's enforced argument scope (M2 §6 #19; §1.3 preserved).
- **No regression on `Sprint71PartialIntakePersistenceTest` 14/14** (M1 functional-surface protection).
- **No regression on Sprint 38-shipped tests** (45 tests).
- **No `ResolveDispositionEvaluator.java` edit** (Sprint 11 / 11.1 / 12 frozen surface).
- **No `IntakeFieldsRegistry.java` or `IntakeFieldExtractor.java` edit** (M1 Sprint 34 functional surface).
- **No `on_fail: observe_only`** (rejected per Sprint 37 freeze §5.2).
- **No `on_fail: downgrade_reason`** in Sprint 39 (deferred; default is `reject_with_hint`).
- **No CLOSE → TERMINAL phase enum rename** (OQ-7.1 default).
- **No silent scope split**.

---

## 7. §6 #4 verbatim S1 authorization quote (LOAD-BEARING; reproduced for dev session)

This is the human authorization governing the S1 `must_cite_source` predicate scope. Quote verbatim from `docs/milestone_objective.md` §6 #4 + Sprint 37 freeze §8.2.4 + Sprint 39 contract §2.6:

> **"Accept the Skill-bounded exception to D-hard-citation-gate. This is not a generic Java grounding-citation gate. It is a narrow S1 terminal predicate that only applies when the bot attempts `record_outcome(class=resolve)` inside the `RESOLVE_FAQ` scope, and only checks citation presence as the minimum grounding-floor condition. The original deferral of a generic Java citation gate remains valid."**

The authorization governs the predicate scope. Sprint 39 implementation MUST NOT expand beyond:

- (a) **Phase = `RESOLVE_FAQ`** — i.e., `current_phase == RESOLVE` AND active Skill is `resolve_faq_grounded_answer` (which has `applicable_use_cases: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]`).
- (b) **Tool call = `record_outcome` with `class=resolve`** — per `RecordOutcomeTool.normalizeOutcomeClass` at `RecordOutcomeTool.java:110-121` resolving canonical `resolve` + legacy `RESOLVED` alias. The predicate fires on `record_outcome(class=resolve)` dispatch ONLY; it does NOT fire on `record_outcome(class=escalate)`, `record_outcome(class=abandon)`, OR any other outcome class.
- (c) **Check = `source_id` citation present in user-facing message** — the predicate verifies the bot's user_message text contains a non-empty `source_id` token. It does NOT judge correctness, relevance, or content quality of the citation — only presence.

Any expansion is OUT OF SCOPE for Sprint 39 and requires a NEW objective + human review.

---

## 8. Behavioural-equivalence test pattern (per design doc §6.4)

Each migrated phase (RESOLVE_FAQ + RESOLVE_INTAKE) ships with `<Phase>SkillIntegrationTest` that asserts:

- For representative UCs in the phase, the Skill-composed PhasePlan equals (or observationally matches) the legacy-branch PhasePlan produced by the pre-migration Java code path.
- `equals` is structural: same `objective` text after placeholder substitution, same `allowedTools` list (order may differ — tests compare as Set), same `requiredContextKeys`, same `maxToolSteps`, same `validTerminalOutcomes`, same `systemInstruction` text after template substitution, same `groundingInstruction`, same `escalationPolicy`.

**Golden-string capture procedure**:

1. BEFORE phase 2 (DELETE legacy RESOLVE branches): copy the pre-migration legacy branch's output for each representative UC. The simplest approach: write a Java snippet that invokes `phaseEvaluator.plan(session, ...)` for each representative UC and serializes the returned PhasePlan to a static string; inline the string in the test file as `private static final String EXPECTED_RESOLVE_FAQ_UC_A_OBJECTIVE = "..."`. Or: cherry-pick the field text from the source file before deletion.
2. AFTER phase 2: re-invoke `phaseEvaluator.plan(session, ...)` for the same representative UC, expect the same PhasePlan (now from `SkillRegistry`-driven composition).
3. The test asserts field-by-field equality.

**Coverage matrix**:

- RESOLVE_FAQ × representative UCs: UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP (7 UCs × representative scenarios).
- RESOLVE_INTAKE × representative UCs: UC-G, UC-H, UC-I, UC-J, UC-K (5 UCs × representative scenarios).
- Total: ~15-25 tests (some UCs may share golden strings; cherry-pick to keep test count reasonable while covering the substitution paths).

If golden strings cannot be matched (e.g., a template substitution placeholder resolves differently than the pre-migration hardcoded value), STOP and surface in handoff §7 OQ.

---

## 9. §4.1 anti-hardcode self-walk required at handoff §6

You self-walk the §4.1 9 questions on the Sprint 39 diff and write the result in handoff §6. The expected verdict is `approve`. For each question:

- Paste relevant diff snippets if there's any ambiguity.
- For each "yes" or each concern, name the reasoning.

Walk:

1. **Q1 Keyword / regex / if-else / enum / per-UC matrix for semantic decision?** NO. SkillRegistry selection is exact-match-then-wildcard lookup (NOT a branch table); `composeSkillPhasePlan(...)` template substitution is registry-driven single-lookup per placeholder (NOT per-UC-pair branch); dispatcher predicate handlers are registry-driven single-condition checks (NOT per-UC-pair branch). The RESOLVE Skill YAMLs' `applicable_use_cases` are explicit UC enumerations (NOT branching tables; they're SCOPE declarations matched by SkillRegistry's lookup semantics).
2. **Q2 Tier-0 justified?** No new Tier-0; C2 + C3 R-items continue DEFER (Sprint 39 dispatcher provides FIRST observed evidence for C2; re-evaluation at Sprint 39 close OR M2 close).
3. **Q3 Soft signal achievable?** N/A — Sprint 39 is architectural refactoring + bounded predicate landing; the migrated predicates were already Runtime-floor enforcement pre-Sprint-39 (Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1); NEW S1 is the M2 §6 #4 verbatim authorized Runtime-floor extension.
4. **Q4 Visible-eval / trace phrasing / CaseSpec id encoded?** NO. The 2 RESOLVE Skill YAMLs carry legacy `PhaseEvaluator.java` Java-string content (which Codex M1 / Sprint 37 / Sprint 38 already cleared). Dispatcher trace shape extends the existing Sprint 6/7/11 trace pattern (`predicate_name` + `reject_reason_label` + new `skill_name` field per design doc §9.4).
5. **Q5 Moves semantic ownership LLM → Java?** NO. The migrated predicates were Java pre-Sprint-39 (Sprint 6/7/11 ALREADY shipped them); the migration relocates the invocation surface (from `AgentRunLoopImpl` to `SkillGuardrailDispatcher`) — no LLM ownership shift. The NEW S1 is bounded per M2 §6 #4 verbatim authorization (grounding-floor minimum-citation-presence is Runtime-owned per §1.4); NOT a shift of LLM-owned next-action / response strategy / customer-facing language.
6. **Q6 If-else in prompt?** NO. RESOLVE Skill YAMLs' `procedure` / `grounding_instruction` / `escalation_policy` text is principle-level + template-substituted with registry-driven values; no per-UC-pair if-else in prompt text.
7. **Q7 Tool schema / capability / PII / grounding floor preserved?** YES (preserved + extended for S1 per M2 §6 #4). Tool schemas unchanged; `PhasePlan.allowedTools` composition unchanged (per phase Skill's `tools_required`); PII / safety floor untouched; grounding floor EXTENDED for S1 bounded per M2 §6 #4 verbatim authorization (NOT a generic citation gate; the original `D-hard-citation-gate` deferral preserved for all other phases / UCs / tools).
8. **Q8 Generalization coverage?** YES per §8 stanza. Target = 2 RESOLVE phase Skills × 12 representative UCs. Neighbor = 4 Sprint 38 phase Skills + Sprint 71 / Sprint 7 / Sprint 11 existing tests. Negative = 4 negatives on NEW S1 (class=escalate, class=abandon, source_id present, Skill scope mismatch) + 2 negatives on S2 (non-matching reason, Skill scope mismatch) + 12 SkillLoader negative cases (carried from Sprint 38 unchanged). Shadow = N/A per M2 §5 recalibration.
9. **Q9 Rollback / sunset?** N/A — Skill Registry abstraction is permanent. Per Sprint 39 STOP discipline: if behavioural-equivalence regresses, dev stops; legacy branches can be restored via `git revert <sprint-39-commit>` if needed.

Expected self-walk verdict: `approve`.

---

## 10. Bundle policy (single dev commit)

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`:

- **Stage these (dev-authored, in your single Sprint 39 dev commit):**
  - `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`
  - `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml`
  - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`
  - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/RejectVerdict.java` (if separate file)
  - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/DispatchContext.java` (if separate file)
  - `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (EDIT)
  - `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (EDIT)
  - `server/src/test/java/com/gumtree/csagent/service/runtime/PhaseEvaluatorResolveSkillIntegrationTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcherTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveFaqGuardrailsTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/ResolveIntakeGuardrailsTest.java`
  - `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillTestFixtures.java` (EDIT if extending; or NEW dispatcher fixture file)
  - Any other tests under `server/src/test/` that you edit due to constructor-call-site updates (e.g., existing tests building `AgentRunLoopImpl` directly that need `SkillGuardrailDispatcher` injection)
  - `docs/sprints/sprint-039-handoff.md` (NEW)
- **Do NOT stage** (deliver-agent-owned; the human bundles at sub-sprint close):
  - `docs/sprint_objective.md`
  - `docs/milestone_objective.md`
  - `docs/10-handoff.md`
  - `docs/action_bank.md`
  - `docs/codex-findings.md`
  - `compact/sprint-039-*.md`
  - Anything in `docs/sprints/sprint-001-*` through `docs/sprints/sprint-038-*` (immutable archives).
  - Anything in `docs/milestones/`.
  - Anything in `docs/foundational/` or `docs/current/`.
  - Anything in `docs/proposals/`.

If you find yourself staging a deliver-agent-owned file, STOP and surface — that's a bundle-policy violation.

---

## 11. Handoff §11 12-section contract

Write `docs/sprints/sprint-039-handoff.md` with these 12 sections per Sprint 31-38 shape. The full contract is in `docs/sprint_objective.md` §11; abbreviated here:

1. **Context Pack** — relevant docs table (cite each doc + tier + status + one-line relevance); verified code shape at session start (HEAD `5787806`) with line-number citations; feedback memory references; doc-status warnings; source-of-truth decision; implementation status; risks-before-implementation list.
2. **Sub-sprint-objective recap** — Sprint 39 scope; layer; semantic-touching multi-layer with multi-fence convergence.
3. **Premise re-verification** — table walking each of the 15 §4 premises with verification evidence (line numbers re-checked); PASS / drift / refinement per premise.
4. **Implementation walkthrough** — high-level summary of 6 execution phases (§4.1-§4.6 of this prompt); cross-reference to design doc sections; report on constructor-call-site updates + any test-file updates.
5. **Sprint 37 freeze fidelity** — for each Sprint 37 freeze decision (e §6.2.5 + §6.2.6) + (g §8.2.1-§8.2.5) + (h §9), cite the implementation file + line range that honors the decision; note any departures with rationale; specifically note S1 predicate scope verification (3-boundary check) + dispatcher boundedness verification (4 typed predicates).
6. **§4.1 anti-hardcode self-walk on the Sprint 39 diff** — walk all 9 questions per §9 of this prompt; expected verdict `approve`.
7. **Open questions for deliver-agent + human** — surface any §1.7 boundary case; any premise drift; any tension between freeze template and implementation; any decision that surfaced unexpected tension. Recommended OQ topics: `on_fail: downgrade_reason` for `intake_complete_required` per design doc §9.6 Alternative D; placement of `RejectVerdict` + `DispatchContext` records; placement of `INTAKE_COMPLETE_GUARD_REJECT_REASON` constant; any C2 Tier-0 re-evaluation evidence; any `prior_use_case_carry` slot allowlist edit (if needed at SkillLoader).
8. **Anti-hardcode self-walk** — same as §6 (duplicate or cross-reference).
9. **Files changed** — table with paths; ~14-20 files (2 NEW YAMLs + 1-3 NEW Java classes + 2 EDITs + 4 NEW test files + any test-edits + handoff).
10. **Layer-classification self-walk** per Sprint 39 §8 stanza.
11. **§5 Eval Acceptance bars** — Sprint 39 hard gates: 2 RESOLVE YAMLs load; dispatcher compiles + 4 typed predicates pass tests; PhaseEvaluator integration + legacy RESOLVE branches DELETED; AgentRunLoopImpl 3 dispatch sites routed + 3 static predicates REMOVED; behavioural equivalence on RESOLVE × 12 representative UCs; Java baseline 1034 + ~60-100 new = ~1094-1134; Sprint71PartialIntakePersistenceTest 14/14; M1 functional-surface; Sprint 38-shipped tests preserved; Sprint 11 / 11.1 / 12 frozen surface preserved. Interactive eval smoke + bad-case suite are OBSERVATION ONLY.
12. **Closure verdict placeholder** — leave for deliver-agent + human + Codex per `feedback_handoff_verdict_section_delegation.md`. **Do NOT fill.**

---

## 12. Commit message template

```
sprint 39: RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 + unified dispatcher (NEW M2 sub-sprint 3)

Sprint 39 is the THIRD sub-sprint of NEW Milestone M2 (Skill Registry
Abstraction + Wholesale Retroactive Externalization) and the SECOND
implementation sub-sprint after the Sprint 37 design freeze + Sprint 38
SkillRegistry-core landing. Sprint 39 implements Sprint 37 freeze
decisions (e §6.2.5 + §6.2.6) + (g §8.2.1-§8.2.5) + (h §9) as runtime
code, completing the SkillRegistry abstraction for ALL 6 phases.

Ships:
- 2 NEW RESOLVE Skill YAMLs at server/src/main/resources/skills/:
  - resolve_faq_grounded_answer.yaml (UC-A/B/C/D/E/F/FP; 3 guardrails:
    faq_miss_handover_requires_resolve_attempt + premature_resolve_outcome_guard
    + must_cite_source); legacy content from PhaseEvaluator RESOLVE-FAQ
    branch.
  - resolve_intake_collect_and_handover.yaml (UC-G/H/I/J/K; 1 guardrail:
    intake_complete_required; template substitution placeholders {uc_name}
    / {uc_id} / {team_name} / {intake_complete_trigger} / {case_creation_note});
    legacy content from PhaseEvaluator RESOLVE-INTAKE branch.
- 1 NEW unified Java dispatcher class at
  server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java
  (Spring @Component); checkBeforeDispatch(...) + checkBeforeOutcomePersist(...);
  4 typed predicate handlers; short-circuit-on-first-reject semantics
  per design doc §9.2.
- PhaseEvaluator.java integration extension: composeSkillPhasePlan(...)
  extended for RESOLVE Skills' template substitution per design doc §6.2.5;
  legacy RESOLVE-INTAKE branch (lines ~417-460) + RESOLVE-FAQ branch (lines
  ~463-530) DELETED; ~120-line reduction.
- AgentRunLoopImpl.java migration: 3 dispatch sites (lines 317, 356, 413)
  routed through SkillGuardrailDispatcher; 3 static predicate methods
  (shouldRejectFaqMissHandover 690-734; shouldRejectIncompleteIntakeHandover
  628-651; shouldRejectPrematureResolveOutcome 745-759) REMOVED. Constructor
  extended with SkillGuardrailDispatcher injection.
- NEW S1 `must_cite_source` predicate declared in resolve_faq_grounded_answer.yaml
  + implemented in dispatcher bounded per M2 §6 #4 verbatim human authorization
  (phase=RESOLVE_FAQ, tool=record_outcome with class=resolve, check=source_id
  citation presence in user-facing message). NO expansion beyond authorization scope.
- NEW S2 `intake_complete_required` predicate declared in
  resolve_intake_collect_and_handover.yaml + implemented in dispatcher;
  semantically identical to the existing Sprint 7 §I2 predicate (the migration
  moves it from inline Java method to declarative Skill guardrail; same
  IntakeFieldsRegistry-driven required-fields check).
- ~60-100 new tests: PhaseEvaluatorResolveSkillIntegrationTest (behavioural
  equivalence for RESOLVE × 12 representative UCs); SkillGuardrailDispatcherTest
  (4 typed predicates + short-circuit semantics + RejectVerdict shape +
  DispatchContext null-safety); ResolveFaqGuardrailsTest (target / neighbor /
  negative for 3 RESOLVE_FAQ guardrails); ResolveIntakeGuardrailsTest (target /
  neighbor / negative for RESOLVE_INTAKE guardrail).

Post-Sprint-39: PhaseEvaluator.java carries ZERO hardcoded systemInstruction/
groundingInstruction/escalationPolicy for any of the 6 phases; AgentRunLoopImpl.java
carries ZERO `shouldRejectXxx` Java methods; all 6 phase Skills are externalized
YAML; the SkillRegistry abstraction fully covers the runtime per-turn
dispatch surface.

Sprint 37 freeze fidelity verified for decisions (e §6.2.5 + §6.2.6) +
(g §8.2.1-§8.2.5) + (h §9). Sprint 11 / 11.1 / 12 frozen surface preserved
(ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome delegation
preserved; only invocation site relocates). M1 functional surfaces preserved
(Sprint71PartialIntakePersistenceTest 14/14; IntakeFieldExtractor;
Sprint 32/33 projection slots). Sprint 38-shipped 45 tests preserved.

§4.1 anti-hardcode kernel self-walk: approve (handoff §6). No new Tier-0
candidate; C2 dispatcher-non-overridability claim — FIRST observed evidence
surface lands Sprint 39; deliver-agent + human re-evaluate at Sprint 39 close
OR M2 close per `feedback_constitution_discipline_vs_planning_anticipation.md`.

Java baseline: pre-Sprint-39 1034/1-inherited/0/2; post-Sprint-39
~1094-1134/1-inherited/0/2 (~60-100 new tests; 1 inherited
SystemPromptUserRequestedTiebreakerTest failure persists).

Codex per-sub-sprint review at Sprint 39 close per `iteration_governance.md`
§4.3 trigger #3 (Runtime grounding-floor surface + Runtime capability floor
+ multi-fence convergence at one sub-sprint).

Refs: docs/sprint_objective.md (Sprint 39 contract); docs/milestone_objective.md
(NEW M2 milestone); docs/proposals/skill_registry_design.md §5.2 + §6.2.5 +
§6.2.6 + §6.4 + §8 + §9 (Sprint 37 freeze, immutable).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

---

End of Sprint 39 dev prompt. Read end-to-end, then go through §2 read order, then re-verify §2.6 code shape, then execute §4 phases with stop checks. Write the handoff per §11, commit per §12, and surface in §10 bundle. If any STOP condition fires (§5 reproduces contract §10), STOP and surface in handoff §7 OQ — do NOT silently work around.
