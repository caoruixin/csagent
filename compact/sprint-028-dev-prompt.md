Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Working tree at session-start carries deliver-agent-owned files + 3 pre-existing unrelated mods — see §10.

---

# Sprint 28 dev prompt — Per-case trace dump for smoke harness

You are the dev agent. Single track, single layer (`infra` / eval-harness). Authoritative scope: `docs/sprint_objective.md`. Ships `R-per-case-trace-dump-for-smoke-harness` (`docs/action_bank.md:448`).

## 1. Loader stanza

Read in order before any code:

1. `AGENTS.md`.
2. `docs/current/doc_governance.md`.
3. `docs/current/agent_context_guide.md`.
4. `docs/current/iteration_governance.md` §1 / §3 / §5 / §7.
5. `docs/sprint_objective.md` — Sprint 28 scope. §2's six premise items are fact; re-verify each at session start by reading the cited paths.
6. `docs/sprints/sprint-025-handoff.md` §3 (Option B chosen), §4 (test pattern), §5 (worked-example shape), §8 (files-changed table). This is the Sprint 28 precedent — same writer-side enrichment shape.
7. `docs/sprints/sprint-027-handoff.md` §1.6 (risks before analysis), §6 ((R2) recommendation), §11 Q2 (consumer use case).
8. Run a fresh Context Pack (per `agent_context_guide.md`) before any code.

## 2. Premise re-verification (mandatory)

Each of the six items in `docs/sprint_objective.md` §2 must hold at session start. Read the cited paths + line numbers. Expected outcomes:

1. `eval_interactive/eval_interactive/batch/executor.py` — `_build_case_result` at 314–377; `_fetch_llm_calls` at 293–312; `llm_calls` placeholders at 376 / 404 / 446 / 505.
2. `eval_interactive/eval_interactive/simulator/agent_client.py` — `get_trace(session_id)` at 124–138; `get_llm_calls(session_id)` at 169–186.
3. `server/src/main/java/com/gumtree/csagent/controller/DemoInspectionController.java` — `/llm-calls` at 94–97; `/trace` at 124–127 returning `List<BotTurn>`.
4. `bot_turns.tool_calls` JSONB (V2 line 11; `BotTurn.java:43–44`); `bot_turns.projected_context` JSONB (V2 line 6; `BotTurn.java:37–38`) containing full `phase_plan` (`ContextProjectionBuilder.java:731–803`) + `intake_state` (`ContextProjectionBuilder.java:346–377`).
5. Latest Flyway is V13 (Sprint 24).
6. `docs/action_bank.md:448` carries `R-per-case-trace-dump-for-smoke-harness` proposed/open.

If any item is false at the cited path/line, STOP and surface it in the handoff §1.6 + §11; do not code under a false premise.

## 3. The Option A/B walk

Walk the same A/B shape Sprint 25 used:

- **Option A** — Java-side new persistence: new `bot_turns` columns or table for `phase_plan` snapshots. Flyway V14, edits to `BotSession` / `BotTurn` / `ControlKernel` / `ContextProjectionBuilder`, new test suite. Higher blast radius. NOT the default.
- **Option B (strong default)** — eval-harness writer-side enrichment. The data is already in `trace_data.turns[]` (`TurnTrace` dataclass at `eval_interactive/eval_interactive/trace/models.py:8–21`), hydrated by `TraceCollector` at `collector.py:515–549` from `agent_client.get_trace(session_id)`. The eval-harness reads it but does NOT serialize it into `results.json`. Sprint 28 extends `_build_case_result` to emit per-turn data as an additive list field.

Sprint 25's precedent: the bot already produced the contract. Sprint 28's premise check confirms the same for all four R-item-named fields. **Option A is the path only if re-verification fails on items 2 / 3 / 4** — and then STOP, write investigation-only handoff, do not ship code.

## 4. Reproducibility rule

