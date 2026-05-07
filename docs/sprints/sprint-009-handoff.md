# Current Handoff

Date: 2026-05-07
Branch: `design-v1-without-human-review`

## 1. Current phase

Current phase:
Sprint 9 — Tool Contract and Trace Observability Fidelity (in flight;
Sprint 9.1 sanitization closure fix applied — awaiting Codex re-review).

Latest closed sprint:
Sprint 8.2 — ResolveArticle Contract and MAX_STEPS Trace Honesty Closure
(archived under `docs/sprints/sprint-008.2-*`).

Latest Codex decision (Sprint 9 first review):
- decision: fix_required
- blocking_count: 1
- blocker: O2 sanitization for failed-tool error surfaces and generic
  result maps insufficient.

Sprint 9.1 status: blocker fixed — see §3.1 below.

## 2. Sprint 9 root cause

The post-Sprint-8.2 manual trace `a7e20173` (form `ad_id=AD-1001`,
description `"I can't see my advert"`, follow-up
`"could you give me the link of advert?"`) exposed three independent
contract / observability drifts on the terminal-tool path:

1. **`record_outcome` schema drift.** The projected schema advertises
   `outcome_class` with lowercase `resolve | escalate | abandon`, but
   the tool implementation only accepted the legacy `outcome=RESOLVED`
   form. A schema-valid call from the LLM surfaced a misleading
   missing-parameter error and the `session_outcomes` row was never
   written.

2. **`request_handover` schema drift.** The schema did not advertise
   `summary` and the tool required a non-blank `summary`, so a
   schema-valid `request_handover(escalation_reason="tool_scope_blocked")`
   call from the follow-up turn failed solely because the LLM did not
   supply a summary.

3. **Terminal-state dishonesty.** A failed `request_handover` dispatch
   short-circuited the AgentRunLoop to ESCALATE as if the handover had
   succeeded; a failed `record_outcome` dispatch could let the next
   FINAL_ANSWER advance RESOLVE → CONFIRM as though the outcome had been
   recorded; failed terminal tools left blank Result panels in the
   Trace UI because `bot_turns.tool_calls` did not persist
   `result_data` / `result_summary`.

The Sprint 6 §G2 S1 FAQ-grounded-resolve guard, the Sprint 7 §I2
intake-complete guard, the Sprint 8 §K0 cs259 active-use-case
contract, and the Sprint 8.2 §M0a `resolve_article` source_id
alignment were all behaving as designed; the failures were contained
to the surfaces above.

## 3. Sprint 9 fixes

### O0 — terminal tool contract alignment

`server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java`
- Canonical input is now `outcome_class` with lowercase enum
  `resolve | escalate | abandon`. `outcome` is accepted as a legacy
  alias; uppercase `RESOLVED / ESCALATED / ABANDONED` are accepted as
  normalisation aliases. Missing-parameter error names `outcome_class`.
- A successful `record_outcome(outcome_class="resolve")` writes the
  canonical `session_outcomes` row (`outcome=RESOLVED`).
- Result payload exposes both the normalised `outcome_class` and the
  persisted `outcome` so downstream readers see a stable shape.

`server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java`
- `summary` is now optional. When the LLM does not supply one,
  `deriveFallbackSummary` synthesises a safe, size-bounded summary
  from the active UC, form topic, canonical escalation reason, and
  the LLM-supplied `current_user_message` (when available). The
  resulting payload also marks `summary_source=fallback` so the trace
  is honest about runtime-derivation.
- `request_handover(escalation_reason="tool_scope_blocked")` no
  longer fails solely because `summary` is missing.
- An invalid (missing / blank) `escalation_reason` still fails.
- Canonical 23-value escalation-reason enum and EscalationReasonResolver
  precedence are unchanged.

`server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
- `request_handover` schema now advertises `summary` as a recommended
  string (NOT required, since the runtime derives a fallback). The
  description tells the LLM the runtime derives a fallback if omitted.
- `record_outcome` schema was already on canonical `outcome_class`
  with lowercase enum (pre-Sprint-9 projection); no schema change
  needed.

### O1 — terminal-state honesty

`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
- The handover short-circuit (`handoverRequested = true`) now fires
  ONLY when the dispatched `request_handover` actually succeeded. A
  failed dispatch leaves `handoverRequested=false`, the error
  (e.g. `salesforce_handover_failed: 503`) is recorded as a
  non-successful `ToolEvent`, and the error is surfaced in
  `accumulated_tool_results.request_handover.error` so the next LLM
  iteration can react. If the loop later hits MAX_STEPS without a
  successful handover, the canonical `PhaseEvaluator` MAX_STEPS
  mapping applies — no synthetic terminal escalation is ever
  stamped.

