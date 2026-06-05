---
title: Sub-sprint A — S-Auto-23 / Sprint 078 — R1+R2+R4 bundle (intake-UC schema + DISCOVER counter wiring + ad-context premise projection slot)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file + docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md §6.1 + §6.2 + §6.4
last_reviewed: 2026-06-05
review_cadence: per sub-sprint
supersedes: []
superseded_by: null
notes: >
  Sub-sprint A of M-Auto-6 (Runtime substrate hygiene at intake +
  DISCOVER surfaces) per proposal §6.5 split. Drafted by deliver-agent
  2026-06-05 after M-Auto-5 close; awaits human review/approve before
  dev session launches.

  Scope: R1.a (intake-UC `request_handover` schema declares `intake_fields`
  + per-active-UC required-fields hint, sourced from existing
  `IntakeFieldsRegistry`) + R2.a (DISCOVER clarification counter wired
  on the live `AgentRunLoopImpl` path + budget projection LLM-facing soft
  signal + `mapBudgetToEscalationReason` re-maps `max-repeated-same-action`
  to existing `clarification_budget_exhausted` enum value) + R4.a
  (ad-context premise projection slot — `customer_context_status` enum
  + `ad_reference` structured fields surfacing already-known runtime
  facts; zero keyword matching of user messages). All three share the
  `ContextProjectionBuilder.java` edit context.

  Sub-sprint B (S-Auto-24 / Sprint 079; R3 UI/observability) is drafted
  as planning context in `compact/sprint-079-dev-prompt.md`; replaces
  this file's contract at A close.

  Forbidden: any semantic / yaml / CaseSpec edit; new escalation_reason
  enum; cross-turn content-similarity dedup; `IntakeFieldsRegistry`
  content change; `SkillGuardrailDispatcher` reject logic; simulator /
  eval framework / scoring SHA / autoloop 5-file set touch. R4-specific:
  no keyword match on user message content (only form_context + lookup
  tool result state).

  Falsifiable prediction at re-bless: `bad_cases` `reducible-flaky`
  6/12 → ≤ 2/12; uc_f_billing + uc_fp_removed → stable ~1.00; anti-误杀
  invariants (anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety
  + shadow cs38s* at 0.000 stable) UNCHANGED. R4 evaluation is observability
  + setup (signal surface for OBS-S1 autoloop work) rather than a direct
  pass-rate move — c7 closure requires R4 + OBS-S1 together; this
  sub-sprint ships only R4 (the enabler).
---

# Sub-sprint A — S-Auto-23 / Sprint 078 — R1+R2+R4 bundle

## Class

**Layer (per `iteration_governance.md` §3):**
- **R1.a** → `prompt_projection` (projects already-existing
  `IntakeFieldsRegistry` contract as schema declaration + per-UC
  required-fields hint on the LLM-facing `request_handover` tool surface).
- **R2.a** → `infra` + `skill_state` (live-path wiring of an existing
  `BudgetChecker.maxClarificationRounds(=2)` counter + budget projection
  + relabeled enum mapping).
- **R4.a** → `prompt_projection` (surfaces already-known runtime facts —
  `form_context.ad_id` presence + `lookup_listing_or_ad` tool result
  state — as a structured boolean/enum slot; zero content matching).

**§7 stanza requirement:** REQUIRED (R1.a + R4.a are both
`prompt_projection` — semantic-touching surfaces). Full stanza in §7
below.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R1.a
projects an existing validator contract; R2.a wires existing dead-code
budget; R4.a projects already-observed runtime state. None requires a
runtime-level safety floor change.

**Semantic hardcode:** No semantic hardcode introduced.
- R1.a's per-active-UC required-fields list is read at projection time
  from `IntakeFieldsRegistry.java:53-67` (which already defines UC-G
  `[case_id]`, UC-H `[case_id]`, UC-I `[transaction_reference,
  dispute_reason]`, UC-J `[report_target, report_type, description]`,
  UC-K `[case_id]`). The projection iterates registry contents — no
  per-UC matrix replicated in this code.
- R2.a counts cardinality (clarification turns matching strict
  structural criteria) — zero content matching.
- R4.a surface fields are STRUCTURAL booleans/enums sourced from
  `FormContextIngestionService` state + the existing
  `lookup_listing_or_ad` tool result — NO keyword match on user
  message content; NO reason text projection (only enum status). LLM
  remains free to decide whether to challenge premise (§1.3 LLM owns
  next-action).

## Goal

Eliminate two LIVE-path runtime wiring defects (R1 + R2) + surface the
ad-context premise signal (R4) that together remove noise from the
intake-UC handover + DISCOVER clarification + UC-A-without-ad_id
surfaces of the post-M-Auto-5 honest baseline
`m-auto-5-baseline-20260604-simfixed-stalledfix`.

After this sub-sprint lands and a re-bless runs:

- **R1**: every `request_handover` first call on UC-G / UC-H / UC-I /
  UC-J / UC-K arrives at the validator with the required `intake_fields`
  already populated (no first-call rejection-and-retry pattern).
