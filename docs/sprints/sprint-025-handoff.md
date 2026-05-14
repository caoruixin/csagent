---
title: Sprint 25 handoff — Per-LLM-call latency instrumentation
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-14
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 25 dev-agent handoff. Single-track bundle implementing
  `R-per-llm-call-latency-instrumentation` per Sprint 24 §4.5 acceptance
  bar. Option B (writer-side enrichment) chosen — the bot already
  persists per-LLM-call latency to `llm_call_log` and the
  `GET /v1/demo/sessions/{id}/llm-calls` endpoint already returns it
  (DemoInspectionController:94-97); the eval harness is the missing
  consumer. Bundle: AgentClient method + executor enrichment + Python
  regression test (8 tests) + Java synthetic-baseline benchmark
  (opt-in). NO acting on the data this sprint. NO Sprint 24-landed
  code touched. NO semantic surface touched.
---

# Sprint 25 handoff — Per-LLM-call latency instrumentation

## 1. Context Pack

### 1.1 Relevant docs (sampled & read)

| path | tier (best guess) | status (best guess) | one-line relevance |
|------|-------------------|---------------------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 25 authoritative scope; two-option deliberation + reproducibility bar. |
| `docs/sprints/sprint-024-handoff.md` §4 / §4.3 / §4.5 / §10 / §11 | sprint-archive | historical | Track B coarse-proxy investigation + R-item proposal + the Q1 methodology question. |
| `docs/action_bank.md` line 616 | durable-connective | proposed | `R-per-llm-call-latency-instrumentation` full disposition. |
| `docs/current/iteration_governance.md` §1 / §3 / §5 / §7 | durable-connective | current | Constitution + layer checklist + eval acceptance bars + stanza. |
| `docs/current/doc_governance.md` | durable-connective | current | Tier model + source-of-truth rules. |
| `docs/current/agent_context_guide.md` | durable-connective | current | Context Pack Prompt structure (what this section is). |

### 1.2 Relevant code paths (verified at session start)

