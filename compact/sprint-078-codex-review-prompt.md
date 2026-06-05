# Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A — Per-Sub-Sprint Codex Review Prompt

## Role identity

You are the **Anti-Hardcode + Per-Sub-Sprint Review Agent** for
**Sprint 078 / S-Auto-23 / M-Auto-6 Sub-sprint A**. You are reviewing
the cumulative commit range `084dd8b..af44903` (4 dev commits) on
branch `auto-loop-branch`. Your job: decide whether this sub-sprint
may proceed to the **HUMAN-LAUNCHED real-LLM re-bless** (§6 of the
dev handoff) and milestone-shared close, OR whether it requires
fix-iteration before re-bless.

This is a **per-sub-sprint review per `iteration_governance.md` §4.3**
because both R1.a and R4.a touch the LLM-facing `prompt_projection`
surface (semantic-touching). Per §4.3 the milestone-shared review at
M-Auto-6 close is the formal close gate; this review is the per-PR
gate that authorizes the human to launch the real-LLM re-bless and
the rest of the milestone work.

## Loader (minimized)

1. `AGENTS.md` (auto-loaded via constitution chain).
2. **This prompt** (contains the full review contract — embedded §4.1
   kernel + scope + focus points + output template).
3. **Dev handoff**: `docs/sprints/sprint-078-handoff.md` (dev-authored
   evidence; ~440 lines; per-change rationale + test names + pre-fix
   characterization + STOP confirmations + §5.7 wiring/real-LLM
   separator). Read this fully.
4. **Code anchors** (read as evidence; do NOT edit):
   - `server/.../ContextProjectionBuilder.java` lines 262-282
     (R1 schema), 427-441 (R1 per-UC required projection),
     702-716 (R2 budgets.clarification), 913-920 (R4 emit), 1358
     (ListingLookupState enum), 1364-1374 (deriveListingLookupState),
     1383-1401 (computeCustomerContextStatus priority), 1404-1416
     (listingLookupToken), 1426-1450 (addAdContextPremiseProjection).
   - `server/.../AgentRunLoopImpl.java` lines 410-411 (`userMsg =
     action.getUserMessage()`), 442-458 (counter increment site),
     1200-1215 (`isDiscoverFreeTextClarification` static predicate +
     javadoc clarifying botReply vs userMessage).
   - `server/.../ControlKernel.java` lines 305-313 (call site),
     733-749 (single-arg overload — UNCHANGED), 763-771 (new 3-arg
     phase-aware overload), 779-781 (`isFreeTextActionKey` helper).
   - `server/.../IntakeFieldsRegistry.java` lines 53-67 (required-fields
     definitions for UC-G/H/I/J/K — source of truth; UNCHANGED).
   - `server/.../skill/SkillGuardrailDispatcher.java` (the validator;
     UNCHANGED — confirm no reject-logic edit).
   - `server/.../BudgetChecker.java` lines 32-37 (`maxClarificationRounds`
     check; UNCHANGED).
   - `server/.../FormContextIngestionService.java` (UNCHANGED; R4
     reads its session state).
   - New test classes (53 tests total):
     `server/.../test/.../IntakeFieldsProjectionTest.java` (9 tests),
     `DiscoverClarificationCounterTest.java` (8 tests),
     `MapBudgetToClarificationLabelTest.java` (12 tests),
     `CustomerContextStatusProjectionTest.java` (12 tests),
     `AdReferenceProjectionTest.java` (12 tests).
5. **Cumulative diff** (read via `git diff 084dd8b..af44903`).

Per `prompt-artifact-rules.md` §9: dev handoff + cumulative diff are
DEV-GENERATED artefacts the prompt cannot embed verbatim — refer by
path. Everything else (kernel, scope, output template) is embedded
below; do NOT navigate elsewhere for review evidence.

## Embedded sub-sprint context

### Class (per `iteration_governance.md` §3.2)

- **R1.a** → `prompt_projection` (projects existing
  `IntakeFieldsRegistry` contract as schema declaration + per-UC
  required-fields hint).
- **R2.a** → `infra` + `skill_state` (live-path wiring of existing
  dead-code `BudgetChecker.maxClarificationRounds(=2)` counter +
  budget projection + phase-aware escalation_reason re-map).
- **R4.a** → `prompt_projection` (surfaces already-observed runtime
  state — `form_context.email`/`ad_id` + `lookup_listing_or_ad` tool
  result — as structured boolean/enum slot; zero content matching).