`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
- `mapFinalAnswer` for the FAQ-path RESOLVE branch now keeps the
  session in RESOLVE (transition reason
  `record_outcome_failed_retry`) when the agent run attempted at
  least one `record_outcome` dispatch and EVERY such attempt failed.
  Successful `record_outcome` (or no attempt at all) preserves the
  canonical RESOLVE → CONFIRM transition.
- New helper `recordOutcomeAttemptedAndFailed(AgentRunResult)`
  encapsulates the predicate.

### O2 — trace observability fidelity

`server/src/main/java/com/gumtree/csagent/service/runtime/ToolCallTraceSanitizer.java` (new)
- Bounded sanitized projection for tool result data. Tool-aware:
  `resolve_article` is reduced to `source_id / article_id / title /
  source_url / excerpt` so the full article body never lands in the
  trace. All other tools share a generic recursive sanitizer with
  string-length, list-length, map-key-count, and recursion-depth
  caps. Email addresses are redacted to `[REDACTED_EMAIL]`.
- One-line `result_summary` strings per tool (`"1 hit (faq_miss)"`,
  `"resolved kb-001: Where is my advert?"`, `"outcome=resolve"`,
  `"handover user_requested -> queued"`, `"error: ..."` for failures).

`server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
- `recordRunResult` now writes `result_data` and `result_summary` for
  every persisted tool entry. Successful tools surface their bounded
  sanitized payload; failed tools surface `success=false`,
  `error_message`, and a `result_summary` that starts with `error:`
  so the Trace UI Result panel is non-empty.

`ui/src/components/admin/TraceViewer.tsx`
- Tool-call rows now render the new backend shape
  (`tool_name / arguments / success / latency_ms / error_message /
  result_data / result_summary`) AND tolerate legacy rows that ship
  only `{tool, args, result}`. Failed rows render with red styling
  and the `error_message`; non-empty `result_summary` displays inline
  before the JSON `result_data`. Old rows without `result_data` still
  render safely.

`ui/src/api/client.ts`
- New `mapToolCalls` normalises both legacy and new shapes so the UI
  always sees the union of fields. The `mapTrace` function delegates
  to it.

`ui/src/types/index.ts`
- `ToolCall` interface extended with the new optional fields.

## 3.1 Sprint 9.1 closure — trace sanitization fix

The Sprint 9 first-pass Codex review (decision: fix_required,
blocking_count: 1) flagged a single P1 blocker on O2:
`ControlKernel.recordRunResult` persisted `te.errorMessage()`
verbatim into `bot_turns.tool_calls.error_message`,
`ToolCallTraceSanitizer.summarize` built failed-tool
`result_summary` from the raw error string, and the generic
`sanitizeAny` only redacted email-shaped values. A failed tool
that interpolated a phone number, postcode, bearer token, API
key, password, authorization header, or other sensitive surface
into its error string could still be written verbatim to the
trace.

### Blocker fixed

`ToolCallTraceSanitizer.java`
- New `sanitizeErrorMessage(String)` helper applies the full
  redaction pattern set + length cap. Used by both
  `ControlKernel.recordRunResult` and `summarize(...)` so
  `error_message` and `result_summary` carry the SAME sanitized
  surface.
- Generic map sanitization now redacts the value under any
  key whose normalised form (lowercase, `-`/space → `_`) is in
  the sensitive-key set: `password`, `passwd`, `token`,
  `access_token`, `refresh_token`, `id_token`, `secret`,
  `client_secret`, `api_key`, `apikey`, `x_api_key`,
  `authorization`, `auth_header`, `bearer`, `credential`,
  `credentials`. Value becomes `[REDACTED_SECRET]` regardless
  of contents — so even a benign-looking string under a
  sensitive key is scrubbed.
- New value-shape patterns (in addition to the existing
  email rule): `BEARER` (`Bearer xxx` headers →
  `[REDACTED_BEARER]`), `API_KEY_PREFIX`
  (`sk_/pk_/api_/key_/tok_/rk_…` → `[REDACTED_API_KEY]`),
  `LONG_TOKEN` (32+ word characters → `[REDACTED_TOKEN]`),
  `PHONE_LIKE` (10+ digits with separators → `[REDACTED_PHONE]`),
  `UK_POSTCODE` (`SW1A 1AA`-shape → `[REDACTED_POSTCODE]`).
