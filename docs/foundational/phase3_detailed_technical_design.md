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

> **[DEVIATION 2026-05-01]** 本章描述的 LLM 响应解析、Context Projection、State Model 与 Trace Schema 已从原 5-action 抽象层迁移至 OpenAI-style 单层 tool-use 模型。详见 `phase0_normative_freeze.md` §0.6 deviation log；新响应契约见 §3.3.3。

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
| **Control Kernel** (v9) | Deterministic turn supervisor: budget management, drift detection, phase transition validation, persistence, response packaging. Does NOT invoke LLM or dispatch tools directly. | Stateless per-turn evaluation against session state |
| **Phase Evaluator** (v9) | Phase **planner**: produces `PhasePlan` (mission briefing) describing what the agent loop should accomplish. After the run: `interpretRunResult()` decides phase transition. Does NOT execute tools or call LLM directly. | Phase-aware planning + result interpretation |
| **Agent Run Loop** (v9 — NEW) | Mechanical model↔tool execution worker. Bounded by `plan.maxToolSteps`. Loop: build projection → invoke LLM → if tool_calls, dispatch via ToolDispatcher → accumulate results → loop. Terminates on final user_message, escalation, or max steps. | Bounded iterative LLM/tool executor |
| **Context Builder** | Pure projection assembly. Called by `AgentRunLoop` every iteration so model sees fresh state after each tool result. Includes the full `PhasePlan` (objective, allowed tools, grounding instruction, valid terminal outcomes) so the LLM understands its mission. | Template engine + state reader |
| **LLM Invocation Layer** | 调用 Vertex AI Gemini；解析结构化输出（tool_calls + user_message + reasoning）| Vertex AI Java SDK |
| **Tool Dispatcher** (v9) | Routes tool calls to implementations; schema validation; policy enforcement; **`validateAgainstPlan(plan, toolCall)`** rejects any tool not in `plan.allowedTools` before dispatch (defense in depth — even if LLM ignores its allowed-tool list, the dispatcher refuses unauthorized calls). | Spring Bean routing + JSON Schema validator |
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
   c. (v9) If FAQ UC routed AND form_context.description non-empty → auto-search:
      ControlKernel.processMessage(session, formDescription) runs immediately,
      greeting becomes the grounded answer (not a static template)
3. Control Kernel:
   a. Read current phase (INIT/DISCOVER/RESOLVE/CONFIRM)
   b. Check budgets (turns, clarification, faq_miss)
   c. Check escalation triggers
   d. If budget exceeded or trigger hit → ESCALATE
4. Context Builder:
   a. Project minimal context from session state + latest message + retrieved knowledge
   b. Inject `tool_schemas` (per-UC visible tool list with full schema) for current UC
5. LLM Invocation:
   a. Send projected context → Vertex AI Gemini
   b. Parse structured response: { user_message, reasoning, tool_calls: [{name, arguments}] }
   c. Semantic interpretation: empty `tool_calls` + non-empty `user_message` ⇒ clarification or grounded answer; presence of `request_handover` in `tool_calls` ⇒ escalation; presence of `record_outcome` only ⇒ session close
6. Tool Dispatcher:
   a. For each entry in `tool_calls[]`: `ToolPolicyEnforcer.isToolAllowed(name, active_use_case)` → execute tool → record result
   b. If tool latency >1.5s → send progress_placeholder
   c. Return tool result to context for next LLM call (if needed)
7. Response Assembly (derived from `tool_calls` + `user_message`):
   a. If `tool_calls` contains `request_handover` → invoke Handover Module
   b. If `tool_calls` contains `record_outcome` only AND no `user_message` → session close
   c. If `tool_calls` contains `search_knowledge` / `resolve_article` → fetch knowledge, then expect a follow-up turn with `user_message` containing the grounded answer
   d. If `user_message` non-empty AND no `tool_calls` → direct response (clarification or grounded answer)
   e. Fixed-script rendering happens at orchestration layer when intake UCs (UC-G/H/I/J/K) need template output
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
    -- [DEVIATION 2026-05-01 — phase0 §0.6] action_selected / action_parameters columns removed.
    -- Semantic actions are now derived from tool_calls + user_message presence (see §3.3.3).
    -- Migration: one-shot Flyway DROP COLUMN on existing deployments.
    
    -- Tool
    tool_calls          JSONB,                       -- OpenAI-style array of {name, arguments, output, latency_ms, status}
    -- [DEVIATION 2026-05-01 — phase0 §0.6] When phase_after = 'ESCALATE' the persisted tool_calls
    -- MUST contain a `request_handover` entry so that downstream graders (eval L1
    -- `escalation_compliance`) can derive the semantic action from the trace. Both write paths
    -- uphold this invariant:
    --   (1) AgentRunLoop path: the LLM's own request_handover tool_call is appended natively.
    --   (2) Legacy ControlKernel.recordTurn() path AND ControlKernel.forceEscalate() (budget /
    --       drift / hard-OOS triggers): the synthetic helper `synthesizeHandoverToolCall(session)`
    --       appends `{name:"request_handover", arguments:{escalation_reason: session.escalationReason}}`
    --       when the existing tool_calls JSONB does not already contain one. Shared helper enforces
    --       parity between the two paths so the trace contract is path-agnostic.
    
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
  "tool_schemas": [
    {
      "name": "search_knowledge",
      "description": "Search the FAQ vector store for grounded knowledge.",
      "arguments": {"type": "object", "properties": {"query": {"type": "string"}}, "required": ["query"]}
    },
    {
      "name": "resolve_article",
      "description": "Materialize 1–3 article IDs into customer-safe packages.",
      "arguments": {"type": "object", "properties": {"source_ids": {"type": "array", "items": {"type": "string"}}}, "required": ["source_ids"]}
    },
    {
      "name": "request_handover",
      "description": "Escalate the conversation to a human agent.",
      "arguments": {"type": "object", "properties": {"escalation_reason": {"type": "string"}}, "required": ["escalation_reason"]}
    },
    {
      "name": "record_outcome",
      "description": "Persist the final session outcome (resolved/escalated/abandoned).",
      "arguments": {"type": "object", "properties": {"outcome": {"type": "string"}}, "required": ["outcome"]}
    }
  ]
}
```

> **[DEVIATION 2026-05-01 — phase0 §0.6]** `allowed_actions` 字段已从 projected context 中移除。`tool_schemas` 升级为 per-UC 完整 tool schema（name + description + arguments JSON schema），由 `ToolPolicyEnforcer` 按 `active_use_case` 过滤后注入，作为 LLM 工具发现的唯一信源。UC × phase 约束统一由 tool 可用性矩阵承担。

**Projection Rules**:

1. **Relevance first** — only inject knowledge/context related to active UC
2. **Recency with unresolved override** — last 3–5 key messages; unresolved issues always retained
3. **Structured over raw** — use `safe_summary`, not full API dump
4. **Grounding priority** — retrieved knowledge always included when available
5. **Risk-aware injection** — risk_flags injected when non-empty
6. **Schema minimization** — only tool schemas allowed for current UC (filtered by `ToolPolicyEnforcer` against `active_use_case`)

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
  │ if ambiguous → emit clarifying user_message  │            │     │
  │ if OOS topic → fixed_script → ESCALATE      │            │     ��
  │ if high-risk UC detected → set risk_flags   │            │     │
  │ if intake-only UC → phase = RESOLVE(intake) │            │     │
  │ if FAQ UC → phase = RESOLVE(faq)            │            │     │
  ▼                                             ▼            │     │
RESOLVE ───────────────────────────────────────────────┐     │     │
  │                                                     │     │     │
  │ FAQ path:                                           │     │     │
  │   search_knowledge → resolve_article →              │     │     │
  │   grounded user_message → phase = CONFIRM           │     │     │
  │                                                     │     │     │
  │ Intake path (UC-G/H/I/J/K):                        │     │     │
  │   fixed_script → user_message (intake fields) →    │     │     │
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

> **Initial phase routing — hard vs soft OUT_OF_SCOPE (deviation 2026-05-01 — phase0 §0.6)**: `SessionManager.createSession()` (`server/src/main/java/.../session/SessionManager.java:170-178`) invokes `UseCaseRouter.route()` on the form's `topic_subject` + `description` and historically forced any `OUT_OF_SCOPE` return value into `currentPhase=ESCALATE / handlingState=QUEUE_TO_HUMAN / containmentOutcome=escalated`, bypassing DISCOVER entirely. This branch is now split:
> - **Hard OOS** — `topic_subject` matches the `UseCaseRouter` handover-only registry (Delivery, Pro Contract, Account Manager Support, Ratings & Reviews; same list referenced in §0.3 row "V1 use case 集合"): keep current behavior — initial phase is ESCALATE, no DISCOVER turn.
> - **Soft OOS** — `UseCaseRouter` returned `UNKNOWN_TOPIC` or an ambiguous match (e.g. `topic_subject` text such as `"Replies & Messaging"` not exactly matching the registry's `"Replies or Messaging"`): initial phase is `DISCOVER` with `activeUseCase=null`. The bot gets one clarifying turn before any escalation decision; the existing DISCOVER → ESCALATE edge in the table below remains the only escalation path for these sessions.
>
> Mitigation against trust-and-safety regression: the hard-OOS list is mirrored explicitly from `UseCaseRouter`'s handover-only registry; any topic added to that registry automatically inherits the immediate-escalate behavior.

> **DISCOVER plan tool surface (deviation 2026-05-02 — phase0 §0.6 / Phase 2 Step 3c)**: `PhaseEvaluator.plan()`'s DISCOVER `allowedTools` is `[search_knowledge, classify_use_case]` (was `[search_knowledge]` only). `classify_use_case` is a new AGENT_VISIBLE tool (§3.4.1.6) that lets the LLM commit `session.activeUseCase` + `session.intentConfidence` once it has enough signal to identify the user's intent. Without this tool, soft-OOS DISCOVER sessions could only ask clarifying questions until the clarification budget exhausted — producing the `CONTRACT_VIOLATION:active_use_case missing_after_turns` class observed on `cs_interactive_001/002/014/029/259`. The DISCOVER → RESOLVE edge in the table below already triggers on `activeUseCase != null` after FINAL_ANSWER; no new transition logic is required, only the commit mechanism.

| From | To | Trigger |
|------|-----|---------|
| INIT | DISCOVER | form_context parsed, budgets loaded; or soft-OOS session (UseCaseRouter `UNKNOWN_TOPIC`/ambiguous, `activeUseCase=null`) |
| INIT | ESCALATE | hard-OOS session — `topic_subject` on UseCaseRouter handover-only registry |
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

### 3.3.3 Agent Run Loop Architecture (v9 — D16)

> **[FOLD-BACK 2026-05-11]** This section was last reconciled with delivered code on 2026-05-11. For the live runtime contract (component ownership, `PhasePlan` table per phase, `tool_schemas` projection filter, direct runtime tool invocations, `accumulated_tool_results` in-loop / same-turn semantics — see also the "Direct runtime tool invocations" note at §3.3.3 below for the explicit no-cross-turn-persistence statement — and TODO_DECISIONs that have not yet been folded back here), see [`docs/current/runtime_contract.md`](../current/runtime_contract.md). This section preserves the *intent* and *historical deviation log*; `runtime_contract.md` is the source of truth for "what currently runs". Sources cited inline below: `ControlKernel.java`, `PhaseEvaluator.java`, `AgentRunLoopImpl.java`, `ContextProjectionBuilder.java`, `ToolDispatcher.java`, `ClassifyUseCaseTool.java`, `RequestHandoverTool.java`, `RecordOutcomeTool.java` (all under `server/src/main/java/com/gumtree/csagent/service/`).

> **Background**: The original V1 design merged turn supervision, phase logic, and tool execution into `ControlKernel + PhaseEvaluator`. As tool surface grew, this became leaky: LLM-requested tools (e.g., `get_customer_context` after the user supplies an ad ID) were parsed and recorded in trace, but **never executed**. Pre-planned tools that PhaseEvaluator hardcoded (e.g., `search_knowledge` in FAQ phase) worked, but generic LLM tool-use did not.
>
> **v9 Decision**: Introduce `AgentRunLoop` as a dedicated model↔tool execution worker. Refactor `PhaseEvaluator` from *executor* to *planner* (returns a `PhasePlan`). Keep `ControlKernel` as the deterministic supervisor.

#### Component Boundaries

| Component | Owns | Does NOT |
|-----------|------|----------|
| `ControlKernel` | Deterministic turn lifecycle: budget, drift, transitions, persistence, response packaging | Talk to LLM, dispatch tools, decide phase content |
| `PhaseEvaluator` | Phase **planner** — returns `PhasePlan`. After run: `interpretRunResult()` decides phase transition. | Call LLM directly, dispatch tools directly, build context JSON |
| `AgentRunLoop` | Mechanical model↔tool loop, bounded by `plan.maxToolSteps` | Decide business phase, validate phase transitions, persist turns |
| `ContextProjectionBuilder` | Pure projection assembly. Called by `AgentRunLoop` every iteration. **[FOLD-BACK 2026-05-11]** It does **not** read `plan.requiredContextKeys` to decide what to include — the live `build(...)` populates a fixed set of slots (`session`, `task_summary`, `risk_flags`, `intake_state`, `candidate_use_cases`, `primary_entity`, plus `form_context` / `customer_context` / `listing_context` when the corresponding `BotSession` field is non-empty) keyed off `BotSession` state, not off `plan.requiredContextKeys`. **No `moderation_context` slot is projected today** (cite: `ContextProjectionBuilder.java:544–552` only emits `customer_context` and `listing_context`) even though FAQ RESOLVE `PhasePlan.requiredContextKeys` declares `moderation_context` — see the matching TODO_DECISION in §3.3.3. | Decide what to include based on the plan's `requiredContextKeys` (despite the name, this is planner metadata only today — see the projection-contract TODO_DECISION) |
| `ToolDispatcher` | Validates every tool call against `plan.allowedTools` (`validateAgainstPlan`), dispatches, audits | Trust the LLM blindly |

#### Per-Turn Execution Flow

```
ControlKernel.processMessage(session, userMsg)
  │
  ├─ 1. budget / drift / forced-escalation gates  (deterministic)
  │
  ├─ 2. PhaseEvaluator.plan(session, userMsg, history) → PhasePlan
  │
  ├─ 3. AgentRunLoop.run(plan, session, userMsg, history) → AgentRunResult
  │     │  loop until terminal (max plan.maxToolSteps):
  │     │    a. ContextProjectionBuilder.build(session, plan, history, accumulatedToolResults)
  │     │    b. LlmInvocation.invokeChat(projection)
  │     │    c. parse → if tool_calls: ToolDispatcher.dispatch(call, plan) → accumulate
  │     │    d. if final user_message: terminate
  │     │    e. if max steps: terminate with MAX_STEPS
  │
  ├─ 4. PhaseEvaluator.interpretRunResult(plan, runResult) → PhaseTransitionDecision
  │
  └─ 5. ControlKernel applies transition + recordRunResult()
        (persists every LlmCallEvent and ToolEvent into bot_turns / llm_call_log / bot_events
         with sequence numbers for replay-quality trace)