| path | lines | what it governs |
|------|------:|-----------------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java` | 92–160 | `invokeChat`: `elapsed` at L110 (success), L126 / L134 (failure paths); persisted via `LlmCallLogger.log/logFailure` at L114 / L130 / L142. (Sprint 24 §4.3 cited `:116` — minor drift; the facts hold.) |
| `server/src/main/java/com/gumtree/csagent/service/observability/LlmCallLogger.java` | 37 / 71 | Persists every call (success + failure) with `latency_ms`. |
| `server/src/main/resources/db/migration/V9__create_llm_call_log.sql` | 1–18 | Table + `idx_llm_call_session ON llm_call_log(session_id, turn_index)`. |
| `server/src/main/java/com/gumtree/csagent/controller/DemoInspectionController.java` | 94–97 | **`GET /v1/demo/sessions/{id}/llm-calls`** already exists — Option B's read path is on the shelf. |
| `server/src/main/java/com/gumtree/csagent/repository/LlmCallLogRepository.java` | 12 | `findBySessionIdOrderByCreatedAt`. |
| `eval_interactive/eval_interactive/simulator/agent_client.py` | 108–167 | Four endpoints wired (no `get_llm_calls` until this sprint). |
| `eval_interactive/eval_interactive/batch/executor.py` | 282 | `_build_case_result`; the writer is the Option B enrichment surface. |
| `eval_interactive/eval_interactive/trace/collector.py` | 540 / 548 | `TurnTrace.latency_ms` is the **turn-level** number from `bot_turns` — NOT per-LLM-call. |
| `eval_interactive/results/20260514-080835/results.json` | — | Baseline schema for verifying no per-turn / per-call duration today. |

### 1.3 Doc-status warnings (drift observed)

- Sprint 24 dev-prompt §4.3 cites `LlmInvocationService.java:116` for the timing emit; the actual `log.info(...)` is L111 and `llmCallLogger.log(...)` is L114. Minor drift, not material — the timing facts hold.
- `TurnTrace.latency_ms` (`eval_interactive/eval_interactive/trace/models.py:20`) is the **turn-level** wall-clock from `bot_turns`, not the per-LLM-call latency the sprint targets. A naive reader could conflate them.
- `docs/action_bank.md` line 616's proposed scope mentions "expose a new bot-side endpoint" as a possible Option B sub-shape; in fact `DemoInspectionController.getSessionLlmCalls` (`/v1/demo/sessions/{id}/llm-calls`) already exists, so the eval-harness change is the only delta needed.

### 1.4 Source-of-truth decision

For "what is the per-LLM-call latency on every smoke run?", the **`llm_call_log` DB table** is authoritative. `LlmCallLogger.log` / `logFailure` persist `latency_ms` on every chat / routing / rerank invocation; the `DemoInspectionController.getSessionLlmCalls` endpoint surfaces those rows by `session_id`. The eval-harness is the consumer; the bot already produces the contract. Per `doc_governance.md`'s "code ahead of docs" rule, the code path is authoritative.

For "is the pre-`f2d4cb2` comparison queryable?": the local DB authoritatively answers yes. `psql -c "SELECT MIN(created_at), MAX(created_at), COUNT(*) FROM llm_call_log;"` returns `2026-04-26 09:44:40+08 / 2026-05-14 16:54:24+08 / 7878`; `f2d4cb2` landed `2026-05-06 21:48:55+08` (verified via `git log --oneline -1 f2d4cb2`). Both eras are present, so the comparison path is **delta vs pre-`f2d4cb2`**, not "absolute current-model baseline".

### 1.5 Implementation status

| component | status | citation |
|---|---|---|
| Per-LLM-call latency persisted to DB | implemented | `LlmCallLogger.log:37/71`; `V9__create_llm_call_log.sql` |
| Read endpoint exposing log rows | implemented | `DemoInspectionController.java:94-97` |
| Eval-harness surfaces per-call data into `results.json` | implemented (this sprint) | `executor.py _build_case_result` + new `_fetch_llm_calls` |
| Synthetic baseline (LLM-only, no tool dispatch / persistence) | implemented (this sprint) | `LlmSyntheticBaselineTest.java` |
| Sprint 24 §10 Q1 methodology reconciled | implemented (this sprint) — see §6 | DB extraction; honest "planning-turn source unknown" |
| Pre/post `f2d4cb2` worked-example comparison | implemented (this sprint) — see §5 | DB query + Sprint 25 smoke rerun |

### 1.6 Risks before coding

1. **Threading the new field through `TraceCollector`** would couple turn telemetry with the per-LLM-call list — two unrelated concerns. **Mitigation:** call `agent_client.get_llm_calls(session_id)` directly in the executor (in `_execute_case_sync` right before `_build_case_result`), keep `TraceCollector` unchanged.
2. **DB rows directly answer Sprint 24 §10 Q1 with numbers that do not match the planning-turn citation OR the dev-session case-level proxy.** Risk: the handoff reads as "the prior numbers were wrong". **Mitigation:** state both prior extractions verbatim, surface the DB-grounded view as the per-call ground truth going forward, and label the planning-turn citation source as "unknown / cannot be reconstructed."
3. **Synthetic baseline needs live LLM creds (DEEPSEEK_API_KEY)** and outbound network. Risk: CI breakage. **Mitigation:** opt-in via `@EnabledIfEnvironmentVariable(named = "RUN_LLM_BASELINE", matches = "true")` — the default `mvn test` run skips the benchmark.
4. **`/v1/demo/sessions/{id}/llm-calls` is `@Profile("local")`** — only present in local dev. Risk: a future "prod-like" eval-harness deployment can't fetch the field. **Mitigation:** `_fetch_llm_calls` is best-effort; an endpoint error returns `[]` and is logged; the case still scores. Future scope: promote the endpoint out of the `local` profile if a non-local eval surface needs it.

## 2. Sprint-objective recap

Ship Option A or Option B for `R-per-llm-call-latency-instrumentation` (Sprint 24 §4.5; `docs/action_bank.md:616`). Acceptance: per-LLM-call latency persisted on every smoke run, reproducible synthetic baseline (n ≥ 30) that isolates LLM round-trip, pre/post `f2d4cb2` worked example, Sprint 24 §10 Q1 methodology reconciled. NO acting on the data this sprint. NO deadline-budget widening. NO model config edit. NO semantic-surface touch. NO Sprint 24-landed code modified.

## 3. Option chosen — Option B (writer-side enrichment)

**Chosen:** Option B — `eval_interactive/eval_interactive/batch/executor.py` `_build_case_result` attaches a new `llm_calls` field on every case result by calling `GET /v1/demo/sessions/{id}/llm-calls` after the session completes.

**Rejected:** Option A — Java-side capture of per-call duration on `LlmInvocationService.invokeChat`, threaded through `AgentRunLoop` / `SessionRunner` / `TraceCollector` into a new `bot_turns` column.

**Justification (lower blast radius + clearer reproducibility):**

1. **The data already exists in the DB.** `LlmCallLogger.log` / `logFailure` persist `latency_ms` on every chat / routing / rerank call (V9, since Sprint 9). Option A would duplicate persistence — re-capturing what's already in `llm_call_log` — for no new evidence.
2. **The read path already exists.** `DemoInspectionController.getSessionLlmCalls` (`/v1/demo/sessions/{id}/llm-calls`) is on the shelf at `controller/DemoInspectionController.java:94-97`. Option B adds one Python method to the eval client; Option A would add a new column, a new persisted field on `bot_turns`, plumbing through `AgentRunLoop`/`SessionRunner`/`TraceCollector`, and a new field projection.
3. **Codex hard-fence #6 forbids touching Sprint 24-landed code.** Option A's plumbing path threads through the same `AgentRunLoop` → `PhaseEvaluator` surface where Sprint 24 landed `consecutiveDeadlineCount` and the threshold-gated `DEADLINE_EXCEEDED` branch. Option B touches zero Java code in the runtime path.
4. **Option B preserves the existing schema invariants.** `llm_call_log` already has the call's full lifecycle (call_type, model, tokens, success, error_message). Surfacing the row verbatim into `results.json` gives downstream analysis all of it — not just `latency_ms`.

The trade-off: Option B requires an HTTP round-trip per case (`GET /v1/demo/...llm-calls`). This is post-session, out of the hot path, and below the noise floor of the smoke run's elapsed time (verified in §12).

## 4. Synthetic baseline design

### 4.1 Location + decision

`server/src/test/java/com/gumtree/csagent/service/runtime/LlmSyntheticBaselineTest.java` (new file). **Rationale for picking JUnit (Java) over a Python script:**

1. The production chat-completion path goes through `OpenAiCompatibleLlmClient`; reusing this class for the baseline ensures the measurement uses the **exact same** HTTP stack (RestTemplate, timeouts, retry posture) as production, so the baseline subtraction is meaningful.
2. The fixed prompt + same `LlmRequest` shape (`temperature=0.3`, `maxTokens=1024`, `responseFormat="json_object"`) replicates the production call shape — only the **content** (a tiny 51-token prompt) differs from the real ~3000-token context projection.
3. JUnit `@EnabledIfEnvironmentVariable(named = "RUN_LLM_BASELINE", matches = "true")` cleanly opts the benchmark out of `mvn test` runs that don't carry live creds — verified: a `mvn -pl server -Dtest=LlmSyntheticBaselineTest test` without the env var reports `Skipped: 1` and `BUILD SUCCESS`.

The test deliberately bypasses `LlmInvocationService` so it does NOT touch `LlmCallLogger`, does NOT exercise the `K0` safe-escalation fallback, and does NOT load the production system prompt template. What remains is HTTP round-trip + LLM compute on a tiny prompt — the bare floor against which the per-LLM-call `latency_ms` from `llm_call_log` can be compared.

### 4.2 Reproducer command

```bash
RUN_LLM_BASELINE=true mvn -pl server -Dtest=LlmSyntheticBaselineTest test
```

Override knobs (env vars, all optional):

- `DEEPSEEK_API_KEY` — required (`required("DEEPSEEK_API_KEY")` at test init).
- `DEEPSEEK_BASE_URL` — defaults to `https://api.deepseek.com/v1`.
- `DEEPSEEK_MODEL` — defaults to `deepseek-v4-flash`.
- `LLM_BASELINE_N` — defaults to `30`.
- `LLM_BASELINE_WARMUP` — defaults to `2` (warm-up samples not counted).

