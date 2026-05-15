---
title: Sprint 28 — Per-case trace dump for smoke harness
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-05-15
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Single-track, single-layer (`infra` / eval-harness) sprint shipping
  `R-per-case-trace-dump-for-smoke-harness` (`docs/action_bank.md:448`).
  Re-enables per-case JSON trace dumping (`tool_calls`, `phase_plan`,
  `projection`, `LlmCallEvents`) under each
  `eval_interactive/results/<run-id>/` directory so the Sprint 27 (R2)
  follow-on, future Track A §3.2 walks, and Track B per-step
  investigations have first-party evidence. Option B (writer-side
  enrichment) is the strongly biased path per Sprint 25 precedent —
  the planning-turn premise check found ALL four named fields already
  exist queryable in `bot_turns` (`tool_calls`, `projected_context`
  containing `phase_plan` + `intake_state`, plus existing `llm_calls`
  for the LlmCallEvent axis); the `/v1/demo/sessions/{id}/trace`
  endpoint already returns the full hydrated `List<BotTurn>`. Sprint
  28 is therefore a pure writer-side serialization step on top of
  data the bot already produces. NO acting on the trace data this
  sprint. NO Sprint 23-27-landed code touched. NO semantic surface
  touched.
---

# Sprint 28 — Per-case trace dump for smoke harness

## 1. Goal

Re-enable per-case JSON trace dumping for the eval-interactive smoke
harness so the four R-item-named fields — `tool_calls`, `phase_plan`,
`projection`, `LlmCallEvents` — appear as additive structured fields
on every case result inside `eval_interactive/results/<run-id>/results.json`.
This unblocks the Sprint 27 (R2) follow-on (which is explicitly
gated on this R-item landing first — Sprint 27 handoff §6 names the
gating three times; see also handoff §1.6 / §11) AND broadens the
eval-harness observability surface for any future probe sprint.

Land **Option B (writer-side enrichment)** as the strong default per
Sprint 25 Track B precedent. The planning-turn premise check
established that all four named fields are already persisted in
runtime state and reachable through existing demo endpoints — see
§3.

## 2. Premise carried forward (verified at planning turn 2026-05-15)

Six items verified before this objective was drafted; each holds.
Sprint 28 does NOT re-derive these in the dev session — they are
fact for the sprint.

1. **`eval_interactive/eval_interactive/batch/executor.py`
   `_build_case_result`** — verified at lines 314–377. Sprint 25
   added `llm_calls` field at line 376 via the `_fetch_llm_calls`
   helper (lines 293–312). The new per-turn-trace field slots in
   beside it as an additive list field.
2. **`eval_interactive/eval_interactive/simulator/agent_client.py`** —
   verified. Existing `get_trace(session_id)` at lines 124–138 returns
   `List<BotTurn>` from `GET /v1/demo/sessions/{id}/trace`; existing
   `get_llm_calls(session_id)` at lines 169–186 (Sprint 25). Each
   `BotTurn` already carries `tool_calls`, `projected_context` (which
   contains `phase_plan` and `intake_state`), `phase_before`,
   `phase_after`, `latency_ms`. The fetch helper does not need a new
   HTTP method.
3. **`server/src/main/java/com/gumtree/csagent/controller/DemoInspectionController.java`** —
   verified. `/v1/demo/sessions/{id}/llm-calls` at lines 94–97
   (Sprint 25). `/v1/demo/sessions/{id}/trace` at lines 124–127
   returns `List<BotTurn>` from `LocalEventStore.getSessionTrace`.
   No new endpoint needed.
