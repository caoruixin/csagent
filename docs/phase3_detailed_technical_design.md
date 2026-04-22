# Phase 3 — Detailed Technical Design

> 将 Phase 0–2 固定的业务契约转化为实现层详细技术设计，覆盖 Runtime / State Model / Control Kernel / Tools / Knowledge / Handover / Guardrails / Observability / NFR。
>
> **前置输入（全部已固定）**:
> - `phase0_normative_freeze.md`（V1 规范冻结层 — 不可漂移的内核约束）
> - `phase1_solution_input_pack.md`（业务 / 领域 / 运营 / 工程 / Eval 完整输入）
> - `phase2_domain_realization_spec.md`（12 UC + 3 OUT_OF_SCOPE registry / risk matrix / escalation matrix / knowledge scope / control policy / tool allocation / guardrails）
> - `data/human_review_annotations_2026-04-22_complete.csv`（**v8 新增** — 367 条 human review 标注，校准 UC 分类 / 路由 / drift / escalation 设计）
> - `customer_service_tool_spec_v0_2.yaml`（V1 Tool Spec v0.2）
> - `salesforce-part-spec.md` / `problem_retrieval_solution_plan_pgvector.md` / `07-engineering-constraints.md` / `platform_api_detailed_reference.md`
> - `data/knowledge/knowledge_base_articles.json` + `article_uc_mapping.csv`（218 篇文章 + UC 映射）
> - `fixed_script_library_v1.md`（14 类 / 50+ 合规审批话术模板）
> - `abtesing-policy.md`（GrowthBook 流量策略）
>
> **下游输出**: `phase4_coding_agent_implementation_packet.md`

---

## 3.1 Runtime Architecture

### 3.1.1 System Context

```text
                         ┌─────────────────┐
                         │   End User      │
                         │  (Web Browser)  │
                         └────────┬────────┘
                                  │ Enhanced Chat Widget
                                  ▼
                    ┌─────────────────────────┐
                    │  Salesforce Platform     │
                    │  ┌───────────────────┐   │
                    │  │ Enhanced Chat +   │   │
                    │  │ Messaging Session │   │
                    │  └────────┬──────────┘   │
                    │           │ Webhook /     │
                    │           │ REST callback │
                    └───────────┼──────────────┘
                                │
            ┌───────────────────▼────────────────────┐
            │         GKE  (europe-west4)            │
            │  ┌─────────────────────────────────┐   │
            │  │     csagent-service (Bot)        │   │
            │  │  ┌────────────────────────────┐  │   │
            │  │  │  Inbound Message Handler   │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  Form Context Ingestion    │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  Control Kernel            │  │   │
            │  │  │  (State Machine + Policy)  │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  Context Builder           │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  LLM Invocation Layer      │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  Tool Dispatcher           │  │   │
            │  │  │  + Policy Enforcer         │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  Handover Module           │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  Script Library            │  │   │
            │  │  ├────────────────────────────┤  │   │
            │  │  │  Trace / Metrics Hooks     │  │   │
            │  │  └────────────────────────────┘  │   │
            │  └─────────────────────────────────┘   │
            │                                        │
            │  ┌──────────────┐  ┌───────────────┐   │
            │  │ Cloud SQL    │  │ Redis Cache   │   │
            │  │ (PostgreSQL  │  │               │   │
            │  │  + pgvector) │  │               │   │
            │  └──────────────┘  └───────────────┘   │
            └────────────────────────────────────────┘
                    │                    │
       ┌────────────┘                    └──────────┐
       ▼                                            ▼
┌──────────────┐  ┌──────────────┐  ┌───────────────────┐
│ Vertex AI    │  │ Gumtree      │  │ GrowthBook        │
│ Gemini /     │  │ Internal APIs│  │ (Feature Flags)   │
│ Embedding    │  │ (bapi /      │  │                   │
│              │  │  gumshield / │  │                   │
│              │  │  user-svc)   │  │                   │
└──────────────┘  └──────────────┘  └───────────────────┘
       │
       ▼
┌──────────────┐
│ Kafka        │
│ (Avro events)│
└──────────────┘
```

### 3.1.2 Major Components

| Component | Responsibility | Technology |
|-----------|---------------|------------|
| **Inbound Message Handler** | 接收 Salesforce Enhanced Chat webhook；解析消息 payload；session 生命周期管理 | Spring Boot REST Controller |
| **Form Context Ingestion** | INIT 阶段解析 pre-chat form data → session state；auto-trigger `get_customer_context` | Runtime capability；无 LLM 调用 |
| **Control Kernel** | 状态机推进（INIT→DISCOVER→RESOLVE→CONFIRM→CLOSE/ESCALATE）；budget 管理；drift detection | Stateless per-turn evaluation against session state |
| **Context Builder** | 每轮构造最小充分 projected context → LLM prompt | Template engine + state reader |
| **LLM Invocation Layer** | 调用 Vertex AI Gemini；解析结构化输出（action + parameters）| Vertex AI Java SDK |
| **Tool Dispatcher** | 路由 tool call 到具体实现；schema validation；policy enforcement；5 agent-visible + 5 runtime-only tools (v0.2.1) | Spring Bean routing + JSON Schema validator |
| **Tool Policy Enforcer** | 每次 tool call 前检查 `active_use_case ∈ allowed_use_cases`；violation → `scope_blocked` | Pre-dispatch interceptor |
| **Handover Module** | 构建 handover payload → Salesforce Omni-Channel Transfer → 写 Bot_Context__c | Salesforce REST API client |
| **Script Library** | 固定话术模板管理与渲染（14 类 / 50+ 模板）| Template store + variable interpolation |
| **Progress Placeholder** | tool call >1.5s 时发送占位消息 | Async timeout trigger |
| **Trace / Metrics Hooks** | 结构化事件记录 → Bot_Event__c + Kafka + local PostgreSQL | Event publisher pipeline |
| **Knowledge Retrieval** | pgvector 语义检索 + rerank + grounding score | SQL query + Vertex AI Embedding |
| **State Store** | Bot session state 持久化 + Bot_Session__c sync | PostgreSQL (local) + Salesforce API (sync) |

### 3.1.3 GrowthBook Traffic Integration

```text
User arrives at Help Centre
  → Enhanced Chat widget loads
  → GrowthBook SDK evaluates feature flag "cs_bot_enabled"
     using gbUserPseudoId (Cookie gt_gb_exp_uid, UUID v4, 365d)
  → Variant A (control): direct to human queue
  → Variant B (treatment): route to csagent-service
```

| Aspect | Detail |
|--------|--------|
| Flag name | `cs_bot_enabled` (boolean feature) |
| Rollout | Dev: 100% → Prod: 10% → 20% → 50% → 100% |
| Hash attribute | `gbUserPseudoId` |
| Tracking | GA4 `experiment_viewed` event with `experimentId` + `variationId` |
| Bot-side | `TrafficVariant__c` on `Bot_Session__c` records the variant for analysis |
| Gate check | Each stage expansion requires release gate pass (containment / CSAT / wrong_containment) |

### 3.1.4 Request Flow (Single Turn)