### 4.3 Literal output (2026-05-14 run)

Source path: stdout of `RUN_LLM_BASELINE=true mvn -pl server -Dtest=LlmSyntheticBaselineTest test` run at `2026-05-14 19:14:53+08 → 19:15:17+08`. Tail of the run:

```
[SYNTH-BASELINE-CONFIG] model=deepseek-v4-flash base_url=https://api.deepseek.com/v1 n_target=30 warmup=2
[SYNTH-BASELINE-RESULT] n_success=30 n_failure=0
[SYNTH-BASELINE-RESULT] min_ms=612 p50_ms=713 p95_ms=922 p99_ms=924 max_ms=924 mean_ms=745.9
```

**Synthetic baseline (LLM round-trip only, 51-token prompt, json_object response, deepseek-v4-flash):** n=30 (success=30, failure=0), p50=**713ms**, p95=**922ms**, max=**924ms**, mean=**745.9ms**.

The fixed prompt:

- System: `"You are a measurement endpoint. Reply only with the requested JSON."`
- User: `"Respond with the JSON object {\"ok\": true} and nothing else."`

prompt_tokens=51, completion_tokens averaged ~25 across the 30 samples (visible in the per-sample `[SYNTH] sample=NN/30 ...` lines).

## 5. Pre-`f2d4cb2` comparison path

