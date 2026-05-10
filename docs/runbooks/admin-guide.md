# CS Agent Demo -- Admin & Operations Guide

## 1. Quick Start

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

### 4.1 Environment Variables (`.env.local`)

| Variable              | Purpose                        | Required |
|-----------------------|--------------------------------|----------|
| `DB_USERNAME`         | PostgreSQL user (default: postgres) | No  |
| `DB_PASSWORD`         | PostgreSQL password             | No       |
| `DB_HOST`             | PostgreSQL host (default: localhost) | No  |
| `DB_PORT`             | PostgreSQL port (default: 5432) | No       |
| `DB_NAME`             | Database name (default: csagent)| No       |
| `REDIS_HOST`          | Redis host (default: localhost) | No       |
| `REDIS_PORT`          | Redis port (default: 6379)     | No       |
| `KIMI_API_KEY`        | Kimi K2.6 API key (primary LLM for chat/routing/rerank) | Yes (for LLM) |
| `KIMI_BASE_URL`       | Kimi API endpoint (default: `https://api.moonshot.ai/v1`) | No |
| `KIMI_MODEL`          | Kimi model name (default: `kimi-k2.6`) | No |
| `DASHSCOPE_API_KEY`   | Alibaba DashScope API key (used for embeddings only) | Yes (for embeddings) |
| `DASHSCOPE_BASE_URL`  | DashScope endpoint             | No       |
| `DASHSCOPE_CHAT_MODEL`| DashScope chat model (fallback if Kimi key not set) | No |
| `DASHSCOPE_EMBEDDING_MODEL` | Embedding model name      | No       |

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

### 4.3 Runtime Config Toggle

Toggle mock business hours at runtime (affects escalation behavior):

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

## 5. Health Checks

| Endpoint                        | Purpose            | Checks          |
|---------------------------------|--------------------|-----------------|
| `GET /internal/health`          | Full health        | DB + Redis      |
| `GET /internal/health/liveness` | K8s liveness probe | Always UP       |
| `GET /internal/health/readiness`| K8s readiness probe| DB + Redis      |

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

### 6.3 Key Log Patterns to Watch

| Pattern | What it means |
|---------|---------------|
| `LLM config loaded` | Startup summary showing resolved provider/model config |
| `LLM request: provider=` | Per-call log showing which provider and model is used |
| `LLM response: model=` | Per-call response with model, latency, and token counts |
| `LLM [chat]` / `LLM [routing]` / `LLM [rerank]` | Scenario-specific LLM call |
| `Embedding request: provider=` | Embedding call with provider and model |
| `LLM invocation failed` | LLM API call failed, bot will use fallback response |
| `Embedding failed` | DashScope embedding API error, search returns empty |
| `Budget exceeded` | Session hit a turn/clarification/faq-miss limit |
| `Drift detected` | User changed topic mid-conversation |
| `Tool scope blocked` | Tool was called for a UC it's not allowed for |
| `Forbidden phrase detected` | Bot response contained a disallowed phrase |
| `PII redacted` | Personal data was redacted from logs/projection |
| `OUT_OF_SCOPE` | Topic not covered by any UC, escalating |

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
| `bot_turns`         | Per-turn traces                   | session_id, turn_index, user_message, bot_response, action_selected |
| `bot_events`        | Event timeline                    | session_id, event_type, payload |
| `session_outcomes`  | Final session outcomes            | session_id, outcome, escalation_reason |
| `kb_articles`       | Knowledge base articles           | article_id, title, uc_tags     |
| `kb_chunks`         | Article chunks with embeddings    | chunk_id, article_id, embedding |
| `mock_cases`        | Mock Salesforce cases             | case_id, use_case_id, status   |
| `mock_handover_log` | Mock handover payloads            | session_id, handover_payload   |
| `flyway_schema_history` | Migration tracking            | version, description           |

### 7.3 Useful Queries