```text
1. Salesforce → POST /v1/chat/inbound (message + session metadata)
2. Inbound Handler:
   a. Load/create session state from PostgreSQL
   b. If first message: Form Context Ingestion → auto-trigger get_customer_context
3. Control Kernel:
   a. Read current phase (INIT/DISCOVER/RESOLVE/CONFIRM)
   b. Check budgets (turns, clarification, faq_miss)
   c. Check escalation triggers
   d. If budget exceeded or trigger hit → ESCALATE
4. Context Builder:
   a. Project minimal context from session state + latest message + retrieved knowledge
   b. Inject allowed_actions and tool_schemas for current UC
5. LLM Invocation:
   a. Send projected context → Vertex AI Gemini
   b. Parse structured response: { action, parameters, user_message }
6. Tool Dispatcher:
   a. If action requires tool call → Policy Enforcer check → execute tool
   b. If tool latency >1.5s → send progress_placeholder
   c. Return tool result to context for next LLM call (if needed)
7. Response Assembly:
   a. If action = answer_grounded → merge LLM output + article links
   b. If action = escalate_human → Handover Module
   c. If action uses fixed_script_library → render template with variables
8. Outbound:
   a. Write reply to Salesforce Messaging Session
   b. Update session state (phase, counters, articles_shown, etc.)
   c. Emit trace event → Bot_Event__c + Kafka + local DB
9. Return HTTP 200 to Salesforce
```

### 3.1.5 Deployment Architecture

| Dimension | Specification |
|-----------|--------------|
| Runtime | Java 17 + Spring Boot 3.2.x |
| Build | Maven |
| Container | Docker → GCP Artifact Registry (`europe-west4-docker.pkg.dev/gum-host-7491/docker-artifact-repo`) |
| Orchestration | GKE (europe-west4, failover europe-west3) |
| Service mesh | Istio (mTLS, VirtualService routing) |
| IaC | Helm chart (`/helm-chart`) |
| Scaling | HPA; target: CPU 70%, memory 80% |
| Health | `/internal/health/liveness`, `/internal/health/readiness` |
| Secrets | GCP Secret Manager |

---

## 3.2 State Model

### 3.2.1 External State — Salesforce Objects

#### Bot_Session__c (new, v5 confirmed)

| Field | Type | Description |
|-------|------|-------------|
| `Name` | Auto Number | Session record name |
| `MessagingSession__c` | Lookup(MessagingSession) | Link to Salesforce chat session |
| `Case__c` | Lookup(Case) | Link to pre-chat-form-created Case |
| `TrafficVariant__c` | Picklist | GrowthBook variant: `control` / `bot_v1` |
| `HandlingState__c` | Picklist | `BOT_HANDLING` / `QUEUE_TO_HUMAN` / `HUMAN_HANDLING` / `CLOSED` |
| `PrimaryIntent__c` | Text(50) | e.g. `UC-FP-01` or `OUT_OF_SCOPE_RATINGS_REVIEWS` (v8: includes OOS categories) |
| `CandidateIntents__c` | Text(255) | JSON array of candidate UC IDs |
| `IntentConfidence__c` | Number(4,2) | 0.00–1.00 |
| `ContainmentOutcome__c` | Picklist | `RESOLVED` / `ESCALATED` / `ABANDONED` / `WRONG_CONTAINMENT` |
| `EscalationReason__c` | Text(100) | e.g. `user_requested`, `faq_miss_threshold_exceeded` |
| `TotalBotTurns__c` | Number | Counter |
| `ClarificationCount__c` | Number | Counter |
| `FaqMissCount__c` | Number | Counter |
| `ArticlesShown__c` | LongText(4000) | JSON array of article IDs shown |
| `PromptVersion__c` | Text(20) | e.g. `v1.0.3` |
| `ModelVersion__c` | Text(50) | e.g. `gemini-2.0-flash` |
| `CreatedDate` | DateTime | Auto |

#### Bot_Event__c (new, v5 confirmed)

| Field | Type | Description |
|-------|------|-------------|
| `Name` | Auto Number | Event record name |
| `Bot_Session__c` | Lookup(Bot_Session__c) | Parent session |
| `EventType__c` | Picklist | `SESSION_STARTED` / `USE_CASE_INFERRED` / `RETRIEVAL_EXECUTED` / `ARTICLE_SHOWN` / `CLARIFICATION_ASKED` / `ESCALATION_REQUESTED` / `CASE_CREATED` / `OUTCOME_RECORDED` / `SESSION_CLOSED` / `TOOL_SCOPE_BLOCKED` / `OUT_OF_SCOPE_HANDOVER` (v8) / `GUARDRAIL_VIOLATION` |
| `TurnIndex__c` | Number | Turn sequence in session |
| `Payload__c` | LongText(32768) | JSON (PII-redacted) |
| `CreatedDate` | DateTime | Auto |

#### Chat_Message_Log__c (existing)

Unchanged — continues to record raw bot ↔ customer messages per turn.

#### Case Fields (existing, from pre-chat form)

Used as-is. Bot reads `Topic Subject`, `Description`, `Email`, `Ad ID Number` from the associated Case as `form_context`.

### 3.2.2 Session State — Local PostgreSQL

Bot-internal session state for fast read/write during conversation. Synced to Salesforce on key events.

```sql
CREATE TABLE bot_sessions (
    session_id          TEXT PRIMARY KEY,           -- Salesforce MessagingSession Id
    sf_bot_session_id   TEXT,                       -- Bot_Session__c Id (after SF sync)
    case_id             TEXT,                       -- Pre-chat-form Case Id
    traffic_variant     TEXT NOT NULL DEFAULT 'bot_v1',
    handling_state      TEXT NOT NULL DEFAULT 'BOT_HANDLING',
    current_phase       TEXT NOT NULL DEFAULT 'INIT',
    active_use_case     TEXT,                       -- e.g. 'UC-FP-01' or 'OUT_OF_SCOPE_RATINGS_REVIEWS' (v8: includes OOS categories)
    candidate_use_cases TEXT[],                     -- e.g. {'UC-A-01','UC-FP-01'}; v8 HR: 90.2% sessions have ≥1 secondary UC
    intent_confidence   NUMERIC(4,2),
    
    -- Budgets & Counters
    total_bot_turns     INT NOT NULL DEFAULT 0,
    clarification_count INT NOT NULL DEFAULT 0,
    faq_miss_count      INT NOT NULL DEFAULT 0,
    repeated_action_count INT NOT NULL DEFAULT 0,
    last_action         TEXT,
    
    -- Context
    form_topic_subject  TEXT,                        -- Pre-chat Form Topic Subject 原始值（不可变；业务评估 L1 维度）
    form_context        JSONB,                      -- pre-chat form data (full)
    customer_context    JSONB,                       -- safe_summary from get_customer_context
    listing_context     JSONB,                       -- safe_summary from lookup_listing_or_ad
    moderation_context  JSONB,                       -- from get_moderation_review_context
    articles_shown      TEXT[] NOT NULL DEFAULT '{}',
    intake_fields       JSONB,                       -- collected intake data for UC-G/H/I/J/K
    
    -- Outcome
    containment_outcome TEXT,                        -- RESOLVED/ESCALATED/ABANDONED
    escalation_reason   TEXT,
    
    -- Versioning
    prompt_version      TEXT NOT NULL,
    model_version       TEXT NOT NULL,
    projection_version  TEXT NOT NULL,
    
    -- Timestamps
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at           TIMESTAMPTZ
);

CREATE INDEX idx_sessions_handling ON bot_sessions(handling_state) WHERE handling_state != 'CLOSED';
```

### 3.2.3 Turn Log — Local PostgreSQL

