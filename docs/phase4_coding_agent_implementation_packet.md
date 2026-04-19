# Phase 4 — Coding Agent Implementation Packet

> **Purpose**: Break the Phase 3 Detailed Technical Design into implementable modules with delivery order, contracts, tests, and done criteria. This packet is the hand-off to developers or a coding agent.
>
> **Status**: Plan only — implementation starts after user confirmation.
>
> **Inputs**: `phase3_detailed_technical_design.md` (runtime / state / control / tools / knowledge / handover / guardrails / observability / NFR), `phase5_evaluation_design.md` (eval harness / graders / suites / gates), `customer_service_tool_spec_v0_2.yaml`, eval datasets (601 sessions / 11,288 turns)

---

## 4.1 Scope Freeze

### This implementation covers

- **csagent-service**: Java 17 + Spring Boot 3.2.x microservice deployed on GKE
- **Knowledge pipeline**: offline ingestion (218 articles → chunk → embed → pgvector) + online retrieval (ANN → rerank → faq_miss)
- **Salesforce integration**: inbound webhook, Case read, Omni-Channel transfer, Bot_Session__c / Bot_Event__c write
- **Core runtime**: control kernel (INIT → DISCOVER → RESOLVE → CONFIRM → CLOSE / ESCALATE), context projection, LLM invocation (Vertex AI Gemini)
- **Tool layer**: 5 agent-visible + 4 runtime-only tools with policy enforcement
- **Guardrails**: fixed_script_library (14 categories / 50+ templates), forbidden phrase detection, PII redaction
- **Observability**: structured events, per-turn traces, Kafka publishing, Salesforce sync
- **Eval harness**: dataset loader, session simulator, code + model graders, CI gate integration
- **E2E wiring**: GrowthBook feature flag, full request pipeline, Helm chart

### This implementation explicitly does NOT cover

- Voice bot / social channels (WhatsApp, Messenger)
- Multi-agent orchestration or procedure-level workflow
- Payments / refunds / identity verification execution
- `moderation_enforcement_action` or `send_followup_email_or_async_update` (human-only tools)
- Salesforce Admin object creation (Bot_Session__c / Bot_Event__c — done by SF Admin separately)
- Knowledge readiness workflow, decision path replay UI (V1.1)
- Full configuration versioning platform (V2)
- Mobile app channel integration

---

## 4.2 Module Breakdown

### M1: Service Foundation

| Aspect | Detail |
|--------|--------|
| **What** | Spring Boot 3.2.x project skeleton, PostgreSQL schema (all tables from §3.2), Redis cache config, health endpoints, Docker + Helm + Jenkinsfile, GCP Secret Manager integration |
| **Output** | Buildable, deployable empty service with DB migrations applied, health checks passing |
| **Key files** | `pom.xml`, `Dockerfile`, `Jenkinsfile`, `/helm-chart/*`, `/src/main/resources/db/migration/*.sql`, `application.yml` |
| **Dependencies** | None (first module) |
| **Estimated scope** | ~15 files |

**DB migrations** (Flyway or Liquibase):
- `V1__create_bot_sessions.sql` — `bot_sessions` table (§3.2.2)
- `V2__create_bot_turns.sql` — `bot_turns` table (§3.2.3)
- `V3__create_session_outcomes.sql` — `session_outcomes` table (§3.2.4)
- `V4__create_kb_articles.sql` — `kb_articles` table (§3.5.1)
- `V5__create_kb_chunks.sql` — `kb_chunks` table + HNSW index (§3.5.1)

### M2: Knowledge Pipeline

| Aspect | Detail |
|--------|--------|
| **What** | Offline: article ingestion script (JSON → clean → chunk → embed via Vertex AI text-embedding-004 → bulk insert pgvector). Online: `/v1/faq/search` endpoint with ANN Top-20 → metadata filter → dedup → rerank → faq_miss two-stage gate |
| **Output** | 218 articles indexed with ~500 chunks; search endpoint returning top-3 results with `faq_miss` flag |
| **Key files** | `KnowledgeIngestionService`, `KnowledgeSearchService`, `EmbeddingClient` (Vertex AI), `ChunkingService`, `RerankService` |
| **Dependencies** | M1 (DB schema + Vertex AI credentials) |
| **Data inputs** | `data/knowledge/knowledge_base_articles.json`, `data/knowledge/article_uc_mapping.csv` |