§7 stanza requirement: **REQUIRED** (R1.a + R4.a are semantic-touching).
Tier-0 invariant: NONE added. Semantic hardcode: NONE introduced (per
dev claim — verify per Q1 / Q3 / Q5 / Q6 below).

### Goal

Eliminate two LIVE-path runtime wiring defects (R1 schema gap, R2
dead-code clarification counter + mislabel) + surface the ad-context
premise signal (R4) on the post-M-Auto-5 baseline
`m-auto-5-baseline-20260604-simfixed-stalledfix`. NO bot semantic /
yaml / CaseSpec / eval / simulator / scoring / autoloop edit. The R4
side ships the runtime signal only; the skill-yaml side that uses the
signal (OBS-S1) is deferred to autoloop AFTER M-Auto-6 close (so the
sub-sprint stays runtime-only).

### Scope summary (7 steps shipped; full detail in handoff §1)

- **#1 R1.a** schema declares optional `intake_fields` object on
  `request_handover` (no per-UC matrix; absent from `required[]`).
- **#2 R1.a** `required_intake_fields_for_active_uc` projection iterates
  `IntakeFieldsRegistry.requiredFieldsFor(activeUc)` (single source of
  truth); OMITTED for null / non-intake UC (NOT empty list).
- **#3 R2.a** counter `+1` on `AgentRunLoopImpl` no-tool-calls branch
  iff DISCOVER + no UC commit + non-empty bot reply (`action.getUserMessage()`
  captured as `userMsg`; explicit field-confusion regression guard).
- **#4 R2.a** `budgets.clarification` projection (DISCOVER-only soft signal).
- **#5 R2.a** `mapBudgetToEscalationReason(bucket, currentPhase, lastAction)`
  3-arg overload re-maps `max-repeated-same-action` →
  `clarification_budget_exhausted` ONLY when phase==DISCOVER AND
  lastAction is "answer"/"clarify" (a free-text key). Single-arg
  overload UNCHANGED. NO new enum value.
- **#6 R4.a** `customer_context_status` enum top-level projection;
  priority-ordered (`missing_email` > `missing_ad_id` > `lookup_failed`
  > `lookup_skipped` > `loaded` — first match wins; load-bearing per
  anti-误杀 #10).
- **#7 R4.a** `ad_reference` struct: `form_ad_id` (literal or null) +
  `listing_lookup` 4-value enum (`ok` / `missing` / `failed` /
  `skipped` — `missing` distinct from `skipped` per anti-误杀 #11).

### Pre-fix scope audit outcome for R2.a #5 (dev confirmed)

**Outcome (b) — phase-guarded.** `max-repeated-same-action` is NOT
DISCOVER-only. The session-level counter `BotSession.repeatedActionCount`
is incremented by `ControlKernel.trackRepeatedAction` at **three**
non-phase-scoped sites:
- `:467` — live path, key = `deriveRunResultKey(runResult)`
  (free-text no-tool yields `"answer"`).
- `:651` and `:676` — legacy `PhaseEvaluator` path, key =
  `deriveRepetitionKey(action)` (free-text clarification yields
  `"clarify"`).

So a blanket re-map would mislabel genuine non-DISCOVER repeated-TOOL-call
budgets. The dev's 3-arg overload re-maps ONLY when phase==DISCOVER AND
lastAction is free-text — exactly the c9 surface, with anti-误杀
invariant #12 preserved.

### Anti-误杀 invariants (the sub-sprint's HARD floor)

1. Anchor uc_g_gdpr / uc_h_appeal / uc_i_payment / uc_j_safety at
   0.000 stable UNCHANGED.
2. Shadow cs38s* at 0.000 stable UNCHANGED.
3. `SkillGuardrailDispatcher` reject-logic UNCHANGED.
4. `IntakeFieldsRegistry.java:53-67` field-definition contents UNCHANGED.
5. R2.a counter cardinality-only; no content/similarity detection.
6. NO new `escalation_reason` enum values.
7. NO yaml / skill procedure edit.
8. R4.a STRUCTURAL ONLY — derived from runtime state, not user message
   content.
9. R4.a does NOT change UC routing / FAQ-grounded resolve gate /
   escalation posture.
10. R4.a `customer_context_status` priority order is LOAD-BEARING.
11. R4.a `ad_reference.listing_lookup` distinguishes `missing`
    (ran-but-no-result) from `skipped` (never-ran).
12. R2.a `max-repeated-same-action` re-map MUST be phase-aware (which
    it is per #5 above).

### Test + baseline claims (verify)

- Java `mvn -o test`: **1297 / 1 / 0 / 2** = baseline 1244/1/0/2 + 53
  new tests; the 1 failure = inherited
  `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`
  (Sprint 6 anchor-tag assertion, OQ-S41.5; verified pre-existing per
  handoff §0).
- eval pytest **553 unchanged** (zero `eval_interactive/**` files
  touched).
- autoloop pytest **324 unchanged** (zero `autoloop/**` files touched).

### Commit split (4 commits — deviation from the 6-commit
recommendation in the dev prompt)

- `a873d18` — R1.a #1 + #2 + R2.a #4 + R4.a #6 + #7 (whole-file
  `ContextProjectionBuilder.java` + 3 new test classes for projection
  changes).
- `840a5e2` — R2.a #3 counter wiring (`AgentRunLoopImpl.java` +
  `DiscoverClarificationCounterTest`).
