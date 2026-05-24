---
title: Sprint 51 / M5 S2 — Per-invocation trace (B2 full persistence) — dev handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-24
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sub-sprint dev-authored handoff. SECOND sub-sprint of Milestone M5 —
  Observability Coherence. OBSERVATION-ONLY: the run loop's control flow,
  LLM call count, ordering, timing, and termination semantics are
  unchanged; only the per-step side-record is added. §12 reserved for
  deliver-agent + human at sub-sprint close.
---

# Sprint 51 / M5 S2 — Per-invocation trace dev handoff

## 1. Goal and outcome

Promotes per-invocation LLM data to a first-class persisted observation
so the admin trace can render every loop step (instead of only the final
step's value, the limitation of `BotTurn`'s single-column overwrite). A
new `bot_turn_llm_calls` table stores one row per LLM call inside an
`AgentRunLoop` step boundary, carrying the full untruncated raw response
+ that step's projection + tool_calls + latency + model. The admin
`/trace` endpoint nests the per-step records under each turn; the UI
renders "Invocation 1..N" per turn (default-collapsed, each expandable
to its own projection + raw response). `BotTurn`'s existing single
columns keep the final-step value (backward-compat).

Implementation is **observation-only** in the strict sense:
`AgentRunLoopImpl` accumulates a side-record at the existing step
boundary, never reads it, never branches on it, and never reorders or
re-times the LLM/tool dispatch. The loop's terminal outcome and
`AgentRunResult`'s pre-existing fields are byte-for-byte unaffected.

## 2. Scope (per `docs/sprint_objective.md` #1-#6)

### #1 — New persistence: `bot_turn_llm_calls` table

- Flyway migration `V15__create_bot_turn_llm_calls.sql` — next version
  after `V14` (the prior latest was `V14__intake_ambiguous_candidates`;
  `V9__create_llm_call_log` is the existing per-call summary table,
  untouched).
- JPA entity `model/BotTurnLlmCall.java` — Lombok `@Data @Builder
  @Entity`, columns: `id` (BIGSERIAL PK), `bot_turn_id` (FK →
  `bot_turns.turn_id` with `ON DELETE CASCADE`), `step_index` (int,
  0-based), `call_type` (`chat` / `routing` / `rerank`), `model`,
  `llm_raw_response` (full text, untruncated), `projected_context`
  (jsonb), `tool_calls` (jsonb), `latency_ms`, `created_at`. Index on
  `(bot_turn_id, step_index)`.
- Repository `repository/BotTurnLlmCallRepository.java` —
  `findByBotTurnIdOrderByStepIndex(String)`.

### #2 — Capture per-step in `AgentRunLoopImpl`

`AgentRunLoopImpl.run()` (~`:160`) gains a local `List<LlmCallRecord>
llmCallRecords = new ArrayList<>()` accumulator alongside the existing
`llmEvents`. After each LLM invocation + parser pass at the existing
step boundary (~`:283`), one `LlmCallRecord` is appended with
`{step_index, "chat", model, latency, raw_response, projection,
toolCalls}`. The parser-failure / null-action early-returns also add a
record (with empty `tool_calls`) so a malformed payload is still
captured in the trace.

The new model class `model/LlmCallRecord.java` is the immutable record
shape passed from the loop to `TraceWriter` via `AgentRunResult`.

**Loop control flow / call count / ordering / timing / termination are
unchanged** — the accumulator is a side-effect that runs after the
existing LLM call + parser, never affects loop decisions, never gates
anything.

### #3 — Persist via the trace-write path

`AgentRunResult` (record) gains one additive field
`List<LlmCallRecord> llmCallRecords`. A backward-compat 8-arg canonical
constructor preserves every existing call site (defaults the new field
to `List.of()`); every static factory gains a new overload accepting
records, with the old overloads delegating to the new one with
`List.of()`. **Existing fields' semantics are unchanged** —
`lastProjection` / `lastLlmRawResponse` still carry the final-step
value.

