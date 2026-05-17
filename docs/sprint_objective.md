---
title: Sprint 37 objective — Skill Registry + state-across-Skill design freeze (M2 sub-sprint 1)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: per round
supersedes: [docs/sprints/sprint-036-objective.md]
superseded_by: null
notes: >
  Sprint 37 is the FIRST sub-sprint of NEW Milestone M2 (Skill
  Registry Abstraction + Wholesale Retroactive Externalization, per
  docs/milestone_objective.md). Sprint 37 is a DOCS-ONLY design
  freeze (NO server/ code; NO test code) that produces
  docs/proposals/skill_registry_design.md with 10 design decisions
  locked covering Skill data model, SkillRegistry shape,
  PhaseEvaluator-as-Skill-Selector integration, procedure vs
  guardrails responsibility split, retroactive migration mappings
  for ALL 6 phase YAMLs + Sprint 23/31/33 teaching paragraphs +
  Sprint 6/7/11 predicates, unified Skill terminal-predicate
  dispatcher design, session-level state model + per-Skill
  state_inheritance semantics, and §4.1 anti-hardcode kernel
  walk-through surfacing any Tier-0 invariant candidate.

  Sprint 37 SUPERSEDES the OLD Sprint 37 contract (S1 implementation
  per OLD M2-Skill framing) that was drafted but never committed.
  The OLD contract assumed Skill = teaching paragraph + adjacent
  Java predicate (minimum-surface incrementalism); NEW Sprint 37
  freezes the Skill Registry abstraction (first-class externalized
  Skill definitions) that the OLD framing's Sprint 36 design freeze
  explicitly avoided. NEW Sprint 37 is a docs-only sprint; S1
  citation predicate work is re-homed inside the Skill abstraction
  at NEW Sprint 39 (after Sprint 38 establishes the SkillRegistry
  core).

  Layer: docs/design only (NO server/ code) — but multi-layer
  PROSPECTIVE per `feedback_multi_layer_prospective_stanza.md`
  covering Sprints 38 (prompt_projection + skill_state + Runtime-
  owned PhasePlan composition), 39 (prompt_projection +
  semantic_planner + skill_state + Runtime grounding floor +
  Runtime capability floor), 40 (prompt_projection +
  semantic_planner), 41 (skill_state + prompt_projection). §7 stanza
  REQUIRED (single-track multi-layer prospective).

  Codex review: per-sub-sprint at Sprint 37 close per
  iteration_governance.md §4.3 trigger #1 (possible Tier-0 candidate
  from Skill terminal-predicate / tool-whitelist enforcement
  semantics) + §4.3 trigger #2 (§1.7 boundary discussion on
  Skill abstraction shape — Codex verifies Skill `procedure` +
  `guardrails` design does NOT introduce per-UC-branch if-else as
  a hidden form). Codex review prompt to be drafted by deliver-
  agent at Sprint 37 close BEFORE Codex dispatch.

  M2 acceptance bar recalibration (per docs/milestone_objective.md
  §5): bad-case suite (Alice) + interactive eval are OBSERVATION
  only for THIS milestone; Sprint 37 contract reflects this — no
  bad-case-rerun success metric; no interactive-eval-pass success
  metric; primary success is "10 design decisions documented,
  §4.1 kernel walked, Tier-0 candidates surfaced if any, Codex
  per-sub-sprint review verdict approve".
---

# Sprint 37 — Skill Registry + state-across-Skill design freeze

**Milestone:** M2 (Skill Registry Abstraction + Wholesale Retroactive Externalization) sub-sprint 1 of 5 per `docs/milestone_objective.md`.

## 1. Sub-sprint class

**Design freeze sub-sprint, single track (Track A), docs/design only (NO server/ code; NO test code), semantic-touching multi-layer PROSPECTIVE.** Sprint 37 ships an architectural decision doc at `docs/proposals/skill_registry_design.md` that locks 10 design decisions covering Sprints 38/39/40/41 implementation work; Sprint 37 itself produces no `server/` code change, no test code, no behaviour change. The multi-layer prospective stanza covers the layers each downstream sub-sprint will hit per `feedback_multi_layer_prospective_stanza.md`.

**§7 stanza REQUIRED — single-track multi-layer prospective** per the M2 §1 sub-sprint layer breakdown.

**Codex review:** per-sub-sprint at Sprint 37 close per `iteration_governance.md` §4.3 trigger #1 (possible Tier-0 candidate from Skill terminal-predicate / tool-whitelist enforcement semantics) + §4.3 trigger #2 (§1.7 boundary discussion on Skill abstraction shape — Codex verifies Skill `procedure` + `guardrails` design does NOT introduce per-UC-branch if-else as a hidden form). If Sprint 37 surfaces a Tier-0 candidate, STOP per §10 stop condition #1 and escalate to human review BEFORE Sprint 38 begins; the Tier-0 candidate write-up file is filed at Sprint 37 close.

## 2. Goal

Produce a single architectural decision doc at `docs/proposals/skill_registry_design.md` that locks 10 design decisions tight enough that Sprint 38 / 39 / 40 / 41 contracts can be drafted directly from the freeze without further architectural rounds:

- **(a) Skill data model** — YAML/JSON external file fields + schema validation contract. Concretely: name (`name`, `description`); applicability (`applicable_phases` enum subset, `applicable_use_cases` enum subset); tool whitelist (`tools_required` — list of canonical tool names that the Skill exposes; PhasePlan.allowedTools is composed from this); procedure (`procedure` — recommended natural-language flow surfaced to LLM as prompt teaching; may be multi-step / branched in PRINCIPLE; MUST NOT contain per-UC-branch if-else per Constitution §1.7); guardrails (`guardrails` — declarative DSL OR freeform constraint expression for terminal-predicate enforcement; carries `must_cite_source` / `intake_complete_required` / Sprint 6/7/11 migrated equivalents); state inheritance (`state_inheritance` — what state the Skill inherits, resets, or sees as soft signal from prior Skill on UC switch). JSON Schema OR equivalent validation contract that SkillLoader uses at registry boot.
- **(b) Skill Registry shape** — Java class location (likely `service/runtime/skill/SkillRegistry.java`); loader implementation (`SkillLoader.java` reads YAML/JSON from `server/src/main/resources/skills/` at Spring bootstrap; fails fast on schema validation error); `select(phase, useCase) → Skill` method signature; fallback semantics on (phase, useCase) tuple not matching any Skill (options: throw exception; return a pre-defined null/default Skill; return Optional<Skill>; deliver-agent + dev recommend per freeze); Skill versioning if any (probably none in M2; future M3 if needed).
- **(c) PhaseEvaluator-as-Skill-Selector integration** — exact mechanism: `PhaseEvaluator.evaluate(session, ...)` queries `skillRegistry.select(session.getPhase(), session.getActiveUseCase())`; Skill composed with session state into PhasePlan (PhasePlan.allowedTools from `skill.getToolsRequired()`; PhasePlan.systemInstruction from `skill.getProcedure()` composed with orchestration shell from system_prompt.txt; PhasePlan.groundingInstruction + escalationPolicy similarly composed from Skill fields); backward-compatible PhasePlan data shape (no breaking change to PhasePlan consumers); intermediate state during migration (Sprint 38 migrates 4 simpler phases at once; Sprint 39 migrates remaining 2; intermediate state where some phases are on Skill and some are still hardcoded — design freeze ensures both paths coexist safely in PhaseEvaluator until Sprint 39 completes).
- **(d) Skill `procedure` (LLM-soft prompt teaching) vs `guardrails` (LLM-soft vs PhasePlan.allowedTools hard tool whitelist vs unified Java terminal-predicate dispatcher) responsibility split** — what content goes where: `procedure` text is LLM-soft prompt teaching (recommended order, what to do "first", "then", "if X then..."); `guardrails` block declares terminal predicates that the unified Java dispatcher enforces (refuse tool call X if condition Y; downgrade outcome Z if condition W); PhasePlan.allowedTools is the HARD tool whitelist composed from `skill.tools_required` and enforced by existing tool dispatch (LLM physically cannot call a tool not on the whitelist). Document how a guardrail is declared (declarative fields like `must_cite_source: true`? freeform constraint expression like `reject record_outcome when class=resolve and citation_absent`? deliver-agent + dev recommend per freeze, leaning declarative for parseability). Document how the unified dispatcher discovers Skill-declared guardrails at runtime (Skill carries `getGuardrails() → List<Guardrail>`; dispatcher iterates per-Skill on each candidate tool call OR outcome emission).
- **(e) Retroactive migration mapping for ALL 6 phase YAMLs** — for each phase (DISCOVER, CONFIRM, RESOLVE_FAQ, RESOLVE_INTAKE, ESCALATE, TERMINAL), map current `PhaseEvaluator.java` Java-string `systemInstruction` / `groundingInstruction` / `escalationPolicy` content (cite line numbers verified at HEAD ed71031) to its target Skill YAML field (`procedure`, `guardrails`, etc.); identify content overlap across phases (if any cross-phase teaching exists, decide if it goes in orchestration shell OR is duplicated across Skills OR refactored into shared partial); document behavioural equivalence test pattern (pre/post-migration PhasePlan observationally identical for representative UCs in each phase). Sprint 38 implements DISCOVER + CONFIRM + ESCALATE + TERMINAL migration; Sprint 39 implements RESOLVE_FAQ + RESOLVE_INTAKE migration; this mapping enables both.
- **(f) Retroactive migration mapping for Sprint 23/31/33 teaching paragraphs in `system_prompt.txt`** — for each paragraph (Sprint 23 `already_called` at lines 23-28; Sprint 31 `alternate_candidate_use_cases` at lines 30-33; Sprint 33 `discover_disambiguation_signals` at lines 36-40 — verify line numbers at HEAD), decide its target home: (i) goes into one Skill's `procedure` (e.g., Sprint 33 `discover_disambiguation_signals` likely into `discover_triage.yaml`); (ii) goes into multiple Skills' `procedure` (e.g., Sprint 23 `already_called` may be cross-Skill — if so document the duplication OR a shared-fragment mechanism); (iii) stays in the orchestration shell (`system_prompt.txt`) IF it's cross-Skill teaching about how to read Skill envelopes; (iv) goes into a Skill's `guardrails` block IF it's a constraint not a procedure. Document the decision rationale per paragraph. Sprint 40 implements this migration; this mapping enables it.
- **(g) Retroactive migration mapping for Sprint 6/7/11 predicates in `AgentRunLoopImpl.java`** — for each Java method (Sprint 6 `shouldRejectFaqMissHandover` at lines 690-734; Sprint 7 `shouldRejectIncompleteIntakeHandover` at lines 628-651; Sprint 11 `shouldRejectPrematureResolveOutcome` at lines 745-759 — verify line numbers at HEAD), decide which Skill `guardrails` block hosts it (Sprint 6 → `resolve_faq_grounded_answer.yaml`; Sprint 7 → `resolve_intake_collect_and_handover.yaml`; Sprint 11 → `resolve_faq_grounded_answer.yaml`). Document how the unified Skill terminal-predicate dispatcher composes Skill-declared guardrails into runtime enforcement equivalent to (or stricter than) the current Java method behaviour. Sprint 39 implements this migration; this mapping enables it.
- **(h) Unified Skill terminal-predicate dispatcher design** — placement: (i) in `AgentRunLoopImpl.java` adjacent to current Sprint 6/7/11 family (then their methods are replaced by dispatcher calls); OR (ii) new `service/runtime/skill/SkillGuardrailDispatcher.java` class injected into AgentRunLoopImpl. Composition order on a single tool call / outcome emission: short-circuit on first refusal? all-must-pass with collected reasons? Document choice with rationale. Failure mode: rejection diagnostic shape (trace entry written via existing trace channel; canonical reason label like `s1_citation_presence_required` per OLD Sprint 36 OQ 7.3); LLM-visible message structure (reject-and-hint per OLD Sprint 36 OQ 7.3 default carried forward); reject-and-retry semantics (LLM sees refusal, can then either add citation OR escalate via `request_handover(faq_miss_threshold_exceeded)`; ALL of this preserved from existing Sprint 6/7/11 pattern).
- **(i) Session-level state model + per-Skill `state_inheritance` semantics for UC switch + state preservation** — what the session state-bus carries (existing `customerContext` / `intakeFields` / `accumulated_tool_results` + new `prior_skill_identifier` / `prior_grounding_citations` if any); how `state_inheritance` is declared per Skill (e.g., `inherit: [customer_context, accumulated_tool_results]`, `reset: [intake_fields]`, `soft_signal_via_projection: [prior_citations, prior_skill_identifier]`); UC switch trigger conditions (rides on M1 Sprint 32 `alternate_candidate_use_cases` projection + M1 Sprint 33 `discover_disambiguation_signals` — NOT a new classifier per M2 §6 #5 fence); SkillStateBus shape (new Java class `service/runtime/skill/SkillStateBus.java` — fields, methods, integration with AgentSession on Skill change). Sprint 41 implements this; the freeze enables it. **§1.7 hard fence**: per-Skill `state_inheritance` declarations MUST NOT be per-UC-pair if-else tables (e.g., `if prior_uc==UC-A and new_uc==UC-C then inherit X` is FORBIDDEN); they MUST be principle-level (e.g., `inherit: [customer_context]` — applies to any prior Skill transitioning into this Skill).
- **(j) §4.1 anti-hardcode kernel walk-through on the proposed design** — walk all 9 questions; for each "yes" or each concern, name the diff snippet (in the proposed design) and the reasoning. Surface any Tier-0 invariant candidate from Skill abstraction semantics: candidates may include "tool whitelist enforcement is unconditional regardless of Skill `procedure`" (Tier-0 candidate if it's a new runtime guarantee); "Skill terminal predicate refusal is non-overridable by LLM" (Tier-0 candidate if it's a new runtime guarantee); "Skill-declared `state_inheritance` is enforced at session-state-bus boundary" (Tier-0 candidate if it's a new runtime guarantee); "S1 citation predicate (must_cite_source on RESOLVE_FAQ) is bounded per §6 #4 verbatim authorization" (NOT a Tier-0 candidate per the bounded inversion). For each candidate, name whether it's load-bearing enough to be a NEW Tier-0 invariant in `docs/runtime_freeze_and_risk_policy.md` §1/§2 OR whether it stays a §1.4 Runtime-floor guarantee that the Skill abstraction enforces by construction (no governance addition needed). If a Tier-0 candidate is identified, file the write-up at Sprint 37 close + escalate to human review per §10 stop condition #1.

Sprint 37 walks the §4.1 nine-question anti-hardcode kernel against the proposed design pre-implementation; surfaces any Tier-0 candidate for explicit human-review escalation per §10. Sprint 37 does NOT write `server/` code; it produces docs that Sprints 38 / 39 / 40 / 41 implement.

## 3. Non-goals (explicit)

- Sprint 37 does NOT write `server/` code. NO production Java change; NO test Java change; NO YAML Skill file creation (those are Sprint 38+ scope); NO `system_prompt.txt` edit (Sprint 40 scope); NO `AgentRunLoopImpl.java` edit (Sprint 39 scope); NO `PhaseEvaluator.java` edit (Sprint 38+39 scope); NO `ContextProjectionBuilder.java` edit (Sprint 41 scope).
- Sprint 37 does NOT pre-commit Sprint 38/39/40/41 implementation specifics beyond what the design freeze (a)-(j) decides. Sprint 38+ contracts are drafted one at a time per deliver-agent + human round AFTER Sprint 37 close.
- Sprint 37 does NOT touch `docs/current/iteration_governance.md` / `docs/current/doc_governance.md` / `docs/current/agent_context_guide.md` / `docs/runtime_freeze_and_risk_policy.md`. Constitution-discipline preserved per `feedback_constitution_discipline_vs_planning_anticipation.md`. **EXCEPTION**: if Sprint 37 surfaces a Tier-0 candidate, the candidate write-up is a NEW file (e.g., `docs/proposals/tier0_candidate_<name>.md`); the actual `runtime_freeze_and_risk_policy.md` edit happens at human-review-approval, NOT in Sprint 37 dev commit. Per M2 §6 #12 EXCEPTION clause.
- Sprint 37 does NOT touch `docs/foundational/`.
- Sprint 37 does NOT touch sprint archives under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-036-*`.
- Sprint 37 does NOT touch milestone archives under `docs/milestones/`. (The OLD M2-Skill archive at `docs/milestones/M2-Skill_objective.md` is created by deliver-agent at M2 approval bundle, NOT in Sprint 37 dev session.)
- Sprint 37 does NOT touch `docs/milestone_objective.md` or `docs/sprint_objective.md` (deliver-agent-owned).
- Sprint 37 does NOT touch `docs/action_bank.md` in the dev session (R-item flow is deliver-agent-owned at sub-sprint close).
- Sprint 37 does NOT touch existing case families under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence carried from M1).
- Sprint 37 does NOT touch shadow CaseSpecs under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
- Sprint 37 does NOT touch `eval_interactive/eval_interactive/` (harness, loader, simulator) — irrelevant to design freeze.
- Sprint 37 does NOT touch `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`. Per M2 §5 recalibration, Alice is observation not gate; Sprint 37 is docs-only so no Alice work either way.
- Sprint 37 does NOT widen judge rubrics or `eval_interactive/case_spec_overrides.yaml`.
- Sprint 37 does NOT add a new Tier-0 invariant to `runtime_freeze_and_risk_policy.md` in the dev commit (per fences above). If Tier-0 candidate is surfaced, file a candidate write-up + escalate; deliver-agent + human evaluate at Sprint 37 close per §10 condition #1.
- Sprint 37 does NOT use mocked-LLM evidence (Sprint 37 is docs-only; no LLM evidence at all).
- Sprint 37 does NOT pre-decide whether `docs/proposals/skill_foundation_design.md` (OLD Sprint 36 freeze) supersede edit is in Sprint 37 dev scope OR deliver-agent at M2 approval bundle. **Default per this contract: deliver-agent applies the supersede pattern at M2 approval bundle (separate from Sprint 37 dev commit)**. Sprint 37 dev MAY reference the supersession in `docs/proposals/skill_registry_design.md` notes (the NEW freeze logically supersedes the OLD freeze) but does NOT touch the OLD file.

## 4. Premise check (deliver-agent verified 2026-05-17; dev re-verifies at session start)

Verified at HEAD `ed71031` (post-Sprint-36-close; pre-NEW-M2-supersession-bundle):

1. **M2 milestone objective approved.** `docs/milestone_objective.md` carries the NEW M2 contract (status `current`; supersedes old M2-Skill); approved by human 2026-05-17.
2. **Sprint 37 contract approved.** `docs/sprint_objective.md` (this file) is the active sub-sprint contract; approved by human 2026-05-17.
3. **Sprint 5 (F2) skill orchestration original proposal** at `docs/proposals/skill_orchestration_candidates.md` — `status: proposal`, `implementation_status: not_started`. Sprint 37 dev SHALL read this as primary architectural input alongside the OLD Sprint 36 freeze and the research-agent proposal in the deliver-agent context (the user-provided research-agent proposal that anchored NEW M2 recalibration 2026-05-17 — see dev prompt for full quote).
4. **OLD Sprint 36 design freeze** at `docs/proposals/skill_foundation_design.md` — `status: proposal` (will be flipped to `superseded` by deliver-agent at M2 approval bundle; Sprint 37 dev sees the OLD freeze as historical reference, NOT as binding decision). The OLD freeze locked "minimum-surface" (reuse PhasePlan + new system_prompt.txt teaching paragraph + new predicate adjacent to existing shouldRejectXxx family); NEW Sprint 37 freeze fundamentally departs (Skill Registry abstraction with externalized YAML + retroactive migration of scattered content). Read for context on what was rejected and why.
5. **`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`** at HEAD carries hardcoded `systemInstruction` / `groundingInstruction` / `escalationPolicy` Java strings per phase (DISCOVER + CONFIRM + RESOLVE_FAQ + RESOLVE_INTAKE + ESCALATE + TERMINAL). Sprint 37 dev SHALL grep these at session start + cite exact line numbers in handoff §4. Known approximate locations (from OLD Sprint 36 + 37 contracts; verify at HEAD): RESOLVE_FAQ branch at lines 598-665; INTAKE_UCS at line 30; escalation_reason enum at lines 39-63; PhaseEvaluator class total may exceed 700 lines.
6. **`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`** at HEAD carries existing Sprint 6/7/11 shouldReject* family methods. Sprint 37 dev SHALL grep these at session start + cite exact line numbers in handoff §4. Known approximate locations: `shouldRejectFaqMissHandover` Sprint 6 §G2 lines 690-734; `shouldRejectIncompleteIntakeHandover` Sprint 7 §I2 lines 628-651; `shouldRejectPrematureResolveOutcome` Sprint 11 §M1 lines 745-759; FAQ_PATH_UCS at lines 106-108.
7. **`server/src/main/resources/prompts/system_prompt.txt`** at HEAD carries the scattered teaching paragraphs. Sprint 37 dev SHALL grep at session start + cite exact line numbers in handoff §4. Known approximate locations: Sprint 23 `already_called` lines 23-28; Sprint 31 `alternate_candidate_use_cases` lines 30-33; Sprint 33 `discover_disambiguation_signals` lines 36-40. Total file line count: ~40-60 lines pre-M2 (verify at HEAD).
8. **`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`** at HEAD carries the projection slots from M1 Sprint 32 (`alternate_candidate_use_cases`) + M1 Sprint 33 (`discover_disambiguation_signals`). Sprint 37 dev SHALL read this to ground decision (i) — what projection slots exist and how `prior_use_case_carry` (Sprint 41 NEW slot) would integrate. NO edit in Sprint 37 (Sprint 41 scope).
9. **`server/src/main/resources/skills/`** directory at HEAD: does NOT exist yet. Sprint 38 creates it on first Skill YAML migration. Sprint 37 design freeze decides the directory shape and file naming convention.
10. **Java baseline.** 983 / 1-inherited (`SystemPromptUserRequestedTiebreakerTest`; attributed to working-tree dirty `system_prompt.txt` state) / 0 / 2 post-Sprint-36-close. Sprint 37 is docs-only so baseline UNCHANGED (no new tests; no test regression). Sprint 37 dev SHALL re-verify at session start that no orphaned working-tree changes from this deliver-agent session block the docs-only commit.

Sprint 37 dev SHALL re-verify these at session start by reading the cited files (one read each + one grep for line-number citation per file in handoff §3). If any premise has drifted between 2026-05-17 and dev session start, STOP and surface; do NOT implement under a false premise.

## 5. Files in scope (Sprint 37 dev ships)

| path | change type |
|------|-------------|
| `docs/proposals/skill_registry_design.md` | **NEW** — architectural decision doc locking 10 design decisions (a)-(j) per §2. Section structure mirrors `docs/proposals/skill_foundation_design.md` (the OLD freeze) for cross-comparison readability. Each design decision section names: the decision; the rationale; the alternatives considered + why rejected; the §1.7 boundary check; downstream sub-sprint references (which sub-sprint implements this decision). The §4.1 anti-hardcode walk-through is its own section. Tier-0 candidate enumeration is its own section at the end. The doc carries doc_governance.md frontmatter: `doc_tier: proposal`, `status: proposal`, `implementation_status: not_started`, `source_of_truth: this file`, `last_reviewed: <Sprint 37 commit date>`, `review_cadence: per milestone`, `supersedes: [docs/proposals/skill_foundation_design.md, docs/proposals/skill_orchestration_candidates.md]`, `superseded_by: null`. |
| (CONDITIONAL) NEW Tier-0 candidate write-up file (e.g., `docs/proposals/tier0_candidate_<name>.md`) | NEW IF Sprint 37 §4.1 walk surfaces a Tier-0 candidate; the candidate is filed for explicit human-review escalation per §10 stop condition #1. Frontmatter: `doc_tier: proposal`, `status: proposal`, `implementation_status: not_started`, `source_of_truth: docs/runtime_freeze_and_risk_policy.md (pending addition)`. Document: the candidate invariant statement; the Skill abstraction context; why it qualifies as Tier-0 per `runtime_freeze_and_risk_policy.md` §1/§2 criteria; the proposed addition text; the impact on Sprint 38+ implementation (what changes if added; what changes if rejected). |
| `docs/sprints/sprint-037-handoff.md` | **NEW** (12-section dev-authored archive per Sprint 31-36 shape). |

### 5.1 Sub-sprint-close artefacts (deliver-agent owned, M2 milestone-scoped)

| path | change type |
|------|-------------|
| `docs/sprints/sprint-037-handoff.md` | NEW (12-section dev-authored archive) |
| `docs/sprint_objective.md` | EDIT at Sprint 37 close (deliver-agent replaces with Sprint 38 contract; archives this contract to `docs/sprints/sprint-037-objective.md`) |
| `docs/10-handoff.md` §1 lead | EDIT at Sprint 37 close (deliver-agent demotes Sprint 37 to "current sub-sprint completed", sets Sprint 38 as current) |
| `docs/action_bank.md` | EDIT at Sprint 37 close (deliver-agent): flip Sprint 5 F2 `skill_orchestration_candidates.md` to `Sprint-37-locked-pending-Sprint-38+-implementation`; if Tier-0 candidate surfaced, add the candidate write-up reference + escalation request; flip `R-grounding-discipline-iterative-search-fabrication` per Sprint 37 freeze decision (d) on whether S1 `must_cite_source` is bounded Runtime-floor per §1.4 |
| `docs/codex-findings.md` | EDIT at Sprint 37 close — Codex per-sub-sprint review fires per §4.3 trigger #1 + #2; deliver-agent dispatches; Codex writes findings; deliver-agent archives at Sprint 37 close to `docs/sprints/sprint-037-codex-review.md` |
| `compact/sprint-037-dev-prompt.md` | deliver-agent-owned (authored before Sprint 37 dev session; NOT staged by dev) |
| `compact/sprint-037-review-prompt.md` | deliver-agent-owned, drafted at Sprint 37 close BEFORE Codex dispatch (NOT in dev session) |

## 6. Files NOT in scope (hard fences)

1. **No `server/` code change** in Sprint 37. NO production Java edit; NO test Java edit; NO YAML Skill file creation; NO `system_prompt.txt` edit; NO `AgentRunLoopImpl` / `PhaseEvaluator` / `ContextProjectionBuilder` / `EscalationReasonResolver` / `IntakeFieldsRegistry` / `IntakeFieldExtractor` / `RecordOutcomeTool` edit. Sprint 37 is docs-only.
2. **No edit to `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md`** (constitution-discipline preserved per `feedback_constitution_discipline_vs_planning_anticipation.md`). EXCEPTION: if Sprint 37 surfaces a Tier-0 candidate, file the candidate write-up file (new doc) for human-review escalation; do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 37 dev commit; deliver-agent + human evaluate at Sprint 37 close.
3. **No edits to other docs under `docs/foundational/` or `docs/current/`** (other than the Sprint 37 handoff itself).
4. **No edits to sprint archives** under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-036-*`.
5. **No edits to milestone archives** under `docs/milestones/`. (The OLD M2-Skill archive at `docs/milestones/M2-Skill_objective.md` is created by deliver-agent at M2 approval bundle, NOT in Sprint 37 dev session.)
6. **No edits to `docs/milestone_objective.md` or `docs/sprint_objective.md`** (deliver-agent-owned; M2 + Sprint 37 contracts stay stable across the dev session).
7. **No edits to `docs/action_bank.md`** in the dev session (deliver-agent-owned at sub-sprint close).
8. **No edits to `docs/codex-findings.md`** in the dev session (Codex writes at Sprint 37 close; deliver-agent dispatches).
9. **No edits to `docs/proposals/skill_foundation_design.md`** (OLD Sprint 36 freeze) in Sprint 37 dev session. The supersession pattern (status: superseded + superseded_by) is applied by deliver-agent at M2 approval bundle, separate from Sprint 37 dev commit. Sprint 37 dev MAY reference the supersession in `docs/proposals/skill_registry_design.md` notes (the NEW freeze logically supersedes the OLD freeze) but does NOT touch the OLD file. (If dev finds this awkward and prefers to apply the supersede pattern in Sprint 37 dev commit instead, surface in handoff §7 OQ for deliver-agent re-decision; otherwise default stands.)
10. **No edits to `docs/proposals/skill_orchestration_candidates.md`** (Sprint 5 F2 original proposal) in Sprint 37 dev session. The chained supersession is applied by deliver-agent at M2 approval bundle.
11. **No edits to existing case families** under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence carried from M1).
12. **No edits to shadow CaseSpecs** under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
13. **No edits to `eval_interactive/eval_interactive/`** (harness, loader, simulator) — irrelevant to design freeze.
14. **No edits to `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`**. Per M2 §5 recalibration, Alice is observation not gate; Sprint 37 is docs-only.
15. **No widening of `eval_interactive/case_spec_overrides.yaml`**.
16. **No mocked-LLM as primary evidence** (Sprint 37 is docs-only; no LLM evidence at all).
17. **No per-UC-branch if-else in the PROPOSED design** at `docs/proposals/skill_registry_design.md` per Constitution §1.7. The §4.1 walk-through must verify this; Codex per-sub-sprint review verifies independently.
18. **No pre-decision of Sprint 38 / 39 / 40 / 41 implementation specifics beyond what the freeze (a)-(j) decides.** Downstream sub-sprint contracts are drafted per deliver-agent + human round AFTER Sprint 37 close.
19. **No Tier-0 invariant added to `runtime_freeze_and_risk_policy.md`** in Sprint 37 dev commit (per fence #2 EXCEPTION). If candidate surfaced, file candidate write-up file separately.

## 7. Bundle policy

- **Single dev commit** with all §5 dev-authored files: `docs/proposals/skill_registry_design.md` (NEW; the design freeze doc); possibly NEW Tier-0 candidate write-up file (CONDITIONAL — only if §4.1 walk surfaces); `docs/sprints/sprint-037-handoff.md` (NEW; the dev archive).
- Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: dev does NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-037-*.md`). Deliver-agent + human bundle those at sub-sprint close.
- **No tests, no LLM rerun, no smoke** — Sprint 37 is docs-only. The §5 hard gates are: design doc shipped; §4.1 walk completed; Tier-0 candidates surfaced if any; handoff written.

## 8. Layer-classification + anti-hardcode stanza (per `iteration_governance.md` §7; REQUIRED; PROSPECTIVE per `feedback_multi_layer_prospective_stanza.md`)

**Target failure layer (Sprint 37 itself):** docs/design only — no behaviour change; no layer touch. Sprint 37 produces an architectural decision doc.

**Target failure layer (PROSPECTIVE, Sprints 38-41 covered by this freeze):** multi-layer per M2 §1 sub-sprint layer breakdown:

- **Sprint 38** layer: `prompt_projection` (Skill envelope reaches LLM via Skill-composed PhasePlan) + `skill_state` (SkillRegistry holds Skill definitions across requests; SkillLoader on boot) + Runtime-owned PhasePlan composition per §1.4.
- **Sprint 39** layer: `prompt_projection` + `semantic_planner` + `skill_state` + Runtime-owned grounding floor per §1.4 (S1 `must_cite_source` predicate) + Runtime-owned capability floor per §1.4 (S2 `intake_complete_required` predicate).
- **Sprint 40** layer: `prompt_projection` (system_prompt.txt structural change to orchestration shell) + `semantic_planner` (LLM input contract change — same teaching content, different surface).
- **Sprint 41** layer: `skill_state` (session-level state-bus + per-Skill `state_inheritance`) + `prompt_projection` (new `prior_use_case_carry` slot).

**Tier-0 invariant:** Sprint 37 adds NO Tier-0 invariant by default; the freeze MAY surface a Tier-0 candidate from Skill predicate / tool-whitelist enforcement semantics per §2 (j). If surfaced: candidate write-up filed at Sprint 37 close; human-review escalation per §10 condition #1; deliver-agent + human + Codex decide at Sprint 37 close whether to add to `runtime_freeze_and_risk_policy.md` §1/§2 (SEPARATE commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`); Sprint 38 proceeds with or without the addition based on decision.

**Semantic hardcode:** No semantic hardcode introduced by Sprint 37 (docs-only). The PROPOSED design at `docs/proposals/skill_registry_design.md` MUST be Constitution-compliant: Skill `procedure` is LLM-soft principle-level teaching (NOT per-UC-branch if-else per §1.7); Skill `guardrails` are bounded Runtime-floor predicates per §1.4 (NOT LLM-owned semantic decisions per §1.3); Skill `state_inheritance` is principle-level (NOT per-UC-pair branch tables per §1.7); §4.1 nine-question kernel walk in §2 (j) verifies this; Codex per-sub-sprint review verifies independently.

**Generalization coverage:** N/A for design freeze. Sprint 37 produces a design doc; no behavioural change to test. PROSPECTIVE coverage for downstream sub-sprints:

- **Target**: Sprint 38 covers 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL behavioural equivalence); Sprint 39 covers 2 complex phase Skills + 3 migrated predicates + 2 new predicates; Sprint 40 covers 3 teaching paragraph migrations; Sprint 41 covers UC switch + state preservation.
- **Neighbor**: each migration sub-sprint verifies behavioural equivalence (Java-test-detectable) on representative UCs in each phase + does not regress Sprint71PartialIntakePersistenceTest 14/14 baseline.
- **Negative**: Skill `guardrails` predicates verified to NOT fire on outcomes/tools they're not scoped to (e.g., S1 `must_cite_source` does NOT fire on `class=escalate/abandon` or non-RESOLVE_FAQ phases).
- **Shadow**: deliver-agent + Codex own at Sprint 39+ close (dev does not read shadow per `_ACCESS_BOUNDARY.md`).