```sql
-- Session overview
SELECT session_id, current_phase, active_use_case, handling_state,
       containment_outcome, total_bot_turns, created_at
FROM bot_sessions ORDER BY created_at DESC LIMIT 20;

-- Turn-by-turn trace for a session
SELECT turn_index, user_message, bot_response, action_selected,
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

### Trace Viewer — LLM Interaction Detail

Each step in the Trace tab now includes a **"View LLM Detail"** button that reveals
the full App↔LLM interaction for that turn. The detail panel shows:

| Section | Content | Source field on `BotTurn` |
|---------|---------|--------------------------|
| **Metadata bar** | Phase transition, active UC, latency, source count | `phaseBefore`, `phaseAfter`, `activeUseCase`, `latencyMs`, `sourceIds` |
| **LLM Reasoning** | The model's internal chain-of-thought (not shown to the customer) | Parsed from `reasoning` key inside `llmRawResponse` JSON |
| **Projected Context** | Full context projection JSON sent to the LLM (session state, allowed actions, budget, form/customer/listing context, conversation history, knowledge hits) | `projectedContext` (jsonb) |
| **LLM Raw Response** | Complete model output JSON (`action`, `parameters`, `user_message`, `reasoning`) | `llmRawResponse` |
| **Action Parameters** | Parsed action-specific parameters | `actionParameters` (jsonb) |

No backend changes are required — the existing `GET /v1/demo/sessions/{id}/trace`
endpoint already returns the full `BotTurn` entity including all fields above.
The frontend `mapTrace` function and `TraceStep` type have been extended to
carry these fields through to the UI.

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

### Demo Inspection API

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

### Verifying LLM provider/model in use

After startup or config change, verify which LLM is active for each scenario:

```bash
# 1. Check startup config summary
grep "LLM config loaded" /tmp/csagent.log
# Expected: kimi=[model=kimi-k2.6, baseUrl=https://api.moonshot.ai/v1], dashscope=[chatModel=qwen-plus, ...]

# 2. Check per-request provider selection (send a test message first)
grep "LLM request:" /tmp/csagent.log | tail -5
# Expected: provider=Kimi, model=kimi-k2.6 for chat/routing/rerank

# 3. Check embedding provider
grep "Embedding request:" /tmp/csagent.log | tail -3
# Expected: provider=DashScope, model=text-embedding-v3

# 4. Scenario-specific checks
grep "LLM \[chat\]" /tmp/csagent.log      # Bot conversation turns
grep "LLM \[routing\]" /tmp/csagent.log    # UC classification
grep "LLM \[rerank\]" /tmp/csagent.log     # Knowledge reranking
```

### LLM not responding

| Symptom | Cause | Fix |
|---------|-------|-----|
| Bot says "experiencing a technical issue" | LLM API call failed | See below |
| `401 Unauthorized` in logs | API keys not loaded | Run via `make backend` (auto-loads `.env.local`), or manually: `set -a && source .env.local && set +a` before starting |
| `LLM request: provider=DashScope` when expecting Kimi | `KIMI_API_KEY` not set | Check `.env.local` has `KIMI_API_KEY=sk-...` and was sourced |
| Knowledge search returns empty | Embedding API failed | Check `DASHSCOPE_API_KEY`; verify `kb_chunks` table has data |
| Slow responses (>10s) | API rate limiting | Check LLM provider dashboard for quota |

**Common root cause**: Spring Boot does NOT auto-read `.env.local` files. The Makefile uses `include .env.local` + `export` to inject them as environment variables. If running manually outside Make, you must source the file first:

```bash
# Required before running spring-boot:run manually
set -a && source .env.local && set +a

# Verify keys are loaded
echo $DASHSCOPE_API_KEY   # Should show sk-f4ada...
echo $KIMI_API_KEY        # Should show sk-GXVVb...
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

## 14. Event Types Reference

The system emits 12 event types to `bot_events`:

| Event Type              | When emitted                                     |
|-------------------------|--------------------------------------------------|
| `SESSION_STARTED`       | New session created from pre-chat form            |
| `USE_CASE_INFERRED`     | UC routing completed (strong prior or LLM)        |
| `RETRIEVAL_EXECUTED`    | Knowledge search performed                        |
| `ARTICLE_SHOWN`         | Knowledge article presented to user               |
| `CLARIFICATION_ASKED`   | Bot asked a clarifying question                   |
| `ESCALATION_REQUESTED`  | Handover to human initiated                       |
| `CASE_CREATED`          | Mock Salesforce case created (UC-H/J/K)           |
| `OUTCOME_RECORDED`      | Session outcome saved (RESOLVED/ESCALATED/etc.)   |
| `SESSION_CLOSED`        | Session fully closed                              |
| `TOOL_SCOPE_BLOCKED`    | Tool called outside allowed UC scope              |
| `GUARDRAIL_VIOLATION`   | Forbidden phrase detected in bot response         |
| `OUT_OF_SCOPE_HANDOVER` | OOS topic detected, immediate handover            |

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
