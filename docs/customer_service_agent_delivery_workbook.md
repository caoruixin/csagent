# Customer Service Agent Delivery Workbook

> 目标：把已有的 Normative Layer，稳定转化为某个具体 customer service agent 项目的 coding-ready technical design，并形成可交给 coding agent 的实现包。

> **Asset audit (2026-05-10, docs-only sweep)** — Several files
> referenced from the tables below are **not present in the repo
> today**. The pre-existing links have been preserved (not silently
> deleted, per `docs/current/doc_governance.md` §"Stale references")
> and annotated inline as `MISSING_ASSET` / `TODO_REVIEW`. The known
> gaps:
>
> - `case-samples.md` — **MISSING_ASSET / TODO_REVIEW**. No file
>   matches at `docs/case-samples.md` or anywhere under `docs/`.
>   Treat as a reference-only pointer until the owner re-supplies
>   it or the reference is consciously retired.
> - `customer_service_conversation_samples_organized.xlsx` —
>   **MISSING_ASSET / TODO_REVIEW**. Not present in the repo.
> - `inferred_tool_candidates_from_human_conversations.xlsx` —
>   **MISSING_ASSET / TODO_REVIEW**. Not present in the repo.
> - `data/human_review_annotations_2026-04-22_complete.csv` —
>   **MISSING_ASSET / TODO_REVIEW (path drift)**. The actual file in the repo is
>   `data/human_review_annotations_2026-04-22_golden.csv` (with
>   `data/bak/human_review_annotations_2026-04-22--bak.csv` as a
>   backup). Either correct the link to `_golden.csv` or confirm
>   the `_complete.csv` artefact was renamed.
>
> This sweep does NOT delete the references — it marks them so
> readers can tell which links are actionable and which point at
> retired/renamed assets. Decisions on rename / retire / re-supply
> should come from the workbook owner, not from a docs-cleanup PR.
> The same MISSING_ASSET pointers also appear in
> `docs/foundational/phase1_solution_input_pack.md` and
> `docs/foundational/phase2_domain_realization_spec.md`; those were
> intentionally left untouched in this docs-only PR (foundational
> docs are edited only on the fold-back cadence — see
> `docs/current/doc_governance.md`).

---

## 使用方式

这份工作文档按 6 个阶段推进：

1. Freeze Normative Layer
2. Build Solution Input Pack
3. Produce Domain Realization Spec
4. Produce Detailed Technical Design
5. Produce Coding Agent Implementation Packet
6. Eval / Release / Feedback Loop

每个阶段都先产出结构化结果，再进入下一阶段。

---

## 阶段产出文件索引

| 阶段 | 状态 | 产出文件 |
|------|------|---------|
| Phase 0 — Freeze Normative Layer | ✅ 已完成（v6; v8 HR 完成反映）| [`foundational/phase0_normative_freeze.md`](./foundational/phase0_normative_freeze.md) |
| Phase 1 — Build Solution Input Pack | ✅ 已完成（v6; v8 HR 完成反映）| [`foundational/phase1_solution_input_pack.md`](./foundational/phase1_solution_input_pack.md) |
| Phase 2 — Produce Domain Realization Spec | ✅ 已完成（v6; v8 HR 完成反映）| [`foundational/phase2_domain_realization_spec.md`](./foundational/phase2_domain_realization_spec.md) |
| Phase 3 — Produce Detailed Technical Design | ✅ 已完成（v1; v8 HR 完成反映）| [`foundational/phase3_detailed_technical_design.md`](./foundational/phase3_detailed_technical_design.md) |
| Phase 4 — Coding Agent Implementation Packet | ✅ 已完成（v1）| [`phase4_coding_agent_implementation_packet.md`](./phase4_coding_agent_implementation_packet.md) |
| Phase 5 — Eval / Release / Feedback Loop | ✅ 已完成（v1; v8 HR 完成反映）| [`foundational/phase5_evaluation_design.md`](./foundational/phase5_evaluation_design.md) |

本 workbook 保留作为**阶段模板与索引**；每个阶段的实际产出在独立文件中维护，便于 review 与迭代。

## 规范层文件（外部依赖）

| 文件 | 角色 |
|------|------|
| [`foundational/customer_service_agent_tech_spec.md`](./foundational/customer_service_agent_tech_spec.md) | 通用 Customer Service Agent 技术规范（V1 内核 / 架构 / 控制 / 工具 / Handover / Guardrails / Observability / NFR / Release Criteria）|
| [`customer_service_agent_eval_spec.md`](./customer_service_agent_eval_spec.md) | 通用 Eval 规范（Eval scope / Dataset / Grader / Metrics / Launch Gates / CI/CD）|

