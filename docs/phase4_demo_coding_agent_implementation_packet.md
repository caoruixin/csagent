# Phase 4 — Demo Coding Agent Implementation Packet

> **Purpose**: Adapt the production Phase 4 implementation packet into a **local Mac demo** that lets stakeholders experience the CS Agent end-to-end — without Salesforce, Gumtree APIs, GKE, or Vertex AI dependencies.
>
> **Delivery Strategy**: Local Demo → Dev Integration → Production
> - **Local Demo (this packet)**: all external dependencies mocked; runs on Mac laptop; uses Kimi/QWEN LLM + DashScope embedding; local PostgreSQL + pgvector; React chat UI
> - **Dev Integration**: swap mocks for real Salesforce/Gumtree sandbox APIs; swap LLM to Vertex AI Gemini; deploy to GKE dev
> - **Production**: prod configs, prod secrets, GrowthBook rollout
>
> **Inputs**: `phase3_detailed_technical_design.md`, `phase5_evaluation_design.md`, `customer_service_tool_spec_v0_2.yaml`, eval datasets (601 sessions), human review annotations (367 sessions), `.env.local` (LLM keys), `design-extract/` (UI reference)

---

## D0. Environment Setup — Local Mac

### D0.1 Prerequisites

| Component | Version | Install |
|-----------|---------|---------|
| Java | 17+ | `brew install openjdk@17` |
| Maven | 3.9+ | `brew install maven` |
| Node.js | 20+ | `brew install node` |
| Python | 3.11+ | `brew install python@3.11` |
| Docker Desktop | Latest | https://docker.com (for PostgreSQL + Redis) |
| Git | Latest | Pre-installed on Mac |

### D0.2 Docker Compose — Local Infrastructure

```yaml
# docker-compose.yml (project root)
version: '3.8'
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: csagent-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: csagent
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: csagent-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s

volumes:
  pgdata:
```

### D0.3 Environment Profiles

Three profiles, switchable via `SPRING_PROFILES_ACTIVE` and `.env.*`:

| Profile | LLM | Embedding | Salesforce | Gumtree APIs | DB | Messaging |
|---------|-----|-----------|------------|-------------|-----|-----------|
| **local** | Kimi / QWEN (OpenAI-compatible) | DashScope `text-embedding-v3` (768-dim) | Mock | Mock | Local Docker PostgreSQL + pgvector | GetStream Chat or local WebSocket |
| **dev** | Vertex AI Gemini | Vertex AI `text-embedding-004` (768-dim) | SF Sandbox | Gumtree Staging APIs | Cloud SQL | Salesforce Enhanced Chat |
| **prod** | Vertex AI Gemini | Vertex AI `text-embedding-004` (768-dim) | SF Production | Gumtree Production APIs | Cloud SQL | Salesforce Enhanced Chat |

**LLM Abstraction**: all LLM calls go through a `LlmClient` interface with two implementations:
- `OpenAiCompatibleLlmClient` — for Kimi (`https://api.moonshot.cn/v1`) and QWEN (`https://dashscope.aliyuncs.com/compatible-mode/v1`); both support OpenAI-compatible chat completions API
- `VertexAiGeminiLlmClient` — for production Vertex AI Gemini

**Embedding Abstraction**: similarly, `EmbeddingClient` interface:
- `DashScopeEmbeddingClient` — DashScope `text-embedding-v3` with `dimension=768` (matches production Vertex AI 768-dim)
- `VertexAiEmbeddingClient` — Vertex AI `text-embedding-004`

### D0.4 Configuration Files

```
# .env.local (already exists — LLM keys)
# Additional local config:
SPRING_PROFILES_ACTIVE=local
DB_HOST=localhost
DB_PORT=5432
DB_NAME=csagent
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379

# LLM (from existing .env.local)
KIMI_API_KEY=sk-GXVVb0AJ...
KIMI_BASE_URL=https://api.moonshot.cn/v1
KIMI_MODEL=moonshot-v1-8k

DASHSCOPE_API_KEY=sk-f4ada48271a54810...
DASHSCOPE_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
DASHSCOPE_CHAT_MODEL=qwen-plus
DASHSCOPE_EMBEDDING_MODEL=text-embedding-v3
DASHSCOPE_EMBEDDING_DIMENSION=768

# Chat UI
GETSTREAM_API_KEY=b2g8vgf3xue3
GETSTREAM_API_SECRET=qrsvyrpgx4yn...
VITE_GETSTREAM_API_KEY=b2g8vgf3xue3
VITE_API_BASE_URL=http://localhost:8080
```

---

## D1. Scope Freeze (Demo)

### This demo covers

- **CS Agent core runtime**: control kernel, state machine, context projection, LLM invocation — functionally identical to production
- **Knowledge pipeline**: 218 articles → chunk → embed (DashScope 768-dim) → pgvector → online search
- **All 10 V1 tools**: 5 agent-visible + 5 runtime-only (including `get_message_moderation_context`); external API tools return mock data
- **Guardrails**: fixed_script_library, forbidden phrase detection, PII redaction — identical to production
- **Demo Chat UI**: React + Vite web app with Gumtree-inspired design; pre-chat form → chat conversation → resolution/escalation
- **Mock Salesforce layer**: case read, session management, handover logging — all local with inspectable payloads
- **Mock Gumtree APIs**: account lookup, listing lookup, moderation review, message moderation — configurable mock responses
- **Observability (local)**: structured events → local PostgreSQL; trace viewer UI; no Kafka (replaced by direct DB write)
- **Eval harness**: Python-based; dataset loader, session simulator, code + model graders, gate evaluator, report generator

