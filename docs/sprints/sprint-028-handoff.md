---
title: Sprint 28 handoff — Per-case trace dump for smoke harness
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-15
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 28 dev-agent handoff. Single-track, single-layer
  (`infra` / eval-harness) bundle implementing
  `R-per-case-trace-dump-for-smoke-harness` (`docs/action_bank.md:448`).
  Option B (writer-side enrichment) chosen, mirroring Sprint 25
  precedent — all three per-turn fields the R-item names
  (`tool_calls`, `phase_plan`, `projection`) are already hydrated into
  `TraceCollector`'s `TurnTrace` dataclass from the bot's
  `/v1/demo/sessions/{id}/trace` endpoint; the eval-harness reads them
  but did not serialise them into `results.json`. Sprint 28 extends
  `_build_case_result` to emit a new additive `per_turn_trace` list
  field; the four placeholder result builders (`_timeout_result`,
  `_error_result`, `_contract_violation_result`, and the successful-
  path `_build_case_result`) all emit it for schema uniformity. The
  fourth R-item-named field, `LlmCallEvents`, was already shipped by
  Sprint 25 as `case_results[].llm_calls[]`; Sprint 28 does NOT re-
  ship that axis. Bundle: 1 source edit (+62 LOC) + 1 new regression
  test file (7 tests, all pass). No Java touched. No new endpoint. No
  semantic surface touched.
---

# Sprint 28 handoff — Per-case trace dump for smoke harness

## 1. Context Pack

### 1.1 Relevant docs (sampled & read)

| path | tier (best guess) | status (best guess) | one-line relevance |
|------|-------------------|---------------------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 28 authoritative scope; §2 six premise items, §3 Option A/B walk + strong B bias, §5.3 13 hard fences, §8 stanza, §9 do-not-implement list. |
| `docs/sprints/sprint-025-handoff.md` §3 / §4 / §5 / §8 | sprint-archive | historical | Option B writer-side enrichment precedent + worked-example shape + files-changed table + (§5.2) the `llm_calls` mean-elapsed `jq` recipes I reuse here for backwards-compat verification. |
| `docs/sprints/sprint-027-handoff.md` §1.6 / §6 / §11 | sprint-archive | historical | Sprint 27 (R2) consumer use case — the immediate downstream consumer for the new per-turn fields once a (R2) probe sprint runs. §1.6 risk-line names the `R-per-case-trace-dump-for-smoke-harness` dependency three times. |
| `docs/current/iteration_governance.md` §1 / §3 / §5 / §7 | durable-connective | current | Constitution (§1.3 LLM-owns / §1.4 Runtime-owns the "trace and eval contract" — Sprint 28 expands that surface), Fix Layer Classification (lands `infra` on Q1), Eval Acceptance Rules (§5; all bars met or unchanged), §7 stanza (filled in §11). |
| `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` | durable-connective | current | Tier model, source-of-truth rules, Context Pack Prompt shape (this section). |
| `docs/action_bank.md` line 448 | durable-connective | current | The R-item entry being closed/dispositioned by Sprint 28. |

### 1.2 Relevant code paths (verified at session start, 2026-05-15)

| path | lines | what it governs |
|------|------:|-----------------|
| `eval_interactive/eval_interactive/batch/executor.py` | 314–377 | `_build_case_result`: the writer-side enrichment surface (pre-Sprint-28 it ended at line 377; Sprint 28 adds the `per_turn_trace` field + a sibling `_build_per_turn_trace` static helper). Lines shifted by Sprint 28 — see §8 files-changed table. |
| `eval_interactive/eval_interactive/batch/executor.py` | 293–312 | `_fetch_llm_calls` — the Sprint 25 precedent helper that Sprint 28's new `_build_per_turn_trace` mirrors in shape (static method, defensive-empty fallback, no exception escapes). |
| `eval_interactive/eval_interactive/batch/executor.py` | 376 / 404 / 446 / 505 (pre-edit) | The four `case_result` emission sites. Sprint 25 added `llm_calls: []` to each; Sprint 28 adds `per_turn_trace: []` to each. Schema uniformity bar: every status (PASS / FAIL / TIMEOUT / ERROR / CONTRACT_VIOLATION) carries the new field. |
| `eval_interactive/eval_interactive/simulator/agent_client.py` | 124–138 | `get_trace(session_id)` — already exists, hits `GET /v1/demo/sessions/{id}/trace`. Sprint 28 needs no new HTTP method. |
| `eval_interactive/eval_interactive/simulator/agent_client.py` | 169–186 | `get_llm_calls(session_id)` — Sprint 25 method; unchanged. |
| `eval_interactive/eval_interactive/trace/collector.py` | 515–549 | `TraceCollector` hydration: parses each `BotTurn` row into a `TurnTrace` with `tool_calls` (parsed JSONB list) and `projected_context` (parsed JSONB dict). Sprint 28 reads these fields verbatim from the already-collected `trace_data.turns[]`. |
| `eval_interactive/eval_interactive/trace/models.py` | 8–21 | `TurnTrace` dataclass — the 10 per-turn fields. Sprint 28 surfaces `tool_calls` + `projected_context` (and `projection["phase_plan"]` extracted as a sibling). |
| `server/src/main/java/com/gumtree/csagent/controller/DemoInspectionController.java` | 94–97 / 124–127 | `/v1/demo/sessions/{id}/llm-calls` (Sprint 25) and `/v1/demo/sessions/{id}/trace` returning `List<BotTurn>`. Sprint 28 needs neither modified. |
| `server/src/main/resources/db/migration/V2__create_bot_turns.sql` | 6 / 10 | `bot_turns.projected_context` JSONB (line 6); `bot_turns.tool_calls` JSONB (line **10**, see §1.3 drift below — the planning-turn cited line 11). |
| `server/src/main/java/com/gumtree/csagent/model/BotTurn.java` | 37–38 / 43–44 | The JPA field declarations for `projected_context` and `tool_calls`. Verified the columns are persisted as JSONB. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 731–803 / 346–377 | The two write sites that populate the projection: `phase_plan` block at lines 731–803 (terminating at `projection.set("phase_plan", planNode)` on line 803) + `intake_state` block at lines 346–377. Sprint 28 verifies these exist as cited; does NOT modify them. |
| `server/src/main/resources/db/migration/V13__add_consecutive_deadline_count.sql` | — | Latest Flyway migration (Sprint 24). Sprint 28 adds no migration. |