```

> **Escalation evidence invariant (deviation 2026-05-01 — phase0 §0.6)**: Two server-side write paths produce a `phase_after = ESCALATE` row in `bot_turns`: the AgentRunLoop path (via `recordRunResult`) and the legacy `ControlKernel.recordTurn()` path (still live for any phase whose route key is absent from `enabled-phases`, plus the budget / drift / hard-OOS branch in `ControlKernel.forceEscalate()`). To uphold the phase0 §0.6 contract that semantic actions are derived from `tool_calls` JSON, **both** paths route through a shared helper `synthesizeHandoverToolCall(session)` that appends `{name:"request_handover", arguments:{escalation_reason: session.getEscalationReason()}}` to `bot_turns.tool_calls` whenever the existing JSONB does not already contain a `request_handover` entry. Cite: `ControlKernel.recordTurn()` (`server/.../control/ControlKernel.java:367-425`) and `ControlKernel.forceEscalate()` (same file `:253-304`). Without this, eval L1 `escalation_compliance` mis-fails escalations produced by the legacy / forced branches.

#### `PhasePlan` Contract

```java
public record PhasePlan(
    String phase,                                    // RESOLVE, INTAKE, CONFIRM, ...
    String useCase,                                  // UC-A, UC-H, ...
    String objective,                                // human-readable mission statement
    List<String> allowedTools,                       // whitelist; ToolDispatcher rejects others
    Set<String> requiredContextKeys,                 // form_context, customer_context, ...
    int maxToolSteps,                                // bounds the loop, e.g. 4
    boolean allowInterimMessage,                     // future: enables ack/progress emits (Phase E)
    Set<TerminalOutcome> validTerminalOutcomes,      // FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE
    String systemInstruction,                        // phase-specific system prompt addendum
    String groundingInstruction,                     // grounding rules for this phase
    String escalationPolicy                          // when to escalate, what reasons valid
) {}
```

The `PhasePlan` is **injected into the projected context** so the LLM understands its mission. `maxToolSteps` is enforced server-side and not shown to the model.

> **[FOLD-BACK 2026-05-11]** Live `ContextProjectionBuilder.build(...)` emits a `phase_plan` node containing only `phase`, `use_case`, `objective`, `allowed_tools`, `grounding_instruction`, `system_instruction`, `escalation_policy`, and `valid_terminal_outcomes` (cite: `ContextProjectionBuilder.java` `if (plan != null)` block). `requiredContextKeys` and `allowInterimMessage` are **not** projected — `requiredContextKeys` is planner metadata only today (not read by the projection builder or enforced as a precondition), and `allowInterimMessage` gates the unimplemented Phase E streaming path. The "full PhasePlan minus only `maxToolSteps`" wording in earlier docs is therefore inaccurate. See the projection-contract TODO_DECISION in the §3.3.3 list.

#### `AgentRunResult` Contract

```java
public record AgentRunResult(
    List<AgentMessage> messages,                     // 1..N: ack? + progress* + final
    List<ToolEvent> toolEvents,                      // every tool call with input/output/latency
    List<LlmCallEvent> llmEvents,                    // every LLM invocation in the loop
    TerminalOutcome terminalOutcome,                 // see enum below
    String finalUserMessage,
    Optional<String> escalationReason,
    String lastProjection,                           // JSON projection sent to final LLM call (Sprint 8.2 §M0b)
    String lastLlmRawResponse                        // verbatim LLM response of final iteration; preserved for trace replay
) {}
```

> **[FOLD-BACK 2026-05-11]** `lastProjection` and `lastLlmRawResponse` were added in Sprint 8.2 §M0b to preserve the final-iteration LLM raw response for replay-quality trace even when the loop terminates with `MAX_STEPS` / `CLARIFICATION_NEEDED`. The original 6-field record predates that sprint.

The actual `TerminalOutcome` enum surfaced by `AgentRunLoopImpl` is wider than the original FINAL_ANSWER / ESCALATE / MAX_STEPS / ERROR set. Current values, with the producing condition:

| Value | Producing condition |
|---|---|
| `FINAL_ANSWER` | LLM returned a response with **no `tool_calls`** and the `user_message` does not match the clarification heuristic (cite: `AgentRunLoopImpl.run(...)` step 5 / `AgentRunResult.finalAnswer(...)`). **Parser failures do NOT produce `FINAL_ANSWER`**: `ActionParser.parse(...)` catches its own exceptions internally and returns a fallback `ParsedAction` whose `tool_calls` contains a single `request_handover` with `escalation_reason="system_failure"` (cite: `ActionParser.buildFallback()`). **[FOLD-BACK 2026-05-11]** Whether that fallback ends the run as `ESCALATE` depends on the active `PhasePlan`: `ToolDispatcher.validateAgainstPlan(plan, "request_handover")` runs **before** `ToolDispatcher.dispatch`, so on plans that omit `request_handover` from `allowedTools` (currently DISCOVER and CLOSE — DISCOVER: `[search_knowledge, classify_use_case]`; CLOSE: `[record_outcome]`) the fallback handover is rejected with `tool_not_in_plan`, the loop records a failed `ToolEvent`, and no coercion / dispatch / `ESCALATE` happens — the run continues until another exit condition fires (e.g. `MAX_STEPS`). On plans that **do** allow `request_handover` (RESOLVE_FAQ, RESOLVE_INTAKE, CONFIRM, ESCALATE), the fallback path proceeds as previously documented: the dispatcher coerces the non-canonical reason to `service_degraded` (`ToolDispatcher.CANONICAL_ESCALATION_REASONS`), the handover dispatches, and the loop terminates with `ESCALATE`. The defensive try/catch around `actionParser.parse(...)` in `AgentRunLoopImpl.run(...)` is therefore unreachable in normal operation; if it ever fires it falls back to a `FINAL_ANSWER` with the raw content, but `ActionParser` does not throw today. |
| `CLARIFICATION_NEEDED` | LLM returned a `user_message` with no `tool_calls` and the message matches the clarification heuristic (ends with `?` / contains "could you", "can you tell", "what is", "which", "do you have"). Sprint 8.1 follow-up. |
| `ESCALATE` | A `request_handover` tool call dispatched **successfully** (any failure in the same batch is recorded as a tool error and the loop continues — Sprint 9 §O1). |
| `USE_CASE_IDENTIFIED` | A successful `classify_use_case` inside a DISCOVER plan committed a non-blank `activeUseCase`. Sprint 8.1 §M3; signals the kernel to attempt a same-turn DISCOVER → RESOLVE replan (see "Same-turn DISCOVER → RESOLVE replan" below). Note: this value is produced by the loop **out-of-band** of the DISCOVER plan's declared `validTerminalOutcomes` — see TODO_DECISION below. |
| `MAX_STEPS` | `plan.maxToolSteps` hit without termination. |
| `ERROR` | Loop entry / projection / LLM invocation failure only — specifically: `null` plan, `ContextProjectionBuilder.build(...)` exception, or `LlmInvocationService.invokeChat(...)` exception (cite: `AgentRunLoopImpl.run(...)` `AgentRunResult.error(...)` call sites). **Tool-dispatch exceptions are NOT mapped to `ERROR`**: they are recorded as a failed `ToolEvent` (`tool_dispatch_exception: ...`) and the loop continues to the next call (cite: `AgentRunLoopImpl.run(...)` step 6b try/catch). Parser failures are not `ERROR` either — see `FINAL_ANSWER` above. |
| `DEADLINE_EXCEEDED` | Wall-clock budget exhausted before any LLM attempt could complete (Sprint 8.1 §M2) |
| `LLM_UNAVAILABLE` | LLM provider chain failed after retry exhaustion. **[FOLD-BACK 2026-05-11]** Live wiring is **DeepSeek v4 pro primary + Kimi 2.6 fallback** (`server/.../config/LlmClientConfig.java` `llmClient(...)` constructs `new FallbackLlmClient(deepseekLlmClient, kimiLlmClient, "deepseek", "kimi")` per Sprint 8.1 follow-up #2). The §3.9.1 "Degraded mode" entry below still reads as Kimi-primary / DeepSeek-fallback — that is the original 2026-05-01 deviation text and is now superseded by the Sprint 8.1 follow-up #2 reversal; treat the live `LlmClientConfig` constructor as authoritative. |

`PhaseEvaluator.interpretRunResult(...)` maps these to phase transitions. `MAX_STEPS` becomes an ESCALATE with the *transition reason* `max_steps_exceeded` and an *escalation reason* derived per plan / session: `incomplete_intake` for INTAKE plans, else `clarification_budget_exhausted` when the session has clarification turns, else `faq_miss_threshold_exceeded` when the loop ran `search_knowledge`, else the catch-all `turn_budget_exhausted` (cite: `PhaseEvaluator.resolveMaxStepsReason(...)` and the `MAX_STEPS` branch of `interpretRunResult(...)`). `ERROR` is gated by the runtime-error retry threshold (§3.3.3 below). Cite: `AgentRunLoopImpl.java` (terminal-outcome short-circuit branches in `run(...)`), `PhaseEvaluator.interpretRunResult(...)`.

#### Loop Bounds vs. Session Budgets

`plan.maxToolSteps` (default 4) is **independent** of session budgets (`max_bot_turns`, `max_faq_miss`, `max_clarification`):

- Session budgets are checked by `ControlKernel` **before** the loop runs (existing behavior).
- `maxToolSteps` bounds a single agent run (one user turn). Hitting it produces `TerminalOutcome.MAX_STEPS`, which `PhaseEvaluator.interpretRunResult` maps to ESCALATE with *transition reason* `max_steps_exceeded` and an *escalation reason* derived per plan / session — see the `TerminalOutcome` table above and `PhaseEvaluator.resolveMaxStepsReason(...)`.

#### Runtime ERROR retry threshold (deviation 2026-05-01 — phase0 §0.6)

`PhaseEvaluator.interpretRunResult()` (`server/src/main/java/.../control/PhaseEvaluator.java:489-517`) historically mapped any `AgentRunResult.ERROR` (projection failure, LLM exception, parser failure, tool-dispatch exception, etc.) directly to `ESCALATE` on first occurrence. Transient runtime failures should not become a user-visible escalation on a single hit. Updated mapping rule:

- Maintain a per-session `runtimeErrorCount` counter on `BotSession` (reset on any non-ERROR outcome, see `PhaseEvaluator.interpretRunResult(...)` non-ERROR branches).
- On `AgentRunResult.ERROR` (cite: `PhaseEvaluator.interpretRunResult(...)` ERROR case):
  1. Read the existing count (default 0), **increment** it, and persist back on the session.
  2. If the new count is `< 2` (i.e. this was the **first** consecutive ERROR) → stay in the current phase with transition reason `agent_error_retry`, return the LLM's `finalUserMessage` (or `"Let me try that again."` if absent), **no user-visible escalation**.
  3. Otherwise (count `≥ 2`, i.e. this is the **second consecutive ERROR** or later) → map to `ESCALATE` with transition reason `agent_error` and `escalation_reason = runtime_error_threshold` (one of the canonical 23 escalation_reason values).
- The counter resets implicitly on any non-ERROR outcome because the non-ERROR branches in `interpretRunResult` zero it; an explicit reset on phase transition is not the mechanism in code today.

`MAX_STEPS` (transition reason `max_steps_exceeded`; derived escalation reason per `resolveMaxStepsReason`), `FINAL_ANSWER`, and `ESCALATE` terminal outcomes are unchanged.

#### Example PhasePlans

> **[FOLD-BACK 2026-05-11]** The example yaml blocks below are *illustrative* only — the live `allowedTools` and `maxToolSteps` per phase are emitted by `PhaseEvaluator.plan(...)` and may differ in detail (notably: RESOLVE/INTAKE no longer exposes `create_case_controlled` to the LLM; that tool is `RUNTIME_ONLY` and invoked directly by `ControlKernel.createCaseIfNeeded` on escalation, see "Direct runtime tool invocations" below). The current per-phase plan table is in [`docs/current/runtime_contract.md`](../current/runtime_contract.md) §"Current PhasePlan concept".

**RESOLVE / FAQ (UC-A "Ad Support")**
```yaml
phase: RESOLVE
objective: "Determine what happened to the customer's ad and explain it clearly"
allowedTools: [get_customer_context, search_knowledge, resolve_article, record_outcome, request_handover]
maxToolSteps: 4
validTerminalOutcomes: [FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE]
groundingInstruction: |
  If tool data contains ad/moderation status, answer from that first.
  For policy explanations, cite knowledge source IDs.