## 业务输入文件

| 文件 | 角色 |
|------|------|
| [`foundational/BRD.md`](./foundational/BRD.md) | Business Requirements — scope、user stories、UX、tone-of-voice、launch criteria |
| [`foundational/PRD_biz_part.md`](./foundational/PRD_biz_part.md) | Business-only PRD — 系统角色、handling 状态机、A–F' 对话流、Salesforce 数据模型骨架 |
| [`case-data-stat.md`](./case-data-stat.md) | 120,367 条 Case 历史数据分析（Case Reason 维度）|
| [`case-samples.md`](./case-samples.md) | 原始 Case 样本 — **MISSING_ASSET / TODO_REVIEW**: file not present in repo (2026-05-10 audit) |
| [`customer_service_conversation_samples_organized.xlsx`](./customer_service_conversation_samples_organized.xlsx) | **新增** — 499 条 Live Chat 会话（含 262 条 transcript、2,890 turns、43 条 curated examples）— **MISSING_ASSET / TODO_REVIEW**: file not present in repo (2026-05-10 audit) |
| [`inferred_tool_candidates_from_human_conversations.xlsx`](./inferred_tool_candidates_from_human_conversations.xlsx) | **新增** — 从 262 条对话反推的 10 类工具候选 + 证据 — **MISSING_ASSET / TODO_REVIEW**: file not present in repo (2026-05-10 audit) |
| [`customer_service_tool_spec_v0_2.yaml`](./customer_service_tool_spec_v0_2.yaml) | **v4 更新** — V1 Tool Spec v0.2（concrete API mapping / pre-chat form / pgvector backend / `get_moderation_review_context` / runtime capabilities）|
| [`runbooks/salesforce-part-spec.md`](./runbooks/salesforce-part-spec.md) | **v4 新增** — Salesforce 组织配置确认（Enhanced Chat / Omni-Channel / Knowledge API / Case 字段 / Queue / Off-hours / Pre-chat Form）|
| [`proposals/problem_retrieval_solution_plan_pgvector.md`](./proposals/problem_retrieval_solution_plan_pgvector.md) | **v4 新增** — pgvector 检索方案确认 |
| [`customer_service_agent-Common-Phrases.md`](./customer_service_agent-Common-Phrases.md) | **v4 新增** — 从 1.18 万条 transcript 提炼的标准话术指导（7 类模板）|
| [`FAQ-knowledge_include_help_url.csv`](./FAQ-knowledge_include_help_url.csv) | **v4 新增，v6 修正** — 218 篇 Salesforce Knowledge 文章（Id / Title / Summary / Help_Site_URL__c）。原统计 3,963 系 CSV 行数（Description__c 含多行 HTML），实际文章数 218 |
| [`platform_api_detailed_reference.md`](./platform_api_detailed_reference.md) | **v4 新增** — 196 REST 端点 / 14 微服务详细 API 参考 |
| [`fixed_script_library_v1.md`](./fixed_script_library_v1.md) | **v4 新增** — Bot V1 固定话术模板库（14 类 / 50+ 模板 / 禁止话术清单），从 1,230+ 条真实 agent 消息提炼；合规审批已通过（v5, 2026-04-19）|
| [`07-engineering-constraints.md`](./07-engineering-constraints.md) | Gumtree 平台代码库提取的工程基线 |

---

# Phase 0 — Freeze Normative Layer

## 0.1 这一步的目标
确认哪些内容是“母规范”，后续项目不得随意漂移。

## 0.2 对 customer service agent 固定的共性约束
- single agent + bounded loop
- state outside, context projected
- grounded before generative
- escalation is first-class
- eval before expansion
- V1 优先高频、低中风险、边界清晰场景
- V1 不做复杂多步骤流程编排
- V1 不做高风险自动决策
- 必须有 handover contract
- 必须有 minimal observability
- 必须有 release gates

## 0.3 本项目需要显式继承的规范项
- V1 scope boundary:
- control kernel:
- allowed actions:
- use case registry lite schema:
- handover payload contract:
- eval suites:
- release gates:
- deferred to V1.1 / V2:

---

# Phase 1 — Build Solution Input Pack

