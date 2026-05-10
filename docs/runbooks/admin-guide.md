---
title: CS Agent Demo — Admin & Operations Guide
doc_tier: runbook
status: current
implementation_status: partial
source_of_truth: server/src/main/resources/application.yml + server/src/main/java/com/gumtree/csagent/config/ + server/src/main/java/com/gumtree/csagent/controller/
last_reviewed: 2026-05-10
review_cadence: every 3-5 sprints
supersedes: []
superseded_by: null
notes: >
  Operational reference for the local/demo deployment shipped in this
  repo. The runtime described here is the only one that ships today.
  Production deployment artefacts (Dockerfile, helm-chart, Jenkinsfile,
  Salesforce live integration) are not in the repo; sections that
  describe a production-style capability are marked PRODUCTION_GAP.
  Sections that describe behaviour only available under the local
  Spring profile (mock Salesforce, mock business-hours toggle, mock
  Gumtree API) are marked LOCAL_ONLY.
---

# CS Agent Demo -- Admin & Operations Guide

> **Status legend used in this guide**
>
> - **CURRENT** — describes delivered behaviour in the local/demo runtime
>   that ships in this repo and is verifiable against `server/`.
> - **LOCAL_ONLY** — only active under the `local` Spring profile (mock
>   Salesforce, mock Gumtree API, mock business-hours toggle, demo
>   inspection endpoints). Not a production capability.
> - **PRODUCTION_GAP** — capability is referenced or partially supported
>   by code (e.g. K8s liveness/readiness endpoints exist) but the
>   surrounding production-readiness assets (Dockerfile, helm-chart,
>   Jenkinsfile, real Salesforce integration, secret management) are
>   **not present in this repo** and are out of scope for this guide.
>
> The whole repo today is the local/demo runtime. Treat any section
> below without an explicit marker as describing that local/demo
> runtime. Do not infer a production deployment exists.

## 1. Quick Start (CURRENT, LOCAL_ONLY)

```bash
# One-time setup
brew services start postgresql@17
brew services start redis
createdb csagent
psql csagent -c "CREATE EXTENSION IF NOT EXISTS vector;"
psql csagent -c "CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';"

# Build & ingest knowledge base
make build        # mvn clean install -DskipTests
make ingest       # Index 218 articles into pgvector

# Run (Makefile auto-loads .env.local for API keys)
make backend      # Spring Boot on :8080
make frontend     # Vite dev server on :5173 (separate terminal)

# Or run manually with env vars loaded:
set -a && source .env.local && set +a
cd server && mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Open `http://localhost:5173` for the chat UI, `http://localhost:5173/admin` for the admin panel.

---

## 2. Service Architecture

```
                    :5173                         :8080
 Browser ──────── Vite Dev ── /v1/* proxy ──── Spring Boot
                  Server                      │
                                              ├── PostgreSQL :5432  (csagent DB)
                                              └── Redis :6379       (cache)
```

| Component     | Port  | Process                                           |
|---------------|-------|---------------------------------------------------|
| Backend       | 8080  | `mvn spring-boot:run -Dspring-boot.run.profiles=local` |
| Frontend      | 5173  | `cd ui && npm run dev`                             |
| PostgreSQL 17 | 5432  | `brew services start postgresql@17`                |
| Redis         | 6379  | `brew services start redis`                        |

---

## 3. Makefile Targets

| Target        | Command                              | What it does                    |
|---------------|--------------------------------------|---------------------------------|
| `make setup`  | Start PG17 + Redis                   | Prerequisite services           |
| `make build`  | `mvn clean install -DskipTests`      | Compile server + eval modules   |
| `make ingest` | Spring Boot with `--ingest` flag     | Index knowledge articles        |
| `make backend`| Spring Boot with `local` profile     | Start API server on :8080       |
| `make frontend`| `cd ui && npm install && npm run dev`| Start Vite dev server on :5173 |
| `make eval-smoke`| `mvn verify -Peval-smoke`         | 75-session smoke test           |
| `make eval-full` | `mvn verify -Peval-full`          | 601-session full regression     |
| `make demo`   | setup + build + ingest + backend + frontend | Full startup sequence |
| `make clean`  | `mvn clean`                          | Remove build artifacts          |

---

## 4. Configuration Reference

### 4.1 Environment Variables (`.env.local`) (CURRENT)

LLM lineup as wired by `LlmClientConfig` (Sprint 8.1 follow-up #2,
2026-05-06): **DeepSeek is the primary chat-completion provider**;
**Kimi is the fallback** engaged only on transient primary failures
(5xx / 429 / network). DashScope is used **only for embeddings** by
`DashScopeEmbeddingClient`; the `dashscope.chat-model` property is
defined on `LlmProperties.DashScopeProperties` (default
`qwen-plus`) but no live code path uses it for chat today.

