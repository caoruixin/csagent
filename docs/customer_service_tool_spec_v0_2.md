# Customer Service Agent — Tool Spec v0.2

> **基于**: tool_spec v0.1 + platform_api_detailed_reference.md + FAQ-knowledge_include_help_url.csv + problem_retrieval_solution_plan_pgvector.md + customer_service_agent-Common-Phrases.md + phase1_solution_input_pack.md §1.4.13 (Pre-chat Form) + phase2_domain_realization_spec.md §2.10
>
> **变更摘要**: 本版将 v0.1 的 abstract candidate_api_mapping 具化为 **平台真实 API 端点**；纳入 pre-chat form 输入对 tool 触发时序的影响；基于 pgvector 方案明确 search_knowledge 实现路径；新增 `get_moderation_review_context` 运行时工具；补齐 allowed/disallowed 矩阵缺口；增加 fixed_script_library 作为运行时能力。

---

## 0. Changelog from v0.1

| 变更 | 说明 |
|------|------|
| **API mapping 具化** | 每个 tool 的 `candidate_api_mapping` 从抽象名称改为具体 HTTP endpoint（来源：platform_api_detailed_reference.md §18 矩阵 + 各服务 OpenAPI 契约）|
| **Pre-chat Form Integration** | `get_customer_context` 新增 `form_context` 输入项；INIT 阶段可用 email 立即触发，不需对话轮次采集 |
| **pgvector 架构落地** | `search_knowledge` 的 retrieval_backend 固定为 pgvector on Cloud SQL PostgreSQL；增加 chunk-level retrieval 说明 |
| **新增 runtime-only tool: `get_moderation_review_context`** | 基于 `gumshield-api POST /api/cs-review/ad-id/` 等获取广告删除/审核原因，供 UC-FP 和 UC-A 的 grounded 解释 |
| **allowed/disallowed 矩阵修补** | UC-B/E 显式加入 `get_customer_context` 的 `disallowed_use_cases`（v0.1 遗漏）；UC-H 明确为 `get_customer_context` disallowed |
| **fixed_script_library** | 新增为运行时能力（非 agent-visible tool），服务 UC-G/H/I/J/K 的固定话术场景 |
| **Common Phrases 对齐** | 话术模板引用从 BRD §6.3 扩展到 `customer_service_agent-Common-Phrases.md`（11.8k 条对话提炼的高频坐席话术）|
| **output_schema 丰富** | `lookup_listing_or_ad` output 增加 `moderation_review_reason`、`deletion_reason_code`；`lookup_customer_account` output 增加 `blacklist_status`、`last_login_date` |

---

## 1. Design Principles (inherited, unchanged)

| Principle | 说明 |
|-----------|------|
| `tool_surface_strategy` | `small_and_strong` — 工具数量最小但每个能力强 |
| `agent_pattern` | `single_agent_bounded_loop` — V1 单 agent |
| `grounding_policy` | `grounded_before_generative` — 能查知识就不自由生成 |
| `escalation_policy` | `escalation_is_first_class` — 转人工是正式能力 |
| `sensitive_write_actions` | `human_only_or_phase2` — 敏感写操作不暴露给 Bot |

---

## 2. Tool Inventory Summary

### 2.1 Agent-Visible Tools (5)

Bot LLM prompt 中可见、可直接发起调用的工具。

| # | Tool Name | Class | Risk | V1 Phase |
|---|-----------|-------|------|----------|
| 1 | `search_knowledge` | knowledge_retrieval | low | V1 |
| 2 | `resolve_article` | knowledge_resolution | low | V1 |
| 3 | `get_customer_context` | composite_context_read | medium | V1 |
| 4 | `request_handover` | controlled_write_action | medium | V1 |
| 5 | `record_outcome` | analytics_trace | low | V1 |

### 2.2 Runtime-Only Tools (5) — v0.1 was 3, +1 in v0.2, +1 in v0.2.1

Bot runtime 根据 use case policy 自动触发，不暴露给 LLM prompt。

| # | Tool Name | Class | Risk | V1 Phase | 变更 |
|---|-----------|-------|------|----------|------|
| 6 | `create_case_controlled` | controlled_write_action | high | V1_limited | unchanged |
| 7 | `lookup_customer_account` | trusted_read | medium | V1 | output enriched; **v0.2.1: UC-C added to allowed** |
| 8 | `lookup_listing_or_ad` | trusted_read | medium | V1 | output enriched; **v0.2.1: UC-C added to allowed** |
| 9 | `get_moderation_review_context` | trusted_read | medium | V1 | **NEW in v0.2** |
| 10 | `get_message_moderation_context` | trusted_read | medium | V1 | **NEW in v0.2.1** — UC-C messaging diagnostic |

### 2.3 Human-Only Tools (2) — unchanged

| # | Tool Name | Class | Risk |
|---|-----------|-------|------|
| 11 | `moderation_enforcement_action` | sensitive_write_action | critical |
| 12 | `send_followup_email_or_async_update` | async_notification | medium |

### 2.4 Runtime Capabilities (non-tool, system-level)

| Capability | 说明 |
|------------|------|
| `fixed_script_library` | 固定话术模板库（UC-G/H/I/J/K + opening/closing/escalation/out-of-hours），由 runtime 注入到 context projection，不作为 tool call |
| `form_context_ingestion` | Pre-chat form 数据解析，在 INIT 阶段将 email / ad_id / description / topic_subject 写入 session state |
| `tool_policy_enforcer` | 每次 tool call 前检查 active_use_case ∈ allowed_use_cases，违规返回 scope_blocked |
| `progress_placeholder` | tool call 超过 1.5s 时给用户 "正在查询" 占位消息 |

---

## 3. Tool Specifications (Detailed)

---

### 3.1 `search_knowledge`

**Visibility**: agent_visible | **Risk**: low | **Phase**: V1