## 1.1 Business Input
### 目标
- 这个 agent 的业务目标是什么：
- 要服务哪些用户：
- 成功的业务结果是什么：

### Scope
- In-scope 场景：
- Out-of-scope 场景：
- 高风险场景：
- 必须转人工场景：

### Use Cases / Cases
- 高频 use cases：
- 典型用户提问样本：
- 历史 bad cases：
- 真实 transcript / ticket / chat 数据来源：

### Success Metrics
- containment:
- escalation quality:
- user satisfaction proxy:
- latency:
- cost:

## 1.2 Domain Knowledge Input
- 权威知识源：
- 可用于 grounding 的内容范围：
- 不能作为事实源的内容：
- freshness 要求：
- ownership：
- 当前已知 knowledge gaps：

## 1.3 Operating Model Input
- 转人工给谁：
- 人工系统 / queue：
- handover 后流程：
- 工作时间限制：
- SLA / TTFR：
- QA / Ops owner：
- bad-case bank owner：

## 1.4 Engineering Constraint Input
- 编程语言：
- 服务框架：
- 数据库：
- 向量检索 / 搜索能力：
- 部署环境：
- CI/CD：
- 日志 / tracing：
- secret / config 管理：
- 合规 / 安全要求：
- 外部系统集成：
- 现有 codebase / repo 约束：

## 1.5 Evaluation Input
- golden dataset：
- clarification dataset：
- escalation dataset：
- bad-case bank：
- release threshold：
- weekly review mechanism：

---

# Phase 2 — Produce Domain Realization Spec

## 2.1 目标
把通用规范映射成这个具体 customer service domain 的业务控制模型。

## 2.2 Use Case Registry
每个 use case 至少定义：
- use_case_id:
- name:
- description:
- example_user_requests:
- knowledge_scope:
- risk_level:
- allow_clarification:
- allow_bot_resolution:
- allowed_actions:
- escalation_conditions:
- outcome_class:

## 2.3 Risk Matrix
- low risk:
- medium risk:
- high risk:
- forbidden automation:

## 2.4 Escalation Matrix
- user requested human:
- insufficient grounding:
- clarification budget exhausted:
- faq miss threshold exceeded:
- policy-required escalation:
- service degraded:

## 2.5 Knowledge Scope Map
- use case → knowledge sources:
- use case → disallowed knowledge:
- use case → expected evidence type:

## 2.6 Control Policy by Use Case
- discover policy:
- clarification policy:
- resolution policy:
- confirmation policy:
- escalation policy:
- close policy:

## 2.7 Outcome Definition
- resolved:
- clarified but unresolved:
- escalated:
- abandoned:
- wrong containment:

---

# Phase 3 — Produce Detailed Technical Design

## 3.1 Runtime Architecture
- system context:
- major components:
- control kernel runtime flow:
- context builder:
- tool dispatcher:
- handover module:
- observability hooks:

## 3.2 State Model
- external state:
- session state:
- memory policy:
- projected context contract:

## 3.3 Control Kernel
- states:
- transitions:
- allowed actions:
- budgets:
- drift handling:

## 3.4 Tools / Capabilities
- search_knowledge:
- resolve_article:
- get_customer_context:
- request_handover:
- record_outcome:

## 3.5 Knowledge and Grounding
- retrieval strategy:
- source tracking:
- answer contract:
- faq miss handling:

## 3.6 Handover Design
- trigger policy:
- payload schema:
- downstream system contract:
- agent / human UX continuity:

## 3.7 Guardrails / Policy
- disclosure:
- unsupported promise:
- sensitive info:
- high-risk automation block:
- policy versioning:

## 3.8 Observability / Analytics
- required events:
- trace schema:
- minimal funnel:
- dashboards:

## 3.9 NFRs
- reliability:
- performance:
- privacy / security:
- maintainability:

---

# Phase 4 — Coding Agent Implementation Packet

## 4.1 Scope Freeze
- This implementation covers:
- This implementation explicitly does NOT cover:

## 4.2 Module Breakdown
- module 1:
- module 2:
- module 3:
- module 4:

## 4.3 Delivery Order
1.
2.
3.
4.

## 4.4 Required Contracts
- DB schema:
- API contracts:
- tool schemas:
- trace/event schemas:
- config schemas:

## 4.5 Required Tests
- smoke tests:
- control evals:
- handover contract tests:
- grounding/policy tests:
- regression suite:

## 4.6 Done Criteria
- implementation complete when:
- blocked release when:

---

# Phase 5 — Eval / Release / Feedback Loop

