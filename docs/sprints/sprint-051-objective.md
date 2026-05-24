---
title: Sprint objective — Sprint 51 / M5 S2 — Per-invocation trace (B2 full persistence)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-24
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-050-objective.md]
superseded_by: null
notes: >
  DRAFT pending human approval (2026-05-24). SECOND sub-sprint of Milestone M5 —
  Observability Coherence. Promotes per-invocation LLM data to a first-class
  persisted observation so the admin trace can render every loop step's own
  projection + raw response (today only the LAST step survives the BotTurn
  single-column overwrite). Human chose the **new `bot_turn_llm_calls` table**
  storage shape (2026-05-24) over BotTurn.llm_calls jsonb / extending
  llm_call_log. infra / §1.4 Runtime-owned trace contract; §7-exempt (pure
  observation) but touches server runtime → carries a clarifying stanza.
  **OBSERVATION-ONLY: no change to the run loop's control flow, LLM call count,
  timing, or termination semantics.** Hard ordering: S2 must land before S3 (the
  projection audit needs per-invocation projections visible in the trace).
  Whole-solution proposal: docs/solutions/observability_coherence_admin_trace_and_projection.md
  §2.B / §4.B (B2) / §8 (#2 fences).
---

# Sprint 51 / M5 S2 — Per-invocation trace (B2 full persistence)

## Class

`infra` — observability / trace contract (§1.4 Runtime-owned "trace and eval
contract"). **§7-exempt**: pure observation; adds no prompt / runtime semantic
decision / eval-spec / judge-calibration change. A short anti-hardcode stanza is
included at §A because the sub-sprint touches `server/` runtime code (the run
loop + trace-write path) and a Flyway migration.

## Goal

A human opening the admin trace for a multi-step turn (FAQ Skill,
`max_tool_steps: 4`) sees **every** LLM invocation — each with its **own**
projected context + full raw response + tool calls + latency — rendered as an
"Invocation 1..N" list, instead of only the final step's values (today
`AgentRunLoopImpl` overwrites `lastLlmRawResponse`/`lastProjection` each step and
`TraceWriter.recordTurn` persists only the final one into `BotTurn`'s single
columns). Full-fidelity per-invocation records live in a NEW
`bot_turn_llm_calls` table, one-to-many under `bot_turns`. This is the
prerequisite tool for S3's projection consumption-map.

## Scope (numbered; this is the contract)

**#1 — New persistence: `bot_turn_llm_calls` table.** Flyway migration (next
version after the current latest under the server migration dir) + JPA entity +
repository. One-to-many FK → `bot_turns(id)`. Columns (final names dev's
discretion): `id`, `bot_turn_id` (FK), `step_index` (int; 0-based loop step),
`call_type` (`chat` / `routing` / `rerank` — from the existing `LlmCallLogger`
callType vocabulary; do NOT lump routing/rerank into chat), `llm_raw_response`
(text, **full / untruncated** — distinct from `llm_call_log`'s 500-char
summary), `projected_context` (jsonb — **that step's** projection),
`tool_calls` (jsonb), `latency_ms`, `model`, `created_at`.

**#2 — Capture per-step in `AgentRunLoopImpl`.** Accumulate, per loop step, the
full `{raw response, that step's projection, tool_calls, latency, step_index,
call_type}`. The loop already accumulates a per-step `llmEvents` list
(`LlmCallEvent` at ~`:224-227`) but it is truncated-summary-only and carries no
projection — enrich that capture OR add a parallel full-fidelity accumulator.
**OBSERVATION-ONLY: do NOT change the loop condition (`for step < maxSteps`),
the number of LLM calls, the call ordering/timing, the termination semantics
(final/escalate/maxSteps), or `AgentRunResult`'s existing fields.** The capture
is a side-record at the existing step boundary.

**#3 — Persist via the trace-write path.** Thread the accumulated per-step
records to `TraceWriter` (extend `recordTurn(...)` or add a sibling method) and
write them to `bot_turn_llm_calls` keyed to the `BotTurn` being written.
**`BotTurn`'s existing `llm_raw_response` + `projected_context` single columns
are UNCHANGED in semantics** — they keep carrying the final-step value exactly
as today (backward-compat for every existing reader of `bot_turns`).

**#4 — Expose via the trace endpoint.** `DemoInspectionController` `/trace`
(~`:124-126`, returns `List<BotTurn>`) gains the per-invocation records nested
under each turn (acceptable for an admin/demo trace given low traffic), OR a
sibling fetch (`/sessions/{id}/turns/{idx}/llm-calls`) if you prefer to keep
`/trace` lean. **Do NOT change the existing `GET /sessions/{id}/llm-calls`
endpoint or its payload** — the eval harness (`agent_client.py::get_llm_calls`
→ `executor.py` `case_results[].llm_calls`) consumes it; it is a frozen contract.

**#5 — Render in the UI.** `ui/src/types/index.ts` `TraceStep` (~`:63-64`) gains
an optional `llm_calls?: LlmCall[]` (each: `step_index`, `call_type`,
`llm_raw_response`, `projected_context`, `tool_calls`, `latency_ms`).
`TraceViewer.tsx` `LlmDetailPanel` (~`:99-205`) renders an "Invocation 1..N"
list per turn, each invocation expandable to its OWN projected context + raw
response. **Default-collapsed** (the per-step projections are heavy). The
existing single-value `step.llm_raw_response` / `step.projected_context`
rendering may stay as a "final invocation" summary or be subsumed by the list —
dev's discretion, but do not lose the final-step view.

**#6 — Tests.** Java: (a) persistence — a multi-step turn writes N
`bot_turn_llm_calls` rows with monotonic `step_index` + correct `call_type`
labels; (b) **observation-only proof** — the existing `AgentRunLoopImpl` /
run-loop tests pass unchanged (the loop's decisions, call count, and
`AgentRunResult` are byte-for-byte unaffected by the side-record); (c)
`BotTurn`'s existing columns still carry the final-step value. UI: a render test
for the Invocation list if the UI test harness supports it (else a typed
fixture + manual eyeball, recorded in the handoff).

## Hard fences / STOP conditions (do NOT do)

- **OBSERVATION-ONLY**: no change to `AgentRunLoopImpl`'s loop condition, LLM
  call count, call ordering, timing, or termination semantics; no change to
  `AgentRunResult`'s existing fields/meaning.
- No semantic change to existing `bot_turns` columns — `llm_raw_response` /
  `projected_context` keep the final-step value (new data goes ONLY in the new
  table).
- Do NOT change the existing `GET /sessions/{id}/llm-calls` endpoint or payload
  (eval `get_llm_calls` contract is frozen). Do NOT change `LlmCallLogger` /
  `llm_call_log` truncation (keep it the cheap 500-char summary; full fidelity
  lives in the new table).
- Routing / rerank invocations labelled by `call_type`; never混入 / mislabelled
  as `chat`.
- No runtime semantic-decision change (no prompt, projection-content, UC
  routing, drift, escalation, Skill, or tool-schema change) — S2 only *records*
  what the loop already produces. **Projection CONTENT is S3's scope, not S2's**;
  S2 captures-and-stores the projection as-is, it does not alter what is projected.
- No scoring / eval-fixture / `composite.py` change.
- **STOP and surface to deliver-agent** if persisting the per-step record cannot
  be done without changing the loop's control flow or `AgentRunResult` (that
  would break the observation-only invariant), or if the migration conflicts
  with an in-flight schema change.

