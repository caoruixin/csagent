---
title: Skill Foundation Design — Hybrid framing (envelope + Runtime-floor predicate); UC-switching wide continuity invariants [SUPERSEDED 2026-05-17]
doc_tier: proposal
status: superseded
implementation_status: superseded_mid_flight (only Sprint 36 freeze shipped; downstream Sprints 37+ per OLD framing never implemented)
source_of_truth: docs/proposals/skill_registry_design.md (NEW Sprint 37 design freeze per NEW M2)
last_reviewed: 2026-05-17
review_cadence: archived; no future review
supersedes: [docs/proposals/skill_orchestration_candidates.md]
superseded_by: docs/proposals/skill_registry_design.md
notes: >
  Sprint 36 (M2-Skill sub-sprint 1) design-freeze output. Refines
  the Sprint 5 (F2) skill orchestration proposal into a hybrid
  framing locked at M2-Skill approval round 2026-05-17: envelope +
  recommended order via prompt; terminal predicate via Java guard
  for Runtime-owned floor ONLY. Six sub-decisions D1-D6 covered.
  Carries the human authorization (verbatim quote) on the S1
  citation predicate scope per OLD `docs/milestones/M2-Skill_objective.md`
  §6 #4 (carried forward into NEW M2 §6 #4 verbatim).

  SUPERSEDED MID-FLIGHT 2026-05-17 (see "Why superseded" section
  below). This freeze's "minimum-surface incrementalism" framing
  (reuse existing PhasePlan + new system_prompt.txt teaching
  paragraph + new predicate adjacent to existing shouldRejectXxx
  family) was wholesale rejected by the human in favor of a
  first-class Skill Registry abstraction with retroactive
  externalization. Body retained as historical reasoning archive
  per `doc_governance.md` "Future proposals" supersede-not-delete
  rule (the alternative-considered reasoning here informs why NEW
  M2 took the bigger step).
---

# Skill Foundation Design — Hybrid framing [SUPERSEDED 2026-05-17]

## Why superseded (2026-05-17)

This doc was the architectural decision doc that Sprints 37 (S1 implementation), 38 (S2 implementation), and 39 (UC-switching wide continuity implementation) under OLD M2-Skill framing rode on. **It was wholesale superseded mid-flight on 2026-05-17 by `docs/proposals/skill_registry_design.md` (NEW Sprint 37 design freeze) per human direction.**

**What this freeze decided (and got superseded for)**: minimum-surface incrementalism — reuse existing PhasePlan; add new teaching paragraph to `system_prompt.txt` adjacent to Sprint 23/31/33 paragraphs; add new Java predicate to `AgentRunLoopImpl.java` adjacent to existing Sprint 6/7/11 `shouldRejectXxx` family; no new Skill data model class, no new SkillRegistry, no externalized YAML/JSON Skill definitions. The freeze argued (§1, §3) that this was the "natural extension" within current PhasePlan framework without "core refactor".

**Why the human rejected the minimum-surface framing**: this freeze, despite naming itself "Skill Foundation", actually PRESERVED the scattered Zhang-Sanfeng pattern that NEW M2 was supposed to fix. Sprint 23 teaching paragraph + Sprint 31 paragraph + Sprint 33 paragraph + would-be Sprint 37 paragraph would continue to accumulate side-by-side in `system_prompt.txt`. Sprint 6 predicate + Sprint 7 predicate + Sprint 11 predicate + would-be Sprint 37 predicate would continue to accumulate side-by-side in `AgentRunLoopImpl.java`. "Skill" in this freeze was just a label for "another paragraph plus another adjacent predicate" — there was no extracted abstraction. The human's M2 architectural intent was always "extract scattered content into a Skill abstraction" (research-agent proposal); this freeze inverted that intent by choosing the minimum-surface path.

**What NEW M2 does instead** (per `docs/milestone_objective.md` NEW M2 + `docs/proposals/skill_registry_design.md` Sprint 37 NEW freeze):

- Skill is a first-class externalized definition (YAML/JSON file under `server/src/main/resources/skills/`).
- New `SkillRegistry` Java class loads + indexes Skill files at boot.
- `PhaseEvaluator.java` becomes a Skill Selector (`skillRegistry.select(phase, useCase) → Skill`).
- All 6 phase content (currently hardcoded as `systemInstruction` / `groundingInstruction` / `escalationPolicy` Java strings in PhaseEvaluator) retroactively migrated into Skill YAMLs.
- All Sprint 23/31/33 teaching paragraphs in `system_prompt.txt` retroactively migrated into corresponding Skill `procedure` / `guardrails` blocks; `system_prompt.txt` shrinks to an orchestration shell.
- All Sprint 6/7/11 Java predicates in `AgentRunLoopImpl.java` retroactively migrated into Skill `guardrails` enforced via a unified Skill terminal-predicate dispatcher.
- S1 (`Resolve.FAQ.GroundedAnswer`) citation predicate ships as a `guardrails.must_cite_source` declaration in `resolve_faq_grounded_answer.yaml`, NOT as a standalone Java method in AgentRunLoopImpl. The §6 #4 verbatim human authorization on bounded inversion of `D-hard-citation-gate` is CARRIED FORWARD into NEW M2 verbatim.
- S2 (`Resolve.Intake.CollectAndHandover`) intake-completeness predicate ships as a `guardrails.intake_complete_required` declaration in `resolve_intake_collect_and_handover.yaml`.
- UC switch + state preservation ships at NEW Sprint 41 with a session-level state-bus + per-Skill `state_inheritance` declaration designed coherently with the Skill abstraction (not as a separate add-on).

**Carry-forward into NEW M2 from this freeze**:

- §6 #4 verbatim human authorization on S1 `must_cite_source` bounded inversion of `D-hard-citation-gate` (preserved verbatim in NEW M2 §6 #4 and in NEW Sprint 37 contract).
- All hard fences on `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` (M3-D deferred).
- All hard fences on `escalation_reason` enum widening (`D-new-escalation-reason-enum` deferred to M3-A).
- All hard fences on `INTAKE_UCS` edit (M3-B Single Handover Orchestrator deferred unless cutover pressure).
- All hard fences on `D-full-issue-ledger` (hard-deferred; requires explicit new-objective approval).
- All hard fences on existing case families + shadow case families (cascade fence carried from M1).
- The Sprint 36 §1.3 MATERIAL FINDING about Sprint 11 §M1 `shouldRejectPrematureResolveOutcome` precedent (informs NEW Sprint 37 design decision (g) on predicate migration mapping).
- The §4.1 nine-question kernel walk pattern (NEW Sprint 37 walks the same 9 questions on the NEW Skill Registry abstraction design).

**What stays valid in this doc as architectural reasoning archive**:

- §1 Purpose + relation to upstream — the framing of "Skill = parametrized PhasePlan" carries over.
- §3 (D2) predicate shape decision — informs NEW Sprint 39 implementation choices on Skill guardrails dispatcher.
- §4 (D3) S1 trigger condition decision — informs NEW S1 `must_cite_source` Skill guardrail scope.
- §6 (D5) UC-switching invariant matrix sketch — informs NEW Sprint 41 design decision (i) state_inheritance semantics.
- §7 §4.1 walk-through pattern — informs NEW Sprint 37 (j) walk methodology.

**What is NO LONGER binding in this doc**:

- D1 ("reuse existing PhasePlan; no new PhasePlan field or skill_envelope projection slot") — NEW M2 introduces new Skill data class, SkillRegistry, SkillLoader, and (in Sprint 41) prior_use_case_carry projection slot.
- D4 ("envelope teaching as new system_prompt.txt paragraph") — NEW M2 migrates teaching INTO Skill YAML; system_prompt.txt shrinks to orchestration shell.
- D6 ("Sprint 37 ships citation predicate adjacent to Sprint 11 §M1 in AgentRunLoopImpl") — NEW M2 Sprint 39 ships citation predicate as Skill `guardrails.must_cite_source` declaration enforced by unified Skill terminal-predicate dispatcher, NOT as adjacent Java method.

The reasoning chain in §1-§8 below is preserved verbatim as historical record. Read it alongside NEW `docs/proposals/skill_registry_design.md` to understand what was rejected and why.