| Variable              | Purpose                        | Required |
|-----------------------|--------------------------------|----------|
| `DB_USERNAME`         | PostgreSQL user (default: postgres) | No  |
| `DB_PASSWORD`         | PostgreSQL password             | No       |
| `DB_HOST`             | PostgreSQL host (default: localhost) | No  |
| `DB_PORT`             | PostgreSQL port (default: 5432) | No       |
| `DB_NAME`             | Database name (default: csagent)| No       |
| `REDIS_HOST`          | Redis host (default: localhost) | No       |
| `REDIS_PORT`          | Redis port (default: 6379)     | No       |
| `DEEPSEEK_API_KEY`    | DeepSeek API key — **PRIMARY** chat-completion provider. Missing key is FATAL at startup (`LlmConfigValidator`). | Yes (for LLM) |
| `DEEPSEEK_BASE_URL`   | DeepSeek API endpoint (default: `https://api.deepseek.com/v1`) | No |
| `DEEPSEEK_MODEL`      | DeepSeek model name (default: `deepseek-v4-pro`; `.env.local` ships `deepseek-v4-flash`) | No |
| `KIMI_API_KEY`        | Kimi K2.6 API key — **FALLBACK** chat-completion provider. Missing key is WARN (fallback degradation). | Recommended |
| `KIMI_BASE_URL`       | Kimi API endpoint (default: `https://api.moonshot.ai/v1`; `.cn` legacy host triggers `base_url_legacy_warning`) | No |
| `KIMI_MODEL`          | Kimi model name (default: `kimi-k2.6`) | No |
| `DASHSCOPE_API_KEY`   | Alibaba DashScope API key (used **only for embeddings**) | Yes (for embeddings) |
| `DASHSCOPE_BASE_URL`  | DashScope endpoint (default: `https://dashscope.aliyuncs.com/compatible-mode/v1`) | No |
| `DASHSCOPE_CHAT_MODEL`| Configured on `LlmProperties.DashScopeProperties` (default `qwen-plus`) but **not used as a chat fallback today** — chat fallback is Kimi (see `LlmClientConfig`). | No |
| `DASHSCOPE_EMBEDDING_MODEL` | Embedding model name (default: `text-embedding-v3`) | No |
| `DASHSCOPE_EMBEDDING_DIMENSION` | Embedding vector dimension (default: 768) | No |

### 4.2 Application Config (`server/src/main/resources/`)

**application.yml** (defaults):
```yaml
server.port: 8080
spring.jpa.hibernate.ddl-auto: validate
spring.flyway.enabled: true
management.endpoints.web.exposure.include: health,metrics,prometheus
management.endpoint.health.show-details: always
```

**application-local.yml** (local profile overrides):
```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/csagent?stringtype=unspecified
spring.datasource.hikari.maximum-pool-size: 10
mock.business-hours: true     # Toggle online/offline escalation behavior
```

### 4.3 Runtime Config Toggle (LOCAL_ONLY)

`POST /v1/demo/config` is registered by `DemoInspectionController` and
flips `MockProperties.businessHours`, which is read by
`MockSalesforceService` to decide between `transferred` and
`offline_logged`. The endpoint and the property only have meaning
because the live profile uses the mock Salesforce implementation —
there is no production Salesforce client wired in this repo.

Toggle mock business hours at runtime (affects mock escalation
behaviour):

```bash
# Set to offline (handover result = "offline_logged")
curl -X POST http://localhost:8080/v1/demo/config \
  -H "Content-Type: application/json" \
  -d '{"business_hours": false}'

# Set to online (handover result = "transferred")
curl -X POST http://localhost:8080/v1/demo/config \
  -H "Content-Type: application/json" \
  -d '{"business_hours": true}'
```

---

## 5. Health Checks (CURRENT)

`HealthController` registers three endpoints under `/internal/health`.
The `liveness` / `readiness` shapes are K8s-probe compatible, but
**no Kubernetes manifests, Dockerfile, or helm chart ship in this
repo (PRODUCTION_GAP)** — wiring the probes into a cluster is a
production-readiness exercise outside this guide.

| Endpoint                        | Purpose                            | Checks          |
|---------------------------------|------------------------------------|-----------------|
| `GET /internal/health`          | Full health                        | DB + Redis      |
| `GET /internal/health/liveness` | K8s-probe-compatible liveness      | Always UP       |
| `GET /internal/health/readiness`| K8s-probe-compatible readiness     | DB + Redis      |