**Retrieval contract** (from §3.5.3):
```
Query → Embed(768-dim) → ANN(ef_search=100, LIMIT 20, uc_tags filter)
  → Retrieval Gate (top1 sim < 0.3 → miss)
  → Article dedup → Rerank top 4-8
  → Answer Gate (grounding_score < 3.5 → miss)
  → Return top 3
```

### M3: Salesforce Integration

| Aspect | Detail |
|--------|--------|
| **What** | Inbound webhook (`POST /v1/chat/inbound`), Case read (pre-chat form data), Omni-Channel Transfer API, Bot_Session__c / Bot_Event__c / Chat_Message_Log__c writes, `is_business_hours` check (New Chat queue agent presence) |
| **Output** | Bot can receive messages from SF, read Case data, transfer to human queue, write session/event objects |
| **Key files** | `InboundController`, `SalesforceClient`, `SalesforceSessionSync`, `OmniChannelTransferService`, `BusinessHoursService` |
| **Dependencies** | M1 (service skeleton) |
| **External** | Salesforce Connected App credentials, OAuth token management |

**Inbound API contract** (from §3.11.1):
```yaml
POST /v1/chat/inbound
Request: { session_id, case_id, message_text, message_type, form_data?, timestamp }
Response: { reply_text, intent, should_end_chat, additional_data }
```

### M4: Core Runtime

| Aspect | Detail |
|--------|--------|
| **What** | Session lifecycle manager, form_context_ingestion, Control Kernel (state machine with 6 phases + transitions from §3.3), budget enforcement, drift detection, context projection (§3.2.6), LLM invocation (Vertex AI Gemini structured output) |
| **Output** | Given a user message + session state → produces action decision + bot response |
| **Key files** | `SessionManager`, `FormContextIngestionService`, `ControlKernel` (phase evaluator + budget checker + drift detector), `ContextProjectionBuilder`, `LlmInvocationService`, `ActionParser` |
| **Dependencies** | M1 (state store), M3 (session init from SF) |

**Control kernel phases**: INIT → DISCOVER → RESOLVE → CONFIRM → CLOSE / ESCALATE

**Budgets** (from §3.3.4):
- max_clarification_rounds = 2
- max_faq_miss = 2
- max_bot_turns_per_issue = 15 (FAQ) / 10 (Intake)
- max_repeated_same_action = 2
- max_total_bot_turns = 25

### M5: Tool Layer

| Aspect | Detail |
|--------|--------|
| **What** | Tool Dispatcher with pre-dispatch Policy Enforcer; implementations for all 9 V1 tools (5 agent-visible + 4 runtime-only) |
| **Output** | Any tool callable by name; scope violations rejected with `scope_blocked`; runtime-only tools triggered by policy, not LLM |
| **Key files** | `ToolDispatcher`, `ToolPolicyEnforcer`, `SearchKnowledgeTool`, `ResolveArticleTool`, `GetCustomerContextTool` (composite), `RequestHandoverTool`, `RecordOutcomeTool`, `CreateCaseControlledTool` (runtime-only), `LookupCustomerAccountService`, `LookupListingOrAdService`, `GetModerationReviewContextService` |
| **Dependencies** | M2 (knowledge search), M3 (Salesforce APIs), M4 (control kernel for policy) |

**Per-UC tool matrix enforcement** (from §2.10):
- `search_knowledge` / `resolve_article`: only UC-A/B/C/D/E/F/FP
- `get_customer_context`: only UC-A/C/D/F/FP/K
- `create_case_controlled`: only UC-H/J/K (runtime-only)
- `request_handover` / `record_outcome`: all UCs

**Internal API chains** (from tool_spec v0.2):
- Account chain: bapi-server → user-service → gumshield-api (6 endpoints)
- Listing chain: bapi-server → advert-service → gumshield-api → livead-search (7 endpoints)
- Moderation review: gumshield-api cs-review (mock in dev, real in prod)

### M6: Guardrails & UX

| Aspect | Detail |
|--------|--------|
| **What** | Fixed script library (YAML template store + Mustache interpolation), forbidden phrase detector (regex patterns from §3.7.3), PII redaction filter (4 layers from §3.7.4), progress placeholder (1.5s timeout async trigger) |
| **Output** | Runtime can select + render templates; every bot response passes guardrail check before sending; PII stripped from logs/traces |
| **Key files** | `ScriptLibraryService`, `ForbiddenPhraseDetector`, `PiiRedactionFilter`, `ProgressPlaceholderService`, `/src/main/resources/scripts/*.yaml` (14 category files) |
| **Dependencies** | M4 (control kernel invokes guardrails) |
| **Data inputs** | `fixed_script_library_v1.md` → converted to YAML template files |