### This demo does NOT cover

- Real Salesforce integration (mocked)
- Real Gumtree internal APIs (mocked)
- GKE / Helm / Jenkins deployment
- Kafka event publishing (local DB instead)
- GrowthBook feature flags (always-on in demo)
- Email CSAT surveys
- Multi-user concurrent sessions (demo is single-user focused)

---

## D2. Module Breakdown

### DM1: Local Infrastructure & Service Foundation

| Aspect | Detail |
|--------|--------|
| **What** | Docker Compose (PostgreSQL + pgvector + Redis), Spring Boot 3.2.x project skeleton, Flyway DB migrations (all tables from Phase 3 §3.2), health endpoints, environment profile system (local/dev/prod), LLM client abstraction, embedding client abstraction |
| **Output** | `docker compose up` → healthy infra; `mvn spring-boot:run` → service starts with health checks passing |
| **Key files** | `docker-compose.yml`, `pom.xml`, `application.yml`, `application-local.yml`, `/src/main/resources/db/migration/*.sql`, `LlmClient.java` (interface), `OpenAiCompatibleLlmClient.java`, `EmbeddingClient.java`, `DashScopeEmbeddingClient.java` |

**DB Migrations** (same as production, all Flyway):
- `V1__create_bot_sessions.sql` — `bot_sessions` table (Phase 3 §3.2.2, including `active_use_case` supports OUT_OF_SCOPE_* values)
- `V2__create_bot_turns.sql` — `bot_turns` table (Phase 3 §3.2.3)
- `V3__create_session_outcomes.sql` — `session_outcomes` table (Phase 3 §3.2.4)
- `V4__create_kb_articles.sql` — `kb_articles` table (Phase 3 §3.5.1)
- `V5__create_kb_chunks.sql` — `kb_chunks` + HNSW index (Phase 3 §3.5.1)
- `V6__create_mock_salesforce.sql` — **Demo-only**: `mock_cases`, `mock_handover_log`, `mock_sessions` tables for inspectable mock data

### DM2: Knowledge Pipeline

| Aspect | Detail |
|--------|--------|
| **What** | Offline: Python ingestion script (`scripts/ingest_knowledge.py`) — read `knowledge_base_articles.json` + `article_uc_mapping.csv` → clean HTML → chunk (256-512 tokens, 10-20% overlap) → embed via DashScope `text-embedding-v3` (768-dim) → bulk insert pgvector. Online: `/v1/faq/search` endpoint with ANN Top-20 → metadata filter → dedup → rerank → faq_miss two-stage gate |
| **Output** | 218 articles indexed with ~500 chunks; `python scripts/ingest_knowledge.py` runs idempotently; search endpoint returns top-3 with `faq_miss` flag |
| **Key files** | `scripts/ingest_knowledge.py`, `scripts/requirements.txt`, `KnowledgeSearchService.java`, `KnowledgeSearchController.java`, `EmbeddingClient.java` |
| **Data inputs** | `data/knowledge/knowledge_base_articles.json`, `data/knowledge/article_uc_mapping.csv` |

**Embedding compatibility**: DashScope `text-embedding-v3` supports `dimension` parameter — set to 768 to match production Vertex AI. When switching to dev/prod, only the embedding client implementation changes; pgvector schema and queries remain identical.

**Retrieval contract** (identical to production):
```
Query → Embed(768-dim) → ANN(ef_search=100, LIMIT 20, uc_tags filter)
  → Retrieval Gate (top1 cosine_sim < 0.3 → retrieval_miss)
  → Article dedup → Rerank top 4-8 (LLM-based grounding score)
  → Answer Gate (grounding_score < 3.5 → answer_miss)
  → Return top 3
```

### DM3: Mock Integration Layer

| Aspect | Detail |
|--------|--------|
| **What** | Mock implementations for all external dependencies: Salesforce (case CRUD, session management, Omni-Channel transfer, business hours), Gumtree internal APIs (account lookup, listing lookup, moderation review, message moderation). All mocks backed by local PostgreSQL tables for inspectability. **Handover payloads logged to `mock_handover_log` table** with full JSON payload visible via API/UI |
| **Output** | All tool chains work end-to-end with realistic mock data; handover payloads inspectable at `/v1/demo/handover-logs` |
| **Key files** | `MockSalesforceService.java`, `MockGumtreeApiService.java`, `MockDataInitializer.java`, `DemoInspectionController.java` |

**Mock Data Strategy**:

| External Service | Mock Approach | Inspectable Endpoint |
|-----------------|---------------|---------------------|
| Salesforce Case | Pre-seeded cases in `mock_cases` table; new cases created by `create_case_controlled` written here | `GET /v1/demo/cases` |
| Salesforce Omni-Channel | Logs transfer request to `mock_handover_log`; returns `transferred` / `offline_logged` based on configurable `mock_business_hours` flag | `GET /v1/demo/handover-logs` |
| Salesforce Bot_Session__c / Bot_Event__c | Written to local `bot_sessions` / `bot_turns` tables (same schema as production) | `GET /v1/demo/sessions`, `GET /v1/demo/events` |
| Gumtree bapi-server (account/listing) | JSON fixture files in `src/main/resources/mock/` → loaded by `MockGumtreeApiService` | `GET /v1/demo/mock-data/{type}` |
| Gumtree gumshield-api (moderation) | JSON fixture files with configurable review reasons per ad_id | Same as above |
| Gumtree message-moderation-history | Fixture files for UC-C diagnostic scenarios | Same as above |
| Business hours | Configurable via `mock.business_hours=true/false` in application-local.yml | Toggle at runtime |

