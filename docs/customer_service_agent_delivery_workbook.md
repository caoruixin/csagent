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
| Phase 0 — Freeze Normative Layer | ✅ 已完成（v3）| [`phase0_normative_freeze.md`](./phase0_normative_freeze.md) |
| Phase 1 — Build Solution Input Pack | ✅ 已完成（v3）| [`phase1_solution_input_pack.md`](./phase1_solution_input_pack.md) |
| Phase 2 — Produce Domain Realization Spec | ✅ 已完成（v3）| [`phase2_domain_realization_spec.md`](./phase2_domain_realization_spec.md) |
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
| [`customer_service_tool_spec_v0_1.yaml`](./customer_service_tool_spec_v0_1.yaml) | **新增** — V1 Tool Spec v0.1（agent_visible + runtime_only + human_only 工具 / per-UC 可用性 / runtime policy）|
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
**Phase 0 / 1 / 2 已完成（v3 迭代，2026-04-14）**。

Phase 3（Detailed Technical Design）及之后阶段**暂缓**，等待用户补齐 §6.4 中的必要信息和数据后再启动。

最近一次迭代（v3）的主要更新（基于新增的 tool spec + conversation samples + inferred tools）：
- **Phase 0**：
  - 新增规范源引用 `customer_service_tool_spec_v0_1.yaml`
  - §0.2 补齐 `tool_surface_strategy: small_and_strong` 与 `sensitive_write_actions: human_only_or_phase2` 两条母规范
  - §0.3 V1 工具集拆分为 agent_visible / runtime_only / human_only 三层，并加入 per-UC 可用性矩阵与 tool risk tier 两行；V1 use case 集合扩展至 12 个（UC-A..F/FP + UC-G/H/I/J/K）
  - §0.4 新增 per-UC 工具可用性矩阵与 `create_case_controlled` required_fields 作为可领域化项
- **Phase 1**：
  - 输入来源列表加入 3 份新文件
  - §1.1.2 新增"Out-of-V1-Bot-Resolution 但需 intake + handover"表（UC-G/H/I/J/K），并加入"活体 Chat 样本分布"与"设计桶分布"两表
  - §1.1.3 加入 Live Chat 真实对话样本与工具候选证据小节，新增"关键对话模式"7 条（从 262 条 transcript 抽象）
  - §1.2 `当前已知 gaps` 增补 UC-H/J/K required_fields 合约缺失 + UC-G 身份核验接点缺失
  - §1.3 Handover 后流程补充 `create_case_controlled` 已创建 Case 的坐席视角 + `send_followup_email_or_async_update` 触发主体
  - §1.4.12 外部集成表补齐每一行到 tool_spec 的映射
  - §1.4.13 新增"Tool Layer Integration"小节（agent_visible / runtime_only / human_only 三层 + tool scope 治理 + runtime policy 映射）
  - §1.5.1 数据集表补 initial 规模 + 新增 Intake/Tool Contract Dataset + Missed-Session Dataset
- **Phase 2**：
  - §2.1 use case 范围从 7 个扩到 12 个；命名约定说明 `UC-X-01 ∈ UC-X`
  - §2.2 新增 UC-G-01 / UC-H-01 / UC-I-01 / UC-J-01 / UC-K-01 五个 use case registry 完整 schema（intake-only 或 intake+case）
  - §2.3 Risk Matrix 加入 critical 级别与扩充 High Risk 表；Forbidden Automation 表加入新 tool_spec 对应的禁令
  - §2.4 Escalation Matrix 新增 `intake_complete` / `intake_required_fields_missing_after_max_attempts` / `gdpr_or_data_request_detected` / `identity_verification_required` / `any_imminent_harm_signal` / `tool_scope_blocked` 等触发条件
  - §2.5 Knowledge Scope Map 加入 UC-G/H/I/J/K 行并显式标记 `search_knowledge` 不可用；Disallowed Knowledge 增加对应行
  - §2.6 新增 UC-G-01 / UC-H-01 / UC-I-01 / UC-J-01 / UC-K-01 的 Control Policy Override（intake 必填字段、固定话术、forbidden behaviors）
  - §2.8 Cross-UC Routing 扩展（UC-FP→UC-H、UC-D→UC-G、UC-F→UC-I、任意→UC-J imminent harm 等）
  - §2.9 Guardrails 加入 tool scope enforcement、最小 PII 暴露、占位话术、禁用"已删除/已限制"类假行动承诺（基于 transcript 观察）
  - **§2.10 新增"Tool Allocation per Use Case"整节** — agent_visible × UC 矩阵、runtime_only × UC 矩阵、human_only 工具、per-UC 期望调用序列、`create_case_controlled` required_fields 草案、tool runtime_policy 对照
  - §2.11（原 §2.10）移交清单更新为 v3 状态

## 6.3 已有输入
- **规范层**: `customer_service_agent_tech_spec.md`（Whole Tech Spec）+ `customer_service_agent_eval_spec.md`（Eval Spec）+ **`customer_service_tool_spec_v0_1.yaml`（V1 Tool Spec，新增）**
- **业务 BRD**: `BRD.md`（scope、user stories、functional/UI/legal requirements、success metrics、tone-of-voice）
- **PRD 业务部分**: `PRD_biz_part.md`（去除技术规范后的业务 PRD：capacity analysis、A–F' 对话流、系统角色、handling 状态机、Salesforce 数据模型骨架）
- **数据分析**: `case-data-stat.md`（120,367 条 Case 结构化分析）
- **Case 样本**: `case-samples.md`
- **Live Chat 会话样本（新增）**: `customer_service_conversation_samples_organized.xlsx`（499 会话，262 条 transcript，2,890 turns，43 curated examples）
- **工具候选证据（新增）**: `inferred_tool_candidates_from_human_conversations.xlsx`（10 类工具候选 + matched_sessions 证据）
- **工程约束**: `07-engineering-constraints.md`
- **阶段产出**: `phase0_normative_freeze.md`、`phase1_solution_input_pack.md`、`phase2_domain_realization_spec.md`