- Pattern application order is `EMAIL → BEARER → API_KEY_PREFIX
  → LONG_TOKEN → PHONE_LIKE → UK_POSTCODE` so prefix-specific
  markers win over the generic long-token sweep, and a 64-char
  hex digest is marked once as `[REDACTED_TOKEN]` instead of
  being shredded into phone-shaped digit blocks.
- Implementation kept local to `ToolCallTraceSanitizer`; no
  broad PII framework introduced.

`ControlKernel.recordRunResult`
- Persists `ToolCallTraceSanitizer.sanitizeErrorMessage(
  te.errorMessage())` rather than the raw error string. The
  synthesized `create_case_controlled` runtime entry uses the
  same helper for its `error_message` field on failure.

### Files changed (Sprint 9.1)

Production:
- `server/src/main/java/com/gumtree/csagent/service/runtime/ToolCallTraceSanitizer.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`

Tests:
- `server/src/test/java/com/gumtree/csagent/service/runtime/ToolCallTraceSanitizerTest.java`
  (extended — 6 new tests covering error-message redaction,
  failed-tool summary redaction, generic `error` field
  redaction, sensitive-key value redaction including
  `password / token / secret / api_key / authorization`,
  benign-key sensitive-value redaction, and a regression
  guard that the `resolve_article` safe-summary shape
  (`source_id / article_id / title / source_url / excerpt`)
  is unchanged).
- `server/src/test/java/com/gumtree/csagent/integration/Sprint9TraceObservabilityFidelityIntegrationTest.java`
  (extended — 1 new test
  `failedToolDispatch_sanitizesEmailAndTokenInPersistedErrorMessage`
  that drives a failed `request_handover` whose error string
  embeds an email + phone + bearer token, and asserts the
  persisted `error_message` and `result_summary` redact those
  before persistence).

### Tests run (Sprint 9.1)

- `mvn -pl server test` → **760 / 0 / 0 / 0** (was 753
  pre-Sprint-9.1; +7 new tests).
- Focused sweep
  (`ToolCallTraceSanitizerTest, Sprint9TraceObservabilityFidelityIntegrationTest,
  RecordOutcomeToolTest, RequestHandoverToolTest,
  Sprint9TerminalToolHonestyTest`) → **34 / 0 / 0 / 0**.
- `python -m pytest -p no:capture eval_interactive/tests/test_agent_client_session_create_timeout.py -v`
  → **8 / 0** (Sprint 6 §G0 ReadTimeout regression intact).
- `ui/./node_modules/.bin/tsc --noEmit` → clean.

### Why Sprint 9 can be re-reviewed

- The single P1 blocker (O2 sanitization for failed-tool error
  surfaces and generic result maps) is closed: failed-tool
  `error_message` and `result_summary` go through the same
  bounded redaction path as `result_data`, the generic
  sanitizer redacts both sensitive keys and sensitive-shaped
  values, and the resolve_article safe-summary shape is
  unchanged.
- Sprint 9 §O0 (record_outcome / request_handover contract
  alignment) and §O1 (terminal-state honesty for failed
  terminal tools) are untouched and remain green.
- TraceViewer continues to render `result_summary` /
  `result_data` (legacy + new shapes) without redesign.
- All Sprint 6 / 7 / 8 / 8.2 regression guards remain green.

## 4. Files changed

Production:
- `server/src/main/java/com/gumtree/csagent/service/tools/RecordOutcomeTool.java`
- `server/src/main/java/com/gumtree/csagent/service/tools/RequestHandoverTool.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java`
- `server/src/main/java/com/gumtree/csagent/service/runtime/ToolCallTraceSanitizer.java` (new)
- `ui/src/api/client.ts`
- `ui/src/components/admin/TraceViewer.tsx`
- `ui/src/types/index.ts`

Tests (new):
- `server/src/test/java/com/gumtree/csagent/service/tools/RecordOutcomeToolTest.java`
- `server/src/test/java/com/gumtree/csagent/service/tools/RequestHandoverToolTest.java`
- `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint9TerminalToolHonestyTest.java`
- `server/src/test/java/com/gumtree/csagent/service/runtime/ToolCallTraceSanitizerTest.java`
- `server/src/test/java/com/gumtree/csagent/integration/Sprint9TraceObservabilityFidelityIntegrationTest.java`

