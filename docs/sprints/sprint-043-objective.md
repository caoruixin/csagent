---
title: Sprint 43 objective archive — Skill `critical_steps` schema + extractor + projection wiring (M3-Eval sub-sprint 2; S-Eval-2)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-21
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-042-objective.md]
superseded_by: null
archive_notes: >
  Archived Sprint 43 contract at sub-sprint close 2026-05-21 (Sprint 43
  closed A — Clean PASS; Codex deferred to M3-Eval milestone close per
  §4.3 default; dev commit `69ed77f`; archive package: this file +
  `docs/sprints/sprint-043-handoff.md`). All 5 §2 outcomes shipped per
  §5 (Files in scope); §6 (Files NOT in scope) honoured; no §10 stop
  signal fired; §1.7 structural defence (DSL parser) over-delivered
  (15 hardcode-flavoured strings rejected vs the §9 ≥6 floor — 2.5×
  over). 5 OQs disposed non-blocking per handoff §12.4 (S43.1 + S43.4
  routed to M3-Eval-shared Codex review; S43.2 + S43.3 + S43.5 deferred
  to S-Eval-3 author feedback or fix-iteration). No new R-items
  surfaced at close. See handoff §12 for full closure verdict.
notes: >
  Sprint 43 is the SECOND sub-sprint of NEW Milestone M3-Eval (Coarse-to-
  Fine Evaluation Architecture; see `docs/milestone_objective.md`).
  S-Eval-1 (Sprint 42, commit `d91bd3d` + close-out `357e949`) closed
  A — Clean PASS 2026-05-21; foundation in place (schema demotions +
  scoring demotions + anchor_outcome suite + `severity` field on
  HardCheckResult / OutcomeCheckResult).

  S-Eval-2 is the **schema + contract sub-sprint**: it extends the
  Skill YAML schema with the NEW `critical_steps` field, lands a
  matching `CriticalStep.java` record + SkillLoader allowlist
  validation, wires `ContextProjectionBuilder` to render
  `critical_steps[].desc` inside `phase_plan.skill`, and creates the
  Python `SkillProcedureExtractor` with the **minimal `trace_check`
  DSL** that is **structurally constrained** to reject regex / keyword /
  message-content matching (the milestone's primary structural defence
  against §1.7 violations on the eval side). Sprint 43 ships **NO
  `critical_steps` content** — only the contract + extractor + projection
  wiring. Content lands in S-Eval-3 (which has per-sub-sprint Codex
  review per §4.3 trigger #2).

  **Multi-layer scope (Java + Python)**: this is a bigger sub-sprint
  than S-Eval-1's pure-Python schema work. Layer per `iteration_governance.md`
  §3.2: `eval_spec` (primary; extractor + DSL parser + composite Tier-2
  wiring) + `prompt_projection` (auxiliary; LLM-visible projection slot
  for `critical_steps[].desc`). §7 stanza names `eval_spec` as primary.

  **Codex review plan**: milestone-shared at M3-Eval close (per §4.3
  default; no per-sub-sprint trigger for S-Eval-2). Only S-Eval-3 fires
  per-sub-sprint Codex per §4.3 trigger #2 (LLM-visible content adjacent
  to §1.7 forbidden-list); S-Eval-2 is contract + structural defence
  only, no content to review.

  **S-Eval-1 close carry-over (`docs/sprints/sprint-042-handoff.md`
  §12.7)**: per-UC anchor distribution (UC-G/H/I/J zero anchor;
  UC-F=1; UC-B=5) is contextual ONLY for S-Eval-2 (no `critical_steps`
  content lands here). The gap becomes load-bearing at S-Eval-3
  planning (Skill `critical_steps` authoring) and S-Eval-4 planning
  (bad-case selection from 17 approved overrides). For S-Eval-2, dev
  should be aware but does NOT take action; the gap does not affect
  schema / extractor / projection design.

  **S-Eval-1 open question OQ-S42.3 (D-2.2 demotion interpretation)**:
  routed to M3-Eval milestone-shared Codex review (`compact/M3-Eval-review-prompt.md`
  drafted at M3-Eval close); NOT a blocker for S-Eval-2 start. Dev does
  NOT need to address OQ-S42.3 in S-Eval-2.

  **Bundle policy**: multi-layer sub-sprint shipped in ONE bundle commit.
  Dev does NOT stage deliver-agent close-out files (sprint_objective
  archive, milestone_objective edits, 10-handoff §1 lead refresh,
  codex-findings reset, action_bank Sprint 43 row) per
  `feedback_commit_at_end_bundles_deliver_artefacts.md`.

  **Cross-session continuity**: if a new dev-agent picks this up cold,
  the dev prompt at `compact/sprint-043-dev-prompt.md` is the entry
  point. Read order: AGENTS.md (auto-loaded) → this file →
  docs/milestone_objective.md (M3-Eval context, §3 S-Eval-2 row) →
  proposal §6 S-Eval-2 (scope detail; reconcile with this contract) →
  docs/sprints/sprint-042-handoff.md (S-Eval-1 close artefacts;
  §12.7 carry-over).
---

# Sprint 43 (NEW M3-Eval sub-sprint 2, S-Eval-2) — Skill `critical_steps` schema + extractor + projection wiring