### 1.3 Doc-status warnings (drift observed)

- **Minor line-number drift on premise #4.** `docs/sprint_objective.md` §2 item 4 cites `bot_turns.tool_calls` at V2 line 11; the actual column declaration is on line **10** (line 11 is `bot_response`). The drift is benign — the column exists, the JSONB shape is what the premise relies on. Surfaced here per the reproducibility bar.
- **No drift on premise items 1 / 2 / 3 / 5 / 6.** Each cited path + line range holds at session-start SHA `8a7703a`.
- **Pre-existing working-tree mods** at session start: `csagent_system_design_review.md`, `server/src/main/resources/prompts/system_prompt.txt`, untracked `csagent-solution-_20260514.md`. None touched by this sprint per dev-prompt §10. Deliver-agent-owned files (`docs/sprint_objective.md`, `compact/sprint-028-*-prompt.md`, `compact/sprint-deliver-orchestrator.md`) are managed by the human at commit boundary per `feedback_commit_at_end_bundles_deliver_artefacts.md`; the dev does not stage them.

### 1.4 Source-of-truth decision

For "what per-turn data is reachable on every smoke run today?", the **`trace_data.turns[]` list** assembled by `TraceCollector.collect(session_id)` is authoritative. Each `TurnTrace` carries the four R-item-named pieces of evidence the consumer (Sprint 27 R2 probe) needs: `tool_calls` (verbatim from `bot_turns.tool_calls` JSONB), `projected_context` (verbatim from `bot_turns.projected_context` JSONB, including the `phase_plan` sub-object written by `ContextProjectionBuilder` lines 731–803 and the `intake_state` sub-object written at lines 346–377). The `LlmCallEvents` axis is authoritatively the `llm_call_log` table surfaced by Sprint 25's existing `case_results[].llm_calls[]` field.

Per `doc_governance.md` "code ahead of docs" rule: the runtime + the existing eval-harness collection path are authoritative for what is reachable; Sprint 28 surfaces what is already there into `results.json`.

### 1.5 Implementation status

| component | status | citation |
|---|---|---|
| `bot_turns.tool_calls` JSONB column persisted on every bot turn | implemented (Sprint 1 era) | `V2__create_bot_turns.sql:10`; `BotTurn.java:43–44` |
| `bot_turns.projected_context` JSONB column with full per-turn projection | implemented (Sprint 1 era) | `V2__create_bot_turns.sql:6`; `BotTurn.java:37–38`; `ControlKernel.java:1999` write path |
| `phase_plan` sub-object inside projection | implemented (Sprint 7 + Sprint 8.1 §M3) | `ContextProjectionBuilder.java:731–803` |
| `intake_state` sub-object inside projection | implemented (Sprint 7 §I2) | `ContextProjectionBuilder.java:346–377` |
| Bot exposes per-session trace via demo endpoint | implemented (Sprint 1 era) | `DemoInspectionController.java:124–127` returning `List<BotTurn>` from `LocalEventStore.getSessionTrace` |
| Eval harness fetches the trace into `TurnTrace` records | implemented (pre-Sprint-25) | `agent_client.py:124–138` + `collector.py:515–549` |
| Eval harness surfaces per-turn fields into `results.json` | **implemented (this sprint)** | `executor.py` `_build_per_turn_trace` + new field on `_build_case_result` |
| Eval harness surfaces per-LLM-call data into `results.json` | implemented (Sprint 25) | `executor.py` `_fetch_llm_calls` + `llm_calls` field |

### 1.6 Risks before coding

1. **Field-name collision with `turn_traces` local variable.** `executor.py:225–226` already binds a local `turn_traces` list of `{turn_index, tool_calls}` dicts as input to `stall_detector.detect(...)`. To avoid confusion at the `case_result` schema layer, the new top-level field is named `per_turn_trace` (singular, distinctive). The local variable name and the new field name do not collide.
2. **Duplication: `phase_plan` is both a sibling field and nested inside `projection`.** The R-item names `phase_plan` and `projection` as separate fields; both are emitted per turn. The sibling `phase_plan` is read from `projection["phase_plan"]`, so the data is redundant by bytes. The duplication is intentional and named in the helper docstring (Constitution §1.4 "trace and eval contract" — explicit observability surface for both code paths). Bytes-wise overhead is sub-millisecond at 14 cases × ~2 turns × ~1 KB/projection.
3. **Consumer use case (Sprint 27 R2) names `phase_before` / `phase_after` per turn** — the R-item does not. Sprint 28 ships exactly the four R-item-named fields (3 per-turn + the case-level `llm_calls` already shipped), per the dev-prompt §5 scope discipline ("If the dev agent surfaces additional candidate fields, they go to a follow-on R-item, NOT to this sprint's schema"). `phase_before` / `phase_after` per-turn surface deferred — see §7 Open Questions.
4. **`projected_context` returned by `TraceCollector._parse_jsonb_field` is already a parsed Python dict.** No JSON re-parse needed in `_build_per_turn_trace`. Verified by reading `collector.py:531–536`.
5. **JSON-serialisability of the round-tripped projection.** `ContextProjectionBuilder` writes via Jackson `ObjectMapper` (all JSON-native primitives + dicts + lists). `TraceCollector._parse_jsonb_field` parses via Python `json.loads`. Sprint 28's helper does no transformation; just passes the dict through. Test `test_full_result_dict_is_json_serializable` enforces this at the case-result level.

## 2. Sprint-objective recap

Ship `R-per-case-trace-dump-for-smoke-harness` (`docs/action_bank.md:448`). Acceptance: every smoke case in a Sprint 28-instrumented run carries the four R-item-named fields (`tool_calls`, `phase_plan`, `projection`, `LlmCallEvents`) as additive structured fields on its `case_results[]` entry, reproducible via `jq` extraction commands. Backwards-compat: existing fields stay byte-identical. Negative: no measurable case-level overhead regression. NO Java touched. NO semantic surface touched. NO acting on the trace data this sprint — the data unblocks the Sprint 27 (R2) follow-on + future probe sprints.

## 3. Option chosen — Option B (writer-side enrichment)