**Template categories** (14): opening, empathy, hold_placeholder, identifier_request, resolution_check, escalation_business_hours, escalation_off_hours, gdpr_intake, appeal_intake, dispute_disclaimer, safety_intake, tech_troubleshoot, policy_explanation, idle_close

### M7: Observability

| Aspect | Detail |
|--------|--------|
| **What** | Event emitter (11 event types from §3.8.1), per-turn trace writer (§3.8.2), Kafka Avro publisher, Salesforce Bot_Event__c sync, Micrometer metrics, Logback PII-safe appender |
| **Output** | Every significant action emits structured event; traces queryable; Kafka topic populated; Prometheus metrics exported |
| **Key files** | `EventEmitter`, `TraceWriter`, `KafkaEventPublisher`, `SalesforceEventSync`, `MetricsService`, `PiiSafeLogFilter` |
| **Dependencies** | M1 (DB + Kafka config), M3 (SF sync) |

**Events**: SESSION_STARTED, USE_CASE_INFERRED, RETRIEVAL_EXECUTED, ARTICLE_SHOWN, CLARIFICATION_ASKED, ESCALATION_REQUESTED, CASE_CREATED, OUTCOME_RECORDED, SESSION_CLOSED, TOOL_SCOPE_BLOCKED, GUARDRAIL_VIOLATION

### M8: Eval Harness

| Aspect | Detail |
|--------|--------|
| **What** | Dataset loader (CSV → session objects), session simulator (replay turns through bot runtime), code-based graders (12 types from §5.1), model-based graders (5 types from §5.2), metrics aggregator (§6), gate evaluator (§7), CI report generator, suite configs (smoke / full / nightly) |
| **Output** | `./eval/` directory; `mvn verify -Peval-smoke` runs smoke suite in CI; full regression runnable as standalone |
| **Key files** | `eval/harness/`, `eval/graders/code/`, `eval/graders/model/`, `eval/metrics/`, `eval/suites/*.yaml` |
| **Dependencies** | M4+M5+M6 (bot runtime to test against), eval datasets |
| **Data inputs** | `csagent/data/eval_datasets/` (7 CSV pairs, 601 sessions) |
| **Language** | Python (eval harness) or Java (if team prefers same stack); Python recommended for grader flexibility |

**CI integration**: Jenkins pipeline adds eval stage after build + unit tests; PR → smoke (75 sessions, ≤5min); RC → full (601 sessions, ≤30min).

### M9: E2E Integration & Hardening

| Aspect | Detail |
|--------|--------|
| **What** | GrowthBook feature flag wiring, full request pipeline integration test, Salesforce sandbox E2E, staging environment deployment, load testing (target QPS), graceful degradation verification |
| **Output** | Complete working system in staging; GrowthBook flag controls traffic routing; degradation to escalation on LLM/tool failure verified |
| **Key files** | `GrowthBookFeatureFlagService`, integration test suite, Helm values per env |
| **Dependencies** | All modules (M1–M8) |

---

## 4.3 Delivery Order

```text
Phase  1 (Foundation)           Phase 2 (Core Capabilities)          Phase 3 (Intelligence)
─────────────────────          ───────────────────────────           ─────────────────────

  ┌─────┐                       ┌─────┐      ┌─────┐                 ┌─────┐
  │ M1  │──────────────────────▶│ M4  │─────▶│ M5  │────────────────▶│ M8  │
  │Found│                       │Core │      │Tools│                  │Eval │
  └──┬──┘                       │Runtm│      └──┬──┘                  └──┬──┘
     │                          └──┬──┘         │                        │
     │    ┌─────┐                  │          ┌─▼───┐                 ┌──▼──┐
     ├───▶│ M2  │──────────────────┘          │ M6  │────────────────▶│ M9  │
     │    │Knowl│                              │Guard│                 │E2E  │
     │    └─────┘                              └─────┘                └─────┘
     │    ┌─────┐                  ┌─────┐
     └───▶│ M3  │──────────────────│ M7  │ (parallel with M5/M6)
          │SF   │                  │Obsrv│
          └─────┘                  └─────┘
```

