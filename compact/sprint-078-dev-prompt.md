# Sprint 078 / S-Auto-23 / M-Auto-6 — Sub-sprint A — Dev Prompt (R1+R2+R4 bundle: intake-UC `request_handover` schema + DISCOVER clarification counter wiring + ad-context premise projection slot)

## Role identity

You are the **dev agent** for **Sprint 078 / S-Auto-23 / M-Auto-6
Sub-sprint A**.

Your one-sentence goal: ship R1.a (intake-UC `request_handover` tool
schema declares `intake_fields` + per-active-UC required-fields
projection), R2.a (DISCOVER clarification counter wired on the live
`AgentRunLoopImpl` path + budget projection LLM-facing soft signal +
`mapBudgetToEscalationReason` re-maps `max-repeated-same-action` to
existing `clarification_budget_exhausted` enum value), and R4.a
(ad-context premise projection slot — `customer_context_status` enum
+ `ad_reference` structured fields surfacing already-observed runtime
state; zero keyword matching of user messages) — three runtime/projection
items that share `ContextProjectionBuilder.java` edit context; zero
semantic / yaml / CaseSpec edit.

## Read order (minimized)

1. `AGENTS.md` (auto-loaded — already in your context via this prompt).
2. **This prompt** (contains the full sub-sprint contract — do NOT read
   `docs/sprint_objective.md` or `docs/milestone_objective.md` for scope;
   they reference this prompt).
3. **Code anchors only as needed** (cited inline in §Scope below): the
   files you will read and edit. Do NOT read other surfaces.

The contract below is COMPLETE. Do not infer additional scope from any
other file.

## Cumulative context (one-page)

- **M-Auto-5 CLOSED 2026-06-05** (Class A — APPROVE_WITH_NON_BLOCKING_OBSERVATIONS;
  Codex `blocking_count=0`). `baseline_dir` =
  `eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/`;
  honest measurement floor across all four columns
  (eval-read / runtime-stamp / input-simulator / gate-vacuous-pass +
  runtime-stamp-downgrade).
- **M-Auto-6 OPENED 2026-06-05** — runtime substrate hygiene at intake +
  DISCOVER surfaces (NOT a semantic milestone). This sub-sprint is
  M-Auto-6's first.
- **Falsifiable hypothesis** (proposal §5.3): R1 + R2 are the dominant
  residual noise sources on intake-UC + DISCOVER surfaces of the post-M-Auto-5
  baseline. If true, post-fix re-bless should show `bad_cases` `reducible-flaky`
  6/12 → ≤ 2/12, with uc_f_billing + uc_fp_removed rising toward stable
  ~1.00. Anti-误杀 invariants must hold (anchor uc_g_gdpr / uc_h_appeal /
  uc_i_payment / uc_j_safety + shadow cs38s* at 0.000 stable UNCHANGED).
