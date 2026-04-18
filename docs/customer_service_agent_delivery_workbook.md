# Customer Service Agent Delivery Workbook

> 目标：把已有的 Normative Layer，稳定转化为某个具体 customer service agent 项目的 coding-ready technical design，并形成可交给 coding agent 的实现包。

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
| Phase 0 — Freeze Normative Layer | ✅ 已完成（v4）| [`phase0_normative_freeze.md`](./phase0_normative_freeze.md) |
| Phase 1 — Build Solution Input Pack | ✅ 已完成（v4）| [`phase1_solution_input_pack.md`](./phase1_solution_input_pack.md) |
| Phase 2 — Produce Domain Realization Spec | ✅ 已完成（v4）| [`phase2_domain_realization_spec.md`](./phase2_domain_realization_spec.md) |
| Phase 3 — Produce Detailed Technical Design | ⏸ 暂缓 | 待用户补齐缺失输入（见 §6.4）后启动 |
| Phase 4 — Coding Agent Implementation Packet | ⏸ 暂缓 | 依赖 Phase 3 |
| Phase 5 — Eval / Release / Feedback Loop | ⏸ 暂缓 | 依赖 Phase 3、4 |

本 workbook 保留作为**阶段模板与索引**；每个阶段的实际产出在独立文件中维护，便于 review 与迭代。

## 规范层文件（外部依赖）

| 文件 | 角色 |
|------|------|
| [`customer_service_agent_tech_spec.md`](./customer_service_agent_tech_spec.md) | 通用 Customer Service Agent 技术规范（V1 内核 / 架构 / 控制 / 工具 / Handover / Guardrails / Observability / NFR / Release Criteria）|
| [`customer_service_agent_eval_spec.md`](./customer_service_agent_eval_spec.md) | 通用 Eval 规范（Eval scope / Dataset / Grader / Metrics / Launch Gates / CI/CD）|

## 业务输入文件

| 文件 | 角色 |
|------|------|
| [`BRD.md`](./BRD.md) | Business Requirements — scope、user stories、UX、tone-of-voice、launch criteria |
| [`PRD_biz_part.md`](./PRD_biz_part.md) | Business-only PRD — 系统角色、handling 状态机、A–F' 对话流、Salesforce 数据模型骨架 |
| [`case-data-stat.md`](./case-data-stat.md) | 120,367 条 Case 历史数据分析（Case Reason 维度）|
| [`case-samples.md`](./case-samples.md) | 原始 Case 样本 |
| [`customer_service_conversation_samples_organized.xlsx`](./customer_service_conversation_samples_organized.xlsx) | **新增** — 499 条 Live Chat 会话（含 262 条 transcript、2,890 turns、43 条 curated examples）|
| [`inferred_tool_candidates_from_human_conversations.xlsx`](./inferred_tool_candidates_from_human_conversations.xlsx) | **新增** — 从 262 条对话反推的 10 类工具候选 + 证据 |
| [`customer_service_tool_spec_v0_2.yaml`](./customer_service_tool_spec_v0_2.yaml) | **v4 更新** — V1 Tool Spec v0.2（concrete API mapping / pre-chat form / pgvector backend / `get_moderation_review_context` / runtime capabilities）|
| [`salesforce-part-spec.md`](./salesforce-part-spec.md) | **v4 新增** — Salesforce 组织配置确认（Enhanced Chat / Omni-Channel / Knowledge API / Case 字段 / Queue / Off-hours / Pre-chat Form）|
| [`problem_retrieval_solution_plan_pgvector.md`](./problem_retrieval_solution_plan_pgvector.md) | **v4 新增** — pgvector 检索方案确认 |
| [`customer_service_agent-Common-Phrases.md`](./customer_service_agent-Common-Phrases.md) | **v4 新增** — 从 1.18 万条 transcript 提炼的标准话术指导（7 类模板）|
| [`FAQ-knowledge_include_help_url.csv`](./FAQ-knowledge_include_help_url.csv) | **v4 新增** — 3,963 篇 Salesforce Knowledge 文章（Id / Title / Summary / Help_Site_URL__c）|
| [`platform_api_detailed_reference.md`](./platform_api_detailed_reference.md) | **v4 新增** — 196 REST 端点 / 14 微服务详细 API 参考 |
| [`fixed_script_library_v1.md`](./fixed_script_library_v1.md) | **v4 新增** — Bot V1 固定话术模板库（14 类 / 50+ 模板 / 禁止话术清单），从 1,230+ 条真实 agent 消息提炼 |
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
**Phase 0 / 1 / 2 已完成（v4 迭代，2026-04-17）**。

Phase 3（Detailed Technical Design）及之后阶段**暂缓**，等待用户补齐 §6.4 中的剩余信息后再启动。v4 已解决了 v3 时代多项阻塞项。

