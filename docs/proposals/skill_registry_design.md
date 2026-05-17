---
title: Skill Registry Design — first-class externalized Skill abstraction (Sprint 37 NEW M2 sub-sprint 1 design freeze)
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-17
review_cadence: per milestone
supersedes:
  - docs/proposals/skill_foundation_design.md
  - docs/proposals/skill_orchestration_candidates.md
superseded_by: null
notes: >
  Sprint 37 (NEW M2 sub-sprint 1) design freeze output. Locks ten
  design decisions (a)-(j) that constraint-bind the four downstream
  M2 implementation sub-sprints (Sprints 38 SkillRegistry core + 4
  simpler phases; 39 RESOLVE_FAQ + RESOLVE_INTAKE + Sprint 6/7/11
  predicate migration + S1/S2 new predicates + unified dispatcher;
  40 Sprint 23/31/33 teaching extraction + orchestration shell
  cleanup; 41 UC switch + state preservation across Skill boundary).

  This freeze supersedes the OLD M2-Skill Sprint 36 freeze at
  `docs/proposals/skill_foundation_design.md` (which locked
  "minimum-surface incrementalism" — reuse existing PhasePlan + add
  teaching paragraph in `system_prompt.txt` + add adjacent Java
  predicate — and was wholesale rejected by the human 2026-05-17 in
  favor of a first-class Skill Registry abstraction with retroactive
  externalization). It chained-supersedes the Sprint 5 (F2)
  `docs/proposals/skill_orchestration_candidates.md` proposal whose
  "Implement S1 as a parametrized PhasePlan inside
  PhaseEvaluator.plan(...) — do NOT introduce a new skill runtime
  framework" constraint NEW M2 deliberately inverts (the human's
  reading 2026-05-17 is that an externalized Skill abstraction IS
  the natural extension of PhasePlan, not a "new framework").

  Sprint 37 walks the §4.1 nine-question anti-hardcode kernel
  against the proposed design and surfaces any Tier-0 invariant
  candidate for human-review escalation per Sprint 37 §10 stop
  condition #2. Sprint 37 ships NO `server/` code; downstream
  sub-sprint contracts are drafted from this freeze AFTER human
  review + Codex per-sub-sprint review verdict `approve` at Sprint
  37 close.
---

# Skill Registry Design — first-class externalized Skill abstraction

## 1. Background + relation to upstream

### 1.1 What this freeze decides

This document is the load-bearing architectural decision doc for NEW
Milestone M2 (Skill Registry Abstraction + Wholesale Retroactive
Externalization, per `docs/milestone_objective.md`). It locks ten
design decisions (a)-(j) covering:

- (a) **Skill data model** — what a Skill is as an externalized
  YAML/JSON file (fields, schema validation contract).
- (b) **Skill Registry shape** — the Java class that loads + indexes
  Skill files at boot; `select(phase, useCase) → Skill` semantics;
  fallback on miss.
- (c) **PhaseEvaluator-as-Skill-Selector integration** — how
  `PhaseEvaluator.plan(...)` queries the SkillRegistry and composes
  the selected Skill with session state into a `PhasePlan` for the
  per-turn LLM invocation.
- (d) **`procedure` vs `guardrails` responsibility split** — what
  content goes into LLM-soft prompt teaching (`procedure`) vs
  Runtime-floor hard enforcement (`guardrails`) vs the existing
  hard tool whitelist (`PhasePlan.allowedTools`).
- (e) **Retroactive migration mapping for ALL 6 phase YAMLs** — per
  phase (DISCOVER + CONFIRM + RESOLVE_FAQ + RESOLVE_INTAKE +
  ESCALATE + TERMINAL), where current hardcoded `systemInstruction`
  / `groundingInstruction` / `escalationPolicy` Java strings land in
  Skill fields.
- (f) **Retroactive migration mapping for Sprint 23/31/33 teaching
  paragraphs** in `system_prompt.txt` — which paragraph goes into
  which Skill's `procedure` / `guardrails`, vs what stays in the
  orchestration shell.
- (g) **Retroactive migration mapping for Sprint 6/7/11 predicates**
  in `AgentRunLoopImpl.java` — which Skill's `guardrails` hosts
  each existing `shouldRejectXxx` method.
- (h) **Unified Skill terminal-predicate dispatcher design** — class
  placement, composition order on a candidate tool call / outcome
  emission, failure-mode trace + LLM-visible message shape.
- (i) **Session-level state model + per-Skill `state_inheritance`
  semantics** — what survives UC switch + how each Skill declares
  inherit / reset / soft-signal posture; SkillStateBus class
  responsibility.
- (j) **§4.1 nine-question anti-hardcode kernel walk** on the
  proposed design — per-question verdict + reasoning; surfacing
  any Tier-0 invariant candidate.

The freeze is tight enough that Sprint 38 / 39 / 40 / 41 contracts
can be drafted directly from this doc without further architectural
rounds, while leaving Sprint 38+ implementation specifics (exact
Java method signatures, exact YAML field syntax for borderline
fields) to dev judgement at implementation time.

### 1.2 Relation to OLD Sprint 36 freeze (`skill_foundation_design.md`)

The OLD freeze locked **minimum-surface incrementalism**: reuse the
existing `PhasePlan` record verbatim; add NEW teaching paragraphs
to `system_prompt.txt` as siblings to Sprint 23 `already_called` /
Sprint 31 `alternate_candidate_use_cases` / Sprint 33
`discover_disambiguation_signals` paragraphs; add NEW Java
predicates to `AgentRunLoopImpl` as siblings to Sprint 6
`shouldRejectFaqMissHandover` / Sprint 7
`shouldRejectIncompleteIntakeHandover` / Sprint 11
`shouldRejectPrematureResolveOutcome` methods. No new class, no
externalized definitions, no extraction of existing content.

The human rejected the framing 2026-05-17 because, despite the name
"Skill Foundation", it preserved the Zhang-Sanfeng pattern the
architectural goal was meant to fix — Sprint 23/31/33/would-be-S1
teaching paragraphs would keep accumulating side-by-side in
`system_prompt.txt`; Sprint 6/7/11/would-be-S1 predicates would
keep accumulating side-by-side in `AgentRunLoopImpl`; there was no
extracted abstraction. NEW M2 promotes Skill to first-class:
externalized YAML/JSON definitions, Skill Registry as the source of
truth for phase content, `system_prompt.txt` reduced to a thin
orchestration shell, `AgentRunLoopImpl` predicate methods replaced
by a unified dispatcher that reads Skill-declared `guardrails`.

This freeze preserves load-bearing content from the OLD freeze:

- The **verbatim human authorization** at OLD M2-Skill §6 #4 on the
  S1 `must_cite_source` bounded inversion of `D-hard-citation-gate`
  carries forward into NEW M2 §6 #4 verbatim, and into Sprint 37
  decision (g) on which Skill `guardrails` block hosts the S1
  predicate (`resolve_faq_grounded_answer.yaml`).
- The **D5 UC-switching continuity invariant matrix** (rows ×
  behaviour codes S/C/N/R/L) informs decision (i) `state_inheritance`
  declaration shape: principle-level invariants, registry-level
  intersection check (not per-UC-pair branch tables per §1.7).
- The **§4.1 walk methodology** (Q1-Q9 verbatim from
  `iteration_governance.md` §4.1; verdict per question with cited
  evidence) is repeated here in §11 against the NEW proposed design.
- The **MATERIAL FINDING** that Sprint 6 / 7 / 11 predicates are
  already shipped (per OLD Sprint 36 §1.2 + Sprint 36 handoff §1.3)
  is the precedent for decision (g) retroactive migration mapping
  (the predicates aren't introduced — they're re-homed inside the
  Skill abstraction).

This freeze rejects load-bearing positions from the OLD freeze:

- **D1 (envelope = PhasePlan data + system_prompt.txt teaching
  paragraph)** — REJECTED. NEW M2 decision (a) introduces a Skill
  data class (`Skill.java`) carrying the envelope as first-class
  fields (`procedure`, `guardrails`, `state_inheritance`).
- **D4 (envelope teaching as NEW system_prompt.txt paragraph)** —
  REJECTED. NEW M2 decision (f) migrates Sprint 23/31/33 paragraphs
  INTO Skill YAML; `system_prompt.txt` shrinks to orchestration
  shell.
- **D6 (S1 citation predicate ships as adjacent Java method in
  AgentRunLoopImpl)** — REJECTED. NEW M2 decision (g) ships S1 as a
  Skill `guardrails.must_cite_source` declaration enforced by the
  unified dispatcher (decision (h)).
- **D2 (predicate code lives as static method on AgentRunLoopImpl)**
  — PARTIALLY REJECTED. The Java code still lives at the same
  dispatch-time point on the tool dispatch path; what's NEW is that
  the code reads its scope + condition from the Skill `guardrails`
  block (Skill-declared) rather than carrying scope + condition
  inline as Java constants.

### 1.3 Relation to Sprint 5 (F2) original proposal (`skill_orchestration_candidates.md`)

The Sprint 5 (F2) proposal introduced five candidate skills (S1
Resolve.FAQ.GroundedAnswer / S2 Resolve.Intake.CollectAndHandover /
S3 Triage.SoftOOS.ClarifyOrEscalate / S4 Triage.Account.LoginRecovery /
S5 Triage.PolicySensitive.Tier2Reasoning) and locked the design
constraint: *"Do NOT introduce a new skill runtime framework.
Implement S1 as a parametrized PhasePlan inside
PhaseEvaluator.plan(...)."* (F2 §6.)

NEW M2 takes a different reading of that constraint: an externalized
Skill abstraction is NOT a "new skill runtime framework" in the
sense F2 §6 meant to exclude. F2 §6's concern was a *new runtime
engine* (a separate orchestration loop that the LLM interacts with
differently from the existing PhasePlan / AgentRunLoop tool-dispatch
flow). NEW M2's Skill Registry is the OPPOSITE — it externalizes
the *data* that `PhaseEvaluator` already produces (per-phase
`PhasePlan.systemInstruction` / `groundingInstruction` /
`escalationPolicy` strings) into a versionable, parseable form. The
runtime loop is unchanged; only the source of the phase content
shifts from hardcoded Java strings to externalized Skill YAMLs.

NEW M2 ships S1 + S2 inside the Skill abstraction at Sprint 39
(re-homing the S1 citation predicate from the OLD M2-Skill Sprint
37 plan into `resolve_faq_grounded_answer.yaml` `guardrails`,
re-homing the S2 intake-completeness check — already shipped as
`shouldRejectIncompleteIntakeHandover` — into
`resolve_intake_collect_and_handover.yaml` `guardrails`). S3 / S4 /
S5 stay deferred to M3-C; the Sprint 5 (F2) proposal body remains
in tree as the upstream architectural reasoning archive.

### 1.4 Relation to the M2 milestone objective + acceptance recalibration

This freeze ships against `docs/milestone_objective.md` (NEW M2,
2026-05-17). Key milestone-level constraints this freeze honors:

- **§1 sub-sprint sequence:** five sub-sprints (S37 design freeze →
  S38 Registry core + 4 phases → S39 RESOLVE_FAQ + RESOLVE_INTAKE +
  predicate migration + S1/S2 + dispatcher → S40 teaching
  extraction + shell cleanup → S41 UC switch + state preservation).
  This freeze produces the architectural binding the four downstream
  sub-sprints implement.
- **§5 acceptance recalibration:** bad-case suite (Alice) +
  interactive eval composite_score are **observation, not gate**
  for THIS milestone (architecture-focused, not behaviour-fix-
  focused). Sprint 37 itself ships zero behaviour change; downstream
  sub-sprints honor the recalibration by gating on functional review
  + Java test no-regression + freeze decisions-honored, NOT on
  bad-case PASS.
- **§6 hard fences:** no per-UC-branch if-else in any Skill body
  (YAML or Java) per §1.7; Skill terminal predicate may be a Java
  enforcement ONLY if it protects Runtime-owned floor per §1.4;
  Skill `procedure` is soft teaching not hard enforcement; the §6
  #4 verbatim authorization on S1 `must_cite_source` bounded
  inversion of `D-hard-citation-gate` carries forward into decision
  (g); no touch to `RuntimeIntentClassifier` / `IntentClassification`
  / `DriftResult` / `DriftDetector` / `UseCaseRouter` /
  `ClassifyUseCaseTool` (M3-D deferred); no `INTAKE_UCS` edit at
  `PhaseEvaluator.java:30` (M3-B deferred); no `escalation_reason`
  enum widening (`D-new-escalation-reason-enum` deferred to M3-A);
  no Tier-0 invariant added without Sprint 37 pre-authorization +
  human escalation; no edits to existing case families / shadow
  CaseSpecs / `eval_interactive/eval_interactive/` / governance docs.

### 1.5 The research-agent proposal (load-bearing architectural input)

The human's M2 architectural intent was anchored by a research-agent
investigation prior to M2 approval 2026-05-17. The research-agent's
framing introduced three load-bearing distinctions reproduced here
(quoted / paraphrased verbatim where load-bearing):

**Three-tier abstraction:**

- **Tools** = atomic capabilities. Already present in
  `server/src/main/java/com/gumtree/csagent/service/tools/` (e.g.,
  `SearchKnowledgeTool`, `ResolveArticleTool`, `RecordOutcomeTool`,
  `RequestHandoverTool`, `ClassifyUseCaseTool`,
  `GetCustomerContextTool`). No change in M2.
- **Skills** = configurable LLM-driven workflow descriptions. NEW
  in M2. Each Skill names a tool whitelist (`tools_required`), a
  recommended LLM-soft flow (`procedure`), and Runtime-floor
  enforcement points (`guardrails`).
- **Workflows** = deterministic code-orchestrated multi-step
  sequences. NOT what M2 is doing. (The pre-Sprint-5 / legacy
  `resolveFaq` path was workflow-style; M2 explicitly chooses
  Skill-as-Prompt over Workflow per Constitution §1.5 / §1.7.)