`TraceWriter` (`service/observability/TraceWriter.java`) gains a new
method `recordLlmCalls(String botTurnId, List<LlmCallRecord> records)`
that persists one `BotTurnLlmCall` row per record. The existing
`recordTurn(...)` method is unchanged. `TraceWriter`'s constructor now
takes the new `BotTurnLlmCallRepository` in addition to its existing
deps; no production caller constructs `TraceWriter` manually (all
Spring-autowired).

`ControlKernel.recordRunResult(...)` (the AgentRunLoop trace-write
path) calls `traceWriter.recordLlmCalls(turn.getTurnId(),
result.llmCallRecords())` immediately after the existing
`turnRepository.save(turn)`. The `BotTurn` row is unchanged; the
per-step rows are FK'd to its already-persisted `turn_id`. The legacy
non-loop path (`ControlKernel.recordTurn(...)`) is untouched (it has no
multi-step records to persist).

`ControlKernel.mergeAgentRunResults(...)` (used for the DISCOVER → RESOLVE
same-turn replan) merges `llmCallRecords` across the two sub-runs so
the merged turn surfaces every invocation.

### #4 — Expose via the trace endpoint

NEW DTO `controller/dto/BotTurnTrace.java` — `{@JsonUnwrapped BotTurn
turn, List<BotTurnLlmCall> llmCalls}`. The `@JsonUnwrapped` keeps the
top-level BotTurn fields flat (the existing trace clients see the same
shape); the new `llmCalls` array is additive.

`LocalEventStore.getSessionTraceWithLlmCalls(String)` joins
`BotTurnRepository` + `BotTurnLlmCallRepository` per turn and returns
`List<BotTurnTrace>`. The existing `getSessionTrace(String)` returning
`List<BotTurn>` is preserved for any internal caller (admin endpoint
swapped to the new method).

`DemoInspectionController.getSessionTrace(...)` now returns
`ResponseEntity<List<BotTurnTrace>>`. **The frozen
`GET /sessions/{id}/llm-calls` endpoint is UNTOUCHED** (eval harness
contract: `agent_client.py::get_llm_calls`).

### #5 — Render in the UI

`ui/src/types/index.ts` — `TraceStep.llm_calls?: LlmCall[]` (optional;
older sessions ship without it). NEW interface `LlmCall` mirrors the
`bot_turn_llm_calls` row shape with snake_case fields.

`ui/src/api/client.ts` — `mapTrace(...)` extracts `t.llmCalls` (camel-
or snake-case) into `TraceStep.llm_calls` (tolerant of either Spring
camelCase or future snake_case payloads).

`ui/src/components/admin/TraceViewer.tsx` — NEW component
`LlmInvocationsPanel` (collapsed by default — only the count is
visible) listing NEW `LlmInvocationCard` per call. Each card shows
{step_index, call_type pill (chat/routing/rerank colored distinctly),
model, latency_ms} and three independent collapsible subsections per
invocation: **Projected Context (this step)** + **LLM Raw Response
(this step)** + **Tool Calls (this step)**. The existing final-step
`LlmDetailPanel` is preserved — the new panel renders **below it**
inside the same expanded step row, so the legacy single-value view is
unchanged and the new list is additive.

### #6 — Tests

NEW
`server/src/test/java/com/gumtree/csagent/integration/Sprint51PerInvocationTracePersistenceTest.java`
— 2 tests:

- `multiStepFaqTurn_persistsOneRowPerInvocation_finalStepValuesPreservedOnBotTurn`:
  drives a 3-step FAQ run (search → resolve_article → final answer)
  and asserts (i) exactly 3 rows written to `bot_turn_llm_calls` with
  monotonic `step_index = 0/1/2`, all `call_type = "chat"`, FK pointing
  at the just-written `BotTurn.turn_id`; (ii) each row carries the
  step's own full raw response + own projection (step-0's row holds
  step-0's content, NOT step-2's — the explicit observation that the
  per-step record survives the loop's overwrite); (iii) the existing
  `BotTurn` single columns keep the FINAL step's value (matches the
  loop's overwrite, unchanged from pre-S2 — backward-compat preserved);
  (iv) `model` is stamped from `LlmInvocationService.getModelName()`.