```

**INTAKE (UC-H "Ad Removal Appeal")**
```yaml
phase: RESOLVE
objective: "Collect required intake fields and hand over to specialist"
# [FOLD-BACK 2026-05-11] live allowedTools is [request_handover] only.
# create_case_controlled is RUNTIME_ONLY and not LLM-visible; see §3.4.2.
allowedTools: [request_handover]
maxToolSteps: 3
validTerminalOutcomes: [CLARIFICATION_NEEDED, ESCALATE]
```

**CONFIRM**
```yaml
phase: CONFIRM
objective: "Determine whether the user is satisfied with the prior answer"
allowedTools: [record_outcome, request_handover]
maxToolSteps: 2   # [FOLD-BACK 2026-05-11] live value is 2, not 1
# [FOLD-BACK 2026-05-11] live values are [FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE]
# (cite: PhaseEvaluator.plan() CONFIRM branch). The original example used
# [CLOSE, RETRY_RESOLVE, ESCALATE], which were not actual TerminalOutcome
# enum values — CLOSE / RETRY_RESOLVE are post-loop phase transitions
# decided by interpretRunResult, not loop terminals.
validTerminalOutcomes: [FINAL_ANSWER, CLARIFICATION_NEEDED, ESCALATE]
```

**DISCOVER**
```yaml
# [FOLD-BACK 2026-05-11] DISCOVER plan added by Sprint 8.1 §M3 / Phase 2 Fix 3c
# (see §3.4.1.6 classify_use_case below).
phase: DISCOVER
useCase: null   # may be null — by definition not yet committed
objective: "Identify the user's use case so RESOLVE can run on the right plan"
allowedTools: [search_knowledge, classify_use_case]
# [FOLD-BACK 2026-05-11] `search_knowledge` is *plan-projected* on DISCOVER
# (cite: PhaseEvaluator.plan() DISCOVER branch) but **policy-blocked** in the
# normal DISCOVER state where activeUseCase = null. `tool-policy.yaml` allows
# `search_knowledge` only for FAQ UCs (UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP),
# and `ToolPolicyEnforcer.isToolAllowed(...)` denies the call when activeUseCase
# is null (cite: server/.../tools/ToolPolicyEnforcer.java `isToolAllowed` —
# `activeUseCase != null && policy.allowedUcs.contains(activeUseCase)`). The
# only DISCOVER tool that passes both filters today is `classify_use_case`
# (`allowed-ucs: [ALL]`). The projection therefore still surfaces a
# `search_knowledge` schema slot, but an actual call would be rejected at
# dispatch with `TOOL_SCOPE_BLOCKED`. See the plan-vs-policy mismatch
# TODO_DECISION in the §3.3.3 list.
maxToolSteps: 2
# [FOLD-BACK 2026-05-11] live values are [CLARIFICATION_NEEDED, FINAL_ANSWER, ESCALATE]
# (cite: PhaseEvaluator.plan() DISCOVER branch). USE_CASE_IDENTIFIED is
# produced by the loop out-of-band of the plan's declared validTerminalOutcomes
# when classify_use_case commits a UC. See TODO_DECISION below.
validTerminalOutcomes: [CLARIFICATION_NEEDED, FINAL_ANSWER, ESCALATE]
```

#### In-loop guards (delivered)

> **[FOLD-BACK 2026-05-11]** Sprints 6 / 7 / 8.1 / 11 introduced four named in-loop guards now owned by `AgentRunLoopImpl`. Each rejects a specific tool call before dispatch (or short-circuits the loop) without escalating the session, giving the LLM room to self-correct on the next iteration. None of these were enumerated in the original §3.3.3 v9 text; they are stable, code-grounded delivered behavior.

| Guard | Plan scope | What it does | Cite |
|---|---|---|---|
| **S1 FAQ-grounded-resolve** (Sprint 6 §G2) | RESOLVE / FAQ UCs | Rejects `request_handover(faq_miss_threshold_exceeded)` when `search_knowledge` returned viable evidence (`faq_miss = false` + hits) **and** `resolve_article` was not yet attempted, so the bot must read the article it just retrieved before claiming an FAQ miss. | `AgentRunLoopImpl` S1 guard branch; `Sprint6FaqGroundedResolveTest` |
| **Intake-complete guard** (Sprint 7 §I2) | RESOLVE / INTAKE UCs (G/H/I/J/K) | Rejects `request_handover(intake_complete_for_uc_X)` when required intake fields are still missing. Surfaces the missing-field hint via `accumulated_tool_results`. | `AgentRunLoopImpl` intake-complete branch; `IntakeFieldsRegistry` |
| **Progressive resolve guard** (Sprint 11 §M1) | RESOLVE / FAQ UCs | Rejects `record_outcome(outcome_class=resolve)` when the deterministic terminal-evidence condition (`ResolveDispositionEvaluator`) has not been met. Reason: `progressive_resolve_record_outcome_premature`. The loop continues so the LLM can produce the missing evidence or surface the answer first. | `AgentRunLoopImpl.PROGRESSIVE_RESOLVE_GUARD_REJECT_REASON` |
| **`classify_use_case` DISCOVER short-circuit** (Sprint 8.1 §M3) | DISCOVER plan only | A successful `classify_use_case` that commits a non-blank `activeUseCase` returns `TerminalOutcome.USE_CASE_IDENTIFIED` immediately, signalling the kernel to attempt a same-turn DISCOVER → RESOLVE replan. | `ClassifyUseCaseTool.execute(...)`; `AgentRunLoopImpl` post-dispatch branch |

#### Terminal-tool short-circuit semantics (delivered)

> **[FOLD-BACK 2026-05-11]** The handling of `request_handover` and `record_outcome` inside the loop is asymmetric and worth being explicit about; the original §3.3.3 / §3.3.4 text was loose on this.

- A **successful** `request_handover` dispatch sets a `handoverRequested` flag and short-circuits **further LLM iterations**, but only after the *current model response's remaining `tool_calls` batch* has finished dispatching. This means a single LLM response that emitted both `record_outcome` and `request_handover` will execute both before the loop exits with `ESCALATE` **only when both calls pass `ToolDispatcher.validateAgainstPlan`, `ToolPolicyEnforcer`, and the in-loop guards above**. Any call rejected at the plan whitelist, per-UC policy, or guard layer is recorded as a failed `ToolEvent` and skipped; the rest of the batch continues. For example: on the CLOSE plan (`allowedTools = [record_outcome]`) an emitted `request_handover` is rejected as `tool_not_in_plan` and only `record_outcome` runs; conversely on the RESOLVE_INTAKE plan (`allowedTools = [request_handover]`) an emitted `record_outcome` is rejected as `tool_not_in_plan` and only `request_handover` runs (Sprint 9 §O1; cite: `AgentRunLoopImpl.run(...)` step 6d and the post-batch `if (handoverRequested)` return).
- A **failed** `request_handover` (malformed payload, `SalesforceService` failure) does **not** set the flag and does **not** itself short-circuit the loop or stamp a successful terminal handover. The error is recorded into `accumulated_tool_results`; the loop continues until a later step succeeds or `maxToolSteps` is exhausted. This preserves Sprint 9 §O1 terminal-state honesty for the **handover-success** path: a `phase_after = ESCALATE` driven by `handoverRequested` is never stamped from a handover that did not actually transmit. The loop can still exit `ESCALATE` via the orthogonal `MAX_STEPS` → `ESCALATE` mapping in `PhaseEvaluator.interpretRunResult(...)` if the run subsequently exhausts `maxToolSteps`; in that case the escalation reason comes from `resolveMaxStepsReason(...)` (e.g. `clarification_budget_exhausted`, `faq_miss_threshold_exceeded`) rather than from the failed handover call.
- `record_outcome` is **not** a loop-terminating tool. Its result is stamped on session state (`recordOutcomeAttempted`, `recordOutcomeSucceeded`) and accumulated; the loop continues normally. The progressive-resolve guard above is the only **post-validation** path that rejects it without dispatch; `ToolDispatcher.validateAgainstPlan` can also reject `record_outcome` **before** the guard runs on plans that omit it from `allowedTools` (e.g. DISCOVER), surfacing as `tool_not_in_plan` rather than `progressive_resolve_record_outcome_premature`.
- `classify_use_case` is loop-terminating only inside DISCOVER (see guard table above). Outside DISCOVER it would fail the plan whitelist before reaching the tool.

#### Same-turn DISCOVER → RESOLVE replan (delivered)

> **[FOLD-BACK 2026-05-11]** Sprint 8.1 §M3 added a bounded same-turn replan so a DISCOVER turn that commits a UC can produce a RESOLVE answer in the same user turn. The original §3.3.3 v9 text predates this; the live algorithm is summarised below. Authoritative: `ControlKernel.processMessage(...)` §M3 block.

When `AgentRunLoop` returns `TerminalOutcome.USE_CASE_IDENTIFIED` from a DISCOVER plan, `ControlKernel.processMessage(...)` performs **at most one** bounded same-turn replan into RESOLVE before returning the response:

1. Apply the deterministic DISCOVER → RESOLVE phase transition on the session (`session.setCurrentPhase("RESOLVE")`) if `controlPolicy.isValidTransition(...)` allows it. The pre-replan phase tracked for transition logging is also promoted to `RESOLVE` so the post-replan transition (e.g. RESOLVE → CONFIRM on a FAQ FINAL_ANSWER) stays inside the policy graph.
2. **Wall-clock budget gate**: the shared `LlmCallContext` budget (used by both DISCOVER and the would-be RESOLVE call) must have at least `MIN_RESOLVE_REPLAN_BUDGET_MS` remaining. If not, the replan is skipped: the session stays in RESOLVE and the kernel returns the DISCOVER result as a transitional response so the next user turn runs RESOLVE fresh. (The per-invocation HTTP-attempt budget is re-armed at `FallbackLlmClient.chat` entry, so two successful DISCOVER calls do not poison the replan's first attempt — only wall-clock bounds whether the replan can fit.)
3. If the budget is sufficient and `phaseEvaluator.plan(...)` returns a non-null RESOLVE plan, the kernel runs `agentRunLoop.run(resolvePlan, ...)` once more on the same user message and history, then **merges** the DISCOVER and RESOLVE `AgentRunResult`s via `mergeAgentRunResults(...)` so the persisted bot turn captures whatever DISCOVER tool events occurred (in practice `classify_use_case` — `search_knowledge` is plan-projected on DISCOVER but policy-blocked while `activeUseCase` is null, see the DISCOVER plan note above) alongside the RESOLVE tool chain. The merged result becomes the turn's outcome and feeds the post-loop phase transition.
4. If the second `plan(...)` returns null, the kernel falls back to the DISCOVER `USE_CASE_IDENTIFIED` transitional response.

The replan is bounded to a single attempt — there is no recursion into further phase replans within one user turn, and the `USE_CASE_IDENTIFIED` flag is not used outside this exit condition.

#### Direct runtime tool invocations (bypass `ToolDispatcher`)

> **[FOLD-BACK 2026-05-11]** A handful of deterministic Java callers construct a `Tool` bean and invoke its `execute(...)` directly, **not** through `ToolDispatcher.dispatch`. These calls do not run `validateAgainstPlan` or `ToolPolicyEnforcer`; they exist to keep side effects under deterministic Java control rather than letting the LLM order them via a tool call.

- `ControlKernel.createCaseIfNeeded` → `CreateCaseControlledTool.execute(...)` (escalation-time case creation for UC-H/J/K).
- `PhaseEvaluator.createCaseIfAllowed` → `CreateCaseControlledTool.execute(...)` (legacy intake-complete path on the rollback `evaluate(...)` flow).
- `FormContextIngestionService` → `GetCustomerContextTool.execute(...)` (auto-trigger at `SessionManager.createSession(...)` when the pre-chat form provides an `email` and at least one of the session's initial candidate UCs is in the per-UC allow set; see `FormContextIngestionService` and the §"Session create and form ingestion" entry in [`runtime_contract.md`](../current/runtime_contract.md)).

These callers are responsible for their own argument shape and per-UC gating; they intentionally do not project a `PhasePlan` and do not emit `TOOL_SCOPE_BLOCKED`. The LLM has no schema for `create_case_controlled` (RUNTIME_ONLY in `tool-policy.yaml`); the form-driven `get_customer_context` auto-trigger populates `session.customerContext` / `listingContext` / `moderationContext` once at session create, before any LLM call. Cross-turn persistence of LLM-issued tool returns into those session slots is **not** implemented today; an LLM-issued `get_customer_context` surfaces only via `accumulated_tool_results.get_customer_context` on the next iteration of the same turn.

#### Allowed-tool enforcement (delivered)

> **[FOLD-BACK 2026-05-11]** Two enforcement points run on every dispatch through `ToolDispatcher`. The §3.2.6 / §3.3.4 text below pre-dates the per-PhasePlan filter (Sprint 8.1 §M3); the live behavior is summarised here.

1. **Plan whitelist** — `ToolDispatcher.validateAgainstPlan(plan, name)` checks the tool name against `PhasePlan.allowedTools` before the per-UC enforcer runs. A failure becomes a rejected `ToolEvent` with reason `tool_not_in_plan: ...` and the loop continues so the LLM can self-correct.
2. **Per-UC policy** — `ToolPolicyEnforcer` loads `server/src/main/resources/config/tool-policy.yaml` at startup. Each tool has `type` (`AGENT_VISIBLE` | `RUNTIME_ONLY`) and an `allowed-ucs` list (`[ALL]` is the wildcard). On a deny the dispatcher emits a `TOOL_SCOPE_BLOCKED` `BotEvent` and returns an error.

The plan-side filter is also applied to the projection: the `tool_schemas` array surfaced to the LLM is filtered to exactly the plan's `allowedTools` (cite: `ContextProjectionBuilder.build(...)` Sprint 8.1 §M3 schema-filter), so the model never sees tools outside the current `PhasePlan`. **The two filters are independent** — `tool_schemas` is *not* the intersection of plan and policy. For example, the FAQ RESOLVE plan exposes `get_customer_context` for all FAQ UCs, but `tool-policy.yaml` only allows that tool for UC-A, UC-C, UC-D, UC-F, UC-FP, UC-K. On UC-B / UC-E the schema is projected and an actual call would be denied at dispatch with `TOOL_SCOPE_BLOCKED`. See the matching TODO_DECISION block in [`runtime_contract.md`](../current/runtime_contract.md) for the open governance question on whether to intersect, widen the policy, or keep the divergence.

`request_handover` carries an extra coercion: any non-canonical `escalation_reason` is logged WARN and rewritten to `service_degraded` inside `ToolDispatcher.dispatch` before the tool runs (cite: `ToolDispatcher.CANONICAL_ESCALATION_REASONS`, mirrored in `PhaseEvaluator.CANONICAL_ESCALATION_REASONS`).

#### Feature Flag Rollout

Migration is gated by `agent.run-loop.enabled-phases` (config). The flag served as the rollout dial during D16.A–D and is now retained as a **rollback safety net**.

```yaml
agent:
  run-loop:
    # D16.E (current application.yml default): all 6 phases use the AgentRunLoop path.
    enabled-phases: [RESOLVE_FAQ, RESOLVE_INTAKE, DISCOVER, CONFIRM, CLOSE, ESCALATE]
    # Historical rollout (kept for reference):
    #   D16.A: []                           — scaffolding only
    #   D16.B: [RESOLVE_FAQ]                — FAQ first
    #   D16.C: [RESOLVE_FAQ, RESOLVE_INTAKE] — INTAKE added
    #   D16.D: [all 6 routes]               — fully rolled out
    # Rollback: clear or remove individual route keys to revert that phase to the legacy executor.
