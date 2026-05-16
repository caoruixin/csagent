---
title: Sprint 34 handoff — Intake field prefill UC-G/H/I/J (M1 sub-sprint 2)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-16
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 34 is the second sub-sprint of Milestone M1 (DISCOVER + Intake)
  per `docs/milestone_objective.md`. Layer `skill_state` per
  `docs/current/iteration_governance.md` §3.2 Q4. Codex sprint-close
  review deferred to M1 milestone-shared close per §4.3 default.
  Closure verdict (§12) is left for the M1 milestone-shared decision;
  this archive captures only the dev-session evidence.
---

# Sprint 34 handoff — Intake field prefill UC-G/H/I/J

## 1. Context pack

- **Sub-sprint:** Sprint 34, second sub-sprint of Milestone M1 (DISCOVER + Intake) per `docs/milestone_objective.md`.
- **Layer:** `skill_state` per `docs/current/iteration_governance.md` §3.2 Q4 (multi-tool / multi-turn flow state was being lost on UC-G/H/I/J intake paths: the user supplied `ad_id` and `email` in the form, the LLM committed an intake-path UC, but `IntakeFieldExtractor` only seeded UC-K's two fields — leaving the next-turn `intake_state.fields_remaining` to report all UC-G/H/J fields missing, so the LLM re-asked the user for fields the form already collected).
- **Bad case in scope:** D2 dimension of `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (intake prefill gap). Per the §2 reframing note in `docs/sprint_objective.md`, Sprint 33 closed Alice's D1 dimension on closure-criterion (a) with a single real-LLM run; Sprint 34 ships D2 coverage for the broader UC-G/H/I/J intake surface (Alice is one trace of many).
- **Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. No per-sub-sprint Codex trigger fired (no Tier-0 candidate, no §1.7 red line, no hard-fence violation, no fix-iteration on a prior sub-sprint).
- **Tier model:** the extractor extension and per-UC helpers are `current-runtime` contracts; the regression tests are normal repo test code; the handoff (this file) is `sprint-archive`. No `foundational` doc edited.

Authoritative sources consulted at session start:

- `docs/milestone_objective.md` (M1 north star: §3 Sprint 34 row, §6 hard fences).
- `docs/sprint_objective.md` (Sprint 34 contract: §2 reframing, §5 file table, §6 hard fences, §8 §7 stanza, §9 success metrics, §10 stop conditions).
- `docs/sprints/sprint-033-handoff.md` (Sprint 33 dev evidence; Alice closure-criterion (a) PASS at HEAD on a single real-LLM rerun).
- `docs/current/iteration_governance.md` §1.3 / §1.4 / §1.7 / §3.2 Q4 / §4.1 / §4.3 / §5.5 / §5.6 / §7 / §8.
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldExtractor.java` (the file extended).
- `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` (alias map + required-fields map; consumed, not edited).
- `server/src/main/java/com/gumtree/csagent/service/runtime/FormContextIngestionService.java` (form-context JSON shape: source-of-truth for `first_name` / `email` / `topic_subject` / `description` / optional `ad_id`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` (lines 169 invocation site + 557-577 `mergePartialIntakeFromContext` plumbing; 628-651 `shouldRejectIncompleteIntakeHandover` guard).
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` (lines 346-377 `intake_state` slot; consumed unchanged).
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint71PartialIntakePersistenceTest.java` (UC-K reference pattern; regression guard kept unchanged).
- `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest.java` (Sprint 33 parameterised-invariance test shape).
- `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (target case + D2 dimension `bad_case_metadata.related_dimensions`).
- `eval_interactive/case_specs/bad_cases/_manifest.md` (bad-case suite convention).

## 2. Sub-sprint-objective recap

Extend `IntakeFieldExtractor` from UC-K-only to also handle UC-G / UC-H / UC-I / UC-J, consuming the form-context aliases already defined in `IntakeFieldsRegistry.java:78-104`. When intake IS the correct path, the bot does NOT re-ask for fields the form context already supplied (`form_context.ad_id` → `ad_id_or_listing_url`, `form_context.email` → `registered_email`, `form_context.description` → canonical `description` for UC-J). UC-I is wired as a deliberate no-op today (form-context shape carries no transaction-reference source; structural-symmetry branch only).

Hard fences honoured: no `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` / `PhaseEvaluator` edits; no `IntakeFieldsRegistry` schema change; no `INTAKE_UCS` widening; no `escalation_reason` enum touch; no Tier-0 invariant; no regex / keyword / per-UC matrix on user content; no edit to UC-K helpers (`extractUcKFields`, `capturePlatform`, `captureRegressionText`, `PLATFORM_TOKEN_PATTERN`, `REGRESSION_MARKER_PATTERN`); no edit to existing `Sprint71PartialIntakePersistenceTest.java`; no edit to `ContextProjectionBuilder.java`; no edit to `FormContextIngestionService.java`; no edit to `system_prompt.txt`; no edit to `AgentRunLoopImpl.java` (the existing `handlesUc`-gated merge plumbing fires for the four new UCs once `handlesUc` returns true); no edit to deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md`); no edit to `eval_interactive/eval_interactive/`; no edit to existing case families; no edit to bad-case suite; no edit to sprint archives.

## 3. Premise re-verification (§4 spot-check)

All 7 §4 premises verified at HEAD post-Sprint-33-close (commit `8a22aa6 sprint 33: DISCOVER UC-A/FP/H soft-signal + classification guidance (M1 sub-sprint 1)`):

1. **`IntakeFieldExtractor.java` UC scope at HEAD** — confirmed `extractFromTurn` (lines 85-99 pre-edit) carries a single `if ("UC-K".equals(uc))` branch; `handlesUc` at lines 220-222 returns true only for UC-K. Verified by Read of the full file.
2. **`IntakeFieldsRegistry.java:53-67` required-fields map** — covers UC-G `[registered_email, data_request_type]`, UC-H `[ad_id_or_listing_url, registered_email, stated_reason_or_context]`, UC-I `[transaction_reference, dispute_reason]`, UC-J `[report_target, report_type, description]`, UC-K `[platform, repro_steps_or_error_message]`. Verified verbatim. Sprint 34 consumes; does NOT edit.
3. **`IntakeFieldsRegistry.java:78-104` alias map** — UC-H has `ad_id → ad_id_or_listing_url`, `listing_url → ad_id_or_listing_url`, `email → registered_email`, `appeal_reason → stated_reason_or_context`, `reason → stated_reason_or_context`; UC-J `target → report_target`, `report_about → report_target`, `type → report_type`, `issue_type → report_type`; UC-I `transaction_id → transaction_reference`, `payment_reference → transaction_reference`, `dispute_description → dispute_reason`; UC-G `request_type → data_request_type`, `data_action → data_request_type`. Verified. Sprint 34 consumes; does NOT edit.
4. **`FormContextIngestionService.java:48-85` form_context JSON shape** — at session start the service writes `first_name`, `email`, `topic_subject`, `description`, optionally `ad_id`. Verified by Read of lines 48-85. These are the SOURCE-OF-TRUTH fields the extractor reads.
5. **`AgentRunLoopImpl.java:557-577` `mergePartialIntakeFromContext` plumbing** — line 560 gates on `IntakeFieldsRegistry.isIntakeUseCase(uc)` (all 5 intake UCs pass); line 561 then gates on `IntakeFieldExtractor.handlesUc(uc)`. Once `handlesUc` returns true for UC-G/H/I/J, the existing plumbing fires automatically. Zero edit to this file (verified post-edit via the new integration test which exercises the path end-to-end).
6. **`Sprint71PartialIntakePersistenceTest.java` UC-K reference test** — 14 tests at HEAD, all pass. Re-run post-Sprint-34 changes: 14/14 still pass. UC-K behaviour byte-identical.
7. **Alice bad case D2 dimension** — `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` `bad_case_metadata.related_dimensions` includes "D2 (intake prefill gap)". The CaseSpec `form_context` carries `ad_id: AD-2001` and `email: alice.removed@example.com` — these are the fields that today would be re-asked if the bot committed UC-H. Verified by Read of lines 1-60.

No premise drift surfaced. Working-tree carries the pre-existing Sprint 24-era mod on `system_prompt.txt` causing the inherited `SystemPromptUserRequestedTiebreakerTest` failure that has been the documented baseline since Sprint 24. Sprint 34 does NOT touch `system_prompt.txt`; the staging discipline excludes that mod from this commit (see §9).

## 4. Implementation walkthrough

### 4.1 `IntakeFieldExtractor.java` — extend `extractFromTurn` dispatch + four per-UC helpers + new field reader

**Insertion point 1 (lines 100-122 post-edit)** — extend `extractFromTurn` dispatch:

The existing `if ("UC-K".equals(uc))` branch is preserved unchanged. Four `else if` branches are added, in this order: UC-H, UC-G, UC-J, UC-I. Each dispatches to a new private static helper. The ordering is deliberate (most-likely-to-fire first to keep the hot path short on the Alice surface), but ordering is not semantically load-bearing since the branches are mutually exclusive on `uc`.

**Insertion point 2 (lines 153-232 post-edit)** — four new per-UC helpers:

- `extractUcHFields(out, formContextJson, objectMapper)` — reads `form_context.ad_id` → canonical `ad_id_or_listing_url`; reads `form_context.email` → canonical `registered_email`. The third UC-H required field (`stated_reason_or_context`) has no form-context source; the LLM owns capturing it via `request_handover.arguments.intake_fields` (`AgentRunLoopImpl.persistInlineIntakeFields`).
- `extractUcGFields(out, formContextJson, objectMapper)` — reads `form_context.email` → canonical `registered_email`. The second UC-G required field (`data_request_type`) has no form-context source.
- `extractUcJFields(out, formContextJson, objectMapper)` — reads `form_context.description` → canonical `description` (canonical name and form-context key coincide). The other two UC-J required fields (`report_target`, `report_type`) have no form-context source.
- `extractUcIFields(out, formContextJson, objectMapper)` — deliberate no-op. The canonical UC-I required fields (`transaction_reference`, `dispute_reason`) have no source-of-truth in `FormContextIngestionService`'s form-context shape (`first_name` / `email` / `topic_subject` / `description` / optional `ad_id`). The branch exists for structural symmetry so a future sprint that adds a transaction-reference form field can extend the method body without re-touching `extractFromTurn`. Surfaced as Sprint 34 OQ1 in §7 below.

Each helper signature is uniform (`Map<String, String> out`, `String formContextJson`, `ObjectMapper objectMapper`) so the dispatch is regular. None of the four reads `userMessage` — the user-text mining surface is UC-K-only by design per Sprint 34 §10 stop condition #4.

**Insertion point 3 (lines 289-316 post-edit)** — new `extractFormContextField(formContextJson, fieldName, objectMapper)` helper:

Generalises the existing `extractFormDescription(formContextJson, objectMapper)` helper to accept an arbitrary field name. Reads the JSON root, returns the trimmed string value, or `null` on missing / blank / unparseable / non-object / null-field / blank-field-name. Tolerant — never throws. The four per-UC helpers call this rather than each re-implementing the parse + null-handling pattern.

**Insertion point 4 (lines 348-361 post-edit)** — extend `handlesUc`:

```java
public static boolean handlesUc(String uc) {
    return "UC-G".equals(uc) || "UC-H".equals(uc)
            || "UC-I".equals(uc) || "UC-J".equals(uc)
            || "UC-K".equals(uc);
}
```

UC-I returns `true` despite the helper being a no-op so the dispatch surface covers every `IntakeFieldsRegistry` intake UC. The `mergeForUc` helper short-circuits on `incoming.isEmpty()` (line 331-333) so UC-I no-op produces no write — verified end-to-end by the V4 variant of the integration test.

**Class Javadoc (lines 12-51 post-edit)** — updated UC-scope section from "UC-K-only" to enumerate the five intake UCs (UC-G/H/I/J/K) with their per-UC source semantics. UC-I no-op is named explicitly with a forward-pointer to the structural-symmetry rationale.

### 4.2 `AgentRunLoopImpl.java` — zero edits (predicted by §5 of `sprint_objective.md`)

The `mergePartialIntakeFromContext` merge gate at lines 557-577 is unchanged. Line 560 (`if (!IntakeFieldsRegistry.isIntakeUseCase(uc)) return;`) is the FAQ-path negative-control gate; line 561 (`if (!IntakeFieldExtractor.handlesUc(uc)) return;`) is the extractor capability gate. Once `handlesUc` returns true for UC-G/H/I/J, the existing plumbing fires:

1. Parse `session.intakeFields` JSONB → existing map.
2. Call `IntakeFieldExtractor.mergeForUc(uc, existing, userMessage, formContext, objectMapper)`.
3. If `mergeForUc` returned the same reference or an equal map, no-op write. Otherwise serialize the merged map back to `session.intakeFields`.

The integration test (`AgentRunLoopUcGHIJIntakePrefillIntegrationTest`) exercises this end-to-end for UC-H/G/J/I/K-reference/UC-A-negative-control across 6 parameterised variants × 5 invariance bars; all 6 PASS without any edit to `AgentRunLoopImpl.java`.

### 4.3 `ContextProjectionBuilder.java` — zero edits (per §6 hard fence #7)

The `intake_state` projection slot at lines 346-377 already consumes `IntakeFieldsRegistry.parseCollectedFields` + `IntakeFieldsRegistry.fieldsRemaining`. Once Sprint 34's extractor writes the new canonical names to `session.intakeFields`, the projection automatically surfaces them in `fields_collected` and drops them from `fields_remaining`. Zero projection-builder change required; verified by the projection-flip test groups in `Sprint34IntakePrefillProjectionAndGuardTest`.

### 4.4 Three new test files (§5 file table)

- **`Sprint34IntakePrefillExtractorTest.java`** — 33 extractor-unit-contract tests across 7 groups: UC-H (9 tests), UC-G (4), UC-J (4), UC-I (3), UC-K regression (3), handlesUc surface (2), `extractFormContextField` direct unit tests (8). Closure shape mirrors `Sprint71PartialIntakePersistenceTest`. All 33 PASS.
- **`Sprint34IntakePrefillProjectionAndGuardTest.java`** — 12 projection-flip + intake-complete-guard tests across 6 groups: UC-H projection-flip + guard-passes-after-stated-reason + guard-still-rejects + user_requested-escape (4), UC-G projection + guard (2), UC-J projection + guard (2), UC-I no-op projection + guard-still-enforced (2), UC-K regression (1), UC-A FAQ-path negative control (1). All 12 PASS.
- **`AgentRunLoopUcGHIJIntakePrefillIntegrationTest.java`** — 6 parameterised variants × 5 invariance bars per Sprint 33 fix-iteration #2 T8 / Sprint 33 disambiguation-non-enforcement precedent. Variants: V1 UC-H (ad_id + email), V2 UC-G (email only), V3 UC-J (description only), V4 UC-I (no-op), V5 UC-K (regression reference), V6 UC-A (FAQ-path negative control — no `intake_state` slot at all). All 6 PASS.

## 5. Bad-case suite Alice rerun (recommended, informational)

**Not run in this dev session.** Per Sprint 34 §1 reframing note + §7 of `docs/sprint_objective.md`: Sprint 33 closed Alice on closure-criterion (a) via UC-A FAQ-path commitment; the Java unit + integration tests are the load-bearing evidence for Sprint 34, not a single-trace Alice rerun. If the M1 milestone close rerun shows Alice flipping to UC-H (LLM variance), Sprint 34's prefill plumbing will fire and the canonical names will appear under `intake_state.fields_collected` — proved end-to-end by the Sprint 34 integration test V1 variant against the same form-context shape as Alice's CaseSpec.

Sprint 34's primary evidence layer per §9 of `docs/sprint_objective.md`:

- Java unit + integration tests (deterministic; load-bearing for Sprint 34 close).
- M1 milestone-shared bad-case suite rerun at M1 close (broader evidence pool; load-bearing for M1 close decision, not Sprint 34 close).

**Why no Alice rerun in Sprint 34 dev session.** A single-trace Alice rerun adds informational value only if it flips to UC-H — and even then, the V1 integration test variant already proves the prefill plumbing on a byte-identical form-context shape. The deliver-agent may opt to run the bad-case suite at sub-sprint close as a sanity check; Sprint 34 dev did not, to keep evidence layers clean (per `feedback_mocked_llm_cannot_prove_prompt_causal_change.md` discipline: Java-deterministic plumbing is proven by Java tests; LLM-behaviour validation belongs to the M1 milestone-shared rerun).

## 6. Generalization coverage table (per §8 stanza)

| coverage class | shape | evidence in Sprint 34 |
|---|---|---|
| **Target** | UC-G/H/I/J intake-path sessions whose form_context supplies one or more canonical-alias fields, where today the bot re-asks for the prefilled fields | 6/6 integration variants PASS the 5 invariance bars; 12/12 projection-flip + guard tests PASS; 33/33 extractor unit tests PASS |
| **Neighbor (UC-K regression)** | the existing UC-K extraction path stays byte-identical | `Sprint71PartialIntakePersistenceTest` 14/14 unchanged + green; Sprint34 extractor test `ucK_stillCapturesPlatformFromUserReply` + `ucK_stillCapturesReproStepsFromCs066FormDescription` + `ucK_doesNotLeakIntoOtherUcBranches` + integration variant V5 PASS |
| **Negative (FAQ-path & no-op)** | UC-A FAQ-path session must not receive any partial-intake write; UC-I helper must remain a deliberate no-op even with full form_context | `Sprint34IntakePrefillProjectionAndGuardTest.faqPathUcs_doNotReceivePartialIntakeMerge` PASS; `ucI_projection_isUnchangedAfterMerge_perDeliberateNoOp` + `ucI_intakeCompleteGuard_stillEnforced_perDeliberateNoOp` PASS; integration variant V4 UC-I no-op + V6 UC-A FAQ-path negative control PASS |
| **Shadow** | held-out LLM-behaviour validation across the bad-case suite (Alice + future UC-G/I/J synthetic traces if added) | DEFERRED to M1 milestone-shared close per `iteration_governance.md` §5.6 + §4.3 default. Sprint 34 ships the Java regression coverage; LLM-behaviour validation is M1-scoped |

Per M1 hard fence #3 (no edits to existing case families), Sprint 34 does NOT author neighbor/negative/shadow case families in `eval_interactive/case_specs/case_families/`. The Java integration-test parameterization (6 variants × 5 invariance bars) is the right shape for Sprint 34's regression coverage; new case families are explicitly out of M1 sub-sprint 2 scope per `sprint_objective.md` §10 condition 9.

## 7. Open questions for M1 (deliver-agent + human)

**OQ1 — Should `FormContextIngestionService` be extended to capture a `transaction_reference` / `dispute_reason` form field, enabling UC-I prefill?**

Sprint 34's UC-I helper is a deliberate no-op because the form-context shape (`first_name` / `email` / `topic_subject` / `description` / optional `ad_id`) lacks a transaction-reference field. If real-traffic UC-I sessions show the user re-typing a transaction reference that they already supplied in the form (or if the front-end form already captures one in a non-canonical key the JSON does not yet surface), a future sprint should: (a) extend `FormContextIngestionService.ingest(...)` to accept a `transactionReference` parameter, (b) write it into the form-context JSON, (c) extend `extractUcIFields` to read it. Recommendation: defer to M2 once real-traffic UC-I evidence is available; do not invent a form schema change pre-emptively.

**OQ2 — Should `extractUcHFields` (and the others) also read user-typed inline mentions of an ad_id / email?**

Today the per-UC helpers read form-context only. If the user opens an intake session WITHOUT filling the form's ad_id field but mentions an ad-id-shaped token inline on turn 1 ("my ad AD-12345 was unfairly removed"), the extractor does NOT capture it. The LLM owns the inline capture via `request_handover.arguments.intake_fields` (`persistInlineIntakeFields` path). Adding a regex on user content for ad-id-shape would be a §1.7-adjacent semantic hardcode (per Sprint 34 §10 stop condition #4 + §8 anti-hardcode self-walk). Recommendation: keep the current discipline (form-context plumbing; LLM owns inline capture). If real-traffic shows the LLM is failing to capture inline mentions, the right fix is the prompt-projection or semantic_planner layer (e.g., teach the LLM to scan turn 1 for canonical-field mentions), not Java regex.

**OQ3 — UC-K `userMessage` mining is asymmetric to UC-G/H/I/J. Should we reconsider?**

UC-K alone reads `userMessage` for the `platform` token and the `repro_steps_or_error_message` regression marker. UC-G/H/I/J read form-context only. The asymmetry is principled (UC-K's `platform` and `repro_steps` are conversational fields the user supplies in their reply; UC-G/H's `ad_id` and `email` are form-supplied fields where inline mention belongs to the LLM). But the difference is not obvious from the extractor's outside view and may surprise future contributors. Recommendation: clarify in the class Javadoc at the next docs-only fold-back; no code change.

**OQ4 — `Sprint71PartialIntakePersistenceTest.extractor_isNoOpForNonUcK` comment is now historically inaccurate.**

That test (line 128-135 in HEAD) asserts UC-H + UC-A return empty when `formContextJson=null`. Sprint 34 preserves this behaviour (UC-H with null form-context returns empty because both `extractFormContextField` reads return null). BUT the test's comment ("UC-H, UC-J, UC-G, UC-I are not yet handled by the extractor — fields still arrive via request_handover.arguments.intake_fields") is now historically inaccurate: post-Sprint-34, UC-G/H/J ARE handled by the extractor (UC-I is a deliberate no-op). The §6 hard fence #6 forbids editing this test file. Recommendation: docs/comment fold-back at the next sprint that touches the file (e.g., the Sprint 7-era anchor reference can stay; only the parenthetical scope-comment is stale). Out of Sprint 34 scope.

**OQ5 — Sprint-objective §5 file table predicted "(if needed): edit AgentRunLoopImpl.java merge-gate plumbing" but no edit was required.**

Pre-implementation prediction in `sprint_objective.md` §5 row 2 was "EDIT (if needed)"; the dev session confirmed zero edit was needed because the existing `handlesUc(uc)`-gated plumbing fires for the four new UCs once `handlesUc` returns true. Observation, not a concern. Recommendation: leave the prediction in the sprint contract as historically interesting; M1 close may note this as a planning-accuracy data point.

## 8. Anti-hardcode self-walk (§4.1 nine-question kernel)

Walked before commit; expected verdict at M1 milestone-shared Codex close: **`approve`**.

1. **Does the PR add a keyword / regex / if-else / enum / per-UC matrix for a semantic decision (drift detection, escalation, UC selection, risk classification, follow-up, intake routing)?** No. The four new per-UC branches in `extractFromTurn` are field-mapping plumbing: given `uc` (which the LLM has already classified, before the merge fires), they tell the extractor which form-context KEYS to read for THAT UC. They do NOT make a routing or escalation decision. No regex on user-typed content was added; the existing UC-K `PLATFORM_TOKEN_PATTERN` / `REGRESSION_MARKER_PATTERN` regexes stay UC-K-scoped per hard fence #5.
2. **If yes to (1), justified as protecting a current Tier-0 invariant?** N/A.
3. **Could the same outcome be achieved by projecting a soft signal to the LLM?** No — the work is plumbing, not signal projection. The "decision" being made is "what canonical name should this form-context value land under?" — a mapping question whose answer is the alias map in `IntakeFieldsRegistry.FIELD_ALIASES` (a registry, not a runtime decision). The projection slot (`intake_state.fields_collected`) is the soft signal; Sprint 34 just unlocks four more UCs into it.
4. **Does the change encode visible-eval case text, trace-specific phrasing, or a CaseSpec id?** No. No CaseSpec id, no trace text, no Alice-specific phrasing. The form-context fields read (`ad_id`, `email`, `description`) are the FormContextIngestionService schema, not eval phrasing. Test fixtures use deliberately generic example data (Alice / Bob / Carol / Dave / Eve / Frank / Grace / Ivan / Stephen / `AD-2001` / `AD-9001` etc.).
5. **Does the change move semantic ownership from the LLM to Java?** No. The LLM still owns UC classification at DISCOVER (Sprint 33 plus pre-existing classifier); the LLM still owns inline `intake_fields` capture via `persistInlineIntakeFields`; the LLM still owns the escalation decision when `stated_reason_or_context` cannot be supplied (the `shouldRejectIncompleteIntakeHandover` guard still rejects `intake_complete_for_uc_h` when fields are missing; the LLM can still gracefully escalate with `user_requested` or `ambiguous_intent`, proved by `Sprint34IntakePrefillProjectionAndGuardTest.ucH_intakeCompleteGuard_acceptsUserRequestedEscape`). Sprint 34 expands Runtime's "persistence + trace contract" responsibility per Constitution §1.4; it does not erode §1.3.
6. **Does the change add an if-else block to the prompt instead of principle-level guidance?** N/A. Sprint 34 ships zero prompt change. `system_prompt.txt` untouched (hard fence #9); `PhaseEvaluator.systemInstruction` untouched (hard fence in §6 and §3 non-goals).
7. **Does the change preserve tool schema, capability / permission boundary, PII / safety floor, and grounding floor?** Yes. Tool schema unchanged; `allowedTools` per-UC unchanged; PII surface unchanged (the form-context fields persisted are the same fields `FormContextIngestionService` already stores in `session.formContext`); grounding floor untouched (Sprint 34 is intake plumbing; no FAQ surface).
8. **Does the PR ship generalization eval coverage (target / neighbor / negative / shadow)?** Target + Neighbor + Negative shipped at the Java layer (see §6 table). Shadow deferred to M1 milestone-shared close per §5.6 + §4.3 default — explicit in this archive and in `sprint_objective.md` §9.
9. **If the change is temporary, does it carry a rollback / sunset plan?** Not temporary. The per-UC helpers are permanent plumbing intended to coexist with `persistInlineIntakeFields` (LLM-supplied path) per the `mergeForUc` skip-if-existing rule.

**Expected verdict at M1 milestone-shared Codex close: `approve`.** No semantic hardcode; Constitution §1.3 ownership preserved; generalization coverage shipped at the Java layer (cross-LLM shadow validation at M1 close).

## 9. Files changed

| path | change | lines | new file? | test count |
|---|---|---|---|---|
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldExtractor.java` | extend `extractFromTurn` dispatch with 4 per-UC branches (UC-H/G/J/I) + 4 new per-UC helpers (`extractUcHFields`, `extractUcGFields`, `extractUcJFields`, `extractUcIFields`) + 1 new generalised field reader (`extractFormContextField`) + extend `handlesUc` to return true for UC-G/H/I/J/K + update class Javadoc UC-scope section | +148 / -11 | no | N/A (production) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillExtractorTest.java` | new — extractor-unit-contract tests across UC-H (9) + UC-G (4) + UC-J (4) + UC-I no-op (3) + UC-K regression (3) + handlesUc (2) + `extractFormContextField` direct unit tests (8) | +335 | yes | 33 |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillProjectionAndGuardTest.java` | new — projection-flip + intake-complete-guard tests across UC-H (4) + UC-G (2) + UC-J (2) + UC-I no-op (2) + UC-K regression (1) + UC-A FAQ-path negative control (1) | +279 | yes | 12 |
| `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopUcGHIJIntakePrefillIntegrationTest.java` | new — 6 parameterised variants × 5 invariance bars per Sprint 33 fix-iteration #2 T8 / Sprint 33 disambiguation-non-enforcement precedent | +315 | yes | 6 |
| `docs/sprints/sprint-034-handoff.md` | this archive | +N | yes | N/A (docs) |

Total staged: **1 production file + 3 test files + 1 handoff archive**. Net Java baseline delta: **+51 new tests** (33 extractor + 12 projection-guard + 6 integration). Full server suite: **983 / 1-inherited / 0 / 2**.

**Staging-discipline note:** the working tree carries the pre-existing Sprint 24-era mod to `system_prompt.txt:66` causing the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` failure (documented baseline since Sprint 24; Sprint 33 §9 carries this note forward). Sprint 34 does NOT touch `system_prompt.txt` and does NOT bundle this mod into the commit. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-034-dev-prompt.md`) are NOT staged by the dev session; the human bundles those at deliver-agent-side commit.

## 10. Layer-classification self-walk (per §3)

Walked the §3.2 decision questions:

1. **Q1 (infra)?** No. No session-start crash, timeout, OOM, or transport issue.
2. **Q2 (java_guard / Tier-0 invariant break)?** No. The Constitution's §1.4 Runtime "persistence + trace contract" governs the surface; no Tier-0 invariant in `runtime_freeze_and_risk_policy.md` §1 / §2 is broken or invented. The intake-complete guard at `AgentRunLoopImpl.java:628-651` (which IS Java-guard territory) is consumed unchanged; Sprint 34 adds NO new Java-guard predicate.
3. **Q3 (prompt_projection)?** No. The LLM was choosing validly within available options after Sprint 33; the gap was downstream of classification — multi-turn intake state durability across the form-context → first-turn boundary. (Sprint 33 was the prompt_projection sub-sprint of M1.)
4. **Q4 (skill_state)?** **YES.** A multi-tool / multi-turn flow was losing state: the user supplied `ad_id` + `email` in the form, but the next-turn `intake_state.fields_remaining` projection showed every UC-H field still missing — because `IntakeFieldExtractor` only seeded UC-K's two fields. Sprint 34 closes this state durability gap. **Layer = `skill_state`**, matching `sprint_objective.md` §8 stanza.
5. **Q5 (semantic_planner)?** No. The LLM's classification choice IS the target outcome variable for Sprint 33; Sprint 34 is downstream of classification.
6. **Q6 (eval_spec)?** No. The bad-case CaseSpec is authoritative; the §7 OQs above flag peripheral concerns (form-schema extension for UC-I, comment fold-back for Sprint71's `extractor_isNoOpForNonUcK`) that are M2 candidates.
7. **Q7 (product_policy)?** No. Form-context prefill plumbing is a UX shape, not a product policy decision.

**Tail rule (judge_calibration)?** N/A. Sprint 34's evidence is Java-deterministic; no LLM-judge re-run involved.

**Default tail (human_review_required)?** N/A. Question 4 matched cleanly.

**Confirmed:** Sprint 34 lands on `skill_state`. Matches §8 stanza in `sprint_objective.md`.

## 11. §5 Eval Acceptance bars

Walked each bar from `iteration_governance.md` §5.1:

| bar | status | cited evidence |
|---|---|---|
| Target cases pass | **PASS** | 33 extractor unit tests + 12 projection-flip + guard tests + 6 integration variants — all green; see §4 and §9 |
| Neighbor cases no regression | **PASS** | UC-K reference path: `Sprint71PartialIntakePersistenceTest` 14/14 unchanged + green; Sprint34 `ucK_stillCapturesPlatformFromUserReply` + `ucK_stillCapturesReproStepsFromCs066FormDescription` + `ucK_doesNotLeakIntoOtherUcBranches` + integration variant V5 PASS; baseline server suite 932/1/0/2 → post-Sprint-34 983/1/0/2 (+51 new tests, no new regressions) |
| Negative-control cases unchanged | **PASS** | UC-A FAQ-path: `Sprint34IntakePrefillProjectionAndGuardTest.faqPathUcs_doNotReceivePartialIntakeMerge` PASS; integration variant V6 PASS (no `intake_state` slot at all, proving the existing `IntakeFieldsRegistry.isIntakeUseCase` gate at `ContextProjectionBuilder.java:351` is preserved); UC-I no-op: `ucI_projection_isUnchangedAfterMerge_perDeliberateNoOp` + integration variant V4 PASS |
| Shadow cases no regression | **DEFERRED to M1 close** per `iteration_governance.md` §5.6 + §4.3 default. Shadow validation surface is the M1 milestone-shared bad-case suite rerun on the cumulative Sprint 33+34(+35) commit range |
| Safety floor unchanged | **PASS** | No PII redaction change; no `escalation_reason` enum touch; no Tier-0 invariant change; intake-complete guard predicate untouched at `AgentRunLoopImpl.java:628-651` — `Sprint34IntakePrefillProjectionAndGuardTest.ucH_intakeCompleteGuard_stillRejects_whenStatedReasonStillMissing` PASS confirms the §1.7 anti-lie principle holds |
| Grounding floor unchanged | **PASS** | No grounding-contract surface touched; the FAQ grounding diagnostics surface (`faq_grounding_contract.md`) is untouched; Sprint 14 grounding tests still green per full-suite run |
| Wrong-containment rate unchanged or down | **DOWN** (across the broader UC-G/H/I/J intake surface) — Sprint 34 closes the form-context re-ask containment pattern. The bot no longer asks the user to retype `ad_id` / `email` (UC-H) or `email` (UC-G) or `description` (UC-J) when the form already supplied them. Evidence: per-UC projection-flip tests + integration V1/V2/V3. UC-I no-op preserves baseline (no claim of improvement on UC-I). M1 close will broaden the evidence base across the bad-case suite |
| Over-escalation rate unchanged or down | **UNCHANGED** — Sprint 34 does NOT add a new escalation path; the existing escalation surfaces (`user_requested`, `ambiguous_intent`, `intake_complete_for_uc_<X>`, etc.) are unchanged. `Sprint34IntakePrefillProjectionAndGuardTest.ucH_intakeCompleteGuard_acceptsUserRequestedEscape` confirms the Alice closure-criterion (c) graceful-escalation shape works |
| Architecture-health metrics not regressed | **UNCHANGED** — collection is `not_started` per `iteration_governance.md` §6 table; no metric regressed because no metric is collected. Direction-of-health: `new_semantic_hardcode_count` = 0 (Sprint 34 ships zero hardcodes — see §8 anti-hardcode self-walk); `soft_signal_conversion_count` = 0 (Sprint 34 is plumbing, not soft-signal conversion); `planner_ownership_ratio` unchanged or up (the LLM still owns UC classification + inline intake_fields capture + escalation choice; the extractor is durable-state plumbing) |

**Smoke composite_score:** observation only per §5.5. Not run in Sprint 34 dev session; M1 milestone close rerun will track the smoke surface alongside the bad-case suite.

## 12. Closure verdict — left for M1 milestone-shared decision

Per `iteration_governance.md` §4.3 default + `feedback_handoff_verdict_section_delegation.md`: Sprint 34 does not close standalone. The closure verdict is decided at M1 milestone close by the deliver-agent + human, against the cumulative Sprint 33 + Sprint 34 (+ 35 + 36 if shipped) commit range and the M1 milestone acceptance bar (Alice closure-criterion + secondary observations).

**Sprint 34 dev-session evidence summary for the M1 close decision:**

- **Java baseline:** preserved (983 / 1-inherited / 0 / 2). Net delta from Sprint 33 close: +51 new tests passing (33 extractor + 12 projection-guard + 6 integration). The single failure remains the pre-existing inherited `SystemPromptUserRequestedTiebreakerTest` Sprint 24-era baseline; unchanged by Sprint 34.
- **UC-K reference:** byte-identical; `Sprint71PartialIntakePersistenceTest` 14/14 green unchanged.
- **§4.1 anti-hardcode self-walk:** expected verdict `approve`. Detail in §8.
- **Layer classification:** `skill_state`. Detail in §10.
- **Generalization coverage:** target + neighbor + negative shipped at Java layer; shadow deferred to M1 close. Detail in §6.
- **Hard fences:** all honoured. No `RuntimeIntentClassifier` / `IntentClassification` / `DriftResult` / `DriftDetector` / `UseCaseRouter` / `ClassifyUseCaseTool` / `PhaseEvaluator` / `IntakeFieldsRegistry` / `INTAKE_UCS` / `escalation_reason` enum / `ContextProjectionBuilder` / `FormContextIngestionService` / `system_prompt.txt` / UC-K helper / `Sprint71PartialIntakePersistenceTest` / sprint-archive / foundational doc / case-family / bad-case-suite / `eval_interactive/eval_interactive/` touch. The `AgentRunLoopImpl.java` merge-gate plumbing required zero edit per the §5 prediction in `docs/sprint_objective.md`.
- **OQs surfaced:** 5 (form-schema extension for UC-I prefill; inline user-content extraction discipline; UC-K userMessage-mining asymmetry; Sprint71 stale comment fold-back; planning-accuracy data point on the `AgentRunLoopImpl.java` no-edit prediction). All are M2 candidates or docs-only fold-backs; none block M1 close.

**Recommended M1 close evaluation focus:** confirm that Sprint 33's DISCOVER soft-signal + Sprint 34's intake-prefill (now both shipped) together close Alice's D1 + D2 dimensions on the M1 milestone rerun, and that the Sprint 35 Option β coverage probe (if shipped) does not surface a regression on the existing soft-signal trajectory. The Sprint 36 conditional is fired only if the M1 close evidence shows Alice's D3 (INTAKE-locked escape) is still load-bearing.
