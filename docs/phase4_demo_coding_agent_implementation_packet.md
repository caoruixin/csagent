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
KIMI_BASE_URL=https://api.moonshot.cn/v1
KIMI_MODEL=moonshot-v1-8k

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
- **Eval harness (Java)**: dataset loader, session simulator, code + model graders, gate evaluator, HTML/JSON report — runs as Maven test profile or standalone JAR

### This demo does NOT cover

- Real Salesforce / Gumtree API integration (mocked)
- GKE / Helm / Jenkins deployment
- Kafka event publishing (local DB)
- GrowthBook feature flags (always-on)
- Email CSAT surveys
- Multi-user concurrent load testing

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

**Retrieval contract** (identical to production Phase 3 §3.5.3):
```
Query → Embed(768-dim) → pgvector ANN(ef_search=100, LIMIT 20, uc_tags filter)
  → Retrieval Gate (top1 cosine_sim < 0.3 → retrieval_miss)
  → Article dedup (highest chunk per article)
  → Rerank top 4-8 (LLM grounding score 1-5)
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
│   │   ├── TraceViewer.tsx           # Turn-by-turn: user msg → projected context → LLM response → action → state
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

### DM9: Eval Harness (Java)

| Aspect | Detail |
|--------|--------|
| **What** | Java-based eval framework, runs as Maven test profile (`mvn verify -Peval-smoke`) or standalone Spring Boot runner. Loads 7 CSV dataset pairs + 367 HR annotations. Replays sessions via HTTP to bot runtime. Runs code-based graders (12 types) + model-based graders (4 types, using same `LlmClient`). Computes all Phase 5 §6 metrics. Evaluates Phase 5 §7 gates. Generates HTML + JSON report |
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
| **5b** | **DM9: Eval** | Yes (with DM8) | DM4-6 | Smoke suite passing; full regression runnable |
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
├── eval/                                     # Java eval harness (Maven submodule)
│   ├── src/main/java/com/gumtree/csagent/eval/
│   │   ├── harness/                          # EvalRunner, DatasetLoader, SessionSimulator
│   │   ├── graders/code/                     # 12 code-based graders
│   │   ├── graders/model/                    # 4 model-based graders (via LlmClient)
│   │   ├── metrics/                          # MetricsAggregator, GateEvaluator
│   │   └── report/                           # HTML + JSON report generators
│   ├── src/main/resources/suites/            # smoke.yaml, full-regression.yaml
│   ├── src/test/java/                        # Grader unit tests
│   └── pom.xml
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