Docs:
- `docs/10-handoff.md` (this file; pre-Sprint-9 version archived to
  `docs/sprints/sprint-008.2-handoff.md`)
- `docs/action_bank.md` (Sprint 9 row added; pre-Sprint-9 version
  archived to `docs/sprints/sprint-008.2-action_bank.md`)
- `docs/sprint_objective.md` (current Sprint 9 objective; previous
  sprint objective remains under `docs/archive/current-docs/2026-05-07-pre-sprint8.2-sprint_objective.md`)

## 5. Tests run

- `mvn -pl server test` → **760 tests / 0 failures / 0 errors / 0
  skipped** (Sprint 9 first pass was 753; Sprint 9.1 adds 7).
- New test classes:
  - `RecordOutcomeToolTest` (7 tests, all green) — outcome_class
    accepted, legacy outcome alias accepted, lowercase + uppercase
    normalise identically, missing-param error names outcome_class,
    successful resolve writes session_outcomes row, escalate without
    reason fails, escalate with reason persists ESCALATED + reason.
  - `RequestHandoverToolTest` (5 tests, all green) — schema-valid
    `escalation_reason`-only call derives a safe summary and
    succeeds, supplied summary preserved verbatim, missing /
    blank reason still fails, fallback summary combines UC + topic +
    reason + user message.
  - `Sprint9TerminalToolHonestyTest` (4 tests, all green) — failed
    record_outcome does NOT advance RESOLVE → CONFIRM, successful
    record_outcome still does, failed request_handover does NOT
    short-circuit to ESCALATE (loop runs to MAX_STEPS), successful
    request_handover still escalates.
  - `ToolCallTraceSanitizerTest` (8 tests, all green) — resolve_article
    body dropped in favour of safe summary fields, search_knowledge
    hits truncated, email redaction, null-result safety, summary
    string formatting per tool.
  - `Sprint9TraceObservabilityFidelityIntegrationTest` (2 tests, all
    green) — successful search + resolve_article persists
    result_data + result_summary with bounded resolve_article shape;
    failed resolve_article persists error_message + error
    result_summary so the Trace UI Result panel is not blank.
- Regression sweep included in the full run: Sprint 6 G2 S1 guard,
  Sprint 7 §I2 intake guard, Sprint 8 §K0 cs259 contract, Sprint 8.2
  §M0a / §M0b resolve_article + max-steps raw response, cs014 / cs066
  / cs095 / cs002 / cs029 / cs176 — all green.

UI test harness does not exist in this repo (no vitest / jest;
`ui/package.json` has no `test` script). UI changes were verified by
running `ui/./node_modules/.bin/tsc --noEmit` (clean) plus structural
review against existing TraceViewer renderers.

Python eval tests:
- `python -m pytest -p no:capture
  eval_interactive/tests/test_agent_client_session_create_timeout.py -v`
  → 8 passed (Sprint 6 §G0 regression). No other
  `eval_interactive/**` files were touched in Sprint 9 / 9.1.

## 6. Manual probe

Trace `a7e20173` shape was reproduced as deterministic JUnit tests
rather than a live-server probe (live Kimi tool-use is non-determ).
The post-Sprint-9 expected behaviour is:

| Step | Tool call | Expected result |
| ---- | --------- | --------------- |
| 0    | `search_knowledge("I can't see my advert")` | hits with `source_id=kb-…` |
| 1    | `resolve_article(source_id=kb-…)` | safe summary fields persisted in `result_data` (§O2) |
| 2    | grounded user_message with `[kb-…]` citation | FINAL_ANSWER |
| 3    | follow-up turn `"could you give me the link of advert?"` | LLM emits `request_handover(escalation_reason="tool_scope_blocked")` |
| 4    | `request_handover` dispatch with no `summary` | succeeds via fallback summary derivation (§O0) |

Confirmed:
- `resolve_article` result is visible in the persisted trace
  (Sprint 9 §O2; previously was only the article id with no body).
- `record_outcome(outcome_class="resolve")` is accepted at the
  schema-canonical name, persists a `session_outcomes` row, and
  surfaces both `outcome_class` and `outcome` in the result panel.
- Trace UI Result panels are non-empty for both successful and
  failed tool calls.