---

# Original Sprint 36 design freeze content below (preserved verbatim 2026-05-17; do NOT edit)

> This document was the architectural decision doc that Sprints 37
> (S1 implementation), 38 (S2 implementation), and 39 (UC-switching
> wide continuity implementation) under OLD M2-Skill framing rode on.
> It supersedes the Sprint 5 (F2) proposal at
> `docs/proposals/skill_orchestration_candidates.md` (status:
> superseded; body retained as upstream reasoning archive per
> `doc_governance.md`). The Sprint 5 (F2) reasoning is not deleted;
> this freeze refines it.
>
> **[2026-05-17 supersession addendum]**: Note this document was
> itself superseded mid-flight on 2026-05-17 by
> `docs/proposals/skill_registry_design.md` (NEW Sprint 37 design
> freeze per NEW M2). See "Why superseded" section above for
> rationale. The reasoning below is preserved as historical record.

## 1. Purpose + relation to upstream

The goal is to lock the architectural framing for a Skill foundation
in the customer-service agent runtime that:

- Promotes the Sprint 5 (F2) proposed `Resolve.FAQ.GroundedAnswer`
  (S1) and `Resolve.Intake.CollectAndHandover` (S2) candidate skills
  from a `proposal / not_started` upstream document to code, per the
  M2-Skill milestone objective (`docs/milestone_objective.md` §2).
- Provides the architectural decisions Sprint 37 (S1 implementation),
  Sprint 38 (S2 implementation), and Sprint 39 (UC-switching wide
  continuity implementation) draft directly from, without further
  architectural rounds in those sub-sprints.
- Honors Constitution §1.3 (LLM owns drift / topic shift / next
  action / response strategy) by keeping the recommended order soft
  and the customer-facing language fully LLM-owned.
- Honors Constitution §1.4 (Runtime owns grounding floor + capability
  + safety + PII + idempotency) by encoding terminal predicates as
  Java guards in the Runtime tool layer.
- Honors Constitution §1.7 (no per-UC-branch if-else for soft semantic
  decisions; no encoding of visible-eval case text into runtime /
  prompt / judge; no widening of eval spec to accept a genuine bot
  mistake).

The framing is **hybrid**: the envelope (skill identity + tool
whitelist + recommended order + intended sequence teaching) is
surfaced as **prompt teaching** so it is LLM-readable and the LLM
owns deviation; the terminal predicate (capability-floor enforcement
of "an outcome class may only persist when its Runtime-owned floor
condition holds") is encoded as a **Java guard** in the Runtime tool
layer. The hybrid framing is the human + deliver-agent's pick
locked at M2-Skill approval round 2026-05-17 (per
`docs/milestone_objective.md` §1 + §2 — "envelope + recommended
order via prompt; terminal predicate via Java guard for Runtime-owned
floor ONLY").

### 1.1 Relation to the Sprint 5 (F2) upstream proposal

The Sprint 5 (F2) proposal at
`docs/proposals/skill_orchestration_candidates.md` introduced five
candidate skills (S1 / S2 / S3 / S4 / S5) and named the design
constraint *"Do NOT introduce a new skill runtime framework.
Implement S1 as a parametrized `PhasePlan` inside
`PhaseEvaluator.plan(...)`."* (Sprint 5 F2 §6.) M2-Skill adopts this
constraint:

- M2-Skill ships **only S1 + S2** (per Decision β-Q2 2026-05-17 +
  `docs/milestone_objective.md` §4 non-goal); S3 / S4 / S5 stay
  deferred to M3-C and later.
- M2-Skill implements S1 + S2 as parametrized `PhasePlan`s inside
  the existing `PhaseEvaluator.plan(...)` per phase + per active UC,
  plus a NEW envelope teaching paragraph in `system_prompt.txt`
  (sibling to Sprint 23 `already_called` / Sprint 31
  `alternate_candidate_use_cases` / Sprint 33
  `discover_disambiguation_signals` teaching paragraphs).
- M2-Skill does NOT introduce a new "skill engine" / "skill runtime
  framework"; the envelope is `PhasePlan` data + prompt text; the
  predicate is a Java method on the existing tool dispatch path in
  `AgentRunLoopImpl`.

### 1.2 Relation to the existing partial implementations (Sprints 6 / 7 / 11)

**Material finding at Sprint 36 premise check (2026-05-17):** the
Sprint 5 (F2) S1 / S2 predicate shapes are **partially shipped
already** in the production runtime, authored as Sprint 6 §G2 /
Sprint 7 §I2 / Sprint 11 §M1 work. The Sprint 36 design freeze
**formalizes + extends** these existing predicates into a coherent
Skill envelope framing; it does NOT introduce them from scratch.

| Existing partial implementation | Class + method | Authored | Skill alignment |
|---|---|---|---|
| FAQ-grounded-resolve guard (cs_259 r2 "search-ran-but-resolve-did-not" shape) | `AgentRunLoopImpl.shouldRejectFaqMissHandover(plan, call, accumulatedToolResults)` at `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java:690-734`; dispatched at `:412-413` | Sprint 6 §G2 | Part of S1's terminal predicate surface, but is the *upstream-step* check (forces `resolve_article` after a viable `search_knowledge` hit). S1 design freeze ADDS a citation-presence check on `record_outcome(class=resolve)` for the *downstream-step* surface (per human authorization §6 #4). |
| Intake-complete guard (refuse `intake_complete_for_uc_X` until all required fields collected) | `AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover(plan, call, session)` at `:628-651`; dispatched at `:317`; reject reason `intake_required_fields_missing_for_intake_complete` at `:120-121`. Calls `IntakeFieldsRegistry.intakeComplete(activeUc, collected)`. | Sprint 7 §I2 | This IS S2's terminal predicate, already shipped. The Sprint 36 design freeze CLARIFIES the framing: the current reject-and-hint behaviour mirrors §E1 (refuse an un-earned Tier-X reason); the alternate of a canonical-reason downgrade to `incomplete_intake` (per Sprint 5 F2 §3 S2 phrasing) is named as an open question (D4 + §8). |
| Premature `record_outcome(class=resolve)` guard | `AgentRunLoopImpl.shouldRejectPrematureResolveOutcome(plan, session, call)` at `:745-759`; dispatched at `:355-356`; delegates to `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass)`. | Sprint 11 §M1 + Sprint 11.1 | Adjacent to S1 (already refuses `record_outcome(class=resolve)` outside CONFIRM/CLOSE on FAQ plan). S1 design freeze ADDS a citation-presence check that fires WITHIN the same dispatch site when the existing phase / disposition gate passes. |

The implication for the Sprint 37 / 38 / 39 implementation contracts:

- **Sprint 37 (S1 implementation)** is narrower than the Sprint 5 (F2)
  proposal implied. The upstream-step part of S1 already ships
  (`shouldRejectFaqMissHandover` + RESOLVE-FAQ `PhasePlan` already
  names the sequence). What is NEW is (a) the citation-presence
  predicate on `record_outcome(class=resolve)` per the human
  authorization §6 #4 quote, and (b) the envelope teaching paragraph
  in `system_prompt.txt` adjacent to existing teaching paragraphs.
- **Sprint 38 (S2 implementation)** is even narrower. The terminal
  predicate already ships (`shouldRejectIncompleteIntakeHandover` +
  `IntakeFieldsRegistry.intakeComplete`). What is NEW is (a) the
  envelope teaching paragraph in `system_prompt.txt`, (b) possible
  reframing of the reject-and-hint behaviour as a canonical-reason
  downgrade to `incomplete_intake` (D4 open question; deliver-agent
  + human pick at Sprint 38 planning round), and (c) per Sprint 5
  (F2) §3 S2 "the skill steps are parametrized by the UC's
  `requiredIntakeFields`" — this is already covered by the existing
  intake state surface (`IntakeFieldsRegistry.requiredFieldsFor` +
  `ContextProjectionBuilder.intake_state` projection), so Sprint 38
  is mostly teaching + framing.

The design freeze documents this clearly so Sprint 37 + 38 do not
re-invent existing predicates and do not double-encode the same
floor invariant.

### 1.3 Relation to the M2-Skill milestone objective

The freeze contract this document produces ships against
`docs/milestone_objective.md`:

- §1 sub-sprint layer breakdown (5 sub-sprints in M2-Skill; Sprint
  36 design-freeze is sub-sprint 1).
- §2 goal — "Promote the Sprint 5 (F2) skill orchestration proposal
  to code via a hybrid Skill foundation, and add wide UC-switching
  continuity."
- §6 hard fences #1 (no per-UC-branch if-else in skill body), #2
  (predicate may only protect Runtime-owned floor), #4 (HARD FENCE
  INVERSION on `D-hard-citation-gate` for the S1 narrow scope —
  with human authorization quoted §4 below verbatim), #18 (no skill
  prescribes LLM customer-facing language), #19 (no skill hard-encodes
  per-step argument values).