4. **Runtime state location for the four named fields**: verified.
   - `tool_calls` → `bot_turns.tool_calls` JSONB column
     (`server/src/main/resources/db/migration/V2__create_bot_turns.sql`
     line 11; `BotTurn.java:43–44`).
   - `phase_plan` → serialized into `projection.phase_plan` JSONB
     (`ContextProjectionBuilder.java` lines 731–803, all PhasePlan
     fields except `maxToolSteps`); persisted via
     `bot_turns.projected_context` (`ControlKernel.java:1999`).
   - `projection` (full per-turn projection including `intake_state`)
     → `bot_turns.projected_context` JSONB
     (`ContextProjectionBuilder.java:346–377` for the intake_state
     slot specifically; `BotTurn.java:37–38` for the column).
   - `LlmCallEvents` (LLM-call timing + outcome) → already surfaced
     as `case_results[].llm_calls[]` field on `results.json` by
     Sprint 25 (`executor.py:376`). No re-shipment of this axis.
5. **Database migrations** — verified. Latest is V13
   (`V13__add_consecutive_deadline_count.sql`, Sprint 24). Sprint 28
   adds NO new migration: the data already exists in V2's
   `bot_turns` and V9's `llm_call_log`.
6. **`docs/action_bank.md` line 448 entry for
   `R-per-case-trace-dump-for-smoke-harness`** — verified. Scope
   text matches: "Re-enable per-case JSON trace dumping (tool_calls,
   phase_plan, projection, LlmCallEvents) under each
   `eval_interactive/results/<run-id>/` directory so future Track A
   §3.2 walks and Track B per-step investigations have first-party
   evidence. Scope: `eval_interactive/eval_interactive/` Python
   package (smoke runner)."

If the dev session finds any of these six items false at session
start, STOP and report (§9 stop conditions).

## 3. Two surface options + bias

### 3.1 Option A — Java-side new instrumentation

Add new persistence on the bot for `phase_plan` / `intake_state`
snapshots; new DB tables; new fields on `bot_turns`. Higher blast
radius: touches `BotSession`, `BotTurn`, `ContextProjectionBuilder`,
`ControlKernel`, requires a Flyway V14 migration. Strongly biased
**AGAINST** by the premise check — the data already exists.

Option A is the path **only if** Sprint 28's dev session finds, at
runtime, that one of the four named fields is in fact NOT persisted
or NOT reachable through the existing demo endpoints. If that
happens, STOP and report — the planning-turn premise was wrong, and
the corrected sprint scope needs human direction.

### 3.2 Option B — Writer-side eval-harness enrichment (BIAS)

Mirror Sprint 25 Track B precedent exactly:

- Extend `_build_case_result` in `eval_interactive/eval_interactive/batch/executor.py`
  with an additional list-shaped field carrying per-turn data
  serialized from the already-collected `trace_data.turns[]`
  (each entry is a `TurnTrace` dataclass at
  `eval_interactive/eval_interactive/trace/models.py:8–21` with the
  four R-item-named fields already populated by `TraceCollector`
  per `eval_interactive/eval_interactive/trace/collector.py:515–549`).
- No new bot-side endpoint; no new HTTP method on `AgentClient`;
  no new Java code; no DB migration. The full work is on the
  eval-harness writer side.
- Backwards compat preserved: existing fields stay byte-identical
  (`llm_calls` from Sprint 25, `transcript`, `l1_results`, etc.).
  The new field is **additive**.

The dev session walks Option A / Option B at session start and
selects per §3 hard-fence rules. Option B is the strong default.

## 4. Sprint 27 (R2) consumer use case

The Sprint 27 (R2) follow-on probe is explicitly gated on this
R-item landing first. Sprint 27 handoff §1.6 (risks) line 131–136
states:

> Several target directives (T1 / T2 / T3 below) require either
> phase trace or projected `intake_state` slot values to confirm
> precondition-fires. Neither is in the present `results.json`
> shape (see `R-per-case-trace-dump-for-smoke-harness` at
> `docs/action_bank.md:448`).

Sprint 28's deliverable must produce per-turn fields rich enough
that a Sprint 29+ probe can:

- Read each turn's `phase_before` / `phase_after` to confirm phase
  transitions consistent with D485.2 (CLOSE record_outcome),
  D564.7 (INTAKE intake-complete handover with structured args),
  D616.2 (RESOLVE FAQ premature-escalation prohibition) preconditions.
- Read each turn's `projected_context.intake_state.fields_remaining`
  / `intake_state.intake_complete` to confirm the D564.7
  precondition fires before the LLM was given its turn.
- Read each turn's `tool_calls` array (with sanitized `arguments`
  per existing `ToolCallTraceSanitizer`) to count `request_handover`
  and `record_outcome` emissions per phase.
- Cross-reference with the existing `case_results[].llm_calls[]`
  from Sprint 25 for the LLM-call timing axis.

## 5. Files in scope (Option B path — strong default)

### 5.1 Files modified by the dev agent

| path | change |
|------|--------|
| `eval_interactive/eval_interactive/batch/executor.py` | Extend `_build_case_result` signature with a serialized-trace argument (or read `trace_data.turns[]` directly in the body); add one new additive list field on the returned dict, sibling to the existing `llm_calls` field at line 376. Mirror Sprint 25's pattern — non-breaking schema extension, swallow exceptions if any sub-field cannot be serialized, log a warning. |
| `eval_interactive/tests/test_<new-trace-dump-test>.py` | New pytest regression file (≥ 6 tests, mirroring Sprint 25's `test_executor_llm_calls_enrichment.py` 257 lines / 8 tests). Tests must demonstrate (a) populated case path (all four fields present per turn), (b) empty / missing case path (defensive defaults), (c) backwards-compat (`llm_calls` and `transcript` still present byte-identical to pre-Sprint-28), (d) JSON-serializability (no datetime / non-JSON types leak). |

### 5.2 Files NOT modified but read for verification

- `eval_interactive/eval_interactive/trace/collector.py` lines
  515–549 — read-only: confirms `TurnTrace` is already hydrated
  with the four fields.
- `eval_interactive/eval_interactive/trace/models.py` lines 8–21
  — read-only: `TurnTrace` schema.
- `server/src/main/java/com/gumtree/csagent/controller/DemoInspectionController.java`
  lines 94–97, 124–127 — read-only: existing demo endpoints.

### 5.3 Files NOT in scope (the 13 hard fences)

These are **prohibited** from edit by Sprint 28:

1. No semantic surface touched. No prompt edits, no Java semantic
   decisions moved, no LLM-from-Java boundary shift.
2. No Sprint 24/25/26/27-landed code edits. Specifically:
   `BotSession.consecutiveDeadlineCount` (Sprint 24); the Sprint 25
   `executor.py llm_calls` enrichment + `agent_client.py get_llm_calls`
   + `LlmSyntheticBaselineTest.java`; Sprint 26's no-code outcome;
   Sprint 27's docs-only outcome.
3. No Sprint 23-landed code edits (`system_prompt.txt` teaching
   paragraph, `AlreadyCalledPromptConsumptionTest.java`).
4. No eval-spec edits (`case_specs`, `case_spec_overrides.yaml`,
   personas, judge rubric).
5. No Sprint 20 case-family edits (cascade fence — never edit
   Sprint 20's CaseSpec families).
6. No foundational doc edits.
7. No governance doc edits (`docs/current/*.md`).
8. No sprint archive edits (`docs/sprints/sprint-001..027-*`).
9. No Tier-0 changes (`docs/runtime_freeze_and_risk_policy.md`).
10. No deadline / model / retry config edits (Sprint 26 accept
    stands).
11. Every quantitative claim in the handoff cites source + extraction
    command (per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`).
12. No mocked-LLM as primary evidence (carry forward Sprint 23 lesson).
13. No scope expansion to opportunistic fields. Only the four named
    in the R-item (`tool_calls`, `phase_plan`, `projection`,
    `LlmCallEvents`). If the dev agent surfaces additional candidate
    fields (`session_state`, `drift_detector_output`, `eval_trace_log`),
    they go into a follow-on R-item, NOT this sprint.

## 6. Bundle policy

Sprint 28 bundles the writer-side enrichment + a regression test
as a single PR-equivalent commit. The bundle policy is:

- **Bundle (default):** writer-side change + regression test, both
  in `eval_interactive/`. No Java touched. No new endpoint. Codex
  per-PR Anti-Hardcode kernel passes cleanly (no semantic decision
  moved). This is the expected outcome.
- **Investigation-only (fallback):** if at session start the dev
  finds the data is NOT actually in `trace_data.turns[]` (premise
  #4 fails) OR the existing demo endpoint returns a sanitized
  shape that strips one of the four fields (premise #3 fails),
  the dev STOPS and writes an investigation-only handoff naming
  the gap and proposing Option A as a follow-on R-item. No code
  ships. Human direction required for the next sprint.

The bundle policy is **explicit in this objective** so Codex does
not flag a bundled writer-side enrichment as scope drift in
review.

## 7. Success metrics (per `iteration_governance.md` §5)

| metric | target |
|--------|--------|
| **Target** | Every smoke case in a Sprint 28-instrumented run has the four new fields populated on `case_results[].<new-field>[]`. Reproducible via a named `jq` extraction command in the handoff. |
| **Reproducibility** | The handoff includes a `jq` (or `python3 -c`) extraction command for each cited number (count of turns, count of `tool_calls`, count of populated `phase_plan` snapshots). Per the reproducibility bar — no number without method. |
| **Neighbor (backwards compat)** | The Sprint 25 `llm_calls[]` extractor (any prior `jq '.case_results[].llm_calls[]'` command) still works post-Sprint-28 on the new `results.json`. The `transcript` field is byte-identical. The `case_id` / `composite_score` / `failure_tags` fields are byte-identical. |
| **Negative (no overhead regression)** | Case-level `elapsed_ms` on a 14-case smoke rerun does not regress by more than the natural variance band (cite the Sprint 26 measurement methodology — distinguish run-to-run variance from a serialization-overhead regression). If `elapsed_ms` widens systematically, surface as an open question and STOP. |
| **Server suite** | `mvn -q -pl server test` returns the same `tests / failures / errors / skipped` line as the pre-Sprint-28 reference (Sprint 27 close commit `8a7703a`). Zero new regressions from Sprint 28 code (which should be zero anyway — Sprint 28 doesn't touch Java). |
| **Eval suite** | `uv run pytest` under `eval_interactive/` returns the same `passed / failed / errors / warnings` line as Sprint 25's reference, modulo the new Sprint 28 tests added (which should all pass). Zero new regressions. |
| **Safety floor** | Unchanged. No PII path, no safety path touched. |
| **Grounding floor** | Unchanged. No FAQ-grounding path touched. |
| **Wrong-containment / over-escalation** | Unchanged. Sprint 28 ships no semantic decision. |
| **Architecture-health metrics** | `new_semantic_hardcode_count` for this sprint = 0 (no semantic surface touched). |

Sprint 28 defers neighbor / negative / shadow on the case-family
axis to G2 (already in `iteration_governance.md` §5 pattern; Sprint
20 case families are not in scope).

## 8. Layer-classification + anti-hardcode stanza (§7 required)

**Target failure layer:** `infra` (eval-harness writer-side
enrichment). The sprint touches `eval_interactive/eval_interactive/`
Python package only. The schema-extension surface (`results.json`)
is the eval contract artefact — per Constitution §1.4, the Runtime
owns the "trace and eval contract"; Sprint 28's edits make the
contract more observable without changing what the runtime owns or
how the LLM is prompted.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. No
clause in `docs/runtime_freeze_and_risk_policy.md` is referenced
or extended.

**Semantic hardcode:** No semantic hardcode introduced. Sprint 28
writes JSON serialization code in Python; no keyword / regex /
if-else / enum / per-UC matrix governs any semantic decision in
the work. No Java guard is added. No prompt text is added or
changed.

**Generalization coverage:**
- **target:** the 14-case smoke run rerun under Sprint 28's
  instrumentation. Every case must show the four new fields
  populated per turn. (Counts: T=14.)
- **neighbor:** the full server suite + the full
  `eval_interactive/` pytest suite. (Counts: N = pre-Sprint-28
  passing totals; verify zero new regressions.)
- **negative:** the case-level `elapsed_ms` must not widen
  systematically. (Counts: G = 14 cases × 2 runs = 28 case-runs
  baseline from Sprint 25 reference + Sprint 26 close runs;
  Sprint 28's rerun is the test cell.)
- **shadow:** held to G2 case-family construction; not built for
  this sprint. (Counts: S = 0 — deferred to G2.)

## 9. Do not implement

Sharp restatement of what Sprint 28 **must not** ship, to keep
scope creep visible:

1. No new bot-side instrumentation (Option A path) unless premise
   verification at dev session start fails — and in that case STOP,
   do not ship; write an investigation-only handoff.
2. No new opportunistic fields. The R-item names four; Sprint 28
   ships four. Do not add `session_state`, `drift_detector_output`,
   `eval_trace_log`, or any other field even if reading the code
   suggests it would be "easy" or "convenient".
3. No breaking change to the existing `results.json` schema.
   Existing fields stay byte-identical; the new fields are
   additive only.
4. No edit of any Sprint 23 / 24 / 25 / 26 / 27-landed code.
5. No edit of any Sprint 20 case-family / shadow content.
6. No edit of any foundational, governance, sprint-archive doc.
7. No new Tier-0 invariant, no new java_guard, no rubric widening,
   no override creation.
8. No mocked-LLM as primary evidence. The regression test mocks
   the agent client at the boundary (per Sprint 25's pattern) but
   the test demonstrates serialization correctness, not LLM
   behavior. If a test of LLM behavior is needed, that's
   out-of-scope for Sprint 28.
9. No act on the trace data. Sprint 28 PRODUCES the data; future
   sprints CONSUME it (Sprint 29+ R2 probe, future Track A walks).
10. No re-extraction of Sprint 27's findings. The R-item disposition
    stays as Sprint 27 set it.
11. No commit of the deliver-agent-owned files in the working tree
    by the dev. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`,
    expect `docs/sprint_objective.md`, `compact/sprint-028-*.md`,
    `compact/sprint-deliver-orchestrator.md`, and the pre-existing
    unrelated mods (`csagent_system_design_review.md`,
    `server/src/main/resources/prompts/system_prompt.txt`,
    `csagent-solution-_20260514.md`) to be present in the working
    tree at session start. The dev should stage only their own
    authored files.

## 10. Pointers

- R-item entry: `docs/action_bank.md:448`.
- Sprint 25 precedent (Option B writer-side enrichment pattern):
  `docs/sprints/sprint-025-handoff.md` §3 (Option B chosen),
  §4 (synthetic baseline design — adapt for Sprint 28's regression
  test design), §5 (worked-example pre/post comparison structure
  — adapt for the per-turn-trace presence demonstration).
- Sprint 27 consumer use case:
  `docs/sprints/sprint-027-handoff.md` §1.6, §11 Q2, §6 (gating
  named three times).
- Trace-data shape: `eval_interactive/eval_interactive/trace/models.py:8–21`
  (`TurnTrace` dataclass).
- Trace-collector hydration:
  `eval_interactive/eval_interactive/trace/collector.py:515–549`.
- Demo endpoints in bot:
  `server/src/main/java/com/gumtree/csagent/controller/DemoInspectionController.java`
  (lines 94–97 + 124–127).
- Constitution: `docs/current/iteration_governance.md` §1 / §3 / §5
  / §7.
- Doc governance: `docs/current/doc_governance.md`.
- Agent-context reading lists:
  `docs/current/agent_context_guide.md`.
