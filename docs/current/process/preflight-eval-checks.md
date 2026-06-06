---
title: Pre-flight eval checks (§5.9 promotion landing)
doc_tier: durable-connective
status: current
implementation_status: partial
source_of_truth: this file (canonical promoted checklist; per-incident additions per §5.9 incremental rule); `docs/current/iteration_governance.md` §5.9 (governing rule); `autoloop/scripts/rebless_baseline.py` (full re-bless orchestrator); `eval_interactive/eval_interactive/cli.py` (per-suite smoke entry)
last_reviewed: 2026-06-07
review_cadence: per milestone close + per new §3 `infra` framework brief
supersedes: []
superseded_by: null
notes: >
  Promoted from `docs/diagnostics/` eval-framework-audit per
  `iteration_governance.md` §5.9 ("The checklist lives in the most
  recent `docs/diagnostics/` eval-framework audit until it outgrows one
  audit's scope, then is promoted to
  `docs/current/process/preflight-eval-checks.md`").

  First-promotion landing 2026-06-07 at M-Auto-6 close prep (before the
  milestone-shared real-LLM re-bless). The initial check inventory in §3
  is derived from the M-Auto-6 sub-sprint surfaces (R1.a / R2.a /
  R2.a#5-ext / R4.a / R5 / R6 / R7) + the standing anti-误杀 floor + the
  long-standing env hazards (backend hot-reload absence per
  `[[feedback_restart_backend_before_eyeball]]`; macOS proxy / Flyway
  migration application).

  The checklist is INCREMENTAL: each new §3 `infra` framework brief
  contributes "the cheapest read-only check that would have caught it"
  per §5.9. See §6 maintenance rule.

  Output of this runbook is a go/no-go for the full real-LLM re-bless
  (`cd autoloop && uv run python scripts/rebless_baseline.py --n 9
  --out-dir ../eval_interactive/results/...`). Smoke pre-flight is
  **wiring evidence** per §5.7 — it does NOT substitute for outcome
  evidence at milestone close.
---

# Pre-flight eval checks

## 1. When to run this

**Mandatory before any of:**

- A milestone-shared real-LLM re-bless (e.g.
  `eval_interactive/results/m-auto-N-baseline-shared-YYYYMMDD/`,
  multi-suite, `--n 9`).
- A baseline re-bless that will move `baseline_dir` or
  `docs/current_eval_baseline.md`.
- Any batch real-LLM run > ~5 minutes wall-time on the v1 / S1 / shadow
  suites.

**Recommended for:**

- Any sub-sprint dev close that wants a wiring-evidence smoke before
  handoff (§5.7 mocked-vs-real evidence separator).
- After any infra change touching: Flyway migrations, the backend
  classpath, the autoloop 5-file SHA-locked scoring set, the
  eval_interactive harness, or LLM client / proxy config.

**Skip for:**

- Pure docs-only / config-governance / characterization-test PRs.
- Mocked-LLM unit / integration runs (different evidence class).

## 2. Runbook (step-by-step)

Output of the runbook is a single **go / no-go** verdict at §4 with
cited evidence (file:line / command output / trace path).

### Step 0 — Env pre-flight (mandatory; ~2 min)

Cheapest checks; failure here is a STOP before any LLM credit is burned.

#### 0.1 Backend restart

Per `[[feedback_restart_backend_before_eyeball]]`: `mvn spring-boot:run`
has NO hot-reload; if dev started the backend before R6's V17 / R7's
tool dispatch / R5's guardrail rewrite (or any new code) was on the
classpath, the running JVM does not see it. The payload visible to the
LLM at smoke time = the JVM's classpath at backend-start time, not the
current working tree.

```bash
# Find and kill any running backend. NOTE: detect by PORT, not by
# `ps aux | grep '[s]pring-boot'` — the running server is a FORKED
# `java -cp …` JVM that the spring-boot:run wrapper launches, so the
# grep misses it (caught at the 2026-06-07 pre-flight, drift #5).
lsof -ti:8080 | xargs -r kill -9
# Wait for port release
until ! lsof -ti:8080 >/dev/null 2>&1; do sleep 1; done
# Re-launch in background; tail log
mvn -o -pl server spring-boot:run > /tmp/csagent-backend-preflight.log 2>&1 &
# Poll until ready (look for "Started CsAgentApplication" or whatever the boot signature is)
until grep -q "Started.*Application" /tmp/csagent-backend-preflight.log; do sleep 2; done
```

**Evidence to cite in §4**: backend start timestamp + first 20 lines of
`/tmp/csagent-backend-preflight.log` showing the boot signature.

#### 0.2 Flyway migration state

```bash
# Confirm the latest expected migration applied (update version number per current milestone)
psql -d "${CSAGENT_DB:-csagent}" -c "SELECT version, description, success, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

**STOP condition**: latest `version` column does not match the highest
`V<NN>__*.sql` file under
`server/src/main/resources/db/migration/`, OR `success = false` on any
recent row. Either means the running backend is on a stale schema.

**Evidence to cite in §4**: the psql output rows (top 5 migrations,
versions + success flags + timestamps).

#### 0.3 Data-flag spot-check (per-milestone)

If the current milestone flips a data flag on specific rows (e.g.
M-Auto-6 R6's `search_knowledge_eligible: true → false` on 2 `(temp)`
template articles), confirm the flag landed in DB after re-ingestion.

```bash
# M-Auto-6 R6 example:
psql -d "${CSAGENT_DB:-csagent}" -c "SELECT article_id, search_knowledge_eligible, published_status FROM kb_articles WHERE article_id IN ('ka41r000000LIEJAA4','ka41r000000LIEEAA4');"
# Expect: both rows have search_knowledge_eligible=false, published_status=true
```

**STOP condition**: the expected flag state on the targeted rows does
not match. Either ingestion did not re-run, or the JSON file is at the
wrong commit, or the new column wasn't persisted by ingestion.

**Root-cause annotation (added 2026-06-07 per the S-Auto-28 blocker
brief, §6 maintenance rule):** if §0.3 shows the targeted rows still at
the default value AND the rows pre-existed the migration, the cause is
**not** a stale JSON commit or cache — it is the insert-only
`KnowledgeIngestionRunner` skipping every already-present `article_id`.
Re-running `--ingest` will NEVER flip an existing-row curation flag.
Remediation = the `--reconcile` data-application mode (S-Auto-28 / R8),
which UPDATEs the mutable curation columns of existing rows in place;
re-run §0.3 after `mvn -o -pl server spring-boot:run
-Dspring-boot.run.arguments=--reconcile`. A manual SQL `UPDATE` is
explicitly forbidden as the remediation (human direction 2026-06-07).

**Evidence to cite in §4**: the psql output rows.

#### 0.4 Env config sanity (one-off but cheap)

Per `[[reference_macos_proxy_httpx_localhost]]`: macOS system proxy
breaks `httpx` localhost calls (`trust_env=True` proxies localhost and
ignores the bypass). If the eval harness uses `httpx` to talk to the
backend on `localhost:8080`, a sudden total LLM-timeout / connection-refused
on smoke is likely the proxy, not the code.

```bash
# Confirm no proxy env is set when the harness will run, OR that the harness uses trust_env=False
env | grep -iE 'http_proxy|https_proxy|all_proxy' || echo "no proxy env (clean)"
```

Per `[[reference_codex_crs_gateway]]`: if the gateway is in use,
`disable_response_storage=true` must be in `~/.codex/config.toml`.

### Step 1 — Wiring smoke on `bad_cases` (mandatory; ~5-10 min)

The `bad_cases` suite is the M-Auto-N falsifiable-hypothesis surface
(per `milestone_objective.md` §5). Run it first at `--n 1 --parallel 1`
to catch gross wiring breaks.

```bash
# From repo root, with a venv that has eval_interactive installed:
python -m eval_interactive run \
    --set bad_cases \
    --parallel 1 \
    --label "m-auto-N-smoke-preflight-bad-cases-$(date +%Y%m%d-%H%M%S)" \
    --verbose
```

`--n` is not a flag on `eval_interactive run` (that's
`autoloop/scripts/rebless_baseline.py`'s aggregator flag). Smoke runs
at one draw per case by default.

**Wall-time budget**: ~5-10 min on bad_cases (12 cases × n=1).

**Evidence to cite in §4**: the `eval_interactive/results/<label>/`
directory path; `results.json` summary block; any per-case `case_passed`
column for bad_cases (pre vs post smoke).

### Step 2 — Anti-误杀 floor sentinel on `anchor_outcome` (mandatory; ~5-10 min)

The persistent-high-risk anchors live in `anchor_outcome`. The
anti-误杀 invariants are HARD per `milestone_objective.md` §5: any rise
in pass-rate on `uc_g_gdpr` / `uc_h_appeal` / `uc_i_payment` /
`uc_j_safety` (away from 0.000 stable) is a `reject` trigger, not an
`approve with observations`.

```bash
python -m eval_interactive run \
    --set anchor_outcome \
    --parallel 1 \
    --label "m-auto-N-smoke-preflight-anchors-$(date +%Y%m%d-%H%M%S)" \
    --verbose
```

**Wall-time budget**: ~5-10 min (~10-15 anchor cases × n=1).

**Evidence to cite in §4**: the `eval_interactive/results/<label>/`
directory path; per-anchor `pass_rate` for `uc_g_gdpr` / `uc_h_appeal` /
`uc_i_payment` / `uc_j_safety`. STOP if any anti-误杀 anchor flips PASS.

### Step 3 — Trace anomaly scan

Read traces (or grep the `results.json` + `per_session_trace.json` /
similar) for the anomaly signatures in §3 below. Each anomaly entry
names the trace signature, the R-item (or layer) it attributes to, and
the go/no-go disposition.

## 3. Anomaly checklist (current inventory; incremental per §5.9)

Each row: trace signature → attribution → go/no-go action. Add a new
row whenever a new `infra`-layer framework brief lands per §6 below.

| # | Trace signature | R-item / surface | Read-only check (cheapest detection) | Go/no-go on hit |
|---|---|---|---|---|
| A1 | `search_knowledge` tool_event returns `hits=[]` on EVERY call across smoke | R6 corpus filter | grep results for `search_knowledge` tool result body shape; expect `hits` non-empty on the vast majority of search-knowledge calls | NO-GO. R6 over-filtering. Verify F4 back-compat default-true; verify only 2 rows have `search_knowledge_eligible=false` in DB. |
| A2 | Backend INFO log has zero `search_knowledge_eligible=false` filter decision entries despite smoke including UC-B / FAQ-path queries | R6 INFO log path α wiring | `grep 'search_knowledge_eligible=false' /tmp/csagent-backend-preflight.log` | NO-GO. Either the filter is never invoked (Service-layer call path issue) or the INFO log is at the wrong layer. |
| A3 | `(temp)` substring appears in any `search_knowledge` tool result body visible to LLM | R6 filter not effective on the live corpus | grep tool result bodies | NO-GO. Re-check §0.3. Most likely cause on a populated DB: the insert-only runner skipped the existing rows so the flag never landed — remediation is the `--reconcile` mode (S-Auto-28 / R8), NOT a re-run of `--ingest` and NOT a manual SQL `UPDATE`. (Less likely: stale JSON commit or cache.) |
| A4 | `update_intake_fields` tool NEVER appears in any `ToolEvent` across intake-UC turns in smoke | R7 schema declaration / dispatch wiring | grep `ToolEvent.tool_name == 'update_intake_fields'` in trace | INVESTIGATE (not auto-NO-GO). R7 adoption is an OBS-S6 autoloop concern, NOT a wiring failure on its own. BUT: zero invocations AND zero schema entries in `tool_schemas` projection = wiring break (NO-GO). Verify projection includes the schema. |
| A5 | `must_cite_source` guardrail rejection rate rises markedly vs pre-M-Auto-6 baseline on the bad_cases set | R5 structural-shape predicate is too strict | compare rejection rate row in `results.json` summary; or grep `must_cite_source` rejection events in traces | NO-GO. Structural-shape predicate may be rejecting URL-less articles. Verify article_id-shape regex matches the URL-less corpus subset (38 articles per the M-Auto-6 contract). |
| A6 | Any anti-误杀 anchor (`uc_g_gdpr` / `uc_h_appeal` / `uc_i_payment` / `uc_j_safety`) flips from 0.000 stable to PASS | Safety floor violation (route (c) per `milestone_objective.md` §5) | per-anchor `pass_rate` in anchor_outcome smoke summary | NO-GO + IMMEDIATE REVERT. This is the most expensive thing to discover at full re-bless time. Diagnose the change that caused the artifact mis-pass before relaunching. |
| A7 | `clarification_budget_exhausted` escalation_reason occurrences = 0 across ALL DISCOVER free-text repeats AND ALL RESOLVE-intake free-text repeats in smoke | R2.a #5 + R2.a#5-ext re-map wiring | grep `escalation_reason: clarification_budget_exhausted` in trace; expect > 0 on the relevant cases | NO-GO. Mapping fall-through means `mapBudgetToEscalationReason` 4-arg overload isn't firing. Re-check phase + intake-UC + free-text predicate at `ControlKernel:313-315`. |
| A8 | `request_handover` validator rejects first-call intake handover on intake-UC (UC-G / H / I / J / K) | R1.a schema wiring | grep validator rejection events in trace, filter to intake UCs | NO-GO. R1.a `intake_fields` slot did not surface in projection schema. Verify `ContextProjectionBuilder` emits the per-active-UC required-fields list iterated from `IntakeFieldsRegistry`. |
| A9 | `customer_context_status` enum or `ad_reference` block missing from per-turn projection | R4.a entity-premise projection wiring | grep per-turn projection JSON for both keys | NO-GO. R4.a wiring break. Note: the LLM may not act on the signal yet (OBS-S1 deferred to autoloop); the wiring evidence is that the slot is PROJECTED every turn. |
| A10 | Smoke run total wall-time exceeds ~3x expected (e.g. bad_cases takes > 30 min instead of ~5-10) | LLM provider latency / connection issue / runaway loop | wall-time on the smoke summary | INVESTIGATE. Could be provider P99 latency, proxy issue (§0.4), or a stall-detector miss; do NOT proceed to full `--n 9` until resolved. |
| A11 | Java baseline regression (any test that was previously passing now fails after backend restart) | Classpath / migration / dependency drift | `mvn -o test` exit code + summary; expect `1348/1/0/2` post-M-Auto-6 with OQ-S41.5 as the sole failure | NO-GO. Java regression must be diagnosed before LLM credit is burned. |

## 4. Go/no-go decision template

Write the verdict block in the smoke-run handoff appendix or at the
bottom of `docs/sprints/sprint-NNN-handoff.md` §3 (sub-sprint-close
case) or at the milestone-close handoff appendix (milestone-shared
re-bless case).

```markdown
## Pre-flight verdict — <date> <time>

**Target run**: <full re-bless command + intended out-dir>
**Verdict**: GO | NO-GO

### Step 0 — Env pre-flight
- 0.1 Backend restart: <evidence>
- 0.2 Flyway migration state: <psql output rows>
- 0.3 Data-flag spot-check: <psql output rows>
- 0.4 Env config sanity: <env output>

### Step 1 — Bad_cases smoke
- Results: `eval_interactive/results/<label>/`
- Summary line: <bad_cases summary>

### Step 2 — Anti-误杀 floor sentinel
- Results: `eval_interactive/results/<label>/`
- Per-anchor pass_rate: uc_g_gdpr=<x>; uc_h_appeal=<x>; uc_i_payment=<x>; uc_j_safety=<x>

### Step 3 — Anomaly scan
- A1: <hit/miss + evidence>
- A2: <hit/miss + evidence>
- ... (one line per anomaly row)

### Verdict justification
GO: all gates green; no anomalies; proceed to full `--n 9` re-bless.
OR
NO-GO on A<N>: <one-sentence diagnosis> + remediation: <pointer>
```

## 5. Anti-误杀 invariants (must not be violated by any change in this runbook)

The runbook itself is read-only. No production code, no skill yaml, no
CaseSpec, no scoring SHA touched. Smoke results are observation only;
they do NOT substitute for outcome evidence at milestone close (per
§5.7 mocked-LLM evidence gate analog: smoke is wiring evidence;
`--n 9` paired-evidence is outcome evidence).

**Hard floor**: any rise in pass-rate on `uc_g_gdpr` / `uc_h_appeal` /
`uc_i_payment` / `uc_j_safety` (away from 0.000 stable) discovered at
this smoke is a `reject` + revert trigger, NOT an `approve with
observations`. The smoke is the **last cheap chance** to catch a safety
floor violation before LLM credit is burned at `--n 9`.

## 6. Maintenance rule — how this checklist grows (§5.9 incremental)

Per `iteration_governance.md` §5.9: "The checklist is incremental: each
new §3 `infra` framework brief contributes the cheapest read-only check
that would have caught it."

**When to add a row to §3**:

- A `docs/diagnostics/failure-briefs/<id>.md` brief with §3 layer =
  `infra` AND scope = eval framework (simulator, trace emitter,
  scoring, baseline aggregation, judge harness) lands.
- A sub-sprint or milestone post-mortem identifies an `infra` failure
  that wasted real-LLM credit before being caught.
- A backend-side env hazard (Flyway / classpath / proxy / DB
  connectivity / config drift) burns LLM credit on smoke or re-bless.

**How to add**: append one row to §3's table with:

1. The cheapest read-only check (grep / SQL / log / env / file-existence
   check) that would have caught the brief's symptom.
2. The R-item / surface attribution.
3. The go/no-go disposition (default: NO-GO; only INVESTIGATE when the
   hit is ambiguous, e.g. R7 adoption per A4).

**When to demote a row**: if a row's check is folded into a permanent
runtime guard (e.g. Java startup assertion that V17 applied), the
runbook row becomes redundant and can be removed with a one-line
`§7 history` entry citing the guard's commit.

**When to fold sections back into governance**: do NOT fold §3 rows
into `iteration_governance.md` §5.9. §5.9 is the rule; this file is the
operational implementation. Per `doc_governance.md` fold-back cadence,
governance docs absorb durable principles, not operational checklists.

## 7. History

| Date | Change | Source brief / context |
|---|---|---|
| 2026-06-07 | First-promotion landing (this file) from `docs/diagnostics/` per §5.9 promotion path. Initial §3 inventory (A1–A11) derived from M-Auto-6 sub-sprint surfaces + standing env hazards. | M-Auto-6 close prep; promoted ahead of the M-Auto-6 milestone-shared re-bless to keep smoke evidence cited from a stable canonical location. |
| 2026-06-07 | Drift fixes caught at the 2026-06-07 pre-flight NO-GO (5): DB name default `csagent_dev`→`csagent` (§0.2/§0.3); smoke command `python -m eval_interactive.cli run`→`python -m eval_interactive run` (Steps 1/2 + Appendix); full re-bless command → `cd autoloop && uv run python scripts/rebless_baseline.py …` (cwd=autoloop + `uv run`; `../` out-dir); §0.1 backend detection by port (`lsof -ti:8080`) not `ps aux \| grep '[s]pring-boot'`; §0.3 + A3 root-cause annotation (insert-only runner ⇒ `--reconcile`, not re-run `--ingest`). | S-Auto-28 / R8 #5; `docs/diagnostics/2026-06-07-m-auto-6-preflight-verdict.md` §"Drift caught" + `docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md` §"Pre-flight checklist contribution". |

---

## Appendix — quick reference

### Smoke commands

```bash
# Bad_cases smoke (Step 1)
python -m eval_interactive run \
    --set bad_cases \
    --parallel 1 \
    --label "smoke-preflight-bad-cases-$(date +%Y%m%d-%H%M%S)" \
    --verbose

# Anchor_outcome smoke (Step 2)
python -m eval_interactive run \
    --set anchor_outcome \
    --parallel 1 \
    --label "smoke-preflight-anchors-$(date +%Y%m%d-%H%M%S)" \
    --verbose
```

### Full re-bless command (post go-decision)

```bash
# Multi-suite n=9 paired-evidence baseline (the actual outcome run; only after smoke GO).
# The script requires cwd=autoloop + `uv run` (its own usage example uses --n 5);
# the --out-dir is therefore relative to autoloop/, hence the ../ prefix.
cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 9 \
    --out-dir ../eval_interactive/results/m-auto-N-baseline-shared-$(date +%Y%m%d)/
```

### Java baseline + pytest baseline pre-flight (orthogonal but cheap)

```bash
# Java baseline (~3-5 min)
cd server && mvn -o test 2>&1 | tail -20

# Pytest baselines (~1 min each)
cd eval_interactive && pytest -q 2>&1 | tail -5
cd autoloop && pytest -q 2>&1 | tail -5
```

Expected (current at 2026-06-07, post-M-Auto-6 dev-side close):

- Java: `1348 / 1 / 0 / 2` (sole failure = inherited OQ-S41.5,
  `SystemPromptUserRequestedTiebreakerTest`).
- autoloop pytest: `324`.
- eval_interactive pytest: `553` (548 + 5 inherited per
  `[[reference_eval_pytest_corpus_lint_conda_python]]`).
- UI vitest: `10 passed / 0 failed / 0 skipped`.