```

#### Two-Track Execution Model (Honest Truth)

`PhaseEvaluator` carries two parallel execution paths that the runtime selects between via the feature flag:

| Path | Entry Points | When Active | LLM/Tool Calls in PhaseEvaluator? |
|------|-------------|-------------|-----------------------------------|
| **New (planner)** | `plan()` → `interpretRunResult()` | Route key is in `enabled-phases` (current `application.yml` default after D16.E) | None — execution lives in `AgentRunLoop` |
| **Legacy (executor)** | `evaluate()` → `evaluateDiscover` / `resolveFaq` / `resolveIntake` / `evaluateConfirm` / `evaluateClose` / `evaluateEscalate` | Route key NOT in `enabled-phases` (rollback only) | Yes — `llmInvocation.invokeChat`, `toolDispatcher.dispatch`, `createCaseTool.execute` are still wired here |

The legacy methods are **deliberately retained** as a rollback target. Under the current configured route set (all six keys in `enabled-phases`) they are unreached in normal operation, but they become live again the moment a route key is removed from `enabled-phases`. Their continued existence is **not a violation of the architectural intent** — it is the rollback safety net.

> **[FOLD-BACK 2026-05-11]** As of 2026-05-11 the live `application.yml` setting is `agent.run-loop.enabled-phases: [RESOLVE_FAQ, RESOLVE_INTAKE, DISCOVER, CONFIRM, CLOSE, ESCALATE]` — i.e. all six route keys. The kernel's `ControlKernel.computeRouteKey(...)` resolves the per-turn route key as follows: DISCOVER / CONFIRM / CLOSE / ESCALATE map directly to their phase name; RESOLVE maps to `RESOLVE_FAQ` when `activeUseCase ∈ {UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP}`, to `RESOLVE_INTAKE` when `activeUseCase ∈ {UC-G, UC-H, UC-I, UC-J, UC-K}`, and to `null` otherwise (no committed UC, or a UC outside both sets). A `null` route key, an unrecognised key, or a `null` return from `PhaseEvaluator.plan(...)` falls back to the legacy `evaluate(...)` path. The legacy path is therefore rollback-only in normal operation but is also the documented fallback for unconfigured / unknown phase-UC combinations. See [`runtime_contract.md`](../current/runtime_contract.md) §"Run-loop routing" for the live route map.

A future cleanup PR may delete the legacy methods once D16.E has been validated in production for a sustained period (suggested gate: ≥ 2 weeks with no eval regressions and no production rollbacks).

#### Streaming UX (Phase E — out of scope for D16)

> **[FUTURE_DESIGN]** Preserved as forward-looking design. Not implemented today; no SSE/WebSocket transport is wired into the live runtime.

Once `AgentRunLoop` emits a sequence of `AgentMessage` values (ack + progress* + final), an SSE/WebSocket transport can push them to the frontend progressively. This is tracked as a separate workstream; D16 delivers the foundational architecture only.

#### TODO_DECISION

> **[FOLD-BACK 2026-05-11]** The first three items below are mirrored from [`docs/current/runtime_contract.md`](../current/runtime_contract.md) "TODO_DECISION" and that file holds the authoritative text. The remaining items were **identified during this Phase 3 fold-back** and are owned here in §3.3.3 — they are *not* present in `runtime_contract.md` today. A future runtime-contract update may pull them up if/when they require live-runtime governance text; until then, treat this section as the authoritative location for them.

Mirrored from `runtime_contract.md`:

- **Plan-vs-policy mismatch on projected `tool_schemas`** — the per-phase plan filter and the per-UC `tool-policy.yaml` matrix are independent, so the projection can surface tools that `ToolPolicyEnforcer` will deny. Two live instances today: (i) `get_customer_context` is projected on the FAQ RESOLVE plan but the policy only allows it for UC-A / UC-C / UC-D / UC-F / UC-FP / UC-K, so on UC-B / UC-E a call is denied at dispatch with `TOOL_SCOPE_BLOCKED`; (ii) `search_knowledge` is projected on the DISCOVER plan (`PhaseEvaluator.plan()` DISCOVER branch) but the policy only allows it for FAQ UCs, and `ToolPolicyEnforcer.isToolAllowed(...)` denies when `activeUseCase` is null — which is the normal DISCOVER state — so the only DISCOVER tool that actually clears both filters is `classify_use_case`. Decide whether to (a) intersect plan ∩ policy in the projection, (b) widen the policy (e.g. add a "pre-classification" allow set so DISCOVER `search_knowledge` is genuinely callable), or (c) document the divergence in `runtime_contract.md` and keep the current behaviour.
- **Dormant runtime-only tool beans** — `lookup_customer_account`, `lookup_listing_or_ad`, `get_moderation_review_context`, `get_message_moderation_context` are registered as `RUNTIME_ONLY` with `Tool` bean implementations but no live deterministic call site has been identified. Decide whether to wire callers, retire, or document an owner. The composite `get_customer_context` covers the form-ingestion path today.
- **Moderation context written but not projected** — `FormContextIngestionService` populates `session.moderationContext` and `RequestHandoverTool` reads it for handover identifiers, but `ContextProjectionBuilder` does not emit a `moderation_context` projection slot, even though FAQ RESOLVE `PhasePlan.requiredContextKeys` lists it. Decide whether to project, drop the required-key, or formally document as session-internal / handover-only.

Owned by this Phase 3 fold-back:

- **`record_outcome` sink ownership** — the live tool only writes the local `session_outcomes` row. The original §3.4.1.5 design also called for Salesforce `Bot_Session__c.ContainmentOutcome__c` sync, `OUTCOME_RECORDED` to `Bot_Event__c`, an Avro publish to a Kafka analytics topic, and per-sink retry. Decide whether `record_outcome` should grow into the multi-sink owner (option a), whether those sinks belong to a separate kernel-side aggregator analogous to the `ARTICLE_SHOWN` path (option b), or whether the local-only behaviour is the intended end state and the production sink list should be retired (option c). Until decided, treat `record_outcome` as local-only and the v0.3 spec as the source of truth for the live sink set.
- **`ARTICLE_SHOWN` dispatch site** — today `ResolveArticleTool` only returns data. `ControlKernel.recordRunResult(...)` collects `source_id`s from successful `search_knowledge` `hits` (not from `resolve_article`) and emits **one `ARTICLE_SHOWN` event per source ID** via `eventEmitter.emitArticleShown(...)`. Decide whether the event should (a) move into `ResolveArticleTool.execute(...)` to keep tool-level traceability and use `resolve_article` as the trigger, (b) stay in the kernel aggregation path keyed off `search_knowledge` hits as today, or (c) be split (per-tool dispatch event + kernel rollup). The §3.8.1 row has been reconciled to the live emit site; this TODO_DECISION governs whether the live behaviour is the intended end state or should change.
- **`ARTICLE_SHOWN` payload contract** — `EventEmitter.emitArticleShown(...)` writes `{ articleId, title }` and `ControlKernel.recordRunResult(...)` calls it with `(sid, null)`, so the live payload is `articleId = <source id>` and `title = null`. The original §3.8.1 contract said `source_id, canonical_urls[]`. Decide whether to (a) rename / re-shape the payload to `{ source_id, canonical_url }` and look up `canonical_url` from the most recent `search_knowledge` / `resolve_article` hit before emitting, (b) declare the live `{ articleId, title }` shape as the contract and update the §3.8.1 row + any downstream consumers (BigQuery / Looker schemas, admin trace viewer) accordingly, or (c) keep `articleId` but populate `title` from the same hit aggregation so the emitted row is at least non-null. Owner: observability holder; coordinate with the "dispatch site" decision above.
- **`PhasePlan.validTerminalOutcomes` vs out-of-band `USE_CASE_IDENTIFIED`** — `AgentRunLoopImpl` returns `TerminalOutcome.USE_CASE_IDENTIFIED` from a successful DISCOVER-plan `classify_use_case`, but the DISCOVER plan's declared `validTerminalOutcomes` is `[CLARIFICATION_NEEDED, FINAL_ANSWER, ESCALATE]` (no USE_CASE_IDENTIFIED). This is an intentional out-of-band loop terminal — the kernel handles it specially via the same-turn DISCOVER → RESOLVE replan. Decide whether to (a) add `USE_CASE_IDENTIFIED` to the DISCOVER plan's declared set so plan ↔ loop are in sync, or (b) formally document `USE_CASE_IDENTIFIED` as out-of-band and out-of-scope for `validTerminalOutcomes`. Either way the kernel logic is unchanged; this is a contract-shape decision.
- **Runtime-only tools: caller-gated vs `ToolPolicyEnforcer`-gated wording** — the canonical text in `customer_service_tool_spec_v0_2.{md,yaml}` and Phase 3 §3.4.2 says runtime-only tools are gated by `tool_policy_enforcer`, but the live direct-call paths (`ControlKernel.createCaseIfNeeded`, `PhaseEvaluator.createCaseIfAllowed`, `FormContextIngestionService` → `GetCustomerContextTool`) bypass both `ToolDispatcher` and `ToolPolicyEnforcer` and rely on the calling Java code for per-UC gating. Decide whether to (a) standardise on caller-side gating in the docs and remove the `ToolPolicyEnforcer` framing for runtime-only tools, (b) route all runtime-only invocations through `ToolDispatcher` so the enforcer actually runs, or (c) document the split (some runtime-only tools dispatcher-gated, some caller-gated).
- **DISCOVER plan-vs-ESCALATE contract** — DISCOVER's `validTerminalOutcomes` includes `ESCALATE` and its `escalationPolicy` tells the model to escalate when the request is "clearly out of scope" or after a failed clarification, but `allowedTools = [search_knowledge, classify_use_case]` omits `request_handover` — so an LLM-emitted `request_handover` inside the DISCOVER loop is rejected as `tool_not_in_plan` (cite: `PhaseEvaluator.plan()` DISCOVER branch + `ToolDispatcher.validateAgainstPlan`). The only paths that actually reach `ESCALATE` from DISCOVER today are deterministic pre-loop branches (`ControlKernel.forceEscalate(...)` for budget / drift / hard-OOS, the synthesize-handover helper on the legacy `recordTurn(...)` path). Decide whether to (a) add `request_handover` to the DISCOVER `allowedTools` so the LLM-driven escalation path actually works, (b) drop `ESCALATE` from DISCOVER `validTerminalOutcomes` and rewrite the `escalationPolicy` to defer escalation to the deterministic supervisor only, or (c) document the live behaviour explicitly — DISCOVER ESCALATE is supervisor-driven only, the LLM should never call `request_handover` from DISCOVER — and keep the plan as-is.
- **LLM observability fidelity (`llm_call_log` served-model)** — `LlmInvocationService` writes `llm_call_log` rows with `modelName = llmProperties.getDeepseek().getModel()` (captured once at construction), and `LlmResponse` carries no provider/model field, so fallback Kimi success is still persisted as DeepSeek (see §3.8.5 fold-back). The `LLM [chat:request] / [chat:response] / [chat:fallback-engaged]` process logs are the only authoritative record of which provider actually served each attempt. Decide whether to (a) extend `LlmResponse` (and the `LlmClient` contract) to carry `provider` / `model` per attempt and rewrite `LlmCallLogger` to persist that, so `llm_call_log` reflects ground truth, (b) document `llm_call_log.model` as "configured primary, not served provider" and treat the process logs as the canonical fidelity surface, or (c) introduce a separate `LLM_PROVIDER_FALLBACK` event row keyed off `LLM [chat:fallback-engaged]` so the served provider is at least queryable from the DB without changing the existing `llm_call_log` shape.
- **`PhasePlan` projection contract (`requiredContextKeys` / `allowInterimMessage`)** — `ContextProjectionBuilder.build(...)` projects only a subset of `PhasePlan` fields (`phase`, `use_case`, `objective`, `allowed_tools`, `grounding_instruction`, `system_instruction`, `escalation_policy`, `valid_terminal_outcomes`) and does **not** project `requiredContextKeys` or `allowInterimMessage`, nor does it read `requiredContextKeys` to gate what context slots get populated (those slots are emitted whenever the corresponding `BotSession` field is non-empty). The "Moderation context written but not projected" item above is one symptom of this. Decide whether to (a) project `requiredContextKeys` into `phase_plan` and enforce it as a precondition on `build(...)` (failing-loud when a required key is missing), (b) project `requiredContextKeys` as a soft hint and let the projection continue regardless, (c) document `requiredContextKeys` and `allowInterimMessage` as planner-internal metadata only and remove them from the `PhasePlan` record (or rename to make that explicit), or (d) keep the live behaviour and document this section as "PhasePlan has two zones: projected fields and planner-internal fields." Owner: runtime-contract holder.
- **`llm_call_log` coverage contract (retry / provider-attempt rows)** — live behaviour writes `chat`, `routing`, and `rerank` rows for logical app-level LLM invocations (cite: `LlmInvocationService.invokeChat/invokeRouting`, `RerankService.scoreCandidate`); per-turn cardinality of those rows is governed by the separate "per-turn cardinality + non-blocking-routing coverage" decision below. Provider-level retry and primary→fallback attempts inside `OpenAiCompatibleLlmClient` / `FallbackLlmClient` are folded into the single `chat` row for the enclosing invocation and visible only in the `LLM [chat:*]` process logs. The §3.8.6 SQL still lists `'retry'` as a call type but no live logger writes it. Decide whether to (a) widen `LlmCallLogger` to persist a row per provider attempt (and start writing `retry` / `fallback` call types so the DB matches the process-log timeline), (b) declare the live "one row per logical app-level call, no per-attempt rows" coverage as the contract, drop `'retry'` from the SQL comment / instrumented-call-sites table, and rely on process logs for per-attempt fidelity, or (c) introduce a separate `llm_provider_attempt_log` table keyed off the `chat` row's `id` so per-attempt rows live alongside the logical-call row without re-shaping the existing table. Coordinates with the §3.3.3 "LLM observability fidelity" decision above and the per-turn cardinality decision below. Owner: observability holder.
- **`llm_call_log.session_id` foreign-key shape** — the §3.8.6 schema in this doc declares `session_id TEXT NOT NULL REFERENCES bot_sessions(session_id)`, but live `db/migration/V9__create_llm_call_log.sql` is plain `session_id TEXT NOT NULL` with **no FK**. Decide whether to (a) add the FK via a follow-up migration (matching the docs) and accept the cascade / lifecycle implications (sessions may outlive logs, or vice versa, depending on retention policy), (b) keep the live no-FK shape and rewrite the §3.8.6 SQL to match — `llm_call_log` would remain a soft-linked observability table, indexed on `session_id` but without referential integrity, or (c) treat the no-FK shape as a temporary deviation, leave the docs as the production target, and mark the live migration with a `TODO(FK)` comment so the next persistence-layer sweep adds it. Owner: persistence-layer holder.
- **`llm_call_log` per-turn cardinality + non-blocking-routing coverage** — earlier §3.8.6 text framed `chat` as "1 per agent turn" and `routing` as "1 per session init"; both are inaccurate against the live runtime. `LlmInvocationService.invokeChat(...)` is called from `AgentRunLoopImpl.run(...)` **once per loop iteration that reaches the LLM**, so a single user turn that drives the loop through multiple steps writes multiple `chat` rows sharing the same `session_id` / `turn_index`. `LlmInvocationService.invokeRouting(...)` is only reached from `UseCaseRouter.routeViaLlm(...)`; `SessionManager.createSession(...)` uses `UseCaseRouter.routeNonBlocking(...)` (Sprint 8.1 §M0) and never calls it, so today **no `routing` row is normally written at session create**. Decide whether to (a) declare the live "one row per logical LLM invocation, multiple `chat` rows per turn" cardinality as the contract, retire the "1 per session init" routing-row expectation, and rewrite §3.8.6 + the trace-API consumer contracts to query the multi-row shape (e.g. aggregate by `(session_id, turn_index)`), (b) keep `routing` as an inert call-type slot in the live runtime and document a follow-up to re-enable a deterministic post-classification routing logger so the `routing` row reappears (e.g. on `classify_use_case` DISCOVER → RESOLVE commits), or (c) collapse `chat` rows server-side into one row per `(session_id, turn_index)` (folding per-step latency / token counts into a summary) so the surface matches the original "1 per turn" framing. Coordinates with the existing "`llm_call_log` coverage contract" and "LLM observability fidelity" decisions above. Owner: observability holder.
- **`get_customer_context` composite fanout vs direct `GumtreeApiService` shape** — §3.4.1.3 originally described `get_customer_context` as a composite read that orchestrates the `RUNTIME_ONLY` beans (`lookup_customer_account`, `lookup_listing_or_ad`, `get_moderation_review_context`) across bapi-server / user-service / gumshield-api / livead-search chains. Live `GetCustomerContextTool.execute(...)` is a thin single-bean caller of `GumtreeApiService` (`getAccountByEmail`, `getListingByAdId`, conditional `getModerationReview`); it does not dispatch the runtime-only beans and does not chain endpoints. The "dormant runtime-only tool beans" item above governs the registered-but-uncalled beans themselves; this decision governs the **composite-tool contract**. Decide whether to (a) declare the live direct-`GumtreeApiService` shape as the intended end state, retire the runtime-only fanout description (and either retire the dormant beans per the item above or repurpose them for non-`get_customer_context` callers), (b) wire `GetCustomerContextTool` to dispatch the runtime-only beans as originally designed so the composite is restored (which also unblocks per-UC tightening via `tool-policy.yaml` on the inner calls), or (c) keep the direct shape but document the FUTURE_DESIGN composite as the production target gated behind the runtime-only bean cleanup. Owner: tool-spec holder; coordinates with `customer_service_tool_spec_v0_3.md` §`get_customer_context`.

---

### 3.3.4 LLM Tool-Use Contract (V1)

LLM 响应采用 OpenAI-style 原生 tool-use 格式：

```json
{
  "user_message": "...",      // 直接面向用户的回复（可为空）
  "reasoning": "...",          // 内部推理（不展示给用户）
  "tool_calls": [              // 工具调用数组（可为空）
    {"name": "search_knowledge", "arguments": {"query": "..."}},
    {"name": "request_handover", "arguments": {"escalation_reason": "user_distress"}}
  ]
}
```

可调用工具范围由 active_use_case 经 ToolPolicyEnforcer 过滤；agent_visible 工具集见 §3.4.1。

**[DEVIATION 2026-05-01 — 见 phase0 §0.6；原 5-action 抽象层（ask_user / retrieve_knowledge / answer_grounded / escalate_human / finish）已移除。语义动作由 tool_calls 内容隐式表达。]**

> **[FOLD-BACK 2026-05-11]** Delivered behavior, code-grounded:
> - The LLM response is parsed by `ActionParser.parse(...)` into a `ParsedAction` of the form `{ tool_calls: [...], user_message: "...", reasoning: "..." }`. The `reasoning` field is parsed and carried on `ParsedAction` (cite: `runtime/ActionParser.java:65` reading `root.get("reasoning")` and `ParsedAction.@Builder.reasoning`) but is **not** consumed for control flow — there is no 5-action enum and no routing-decision layer, and all control-flow branching in `AgentRunLoopImpl.run(...)` derives from `tool_calls` + `user_message` + session state. `reasoning` is preserved on the parsed action for traceability only.
> - Tool-call validation is two-stage (plan whitelist + per-UC policy) — see "Allowed-tool enforcement (delivered)" in §3.3.3.
> - Argument-shape canonicalisation and legacy-alias acceptance per tool live in [`docs/current/customer_service_tool_spec_v0_3.md`](../current/customer_service_tool_spec_v0_3.md), which supersedes `customer_service_tool_spec_v0_2.{md,yaml}` for what the LLM actually sees today.

### 3.3.5 Control Budgets

| Budget | Value | Rationale |
|--------|-------|-----------|
| `max_clarification_rounds` | **2** | PRD §1.6.2.1 "2 轮内"；eval clarification dataset calibrated to this |
| `max_faq_miss` | **2** | BRD "faq_miss ≥ 2" triggers escalation |
| `max_bot_turns_per_issue` | **15** (FAQ UCs) / **10** (Intake UCs) | FAQ UCs may need retrieval + confirm + retry; Intake UCs are shorter (collect fields → handover) |
| `max_repeated_same_action` | **2** | Prevent infinite loop of same tool call |
| `max_total_bot_turns_before_forced_escalation` | **25** | Combined with Salesforce hard limit of 50 turns (Bot ≤ 25, leave room for handover messaging). Takes `min(25, 50 - overhead)` |
| Re-retrieval attempts after "not resolved" | **1** | CONFIRM → RESOLVE retry; only once with reformulated query |

### 3.3.6 Drift Handling Implementation（v8 HR-calibrated）

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

> **[FOLD-BACK 2026-05-11]** The live tool registry is governed by `server/src/main/resources/config/tool-policy.yaml` and the `Tool` beans under `server/src/main/java/com/gumtree/csagent/service/tools/`. Today's count is **6 AGENT_VISIBLE + 5 RUNTIME_ONLY = 11 tools**, with no `HUMAN_ONLY` type registered. The agent-visible set is `search_knowledge`, `resolve_article`, `get_customer_context`, `request_handover`, `record_outcome`, **`classify_use_case`** (added 2026-05-02; DISCOVER-only via plan, see §3.4.1.6). The runtime-only set is `create_case_controlled` plus four dormant beans (`lookup_customer_account`, `lookup_listing_or_ad`, `get_moderation_review_context`, `get_message_moderation_context`) — see "Dormant runtime-only tools" TODO_DECISION in [`runtime_contract.md`](../current/runtime_contract.md). The authoritative per-tool argument and return contract is [`docs/current/customer_service_tool_spec_v0_3.md`](../current/customer_service_tool_spec_v0_3.md), which **supersedes** the older `customer_service_tool_spec_v0_2.{md,yaml}`. The §3.4.x sections below preserve durable design intent.

### 3.4.1 Agent-Visible Tools — Implementation Detail

#### 3.4.1.1 `search_knowledge`

```
Endpoint:  Internal — KnowledgeRetrievalService.search()
```

**Implementation**:
1. Receive `query` + `use_case_id` + optional `knowledge_scope`
2. Embed query via the configured embedding client. **[FOLD-BACK 2026-05-11]** Live runtime uses `DashScopeEmbeddingClient` with `text-embedding-v3` at 768-dim (cite: `application-local.yml` `llm.dashscope.embedding-model` / `embedding-dimension`, `LlmProperties.DashScopeProperties`). **[FUTURE_DESIGN]** Vertex AI `text-embedding-004` (768-dim) is preserved as the production-design intent for the GCP cutover; not wired today.
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
6. **Query Enrichment** (v9): if user message is short (< 20 chars) or is turn 1, augment search query with `form_context.description` when available — prevents dead-end retrieval on vague messages like "Pls help" when the pre-chat form already contains the real question
7. Rerank top 4–8 via LLM scoring (grounding relevance 1–5 scale) — **parallel execution** (v9): each candidate scored via independent `CompletableFuture`; all candidates scored concurrently, joined with `allOf()`
8. **Answer Gate**: if best `grounding_score < 3.5` → `faq_miss = true`
9. Return top 3 results with `source_id`, `title`, `snippet`, `canonical_url`, `score`

**Failure handling**: `max_retries: 1`. On `no_results` → allow one clarification or escalate. On `knowledge_backend_timeout` → escalate with `service_degraded`.

**FAQ miss with form context fallback** (v9): when `faq_miss = true` but `form_context.description` is non-empty (> 10 chars), instead of the hardcoded "describe your issue in more detail" response, invoke the main LLM with the form description as additional context and instruct it to respond helpfully or escalate. This prevents the bot from asking the user to repeat information they already provided in the pre-chat form.

#### 3.4.1.2 `resolve_article`

> **[FOLD-BACK 2026-05-11]** Delivered shape: the canonical projected schema accepts a single **`source_id: string`** (Sprint 8.2 §M0a); `article_id` is accepted as a legacy alias by `ResolveArticleTool`. Unpublished articles are refused with the deterministic reason `article_unpublished_safe_refuse` (Sprint 14 §L0). The tool emits both `source_url` and `canonical_url` so callers reading either name keep working; `canonical_url_missing` is stamped per hit. See [`docs/current/customer_service_tool_spec_v0_3.md`](../current/customer_service_tool_spec_v0_3.md) §`resolve_article` for the live arg/return contract.

**Implementation (current)**:
1. Receive `source_id` (canonical) or `article_id` (legacy alias) — single string
2. Verify `is_published = true` in `kb_articles` table; on failure return error `article_unpublished_safe_refuse: article '<id>' is not published`
3. Construct customer-safe package: `title` + summary + `description` + `canonical_url` (Help_Site_URL__c) + `source_url` (legacy mirror); stamp `canonical_url_missing` and `safe_to_show`
4. Return the resolved article with `safe_to_show` flag (cite: `ResolveArticleTool.execute(...)` ends with `ToolResult.ok(data)`).

> **[FOLD-BACK 2026-05-11]** `ARTICLE_SHOWN` is **not** emitted by this tool. The event is emitted later by `ControlKernel.recordRunResult(...)`, which collects `source_id`s **only** from successful `search_knowledge` `hits[*].source_id` entries (cite: `ControlKernel.java` aggregation block — `if ("search_knowledge".equals(te.toolName()) && te.success())`) and then emits **one `ARTICLE_SHOWN` event per source ID** via `eventEmitter.emitArticleShown(...)` in a per-id loop (cite: `ControlKernel.recordRunResult(...)` `for (String sid : sourceIds)`). Successful `resolve_article` events do not feed this aggregation today. See TODO_DECISION below for whether the dispatch site should move into the tool itself or stay in the kernel aggregation path.

#### 3.4.1.3 `get_customer_context`

> **[FOLD-BACK 2026-05-11]** Live `GetCustomerContextTool.execute(...)` is a thin, single-bean caller of `GumtreeApiService` — it does **not** orchestrate the runtime-only tool beans (`lookup_customer_account`, `lookup_listing_or_ad`, `get_moderation_review_context`) and does **not** chain multiple bapi-server / gumshield-api / user-service / livead-search endpoints. The composite-fanout "Implementation" steps below are **[FUTURE_DESIGN]** intent preserved for the production topology; they are not the live shape. The live shape is documented in the `[FOLD-BACK]` block immediately following.

**Implementation (current — direct `GumtreeApiService`)**:
1. **Input**: `email` (optional), `ad_id` (optional); at least one is required, otherwise the tool returns `error("At least 'email' or 'ad_id' parameter is required")`.
2. **Account branch** (when `email` is non-blank): one call to `gumtreeApiService.getAccountByEmail(email)`; result is PII-sanitised via `sanitizeAccount(...)` (keeps `account_status`, `account_type`, `creation_date`, `active_ads_count`, `total_ads_count`, `has_verified_email`, `has_verified_phone`; drops raw email / phone / name / address). No bapi-server / user-service / gumshield-api fanout from this tool today.
3. **Listing branch** (when `ad_id` is non-blank): one call to `gumtreeApiService.getListingByAdId(adId)`; result is PII-sanitised via `sanitizeListing(...)`. When the returned listing `status` is `removed` or `moderated`, the tool issues one follow-up call to `gumtreeApiService.getModerationReview(adId)` and emits its raw payload under `moderation_review` (no further sanitisation). Per-UC narrowing (UC-A/FP for moderation review) is not enforced inside the tool today; it is governed upstream by `tool-policy.yaml` `allowed-ucs` and by the form-ingestion auto-trigger's per-UC allow set.
4. **Cross-turn persistence into `session.customerContext` / `listingContext` / `moderationContext`** is populated **only** by the deterministic `FormContextIngestionService` auto-trigger path (see §3.3.3 "Direct runtime tool invocations"). An LLM-issued `get_customer_context` surfaces its return via `accumulated_tool_results.get_customer_context` for the current turn only and does not write back to the session-level context slots — cite: `runtime/AgentRunLoopImpl.run(...)` accumulation block and the absence of session-slot writes in `GetCustomerContextTool.execute(...)`.
5. **Auto-trigger**: `FormContextIngestionService` invokes this tool directly (not through `ToolDispatcher`) at `SessionManager.createSession(...)` when the pre-chat form provides an `email` **and** at least one of the session's initial candidate UCs is in the per-UC allow set. See the runtime-contract §"Session create and form ingestion" entry for the exact gating.

> **[FUTURE_DESIGN — see TODO_DECISION below]** The steps that follow describe the originally intended composite fanout. None of the bapi-server / user-service / gumshield-api / livead-search chains, the `account chain` / `listing chain` UC narrowing, the `must_auto_trigger_on_form_context` schema flag, or the gumshield mock → prod gating below are wired into `GetCustomerContextTool` today; the live shape is the `GumtreeApiService` direct-call summary above. Preserved as the durable production design.
>
> 1. **Input**: `context_type` (account / listing / combined) + identifiers (email / ad_id from form_context or user input)
> 2. **Account chain** (when email available, for UC-A/C/D/F/FP/K):
>    - `GET /api/emails/{email}/user` → bapi-server (resolve email → user/account)
>    - Fallback: `GET /users/{email}/user` → user-service
>    - `GET /api/accounts/{accountId}` → bapi-server (account details)
>    - `GET /api/users/{id}/known-good` → gumshield-api (trust status)
>    - `POST /api/blacklists/email/check` → gumshield-api (blacklist check)
>    - `GET /users/last-logged-in/{userId}` → user-service
> 3. **Listing chain** (when ad_id available, for UC-A/FP/K):
>    - `GET /api/adverts/{id}` → bapi-server
>    - `GET /api/adverts/{id}/features` → bapi-server
>    - `GET /api/adverts/status/{id}` → gumshield-api (moderation status)
>    - `GET /api/adverts/{id}/known-good` → gumshield-api
>    - **For UC-A/FP only**: `POST /api/cs-review/ad-id/` → gumshield-api (moderation review reason via `get_moderation_review_context`)
>    - `GET /api/advert/{advertId}` → livead-search
> 4. **safe_summary** construction: strip PII; compose human-readable summary; store structured context in session state
> 5. **Auto-trigger**: `must_auto_trigger_on_form_context: true` — when session starts with email, automatically run account lookup (before UC routing)
>
> **gumshield mock → prod strategy (v6)** (also FUTURE_DESIGN — not wired in the live tool):
> - Dev/demo: `get_moderation_review_context` returns mock data from `src/test/resources/mock/cs-review-responses.json`
> - Staging: if gumshield service account approved → real API; else → mock with `X-Mock: true` header logged
> - Prod: real API only after service account approval confirmed

#### 3.4.1.4 `request_handover`

> **[FOLD-BACK 2026-05-11]** Delivered shape: `escalation_reason` is required and constrained to a 23-value canonical enum (mirrored in `ToolDispatcher.CANONICAL_ESCALATION_REASONS` and `PhaseEvaluator.CANONICAL_ESCALATION_REASONS`); a non-canonical value is logged WARN and **coerced to `service_degraded`** before the tool runs (Sprint 9 §O0). `summary` is **optional** — when absent, `RequestHandoverTool.deriveFallbackSummary(...)` synthesises a bounded summary from session topic / UC / canonical reason / optional `current_user_message`, and the outbound payload includes `summary_source: "fallback"`. The 23 canonical values, the full outbound payload (including derived `primary_use_case = session.activeUseCase`, which may be null on early forced escalations), and the loop-level short-circuit semantics are documented in [`docs/current/customer_service_tool_spec_v0_3.md`](../current/customer_service_tool_spec_v0_3.md) and §3.3.3 above.
>
> **[FUTURE_DESIGN — Sprint 16 §H0]** A unified `HandoverOrchestrator` is defined in [`docs/proposals/handover_orchestrator_design.md`](../proposals/handover_orchestrator_design.md) as the single owner of handover side effects, idempotent by `session_id`. Today, two paths can independently produce a local/mock handover write (`RequestHandoverTool.execute(...)` → `SalesforceService.requestHandover(...)`, and `SessionManager.recordHandover(...)`); `Sprint16HandoverDualPathReproTest` characterises the current dual-path shape and the disabled `futureInvariant_atMostOneTransmittedHandoverDecisionPerSessionId_disabledUntilOrchestratorLands` test pins the future invariant. Real Salesforce cutover is gated by the orchestrator landing — see `docs/release_gate.md` §1.1.

**Implementation (current — local/mock)**:
1. Validate canonical `escalation_reason`; coerce non-canonical values to `service_degraded` at the dispatcher layer (`ToolDispatcher.dispatch`).
2. Resolve `summary`: use the LLM-supplied value if present; otherwise call `deriveFallbackSummary(...)` and stamp `summary_source: "fallback"`.
3. Build the handover payload (§3.6.2) and call `SalesforceService.requestHandover(sessionId, payload)`. **Cite: `RequestHandoverTool.execute(...)`**.
4. In the local/mock implementation (`MockSalesforceService.requestHandover(...)`) the `transfer_result` is computed deterministically from `mockProperties.isBusinessHours()` (`"transferred"` vs `"offline_logged"`) and the call writes a single `MockHandoverLog` row.
5. Return `transfer_result` plus `session_id`, `escalation_reason`, `summary`, and (when fallback) `summary_source`.

> **[FUTURE_DESIGN — gated by `docs/release_gate.md` §1.1]** The original §3.4.1.4 implementation steps below describe the intended **production** Salesforce side effects. None of the following are wired in the live runtime today: writing `Bot_Context__c` JSON to the Case, calling the Salesforce Omni-Channel Transfer API, queue routing between `CS_NEW_chat` and `CS_Cases_New`, querying real Salesforce agent availability for `is_business_hours`, and emitting `ESCALATION_REQUESTED` from the tool itself. They are preserved as the durable production design and remain blocked behind the `HandoverOrchestrator` cutover (`docs/release_gate.md` §1.1; `docs/proposals/handover_orchestrator_design.md`):
>
> 1. Determine message by `is_business_hours` + `escalation_reason`:
>    - Business hours → `escalation_business_hours` template
>    - Off hours → `escalation_off_hours` template
> 2. Write `Bot_Context__c` to Case (JSON payload).
> 3. Call Salesforce Omni-Channel Transfer API:
>    - Online → route to `CS_NEW_chat` queue
>    - Offline → route to `CS_Cases_New` queue
> 4. Write `ESCALATION_REQUESTED` event from this tool.
> 5. `is_business_hours` check: query Salesforce for "New Chat" queue agent availability. If ≥1 agent online → business hours.

#### 3.4.1.5 `record_outcome`

> **[FOLD-BACK 2026-05-11]** Delivered shape: the canonical projected arg is **`outcome_class: string`** with lowercase enum `resolve | escalate | abandon` (Sprint 9 §O0). The legacy `outcome` arg name and the uppercase past-tense forms (`RESOLVED / ESCALATED / ABANDONED`) are still accepted by `RecordOutcomeTool.normalizeOutcomeClass`; the persisted column in `session_outcomes` keeps the legacy uppercase form so downstream eval/L1 readers are unaffected. `escalation_reason` is required when the normalized class is `escalate`. The progressive-resolve guard (§3.3.3) rejects `outcome_class=resolve` on RESOLVE/FAQ plans before deterministic terminal evidence has been produced.

**Implementation (current)**:
1. Read `outcome_class` (canonical) or `outcome` (legacy alias); normalise via `normalizeOutcomeClass(...)` to lowercase canonical.
2. Require `escalation_reason` if normalised class is `escalate`.
3. Write a single row to the local `session_outcomes` table via `SessionOutcomeRepository.save(...)` (persisted form: uppercase `RESOLVED / ESCALATED / ABANDONED`). **Cite: `RecordOutcomeTool.execute(...)`**.
4. Return `{session_id, outcome_class (canonical lowercase), outcome (persisted uppercase), use_case_id}`.

> **[FUTURE_DESIGN]** The original §3.4.1.5 sink list — Salesforce `Bot_Session__c.ContainmentOutcome__c` update via REST, `OUTCOME_RECORDED` event to `Bot_Event__c`, Avro publish to a Kafka analytics topic, and per-sink retry-on-failure — describes the intended production sink topology and is **not currently wired into `RecordOutcomeTool`**. The tool's live side effect is local-only. Preserved as durable design intent; see TODO_DECISION below for whether this tool should grow into the multi-sink owner or whether those sinks belong elsewhere.

#### 3.4.1.6 `classify_use_case` *(added 2026-05-02 — see phase0 §0.6 deviation)*

**Purpose**: lets the LLM commit a use-case classification once the DISCOVER phase has gathered enough signal. Closes the soft-OOS DISCOVER execution gap surfaced by Phase 2 Step 3c — the only mechanism by which a soft-OOS session can advance into RESOLVE without exhausting the clarification budget.

**Allowed phases**: DISCOVER only (per `PhaseEvaluator.plan()` allowedTools).
**Per-UC visibility**: `ALL` use cases — the tool is policy-allowed before `activeUseCase` is set, since by definition it is the call that sets it.

**Arguments JSON schema**:
```json
{
  "type": "object",
  "properties": {
    "use_case_id": {"type": "string", "enum": ["UC-A","UC-B","UC-C","UC-D","UC-E","UC-F","UC-FP","UC-G","UC-H","UC-I","UC-J","UC-K"]},
    "confidence": {"type": "number", "minimum": 0, "maximum": 1},
    "reasoning":  {"type": "string"}
  },
  "required": ["use_case_id", "confidence"]
}
```

**Implementation**:
1. Validate `use_case_id` against `UseCaseRegistryService.isKnownUseCase(...)` — if invalid return failure with reason `unknown_use_case`.
2. Validate `confidence` is numeric and in `[0, 1]` — else `invalid_confidence`.
3. **Strong-prior carry-forward guard** (Sprint §C2; cite: `ClassifyUseCaseTool.shouldPreserveStrongPrior`): if the session already has an `activeUseCase` matching the deterministic strong-prior / B2 phrase-bias UC for the current form context, refuse to overwrite with a *different* UC. Returns `{committed: false, preserved_use_case_id, rejected_use_case_id, reason: "strong_prior_carry_forward"}`. Idempotent re-classification with the same UC is allowed and still emits the event. Hard-shift drift (UC-G/I/J/H) is pre-empted by `DriftDetector` before the loop runs, so the guard releases on legitimate hard shifts.
4. Set `session.activeUseCase = use_case_id`, `session.intentConfidence = BigDecimal(confidence)` (clamped to scale 2 to fit the column).
5. Persist via `BotSessionRepository.save(session)`.
6. Emit a `CLASSIFICATION_COMMITTED` event row (sessionId, payload `{use_case_id, confidence, reasoning}`).
7. Return success payload `{committed: true, use_case_id, confidence}`.

> **[FOLD-BACK 2026-05-11]** A successful classify_use_case in DISCOVER short-circuits the run loop with `TerminalOutcome.USE_CASE_IDENTIFIED`, which the kernel uses to trigger the bounded same-turn DISCOVER → RESOLVE replan described in §3.3.3. The note in step 6/7 of the original §3.4.1.6 text — "the AgentRunLoop continues" — was correct for the in-loop behaviour around classify_use_case before the same-turn replan landed; the live behaviour is now: *the AgentRunLoop returns USE_CASE_IDENTIFIED, and the kernel may then run a fresh RESOLVE plan in the same user turn if the wall-clock budget allows it*. `PhaseEvaluator.interpretRunResult` continues to map DISCOVER FINAL_ANSWER (no `classify_use_case` issued) into the appropriate transition based on whether `activeUseCase` was committed.

### 3.4.2 Runtime-Only Tools

These are never exposed to LLM prompt. Triggered by runtime logic based on UC policy.

#### `create_case_controlled`

> **[FOLD-BACK 2026-05-11]** Live wiring: `RUNTIME_ONLY` in `tool-policy.yaml` with no LLM-visible schema. The two live deterministic call sites — `ControlKernel.createCaseIfNeeded(...)` (escalation-time case creation) and `PhaseEvaluator.createCaseIfAllowed(...)` (legacy intake-complete path on the rollback `evaluate(...)` flow) — invoke `CreateCaseControlledTool.execute(...)` **directly**, bypassing both `ToolDispatcher.validateAgainstPlan` and `ToolPolicyEnforcer`. The deterministic caller gates the UC set (UC ∈ {UC-H, UC-J, UC-K}); the per-UC required-field validation lives **inside** `CreateCaseControlledTool.execute(...)` (cite: `CreateCaseControlledTool.REQUIRED_FIELDS_BY_UC` + the `missingFields` check at the top of `execute(...)`), so a missing field surfaces as a `ToolResult.error("Missing required fields for ...")` from the tool itself rather than being short-circuited by the caller. `ControlKernel.createCaseIfNeeded(...)` pulls `description` / `email` / `ad_id` best-effort from `BotSession.formContext` and forwards whatever is present; `PhaseEvaluator.createCaseIfAllowed(...)` is analogous. See §3.3.3 "Direct runtime tool invocations". Production Salesforce Case creation is **gated by `docs/release_gate.md` §1.1** (single-owner handover orchestrator before Salesforce cutover) — today the local/mock implementation writes a `MockCase` row via `MockSalesforceService` rather than calling the real Salesforce REST API.

- **Triggered when (live)**: `ControlKernel.createCaseIfNeeded(...)` on escalation for UC-H / UC-J / UC-K; `PhaseEvaluator.createCaseIfAllowed(...)` on the legacy intake-complete rollback path.
- **Validation (live)**: deterministic Java caller checks UC ∈ {UC-H, UC-J, UC-K} before invoking the tool; per-UC required-field validation (§2.10.5) is enforced **inside** `CreateCaseControlledTool.execute(...)` and surfaces as a tool-level `ToolResult.error` when a required field is absent. `ToolPolicyEnforcer` is **not** in this path.
- **API (current)**: local/mock — `MockSalesforceService.createCase(...)` writes a `MockCase` row.
- **API (future, gated by `release_gate.md` §1.1)**: `POST /services/data/vXX.0/sobjects/Case` → Salesforce.
- **Field mapping** (intent — applies to both mock and the future Salesforce call):

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

> **[FOLD-BACK 2026-05-11]** These four tool beans are registered with `type: RUNTIME_ONLY` in `tool-policy.yaml` but have **no live deterministic caller** today. The original §3.4.2 text said they were "internally invoked by `get_customer_context`"; that is not what the live runtime does. `GetCustomerContextTool.execute(...)` calls `GumtreeApiService` (`getAccountByEmail` / `getListingByAdId` / `getModerationReview`) **directly**, not via tool dispatch — the four tool beans are not reached from this path. They are therefore dormant runtime-only beans: present in the registry, agent-invisible by `tool-policy.yaml`, and never triggered. See also the "Dormant runtime-only tools" TODO_DECISION in [`docs/current/runtime_contract.md`](../current/runtime_contract.md).
>
> **[FUTURE_DESIGN]** The original design intent — a composite `get_customer_context` that fans out via tool dispatch to per-API runtime tools (so each leg gets a `ToolEvent` row, scope enforcement, and a uniform trace shape) — is preserved as forward-looking design. The sections below describe that intent and are **not** the live behaviour.

Designed external surface (intent — not how the live runtime is wired): invoked by `get_customer_context` — see §3.4.1.3.

**`get_message_moderation_context`** (v0.2.1; V1 scope confirmed at design time) **[FUTURE_DESIGN / DEFERRED — no live deterministic caller]**:
- **Allowed UCs**: UC-C only
- **Designed trigger**: `get_customer_context` in UC-C messaging diagnostic scenarios (user reports "can't get replies" / "messages not working"). **Live today**: not wired — `GetCustomerContextTool` does not dispatch this bean; UC-C diagnostic resolution does not currently include a message-moderation lookup.
- **API**: `POST /history/moderation/search` → message-moderation-history microservice
- **Purpose**: Determine if user's messages are being blocked by platform safety filters vs. simply unread by recipients
- **Output**: `has_blocked_messages` flag + count + reason summary → feeds into UC-C diagnostic resolution path (Phase 2 §2.6 UC-C-01)

> **TODO_DECISION (dormant runtime-only tools)**: Decide whether `lookup_customer_account`, `lookup_listing_or_ad`, `get_moderation_review_context`, and `get_message_moderation_context` should be (a) **retired** (deleted from the registry / `tool-policy.yaml` because `GetCustomerContextTool` already speaks to `GumtreeApiService` directly), (b) **wired** as the back-end legs of a refactored `GetCustomerContextTool` that dispatches through `ToolDispatcher` so each leg produces its own `ToolEvent` and obeys `ToolPolicyEnforcer`, or (c) **explicitly kept as future design** with a `@Deprecated` annotation and a doc pointer so they do not look like live wiring. Owner: runtime-contract holder. Mirror to [`docs/current/runtime_contract.md`](../current/runtime_contract.md) once decided.

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
  ├─ 1. Embed query → embedding client (768-dim)
  │     [FOLD-BACK 2026-05-11] Live: DashScopeEmbeddingClient + text-embedding-v3.
  │     [FUTURE_DESIGN] Vertex AI text-embedding-004 preserved as production intent.
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
  ├─ 4b. Query Enrichment (v9)
  │     └─ if userMessage.length < 20 OR turnIndex == 0:
  │        enrichedQuery = userMessage + " | Context: " + formContext.description
  │
  ├─ 5. Rerank top 4–8 via LLM grounding judge (1–5 scale)
  │     └─ v9: parallel CompletableFuture per candidate (was serial for-loop)
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
| FAQ UCs (A/B/C/D/E/F/FP) | pgvector retrieval + `source_ids` | Every grounded `user_message` (faq_source_backed mode, no `request_handover` in `tool_calls`) must have ≥1 source_id; eval grader: `groundedness_pass_rate ≥ 98%` |
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
| Tool schemas | `toolspec-v{major}.{minor}` | **Live: [`docs/current/customer_service_tool_spec_v0_3.md`](../current/customer_service_tool_spec_v0_3.md)**, sourced from `server/src/main/resources/config/tool-policy.yaml` + Java tool beans. The original `customer_service_tool_spec_v0_2.yaml` reference is preserved here as the legacy artifact. |
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
| `RETRIEVAL_EXECUTED` | After `search_knowledge` | query (v9: enriched query, not raw userMessage), result_count, top_score, faq_miss, retrieval_latency_ms |
| `ARTICLE_SHOWN` | **[FOLD-BACK 2026-05-11]** Emitted by `ControlKernel.recordRunResult(...)` after a run completes, **one event per `source_id`** drawn from successful `search_knowledge` `hits[*].source_id` entries (cite: `ControlKernel.java` aggregation block — `if ("search_knowledge".equals(te.toolName()) && te.success())` plus `for (String sid : sourceIds)` calling `eventEmitter.emitArticleShown(...)`). Successful `resolve_article` events do not currently feed this aggregation — see the §3.4.1.2 fold-back note. The original "after `resolve_article`" wording is **superseded** | **[FOLD-BACK 2026-05-11]** Live payload (cite: `EventEmitter.emitArticleShown(...)`): `articleId` (string — the source id passed in from the kernel) and `title` (string or null — `ControlKernel.recordRunResult(...)` currently passes `null`, see L2045–2046). The original "source_id, canonical_urls[]" contract is **not** what the emitter writes today; see the ARTICLE_SHOWN payload-contract TODO_DECISION in §3.3.3. |
| `CLARIFICATION_ASKED` | After bot turn where `tool_calls` is empty AND `user_message` contains a question (heuristic: ends with `?` or contains clarifying language). Detected by ControlKernel post-LLM. | clarification_count, question_topic |
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
  "tool_calls": [
    {
      "name": "search_knowledge",
      "arguments": {"query": "why was my ad removed multiple accounts"},
      "latency_ms": 320,
      "status": "success",
      "result_count": 3,
      "faq_miss": false
    }
  ],
  "user_message_present": true,
  "source_ids": ["ka4P200000002hlIAA"],
  "outcome": null,
  "latency_ms": 1850,
  "timestamp": "2026-04-20T14:30:00Z"
}
```