## 9. Success metrics (per `iteration_governance.md` §5; ADAPTED for docs-only design freeze per M2 §5 recalibration)

**Hard gates (must pass for Sprint 37 close):**

- `docs/proposals/skill_registry_design.md` exists and locks all 10 design decisions (a)-(j) per §2.
- Each design decision section names: the decision; the rationale; alternatives considered + why rejected; §1.7 boundary check; downstream sub-sprint reference.
- §4.1 anti-hardcode kernel walk-through completed in the design doc, with explicit "yes / no / concern + reasoning" for each of the 9 questions on the PROPOSED design.
- Tier-0 candidate enumeration completed: each candidate named, qualified or rejected per `runtime_freeze_and_risk_policy.md` §1/§2 criteria, with proposed addition text if qualified.
- Retroactive migration mappings (e) (f) (g) cite line numbers at HEAD for each scattered content piece (so Sprint 38/39/40 dev can verify mapping at session start).
- Constitution-compliance verified: no per-UC-branch if-else in PROPOSED Skill data model OR PROPOSED `procedure` text OR PROPOSED `guardrails` declarations OR PROPOSED `state_inheritance` declarations (§1.7); no Skill terminal predicate enforces LLM-owned semantic decision (§1.3); no Skill prescribes LLM customer-facing language (§1.3); no Skill hard-encodes per-step argument values (§1.3); §1.4 Runtime-owned floor responsibilities preserved.
- Handoff §8 walks §4.1 kernel on Sprint 37 itself (expected `approve` since docs-only design freeze with constitution-compliant proposed design; Codex verifies independently).
- Reproducibility: every quantitative or code-citation claim in the design doc + handoff cites source path + line number + extraction recipe per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