## 5.1 Offline Eval
- core FAQ suite:
- clarification suite:
- escalation suite:
- handover suite:
- control suite:
- production replay suite:

## 5.2 Online Monitoring
- containment by use case:
- escalation by use case:
- abandonment after clarification:
- wrong containment:
- repeat contact proxy:
- latency / timeout / tool error:

## 5.3 Release Gates
- active use case accuracy:
- groundedness pass rate:
- escalation recall:
- handover completeness:
- wrong containment threshold:
- latency threshold:

## 5.4 Feedback Loop
- bad-case intake:
- review cadence:
- who updates eval bank:
- who updates use case registry:
- who updates technical design:

---

# Phase 6 — Current Project Working Area

## 6.1 当前项目名称
Gumtree Customer Service Live Chat Bot（Salesforce Enhanced Chat 自研 Bot）

## 6.2 当前阶段
**Phase 0 / 1 / 2 已完成（v6 迭代，2026-04-19；v5 迭代，2026-04-19；v4 迭代，2026-04-17）**。

**✅ Phase 3 已完成（v1，2026-04-19）**：`phase3_detailed_technical_design.md` 已产出，覆盖 Runtime Architecture / State Model / Control Kernel / Tools / Knowledge / Handover / Guardrails / Observability / NFR 全部章节。Phase 4 Coding Agent Implementation Packet 可启动。

v6 解决了最后 5 项阻塞项（Embedding 模型 / 流量策略 / article→UC 映射 / gumshield API dev-phase / human review 决策），Phase 3 的全部前置信息现已齐备。

最近一次迭代（v6，2026-04-19）主要更新（基于 Embedding 选型 + A/B testing 策略 + article→UC 映射初版 + gumshield API dev-phase 方案 + human review 跳过决策）：
- **Phase 0 / 1 / 2 更新**：
  - **Embedding 模型确认**（design intent）：**PRODUCTION_TARGET / FUTURE_DESIGN** — Vertex AI `text-embedding-004`（768 维，GCP native）；**CURRENT_LOCAL / CURRENT_DEMO** runtime uses **DashScope `text-embedding-v3` (768-dim)** via `DashScopeEmbeddingClient` (`@Profile("local")`). The Vertex AI swap is not implemented in this repo.
  - **流量分配策略确认**：GrowthBook 管理，10%→20%→50%→100% 渐进放量；代码不硬编码阈值
  - **Article → UC 映射初版完成**：`data/knowledge/article_uc_mapping.csv`（218 篇 auto-mapped）+ `knowledge_base_articles.json`（见 `data/knowledge/mapping_summary_report.md`）
  - **gumshield API dev-phase 方案**：mock 数据先行开发和 debug；正式接入审批流程进行中，prod 部署前完成
  - **Human review 决策**：~~当前跳过 367 条 queue 人工标注~~ **v8 更新（2026-04-22）：367 sessions 已全部标注完成**（`data/human_review_annotations_2026-04-22_complete.csv` — **MISSING_ASSET / TODO_REVIEW**: actual file in repo is `data/human_review_annotations_2026-04-22_golden.csv`; confirm rename or update reference）；作为 supplementary ground truth 用于 eval 校准，主 ground truth 仍为 `csagent/data/eval_datasets/`（7 类 datasets / 601 sessions / 11,288 turns）
- **Phase 3 启动条件达成**：§6.4 所有阻塞项已解决

最近一次迭代（v5，2026-04-19）主要更新（基于合规审批结果 + GDPR/PII 决策 + repo 创建 + pgvector 索引策略 + CSAT 机制确认 + queue routing 统一 + Bot_Session__c 追加决策）：
- **Phase 0 更新**：
  - `fixed_script_library` 14 类模板 / 50+ 话术 **合规审批通过**；UC-FP-01 / UC-H-01 话术终稿确认
  - **HNSW + cosine 索引**确认（`m=16, ef_construction=64`）；**两段式 faq_miss 判定**（Retrieval Gate + Answer Gate `grounding_score < 3.5`）已入规范
  - **Queue 路由统一**：online → CS_NEW_chat；offline → CS_Cases_New；不设 per-UC 专属 queue
  - **Bot_Session__c / Bot_Event__c 追加创建**确认：升级为 session + event 显式状态模型
  - **email 跟进**：遵循现有 Salesforce CS 系统；Bot 不直接向用户发邮件