```bash
# Quick health check
curl -s http://localhost:8080/internal/health | python3 -m json.tool

# Expected output:
# { "status": "UP", "components": { "db": { "status": "UP" }, "redis": { "status": "UP" } } }
```

Returns **200** when healthy, **503** when any component is down.

---

## 6. Logging

The application uses Spring Boot default logging (SLF4J + Logback to stdout). No custom logback config file.

### 6.1 Viewing Logs

```bash
# When running via Makefile, logs go to stdout.
# To capture logs to a file:
cd server && mvn spring-boot:run -Dspring-boot.run.profiles=local 2>&1 | tee /tmp/csagent.log

# Follow logs in real-time:
tail -f /tmp/csagent.log

# Filter for errors only:
grep -E "ERROR|WARN|Exception" /tmp/csagent.log
```

### 6.2 Adjusting Log Level at Runtime

Add to `application-local.yml`:
```yaml
logging:
  level:
    root: INFO
    com.gumtree.csagent: DEBUG              # All app classes
    com.gumtree.csagent.service.runtime: DEBUG  # Core runtime detail
    com.gumtree.csagent.service.llm: DEBUG      # LLM call detail
    com.gumtree.csagent.service.knowledge: DEBUG # Knowledge pipeline
    com.gumtree.csagent.service.tools: DEBUG     # Tool dispatching
    com.gumtree.csagent.service.guardrails: DEBUG # Guardrail decisions
    org.flywaydb: INFO
    org.hibernate.SQL: DEBUG                 # Show SQL queries
    org.hibernate.type.descriptor.sql: TRACE # Show SQL bind params
```

### 6.3 Key Log Patterns to Watch (CURRENT)

Patterns below match strings actually emitted by the current code
(`LlmClientConfig`, `OpenAiCompatibleLlmClient`, `FallbackLlmClient`,
`LlmInvocationService`, `RerankService`, `DashScopeEmbeddingClient`,
`ForbiddenPhraseDetector`, `SessionManager`, `EventEmitter`).

| Pattern | What it means | Source |
|---------|---------------|--------|
| `LLM provider lineup:` | Startup summary line emitted once by `LlmClientConfig` after `LlmConfigValidator.validateOrThrow`. Format: `primary=deepseek[model=…, base=…, key=present\|placeholder\|blank], fallback=kimi[…]`. | `LlmClientConfig.llmClient` |
| `LLM config FATAL/WARN [<provider>/<code>]` | Per-diagnostic line from `LlmConfigValidator` (e.g. `api_key_blank`, `api_key_placeholder`, `base_url_legacy_warning`). FATAL aborts startup. | `LlmConfigValidator.validateOrThrow` |
| `LLM [chat:request] provider=… model=…` | Per-attempt outbound chat call. | `OpenAiCompatibleLlmClient` |
| `LLM [chat:response] provider=… model=… latency=… tokens=…` | Successful chat response. | `OpenAiCompatibleLlmClient` |
| `LLM [chat:retry]` / `LLM [chat:non-retryable-status]` / `LLM [chat:exhausted]` | Retry / abort decisions inside the per-provider client. | `OpenAiCompatibleLlmClient` |
| `LLM [chat:auth-error]` | 401/403 from the provider; check key + base URL. | `OpenAiCompatibleLlmClient` |
| `LLM [chat:fallback-engaged] primary=… failed transiently` | Fallback chain switched from DeepSeek to Kimi. | `FallbackLlmClient` |
| `LLM [chat:fallback-skipped-deadline-exceeded]` / `LLM [chat:fallback-skipped-budget]` / `LLM [chat:fallback-skipped-insufficient-budget]` | Fallback was not attempted because the wall-clock / attempt budget was already used. | `FallbackLlmClient` |
| `LLM [routing] response: latency=` | Routing-scenario LLM call completion. | `LlmInvocationService` |
| `LLM [rerank] scored … candidates` | Rerank-scenario LLM call completion. | `RerankService` |
| `Embedding request: provider=DashScope, model=… texts=…` | Embedding call. | `DashScopeEmbeddingClient` |
| `Embedding API call failed` | DashScope embedding API error; search returns empty. | `DashScopeEmbeddingClient` |
| `Forbidden phrase detected [<rule>]: '…' at position …` | Bot response contained a disallowed phrase. | `ForbiddenPhraseDetector` |
| `Mock case created: caseId=… useCaseId=… sessionId=…` (LOCAL_ONLY) | Mock Salesforce case creation. | `MockSalesforceService` |
| `Mock handover logged: logId=… sessionId=… result=transferred\|offline_logged` (LOCAL_ONLY) | Mock Salesforce handover write. | `MockSalesforceService` |
| `Session …: OUT_OF_SCOPE routing detail=` | Soft OOS routing decision. | `SessionManager` |
| `OUT_OF_SCOPE_HANDOVER` (event payload) | OOS topic detected, immediate handover. | `EventEmitter` / `SessionManager` |
| `"reasoning":"LLM invocation failure"` (in `bot_response`) | Sentinel marker for the bot's safe-escalation response when the LLM call could not be parsed. | `LlmInvocationService` / `ControlKernel` |

