# Prompt / Context Projection Audit — Sprint 5 (F1)

Date: 2026-05-05
Sprint: Prompt / Context Projection and Fix-Layer Diagnostic Sprint 5
Status: diagnostic — the document proposes minimal, scoped prompt / context
changes but does NOT implement them. Per `docs/sprint_objective.md` §"Do
not implement" → "broad prompt rewrite".

## 1. Current prompt / context surfaces (what the LLM actually sees)

The bot LLM sees three layers of "instruction surface" per turn. They are
listed below in the order they hit the model.

### 1.1 System prompt

File: `server/src/main/resources/prompts/system_prompt.txt` (66 lines).
Loaded once at boot by `LlmInvocationService` and prepended verbatim to
every chat turn (`LlmInvocationService.invokeChat:82` —
`systemPromptTemplate + "\n\nCurrent context:\n" + projectedContext`).

Sections:

- General role + JSON-output contract (lines 1–8).
- `tool_calls` semantics (lines 10–13).
- Generic rules (lines 14–22) — empathy, no fabrication, no human claim,
  no promised actions, brevity.
- DISCOVER guidance (lines 23–30) — confidence thresholds for
  `classify_use_case`.
- `request_handover` decision tree (lines 32–66) — 23-value enum, mapped
  to 8 categorical buckets (USER-EXPLICIT, DISTRESS / SAFETY, APPEALS /
  RESTORATION, COMPLIANCE / ACCOUNT, INTAKE COMPLETION, BOT LIMITS,
  INFRASTRUCTURE / TOOLING).

### 1.2 Routing prompt

File: `server/src/main/resources/prompts/routing_prompt.txt` (17 lines).
Used only by `LlmInvocationService.invokeRouting` for the legacy two-stage
UC routing. Contains topic-subject + description placeholders + 6
disambiguation tiebreaker rules.

### 1.3 Projected context (per-turn JSON)

Built by `ContextProjectionBuilder.build(session, history, plan, userMessage,
accumulatedToolResults)` (`ContextProjectionBuilder.java:442`). Composes:

- `session` — `session_id`, `current_phase`, `active_use_case`,
  `total_bot_turns`, `clarification_count`, `faq_miss_count`,
  `handling_state` (lines 290–299).
- `task_summary` — derived from form topic + active UC name + intent
  confidence + current phase (`buildTaskSummary`, lines 555–577).
- `risk_flags` — array of `riskLevel` strings from the UC registry; empty
  when no UC is committed (lines 307–314).
- `budget_state` — counters + max thresholds (lines 317–324).
- `tool_schemas` — full per-UC tool schemas, filtered by
  `ToolPolicyEnforcer.getVisibleToolsForUc(activeUc)`; the DISCOVER plan
  also injects `classify_use_case` schema even when activeUc is null
  (`build:480-497`).
- `form_context` — pre-chat form data (raw JSON pass-through).
- `customer_context`, `listing_context` — `safe_summary` JSON when
  available.
- `conversation_history` — last 10 turns, each with `turn_index`,
  `user_message` (PII-redacted), `bot_response`, `tool_calls`.
- `knowledge_hits` — only when the legacy `buildProjection(...)` path is
  used (the AgentRunLoop world flows knowledge results through
  `accumulated_tool_results` instead).
- `current_user_message` — PII-redacted user message.
- `phase_plan` — when the AgentRunLoop is driving:
  `phase`, `use_case`, `objective`, `allowed_tools`, `grounding_instruction`,
  `system_instruction`, `escalation_policy`, `valid_terminal_outcomes`.
  Sourced from `PhaseEvaluator.plan(session, userMessage, history)`.
- `accumulated_tool_results` — last-write-wins map of tool_name →
  result.data.

### 1.4 PhasePlan content (per-phase mission briefing)

`PhaseEvaluator.plan(...)` returns one of five PhasePlans depending on
`current_phase` and `active_use_case`:

| Phase / UC | maxToolSteps | allowedTools | objective (verb) |
|---|---:|---|---|
| DISCOVER | 2 | `[search_knowledge, classify_use_case]` | identify use case |
| RESOLVE / FAQ-UC | 4 | `[get_customer_context, search_knowledge, resolve_article, request_handover]` | determine issue + grounded answer |
| RESOLVE / INTAKE-UC | 3 | `[request_handover]` (case-creation deterministic, runtime-only) | collect intake fields + handover |
| CONFIRM | 2 | `[record_outcome, request_handover]` | check satisfaction |
| CLOSE | 2 | `[record_outcome]` | polite close + record |
| ESCALATE | 2 | `[request_handover, record_outcome]` | finalize handover |