- `traceWriterNull_perStepRecordsSilentlySkipped_botTurnUnaffected`:
  exercises the back-compat 15-arg `ControlKernel` constructor (null
  `TraceWriter`) and asserts the single-column `BotTurn` trace is
  unaffected — the per-step persistence is silently skipped, matching
  the observation-only invariant.

Observation-only proof: the existing 1163 main + 2-skipped baseline
passes unchanged (see §4); +2 new tests added by this sub-sprint.

## 3. §4.1 anti-hardcode self-walk (sub-sprint §7-exempt — Codex deferred to M5 close per §4.3)

S2 is `infra` / observation; §7-exempt per `sprint_objective.md` §"Class".
A short §A stanza is in the sprint contract because S2 touches `server/`
runtime code; the 9-question kernel walk:

1. **Adds keyword/regex/if-else/enum/per-UC matrix for a semantic
   decision?** NO. The new code is pure side-record + persistence +
   rendering. No keyword, regex, enum, or per-UC branch. `call_type`
   is sourced from the loop's existing call-site (always `"chat"` at
   the loop boundary; routing/rerank labelling is reserved for the
   `LlmInvocationService` if those paths ever flow through here).
2. **Tier-0 invariant protection?** N/A — no new Tier-0; nothing
   protected.
3. **Soft-signal alternative?** N/A — there is no semantic decision to
   route through a projection.
4. **Encodes visible-eval text / trace phrasing / CaseSpec id?** NO. No
   eval text or CaseSpec id touched.
5. **Moves semantic ownership LLM → Java?** NO. The new code only
   *records* what the loop already produced. The LLM owns every
   semantic decision; we just store more of what it did.
6. **If-else block in prompt instead of principle-level guidance?**
   N/A — the prompt is not touched.
7. **Preserves tool schema / capability / PII / grounding floors?**
   YES — all four untouched.
8. **Generalization coverage (target / neighbor / negative / shadow)?**
   N/A — observation-face change, no LLM behaviour change.
9. **Rollback/sunset plan if temporary?** N/A — not a temporary
   measure; this is the canonical per-invocation trace surface going
   forward.

Expected verdict: clean `approve` (named exemption: pure observation
record + persistence + rendering).

## 4. Tests + baselines

### Java — `mvn test -B`

- Baseline at `9ef9d1e` (M5 S1 close): **1163 main + 1 inherited fail
  (`SystemPromptUserRequestedTiebreakerTest`) + 0 errors + 2 skipped**.
- This sub-sprint result at working-tree HEAD: **1165 main + 1
  inherited fail + 0 errors + 2 skipped** (the `+2` is the new
  `Sprint51PerInvocationTracePersistenceTest`'s two passing tests; the
  inherited tiebreaker failure persists per OQ-S41.5 STATUS QUO,
  unrelated to this sub-sprint).
- Run command + output excerpt (from `target/surefire-reports`):

  ```
  [ERROR] Tests run: 1165, Failures: 1, Errors: 0, Skipped: 2
  [ERROR]   SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53
        ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor ==> expected: <true> but was: <false>
  ```

- The 1163 existing run-loop tests pass UNCHANGED — this is the
  observation-only proof at the test-suite level (the loop's terminal
  outcomes, call counts, ordering, etc. are byte-for-byte preserved).
  Sprint 8 / 9 / 11 / 14 etc. AgentRunLoop integration tests all
  pass without modification.

### Python — `cd eval_interactive && uv run python -m pytest --tb=no -q`

- Baseline at `9ef9d1e` (M5 S1 close): **3 failed / 486 passed**.
- This sub-sprint result: **3 failed / 486 passed** — identical
  distribution (the 3 residual env-specific failures
  `test_v2_schema_loads_cleanly`, `test_smoke_review_report_tracks_smoke_set_and_overrides`,
  `test_full_corpus_lints_clean_with_smoke_subset_flag` persist per
  OQ-S47.3; not regressions; no S2 Python touch).

