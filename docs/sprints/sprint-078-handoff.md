---
title: Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A dev handoff — R1.a + R2.a + R4.a runtime/projection
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: code (server/.../runtime/{ContextProjectionBuilder,AgentRunLoopImpl,ControlKernel}.java)
last_reviewed: 2026-06-05
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  M-Auto-6's first sub-sprint. Three runtime/projection items sharing
  ContextProjectionBuilder edit context: R1.a (request_handover intake_fields
  schema + per-UC required-fields projection), R2.a (DISCOVER clarification
  counter live-path wiring + budget projection + phase-aware mapping fix),
  R4.a (ad-context premise projection slot). ZERO semantic / yaml / CaseSpec /
  simulator / scoring edit. Mocked-LLM Java tests are wiring evidence (§5.7);
  the HUMAN-launched real-LLM re-bless (§6) is the eval evidence gate and is
  NOT run by this dev session.
---

# Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — dev handoff

## §0 Cold-start summary + verdict

**Goal.** Eliminate two LIVE-path runtime wiring defects (R1 schema gap, R2
dead-code clarification counter + mislabel) + surface the ad-context premise
signal (R4) — the intake-UC handover, DISCOVER clarification, and
UC-A-without-ad_id noise sources on the post-M-Auto-5 baseline
`m-auto-5-baseline-20260604-simfixed-stalledfix`.