---

## 7. Database Operations

### 7.1 Connection

```bash
# Connect to csagent database
psql -U postgres csagent

# Or with explicit host/port
psql -h localhost -p 5432 -U postgres csagent
```

### 7.2 Key Tables

| Table               | Purpose                           | Key columns                    |
|---------------------|-----------------------------------|--------------------------------|
| `bot_sessions`      | All chat sessions                 | session_id, current_phase, active_use_case, handling_state, containment_outcome |
| `bot_turns`         | Per-turn traces                   | session_id, turn_index, user_message, projected_context, llm_raw_response, tool_calls, bot_response, source_ids, phase_before, phase_after, active_use_case, latency_ms |
| `bot_events`        | Event timeline                    | session_id, event_type, payload |
| `session_outcomes`  | Final session outcomes            | session_id, outcome, escalation_reason |
| `kb_articles`       | Knowledge base articles           | article_id, title, uc_tags     |
| `kb_chunks`         | Article chunks with embeddings    | chunk_id, article_id, embedding |
| `mock_cases`        | Mock Salesforce cases             | case_id, use_case_id, status   |
| `mock_handover_log` | Mock handover payloads            | session_id, handover_payload   |
| `flyway_schema_history` | Migration tracking            | version, description           |

### 7.3 Useful Queries

> **Schema note (CURRENT)**: Migration `V11__drop_action_columns_from_bot_turns.sql`
> dropped `bot_turns.action_selected` and `bot_turns.action_parameters`.
> Phase 0 deviation 2026-05-01 retired the action layer; semantic
> actions are now derived from `bot_turns.tool_calls` JSONB. Queries
> below select only columns that exist on the live table; if you have
> a custom dashboard or notebook still reading `action_selected` /
> `action_parameters`, update it to read from `tool_calls`.

```sql
-- Session overview
SELECT session_id, current_phase, active_use_case, handling_state,
       containment_outcome, total_bot_turns, created_at
FROM bot_sessions ORDER BY created_at DESC LIMIT 20;

-- Turn-by-turn trace for a session
SELECT turn_index, user_message, bot_response, tool_calls,
       phase_before, phase_after, latency_ms
FROM bot_turns
WHERE session_id = '<SESSION_ID>'
ORDER BY turn_index;

-- Event timeline for a session
SELECT event_type, turn_index, payload, created_at
FROM bot_events
WHERE session_id = '<SESSION_ID>'
ORDER BY created_at;

-- Handover payloads (inspect what gets sent to human agents)
SELECT session_id, transfer_result,
       handover_payload::jsonb->>'escalation_reason' AS reason,
       handover_payload::jsonb->>'primary_use_case' AS use_case,
       created_at
FROM mock_handover_log ORDER BY created_at DESC;

-- Session funnel
SELECT containment_outcome, COUNT(*) AS cnt
FROM bot_sessions
WHERE containment_outcome IS NOT NULL
GROUP BY containment_outcome;

-- Per-UC breakdown
SELECT active_use_case, containment_outcome, COUNT(*)
FROM bot_sessions
WHERE active_use_case IS NOT NULL
GROUP BY active_use_case, containment_outcome
ORDER BY active_use_case;

-- Knowledge base stats
SELECT COUNT(*) AS articles FROM kb_articles;
SELECT COUNT(*) AS chunks FROM kb_chunks;

-- Find sessions with errors (LLM fallback)
SELECT session_id, turn_index, bot_response
FROM bot_turns
WHERE bot_response LIKE '%technical issue%'
ORDER BY created_at DESC;

-- Escalation reasons
SELECT escalation_reason, COUNT(*) AS cnt
FROM bot_sessions
WHERE escalation_reason IS NOT NULL
GROUP BY escalation_reason
ORDER BY cnt DESC;
```

### 7.4 Database Reset

```bash
# Full reset (drop + recreate + re-apply migrations)
dropdb csagent
createdb csagent
psql csagent -c "CREATE EXTENSION IF NOT EXISTS vector;"
# Migrations run automatically on next backend start

# Partial reset (clear data, keep schema)
psql csagent -c "TRUNCATE bot_turns, bot_events, session_outcomes, mock_handover_log, mock_cases, bot_sessions CASCADE;"

# Clear only knowledge base (to re-ingest)
psql csagent -c "TRUNCATE kb_chunks, kb_articles CASCADE;"
```