- **Phase 1 更新**：
  - UC-G GDPR intake 字段边界确认（Bot 采集 6 类最小必要字段 / 人工核验：身份真实性 + 实际执行）
  - **CSAT 机制**确认：bot-resolved → session 结束后邮件 CSAT；转人工 → 不下发 Bot CSAT
  - **PII 脱敏**：遵循 GDPR 要求；`safe_summary` 原则
  - **Bot repo 已创建**，名称：`csagent`
  - 数据模型：Bot_Session__c / Bot_Event__c 追加确认
- **Phase 2 更新**：
  - §2.6 UC-G-01 clarification_policy：Bot 可采集字段 vs 人工核验字段明确分边界
  - §2.6 close_policy：CSAT 机制入文
  - §2.9 Guardrails：GDPR / PII 规则更新
  - §2.10.3 Human-Only Tool：email 跟进遵循现有 SF CS 系统
  - §2.10.5 Queue routing：per-UC queue 决策已关闭（统一）
  - §2.11 Phase 3 移交清单：仍待补齐从 13 项压缩至 5 项

最近一次迭代（v4）的主要更新（基于 tool_spec v0.2 + salesforce-part-spec + pgvector plan + common phrases + FAQ CSV + platform API ref + eval datasets）：
- **Phase 0**：
  - 规范源引用升级 v0.1 → **v0.2** + 新增 salesforce-part-spec / pgvector plan / platform API ref
  - §0.3 runtime_only 工具从 3 → **4**（新增 `get_moderation_review_context`）；新增 runtime capabilities 行（`fixed_script_library` / `form_context_ingestion` / `tool_policy_enforcer` / `progress_placeholder`）
  - §0.3 context projection contract 新增 `form_context`
  - §0.3 新增 3 条已确认硬约束：Salesforce 平台（Enhanced Chat max 50 turns / Knowledge API 不可用 / queue 名称 / pre-chat form = turn 0）、向量检索选型（pgvector）、平台 API 基线（196 端点 / 14 微服务）
- **Phase 1**：
  - 输入来源从 9 → **15**（新增 tool_spec v0.2 / salesforce-part-spec / pgvector plan / common phrases / FAQ CSV / platform API ref / eval datasets）
  - §1.2 Knowledge：218 篇文章 CSV 已可用（原误计 3,963 为 CSV 行数）；Knowledge API **不可用**确认；pgvector 为唯一检索后端；新增 `get_moderation_review_context` 审核原因数据源
  - §1.3 Operating Model：queue 名称已确认（CS_NEW_chat / CS_Cases_New）；off-hours 判定逻辑已确认；Bot 最大 50 turns 约束已确认；已确认 Case 字段与 Bot 输出 JSON 结构
  - §1.3.3 数据模型：新增已确认的 `Chat_Message_Log__c` 自定义对象 + Case 字段（11 个 Topic Subject 选项）+ Bot 输出 JSON
  - §1.4.3 向量检索：从 TBD 升级为 **已确认 pgvector on Cloud SQL**（含分块策略 / 双层表 / 在线检索流程）
  - §1.4.13（用户已新增）Pre-chat Form & Case Creation Flow
  - §1.4.14 Tool Layer：升级对齐 v0.2（4 runtime-only 工具 / runtime capabilities / concrete API dependencies / 新增 runtime policies）
  - §1.5.1 Eval 数据集：从"目标规模"升级为 **全部已构建**（601 sessions / 11,288 turns + 367 human review queue，**v8 已全部标注完成**）；标注数据质量发现
- **Phase 2**：
  - §2.5 Knowledge Scope Map：新增 Knowledge API 不可用说明 + pgvector 为唯一后端 + `get_moderation_review_context` 对 UC-FP 增强
  - §2.6（用户已新增）通用 discover_policy 的 pre-chat form integration（form_context_usage / strong_routing_signals / weak_routing_signals）+ clarification_policy 的新版表单影响
  - §2.9 Guardrails：对齐 `customer_service_agent-Common-Phrases.md` + `fixed_script_library`
  - §2.10 Tool Allocation 全面升级对齐 v0.2：`get_customer_context` disallowed UC-B/UC-E 明确化；新增 `get_moderation_review_context` × UC 矩阵；per-UC tool call sequence 加入 form_context INIT 触发；`create_case_controlled` required_fields 对齐 Salesforce Case 字段 + Topic Subject；新增 §2.10.7 Runtime Capabilities
  - §2.11 移交清单：标注已解决 vs 仍待补齐的阻塞项