Each phase carries a separate `systemInstruction`, `groundingInstruction`,
and `escalationPolicy` string (≈ 1–4 sentences each).

## 2. Where the LLM lacks useful state or constraints

These are the gaps observed in the cs_176 / cs_192 / cs_015 / cs_259 /
cs_066 evidence in `docs/fix_layer_taxonomy.md`. Each gap is small and
local — none of them justify a broad prompt rewrite.

### Gap 2.1 — `request_handover` decision tree is not active_use_case-aware

**Symptom**: cs_176 r1 picks `payment_dispute_detected` for a UC-E
"why isn't my paid Top Ad at the top" form context where the user's
turn-2 message contains "give me my £50 back". The bot LLM correctly
interpreted "£50 back" as a payment-related signal, but the
`request_handover` system-prompt section does not surface
`active_use_case` as a tiebreaker.

**Current text** (lines 51 of `system_prompt.txt`): "Payment dispute /
chargeback / billing issue → `payment_dispute_detected`."

**What's missing**: the cs_176 spec is unambiguous —
`escalation_trigger=user_requested`. The persona asks "What about
giving a phone number to talk to someone", which is an explicit
human-help request. The bot must preserve / produce `user_requested`
(priority 1 in `EscalationReasonResolver.PRIORITY_TABLE`) regardless of
the literal "£50 back" phrasing in the same persona's earlier turn.
`payment_dispute_detected` (r1) and `service_degraded` (r2 — bot
drifted to UC-I) are both cross-family against `user_requested` and
therefore L1:escalation_compliance fails. `faq_miss_threshold_exceeded`
(bot_limit family) and `intake_complete_for_uc_k` (intake-complete
family) are NOT family-match against `user_requested` and are NOT
acceptable substitutes — earlier draft language to that effect was
incorrect and is corrected here. The active-UC tiebreaker must steer
the LLM toward `user_requested` when the user's turn-2 message
contains an explicit human-help cue, not toward FAQ-family reasons.

**Why this is a prompt fix and not a runtime fix**: a runtime guard for
"if active_use_case ∈ {UC-A, UC-B, UC-E} downgrade
`payment_dispute_detected` to `faq_miss_threshold_exceeded`" would be
the second step of the §E1 pattern — but `payment_dispute_detected` is
not Tier-0 like `user_distress`; the precedence table in
`EscalationReasonResolver` does not currently encode "payment_dispute is
weaker than faq_miss for FAQ UCs". Encoding that would be content-aware
runtime logic, which is exactly what the prompt is the right surface
for.

### Gap 2.2 — Routing prompt has no UC-FP tiebreaker for the "what happened to my ad" form

**Symptom**: cs_015 r1 routes UC-A ("Ad Status & Visibility") instead of
UC-FP ("Posting Policies / appeal a removal"). The form description is
"Hi - can you tell me what happened to my ad?", which `routing_prompt.txt`
already maps to UC-K via "What happened to my ad, where is my ad, with no
further detail → UC-K". But the persona's actual issue is a UC-FP
content-policy violation (rejected for nudity / category mismatch). UC-A
is the wrong route because the ad is not in flight — it has been rejected.

**Current text** (line 13 of `routing_prompt.txt`): "What happened to my
ad", "where is my ad", with no further detail → UC-K (intake — needs
moderation/account state lookup, then handover).

**What's missing**:

(a) The `ad on hold pending review`, `moderation status`, `policy
violation reason` keywords are listed as UC-A (line 10) AND as UC-FP
(line 12) — the precedence between those two rules is not stated. When
the description is short ("what happened to my ad?"), neither rule
fires and the LLM falls back to an arbitrary pick.

(b) `get_customer_context` should run before `classify_use_case` on
form-context cases that supply an email — Phase 3 §3.1.4 step 2c says
this is auto-triggered. But the projected context for the DISCOVER turn
does not always carry a populated `customer_context.moderation_status`
field, so the LLM cannot use moderation state as a UC-A vs UC-FP
tiebreaker.