### UI — `npx tsc -b`

- Clean (exit 0).

### Spot real-LLM multi-step trace eyeball

**Not run — STOP-and-surface per the sub-sprint contract.**

The contract requires a real-LLM run of one multi-step FAQ turn to
confirm the admin trace shows N invocations each with own projection +
raw response, AND that the turn's terminal outcome is unchanged vs a
pre-S2 run. This requires:

- `make backend` running with a live LLM key + Postgres + Redis;
- one multi-step FAQ session driven through the UI;
- the new `/v1/demo/sessions/{id}/trace` payload eyeballed in the
  Admin tab.

The dev session is running in a sandbox without backend / LLM keys
available, so this step is **deferred to deliver-agent + human at
sub-sprint close** per §"STOP and surface" in the dev-prompt and the
"backend/keys unavailable → STOP and surface" clause in §6 of the
contract. The Java observation-only test
(`Sprint51PerInvocationTracePersistenceTest`) provides the logic
proof; the live-trace eyeball is the contract surface that wants the
real bot in the loop.

**Deliver-agent action at close**: bring up `make backend`, run one
multi-step FAQ turn (e.g. UC-A "I can't see my advert" with
`max_tool_steps=4`), GET `/v1/demo/sessions/{id}/trace`, and confirm:
(a) each turn's `llmCalls` array is present with N ≥ 2 entries (FAQ
multi-step) and the entries have monotonic `stepIndex`; (b) each
`projectedContext` and `llmRawResponse` is distinct (not all equal to
the final step's); (c) the terminal outcome of the turn matches a
pre-S2 run of the same input (e.g. FINAL_ANSWER vs ESCALATE, same as
the pre-S2 behaviour). Record the eyeball evidence in §12 (deliver-
agent).

### Bad-case suite

Out of scope per `sprint_objective.md` §"Test / eval requirements" — the
bad-case suite regression rerun is the **M5 milestone-close gate**, not
this S2 close. S2 is observation-only and must not change bot behaviour.

## 5. Files changed (`git show --numstat` style)

NEW (server):

- `server/src/main/resources/db/migration/V15__create_bot_turn_llm_calls.sql` (+19 lines)
- `server/src/main/java/com/gumtree/csagent/model/BotTurnLlmCall.java` (entity)
- `server/src/main/java/com/gumtree/csagent/model/LlmCallRecord.java` (in-memory record)
- `server/src/main/java/com/gumtree/csagent/repository/BotTurnLlmCallRepository.java`
- `server/src/main/java/com/gumtree/csagent/controller/dto/BotTurnTrace.java`

NEW (test):

- `server/src/test/java/com/gumtree/csagent/integration/Sprint51PerInvocationTracePersistenceTest.java`

MODIFIED (server):

- `server/src/main/java/com/gumtree/csagent/model/AgentRunResult.java` —
  +1 additive field `llmCallRecords` + factory overloads + 8-arg
  back-compat constructor.
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java` —
  side-record accumulator + per-step `LlmCallRecord` append at existing
  step boundary; all factory returns updated to pass `llmCallRecords`.
- `server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java` —
  +1 `getModelName()` getter used to stamp `model` on each per-step
  record.
- `server/src/main/java/com/gumtree/csagent/service/observability/TraceWriter.java` —
  +1 `recordLlmCalls(...)` method + constructor takes the new
  repository.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` —
  constructor + back-compat overloads accept the optional `TraceWriter`
  (null when wired by older tests); calls
  `traceWriter.recordLlmCalls(...)` after `turnRepository.save(turn)`
  in `recordRunResult`; `mergeAgentRunResults(...)` merges
  `llmCallRecords`.
- `server/src/main/java/com/gumtree/csagent/service/observability/LocalEventStore.java` —
  +1 `getSessionTraceWithLlmCalls(...)` method + ctor takes the new
  repo.
- `server/src/main/java/com/gumtree/csagent/controller/DemoInspectionController.java` —
  `/trace` returns `List<BotTurnTrace>` (was `List<BotTurn>`);
  `/llm-calls` UNCHANGED.

MODIFIED (UI):

- `ui/src/types/index.ts` — +1 `LlmCall` interface + optional
  `TraceStep.llm_calls`.
- `ui/src/api/client.ts` — `mapTrace(...)` maps `llmCalls` array.
- `ui/src/components/admin/TraceViewer.tsx` — NEW
  `LlmInvocationsPanel` + `LlmInvocationCard`; rendered after the
  existing final-step `LlmDetailPanel` inside each expanded step row.

## 6. Risks / known caveats

- **TraceWriter is constructor-optional on `ControlKernel`** to keep
  the existing 13-arg and 15-arg test fixtures compiling unchanged.
  Production wiring (full `@Autowired` constructor) always supplies it;
  the null path is exercised by
  `traceWriterNull_perStepRecordsSilentlySkipped_botTurnUnaffected` to
  ensure the older test paths see the observation-only contract held.
- **`call_type` is always `"chat"` for the records produced by
  `AgentRunLoopImpl`** — the loop only emits chat calls. Routing /
  rerank calls live in `LlmInvocationService.invokeRouting(...)` /
  `RerankService` outside the AgentRunLoop step boundary; they are
  already labelled distinctly in the frozen `llm_call_log` table
  (`LlmCallLogger.callType`). If S3 or a future sub-sprint surfaces
  routing/rerank in the per-step trace, it should follow the same
  `call_type` vocabulary (the column accepts any string but the schema
  intent matches `llm_call_log.call_type`).
- **`projected_context` is stored as a JSON STRING** (the same shape
  `BotTurn.projected_context` uses, jsonb-typed at the DB but
  serialized to a string at the JPA boundary). The UI parses lazily on
  expand.
- **DB schema change requires a Flyway migration on existing local
  dev DBs** — `mvn spring-boot:run` will run `V15` automatically on
  startup; no manual SQL needed. A `make backend` restart picks it up.

## 7. R-items (no flips this sub-sprint)

- No R-item closed by S2 — observation-only infrastructure, no backlog
  item it directly maps to.
- No new R-item surfaced — the sub-sprint's outcome surfaces no new
  bad case or architectural finding (it is the *tool* that S3 will use
  to do the projection audit, where new R-items are expected).

## 8. OQs (open questions for deliver-agent / human at close)

- **OQ-S51.1 — Spot real-LLM trace eyeball deferred to close**: see §4
  "Spot real-LLM multi-step trace eyeball" — backend / LLM keys not
  available in the dev sandbox; deliver-agent runs the multi-step FAQ
  turn at close and records the eyeball evidence + terminal-outcome
  parity in §12. STOP-surfaced per the contract.

## 9. Layer + class

- `infra` / observability — §1.4 Runtime-owned "trace and eval
  contract".
- §7-exempt sub-sprint (pure observation; §A clarifying stanza
  included in `sprint_objective.md`).

## 10. Hard fences honored

- ✅ OBSERVATION-ONLY: no change to `AgentRunLoopImpl` loop condition,
  LLM call count, ordering, timing, or termination semantics; no
  change to `AgentRunResult` existing fields' meaning (the new
  `llmCallRecords` is additive, defaults to empty for every legacy
  call site).