**Mock Fixture Files** (`src/main/resources/mock/`):
```
mock/
├── accounts/              # Per-email account fixtures
│   ├── active_user.json
│   ├── suspended_user.json
│   └── blacklisted_user.json
├── listings/              # Per-ad-id listing fixtures
│   ├── live_ad.json
│   ├── removed_ad_policy.json
│   ├── removed_ad_multiple_accounts.json
│   └── processing_ad.json
├── moderation_reviews/    # Per-ad-id moderation review fixtures
│   ├── multiple_accounts.json
│   ├── prohibited_content.json
│   └── pet_limit.json
├── message_moderation/    # Message moderation fixtures
│   ├── clean.json
│   └── blocked_messages.json
└── cases/                 # Pre-seeded case fixtures
    ├── ad_support_case.json
    ├── account_support_case.json
    └── gdpr_request_case.json
```

### DM4: Core Runtime

| Aspect | Detail |
|--------|--------|
| **What** | Functionally identical to production M4 — session lifecycle, form_context_ingestion, Control Kernel (INIT→DISCOVER→RESOLVE→CONFIRM→CLOSE/ESCALATE), budget enforcement, drift detection, context projection, LLM invocation. **Key difference**: LLM calls via `OpenAiCompatibleLlmClient` (Kimi/QWEN) instead of Vertex AI |
| **Output** | Given user message + session state → action decision + bot response |
| **Key files** | `SessionManager.java`, `FormContextIngestionService.java`, `ControlKernel.java`, `UseCaseRouter.java`, `ContextProjectionBuilder.java`, `LlmInvocationService.java`, `ActionParser.java` |

**LLM Invocation** (OpenAI-compatible format):
```java
// All LLM providers use the same chat completions format
POST {BASE_URL}/chat/completions
{
  "model": "{MODEL_NAME}",  // moonshot-v1-8k / qwen-plus / gemini-2.0-flash
  "messages": [
    {"role": "system", "content": "{system prompt with projected context}"},
    {"role": "user", "content": "{user message}"}
  ],
  "temperature": 0.3,
  "response_format": {"type": "json_object"}
}
```

**LLM Structured Output**: Bot expects JSON response:
```json
{
  "action": "answer_grounded",        // ask_user | retrieve_knowledge | answer_grounded | escalate_human | finish
  "parameters": {                     // action-specific params
    "query": "...",                    // for retrieve_knowledge
    "escalation_reason": "...",       // for escalate_human
    "intake_field": "..."             // for ask_user intake
  },
  "user_message": "Here's what I found...",  // message to show user
  "reasoning": "User asks about..."   // internal reasoning (logged, not shown)
}
```

**Two-stage UC Routing** (v8 HR-calibrated, from Phase 2 §2.11.2):
- Stage 1: Topic Subject prior evaluation (strong/weak/fallback)
- Stage 2: Description-based classification (LLM-powered)
- **Account Support special handling**: skip Stage 1 prior, go directly to full-candidate-set classification
- **OUT_OF_SCOPE detection**: if handover-only Topic Subject + no UC match → OUT_OF_SCOPE_* → fixed_script → ESCALATE

**Control budgets** (identical to production):
- max_clarification_rounds = 2
- max_faq_miss = 2
- max_bot_turns_per_issue = 15 (FAQ) / 10 (Intake)
- max_repeated_same_action = 2
- max_total_bot_turns = 25

### DM5: Tool Layer

| Aspect | Detail |
|--------|--------|
| **What** | Tool Dispatcher + Policy Enforcer; all 10 V1 tools (5 agent-visible + 5 runtime-only). External API tools delegate to DM3 mock layer. Knowledge tools use real pgvector. Policy enforcement identical to production |
| **Output** | All tools callable; scope violations rejected with `scope_blocked` |
| **Key files** | `ToolDispatcher.java`, `ToolPolicyEnforcer.java`, + per-tool implementation classes |

**Per-UC tool matrix** (identical to production Phase 2 §2.10):
- `search_knowledge` / `resolve_article`: UC-A/B/C/D/E/F/FP only
- `get_customer_context`: UC-A/C/D/F/FP/K only → delegates to mock account/listing/moderation
- `get_message_moderation_context`: UC-C only (v0.2.1)
- `create_case_controlled`: UC-H/J/K only → writes to `mock_cases` table
- `request_handover` / `record_outcome`: all UCs + OUT_OF_SCOPE_*

### DM6: Guardrails & UX

| Aspect | Detail |
|--------|--------|
| **What** | Identical to production — fixed script library (YAML templates from `fixed_script_library_v1.md`), forbidden phrase detector, PII redaction, progress placeholder |
| **Output** | Templates render correctly; forbidden phrases caught; PII stripped from logs |
| **Key files** | `ScriptLibraryService.java`, `ForbiddenPhraseDetector.java`, `PiiRedactionFilter.java`, `ProgressPlaceholderService.java`, `/src/main/resources/scripts/*.yaml` |

**Template categories** (14 + OOS): opening, empathy, hold_placeholder, identifier_request, resolution_check, escalation_business_hours, escalation_off_hours, gdpr_intake, appeal_intake, dispute_disclaimer, safety_intake, tech_troubleshoot, policy_explanation, idle_close + OOS templates (oos_delivery, oos_pro_contract, oos_account_manager, oos_ratings_reviews, oos_generic)

### DM7: Observability (Local)