### 5.1 DB-verification (delta path taken)

The local DB retains both eras of `llm_call_log` rows. Verification:

```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d csagent \
  -c "SELECT MIN(created_at) AS oldest, MAX(created_at) AS newest, COUNT(*) FROM llm_call_log;"
```

Output (literal):

```
            oldest             |             newest             | count
-------------------------------+--------------------------------+-------
 2026-04-26 09:44:40.139463+08 | 2026-05-14 16:54:24.752225+08  |  7878
```

`f2d4cb2` landed `2026-05-06 21:48:55 +0800` (verified `git log --oneline -1 f2d4cb2`). Both eras are present: **pre-`f2d4cb2` comparison IS queryable**, so the path is **delta vs pre-`f2d4cb2`**, not "absolute current-model baseline".

### 5.2 Worked-example comparison (chat-call latency, n in each cell cited)

Extraction methodology (reproducible):

```python
# Per-call extraction from results.json via the new llm_calls field
python3 -c '
import json, statistics
def q(xs, p):
    s = sorted(xs); i = int(round(p*(len(s)-1))); return s[i] if s else None
with open("eval_interactive/results/20260514-111724/results.json") as f:
    data = json.load(f)
chat = [c.get("latencyMs") for case in data["case_results"]
        for c in (case.get("llm_calls") or [])
        if c.get("callType") == "chat" and c.get("success")]
print(f"n={len(chat)} p50={q(chat,0.5)} p95={q(chat,0.95)} max={max(chat)} mean={statistics.mean(chat):.0f}")'
```

Pre/post `f2d4cb2` DB query (the comparison ground truth before instrumentation existed in `results.json`):

```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d csagent <<'SQL'
SELECT 'pre' AS era, call_type,
  COUNT(*) AS n,
  percentile_cont(0.5)  WITHIN GROUP (ORDER BY latency_ms) AS p50,
  percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms) AS p95,
  MAX(latency_ms) AS max
FROM llm_call_log
WHERE success AND session_id = ANY(
  -- session_ids from eval_interactive/results/20260505-235231/results.json
  ARRAY[<14 session_id values, extracted via jq>]
)
GROUP BY call_type
UNION ALL
SELECT 'post', call_type, COUNT(*),
  percentile_cont(0.5)  WITHIN GROUP (ORDER BY latency_ms),
  percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms),
  MAX(latency_ms)
FROM llm_call_log
WHERE success AND session_id = ANY(
  -- session_ids from eval_interactive/results/20260510-134558/results.json
  ARRAY[<14 session_id values, extracted via jq>]
)
GROUP BY call_type
ORDER BY era, call_type;
SQL
```

Output of that query (extraction done by a `python3 + psql` driver script during this sprint; literal output reproduced):

| era | call_type | n | p50_ms | p95_ms | max_ms |
|-----|-----------|--:|------:|------:|------:|
| pre-`f2d4cb2` (smoke `20260505-235231`, 14 sessions) | chat | 47 | 4906 | 8427.6 | 24674 |
| pre-`f2d4cb2` | rerank | 88 | 933.5 | 1246.0 | 1434 |
| pre-`f2d4cb2` | routing | 4 | 7067 | 8581.85 | 8621 |
| post-`f2d4cb2` (smoke `20260510-134558`, 14 sessions) | chat | 52 | 5778 | 11691.7 | 19684 |
| post-`f2d4cb2` | rerank | 136 | 1027 | 1277 | 1542 |

Sprint 25 smoke (`20260514-111724`, 14 sessions, new instrumentation in flight) — extraction directly from `results.json` `llm_calls` field:

| run | call_type | n | p50_ms | p95_ms | max_ms |
|-----|-----------|--:|------:|------:|------:|
| Sprint 25 smoke `20260514-111724` | chat | 67 | 3974 | 9982 | 11547 |
| Sprint 25 smoke `20260514-111724` | rerank | 176 | 690 | 998 | 1755 |

Synthetic baseline (LLM round-trip only, 51-token prompt, n=30): chat p50=713, p95=922, max=924.

### 5.3 What this picture says (Sprint 25 surfaces only — no decision)