- ✅ No semantic change to existing `bot_turns` columns — single
  `llm_raw_response` / `projected_context` keep the final-step value
  (verified by Java test `multiStepFaqTurn_..._finalStepValuesPreservedOnBotTurn`).
- ✅ `GET /sessions/{id}/llm-calls` endpoint + `LlmCallLogger` +
  `llm_call_log` 500-char truncation UNTOUCHED. The eval harness
  `agent_client.py::get_llm_calls` contract holds.
- ✅ `call_type` labels chat distinctly from routing/rerank (this
  sub-sprint emits only `"chat"` from the AgentRunLoop step boundary;
  routing/rerank stay in their existing per-call logger surface).
- ✅ No runtime semantic-decision change (no prompt, projection-content,
  UC routing, drift, escalation, Skill, or tool-schema change). The
  S2 capture stores the projection AS-IS — projection CONTENT is S3's
  scope.
- ✅ No scoring / eval-fixture / `composite.py` change.
- ✅ No eval-harness change beyond the read-only DTO field rename
  tolerance in the UI mapper (Python side unchanged;
  `get_llm_calls` contract preserved).

## 11. Commit discipline / staging

This sub-sprint stages ONLY S2 scope. The intended commit pulls:

- `server/src/main/java/**` (the 7 modified + 4 new source files)
- `server/src/main/resources/db/migration/V15__create_bot_turn_llm_calls.sql`
- `server/src/test/java/com/gumtree/csagent/integration/Sprint51PerInvocationTracePersistenceTest.java`
- `ui/src/types/index.ts`, `ui/src/api/client.ts`,
  `ui/src/components/admin/TraceViewer.tsx`