| Aspect | Detail |
|--------|--------|
| **What** | Structured events → local PostgreSQL (no Kafka in demo). Per-turn trace writer. Micrometer metrics exposed at `/internal/metrics`. Simple trace viewer accessible via demo UI |
| **Output** | Every action emits structured event; traces queryable via API; metrics visible |
| **Key files** | `EventEmitter.java`, `TraceWriter.java`, `LocalEventStore.java` (replaces KafkaEventPublisher), `MetricsService.java` |

**Events** (identical to production): SESSION_STARTED, USE_CASE_INFERRED, RETRIEVAL_EXECUTED, ARTICLE_SHOWN, CLARIFICATION_ASKED, ESCALATION_REQUESTED, CASE_CREATED, OUTCOME_RECORDED, SESSION_CLOSED, TOOL_SCOPE_BLOCKED, GUARDRAIL_VIOLATION, OUT_OF_SCOPE_HANDOVER

**Demo inspection endpoints**:
- `GET /v1/demo/sessions` — list all bot sessions with outcome
- `GET /v1/demo/sessions/{id}/trace` — full turn-by-turn trace for a session
- `GET /v1/demo/sessions/{id}/events` — event timeline
- `GET /v1/demo/handover-logs` — all handover payloads (JSON, inspectable)
- `GET /v1/demo/metrics/funnel` — session funnel (total/understood/resolved/escalated/abandoned)

### DM8: Demo Chat UI

| Aspect | Detail |
|--------|--------|
| **What** | React + Vite + TypeScript web application. Gumtree-inspired design (from `design-extract/`). Includes: pre-chat form, real-time chat conversation, article cards with links, resolution confirmation, escalation flow, admin/inspection panel |
| **Output** | `http://localhost:5173` — full chat experience; `http://localhost:5173/admin` — trace viewer + handover log inspector |
| **Key files** | `ui/` directory — React app |
| **Dependencies** | DM4 (backend API) |

**UI Architecture**:
```
ui/
├── src/
│   ├── components/
│   │   ├── chat/
│   │   │   ├── ChatWidget.tsx          # Main chat container (bottom-right floating)
│   │   │   ├── PreChatForm.tsx         # Pre-chat form (first_name, email, topic_subject, ad_id, description)
│   │   │   ├── MessageBubble.tsx       # User/bot message bubbles
│   │   │   ├── ArticleCard.tsx         # Knowledge article display (title + summary + link)
│   │   │   ├── ResolutionCheck.tsx     # "Did that answer your question?" Yes/No
│   │   │   ├── EscalationNotice.tsx    # Handover message display
│   │   │   ├── TypingIndicator.tsx     # Bot thinking/loading state
│   │   │   └── ChatInput.tsx           # Message input + send button
│   │   ├── admin/
│   │   │   ├── SessionList.tsx         # List all sessions with status
│   │   │   ├── TraceViewer.tsx         # Turn-by-turn trace with state snapshots
│   │   │   ├── HandoverLogViewer.tsx   # Handover payload JSON inspector
│   │   │   ├── EventTimeline.tsx       # Event timeline visualization
│   │   │   └── MetricsDashboard.tsx    # Funnel metrics display
│   │   └── layout/
│   │       ├── Header.tsx              # Gumtree-style nav bar
│   │       └── DemoPage.tsx            # Demo landing page with chat widget
│   ├── api/
│   │   └── client.ts                   # API client for backend
│   ├── styles/
│   │   └── gumtree-theme.css           # Design tokens from design-extract
│   ├── App.tsx
│   └── main.tsx
├── package.json
├── vite.config.ts
└── tsconfig.json
```

**Design Tokens** (from `design-extract/design-guide.md`):
- Primary: `#0D475C` (Gumtree teal)
- Accent CTA: `#5CE00B` (lime green)
- Text: `#000000` / `#3C3241` / `#635B67`
- Background: `#FFFFFF` / `#F1F1F1`
- Font: "Readex Pro", sans-serif
- Card radius: 8px, shadow: `rgba(0,0,0,0.2) 0 0 12px`

**Chat Widget Behavior**:
1. Floating button (bottom-right) → click to open chat drawer
2. Pre-chat form slides in: First Name*, Email*, Topic Subject* (dropdown, 11 options), Ad ID (optional), Description*
3. Submit → create session → bot opening message with greeting
4. Real-time message exchange → article cards → resolution check → close or escalate
5. Escalation → handover notice with case number (if created) + next steps
6. Admin panel: separate route (`/admin`) for trace viewer, handover logs, event timeline

**API Integration** (chat UI → backend):
```typescript
// Start session (pre-chat form submit)
POST /v1/chat/sessions
{
  form_data: { first_name, last_name, email, topic_subject, ad_id_number, description }
}
→ { session_id, reply_text, intent }

// Send message
POST /v1/chat/sessions/{session_id}/messages
{
  message_text: "my ad was removed"
}
→ { reply_text, intent, should_end_chat, additional_data }
```

### DM9: Eval Harness

| Aspect | Detail |
|--------|--------|
| **What** | Python-based eval runner. Loads 7 CSV dataset pairs + 367 HR annotations. Replays sessions through bot runtime. Runs code-based + model-based graders. Computes all Phase 5 §6 metrics. Evaluates Phase 5 §7 launch gates. Generates HTML/JSON report |
| **Output** | `python -m eval.run --suite smoke` runs 75 sessions in ~5min; `--suite full` runs 601 sessions in ~30min |
| **Key files** | `eval/` directory (Python) |

