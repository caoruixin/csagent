---
title: Sprint 44 (NEW M3-Eval sub-sprint 3, S-Eval-3) — Populate `critical_steps` for the 6 Skills
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file (until S-Eval-3 close, then archived to docs/sprints/sprint-044-objective.md)
last_reviewed: 2026-05-21
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-043-objective.md]
superseded_by: null
notes: >
  Sprint 44 is the THIRD sub-sprint of NEW Milestone M3-Eval (Coarse-to-
  Fine Evaluation Architecture; see `docs/milestone_objective.md`).
  S-Eval-2 (Sprint 43) closed Clean PASS 2026-05-21 (dev commit
  `69ed77f`; archive at `docs/sprints/sprint-043-objective.md` +
  `docs/sprints/sprint-043-handoff.md`). S-Eval-2 shipped the schema +
  contract + structural defence (DSL parser); S-Eval-3 ships the
  CONTENT on top.

  **S-Eval-3 is the critical governance gate of M3-Eval.** Per
  `iteration_governance.md` §4.3 trigger #2 (LLM-visible content;
  §1.7 forbidden-list adjacent), this sub-sprint requires **per-sub-
  sprint Codex review** at close, BEFORE S-Eval-4 begins. Codex must
  independently verify each populated `critical_steps[].desc` against
  the proposal §5.3 standard. The S-Eval-2 DSL parser is the structural
  defence; the §5.3 standard is the SEMANTIC defence; together they
  prevent S-Eval-3 from accidentally landing a §1.7 violation in
  LLM-visible territory.

  **Scope summary** (per `docs/milestone_objective.md` §3 S-Eval-3 +
  proposal §6 S-Eval-3): populate 3-5 `critical_steps` per Skill on
  the 6 Skill YAMLs (18-30 total). Coverage focuses on 4 scenario
  clusters: UC-FP "post deleted" decision tree; UC-G/H/I/J/K
  intake-then-handover; UC-A/B/C/D/E/F FAQ-grounded answer; DISCOVER
  disambiguation. Each `desc` is **soft procedural narrative**;
  each `trace_check` uses **only** the 6 frozen DSL primitives from
  S-Eval-2.

  **Codex review plan**: per-sub-sprint at S-Eval-3 close. The §4.3
  trigger #2 (LLM-visible content + §1.7 adjacent) fires; this is NOT
  the milestone-shared default that applies to S-Eval-1 / S-Eval-2 /
  S-Eval-4 / S-Eval-5. **S-Eval-4 BLOCKED until S-Eval-3 Codex returns
  `decision: pass`** (or `approve with downgrade-to-signal follow-up`
  per §4.1 verdict set; `reject as semantic hardcode` triggers
  fix-iteration sub-sprint).

  **S-Eval-1 + S-Eval-2 carry-over informing S-Eval-3**:
  - **Thin-corpus UCs** (S-Eval-1 §12.7 → S-Eval-2 §12.7): UC-G / UC-H
    / UC-I / UC-J = 0 anchor coverage; UC-F = 1; UC-B = 5. S-Eval-3
    `critical_steps` for the intake-then-handover cluster (UC-G/H/I/J/K)
    and the UC-F / UC-B FAQ cluster have thin exercise surfaces; bad-
    case Alice + the 12 new anchor_outcome cases (1 per canonical UC)
    are the primary calibration surfaces.
  - **OQ-S43.2 informs S-Eval-3 author**: the projection slot carries
    `[{id, desc}]` format with `id` available for cross-reference (e.g.,
    "after the `search_before_answer` step"). Use `id` if it helps
    natural-language reference between steps; leave unused otherwise.
    Document choice in handoff §3 + §4 per S-Eval-3-author-records-rationale
    convention.
  - **OQ-S43.5 monitoring**: if the `session.<flag>_present` double-
    fallback (explicit `_present` key OR truthiness of bare flag) proves
    too permissive for any S-Eval-3 step (a step triggers on flag
    truthiness alone), surface as OQ-S44.N + tighten via S-Eval-3
    fix-iteration if needed.

  **Bundle policy**: dev ships content + Tier-2 verification run + handoff
  in ONE bundle commit. Dev does NOT stage deliver-agent close-out files
  (sprint_objective archive, milestone_objective edits, 10-handoff §1
  lead refresh, codex-findings reset, action_bank Sprint 44 row) per
  `feedback_commit_at_end_bundles_deliver_artefacts.md`.

  **Cross-session continuity**: if a new dev-agent picks this up cold,
  the dev prompt at `compact/sprint-044-dev-prompt.md` is the entry
  point. Read order: AGENTS.md (auto-loaded) → this file →
  docs/milestone_objective.md (M3-Eval context, §3 S-Eval-3 row;
  §8 Codex review plan) → proposal §5 + §5.3 (decision-5 deep-dive +
  ANTI-HARDCODE STANDARD TABLE — LOAD-BEARING) + §6 S-Eval-3 →
  docs/sprints/sprint-043-handoff.md (S-Eval-2 close artefacts;
  §12.7 carry-over) → docs/sprints/sprint-042-handoff.md §12.7
  (anchor-distribution carry-over).