- NEW `docs/sprints/sprint-051-handoff.md` (this file)

Deliver-agent close-bundle files (the sprint-objective archive, the
`docs/10-handoff.md` lead, the action_bank §6 row, the milestone
objective S2 disposition) are deliberately NOT staged here; the human
bundles them at close per the existing convention. **No `git add -A`.**

## 12. Closure verdict (deliver-agent + human at close)

**Classification: A — Clean PASS** (deliver-agent ran the dev-deferred
OQ-S51.1 real-LLM trace eyeball at close → PASS; Codex DEFERRED to the
M5 milestone-shared close per `iteration_governance.md` §4.3 — S2 is
infra / observation, §7-exempt, no per-sub-sprint trigger). Closed
jointly by deliver-agent + human 2026-05-25.

### 12.1 Deliver-agent independent verification (not dev self-report)

- **Footprint** matches §5 exactly: 7 modified + 5 new server Java files
  (`BotTurnLlmCall`, `LlmCallRecord`, `BotTurnLlmCallRepository`,
  `controller/dto/BotTurnTrace`, `V15` migration) + 1 new integration
  test + 3 UI files. (`git diff --numstat` working tree.)
- **Hard fences verified**: zero `eval_interactive/` / `eval/` / `data/`
  touch; the frozen `GET /sessions/{id}/llm-calls` body is UNCHANGED
  (`getSessionLlmCalls` still returns
  `llmCallLogRepository.findBySessionIdOrderByCreatedAt(...)`); only the
  NEW `/trace` endpoint gained `List<BotTurnTrace>`.
- **Observation-only verified by diff**: `AgentRunLoopImpl`'s
  `llmCallRecords` is an append-only side-record — never read in the
  loop, never gates a branch; the `for step < maxSteps` loop condition
  is not in the diff; exactly one record per step (parser-failure /
  null-action early-returns add their record then `return`; the main
  step-boundary add sits after both), no double-count. `ControlKernel`'s
  only S2 additions are the `TraceWriter` field + constructor param + a
  null-guarded `recordLlmCalls(...)` call;
  `mergeFaqGroundingIntoProjection` / `lastProjection()` are NOT in S2's
  added lines (pre-existing — see §12.3).
- **Java suite reproduced**: `mvn test -B` →
  `Tests run: 1165, Failures: 1, Errors: 0, Skipped: 2` — matches the
  dev claim; `+2` vs the `1163` baseline = the two new Sprint51 tests;
  the lone failure is the inherited `SystemPromptUserRequestedTiebreakerTest`
  (OQ-S41.5 STATUS QUO), not an S2 regression. The 1163 run-loop tests
  pass unchanged = observation-only proven at suite level.

### 12.2 OQ-S51.1 — live real-LLM multi-step trace eyeball: **PASS**

