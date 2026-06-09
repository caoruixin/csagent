Paste the content below this line into a fresh Claude Code session after the human commits the deliver-agent's S-Eval-1 close + S-Eval-2 launch bundle. The S-Eval-2 sub-sprint contract is `docs/sprint_objective.md` at HEAD.

---

You are Claude Code working as the **dev agent** for **Sprint 43 — Skill `critical_steps` schema + extractor + projection wiring** (S-Eval-2; the SECOND sub-sprint of NEW Milestone M3-Eval: Coarse-to-Fine Evaluation Architecture, per `docs/milestone_objective.md`).

S-Eval-1 (Sprint 42) closed Clean PASS 2026-05-21 (dev commit `d91bd3d` + close-out `357e949`; archive at `docs/sprints/sprint-042-objective.md` + `docs/sprints/sprint-042-handoff.md`). S-Eval-1 landed: 4 schema demotions (`bot_handling_pattern` Optional; `escalation_trigger` coupling relaxed via warning; `expected_tool_sequence` / `forbidden_tools` scoring-effect demoted) + 3 scoring dims to Tier-3 advisory + new `severity` field on `OutcomeCheckResult` + 12 new `anchor_outcome/` cases + composite gate opt-in/opt-out via `scoring.outcome_checks`. **S-Eval-2 builds on S-Eval-1's `severity` field convention** — Tier-2 results use the same `critical | advisory` semantics that S-Eval-1 D-2.5 introduced.

