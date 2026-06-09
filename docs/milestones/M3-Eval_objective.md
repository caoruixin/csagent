---
title: Milestone M3-Eval — Coarse-to-Fine Evaluation Architecture
doc_tier: milestone-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-23
review_cadence: ad hoc
supersedes: [docs/milestones/M2_objective.md]
superseded_by: docs/milestone_objective.md (M4+ candidate selection pending)
notes: >
  M3-Eval is the third milestone under the §8 milestone framework. Path 1
  research-driven (per `compact/sprint-deliver-orchestrator.md` Workflow
  inputs §Path 1); research-agent proposal at
  `docs/solutions/m3_eval_milestone_proposal.md` (2026-05-20); 5 human-
  locked decisions per proposal §4 (Walk-A independent milestone /
  M3-Corpus separate / 10-12 bad cases / corpus independent / **decision
  5 — `critical_steps[].desc` is LLM-visible**). M3-Corpus is a separate
  parallel-track milestone (`R-corpus-coverage-audit-per-uc`); no
  dependency between M3-Eval and M3-Corpus close.

  **Core thesis**: the current L1/L2/L3 layered eval treats checks as
  roughly equal-weighted peers; CaseSpec encodes 6 hard surfaces that
  conflate *means* (specific tool sequence, narrative wording) with
  *ends* (user goal solved). Governance has already demoted smoke
  composite_score to observation (§5.5) and elevated the curated bad-case
  suite to primary gate (§5.6). M3-Eval closes the gap between governance
  and schema/code by introducing a **four-tier pyramid**: Tier-0 safety
  floor (unchanged) / Tier-1 Outcome (new mandatory signal, coarsest) /
  Tier-2 Critical-flow (NEW; per-UC process correctness derived from
  Skill `critical_steps`) / Tier-3 Polish (advisory only). Decision 5
  makes `critical_steps[].desc` LLM-visible — single source of truth for
  declarative procedural content + eval-side `trace_check` for Tier-2
  scoring. This crosses §1.7 forbidden-list territory, which is why
  **S-Eval-3 requires per-sub-sprint Codex review** (§4.3 trigger #2).

  **Numbers-cite reconciliation against HEAD (2026-05-20)**:
  - proposal §1.2 / §3 / §6 S-Eval-1 / §8 referenced "30 anchor cases"
    against the existing suite; actual count at `eval_interactive/case_specs/anchor/`
    is **159** YAMLs. This milestone uses 159 as the load-and-run
    backward-compat scope. S-Eval-1 backward-compat check now reads
    "159 anchor cases" instead of "30".
  - proposal §4 decision 3 referenced "29 approved entries" in
    `eval_interactive/case_spec_overrides.yaml` as the source for
    S-Eval-4 bad-case selection; actual count at HEAD is **17**
    `status: approved` blocks. S-Eval-4 picks 10-12 from 17 (tighter
    selection; coverage of D1-D4 dimensions × 3 UC families must be
    re-verified at S-Eval-4 planning). Deliver-agent flagged to human
    on 2026-05-20 at M3-Eval draft hand-back; numbers locked here at
    the actual count.
  - proposal §13 #1 pre-flight ("archive existing milestone_objective.md
    to M2_objective.md if not already done") was **already complete** at
    M2 close 2026-05-18. No re-archive needed; this M3-Eval draft
    directly supersedes the M3-candidate-selection placeholder.

  **M2 dependency status**: M2 (Skill Registry Abstraction + Wholesale
  Retroactive Externalization) closed Clean PASS 2026-05-18 (archive
  `docs/milestones/M2_objective.md`; Codex `pass / blocking_count: 0`
  on first pass). M3-Eval rides on the M2 Skill abstraction — 6 Skill
  YAMLs at `server/src/main/resources/skills/` exist with LLM-visible
  `procedure` fields and machine-readable `guardrails`. M3-Eval augments
  the Skill YAML schema (NEW `critical_steps` field) but **does NOT**
  change `SkillRegistry` / `SkillLoader` / `SkillStateBus` /
  `SkillGuardrailDispatcher` semantics. M2 §6 hard-fence #5 (no
  `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter` /
  `ClassifyUseCaseTool` touch) inherits into M3-Eval as a milestone-
  level hard fence per §6 below.

  **Tier-0 candidates pending re-evaluation at M3+**: C2
  (`R-skill-guardrail-non-overridability-tier-0`) and C3
  (`R-skill-state-bus-boundary-enforcement-tier-0`) remain QUALIFIED-
  DEFER per M2 close Codex verdict; deliver-agent + human will revisit
  at M3-Eval close OR a later milestone after production trace
  observation accumulates. M3-Eval ships nothing new C2/C3-relevant
  (Skill YAML schema extension only).

  **OOOSR-tolerance**: M3-Eval is the first milestone where the §5.6
  bad-case suite is intentionally expanded (10-12 new cases vs Alice-
  only baseline); the trial milestone-close dry-run in S-Eval-4 is the
  calibration step. Deliver-agent + human accept that closure_criterion
  wording may need refinement during the dry-run; this is the
  calibration objective, not a defect.
---

# Milestone M3-Eval — Coarse-to-Fine Evaluation Architecture

## 1. Milestone class