- §10 stop condition #1 — Sprint 36 freeze may surface a Tier-0
  candidate; §8 below addresses this explicitly.

## 2. D1 — Skill envelope shape

A **Skill envelope** is the architectural container that carries
(i) skill identity, (ii) tool whitelist, (iii) recommended order,
(iv) intended-sequence teaching, surfaced to the LLM so the LLM
understands its own context and the runtime's expectations.

### 2.1 Proposed shape

The envelope is **NOT** a new class or framework. It is composed of
two existing surfaces:

1. **`PhasePlan` data surface** (the existing record at
   `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java`,
   fields `phase / useCase / objective / allowedTools /
   requiredContextKeys / maxToolSteps / allowInterimMessage /
   validTerminalOutcomes / systemInstruction / groundingInstruction
   / escalationPolicy`). The Skill envelope reuses these existing
   fields verbatim:
   - `phase` + `useCase` together identify *which Skill is active*
     (e.g., `phase=RESOLVE` + `useCase=UC-A` (FAQ-path) implies the
     S1 `Resolve.FAQ.GroundedAnswer` envelope; `phase=RESOLVE` +
     `useCase=UC-H` (INTAKE-path) implies the S2
     `Resolve.Intake.CollectAndHandover` envelope).
   - `allowedTools` is the tool whitelist (already enforced by
     `ToolDispatcher.validateAgainstPlan`).
   - `objective` + `systemInstruction` + `groundingInstruction` +
     `escalationPolicy` carry the recommended order + intended
     sequence teaching at the **principle level**. The existing
     RESOLVE-FAQ branch at
     `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:618-665`
     already names the S1 sequence:
     *"FAQ-path RESOLVE flow (S1): the intended terminal sequence is
     search_knowledge -> resolve_article -> grounded customer-facing
     answer (with a source_id citation) -> record_outcome. Only
     escalate via request_handover after a valid resolve attempt
     cannot complete (no viable hit, or resolve_article could not
     produce a grounded answer)."*