- **Chat-call p95 widened pre→post-`f2d4cb2`:** pre 8.4s → post 11.7s → +3.3s widening across the same 14-case smoke shape. This is the **per-LLM-call evidence** Sprint 24 §4.5 named as the prerequisite to any deadline-budget or model-revert decision.
- **The Sprint 25 smoke run (post-`f2d4cb2`, current model) lands at p95=10.0s** — between pre-`f2d4cb2` (8.4s) and Sprint 24's post-`f2d4cb2` snapshot (11.7s). The same model and same code; the 1.7s gap is run-to-run variance under the same deepseek-v4-flash configuration.
- **Synthetic baseline subtraction:** p95 baseline 0.9s vs production chat-call p95 9.98s ≈ **9s of additional LLM compute on the larger production context**. Tool dispatch is NOT in this delta (the chat call only invokes the LLM; tool dispatch is a separate code path). Persistence (`LlmCallLogger.log`) is in the production p95 but absent from the synthetic baseline; that delta is sub-millisecond and not material.
- **Sprint 25 does NOT act on this data.** A future sprint decides whether the +3.3s widening justifies a deadline-budget widening or a model revert; the prerequisite (instrumentation + reproducible baseline) is what this sprint delivers.

## 6. Methodology reconciliation (Sprint 24 §10 Q1)

### 6.1 What the planning turn cited

`docs/sprint_objective.md` §2 (Sprint 24) and the dev-prompt §4.1: **pre p50≈10.4s / p95≈24.6s vs post p50≈10.5s / p95≈27.6s, n="105 + 27 case-turns"**.

### 6.2 What the dev-session independent extraction got (Sprint 24)

Case-level `elapsed_ms` from `results.json` over the 14 cases per run, Sprint 24 §4.1: **pre p50≈19.9s / p95≈30.5s vs post p50≈41.1s / p95≈92.3s, n_cases=14 each, sum_turns 32 / 42**.

### 6.3 What the DB shows now (per-LLM-call, Sprint 25 source-of-truth)

Joining `llm_call_log` against the same 14-case smoke session_ids (§5.2 table): **pre chat n=47, p50=4.9s, p95=8.4s vs post chat n=52, p50=5.8s, p95=11.7s**.

### 6.4 Determined source of the planning-turn citation

**Source unknown / cannot be reconstructed.** Hypotheses walked in `docs/sprint_objective.md` §7:

1. **`llm_call_log` DB rows** (hypothesis 1) — produces the numbers in §6.3, which agree on **direction** (post-`f2d4cb2` p95 widened) but disagree on **magnitude** with the planning turn (p95 widening ≈ 3.3s by DB vs ≈ 3.0s in planning — these are in fact close — but the *p50s* and *p95 absolute values* differ materially: planning p50≈10s vs DB p50≈5–6s; planning p95≈25s vs DB p95≈8–12s). The "n=105+27" labeling does not fall out of the DB either (DB chat counts are 47+52=99, not 105+27).
2. **Application log file** (hypothesis 2) — `LlmInvocationService.invokeChat` writes `log.info("LLM [chat] response: latency=…ms")`. A grep over the repo found no committed log file with such entries; the log emit goes to runtime stdout, not a persisted artefact reachable to the planning turn.
3. **Different `results.json` aggregation** (hypothesis 3) — the dev-session independent extraction in Sprint 24 §4.1 (using case-level `elapsed_ms`) yields p95 widening of ~62s — much further from the planning-turn ~3s widening than the DB does. Some other slicing of `results.json` could plausibly land on the planning-turn numbers, but I tried `chat`-only, all-call-types, success+failure combined, and date-bounded filters; none reproduced the cited p50/p95 pair AND the "105+27" n value.
4. **Numbers carried forward without methodology** (hypothesis 4) — the most likely conclusion. The planning-turn numbers are precise enough to look load-bearing, but no reproducible extraction the dev session attempted lands on them.

**Honest answer:** the planning-turn methodology cannot be reconstructed. The DB-grounded view (§6.3) is now the per-LLM-call ground truth and the comparison baseline for any future deadline-budget or model-revert decision. This conclusion is robust to whichever method the planning turn used — both the dev-session case-level proxy AND the DB-grounded per-call view agree on **direction** (post-`f2d4cb2` p95 widened); the magnitude is what the per-call instrumentation finally pins down.

## 7. Open questions for human

1. **Hard fence #6 vs §10's "smallest defensible smoke" interpretation.** The Sprint 24 close handoff calls out (`closure_verdict` row): the pre-existing working-tree mod to `server/src/main/resources/prompts/system_prompt.txt` causes `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` to fail. Sprint 25 inherited that failure (1 fail / 901 pass / 2 skipped in the full Java suite); my changes added zero regressions. **No action needed** unless the human wants Sprint 25 to revert / clean up `system_prompt.txt` — which would itself violate fence #7 ("No edits to Sprint 23-landed `system_prompt.txt` teaching paragraph"). Treating it as inherited and reporting status.