- **Source-of-truth proposal**:
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`
  (Path-2 bad-case-driven, research-agent 2026-06-05, traces c1–c9; R1
  = c1/c5/c6, R2 = c3/c9). Code anchors verified at HEAD `021a86a` per
  proposal §4.

## Class (layer classification)

**Layer (per `iteration_governance.md` §3.2):**

- **R1.a** → `prompt_projection` (projects existing `IntakeFieldsRegistry`
  contract as schema declaration + per-active-UC required-fields hint).
- **R2.a** → `infra` + `skill_state` (live-path wiring of existing
  dead-code `BudgetChecker.maxClarificationRounds(=2)` counter + budget
  projection + relabeled escalation_reason mapping).
- **R4.a** → `prompt_projection` (surfaces already-observed runtime
  state — `form_context.ad_id` presence + `lookup_listing_or_ad` tool
  result + `get_customer_context` tool result — as a structured
  boolean/enum slot; zero content matching of user messages).

**§7 stanza requirement:** REQUIRED — R1.a + R4.a both touch the
LLM-facing projection surface. Full stanza below in §§7.

**Tier-0 invariant:** NONE added. R1.a projects an existing validator
contract; R2.a wires existing dead-code budget; R4.a projects
already-observed runtime state. None requires a new runtime-level
safety floor.

**Semantic hardcode:** NONE introduced.
- R1.a's per-active-UC required-fields list is iterated from
  `IntakeFieldsRegistry.java:53-67` at projection time (zero per-UC
  matrix in this code; registry is the single source of truth).
- R2.a counts cardinality (clarification turns matching strict
  structural criteria); zero content matching. R2.a's mapping fix
  re-maps to an EXISTING enum value, no new enum.
- R4.a `customer_context_status` enum value derives ENTIRELY from
  runtime state (form_context fields + tool result state); ZERO
  keyword matching of user message content; NO reason text emitted.
  `ad_reference` block surfaces structural ground-truth state only.

## Goal

Eliminate two LIVE-path runtime wiring defects (R1 + R2) + surface the
ad-context premise signal (R4) that together remove noise from the
intake-UC handover + DISCOVER clarification + UC-A-without-ad_id
surfaces of `m-auto-5-baseline-20260604-simfixed-stalledfix`.

After this sub-sprint ships and the HUMAN-LAUNCHED re-bless runs:

- **R1**: every `request_handover` first call on UC-G / UC-H / UC-I /
  UC-J / UC-K arrives at the validator with the required `intake_fields`
  already populated (NO first-call rejection-and-retry pattern).
- **R2**: DISCOVER clarification turns on the live `AgentRunLoopImpl`
  path increment `session.clarificationCount`; the LLM sees the budget
  as a projected soft signal; when the cap is reached, the loop
  escalates with the correctly-labeled `clarification_budget_exhausted`
  escalation_reason (NOT the misleading `turn_budget_exhausted`).
- **R4**: when form_context lacks `ad_id` and the user references their
  own advert, the LLM-facing projection includes
  `customer_context_status: missing_ad_id` and
  `ad_reference: {form_ad_id: null, listing_lookup: missing}`. R4 is
  the ENABLER for OBS-S1 (UC-A verify-ad procedure step) which is
  autoloop work AFTER M-Auto-6 close. This sub-sprint ships only the
  runtime signal; the skill-yaml step that uses the signal is
  intentionally deferred so this sub-sprint stays runtime-only.

## Scope (executable, #1–#9)

### #1 — R1.a step 1: `request_handover` tool schema declares `intake_fields`

**Anchor:** `server/src/main/java/.../ContextProjectionBuilder.java` —
`buildRequestHandoverArgsSchema()` at line 116-120 + the args schema
body at line 200-260.

**Change:** Add `intake_fields` as an OPTIONAL `object` JSON-schema
property to the `request_handover` args schema. Semantics: free-form
`string → string` key-value map (DO NOT enumerate per-UC properties
in the schema itself — that would be the per-UC matrix forbidden by
§1.7).

Description text (LLM-facing): "Required for intake UCs (UC-G / UC-H /
UC-I / UC-J / UC-K) — see the `required_intake_fields_for_active_uc`
projection field for the active UC's required-fields list."

**Why:** the validator at `SkillGuardrailDispatcher.java:265-298`
already expects `session.intakeFields` to contain the required fields,
and `AgentRunLoopImpl.java:486-494` already merges
`call.arguments.intake_fields` into session BEFORE running the
validator. The schema simply did not declare the field for the LLM to
populate. Result: the LLM's first call never carries `intake_fields`,
fails the validator with `intake_required_fields_missing_for_intake_complete`,
and only recovers on the retry after the hint. This burns ≥ 1 turn
budget per intake handover (c1 = 6 turns no clean handover, c5/c6
recover on retry).

### #2 — R1.a step 2: per-active-UC required-fields hint projection

**Anchor:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add a new projection field `required_intake_fields_for_active_uc` near
the existing `intake_state` projection.

**Change:** Read `IntakeFieldsRegistry.requiredFieldsFor(activeUseCase)`
at projection time. When `activeUseCase` is an intake UC (UC-G / UC-H /
UC-I / UC-J / UC-K), emit the resulting `List<String>` (e.g.
`["report_target", "report_type", "description"]` for UC-J). When
`activeUseCase` is null OR a non-intake UC, the field is `null` or
omitted (DO NOT emit empty list — distinguish "not applicable" from
"no fields required").

**Why:** gives the LLM a per-turn structured hint of EXACTLY which
field keys to populate in the next `request_handover.arguments.intake_fields`
call. The required-fields list is sourced from the SAME registry
(`IntakeFieldsRegistry.java:53-67`) that the validator uses — single
source of truth, no per-UC matrix replicated here.

### #3 — R2.a step 1: DISCOVER clarification counter live-path wiring

**Anchor:** `server/src/main/java/.../AgentRunLoopImpl.java` — DISCOVER
branch, after a turn completes (BEFORE the next turn's budget check).

**Change:** Identify the turn as a "free-text clarification" iff ALL
the following STRUCTURAL criteria hold (zero content matching):

- `session.currentPhase == DISCOVER`,
- the turn produced ZERO tool calls,
- the turn did NOT commit an active use case (`session.activeUseCase`
  unchanged from before the turn),
- the turn produced a NON-EMPTY `user_message` (bot replied with
  free text rather than no-op).

When all four hold, `session.clarificationCount += 1`.

**Why:** the live `AgentRunLoopImpl` path never increments
`session.clarificationCount`; the only existing `+1` site at
`PhaseEvaluator.java:880` is on the legacy path that the live loop does
not reach. The counter therefore stays at 0 forever and the
`BudgetChecker.java:32-37` cap is unreachable on the live path (c3 = 2
verbatim-identical clarifications, no dedup, no budget escape).

**Hard fence:** DO NOT use content / similarity / Jaccard heuristics
to decide whether a turn is a "clarification". Structural cardinality
only.

### #4 — R2.a step 2: budget projection (LLM-facing soft signal)

**Anchor:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add `budgets.clarification` projection field near the existing budget
projections (line 351 / 658 vicinity).

**Change:** Emit `budgets.clarification: {used: session.clarificationCount,
max: controlPolicy.maxClarificationRounds}` per turn. Cardinality only;
LLM may or may not adapt (per §1.3 LLM owns next-action).

**Why:** observable state for the LLM to make better DISCOVER
sequencing decisions BEFORE the hard cap fires. Anti-误杀 invariant
preserved (no semantic hardcode; LLM still owns the decision).

### #5 — R2.a step 3: `mapBudgetToEscalationReason` label fix

**Anchor:** `server/src/main/java/.../ControlKernel.java` —
`mapBudgetToEscalationReason` method around line 305-308 + the
escalation inject path.

**Change:** When the budget hit is `max-repeated-same-action` (an
EXISTING `BudgetChecker` budget type, currently fall-through-mapped to
the generic `turn_budget_exhausted`), re-map to the EXISTING
`clarification_budget_exhausted` enum value (member of the 23-value
`escalation_reason` enum set). Do NOT add new enum values. Do NOT
touch other budget mappings.

**Why:** c9 surfaces the label-misleading effect — `max-repeated-same-action`
is the budget that fired but the inject is stamped `turn_budget_exhausted`,
muddying the eval signal and downstream paired-evidence review.

**Hard fence:** no enum widening. `D-new-escalation-reason-enum`
(action_bank §4 deferred) + M-Auto-3 §4 verdict explicitly forbid
adding a new enum value. The 23-value enum already contains
`clarification_budget_exhausted` — reuse it.

### #6 — R4.a step 1: `customer_context_status` enum slot in projection

**Anchor:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add a new top-level projection field `customer_context_status` to the
per-turn projection.

**Change:** Compute the enum value from runtime state (no content
matching):

| Condition (computed from runtime state) | Emit value |
|---|---|
| `get_customer_context` tool result is in session state AND populated | `loaded` |
| `form_context.email` is null/blank (no email to do lookup with) | `missing_email` |
| `form_context.ad_id` is null/blank (no ad_id to do listing lookup with) | `missing_ad_id` |
| `lookup_listing_or_ad` was triggered but failed | `lookup_failed` |
| `lookup_listing_or_ad` was not triggered (ad_id provided but tool not called) | `lookup_skipped` |

Field type: enum/string with the five allowed values above.

**Why:** when form_context lacks ad_id and the user references their own
advert (c7 pattern), the LLM-facing projection currently has no way to
know that the runtime cannot verify the ad reference. Surfacing this as
a structured enum gives the LLM observable premise state without telling
it WHAT to say (§1.3 LLM owns next-action).

**Hard fence:** DO NOT emit a reason string. ONLY the enum value. DO
NOT match keywords like "my ad" / "my listing" in the user message to
decide what to emit — derive ENTIRELY from runtime form_context + tool
result state.

### #7 — R4.a step 2: `ad_reference` structured fields

**Anchor:** `server/src/main/java/.../ContextProjectionBuilder.java` —
add a new `ad_reference` projection block (top-level or nested in
`form_context` per existing schema convention).

**Change:** Emit `ad_reference: {form_ad_id: <value>|null,
listing_lookup: <ok|missing|failed>}`:

- `form_ad_id`: the literal `form_context.ad_id` value (null if absent).
- `listing_lookup`: `ok` if `lookup_listing_or_ad` returned a result
  that resolves the ad; `missing` if the tool was not triggered or
  returned no listing; `failed` if the tool errored.

**Why:** complements #6 with the ground-truth state the LLM may need to
reason about whether to ask for ad clarification. R4.a slot pair (#6
enum + #7 structured fields) is the complete projection surface for
the "premise unverified" condition.

**Hard fence:** ONLY runtime-observed fields. NO inference about user
intent. NO reason text. Field MUST NOT contain any LLM-generated
content. Block emitted regardless of active UC (NOT a per-UC matrix —
fires every turn; UC-A is just the case where it materially helps).

### #8 — Anti-误杀 counter-tests (positive + negative)

**Anchors (new test classes):**

- `server/src/test/java/.../IntakeFieldsProjectionTest.java`
- `server/src/test/java/.../DiscoverClarificationCounterTest.java`
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java`
- `server/src/test/java/.../CustomerContextStatusProjectionTest.java`
- `server/src/test/java/.../AdReferenceProjectionTest.java`