**Verdict.** All seven scope items (#1–#7) + the five anti-误杀 test classes
(#8) landed. Java `mvn -o test` = **1297 run / 1 fail / 0 err / 2 skip** =
baseline `1244 / 1 / 0 / 2` + **53 new tests**, same inherited failure. The 1
failure is the documented inherited baseline
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
(a Sprint-6 system-prompt anchor-tag assertion, unrelated to this sub-sprint) —
**verified pre-existing**: it fails identically on clean main (the three edited
mains stashed). eval pytest **553 unchanged** + autoloop pytest **324
unchanged** — by construction: `git status` shows ZERO files touched under
`eval_interactive/**` (excluding `results/`) or `autoloop/**`. Mocked-LLM tests
are wiring evidence (§5.7); the real-LLM re-bless (§6) is HUMAN-launched.

**Layer (per `iteration_governance.md` §3.2).** R1.a + R4.a = `prompt_projection`;
R2.a = `infra` + `skill_state`. No Tier-0 invariant added; no semantic hardcode;
no new `escalation_reason` enum value.

---

## §1 — Per-change evidence (#1–#7)

### #1 — R1.a: `request_handover` schema declares `intake_fields`

- **File:line** — `ContextProjectionBuilder.java:262-280` (in
  `buildRequestHandoverArgsSchema()`).
- **What** — adds an OPTIONAL `object`-typed `intake_fields` property to the
  `request_handover` args schema. Free-form string→string map; NO per-UC
  property enumeration (that would be the §1.7 per-UC matrix). Absent from the
  `required[]` array (only `escalation_reason` stays required), so the
  validator is never forced and non-intake UCs are unaffected.
- **Why** — the validator (`SkillGuardrailDispatcher.java:265-298`) already
  expects `session.intakeFields` populated, and
  `AgentRunLoopImpl.persistInlineIntakeFields` (called at the handover dispatch
  site, ~`AgentRunLoopImpl.java:486-494`) already merges
  `call.arguments.intake_fields` into the session BEFORE the validator runs.
  The schema simply never declared the field, so the LLM's first call never
  carried it → first-call reject + retry (c1/c5/c6).
- **Anti-误杀 tests** — `IntakeFieldsProjectionTest`:
  `requestHandoverSchema_declaresOptionalIntakeFieldsObject` (object type, no
  per-UC `properties`, NOT in `required[]`).

### #2 — R1.a: `required_intake_fields_for_active_uc` projection

- **File:line** — `ContextProjectionBuilder.java:427-440` (in `buildProjection`,
  immediately after the `intake_state` block).
- **What** — for an intake UC (G/H/I/J/K), emits the registry's required-fields
  `List<String>`; for null / non-intake UC, the field is OMITTED entirely (NOT
  an empty list — distinguishes "not applicable" from "none required").
- **Why** — gives the LLM a per-turn structured hint of exactly which keys to
  populate in the next `request_handover.arguments.intake_fields`. Read from
  the SAME `IntakeFieldsRegistry.requiredFieldsFor(activeUc)` the validator uses
  (`IntakeFieldsRegistry.java:53-67`) — single source of truth, no replicated
  per-UC list.
- **Anti-误杀 tests** — `IntakeFieldsProjectionTest`: 5 positive
  (`intakeUcG/H/I/J/K_projectsRegistryRequiredFields`),
  `projectedRequiredFields_matchRegistrySource` (verbatim-equals-registry
  guard), 2 negative (`nonIntakeUcs_omitRequiredIntakeFieldsField`,
  `nullActiveUc_omitsRequiredIntakeFieldsField`).

### #3 — R2.a: DISCOVER clarification counter live-path wiring

- **File:line** — increment at `AgentRunLoopImpl.java:442-457` (the no-tool-calls
  branch of `run()`); predicate at `AgentRunLoopImpl.java:1207-1213`
  (`isDiscoverFreeTextClarification`).
- **Bot-reply field name (REQUIRED confirmation).** The bot's outgoing
  free-text reply on the live path is **`action.getUserMessage()`** — captured
  as the local **`userMsg`** at `AgentRunLoopImpl.java:410`
  (`String userMsg = action.getUserMessage();`). This is the LLM's outgoing
  `user_message` field (the bot→customer message). It is **NOT** the `run()`
  `userMessage` parameter (`AgentRunLoopImpl.java:163`), which is the
  CUSTOMER's incoming turn. The increment is gated on `userMsg`; the predicate
  takes only the bot reply, so a customer message can never be misclassified.
- **What** — increments `session.clarificationCount` iff ALL four STRUCTURAL
  criteria hold: phase == DISCOVER (`plan.phase()`), zero tool calls (this
  branch), no UC commit this turn (`!Objects.equals(activeUc,
  session.getActiveUseCase())` against the run-start snapshot), and a non-empty
  bot reply (`userMsg`). Cardinality only — zero content / similarity / Jaccard.
- **Why** — the live `AgentRunLoopImpl` path never incremented
  `clarificationCount`; the only existing `+1` is the legacy
  `PhaseEvaluator.java:880`, which the live loop does not reach. The counter
  stayed 0 forever, so `BudgetChecker.java:32-37`
  (`maxClarificationRounds`) was unreachable (c3).
- **Anti-误杀 tests** — `DiscoverClarificationCounterTest`: positive
  (`discoverNoToolNoCommitWithBotReply_increments`,
  `nonQuestionFreeTextStillCounts_structuralNotContentBased`) + 5 negatives
  (tool call, UC commit, empty bot reply, non-DISCOVER phase,
  `customerMessageFieldConfusion_doesNotIncrement`).

### #4 — R2.a: `budgets.clarification` projection (LLM-facing soft signal)

- **File:line** — `ContextProjectionBuilder.java:702-718` (in `buildProjection`,
  after `budget_state`).
- **What** — emits `budgets.clarification: {used: clarificationCount, max:
  maxClarificationRounds}` ONLY in DISCOVER phase; absent in every other phase.
  Cardinality only — the LLM still owns next-action (§1.3).
- **Why** — observable state so the LLM can sequence DISCOVER turns before the
  hard cap fires (now reachable via #3).
- **Anti-误杀 tests** — present/absent-by-phase covered transitively by
  `CustomerContextStatusProjectionTest.enumIndependentOfOtherProjectionFields`
  (toggling phase toggles `budgets` without shifting the R4 enum).

### #5 — R2.a: `mapBudgetToEscalationReason` phase-aware label fix

- **File:line** — call site `ControlKernel.java:305-313`; new 3-arg overload
  `ControlKernel.java:763-776`; `isFreeTextActionKey` helper
  `ControlKernel.java:779-781`. Single-arg overload (`:726-742`) UNCHANGED.
- **PRE-FIX SCOPE AUDIT OUTCOME — outcome (b), phase-guarded.**
  `max-repeated-same-action` is **NOT DISCOVER-only**. The session-level
  counter `BotSession.repeatedActionCount` is incremented by
  `ControlKernel.trackRepeatedAction` (`:1748-1755`) at THREE call sites, none
  phase-scoped:
  - `:467` — live path, key = `deriveRunResultKey(runResult)` (a free-text
    no-tool reply yields `"answer"`);
  - `:651` and `:676` — legacy `PhaseEvaluator` action path, key =
    `deriveRepetitionKey(action)` (free-text clarification yields `"clarify"`).
  The budget fires (`BudgetChecker.java:68-72`) on ANY repeated action key in
  ANY phase (incl. a repeated RESOLVE `search_knowledge`). So a blanket re-map
  would mislabel genuine non-DISCOVER repeated-TOOL-call budgets. The overload
  therefore re-maps to `clarification_budget_exhausted` ONLY when
  `session.getCurrentPhase()==DISCOVER` AND `session.getLastAction()` is a
  free-text key (`"answer"` / `"clarify"`, never a tool name). Every other
  bucket / phase / action-key delegates unchanged to the single-arg overload.
  NO new enum value (reuses the existing `clarification_budget_exhausted`,
  member of the 23-value set at `ContextProjectionBuilder.java:227-253`).
- **Anti-误杀 tests** — `MapBudgetToClarificationLabelTest`: positive (DISCOVER
  + `answer`/`clarify` → relabeled), multi-phase guard (RESOLVE/INTAKE → stays
  `turn_budget_exhausted`), free-text guard (DISCOVER + tool key / null →
  stays `turn_budget_exhausted`), other-budgets-unchanged, and a
  single-arg-default-unchanged regression lock. The existing
  `ControlKernelEscalationReasonTest` (single-arg) stays green.

### #6 — R4.a: `customer_context_status` enum projection

- **File:line** — emit wiring `ContextProjectionBuilder.java:913-920` (in the
  live `build(...)` overload, before the early-return so it is present on every
  live turn); helpers `computeCustomerContextStatus` (`:1383-1401`),
  `addAdContextPremiseProjection` (`:1426-1450`), `formField` (form JSON parse).
- **What** — a top-level enum from `{missing_email, missing_ad_id,
  lookup_failed, lookup_skipped, loaded}`, derived ENTIRELY from runtime state
  (form_context.email / form_context.ad_id + the `lookup_listing_or_ad` tool
  event). Zero content matching of user messages; no reason text. Priority
  order is LOAD-BEARING (first match wins): `missing_email` > `missing_ad_id` >
  `lookup_failed` > `lookup_skipped` > `loaded`, so a populated customer
  context never masks an absent ad_id premise (c7).
- **Anti-误杀 tests** — `CustomerContextStatusProjectionTest`: 5 positive
  (one per value), 4 priority-order (incl. the c7 `loaded`-vs-`missing_ad_id`
  case + the no-keyword-leakage test), 2 negative (UC-independence,
  other-field-independence), + a with-ad_id `loaded` integration.

### #7 — R4.a: `ad_reference` structured fields

- **File:line** — `ContextProjectionBuilder.java:1442-1449`
  (`addAdContextPremiseProjection`); `deriveListingLookupState` (`:1364-1373`),
  `listingLookupToken` (`:1404-1414`), `ListingLookupState` enum (`:1358`).
- **What** — `ad_reference: {form_ad_id: <value>|null, listing_lookup:
  <ok|missing|failed|skipped>}`. `listing_lookup` distinguishes `missing`
  (ran-but-no-result: `lookup_listing_or_ad` success + `found:false`) from
  `skipped` (never-ran) — load-bearing per anti-误杀 #11. Runtime ground-truth
  only; emitted every live turn regardless of active UC (not a per-UC matrix).
- **Anti-误杀 tests** — `AdReferenceProjectionTest`: 4 positive
  (ok/missing/failed/skipped via state derivation + token), explicit
  `missingIsDistinctFromSkipped`, 4 build()-integration
  (ok/missing/failed/skipped + form_ad_id literal/null), 2 negative
  (UC-independence, structural-keys-only/no-reason-text), +
  `downstreamProjectionFieldsUnchangedInShape` (form_context /
  accumulated_tool_results / intake_state unchanged; ad_reference top-level not
  inside form_context).

### R4 wiring evidence (deterministic, mocked — §5.7 wiring tier)

Concrete `build(...)` projection output (deterministic; real-LLM traces are the
§6 human re-bless, NOT this dev session):

```
no-ad_id  UC-A /RESOLVE   => customer_context_status=missing_ad_id  ad_reference={"form_ad_id":null,"listing_lookup":"skipped"}
no-ad_id  UC-FP/RESOLVE   => customer_context_status=missing_ad_id  ad_reference={"form_ad_id":null,"listing_lookup":"skipped"}
no-ad_id  UC-FP/DISCOVER  => customer_context_status=missing_ad_id  ad_reference={"form_ad_id":null,"listing_lookup":"skipped"}
with-ad_id UC-A (lookup ok)      => customer_context_status=loaded         ad_reference={"form_ad_id":"ad-9","listing_lookup":"ok"}
with-ad_id UC-A (lookup no-result)=> customer_context_status=loaded        ad_reference={"form_ad_id":"ad-9","listing_lookup":"missing"}
with-ad_id UC-A (lookup not run)  => customer_context_status=lookup_skipped ad_reference={"form_ad_id":"ad-9","listing_lookup":"skipped"}
```

≥3 no-ad_id UC-A/UC-FP traces show `missing_ad_id` + `form_ad_id:null` +
`listing_lookup:skipped`; ≥3 with-ad_id traces are NEVER `missing_ad_id`.

### PRE-fix corpus characterization (evidence baseline)

Measured on the M-Auto-5 baseline `bad_cases` scored corpus
(`eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/bad_cases/aggregated.json`,
12 cases × 9 attempts), method = `jq`/python over `case_results[].attempts[]`:

- **(a) Intake-UC first-call rejection (UC-G/H/I/J/K).** Closest scored signal:
  `failure_tags` `TIER2:uc-h-intake-complete-before-handover` = **6** attempt
  occurrences (intake handover gated before intake-complete — the R1 schema-gap
  symptom). Trace-level root cause (`intake_required_fields_missing_for_intake_complete`)
  documented in the source proposal traces c1 (6 turns, no clean handover),
  c5/c6 (recover only on the post-hint retry).
- **(b) DISCOVER clarification cap-hit. = 0 (CONFIRMED from data).**
  `escalation_reason` distribution across the corpus contains
  `turn_budget_exhausted` ×26, `faq_miss_threshold_exceeded` ×15,
  `user_requested` ×45, intake-complete/account/appeal — and **ZERO
  `clarification_budget_exhausted`**. The clarification cap is never reached
  pre-fix (dead-code counter, R2 #3).
- **(c) `turn_budget_exhausted` attributable to `max-repeated-same-action`.**
  All **26** `turn_budget_exhausted` stamps are the only bucket the
  clarification-repetition exit can land in pre-fix (since
  `clarification_budget_exhausted` = 0); the c9 DISCOVER repeated-clarification
  mislabel is a subset. The 0-vs-26 split is the joint smoking gun for R2 #3
  (counter dead) and R2 #5 (mislabel).
- **Noise floor.** `bad_cases` stability = 4 reducible-flaky + 2 near-coinflip +
  6 stable (6/12 flaky-ish).

### Test results (full numeric)

- **Java** `mvn -o test`: **Tests run 1297, Failures 1, Errors 0, Skipped 2**
  (= baseline 1244/1/0/2 + 53 new). Per new class: IntakeFieldsProjectionTest 9,
  DiscoverClarificationCounterTest 8, MapBudgetToClarificationLabelTest 12,
  CustomerContextStatusProjectionTest 12, AdReferenceProjectionTest 12. The 1
  failure = inherited `SystemPromptUserRequestedTiebreakerTest` (verified
  pre-existing on clean main).
- **eval pytest** 553 — **unchanged by construction** (no `eval_interactive/**`
  file touched; git-verified).
- **autoloop pytest** 324 — **unchanged by construction** (no `autoloop/**`
  file touched; git-verified).
- **17-fixture 31** — unchanged (no fixture-affecting edit).

---

## §2 — Layer-classification + anti-hardcode stanza (verbatim, §7)

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
DISCOVER && no tool calls && no UC commit && non-empty
bot/assistant free-text reply — NOT user_message, which is the
customer's incoming turn); zero content matching. R2.a's mapping fix
re-maps an existing clarification_budget_exhausted enum value with a
phase-guard or pre-confirmed-DISCOVER-only budget, NOT a new enum. R4.a
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

---

## §6 — HUMAN-launched post-fix re-bless (real-LLM, §5.7) — DEV STOPS HERE

This dev session does **NOT** launch the re-bless and does **NOT** move
`baseline_dir`. (Self-check labels this the "§9 re-bless command"; it is the
same content.)

**Preconditions (human):**

1. **Rebuild backend** — `server/` Java changed (#3 AgentRunLoopImpl, #5
   ControlKernel, and ContextProjectionBuilder). The build is stale; rebuild
   before any session-creating run (no hot-reload —
   `feedback_restart_backend_before_eyeball`):
   `cd server && mvn -o -DskipTests package` (or `mvn -o spring-boot:run
   -Dspring-boot.run.profiles=local`; Postgres + Redis up; creds in
   `.env.local`; `curl -s localhost:8080/actuator/health` → `status:UP`).
2. **Clean committed tree** — run on a clean tree (the dev scope is committed;
   `project_autoloop_dirty_index_hazard`).
3. **Mac caffeinate** — keep the Mac awake for the full multi-suite run
   (`feedback_long_llm_run_no_sleep`); a sleep-spanned run is uncertifiable.
4. **`baseline_dir` NOT moved by dev** — still
   `m-auto-5-baseline-20260604-simfixed-stalledfix`.

**Exact command (fresh dated dir; old baseline RETAINED, pointer NOT moved):**

```bash
# Multi-suite (bad_cases + anchor_outcome + shadow), real-LLM,
# deterministic at simulator_temperature=0.0. --n per re-bless protocol
# (matches the M-Auto-5 close pattern; --n 9 was the S-Auto-21/22 depth).
cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 9 \
    --out-dir ../eval_interactive/results/m-auto-6-baseline-r1r2r4-20260605
```

(Use today's date if it differs. The script writes `<out>/<suite>/aggregated.json`
per suite + `<out>/_rebless_report.json`; the dir is gitignored.)

**Abort criteria (anti-误杀 floor — STOP if violated).** On the new baseline the
anchor + shadow floor must stay at its pre-fix value of **0.000 stable**:
anchor `uc_g_gdpr` / `uc_h_appeal` / `uc_i_payment` / `uc_j_safety` and shadow
`cs38s01_uc_j_scam_seller_full_narrative` / `cs38s02_uc_j_harassment_full_narrative`
MUST NOT rise above 0.000 stable. Any rise = anti-误杀 violation → abort +
investigate (likely measurement contamination, not a real gain).

**Forensic policy.** Keep the prior M-Auto-5 forensic dirs
(`m-auto-5-baseline-20260604-simfixed-stalledfix/` and the earlier `-simfixed/`)
read-only — not modified, not deleted.

**Validation gates (split HARD / OBSERVABLE).**

- HARD close gates (R1 + R2 effects):
  - `bad_cases` `reducible-flaky` count ≤ 2/12 (vs the 4 reducible-flaky / 6
    flaky-ish pre-fix);
  - the UC-F/UC-FP billing/removed neighbour cases rise toward stable ~1.00;
  - anti-误杀 floor preserved (anchor uc_g/h/i/j + shadow cs38s* at 0.000 stable);
  - **NEW post-fix expectation:** `clarification_budget_exhausted` should now
    appear in DISCOVER repeated-clarification exits where pre-fix showed 0
    (R2 #3 + #5 effect), and the intake first-call-reject signal
    (`TIER2:uc-h-intake-complete-before-handover`) should drop (R1 effect).
- OBSERVABLE (R4 wiring — NOT a hard close gate; OBS-S1 yaml is autoloop work
  after M-Auto-6 close):
  - Sample ≥ 3 UC-A/UC-FP no-ad_id traces → projection MUST include
    `customer_context_status: missing_ad_id` + `ad_reference.form_ad_id: null` +
    `listing_lookup: skipped` (or `missing` if the tool was triggered);
  - Sample ≥ 3 with-ad_id UC-A negatives → projection MUST NOT include
    `customer_context_status: missing_ad_id`;
  - any bot behaviour shift in response to R4 is bonus observation, not a gate.

---

## §7 — STOP confirmations

- File fence respected — only `ContextProjectionBuilder.java`,
  `AgentRunLoopImpl.java`, `ControlKernel.java`, the 5 new test classes, and
  this handoff were edited. No forbidden file touched.
- `IntakeFieldsRegistry.java` content **UNCHANGED** (only iterated via
  `requiredFieldsFor` / `isIntakeUseCase`).
- `SkillGuardrailDispatcher.java` reject logic **UNCHANGED**.
- `BudgetChecker.java` **UNCHANGED**.
- `FormContextIngestionService.java` **UNCHANGED** (R4 reads `session`
  form/tool state; does not modify it).
- **No new `escalation_reason` enum value** — #5 re-maps to the existing
  `clarification_budget_exhausted`.
- No yaml / prompt / CaseSpec / simulator (`user_simulator.py`) / scoring
  (`composite.py` / `hard_checks.py`) / autoloop 5-file SHA-locked set touched.
- `config.fitness.baseline_dir` **NOT moved**;
  `docs/current_eval_baseline.md` **UNCHANGED**.
- Full re-bless **NOT run** by this dev session (wiring evidence = mocked-LLM
  Java tests only, §5.7).

## §8 — wiring evidence vs real-LLM evidence (§5.7 separator)

- **Wiring evidence (this dev session, mocked/deterministic).** The 53 new Java
  tests + the §1 R4 deterministic `build(...)` dump. These prove
  projection/dispatch/mapping WIRING — the mock controls the measured variable,
  so they are NOT primary evidence of a behaviour change.
- **Real-LLM evidence (HUMAN-launched, §6).** The post-fix multi-suite re-bless
  is the §5.7 eval evidence gate for whether R1+R2 reduce the measured noise.
  It is not run here.

## §9 — Codex review pointers (§4.3 / prompt §"Codex review plan")

Per-sub-sprint Codex review REQUIRED (R1.a + R4.a touch `prompt_projection`).
Focus: Q1 (no per-UC keyword/regex/matrix in `ContextProjectionBuilder`;
required-fields iterate `IntakeFieldsRegistry`; R4 enum is purely structural),
Q3 (schema makes the validator contract explicit; R4 surfaces premise state
without dictating wording), Q4 (R2 counter tracks DISCOVER state without leaking
content), Q5 (no semantic decision moved LLM→Java; FAQ-grounded resolve gate /
escalation posture unchanged), Q7 (PII/safety/grounding floors unchanged), Q8
(generalization coverage matches the §2 stanza, target 6 incl. c7), Q9 (no
temporary hardcode — all changes durable).

## Files changed

- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  (#1 schema, #2 + #4 projection, #6 + #7 R4 + helpers)
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  (#3 counter wiring + predicate, `java.util.Objects` import)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
  (#5 phase-aware mapping overload + call site)
- `server/src/test/java/.../IntakeFieldsProjectionTest.java` (new, 9)
- `server/src/test/java/.../DiscoverClarificationCounterTest.java` (new, 8)
- `server/src/test/java/.../MapBudgetToClarificationLabelTest.java` (new, 12)
- `server/src/test/java/.../CustomerContextStatusProjectionTest.java` (new, 12)
- `server/src/test/java/.../AdReferenceProjectionTest.java` (new, 12)
- `docs/sprints/sprint-078-handoff.md` (this file)