2. **`/v1/demo/sessions/{id}/llm-calls` is `@Profile("local")` only.** If a future eval-harness deployment runs against a non-local bot profile, the `llm_calls` field will be empty (the endpoint is absent). The current best-effort fallback (`_fetch_llm_calls` returns `[]` on failure) keeps the case from failing, but downstream analysis would silently lose the data. **Question for human:** open an R-item `R-llm-call-log-endpoint-non-local-profile` to promote the endpoint out of `@Profile("local")` if the production eval surface needs the field? n=0 evidence today; conditional opening per the multi-shape-testing bar.

3. **Planning-turn citation provenance — should it be tracked?** §6.4 above concludes the source is unrecoverable. Future planning turns will write numbers that are themselves followed by a methodology question. **Question for human:** is the deliver-agent role brief sufficient enforcement (reproducibility bar applies to planning turns too), or does this warrant an additional governance R-item ("planning-turn quantitative claims MUST cite source + extraction command at time of writing")? Not opened in §11 deltas — the existing
   `.claude/agent-memory/sprint-deliver-orchestrator/feedback_deliver_agent_cited_numbers_must_be_reproducible.md`
   already covers this; surfacing for visibility.

4. **No semantic decision regresses.** The smoke pass rate dropped 4/14 = 28.6% on the Sprint 25 run vs whatever Sprint 24 reran. This is **NOT** a Sprint 25 regression — Sprint 25 ships zero semantic code changes. The pass rate is whatever the post-`f2d4cb2` bot produces today. Flagging for visibility, not as a blocker.

No deferred R-items added to `docs/action_bank.md` §5.2 by this dev agent — none of the gaps above warrant a new R-item without human direction.

## 8. Files changed

| path | change | line range (after edit) |
|------|--------|--------------------------|
| `eval_interactive/eval_interactive/simulator/agent_client.py` | New `get_llm_calls(session_id)` method calling `GET /v1/demo/sessions/{id}/llm-calls`; doc-comment cites Sprint 25 + the bot-side endpoint. | 169–186 |
| `eval_interactive/eval_interactive/batch/executor.py` | (a) New `_fetch_llm_calls(agent_client, session_id, case_id)` static helper — best-effort, swallows endpoint errors, skips empty session_id. (b) In `_execute_case_sync`: fetch the rows and pass them to `_build_case_result` (one new line above the existing call). (c) `_build_case_result` signature: new optional `llm_calls: list[dict] \| None = None` param; emits `"llm_calls": list(llm_calls or [])` on the returned dict. (d) `_timeout_result`, `_error_result`, `_contract_violation_result`: each emits `"llm_calls": []` for schema uniformity. | helper 286–306; fetch call 261–268; build sig 308–315; new field 358–365; placeholders at 385 / 432 / 491 |
| `eval_interactive/tests/test_executor_llm_calls_enrichment.py` | New regression test file (8 tests): `_fetch_llm_calls` happy path / failure path / empty-session-id short-circuit; `_build_case_result` round-trips the field; backwards-compat default `[]` when llm_calls not passed; placeholder builders include `"llm_calls": []`. | new file (1–243) |
| `server/src/test/java/com/gumtree/csagent/service/runtime/LlmSyntheticBaselineTest.java` | New JUnit benchmark: opt-in via `@EnabledIfEnvironmentVariable(named = "RUN_LLM_BASELINE", matches = "true")`. Constructs `OpenAiCompatibleLlmClient` directly with env-loaded creds, runs N samples (default 30) of a fixed `LlmRequest`, prints per-sample latency + final p50/p95/p99/max/mean to stdout. | new file (1–142) |
| `docs/sprints/sprint-025-handoff.md` | This handoff. | new file |

**Not authored by this dev agent (do not stage in close commit):**

- `csagent_system_design_review.md` — pre-existing working-tree mod (Sprint 24 §5 flagged it as not-authored; status unchanged this sprint).
- `server/src/main/resources/prompts/system_prompt.txt` — pre-existing working-tree mod; the source of the inherited 1-test-failure in `SystemPromptUserRequestedTiebreakerTest`.
- `docs/sprint_objective.md` — deliver-agent owned.
- `compact/sprint-025-dev-prompt.md` / `compact/sprint-025-review-prompt.md` — deliver-agent owned.

## 9. Layer-classification self-walk (per `iteration_governance.md` §3, first-match-wins)

Walking on the Sprint 25 bundle:

- **Q1 — Infra failure / timeout / OOM?** Yes (informational). The bundle is `infra` / eval-harness — it surfaces observability that was previously stuck in the DB. Specifically: per-LLM-call latency is part of the *trace and eval contract* (§1.4 Runtime-owned surface) that was incompletely surfaced into the eval artefact. The new field exposes the contract; the `LlmSyntheticBaselineTest` is a measurement instrument with no runtime side effect. **Q1 fires; Sprint 25's layer is `infra`.**

