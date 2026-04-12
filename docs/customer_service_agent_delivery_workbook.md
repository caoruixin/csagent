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
| Phase 0 — Freeze Normative Layer | ✅ 已完成 | [`phase0_normative_freeze.md`](./phase0_normative_freeze.md) |
| Phase 1 — Build Solution Input Pack | ✅ 已完成 | [`phase1_solution_input_pack.md`](./phase1_solution_input_pack.md) |
| Phase 2 — Produce Domain Realization Spec | ✅ 已完成 | [`phase2_domain_realization_spec.md`](./phase2_domain_realization_spec.md) |
| Phase 3 — Produce Detailed Technical Design | ⏳ 待启动 | `phase3_detailed_technical_design.md`（待产出） |
| Phase 4 — Coding Agent Implementation Packet | ⏳ 待启动 | `phase4_implementation_packet.md`（待产出） |
| Phase 5 — Eval / Release / Feedback Loop | ⏳ 待启动 | `phase5_eval_release_feedback.md`（待产出） |

本 workbook 保留作为**阶段模板与索引**；每个阶段的实际产出在独立文件中维护，便于 review 与迭代。

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
**Phase 2 — Domain Realization Spec 已完成**。下一步启动 Phase 3（Detailed Technical Design）。

## 6.3 已有输入
- **规范层**: `customer_service_agent_version_new_spec.md`（Part I Whole Tech Spec + Part II Eval Spec）
- **业务 BRD**: `BRD.md`（scope、user stories、functional/UI/legal requirements、success metrics）
- **PRD 业务部分**: `PRD_biz_part.md`（capacity analysis、A–F' use case flows、架构参考、数据模型）
- **数据分析**: `case-data-stat.md`（120,367 条 Case 结构化分析，渠道/原因/子原因占比）
- **Case 样本**: `case-samples.md`（原始 Case 样本）
- **工程约束**: `07-engineering-constraints.md`（从 Gumtree 平台代码库提取的语言/框架/部署/CI/CD/可观测性/安全等基线）
- **阶段产出**: `phase0_normative_freeze.md`、`phase1_solution_input_pack.md`、`phase2_domain_realization_spec.md`

## 6.4 缺失输入
1. **Help Centre 文章清单与 URL 映射**：需具体 Knowledge 文章 ID、URL 和覆盖范围列表，完成 `knowledge_scope` 到实际文章的精确映射。
2. **标准话术模板终稿**：UC-FP-01 政策解释话术、各类安全提示话术需与合规/产品终审确认。
3. **Salesforce 组织配置确认**：Enhanced Chat、Omni-Channel、Knowledge API 的具体版本与配置状态。
4. **向量库选型决策**：pgvector on Cloud SQL / Vertex AI Vector Search / GKE self-hosted 三选一。
5. **Embedding 模型选型**：Vertex AI text-embedding / 其他 GCP 原生模型终审。
6. **Golden Dataset 初始版本**：基于 A–F' 场景的评测用例集尚未构建。
7. **流量分配策略确认**：初始试点比例（5%/10%/50%）与 go/no-go 阈值的业务方确认。
8. **Off-hours 策略确认**：非工作时间 Bot 行为边界（是否仍可转人工、是否创建 Case 供后续跟进）。
9. **Bot 项目新 repo 创建**：greenfield repo 初始化（Helm chart、Jenkinsfile、Dockerfile、contract 目录骨架）。

## 6.5 下一步行动
1. **对齐 Phase 2 输出**：与业务方（Chelsea Fagan-Hall）、产品方（Lance Li）review Domain Realization Spec，确认 use case 边界和升级条件。
2. **补齐缺失输入**（§6.4），特别是向量库选型、Help Centre 文章清单、话术模板。
3. **启动 Phase 3 — Detailed Technical Design**：基于已确认的 Domain Realization Spec + 工程约束，产出 Runtime Architecture、State Model、Control Kernel 实现、Tools/Capabilities、Knowledge & Grounding、Handover、Guardrails、Observability、NFRs。输出文件：`phase3_detailed_technical_design.md`。
4. **启动 Golden Dataset 构建**：从 `case-samples.md` 中提取 A–F' 高频样本，转化为评测用例格式。
5. **创建 Bot 服务 greenfield repo 骨架**：按 `07-engineering-constraints.md` §7.4 baseline（Java 17 + Spring Boot 3.x，GKE + Helm + Jenkins）。