**Purpose**: 在 FAQ 向量库 + Salesforce Knowledge 中检索与用户问题相关的知识片段，返回 grounded 候选结果。

**Allowed UCs**: UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP
**Disallowed UCs**: UC-G, UC-H, UC-I, UC-J, UC-K（这些 UC 使用 fixed_script_library，不做知识检索）

**Input Schema**:
```yaml
type: object
required: [query, use_case_id]
properties:
  query:
    type: string
    description: 从用户问题或 form_context.description 派生的检索查询
  use_case_id:
    type: string
    enum: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]
  customer_locale:
    type: string
    default: en_GB
  top_k:
    type: integer
    minimum: 1
    maximum: 5
    default: 3    # v0.2 changed: 默认从 5 降为 3，对齐 pgvector plan "Top 2-3"
  knowledge_scope:
    type: array
    items:
      type: string
    description: 可选的 collection / category 过滤
```

**Output Schema**:
```yaml
type: object
required: [results, faq_miss]
properties:
  results:
    type: array
    items:
      type: object
      required: [source_id, title, snippet, canonical_url, score]
      properties:
        source_id:
          type: string
          description: "Salesforce Knowledge Article Id (e.g. ka4P2000000021pIAA)"
        title:
          type: string
        snippet:
          type: string
          description: "chunk 文本（256-512 tokens），带 breadcrumb section_heading"
        canonical_url:
          type: string
          description: "Help_Site_URL__c（如 https://help.gumtree.com/s/...）"
        score:
          type: number
        source_type:
          type: string
          enum: [salesforce_knowledge, help_centre, approved_git_doc]
        chunk_id:
          type: string          # v0.2 new: pgvector chunk 标识
        article_id:
          type: string          # v0.2 new: 文章级 ID
  faq_miss:
    type: boolean
    description: "当 top result score < threshold 时为 true"
  retrieval_trace:
    type: object
    description: "query_hash, chunk_ids, scores, latency_ms"
```

**Retrieval Backend (v0.2 具化)**:
```
Architecture: pgvector on Cloud SQL PostgreSQL (对齐 problem_retrieval_solution_plan_pgvector.md)

Ingestion Pipeline:
  source → Salesforce Knowledge articles (FAQ-knowledge_include_help_url.csv 为初始集)
  chunking → 256-512 tokens sliding window, 10-20% overlap
  embedding → Google text-embedding (Vertex AI) / Gemini embedding
  storage → pgvector table: article_chunks(chunk_id, article_id, chunk_index,
            chunk_text, embedding vector(dim), section_heading, token_count)
  metadata → articles(article_id, title, summary, canonical_url, published, version)

Online Retrieval:
  1. Query → Embedding API → query_vector
  2. SQL: SELECT ... FROM article_chunks
         WHERE published = true
         ORDER BY embedding <=> query_vector
         LIMIT :top_k
  3. Optional: cross-encoder reranker (TBD)
  4. Filter: only published + in-validity-period chunks
  5. Deduplicate: same article_id keep highest score chunk only
```

**Concrete API Dependencies**:
| Step | API | Service |
|------|-----|---------|
| Vector search | `POST /v1/faq/search` (Bot 内部 API) | CSAgent service (pgvector) |
| Metadata verify | Salesforce Knowledge API (article publish status) | Salesforce |
| Embedding | Vertex AI text-embedding API | GCP |

**Failure Modes**:
- `no_results` → allow clarification or escalate (1 retry with rephrased query)
- `low_confidence_results` → faq_miss = true, increment counter
- `knowledge_backend_timeout` → retry 1x, then degrade to fixed_script + escalate
- `scope_blocked` → use_case not in allowed_use_cases

**Runtime Policy**:
```yaml
max_retries: 1
on_no_results: allow_clarification_or_escalate
must_log_article_candidates: true
score_threshold: TBD  # 低于此分数标记 faq_miss
```

---

### 3.2 `resolve_article`

**Visibility**: agent_visible | **Risk**: low | **Phase**: V1

**Purpose**: 将 search_knowledge 返回的候选结果转化为客户可见的文章包（title + 1-3 句摘要 + canonical link + 使用边界）。

**Allowed UCs**: UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP
**Disallowed UCs**: UC-G, UC-H, UC-I, UC-J, UC-K

**Input Schema**:
```yaml
type: object
required: [source_ids, use_case_id]
properties:
  source_ids:
    type: array
    items:
      type: string
      description: "Salesforce Knowledge article Id"
    minItems: 1
    maxItems: 3     # v0.2: tightened from 5
  use_case_id:
    type: string
    enum: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP]
  summary_style:
    type: string
    enum: [concise, standard]
    default: concise
```

**Output Schema**:
```yaml
type: object
required: [resolved_articles]
properties:
  resolved_articles:
    type: array
    items:
      type: object
      required: [source_id, title, summary, canonical_url]
      properties:
        source_id:
          type: string
        title:
          type: string
        summary:
          type: string
          description: "1-3 sentence grounded summary"
        canonical_url:
          type: string
          description: "Help_Site_URL__c — 展示给用户的唯一链接"
        safe_to_show:
          type: boolean
          description: "文章是否为 published + 对外可见"
        usage_boundary:
          type: string
          description: "使用限制说明（如 'does not constitute legal advice'）"
  article_shown_log_payload:
    type: object
    description: "ARTICLE_SHOWN 事件负载（source_ids, use_case, timestamp）"
```

**Concrete API Dependencies**:
| Step | API | Service |
|------|-----|---------|
| Article metadata | Salesforce Knowledge API (by article Id) | Salesforce |
| URL resolution | Help_Site_URL__c field from Knowledge object | Salesforce |
| Summary generation | LLM grounding mode OR pre-generated summary from articles table | CSAgent / pgvector DB |

**Runtime Policy**:
```yaml
must_track_article_shown: true           # 每次 resolve 写 ARTICLE_SHOWN event
customer_output_pattern: title_plus_1_to_3_sentence_summary_plus_link
must_verify_published_status: true       # v0.2 new: 确认文章仍为 published
```

