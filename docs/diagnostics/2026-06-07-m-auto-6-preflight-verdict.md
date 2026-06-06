---
title: M-Auto-6 milestone-shared re-bless — pre-flight verdict (NO-GO)
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: this file (point-in-time pre-flight record); checks per `docs/current/process/preflight-eval-checks.md`
last_reviewed: 2026-06-07
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Pre-flight executed 2026-06-07 ahead of the M-Auto-6 milestone-shared
  real-LLM re-bless. Verdict NO-GO at §0.3. Root cause filed as
  docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md.
  The full --n 9 re-bless was NOT launched. No DB hand-patch applied
  (human direction). Backend left running fresh on V17 / current classpath.
---

# Pre-flight verdict — 2026-06-07 ~02:58 CST

**Target run (NOT launched):** `cd autoloop && uv run python scripts/rebless_baseline.py --n 9 --out-dir ../eval_interactive/results/m-auto-6-baseline-shared-20260607/`
**Verdict:** **NO-GO** (held at §0.3; Steps 1–4 not run)

## Step 0 — Env pre-flight

- **0.1 Backend restart — PASS.** Stale backend (PID 39786, started
  2026-06-05 18:52, predating the M-Auto-6 R5/R6/R7 commits) stopped; port
  released; fresh `mvn -o -pl server spring-boot:run` (profile `local`,
  datasource `jdbc:postgresql://localhost:5432/csagent`) booted on the
  current classpath. Boot log: `Started CsAgentApplication in 2.575 seconds`,
  `Tomcat started on port 8080`. Log: `/tmp/csagent-backend-preflight.log`.
- **0.2 Flyway migration state — PASS.** Boot log:
  `Current version of schema "public": 16` → `Migrating schema "public" to
  version "17 - add kb search knowledge eligible"` → `Successfully applied 1
  migration … now at version v17`; `Successfully validated 17 migrations`.
  (The migration file `V17__add_kb_search_knowledge_eligible.sql` is the
  highest on disk; pre-restart DB max was V16.)
- **0.3 Data-flag spot-check — NO-GO.** Both `(temp)` template articles
  read `search_knowledge_eligible = t`; all 218 `kb_articles` rows `t`:
  ```
  ka41r000000LIEJAA4 | t | (temp) Ad removed - By CS (general)
  ka41r000000LIEEAA4 | t | (temp) NTD Ad Removed Information
  t | 218
  ```
  Expected: both `(temp)` rows `false`. **Root cause:**
  `KnowledgeIngestionRunner` is insert-only and skips already-present
  articles (`KnowledgeIngestionRunner.java:84-105`), so the documented
  "JSON re-ingestion flips the 2 templates to FALSE" never applies to a
  populated table. The JSON source of truth is correct (exactly 2 articles
  flagged `false`); the DB cannot be reconciled via `--ingest`. Filed:
  `docs/diagnostics/failure-briefs/preflight-2026-06-07-kb-ingest-skip-existing-blocks-r6-flag.md`.
- **0.4 Env config sanity — PASS.** No `http_proxy`/`https_proxy`/`all_proxy`
  in the harness shell (clean). LLM creds present in env (DeepSeek /
  Moonshot-Kimi / Zhipu). Not the blocker.

## Steps 1–4 — NOT RUN

Held at §0.3 STOP before any real-LLM credit was burned. bad_cases smoke
(Step 1), anchor_outcome anti-误杀 sentinel (Step 2), anomaly scan
(Step 3), and the full `--n 9` re-bless (Step 5) were not executed.

## Verdict justification

**NO-GO on §0.3 (maps to anomaly A3 root-caused to an `infra` defect).**
R6's filter has no `false` article to act on, so a milestone re-bless would
read R6 as non-functional and spuriously trip A1/A2/A3 after spending
multi-suite `--n 9` credit. Remediation = structural reconcile path in
`KnowledgeIngestionRunner` (not a manual `UPDATE`; human direction
2026-06-07). After the reconcile path ships and §0.3 passes (2 rows
`false`, 216 `true`), resume the pre-flight from Step 1.

## Drift caught against `preflight-eval-checks.md` (for §6 maintenance)

1. **§0.3 example IDs are accurate** (`ka41r000000LIEJAA4` /
   `ka41r000000LIEEAA4`) — confirmed against the JSON source.
2. **DB name in §0.2/§0.3 is wrong:** runbook defaults to `csagent_dev`;
   actual DB is `csagent` (`DB_NAME:csagent` in `application-local.yml`).
3. **Smoke command form:** runbook shows `python -m eval_interactive.cli run`;
   the working invocation is `python -m eval_interactive run` (verified
   `--set bad_cases|anchor_outcome`, `--parallel`, `--label`, `-v`).
4. **Full re-bless command:** runbook appendix shows
   `python autoloop/scripts/rebless_baseline.py --n 9 --out-dir eval_interactive/results/…`;
   the script requires `cd autoloop && uv run python scripts/rebless_baseline.py
   --n 9 --out-dir ../eval_interactive/results/…` and its own usage example
   uses `--n 5`.
5. **§0.1 grep miss:** `ps aux | grep '[s]pring-boot'` does not match the
   forked `java -cp …` server JVM; detect the backend by port
   (`lsof -ti:8080`) instead.

(These are runbook-accuracy notes for a future `preflight-eval-checks.md`
update; not applied here pending confirmation.)