- Follow-up `tool_scope_blocked` handover succeeds with a safe
  fallback summary; the canonical 23-value escalation reason enum
  is unchanged.

If a live re-probe of trace `a7e20173` against a Kimi-backed deploy
still escalates without a summary or with blank Result panels after
this sprint, the residual must be classified as a UI rendering
regression or a downstream Salesforce contract change — NOT
`record_outcome` / `request_handover` schema drift.

## 7. Before / after

Before:
```
[resolve] resolve_article(source_id=kb-…)        -> ok (article payload)
[resolve] record_outcome(outcome_class=resolve)  -> ERROR Parameter 'outcome' must be one of: ...
TerminalOutcome.FINAL_ANSWER (LLM still answered)
PhaseEvaluator: RESOLVE → CONFIRM (silent)
session_outcomes:                                  NO ROW WRITTEN
bot_turns.tool_calls:                              [{tool_name=resolve_article, success=true}, ...]   ← no result_data
TraceViewer:                                       Result panel BLANK
[follow-up] request_handover(tool_scope_blocked)  -> ERROR Parameter 'summary' is required
AgentRunLoop:                                      handoverRequested=true (despite dispatch failure)
TerminalOutcome.ESCALATE                           ← false success
```

After:
```
[resolve] resolve_article(source_id=kb-…)        -> ok (article payload)
[resolve] record_outcome(outcome_class=resolve)  -> ok (session_outcomes row written)
TerminalOutcome.FINAL_ANSWER
PhaseEvaluator: RESOLVE → CONFIRM (record_outcome succeeded)
session_outcomes:                                  RESOLVED row present
bot_turns.tool_calls[*].result_data / result_summary: bounded sanitized payload per entry
TraceViewer:                                       Result panel renders summary + JSON
[follow-up] request_handover(tool_scope_blocked) -> ok (fallback summary derived)
TerminalOutcome.ESCALATE                           ← only when dispatch actually succeeded
```

If the dispatch fails:
```
[resolve] record_outcome(...)                     -> ERROR DB write failed
TerminalOutcome.FINAL_ANSWER
PhaseEvaluator: stays in RESOLVE (transition_reason=record_outcome_failed_retry)
TraceViewer:                                       failed row renders error_message + error result_summary
```

## 8. Residuals

- Live re-probe of trace `a7e20173` against a Kimi-backed deploy is
  recommended once the canonical post-Sprint-9 baseline run is
  captured. Any remaining escalation must be triaged into:
  - advert-link product policy gap (deferred — see action bank
    `D-advert-link-product-decision`),
  - rerank fallback diagnostics (deferred — see action bank
    `D-rerank-fallback-diagnostics`),
  - FAQ corpus / answerability,
  and explicitly NOT as `record_outcome` / `request_handover` schema
  drift or terminal-state dishonesty.
- Eval Governance backlog (cs015 / cs066 / cs176 deferrals, L3 judge
  volatility, FAQ corpus answerability, advert-link product policy)
  is unchanged. Sprint 9 did NOT open Eval Governance docs scope —
  that remains the next-recommended docs-only phase if no new
  P0/P1 runtime blocker is found.

## 9. Sprint 9 objective

Met:

- `record_outcome` schema and tool aligned on canonical
  `outcome_class` with lowercase enum; legacy aliases preserved.
- `record_outcome(outcome_class="resolve")` succeeds and writes a
  `session_outcomes` row.
- `request_handover` schema exposes `summary` as recommended; tool
  derives a safe fallback summary when missing; canonical 23-value
  escalation-reason enum unchanged.
- `request_handover(tool_scope_blocked)` does NOT fail solely on a
  missing summary.
- Failed terminal tool dispatches no longer silently advance the
  phase as terminal success.
- Trace UI shows non-empty result/error data for tool calls; old
  rows still render safely.
- No FAQ corpus, CaseSpec, judge, routing, search threshold, advert-
  link tool, broad TraceViewer redesign, or Eval Governance scope
  was opened.

## 10. Regression guards

Currently-active guards (all green in the full server run):