### Recommended Delivery Sequence

| Order | Module | Parallel? | Prerequisite | Deliverable |
|-------|--------|-----------|-------------|-------------|
| **1** | **M1: Service Foundation** | — | None | Deployable empty service + DB schema + CI pipeline |
| **2a** | **M2: Knowledge Pipeline** | Yes (with M3) | M1 | 218 articles indexed; `/v1/faq/search` working |
| **2b** | **M3: Salesforce Integration** | Yes (with M2) | M1 | Inbound webhook + Case read + transfer API |
| **3** | **M4: Core Runtime** | — | M1, M2 stub, M3 stub | State machine + context projection + LLM invocation |
| **4a** | **M5: Tool Layer** | Yes (with M6, M7) | M2, M3, M4 | All 9 tools dispatchable with policy enforcement |
| **4b** | **M6: Guardrails & UX** | Yes (with M5, M7) | M4 | Script library + forbidden phrase + PII + placeholder |
| **4c** | **M7: Observability** | Yes (with M5, M6) | M1, M3 | Events + traces + Kafka + metrics |
| **5** | **M8: Eval Harness** | — | M4, M5, M6 | Smoke suite passing; full regression runnable |
| **6** | **M9: E2E Integration** | — | All (M1–M8) | Staging deployment; GrowthBook wired; load tested |

**Critical path**: M1 → M4 → M5 → M8 → M9

**Calendar estimate** (not a commitment, for planning only):
- Phase 1 (M1): ~3 days
- Phase 2 (M2+M3 parallel): ~5 days
- Phase 3 (M4): ~5 days
- Phase 4 (M5+M6+M7 parallel): ~7 days
- Phase 5 (M8): ~5 days
- Phase 6 (M9): ~3 days
- Buffer + integration: ~3 days
- **Total**: ~4–5 weeks for one developer

---

## 4.4 Required Contracts

### 4.4.1 DB Schema

| File | Tables | Source |
|------|--------|--------|
| `V1__create_bot_sessions.sql` | `bot_sessions` | Phase 3 §3.2.2 |
| `V2__create_bot_turns.sql` | `bot_turns` | Phase 3 §3.2.3 |
| `V3__create_session_outcomes.sql` | `session_outcomes` | Phase 3 §3.2.4 |
| `V4__create_kb_articles.sql` | `kb_articles` | Phase 3 §3.5.1 |
| `V5__create_kb_chunks.sql` | `kb_chunks` + HNSW index | Phase 3 §3.5.1 |

### 4.4.2 API Contracts (OpenAPI)

| Endpoint | Method | Source |
|----------|--------|--------|
| `/v1/chat/inbound` | POST | Phase 3 §3.11.1 |
| `/v1/faq/search` | POST (internal) | Phase 3 §3.11.2 |
| `/internal/health` | GET | Phase 3 §3.11.3 |
| `/internal/health/liveness` | GET | Phase 3 §3.11.3 |
| `/internal/health/readiness` | GET | Phase 3 §3.11.3 |

### 4.4.3 Tool Schemas

Source: `customer_service_tool_spec_v0_2.yaml` — all 11 tools with input/output JSON Schema.

### 4.4.4 Trace / Event Schemas

| Schema | Format | Source |
|--------|--------|--------|
| Bot_Event__c payload | JSON (Salesforce LongText) | Phase 3 §3.8.1 |
| Per-turn trace | JSON | Phase 3 §3.8.2 |
| Kafka analytics event | Avro | Phase 3 §3.8.3 (to be defined as `.avsc`) |

### 4.4.5 Config Schemas

| Config | Format | What It Controls |
|--------|--------|-----------------|
| `use-case-registry.yaml` | YAML | 12 UC definitions (from Phase 2 §2.2) |
| `control-policy.yaml` | YAML | Budgets, phase transitions, drift thresholds |
| `tool-policy.yaml` | YAML | Per-UC allowed/disallowed tool matrix |
| `escalation-triggers.yaml` | YAML | 17 escalation trigger conditions + reason codes |
| `scripts/*.yaml` | YAML | 14 template categories (from `fixed_script_library_v1.md`) |
| `application.yml` | YAML | Spring Boot config (DB, Redis, Vertex AI, Kafka, Salesforce) |

---

## 4.5 Required Tests

### Per-Module Test Requirements