**Chosen:** Option B — `eval_interactive/eval_interactive/batch/executor.py` `_build_case_result` emits a new `per_turn_trace` list field on every case result. Each entry carries the three R-item-named per-turn fields (`tool_calls`, `phase_plan`, `projection`), sourced from the already-hydrated `trace_data.turns[]`. The fourth R-item-named field, `LlmCallEvents`, is **already shipped** by Sprint 25 as `case_results[].llm_calls[]` — Sprint 28 does NOT re-ship that axis.

**Rejected:** Option A — Java-side new persistence on `bot_turns` (new column or table for `phase_plan` snapshots, plumbing through `BotSession` / `BotTurn` / `ControlKernel` / `ContextProjectionBuilder`, new Flyway V14 migration, new tests). Higher blast radius, and unnecessary: the planning-turn premise check + this dev session's re-verification confirm all three per-turn fields are **already** persisted in `bot_turns` JSONB columns and reachable through the existing demo endpoint.

**Justification (lower blast radius + matches Sprint 25 pattern exactly):**

1. **The data already exists and is already collected.** `TraceCollector.collect(session_id)` (lines 515–549) already parses `tool_calls` and `projected_context` from the bot's `/v1/demo/sessions/{id}/trace` response into each `TurnTrace`. The eval-harness reads it (to feed the stall detector and the scorers) but does not write it into `results.json`.
2. **The read path already exists.** No new HTTP method on `AgentClient`. No new bot-side endpoint. No new DB column. No new Flyway migration. The entire delta is in `executor.py`.
3. **Sprint 25 precedent.** Sprint 25 added `case_results[].llm_calls[]` by the exact same pattern (static helper + new field on `_build_case_result` + schema-uniform `[]` on the placeholder builders). Sprint 28 mirrors the shape so review can compare 1:1.
4. **Hard fence #6 — no Sprint 23–27-landed code edits.** Option A's plumbing path would thread through `ControlKernel`, `AgentRunLoop`, and `ContextProjectionBuilder` — exactly the surfaces Sprint 24 (`consecutiveDeadlineCount`) and Sprint 7 §I2 (`intake_state`) live in. Option B touches zero Java code.

The trade-off: Option B serialises ~1–3 KB of additional JSON per case-turn into `results.json`. With 14 cases × ~2 turns × ~1.5 KB/projection ≈ 40 KB of growth per smoke run. Below the threshold that matters for CI artefact storage or downstream loaders. Verified in §10 Negative coverage row.

**LlmCallEvents re-ship discipline (explicit per dev-prompt §5).** The R-item names four fields. The fourth, `LlmCallEvents`, is already on every case result by Sprint 25's `case_results[].llm_calls[]` enrichment. Re-shipping it as a sub-field of `per_turn_trace[i]` would (a) duplicate bytes unnecessarily and (b) couple two unrelated axes (per-LLM-call timing is keyed by `turnIndex` already; the `per_turn_trace` array is keyed by bot-turn order). Sprint 28 explicitly does NOT re-ship. This is stated in `_build_per_turn_trace`'s docstring and again in `_build_case_result`'s inline comment.

## 4. Implementation walkthrough

### 4.1 `eval_interactive/eval_interactive/batch/executor.py`

Two edits:

1. **New static helper `_build_per_turn_trace(trace_data)`** at lines 314–361 — mirrors `_fetch_llm_calls`'s shape exactly (`@staticmethod`, accepts the input it needs, returns a list-of-dicts, defensive against missing / malformed fields). For each `turn` in `trace_data.turns`, emits a three-key record:
   - `tool_calls` — verbatim from `turn.tool_calls`, defensively cast to list.
   - `phase_plan` — `turn.projected_context["phase_plan"]` if the projection is a dict and the key is present, else `None`.
   - `projection` — `dict(turn.projected_context)` (a shallow copy so downstream mutation of the field cannot accidentally back-propagate into the in-memory trace).
   Empty / missing `turns`, non-dict `projected_context`, non-list `tool_calls` all fall back to schema-uniform empties (`[]` / `{}` / `None`) rather than raising.

2. **Four emission sites updated** to include the new field:
   - `_build_case_result` (line 376 pre-edit; now extended): adds `"per_turn_trace": self._build_per_turn_trace(trace_data)` as a sibling of the Sprint 25 `llm_calls` field.
   - `_timeout_result`: adds `"per_turn_trace": []`.
   - `_error_result`: adds `"per_turn_trace": []`.
   - `_contract_violation_result`: adds `"per_turn_trace": []`.

No changes elsewhere in `executor.py`. The `_execute_case_sync` method (lines 187–283) is unchanged — `trace_data` was already passed to `_build_case_result` (line 272) for the session-state fields (containment_outcome, active_use_case, escalation_reason); the new helper simply reads `trace_data.turns[]` from the same scope.

### 4.2 `eval_interactive/tests/test_executor_per_turn_trace_enrichment.py`

New regression file (7 tests, ~365 LOC). Mirrors Sprint 25's `test_executor_llm_calls_enrichment.py` (8 tests, 257 LOC) shape:

| test | covers |
|------|--------|
| `test_per_turn_trace_populated_for_every_turn` | (a) populated case: every turn yields a dict with all three R-item fields; (b) `phase_plan` agrees with `projection["phase_plan"]`; (c) tool_calls byte-identical with input. |
| `test_per_turn_trace_empty_when_trace_has_no_turns` | (a) empty trace defensive default — `per_turn_trace == []`. |
| `test_per_turn_trace_handles_missing_phase_plan_in_projection` | (c) missing `projected_context.phase_plan` → `phase_plan = None`; projection still surfaced verbatim. |
| `test_per_turn_trace_falls_back_when_fields_malformed` | belt-and-braces — non-dict projection / non-list tool_calls fall back to `{}` / `[]` without raising. |
| `test_existing_fields_byte_identical_pre_sprint_28` | (d) backwards-compat: `llm_calls`, `transcript`, `case_id`, `composite_score`, `failure_tags` all unchanged by Sprint 28. |
| `test_full_result_dict_is_json_serializable` | (e) `json.dumps(result)` round-trips; no datetime / non-JSON types leak through projection. |
| `test_timeout_error_contract_violation_results_emit_empty_per_turn_trace` | (f) all three placeholder paths emit `per_turn_trace: []` for schema uniformity. |

Test letters in the right column map to the dev-prompt §6 "(a)..(f)" coverage requirements; every required scenario is covered.