**Change:** For each of #1-#7, ship anti-误杀 paired tests:

- **#1/#2 positive**: each intake UC (UC-G / UC-H / UC-I / UC-J / UC-K)
  projects the correct required-fields list.
- **#1/#2 negative**: non-intake UCs (UC-A with `ad_id`, UC-B, UC-D,
  UC-F, UC-FP): `required_intake_fields_for_active_uc` is `null` /
  omitted (NOT empty list). The schema MAY include `intake_fields` for
  all UCs (per proposal §9 — schema field is universal optional, only
  projected REQUIREMENT is per-UC).
- **#3 positive**: DISCOVER turn with zero tool calls + uncommitted UC
  + non-empty user_message increments counter by exactly 1.
- **#3 negative (4 separate tests)**:
  - DISCOVER turn with at least one tool call → counter NOT incremented.
  - DISCOVER turn that commits a UC → counter NOT incremented.
  - DISCOVER turn with empty user_message → counter NOT incremented.
  - Non-DISCOVER phase turn (e.g. RESOLVE / INTAKE / CONFIRM / CLOSE) →
    counter NOT incremented.
- **#4**: projection emits `budgets.clarification` with correct
  used/max; verify present when phase==DISCOVER and absent otherwise.
- **#5 positive**: `max-repeated-same-action` budget hit → emits
  `clarification_budget_exhausted` escalation_reason in the runtime
  inject.
