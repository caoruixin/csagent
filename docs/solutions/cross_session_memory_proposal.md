---
title: 跨会话记忆机制（Cross-Session Memory）方案
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-05-24
mode: forward-looking
---

# 跨会话记忆机制（Cross-Session Memory）方案

> Path 1 research-driven。消费 human thought：「memory 机制缺乏，行业先进实践是
> "向量 memory + 实体图谱 + 短/长期记忆分离"，当前 csagent 只有"会话级持久化、无
> 跨会话记忆"，需要一个符合业务场景的方案」。
>
> 本文是 proposal（建议，非绑定决定）。最终 scope 拆分、milestone 归属由 deliver
> agent + human 决定；本文给出 ≥2 设计方案 + 推荐 + §3.2 layer 分类 + §7 stanza
> 预填 + hard fences + compounding-effect 分析。所有代码引用在 HEAD `9ef9d1e` 验证。

---

## 1. Executive summary

当前 agent 的记忆是**严格会话隔离**的：每个 `bot_sessions` 行以随机 `session_id`
(UUID) 为主键，无任何稳定的用户/账号维度键，`BotSessionRepository` 只能按
`session_id` 或 `handling_state` 查询，**没有任何机制把同一用户的多次会话连起来**。
换句话说，今天的"短期记忆"（会话内 `conversation_history` 最近 10 轮 + transient
session state + `prior_use_case_carry` 软信号）已相当成熟，但"长期记忆"（跨会话）
完全空白。

好消息是：实现跨会话记忆所需的**全部底层能力本仓已经具备**——可插拔的
`EmbeddingClient`（768 维）+ pgvector(768)+HNSW 余弦检索栈、会话终态写入锚点
（`SessionOutcome` @ CLOSE/ESCALATE）、以及一个教科书式的「软投影信号」先例
（`prior_use_case_carry`，registry-driven、LLM-owned）。真正的工作不在"造轮子"，
而在三件**领域约束**上把方案做对：(1) **身份键**——应锚定平台已认证的账号身份，
而非用户自填的 email；(2) **存什么**——存 LLM 生成的结构化派生摘要，**绝不存原始
transcript**；(3) **隐私底线**——写入即脱敏 + 保留期/TTL + GDPR 可擦除（GDPR 占真实
case 的 24.6%，本仓 GDPR 是 escalation-only 且"绝不声称已删除"）。

推荐方案 **Alt B（结构化实体键存储 + 向量语义召回，分阶段交付）**：以 account 维度
的结构化记忆为脊柱，复用 FAQ 向量栈做语义召回，召回结果作为**软投影槽**（沿用
`prior_use_case_carry` 模式）交给 LLM 自主决定是否使用。明确**反对** Alt C（独立
graph DB / 外部 memory 框架）——违反"Postgres 单一真相源"工程约束、GDPR 不可控、
对 CS 实体基数（账号→少量 listing→少量 case）属过度工程。

**与活跃工作的关系**：本能力是 standing P0（Single Handover Orchestrator,
`release_gate.md` §1.1）之外的**并行能力轨**，不挤占 P0；召回槽应在 **M5-S3**
（projection 转 Skill-driven）落地之后、经 `requiredContextKeys` 声明接入，避免给
M5-S4 denoise 制造新的常驻投影噪声。

---

## 2. Current-state survey（code-grounded，HEAD `9ef9d1e`）

### 2.1 会话/身份模型：严格会话隔离，身份键缺失

- `BotSession` 主键是 `session_id`（UUID），**无 `user_id`/`account_id`/`email`
  列**：`server/.../model/BotSession.java:27-29`。email、first_name 等只作为
  `form_context` jsonb（`BotSession.java:105-106`）和派生的 `customer_context`
  jsonb（`:108-109`）存在，无独立列、无索引。
- `BotSessionRepository` 仅 `findByHandlingState(...)` + JpaRepository 默认
  `findById`，**没有 `findByEmail` / `findByAccountId`**：
  `server/.../repository/BotSessionRepository.java`（全文 13 行）。
- `sf_bot_session_id` 字段已声明（`BotSession.java:31-32`）但**全仓从未被赋值**
  （Salesforce 集成 mock，production gap，见 `docs/runbooks/salesforce-part-spec.md`
  `implementation_status: not_started`）。
