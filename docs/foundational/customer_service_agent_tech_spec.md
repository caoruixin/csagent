# Customer Service Agent — Whole Tech Spec

> 目标：以**高频场景优先、内核先行、避免过早复杂化**为原则，定义 Customer Service Agent 的新版本正式规格。该版本不是追求一次性做成完整平台，而是先交付一个可上线、可评测、可扩展的生产级 V1 内核，并为后续能力扩展预留清晰边界。

---

## 1. Document Purpose

本文档定义 Customer Service Agent 新版本的正式技术规格，覆盖：

- V1 的产品目标与范围
- 最小可上线内核设计
- Runtime / Control / Context / Tools / Handover / Guardrails / Observability
- V1 必做与后续扩展的边界
- 与评测、发布、运营的衔接要求

本文档明确采用如下策略：

**先做一个好的内核，再根据 user case 覆盖范围和预期效果逐步扩展，不为了"未来可能需要"而提前引入复杂度。**

---

## 2. Product Strategy

### 2.1 Product Objective

Customer Service Agent 的目标是在客服聊天线程中，优先处理**高频、低到中风险、知识与流程边界较清晰**的用户问题，提供：

1. grounded 的 FAQ / guidance 自助支持
2. 必要但受控的澄清
3. 无法可靠解决时的结构化升级与上下文交接
4. 可回归、可追踪、可持续优化的评测闭环

### 2.2 Versioning Strategy

本方案采用三阶段递进策略：

#### V1 — Minimal Production Kernel

聚焦上线最小必要能力：

- 覆盖高频 FAQ / guidance 场景
- 支持有限澄清
- 支持受控升级人工
- 支持基础 trace 与 eval
- 支持上线门禁与回归

#### V1.1 — Controlled Enrichment

在 V1 运行稳定后补：

- 更强的业务配置层
- procedure 化流程能力
- 更强的运营分析与调试体验
- 知识就绪度治理

#### V2 — Platformization

在业务价值与组织能力验证后再补：

- 更细粒度流程编排
- 多渠道 / 多团队 / 多handoff policy
- 版本化配置对象全面治理
- 更成熟的运营工作台与自动化优化能力

### 2.3 Scope Principle

V1 不追求"覆盖所有客服场景"，而是追求：

- 覆盖高频场景
- 正确处理边界场景
- 高风险场景不乱答
- 转人工质量高
- 系统行为稳定可控

---

## 3. Prioritization of the 8 Improvement Areas

下面将前面识别出的 8 个可借鉴点，按上线优先级拆分。

### 3.1 V1 Must-Have

#### A. Use Case Registry（轻量版）

V1 需要，但只做**轻量版**。

不是做复杂 topic platform，而是建立一个最小可运营的注册表，用来定义：

- use case id
- 问题域说明
- 可覆盖示例意图
- 允许动作集合
- 默认知识源范围
- 是否允许澄清
- 是否允许自助解决
- 升级条件
- 风险等级

原因：
V1 若只有 intent 和 action，而没有 use case 层，后续运营、QA、bad-case 回归都会缺少稳定的管理对象。

#### B. Handover Policy Engine（最小版）

V1 需要，但只做**最小策略版**。

V1 的 handover policy 至少要解决：

- 何时必须升级
- 何时允许继续 bot 自助
- 用户明确要人工时如何处理
- FAQ miss / clarification 超预算时如何处理
- 升级时需要携带哪些字段

V1 不做复杂 team routing，但必须把"升级判断 + payload contract"固定下来。

#### C. Isolated Control Testing

V1 需要。

除了端到端对话评测，V1 必须支持控制层隔离测试，至少覆盖：

- action selection
- termination
- delayed escalation
- repeated same action
- issue loss
- clarification budget

原因：
仅做端到端 eval，无法高效定位 control loop 失败。

#### D. Core Operational Funnel（精简版）

V1 需要，但只做精简版运营漏斗。

至少跟踪：

- total sessions
- understood sessions
- bot-contained sessions
- escalated sessions
- abandoned sessions
- repeat-contact proxy
- wrong containment

原因：
如果没有这条最小漏斗，V1 上线后很难判断到底是理解、回答、升级还是交接出问题。

### 3.2 V1.1 Should-Have

#### E. Hybrid Orchestration

V1.1 再引入。

V1 暂不做完整的 dialogue / procedure / open loop 三层混合编排，只保留单一 bounded loop。

V1.1 再补：

- FAQ / guidance 走 generative loop
- 少量高价值任务走 lightweight procedure
- 极强约束场景再做 strict workflow

原因：
这是很有价值的演进方向，但 V1 若提前引入，会明显增加 runtime、测试与配置复杂度。

