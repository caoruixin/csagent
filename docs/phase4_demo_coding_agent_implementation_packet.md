# Phase 4 — Demo Coding Agent Implementation Packet

> **Purpose**: Adapt the production Phase 4 into a **local Mac demo** that lets stakeholders experience the CS Agent end-to-end — without Salesforce, Gumtree APIs, GKE, or Vertex AI dependencies. All external dependencies are mocked; all infrastructure runs natively on Mac via Homebrew.
>
> **Tech Stack**: Java 17 + Spring Boot 3.2.x (aligned with `07-engineering-constraints.md`). Eval harness, knowledge ingestion, and graders are all Java. Frontend is React + Vite (TypeScript).
>
> **Inputs**: All Phase 0–5 design documents, `customer_service_tool_spec_v0_2.yaml`, eval datasets (601 sessions), human review annotations (367 sessions), `.env.local` (LLM keys), `design-extract/` (UI reference)

---

## D0. Local Mac Environment Setup

### D0.1 Prerequisites

| Component | Version | Install | Purpose |
|-----------|---------|---------|---------|
| Java | 17+ | `brew install openjdk@17` | Backend + eval harness |
| Maven | 3.9+ | `brew install maven` | Build tool |
| Node.js | 20+ LTS | `brew install node` | Frontend build |
| PostgreSQL | 16 | `brew install postgresql@16` | Session state + knowledge vectors |
| pgvector | 0.7+ | `brew install pgvector` | Vector similarity search extension |
| Redis | 7+ | `brew install redis` | Cache layer |
| Git | Latest | Pre-installed on Mac | — |

### D0.2 Local Service Setup (Homebrew, no Docker)

```bash
# ── 1. PostgreSQL ──
brew install postgresql@16
brew services start postgresql@16

# Create database and user
createdb csagent
psql csagent -c "CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';"

# Install pgvector extension
brew install pgvector
psql csagent -c "CREATE EXTENSION IF NOT EXISTS vector;"

# ── 2. Redis ──
brew install redis
brew services start redis

# ── 3. Verify ──
psql csagent -c "SELECT extversion FROM pg_extension WHERE extname='vector';"
# → should show 0.7.x or 0.8.x
redis-cli ping
# → PONG
```

### D0.3 Environment Configuration

```properties
# .env.local (project root — already partially exists)

# ── Spring ──
SPRING_PROFILES_ACTIVE=local

# ── Database ──
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csagent
DB_USERNAME=postgres
DB_PASSWORD=postgres

# ── Redis ──
REDIS_HOST=localhost
REDIS_PORT=6379

# ── LLM (OpenAI-compatible) ──
KIMI_API_KEY=sk-GXVVb0AJBZQpLBfWbOl9CL8QWU359ChkCGiPUZTbfsxaUVV7
KIMI_BASE_URL=https://api.moonshot.ai/v1
KIMI_MODEL=kimi-k2.6

DASHSCOPE_API_KEY=sk-f4ada48271a54810a13147ac708555dd
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_CHAT_MODEL=qwen-plus
DASHSCOPE_EMBEDDING_MODEL=text-embedding-v3
DASHSCOPE_EMBEDDING_DIMENSION=768

# ── Chat UI ──
GETSTREAM_API_KEY=b2g8vgf3xue3
GETSTREAM_API_SECRET=qrsvyrpgx4ynfr7tgz2yq83tr3sudg6yjfxz95ads76ye2abv32vmp6hsr3etjwy
VITE_GETSTREAM_API_KEY=b2g8vgf3xue3
VITE_API_BASE_URL=http://localhost:8080
```

**LLM Abstraction**: all LLM calls go through a `LlmClient` interface. Local demo uses `OpenAiCompatibleLlmClient` (Kimi / QWEN both support OpenAI chat completions format). Dev/prod implementations swap in via Spring profile — no code changes.

**Embedding Abstraction**: `EmbeddingClient` interface. Local uses `DashScopeEmbeddingClient` with `text-embedding-v3` (dimension=768, matching production Vertex AI 768-dim). Swap by profile.

---

## D1. Scope Freeze

### This demo covers

- **CS Agent core runtime**: control kernel, state machine (INIT→DISCOVER→RESOLVE→CONFIRM→CLOSE/ESCALATE), context projection, LLM invocation — functionally identical to production
- **Knowledge pipeline**: 218 articles → chunk → embed (DashScope 768-dim) → pgvector → online retrieval with two-stage faq_miss gate
- **All 10 V1 tools**: 5 agent-visible + 5 runtime-only (v0.2.1, including `get_message_moderation_context`); external API tools return mock data
- **Guardrails**: fixed_script_library (14+OOS categories), forbidden phrase detection, PII redaction — identical to production
- **Demo Chat UI**: React + Vite web app with Gumtree-inspired design; pre-chat form → chat → resolution/escalation + admin inspection panel
- **Mock Salesforce layer**: case CRUD, session management, handover payload logging — all local PostgreSQL, fully inspectable
- **Mock Gumtree APIs**: account lookup, listing lookup, moderation review, message moderation — JSON fixture-based
- **Observability (local)**: structured events → local PostgreSQL; trace viewer; per-turn traces; funnel metrics — no Kafka (direct DB)
- **Eval harness (Java replay)**: dataset loader, session simulator, code + model graders, gate evaluator, HTML/JSON report — runs as Maven test profile or standalone JAR
- **Interactive eval harness (Python)**: CaseSpec-driven interactive evaluation with LLM-based user simulation, 3-layer scoring (Hard Checks → Outcome Checks → LLM Judge), stall detection, before/after comparison, and batch execution — runs as Python CLI (`eval_interactive/`)

### This demo does NOT cover

- Real Salesforce / Gumtree API integration (mocked)
- GKE / Helm / Jenkins deployment
- Kafka event publishing (local DB)
- GrowthBook feature flags (always-on)
- Email CSAT surveys
- Multi-user concurrent load testing
- Interactive eval as CI hard gate (advisory in V1; see Phase 5 §15.1 for promotion plan)

---

## D2. Module Breakdown

### DM1: Service Foundation & Local Infrastructure

| Aspect | Detail |
|--------|--------|
| **What** | Spring Boot 3.2.x project skeleton; Flyway DB migrations (all tables from Phase 3 §3.2 + §3.5 + demo-only mock tables); Spring profiles (local/dev/prod); LLM client abstraction (`LlmClient` interface); embedding client abstraction (`EmbeddingClient` interface); health endpoints; HikariCP connection pool for local PostgreSQL |
| **Output** | `mvn spring-boot:run` → service starts on `localhost:8080` with health checks green at `/internal/health` |
| **Key files** | `pom.xml`, `application.yml`, `application-local.yml`, `db/migration/V1-V7*.sql`, `LlmClient.java`, `OpenAiCompatibleLlmClient.java`, `EmbeddingClient.java`, `DashScopeEmbeddingClient.java` |

**DB Migrations** (Flyway, all in `src/main/resources/db/migration/`):

| Migration | Table(s) | Source |
|-----------|----------|--------|
| `V1__create_bot_sessions.sql` | `bot_sessions` | Phase 3 §3.2.2 (incl. OUT_OF_SCOPE_* in `active_use_case`) |
| `V2__create_bot_turns.sql` | `bot_turns` | Phase 3 §3.2.3 |
| `V3__create_session_outcomes.sql` | `session_outcomes` | Phase 3 §3.2.4 |
| `V4__create_kb_articles.sql` | `kb_articles` | Phase 3 §3.5.1 |
| `V5__create_kb_chunks.sql` | `kb_chunks` + HNSW index | Phase 3 §3.5.1 |
| `V6__create_bot_events.sql` | `bot_events` | Phase 3 §3.2.1 (local equivalent of Bot_Event__c) |
| `V7__create_mock_tables.sql` | `mock_cases`, `mock_handover_log` | **Demo-only**: inspectable mock Salesforce data |

### DM2: Knowledge Pipeline (Java)

| Aspect | Detail |
|--------|--------|
| **What** | **Offline ingestion**: Spring Boot command-line runner (`KnowledgeIngestionRunner`) — reads `knowledge_base_articles.json` + `article_uc_mapping.csv` → clean HTML (Jsoup) → chunk (sliding window 256-512 tokens, 10-20% overlap) → embed via DashScope `text-embedding-v3` (768-dim) → JDBC bulk insert to pgvector. **Online retrieval**: `KnowledgeSearchService` — ANN Top-20 → metadata filter → article dedup → LLM-based rerank → two-stage faq_miss gate → return top 3 |
| **Output** | `mvn spring-boot:run -Dspring-boot.run.arguments=--ingest` → 218 articles / ~500 chunks indexed; `POST /v1/faq/search` returns results with `faq_miss` flag |
| **Key files** | `KnowledgeIngestionRunner.java`, `ChunkingService.java`, `KnowledgeSearchService.java`, `KnowledgeSearchController.java`, `RerankService.java` |
| **Data inputs** | `data/knowledge/knowledge_base_articles.json`, `data/knowledge/article_uc_mapping.csv` |

**Retrieval contract** (identical to production Phase 3 §3.5.3, updated v9):
```
Query → Query Enrichment (v9: augment with form_context.description if short/early turn)
  → Embed(768-dim) → pgvector ANN(ef_search=100, LIMIT 20, uc_tags filter)
  → Retrieval Gate (top1 cosine_sim < 0.3 → retrieval_miss)
  → Article dedup (highest chunk per article)
  → Rerank top 4-8 (LLM grounding score 1-5, v9: parallel CompletableFuture)
  → Answer Gate (grounding_score < 3.5 → answer_miss)
  → Return top 3 { source_id, title, snippet, canonical_url, score }
  → faq_miss = retrieval_miss OR answer_miss
```

### DM3: Mock Integration Layer

| Aspect | Detail |
|--------|--------|
| **What** | Spring `@Profile("local")` beans that replace real external service clients. All mocks backed by local PostgreSQL or JSON fixtures for inspectability. **Handover payloads logged to `mock_handover_log` table** — each entry contains full JSON matching Phase 3 §3.6.2 schema. Configurable `mock.business_hours` flag (toggle online/offline scenarios at runtime) |
| **Output** | All tool chains work E2E with realistic mock data; handover payloads inspectable via API |
| **Key files** | `MockSalesforceService.java`, `MockGumtreeApiService.java`, `MockDataInitializer.java`, `DemoInspectionController.java` |

**Mock strategy per external service**:

| External Service | Mock Approach | Inspectable At |
|-----------------|---------------|----------------|
| **SF Case** | Pre-seeded in `mock_cases`; `create_case_controlled` writes here | `GET /v1/demo/cases` |
| **SF Omni-Channel Transfer** | Logs to `mock_handover_log`; returns `transferred`/`offline_logged` per `mock.business_hours` | `GET /v1/demo/handover-logs` |
| **SF Bot_Session / Bot_Event** | Written to local `bot_sessions` / `bot_events` (same schema as prod) | `GET /v1/demo/sessions`, `GET /v1/demo/events` |
| **Gumtree bapi-server** (account/listing) | JSON fixtures in `src/main/resources/mock/` | `GET /v1/demo/mock-data/{type}` |
| **Gumtree gumshield** (moderation review) | Fixtures with per-ad_id review reasons | Same |
| **Gumtree message-moderation-history** | Fixtures for UC-C diagnostic | Same |
| **Business hours** | `mock.business_hours=true/false` in `application-local.yml` | Runtime toggle via `POST /v1/demo/config` |

**Mock Fixture Directory** (`src/main/resources/mock/`):
```
mock/
├── accounts/                     # Per-email account state
│   ├── active_user.json
│   ├── suspended_user.json
│   └── blacklisted_user.json
├── listings/                     # Per-ad_id listing state
│   ├── live_ad.json
│   ├── removed_policy.json
│   ├── removed_multiple_accounts.json
│   └── processing_ad.json
├── moderation_reviews/           # Per-ad_id review reason
│   ├── multiple_accounts.json
│   ├── prohibited_content.json
│   └── pet_limit.json
├── message_moderation/           # UC-C diagnostic fixtures
│   ├── clean.json
│   └── blocked_messages.json
└── cases/                        # Pre-seeded Case fixtures
    ├── ad_support.json
    ├── account_support.json
    └── gdpr_request.json
```

### DM4: Core Runtime