- **结论**：跨会话关联在数据层、仓储层、服务层三处皆缺位。每次会话从零开始。

### 2.2 身份其实存在于上游——只是没被用作键

- 平台账号身份在 mock 后端是稳定存在的：`server/.../resources/mock/accounts/
  active_user.json` 携带稳定 `user_id: USR-001` + `email` + `account_status` +
  `registration_date` 等。
- 会话创建时 `FormContextIngestionService` 在 email 存在时自动调用
  `GetCustomerContextTool`（经 `GumtreeApiService`，mock）富化
  `customer_context`/`listing_context`/`moderation_context`，且**主动剥离原始
  PII**（email/phone/full_name/address），只回派生事实（account_status、租龄、
  在架数量、verified 标志等）：`server/.../service/tools/GetCustomerContextTool.java`。
- 关键文档证据（`docs/action_bank.md:397`，R-l3 rubric 关闭说明）：production 模型里
  chat 是**账号绑定**的，`form_context.first_name` 是"平台账号绑定 chat session
  surfaced 的 canonical 预知身份，**不是**用户自填自由文本"。
- **结论**：稳定账号身份在上游已认证存在，只是没有被 surface/persist 到
  `bot_sessions` 上、没被当作跨会话键。这是记忆方案的**第 0 步**（见 §6）。

### 2.3 短期记忆（会话内）已成熟——可作为长期记忆的设计范式

- 每轮投影由 `ContextProjectionBuilder.buildProjection(...)` 组装
  （`server/.../service/runtime/ContextProjectionBuilder.java:337`）。其中：
  - `conversation_history` 截最近 **10 轮**（`:650-668`）——会话内工作记忆窗口。
  - 大量**软信号槽**：`candidate_use_cases`、`alternate_candidate_use_cases`
    （`:431-440`）、`discover_disambiguation_signals`（`:456`）、
    **`prior_use_case_carry`**（`:472-474` + 构造器 `:1062-1163`）、
    `drift_history`/`task_history`（`:600-605`）等。
- **`prior_use_case_carry` 是本方案最重要的先例**：它把"会话内 UC 切换后的连续性
  状态"作为**软信号**投影给 LLM——registry-driven（单一 aging 常量
  `PRIOR_USE_CASE_CARRY_AGING_TURNS=4` `:51`、单一 citation cap=3 `:59`，**无 per-UC
  分支**），LLM-owned（`:1058-1060` 明确"LLM 决定 surface/ask/ignore，runtime 不
  enforce"）。**跨会话记忆召回槽应当 1:1 沿用这个模式**。
- 投影里的 PII 脱敏极弱：`redactPii` 只用正则替换 email（`:1323-1328`），且
  `customer_context`/`form_context` 是**原样**投影（`:641-648`）。

### 2.4 Skill 抽象（M2）：记忆接入的天然挂载点

- `Skill` record 携带 `requiredContextKeys`（`server/.../skill/Skill.java:59`）。
  该字段已流转到 `PhasePlan`（`PhaseEvaluator.java:455`
  `.requiredContextKeys(new LinkedHashSet<>(skill.requiredContextKeys()))`），但
  **尚未被投影消费来 gate 槽位**——这正是 **M5-S3** 计划要做的
  （`docs/milestone_objective.md` §3 S3：「make the projection read Skill
  declarations — `skill.requiredContextKeys()` decides which context slots to
  project」）。记忆召回槽应通过这个机制声明式接入。
- `StateInheritance`（`server/.../skill/StateInheritance.java`）已定义三种跨-Skill
  状态姿态：`inherit` / `reset` / `soft_signal_via_projection`，且
  `soft_signal_via_projection` 的合法槽名单已包含 `prior_use_case_carry`
  （`:31`）。**跨会话记忆是这个"状态继承"框架的自然外延**——从"跨-Skill"扩到
  "跨-session"。

### 2.5 会话终态写入锚点已存在

- 终态在 CLOSE/ESCALATE phase 收敛（`ControlKernel.java:555/564/1145`）。
- `SessionOutcome` 在终态写入，已捕获：`outcome`、`use_case_id`、
  `escalation_reason`、`articles_shown`、`total_turns`、**`trace_metadata`
  jsonb**（`server/.../model/SessionOutcome.java:28-48`），写入路径在
  `RecordOutcomeTool` + `SessionManager`。