## 6.3 已有输入
- **规范层**: `customer_service_agent_tech_spec.md`（Whole Tech Spec）+ `customer_service_agent_eval_spec.md`（Eval Spec）+ **`customer_service_tool_spec_v0_2.yaml`（V1 Tool Spec v0.2）**
- **业务 BRD**: `BRD.md`
- **PRD 业务部分**: `PRD_biz_part.md`
- **数据分析**: `case-data-stat.md`（120,367 条 Case 结构化分析）
- **Case 样本**: `case-samples.md` — **MISSING_ASSET / TODO_REVIEW** (file not present in repo, 2026-05-10 audit)
- **Live Chat 会话样本**: `customer_service_conversation_samples_organized.xlsx`（499 会话，262 条 transcript）— **MISSING_ASSET / TODO_REVIEW** (file not present in repo, 2026-05-10 audit)
- **工具候选证据**: `inferred_tool_candidates_from_human_conversations.xlsx`（10 类工具候选）— **MISSING_ASSET / TODO_REVIEW** (file not present in repo, 2026-05-10 audit)
- **Salesforce 配置确认（v4 新增）**: `salesforce-part-spec.md`（Enhanced Chat / Omni-Channel / Knowledge API / Case 字段 / Queue / Off-hours / Pre-chat Form）
- **向量检索方案（v4 新增）**: `problem_retrieval_solution_plan_pgvector.md`
- **标准话术指导（v4 新增）**: `customer_service_agent-Common-Phrases.md`（从 1.18 万条 transcript 提炼）
- **Knowledge 文章库（v4 新增，v6 修正）**: `FAQ-knowledge_include_help_url.csv`（218 篇文章；原 3,963 系 CSV 行数）
- **平台 API 参考（v4 新增）**: `platform_api_detailed_reference.md`（196 端点 / 14 微服务）
- **Eval 数据集（v4 新增）**: `data/eval_datasets/*`（7 类数据集，601 sessions / 11,288 turns + **367 sessions human review 已完成标注 v8**）
- **工程约束**: `07-engineering-constraints.md`
- **阶段产出**: `phase0_normative_freeze.md`、`phase1_solution_input_pack.md`、`phase2_domain_realization_spec.md`

## 6.4 缺失输入状态（v5 更新，2026-04-19）

### 已解决（v4）

| # | 原缺失项 | 解决方式 | 来源文件 |
|---|---------|---------|---------|
| 1 | Help Centre 文章清单与 URL 映射 | **218 篇文章 CSV 已可用**（Id / Title / Summary / Help_Site_URL__c；原 3,963 系 CSV 行数误计）；article → UC 映射已完成初版（v6） | `FAQ-knowledge_include_help_url.csv` |
| 4 | `create_case_controlled` per-UC required_fields | **tool_spec v0.2 已定义初版**（UC-H / UC-J / UC-K）；queue 统一用 CS_NEW_chat / CS_Cases_New | `customer_service_tool_spec_v0_2.yaml` §per_uc_required_fields |
| 6 | Salesforce 组织配置确认 | **Enhanced Chat Web v1 / Omni-Channel 标准 / Knowledge API 无 / max 50 turns / 坐席并发 2** | `salesforce-part-spec.md` |
| 7 | 向量库选型 | **pgvector on Cloud SQL PostgreSQL 已确认** | `problem_retrieval_solution_plan_pgvector.md` |
| 9 | Golden Dataset 扩充 | **7 类数据集全部已构建**（601 sessions / 11,288 turns）；**v8：367 sessions human review 已全部标注完成** | `data/eval_datasets/DATASET_REPORT.md` |
| 11 | Off-hours 策略 | **检查 New Chat queue agent 在线状态**；离线时 Case → CS_Cases_New；Bot 给 offline 话术 | `salesforce-part-spec.md` |
| 12 | Salesforce 自定义对象命名 | **`Chat_Message_Log__c` 已存在**；Bot_Session__c / Bot_Event__c 待 Salesforce Admin 确认是否追加 | `salesforce-part-spec.md` |

### 已解决（v5，2026-04-19）