```sql
CREATE TABLE bot_turns (
    turn_id             TEXT PRIMARY KEY,            -- UUID
    session_id          TEXT NOT NULL REFERENCES bot_sessions(session_id),
    turn_index          INT NOT NULL,
    
    -- Input
    user_message        TEXT,
    
    -- LLM
    projected_context   JSONB,                       -- the context sent to LLM (PII-redacted)
    llm_raw_response    TEXT,                        -- raw LLM output (PII-redacted)
    action_selected     TEXT,                        -- ask_user / retrieve_knowledge / answer_grounded / escalate_human / finish
    action_parameters   JSONB,
    
    -- Tool
    tool_calls          JSONB,                       -- array of {tool_name, input, output, latency_ms, status}
    
    -- Output
    bot_response        TEXT,                        -- final message sent to user
    source_ids          TEXT[],                      -- article IDs used for grounding
    
    -- State snapshot
    phase_before        TEXT,
    phase_after         TEXT,
    active_use_case     TEXT,
    
    -- Timing
    latency_ms          INT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_turns_session ON bot_turns(session_id, turn_index);
```

### 3.2.4 Session Outcome — Local PostgreSQL

```sql
CREATE TABLE session_outcomes (
    session_id          TEXT PRIMARY KEY REFERENCES bot_sessions(session_id),
    outcome             TEXT NOT NULL,               -- resolved/escalated/abandoned/wrong_containment
    use_case_id         TEXT,
    escalation_reason   TEXT,
    articles_shown      TEXT[],
    total_turns         INT,
    total_latency_ms    BIGINT,
    trace_metadata      JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 3.2.5 Memory Policy

V1 memory = **极轻**。不维护跨 session 用户记忆。Session state 在同一会话内有效。

### 3.2.6 Projected Context Contract

每轮发送给 LLM 的结构化 JSON（最小充分原则）：

```json
{
  "task_summary": "User asks why their ad was removed. UC-FP-01 detected with high confidence.",
  "active_use_case": "UC-FP-01",
  "candidate_use_cases": ["UC-A-01"],
  "form_context": {
    "first_name": "Hill",
    "email": "[REDACTED]",
    "topic_subject": "Ad Support",
    "ad_id": "1487477877",
    "description": "my ad was removed for no reason"
  },
  "customer_context": {
    "safe_summary": "Account active. 1 ad currently under moderation review.",
    "moderation_review_reason": "multiple_accounts"
  },
  "recent_messages": [
    {"role": "user", "content": "my ad was removed for no reason", "turn": 1},
    {"role": "bot", "content": "I'm sorry to hear that. Let me check what happened.", "turn": 2}
  ],
  "retrieved_knowledge": [
    {
      "source_id": "ka4P200000002hlIAA",
      "title": "Posting Policies",
      "snippet": "Each user is allowed a maximum of one account...",
      "canonical_url": "https://help.gumtree.com/s/policies?cat=Posting_Policies&article=General-Posting-Policies2",
      "score": 0.87
    }
  ],
  "risk_flags": [],
  "budget_state": {
    "total_bot_turns": 3,
    "max_bot_turns": 15,
    "clarification_count": 0,
    "max_clarification": 2,
    "faq_miss_count": 0,
    "max_faq_miss": 2
  },
  "allowed_actions": ["retrieve_knowledge", "answer_grounded", "ask_user", "escalate_human", "finish"],
  "tool_schemas": ["search_knowledge", "resolve_article", "get_customer_context", "request_handover", "record_outcome"]
}
```

**Projection Rules**:

1. **Relevance first** — only inject knowledge/context related to active UC
2. **Recency with unresolved override** — last 3–5 key messages; unresolved issues always retained
3. **Structured over raw** — use `safe_summary`, not full API dump
4. **Grounding priority** — retrieved knowledge always included when available
5. **Risk-aware injection** — risk_flags injected when non-empty
6. **Schema minimization** — only tool schemas allowed for current UC

**Default exclusions from prompt**:
- Full message history (only recent 3–5 turns)
- Full tool call logs (only last result summary)
- Knowledge results not related to current UC
- Tool schemas not allowed for current UC
- Raw PII (always redacted in projected context)

---

## 3.3 Control Kernel

### 3.3.1 State Machine

```text
INIT ─────────────────────────────────────────────────────────────┐
  │                                                               │
  │ parse form_context                                            │
  │ auto-trigger get_customer_context (if UC allows)              │
  │ load budgets                                                  │
  │ set phase = DISCOVER                                          │
  ▼                                                               │
DISCOVER ──────────────────────────────────────┐                  │
  │                                             │                  │
  │ Two-stage UC routing (phase2 §2.11,          │ any escalation   │
  │   v8 HR-calibrated):                        │ trigger hit      │
  │   Stage 1: topic_subject prior              │                  │
  │     strong (>70%): direct route UC-G/J      │ ───────────┐     │
  │     handover-only (4 TS): check desc ���      │            │     │
  │       match UC �� route; else → OOS handover │            │     │
  │     weak: → Stage 2                         │            │     │
  │   Stage 2: description classification       │            │     │
  │     within topic_subject UC candidate set   │            │     │
  │     + cross-TS spillover detection          │            │     │
  │                                             │            │     │
  │ if ambiguous → ask_user (clarify)           │            │     │
  │ if OOS topic → fixed_script → ESCALATE      │            │     ��
  │ if high-risk UC detected → set risk_flags   │            │     │
  │ if intake-only UC → phase = RESOLVE(intake) │            │     │
  │ if FAQ UC → phase = RESOLVE(faq)            │            │     │
  ▼                                             ▼            │     │
RESOLVE ───────────────────────────────────────────────┐     │     │
  │                                                     │     │     │
  │ FAQ path:                                           │     │     │
  │   search_knowledge → resolve_article →              │     │     │
  │   answer_grounded → phase = CONFIRM                 │     │     │
  │                                                     │     │     │
  │ Intake path (UC-G/H/I/J/K):                        │     │     │
  │   fixed_script → ask_user (intake fields) →         │     │     │
  │   if intake_complete:                               │     │     │
  │     UC-H/J/K: create_case_controlled →              │     │     │
  │     all intake UC: → ESCALATE                       │     │     │
  │                                                     │     │     │
  │ if faq_miss → increment counter                     │     │     │
  │ if faq_miss >= 2 → ESCALATE                         │     │     │
  ▼                                                     ▼     ▼     │
CONFIRM ─────────────────────────────────────────────────────────  │
  │                                                               │
  │ "Did that answer your question?"                              │
  │                                                               │
  │ Yes → phase = CLOSE (resolved)                                │
  │ No  → one more retrieval attempt OR → ESCALATE               │
  │ New issue → soft shift (DISCOVER)                             │
  │ Appeal/emotion → hard shift (ESCALATE)                        │
  ▼                                                               │
CLOSE ◄───────────────────────────────────────────────────────────┘
  │
  │ record_outcome
  │ sync to Salesforce (Bot_Session__c, Bot_Event__c)
  │ emit session_closed event
  │ CSAT: if resolved → trigger email CSAT
  │
  ▼
  (session ends)

ESCALATE ◄─── (reachable from any phase via escalation triggers)
  │
  │ build handover payload
  │ if UC-H/J/K and intake_complete → create_case_controlled first
  │ request_handover (select message by hours + reason)
  │ record_outcome (escalated)
  │ sync to Salesforce
  │
  ▼
  HUMAN_HANDLING → (agent takes over in same thread) → CLOSED