## 6.4 缺失输入（阻塞 Phase 3 启动）

| # | 缺失项 | 影响的 Phase 3 章节 | 责任方建议 |
|---|-------|-------------------|-----------|
| 1 | **Help Centre 文章清单与 URL 映射**（UC-A / UC-B / UC-C / UC-D / UC-E / UC-F / UC-FP）| 3.5 Knowledge & Grounding | Knowledge & Content Ops |
| 2 | **UC-FP-01 标准解释话术终稿 + UC-H-01 安抚话术终稿** | 3.7 Guardrails / 3.4 Tools | 合规 + 产品 |
| 3 | **UC-G-01 / UC-H-01 / UC-I-01 / UC-J-01 / UC-K-01 固定话术库合规审批**（`search_knowledge` 禁用场景的话术模板）| 3.4 Tools / 3.7 Guardrails | 合规 + 产品 + Ops |
| 4 | **`create_case_controlled` per-UC required_fields 终稿 + Queue 路由表**（UC-H / UC-J / UC-K）| 3.4 Tools / 3.6 Handover | Salesforce Admin + Ops + 产品 |
| 5 | **UC-G-01 GDPR intake 字段边界**（哪些字段由 Bot 采集、哪些必须人工核验）| 3.4 Tools (UC-G policy) | 合规（Privacy）+ 产品 |
| 6 | **Salesforce 组织配置确认**（Enhanced Chat / Omni-Channel / Knowledge API 版本与可用能力） | 3.1 Runtime Architecture / 3.6 Handover | Salesforce Admin |
| 7 | **向量库选型决策**（pgvector / Vertex AI Vector Search / GKE self-hosted） | 3.4 Tools (search_knowledge) / 3.5 Retrieval Strategy | 工程 + 架构评审 |
| 8 | **Embedding 模型选型** | 3.5 Retrieval Strategy | 工程 + 数据科学 |
| 9 | **Golden Dataset 初始版本扩充**（从 43 curated + 30 self_serve 样本扩展到 ≥ 150 条 + 40 条 Intake/Tool Contract cases） | Phase 5 Eval | Product / Ops + QA |
| 10 | **流量分配策略与 go/no-go 阈值** | 3.1 Runtime（流量门控）+ Phase 5 Release Gates | Product + Support + Data |
| 11 | **Off-hours 策略**（is_business_hours 判定 + 离线时段 Bot 行为边界） | 3.6 Handover Trigger Policy | Operations |
| 12 | **Salesforce 自定义对象/字段命名最终方案**（`Bot_Session__c` / `Bot_Event__c` / `Bot_Context__c` 是否采用，或使用既有对象） | 3.2 State Model / 3.6 Handover Payload | Salesforce Admin + 产品 |
| 13 | **CSAT 采集机制与阈值**（"X 点跌幅"具体值） | Phase 5 Release Gates | Data + Product |
| 14 | **PII redaction 规则与字段清单**（含 `safe_summary` 组装规则）| 3.7 Guardrails / 3.8 Observability | Legal + 工程 |
| 15 | **Bot 项目新 repo** 是否创建及命名 | Phase 4 Coding Agent Implementation Packet | 工程 |
| 16 | **`send_followup_email_or_async_update` 触发主体**（一期明确为人工/back-office；相关 email 模板 ID 清单）| 3.6 Handover / 3.4 Tools | Ops + 产品 |

## 6.5 下一步行动

1. **Review v3 输出**：与业务方（Chelsea Fagan-Hall）、产品方（Lance Li）确认 Phase 0 / 1 / 2 v3 内容，重点是：
   - 新增的 UC-G / UC-H / UC-I / UC-J / UC-K 五个 use case schema 与 "intake + handover" 定位
   - §2.10 Tool Allocation 矩阵是否与 `customer_service_tool_spec_v0_1.yaml` 完全一致
   - UC-FP-01 → UC-H-01 降判路径的边界
   - §2.9 Guardrails 中关于"禁止模仿真实坐席已删除/已限制类话术"的条款
2. **补齐 §6.4 缺失输入**：每项指定 owner 与 due date；**优先补 1/2/3/4/5**（直接阻塞 tool contract 落地）。
3. **暂不启动** Phase 3 / 4 / 5 — 待 §6.4 输入到位后再开。
4. **可并行**：
   - 从 `customer_service_conversation_samples_organized.xlsx` 第 05 sheet（43 curated examples）+ 第 01 sheet `design_bucket=self_serve_design` 的 30 条，构建 Golden Dataset 初始版本（73 条起步，目标 ≥ 150）
   - 从 `design_bucket=escalation_design` 的 207 条样本中，为每个 escalation_reason 标注 5–10 条 reference transcript，构建 Handover Dataset
   - 从 `frustration_or_failure_mode`(7) + `likely_outcome=abandoned_or_timeout`(25) + `escalation_signal=True`(63) 中去重，形成初始 bad-case bank（目标 ≥ 80 条）