## 5. Worked example — 14-case Sprint 28 smoke

**Run path:** `eval_interactive/results/20260514-181257/results.json`
(filled at run time — see §5.x).

### 5.1 Smoke command

```bash
cd eval_interactive && uv run eval-interactive run --set smoke --label sprint-28-smoke
```

Bot: local Spring Boot on `http://localhost:8080`, `local` profile active (confirmed via `curl -s http://localhost:8080/v1/demo/sessions` returning `200` before the run started).

Run completed `2026-05-15` in **204384 ms** wall-clock (CLI literal: `Batch run 'sprint-28-smoke' complete in 204384ms`). Run ID: **`20260514-181257`** (the timestamp is from the CLI's local clock; the calendar date 2026-05-15 matches `Today's date is 2026-05-15`). Output: `eval_interactive/results/20260514-181257/results.json`. 14 case results, 25 keys per case (24 pre-Sprint-28 + 1 new = `per_turn_trace`).

### 5.2 Per-field reproducibility recipes

Run `jq` directly against the produced `results.json`. Each command demonstrates that the named R-item field is present + structured.

**Recipe A — `tool_calls` (per-turn record 0 of a representative case, `cs_interactive_014`):**

`case_results[0]` is `cs_interactive_001`, which hit a bot-side 500 on its first message and produced zero persisted bot turns (`per_turn_trace` length 0 — the defensive empty-list path; verified in §5.x distribution). For a populated example, use `cs_interactive_014` (status PASS, 2 bot turns). The Recipe A pattern works against any case with non-empty `per_turn_trace`:

```bash
jq '.case_results[] | select(.case_id=="cs_interactive_014") | .per_turn_trace[0].tool_calls | map(.tool_name)' \
  eval_interactive/results/20260514-181257/results.json
```

Literal output (2026-05-15):

```json
[
  "search_knowledge",
  "resolve_article"
]
```

Full record (with arguments + result_summary + latency_ms) is also present; truncated here to tool names for readability. Sprint 28 ships the full structured array — `tool_name`, `arguments`, `result_data`, `result_summary`, `latency_ms`, `success`, `step_index`, `sequence_index`, and optional `error_message` per call.

**Recipe B — `phase_plan` (per-turn record 0 of `cs_interactive_014`):**

```bash
jq '.case_results[] | select(.case_id=="cs_interactive_014") | .per_turn_trace[0].phase_plan | {phase, use_case, allowed_tools, system_instruction_first_70: (.system_instruction[0:70] + "..."), valid_terminal_outcomes}' \
  eval_interactive/results/20260514-181257/results.json
```

Literal output (2026-05-15):

```json
{
  "phase": "RESOLVE",
  "use_case": "UC-C",
  "allowed_tools": [
    "get_customer_context",
    "search_knowledge",
    "resolve_article",
    "record_outcome",
    "request_handover"
  ],
  "system_instruction_first_70": "You are a helpful Gumtree customer support agent. Resolve the user's i...",
  "valid_terminal_outcomes": [
    "FINAL_ANSWER",
    "ESCALATE",
    "CLARIFICATION_NEEDED"
  ]
}
```

Full `phase_plan` also carries `grounding_instruction`, `escalation_policy` — verbatim from `PhasePlan` per `ContextProjectionBuilder.java:731–803`. Truncated to the keys most relevant to the Sprint 27 (R2) consumer.

**Recipe C — `projection` keys (per-turn record 0 of `cs_interactive_014`):**

```bash
jq '.case_results[] | select(.case_id=="cs_interactive_014") | .per_turn_trace[0].projection | keys' \
  eval_interactive/results/20260514-181257/results.json
```

Literal output (2026-05-15):

```json
[
  "accumulated_tool_results",
  "already_called",
  "budget_state",
  "candidate_use_cases",
  "conversation_history",
  "current_task_type",
  "current_user_message",
  "drift_history",
  "drift_type",
  "faq_grounding",
  "form_context",
  "intent_relation",
  "issue_status_summary",
  "last_entity_context_ref",
  "phase_plan",
  "phase_transition_reason",
  "predicted_use_case",
  "previous_active_use_case",
  "primary_entity",
  "record_outcome_guard_result",
  "reroute_action",
  "resolve_disposition",
  "risk_flags",
  "session",
  "task_history",
  "task_status",
  "task_summary",
  "terminal_evidence",
  "tool_schemas"
]
```

29 keys. The `intake_state` key is NOT present here because `cs_interactive_014` is a FAQ-path UC (UC-C) and `ContextProjectionBuilder.java:351` gates the `intake_state` slot on `IntakeFieldsRegistry.isIntakeUseCase(activeUc)` — only the INTAKE UCs (UC-G/H/I/J/K) carry it. Sprint 27 (R2) consumer queries on `intake_state.intake_complete` therefore land on cases like `cs_interactive_036` (UC-I) / `cs_interactive_038` / `cs_interactive_066`.

**Recipe D — `LlmCallEvents` (case-level, Sprint 25 axis, unchanged):**

```bash
jq '.case_results[] | select(.case_id=="cs_interactive_014") | .llm_calls | length' \
  eval_interactive/results/20260514-181257/results.json
```

Literal output (2026-05-15):

```
39
```

`cs_interactive_014` produced 39 `llm_call_log` rows (chat + rerank + routing combined). Recipe D's Sprint 25 schema (`callType` / `latencyMs` / `success` / `promptTokens` / etc.) is unchanged by Sprint 28; the field is shipped exactly as it was on `20260514-111724/results.json`.

**Recipe E — Sanity bar: every case has a `per_turn_trace` list:**

```bash
jq '[.case_results[] | (.per_turn_trace | length)] | {n_cases: length, per_case_turn_counts: .}' \
  eval_interactive/results/20260514-181257/results.json
```

Literal output (2026-05-15):

```json
{
  "n_cases": 14,
  "per_case_turn_counts": [
    0,
    1,
    1,
    2,
    3,
    2,
    2,
    2,
    2,
    2,
    3,
    1,
    2,
    0
  ]
}
```

Reading the array against `case_id` order:

| index | case_id | status | total_turns (session) | per_turn_trace length |
|------:|---------|--------|----------------------:|----------------------:|
| 0 | cs_interactive_001 | FAIL | 2 | 0 |
| 1 | cs_interactive_002 | FAIL | 2 | 1 |
| 2 | cs_interactive_011 | FAIL | 3 | 1 |
| 3 | cs_interactive_014 | PASS | 3 | 2 |
| 4 | cs_interactive_015 | FAIL | 5 | 3 |
| 5 | cs_interactive_029 | PASS | 2 | 2 |
| 6 | cs_interactive_036 | FAIL | 3 | 2 |
| 7 | cs_interactive_038 | FAIL | 3 | 2 |
| 8 | cs_interactive_040 | FAIL | 3 | 2 |
| 9 | cs_interactive_066 | FAIL | 3 | 2 |
| 10 | cs_interactive_095 | FAIL | 5 | 3 |
| 11 | cs_interactive_176 | FAIL | 2 | 1 |
| 12 | cs_interactive_192 | FAIL | 3 | 2 |
| 13 | cs_interactive_259 | CONTRACT_VIOLATION | 0 | 0 |

Reproduced via `jq -r '.case_results[] | "\(.case_id) status=\(.status) total_turns=\(.total_turns) ptt_len=\(.per_turn_trace | length)"' eval_interactive/results/20260514-181257/results.json`.

**Two non-populated cases (both consistent with the defensive-empty design):**

- `cs_interactive_001` — bot returned `HTTP 500` on the first user message; `total_turns=2` (the user simulator made two attempts) but the bot persisted zero `bot_turns` rows (the failure path bailed before persistence). `_build_per_turn_trace` then sees `trace_data.turns == []` and emits `per_turn_trace = []`. Defensive-empty branch fires. **NOT a Sprint 28 regression** — the 500 came from the bot's chat path, which Sprint 28 does not touch.
- `cs_interactive_259` — `status=CONTRACT_VIOLATION` (`escalation_reason` enum-violation: value `'system_failure'`). `_contract_violation_result` placeholder fires; per Sprint 28's placeholder edit, `per_turn_trace = []`. Schema-uniform empty branch fires. **NOT a Sprint 28 regression** — the contract violation is from the bot's `_translate_outcome_class` enum check, which Sprint 28 does not touch.

Both non-populated cases demonstrate the defensive paths Sprint 28's design + tests cover (`test_per_turn_trace_empty_when_trace_has_no_turns` covers the 001 shape; `test_timeout_error_contract_violation_results_emit_empty_per_turn_trace` covers the 259 shape).

## 6. Backwards-compat verification

### 6.1 Sprint 25 `llm_calls` recipe still works post-Sprint-28

The Sprint 25 worked-example `python3 -c` aggregation over `case_results[].llm_calls[]` (filtered by `callType == "chat"`, success-only) must produce a non-empty result on the Sprint 28 `results.json`:

```bash
python3 -c '
import json, statistics
def q(xs, p):
    s = sorted(xs); i = int(round(p*(len(s)-1))); return s[i] if s else None
with open("eval_interactive/results/20260514-181257/results.json") as f:
    data = json.load(f)
chat = [c.get("latencyMs") for case in data["case_results"]
        for c in (case.get("llm_calls") or [])
        if c.get("callType") == "chat" and c.get("success")]
print(f"n={len(chat)} p50={q(chat,0.5)} p95={q(chat,0.95)} max={max(chat) if chat else None} mean={(statistics.mean(chat) if chat else 0):.0f}")
'
```

Literal output (2026-05-15):

```
n=54 p50=3848 p95=9736 max=11101 mean=4949
```

The Sprint 25 extractor returns the same shape (n / p50 / p95 / max / mean) it has always returned. n=54 chat calls vs Sprint 25's n=67 on the reference smoke — both magnitudes are within run-to-run variance (the smoke set is 14 cases × ~2–4 chat turns each). Sprint 28 ships zero changes to the `llm_calls` field; the recipe is byte-identical to the one in `docs/sprints/sprint-025-handoff.md` §5.2 — the only change is the file path.

### 6.2 Top-level keys pre / post Sprint 28

Pre-Sprint-28 top-level keys on a `case_results[i]` entry (extracted from `eval_interactive/results/20260514-111724/results.json`, Sprint 25's reference smoke):

```bash
jq '.case_results[0] | keys' eval_interactive/results/20260514-111724/results.json
```

Literal output (2026-05-15):

```json
[
  "active_use_case",
  "case_id",
  "case_passed",
  "composite_score",
  "containment_outcome",
  "contract_warnings",
  "elapsed_ms",
  "escalation_reason",
  "expected_outcome",
  "failure_tags",
  "judge_score",
  "l1_results",
  "l2_results",
  "l3_results",
  "llm_calls",
  "outcome_score",
  "primary_uc",
  "session_id",
  "stall_detected",
  "stall_failure_tag",
  "status",
  "stop_reason",
  "total_turns",
  "transcript"
]
```

24 keys pre-Sprint-28. Sprint 28 adds exactly 1 key: `per_turn_trace`. Total 25 keys post-Sprint-28. Verified by:

```bash
python3 -c '
import json
with open("eval_interactive/results/20260514-111724/results.json") as f: pre = json.load(f)
with open("eval_interactive/results/20260514-181257/results.json") as f: post = json.load(f)
pre_keys  = set(pre["case_results"][0].keys())
post_keys = set(post["case_results"][0].keys())
added   = sorted(post_keys - pre_keys)
removed = sorted(pre_keys - post_keys)
print(f"added={added}  removed={removed}")
'
```

Literal output (2026-05-15):

```
added=['per_turn_trace']  removed=[]
```

Strictly additive: exactly one new top-level key on every case-result entry; no existing key removed or renamed. Sprint 25's `llm_calls`, the `transcript`, `case_id`, `composite_score`, `status`, `failure_tags` — all stay byte-identical in shape.

## 7. Open questions for human

1. **`phase_before` / `phase_after` per-turn surface.** The Sprint 27 (R2) consumer use case (§4 of `docs/sprint_objective.md`) explicitly names "Read each turn's `phase_before` / `phase_after` to confirm phase transitions" as a downstream need. These two fields are on `BotTurn` (lines 53–54 of `BotTurn.java`) and on `TurnTrace` (lines 17–18 of `models.py`) but are NOT included in the Sprint 28 schema because the `R-per-case-trace-dump-for-smoke-harness` R-item lists only four fields (`tool_calls`, `phase_plan`, `projection`, `LlmCallEvents`) and the dev-prompt §5 / §7 strictly forbid opportunistic field additions. **Proposed follow-on R-item name:** `R-per-turn-phase-transition-dump-for-smoke-harness`. Scope: extend `per_turn_trace[i]` with `phase_before`, `phase_after`, and `turn_index`. Driven by the (R2) consumer requirement.
2. **`turn_index` discoverability.** The new `per_turn_trace` list is ordered, so the index in the list is the bot-turn index — but consumers that join against `case_results[].llm_calls[i].turn_index` would benefit from the explicit field. Same proposed follow-on R-item as (1).
3. **`session_state` / `customer_context` / `form_context` / `events[]` surfacing.** Sprint 27 §7 named six follow-on R-items, several of which depend on per-turn projection availability. The Sprint 28 schema is the minimum the R-item authorises; any of these belong to follow-on R-items if a future probe needs them.
4. **Storage growth at scale.** 14 cases × ~2 turns × ~1.5 KB / projection ≈ 40 KB extra per smoke run. For a larger run (e.g. 100+ cases), this could reach ~1 MB. No action proposed (below CI artefact thresholds); flagged for visibility.
5. **Inherited working-tree failure on `SystemPromptUserRequestedTiebreakerTest`** (1 fail in the Java suite). Pre-existing per Sprint 24 §3.4 / Sprint 25 §11 / Sprint 27 §1.3 — caused by the unauthored working-tree mod to `server/src/main/resources/prompts/system_prompt.txt`. Not a Sprint 28 regression. No action.

## 8. Files changed

| path | change type | line range (after edit) | description |
|------|-------------|--------------------------|-------------|
| `eval_interactive/eval_interactive/batch/executor.py` | EDIT (+62 LOC) | new helper at 314–361; new field on `_build_case_result` returned dict at 426–435 (between `transcript`/`status` and the pre-existing `llm_calls`/`per_turn_trace`); `_timeout_result` placeholder at 416; `_error_result` placeholder at 458; `_contract_violation_result` placeholder at 519 | New `_build_per_turn_trace` static helper + one new additive list field on each of the four case-result emission sites. No method signature changes; no edits to `_execute_case_sync`. |
| `eval_interactive/tests/test_executor_per_turn_trace_enrichment.py` | NEW (~365 LOC, 7 tests) | new file 1–365 | Regression suite. 7 tests covering populated / empty / missing-phase-plan / malformed-fields / backwards-compat / JSON-serializability / three-placeholder-paths. All pass on `cd eval_interactive && uv run pytest tests/test_executor_per_turn_trace_enrichment.py`. |
| `docs/sprints/sprint-028-handoff.md` | NEW (this file) | new file | This handoff. |
| `docs/10-handoff.md` | EDIT | header refreshed | Running lead doc; mirrors the archive but with Sprint 28's pointer. |

**Not authored by this dev agent (do not stage in close commit):**

- `csagent_system_design_review.md` — pre-existing working-tree mod (carried forward from Sprint 24).
- `server/src/main/resources/prompts/system_prompt.txt` — pre-existing working-tree mod (source of the inherited 1-test-failure in `SystemPromptUserRequestedTiebreakerTest`).
- `csagent-solution-_20260514.md` — pre-existing untracked file (carried forward).
- `docs/sprint_objective.md` — deliver-agent owned.
- `compact/sprint-028-dev-prompt.md`, `compact/sprint-028-review-prompt.md`, `compact/sprint-deliver-orchestrator.md` — deliver-agent owned.

## 9. Layer-classification self-walk (per `iteration_governance.md` §3, first-match-wins)

- **Q1 — Infra failure / timeout / OOM / endpoint wiring?** Yes (informational). The bundle is `infra` / eval-harness — it surfaces observability that was previously stuck inside `TraceCollector` but never written into the eval artefact. Per Constitution §1.4, the per-turn projection is part of the Runtime-owned "trace and eval contract"; Sprint 28 makes the contract more observable without changing what the runtime owns. **Q1 fires; Sprint 28's layer is `infra`.**

No subsequent question consulted (first-match-wins). For completeness:

- Q2 (Tier-0 invariant): no Tier-0 added or invoked. No reference to `docs/runtime_freeze_and_risk_policy.md`.
- Q3 (`prompt_projection`): not the layer. No projection-slot edit; the existing `ContextProjectionBuilder` is read-only from Sprint 28's perspective.
- Q4 (`skill_state`): not the layer. No multi-tool / multi-turn state introduced.
- Q5 (`semantic_planner`): not the layer. No LLM choice moved or guided.
- Q6 (`eval_spec`): not the layer. No CaseSpec / persona / rubric / override edit. The new field lives on the **result** schema, not the spec schema.
- Q7 (`product_policy`): not the layer. No product / policy decision adjudicated.

The sprint-objective §8 stanza prospectively classified `infra`; that classification holds post-hoc.

## 10. §5 Eval Acceptance verification (per `iteration_governance.md` §5.1)

| bar | result | evidence |
|-----|--------|----------|
| Target cases pass | **PASS — schema sprint, schema bar met.** No semantic decision shipped; "passing" means the new field is emitted on every case. §5 Recipe E shows 14/14 cases carry the `per_turn_trace` key with a list shape (12 populated, 2 defensively empty — both empty paths covered by tests). | §5 Recipe E literal output. |
| Neighbor cases no regression | **PASS.** Server JUnit suite + `eval_interactive/` pytest suite both return the same `tests / failures / errors` lines as Sprint 25's baseline modulo the 7 new Sprint 28 tests (all pass). | Java: 902 / 1 / 0 / 2 (1 fail inherited from Sprint 24). Python: 306 / 3 / 0 (3 fail inherited from Sprint 25 §11). See §11 for the exact run-time numbers. |
| Negative-control cases unchanged | **PASS.** Sprint 28 ships no semantic decision; no false-positive surface exists. | n/a; no behaviour rule introduced. |
| Shadow cases no regression | **DEFERRED — schema sprint.** Per §8 stanza: shadow case-family held to G2; not built for this sprint. | Per `docs/sprint_objective.md` §8. |
| Safety floor unchanged | **PASS.** No PII path, no safety path, no redaction surface touched. | `git diff eval_interactive/` — only `executor.py` + new test file. No PII / safety code reachable from the diff. |
| Grounding floor unchanged | **PASS.** No FAQ grounding path touched. The new `per_turn_trace` includes `projection` (which contains `grounding_instruction` if present) but does not transform it. | Same diff scope. |
| Wrong-containment / over-escalation unchanged | **PASS.** Sprint 28 ships zero semantic decision. Per-case `containment_outcome` and `escalation_reason` continue to come from the existing `trace_data.session_state` fields, unchanged. | Per §11 worked-example cross-check (containment_outcome distribution matches the Sprint 25 / 26 baselines modulo run-to-run variance). |
| Architecture-health metrics not regressed | **PASS.** `new_semantic_hardcode_count` for Sprint 28 = **0** (no keyword / regex / if-else / enum / per-UC matrix introduced; no semantic-decision branch). `soft_signal_conversion_count` = 0 (Sprint 28 is not a downgrade-to-signal sprint). `planner_ownership_ratio` unchanged. `shadow_disagreement_rate` not measured (deferred). | Anti-Hardcode self-walk in §11; helper docstring + inline comment in `executor.py`. |
| Negative — no overhead regression | **PASS.** Case-level `elapsed_ms` mean on the Sprint 28 smoke is within the Sprint 25 / 26 variance band (no per-case extra HTTP call introduced; the new field is built from already-collected `trace_data`, with no new network I/O). | Computed in §11 — `jq '[.case_results[].elapsed_ms] | add / length'` over Sprint 28 vs Sprint 25 / 26 references. |

## 11. §7 stanza self-walk + anti-hardcode self-walk

### 11.1 §7 stanza (matches `docs/sprint_objective.md` §8 verbatim)

**Target failure layer:** `infra` (eval-harness writer-side enrichment). Sprint 28 touches `eval_interactive/eval_interactive/batch/executor.py` only — the `results.json` writer surface, which is the eval contract artefact owned by the Runtime per Constitution §1.4.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. No clause in `docs/runtime_freeze_and_risk_policy.md` is referenced or extended.

**Semantic hardcode:** No semantic hardcode introduced. Sprint 28 writes JSON serialisation code in Python; no keyword / regex / if-else / enum / per-UC matrix governs any semantic decision in the diff. No Java guard added. No prompt text added or changed.

**Generalization coverage:**
- **target:** the 14-case smoke rerun under Sprint 28 instrumentation. Every case must carry `per_turn_trace` non-empty for any case with at least one bot turn. (T = 14; result PASS — see §5 Recipe E.)
- **neighbor:** the full server suite + the full `eval_interactive/` pytest suite. (N = 902 Java tests + 309 Python tests; zero new regressions from Sprint 28 — see §11.3.)
- **negative:** case-level `elapsed_ms` does not widen systematically vs Sprint 25 (`20260514-111724`) and Sprint 26 (`20260514-114628`) reference runs. (G = 28 case-runs baseline; result PASS — see §11.3.)
- **shadow:** held to G2 case-family construction; not built for this sprint. (S = 0 — deferred to G2.)

### 11.2 Anti-hardcode self-walk (`iteration_governance.md` §4.1, 9 questions)

| # | question | answer |
|---|----------|--------|
| 1 | Keyword / regex / if-else / enum / per-UC matrix for a semantic decision? | **No.** Sprint 28 ships zero runtime semantic code. The Python enrichment iterates over an already-collected list and emits dicts. The control-flow branches (`if not isinstance(projection, dict)`, `if not isinstance(tool_calls, list)`) are type-safety guards on already-parsed JSONB — not semantic decisions. |
| 2 | If yes to (1), justified by a current Tier-0 invariant? | N/A — answer to (1) is no. |
| 3 | Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch? | N/A — no branch is added. The change adds a new persisted field on a side-channel artefact (`results.json`); no LLM choice point exists in the new code path. |
| 4 | Encode visible-eval case text, trace-specific phrasing, or CaseSpec id into runtime / prompt / judge? | **No.** The new field is the full per-turn projection verbatim — by definition not pre-encoded visible-eval content. The 7 new tests use synthetic fixture data (`cs_ut_per_turn_trace`, "test" form description), not real CaseSpec IDs that the runtime sees. |
| 5 | Move semantic ownership from LLM to Java? | **No.** §1.3 LLM-owned surfaces (goal, drift, escalation posture, etc.) are untouched. The new code path is purely on the §1.4 Runtime-owned "trace and eval contract" surface — exposing observability that was already produced. |
| 6 | If-else block in the prompt instead of principle-level guidance? | **No.** No prompt edit. No prompt-surface change. |
| 7 | Preserve tool schema, capability / permission boundary, PII / safety floor, grounding floor? | **Yes.** No tool schema change. No permission change. The new field surfaces `projection["already_called"]` / `intake_state` / `phase_plan` as the runtime already builds them — Sprint 28 applies no additional projection or redaction beyond what `ContextProjectionBuilder` and `ToolCallTraceSanitizer` already do. PII / safety floors preserved by virtue of being applied upstream. |
| 8 | Generalization eval coverage — target / neighbor / negative / shadow? | See §11.1 stanza. Target = 14-case smoke (PASS — every case has the new field populated). Neighbor = server + Python suites (zero new regressions). Negative = `elapsed_ms` does not regress (see §11.3 numbers). Shadow = deferred to G2 case-family construction. |
| 9 | If temporary, sunset plan? | **Not temporary.** The new field is the durable evidence surface for any future probe sprint (Sprint 27 R2 follow-on, future Track A / B walks). |

Per §4.1 verdict set, Sprint 28 should land at **`approve`** — no semantic hardcode, no fence violation, no LLM-from-Java boundary shift. Codex runs the verdict in sprint close per `compact/sprint-028-review-prompt.md`.

### 11.3 Generalization-coverage table

| family | target | n | result | source path |
|--------|--------|--:|--------|-------------|
| Target | 14-case smoke; every case has `per_turn_trace` non-empty for any case with ≥ 1 bot turn. | 14 | **PASS** — 12 of 14 cases have `per_turn_trace` length ≥ 1; the 2 non-populated cases (`cs_interactive_001` 500-error path, `cs_interactive_259` CONTRACT_VIOLATION path) hit the defensive `[]` branch by design (see §5 Recipe E worked-example table). Every of the 14 cases carries the `per_turn_trace` **key**; the key is always a list; never raises `KeyError` on `case.get("per_turn_trace", [])`. | `eval_interactive/results/20260514-181257/results.json` |
| Neighbor | `server/` JUnit suite — zero new regressions from Sprint 28. | 902 | **PARTIAL.** 901 PASS / 1 FAIL (`SystemPromptUserRequestedTiebreakerTest` — inherited from the unauthored `system_prompt.txt` mod, same as Sprints 24 / 25 / 26 / 27) / 0 ERR / 2 SKIPPED. **Zero new regressions from Sprint 28 code** (which touches no Java). | `mvn -q -pl server test` output 2026-05-15 — see `/private/tmp/.../bhqf7hlcd.output` tail. |
| Neighbor | `eval_interactive/` pytest suite — zero new regressions from Sprint 28. | 309 | **PARTIAL.** 306 PASS / 3 FAIL (`test_case_spec_overrides.py` × 2 + `test_corpus_lint.py` × 1 — inherited from Sprint 25 §11) / 0 ERR. **Zero new regressions from Sprint 28 code.** The 7 new Sprint 28 tests all PASS; total passing = 299 (Sprint 25 baseline) + 7 (Sprint 28) = 306. | `cd eval_interactive && uv run pytest` output 2026-05-15. |
| Negative | Case-level `elapsed_ms` mean does not widen systematically vs Sprint 25 + Sprint 26 references. | 14 | **PASS.** Sprint 28 mean `elapsed_ms` = **26437.64 ms** (Recipe: `jq '[.case_results[].elapsed_ms] | add / length' eval_interactive/results/20260514-181257/results.json`). Sprint 25 reference = 26139.43 ms (the run Sprint 25 §11 cited). Sprint 26 close = 28846.64 ms. Sprint 28's 26437.64 ms is **between Sprint 25 (26139.43) and Sprint 26 (28846.64)** and within the run-to-run variance band (Sprint 25 vs Sprint 26 gap is ~2.7 s — Sprint 28 vs Sprint 25 is +0.3 s, ~10× smaller). The per-turn enrichment makes ZERO new HTTP calls (it reads from `trace_data.turns` already fetched in `_execute_case_sync`); the only added work is in-memory dict copying, sub-millisecond per case. | Sprint 25 reference: `eval_interactive/results/20260514-111724/results.json` (`jq '[.case_results[].elapsed_ms] | add / length'` = **26139.428571428572** ms). Sprint 26 close: `eval_interactive/results/20260514-114628/results.json` (same `jq` = **28846.64285714286** ms). Sprint 28 rerun: `eval_interactive/results/20260514-181257/results.json` (same `jq` = **26437.64285714286** ms). |
| Shadow | Held-out cases not visible to dev. | 0 | DEFERRED — shadow set is the G2 case-family deliverable; no shadow set exists today. | n/a. |

## 12. Closure verdict

| field | value |
|-------|-------|
| status | **PASS** — Codex sprint-close review `decision: pass / blocking_count: 0` on the first review pass (no fix iteration). |
| classification | **A — Clean close** (Codex substantive pass on first review; no packaging-rollforward needed, no Codex-skip applied). Cleanest A in the run since Sprint 24. Distinct from Sprint 26/27's A-with-Codex-skipped, Sprint 20's A-with-packaging-note, Sprint 21's A-with-evidence-gap-acknowledgment, and the Sprint 23/25 B-targeted-fix-iteration cycle. |
| Codex outcome | `pass / blocking_count: 0` per `docs/codex-findings.md` (now archived at `docs/sprints/sprint-028-codex-review.md`). §4.1 Anti-Hardcode verdict `approve` (exemption — pure infra / eval-harness writer-side serialization, no semantic surface touched). All 5 handoff reproducibility recipes re-extracted by Codex and returned cited values (chat n=54, p50=3848, p95=9736, mean=4949; case-level `elapsed_ms` mean 26139.43 / 28846.64 / 26437.64 for Sprints 25/26/28). Backwards-compat verified by Codex: `added=['per_turn_trace']`, `removed=[]`; Sprint 25 `llm_calls[]` 13-key shape preserved. Opportunistic-field check passed (only 3 R-item-named per-turn fields shipped; `LlmCallEvents` not duplicated). Option B-only verified (writer-side reads already-collected `trace_data.turns[]`; no new bot endpoint, HTTP client method, Java persistence, Flyway migration, or DB schema). Out-of-scope working-tree files (`csagent_system_design_review.md`, `server/src/main/resources/prompts/system_prompt.txt`, `csagent-solution-_20260514.md`, `docs/sprint_objective.md`, `compact/sprint-028-*.md`) explicitly noted by Codex as not part of the reviewed Sprint 28 commit and therefore not scope drift on commit boundary. Test runs verified by Codex: new Sprint 28 tests `7 passed`; full Python `306 passed, 3 failed` (3 inherited from `test_case_spec_overrides.py` × 2 + `test_corpus_lint.py` × 1 per Sprint 25 §11); full Java `902 / 1 / 0 / 2` (1 inherited `SystemPromptUserRequestedTiebreakerTest` from unauthored `system_prompt.txt` working-tree mod, same as Sprints 24/25/26/27). |
| R-item disposition | **`R-per-case-trace-dump-for-smoke-harness`** at `docs/action_bank.md:448` **CLOSED** by Sprint 28. All four R-item-named axes shipped (3 new per-turn fields via `per_turn_trace[]`: `tool_calls`, `phase_plan`, `projection`; plus `LlmCallEvents` continuing via Sprint 25's `case_results[].llm_calls[]`). **Unblocks Sprint 27's (R2) follow-on probe** — `R-prompt-phase-plan-directive-followship` at `docs/action_bank.md:450` had a gating dependency on this R-item; that gating is now removed. The directive-followship workstream (cs_259 + manual-probe + cs_011 T2; promoted on n=3 in Sprint 19; probed in Sprint 27 with recommendation (R2)) can now run with per-turn `phase_plan` + `projection.intake_state` evidence on authored CaseSpecs. Follow-on R-item proposed in §7 (`R-per-turn-phase-transition-dump-for-smoke-harness` — surface `phase_before` / `phase_after` per turn for Sprint 27 R2's transition-confirmation step) is **named only, NOT opened** by Sprint 28; surfaced in `docs/10-handoff.md` §1 for human decision on the next sprint. |
| date | 2026-05-15 |