| Module | Unit Tests | Integration Tests | Contract Tests |
|--------|-----------|------------------|---------------|
| **M1** | Health endpoint, config loading, DB migration | Cloud SQL connectivity | — |
| **M2** | Chunking logic, embedding mock, ANN query, rerank, faq_miss gate | Vertex AI embedding (staging), pgvector search E2E | — |
| **M3** | SF payload parsing, OAuth token refresh, transfer request builder | SF sandbox: create session, read case, write event | Pact: inbound webhook contract |
| **M4** | Phase transitions (all 12 from §3.3.2), budget enforcement (each budget), drift detection (3 types), context projection (field presence + PII exclusion) | Full turn cycle with mocked tools | — |
| **M5** | Per-tool input validation, output schema, UC scope matrix (all 60 cells from §2.10.1–2), tool chain composition (get_customer_context → lookup_*) | Internal API calls (mock or staging) | Pact: internal API contracts |
| **M6** | Template rendering (all 50+ templates), variable interpolation, forbidden phrase detection (all 9 categories), PII regex (email/phone/name), placeholder timing | — | — |
| **M7** | Event emission (11 types), trace schema validation, Kafka serialization, PII filter in logs | Kafka topic write, SF event sync | Avro schema compatibility |
| **M8** | Dataset loading (all 7), grader logic (12 code graders), metrics aggregation, gate evaluation | Full smoke suite (75 sessions) | — |
| **M9** | GrowthBook flag evaluation, degradation path | Staging full pipeline, load test | — |

### Eval Suite Integration (from Phase 5)

| Level | When | Suites | Gate |
|-------|------|--------|------|
| **Smoke** | Every PR | Core Routing (30) + Grounding Safety (20) + Handover Schema (15) + Tool Scope (10) = 75 sessions | All Hard Gates on subset |
| **Full Regression** | Release Candidate | All 6 suites = 601 sessions | All Hard + Soft Gates |
| **Nightly** | Cron 1am UTC | Full + extended drift | Alert only |

### Critical Test Scenarios (must exist before launch)

| # | Scenario | Module | Type |
|---|----------|--------|------|
| 1 | FAQ UC resolved with grounded answer + article link | M4+M5 | E2E |
| 2 | Intake UC (UC-H) → create_case → handover with complete payload | M4+M5 | E2E |
| 3 | User says "talk to agent" → immediate escalation from any phase | M4 | Unit |
| 4 | faq_miss = 2 → automatic escalation | M4 | Unit |
| 5 | Clarification count = 2 → budget exhausted → escalation | M4 | Unit |
| 6 | Total turns = 25 → forced escalation | M4 | Unit |
| 7 | Tool scope violation (search_knowledge on UC-H) → scope_blocked | M5 | Unit |
| 8 | Forbidden phrase in LLM output → blocked + substituted | M6 | Unit |
| 9 | PII in projected context → redacted before LLM call | M4+M6 | Unit |
| 10 | Off-hours → handover to CS_Cases_New with offline message | M3+M5 | Integration |
| 11 | LLM timeout → degradation to escalation | M4 | Unit |
| 12 | Handover payload validates against JSON schema | M5 | Unit |
| 13 | Soft shift → original UC preserved in candidate_use_cases | M4 | Unit |
| 14 | Pre-chat form email → auto-trigger get_customer_context | M4+M5 | Integration |
| 15 | GrowthBook flag off → user routed to human (no bot) | M9 | Integration |

---

## 4.6 Done Criteria

### Implementation Complete When

- [ ] All 9 modules (M1–M9) implemented and passing unit + integration tests
- [ ] DB migrations applied cleanly on fresh database
- [ ] 218 articles indexed in pgvector with HNSW index
- [ ] Eval smoke suite (75 sessions) passes all Hard Gates in CI
- [ ] Eval full regression (601 sessions) passes all Hard + Soft Gates
- [ ] All 15 critical test scenarios (§4.5) green
- [ ] Helm chart deploys to GKE staging with health checks passing
- [ ] Salesforce sandbox: inbound → bot response → transfer to human verified
- [ ] GrowthBook flag toggles bot on/off correctly
- [ ] Prompt, projection, control policy, tool schema, eval suite versions recorded in traces
- [ ] Forbidden phrase detector catches all 9 categories with 0 false negatives on test corpus
- [ ] PII redaction verified on projected_context, bot_turns, Bot_Event__c, Kafka events
- [ ] `gumshield` mock data used in dev; prod switch-over documented