**Eval Architecture**:
```
eval/
├── harness/
│   ├── runner.py              # Main orchestrator
│   ├── dataset_loader.py      # Load CSV + HR annotations overlay
│   ├── session_simulator.py   # Replay turns via HTTP to bot runtime
│   ├── form_normalizer.py     # Old form → new form field mapping
│   └── result_collector.py    # Aggregate per-session results
├── graders/
│   ├── code/
│   │   ├── routing_grader.py        # UC routing accuracy
│   │   ├── escalation_grader.py     # Escalation recall/precision/timing
│   │   ├── handover_grader.py       # Payload completeness, schema validation
│   │   ├── control_grader.py        # Budget enforcement, phase transitions
│   │   ├── tool_contract_grader.py  # Tool scope, sequence matching
│   │   ├── policy_grader.py         # Forbidden phrases, PII leakage
│   │   └── drift_grader.py          # Drift type classification, issue preservation
│   └── model/
│       ├── groundedness_grader.py   # LLM judge: grounded or not
│       ├── relevance_grader.py      # LLM judge: 1-5 relevance score
│       ├── summary_quality_grader.py  # LLM judge: handover summary quality
│       └── clarification_grader.py  # LLM judge: necessary vs unnecessary
├── metrics/
│   ├── aggregator.py          # Compute all §6 metrics
│   └── gate_evaluator.py      # Check §7 launch gates (hard + soft)
├── reports/
│   ├── html_report.py         # Generate HTML report with charts
│   └── json_report.py         # Machine-readable results
├── suites/
│   ├── smoke.yaml             # 75 sessions (PR gate)
│   ├── full_regression.yaml   # 601 sessions (release gate)
│   └── nightly.yaml           # Full + extended
├── config.yaml                # LLM config for model-based graders
└── requirements.txt
```

**Eval LLM**: model-based graders use the same LLM abstraction. In local demo, QWEN is used as judge (via OpenAI-compatible API). In prod, Gemini.

**HR Annotations Overlay** (v8): when `human_review_overlay` is configured, the eval harness:
1. Joins HR annotations by `session_id`
2. Overrides auto-classified UC with `primary_uc_corrected` where different
3. Uses HR `expected_tool_sequence` and `forbidden_tools` for tool contract grading
4. Uses HR `drift_type` for drift grading
5. Uses HR `should_escalate` + `escalation_trigger` for escalation grading

**Gate Evaluation** (from Phase 5 §7):

Hard gates (must pass):
- critical_policy_violation = 0
- wrong_containment ≤ 2%
- groundedness_pass_rate ≥ 98%
- escalation_recall ≥ 95%
- handover_completeness ≥ 98%
- tool_scope_violation = 0
- forbidden_phrase = 0
- budget_enforcement = 100%
- phase_transition_validity = 100%
- critical/high risk escalation compliance = 100%
- OUT_OF_SCOPE detection ≥ 90%

### DM10: E2E Demo Wiring & Demo Scripts

| Aspect | Detail |
|--------|--------|
| **What** | Connect all modules; provide demo walkthrough scripts; sample conversations for each UC; mock data configuration for demo scenarios |
| **Output** | Single-command startup; guided demo script; stakeholder presentation ready |

**One-Command Startup**:
```bash
# Terminal 1: Infrastructure
docker compose up -d

# Terminal 2: Knowledge ingestion (one-time)
cd scripts && pip install -r requirements.txt
python ingest_knowledge.py

# Terminal 3: Backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 4: Frontend
cd ui && npm install && npm run dev

# Terminal 5: Eval (optional)
cd eval && pip install -r requirements.txt
python -m eval.run --suite smoke --base-url http://localhost:8080
```

Or via Makefile:
```makefile
demo-up:     docker compose up -d && sleep 3 && make ingest && make backend & make frontend
demo-down:   docker compose down
ingest:      cd scripts && python ingest_knowledge.py
backend:     mvn spring-boot:run -Dspring-boot.run.profiles=local
frontend:    cd ui && npm run dev
eval-smoke:  cd eval && python -m eval.run --suite smoke
eval-full:   cd eval && python -m eval.run --suite full
```

**Demo Scenarios** (pre-configured with mock data):

| # | Scenario | Topic Subject | Expected UC | Expected Outcome | Mock Data Needed |
|---|----------|--------------|-------------|-----------------|-----------------|
| 1 | "My ad is not showing" | Ad Support | UC-A-01 | Resolved (FAQ) | live_ad fixture |
| 2 | "How do I post an ad?" | Ad Support | UC-B-01 | Resolved (FAQ) | — |
| 3 | "Why was my ad removed?" | Ad Support | UC-FP-01 | Resolved (policy explanation) | removed_ad_multiple_accounts fixture |
| 4 | "My ad was removed unfairly, I want to appeal" | Ad Support | UC-H-01 | Escalated (intake + case) | removed_ad fixture |
| 5 | "I can't log in" | Account Support | UC-D-01 | Resolved (FAQ) | active_user fixture |
| 6 | "I want to delete my account" | Delete My Account or Data | UC-G-01 | Escalated (GDPR intake) | — |
| 7 | "I was scammed by a seller" | Report a Safety Issue | UC-J-01 | Escalated (safety intake + case) | — |
| 8 | "I want a refund" | Payments | UC-I-01 | Escalated (dispute handover) | — |
| 9 | "I'm not getting replies to my messages" | Replies or Messaging | UC-C-01 | Resolved or Escalated | account + listing + message_moderation fixtures |
| 10 | "I need help with a delivery" | Delivery | OUT_OF_SCOPE_DELIVERY | Escalated (OOS handover) | — |
| 11 | User says "talk to an agent" mid-conversation | Any | Any | Immediate escalation | — |
| 12 | User frustration/threat | Any | UC-H or UC-J | Escalated (user_distress) | — |