**Codex review (per-sub-sprint at Sprint 37 close):**

- §4.1 anti-hardcode kernel verdict: `approve` (the PROPOSED design honors Constitution + Tier-0 candidate process; no scope expansion).
- §4.2 sprint-close header: `decision: pass | fix_required | out_of_scope_review` per outcome.
- Codex independently re-walks the §4.1 9 questions on the PROPOSED design.
- Codex independently verifies the §6 #4 verbatim authorization is preserved (S1 `must_cite_source` is bounded; not generalized to non-citation enforcement).
- Codex independently verifies no per-UC-branch if-else in PROPOSED Skill bodies.
- Codex independently verifies the Tier-0 candidate enumeration (if any candidates surfaced, Codex assesses qualification independently).

**Observations (not gates; per M2 §5 recalibration; MAY regress on future implementation sub-sprints, NOT in Sprint 37):**

- N/A for Sprint 37 (docs-only; no eval; no behaviour observation).
- Architecture-health metrics direction (§6 governance): N/A for Sprint 37; baseline established by Sprint 37 freeze for measurement at Sprint 38+ closes.

## 10. Stop conditions (dev-agent)

STOP and report (do NOT silently work around) when:

1. **Premise drift on §4 items** — any of the 10 premises has changed since 2026-05-17. STOP and surface in handoff §3.
2. **Tier-0 candidate surfaced** from §4.1 walk on PROPOSED design — STOP; file candidate write-up file; surface in handoff §7 OQ for deliver-agent + human evaluation at Sprint 37 close. Do NOT edit `runtime_freeze_and_risk_policy.md` in Sprint 37 dev commit; human escalation is required first.
3. **§1.7 forbidden-list violation cannot be cleanly resolved** in the PROPOSED design (e.g., the only way to encode Skill `procedure` for a multi-UC phase is per-UC-branch if-else; or the only way to declare `guardrails` for cross-Skill predicate is per-UC enum; or the only way to declare `state_inheritance` is per-UC-pair table). STOP and surface; do NOT silently introduce the §1.7 violation. Deliver-agent + human re-frame.
4. **§1.3 boundary violation in PROPOSED design** — a Skill terminal predicate is proposed that enforces LLM-owned semantic decision (e.g., predicate refuses LLM's choice of `request_handover` even when LLM has valid grounds; predicate enforces customer-facing language). STOP and surface.
5. **§1.4 boundary mismatch in PROPOSED design** — a Skill terminal predicate is proposed that does NOT correspond to a Runtime-owned floor (e.g., predicate enforces UC hypothesis choice; predicate enforces response strategy). STOP and surface.
6. **Tempted to write `server/` code** in Sprint 37 dev commit — STOP. Sprint 37 is docs-only by design. If a design decision can only be validated by running Java code, surface in handoff §7 OQ for deliver-agent + human re-decision (possibly split Sprint 37 into design + spike).
7. **Tempted to edit `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md`** — STOP. Per constitution-discipline + Sprint 37 §6 #2 fence. If Tier-0 candidate surfaced, file candidate write-up file (NEW doc) for human-review escalation; do NOT silently edit governance docs.
8. **Tempted to apply the supersede pattern to `docs/proposals/skill_foundation_design.md`** in Sprint 37 dev commit — STOP per §6 #9 fence. Default per this contract: deliver-agent applies supersede at M2 approval bundle. If dev prefers to apply in Sprint 37 dev commit, surface in handoff §7 OQ.
9. **Tempted to draft Sprint 38 / 39 / 40 / 41 contracts** in Sprint 37 dev commit — STOP per §6 #18 fence. Downstream sub-sprint contracts are deliver-agent-owned at planning round AFTER Sprint 37 close.
10. **Tempted to pre-decide Sprint 38+ implementation specifics beyond what (a)-(j) decides** — STOP. The freeze locks scope decisions; implementation specifics (e.g., exact Java method signatures; exact YAML field syntax) are dev's at Sprint 38+ session.
11. **Tempted to delete OR relocate `docs/proposals/skill_foundation_design.md` OR `docs/proposals/skill_orchestration_candidates.md`** — STOP. Both stay as historical reasoning per doc_governance.md; supersession pattern (status: superseded + superseded_by) is applied by deliver-agent at M2 approval bundle.
12. **Tempted to modify `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`** — STOP. Per Constitution §1.7 "widening eval spec to accept a genuine bot mistake" forbidden line + M2 §6 #22 + Sprint 37 §6 #14. Sprint 37 is docs-only; Alice is observation not gate at M2.

## 11. Handoff document contract (12 sections per Sprint 31-36 shape)

Standard 12-section shape adapted for design freeze:

1. **Context Pack** — the M2 milestone objective + Sprint 37 contract (this file) + Sprint 5 (F2) `skill_orchestration_candidates.md` original proposal + OLD Sprint 36 `skill_foundation_design.md` freeze (historical reference) + verified code shape at HEAD ed71031 (PhaseEvaluator + AgentRunLoopImpl + ContextProjectionBuilder + system_prompt.txt grep with line numbers cited) + user-provided research-agent proposal (quoted in compact/sprint-037-dev-prompt.md).
2. **Sub-sprint-objective recap** — Sprint 37 design freeze; 10 design decisions; docs-only.
3. **Premise re-verification** — §4 spot-check; 10 premises; one short paragraph per premise citing source.
4. **Design doc walkthrough** — high-level summary of each of the 10 decisions (a)-(j); cross-reference to `docs/proposals/skill_registry_design.md` section numbers; any departures from §2 framing with rationale; any decision that surfaced unexpected tension during drafting.
5. **§4.1 anti-hardcode walk-through summary** — Sprint 37 §2 (j) details live in the design doc; handoff §5 summarizes the verdict per question (yes / no / concern); cross-reference to design doc section.
6. **Tier-0 candidate enumeration outcome** — any candidates surfaced; for each: candidate statement + qualification verdict (qualified / rejected) + (if qualified) the candidate write-up file path. If NO candidates surfaced, state so explicitly with reasoning.
7. **Open questions for deliver-agent + human** — any §1.7 boundary case requiring deliver-agent + human + Codex review; any premise drift that surfaced at session start; any tension between Sprint 5 F2 original framing and NEW M2 hybrid framing; any §6 fence that proved awkward (e.g., supersede pattern timing); any §1.3 / §1.4 boundary close calls in Skill `procedure` vs `guardrails` split (decision (d)).
8. **Anti-hardcode self-walk (§4.1 nine questions) on Sprint 37 itself** — Sprint 37 is docs-only design freeze; expected verdict `approve` for Sprint 37 as a sub-sprint (no production code change; PROPOSED design constitution-compliant).
9. **Files changed** — table with paths: `docs/proposals/skill_registry_design.md` (NEW); possibly NEW Tier-0 candidate write-up file; `docs/sprints/sprint-037-handoff.md` (NEW). No production code; no tests.
10. **Layer-classification self-walk** per Sprint 37 §8 — Sprint 37 itself lands on docs/design only; PROSPECTIVE coverage for Sprints 38-41 layers stated.
11. **§5 Eval Acceptance bars (adapted for design freeze + M2 §5 recalibration)** — Sprint 37 hard gates: design doc shipped with 10 decisions; §4.1 walk completed; Tier-0 candidates surfaced if any; constitution-compliance verified; Codex per-sub-sprint review verdict `approve` expected. NO eval rerun (docs-only); NO bad-case-suite rerun (Sprint 37 is docs-only AND M2 §5 has them as observation only).
12. **Closure verdict placeholder** — leave for deliver-agent + human + Codex per-sub-sprint review per `feedback_handoff_verdict_section_delegation.md`. Sprint 37 closes at sub-sprint close per §4.3 per-sub-sprint Codex trigger #1 + #2 (NOT deferred to M2 milestone close).

## 12. M2 milestone context (cross-reference, not Sprint 37 contract)

Sprint 37 is the first of M2's 5 sub-sprints per `docs/milestone_objective.md`. After Sprint 37 commits:

1. **Deliver-agent + human review** Sprint 37 handoff + design doc at sub-sprint close. Validate: 10 design decisions documented; §4.1 walk completed; Tier-0 candidates surfaced if any; constitution-compliance.
2. **Deliver-agent drafts `compact/sprint-037-review-prompt.md`** + dispatches Codex per-sub-sprint review per §4.3 trigger #1 + #2.
3. **Codex review verdict.** If `pass`: proceed to Sprint 38 planning round. If `fix_required` with `blocking_count ≥ 1`: fix-iteration sub-sprint per existing convention BEFORE Sprint 38. If `out_of_scope_review`: deliver-agent + human classify per §4.2.
4. **If Tier-0 candidate surfaced AND human-review approves Tier-0 addition**: deliver-agent commits `runtime_freeze_and_risk_policy.md` §1/§2 addition as SEPARATE commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`; THEN Sprint 38 planning begins.
5. **Deliver-agent updates `docs/action_bank.md`** at Sprint 37 close (flip Sprint 5 F2 to `Sprint-37-locked-pending-Sprint-38+-implementation`; flip `R-grounding-discipline-iterative-search-fabrication` per Sprint 37 freeze decision on S1 predicate Runtime-floor status; close Sprint 37 in §6 action index).
6. **Deliver-agent + human housekeeping bundle at Sprint 37 close** (separate from Sprint 37 dev commit): supersede pattern applied to `docs/proposals/skill_foundation_design.md` (status: superseded + superseded_by + "Why superseded" section); chained supersede on `docs/proposals/skill_orchestration_candidates.md`; OLD M2-Skill milestone_objective archived to `docs/milestones/M2-Skill_objective.md` (status: superseded); `docs/10-handoff.md` §1 lead refreshed.
7. **Deliver-agent drafts Sprint 38 contract** at `docs/sprint_objective.md` (replacing this Sprint 37 contract; archives this contract to `docs/sprints/sprint-037-objective.md`) + `compact/sprint-038-dev-prompt.md`. Sprint 38 = SkillRegistry core + 4 simpler phase Skills migration per `docs/milestone_objective.md` §3 Sprint 38 row. Surfaces to human for review BEFORE Sprint 38 dev session launch.
8. **M2 milestone close** happens after Sprints 37 + 38 + 39 + 40 + 41 all close; deliver-agent + human dispatch milestone-shared cumulative Codex review at that point per `iteration_governance.md` §4.3 (in addition to the per-sub-sprint reviews already done).