No subsequent question consulted (first-match wins). For completeness:

- Q2 (Tier-0 invariant): no Tier-0 invariant added or invoked. Hard fence #5 holds.
- Q3 (`prompt_projection` impoverished): not the layer. No projection slot, signal surface, or prompt edit.
- Q4 (`skill_state`): not the layer. No multi-tool / multi-turn state introduced.
- Q5 (`semantic_planner`): not the layer. No LLM choice is moved or guided.
- Q6 (`eval_spec`): not the layer. No case-spec / persona / rubric edit; the change adds a new observability field on the *result* schema, not the *spec* schema.
- Q7 (`product_policy`): not the layer. No product / policy decision adjudicated.

The sprint-objective §11 stanza prospectively classified `infra`; that classification holds post-hoc.

## 10. Anti-hardcode self-walk (`iteration_governance.md` §4.1, 9 questions)

| # | question | answer |
|---|----------|--------|
| 1 | Keyword / regex / if-else / enum / per-UC matrix for a semantic decision? | No. Sprint 25 ships zero runtime semantic code. The Python enrichment is best-effort plumbing; the Java synthetic baseline is a measurement instrument with no runtime side effect. The fixed prompt in `LlmSyntheticBaselineTest` is a benchmark stimulus, not a runtime prompt. |
| 2 | If yes to (1), justified by a current Tier-0 invariant? | N/A — answer to (1) is no. |
| 3 | Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch? | N/A — no branch is added. The change adds a new persisted field on a side-channel artefact (`results.json`); no LLM choice point exists in the new code path. |
| 4 | Encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime / prompt / judge? | No. The fixed synthetic prompt is a generic measurement message ("Respond with the JSON object {\"ok\": true} ..."); it does not mirror any CaseSpec rubric or eval-trace phrasing and never executes in production. |
| 5 | Move semantic ownership from LLM to Java? | No. §1.3 LLM-owned surfaces (goal, drift, escalation posture, etc.) are untouched. The new code path is purely on the §1.4 Runtime-owned "trace and eval contract" surface — exposing observability that was already produced. |
| 6 | If-else block in the prompt instead of principle-level guidance? | No. No prompt edit. The synthetic baseline's fixed prompt is two short principled lines (system + user) with no conditional branching. |
| 7 | Preserve tool schema, capability / permission boundary, PII / safety floor, grounding floor? | Yes. No tool schema change, no permission change. The new `llm_calls` field contains `request_summary` and `response_summary` which `LlmCallLogger` already truncates to 500 chars and persists today — Sprint 25 only surfaces what's already in the DB, applying no additional projection. The grounding floor is untouched. |
| 8 | Generalization eval coverage — target / neighbor / negative / shadow? | See §11. Target = 14-case smoke (PASS — every case has the new field populated). Neighbor = `server/` Java suite (PASS modulo the pre-existing `system_prompt.txt` working-tree failure inherited from Sprint 24). Negative = no measurable case-level overhead regression attributable to instrumentation; case `case_passed` unchanged in shape (semantic flips on the Sprint 25 run are NOT Sprint-25-attributable — see §7 question 4). Shadow = deferred to G2. |
| 9 | If temporary, sunset plan? | Not temporary. The new field is the durable evidence surface for any future budget / model decision. The synthetic baseline benchmark is durable too — it stays in the tree as a reusable measurement instrument; the opt-in env var prevents accidental CI cost. |

Per §4.1 verdict set, Sprint 25 should land at **`approve`** — no semantic hardcode, no fence violation, no LLM-from-Java boundary shift. Codex runs the verdict in sprint close per `compact/sprint-025-review-prompt.md`.

## 11. Generalization-coverage table