---

## D3. Delivery Order

```text
Phase 1 (Foundation)         Phase 2 (Intelligence)       Phase 3 (Experience)
─────────────────────       ───────────────────────       ────────────────────

  ┌──────┐                    ┌──────┐                     ┌──────┐
  │ DM1  │───────────────────▶│ DM4  │────────────────────▶│ DM8  │
  │Infra │                    │Core  │                      │UI    │
  └──┬───┘                    │Runtm │                      └──┬───┘
     │                        └──┬───┘                         │
     │    ┌──────┐               │         ┌──────┐         ┌──▼───┐
     ├───▶│ DM2  │───────────────┤    ┌───▶│ DM6  │────────▶│ DM10 │
     │    │Knowl │               │    │    │Guard │          │E2E   │
     │    └──────┘               │    │    └──────┘          └──────┘
     │    ┌──────┐               │    │    ┌──────┐
     └───▶│ DM3  │───────────────┘    │    │ DM7  │
          │Mock  │                    │    │Obsrv │
          └──────┘               ┌────┘    └──────┘
                                 │
                              ┌──▼───┐     ┌──────┐
                              │ DM5  │     │ DM9  │
                              │Tools │     │Eval  │
                              └──────┘     └──────┘
```

### Recommended Delivery Sequence

| Order | Module | Parallel? | Prerequisite | Key Deliverable |
|-------|--------|-----------|-------------|----------------|
| **1** | **DM1: Infrastructure** | — | None | Docker Compose up; Spring Boot runs; DB migrations applied |
| **2a** | **DM2: Knowledge Pipeline** | Yes (with DM3) | DM1 | 218 articles in pgvector; search endpoint working |
| **2b** | **DM3: Mock Integration** | Yes (with DM2) | DM1 | All mock services returning fixture data; handover log endpoint |
| **3** | **DM4: Core Runtime** | — | DM1, DM2, DM3 | State machine + LLM invocation + UC routing working via API |
| **4a** | **DM5: Tool Layer** | Yes (with DM6, DM7) | DM2, DM3, DM4 | All 10 tools dispatchable; mock chains working |
| **4b** | **DM6: Guardrails** | Yes (with DM5, DM7) | DM4 | Script library + forbidden phrases + PII redaction |
| **4c** | **DM7: Observability** | Yes (with DM5, DM6) | DM1 | Events logged; trace queryable; demo inspection endpoints |
| **5a** | **DM8: Demo Chat UI** | Yes (with DM9) | DM4 | Chat widget + pre-chat form + admin panel |
| **5b** | **DM9: Eval Harness** | Yes (with DM8) | DM4, DM5, DM6 | Smoke suite passing; full regression runnable |
| **6** | **DM10: E2E Wiring** | — | All (DM1-DM9) | One-command startup; demo scenarios verified; stakeholder ready |

**Critical path**: DM1 → DM4 → DM5 → DM8/DM9 → DM10

---

## D4. Required Contracts

### D4.1 DB Schema

Identical to production (Phase 3 §3.2, §3.5) plus:

| File | Tables | Note |
|------|--------|------|
| `V1-V5` | Same as production | `bot_sessions`, `bot_turns`, `session_outcomes`, `kb_articles`, `kb_chunks` |
| `V6__create_mock_tables.sql` | `mock_cases`, `mock_handover_log` | Demo-only; dropped when migrating to real Salesforce |

### D4.2 API Contracts

| Endpoint | Method | Purpose | Note |
|----------|--------|---------|------|
| `POST /v1/chat/sessions` | POST | Create session (pre-chat form) | **Demo-specific**: replaces SF webhook |
| `POST /v1/chat/sessions/{id}/messages` | POST | Send user message | **Demo-specific**: replaces SF webhook |
| `GET /v1/chat/sessions/{id}` | GET | Get session state | **Demo-specific** |
| `POST /v1/faq/search` | POST (internal) | Knowledge search | Same as production |
| `GET /v1/demo/sessions` | GET | List all sessions | **Demo-only inspection** |
| `GET /v1/demo/sessions/{id}/trace` | GET | Full trace | **Demo-only inspection** |
| `GET /v1/demo/handover-logs` | GET | Handover payloads | **Demo-only inspection** |
| `GET /v1/demo/events` | GET | Event timeline | **Demo-only inspection** |
| `GET /v1/demo/metrics/funnel` | GET | Funnel metrics | **Demo-only inspection** |
| `GET /internal/health` | GET | Health check | Same as production |

### D4.3 Config Schemas