- **#5 negative**: other budget types still emit their existing
  escalation_reasons (`max-clarification-rounds` → unchanged;
  `max-faq-miss` → unchanged; default fall-through →
  `turn_budget_exhausted` for non-clarification budgets).
- **#6 positive (5 tests)**: one per enum value (`loaded` /
  `missing_email` / `missing_ad_id` / `lookup_failed` /
  `lookup_skipped`), each verifying the exact runtime condition that
  triggers it.
- **#6 negative (3 tests)**: (a) user message contains "my ad" /
  "my listing" / similar but runtime state unchanged → enum value
  unchanged; (b) `customer_context_status` does NOT depend on active
  UC (must be the same for any UC at a given runtime state); (c)
  changes to OTHER projection fields (intake_state, budgets, etc.)
  do NOT affect the enum value.
- **#7 positive**: each `listing_lookup` value (`ok` / `missing` /
  `failed`) emitted under the matching runtime tool-result state;
  `form_ad_id` = literal value when present; `form_ad_id` = null
  when absent.
- **#7 negative (2 tests)**: (a) block emitted regardless of active
  UC (e.g. UC-A with ad_id, UC-J without ad_id, UC-D — all emit
  `ad_reference`); (b) block content does NOT contain any reason text
  or LLM-generated string.