| Aspect | Detail |
|--------|--------|
| **What** | Functionally identical to production M4 — session lifecycle, `FormContextIngestionService`, Control Kernel (6 phases + transitions from Phase 3 §3.3), budget enforcement, drift detection (v8: 90.2% sessions have drift), context projection (Phase 3 §3.2.6), `UseCaseRouter` (two-stage classification, v8 HR-calibrated), LLM invocation via `OpenAiCompatibleLlmClient` |
| **Output** | Given user message + session state → action decision + bot response |
| **Key files** | `SessionManager.java`, `FormContextIngestionService.java`, `ControlKernel.java`, `PhaseEvaluator.java`, `BudgetChecker.java`, `DriftDetector.java`, `UseCaseRouter.java`, `ContextProjectionBuilder.java`, `LlmInvocationService.java`, `ActionParser.java` |

**LLM structured output** — Bot expects JSON from LLM:
```json
{
  "action": "answer_grounded",
  "parameters": { "query": "..." },
  "user_message": "Here's what I found...",
  "reasoning": "User asks about ad removal..."
}
```
Actions: `ask_user` | `retrieve_knowledge` | `answer_grounded` | `escalate_human` | `finish`

**Two-stage UC routing** (v8 HR-calibrated, Phase 2 §2.11.2):
- Stage 1: Topic Subject prior (strong >85% → direct route UC-G/J/C; weak → Stage 2; handover-only → check Description then OOS)
- Stage 2: Description-based LLM classification within UC candidate set
- **Account Support special path**: skip Stage 1, full-candidate-set classification (HR accuracy only 18.2%)
- **OUT_OF_SCOPE**: handover-only TS + no UC match → `OUT_OF_SCOPE_{TOPIC}` → fixed_script → ESCALATE

**Control budgets** (Phase 3 §3.3.4):
- `max_clarification_rounds` = 2
- `max_faq_miss` = 2
- `max_bot_turns_per_issue` = 15 (FAQ) / 10 (Intake)
- `max_repeated_same_action` = 2
- `max_total_bot_turns` = 25

### DM5: Tool Layer

| Aspect | Detail |
|--------|--------|
| **What** | `ToolDispatcher` + `ToolPolicyEnforcer`; all 10 V1 tools (5 agent-visible + 5 runtime-only v0.2.1). External API tools delegate to DM3 mock beans. Knowledge tools use real pgvector. Policy enforcement identical to production |
| **Output** | Any tool callable by name; UC scope violations → `scope_blocked` event; runtime-only tools triggered by policy |
| **Key files** | `ToolDispatcher.java`, `ToolPolicyEnforcer.java`, + per-tool implementations |

**Tool matrix enforcement** (Phase 2 §2.10):
| Tool | Allowed UCs |
|------|------------|
| `search_knowledge` / `resolve_article` | UC-A/B/C/D/E/F/FP |
| `get_customer_context` | UC-A/C/D/F/FP/K → delegates to mock layer |
| `get_message_moderation_context` | UC-C only (v0.2.1) |
| `create_case_controlled` | UC-H/J/K → writes to `mock_cases` |
| `request_handover` / `record_outcome` | All UCs + OUT_OF_SCOPE_* |

### DM6: Guardrails & UX

| Aspect | Detail |
|--------|--------|
| **What** | Identical to production — `ScriptLibraryService` (YAML templates from `fixed_script_library_v1.md`), `ForbiddenPhraseDetector` (regex from Phase 3 §3.7.3), `PiiRedactionFilter` (4 layers from Phase 3 §3.7.4), `ProgressPlaceholderService` (1.5s async timeout) |
| **Output** | Correct template selected per UC + phase; forbidden phrases caught before sending; PII stripped from logs/traces |
| **Key files** | `ScriptLibraryService.java`, `ForbiddenPhraseDetector.java`, `PiiRedactionFilter.java`, `ProgressPlaceholderService.java`, `src/main/resources/scripts/*.yaml` |

**Template categories** (14 + OOS): opening, empathy, hold_placeholder, identifier_request, resolution_check, escalation_business_hours, escalation_off_hours, gdpr_intake, appeal_intake, dispute_disclaimer, safety_intake, tech_troubleshoot, policy_explanation, idle_close, oos_delivery, oos_pro_contract, oos_account_manager, oos_ratings_reviews, oos_generic

### DM7: Observability (Local)

| Aspect | Detail |
|--------|--------|
| **What** | `EventEmitter` → local PostgreSQL `bot_events` table (replaces Kafka + SF sync in demo). `TraceWriter` → `bot_turns` table. Micrometer metrics at `/internal/metrics`. Demo inspection REST endpoints for traces, events, handover logs, session funnel |
| **Output** | Every significant action emits structured event; full turn-by-turn trace queryable; funnel metrics available |
| **Key files** | `EventEmitter.java`, `TraceWriter.java`, `LocalEventStore.java`, `DemoInspectionController.java` |

**Events** (12 types, same as production): `SESSION_STARTED`, `USE_CASE_INFERRED`, `RETRIEVAL_EXECUTED`, `ARTICLE_SHOWN`, `CLARIFICATION_ASKED`, `ESCALATION_REQUESTED`, `CASE_CREATED`, `OUTCOME_RECORDED`, `SESSION_CLOSED`, `TOOL_SCOPE_BLOCKED`, `GUARDRAIL_VIOLATION`, `OUT_OF_SCOPE_HANDOVER`

**Demo inspection endpoints**:
| Endpoint | Returns |
|----------|---------|
| `GET /v1/demo/sessions` | All bot sessions with outcome + UC + turns count |
| `GET /v1/demo/sessions/{id}/trace` | Full turn-by-turn trace (projected context, LLM response, action, tool calls, state snapshot) |
| `GET /v1/demo/sessions/{id}/llm-calls` | All LLM calls (routing, chat, rerank, retry) with model/tokens/latency (v9 §D14.3) |
| `GET /v1/demo/sessions/{id}/events` | Chronological event timeline |
| `GET /v1/demo/handover-logs` | All handover payloads (full JSON per Phase 3 §3.6.2 schema) |
| `GET /v1/demo/metrics/funnel` | Aggregate funnel: total / understood / resolved / escalated / abandoned |

### DM8: Demo Chat UI

| Aspect | Detail |
|--------|--------|
| **What** | React + Vite + TypeScript web app. Gumtree-inspired design (colors/fonts from `design-extract/`). Two views: (1) **Customer chat** — pre-chat form → conversation → resolution/escalation; (2) **Admin panel** — session list, trace viewer, handover log inspector, event timeline, funnel dashboard |
| **Output** | `http://localhost:5173` — customer chat; `http://localhost:5173/admin` — admin/inspection |
| **Key files** | `ui/` directory |
| **Dependencies** | DM4 backend running on `localhost:8080` |

**UI components**:
```
ui/src/
├── components/
│   ├── chat/
│   │   ├── ChatWidget.tsx            # Floating bottom-right chat drawer
│   │   ├── PreChatForm.tsx           # first_name*, email*, topic_subject* (11 options), ad_id, description*
│   │   ├── MessageBubble.tsx         # User (right, grey) / Bot (left, teal) bubbles
│   │   ├── ArticleCard.tsx           # Knowledge article: title + summary + canonical_url link
│   │   ├── ResolutionCheck.tsx       # "Did that answer your question?" → Yes / No buttons
│   │   ├── EscalationNotice.tsx      # Handover message with case number + next steps
│   │   ├── TypingIndicator.tsx       # Bot "thinking" animation
│   │   └── ChatInput.tsx             # Text input + Send button (green CTA)
│   ├── admin/
│   │   ├── SessionList.tsx           # Table: session_id, UC, outcome, turns, timestamp
│   │   ├── TraceViewer.tsx           # Turn-by-turn: user msg → projected context → LLM response → action → state; includes LlmDetailPanel for full App↔LLM interaction view (projected context, raw LLM response with reasoning, action parameters, phase transitions)
│   │   ├── HandoverLogViewer.tsx     # JSON tree viewer for handover payloads
│   │   ├── EventTimeline.tsx         # Vertical timeline of bot events
│   │   └── MetricsDashboard.tsx      # Funnel chart + per-UC breakdown
│   └── layout/
│       ├── Header.tsx                # Gumtree-style nav (teal #0D475C)
│       └── DemoPage.tsx              # Landing page hosting chat widget
├── api/client.ts                     # Axios client for backend
├── styles/gumtree-theme.css          # Tokens: #0D475C, #5CE00B, Readex Pro, 8px radius
├── App.tsx
└── main.tsx
```

**Design tokens** (from `design-extract/`):
| Token | Value |
|-------|-------|
| Primary | `#0D475C` (Gumtree teal) |
| CTA | `#5CE00B` (lime green) |
| Text | `#000` / `#3C3241` / `#635B67` |
| Background | `#FFF` / `#F1F1F1` |
| Font | "Readex Pro", sans-serif |
| Card | radius 8px, shadow `rgba(0,0,0,0.2) 0 0 12px` |

**Chat API** (UI ↔ backend):
```
POST /v1/chat/sessions                    # Pre-chat form submit → { session_id, reply_text }
POST /v1/chat/sessions/{id}/messages      # User message → { reply_text, intent, should_end_chat, additional_data }
GET  /v1/chat/sessions/{id}               # Get current session state
```

### DM9: Eval Harness (Java Replay)

| Aspect | Detail |
|--------|--------|
| **What** | Java-based replay eval framework for CI gate. Runs as Maven test profile (`mvn verify -Peval-smoke`) or standalone Spring Boot runner. Loads 7 CSV dataset pairs + 367 HR annotations. Replays sessions via HTTP to bot runtime. Runs code-based graders (12 types) + model-based graders (4 types, using same `LlmClient`). Computes all Phase 5 §6 metrics. Evaluates Phase 5 §7 gates. Generates HTML + JSON report. **See DM11 (§D13) for interactive eval.** |
| **Output** | `mvn verify -Peval-smoke` → 75 sessions, ~5min, all hard gates checked; `mvn verify -Peval-full` → 601 sessions, ~30min, full report |
| **Key files** | `eval/` Maven submodule |

**Eval module structure** (`eval/`):
```
eval/
├── src/main/java/com/gumtree/csagent/eval/
│   ├── harness/
│   │   ├── EvalRunner.java               # Main orchestrator
│   │   ├── DatasetLoader.java            # CSV parser + HR annotations overlay
│   │   ├── SessionSimulator.java         # HTTP replay through bot runtime
│   │   ├── FormNormalizer.java           # Old form → new form field mapping
│   │   └── ResultCollector.java          # Per-session result aggregation
│   ├── graders/
│   │   ├── code/
│   │   │   ├── RoutingGrader.java        # UC routing accuracy (exact match)
│   │   │   ├── EscalationGrader.java     # Recall, precision, timing, wrong containment
│   │   │   ├── HandoverGrader.java       # Payload completeness (JSON schema validation)
│   │   │   ├── ControlGrader.java        # Budget enforcement, phase transitions
│   │   │   ├── ToolContractGrader.java   # Scope enforcement, sequence matching
│   │   │   ├── PolicyGrader.java         # Forbidden phrases, PII leakage
│   │   │   └── DriftGrader.java          # Drift type, issue preservation
│   │   └── model/
│   │       ├── GroundednessGrader.java   # LLM judge: GROUNDED / NOT_GROUNDED
│   │       ├── RelevanceGrader.java      # LLM judge: 1-5 relevance
│   │       ├── SummaryQualityGrader.java # LLM judge: 1-5 handover summary
│   │       └── ClarificationGrader.java  # LLM judge: NECESSARY / UNNECESSARY
│   ├── metrics/
│   │   ├── MetricsAggregator.java        # Compute all Phase 5 §6 metrics
│   │   └── GateEvaluator.java            # Check Phase 5 §7 hard + soft gates
│   ├── report/
│   │   ├── HtmlReportGenerator.java      # HTML report with tables + charts
│   │   └── JsonReportGenerator.java      # Machine-readable JSON
│   └── config/
│       └── EvalConfig.java               # Suite definitions, gate thresholds
├── src/main/resources/
│   ├── suites/
│   │   ├── smoke.yaml                    # 75 sessions (PR gate)
│   │   ├── full-regression.yaml          # 601 sessions (release gate)
│   │   └── nightly.yaml                  # Full + extended
│   └── eval-application.yml              # LLM config for model graders
├── src/test/                             # Grader unit tests
└── pom.xml                               # Maven submodule
```

**HR Annotations Overlay** (v8): when configured, `DatasetLoader`:
1. Joins HR annotations by `session_id`
2. Overrides auto-classified UC with `primary_uc_corrected`
3. Uses HR `expected_tool_sequence` / `forbidden_tools` for tool contract grading
4. Uses HR `drift_type` / `should_escalate` / `escalation_trigger` for corresponding graders