---

## 8. Admin Panel (UI)

URL: `http://localhost:5173/admin`

### Tabs

| Tab            | Data source endpoint          | What it shows                         |
|----------------|-------------------------------|---------------------------------------|
| Sessions       | `GET /v1/demo/sessions`       | All sessions with UC, status, outcome |
| Traces         | `GET /v1/demo/sessions/{id}/trace` | Turn-by-turn decision trace      |
| Handover Logs  | `GET /v1/demo/handover-logs`  | Escalation payloads (JSON)            |
| Events         | `GET /v1/demo/sessions/{id}/events` | Chronological event timeline    |
| Metrics        | `GET /v1/demo/metrics/funnel` + `per-uc` | Funnel chart + per-UC table |

### Session Status Badges
- **active** (green) = `BOT_HANDLING`
- **escalated** (yellow) = `QUEUE_TO_HUMAN` or `HUMAN_HANDLING`
- **ended** (blue) = `CLOSED`

### Trace Viewer — LLM Interaction Detail (CURRENT)

Each step in the Trace tab includes a **"View LLM Detail"** button that
reveals the full App↔LLM interaction for that turn. The detail panel
shows the following sections; column "Source field on `BotTurn`" tracks
the live entity (`server/.../model/BotTurn.java`) after the
`V11__drop_action_columns_from_bot_turns.sql` migration removed the
old action-layer columns.

| Section | Content | Source field on `BotTurn` |
|---------|---------|--------------------------|
| **Metadata bar** | Phase transition, active UC, latency, source count | `phaseBefore`, `phaseAfter`, `activeUseCase`, `latencyMs`, `sourceIds` |
| **LLM Reasoning** | The model's internal chain-of-thought (not shown to the customer) | Parsed from `reasoning` key inside `llmRawResponse` JSON |
| **Projected Context** | Full context projection JSON sent to the LLM (session state, plan, allowed tools, budget, form/customer/listing context, conversation history, accumulated tool results) | `projectedContext` (jsonb) |
| **LLM Raw Response** | Complete model output JSON (`tool_calls`, `user_message`, `reasoning` per the v0.3 single-layer tool-use contract) | `llmRawResponse` |
| **Tool Calls** | Tool invocations issued by the LLM in this turn (current source for tool-dispatch detail after the V11 migration retired the action layer). | `toolCalls` (jsonb) |

**DB-side cleanup vs UI cleanup are separate (TODO_REVIEW for UI cleanup).**
On the **DB side**, `bot_turns.action_selected` and
`bot_turns.action_parameters` were retired/dropped by
`V11__drop_action_columns_from_bot_turns.sql` and are no longer
present on `BotTurn` / new traces. On the **UI side**,
`ui/src/components/admin/TraceViewer.tsx` still renders a
**conditional legacy "Action Parameters" panel** when an inbound
trace payload happens to carry `action_parameters` (e.g. older trace
data, replays, or external readers populating the optional
`action_parameters?` field on the `TraceStep` type in
`ui/src/types/index.ts`). New turns persisted by the current backend
do not populate this field, so the panel collapses, but the UI
cleanup is **not yet complete**. Treat the Tool Calls panel as the
canonical source for new traces and the Action Parameters panel as a
backward-compatibility fallback whose removal is a separate UI-side
task — out of scope for this docs PR (`docs/current/doc_governance.md`
forbids code edits in this PR scope).

The existing `GET /v1/demo/sessions/{id}/trace` endpoint returns the
full `BotTurn` entity including all the fields above.

---

## 9. REST API Quick Reference

### Chat API

```bash
# Create session
curl -X POST http://localhost:8080/v1/chat/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "first_name": "John",
    "email": "john@example.com",
    "topic_subject": "Ad Support",
    "ad_id": "AD-1001",
    "description": "My ad is not showing"
  }'

# Send message (use session_id from above)
curl -X POST http://localhost:8080/v1/chat/sessions/{session_id}/messages \
  -H "Content-Type: application/json" \
  -d '{"message": "Where is my ad?"}'

# Get session state
curl http://localhost:8080/v1/chat/sessions/{session_id}
```

### Demo Inspection API (LOCAL_ONLY)

The `/v1/demo/*` surface is registered by `DemoInspectionController` and
backed by the mock Salesforce / mock Gumtree API services that activate
under the `local` Spring profile. It is intended for the demo UI and
local diagnostics; it is not a production API surface.

