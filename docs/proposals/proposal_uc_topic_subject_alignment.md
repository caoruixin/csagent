---
title: UC 分类与 Topic Subject 拉齐方案
doc_tier: proposal
status: partial
implementation_status: partial
runtime_contract: false
last_reviewed: 2026-05-10
review_cadence: on_reactivation
notes: >
  Proposal whose top banner reports the alignment as confirmed and
  executed (2026-04-21) with foundational docs updated. Treat this file
  as a historical decision record explaining the change; the current
  runtime contract for UC ↔ Topic Subject mapping lives in the
  foundational specs (phase0/phase1/phase2/phase3) and the tool spec
  YAML, plus live code. Verify before relying on any specific mapping
  detail here.
---

> This document is not the current runtime contract unless a docs/current/* contract or live code path confirms it.

# UC 分类与 Topic Subject 拉齐方案

> **状态**: ✅ **已确认并执行**（2026-04-21）
>
> **决策确认**：
> 1. UC-E-01 归入 "Technical Support"（方案 A）✅
> 2. 4 个 handover-only Topic Subject 固定话术已从真实对话提取并更新到 `fixed_script_library_v1.md` §9 ✅
> 3. 新表单已上线，Topic Subject 分布变化符合预期 ✅
> 4. `form_topic_subject` 保持用户原始选择，Bot 不覆写 ✅
>
> **已更新的文档**：
> - `phase2_domain_realization_spec.md` — 12 UC 全部增加 `parent_topic_subject` + 新增 §2.11 Topic Subject 路由策略
> - `customer_service_tool_spec_v0_2.yaml` — `use_cases` section 增加 `parent_topic_subject` + handover-only TS 注释
> - `phase0_normative_freeze.md` — §0.3 UC 集合行更新为两层分类 + handover payload 增加 `form_topic_subject`
> - `phase1_solution_input_pack.md` — §1.1.2 scope 表格改为 Topic Subject 聚合 + §1.4.13 映射表升级为正式两层映射
> - `fixed_script_library_v1.md` — 新增 §9 Out-of-Scope Topic Subject 模板（4 条 + 通用 fallback）
> - `phase3_detailed_technical_design.md` — session table 增加 `form_topic_subject` 列 + DISCOVER 路由逻辑更新
> - `phase5_evaluation_design.md` — 增加 Topic Subject 维度指标 + eval reporting dimensions
>
> **目标**：将 V1 UC 集合与 Pre-chat Form 的 11 个 Topic Subject 对齐，使 Bot 效果评估与业务 Case 分类一致。
>
> **约束**：不改变控制内核、工具层、handover contract 等母规范项（phase0）；只调整分类映射层。

---

## 1. 设计原则

1. **Topic Subject 是 L1（业务评估维度）**：用户在 Pre-chat Form 中选择，100% 覆盖（新版表单必填下拉），业务方按此维度看 containment / CSAT / escalation 等指标。
2. **UC 是 L2（Agent 路由/处理维度）**：Bot 根据 Topic Subject + Description 文本做意图分类，决定处理路径（FAQ / intake+handover / direct handover）。
3. **每个 UC 必须有且仅有一个 `parent_topic_subject`**：标识该 UC 在业务报表中归属哪个 Topic Subject。评估时按 `parent_topic_subject` 聚合。
4. **一个 Topic Subject 可包含多个 UC**（1:N）：如 "Ad Support" 下有 4 个 UC，各自有不同风险等级和处理逻辑。
5. **UC 可以被跨 Topic Subject 激活**：用户选了 "Account Support" 但描述的是 GDPR 请求 → 意图分类器路由到 UC-G（parent = "Delete My Account or Data"）。这是 cross-UC routing（已有机制），不影响分类归属。
6. **未被 UC 覆盖的 Topic Subject = direct handover**：不创建空壳 UC，而是在 routing 层定义 fallback 规则。

---

## 2. 完整映射表：Topic Subject → UC（新旧对照）

### 2.1 已覆盖的 Topic Subject（7 个，含 11 个 UC）

| # | Topic Subject (L1) | UC ID (L2) | UC 名称 | 类型 | 变更说明 |
|---|---|---|---|---|---|
| 1 | **Account Support** | UC-D-01 | Account & Login (Non-Sensitive) | FAQ-resolvable | **新增 `parent_topic_subject: Account Support`** |
| 2 | **Ad Support** | UC-A-01 | Ad Status & Visibility | FAQ-resolvable | **新增 `parent_topic_subject: Ad Support`** |
| 2 | | UC-B-01 | Posting & Editing Guidance | FAQ-resolvable | **新增 `parent_topic_subject: Ad Support`** |
| 2 | | UC-FP-01 | Correct Deletion / Compliance Takedown | FAQ-resolvable (fixed script) | **新增 `parent_topic_subject: Ad Support`** |
| 2 | | UC-H-01 | Incorrect Deletion Appeal | Intake + Case + Handover | **新增 `parent_topic_subject: Ad Support`** |
| 3 | **Delete My Account or Data** | UC-G-01 | GDPR / Data Deletion Intake | Intake + Handover | **新增 `parent_topic_subject: Delete My Account or Data`** |
| 5 | **Payments** | UC-F-01 | Payment Inquiry (Non-Dispute) | FAQ-resolvable | **新增 `parent_topic_subject: Payments`** |
| 5 | | UC-I-01 | Refund / Payment Dispute Intake | Intake + Handover | **新增 `parent_topic_subject: Payments`** |
| 9 | **Replies or Messaging** | UC-C-01 | Messages & Replies | FAQ-resolvable | **新增 `parent_topic_subject: Replies or Messaging`** |
| 10 | **Report a Safety Issue** | UC-J-01 | Trust & Safety / Fraud Report | Intake + Case + Handover | **新增 `parent_topic_subject: Report a Safety Issue`** |
| 11 | **Technical Support** | UC-K-01 | Technical Issue Intake | Intake + Case + Handover | **新增 `parent_topic_subject: Technical Support`** |

### 2.2 未覆盖的 Topic Subject（4 个 → Direct Handover）

| # | Topic Subject | V1 UC | Bot 行为 | 说明 |
|---|---|---|---|---|
| 4 | **Delivery** | 无 | 固定话术 → `request_handover` | Gumtree 是广告平台，配送由买卖双方自行处理；少量涉及 Gumtree 官方配送服务的咨询需人工 |
| 6 | **Pro Contract** | 无 | 固定话术 → `request_handover` | B2B 商业合同咨询，需专属团队处理 |
| 7 | **Account Manager Support** | 无 | 固定话术 → `request_handover` | 大客户经理支持，V1 不覆盖 |
| 8 | **Ratings Reviews** | 无 | 固定话术 → `request_handover` | 评价评论系统咨询，V1 不覆盖 |

**处理策略**：
- 不新建 UC，在 routing 层增加规则：`if topic_subject ∈ {Delivery, Pro Contract, Account Manager Support, Ratings Reviews} → DISCOVER 阶段直接标记 out_of_v1_scope → 固定话术 → request_handover`
- 固定话术示例：*"Thanks for reaching out. For [Topic Subject] queries, I'll connect you to the team who can best help. Let me pass this over now."*
- 在 `fixed_script_library` 中增加 4 条 `out_of_scope_topic` 模板
- 评估时这 4 类以 `topic_subject` + `outcome = handover_out_of_scope` 独立统计，不归入任何 UC

### 2.3 UC-E-01 处置（孤儿 UC）

#### 问题

UC-E-01（General Product & Search，3,811 cases / 3.2%）没有对应的 Topic Subject 下拉选项。在新版表单中，用户无法选择"通用产品与搜索"——他们会根据实际问题选择最接近的 Topic Subject（如 "Technical Support"、"Ad Support"、"Account Support" 等）。

#### 方案对比

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| **A. 归入 Technical Support** | UC-E-01 的 `parent_topic_subject` 设为 "Technical Support"，与 UC-K-01 并列 | 改动最小；"产品功能/搜索使用"归类到 "Technical Support" 在用户视角合理 | UC-E（FAQ-resolvable）与 UC-K（intake+handover）处理模式差异大，共存于同一 Topic Subject 需在意图分类时精确区分 |
| **B. 消解 UC-E，分散到相关 UC** | 将 UC-E 的 knowledge scope 和 example queries 分配到 UC-A/B/D/K 等相关 UC；删除 UC-E-01 定义 | 最"干净"的拉齐方案；无孤儿 UC | 改动大（tool_spec allowed_use_cases / 知识库映射 / eval 数据集 / routing 规则全要改）；失去"通用产品问题"的独立追踪能力 |
| **C. 保持 UC-E，标记为跨 Topic Subject** | UC-E 的 `parent_topic_subject` 设为 `null` 或 `cross_topic`，可在任何 Topic Subject 下激活 | 零改动；保留独立追踪 | 破坏"每个 UC 必须有唯一 parent_topic_subject"原则；评估时 UC-E 的数据不属于任何 Topic Subject，难以向业务解释 |

#### **推荐方案 A：归入 Technical Support**

理由：
1. **用户视角**：新表单中，选 "Technical Support" 的用户包含两类：(a) 真正的技术故障 → UC-K，(b) 功能使用疑问 → UC-E。这与 "Technical Support" 的自然语义吻合。
2. **评估一致性**：UC-E 的 containment rate 和 UC-K 的 handover rate 分别贡献 "Technical Support" 的整体指标，业务方可看到 "Technical Support 中有 X% 被 Bot 直接解决（UC-E）, Y% 需要转人工（UC-K）"。
3. **改动最小**：UC-E 的定义、tool allowed_use_cases、知识库映射、eval 数据集全部保留，只新增一个字段。
4. **意图区分已有基础**：UC-E 的 example queries（"How does search work?"、"What features does Gumtree offer?"）与 UC-K 的 example queries（"App crashes"、"Can't login"、"Payment page bug"）语义差异明显，意图分类器可以区分。

---

## 3. 变更汇总

### 3.1 UC Registry 字段变更

**每个 UC 定义新增字段**：

```yaml
# 在每个 UC 的 YAML schema 中增加
parent_topic_subject:
  type: string
  description: >
    该 UC 在业务评估中归属的 Topic Subject（Pre-chat Form 下拉值）。
    评估按此字段聚合。跨 Topic Subject 路由时保留 form 原始值用于分析。
  enum:
    - Account Support
    - Ad Support
    - Delete My Account or Data
    - Delivery          # V1 无 UC，仅用于 out_of_scope routing
    - Payments
    - Pro Contract      # V1 无 UC，仅用于 out_of_scope routing
    - Account Manager Support  # V1 无 UC
    - Ratings Reviews          # V1 无 UC
    - Replies or Messaging
    - Report a Safety Issue
    - Technical Support
```

### 3.2 完整新旧对照表

| UC ID | UC 名称 | 旧版归属 | **新增 `parent_topic_subject`** | 处理类型 | 其他变更 |
|-------|--------|---------|-------------------------------|---------|---------|
| UC-A-01 | Ad Status & Visibility | （无） | **Ad Support** | FAQ-resolvable | 无 |
| UC-B-01 | Posting & Editing Guidance | （无） | **Ad Support** | FAQ-resolvable | 无 |
| UC-C-01 | Messages & Replies | （无） | **Replies or Messaging** | FAQ-resolvable | 无 |
| UC-D-01 | Account & Login (Non-Sensitive) | （无） | **Account Support** | FAQ-resolvable | 无 |
| UC-E-01 | General Product & Search | （无，孤儿） | **Technical Support** | FAQ-resolvable | 与 UC-K-01 共享 Topic Subject |
| UC-F-01 | Payment Inquiry (Non-Dispute) | （无） | **Payments** | FAQ-resolvable | 无 |
| UC-FP-01 | Correct Deletion / Compliance Takedown | （无） | **Ad Support** | FAQ-resolvable (fixed script) | 无 |
| UC-G-01 | GDPR / Data Deletion Intake | （无） | **Delete My Account or Data** | Intake + Handover | 无 |
| UC-H-01 | Incorrect Deletion Appeal | （无） | **Ad Support** | Intake + Case + Handover | 无 |
| UC-I-01 | Refund / Payment Dispute Intake | （无） | **Payments** | Intake + Handover | 无 |
| UC-J-01 | Trust & Safety / Fraud Report | （无） | **Report a Safety Issue** | Intake + Case + Handover | 无 |
| UC-K-01 | Technical Issue Intake | （无） | **Technical Support** | Intake + Case + Handover | 与 UC-E-01 共享 Topic Subject |

### 3.3 Topic Subject 覆盖率汇总

| Topic Subject | UC 数量 | FAQ-resolvable | Intake+Handover | 历史 Case 量（估） | 备注 |
|---|---|---|---|---|---|
| **Ad Support** | 4 | UC-A, UC-B, UC-FP | UC-H | ~33,541 (27.9%) | 最复杂的 Topic Subject；意图分类需区分 4 UC |
| **Account Support** | 1 | UC-D | — | ~4,906 (4.1%) | 1:1 映射；但历史数据显示 37% 实际为 UC-H spillover，需靠 description 纠偏 |
| **Delete My Account or Data** | 1 | — | UC-G | ~29,638 (24.6%) | 1:1 映射；强先验信号（70%） |
| **Payments** | 2 | UC-F | UC-I | ~3,488 (2.9%) | 需区分"咨询"vs"争议"，风险等级差异大 |
| **Replies or Messaging** | 1 | UC-C | — | ~2,627 (2.2%) | 1:1 映射 |
| **Report a Safety Issue** | 1 | — | UC-J | ~6,238 (5.2%) | 1:1 映射；强先验信号（71%） |
| **Technical Support** | 2 | UC-E | UC-K | ~10,852 (9.0%) | UC-E (FAQ) + UC-K (intake)；需区分"功能疑问"vs"故障" |
| **Delivery** | 0 | — | — | ~1,200 (1.0%) | Direct handover |
| **Pro Contract** | 0 | — | — | ~300 (0.2%) | Direct handover |
| **Account Manager Support** | 0 | — | — | ~300 (0.2%) | Direct handover |
| **Ratings Reviews** | 0 | — | — | ~1,500 (1.2%) | Direct handover |

---

## 4. Session State 与 Eval 的 Topic Subject 集成

### 4.1 Session State 新增字段

```yaml
# session_state 增加
form_topic_subject:
  type: string
  description: 用户在 Pre-chat Form 中选择的 Topic Subject 原始值（不可变）
  source: form_context.topic_subject

resolved_topic_subject:
  type: string
  description: >
    最终用于评估的 Topic Subject。
    通常 = form_topic_subject。
    当跨 UC 路由发生时（如用户选 "Account Support" 但实际是 GDPR → UC-G），
    resolved_topic_subject 仍保持 form 原始值（"Account Support"），
    但 active_use_case 变为 UC-G-01。
    这样评估可以同时切两个维度。
```

### 4.2 Eval 报告双维度

评估报告需同时提供：
- **Topic Subject 维度**（业务视角）：按 `form_topic_subject` 聚合 containment / CSAT / escalation 等
- **UC 维度**（Agent 视角）：按 `active_use_case` 聚合 routing accuracy / grounding quality / tool compliance 等
- **交叉维度**：Topic Subject × UC 矩阵，识别"用户选了 A 但实际问的是 B"的分布

### 4.3 Handover Payload 增加字段

```yaml
# handover payload 增加
form_topic_subject: "Account Support"  # 用户原始选择
primary_use_case: "UC-G-01"           # Bot 分类结果
topic_uc_mismatch: true               # 标记跨 Topic Subject 路由
```

---

## 5. 意图分类器设计影响

### 5.1 两阶段分类

```
Stage 1: Topic Subject 先验
  ├─ 强先验（>70%）：直接路由
  │   - "Delete My Account or Data" → UC-G-01
  │   - "Report a Safety Issue" → UC-J-01
  ├─ 无 UC（4 个）：直接 handover
  │   - "Delivery" / "Pro Contract" / "Account Manager Support" / "Ratings Reviews"
  └─ 弱先验（≤70%）：进入 Stage 2

Stage 2: Description 文本分类
  ├─ 在 Topic Subject 对应的 UC 候选集内做意图分类
  │   - "Ad Support" → {UC-A, UC-B, UC-FP, UC-H} 四选一
  │   - "Payments" → {UC-F, UC-I} 二选一
  │   - "Technical Support" → {UC-E, UC-K} 二选一
  │   - "Account Support" → {UC-D} 唯一（但需检测 UC-H/G spillover）
  └─ 若分类结果指向其他 Topic Subject 的 UC → 跨 UC 路由
```

### 5.2 Topic Subject 内 UC 消歧关键信号

| Topic Subject | UC 消歧 | 关键信号 |
|---|---|---|
| **Ad Support** | UC-A vs UC-B vs UC-FP vs UC-H | 状态查询 → A；发帖/编辑 → B；被删+理解/接受 → FP；被删+申诉/不服 → H |
| **Payments** | UC-F vs UC-I | 流程/规则咨询 → F；退款/争议/扣款错误 → I |
| **Technical Support** | UC-E vs UC-K | 功能使用/搜索方法 → E；故障/报错/无法操作 → K |
| **Account Support** | UC-D（+ spillover 检测）| 登录/密码/设置 → D；被封 → UC-FP/H（跨 TS 路由）；删号 → UC-G（跨 TS 路由）|

---

## 6. 对 Cross-UC Routing 的影响

现有 cross-UC routing 规则（phase2 §2.7）**不需要变更**。Topic Subject 是 form 层面的静态标签，UC routing 是 Agent 层面的动态决策。两者独立运作：

| 场景 | form_topic_subject | 初始 UC | 路由后 UC | 说明 |
|------|---|---|---|---|
| 用户选 "Account Support"，描述 GDPR | Account Support | UC-D-01 | → UC-G-01 | 跨 TS 路由；form_topic_subject 不变 |
| 用户选 "Ad Support"，从 UC-FP 转为申诉 | Ad Support | UC-FP-01 | → UC-H-01 | 同 TS 内路由；自然 |
| 用户选 "Payments"，涉及退款争议 | Payments | UC-F-01 | → UC-I-01 | 同 TS 内路由；自然 |
| 用户选 "Technical Support"，问搜索怎么用 | Technical Support | (分类阶段) | UC-E-01 | 直接命中 UC-E（同 TS 下） |

**无需修改的规则**：phase2 §2.7 中的 12 条 cross-UC routing 规则保持原样，因为它们基于 UC 而非 Topic Subject 运作。

---

## 7. 具体变更清单

### 7.1 需要修改的文件（按优先级排序）

| 优先级 | 文件 | 变更内容 | 工作量 |
|--------|------|---------|--------|
| **P0** | `phase2_domain_realization_spec.md` | §2.2 每个 UC 定义增加 `parent_topic_subject` 字段；§2.1 增加两层分类说明；新增 §2.x "Topic Subject 路由策略"（含 4 个 handover-only TS 规则 + UC-E 归属说明） | 大 |
| **P0** | `customer_service_tool_spec_v0_2.yaml` | `use_cases` section 增加 `parent_topic_subject` per UC；无需改 allowed_use_cases（UC ID 不变） | 中 |
| **P1** | `phase0_normative_freeze.md` | §0.3 "V1 use case 集合" 行更新：增加 parent_topic_subject 维度说明 + 4 个 handover-only TS | 小 |
| **P1** | `phase1_solution_input_pack.md` | §1.1.2 scope 表格增加 Topic Subject 列；§1.4.13 映射表改为正式两层映射 | 中 |
| **P1** | `fixed_script_library_v1.md` | 增加 4 条 `out_of_scope_topic` 固定话术模板（Delivery / Pro Contract / Account Manager Support / Ratings Reviews） | 小 |
| **P2** | `phase3_detailed_technical_design.md` | 增加 session_state 的 `form_topic_subject` / `resolved_topic_subject` 字段设计；routing layer 增加 out_of_scope_topic 规则 | 中 |
| **P2** | `phase5_evaluation_design.md` | eval 报告增加 Topic Subject 维度；eval dataset 增加 `parent_topic_subject` 标签 | 中 |
| **P3** | `phase4_coding_agent_implementation_packet.md` | UC 引用处补充 Topic Subject 上下文 | 小 |

### 7.2 不需要修改的内容

- **UC ID**：全部保留（UC-A-01 ~ UC-K-01 + UC-FP-01），无 ID 变更
- **tool_spec allowed_use_cases**：UC ID 不变，无影响
- **control kernel state machine**：不涉及 Topic Subject，无变更
- **handover payload contract**：增加 `form_topic_subject` 字段但不改已有字段
- **eval datasets 标签**：可后续增量打标 `parent_topic_subject`，不需要重新构建

---

## 8. 待确认项

| # | 问题 | 建议 | 确认方 |
|---|------|------|--------|
| 1 | UC-E-01 归入 "Technical Support" 是否接受？还是偏好方案 B（消解）或 C（跨 TS）？ | 推荐方案 A | 产品/设计 |
| 2 | 4 个 handover-only Topic Subject 的固定话术是否需要更细化的模板（如 Delivery 需说明配送责任归属）？ | 建议各写一条 ~2 句话的模板 | 产品/Ops |
| 3 | 历史数据中 "Account Support" 37% 实际为 UC-H — 新表单上线后此比例是否会下降（因为有 "Ad Support" 选项）？需要在 Bot 上线后观测 | 上线后 2 周出数据 | Data/Ops |
| 4 | `form_topic_subject` 是否需要在 Salesforce Case 字段中回写？（当前 Case 的 `Topic Subject` 来自表单，Bot 不修改） | 保持表单原值，Bot 不覆写 | Salesforce Admin |