**Hard gates** (from Phase 5 §7.1):
| Gate | Threshold |
|------|-----------|
| `critical_policy_violation` | = 0 |
| `wrong_containment` | ≤ 2% |
| `groundedness_pass_rate` | ≥ 98% |
| `escalation_recall` | ≥ 95% |
| `handover_completeness` | ≥ 98% |
| `tool_scope_violation` | = 0 |
| `forbidden_phrase` | = 0 |
| `budget_enforcement` | = 100% |
| `phase_transition_validity` | = 100% |
| `critical_high_risk_escalation` | = 100% |
| `out_of_scope_detection` | ≥ 90% |

### DM10: E2E Demo Wiring

| Aspect | Detail |
|--------|--------|
| **What** | Connect all modules; startup automation (Makefile); 12 pre-configured demo scenarios with mock data; guided demo walkthrough |
| **Output** | Two-command startup; all scenarios verified; stakeholder-ready |

**Startup**:
```bash
# One-time: install + ingest
brew services start postgresql@16 && brew services start redis
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.arguments=--ingest    # index 218 articles

# Run:
mvn spring-boot:run -Dspring-boot.run.profiles=local &      # backend :8080
cd ui && npm install && npm run dev &                         # frontend :5173

# Eval (separate terminal):
mvn verify -Peval-smoke                                       # 75 sessions
mvn verify -Peval-full                                        # 601 sessions
```

**Makefile targets**:
```makefile
setup:      brew services start postgresql@16 && brew services start redis
build:      mvn clean install -DskipTests
ingest:     mvn spring-boot:run -Dspring-boot.run.arguments=--ingest
backend:    mvn spring-boot:run -Dspring-boot.run.profiles=local
frontend:   cd ui && npm run dev
eval-smoke: mvn verify -Peval-smoke
eval-full:  mvn verify -Peval-full
eval-extract:  cd eval_interactive && python -m eval_interactive extract --hr-csv ../data/human_review_annotations_*.csv --turns-dir ../data/eval_datasets/ --output case_specs/
eval-anchor:   cd eval_interactive && python -m eval_interactive run --set anchor
eval-full-interactive: cd eval_interactive && python -m eval_interactive run --set all
eval-compare:  cd eval_interactive && python -m eval_interactive compare --baseline $(BASELINE) --current $(CURRENT)
demo:       make setup && make build && make ingest && make backend & make frontend
```

**Demo Scenarios** (12, pre-configured with mock fixtures):

| # | User Input | Topic Subject | Expected UC | Expected Outcome | Key Verification |
|---|-----------|---------------|-------------|-----------------|-----------------|
| 1 | "My ad is not showing" | Ad Support | UC-A | Resolved (FAQ) | Knowledge retrieval + article link |
| 2 | "How do I post an ad?" | Ad Support | UC-B | Resolved (FAQ) | Step-by-step FAQ |
| 3 | "Why was my ad removed?" | Ad Support | UC-FP | Resolved (policy) | `get_moderation_review_context` → specific reason |
| 4 | "My ad was removed unfairly" | Ad Support | UC-H | Escalated | Intake → `create_case_controlled` → handover payload |
| 5 | "I can't log in" | Account Support | UC-D | Resolved (FAQ) | `get_customer_context` → account status + FAQ |
| 6 | "Delete my account" | Delete My Account or Data | UC-G | Escalated | GDPR intake → handover (no `search_knowledge`) |
| 7 | "I was scammed" | Report a Safety Issue | UC-J | Escalated | Safety intake → `create_case_controlled` → handover |
| 8 | "I want a refund" | Payments | UC-I | Escalated | Dispute handover (no case creation for UC-I) |
| 9 | "Not getting replies" | Replies or Messaging | UC-C | Resolve or Escalate | `get_message_moderation_context` chain |
| 10 | "Delivery problem" | Delivery | OOS_DELIVERY | Escalated | OUT_OF_SCOPE detection → fixed_script → handover |
| 11 | "Talk to an agent" (mid-chat) | Any | Any | Immediate escalation | `user_requested` trigger from any phase |
| 12 | Frustrated/threatening user | Any | UC-H/J | Escalated | `user_distress` detection → safety escalation |

---

## D3. Delivery Order

```text
Phase 1 (Foundation)         Phase 2 (Intelligence)       Phase 3 (Experience)
─────────────────────       ───────────────────────       ────────────────────

  ┌──────┐                    ┌──────┐                     ┌──────┐
  │ DM1  │───────────────────▶│ DM4  │────────────────────▶│ DM8  │
  │Found │                    │Core  │                      │ UI   │
  └──┬───┘                    └──┬───┘                      └──┬───┘
     │                           │                             │
     │    ┌──────┐               │         ┌──────┐         ┌──▼───┐
     ├───▶│ DM2  │───────────────┤    ┌───▶│ DM6  │────────▶│ DM10 │
     │    │Knowl │               │    │    │Guard │          │ E2E  │
     │    └──────┘               │    │    └──────┘          └──────┘
     │    ┌──────┐            ┌──▼───┐     ┌──────┐
     └───▶│ DM3  │───────────▶│ DM5  │     │ DM7  │
          │Mock  │            │Tools │     │Obsrv │
          └──────┘            └──────┘     └──┬───┘
                                              │
                                           ┌──▼───┐
                                           │ DM9  │
                                           │ Eval │
                                           └──────┘
```

| Order | Module | Parallel? | Prerequisite | Deliverable |
|-------|--------|-----------|-------------|-------------|
| **1** | **DM1: Foundation** | — | Homebrew setup | Spring Boot runs; DB migrations applied; LLM client working |
| **2a** | **DM2: Knowledge** | Yes (with DM3) | DM1 | 218 articles indexed; `/v1/faq/search` working |
| **2b** | **DM3: Mock Layer** | Yes (with DM2) | DM1 | Mock fixtures loaded; handover log endpoint working |
| **3** | **DM4: Core Runtime** | — | DM1, DM2, DM3 | State machine + LLM invocation + UC routing via API |
| **4a** | **DM5: Tools** | Yes (with DM6, DM7) | DM2-4 | All 10 tools with policy enforcement |
| **4b** | **DM6: Guardrails** | Yes | DM4 | Script library + forbidden phrases + PII |
| **4c** | **DM7: Observability** | Yes | DM1 | Events + traces + inspection endpoints |
| **5a** | **DM8: Chat UI** | Yes (with DM9) | DM4 | Chat widget + admin panel |
| **5b** | **DM9: Eval (Java Replay)** | Yes (with DM8) | DM4-6 | Smoke suite passing; full regression runnable |
| **5c** | **DM11: Interactive Eval (Python)** | Yes (with DM8-10) | DM4, DM7 | Interactive harness runnable; CaseSpec extraction; stall detector; before/after comparison |
| **6** | **DM10: E2E** | — | All | Demo scenarios verified; stakeholder-ready |

**Critical path**: DM1 → DM4 → DM5 → DM8+DM9 → DM10

---

## D4. Required Contracts

### D4.1 DB Schema

| Migration | Tables | Source |
|-----------|--------|--------|
| V1-V6 | `bot_sessions`, `bot_turns`, `session_outcomes`, `kb_articles`, `kb_chunks`, `bot_events` | Phase 3 §3.2, §3.5 |
| V7 | `mock_cases`, `mock_handover_log` | Demo-only (dropped in dev/prod migration) |

### D4.2 API Endpoints

| Endpoint | Purpose | Scope |
|----------|---------|-------|
| `POST /v1/chat/sessions` | Create session from pre-chat form | Demo (replaces SF webhook in prod) |
| `POST /v1/chat/sessions/{id}/messages` | Send user message | Demo |
| `GET /v1/chat/sessions/{id}` | Get session state | Demo |
| `POST /v1/faq/search` | Knowledge search (internal) | Same as production |
| `GET /v1/demo/sessions` | List sessions | Demo inspection |
| `GET /v1/demo/sessions/{id}/trace` | Turn-by-turn trace | Demo inspection |
| `GET /v1/demo/sessions/{id}/events` | Event timeline | Demo inspection |
| `GET /v1/demo/handover-logs` | Handover payloads | Demo inspection |
| `GET /v1/demo/metrics/funnel` | Funnel metrics | Demo inspection |
| `POST /v1/demo/config` | Toggle mock.business_hours etc. | Demo inspection |
| `GET /internal/health[/*]` | Health checks | Same as production |

### D4.3 Config Files

| Config | Format | Content |
|--------|--------|---------|
| `use-case-registry.yaml` | YAML | 12 UCs + 4 OUT_OF_SCOPE definitions |
| `control-policy.yaml` | YAML | Budgets, phase transitions, drift thresholds |
| `tool-policy.yaml` | YAML | Per-UC allowed/disallowed tool matrix |
| `escalation-triggers.yaml` | YAML | 17 trigger conditions + reason codes |
| `scripts/*.yaml` | YAML | 14+OOS template categories from `fixed_script_library_v1.md` |
| `application-local.yml` | YAML | Local DB/Redis/LLM/mock config |

---

## D5. Required Tests

| Module | Unit Tests | Integration Tests |
|--------|-----------|------------------|
| **DM1** | Health endpoint, config loading, profile switching | PostgreSQL + pgvector connectivity |
| **DM2** | Chunking, embedding mock, ANN query, rerank, faq_miss gate | DashScope embedding API, pgvector E2E search |
| **DM3** | Fixture loading, handover log write, case write | Mock chain correctness |
| **DM4** | All 12 phase transitions, each budget, 3 drift types, context projection, UC routing (incl. Account Support full-candidate, OOS detection) | Full turn cycle with mock tools |
| **DM5** | Per-tool validation, UC scope matrix (all cells), tool chain | Mock API chain E2E |
| **DM6** | 50+ template rendering, 9 forbidden phrase categories, PII regex | — |
| **DM7** | 12 event types, trace schema | DB event write |
| **DM8** | React component rendering | — |
| **DM9** | Dataset loading (7+HR), 12 code graders, 4 model graders, metrics, gates | Smoke suite against running bot |
| **DM11** | CaseSpec loader, stall detector, hard checks, outcome checks, composite scoring, diff engine | 10-case anchor run against running bot; before/after comparison with synthetic baseline |

**12 Critical test scenarios**: see DM10 demo scenarios table — each must pass.

---

## D6. Done Criteria

### Demo Complete

- [ ] Homebrew PostgreSQL + pgvector + Redis running (`brew services list` shows `started`)
- [ ] 218 articles / ~500 chunks in pgvector (`SELECT count(*) FROM kb_articles; SELECT count(*) FROM kb_chunks;`)
- [ ] Backend starts on `:8080` with `/internal/health` returning `UP`
- [ ] Chat UI loads at `http://localhost:5173` with Gumtree-styled design
- [ ] Pre-chat form → bot greeting with `{first_name}` → conversation works
- [ ] All 12 demo scenarios (§DM10) pass end-to-end through UI
- [ ] Handover payloads at `/v1/demo/handover-logs` contain all Phase 3 §3.6.2 fields including `form_topic_subject` + `topic_uc_mismatch`
- [ ] Admin panel at `/admin` shows: session list, trace viewer, event timeline, handover logs, funnel
- [ ] `mvn verify -Peval-smoke` passes all hard gates (75 sessions)
- [ ] `mvn verify -Peval-full` produces full report (601 sessions)
- [ ] Forbidden phrases: 0 false negatives on test corpus
- [ ] PII redacted in `bot_turns.projected_context` and `bot_events.payload`
- [ ] OUT_OF_SCOPE topics correctly route to handover
- [ ] LLM / embedding / mock switchable by Spring profile (no code changes)

### Interactive Eval Ready (DM11)

- [ ] `eval_interactive/` directory exists with all modules from Phase 5 §13.2
- [ ] `python -m eval_interactive extract` generates CaseSpec YAMLs from HR CSV + turns
- [ ] Anchor set (~30 cases) defined in `case_specs/anchor/`
- [ ] `python -m eval_interactive run --set anchor` completes all cases against running bot
- [ ] Stall detector correctly flags synthetic stall case; does not false-positive on clean case
- [ ] 3-layer scoring produces: L1 hard check pass/fail, L2 outcome scores, L3 LLM judge scores
- [ ] `python -m eval_interactive compare` produces regression report between two runs
- [ ] HTML report displays 7 key metrics (Phase 5 §6.9) with per-case drill-down
- [ ] Java eval (`mvn verify -Peval-smoke`) continues to pass — no interference

### Stakeholder Demo Ready