---

### 3.3 `get_customer_context`

**Visibility**: agent_visible | **Risk**: medium | **Phase**: V1

**Purpose**: 从可信系统读取客户/账户/广告的最小必要上下文，返回结构化安全摘要。V1 为复合工具，内部组合 `lookup_customer_account` + `lookup_listing_or_ad` + `get_moderation_review_context`（v0.2 新增）。

**Allowed UCs**: UC-A, UC-C, UC-D, UC-F, UC-FP, UC-K
**Disallowed UCs**: UC-B, UC-E, UC-G, UC-H, UC-I, UC-J（v0.2: UC-B/E 显式加入 disallowed）

**Input Schema**:
```yaml
type: object
required: [context_type]
properties:
  context_type:
    type: string
    enum: [account, listing, combined]
  # ── Identifier sources ──
  email:
    type: string
    description: "来自 pre-chat form（必填）或用户对话输入"
  phone:
    type: string
  account_id:
    type: string
  listing_id:
    type: string
  ad_id:
    type: string
    description: "来自 pre-chat form（选填）或用户对话输入"
  use_case_id:
    type: string
  # ── v0.2 new: Pre-chat Form Context ──
  form_context:
    type: object
    description: "从 pre-chat form 直接注入，INIT 阶段可用"
    properties:
      first_name:
        type: string
      last_name:
        type: string
      email:
        type: string
        description: "新版表单必填 — Bot 无需再问 email"
      topic_subject:
        type: string
        description: "下拉选项 — UC 预分类弱信号"
      ad_id_number:
        type: string
        description: "选填 — 有值时可立即触发 listing lookup"
      description:
        type: string
        description: "必填 — 意图分类主输入"
```

**Output Schema**:
```yaml
type: object
required: [safe_summary]
properties:
  safe_summary:
    type: string
    description: "脱敏的结构化文本摘要，可直接注入 context projection"
  account_context:
    type: object
    properties:
      account_status:
        type: string
        enum: [ACTIVE, SUSPENDED, CLOSED, COLLECTION_AGENCY]
      restriction_flags:
        type: array
        items:
          type: string
      blacklist_status:
        type: string                # v0.2 new
        description: "from gumshield blacklist check"
      known_good:
        type: boolean               # v0.2 new
        description: "from gumshield known-good check"
      last_login_date:
        type: string                # v0.2 new
        description: "from user-service last-logged-in"
      identity_match_confidence:
        type: number
  listing_context:
    type: object
    properties:
      listing_status:
        type: string
      moderation_status:
        type: string
      visibility_reason:
        type: string
      moderation_review_reason:
        type: string                # v0.2 new: from get_moderation_review_context
      deletion_reason_code:
        type: string                # v0.2 new
  missing_identifiers:
    type: array
    items:
      type: string
    description: "所需但缺失的标识符列表"
```

**Concrete API Dependencies (v0.2 具化)**:

**Account Lookup Chain** (由 `lookup_customer_account` 执行):
| Step | API Endpoint | Service | 用途 |
|------|-------------|---------|------|
| 1. Email → User | `GET /api/emails/{email}/user` | bapi-server | 通过 email 找到 user/account |
| 1a. Alt | `GET /users/{email}/user` | user-service | 备选用户查找 |
| 2. Account details | `GET /api/accounts/{accountId}` | bapi-server | 账户状态、地址等 |
| 3. Account emails | `GET /api/accounts/{id}/emails` | bapi-server | 确认关联邮箱 |
| 4. Trust status | `GET /api/users/{id}/known-good` | gumshield-api | 用户可信度 |
| 5. Blacklist check | `POST /api/blacklists/email/check` | gumshield-api | 邮箱是否在黑名单 |
| 6. Last login | `GET /users/last-logged-in/{userId}` | user-service | 最近登录时间 |

**Listing Lookup Chain** (由 `lookup_listing_or_ad` 执行):
| Step | API Endpoint | Service | 用途 |
|------|-------------|---------|------|
| 1. Ad by ID | `GET /api/adverts/{id}` | bapi-server | 广告详情 |
| 1a. Alt | `GET /api/adverts/by-advert-id/{advert-id}` | advert-service | 备选查找 |
| 2. Ad features | `GET /api/adverts/{id}/features` | bapi-server | 付费特性 |
| 3. Moderation status | `GET /api/adverts/status/{id}` | gumshield-api | 审核状态 |
| 4. Known good | `GET /api/adverts/{id}/known-good` | gumshield-api | 广告可信度 |
| 5. Search index | `GET /api/advert/{advertId}` | livead-search | 搜索索引中的状态 |
| 6. User's ads | `GET /api/accounts/{id}/adverts` | bapi-server | 用户名下所有广告 |

**Moderation Review Chain** (由 `get_moderation_review_context` 执行, v0.2 新增):
| Step | API Endpoint | Service | 用途 |
|------|-------------|---------|------|
| 1. CS review by ad | `POST /api/cs-review/ad-id/` | gumshield-api | 获取广告的 CS 审核记录 |
| 2. Latest review | `POST /api/cs-review/latest/` | gumshield-api | 获取最新审核决定 |
| 3. Review by reason | `POST /api/cs-review/id-by-reason/` | gumshield-api | 按原因查审核记录 |

**Pre-chat Form 触发时序变更 (v0.2)**:
```
旧流程 (v0.1):
  用户进入对话 → Bot 问 email → 用户回答 → get_customer_context

新流程 (v0.2, 新版表单):
  用户填 pre-chat form (email 必填, ad_id 选填)
    → form_context 注入 session state
    → INIT 阶段 runtime 自动触发 get_customer_context(email=form.email, ad_id=form.ad_id)
    → Bot 首条消息已可引用 safe_summary

影响:
  - email 采集 clarification 轮次 = 0（旧版约 25% 会话需要 1 轮）
  - Bot 可在开场个性化: "Hi {first_name}, I can see your account..."
  - ad_id 如果表单已填 → listing_context 在首轮就可用
```

