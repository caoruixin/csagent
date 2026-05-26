# Sprint 51 / M5 S2 — Per-invocation trace (B2) — Dev Implementation Prompt

You are the dev agent for **Sprint 51 / S2**, the SECOND sub-sprint of **Milestone
M5 — Observability Coherence**. Goal: persist + expose + render **every** LLM
invocation in a turn (not just the last), so the admin trace shows "Invocation
1..N" each with its OWN projected context + full raw response. This is the
prerequisite tool for S3 (the projection audit).

**This is OBSERVATION-ONLY.** You are adding a side-record at the existing run-loop
step boundary. You must NOT change the loop's control flow, LLM call count,
timing, or termination semantics. Read this prompt + the contracts in §1 before
writing any code.

Storage shape is **decided** (human, 2026-05-24): a **NEW `bot_turn_llm_calls`
table** (one-to-many under `bot_turns`). NOT a `BotTurn.llm_calls` jsonb column;
NOT an extension of `llm_call_log`.

## 1. Read order (cold start)

1. `AGENTS.md` (auto-loaded constitution chain). Do not re-read if in context.
2. `docs/solutions/observability_coherence_admin_trace_and_projection.md` —
   **READ §2.B (the data-flow + the exact line numbers), §4.B (B2 design), §8
   (#2 fences), §9 (Risk A storage / Risk on routing-turn_index), §10 (truncation
   note).** This is the detail design.
3. `docs/sprint_objective.md` — the Sprint 51 contract (numbered scope #1-#6 is
   binding).
4. `docs/milestone_objective.md` §3 S2 + §6 hard fences (S2 row).
5. `docs/10-handoff.md` §0 — baselines (Java `1163 / 1-inherited / 0 / 2`;
   Python `3 failed / 486 passed`).

## 2. The current data flow (verified — your starting map)

- `AgentRunLoopImpl.run()` — `for (step = 0; step < maxSteps; step++)` loop
  (~`:170`). Each step: builds projection → `lastProjection = projection`
  (~`:189`, **overwrite**); calls `llmInvocation.invokeChat(...)` (~`:201`);
  `lastLlmRawResponse = response.getContent()` (~`:221`, **overwrite**); appends a
  `LlmCallEvent` to `llmEvents` (~`:224-227`). At loop end, `AgentRunResult`
  carries `lastProjection` + `lastLlmRawResponse` (final step only); `llmEvents`
  is on the result but `TraceWriter.recordTurn(...)` does NOT receive it.
- `LlmCallEvent` (`model/LlmCallEvent.java`) stores only a `responseSummary`
  truncated to 500 chars (`of()` ~`:44`) — no projection, not full raw.
- `TraceWriter.recordTurn(...)` (`TraceWriter.java` ~`:27-52`) writes ONE
  `BotTurn` per turn with ONE `llmRawResponse` + ONE `projectedContext`.
- `BotTurn` (`model/BotTurn.java`) — single `llm_raw_response` (~`:41`) + single
  `projected_context` jsonb (~`:38`) columns.
- `DemoInspectionController` — `GET /sessions/{id}/trace` (~`:124-126`) returns
  `List<BotTurn>`; `GET /sessions/{id}/llm-calls` (~`:94`) returns per-call
  `llm_call_log` rows (**eval harness consumes this — frozen**).
- UI: `ui/src/types/index.ts` `TraceStep.llm_raw_response?` / `projected_context?`
  (~`:63-64`, single values); `TraceViewer.tsx` `LlmDetailPanel` (~`:99-205`)
  renders the single values.

So the gap: per-step raw + projection are OVERWRITTEN, never persisted per-step;
`llm_call_log` has per-call rows but truncated + no projection. You add a
full-fidelity per-step store.

## 3. Items (per `docs/sprint_objective.md` #1-#6)

- **#1 New table `bot_turn_llm_calls`** — Flyway migration (next version under
  the server migration dir; the `llm_call_log` table was `V9` — find the current
  latest and increment) + JPA entity + repository. FK → `bot_turns(id)`. Columns:
  `id`, `bot_turn_id`, `step_index`, `call_type` (`chat`/`routing`/`rerank`),
  `llm_raw_response` (full text), `projected_context` (jsonb, the step's
  projection), `tool_calls` (jsonb), `latency_ms`, `model`, `created_at`.
- **#2 Capture per-step in `AgentRunLoopImpl`** — accumulate the full per-step
  record at the existing step boundary (enrich the `llmEvents` capture or add a
  parallel accumulator). **Do NOT alter the loop condition, call count, ordering,
  timing, termination, or `AgentRunResult` existing fields.**
- **#3 Persist via `TraceWriter`** — thread the accumulated records into the
  trace-write path; write N rows to `bot_turn_llm_calls` keyed to the `BotTurn`.
  **`BotTurn`'s existing single columns keep the final-step value (unchanged
  semantics).**
- **#4 Trace endpoint** — nest the per-invocation records under each turn in
  `/trace` (fine for admin/demo traffic) OR add a sibling fetch. **Do NOT touch
  `GET /sessions/{id}/llm-calls` or its payload** (frozen eval contract).