The dev STOP-surfaced this (sandbox had no backend/LLM keys). Deliver-
agent ran it at close against the live S2 backend (PID confirmed started
after the 18:55 S2 build; Flyway `V15` applied; `bot_turn_llm_calls`
table present). Drove a real UC-A FAQ turn (session
`92a5c7c7-2054-4b54-84f0-0dc41a95703c`, real LLM `deepseek-v4-flash`),
read `GET /v1/demo/sessions/{id}/trace`, cross-checked Postgres:

- (a) ✅ turn's `llmCalls` array present, **N=2**, monotonic
  `stepIndex` 0,1, all `call_type=chat`.
- (b) ✅ each invocation carries its **own** projection + raw response —
  step0 (proj 9622 chars, raw, search tool-call, 2364ms) ≠ step1 (proj
  9745, raw, empty tool-calls = final, 1904ms); NOT all equal to the
  final step.
- (c) ✅ terminal-outcome parity: the turn produced a DISCOVER
  clarifying question; observation-only held (loop logic byte-identical;
  call count unchanged — see completeness cross-check).
- **Completeness cross-check**: the independent `llm_call_log` table
  (S2-untouched) shows `chat = 2` for the session — exactly the 2
  `bot_turn_llm_calls` rows. No missing / duplicate invocation; the
  "S2 dropped a record" failure mode is ruled out.
- **Backward-compat**: `BotTurn.llm_raw_response` == final step (step1);
  the `BotTurn` single columns behave exactly as pre-S2.

### 12.3 OQ-S51.2 (NEW, surfaced at close) — folded into S3 (no R-item)

The eyeball surfaced one projection-coherence nuance the mocked Java
test structurally could not: `BotTurn.projected_context` (10083 chars)
matches **neither** recorded invocation's projection (9622 / 9745). Root
cause traced to the pre-existing `ControlKernel.recordRunResult` path
(~`:2012`/`:2029`/`:2042`): the single-column value is
`result.lastProjection()` (= step1's raw projection) **with an
FAQ-grounding overlay merged in** via `mergeFaqGroundingIntoProjection`.
So the NEW per-step record reflects **what the LLM actually received**
(raw 9745), while the legacy single column is a post-hoc trace-merged
view (9745 + FAQ-grounding). This is **benign and pre-existing for S2**
(diff-confirmed S2 did not touch that merge) — it is NOT an S2 blocker.
Per the human's close decision (2026-05-25, `AskUserQuestion`), it is
recorded as **OQ-S51.2 and folded into S3's C1 projection consumption-
map** (no dedicated `action_bank` R-item): S3 is chartered to audit
"which projection is canonical for the admin-trace / eval-trace
contract," and this finding is adjacent to the FAQ-grounding /
`knowledge_hits` dual-path coherence question already flagged in
`milestone_objective.md` §7.

### 12.4 Baselines + gates

- **Java**: `1165 / 1-inherited / 0 / 2` (was `1163 / 1 / 0 / 2`; +2 new
  S2 tests; no NEW regression). **Python**: `3 failed, 486 passed` via
  `uv run python -m pytest` — UNCHANGED (no S2 Python touch).
- Hard gates: Java no NEW regression ✅; observation-only proven ✅;
  OQ-S51.1 eyeball PASS ✅; Tier-0 safety / grounding floor untouched
  (no semantic surface) ✅; Codex deferred to M5 milestone-shared close
  per §4.3 ✅.
- Bad-case suite regression rerun is the **M5 milestone-close** gate
  (not this S2 close) — S2 is observation-only and must not change bot
  behaviour, which the live eyeball + the unchanged call count confirm.

### 12.5 Commit / bundling note

S2 dev work is uncommitted in the working tree at close (commit-at-end).
The human commits the S2 dev scope (per §11) + the deliver-agent close
bundle (this §12, the `sprint-051-objective.md` archive, the
`10-handoff.md` §0/§1 update, the `action_bank.md` §6 row, the S3 contract
+ `compact/sprint-052-dev-prompt.md`). The prior S1 close bundle (also
uncommitted in the working tree) is bundled at the human's discretion.
Dev did not `git add -A`; deliver-agent files are bundled by the human.