- [ ] 5-minute guided walkthrough prepared
- [ ] Each UC type demonstrable
- [ ] Trace viewer shows clear decision path per conversation
- [ ] Eval report shareable with gate pass/fail

---

## D7. Repo Structure

```
csagent/
├── server/                                   # Java 17 + Spring Boot 3.2.x backend
│   ├── src/main/java/com/gumtree/csagent/
│   │   ├── controller/
│   │   │   ├── ChatController.java           # Demo chat API
│   │   │   ├── HealthController.java
│   │   │   └── DemoInspectionController.java # /v1/demo/* endpoints
│   │   ├── service/
│   │   │   ├── runtime/                      # SessionManager, ControlKernel, UseCaseRouter,
│   │   │   │                                 # ContextProjectionBuilder, FormContextIngestion,
│   │   │   │                                 # DriftDetector, PhaseEvaluator, BudgetChecker
│   │   │   ├── llm/                          # LlmClient interface + OpenAiCompatible / VertexAi impls
│   │   │   ├── embedding/                    # EmbeddingClient interface + DashScope / VertexAi impls
│   │   │   ├── knowledge/                    # KnowledgeSearchService, KnowledgeIngestionRunner,
│   │   │   │                                 # ChunkingService, RerankService
│   │   │   ├── tools/                        # ToolDispatcher, ToolPolicyEnforcer, per-tool impls
│   │   │   ├── mock/                         # @Profile("local") — MockSalesforceService,
│   │   │   │                                 # MockGumtreeApiService, MockDataInitializer
│   │   │   ├── guardrails/                   # ScriptLibraryService, ForbiddenPhraseDetector,
│   │   │   │                                 # PiiRedactionFilter, ProgressPlaceholderService
│   │   │   └── observability/                # EventEmitter, TraceWriter, LocalEventStore
│   │   ├── model/                            # Domain objects
│   │   └── config/                           # Spring config classes
│   ├── src/main/resources/
│   │   ├── db/migration/                     # Flyway V1-V7
│   │   ├── prompts/                          # LLM prompt templates (versioned)
│   │   ├── scripts/                          # Fixed script library YAML (14+OOS)
│   │   ├── config/                           # use-case-registry, control-policy, tool-policy, etc.
│   │   ├── mock/                             # JSON fixture files
│   │   ├── application.yml
│   │   └── application-local.yml
│   ├── src/test/java/                        # Unit + integration tests
│   └── pom.xml
├── eval/                                     # Java replay eval harness (Maven submodule, CI gate)
│   ├── src/main/java/com/gumtree/csagent/eval/
│   │   ├── harness/                          # EvalRunner, DatasetLoader, SessionSimulator
│   │   ├── graders/code/                     # 7 code-based grader files (12 checks)
│   │   ├── graders/model/                    # 4 model-based graders (via LlmClient)
│   │   ├── metrics/                          # MetricsAggregator, GateEvaluator
│   │   └── report/                           # HTML + JSON report generators
│   ├── src/main/resources/suites/            # smoke.yaml, full-regression.yaml
│   ├── src/test/java/                        # Grader unit tests
│   └── pom.xml
├── eval_interactive/                          # Python interactive eval harness (Phase 5 §13.2)
│   ├── pyproject.toml                         # Python 3.11+; httpx, pyyaml, openai, jinja2, click
│   ├── eval_interactive/
│   │   ├── cli.py                             # click CLI: extract, run, compare, report
│   │   ├── case_spec/                         # loader.py, extractor.py, schema.py
│   │   ├── simulator/                         # user_simulator.py, agent_client.py, session_runner.py
│   │   ├── trace/                             # collector.py, models.py
│   │   ├── scoring/                           # hard_checks.py, outcome_checks.py, llm_judge.py,
│   │   │                                      # stall_detector.py, composite.py
│   │   ├── comparison/                        # diff_engine.py (before/after regression diff)
│   │   ├── batch/                             # executor.py, sets.py (anchor/promotion/exploration)
│   │   └── report/                            # html_report.py, json_report.py
│   ├── case_specs/                            # Generated CaseSpec YAMLs
│   │   ├── anchor/                            # ~30 stable regression cases
│   │   ├── promotion/                         # ~40 broader coverage
│   │   └── exploration/                       # ~30 edge cases
│   ├── results/                               # Run results (JSON per run)
│   ├── eval_interactive.yaml                  # Main configuration
│   └── tests/                                 # pytest
├── ui/                                       # React + Vite + TypeScript frontend
│   ├── src/
│   │   ├── components/chat/                  # ChatWidget, PreChatForm, MessageBubble, etc.
│   │   ├── components/admin/                 # SessionList, TraceViewer, HandoverLogViewer, etc.
│   │   ├── components/layout/                # Header, DemoPage
│   │   ├── api/client.ts
│   │   └── styles/gumtree-theme.css
│   ├── package.json
│   └── vite.config.ts
├── data/
│   ├── eval_datasets/                        # 7 CSV pairs (601 sessions / 11,288 turns)
│   ├── human_review_annotations_2026-04-22_complete.csv
│   └── knowledge/                            # knowledge_base_articles.json, article_uc_mapping.csv
├── Makefile
├── .env.local
└── pom.xml                                   # Parent POM (server + eval modules)
```

---

## D11. Iteration 1 — Tool Layer Wiring ("Bot 变聪明")

> **Goal**: Wire the already-implemented tool layer into the runtime control loop so the Bot can see customer account/listing/moderation context and provide personalized, grounded responses instead of generic FAQ answers.
>
> **Scope**: Local demo only. All Gumtree/Salesforce APIs remain mocked via `MockGumtreeApiService` / `MockSalesforceService`. No external integration changes.
>
> **Prerequisite**: D0–D10 complete (current demo baseline).

### D11.0 Gap Summary (Design vs Current Implementation)

The following features are **fully specified in Phase 3** and **have implementation code** (tool classes, dispatcher, policy enforcer) but are **not wired** into the runtime control loop:

| Gap | Design Reference | Current Code Status | Impact |
|-----|-----------------|-------------------|--------|
| `get_customer_context` not auto-triggered on session creation | Phase 3 §3.1.4 step 2b, §3.4.1.3, §3.4.3 | `GetCustomerContextTool.java` complete; `FormContextIngestionService.ingest()` does NOT call it | LLM has no account/listing/moderation context — `session.customerContext` always NULL; responses are generic |
| Context projection missing 5 contract fields | Phase 3 §3.2.6 | `ContextProjectionBuilder` omits `allowed_actions`, `tool_schemas`, `task_summary`, `risk_flags`, `budget_state` | LLM lacks action-space awareness and budget visibility; contributes to action mismatch (e.g., `retrieve_knowledge` in intake) |
| `create_case_controlled` not called on intake completion | Phase 3 §3.3.1, §3.4.2 | `CreateCaseControlledTool.java` complete; `PhaseEvaluator.resolveIntake()` escalates without case creation | Handover payload has no `case_id` for UC-H/J/K; agent must create case manually |

### D11.1 get_customer_context Auto-Trigger

**Where**: `FormContextIngestionService.ingest()` — append after form context is saved to session.

**Trigger condition**: `form_context.email` is present AND `active_use_case` (or any candidate UC) is in `get_customer_context.allowed_use_cases` (UC-A/C/D/F/FP/K). If `active_use_case` is not yet set (INIT phase), check whether any `candidate_use_cases` overlap with allowed UCs.

**Implementation spec**:

```
FormContextIngestionService.ingest(session, firstName, email, topicSubject, adId, description)
  │
  ├─ [existing] sanitize, build formContext JSON, set candidateUseCases
  │
  └─ [NEW] Auto-trigger customer context lookup:
       │
       ├─ Check: email present? YES
       │  Check: any candidateUC ∈ {UC-A,UC-C,UC-D,UC-F,UC-FP,UC-K}? YES
       │
       ├─ Call GetCustomerContextTool.execute(session, {email, ad_id})
       │   └─ Internally calls (all via MockGumtreeApiService):
       │       ├─ getAccountByEmail(email) → sanitized account summary
       │       ├─ getListingByAdId(adId) → sanitized listing summary (if ad_id present)
       │       └─ getModerationReview(adId) → review reason (if listing status=removed/moderated)
       │
       ├─ Store results:
       │   ├─ session.customerContext = result.data.account (JSONB)
       │   ├─ session.listingContext = result.data.listing (JSONB, if present)
       │   └─ session.moderationContext = result.data.moderation_review (JSONB, if present)
       │
       └─ On failure: log warning, continue (non-blocking — session proceeds without context)
```

**Dependencies**:
- Inject `GetCustomerContextTool` into `FormContextIngestionService` constructor
- `MockGumtreeApiService` already provides fixture-based responses — no mock changes needed
- `ObjectMapper` for JSON serialization of tool result to session fields

**Verification**:
- After session creation, `GET /v1/chat/sessions/{id}` returns non-null `customerContext`
- Demo scenario #3 (UC-FP "Why was my ad removed?"): bot response includes specific moderation reason (e.g., "multiple accounts") instead of generic "policy violation"
- Demo scenario #5 (UC-D "I can't log in"): bot response references account status

### D11.2 Context Projection Enhancement

**Where**: `ContextProjectionBuilder.buildProjection()` — add 5 missing fields from Phase 3 §3.2.6 contract.

**New fields**:

```json
{
  "task_summary": "User asks why their ad was removed. UC-FP detected with high confidence.",
  "allowed_actions": ["retrieve_knowledge", "answer_grounded", "ask_user", "escalate_human", "finish"],
  "risk_flags": ["medium"],
  "budget_state": {
    "total_bot_turns": 3,
    "max_bot_turns": 15,
    "clarification_count": 0,
    "max_clarification": 2,
    "faq_miss_count": 0,
    "max_faq_miss": 2
  },
  "tool_schemas": ["search_knowledge", "resolve_article", "get_customer_context"]
}
```

**Implementation spec**:

| Field | Source | Logic |
|-------|--------|-------|
| `task_summary` | session state | `"User inquiry about {formTopicSubject}. {activeUseCase} detected with {intentConfidence} confidence. Current phase: {currentPhase}."` |
| `allowed_actions` | phase + UC type | INTAKE UCs: `["ask_user", "escalate_human"]`; FAQ UCs in RESOLVE: `["retrieve_knowledge", "answer_grounded", "ask_user", "escalate_human", "finish"]`; CONFIRM: `["answer_grounded", "escalate_human", "finish"]` |
| `risk_flags` | UC registry | Read `risk_level` from `use-case-registry.yaml` for active UC |
| `budget_state` | session counters | Read `totalBotTurns`, `clarificationCount`, `faqMissCount` + max values from `ControlPolicyService` |
| `tool_schemas` | tool policy | `ToolPolicyEnforcer.getVisibleToolsForUc(activeUseCase)` — returns tool names allowed for this UC |

**Key benefit**: `allowed_actions` per phase eliminates the action mismatch bug (LLM returning `retrieve_knowledge` in intake mode) at the projection level, complementing the instruction-based fix already in `PhaseEvaluator.resolveIntake()`.

### D11.3 create_case_controlled Integration

**Where**: `PhaseEvaluator.resolveIntake()` — insert before `PhaseResult.escalate()` when intake is complete.

**Trigger condition**: LLM returns `escalate_human` or `finish` for UC ∈ {UC-H, UC-J, UC-K}.

**Implementation spec**:

```
resolveIntake() — escalation path (existing lines 293-299):
  │
  ├─ [existing] LLM returns escalate_human or finish
  │
  ├─ [NEW] If activeUC ∈ {UC-H, UC-J, UC-K}:
  │   │
  │   ├─ Build case fields from session:
  │   │   ├─ first_name, email ← formContext
  │   │   ├─ topic_subject ← UC-H:"Ad Support" / UC-J:"Report a Safety Issue" / UC-K:"Technical Support"
  │   │   ├─ description ← assembled from intake_fields + session summary
  │   │   ├─ ad_id ← formContext.ad_id (if available)
  │   │   └─ bot_context ← handover payload JSON
  │   │
  │   ├─ CreateCaseControlledTool.execute(session, caseFields)
  │   │   └─ Validates required fields per UC (Phase 2 §2.10.5)
  │   │   └─ Writes to mock_cases table (MockSalesforceService)
  │   │   └─ Returns case_id
  │   │
  │   ├─ session.caseId = result.caseId
  │   │
  │   └─ Emit CASE_CREATED event with {case_id, use_case_id, topic_subject}
  │
  ├─ [existing] Render escalation template (with {CASE_NUMBER} now populated)
  └─ [existing] PhaseResult.escalate(msg, "intake_complete")
```