```bash
# List all sessions
curl http://localhost:8080/v1/demo/sessions

# Get session trace (turn-by-turn)
curl http://localhost:8080/v1/demo/sessions/{session_id}/trace

# Get session events
curl http://localhost:8080/v1/demo/sessions/{session_id}/events

# List handover logs
curl http://localhost:8080/v1/demo/handover-logs

# List mock cases
curl http://localhost:8080/v1/demo/cases

# Funnel metrics
curl http://localhost:8080/v1/demo/metrics/funnel

# Per-UC metrics
curl http://localhost:8080/v1/demo/metrics/per-uc

# Get mock fixtures (type: accounts, listings, moderation_reviews, message_moderation)
curl http://localhost:8080/v1/demo/mock-data/accounts
```

### Knowledge API

```bash
# Search knowledge base
curl -X POST http://localhost:8080/v1/faq/search \
  -H "Content-Type: application/json" \
  -d '{"query": "how to post an ad", "uc_tags": ["UC-A", "UC-B"]}'
```

### Actuator

```bash
curl http://localhost:8080/internal/health           # Health check
curl http://localhost:8080/actuator/metrics           # All metrics
curl http://localhost:8080/actuator/prometheus         # Prometheus format
```

---

## 10. Knowledge Base Management

### Ingesting Articles

```bash
# First-time ingestion (or after clearing kb tables)
make ingest

# Manual invocation
cd server && mvn spring-boot:run -Dspring-boot.run.arguments=--ingest
```

The ingestion process:
1. Reads `data/knowledge/knowledge_base_articles.json` (218 articles)
2. Reads `data/knowledge/article_uc_mapping.csv` (UC tag mapping)
3. Cleans HTML, chunks text (256-512 tokens, 15% overlap)
4. Embeds chunks via DashScope API (768-dim, batches of 20)
5. Stores in `kb_articles` + `kb_chunks` tables

**Progress is logged** every 10 articles. Existing articles are skipped (idempotent).

### Re-ingesting

```bash
# Clear existing knowledge base
psql csagent -c "TRUNCATE kb_chunks, kb_articles CASCADE;"

# Re-run ingestion
make ingest
```

---

## 11. Eval Harness

### Running Evaluations

```bash
# Smoke suite (75 sessions, ~5 min) -- PR gate
make eval-smoke

# Full regression (601 sessions, ~30 min) -- release gate
make eval-full
```

**Prerequisite**: Backend must be running on `:8080`.

### Eval Configuration (`eval/src/main/resources/eval-application.yml`)

| Property               | Default                    | Description               |
|------------------------|----------------------------|---------------------------|
| `eval.bot-base-url`    | `http://localhost:8080`    | Bot API to test against   |
| `eval.suite`           | `smoke`                    | Suite name (smoke/full-regression) |
| `eval.report-dir`      | `target/eval-reports`      | Output directory for reports |
| `eval.dataset-dir`     | `../data/eval_datasets`    | Path to CSV datasets      |

### Reports

After eval runs, find reports in `eval/target/eval-reports/`:
- `report.html` -- Human-readable HTML with charts and tables
- `report.json` -- Machine-readable JSON with all metrics

### Hard Gates (must pass)

| Gate                        | Threshold |
|-----------------------------|-----------|
| `critical_policy_violation` | = 0       |
| `wrong_containment`         | <= 2%     |
| `groundedness_pass_rate`    | >= 98%    |
| `escalation_recall`         | >= 95%    |
| `handover_completeness`     | >= 98%    |
| `tool_scope_violation`      | = 0       |
| `forbidden_phrase`           | = 0       |
| `budget_enforcement`         | = 100%    |
| `phase_transition_validity`  | = 100%    |
| `critical_high_risk_escalation`| = 100%  |
| `out_of_scope_detection`     | >= 90%    |

---

## 12. Troubleshooting

### Backend won't start

| Symptom | Cause | Fix |
|---------|-------|-----|
| `Connection refused :5432` | PostgreSQL not running | `brew services start postgresql@17` |
| `FATAL: role "postgres" does not exist` | Missing DB role | `psql -c "CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';"` |
| `extension "vector" is not available` | pgvector not installed for PG version | Check `brew info pgvector`, ensure it matches your PG version |
| `relation "bot_sessions" does not exist` | Flyway migrations not applied | They run automatically on startup; check Flyway logs |
| `column is of type jsonb but expression is of type character varying` | Missing `?stringtype=unspecified` in JDBC URL | Check `application-local.yml` datasource URL |

### Verifying LLM provider/model in use (CURRENT)

After startup or config change, verify which LLM is active for each
scenario. Log strings below match the literal patterns emitted by
`LlmClientConfig`, `OpenAiCompatibleLlmClient`, `LlmInvocationService`,
`RerankService`, and `DashScopeEmbeddingClient`.