Same as production (use-case-registry.yaml, control-policy.yaml, tool-policy.yaml, escalation-triggers.yaml, scripts/*.yaml) plus:

| Config | Format | Demo-specific |
|--------|--------|--------------|
| `application-local.yml` | YAML | Local profile with mock flags, LLM keys |
| `mock-data-config.yaml` | YAML | Which mock fixtures to load, business hours flag |

---

## D5. Required Tests

### Per-Module Test Requirements

| Module | Unit Tests | Integration Tests |
|--------|-----------|------------------|
| **DM1** | Health endpoint, config loading, DB migration | Docker PostgreSQL connectivity, Redis connectivity |
| **DM2** | Chunking logic, embedding mock, ANN query, rerank, faq_miss gate | DashScope embedding call, pgvector search E2E |
| **DM3** | Mock fixture loading, handover log write, case creation | Mock API response correctness |
| **DM4** | Phase transitions (all 12), budget enforcement, drift detection (3 types), context projection, UC routing (including Account Support full-candidate), OUT_OF_SCOPE detection | Full turn cycle with mock tools |
| **DM5** | Per-tool validation, UC scope matrix (all cells), tool chain composition | Mock API chain E2E |
| **DM6** | Template rendering (50+ templates), forbidden phrase (9 categories), PII regex | — |
| **DM7** | Event emission (12 types), trace schema validation | Local DB event write |
| **DM8** | Component rendering (React Testing Library) | — |
| **DM9** | Dataset loading (7+1 HR), grader logic (12 code graders), metrics aggregation, gate evaluation | Smoke suite (75 sessions) against running bot |

### Critical Demo Test Scenarios

| # | Scenario | Module | What's Verified |
|---|----------|--------|----------------|
| 1 | FAQ UC resolved with grounded answer + article link | DM4+DM5 | Knowledge retrieval → answer → resolution check |
| 2 | Intake UC (UC-H) → create_case → handover with payload | DM4+DM5+DM3 | Full intake flow; payload logged to `mock_handover_log` |
| 3 | "Talk to agent" → immediate escalation | DM4 | User-requested escalation from any phase |
| 4 | faq_miss=2 → automatic escalation | DM4 | Budget enforcement |
| 5 | Tool scope violation → scope_blocked | DM5 | Policy enforcer blocks UC-H from search_knowledge |
| 6 | Forbidden phrase in LLM output → blocked | DM6 | Guardrail detection and substitution |
| 7 | OUT_OF_SCOPE Topic Subject → handover | DM4+DM6 | OOS detection → fixed_script → escalation |
| 8 | "Account Support" → correct UC via Description classification | DM4 | Two-stage routing with weak prior |
| 9 | Handover payload JSON validates against schema | DM5+DM3 | Payload inspectable at `/v1/demo/handover-logs` |
| 10 | Eval smoke suite passes all hard gates | DM9 | 75 sessions, all gates green |
| 11 | Pre-chat form → auto-trigger get_customer_context | DM4+DM5 | Form email → mock account lookup |
| 12 | UC-C messaging diagnostic → message moderation check | DM5 | get_message_moderation_context chain |

---

## D6. Done Criteria

### Demo Complete When

- [ ] `docker compose up` starts PostgreSQL + pgvector + Redis cleanly
- [ ] 218 articles indexed in pgvector (verified via `SELECT count(*) FROM kb_articles`)
- [ ] `mvn spring-boot:run -Dspring-boot.run.profiles=local` starts backend with health checks passing
- [ ] Chat UI (`http://localhost:5173`) loads with Gumtree-inspired design
- [ ] Pre-chat form submits → bot greeting appears with user's first name
- [ ] All 12 demo scenarios (§DM10) work end-to-end through the UI
- [ ] Handover payloads visible and inspectable at `/v1/demo/handover-logs` with full JSON schema
- [ ] Admin panel (`/admin`) shows: session list, trace viewer, event timeline, handover logs
- [ ] Eval smoke suite (75 sessions) passes all hard gates
- [ ] Eval full regression (601 sessions) produces report with all metrics
- [ ] Forbidden phrase detector catches all 9 categories (0 false negatives on test corpus)
- [ ] PII redaction verified in projected_context and bot_turns
- [ ] OUT_OF_SCOPE topics (Delivery, Pro Contract, Account Manager, Ratings Reviews) correctly route to handover
- [ ] LLM provider switchable by changing `SPRING_PROFILES_ACTIVE` (local→dev config swap)
- [ ] Mock/real service switchable by profile (no code changes needed for dev migration)

### Demo Presentation Ready When

- [ ] 5-minute guided walkthrough script prepared
- [ ] Each UC type demonstrable with pre-configured mock data
- [ ] Trace viewer shows clear decision path for each demo conversation
- [ ] Handover payloads demonstrably contain all required fields from Phase 3 §3.6.2
- [ ] Eval report printable/shareable with gate pass/fail status

---

## D7. Repo Structure (Demo)

```
csagent/
├── server/
│   ├── src/main/java/com/gumtree/csagent/
│   │   ├── controller/
│   │   │   ├── ChatController.java          # /v1/chat/sessions, /v1/chat/sessions/{id}/messages
│   │   │   ├── HealthController.java        # /internal/health
│   │   │   └── DemoInspectionController.java # /v1/demo/* (demo-only inspection endpoints)
│   │   ├── service/
│   │   │   ├── runtime/
│   │   │   │   ├── SessionManager.java
│   │   │   │   ├── ControlKernel.java
│   │   │   │   ├── UseCaseRouter.java       # Two-stage UC routing (v8 HR-calibrated)
│   │   │   │   ├── ContextProjectionBuilder.java
│   │   │   │   ├── FormContextIngestionService.java
│   │   │   │   └── DriftDetector.java
│   │   │   ├── llm/
│   │   │   │   ├── LlmClient.java           # Interface
│   │   │   │   ├── OpenAiCompatibleLlmClient.java  # Kimi / QWEN (local)
│   │   │   │   ├── VertexAiGeminiLlmClient.java     # Gemini (dev/prod)
│   │   │   │   └── ActionParser.java
│   │   │   ├── embedding/
│   │   │   │   ├── EmbeddingClient.java     # Interface
│   │   │   │   ├── DashScopeEmbeddingClient.java    # DashScope (local)
│   │   │   │   └── VertexAiEmbeddingClient.java     # Vertex AI (dev/prod)
│   │   │   ├── knowledge/
│   │   │   │   ├── KnowledgeSearchService.java
│   │   │   │   └── RerankService.java
│   │   │   ├── tools/
│   │   │   │   ├── ToolDispatcher.java
│   │   │   │   ├── ToolPolicyEnforcer.java
│   │   │   │   ├── SearchKnowledgeTool.java
│   │   │   │   ├── ResolveArticleTool.java
│   │   │   │   ├── GetCustomerContextTool.java
│   │   │   │   ├── RequestHandoverTool.java
│   │   │   │   ├── RecordOutcomeTool.java
│   │   │   │   ├── CreateCaseControlledTool.java
│   │   │   │   └── ... (lookup services)
│   │   │   ├── mock/                         # Demo mock implementations
│   │   │   │   ├── MockSalesforceService.java
│   │   │   │   ├── MockGumtreeApiService.java
│   │   │   │   └── MockDataInitializer.java
│   │   │   ├── guardrails/
│   │   │   │   ├── ScriptLibraryService.java
│   │   │   │   ├── ForbiddenPhraseDetector.java
│   │   │   │   └── PiiRedactionFilter.java
│   │   │   └── observability/
│   │   │       ├── EventEmitter.java
│   │   │       ├── TraceWriter.java
│   │   │       └── LocalEventStore.java      # Demo: replaces Kafka
│   │   ├── model/
│   │   └── config/
│   ├── src/main/resources/
│   │   ├── db/migration/
│   │   ├── prompts/                          # LLM prompt templates (versioned)
│   │   ├── scripts/                          # Fixed script library YAML (14+ categories)
│   │   ├── config/                           # use-case-registry.yaml, control-policy.yaml, etc.
│   │   ├── mock/                             # Mock fixture JSON files
│   │   ├── application.yml
│   │   ├── application-local.yml
│   │   └── application-dev.yml
│   └── src/test/
├── ui/                                       # React + Vite frontend
│   ├── src/
│   │   ├── components/chat/
│   │   ├── components/admin/
│   │   ├── components/layout/
│   │   ├── api/
│   │   ├── styles/
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── package.json
│   └── vite.config.ts
├── eval/                                     # Python eval harness
│   ├── harness/
│   ├── graders/code/
│   ├── graders/model/
│   ├── metrics/
│   ├── suites/
│   ├── reports/
│   ├── config.yaml
│   └── requirements.txt
├── scripts/
│   ├── ingest_knowledge.py                   # Knowledge ingestion script
│   └── requirements.txt
├── data/
│   ├── eval_datasets/                        # 7 CSV pairs (601 sessions)
│   ├── human_review_annotations_2026-04-22_complete.csv
│   └── knowledge/
├── docker-compose.yml
├── Makefile
├── .env.local
├── .env.dev
└── .env.prod
```

---

## D8. Local → Dev → Prod Migration Path

| Dimension | Local Demo | Dev | Prod |
|-----------|-----------|-----|------|
| **LLM** | Kimi/QWEN via OpenAI-compatible API | Vertex AI Gemini | Vertex AI Gemini |
| **Embedding** | DashScope text-embedding-v3 (768-dim) | Vertex AI text-embedding-004 (768-dim) | Vertex AI text-embedding-004 (768-dim) |
| **DB** | Docker PostgreSQL + pgvector | Cloud SQL + pgvector | Cloud SQL + pgvector |
| **Redis** | Docker Redis | GCP Redis | GCP Redis |
| **Salesforce** | Mock (local DB) | SF Sandbox | SF Production |
| **Gumtree APIs** | Mock (fixture files) | Staging APIs | Production APIs |
| **Chat transport** | Direct HTTP API + React UI | Salesforce Enhanced Chat webhook | Salesforce Enhanced Chat webhook |
| **Events** | Local PostgreSQL | Kafka + SF sync | Kafka + SF sync |
| **Deployment** | `mvn spring-boot:run` | GKE dev (Helm) | GKE prod (Helm) |
| **Feature flags** | Always on | GrowthBook (dev) | GrowthBook (prod rollout) |

**Migration steps** (Local → Dev):
1. Switch `SPRING_PROFILES_ACTIVE=dev`
2. Re-run knowledge ingestion with Vertex AI embedding client
3. Replace `.env.local` with `.env.dev` (Cloud SQL, Vertex AI, SF Sandbox credentials)
4. Deploy to GKE dev via Helm
5. Configure Salesforce Connected App + webhook pointing to GKE service
6. Replace `ChatController` endpoints with `InboundController` (Salesforce webhook)

**Migration steps** (Dev → Prod):
1. Switch to `.env.prod`
2. Confirm gumshield API service account approved
3. Bot_Session__c / Bot_Event__c objects created by SF Admin
4. GrowthBook flag `cs_bot_enabled` created
5. Helm deploy to prod GKE
6. Begin 10% rollout via GrowthBook

---

## D9. External Dependencies (Demo)

| Dependency | Required for Demo? | How Provided |
|-----------|-------------------|--------------|
| Docker Desktop | Yes | User installs |
| PostgreSQL 16 + pgvector | Yes | Docker image `pgvector/pgvector:pg16` |
| Redis 7 | Yes | Docker image `redis:7-alpine` |
| Kimi API key | Yes (LLM) | Already in `.env.local` |
| DashScope API key | Yes (embedding + backup LLM) | Already in `.env.local` |
| GetStream API key | Optional (chat transport) | Already in `.env.local`; can use direct HTTP instead |
| Internet connection | Yes | For LLM/embedding API calls |
| ~2GB disk | Yes | Docker images + pgvector data + Node modules |
| ~4GB RAM | Recommended | PostgreSQL + Spring Boot + Node dev server |