**Failure Modes**:
- `identifier_missing` → prompt for identifiers (仅当 form_context 也缺失时)
- `no_matching_record` → inform user, offer manual identifier input
- `multiple_matches` → ask for disambiguation
- `backend_timeout` → retry 1x, then proceed without context
- `permission_denied` → escalate

**Runtime Policy**:
```yaml
must_minimize_pii: true
must_return_safe_summary_only: true
should_prompt_for_identifiers_when_missing: true  # 仅当 form_context 缺失时生效
must_auto_trigger_on_form_context: true           # v0.2 new
```

---

### 3.4 `request_handover`

**Visibility**: agent_visible | **Risk**: medium | **Phase**: V1

**Purpose**: 将对话升级至人工队列，携带结构化 handover payload + 客户可见的下一步消息。

**Allowed UCs**: ALL (UC-A through UC-K)
**Disallowed UCs**: none

**Input Schema**:
```yaml
type: object
required: [primary_use_case, summary, escalation_reason]
properties:
  primary_use_case:
    type: string
  candidate_use_cases:
    type: array
    items:
      type: string
  intent_confidence:
    type: number
  summary:
    type: string
    maxLength: 500    # v0.2: 加上限制
  customer_answers:
    type: object
    description: "用户在 intake / clarification 中的结构化回答"
  articles_shown:
    type: array
    items:
      type: string
  identifiers:
    type: object
    description: "已收集的 email / ad_id / phone 等"
  escalation_reason:
    type: string
    enum:             # v0.2: 对齐 Phase 2 §2.4 完整 escalation reason 列表
      - user_requested
      - faq_miss_threshold_exceeded
      - clarification_budget_exhausted
      - intake_complete_for_uc_g
      - intake_complete_for_uc_h
      - intake_complete_for_uc_i
      - intake_complete_for_uc_j
      - intake_complete_for_uc_k
      - incomplete_intake
      - payment_dispute_detected
      - appeal_requires_human
      - user_distress
      - imminent_harm
      - incorrect_deletion_appeal
      - trust_safety_required
      - account_compliance
      - gdpr_intake
      - identity_verification_required
      - out_of_scope
      - service_degraded
      - turn_budget_exhausted
      - tool_scope_blocked
  case_id:
    type: string
    description: "如果 create_case_controlled 已创建"
  is_business_hours:
    type: boolean     # v0.2 new: 影响队列路由与用户话术
```

**Output Schema**:
```yaml
type: object
required: [handover_status]
properties:
  handover_status:
    type: string
    enum: [queued, transferred, offline_logged, failed]  # v0.2: added offline_logged
  queue:
    type: string
  handover_id:
    type: string
  customer_next_step_message:
    type: string
    description: "基于 escalation_reason + is_business_hours 选择的话术模板"
```

**Customer-Facing Message Templates** (from BRD §6.3 + Common-Phrases.md):
```yaml
business_hours_escalation: >
  I'll pass this to an agent now. I'll share what you've told me
  so you don't need to repeat yourself.

off_hours_escalation: >
  The team is currently offline. I've logged your details for follow-up —
  you'll hear back by email within {SLA_HOURS} hours.

intake_complete_handover: >
  Thank you for those details. I've created a case for our {TEAM_NAME} team —
  you'll hear back by email within {SLA_HOURS} hours.

imminent_harm_escalation: >
  I'm connecting you to our safety team right away.
  If you're in immediate danger, please contact the police.
```

**Concrete API Dependencies**:
| Step | API / System | 说明 |
|------|-------------|------|
| Queue routing | Salesforce Omni-Channel Transfer API | 按 use_case + business_hours 路由到对应 queue |
| Payload persist | Salesforce `Bot_Context__c` field on Case/Session | JSON 结构化 handover payload |
| Thread continuity | Salesforce Messaging Session API | must_preserve_same_thread |

**Runtime Policy**:
```yaml
must_preserve_same_thread: true
must_include_structured_summary: true
must_log_escalation_reason: true
must_select_message_by_hours_and_reason: true  # v0.2 new
```

---

### 3.5 `record_outcome`

**Visibility**: agent_visible | **Risk**: low | **Phase**: V1

**Purpose**: 记录会话结果、已展示文章、升级原因与 trace 元数据。

**Allowed UCs**: ALL
**Disallowed UCs**: none

**Input Schema** (unchanged from v0.1 except outcome enum):
```yaml
type: object
required: [session_id, outcome, use_case_id]
properties:
  session_id:
    type: string
  outcome:
    type: string
    enum:
      - resolved
      - escalated
      - abandoned
      - wrong_containment
      - over_escalation
      - unresolved_after_answer   # v0.2 new: 对齐 Phase 2 §2.7
  use_case_id:
    type: string
  articles_shown:
    type: array
    items:
      type: string
  escalation_reason:
    type: string
  trace_metadata:
    type: object
    description: "trace_id, turn_count, prompt_version, model_version, projection_version, latency_ms, cost"
```

**Concrete API Dependencies**:
| Sink | System | 说明 |
|------|--------|------|
| Salesforce | `Bot_Session__c.ContainmentOutcome__c` + `Bot_Event__c` | 结构化结果写入 SF 报表对象 |
| Analytics | Kafka (Avro) → analytics pipeline | 域事件同步 |
| Trace store | PostgreSQL (session outcome table) | 本地持久化 + eval 回归 |

**Runtime Policy**:
```yaml
must_be_called_on_close_or_escalation: true
retry_on_failure: true                          # v0.2: analytics sink 失败时重试
```

---

### 3.6 `create_case_controlled`

**Visibility**: runtime_only | **Risk**: high | **Phase**: V1_limited

**Purpose**: 为显式允许的类别创建 Salesforce Case，需 required_fields 完整且 policy 允许。