- **结论**：`trace_metadata` jsonb 是"终态写结构化派生数据"的先例；记忆写入应**与
  之共址**（同一终态钩子）。

### 2.6 向量/嵌入检索栈：完全可复用（FAQ RAG 范式）

- `EmbeddingClient` 接口（`embed(String)` / `embedBatch(List)`）+
  `DashScopeEmbeddingClient`，**768 维**，`@Profile` 可换：
  `server/.../service/embedding/`。
- pgvector 已启用：`V5__create_kb_chunks.sql` `CREATE EXTENSION vector` +
  `embedding vector(768)` + **HNSW** `vector_cosine_ops` 索引；ORM 经
  `VectorType`（`config/VectorType.java`）映射 `vector(768)↔float[]`；相似度查询用
  `<=>` 算子（`KbChunkRepository.findNearestByEmbedding*`）。
- 检索管线（`KnowledgeSearchService`）：embed → ANN(limit 20) → 余弦去重 → LLM
  rerank(1-5) → 双门控（retrievalGate 0.3 / answerGate 3.5）→ top-3，全部阈值外置
  到 `application.yml`。
- **结论**：记忆向量层无需新基建，直接复用同一 embedding + pgvector + 门控范式
  （只需新表 + 新 native query）。

### 2.7 隐私/安全底线（binding constraint，详见 §8）

- **脱敏只在展示/trace 时**：`PiiRedactionFilter` / `ToolCallTraceSanitizer` 都在
  trace-display 层；**原始用户消息以 `bot_turns.user_message` 原文持久化、无 TTL、
  无清理 job、保留期未文档化（默认无限期）**（`service/observability/TraceWriter.java`
  + migrations）。
- Tier-0（`docs/runtime_freeze_and_risk_policy.md` §1/§2 +
  `EscalationReasonResolver.java`）：`identity_verification_required` 是**会话内**
  升级门，**不跨会话继承**；凭据（密码/卡号/国民 ID）**绝不持久化**；GDPR 为
  escalation-only、"绝不声称已删除"。
- GDPR 占真实 case **24.6%**（`docs/case-data-stat.md`，120,367 历史 case 统计）。

### 2.8 活跃工作

- 活跃 milestone **M5 — Observability Coherence**（S1 已关；S2 per-invocation trace
  进行中；**S3 将把投影转 Skill-driven**）。
- standing P0：**Single Handover Orchestrator**（`release_gate.md` §1.1 handover
  side-effect exactly-once by `session_id`）——未被 M5 取代，仍是 M5+ 候选。

---

## 3. Gap analysis

| 维度 | 行业先进实践（human 引用） | 本仓现状 | Gap |
|---|---|---|---|
| 短期记忆 | 会话内工作记忆 | ✅ `conversation_history`(10 轮)+transient state+`prior_use_case_carry` | 基本无 gap |
| 长期记忆 | 跨会话持久记忆 | ❌ 完全无 | **核心 gap** |
| 身份键 | 稳定 user 维度 | ⚠️ 账号身份在上游存在(`USR-001`)，但未 surface 到 session、无键无索引 | **前置 gap（必须先修）** |
| 实体图谱 | entity graph | ⚠️ 领域实体天然存在(account/ad_id/case_id)，但无跨会话实体索引 | 中等 gap（轻量即可） |
| 向量记忆 | vector store | ✅ 嵌入+pgvector+HNSW+门控栈完备（FAQ 用） | 0 基建 gap，只缺记忆表+查询 |
| 召回注入 | retrieval→prompt | ⚠️ 软投影槽范式成熟(`prior_use_case_carry`)、`requiredContextKeys` 挂载点存在但休眠 | 小 gap（沿用范式） |
| 写入锚点 | session 末摘要 | ✅ `SessionOutcome`@CLOSE/ESCALATE + `trace_metadata` jsonb 先例 | 小 gap（共址新写） |
| 隐私/保留 | 数据最小化+TTL+擦除 | ❌ 脱敏仅展示层、原文无限期留存、无 TTL、GDPR 仅 escalation | **横切 gap（必须随写入同步补齐）** |

**一句话**：能力齐备，gap 集中在"身份键 + 长期存储 + 隐私治理"三件领域约束，而非
技术基建。