### Release Blocked When

- [ ] `critical_policy_violation > 0` on any eval run
- [ ] `wrong_containment > 2%` on full regression
- [ ] `groundedness_pass_rate < 98%`
- [ ] `escalation_recall < 95%` on required-escalation cases
- [ ] `handover_completeness < 98%`
- [ ] Any `tool_scope_violation` detected
- [ ] Any `forbidden_phrase` in bot output
- [ ] Budget enforcement < 100% (any budget exceeded without escalation)
- [ ] Phase transition validity < 100% (illegal FSM transition)
- [ ] Salesforce Bot_Session__c / Bot_Event__c objects not created by Admin
- [ ] gumshield prod service account not approved (blocks UC-FP moderation context)

---

## 4.7 Repo Structure

```
csagent/
├── server/
│   ├── src/main/java/com/gumtree/csagent/
│   │   ├── controller/          # InboundController, HealthController
│   │   ├── service/
│   │   │   ├── runtime/         # SessionManager, ControlKernel, ContextProjectionBuilder
│   │   │   ├── knowledge/       # KnowledgeSearchService, KnowledgeIngestionService
│   │   │   ├── tools/           # ToolDispatcher, ToolPolicyEnforcer, *Tool impls
│   │   │   ├── salesforce/      # SalesforceClient, OmniChannelTransferService
│   │   │   ├── llm/             # LlmInvocationService, ActionParser
│   │   │   ├── guardrails/      # ScriptLibraryService, ForbiddenPhraseDetector, PiiRedactionFilter
│   │   │   └── observability/   # EventEmitter, TraceWriter, KafkaEventPublisher
│   │   ├── model/               # Domain objects (Session, Turn, UseCase, HandoverPayload, etc.)
│   │   └── config/              # Spring config classes
│   ├── src/main/resources/
│   │   ├── db/migration/        # Flyway SQL migrations
│   │   ├── prompts/             # LLM prompt templates (versioned)
│   │   ├── scripts/             # Fixed script library YAML (14 categories)
│   │   ├── config/              # use-case-registry.yaml, control-policy.yaml, tool-policy.yaml
│   │   └── application.yml
│   ├── src/test/java/           # Unit + integration tests
│   └── src/test/resources/
│       └── mock/                # gumshield mock responses, SF mock responses
├── eval/
│   ├── harness/                 # Eval runner, dataset loader, session simulator
│   ├── graders/
│   │   ├── code/                # 12 code-based graders
│   │   └── model/               # 5 model-based graders
│   ├── metrics/                 # Aggregator, gate evaluator
│   ├── suites/                  # smoke.yaml, full_regression.yaml, nightly.yaml
│   └── reports/                 # CI report, dashboard export
├── data/
│   ├── eval_datasets/           # 7 CSV pairs (symlink to csagent repo)
│   └── knowledge/               # knowledge_base_articles.json, article_uc_mapping.csv
├── helm-chart/                  # Helm chart for GKE deployment
├── contract/                    # OpenAPI specs, Avro schemas, Pact contracts
├── Dockerfile
├── Jenkinsfile
├── pom.xml
└── README.md
```

---

## 4.8 External Dependencies Checklist

| Dependency | Owner | Status | Blocking Module |
|-----------|-------|--------|----------------|
| Cloud SQL PostgreSQL instance | Platform/Infra | Available (existing infra) | M1 |
| pgvector extension enabled on Cloud SQL | Platform/Infra | Needs provisioning | M2 |
| Vertex AI API access (Embedding + Gemini) | Platform/Infra | Needs project enablement | M2, M4 |
| Salesforce Connected App (OAuth) | Salesforce Admin | Needs creation | M3 |
| Bot_Session__c / Bot_Event__c objects | Salesforce Admin | v5 confirmed; needs creation | M3, M7 |
| GrowthBook feature flag (`cs_bot_enabled`) | Frontend/Growth | Needs creation | M9 |
| Kafka topic + Avro schema registration | Platform/Infra | Needs provisioning | M7 |
| gumshield API service account (dev) | Security/Gumshield | Mock for dev; approval in progress | M5 |
| gumshield API service account (prod) | Security/Gumshield | Approval in progress | M5 (prod only) |
| Jenkins pipeline library access | Platform/DevOps | Available | M1 |
| GCP Secret Manager entries | Platform/Infra | Needs provisioning | M1 |