#### F. Knowledge Readiness Workflow

V1.1 再补。

V1 先保证 grounded retrieval 和 faq miss 统计；V1.1 再引入：

- article readiness score
- coverage gap analysis
- freshness / ownership
- low-quality source suppression

#### G. Decision Path / Failure Drilldown UI

V1.1 再补。

V1 先有结构化 trace；V1.1 再把 trace 产品化为：

- replay view
- decision path
- failure drilldown

### 3.3 V2 Platformization

#### H. Full Configuration Versioning

V2 再补。

V1 只要求版本化：

- prompt
- tool schema
- projection policy
- control policy
- eval suite

V2 再扩展版本化对象到：

- use case definitions
- procedures
- routing rules
- handoff flows
- knowledge slices
- escalation mappings

---

## 4. V1 Product Scope

### 4.1 In-Scope for V1

V1 聚焦以下问题类型：

- FAQ 型问题
- 产品 / 功能说明型问题
- 常见操作指导型问题
- 需要 1–2 轮澄清即可回答的问题
- 明确应转人工的问题

### 4.2 Out-of-Scope for V1

V1 不做：

- 复杂多步骤业务流程编排
- 高风险自动决策
- 复杂写操作自动化
- 多 agent orchestration
- 长链路事务执行
- 跨渠道复杂会话生命周期治理
- 精细化团队路由策略

### 4.3 Success Criteria

V1 的成功不是"覆盖面最大"，而是：

- 高频场景有稳定 containment
- 不会在高风险场景乱答
- 升级时上下文质量高
- 坏 case 能被回归与修复
- 上线后能持续优化

---

## 5. Design Principles

### 5.1 Kernel First

先把控制内核、上下文投影、升级、评测做对，再扩展业务能力层。

### 5.2 Single Agent First

V1 使用 single agent + bounded loop，不做多 agent。

### 5.3 Harness-Centric Reliability

系统可靠性主要来自 harness，而不是来自 prompt 本身。

### 5.4 State Outside, Context Projected

真实状态在外部维护；LLM 每轮只看最小充分上下文。

### 5.5 Grounded Before Generative

能基于知识回答的，不靠自由生成扩写。

### 5.6 Escalation Is a First-Class Outcome

转人工不是失败兜底，而是正式能力。

### 5.7 Eval Before Expansion

任何新增能力都先定义 eval 再扩展。

---

## 6. System Definition

### 6.1 Formal Definition

系统定义为：

**Customer Service Agent = LLM + Control Kernel + Runtime/Harness + Runtime Capabilities**

### 6.2 Responsibility Split

#### LLM

负责：

- 理解当前投影上下文
- 在受限动作空间内选择下一步
- 输出结构化决策
- 基于 grounding 生成可读回复

#### Control Kernel

负责：

- 当前阶段定义
- 动作策略
- 预算与终止判断
- escalation 语义
- drift 处理语义

#### Runtime / Harness

负责：

- 状态读取与写回
- context projection
- LLM 调用
- tool dispatch
- schema validation
- policy check
- tracing
- failure recovery

#### Runtime Capabilities

包括：

- knowledge retrieval
- customer context
- escalation / handover interface
- state store
- observability

---

## 7. V1 Minimal Architecture

```text
User
  ↓
Chat Channel / Messaging Session
  ↓
Customer Service Agent Runtime
  ├─ Control Kernel
  ├─ Context Builder
  ├─ LLM Invocation Layer
  ├─ Tool Dispatcher
  ├─ State Store
  ├─ Handover Module
  ├─ Guardrail Layer
  └─ Trace / Metrics Hooks
       ├─ Knowledge Retrieval
       ├─ Customer Context Resolver
       ├─ Escalation API / Handover API
       └─ Metrics / Trace Store
```

### 7.1 Architectural Intention

V1 架构的目标不是支持复杂流程编排，而是支持：

- 高质量 FAQ resolution
- 受控澄清
- 受控升级
- 可观测可评测

---

## 8. State Model

### 8.1 State Layers

V1 明确区分四类对象。

#### External State

系统权威状态，至少包括：

- session_id
- active_use_case
- candidate_use_cases
- issue_status
- risk_flags
- counters / budgets
- articles_shown
- escalation_state
- transcript_ref

#### Session State

会话运行态，至少包括：

- recent turns
- last action
- last retrieval result summary
- clarification count
- faq miss count
- bot turn count

#### Memory

V1 默认极轻，不作为核心依赖。

默认仅允许少量低风险长期信息；多数会话处理不依赖长期 memory。

#### Context

每轮投影给 LLM 的最小充分信息切片。

### 8.2 Why This Matters