---

## 4. Design alternatives + trade-offs

> 三个方案共享同一注入范式（召回=软投影槽、写入=终态钩子、摘要=LLM 生成、检索机械
> 但使用 LLM-owned）。差异在**存储与召回的复杂度**。

### Alt A — 结构化实体键记忆（无向量）

- **存储**：新表 `customer_memory`，键 `account_id`（+ 可选 `ad_id` / `case_id`），
  每行 = 一次会话终态时 LLM 生成的**结构化摘要**（issue 概述、UC、涉及实体、终态
  status[resolved/escalated/abandoned]、已收集 intake 字段、escalation 历史），
  写入即脱敏。
- **召回**：会话开始/每轮按 `account_id`（+ 当前 `ad_id`/open `case_id` 实体精确
  匹配）取最近 N 条，作为软投影槽 `prior_interactions` 注入。
- **Trade-off**：✅ 最低风险、最快、完全复用 `SessionOutcome`@close + 投影槽范式；
  ✅ GDPR 擦除简单（按 account_id 删行）；❌ 无语义召回——只能"同实体/近期"召回，
  无法识别"不同 listing 但同类问题"的跨会话模式。

### Alt B — 结构化 + 向量混合（**推荐**，分阶段）

- **存储**：Alt A 的结构化脊柱 + 给每条摘要算 embedding（复用 `EmbeddingClient`，
  pgvector(768)）。
- **召回（双通道）**：
  1. **确定性实体通道**（机械、总是 surface）：同 `ad_id` / open `case_id` /
     同 `account_id` 近期 N 条；
  2. **语义通道**（机械检索、LLM-owned 使用）：用当前 issue 文本 embedding 在该
     `account_id` 的记忆里做 top-k 余弦召回，套一个相似度门控（类比 FAQ 的
     retrievalGate）。
  - 两通道结果合并去重，作为软投影槽 `prior_interactions` 注入；**排序/取舍由 LLM
    在槽内自主完成**（不在 Java 里做语义优先级）。
- **映射 human 的三要素**：短/长期分离=会话内(已存)vs跨会话(本表)；实体图谱=
  account/ad_id/case_id 轻量实体键（**非 graph DB**）；向量记忆=语义通道。
- **Trade-off**：✅ 完整覆盖"vector+entity+短长分离"且领域右尺寸；✅ 对高频 CS 用户
  （历史多到塞不进 context）用向量做**选择**、用 LLM 做**使用**，正好契合 6 个月后
  大窗口+强摘要的 LLM；❌ 比 A 多一层（embedding 写入 + 向量查询 + 门控调参）。

### Alt C — 独立 graph DB / 外部 memory 框架（Zep / Mem0 / Neo4j 风格）

- **Trade-off**：❌ 违反"Postgres 为客户数据单一真相源"工程约束
  (`docs/07-engineering-constraints.md`)；❌ 新增运维面 + GDPR 跨系统擦除难控；
  ❌ CS 域实体基数低（account→少量 listing→少量 case），全图谱属过度工程；
  ❌ 与本仓"反过度工程 / LLM-first 而非堆基建"姿态冲突。**不推荐。**

---

## 5. Recommended option + rationale

**推荐 Alt B，但分阶段交付（先 A 的脊柱，再叠向量层）。**

理由：

1. **领域右尺寸**：human 引用的"向量+实体图谱+短长分离"在 Alt B 里被一一映射到
   marketplace 域，且砍掉了不适配的全图谱（Alt C）。
2. **复用而非新建**：脊柱复用 `SessionOutcome`@close 写入锚点 + 投影软槽范式；向量层
   复用 FAQ 的 embedding/pgvector/门控栈——基建 0 新增。
3. **Anti-hardcode 合规**：召回是软信号、LLM-owned（沿用 `prior_use_case_carry`）；
   检索是机械的（embedding+实体匹配，与已获批的 FAQ 检索同类），**使用决策不落
   Java**。无 keyword/regex/per-UC 矩阵。
4. **Forward-looking**：6 个月后主流 LLM 大窗口 + 强摘要 → 让 LLM 在 close 时做压缩
   摘要、在召回槽里做取舍；向量只承担"选哪些进 context"（高频用户必需），不承担语义
   判断。避免堆砌很快会被 LLM 取代的 Java 检索启发式。