| # | 原缺失项 | 解决方式 | 影响文件 |
|---|---------|---------|---------|
| 2 | UC-FP-01 话术终稿 + UC-H-01 安抚话术终稿 | **合规审批通过**；`fixed_script_library_v1.md` §2-3 为合规版本 | `phase0_normative_freeze.md` / `phase2_domain_realization_spec.md` §2.9 |
| 3 | UC-G/H/I/J/K 固定话术库（`fixed_script_library`）合规审批 | **合规审批通过**（14 类模板 / 50+ 话术 / 禁止话术清单）| `phase0_normative_freeze.md` §0.3 runtime capabilities |
| 5 | UC-G-01 GDPR intake 字段边界 | **Bot 可采集**：请求类型 / 注册邮箱 / ad ID / 问题概述 / 是否本人发起 / 可回联邮箱；**人工核验**：身份真实性 / 实际数据删除执行 / 代理人授权 | `phase2_domain_realization_spec.md` §2.6 UC-G-01 |
| 13 | CSAT 采集机制与阈值 | **机制确认**：bot-resolved 会话 → session 结束后邮件 CSAT；转人工 → 不下发 Bot CSAT；具体阈值待业务定 | `phase1_solution_input_pack.md` §1.1.4 / `phase2_domain_realization_spec.md` §2.6 |
| 14 | PII redaction 规则与字段清单 | **遵循 GDPR 隐私保护要求执行**；transcript / 日志脱敏；`safe_summary` 设计原则 | `phase1_solution_input_pack.md` §1.4.11 / `phase2_domain_realization_spec.md` §2.9 |
| 15 | Bot 项目新 repo 是否创建及命名 | **repo 已创建，名称：`csagent`** | `phase1_solution_input_pack.md` §1.4.10 |
| 16 | `send_followup_email_or_async_update` 触发主体与 email 模板 ID | **遵循现有 Salesforce 人工 CS 系统**的规则/模版/内容；Bot 不直接发邮件；坐席 / back-office 触发 | `phase0_normative_freeze.md` §0.3 / `phase2_domain_realization_spec.md` §2.10.3 |
| 18 | pgvector 索引策略（IVFFlat vs HNSW）+ faq_miss score_threshold | **HNSW + cosine**（`m=16, ef_construction=64`）；**两段式 faq_miss 判定**（Retrieval Gate + Answer Gate `grounding_score < 3.5`）| `phase0_normative_freeze.md` §0.3 / `phase1_solution_input_pack.md` §1.4.3 |
| 21 | per-UC 专属 queue 是否需要 | **统一规则**：online → CS_NEW_chat；offline → CS_Cases_New；不设 per-UC 专属 queue | `phase2_domain_realization_spec.md` §2.10.5 |
| 22 | Bot_Session__c / Bot_Event__c 是否追加创建 | **确认追加创建**：升级为 session + event 显式状态模型，便于 control kernel / observability / replay | `phase1_solution_input_pack.md` §1.3.3 / `phase0_normative_freeze.md` §0.3 |

### 已解决（v6，2026-04-19）

| # | 原缺失项 | 解决方式 | 影响文件 |
|---|---------|---------|---------|
| 8 | Embedding 模型选型 | **PRODUCTION_TARGET / FUTURE_DESIGN**: Vertex AI `text-embedding-004`（GCP native，768 维，入库与在线 Query 同一模型）。**CURRENT_LOCAL / CURRENT_DEMO**: DashScope `text-embedding-v3` (768-dim) via `DashScopeEmbeddingClient` (`@Profile("local")`); see `docs/runbooks/admin-guide.md` §4.1 / §10. The Vertex AI swap is NOT implemented in this repo. | `phase0_normative_freeze.md` §0.3 / `phase1_solution_input_pack.md` §1.4.3 |
| 10 | 流量分配策略与 go/no-go 阈值 | **Policy defined; csagent runtime integration pending (PRODUCTION_GAP / FUTURE_DESIGN).** **GrowthBook** 管理；10%→20%→50%→100% 渐进放量；代码不硬编码阈值（见 `docs/runbooks/abtesing-policy.md`）。`server/` 与 `ui/` 目前没有 GrowthBook SDK 引用、不读取 `cs_bot_enabled` flag — see `docs/runbooks/abtesing-policy.md` for the not-started status. | `phase1_solution_input_pack.md` §1.1.4 Go/No-Go |
| 17 | article → UC 映射（218 篇文章 UC 分类标注）| **初版已完成**：`data/knowledge/article_uc_mapping.csv`（218 篇全量 auto-mapped，0 unmatched）+ `knowledge_base_articles.json`（pgvector 入库就绪）；详见 `data/knowledge/mapping_summary_report.md` | `phase2_domain_realization_spec.md` §2.5 |
| 19 | gumshield cs-review API Bot 服务账号访问审批 | **Dev/demo 阶段**：使用 mock 数据开发和 debug；**正式审批**：流程进行中，prod 部署前完成配置（非阻塞 Phase 3 设计）| `phase2_domain_realization_spec.md` §2.10.2 |
| 20 | Golden Dataset human review 标注完成（367 条 queue）| ~~当前阶段跳过人工标注~~ **v8 更新（2026-04-22）：367 sessions 已全部标注完成**（`data/human_review_annotations_2026-04-22_complete.csv` — **MISSING_ASSET / TODO_REVIEW**: actual file in repo is `data/human_review_annotations_2026-04-22_golden.csv`）；作为 supplementary ground truth 用于 eval 校准 | `phase1_solution_input_pack.md` §1.5.1 / `phase5_evaluation_design.md` |

