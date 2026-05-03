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

> **Background**: The original V1 design merged turn supervision, phase logic, and tool execution into `ControlKernel + PhaseEvaluator`. As tool surface grew, this became leaky: LLM-requested tools (e.g., `get_customer_context` after the user supplies an ad ID) were parsed and recorded in trace, but **never executed**. Pre-planned tools that PhaseEvaluator hardcoded (e.g., `search_knowledge` in FAQ phase) worked, but generic LLM tool-use did not.
>
> **v9 Decision**: Introduce `AgentRunLoop` as a dedicated model↔tool execution worker. Refactor `PhaseEvaluator` from *executor* to *planner* (returns a `PhasePlan`). Keep `ControlKernel` as the deterministic supervisor.

#### Component Boundaries

| Component | Owns | Does NOT |
|-----------|------|----------|
| `ControlKernel` | Deterministic turn lifecycle: budget, drift, transitions, persistence, response packaging | Talk to LLM, dispatch tools, decide phase content |
| `PhaseEvaluator` | Phase **planner** — returns `PhasePlan`. After run: `interpretRunResult()` decides phase transition. | Call LLM directly, dispatch tools directly, build context JSON |
| `AgentRunLoop` | Mechanical model↔tool loop, bounded by `plan.maxToolSteps` | Decide business phase, validate phase transitions, persist turns |
| `ContextProjectionBuilder` | Pure projection assembly. Called by `AgentRunLoop` every iteration. Reads `requiredContextKeys` from PhasePlan. | Decide what to include — that's the plan's job |
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

The full `PhasePlan` is **injected into the projected context** so the LLM understands its mission. `maxToolSteps` is enforced server-side and not shown to the model.

#### `AgentRunResult` Contract

```java
public record AgentRunResult(
    List<AgentMessage> messages,                     // 1..N: ack? + progress* + final
    List<ToolEvent> toolEvents,                      // every tool call with input/output/latency
    List<LlmCallEvent> llmEvents,                    // every LLM invocation in the loop
    TerminalOutcome terminalOutcome,                 // FINAL_ANSWER | ESCALATE | MAX_STEPS | ERROR
    String finalUserMessage,
    Optional<String> escalationReason
) {}
```

#### Loop Bounds vs. Session Budgets

`plan.maxToolSteps` (default 4) is **independent** of session budgets (`max_bot_turns`, `max_faq_miss`, `max_clarification`):

- Session budgets are checked by `ControlKernel` **before** the loop runs (existing behavior).
- `maxToolSteps` bounds a single agent run (one user turn). Hitting it produces `TerminalOutcome.MAX_STEPS`, which `PhaseEvaluator.interpretRunResult` maps to escalation with reason `agent_max_steps_exceeded`.

#### Runtime ERROR retry threshold (deviation 2026-05-01 — phase0 §0.6)

`PhaseEvaluator.interpretRunResult()` (`server/src/main/java/.../control/PhaseEvaluator.java:489-517`) historically mapped any `AgentRunResult.ERROR` (projection failure, LLM exception, parser failure, tool-dispatch exception, etc.) directly to `ESCALATE` on first occurrence. Transient runtime failures should not become a user-visible escalation on a single hit. Updated mapping rule:

- Maintain a per-session, per-phase `runtimeErrorCount` counter on `BotSession` (a small counter field; reuse an existing per-phase counter if grep on the session class shows one — confirm before final implementation).
- On `AgentRunResult.ERROR`:
  - If `runtimeErrorCount` in the **current phase** is `< 2` → increment, **stay in current phase**, re-prompt next turn (no user-visible escalation).
  - If `runtimeErrorCount` in the current phase is `≥ 2` → map to `ESCALATE` with `escalation_reason = runtime_error_threshold` (new value added to the canonical 22-value escalation_trigger enum, expanding it to 23).
- `runtimeErrorCount` resets on phase transition.

`MAX_STEPS` (`agent_max_steps_exceeded`), `FINAL_ANSWER`, and `ESCALATE` terminal outcomes are unchanged.