5. **风险可控**：分阶段让结构化脊柱先证明价值（且先把隐私治理补齐），再加语义层；每
   阶段 = 一个 sub-sprint 量级。

---

## 6. Scope split + delivery priority suggestion

> 建议（非决定）。最终 milestone/sub-sprint 拆分、是否独立成 milestone（"Customer
> Memory" milestone）由 deliver agent + human 定。强烈建议**独立 milestone**，因为
> 这是一条新能力轨，与 M5（observability）正交。

**Hard order（compounding-effect 驱动，见 §9）**：S0 身份 → S1 写入+隐私 → S2 实体
召回 → S3 向量层。S0 是所有后续的前置闸门；隐私治理必须随 S1 同步落地。

| 顺序 | sub-sprint（建议名） | 层(§3.2) | 范围（≤3 句） | 依赖 |
|---|---|---|---|---|
| **S0** | 跨会话身份键 wiring | `infra` | 把平台已认证 `account_id` surface 并 persist 到 `bot_sessions`（新列+索引），从 `GetCustomerContextTool`/账号上游回填；**不引入任何记忆**。这是"先修身份再谈记忆"的前置闸门。 | 无 |
| **S1** | 记忆写入 + 隐私治理 | `infra`+`skill_state`(+`semantic_planner` 摘要) | 新 `customer_memory` 表（account-scoped、结构化、**写入即脱敏**、TTL/保留期、GDPR 擦除钩子）；终态钩子（与 `SessionOutcome`/`RecordOutcomeTool` 共址）写入 **LLM 生成的结构化摘要**；**无召回**。 | S0 |
| **S2** | 实体召回软槽 | `prompt_projection` | 确定性实体通道；`prior_interactions` 软投影槽（沿用 `prior_use_case_carry` 模式）；经 `requiredContextKeys` 声明式 gate（与 M5-S3 协调）。real-LLM bad-case rerun 为证据闸。 | S1 + (建议)M5-S3 |
| **S3** | 向量语义召回层 | `prompt_projection`+`infra` | 给摘要算 embedding；语义 top-k + 相似度门控；与实体通道合并。 | S2 |
| 横切 | 记忆可观测性 | `infra` | 记忆召回/写入 trace 事件、admin 记忆审计视图、GDPR 擦除审计。搭 M5 的 trace 工作。 | 随 S1/S2 |

**优先级**：S0+S1 先行（即便不做召回，"身份键 + 合规存储"本身就降低现有无限期留存的
隐私债）；S2 是用户可感知价值的最小闭环；S3 在 S2 证明价值后追加。

---

## 7. Layer classification + §7 stanza pre-fill

### 7.1 §3.2 Fix Layer 分类（multi-layer prospective）

- **召回槽** → `prompt_projection`（§3.2 Q3：给 LLM 一个新软信号，LLM 拥有是否使用）。
- **跨会话持久/耐久** → `skill_state`（§3.2 Q4：跨轮/跨会话状态的持久性）。
- **表/migration/身份键/脱敏/TTL/擦除** → `infra`。
- **摘要内容质量** → `semantic_planner`（§3.2 Q5：摘要由 LLM 生成，语义所有权归 LLM）。
- **Tier-0**：默认**不新增** Tier-0 invariant；但**浮现一个 Tier-0 候选**（见下），按
  §3.2 Q2 路由 `human_review_required`——**不自行发明 Tier-0**。

### 7.2 §7 stanza 预填（草稿，供 deliver agent 填入 sprint_objective）

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection（召回软槽）+ skill_state（跨会话持久）
—— multi-layer prospective（per §7 multi-layer 变体）。infra（表/键/脱敏/TTL/擦除）
与 semantic_planner（close 摘要）为配套层。

**Tier-0 invariant:** 本工作默认不新增 Tier-0 invariant。但浮现一个 **Tier-0 候选**
交 human_review_required（§3.2 Q2）：跨会话记忆必须 (a) account-scoped（锚定已认证
账号，非自填 email）、(b) credential-free（绝不持久化密码/卡号/国民 ID，复用
`runtime_freeze_and_risk_policy` §3.3b）、(c) GDPR-erasable（按 account 可擦除）；
且**召回的记忆绝不满足本会话内的 `identity_verification_required` 门**（"上次已验证"
不能替代本次敏感动作的验证）。是否将其升格为正式 Tier-0 由 human 决定。