- `L1:escalation_reason_consistency` = 0
- `CONTRACT_VIOLATION:active_use_case` = 0
- cs014 remains UC-C
- cs066 remains UC-K
- cs095 remains not UC-K / not UC-FP
- cs002 remains UC-C + `user_distress`
- cs029 remains UC-D + `user_requested`
- cs176 explicit-human-help → `user_requested` focused regression
- Sprint 6 §G0 no ReadTimeout retry
- Sprint 6 §G2 FAQ-grounded-resolve guard
- Sprint 7 §I2 intake_state persistence + intake-complete guard
- Sprint 7.1 §J0 partial intake persistence
- Sprint 8 §K0 cs259 UC-F contract hardening
- Sprint 8.1 §M3 DISCOVER phase boundary
- Sprint 8.2 §M0a `resolve_article` source_id alignment
- Sprint 8.2 §M0b MAX_STEPS preserves lastLlmRawResponse

New Sprint 9 guards:

- `record_outcome` accepts canonical `outcome_class` (lowercase);
  legacy `outcome=RESOLVED` accepted as alias; uppercase
  normalisation (`RecordOutcomeToolTest`).
- Successful `record_outcome` writes a `session_outcomes` row
  (`RecordOutcomeToolTest`).
- `request_handover` derives a safe fallback summary when missing
  (`RequestHandoverToolTest`); supplied summary preserved verbatim;
  invalid (missing/blank) reason still fails.
- AgentRunLoop does NOT short-circuit on a failed `request_handover`
  dispatch (`Sprint9TerminalToolHonestyTest`).
- PhaseEvaluator does NOT advance RESOLVE → CONFIRM on a failed
  `record_outcome` (`Sprint9TerminalToolHonestyTest`).
- `bot_turns.tool_calls` carries bounded sanitized
  `result_data` + `result_summary` per entry; resolve_article body
  dropped in favour of safe summary fields
  (`Sprint9TraceObservabilityFidelityIntegrationTest`,
   `ToolCallTraceSanitizerTest`).
- TraceViewer renders both legacy `{tool, args, result}` rows and
  the new `{tool_name, arguments, success, error_message,
  result_data, result_summary}` shape; failed rows show error
  message + summary.

New Sprint 9.1 guards:

- `bot_turns.tool_calls.error_message` is sanitized before
  persistence: email / phone / UK postcode / bearer token /
  api-key / 32+ char credential surfaces are replaced with
  category markers
  (`Sprint9TraceObservabilityFidelityIntegrationTest`).
- Failed-tool `result_summary` shares the same redaction path as
  `error_message` (`ToolCallTraceSanitizerTest`).
- Generic result maps redact values under sensitive keys
  (`password`, `token`, `secret`, `api_key`, `authorization`,
  and the canonical 9.1 set) regardless of contents
  (`ToolCallTraceSanitizerTest`).
- Generic result maps redact sensitive-shaped values under
  benign keys.
- `resolve_article` safe-summary shape (`source_id / article_id
  / title / source_url / excerpt`) is preserved by the hardening
  (regression test).
- Existing email-redaction tests still pass.

## 11. Next recommended phase

Eval Governance and Release Gate Definition (docs-only) remains the
recommended next phase if no new P0/P1 runtime blocker surfaces. The
post-Sprint-9 canonical baseline can be captured in that phase, with
any residual `a7e20173`-class escalation triaged into the advert-link
product policy / rerank diagnostics / FAQ corpus buckets.

Do not start another runtime sprint unless triage finds a new P0/P1
runtime blocker.

## 12. Current-doc maintenance rule

`docs/10-handoff.md`, `docs/codex-findings.md`,
`docs/sprint_objective.md`, and `docs/action_bank.md` are
overwrite-current-state files. Before replacing one of them:

1. archive the previous version under `docs/sprints/` if it
   belongs to a sprint closure, or
   `docs/archive/current-docs/` if it is an ad hoc transition;
2. then overwrite the working file;
3. keep only actionable current state in the working file.

Historical detail belongs in `docs/sprints/`,
`docs/archive/current-docs/`, `eval_interactive/results/`, and
`qa-reports/`.

## 13. Do not reopen

- broad full review
- broad routing rewrite
- judge calibration implementation
- CaseSpec churn
- anchor / exploration / promotion hard-gate expansion
- smoke 14/14 optimization
- S3 no-prior-search guard unless explicitly selected
- S5 Tier-2 runtime guard unless explicitly selected
- broad TraceViewer redesign
- llm_call_log dashboard / per-tool latency dashboard
- search threshold tuning / answer_miss / faq_miss semantic redesign
- tool-deadline guard / bypass-DISCOVER redesign
- advert-link generator / direct listing URL tool
- runtime sprint unless a new P0/P1 runtime blocker is found