**Allowed UCs**: UC-H, UC-J, UC-K
**Disallowed UCs**: UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP, UC-G, UC-I

**Input Schema** (unchanged):
```yaml
type: object
required: [use_case_id, summary, required_fields]
properties:
  use_case_id:
    type: string
    enum: [UC-H, UC-J, UC-K]
  summary:
    type: string
  required_fields:
    type: object
    description: "per-UC 必填字段（见 Required Fields 合约）"
  urgency:
    type: string
    enum: [normal, high, critical]
  customer_identifiers:
    type: object
```

**Per-UC Required Fields Contract (v0.2 草案, 待合规+产品终审)**:
```yaml
UC-H:
  required:
    - ad_id_or_listing_url
    - registered_email
    - stated_reason_or_context
  queue_candidate: "Ad Moderation / Appeal queue"
  case_fields:
    Contact_Reason: "Incorrect Deletion Appeal"
    Sub_Reason: TBD
    Product_Area: "Ads"

UC-J:
  required:
    - report_target          # ad_id / seller / URL / phone
    - report_type            # fraud / scam / impersonation / harassment / other
    - description
  optional:
    - contacted_police
    - evidence_references
  queue_candidate: "Trust & Safety queue"
  case_fields:
    Contact_Reason: "Trust & Safety Report"
    Sub_Reason: "{report_type}"
    Product_Area: "Safety"

UC-K:
  required:
    - platform               # ios / android / web
    - repro_steps_or_error_message
  optional:
    - browser_or_app_version
    - screenshot_reference
  queue_candidate: "Technical Support queue"
  case_fields:
    Contact_Reason: "Technical Issue"
    Sub_Reason: TBD
    Product_Area: "Platform"
```

**Concrete API Dependencies**:
| Step | API / System | 说明 |
|------|-------------|------|
| Case creation | Salesforce REST/Composite API (`POST /services/data/vXX.0/sobjects/Case`) | 创建 Case |
| Queue assignment | Salesforce Assignment Rules / Omni-Channel queue config | 按 UC + urgency 路由 |
| Context attach | Salesforce `Bot_Context__c` field (Long Text, JSON) | 附加结构化 intake 结果 |

**Runtime Policy** (unchanged):
```yaml
model_direct_invocation_allowed: false
must_validate_required_fields: true
must_follow_use_case_policy: true
```

---

### 3.7 `lookup_customer_account`

**Visibility**: runtime_only | **Risk**: medium | **Phase**: V1

**Purpose**: 通过 email/phone/account_id 查找客户账户状态。由 `get_customer_context` 内部调用。

**Allowed UCs**: UC-C, UC-D, UC-FP, UC-K (via get_customer_context) — **v0.2.1: added UC-C** (messaging diagnostic needs account restriction check; fixes matrix inconsistency with `get_customer_context.allowed_use_cases`)
**Disallowed UCs**: UC-G, UC-I, UC-J

**Input Schema** (unchanged):
```yaml
type: object
properties:
  email:
    type: string
  phone:
    type: string
  account_id:
    type: string
```

**Output Schema (v0.2 enriched)**:
```yaml
type: object
properties:
  account_id:
    type: string
  user_id:
    type: string
  account_status:
    type: string
    enum: [ACTIVE, SUSPENDED, CLOSED, COLLECTION_AGENCY]
  restriction_flags:
    type: array
    items:
      type: string
  blacklist_status:           # v0.2 new
    type: object
    properties:
      is_blacklisted:
        type: boolean
      reason:
        type: string
  known_good:                 # v0.2 new
    type: boolean
  last_login_date:            # v0.2 new
    type: string
    format: date-time
  safe_summary:
    type: string
```

**Concrete API Call Chain**:
```
1. GET /api/emails/{email}/user  (bapi) → user_id, account_id
   OR GET /users/{email}/user    (user-service) → fallback
2. GET /api/accounts/{accountId} (bapi) → account_status, name, etc.
3. GET /api/users/{id}/known-good (gumshield) → known_good flag
4. POST /api/blacklists/email/check (gumshield) → is_blacklisted, reason
5. GET /users/last-logged-in/{userId} (user-service) → last_login
6. Assemble safe_summary (redact PII, return structured flags only)
```

---

### 3.8 `lookup_listing_or_ad`

**Visibility**: runtime_only | **Risk**: medium | **Phase**: V1

**Purpose**: 通过 ad_id/listing_id 查找广告状态、审核状态、可见性原因。由 `get_customer_context` 内部调用。

**Allowed UCs**: UC-A, UC-C, UC-FP, UC-K (via get_customer_context) — **v0.2.1: added UC-C** ("can't get replies to specific ad" needs ad status check; read-only, risk unchanged)
**Disallowed UCs**: UC-G, UC-I, UC-J

**Output Schema (v0.2 enriched)**:
```yaml
type: object
properties:
  ad_id:
    type: string
  listing_status:
    type: string
    description: "e.g., ACTIVE, PAUSED, EXPIRED, DELETED, PENDING_REVIEW"
  moderation_status:
    type: string
    description: "e.g., APPROVED, REJECTED, PENDING, TAKEN_DOWN"
  visibility_reason:
    type: string
    description: "为什么广告不可见（如 'rejected by TMX filter'）"
  moderation_review_reason:     # v0.2 new: from get_moderation_review_context
    type: string
    description: "CS review 中记录的具体删除/拒绝原因"
  deletion_reason_code:         # v0.2 new
    type: string
  known_good:                   # v0.2 new
    type: boolean
  category:
    type: string
  location:
    type: string
  safe_summary:
    type: string
```