**Dependencies**:
- Inject `CreateCaseControlledTool` into `PhaseEvaluator` constructor
- Inject `BotEventRepository` (or `EventEmitter`) for CASE_CREATED event
- `MockSalesforceService.createCase()` already writes to `mock_cases` — no mock changes needed

**Verification**:
- Demo scenario #4 (UC-H): handover payload contains `case_id`; `mock_cases` table has new row
- Demo scenario #7 (UC-J): same
- Demo scenario #8 (UC-I): NO case creation (UC-I not in allowed set)
- Escalation template renders `{CASE_NUMBER}` with actual case ID

### D11.4 Eval Validation

After Iteration 1, run `eval-smoke` and verify:
- `handover_completeness` improves (case_id populated for UC-H/J/K)
- `groundedness_pass_rate` does not regress
- `routing_accuracy` does not regress
- No new `tool_scope_violation` (case creation only for UC-H/J/K)

---

## D12. Iteration 2 — Harness Completion ("架构对齐")

> **Goal**: Wire `ToolDispatcher` into the control loop so all tool calls go through policy enforcement; integrate `progress_placeholder`; complete event emission; add **Human Agent Queue** tab to admin panel for handover inspection.
>
> **Scope**: Local demo only. All external APIs remain mocked.
>
> **Prerequisite**: D11 complete.

### D12.1 ToolDispatcher Integration into Control Loop

**Current state**: `PhaseEvaluator.resolveFaq()` calls `KnowledgeSearchService.search()` directly (line 154), bypassing `ToolDispatcher` and `ToolPolicyEnforcer`.

**Target state**: All tool invocations go through `ToolDispatcher.dispatch()`, which applies policy enforcement before execution.

**Implementation spec**:

```
BEFORE (current):
  resolveFaq() line 154:
    KnowledgeSearchResult searchResult = knowledgeSearchService.search(userMessage, ucTags);

AFTER:
  resolveFaq():
    ToolResult toolResult = toolDispatcher.dispatch("search_knowledge", session,
        Map.of("query", userMessage, "uc_tags", ucTags));

    if (toolResult.isError() && "scope_blocked".equals(toolResult.getErrorCode())) {
        // UC not allowed to search knowledge — escalate
        return PhaseResult.escalate(session, "...", "tool_scope_blocked");
    }

    KnowledgeSearchResult searchResult = (KnowledgeSearchResult) toolResult.getData();
```

**Changes required**:

| Component | Change |
|-----------|--------|
| `PhaseEvaluator` | Inject `ToolDispatcher`; replace direct `knowledgeSearchService.search()` with `toolDispatcher.dispatch("search_knowledge", ...)` |
| `SearchKnowledgeTool` | Ensure `execute()` returns `KnowledgeSearchResult` wrapped in `ToolResult.ok(data)` |
| `ToolDispatcher.dispatch()` | Add latency tracking; emit `TOOL_SCOPE_BLOCKED` event on policy violation (existing code, just needs to be reachable) |
| `ControlKernel` | No changes (ToolDispatcher is used inside PhaseEvaluator, not ControlKernel directly) |

**Verification**:
- INTAKE UCs (UC-G/H/I/J/K) attempting `search_knowledge` → `scope_blocked` event logged
- FAQ UCs → search works as before
- `ToolContractGrader` in eval detects scope violations

### D12.2 progress_placeholder Integration

**Where**: `ToolDispatcher.dispatch()` — wrap tool execution in async timeout.

**Implementation spec**:

```
ToolDispatcher.dispatch(toolName, session, params):
  │
  ├─ Policy check (existing)
  │
  ├─ [NEW] Start timer
  │
  ├─ Execute tool
  │   │
  │   └─ If latency > 1500ms:
  │       └─ Send ProgressPlaceholderService.getPlaceholder() to user
  │           (via callback or injected response sender)
  │
  └─ Return ToolResult
```

**Placeholder messages** (round-robin, from existing `ProgressPlaceholderService`):
1. "One moment while I look into this for you."
2. "Thanks for your patience — I'm checking your details now."
3. "I'm still looking into this for you. Just a moment longer."

**Threshold**: 1500ms (Phase 3 §3.4.3).

**Note**: In the local demo with mock APIs, tool latency is typically <50ms. To verify this feature:
- Add `mock.tool_latency_ms=2000` config option in `application-local.yml` (defaults to 0)
- When set, `MockGumtreeApiService` adds artificial delay
- This allows demo/eval of placeholder behavior without real API latency

### D12.3 Event Emission Completeness

**Current gaps**: Several event types defined in Phase 3 §3.8.1 are never emitted.

| Event Type | Current Status | Fix |
|------------|---------------|-----|
| `RETRIEVAL_EXECUTED` | Not emitted | Emit from `SearchKnowledgeTool.execute()` after KB search: `{query_hash, result_count, top_score, faq_miss, latency_ms}` |
| `ARTICLE_SHOWN` | Not emitted | Emit from `ResolveArticleTool.execute()`: `{source_ids[], canonical_urls[]}` |
| `CASE_CREATED` | Not emitted | Emit from D11.3 case creation: `{case_id, use_case_id, topic_subject}` |
| `CLARIFICATION_ASKED` | Not emitted | Emit from `PhaseEvaluator.evaluateDiscover()` when `ask_user` action: `{clarification_count, question_topic}` |
| `OUTCOME_RECORDED` | Partially emitted | Add to `RecordOutcomeTool.execute()` or `SessionManager.recordOutcome()` |

**Implementation**: Each emission is a one-line call to the existing `BotEventRepository.save()` pattern used throughout the codebase. No new infrastructure needed.

**Verification**: Admin panel Event Timeline tab shows all 12 event types for a complete session lifecycle.

### D12.4 Turn Log Enhancement

**Where**: `ControlKernel.recordTurn()` — add `tool_calls` field.

**Current state**: `BotTurn.toolCalls` field exists in the entity and DB schema but is never populated.

**Implementation spec**:

```
recordTurn() — after PhaseResult returned:
  │
  ├─ [NEW] If tool was dispatched during this turn:
  │   Build tool_calls JSON array:
  │   [{
  │     "tool_name": "search_knowledge",
  │     "latency_ms": 320,
  │     "status": "success",
  │     "result_count": 3,
  │     "faq_miss": false
  │   }]
  │
  └─ Save to BotTurn.toolCalls (JSONB)
```

**Approach**: `ToolDispatcher.dispatch()` records each invocation to a thread-local or request-scoped `ToolCallLog`. `ControlKernel.recordTurn()` reads the log and serializes to `tool_calls` JSONB. Clear the log after each turn.

### D12.5 Human Agent Queue Tab (Admin Panel)

**Purpose**: Stakeholders and QA can inspect every handover — the full payload, the bot's escalation reasoning, and the message the customer would see. Replaces the current basic `HandoverLogViewer` with a richer, queue-style interface.

**Route**: `http://localhost:5173/admin` → "Handover Queue" tab (rename existing "Handover Logs" tab).

**Data source**: `GET /v1/demo/handover-logs` (existing endpoint, returns `mock_handover_log` rows).

**UI spec**:

```
┌──────────────────────────────────────────────────────────────────────┐
│  Admin Panel > Handover Queue                                        │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │ Filter: [All UCs ▼] [All Reasons ▼] [Date range]  [Search] │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌────┬──────────┬────────┬───────────────────┬───────┬──────────┐  │
│  │ #  │ Session  │ UC     │ Escalation Reason │ Turns │ Time     │  │
│  ├────┼──────────┼────────┼───────────────────┼───────┼──────────┤  │
│  │ 1  │ 550e8401 │ UC-H   │ intake_complete   │ 4     │ 14:32    │  │
│  │ 2  │ 550e8402 │ UC-J   │ trust_safety_req  │ 3     │ 14:28    │  │
│  │ 3  │ 550e8403 │ UC-A   │ faq_miss_exceeded │ 6     │ 14:15    │  │
│  │ 4  │ 550e8404 │ OOS_DL │ out_of_scope      │ 1     │ 14:10    │  │
│  └────┴──────────┴────────┴───────────────────┴───────┴──────────┘  │
│                                                                      │
│  ══════════ Selected: #1 (550e8401) ═══════════════════════════════  │
│                                                                      │
│  ┌─── Summary ──────────────────────────────────────────────────┐   │
│  │ UC: UC-H (Ad Removal Appeal)                                  │   │
│  │ Escalation Reason: intake_complete                            │   │
│  │ Case ID: CS-550E8401                                          │   │
│  │ Confidence: 0.92                                              │   │
│  │ Bot Turns: 4 | Clarifications: 1 | FAQ Misses: 0             │   │
│  │ Duration: 45s                                                 │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─── Customer Message (what user saw) ─────────────────────────┐   │
│  │ "Thanks for those details. I've created a case (CS-550E8401)  │   │
│  │  for our Ad Support team. You'll hear back by email within    │   │
│  │  24-48 hours."                                                │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─── Handover Payload (JSON, what agent receives) ─────────────┐   │
│  │ {                                                              │   │
│  │   "version": "1.0",                                           │   │
│  │   "session_id": "550e8401",                                   │   │
│  │   "primary_use_case": "UC-H",                                 │   │
│  │   "candidate_use_cases": ["UC-FP"],                           │   │
│  │   "current_status": "intake_complete",                        │   │
│  │   "summary": "User reports ad 1487477877 was incorrectly...", │   │
│  │   "case_id": "CS-550E8401",                                   │   │
│  │   "escalation_reason": "intake_complete",                     │   │
│  │   "identifiers_collected": { "email": "...", "ad_id": "..." },│   │
│  │   "intake_fields": { "stated_reason": "..." },                │   │
│  │   "total_bot_turns": 4,                                       │   │
│  │   "form_topic_subject": "Ad Support",                         │   │
│  │   "topic_uc_mismatch": false,                                 │   │
│  │   ...                                                         │   │
│  │ }                                                              │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌─── Conversation Transcript ──────────────────────────────────┐   │
│  │ [Bot]  Hi Jane! I'm here to help...                           │   │
│  │ [User] My ad was removed unfairly                             │   │
│  │ [Bot]  I'm sorry to hear that. Could you confirm your ad ID?  │   │
│  │ [User] 1487477877                                             │   │
│  │ [Bot]  Thanks. Could you describe what happened?              │   │
│  │ [User] I'm selling a laptop, didn't break any rules           │   │
│  │ [Bot]  I've created a case for our Ad Support team...         │   │
│  └───────────────────────────────────────────────────────────────┘   │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**Backend changes**:

| Change | Detail |
|--------|--------|
| `mock_handover_log` table | Add column `customer_message TEXT` — the escalation message shown to the user |
| `mock_handover_log` table | Add column `transcript JSONB` — array of `{role, message, turn_index}` for the session |
| `SessionManager.recordHandover()` | Populate `customer_message` from the escalation response text |
| `SessionManager.recordHandover()` | Populate `transcript` by querying `bot_turns` for the session and building `[{role, message}]` array |
| `DemoInspectionController` | Update `GET /v1/demo/handover-logs` response to include `customer_message` and `transcript` |
| `DemoInspectionController` | Add `GET /v1/demo/handover-logs/{id}` for single handover detail |

**Frontend changes**:

| Component | Change |
|-----------|--------|
| `AdminPage.tsx` | Rename "Handover Logs" tab → "Handover Queue" |
| `HandoverQueueView.tsx` (new) | Replace `HandoverLogViewer.tsx` — list + detail layout as shown above |
| `HandoverDetail.tsx` (new) | Summary card + customer message card + JSON payload viewer + transcript viewer |
| `HandoverFilters.tsx` (new) | UC filter dropdown, escalation reason filter, date range |

**DB migration**:

```sql
-- V8__add_handover_detail_columns.sql
ALTER TABLE mock_handover_log ADD COLUMN customer_message TEXT;
ALTER TABLE mock_handover_log ADD COLUMN transcript JSONB;
```

### D12.6 Updated Done Criteria

In addition to D6 done criteria, after D11+D12:

**Iteration 1 (D11) — "Bot 变聪明"**:

- [ ] `session.customerContext` populated after session creation when email present and UC allows
- [ ] Demo scenario #3 (UC-FP): bot cites specific moderation reason from `moderation_context`
- [ ] Demo scenario #5 (UC-D): bot references account status from `customer_context`
- [ ] Context projection includes `allowed_actions`, `task_summary`, `budget_state`, `risk_flags`, `tool_schemas`
- [ ] `create_case_controlled` fires for UC-H/J/K on intake completion; `mock_cases` table has new row
- [ ] Demo scenario #4 (UC-H): handover payload contains populated `case_id`
- [ ] Demo scenario #8 (UC-I): NO case creation (UC-I not in allowed set)
- [ ] Escalation template renders `{CASE_NUMBER}` with actual case ID from `create_case_controlled`
- [ ] `eval-smoke` passes all hard gates; no regressions in routing/groundedness/escalation

**Iteration 2 (D12) — "Harness 变完整"**:

- [ ] All `search_knowledge` calls go through `ToolDispatcher.dispatch()` with policy enforcement
- [ ] INTAKE UCs attempting `search_knowledge` via ToolDispatcher → `scope_blocked` event logged
- [ ] `mock.tool_latency_ms` config: when set >1500, progress placeholder message sent
- [ ] All 12 event types emitted for a complete session lifecycle (verify via Event Timeline tab)
- [ ] `bot_turns.tool_calls` JSONB populated for turns that invoke tools
- [ ] Admin panel "Handover Queue" tab: list view with UC/reason filters
- [ ] Handover Queue detail view: summary + customer message + JSON payload + conversation transcript
- [ ] `mock_handover_log` contains `customer_message` and `transcript` for every escalated session
- [ ] `eval-full` produces complete report with all hard gates checked

---

## D13. DM11 — Interactive Eval Harness ("交互式评估")

> **Goal**: Build a Python-based interactive evaluation harness that complements the Java replay eval. It drives the CS Agent with an LLM-based User Simulator using structured CaseSpecs derived from human review data, applies 3-layer scoring, detects stalls, and compares results across runs.
>
> **Scope**: Local demo. Uses same CS Agent at `localhost:8080`.
>
> **Prerequisite**: D11 complete (bot must have working tool integration).
>
> **Tech**: Python 3.11+, httpx, pyyaml, openai (for LLM API), jinja2, click
>
> **Design Reference**: Phase 5 §2.3, §3.4, §5.4, §6.9, §13.2–13.5

### D13.1 CaseSpec Extraction from HR Data

**Where**: `eval_interactive/eval_interactive/case_spec/extractor.py`

**Input**:
- `data/human_review_annotations_*.csv` (367 sessions, headers: `session_id`, `case_id`, `source_dataset`, `form_topic_subject`, `primary_uc`, `secondary_ucs`, `outcome_class`, `expected_tool_sequence`, `forbidden_tools`, `should_escalate`, `escalation_trigger`, `risk_level`, `drift_type`, `has_frustration`, `frustration_type`, `grounding_required`, `answer_must_not_contain`, `quality_score`, etc.)
- `data/eval_datasets/*_turns.csv` (turn data: `conversation_id`, `case_id`, `sequence`, `role`, `speaker`, `message_redacted`)

**Output**: YAML files in `case_specs/{anchor,promotion,exploration}/`

**Implementation spec**:

```
extractor.py:
  │
  ├─ For each HR row:
  │   ├─ Find matching turns by session_id / case_id
  │   ├─ Extract form_context from sequence=0 (parse "[Form] Subject:... | Description:...")
  │   ├─ Extract seed_messages: first 1-3 visitor turns (sequence > 0, role = visitor)
  │   ├─ Map HR fields to CaseSpec expected block (see Phase 5 Appendix C)
  │   ├─ Derive persona:
  │   │   ├─ frustration_level: none if !has_frustration, mild if frustration_type=general, high if litigation/threat
  │   │   ├─ drift_behavior: direct from drift_type
  │   │   └─ verbosity: infer from message lengths (< 20 chars → terse, > 100 → verbose, else normal)
  │   ├─ Derive grounding_mode:
  │   │   ├─ grounding_required=true → faq_source_backed
  │   │   └─ UC ∈ {G,H,I,J,K} → fixed_script_only
  │   └─ Write CaseSpec YAML
  │
  └─ Assign to case sets:
      ├─ anchor: quality_score ≥ 4, clear expected outcome, balanced UC coverage (~30)
      ├─ promotion: medium confidence, broader coverage (~40)
      └─ exploration: edge cases, frustration, complex drift (~30)
```

**CLI**: `python -m eval_interactive extract --hr-csv <path> --turns-dir <path> --output case_specs/`

### D13.2 User Simulator

**Where**: `eval_interactive/eval_interactive/simulator/user_simulator.py`

**Implementation**: LLM-based user turn generator. Uses same `DASHSCOPE_*` or `KIMI_*` credentials from `.env.local`.

**System prompt** (Jinja2 template):

```text
You are a customer contacting Gumtree support.

Your persona:
- Goal: {{ persona.goal_summary }}
- Frustration level: {{ persona.frustration_level }}
- Style: {{ persona.verbosity }}

You submitted a form with:
- Topic: {{ form_context.topic_subject }}
- Description: {{ form_context.description }}

Facts you know (reveal naturally when relevant):
{% for fact in persona.hidden_facts %}
- {{ fact.fact }} (reveal: {{ fact.disclose_when }})
{% endfor %}

{% if persona.will_request_human_if %}
If the bot {{ persona.will_request_human_if }}, ask to speak with a human agent.
{% endif %}

Rules:
- Respond as a real customer would. Do NOT reveal you are an AI.
- If the bot has resolved your issue, say something like "thank you, that helps."
- If the bot is clearly unable to help after multiple attempts, say "can I speak to someone?"
- Keep responses concise (1-3 sentences).

Respond with JSON: {"message": "your response", "goal_status": "in_progress|achieved|impossible"}
```

**Input per turn**: CaseSpec + full conversation history + last bot reply
**Output**: `{"message": str, "goal_status": str}`
**LLM config**: `temperature=0.7` (more natural than grader calls)

### D13.3 Session Runner

**Where**: `eval_interactive/eval_interactive/simulator/session_runner.py`

**Orchestrates** the user↔bot loop:

```
run_session(case_spec, agent_client, user_simulator, stall_detector):
  │
  ├─ Create bot session: POST /v1/chat/sessions with case_spec.form_context
  │   → session_id, bot_greeting
  │
  ├─ First user message:
  │   case_spec.persona.seed_messages[0] OR case_spec.form_context.description
  │
  ├─ Loop (turn = 1 to case_spec.expected.max_turns):
  │   ├─ POST /v1/chat/sessions/{id}/messages with user_msg
  │   │   → bot_reply, should_end_chat, additional_data
  │   │
  │   ├─ Record turn: {user_msg, bot_reply, latency_ms, raw_response}
  │   │
  │   ├─ Check stop conditions (in priority order):
  │   │   1. should_end_chat = true              → stop_reason = "bot_ended"
  │   │   2. turn >= max_turns                   → stop_reason = "max_turns_exceeded"
  │   │   3. stall_detector.detect(transcript)   → stop_reason = "stall_detected"
  │   │   4. last 2 bot replies identical         → stop_reason = "loop_detected"
  │   │
  │   ├─ If not stopped:
  │   │   user_simulator.generate_next(case_spec, transcript, bot_reply)
  │   │   → {message, goal_status}
  │   │   ├─ goal_status = "achieved"            → stop_reason = "goal_achieved"
  │   │   ├─ goal_status = "impossible"           → stop_reason = "goal_impossible"
  │   │   └─ goal_status = "in_progress"          → continue with message as next user_msg
  │   │
  │   └─ turn++
  │
  └─ Return SessionTrace: {session_id, transcript, stop_reason, total_turns, elapsed_ms}
```

### D13.4 Trace Collector

**Where**: `eval_interactive/eval_interactive/trace/collector.py`

After session completes, fetches full trace from CS Agent API:

| Endpoint | Data Collected |
|----------|---------------|
| `GET /v1/chat/sessions/{id}` | Final session state: `activeUseCase`, `candidateUseCases`, `containmentOutcome`, `escalationReason`, `totalBotTurns`, `clarificationCount`, `faqMissCount`, `formContext`, `customerContext`, `articlesShown`, `currentPhase` |
| `GET /v1/demo/sessions/{id}/trace` | Per-turn: `actionSelected`, `toolCalls` (JSONB), `sourceIds`, `phaseBefore`/`phaseAfter`, `projectedContext`, `latencyMs` |
| `GET /v1/demo/sessions/{id}/events` | Event timeline: `eventType`, `turnIndex`, `payload` (all 12 types) |
| `GET /v1/demo/handover-logs` | If escalated: `handoverPayload` (JSON with all Phase 3 §3.6.2 fields), `customerMessage`, `transcript` |

**Output**: Structured `TraceData` object combining all sources, ready for scoring.

### D13.5 3-Layer Scoring Implementation

**Where**: `eval_interactive/eval_interactive/scoring/`

See Phase 5 §5.4 for full specification. Implementation:

**`hard_checks.py`** — L1 deterministic checks:
- Iterates `case_spec.scoring.hard_checks` list
- Each check reads trace data and returns `pass` / `fail` with detail
- ANY failure → `case_passed = false`
- Key checks: `no_forbidden_tools` (reads `tool_calls` from turn trace), `budget_enforcement` (reads session counters), `no_stall` (calls `stall_detector`)

**`outcome_checks.py`** — L2 result-level checks:
- Each check returns float 0.0-1.0
- `correct_uc`: compare `trace.session_state.activeUseCase` vs `case_spec.expected.primary_uc`
- `correct_outcome`: compare `trace.session_state.containmentOutcome` vs `case_spec.expected.outcome_class`
- `tool_sequence_match`: compare actual tool calls from turn trace vs `case_spec.expected.expected_tool_sequence`

**`llm_judge.py`** — L3 semantic scoring:
- For each dimension in `case_spec.scoring.llm_judge_dimensions`:
  - Load Jinja2 prompt template from `scoring/prompts/`
  - Render with conversation transcript + trace data
  - Call LLM (same provider, `temperature=0.0`)
  - Parse 1-5 score from response

**`stall_detector.py`** — Promise-without-result detection:
- Scans bot responses for promise patterns (configurable regex list)
- Cross-references with `tool_calls` from turn trace
- Checks if visible result appeared within `followup_window_turns`
- Returns `StallResult: {detected: bool, turn_index: int, pattern_matched: str}`

**`composite.py`** — Score aggregation:
```python
def compute_composite(l1_passed: bool, l2_scores: list[float], l3_scores: list[float]) -> float:
    if not l1_passed:
        return 0.0
    outcome_score = mean(l2_scores) if l2_scores else 0.0
    judge_score = (mean(l3_scores) / 5.0) if l3_scores else 0.0
    return 0.5 * outcome_score + 0.5 * judge_score
```

### D13.6 Before/After Comparison

**Where**: `eval_interactive/eval_interactive/comparison/diff_engine.py`

**Input**: Two result JSON files (baseline + current)
**Output**: Diff report (JSON + human-readable summary)

**Logic**:
1. Load both result files, join by `case_id`
2. For each case, classify as:
   - **Improved**: failed in baseline, passed in current
   - **Stable pass**: passed in both
   - **Stable fail**: failed in both
   - **Regressed**: passed in baseline, failed in current
3. Compute `regression_rate = regressed / total`
4. Compute metric deltas for all 7 key metrics
5. List failure tag changes (new failures, resolved failures)

**CLI**: `python -m eval_interactive compare --baseline <path> --current <path>`

### D13.7 Batch Executor

**Where**: `eval_interactive/eval_interactive/batch/executor.py`

**Implementation**:
- Uses `asyncio` + `httpx.AsyncClient` for concurrent sessions
- Configurable parallelism via `--parallel` flag (default 5)
- Each CaseSpec runs independently (separate bot session)
- Progress output via `click.progressbar` or `tqdm`
- Timeout per session: configurable (default 120s)

**CLI**:
```bash
python -m eval_interactive run --set anchor --label baseline_v1.0.3 --parallel 5
python -m eval_interactive run --set all --label release_v1.0.4
```

**Set management** (`batch/sets.py`):
- Reads CaseSpec YAML files from `case_specs/{anchor,promotion,exploration}/`
- `--set anchor` → only `case_specs/anchor/*.yaml`
- `--set all` → all three directories
- Custom: `--cases case_specs/custom/*.yaml`

### D13.8 Report Generation

**Where**: `eval_interactive/eval_interactive/report/`

**HTML report** includes:
- **Summary dashboard**: 7 key metrics (Phase 5 §6.9), pass/fail counts, composite score distribution
- **Per-case drill-down**: CaseSpec details, conversation transcript, 3-layer scores, stall flags, failure tags
- **Regression table**: if comparison data available, shows regressions/improvements
- **Per-UC breakdown**: metrics grouped by `expected.primary_uc`

**JSON report**: machine-readable, same data structure.

**Output**: `results/{run_id}/report.html` + `results/{run_id}/report.json`

### D13.9 Configuration

```yaml
# eval_interactive.yaml
bot:
  base_url: http://localhost:8080

llm:
  base_url: ${DASHSCOPE_BASE_URL}        # or ${KIMI_BASE_URL}
  api_key: ${DASHSCOPE_API_KEY}           # or ${KIMI_API_KEY}
  model: ${DASHSCOPE_CHAT_MODEL}          # or ${KIMI_MODEL}
  temperature: 0.0                         # for grader calls
  simulator_temperature: 0.7               # for user simulator

simulator:
  max_turns: 15
  default_persona:
    frustration_level: none
    verbosity: normal
    drift_behavior: none

stall_detector:
  promise_patterns:
    - "let me (check|look|find|verify|search)"
    - "i('m| am) (checking|looking|searching|investigating)"
    - "one moment"
    - "i'll (look into|check|investigate|find)"
    - "thanks for your patience"
    - "just a moment"
  followup_window_turns: 2

batch:
  parallel: 5
  timeout_per_session_seconds: 120

report:
  output_dir: results/
```

### D13.10 Done Criteria (DM11)

- [ ] `pyproject.toml` with all deps; `pip install -e .` works on Mac
- [ ] CaseSpec extraction: `python -m eval_interactive extract` produces valid YAML from HR CSV
- [ ] Anchor set: ~30 cases covering all UC risk levels, drift types, and outcome classes
- [ ] User Simulator: generates coherent, persona-consistent user messages (verified by manual inspection of 5 cases)
- [ ] Session Runner: completes 30-case anchor run against running bot (all cases reach termination)
- [ ] Stall Detector: correctly flags synthetic stall case; does not false-positive on clean case
- [ ] 3-Layer Scoring: all three layers produce scores; composite formula verified against known case
- [ ] Before/After Comparison: regression correctly detected when synthetic case score drops
- [ ] Batch executor: 30-case anchor run completes in < 20 minutes (with `--parallel 5`)
- [ ] HTML report: viewable in browser, 7 key metrics displayed, per-case drill-down works
- [ ] No interference: `mvn verify -Peval-smoke` continues to pass

### D13.11 Delivery Dependencies

| DM11 Component | Depends On | Notes |
|----------------|-----------|-------|
| CaseSpec Extractor | HR CSV + turns CSV data | Data already exists |
| User Simulator | LLM API credentials (`.env.local`) | Same as DM4 |
| Agent Client | DM4 bot running on `:8080` | DM4 must be complete |
| Trace Collector | DM7 inspection endpoints | All `/v1/demo/*` endpoints exist |
| Stall Detector | Turn trace with `tool_calls` | DM12 §D12.4 (turn log enhancement) |
| Hard Checks | DM5 tool policy, DM6 guardrails | Policy enforcement must work |
| LLM Judge | LLM API credentials | Same provider as User Simulator |

---

## D14. Iteration 3 — Performance & Observability ("性能 + 可观测性")

> **Goal**: Fix three critical issues discovered during session `dd895a1c` analysis: (1) serial rerank causing 80% of turn latency, (2) form description ignored during knowledge search leading to dead-end retrieval, (3) 100% LLM call observability blackout — 17 calls per session, 0 recorded in trace.
>
> **Prerequisite**: D11+D12 complete (current state).
>
> **Triggered by**: Session analysis showing 5.7s rerank latency (serial 8× LLM calls), vague "Pls help" queries failing retrieval despite rich form descriptions, and complete trace blindspot for routing/rerank/retry LLM calls.

### D14.1 Parallel Rerank (Fix 1 — Latency)

**Current state**: `RerankService.rerank()` (line 55) iterates candidates in a serial `for` loop, calling `llmClient.chat()` one at a time. With 8 candidates at ~700ms each = ~5.7s total rerank latency, consuming 80% of per-turn wall time.

**Target state**: All `scoreCandidate()` calls execute in parallel via `CompletableFuture`.

**Implementation**:

| File | Change |
|------|--------|
| `RerankService.java` | Replace serial `for` loop with `CompletableFuture.supplyAsync()` per candidate. Use a dedicated `Executor` (fixed thread pool, size = `RERANK_CANDIDATES` = 8). Join all with `CompletableFuture.allOf()`. Maintain existing sort-by-score and logging. |

**Before / After**:
```
Before: 8 × ~700ms serial  = ~5700ms
After:  max(~700ms) parallel = ~700-1000ms  (limited by slowest single call)
```

**Error handling**: Same as current — individual `scoreCandidate()` failures return 2.5 (neutral score). A failed future does not block others.

**Thread pool config**: `Executors.newFixedThreadPool(8)` — bounded pool to prevent unbounded thread creation under load. The pool is shared across all rerank invocations within the service.

### D14.2 Form Description Query Enrichment (Fix 2 — Quality)

**Current state**: `PhaseEvaluator.resolveFaq()` (line 178) passes raw `userMessage` as the knowledge search query:
```java
toolDispatcher.dispatch("search_knowledge", session,
    Map.of("query", userMessage, "uc_tags", ucTags));
```

When user writes "Pls help" in chat but filled detailed description in the pre-chat form ("why I cannot post my advert... keeps saying not being posted due to posting rules"), the rich context is wasted. Result: consecutive FAQ misses (rerank score 3.0 < 3.5 threshold) → unnecessary escalation.

**Target state**: Enrich the search query with `form_context.description` when the user message alone is likely insufficient.

**Implementation**:

| File | Change |
|------|--------|
| `PhaseEvaluator.java` | Add private method `enrichQueryWithFormContext(String userMessage, BotSession session)`. Logic: if `session.getFormContext()` has non-empty `description` field AND (`userMessage.length() < 20` OR `turnIndex <= 1`), construct enriched query: `userMessage + " | Context: " + formDescription` (truncate description to 200 chars). Otherwise return `userMessage` unchanged. Call this before `toolDispatcher.dispatch()`. |

**Enrichment rules**:
- Only activate when `userMessage` is short (< 20 chars) **OR** it's the first 2 turns (index 0 or 1)
- Only enrich if `form_context.description` exists and is non-blank
- Truncate form description to 200 chars to avoid polluting embedding quality
- Separator `" | Context: "` helps the embedding model distinguish the primary query from supplementary context

**Expected impact**: Vague messages like "Pls help" / "Any update" that previously scored rerank 3.0 should now retrieve relevant articles because the enriched query contains the actual problem description from the form.

### D14.3 LLM Call Log Table (Fix 3 — Observability, Option B)

**Current state**: Only the final PhaseEvaluator LLM response is recorded in `bot_turns.llm_raw_response`. Routing calls (in `UseCaseRouter`), rerank calls (in `RerankService`, N per search), and retry calls are 100% invisible to the trace API and admin UI.

In session `dd895a1c`: 17 LLM calls occurred, 0 were recorded.

**Target state**: Every LLM call is recorded in a dedicated `llm_call_log` table and exposed via a new trace endpoint.

**Implementation**:

#### D14.3.1 Database Migration

**New file**: `V8__create_llm_call_log.sql`

```sql
CREATE TABLE llm_call_log (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          TEXT NOT NULL REFERENCES bot_sessions(session_id),
    turn_index          INT,                         -- NULL for routing (pre-turn)
    call_type           TEXT NOT NULL,                -- 'routing' | 'chat' | 'rerank' | 'retry'
    model               TEXT NOT NULL,
    prompt_tokens       INT,
    completion_tokens   INT,
    latency_ms          INT NOT NULL,
    request_summary     TEXT,                         -- truncated prompt (first 200 chars)
    response_summary    TEXT,                         -- truncated response (first 500 chars)
    success             BOOLEAN NOT NULL DEFAULT TRUE,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_llm_call_session ON llm_call_log(session_id, turn_index);
CREATE INDEX idx_llm_call_type ON llm_call_log(call_type);
```

#### D14.3.2 JPA Entity + Repository

| File | Detail |
|------|--------|
| `LlmCallLog.java` (new, `model/`) | JPA `@Entity` with all columns above. `@GeneratedValue(strategy = IDENTITY)` for `id`. |
| `LlmCallLogRepository.java` (new, `repository/`) | Spring Data JPA. Methods: `findBySessionIdOrderByCreatedAt(String sessionId)`, `findBySessionIdAndCallType(String sessionId, String callType)` |

#### D14.3.3 LlmCallLogger Service

| File | Detail |
|------|--------|
| `LlmCallLogger.java` (new, `service/observability/`) | Stateless `@Service`. Method: `log(String sessionId, Integer turnIndex, String callType, String model, int promptTokens, int completionTokens, int latencyMs, String requestSummary, String responseSummary, boolean success, String errorMessage)`. Persists to `llm_call_log` via repository. |

#### D14.3.4 Instrumentation Points

| Call Site | How to Instrument |
|-----------|-------------------|
| `LlmInvocationService.invokeRouting()` | After `llmClient.chat()` returns, call `llmCallLogger.log(sessionId, null, "routing", ...)` |
| `LlmInvocationService.invokeChat()` | After `llmClient.chat()` returns, call `llmCallLogger.log(sessionId, turnIndex, "chat", ...)`. For retries: `callType = "retry"` |
| `RerankService.scoreCandidate()` | After `llmClient.chat()` returns, call `llmCallLogger.log(sessionId, turnIndex, "rerank", ...)`. Requires passing `sessionId` + `turnIndex` into `rerank()` method signature. |

**RerankService signature change**:
```java
// Before:
public List<ScoredCandidate> rerank(String query, List<RerankCandidate> candidates)

// After:
public List<ScoredCandidate> rerank(String query, List<RerankCandidate> candidates,
                                     String sessionId, int turnIndex)
```

#### D14.3.5 Trace API Endpoint

| File | Change |
|------|--------|
| `DemoInspectionController.java` | Add `GET /v1/demo/sessions/{id}/llm-calls` → `llmCallLogRepository.findBySessionIdOrderByCreatedAt(id)` |

#### D14.3.6 Admin UI Enhancement

| File | Change |
|------|--------|
| `ui/src/components/admin/TraceViewer.tsx` | Add "LLM Calls" tab. Fetch from `/v1/demo/sessions/{id}/llm-calls`. Render table: `call_type` | `model` | `turn` | `latency_ms` | `tokens (p/c)` | `response_summary`. Show aggregate totals at bottom: total calls, total latency, total tokens. |

### D14.4 Delivery Sequence

```
D14.3.1 (migration)  ─┐
D14.3.2 (entity/repo) ─┤── D14.3.3 (logger) ── D14.3.4 (instrument) ── D14.3.5 (API) ── D14.3.6 (UI)
D14.1 (parallel rerank) ─── (independent, can parallel with D14.3.*)
D14.2 (query enrichment) ── (independent, can parallel with D14.1 / D14.3.*)
```

All three fixes are independent and can be developed in parallel.

### D14.5 Done Criteria (D14.1–D14.3)

- [ ] Rerank latency < 1.5s for 8 candidates (was ~5.7s serial)
- [ ] Vague message "Pls help" with detailed form description → retrieves relevant articles (not faq_miss)
- [ ] `GET /v1/demo/sessions/{id}/llm-calls` returns all LLM calls including routing, rerank, retry
- [ ] Session with 2 turns + 8 rerank candidates each → `llm_call_log` has 17 rows (1 routing + 8 rerank×2)
- [ ] Admin TraceViewer "LLM Calls" tab shows per-call breakdown with aggregate totals
- [ ] Existing unit tests pass; new tests cover parallel rerank, query enrichment logic, and LLM call logging
- [ ] No regressions in `eval-smoke`

### D14.6 Auto-Search on Session Creation (Fix A — "Bot不再说空话")

> **Triggered by**: Session `5ea63180` analysis. Bot returns "Let me look into this for you" but does nothing — waits idle for user input. User's form description `"why I can't post advert"` is ignored until the user repeats it in chat.

**Current state**: `SessionManager.createSession()` (line 260) returns a static greeting: `"Hi {name}! I'm here to help with your inquiry about {topic}. Let me look into this for you."` No knowledge search, no LLM call, no `ControlKernel.processMessage()` — the session is idle until the user sends a chat message.

**Target state**: When routing identifies a FAQ UC and `form_context.description` is non-empty, automatically invoke `ControlKernel.processMessage(session, formDescription)` during session creation. The greeting becomes the actual grounded answer.

**Implementation**:

| File | Change |
|------|--------|
| `SessionManager.java` | In the `ROUTED` case (line 117-126), after setting the phase to RESOLVE, check if `description` is non-empty and the UC is a FAQ UC (not INTAKE). If both true: (1) save session first (`sessionRepository.save(session)`), (2) call `controlKernel.processMessage(session, description)`, (3) use the kernel result's `responseText` as the greeting instead of the static template, (4) save session again with updated state. If the kernel call fails, fall back to the static greeting. |

**FAQ UC detection**: Check against the INTAKE UC set (`UC-G, UC-H, UC-I, UC-J, UC-K`). All other routed UCs are FAQ UCs that support knowledge search.

**Guard conditions**:
- `description != null && description.length() > 10` — avoid auto-search on empty or trivial descriptions like "help"
- `routingResult.outcome() == ROUTED` — only for successfully routed sessions
- UC is NOT an INTAKE UC — intake UCs use fixed scripts, not knowledge search
- Wrap in try/catch — auto-search failure must not break session creation

**Greeting behavior change**:
```
Before: "Hi hr! I'm here to help... Let me look into this for you."  [IDLE]
After:  "Hi hr! <actual grounded answer based on form description>"  [ANSWERED]
```

**Session state after auto-search**: `totalBotTurns = 1`, `currentPhase = CONFIRM` (if answered) or `RESOLVE` (if FAQ miss), `faqMissCount` and `articlesShown` updated.

### D14.7 FAQ Miss Fallback Uses Form Context (Fix B)

> **Triggered by**: Session `5ea63180` Turn 1. Knowledge search returned `faqMiss=true` for query `"hi"`. Bot responded with hardcoded "Could you describe your issue in a bit more detail?" — even though `form_context.description = "why I can't post advert"` was available.

**Current state**: `PhaseEvaluator.resolveFaq()` (lines 197-218) returns a hardcoded ask_user response on FAQ miss, regardless of whether form context contains a detailed description. No LLM is invoked for FAQ miss turns.

**Target state**: When `faqMiss=true` but `form_context.description` exists and is substantive (> 10 chars), invoke the main LLM with a special projection that includes the form description, instructing it to help based on general knowledge or escalate.

**Implementation**:

| File | Change |
|------|--------|
| `PhaseEvaluator.java` | In `resolveFaq()`, replace the hardcoded FAQ miss response block (lines 210-218) with: if `session.getFormContext()` has a non-empty `description` field (> 10 chars), build a context projection with `form_context` and an instruction: `"Knowledge search did not find matching articles, but the user described their issue in the pre-chat form: {description}. Provide a helpful response using your general knowledge about the topic, or choose escalate_human if you cannot help."` Then call `llmInvocation.invokeChat()` and return the LLM's response. If description is absent/short, fall back to the current hardcoded response. |

**LLM invocation details**:
- Build projection via `contextProjection.buildProjection(session, history, null, userMessage)` — null knowledge hits
- Add a `faq_miss_instruction` field to the projection: explains what happened and instructs the LLM to use form context
- The LLM can choose `answer_grounded` (without source_ids — general guidance), `ask_user` (specific follow-up), or `escalate_human`
- This path still increments `faqMissCount` — if the LLM can't help either, the budget limit will trigger escalation on the next turn

**Guard conditions**:
- Only activate when `formDescription.length() > 10` — trivial descriptions like "help" should still trigger the standard "describe your issue" response
- If LLM invocation fails, fall back to the current hardcoded response
- `faqMissCount` is still incremented (preserving budget enforcement)

### D14.8 Event Logging Uses Enriched Query (Fix C)

> **Triggered by**: Session `5ea63180` event log shows `RETRIEVAL_EXECUTED` with `"query":"hi"`, but the actual search used the enriched query. This makes it impossible to debug query enrichment behavior from events.

**Current state**: `PhaseEvaluator.java:193` passes `userMessage` to `emitRetrievalExecuted`, not `enrichedQuery`.

**Target state**: Pass `enrichedQuery` so the event accurately reflects the actual search query.

**Implementation**:

| File | Change |
|------|--------|
| `PhaseEvaluator.java` | Line 193-194: change `userMessage` to `enrichedQuery` in the `emitRetrievalExecuted` call. |

Single-line fix. No other files affected.

### D14.9 Delivery Sequence (D14.6–D14.8)

```
D14.8 (event logging fix) ──── (trivial, independent)
D14.7 (FAQ miss fallback) ──── (independent — modifies PhaseEvaluator FAQ miss block)
D14.6 (auto-search on create) ── (independent — modifies SessionManager.createSession)
```

All three are independent and can be developed in parallel. D14.6 and D14.7 address different code paths (SessionManager vs PhaseEvaluator FAQ miss block).

### D14.10 Done Criteria (D14.6–D14.8)

- [ ] New session with form description "why I can't post advert" → greeting contains actual guidance (not static template)
- [ ] New session with empty form description → static greeting unchanged (backward compatible)
- [ ] INTAKE UC sessions (UC-G/H/I/J/K) → static greeting unchanged (no auto-search)
- [ ] FAQ miss + form description present → LLM invoked with form context, returns helpful response (not hardcoded "describe more")
- [ ] FAQ miss + no form description → hardcoded "describe your issue" response unchanged
- [ ] `RETRIEVAL_EXECUTED` event records enriched query, not raw userMessage
- [ ] `faqMissCount` still incremented on FAQ miss with form fallback (budget enforcement preserved)
- [ ] Auto-search failure does not break session creation (graceful fallback)
- [ ] Existing unit tests pass; new tests cover auto-search, FAQ miss fallback, event logging
- [ ] No regressions in `eval-smoke`

---

## D15. Eval Scoring Bug Fixes (B1-B7)

> **Triggered by**: Analysis of `cs_interactive_001` eval report showing Expected UC / Actual UC / Check names all displayed as N/A, L2 score inflation from alias duplication, and L3 groundedness systematic underscoring for escalation cases.
>
> **Scope**: Python eval harness only (`eval_interactive/`). No Java backend changes.

### D15.1 Fix B1-B5: HTML Report Key Mismatches (Display Only)

**Current state**: `executor._build_case_result()` serializes dict keys that don't match what `html_report._render_case()` reads.

**Key mapping fixes** in `html_report.py`:

| Line | Current read key | Correct read key |
|------|-----------------|------------------|
| 462 | `cr.get("expected_uc", "N/A")` | `cr.get("primary_uc", "N/A")` |
| 463 | `cr.get("actual_uc", "N/A")` | `cr.get("active_use_case", "N/A")` |
| 465 | `cr.get("actual_outcome", "N/A")` | `cr.get("containment_outcome", "N/A")` |
| 471 | `cr.get("source_session_id", "N/A")` | `cr.get("session_id", "N/A")` |
| 516 (L1) | `r.get("check_name", "")` | `r.get("check", "")` |
| 539 (L2) | `r.get("check_name", "")` | `r.get("check", "")` |

Single file change, 6 line edits.

### D15.2 Fix B6: Deduplicate Outcome Check Aliases (Affects Scoring)

**Current state**: `outcome_checks.py` defines aliases (`answer_accuracy → correct_outcome`, `escalation_triggered → escalation_timing`, `resolution_achieved → correct_outcome`). When a CaseSpec configures both a canonical check and its alias, the same function runs twice, inflating the L2 denominator.

**Fix**: In `OutcomeChecker.run_checks()`, track which canonical function has been executed. Skip aliases that map to an already-executed function.

```python
# Build reverse map: alias → canonical name
_ALIAS_MAP = {
    "answer_accuracy": "correct_outcome",
    "escalation_triggered": "escalation_timing",
    "intake_fields_collected": "handover_completeness",
    "resolution_achieved": "correct_outcome",
}

def run_checks(self, case_spec, trace):
    configured = set(case_spec.scoring.outcome_checks)
    results = []
    executed_canonical = set()  # track which canonical checks have run

    for name in configured:
        canonical = _ALIAS_MAP.get(name, name)  # resolve alias to canonical
        if canonical in executed_canonical:
            continue  # skip duplicate
        executed_canonical.add(canonical)
        if name in dispatch:
            results.append(dispatch[name]())

    return results
```

**Impact on cs_interactive_001**: L2 changes from `(1.0+0.5+1.0+0.0)/4 = 0.625` to `(1.0+0.5+0.0)/3 = 0.50`. Composite changes from 0.48 to 0.42.

### D15.3 Fix B7: Groundedness Judge Prompt for Escalation (Affects Scoring)

**Current state**: `llm_judge.py:_judge_groundedness()` (line 100-118) uses a prompt that scores 1/5 when "no source IDs found in any turn" — but for escalation-only responses with zero factual claims, this penalizes correct behavior.

**Fix**: Add an escalation-aware preamble to the scoring rubric:

```python
prompt = f"""\
You are an expert evaluator for a customer service chatbot.

TASK: Evaluate how well the bot's responses are grounded in retrieved knowledge sources.

IMPORTANT: If the bot's response is purely procedural or an escalation message (e.g., 
"transferring you to a human agent", "let me connect you with a specialist") with no 
factual claims about the user's issue, score 5 — there are no claims that require 
grounding. Only score low when the bot makes factual claims without citing sources.

TRANSCRIPT:
{self._format_transcript(transcript)}

SOURCE CITATIONS PER TURN:
{sources_block}

SCORING (1-5):
5 = Every factual claim is backed by a cited source, OR no factual claims were made
4 = Most claims are grounded, minor unsourced details
3 = Some claims are grounded but notable gaps exist
2 = Few claims are grounded; bot invents information
1 = Bot fabricates factual answers without any source backing

Respond with ONLY a JSON object: {{"score": <1-5>, "reasoning": "<brief explanation>"}}"""
```

### D15.4 Delivery Sequence

```
D15.1 (report keys) ─── independent
D15.2 (alias dedup) ─── independent
D15.3 (judge prompt) ── independent
```

All three are fully independent — different files, no shared state.

### D15.5 Done Criteria

- [ ] HTML report shows correct Expected UC, Actual UC, Actual Outcome, Session ID (not N/A)
- [ ] L1/L2 check names display correctly in report tables
- [ ] CaseSpec with `correct_outcome + answer_accuracy` → only one check executes (not duplicated)
- [ ] Escalation-only session → groundedness score ≥ 4.0 (not 1.0)
- [ ] Re-run `cs_interactive_001` eval: verify corrected L2 score (~0.50) and improved L3 groundedness
- [ ] Existing eval tests pass

---

## Appendix A: Dev / Prod Migration Reference

> This appendix is for future reference only. Local demo development should not be blocked by these items.

### Environment Comparison

| Dimension | Local Demo | Dev | Prod |
|-----------|-----------|-----|------|
| LLM | Kimi / QWEN (OpenAI-compatible) | Vertex AI Gemini | Vertex AI Gemini |
| Embedding | DashScope text-embedding-v3 (768-dim) | Vertex AI text-embedding-004 (768-dim) | Vertex AI text-embedding-004 (768-dim) |
| DB | Homebrew PostgreSQL + pgvector | Cloud SQL + pgvector | Cloud SQL + pgvector |
| Redis | Homebrew Redis | GCP Memorystore | GCP Memorystore |
| Salesforce | Mock (local DB) | SF Sandbox | SF Production |
| Gumtree APIs | Mock (fixtures) | Staging APIs | Production APIs |
| Chat transport | HTTP API + React UI | SF Enhanced Chat webhook | SF Enhanced Chat webhook |
| Events | Local PostgreSQL | Kafka + SF sync | Kafka + SF sync |
| Deployment | `mvn spring-boot:run` | GKE (Helm) | GKE (Helm) |
| Feature flags | Always on | GrowthBook | GrowthBook |

### Migration Steps (Local → Dev)

1. `SPRING_PROFILES_ACTIVE=dev`
2. Add `VertexAiGeminiLlmClient` + `VertexAiEmbeddingClient` implementations
3. Re-run knowledge ingestion with Vertex AI embeddings
4. Add `InboundController` (SF webhook) alongside existing `ChatController`
5. Add `KafkaEventPublisher` alongside `LocalEventStore`
6. Add `SalesforceClient` + `SalesforceSessionSync` alongside mock beans
7. Configure: Cloud SQL, Vertex AI, SF Sandbox credentials in `.env.dev`
8. Helm chart + Jenkinsfile + Dockerfile for GKE deployment

### Migration Steps (Dev → Prod)

1. `.env.prod` credentials
2. gumshield API service account approved
3. Bot_Session__c / Bot_Event__c created by SF Admin
4. GrowthBook `cs_bot_enabled` flag
5. Helm deploy to prod GKE
6. 10% → 20% → 50% → 100% rollout