> **Note**: Semantic actions (escalate / clarify / answer / finish) can be derived from `tool_calls` + `user_message` presence; see `phase0_normative_freeze.md` §0.6 deviation. Examples: `tool_calls` contains `request_handover` ⇒ escalate; `tool_calls` empty AND `user_message` non-empty AND ends with `?` ⇒ clarify; `tool_calls` empty AND `user_message` non-empty (statement) ⇒ answer; `tool_calls` contains only `record_outcome` AND no `user_message` ⇒ finish.

### 3.8.3 Event Pipeline

> **[FOLD-BACK 2026-05-11]** Live wiring: `EventEmitter` writes the local `bot_events` table only via `BotEventRepository.save(...)`; `bot_turns` and `session_outcomes` are written by their respective repository call sites. **Salesforce sync (`Bot_Session__c`, `Bot_Event__c`) and Kafka Avro publish are not wired** — the diagram below preserves the durable production sink topology and is gated by the production cutover (`docs/release_gate.md` §1.1).

```text
Bot Runtime
  │
  ├─► PostgreSQL (bot_turns, session_outcomes, bot_events)    — local hot store [LIVE]
  ├─► Salesforce (Bot_Session__c, Bot_Event__c)               — agent-visible + reporting [FUTURE_DESIGN — gated by release_gate.md §1.1]
  └─► Kafka (Avro, analytics topic)                            — data warehouse pipeline [FUTURE_DESIGN — gated by release_gate.md §1.1]
       │
       └─► BigQuery / Looker (downstream)                       — dashboards [FUTURE_DESIGN]
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

### 3.8.5 LLM Provider Identification Logging

All LLM call-sites MUST emit structured INFO-level logs that identify the provider, model, and scenario in use. This enables rapid verification of which LLM backs each feature, especially after provider/model switches (e.g. DashScope → Kimi K2.6) and to distinguish primary vs fallback invocations introduced by the §3.9.1 fallback path.

> **[FOLD-BACK 2026-05-11]** Chat-completion provider lineup: `LlmInvocationService` (server-side agent loop) treats **DeepSeek v4 pro as primary** and **Kimi 2.6 as fallback** per Sprint 8.1 follow-up #2 (cite: `LlmClientConfig.llmClient(...)` constructs `new FallbackLlmClient(deepseekLlmClient, kimiLlmClient, "deepseek", "kimi")`). Embeddings remain DashScope-only (`text-embedding-v3`, 768-dim) and are unaffected. The `eval_interactive` harness (its own user-simulator + judge LLM) also uses DashScope `qwen-plus` and is not in scope. See §3.9.1 "Degraded mode" for the fallback decision logic and the original deviation history.
>
> Operationally this means a single chat turn may produce one or two `LLM [chat:request]` log lines (primary attempt + optional fallback attempt). **[FOLD-BACK 2026-05-11]** The corresponding row in `llm_call_log` does **not** reflect the actually-served model: `LlmInvocationService` captures `modelName = llmProperties.getDeepseek().getModel()` once at construction (cite: `LlmInvocationService` constructor — Sprint 8.1 follow-up #2 comment "Telemetry tag for the modelName surface points at the primary's model so trace metadata reflects the model that ran most often. Fallback model leaks into per-call client logs already.") and writes that same `modelName` into every `llmCallLogger.log(...)` call for both primary success and fallback success. `LlmResponse` carries no provider/model field today (cite: `model/LlmResponse.java` — only `content`, `finishReason`, `promptTokens`, `completionTokens`, `latencyMs`), so the per-call client cannot tell the logger which provider actually served the request. The authoritative source for "which provider served this attempt" is the `LLM [chat:request] / [chat:response] / [chat:fallback-engaged]` process-log lines on `OpenAiCompatibleLlmClient` / `FallbackLlmClient`. See the LLM observability fidelity TODO_DECISION in §3.3.3.

#### Startup log (once per boot)

On application startup, `LlmClientConfig.llmClient(...)` logs the resolved provider lineup via `LlmConfigValidator.describe(...)` (the validator reports key presence as `present | placeholder | blank` and never logs the secret itself):

```
INFO  LLM provider lineup: <describe(props) output — deepseek primary, kimi fallback, dashscope embedding-only>
```

> **[FOLD-BACK 2026-05-11]** Live source-of-truth is `LlmClientConfig.java` line `log.info("LLM provider lineup: {}", LlmConfigValidator.describe(props));`. The earlier `LLM config loaded:` line documented here previously did not match any live log; the live startup line is `LLM provider lineup:`.

#### Per-request logs (every LLM call)

| Layer | Log pattern | Level | Key fields |
|-------|-------------|-------|------------|
| `OpenAiCompatibleLlmClient` (request) | `LLM [chat:request] provider={} model={} url={} attempt={}` | INFO | provider (`deepseek` / `kimi` / `dashscope`), model name, full endpoint URL, retry attempt index (1-based) |
| `OpenAiCompatibleLlmClient` (response) | `LLM [chat:response] provider={} model={} attempt={} latency={}ms tokens={}/{}` | INFO | provider, model, attempt, latency, prompt/completion tokens |
| `OpenAiCompatibleLlmClient` (retry / non-retryable) | `LLM [chat:retry] ...` / `LLM [chat:non-retryable-status] ...` / `LLM [chat:exhausted] ...` | WARN / ERROR | provider, attempt, status code, elapsed, retry decision |
| `FallbackLlmClient` (fallback engagement) | `LLM [chat:fallback-engaged] primary={} failed transiently (...); ...` then `LLM [chat:fallback-success] fallback={} succeeded ...` or `LLM [chat:fallback-failed] both primary={} and fallback={} failed` | WARN / INFO / ERROR | primary label (`deepseek`), fallback label (`kimi`), failure class for engagement |
| `LlmInvocationService` | `LLM [chat] response: latency={}ms, tokens={}/{}` / `LLM [routing] response: latency={}ms` | INFO | scenario tag, latency, token counts |
| `RerankService` | `LLM [rerank] scored {} candidates, top={}` | INFO | candidate count, top score |
| `DashScopeEmbeddingClient` | `Embedding request: provider=DashScope, model={}, texts={}` | INFO | model, batch size |

> **TODO_DECISION (LLM logging contract source-of-truth)**: The live patterns above are emitted from `OpenAiCompatibleLlmClient` / `FallbackLlmClient` / `LlmInvocationService` and are the authoritative source. Decide whether this §3.8.5 table should (a) continue to mirror the live patterns by hand and accept fold-back drift each time the code log lines change, or (b) point at the code as source-of-truth and keep only the *contract* (which call sites MUST log provider + model + attempt + outcome) in this section. Owner: observability holder.

#### Python eval_interactive logs

| Component | Log pattern | Level |
|-----------|-------------|-------|
| `UserSimulator` | `LLM [simulator] call: model={}, base_url={}` | INFO |
| `LlmJudge` | `LLM [judge:{}] call: model={}, base_url={}` | INFO |

#### Verification

Quick verification command after deployment or config change:

```bash
grep "LLM \[chat:request\]" /tmp/csagent.log | head -5    # Which provider/model/attempt per call
grep "LLM provider lineup:" /tmp/csagent.log              # Startup provider lineup (deepseek primary, kimi fallback)
grep "LLM \[chat:fallback-engaged\]" /tmp/csagent.log     # When the primary fell through to the fallback
```

### 3.8.6 LLM Call Log (v9 — Full Observability)

Problem statement: the existing trace system only records the **final** LLM response per turn (in `bot_turns.llm_raw_response`). Routing calls and rerank calls (N per turn) are completely invisible — creating a major observability gap for the non-chat LLM invocations.

**Solution**: dedicated `llm_call_log` table that records **logical app-level LLM calls** (one row per `routing` / `chat` / `rerank` invocation).

> **[FOLD-BACK 2026-05-11]** Coverage contract — live behaviour: `llm_call_log` records **one row per logical app-level LLM invocation**, across three logger-emitted `call_type` values:
> - **`chat`** — one row per `LlmInvocationService.invokeChat(...)` call. This is **per `AgentRunLoopImpl.run(...)` iteration**, not per user turn: a single user turn that drives the loop through multiple steps (e.g. `search_knowledge` → `resolve_article` → final answer) produces **multiple `chat` rows for the same `session_id` / `turn_index`**, one per loop step that actually reached the LLM. Provider-level retry and primary→fallback attempts inside `OpenAiCompatibleLlmClient` / `FallbackLlmClient` are folded into the single `chat` row for that invocation and surface only in the `LLM [chat:request] / [chat:retry] / [chat:fallback-engaged] / [chat:response]` process logs.
> - **`routing`** — one row per `LlmInvocationService.invokeRouting(...)` call. The only live call site is `UseCaseRouter.routeViaLlm(...)`, reached on the **blocking** route path (`UseCaseRouter.route(..., allowLlm=true)`). The session-init create path (`SessionManager.createSession(...)`) explicitly takes `UseCaseRouter.routeNonBlocking(...)` (Sprint 8.1 §M0), which short-circuits before `routeViaLlm`, so **no `routing` row is written at session create today**. The earlier "1 per session init" wording is incorrect; see the TODO_DECISION below for whether `routing` is now an inert call-type slot in the live runtime.
> - **`rerank`** — one row per `RerankService.scoreCandidate(...)` call (typically 4–8 per `search_knowledge` invocation).
>
> The earlier "records every LLM call regardless of call site" wording overstates this. See the call-log coverage TODO_DECISION in §3.3.3 and the new per-turn cardinality / non-blocking-routing TODO_DECISION below.

```sql
CREATE TABLE llm_call_log (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          TEXT NOT NULL,                -- [FOLD-BACK 2026-05-11] live migration V9 has no FK to bot_sessions
    turn_index          INT,                          -- NULL for routing (pre-turn)
    call_type           TEXT NOT NULL,                -- live: 'routing' | 'chat' | 'rerank'
    model               TEXT NOT NULL,                -- e.g. 'kimi-k2.6' — see §3.8.5 fold-back: this is the configured primary, not the actually-served provider
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

> **[FOLD-BACK 2026-05-11]** Live `server/src/main/resources/db/migration/V9__create_llm_call_log.sql` defines `session_id TEXT NOT NULL` with **no foreign key** to `bot_sessions(session_id)`. The `REFERENCES bot_sessions(session_id)` clause shown above is the **[FUTURE_DESIGN]** intent and is **not** in the live migration; see the FK shape TODO_DECISION in §3.3.3. Similarly, `'retry'` is preserved as a forward-looking call-type slot but is **not** written by any live logger today.

**Instrumented call sites** (live):

| Call Site | `call_type` | `turn_index` | Notes |
|-----------|-------------|--------------|-------|
| `LlmInvocationService.invokeRouting()` | `routing` | NULL | One row per blocking `UseCaseRouter.routeViaLlm(...)` (cite: `LlmInvocationService.java:266`). **Not** called from `SessionManager.createSession(...)`: that path uses `UseCaseRouter.routeNonBlocking(...)` and short-circuits before any LLM call (Sprint 8.1 §M0), so today no `routing` row is normally produced — see the §3.3.3 "`llm_call_log` per-turn cardinality + non-blocking-routing coverage" TODO_DECISION |
| `LlmInvocationService.invokeChat()` | `chat` | current turn | One row **per `AgentRunLoopImpl.run(...)` loop iteration** that reaches the LLM, not per user turn (cite: `LlmInvocationService.java:114`, `AgentRunLoopImpl.run(...)` step 2 inside the per-step loop). A single user turn driving multiple loop steps produces multiple `chat` rows sharing the same `session_id` / `turn_index`. Provider retry / fallback attempts are **not** separate `chat` rows — they are folded into the single `chat` row for that invocation and visible only in process logs |
| `RerankService.scoreCandidate()` | `rerank` | current turn | N per search (4–8 candidates) (cite: `RerankService.java:160`) |
| **[FUTURE_DESIGN]** retry / per-provider attempt rows | `retry` | current turn | Not written by any live logger today; preserved as the v9 aspirational coverage |

**Trace API endpoint**:

```yaml
GET /v1/demo/sessions/{id}/llm-calls
Response:
  - id: 1
    session_id: "abc"
    turn_index: null
    call_type: "routing"
    model: "kimi-k2.6"
    prompt_tokens: 168
    completion_tokens: 132
    latency_ms: 2795
    request_summary: "Classify the user's issue..."
    response_summary: "UC-B, confidence=0.85"
    success: true
  - id: 2
    session_id: "abc"
    turn_index: 1
    call_type: "rerank"
    ...
```

**Admin UI integration**: TraceViewer gains an "LLM Calls" tab showing all calls per session, with per-call type/model/latency/tokens breakdown and aggregate totals.

### 3.8.7 Technology Stack

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

> **[FOLD-BACK 2026-05-11]** The original cells in this table mix delivered behaviour with future Salesforce-cutover behaviour and pre-date the Sprint 8.1 follow-up #2 LLM provider reversal. The fold-back markers below correct the live claims; durable production-design intent is preserved verbatim under each row.

| Requirement | Implementation |
|-------------|---------------|
| Handover request retryable | **[FUTURE_DESIGN — gated by `docs/release_gate.md` §1.1]** `request_handover` retries once on Salesforce API failure; if still fails → return `failed` status + log alert; degrade to "please contact us again" message. **Live today**: `RequestHandoverTool` calls `SalesforceService.requestHandover(...)`; the local `MockSalesforceService` writes a single `MockHandoverLog` row and returns `transferred` / `offline_logged` based on `mockProperties.isBusinessHours()`. There is no real Salesforce retry path because there is no real Salesforce client wired yet. See §3.4.1.4. |
| State write verifiable | **[FOLD-BACK 2026-05-11]** PostgreSQL writes are attempted via Spring Data JPA `…Repository.save(...)` (e.g. `BotSessionRepository.save(...)`, `BotEventRepository.save(...)`, `BotTurnRepository.save(...)`) within the surrounding `@Transactional` boundary. Core write entities (`BotTurn`, `BotEvent`, `SessionOutcome`) use **application-assigned** IDs (cite: `BotTurn.@Id turnId`, `BotEvent.@Id eventId`, `SessionOutcome.@Id sessionId` — no `@GeneratedValue`), so the JPA flush does not produce a generated-id round-trip. No `RETURNING` clause exists in `server/src/main/resources/db/migration/*.sql` or in runtime write paths, and **no explicit row-echo verification exists today** beyond JDBC's reported affected-row count. The original "PostgreSQL transaction with `RETURNING` clause" wording is preserved here as the durable design intent (explicit DB-level verification of the inserted row) but is **not** what the live runtime does. **[FUTURE_DESIGN — gated by `docs/release_gate.md` §1.1]** Salesforce write verified via HTTP 201 response — no real Salesforce write path exists in the live runtime; the mock returns deterministic strings rather than HTTP-201 verification. |
| Degraded mode | **[FOLD-BACK 2026-05-11]** Live wiring is **DeepSeek v4 pro primary + Kimi 2.6 fallback** (cite: `LlmClientConfig.llmClient(...)` constructs `new FallbackLlmClient(deepseekLlmClient, kimiLlmClient, "deepseek", "kimi")` per Sprint 8.1 follow-up #2). The transient-failure classes catalogued below still describe the trigger conditions for fallback engagement; only the primary/fallback identity is reversed. **Provider-chain failure does NOT surface as `AgentRunResult.ERROR` today**: `AgentRunLoopImpl` catches `LlmDeadlineExceededException` → `TerminalOutcome.DEADLINE_EXCEEDED` and `LlmUnavailableException` → `TerminalOutcome.LLM_UNAVAILABLE`, both of which `PhaseEvaluator.interpretRunResult(...)` (Sprint 8.1 §M2) intentionally **does not escalate** — the session stays in the current phase with `shouldEndChat=false` so the user can retry on their next message, rather than producing a fake business handover that hides the real infra failure. The `runtime_error_threshold` ESCALATE path is reached only when the loop produces `ERROR` (null plan, projection-build exception, or generic LLM-invocation exception) **twice consecutively** — see §3.3.3 "Runtime ERROR retry threshold". The original 2026-05-01 deviation text below is preserved as the historical decision log; treat the live `LlmClientConfig.llmClient(...)` constructor and the §3.3.3 ERROR / DEADLINE_EXCEEDED / LLM_UNAVAILABLE behaviour as authoritative. **Two-tier fallback (deviation 2026-05-01 — phase0 §0.6, superseded by Sprint 8.1 follow-up #2)**: Server-side chat completion originally had primary = Kimi 2.6, fallback = DeepSeek v4 pro. `LlmInvocationService` (or its underlying client wrapper) catches a defined set of transient failure classes from the primary call — timeout, HTTP 5xx, rate-limit (HTTP 429), and provider-specific connection-reset / circuit-broken exceptions (the exact exception classes are enumerated by the implementer based on the actual client surface) — and automatically retries the same prompt against the fallback with the same tool schema. Non-transient errors (4xx other than 429, parse failures, prompt-too-long) skip the fallback and surface immediately. After the threshold is exceeded the user-facing message remains "I'm having trouble right now — let me connect you to the team." (`escalation_reason = service_degraded` is retained for non-LLM degraded paths.) Fallback is for chat completions only — embeddings (`DashScopeEmbeddingClient`) are unaffected, and the Python `eval_interactive` harness's own LLM (DashScope `qwen-plus` per `eval_interactive.yaml`) is out of scope. Provider configuration: `application-local.yml` `llm.kimi` + `llm.deepseek` blocks; env vars `KIMI_*` and `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` (default `https://api.deepseek.com/v1`) / `DEEPSEEK_MODEL` (default `deepseek-v4-pro`). DeepSeek's API is OpenAI-compatible, so the implementer can either reuse `OpenAiCompatibleLlmClient` with swapped base-url/key/model or introduce a thin `DeepSeekClient` wrapper. |
| No data loss on crash | Session state persisted to PostgreSQL on every turn completion is delivered. **[FUTURE_DESIGN — gated by `docs/release_gate.md` §1.1]** "Salesforce sync is async but has retry queue" describes the future production sync path; no Salesforce sync queue is wired in the live runtime today. |
| Idempotent message handling | Dedup on `(session_id, turn_index)` unique constraint; replay-safe |

> **TODO_DECISION (State-write verification semantics)**: The original "PostgreSQL transaction with `RETURNING` clause" wording specified DB-level row-echo verification, but no `RETURNING` clause exists today in either migrations or runtime write paths — all writes use Spring Data JPA `Repository.save(...)` with application-assigned IDs on `BotTurn` / `BotEvent` / `SessionOutcome` (no `@GeneratedValue`, so no generated-id round-trip either). Decide whether (a) the "verifiable write" contract should be reframed to "JPA-managed write inside `@Transactional` with no explicit row-echo verification" (treat the live behaviour as the contract), (b) introduce explicit `RETURNING` semantics on critical write paths (`bot_sessions`, `bot_events`, `bot_turns`, `session_outcomes`) where row-echo verification is genuinely needed, or (c) keep `RETURNING` as forward-looking intent for the production Salesforce-cutover write path only. Owner: persistence-layer holder.

### 3.9.2 Performance

> **[FOLD-BACK 2026-05-11]** The FAQ-answer p95 budget and the escalation-request p95 budget include Salesforce write / Transfer API contributions that are **not wired in the live runtime today** (production Salesforce cutover gated by `docs/release_gate.md` §1.1; live handover writes a `MockHandoverLog` row only — see §3.4.1.4). The targets are preserved as durable production-design intent; today's live latency profile excludes the Salesforce contribution.

| Metric | Target | Enforcement |
|--------|--------|-------------|
| FAQ answer e2e p95 | ≤ 5s | Includes: query embedding (~100ms) + pgvector search (~50ms) + rerank (~700ms parallel, was ~5s serial pre-v9) + LLM generation (~2–3s) **+ Salesforce write (~500ms) [FUTURE_DESIGN — gated by `release_gate.md` §1.1]** |
| Escalation request e2e p95 | ≤ 3s | **[FUTURE_DESIGN — gated by `release_gate.md` §1.1]** Salesforce Transfer API + payload write. **Live today**: `RequestHandoverTool` → `MockSalesforceService.requestHandover(...)` writes one `MockHandoverLog` row; no real Salesforce Transfer API call. |
| Median turns for solved FAQ | ≤ 6 | Control budget enforcement |
| pgvector query latency p95 | ≤ 100ms | HNSW index with ef_search=100; 218 articles / ~500 chunks is small dataset |
| Embedding latency p95 | ≤ 200ms | Single query embedding (768-dim). **[FOLD-BACK 2026-05-11]** Live runtime: `DashScopeEmbeddingClient` with `text-embedding-v3`. **[FUTURE_DESIGN]** Vertex AI `text-embedding-004` preserved as the production-cutover intent; target latency applies to whichever provider is configured. |
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
| Bounded tool surface | **6 agent-visible** (Sprint 2026-05-02 added `classify_use_case`) **+ 5 runtime-only** (1 active deterministic caller + 4 dormant — see [`runtime_contract.md`](../current/runtime_contract.md) TODO_DECISION); no tool added without eval-first |
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
| `bot` | `bot_sessions`, `bot_turns`, `session_outcomes`, `llm_call_log` | Bot session state + turn log + outcome tracking + LLM call audit (v9) |
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
| Tool implementation specs (live: **6 agent-visible + 5 runtime-only + 0 human-only = 11 tools**, + 4 capabilities; original Phase 3 handoff line read "5 + 5 + 2") | ✅ §3.4 — see also [`docs/current/customer_service_tool_spec_v0_3.md`](../current/customer_service_tool_spec_v0_3.md) |
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