Every number in your handoff cites source path + extraction recipe (jq filter or `python3 -c`) + literal output. No number without method. Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md`.

## 5. Scope discipline — only the four R-item-named fields

The R-item names FOUR fields:

1. `tool_calls` — already in `TurnTrace.tool_calls`; serialize as-is.
2. `phase_plan` — already serialized inside `TurnTrace.projected_context["phase_plan"]`; emit as sub-field of the per-turn record or sibling top-level per-turn field.
3. `projection` — full per-turn projection; already in `TurnTrace.projected_context`; serialize as-is.
4. `LlmCallEvents` — **already covered** by Sprint 25's `case_results[].llm_calls[]`. Do NOT re-ship. Handoff §3 must state this explicitly.

Do NOT add `session_state`, `drift_detector_output`, `eval_trace_log`, `customer_context`, `form_context`, `events[]`, or any other field even if the code suggests it would be "easy". They go to a follow-on R-item in handoff §7 / §11, NOT to this sprint's schema. Sprint 26's no-new-instrumentation discipline applies.

## 6. Files in scope (Option B path)

| path | change |
|------|--------|
| `eval_interactive/eval_interactive/batch/executor.py` | Extend `_build_case_result` (lines 314–377) to emit one new list-shaped field carrying per-turn data from `trace_data.turns[]`. Mirror Sprint 25 `llm_calls` enrichment exactly. Add the new field next to `llm_calls` at line 376 AND to `_timeout_result` / `_error_result` defensive paths so all four `case_result` emission sites stay schema-consistent. |
| `eval_interactive/tests/test_<new>.py` | New pytest regression file (≥ 6 tests, ≤ ~280 lines mirroring Sprint 25's `test_executor_llm_calls_enrichment.py` shape). Cover: (a) populated case — every turn has the four fields; (b) empty trace — defensive `[]` default; (c) missing `projected_context.phase_plan` — defensive default; (d) backwards compat — `llm_calls` + `transcript` byte-identical; (e) JSON-serializability — no datetime / non-JSON types; (f) `_timeout_result` / `_error_result` paths emit `[]`. |

## 7. Hard fences (the 13 forbidden surfaces)

Per `docs/sprint_objective.md` §5.3 — quick summary:

1. No semantic surface touched.
2. No Sprint 24/25/26/27-landed code edits.
3. No Sprint 23-landed code edits (`system_prompt.txt`, `AlreadyCalledPromptConsumptionTest.java`).
4. No eval-spec edits.
5. No Sprint 20 case-family edits (cascade fence).
6. No foundational doc edits.
7. No governance doc edits.
8. No sprint archive edits (`docs/sprints/sprint-001..027-*`).
9. No Tier-0 changes.
10. No deadline / model / retry config edits.
11. Every quantitative claim reproducible.
12. No mocked-LLM as primary evidence.
13. No opportunistic 5th / 6th field.

§1.7 hard gate: under Option B no Java code is touched, no prompt text changes, no eval CaseSpec rubric widening, no keyword/regex/if-else/enum/per-UC matrix introduced. If any commit in your range would violate one of these, STOP — there is no small-exception path.

## 8. 12-section handoff doc contract

Write `docs/10-handoff.md` as the running lead AND the archive copy at `docs/sprints/sprint-028-handoff.md`. The archive copy includes:

1. **Context Pack** (1.1 docs / 1.2 code / 1.3 doc-status / 1.4 source-of-truth / 1.5 implementation-status / 1.6 risks).
2. **Sprint-objective recap** — one paragraph.
3. **Option chosen** — A or B with reason. If B (expected), cite Sprint 25 precedent + premise table.
4. **Implementation walkthrough** — the executor.py change cited by line range + the new test file by test names.
5. **Worked example** — 14-case smoke rerun under Sprint 28 instrumentation. Include a `jq` extraction command for EACH of the four fields (one example: `jq '.case_results[0].<new-field>[0].tool_calls' <path>`) with literal output inlined.
6. **Backwards-compat verification** — `jq` showing Sprint 25's `llm_calls[]` extractor returns identical shape; Python `set()` comparison of top-level keys pre- vs post-Sprint-28.
7. **Open questions for human** — anything you found but didn't act on. Each becomes a possible follow-on R-item.
8. **Files changed** — table with paths + line range / test counts.
9. **Layer-classification self-walk** (per §3 first-match-wins). Lands on `infra`. State reasoning.
10. **§5 Eval Acceptance verification** — each of the nine bars line by line, with cited evidence.
11. **§7 stanza self-walk** — single-layer `infra`; no Tier-0; no semantic hardcode; target / neighbor / negative / shadow per objective §8.
12. **Closure verdict placeholder** — leave for human + deliver agent. State: "human + deliver agent will fill at close" per `feedback_handoff_verdict_section_delegation.md`.

## 9. Stop conditions

STOP and report (not silently work around) when:

1. Any of §2's six premise items fails at session start.
2. Reading the code surfaces a need for Java instrumentation (Option A). Investigation-only handoff; human direction.
3. `agent_client.get_trace(session_id)` response shape has fewer fields than `TurnTrace` expects (demo endpoint sanitizes one of the four).
4. Adding the new field would require non-additive changes (renames, type shifts, removed fields). Strict additive bar.
5. 14-case smoke rerun shows `elapsed_ms` systematically widening beyond the Sprint 26 variance band.
6. Tempted to add a 5th / 6th field.
7. Tempted to touch Sprint 23 / 24 / 25 / 26 / 27-landed code.
8. Tempted to widen scope (probe-case authoring, manual case reruns, R2 follow-on surface walks). All downstream.

## 10. Working tree at start (commit-at-end pattern)

Expect uncommitted files at session start:

- `docs/sprint_objective.md`, `compact/sprint-028-dev-prompt.md`, `compact/sprint-028-review-prompt.md`, `compact/sprint-deliver-orchestrator.md` — deliver-agent files.
- `csagent_system_design_review.md`, `server/src/main/resources/prompts/system_prompt.txt`, `csagent-solution-_20260514.md` — pre-existing unrelated mods, NOT Sprint 28 work.

**Do not stage these.** Stage only your authored §6 files for your final commit. Do not run `git add -A` / `git add .`. The human bundles deliver-agent files at commit time per the commit-at-end pattern. Per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

## 11. Final-commit run commands

1. `mvn -q -pl server test` — zero new regressions from `8a7703a` baseline. Sprint 28 doesn't touch Java; any delta is inherited / environmental — surface in handoff §11.
2. `cd eval_interactive && uv run pytest` — zero new regressions modulo your new tests (all pass).
3. Smoke rerun on the 14-case set. The handoff §5 worked example references the actual `results.json` your run produced; cite its run-id path.
4. The handoff §5 `jq` commands all run cleanly against your produced `results.json`.

If you hit a stop condition, write the handoff up to the stop point and mark later sections "not reached due to <stop condition>". Do not skip silently.