---

# Sprint 44 (NEW M3-Eval sub-sprint 3, S-Eval-3) — Populate `critical_steps` for the 6 Skills

## 1. Sub-sprint class

**Single-track semantic-touching sub-sprint, layer `eval_spec` per `iteration_governance.md` §3.2 Q6 (content under an LLM-visible contract that S-Eval-2 wired).** §7 stanza REQUIRED. Codex review **per-sub-sprint** (§4.3 trigger #2 — §1.7 forbidden-list adjacent; this IS the milestone's critical governance gate).

Class breakdown:

- **Primary layer**: `eval_spec` — content authoring on 6 Skill YAMLs (`critical_steps[].desc` LLM-visible procedural narrative + `critical_steps[].trace_check` eval-side DSL expression).
- **§1.7 forbidden-list adjacency**: **YES — this IS the §1.7 risk surface for the entire M3-Eval milestone.** S-Eval-2's DSL parser is the structural defence (rejects regex / keyword / message-content matching at parse time); the §5.3 standard table (proposal §5.3) is the SEMANTIC defence on `desc` wording. **Codex per-sub-sprint review at close is the milestone's gate**.
- **Tier-0 invariant claim**: no new Tier-0 invariant added.
- **No new Java code, no new runtime behaviour**: S-Eval-3 is YAML content authoring only (6 Skill YAMLs extended with populated `critical_steps:` blocks). No `Skill.java` / `SkillLoader.java` / `ContextProjectionBuilder.java` / `composite.py` / `skill_procedure_check.py` edits beyond minor fixes if needed (e.g., new DSL primitive — but that's a STOP-and-surface signal per §10).

## 2. Goal

Populate **18-30 `critical_steps`** across the 6 Skill YAMLs (3-5 each), with each step carrying:

- `id`: short kebab-case identifier (cross-referenceable per OQ-S43.2; usage optional).
- `desc`: **soft procedural narrative** describing what a good CS agent does in this situation — written per the proposal §5.3 standard table (no if-else, no keyword matrix, no regex; soft procedural / soft diagnostic only).
- `trace_check`: DSL expression using ONLY the 6 frozen primitives from S-Eval-2 (`accumulated_tool_results.<tool>` / `tool_event_seq(...) < tool_event_seq(...)` / `intake_state.fields_collected.contains(...)` / `session.<flag>_present` / `any_of(...)` / `all_of(...)`).
- `mandatory_for`: list of UC ids where this step is mandatory (validates Tier-2 gate scope).
- `severity`: `mandatory` (gate-contributing on `mandatory_for` UCs) OR `advisory` (Tier-3 observation only).

**Coverage clusters** (per proposal §6 S-Eval-3 step 1 + `docs/milestone_objective.md` §3 S-Eval-3):

| Cluster | Target Skill(s) | Anticipated steps | Notes |
|---|---|---|---|
| **UC-FP "post deleted" decision tree** | `discover_triage` (route hint) + `resolve_intake_collect_and_handover` (intake on appeal) + `resolve_faq_grounded_answer` (explain when actionable) | 3-5 across 1-3 Skills | "Consult moderation context before explaining"; "Provide reposting guidance when actionable"; "Route to UC-H intake on appeal request". |
| **UC-G/H/I/J/K intake-then-handover** | `resolve_intake_collect_and_handover` + `escalate` | 4-5 across 1-2 Skills | "Collect required intake fields before creating controlled case"; "Create case before handover"; "Handover includes intake fields". |
| **UC-A/B/C/D/E/F FAQ-grounded answer** | `resolve_faq_grounded_answer` | 3-4 | "Retrieve a knowledge article before composing a factual answer"; "Cite source on factual_answer output class"; "Do not paraphrase a generic article as case-specific". |
| **DISCOVER disambiguation** | `discover_triage` + `confirm` | 3-4 across 2 Skills | "Respect clarification-budget"; "Do not commit to a UC before sufficient evidence"; "Ask one clarifying question before routing on ambiguity". |

**Total expected**: 13-18 from the 4 clusters; remaining 0-12 from terminal / escalate / cross-cluster polish to reach the 18-30 envelope.

**Per-Skill target distribution** (preliminary; dev + deliver-agent refine at planning round if needed):

| Skill | Anticipated `critical_steps` count | Primary clusters |
|---|---|---|
| `discover_triage.yaml` | 3-4 | DISCOVER disambiguation + UC-FP route hint |
| `confirm.yaml` | 2-3 | DISCOVER disambiguation tail (confirmation behaviour) |
| `resolve_faq_grounded_answer.yaml` | 4-5 | UC-A/B/C/D/E/F FAQ-grounded + UC-FP explain-when-actionable |
| `resolve_intake_collect_and_handover.yaml` | 4-5 | UC-G/H/I/J/K intake-then-handover + UC-FP intake-on-appeal |
| `escalate.yaml` | 2-3 | Escalation reason present; intake fields handed off |
| `terminal.yaml` | 1-2 | No new tools after terminal; final user message present |
| **TOTAL** | **16-22** | (within 18-30 envelope; lower bound) |

**Verification step**: after population, run the existing bad-case suite (Alice) + the new `anchor_outcome` suite (S-Eval-1) + 14 smoke + a sampled subset of the 159 anchor against the populated `critical_steps`. Verify the Tier-2 failures align with known failure modes documented in `eval_interactive/case_specs/bad_cases/_manifest.md` (Alice's UC-H intake-then-handover failure shape should surface as an `intake_complete_required` Tier-2 fail on UC-H; record the actual Tier-2 emission pattern in handoff §6 for Codex independent verification).

## 3. Non-goals (explicit)

S-Eval-3 does NOT:

1. **Modify any existing Skill `procedure` text** or any other M2-landed Skill field (`guardrails`, `state_inheritance`, `applicable_use_cases`, `system_instruction`) on any of the 6 YAMLs. ONLY `critical_steps:` blocks are added.
2. **Create new Skill YAML files.** The 6 existing Skills are the only authoring surface.
3. **Modify `Skill.java` / `CriticalStep.java` / `SkillLoader.java` / `SkillRegistry.java` / `SkillStateBus.java` / `SkillGuardrailDispatcher.java` / `ContextProjectionBuilder.java`.** S-Eval-2 just shipped these; S-Eval-3 only authors YAML content.
4. **Extend the `trace_check` DSL beyond the 6 frozen primitives.** If any S-Eval-3 step appears to need a new primitive (e.g., a "moderation context loaded" check that doesn't fit the 6), STOP and surface per §10 #1 — deliver-agent + human decide DSL extension OR step rephrasing.
5. **Touch `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` or `composite.py`.** S-Eval-2 shipped these; only minor fixes are allowed under §10 surface-and-extend discipline, not author-side rewrites.
6. **Touch runtime semantic surfaces** (RuntimeIntentClassifier, IntentClassification, DriftResult, DriftDetector, UseCaseRouter, ClassifyUseCaseTool, PhaseEvaluator semantic dispatch, AgentRunLoopImpl, ControlKernel, system_prompt.txt, tool-policy.yaml, IntakeFieldsRegistry, UseCaseRegistryService, ResolveDispositionEvaluator). M2 §6 #5 hard fence inherits.
7. **Touch any case fixture** under smoke / anchor / anchor_outcome / case-family / bad-case / shadow / case_spec_overrides.yaml. The bad-case + anchor_outcome run is read-only verification.
8. **Touch the L3 judge / rubric** (`llm_judge.py` is S-Eval-5 territory).
9. **Touch the eval harness / loader / simulator** (`eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`).
10. **Add new Tier-0 invariants.** Milestone-level §6 hard fence.
11. **Add any runtime if-else / keyword matrix / per-UC enumeration / regex anywhere in `desc` or `trace_check`.** §1.7 red line. Self-walk against the proposal §5.3 standard table BEFORE commit.
12. **Touch governance docs** (`iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`, `runtime_freeze_and_risk_policy.md`), foundational docs, or sprint / milestone archives (including the just-archived `docs/sprints/sprint-043-objective.md`).
13. **Touch `docs/codex-findings.md`** (review-agent territory; Codex per-sub-sprint review will WRITE here at S-Eval-3 close).
14. **Touch `docs/milestone_objective.md`** (deliver-agent territory).
15. **Address S-Eval-2 OQ-S43.1 / OQ-S43.4** (routed to M3-Eval-shared Codex review per S-Eval-2 close §12.4).

## 4. Premise check (deliver-agent verified 2026-05-21)

- ✅ `server/src/main/resources/skills/` carries all 6 Skill YAMLs at HEAD `69ed77f`:
  - `discover_triage.yaml` (~32 lines post-M2; carries `procedure` + `guardrails` + `state_inheritance` + `applicable_use_cases`).
  - `confirm.yaml` (~30 lines).
  - `resolve_faq_grounded_answer.yaml` (~53 lines).
  - `resolve_intake_collect_and_handover.yaml` (~38 lines).
  - `escalate.yaml` (~29 lines).
  - `terminal.yaml` (~26 lines).
  - **None currently carry `critical_steps:` blocks** (S-Eval-2 ships the schema; S-Eval-3 populates).
- ✅ S-Eval-2 shipped: `Skill.java` extended (+ `criticalSteps` field via backward-compat secondary ctor); NEW `CriticalStep.java` record with nested `Severity` enum; `SkillLoader.java` validates `critical_steps:` block per Sprint 38-fix allowlist precedent; `ContextProjectionBuilder.java` renders `[{id, desc}]` list-of-objects under `phase_plan.critical_steps` (empty array → no key); NEW `skill_procedure_check.py` with `SkillProcedureExtractor` + 6-primitive DSL parser + §1.7 structural defence (15 hardcode-flavoured strings rejected at parse time per `test_skill_procedure_dsl_parser_rejects_hardcodes.py`); `composite.py` Tier-2 wiring via S-Eval-1 `severity` field convention.
- ✅ Bad-case suite at `eval_interactive/case_specs/bad_cases/` carries 1 case (Alice) + `_manifest.md`; expected to surface `intake_complete_required` Tier-2 fail on UC-H once S-Eval-3 populates the intake-then-handover cluster.
- ✅ `anchor_outcome/` suite at `eval_interactive/case_specs/anchor_outcome/` carries 12 cases (1 per canonical UC, including 1 synthetic UC-G per S-Eval-1 OQ-S42.1) for Tier-2 alignment verification.
- ✅ Proposal §5.3 standard table is the SEMANTIC defence; reproduced verbatim in §8 stanza below and in the dev prompt §6.

## 5. Files in scope (Sprint 44 dev ships)

**6 Skill YAMLs (CONTENT extension; no other field modified):**

- `server/src/main/resources/skills/discover_triage.yaml`: append `critical_steps:` block (3-4 steps; DISCOVER disambiguation + UC-FP route hint).
- `server/src/main/resources/skills/confirm.yaml`: append `critical_steps:` block (2-3 steps; DISCOVER disambiguation tail).
- `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml`: append `critical_steps:` block (4-5 steps; FAQ-grounded answer + UC-FP explain-when-actionable).
- `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml`: append `critical_steps:` block (4-5 steps; intake-then-handover + UC-FP intake-on-appeal).
- `server/src/main/resources/skills/escalate.yaml`: append `critical_steps:` block (2-3 steps; escalation reason + intake-on-handover).
- `server/src/main/resources/skills/terminal.yaml`: append `critical_steps:` block (1-2 steps; terminal behaviour).

**Verification artefacts (no code edits; run + record):**

- Run S-Eval-2 Python extractor + composite scorer against bad-case suite (Alice) + anchor_outcome suite (12) + 14 smoke + sampled subset of 159 anchor. Record per-Tier-2-fail observations in handoff §6 (mapping each fail to the known failure mode in `_manifest.md` where applicable).
- Run Java + Python regression suites; baseline preservation expected.

**Handoff document:**

- `docs/sprints/sprint-044-handoff.md` (dev-authored at sub-sprint close); follows 12-section template per Sprint 35 / 41 / 42 / 43 precedent.

**Optional minor fixes (only if §10 STOP-and-surface dialogue authorizes):**

- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`: minor DSL extension OR `session.<flag>_present` fallback tightening per OQ-S43.5 monitoring, IF S-Eval-3 author surfaces a need that deliver-agent + human authorize.
- Otherwise NO Java / Python code edits.

## 6. Files NOT in scope (hard fences)

**1. M2-landed + S-Eval-2-landed Skill semantics — NO touch:**

- `Skill.java`, `CriticalStep.java`, `SkillRegistry.java`, `SkillLoader.java`, `SkillStateBus.java`, `SkillGuardrailDispatcher.java`, `StateInheritance.java`, `Guardrail.java`.
- `ContextProjectionBuilder.java` (S-Eval-2 wired `phase_plan.critical_steps`; S-Eval-3 only populates the YAML side).
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (S-Eval-2 shipped; only authorized minor fixes per §5 / §10).
- `eval_interactive/eval_interactive/scoring/composite.py` (S-Eval-2 wired Tier-2 band).
- 6 Skill YAMLs' `procedure` / `guardrails` / `state_inheritance` / `applicable_use_cases` / `system_instruction` (UNCHANGED; only `critical_steps:` block appended).

**2. Runtime semantic surfaces — NO touch (M2 §6 #5 + S-Eval-1/2 inherits):**

- `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java`.
- `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `ControlKernel.java`.
- `system_prompt.txt`, `tool-policy.yaml`.
- `IntakeFieldsRegistry.java`, `UseCaseRegistryService.java`, `ResolveDispositionEvaluator.java`.

**3. Eval case fixtures + harness — NO touch (cascade fence):**

- Any case under `eval_interactive/case_specs/smoke/` (14 cases).
- Any case under `eval_interactive/case_specs/anchor/` (159 cases).
- Any case under `eval_interactive/case_specs/anchor_outcome/` (12 cases).
- Any case under `eval_interactive/case_specs/case_families/<existing>/` (12 directories).
- The existing Alice bad case `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`.
- `eval_interactive/case_specs/bad_cases/_manifest.md` (S-Eval-4 territory).
- Shadow CaseSpecs under `eval_interactive/case_specs_shadow/`.
- `eval_interactive/case_spec_overrides.yaml`.
- `eval_interactive/eval_interactive/loader/`, `eval_interactive/eval_interactive/simulator/`, `eval_interactive/eval_interactive/case_spec/schema.py` (S-Eval-1 landed), `eval_interactive/eval_interactive/case_spec/loader.py`, `eval_interactive/eval_interactive/scoring/hard_checks.py`, `eval_interactive/eval_interactive/scoring/outcome_checks.py`, `eval_interactive/eval_interactive/scoring/llm_judge.py` (S-Eval-5 territory).

**4. Governance and archives — NO touch:**

- `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/customer_service_tool_spec_v0_3.md`.
- `docs/current/iteration_governance.md`, `docs/current/doc_governance.md`, `docs/current/agent_context_guide.md`, `docs/current/faq_grounding_contract.md`.
- `docs/sprints/sprint-001-*` through `docs/sprints/sprint-043-*` (including the just-archived S-Eval-2 contract).
- `docs/milestones/M1_objective.md`, `M2_objective.md`, `M2-Skill_objective.md`, `M2_codex-review.md`.
- `docs/milestone_objective.md` (M3-Eval; deliver-agent territory).
- `docs/codex-findings.md` (review-agent territory; Codex per-sub-sprint review WRITES here at S-Eval-3 close — NOT dev's territory).

**5. Action bank — limited touch:**

- `docs/action_bank.md`: dev SHOULD NOT close R-items in S-Eval-3 (R-item closures happen at S-Eval-5 close per `docs/milestone_objective.md` §7). Dev MAY append a Sprint 44 close-action index row in §6 at sub-sprint close per existing convention.
- The new R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close) is unrelated to S-Eval-3 and SHOULD NOT be addressed here.

## 7. Bundle policy

S-Eval-3 dev ships in **ONE bundle commit** containing:

- 6 Skill YAML files with `critical_steps:` blocks appended.
- Optional minor fixes per §5 IF surfaced + deliver-agent + human authorized.
- Dev handoff `docs/sprints/sprint-044-handoff.md`.

Expected: 7-8 files in the bundle (much smaller than S-Eval-2's 12; content-only authoring).

Deliver-agent + human bundle (separately, post-close):

- This sub-sprint contract archive (`docs/sprints/sprint-044-objective.md`).
- Codex per-sub-sprint review at `compact/sprint-044-review-prompt.md` (drafted by deliver-agent BEFORE Codex dispatch; reviewed by human).
- Codex review output → archived to `docs/sprints/sprint-044-codex-review.md` (after Codex returns).
- Live `docs/sprint_objective.md` replaced with S-Eval-4 contract (when S-Eval-4 planning round completes; **only after Codex S-Eval-3 returns `pass`**).
- `docs/10-handoff.md` §1 lead refresh.
- `docs/codex-findings.md` reset to scaffold (after archive).
- `docs/action_bank.md` Sprint 44 close-action index row (if dev did not already append).

Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent close-out files; human bundles at deliver-agent's commit.

## 8. Layer-classification + anti-hardcode stanza (per §7; REQUIRED)

**Target failure layer:** `eval_spec` (content authoring under the LLM-visible contract S-Eval-2 wired).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. The new Tier-2 `skill_procedure_followship` gate (S-Eval-2 ship) gains its first content in S-Eval-3 but remains an eval-side check, not Tier-0 runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2.

**Semantic hardcode:** Introduces 18-30 `critical_steps[].desc` fields as **LLM-visible soft procedural narratives**. **Justification**: replaces implicit reliance on tool whitelists + retrieval results with EXPLICIT procedural guidance that the LLM owns acting on. NO if-else, NO keyword matrix, NO regex; NO message-content matching in `trace_check` (S-Eval-2 DSL parser enforces structurally; S-Eval-3 self-walks against §5.3 standard semantically). **Anti-hardcode standard per the proposal §5.3 (reproduced below)**:

| Form of `desc` | Example | Verdict |
|---|---|---|
| Soft procedural narrative | "For UC-FP issues, consult the moderation context before explaining the reason" | ✅ LLM-first; procedural guidance the LLM owns acting on |
| Soft diagnostic signal | "If the user mentions 'appeal' or 'review', consider whether this is actually UC-H rather than UC-FP" | ⚠️ Edge — Codex will scrutinise; acceptable as guidance, NOT as a rule |
| Hard if-else rule | "IF user.message.contains('appeal') THEN active_use_case := UC-H" | ❌ Violates §1.7; Codex rejects |
| Keyword enumeration matrix | "Trigger words for UC-H: ['appeal', 'review', 'wrongly', 'unfairly', ...]" | ❌ Violates §1.7; Codex rejects |

**Sunset plan**: N/A — this IS the soft-signal form that replaces existing scattered teaching paragraphs in `system_prompt.txt` and the implicit reliance on tool whitelisting. The 18-30 `critical_steps[].desc` is the canonical declarative procedural surface going forward; not a temporary workaround.

**Generalization coverage:**

- **Target**: 18-30 `critical_steps[].desc` populated across 6 Skills; each `desc` walked against the §5.3 standard before commit; each `trace_check` uses only the 6 frozen DSL primitives.
- **Neighbor**: Alice bad case + 12 anchor_outcome cases (1 per canonical UC including 1 synthetic UC-G) + 14 smoke + sampled anchor cases run end-to-end; Tier-2 emissions verified against expected failure modes from `bad_cases/_manifest.md`.
- **Negative**: a UC-A case must NOT trigger UC-FP `critical_steps` (active Skill scoping via `mandatory_for` validates this). A UC-FP case must NOT trigger UC-H intake `critical_steps` until the active Skill becomes `resolve_intake_collect_and_handover`. Documented per-cluster in handoff §6.
- **Shadow**: 3-5 cases held out of dev visibility (per `eval_interactive/case_specs_shadow/_ACCESS_BOUNDARY.md` convention) for generalization check at M3-Eval close; S-Eval-3 dev does NOT read shadow.

## 9. Success metrics (per `iteration_governance.md` §5)

**Hard gates (sub-sprint close PASS only if all clear):**

- [ ] 18-30 `critical_steps` populated across 6 Skills per §2 distribution (3-5 per Skill); each step carries all 5 required fields (`id`, `desc`, `trace_check`, `mandatory_for`, `severity`).
- [ ] **§5.3 anti-hardcode self-walk on every populated `desc` PASSES**. Walk template in dev prompt §6. Document per-step verdict in handoff §6 (CRITICAL for Codex per-sub-sprint review).
- [ ] **Every `trace_check` parses cleanly through the S-Eval-2 DSL parser** (positive grammar; the 6 frozen primitives). Verified by SkillLoader at Spring bootstrap + Python `parse_trace_check` invocation; no `TraceCheckDSLSyntaxError` raised on any populated step.
- [ ] Java baseline preservation: pre-S-Eval-3 baseline `1163 / 1-inherited / 0 / 2` (S-Eval-2 close). S-Eval-3 adds no new Java code; new test count expected ~0-5 (if any SkillLoader edge cases surface). Inherited `SystemPromptUserRequestedTiebreakerTest:53` persists unchanged.
- [ ] Python baseline preservation: pre-S-Eval-3 baseline `392 passed / 9 pre-existing failed` (S-Eval-2 close). S-Eval-3 may add 0-15 new tests (if any extractor edge cases surface during Tier-2 verification run). NO new Python test failures introduced.
- [ ] **Tier-2 verification run executed**: bad-case (Alice) + anchor_outcome (12) + smoke (14) + sampled anchor (≥ 20 cases) run end-to-end via S-Eval-2 extractor + composite scorer. Per-Tier-2-fail observation recorded in handoff §6 (each fail mapped to a known failure mode in `bad_cases/_manifest.md` where applicable). **Alice UC-H intake-then-handover failure expected to surface as `intake_complete_required` Tier-2 fail** (or equivalent step id assigned in `resolve_intake_collect_and_handover.yaml`).
- [ ] Per-cluster negative check verified: a UC-A case does NOT trigger UC-FP critical_steps; a UC-FP case does NOT trigger UC-H intake critical_steps; recorded in handoff §6.
- [ ] Projection token-cost observation: per-turn projection byte / token delta vs S-Eval-2 baseline (0 bytes; empty `critical_steps[]`). Expected: ≤ 1000 tokens per turn per `docs/milestone_objective.md` §5 acceptance bar. Recorded in handoff §9.
- [ ] Numbers-cite discipline (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`): every count in handoff (step counts per Skill, Tier-2 fail counts, test counts, line counts) reproducible from `git show --numstat <commit>` or `grep -c` or test-run direct output.
- [ ] **Codex per-sub-sprint review verdict: `decision: pass`** (or `approve with downgrade-to-signal follow-up` per §4.1 verdict set) BEFORE S-Eval-4 begins. **`reject as semantic hardcode` triggers fix-iteration sub-sprint**; deliver-agent + human classify per S-Eval-3 close discussion.

**Observation-only (recorded; does not gate close):**

- Architecture-health metric direction: `new_semantic_hardcode_count` = 0 (every populated `desc` is soft narrative per §5.3 standard; `trace_check` strings use only the 6 frozen DSL primitives — no keyword/regex/message-content); `soft_signal_conversion_count` = 18-30 (the populated steps ARE soft-signal projections of previously implicit / scattered procedural intent per proposal §5.2 single-source-of-truth claim).
- Thin-corpus UC observations: any S-Eval-3 step targeting UC-G / UC-H / UC-I / UC-J / UC-F / UC-B has limited exercise surface (carry-over from S-Eval-1 §12.7); record per-UC step counts in handoff §6 for S-Eval-4 calibration input.
- OQ-S43.2 `id` usage observation: dev records whether `id` cross-reference was used in any populated `desc` (informs M3-Eval-close calibration for whether to drop the `id` field as a follow-on).
- OQ-S43.5 `session.<flag>_present` permissiveness observation: dev records whether any populated step relies on the bare-flag truthiness fallback (if YES, surface as OQ-S44.N for S-Eval-3 fix-iteration consideration).

## 10. Stop conditions (dev-agent)

Dev STOPS and surfaces to deliver-agent + human (instead of pressing on) if any of:

1. **A needed `desc` cannot be expressed without resorting to keyword / regex / message-content matching** to make Codex review pass §5.3 standard. Surface; deliver-agent + human decide whether to rephrase the step OR drop it.
2. **A needed `trace_check` cannot be expressed in the 6 frozen DSL primitives** (e.g., a step like "moderation context loaded" requires a primitive not in S-Eval-2's frozen set). Do NOT extend the DSL silently; surface to deliver-agent + human who decide DSL extension within S-Eval-3 (must go through S-Eval-2 author + Codex review boundary) OR step rephrasing OR step drop.
3. **A `trace_check` parses successfully but the §1.7 structural defence test flags it as a hardcode-shape** (the dev should run `test_skill_procedure_dsl_parser_rejects_hardcodes.py` mentally against each `trace_check` — if any populated `trace_check` looks like it would be on the blocklist, that's a STOP signal even if the parser accepts it).
4. **Codex per-sub-sprint review returns `reject as semantic hardcode`** on first pass. Dev does NOT silently iterate; surface to deliver-agent + human who decide fix-iteration scope (sub-sprint extension OR new fix-iteration sub-sprint).
5. **Alice bad case Tier-2 fail does NOT surface** the `intake_complete_required` step OR equivalent. May indicate the populated step is mis-scoped or the Tier-2 wiring has an edge case. Surface; do NOT massage the step content to force the expected fail.
6. **Backward-compat break** on any of the 6 Skill YAMLs (SkillLoader rejects the populated YAML; Spring bootstrap fails; existing prompt-composition golden tests regress). Halt and surface.
7. **Java or Python baseline regresses** (any new test failure introduced by S-Eval-3 beyond the inherited `SystemPromptUserRequestedTiebreakerTest:53` and the 9 pre-existing Python failures). Halt and surface; do NOT commit.
8. **Any file under "Files NOT in scope" §6** needs touching to complete S-Eval-3. Hard fence violation; halt and surface; do NOT smuggle scope.
9. **Per-turn projection token-cost exceeds 1000 tokens** (milestone §5 acceptance bar). Surface; deliver-agent + human decide whether to trim `desc` strings OR accept as a milestone observation.

## 11. Handoff document contract (12 sections per Sprint 35 / 41 / 42 / 43 shape)

`docs/sprints/sprint-044-handoff.md` must include:

§1 Sprint identity (Sprint 44 / S-Eval-3 / M3-Eval sub-sprint 3).
§2 Summary (one paragraph).
§3 Files shipped + per-file line-count numstat (reproducible via `git show --numstat <commit>`). Includes OQ-S43.2 `id` usage choice + OQ-S43.5 `session.<flag>_present` reliance observation.
§4 Per-Skill `critical_steps` content map (which steps landed on which Skill; cluster mapping; mandatory_for UC coverage per step).
§5 §5.3 anti-hardcode self-walk per populated `desc` (table format; per-step verdict against the 4-row §5.3 standard; **CRITICAL for Codex per-sub-sprint review**).
§6 §4.1 anti-hardcode self-walk verdicts (Q1-Q9 with one-line justification each; **Q1 must explicitly enumerate §5.3 self-walk and the S-Eval-2 DSL parser as the two layers of defence**).
§7 Open questions (OQ-S44.N format; non-blocking decisions for deliver-agent + human at close; surface any OQ-S43.x follow-ups observed during S-Eval-3 authoring).
§8 Generalization coverage filled (target / neighbor / negative / shadow counts per §8 stanza).
§9 Validation runs:
- Java test suite (`mvn test`): baseline vs pre-S-Eval-3 (1163); explicit count of S-Eval-3-added tests (expected 0-5).
- Python test suite (`uv run pytest`): baseline vs pre-S-Eval-3 (392 passed / 9 pre-existing failed); explicit count of S-Eval-3-added tests (expected 0-15); confirm 9 pre-existing failures unchanged.
- **Tier-2 verification run**: bad-case (Alice) + anchor_outcome (12) + smoke (14) + sampled anchor (≥ 20). Per-Tier-2-fail observation table mapped to `bad_cases/_manifest.md` known failure modes. **Alice UC-H expected fail surfaces** — record actual `step_id` and `severity`.
- Per-cluster negative check: UC-A case does NOT trigger UC-FP steps; UC-FP case does NOT trigger UC-H intake steps.
- Projection token-cost observation: per-turn projection byte / token delta vs S-Eval-2 baseline (0 bytes; empty `critical_steps[]` → populated `critical_steps[]`).
§10 Contract drift (any deviation from this contract; classify per §7-a / §7-b / §7-c / §7-d convention).
§11 Bundle policy honored (dev shipped one commit; deliver-agent close-out files NOT staged).
§12 Closure verdict (LEFT EMPTY by dev per `feedback_handoff_verdict_section_delegation.md`).

## 12. M3-Eval milestone context (cross-reference, not Sprint 44 contract)

- `docs/milestone_objective.md` is the live M3-Eval milestone objective. Dev SHOULD load it on cold start for layer + scope context. ESPECIALLY §3 S-Eval-3 row (this sub-sprint's scope detail) + §6 hard fences (15 milestone-level items; S-Eval-3 §6 inherits) + §8 Codex review plan (**S-Eval-3 fires per-sub-sprint Codex** per §4.3 trigger #2).
- `docs/solutions/m3_eval_milestone_proposal.md` is the research-agent proposal source. **LOAD-BEARING sections**: §5 (decision-5 deep-dive — explains WHY `critical_steps[].desc` is LLM-visible) + **§5.3 (ANTI-HARDCODE STANDARD TABLE — the SEMANTIC defence; reproduced in §8 stanza above and dev prompt §6)** + §5.4 (token cost estimate 500-900 tokens; milestone §5 acceptance bar ≤ 1000) + §6 S-Eval-3 (scope detail).
- `docs/sprints/sprint-043-handoff.md` is the S-Eval-2 dev archive. LOAD-BEARING sections: §3 (S-Eval-2 file shipments including the DSL parser at `skill_procedure_check.py` — the structural defence S-Eval-3 builds on) + §4 (schema details — `CriticalStep` record shape that S-Eval-3 populates) + §5 (projection wiring details — empty-array parity; how populated `critical_steps[]` will render) + §12.7 (carry-over: thin-corpus UCs UC-G/H/I/J=0 / UC-F=1 / UC-B=5; OQ-S43.2 `id` cross-reference availability; OQ-S43.5 flag fallback monitoring) + §12 closure verdict.
- `docs/sprints/sprint-042-handoff.md` §12.7 — the per-UC anchor distribution observation (origin of the thin-corpus UC awareness).
- S-Eval-4 (next sub-sprint after S-Eval-3 close) expands the bad-case suite (10-12 new bad cases) and exercises Tier-2 against the populated content. **S-Eval-4 BLOCKED until S-Eval-3 Codex returns `pass`** per `iteration_governance.md` §4.3 trigger #2.
- S-Eval-5 closes the 4 R-items named in `docs/milestone_objective.md` §7. Dev SHOULD NOT touch the R-items in S-Eval-3.