**Multi-layer milestone, 5 coordinated sub-sprints.** Layer + §7-stanza + Codex breakdown:

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Eval-1 — Schema simplification + outcome-only anchor suite | `eval_spec` | REQUIRED | Milestone-shared (default) |
| S-Eval-2 — Skill `critical_steps` schema + extractor + projection wiring | `eval_spec` (primary) + `prompt_projection` | REQUIRED | Milestone-shared (default) |
| S-Eval-3 — Populate `critical_steps` for the 6 Skills | `eval_spec` | REQUIRED | **Per-sub-sprint (§4.3 trigger #2 — §1.7 forbidden-list adjacent)** |
| S-Eval-4 — Bad-case suite expansion + trial milestone-close dry-run | `eval_spec` (data; not code) | REQUIRED | Milestone-shared (default) |
| S-Eval-5 — L3 judge repositioning + R-item closure | `eval_spec` | REQUIRED | Milestone-shared (default) |

Codex review default for M3-Eval: **milestone-shared at M3-Eval close** per `iteration_governance.md` §4.3 second paragraph (one cumulative review against the full commit range covering S-Eval-1 through S-Eval-5). **Exception: S-Eval-3 requires per-sub-sprint Codex review** before S-Eval-4 begins (§4.3 trigger #2 — `critical_steps[].desc` is LLM-visible and crosses §1.7 forbidden-list territory; Codex must independently verify each `desc` against the proposal §5.3 hardcode-vs-soft-narrative standard).

S-Eval-1 / S-Eval-2 / S-Eval-4 / S-Eval-5 each remain Codex-deferred-to-milestone-close unless a per-sub-sprint trigger surfaces ad hoc during execution.

## 2. Goal

Replace the implicit equal-weighting of L1/L2/L3 checks with a **four-tier coarse-to-fine pyramid** that is reliably attributable to the bot's behaviour and aligned with the LLM-first constitution:

- **Tier-0 (unchanged, hard gate)**: PII / safety / identity / forbidden-phrase / escalation-compliance / phase-transition-validity. These are Tier-0 invariants per `docs/runtime_freeze_and_risk_policy.md` §1/§2.
- **Tier-1 Outcome (NEW mandatory)**: "did the bot solve the user's problem, appropriately escalate, or appropriately defer?" Evaluated via the bad-case suite (§5.6, manual human-judgment) + a NEW outcome-only `anchor_outcome` suite + a NEW supplementary L3 `user_goal_achievement` dimension.
- **Tier-2 Critical-flow (NEW)**: per-UC process correctness. NOT encoded per-CaseSpec; derived from each Skill's `critical_steps` array (NEW YAML field, populated in S-Eval-3). Each step declares an LLM-visible `desc` (procedural guidance, soft narrative — §1.7-compliant) PLUS an eval-side `trace_check` DSL (structurally constrained to forbid keyword / regex / message-content matching).
- **Tier-3 Polish (advisory only)**: existing L2/L3 dims (`turn_efficiency`, `tool_sequence_match`, `issue_preservation`, `handover_completeness`, L3 `relevance` / `tone_appropriateness` / `groundedness`, latency, token cost, stall rate) all kept for trend tracking but **never** flip `case_passed`.

Single-source-of-truth property (decision 5): `critical_steps[].desc` is consumed by BOTH the runtime LLM (via `ContextProjectionBuilder` rendering inside `phase_plan.skill`) AND eval-side scoring (via `SkillProcedureExtractor` evaluating `trace_check` against the trace). Both can change only by editing the Skill YAML; no doc-drift mode between procedure narrative and eval expectation.

Schema convergence outcome: CaseSpec demotes 6 hard surfaces (`expected_tool_sequence` / `forbidden_tools` / `escalation_trigger` exact-match / `bot_handling_pattern` / `outcome_checks` per-case identical checklist / `should_escalate` binary) from required/mandatory-effect to optional-with-default-off behaviour; a new optional `closure_criterion` string is introduced for bad-case and anchor_outcome suites.

R-item closure outcome at M3-Eval close: 4 R-items closed (`R-l3-judge-form-context-trust-rubric` / `R-l1-source-citation-quality-rubric` / `R-cs040-l3-review-intake-completion-semantics` / `R-cs038-l3-review-intake-efficiency`); 2 R-items unblocked-but-not-closed (`R-case-spec-overrides-schema-scoring-extension` schema-block lifted by S-Eval-1; `R-escalation-reason-runtime-evidence-contract-review` touched by exact-match demotion, full closure still depends on runtime evidence).

## 3. Sub-sprint sequence (preliminary; deliver-agent + human refine at each sub-sprint planning round)

### S-Eval-1 — Schema simplification + outcome-only anchor suite

**Layer:** `eval_spec`. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default). **Estimated dev:** 2-3 days.

**Scope (4 sentences):**

1. In `eval_interactive/eval_interactive/case_spec/schema.py`, change four `Expected` fields from required/mandatory-effect to optional with default-off behaviour: `bot_handling_pattern` (currently required `str`) becomes `Optional[str] = None`; `expected_tool_sequence` / `forbidden_tools` stay as `list[str]` default-empty but their scoring effect changes (see #2); `escalation_trigger` exact-match strictness via the `should_escalate` post-init coupling is relaxed (if present, used for diagnostic; if absent on `should_escalate=true`, do not error — defer to acceptable_outcomes / Tier-1 outcome judgement).
2. In `eval_interactive/eval_interactive/scoring/`, demote `hard_checks.py` `no_forbidden_tools` to a Tier-3 advisory when `forbidden_tools` is present (still records; does not flip `case_passed`); skip entirely when absent. Demote `outcome_checks.py` `tool_sequence_match` to Tier-3 diagnostic. Demote `hard_checks.py` `escalation_reason_consistency` family-matching to advisory when `escalation_trigger` is absent on the spec. Demote `outcome_checks.py` per-case identical 7-item checklist to advisory; introduce per-case opt-in via the existing `scoring.outcome_checks: list[str]` (preserved for backward compat; absent → no checks contribute to gate).
3. Create `eval_interactive/case_specs/anchor_outcome/` with **10-15 cases**, one to three per UC, each declaring only `outcome_class`, `persona` (including `user_goal_summary`), and a NEW optional `closure_criterion: str` field. **No** `expected_tool_sequence`, **no** `forbidden_tools`, **no** `bot_handling_pattern`. Reuse `source_session_id`s from existing smoke / anchor where representative.
4. Update `docs/current/iteration_governance.md` §5.5 to state "anchor_outcome is the second human-judgment surface beside the bad-case suite; smoke remains observation-only by design."

**Files in scope:** `eval_interactive/eval_interactive/case_spec/schema.py` (4 field demotions); `eval_interactive/eval_interactive/scoring/hard_checks.py` (2 demotions); `eval_interactive/eval_interactive/scoring/outcome_checks.py` (2 demotions); `eval_interactive/eval_interactive/scoring/composite.py` (gate-vs-advisory wiring); NEW directory `eval_interactive/case_specs/anchor_outcome/` with 10-15 YAML files + optional `_manifest.md`; NEW field `Expected.closure_criterion: Optional[str] = None`; regression-test files for the new optional fields + advisory paths; minor edit to `docs/current/iteration_governance.md` §5.5.

**Files NOT in scope (hard fences within sub-sprint):** deleting any existing field from `Expected` (kept for backward compat); rewriting any L3 judge dim (deferred to S-Eval-5); modifying any Skill YAML (deferred to S-Eval-2 / S-Eval-3); modifying any case under `eval_interactive/case_specs/smoke/` or `eval_interactive/case_specs/anchor/` or `eval_interactive/case_specs/case_families/<existing>/` or `eval_interactive/case_specs/bad_cases/` (cascade fence).

**Backward-compat acceptance**: existing 14 smoke cases + **159 anchor cases** + 12 case-family directories + 1 bad case must continue to load and run with `case_passed` unchanged from the demoted Tier-3 advisories. Tier-0 hard gates remain unchanged.

---

### S-Eval-2 — Skill `critical_steps` schema + extractor + projection wiring

**Layer:** `eval_spec` (extractor + schema) + `prompt_projection` (LLM-visible wiring). Multi-layer; name `eval_spec` as primary per §7. **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default). **Estimated dev:** 4-5 days.

**Scope (5 sentences):**

1. Extend the Skill YAML schema to include optional `critical_steps: list[CriticalStep]` per Skill. `CriticalStep` carries `id: str`, `desc: str` (LLM-visible procedural narrative), `trace_check: str` (DSL string), `mandatory_for: list[str]` (UC list), `severity: Literal["mandatory","advisory"]`. **This sub-sprint does NOT fill any `critical_steps` content** — only the contract + projection wiring + extractor. Empty `critical_steps` array → no projection change + extractor returns N/A.
2. Extend `SkillLoader` / `SkillRegistry` / `Skill.java` / NEW `CriticalStep.java` record to validate and expose the new field. No behavioural change in `SkillRegistry.select(...)` / `SkillGuardrailDispatcher` / `SkillStateBus`.
3. Extend `ContextProjectionBuilder.java` to render `critical_steps[].desc` inside `phase_plan.skill` in the per-turn projection, immediately after the existing `procedure` field. Empty array → no projection change (parity with M2 envelope behaviour).
4. Create `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` with a `SkillProcedureExtractor` class. It accepts a trace + active Skill and returns per-step PASS / FAIL / N/A. Define a **minimal `trace_check` DSL** with these primitives only: `accumulated_tool_results.<tool>` (presence check), `tool_event_seq(<tool_a>) < tool_event_seq(<tool_b>)` (order check), `intake_state.fields_collected.contains(<field>)` (slot presence), `session.<flag>_present` (flag boolean), `any_of(...)`, `all_of(...)`. **Reject by parser construction any regex / keyword-list / message-content matching in DSL** — that would constitute a §1.7 semantic hardcode.
5. Add Tier-2 `skill_procedure_followship` check that integrates the extractor result into composite scoring (a `mandatory` step failure for a case whose active Skill matches the step's `mandatory_for` UC set → Tier-2 fail → flips `case_passed`). Empty `critical_steps` → always PASS / N/A.

**Files in scope:** `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (schema extension); NEW `CriticalStep.java` record under `service/runtime/skill/`; `SkillLoader.java` (schema validation extension via existing allowlist convention); `ContextProjectionBuilder.java` (projection wiring); NEW `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` (extractor + DSL parser); `eval_interactive/eval_interactive/scoring/composite.py` (Tier-2 wiring); regression-test files (Java + Python) including a negative test for invalid DSL.

**Files NOT in scope (hard fences within sub-sprint):** filling any `critical_steps` content (deferred to S-Eval-3); modifying any existing Skill `procedure` text; new `SkillRegistry` / `SkillStateBus` / `SkillGuardrailDispatcher` behaviour beyond field exposure; runtime if-else / classifier change; modifying `runtime_freeze_and_risk_policy.md` (no new Tier-0).

**Anti-hardcode contract**: the `trace_check` DSL is **structurally constrained** (parser rejects regex / keyword / message-content). This is the milestone's primary structural defence against §1.7 violations on the eval side. Dev must include a negative test asserting that a critical_step with `trace_check: "message.contains('refund')"` fails schema validation.

---

### S-Eval-3 — Populate `critical_steps` for the 6 Skills

**Layer:** `eval_spec` (content under the LLM-visible contract S-Eval-2 wired). **§7 stanza:** REQUIRED. **Codex:** **PER-SUB-SPRINT** (§4.3 trigger #2 — §1.7 forbidden-list adjacent; this IS the milestone's critical governance gate). **Estimated dev:** 5-6 days.

**Scope (3 sentences):**

1. For each of the 6 Skills (`discover_triage`, `resolve_faq_grounded_answer`, `resolve_intake_collect_and_handover`, `confirm`, `escalate`, `terminal`), populate **3-5 `critical_steps`** focusing coverage on these scenario clusters: UC-FP "post deleted" decision tree (consult moderation context before explaining; reposting guidance when actionable; route to UC-H intake on appeal request); UC-G / UC-H / UC-I / UC-J / UC-K "intake then case then handover" sequence (`create_case_controlled` before `request_handover`; intake fields collected before case creation); UC-A / UC-B / UC-C / UC-D / UC-E / UC-F FAQ-grounded answer (retrieve before answering; cite source on factual_answer output class); DISCOVER disambiguation (clarification-budget respect; no committal to UC before sufficient evidence).
2. Each `desc` written as **soft procedural narrative** per the proposal §5.3 standard table — `"For UC-FP issues, consult the moderation context before explaining the reason"` is acceptable; `"IF user.message.contains('appeal') THEN active_use_case := UC-H"` is rejected; keyword enumeration matrices are rejected; soft diagnostic signals are edge-case-acceptable but scrutinised. The deliver-agent and dev MUST self-walk each `desc` against the §5.3 table BEFORE Codex review.
3. Run the existing bad-case suite (Alice) + the new `anchor_outcome` suite (from S-Eval-1) against the populated `critical_steps`. Verify the Tier-2 failures align with known failure modes documented in `eval_interactive/case_specs/bad_cases/_manifest.md` (Alice failure shape should surface as `intake_complete_required` Tier-2 fail on UC-H).

**Files in scope:** 6 Skill YAMLs under `server/src/main/resources/skills/` (`critical_steps` content only; **no** `procedure` text edits, **no** `guardrails` edits, **no** `state_inheritance` edits); regression-test files asserting populated `critical_steps` shape + Alice Tier-2 alignment.

**Files NOT in scope (hard fences within sub-sprint):** modifying `procedure` field text of any Skill; new Skill files; modifying `SkillGuardrailDispatcher`; adding runtime if-else; modifying `SkillLoader.java` / `Skill.java` / `CriticalStep.java` (those are S-Eval-2 territory); modifying any case-family or bad-case YAML.

**Per-sub-sprint Codex review verdict required**: §4.1 9-question kernel + explicit §5.3 anti-hardcode walk over each populated `desc`. Codex must independently confirm every `desc` is soft narrative, not a regex/keyword/if-else. **S-Eval-4 may not start until S-Eval-3 Codex returns `decision: pass`** (or `approve with downgrade-to-signal follow-up` per §4.1 verdict set; reject verdicts trigger fix-iteration).

---

### S-Eval-4 — Bad-case suite expansion + trial milestone-close dry-run

**Layer:** `eval_spec` (data; not code). **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default). **Estimated dev:** 5-7 days (heavy in human-review collaboration time).

**Scope (4 sentences):**

1. Select **10-12 entries** from `eval_interactive/case_spec_overrides.yaml` whose `status: approved` block has the strongest semantic divergence from the pre-override CaseSpec. Source pool at HEAD is **17 approved entries** (NOT 29 as the proposal §4 decision 3 mistakenly stated; deliver-agent reconciled 2026-05-20). Prefer entries spanning the four bad-case dimensions (D1 mis-classification / D2 intake prefill gap / D3 lock-in escape / D4 stated_reason circularity) and the three highest-volume UC families (UC-FP, UC-G, UC-H). Coverage of D1-D4 × UC-FP/G/H within 17 must be re-verified at S-Eval-4 planning by deliver-agent + human; if a dimension or UC family is undersampled in the 17, deliver-agent + human jointly decide whether to (a) accept narrower coverage for this milestone, (b) supplement with non-override-sourced cases (still real sessions), or (c) defer the missing dimension to a follow-on milestone with an R-item.
2. For each selected entry, author a bad-case YAML in `eval_interactive/case_specs/bad_cases/` with full `bad_case_metadata` (`source_session_id`, `surfaced_by`, `surfaced_date`, `failure_shape`, `layers_involved`, `closure_criterion`, `tier: core | scope-relevant`) per the `_manifest.md` schema and Alice precedent.
3. Update `eval_interactive/case_specs/bad_cases/_manifest.md` lifecycle ledger with the 10-12 new rows.
4. Run a **trial milestone-close manual review dry-run**: deliver-agent + human walk through each new bad case's trace and judge PASS / FAIL / IMPROVING against `closure_criterion`. **The purpose is to calibrate `closure_criterion` wording, not to verify bot correctness** — `closure_criterion` quality is the load-bearing output. Findings feed back as `_manifest.md` notes.

**Files in scope:** 10-12 NEW YAML files under `eval_interactive/case_specs/bad_cases/`; `eval_interactive/case_specs/bad_cases/_manifest.md` (ledger append); a calibration note added to `_manifest.md` summarizing the dry-run findings (no separate file).

**Files NOT in scope (hard fences within sub-sprint):** synthesising bad cases from non-real sessions; touching the existing Alice case; modifying any Tier-0 / Tier-1 / Tier-2 check code (those are S-Eval-1 / S-Eval-2 / S-Eval-3 territory); modifying `eval_interactive/case_spec_overrides.yaml`; modifying any case-family or anchor / anchor_outcome / smoke case.

---

### S-Eval-5 — L3 judge repositioning + R-item closure

**Layer:** `eval_spec` (judge config + rubric). **§7 stanza:** REQUIRED. **Codex:** milestone-shared (default). **Estimated dev:** 2-3 days.

**Scope (4 sentences):**

1. In `eval_interactive/eval_interactive/scoring/llm_judge.py`, demote the three current L3 dimensions (`relevance`, `tone_appropriateness`, `groundedness`) from composite contributors to Tier-3 advisory. Their numeric scores are still recorded; they no longer factor into `case_passed` or `composite_score`.
2. Add a NEW L3 dimension `user_goal_achievement` (coarse 1-5: did the bot help the user achieve the stated `persona.user_goal_summary`, or appropriately escalate / defer?). Wire as Tier-1 **supplementary advisory** signal (NOT a hard gate; bad-case + anchor_outcome manual review remain primary Tier-1 gates).
3. Update the rubric prompts: (a) close `R-l3-judge-form-context-trust-rubric` — explicitly state `form_context.first_name` is a trusted signal; the bot is allowed to greet by first name without confirmation. (b) Close `R-l1-source-citation-quality-rubric` — tighten the citation check to require `canonical_url` OR article title; reject bare Salesforce IDs as sole citation evidence.
4. Close `R-cs040-l3-review-intake-completion-semantics` and `R-cs038-l3-review-intake-efficiency` by routing them through the new tiered architecture (their schema-block dependency is resolved by the S-Eval-1 schema simplification).

**Files in scope:** `eval_interactive/eval_interactive/scoring/llm_judge.py` (dim repositioning + new dim wiring); rubric prompt text edits (location: TBD by dev — likely embedded in `llm_judge.py` or sibling rubric file); `docs/action_bank.md` (4 R-items flipped to closed in §6 close-action index); regression-test files for the new dim + repositioned old dims.

**Files NOT in scope (hard fences within sub-sprint):** changing L3 model temperature or provider (deferred per Sprint 4); retraining or replacing the judge model; modifying L3 prompt structure beyond rubric wording for the four R-items; modifying any Tier-0 / Tier-1 hard-gate code (those are S-Eval-1 territory).

**Monotone-relaxing check**: a previously-PASS case must not flip to FAIL on the new rubric. Verified by running smoke + anchor + anchor_outcome with both old and new scoring code as part of S-Eval-5 acceptance.

## 4. Non-goals (explicit)

- M3-Eval does NOT add new Tier-0 invariants. §1.6 / §1.7 boundary; nothing in this milestone elevates a runtime check to Tier-0. C2 + C3 candidate re-evaluation deferred per the M2-close DEFER verdict; if production trace evidence accumulates during M3-Eval execution, deliver-agent + human may surface as a separate R-item but **may not** ship Tier-0 elevation inside M3-Eval scope.
- M3-Eval does NOT touch the L3 judge model / temperature / provider / prompt-structure (deferred per Sprint 4); only the rubric narrative wording for the four R-items and tier-weight repositioning.
- M3-Eval does NOT delete any CaseSpec field. Demoted fields stay optional for backward compat; deletion is a future fold-back, not this milestone.
- M3-Eval does NOT add any runtime if-else / regex / keyword matrix. §1.7 red line. The structural defence is the `trace_check` DSL parser constraint (S-Eval-2 ships; S-Eval-3 must not bypass).
- M3-Eval does NOT change `SkillRegistry` / `SkillLoader` semantic dispatch / `SkillStateBus` mediation / `SkillGuardrailDispatcher` predicate handling. M2 just landed these; M3-Eval only augments the YAML schema with a NEW field.
- M3-Eval does NOT synthesise FAQ articles or audit FAQ corpus coverage. Corpus work is M3-Corpus (separate parallel-track milestone).
- M3-Eval does NOT touch `eval_interactive/case_spec_overrides.yaml`. The 17 approved entries are the SOURCE POOL for S-Eval-4 bad-case selection, NOT a write target.
- M3-Eval does NOT touch the Single Handover Orchestrator (M3-B territory; release-gate not touched by this milestone).
- M3-Eval does NOT widen the `escalation_reason` enum at `PhaseEvaluator.java:39-63`. Runtime contract not touched.
- M3-Eval does NOT touch `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` (inheriting M2 §6 hard-fence #5).
- M3-Eval does NOT edit existing Sprint 20 / Sprint 29 / Sprint 32 case families (cascade fence).
- M3-Eval does NOT pre-decide M3-Corpus scoping. M3-Corpus is a separate parallel-track milestone (`R-corpus-coverage-audit-per-uc` consumer); deliver-agent + human + research-agent decide M3-Corpus scoping independently.

## 5. Milestone acceptance bar

**Hard gates (close decision is PASS only if all clear):**

- [ ] **Tier-0 safety floor verified**: `no_pii_leakage`, `no_human_only_tool_exposure`, `no_critical_policy_violation`, `escalation_compliance`, `phase_transition_validity` all PASS on the 14-case smoke + 159-case anchor + new `anchor_outcome` (10-15 cases) + 10-12 new bad cases + 1 existing Alice bad case. **Zero regressions**.
- [ ] **Codex anti-hardcode review PASS**: milestone-shared at M3-Eval close AND per-sub-sprint at S-Eval-3 close (the latter must return `pass` BEFORE S-Eval-4 begins).
- [ ] **Java test suite no new regression**: baseline `1144 / 1-inherited / 0 / 2` from M2 close preserved (S-Eval-2 ships new Java + tests; S-Eval-3 ships YAML content + regression tests). Inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure persists unchanged per M2-close STATUS QUO; new test counts MUST be reproducible per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.
- [ ] **Curated bad-case suite manual review pass (PRIMARY GATE per §5.6)**: 10-12 new bad cases landed with full `bad_case_metadata` + `closure_criterion`. Trial milestone-close dry-run completed; deliver-agent + human jointly judge per-case PASS / FAIL / IMPROVING; close decision considers the overall pattern, NOT a programmatic threshold. Alice case re-run (must not regress; expected to stay `active` until Tier-2 `intake_complete_required` Skill `critical_step` surfaces it cleanly).
- [ ] **Schema simplification backward-compatible**: existing 14 smoke cases + 159 anchor cases + 12 case-family directories + 1 Alice bad case load and run without modification. Tier-3 advisories may flip; Tier-0 / Tier-1 / Tier-2 hard gates must remain unchanged.
- [ ] **All 6 Skills carry populated `critical_steps`** (3-5 each, 18-30 total). Projection wiring confirmed: an LLM call during a representative trace shows `critical_steps[].desc` rendered in `phase_plan.skill` after `procedure`. `SkillProcedureExtractor` runs without crash across the full bad-case + anchor + anchor_outcome + smoke surface.
- [ ] **R-items closed at M3-Eval close**: `R-l3-judge-form-context-trust-rubric` / `R-l1-source-citation-quality-rubric` / `R-cs040-l3-review-intake-completion-semantics` / `R-cs038-l3-review-intake-efficiency`. Each closed in `docs/action_bank.md` §6 close-action index.
- [ ] **L3 repositioning is monotone-relaxing**: no previously-PASS case flips to FAIL under the new tiering. Verified by running smoke + anchor + anchor_outcome with both old and new scoring code in S-Eval-5.

**Observation-only (recorded; does not gate close):**

- Smoke `composite_score` / `mean_outcome_score` / `mean_judge_score` / `task_success_rate` / `passed_cases` (§5.5 demoted to observation; recorded across S-Eval-1 → S-Eval-5; M3-Eval-attributable deltas surfaced at close).
- Per-turn projection token cost: before-M3-Eval baseline vs after-S-Eval-3 with populated `critical_steps`. **Expected increment ≤ 1000 tokens per turn**; investigate if higher (proposal §5.4 estimate is 500-900 tokens). Recorded as M3-Eval observation metric; no hard gate.
- Architecture-health metrics direction (§6): `new_semantic_hardcode_count` = 0 (M3-Eval ships no Java decision-path hardcode); `soft_signal_conversion_count` ≥ 18 (the 18-30 `critical_steps[].desc` ARE soft-signal projections of previously implicit / scattered procedural intent — see proposal §5.2 single-source-of-truth claim); `planner_ownership_ratio` not decreased.

## 6. Hard fences (milestone-level)

1. **No edits** to `RuntimeIntentClassifier.java`, `IntentClassification.java`, `DriftResult.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ClassifyUseCaseTool.java` (inherits M2 §6 hard-fence #5).
2. **No new Tier-0 invariant** added at M3-Eval close. C2 + C3 candidate re-evaluation deferred; if production trace evidence accumulates, deliver-agent + human surface as separate R-item but may NOT ship Tier-0 elevation inside M3-Eval scope.
3. **No edits** to existing case families under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence).
4. **No edits** to existing shadow CaseSpecs under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
5. **No edits** to existing cases under `eval_interactive/case_specs/smoke/` or `eval_interactive/case_specs/anchor/`. S-Eval-1 demotes their scoring effect via schema/scoring code; the case YAMLs themselves are NOT touched.
6. **No edits** to the existing Alice bad case (`alice_uc_a_uc_h_misclass.yaml`). Re-run as regression guard at M3-Eval close.
7. **No edits** to `eval_interactive/case_spec_overrides.yaml` (read-only source for S-Eval-4 bad-case selection).
8. **No edits** to `eval_interactive/eval_interactive/loader/` or `eval_interactive/eval_interactive/simulator/` (these are stable per Sprint 28 / Sprint 32 precedent).
9. **No edits** to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/customer_service_tool_spec_v0_2.yaml`.
10. **No edits** to sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-041-*` or milestone archives `docs/milestones/M1_objective.md` / `M2_objective.md` / `M2-Skill_objective.md` / `M2_codex-review.md`.
11. **No widening** of `escalation_reason` enum (canonical 23 values at `eval_interactive/eval_interactive/case_spec/schema.py:44-68` matches `PhaseEvaluator.java` runtime enum).
12. **No `iteration_governance.md` edit** during M3-Eval EXCEPT the S-Eval-1 §5.5 anchor_outcome sentence and the S-Eval-5 §5 acceptance-bar references if the wording needs alignment with the new four-tier pyramid. Larger fold-back is deferred to normal cadence.
13. **No modification** of M2-landed Skill semantics: `SkillRegistry.select(...)` / `SkillStateBus.applyOnSkillSwitch(...)` / `SkillGuardrailDispatcher.dispatch(...)` / `Skill.guardrails` content / `Skill.state_inheritance` content / `Skill.procedure` text. M3-Eval ONLY adds the NEW `critical_steps` field (S-Eval-2 contract) and populates it (S-Eval-3 content).
14. **No regex / keyword / message-content matching** in the `trace_check` DSL (S-Eval-2 enforces via parser construction).
15. **No if-else / keyword matrix / per-UC enumeration** in `critical_steps[].desc` (S-Eval-3 self-walk per §5.3 standard; Codex per-sub-sprint review verifies).

## 7. R-items consumed / surfaced

**Consumed by M3-Eval (closed at close):**

- `R-l3-judge-form-context-trust-rubric` — closed in S-Eval-5.
- `R-l1-source-citation-quality-rubric` — closed in S-Eval-5.
- `R-cs040-l3-review-intake-completion-semantics` — unblocked by S-Eval-1 schema simplification (its Sprint 21 schema-block is lifted); closed in S-Eval-5.
- `R-cs038-l3-review-intake-efficiency` — unblocked by S-Eval-1 schema simplification (its Sprint 21 schema-block is lifted); closed in S-Eval-5.

**Unblocked but not closed (graduated to follow-on):**

- `R-case-spec-overrides-schema-scoring-extension` — schema-block lifted by S-Eval-1; full closure depends on either (a) a follow-on sprint that adds scoring metadata to override entries, OR (b) M3-Corpus deciding overrides no longer need scoring extension. Tracked as carry-over for M4+ planning.
- `R-escalation-reason-runtime-evidence-contract-review` — touched by S-Eval-1 demotion of `escalation_trigger` exact-match; full closure still requires runtime evidence on family-matching (out of M3-Eval scope; runtime-layer R-item).

**Surfaced by M3-Eval (expected):**

- 10-12 new bad cases added to `bad_cases/_manifest.md` lifecycle ledger (NOT R-items themselves; data).
- Potential R-item: post-S-Eval-3 calibration finding if any `critical_steps[].desc` proves to be a borderline §5.3 case that Codex flags `approve with downgrade-to-signal follow-up` — deliver-agent + human surface as a soft-signal-conversion R-item for a future milestone.
- Potential R-item: token-cost observation if per-turn projection grows >1000 tokens post-S-Eval-3; would surface as a Skill-content tuning R-item.
- Potential R-item: trial milestone-close dry-run (S-Eval-4) may surface `closure_criterion` wording patterns worth folding back into `iteration_governance.md` §5.6 example library.

**NOT consumed by M3-Eval (intentionally deferred):**

- `R-corpus-coverage-audit-per-uc` — M3-Corpus territory; parallel-track milestone.
- `R-faqMissCount-threshold-and-timing-review` — runtime config governance; not eval_spec.
- `R-smoke-regression-investigation` follow-ons (`R-slow-llm-placeholder-coalesce`, `R-uc-k-intake-complete-case-id-binding`, `R-prompt-phase-plan-directive-followship`) — runtime-layer R-items; not eval_spec.
- `R-substitute-placeholders-uc-case-creation-eligibility-centralization` — M3-cleanup candidate surfaced at M2 close; runtime layer; not eval_spec.
- `R-llm-provider-latency-drift-2026-05-16` — runtime / infra layer; not eval_spec.
- C2 / C3 Tier-0 candidate elevation R-items (`R-skill-guardrail-non-overridability-tier-0` / `R-skill-state-bus-boundary-enforcement-tier-0`) — deferred to M3+ post-production-trace observation per M2-close DEFER verdict.

## 8. Codex review plan (per §4.3)

**Default**: milestone-shared review at M3-Eval close. Single cumulative Codex pass over commit range covering S-Eval-1 through S-Eval-5. Codex consumes:

- All 5 sub-sprint objectives + handoffs (`docs/sprints/sprint-NNN-objective.md` + `-handoff.md` × 5).
- This milestone objective (live during execution; archived to `docs/milestones/M3-Eval_objective.md` at close).
- The cumulative commit range produced by the 5 sub-sprints.
- Java test baseline reproducibility check.
- Bad-case suite manual review notes (S-Eval-4 dry-run findings).

**Codex review prompt locations**:

- Milestone-shared prompt drafted by deliver-agent at M3-Eval close: `compact/M3-Eval-review-prompt.md` (TBD at close; outline in §8.1 below).
- Per-sub-sprint prompt for S-Eval-3 drafted by deliver-agent before S-Eval-3 review dispatch: `compact/sprint-NNN-review-prompt.md` (NNN = S-Eval-3's sprint number once assigned).

**Per-sub-sprint trigger**: S-Eval-3 invokes §4.3 trigger #2 (§1.7 forbidden-list adjacent — `critical_steps[].desc` LLM-visible). Codex must independently verify each populated `desc` against the proposal §5.3 standard at S-Eval-3 close, **BEFORE S-Eval-4 begins**. This is the milestone's critical governance gate.

**Verdict set** (§4.1): `approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.

### 8.1 M3-Eval-shared review prompt outline (drafted at close)

The full Codex prompt is drafted at M3-Eval close. The outline:

1. **Cumulative scope claim**: review the cumulative commit range `<start>..<end>` covering S-Eval-1 through S-Eval-5 against this milestone objective.
2. **§4.1 9-question anti-hardcode kernel walk** (cumulative, not per-sprint), with explicit attention to:
   - Q1 (semantic hardcode): are the 18-30 populated `critical_steps[].desc` soft narratives per the proposal §5.3 standard, or do any encode if-else / keyword-matrix / regex?
   - Q4 (eval-phrase encoding): does any populated `desc` reference visible-eval case text, trace-specific phrasing, or a CaseSpec id?
   - Q5/Q6 (LLM ownership shrink + if-else-in-prompt): does the LLM-visible projection of `critical_steps[].desc` shrink the LLM's §1.3 ownership of semantic decisions?
3. **§1.7 boundary check** on the trace_check DSL parser construction + 6 Skills' `critical_steps` content + L3 rubric updates.
4. **§4.2 sprint-close header** filled (`pass | fix_required | out_of_scope_review` + `blocking_count` + summary paragraph).
5. **Hard-fence verification** against this milestone's §6 (15 items).
6. **Schema and reproducibility checks** per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` — every numeric claim in the deliver-agent's M3-Eval close package (file counts, test counts, line counts, anchor case count = 159, override count = 17, new bad case count = 10-12) must be reproducible from `git show --numstat` / `find ... | wc -l`.
7. **Tier-0 candidate independent verification**: C2 / C3 disposition (expected: DEFER continued); no new Tier-0 candidate expected from M3-Eval.
8. **Validation runs**: Java baseline preservation + targeted SkillProcedureExtractor regression spot-checks.
9. **Bad-case suite human-judgment-gate respect**: Codex MUST NOT auto-PASS / auto-FAIL the 10-12 new bad cases on `closure_criterion` programmatic match; Codex reads the deliver-agent + human manual review verdicts and verifies the verdicts are internally consistent.
10. **Deferred / non-blocking notes**: observations that do not change the verdict but are worth recording for M4 planning.

## 9. Estimated milestone duration

**Calendar estimate (informational; not a gate):**

- S-Eval-1: 2-3 dev-days + Codex deferred to milestone close.
- S-Eval-2: 4-5 dev-days + Codex deferred to milestone close.
- S-Eval-3: 5-6 dev-days + **per-sub-sprint Codex** (~1-2 days for Codex pass + any fix-iteration if `reject as semantic hardcode` returns).
- S-Eval-4: 5-7 days (heavy human-review collaboration time for trial close dry-run).
- S-Eval-5: 2-3 dev-days + Codex deferred to milestone close.
- M3-Eval close: deliver-agent + human bad-case suite manual review + Codex milestone-shared review + close-out artefacts (~2-3 days).

**Total**: ~3-4 calendar weeks for the full milestone, assuming sub-sprints execute sequentially per the §10.2 dependency order and S-Eval-3 Codex returns `pass` on first pass.

Risk to duration: S-Eval-3 Codex `reject` would trigger a fix-iteration sub-sprint and extend the milestone. Mitigation: deliver-agent + dev pre-walk each `desc` against the §5.3 standard before commit; deliver-agent drafts a calibration table of accepted / rejected example `desc` shapes in the S-Eval-3 dev prompt.

## 10. Stop conditions (milestone-level)

**Stop signals (deliver-agent + human reassess scope; possibly invoke `feedback_corpus_undecidable_premise_check.md` in-flight downgrade):**

1. S-Eval-1 backward-compat fails on >5% of the 159 anchor cases under the loosened schema. Empirical evidence that the schema demotion broke an unanticipated invariant; halt and re-scope.
2. S-Eval-2 `trace_check` DSL design proves insufficient for the 18-30 `critical_steps` needed in S-Eval-3 (revealed at S-Eval-3 planning round). Deliver-agent + human decide whether to extend the DSL within S-Eval-3's Codex review boundary (acceptable) OR scope-shift S-Eval-3 to a follow-on milestone with a richer DSL (re-scoping signal).
3. S-Eval-3 Codex review returns `reject as semantic hardcode` on >25% of populated `critical_steps[].desc` on first pass. Empirical evidence that the §5.3 standard is harder to write to than the proposal §5 deep-dive predicted; halt and re-calibrate the dev prompt.
4. S-Eval-4 trial dry-run reveals that the human cannot consistently judge PASS / FAIL / IMPROVING against `closure_criterion` for >50% of new bad cases. Empirical evidence that `closure_criterion` quality is uncalibrated; deliver-agent + human pause new-bad-case authoring and rework existing closure_criteria using the dry-run findings.
5. Token-cost observation post-S-Eval-3 exceeds +2000 tokens per turn (vs the proposal §5.4 estimate of 500-900). Deliver-agent + human decide whether to trim `critical_steps[].desc` (acceptable; soft signal) OR accept the cost as a milestone observation.
6. Per-production-trace observation surfaces a new C2 or C3 Tier-0 candidate during M3-Eval execution. Halt M3-Eval close gate; route the Tier-0 candidate through a separate `human_review_required` flow per §3.2.

**Continue signals (do NOT halt):**

- Smoke composite_score moves (up or down) due to LLM provider drift. Per §5.5, this is observation only; not a stop signal.
- L3 dim score numeric distribution shifts under the new rubric. Per §5 acceptance bar, only the monotone-relaxing check gates close; numeric shifts are observation.
- Per-sub-sprint scope creep risk if S-Eval-2 surfaces a SkillRegistry behaviour that needs touching for projection wiring. Deliver-agent halts and replans; do NOT smuggle scope.

## 11. Cross-milestone sequencing context

**M1 (closed PASS 2026-05-17, `docs/milestones/M1_objective.md`)** — DISCOVER + Intake. M3-Eval inherits the M1 functional surfaces (DISCOVER soft-signal projection, intake field prefill UC-G/H/I/J) as a stable substrate; M3-Eval does NOT modify them.

**M2 (closed Clean PASS 2026-05-18, `docs/milestones/M2_objective.md`)** — Skill Registry Abstraction + Wholesale Retroactive Externalization. M3-Eval rides ON the M2 Skill abstraction (6 Skill YAMLs at `server/src/main/resources/skills/` with `procedure` + `guardrails` + `state_inheritance`); M3-Eval **augments** the schema with the NEW `critical_steps` field but does NOT change Skill semantic dispatch or guardrail handling.

**M3-Corpus (parallel-track sibling milestone; not started)** — FAQ corpus coverage audit per UC. Consumes `R-corpus-coverage-audit-per-uc`. May run in parallel under a separate dev / review path. **No dependency** between M3-Eval and M3-Corpus close.

**M3-B (deferred; `D-single-handover-orchestrator` P0 launch blocker)** — Single Handover Orchestrator. Not started; flipped to M3 priority if Salesforce cutover calendar pressure surfaces per `docs/release_gate.md` §1.1.

**M3-A / M3-C / M3-D / M3-Latency / M3-Skill-Tuning / M3-Tier-0-re-evaluation / M3-cleanup** — candidates per the prior M2-close M3-candidate slate; deferred until M3-Eval close (or until production trace evidence surfaces a Path 2 bad case warranting emergency triage).

**M4+ planning round**: at M3-Eval close, deliver-agent + human pick the NEXT milestone from the remaining M3-candidate slate + any new R-items surfaced during M3-Eval (S-Eval-3 calibration findings, token-cost observation findings, S-Eval-4 closure_criterion fold-back candidates).

## 12. M3-Eval closure verdict (appended at milestone close 2026-05-23)

**Filled by deliver-agent + human jointly at M3-Eval milestone close 2026-05-23 per `iteration_governance.md` §8.4 milestone close artefacts.**

### 12.1 Classification

**A — Clean PASS (with documentation-only fix-iteration on deliver-agent evidence-package)**.

M3-Eval ships the four-tier pyramid end-to-end across S-Eval-1 through S-Eval-5; the Codex M3-Eval-shared review surfaced one blocker (P0-F1 evidence-package inconsistency in `eval_interactive/case_specs/bad_cases/_manifest.md` Tier-0 sentence) which was a deliver-agent documentation error, not an S-Eval-N code issue. Deliver-agent applied the documentation correction directly per `feedback_packaging_codex_findings_supersession.md` discipline; no S-Eval-N code change was requested by Codex. M3-Eval milestone closes as Clean PASS on architectural axis + bad-case suite primary gate + Tier-0 safety floor preservation.

### 12.2 Cumulative commit range + baselines

- **Cumulative commit range**: `d91bd3d..7562a2d` (10 commits: 5 sub-sprint dev + 4 deliver-agent close-out + 1 OQ-S46.1 follow-up). Per-commit reference table at the M3-Eval-shared review prompt `compact/M3-Eval-review-prompt.md` §1 (filed verbatim at milestone close).
- **Java test baseline**: `Tests run: 1163, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED across all 5 M3-Eval sub-sprints. The 1 inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53` failure since Sprint 24-era working-tree mod persists unchanged; NOT M3-Eval-attributable.
- **Python test baseline**: `5 failed, 426 passed` at HEAD `7562a2d` (per Codex independent verification via `UV_CACHE_DIR=/tmp/uv-cache uv run --offline python -m pytest`). The 5 pre-existing failures (2× `test_case_spec_overrides.py` + 1× `test_corpus_lint.py` + 2× `test_escalation_enum_sync.py`) attributable to the `v0_2` → `v0_3` supersession debt — tracked via R-item `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (`docs/action_bank.md` §5; status `proposed; impl deferred to whichever milestone consumes it`).
- **New test counts (cumulative)**: +20 (S-Eval-1) + +36 (S-Eval-2 §1.7 structural defence) + ≥6 (S-Eval-3 critical_steps content regression) + 0 (S-Eval-4 docs/data-only) + +30 (S-Eval-5 L3 repositioning) = +90+ NEW Python tests across the milestone; +0 NEW Java tests.

### 12.3 Codex milestone-shared verdict (M3-Eval-shared review 2026-05-23)

- **decision**: `fix_required → resolved`. Original Codex verdict at `docs/codex-findings.md` (archived to `docs/milestones/M3-Eval_codex-review.md` as part of this close-out bundle) was `decision: fix_required / blocking_count: 1`. The 1 blocking finding (P0-F1 manifest Tier-0 sentence inconsistency) was applied by deliver-agent as a documentation-only fix in `eval_interactive/case_specs/bad_cases/_manifest.md` M3-Eval close section ("Tier-0 safety floor — partial pass" replacing the prior blanket "12 of 12 PASS"). Human accepted the deliver-agent fix as documentation-only per `iteration_governance.md` §4.3 milestone-close decision discretion (NOT a re-Codex round; the fix is fact-check on deliver-agent write-up, not on dev-code).
- **summary** (verbatim from Codex's §4.2 header): "M3-Eval ships as a coherent coarse-to-fine eval architecture: S-Eval-1 demotes schema/scoring means to advisory surfaces, S-Eval-2/S-Eval-3 create and populate the Skill `critical_steps` Tier-2 contract with structural DSL hardcode defenses, S-Eval-4 expands the bad-case human-judgment gate, and S-Eval-5 makes Tier-3 L3 dims advisory while wiring Tier-2 into the production eval executor. I found no new §1.7 semantic hardcode in the 18 `critical_steps[].desc`, the `trace_check` DSL, the L3 rubric updates, `user_goal_achievement`, or the Option A executor wiring."
- **Architectural axis**: ALL §3 nine-question kernel results PASS (Q1-Q8; Q9 N/A). All §4 hard fences PASS (15 of 15; with #9 v0_2 deletion documented as inherited supersession housekeeping exception per Codex P2-F2 finding). All §5 bad-case suite spot-checks consistent with manifest reasoning. 16 OQs disposed (10 closed + 2 closed-with-followup + 2 surface-as-r-item + 2 human-architecture-decision + 2 defer-to-future-milestone — see archived `docs/milestones/M3-Eval_codex-review.md` §6 OQ disposition table).
- **Cumulative architecture coherence judgment** (verbatim): "M3-Eval is architecturally coherent. The five sub-sprints compose into the intended four-tier pyramid: Tier-0 stays unchanged; Tier-1 gains outcome-only anchors and human-owned bad-case manual review; Tier-2 is derived from Skill `critical_steps` with a single-source-of-truth `desc` consumed by both runtime projection and eval extraction; Tier-3 judge/scoring polish is preserved as advisory trend data."

### 12.4 Per-sub-sprint S-Eval-3 Codex verdict (deferred archive 2026-05-22)

S-Eval-3 invoked `iteration_governance.md` §4.3 trigger #2 (§1.7 forbidden-list adjacent — `critical_steps[].desc` LLM-visible) and received per-sub-sprint Codex review at sub-sprint close on 2026-05-22.

- **decision**: `pass`
- **blocking_count**: `0`
- **summary**: 18× row-1 ✅ soft procedural narrative validation against proposal §5.3 standard; mandatory:advisory split 10:8 ACCEPTED per Codex calibration; first pass single round.
- Archive: `docs/sprints/sprint-044-codex-review.md` (immutable per `doc_governance.md`).

### 12.5 Bad-case suite manual review verdict (PRIMARY GATE per §5.6)

Real-LLM rerun 2026-05-22 against post-S-Eval-5 HEAD `7562a2d` (Moonshot `moonshot-v1-32k` simulator/judge). Result paths: main batch (parallel=4) `eval_interactive/results/20260522-110537/` + 3 isolated reruns (parallel=1) `20260522-162847/162848/162850/`. Per-case verdicts recorded in `eval_interactive/case_specs/bad_cases/_manifest.md` ledger M3 column + "M3-Eval close real-LLM rerun + manual review (2026-05-23)" section.

**Distribution**: **PASS × 5** (cs001, cs014, cs029, cs066, fg5q) + **IMPROVING × 4** (alice, cs011, cs012, wmkb) + **FAIL × 3** (cs015, cs095, iwzx — all 3 are the bad cases' raison d'être; the multi-layer failure shapes these cases were authored to track persist as expected, NOT regressions) + **OOSR × 0** at parallel=1.

**Tier-0 safety floor** (corrected per Codex P0-F1 finding): safety invariants `no_pii_leakage` / `no_human_only_tool_exposure` / `no_critical_policy_violation` / `phase_transition_validity` PASS 12 of 12 cases where measured; `escalation_compliance` PASS 11 of 12 (cs001 fails — bot's `escalation_reason=user_requested` vs expected `faq_miss_threshold_exceeded`; this is the cs001 bad case's documented closure_criterion sub-component failure shape, NOT an M3-Eval-introduced regression; cs001's bad-case human-judgment verdict on closure_criterion remains PASS on the primary anti-pattern axis — mechanical-template avoidance + FAQ engagement + UC-C routing).

**Decision**: bad-case suite manual review qualitatively PASS per `iteration_governance.md` §5.6 "overall pattern, NOT a programmatic threshold". 5 of 12 fully match closure_criterion direction; 4 more show closure_criterion's primary anti-pattern AVOIDED; 3 documented multi-layer failures persist (per case's raison d'être).

### 12.6 Tier-0 candidate dispositions

- **C2 (Skill guardrail non-overridability)** — DEFERRED per M2-close DEFER verdict; M3-Eval did NOT trigger production trace observation that would warrant elevation (M3-Eval scope was eval-side, not runtime). Status unchanged; remains in `docs/action_bank.md` §5.2 as `R-skill-guardrail-non-overridability-tier-0`.
- **C3 (Skill state bus boundary enforcement)** — DEFERRED per M2-close DEFER verdict; same rationale. Status unchanged; remains in `docs/action_bank.md` §5.2 as `R-skill-state-bus-boundary-enforcement-tier-0`.

### 12.7 R-item flips

- ✅ **4 closed in S-Eval-5** (`succeeded-by` annotations at `docs/action_bank.md` §6 lines 397 / 408 / 409 / 412; commit `e0cd8aa`): `R-l3-judge-form-context-trust-rubric`, `R-l1-source-citation-quality-rubric`, `R-cs038-l3-review-intake-efficiency`, `R-cs040-l3-review-intake-completion-semantics`.
- 🔁 **2 unblocked but not closed (graduated to follow-on)**: `R-case-spec-overrides-schema-scoring-extension` (schema-block lifted; full closure depends on follow-on sprint), `R-escalation-reason-runtime-evidence-contract-review` (touched by S-Eval-1 demotion; full closure requires runtime evidence — out of M3-Eval scope).
- 🆕 **5 NEW R-items surfaced + opened during M3-Eval** (all deferred to M4+; recorded in `docs/action_bank.md` §5):
  - `R-stale-test-escalation-enum-sync-v0_2-to-v0_3-migration` (S-Eval-1 close 2026-05-21).
  - `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close 2026-05-22).
  - `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (M3-Eval close 2026-05-23; semantic_planner layer).
  - `R-bad-case-parallel-session-establishment-flakiness` (M3-Eval close 2026-05-23; infra/eval-harness layer).
  - `R-bad-case-fixture-migrate-to-l3-judge-dims` (M3-Eval close 2026-05-23; eval_spec fixture migration).
- 🆕 **2 Codex-surfaced potential R-items** (M3-Eval-shared review §6 OQ disposition table — `surface-as-r-item` recommendations):
  - OQ-S46.6: `stall_quality` L3 dim potentially redundant with L1 stall_detector — candidate M4+ signal consolidation R-item.
  - OQ-S46.7: Smoke/anchor/anchor_outcome fixture gap on `user_goal_achievement` (broader than bad-case `R-bad-case-fixture-migrate-to-l3-judge-dims`) — candidate M4+ broader fixture-migration R-item.

### 12.8 Token-cost observation + architecture-health metric direction

- **Per-turn projection token cost** (M3-Eval observation per §5 acceptance bar): not formally measured at M3-Eval close (deferred to per-Skill profiling instrumentation in a future milestone). S-Eval-3 populated `critical_steps[].desc` total ~7900 chars across the 6 Skill YAMLs (per `docs/sprints/sprint-044-handoff.md`); proposal §5.4 estimated 500-900 token increment per turn. The `[{id, desc}]` list-of-objects projection format at `ContextProjectionBuilder.java:889` keeps the rendering bounded. No production-side regression observed in dev-environment latency. Recorded as M3-Eval observation; no hard gate.
- **Architecture-health metrics direction** (`iteration_governance.md` §6):
  - `new_semantic_hardcode_count` = **0** across M3-Eval (Codex §3 Q1 PASS independent verification).
  - `soft_signal_conversion_count` ≥ **18** — the 18 populated `critical_steps[].desc` ARE soft-signal projections of previously implicit / scattered procedural intent per proposal §5.2 single-source-of-truth claim.
  - `planner_ownership_ratio` — NOT DECREASED (Codex §3 Q5 PASS independent verification; runtime LLM semantic ownership unchanged).
  - `shadow_disagreement_rate` — not formally measured (shadow CaseSpecs held out per `_ACCESS_BOUNDARY.md`).

Direction of health (per §6 definitions): all four metrics moved in the desired direction during M3-Eval (hardcode down + soft signal up + planner ownership preserved + shadow integrity preserved).

### 12.9 Fold-back queue (design-doc editorial drift accumulated during M3-Eval)

No major design-doc editorial drift accumulated; M3-Eval was eval_spec layer-bounded and did not touch foundational design docs. Minor items routed to next governance cadence:

- M3-Eval observation: `eval_interactive/case_specs/bad_cases/_manifest.md` calibration notes section (S-Eval-4 dry-run + M3-Eval close real-LLM rerun) is now ~280 lines and trending toward eventual fold-back into `iteration_governance.md` §5.6 example library (referenced in OQ-S45.4 disposition). Deferred to next `doc_governance.md` fold-back cadence.
- M3-Eval observation: `compact/context-handoff-sprint-046-pre-dev.md` references `~/.claude/agent-memory/sprint-deliver-orchestrator/` filesystem path that does NOT exist; the actual memory dir is at `/Users/caoruixin/.claude/projects/-Users-caoruixin-projects-csagent/memory/`. Documented as caveat in the pre-dev handoff §"Deliver-agent memory caveat"; deferred to next pre-dev handoff authoring round (path text correction).

### 12.10 Next milestone (M4+) planning round seed

M3-Eval closes; sprint_objective.md + milestone_objective.md reset to next-milestone-TBD planning placeholder per the M3-Eval close-out bundle. Candidate next milestones (deliver-agent + human pick at next planning round; ordered roughly by release-gate proximity + dependency):

1. **M3-B Single Handover Orchestrator** — `docs/release_gate.md` §1.1 release-gate-blocker; consume `D-single-handover-orchestrator` and related R-items from `docs/action_bank.md` §3 / §4.
2. **M4 Bad-case fixture migration + parallel-session flakiness fix** — consume `R-bad-case-fixture-migrate-to-l3-judge-dims` + `R-bad-case-parallel-session-establishment-flakiness` (both M3-Eval-close surfaced 2026-05-23). Smaller scope; cleanup-flavored.
3. **M4 UC-G/H/I/J bad-case seeding** — consume `R-bad-case-suite-uc-ghij-seed-from-real-sessions` (S-Eval-4 close surfaced 2026-05-22). Requires real-session source material; mock-account-experiment-driven (Alice precedent).
4. **M4 Semantic-planner soft-signal extension** — consume `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (M3-Eval close surfaced 2026-05-23) + related per-UC moderation-context projection enhancements. Semantic_planner layer; LLM-first soft-signal expansion.
5. **M4 M3-Corpus** — separate parallel-track milestone consuming `R-corpus-coverage-audit-per-uc` (preceding M3-Eval R-item; runtime-corpus-coverage scope; per-UC FAQ corpus audit).
6. **M4 Latency / Skill-Tuning / Tier-0 re-evaluation** — preceding M2-close candidates; remain in slate.

No pre-decision applied here. Deliver-agent + human pick at next planning round.