**Proposed minimal change**: add a single line to `routing_prompt.txt`
that says: when description is short and unspecific (≤ 12 words and
contains "happened to my ad" / "where is my ad" / "ad rejected" /
"rejected ad"), and `customer_context.moderation_status` indicates a
moderation rejection or appeal-eligible state, prefer UC-FP. When the
ad is still live but invisible (moderation_status missing or
"under_review"), prefer UC-K intake.

### Gap 2.3 — RESOLVE FAQ grounding instruction does not enforce "search before answering"

**Symptom**: cs_192 r2 — the bot answered "Yes, you can give away free
items on Gumtree…" with no `search_knowledge` call and no
`source_id` citation. `L1:source_citation_present` failed.

**Current text** (`PhaseEvaluator.java:566` — RESOLVE FAQ
`groundingInstruction`):

> "If tool data contains specific information about the user's case
> (account/ad/moderation), answer from that first. For policy/process
> explanations, cite knowledge source IDs from `search_knowledge`
> results."

**What's missing**: the instruction tells the LLM what to do *if* it has
search results, but does not tell it that on turn 1 of a FAQ RESOLVE
phase with no `accumulated_tool_results.search_knowledge` entry, it
must call `search_knowledge` before producing a customer-facing answer.

**Proposed minimal change**: append one sentence to the RESOLVE FAQ
`groundingInstruction`: "If `accumulated_tool_results.search_knowledge`
is empty AND your `user_message` would contain a factual policy /
how-to claim, you MUST call `search_knowledge` first; do not answer
from prior knowledge."

### Gap 2.4 — Intake systemInstruction does not surface "fields collected so far / fields remaining"

**Symptom**: cs_066 r2 — UC-K intake (required fields:
`platform`, `repro_steps_or_error_message`). The bot asked
`repro_steps` on turn 3, then `category` on turn 5, exhausting the turn
budget without committing
`request_handover(intake_complete_for_uc_k)`. The LLM had to re-derive
the missing-field set every turn from `conversation_history`.

**Current text** (`PhaseEvaluator.buildIntakeSystemInstruction(activeUc,
ucDef)` — not shown above, but it's a per-UC paragraph listing the
required intake fields and the handover rule).

**What's missing**: the projected context does not currently include a
`session.intake_fields_collected` or `session.intake_fields_remaining`
slot. The DB column `intake_fields JSONB` exists (`bot_sessions` schema
line 264, `phase3_detailed_technical_design.md` §3.2.2), but
`ContextProjectionBuilder.buildProjection` does not surface it. The LLM
is asked to do a stateful diff against the conversation history every
turn.

**Proposed minimal change**: project `session.intake_fields_collected`
and the derived `session.intake_fields_remaining` (computed against
`UseCaseRegistryService.UseCaseDefinition.requiredIntakeFields`) into
the per-turn JSON when `current_phase == RESOLVE` and `active_use_case
∈ {UC-G, UC-H, UC-I, UC-J, UC-K}`. Update the intake `systemInstruction`
to say "ask only the next un-collected required field; once
`intake_fields_remaining` is empty, call request_handover(reason='X')".