**Anti-误杀 fence**: 
- Any test exercising a UC-J / UC-H / UC-I intake handover MUST also
  assert validator-side contract (`SkillGuardrailDispatcher` reject
  behaviour) is UNCHANGED when `intake_fields` is absent.
- Any test that exercises R4 emit MUST verify DOWNSTREAM consumers
  (`accumulated_tool_results`, `intake_state`, `form_context`
  projections) are unchanged in shape — R4 adds a new field; existing
  fields are not modified.

### #9 — HUMAN-LAUNCHED post-fix re-bless (real-LLM, §5.7)

**You DO NOT launch the re-bless.** Provide the human a ready-to-launch
command in `docs/sprints/sprint-078-handoff.md` §6:

- Backend rebuild required (you ran Java changes #3, #5).
- Proposed output dir:
  `eval_interactive/results/m-auto-6-baseline-r1r2-YYYYMMDD/`.
- Multi-suite re-bless template matching the M-Auto-5 close pattern
  (three suites: bad_cases + anchor_outcome + shadow;
  `samples_per_case` per current re-bless protocol; real-LLM;
  deterministic at `simulator_temperature=0.0`).
- Preconditions: working tree clean; backend rebuilt; M-Auto-5 baseline
  pointer NOT moved by you (it is in place at
  `m-auto-5-baseline-20260604-simfixed-stalledfix`).
- Abort criteria: anchor uc_g_gdpr / uc_h_appeal / uc_i_payment /
  uc_j_safety + shadow cs38s* rise above 0.000 stable on the new
  baseline (anti-误杀 violation).
- Forensic policy: keep prior M-Auto-5 forensic dirs.

**You characterize the PRE-fix corpus** before claiming done (handoff
§1 evidence baseline; see Handoff Requirements below).

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety at
   0.000 stable** must NOT shift after R1+R2+R4.
2. **Shadow cs38s* (scam+harassment family) at 0.000 stable** must NOT
   shift.
3. **No `SkillGuardrailDispatcher` reject-logic edits.** The validator
   remains the last line of defence.
4. **`IntakeFieldsRegistry.java:53-67` field-definition contents
   UNCHANGED.** R1.a iterates the existing registry.
5. **R2.a counter is cardinality-only**; no content / semantic
   similarity detection.
6. **NO new `escalation_reason` enum values.** R2.a's #5 re-maps to
   an existing enum value.
7. **NO yaml prompt / skill procedure edit.** OBS-S1 (UC-A verify-ad
   procedure) is autoloop work AFTER M-Auto-6 close.
8. **R4.a `customer_context_status` enum + `ad_reference` block are
   STRUCTURAL ONLY** — derived from runtime state, NEVER from user
   message content / NLP / keyword matching.
9. **R4.a does NOT change any UC routing / FAQ-grounded resolve gate /
   escalation posture** — only adds projection state. Bot behaviour
   change in response is the LLM's call; the yaml side that uses the
   signal (OBS-S1) is autoloop work after M-Auto-6 close.

## Hard fences / STOP conditions

**Files allowed to edit** (the fence):

- `server/src/main/java/.../ContextProjectionBuilder.java`
- `server/src/main/java/.../AgentRunLoopImpl.java`
- `server/src/main/java/.../ControlKernel.java`
- (Read-only for R4 state queries) `server/src/main/java/.../FormContextIngestionService.java` — DO NOT edit; R4.a reads its existing public state to drive projection.
- `server/src/test/java/.../IntakeFieldsProjectionTest.java` (new)
- `server/src/test/java/.../DiscoverClarificationCounterTest.java` (new)
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java` (new)
- `server/src/test/java/.../CustomerContextStatusProjectionTest.java` (new)
- `server/src/test/java/.../AdReferenceProjectionTest.java` (new)
- (Optional) `server/src/test/java/.../ContextProjectionBuilderTest.java`
  (existing — only update if existing tests asserted exact schema
  field set per proposal §9 risk row).
- `docs/sprints/sprint-078-handoff.md` (your dev handoff).

**Files FORBIDDEN to edit**:

- Any prompt / yaml under `server/src/main/resources/`.
- `IntakeFieldsRegistry.java`.
- `SkillGuardrailDispatcher.java`.
- `BudgetChecker.java`.
- `control-policy.yaml`.
- `FormContextIngestionService.java` (R4.a reads its state; does not modify).
- `ResolveArticleTool.java`, `ResolveDispositionEvaluator.java`, any other tool/skill class.
- Any UI file (R3 / Sub-sprint B scope).
- Any `eval_interactive/case_specs/**` CaseSpec or override.
- `eval_interactive/eval_interactive/simulator/user_simulator.py`
  (M-Auto-5 S-Auto-21 fix).
- `eval_interactive/eval_interactive/scoring/composite.py` (M-Auto-5
  S-Auto-22 fix).
- `eval_interactive/eval_interactive/scoring/hard_checks.py` (M-Auto-5
  S-Auto-22 fix).
- Autoloop 5-file SHA-locked scoring set (fence-#13).
- `autoloop/config.yaml` `baseline_dir` (M-Auto-5 close set it).

**STOP conditions**:

- If structural "free-text clarification" criteria at #3 miss > 5 % of
  clarification-shaped turns on the `bad_cases` corpus characterization
  run, surface as OQ before re-bless. DO NOT widen criteria with
  content heuristics — that violates anti-误杀 §11 floor.
- If `mapBudgetToEscalationReason` change at #5 surfaces > 3 affected
  eval CaseSpecs (mismatched `expected.escalation_reason`), STOP at the
  test stage; report to deliver-agent for either CaseSpec spec-sync
  (controlled §5.4 update per proposal §9 risk row) or scope reduction.
- Any anti-误杀 invariant test FAILS in CI → STOP, do not commit.

## Test / eval requirements

- All new tests in #6 GREEN.
- Java baseline `1244 / 1 / 0 / 2` + the new tests (target around
  `1256 / 1 / 0 / 2` with the ~12-15 new tests across the three test
  classes; the 1 inherited failure stays unchanged).
- Eval pytest `553` UNCHANGED (no eval-side edits in this sub-sprint).
- Autoloop pytest `324` UNCHANGED.
- 17-fixture `31` UNCHANGED.
- Mocked-LLM tests are wiring evidence only (§5.7); real-LLM evidence
  comes from the HUMAN-LAUNCHED re-bless after you ship.

## §7 Layer-classification + anti-hardcode stanza (REQUIRED — copy verbatim into handoff)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection (R1.a `request_handover`
schema declaration + per-active-UC required-fields projection;
R4.a `customer_context_status` enum + `ad_reference` block) +
infra (R2.a counter wiring + budget projection + mapping fix) +
skill_state (R2.a counter accumulation across turns).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.
(R1.a projects an existing IntakeFieldsRegistry contract; R2.a wires an
existing BudgetChecker.maxClarificationRounds(=2) budget that has been
declared since substrate-hygiene M-Auto-3 / control-policy.yaml; R4.a
projects already-observed runtime form_context state + tool result
state. None requires a new runtime-level safety floor.)

**Semantic hardcode:** No semantic hardcode introduced.
(R1.a's per-active-UC required-fields list is read from
IntakeFieldsRegistry at projection time — zero per-UC matrix in this
code; registry is the single source of truth. R2.a counts cardinality
(clarification turns matching strict structural criteria: phase ==
DISCOVER && no tool calls && no UC commit && non-empty user_message);
zero content matching. R2.a's mapping fix re-maps to an existing
clarification_budget_exhausted enum value, NOT a new enum. R4.a
`customer_context_status` enum value derives ENTIRELY from runtime
state — form_context.email/ad_id presence + lookup_listing_or_ad tool
result + get_customer_context tool result — zero keyword matching of
user message content; no reason text. `ad_reference` block surfaces
structural ground-truth state only.)

**Generalization coverage:** target / neighbor / negative / shadow case counts: 6 / ~22 / ~12 / ~22
- target: c1 (UC-J), c5 (UC-J), c6 (UC-I), c3 (DISCOVER 2x clarification),
  c9 (DISCOVER label mis-stamp), c7 (UC-A no ad_id — R4 surface)
- neighbor: every UC-G/UC-H/UC-I/UC-J/UC-K case_families in bad_cases
  + anchor_outcome; every DISCOVER-phase budget-exit case across all
  three suites; UC-A + UC-FP cases where form_context lacks ad_id
- negative: non-intake UCs (UC-A with ad_id, UC-B, UC-D, UC-F, UC-FP)
  must not be falsely activated; DISCOVER turns with tool calls or UC
  commits must not increment clarificationCount; UC-A WITH ad_id MUST
  emit `customer_context_status` = `loaded` or `lookup_skipped`;
  user messages with "my ad" / "my listing" WITHOUT matching runtime
  state change MUST NOT shift `customer_context_status`
- shadow: existing shadow suite (cs01s* / cs15s* / cs32s* / cs38s* /
  cs59s* / cs76s* / cs92s*); at minimum verify shadow UC-K + UC-J +
  UC-A + UC-I + UC-G coverage holds
```

## Codex review plan (§4.3)

**Per-sub-sprint Codex review REQUIRED** because R1.a + R4.a both
touch the `prompt_projection` LLM-facing surface (semantic-touching per
`iteration_governance.md` §7 + `process/milestone-framework.md` §4.3).

Codex prompt artifact: `compact/sprint-078-codex-review-prompt.md`
(deliver-agent authors at sub-sprint close; embeds §4.1 nine-question
kernel + §7 stanza + file-path fence + anti-误杀 invariants +
generalization coverage).

**Focus points for Codex** (per §4.3):

- Q1: confirm no per-UC keyword/regex/matrix added in
  `ContextProjectionBuilder` for the required-fields projection (must
  iterate `IntakeFieldsRegistry`); confirm R4.a
  `customer_context_status` enum derivation is purely structural
  (zero content matching on user message).
- Q3: confirm projection change #2 makes the LLM's contract with the
  validator explicit (not just hint text after rejection); confirm
  R4.a surfaces premise state without telling the LLM what to say.
- Q4: confirm R2.a counter correctly tracks DISCOVER clarification
  state across turns without leaking content.
- Q5: confirm no semantic decision moved from LLM to Java (escalation
  posture / UC choice / follow-up policy / FAQ-grounded resolve gate
  all unchanged); R4 does NOT inject a decision rule.
- Q7: PII / safety / grounding floors unchanged.
- Q8: generalization coverage matches the §7 stanza counts (target 6
  incl. c7 for R4 surface).
- Q9: no temporary hardcode; all changes are durable.

## Handoff requirements (you author `docs/sprints/sprint-078-handoff.md`)

§1 of your handoff must include:

- For each of #1-#5: file:line ranges of the change + the rationale
  paragraph + the anti-误杀 test name(s) that gate it.
- Java test results (full numeric: passed / failed / skipped / errors).
- Eval pytest results (unchanged from 553, just re-confirmed).
- Autoloop pytest results (unchanged from 324, just re-confirmed).
- A one-paragraph **PRE-fix corpus characterization** on `bad_cases`
  measuring:
  - (a) Intake-UC first-call rejection count (UC-G/H/I/J/K).
  - (b) DISCOVER clarification cap-hit count (expected 0 pre-fix).
  - (c) `turn_budget_exhausted` count attributable to
    `max-repeated-same-action` budget (per c9 label-misleading).
  This is the pre-re-bless evidence baseline; post-fix re-bless will
  be compared against it.
- The exact HUMAN re-bless command (template from the M-Auto-5 close
  pattern) ready-to-launch with preconditions, abort criteria, and
  forensic policy.
- A clear separator between "wiring evidence" (mocked-LLM tests) and
  "real-LLM evidence" (HUMAN-LAUNCHED re-bless) per §5.7.
- STOP confirmations:
  - File fence respected (no forbidden file touched).
  - `IntakeFieldsRegistry` content unchanged.
  - `SkillGuardrailDispatcher` reject logic unchanged.
  - `BudgetChecker` unchanged.
  - No new `escalation_reason` enum values.
  - No yaml / prompt / CaseSpec / simulator / scoring SHA touched.
  - `baseline_dir` NOT moved.
  - `docs/current_eval_baseline.md` NOT edited.

## Commit discipline

Recommended commit split (per proposal §9 risk mitigation row):

1. **Commit 1 — R1.a (#1 + #2)**: `ContextProjectionBuilder.java`
   schema + per-UC required-fields projection + `IntakeFieldsProjectionTest`.
2. **Commit 2 — R2.a step 1 (#3)**: `AgentRunLoopImpl.java` counter
   wiring + `DiscoverClarificationCounterTest`.
3. **Commit 3 — R2.a step 2 (#4)**: `ContextProjectionBuilder.java`
   budget projection field + test (extend existing or new test class).
4. **Commit 4 — R2.a step 3 (#5)**: `ControlKernel.java` mapping fix
   + `MapBudgetToClarificationLabelTest`.
5. **Commit 5 — R4.a (#6 + #7)**: `ContextProjectionBuilder.java`
   `customer_context_status` enum + `ad_reference` block +
   `CustomerContextStatusProjectionTest` + `AdReferenceProjectionTest`.
6. **Commit 6 — Dev handoff**: `docs/sprints/sprint-078-handoff.md`.

Commit messages follow the established convention (see `git log` for
sprint-077 commits as reference, e.g. `Sprint 077 / S-Auto-22 / M-Auto-5 — <topic>`).

All five commits are by you (dev agent). Deliver-agent commits its own
close-archive artifacts separately at sub-sprint close.

## Self-check checklist (complete BEFORE claiming done)

- [ ] Each of #1-#5 implemented; file:line ranges captured in handoff §1.
- [ ] Each new anti-误杀 test in #6 GREEN, covering both positive AND
      negative cases.
- [ ] Java baseline `1244 / 1 / 0 / 2` + new tests; no regressions
      (inherited 1 failure unchanged).
- [ ] Eval pytest 553 unchanged (no eval-side edits in this sub-sprint).
- [ ] Autoloop pytest 324 unchanged.
- [ ] No file outside the file fence has been touched.
- [ ] `IntakeFieldsRegistry` content UNCHANGED (only iterated).
- [ ] No new `escalation_reason` enum values added (only re-mapping at
      #5).
- [ ] Anti-误杀 invariants 1-7 in §Anti-误杀 NOT violated by any test or
      code change.
- [ ] PRE-fix corpus characterization included in handoff §1
      (intake first-call rejection rate / DISCOVER cap-hit /
      mapping mis-label count).
- [ ] §7 stanza copied VERBATIM into the handoff.
- [ ] HUMAN re-bless command drafted in handoff §6 with preconditions +
      abort criteria + forensic policy (handed to human; NOT launched
      by you).
- [ ] `baseline_dir` NOT moved.
- [ ] `docs/current_eval_baseline.md` UNCHANGED.

When all checked: hand back to deliver-agent for close review +
per-sub-sprint Codex dispatch + human re-bless launch decision.