### 🟢 仍待补齐：无 Phase 3 设计阻塞项

**§6.4 所有 Phase 3 设计阻塞项已解决，Phase 3 Detailed Technical Design 可以正式启动。**

> **Production-readiness scope (NOT closed by this section).** The
> bullets above only certify that no Phase 3 *design* item remains
> open. Several entries above were closed at design-intent level
> while their production runtime is still **PRODUCTION_GAP /
> FUTURE_DESIGN** in the current repo. Do not read this section as
> "production is ready". Open items include, but are not limited to:
>
> - **#8 Embedding model**: PRODUCTION_TARGET is Vertex AI
>   `text-embedding-004`; CURRENT_LOCAL / CURRENT_DEMO runtime uses
>   DashScope `text-embedding-v3`. Vertex AI swap not implemented.
> - **#10 GrowthBook rollout**: Policy defined; csagent server/UI
>   integration not implemented (no `growthbook` references in
>   `server/` or `ui/`). See `docs/runbooks/abtesing-policy.md`.
> - **#19 gumshield API**: dev/demo uses mock data only; production
>   service-account approval still in flight.
> - **Salesforce live integration**: `MockSalesforceService` is the
>   only live `SalesforceService` implementation; real Enhanced
>   Chat / Omni-Channel queues, `Chat_Message_Log__c`, and
>   `Bot_Session__c` / `Bot_Event__c` objects are PRODUCTION_GAP.
>   See `docs/runbooks/salesforce-part-spec.md` and
>   `docs/runbooks/admin-guide.md` §13.4.
>
> Track these in §6.5 below as PRODUCTION_GAP / FUTURE_DESIGN, not
> as design blockers.

## 6.5 下一步行动

**Phase 0–5 设计文档已全部完成。下一步是实现。**

1. **🟢 启动实现（按 Phase 4 模块顺序）**：
   - 按 `phase4_coding_agent_implementation_packet.md` §4.3 交付顺序：M1(Foundation) → M2+M3(parallel) → M4(Runtime) → M5+M6+M7(parallel) → M8(Eval) → M9(E2E)
   - 关键路径：M1 → M4 → M5 → M8 → M9
   - 实现前需用户确认启动

2. **并行推进（外部依赖，不阻塞设计但需在编码前到位）**：
   - **gumshield API 正式审批**（#19）：dev + prod 环境服务账号配置，prod 部署前完成
   - **pgvector extension**：Cloud SQL 实例启用 pgvector
   - **Vertex AI API**（**PRODUCTION_GAP / FUTURE_DESIGN**）：项目启用 Embedding + Gemini API。Not part of CURRENT_LOCAL / CURRENT_DEMO runtime; the live embedding path is DashScope `text-embedding-v3` and the chat path is DeepSeek primary / Kimi fallback (see `docs/runbooks/admin-guide.md` §4.1).
   - **Salesforce Connected App**：OAuth 凭证创建
   - **Bot_Session__c / Bot_Event__c 对象创建**：Salesforce Admin 按 Phase 3 §3.2.1 建字段
   - **GrowthBook feature flag**（**PRODUCTION_GAP / FUTURE_DESIGN**）：创建 `cs_bot_enabled` flag。Policy lives in `docs/runbooks/abtesing-policy.md`; csagent server/UI does not yet consume it (`growthbook` is not imported in `server/` or `ui/`).
   - **Kafka topic + Avro schema**：analytics 事件通道
   - **GCP Secret Manager entries**：所有 API key / token

3. **Article → UC 映射校准（可并行）**：
   - 218 篇全量初版已可用；后续人工校准 87 篇 multi-UC 边界