## 1. Sub-sprint class

**Multi-layer semantic-touching sub-sprint, `eval_spec` (primary) + `prompt_projection` (auxiliary) per `iteration_governance.md` §3.2 Q6 + Q3.** §7 stanza REQUIRED. Codex review default (milestone-shared at M3-Eval close).

Class breakdown:

- **Primary layer**: `eval_spec` — Skill YAML schema extension; `SkillLoader` allowlist validation; NEW `SkillProcedureExtractor` Python class; minimal `trace_check` DSL parser; Tier-2 `skill_procedure_followship` composite wiring.
- **Auxiliary layer**: `prompt_projection` — `ContextProjectionBuilder` extension to render `critical_steps[].desc` inside `phase_plan.skill` as LLM-visible projection slot. Empty `critical_steps[]` → no projection change (parity with M2 envelope behaviour).
- **Tier-0 invariant claim**: no new Tier-0 invariant; this sub-sprint adds a NEW eval-side check (Tier-2 `skill_procedure_followship`) but the existing Tier-0 safety floor surfaces are untouched.
- **§1.7 forbidden-list adjacency**: **yes — primary structural concern of this sub-sprint**. The `trace_check` DSL parser is the structural defence against §1.7 violations in S-Eval-3 content. Parser MUST reject regex / keyword-list / message-content matching by construction (negative test required — see §5 / §9). S-Eval-2 itself does not add any semantic hardcode (no content lands here); the parser constraint is what prevents S-Eval-3 from accidentally introducing one.

## 2. Goal

Land the **schema + contract + structural defence** for the M3-Eval four-tier pyramid's Tier-2 Critical-flow layer:

- **Outcome 1** (Java Skill schema): extend `Skill.java` + NEW `CriticalStep.java` record (under `server/src/main/java/com/gumtree/csagent/service/runtime/skill/`) to carry optional `critical_steps: list[CriticalStep]`. `CriticalStep` carries: `id: String`, `desc: String` (LLM-visible procedural narrative), `traceCheck: String` (DSL expression as a string), `mandatoryFor: List<String>` (UC list; canonical UC ids), `severity: Severity` (enum `mandatory | advisory`). Default empty list when absent on YAML.
- **Outcome 2** (SkillLoader): extend `SkillLoader.java` allowlist validation (per Sprint 38-fix `VALID_STATE_KEYS` / `VALID_PROJECTION_SLOTS` precedent) to validate the NEW `critical_steps` block + each step's required fields + enum range on `severity` + UC range on `mandatoryFor`. NO behavioural change in `SkillRegistry.select(...)` / `SkillGuardrailDispatcher.dispatch(...)` / `SkillStateBus.applyOnSkillSwitch(...)`. Existing 6 Skill YAMLs continue to load unchanged (empty `critical_steps`).
- **Outcome 3** (ContextProjectionBuilder): extend `ContextProjectionBuilder.java` to render `critical_steps[].desc` inside `phase_plan.skill` per-turn projection, immediately after the existing `procedure` field. Empty `critical_steps[]` → no projection change (parity preservation; existing prompt-composition tests must continue to pass). Implementation detail: list-of-strings under a `critical_steps:` key, OR a list-of-objects with `{id, desc}` — dev chooses based on minimum-projection-bytes; document choice in handoff §3.
- **Outcome 4** (Python SkillProcedureExtractor + DSL): create `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` with a `SkillProcedureExtractor` class. Accepts (a) a trace (`case_results[].per_turn_trace[]` or equivalent) + (b) the active Skill (loaded from YAML via existing eval-side Skill loader, OR via a NEW thin Python Skill-loader pointer if none exists). Returns per-step `PASS / FAIL / N/A`. **Minimal `trace_check` DSL** primitives (FROZEN scope; no extensions in S-Eval-2): `accumulated_tool_results.<tool>` (presence check), `tool_event_seq(<tool_a>) < tool_event_seq(<tool_b>)` (order check), `intake_state.fields_collected.contains(<field>)` (slot presence), `session.<flag>_present` (flag boolean), `any_of(...)`, `all_of(...)`. **Reject by parser construction** any regex / keyword-list / message-content matching — that would constitute a §1.7 semantic hardcode. Parser MUST reject `message.contains(...)`, `re.match(...)`, `text in [...]`, or any string-matching primitive against user / bot message content (verified by negative test, see §5).
- **Outcome 5** (composite Tier-2 wiring): add Tier-2 `skill_procedure_followship` check that integrates the extractor result into composite scoring. A `mandatory` step failure for a case whose `active_skill` matches the step's `mandatoryFor` UC set → Tier-2 fail → flips `case_passed`. Empty `critical_steps[]` → always PASS / N/A. Wire via the existing `severity` field (S-Eval-1 D-2.5): Tier-2 results are `severity="critical"` by default; Tier-3 polish dims (S-Eval-1 demoted) stay `severity="advisory"`. Update `composite.py` to read Tier-2 as a new gate-contributing band beside the existing Tier-0 / Tier-1 / Tier-3.

**Backward-compat invariant**: existing 6 Skill YAMLs load unchanged (no `critical_steps:` declaration needed; loader default = empty list); existing 14 smoke + 159 anchor + 12 case-family + 1 Alice bad case + 12 anchor_outcome fixtures all run unchanged at the composite-gate level (empty `critical_steps` → Tier-2 always PASS / N/A → no `case_passed` flip).