- `247da11` — R2.a #5 phase-aware mapping (`ControlKernel.java` +
  `MapBudgetToClarificationLabelTest`).
- `af44903` — dev handoff.

Dev's stated rationale: whole-file `ContextProjectionBuilder` for
surgical revert of all projection changes; runtime-behaviour items
isolated. **Assess whether this split is acceptable for
reviewability** (see Focus Point E below).

## Embedded §4.1 Nine-Question Anti-Hardcode Kernel

```text
You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
docs/current/iteration_governance.md §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the PR
is purely one of those, return `approve` with a one-line note naming
the exemption.

For every other PR, walk these nine questions in order. For each
"yes" or each concern, paste the diff snippet and the reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC
   selection, risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in docs/runtime_freeze_and_risk_policy.md
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what docs/current/iteration_governance.md §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate generalization
  coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in docs/current/iteration_governance.md §3 that the fix should
target.
```

## Specific focus points (review-side asks — human direction)

In addition to the nine-question kernel above, the human + deliver-agent
have flagged these axes specifically. Each must be answered explicitly in
your write-up.

### A. R1.a + R4.a — `prompt_projection` purity

Verify these projection-surface changes are **purely structural projection
of already-existing runtime state**, NOT semantic hardcodes:

- **R1.a #1**: the `intake_fields` schema property is an OPTIONAL `object`
  with no per-UC properties enumerated — it does NOT encode any per-UC
  decision rule. The semantic of "which fields a UC needs" lives entirely
  in `IntakeFieldsRegistry` (already-existing, UNCHANGED). Verify this.
- **R1.a #2**: the `required_intake_fields_for_active_uc` projection
  iterates `IntakeFieldsRegistry.requiredFieldsFor(activeUc)`. No
  per-UC list is replicated in `ContextProjectionBuilder`. The decision
  of WHEN to project (intake UCs only via `isIntakeUseCase`) is the
  registry's existing classification, not new. Verify this. Also
  confirm: omitting for null/non-intake UC (not emitting an empty
  list) — is this a hidden "if-else by UC" or genuinely a
  structural shape distinction?
- **R4.a #6 / #7**: `customer_context_status` enum + `ad_reference`
  struct are computed from runtime state ONLY (form_context.email +
  ad_id + lookup tool result). Verify there is ZERO content matching
  of user messages anywhere in `addAdContextPremiseProjection`,
  `computeCustomerContextStatus`, `deriveListingLookupState`, or
  `listingLookupToken`.
- **R4.a priority order**: missing_email > missing_ad_id > lookup_failed
  > lookup_skipped > loaded. Verify this is the priority encoded in
  `computeCustomerContextStatus` at `ContextProjectionBuilder.java:1383-1401`
  (first match wins; `loaded` is the fallback). Confirm the c7 scenario
  (loaded customer context + null `form_context.ad_id`) emits
  `missing_ad_id` (NOT `loaded`).
- **R4.a `listing_lookup` 4-value**: `missing` (ran-but-no-result) is
  DISTINCT from `skipped` (never-ran). Verify at
  `deriveListingLookupState:1364-1374`.

### B. R2.a — `infra` + `skill_state` repair, not a semantic surface

Verify R2.a is purely:

- **#3 counter wiring**: the increment site at
  `AgentRunLoopImpl.java:442-458` uses STRUCTURAL criteria only
  (phase, tool-call count, UC commit, bot-reply emptiness). No
  content / NLP / similarity check. The bot-reply field used is
  `userMsg = action.getUserMessage()` (line 410) — the LLM's
  outgoing message field. Confirm this is NOT the customer's
  incoming `userMessage` run() parameter (line 162-165). The
  predicate at `:1207-1215` takes only the bot reply (regression
  guard against field confusion).
- **#4 budgets projection**: structural cardinality only; emitted only
  in DISCOVER phase.
- **#5 phase-aware mapping**: the 3-arg overload at
  `ControlKernel.java:763-771` re-maps `max-repeated-same-action` →
  `clarification_budget_exhausted` ONLY when phase==DISCOVER AND
  lastAction is `"answer"` or `"clarify"` (free-text key, never a
  tool name). Single-arg overload at `:733-749` UNCHANGED. NO new
  `escalation_reason` enum value introduced.
- The pre-fix scope audit outcome (b) — phase-guarded — is the
  correct call given `trackRepeatedAction` fires at three
  non-phase-scoped sites. Confirm this guard correctly prevents
  RESOLVE/INTAKE repeated-TOOL-call budgets from being mislabeled
  as clarification.

R2.a should be exempt from §7 / Q1 as pure `infra` wiring + structural
state-tracking. But the bundle is reviewed jointly with R1.a + R4.a
because the bundle ships in one sub-sprint.

### C. Phase-aware mapping guard correctness

Independently verify the phase guard at `ControlKernel.java:763-771`:

- All three conditions are AND-ed (bucket + phase + lastAction).
- `isFreeTextActionKey` (`:779-781`) returns true only for `"answer"`
  / `"clarify"` (no tool name).
- All other paths delegate unchanged to the single-arg overload via
  `return mapBudgetToEscalationReason(bucket);` at line 770.
- The call site at `:305-313` is the ONLY call site that uses the
  3-arg overload (verify via cumulative grep). No accidental
  silent activation elsewhere.

If the guard is correct, this is the canonical example of the
"projecting structural state + reusing existing enum" pattern that
governance §1.5 + §1.7 prefer over enum widening.

### D. Anti-误杀 floor preservation (no eval-spec masking)

Verify that NONE of the changes could cause:

- A persistent high-risk failure (anchor uc_g/h/i/j_safety, shadow
  cs38s*) to silently start passing on the post-fix re-bless.
- A genuine bot mistake to be eval-side masked (§5.4 violation).
- A validator-side weakening (`SkillGuardrailDispatcher` start
  passing partial intake).

The 53 new tests should structurally guarantee these (especially
`IntakeFieldsProjectionTest.intakeValidatorRejectBehaviour_unchangedWhenIntakeFieldsAbsent`
or equivalent), but confirm the design intent matches code.

### E. 4-commit split — acceptability