Sprint 43 is the **schema + contract sub-sprint** of M3-Eval — multi-layer (Java + Python). It extends the Skill YAML schema with the NEW `critical_steps` field, lands `CriticalStep.java` + SkillLoader allowlist validation + ContextProjectionBuilder projection wiring, creates the Python `SkillProcedureExtractor` with the **minimal 6-primitive `trace_check` DSL** that is **structurally constrained** to reject regex / keyword / message-content matching (this is the **milestone's primary structural defence against §1.7 violations** on the eval side), and wires Tier-2 `skill_procedure_followship` into composite scoring.

**Sprint 43 ships NO `critical_steps` content** — only the contract + extractor + projection wiring. Content lands in S-Eval-3 (which has per-sub-sprint Codex review per §4.3 trigger #2 because the content is LLM-visible and §1.7-adjacent). The structural defence (the DSL parser) shipped at S-Eval-2 is what makes S-Eval-3 content authoring safe.

The scope is locked at `docs/sprint_objective.md` §2 (5 outcomes) + §5 (Files in scope) + §6 (Files NOT in scope). All 12 sections of the sub-sprint contract are binding.

Sprint 43 explicitly DOES NOT:

- Fill any `critical_steps` content on any Skill YAML (deferred to S-Eval-3; S-Eval-3 has per-sub-sprint Codex review).
- Modify any existing Skill `procedure` text or `guardrails` content or `state_inheritance` content or `applicable_use_cases` or `system_instruction` on any of the 6 YAMLs.
- Touch `SkillRegistry.select(...)` body or `SkillStateBus.applyOnSkillSwitch(...)` body or `SkillGuardrailDispatcher.dispatch(...)` body (only optional getter extension on SkillRegistry IF needed for ContextProjectionBuilder wiring; check before touching).
- Touch runtime semantic surfaces (RuntimeIntentClassifier, IntentClassification, DriftResult, DriftDetector, UseCaseRouter, ClassifyUseCaseTool, PhaseEvaluator semantic dispatch, AgentRunLoopImpl, ControlKernel, system_prompt.txt, tool-policy.yaml).
- Touch IntakeFieldsRegistry, UseCaseRegistryService, ResolveDispositionEvaluator (M2 frozen surfaces).
- Touch S-Eval-1-landed schema (`schema.py`, `loader.py`, `hard_checks.py`, `outcome_checks.py`) — those are S-Eval-1 territory; S-Eval-2 only edits `composite.py` for Tier-2 wiring.
- Touch L3 judge / rubric (`llm_judge.py` is S-Eval-5 territory).
- Touch any case fixture (smoke, anchor, anchor_outcome, case-family, bad-case, shadow) or `case_spec_overrides.yaml`.
- Touch the eval harness / loader / simulator (`eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`).
- Extend the `trace_check` DSL beyond the 6 frozen primitives (if S-Eval-3 needs a new primitive, that's a deliver-agent + human decision at S-Eval-3 planning; do NOT pre-emptively widen).
- Touch governance docs (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`, `customer_service_tool_spec_v0_3.md`), foundational docs, or sprint / milestone archives.
- Touch `docs/codex-findings.md` (review-agent territory).
- Add Tier-0 invariants (milestone-level §6 hard fence).
- Address S-Eval-1 OQ-S42.3 (D-2.2 demotion interpretation; routed to M3-Eval milestone-shared Codex review).
- Use mocked-LLM as primary evidence for any LLM-behaviour claim.

## 1. Read order on cold start

1. **`AGENTS.md`** (transitively loads `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` §1 / §3 / §4 / §5 / §5.5 / §5.6 / §7 / §8).
2. **`docs/milestone_objective.md`** — NEW M3-Eval north star. ESPECIALLY:
   - §1 Milestone class (5 sub-sprints; S-Eval-2 is multi-layer `eval_spec` + `prompt_projection`; **Codex deferred to milestone close**, NOT per-sub-sprint).
   - §2 Goal (four-tier pyramid; S-Eval-2 lands the Tier-2 schema + extractor + projection wiring).
   - §3 Sub-sprint sequence → S-Eval-2 scope detail (5 items).
   - §4 Non-goals (M3-Eval-level; inherited into S-Eval-2).
   - §6 Hard fences (15 milestone-level items; S-Eval-2 §6 inherits all).
   - §8.1 Codex review prompt outline (S-Eval-2's contribution gets reviewed at M3-Eval close).
3. **`docs/sprint_objective.md`** — Sprint 43 / S-Eval-2 contract. ALL 12 sections binding. §4 premise check already verified by deliver-agent 2026-05-21; you may re-verify but DO NOT skip if §4 line counts or field locations have drifted.
4. **`docs/solutions/m3_eval_milestone_proposal.md`** — proposal source. LOAD-BEARING sections: §2 (four-tier architecture) + §5 (decision-5 deep-dive — explains WHY `critical_steps[].desc` is LLM-visible, even though S-Eval-2 ships no content; the structural defence is built TO catch S-Eval-3 violations) + §5.3 (anti-hardcode standard table — informational for the DSL parser design) + §6 S-Eval-2 (scope detail; reconcile with the contract — contract takes precedence on numeric / scope discrepancies). CONTEXTUAL only: §6 S-Eval-3 (next sub-sprint; you do NOT pre-anticipate).
5. **`docs/sprints/sprint-042-handoff.md`** — S-Eval-1 dev archive. LOAD-BEARING sections: §3 (S-Eval-1 file shipments + `severity` field landing; S-Eval-2 builds on this) + §5 (scoring code demotion details; S-Eval-2 extends the `severity`-driven model to Tier-2) + §12.7 (carry-over — UC distribution; CONTEXTUAL only for S-Eval-2). CONTEXTUAL only: §4 / §6 / §8 / §9 / §10.
6. **`docs/current/iteration_governance.md`** specifically:
   - §1.3 LLM-owned (S-Eval-2 augments the LLM's per-turn projection with `critical_steps[].desc` rendering; this PRESERVES §1.3 LLM-owned by surfacing soft procedural narrative to the LLM, not by encoding decisions in Java).
   - §1.4 Runtime-owned (S-Eval-2 adds NO new Runtime-floor enforcement; Tier-2 is eval-side, not runtime-side).
   - **§1.7 Forbidden** — **VERY LOAD-BEARING for the DSL parser design.** Re-read the forbidden list carefully; the parser MUST reject any primitive that would constitute a keyword / regex / message-content semantic decision.
   - §3.2 Q3 (`prompt_projection` layer) + Q6 (`eval_spec` layer) classification (S-Eval-2 is multi-layer; primary `eval_spec`).
   - §4.1 nine-question kernel (you self-walk against the S-Eval-2 diff at handoff §6; **Q1 must explicitly cite the §1.7 structural defence test**).
   - §5 / §5.5 / §5.6 acceptance bars.
   - §7 sprint-objective stanza (the contract §8 stanza is already filled).
7. **`docs/runtime_freeze_and_risk_policy.md`** §1 + §2 — verify NO Tier-0 invariant added at S-Eval-2.
8. **Code source files to spot-check at HEAD `357e949`** (read on demand during §3 premise re-verification + §4 implementation, NOT end-to-end):
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (~135 lines post-M2; the record you extend).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` (~59 lines; precedent for `CriticalStep.java` record shape).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (~313 lines; locate `VALID_STATE_KEYS` and `VALID_PROJECTION_SLOTS` allowlists for the precedent; add `VALID_CRITICAL_STEP_FIELDS` per same pattern).
   - `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (1341 lines post-S41; locate the existing `phase_plan.skill` envelope projection construction site — grep for `phase_plan` + `procedure` to find the integration point).
   - 6 Skill YAMLs under `server/src/main/resources/skills/` (verify each loads unchanged AFTER S-Eval-2 SkillLoader extension; none should need `critical_steps:` declaration).
   - `eval_interactive/eval_interactive/scoring/composite.py` (302 lines post-S-Eval-1; locate the S-Eval-1 D-2.5 `severity`-driven advisory-vs-gate wiring; Tier-2 extends the same model).
   - `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (DOES NOT EXIST; S-Eval-2 creates).
   - Existing Python Skill loader (if any) under `eval_interactive/eval_interactive/`: verify at sub-sprint start; if absent, you create a minimum-viable embedded helper in `skill_procedure_check.py` (NOT a separate file).
   - `eval_interactive/tests/test_composite_gate.py` (S-Eval-1 extended; you extend further for Tier-2 wiring).
   - `eval_interactive/eval_interactive/case_spec/schema.py` (256 lines post-S-Eval-1; **READ-ONLY for S-Eval-2**).

## 2. Goal (5 outcomes)

### Outcome 1 — Java Skill schema extension

In `server/src/main/java/com/gumtree/csagent/service/runtime/skill/`:

- **D-1.1** Extend `Skill.java` record with `critical_steps: List<CriticalStep>` field. Default to empty list when absent on YAML. Preserve all existing constructor args + field order; new field is appended.
- **D-1.2** NEW `CriticalStep.java` record with fields: `id: String`, `desc: String`, `traceCheck: String`, `mandatoryFor: List<String>`, `severity: Severity`. Where to put the `Severity` enum: either nested in `CriticalStep` (e.g., `CriticalStep.Severity { MANDATORY, ADVISORY }`) or as a separate `Severity.java` file in the same package. **Pick the option with smallest surface area; document rationale in handoff §3.**

### Outcome 2 — SkillLoader allowlist validation

In `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java`:

- **D-2.1** Add a `VALID_CRITICAL_STEP_FIELDS` allowlist per Sprint 38-fix `VALID_STATE_KEYS` / `VALID_PROJECTION_SLOTS` precedent.
- **D-2.2** Validate each step's required fields: `id` + `desc` + `traceCheck` non-empty; `mandatoryFor` non-empty list; `severity` ∈ {mandatory, advisory}.
- **D-2.3** Validate `mandatoryFor` UC ids against `UseCaseRegistryService` canonical UC set (use existing `applicable_use_cases` UC-validation precedent).
- **D-2.4** DO NOT validate `traceCheck` DSL syntax in Java — the Python DSL parser at Outcome 4 owns DSL syntax validation; Java side just preserves the string verbatim.
- **D-2.5** NO behavioural change in `SkillRegistry.select(...)`, `SkillGuardrailDispatcher.dispatch(...)`, or `SkillStateBus.applyOnSkillSwitch(...)`. Verify these stay UNCHANGED via `git diff`.

### Outcome 3 — ContextProjectionBuilder projection wiring

In `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`:

- **D-3.1** Locate the existing `phase_plan.skill` envelope projection construction site (grep for `phase_plan` + `procedure`).
- **D-3.2** Add `critical_steps:` sibling rendering immediately after `procedure`. Implementation choice: **list-of-strings** (just `desc` values), OR **list-of-objects with `{id, desc}`** — pick based on minimum-projection-bytes + maximum-LLM-comprehensibility. **Document choice in handoff §3 + §5 with rationale.**
- **D-3.3** Empty `critical_steps[]` → NO `critical_steps:` key in projection output. **CRITICAL: parity preservation; existing M2-landed prompt-composition golden tests must continue to pass unchanged.** Verify before commit.

### Outcome 4 — Python SkillProcedureExtractor + minimal DSL

Create `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`:

- **D-4.1** `SkillProcedureExtractor` class with public method `extract(trace, active_skill) -> list[CriticalStepResult]`. Each result: `step_id: str`, `desc: str`, `outcome: Literal["PASS", "FAIL", "N/A"]`, `severity: Literal["mandatory", "advisory"]`.
- **D-4.2** `trace_check` DSL parser supporting **EXACTLY these 6 frozen primitives** (no more):
  - `accumulated_tool_results.<tool>` — presence check.
  - `tool_event_seq(<tool_a>) < tool_event_seq(<tool_b>)` — order check.
  - `intake_state.fields_collected.contains(<field>)` — slot presence.
  - `session.<flag>_present` — flag boolean.
  - `any_of(<expr1>, <expr2>, ...)` — combinator.
  - `all_of(<expr1>, <expr2>, ...)` — combinator.
- **D-4.3** Implementation approach: prefer a **minimal recursive-descent parser** or a tiny PEG library (no full-blown ANTLR / lark dependency unless necessary). **Document choice in handoff §3 + §4.**
- **D-4.4** **§1.7 STRUCTURAL DEFENCE (CRITICAL)**: parser MUST reject by construction any of: regex literal patterns, `.contains(...)` / `.matches(...)` / `.startswith(...)` on string-content fields, `re.match` / `re.search` calls in expression strings, list-membership checks against bot-message or user-message content, keyword-list primitives like `keyword_match(...)`. **Reject AT PARSE TIME** (before evaluation); raise `TraceCheckDSLSyntaxError` with a clear error message naming the rejected primitive (helps S-Eval-3 dev when iterating).
- **D-4.5** Embedded helper: minimum-viable Python-side Skill loader if no existing Python Skill loader is in the repo (verify at sub-sprint start; document choice in handoff §3).

### Outcome 5 — composite Tier-2 wiring

In `eval_interactive/eval_interactive/scoring/composite.py`:

- **D-5.1** Add Tier-2 `skill_procedure_followship` gate band beside the existing Tier-0 / Tier-1 / Tier-3 bands.
- **D-5.2** A `mandatory` step failure for a case whose `active_skill` matches the step's `mandatoryFor` UC set → Tier-2 fail → flips `case_passed`.
- **D-5.3** Empty `critical_steps[]` → Tier-2 = always PASS / N/A (does NOT flip `case_passed`).
- **D-5.4** Wire via the existing `severity` field convention (S-Eval-1 D-2.5): Tier-2 mandatory result carries `severity="critical"`; Tier-2 advisory step results carry `severity="advisory"`.

## 3. Read order specific to deliverables

- For Outcome 1: `Skill.java` + `StateInheritance.java` (record shape precedent) + `Guardrail.java` (record shape precedent).
- For Outcome 2: `SkillLoader.java` (grep for `VALID_STATE_KEYS` and `VALID_PROJECTION_SLOTS` for the allowlist precedent).
- For Outcome 3: `ContextProjectionBuilder.java` (grep for `phase_plan` + `procedure` to find the integration point; the existing `procedure` rendering is the immediate sibling to model).
- For Outcome 4: read 1-2 example traces from `eval_interactive/results/<recent-run>/case_results[].per_turn_trace[]` to understand the trace shape the DSL evaluates against; verify whether an existing Python Skill loader exists under `eval_interactive/eval_interactive/`.
- For Outcome 5: `eval_interactive/eval_interactive/scoring/composite.py` (S-Eval-1 D-2.5 wiring at the gate-vs-advisory band; Tier-2 adds a new band using the same pattern); `eval_interactive/tests/test_composite_gate.py` (S-Eval-1 extended this; you extend further).

## 4. STOP discipline (8 LOAD-BEARING per contract §10)

You STOP and surface to deliver-agent + human (instead of pressing on) if any of:

1. **`trace_check` DSL design proves insufficient** for the anticipated S-Eval-3 use cases (e.g., a step like "consult moderation context before explaining" cannot be expressed in the 6 frozen primitives without resorting to message-content matching). Do NOT pre-emptively widen; surface to deliver-agent + human.
2. **Empty `critical_steps[]` rendering breaks existing prompt-composition golden tests** (the empty-case parity invariant fails). Halt and surface; Outcome 3 implementation choice may need to revisit.
3. **SkillLoader allowlist validation requires touching `SkillRegistry.select(...)` / `SkillGuardrailDispatcher.dispatch(...)` / `SkillStateBus.applyOnSkillSwitch(...)` semantic dispatch.** Hard fence violation; halt and surface.
4. **ContextProjectionBuilder projection wiring requires extending the schema beyond the field-add** (e.g., new container record type for the projection envelope, or modifying the `phase_plan` Java record). Surface; deliver-agent + human decide expand-scope or defer.
5. **Any file under "Files NOT in scope" §6** needs touching to complete S-Eval-2. Hard fence violation; halt and surface; do NOT smuggle scope.
6. **Backward-compat break** on the 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice fixture suite (any case flipping at the composite gate due to S-Eval-2 changes). Halt and surface.
7. **§1.7 structural defence DSL parser cannot reject ≥ 6 representative hardcode-flavoured `trace_check` strings cleanly** — the parser design needs rework. Halt and surface; deliver-agent + human decide extend-parser-design (acceptable; this IS the structural defence) OR scope-shift.
8. **Java baseline regresses** (any new test failure introduced by S-Eval-2 beyond the inherited `SystemPromptUserRequestedTiebreakerTest:53`). Halt and surface; do NOT commit.

## 5. Hard fences (per contract §6; 8 categories)

1. **M2-landed Skill semantics — NO touch** (`SkillRegistry.select(...)` body, `SkillStateBus.applyOnSkillSwitch(...)` body, `SkillGuardrailDispatcher.dispatch(...)` body, all 6 Skill YAMLs' `procedure` / `guardrails` / `state_inheritance` / `applicable_use_cases` / `system_instruction` content).
2. **Runtime semantic surfaces — NO touch (M2 §6 #5 inherits)** (RuntimeIntentClassifier, IntentClassification, DriftResult, DriftDetector, UseCaseRouter, ClassifyUseCaseTool, PhaseEvaluator semantic dispatch, AgentRunLoopImpl, ControlKernel, system_prompt.txt, tool-policy.yaml, IntakeFieldsRegistry, UseCaseRegistryService, ResolveDispositionEvaluator).
3. **Eval case fixtures — NO touch (cascade fence)** (smoke 14 / anchor 159 / anchor_outcome 12 / case_families 12 / Alice bad case 1 / shadow / case_spec_overrides.yaml).
4. **Eval harness / loader / simulator — NO touch** (`eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`).
5. **S-Eval-1-landed schema — NO touch** (`schema.py`, `loader.py`, `hard_checks.py`, `outcome_checks.py`); S-Eval-2 only edits `composite.py` for Tier-2 wiring.
6. **Judge surfaces — NO touch in S-Eval-2** (`llm_judge.py` is S-Eval-5 territory).
7. **Governance and archives — NO touch** (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`, `customer_service_tool_spec_v0_3.md`, `docs/foundational/`, all sprint / milestone archives, live `milestone_objective.md`, live `codex-findings.md`).
8. **Action bank — limited touch** (do not close R-items in S-Eval-2; you MAY append a Sprint 43 close-action index row at sub-sprint close; the new `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close) is unrelated to S-Eval-2 and SHOULD NOT be addressed here).

## 6. §4.1 anti-hardcode self-walk template

Before commit, walk each Q1-Q9 against the S-Eval-2 diff. Write the verdicts in handoff §6:

- **Q1** Semantic hardcode introduced (keyword / regex / if-else / enum / per-UC matrix)? Expected: **pass** — Sprint 43 ships NO `critical_steps` content (no `desc` strings, no `traceCheck` strings populated on any Skill). The DSL parser is the **structural mechanism preventing** hardcodes in S-Eval-3 content — cite `test_skill_procedure_dsl_parser_rejects_hardcodes.py` as evidence (≥ 6 hardcode-flavoured strings rejected at parse time).
- **Q2** Tier-0 invariant protection claimed? Expected: **pass** — no Tier-0 invariant added; Tier-2 `skill_procedure_followship` is eval-side, not Tier-0.
- **Q3** Could a soft signal replace a hard branch? Expected: **pass** — projection wiring DOES surface `critical_steps[].desc` as a soft signal to the LLM (LLM-visible procedural guidance the LLM owns acting on); the eval-side `traceCheck` is the structural check, not a hard branch in code.
- **Q4** Eval phrase / trace-specific phrasing / CaseSpec-id encoded? Expected: **pass** — no eval phrase / CaseSpec id touched. The DSL parser primitives reference TOOL NAMES (e.g., `accumulated_tool_results.get_moderation_review_context`) and SLOT NAMES (e.g., `intake_state.fields_collected.contains(ad_id)`), which are runtime canonical surfaces, not eval-specific.
- **Q5** LLM ownership shrunk (§1.3 surfaces moved to Java)? Expected: **pass** — the LLM's per-turn projection GAINS `critical_steps[].desc` as soft procedural guidance (PRESERVES §1.3 ownership; the LLM owns acting on the guidance, not the Java/Python evaluating it).
- **Q6** Prompt if-else added? Expected: **pass** — no prompt if-else added; `critical_steps[].desc` (when populated in S-Eval-3) is principle-level guidance per the proposal §5.3 standard, not if-else.
- **Q7** Tool schema / capability / PII / grounding floor preserved? Expected: **pass** — no tool schema change; no capability / permission change; no PII handling change; FAQ grounding contract unchanged. Tier-0 safety floor checks all remain critical-severity.
- **Q8** Generalization coverage (target / neighbor / negative / shadow)? Expected: **pass** — per contract §8 stanza.
- **Q9** Rollback / sunset plan if temporary? Expected: **N/A** — S-Eval-2 changes are intended permanent (schema + DSL parser are foundational for the Tier-2 layer).

If any Q1-Q9 returns `concern` or `fail`, STOP and surface to deliver-agent + human before commit.

## 7. Handoff §11 12-section contract

`docs/sprints/sprint-043-handoff.md` must include:

- **§1** Sprint identity (Sprint 43 / S-Eval-2 / M3-Eval sub-sprint 2).
- **§2** Summary (one paragraph).
- **§3** Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`). Includes implementation-choice rationale: (a) Outcome 1 `Severity` enum location (nested in `CriticalStep` vs separate file); (b) Outcome 3 projection rendering (list-of-strings vs list-of-objects); (c) Outcome 4 DSL parser implementation (recursive-descent vs PEG library); (d) ctor-update strategy for existing Java tests (positional vs optional constructor arg) per Sprint 41 OQ-S41.3 / Drift §7-a precedent; (e) Python-side Skill loader strategy (existing vs minimum-viable embedded helper).
- **§4** Schema extension details (which fields added to `Skill`; `CriticalStep` record shape; SkillLoader allowlist entries; `Severity` enum range).
- **§5** Projection wiring details (where in `ContextProjectionBuilder` the rendering is added; empty-array parity preservation verification; per-turn projection byte delta observation).
- **§6** §4.1 anti-hardcode self-walk verdicts (Q1-Q9 with one-line justification; **Q1 MUST explicitly cite `test_skill_procedure_dsl_parser_rejects_hardcodes.py`** as the §1.7 structural defence).
- **§7** Open questions (OQ-S43.N format; non-blocking decisions for deliver-agent + human at close; surface DSL extension requests from anticipated S-Eval-3 needs if any).
- **§8** Generalization coverage filled (target / neighbor / negative / shadow counts per §8 stanza).
- **§9** Validation runs:
  - Java test suite (`mvn test`): new baseline vs pre-S-Eval-2 baseline `1144 / 1-inherited / 0 / 2`; explicit count of S-Eval-2-added tests (~30-50 expected); confirm inherited `SystemPromptUserRequestedTiebreakerTest:53` unchanged.
  - Python test suite (`uv run pytest`): new baseline vs pre-S-Eval-2 baseline `320 passed / 9 pre-existing failed`; explicit count of S-Eval-2-added tests (~15-25 expected); confirm 9 pre-existing failures unchanged.
  - Backward-compat run: load 6 Skill YAMLs + 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice; assert no schema-side / composite-gate regression.
  - Projection token-cost observation: per-turn projection byte count for a representative trace before vs after (expected 0 delta for empty `critical_steps[]`).
- **§10** Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention).
- **§11** Bundle policy honored (dev shipped one commit; deliver-agent close-out files NOT staged).
- **§12** Closure verdict (LEFT EMPTY by dev per `feedback_handoff_verdict_section_delegation.md`).

## 8. Bundle policy

Dev ships **ONE bundle commit** containing:

- Java: `Skill.java`, NEW `CriticalStep.java` (+ optional separate `Severity.java`), `SkillLoader.java`, `ContextProjectionBuilder.java`, optionally `SkillRegistry.java`.
- Python: NEW `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`, `eval_interactive/eval_interactive/scoring/composite.py`.
- Java tests: NEW `SkillCriticalStepsLoadingTest.java`, NEW `CriticalStepsProjectionTest.java`, (0-30) ctor-update existing test files per dev strategy choice.
- Python tests: NEW `test_skill_procedure_extractor.py`, NEW `test_skill_procedure_dsl_parser_rejects_hardcodes.py`, EDIT `test_composite_gate.py`.
- `docs/sprints/sprint-043-handoff.md` (dev-authored).

Expected: 12-20 files in the bundle.

Dev does NOT stage:

- `docs/sprint_objective.md` archive copy (deliver-agent territory at close).
- `docs/milestone_objective.md` updates (deliver-agent territory).
- `docs/10-handoff.md` §1 lead refresh (deliver-agent territory).
- `docs/codex-findings.md` reset (review-agent + deliver-agent territory).
- `docs/action_bank.md` Sprint 43 close-action row (you MAY append at close; deliver-agent + human may also do this at close-out bundle).

Suggested commit message:

```
sprint 43 / S-Eval-2: Skill critical_steps schema + extractor + projection wiring (NEW M3-Eval sub-sprint 2)
```

## 9. Self-check before commit

Run these mental checks against your diff before staging:

1. **Hard fence audit**: walk §5 of this prompt; for each "NO touch" item, run `git diff <path>` and confirm empty.
2. **Numbers-cite discipline**: every count in handoff §3 / §9 must be reproducible. Use `git show --numstat <staged-commit>` (or `git diff --numstat`) for per-file line counts; `mvn test 2>&1 | tail -5` for the Java baseline; `uv run pytest 2>&1 | tail -3` for the Python baseline.
3. **§4.1 self-walk**: Q1-Q9 verdicts written in handoff §6 (none `concern` / `fail`); **Q1 explicitly cites `test_skill_procedure_dsl_parser_rejects_hardcodes.py`**.
4. **Backward-compat smoke**: load 6 Skill YAMLs + 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice; assert no schema-side or composite-gate regression.
5. **§1.7 structural defence test PASSES**: `test_skill_procedure_dsl_parser_rejects_hardcodes.py` asserts ≥ 6 hardcode-flavoured `trace_check` strings fail parse with `TraceCheckDSLSyntaxError`. **This is the primary milestone-level §1.7 protection** — verify it passes BEFORE commit.
6. **Empty-array parity**: existing M2-landed prompt-composition golden tests pass unchanged (empty `critical_steps[]` → no `critical_steps:` key in projection output → no projection byte delta).
7. **Implementation-choice rationale captured**: 5 choice points (a)-(e) per handoff §3 documented with one-paragraph rationale each.
8. **No-mocked-LLM-as-proof**: if any handoff claim relies on LLM behaviour, cite a real-LLM source — but S-Eval-2 should not need any LLM-behaviour claim; this is structural / schema / wiring / parser work.
9. **Bundle policy**: `git status` shows ONLY files listed in §8; nothing under `docs/milestones/`, `docs/sprints/sprint-001-*..sprint-042-*`, no live `docs/milestone_objective.md` edit, no live `docs/codex-findings.md` edit, no live `docs/action_bank.md` edit beyond optional close-action row.
10. **Stop-conditions review**: any of the 8 STOP signals in §4 fired? If yes, STOP and surface; do NOT commit.

After commit, surface to deliver-agent + human:

- Commit SHA.
- Java baseline result (new total / failures / errors / skipped; explicit count of S-Eval-2-added tests).
- Python test pass status (new total passed / pre-existing failures preserved).
- Backward-compat sample-run result (6 Skills + 5 fixture buckets all clean).
- Projection token-cost observation (expected 0-byte delta for empty `critical_steps[]`).
- §1.7 structural defence test result (must PASS; explicit count of rejected hardcode-flavoured `trace_check` strings).
- 5 implementation-choice rationales (a)-(e) per handoff §3.
- Open questions OQ-S43.N list (especially any DSL extension requests anticipated for S-Eval-3).
- Any close-readiness notes (your guess at A / A-with-X / B classification — deliver-agent + human make the actual call at close).
