---
title: Java Guard / Prompt Flexibility / Skill / Eval — Layer Responsibilities — Sprint 5 (F3)
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
runtime_contract: false
last_reviewed: 2026-05-10
review_cadence: on_reactivation
notes: >
  Self-declared diagnostic defining the responsibility split between Java
  guard, prompt flexibility, skill orchestration, and eval verification,
  plus stop conditions for future sprints. The file itself implements no
  runtime / prompt / spec change. Use as a layer-selection lens before
  picking a fix layer for a given failure.
---

> This document is not the current runtime contract unless a docs/current/* contract or live code path confirms it.

# Java Guard / Prompt Flexibility / Skill / Eval — Layer Responsibilities — Sprint 5 (F3)

Date: 2026-05-05
Sprint: Prompt / Context Projection and Fix-Layer Diagnostic Sprint 5
Status: diagnostic — defines the responsibility split between Java guard,
prompt flexibility, skill orchestration, and eval verification, plus stop
conditions for future implementation sprints. No runtime / prompt / spec
change is implemented in this file.

## 1. Why this document exists

Sprints 1–4 trended toward Java runtime fixes for every recurring smoke
failure:

- Sprint 1: deterministic resolver, UC-K override, handover assembler.
- Sprint 2: §B0 reason normalization, §B1 distress detector, §B2
  routing bias, §B3 fallback UC.
- Sprint 3: §C0 LlmConfigValidator, §C1 LLM retry, §C2 strong-prior
  carry-forward.
- Sprint 4: §E1 LLM-distress gate, §E2 cs029 spec override, §E3
  override / audit consistency.

Codex review at the end of Sprint 4 effectively asked: *which remaining
failures are genuinely Java guard concerns vs. which are prompt /
context projection / skill orchestration / case-spec / infra concerns?*

The fix-layer taxonomy (`docs/diagnostics/fix_layer_taxonomy.md`) classified the
remaining smoke failures and found that **zero** of them are best fixed
as new Java runtime invariants. The marginal Java-guard payoff is now
low, and continuing to add runtime taxonomies on top of LLM enum picks
risks runtime / prompt drift.

This document defines the boundary so future sprints can pick the right
layer the first time.

## 2. Definitions

| Layer | Owns |
|---|---|
| **Java guard** | Deterministic invariants the runtime MUST guarantee regardless of LLM output. Implemented as bean-level code in `ControlKernel`, `EscalationReasonResolver`, `UseCaseRouter`, `PhaseEvaluator`, `HandoverPayloadAssembler`, `SessionManager`, `ToolDispatcher`, `ToolPolicyEnforcer`, `ClassifyUseCaseTool`. Pinned by `mvn` integration tests. |
| **Prompt flexibility** | What the LLM *should* prefer when multiple legal options exist. Implemented in `system_prompt.txt`, `routing_prompt.txt`, and the `systemInstruction` / `groundingInstruction` / `escalationPolicy` strings inside `PhaseEvaluator.plan`. Pinned by golden snapshot tests of the prompt strings + smoke run regression. |
| **Skill orchestration** | A deterministic state machine wrapping a recurring multi-tool / multi-turn flow. Implemented as a parametrized `PhasePlan` (see F2 §6). Pinned by terminal-state predicates + skill-level integration tests. |
| **Eval verification** | What gets measured. Implemented in `eval_interactive/case_specs/...`, `case_spec_overrides.yaml`, the L1/L2/L3 scoring code, and the override pipeline. Pinned by `pytest eval_interactive/tests`. |

## 3. Responsibility split

### 3.1 What Java MUST guarantee (java_guard)

Tier-0 invariants. The LLM cannot be trusted to maintain these because
their violation breaks contracts the eval / human reviewers rely on.

- **Resolver precedence**: the canonical priority table in
  `EscalationReasonResolver` (priority 1 `user_requested` >
  priority 2 `user_distress` > priority 40
  `clarification_budget_exhausted` > priority 41
  `faq_miss_threshold_exceeded` > priority 42 `turn_budget_exhausted` >
  intake-complete family > Tier-2 policy reasons > infra reasons).
  This is the *contract* — every persisted handover, every session
  state, every payload uses the same canonical value.

- **Reason canonicalisation across surfaces**
  (`L1:escalation_reason_consistency`): the persisted
  `request_handover.arguments.escalation_reason`, the session
  `escalationReason`, and the handover payload all carry the same
  resolver-canonical value. Implemented by Sprint 2 §B0
  (`ControlKernel.normalizeHandoverArgsToSessionReason` +
  `PhaseEvaluator.canonicalize`).

- **Deterministic distress detector** (Sprint 2 §B1):
  `EscalationReasonResolver.detectDistressSignal` + ALL-CAPS shout
  heuristic. Earned by deterministic patterns; the LLM cannot stamp
  `user_distress` without a B1 hit (Sprint 4 §E1 gate).

- **UC-K technical regression override** (Sprint 1 §A2):
  `UseCaseRouter.matchUcKTechnicalRegression` for the cs_066-shape
  technical regression patterns. Pre-LLM deterministic.

- **Strong-prior carry-forward** (Sprint 3 §C2):
  `ClassifyUseCaseTool.shouldPreserveStrongPrior` refuses LLM-driven
  UC overwrite when current active UC matches the deterministic
  strong-prior derivation. Releases on hard-shift exits.

- **Soft-OOS UC fallback** (Sprint 2 §B3):
  `ControlKernel.inferFallbackUseCase` fills in `active_use_case` on
  forced escalation when no UC committed, so the contract gate
  doesn't fire on UNKNOWN-topic + immediate-escalate sessions.

- **Tool whitelist enforcement**: `ToolDispatcher.validateAgainstPlan`
  refuses any tool not in `plan.allowedTools` regardless of what the
  LLM emits. Defense in depth even if the prompt's allowed-tool cue
  is ignored.

- **Tool policy per UC**:
  `ToolPolicyEnforcer.getVisibleToolsForUc` filters `tool_schemas`
  by `active_use_case`. Forbidden tools (per
  `customer_service_tool_spec_v0_2.yaml`) never appear in the
  projection.

- **Override / audit consistency** (Sprint 4 §E3):
  `test_smoke_yaml_matches_override_pipeline_output` hard-fails on
  any direct hand-edit of `expected.*` fields without a matching
  approved override entry. Smoke curator
  `_REQUIRED_CASE_IDS` pins cs029 + cs066.

- **Already-escalated reconcile** (Sprint 3.1):
  `SessionManager.reconcileEscalationReasonOnAlreadyEscalatedSession`
  uses the resolver directly so the reconcile path bypasses the §E1
  gate (intentional — once a session is escalated, the LLM's
  per-turn output cannot downgrade it).

- **Auto-fill `request_handover` tool call on legacy paths** (Sprint
  2 §B0): both AgentRunLoop and the legacy `recordTurn` ESCALATE
  branch synthesize a `request_handover` entry in `bot_turns.tool_calls`
  if missing, so eval's L1:escalation_compliance always has the
  evidence row.

These are the only Java guarantees the runtime should be making for
content-aware behaviour. **New java_guards SHOULD only be added when**:

- A specific failure mode breaks one of the existing invariants OR
- A new invariant is required by an eval contract that cannot be
  expressed any other way (rare).

### 3.2 What the prompt SHOULD guide (prompt_context_projection)

Prompt owns *preferences* between legal options when the LLM has multiple
valid choices. The prompt should never re-encode information the runtime
can produce deterministically.

- Customer-facing language, tone, brevity (system_prompt.txt
  generic-rules section).
- DISCOVER vs RESOLVE phase posture (system_prompt.txt DISCOVER
  guidance, PhaseEvaluator's per-phase systemInstruction).
- Tiebreaker hints between two equally legal UC routes (e.g.
  routing_prompt UC-A vs UC-FP — F1 §C2).
- Tiebreaker hints between two equally legal escalation reasons
  (e.g. `payment_dispute_detected` vs `faq_miss_threshold_exceeded`
  for FAQ UCs — F1 §C1).
- "Search before answering" preference for FAQ RESOLVE turn 1
  (F1 §C3).
- "Ask only the missing intake field" preference for INTAKE RESOLVE
  (F1 §C4).
- "Empty `candidate_use_cases` should trigger search, not escalate"
  preference for soft-OOS DISCOVER (F1 §C5).

The prompt should NOT:

- Re-encode the resolver precedence table (drift risk).
- Re-encode the §B1 distress patterns (would create false-positive
  precedent for the LLM to claim distress without the deterministic
  hit).
- Carry per-case keyword lists that the runtime already encodes
  (B2 messaging-bias, UC-K override).

The projection (`ContextProjectionBuilder`) should expose:

- `session.candidate_use_cases` (currently missing — F1 §C5 gap).
- `intake_state.fields_collected / fields_remaining` for INTAKE
  phases (currently missing — F1 §C4 gap).
- `customer_context.moderation_status` when known (currently
  inconsistent — F1 §C2 dependency).
- `accumulated_tool_results` keyed by sequence (currently last-write-
  wins, deferred per F1 §2.7).

### 3.3 What skills SHOULD orchestrate (skill_orchestration)

Skills own *recurring multi-step flows* with a deterministic terminal-
state predicate. They are parametrized PhasePlans, not a separate
runtime framework (per `docs/sprint_objective.md` §"Do not implement").

Skills should be introduced when:

- A recurring shape spans 3+ tools or 2+ turns.
- The terminal-state predicate (search has hit ≥ threshold; intake
  fields all collected; FAQ resolve emitted with citation) is
  deterministic.
- Prompt-only fixes have been tried and the failure mode persists
  across nondeterminism re-runs.

Per `docs/proposals/skill_orchestration_candidates.md` §6 and `docs/diagnostics/codex-findings.md`
Sprint 5.1 review, Sprint 6 ships exactly one skill:

- **S1 FAQ-grounded-resolve**: DISCOVER → search → resolve_article →
  grounded customer-facing answer → record_outcome (or explicit
  handover only after a valid resolve attempt cannot complete).
  Anchors cs_192 ("answer emitted without citation / resolve
  sequence incomplete") and cs_259 ("search happened, resolve did
  not complete"); regression-guards cs_001 / cs_011.

Defer (NOT Sprint 6 implementation scope):

- **S2** Intake-collect-and-handover: parametrized by UC's required
  intake fields. Anchors cs_066. Higher test cost than S1.
- **S3** Soft-OOS clarify-or-escalate. The previously-suggested
  "F1 §C5 + small runtime guard on `request_handover(faq_miss)`
  without prior search" is removed per Sprint 5.1 codex correction —
  cs_259 r2 already had a prior `search_knowledge` call, so the
  no-prior-search guard would not address cs_259's observed failure
  and is NOT the cs_259 fix. cs_259 is owned by S1.
- **S4** Account login-recovery (cs_011 currently PASSes — defensive
  only).
- **S5** Tier-2-reason × UC compatibility (ship F1 §C1 prompt fix
  first).

### 3.4 What eval SHOULD verify (case_spec_eval)

Eval is the only layer that scores semantic outcome. The eval surface:

- Defines the spec (`eval_interactive/case_specs/...`) which encodes
  the expected primary_uc, escalation_trigger, outcome_class,
  expected_tool_sequence, forbidden_tools, etc.
- Routes spec corrections through `case_spec_overrides.yaml` (Sprint 4
  §E3 enforces this — no direct `expected.*` hand-edits).
- Scores with L1 (hard contracts), L2 (outcome / sequence), L3
  (LLM-judged tone / relevance / groundedness).
- Runs the smoke / anchor / exploration sets through the override
  pipeline; the regression tests
  (`test_smoke_yaml_matches_override_pipeline_output`,
  `test_v2_schema_loads_cleanly`,
  `test_cs_interactive_*_override_survives_fresh_extraction`) pin
  the override-pipeline integrity.

Eval-side fixes should be reserved for:

- Spec authoring errors (`L2_GATE:correct_uc` mismatches that the
  override pipeline can fix without changing runtime).
- Override-pipeline integrity (Sprint 4 §E3).
- Adding L1 / L2 / L3 dimensions when a new contract is needed.
- Judge calibration (deferred per `docs/action_bank.md` D15).

Eval-side fixes should NOT be used to:

- Mask a genuine bot mistake by widening the spec to accept the
  bot's wrong answer.
- Encode runtime invariants that should live in Java.
- Replace prompt or skill fixes when the failure is a behaviour
  shape, not a spec mismatch.

### 3.5 What infra SHOULD handle (infra_runtime)

Infra owns reachability and latency. The current open infra concern
is the Kimi `session_create_failed: ReadTimeout` family (5 unique
cases across the two Sprint 4 runs). Per
`docs/current_eval_baseline.md` "Recommended next sprint direction":

- Widen eval-client timeout to 120s, OR
- Pre-warm the first Kimi call, OR
- Async pre-fetch FAQ snapshots before the first user turn, OR
- Accept-and-retry on ReadTimeout.

Pick one, implement narrowly, do not couple to runtime / prompt /
skill changes.

## 4. Decision tree for picking the layer (next sprints)

When a new failure surfaces, walk the tree:

1. **Does the session ever start?** No → `infra_runtime`. Yes →
   continue.
2. **Does the failure violate a Tier-0 contract**
   (resolver precedence, reason consistency, override pipeline
   integrity, tool whitelist)? Yes → `java_guard`. No → continue.
3. **Is the LLM picking from a legal option set, just the wrong
   element?** (e.g. `payment_dispute_detected` vs
   `faq_miss_threshold_exceeded` for a FAQ UC.) Yes → start with
   `prompt_context_projection`. If the prompt fix is unstable across
   nondeterminism re-runs, escalate to `skill_orchestration` or
   `java_guard`.
4. **Is the failure a recurring multi-tool / multi-turn shape with a
   deterministic terminal predicate?** (e.g. UC-K intake
   completion, FAQ-grounded resolve.) Yes →
   `skill_orchestration`.
5. **Is the spec asking for something the runtime doesn't / can't do
   without fabricating?** (e.g. cs_095 email-sync resolve when the
   FAQ surface has no article.) Yes → `product_policy_gap` (Phase 2
   spec change) or `case_spec_eval` (override).
6. **Is the same bot output flapping under nondeterminism?** Yes →
   `judge_calibration` (deferred D15) — not a runtime / prompt
   issue.
7. **None of the above** → `unknown_needs_human_review`. Walk the
   transcript before committing a layer.

## 5. Stop conditions for future implementation sprints

A future sprint should NOT add a new `java_guard` unless ALL of:

1. The failure breaks an existing Tier-0 invariant OR a new Tier-0
   invariant is required by an eval contract that no other layer
   can express.
2. A prompt-only fix has been attempted and demonstrated insufficient
   across at least 2 nondeterminism re-runs OR the prompt fix is not
   feasible (e.g. the contract is content-independent, like the
   override-pipeline guard in Sprint 4 §E3).
3. The proposed guard has a bounded test surface (≤ ~20 mvn
   integration tests) and a clear rollback condition.
4. The guard does NOT re-encode prompt or skill behaviour; it is a
   genuine deterministic invariant.

A future sprint should NOT add a new prompt section unless ALL of:

1. The candidate sentence anchors to a specific runtime observable
   (`session.*`, `accumulated_tool_results`, `phase_plan.*`,
   `customer_context.*`).
2. The candidate is target-case-specific — at most one new section
   per failing case shape per sprint.
3. The candidate has a paired Java integration test pinning the
   projection shape, AND a golden-snapshot test on the prompt
   string.
4. A smoke run before/after has been completed and documented.

A future sprint should NOT add a new skill unless ALL of:

1. The recurring shape is documented across at least 2 distinct
   smoke cases.
2. The terminal-state predicate is deterministic (testable by Java).
3. The skill has been expressed as a parametrized `PhasePlan`, not a
   new framework.
4. Existing passing cases that touch the skill's UC family have
   regression integration tests.

A future sprint should NOT add a new eval-side override unless ALL of:

1. The override goes through the approved Wave A6.6 v2 path
   (`case_spec_overrides.yaml` + audit + smoke YAML regenerated +
   regression test).
2. The override has reviewer, date, source, confidence, supporting
   turn numbers, rationale.
3. The override is not masking a genuine bot bug (i.e. it's a spec
   authoring correction, not a behaviour reclassification).

A future sprint should NOT defer to `judge_calibration` or
`product_policy_gap` without explicit scope. These are out-of-band
concerns; the immediate sprint cannot fix them and should not
attempt to.

## 6. Anti-patterns to avoid

These are patterns that prior sprints encountered and that future
sprints should explicitly guard against.

- **Java guard for every LLM enum pick**: re-encoding the
  `request_handover` enum's 23 values into runtime downgrade rules
  per active UC. Each rule adds test cost and brittleness.
  Counter-example to follow: §E1 Tier-0 distress-gate (correct —
  Tier-0 invariant). Anti-pattern: a Tier-2 reason × UC compatibility
  table baked into Java when a prompt sentence would have done.

- **Prompt encoding what the runtime knows**: re-listing the §B1
  distress patterns or the §C2 strong-prior aliases in the prompt
  creates drift risk. The runtime already enforces these; the
  prompt should reference the *runtime observable*
  (`session.escalationReason`, `session.activeUseCase`), not
  re-encode the source-of-truth.

- **Skill that asks the LLM to read its own prior reasoning**:
  multi-turn planning loops are out of scope for V1. Any "skill"
  that requires the LLM to chain its own previous outputs is a
  framework concern, not a PhasePlan extension.

- **Eval override that masks behaviour**: changing
  `outcome_class=resolve` to `outcome_class=escalate` on a case
  where the bot is clearly wrong, just to make the case PASS. The
  override path has Sprint 4 §E3 guards but the *judgement* of
  when to override is human. Document the rationale; do not
  override to mask.

- **Sprint scope creep by full-review → fix → full-review loops**:
  per `docs/action_bank.md` §"Rule (carry-over)", each sprint must
  name 3–4 accepted actions, define target cases, and close only
  when Codex reports no blocking sprint failures or only
  non-blocking P2 notes remain.

## 7. Cross-references

- Fix-layer per case: `docs/diagnostics/fix_layer_taxonomy.md`.
- Prompt / context candidate changes: `docs/diagnostics/prompt_context_projection_audit.md`.
- Skill candidates: `docs/proposals/skill_orchestration_candidates.md`.
- Recommended next sprint direction:
  `docs/current_eval_baseline.md` §"Recommended next sprint direction".
- Carry-over deferrals: `docs/action_bank.md` D1–D7, D15.
- Sprint scoping rule: `docs/action_bank.md` §"Rule (carry-over)".