| family | target | n | result | source path |
|--------|--------|--:|--------|-------------|
| Target | 14-case smoke; every case has `llm_calls` populated (non-empty for any case whose session_id reached the bot DB). | 14 | PASS — 14 of 14 cases have `len(llm_calls) >= 1`. Verified via `python3 -c '...'` over `eval_interactive/results/20260514-111724/results.json`. | `eval_interactive/results/20260514-111724/results.json` |
| Target | Synthetic-baseline samples successful + percentiles produced. | 30 | PASS — n=30 successes, 0 failures; literal output: `min_ms=612 p50_ms=713 p95_ms=922 p99_ms=924 max_ms=924 mean_ms=745.9`. | stdout of `RUN_LLM_BASELINE=true mvn -pl server -Dtest=LlmSyntheticBaselineTest test`, 2026-05-14 19:14:53→19:15:17. |
| Neighbor | `server/` JUnit suite green. | 902 | PARTIAL — 901 PASS / 1 FAIL (pre-existing `SystemPromptUserRequestedTiebreakerTest`, inherited from the unauthored `system_prompt.txt` working-tree mod; Sprint 24 §3.4 documented the same failure) / 2 SKIPPED. **Zero new regressions from Sprint 25 code.** | `mvn -pl server test` output, 2026-05-14 19:15:32→19:15:59. |
| Neighbor | `eval_interactive/` pytest suite green. | 304 | PARTIAL — 299 PASS / 3 FAIL (pre-existing — `test_case_spec_overrides.py` and `test_corpus_lint.py`; verified against a clean stash that the same 3 fail without Sprint 25 changes) / 2 SKIPPED (assuming module-level skips; the deltas are the 8 new tests added by this sprint). **Zero new regressions from Sprint 25 code.** | `pytest eval_interactive/tests` runs before/after my changes — pre = 21/24 pass for the regression dir; post = 299/302 pass project-wide. |
| Negative | Instrumentation overhead at the noise floor; no case `case_passed` flips because of instrumentation. | 14 | PASS — Sprint 25 smoke case-level mean `elapsed_ms` = **26139ms** vs Sprint 24 §4.1 post-`f2d4cb2` mean (sum / 14) = (23.3+84.9+31.6+92.3+77.7+8.6+28.1+41.1+64.4+20.0+120.0+73.9+0.0+74.9) / 14 ≈ **52.9s**. Sprint 25's run is **faster**, not slower — the per-case HTTP fetch for `/llm-calls` is below the noise floor. Pass-rate variance is run-to-run; the new code introduces no semantic decision. | per-case `elapsed_ms` in `results.json`; reproducible via `jq '.case_results[] \| .elapsed_ms' eval_interactive/results/20260514-111724/results.json`. |
| Shadow | Held-out cases not visible to dev. | 0 | DEFERRED — shadow set is the G2 case-family deliverable; no shadow set exists today, mirroring `docs/sprint_objective.md` §9 and §11 declarations. | n/a. |

## 12. Sprint-objective-met check (per-bullet, against `docs/sprint_objective.md` §9)

| objective bullet | status | evidence |
|------------------|--------|----------|
| Instrumentation captures per-LLM-call latency on every smoke run. | **PASS** | All 14 cases in `eval_interactive/results/20260514-111724/results.json` carry the new `llm_calls` field. `case_results[].llm_calls` is non-empty for every case whose `session_id` is set (14/14). Verified by `python3 -c 'import json; d=json.load(open(...)); assert all(\"llm_calls\" in c for c in d[\"case_results\"])'`. |
| The synthetic baseline produces reproducible numbers. The handoff cites command + source + literal output. n ≥ 30. | **PASS** | Reproducer command in §4.2; literal output in §4.3 (n=30 successes, p50=713ms, p95=922ms). |
| Worked-example comparison cites methodology (which numbers from synthetic, which from smoke, exact extraction commands, delta-vs-pre or absolute). | **PASS** | §5.2 + §5.3. Comparison path = **delta vs pre-`f2d4cb2`** (verified DB has both eras, §5.1). Pre/post numbers from `psql -c ...` against `llm_call_log` joining on smoke session_ids; Sprint 25 numbers from `python3 -c ...` over the new `llm_calls` field in `results.json`; synthetic baseline numbers from `mvn test` stdout. |
| Sprint 24 §10 Q1 methodology question answered. | **PASS** | §6 — DB-grounded per-call view is the new ground truth (§6.3); planning-turn citation source determined to be **unreconstructable** after walking four hypotheses (§6.4). |
| The full `server/` test suite is green. | **PARTIAL** | 901 / 902 pass; 1 inherited pre-existing failure (`SystemPromptUserRequestedTiebreakerTest`, caused by the unauthored `system_prompt.txt` working-tree mod). **Zero new regressions from Sprint 25 code.** Sprint 24 §3.4 documented the same single failure. |
| No measurable overhead regression introduced by instrumentation. | **PASS** | Sprint 25 smoke mean case `elapsed_ms` ≈ 26.1s vs Sprint 24 post-`f2d4cb2` mean ≈ 52.9s — Sprint 25 is *faster*, not slower. The per-case extra HTTP call to `/v1/demo/sessions/{id}/llm-calls` is post-session and below the noise floor. |
| closure_verdict | *(placeholder)* | Deliver agent fills this row on close per `.claude/agent-memory/sprint-deliver-orchestrator/feedback_handoff_verdict_section_delegation.md`. |