最近一次迭代（v4）的主要更新（基于 tool_spec v0.2 + salesforce-part-spec + pgvector plan + common phrases + FAQ CSV + platform API ref + eval datasets）：
- **Phase 0**：
  - 规范源引用升级 v0.1 → **v0.2** + 新增 salesforce-part-spec / pgvector plan / platform API ref
  - §0.3 runtime_only 工具从 3 → **4**（新增 `get_moderation_review_context`）；新增 runtime capabilities 行（`fixed_script_library` / `form_context_ingestion` / `tool_policy_enforcer` / `progress_placeholder`）
  - §0.3 context projection contract 新增 `form_context`
  - §0.3 新增 3 条已确认硬约束：Salesforce 平台（Enhanced Chat max 50 turns / Knowledge API 不可用 / queue 名称 / pre-chat form = turn 0）、向量检索选型（pgvector）、平台 API 基线（196 端点 / 14 微服务）
- **Phase 1**：
  - 输入来源从 9 → **15**（新增 tool_spec v0.2 / salesforce-part-spec / pgvector plan / common phrases / FAQ CSV / platform API ref / eval datasets）
  - §1.2 Knowledge：3,963 篇文章 CSV 已可用；Knowledge API **不可用**确认；pgvector 为唯一检索后端；新增 `get_moderation_review_context` 审核原因数据源
  - §1.3 Operating Model：queue 名称已确认（CS_NEW_chat / CS_Cases_New）；off-hours 判定逻辑已确认；Bot 最大 50 turns 约束已确认；已确认 Case 字段与 Bot 输出 JSON 结构
  - §1.3.3 数据模型：新增已确认的 `Chat_Message_Log__c` 自定义对象 + Case 字段（11 个 Topic Subject 选项）+ Bot 输出 JSON
  - §1.4.3 向量检索：从 TBD 升级为 **已确认 pgvector on Cloud SQL**（含分块策略 / 双层表 / 在线检索流程）
  - §1.4.13（用户已新增）Pre-chat Form & Case Creation Flow
  - §1.4.14 Tool Layer：升级对齐 v0.2（4 runtime-only 工具 / runtime capabilities / concrete API dependencies / 新增 runtime policies）
  - §1.5.1 Eval 数据集：从"目标规模"升级为 **全部已构建**（601 sessions / 11,288 turns + 367 human review queue）；标注数据质量发现
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
- **Case 样本**: `case-samples.md`
- **Live Chat 会话样本**: `customer_service_conversation_samples_organized.xlsx`（499 会话，262 条 transcript）
- **工具候选证据**: `inferred_tool_candidates_from_human_conversations.xlsx`（10 类工具候选）
- **Salesforce 配置确认（v4 新增）**: `salesforce-part-spec.md`（Enhanced Chat / Omni-Channel / Knowledge API / Case 字段 / Queue / Off-hours / Pre-chat Form）
- **向量检索方案（v4 新增）**: `problem_retrieval_solution_plan_pgvector.md`
- **标准话术指导（v4 新增）**: `customer_service_agent-Common-Phrases.md`（从 1.18 万条 transcript 提炼）
- **Knowledge 文章库（v4 新增）**: `FAQ-knowledge_include_help_url.csv`（3,963 篇文章）
- **平台 API 参考（v4 新增）**: `platform_api_detailed_reference.md`（196 端点 / 14 微服务）
- **Eval 数据集（v4 新增）**: `data/eval_datasets/*`（7 类数据集，601 sessions / 11,288 turns + 367 条 human review queue）
- **工程约束**: `07-engineering-constraints.md`
- **阶段产出**: `phase0_normative_freeze.md`、`phase1_solution_input_pack.md`、`phase2_domain_realization_spec.md`

## 6.4 缺失输入状态（v4 更新）

### 已解决（v4）

| # | 原缺失项 | 解决方式 | 来源文件 |
|---|---------|---------|---------|
| 1 | Help Centre 文章清单与 URL 映射 | **3,963 篇文章 CSV 已可用**（Id / Title / Summary / Help_Site_URL__c）；article → UC 映射待构建 | `FAQ-knowledge_include_help_url.csv` |
| 4 | `create_case_controlled` per-UC required_fields | **tool_spec v0.2 已定义初版**（UC-H / UC-J / UC-K）；queue 统一用 CS_NEW_chat / CS_Cases_New | `customer_service_tool_spec_v0_2.yaml` §per_uc_required_fields |
| 6 | Salesforce 组织配置确认 | **Enhanced Chat Web v1 / Omni-Channel 标准 / Knowledge API 无 / max 50 turns / 坐席并发 2** | `salesforce-part-spec.md` |
| 7 | 向量库选型 | **pgvector on Cloud SQL PostgreSQL 已确认** | `problem_retrieval_solution_plan_pgvector.md` |
| 9 | Golden Dataset 扩充 | **7 类数据集全部已构建**（601 sessions / 11,288 turns）；367 条 human review queue 待标注 | `data/eval_datasets/DATASET_REPORT.md` |
| 11 | Off-hours 策略 | **检查 New Chat queue agent 在线状态**；离线时 Case → CS_Cases_New；Bot 给 offline 话术 | `salesforce-part-spec.md` |
| 12 | Salesforce 自定义对象命名 | **`Chat_Message_Log__c` 已存在**；Bot_Session__c / Bot_Event__c 待 Salesforce Admin 确认是否追加 | `salesforce-part-spec.md` |

### 仍待补齐（阻塞 Phase 3 启动）