#### Example PhasePlans

**RESOLVE / FAQ (UC-A "Ad Support")**
```yaml
phase: RESOLVE
objective: "Determine what happened to the customer's ad and explain it clearly"
allowedTools: [get_customer_context, search_knowledge, resolve_article, request_handover]
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
allowedTools: [request_handover, create_case_controlled]   # NO search_knowledge / resolve_article
maxToolSteps: 2
validTerminalOutcomes: [CLARIFICATION_NEEDED, ESCALATE]
```

**CONFIRM**
```yaml
phase: CONFIRM
objective: "Determine whether the user is satisfied with the prior answer"
allowedTools: [record_outcome, request_handover]
maxToolSteps: 1
validTerminalOutcomes: [CLOSE, RETRY_RESOLVE, ESCALATE]
```

#### Feature Flag Rollout

Migration is gated by `agent.run-loop.enabled-phases` (config). The flag served as the rollout dial during D16.A–D and is now retained as a **rollback safety net**.

```yaml
agent:
  run-loop:
    # D16.E (current production default): all 6 phases use the AgentRunLoop path.
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
| **New (planner)** | `plan()` → `interpretRunResult()` | Route key is in `enabled-phases` (production default after D16.E) | None — execution lives in `AgentRunLoop` |
| **Legacy (executor)** | `evaluate()` → `evaluateDiscover` / `resolveFaq` / `resolveIntake` / `evaluateConfirm` / `evaluateClose` / `evaluateEscalate` | Route key NOT in `enabled-phases` (rollback only) | Yes — `llmInvocation.invokeChat`, `toolDispatcher.dispatch`, `createCaseTool.execute` are still wired here |

The legacy methods are **deliberately retained** as a rollback target. They are dead code in production after D16.E but become live again the moment a route key is removed from `enabled-phases`. Their continued existence is **not a violation of the architectural intent** — it is the rollback safety net.

A future cleanup PR may delete the legacy methods once D16.E has been validated in production for a sustained period (suggested gate: ≥ 2 weeks with no eval regressions and no production rollbacks).

#### Streaming UX (Phase E — out of scope for D16)

Once `AgentRunLoop` emits a sequence of `AgentMessage` values (ack + progress* + final), an SSE/WebSocket transport can push them to the frontend progressively. This is tracked as a separate workstream; D16 delivers the foundational architecture only.

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
6. **Query Enrichment** (v9): if user message is short (< 20 chars) or is turn 1, augment search query with `form_context.description` when available — prevents dead-end retrieval on vague messages like "Pls help" when the pre-chat form already contains the real question
7. Rerank top 4–8 via LLM scoring (grounding relevance 1–5 scale) — **parallel execution** (v9): each candidate scored via independent `CompletableFuture`; all candidates scored concurrently, joined with `allOf()`
8. **Answer Gate**: if best `grounding_score < 3.5` → `faq_miss = true`
9. Return top 3 results with `source_id`, `title`, `snippet`, `canonical_url`, `score`

**Failure handling**: `max_retries: 1`. On `no_results` → allow one clarification or escalate. On `knowledge_backend_timeout` → escalate with `service_degraded`.

**FAQ miss with form context fallback** (v9): when `faq_miss = true` but `form_context.description` is non-empty (> 10 chars), instead of the hardcoded "describe your issue in more detail" response, invoke the main LLM with the form description as additional context and instruct it to respond helpfully or escalate. This prevents the bot from asking the user to repeat information they already provided in the pre-chat form.

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
3. Set `session.activeUseCase = use_case_id`, `session.intentConfidence = BigDecimal(confidence)`.
4. Persist via `BotSessionRepository.save(session)`.
5. Emit a `CLASSIFICATION_COMMITTED` event row (sessionId, payload `{use_case_id, confidence, reasoning}`).
6. Return success payload `{committed: true, use_case_id, confidence}`. The AgentRunLoop continues — the LLM can then call `search_knowledge` or emit a final user_message; `PhaseEvaluator.interpretRunResult` already transitions DISCOVER → RESOLVE on FINAL_ANSWER once `activeUseCase != null`.

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
| `RETRIEVAL_EXECUTED` | After `search_knowledge` | query (v9: enriched query, not raw userMessage), result_count, top_score, faq_miss, retrieval_latency_ms |
| `ARTICLE_SHOWN` | After `resolve_article` | source_ids[], canonical_urls[] |
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

### 3.8.5 LLM Provider Identification Logging

All LLM call-sites MUST emit structured INFO-level logs that identify the provider, model, and scenario in use. This enables rapid verification of which LLM backs each feature, especially after provider/model switches (e.g. DashScope → Kimi K2.6) and to distinguish primary vs fallback invocations introduced by the §3.9.1 Kimi → DeepSeek fallback path.

> **Chat-completion provider lineup (deviation 2026-05-01 — phase0 §0.6)**: `LlmInvocationService` (server-side agent loop) treats Kimi 2.6 as **primary** and DeepSeek v4 pro as **fallback** for chat completions. Embeddings remain DashScope-only (`text-embedding-v3`, 768-dim) and are unaffected. The `eval_interactive` harness (its own user-simulator + judge LLM) also uses DashScope `qwen-plus` and is not in scope. See §3.9.1 "Degraded mode" for the fallback decision logic.
>
> Operationally this means a single chat turn may produce one or two `LLM request:` log lines (primary attempt + optional fallback attempt), and the corresponding row(s) in `llm_call_log` will record the actual model that served the request (`kimi-k2.6` vs `deepseek-v4-pro`).

#### Startup log (once per boot)

On application startup, `OpenAiCompatibleLlmClient` logs the resolved LLM configuration. After the deviation 2026-05-01 fallback work lands, the line MUST include the DeepSeek fallback block alongside the existing primaries:

```
INFO  LLM config loaded: kimi=[model=kimi-k2.6, baseUrl=https://api.moonshot.ai/v1] (primary chat), deepseek=[model=deepseek-v4-pro, baseUrl=https://api.deepseek.com/v1] (fallback chat), dashscope=[chatModel=qwen-plus, baseUrl=https://dashscope.aliyuncs.com/compatible-mode/v1] (embedding + eval-side only)
```

#### Per-request logs (every LLM call)

| Layer | Log pattern | Level | Key fields |
|-------|-------------|-------|------------|
| `OpenAiCompatibleLlmClient` (request) | `LLM request: provider={}, model={}, url={}, role={}` | INFO | provider (Kimi / DeepSeek / DashScope), model name, full endpoint URL, role (`primary` / `fallback`) |
| `OpenAiCompatibleLlmClient` (response) | `LLM response: model={}, latency={}ms, tokens={}/{}` | INFO | model, latency, prompt/completion tokens |
| `LlmInvocationService` | `LLM [chat] ...` / `LLM [routing] ...` | INFO | scenario tag, latency, token counts; on fallback engagement also emit `LLM [chat:fallback-engaged] reason={exceptionClass}, primary=kimi-k2.6, fallback=deepseek-v4-pro` |
| `RerankService` | `LLM [rerank] scored {} candidates, top={}` | INFO | candidate count, top score |
| `DashScopeEmbeddingClient` | `Embedding request: provider=DashScope, model={}, texts={}` | INFO | model, batch size |

#### Python eval_interactive logs

| Component | Log pattern | Level |
|-----------|-------------|-------|
| `UserSimulator` | `LLM [simulator] call: model={}, base_url={}` | INFO |
| `LlmJudge` | `LLM [judge:{}] call: model={}, base_url={}` | INFO |

#### Verification

Quick verification command after deployment or config change:

```bash
grep "LLM request:" /tmp/csagent.log | head -5    # Which provider/model per call
grep "LLM config loaded:" /tmp/csagent.log         # Startup config summary
```

### 3.8.6 LLM Call Log (v9 — Full Observability)

Problem statement: the existing trace system only records the **final** LLM response per turn (in `bot_turns.llm_raw_response`). Routing calls, rerank calls (N per turn), and retry calls are completely invisible — creating a 100% observability blackout for the majority of LLM invocations.

**Solution**: dedicated `llm_call_log` table that records **every** LLM call regardless of call site.

```sql
CREATE TABLE llm_call_log (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          TEXT NOT NULL REFERENCES bot_sessions(session_id),
    turn_index          INT,                         -- NULL for routing (pre-turn)
    call_type           TEXT NOT NULL,                -- 'routing' | 'chat' | 'rerank' | 'retry'
    model               TEXT NOT NULL,                -- e.g. 'kimi-k2.6'
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

**Instrumented call sites**:

| Call Site | `call_type` | `turn_index` | Notes |
|-----------|-------------|--------------|-------|
| `LlmInvocationService.invokeRouting()` | `routing` | NULL | 1 per session init |
| `LlmInvocationService.invokeChat()` | `chat` | current turn | Main agent LLM call |
| `LlmInvocationService.invokeChat()` (retry) | `retry` | current turn | When first call returns invalid action |
| `RerankService.scoreCandidate()` | `rerank` | current turn | N per search (4–8 candidates) |

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

| Requirement | Implementation |
|-------------|---------------|
| Handover request retryable | `request_handover` retries once on Salesforce API failure; if still fails → return `failed` status + log alert; degrade to "please contact us again" message |
| State write verifiable | PostgreSQL transaction with `RETURNING` clause; Salesforce write verified via HTTP 201 response |
| Degraded mode | **Two-tier fallback (deviation 2026-05-01 — phase0 §0.6)**: Server-side chat completion has primary = Kimi 2.6, fallback = DeepSeek v4 pro. `LlmInvocationService` (or its underlying client wrapper) catches a defined set of transient failure classes from the primary call — timeout, HTTP 5xx, rate-limit (HTTP 429), and provider-specific connection-reset / circuit-broken exceptions (the exact exception classes are enumerated by the implementer based on the actual client surface) — and automatically retries the same prompt against DeepSeek v4 pro with the same tool schema. Non-transient errors (4xx other than 429, parse failures, prompt-too-long) skip the fallback and surface immediately. If both providers fail, `AgentRunResult.ERROR` is produced and handled by the §3.3.3 ERROR retry threshold (≥ 2 errors in current phase → `ESCALATE` with `escalation_reason = runtime_error_threshold`). After the threshold is exceeded the user-facing message remains "I'm having trouble right now — let me connect you to the team." (`escalation_reason = service_degraded` is retained for non-LLM degraded paths.) Fallback is for chat completions only — embeddings (`DashScopeEmbeddingClient`) are unaffected, and the Python `eval_interactive` harness's own LLM (DashScope `qwen-plus` per `eval_interactive.yaml`) is out of scope. Provider configuration: `application-local.yml` `llm.kimi` (primary) + `llm.deepseek` (fallback) blocks; env vars `KIMI_*` (existing) and `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` (default `https://api.deepseek.com/v1`) / `DEEPSEEK_MODEL` (default `deepseek-v4-pro`). DeepSeek's API is OpenAI-compatible, so the implementer can either reuse `OpenAiCompatibleLlmClient` with swapped base-url/key/model or introduce a thin `DeepSeekClient` wrapper. |
| No data loss on crash | Session state persisted to PostgreSQL on every turn completion; Salesforce sync is async but has retry queue |
| Idempotent message handling | Dedup on `(session_id, turn_index)` unique constraint; replay-safe |

### 3.9.2 Performance

| Metric | Target | Enforcement |
|--------|--------|-------------|
| FAQ answer e2e p95 | ≤ 5s | Includes: query embedding (~100ms) + pgvector search (~50ms) + rerank (~700ms parallel, was ~5s serial pre-v9) + LLM generation (~2–3s) + Salesforce write (~500ms) |
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
