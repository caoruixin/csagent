Paste the content below this line into a fresh Claude Code session on branch `refactor/remove-the-shackles`.

---

You are the dev agent for Sprint 34 — the second sub-sprint of Milestone M1 (DISCOVER + Intake) per the 2026-05-16 governance upgrade to the milestone framework (`docs/current/iteration_governance.md` §8).

Sprint 34 extends `IntakeFieldExtractor` from UC-K-only to also handle UC-G / UC-H / UC-I / UC-J, consuming the form-context aliases already defined in `IntakeFieldsRegistry.java`. This closes the "bot re-asks for form-context-supplied fields" gap (Alice bad case D2 dimension, but BROADER: any UC-G/H/I/J intake session today).

**Reframing note (critical context).** Sprint 33's real-LLM rerun against the Alice bad case PASSED closure-criterion (a) — the bot stayed on UC-A FAQ path and never entered intake. The original M1 plan predicted Sprint 33 alone would be IMPROVING on Alice; in fact it closed (a). Sprint 34 still ships for THREE reasons (not as an Alice-gated fix):

1. Single-run evidence is thin — M1 milestone close rerun across multiple traces is the durable evidence layer.
2. The D2 surface is broader than Alice — UC-G (GDPR), UC-I (refund), UC-J (T&S) intake paths also re-ask for form-supplied fields today.
3. Soft-signal architecture coherence — Sprint 33 told the LLM ambiguity exists; without Sprint 34, even a correctly-classified UC-H still re-asks for `ad_id` and `email`.