- **#5 UI** — `TraceStep.llm_calls?: LlmCall[]`; `TraceViewer` renders
  "Invocation 1..N" per turn, each expandable to its own projection + raw
  response; **default-collapsed**. Keep the final-step view.
- **#6 Tests** — Java persistence (N rows, monotonic `step_index`, correct
  `call_type`) + observation-only proof (existing run-loop tests unchanged;
  `AgentRunResult` unaffected) + `BotTurn` final-step columns intact. UI render
  test or typed-fixture + eyeball.

## 4. Hard fences (from `docs/sprint_objective.md` §"Hard fences" — DO NOT VIOLATE)

- **OBSERVATION-ONLY**: no `AgentRunLoopImpl` control-flow / call-count /
  timing / termination / `AgentRunResult` change.
- No semantic change to existing `bot_turns` columns (new data → new table only).
- Do NOT change `GET /sessions/{id}/llm-calls` or `LlmCallLogger` /
  `llm_call_log` truncation (cheap summary stays; full fidelity is the new table).
- `call_type` labels routing/rerank distinctly — never as `chat`.
- No runtime semantic-decision change. **Projection CONTENT is S3's scope** — S2
  stores the projection as-is; it does not change what is projected.
- No scoring / eval-fixture / `composite.py` change. No eval-harness change
  beyond a read-only addition (if any) that leaves `get_llm_calls` untouched.
- **STOP and surface** if per-step persistence cannot be done without changing
  loop control flow / `AgentRunResult` (breaks observation-only), or if the
  migration conflicts with an in-flight schema change.

## 5. §4.1 anti-hardcode self-walk (sub-sprint is §7-EXEMPT)

S2 is `infra` / observation — §7-exempt, no semantic surface. Walk the §4.1
9-question kernel; capture in handoff §3; **expected: clean `approve`** (name the
exemption: pure observation record + persistence + rendering). Key: Q1 — no
keyword/regex/enum, no semantic branch added; Q5 — no semantic ownership moves
LLM → Java (you only *record* what the loop already does); Q7 — tool schema /
PII / grounding floors untouched.

## 6. Tests to run

- **Java**: `./gradlew :server:test` — no NEW regression vs `1163 / 1-inherited
  / 0 / 2` (count grows with new S2 tests; inherited tiebreaker failure persists).
  Existing run-loop tests passing unchanged = the observation-only proof.
- **Spot real-LLM** (needs `make backend` + LLM keys): one multi-step FAQ turn →
  admin trace shows N invocations each with own projection + raw; terminal outcome
  unchanged vs pre-S2. Record in handoff. (Backend/keys unavailable → STOP and
  surface.)
- **Python**: `cd eval_interactive && uv run python -m pytest --tb=no -q` — no NEW
  regression vs `3 failed / 486 passed` (use `python -m pytest`, not the segfaulting
  console script).
- Full bad-case regression rerun = M5 milestone-close gate, NOT this close.

## 7. Handoff + bundle

- Produce `docs/sprints/sprint-051-handoff.md`; **§12 reserved** (deliver-agent +
  human at close).
- Record: `git show --numstat`; migration version + new entity/repo/controller;
  observation-only evidence (run-loop tests unchanged + spot real-LLM multi-step
  trace eyeball with unchanged terminal outcome); §4.1 self-walk.
- Numbers from reproducible commands. **Stage ONLY S2 scope** (`server/**`,
  `server/src/main/resources/db/migration/**`, `ui/src/**`, new tests, NEW
  `sprint-051-handoff.md`); **no `git add -A`**; enumerate in the commit message.

## 8. Self-check (before claiming complete)

- [ ] Read the obs proposal §2.B/§4.B/§8 + understood OBSERVATION-ONLY?
- [ ] New `bot_turn_llm_calls` table (migration + entity + repo); FK → bot_turns;
  full untruncated raw + per-step projection jsonb + call_type?
- [ ] Per-step capture rides the EXISTING loop boundary — loop condition / call
  count / timing / termination / `AgentRunResult` UNCHANGED?
- [ ] `BotTurn` existing columns keep the final-step value (unchanged semantics)?
- [ ] `/sessions/{id}/llm-calls` + `llm_call_log` truncation UNTOUCHED (frozen
  eval contract)?
- [ ] routing/rerank labelled distinctly by `call_type`?
- [ ] UI renders Invocation 1..N, each expandable to own projection + raw,
  default-collapsed; final-step view preserved?
- [ ] Java: persistence test (N rows) + observation-only proof (run-loop tests
  unchanged) PASS; no NEW Java regression?
- [ ] Spot real-LLM multi-step turn: N invocations shown, terminal outcome
  unchanged (or STOP-surfaced if backend unavailable)?
- [ ] §4.1 self-walk clean approve (§7-exempt observation)?
- [ ] Handoff §1-§11 complete; §12 reserved; staged only S2-scope files?

If any checkbox is unchecked, surface to deliver-agent BEFORE declaring complete.
The observation-only invariant (the loop behaves identically; you only record
more) is the close gate alongside the Java baseline.