V1 若把所有历史、所有工具日志、所有知识结果都直接塞进 prompt，会导致：

- 成本上升
- 时延上升
- 推理漂移
- 错误延续
- 控制失真

因此 V1 必须坚持上下文投影，而不是拼接式 prompt。

---

## 9. Use Case Registry (V1 Lite)

### 9.1 Objective

Use Case Registry 是 V1 新增的最小业务配置层。

其目的不是替代 Control Kernel，而是让业务范围、动作边界和升级策略有一个稳定配置对象。

### 9.2 Required Fields

每个 use case 至少定义：

```yaml
use_case_id:
name:
description:
example_user_requests: []
knowledge_scope: []
risk_level: low | medium | high
allow_clarification: true | false
allow_bot_resolution: true | false
allowed_actions: []
escalation_conditions: []
outcome_class: resolve | clarify | escalate
```

### 9.3 V1 Scope

V1 use case registry 只支持：

- 手工维护
- 数量有限
- 单层结构
- 无复杂继承
- 无 procedure 绑定

---

## 10. Context Projection Specification

### 10.1 Objective

每轮只投影完成当前任务所需的最小充分信息。

### 10.2 Projection Inputs

V1 可考虑进入上下文的输入：

- current task summary
- active use case
- candidate use cases
- issue status
- recent 3–5 key messages
- retrieved knowledge snippets
- last tool result summary
- risk flags
- budget state
- allowed actions
- tool schemas in scope

### 10.3 Projection Rules

1. relevance first
2. recency with unresolved override
3. structured over raw
4. grounding priority
5. risk-aware injection
6. schema minimization

### 10.4 Prompt Exclusions by Default

默认不进入 prompt：

- 全量历史消息
- 全量工具日志
- 与当前 use case 无关的知识结果
- 不在本轮可用的 tool schema
- 冗余敏感信息

### 10.5 Projection Output Contract

```json
{
  "task_summary": "",
  "active_use_case": "",
  "candidate_use_cases": [],
  "recent_messages": [],
  "retrieved_knowledge": [],
  "risk_flags": [],
  "budget_state": {},
  "allowed_actions": [],
  "tool_schemas": []
}
```

---

## 11. Control Kernel Specification

### 11.1 Objective

Control Kernel 定义 V1 的核心控制语义。

### 11.2 State Machine

```text
INIT
  → DISCOVER
  → RESOLVE
  → CONFIRM
  → CLOSE
          ↘
           ESCALATE
```

### 11.3 Phase Definitions

#### INIT

- bot disclosure
- initialize state
- load budgets and use case candidates

#### DISCOVER

- infer active use case
- detect ambiguity
- decide whether clarification is needed
- detect risk / escalation triggers

#### RESOLVE

- retrieve knowledge
- answer grounded
- ask clarification
- prepare escalation

#### CONFIRM

- check whether user issue is solved
- detect whether a new issue is introduced
- decide continue / close / escalate

#### ESCALATE

- build structured handover payload
- transfer control to human system

#### CLOSE

- mark outcome
- record trace

### 11.4 Allowed Actions for V1

V1 只允许以下动作类型：

1. ask_user
2. retrieve_knowledge
3. answer_grounded
4. escalate_human
5. finish

V1 默认不做：

- create_case as generic autonomous action
- sub_agent_call
- unrestricted multi-tool composition
- workflow chaining

### 11.5 Control Budgets

V1 最少需要这些预算：

- max_clarification_rounds
- max_faq_miss
- max_bot_turns_per_issue
- max_repeated_same_action
- max_total_bot_turns_before_forced_escalation

### 11.6 Intent Drift Semantics

V1 必须支持最小 drift 处理：

- minor drift：补充信息，不切换 use case
- soft shift：引入新问题，允许切 active issue，但保留未解决问题标记
- hard shift：高风险或优先级更高问题，直接升级或切策略

V1 不要求复杂 issue graph，只要求不丢失主问题状态。

---

## 12. Runtime / Harness Specification

### 12.1 Core Responsibilities

Runtime 必须实现：

- state read/write
- context build
- model invocation
- action parsing
- tool dispatch
- schema validation
- policy validation
- persistence
- retry / fallback
- tracing hooks

### 12.2 Failure Handling

工具或 retrieval 失败时，Runtime 不得默认无限重试。

应返回标准失败类型，由 Control Kernel 决定：

- retry once
- ask clarification
- fallback response
- escalate

### 12.3 Persistence

V1 至少持久化：

- active_use_case
- counters
- articles_shown
- escalation_state
- outcome
- trace metadata

---

## 13. Tooling Specification

### 13.1 V1 Tool Set