**Concrete API Call Chain**:
```
1. GET /api/adverts/{id}  (bapi) → 广告详情（title, status, category, location）
   OR GET /api/adverts/by-advert-id/{advert-id} (advert-service)
2. GET /api/adverts/{id}/features (bapi) → 付费特性
3. GET /api/adverts/status/{id}   (gumshield) → moderation_status
4. GET /api/adverts/{id}/known-good (gumshield) → known_good
5. POST /api/cs-review/ad-id/    (gumshield) → moderation_review_reason [v0.2]
6. GET /api/advert/{advertId}     (livead-search) → 搜索索引状态
7. Assemble safe_summary
```

---

### 3.9 `get_moderation_review_context` — NEW in v0.2

**Visibility**: runtime_only | **Risk**: medium | **Phase**: V1

**Purpose**: 获取广告/用户的审核历史与删除原因。为 UC-FP（解释删除原因）和 UC-A（解释可见性）提供 grounded 事实依据。由 `lookup_listing_or_ad` 内部调用或 `get_customer_context(context_type=combined)` 时自动串联。

**Allowed UCs**: UC-A, UC-FP (via get_customer_context → lookup_listing_or_ad)
**Disallowed UCs**: UC-G, UC-H, UC-I, UC-J, UC-K（UC-H 是申诉方，不应看到内部审核细节）

**Motivation**:
- UC-FP（正确删除解释）是最高量 use case（20,875 cases, 17.3%）
- 人工坐席回答"你的广告为什么被删"时，需要查 gumshield cs-review 才能给出具体原因
- 如果 Bot 只能说"违反了政策"而无法说明具体是哪条，containment 率会极低
- 通过读取 cs-review 中的 reason code/description，Bot 可以说"your ad was removed because it [specific reason]"，再附上对应 Community Standards 链接

**Input Schema**:
```yaml
type: object
required: [ad_id]
properties:
  ad_id:
    type: string
  review_type:
    type: string
    enum: [latest, by_reason, all]
    default: latest
```

**Output Schema**:
```yaml
type: object
properties:
  has_review:
    type: boolean
  review_reason:
    type: string
    description: "删除/拒绝的具体原因（human-readable）"
  reason_code:
    type: string
    description: "原因代码（用于映射到 Community Standards 链接）"
  review_date:
    type: string
    format: date-time
  reviewer_type:
    type: string
    enum: [automatic, manual]
  safe_summary:
    type: string
    description: "可在 context projection 中使用的脱敏摘要"
```

**Concrete API Dependencies**:
| Step | API Endpoint | Service | 说明 |
|------|-------------|---------|------|
| 1. Latest review | `POST /api/cs-review/latest/` | gumshield-api | 最新审核决定 |
| 2. Review by ad | `POST /api/cs-review/ad-id/` | gumshield-api | 按广告 ID 查审核记录 |
| 3. Review by reason | `POST /api/cs-review/id-by-reason/` | gumshield-api | 按原因查审核记录 |
| 4. Ad screen items | `GET /api/ad-screen-items/advert/{advert-id}` | gumshield-rules | 触发的审核规则 |

**Runtime Policy**:
```yaml
agent_direct_invocation_allowed: false
must_minimize_pii: true
must_return_safe_summary_only: true     # 不暴露内部审核工具截图/日志
must_map_reason_to_public_policy: true  # reason_code → Community Standards 条目映射
```

**Why This Is a V1 Tool (not V1.1)**:
- UC-FP 是量最大的 use case (17.3% of all cases)
- 没有具体删除原因，Bot 只能给泛化模板，用户不会满意 → containment 极低
- gumshield cs-review API 已存在且为 GET/POST 只读查询
- 不涉及任何写操作，risk 可控

---

### 3.10 `get_message_moderation_context` — NEW in v0.2.1

**Visibility**: runtime_only | **Risk**: medium | **Phase**: V1

**Purpose**: 查询用户近期消息是否被平台消息审核系统拦截。为 UC-C（消息与回复）提供 grounded 诊断依据：区分"消息已正常投递但对方未回复" vs "消息被审核过滤器拦截"。由 `get_customer_context` 在 `active_use_case = UC-C` 时链式调用。

**Allowed UCs**: UC-C (via get_customer_context)
**Disallowed UCs**: UC-A, UC-B, UC-D, UC-E, UC-F, UC-FP, UC-G, UC-H, UC-I, UC-J, UC-K

**Motivation**:
- UC-C "收不到回复" 是常见支持场景（参考 case 570Q5000008oMIVIA2）
- 人工坐席处理此类问题时会检查：账户限制 → 广告状态 → 消息审核历史
- 无消息审核查询时，Bot 只能说"账户没问题"但无法排除消息级拦截
- `message-moderation-history` 微服务提供只读搜索 API，与广告审核查询 `get_moderation_review_context` 完全同构

**Input Schema**:
```yaml
type: object
required: [email]
properties:
  email:
    type: string
    description: "用户 email（from form_context）"
  conversation_id:
    type: string
    description: "可选 — 如果用户提供了具体对话/广告 ID"
  lookback_days:
    type: integer
    default: 7
    description: "向前搜索天数"
```

**Output Schema**:
```yaml
type: object
properties:
  has_blocked_messages:
    type: boolean
  blocked_count:
    type: integer
    description: "lookback 期间被拦截的消息数"
  latest_block_reason:
    type: string
    description: "最近一条被拦截消息的审核原因"
  moderation_decision:
    type: string
    enum: [VALID, BLOCKED, PENDING, UNKNOWN]
  safe_summary:
    type: string
    description: "可注入 context projection 的脱敏摘要"
```

**Concrete API Dependencies**:
| Step | API Endpoint | Service | 说明 |
|------|-------------|---------|------|
| 1. Search by email | `POST /history/moderation/search` | message-moderation-history | filter: `field=EMAIL, value={email}` + `field=STATUS, operator=EQ, value=BLOCKED` |
| 2. Search by conversation | `POST /history/moderation/search` | message-moderation-history | filter: `field=CONVERSATION_ID, value={conversation_id}`（可选） |

**Runtime Policy**:
```yaml
agent_direct_invocation_allowed: false
used_by: [get_customer_context]
must_minimize_pii: true
must_return_safe_summary_only: true
retry_on_failure: true
```