This means: your Java unit + integration tests are the LOAD-BEARING evidence. The Alice bad-case rerun is OPTIONAL informational evidence (Alice may stay on UC-A FAQ path again on the rerun; that's fine).

**Codex review:** deferred to M1 milestone-shared close per `iteration_governance.md` §4.3 default. You do NOT dispatch Codex at Sprint 34 close. Deliver-agent + human dispatch at M1 close against the cumulative Sprint 33 + Sprint 34 (+ 35 + 36 if shipped) commit range.

## 1. Loader

1. `AGENTS.md` (transitively loads `iteration_governance.md` §1 / §3 / §4 / §5 / §7 / **§8 NEW Milestone framework** + `doc_governance.md` + `agent_context_guide.md`).
2. `docs/milestone_objective.md` — the M1 milestone north star. Read §3 Sprint 34 row for the milestone-level scope and §6 hard fences.
3. `docs/sprint_objective.md` — your Sprint 34 contract. §2 reframing note (CRITICAL); §5 file table; §6 hard fences; §8 §7 stanza; §9 success metrics; §10 stop conditions.
4. `docs/sprints/sprint-033-handoff.md` — Sprint 33 dev evidence. §7 OQs (OQ1 = PROCESSING extension deferred, OQ3 = RESOLVE/CONFIRM projection deferred; both M2 candidates). §5 trace evidence (Alice closure-criterion (a) PASS).
5. `docs/sprints/sprint-007-handoff.md` / `docs/sprints/sprint-007-1-handoff.md` (if they exist) — the original Sprint 7 / Sprint 7.1 context for the `IntakeFieldExtractor` UC-K design. The §J0 pattern is the reference shape Sprint 34 mirrors.
6. `docs/current/iteration_governance.md` §1.3 (LLM owns classification — Sprint 34 does NOT touch this), §1.4 (Runtime owns persistence + trace contract — Sprint 34 IS this), §1.7 (forbidden list — Sprint 34's per-UC FORM-CONTEXT field mapping is plumbing, NOT a semantic hardcode; the explicit anti-hardcode walk is in §8 stanza of your sprint_objective), §3.2 Q4 (skill_state layer rationale), §4.1 (anti-hardcode kernel that Codex will walk at M1 close), §5.5/§5.6 (smoke demoted to observation; bad-case suite as new primary gate), §7 (sprint-objective stanza requirement).
7. `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` — the target bad case. Note `bad_case_metadata.related_dimensions` includes D2 (intake prefill gap); your sprint addresses D2 as side benefit, not primary gate.
8. `eval_interactive/case_specs/bad_cases/_manifest.md` — the bad-case suite convention.
9. `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldExtractor.java` — the FILE you extend. Read the entire file end-to-end. The UC-K helpers (lines 47-128, `PLATFORM_TOKEN_PATTERN`, `REGRESSION_MARKER_PATTERN`, `extractUcKFields`, `capturePlatform`, `captureRegressionText`) MUST NOT be touched per §6 hard fence #5.
10. `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` — read the FIELD_ALIASES map at lines 78-104 and the REQUIRED_FIELDS_BY_UC map at lines 53-67. These are the source-of-truth for what canonical field names each UC needs and what aliases map to them. Sprint 34 CONSUMES; does NOT EDIT this file.
11. `server/src/main/java/com/gumtree/csagent/service/runtime/FormContextIngestionService.java` lines 48-85 — confirms form_context JSON shape: `first_name`, `email`, `topic_subject`, `description`, optionally `ad_id`. These are the SOURCE fields your extractor reads.
12. `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` lines 557-577 (`mergePartialIntakeFromContext`) — the run-loop merge gate. Line 561 `if (!IntakeFieldExtractor.handlesUc(uc)) return;` is the SINGLE plumbing point that gates UC-G/H/I/J. Once `handlesUc` returns true for those UCs, the existing run-loop plumbing fires.
13. `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint71PartialIntakePersistenceTest.java` — the UC-K reference test pattern. Mirror its shape for Sprint 34's `Sprint34IntakePrefillExtractorTest` + `Sprint34IntakePrefillProjectionAndGuardTest`. **MUST CONTINUE TO PASS UNCHANGED after Sprint 34.**

## 2. Premise re-verification

Spot-check at session start (HEAD post-Sprint-33-close, commit landed on `refactor/remove-the-shackles`):

1. **`IntakeFieldExtractor.java:85-99`** — confirm `extractFromTurn` has a single `if ("UC-K".equals(uc))` branch and that `handlesUc` at lines 220-222 returns true only for UC-K. If a prior agent added UC-G/H/I/J support, STOP and surface.
2. **`IntakeFieldsRegistry.java:53-67`** — confirm required-fields map covers all 5 UCs (G/H/I/J/K) with the documented field lists. NO schema edit needed.
3. **`IntakeFieldsRegistry.java:78-104`** — confirm the alias map has the documented entries for UC-G/H/I/J/K. NO alias addition needed for Sprint 34.
4. **`FormContextIngestionService.java:48-85`** — confirm form_context JSON fields written at session start: `first_name`, `email`, `topic_subject`, `description`, optionally `ad_id`.
5. **`AgentRunLoopImpl.java:557-577`** — confirm the `mergePartialIntakeFromContext` plumbing at line 561 gates on `IntakeFieldExtractor.handlesUc(uc)`.
6. **`Sprint71PartialIntakePersistenceTest.java`** — confirm the UC-K reference test exists and passes at HEAD. `cd server && mvn -pl . test -Dtest=Sprint71PartialIntakePersistenceTest` (or your equivalent invocation) should report green.
7. **Alice bad case D2 dimension** — confirm `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` `form_context` carries `ad_id` and `email` fields (these are the values that today would be re-asked if the bot committed UC-H).

If any premise drifts, STOP and surface in handoff §3.

## 3. The work

### 3.1 Extend `IntakeFieldExtractor` for UC-G / UC-H / UC-I / UC-J

Path: `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldExtractor.java`.

**Step 1 — extend `handlesUc`** (lines 220-222) to return true for UC-G/H/I/J/K. The existing UC-K behaviour MUST remain (don't accidentally drop UC-K).

```java
public static boolean handlesUc(String uc) {
    return "UC-G".equals(uc) || "UC-H".equals(uc) || "UC-I".equals(uc)
            || "UC-J".equals(uc) || "UC-K".equals(uc);
}
```

**Step 2 — extend `extractFromTurn`** (lines 85-99) to dispatch on UC. Existing UC-K dispatch stays untouched; add per-UC branches:

```java
public static Map<String, String> extractFromTurn(String uc,
                                                 String userMessage,
                                                 String formContextJson,
                                                 ObjectMapper objectMapper) {
    Map<String, String> extracted = new LinkedHashMap<>();
    if (uc == null) {
        return extracted;
    }

    if ("UC-K".equals(uc)) {
        extractUcKFields(extracted, userMessage,
                extractFormDescription(formContextJson, objectMapper));
    } else if ("UC-H".equals(uc)) {
        extractUcHFields(extracted, userMessage, formContextJson, objectMapper);
    } else if ("UC-G".equals(uc)) {
        extractUcGFields(extracted, userMessage, formContextJson, objectMapper);
    } else if ("UC-I".equals(uc)) {
        extractUcIFields(extracted, userMessage, formContextJson, objectMapper);
    } else if ("UC-J".equals(uc)) {
        extractUcJFields(extracted, userMessage, formContextJson, objectMapper);
    }
    return extracted;
}
```

**Step 3 — add per-UC helpers** below the existing `extractUcKFields` helper. Each helper reads form_context JSON by canonical field name and seeds the canonical intake-field name.

Reference: which form_context fields map to which canonical intake fields per UC.

| UC | required fields | from form_context | seed source |
|----|-----------------|-------------------|-------------|
| **UC-G** (`[registered_email, data_request_type]`) | `registered_email` | `form_context.email` | direct |
| **UC-H** (`[ad_id_or_listing_url, registered_email, stated_reason_or_context]`) | `ad_id_or_listing_url`, `registered_email` | `form_context.ad_id`, `form_context.email` | direct |
| **UC-I** (`[transaction_reference, dispute_reason]`) | (no direct form_context seed; form fields don't carry transaction_reference) | — | (extractor may no-op for UC-I if no form_context source; document in §4 of handoff) |
| **UC-J** (`[report_target, report_type, description]`) | `description` | `form_context.description` | direct (note: UC-J's `description` is the canonical-name; the form_context field is also `description`) |

**UC-I caveat — important judgment call.** The form_context fields (`first_name`, `email`, `topic_subject`, `description`, optionally `ad_id`) do NOT include `transaction_reference` or `dispute_reason` directly. **For UC-I, the dev has a choice:**

- **Option (1, preferred)**: implement `extractUcIFields` as a deliberate no-op (returns the empty map). Document this in handoff §4 as "UC-I has no form-context seed source for `transaction_reference` / `dispute_reason`; this branch exists for symmetry and future-extensibility, but produces no fields today." Add a unit test asserting the no-op behaviour. This keeps Sprint 34 honest about which UCs ACTUALLY gain prefill coverage today.
- **Option (2, alternative)**: skip adding UC-I to `handlesUc` and skip the helper entirely; document in handoff §4 that UC-I is excluded because the form_context lacks a transaction_reference source. Surface this as an OQ for M1 close on whether to ask the human to extend `FormContextIngestionService` to capture a transaction_reference in a future sprint.

**Preferred: Option (1)** — keeps the per-UC helper structure complete and lets a future sprint add the form_context source without re-touching the extractor's structure.

**Step 4 — add `extractFormContextField` helper** (mirrors the existing `extractFormDescription` helper at lines 169-183). One method, one parameter for the field name, returns String or null:

```java
static String extractFormContextField(String formContextJson, String fieldName, ObjectMapper objectMapper) {
    if (formContextJson == null || formContextJson.isBlank() || objectMapper == null || fieldName == null) {
        return null;
    }
    try {
        JsonNode root = objectMapper.readTree(formContextJson);
        if (root == null || !root.isObject()) return null;
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) return null;
        String text = node.isValueNode() ? node.asText("") : node.toString();
        return (text == null || text.isBlank()) ? null : text;
    } catch (Exception ex) {
        return null;
    }
}
```

**Step 5 — each per-UC helper.** Example for UC-H:

```java
private static void extractUcHFields(Map<String, String> out,
                                     String userMessage,
                                     String formContextJson,
                                     ObjectMapper objectMapper) {
    // UC-H form_context seed: ad_id → ad_id_or_listing_url, email → registered_email.
    // The user's first turn message is NOT mined for these (per Sprint 34 §10
    // stop condition #4 — inline user-content extraction is a §1.7-adjacent
    // decision). The LLM owns capturing inline mentions via
    // request_handover.arguments.intake_fields (persistInlineIntakeFields).
    String adId = extractFormContextField(formContextJson, "ad_id", objectMapper);
    if (adId != null) {
        out.put("ad_id_or_listing_url", adId);
    }
    String email = extractFormContextField(formContextJson, "email", objectMapper);
    if (email != null) {
        out.put("registered_email", email);
    }
}
```

Mirror this shape for UC-G (reads `email`), UC-J (reads `description`), and UC-I (no-op per Option 1).

**Critical anti-regression discipline.** Do NOT touch `extractUcKFields`, `capturePlatform`, `captureRegressionText`, `extractFormDescription`, or the two static `Pattern` constants. UC-K behaviour MUST be byte-identical. Run `Sprint71PartialIntakePersistenceTest` after each step.

### 3.2 Confirm `mergePartialIntakeFromContext` plumbing fires for UC-G/H/I/J

Path: `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` lines 557-577.

Most likely zero change. Once `IntakeFieldExtractor.handlesUc` returns true for UC-G/H/I/J, the existing gate at line 561 falls through and the extractor fires. **Verify this via your new integration test.** If a non-obvious wiring issue surfaces, document it in handoff §4 and surface the diff.

### 3.3 New unit-test file `Sprint34IntakePrefillExtractorTest.java`

Path: `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillExtractorTest.java`.

Mirror `Sprint71PartialIntakePersistenceTest` extractor-unit-contract shape. Per-UC test groups:

- **UC-G group (~3-4 tests):** form_context with `email` seeds `registered_email`; form_context without `email` returns empty map; user-message-only run (no form_context) returns empty map for UC-G; null UC returns empty map.
- **UC-H group (~5-6 tests):** form_context with both `ad_id` and `email` seeds both canonical fields; form_context with only `ad_id` seeds only `ad_id_or_listing_url`; form_context with neither returns empty map; user-message only (no form_context) returns empty map; existing fields are not overwritten via `mergeForUc` filter (delegated to existing helper); null/blank form_context handled gracefully.
- **UC-I group (~2-3 tests):** asserts no-op behaviour (empty map) under all form_context shapes per Option 1 of §3.1. Documents UC-I has no form-context source today.
- **UC-J group (~4-5 tests):** form_context with `description` seeds canonical `description`; empty `description` returns empty map; null form_context returns empty map; non-J UC (e.g., "UC-H") calling this branch indirectly does not pollute results.
- **UC-K regression group (~2-3 tests):** the existing UC-K extractor still captures `platform` from user reply and `repro_steps_or_error_message` from form description (assert these via the same fixture as `Sprint71PartialIntakePersistenceTest`).
- **handlesUc group (~2 tests):** assert `handlesUc` returns true for UC-G/H/I/J/K and false for UC-A/B/C/D/F/FP.

Target: ~20-30 unit tests total.

### 3.4 New unit-test file `Sprint34IntakePrefillProjectionAndGuardTest.java`

Path: `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint34IntakePrefillProjectionAndGuardTest.java`.

Mirror `Sprint71PartialIntakePersistenceTest` projection-flip + guard shape. Per-UC tests:

- **Projection flip per UC (~6-8 tests):** for UC-G/H/J, after `mergeForUc` writes form_context-derived fields to `session.intakeFields`, the next `ContextProjectionBuilder.buildProjection` call shows those fields in `intake_state.fields_collected` and missing-them in `fields_remaining`. UC-I asserts no projection change (no-op).
- **Intake-complete guard per UC (~4-6 tests):** when extractor seeds enough fields AND the LLM supplies the remainder via `intake_fields` (simulate via the existing `persistInlineIntakeFields` path), `IntakeFieldsRegistry.intakeComplete(uc, collected)` flips true. Example for UC-H: extractor seeds `ad_id_or_listing_url` + `registered_email`; LLM provides `stated_reason_or_context` on `request_handover.arguments.intake_fields`; guard returns true. UC-G: extractor seeds `registered_email`; LLM provides `data_request_type`; guard returns true.
- **UC-K regression (~2 tests):** existing UC-K guard behaviour unchanged.

Target: ~12-16 tests.

### 3.5 New integration test `AgentRunLoopUcGHIJIntakePrefillIntegrationTest.java`

Path: `server/src/test/java/com/gumtree/csagent/integration/AgentRunLoopUcGHIJIntakePrefillIntegrationTest.java`.

Parameterised across 4-8 variants × ~5 invariance bars. Mirror Sprint 33's `AgentRunLoopDiscoverDisambiguationNonEnforcementIntegrationTest` or Sprint 31 fix-iteration #2's T8 shape (whichever you find cleaner; both are in the test tree).

Variants:
- **UC-G with form_context.email** — assert `intake_state.fields_collected` includes `registered_email` after one merge cycle.
- **UC-H with form_context.ad_id + form_context.email** — assert `intake_state.fields_collected` includes both `ad_id_or_listing_url` and `registered_email`.
- **UC-J with form_context.description** — assert `intake_state.fields_collected` includes `description`.
- **UC-I (any form_context)** — assert `intake_state.fields_collected` unchanged (no-op).
- **UC-K reference** — assert existing platform + repro_steps behaviour unchanged.
- **Negative: UC-A FAQ-path** — assert `mergePartialIntakeFromContext` no-ops (no `intake_state.fields_collected` write).

Invariance bars (the 5):
1. TerminalOutcome unchanged across variants (no new terminal outcomes from this plumbing change).
2. LLM call count unchanged across variants (plumbing doesn't add LLM calls).
3. Tool dispatch sequence unchanged across variants (plumbing doesn't add tool calls).
4. `session.intakeFields` write count: 1 for UC-G/H/J (and any UC-K that fires), 0 for UC-I no-op + UC-A negative.
5. Next-turn `intake_state.fields_collected` contains the expected canonical keys per the per-UC variant.

### 3.6 Bad-case suite Alice rerun (OPTIONAL, informational)

After the Java tests pass, OPTIONALLY rerun `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/`. Document in handoff §5:

- If Alice stays on UC-A FAQ path (Sprint 33 outcome carries forward), document trace evidence; the Sprint 34 prefill plumbing is in place and ready but does not exercise on this single trace. PASS.
- If Alice flips to UC-H (LLM variance), document whether prefill fired; expected closure-criterion is now (c) (graceful escalation with reason=user_requested / ambiguous_intent) because intake should not lie about collected fields. Note: UC-H requires `stated_reason_or_context` which the user CANNOT supply ("I don't know, that's what I want to find out") — so even with `ad_id_or_listing_url` + `registered_email` prefilled, `intake_complete_for_uc_h` should NOT fire.

This run is NOT load-bearing for Sprint 34 close. Your Java unit + integration tests are.

## 4. Self-walk checklist before commit

1. **Java test suite clean.** Run the full server suite. Net delta vs Sprint 33 baseline (932 / 1-inherited / 0 / 2): + ~32-46 new tests (~20-30 extractor + ~12-16 projection&guard + 4-8 integration). NO regressions. The inherited `SystemPromptUserRequestedTiebreakerTest` failure is documented baseline; everything else green.
2. **`Sprint71PartialIntakePersistenceTest` green.** This is the load-bearing UC-K regression guard. If it goes red after your changes, you broke UC-K. Roll back UC-K-touching changes and re-verify.
3. **Anti-hardcode self-walk per §4.1 nine questions.** Expected verdict `approve`. Each Q answered in handoff §8 with diff citation.
4. **§4 premise re-verification.** All 7 premises spot-checked in handoff §3.
5. **Files-changed table.** Handoff §9 lists every staged file with line range / test count.
6. **§5 Eval Acceptance bars table.** Handoff §11 walks each bar with cited evidence.
7. **Closure verdict placeholder.** Handoff §12 explicitly defers verdict to M1 milestone-shared close per §4.3 default. DO NOT write a PASS/FAIL verdict yourself.

## 5. Bundle policy

- Single commit with all §5 production + test files + `docs/sprints/sprint-034-handoff.md`. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`: DO NOT stage deliver-agent-owned files (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md` / `compact/sprint-034-dev-prompt.md`). Human bundles those at deliver-agent-side commit.

## 6. Stop conditions (mirroring §10 of sprint_objective)

STOP and report when:

1. Premise drift on §4 items.
2. Tempted to edit `IntakeFieldsRegistry` schema.
3. Tempted to widen `escalation_reason` enum.
4. Tempted to add a regex on user-typed content for UC-G/H/I/J extraction.
5. Tempted to touch `RuntimeIntentClassifier` / `UseCaseRouter` / `DriftDetector` / `ClassifyUseCaseTool` / `PhaseEvaluator`.
6. Tempted to widen DISCOVER `allowedTools` or `INTAKE_UCS`.
7. Any Java test regression beyond the documented inherited failure.
8. `Sprint71PartialIntakePersistenceTest` shows any new failure.
9. Tempted to author new case families.
10. Tempted to skip the integration test.

When you STOP, write your STOP rationale in handoff §3 (premise re-verification) or §7 (open questions) and surface to the human.

## 7. Handoff document

Write `docs/sprints/sprint-034-handoff.md` with the standard 12-section shape per `docs/sprint_objective.md` §11. Front matter:

```yaml
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
```

Section ordering and contents per `docs/sprint_objective.md` §11 (12 sections; §12 is closure-verdict-placeholder, NOT a verdict).

---

End of Sprint 34 dev prompt.