**Four orchestration patterns** (research-agent enumeration; M2
adopts #3 Skill-as-Prompt):

1. *Pure ReAct* — LLM autonomous, no flow guidance. Too unpredictable
   for production CS; rejected.
2. *Plan-then-Execute* — LLM generates a plan, then executes. Good
   for complex multi-step problem-solving; not the M2 fit (M2
   bounds the flow at the Skill envelope level, not at the plan
   level).
3. **Skill-as-Prompt** *(the M2 fit)* — Phase + scenario → inject the
   matching Skill Prompt → LLM autonomously drives tool calls
   within the Skill's tool whitelist guided by `procedure` text +
   bounded by `guardrails`. Flexible but bounded.
4. *Hardcoded Workflow* — Java/Python deterministic orchestration.
   Legacy pattern; M2 moves AWAY from this.

**Why Skill-as-Prompt for CS** (research-agent's rationale,
load-bearing for decision (d)):

- Customer-service problems by type have *standard handling flows*
  — the FAQ-grounded-resolve flow, the intake-collect-and-handover
  flow, the soft-OOS clarify flow. These flows are stable across
  customers within a UC and somewhat stable across UCs within a
  phase. Skill captures this stability without freezing it.
- LLM instruction-following is *good enough* (Claude 4 series) to
  follow Skill `procedure` reliably while exercising judgment
  within the envelope (deviating when a specific customer
  context warrants, e.g., a customer-context answer for an
  account-specific question instead of a generic FAQ search).
- Skill content can be *independently iterated* (YAML edit + reload,
  no Java recompile required for Skill-content tuning).
- Deterministic boundaries are preserved as hard fallback: tool
  whitelist via `PhasePlan.allowedTools` (composed from Skill
  `tools_required`); safety guards via ControlKernel; terminal
  predicates via Skill `guardrails` enforced by the unified
  dispatcher.

**Three-level branch-logic handling** (research-agent's framing;
load-bearing for decision (d) / decision (j) §1.7 walk):

- *Level 1 — LLM-soft branch* in Skill `procedure` text. The LLM
  reads "first try search_knowledge; if a viable hit, then
  resolve_article; if no viable hit, then request_handover with
  faq_miss_threshold_exceeded" and judges per turn. Principle-
  level, NOT per-UC-pair if-else per §1.7.
- *Level 2 — Deterministic hard branch* in PhaseEvaluator /
  ControlKernel. Safety-critical decisions (Tier-0 invariants per
  `runtime_freeze_and_risk_policy.md`); not LLM-owned.
- *Level 3 — Hybrid* (this architecture is already here pre-M2):
  PhasePlan constrains tool whitelist (hard), Skill `procedure`
  guides flow (soft), Skill `guardrails` enforce Runtime-floor
  invariants (hard, narrow). M2 makes the hybrid *explicit* by
  separating `procedure` (soft) from `guardrails` (hard) at the
  Skill data-model level.

**Key research-agent insight** (load-bearing; quoted at human's
request 2026-05-17):

> "Your architecture only needs the last step — extract the flow
> description strings scattered in PhaseEvaluator into independent,
> versionable Skill definition files. This change is a natural
> extension within your current PhasePlan framework; it does not
> require core refactor."

NEW M2 takes this insight as the architectural premise. It extends
the research-agent proposal by ALSO migrating Sprint 23/31/33
teaching paragraphs from `system_prompt.txt` (decision (f)) AND
Sprint 6/7/11 predicates from `AgentRunLoopImpl.java` (decision (g))
into the same Skill abstraction. The research-agent proposal
addressed only PhaseEvaluator's hardcoded strings; the human's
observation 2026-05-17 was that the same scattering pattern existed
across two more surfaces, and the M2 migration should cover all
three.

### 1.6 Architectural diagram (post-M2 target state)

```
┌─────────────────────────────────────────────────────────────┐
│                       ControlKernel                          │
│        (safety floor, budget, phase transitions,             │
│         pre-plan reroute, ResolveDisposition guard)          │
├─────────────────────────────────────────────────────────────┤
│                      PhaseEvaluator                          │
│            (Skill Selector: skillRegistry.select(            │
│              phase, useCase) → Skill; compose Skill +        │
│              session state into PhasePlan)                   │
├─────────────────────────────────────────────────────────────┤
│                    SkillRegistry  ← NEW (M2 Sprint 38)       │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ skill: resolve_faq_grounded_answer                  │   │
│   │   tools_required: [get_customer_context,            │   │
│   │                    search_knowledge,                │   │
│   │                    resolve_article,                 │   │
│   │                    record_outcome,                  │   │
│   │                    request_handover]                │   │
│   │   procedure: |                                      │   │
│   │     The intended terminal sequence is               │   │
│   │     search_knowledge → resolve_article →            │   │
│   │     grounded customer-facing answer (with a         │   │
│   │     source_id citation) → record_outcome. Only      │   │
│   │     escalate via request_handover after a valid     │   │
│   │     resolve attempt cannot complete...              │   │
│   │   guardrails:                                       │   │
│   │     - must_cite_source                              │   │
│   │       (refuse record_outcome(class=resolve)         │   │
│   │        when user-facing message lacks source_id)    │   │
│   │     - faq_miss_handover_requires_resolve_attempt    │   │
│   │       (Sprint 6 §G2 migrated)                       │   │
│   │     - premature_resolve_outcome_guard               │   │
│   │       (Sprint 11 §M1 migrated)                      │   │
│   │   state_inheritance:                                │   │
│   │     inherit: [customer_context,                     │   │
│   │                accumulated_tool_results]            │   │
│   │     reset: []                                       │   │
│   │     soft_signal_via_projection:                     │   │
│   │       [prior_use_case_carry]                        │   │
│   └─────────────────────────────────────────────────────┘   │
│   (six Skills total: discover_triage / confirm /            │
│    resolve_faq_grounded_answer /                            │
│    resolve_intake_collect_and_handover / escalate /         │
│    terminal)                                                 │
├─────────────────────────────────────────────────────────────┤
│                     AgentRunLoopImpl                         │
│   (LLM ↔ Tool loop; ToolDispatcher.validateAgainstPlan       │
│    enforces tool whitelist; SkillGuardrailDispatcher         │
│    enforces guardrails read from active Skill)               │
├─────────────────────────────────────────────────────────────┤
│              SkillGuardrailDispatcher  ← NEW (M2 Sprint 39)  │
│   (replaces scattered shouldRejectXxx methods; reads         │
│    active Skill's guardrails block; composes Sprint 6/7/11   │
│    migrated predicates + NEW S1 / S2)                        │
├─────────────────────────────────────────────────────────────┤
│                  SkillStateBus  ← NEW (M2 Sprint 41)         │
│   (mediates session-level state across Skill boundary;       │
│    applies per-Skill state_inheritance declaration on        │
│    UC switch; surfaces prior_use_case_carry projection slot) │
├─────────────────────────────────────────────────────────────┤
│                ToolDispatcher + Tools                        │
│            (atomic capability layer, unchanged)              │
└─────────────────────────────────────────────────────────────┘

orchestration shell: server/src/main/resources/prompts/system_prompt.txt
   — thin orchestration teaching about Skill envelopes after M2
     Sprint 40 cleanup; phase-specific teaching content lives in
     Skill YAML.
```

### 1.7 What this freeze does NOT decide

Per Sprint 37 contract §6 fences + §3 non-goals, this freeze does
NOT:

- Pre-decide Sprint 38 / 39 / 40 / 41 implementation specifics
  beyond what (a)-(j) decides. Exact Java method signatures, exact
  YAML field syntax for borderline cases, exact test-class names
  are dev judgement at implementation time.
- Decide the closure of Sprint 5 F2 S3 / S4 / S5 candidates (deferred
  to M3-C; ride on validated M2 Skill abstraction).
- Decide UC switch detection semantics beyond the existing M1
  Sprint 32 `alternate_candidate_use_cases` projection + M1 Sprint
  33 `discover_disambiguation_signals` projection that Sprint 41
  rides on (no new classifier per M2 §6 #5 fence).
- Edit `docs/runtime_freeze_and_risk_policy.md` § Tier-0 catalogue.
  If §11 (decision (j)) surfaces a qualified Tier-0 candidate, the
  candidate is enumerated in §12 with a recommendation (qualified /
  rejected / deferred); the actual `runtime_freeze_and_risk_policy.md`
  edit (if approved) is a SEPARATE deliver-agent commit at Sprint
  37 close per `feedback_commit_at_end_bundles_deliver_artefacts.md`,
  NOT included in this dev commit.
- Decide how `system_prompt.txt` lines 45-52 (DISCOVER phase
  guidance) and lines 54-101 (`request_handover` escalation_reason
  decision tree) split between orchestration shell vs Skill
  procedure content. Decision (f) covers Sprint 23/31/33 paragraphs
  per Sprint 37 §2 (f) literal scope; the orchestration-shell
  content beyond those three paragraphs is a Sprint 40 dev decision
  guided by decision (d)'s principle ("cross-Skill teaching about
  envelope mechanics → shell; phase-specific behaviour → Skill").
  See premise refinement in §13.

## 2. Decision (a) — Skill data model

### 2.1 Decision statement

A **Skill** is an externalized YAML/JSON file under
`server/src/main/resources/skills/`. Each Skill file deserializes
into a `Skill` Java record/class (`server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java`)
carrying these fields:

| Field | Type | Required | Description |
|-------|------|---------:|-------------|
| `name` | string | yes | Canonical Skill identifier (snake_case; e.g., `resolve_faq_grounded_answer`). Matches the file name minus `.yaml` extension. Source of truth for `SkillRegistry.select(...)` key composition. |
| `description` | string | yes | One-line human-readable purpose. NOT surfaced to LLM (internal documentation). |
| `applicable_phases` | list of strings | yes | Subset of phase enum: `DISCOVER`, `CONFIRM`, `RESOLVE_FAQ`, `RESOLVE_INTAKE`, `ESCALATE`, `TERMINAL`. Most Skills name exactly one phase; the `applicable_phases` list shape allows future M3+ Skills to span phases without schema change. |
| `applicable_use_cases` | list of strings OR sentinel `["*"]` | yes | UC subset OR `["*"]` for "any UC in `applicable_phases`". E.g., `resolve_faq_grounded_answer.applicable_use_cases = [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]` (FAQ-path UCs); `discover_triage.applicable_use_cases = ["*"]` (any UC in DISCOVER). The list is registry-scoping data — describes WHEN this Skill applies — NOT an LLM-readable decision branch (§1.7 boundary check in §2.4 below). |
| `tools_required` | list of canonical tool names | yes | Tool whitelist for this Skill. The `PhasePlan.allowedTools` is composed from this. Canonical tool names match `server/src/main/java/com/gumtree/csagent/service/tools/` class names (e.g., `search_knowledge`, `resolve_article`, `record_outcome`, `request_handover`, `get_customer_context`, `classify_use_case`). Empty list `[]` is valid (e.g., CONFIRM Skill may have no tool calls). |
| `procedure` | multi-line string (markdown / plain text) | yes | LLM-soft prompt teaching. Names the recommended order at the principle level (e.g., "first call search_knowledge; if a viable hit, then resolve_article; cite source_id in the user-facing message; then record_outcome"). Surfaced to the LLM as the `PhasePlan.systemInstruction` (composed with orchestration shell teaching). **MUST NOT contain per-UC-branch if-else** per Constitution §1.7. |
| `grounding_instruction` | multi-line string | optional | LLM-soft grounding-floor teaching. Maps to `PhasePlan.groundingInstruction`. Some Skills carry it (e.g., RESOLVE_FAQ's "MUST call search_knowledge first if accumulated_tool_results.search_knowledge is empty"); others omit (e.g., CONFIRM). |
| `escalation_policy` | multi-line string | optional | LLM-soft escalation guidance. Maps to `PhasePlan.escalationPolicy`. Names which escalation_reason is appropriate when AND when escalation is allowed at all. |
| `guardrails` | list of guardrail declarations | optional (default `[]`) | Runtime-floor Java enforcement points. Each guardrail is a structured declaration (NOT free-form text) parseable into a dispatch-time check. See decision (d) §5 + decision (g) §8 + decision (h) §9 for the schema. |
| `state_inheritance` | object | optional (default `{inherit: [], reset: [], soft_signal_via_projection: []}`) | Per-Skill state-bus posture for UC switch. Names what session state the Skill `inherit`s, `reset`s, or sees as `soft_signal_via_projection` from the prior Skill. See decision (i) §10 for schema + semantics. |
| `objective` | string | optional | Short statement of what the Skill is trying to accomplish from the user's perspective. Maps to `PhasePlan.objective`. |
| `max_tool_steps` | integer | optional (default uses existing per-phase default) | Per-Skill tool-step budget. Maps to `PhasePlan.maxToolSteps`. Most Skills use the existing per-phase default (DISCOVER=2, CONFIRM=2, RESOLVE_FAQ=4, RESOLVE_INTAKE=3, ESCALATE=2, TERMINAL=2 per HEAD `PhaseEvaluator.java`). |
| `allow_interim_message` | boolean | optional (default false) | Maps to `PhasePlan.allowInterimMessage`. |
| `valid_terminal_outcomes` | list of `TerminalOutcome` enum values | optional | Maps to `PhasePlan.validTerminalOutcomes`. |
| `required_context_keys` | list of strings | optional | Maps to `PhasePlan.requiredContextKeys`. |

### 2.2 Schema validation contract

The Skill data model carries a JSON Schema (or equivalent
declarative validation contract) used by `SkillLoader` at Spring
bootstrap to fail-fast on:

- Required fields missing (`name`, `description`, `applicable_phases`,
  `applicable_use_cases`, `tools_required`, `procedure`).
- `applicable_phases` values not in the phase enum.
- `applicable_use_cases` values not in the UC registry (with `["*"]`
  sentinel allowed).
- `tools_required` values not in the canonical tool name set.
- `guardrails[]` entries whose `type` does not match a known
  dispatcher predicate type (see decision (h) §9).
- `state_inheritance` keys not in the allowed key set (see decision
  (i) §10).
- `valid_terminal_outcomes` values not in the `TerminalOutcome`
  enum.

Sprint 38 implements the schema validation contract. The exact JSON
Schema (or equivalent) is dev judgement at Sprint 38; the freeze
locks the validation responsibility surface, not the syntax.

### 2.3 Rationale

- **Externalized YAML/JSON over inline Java strings.** The four
  motivations: (i) tunability — a Skill content tweak is a YAML
  edit + Spring reload, not a Java recompile; (ii) versioning — a
  Skill file's git history records iteration on Skill content
  separately from Java logic iteration; (iii) Codex / human
  reviewability — a Skill YAML is denser and more readable than a
  multi-line Java `+ "..."` string concatenation; (iv) testability
  — a Skill file can be loaded into a unit test fixture, mutated,
  and assertions made about composed PhasePlan output.
- **Required-fields set chosen for migration completeness.** Each
  required field maps to an existing `PhasePlan` field or to a
  load-bearing M2 introduction (`guardrails`, `state_inheritance`).
  No required field is speculative; all six current PhaseEvaluator
  branches (DISCOVER / CONFIRM / RESOLVE_FAQ / RESOLVE_INTAKE /
  ESCALATE / TERMINAL — note CLOSE is consolidated with TERMINAL in
  decision (e) §6) can be represented losslessly. See decision (e)
  §6 migration mapping per phase.
- **Optional-fields set chosen for backward compatibility.** Fields
  like `max_tool_steps`, `allow_interim_message`,
  `valid_terminal_outcomes`, `required_context_keys` correspond to
  existing PhasePlan fields that already have per-phase defaults;
  Skills inherit the default unless explicitly overridden.
- **`applicable_use_cases` shape (list OR `["*"]`).** Most Skills
  scope to a UC-path subset (FAQ-path or INTAKE-path). A handful
  (DISCOVER, CONFIRM, ESCALATE, TERMINAL) apply across UCs; the
  `["*"]` sentinel signals "any UC for the phases this Skill
  applies to" without requiring the Skill to enumerate all UCs.
  This is a registry-scoping declaration, NOT a per-UC-branch
  semantic decision (§1.7 boundary check in §2.4 below).

### 2.4 Alternatives considered + why rejected

**Alternative A: keep PhaseEvaluator strings inline as Java
constants (OLD Sprint 36 D1).** Rejected per §1.2: this preserves
the Zhang-Sanfeng scattering pattern; iteration on Skill content
requires Java recompile; testability and reviewability suffer.

**Alternative B: Skill as a separate runtime engine with its own
orchestration loop.** Rejected per §1.3 (Sprint 5 F2 §6 explicit
constraint, carried forward as load-bearing): a new runtime engine
would force the LLM to interact with the orchestration in a
different shape, regress the existing tool-dispatch tests, and
re-architect AgentRunLoopImpl. NEW M2 keeps AgentRunLoopImpl as the
single tool-dispatch loop and externalizes only the *data*
PhaseEvaluator produces.

**Alternative C: monolithic Skill data class with all fields
inlined as required (no optional fields).** Rejected: forces every
Skill to enumerate fields that don't apply to its phase (e.g.,
DISCOVER Skill would have to spell out `grounding_instruction:
null`). Adds boilerplate without information.

**Alternative D: per-Skill DSL for `guardrails` and
`state_inheritance` as free-form text strings parsed at dispatch
time.** Rejected: a free-form DSL makes Codex review harder (no
schema; ambiguous interpretation) and forces the dispatcher to
re-parse on every tool call. Structured declarations (typed object
shapes) are parseable at Skill load time, validated by schema, and
unambiguous at dispatch.

**Alternative E: per-Skill versioning + version-pinning in Skill
Registry.** Rejected for M2 (no version field in v1 Skill data
model): the existing `git history` of the Skill file IS the
version record; M2 scope does NOT include multiple concurrently-
loadable versions of the same Skill. Add `version` field in M3+ if
need surfaces (e.g., A/B testing two Skill `procedure` shapes).

### 2.5 §1.7 boundary check

The Skill data model itself does NOT introduce per-UC-branch if-else.
Discussion per concerning field:

- **`applicable_use_cases` as a list of UCs** — this is CAPABILITY
  SCOPE (when is this Skill in scope?), NOT a semantic decision
  (what should the LLM do per UC?). The Skill that lists `[UC-A,
  UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]` declares "I apply to these
  FAQ-path UCs"; it does NOT say "if UC=UC-A do X, if UC=UC-B do
  Y". Within the Skill body, the LLM sees ONE `procedure` text
  applicable to all UCs the Skill scopes to. Per §1.7 walk on
  PROPOSED design in §11 (decision (j)) Q1: this is registry-
  scoping, not per-UC-branch.
- **`applicable_phases` as a list of phases** — same reasoning;
  this is phase-scoping, structural to the phase machine (Sprint
  10 / 11 / 11.1 frozen surface).
- **`tools_required` as a list per Skill** — same reasoning; this
  is capability scope per Skill, hard-enforced by
  `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`.
  Not a semantic per-UC decision.

§1.7 boundary preserved: PER-UC-PAIR enums (e.g., a Skill that
encodes `if old_uc==UC-A and new_uc==UC-C: do X`) are forbidden in
ANY Skill field. Decision (i) §10 enforces this for
`state_inheritance` semantics; decision (h) §9 enforces this for
`guardrails` semantics; decision (g) §8 enforces this for migrated
predicates.

### 2.6 §1.3 / §1.4 boundary check

- **§1.3 (LLM-owned semantic decisions) preserved.** Skill
  `procedure` is LLM-soft teaching; LLM owns response strategy,
  customer-facing wording, drift / topic-shift detection,
  escalation posture choice (within the Skill's `escalation_policy`
  envelope), next-action choice among `tools_required`. None of
  these is moved into Java enforcement.
- **§1.4 (Runtime-owned floor) preserved.** Skill `guardrails`
  enforce Runtime-floor invariants only (grounding-floor,
  capability/permission, PII/safety, idempotency); they DO NOT
  enforce LLM-owned next-action / response-strategy / UC-hypothesis.
  Decision (d) §5 + decision (g) §8 enforce this for each migrated
  / new predicate. The hard tool whitelist (`tools_required` →
  `PhasePlan.allowedTools`) preserves the existing
  `ToolDispatcher.validateAgainstPlan` capability-floor invariant.

### 2.7 Downstream sub-sprint reference

Decision (a) is implemented in **Sprint 38** (SkillRegistry core).
Sprint 38 ships:

- `server/src/main/java/com/gumtree/csagent/service/runtime/skill/Skill.java`
  (Skill data record/class per the §2.1 field set).
- JSON Schema (or equivalent) validation contract per §2.2.
- 4 Skill YAMLs (the 4 simpler phases per decision (e) §6 migration
  mapping: `skills/discover_triage.yaml`, `skills/confirm.yaml`,
  `skills/escalate.yaml`, `skills/terminal.yaml`).
- Skill class unit tests + JSON Schema validation tests.

Sprint 39 extends the data model:

- 2 more Skill YAMLs (`skills/resolve_faq_grounded_answer.yaml`,
  `skills/resolve_intake_collect_and_handover.yaml`).
- Concrete `guardrails` declarations populating each Skill (per
  decision (g) §8 + decision (h) §9).

Sprint 41 extends the data model:

- `state_inheritance` field populated on all 6 Skill YAMLs (per
  decision (i) §10).

## 3. Decision (b) — Skill Registry shape

### 3.1 Decision statement

A new Java class `SkillRegistry` at
`server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillRegistry.java`
holds the indexed map of all loaded Skills and exposes a
`select(phase, useCase) → Skill` method. A new Java class
`SkillLoader` at
`server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java`
loads + schema-validates YAML/JSON files from
`server/src/main/resources/skills/` at Spring bootstrap.

Public surface (signature shape; exact syntax is Sprint 38 dev
judgement):

```java
@Component
public class SkillRegistry {
    private final Map<SkillKey, Skill> indexedSkills;  // composed at boot

    // Primary selection method.
    public Skill select(String phase, String activeUseCase);

    // Diagnostic / introspection method (used in tests + admin tools).
    public Collection<Skill> allSkills();

    // Diagnostic — returns the Skills applicable for the phase
    // regardless of UC, ordered by registration (DISCOVER returns
    // [discover_triage]; RESOLVE returns
    // [resolve_faq_grounded_answer, resolve_intake_collect_and_handover];
    // etc.).
    public List<Skill> skillsForPhase(String phase);
}
```

The `SkillKey` is an internal composition record (e.g., `record
SkillKey(String phase, String useCase)`). Two Skills MAY share a
phase but MUST NOT collide on `(phase, useCase)` after expansion
(`["*"]` sentinel expanded to the registered UC set per phase).
Schema validation at boot fails-fast on collision.

### 3.2 Selection semantics

`select(phase, useCase)` resolves in this order:

1. **Exact match.** Find a Skill where `applicable_phases.contains(phase)`
   AND `applicable_use_cases.contains(useCase)` (literal match).
2. **Wildcard UC match.** Find a Skill where
   `applicable_phases.contains(phase)` AND `applicable_use_cases ==
   ["*"]`. (E.g., DISCOVER + UC-A → `discover_triage` which lists
   `applicable_use_cases: ["*"]`.)
3. **Fallback to legacy path.** If neither (1) nor (2) returns a
   Skill, `select(...)` returns `null` (Optional.empty()
   semantically), and `PhaseEvaluator.plan(...)` falls back to its
   existing legacy Java-string-based branch (the M2 migration is
   incremental — Sprint 38 migrates 4 phases, Sprint 39 migrates 2
   more; during the Sprint 38-→-Sprint 39 window some
   `(phase, useCase)` tuples have a Skill and some don't).

The fallback semantics on miss (option (3)) is *return null + log
warning + fall to legacy path*. NOT *throw exception* (which would
break the existing tool-dispatch tests during the migration
window). NOT *return a default-Skill* (which would obscure the
miss and force the dev to wonder which Skill was actually
selected). The null-return semantics is the cleanest fit for
incremental migration; once Sprint 39 closes, no `(phase, useCase)`
tuple should miss, and Codex Sprint 39 review verifies the no-miss
invariant.

Post-Sprint-39 (when all 6 phases are migrated), the fallback
remains as DEFENSIVE — a runtime guard against an unforeseen phase
or UC reaching `PhaseEvaluator.plan(...)`. It is NOT removed; the
log-warning surfaces the unexpected case in trace + ops dashboard.

### 3.3 Loader semantics

`SkillLoader` is invoked at Spring bootstrap from `SkillRegistry`'s
`@PostConstruct`:

1. Enumerate files under `server/src/main/resources/skills/*.yaml`
   (and `.yml` if needed; consistency is dev judgement at Sprint 38).
2. Deserialize each file to the `Skill` record via Jackson YAML.
3. Validate each Skill against the JSON Schema per decision (a)
   §2.2.
4. Validate the composed registry against collision invariants
   (§3.1 above).
5. On any validation failure, throw at boot — Spring fails to start.
   This is fail-fast posture; a malformed Skill file should not
   silently degrade.

`SkillLoader` reads YAML files as a one-shot at boot; M2 does NOT
implement Skill hot-reload. Sprint 39+ Skill content changes
require a Spring reload (or a new JVM); the tunability claim
(decision (a) §2.3 rationale (i)) is "YAML edit + Spring reload",
not "live hot-reload".

### 3.4 Rationale

- **Per-Skill files (not a monolithic Skills.yaml).** Smaller diffs
  in git history; easier Codex review (per-file scope); per-Skill
  ownership clearer.
- **`select(phase, useCase)` returns null on miss (Optional).**
  Incremental migration safety — Sprint 38 migrates 4 phases
  without breaking the 2 not-yet-migrated phases.
- **Fail-fast at boot.** Configuration errors should surface at
  Spring start, not at the first matching tool dispatch (which
  could be hours later in production).
- **`SkillKey` as internal record.** Future M3+ Skills may use more
  selection dimensions (e.g., user segment, A/B variant); the
  internal record shape allows extending without changing the
  public `select(...)` signature.
- **`SkillRegistry` is a Spring `@Component`** (not a static).
  Allows injection into PhaseEvaluator + tests + admin tools;
  parallels existing `IntakeFieldsRegistry` and
  `UseCaseRegistryService` patterns.

### 3.5 Alternatives considered + why rejected

**Alternative A: throw exception on miss.** Rejected per §3.2:
breaks incremental migration; turns Sprint 38 close into a
mandatory full-migration commit (i.e., would force Sprint 38 to
ALSO migrate RESOLVE_FAQ + RESOLVE_INTAKE, exceeding the
sub-sprint contract). null-return semantics is the cleaner fit.

**Alternative B: return a hardcoded default Skill on miss.**
Rejected: obscures the miss; the dev wondering "which Skill is
applied for (RESOLVE, UC-A) when I haven't migrated yet?" sees the
default Skill, not the legacy path. null-return + warning makes
the fallback explicit.

**Alternative C: monolithic `skills.yaml` with all Skills inlined.**
Rejected per §3.4: harder Codex review; harder per-Skill ownership;
larger diffs per Skill edit.

**Alternative D: hot-reload at runtime.** Rejected: scope creep
for M2 (tunability claim is "YAML + Spring reload", not "live
mid-session reload"); changes to a Skill mid-session would cause
cross-Skill state inconsistencies in active sessions.
Reload-without-restart is a M3+ candidate if operational need
surfaces.

**Alternative E: per-Skill versioning + concurrent multi-version
loading (Skill A v1 + Skill A v2 both loaded).** Rejected for M2
(no `version` field per decision (a) §2.4 Alternative E): M2 scope
is the abstraction landing + first migration; versioning is M3+ if
need surfaces (e.g., A/B testing on prod traffic).

### 3.6 §1.7 boundary check

`SkillRegistry` + `SkillLoader` are infrastructure — they hold
+ index data. No semantic decisions are encoded in this surface.
`select(...)` is a pure lookup; the only "branching" is the exact-
match-then-wildcard fallback ordering, which is structural (NOT
per-UC-pair semantic). §1.7 not implicated.

### 3.7 §1.3 / §1.4 boundary check

§1.3 (LLM-owned) not implicated; `SkillRegistry` doesn't touch any
LLM decision. §1.4 (Runtime-owned): `SkillRegistry` is part of the
Runtime's "tool schema + capability boundary + persistence" surface
per §1.4. The registry holds the source-of-truth for which
capabilities (`tools_required`) and which guardrails apply per
(phase, useCase). Per §1.4, the runtime owns this responsibility;
the implementation surface is appropriate.

### 3.8 Downstream sub-sprint reference

Decision (b) is implemented in **Sprint 38** (SkillRegistry core).
Sprint 38 ships:

- `SkillRegistry.java` per §3.1 public surface.
- `SkillLoader.java` per §3.3 semantics.
- The `server/src/main/resources/skills/` directory (created from
  empty per Sprint 37 premise check #9).
- Integration with Spring `@PostConstruct` for boot-time loading.
- Unit tests: `SkillRegistryTest` (selection semantics including
  exact match, wildcard match, null on miss);
  `SkillLoaderTest` (YAML deserialization, schema validation
  fail-fast).
- Spring integration test verifying boot-time failure on malformed
  Skill YAML.

Sprint 39+ adds Skills to the registry without changing the
SkillRegistry / SkillLoader implementation; the data model + loader
+ select semantics are stable post-Sprint-38.

## 4. Decision (c) — PhaseEvaluator-as-Skill-Selector integration

### 4.1 Decision statement

`PhaseEvaluator.plan(...)` (at `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:387-666`
verified HEAD 6d97888) becomes a **Skill Selector** for migrated
phases. It queries `skillRegistry.select(phase, activeUc)` and
composes the returned Skill with session state into a `PhasePlan`.
For phases not yet migrated (Sprint 38 → Sprint 39 window), the
existing hardcoded Java-string branches stay in place as the
legacy fallback path; `PhaseEvaluator` returns the legacy
`PhasePlan` for those.

The exact integration shape (per Sprint 38 dev judgement; freeze
locks the surface):

```java
public PhasePlan plan(BotSession session, String userMessage,
                     List<BotTurn> history) {
    if (session == null) return null;
    String phase = session.getCurrentPhase();
    String activeUc = session.getActiveUseCase();

    // M2 NEW path: query SkillRegistry first.
    Skill skill = skillRegistry.select(phase, activeUc);
    if (skill != null) {
        return composeSkillPhasePlan(skill, session, userMessage, history);
    }

    // Legacy fallback: existing branches (DISCOVER / CONFIRM /
    // CLOSE / ESCALATE / RESOLVE-INTAKE / RESOLVE-FAQ) per the
    // current code at lines 397-665.
    if ("DISCOVER".equals(phase)) {
        return /* existing DISCOVER branch */;
    }
    // ... other legacy branches ...
}

private PhasePlan composeSkillPhasePlan(Skill skill,
                                       BotSession session,
                                       String userMessage,
                                       List<BotTurn> history) {
    // Compose Skill fields with session-dynamic state into PhasePlan.
    return PhasePlan.builder()
        .phase(session.getCurrentPhase())
        .useCase(session.getActiveUseCase())
        .objective(resolveObjective(skill, session))   // may
                                                       // parametrize
                                                       // by UC name
        .allowedTools(skill.getToolsRequired())
        .requiredContextKeys(skill.getRequiredContextKeys())
        .maxToolSteps(skill.getMaxToolSteps())
        .allowInterimMessage(skill.isAllowInterimMessage())
        .validTerminalOutcomes(skill.getValidTerminalOutcomes())
        .systemInstruction(composeSystemInstruction(skill, session))
        .groundingInstruction(skill.getGroundingInstruction())
        .escalationPolicy(skill.getEscalationPolicy())
        .build();
}
```

`composeSystemInstruction(skill, session)` combines the Skill
`procedure` with any session-dynamic parametrization (e.g.,
substituting the UC display name into a `{uc_name}` placeholder in
the Skill `procedure`, OR appending a per-UC fragment from the
`UC_TEAM_NAME` map for INTAKE Skills). The exact parametrization
mechanism is Sprint 38 / Sprint 39 dev judgement; the principle is
"Skill `procedure` is the principle-level teaching; session state
fills in placeholders for entity names". NO per-UC if-else logic
is allowed in this composition — only template substitution and
UC-display-name lookup.

### 4.2 Backward-compatible PhasePlan data shape

`PhasePlan.java` (record at
`server/src/main/java/com/gumtree/csagent/model/PhasePlan.java`,
146 lines, 11 fields per HEAD 6d97888) is **NOT changed in M2**.
The Skill abstraction reuses the existing 11 fields verbatim;
Skill is the NEW source for these fields' values, not a new field
on PhasePlan.

This preserves:

- All existing PhasePlan consumers (`AgentRunLoopImpl`,
  `ToolDispatcher`, projection slot composition,
  PhaseEvaluator's post-loop interpreter). No consumer signature
  changes.
- All existing PhasePlan tests. The 983-baseline Java suite passes
  because every consumer sees a structurally identical PhasePlan.
- The Sprint 11 / 11.1 / 12 frozen runtime surface per
  `docs/runtime_freeze_and_risk_policy.md` §1.1. The Skill
  abstraction is an UPSTREAM change (where the PhasePlan data
  *comes from*); the frozen surface is the DOWNSTREAM contract
  (what the PhasePlan does at the tool-dispatch loop).

### 4.3 Intermediate state during migration

Between Sprint 38 close and Sprint 39 close, the system has
**mixed** state: 4 phases (DISCOVER, CONFIRM, ESCALATE, TERMINAL)
on Skill; 2 phases (RESOLVE_FAQ, RESOLVE_INTAKE) on legacy
hardcoded Java-string branches. The mixed state is safe because:

- `SkillRegistry.select(...)` returns null for unmapped tuples →
  legacy branch fires per §4.1.
- Both paths produce structurally identical PhasePlans (same 11
  fields), so downstream consumers don't observe the difference.
- Behavioural equivalence is the Sprint 38 dev gate: for each of
  the 4 migrated phases, dev writes a Java test asserting that the
  Skill-composed PhasePlan and the legacy-branch PhasePlan are
  observationally equal for representative UCs in that phase.

Post-Sprint-39 close: all 6 phases are on Skill; the legacy
branches are removed from `PhaseEvaluator.plan(...)`. Sprint 39 dev
verifies the legacy-branch removal is safe by re-running the
behavioural-equivalence tests + the full 983-baseline.

### 4.4 Rationale

- **Skill takes priority over legacy in `plan(...)`.** Simple
  routing logic; one `if (skill != null)` check; preserves the
  legacy path as fall-through.
- **`composeSkillPhasePlan` is a private helper.** Encapsulates
  the Skill-to-PhasePlan composition; testable in isolation;
  Sprint 38 dev iterates without touching the public surface.
- **Session-dynamic parametrization through placeholder
  substitution.** Avoids per-UC Java if-else in
  `composeSystemInstruction`; the Skill `procedure` carries the
  principle-level teaching, with placeholders like `{uc_name}` or
  `{team_name}` filled at compose time from registry lookups
  (`UseCaseRegistryService.getUseCase(activeUc).name()`,
  `PhaseEvaluator.UC_TEAM_NAME.get(activeUc)`). No semantic
  branching.
- **PhasePlan data shape preserved.** Honors the Sprint 11 / 11.1 /
  12 frozen surface per Constitution §1.4 + `runtime_freeze_and_risk_policy.md`
  §1.1. The Skill abstraction is purely upstream of the frozen
  surface.

### 4.5 Alternatives considered + why rejected

**Alternative A: add a new `Skill skill` field on PhasePlan and
have downstream consumers read the Skill directly.** Rejected: a
new PhasePlan field touches the frozen surface; downstream
consumers (`AgentRunLoopImpl`, `ToolDispatcher`) would need to be
aware of Skills. The Skill abstraction stays UPSTREAM of
PhasePlan; downstream sees only the existing PhasePlan fields.

**Alternative B: replace `PhaseEvaluator.plan(...)` entirely with
a thin wrapper that just returns `SkillRegistry.select(...).toPhasePlan(...)`.** Rejected per §4.3: forces atomic full-migration in
Sprint 38 (no legacy fallback during the window); exceeds
sub-sprint scope.

**Alternative C: PhasePlan composition lives inside the Skill
class itself (`skill.toPhasePlan(session)`).** Rejected: violates
separation of concerns. The Skill class is a data record (per
decision (a)); composition with session state is PhaseEvaluator's
responsibility (the existing PhaseEvaluator is the per-phase
PhasePlan builder; M2 just changes the *source* of phase data,
not the *builder* of PhasePlan).

**Alternative D: keep PhaseEvaluator's legacy branches forever +
load Skills as a parallel test channel.** Rejected: defeats the
M2 migration goal. Sprint 39 close removes the legacy branches.

### 4.6 §1.7 boundary check

- The integration introduces ONE new `if (skill != null)` check at
  the top of `PhaseEvaluator.plan(...)`. This is STRUCTURAL routing
  (Skill-found-or-not), NOT per-UC-branch semantics. §1.7 not
  implicated.
- `composeSkillPhasePlan` may apply template substitution
  (`{uc_name}` → registry lookup result). This is REGISTRY-DRIVEN
  parametrization, NOT per-UC-pair branch logic. §1.7 not
  implicated.
- Legacy fallback branches (the existing DISCOVER + CONFIRM +
  CLOSE + ESCALATE + RESOLVE-INTAKE + RESOLVE-FAQ branches at
  `PhaseEvaluator.java:397-665`) are preserved verbatim during the
  Sprint 38 → Sprint 39 window. Sprint 39 close REMOVES them. No
  new per-UC if-else introduced; the migration only EXTRACTS
  existing content into Skills.

### 4.7 §1.3 / §1.4 boundary check

- **§1.3 (LLM-owned semantic decisions) preserved.** The integration
  is structural / infra; no LLM decision is moved into Java.
- **§1.4 (Runtime-owned floor) preserved + extended.** The
  PhasePlan composition is part of "Runtime owns tool schema +
  capability boundary" per §1.4; M2 strengthens this by making the
  source of per-phase tool whitelist + system instruction
  declarative (Skill YAML) rather than hardcoded Java strings.

### 4.8 Downstream sub-sprint reference

Decision (c) is implemented in **Sprint 38** (4 simpler phases
migrated) + **Sprint 39** (RESOLVE_FAQ + RESOLVE_INTAKE migrated +
legacy branches removed). Sprint 38 ships:

- `PhaseEvaluator.plan(...)` modification: prepend
  `skillRegistry.select(...)` query; on non-null Skill, call
  `composeSkillPhasePlan` and return; on null, fall through to
  existing legacy branches.
- `composeSkillPhasePlan` private helper per §4.1.
- Behavioural-equivalence tests for DISCOVER + CONFIRM + ESCALATE
  + TERMINAL phases.

Sprint 39 extends:

- `composeSkillPhasePlan` handles RESOLVE_FAQ + RESOLVE_INTAKE
  Skills (with their guardrails populated per decision (g)).
- Legacy branches at `PhaseEvaluator.java:397-665` are REMOVED;
  `PhaseEvaluator.plan(...)` post-Sprint-39 is much shorter (the
  Skill query + composition path is the only path).
- Final behavioural-equivalence test pass on all 6 phases.

## 5. Decision (d) — `procedure` vs `guardrails` responsibility split

### 5.1 Decision statement

The Skill data model separates LLM-soft prompt teaching from
Runtime-floor hard enforcement at the field level:

- **`procedure`** (multi-line string) is **LLM-soft**. The recommended
  flow at the principle level. Surfaced to the LLM through
  `PhasePlan.systemInstruction` (composed with orchestration shell
  teaching from `system_prompt.txt`). The LLM reads, judges, and
  may deviate when its semantic decision warrants (per §1.3 next-
  action / response strategy ownership). The Java runtime DOES NOT
  enforce `procedure` step-by-step; it surfaces and reads.
- **`grounding_instruction`** (multi-line string) — also **LLM-soft**.
  Same shape as `procedure`; separate field because it maps to the
  existing `PhasePlan.groundingInstruction` (a separate LLM input
  field per HEAD `PhaseEvaluator.java:642-653` for RESOLVE_FAQ).
- **`escalation_policy`** (multi-line string) — also **LLM-soft**.
  Maps to `PhasePlan.escalationPolicy`.
- **`tools_required`** (list of tool names) is **Runtime-hard**.
  Composed into `PhasePlan.allowedTools` and enforced by the
  existing `ToolDispatcher.validateAgainstPlan` check
  (capability-floor invariant — the LLM cannot physically dispatch
  a tool name not in this list).
- **`guardrails`** (list of structured guardrail declarations) is
  **Runtime-hard**. Each guardrail is a declarative typed object
  parseable by the unified `SkillGuardrailDispatcher` (decision (h)
  §9). Examples: `must_cite_source`, `intake_complete_required`,
  `faq_miss_handover_requires_resolve_attempt`,
  `premature_resolve_outcome_guard`. Each guardrail fires at a
  specific dispatch site (e.g., `record_outcome` tool call,
  `request_handover` tool call) and either REJECTS the call or
  DOWNGRADES the canonical reason per its declared `on_fail` mode.

### 5.2 Guardrail declaration schema (declarative DSL)

Each entry in `guardrails[]` is a typed object:

```yaml
guardrails:
  - type: must_cite_source
    on_fail: reject_with_hint
    parameters:
      outcome_class: resolve
      cite_token_field: source_id

  - type: faq_miss_handover_requires_resolve_attempt
    on_fail: reject_with_hint
    parameters: {}
      # No tunable params; Sprint 6 §G2 semantics fully fixed.

  - type: premature_resolve_outcome_guard
    on_fail: reject_with_hint
    parameters:
      # Sprint 11 §M1 + Sprint 11.1 — the existing
      # ResolveDispositionEvaluator delegation. No tunable params.

  - type: intake_complete_required
    on_fail: reject_with_hint
    parameters:
      escalation_reason_pattern: "intake_complete_for_uc_*"
      required_fields_source: IntakeFieldsRegistry
```

Schema rules:

- `type` (required) — names a guardrail predicate type registered
  in `SkillGuardrailDispatcher`. Sprint 39 adds the four
  types listed above + the two NEW S1/S2 types. Schema validation
  at Skill load (decision (a) §2.2) verifies the type is known.
- `on_fail` (required) — one of `reject_with_hint`,
  `downgrade_reason`. (NO `observe_only`; observations belong in
  projection / trace, not in guardrails — decision (d) follows OLD
  Sprint 36 D2 §3.2 verbatim on this exclusion.)
- `parameters` (optional, default `{}`) — type-specific tunable
  fields. Empty for predicates whose semantics are fully fixed by
  Java implementation (e.g., `faq_miss_handover_requires_resolve_attempt`
  is Sprint 6 §G2 verbatim, no tunable params); populated for
  predicates with declared scope (e.g., `must_cite_source` names
  the `outcome_class` and `cite_token_field` so the predicate is
  reusable in M3+ for non-`record_outcome` outcomes if need
  surfaces).

The declarative schema is chosen over a free-form string DSL
because: (i) schema validation at boot catches typos; (ii) Codex
review can reason about each guardrail's declared parameters; (iii)
the dispatcher can iterate over typed objects without runtime
parsing.

### 5.3 What goes where (content placement rules)

| Content shape | Goes into | Why |
|---|---|---|
| Principle-level recommended order ("first call search_knowledge, then resolve_article, then cite source_id, then record_outcome") | `procedure` | LLM-soft teaching; LLM owns deviation per §1.3. |
| Grounding-floor instruction ("MUST call search_knowledge first if accumulated_tool_results.search_knowledge is empty") | `grounding_instruction` | LLM-soft teaching about grounding; LLM reads + complies; Runtime backstop in `guardrails` (e.g., `faq_miss_handover_requires_resolve_attempt`). |
| Escalation-decision teaching ("escalate via request_handover if (a) user explicitly requests a human, (b) search returned no viable hit, (c) issue out of scope") | `escalation_policy` | LLM-soft teaching; LLM owns the escalation choice within the named envelope. |
| Capability whitelist (which tools this Skill may dispatch) | `tools_required` | Runtime-hard enforcement; `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`. |
| Grounding-floor enforcement ("refuse record_outcome(class=resolve) when user-facing message lacks source_id") | `guardrails` (`must_cite_source` type) | Runtime-floor invariant per §1.4 + bounded per M2 §6 #4 verbatim authorization. |
| Capability-floor enforcement ("refuse request_handover(intake_complete_for_uc_X) when required fields missing") | `guardrails` (`intake_complete_required` type) | Runtime-floor invariant per §1.4; mirrors §E1 pattern + Sprint 7 §I2 precedent. |
| Customer-facing language, empathy, brevity, per-step argument values | NEITHER `procedure` NOR `guardrails`; the orchestration shell `system_prompt.txt` covers cross-Skill language norms; per-step arguments are LLM-owned per §1.3. | §1.3 LLM-owned customer language; §1.4 boundary respected. |
| Per-UC-pair branch logic ("if UC=X do Y; if UC=Z do W") | FORBIDDEN. NOT allowed in `procedure`, `guardrails`, `state_inheritance`, or any other Skill field. | §1.7 enforced; Codex per-sub-sprint review verifies. |
| Cross-Skill teaching (envelope mechanics: "you operate within the Skill the runtime selects; the Skill envelope names the tool whitelist and procedure; respect the envelope but exercise judgment on response strategy") | Orchestration shell (`system_prompt.txt`) | Cross-Skill content; sibling to existing Sprint 23 `already_called` cross-Skill teaching pattern. |

### 5.4 Rationale

- **Field-level separation makes the LLM-soft vs Runtime-hard
  distinction explicit at the data-model level.** A Skill author
  declaring `guardrails` knows they are declaring Runtime
  enforcement; declaring `procedure` knows they are declaring
  LLM-soft teaching. The two cannot be confused.
- **Declarative `guardrails` DSL (typed objects) over free-form
  text.** Reasons enumerated in §5.2.
- **`observe_only` mode explicitly excluded.** Per OLD Sprint 36
  D2 §3.2 verbatim: an observation is NOT a guardrail; observations
  belong in `ContextProjectionBuilder` slots + trace logs. A
  guardrail by definition GATES a capability-floor invariant.
- **`grounding_instruction` and `escalation_policy` are separate
  fields, not nested under `procedure`.** Matches the existing
  PhasePlan record's 3-field structure (`systemInstruction` +
  `groundingInstruction` + `escalationPolicy` — verified at
  `PhasePlan.java`); preserves migration losslessness (decision (e)
  §6 maps each existing PhaseEvaluator branch's content into the
  corresponding Skill field without merging).
- **No `customer_language_constraints` field on Skill.** §1.3
  reserves customer-facing language to the LLM; the orchestration
  shell carries cross-Skill brevity / empathy norms; per-Skill
  customer-language constraints would violate §1.3.

### 5.5 Alternatives considered + why rejected

**Alternative A: merge `procedure` + `grounding_instruction` +
`escalation_policy` into one field `instruction_text`.** Rejected:
loses the migration mapping fidelity (existing PhaseEvaluator
branches carry these as separate strings); harder to map to the
existing PhasePlan record without re-shape.

**Alternative B: `guardrails` as free-form text strings parsed
at dispatch time by a regex or grammar.** Rejected per §5.2:
schema validation at boot is more reliable + Codex-reviewable +
testable than runtime parsing.

**Alternative C: `guardrails` as inline Java method references
(e.g., `guardrails: [com.gumtree.csagent.runtime.skill.guards.MustCiteSource]`).**
Rejected: tightly couples Skill YAML to Java class names; breaks
the externalization goal (a Skill content tweak would need both
YAML and Java edits). Typed declarative shape is the cleaner fit.

**Alternative D: `procedure` contains both teaching AND inline
guardrail markers (e.g., `procedure: |\n  Call search_knowledge.\n  Then resolve_article.\n  Then record_outcome.\n  [GUARD: must_cite_source]`).**
Rejected: mixes LLM-soft and Runtime-hard in one field; the LLM
might mistake `[GUARD:...]` markers for instructions to interpret;
the dispatcher would need to re-parse the procedure text. Separate
fields are cleaner.

**Alternative E: `guardrails` `on_fail: observe_only` mode allowed.**
Rejected per OLD Sprint 36 D2 §3.2 + §5.4: observations belong in
projection / trace, NOT in guardrails. A guardrail without an
enforcement effect is not a guardrail. Cleaner separation.

### 5.6 §1.7 boundary check

- The `procedure` field carries principle-level teaching only.
  Codex per-sub-sprint review verifies per-Skill body that no
  per-UC-pair if-else is present.
- The `guardrails` declarations carry parameters that may name a
  UC-scoped condition (e.g., `escalation_reason_pattern:
  "intake_complete_for_uc_*"`), but the parameter is REGISTRY-
  DRIVEN scoping (the predicate fires when the LLM emits an
  escalation reason matching the pattern), NOT a per-UC-pair branch
  in the predicate body. The actual required-fields check is via
  `IntakeFieldsRegistry.intakeComplete(uc, collected)` — a single
  registry lookup, not a per-UC table.
- The orchestration shell content (cross-Skill teaching) is bounded
  to envelope mechanics + universal norms (empathy, brevity); per-
  Skill content moves into Skill `procedure`. No per-UC-pair if-
  else in either surface.

§1.7 boundary preserved.

### 5.7 §1.3 / §1.4 boundary check

- **§1.3 preserved.** `procedure` / `grounding_instruction` /
  `escalation_policy` are LLM-soft. The LLM owns deviation, response
  strategy, UC hypothesis judgment, customer-facing language, per-
  step argument choice. No semantic decision is enforced by Java
  for these fields.
- **§1.4 preserved.** `tools_required` (capability boundary),
  `guardrails` (grounding-floor / capability-floor enforcement)
  are Runtime-hard per §1.4. The bounded inversion of
  `D-hard-citation-gate` for `must_cite_source` is authorized
  verbatim by M2 §6 #4 (carried into decision (g) §8 for S1
  predicate scope); no expansion.

### 5.8 Downstream sub-sprint reference

Decision (d) is implemented in **Sprint 38** (Skill data model
fields) + **Sprint 39** (guardrails populated). Sprint 38 ships
the schema; Sprint 39 ships the concrete guardrail types
(`must_cite_source`, `intake_complete_required`,
`faq_miss_handover_requires_resolve_attempt`,
`premature_resolve_outcome_guard`) + their dispatcher
implementations (per decision (h) §9).

## 6. Decision (e) — retroactive migration mapping for ALL 6 phase YAMLs

### 6.1 Decision statement

The existing 6 phase content sources in `PhaseEvaluator.java`
(verified HEAD 6d97888 — DISCOVER at lines 397-457; CONFIRM at
lines 462-487; CLOSE at lines 492-509; ESCALATE at lines 513-542;
RESOLVE-INTAKE at lines 552-596; RESOLVE-FAQ at lines 598-665)
migrate into 6 Skill YAML files under
`server/src/main/resources/skills/`:

| Phase (current PhaseEvaluator branch) | Target Skill YAML file | Sprint that migrates |
|---|---|---|
| DISCOVER (lines 397-457) | `skills/discover_triage.yaml` | Sprint 38 |
| CONFIRM (lines 462-487) | `skills/confirm.yaml` | Sprint 38 |
| ESCALATE (lines 513-542) | `skills/escalate.yaml` | Sprint 38 |
| CLOSE (lines 492-509) + TERMINAL (post-loop terminal handling) | `skills/terminal.yaml` (consolidated) | Sprint 38 |
| RESOLVE-INTAKE (lines 552-596) | `skills/resolve_intake_collect_and_handover.yaml` | Sprint 39 |
| RESOLVE-FAQ (lines 598-665) | `skills/resolve_faq_grounded_answer.yaml` | Sprint 39 |

**CLOSE / TERMINAL consolidation note.** The current
`PhaseEvaluator.java` carries a CLOSE branch (lines 492-509) that
handles the closing turn with `record_outcome` + a closing
`user_message`. Per the M2 acceptance bar §6.1 above ("six phases:
DISCOVER + CONFIRM + RESOLVE_FAQ + RESOLVE_INTAKE + ESCALATE +
TERMINAL"), CLOSE consolidates with TERMINAL into one Skill file
`terminal.yaml`. Sprint 38 dev decides whether the consolidated
Skill carries `applicable_phases: [CLOSE, TERMINAL]` or whether the
runtime phase-name CLOSE is renamed at Sprint 38 to TERMINAL (the
latter would touch the runtime phase enum, which is RISKIER and
likely OUT OF SCOPE for Sprint 38 per M2 §6 #2 "Skill terminal
predicate may be a Java enforcement ONLY if it protects Runtime-
owned floor" + general scope conservatism). **Default
recommendation: Sprint 38 keeps the phase enum as "CLOSE" + creates
`terminal.yaml` with `applicable_phases: ["CLOSE"]`; the file name
`terminal.yaml` reflects the architectural intent that this Skill
represents the terminal phase, and a future M3+ sprint may rename
the phase enum without further Skill-file churn.** Surface to
deliver-agent + human OQ at Sprint 38 planning round.

### 6.2 Per-phase field mapping

For each phase, the migration maps current Java-string content into
Skill fields. The mappings below cite the HEAD line numbers for
each source content piece.

#### 6.2.1 DISCOVER → `skills/discover_triage.yaml`

Current source (HEAD `PhaseEvaluator.java:397-457`):

- `objective` (line 401-402): "Identify the user's use case by
  asking clarifying questions or interpreting their message, then
  commit it via classify_use_case"
- `allowedTools` (line 403): `[search_knowledge, classify_use_case]`
- `requiredContextKeys` (line 404): `[form_context,
  candidate_use_cases]`
- `maxToolSteps` (line 405): 2
- `allowInterimMessage` (line 406): false
- `validTerminalOutcomes` (lines 407-410): `[CLARIFICATION_NEEDED,
  FINAL_ANSWER, ESCALATE]`
- `systemInstruction` (lines 411-448): the multi-paragraph DISCOVER
  teaching, including:
  - Lines 412-417: core DISCOVER directive (identify UC, look at
    form context, call classify_use_case).
  - Lines 418-431: Sprint 7 §I0 weak-candidate cue (when
    candidate_use_cases is empty/weak AND form context is
    empty/UNKNOWN, do NOT request_handover; gather evidence
    instead).
  - Lines 432-448: Sprint 33 ad-status disambiguation cue (when
    ad_status_observed is REMOVED/SUSPENDED/EXPIRED AND
    topic_subject_carries_multiple_candidate_ucs is true, ask ONE
    clarifying question before committing classify_use_case).
- `groundingInstruction` (lines 449-452): "Do not commit to
  detailed answers in DISCOVER. Your job is to determine the use
  case category..."
- `escalationPolicy` (lines 453-455): "Escalate if user explicitly
  requests human help, if request is clearly out of scope, or if
  you cannot disambiguate after one clarification."

Target Skill YAML field mapping (`skills/discover_triage.yaml`):

```yaml
name: discover_triage
description: DISCOVER phase Skill — identify UC via clarifying questions or commit when intent is clear.
applicable_phases: [DISCOVER]
applicable_use_cases: ["*"]
tools_required:
  - search_knowledge
  - classify_use_case
required_context_keys:
  - form_context
  - candidate_use_cases
max_tool_steps: 2
allow_interim_message: false
valid_terminal_outcomes:
  - CLARIFICATION_NEEDED
  - FINAL_ANSWER
  - ESCALATE
objective: |
  Identify the user's use case by asking clarifying questions or
  interpreting their message, then commit it via classify_use_case.
procedure: |
  You are in the DISCOVER phase. Your goal is to identify which
  Use Case applies to the customer. Look at the form context,
  candidate use cases, and conversation history. When the user's
  intent is clear (or you can infer it with a supporting detail),
  call classify_use_case with the matching use_case_id and a
  confidence in [0,1]. Otherwise ask one clear clarifying question,
  or escalate if the user's request is out of scope.

  Weak-candidate cue (Sprint 7 §I0 migration): when
  candidate_use_cases is empty or weak AND form context is
  empty/UNKNOWN AND the current user message is clearly FAQ-shaped
  ("how do I X", "can I Y", "what items are allowed") OR is payment
  / sale-proceeds-shaped ("how do I receive payment", "how do I
  get paid when I sell", "how does payout work"), do NOT
  request_handover with `faq_miss_threshold_exceeded` after a
  single user turn. Instead, gather enough evidence to classify
  toward the right FAQ-path UC: call search_knowledge with the
  user's question as the query, then call classify_use_case with
  the most plausible UC (payment / sale-proceeds questions point
  to UC-F; how-to-post and general advertising questions to UC-B;
  messaging to UC-C; account / login to UC-D). Once classified,
  RESOLVE will run the grounded resolve sequence.

  Sprint 33 ad-status disambiguation cue (Sprint 33 migration —
  read alongside `candidate_use_cases` and the
  `discover_disambiguation_signals` projection): when the projection
  shows the user's listing is in a not-visible state
  (`ad_status_observed` is one of REMOVED / SUSPENDED / EXPIRED)
  AND `topic_subject_carries_multiple_candidate_ucs` is true, the
  user's literal request is the disambiguation signal — not the
  listing's database row. A user asking why the ad is gone, what
  happened to it, or where it went is asking to UNDERSTAND the
  situation (FAQ-resolvable, classify toward the visibility /
  ad-status explanation UC). A user asking to appeal, contest, or
  reverse the removal is asking to ACT (the appeal UC, an intake
  path). If the user's request is ambiguous between understanding
  and acting, ask ONE focused clarifying question this turn before
  committing classify_use_case (for example: "Do you want to know
  the reason it was removed, or do you want to appeal the
  removal?"). Do not commit an intake-path UC purely on
  `ad_status_observed` alone; the user's stated need is the
  disambiguation signal.
grounding_instruction: |
  Do not commit to detailed answers in DISCOVER. Your job is to
  determine the use case category (call classify_use_case), then
  RESOLVE will produce the actual resolution.
escalation_policy: |
  Escalate if user explicitly requests human help, if request is
  clearly out of scope, or if you cannot disambiguate after one
  clarification.
guardrails: []
state_inheritance:
  inherit: [customer_context]
  reset: []
  soft_signal_via_projection: [alternate_candidate_use_cases, discover_disambiguation_signals]
```

Note: the Sprint 33 ad-status disambiguation cue currently lives
in PhaseEvaluator's DISCOVER systemInstruction (lines 432-448),
NOT in system_prompt.txt — verified at HEAD. (The
`system_prompt.txt:36-43` `discover_disambiguation_signals`
paragraph covers the PROJECTION SLOT explanation; the per-UC
disambiguation cue lives in PhaseEvaluator.) This means Sprint 38
DISCOVER migration carries the Sprint 33 cue as part of
`procedure`; Sprint 40 separately handles the system_prompt.txt
side.

#### 6.2.2 CONFIRM → `skills/confirm.yaml`

Current source (HEAD `PhaseEvaluator.java:462-487`):

- `objective` (line 466): "Determine whether the user is satisfied
  with the prior answer"
- `allowedTools` (line 467): `[record_outcome, request_handover]`
- `requiredContextKeys` (line 468): `[form_context,
  conversation_history]`
- `maxToolSteps` (line 469): 2
- `allowInterimMessage` (line 470): false
- `validTerminalOutcomes` (lines 471-474): `[FINAL_ANSWER,
  CLARIFICATION_NEEDED, ESCALATE]`
- `systemInstruction` (lines 475-481): CONFIRM interpretation
  guidance (interpret satisfaction; record_outcome resolved if
  satisfied; request_handover or transition to RESOLVE otherwise).
- `groundingInstruction` (lines 482-484): "Read the user's response
  carefully. Sentiment matters more than literal words..."
- `escalationPolicy` (lines 485-486): "Escalate if user clearly
  expresses dissatisfaction or requests a human."

Target Skill YAML mapping (`skills/confirm.yaml`):

```yaml
name: confirm
description: CONFIRM phase Skill — interpret user satisfaction with prior answer; record_outcome resolved or rebound to RESOLVE.
applicable_phases: [CONFIRM]
applicable_use_cases: ["*"]
tools_required:
  - record_outcome
  - request_handover
required_context_keys:
  - form_context
  - conversation_history
max_tool_steps: 2
allow_interim_message: false
valid_terminal_outcomes:
  - FINAL_ANSWER
  - CLARIFICATION_NEEDED
  - ESCALATE
objective: |
  Determine whether the user is satisfied with the prior answer.
procedure: |
  You are in the CONFIRM phase. Interpret whether the user is
  satisfied with the prior answer. If satisfied (e.g., 'thanks',
  'that helps', 'yes'), call record_outcome with outcome='RESOLVED'.
  If not satisfied (e.g., 'no', 'still not working', 'I need more
  help'), call request_handover with reason 'user_dissatisfied' OR
  transition back to RESOLVE if appropriate.
grounding_instruction: |
  Read the user's response carefully. Sentiment matters more than
  literal words. When unclear, ask a single yes/no clarification.
escalation_policy: |
  Escalate if user clearly expresses dissatisfaction or requests a
  human.
guardrails: []
state_inheritance:
  inherit: [customer_context, accumulated_tool_results]
  reset: []
  soft_signal_via_projection: []
```

#### 6.2.3 ESCALATE → `skills/escalate.yaml`

Current source (HEAD `PhaseEvaluator.java:513-542`):

- `allowedTools` (line 522): `[request_handover, record_outcome]`
  (note `create_case_controlled` is INTENTIONALLY excluded per the
  Codex 1.8 / customer_service_tool_spec_v0_2.yaml runtime-only
  visibility rule per inline comment at lines 514-521).
- `objective` (line 526): "Complete the handover to a human agent
  and inform the customer"
- `requiredContextKeys` (line 528): `[form_context,
  customer_context]`
- `maxToolSteps` (line 529): 2
- `validTerminalOutcomes` (lines 531-533): `[ESCALATE, FINAL_ANSWER]`
- `systemInstruction` (lines 534-536): "You are in the ESCALATE
  phase. Send a clear handover message and ensure request_handover
  has been called with an appropriate escalation_reason."
- `groundingInstruction` (lines 537-539): "Tell the user a human
  agent will assist them. Provide expected SLA if known. Do not
  promise specific outcomes."
- `escalationPolicy` (lines 540-541): "Already in ESCALATE —
  finalize the handover."

Target Skill YAML mapping (`skills/escalate.yaml`):

```yaml
name: escalate
description: ESCALATE phase Skill — finalize handover and inform the customer.
applicable_phases: [ESCALATE]
applicable_use_cases: ["*"]
tools_required:
  - request_handover
  - record_outcome
required_context_keys:
  - form_context
  - customer_context
max_tool_steps: 2
allow_interim_message: false
valid_terminal_outcomes:
  - ESCALATE
  - FINAL_ANSWER
objective: |
  Complete the handover to a human agent and inform the customer.
procedure: |
  You are in the ESCALATE phase. Send a clear handover message and
  ensure request_handover has been called with an appropriate
  escalation_reason.
grounding_instruction: |
  Tell the user a human agent will assist them. Provide expected
  SLA if known. Do not promise specific outcomes.
escalation_policy: |
  Already in ESCALATE — finalize the handover.
guardrails: []
state_inheritance:
  inherit: [customer_context, accumulated_tool_results]
  reset: []
  soft_signal_via_projection: []
```

**Note on `create_case_controlled` exclusion**: the inline comment
at PhaseEvaluator lines 514-521 documents this is a runtime-only
tool deliberately not exposed to the LLM. The Skill migration
preserves this — `escalate.yaml` does NOT list
`create_case_controlled` in `tools_required`. The runtime continues
to create cases deterministically via
`ControlKernel.createCaseIfNeeded` for forced escalations and
`PhaseEvaluator.createCaseIfAllowed` for intake completion (per the
existing pattern; out of M2 scope to change).

#### 6.2.4 CLOSE (consolidated as `terminal`) → `skills/terminal.yaml`

Current source (HEAD `PhaseEvaluator.java:492-509`):

- `objective` (line 496): "Send a polite closing message and record
  the final outcome"
- `allowedTools` (line 497): `[record_outcome]`
- `requiredContextKeys` (line 498): `[form_context]`
- `maxToolSteps` (line 499): 2
- `validTerminalOutcomes` (line 501): `[FINAL_ANSWER]`
- `systemInstruction` (lines 502-504): "You are in the CLOSE phase.
  Thank the user and confirm the outcome. Call record_outcome with
  the appropriate outcome if not already recorded."
- `groundingInstruction` (lines 505-506): "Keep the closing message
  brief, warm, and final. Do not introduce new topics."
- `escalationPolicy` (lines 507-508): "Do not escalate from CLOSE.
  The session is ending."

Target Skill YAML mapping (`skills/terminal.yaml`):

```yaml
name: terminal
description: Terminal-phase Skill — polite closing + record final outcome (current CLOSE phase; possibly renamed to TERMINAL in M3+).
applicable_phases: [CLOSE]   # OQ: rename phase to TERMINAL? Deliver-agent + human pick at Sprint 38.
applicable_use_cases: ["*"]
tools_required:
  - record_outcome
required_context_keys:
  - form_context
max_tool_steps: 2
allow_interim_message: false
valid_terminal_outcomes:
  - FINAL_ANSWER
objective: |
  Send a polite closing message and record the final outcome.
procedure: |
  You are in the CLOSE phase. Thank the user and confirm the
  outcome. Call record_outcome with the appropriate outcome if not
  already recorded.
grounding_instruction: |
  Keep the closing message brief, warm, and final. Do not introduce
  new topics.
escalation_policy: |
  Do not escalate from CLOSE. The session is ending.
guardrails: []
state_inheritance:
  inherit: [customer_context, accumulated_tool_results]
  reset: []
  soft_signal_via_projection: []
```

#### 6.2.5 RESOLVE-INTAKE → `skills/resolve_intake_collect_and_handover.yaml`

Current source (HEAD `PhaseEvaluator.java:552-596`):

- `phase`, `useCase` (lines 570-571): RESOLVE phase + activeUc in
  `{UC-G, UC-H, UC-I, UC-J, UC-K}` per `INTAKE_UCS` at line 30.
- `objective` (lines 572-573): "Collect required intake details for
  " + ucDef.name() + " and hand over to the " + teamName + " team"
  (parameterized by UC + team).
- `allowedTools` (line 574): `[request_handover]` (note: per
  Codex 1.8 inline comment at lines 560-565,
  `create_case_controlled` is intentionally excluded for INTAKE
  UCs as well).
- `requiredContextKeys` (line 575): `[form_context, customer_context]`
- `maxToolSteps` (line 576): 3
- `validTerminalOutcomes` (lines 578-580): `[CLARIFICATION_NEEDED,
  ESCALATE]`
- `systemInstruction` (line 581): `buildIntakeSystemInstruction(activeUc, ucDef)` — a per-UC composition method at
  `PhaseEvaluator.java:207-208`. The method composes:
  - UC-team-specific intake script using `UC_TEAM_NAME` map at
    line 112.
  - Required-fields list from `IntakeFieldsRegistry.requiredFieldsFor(uc)`.
  - Per-UC team escalation reason via
    `intakeCompleteTrigger(activeUc)` at line 137-148.
- `groundingInstruction` (lines 582-590): "Use fixed-script
  templates and standard intake questions. Do NOT cite knowledge
  articles. Do NOT search the knowledge base. Your job is to
  collect required information and escalate to the human " +
  teamName + " team. " + (needsCase ? "A tracking case will be
  created automatically by the runtime when you escalate..." : "")
- `escalationPolicy` (lines 591-594): "Escalate via request_handover
  with reason='" + intakeCompleteTrigger(activeUc) + "' once intake
  fields are collected. Escalate immediately if the user explicitly
  requests human help."

Target Skill YAML mapping (`skills/resolve_intake_collect_and_handover.yaml`):

```yaml
name: resolve_intake_collect_and_handover
description: RESOLVE-INTAKE phase Skill — collect required intake fields and hand over to the matching human team.
applicable_phases: [RESOLVE]
applicable_use_cases:
  - UC-G
  - UC-H
  - UC-I
  - UC-J
  - UC-K
tools_required:
  - request_handover
required_context_keys:
  - form_context
  - customer_context
max_tool_steps: 3
allow_interim_message: false
valid_terminal_outcomes:
  - CLARIFICATION_NEEDED
  - ESCALATE
objective: |
  Collect required intake details for {uc_name} and hand over to
  the {team_name} team.
procedure: |
  You are collecting intake information for a {team_name} request.
  Use fixed-script templates and standard intake questions for the
  required fields of {uc_id}; do NOT cite knowledge articles; do
  NOT search the knowledge base. Your job is to collect required
  information and then call request_handover with
  escalation_reason='{intake_complete_trigger}' once the required
  fields are collected. {case_creation_note} Escalate immediately
  if the user explicitly requests human help.
grounding_instruction: |
  Use fixed-script templates and standard intake questions. Do NOT
  cite knowledge articles. Do NOT search the knowledge base.
escalation_policy: |
  Escalate via request_handover with the matching
  intake_complete_for_uc_X reason once intake fields are collected.
  Escalate immediately if the user explicitly requests human help.
guardrails:
  - type: intake_complete_required
    on_fail: reject_with_hint
    parameters:
      escalation_reason_pattern: "intake_complete_for_uc_*"
      required_fields_source: IntakeFieldsRegistry
state_inheritance:
  inherit: [customer_context, intake_fields_partial]
  reset: []
  soft_signal_via_projection: [prior_use_case_carry]
```

Template substitution placeholders: `{uc_name}`, `{uc_id}`,
`{team_name}`, `{intake_complete_trigger}`, `{case_creation_note}`.
Filled at compose time by `PhaseEvaluator.composeSkillPhasePlan(...)`
per decision (c) §4.1; values from
`UseCaseRegistryService.getUseCase(uc)`, `UC_TEAM_NAME` map,
`intakeCompleteTrigger(uc)`. **No per-UC-pair branch logic in the
template substitution** — each placeholder is a single registry /
map lookup keyed by the active UC.

The Sprint 7 §I2 `shouldRejectIncompleteIntakeHandover` predicate
(currently at `AgentRunLoopImpl.java:628-651`) migrates into the
`intake_complete_required` guardrail; see decision (g) §8 for the
predicate migration mapping + decision (h) §9 for dispatcher
semantics.

#### 6.2.6 RESOLVE-FAQ → `skills/resolve_faq_grounded_answer.yaml`

Current source (HEAD `PhaseEvaluator.java:598-665`):

- `objective` (lines 621-622): "Determine the customer's issue and
  provide a grounded, helpful answer for " + ucDef.name()
- `allowedTools` (lines 623-624): `[get_customer_context,
  search_knowledge, resolve_article, record_outcome,
  request_handover]`
- `requiredContextKeys` (lines 625-626): `[form_context,
  customer_context, listing_context, moderation_context]`
- `maxToolSteps` (line 627): 4
- `validTerminalOutcomes` (lines 629-632): `[FINAL_ANSWER,
  CLARIFICATION_NEEDED, ESCALATE]`
- `systemInstruction` (lines 633-641): the S1 FAQ-grounded-resolve
  sequence teaching ("the intended terminal sequence is
  search_knowledge → resolve_article → grounded customer-facing
  answer (with a source_id citation) → record_outcome").
- `groundingInstruction` (lines 642-653): the grounding-floor
  teaching ("MUST call search_knowledge first if
  accumulated_tool_results.search_knowledge is empty"; "after
  search_knowledge returns a viable hit, MUST call resolve_article";
  citation requirement; faq_miss_threshold_exceeded fallback).
- `escalationPolicy` (lines 654-664): per-UC-applicable escalation
  guidance + the Sprint 6 §G2 prohibition on short-circuit to
  `faq_miss_threshold_exceeded` when search_knowledge returned a
  viable hit and resolve_article has not yet been attempted.

Target Skill YAML mapping (`skills/resolve_faq_grounded_answer.yaml`):

```yaml
name: resolve_faq_grounded_answer
description: RESOLVE-FAQ phase Skill — grounded FAQ resolve with citation.
applicable_phases: [RESOLVE]
applicable_use_cases:
  - UC-A
  - UC-B
  - UC-C
  - UC-D
  - UC-E
  - UC-F
  - UC-FP
tools_required:
  - get_customer_context
  - search_knowledge
  - resolve_article
  - record_outcome
  - request_handover
required_context_keys:
  - form_context
  - customer_context
  - listing_context
  - moderation_context
max_tool_steps: 4
allow_interim_message: false
valid_terminal_outcomes:
  - FINAL_ANSWER
  - CLARIFICATION_NEEDED
  - ESCALATE
objective: |
  Determine the customer's issue and provide a grounded, helpful
  answer for {uc_name}.
procedure: |
  You are a helpful Gumtree customer support agent. Resolve the
  user's issue using the provided tools.

  FAQ-path RESOLVE flow (S1): the intended terminal sequence is
  search_knowledge → resolve_article → grounded customer-facing
  answer (with a source_id citation) → record_outcome. Only
  escalate via request_handover after a valid resolve attempt
  cannot complete (no viable hit, or resolve_article could not
  produce a grounded answer).
grounding_instruction: |
  If tool data contains specific information about the user's case
  (account/ad/moderation), answer from that first. For
  policy/process explanations, you MUST call search_knowledge
  first if accumulated_tool_results.search_knowledge is empty; do
  not produce a factual customer-facing answer without grounded
  knowledge evidence. After search_knowledge returns a viable hit,
  you MUST call resolve_article for the top hit before answering
  the customer; cite the source_id in your user_message. If
  search_knowledge returns no viable hit, request_handover with
  escalation_reason='faq_miss_threshold_exceeded' is allowed.
escalation_policy: |
  Escalate via request_handover if (a) the user explicitly
  requests a human (use escalation_reason='user_requested',
  priority 1), or (b) search_knowledge returned no viable hit and
  you cannot answer (use 'faq_miss_threshold_exceeded'), or (c)
  the issue is genuinely out of scope (use 'out_of_scope'). Do NOT
  short-circuit to
  request_handover('faq_miss_threshold_exceeded') when
  search_knowledge already returned a viable hit and
  resolve_article has not yet been attempted — the runtime will
  refuse such a handover and require a resolve_article attempt
  first.
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
state_inheritance:
  inherit: [customer_context, accumulated_tool_results]
  reset: []
  soft_signal_via_projection: [prior_use_case_carry]
```

The three guardrails on `resolve_faq_grounded_answer.yaml`:

- `faq_miss_handover_requires_resolve_attempt` — migrates Sprint 6
  §G2 `shouldRejectFaqMissHandover` (currently at
  `AgentRunLoopImpl.java:690-734`); see decision (g) §8.
- `premature_resolve_outcome_guard` — migrates Sprint 11 §M1
  `shouldRejectPrematureResolveOutcome` (currently at
  `AgentRunLoopImpl.java:745-759`); see decision (g) §8.
- `must_cite_source` — NEW S1 predicate per M2 §6 #4 verbatim
  authorization; bounded to (phase=RESOLVE_FAQ, tool_call=record_outcome
  with class=resolve, check=source_id citation present in
  user-facing message). See decision (g) §8 + Tier-0 candidate
  enumeration in §12.

### 6.3 Content overlap across phases (cross-Skill identification)

Reviewing the 6 Skill YAMLs above, cross-Skill content overlaps
are minimal. Each Skill's `procedure` is phase-specific. Common
patterns (e.g., "Escalate immediately if the user explicitly
requests human help") appear in multiple Skills' `escalation_policy`
sections but are PRINCIPLE-LEVEL universal — they should appear in
each Skill that allows escalation, NOT centralized as a shared
fragment. Rationale:

- A Skill is self-contained: the LLM reads ONE Skill envelope per
  turn; cross-referencing a shared fragment would require the LLM
  to mentally compose teaching from two sources.
- Universal escalation norms (the "user_requested takes priority"
  rule, the canonical escalation_reason enum) belong in the
  orchestration shell (`system_prompt.txt`) per decision (f) §7;
  they're cross-Skill teaching about HOW the LLM operates within
  any Skill envelope.

### 6.4 Behavioural equivalence test pattern

Each migrated phase ships with a `<Phase>SkillIntegrationTest`
that asserts:

- For representative UCs in the phase, the Skill-composed PhasePlan
  equals (or observationally matches) the legacy-branch PhasePlan
  produced by the pre-migration Java code path.
- `equals` is structural: same `objective` text after placeholder
  substitution, same `allowedTools` list (order may differ — tests
  compare as Set), same `requiredContextKeys`, same `maxToolSteps`,
  same `validTerminalOutcomes`, same `systemInstruction` text after
  template substitution, same `groundingInstruction`, same
  `escalationPolicy`.

The test pattern is dev judgement at Sprint 38 / Sprint 39; the
freeze locks the responsibility (behavioural equivalence test for
each migrated phase) per Sprint 38 / Sprint 39 hard fence in M2 §3.

### 6.5 Rationale

- **One Skill per phase (with UC scope per Skill).** Maps directly
  to current PhaseEvaluator's per-phase branches; minimal cognitive
  overhead for the migration; allows Sprint 38 to migrate the 4
  simpler phases without touching the 2 complex ones.
- **Template-substitution placeholders for per-UC entity names.**
  Avoids per-UC-pair branch logic in `procedure` / `objective` /
  `grounding_instruction`; preserves §1.7.
- **Sprint 33 ad-status disambiguation cue migrated as part of
  DISCOVER `procedure`** (NOT extracted to a separate Skill).
  Rationale: the cue applies to DISCOVER-phase decisions; it's
  phase-specific, not cross-Skill.
- **CLOSE→TERMINAL consolidation note** preserves backward
  compatibility (no phase enum rename in Sprint 38) while
  signalling architectural intent in the file name.
- **Behavioural-equivalence test per phase** is the migration
  safety gate; no behaviour change is allowed in any one migration
  sub-sprint.

### 6.6 Alternatives considered + why rejected

**Alternative A: one mega-Skill `resolve.yaml` covering both
RESOLVE-FAQ and RESOLVE-INTAKE.** Rejected: the two branches have
fundamentally different tool whitelists, terminal predicates, and
intake-vs-FAQ semantics; merging them would force per-UC if-else
inside the Skill body (§1.7 violation).

**Alternative B: split each phase Skill further by UC sub-paths
(e.g., `resolve_faq_uc_a.yaml`, `resolve_faq_uc_b.yaml`).**
Rejected: the per-UC Skill content is structurally identical
across FAQ-path UCs (the same procedure, same guardrails); only
the entity name differs (handled via template substitution). 7
per-UC files would multiply maintenance overhead without
information gain.

**Alternative C: cross-Skill shared-fragment mechanism (e.g.,
`includes: [common_escalation_norms.yaml]`).** Rejected for M2:
adds Skill-composition complexity at YAML load time; the universal
norms fit cleanly in the orchestration shell per decision (f).
Revisit in M3+ if specific shared fragments warrant it.

**Alternative D: migrate phase content into Skills but keep the
runtime phase enum exactly as-is including the legacy CLOSE name.**
Accepted as default (see §6.1 CLOSE/TERMINAL note); the file name
`terminal.yaml` carries the architectural intent without forcing a
risky phase-enum rename in Sprint 38.

### 6.7 §1.7 boundary check

- Per-phase Skills each carry principle-level `procedure` text;
  no per-UC-pair if-else. Codex Sprint 38 / Sprint 39 review
  verifies per-Skill body.
- Template substitution placeholders are registry-driven (single
  lookup per placeholder); NOT per-UC-pair branch logic.
- `applicable_use_cases` lists enumerate UCs the Skill scopes to —
  registry-scoping, NOT semantic per-UC decision. See decision (a)
  §2.5 + §11 (decision (j)) Q1.
- Sprint 33 ad-status disambiguation cue (DISCOVER Skill
  `procedure`) is principle-level: "when the projection shows...
  AND ... is true, ask ONE focused clarifying question". NOT
  per-UC-pair branch — applies uniformly to any UC where the
  projection signals are populated.

### 6.8 §1.3 / §1.4 boundary check

- **§1.3 preserved.** Each Skill's `procedure` is LLM-soft. The
  LLM owns deviation from the recommended order (e.g., on UC-D
  account-recovery, the LLM may judge that customer_context is
  sufficient and skip search_knowledge per the OLD F2 §3 S4
  description — that judgement remains LLM-owned).
- **§1.4 preserved + extended for RESOLVE-FAQ.** RESOLVE-FAQ
  Skill carries three guardrails (two migrated Sprint 6/11; one
  NEW S1 must_cite_source per §6 #4 verbatim authorization).
  RESOLVE-INTAKE carries one guardrail (intake_complete_required,
  migrating Sprint 7). All four guardrails enforce Runtime-floor
  invariants only.

### 6.9 Downstream sub-sprint reference

Decision (e) is implemented in **Sprint 38** (4 simpler phases:
discover_triage + confirm + escalate + terminal) + **Sprint 39**
(2 complex phases: resolve_faq_grounded_answer +
resolve_intake_collect_and_handover, plus the three migrated
predicates + S1/S2 + dispatcher). Sprint 38 ships the 4 Skill
YAMLs + their integration tests; Sprint 39 ships the remaining 2
Skill YAMLs + their guardrails + the dispatcher; Sprint 39 close
removes the legacy branches from PhaseEvaluator.

## 7. Decision (f) — retroactive migration mapping for Sprint 23/31/33 teaching paragraphs

### 7.1 Decision statement

The three teaching paragraphs currently in
`server/src/main/resources/prompts/system_prompt.txt` migrate
into corresponding Skill `procedure` / `grounding_instruction`
blocks at Sprint 40. Mapping verified at HEAD 6d97888
(file is 101 lines):

| Paragraph | Current location | Sprint that authored | Target Skill home |
|---|---|---|---|
| `already_called` teaching ("Re-using prior tool results...") | `system_prompt.txt:23-28` | Sprint 23 | **Orchestration shell** (stays in `system_prompt.txt` — cross-Skill teaching about envelope mechanics; see §7.4 rationale) |
| `alternate_candidate_use_cases` teaching | `system_prompt.txt:30-34` | Sprint 31 | **`skills/discover_triage.yaml` `procedure`** — the projection slot informs DISCOVER decisions; LLM reads when about to commit classify_use_case |
| `discover_disambiguation_signals` teaching | `system_prompt.txt:36-43` | Sprint 33 | **`skills/discover_triage.yaml` `procedure`** — the projection slot informs DISCOVER decisions; integrates with the Sprint 33 ad-status cue already in DISCOVER `procedure` per decision (e) §6.2.1 |

### 7.2 Per-paragraph rationale

#### 7.2.1 Sprint 23 `already_called` → orchestration shell (stays)

This paragraph teaches the LLM how to read the `already_called`
projection slot — re-using prior tool results within the same run
rather than re-emitting calls. The teaching is CROSS-SKILL: it
applies to ANY Skill the runtime selects, on ANY tool, on ANY UC.
The principle is "don't repeat work the runtime has already
performed in this run".

Per decision (d) §5.3 content placement rules ("Cross-Skill
teaching (envelope mechanics) → orchestration shell"), this
paragraph stays in `system_prompt.txt`. Sprint 40 may slightly
re-word it to reference "Skill envelope" framing (instead of
implicit phase / plan framing) but the content is structurally
unchanged.

#### 7.2.2 Sprint 31 `alternate_candidate_use_cases` → DISCOVER Skill

This paragraph teaches the LLM how to read the
`alternate_candidate_use_cases` projection slot — UCs the intake
router considered plausible at session creation, surfaced as
soft evidence the LLM may use to consider a reroute. The teaching
is PHASE-SPECIFIC: the slot is most relevant in DISCOVER (when
the LLM is choosing classify_use_case) and in early RESOLVE
turns (when a topic shift might surface). It is NOT cross-Skill
in the same way as `already_called` (which applies on every tool
dispatch).

Sprint 40 migrates the paragraph from `system_prompt.txt:30-34`
into `discover_triage.yaml` `procedure` as an additional
principle-level teaching block. The LLM reads it within the
DISCOVER Skill envelope.

**Note on RESOLVE-side carry**: the paragraph's later-turn use
(after DISCOVER closes) is partially covered by Sprint 41's
`prior_use_case_carry` projection slot + per-Skill `state_inheritance`
declaration (decision (i) §10). The Sprint 31 paragraph's mid-
session reroute teaching ("when a later user turn surfaces
evidence the active UC is no longer the best fit") may surface
as a soft signal in resolve_faq / resolve_intake Skills via
`state_inheritance.soft_signal_via_projection`. Sprint 40 / Sprint
41 dev judgement on whether the teaching needs a copy in
resolve_faq / resolve_intake `procedure` OR whether the projection
slot + state_inheritance soft signal is sufficient. **Default
recommendation: only place the teaching in `discover_triage.yaml`
(plus an orchestration-shell one-liner about reading projection
slots universally); rely on the projection slot's intrinsic
self-documentation via the slot description for resolve-side
reads.**

#### 7.2.3 Sprint 33 `discover_disambiguation_signals` → DISCOVER Skill

This paragraph teaches the LLM how to read the
`discover_disambiguation_signals` projection slot — a structured
object surfacing ad-status disambiguation signals. The teaching
is DISCOVER-specific: the slot drives DISCOVER's
classify_use_case decision (FAQ-resolvable vs intake-path appeal
choice). It already partially lives in PhaseEvaluator's DISCOVER
systemInstruction (lines 432-448 — the Sprint 33 ad-status
disambiguation cue); see decision (e) §6.2.1.

Sprint 40 migrates the paragraph from `system_prompt.txt:36-43`
into `discover_triage.yaml` `procedure` as an additional block.
Together with the Sprint 33 cue already migrated per decision (e),
the DISCOVER Skill's `procedure` post-Sprint-40 carries both: (i)
the slot-reading teaching (Sprint 31 from system_prompt.txt
lines 36-43), and (ii) the cue teaching (Sprint 33 from
PhaseEvaluator lines 432-448).

### 7.3 Orchestration-shell content (post-Sprint-40 `system_prompt.txt`)

The post-Sprint-40 `system_prompt.txt` is a thin orchestration
shell containing:

| Content | Source (current HEAD) | Why stays in shell |
|---|---|---|
| Bot identity, JSON output contract, three required fields | `system_prompt.txt:1-9` | Cross-Skill identity / protocol; ALL Skills require this. |
| Empty vs non-empty `tool_calls` guidance | `system_prompt.txt:10-12` | Cross-Skill tool-dispatch protocol. |
| Universal rules (helpful, empathetic, professional; never claim human; never promise actions; never fabricate; ground in retrieved knowledge or context; concise) | `system_prompt.txt:14-21` | Cross-Skill universal norms. |
| `already_called` projection slot teaching | `system_prompt.txt:23-28` | Cross-Skill (per §7.2.1). |
| NEW: orchestration teaching about Skill envelope mechanics | Sprint 40 ADDS | "You operate within the Skill envelope the runtime selects per (phase, active_use_case). The envelope names the tool whitelist (you cannot dispatch a tool outside it), the recommended procedure (LLM-soft guidance, deviate when judgement warrants), the grounding instruction (LLM-soft), and the escalation policy (LLM-soft). Skill-declared guardrails enforce Runtime-floor invariants (e.g., a `record_outcome(class=resolve)` without a source_id citation will be rejected). Respect the envelope; exercise judgment on response strategy per the Constitution." |
| DISCOVER phase guidance (lines 45-52) — but compressed | `system_prompt.txt:45-52` | **PREMISE REFINEMENT (Sprint 37 §13)**: this content is currently in the shell, but is DISCOVER-specific. Sprint 40 dev judgement: either MIGRATE this paragraph into `discover_triage.yaml` `procedure` (it duplicates content already there), OR keep a compressed one-line orchestration version pointing to Skill `procedure` as source. **Default recommendation**: migrate the bulk into `discover_triage.yaml`; leave a one-line shell pointer ("Phase-specific guidance lives in the Skill `procedure`; refer to your current Skill envelope for DISCOVER directives"). |
| `request_handover` escalation_reason canonical-enum decision tree (lines 54-101) | `system_prompt.txt:54-101` | **PREMISE REFINEMENT (Sprint 37 §13)**: this content is cross-Skill (the canonical 23-value escalation_reason enum applies to any Skill that allows `request_handover` dispatch). It STAYS in the orchestration shell. The per-Skill `escalation_policy` block carries Skill-specific scope (e.g., DISCOVER's "escalate only if user explicitly requests, OOS, or can't disambiguate"); the canonical enum + decision tree stays universal. Sprint 40 verifies the cross-Skill content stays + Sprint-specific reduces. |

The exact post-Sprint-40 shell shape is dev judgement at Sprint
40. The freeze locks the principle ("cross-Skill content stays;
phase/Skill-specific content migrates") + the four migrations
above; Sprint 40 dev applies the principle to all 101 lines.

### 7.4 Rationale

- **Sprint 23 `already_called` is cross-Skill — stays in shell.**
  Universally relevant; no Skill should re-declare it.
- **Sprint 31 + Sprint 33 are DISCOVER-specific — migrate.** Phase-
  specific teaching should travel with the Skill; reduces shell
  size; makes the Skill self-contained for DISCOVER work.
- **System prompt token-count reduction is a meaningful but not
  primary goal.** The Skill envelope projection (per-turn LLM
  prompt) includes both the shell AND the Skill-specific
  `procedure`, so total token count per turn is approximately
  preserved. The benefit is reviewability: a developer wanting to
  understand DISCOVER behaviour reads `discover_triage.yaml`, not
  the entire `system_prompt.txt`.
- **`request_handover` decision tree (lines 54-101) stays cross-
  Skill.** The 23-value enum is universal; Skill-specific scope
  lives in per-Skill `escalation_policy`.

### 7.5 Alternatives considered + why rejected

**Alternative A: migrate ALL teaching paragraphs (including
`already_called`) into per-Skill `procedure` / `grounding_instruction`.**
Rejected: cross-Skill content would be duplicated across 6 Skills;
maintenance hazard; defeats the "Skill is self-contained" benefit
by creating cross-Skill copy-paste.

**Alternative B: keep ALL teaching paragraphs in
`system_prompt.txt`; move only the predicates (Sprint 6/7/11)
into Skill `guardrails`.** Rejected: defeats the M2 goal of
extracting scattered content. Sprint 31 / Sprint 33 are phase-
specific; their natural home is the Skill that uses them.

**Alternative C: migrate Sprint 31 `alternate_candidate_use_cases`
into BOTH `discover_triage.yaml` AND `resolve_faq_grounded_answer.yaml`
+ `resolve_intake_collect_and_handover.yaml` (the slot is
relevant on later-turn topic-shift detection too).** Rejected
per §7.2.2: cross-Skill copy is a maintenance hazard; the
projection slot's intrinsic self-documentation (the slot value
itself + the orchestration shell's universal "read projection
slots" teaching) is sufficient for resolve-side reads. The Sprint
41 `prior_use_case_carry` slot provides explicit cross-Skill
state inheritance.

**Alternative D: introduce a Skill `cross_skill_includes` field
that allows pulling fragments from a shared library.** Rejected
for M2 per decision (e) §6.6 Alternative C — adds complexity
without clear M2 need; revisit in M3+.

### 7.6 §1.7 boundary check

- The migrated teaching paragraphs (Sprint 31 + Sprint 33) are
  principle-level. Codex Sprint 40 review verifies post-migration
  the Skill `procedure` carries no per-UC-pair if-else.
- The orchestration shell post-Sprint-40 retains universal norms +
  cross-Skill teaching; no per-UC-pair logic.

### 7.7 §1.3 / §1.4 boundary check

- **§1.3 preserved.** Migrated teaching paragraphs are LLM-soft;
  LLM reads + judges. No semantic decision moved to Java.
- **§1.4 preserved.** Migration is a SURFACE-RELOCATION change
  (the teaching moves from one prompt assembly point to another);
  no new Runtime enforcement, no Java predicate change in Sprint
  40.

### 7.8 Downstream sub-sprint reference

Decision (f) is implemented in **Sprint 40** (teaching extraction
+ orchestration shell cleanup). Sprint 40 ships:

- `system_prompt.txt` EDIT: remove Sprint 31 paragraph (lines
  30-34) + Sprint 33 paragraph (lines 36-43); add orchestration-
  shell teaching about Skill envelope mechanics; refactor lines
  45-52 (DISCOVER phase guidance) per default recommendation in
  §7.3; preserve `request_handover` decision tree (lines 54-101)
  per §7.3.
- `discover_triage.yaml` EDIT: extend `procedure` with migrated
  Sprint 31 + Sprint 33 teaching content + the Sprint 33 cue
  already present.
- Behavioural-equivalence test (`SkillTeachingMigrationIntegrationTest`):
  real-LLM tests on representative DISCOVER + RESOLVE-FAQ +
  RESOLVE-INTAKE traces confirming the migrated teaching content
  reaches the LLM via Skill envelope projection equivalently to
  prior monolithic `system_prompt.txt`.
- Possibly: resolve inherited
  `SystemPromptUserRequestedTiebreakerTest` failure if the
  teaching reorganization addresses the underlying drift (deliver-
  agent + human evaluate at Sprint 40 close).

## 8. Decision (g) — retroactive migration mapping for Sprint 6/7/11 predicates

### 8.1 Decision statement

The three Java predicate methods currently in
`AgentRunLoopImpl.java` (verified HEAD 6d97888) migrate into Skill
`guardrails` blocks at Sprint 39:

| Predicate (current Java method) | Current location | Sprint that authored | Target Skill home | Target `guardrails` type |
|---|---|---|---|---|
| `shouldRejectFaqMissHandover` | `AgentRunLoopImpl.java:690-734`; dispatched at line 413 | Sprint 6 §G2 | `skills/resolve_faq_grounded_answer.yaml` | `faq_miss_handover_requires_resolve_attempt` |
| `shouldRejectIncompleteIntakeHandover` | `AgentRunLoopImpl.java:628-651`; dispatched at line 317; reject reason `INTAKE_COMPLETE_GUARD_REJECT_REASON` at line 120 | Sprint 7 §I2 | `skills/resolve_intake_collect_and_handover.yaml` | `intake_complete_required` |
| `shouldRejectPrematureResolveOutcome` | `AgentRunLoopImpl.java:745-759` (delegates to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`); dispatched at line 356 | Sprint 11 §M1 + Sprint 11.1 | `skills/resolve_faq_grounded_answer.yaml` | `premature_resolve_outcome_guard` |

Plus two NEW guardrail types added in Sprint 39:

| New predicate | Target Skill home | `guardrails` type | Bounded scope (per §6 #4 verbatim auth) |
|---|---|---|---|
| S1 citation presence on `record_outcome(class=resolve)` | `skills/resolve_faq_grounded_answer.yaml` | `must_cite_source` | (a) phase=RESOLVE_FAQ; (b) tool call=record_outcome with class=resolve; (c) check=source_id citation present in user-facing message. NOT a generic Java citation gate. |
| S2 intake-completeness on `request_handover` | (already covered by `intake_complete_required` above — S2 IS the existing Sprint 7 §I2 predicate; the M2 contribution is moving it from inline Java method to declarative Skill guardrail) | `intake_complete_required` | Bounded to (a) phase=RESOLVE_INTAKE; (b) tool call=request_handover with escalation_reason matching `intake_complete_for_uc_*`; (c) check=required fields per `IntakeFieldsRegistry.intakeComplete(uc, collected)`. |

### 8.2 Per-predicate migration mapping

#### 8.2.1 Sprint 6 §G2 `shouldRejectFaqMissHandover` → `faq_miss_handover_requires_resolve_attempt`

Current Java method (lines 690-734):

```java
static boolean shouldRejectFaqMissHandover(PhasePlan plan,
                                          ToolCall call,
                                          Map<String, Object> accumulatedToolResults) {
    if (plan.useCase() == null || !FAQ_PATH_UCS.contains(plan.useCase())) {
        return false;
    }
    // ... checks: tool=request_handover, reason=faq_miss_threshold_exceeded,
    //     accumulated_tool_results.search_knowledge is non-empty (viable hit
    //     surfaced), accumulated_tool_results.resolve_article is empty
    //     (no resolve_article attempt yet).
    // If all conditions match, return true (reject the handover; LLM should
    // call resolve_article first).
}
```

Migration target: `skills/resolve_faq_grounded_answer.yaml`
`guardrails[]`:

```yaml
- type: faq_miss_handover_requires_resolve_attempt
  on_fail: reject_with_hint
  parameters: {}
```

Semantic mapping:

- The predicate's `FAQ_PATH_UCS.contains(plan.useCase())` check
  becomes implicit: the guardrail is declared on
  `resolve_faq_grounded_answer.yaml`, which has
  `applicable_use_cases: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]`.
  The dispatcher only fires the guardrail when the active Skill is
  `resolve_faq_grounded_answer`; the FAQ-path UC check is
  registry-driven, not inlined.
- The remaining checks (tool=request_handover with
  reason=faq_miss_threshold_exceeded; search_knowledge non-empty;
  resolve_article empty) are encoded in the `SkillGuardrailDispatcher`'s
  implementation of the `faq_miss_handover_requires_resolve_attempt`
  type. The Java implementation lives in
  `SkillGuardrailDispatcher.handleFaqMissHandoverRequiresResolveAttempt`
  (or equivalent; Sprint 39 dev judgement on method name).
- The Java method `shouldRejectFaqMissHandover` is REMOVED from
  `AgentRunLoopImpl.java` at Sprint 39; the dispatch site at line
  413 is replaced by a generic `SkillGuardrailDispatcher.checkBeforeDispatch(...)`
  call (decision (h) §9.2).

#### 8.2.2 Sprint 7 §I2 `shouldRejectIncompleteIntakeHandover` → `intake_complete_required`

Current Java method (lines 628-651):

```java
static boolean shouldRejectIncompleteIntakeHandover(PhasePlan plan,
                                                   ToolCall call,
                                                   BotSession session) {
    if (plan.useCase() == null) return false;
    if (!plan.useCase().startsWith("UC-")) return false;
    // Check: tool=request_handover with reason matching
    //   intake_complete_for_uc_*; call IntakeFieldsRegistry.intakeComplete
    //   to see if required fields are present.
    // If reason matches the pattern AND intake is NOT complete, return true.
}
```

Migration target: `skills/resolve_intake_collect_and_handover.yaml`
`guardrails[]`:

```yaml
- type: intake_complete_required
  on_fail: reject_with_hint
  parameters:
    escalation_reason_pattern: "intake_complete_for_uc_*"
    required_fields_source: IntakeFieldsRegistry
```

Semantic mapping:

- The predicate's UC-G/H/I/J/K scope check is implicit via
  `applicable_use_cases: [UC-G, UC-H, UC-I, UC-J, UC-K]` on the
  Skill.
- The reason-pattern check + required-fields check are encoded in
  the `intake_complete_required` dispatcher implementation,
  parameterized by `escalation_reason_pattern` and
  `required_fields_source`.
- The `INTAKE_COMPLETE_GUARD_REJECT_REASON` constant at
  `AgentRunLoopImpl.java:120` is preserved as the reject-reason
  label in the dispatcher's hint structure (Sprint 39 may move it
  to `SkillGuardrailDispatcher` as a public constant).
- The Java method `shouldRejectIncompleteIntakeHandover` is
  REMOVED from `AgentRunLoopImpl.java` at Sprint 39.

#### 8.2.3 Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` → `premature_resolve_outcome_guard`

Current Java method (lines 745-759); delegates to
`ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)`.

Migration target: `skills/resolve_faq_grounded_answer.yaml`
`guardrails[]`:

```yaml
- type: premature_resolve_outcome_guard
  on_fail: reject_with_hint
  parameters: {}
```

Semantic mapping:

- The predicate ALREADY delegates to
  `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`
  per current code. The migration preserves this delegation: the
  `premature_resolve_outcome_guard` dispatcher implementation
  calls the SAME `ResolveDispositionEvaluator` method. No
  behavioural change.
- The `ResolveDispositionEvaluator` class itself is part of the
  Sprint 11 / 11.1 / 12 frozen surface (per
  `runtime_freeze_and_risk_policy.md` §1.1 #3 ResolveDisposition
  terminal-evidence guard); the M2 migration does NOT modify it,
  only changes which class invokes it.
- The Java method `shouldRejectPrematureResolveOutcome` in
  `AgentRunLoopImpl.java` is REMOVED at Sprint 39; the dispatch
  site at line 356 routes through the unified dispatcher.

#### 8.2.4 NEW S1 `must_cite_source` on `resolve_faq_grounded_answer.yaml`

This guardrail is NEW in M2 (the citation-presence predicate the
OLD M2-Skill Sprint 37 was meant to ship; NEW M2 re-homes it
inside the Skill abstraction at Sprint 39).

**Verbatim human authorization** (carried forward from OLD M2-Skill
§6 #4 into NEW M2 §6 #4, load-bearing for predicate scope):

> "Accept the Skill-bounded exception to D-hard-citation-gate.
> This is not a generic Java grounding-citation gate. It is a
> narrow S1 terminal predicate that only applies when the bot
> attempts `record_outcome(class=resolve)` inside the `RESOLVE_FAQ`
> scope, and only checks citation presence as the minimum
> grounding-floor condition. The original deferral of a generic
> Java citation gate remains valid."

The authorization governs the predicate scope. Sprint 39
implementation MUST NOT expand beyond:

- (a) Phase = `RESOLVE_FAQ` (i.e., `current_phase == RESOLVE` AND
  `active_use_case ∈ FAQ-path UCs` per Skill `applicable_use_cases`).
- (b) Tool call = `record_outcome` with `class=resolve` (per
  `RecordOutcomeTool.normalizeOutcomeClass` at
  `RecordOutcomeTool.java:110-121` resolving canonical `resolve` +
  legacy `RESOLVED` alias).
- (c) Check = `source_id` citation present in the bot's user-facing
  message. The predicate verifies the `user_message` from the same
  turn (or the prior bot turn if `record_outcome` is on a no-
  user-message turn) contains a non-empty `source_id` token. The
  predicate does NOT judge citation correctness, relevance, or
  content quality — only presence.

The predicate does NOT fire on:

- `record_outcome(class=escalate)` or `record_outcome(class=abandon)`.
- Non-RESOLVE_FAQ phases (DISCOVER / CONFIRM / CLOSE / ESCALATE /
  RESOLVE_INTAKE).
- INTAKE-path UCs (UC-G / UC-H / UC-I / UC-J / UC-K) — bounded by
  Skill `applicable_use_cases`.

Migration target: `skills/resolve_faq_grounded_answer.yaml`
`guardrails[]`:

```yaml
- type: must_cite_source
  on_fail: reject_with_hint
  parameters:
    outcome_class: resolve
    cite_token_field: source_id
```

Java implementation at Sprint 39:
`SkillGuardrailDispatcher.handleMustCiteSource(skill, context)`
reads the bot's user_message from the dispatch context
(`AgentRunLoopImpl.lastLlmRawResponse` per existing line 156 +
parsed user_message via `actionParser`), checks for `source_id`
token presence, and rejects the `record_outcome` call with hint
`s1_citation_presence_required` if absent. The LLM sees the
rejection on the next loop iteration and may retry with a
citation OR escalate via
`request_handover(faq_miss_threshold_exceeded)`.

**Tier-0 candidate question**: does this predicate semantics
qualify as a Tier-0 invariant for
`docs/runtime_freeze_and_risk_policy.md` §1 / §2? See §12 (Tier-0
candidate enumeration). Default recommendation per OLD Sprint 36
§8.3 (carried forward): DEFER — re-evaluate post-Sprint-39
observed traces.

#### 8.2.5 S2 intake-completeness — already covered

S2 is `intake_complete_required` per §8.2.2; the existing Sprint
7 §I2 predicate ALREADY implements (a)/(b)/(c). M2's contribution
is moving it from inline Java method to Skill guardrail
declaration. No new predicate semantics.

### 8.3 Rationale

- **Migrate predicates inside the Skill abstraction (NOT as
  adjacent Java methods per OLD Sprint 36 D6).** Reasons in §1.2:
  preserves the abstraction goal; predicate scope becomes
  declarative + reviewable; dispatcher unifies enforcement.
- **Preserve Sprint 11 §M1 + 11.1 delegation to
  `ResolveDispositionEvaluator`.** The Sprint 11 frozen surface
  per `runtime_freeze_and_risk_policy.md` §1.1 #3 is untouched;
  the migration only changes which class invokes the evaluator.
- **S1 `must_cite_source` scope strictly per §6 #4 verbatim.**
  Sprint 39 implementation + Codex Sprint 39 review verify no
  expansion.
- **Existing reject-reason labels preserved.** Sprint 6 / Sprint
  7 / Sprint 11 reject-reasons map to dispatcher hint structures
  for backward-compatible trace shape.

### 8.4 Alternatives considered + why rejected

**Alternative A: keep predicates as Java methods in
`AgentRunLoopImpl` (OLD Sprint 36 D6).** Rejected per §1.2: defeats
the abstraction goal; scattered predicates continue to accumulate.

**Alternative B: predicate code lives inline in each Skill's Java
SkillGuard class (one Java class per Skill that hosts its
predicates).** Rejected: forces every Skill to have a Java
companion class; loses the "Skill is YAML-only" simplicity; tighter
coupling between Skill content and Java code.

**Alternative C: S1 predicate as a DOWNGRADE to a new
`resolve_no_citation` outcome class (OLD Sprint 36 §4.4 alternative).**
Rejected per OLD Sprint 36 default recommendation: introducing a
new outcome class enum touches `RecordOutcomeTool.normalizeOutcomeClass`
+ `SessionOutcome.outcome` persistence + downstream eval — larger
surface than the §6 #4 authorization allows. Reject-and-hint is
the default; revisit in M3+ if observed traces warrant.

**Alternative D: encode predicate logic in the Skill `procedure`
text and rely on the LLM to self-enforce.** Rejected: violates §1.4
(grounding floor is Runtime-owned); the LLM can be nudged but
cannot be relied on for floor enforcement.

### 8.5 §1.7 boundary check

- Each migrated predicate's logic is REGISTRY-DRIVEN
  (`IntakeFieldsRegistry`, `FAQ_PATH_UCS` set, single
  `accumulated_tool_results` field lookups). No per-UC-pair
  branch.
- The S1 predicate scope is bounded per §6 #4 verbatim; the
  predicate body checks (phase, tool_call, citation presence) —
  three boolean conditions, NOT per-UC-pair logic.
- The S2 predicate scope is bounded per Sprint 7 §I2 + §E1
  precedent; registry-driven via `IntakeFieldsRegistry`.

§1.7 preserved.

### 8.6 §1.3 / §1.4 boundary check

- **§1.3 preserved.** All four guardrails enforce Runtime-floor
  invariants (grounding-floor, capability-floor); none enforces
  LLM-owned next-action / response-strategy / UC-hypothesis.
- **§1.4 preserved + extended for S1.** The S1 `must_cite_source`
  guardrail is a NEW Runtime-floor enforcement, bounded per §6 #4
  verbatim authorization. The bounded inversion of
  `D-hard-citation-gate` is the principled exception (NOT a
  generic citation gate; only the narrow Skill-bounded predicate);
  the original deferral of generic Java citation gate is preserved.

### 8.7 Downstream sub-sprint reference

Decision (g) is implemented in **Sprint 39** (RESOLVE-FAQ +
RESOLVE-INTAKE Skill migration + predicate migration + S1/S2 new
predicates + unified dispatcher). Sprint 39 ships:

- Three migrated guardrails on the two RESOLVE Skill YAMLs (per
  §8.1 table).
- Two NEW guardrails on `resolve_faq_grounded_answer.yaml` (S1
  must_cite_source) — S2 is the existing predicate moved.
- `SkillGuardrailDispatcher.java` implementing all five guardrail
  types (per decision (h) §9).
- REMOVAL of `shouldRejectFaqMissHandover`,
  `shouldRejectIncompleteIntakeHandover`,
  `shouldRejectPrematureResolveOutcome` from `AgentRunLoopImpl.java`.
- Dispatcher integration at the three existing dispatch sites
  (lines 317, 356, 413 → unified `dispatcher.checkBeforeDispatch(...)`).
- Behavioural-equivalence tests: existing predicate-fire scenarios
  preserved; NEW S1 tests covering target / neighbor / negative
  per Sprint 39 §8 stanza.

## 9. Decision (h) — unified Skill terminal-predicate dispatcher

### 9.1 Decision statement

A new Java class `SkillGuardrailDispatcher` at
`server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java`
provides a unified dispatch surface that reads the active Skill's
`guardrails[]` declarations and enforces each registered guardrail
type at the appropriate dispatch site. It REPLACES the scattered
`shouldRejectXxx` static methods in `AgentRunLoopImpl.java` (per
decision (g) §8).

Public surface (Sprint 39 dev judgement on exact syntax):

```java
@Component
public class SkillGuardrailDispatcher {

    /**
     * Invoked BEFORE the tool call is dispatched. Walks the active
     * Skill's guardrails and returns a non-empty Optional with a
     * RejectVerdict if any guardrail rejects the call; empty
     * Optional if all pass.
     */
    public Optional<RejectVerdict> checkBeforeDispatch(
            Skill activeSkill,
            ToolCall call,
            DispatchContext context);

    /**
     * Invoked BEFORE outcome persistence (record_outcome dispatch).
     * Same shape as above; some guardrails fire only on outcome
     * persistence (must_cite_source).
     */
    public Optional<RejectVerdict> checkBeforeOutcomePersist(
            Skill activeSkill,
            String outcomeClass,
            DispatchContext context);
}

public record RejectVerdict(
    String predicateName,      // canonical label (e.g., s1_citation_presence_required)
    String hint,               // LLM-visible message
    Map<String, Object> trace  // trace fields (predicate-input data surface)
) {}

public record DispatchContext(
    PhasePlan plan,
    BotSession session,
    Map<String, Object> accumulatedToolResults,
    String lastLlmRawResponse,    // for must_cite_source citation check
    Optional<String> parsedUserMessage
) {}
```

### 9.2 Composition order on a single tool call / outcome emission

When the active Skill carries multiple `guardrails[]` entries, the
dispatcher walks them in **declaration order** (top-to-bottom in
the Skill YAML). For `resolve_faq_grounded_answer.yaml`, the
order is:

1. `faq_miss_handover_requires_resolve_attempt` (Sprint 6
   migration).
2. `premature_resolve_outcome_guard` (Sprint 11 migration).
3. `must_cite_source` (NEW S1).

The dispatcher uses **short-circuit on first reject** semantics:
the first guardrail that rejects ends the check; subsequent
guardrails on the same call are NOT evaluated. Rationale:

- Matches the existing pattern in `AgentRunLoopImpl` where each
  `shouldRejectXxx` predicate is checked at its own dispatch site
  with its own short-circuit return.
- Avoids cascading hint structures (the LLM gets ONE hint per
  rejection, not a list of hints).
- Predictable for the LLM: the dispatcher's behaviour mirrors a
  natural "one error at a time" debugging flow.

Alternative "all-must-pass with collected reasons" was considered
+ rejected — see §9.5.

### 9.3 Per-tool-call dispatch site routing

The dispatcher fires at the existing dispatch sites in
`AgentRunLoopImpl`, mapped per guardrail type:

| Guardrail type | Dispatch site (Sprint 39 routing) | Pre-migration code (Sprint 39 removes) |
|---|---|---|
| `faq_miss_handover_requires_resolve_attempt` | `request_handover` dispatch site (`AgentRunLoopImpl.java:404-460` current; rewritten in Sprint 39) | `shouldRejectFaqMissHandover` call at line 413 |
| `intake_complete_required` | `request_handover` dispatch site | `shouldRejectIncompleteIntakeHandover` call at line 317 |
| `premature_resolve_outcome_guard` | `record_outcome` dispatch site | `shouldRejectPrematureResolveOutcome` call at line 356 |
| `must_cite_source` | `record_outcome` dispatch site (same as `premature_resolve_outcome_guard`) | NEW Sprint 39 |

The dispatcher integration replaces each per-predicate `if (...) {
reject... }` block at the dispatch sites with a unified call:

```java
// Pre-migration (current code):
if (shouldRejectFaqMissHandover(plan, call, accumulatedToolResults)) {
    // reject with INTAKE_COMPLETE_GUARD_REJECT_REASON ...
}

// Post-Sprint-39:
Optional<RejectVerdict> verdict = skillGuardrailDispatcher
    .checkBeforeDispatch(activeSkill, call, dispatchContext);
if (verdict.isPresent()) {
    // emit reject hint to accumulated_tool_results
    // log trace per verdict.trace() fields
    return; // short-circuit
}
```

### 9.4 Failure-mode trace + LLM-visible message shape

On guardrail reject, the dispatcher emits:

**Trace entry shape** (extends existing Sprint 6/7/11 trace
pattern):

- `predicate_name` — canonical label (e.g.,
  `s1_citation_presence_required`,
  `faq_miss_handover_requires_resolve_attempt`,
  `intake_required_fields_missing_for_intake_complete`,
  `progressive_resolve_record_outcome_premature`).
- `decision_outcome` — `rejected` (M2 default for all five
  guardrail types).
- `predicate_input_data` — the data surface the guardrail
  consulted (e.g., for `must_cite_source`: the user_message text +
  the absence of any `source_id` token; for `intake_complete_required`:
  the missing fields list).
- `reject_reason_label` — same as `predicate_name`; persisted to
  `accumulated_tool_results.<tool>.error` for LLM-visible read.
- `skill_name` — the active Skill name (NEW in M2; allows trace
  reviewers to correlate the rejection with the Skill that declared
  the guardrail).

**LLM-visible accumulated_tool_results entry**:

```json
{
  "tool_name": "record_outcome",
  "error": "s1_citation_presence_required",
  "hint": "The user-facing message must include a source_id citation from a successful resolve_article call before record_outcome with class=resolve can persist. Cite the relevant article in your user_message.",
  "skill_name": "resolve_faq_grounded_answer"
}
```

The LLM sees the entry on the next loop iteration and may either:

- Retry with the citation added (reading
  `accumulated_tool_results.resolve_article.source_id` and citing
  it).
- Escalate via `request_handover(faq_miss_threshold_exceeded)` if
  no viable resolve_article result is available.

This is the existing reject-and-hint pattern preserved from Sprint
6/7/11 — Sprint 39 carries it forward unchanged at the dispatch-
shape level; only the routing changes (per-guardrail-type instead
of per-`shouldRejectXxx`-method).

### 9.5 Rationale

- **Short-circuit on first reject.** Matches existing pattern;
  predictable LLM interaction; simpler trace.
- **One dispatcher class, two public methods
  (`checkBeforeDispatch` + `checkBeforeOutcomePersist`).** Mirrors
  the two existing dispatch surfaces (tool call dispatch +
  outcome persistence); avoids adding a third entry point.
- **Trace entry includes `skill_name`.** Allows post-hoc trace
  review to correlate a rejection with the Skill declaration that
  caused it; supports debugging during M2 rollout.
- **Reject-and-hint preserved** as the only `on_fail` mode for
  M2-scope guardrails (per decision (d) §5.5 Alternative E
  rejecting `observe_only`; per OLD Sprint 36 §4.4 default reject-
  and-hint over `resolve_no_citation` outcome-class downgrade).
- **`SkillGuardrailDispatcher.java` placement under
  `service/runtime/skill/`.** Parallels Skill / SkillRegistry /
  SkillLoader / SkillStateBus location; clean package boundary.

### 9.6 Alternatives considered + why rejected

**Alternative A: all-must-pass with collected reasons (no
short-circuit).** Rejected per §9.2: multiple hints in one
`accumulated_tool_results` entry confuses the LLM; cascading
rejections are not a real failure mode (one root cause is the
norm; the LLM should fix the first issue and re-evaluate).

**Alternative B: dispatcher placed inside `AgentRunLoopImpl` as
adjacent static methods (OLD Sprint 36 D2 default).** Rejected per
§1.2: preserves the AgentRunLoopImpl bloat; dispatcher should be
its own class.

**Alternative C: per-Skill dispatcher (each Skill brings its own
Java SkillGuard class).** Rejected per §8.4 Alternative B: forces
every Skill to have a Java companion; loses YAML-only Skill
simplicity.

**Alternative D: `on_fail: downgrade_reason` for `intake_complete_required`
(OLD Sprint 36 §5.3 Option B).** Defer (OQ): the default Option A
(reject-and-hint) carries forward to Sprint 39; Option B
(canonical-reason downgrade to `incomplete_intake`) is named in
the dispatcher's `on_fail` enum as a future-extensible mode but
NOT used in M2-scope guardrails. Deliver-agent + human revisit at
Sprint 39 planning round per OLD Sprint 36 OQ 7.4 disposition;
surfaced in handoff §7.

**Alternative E: dispatcher as a Spring AOP aspect intercepting
tool calls.** Rejected: hides the dispatch point; harder to
debug; Spring AOP magic is harder to reason about than explicit
dispatcher calls.

### 9.7 §1.7 boundary check

- The dispatcher itself is generic over guardrail types; no per-UC
  branch in the dispatcher body.
- Each guardrail-type implementation is bounded per §8 mapping
  (registry-driven, single-condition checks, no per-UC-pair
  logic).
- The trace + LLM-visible hint structures are uniform across
  guardrail types; no per-UC variation.

§1.7 preserved.

### 9.8 §1.3 / §1.4 boundary check

- **§1.3 preserved.** Dispatcher enforces Runtime-floor only; LLM
  retains §1.3 ownership of next-action / response strategy /
  customer-facing language.
- **§1.4 preserved.** Dispatcher is the Runtime tool dispatch
  layer surface for guardrails; `PhasePlan.allowedTools` continues
  to be enforced by `ToolDispatcher.validateAgainstPlan` (separate
  capability-floor enforcement upstream of the dispatcher).

### 9.9 Tier-0 candidate question — guardrail refusal non-overridability

§12 surfaces this candidate. Statement: "A guardrail refusal from
`SkillGuardrailDispatcher` cannot be overridden by LLM retry
within the same Skill invocation; the LLM must either satisfy the
guardrail condition OR escalate via `request_handover` with a
valid escalation_reason. There is no 'soft refusal' or
'force-through' override path."

This semantic is the load-bearing claim for guardrail safety: if a
future change introduced a "force-through" path (e.g., the LLM
returning a `force_override: true` field), the Runtime-floor
invariant would become voidable, defeating the §1.4 enforcement.
Tier-0 status would freeze the non-overridability claim.

Default recommendation: DEFER (per `feedback_constitution_discipline_vs_planning_anticipation.md`,
governance should not be edited speculatively before observing
the dispatcher in production traces). Deliver-agent + human +
Codex evaluate at Sprint 37 close.

### 9.10 Downstream sub-sprint reference

Decision (h) is implemented in **Sprint 39**. Sprint 39 ships:

- `SkillGuardrailDispatcher.java` per §9.1 public surface.
- All five guardrail type implementations (per §9.3 table).
- Dispatcher integration replacing the three existing dispatch
  sites in `AgentRunLoopImpl.java` (lines 317, 356, 413).
- Unit tests: `SkillGuardrailDispatcherTest` (each guardrail
  type's fire / no-fire scenarios; short-circuit semantics;
  trace entry shape).

## 10. Decision (i) — session-level state model + per-Skill `state_inheritance`

### 10.1 Decision statement

A new Java class `SkillStateBus` at
`server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillStateBus.java`
mediates session-level state across Skill boundaries on UC
switch. Each Skill carries a `state_inheritance` declaration in
its YAML (per decision (a) §2.1 + §10.2 below) that names what
state the Skill inherits, resets, or sees as soft signal from the
prior Skill.

UC switch detection rides on existing M1 Sprint 32
`alternate_candidate_use_cases` projection + M1 Sprint 33
`discover_disambiguation_signals` projection per M2 §6 #5 fence
(no new classifier).

### 10.2 `state_inheritance` declaration schema

Each Skill's `state_inheritance` field is an object with three
keys:

```yaml
state_inheritance:
  inherit: [<state key list>]                 # carried verbatim from prior Skill
  reset: [<state key list>]                   # dropped at the Skill switch boundary
  soft_signal_via_projection: [<slot name list>]  # projected as observable signal; LLM owns next step
```

**Allowed state keys** (schema-validated at Skill load time per
decision (a) §2.2; new keys require Skill schema + SkillStateBus
update):

- `customer_context` — corresponds to existing
  `BotSession.customerContext` (persisted DB column).
- `accumulated_tool_results` — corresponds to per-loop
  `AgentRunLoopImpl:153` accumulator (in-memory; persisted to
  trace at session close).
- `intake_fields_partial` — corresponds to
  `BotSession.intakeFields` (JSONB); the per-UC required-field
  subset survives if the canonical field name appears in the new
  UC's required-fields set (registry-level intersection check via
  `IntakeFieldsRegistry.canonicalFieldName` +
  `requiredFieldsFor(newUc)` per OLD Sprint 36 D5 6.1.C carried
  forward).

**Allowed projection slot names** for `soft_signal_via_projection`:

- `alternate_candidate_use_cases` — existing M1 Sprint 32 slot in
  `ContextProjectionBuilder.java:396-416`.
- `discover_disambiguation_signals` — existing M1 Sprint 33 slot
  in `ContextProjectionBuilder.java:418-432`.
- `prior_use_case_carry` — NEW M2 Sprint 41 slot; surfaces prior
  Skill identifier + prior citations as soft signal. See §10.4.

The schema is INTENTIONALLY narrow at M2: three state keys + three
projection slots. New entries require explicit deliver-agent +
human review at the M3+ planning round; this prevents speculative
state-bus expansion.

### 10.3 SkillStateBus semantics

The bus is invoked by `PhaseEvaluator` (or `ControlKernel`; Sprint
41 dev judgement) at the moment a Skill switch is detected:

```java
@Component
public class SkillStateBus {
    /**
     * Applied at Skill switch boundary. Reads the new Skill's
     * state_inheritance declaration; for each dimension:
     *  - `inherit`: no-op (state passes through unchanged).
     *  - `reset`: clear the state in the session.
     *  - `soft_signal_via_projection`: ensure ContextProjectionBuilder
     *    includes the named slot on the next turn.
     */
    public void applyOnSkillSwitch(Skill priorSkill,
                                  Skill newSkill,
                                  BotSession session);

    /**
     * Diagnostic: returns the inheritance dimensions for a Skill.
     */
    public StateInheritance inheritanceFor(Skill skill);
}
```

**Registry-level intersection check for `intake_fields_partial`**:
the bus applies the intersection rule per OLD Sprint 36 D5 §6.1.C
(carried forward):

```java
// For each canonical field key in session.intakeFields:
//   if IntakeFieldsRegistry.requiredFieldsFor(newSkill's UC scope)
//      .contains(canonicalFieldName(key)):
//     preserve the field
//   else:
//     drop the field
```

The intersection is a SINGLE rule (registry-driven); NOT a per-UC-
pair branch table. The bus body has no `if (oldUc, newUc) ...`
logic.

UC switch trigger conditions: the bus is invoked when
`PhaseEvaluator.plan(...)` (or upstream `ControlKernel.applyRerouteDecision`
per Sprint 10 §L1) detects `session.getActiveUseCase()` has
changed since the prior turn AND the new `select(phase, newUc)`
returns a different Skill from the prior turn's active Skill.
The detection rides on existing M1 surfaces; no new classifier
introduced.

### 10.4 NEW `prior_use_case_carry` projection slot

Sprint 41 adds a new projection slot in `ContextProjectionBuilder.java`
(sibling to existing Sprint 31 `alternate_candidate_use_cases` slot
at lines 396-416 and Sprint 33 `discover_disambiguation_signals`
slot at lines 418-432). The slot surfaces continuity state as soft
signal:

```json
{
  "prior_use_case_carry": {
    "prior_citations": [
      {"source_id": "ka41r000000LIEXAAY", "from_use_case": "UC-A", "turn_index": 3},
      {"source_id": "ka41r000000LIFJAA4", "from_use_case": "UC-A", "turn_index": 5}
    ],
    "prior_active_use_case": "UC-A",
    "prior_skill_name": "resolve_faq_grounded_answer",
    "ages_out_after_turns": 4
  }
}
```

**Slot semantics** (per OLD Sprint 36 D5 §6.6 carried forward):

- Cap: 3 most recent citations (default; OLD Sprint 36 OQ 7.7
  carried forward; revisit at Sprint 41 planning round).
- Aging: drop after 4 turns (default; OLD Sprint 36 OQ 7.7
  carried forward).
- Empty / null when no prior UC switch has occurred OR when the
  aging-out window has elapsed.
- Skills declare consumption via
  `soft_signal_via_projection: [prior_use_case_carry]`.

LLM-owned read: the LLM sees the slot's value on the next turn
and judges whether to surface continuity to the user, ask a
clarifying question about whether to carry context, OR proceed
without reference. Per §1.3, the runtime does NOT enforce action
on the slot value.

### 10.5 UC-switching invariant matrix (carried forward from OLD Sprint 36 D5)

The 5-row × 5-column matrix from OLD Sprint 36 §6.3 maps directly
into per-Skill `state_inheritance` declarations:

| Dimension | OLD Sprint 36 behaviour | M2 declaration shape |
|---|---|---|
| `accumulated_tool_results` | **S** (survives unconditionally) | All resolve-side Skills: `inherit: [accumulated_tool_results]` |
| `customer_context` | **S** (survives) | All Skills: `inherit: [customer_context]` |
| `intake_fields_partial` | **C** (survives if compatible, registry intersection) | Resolve-INTAKE Skill: `inherit: [intake_fields_partial]` with bus applying intersection rule; other Skills: NOT declared (default reset) |
| Prior citations / grounding-history | **N** (NEW soft-signal projection slot `prior_use_case_carry`) | Resolve-side Skills: `soft_signal_via_projection: [prior_use_case_carry]` |
| Skill terminal predicate state | **L** (scoped to current UC only) | NOT a state-bus dimension; per-dispatch, not session-state. NO declaration in `state_inheritance` (the bus doesn't track predicate state). |

The matrix preserves OLD Sprint 36's principle-level shape (one
rule per row; registry-driven intersection for the C case; no
per-UC-pair branch).

### 10.6 Rationale

- **Declarative `state_inheritance` per Skill.** Each Skill names
  its state posture explicitly; reviewable; testable.
- **Narrow schema (3 state keys + 3 projection slots).** Prevents
  speculative state-bus expansion; new dimensions require deliver-
  agent + human review.
- **Registry-level intersection for `intake_fields_partial`.**
  Honors §1.7 (no per-UC-pair branch); leverages existing
  `IntakeFieldsRegistry` registry lookups.
- **`prior_use_case_carry` as a NEW soft-signal projection slot.**
  Mirrors existing Sprint 31 / Sprint 33 slot pattern; LLM-owned
  read; no Runtime enforcement.
- **UC switch detection on existing surfaces.** Honors M2 §6 #5
  fence (no new classifier); rides on the M1 surfaces.
- **`SkillStateBus.java` placement under `service/runtime/skill/`.**
  Parallels other M2 NEW classes.

### 10.7 Alternatives considered + why rejected

**Alternative A: monolithic "all state always survives" rule
(no per-Skill `state_inheritance`).** Rejected per OLD Sprint 36
§6.3 carried forward: `intake_fields_partial` is per-UC-required;
preserving fields not relevant to the new UC pollutes the new
Skill's state.

**Alternative B: per-UC-pair `state_inheritance` tables (e.g.,
`if old_uc==UC-A and new_uc==UC-C: inherit X`).** Rejected per
§1.7: per-UC-pair branch tables are forbidden. The intersection
rule is principle-level + registry-driven.

**Alternative C: state-bus enforces Runtime-floor on
`state_inheritance` declarations (e.g., a Skill CANNOT `inherit`
`prior_citations` unless its `applicable_use_cases` includes the
prior UC).** Rejected: over-constrains the declarations; the
declaration shape is configuration data, not enforcement; the bus
applies declarations as written.

**Alternative D: pure-projection design (no Java state-bus; all
state inheritance is via projection slots that the LLM reads and
chooses to carry).** Rejected: `intake_fields_partial` carry-over
needs deterministic registry-intersection enforcement (the LLM
can't be relied on to filter intake fields correctly per registry
requirements); pure projection would weaken the capability-floor
invariant. The hybrid is the cleaner fit.

**Alternative E: `state_inheritance` as a Java method per Skill
(no YAML field).** Rejected per §8.4 Alternative B: forces Java
companion class per Skill; loses YAML simplicity.

### 10.8 §1.7 boundary check

- `state_inheritance` declarations are principle-level: each Skill
  names INHERIT / RESET / SOFT_SIGNAL dimensions. No per-UC-pair
  branch.
- `SkillStateBus.applyOnSkillSwitch` uses REGISTRY-DRIVEN
  intersection (`IntakeFieldsRegistry`) for the C-case dimension;
  no per-UC-pair logic.
- `prior_use_case_carry` slot construction is REGISTRY-DRIVEN
  (reads prior citations from accumulated trace; aging window is a
  single integer constant per OQ 10.4); no per-UC variation.

§1.7 preserved.

### 10.9 §1.3 / §1.4 boundary check

- **§1.3 preserved.** `prior_use_case_carry` is a soft-signal
  projection; LLM owns the read decision (surface to user / ask /
  ignore). State-bus enforcement on `inherit` / `reset` is
  CAPABILITY-FLOOR enforcement (which fields carry over), NOT
  semantic decision enforcement.
- **§1.4 preserved.** State-bus is Runtime-owned state-management
  infrastructure per §1.4 ("persistence"); the bus carries +
  filters session state without making semantic decisions.

### 10.10 Tier-0 candidate question — state-bus boundary enforcement

§12 surfaces this candidate. Statement: "When PhaseEvaluator
selects a new Skill on UC switch, SkillStateBus applies the new
Skill's `state_inheritance` declaration unconditionally; the LLM
cannot 'inherit' or 'override' state the new Skill declares as
`reset`. The bus is the single enforcement point for cross-Skill
state preservation."

Tier-0 status would freeze the bus's enforcement role: a future
change cannot bypass the bus on Skill switch (e.g., to allow per-
turn intake-field copying outside the registry intersection
rule). Default recommendation: DEFER (the bus is new in Sprint 41;
observe production behaviour before elevating).

### 10.11 Downstream sub-sprint reference

Decision (i) is implemented in **Sprint 41** (UC switch + state
preservation across Skill boundary). Sprint 41 ships:

- `Skill.java` EXTEND: add `state_inheritance` field.
- All 6 Skill YAMLs EXTEND: each declares its `state_inheritance`
  block per §10.5 matrix.
- `SkillStateBus.java` NEW per §10.3 public surface.
- `ContextProjectionBuilder.java` EXTEND: add `prior_use_case_carry`
  slot per §10.4 shape.
- `BotSession.java` (or equivalent session-state holder) +
  `PhaseEvaluator.java` (or `ControlKernel.java` per dev judgement)
  EDIT: integrate SkillStateBus on Skill change.
- Unit tests: `SkillStateBusTest`, `UcSwitchStateInheritanceTest`,
  `PriorUseCaseCarryProjectionTest`, integration tests for
  representative UC switch scenarios.

## 11. Decision (j) — §4.1 nine-question anti-hardcode kernel walk

This section walks the nine §4.1 anti-hardcode kernel questions
(verbatim from `iteration_governance.md` §4.1) against the
PROPOSED design above (decisions (a)-(i)). The walk verifies the
design honors Constitution §1.3 / §1.4 / §1.7 BEFORE implementation.

### 11.1 Q1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision?

**Design's answer: NO.** Walking each design surface:

- **(a) Skill data model fields.** `applicable_use_cases` and
  `applicable_phases` are CAPABILITY-SCOPE enums (when does this
  Skill apply?), NOT semantic decisions. Per decision (a) §2.5 +
  decision (e) §6.7. Verdict: NOT a semantic hardcode.
- **(b) SkillRegistry shape.** Infrastructure (registry +
  loader); no semantic decisions in the registry body. Per decision
  (b) §3.6. Verdict: not implicated.
- **(c) PhaseEvaluator integration.** One new `if (skill != null)`
  routing check; template-substitution placeholders are
  registry-driven. Per decision (c) §4.6. Verdict: NOT a semantic
  hardcode.
- **(d) procedure vs guardrails split.** `procedure` is LLM-soft
  principle-level; `guardrails` are declarative typed objects
  whose parameters are registry-driven or single-condition
  bounded. Per decision (d) §5.6. Verdict: NOT a semantic
  hardcode.
- **(e) phase YAML migration.** Each Skill `procedure` is
  principle-level; template placeholders for per-UC entity names
  are registry-driven (no per-UC branch in procedure text). Per
  decision (e) §6.7. Verdict: NOT a semantic hardcode.
- **(f) teaching paragraph migration.** Sprint 31 + Sprint 33
  paragraphs migrate principle-level into DISCOVER Skill
  `procedure`; orchestration shell keeps Sprint 23 + universal
  norms. No per-UC-pair if-else. Per decision (f) §7.6. Verdict:
  NOT a semantic hardcode.
- **(g) predicate migration.** All four guardrails (three
  migrated + one NEW S1) are registry-driven, single-condition,
  scope-bounded. Per decision (g) §8.5. The S1 `must_cite_source`
  scope is bounded per §6 #4 verbatim authorization. Verdict: NOT
  a semantic hardcode.
- **(h) unified dispatcher.** Generic dispatcher class; per-
  guardrail-type implementations are bounded per (g); no per-UC
  branch in dispatcher body. Per decision (h) §9.7. Verdict: NOT
  a semantic hardcode.
- **(i) state_inheritance declarations.** Principle-level per-
  Skill INHERIT / RESET / SOFT_SIGNAL dimensions; registry-driven
  intersection for the C-case (`intake_fields_partial`). Per
  decision (i) §10.8. Verdict: NOT a semantic hardcode.

**Q1 verdict: NO** — the proposed design does NOT add a keyword /
regex / if-else / enum / per-UC matrix for a semantic decision.

### 11.2 Q2. If yes to (1), is the change justified as protecting a current Tier-0 invariant?

**Q1 is NO, so Q2 is N/A.** However, the design surfaces two
Tier-0 candidate questions (per decision (h) §9.9 +
decision (i) §10.10):

- Guardrail refusal non-overridability.
- State-bus boundary enforcement.

Plus two from OLD Sprint 36 carried forward (per §1.2):

- S1 `must_cite_source` predicate semantics.
- S2 `intake_complete_required` predicate semantics (already in
  production since Sprint 7 §I2; M2 only relocates).

Plus one inherited invariant candidate:

- Skill tool-whitelist enforcement unconditional (per existing
  `PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`
  invariant, inherited by Skill abstraction).

See §12 for enumeration + verdict per candidate.

### 11.3 Q3. Could the same outcome be achieved by projecting a soft signal to the LLM?

**Partially YES; partially NO.**

- **State-bus on UC switch (decision (i)):** the bus has TWO
  enforcement dimensions:
  - INHERIT / RESET on `customer_context`, `accumulated_tool_results`,
    `intake_fields_partial` — Runtime-floor enforcement (capability/
    permission boundary; the LLM cannot "inherit" intake fields
    not relevant to the new UC). Pure-projection would not enforce
    this; rejected per decision (i) §10.7 Alternative D.
  - `soft_signal_via_projection` on `prior_use_case_carry` — IS a
    soft signal. The LLM owns the read decision; runtime does NOT
    enforce surface-to-user. Per decision (i) §10.4.

  The design correctly uses soft-signal where appropriate (prior
  citations / grounding history) and Runtime-enforcement where
  appropriate (registry-intersection on intake_fields_partial per
  §1.4 capability floor).

- **S1 `must_cite_source` predicate (decision (g)):** could it be
  pure-projection (a `citation_required` slot the LLM reads)?
  Answer: NO at the terminal-predicate level. Per Constitution
  §1.4 + §6 #4 verbatim authorization, the grounding-floor
  invariant ("the runtime SHALL NOT persist a fabricated FAQ
  resolution without source_id citation") IS a Runtime-owned
  Java guard. The envelope teaching in `procedure` IS the soft
  surface; the guardrail is the hard backstop. This is the
  bounded inversion of `D-hard-citation-gate`.

- **S2 `intake_complete_required` predicate:** same reasoning;
  the capability-floor invariant ("the runtime SHALL NOT persist
  an intake-complete handover when required fields are missing")
  is Runtime-owned per §1.4. Sprint 7 §I2 already ships this; M2
  only relocates.

- **Sprint 6 + Sprint 11 migrated predicates:** same; both protect
  Runtime-floor invariants per §1.4.

**Q3 verdict: PARTIALLY YES (state-bus soft-signal dimensions);
PARTIALLY NO (terminal predicates are §1.4 hard surfaces per
verbatim authorizations + frozen Sprint 6/7/11 precedents).** The
design correctly uses soft-signal where appropriate and Runtime-
enforcement where appropriate.

### 11.4 Q4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id?

**Design's answer: NO.** Walking each surface:

- The design doc references the Codex M1 Finding 1 Alice
  fabrication shape in prose (e.g., decision (g) §8.2.4 references
  the failure shape S1 prevents) but does NOT encode CaseSpec ids
  (Alice's CaseSpec id, any `cs_interactive_*` id) into runtime /
  prompt / judge.
- Skill `procedure` texts are universally applicable across UCs
  the Skill scopes to; no per-trace phrasing.
- `must_cite_source` guardrail checks for `source_id` token
  presence — a SCHEMA-level check, not a specific article-slug
  match. The predicate does NOT match any user-message text.
- `intake_complete_required` guardrail checks `IntakeFieldsRegistry.intakeComplete(...)` — registry-driven, not text-driven.
- No CaseSpec id appears in any Skill YAML, in `SkillGuardrailDispatcher.java`,
  or in `SkillStateBus.java`.

**Q4 verdict: NO** — no visible-eval case text / trace phrasing /
CaseSpec id encoded.

### 11.5 Q5. Does the change move semantic ownership from the LLM to Java — that is, shrink what §1.3 says the LLM owns?

**Design's answer: NO.** §1.3 names LLM-owned: user goal, issue
relation, use case hypothesis, drift / topic shift, next action,
escalation posture, response strategy, natural customer-facing
wording. Walking the design:

- **`procedure` / `grounding_instruction` / `escalation_policy`
  are LLM-soft (decision (d)).** The LLM continues to own per-step
  argument choice, recommended-order deviation, customer-facing
  language, UC hypothesis, drift detection, response strategy
  within the envelope.
- **`guardrails` enforce Runtime-floor invariants only (decision
  (d) §5 + decision (g) §8).** None of the four migrated
  guardrails (Sprint 6 / 7 / 11) enforces an LLM-owned semantic
  decision. NEW S1 `must_cite_source` enforces grounding-floor
  citation presence — a §1.4 Runtime-owned floor invariant per the
  verbatim §6 #4 authorization; NOT a semantic decision.
- **`state_inheritance` (decision (i)):** state-bus enforces
  capability-floor (registry-intersection on intake fields); LLM
  retains §1.3 ownership of how to use carried state (surface to
  user / ignore / ask).
- **Tool whitelist (`tools_required` → `PhasePlan.allowedTools`)
  unchanged from pre-M2 enforcement.**

**Q5 verdict: NO** — semantic ownership stays LLM-owned per §1.3;
the design extends Runtime-floor enforcement (§1.4) narrowly per
verbatim authorizations.

### 11.6 Q6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?

**Design's answer: NO.** Each Skill `procedure` is principle-level:

- DISCOVER `procedure`: principle-level "identify the UC; ask one
  clarifying question if unclear" + Sprint 7 §I0 weak-candidate
  cue (principle-level observable-state guidance) + Sprint 33
  ad-status disambiguation cue (principle-level
  observable-state guidance about the projection slot signals).
- CONFIRM `procedure`: principle-level satisfaction-interpretation.
- ESCALATE `procedure`: principle-level handover guidance.
- TERMINAL `procedure`: principle-level closing.
- RESOLVE_INTAKE `procedure`: principle-level field-by-field
  collection with template-substitution placeholders
  (registry-driven, not branch logic).
- RESOLVE_FAQ `procedure`: principle-level grounded resolve
  sequence with the S1 sequence teaching.

The orchestration shell (`system_prompt.txt` post-Sprint-40)
contains the cross-Skill universal norms + the canonical
escalation_reason decision tree (lines 54-101 stays per decision
(f) §7.3). The decision tree IS structured as a decision walk
("USER-EXPLICIT REQUESTS: ..."; "DISTRESS / SAFETY: ..."; etc.),
but this is CROSS-CATEGORY guidance for an LLM choosing among the
canonical 23-value enum — NOT per-UC-pair if-else. The decision
tree teaches the LLM how to map a *user situation* to one
canonical reason; the LLM owns the situation interpretation.

**Q6 verdict: NO** — principle-level guidance throughout; no
per-UC-pair if-else.

### 11.7 Q7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?

**Design's answer: YES** with one principled extension:

- **Tool schema unchanged.** No new tools; no tool field changes.
- **Capability / permission boundary preserved.**
  `PhasePlan.allowedTools` continues to enforce; Skill
  `tools_required` is the NEW source for the tool whitelist values
  (cleanly composed at PhaseEvaluator integration per decision
  (c)).
- **PII / safety floor preserved.** ControlKernel unchanged; PII /
  safety surfaces are pre-Skill-abstraction surfaces (per Sprint
  11 / 12 frozen surface).
- **Grounding floor preserved + NARROWLY extended.** The S1
  `must_cite_source` guardrail extends the existing
  `L1:source_citation_present` boundary (currently observation-
  only per `faq_grounding_contract.md`) from observation to
  dispatch-time guard. Extension is BOUNDED per §6 #4 verbatim
  authorization (RESOLVE_FAQ phase + record_outcome(class=resolve)
  + citation presence only). The generic-Java-citation-gate
  deferral (`D-hard-citation-gate`) is preserved.

**Q7 verdict: YES** — all four floors preserved; grounding floor
narrowly extended per verbatim authorization.

### 11.8 Q8. Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases?

**Sprint 37 ships zero behaviour change** (docs-only design
freeze). Generalization eval coverage is PROSPECTIVE for
Sprints 38 / 39 / 40 / 41 per Sprint 37 §8 stanza:

- **Sprint 38** (Registry core + 4 phases): Target = behavioural
  equivalence on 4 migrated phase Skills (DISCOVER + CONFIRM +
  ESCALATE + TERMINAL) for representative UCs. Neighbor =
  cross-UC variants within each phase. Negative = phases
  NOT yet migrated (RESOLVE_FAQ + RESOLVE_INTAKE behaviour
  preserved via legacy fallback). Shadow = deliver-agent + Codex
  at sub-sprint close.
- **Sprint 39** (RESOLVE + predicates + S1/S2 + dispatcher):
  Target = behavioural equivalence on RESOLVE_FAQ + RESOLVE_INTAKE
  Skills + 3 migrated predicates + S1/S2 new predicates;
  Sprint71PartialIntakePersistenceTest 14/14 preserved. Neighbor =
  UCs within FAQ-path / INTAKE-path. Negative = predicates do NOT
  fire on out-of-scope tool calls (e.g., `must_cite_source` does
  NOT fire on `record_outcome(class=escalate/abandon)`). Shadow =
  deliver-agent + Codex.
- **Sprint 40** (teaching extraction + shell cleanup): Target =
  3 teaching paragraph migrations behavioural-equivalence on
  representative DISCOVER + RESOLVE traces. Neighbor = unchanged
  cross-Skill universal norms. Negative = removed paragraphs
  do NOT regress LLM behaviour (real-LLM integration test).
- **Sprint 41** (UC switch + state preservation): Target =
  synthetic UC-switching trace (e.g., UC-A → UC-C mid-flow per
  OLD Sprint 36 §6.4). Neighbor = other UC-pair transitions per
  §10.5 matrix. Negative = single-UC traces (no switch; state-bus
  must NOT trigger spurious carry-over). Shadow = deliver-agent +
  Codex.

**Q8 verdict: N/A for Sprint 37 itself; PROSPECTIVE coverage
specified for Sprints 38-41 per §8 stanza.**

### 11.9 Q9. If the change is temporary, does it carry an explicit rollback or sunset plan?

**Design's answer: N/A.** The Skill Registry abstraction is NOT
temporary; it's a long-term architectural shift. Individual Skill
content may be tuned in M3-Skill-Tuning (per
`docs/milestone_objective.md` §11 cross-milestone context); the
abstraction itself is permanent. No sunset.

### 11.10 Walk verdict

**`approve`** — the proposed design honors Constitution §1.3 /
§1.4 / §1.7 boundaries. No semantic hardcode introduced. Tier-0
candidate questions surfaced in §12 for human-review escalation
per Sprint 37 §10 stop condition #2 (default recommendation:
DEFER per `feedback_constitution_discipline_vs_planning_anticipation.md`).
Codex per-sub-sprint review at Sprint 37 close verifies
independently.

## 12. Tier-0 candidate enumeration

Sprint 37 §10 stop condition #2 requires that any Tier-0
invariant candidate surfaced from §11 (decision (j)) §4.1 walk is
enumerated here with a qualification verdict. Five candidates are
enumerated below. Default recommendation across all five is
**DEFER** per `feedback_constitution_discipline_vs_planning_anticipation.md`
("constitution-discipline review at execution time"): do NOT
edit `runtime_freeze_and_risk_policy.md` in Sprint 37 dev commit;
deliver-agent + human + Codex evaluate at Sprint 37 close BEFORE
Sprint 38 begins.

No separate Tier-0 candidate write-up file is filed in Sprint 37
dev commit because every candidate's recommendation is DEFER.
Surface in handoff §7 OQ for deliver-agent + human + Codex
evaluation at Sprint 37 close per Sprint 37 §10 stop condition #2.

### 12.1 Candidate C1 — Skill tool-whitelist enforcement unconditional

**Statement.** "The tool whitelist composed from `skill.tools_required`
is enforced by `PhasePlan.allowedTools` via the existing
`ToolDispatcher.validateAgainstPlan` invariant; an LLM tool call
outside the whitelist is rejected unconditionally regardless of
any soft `procedure` guidance."

**Qualification verdict: REJECTED as new Tier-0 candidate.**

**Reasoning.** This is an EXISTING runtime guarantee from pre-M2
`PhasePlan.allowedTools` + `ToolDispatcher.validateAgainstPlan`.
The Skill abstraction INHERITS the invariant (the Skill is the new
SOURCE for the tool list values; the enforcement mechanism is
unchanged). Formalizing it as Tier-0 would not add new freezing —
it would only re-state what `runtime_freeze_and_risk_policy.md`
implicitly already covers under §1.4 capability boundary.

**Implication for Sprint 38+:** none. Sprint 38 implementation
preserves the existing enforcement semantics; no governance edit
required.

### 12.2 Candidate C2 — Skill terminal predicate refusal non-overridable by LLM

**Statement.** "A guardrail refusal from `SkillGuardrailDispatcher`
cannot be overridden by LLM retry within the same Skill invocation;
the LLM must either satisfy the guardrail condition OR escalate
via `request_handover` with a valid escalation_reason. There is
no 'soft refusal' or 'force-through' override path."

**Qualification verdict: QUALIFIED CANDIDATE; recommendation
DEFER.**

**Reasoning.** The dispatcher is NEW in Sprint 39; the non-
overridability claim is a NEW runtime guarantee that Sprint 39
implementation establishes by construction (the dispatcher has no
override mechanism; per decision (h) §9.4 there is no
`force_override` field in any tool schema). A future change could
introduce a "force-through" path that defeats the §1.4 grounding-
floor / capability-floor invariants the dispatcher enforces. Tier-0
status would freeze the non-overridability claim, preventing
silent erosion.

**Why DEFER (default recommendation).** Per
`feedback_constitution_discipline_vs_planning_anticipation.md`,
governance is principle-teaching; current-state findings belong
in `action_bank.md`. The non-overridability semantics is BOTH:

- A principle (Runtime-floor enforcement should not be voidable by
  LLM retry).
- A current-state observation (the Sprint 39 dispatcher has no
  override path by construction).

The principle could be folded into `runtime_freeze_and_risk_policy.md`
§2 (the seven-rule runtime contract) as an eighth rule. But the
specific phrasing (which dispatcher, which override paths are
prohibited) is current-state; should NOT pre-commit before
observing Sprint 39 implementation in production traces.

**Implication if ELEVATED (deliver-agent + human + Codex choice
at Sprint 37 close):** Sprint 39 §7 stanza Tier-0 invariant
line would cite the new invariant id. Sprint 39 Codex review
verifies the dispatcher has no override path AND the YAML schema
forbids `on_fail: force_override` (which is NOT in the v1
schema; would prevent future-PR drift).

**Implication if DEFERRED (default):** Sprint 39 §7 stanza
declares "no new Tier-0 invariant"; the dispatcher non-overridability
is preserved by construction; the candidate stays open as
`R-skill-guardrail-non-overridability-tier-0` in `action_bank.md`
for M3+ revisit. Sprint 39 Codex review verifies no override path
exists in the dispatcher implementation; the verification surface
is the same regardless of Tier-0 status.

### 12.3 Candidate C3 — Skill `state_inheritance` enforced at session-state-bus boundary

**Statement.** "When PhaseEvaluator selects a new Skill on UC
switch, SkillStateBus applies the new Skill's `state_inheritance`
declaration unconditionally; the LLM cannot 'inherit' or
'override' state the new Skill declares as `reset`. The bus is
the single enforcement point for cross-Skill state preservation."

**Qualification verdict: QUALIFIED CANDIDATE; recommendation
DEFER.**

**Reasoning.** Same shape as C2: SkillStateBus is NEW in Sprint
41; the boundary-enforcement claim is a NEW runtime guarantee
that Sprint 41 implementation establishes. A future change could
bypass the bus on Skill switch (e.g., to allow per-turn intake-
field copying outside the registry-intersection rule). Tier-0
status would freeze the bus's enforcement role.

**Why DEFER.** Same reasoning as C2. The principle is
load-bearing; the implementation surface is Sprint 41 new code.
Pre-committing the Tier-0 status before observing the bus's
behaviour in production traces is speculative. Carry as
`R-skill-state-bus-boundary-enforcement-tier-0` in
`action_bank.md` for M3+ revisit.

**Implication if ELEVATED:** Sprint 41 §7 stanza cites the new
invariant id; Codex Sprint 41 review verifies the bus is the
single enforcement point for `state_inheritance`.

**Implication if DEFERRED (default):** Sprint 41 §7 stanza
declares "no new Tier-0 invariant"; the bus enforcement is
preserved by construction; Sprint 41 Codex review verifies
independently.

### 12.4 Candidate C4 — S1 `must_cite_source` predicate semantics

**Statement.** "The S1 `must_cite_source` guardrail fires ONLY on
(phase=RESOLVE_FAQ, tool_call=record_outcome with class=resolve,
check=source_id citation present in user-facing message);
expansion requires new human authorization."

**Qualification verdict: NOT A TIER-0 CANDIDATE PER M2 §6 #4
BOUNDED INVERSION.**

**Reasoning.** Per the verbatim §6 #4 authorization carried
forward from OLD M2-Skill: "Accept the Skill-bounded exception to
D-hard-citation-gate. This is not a generic Java grounding-citation
gate. It is a narrow S1 terminal predicate... The original
deferral of a generic Java citation gate remains valid."

The bounded inversion is explicitly a Runtime-floor narrow
predicate per §1.4, NOT a Tier-0 structural invariant. The
`D-hard-citation-gate` deferral in `action_bank.md` is preserved
(generic Java citation gate stays deferred); only the narrow S1
predicate is authorized. Elevating S1 semantics to Tier-0 would
risk drifting the §6 #4 narrow bound into a broader Tier-0
invariant — which is precisely what the bounded inversion was
designed to avoid.

OLD Sprint 36 §8.3 enumerated the same candidate with the same
DEFER recommendation; NEW Sprint 37 carries forward verbatim.

**Implication for Sprint 38+:** none. Sprint 39 §7 stanza declares
"no new Tier-0 invariant" for S1; the predicate is a §1.4
Runtime-floor narrow guard per the verbatim authorization. Codex
Sprint 39 review verifies the predicate scope matches §6 #4
verbatim.

### 12.5 Candidate C5 — S2 `intake_complete_required` predicate semantics

**Statement.** "The S2 `intake_complete_required` guardrail fires
ONLY on (phase=RESOLVE_INTAKE, tool_call=request_handover with
escalation_reason matching `intake_complete_for_uc_*`,
check=`IntakeFieldsRegistry.intakeComplete(uc, collected)`)."

**Qualification verdict: NOT A TIER-0 CANDIDATE PER OLD SPRINT 7
§I2 PRECEDENT.**

**Reasoning.** The S2 predicate is ALREADY shipped in production
since Sprint 7 §I2 (per OLD Sprint 36 §1.2 material finding,
carried forward into NEW M2 decision (g) §8.2.2). M2's contribution
is moving the predicate from inline Java method
(`AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover`) to
declarative Skill guardrail. No new semantics, no new runtime
guarantee.

The existing `runtime_freeze_and_risk_policy.md` §2 rule 6
("`record_outcome(resolve)` requires disposition + guard
approval") covers a similar Sprint 11 §M1 precedent; the Sprint 7
§I2 predicate is NOT named in the Tier-0 catalogue (the
deliver-agent + human evaluated this in OLD Sprint 36 §8.4 with
DEFER recommendation — the predicate has been in production since
Sprint 7 without urgency to formalize). NEW Sprint 37 carries
forward DEFER.

**Implication for Sprint 38+:** none. Sprint 39 §7 stanza declares
"no new Tier-0 invariant" for S2; the predicate is the existing
Sprint 7 §I2 guard, relocated.

### 12.6 Aggregate verdict

Five Tier-0 candidates enumerated; all recommend **DEFER**:

| Candidate | Verdict | Recommendation |
|---|---|---|
| C1 Skill tool-whitelist enforcement | REJECTED as new candidate (existing invariant) | No action |
| C2 Guardrail refusal non-overridability | QUALIFIED | DEFER; carry as `R-skill-guardrail-non-overridability-tier-0` |
| C3 State-bus boundary enforcement | QUALIFIED | DEFER; carry as `R-skill-state-bus-boundary-enforcement-tier-0` |
| C4 S1 `must_cite_source` semantics | NOT A CANDIDATE per §6 #4 bounded inversion | No action; preserve OLD Sprint 36 §8.3 DEFER |
| C5 S2 `intake_complete_required` semantics | NOT A CANDIDATE per Sprint 7 §I2 precedent | No action; preserve OLD Sprint 36 §8.4 DEFER |

**No separate Tier-0 candidate write-up file is filed in Sprint
37 dev commit** because no candidate's recommendation is
QUALIFIED-AND-ELEVATE. Sprint 37 dev surfaces the enumeration in
handoff §7 OQ for deliver-agent + human + Codex evaluation at
Sprint 37 close per Sprint 37 §10 stop condition #2.

If deliver-agent + human + Codex evaluate at Sprint 37 close that
C2 OR C3 should be ELEVATED, the deliver-agent commits the
`runtime_freeze_and_risk_policy.md` §1 / §2 addition as a
SEPARATE commit per `feedback_commit_at_end_bundles_deliver_artefacts.md`
BEFORE Sprint 39 / Sprint 41 begins (depending on which is
elevated). NEW Sprint 39 / Sprint 41 §7 stanza Tier-0 invariant
line then cites the added invariant id.

## 13. Downstream sub-sprint reference index + premise refinements

### 13.1 Decision-to-sub-sprint reference index

| Decision | Implemented at | Notes |
|---|---|---|
| (a) Skill data model | Sprint 38 (core fields + schema validation); Sprint 39 (guardrails populated); Sprint 41 (state_inheritance populated) | Cross-sub-sprint; freeze schema is stable post-Sprint-38 |
| (b) SkillRegistry shape | Sprint 38 | SkillRegistry + SkillLoader; select semantics stable post-Sprint-38 |
| (c) PhaseEvaluator integration | Sprint 38 (4 phases); Sprint 39 (2 remaining phases + legacy branch removal) | Backward-compatible PhasePlan data shape |
| (d) procedure vs guardrails responsibility split | Sprint 38 (schema); Sprint 39 (concrete guardrail types implemented) | Field-level separation |
| (e) Phase YAML migration mapping (6 phases) | Sprint 38 (DISCOVER + CONFIRM + ESCALATE + TERMINAL/CLOSE); Sprint 39 (RESOLVE_FAQ + RESOLVE_INTAKE) | Behavioural-equivalence test per phase |
| (f) Sprint 23/31/33 teaching paragraph migration | Sprint 40 | Sprint 31 + Sprint 33 → discover_triage; Sprint 23 stays in shell |
| (g) Sprint 6/7/11 predicate migration | Sprint 39 | + NEW S1 + S2 (existing) into Skill guardrails |
| (h) Unified dispatcher | Sprint 39 | SkillGuardrailDispatcher replaces shouldRejectXxx scattered methods |
| (i) State model + state_inheritance + SkillStateBus + prior_use_case_carry slot | Sprint 41 | UC switch + state preservation |
| (j) §4.1 anti-hardcode kernel walk | Sprint 37 (this file §11) | Tier-0 candidates surfaced in §12 |

### 13.2 Premise refinements surfaced at Sprint 37 dev session

Sprint 37 §4 premises (10 items) verified at HEAD 6d97888 with
two refinements:

**Refinement 1 — `system_prompt.txt` is 101 lines, not ~40-60.**
Sprint 37 §4 premise #7 cited "~40-60 lines pre-M2" but the
verified line count is 101 (HEAD 6d97888). The Sprint 23 / 31 / 33
paragraphs (lines 23-28, 30-34, 36-43 respectively) are present
at the cited approximate locations; but the file ALSO carries:

- Lines 1-22: core rules (bot identity, JSON output contract,
  three required fields, tool dispatch protocol, universal rules).
- Lines 45-52: DISCOVER phase guidance (sibling content to Sprint
  23/31/33 paragraphs; pre-M2 origin unclear from this file
  alone).
- Lines 54-101: `request_handover` escalation_reason canonical-
  enum decision tree (the largest block; cross-Skill universal
  reason-picking teaching).

This refinement is load-bearing for decision (f) §7: the
orchestration shell post-Sprint-40 carries MORE content than the
"thin orchestration shell" framing might suggest. The post-
Sprint-40 shell retains lines 1-22 + 23-28 (Sprint 23
already_called) + NEW Skill envelope mechanics teaching + lines
54-101 (`request_handover` decision tree). Sprint 40 dev judgement
on whether lines 45-52 DISCOVER guidance is migrated into
`discover_triage.yaml` (default recommendation) OR compressed in
shell.

Surfaced in handoff §7 OQ for deliver-agent + human awareness at
Sprint 38+ contract draft rounds (Sprint 40 specifically).

**Refinement 2 — Sprint 33 ad-status disambiguation cue lives in
`PhaseEvaluator.java`, NOT in `system_prompt.txt`.** OLD Sprint
36 §1.2 + handoff §1.2 cite this; verified at HEAD 6d97888 lines
432-448. The current `system_prompt.txt:36-43` paragraph covers
the PROJECTION SLOT description (`discover_disambiguation_signals`
slot's three fields explanation); the per-UC disambiguation cue
("when ad_status_observed is one of REMOVED / SUSPENDED / EXPIRED
AND topic_subject_carries_multiple_candidate_ucs is true, ask ONE
focused clarifying question before committing classify_use_case")
lives in PhaseEvaluator's DISCOVER systemInstruction.

This split is preserved in M2 decision (e) §6.2.1: the cue
content migrates as part of DISCOVER Skill `procedure` at Sprint
38 (along with the rest of PhaseEvaluator's DISCOVER systemInstruction);
the slot description migrates from `system_prompt.txt:36-43` at
Sprint 40 separately. Post-Sprint-40 the two pieces (cue +
slot-description) BOTH live in `discover_triage.yaml` `procedure`.

### 13.3 Open questions deferred to deliver-agent + human

The freeze surfaces these open questions for deliver-agent + human
decision at Sprint 37 close OR at subsequent sub-sprint planning
rounds (per OLD Sprint 36 §8 carried forward + NEW M2 surface
issues):

1. **OQ-13.3.1** — CLOSE → TERMINAL phase enum rename: ship
   `terminal.yaml` with `applicable_phases: [CLOSE]` (default
   recommendation; phase enum unchanged) OR rename phase enum at
   Sprint 38 (touches runtime phase machine; riskier). Per §6.1
   note.
2. **OQ-13.3.2** — S1 `must_cite_source` Tier-0 elevation:
   QUALIFIED-CANDIDATE per OLD Sprint 36 §8.3 OR not-a-candidate
   per M2 §6 #4 bounded inversion. NEW Sprint 37 §12.4 verdict:
   not a Tier-0 candidate. Codex verifies at Sprint 37 close.
3. **OQ-13.3.3** — S2 `intake_complete_required` Tier-0 elevation:
   per OLD Sprint 36 §8.4 DEFER (predicate in production since
   Sprint 7; no urgency). NEW Sprint 37 §12.5 verdict: not a
   Tier-0 candidate. Codex verifies.
4. **OQ-13.3.4** — Guardrail refusal non-overridability Tier-0
   elevation: NEW candidate from M2 abstraction; DEFER per
   `feedback_constitution_discipline_vs_planning_anticipation.md`.
   NEW Sprint 37 §12.2 + handoff §6.
5. **OQ-13.3.5** — State-bus boundary enforcement Tier-0
   elevation: NEW candidate from M2 abstraction; DEFER. NEW
   Sprint 37 §12.3 + handoff §6.
6. **OQ-13.3.6** — Guardrail `on_fail: downgrade_reason` mode for
   `intake_complete_required` (OLD Sprint 36 §5.3 Option B):
   Default Option A (reject-and-hint) carries forward to Sprint
   39; Option B remains a future-extensible mode in the
   dispatcher's enum but NOT used in M2-scope guardrails. Deliver-
   agent + human revisit at Sprint 39 planning round.
7. **OQ-13.3.7** — Sprint 31 `alternate_candidate_use_cases`
   teaching duplication: place only in `discover_triage.yaml`
   (default) OR copy to `resolve_faq_grounded_answer.yaml` +
   `resolve_intake_collect_and_handover.yaml` (mid-session topic-
   shift detection)? Default per §7.2.2: only DISCOVER + rely on
   `prior_use_case_carry` for resolve-side carry. Deliver-agent
   + human revisit at Sprint 40 / 41 planning round.
8. **OQ-13.3.8** — `prior_use_case_carry` slot cap (3 default) +
   aging (4 turns default): carried forward from OLD Sprint 36
   §6.6 / §8.9. Deliver-agent + human confirm at Sprint 41
   planning round.
9. **OQ-13.3.9** — Dropped-intake-fields trace surface at UC
   switch (carried from OLD Sprint 36 §6.7 / §8.7): project
   dropped fields as a soft-signal slot
   `carried_intake_fields_dropped_at_switch` (default) OR silent
   drop? Deliver-agent + human revisit at Sprint 41 planning
   round.
10. **OQ-13.3.10** — `system_prompt.txt` line count premise
    refinement (per §13.2 Refinement 1): post-Sprint-40 shell
    shape verification. Deliver-agent + human visibility for
    Sprint 40 planning.
11. **OQ-13.3.11** — Supersede-pattern timing: this freeze's
    frontmatter declares `supersedes: [skill_foundation_design.md,
    skill_orchestration_candidates.md]`. The OLD freeze
    (`skill_foundation_design.md`) already has `status: superseded`
    + `superseded_by: docs/proposals/skill_registry_design.md`
    set by deliver-agent at commit fd7396a; this Sprint 37 dev
    commit does NOT re-edit those files (per Sprint 37 §6 #9
    fence). Status: no action required.

### 13.4 Self-walk on Sprint 37 design freeze itself

Per Sprint 37 §11 handoff contract §8 (anti-hardcode self-walk
on Sprint 37 itself): expected verdict `approve` because Sprint
37 ships docs-only design freeze + constitution-compliant
proposed design + no per-UC-pair if-else in proposed design. The
substantive §4.1 walk is in §11 above on the PROPOSED design;
the self-walk on the Sprint 37 commit DIFF (this design doc +
handoff) is in the handoff §8.

The freeze is internally consistent: each decision (a)-(j)
references downstream sub-sprints; the §4.1 walk in §11 covers
all decisions; the Tier-0 enumeration in §12 covers all surfaces
the §4.1 walk identifies as potential candidates; the open-
questions index in §13.3 surfaces all unresolved decision points
for deliver-agent + human + Codex at Sprint 37 close.

This concludes the Sprint 37 design freeze content. Sprint 38 /
39 / 40 / 41 implementation rounds proceed from this freeze
after Sprint 37 close per deliver-agent + human + Codex per-sub-
sprint review verdict `approve`.