---

### 3.11 `moderation_enforcement_action` (human_only, unchanged)

**Visibility**: human_only | **Risk**: critical | **Phase**: Phase2_or_manual_only

**Purpose**: 高风险审核执行（删帖、限号、账号限制）。Bot 永远不可调用。

**Concrete API Mapping (for reference only — not called by Bot)**:
| Action | API Endpoint | Service |
|--------|-------------|---------|
| Takedown ad | `POST /api/cs/adverts/takedown` | bapi-server |
| Auto reject | `POST /api/cs/adverts/auto_reject` | bapi-server |
| Auto delete | `POST /api/cs/adverts/auto_delete` | bapi-server |
| Delete by user | `POST /api/cs/adverts/delete_by_user` | bapi-server |
| Publish ad | `POST /api/cs/adverts/publish` | bapi-server |
| Change moderation status | `POST /api/adverts/status` | gumshield-api |
| Create CS review | `POST /api/cs-review` | gumshield-api |
| Create user report | `POST /api/user-reports` | gumshield-api |
| Add to blacklist | `POST /api/blacklists/email` | gumshield-api |
| Remove from blacklist | `DELETE /api/blacklists/email` | gumshield-api |
| Flag conversation | `POST /api/conversation/flag` | gumshield-api |

---

### 3.12 `send_followup_email_or_async_update` (human_only, unchanged)

**Visibility**: human_only | **Risk**: medium | **Phase**: V1_1_or_manual_only

**Purpose**: 异步邮件/状态更新。V1 由人工或 back-office 触发。

**Concrete API Mapping (for reference)**:
| Action | API | Service |
|--------|-----|---------|
| CRM email | Salesforce Email-to-Case / Email templates | Salesforce |
| GDPR SAR | `GET /gdpr/sar/{email}` | braze-gateway |
| GDPR DDR | `DELETE /gdpr/ddr/{email}` | braze-gateway |

---

## 4. Agent-Visible Tool × Use Case Matrix (v0.2)

| Tool \\ UC | UC-A | UC-B | UC-C | UC-D | UC-E | UC-F | UC-FP | UC-G | UC-H | UC-I | UC-J | UC-K |
|-----------|:---:|:---:|:---:|:---:|:---:|:---:|:----:|:---:|:---:|:---:|:---:|:---:|
| `search_knowledge` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `resolve_article` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `get_customer_context` | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `request_handover` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `record_outcome` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

## 5. Runtime-Only Tool × Use Case Matrix (v0.2.1)

| Tool \\ UC | UC-A | UC-B | UC-C | UC-D | UC-E | UC-F | UC-FP | UC-G | UC-H | UC-I | UC-J | UC-K |
|-----------|:---:|:---:|:---:|:---:|:---:|:---:|:----:|:---:|:---:|:---:|:---:|:---:|
| `lookup_customer_account` | ❌ | ❌ | **✅** | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `lookup_listing_or_ad` | ✅ | ❌ | **✅** | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `get_moderation_review_context` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **`get_message_moderation_context`** | ❌ | ❌ | **✅** | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `create_case_controlled` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ |

> **v0.2.1 变更**：UC-C 新增 `lookup_customer_account` + `lookup_listing_or_ad` + `get_message_moderation_context`（消息审核诊断链路），修复矩阵不一致并增强消息场景的 grounded 诊断能力。

---

## 6. Per-UC Tool Call Sequence (v0.2 updated)

| Use Case | Typical Sequence | 变化 |
|----------|-----------------|------|
| UC-A-01 | `get_customer_context`(listing, **auto from form_context.ad_id**) → `search_knowledge` → `resolve_article` → confirmation → `record_outcome` | v0.2: auto-trigger from pre-chat form |
| UC-B-01 | `search_knowledge` → `resolve_article` → confirmation → `record_outcome` | unchanged |
| UC-C-01 | `get_customer_context`(**combined: account+listing+message_moderation**, auto from form) → IF restricted/blocked/ad_inactive → explain + escalate option; IF clean → `search_knowledge` → `resolve_article` → confirmation → `record_outcome` | **v0.2.1: diagnostic path with account+ad+message moderation checks** |
| UC-D-01 | `get_customer_context`(account, **auto from form_context.email**) → `search_knowledge` → `resolve_article` → confirmation → `record_outcome` | v0.2: auto-trigger |
| UC-E-01 | `search_knowledge` → `resolve_article` → confirmation → `record_outcome` | unchanged |
| UC-F-01 | `get_customer_context`(listing)? → `search_knowledge` → `resolve_article` → confirmation → `record_outcome` | unchanged |
| UC-FP-01 | `get_customer_context`(combined, **incl. moderation_review**) → `search_knowledge` → `resolve_article` → confirmation → `record_outcome` | v0.2: moderation review context enriches explanation |
| UC-G-01 | fixed_script(gdpr_intro) → ask_user(intake) → `request_handover` → `record_outcome` | unchanged |
| UC-H-01 | fixed_script(empathy+intake) → ask_user(intake) → runtime:`create_case_controlled` → `request_handover` → `record_outcome` | unchanged |
| UC-I-01 | fixed_script(dispute_disclaimer) → ask_user(intake) → `request_handover` → `record_outcome` | unchanged |
| UC-J-01 | fixed_script(safety_intro) → ask_user(intake) → runtime:`create_case_controlled` → `request_handover` → `record_outcome` | unchanged |
| UC-K-01 | `get_customer_context`(combined) → [if explainable] → fixed_script + confirmation → `record_outcome`; [if not] → ask_user(intake) → runtime:`create_case_controlled` → `request_handover` → `record_outcome` | unchanged |

---

## 7. Fixed Script Library (Runtime Capability, v0.2 NEW)

Not a tool — a runtime-managed template library injected into context projection when `active_use_case ∈ {UC-G, UC-H, UC-I, UC-J, UC-K}` or for shared opening/closing/escalation messages.