This is a prompt + projection change, NOT a runtime change. The Java
side just needs to expose the field set in the projection (one new
helper in `ContextProjectionBuilder` reading `session.intakeFields`
JSONB + the UC registry's required fields).

### Gap 2.5 — DISCOVER systemInstruction does not surface "candidate_use_cases" prior

**Symptom**: cs_259 r2 — the form is UNKNOWN topic + empty description
+ user turn 1 "How do I receive the payment when I sell an item". The
bot DID call `search_knowledge` and DID commit UC-F via
`classify_use_case`, but then short-circuited to
`request_handover(faq_miss_threshold_exceeded)` after a single user
turn instead of running `resolve_article` + grounded answer +
`record_outcome`. Actual r2 tool sequence:
`['search_knowledge', 'classify_use_case', 'request_handover']` with
`lcs=1/4` against the spec sequence
`[get_customer_context, search_knowledge, resolve_article,
record_outcome]`. **The failure is "search happened, resolve did not
complete", NOT "no prior search".**

**Current text** (`PhaseEvaluator.java:381` — DISCOVER `systemInstruction`):

> "You are in the DISCOVER phase. Your goal is to identify which Use
> Case applies to the customer. Look at the form context, candidate use
> cases, and conversation history…"

**What's missing**: `candidate_use_cases` is named in the instruction
but the projection contract (`ContextProjectionBuilder.buildProjection`)
does not currently set a `candidate_use_cases` field even though the
`bot_sessions` schema has a `candidate_use_cases TEXT[]` column and
`phase2_domain_realization_spec.md` lists it as a required projection
field. Phase 3 §3.2.6 example shows
`"candidate_use_cases": ["UC-A-01"]` but the actual projection JSON
has `risk_flags` and `task_summary` and no `candidate_use_cases`.

The LLM is asked to "look at candidate use cases" that the projection
never gives it.

**Proposed minimal change**: project `session.candidate_use_cases` (if
present) into the per-turn JSON. Where the candidate_use_cases array
is empty (UNKNOWN topic + empty description + no LLM routing yet),
emit `candidate_use_cases: []` explicitly so the LLM knows to ask one
clarifying question, classify_use_case with low confidence, OR run a
search_knowledge query against the user's first turn. The DISCOVER
`systemInstruction` should clarify: "If `candidate_use_cases` is
empty AND the user's message contains a clear FAQ-shaped question
('how do I X', 'can I Y', 'what items are allowed'), call
`search_knowledge` with the user's question as the query before
deciding whether to classify_use_case or escalate."

**Correction (Sprint 5.1 codex-driven)**: cs_259 r2 evidence shows
that `search_knowledge` already ran and UC-F was already classified.
The recurring failure is the missing `resolve_article` → grounded
answer → `record_outcome` tail, not the absence of a prior search.
The **primary** cs_259 fix is therefore F2 §S1 (FAQ-grounded-resolve
skill / `PhasePlan` predicate that enforces
`search_knowledge → resolve_article → grounded customer-facing
answer → record_outcome`, OR an explicit handover only after a valid
resolve attempt cannot complete). C5 surfacing
`candidate_use_cases` is still useful for the DISCOVER turn but is
NOT the cs_259 primary fix and the previously-considered "java guard
refusing `faq_miss_threshold_exceeded` without a prior
`search_knowledge`" is removed/deferred — it would not address
cs_259 r2 because the search did happen.

### Gap 2.6 — `request_handover` decision tree silently mixes Tier-0 and Tier-2 buckets

**Observation** (lower-priority — flagged as audit context, not an
immediate fix): the system prompt's `request_handover` section
(lines 32–66) groups "DISTRESS / SAFETY" and "BOT LIMITS" without
exposing the precedence table from
`EscalationReasonResolver.PRIORITY_TABLE`. The runtime knows
`user_requested` (priority 1) beats `user_distress` (priority 2) beats
`faq_miss_threshold_exceeded` (priority 41) — but the prompt expresses
this only via the imperative "Prefer SPECIFIC over GENERIC" hint
(line 33).

**Why we leave this gap unaddressed in Sprint 5**: the Sprint 4 §E1
runtime gate already handles the distress-vs-faq_miss precedence
deterministically. Re-encoding the priority table in the prompt would
be redundant runtime-prompt drift — if the runtime ever changes the
precedence (Sprint 4 §E1 added a downgrade), the prompt would lag.
Defer.

### Gap 2.7 — `accumulated_tool_results` is opaque to the LLM about *which step is which*

**Observation**: the projection passes `accumulated_tool_results` as a
plain `tool_name → result.data` last-write-wins map. The LLM cannot
tell whether `search_knowledge` was called once or three times, what
the prior queries were, or which iteration of `resolve_article` came
from which `source_id`. This is a known
`docs/phase3_detailed_technical_design.md` §3.2.6 simplification.

**Why we leave this gap unaddressed in Sprint 5**: no current smoke
failure is causally attributable to this. Re-keying the map by
sequence index would help future-multi-step skills (F2) but is not
required for any of the F0 cases.

## 3. Candidate prompt / context changes (proposed; not implemented)

Each candidate is anchored to a specific evidence file + transcript and
specifies the file to edit, the lines to add, the cases that should test
it, and the rollback condition.

### Candidate C1 — active_use_case-aware `request_handover` reason picking

Files: `server/src/main/resources/prompts/system_prompt.txt`.
Insert after line 51 (`Payment dispute / chargeback / billing issue →
`payment_dispute_detected``), one short paragraph:

> "ACTIVE-UC TIEBREAKER. Before picking a Tier-2 policy reason
> (`payment_dispute_detected`, `appeal_requires_human`,
> `incorrect_deletion_appeal`, `account_compliance`,
> `trust_safety_required`, `gdpr_intake`,
> `identity_verification_required`), check `session.active_use_case`
> AND check whether the user has explicitly asked for human help
> (e.g. 'talk to someone', 'give me a phone number', 'speak to a
> person'). If the user has explicitly requested human help, pick
> `user_requested` regardless of whether the same conversation
> contains payment-keyword phrasing — `user_requested` is priority 1
> in the resolver and beats every Tier-2 policy reason. Only pick a
> Tier-2 reason if the user explicitly invokes a chargeback, GDPR,
> identity verification, or formal appeal flow AND has NOT separately
> asked for a human. If active_use_case is UC-A (Ad Status &
> Visibility), UC-B (How-to FAQ), or UC-E (Generic FAQ — advertising
> feature explanation), do NOT pick `payment_dispute_detected` for
> advertising-fee inquiries; those are UC-E feature explanations or
> UC-K intake, not UC-FP chargebacks."

Target case: cs_interactive_176.
Target Sprint 6 acceptance — corrected to align with the eval
contract: cs_176 r1+r2 must produce `escalation_reason=user_requested`
(or its resolver-canonical family — same priority 1 family). The spec
is `escalation_trigger=user_requested`; `faq_miss_threshold_exceeded`
and `intake_complete_for_uc_k` are NOT family-match against
`user_requested` and are NOT acceptable substitutes (earlier draft
language to that effect was incorrect and is corrected here).
Specifically, C1 must stop the bot from picking
`payment_dispute_detected` (r1 failure mode) for advertising-fee /
UC-E feature-explanation contexts when the user has explicitly asked
for human help — and must steer the bot toward `user_requested`
(priority 1) instead.

Residual risk: C1 may reduce r1 `payment_dispute_detected` picks but
does NOT fully address the r2 `active_use_case=UC-I` /
`service_degraded` drift — that drift is upstream, in
`classify_use_case` rather than `request_handover`, and remains
out-of-scope for a single-paragraph prompt change. Sprint 6 acceptance
criteria must therefore EITHER include "no unjustified
active_use_case=UC-I drift on cs_176 r2" OR explicitly defer the
r2 UC-drift question to a later sprint. Do not silently widen the
acceptance to accept service_degraded / faq_miss_threshold_exceeded /
intake_complete_for_uc_k as substitutes for `user_requested`.

Risks: the LLM may now under-route real payment disputes (UC-FP
chargeback) to FAQ family or to `user_requested`. Mitigated by
retaining the explicit "user explicitly invokes a chargeback / GDPR /
identity / appeal flow" escape hatch + by anchoring this in
active_use_case (UC-FP would be the natural UC for a real chargeback).

### Candidate C2 — Routing-prompt UC-FP / UC-A tiebreaker for short ad-rejection forms

Files: `server/src/main/resources/prompts/routing_prompt.txt`.
Insert one bullet after line 12:

> "- Short, low-detail descriptions like 'what happened to my ad?',
>   'where is my ad?', or 'ad rejected' with `Ad Support` topic →
>   prefer UC-FP (Posting Policies / removal reason) when
>   `customer_context.moderation_status` indicates a removal /
>   rejection / pending appeal; otherwise prefer UC-K (intake — needs
>   moderation lookup) over UC-A. UC-A is correct only when the ad is
>   live and the question is about visibility / search ranking."

Target case: cs_interactive_015.
Target Sprint 6 acceptance: cs_015 r1 routes UC-FP (or UC-K, both
secondary) on the form-context-only seed. Current behavior was UC-A.

Risks: cs_095 (which legitimately is UC-A "ad visibility / no adverts
showing") could shift if the new rule misfires. Mitigated by gating on
`customer_context.moderation_status` value, which cs_095 does not
populate (the user is asking about the email-sync / messaging path).

### Candidate C3 — RESOLVE FAQ grounding: "search before answering on turn 1"

Files: `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`.
Update the RESOLVE FAQ branch's `groundingInstruction` (current at
`PhaseEvaluator.java:566–570`) to append:

> "If `accumulated_tool_results.search_knowledge` is empty AND your
> `user_message` would contain a factual policy / how-to claim, you
> MUST call `search_knowledge` first; do not answer from prior
> knowledge. After `search_knowledge` returns, you may answer with a
> citation to one or more `source_id`s, OR call `resolve_article` for
> the highest-scoring hit before answering."

Target case: cs_interactive_192.
Target Sprint 6 acceptance: cs_192 r2 stops failing
`L1:source_citation_present` and `L2:tool_sequence_match` on the
"can I give away free items / what items are allowed" turns.

Risks: doubles LLM calls per FAQ turn for cases that previously
answered without searching. Bounded by the existing
`PhasePlan.maxToolSteps=4` for FAQ RESOLVE so the loop cannot run
away.

### Candidate C4 — Surface `intake_fields_collected` / `intake_fields_remaining` for INTAKE phases

Files: `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`.
Add a new projection slot in `buildProjection(...)` (lines 290–411) for
INTAKE-path UCs:

```json
{
  "intake_state": {
    "fields_collected": {"platform": "Chrome on Windows 11"},
    "fields_remaining": ["repro_steps_or_error_message"]
  }
}
```

Source `fields_collected` from `session.intakeFields` JSONB; source the
required field set from `UseCaseRegistryService.UseCaseDefinition.requiredIntakeFields`
(or the equivalent existing field). Update
`PhaseEvaluator.buildIntakeSystemInstruction` to reference
`intake_state.fields_remaining` explicitly: "Ask only for the next
field in `intake_state.fields_remaining`. When the array becomes
empty, call request_handover(escalation_reason='intake_complete_for_uc_X')."

Target case: cs_interactive_066.
Target Sprint 6 acceptance: cs_066 r2 finishes UC-K intake within turn
budget; reason is `intake_complete_for_uc_k`.

Risks: requires a small Java change in `ContextProjectionBuilder`. The
`session.intakeFields` JSONB schema must already align with the UC
registry's required field names (verify before implementing). If
existing fields are stored under different keys per UC, will need a
small mapping.

### Candidate C5 — Surface `candidate_use_cases` in projected JSON, update DISCOVER instruction

Files: `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`,
`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`.

Add `candidate_use_cases` to the projection (sourced from
`session.candidateUseCases`). Update the DISCOVER `systemInstruction`
to add:

> "If `candidate_use_cases` is empty AND the user's first message
> contains a clear FAQ-shaped question, call `search_knowledge` with
> the question as the query — do NOT escalate with
> `faq_miss_threshold_exceeded` after a single turn. After search
> returns, classify_use_case with the most plausible UC."

Target case: contributes to cs_interactive_259 (DISCOVER-side
support). NOTE: cs_259 r2's actual failure mode is "search happened,
resolve did not complete" — see Gap 2.5 correction. The cs_259
**primary** fix is F2 §S1 FAQ-grounded-resolve skill (search →
`resolve_article` → grounded answer → `record_outcome`, OR explicit
handover only after a valid resolve attempt cannot complete). C5 is
therefore a DISCOVER-side support change, not a sufficient fix for
cs_259 on its own.

Target Sprint 6 acceptance (revised): cs_259 r1 contract violation
shape stops firing (UC-F gets committed before any handover). The r2
"search happened, resolve did not complete" shape is owned by the S1
skill, not by C5.

Removed/deferred: an earlier draft of C5 paired this candidate with a
small Java guard that "refuses `request_handover(faq_miss_threshold_exceeded)`
when no prior `search_knowledge` is in `accumulated_tool_results`".
That guard is removed/deferred per Sprint 5.1 codex correction — cs_259
r2 evidence shows `search_knowledge` already ran, so the "no prior
search" guard would not change the observed cs_259 failure. The
guard could still be considered as a *defensive* invariant in a future
sprint, but it is NOT the cs_259 fix and must not be promoted as such.

Risks: small — `candidateUseCases` is already in the session DB schema;
projection just needs to expose it.

## 4. Cases that justify each candidate

| Candidate | Target case(s) | Evidence path |
|---|---|---|
| C1 (request_handover UC-aware) | cs_interactive_176 | r1: `eval_interactive/results/20260504-221916/results.json` cs_176 row, tags `L1:escalation_compliance`. r2: `eval_interactive/results/20260504-223153/results.json` cs_176 row. |
| C2 (UC-FP / UC-A tiebreaker) | cs_interactive_015 | r1: `eval_interactive/results/20260504-221916/results.json` cs_015 row, tags `L2_GATE:correct_uc`, `L2:correct_uc`, `L2:tool_sequence_match`. |
| C3 (search before answer) | cs_interactive_192 | r2: `eval_interactive/results/20260504-223153/results.json` cs_192 row, tags `L1:source_citation_present`, `L2:tool_sequence_match`. |
| C4 (intake_state projection) | cs_interactive_066 | r2: `eval_interactive/results/20260504-223153/results.json` cs_066 row, tags `L1:escalation_compliance`, total_turns=5, escalation_reason=turn_budget_exhausted. |
| C5 (candidate_use_cases projection) | cs_interactive_259 (DISCOVER-side support only — primary cs_259 fix is F2 §S1) | r1+r2: both result files cs_259 rows. r1 contract violation, r2 actual tool sequence `['search_knowledge', 'classify_use_case', 'request_handover']` (`lcs=1/4`) — search happened, resolve did not complete. |

## 5. Risks (cross-cutting)

- **Prompt-runtime drift**: every prompt rule that *could* live in the
  Java guard creates a maintenance liability if the runtime contract
  changes. Mitigated by anchoring each candidate to a specific runtime
  observable (`active_use_case`, `accumulated_tool_results`,
  `intake_state`, `candidate_use_cases`, `customer_context.moderation_status`)
  rather than to free-form narrative.
- **Token / cost growth**: each new prompt sentence adds ~ 30–60
  tokens to every chat call. Five new sections add ≈ 250 tokens per
  turn. Acceptable given current per-turn prompt ~ 2 kB.
- **Loss of test parity**: each candidate needs at minimum a Java
  integration test that pins the projected JSON shape (for projection
  changes) or a Python golden-prompt test (for prompt-template
  changes). Without those guards the next change can silently break
  the assumption.
- **Regression on currently-passing cases**: cs_001 / cs_002 / cs_011 /
  cs_029 / cs_036 / cs_038 / cs_040 / cs_066-r1 currently PASS. Each
  candidate must either be no-op for those cases, or include an
  explicit Sprint 6 regression check that PASS rate does not drop on
  them.

## 6. Tests / evals required before implementation

For each candidate:

1. **Java unit tests on the projection change** (C3, C4, C5): pin the
   projected JSON shape against a representative session fixture in
   `server/src/test/java/.../service/runtime/ContextProjectionBuilderTest.java`
   so the new field appears in the projection AND the existing fields
   are unchanged.
2. **PhaseEvaluator instruction tests** (C3, C4, C5): pin the
   `groundingInstruction` / `systemInstruction` strings against a
   golden snapshot. Currently no such test exists; add one.
3. **One smoke run before / after** per candidate: full `--set smoke
   --parallel 1` run is sufficient. Compare per-case composite, per-tag
   count.
4. **Targeted re-runs** for the case the candidate addresses: cs_176
   (C1), cs_015 (C2), cs_192 (C3), cs_066 (C4), cs_259 (C5). Re-run
   each at least 2× to characterize the LLM nondeterminism floor.
5. **Regression guard list** for non-target cases: cs_001, cs_002,
   cs_011, cs_029, cs_036, cs_038, cs_040, cs_066-r1, cs_095. PASS
   rate on these cases must not regress.

## 7. Stop conditions

Per `docs/sprint_objective.md` §"Do not implement", this Sprint 5
diagnostic does NOT implement any of C1–C5. Sprint 6 should pick at
most 2–3 of them to actually ship, anchored to the post-Sprint-4
canonical baseline `eval_interactive/results/20260504-221916/results.json`.

A Sprint 6 candidate is ready to land only when:

- The target case's spec has been verified (no override drift since
  Sprint 4).
- The evidence transcript shows the failure mode the candidate is
  meant to fix is the actual failure mode in the latest run, not a
  stale one from an older sprint.
- The Java integration tests + PhaseEvaluator instruction snapshot
  tests are in place.
- The smoke r1/r2 baseline has been refreshed.