**Semantic hardcode:** 无 semantic hardcode。召回槽 `prior_interactions` 是软信号
（1:1 沿用 `prior_use_case_carry`：registry-driven、LLM-owned、无 per-UC 分支）；
记忆**选择**用 embedding 相似度 + 实体精确匹配（机械检索，与已获批的 FAQ 检索同类，
非语义决策分支）；记忆的**使用**（是否提起、如何措辞、是否据此跳过 intake）完全 LLM-
owned。无 keyword/regex/if-else/enum/per-UC 矩阵决定召回内容或动作。

**Generalization coverage:** target = 重复联系连续性（同账号、同 listing 跨会话）；
neighbor = 重复联系但不同相关问题（语义召回，S3）；negative = 首次联系（无记忆→槽空
→零行为变化）+ 不同账号（零泄漏）；shadow = held-out 重复联系 traces。bad-case 需新
seed（UC-A/UC-H removal-appeal 复访、UC-K account-access 复访、UC-C recurring
messaging）；counts 待 case-family sub-sprint 落定。
```

---

## 8. Hard fences + non-goals

**Hard fences（红线）：**

1. **绝不存原始 transcript**：只存 LLM 生成的结构化派生摘要（沿用
   `GetCustomerContextTool` 剥 PII 回派生事实的范式）。
2. **绝不以自填 email 作键**：键=平台已认证 `account_id`（S0 wiring 的产物）；
   account-bound chat 之外的会话（如纯 mock 自填）记忆能力降级为空。
3. **写入即脱敏 + TTL/保留期 + GDPR 擦除**必须随 S1 同步落地（不能先写后治理）。
4. **召回不得绕过会话内 Tier-0 门**：`identity_verification_required`、凭据处理保持
   per-session（召回的"上次已验证"不满足本次）。
5. **registry/Skill-driven，无 per-UC if-else**（§1.7）；召回槽经 `requiredContextKeys`
   声明，不硬编进 `ContextProjectionBuilder` 的 per-UC 分支。
6. **不改 escalation_reason 23 值 enum / tool schema / Tier-0 floor**。
7. **real-LLM rerun 为证据闸**；mocked-LLM 只覆盖召回渲染/写入 wiring（§5.6 eval
   evidence gate）。

**Non-goals：**

- 不做 Alt C（graph DB / 外部 memory 框架）。
- 不挤占 / 不取代 standing P0（Single Handover Orchestrator）。
- 不在本轨实现真实 Salesforce 集成（仍 mock；S0 在 mock 账号上 wiring `account_id`
  即可，real SF 是独立 production-gap 轨）。
- 不把记忆用于"让 eval 通过"（绝不编码 case text / source_session_id 进 runtime）。
- 不重构 `ControlKernel`/`PhaseEvaluator` phase 机。

---

## 9. Risk + compounding-effect analysis

**Compounding-effect / 顺序约束（错序会更糟）：**

1. **身份键必须最先（S0）**：在错误/未认证身份上建记忆 = 跨用户**记忆泄漏或错配**
   （把 A 的历史召回给输入了 A email 的 B）。这正是 research-agent role 中"先修
   DISCOVER mis-classification 再修 intake-prefill"反模式的同构——**先修身份，再谈
   召回**。S0 不先行，S1/S2 皆不可启动。
2. **隐私治理必须随写入同步（S1）**：本仓已有"原文无限期留存、无 TTL"的潜在合规债
   （§2.7）。先写记忆后补 TTL/擦除 = **放大**该债并制造 GDPR 责任面。写入与脱敏/
   TTL/擦除是同一 sub-sprint 的不可分项。
3. **召回槽建议在 M5-S3 之后（S2）**：M5-S3 正把投影转 Skill-driven（经
   `requiredContextKeys` gate 槽位），M5-S4 在做 denoise。若召回槽在 S3 前作为又一个
   常驻槽硬塞进 `ContextProjectionBuilder`，会给 M5-S4 制造新噪声、且不符合"声明式
   gate"方向。建议召回槽经 `requiredContextKeys` 接入（与 M5-S3 协调或紧随其后）。
4. **向量层依赖结构化脊柱（S3 after S1/S2）**：先有结构化摘要与身份键，才谈 embedding
   与语义召回。

**其它风险：**

- **召回与 drift/topic-shift 交互**：记忆召回不应削弱已有的 drift 检测/`prior_use_case_carry`；
  bad-case 需覆盖"复访 + 本次换了话题"的负样本（不要因为记得上次就把本次锚回旧 UC）。
- **召回过载**：高频 CS 用户记忆多 → 必须靠向量门控 + N 上限（类比 FAQ top-3 +
  `prior_use_case_carry` citation cap=3）控制注入体量，否则投影膨胀。
- **mock↔real 鸿沟**：S0 在 mock 账号上 wiring，real SF account binding 仍是 gap；
  记忆能力在 production 的真正启用依赖 account-bound chat 落地（与 P0/SF 轨耦合，需在
  milestone planning 标注）。
- **摘要漂移/幻觉**：LLM close 摘要可能夸大（"已解决"实际未解决）→ 摘要应带终态
  status 字段（resolved/escalated/abandoned，复用 `SessionOutcome.outcome`）作为
  事实锚，避免纯自然语言摘要漂移。
- **Stale 文档风险**：`prior_use_case_carry`/`requiredContextKeys`/`StateInheritance`
  细节会被 M5-S3 改动——dev 启动前需对 HEAD 复核（M5-S3 是否已改投影 gate 机制）。

---

## 10. Observability / trace / report implications

- **召回可观测**：每次记忆召回应像 tool call 一样可追踪——召回了哪些 memory、相似度
  分、为何 surface/未 surface。投影槽本身已随 `projected_context` 每轮持久化，但需补
  一个"memory recall"事件维度（可搭 M5-S2 的 per-invocation trace）。
- **写入可观测**：close 时写了什么摘要、脱敏前后、命中哪个 account_id——需 memory-write
  trace。
- **GDPR 审计**：擦除操作必须留审计（谁/何时/擦了哪个 account 的哪些行），且擦除是
  GDPR case（24.6%）的合规闭环的一部分。
- **admin 视图**：建议一个 admin 记忆审计页（某 account 的记忆行 + 擦除按钮），与 M5
  的 admin trace 工作风格一致。
- **eval report**：bad-case 复访场景的 trace 需在 report 里能看到"召回了 prior
  interaction X，LLM 据此做了 Y"，供人审判断召回是否帮到/干扰了问题解决。

---

## 附录 A — Coverage check（与现有 backlog / scope 对比）

- `docs/action_bank.md` §5 全量开放 R-item 中**无任何跨会话客户记忆条目**（grep
  "memory"/"cross-session" 命中的均为 deliver-agent memory 文件或会话内
  conversation memory，非客户长期记忆）。**结论：本能力 net-new，无重叠 R-item。**
- 邻接但不同：(a) `HandoverOrchestrator` design freeze（P0，exactly-once by
  session_id）——关乎 handover 持久化/case 关联，**非**记忆；(b)
  `R-uc-cdf-get-customer-context-bot-actual-usage`——bot 是否真的用 customer
  context，是"上下文富化使用"轨，记忆是其跨会话外延（可在 milestone planning 时关联，
  但不合并）。
- 与活跃 M5 正交（M5=observability display/trace；记忆=新能力轨），但 S2 召回槽与
  M5-S3 的 `requiredContextKeys` 投影 gate 机制**有顺序耦合**（见 §9.3）。

## 附录 B — 待 human / deliver 决策的开放问题

1. 是否独立成 "Customer Memory" milestone（建议是），还是并入某 M6+ 主题？
2. §7.2 浮现的 Tier-0 候选（account-scoped + credential-free + erasable + 不绕过会话内
   验证门）是否升格为正式 Tier-0？（§3.2 Q2 要求 human_review_required）
3. 保留期/TTL 的具体时长（产品/合规决策，非 research 可定）。
4. S0 身份 wiring 是否等 real SF account-binding，还是先在 mock `account_id` 上做
   （建议先 mock 做，解耦 SF production-gap 轨）。
5. 记忆能力是否对所有 UC 一致开放，还是先在高复访价值 UC（UC-A/UC-H/UC-K/UC-C）试点
   （建议经 `requiredContextKeys` 让相关 Skill 声明，自然实现 per-Skill 试点）。