### 7.1 Source Inputs

| Source | Coverage |
|--------|---------|
| BRD §6.3 (naming, tone, recommended/forbidden phrases) | Opening, self-service, resolution check, escalation, out-of-hours |
| `customer_service_agent-Common-Phrases.md` (11.8k transcript patterns) | Agent opener, empathy, hold, info-request, conclusion, idle-close patterns |
| Phase 2 §2.6 per-UC policy overrides | UC-specific templates (e.g., UC-FP standard explanation, UC-H empathy + intake) |
| Compliance review (pending) | UC-FP policy explanation finals, UC-H appeal response, UC-G GDPR process |

### 7.2 Template Categories

| Category | Key Templates | Used By |
|----------|--------------|---------|
| `opening` | Bot disclosure + personalized greeting using first_name | All UCs |
| `empathy` | Acknowledgement templates (no "I understand how you feel") | UC-FP, UC-H, UC-I, UC-J |
| `hold_placeholder` | "One moment while I look into this" | All UCs (during tool calls) |
| `identifier_request` | "Could you confirm your {field} so I can {reason}?" | UC-A/C/D/F/FP/K (when form_context insufficient) |
| `resolution_check` | "Did that answer your question?" + yes/no branches | UC-A/B/C/D/E/F/FP |
| `escalation` | Business hours + off-hours variants | All UCs |
| `gdpr_intake` | GDPR process explanation + required field prompts | UC-G |
| `appeal_intake` | Empathy + intake prompts + case creation confirmation | UC-H |
| `dispute_disclaimer` | "Payment disputes are handled by our specialist team" | UC-I |
| `safety_intake` | "Thank you for reporting this" + structured intake | UC-J |
| `tech_troubleshoot` | Basic troubleshooting (clear cache, update browser) | UC-K |
| `policy_explanation` | Community Standards explanation + repost guidance | UC-FP |
| `idle_close` | Timeout warning + close | All UCs |
| `forbidden_response` | What NOT to say (per BRD + transcript analysis) | Eval grader reference |

### 7.3 Forbidden Phrases (from BRD §6.3 + Common-Phrases §6 + transcript analysis)

Bot must NEVER output:
- "I understand how you feel" (sounds fake when bot is wrong)
- "I'm sorry to hear that" (repeatedly — max 1x per session)
- "As an AI language model..."
- "I've fixed that" / "I'll process your refund" / "The ads have now been removed"
- "I have also restricted your number" / "Your restriction is lifted"
- "You are speaking to a human" / "I am a real human"
- Any implication that bot has taken enforcement action
- Financial or legal advice phrasing

---

## 8. API Not Mapped to Any Tool (Excluded from V1)

The following platform APIs from `platform_api_detailed_reference.md` are **intentionally excluded** from V1 Bot tool surface:

| API Category | Example Endpoints | Exclusion Reason |
|-------------|------------------|------------------|
| **Order / Payment / Refund** | `bapi POST /api/orders/refund`, `gumshield POST /api/refunds` | Financial operations — human_only / forbidden |
| **GDPR execution** | `bapi DELETE /api/gdpr/account`, `user-service DELETE /users/gdpr/*` | GDPR execution — human_only with audit |
| **Account mutations** | `bapi PUT /api/accounts/{id}` (status change) | Account state changes — human_only |
| **Ad mutations** | `bapi POST/PUT/DELETE /api/adverts/*`, `POST /api/cs/adverts/*` | Write operations — human_only |
| **Blacklist mutations** | `gumshield POST/DELETE /api/blacklists/email` | Enforcement — human_only |
| **Message moderation** | `gumshield POST /api/cs-message-review` | Content moderation — human_only |
| **Reply/messaging** | `bapi POST /api/reply` | Sending messages as user — out of scope |
| **Pricing/packaging** | `pricing-api`, `bapi /api/price/*` | Not relevant to CS agent |
| **Category/rules management** | `gumshield-rules *` (write), `message-moderation-rules *` | Config management — admin only |

---

## 9. Open Decisions (carried forward + new)

| # | Decision | Status | Owner |
|---|----------|--------|-------|
| 1 | Final queue routing table by UC and operating hours | Pending | Salesforce Admin + Ops |
| 2 | `create_case_controlled` per-UC required_fields finalization | Pending | Compliance + Product |
| 3 | Help Centre article ID ↔ UC mapping (from FAQ-knowledge_include_help_url.csv) | **Partially available** — CSV has article IDs; UC mapping needed | Knowledge Ops |
| 4 | Knowledge allowlist governance for approved_git_doc | Pending | Knowledge Ops |
| 5 | Embedding model selection (Vertex AI text-embedding vs Gemini embedding) | Pending | Engineering |
| 6 | pgvector index strategy (IVFFlat vs HNSW) | Pending | Engineering |
| 7 | `score_threshold` for faq_miss determination | Pending | Engineering + QA |
| 8 | `get_moderation_review_context` — gumshield cs-review API access approval for Bot service account | **NEW** | Security + gumshield team |
| 9 | Pre-chat form new version field names (Topic Subject dropdown options) | **NEW** | Product |
| 10 | `is_business_hours` determination logic (9am-8pm Mon-Sun UK?) | Pending | Operations |

---

## 10. Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| v0.1 | 2026-04-01 | — | Initial tool spec |
| v0.2 | 2026-04-17 | — | API mapping 具化; pre-chat form integration; new `get_moderation_review_context`; pgvector architecture; fixed_script_library; allowed/disallowed matrix fixes; Common Phrases alignment |
| v0.2.1 | 2026-04-21 | — | UC-C messaging diagnostic: `lookup_customer_account` + `lookup_listing_or_ad` added UC-C to allowed (matrix fix); new `get_message_moderation_context` runtime-only tool; UC-C tool call sequence updated to diagnostic path; parent_topic_subject per UC; handover-only Topic Subjects |