V1 工具集合必须小而强：

- search_knowledge
- resolve_article
- get_customer_context
- request_handover
- record_outcome

### 13.2 Tool Design Requirements

每个工具必须有：

- stable name
- clear description
- parameter schema
- permission boundary
- standardized success / error payload

### 13.3 Tooling Non-goals for V1

V1 不引入：

- 过多专用工具
- 任意工具链组合
- 强写操作自动化
- 多系统事务编排

---

## 14. Knowledge and Grounding

### 14.1 Knowledge Principle

V1 的 bot 回复应优先基于知识源生成，而不是无依据自由扩写。

### 14.2 Retrieval Responsibilities

V1 至少支持：

- use case scoped retrieval
- source ids tracking
- article shown logging
- faq miss tracking

### 14.3 Answer Contract

所有可解答型回复都应满足：

- grounded
- understandable
- concise enough for support UX
- no unsupported promise

### 14.4 Deferred Work

以下知识治理能力延期到 V1.1：

- readiness score
- coverage analytics
- freshness governance
- knowledge ownership workflow

---

## 15. Handover Specification

### 15.1 Objective

V1 将 handover 作为正式能力，而不是兜底字符串拼接。

### 15.2 Handover Policy (V1 Minimal)

以下情况触发升级：

- user explicitly requests human
- high-risk use case
- clarification budget exhausted
- faq miss threshold exceeded
- policy requires escalation
- system confidence / grounding insufficient

### 15.3 Required Handover Payload

V1 payload 至少包括：

- session_id
- primary_use_case
- candidate_use_cases
- current_status
- summary
- clarification_count
- faq_miss_count
- articles_shown
- escalation_reason
- transcript_ref

### 15.4 UX Requirement

- 用户侧保持连续会话体验
- 人工侧看到结构化摘要而不是纯 transcript
- 避免用户重复描述问题

### 15.5 Deferred Work

以下延后到 V1.1 / V2：

- intelligent team routing
- off-hours routing policy
- async follow-up mode
- handback lifecycle
- multi-team escalation mapping

---

## 16. Guardrails and Policy

### 16.1 Mandatory Rules

系统必须：

- 明确 bot 身份
- 不伪装人工
- 不在无依据时编造答案
- 不处理高风险自动裁决
- 不做越权承诺
- 只暴露最小必要信息

### 16.2 Governance Form

V1 至少将以下内容版本化：

- prompt version
- projection policy version
- control policy version
- tool schema version
- eval suite version

### 16.3 Deferred Governance

V2 再做更细粒度的 use case / handoff / routing / knowledge slice 版本治理。

---

## 17. Observability and Analytics

### 17.1 Required Event Types

V1 至少记录：

- session_started
- use_case_inferred
- retrieval_executed
- article_shown
- clarification_asked
- escalation_requested
- outcome_recorded
- session_closed

### 17.2 Trace Fields

每个 turn 至少包含：

- trace_id
- session_id
- turn_id
- prompt_version
- model_version
- projection_version
- active_use_case
- action_selected
- tool_calls
- source_ids
- outcome

### 17.3 V1 Minimal Funnel

V1 dashboard 至少展示：

- total sessions
- understood sessions
- resolved by bot
- escalated to human
- abandoned sessions
- wrong containment
- repeat-contact proxy
- latency p50 / p95
- cost per conversation

### 17.4 Deferred UX

trace replay、decision path、failure drilldown 作为 V1.1 能力。

---

## 18. Non-Functional Requirements

### 18.1 Reliability

- handover request 可重试
- state write 可校验
- degraded mode 可落到安全升级

### 18.2 Performance

建议初始目标：

- FAQ answer p95 ≤ 5s
- escalation request p95 ≤ 3s
- median turns for solved FAQ ≤ 6

### 18.3 Maintainability

- clear versioned configs
- bounded tool surface
- regression-friendly architecture

### 18.4 Privacy / Security

- minimum data exposure
- redacted logs
- secret isolation
- retention policy compliance

---

## 19. Release Criteria for V1

V1 上线前必须满足：

- core bounded loop implemented
- use case registry lite implemented
- projection policy fixed and versioned
- handover payload fixed
- critical eval suite runnable
- critical policy violation = 0
- regression gate in CI
- minimal analytics funnel available

---

## 20. Post-V1 Expansion Plan

### 20.1 V1.1 Expansion

- hybrid orchestration for selected tasks
- lightweight procedures
- knowledge readiness workflow
- decision path / replay tooling
- stronger analytics breakdown

### 20.2 V2 Expansion

- full configuration versioning
- handback lifecycle
- richer routing policy engine
- broader channel lifecycle support
- more advanced procedural automation