2. **`system_prompt.txt` teaching paragraph surface** — a NEW
   principle-level paragraph (one per skill at most) adjacent to the
   existing Sprint 23 `already_called` (line 23-28), Sprint 31
   `alternate_candidate_use_cases` (line 30-33), Sprint 33
   `discover_disambiguation_signals` (line 36-37) teaching paragraphs.
   The S1 / S2 envelope teaching paragraphs land in Sprint 37 /
   Sprint 38 dev sessions; the design freeze contract names the
   shape:
   - Names the skill by name (e.g., "Resolve.FAQ.GroundedAnswer
     skill envelope").
   - Names the recommended order at the principle level (search →
     resolve → grounded answer with citation → record_outcome).
   - States the LLM-owned deviation posture: the order is recommended,
     not enforced at every step. The LLM may deviate when a viable
     non-FAQ resolution surface applies (e.g., a customer-context
     answer for an account-specific question). The Runtime enforces
     ONLY the Runtime-owned floor (per D2 + D3).
   - Names the projection slots the LLM should read (`already_called`,
     `intake_state`, `accumulated_tool_results`, etc.) to assess
     whether the envelope's sequence has been satisfied.
   - Does NOT contain per-UC-branch if-else logic (§1.7 enforced).
   - Does NOT prescribe customer-facing language (§6 hard fence
     #18 enforced).

The Skill envelope is **descriptive teaching plus existing `PhasePlan`
data**, not a new field, not a new class, not a new framework.

### 2.2 Why this shape (rationale)

- **Reuses existing infrastructure.** The Sprint 5 (F2) proposal
  explicitly said *"Do NOT introduce a new skill runtime framework"*
  (F2 §6). The proposed shape adds zero new classes; it adds at
  most NEW `system_prompt.txt` teaching paragraphs and (Sprint
  37/38) NEW predicate methods in `AgentRunLoopImpl` that hook into
  the existing dispatch path.
- **Already partially shipped.** The RESOLVE-FAQ `PhasePlan` at
  `PhaseEvaluator.java:618-665` already carries the S1 envelope's
  recommended-order teaching in `systemInstruction` +
  `groundingInstruction` + `escalationPolicy`. The Sprint 37
  envelope teaching paragraph in `system_prompt.txt` is the SIBLING
  surface that makes the envelope LLM-discoverable from the system
  prompt itself, not only the per-turn projection.
- **Honors §1.7.** The envelope teaching paragraph is principle-level
  (one paragraph, no per-UC-pair branch); the `PhasePlan` data is
  parametrized by `useCase` but carries no per-UC if-else in the
  envelope body — only the existing per-phase + per-UC-path branching
  in `PhaseEvaluator.plan(...)`, which is structural (DISCOVER /
  RESOLVE-FAQ / RESOLVE-INTAKE / CONFIRM / CLOSE / ESCALATE), not
  per-UC-pair logic.
- **Honors §1.3.** The envelope is teaching, not enforcement. The
  LLM owns the per-step argument choice, the customer-facing
  language, and the recommended-order deviation. The Runtime
  enforces ONLY the terminal-predicate floor per D2 + D3 + D4.

### 2.3 Open question — `D1.1` (envelope-identity surfacing)

Open question for Sprint 37 planning round: does the projection
slot need a NEW `skill_envelope` field that names the active skill
explicitly (e.g., `"skill_envelope": "Resolve.FAQ.GroundedAnswer"`),
or is the current `phase` + `active_use_case` + recommended-order
teaching sufficient? The design doc recommends **default no**
(rely on existing projection fields; the envelope teaching paragraph
in `system_prompt.txt` names the skill by name); deliver-agent +
human can revisit at Sprint 37 planning round if observed traces
show the LLM benefits from an explicit per-turn skill name.

## 3. D2 — Skill terminal predicate shape

A **Skill terminal predicate** is a deterministic Java check on a
specific tool call within a specific phase + UC scope. It enforces
the Constitution §1.4 Runtime-owned floor (grounding-citation,
capability, safety, PII, idempotency). It does NOT enforce LLM-owned
next-action / UC-hypothesis / response strategy choices (§1.3).

### 3.1 Where predicate code lives

Terminal predicate code lives in the Runtime tool dispatch layer
(`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
+ adjacent classes in `service/runtime/` and `service/tools/`). The
existing partial implementations (per §1.2) are the precedent:

- `shouldRejectFaqMissHandover` (Sprint 6 §G2) — static method in
  `AgentRunLoopImpl`, dispatched in the tool dispatch loop at line
  412-413.
- `shouldRejectIncompleteIntakeHandover` (Sprint 7 §I2) — static
  method, dispatched at line 317.
- `shouldRejectPrematureResolveOutcome` (Sprint 11 §M1) — static
  method, dispatched at line 355-356; delegates to
  `ResolveDispositionEvaluator`.

NEW S1 + S2 predicates (Sprint 37 / 38) follow this pattern: a
static method in the appropriate Runtime class (`AgentRunLoopImpl`
for tool-dispatch-time checks; `EscalationReasonResolver` for
canonical-reason downgrades; `RecordOutcomeTool` for outcome-class
persistence checks), dispatched at the existing dispatch site that
already handles the related tool call.

### 3.2 Predicate-fail behaviour

When the predicate fires, three response patterns are available:

1. **Rejection (reject-and-hint).** The tool call is rejected; the
   accumulated_tool_results slot for the tool carries an error +
   hint structure (see existing `INTAKE_COMPLETE_GUARD_REJECT_REASON`
   at `AgentRunLoopImpl.java:120-121` + the wrap structure at
   `:332-340`). The LLM sees the error on the next loop iteration
   and retries. **This is the existing pattern for Sprint 6 / 7 / 11
   guards.**
2. **Canonical-reason downgrade.** The escalation_reason canonical
   value is downgraded from a higher-priority value to a
   lower-priority value (mirrors `ControlKernel.applyEscalationReason`
   line 157-162: un-confirmed `user_distress` downgraded to
   `faq_miss_threshold_exceeded`). The tool call still proceeds;
   the canonical reason recorded in the session reflects the
   downgrade.
3. **Observation-only.** The predicate logs a trace entry but does
   not reject + does not downgrade. **OUT OF SCOPE** for a *terminal
   predicate* — observations belong in projection (`ContextProjectionBuilder`)
   + trace logging (`LlmCallLogger` / `BotSessionTraceWriter`), NOT
   in the predicate layer. A predicate by definition gates a
   capability-floor invariant; an observation does not.

The S1 / S2 design (D3 + D4 below) picks among (1) and (2) per
predicate; (3) is explicitly excluded as not-a-predicate.

### 3.3 Predicate scope discipline

A predicate is in scope for §1.4 only when ALL of the following
hold:

- It protects a Runtime-owned floor: grounding-citation presence,
  capability / permission boundary, PII / safety floor, idempotency
  guard, or the equivalent.
- It does NOT enforce an LLM-owned semantic decision: UC hypothesis,
  drift / topic shift, next-action choice, escalation posture,
  customer-facing wording.
- It does NOT branch on visible-eval CaseSpec ids, trace phrasing,
  or user-message keywords (§1.7).
- It honors a verifiable, reproducible trace contract: when the
  predicate fires, the trace records (per
  `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`)
  enough state for a future reviewer to reproduce the predicate
  decision from the per-turn trace alone (predicate-input data
  surface + decision outcome).

### 3.4 Trace-log entry shape

When a predicate fires, the trace SHALL record:

- The predicate name (e.g., `s1_citation_presence_required`,
  `intake_required_fields_missing_for_intake_complete`).
- The decision outcome (rejected / downgraded; downgraded-from /
  downgraded-to if (2)).
- The predicate-input data surface that drove the decision (e.g.,
  for S1 citation predicate: the user-facing message text + the
  absence of any `source_id` token, both as observed by the
  predicate at decision time; for S2: the missing fields list per
  `IntakeFieldsRegistry.fieldsRemaining`).
- The wired reject reason / downgrade reason persisted to
  `accumulated_tool_results.<tool>.error` (for reject) or
  `session.escalationReason` (for downgrade).

The existing predicates already emit ToolEvent entries with reject
reasons (e.g., `AgentRunLoopImpl.java:326-340` for the intake guard
pattern); Sprint 37 / 38 NEW predicates extend this pattern. Trace
schema is unchanged; only the per-predicate label set widens.

## 4. D3 — S1 (`Resolve.FAQ.GroundedAnswer`) trigger + predicate

### 4.1 Human authorization (verbatim, load-bearing)

This sub-decision is governed by the human authorization at the
M2-Skill approval round 2026-05-17, reproduced here verbatim from
`docs/milestone_objective.md` §6 #4:

> "Accept the Skill-bounded exception to D-hard-citation-gate. This
> is not a generic Java grounding-citation gate. It is a narrow S1
> terminal predicate that only applies when the bot attempts
> `record_outcome(class=resolve)` inside the `RESOLVE_FAQ` scope,
> and only checks citation presence as the minimum grounding-floor
> condition. The original deferral of a generic Java citation gate
> remains valid."

The authorization governs the predicate scope. Sprint 37
implementation MUST NOT expand the predicate beyond the
authorized boundaries enumerated in §4.3 below.

### 4.2 S1 trigger conditions

The S1 envelope is **active** (per D1) when:

- `current_phase == RESOLVE`
- AND `active_use_case ∈ FAQ-path UCs` per
  `UseCaseRegistryService.UseCaseDefinition.path == "FAQ"`. The
  current FAQ-path UCs per existing code at
  `AgentRunLoopImpl.java:106-108` (`FAQ_PATH_UCS`) are: UC-A,
  UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP. Source-of-truth is the
  registry (`UseCaseRegistryService`) — the predicate reads the
  registry, NOT a hardcoded UC list in the predicate body.

The S1 envelope teaching paragraph (Sprint 37 ships) is universal
across FAQ-path UCs — it names the principle-level recommended
order, not per-UC content. The existing RESOLVE-FAQ `PhasePlan`'s
`systemInstruction` at `PhaseEvaluator.java:618-665` already
parametrizes by `activeUc` for the objective ("for " + ucDef.name())
only, not for the sequence guidance.

### 4.3 S1 terminal predicate scope (per human authorization, EXACTLY)

The S1 terminal predicate fires ONLY when ALL of the following
hold (per §4.1 verbatim):

- (a) **Phase = `RESOLVE_FAQ`** — i.e., `current_phase == RESOLVE`
  AND `active_use_case ∈ FAQ-path UCs` (per §4.2). The existing
  `PhaseEvaluator` RESOLVE-FAQ branch at `:598-665` is the scope
  marker.
- (b) **Tool call = `record_outcome`** with `class=resolve` (or
  the legacy `RESOLVED` alias per `RecordOutcomeTool.normalizeOutcomeClass`
  at `:110-121`).
- (c) **Check = `source_id` citation present in the user-facing
  message**. The predicate verifies that the bot's intended
  customer-facing message (the LLM's user_message output on the
  same turn the `record_outcome` is being called, OR the prior
  bot turn's user_message if `record_outcome` is called on a
  no-tool-message turn) contains a non-empty `source_id` citation
  token. The predicate does NOT judge citation correctness,
  citation relevance, or citation content — only presence.

The predicate does NOT fire when:

- The tool call is `record_outcome(class=escalate)` or
  `record_outcome(class=abandon)` — the authorization is explicit:
  "only applies when the bot attempts `record_outcome(class=resolve)`".
- The phase is RESOLVE-INTAKE, CONFIRM, CLOSE, ESCALATE, or
  DISCOVER — the authorization is explicit: "inside the
  `RESOLVE_FAQ` scope".
- The phase is RESOLVE but the active UC is INTAKE-path (UC-G /
  UC-H / UC-I / UC-J / UC-K). The S1 predicate is FAQ-path scoped
  per §4.2.

### 4.4 Predicate-fail behaviour (D2 picks)

**Default option (recommended by this design freeze; deliver-agent
+ human confirm at Sprint 37 planning round):** **Rejection
(reject-and-hint)**, mirroring the existing
`shouldRejectPrematureResolveOutcome` and
`shouldRejectIncompleteIntakeHandover` patterns. The `record_outcome`
call is rejected at the dispatch site in `AgentRunLoopImpl`
(adjacent to the existing line 355-356 dispatch for
`shouldRejectPrematureResolveOutcome`); the reject reason is
`s1_citation_presence_required` (or equivalent canonical label;
Sprint 37 picks the exact constant name); the accumulated_tool_results
slot for `record_outcome` carries:

```
{
  "error": "s1_citation_presence_required",
  "hint": "The user-facing message must include a source_id citation
           from a successful resolve_article call before record_outcome
           with class=resolve can persist. Cite the relevant article."
}
```

The LLM sees the error on the next loop iteration and retries; the
loop's `maxToolSteps=4` on RESOLVE-FAQ allows one or two retries
within the same turn. If the loop exhausts steps, the existing
MAX_STEPS terminal-outcome path (`PhaseEvaluator.java` MAX_STEPS
mapping) handles fall-through.

**Alternative option (deferred; deliver-agent + human revisit if
default behaves badly on observed traces):** Downgrade the
`outcome_class` from `resolve` to a NEW `resolve_no_citation`
class for trace-observation only. **NOT** recommended for default
because it implies inventing a new outcome class enum value
(touches `RecordOutcomeTool.normalizeOutcomeClass` +
`SessionOutcome.outcome` persistence + downstream eval) — a
larger surface than the human authorization scope allows. Carry
as an option for Sprint 37 planning round.

### 4.5 Predicate-input data surface

The predicate at decision time reads:

- The tool call arguments (already available at dispatch site).
- The bot's user_message for the current turn (or the prior bot
  turn's message if the record_outcome is being called on a
  no-user-message turn). The user_message comes from the LLM's
  response; available in the dispatch loop at `AgentRunLoopImpl`
  step 4 / step 5 (the existing `lastLlmRawResponse` at line 156
  + the parsed user_message from `actionParser`).
- The accumulated_tool_results for `resolve_article` and
  `search_knowledge` (already available; needed to verify a
  successful resolve_article ran with a `source_id` field — the
  citation token in the message must match).

The citation-presence check is principle-level: a `source_id` token
(typically the article slug `ka41r000000LIEXAAY` or equivalent;
schema per `resolve_article` tool output) appears literally in the
user-facing message. The predicate does NOT semantically validate
the citation; that judgement is LLM-owned per §1.3.

### 4.6 What the S1 predicate does NOT do (out-of-scope enumeration)

Per the human authorization §4.1, the S1 predicate does NOT:

- Enforce citation on `record_outcome(class=escalate)` — escalations
  do not require citation. Per Constitution: `request_handover`
  carries the escalation_reason; the runtime does not require
  grounding for an escalation.
- Enforce content-quality of the citation (e.g., does NOT verify
  the cited article is *the most relevant* article; does NOT
  re-score the citation against the user question; does NOT
  re-rank). Content judgement is LLM-owned per §1.3.
- Fan out to other phases (DISCOVER / CONFIRM / CLOSE / ESCALATE /
  RESOLVE-INTAKE). Scope is fenced to RESOLVE-FAQ per §4.3 (a).
- Enforce a generic Java grounding-citation gate across the runtime.
  This is the bounded inversion of `D-hard-citation-gate` per
  `docs/milestone_objective.md` §6 #4 + `docs/action_bank.md`;
  the generic-gate deferral remains valid.
- Inspect the citation text for any per-CaseSpec / per-trace
  matching (§1.7 forbidden — no encoding of visible-eval text).

### 4.7 Out-of-scope expansion is a Sprint 36 STOP condition

Per `docs/sprint_objective.md` §10 stop condition #3 (and per
`docs/milestone_objective.md` §10 stop condition #1, which extends
beyond Sprint 36 implementation to all M2-Skill sub-sprints):
Sprint 37 expansion of the predicate beyond §4.3 (a)/(b)/(c) is a
STOP condition. Codex Sprint 37 review must verify the predicate
scope matches §4.3 verbatim. If Sprint 37 implementation surfaces
that the §4.3 scope is too narrow to prevent the M1 Codex Finding
1 Alice fabrication shape (i.e., the predicate misses some
fabrication channel within RESOLVE-FAQ), the response is **STOP
+ surface to deliver-agent + human + Codex per-sub-sprint review**,
NOT an in-Sprint-37 broadening.

## 5. D4 — S2 (`Resolve.Intake.CollectAndHandover`) trigger + predicate

### 5.1 S2 trigger conditions

The S2 envelope is **active** (per D1) when:

- `current_phase == RESOLVE`
- AND `active_use_case ∈ {UC-G, UC-H, UC-I, UC-J, UC-K}` — the
  INTAKE-path UCs per the existing `PhaseEvaluator.INTAKE_UCS`
  set at `:30` AND the existing `IntakeFieldsRegistry.isIntakeUseCase`
  predicate. Source-of-truth is the `INTAKE_UCS` set; Sprint 38
  predicate reads the set, NOT a hardcoded UC list in the predicate
  body.

The S2 envelope teaching paragraph (Sprint 38 ships) is universal
across INTAKE-path UCs — it names the principle-level field-by-field
collection cadence, not per-UC content. The existing RESOLVE-INTAKE
`PhasePlan` at `PhaseEvaluator.java:552-596` already parametrizes
by `activeUc` for the team name + the per-UC `buildIntakeSystemInstruction`
helper; Sprint 38 extends with the envelope teaching at the
system_prompt.txt level.

### 5.2 S2 terminal predicate scope

The S2 terminal predicate fires when ALL of the following hold:

- (a) **Phase = `RESOLVE_INTAKE`** — i.e., `current_phase == RESOLVE`
  AND `active_use_case ∈ INTAKE-path UCs` (per §5.1).
- (b) **Tool call = `request_handover`** with
  `escalation_reason == intake_complete_for_uc_X` where X is the
  active UC's lowercase suffix (per existing
  `AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover` at
  `:644-645`).
- (c) **Check = `requiredIntakeFields` not fully populated in
  `session.intakeFields`**. The predicate reads `IntakeFieldsRegistry.intakeComplete(activeUc, parsedFields)`
  (per existing implementation at
  `IntakeFieldsRegistry.java:184-193`). The `requiredIntakeFields`
  source is `IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC` (NOT a
  field on `UseCaseDefinition`; **see premise correction note in
  §8**).

**This predicate is already shipped** as
`AgentRunLoopImpl.shouldRejectIncompleteIntakeHandover` per §1.2.
Sprint 38's contribution is the envelope teaching paragraph + the
optional reframing of the reject-and-hint behaviour into a
canonical-reason downgrade (per §5.3 option (B)); the predicate
*logic* is already in production.

### 5.3 Predicate-fail behaviour (D2 picks + open question)

**Option (A) — keep the existing reject-and-hint pattern**
(currently shipped at `AgentRunLoopImpl:317-340`). The
`request_handover(intake_complete_for_uc_X)` call is rejected;
accumulated_tool_results carries the
`intake_required_fields_missing_for_intake_complete` error + a
hint listing the missing fields per
`IntakeFieldsRegistry.fieldsRemaining(activeUc, collected)`. The
LLM retries on the next iteration.

**Option (B) — canonical-reason downgrade to `incomplete_intake`**
(per Sprint 5 F2 §3 S2 phrasing — "if candidate is
`intake_complete_for_uc_X` and `session.intakeFields` is missing
any required field for active UC, downgrade to `incomplete_intake`.
Mirrors the §E1 pattern of refusing an un-earned Tier-X reason").
The `request_handover` call proceeds; the canonical
`escalation_reason` persisted is `incomplete_intake` (priority 25
per `EscalationReasonResolver.PRIORITY:103`) instead of
`intake_complete_for_uc_X` (priorities 20-24).

**Default recommended by this freeze:** Option (A) — keep existing
behaviour. Sprint 38 surfaces the envelope teaching paragraph
without changing the predicate's reject-and-hint mechanics. The
existing pattern is already in production, already tested
(`Sprint71PartialIntakePersistenceTest` per M1 Sprint 34 work), and
already mirrors the §E1 principle ("refuse an un-earned reason")
at the dispatch-site rejection level rather than the
canonical-reason resolver level.

**Open question for Sprint 38 planning round (D4.1):** does
Option (B) provide additional architectural clarity (the canonical
reason aligns with the precedence table) that justifies the
mechanic change, OR does Option (A) preserve a simpler dispatch
path (one fewer reason-resolution step)? Deliver-agent + human
decide at Sprint 38 planning round.

### 5.4 Predicate-input data surface

The predicate at decision time reads:

- The tool call arguments (already available at dispatch site).
- The active UC (`plan.useCase()` — already available).
- The session's intake_fields JSON blob (`session.getIntakeFields()`
  — already available).
- The required-fields list for the active UC
  (`IntakeFieldsRegistry.requiredFieldsFor(activeUc)` — already a
  static method).

The predicate **does NOT** judge field content quality. Only
presence. Content judgement is LLM-owned per §1.3. The existing
`IntakeFieldExtractor` (M1 Sprint 34) populates `session.intakeFields`
from form context + LLM inline fields; the predicate consumes the
populated state.

### 5.5 What the S2 predicate does NOT do (out-of-scope enumeration)

- Enforce which fields are required per UC. The required-fields set
  is registry-driven (`IntakeFieldsRegistry.REQUIRED_FIELDS_BY_UC`)
  — read at predicate time, not hardcoded in the predicate body.
- Judge field content quality (e.g., does NOT verify the user's
  email is a valid email; does NOT verify the user's repro_steps
  contain enough detail). Content judgement is LLM-owned.
- Fan out to non-INTAKE-path UCs.
- Fan out to non-`intake_complete_for_uc_X` escalation reasons.
  `user_requested`, `user_distress`, `out_of_scope`, real Tier-2
  reasons all pass through unchanged (per existing
  `shouldRejectIncompleteIntakeHandover` line 644-647 check).
- Modify `IntakeFieldsRegistry` schema. Registry is read-only at
  predicate time.

## 6. D5 — UC-switching wide continuity invariant matrix

Per Q3 human pick at M2-Skill approval round 2026-05-17: **Wide
full continuity**. Per `docs/milestone_objective.md` §2 + §10 stop
condition #5: the continuity invariants are stated as principles +
observable-state guards (registry-level intersection check), NOT as
per-UC-pair branch tables (§1.7 enforced).

Sprint 39 implements this matrix. Sprint 36 design freeze locks
the matrix shape.

### 6.1 Continuity dimensions (rows)

The continuity matrix has these dimensions (the per-row what-survives
analysis):

| # | Dimension | Source | Persistence shape |
|---|---|---|---|
| 6.1.A | `accumulated_tool_results` | per-run loop accumulator at `AgentRunLoopImpl:153` | In-memory per-loop; persisted to trace at session close. |
| 6.1.B | `session.customerContext` | `BotSession.customerContext` field | Persisted (DB column). |
| 6.1.C | `session.intakeFields` (partial state) | `BotSession.intakeFields` JSONB | Persisted; populated by `IntakeFieldExtractor` + LLM inline. |
| 6.1.D | Prior citations / grounding-history | Derivable from past `resolve_article` calls in trace; **NEW projection slot** `prior_use_case_carry` aggregates as soft signal | Derived per-turn from trace; NEW projection field. |
| 6.1.E | Skill terminal predicate state (e.g., S1 citation check result; S2 intake-completeness check result) | Currently per-call; not session-scoped | NOT preserved across UC switch — terminal predicates are scoped to the active skill at the time of dispatch. |

### 6.2 Behaviour-on-switch (columns)

Each cell of the matrix names one of these column behaviours when
the active UC changes from UC-X to UC-Y mid-session:

| Code | Column | Meaning |
|---|---|---|
| **S** | Survives unconditionally | The data is preserved as-is across the switch. The LLM sees it in the projection / accumulator on the next turn. |
| **C** | Survives if compatible | The data is preserved only when an observable registry-level check passes (e.g., field keys intersect with the new UC's required fields). Per-UC-pair compatibility is NOT a branch table; it is a single intersection rule at the principle level. |
| **N** | Surfaces as soft signal | The data is preserved AND projected as a NEW soft-signal slot for the LLM to read; the LLM owns whether to surface to the user, ignore, or ask a clarifying question. |
| **R** | Resets | The data is dropped at the switch boundary; the new UC starts clean on this dimension. |
| **L** | Scoped to current UC only | The data is per-skill / per-active-UC by design and was never session-scoped; the switch is a no-op. |

### 6.3 Invariant matrix (filled)

| Dimension \ Behaviour | S | C | N | R | L |
|---|---|---|---|---|---|
| 6.1.A `accumulated_tool_results` | ✓ |  |  |  |  |
| 6.1.B `session.customerContext` | ✓ |  |  |  |  |
| 6.1.C `session.intakeFields` partial state |  | ✓ |  |  |  |
| 6.1.D Prior citations / grounding-history (NEW `prior_use_case_carry` slot) |  |  | ✓ |  |  |
| 6.1.E Skill terminal predicate state |  |  |  |  | ✓ |

### 6.4 Per-cell rationale

- **6.1.A `accumulated_tool_results` → S (survives).** Tool results
  are evidence collected during the run; they remain factually
  valid regardless of which UC the bot is now in. (Example: a
  `get_customer_context` result for the same user is valid whether
  the active UC is UC-A or UC-C; the customer's email and account
  state did not change because the UC label flipped.) Sprint 39
  implements: do not clear `accumulatedToolResults` at UC switch.
- **6.1.B `session.customerContext` → S (survives).** Persisted
  customer context is a property of the user, not of the UC. The
  existing `BotSession.customerContext` field is already
  session-scoped per Sprint 11 §M0; Sprint 39 implements: confirm
  no per-UC-switch reset path exists in `ControlKernel` /
  `SessionManager`.
- **6.1.C `session.intakeFields` partial state → C (survives if
  compatible).** Intake fields are per-UC-required; some fields may
  apply across UCs (e.g., `email` is general). Sprint 39 implements
  a **registry-level intersection check**:
  - For each field key in `session.intakeFields`:
    - Look up the field's canonical name via
      `IntakeFieldsRegistry.canonicalFieldName(key)`.
    - Preserve the field iff the canonical name appears in
      `IntakeFieldsRegistry.requiredFieldsFor(newActiveUC)`.
    - Drop fields whose canonical name is not in the new UC's
      required-fields set.
  - This is a single rule at the principle level. It is NOT a
    per-UC-pair branch table. The matrix lists no per-UC-pair
    behaviour; the registry data is the source.
- **6.1.D Prior citations / grounding-history → N (soft signal,
  NEW `prior_use_case_carry` projection slot).** Sprint 39
  implements a NEW projection slot in `ContextProjectionBuilder`
  adjacent to the existing `alternate_candidate_use_cases` /
  `discover_disambiguation_signals` slots. The slot carries:
  - A short list of prior `source_id` citations the bot has emitted
    in the current session (cap at, e.g., 3 most recent; deliver-agent
    + human confirm cap at Sprint 39 planning round).
  - The prior UC label associated with each citation.
  - An aging-out behaviour: citations older than N turns drop off
    (deliver-agent + human pick N; default 4 turns covering one
    full RESOLVE-FAQ loop).
  - **LLM-owned read.** Sprint 39 envelope teaching paragraph
    (sibling to S1 / S2 envelopes) names the slot's provenance and
    the soft-signal posture: the LLM reads, the runtime does not
    enforce action on the slot's value.
- **6.1.E Skill terminal predicate state → L (scoped to current UC
  only).** The S1 citation predicate fires per `record_outcome`
  call within RESOLVE-FAQ scope. The S2 intake-complete predicate
  fires per `request_handover(intake_complete_for_uc_X)` call within
  RESOLVE-INTAKE scope. Both are dispatch-time predicates, not
  session-state predicates. A UC switch is a no-op on this dimension
  — the new active skill's predicates will fire when their dispatch
  conditions hold; the prior skill's predicate evaluations are not
  preserved.

### 6.5 The §1.7 enforcement — registry-level intersection check, NOT per-UC-pair branch

The above matrix is FIVE rows × FIVE columns and reads as ONE
principle per row:

- `accumulated_tool_results`: keep.
- `customerContext`: keep.
- `intakeFields`: keep iff the canonical field name appears in the
  new UC's required set (registry-driven).
- Prior citations: project as soft signal; LLM owns next step.
- Predicate state: per-dispatch, not per-session.

There is no per-UC-pair logic ("if old UC is UC-A and new UC is
UC-C, preserve X; if old UC is UC-A and new UC is UC-D, preserve
Y"). The behaviour at each cell is uniform across UC pairs;
per-UC variation enters only via the registry-level intersection
check on 6.1.C, which is a single intersection rule (not a
per-UC-pair branch).

This honors `docs/sprint_objective.md` §10 stop condition #9 and
`docs/milestone_objective.md` §6 hard fence #1.

### 6.6 The `prior_use_case_carry` slot shape

Sprint 39 builds the slot. Sprint 36 freeze locks the shape:

```jsonc
{
  "prior_use_case_carry": {
    "prior_citations": [
      {"source_id": "ka41r000000LIEXAAY",
       "from_use_case": "UC-A",
       "turn_index": 3},
      {"source_id": "ka41r000000LIFJAA4",
       "from_use_case": "UC-A",
       "turn_index": 5}
    ],
    "prior_active_use_case": "UC-A",
    "ages_out_after_turns": 4
  }
}
```

Empty / null when no prior UC switch has occurred or when the
aging-out window has elapsed.

### 6.7 Open question — D5.1 (intake-field intersection edge case)

Open question for Sprint 39 planning round: when the registry-level
intersection check (per 6.1.C above) drops a field, does the runtime
project the dropped fields as a NEW soft-signal slot
`carried_intake_fields_dropped_at_switch` so the LLM can decide
whether to re-collect or ignore? OR does the runtime silently drop
the fields with no projection trace?

Recommended default: project the dropped fields as a soft signal
(LLM-readable; LLM-owned action). Deliver-agent + human confirm at
Sprint 39 planning round.

## 7. D6 — §4.1 nine-question anti-hardcode kernel walk-through

This section walks the nine §4.1 anti-hardcode kernel questions
against the proposed D1-D5 design BEFORE Sprint 37 / 38 / 39
implement. Each question is stated verbatim from
`docs/current/iteration_governance.md` §4.1.

### Q1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision (drift detection, escalation, UC selection, risk classification, follow-up, intake routing)?

**Design's answer: NO.** The Sprint 36 freeze adds zero runtime
code. Sprint 37 + 38 add terminal predicates that protect
Runtime-owned floor (citation presence; intake-completeness); these
are NOT semantic decisions (the LLM still owns next-action,
response strategy, UC hypothesis). Sprint 39 adds a soft-signal
projection slot + a registry-level intersection check on intake-field
carry-over; the check is a single rule at the principle level (not
per-UC-pair branch). Cited: D1 §2.2 (envelope is teaching, not
enforcement); D3 §4.6 (S1 out-of-scope enumeration); D4 §5.5 (S2
out-of-scope enumeration); D5 §6.5 (registry-level intersection
check, not per-UC-pair).

### Q2. If yes to (1), is the change justified as protecting a current Tier-0 invariant named in `docs/runtime_freeze_and_risk_policy.md` §1 / §2?

**Design's answer: N/A (Q1 is NO).** However, see §8 below — the
S1 citation predicate semantics may qualify as Tier-0 territory
worth elevating; the design freeze surfaces this as a candidate
question for human-review escalation.

### Q3. Could the same outcome be achieved by projecting a soft signal to the LLM (an additional projected slot, a candidate list, a diagnostic flag) instead of a hard branch in Java or the prompt?

**Design's answer: PARTIALLY YES; partially NO.** For D5
UC-switching continuity, the answer is YES — the design IS a
soft-signal projection (`prior_use_case_carry`) plus a registry-level
state-preservation rule. For D3 S1 citation predicate, the answer
is NO at the terminal predicate level — the Runtime-owned grounding
floor is by definition a Java guard per Constitution §1.4 (the
human authorization §6 #4 makes this explicit: a narrow citation
gate inside RESOLVE-FAQ). The envelope teaching (D1) IS a soft
surface; the predicate (D3 + D4) is a hard surface, bounded to the
Runtime floor per §1.4. Cited: D2 §3.3 (predicate scope discipline);
D3 §4 (human authorization).

### Q4. Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime, prompt, or judge config?

**Design's answer: NO.** The freeze names trace shapes (e.g.,
"the bot fabrication shape described in M1 Codex Finding 1") in
prose, but does NOT encode CaseSpec ids (Alice's CaseSpec id, any
`cs_interactive_*` id) into runtime / prompt / judge. The S1
citation predicate checks for `source_id` token presence (a
schema-level check), not for any specific cited article slug. The
predicate does NOT match any user message text. Cited: D3 §4.6
("Inspect the citation text for any per-CaseSpec / per-trace
matching — §1.7 forbidden").

### Q5. Does the change move semantic ownership from the LLM to Java — that is, shrink what `docs/current/iteration_governance.md` §1.3 says the LLM owns?

**Design's answer: NO.** The Skill envelope (D1) explicitly states
the LLM still owns: per-step argument choice; recommended-order
deviation; customer-facing language; UC hypothesis; drift / topic
shift; next-action choice; response strategy; escalation posture.
The terminal predicates (D3 + D4) enforce ONLY the Runtime-owned
floor per §1.4 (citation presence; intake-completeness). Cited:
§1 introductory paragraph; D2 §3.3 (predicate scope discipline).

### Q6. Does the change add an if-else block to the prompt instead of principle-level or observable-state guidance?

**Design's answer: NO.** D1 §2.1 names the envelope teaching
paragraph as principle-level. The proposed shape is sibling to the
existing `already_called` / `alternate_candidate_use_cases` /
`discover_disambiguation_signals` paragraphs, which are all
principle-level. The S1 envelope paragraph names the recommended
order ("search → resolve → grounded answer with citation →
record_outcome") at the principle level, not as a per-UC if-else.
Cited: D1 §2.1 (envelope teaching paragraph shape); D1 §2.2
(rationale — sibling to existing precedent).

### Q7. Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?

**Design's answer: YES** — the change extends the grounding floor
NARROWLY (per the human authorization §6 #4). Tool schema unchanged
(no new tools, no new tool fields). Capability / permission boundary
unchanged (no new tool granted to LLM). PII / safety floor
unchanged. Grounding floor: the S1 citation predicate extends the
existing grounding-floor diagnostics per `faq_grounding_contract.md`
(the `L1:source_citation_present` boundary already named in the
contract) from observation-only to dispatch-time guard, NARROWLY
scoped to RESOLVE-FAQ `record_outcome(class=resolve)` only. Cited:
D3 §4.3 (predicate scope); D3 §4.6 (out-of-scope enumeration).

### Q8. Does the PR ship generalization eval coverage — target, neighbor, negative, and shadow cases — and not only the target case?

**Design's answer:** Sprint 36 itself ships zero behaviour change,
so target/neighbor/negative/shadow generalization eval does not
apply to Sprint 36. The Track B prospective stanza in
`docs/sprint_objective.md` §8 (and re-stated in the Sprint 36
handoff §6) names the generalization coverage Sprints 37 / 38 / 39
will ship:

- Sprint 37 (S1): Target = Alice bad case multi-trace closure-criterion
  (a); Neighbor = other FAQ-path UC traces (UC-B / UC-C-FAQ /
  UC-D-FAQ / UC-E / UC-F-FAQ / UC-FP-FAQ); Negative =
  `record_outcome(class=escalate)` traces (predicate must NOT
  fire); Shadow = held-out FAQ-grounded-resolve traces.
- Sprint 38 (S2): Target = cs_066 (UC-K); Neighbor = cs_036 (UC-I)
  + cs_038 (UC-J) + cs_040 (UC-K) regression guards + Alice
  UC-G/H carry-over; Negative = legitimate `intake_complete_for_uc_X`
  traces with all fields populated (predicate must NOT downgrade);
  Shadow = held-out UC-G/H/I/J/K intake traces.
- Sprint 39 (UC-switching wide continuity): Target = a synthetic
  UC-switching trace (e.g., UC-A → UC-C mid-flow); Neighbor =
  other UC-pair transitions per the Sprint 36 freeze D5 matrix;
  Negative = single-UC traces (no switch; continuity invariants
  must NOT trigger spurious carry-over); Shadow = held-out
  UC-switching traces.

Cited: `docs/sprint_objective.md` §8 Track B table; Sprint 36
handoff §6 Track B re-statement.

### Q9. If the change is temporary, does it carry an explicit rollback or sunset plan (downgrade-to-signal trigger, retirement sprint id)?

**Design's answer: N/A.** The S1 / S2 predicates protect the
Runtime-owned floor per §1.4 (a permanent architectural surface);
the UC-switching continuity invariants are permanent state-management
rules. None of D1-D5 is temporary. If observed traces show that
S1's narrow predicate is insufficient to prevent the M1 Codex
Finding 1 Alice fabrication shape (i.e., fabrication channels exist
within RESOLVE-FAQ that the citation-presence check does NOT
catch), the response is a NEW design round (Sprint 37 fix-iteration
or Sprint 40+) — NOT a sunset of S1.

### Q-walk verdict

`approve` — the proposed D1-D5 design does not introduce a
semantic hardcode; the terminal predicates honor §1.4 boundary;
the envelope teaching honors §1.3 boundary; the UC-switching matrix
honors §1.7 (no per-UC-pair branch). One Tier-0 candidate question
surfaces from D3 + D4 predicate semantics — written up in §8
below for human-review escalation per `docs/milestone_objective.md`
§10 stop condition #1.

## 8. Open questions / Tier-0 candidate write-up

### 8.1 Premise correction surfaced at Sprint 36 dev session

`docs/sprint_objective.md` §4 premise #8 cites
`UseCaseRegistryService.UseCaseDefinition.requiredIntakeFields` as
the source for per-UC required intake fields. At HEAD `eb65e2b`
spot-check (2026-05-17), `UseCaseDefinition` at
`UseCaseRegistryService.java:166-173` is a 6-field record
(`ucId / name / topicSubjects / riskLevel / allowBotResolution /
path`) with NO `requiredIntakeFields` field. The actual source is a
SEPARATE static registry class `IntakeFieldsRegistry` with the
`REQUIRED_FIELDS_BY_UC` map + public static helpers
`requiredFieldsFor(uc)`, `isIntakeUseCase(uc)`,
`fieldsRemaining(uc, collected)`, and `intakeComplete(uc, collected)`.

The premise *intent* (registry-driven source, no schema change)
holds; only the class name in the premise is wrong. The design
freeze cites the correct source (`IntakeFieldsRegistry`). Surfaced
to deliver-agent + human for Sprint 38 contract draft accuracy.

### 8.2 Material finding — S1 + S2 partial implementations already shipped

Per §1.2 above. Sprint 6 §G2 / Sprint 7 §I2 / Sprint 11 §M1 work
already ships partial implementations of what the Sprint 5 (F2)
proposal scoped as S1 + S2 predicates. The design freeze formalizes
+ extends these into the Skill envelope framing. Sprint 37 + Sprint
38 contracts should be drafted with the understanding that the
narrow NEW work is (S1) the citation-presence predicate + envelope
teaching paragraph; (S2) the envelope teaching paragraph + an
optional rephrasing of the existing reject-and-hint as a
canonical-reason downgrade.

### 8.3 Tier-0 candidate question — S1 citation predicate semantics

**Question:** does the S1 citation predicate per §4.3 (a)/(b)/(c)
qualify as a candidate Tier-0 invariant for
`docs/runtime_freeze_and_risk_policy.md` §1 / §2?

**Why this question matters:**

- The S1 predicate enforces a grounding-floor invariant: "the
  runtime SHALL NOT persist a `record_outcome(class=resolve)` on a
  RESOLVE-FAQ plan when the user-facing message lacks a `source_id`
  citation."
- Constitution §1.4 names "grounding floor for factual claims" as a
  Runtime-owned responsibility. The S1 predicate IS a concrete
  manifestation of this responsibility.
- The current `runtime_freeze_and_risk_policy.md` §1.1 / §2 names
  the frozen runtime contract: §2 rule 6 says
  "`record_outcome(resolve)` requires disposition + guard approval"
  (Sprint 11 §M1 + Sprint 11.1; the existing premature-resolve
  guard). The S1 citation predicate adds a SECOND condition on the
  same call site: in addition to phase-disposition approval, the
  user-facing message must carry a citation.
- Adding "citation presence" as a Tier-0 invariant alongside the
  existing "disposition + guard approval" would make the floor
  explicit + auditable + frozen, preventing future drift.

**Why this question is open (rather than decided by the freeze):**

- The human authorization at §6 #4 (2026-05-17) is narrow —
  authorizing the S1 predicate scope, NOT explicitly authorizing
  a Tier-0 elevation. Sprint 36 design freeze must NOT silently
  promote a predicate semantics to Tier-0 invariant status; that is
  the human-review-escalation gate per
  `docs/milestone_objective.md` §10 stop condition #1.
- The current Tier-0 surface in `runtime_freeze_and_risk_policy.md`
  is conservative (intentionally; Sprint 13 § purpose). Adding a
  Tier-0 row has downstream test-coverage + freeze-discipline
  implications. The deliver-agent + human + Codex per-sub-sprint
  review at Sprint 36 close evaluate this.

**Escalation request (per `docs/milestone_objective.md` §10 stop
condition #1):** the deliver-agent + human + Codex evaluate this
candidate at Sprint 36 close. If escalation is **authorized** AND
the human decides to add the Tier-0 invariant, deliver-agent commits
the `runtime_freeze_and_risk_policy.md` §1 / §2 addition as a
SEPARATE commit (per
`feedback_commit_at_end_bundles_deliver_artefacts.md`) BEFORE
Sprint 37 begins. The new Sprint 37 §7 stanza Tier-0 invariant line
then cites the added invariant id. If escalation is **declined** or
**deferred**, Sprint 37 proceeds without the Tier-0 addition; the
candidate stays open in `docs/action_bank.md` for M3+ revisit.

**Recommended default (this design freeze's recommendation; deliver-
agent + human + Codex have final say):** **DEFER** — the S1
predicate is a narrow surface inside the broader grounding-floor
discipline; elevating to Tier-0 before observing how the implemented
predicate behaves in production traces (Sprint 37 + post-Sprint-37)
may freeze a sub-optimal exact phrasing. Carry the question forward;
re-evaluate at Sprint 37 close OR M2-Skill close based on the
observed Alice multi-trace closure-criterion (a) evidence.

### 8.4 Tier-0 candidate question — S2 intake-completeness predicate semantics

**Question:** does the S2 intake-completeness predicate per §5.2
qualify as a candidate Tier-0 invariant for
`docs/runtime_freeze_and_risk_policy.md` §1 / §2?

**Why this question matters:** symmetric to §8.3 — the S2 predicate
enforces a capability-floor invariant: "the runtime SHALL NOT
persist `request_handover(intake_complete_for_uc_X)` when the
required intake fields are missing." Constitution §1.4 names
"capability / permission boundary" as Runtime-owned.

**Why this question is open:**

- The S2 predicate is ALREADY SHIPPED in `AgentRunLoopImpl` (per
  Sprint 7 §I2 + §1.2 above). Elevating to Tier-0 would
  retroactively formalize an existing predicate; no new code-surface
  change.
- The existing `runtime_freeze_and_risk_policy.md` does NOT name
  the Sprint 7 §I2 guard as a Tier-0 row; whether this absence is
  by design or by oversight is itself an open question for the
  deliver-agent + human.

**Recommended default:** **DEFER** — same reasoning as §8.3. The
S2 predicate has been in production since Sprint 7; the deliver-
agent + human can elevate at any fold-back cadence if they choose,
without urgency.

### 8.5 D3 sub-decision open question — predicate-fail behaviour

Per §4.4: default recommended is **rejection (reject-and-hint)**;
alternative is **downgrade to a new `resolve_no_citation` outcome
class**. Deliver-agent + human pick at Sprint 37 planning round.

### 8.6 D4 sub-decision open question — reject-and-hint vs canonical-reason downgrade

Per §5.3: default recommended is **Option (A) keep existing
reject-and-hint pattern**; alternative is **Option (B)
canonical-reason downgrade to `incomplete_intake`**. Deliver-agent
+ human pick at Sprint 38 planning round.

### 8.7 D5 sub-decision open question — dropped-intake-fields trace surface

Per §6.7: open question whether the registry-level intersection
check projects dropped fields as a soft-signal slot
(`carried_intake_fields_dropped_at_switch`) or silently drops with
no trace. Default recommended: project. Deliver-agent + human pick
at Sprint 39 planning round.

### 8.8 D1 sub-decision open question — explicit `skill_envelope` projection field

Per §2.3: default no; rely on existing projection fields. Deliver-
agent + human revisit at Sprint 37 planning round if observed
traces show the LLM benefits from an explicit per-turn skill name.

### 8.9 D5 sub-decision open question — `prior_use_case_carry` cap + aging

Per §6.6: default cap = 3 most recent citations, aging-out after 4
turns. Deliver-agent + human confirm at Sprint 39 planning round
based on observed UC-switching trace shapes.