| # | 缺失项 | 影响的 Phase 3 章节 | 责任方建议 |
|---|-------|-------------------|-----------|
| 2 | **UC-FP-01 标准解释话术终稿 + UC-H-01 安抚话术终稿** — **初版已完成**，见 `fixed_script_library_v1.md` §2-3 + `customer_service_agent-Common-Phrases.md` §7.1-7.2；从 18 条 UC-FP + 35 条 UC-H 真实对话提炼；**待合规终审** | 3.7 Guardrails / 3.4 Tools | 合规 + 产品 |
| 3 | **UC-G/H/I/J/K 固定话术库（`fixed_script_library`）合规审批** — **初版已完成**，见 `fixed_script_library_v1.md`（14 类模板 / 50+ 话术 / 禁止话术清单）+ `customer_service_agent-Common-Phrases.md` §7-8；从 1,230+ 条真实 agent 消息提炼；**待合规终审** | 3.4 Tools / 3.7 Guardrails | 合规 + 产品 + Ops |
| 5 | **UC-G-01 GDPR intake 字段边界**（哪些由 Bot 采集 vs 必须人工核验）| 3.4 Tools (UC-G policy) | 合规（Privacy）+ 产品 |
| 8 | **Embedding 模型选型** | 3.5 Retrieval Strategy | 工程 + 数据科学 |
| 10 | **流量分配策略与 go/no-go 阈值** | 3.1 Runtime + Phase 5 Release Gates | Product + Support + Data |
| 13 | **CSAT 采集机制与阈值** | Phase 5 Release Gates | Data + Product |
| 14 | **PII redaction 规则与字段清单** | 3.7 Guardrails / 3.8 Observability | Legal + 工程 |
| 15 | **Bot 项目新 repo** 是否创建及命名 | Phase 4 | 工程 |
| 16 | **`send_followup_email_or_async_update` 触发主体与 email 模板 ID** | 3.6 Handover | Ops + 产品 |
| 17 | **article → UC 映射**（3,963 篇文章的 UC 分类标注） | 3.5 Knowledge & Grounding | Knowledge Ops + 工程 |
| 18 | **pgvector 索引策略**（IVFFlat vs HNSW）+ faq_miss score_threshold | 3.5 Retrieval Strategy | 工程 |
| 19 | **gumshield cs-review API Bot 服务账号访问审批** | 3.4 Tools (`get_moderation_review_context`) | Security + Gumshield team |
| 20 | **Golden Dataset human review 标注完成**（367 条 queue，P0 优先 150 Golden）| Phase 5 Eval | Product/Ops + QA + Salesforce 三角色 |
| 21 | **per-UC 专属 queue 是否需要**（或统一 CS_Cases_New / CS_NEW_chat） | 3.6 Handover queue routing | Salesforce Admin + Ops |
| 22 | **Bot_Session__c / Bot_Event__c 是否追加创建**（或仅用现有 `Chat_Message_Log__c`） | 3.2 State Model | Salesforce Admin + 产品 |

## 6.5 下一步行动

1. **Review v4 输出**：与业务方（Chelsea Fagan-Hall）、产品方（Lance Li）确认 Phase 0 / 1 / 2 v4 内容，重点是：
   - tool_spec v0.2 的 `get_moderation_review_context` 新工具（gumshield cs-review API 访问审批）
   - pre-chat form integration 的 form_context → 自动 `get_customer_context` 触发设计
   - `fixed_script_library` 14 类话术模板的合规审批路径
   - Salesforce 确认项：Bot max 50 turns / Knowledge API 不可用 / queue 统一 or per-UC
2. **优先推动 §6.4 仍待补齐项**：
   - **P0**（直接阻塞 Phase 3 实现）：#2 话术合规终审 / #3 fixed_script_library 合规审批 / #8 Embedding 模型选型 / #19 gumshield API 访问审批
   - **P1**（阻塞 eval 完成）：#20 Golden Dataset human review 标注（367 条，估计 ~3 周 @15 sessions/day/reviewer）
   - **P2**（Phase 3 中期需要）：#5 UC-G GDPR intake 边界 / #17 article→UC 映射 / #18 pgvector 索引策略 / #21 per-UC queue 决策 / #22 Bot_Session__c 追加决策
3. **暂不启动** Phase 3 / 4 / 5 — 待 §6.4 P0 项到位后再开。
4. **可并行推进**（不依赖 Phase 3）：
   - **Human review 标注启动**：按 `HUMAN_REVIEW_GUIDE.md` 分工（Product/Ops → Golden+Intake；QA → Bad-case+Drift；Salesforce → Handover+Escalation）
   - **pgvector 离线入库 POC**：用 3,963 篇 FAQ CSV 跑通分块 → Embedding → pgvector 写入 → 检索 Top-3 链路（`problem_retrieval_solution_plan_pgvector.md` M1–M2）
   - **fixed_script_library 初稿**：基于 `customer_service_agent-Common-Phrases.md` 提炼 14 类模板，提交合规审批
   - **article → UC 映射**：从 3,963 篇 title/URL heuristics 自动标注初版 UC 分类，再人工校准