- **R2**: DISCOVER clarification turns on the live `AgentRunLoopImpl`
  path increment `session.clarificationCount`; the LLM sees the budget
  as a projected soft signal; when the cap is reached, the loop
  escalates with the correctly-labeled `clarification_budget_exhausted`
  escalation_reason (NOT the misleading `turn_budget_exhausted`).
- **R4**: when form_context lacks `ad_id` AND user references their own
  advert, the LLM-facing projection includes `customer_context_status:
  missing_ad_id` and `ad_reference: {form_ad_id: null, listing_lookup:
  missing}`. R4 is the ENABLER for OBS-S1 (UC-A verify-ad procedure
  step) which is autoloop work AFTER M-Auto-6 close. This sub-sprint
  ships only the runtime signal; the skill-yaml step that uses the
  signal is intentionally deferred so this sub-sprint stays
  runtime-only (no yaml edits).

## Scope (executable, #1–#9)

### #1 — R1.a step 1: `request_handover` tool schema declares `intake_fields`

**File:** `server/src/main/java/.../ContextProjectionBuilder.java` —
`buildRequestHandoverArgsSchema()` at line 116-120 + the args schema body
at line 200-260.

**Change:** Add `intake_fields` as an optional `object` JSON-schema
property to the `request_handover` args schema. Property semantics:
free-form `string → string` key-value map (DO NOT enumerate per-UC
properties in the schema itself — that would be the per-UC matrix
forbidden by §1.7). Tool description note: "Required for intake UCs
(UC-G / UC-H / UC-I / UC-J / UC-K) — see the `required_intake_fields_for_active_uc`
projection field for the active UC's required-fields list."

**Why:** validator at `SkillGuardrailDispatcher.java:265-298` already
expects `session.intakeFields` to contain the required fields, and
`AgentRunLoopImpl.java:486-494` already merges
`call.arguments.intake_fields` into session BEFORE running the validator.
The schema simply did not declare the field for the LLM to populate
(the gap c1/c5/c6 surface).

### #2 — R1.a step 2: per-active-UC required-fields hint projection

**File:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add a new projection field `required_intake_fields_for_active_uc` next
to `intake_state` (existing projection field).

**Change:** Read `IntakeFieldsRegistry.requiredFieldsFor(activeUseCase)`
at projection time when `activeUseCase` is an intake UC; emit the
resulting `List<String>` (e.g. `["report_target", "report_type",
"description"]` for UC-J) into the projection. When `activeUseCase`
is null OR a non-intake UC, the field is `null` or omitted (DO NOT emit
empty list — distinguish "not applicable" from "no fields required").

**Why:** gives the LLM a structured per-turn hint of EXACTLY which
field keys to populate in the next `request_handover.arguments.intake_fields`
call. Zero per-UC content matching in this file — `IntakeFieldsRegistry`
is the source of truth; this projection iterates it.

### #3 — R2.a step 1: DISCOVER clarification counter live-path wiring

**File:** `server/src/main/java/.../AgentRunLoopImpl.java` — in the
DISCOVER branch, after a turn completes.

**Change:** Identify the turn as a "free-text clarification" if and only
if ALL the following hold (strict cardinality criteria; NO content
matching):
- `session.currentPhase == DISCOVER`,
- the turn produced zero tool calls,
- the turn did not commit an active use case
  (i.e. `session.activeUseCase` is unchanged from before the turn),