```bash
# 1. Check startup lineup summary (DeepSeek primary, Kimi fallback after Sprint 8.1 follow-up #2)
grep "LLM provider lineup" /tmp/csagent.log
# Expected (one line):
#   LLM provider lineup: primary=deepseek[model=deepseek-v4-flash, base=https://api.deepseek.com/v1, key=present], fallback=kimi[model=kimi-k2.6, base=https://api.moonshot.ai/v1, key=present]

# 2. Check per-attempt chat call (send a test message first)
grep "LLM \[chat:request\]" /tmp/csagent.log | tail -5
# Expected: provider=deepseek, model=deepseek-v4-flash on the primary path
grep "LLM \[chat:response\]" /tmp/csagent.log | tail -5

# 3. Check fallback engagement (only on transient primary failure)
grep "LLM \[chat:fallback-engaged\]" /tmp/csagent.log | tail -5

# 4. Check embedding provider (DashScope is the only embedding wiring today)
grep "Embedding request:" /tmp/csagent.log | tail -3
# Expected: provider=DashScope, model=text-embedding-v3

# 5. Scenario-specific checks
grep "LLM \[chat:" /tmp/csagent.log        # Bot conversation chat path
grep "LLM \[routing\]" /tmp/csagent.log     # UseCaseRouter LLM call
grep "LLM \[rerank\]" /tmp/csagent.log      # Knowledge reranking
```

### LLM not responding

| Symptom | Cause | Fix |
|---------|-------|-----|
| Bot says "experiencing a technical issue" | LLM API call failed (`LlmInvocationService` SAFE_ESCALATION_RESPONSE) | See below |
| `LLM [chat:auth-error]` in logs | 401/403 from primary provider | Check `DEEPSEEK_API_KEY` / `KIMI_API_KEY` in `.env.local`; sourced via `make backend` or `set -a && source .env.local && set +a` |
| `LLM config FATAL [deepseek/api_key_blank]` at startup | `DEEPSEEK_API_KEY` empty/unset; `LlmConfigValidator` aborts startup | Set `DEEPSEEK_API_KEY` (primary). Missing Kimi key is WARN, not FATAL. |
| `LLM [chat:fallback-engaged]` more often than expected | DeepSeek primary returning transient errors | Check provider dashboard; Kimi must remain configured to absorb the failover |
| `Embedding API call failed` in logs | DashScope embedding API error | Check `DASHSCOPE_API_KEY`; verify `kb_chunks` table has data |
| Slow responses (>10s) | API rate limiting / deadline exceeded (`LLM [chat:deadline-exceeded]`) | Check LLM provider dashboard for quota; review `LlmCallContext` deadlines |

**Common root cause**: Spring Boot does NOT auto-read `.env.local` files. The Makefile uses `include .env.local` + `export` to inject them as environment variables. If running manually outside Make, you must source the file first:

```bash
# Required before running spring-boot:run manually
set -a && source .env.local && set +a

# Verify keys are loaded (DeepSeek is the chat primary; Kimi the fallback; DashScope is embeddings only)
echo $DEEPSEEK_API_KEY    # Should show sk-...     (PRIMARY chat)
echo $KIMI_API_KEY        # Should show sk-...     (FALLBACK chat)
echo $DASHSCOPE_API_KEY   # Should show sk-...     (embeddings only)
```

### Frontend issues

| Symptom | Cause | Fix |
|---------|-------|-----|
| White page at `/admin` | JS error in admin components | Check browser console (F12) for errors |
| Chat form won't submit | Backend not running or CORS | Verify `:8080` is up; check Vite proxy config |
| API calls return 404 | Vite proxy not working | Verify `vite.config.ts` has `/v1` proxy to `:8080` |

### Database issues

```bash
# Check PostgreSQL is running
brew services list | grep postgres

# Check Redis is running
redis-cli ping    # Should return PONG

# Check pgvector extension
psql csagent -c "SELECT extversion FROM pg_extension WHERE extname='vector';"

# Check table existence
psql csagent -c "\dt"

# Check Flyway migration status
psql csagent -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

## 13. Process Management

### Starting all services

```bash
# Terminal 1: Infrastructure
brew services start postgresql@17
brew services start redis

# Terminal 2: Backend
cd /path/to/project/server
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 3: Frontend
cd /path/to/project/ui
npm run dev
```

### Stopping services

```bash
# Stop backend (Ctrl+C in terminal, or)
pkill -f "spring-boot:run"

# Stop frontend (Ctrl+C in terminal, or)
pkill -f "vite"

# Stop infrastructure (optional, persists between sessions)
brew services stop postgresql@17
brew services stop redis
```

### Running in background

```bash
# Backend (logs to file)
cd server && mvn spring-boot:run -Dspring-boot.run.profiles=local > /tmp/csagent.log 2>&1 &