The dev prompt recommended a 6-commit split (one commit per scope
item #1, #3, #4, #5, R4 #6+#7, handoff). Dev shipped a 4-commit
split: ContextProjectionBuilder whole-file (#1+#2+#4+#6+#7) +
AgentRunLoopImpl (#3) + ControlKernel (#5) + handoff.

Assess: does the consolidation harm reviewability or revert
granularity?

- The ContextProjectionBuilder commit `a873d18` contains 5 logically
  distinct projection additions; if any one is later found broken,
  the surgical revert path is to revert + re-apply the other 4. Is
  the bundle reviewable as a single commit, or does the consolidation
  hide an interaction effect?
- The runtime-behaviour commits (`840a5e2` #3 counter, `247da11` #5
  mapping) are isolated as the prompt requested — that's good.
- Both R1+R4 projection items + R2 budget projection share `objectMapper`
  + `ContextProjectionBuilder` plumbing; consolidating them avoids
  fragile inter-commit dependencies.

Issue a verdict on the split:
- ACCEPTABLE (no flag) — and proceed.
- ACCEPTABLE-WITH-NOTE — proceed but recommend stricter split in
  future sub-sprints.
- BLOCKING — request the dev split before merge.

### F. Human re-bless gate decision

Given the verdict on A through E and the §4.1 kernel walk, decide:

- **CAN proceed to HUMAN-launched real-LLM re-bless** (with output
  dir `eval_interactive/results/m-auto-6-baseline-r1r2r4-20260605`,
  backend rebuilt, multi-suite, anti-误杀 abort floor in place).
- **CANNOT proceed yet** — surface specific blockers in §4 below;
  fix-iteration required before re-bless.

If you CAN'T proceed, blocking items must each have:
- P0/P1 priority.
- Minimal required fix scope (file:line ranges if applicable).
- Re-review trigger (when to re-dispatch this prompt).

## Output format

Append your review to **`docs/codex-findings.md`** (currently a §4.2
scaffold). Use this exact structure:

```markdown
## Sprint Review Decision
decision: <pass | fix_required | out_of_scope_review>
blocking_count: <number>
final_verdict: <APPROVE_S_AUTO_23 | APPROVE_S_AUTO_23_WITH_OBSERVATIONS | BLOCK_S_AUTO_23>
human_rebless_gate: <PROCEED | HOLD>
summary: <one paragraph>

## §1 Per-Change Verdicts

**R1.a #1 (schema)**: approve / reject + rationale + file:line evidence.
**R1.a #2 (per-UC required projection)**: approve / reject + rationale + file:line evidence.
**R2.a #3 (counter wiring)**: approve / reject + rationale + file:line evidence.
**R2.a #4 (budgets projection)**: approve / reject + rationale + file:line evidence.
**R2.a #5 (phase-aware mapping)**: approve / reject + rationale + file:line evidence.
**R4.a #6 (customer_context_status)**: approve / reject + rationale + file:line evidence.
**R4.a #7 (ad_reference)**: approve / reject + rationale + file:line evidence.

## §2 Nine-Question Kernel Walkthrough

Walk Q1–Q9 against the cumulative diff. For each "yes" or concern,
paste the diff snippet and reasoning. Verdict per §4.1.

## §3 Specific-Focus-Point Verdicts

**A. R1/R4 prompt_projection purity**: <verified | violated> + evidence.
**B. R2 infra/skill_state repair**: <verified | violated> + evidence.
**C. Phase-aware mapping guard correctness**: <correct | flawed> + evidence.
**D. Anti-误杀 floor preservation**: <preserved | at-risk> + evidence.
**E. 4-commit split acceptability**: <ACCEPTABLE | ACCEPTABLE-WITH-NOTE | BLOCKING> + rationale.
**F. Human re-bless gate**: <PROCEED | HOLD> + condition.

## §4 Blocking Findings

<None | P0/P1 list with minimal required fix scope + re-review trigger>

## §5 Non-Blocking Observations

<numbered observations that do not block re-bless but should route to
the dev handoff §1 / action_bank / M-Auto-6 close>
```

## Constraints

1. **Do NOT edit any code.** Read-only review.
2. **Do NOT re-judge bad-case semantic correctness via fresh LLM
   judgement.** Use code / artefacts / traces / results / stated
   evidence only.
3. **Do NOT propose code fixes beyond naming the §3 layer.** If a
   blocker exists, name the layer (`prompt_projection` /
   `skill_state` / `semantic_planner` / `infra` / `java_guard` /
   `eval_spec` / `product_policy` / `judge_calibration` /
   `human_review_required`) per `iteration_governance.md` §3.
4. **Do NOT promote OBS-* observations (OBS-S1/S2/S3/S4/S5) to
   blockers** for this sub-sprint. They are explicitly deferred to
   autoloop AFTER M-Auto-6 close.
5. **Do NOT block on R4 semantic effects.** R4 ships the runtime
   signal only; OBS-S1 (UC-A verify-ad procedure step) is autoloop
   work. The R4 close gate is wiring observability (≥3 no-ad_id +
   ≥3 with-ad_id projection samples), NOT bot-behaviour shift.
6. **Do NOT request the dev to launch the re-bless.** The re-bless
   is HUMAN-launched per §6 of the dev handoff (anti-误杀 protocol).
7. **Do NOT widen scope.** This is Sub-sprint A (R1+R2+R4) only.
   R3 (UI) is Sub-sprint B, drafted as planning context at
   `compact/sprint-079-dev-prompt.md` and replaces sprint_objective
   at A close. Do not block A on B's status.
8. If you suspect the inherited 1 Java test failure
   (`SystemPromptUserRequestedTiebreakerTest`) was actually caused
   by this sub-sprint, say so explicitly with evidence; otherwise
   accept dev's pre-existing-on-clean-main verification (OQ-S41.5).
