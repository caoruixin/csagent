---
title: Skill Orchestration Candidates — Sprint 5 (F2)
doc_tier: proposal
status: superseded
implementation_status: not_started
runtime_contract: false
last_reviewed: 2026-05-10
review_cadence: on_reactivation
superseded_by: docs/proposals/skill_foundation_design.md
notes: >
  Self-declared diagnostic that proposes candidate skill / plan-template
  shapes (e.g. Resolve.FAQ, intake-collect-and-handover) without
  implementing them. Use as a design starting point when a sprint picks
  up skill orchestration; do not treat any candidate as a current runtime
  feature. Superseded 2026-05-17 by `docs/proposals/skill_foundation_design.md`
  (Sprint 36 / M2-Skill sub-sprint 1 design freeze). Body retained as
  upstream reasoning archive per `doc_governance.md` "Forward-looking
  proposal docs are first-class citizens"; the M2-Skill freeze refines
  the F2 candidates (S1 + S2 only; S3 / S4 / S5 deferred to M3-C).
---

> This document is not the current runtime contract unless a docs/current/* contract or live code path confirms it.

# Skill Orchestration Candidates — Sprint 5 (F2)

Date: 2026-05-05
Sprint: Prompt / Context Projection and Fix-Layer Diagnostic Sprint 5
Status: diagnostic — proposes candidate skill / plan-template shapes but
does NOT implement them. Per `docs/sprint_objective.md` §"Do not
implement" → "new skill runtime framework".

## 1. What "skill orchestration" means here

A "skill" or "plan template" is a deterministic multi-step state machine
that wraps a recurring multi-tool / multi-turn flow. The runtime invokes
it as a phase plan; the LLM still produces the final user-facing
language and the per-step tool arguments, but the *sequence* and the
*terminal-state evaluation* are deterministic.

Today the V1 runtime expresses this only at the phase granularity:
`PhaseEvaluator.plan(...)` produces a `PhasePlan` with `allowedTools`
and `objective`, and the AgentRunLoop iterates up to `maxToolSteps`. A
"skill" would be a finer-grained PhasePlan variant — same `PhasePlan`
shape, but with explicit ordered steps, per-step preconditions, and a
terminal-state predicate.

This file does NOT propose a new framework. The candidates below are
written so they can be implemented as parametrized PhasePlans (or as
small composable pieces that PhaseEvaluator returns) without
introducing a separate "skill engine".

## 2. Why scan now (root-cause framing)

Per `docs/diagnostics/fix_layer_taxonomy.md`, two of the recurring failure modes are
not best fixed by a one-line prompt cue:

- **cs_259** UC-F payment FAQ — r2 evidence shows
  `search_knowledge` was called and UC-F was committed, but the bot
  short-circuited to `request_handover(faq_miss_threshold_exceeded)`
  without running `resolve_article` + grounded answer +
  `record_outcome`. Actual r2 tool sequence:
  `['search_knowledge', 'classify_use_case', 'request_handover']`
  (`lcs=1/4`). The recurring shape is therefore "search happened,
  resolve did not complete" — a multi-step `search_knowledge →
  resolve_article → grounded customer-facing answer →
  record_outcome` sequence (or, alternatively, an explicit handover
  only after a valid resolve attempt cannot complete). This is
  exactly what an S1 `Resolve.FAQ` skill would package.
- **cs_066** UC-K intake — the bot keeps asking clarifying questions
  outside the required intake field set, hitting `turn_budget_exhausted`
  before completing intake. The recurring shape is "for UC-G/H/I/J/K,
  collect required fields one-by-one, then call request_handover".

Each candidate below is anchored to specific cases, has a deterministic
trigger condition, an explicit Java guard boundary, an explicit prompt
responsibility, and a finite set of terminal outcomes.

## 3. Candidate skills

### Skill S1 — `Resolve.FAQ.GroundedAnswer` (FAQ-grounded-resolve)

**Trigger conditions** (deterministic, evaluated by
`PhaseEvaluator.plan`):

- `current_phase == RESOLVE`.
- `active_use_case ∈ {UC-A, UC-B, UC-C (FAQ side), UC-D (FAQ side),
  UC-E, UC-F (FAQ side), UC-FP (FAQ side)}` — i.e. any UC whose
  `path == FAQ` in `UseCaseRegistryService.UseCaseDefinition`.
- The skill triggers in either of two shapes — both observed in
  Sprint 4 smoke evidence:
  1. **search-not-yet-run shape (cs_192-style)**:
     `accumulated_tool_results.search_knowledge` is empty AND a
     factual user-facing answer is about to be emitted.
  2. **search-ran-but-resolve-did-not shape (cs_259 r2-style)**:
     `accumulated_tool_results.search_knowledge` is non-empty AND
     `accumulated_tool_results.resolve_article` is empty AND the
     bot is about to emit `request_handover(faq_miss_threshold_exceeded)`
     or a customer-facing FAQ answer without a citation. The skill's
     terminal predicate must enforce "search → resolve_article →
     grounded answer → record_outcome", OR an explicit handover only
     after a valid resolve attempt cannot complete.

**Required tools** (in deterministic order):

1. `get_customer_context` — runs only if `customer_context` slot is
   empty AND form_context.email is present. Skip otherwise.
2. `search_knowledge` — query is the user's first turn or the form
   description, whichever is non-empty.
3. `resolve_article` — for the top-1 hit if score ≥ threshold.
4. (LLM produces the grounded user_message with citation.)
5. `record_outcome(outcome_class=resolve)` — only when
   `accumulated_tool_results.resolve_article` is present AND the
   user-facing message has a non-empty `source_id` citation.

**Required state**:

- Reads: `session.activeUseCase`, `session.formContext`,
  `session.customerContext`, `accumulated_tool_results`.
- Writes: `accumulated_tool_results.search_knowledge`,
  `accumulated_tool_results.resolve_article`,
  `session.containmentOutcome=resolved` (via record_outcome).

**Terminal outcomes**:

- `RESOLVED` — search returned a hit ≥ threshold AND the LLM's
  grounded answer cites the `source_id`. → CONFIRM phase.
- `ESCALATE_FAQ_MISS` — search returned no hit ≥ threshold (or
  threshold-meeting hits failed `resolve_article`). →
  `request_handover(faq_miss_threshold_exceeded)`.
- `ESCALATE_USER_REQUESTED` — user explicitly asked for human at any
  point. → `request_handover(user_requested)`.
- `MAX_STEPS` — step budget exhausted (`PhasePlan.maxToolSteps`).
  → fall through to existing AgentRunLoop max-steps termination.

**Java guard boundaries** (what the runtime MUST guarantee, regardless
of what the LLM does):

- `ToolDispatcher.validateAgainstPlan` rejects tools outside the skill's
  `allowedTools`.
- `EscalationReasonResolver` precedence still applies — if `user_distress`
  fires via §B1 deterministic detector mid-skill, the resolver still
  stamps `user_distress` and the skill terminates as
  `ESCALATE_USER_DISTRESS`.
- `record_outcome(outcome_class=resolve)` MUST NOT be persisted if the
  user-facing message has no citation (the existing
  `L1:source_citation_present` gate covers this on the eval side; the
  runtime should refuse the call too, via a small guard in
  `RecordOutcomeTool`).

**Prompt responsibilities** (what the LLM owns, the skill does NOT
encode):

- The actual customer-facing language of the grounded answer.
- The choice of which top-N hits to ask `resolve_article` for if the
  top-1 hit is ambiguous (within the skill's max step budget).
- Empathetic reply-shape and brevity (system prompt rules).

**Eval cases that should test it**:

- cs_interactive_192 (UC-B FAQ "give away free items").
- cs_interactive_259 (UC-F payment FAQ "how do I receive payment").
- cs_interactive_001 (UC-C FAQ — currently PASS via §E1 but the skill
  should preserve PASS).
- cs_interactive_011 (UC-D FAQ — currently PASS, regression guard).

**Why this is a skill candidate and not a prompt fix alone**: the
recurring shape is multi-tool + multi-turn AND the terminal-state
predicate (`L1:source_citation_present` + non-empty
`accumulated_tool_results.search_knowledge`) is deterministic. A prompt
sentence can nudge the LLM but cannot guarantee the citation is
present in the response — a skill predicate (refuse `record_outcome` /
`final_answer` without a citation) makes the contract enforceable.

### Skill S2 — `Resolve.Intake.CollectAndHandover` (UC-G/H/I/J/K intake)

**Trigger conditions**:

- `current_phase == RESOLVE`.
- `active_use_case ∈ {UC-G, UC-H, UC-I, UC-J, UC-K}` (path == INTAKE in
  the UC registry).
- The skill steps are parametrized by the UC's `requiredIntakeFields`.

**Required tools** (in deterministic order):

1. (LLM emits a single clarifying question for the *next un-collected
   required field*, using the field name and a per-UC prompt
   fragment.)
2. (Runtime parses the user reply against an extractor — for V1 this
   is the existing implicit pattern, no schema change.)
3. (Runtime updates `session.intakeFields` with the extracted value.
   Java guard.)
4. Repeat 1–3 until `intake_fields_remaining` is empty.
5. `request_handover(escalation_reason=intake_complete_for_uc_X)` —
   X matches the active UC.
6. (`create_case_controlled` runs deterministically *server-side* for
   UC-H / UC-J / UC-K per Phase 3 §3.4.1; the LLM does not call it.)

**Required state**:

- Reads: `session.activeUseCase`, `session.intakeFields`,
  `UseCaseRegistryService.UseCaseDefinition.requiredIntakeFields`.
- Writes: `session.intakeFields` (one new key per turn),
  `session.escalationReason=intake_complete_for_uc_X` (via
  `EscalationReasonResolver`).

**Terminal outcomes**:

- `INTAKE_COMPLETE` → `request_handover(intake_complete_for_uc_X)` →
  ESCALATE phase. Java runtime creates the case for UC-H/J/K.
- `INTAKE_INCOMPLETE_USER_REFUSED` — user explicitly refuses to
  provide a required field, OR `clarification_count` exceeds the
  per-UC budget. → `request_handover(incomplete_intake)`.
- `ESCALATE_USER_REQUESTED` — same as S1.
- `ESCALATE_USER_DISTRESS` — same as S1 (B1 deterministic).
- `MAX_STEPS` — `request_handover(turn_budget_exhausted)` (current
  fall-through).

**Java guard boundaries**:

- `EscalationReasonResolver` precedence: `user_requested` (1) >
  `user_distress` (2) > `intake_complete_for_uc_X` (intake-complete
  family) > `turn_budget_exhausted` (42) > `clarification_budget_exhausted`
  (40). Already implemented; the skill does not change this.
- The skill must NOT allow the LLM to stamp `intake_complete_for_uc_X`
  before all required fields are present in `session.intakeFields`.
  Add a runtime check in `RequestHandoverTool` /
  `EscalationReasonResolver.applyEscalationReason`: if candidate is
  `intake_complete_for_uc_X` and `session.intakeFields` is missing
  any required field for active UC, downgrade to
  `incomplete_intake`. (Mirrors the §E1 pattern of refusing an
  un-earned Tier-X reason.)

**Prompt responsibilities**:

- Per-UC clarifying-question phrasing (one question per missing
  field, friendly tone).
- Empathy / acknowledgment shape.
- Recognising when the user has provided multiple fields in one
  message (e.g. cs_066 r2 turn 4: "Using Chrome on Windows 11. This
  happened after the last site update — used to be right below the
  description box." — this carries `platform=Chrome on Windows 11`
  AND `repro_steps_or_error_message=disappeared after site update`,
  even though those are two fields).

**Eval cases that should test it**:

- cs_interactive_066 (UC-K — currently flapping between
  `intake_complete_for_uc_k` and `turn_budget_exhausted`).
- cs_interactive_036 (UC-I — currently PASS, regression guard).
- cs_interactive_038 (UC-J — currently PASS, regression guard).
- cs_interactive_040 (UC-K — currently PASS, regression guard).

**Why this is a skill candidate and not a prompt fix alone**:
prompt-only fix (F1 §C4 — surface `intake_state.fields_remaining`)
helps the LLM see the missing field, but does not stop the LLM from
asking a *non-required* clarifying question on a turn where the user
has gone off-topic. The skill predicate (refuse to advance phase
until `fields_remaining` is empty AND refuse `intake_complete_for_uc_X`
unless the field set is complete) makes the contract enforceable.

### Skill S3 — `Triage.SoftOOS.ClarifyOrEscalate` (UNKNOWN-topic disambiguation)

**Trigger conditions**:

- `current_phase == DISCOVER`.
- `session.activeUseCase == null` AND `session.candidateUseCases`
  is empty AND `form_context.topic_subject == "UNKNOWN"` (or in the
  ambiguous-topic registry).

**Required tools** (in deterministic order):

1. (LLM emits at most ONE clarifying question OR runs
   `search_knowledge` against the user's first turn — whichever is
   more informative given the user's first message shape.)
2. `classify_use_case(use_case_id, confidence)` — required terminal
   step UNLESS terminal is escalate.

**Terminal outcomes**:

- `CLASSIFIED` — classify_use_case committed with confidence ≥ 0.5.
  → RESOLVE phase (FAQ or INTAKE branch).
- `ESCALATE_OOS` — user's request is genuinely out of scope (e.g.
  hard-OOS topic surfaces post-clarification). →
  `request_handover(out_of_scope)`.
- `ESCALATE_USER_REQUESTED` — same as S1.
- `MAX_STEPS` — single clarifying turn already used; if the next
  user message is still ambiguous, fall through to
  `request_handover(clarification_budget_exhausted)`.

**Java guard boundaries**:

- DISCOVER `maxToolSteps=2` is preserved.
- `ControlKernel.inferFallbackUseCase` (B3) still fires if the skill
  escalates without an `active_use_case` — already implemented.
- **Removed/deferred (Sprint 5.1 codex correction)**: an earlier
  draft of S3 added a runtime check in `RequestHandoverTool` that
  refused `faq_miss_threshold_exceeded` when no
  `search_knowledge` was in `accumulated_tool_results`. cs_259 r2
  evidence shows that `search_knowledge` already ran in the
  observed failure (`['search_knowledge', 'classify_use_case',
  'request_handover']`, `lcs=1/4`), so a "no prior search" guard
  would not address the cs_259 failure. The guard is therefore
  removed from the cs_259 fix and is NOT recommended as a primary
  cs_259 mitigation. The cs_259 primary fix is S1 (FAQ-grounded-
  resolve skill / `PhasePlan` predicate enforcing
  `search_knowledge → resolve_article → grounded customer-facing
  answer → record_outcome`, OR an explicit handover only after a
  valid resolve attempt cannot complete). A "no prior search"
  defensive invariant could still be considered in a future sprint
  for cases that genuinely have no prior search, but it must not be
  promoted as the cs_259 fix.

**Prompt responsibilities**:

- The clarifying question text (one short sentence, friendly).
- The decision between "ask one clarifying question" vs "run search".
- The classify_use_case `reasoning` string.

**Eval cases that should test it**:

- cs_interactive_259 (UNKNOWN topic + FAQ-shaped user turn).
- cs_interactive_029 (UNKNOWN topic + callback request — currently
  PASS via §E2 override, regression guard).

**Why this is a skill candidate and not a prompt fix alone**:
prompt-only fix (F1 §C5 — surface `candidate_use_cases` + DISCOVER
instruction) helps the LLM understand the empty state. The
previously-claimed deterministic predicate "refuse
`faq_miss_threshold_exceeded` without a prior `search_knowledge`
call" does NOT match the cs_259 r2 evidence (search did happen) and
is removed from the cs_259 fix per Sprint 5.1 codex correction. The
cs_259 r2 anti-pattern ("search ran, resolve did not complete") is
owned by S1, not S3.

### Skill S4 — `Triage.Account.LoginRecovery` (UC-D login-issue intake)

**Trigger conditions**:

- `current_phase == RESOLVE`.
- `active_use_case == UC-D`.
- The user's first turn or form description matches the B2 pre-LLM
  bias (`UseCaseRouter.matchAccountMessagingBias` UC-D branch:
  "account locked / can't log in / forgot password").

This is structurally a sub-case of S1 (FAQ-grounded-resolve) for the
account-recovery path: the FAQ surface for cs011-shape "I tried the
reset link and it doesn't work" is genuinely thin in the V1 corpus,
so the skill should:

1. `get_customer_context` (already auto-triggered).
2. `search_knowledge` for password-reset / account-lock articles.
3. If hit found, `resolve_article` + grounded answer.
4. If no hit OR user reports the standard reset flow already failed
   (cs011 pattern: "Your be the third agent today / I followed the
   process and the link is not recognised"), escalate
   `faq_miss_threshold_exceeded` (consistent with cs011 spec).

**Why list as a separate skill instead of folding into S1**: the
"standard reset flow failed" detection requires a small narrow
keyword pattern ("reset link", "not recognised", "third agent", "tried
again"), and the right escalation reason is the FAQ-family
`faq_miss_threshold_exceeded` rather than the bot-limit-family
`turn_budget_exhausted`. Phrased as a separate skill, it can be
tested in isolation.

**Eval cases that should test it**:

- cs_interactive_011 (currently PASS via §E1 + §E3 override, regression
  guard).

**Lower priority than S1/S2/S3**: cs_011 currently passes — list this
skill as a *defensive structural improvement* rather than a Sprint 6
must-land. Including it here so the next sprint can decide whether to
ship S1+S4 as a pair or only S1.

### Skill S5 — `Triage.PolicySensitive.Tier2Reasoning` (UC-FP / UC-G / UC-H boundary cases)

**Trigger conditions**:

- The LLM is about to call `request_handover` with a Tier-2 policy
  reason: `payment_dispute_detected`, `appeal_requires_human`,
  `incorrect_deletion_appeal`, `account_compliance`,
  `trust_safety_required`, `gdpr_intake`,
  `identity_verification_required`.

**Required tools / steps**:

1. (Server-side guard, not LLM-visible.) Before persisting
   `request_handover` with a Tier-2 reason, the runtime checks
   `session.activeUseCase` against the Tier-2 reason's compatible UC
   list:

   - `payment_dispute_detected` → UC-FP (Posting Policies — refund
     adjacency) or UC-G (Payment Issue intake), NOT UC-A / UC-B /
     UC-E.
   - `appeal_requires_human` / `incorrect_deletion_appeal` → UC-FP.
   - `gdpr_intake` → UC-H (GDPR / data request intake).
   - `identity_verification_required` → UC-D (account) or UC-H.
   - `trust_safety_required` → UC-J (Trust & Safety intake).
   - `account_compliance` → UC-D / UC-H.

2. If incompatible (cs_176 pattern: `payment_dispute_detected`
   stamped on UC-E), downgrade to `faq_miss_threshold_exceeded`.

This is essentially the `java_guard` form of F1 §C1 (active_use_case-aware
`request_handover` reason picking). Listed here as a skill to make the
prompt-vs-runtime trade-off explicit:

- F1 §C1 = prompt-only nudge. Lower test cost. Higher residual
  variance under LLM nondeterminism.
- F2 §S5 = runtime guard. Higher test cost (24 mvn integration
  tests, one per Tier-2 reason × UC pair). Lower residual variance.

**Recommendation**: ship F1 §C1 first; if cs_176 still flaps after
prompt fix, add S5 as a follow-up. Do NOT ship both at once.

**Eval cases that should test it**:

- cs_interactive_176 (UC-E + LLM-supplied `payment_dispute_detected`).

## 4. Skill comparison summary

| Skill | Recurring shape | Anchor cases | Prompt-only sufficient? | Java-guard sufficient? | Right fit |
|---|---|---|---|---|---|
| S1 FAQ-grounded-resolve | DISCOVER → search → resolve_article → cite → CONFIRM | cs_192, cs_259, cs_001, cs_011 | partially (F1 §C3) | no (citation predicate is content-aware) | skill |
| S2 Intake-collect-and-handover | UC-G/H/I/J/K field-by-field intake | cs_066, cs_036, cs_038, cs_040 | partially (F1 §C4) | partially (intake_complete_for_uc_X downgrade is content-aware) | skill |
| S3 Soft-OOS clarify-or-escalate | UNKNOWN topic + ambiguous turn 1 | cs_029 (cs_259 r2 NOT owned by S3 — see §3 S3 correction; cs_259 r2 is owned by S1) | n/a for cs_259 (search already happened in cs_259 r2) | no — the previously-claimed "refuse `faq_miss_threshold_exceeded` without prior `search_knowledge`" guard is removed from the cs_259 fix per Sprint 5.1 codex correction; it is NOT sufficient for cs_259 and is not viable Sprint 6 scope | defer — not Sprint 6 scope |
| S4 Account login-recovery | UC-D + reset-loop pattern | cs_011 | yes (existing behaviour PASSes) | n/a | defer; defensive only |
| S5 Tier-2-reason UC-compat | request_handover Tier-2 reason × UC compatibility | cs_176 | yes (F1 §C1) | yes (mirror of §E1 pattern) | prompt first; runtime guard if prompt fails |

## 5. Skill / Java guard / prompt boundary table (for cross-reference with F3)

| Concern | Java guard | Skill | Prompt |
|---|---|---|---|
| Resolver precedence (`user_requested` > `user_distress` > FAQ-family > intake-complete > bot-limit) | yes — `EscalationReasonResolver.PRIORITY_TABLE`. Tier-0 invariant. | no — uses the table | no |
| §B1 deterministic distress detector | yes — `EscalationReasonResolver.detectDistressSignal` | no — runs ahead of skills | no |
| §C2 strong-prior carry-forward (D11) | yes — `ClassifyUseCaseTool.shouldPreserveStrongPrior` | no | no |
| Tool whitelist per phase | yes — `PhasePlan.allowedTools` enforced by `ToolDispatcher.validateAgainstPlan` | inherits | mentions allowed-tools cue |
| Citation present on FAQ resolve | partially — runtime can refuse `record_outcome(resolve)` without citation, OR rely on eval `L1:source_citation_present`; current state is the latter | yes — S1 predicate enforces it | nudge only |
| Intake-completion-completeness (no `intake_complete_for_uc_X` until all fields collected) | not yet — gap! S2 proposes a runtime downgrade when fields missing | yes — S2 step 5 condition | nudge only via F1 §C4 |
| Tier-2-reason × UC compatibility | not yet — gap; F2 §S5 vs F1 §C1 trade-off | optional (S5) | optional (F1 §C1) |
| `resolve_article` + grounded answer + `record_outcome` after `search_knowledge` (cs_259 r2 shape) | not yet — gap; primary cs_259 fix is S1 terminal predicate | yes — S1 predicate (search-ran-but-resolve-did-not branch) | nudge only via F1 §C3 / §C5 |
| `search_knowledge` before `faq_miss_threshold_exceeded` (defensive only) | not yet — defensive only; previously paired with S3 but removed from cs_259 fix per Sprint 5.1 codex correction (cs_259 r2 already had a prior search) | optional defensive guard, not the cs_259 fix | n/a |
| Customer-facing language / empathy / brevity | no | no | yes — `system_prompt.txt` |
| Per-UC clarifying-question phrasing | no | partially — skill names the field | yes — system prompt + per-UC `systemInstruction` |

## 6. Recommended Sprint 6 scope (skill side)

Per `docs/diagnostics/codex-findings.md` Sprint 5.1 review, Sprint 6 is capped at
**exactly 3 actions** (one infra + one prompt + one skill). On the
skill side that is exactly **one** skill:

- **ship in Sprint 6**: S1 (FAQ-grounded-resolve) — biggest single
  recurring shape, anchors cs_192 + cs_259 + the long tail of FAQ
  failures. Implement as a parametrized `PhasePlan` branch inside
  `PhaseEvaluator.plan(...)` off the existing FAQ RESOLVE plan, with
  a small extension to `ContextProjectionBuilder` to surface the
  necessary state slots (`accumulated_tool_results.search_knowledge`,
  `accumulated_tool_results.resolve_article`).

- **defer (NOT Sprint 6 scope)**: S2 (UC-G/H/I/J/K intake) — anchors
  cs_066 r2 and the structural intake-completion gap. Higher test
  cost. Pick up in a later sprint.
- **defer (NOT Sprint 6 scope)**: S3. The previously-suggested cs_259
  framing "F1 §C5 + small runtime guard refusing
  `request_handover(faq_miss_threshold_exceeded)` without prior
  `search_knowledge`" is removed per Sprint 5.1 codex correction —
  cs_259 r2 already had a prior `search_knowledge` call, so that
  guard would not address cs_259's observed failure. The "no-prior-
  search" guard is NOT viable Sprint 6 scope and is NOT the cs_259
  fix; cs_259 is owned by S1.
- **defer (NOT Sprint 6 scope)**: S4 (cs_011 currently PASSes;
  defensive only).
- **defer (NOT Sprint 6 scope)**: S5 (ship F1 §C1 prompt fix first;
  only escalate to runtime guard if prompt is insufficient).

Do NOT introduce a new skill runtime framework. Implement S1 as a
parametrized PhasePlan inside `PhaseEvaluator.plan(...)`.

## 7. Stop conditions

A Sprint 6 skill candidate is ready to ship only when:

- The trigger condition is *deterministic* — depends only on Java
  state (`session.*`, `accumulated_tool_results`,
  `UseCaseRegistryService` lookups), not on LLM output.
- The terminal-outcome predicates are *enforceable* by Java guards,
  not by prompt.
- The Java guard boundaries are explicit and tested (mvn integration
  tests pinning each predicate).
- The skill has a fallback termination path (MAX_STEPS) that does not
  break existing passing cases.
- The skill leaves customer-facing language to the LLM.

A skill that requires the LLM to read its own prior turn's reasoning
to advance is NOT yet appropriate for V1 — that's a planning-loop
shape and is explicitly out of scope per
`docs/sprint_objective.md` §"Do not implement" → "new skill runtime
framework".