# Frontend (logs to file)
cd ui && npm run dev > /tmp/csagent-ui.log 2>&1 &

# Check if running
curl -s http://localhost:8080/internal/health
curl -s http://localhost:5173 | head -1
```

---

## 13.4 Production deployment (PRODUCTION_GAP)

This repo does not ship production deployment artefacts. The following
are intentionally absent and out of scope for this guide:

- **No `Dockerfile`** at the repo root or under `server/`.
- **No Kubernetes manifests** (`k8s/`, `deploy/`) and **no helm chart**
  (`helm-chart/`). The K8s-probe-compatible `/internal/health/liveness`
  and `/internal/health/readiness` endpoints exist on `HealthController`
  but no chart consumes them.
- **No CI/CD pipeline** (`Jenkinsfile`, GitHub Actions workflow, GitLab
  CI). `make eval-smoke` / `make eval-full` are the only release-gate
  runners, executed locally.
- **No real Salesforce client.** `SalesforceService` is implemented only
  by `MockSalesforceService` (`@Profile("local")`). A production-grade
  implementation, OAuth wiring, queue routing, and `Chat_Message_Log__c`
  upsert path are not in this repo. See
  `docs/runbooks/salesforce-part-spec.md` for the target Salesforce
  configuration; treat that document as the production-readiness
  specification, not as live behaviour.
- **No production secret management.** `.env.local` is the only
  credential source. Wiring GCP Secret Manager / Vault / AWS Secrets
  Manager is a production-readiness exercise.
- **No observability backends wired.** `management.endpoints.web.exposure.include`
  exposes `prometheus` from `application.yml`, but no Prometheus
  scraper, dashboard, or alert routing is bundled with this repo.

If you need to deploy this beyond a developer laptop, treat the items
above as the production-readiness backlog rather than implicit defaults.

## 14. Event Types Reference (CURRENT)

The system emits 13 event types to `bot_events` (source:
`server/.../model/enums/EventType.java`):

| Event Type                 | When emitted                                     |
|----------------------------|--------------------------------------------------|
| `SESSION_STARTED`          | New session created from pre-chat form            |
| `USE_CASE_INFERRED`        | UC routing completed (strong prior or LLM)        |
| `RETRIEVAL_EXECUTED`       | Knowledge search performed                        |
| `ARTICLE_SHOWN`            | Knowledge article presented to user               |
| `CLARIFICATION_ASKED`      | Bot asked a clarifying question                   |
| `ESCALATION_REQUESTED`     | Handover to human initiated                       |
| `CASE_CREATED`             | Mock Salesforce case created (UC-H/J/K) — LOCAL_ONLY |
| `OUTCOME_RECORDED`         | Session outcome saved (RESOLVED/ESCALATED/etc.)   |
| `SESSION_CLOSED`           | Session fully closed                              |
| `TOOL_SCOPE_BLOCKED`       | Tool called outside allowed UC scope              |
| `GUARDRAIL_VIOLATION`      | Forbidden phrase detected in bot response         |
| `OUT_OF_SCOPE_HANDOVER`    | OOS topic detected, immediate handover            |
| `CLASSIFICATION_COMMITTED` | `classify_use_case` tool committed an `activeUseCase` during DISCOVER (see `runtime_contract.md` and `customer_service_tool_spec_v0_3.md`) |

Query events:
```bash
# All events for a session
curl http://localhost:8080/v1/demo/sessions/{session_id}/events

# Via SQL
psql csagent -c "SELECT event_type, COUNT(*) FROM bot_events GROUP BY event_type ORDER BY COUNT(*) DESC;"
```

---

## 15. Session State Machine

```
INIT ──► DISCOVER ──► RESOLVE ──► CONFIRM ──► CLOSE
              │           │           │
              ▼           ▼           ▼
           ESCALATE ◄────────────────┘
              │
              ▼
            CLOSE
```

| Phase     | What happens                                          |
|-----------|-------------------------------------------------------|
| INIT      | Form parsed, budgets loaded, auto-transition to DISCOVER |
| DISCOVER  | UC routing (two-stage), OOS detection                 |
| RESOLVE   | FAQ: knowledge search + grounded answer; INTAKE: collect fields |
| CONFIRM   | "Did that help?" -- Yes→CLOSE, No→retry or ESCALATE  |
| CLOSE     | Outcome recorded, session ended                       |
| ESCALATE  | Handover payload built, transferred to human queue    |

Query phase distribution:
```sql
SELECT current_phase, COUNT(*) FROM bot_sessions GROUP BY current_phase;
```
