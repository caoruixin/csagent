---
title: Milestone M2 — Skill Registry Abstraction + Wholesale Retroactive Externalization (LLM-led, Policy-bounded)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: per milestone
supersedes: [docs/milestones/M2-Skill_objective.md, docs/milestones/M1_objective.md]
superseded_by: null
notes: >
  M2 — first framing ("M2-Skill", Skill Foundation + UC-Switching
  Continuity, drafted 2026-05-17, approved same day, only Sprint 36
  design freeze shipped) — was wholesale superseded mid-flight by
  this NEW M2 framing on 2026-05-17 per human direction. The OLD
  framing's Sprint 36 design freeze locked "reuse existing PhasePlan
  + new system_prompt.txt teaching paragraph + new predicate adjacent
  to existing shouldRejectXxx family" — minimum-surface
  incrementalism that PRESERVED the scattered Zhang-Sanfeng pattern
  (Sprint 23/31/33 teaching paragraphs continued to accumulate
  side-by-side in system_prompt.txt; Sprint 6/7/11 predicates
  continued to accumulate side-by-side in AgentRunLoopImpl). The
  human's M2 architectural intent was always "extract scattered
  content into a Skill abstraction"; the OLD framing's
  minimum-surface freeze inverted that intent.

  NEW M2 promotes Skill to a first-class abstraction (Skill Registry
  + externalized YAML/JSON definitions + PhaseEvaluator-as-Skill-
  Selector) and RETROACTIVELY migrates ALL phase content (DISCOVER +
  CONFIRM + RESOLVE_FAQ + RESOLVE_INTAKE + ESCALATE + TERMINAL) +
  Sprint 23/31/33 teaching paragraphs + Sprint 6/7/11 predicates
  into the new abstraction. UC switching + state preservation
  (originally OLD M2-Skill Sprint 39) is integrated as the final
  M2 sub-sprint, designed coherently with the new Skill abstraction.

  M2 takes BIGGER STEPS than the OLD M2-Skill framing per explicit
  human direction 2026-05-17: replace and remove unnecessary
  hardcode FASTER; interactive eval is OBSERVATION only and is
  ALLOWED to regress (eval itself is unstable per §5.5 demotion);
  Alice bad case is OBSERVATION only for THIS milestone (NOT hard
  gate) — the architecture goal is "Skill design itself flexible
  enough to cover variant scenarios" not "specific case PASS
  blocks frame upgrade"; if individual Skill needs tuning AFTER
  framework upgrade is complete, that tuning is easier with the
  abstraction landed. M2 acceptance gate = functional review +
  Java tests pass + Sprint 37 freeze decisions honored across
  implementation sub-sprints. This is a deliberate human-judgment
  recalibration of §5.6 "primary gate" framing for THIS milestone
  (architecture-focused, not behaviour-fix-focused); future
  milestones may revert to bad-case-suite-primary if behaviour-
  focused.

  OLD M2-Skill milestone_objective is archived at
  docs/milestones/M2-Skill_objective.md (status: superseded;
  supersession-mid-flight, only Sprint 36 shipped). Sprint 36 commit
  (ed71031) and its handoff/codex-review archive stay immutable per
  doc_governance.md sprint-archive rule. The Sprint 36 design freeze
  doc (docs/proposals/skill_foundation_design.md) is superseded
  in-place (added frontmatter status: superseded + superseded_by:
  docs/proposals/skill_registry_design.md + new "Why superseded
  2026-05-17" section preserving the minimum-surface reasoning as
  historical record per doc_governance.md "Future proposals"
  supersede-not-delete rule).

  M2 scope discipline: Skill abstraction is the design-freeze
  decision Sprint 37 locks; retroactive migration ships across
  Sprints 38-40 (Sprint 38 SkillRegistry core + 4 simpler phases
  migrated; Sprint 39 the 2 complex phases + all predicate work +
  S1/S2 new predicates + unified dispatcher; Sprint 40 teaching
  paragraphs from system_prompt.txt into Skill YAML + orchestration
  shell cleanup); UC switch + state preservation closes M2 at
  Sprint 41 building on the validated Skill abstraction. No
  per-UC-branch if-else in skill bodies (§1.7); no
  RuntimeIntentClassifier / DriftDetector / UseCaseRouter /
  ClassifyUseCaseTool touch (M3-D deferred); no escalation_reason
  enum widening (M3-A deferred); R-llm-provider-latency-drift-
  2026-05-16 deferred to next milestone (orthogonal infra
  parallel-track, kept in action_bank). Tier-0 invariants may be
  candidate territory at Sprint 37 design freeze close, requiring
  explicit human-review escalation per §10.
---

# Milestone M2 — Skill Registry Abstraction + Wholesale Retroactive Externalization

## 1. Milestone class

**Multi-layer milestone, 5 coordinated sub-sprints across `prompt_projection` + `semantic_planner` + `skill_state` + Runtime-owned floor per Constitution §1.4.** All sub-sprints are semantic-touching and REQUIRE the §7 stanza. Sub-sprint layer breakdown:

| Sub-sprint | Track | Layer | §7 stanza | Codex review |
|---|---|---|---|---|
| Sprint 37 — Skill Registry + state-across-Skill design freeze | A (anchor) | docs/design only (NO `server/` code; produces `docs/proposals/skill_registry_design.md`) | REQUIRED (multi-layer prospective covering Sprints 38/39/40/41) | Per-sub-sprint (§4.3 trigger #1: possible Tier-0 candidate from Skill predicate / tool-whitelist enforcement semantics + #2: §1.7 boundary discussion on abstraction shape) |
| Sprint 38 — SkillRegistry core + 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL) migration | A | `prompt_projection` + `skill_state` (Skill Registry data + loader) + Runtime-owned PhasePlan composition | REQUIRED | Per-sub-sprint (§4.3 trigger #3: new architectural surface — SkillRegistry mediates between LLM prompt context and tool dispatch) |
| Sprint 39 — RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified Skill terminal-predicate dispatcher | A | `prompt_projection` + `semantic_planner` + `skill_state` + Runtime-owned grounding floor + Runtime-owned capability floor per §1.4 | REQUIRED | Per-sub-sprint (§4.3 trigger #3: Runtime grounding-floor surface + Runtime intake capability floor + unified dispatcher = sensitive surface; multi-fence convergence at one sub-sprint) |
| Sprint 40 — Teaching extraction from `system_prompt.txt` (Sprint 23/31/33 paragraphs) + orchestration shell cleanup | A | `prompt_projection` (system_prompt.txt structure) + `semantic_planner` (LLM input contract change) | REQUIRED | Per-sub-sprint (§4.3 trigger #3: system_prompt.txt structural change — LLM input contract surface) |
| Sprint 41 — UC switch + state preservation across Skill boundary | A | `skill_state` + `prompt_projection` | REQUIRED | Per-sub-sprint (§4.3 trigger #3: session state model touch across Skill boundary) |

**Codex review default for M2: PER-SUB-SPRINT for ALL semantic-touching sub-sprints** (Sprints 37, 38, 39, 40, 41). This DIFFERS from §4.3 default (milestone-shared) because every M2 semantic-touching sub-sprint hits a §4.3 trigger (new architectural surface; Runtime-owned floor touch; session state model touch; potential Tier-0 candidate). Plus a cumulative architectural-posture check at M2 close.

## 2. Goal

Promote the Sprint 5 (F2) skill-orchestration proposal (`docs/proposals/skill_orchestration_candidates.md`, status `proposal` / `not_started`) to code via a **Skill Registry abstraction** in which a Skill is a first-class externalized definition (YAML/JSON file under `server/src/main/resources/skills/`) carrying: `name`, `description`, `applicable_phases`, `applicable_use_cases`, `tools_required` (tool whitelist), `procedure` (recommended natural-language flow surfaced as prompt teaching), `guardrails` (LLM-soft + Runtime-hard). The `PhaseEvaluator.java` becomes a Skill Selector that queries the Skill Registry on (phase, use_case) and composes the resulting Skill with session state into a `PhasePlan` for the per-turn LLM invocation. `system_prompt.txt` becomes a thin orchestration shell teaching the LLM how to read Skill envelopes; the procedural teaching content moves into Skill `procedure` blocks. Existing scattered Sprint 6/7/11 Java predicates in `AgentRunLoopImpl.java` are retroactively factored into Skill `guardrails` enforced via a unified Skill terminal-predicate dispatcher.

Concretely M2 ships:

- **Skill Registry abstraction landed** (Sprint 37 freeze + Sprint 38 core). A new `SkillRegistry` Java class loads YAML/JSON Skill definitions from `server/src/main/resources/skills/`; provides `select(phase, useCase) → Skill`; integrates with `PhaseEvaluator` such that the existing `PhasePlan` data shape (tool whitelist + system instruction + grounding instruction + escalation policy) is now composed from the selected Skill rather than hardcoded in Java strings.
- **All 6 phase Skills externalized to YAML** (Sprint 38 + Sprint 39). Sprint 38 migrates the 4 simpler phases first (DISCOVER + CONFIRM + ESCALATE + TERMINAL — `skills/discover_triage.yaml`, `skills/confirm.yaml`, `skills/escalate.yaml`, `skills/terminal.yaml`); Sprint 39 migrates the 2 complex resolution phases (RESOLVE_FAQ + RESOLVE_INTAKE — `skills/resolve_faq_grounded_answer.yaml`, `skills/resolve_intake_collect_and_handover.yaml`). Post-Sprint-39 PhaseEvaluator carries ZERO hardcoded systemInstruction / groundingInstruction / escalationPolicy Java strings — all phase content lives in Skill YAML.
- **All scattered predicates migrated to unified Skill terminal-predicate dispatcher** (Sprint 39). Sprint 6 `shouldRejectFaqMissHandover` (`AgentRunLoopImpl.java:690-734`) + Sprint 7 `shouldRejectIncompleteIntakeHandover` (`AgentRunLoopImpl.java:628-651`) + Sprint 11 `shouldRejectPrematureResolveOutcome` (`AgentRunLoopImpl.java:745-759`) factored into Skill `guardrails` enforced via the unified Skill terminal-predicate dispatcher. NEW S1 `must_cite_source` predicate (refuses `record_outcome(class=resolve)` persistence without `source_id` citation present in user-facing message; scoped to RESOLVE_FAQ only per §6 #4 bounded authorization) declared in `resolve_faq_grounded_answer.yaml` `guardrails`. NEW S2 `intake_complete_required` predicate (downgrades `request_handover(escalation_reason=intake_complete_for_uc_X)` to `incomplete_intake` until `requiredIntakeFields` fully populated per M1 Sprint 34 extractor) declared in `resolve_intake_collect_and_handover.yaml` `guardrails`. Post-Sprint-39 `AgentRunLoopImpl.java` carries ZERO `shouldRejectXxx` Java methods — all enforcement lives in Skill `guardrails` + the dispatcher.
- **All scattered teaching paragraphs migrated from system_prompt.txt to Skill YAML** (Sprint 40). Sprint 23 `already_called` (`system_prompt.txt:23-28`) + Sprint 31 `alternate_candidate_use_cases` (`system_prompt.txt:30-33`) + Sprint 33 `discover_disambiguation_signals` (`system_prompt.txt:36-40`) factored into corresponding Skill `procedure` / `guardrails` blocks per Sprint 37 freeze mapping. Post-Sprint-40 `system_prompt.txt` is a thin orchestration shell teaching the LLM how to read Skill envelopes; phase-specific behavioural teaching lives in Skill YAML.
- **UC switch + state preservation across Skill boundary landed** (Sprint 41). Session-level state model (`accumulated_tool_results` / `customerContext` / `intakeFields` partial / prior grounding citations / prior Skill identifier) survives Skill switches per a per-Skill `state_inheritance` declaration in Skill YAML AND a session-level state-bus that mediates cross-Skill state visibility. UC switch detection rides on existing M1 Sprint 32 `alternate_candidate_use_cases` projection + M1 Sprint 33 `discover_disambiguation_signals` (NOT a new classifier — per §6 #5 fence). New `prior_use_case_carry` projection slot surfaces continuity state as soft signal to the LLM; LLM owns whether to surface continuity to user, ask confirmation, OR proceed per Constitution §1.3.

## 3. Sub-sprint sequence (preliminary; deliver-agent + human refine at each sub-sprint planning round)

### Sprint 37 — Skill Registry + state-across-Skill design freeze

**Track:** A (anchor). **Layer:** docs/design only (NO `server/` code; NO test code). **§7 stanza:** REQUIRED (multi-layer prospective covering Sprints 38/39/40/41 layer breakdown). **Codex:** per-sub-sprint (§4.3 trigger #1: possible Tier-0 candidate from Skill terminal-predicate / tool-whitelist enforcement semantics; §4.3 trigger #2: §1.7 boundary discussion on Skill abstraction shape).

**Scope:** Produce an architectural decision doc at `docs/proposals/skill_registry_design.md` that locks 10 design decisions:

- (a) **Skill data model** — YAML/JSON external file fields + schema validation contract; field set (`name`, `description`, `applicable_phases`, `applicable_use_cases`, `tools_required`, `procedure`, `guardrails`, `state_inheritance`); JSON Schema if used.
- (b) **Skill Registry shape** — Java class location (likely `service/runtime/skill/SkillRegistry.java`), loader (`SkillLoader.java`), fallback semantics on no-Skill-found (default-Skill OR exception OR pre-defined "null Skill"), Skill versioning if any.
- (c) **PhaseEvaluator-as-Skill-Selector integration** — exact mechanism: `phaseEvaluator.evaluate(...)` queries `skillRegistry.select(phase, useCase)`; Skill composed with session state into PhasePlan; backward-compatible PhasePlan data shape; intermediate state during migration (some phases on Skill, some still hardcoded — but per design freeze § Migration Order, Sprint 38 migrates all 4 simpler phases at once, Sprint 39 migrates remaining 2, so intermediate state is small).
- (d) **Skill `procedure` (LLM-soft prompt teaching) vs `guardrails` (LLM-soft vs PhasePlan.allowedTools hard tool whitelist vs unified Java terminal-predicate dispatcher) responsibility split** — what content goes where; how a guardrail is declared (DSL? declarative fields like `must_cite_source: true`? freeform constraint expression?); how the dispatcher discovers Skill-declared guardrails at runtime.
- (e) **Retroactive migration mapping for ALL 6 phase YAMLs** — for each phase (DISCOVER, CONFIRM, RESOLVE_FAQ, RESOLVE_INTAKE, ESCALATE, TERMINAL), map current PhaseEvaluator Java-string `systemInstruction` / `groundingInstruction` / `escalationPolicy` content to its target Skill YAML field; identify content overlap across phases (if any); document behavioural equivalence test pattern (pre/post-migration PhasePlan observationally identical).
- (f) **Retroactive migration mapping for Sprint 23/31/33 teaching paragraphs** — for each paragraph in `system_prompt.txt`, decide whether it goes into one Skill's `procedure`, multiple Skills' `procedure`, the orchestration shell that remains in `system_prompt.txt`, OR a Skill's `guardrails` block. Document the decision rationale (cross-Skill teaching → shell; phase-specific teaching → Skill).
- (g) **Retroactive migration mapping for Sprint 6/7/11 predicates** — for each Java method (`shouldRejectFaqMissHandover`, `shouldRejectIncompleteIntakeHandover`, `shouldRejectPrematureResolveOutcome`), decide which Skill `guardrails` block hosts it; document how the unified Skill terminal-predicate dispatcher composes Skill-declared guardrails into runtime enforcement.
- (h) **Unified Skill terminal-predicate dispatcher design** — placement (in `AgentRunLoopImpl` OR new `service/runtime/skill/SkillGuardrailDispatcher.java`); composition order (multiple guardrails on same Skill — short-circuit OR all-must-pass); failure mode (rejection diagnostic shape — what trace entry is written; LLM-visible message structure).
- (i) **Session-level state model + per-Skill `state_inheritance` semantics** — what the session state-bus carries (existing `customerContext` / `intakeFields` / `accumulated_tool_results` + new fields if any); how `state_inheritance` is declared per Skill (e.g., `inherit: [customer_context, accumulated_tool_results]`, `reset: [intake_fields]`, `soft_signal_via_projection: [prior_citations]`); UC switch trigger conditions (rides on M1 Sprint 32/33 projections, not a new classifier).
- (j) **§4.1 anti-hardcode kernel walk-through** on the proposed design surfacing any Tier-0 invariant candidate (e.g., "tool whitelist enforcement is unconditional regardless of Skill `procedure`"; "Skill terminal predicate refusal is non-overridable by LLM"; "Skill-declared `state_inheritance` is enforced at session-state-bus boundary"). For each candidate, name whether it's load-bearing enough to be Tier-0 OR whether it stays a §1.4 Runtime-floor guarantee that the Skill abstraction enforces by construction.

Sprint 37 walks the §4.1 nine-question anti-hardcode kernel against the proposed design pre-implementation; surfaces any Tier-0 candidate for explicit human-review escalation per §10 stop condition #1. Sprint 37 does NOT write `server/` code; it produces docs that Sprints 38 / 39 / 40 / 41 implement, scoped tight enough that downstream sub-sprint contracts can be drafted directly from the freeze without further architectural rounds.

**Files in scope (preliminary; dev refines):**

- **NEW**: `docs/proposals/skill_registry_design.md` — architectural decision doc; locks (a)-(j) above; carries §4.1 anti-hardcode walk-through + 10 design-decision sections.
- **EDIT** (may be deliver-agent at M2 approval bundle rather than Sprint 37 dev): `docs/proposals/skill_foundation_design.md` — add frontmatter `status: superseded` + `superseded_by: docs/proposals/skill_registry_design.md` + new "Why superseded (2026-05-17)" section.
- **EDIT** (may be deliver-agent): `docs/proposals/skill_orchestration_candidates.md` — verify chained `superseded_by` pointer.
- **NEW (if surfaced at Sprint 37 close):** explicit Tier-0 candidate write-up file — Sprint 37 may surface that Skill tool-whitelist enforcement OR Skill terminal-predicate refusal IS a candidate Tier-0 invariant for `docs/runtime_freeze_and_risk_policy.md` §1/§2. If so, file the candidate at Sprint 37 close for explicit human-review escalation (STOP per §10 condition #1) BEFORE Sprint 38 begins.
- **UPDATED**: `docs/action_bank.md` (deliver-agent at sub-sprint close, not dev) — flip relevant R-items to Sprint-37-locked; surface new R-items from freeze discussion.

**Hard fences (Sprint 37):** NO `server/` code change; NO test code; NO `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` edit; NO Sprint 38+ implementation pre-commit (those are separate sub-sprint planning rounds with separate Codex review).

### Sprint 38 — SkillRegistry core + 4 simpler phase Skills migration (DISCOVER + CONFIRM + ESCALATE + TERMINAL)

**Track:** A. **Layer:** `prompt_projection` (Skill envelope reaches LLM via Skill-composed PhasePlan) + `skill_state` (SkillRegistry holds Skill definitions across requests) + Runtime-owned PhasePlan composition per §1.4. **§7 stanza:** REQUIRED. **Codex:** per-sub-sprint (§4.3 trigger #3: new architectural surface — SkillRegistry mediates between LLM prompt context and tool dispatch).

**Scope:** Implement the SkillRegistry per the Sprint 37 freeze: new `SkillRegistry` Java class loads YAML/JSON Skill definitions from `server/src/main/resources/skills/`; new `SkillLoader` parses + schema-validates Skill files; modify `PhaseEvaluator.java` to query SkillRegistry on (phase, useCase) and compose Skill + session state into PhasePlan. Migrate 4 simpler phase Skills (DISCOVER + CONFIRM + ESCALATE + TERMINAL) as the first migration wave — these 4 phases carry less embedded predicate logic than RESOLVE_FAQ / RESOLVE_INTAKE (deferred to Sprint 39), so their migration is mostly mechanical YAML extraction + behavioural equivalence verification. Verify zero behavioural regression on existing tests for migrated phases (pre/post-migration PhasePlan observationally identical for representative UCs in each phase).

**Files in scope (preliminary):**

- NEW: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (Skill data class per Sprint 37 freeze).
- NEW: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java` (registry + `select(phase, useCase)` method).
- NEW: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java` (YAML/JSON loader + schema validation).
- NEW (4 Skill files): `server/src/main/resources/skills/discover_triage.yaml` + `skills/confirm.yaml` + `skills/escalate.yaml` + `skills/terminal.yaml` (extracted phase content per Sprint 37 freeze field mapping).
- EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (DISCOVER + CONFIRM + ESCALATE + TERMINAL branches: replace hardcoded Java-string content with SkillRegistry.select() call + composition). RESOLVE_FAQ + RESOLVE_INTAKE branches stay UNCHANGED in Sprint 38 (Sprint 39 scope).
- NEW tests: `SkillTest`, `SkillRegistryTest`, `SkillLoaderTest`, `PhaseEvaluatorSkillIntegrationTest` (behavioural-equivalence test for 4 migrated phases: pre/post-migration PhasePlan observationally identical for representative UCs).
- POSSIBLY: `eval_interactive/case_specs/` — NO expected edit; defer to Sprint 39+ if any case-family change is warranted.

**Hard fences (Sprint 38):** NO migration of RESOLVE_FAQ / RESOLVE_INTAKE Skills (Sprint 39 scope); NO migration of `system_prompt.txt` teaching paragraphs (Sprint 40 scope); NO migration of Sprint 6/7/11 AgentRunLoopImpl predicates (Sprint 39 scope); NO addition of S1/S2 predicates (Sprint 39 scope); NO unified Skill terminal-predicate dispatcher (Sprint 39 scope — depends on Skill guardrails fields, which are migrated in Sprint 39); NO UC-switch state preservation (Sprint 41 scope); NO touch to `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter`; NO `escalation_reason` enum widening; NO Tier-0 invariant added without Sprint 37 freeze pre-authorization + human escalation; NO edit to existing case families (cascade fence preserved).

### Sprint 39 — RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified Skill terminal-predicate dispatcher

**Track:** A. **Layer:** `prompt_projection` + `semantic_planner` + `skill_state` + Runtime-owned grounding floor + Runtime-owned capability floor per §1.4. **§7 stanza:** REQUIRED. **Codex:** per-sub-sprint (§4.3 trigger #3: Runtime grounding-floor + capability-floor multi-fence convergence at one sub-sprint; Codex verifies S1 predicate scope matches §6 #4 verbatim authorization).

**Scope:** Migrate the 2 complex resolution-path Skills + all predicate work in one sub-sprint. Migrate RESOLVE_FAQ phase content (PhaseEvaluator.java:633-665 systemInstruction/groundingInstruction/escalationPolicy) into `skills/resolve_faq_grounded_answer.yaml`; migrate RESOLVE_INTAKE phase content into `skills/resolve_intake_collect_and_handover.yaml`. Introduce the unified Skill terminal-predicate dispatcher (in `AgentRunLoopImpl` OR new `SkillGuardrailDispatcher.java` per Sprint 37 freeze decision (h)). Factor Sprint 6 `shouldRejectFaqMissHandover` (AgentRunLoopImpl.java:690-734) + Sprint 11 `shouldRejectPrematureResolveOutcome` (AgentRunLoopImpl.java:745-759) into `resolve_faq_grounded_answer.yaml` `guardrails` block. Factor Sprint 7 `shouldRejectIncompleteIntakeHandover` (AgentRunLoopImpl.java:628-651) into `resolve_intake_collect_and_handover.yaml` `guardrails` block. Add NEW S1 `must_cite_source` guardrail (refuse `record_outcome(class=resolve)` persistence without `source_id` citation present in user-facing message; scoped to RESOLVE_FAQ phase only per §6 #4 verbatim authorization) to `resolve_faq_grounded_answer.yaml` `guardrails`. Add NEW S2 `intake_complete_required` guardrail (downgrade `request_handover(escalation_reason=intake_complete_for_uc_X)` to `incomplete_intake` until `requiredIntakeFields` fully populated per M1 Sprint 34 extractor) to `resolve_intake_collect_and_handover.yaml` `guardrails`. Post-Sprint-39: PhaseEvaluator has ZERO hardcoded systemInstruction/groundingInstruction/escalationPolicy across all 6 phases; AgentRunLoopImpl has ZERO `shouldRejectXxx` Java methods (all replaced by dispatcher + Skill-declared guardrails).

**Files in scope (preliminary):**

- NEW: `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` (Skill definition; `guardrails`: migrated Sprint 6 FAQ-miss-handover refusal + Sprint 11 premature-resolve refusal + NEW S1 `must_cite_source`).
- NEW: `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` (Skill definition; `guardrails`: migrated Sprint 7 incomplete-intake-handover refusal + NEW S2 `intake_complete_required`).
- NEW (or EDIT to AgentRunLoopImpl): `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java` (unified terminal-predicate dispatcher per Sprint 37 freeze (h)).
- EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (RESOLVE_FAQ + RESOLVE_INTAKE branches: replace hardcoded Java-string content with SkillRegistry.select() call; same pattern as Sprint 38).
- EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (REMOVE `shouldRejectFaqMissHandover` + `shouldRejectIncompleteIntakeHandover` + `shouldRejectPrematureResolveOutcome` Java methods; their semantics moved into Skill `guardrails`; dispatcher routes; remove related FAQ_PATH_UCS hardcoded references if any).
- POSSIBLY EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/EscalationReasonResolver.java` (S2 predicate fires here OR in dispatcher per Sprint 37 freeze).
- NEW tests: `ResolveFaqSkillTest`, `ResolveIntakeSkillTest`, `SkillGuardrailDispatcherTest`, `ResolveFaqGuardrailsTest` (FAQ-miss + premature-resolve + S1 citation), `ResolveIntakeGuardrailsTest` (incomplete-intake + S2 intake completeness); Sprint71 regression preservation test (Sprint71PartialIntakePersistenceTest stays 14/14).
- POSSIBLY: `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (closure-criterion refinement at Sprint 39 close if observed traces warrant; per §5.6 lifecycle).

**Hard fences (Sprint 39):** **NO expansion of S1 predicate beyond human-authorized scope per §6 #4 verbatim** (citation on `class=resolve` only; RESOLVE_FAQ phase only; citation presence only — NOT content quality, NOT enforcement on `class=escalate/abandon`, NOT fan-out beyond RESOLVE_FAQ); NO migration of system_prompt.txt teaching paragraphs (Sprint 40 scope); NO UC-switch state preservation (Sprint 41 scope); NO touch to `RuntimeIntentClassifier` / `DriftDetector` / `UseCaseRouter`; NO `escalation_reason` enum widening; NO Tier-0 invariant added without Sprint 37 freeze pre-authorization + human escalation; NO new outcome class (e.g., `resolve_no_citation`) — rejection (reject-and-hint) is the S1 fail behaviour per OLD Sprint 36 OQ 7.3 default carried forward; NO touch to `IntakeFieldsRegistry.java` / `IntakeFieldExtractor.java` (M1 Sprint 34 schema preserved); NO regression on Sprint71PartialIntakePersistenceTest 14/14 baseline.

### Sprint 40 — Teaching extraction from `system_prompt.txt` + orchestration shell cleanup

**Track:** A. **Layer:** `prompt_projection` (system_prompt.txt structure) + `semantic_planner` (LLM input contract change). **§7 stanza:** REQUIRED. **Codex:** per-sub-sprint (§4.3 trigger #3: system_prompt.txt structural change — LLM input contract surface).

**Scope:** Migrate the 3 scattered teaching paragraphs from `system_prompt.txt` into corresponding Skill YAML files per Sprint 37 freeze mapping (decision (f)). Specifically: Sprint 23 `already_called` paragraph (`system_prompt.txt:23-28`) into the Skill `procedure` or `guardrails` block per freeze; Sprint 31 `alternate_candidate_use_cases` paragraph (`system_prompt.txt:30-33`) into `discover_triage.yaml` (most likely) per freeze; Sprint 33 `discover_disambiguation_signals` paragraph (`system_prompt.txt:36-40`) into `discover_triage.yaml` per freeze. After migration, `system_prompt.txt` shrinks to a thin orchestration shell teaching the LLM how to read Skill envelopes (e.g., "you operate within the Skill the runtime selects for the current phase; the Skill envelope names the tool whitelist, recommended order, and guardrails; respect the envelope but exercise judgment on response strategy per Constitution §1.3"). Verify behavioural equivalence: pre/post-migration LLM behaviour on representative DISCOVER + RESOLVE_FAQ + RESOLVE_INTAKE traces is observationally similar (LLM still observes the teaching content; just sourced from Skill YAML instead of monolithic system_prompt.txt).

**Files in scope (preliminary):**

- EDIT: `server/src/main/resources/prompts/system_prompt.txt` (REMOVE Sprint 23 + Sprint 31 + Sprint 33 teaching paragraphs; ADD thin orchestration-shell teaching about Skill envelopes; shrink token count materially).
- EDIT: `server/src/main/resources/skills/discover_triage.yaml` (extend with migrated Sprint 31 + Sprint 33 teaching content in `procedure` or `guardrails` block per Sprint 37 freeze).
- EDIT: `server/src/main/resources/skills/resolve_faq_grounded_answer.yaml` (extend with migrated Sprint 23 `already_called` teaching content per Sprint 37 freeze — IF mapped to this Skill; alternatively to multiple Skills or orchestration shell per freeze).
- POSSIBLY EDIT: other Skill YAMLs receiving fragments of Sprint 23 `already_called` per cross-Skill mapping if Sprint 37 freeze decides distribution.
- NEW tests: `OrchestrationShellTest` (verifies thin system_prompt.txt structure post-migration), `SkillTeachingMigrationIntegrationTest` (real-LLM tests confirming the migrated teaching content reaches the LLM via Skill envelope projection); possibly update existing `SystemPromptUserRequestedTiebreakerTest` if the inherited failure resolves after teaching reorganization.

**Hard fences (Sprint 40):** NO addition of new predicates (Sprint 39 work; Sprint 40 is teaching-content-relocation only); NO migration of Java-side phase content (Sprint 38 + 39 work; Sprint 40 is prompt-side cleanup only); NO UC-switch state preservation (Sprint 41 scope); NO addition of per-UC-branch if-else into Skill YAML `procedure` blocks per §1.7; NO Tier-0 invariant added without Sprint 37 freeze pre-authorization + human escalation; NO change to Skill data model (Sprint 37 freeze).

### Sprint 41 — UC switch + state preservation across Skill boundary

**Track:** A. **Layer:** `skill_state` (session-level state-bus + per-Skill `state_inheritance`) + `prompt_projection` (new `prior_use_case_carry` slot surfaces continuity as soft signal). **§7 stanza:** REQUIRED. **Codex:** per-sub-sprint (§4.3 trigger #3: session state model touch across Skill boundary).

**Scope:** Implement the UC-switch + state preservation per the Sprint 37 freeze decisions: session-level state-bus model (`accumulated_tool_results` / `customerContext` / `intakeFields` partial / prior grounding citations / prior Skill identifier) survives Skill switches; per-Skill `state_inheritance` declaration in Skill YAML names what state the new Skill inherits, resets, or sees as soft signal from the prior Skill; UC switch detection rides on existing M1 Sprint 32 `alternate_candidate_use_cases` projection + M1 Sprint 33 `discover_disambiguation_signals` (NOT a new classifier). Add new `prior_use_case_carry` projection slot in `ContextProjectionBuilder.java` surfacing the prior Skill identifier + inherited state to the LLM as soft signal; LLM owns whether to surface continuity to user, ask confirmation, OR proceed per Constitution §1.3. Anchored on observable behavioural evidence (real-LLM trace showing UC switch preserves prior state) + functional review at Sprint 41 close; NOT a hard gate on any specific bad case (per M2 §5 acceptance recalibration).

**Files in scope (preliminary; dev refines per Sprint 37 freeze):**

- NEW: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java` (session-level state-bus mediating cross-Skill state visibility per Sprint 37 freeze (i)).
- EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java` (add `state_inheritance` field per Sprint 37 freeze schema).
- EDIT: all 6 Skill YAMLs (`discover_triage.yaml`, `confirm.yaml`, `resolve_faq_grounded_answer.yaml`, `resolve_intake_collect_and_handover.yaml`, `escalate.yaml`, `terminal.yaml`) each declares its `state_inheritance` block per Sprint 37 freeze.
- EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/AgentSession.java` (or equivalent session-state holder; integrate SkillStateBus on Skill switch).
- EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (NEW `prior_use_case_carry` projection slot — soft signal of prior Skill identifier + inherited state).
- EDIT: `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` (UC-switch transition handling: invoke SkillStateBus on Skill change per `state_inheritance` declaration).
- POSSIBLY EDIT: `server/src/main/resources/prompts/system_prompt.txt` (orchestration-shell teaching about prior_use_case_carry projection slot IF Sprint 37 freeze maps it as cross-Skill teaching).
- NEW tests: `SkillStateBusTest`, `UcSwitchStateInheritanceTest`, `PriorUseCaseCarryProjectionTest`, integration tests for representative UC switch scenarios.
- POSSIBLY NEW: `eval_interactive/case_specs/bad_cases/uc_switch_state_preservation_*.yaml` (deliver-agent + human review at Sprint 41 planning round per §5.6 lifecycle if a load-bearing UC-switch scenario is identified).

**Hard fences (Sprint 41):** **NO touch to `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java`** (M3-D Topic↔UC binding loosening stays deferred); NO change to `INTAKE_UCS` set at `PhaseEvaluator.java:30` (M3-B Single Handover Orchestrator deferred); NO new tool for UC-switch detection (rides on existing projections); §1.7 **per-UC-pair if-else logic explicitly forbidden** in Skill `state_inheritance` declarations — continuity invariants are stated as principles + observable-state guards, NOT as per-UC-pair branch tables; NO `escalation_reason` enum widening; NO Tier-0 invariant addition without Sprint 37 freeze pre-authorization + human escalation; NO regression on Sprint 38-40 migrated Skills behavioural equivalence.

## 4. Non-goals (explicit)

- M2 does NOT consume the original M2-customer-honesty work — sibling rationale/confidence fields on `request_handover`, handover human UX rewrite, URL policy + factual narrowing. All three deferred to M3-A per OLD M2-Skill Decision β-2026-05-17 (carried forward).
- M2 does NOT touch `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` semantic surfaces. **M3-D Topic↔UC binding loosening** is deferred to M3+ per M1 close Decision C 2026-05-17 (carried forward).
- M2 does NOT consume `D-single-handover-orchestrator` (M3-B P0 deferred unless Salesforce cutover calendar pressure surfaces; if it surfaces, deliver-agent + human re-scope M2 to fit OR open M3 immediately).
- M2 does NOT consume `D-full-issue-ledger` (hard-deferred per `docs/action_bank.md`; requires explicit new-objective approval).
- M2 does NOT consume `R-loosen-topic-uc-binding-llm-owned-drift` (M3-D deferred per M1 close Decision C 2026-05-17).
- M2 does NOT consume Sprint 5 S3 (Soft-OOS triage) or S4 (Account login-recovery) or S5 (Tier-2-reason UC-compat) — deferred to M3-C per OLD M2-Skill Decision β-Q2 2026-05-17 (carried forward; rides on the validated M2 Skill abstraction in M3-C).
- M2 does NOT consume `R-llm-provider-latency-drift-2026-05-16` — orthogonal infra parallel-track deferred to next milestone (orthogonal to Skill architecture; preserved in `docs/action_bank.md` deferred).
- M2 does NOT add Tier-0 invariants without Sprint 37 design-freeze pre-authorization + explicit human-review escalation (Sprint 37 may surface a Tier-0 candidate from Skill predicate / tool-whitelist enforcement semantics — STOP and escalate per §10).
- M2 does NOT widen the `escalation_reason` enum at `PhaseEvaluator.java:39-63` (`D-new-escalation-reason-enum` deferral preserved; sibling rationale/confidence work deferred to M3-A).
- M2 does NOT widen judge rubrics or `eval_interactive/case_spec_overrides.yaml`.
- M2 does NOT edit existing case families under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence preserved from M1).
- M2 does NOT touch `eval_interactive/eval_interactive/` (harness, loader, simulator).
- M2 does NOT pre-decide Sprint 38 / 39 / 40 / 41 implementation specifics — those sub-sprint contracts get drafted one at a time per deliver-agent + human round AFTER Sprint 37 freeze closes (the freeze is the upstream architectural decision; downstream contracts ride on it).
- M2 does NOT promote any existing case family or shadow case family to bad-case suite without explicit deliver-agent + human curation per §5.6 lifecycle.
- M2 does NOT gate close on Alice bad case behaviour OR any interactive eval metric — per §5 acceptance recalibration: those are observations, not gates, for THIS milestone (architecture-focused).

## 5. Milestone acceptance bar (recalibrated 2026-05-17 per human direction)

**Recalibration rationale.** §5.6 names the curated bad-case suite (manual review) as the primary acceptance gate for sprint / milestone close. For THIS milestone (M2 — architecture-focused, NOT behaviour-fix-focused), the human + deliver-agent at M2 approval round (2026-05-17) jointly recalibrate the primary gate to functional review + Java tests pass + Sprint 37 freeze decisions honored across implementation sub-sprints. The bad-case suite (Alice) and interactive eval (smoke composite_score + judge dims) remain **observations** (recorded at each sub-sprint close + M2 close), but they do NOT block close — they may even regress and the milestone may still close.

Rationale for recalibration:

1. **Architecture-focused milestone**: M2 ships the Skill abstraction; behavioural improvements (e.g., Alice closure-criterion (a) PASS) ride on individual Skill tuning, which is EASIER after the abstraction lands than before. Gating M2 on specific case behaviour creates a misaligned acceptance bar (architecture work doesn't directly fix case behaviour; case behaviour fix doesn't require the architecture).
2. **Eval instability**: interactive eval composite_score per §5.5 is observation only (demoted 2026-05-16 due to multiple confounding sources: external LLM provider drift; judge calibration variance; mocked-vs-real-LLM gap; rubric stale weights). Re-using it as M2 gate would re-import the instability the §5.5 demotion was designed to escape.
3. **Case sparsity**: only 1 bad case in `case_specs/bad_cases/` (Alice) at M2 start. Single-case gates are vulnerable to case-specific variance (e.g., Alice may query FAQ in one trace + query own existing post in another; Skill design SHOULD cover both flexibly, not blocked on one trace pattern). Skill design itself should be flexible enough to cover variant scenarios; a specific case PASS shouldn't block frame upgrade.
4. **"Easier to tune Skill after abstraction lands"** principle: with the Skill Registry in place, tuning a Skill's `procedure` / `guardrails` to better handle Alice (or any specific case) is a localized edit; without the Skill Registry, the same tuning requires PhaseEvaluator + AgentRunLoopImpl + system_prompt.txt coordinated changes per the old scattered pattern.

This is a deliberate human-judgment recalibration of §5.6 "primary gate" framing for THIS milestone. Future milestones (especially M3-A customer-honesty surface OR M3-B Single Handover Orchestrator) may revert to bad-case-suite-primary if behaviour-focused. The recalibration is documented here + carries forward to per-sub-sprint contracts + Codex review prompts; Codex per-sub-sprint review prompts will state that bad-case suite results are observation only for THIS milestone.

**Primary gate (M2 close):**

- **All 5 sub-sprints' contracted deliverables shipped** per §3 (Sprint 37 design freeze doc; Sprint 38 SkillRegistry core + 4 phase migrations; Sprint 39 RESOLVE_FAQ + RESOLVE_INTAKE migration + predicate migration + S1/S2 + dispatcher; Sprint 40 teaching extraction + orchestration shell cleanup; Sprint 41 UC switch + state preservation).
- **Sprint 37 freeze decisions honored across implementation sub-sprints**. Each sub-sprint handoff cites Sprint 37 freeze sections (a)-(j); deliver-agent + human verify at sub-sprint close that implementation matches freeze.
- **Java test suite no regression on existing tests** (post-M1 baseline 983 preserved; inherited `SystemPromptUserRequestedTiebreakerTest` failure may resolve or persist depending on `system_prompt.txt` final shape; deliver-agent + human evaluate at Sprint 40 close).
- **New Skill abstraction tests pass** (SkillTest, SkillRegistryTest, SkillLoaderTest, SkillGuardrailDispatcherTest, per-Skill tests, behavioural equivalence tests across all 6 migrated phases).
- **Sprint71PartialIntakePersistenceTest 14/14 preserved through M2** (UC-K regression guard carried from M1).
- **Per-sub-sprint Codex anti-hardcode kernel verdict: `approve`** (no per-UC-branch if-else in any Skill body — neither YAML nor Java; no §1.7 boundary violation; no unwarranted Tier-0 candidate; no eval-spec widening).
- **Functional human review at M2 close**: deliver-agent + human qualitative review confirms:
  - SkillRegistry abstraction works as designed (all 6 phase Skills loadable; PhaseEvaluator queries SkillRegistry; PhasePlan composed correctly).
  - `AgentRunLoopImpl` post-M2 carries ZERO `shouldRejectXxx` Java methods (all migrated to Skill guardrails + unified dispatcher).
  - `PhaseEvaluator.java` post-M2 carries ZERO hardcoded `systemInstruction` / `groundingInstruction` / `escalationPolicy` Java strings (all migrated to Skill YAML).
  - `system_prompt.txt` post-M2 is materially shrunken (Sprint 23/31/33 teaching paragraphs extracted; orchestration-shell remains).
  - UC switch + state preservation functionally works in representative real-LLM traces (qualitative review, not a metric).

**Secondary observations (informational; NOT gates; MAY regress):**

- Alice bad case behaviour at M2 close (re-run `case_specs/bad_cases/`; record closure-criterion (a) result + fabrication-condition trigger count). MAY REGRESS or stay flat; does NOT block M2 close.
- Interactive eval smoke composite_score / pass_rate / judge dims (re-run at M2 close for tracking). MAY REGRESS per §5.5 demotion; does NOT block.
- Architecture-health metrics direction (§6 governance):
  - `new_semantic_hardcode_count` expected = 0 (M2 ships zero per-UC-branch if-else; Skill `procedure` and `guardrails` are principle-level).
  - `soft_signal_conversion_count` expected ≥ 5 (DISCOVER + CONFIRM + RESOLVE_FAQ + RESOLVE_INTAKE + ESCALATE/TERMINAL phase teaching moved from hardcoded Java strings + system_prompt.txt to externalized Skill YAML; Sprint 6/7/11 predicates moved from scattered Java methods to Skill guardrails dispatcher).
  - `planner_ownership_ratio` expected NOT decreased (Skill `procedure` is LLM-soft teaching; Skill `guardrails` enforces Runtime-owned floor only per §1.4; LLM retains §1.3 ownership of UC hypothesis, drift, escalation posture, response strategy, customer-facing wording).
- `system_prompt.txt` token count reduction (target: meaningful; specific target deferred to Sprint 37 freeze + measured at Sprint 40 close).
- `AgentRunLoopImpl.java` `shouldRejectXxx` Java method count reduction (target: 0 post-Sprint-39; replaced by Skill guardrails dispatcher).
- `PhaseEvaluator.java` line-count reduction (target: significant; phase content extracted to Skill YAML; PhaseEvaluator becomes thin Skill Selector).

## 6. Hard fences (milestone-level)

1. **No per-UC-branch if-else in any Skill body** (YAML `procedure` text, YAML `guardrails`, Java SkillRegistry/Skill/SkillLoader/dispatcher code, or system_prompt.txt). Skills encode envelope + recommended order + terminal predicate at PRINCIPLE level; they do NOT contain `if UC == X: do Y; if UC == Z: do W`. Per Constitution §1.7. Enforced by Codex per-sub-sprint review on Sprints 38 / 39 / 40 / 41.
2. **Skill terminal predicate may be a Java enforcement ONLY if it protects Runtime-owned floor** per Constitution §1.4 (grounding-citation, capability, safety, PII, idempotency). It may NOT enforce LLM-owned next-action / UC-hypothesis / response strategy / customer-facing language choices per §1.3.
3. **Skill `procedure` (recommended order, soft teaching) is soft, not hard.** LLM picks which whitelisted tool to call when within the envelope; `procedure` surfaces the recommended order as guidance, not enforcement. Tool whitelist `tools_required` IS hard (enforced by `PhasePlan.allowedTools` composed from Skill).
4. **HARD FENCE INVERSION from M2-customer-honesty draft (preserved from OLD M2-Skill §6 #4; carried forward with rationale + human authorization):** The OLD M2-customer-honesty draft barred Java grounding-citation gate per `D-hard-citation-gate` deferral. M2 (this milestone) INVERTS this: S1's terminal predicate (refuse `record_outcome(class=resolve)` persistence without `source_id` citation present in user-facing message) IS a legitimate Runtime grounding-floor guard per §1.4. The inversion is BOUNDED: the predicate fires ONLY within S1's `RESOLVE_FAQ`-phase scope, ONLY on `record_outcome(class=resolve)`, ONLY checking citation presence. It is NOT a generic citation gate (which `D-hard-citation-gate` correctly bars); it is a narrow Skill-bounded `guardrails.must_cite_source` declaration per the Sprint 5 (F2) author's original framing. `D-hard-citation-gate`'s rationale (no generic Java citation gate across all FAQ paths, no citation gate that would enforce content judgement) is preserved; the M2 inversion is the principled Skill-bounded exception.

   **Human authorization (2026-05-17, at OLD M2-Skill approval round; carried forward into NEW M2):** "Accept the Skill-bounded exception to D-hard-citation-gate. This is not a generic Java grounding-citation gate. It is a narrow S1 terminal predicate that only applies when the bot attempts `record_outcome(class=resolve)` inside the `RESOLVE_FAQ` scope, and only checks citation presence as the minimum grounding-floor condition. The original deferral of a generic Java citation gate remains valid."

   This authorization governs the predicate scope. Sprint 39 implementation MUST NOT expand the predicate beyond: (a) Phase = RESOLVE_FAQ; (b) Tool call = `record_outcome` with `class=resolve`; (c) Check = `source_id` citation present in user-facing message. Any expansion (e.g., enforcing citation on `class=escalate/abandon`, enforcing content-quality of the citation, fanning out to other phases) is OUT OF SCOPE and requires a new objective + human review. Codex Sprint 39 review must verify the Skill `guardrails.must_cite_source` declaration scope matches this authorization.

5. **No `RuntimeIntentClassifier.java` / `IntentClassification.java` / `DriftResult.java` / `DriftDetector.java` / `UseCaseRouter.java` / `ClassifyUseCaseTool.java` touch.** M3-D Topic↔UC binding loosening deferred to M3+.
6. **No edits to `INTAKE_UCS` set at `PhaseEvaluator.java:30`.** M3-B Single Handover Orchestrator deferred to M3 unless cutover pressure.
7. **No `escalation_reason` enum widening at `PhaseEvaluator.java:39-63`.** `D-new-escalation-reason-enum` deferral preserved. M3-A sibling rationale/confidence fields deferred to M3.
8. **No Tier-0 invariant added without Sprint 37 design-freeze pre-authorization + explicit human-review escalation.** Sprint 37 may surface that S1's citation predicate OR Skill tool-whitelist enforcement OR Skill terminal-predicate refusal IS a candidate Tier-0 invariant for `docs/runtime_freeze_and_risk_policy.md` §1/§2. If so, STOP per §10 condition #1 and dispatch human review BEFORE Sprint 38 begins.
9. **No edits to existing case families** under `eval_interactive/case_specs/case_families/<existing>/` (cascade fence carried from M1).
10. **No edits to existing shadow CaseSpecs** under `eval_interactive/case_specs_shadow/case_families/<existing>/`.
11. **No edits to `eval_interactive/eval_interactive/`** (harness, loader, simulator).
12. **No `iteration_governance.md` / `doc_governance.md` / `agent_context_guide.md` / `runtime_freeze_and_risk_policy.md` edit during M2** (M1 close confirmed the constitution-discipline review pattern; M2 inherits). Sprint 37 design freeze produces a proposal doc at `docs/proposals/skill_registry_design.md`; iteration_governance refinement is a separate fold-back sprint per `doc_governance.md` fold-back cadence. **EXCEPTION**: if Sprint 37 surfaces a Tier-0 candidate that is authorized for addition at M2 close, `runtime_freeze_and_risk_policy.md` MAY be edited as a SEPARATE deliver-agent commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`.
13. **No widening of `eval_interactive/case_spec_overrides.yaml`.**
14. **No edits to sprint archives** under `docs/sprints/sprint-001-*` through `docs/sprints/sprint-036-*`.
15. **No edits to `docs/foundational/`.**
16. **No edits to `docs/milestones/M1_objective.md`** (M1 archive is immutable per `doc_governance.md`). **No edits to `docs/milestones/M2-Skill_objective.md`** post-archival (OLD M2-Skill is also immutable once archived).
17. **No mocked-LLM as primary evidence** for any LLM-behaviour claim per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md`. The Skill envelope teaching effect + UC-switch continuity evidence comes from real-LLM reruns; Skill data-loading + Skill terminal-predicate Java logic tests may mock LLM (those are deterministic Java logic tests).
18. **No Skill that prescribes LLM customer-facing language.** Skill `procedure` teaching is principle-level + tool envelope + recommended order; customer-facing language is LLM-owned per §1.3.
19. **No Skill that hard-encodes per-step argument values.** Skill `procedure` names tools + recommended order; per-step arguments are LLM-owned.
20. **No deletion of `docs/proposals/skill_foundation_design.md`** (OLD Sprint 36 freeze); supersession pattern only per doc_governance.md.
21. **No reset / migration of M1-shipped FUNCTIONAL surfaces** (`IntakeFieldExtractor.java` UC-G/H/I/J/K extension; `Sprint71PartialIntakePersistenceTest` 14/14 guard; Sprint 32 `alternate_candidate_use_cases` projection slot in `ContextProjectionBuilder`; Sprint 33 `discover_disambiguation_signals` projection slot in `ContextProjectionBuilder`) beyond the EXPLICIT retroactive migration scope in §3 sub-sprint sequence. M1 FUNCTIONAL behaviour is preserved through M2; only the HOME of teaching/predicate content moves. Functional behaviour MAY shift on individual cases (Alice etc.) per §5 recalibration; the Skill abstraction is the goal, not the specific behaviour fix.
22. **No deletion or relocation of `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml`** at M2 — it stays as an observation case (not a gate) per §5 recalibration. The closure_criterion field may be refined per §5.6 lifecycle at any sub-sprint close if observed behaviour shape changes; refinement is sharpening, NOT widening per Constitution §1.7.

## 7. R-items consumed / surfaced

**Consumed by M2 (expected):**

- `R-grounding-discipline-iterative-search-fabrication` (opened at M1 close 2026-05-17; M2 Sprint 39 closes by ADDING the S1 `must_cite_source` Skill `guardrails` — note: closure does NOT depend on observed Alice closure-criterion (a) PASS per §5 recalibration; closure depends on the predicate being declared in the Skill `guardrails` block AND functionally enforced by the dispatcher).
- Sprint 5 (F2) `docs/proposals/skill_orchestration_candidates.md` (status `proposal` / `not_started` → promoted to code via Sprint 37 freeze + Sprint 38 Registry + Sprint 39 RESOLVE Skills migration + Sprint 40 teaching extraction + Sprint 41 state preservation; original proposal stays in tree as reasoning archive with chained `superseded_by` pointer through `skill_foundation_design.md` → `skill_registry_design.md`).
- Sprint 5 S1 + S2 candidates (`not_started` → `implemented` via Sprint 39 within Skill abstraction).
- UC-switching scenarios surfaced by human review 2026-05-17 (Sprint 41 implementation within Skill abstraction).
- OLD M2-Skill Sprint 36 design freeze (status: in-flight at supersession; carried forward as superseded reasoning; replaced by Sprint 37 NEW freeze).
- All OLD M2-Skill Sprint 37 (S1 implementation per OLD framing) contract pending state (live `docs/sprint_objective.md` at 2026-05-17) → wholesale replaced by NEW M2 Sprint 37 design freeze contract; S1 work itself NOT abandoned, just re-homed inside Skill abstraction at Sprint 39.

**Surfaced by M2 (expected):**

- Sprint 37 freeze MAY surface a Tier-0 candidate for `docs/runtime_freeze_and_risk_policy.md` §1/§2 (Skill tool-whitelist enforcement; Skill terminal-predicate refusal non-overridability; S1 citation predicate semantics). If surfaced, new R-item naming the candidate for human-review escalation per §10.
- Sprint 38 SkillRegistry core implementation may surface migration-pattern R-items (behavioural equivalence test reusable fixture; SkillLoader schema validation pattern; SkillRegistry fallback semantics edge cases). New R-items if so.
- Sprint 39 RESOLVE migration + S1/S2 + dispatcher may surface predicate-composition refinements; new R-items if so.
- Sprint 40 teaching extraction may surface orchestration-shell content shape questions (what stays in system_prompt.txt vs Skill YAML borderline); new R-items if so.
- Sprint 41 UC-switching state preservation may surface additional state-inheritance invariants worth case-family coverage; new R-item for M3 case family if so OR new bad case authored per §5.6.
- Possible new R-item at M2 close: M3-C Sprint 5 S3/S4/S5 skills now unblocked (validated abstraction).
- Possible new R-item: M3-Skill-tuning sub-sprint to tune individual Skill `procedure` / `guardrails` content for specific case-behaviour improvements (e.g., Alice closure-criterion (a) PASS if not achieved organically). Per §5 recalibration this is M3 work, NOT M2.

**NOT consumed by M2 (deferred):**

- Original M2-customer-honesty Sprint 37/38/39 (sibling rationale/confidence fields, handover human UX, URL policy) → M3-A.
- `D-single-handover-orchestrator` → M3-B unless cutover pressure.
- `D-full-issue-ledger` → hard-deferred per `docs/action_bank.md`.
- `R-loosen-topic-uc-binding-llm-owned-drift` → M3-D unchanged.
- `D-new-escalation-reason-enum` → deferral preserved.
- `D-hard-citation-gate` → REINTERPRETED with M2 Skill predicate bounded inversion (per §6 hard fence #4); the generic-Java-citation-gate deferral itself is preserved.
- `D-advert-link-product-decision` → M3-A with Sprint 23 URL extension.
- `R-llm-provider-latency-drift-2026-05-16` → next milestone (orthogonal infra parallel-track; preserved in `docs/action_bank.md` deferred).
- Sprint 5 S3 / S4 / S5 candidates → M3-C on the validated M2 Skill abstraction.
- Sprint 33 OQ1-5, Sprint 34 OQ1-5, Sprint 35 OQ1-5, Sprint 36 OQs — all M3 / docs fold-back candidates.

## 8. Codex review plan (per §4.3)

**Default for M2: PER-SUB-SPRINT Codex review for ALL semantic-touching sub-sprints** (Sprints 37, 38, 39, 40, 41). This DIFFERS from §4.3 default (milestone-shared) and from the OLD M2-Skill plan (which also defaulted per-sub-sprint for similar reasons) because every M2 semantic-touching sub-sprint hits a §4.3 trigger:

- **Sprint 37** (design freeze): §4.3 trigger #1 (possible new Tier-0 candidate from Skill predicate / tool-whitelist enforcement semantics) + §4.3 trigger #2 (§1.7 boundary discussion on Skill abstraction shape — Codex verifies Skill `procedure` + `guardrails` design does NOT introduce per-UC-branch if-else as a hidden form).
- **Sprint 38** (SkillRegistry core + 4 simpler phase Skills migration): §4.3 trigger #3 (new architectural surface — SkillRegistry mediates between LLM prompt context and tool dispatch; Codex verifies behavioural equivalence pre/post-migration on 4 phases).
- **Sprint 39** (RESOLVE_FAQ + RESOLVE_INTAKE Skill migration + Sprint 6/7/11 predicate migration + S1/S2 new predicates + unified dispatcher): §4.3 trigger #1 (Tier-0 candidate territory if Sprint 37 freeze surfaced one) + §4.3 trigger #3 (touches Runtime-owned grounding floor + capability floor + unified dispatcher = multi-fence convergence; Codex verifies the Skill `guardrails.must_cite_source` declaration scope matches the verbatim §6 #4 human authorization).
- **Sprint 40** (teaching extraction + orchestration shell cleanup): §4.3 trigger #3 (system_prompt.txt structural change — LLM input contract surface; Codex verifies extracted teaching reaches LLM via Skill envelope projection equivalently to prior monolithic system_prompt.txt).
- **Sprint 41** (UC switch + state preservation): §4.3 trigger #3 (session state model touch across Skill boundary — sensitive surface; verifies per-Skill `state_inheritance` declarations do NOT introduce per-UC-pair if-else as a hidden form per §6 #1).

**Per-sub-sprint Codex review prompt language for M2 (deliver-agent drafts)**: Each `compact/sprint-NNN-review-prompt.md` will explicitly state that bad-case suite results + interactive eval composite_score are observation-only for THIS milestone per §5 recalibration; Codex anti-hardcode kernel verdict + Java test regression check + functional architecture review + Skill `guardrails` declared-scope verification (per §6 #4 verbatim authorization) are the gating outputs.

**Milestone-shared cumulative Codex review at M2 close**: a single architectural-posture check across all 5 sub-sprints to confirm the cumulative shape preserves Constitution + hard fences + bad-case suite (as observation). This is IN ADDITION to per-sub-sprint reviews, not a replacement. Specifically Codex re-walks: (a) §4.1 nine-question kernel on the cumulative diff; (b) M1 functional surfaces preservation (IntakeFieldExtractor + Sprint71PartialIntakePersistenceTest + Sprint 32/33 projections); (c) Skill abstraction self-consistency (no per-UC-pair branch in any Skill body across all 5 sub-sprints; all 6 phase Skills present; AgentRunLoopImpl has ZERO `shouldRejectXxx` Java methods); (d) UC-switching continuity behavioural evidence on representative traces (qualitative, not metric).

**Codex review prompts**: deliver-agent drafts at `compact/sprint-NNN-review-prompt.md` per sub-sprint (Sprint 37 / 38 / 39 / 40 / 41) + `compact/M2-review-prompt.md` for milestone-close cumulative check.

## 9. Estimated milestone duration

Informational, not a gate. Estimated 3-4 weeks for the 5 sub-sprints + per-sub-sprint Codex review rounds + deliver-agent + human planning rounds + milestone close. M1 actual was less than 2 days for 3 sub-sprints (calibration baseline per M1 §12.6); NEW M2 has bigger steps per human direction (5 sub-sprints; 6 phase Skill migrations; predicate migration + S1/S2; teaching extraction; UC-switch state). Per-sub-sprint Codex review on 5 of 5 + functional review at M2 close. The 3-4 week estimate accounts for the design-freeze sprint running ahead of any implementation work + 4 implementation/migration sprints with behavioural equivalence verification at each + the cumulative architectural-posture Codex check at M2 close.

## 10. Stop conditions (milestone-level)

STOP and re-plan the milestone when:

1. **Sprint 37 design freeze surfaces a Tier-0 invariant candidate** (Skill tool-whitelist enforcement, Skill terminal-predicate refusal, S1 citation predicate, OR any other Skill predicate qualifies as Tier-0 territory per `runtime_freeze_and_risk_policy.md` §1/§2). STOP. Dispatch explicit human review per §4.3 trigger #1 BEFORE Sprint 38 begins; do NOT invent a new Tier-0 invariant without human escalation per Constitution §3.2 question 2.
2. **Sprint 37 design freeze surfaces a §1.7 forbidden-list violation** that cannot be cleanly resolved within Skill abstraction (e.g., the only way to encode Skill `procedure` for a multi-UC phase is per-UC-branch if-else). STOP. Deliver-agent + human re-frame; possibly split scope or push to M3.
3. **Sprint 38 SkillRegistry core implementation surfaces behavioural regression** on any of the 4 migrated phase Skills that cannot be cleanly resolved within abstraction (e.g., the YAML schema cannot express what the Java string was conveying without information loss). STOP. Return to Sprint 37 freeze to refine.
4. **Sprint 39 RESOLVE migration + S1/S2 + dispatcher** surfaces an enforcement gap (e.g., the Skill `guardrails.must_cite_source` predicate cannot be safely encoded without per-UC-branch logic; or the predicate over-fires on legitimate non-citation outputs such as `record_outcome(class=escalate)`; or the unified dispatcher creates a regression on Sprint 6/7/11 migrated logic). STOP. Return to Sprint 37 freeze to refine.
5. **Sprint 40 teaching extraction** surfaces a teaching paragraph whose Skill home is ambiguous OR whose extraction causes behavioural regression (e.g., the LLM no longer observes the teaching in expected form because Skill envelope projection differs from monolithic system_prompt.txt). STOP. Return to Sprint 37 freeze (f) mapping decision OR refine Skill projection mechanism.
6. **Sprint 41 UC-switching wide continuity** surfaces an invariant conflict (e.g., preserving citation history across UC switch conflicts with grounding floor on the new UC; or preserving intake fields across UC switch conflicts with capability scope of the new UC). STOP. Sprint 37 freeze invariant matrix may need refinement; OR scope down Sprint 41 + push wide continuity to M3.
7. **Any sub-sprint surfaces a §1.7 forbidden-list violation in committed code** (immediate STOP; deliver-agent + human address before continuing).
8. **Salesforce cutover calendar pressure surfaces mid-M2** — deliver-agent + human evaluate whether to pause M2 and re-prioritize M3-B Single Handover Orchestrator immediately.
9. **Per-sub-sprint Codex review verdict comes back `fix_required` with `blocking_count` ≥ 2** on any of Sprints 37 / 38 / 39 / 40 / 41. STOP. Fix-iteration sub-sprint per existing convention; do NOT proceed to next sub-sprint until cleared.
10. **Behavioural equivalence test fails on Sprint 38 / 39 / 40 migration** (the pre-migration phase behaviour is NOT observationally preserved post-migration; OR the Sprint 71 UC-K regression guard fails). STOP. Surface to deliver-agent + human; either Skill schema needs refinement OR migration mapping was wrong. NOTE: behavioural equivalence here means "Java-test-detectable behavioural preservation"; LLM-observed behaviour (interactive eval, Alice case) MAY shift per §5 recalibration and does NOT trigger this stop.
11. **Sprint 41 surfaces a UC-switch scenario where the Skill `state_inheritance` declaration semantics are ambiguous** (the same scenario could be modeled multiple ways). STOP. Return to Sprint 37 freeze.
12. **Java test suite regression on existing tests** (post-M1 baseline 983 NOT preserved; or Sprint71PartialIntakePersistenceTest fails). STOP immediately. Diagnose before continuing.

## 11. Cross-milestone sequencing context

M2 closes the Skill Registry abstraction + wholesale retroactive externalization + UC switching state preservation. M3 candidates per 2026-05-17 deliver-agent + human governance round (carried forward from M1 close + OLD M2-Skill + NEW M2 recalibration):

- **M3-A**: Customer-honesty surface deferred from M2-customer-honesty draft + OLD M2-Skill — sibling rationale/confidence fields on `request_handover` (Sprint 21 carry-forward), handover human-message UX rewrite, URL policy + factual narrowing extension (Sprint 23 carry-forward, including `D-advert-link-product-decision` resolution). Rides on M2 Skill abstraction (sibling fields declared in `Resolve.FAQ` / `Resolve.Intake` Skill `guardrails`; UX rewrite as cross-Skill orchestration shell content).
- **M3-B**: Lifecycle (`D-single-handover-orchestrator` P0 launch blocker + scoped `D-full-issue-ledger` with explicit new-objective approval if granted + semantic planner shadow mode if scoped). Rides on M2 Skill abstraction (single-handover-orchestrator may be modeled as a new `Escalate.Handover` Skill).
- **M3-C**: Sprint 5 S3 / S4 / S5 skills (Soft-OOS triage / Account login-recovery / Tier-2-reason UC-compat) directly on the validated M2 Skill abstraction (new Skill YAML files; minimal infra change).
- **M3-D**: Topic↔UC binding loosening (`R-loosen-topic-uc-binding-llm-owned-drift` constraint-removal; requires research-agent investigation round before scoping).
- **M3-Latency** (or M3-Infra): consume `R-llm-provider-latency-drift-2026-05-16` deferred from M2. Likely milestone-of-one per §8.5.
- **M3-Skill-Tuning**: post-M2 behavioural tuning of individual Skill `procedure` / `guardrails` content to improve specific case behaviour (e.g., Alice closure-criterion (a) PASS if not achieved organically through Skill abstraction landing; UC-switch specific scenarios). Localized Skill edits on validated abstraction. Possibly a milestone-of-many-sub-sprints or rolling into M3-A.
- **M3-other**: any R-item M2 surfaces (esp. Tier-0 candidates from Sprint 37 freeze; UC-switching case family; behavioural equivalence test pattern as a reusable test fixture).

M3 selection happens at M2 close based on (a) which M2 sub-sprints fired with what evidence, (b) which R-items M2 surfaced, (c) Salesforce cutover calendar pressure (which would flip M3-B to highest priority), (d) bad-case suite state (Alice observation results: closer to PASS suggests M3-Skill-Tuning lower priority; further from PASS suggests it higher), (e) human priorities, (f) whether Sprint 37 surfaced a Tier-0 candidate that needs immediate human-review milestone. The deliver-agent does NOT pre-decide M3 here.

---

**Cross-references:**

- Sprint 36 (OLD M2-Skill design freeze) commit `ed71031`; archive at `docs/sprints/sprint-036-objective.md` + `docs/sprints/sprint-036-handoff.md` + `docs/sprints/sprint-036-codex-review.md`.
- Sprint 36 freeze design doc `docs/proposals/skill_foundation_design.md` — to be superseded in-place at M2 approval bundle (status: superseded + superseded_by: docs/proposals/skill_registry_design.md + new "Why superseded 2026-05-17" section).
- OLD M2-Skill milestone_objective archive at `docs/milestones/M2-Skill_objective.md` (to be created at M2 approval bundle).
- M1 archive at `docs/milestones/M1_objective.md` (immutable).
- Constitution chain: `AGENTS.md` → `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` + `docs/current/iteration_governance.md` (§1 Constitution + §3 Fix Layer Checklist + §4 Anti-Hardcode + §5 Eval Acceptance + §5.6 bad-case-suite (recalibrated for THIS milestone to observation per §5) + §7 Stanza + §8 Milestone framework).
- Deliver-agent role: `compact/sprint-deliver-orchestrator.md`.
- Bad case suite: `eval_interactive/case_specs/bad_cases/` + `_manifest.md` (Alice case = M2 observation, NOT gate).
- Sprint 5 (F2) original Skill orchestration proposal: `docs/proposals/skill_orchestration_candidates.md` (`status: proposal`, `not_started` pre-M2; promoted to code via M2 with chained `superseded_by` through Sprint 36 freeze → Sprint 37 freeze).