## 3. Non-goals (explicit)

S-Eval-2 does NOT:

1. **Fill any `critical_steps` content** in any Skill YAML. (Deferred to S-Eval-3 per `docs/milestone_objective.md` §3; S-Eval-3 carries per-sub-sprint Codex per §4.3 trigger #2.)
2. **Modify any existing Skill `procedure` text** or any other M2-landed Skill field (`guardrails`, `state_inheritance`, `applicable_use_cases`, `system_instruction`).
3. **Touch `SkillRegistry.select(...)` or `SkillGuardrailDispatcher.dispatch(...)` or `SkillStateBus.applyOnSkillSwitch(...)` semantic dispatch** beyond field exposure (loader-side allowlist validation + projection-side rendering only).
4. **Add a runtime if-else / regex / keyword matrix / per-UC enumeration anywhere.** §1.7 red line. The structural defence (Outcome 4 DSL parser construction) is what enforces this for S-Eval-3.
5. **Extend the `trace_check` DSL beyond the 6 frozen primitives** (`accumulated_tool_results.<tool>` / `tool_event_seq(...) < tool_event_seq(...)` / `intake_state.fields_collected.contains(...)` / `session.<flag>_present` / `any_of(...)` / `all_of(...)`). If S-Eval-3 planning surfaces a need for a new primitive, deliver-agent + human decide whether to extend within S-Eval-3's Codex review boundary or scope-shift; S-Eval-2 itself does not pre-emptively widen.
6. **Touch the eval harness / loader / simulator** (`eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`).
7. **Touch any case fixture** under `eval_interactive/case_specs/smoke/`, `eval_interactive/case_specs/anchor/`, `eval_interactive/case_specs/anchor_outcome/`, `eval_interactive/case_specs/case_families/<existing>/`, `eval_interactive/case_specs/bad_cases/`, or `eval_interactive/case_specs_shadow/`.
8. **Touch `eval_interactive/case_spec_overrides.yaml`** (S-Eval-4 read-only source).
9. **Touch the L3 judge dim or rubric** (`llm_judge.py` is S-Eval-5 territory).
10. **Modify the `severity` enum on `HardCheckResult` / `OutcomeCheckResult`** beyond the existing `critical | advisory` set (S-Eval-1 D-2.5 introduced; S-Eval-2 consumes but does not extend).
11. **Add new Tier-0 invariants.** (Milestone-level §6 hard fence + §1.7 forbidden-list adjacency precludes silently elevating the Tier-2 check to Tier-0.)
12. **Touch runtime semantic surfaces**: `RuntimeIntentClassifier`, `IntentClassification`, `DriftResult`, `DriftDetector`, `UseCaseRouter`, `ClassifyUseCaseTool`, `PhaseEvaluator` semantic dispatch (M2 §6 #5 hard fence inherits).
13. **Touch governance docs** (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`), foundational docs (`docs/foundational/`), or sprint / milestone archives (`docs/sprints/sprint-001-*` through `docs/sprints/sprint-042-*`, `docs/milestones/M*`).
14. **Touch `docs/codex-findings.md`** (review-agent territory; remains scaffold).
15. **Address S-Eval-1 OQ-S42.3** (D-2.2 demotion interpretation; routed to M3-Eval milestone-shared Codex review per S-Eval-1 close §12.4).

## 4. Premise check (deliver-agent verified 2026-05-21)

- ✅ `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` exists at HEAD (~6437 bytes / ~135 lines post-M2 close); record carries the M2-landed fields (`name`, `applicable_use_cases`, `system_instruction`, `procedure`, `guardrails`, `state_inheritance`).
- ✅ `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` exists (~13764 bytes / ~313 lines); carries `VALID_STATE_KEYS` + `VALID_PROJECTION_SLOTS` allowlists per Sprint 38-fix precedent; new `critical_steps` block validation extends this same convention.
- ✅ `server/src/main/java/com/gumtree/csagent/service/runtime/skill/StateInheritance.java` exists as a record-type precedent for `CriticalStep.java` shape.
- ✅ `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` exists at HEAD (1341 lines post-S41); existing `phase_plan.skill` envelope projection is the integration point for the NEW `critical_steps[].desc` rendering.
- ✅ `server/src/main/resources/skills/` carries all 6 Skill YAMLs (`discover_triage.yaml`, `confirm.yaml`, `resolve_faq_grounded_answer.yaml`, `resolve_intake_collect_and_handover.yaml`, `escalate.yaml`, `terminal.yaml`); none carry `critical_steps:` blocks yet (S-Eval-3 fills).
- ✅ `eval_interactive/eval_interactive/scoring/composite.py` exists at HEAD (302 lines post-S-Eval-1); carries S-Eval-1 D-2.5 advisory-vs-gate wiring per `severity` field on results. NEW Tier-2 wiring extends the same field-driven model.
- ✅ `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` does NOT exist yet (S-Eval-2 creates).
- ✅ `CriticalStep.java` does NOT exist yet (S-Eval-2 creates).
- ✅ Python eval-side Skill loader: dev to verify at sub-sprint start whether an existing Python-side Skill loader exists (would be at `eval_interactive/eval_interactive/...`) OR whether the extractor reads the YAMLs directly via a thin pyYAML pointer. If neither exists, dev creates the minimum-viable Python-side Skill loader as part of `skill_procedure_check.py` (NOT a separate file; embedded helper).

## 5. Files in scope (Sprint 43 dev ships)

**Java (5 files):**

- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (record extension):
  - Add `critical_steps: List<CriticalStep>` field; default to empty list when absent on YAML.
  - Backward-compat: preserve all existing constructor args + field order; new field is appended.
- NEW `server/src/main/java/com/gumtree/csagent/service/runtime/skill/CriticalStep.java` (Java record):
  - Fields: `id: String`, `desc: String`, `traceCheck: String`, `mandatoryFor: List<String>`, `severity: Severity`.
  - NEW nested enum `CriticalStep.Severity { MANDATORY, ADVISORY }` OR separate `Severity.java` record (dev chooses; document in handoff §3).
- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (allowlist extension):
  - Add a `VALID_CRITICAL_STEP_FIELDS` allowlist per Sprint 38-fix precedent.
  - Validate each step's required fields (`id` + `desc` + `traceCheck` non-empty; `mandatoryFor` non-empty list; `severity` ∈ {mandatory, advisory}).
  - Validate `mandatoryFor` UC ids against `UseCaseRegistryService` canonical UC set (per existing `applicable_use_cases` precedent).
  - DO NOT validate `traceCheck` DSL syntax in Java (the Python DSL parser at Outcome 4 owns DSL syntax validation; Java side just preserves the string).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (projection wiring):
  - In the existing `phase_plan.skill` envelope projection construction site (locate via grep on `phase_plan` + `procedure`), add `critical_steps:` sibling rendering immediately after `procedure`.
  - Render each step's `desc` as a list entry. Implementation choice: list-of-strings (just `desc` values), OR list-of-objects with `{id, desc}` — dev picks based on minimum-projection-bytes + maximum-LLM-comprehensibility; document choice in handoff §3.
  - Empty `critical_steps[]` → no `critical_steps:` key in projection output (parity preservation; existing prompt-composition tests must continue to pass).
- (Optional) `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` IF the SkillRegistry exposes a getter that must be extended for the new field; otherwise UNCHANGED.

**Python (2 files):**

- NEW `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`:
  - `SkillProcedureExtractor` class with public method `extract(trace, active_skill) -> list[CriticalStepResult]` where each result carries `step_id`, `desc`, `outcome: Literal["PASS", "FAIL", "N/A"]`, `severity: Literal["mandatory", "advisory"]`.
  - `trace_check` DSL parser supporting EXACTLY the 6 frozen primitives (Outcome 4 §). Implementation: prefer a minimal recursive-descent parser or a tiny PEG library (no full-blown ANTLR / lark dependency unless necessary; document choice in handoff §3).
  - **§1.7 structural defence**: parser MUST reject any of: regex literal patterns, `.contains(...)` / `.matches(...)` / `.startswith(...)` on string-content fields, `re.match` / `re.search` calls in expression strings, list-membership checks against bot-message or user-message content. Reject AT PARSE TIME (before evaluation); raise a `TraceCheckDSLSyntaxError` with a clear error message naming the rejected primitive.
  - Embedded helper (or top-level helper function): minimum-viable Python-side Skill loader if no existing Python Skill loader is in the repo (verify at sub-sprint start; document in handoff §3).
- `eval_interactive/eval_interactive/scoring/composite.py`:
  - Add Tier-2 `skill_procedure_followship` gate band beside the existing Tier-0 / Tier-1 / Tier-3 bands.
  - A `mandatory` step failure for a case whose `active_skill` matches the step's `mandatoryFor` UC set → Tier-2 fail → flips `case_passed`.
  - Empty `critical_steps[]` → Tier-2 = always PASS / N/A (does NOT flip `case_passed`).
  - Wire via the existing `severity` field convention (S-Eval-1 D-2.5): Tier-2 mandatory result carries `severity="critical"`; Tier-2 advisory step results carry `severity="advisory"`.

**Java regression tests (3-4 files):**

- NEW `server/src/test/java/com/gumtree/csagent/service/runtime/skill/SkillCriticalStepsLoadingTest.java`:
  - Load each of the 6 Skill YAMLs unchanged; assert `critical_steps` field is empty list (default).
  - Load a synthetic test Skill YAML with a valid `critical_steps:` block; assert all 5 fields parsed correctly + `severity` enum range.
  - Load a synthetic test Skill YAML with INVALID fields (missing `id`, empty `desc`, unknown UC in `mandatoryFor`, invalid `severity`); assert loader throws clear validation error per the allowlist convention.
- NEW `server/src/test/java/com/gumtree/csagent/service/runtime/CriticalStepsProjectionTest.java`:
  - Build a `Skill` with empty `critical_steps[]`; assert projection has NO `critical_steps:` key (parity with current behaviour).
  - Build a `Skill` with 2-3 `critical_steps`; assert projection contains `critical_steps:` sibling after `procedure` with the expected rendered structure (per Outcome 3 implementation choice).
  - Assert `desc` value is verbatim from the input (no transformation; LLM-visible as-written).
- EDIT (~20-30 existing Java test files) with ctor-signature update if `Skill` record gains a new field via positional constructor; OR — preferable — make `critical_steps` constructor arg optional (default empty list) so most existing tests don't need ctor-update. Dev picks; document in handoff §3 per Sprint 41 OQ-S41.3 / Drift §7-a precedent.

**Python regression tests (2-3 files):**

- NEW `eval_interactive/tests/test_skill_procedure_extractor.py`:
  - Happy path: trace with `accumulated_tool_results.get_moderation_review_context` present + Skill with `critical_step.trace_check = "accumulated_tool_results.get_moderation_review_context"` → PASS.
  - `tool_event_seq` ordering: trace with `create_case_controlled` BEFORE `request_handover` + Skill with `trace_check = "tool_event_seq(create_case_controlled) < tool_event_seq(request_handover)"` → PASS; reversed order → FAIL.
  - `intake_state.fields_collected.contains(...)` happy + miss paths.
  - `any_of(...)` / `all_of(...)` combinator tests (at least 2 nesting levels deep).
  - **N/A path**: a case whose `active_skill` does NOT match the step's `mandatoryFor` UC set → step outcome N/A.
- NEW `eval_interactive/tests/test_skill_procedure_dsl_parser_rejects_hardcodes.py`:
  - **CRITICAL (§1.7 structural defence)**: parser MUST reject each of these `trace_check` strings at parse time, raising `TraceCheckDSLSyntaxError`:
    - `"message.contains('refund')"`
    - `"user_message.contains('appeal')"`
    - `"bot_message.matches(r'.*sorry.*')"`
    - `"re.search(r'\\bpayment\\b', user_message)"`
    - `"user_intent in ['UC-A', 'UC-FP']"` (membership against UC strings derived from message; rejected because UC-string-extraction-from-message is a regex/keyword surface; UC-membership via `active_skill.applicable_use_cases.contains(...)` is acceptable if dev judges that does not violate §1.7 — but recommend rejecting and letting `mandatory_for` enforce UC scope).
    - `"keyword_match('refund', user_message)"`.
  - Each rejection should carry a clear error message naming the rejected primitive (helps S-Eval-3 dev when iterating).
- EDIT `eval_interactive/tests/test_composite_gate.py` for new Tier-2 wiring:
  - `_StubCaseSpec` carries `active_skill` (or equivalent) + the extractor result list.
  - Test: a `mandatory` failure flips `case_passed`.
  - Test: an `advisory` failure does NOT flip `case_passed`.
  - Test: empty `critical_steps[]` → Tier-2 PASS / N/A, no gate flip.
  - Test: a step that does NOT match the case's active_skill UC → N/A, no gate flip.

**Handoff document:**

- `docs/sprints/sprint-043-handoff.md` (dev-authored at sub-sprint close); follows 12-section template per Sprint 35 / 41 / 42 precedent.

## 6. Files NOT in scope (hard fences)

**1. M2-landed Skill semantics — NO touch:**

- `SkillRegistry.select(...)` body (UNCHANGED; only optional getter extension if needed for ContextProjectionBuilder wiring).
- `SkillStateBus.applyOnSkillSwitch(...)` body / `SkillStateBus.inheritanceFor(...)` body (UNCHANGED).
- `SkillGuardrailDispatcher.dispatch(...)` body / `handleFaqMissHandoverRequiresResolveAttempt(...)` / `handleIntakeCompleteRequired(...)` / `handlePrematureResolveOutcomeGuard(...)` / `handleMustCiteSource(...)` (UNCHANGED).
- `Skill.procedure` text / `Skill.guardrails` content / `Skill.state_inheritance` content / `Skill.applicable_use_cases` / `Skill.system_instruction` on any of the 6 YAMLs (UNCHANGED — S-Eval-2 ships NO content).

**2. Runtime semantic surfaces — NO touch (M2 §6 #5 hard-fence inherits):**

- `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`.
- `PhaseEvaluator.java` semantic dispatch (`plan(...)` / `composeSkillPhasePlan(...)` / `substitutePlaceholders(...)` / `maybeApplyStateBusOnSwitch(...)`).
- `AgentRunLoopImpl.java` / `ControlKernel.java`.
- `system_prompt.txt` / `server/src/main/resources/config/tool-policy.yaml`.
- `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java` (M2 frozen surfaces).

**3. Eval case fixtures — NO touch (cascade fence):**

- Any case under `eval_interactive/case_specs/smoke/` (14 cases).
- Any case under `eval_interactive/case_specs/anchor/` (159 cases).
- Any case under `eval_interactive/case_specs/anchor_outcome/` (12 cases; S-Eval-1 landed).
- Any case under `eval_interactive/case_specs/case_families/<existing>/` (12 directories).
- The existing Alice bad case `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`.
- `eval_interactive/case_specs/bad_cases/_manifest.md` (S-Eval-4 territory).
- Shadow CaseSpecs under `eval_interactive/case_specs_shadow/` (per `_ACCESS_BOUNDARY.md`).
- `eval_interactive/case_spec_overrides.yaml` (S-Eval-4 read-only source).

**4. Eval harness / loader / simulator — NO touch:**

- `eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/` (stable per Sprint 28 / Sprint 32 precedent).

**5. Schema (S-Eval-1 just landed) — limited touch:**

- `eval_interactive/eval_interactive/case_spec/schema.py`: NO touch. S-Eval-1 just landed the demotions; S-Eval-2 does not extend the CaseSpec schema (Tier-2 wiring rides on the active_skill + extractor result, NOT on a CaseSpec field).
- `eval_interactive/eval_interactive/case_spec/loader.py`: NO touch.
- `eval_interactive/eval_interactive/scoring/hard_checks.py` / `outcome_checks.py`: NO touch (S-Eval-1 just demoted dims; S-Eval-2 only adds Tier-2 via composite.py).
- `eval_interactive/eval_interactive/scoring/llm_judge.py`: NO touch (S-Eval-5 territory).

**6. Governance and archives — NO touch:**

- `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/customer_service_tool_spec_v0_3.md`.
- `docs/current/iteration_governance.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/faq_grounding_contract.md`.
- `docs/sprints/sprint-001-*` through `docs/sprints/sprint-042-*`.
- `docs/milestones/M1_objective.md`, `M2_objective.md`, `M2-Skill_objective.md`, `M2_codex-review.md`.
- `docs/milestone_objective.md` (M3-Eval; deliver-agent territory).
- `docs/codex-findings.md` (review-agent territory; scaffold at S-Eval-2 start).

**7. Action bank — limited touch:**

- `docs/action_bank.md`: dev SHOULD NOT close R-items in S-Eval-2 (R-item closures happen at S-Eval-5 close per `docs/milestone_objective.md` §7). Dev MAY append a Sprint 43 close-action index row in §6 close-action index at sub-sprint close per existing convention.
- The new R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close; pending) is NOT touched by S-Eval-2.

**8. Sprint 41 OQ-S41.1 design-doc fold-back (M2 close queue)**: NOT touched by S-Eval-2; routed to a SEPARATE governance commit per `doc_governance.md` cadence (deliver-agent + human discretion).

## 7. Bundle policy

S-Eval-2 dev ships in **ONE bundle commit** containing:

- 4-5 Java files: `Skill.java`, NEW `CriticalStep.java`, `SkillLoader.java`, `ContextProjectionBuilder.java`, optionally `SkillRegistry.java`.
- 2 Python files: NEW `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`, `eval_interactive/eval_interactive/scoring/composite.py`.
- 3-4 new Java test files + (0-30) ctor-update existing test files (dev picks ctor strategy per §5).
- 2-3 new Python test files + 1 edited existing Python test (`test_composite_gate.py`).
- Dev handoff `docs/sprints/sprint-043-handoff.md`.

Expected file count: 12-20 files in the bundle. Lower than Sprint 41's 46 (Sprint 41 was the multi-fence M2 LAST sub-sprint); higher than S-Eval-1's 22 (S-Eval-1 was pure-Python schema work).

Deliver-agent + human bundle (separately, post-close):

- This sub-sprint contract archive (`docs/sprints/sprint-043-objective.md`).
- Live `docs/sprint_objective.md` replaced with S-Eval-3 contract (when S-Eval-3 planning round completes).
- `docs/10-handoff.md` §1 lead refresh (demote S-Eval-2 to Preceding sub-sprint; set S-Eval-3 as Current).
- `docs/action_bank.md` Sprint 43 close-action index row (if dev did not already append).

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent close-out files; human bundles at deliver-agent's commit.

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `eval_spec` (primary) + `prompt_projection` (auxiliary).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The Tier-2 `skill_procedure_followship` check is added at the composite-scoring layer (eval-side), not the runtime layer; it gates `case_passed` but is NOT a Tier-0 runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2.

**Semantic hardcode:** No semantic hardcode introduced. Sprint 43 ships **contract + projection wiring + extractor + structural defence only**; NO `critical_steps` content lands until S-Eval-3. The **`trace_check` DSL is structurally constrained** to NOT permit regex / keyword-list / message-content matching, enforced by the DSL parser at parse time (Outcome 4 §1.7 structural defence). A NEGATIVE test (`test_skill_procedure_dsl_parser_rejects_hardcodes.py`) asserts that ≥ 6 representative hardcode-flavoured `trace_check` strings fail parse with a clear error message — this is the primary §1.7 protection mechanism for the entire M3-Eval Tier-2 surface, not just for S-Eval-2 itself.

**Generalization coverage:**

- **Target**: NEW `Skill.critical_steps` field + `CriticalStep` record + SkillLoader allowlist validation + ContextProjectionBuilder projection wiring + NEW `SkillProcedureExtractor` Python class + 6-primitive `trace_check` DSL parser + Tier-2 `skill_procedure_followship` composite gate band.
- **Neighbor**: existing 14 smoke + **159 anchor** + **12 anchor_outcome** (S-Eval-1 landed) + 12 case-family + 1 Alice bad case all load and run unchanged at the composite-gate level (empty `critical_steps[]` → Tier-2 always PASS / N/A → no `case_passed` flip). Existing 6 Skill YAMLs load unchanged via SkillLoader (no `critical_steps:` declaration needed; default = empty list).
- **Negative**: ≥ 6 representative hardcode-flavoured `trace_check` strings MUST fail parse with `TraceCheckDSLSyntaxError` per the §1.7 structural defence test. A synthetic `Skill` YAML with malformed `critical_steps:` (missing fields, unknown UC, invalid severity) MUST fail SkillLoader validation with a clear error message.
- **Shadow**: S-Eval-3 (next sub-sprint after S-Eval-2 close) exercises the schema + extractor with populated `critical_steps` content; S-Eval-4 + S-Eval-5 + M3-Eval close cumulatively. S-Eval-2 does NOT touch shadow CaseSpecs.

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (sub-sprint close PASS only if all clear):**

- [ ] All 6 existing Skill YAMLs load unchanged through the extended SkillLoader. Verified by regression test (Java); SkillLoader does NOT raise on absent `critical_steps:` block.
- [ ] All 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice bad case fixtures continue to run end-to-end with `case_passed` unchanged. Tier-2 wiring contributes only when `critical_steps[]` is non-empty (which is never in S-Eval-2; only S-Eval-3 populates).
- [ ] `case_passed` parity preserved at the composite-gate level: no false flips from S-Eval-1 baseline.
- [ ] `ContextProjectionBuilder` projection for the 6 existing Skill YAMLs is UNCHANGED (empty `critical_steps[]` → no `critical_steps:` key in projection output). Verified by existing prompt-composition tests (must continue to pass — golden tests from M2).
- [ ] **§1.7 structural defence test PASSES**: `test_skill_procedure_dsl_parser_rejects_hardcodes.py` asserts that ≥ 6 representative hardcode-flavoured `trace_check` strings fail parse with `TraceCheckDSLSyntaxError`. This test is the milestone's primary structural defence against S-Eval-3 introducing §1.7 violations.
- [ ] SkillLoader negative tests PASS: synthetic Skill YAML with malformed `critical_steps:` (missing field, unknown UC, invalid severity) fails loader validation with clear error.
- [ ] `SkillProcedureExtractor` happy-path tests PASS: each of the 6 frozen DSL primitives evaluates correctly against synthetic traces.
- [ ] Java test suite passes (`mvn test`); baseline preservation: pre-S-Eval-2 baseline is `1144 / 1-inherited / 0 / 2` from M2 close (S-Eval-1 added no Java tests). S-Eval-2 will add ~30-50 new Java tests; new baseline expected `~1175-1195 / 1-inherited / 0 / 2` (dev reports actual). Inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure persists unchanged.
- [ ] Python test suite passes (`uv run pytest`); pre-S-Eval-2 baseline is 320 passed / 9 pre-existing failed (per S-Eval-1 handoff §9). S-Eval-2 will add ~15-25 new Python tests; new baseline expected `~335-345 passed / 9 pre-existing failed`. NO new Python test failures introduced by S-Eval-2.
- [ ] Numbers-cite discipline (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`): every count in handoff (file counts, line counts, test counts, baseline counts) reproducible from `git show --numstat <commit>` or `pytest --collect-only` or `mvn test` direct output.

**Observation-only (recorded; does not gate close):**

- Projection token cost change: per-turn projection delta for a representative trace before vs after S-Eval-2 (empty `critical_steps[]` → expected 0-byte delta; if non-zero, dev investigates). Recorded as M3-Eval observation per `docs/milestone_objective.md` §5 token-cost observation.
- Smoke composite_score: observation only per §5.5; expected unchanged at S-Eval-2 close (empty `critical_steps[]` → no Tier-2 gate activity → no composite shift attributable to S-Eval-2).
- DSL parser implementation approach (recursive-descent vs PEG library): dev records choice + rationale in handoff §3 + §4.

## 10. Stop conditions (dev-agent)

Dev STOPS and surfaces to deliver-agent + human (instead of pressing on) if any of:

1. **`trace_check` DSL design proves insufficient** for any of the anticipated S-Eval-3 use cases (e.g., a step like "consult moderation context before explaining" cannot be expressed in the 6 frozen primitives without resorting to message-content matching). Dev should NOT pre-emptively widen the DSL; surface to deliver-agent + human who decide whether to extend in S-Eval-3 (within Codex review boundary) OR scope-shift.
2. **Adding empty `critical_steps[]` rendering** to `phase_plan.skill` breaks existing prompt-composition golden tests (the empty-case parity invariant fails). Halt and surface; the Outcome 3 implementation choice may need to revisit (list-of-strings vs list-of-objects; whether the `critical_steps:` key appears even when empty).
3. **SkillLoader allowlist validation** requires touching `SkillRegistry.select(...)`, `SkillGuardrailDispatcher.dispatch(...)`, or `SkillStateBus.applyOnSkillSwitch(...)` semantic dispatch. Hard fence #1 violation; halt and surface.
4. **`ContextProjectionBuilder` projection wiring** requires extending the schema beyond the field-add (e.g., introducing a new container record type for the projection envelope, or modifying the `phase_plan` Java record). Surface; deliver-agent + human decide whether to expand S-Eval-2 scope or defer the schema extension to a follow-on.
5. **Any file under "Files NOT in scope" §6** needs to be touched to complete S-Eval-2. Hard fence violation; halt and surface; do NOT smuggle scope.
6. **Backward-compat break** on the 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice fixture suite (any case flipping at the composite gate due to S-Eval-2 changes). Halt and surface.
7. **§1.7 structural defence DSL parser** cannot reject ≥ 6 representative hardcode-flavoured `trace_check` strings cleanly — the parser design needs rework. Halt and surface; deliver-agent + human decide whether to extend parser design within S-Eval-2 (acceptable; this IS the structural defence) OR scope-shift.
8. **Java baseline regresses** (any new test failure introduced by S-Eval-2 beyond the inherited `SystemPromptUserRequestedTiebreakerTest:53`). Halt and surface; do NOT commit.

## 11. Handoff document contract (12 sections per Sprint 35 / 41 / 42 shape)

`docs/sprints/sprint-043-handoff.md` must include:

§1 Sprint identity (Sprint 43 / S-Eval-2 / M3-Eval sub-sprint 2).
§2 Summary (one paragraph).
§3 Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`). Includes implementation-choice rationale: (a) Outcome 1 `Severity` enum location choice (nested in `CriticalStep` vs separate file); (b) Outcome 3 projection rendering choice (list-of-strings vs list-of-objects); (c) Outcome 4 DSL parser implementation choice (recursive-descent vs PEG library); (d) ctor-update strategy for existing Java tests (positional vs optional constructor arg) per Sprint 41 OQ-S41.3 / Drift §7-a precedent; (e) Python-side Skill loader strategy (existing vs minimum-viable embedded helper).
§4 Schema extension details (which fields added to `Skill`; `CriticalStep` record shape; SkillLoader allowlist entries; `Severity` enum range).
§5 Projection wiring details (where in `ContextProjectionBuilder` the rendering is added; empty-array parity preservation; per-turn projection byte delta observation).
§6 §4.1 anti-hardcode self-walk verdicts (Q1-Q9 with one-line justification each; **Q1 must explicitly cite the §1.7 structural defence DSL parser test** — `test_skill_procedure_dsl_parser_rejects_hardcodes.py` — as the structural mechanism preventing semantic hardcodes in S-Eval-3 content).
§7 Open questions (OQ-S43.N format; non-blocking decisions for deliver-agent + human at close; surface DSL extension requests from S-Eval-3 if any).
§8 Generalization coverage filled (target / neighbor / negative / shadow counts per §8 stanza).
§9 Validation runs:
- Java test suite (`mvn test`): new baseline vs pre-S-Eval-2 (1144); explicit count of S-Eval-2-added tests.
- Python test suite (`uv run pytest`): new baseline vs pre-S-Eval-2 (320 passed / 9 pre-existing failed); explicit count of S-Eval-2-added tests; confirm 9 pre-existing failures unchanged (no S-Eval-2 introduction).
- Backward-compat run: load 6 Skill YAMLs + 14 smoke + 159 anchor + 12 anchor_outcome + 12 case-family + 1 Alice; assert no schema-side / composite-gate regression.
- Projection token-cost observation: per-turn projection byte count for a representative trace before vs after (expected 0 delta for empty `critical_steps[]`).
§10 Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention from M2 / S-Eval-1 precedent).
§11 Bundle policy honored (dev shipped one commit; deliver-agent close-out files NOT staged).
§12 Closure verdict (LEFT EMPTY by dev; deliver-agent + human fill at close per `feedback_handoff_verdict_section_delegation.md`).

## 12. M3-Eval milestone context (cross-reference, not Sprint 43 contract)

- `docs/milestone_objective.md` is the live M3-Eval milestone objective. Dev SHOULD load it on cold start for layer + scope context. ESPECIALLY §3 S-Eval-2 row (this sub-sprint's scope detail) + §6 hard fences (15 milestone-level items; S-Eval-2 §6 inherits) + §8 Codex review plan (S-Eval-2 deferred to milestone close).
- `docs/solutions/m3_eval_milestone_proposal.md` is the research-agent proposal source. Dev MAY load §5.3 (anti-hardcode standard table — **informational for understanding why the DSL parser must reject keyword/regex/message-content matching**, even though S-Eval-2 ships no content; the parser is built TO catch S-Eval-3 violations) + §6 S-Eval-2 (scope detail; reconcile with this contract — this contract takes precedence on numeric / scope discrepancies).
- `docs/sprints/sprint-042-handoff.md` is the S-Eval-1 dev archive. Dev SHOULD load §12.7 (carry-over — per-UC anchor distribution; CONTEXTUAL for S-Eval-2, not actionable) + §3 (S-Eval-1 schema + scoring deltas; S-Eval-2 builds on the `severity` field convention from S-Eval-1 D-2.5).
- S-Eval-3 (next sub-sprint after S-Eval-2 close) populates `critical_steps` content for the 6 Skills. **S-Eval-3 has per-sub-sprint Codex review per §4.3 trigger #2** (LLM-visible content; §1.7 forbidden-list adjacent). S-Eval-2 dev SHOULD NOT pre-anticipate S-Eval-3 content scope; the contract here is empty-content-only.
- S-Eval-5 closes the 4 R-items named in `docs/milestone_objective.md` §7. Dev SHOULD NOT touch the R-items in S-Eval-2.
- The new R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close; status: proposed; impl deferred) is unrelated to S-Eval-2 scope and SHOULD NOT be addressed here.