## Test / eval requirements

- **Java**: `./gradlew :server:test` — **no NEW regression** vs the
  `1163 / 1-inherited / 0 / 2` baseline (test count grows with the new S2 tests;
  the inherited `SystemPromptUserRequestedTiebreakerTest` failure persists per
  OQ-S41.5). The existing run-loop tests passing unchanged IS the primary
  observation-only proof.
- **Spot real-LLM check** (needs backend up + LLM keys): run one multi-step FAQ
  turn; confirm the admin trace shows N invocations (N = the loop steps, up to 4)
  each with its own projection + raw response, AND the turn's terminal outcome is
  unchanged vs a pre-S2 run of the same input. Record in the handoff. (If backend
  / keys unavailable, STOP and surface — the Java observation-only tests are the
  logic proof but the contract wants the live trace eyeball.)
- **Python**: `cd eval_interactive && uv run python -m pytest --tb=no -q` — no
  NEW regression vs `3 failed, 486 passed` (S2 should not touch eval-harness
  code; if `agent_client.py` needs a read-only addition for the nested records,
  keep `get_llm_calls` untouched).
- The full **bad-case suite regression rerun is the M5 milestone-close gate**,
  not an S2-close gate (S2 is observation-only; it must not change bot behaviour,
  so the distribution is expected unchanged).

## Handoff requirements

- Author `docs/sprints/sprint-051-handoff.md`; leave **§12** empty (deliver-agent
  + human at close).
- Record: `git show --numstat`; the migration version + the new entity/repo; the
  observation-only evidence (run-loop tests unchanged + the spot real-LLM
  multi-step trace eyeball); the §A self-walk.

## Commit discipline

Dev stages **only S2 scope**: `server/src/main/java/**` (run loop + trace writer
+ entity + repository + controller), `server/src/main/resources/db/migration/**`
(the new migration), `ui/src/**` (types + TraceViewer), the new Java/UI tests,
and NEW `docs/sprints/sprint-051-handoff.md`. **No `git add -A`** — deliver-agent
close-bundle files are bundled by the human at close.

## Codex review plan (§4.3)

DEFERRED to the M5 milestone-shared close per §4.3 default — S2 is infra /
observation, §7-exempt, with no per-sub-sprint trigger (no Tier-0 candidate, no
§1.7 cross, no hard-fence violation). The "observation-only, no loop behaviour
change" property is verified by the Java test suite (existing run-loop tests
unchanged) at this close, and re-verified by the milestone-shared Codex pass
over the cumulative range at M5 close. (Contrast S3, which DOES get a
per-sub-sprint Codex per `milestone_objective.md` §8.)

## §A — Anti-hardcode stanza (sub-sprint is §7-exempt; included for Codex)

**Target failure layer:** `infra` (observability / trace contract, §1.4
Runtime-owned).

**Tier-0 invariant:** adds no Tier-0 invariant. Adds per-invocation observation
records + their persistence/exposure/rendering; does not change runtime
decisions, the PII / safety floor, the grounding floor, the LLM call count, or
the loop timing/termination.

**Semantic hardcode:** none. New `bot_turn_llm_calls` table + per-step capture +
trace-endpoint exposure + UI list; zero keyword / regex / enum; no change to
`AgentRunLoopImpl`'s control flow (capture rides the existing step boundary).

**Generalization coverage:** observation-face change, no semantic decision.
target = any multi-invocation turn (FAQ Skill `max_tool_steps=4`) shows N
invocation records; neighbor = a single-invocation turn shows 1 (and the
final-step view is unchanged); negative / shadow = N/A (no bot behaviour / judge
change). Java baseline no NEW regression; the run-loop tests passing unchanged is
the observation-only proof.

## OQ (open questions — filled during the sub-sprint)

- _none yet_