```

### 3.3.2 Phase Transitions

| From | To | Trigger |
|------|-----|---------|
| INIT | DISCOVER | form_context parsed, budgets loaded |
| DISCOVER | RESOLVE | active_use_case identified with sufficient confidence |
| DISCOVER | ESCALATE | high-risk/out-of-scope detected, or user requests human |
| RESOLVE | CONFIRM | grounded answer delivered (FAQ path) |
| RESOLVE | ESCALATE | intake complete (intake path), or faq_miss ≥ 2, or budget exceeded |
| CONFIRM | CLOSE | user confirms resolved |
| CONFIRM | RESOLVE | user says "not resolved" → one more retrieval attempt |
| CONFIRM | DISCOVER | user introduces new issue (soft shift) |
| CONFIRM | ESCALATE | user says "not resolved" after retry, or requests human |
| Any | ESCALATE | any escalation trigger from §2.4 |
| ESCALATE | CLOSE | handover completed |

### 3.3.3 Allowed Actions (V1)

| Action | Description | Phases |
|--------|-------------|--------|
| `ask_user` | Ask clarifying question or collect intake field | DISCOVER, RESOLVE |
| `retrieve_knowledge` | Search FAQ vector store | RESOLVE (FAQ UCs only) |
| `answer_grounded` | Deliver grounded response with source citations | RESOLVE, CONFIRM |
| `escalate_human` | Transfer to human agent | Any |
| `finish` | End session normally | CLOSE |

### 3.3.4 Control Budgets

| Budget | Value | Rationale |
|--------|-------|-----------|
| `max_clarification_rounds` | **2** | PRD §1.6.2.1 "2 轮内"；eval clarification dataset calibrated to this |
| `max_faq_miss` | **2** | BRD "faq_miss ≥ 2" triggers escalation |
| `max_bot_turns_per_issue` | **15** (FAQ UCs) / **10** (Intake UCs) | FAQ UCs may need retrieval + confirm + retry; Intake UCs are shorter (collect fields → handover) |
| `max_repeated_same_action` | **2** | Prevent infinite loop of same tool call |
| `max_total_bot_turns_before_forced_escalation` | **25** | Combined with Salesforce hard limit of 50 turns (Bot ≤ 25, leave room for handover messaging). Takes `min(25, 50 - overhead)` |
| Re-retrieval attempts after "not resolved" | **1** | CONFIRM → RESOLVE retry; only once with reformulated query |

### 3.3.5 Drift Handling Implementation（v8 HR-calibrated）

> **v8 HR 关键数据**：90.2% 的会话存在 drift — hard_shift 47.1% / soft_shift 41.4% / minor_drift 1.6% / none 9.8%。多意图处理是 **default case** 而非 edge case。90.2% 会话有 ≥1 secondary UC；36.8% 有 ≥3 secondary UCs。

| Drift Type | Detection | Action | **HR 频次** | **HR Escalation Rate** |
|------------|-----------|--------|------------|----------------------|
| **Minor drift** | User message adds details but intent unchanged (cosine similarity to original query > 0.85) | Keep active_use_case; append info to session state; continue current phase | **1.6%（6/367）** | 50% |
| **Soft shift** | User introduces new topic (intent classifier returns different UC with confidence > 0.6) | Update `active_use_case`; move old UC to `candidate_use_cases`; retain unresolved flag; re-enter DISCOVER | **41.4%（152/367）** | **40.1%** |
| **Hard shift** | New topic is high-risk (UC-G/H/I/J), or user explicitly requests human, or imminent harm signal | Immediately ESCALATE with both original and new UC in payload | **47.1%（173/367）** | **82.7%** |

**Issue preservation**: when soft shift occurs, the `candidate_use_cases` array retains the previous `active_use_case`. The handover payload always includes both if session ends in ESCALATE.

**v8 Design implications**:
- `candidate_use_cases` management is a **core runtime capability** — 90.2% of sessions need it
- Hard shift → ESCALATE is the dominant pattern (82.7%); runtime should optimize this path
- The most common secondary UCs are UC-K(179 appearances), UC-B(114), UC-D(111) — these appear as context even when not the primary intent
- For "Account Support" sessions (46.3% of HR data), drift is nearly universal because the primary UC is often misclassified initially

---

## 3.4 Tools / Capabilities

### 3.4.1 Agent-Visible Tools — Implementation Detail

#### 3.4.1.1 `search_knowledge`

```
Endpoint:  Internal — KnowledgeRetrievalService.search()
```

**Implementation**:
1. Receive `query` + `use_case_id` + optional `knowledge_scope`
2. Embed query via Vertex AI `text-embedding-004` (768-dim)
3. Execute pgvector ANN search:
   ```sql
   SET LOCAL hnsw.ef_search = 100;
   SET LOCAL hnsw.iterative_scan = relaxed_order;

   SELECT c.chunk_id, c.article_id, c.chunk_text, c.chunk_index,
          a.title, a.summary, a.source_url, a.uc_tags,
          1 - (c.embedding <=> $1::vector) AS cosine_similarity
   FROM kb_chunks c
   JOIN kb_articles a ON c.article_id = a.article_id
   WHERE a.is_published = true
     AND a.uc_tags && $2::text[]   -- UC filter (array overlap)
   ORDER BY c.embedding <=> $1::vector
   LIMIT 20;
   ```
4. **Retrieval Gate**: evaluate candidate quality
   - If `cosine_similarity` of top result < 0.3 → `retrieval_miss`
   - If fewer than 2 candidates after filtering → `retrieval_weak_hit`
   - Otherwise → `retrieval_hit`
5. Article-level dedup (keep highest-scoring chunk per article)
6. Rerank top 4–8 via LLM scoring (grounding relevance 1–5 scale)
7. **Answer Gate**: if best `grounding_score < 3.5` → `faq_miss = true`
8. Return top 3 results with `source_id`, `title`, `snippet`, `canonical_url`, `score`

**Failure handling**: `max_retries: 1`. On `no_results` → allow one clarification or escalate. On `knowledge_backend_timeout` → escalate with `service_degraded`.

#### 3.4.1.2 `resolve_article`

**Implementation**:
1. Receive `source_ids[]` (1–3 article IDs)
2. For each article: verify `is_published = true` in `kb_articles` table
3. Construct customer-safe package: `title` + 1–3 sentence summary + `canonical_url` (Help_Site_URL__c)
4. Write `ARTICLE_SHOWN` event to Bot_Event__c
5. Return `resolved_articles[]` with `safe_to_show` flag

#### 3.4.1.3 `get_customer_context`

**Implementation** (composite read — orchestrates runtime-only tools):
1. **Input**: `context_type` (account / listing / combined) + identifiers (email / ad_id from form_context or user input)
2. **Account chain** (when email available, for UC-A/C/D/F/FP/K):
   - `GET /api/emails/{email}/user` → bapi-server (resolve email → user/account)
   - Fallback: `GET /users/{email}/user` → user-service
   - `GET /api/accounts/{accountId}` → bapi-server (account details)
   - `GET /api/users/{id}/known-good` → gumshield-api (trust status)
   - `POST /api/blacklists/email/check` → gumshield-api (blacklist check)
   - `GET /users/last-logged-in/{userId}` → user-service
3. **Listing chain** (when ad_id available, for UC-A/FP/K):
   - `GET /api/adverts/{id}` → bapi-server
   - `GET /api/adverts/{id}/features` → bapi-server
   - `GET /api/adverts/status/{id}` → gumshield-api (moderation status)
   - `GET /api/adverts/{id}/known-good` → gumshield-api
   - **For UC-A/FP only**: `POST /api/cs-review/ad-id/` → gumshield-api (moderation review reason via `get_moderation_review_context`)
   - `GET /api/advert/{advertId}` → livead-search
4. **safe_summary** construction: strip PII; compose human-readable summary; store structured context in session state
5. **Auto-trigger**: `must_auto_trigger_on_form_context: true` — when session starts with email, automatically run account lookup (before UC routing)

**gumshield mock → prod strategy (v6)**:
- Dev/demo: `get_moderation_review_context` returns mock data from `src/test/resources/mock/cs-review-responses.json`
- Staging: if gumshield service account approved → real API; else → mock with `X-Mock: true` header logged
- Prod: real API only after service account approval confirmed

#### 3.4.1.4 `request_handover`

**Implementation**:
1. Build handover payload (§3.6.2)
2. Determine message by `is_business_hours` + `escalation_reason`:
   - Business hours → `escalation_business_hours` template
   - Off hours → `escalation_off_hours` template
3. Write `Bot_Context__c` to Case (JSON payload)
4. Call Salesforce Omni-Channel Transfer API:
   - Online → route to `CS_NEW_chat` queue
   - Offline → route to `CS_Cases_New` queue
5. Write `ESCALATION_REQUESTED` event
6. Return `handover_status`: `queued` / `transferred` / `offline_logged` / `failed`

**is_business_hours check**: query Salesforce for "New Chat" queue agent availability. If ≥1 agent online → business hours.

#### 3.4.1.5 `record_outcome`

**Implementation**:
1. Write to local `session_outcomes` table
2. Update `Bot_Session__c.ContainmentOutcome__c` via Salesforce REST
3. Write `OUTCOME_RECORDED` event to `Bot_Event__c`
4. Publish Avro event to Kafka analytics topic
5. **retry_on_failure: true** — if any sink fails, retry once; log failure but don't block session close

### 3.4.2 Runtime-Only Tools

These are never exposed to LLM prompt. Triggered by runtime logic based on UC policy.

#### `create_case_controlled`

- **Triggered when**: `intake_complete` for UC-H, UC-J, or UC-K
- **Validation**: `tool_policy_enforcer` checks UC ∈ {UC-H, UC-J, UC-K}; validates per-UC required fields (§2.10.5)
- **API**: `POST /services/data/vXX.0/sobjects/Case` → Salesforce
- **Field mapping**:

| Case Field | Source |
|-----------|--------|
| RecordType | `Customer Service` |
| First Name | `form_context.first_name` |
| Last Name | `form_context.last_name` |
| Email | `form_context.email` |
| Topic Subject | UC-H → `Ad Support` / UC-J → `Report a Safety Issue` / UC-K → `Technical Support` |
| Description | Assembled from intake fields + session summary |
| Ad ID Number | `intake_fields.ad_id` (if available) |
| Bot_Context__c | Full handover JSON |

#### `lookup_customer_account` / `lookup_listing_or_ad` / `get_moderation_review_context` / `get_message_moderation_context`

Internally invoked by `get_customer_context` — see §3.4.1.3. No separate external surface.

**`get_message_moderation_context`** (v0.2.1 新���，V1 scope confirmed):
- **Allowed UCs**: UC-C only
- **Trigger**: `get_customer_context` in UC-C messaging diagnostic scenarios (user reports "can't get replies" / "messages not working")
- **API**: `POST /history/moderation/search` → message-moderation-history microservice
- **Purpose**: Determine if user's messages are being blocked by platform safety filters vs. simply unread by recipients
- **Output**: `has_blocked_messages` flag + count + reason summary → feeds into UC-C diagnostic resolution path (Phase 2 §2.6 UC-C-01)

### 3.4.3 Runtime Capabilities (Non-Tool)

#### `fixed_script_library`

- **Storage**: `src/main/resources/scripts/` — YAML files per category (opening.yaml, empathy.yaml, etc.)
- **Template engine**: Mustache-style variable interpolation (`{first_name}`, `{CASE_NUMBER}`, etc.)
- **Selection matrix**: §9 of `fixed_script_library_v1.md` — runtime selects template by `active_use_case` + conversation phase
- **Version tracking**: library version recorded in `prompt_version` (e.g. `scripts-v1.0`)

#### `form_context_ingestion`

- **Trigger**: first message in session (turn 0)
- **Process**: parse pre-chat form fields from Salesforce Case → write to `bot_sessions.form_context`
- **Auto-lookup**: if `form_context.email` present and `active_use_case` allows `get_customer_context` → trigger automatically

#### `tool_policy_enforcer`

- **Implementation**: pre-dispatch interceptor on Tool Dispatcher
- **Logic**: `if (active_use_case NOT IN tool.allowed_use_cases) → return scope_blocked; emit TOOL_SCOPE_BLOCKED event`
- **Enforcement**: hard block — tool call is rejected, event logged, escalation trigger set

#### `progress_placeholder`

- **Threshold**: 1500ms after tool call initiated
- **Message**: "One moment while I look into this for you." (short) / "Thanks for your patience — I'm checking your details now." (medium, >3s)
- **Implementation**: `CompletableFuture.orTimeout()` with placeholder send on timeout

---

## 3.5 Knowledge and Grounding

### 3.5.1 pgvector Schema

```sql
-- Enable extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Article-level table
CREATE TABLE kb_articles (
    article_id      TEXT PRIMARY KEY,           -- Salesforce Knowledge Article Id
    title           TEXT NOT NULL,
    summary         TEXT,
    description     TEXT,                        -- Full article text (for chunking)
    source_url      TEXT,                        -- Help_Site_URL__c (canonical)
    url_category    TEXT,                        -- URL path category
    uc_tags         TEXT[] NOT NULL DEFAULT '{}', -- e.g. {'UC-B','UC-FP'}
    is_published    BOOLEAN NOT NULL DEFAULT true,
    token_count     INT,
    version         INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Chunk-level table
CREATE TABLE kb_chunks (
    chunk_id        TEXT PRIMARY KEY,            -- article_id + '_' + chunk_index
    article_id      TEXT NOT NULL REFERENCES kb_articles(article_id),
    chunk_index     INT NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       vector(768) NOT NULL,        -- Vertex AI text-embedding-004
    token_count     INT NOT NULL,
    section_heading TEXT,                         -- breadcrumb for citation
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (article_id, chunk_index)
);

-- HNSW index (v5 confirmed)
CREATE INDEX CONCURRENTLY idx_kb_chunks_embedding_hnsw
ON kb_chunks USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- B-tree indexes for filtering
CREATE INDEX idx_kb_articles_published ON kb_articles(is_published);
CREATE INDEX idx_kb_articles_uc_tags ON kb_articles USING gin(uc_tags);
```

### 3.5.2 Embedding Pipeline (Offline)

```text
Input: knowledge_base_articles.json (218 articles)
       + article_uc_mapping.csv (UC tags)

Pipeline:
1. For each article:
   a. Clean HTML from Description__c → plain text
   b. Chunk: sliding window 256–512 tokens, 10–20% overlap
      - Preserve table boundaries and numbered lists
      - Attach section_heading from HTML <h*> tags
   c. Embed each chunk via Vertex AI text-embedding-004 (768-dim)
   d. Bulk insert into kb_chunks table
   e. Update kb_articles with metadata + uc_tags

2. Build HNSW index (CREATE INDEX CONCURRENTLY — no downtime)

3. Validation:
   - Verify 218 articles loaded
   - Verify chunk count (~450–600 expected, given 73 articles > 512 tokens)
   - Run 10 known queries → check top-3 recall
```

**Token budget** (from mapping report):
- Total: ~105,085 tokens across 218 articles
- Avg: ~482 tokens/article
- 73 articles need chunking (> 512 tokens)
- 145 articles fit in single chunk

### 3.5.3 Online Retrieval Pipeline

```text
User query
  │
  ├─ 1. Embed query → Vertex AI text-embedding-004 (768-dim)
  │
  ├─ 2. pgvector ANN search (ef_search=100, iterative_scan=relaxed_order)
  │     WHERE is_published=true AND uc_tags && {active_uc_tags}
  │     ORDER BY embedding <=> query_embedding LIMIT 20
  │
  ├─ 3. Retrieval Gate
  │     ├─ top1 cosine_similarity < 0.3 → retrieval_miss
  │     ├─ < 2 candidates after filter → retrieval_weak_hit
  │     └─ otherwise → retrieval_hit
  │
  ├─ 4. Article-level dedup (highest chunk per article)
  │
  ├─ 5. Rerank top 4–8 via LLM grounding judge (1–5 scale)
  │
  ├─ 6. Answer Gate
  │     ├─ best grounding_score < 3.5 → faq_miss (answer_miss)
  │     └─ otherwise → answerable
  │
  ├─ 7. Take top 3 → return to Context Builder
  │
  └─ faq_miss = retrieval_miss OR answer_miss
```

### 3.5.4 Article → UC Integration

At ingest time, each article receives `uc_tags[]` from `article_uc_mapping.csv`. During search, the `uc_tags && {active_uc_tags}` filter restricts results to articles relevant to the current use case, improving precision.

Multi-UC articles (e.g., `UC-B|UC-FP`) are returned for either UC.

### 3.5.5 Answer Contract

All grounded answers must satisfy:
- **Grounded**: backed by at least one `source_id` from retrieved knowledge
- **Understandable**: plain English, concise, UK-appropriate tone
- **Concise**: 1–3 sentences + link (not walls of text)
- **No unsupported promise**: never say "I've fixed it" / "Your refund is processed"
- **Citation**: include `canonical_url` as clickable link

---

## 3.6 Handover Design

### 3.6.1 Trigger Policy (V1)

Escalation is triggered when any condition from §2.4 Escalation Matrix is met. The Control Kernel evaluates triggers **every turn**.

Priority order (if multiple triggers fire simultaneously):
1. `imminent_harm` → immediate, highest priority
2. `user_requested` → immediate
3. `tool_scope_blocked` → immediate
4. `max_bot_turns_exceeded` → forced
5. `intake_complete` → planned (UC-G/H/I/J/K)
6. `faq_miss_threshold_exceeded` → automatic
7. `clarification_budget_exhausted` → automatic
8. All other triggers → automatic

### 3.6.2 Handover Payload Schema

Written to `Case.Bot_Context__c` (LongText) as JSON:

```json
{
  "version": "1.0",
  "session_id": "5Xx000000000001",
  "bot_session_id": "a1B000000000001",
  "primary_use_case": "UC-H-01",
  "candidate_use_cases": ["UC-FP-01"],
  "current_status": "intake_complete",
  "summary": "User reports ad 1487477877 was incorrectly removed. Claims they did not violate posting policies. Ad ID and email collected. Case created.",
  "intent_confidence": 0.92,
  "clarification_count": 1,
  "faq_miss_count": 0,
  "articles_shown": ["ka4P200000002hlIAA"],
  "escalation_reason": "intake_complete_for_uc_h",
  "transcript_ref": "bot_session:a1B000000000001",
  "case_id": "500000000000123",
  "identifiers_collected": {
    "email": "user@example.com",
    "ad_id": "1487477877"
  },
  "intake_fields": {
    "stated_reason": "ad removed for no reason, did not break any rules",
    "platform": null
  },
  "total_bot_turns": 4,
  "handling_duration_seconds": 45,
  "form_topic_subject": "Ad Support",
  "topic_uc_mismatch": false,
  "prompt_version": "v1.0.3",
  "model_version": "gemini-2.0-flash"
}
```

### 3.6.3 Queue Routing

| Condition | Queue | Mechanism |
|-----------|-------|-----------|
| Agent(s) online in "New Chat" queue | `CS_NEW_chat` | Salesforce Omni-Channel Transfer |
| No agents online | `CS_Cases_New` | Case queue assignment (offline processing) |

**All UCs follow the same routing rule** — no per-UC queues (v5 confirmed).

### 3.6.4 Customer-Facing Messages

| Scenario | Template | Source |
|----------|----------|--------|
| Business hours | "I'll pass this to an agent now. I'll share what you've told me so you don't need to repeat yourself." | BRD §6.3 / `fixed_script_library_v1.md` §1.5 |
| Off hours | "The team is currently offline. I've logged your details for follow-up — you'll hear back by email within {SLA_HOURS} hours." | `fixed_script_library_v1.md` §1.6 |
| After case created (UC-H/J/K) | "Thanks for those details. I've created a case ({CASE_NUMBER}) for our {TEAM_NAME} team. You'll hear back by email within {SLA_HOURS} hours." | `fixed_script_library_v1.md` §3.3 |

### 3.6.5 Agent UX Continuity

- Customer stays in **same Messaging Session thread** (`must_preserve_same_thread: true`)
- Agent sees `Bot_Context__c` JSON in Case detail — structured summary, not raw transcript
- Agent sees bot-sent messages in transcript (marked as `Sender_Type__c = Bot`)
- Agent does **not** need to re-ask for identifiers already collected by Bot
- Email follow-up: handled by agent/back-office using existing Salesforce templates; Bot does not send emails

---

## 3.7 Guardrails / Policy

### 3.7.1 Bot Identity Disclosure

- INIT message: "Hi {first_name} — I'm the Gumtree Support Assistant. I can help with common questions, or connect you to the team."
- Never claim to be human
- If user asks "Are you a real person?" → "I'm the Gumtree Support Assistant — an automated helper. I can connect you to our team anytime."

### 3.7.2 Grounding Enforcement

| UC Type | Grounding Source | Enforcement |
|---------|-----------------|-------------|
| FAQ UCs (A/B/C/D/E/F/FP) | pgvector retrieval + `source_ids` | Every `answer_grounded` must have ≥1 source_id; eval grader: `groundedness_pass_rate ≥ 98%` |
| Intake UCs (G/H/I/J/K) | `fixed_script_library` templates only | No generative answers; runtime blocks `search_knowledge` / `resolve_article` calls |

### 3.7.3 Forbidden Phrases — Runtime Detection

The following patterns are checked against every bot response before sending:

```yaml
forbidden_patterns:
  identity_impersonation:
    - "Hello, my name is"
    - "You are speaking to a human"
    - "I am a real human"
  false_action:
    - "I've fixed that"
    - "I've removed the ad"
    - "I've restricted their account"
    - "Your restriction is lifted"
    - "The ads have now been removed"
  false_promise:
    - "I'll process your refund"
    - "Your refund has been issued"
    - "I can guarantee"
  fake_empathy_repeated:
    - "I understand how you feel"  # blocked always
  ai_disclosure:
    - "As an AI language model"
    - "As an AI"
  data_promise:
    - "I have deleted your data"
    - "Your account has been erased"
```

If any pattern matches → log `GUARDRAIL_VIOLATION` event → substitute with safe alternative or escalate.

### 3.7.4 PII Redaction

| Layer | What's Redacted | How |
|-------|----------------|-----|
| `projected_context` | email → `[REDACTED_EMAIL]`, phone → `[REDACTED_PHONE]` | Pre-LLM projection filter |
| `bot_turns.projected_context` | Same as above | Stored redacted |
| `Bot_Event__c.Payload__c` | email, phone, ad_id patterns → redacted | JSON field-level redaction before Salesforce write |
| Kafka events | Same | Avro serializer applies redaction |
| `safe_summary` | Full account/listing dump → human-readable summary without raw identifiers | `get_customer_context` output contract |

### 3.7.5 Policy Versioning (V1)

| Artifact | Version Format | Storage |
|----------|---------------|---------|
| Prompt templates | `prompt-v{major}.{minor}.{patch}` | `/src/main/resources/prompts/` |
| Projection policy | `projection-v{major}.{minor}` | Code constant + config |
| Control policy (budgets, transitions) | `control-v{major}.{minor}` | Code constant + config |
| Tool schemas | `toolspec-v{major}.{minor}` | `customer_service_tool_spec_v0_2.yaml` |
| Eval suite | `eval-v{major}.{minor}` | `data/eval_datasets/` |
| Script library | `scripts-v{major}.{minor}` | `/src/main/resources/scripts/` |

All versions recorded in `bot_sessions` and `bot_turns` tables for traceability. Any version change triggers eval re-run (eval_spec §4).

---

## 3.8 Observability / Analytics

### 3.8.1 Required Events

| Event Type | When Emitted | Key Payload Fields |
|------------|-------------|-------------------|
| `SESSION_STARTED` | First message received | session_id, traffic_variant, form_context (redacted), prompt_version |
| `USE_CASE_INFERRED` | DISCOVER phase completes | active_use_case, candidate_use_cases, intent_confidence, routing_signal_source |
| `RETRIEVAL_EXECUTED` | After `search_knowledge` | query_hash, result_count, top_score, faq_miss, retrieval_latency_ms |
| `ARTICLE_SHOWN` | After `resolve_article` | source_ids[], canonical_urls[] |
| `CLARIFICATION_ASKED` | After `ask_user` action | clarification_count, question_topic |
| `ESCALATION_REQUESTED` | Handover initiated | escalation_reason, is_business_hours, queue, case_id |
| `CASE_CREATED` | After `create_case_controlled` | case_id, use_case_id, topic_subject |
| `OUTCOME_RECORDED` | Session close/escalate | outcome, use_case_id, total_turns, articles_shown |
| `SESSION_CLOSED` | Session ends | containment_outcome, total_duration_ms |
| `TOOL_SCOPE_BLOCKED` | Policy enforcer rejects call | tool_name, active_use_case, attempted_use_case |
| `GUARDRAIL_VIOLATION` | Forbidden phrase detected | violation_type, original_text_hash |

### 3.8.2 Trace Schema (per turn)

```json
{
  "trace_id": "uuid",
  "session_id": "sf_session_id",
  "turn_id": "uuid",
  "turn_index": 3,
  "prompt_version": "prompt-v1.0.3",
  "model_version": "gemini-2.0-flash",
  "projection_version": "projection-v1.0",
  "active_use_case": "UC-FP-01",
  "phase": "RESOLVE",
  "action_selected": "answer_grounded",
  "tool_calls": [
    {
      "tool_name": "search_knowledge",
      "latency_ms": 320,
      "status": "success",
      "result_count": 3,
      "faq_miss": false
    }
  ],
  "source_ids": ["ka4P200000002hlIAA"],
  "outcome": null,
  "latency_ms": 1850,
  "timestamp": "2026-04-20T14:30:00Z"
}
```

### 3.8.3 Event Pipeline

```text
Bot Runtime
  │
  ├─► PostgreSQL (bot_turns, session_outcomes)    — local hot store
  ├─► Salesforce (Bot_Session__c, Bot_Event__c)   — agent-visible + reporting
  └─► Kafka (Avro, analytics topic)               — data warehouse pipeline
       │
       └─► BigQuery / Looker (downstream)          — dashboards
```

### 3.8.4 V1 Minimal Funnel Dashboard

| Metric | Source | Target |
|--------|--------|--------|
| Total sessions | `SESSION_STARTED` count | — |
| Understood sessions | `USE_CASE_INFERRED` with confidence > threshold | — |
| Resolved by bot | `OUTCOME_RECORDED` where outcome=resolved | D1: 10–25% |
| Escalated to human | `OUTCOME_RECORDED` where outcome=escalated | — |
| Abandoned sessions | `OUTCOME_RECORDED` where outcome=abandoned | Threshold TBD |
| Wrong containment | Back-annotated via repeat contact or human review | ≤ 2% |
| Repeat-contact proxy | Same user + same UC within 24h | Should not increase |
| Latency p50 / p95 | `bot_turns.latency_ms` | p95 ≤ 5s (FAQ answer) |
| Cost per conversation | LLM token usage × pricing | — |

### 3.8.5 Technology Stack

| Layer | Technology |
|-------|-----------|
| Application logging | Logback + SLF4J → GCP Cloud Logging |
| Metrics | Micrometer → Prometheus (PodMonitoring on `/internal/metrics`) |
| Error tracking | Sentry |
| Tracing | Istio sidecar + GCP Cloud Trace |
| Dashboards | Grafana (Prometheus) + Looker (BigQuery) |

---

## 3.9 Non-Functional Requirements

### 3.9.1 Reliability

| Requirement | Implementation |
|-------------|---------------|
| Handover request retryable | `request_handover` retries once on Salesforce API failure; if still fails → return `failed` status + log alert; degrade to "please contact us again" message |
| State write verifiable | PostgreSQL transaction with `RETURNING` clause; Salesforce write verified via HTTP 201 response |
| Degraded mode | If LLM unavailable or times out → Control Kernel enters ESCALATE with reason `service_degraded`; user gets "I'm having trouble right now — let me connect you to the team." |
| No data loss on crash | Session state persisted to PostgreSQL on every turn completion; Salesforce sync is async but has retry queue |
| Idempotent message handling | Dedup on `(session_id, turn_index)` unique constraint; replay-safe |

### 3.9.2 Performance

| Metric | Target | Enforcement |
|--------|--------|-------------|
| FAQ answer e2e p95 | ≤ 5s | Includes: query embedding (~100ms) + pgvector search (~50ms) + rerank (~200ms) + LLM generation (~2–3s) + Salesforce write (~500ms) |
| Escalation request e2e p95 | ≤ 3s | Salesforce Transfer API + payload write |
| Median turns for solved FAQ | ≤ 6 | Control budget enforcement |
| pgvector query latency p95 | ≤ 100ms | HNSW index with ef_search=100; 218 articles / ~500 chunks is small dataset |
| Vertex AI Embedding latency p95 | ≤ 200ms | Single query embedding (768-dim) |
| Progress placeholder threshold | 1.5s | CompletableFuture timeout |

### 3.9.3 Privacy / Security

| Requirement | Implementation |
|-------------|---------------|
| Minimum data exposure | `safe_summary` pattern on all context lookups; raw PII never in LLM prompt |
| Redacted logs | PII regex filter on all log appenders (email, phone, name patterns) |
| Secret isolation | All API keys/tokens in GCP Secret Manager; env var injection; never in code/config |
| Retention policy compliance | Chat transcripts retained per Gumtree data retention policy; `kb_chunks.embedding` vectors are derivative data — retain as long as source article exists |
| UK GDPR compliance | No sensitive data in free text; explain reason for identifier collection; GDPR intake (UC-G) follows strict field boundary (§2.6) |
| Service-to-service auth | Istio mTLS within GKE; AES-encrypted auth tokens for external APIs |

### 3.9.4 Maintainability

| Requirement | Implementation |
|-------------|---------------|
| Clear versioned configs | All prompts, policies, tool schemas, scripts versioned with semver; recorded in trace |
| Bounded tool surface | 5 agent-visible + 4 runtime-only; no tool added without eval-first |
| Regression-friendly | All changes trigger eval via CI gate; eval datasets are ground truth |
| Config vs code separation | Budgets, UC registry, escalation triggers → YAML config (hot-reloadable); control logic → code |
| Repo structure | Standard Gumtree layout: `/server` (Spring Boot), `/helm-chart`, `/contract` (OpenAPI), `/data` (eval datasets + knowledge) |

### 3.9.5 Scalability

| Aspect | Design |
|--------|--------|
| Horizontal scaling | Stateless request handling (session state in PostgreSQL); HPA on CPU/memory |
| Connection pooling | HikariCP for Cloud SQL (pgvector queries + session state); cloud-sql-proxy sidecar |
| LLM rate limiting | Vertex AI quota managed via GCP project; client-side rate limiter with backpressure |
| Kafka throughput | Async event publishing; batch Avro serialization |

---

## 3.10 Database Summary

### Cloud SQL PostgreSQL Instance

Single Cloud SQL instance (shared with pgvector) hosts:

| Schema | Tables | Purpose |
|--------|--------|---------|
| `bot` | `bot_sessions`, `bot_turns`, `session_outcomes` | Bot session state + turn log + outcome tracking |
| `knowledge` | `kb_articles`, `kb_chunks` | Knowledge base + vector index |

**Connection**: via cloud-sql-proxy sidecar (same as all Gumtree services).

### Salesforce Objects

| Object | Purpose | Write Frequency |
|--------|---------|----------------|
| `Bot_Session__c` | Session lifecycle tracking | On session start + on close/escalate |
| `Bot_Event__c` | Event audit trail | On each significant event (§3.8.1) |
| `Chat_Message_Log__c` | Raw message log | On each message |
| `Case` (existing) | Pre-chat form Case + Bot-created Cases | On `create_case_controlled` |
| `Case.Bot_Context__c` | Handover payload JSON | On `request_handover` |

---

## 3.11 API Contracts

### 3.11.1 Inbound (Salesforce → Bot)

```yaml
POST /v1/chat/inbound
Content-Type: application/json

Request:
  session_id: string           # Salesforce MessagingSession Id
  case_id: string              # Pre-chat-form Case Id
  message_text: string         # User message content
  message_type: string         # text / system / form_submit
  form_data:                   # Only on first message (turn 0)
    first_name: string
    last_name: string
    email: string
    topic_subject: string
    ad_id_number: string?
    description: string
  timestamp: string            # ISO 8601

Response:
  reply_text: string           # Bot response to show user
  intent: string               # CONTINUE / TRANSFER_TO_HUMAN / END_CHAT
  should_end_chat: boolean
  additional_data:
    case_number: string?       # If case was created
    handover_queue: string?    # If transferring
```

### 3.11.2 Internal — Knowledge Search

```yaml
POST /v1/faq/search (internal)

Request:
  query: string
  use_case_id: string
  top_k: integer (default 3, max 5)
  customer_locale: string (default "en_GB")

Response:
  results:
    - source_id: string
      title: string
      snippet: string
      canonical_url: string
      score: number
      chunk_id: string
      article_id: string
  faq_miss: boolean
  retrieval_trace:
    retrieval_gate: string     # hit / weak_hit / miss
    answer_gate: string        # answerable / miss
    top_similarity: number
    candidate_count: integer
```

### 3.11.3 Health

```yaml
GET /internal/health
GET /internal/health/liveness
GET /internal/health/readiness

Response: { "status": "UP" | "DOWN", "checks": {...} }
```

---

## 3.12 Release Criteria for V1

Inherited from `phase0_normative_freeze.md` §0.3, with Gumtree-specific instantiation.

> **v8 HR calibration note**: Human review of 367 sessions revealed Topic Subject routing accuracy of only 34.9%. The `active_use_case_accuracy ≥ 85%` gate measures Bot's **Description-based classification** accuracy (not Topic Subject → UC routing). The UC correction rate of 16.3% in auto-classified data sets a quality baseline — Bot must do better than the original auto-classifier.

| Gate | Threshold | Measurement |
|------|-----------|-------------|
| Core bounded loop implemented | ✅ | Manual verification |
| Use case registry lite implemented | 12 UCs registered | Config + test |
| Projection policy fixed and versioned | Version tracked in traces | Trace inspection |
| Handover payload fixed | Schema validated in CI | Handover Contract Suite |
| Critical eval suite runnable | P0 suites pass | CI gate |
| Critical policy violation | = 0 | Grounding & Policy Suite |
| Active use case accuracy | ≥ 85% | Core E2E Suite (150 sessions) |
| Candidate use case recall | ≥ 95% | Core E2E Suite |
| Groundedness pass rate | ≥ 98% | Grounding & Policy Suite |
| Escalation recall on required cases | ≥ 95% | Escalation Suite (150 sessions) |
| Wrong containment | ≤ 2% | Online monitoring + human review |
| Handover completeness | ≥ 98% | Handover Suite (76 sessions) |
| Repeated same action rate | Below threshold | Control Suite (30 sessions) |
| Median turns for solved FAQ | ≤ 6 | Runtime metrics |
| FAQ answer p95 | ≤ 5s | Runtime metrics |
| Regression gate in CI | Smoke on PR, full on RC | Jenkins pipeline |
| Minimal analytics funnel available | Dashboard operational | Manual verification |

---

## 3.13 Phase 3 → Phase 4 Handoff

Phase 4 (Coding Agent Implementation Packet) receives:

| Deliverable | Status |
|-------------|--------|
| Runtime architecture + component breakdown | ✅ This document §3.1 |
| State model DDL (PostgreSQL + Salesforce) | ✅ §3.2 |
| Control kernel state machine + budgets + transitions | ✅ §3.3 |
| Tool implementation specs (all 12 tools: 5 agent-visible + 5 runtime-only + 2 human-only; + 4 capabilities) | ✅ §3.4 |
| pgvector schema + retrieval pipeline + embedding config | ✅ §3.5 |
| Handover payload schema + routing + UX | ✅ §3.6 |
| Guardrails implementation (forbidden phrases, PII, versioning) | ✅ §3.7 |
| Observability event schema + trace schema + funnel | ✅ §3.8 |
| NFR targets + implementation approach | ✅ §3.9 |
| API contracts (inbound, search, health) | ✅ §3.11 |
| Release criteria with thresholds | ✅ §3.12 |

**v8 additions from human review**:
- OUT_OF_SCOPE category handling in state machine (§3.3.1) + session state (§3.2.2) + events (§3.2.1)
- Drift handling calibrated with HR distribution data (§3.3.5): 90.2% drift prevalence
- "Account Support" special routing path requiring full-candidate-set classification (phase2 §2.11.2)
- Human review annotations as supplementary ground truth for eval datasets (phase5)

**Phase 4 will produce**: module breakdown, delivery order, required contracts (DB schema DDL, API OpenAPI specs, tool schema files, trace/event Avro schemas, config YAML schemas), required tests, and done criteria.