- the turn produced a non-empty **bot/assistant free-text reply** — i.e.
  the model's outgoing response field (e.g. `Turn.botMessage` /
  `Turn.assistantReply` / `BotResponse.text` — **Dev: verify the
  exact field name on the live `AgentRunLoopImpl` turn record before
  wiring**). **CRITICAL: this is the BOT's outgoing reply, NOT
  `user_message` (which is the customer's incoming turn).** Confusing
  these will misclassify customer messages as bot clarifications and
  corrupt the counter.

When all four hold, `session.clarificationCount += 1` BEFORE the next
turn's budget check.

**Why:** the live `AgentRunLoopImpl` path never increments
`session.clarificationCount`; the only existing `+1` site at
`PhaseEvaluator.java:880` is on the legacy path the live loop does not
reach. The counter therefore stays at 0 forever and
`BudgetChecker.java:32-37` cap is unreachable on the live path (c3/c9
root cause).

### #4 — R2.a step 2: budget projection (LLM-facing soft signal)

**File:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add a new `budgets.clarification` projection field next to the existing
budget projections (line 351 / 658 vicinity).

**Change:** Emit `budgets.clarification: {used: session.clarificationCount,
max: controlPolicy.maxClarificationRounds}` in the per-turn projection.
This is a structured cardinality field; LLM may or may not adapt
behaviour based on it (soft signal per §1.3 — LLM owns next-action
decision).

**Why:** gives the LLM observable state to make better DISCOVER
sequencing decisions even before the hard cap fires.

### #5 — R2.a step 3: `mapBudgetToEscalationReason` label fix

**File:** `server/src/main/java/.../ControlKernel.java` — at line 305-308
(`mapBudgetToEscalationReason` method) + the inject path at the same
location.

**Change:** When the budget hit is `max-repeated-same-action` (an
existing `BudgetChecker` budget type, currently fall-through-mapped to
the generic `turn_budget_exhausted`), re-map to the **already-existing**
`clarification_budget_exhausted` enum value (one of the 23-value
`escalation_reason` set; documented under
`R-runtime-escalation-reason-misstamp-maxsteps-faq` close pattern
where reuse-existing-enum was preferred over adding a new enum). Do
NOT add new enum values. Do NOT touch other budget mappings.

**Why:** c9 surfaces the label-misleading effect — `max-repeated-same-action`
is the budget that fired but the inject is stamped `turn_budget_exhausted`,
muddying the eval signal.

**Pre-fix characterization REQUIRED**: before wiring the re-map, dev
MUST audit `BudgetChecker` + every call site that consumes
`max-repeated-same-action` to confirm the budget is **only** raised on
DISCOVER-phase clarification repetition. Two outcomes:

- (a) Confirmed DISCOVER-only → re-map is unconditional (the simpler
  path; matches c9 evidence).
- (b) NOT DISCOVER-only (the budget also fires in RESOLVE / INTAKE /
  other phases for non-clarification repeated tool calls) → either
  add a phase guard so re-map fires only when
  `session.currentPhase == DISCOVER` at injection time AND the
  repeated action is a free-text clarification (NOT a tool call),
  OR STOP and surface to deliver-agent + human for scope clarification.
  Do NOT silently re-map every `max-repeated-same-action` hit if the
  budget has multi-phase semantics.

Document the audit outcome in §1 of the handoff. The negative test in
#8 will gate this regardless.

### #6 — R4.a step 1: `customer_context_status` enum slot in projection

**File:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add a new top-level projection field `customer_context_status` to the
per-turn projection.

**Change:** Compute the field from runtime state (no content matching).
**Critical: priority order MATTERS — `missing_*` conditions take
precedence over `loaded` so that a partially-loaded customer context
does not mask an absent ad_id premise.** Evaluate top-down; FIRST
matching condition wins:

| Priority | Condition (computed from runtime state) | Emit value |
|---|---|---|
| 1 | `form_context.email` is null/blank | `missing_email` |
| 2 | `form_context.ad_id` is null/blank | `missing_ad_id` |
| 3 | `lookup_listing_or_ad` was triggered but failed (transport/server error) | `lookup_failed` |
| 4 | `lookup_listing_or_ad` was NOT triggered despite ad_id being present (tool not called) | `lookup_skipped` |
| 5 | `get_customer_context` tool result is in session state AND populated, AND ad_id is present AND lookup succeeded | `loaded` |

Field type: enum/string with the five allowed values above (one of
`{missing_email, missing_ad_id, lookup_failed, lookup_skipped, loaded}`).

**Why priority order matters**: the c7 scenario is
`get_customer_context` returns populated customer data (loaded via the
form's `email`) BUT `form_context.ad_id` is null. Without the
ordering, the value would be `loaded` (because customer context IS
loaded) and the LLM would never see that the AD reference is unverified.
With this ordering, `missing_ad_id` is surfaced, which is the R4
target signal. If `customer_context` is the dominant concern in
another flow, the projection should add a separate
`customer_context_loaded` boolean field; do NOT relax the priority
order to make `loaded` win.

**Why:** when form_context lacks ad_id and the user references their own
advert (c7 pattern), the LLM-facing projection currently has no way to
know that the runtime cannot verify the ad reference. Surfacing this as
a structured enum gives the LLM observable premise state without telling
it WHAT to say (§1.3 LLM owns next-action).

**Hard fence:** DO NOT emit a reason string (e.g. `"because the ad_id
is missing, the bot cannot..."` ); only the enum value. DO NOT match
keywords like "my ad" / "my listing" in the user message to decide what
to emit — derive ENTIRELY from runtime form_context + tool result state.

### #7 — R4.a step 2: `ad_reference` structured fields

**File:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add a new `ad_reference` projection block next to `form_context` (or
nested inside it, depending on existing schema convention).

**Change:** Emit `ad_reference: {form_ad_id: <value>|null,
listing_lookup: <ok|missing|failed|skipped>}`:

- `form_ad_id`: the literal `form_context.ad_id` value (null if absent).
- `listing_lookup`: distinguishes four runtime conditions:
  - `ok` — `lookup_listing_or_ad` was triggered AND returned a result
    that resolves the ad.
  - `missing` — `lookup_listing_or_ad` was triggered AND ran successfully
    BUT no listing was found (ad does not exist or already removed
    from the backing system).
  - `failed` — `lookup_listing_or_ad` was triggered but errored
    (transport / server error / timeout).
  - `skipped` — `lookup_listing_or_ad` was NOT triggered (typically
    because `form_context.ad_id` is null, but also covers any other
    runtime path that skipped the auto-trigger).

The distinction between `missing` (ran-but-no-result) and `skipped`
(never-ran) is load-bearing for R4's downstream use: a `missing` value
means the bot knows the ad does not exist in our system; `skipped`
means the bot has no information either way. Collapsing them would
remove the signal that drives the c7 OBS-S1 autoloop work.

**Why:** complements #6 with the ground-truth state the LLM may need
to reason about whether to ask for ad clarification. R4.a slot pair
(#6 enum + #7 structured fields) is the complete projection surface for
the "premise unverified" condition. NOT a per-UC matrix — this fires on
every turn regardless of active UC; UC-A pattern (c7) is the use case
where it materially helps, but UC-FP and others may also benefit.

**Hard fence:** ONLY runtime-observed fields. NO inference about user
intent. NO reason text. Field MUST NOT include any LLM-generated
content.

### #8 — Anti-误杀 counter-tests (both eval-aware and runtime)

**Files (new test classes):**
- `server/src/test/java/.../IntakeFieldsProjectionTest.java`
- `server/src/test/java/.../DiscoverClarificationCounterTest.java`
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java`
- `server/src/test/java/.../CustomerContextStatusProjectionTest.java`
- `server/src/test/java/.../AdReferenceProjectionTest.java`

**Change:** For each of #1-#7, ship anti-误杀 paired tests:

- #1 positive: `request_handover` tool schema includes the optional
  `intake_fields` property for ALL UCs (the schema is global).
- #1 negative: validator behaviour UNCHANGED when `intake_fields` is
  absent or partial — `SkillGuardrailDispatcher` still rejects with
  `intake_required_fields_missing_for_intake_complete` on intake UCs.
  Schema declaration must NOT cause partial intake to start passing.
- #2 positive: each intake UC (UC-G / UC-H / UC-I / UC-J / UC-K)
  projects `required_intake_fields_for_active_uc` with the correct
  required-fields list (iterated from `IntakeFieldsRegistry`).
- #2 negative: non-intake UC (UC-A with `ad_id`, UC-B, UC-D, UC-F,
  UC-FP) projection does NOT include
  `required_intake_fields_for_active_uc` (the field is null or omitted
  — distinguish "not applicable" from "no fields required").
- #3 positive: DISCOVER turn with zero tool calls + uncommitted UC +
  non-empty **bot/assistant free-text reply** (NOT `user_message`)
  increments counter by exactly 1.
- #3 negative: DISCOVER turn with a tool call does NOT increment; turn
  that commits a UC does NOT increment; turn with empty bot reply does
  NOT increment. Customer-side `user_message` content NEVER affects
  the counter regardless of its value.
- #4 projection emits `budgets.clarification` with correct used/max.
- #5 positive: DISCOVER-phase `max-repeated-same-action` budget on a
  free-text repeated reply → emits `clarification_budget_exhausted`
  escalation_reason.
- #5 negative (multi-phase guard): NON-DISCOVER phase
  `max-repeated-same-action` budget (e.g. fires in RESOLVE on a
  repeated tool call) MUST NOT be relabeled to
  `clarification_budget_exhausted`. Either the re-map is
  phase-guarded, OR if the dev-side pre-fix scope audit (see #5
  rationale) confirms `max-repeated-same-action` is DISCOVER-only,
  this test asserts no other call site raises the budget; if a
  multi-phase use later appears, this test fails loudly.
- #5 negative (other budgets): other budget types still emit their
  existing escalation_reasons (`max-clarification-rounds` → unchanged;
  `max-faq-miss` → unchanged; default fall-through →
  `turn_budget_exhausted` for non-clarification budgets).
- #6 — `customer_context_status` enum priority: five separate tests,
  one per enum value (`missing_email` / `missing_ad_id` /
  `lookup_failed` / `lookup_skipped` / `loaded`), each verifying the
  exact runtime condition that triggers it. Additional priority-order
  tests (CRITICAL):
  - (a) `get_customer_context` is loaded AND `form_context.ad_id` is
    null → `missing_ad_id` wins (NOT `loaded`). This is the c7
    scenario — failure of this priority gate defeats R4's purpose.
  - (b) `get_customer_context` is loaded AND `form_context.email` is
    null → `missing_email` wins (NOT `loaded`).
  - (c) `get_customer_context` is loaded AND `lookup_listing_or_ad`
    failed → `lookup_failed` wins.
  - (d) User message contains "my ad" / "my listing" without runtime
    state change → enum value UNCHANGED (no keyword leakage).
- #7 — `ad_reference` block: positive tests for each `listing_lookup`
  value (`ok` / `missing` / `failed` / `skipped`); explicit test that
  `missing` (ran-but-no-result) is distinct from `skipped` (never-ran)
  — they MUST NOT be collapsed. Positive for `form_ad_id` = literal
  value when present; negative for `form_ad_id` = null when absent.
  Verify the block is emitted regardless of active UC (NOT per-UC
  matrix).

**Anti-误杀 high-risk fence**: 
- Any test that exercises a UC-J / UC-H / UC-I-flavored intake handover
  MUST also assert that the validator-side contract
  (`SkillGuardrailDispatcher` reject behaviour) is unchanged when
  `intake_fields` is absent. Schema declaration must not cause the
  validator to start passing partial intake.
- Any test that exercises R4 emit MUST verify that DOWNSTREAM consumers
  (`accumulated_tool_results` projection, `intake_state` projection,
  `form_context` projection) are unchanged in shape — R4 adds a new
  field; existing fields are not modified.
- Test that injects a user message with the string "my ad" / "my
  listing" / similar WITHOUT runtime-observed ad_id state changes MUST
  NOT shift `customer_context_status` away from its runtime-state-only
  value.

### #9 — HUMAN-LAUNCHED post-fix re-bless (real-LLM, §5.7)

**Backend rebuild required** for the Java changes (#3, #5).

**Output dir** (proposed): `eval_interactive/results/m-auto-6-baseline-r1r2r4-YYYYMMDD/`.

**Command** (drafted; HUMAN reviews before launch): same multi-suite
re-bless template the human used at M-Auto-5 close — three suites
(bad_cases + anchor_outcome + shadow), `samples_per_case` per the
current re-bless protocol, real-LLM, deterministic at
`simulator_temperature=0.0`.

**Validation gate (HARD close gates — R1 + R2 measurement effects)**:

- `bad_cases` `reducible-flaky` count: expect ≤ 2/12 (vs 6/12 pre-fix).
- uc_f_billing + uc_fp_removed: expect → stable ~1.00 (vs 0.89
  reducible-flaky).
- Anti-误杀: anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety
  + shadow cs38s* MUST stay at 0.000 stable.

**Validation gate (R4 wiring — observable, NOT hard close gate)**:

R4 is a signal-surface change; the c7 semantic loop closure depends
on OBS-S1 (UC-A verify-ad procedure skill yaml) which is autoloop
work AFTER M-Auto-6 close. R4 evaluation at this re-bless is
**wiring-correctness verification**, not pass-rate validation:

- Sample at least 3 UC-A and 3 UC-FP traces where `form_context.ad_id`
  is null in the test corpus: the projection in each trace MUST
  include `customer_context_status: missing_ad_id` and
  `ad_reference: {form_ad_id: null, listing_lookup: skipped}` (or
  `missing` if the tool was triggered).
- Sample at least 3 with-ad_id UC-A traces (negative): projection MUST
  NOT include `customer_context_status: missing_ad_id` (expect
  `loaded` or `lookup_skipped` or `lookup_failed` per runtime path).
- Any observable bot behaviour shift in response to R4 signal is a
  bonus observation, NOT a close gate. The skill-yaml side that uses
  the signal (OBS-S1) is autoloop work AFTER M-Auto-6 close.

**Routing decision**:

- If R1+R2 prediction holds AND R4 wiring verified → S-Auto-23 close;
  open S-Auto-24 (Sub-sprint B / R3) per `docs/milestone_objective.md`
  §3 route (a).
- If R1+R2 prediction fails (independent of R4 wiring) → S-Auto-23
  surfaces a re-diagnosis brief; route (b) per same.
- If R4 wiring is wrong but R1+R2 succeed → fix-iteration sub-sprint
  for R4 wiring only; do NOT block S-Auto-24 launch on R4 semantic
  effects.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety at
   0.000 stable** must NOT shift after R1+R2+R4. These persisted at
   0.000 through M-Auto-5 close (a stable safety-floor signal) and any
   change in either direction is a regression to investigate.
2. **Shadow cs38s* (scam+harassment family) at 0.000 stable** must NOT
   shift.
3. **No `SkillGuardrailDispatcher` reject-logic edits.** The validator
   remains the last line of defence; R1.a's schema declaration may NOT
   cause partial-intake handovers to start passing the validator.
4. **`IntakeFieldsRegistry.java:53-67` field-definition contents
   UNCHANGED.** R1.a iterates the existing registry; if the registry's
   required-fields set is itself wrong for any UC, that is a separate
   `eval_spec` question outside this sub-sprint.
5. **R2.a counter is cardinality-only**; no content/semantic similarity
   detection.
6. **NO new `escalation_reason` enum values.** R2.a's #5 step re-maps
   one budget type to an existing enum value.
7. **NO yaml prompt / skill procedure edit anywhere in M-Auto-6**;
   OBS-S1 (UC-A verify-ad procedure) is autoloop work AFTER M-Auto-6
   close.
8. **R4.a `customer_context_status` enum + `ad_reference` block are
   STRUCTURAL ONLY** — derived from runtime state (form_context fields
   + lookup tool result state), NEVER from user message content / NLP /
   keyword matching. If a future need to incorporate user-message
   signals arises, that is a separate sub-sprint requiring §7 stanza
   re-evaluation.
9. **R4.a does NOT change any UC routing / FAQ-grounded resolve gate /
   escalation posture** — only adds projection state. Bot behaviour
   change in response to the new state is the LLM's call (§1.3); the
   yaml side that uses the signal (OBS-S1) is autoloop work after
   M-Auto-6 close.
10. **R4.a `customer_context_status` priority ordering is
    LOAD-BEARING**: `missing_email` / `missing_ad_id` / `lookup_failed` /
    `lookup_skipped` take precedence over `loaded`. A populated
    `get_customer_context` result MUST NOT mask an absent
    `form_context.ad_id` (the c7 scenario). If a future flow needs
    customer_context_loaded as a separate signal, add a separate
    boolean field; do NOT relax the priority order.
11. **R4.a `ad_reference.listing_lookup` MUST distinguish `missing`
    (ran-but-no-result) from `skipped` (never-ran)** — collapsing
    them removes load-bearing signal for OBS-S1 autoloop work.
12. **R2.a `max-repeated-same-action` re-map MUST be phase-aware OR
    the budget MUST be confirmed DISCOVER-only by pre-fix audit**.
    Silently relabeling every `max-repeated-same-action` hit to
    `clarification_budget_exhausted` regardless of phase would
    mis-label genuine RESOLVE/INTAKE repeated-action budgets — a
    new false-label artifact in the eval signal.

## Hard fences / STOP conditions

**Files allowed to edit** (fence):

- `server/src/main/java/.../ContextProjectionBuilder.java`
- `server/src/main/java/.../AgentRunLoopImpl.java`
- `server/src/main/java/.../ControlKernel.java`
- (Read-only access for R4 state queries) `server/src/main/java/.../FormContextIngestionService.java` — DO NOT edit; R4.a reads its existing public state to drive the projection.
- `server/src/test/java/.../IntakeFieldsProjectionTest.java` (new)
- `server/src/test/java/.../DiscoverClarificationCounterTest.java` (new)
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java` (new)
- `server/src/test/java/.../CustomerContextStatusProjectionTest.java` (new)
- `server/src/test/java/.../AdReferenceProjectionTest.java` (new)
- (Optional) `server/src/test/java/.../ContextProjectionBuilderTest.java` (existing — update to reflect new schema field).
- `docs/sprints/sprint-078-handoff.md` (new dev handoff).

**Files FORBIDDEN to edit**:

- Any prompt / yaml under `server/src/main/resources/`.
- `IntakeFieldsRegistry.java`.
- `SkillGuardrailDispatcher.java`.
- `BudgetChecker.java`.
- `control-policy.yaml`.
- `FormContextIngestionService.java` (R4.a reads its state; does not modify).
- `ResolveArticleTool.java`, `ResolveDispositionEvaluator.java`, any other tool/skill class (R4.a is projection-only).
- Any UI file (R3/Sub-sprint B scope).
- Any `eval_interactive/case_specs/**` CaseSpec or override.
- `eval_interactive/eval_interactive/simulator/user_simulator.py` (M-Auto-5 fix).
- `eval_interactive/eval_interactive/scoring/composite.py` (M-Auto-5 S-Auto-22 fix).
- `eval_interactive/eval_interactive/scoring/hard_checks.py` (M-Auto-5 S-Auto-22 fix).
- Autoloop 5-file SHA-locked scoring set (per fence-#13).
- `autoloop/config.yaml` `baseline_dir` (M-Auto-5 close set it).

**STOP conditions**:

- If counter wiring at #3 reveals that "free-text clarification"
  detection criteria miss > 5 % of clarification-shaped turns on the
  bad_cases corpus characterization run, surface as OQ before re-bless;
  do NOT widen criteria with content heuristics.
- If `mapBudgetToEscalationReason` change at #5 surfaces > 3 affected
  eval CaseSpecs (mismatched `expected.escalation_reason`), STOP at
  the test stage; report to deliver-agent + human for either CaseSpec
  spec-sync (controlled §5.4 update per the proposal §9 risk row) or
  scope-reduction.
- Any anti-误杀 invariant test FAILS in CI → STOP, do not commit until
  fixed.

## Test / eval requirements

- All new tests in #6 GREEN.
- Existing test baselines unchanged: Java `1244 / 1 / 0 / 2` baseline +
  new tests; eval pytest `553` baseline (no eval-side change in this
  sub-sprint); autoloop pytest `324` baseline.
- `ContextProjectionBuilderTest` updates if existing tests asserted the
  exact schema field set (proposal §9 risk row); document any update
  in the handoff.
- Mocked-LLM tests for the LLM-facing projection surface MUST be
  treated as wiring evidence only (§5.7); the only real evidence is
  the post-fix HUMAN-LAUNCHED re-bless.

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `prompt_projection` (R1.a `request_handover`
schema declaration + per-active-UC required-fields projection;
R4.a `customer_context_status` enum + `ad_reference` block) +
`infra` (R2.a counter wiring + budget projection + mapping fix) +
`skill_state` (R2.a counter accumulation across turns).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. R1.a
projects an existing `IntakeFieldsRegistry` contract; R2.a wires an
existing `BudgetChecker.maxClarificationRounds(=2)` budget that has
been declared since substrate-hygiene M-Auto-3 / `control-policy.yaml`;
R4.a projects already-observable runtime form_context state + tool
result state. None requires a new runtime-level safety floor.

**Semantic hardcode:** No semantic hardcode introduced.
- R1.a's per-active-UC required-fields list is read from
  `IntakeFieldsRegistry` at projection time (zero per-UC matrix in this
  code; registry is the source of truth).
- R2.a counts cardinality (clarification turns matching strict
  structural criteria: phase == DISCOVER && no tool calls && no UC
  commit && non-empty BOT/ASSISTANT free-text reply — NOT
  `user_message`, which is the customer's incoming turn); zero content
  matching. R2.a's mapping fix re-maps an existing enum value with a
  phase-guard or pre-confirmed-DISCOVER-only budget, NOT a new enum.
- R4.a `customer_context_status` enum value derives ENTIRELY from
  runtime state (`form_context.email`/`ad_id` presence +
  `lookup_listing_or_ad` tool result); zero keyword matching of user
  message content; no reason text. `ad_reference` block is structural
  (literal `form_context.ad_id` value + enum tool-result status). LLM
  is free to decide whether to challenge premise.

**Generalization coverage** (target / neighbor / negative / shadow):

- **Target**: c1 (UC-J 4× intake first-call rejection), c5 (UC-J), c6
  (UC-I), c3 (DISCOVER 2× identical clarification), c9 (DISCOVER label
  mis-stamp), c7 (UC-A no ad_id, premise unverified). 6 of the 9
  proposal traces.
- **Neighbor**: every UC-G / UC-H / UC-I / UC-J / UC-K
  `case_families/*` case in `bad_cases/` + `anchor_outcome/` (~12-18
  cases). Plus every DISCOVER-phase case that uses budget exits across
  all three suites. Plus UC-A + UC-FP cases where form_context lacks
  `ad_id` (target for R4 negative-test coverage too).
- **Negative**: non-intake UCs (UC-A with ad_id, UC-B, UC-D, UC-F,
  UC-FP) — `request_handover` must NOT carry `intake_fields` or carry
  it with empty payload without rejection. DISCOVER turns that DO call
  tools or commit a UC must NOT increment `clarificationCount`. UC-A
  WITH ad_id in form_context: `customer_context_status` MUST be `loaded`
  or `lookup_skipped` (NOT `missing_ad_id`). User messages containing
  the string "my ad" / "my listing" WITHOUT a matching runtime state
  change MUST NOT shift `customer_context_status`.
- **Shadow**: existing shadow suite (cs01s* / cs15s* / cs32s* / cs38s*
  / cs59s* / cs76s* / cs92s*) — at minimum verify shadow UC-K + UC-J +
  UC-A + UC-I + UC-G coverage holds.

## Codex review plan (§4.3)

**Per-sub-sprint Codex review REQUIRED** because R1.a + R4.a both
touch the `prompt_projection` LLM-facing surface (semantic-touching per
`iteration_governance.md` §7 + `process/milestone-framework.md` §4.3).

Codex prompt artifact: `compact/sprint-078-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close, embeds §4.1 nine-question
kernel + §7 stanza + file-path fence + anti-误杀 invariants +
generalization coverage).

**Focus points for Codex** (per §4.3):

- Q1: confirm no per-UC keyword/regex/matrix added in `ContextProjectionBuilder`
  for the required-fields projection (must iterate `IntakeFieldsRegistry`);
  confirm R4.a `customer_context_status` enum derivation is purely
  structural (zero content matching on user message).
- Q3: confirm the projection change at #2 makes the LLM's contract
  with the validator explicit (not just hint text after rejection);
  confirm R4.a surfaces premise state without telling the LLM what
  to say.
- Q4: confirm R2.a counter correctly tracks DISCOVER clarification
  state across turns without leaking content.
- Q5: confirm no semantic decision moved from LLM to Java (escalation
  posture / UC choice / follow-up policy / FAQ-grounded resolve gate
  all unchanged); R4 does NOT inject a decision rule.
- Q7: PII / safety / grounding floors unchanged.
- Q8: generalization coverage matches §7 stanza counts (target 6 incl.
  c7 for R4 surface).
- Q9: no temporary hardcode; all changes are durable.

## Handoff requirements (dev authors `docs/sprints/sprint-078-handoff.md`)

Dev handoff §1 must include:

- For each of #1-#7: file:line ranges of the change + the rationale
  paragraph + the anti-误杀 test name(s) that gate it.
- For #5: the **pre-fix `max-repeated-same-action` scope audit
  outcome** (DISCOVER-only OR phase-guarded), with cited call sites.
- For #3: the **exact field name on the live `Turn` / `BotResponse`
  record** that represents the bot's free-text reply (NOT
  `user_message`), with cited code anchor.
- For #6/#7: the **R4 wiring evidence** — projection sample for at
  least 3 UC-A/UC-FP no-ad_id traces showing
  `customer_context_status: missing_ad_id` + `ad_reference.form_ad_id:
  null` + `listing_lookup: skipped` (or `missing`); 3 with-ad_id
  negatives showing NOT `missing_ad_id`.
- Java test results (full numeric: passed / failed / skipped / errors).
- Eval pytest results (unchanged, just re-confirmed).
- A one-paragraph characterization of the `bad_cases` corpus on the
  PRE-fix baseline measuring (a) intake-UC first-call rejection count
  (UC-G/H/I/J/K), (b) DISCOVER clarification cap-hit count (expected
  0), (c) `turn_budget_exhausted` count attributable to
  `max-repeated-same-action` budget (per c9). This is the
  pre-re-bless evidence baseline.
- The exact HUMAN re-bless command (per the M-Auto-5 close pattern)
  ready-to-launch with preconditions, abort criteria, and forensic
  policy. Output dir: `eval_interactive/results/m-auto-6-baseline-r1r2r4-YYYYMMDD/`.
- A clear "what is wiring evidence" vs "what is real-LLM evidence"
  separator per §5.7 mocked-LLM gate.
- STOP confirmations (file fence respected; no forbidden file touched).

## Commit discipline

Recommended commit split (per proposal §9 risk mitigation row):

1. **Commit 1 — R1.a (#1 + #2)**: `ContextProjectionBuilder.java` schema
   + per-UC required-fields projection + `IntakeFieldsProjectionTest`.
2. **Commit 2 — R2.a step 1 (#3)**: `AgentRunLoopImpl.java` counter
   wiring + `DiscoverClarificationCounterTest`.
3. **Commit 3 — R2.a step 2 (#4)**: `ContextProjectionBuilder.java`
   budget projection field + test.
4. **Commit 4 — R2.a step 3 (#5)**: `ControlKernel.java` mapping fix
   + `MapBudgetToClarificationLabelTest` (incl. multi-phase negative).
5. **Commit 5 — R4.a (#6 + #7)**: `ContextProjectionBuilder.java`
   `customer_context_status` enum (priority-ordered) + `ad_reference`
   block (4-value listing_lookup) + `CustomerContextStatusProjectionTest`
   + `AdReferenceProjectionTest`.
6. **Commit 6 — Dev handoff**: `docs/sprints/sprint-078-handoff.md`.

Each commit message follows the established convention (see git log for
sprint-077 commits as reference). All six commits are by the dev agent;
deliver-agent commits its own close-archive artifacts separately at
sub-sprint close.

## Self-check checklist (dev completes before claiming done)

- [ ] Each of #1-#7 implemented with file:line ranges captured in §1
      of the handoff.
- [ ] Each of the new anti-误杀 tests in #8 GREEN (across 5 new test
      classes), covering both positive AND negative cases.
- [ ] **#3 field name confirmed**: handoff cites the exact runtime
      field representing the bot's free-text reply (NOT
      `user_message`).
- [ ] **#5 scope audit completed**: handoff documents whether
      `max-repeated-same-action` is DISCOVER-only OR the re-map is
      phase-guarded.
- [ ] **#6 priority order verified**: tests confirm `missing_email` /
      `missing_ad_id` / `lookup_failed` / `lookup_skipped` take
      precedence over `loaded` (esp. the c7 `loaded`-vs-`missing_ad_id`
      case).
- [ ] **#7 listing_lookup 4-value verified**: tests confirm `missing`
      (ran-but-no-result) is distinct from `skipped` (never-ran).
- [ ] **R4 wiring sample** in handoff §1: ≥3 no-ad_id traces show
      `missing_ad_id`; ≥3 with-ad_id negatives do NOT.
- [ ] Java baseline `1244 / 1 / 0 / 2` + new tests, no regressions.
- [ ] Eval pytest `553` unchanged (no eval-side edits in this sub-sprint).
- [ ] No file outside the file fence has been touched.
- [ ] No `IntakeFieldsRegistry` content changed (only iterated).
- [ ] No new `escalation_reason` enum values added (only re-mapping at
      #5).
- [ ] Anti-误杀 invariants 1-12 in §Anti-误杀 NOT violated by any test
      or code change.
- [ ] PRE-fix corpus characterization included in handoff §1
      (intake first-call rejection rate / DISCOVER cap-hit / mapping
      mis-label count).
- [ ] §7 stanza copied verbatim into the handoff.
- [ ] HUMAN re-bless command drafted in handoff §9 with preconditions +
      abort criteria + forensic policy + output dir
      `m-auto-6-baseline-r1r2r4-YYYYMMDD/` (handed to human; NOT
      launched by dev).
- [ ] `baseline_dir` NOT moved (M-Auto-5 close set it to
      `m-auto-5-baseline-20260604-simfixed-stalledfix`; the next move
      happens at S-Auto-23 close after re-bless + paired-evidence).
- [ ] `docs/current_eval_baseline.md` UNCHANGED (next flip is at
      S-Auto-23 close, OR at M-Auto-6 close if S-Auto-23 ships without
      a baseline move).
