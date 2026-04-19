# Phase 1 — Solution Input Pack

> 汇总业务、领域知识、运营模型、工程约束、评测输入与品牌口径，形成进入 Domain Realization 的完整素材包。
>
> **输入来源**:
> - `BRD.md`（business requirements、user stories、UX、tone-of-voice、launch criteria）
> - `PRD_biz_part.md`（business-only PRD: 系统角色、handling 状态机、A–F' 对话流、Salesforce 数据模型骨架）
> - `case-data-stat.md`（120,367 条 Case 历史数据分析 — **Case Reason 维度**）
> - `case-samples.md`（原始 Case 样本）
> - `customer_service_conversation_samples_organized.xlsx`（499 条 Live Chat 会话，其中 262 条含完整 transcript）
> - `inferred_tool_candidates_from_human_conversations.xlsx`（从 262 条人工对话 transcript 反推的 10 类工具候选）
> - `customer_service_tool_spec_v0_2.yaml`（**v4 更新** — V1 Tool Spec v0.2，含 concrete API mapping、pre-chat form integration、pgvector backend、`get_moderation_review_context` 新工具、`fixed_script_library` runtime capability）
> - `salesforce-part-spec.md`（**v4 新增** — Salesforce 组织配置确认：Enhanced Chat / Omni-Channel / Knowledge API / Case 字段 / Queue / Off-hours / Pre-chat Form / 自定义对象）
> - `problem_retrieval_solution_plan_pgvector.md`（**v4 新增** — pgvector 检索方案确认：分块策略 / 索引 / 在线检索 / Prompt 设计）
> - `customer_service_agent-Common-Phrases.md`（**v4 新增** — 从 1.18 万条真实 transcript 提炼的客服标准话术指导，含开场/共情/等待/索取信息/升级/收尾 7 类模板）
> - `FAQ-knowledge_include_help_url.csv`（**v4 新增** — 218 篇 Salesforce Knowledge 文章，含 Id / Title / Summary / Help_Site_URL__c；knowledge grounding 的主数据源）
> - `platform_api_detailed_reference.md`（**v4 新增** — 196 REST 端点 / 14 微服务详细 API 参考；tool_spec v0.2 的 concrete_api_dependencies 源）
> - `data/eval_datasets/*`（**v4 新增** — 已构建的 7 类 eval 数据集，共 601 session / 11,288 turns + 367 条 human review queue）
> - `07-engineering-constraints.md`（Gumtree 平台代码库提取的工程基线）
> - `customer_service_agent_tech_spec.md` §4 / `customer_service_agent_eval_spec.md`（规范层引用）

---

## 1.1 Business Input

### 1.1.1 目标

| 维度 | 内容 |
|------|------|
| **业务目标** | 在 Gumtree 官网 Salesforce Enhanced Chat 上引入自定义 Chat Bot 作为首触达，通过 FAQ 自助与引导式对话提升自助解决率，降低坐席重复性工作量 |
| **三大核心结果**（BRD §1）| (1) 通过自助分流降低可避免的客户接触；(2) 通过更快的解答与更清晰的下一步提升客户体验；(3) 通过减少坐席重复劳动提升支持效率 |
| **服务用户** | 所有通过 Web Enhanced Chat 发起咨询的 Gumtree 用户（买家、卖家、新老用户）；尤其在高峰期与非工作时段联系的客户（BRD §3.3） |
| **内部受影响方** | Customer Support Agents、Team Leaders、Knowledge Owners、Salesforce Admins / Operations |
| **明确反目标** | 非"替代坐席 / 降编"目标。释放产能以处理更复杂工单（BRD §1） |

### 1.1.2 Scope

#### In-scope 场景（Phase 1，代号 A–F'，按 PRD §1.6.2 量级）

**按全量 Case Reason 体量**：

| 代号 | 类别 | 案例量 | 占比 |
|------|------|-------|------|
| A | 帖子/广告状态与可见性 | 5,506 | 4.6% |
| B | 发帖与编辑指引 | 3,160 | 2.6% |
| C | 消息与回复 | 2,627 | 2.2% |
| D | 账户与登录-非敏感执行 | 4,906 | 4.1% |
| E | 通用产品与搜索 | 3,811 | 3.2% |
| F | 支付咨询-非争议执行 | 488 | 0.4% |
| F' | 正确删除/合规下架话术 | 20,875 | 17.3% |
| **合计 Phase 1 可承接** | — | **41,373** | **34.4%（占全库）/ 53.8%（占 Chat+Email 渠道）** |

#### Out-of-V1-Bot-Resolution 但需 intake + handover 的 Phase 1 场景（新增，对齐 tool_spec UC-G/H/I/J/K）

> 说明：tool_spec v0.1 将 UC-G / UC-H / UC-I / UC-J / UC-K 也列入 V1 use case registry，但 `allow_bot_resolution=false`；Bot 承接职责是**结构化 intake + `request_handover` + 对 UC-H/J/K 调 `create_case_controlled`**，不做自主回答或裁决。

| 代号 | 类别 | 案例量 | 占比 | V1 Bot 职责 |
|------|------|-------|-----|-----------|
| G | GDPR / 账号与数据 | ~30,000 | ~24.6% | 识别意图 → 固定话术解释流程 → 请求身份核验信息（由人工流程进行）→ handover |
| H | 删帖与误删申诉 | ~4,000 | ~3.3% | 收集 ad_id / email + 申诉原因 → `create_case_controlled` → handover |
| I | 支付退款与争议 | ~3,000 | ~2.5% | 固定话术（Bot 不做规则判定）+ 收集 order/payment 标识 → handover |
| J | 信任与安全 | ~6,238 | 5.2% | 结构化举报采集（举报对象 / ad_id / 描述）→ `create_case_controlled` → handover |
| K | 技术故障 | ~7,041 | 5.8% | 基本排障澄清（2 轮上限）→ 若无法解决 → `create_case_controlled` → handover |

#### 活体 Chat 样本分布（对齐 `customer_service_conversation_samples_organized.xlsx`，262 条 transcript）

Chat 渠道的 **实际** 会话分布与全量 Case Reason 分布差异显著 — 低风险 FAQ 类在 Chat 渠道占比极低，因用户更倾向于自助解决；Chat 渠道以复杂 / 申诉 / 情绪类为主：

| Use Case | transcript 条数 | 占 transcript 样本 | 平均 turns | escalation_signal=True 比例 |
|----------|----------------|-------------------|-----------|----------------------------|
| UC-H（incorrect deletion appeal）| 116 | 44.3% | 9.6 | 28.4% |
| UC-I（refund / payment dispute）| 50 | 19.1% | 13.6 | 12.0% |
| UNMAPPED（无法归类 / 内容稀疏）| 29 | 11.1% | — | — |
| UC-J（Trust & Safety）| 25 | 9.5% | 17.4 | 36.0% |
| UC-FP（correct deletion explanation）| 21 | 8.0% | 12.2 | 28.6% |
| UC-D（account & login）| 12 | 4.6% | 7.2 | 16.7% |
| UC-G（GDPR / data deletion）| 4 | 1.5% | 28.0 | 75.0% |
| UC-B / UC-C / UC-A 合计 | 5 | 1.9% | 19–24 | 0% |

**解读**：
- Phase 1 低风险 FAQ 类（A/B/C/E）在 Chat 渠道自然流入极少（≤1% each），说明"Bot 首触达挡掉这几类"的 containment gains 会来自**需求诱导/菜单引导**，而非被动承接。
- UC-H + UC-I + UC-J + UC-FP 合计约占 Chat transcript 样本的 81%，其中绝大多数 outcome = unclear 或 escalated_or_async_followup → **Bot 首触达必须把"intake + handover"流程设计为最重要的交付**。
- 237 条 Missed（无 transcript）沉没会话 — 客户进入 Chat 但坐席未接通/无响应，这部分是 Bot 上线后**最可能被首触达承接的流量**（可做 FAQ 分流 + 结构化采集）。
- 平均 turns：UC-G 最长（28.0）、UC-J 次之（17.4）、UC-I（13.6）、UC-FP（12.2）、UC-H（9.6）— Bot 的 `max_total_bot_turns_before_forced_escalation` 预算应对不同 UC 分层设置。

#### 设计桶分布（design_bucket，Phase 1 样本总 499）

| Bucket | 条数 | 设计意义 |
|--------|------|---------|
| manual_review | 262 | 需人工审核打标 → Golden Dataset / bad-case bank 初始池 |
| escalation_design | 207 | 直接证据集合 — 输入 Phase 2 escalation matrix 与 handover payload 设计 |
| self_serve_design | 30 | Phase 1 Bot FAQ 自助候选（30 条精选 curated examples）|

#### Out-of-scope（BRD §1.3、PRD §4.3）

- 语音机器人（IVR / 语音 Bot）
- 社交渠道（WhatsApp / Facebook Messenger 等）
- 支付 / 退款执行 / 强身份验证自动化
- 复杂纠纷 / 法律投诉 / 欺诈自动裁决
- Open-ended GenAI 自由问答
- 替代坐席 / 降编为目的

#### 高风险场景（必须人工或固定话术）

- 支付争议 / 退款执行 / chargeback / INAD
- GDPR 数据删除 / 账户删除（约 3.1 万条，约 24.6%）
- 误删申诉 / 黑名单复核（约 4k+）
- 欺诈与举报判定 / Trust & Safety（约 1 万+）

#### 必须转人工（BRD §B 功能要求 + PRD §1.6.2 边界）

- 用户主动请求人工
- 高风险类别命中
- 申诉 / 复核请求
- 用户表达情绪 / 威胁
- `faq_miss ≥ 2`
- 澄清 2 轮仍无法理解
- 命中 out-of-scope

### 1.1.3 Use Cases / Cases

- **高频 Case Reason**（case-data-stat §1.2）：GDPR/Data Deletion 24.6%、Ads 22.9%、Technical Issue 5.8%、Trust & Safety 5.2%、Accounts 4.3%。
- **典型用户提问样本**：见 `case-samples.md`（Subject / Case Reason / Sub Reason / Opened Date / Case Origin），Sub Reason 约 200 种，适合"规则映射 + 意图识别"混合策略。
- **Live Chat 真实对话样本（新增）**：`customer_service_conversation_samples_organized.xlsx`
  - 499 会话元数据 + 262 条完整 transcript + 2,890 条 turn 级数据 + 43 条 curated examples
  - 已按 `mapped_use_case / risk_tier / likely_outcome / frustration_flag / escalation_signal / identifier_present_or_requested / design_bucket` 标注
  - Curated examples 分 4 类：`self_serve_candidate`(10) / `handover_or_escalation`(15) / `identifier_collection`(10) / `frustration_or_failure_mode`(7) — 直接作为 Golden Dataset 初始素材与失败样本池
- **工具候选证据（新增）**：`inferred_tool_candidates_from_human_conversations.xlsx`
  - 10 类工具候选，每个含 regex signal / matched_sessions / 5 条证据行
  - 证据支持：`lookup_listing_or_ad`(97) > `request_handover_to_human`(76) > `collect_required_identifier`(71) > `retrieve_help_or_policy_article`(57) > `lookup_customer_account`(43) > `explain_moderation_or_policy_reason`(40) > `send_followup_email_or_async_update`(38) > `moderation_enforcement_action`(25) > `record_outcome_and_trace`(18) > `create_support_case`(12)
  - 人工坐席的常见行为序列（给 Bot 设计参考）：`collect_required_identifier` → `lookup_listing_or_ad` / `lookup_customer_account` → `explain_moderation_or_policy_reason`（若 UC-FP）或 `request_handover_to_human`（若 UC-H/I/J/K）→ `send_followup_email_or_async_update`（由人工或后台发起）
- **Sub Reason / Case Reason 空白率**：12%（约 15,113 条）— 需意图模型补全，否则易落入"未知 → 转人工"。
- **历史 bad cases**：已有 262 条 transcript 中标注为 `frustration_flag=True` 或 `likely_outcome=abandoned_or_timeout` 的会话作为 bad-case bank 初始池；上线后持续累积。
- **真实数据来源**：Salesforce Case 报表 `report1768917670361.xlsb` + Live Chat Session 导出样本（conversation_samples_organized.xlsx 来源文件约 500 行原始数据）。

#### 关键对话模式（从 262 条 transcript 抽象）

| 模式 | 频次信号 | Bot 设计含义 |
|------|---------|------------|
| 坐席开场"Hello, my name is {X}. Thanks so much for reaching out..." | 高频固定句式 | Bot opening 需明确"非坐席"身份（disclosure），避免伪装 |
| 坐席主动索取 identifier（email / ad_id / phone）| 126/499（25%）| `collect_required_identifier` 作为受控动作；Bot 必须说明原因（"so I can locate your account"）|
| "This has been escalated to the relevant department... I'll revert back via email within 24 hours" | 频繁出现于 UC-H/G | Bot handover 话术需对齐此模式：明确队列 + TTFR 预期 + 跟进渠道 |
| 用户提供 identifier → 坐席"one moment please" → 解释/结论 | 占 UC-H/I/J 主流程 | Bot 需在 tool call 期间维持"正在查询"的占位话术（避免沉默引发 abandonment）|
| 用户重复追问"Are you still there?" / "?????" | frustration_flag 主要来源 | Bot 需主动提供进度反馈；`max_repeated_same_action` 需考虑用户重复提问信号 |
| "End chat" / 自动 idle 关闭 | 多数 UC-H 收尾 | abandonment 归因需区分"Bot 主动关闭" vs "用户离开" |
| Chat → Email 切换（"I'll send you an email"）| UC-H/G/K 共性 | V1 Bot 不直接发邮件；标记 handover_reason = async_followup_required，由坐席/后台触发 |

### 1.1.4 Success Metrics

#### 主 KPI（BRD §5.1）

| 类别 | 指标 | 目标 / 来源 |
|------|------|-----------|
| **效率与分流** | bot containment rate | D1 全自动结案 10–25%；D2 有效承接/分流 45–55% |
| | agent-handled chat volume | 在 in-scope intents 上下降 |
| | AHT for agent chats | 通过更好的 triage 与上下文，预期下降 |
| | queue performance | peak-time backlog 下降 / 响应时间改善 |
| **自助采纳** | Help Centre usage via chat | bot 推送文章被点击的会话占比 |
| | deflection rate | 接受自助答案后正常结束的客户占比 |
| **客户体验** | CSAT for bot conversations | **Bot 直接 resolve 的会话：session 结束后通过邮件发送 CSAT 问卷（v5 已确认机制）**；具体分数阈值待 Support + Product + Data 确认 |
| | CSAT for escalated (bot→agent) | **转人工的会话不下发 Bot CSAT**（避免与坐席 CSAT 混淆，v5 已确认）|
| | drop-off rate | 在 Bot 流程中放弃的客户占比，需在阈值内 |
| | AI 满意度（PRD §1.4）| 40%–70% |
| | AI 参评数 / 参评率（PRD §1.4）| 80–90% / 7% |
| **延迟** | FAQ answer p95 | ≤ 5s（tech_spec §18.2） |
| **成本** | chat capacity | 当前约 4.1 FTE / £65K/年；目标通过 containment 释放部分产能 |
| **运营保护** | repeat contact | 不应因失败 bot 旅程而上升 |
| | misrouted chats / cases | 不应上升 |
| | agent confirms handover context sufficient | 必须 |

#### Go / No-Go 上线条件（BRD §5.1）

- CSAT for bot journeys（bot-resolved 会话 email CSAT）不显著低于 baseline（>X 点跌幅前不扩流量；X 阈值待 Support + Product + Data 确认）
- Escalated journey CSAT 不显著下降
- Drop-off rate 在可接受阈值内
- **流量分配策略（v6 已确认，基于 GrowthBook）**：开发阶段 100% 打开；prod 环境按 **10% → 20% → 50% → 100%** 渐进放量；分桶标识使用 `gbUserPseudoId`（Cookie `gt_gb_exp_uid`，UUID v4，365 天）；所有 coverage % / variant weights / rollout % 均在 **GrowthBook Dashboard** 配置下发，代码层面不硬编码阈值；每阶段扩量前需通过 release gate 检查（containment / CSAT / wrong_containment 等）

### 1.1.5 品牌口径与体验要求（BRD §6 + §C UI 要求）

#### Bot 命名

- **客户侧命名**：`Gumtree Support Assistant`（或 `Support Assistant` / `Virtual Assistant`）
- **避免**：在客户 UI 称为 "AI"、"Chatbot"、"AI language model"
- **内部用语**：`Chatbot` 可用

#### 语气原则

- Friendly, clear, human — 避免过度机器人化
- Keep it short — UK 用户偏好简洁直接
- Be transparent — 明确告知是 automated assistant
- 避免企业化用语：`leveraging`、`delighted`、`we apologise for inconvenience`

#### 必备话术片段

| 场景 | 推荐措辞 |
|------|---------|
| Opening | "Hi — I'm the Gumtree Support Assistant. I can help with common questions, or connect you to the team." |
| Self-service | "Here's the best article for that — it should sort it in a couple of minutes." |
| Resolution check | "Did that answer your question?" → Yes: "Great — anything else I can help with?" / No: "No problem — I'll connect you to the team." |
| Escalation | "I'll pass this to an agent now. I'll share what you've told me so you don't need to repeat yourself." |
| Out of hours | "The team is currently offline. I can still help with common questions, or log this for follow-up." |

#### 禁用话术

- "I understand how you feel"（在 bot 答错时显得虚假）
- 反复 "I'm sorry to hear that"
- "As an AI language model..."
- 任何 implies bot 已采取行动而其实未确认的措辞（如 "I've fixed that"）

#### UI / Experience 要求

- 客户必须明确知道在与 bot 交互（disclosure）
- 不强迫客户走完长流程才能升级
- 必须提供 "Start again" 选项
- 必须支持 mobile 与 desktop
- 必须满足基本 WCAG 无障碍要求

---

## 1.2 Domain Knowledge Input

| 维度 | 内容 |
|------|------|
| **权威知识源** | Salesforce Knowledge（与 Help Centre 同步）|
| **知识库规模（v4 已确认，v6 修正）** | **218 篇文章**（`FAQ-knowledge_include_help_url.csv`）；每篇含 Id / Title / Summary / UrlName / Description__c（HTML rich text）/ Help_Site_URL__c（canonical URL）。注：原统计 3,963 系 CSV 行数（Description__c 含多行 HTML），实际文章数为 218 |
| **Grounding 内容范围** | Help Centre 文章、Community Standards / 政策页面、操作指南 |
| **Knowledge API 状态（v4 已确认）** | **当前无可用开发 API**（`salesforce-part-spec.md`）— Salesforce Knowledge API 不可用于在线实时检索；Bot 的知识检索**完全依赖 pgvector 离线索引**，Salesforce Knowledge 仅作为元数据与发布状态的权威源（通过 article Id 查询发布状态） |
| **检索后端（v4 已确认）** | **pgvector on Cloud SQL PostgreSQL**；chunk 策略 256–512 tokens sliding window + 10–20% overlap；article-level + chunk-level 双层表；在线检索 Top-2~3 chunks + Title + Summary → LLM 组装回复 |
| **不能作为事实源** | 用户自由输入内容、未审批的 Git 内部文档、外部网站内容 |
| **Freshness 要求** | 随 Salesforce Knowledge 发布状态同步；pgvector 索引需定期 re-embed（文章变更时 incremental update，见 `problem_retrieval_solution_plan_pgvector.md` §3.5） |
| **Ownership** | Knowledge & Content Ops 团队 |
| **当前已知 gaps** | (1) Sub Reason / Case Reason 空白约 12%（15,113 条），需意图模型补全；(2) ~~Help Centre 具体文章 ID/URL 与 use case 的精确映射尚未完成~~ → **初版已完成（v6）**：`data/knowledge/article_uc_mapping.csv` 已完成 218 篇文章的 auto-mapping（url_category 177 + title_keyword 37 + content_keyword 4）；`knowledge_base_articles.json` 已可用于 pgvector 入库；218 篇即为 CSV 全量（原 3,963 系行数误计），全部已完成初版映射（human review 后续校准）；(3) ~~UC-FP-01 政策解释模板需合规终审~~ → 已通过（v5）；(4) ~~UC-H/J/K required_fields 合约~~ → 已有初版（v4）；(5) ~~UC-G 身份核验流程~~ → GDPR intake 边界已确认（v5）|
| **可选辅助索引** | Git 仓库内部文档（PRD §B-OP-01；需治理审批后建标签隔离 collection）|
| **广告审核原因数据源（v4 新增）** | tool_spec_v0.2 新增 `get_moderation_review_context`（gumshield cs-review API），为 UC-FP 提供具体删除/审核原因的 grounded 事实依据，避免只能给 generic "policy violation" 解释 |

---

## 1.3 Operating Model Input

| 维度 | 内容 |
|------|------|
| **转人工接收方** | Customer Support Agents（通过 Salesforce Omni-Channel 路由）|
| **人工系统 / queue（v4 已确认）** | 在线聊天 queue：**CS_NEW_chat**（坐席主动认领会话）；离线 Case queue：**CS_Cases_New**；坐席最大并发 **2** 会话 |
| **Handover 后流程** | 坐席在同一 Messaging Session 线程中接续；坐席可见 intent / confidence / structured answers / articles_shown / Case Id（如有）/ transcript（完整或摘要，按性能与隐私评审）；对 UC-G/H/J/K 类已通过 `create_case_controlled` 创建 Case 的，坐席可直接基于 Case payload 继续；异步跟进（邮件）**遵循现有 Salesforce 人工 CS 系统的规则/模版/内容，Bot 不直接向用户发邮件**；由坐席或 back-office 通过 `send_followup_email_or_async_update`（human_only tool）触发 |
| **工作时间限制（v4 已确认）** | **判定逻辑**：检查 Messaging queue "New Chat" 是否有 agent 在线；有 → 工作时间；无 → 离线时段。离线时 Bot 可继续 FAQ；复杂问题以 "log this for follow-up" 话术承接，工单转到 **CS_Cases_New** queue 待坐席离线处理。tool_spec_v0.2 `request_handover` 增加 `is_business_hours` 输入 + `offline_logged` 状态 |
| **Bot 最大会话轮次（v4 已确认）** | Enhanced Chat Web v1 约束：自研 Bot **最多 50 次会话轮次**，超过必须 transfer to human。此约束叠加在 tech_spec `max_total_bot_turns_before_forced_escalation` 之上（取两者中较小值） |
| **SLA / TTFR** | 现有 AHT 469s；目标降低转人工后首响时间 |
| **QA / Ops owner** | Customer Service Management + Knowledge Owners |
| **Bad-case bank owner** | QA / Ops 团队，weekly review cadence |
| **业务 / 产品 owner** | Lance Li（产品）/ Chelsea Fagan-Hall（业务）/ Alanna Barron（exec sponsor）|

### 1.3.1 系统角色（PRD §2）

| 角色 | 职责边界 |
|------|---------|
| **End User（客户）** | 通过 Gumtree help centre Enhanced Chat 发起；可随时显式转人工 |
| **Custom Chat Bot（自研 Bot 服务）** | 无独立 C 端 UI，嵌入 Enhanced Chat；一期职责 = FAQ 意图识别 + 检索增强回答 + 解决确认 + 触发 Case 创建（按规则）+ 请求转人工并附上下文 |
| **Human Agent（人工坐席）** | 在 Service Console / Omni 处理；接收 Bot 结构化上下文；一期不要求"再调 Bot"按钮 |
| **Salesforce Platform** | 提供 Messaging Session、Omni-Channel、Case、Knowledge、报表对象；客户身份 / 队列 / 合规审计的权威记录系统 |
| **Knowledge & Content Ops** | 维护 Salesforce Knowledge 与 Help Centre；审批 Git 源准入与版本策略 |

### 1.3.2 Handling 状态机（PRD §3.2）

落地为 Salesforce 侧可查询状态（自定义字段或平台事件）：

```text
用户进入 → BOT_HANDLING（默认首触达）
  ├─ 用户点击「转人工」/ 关键词 → QUEUE_TO_HUMAN → HUMAN_HANDLING → CLOSED
  ├─ 自动升级条件满足           → QUEUE_TO_HUMAN → HUMAN_HANDLING → CLOSED
  └─ Bot 解决并结束（containment）→ CLOSED
```

- **Bot → 人工**：一期必做。
- **人工 → Bot**：一期不做（PRD 文档化预留 V1.x/V2 启用）。

### 1.3.3 数据模型骨架（PRD §9 + `salesforce-part-spec.md` 确认）

**PRD 逻辑模型（v5 已确认追加 Bot_Session__c / Bot_Event__c，升级为 session + event 显式状态模型）：**
- `Bot_Session__c`：MessagingSession__c lookup、TrafficVariant__c、HandlingState__c（BOT/HUMAN/CLOSED）、PrimaryIntent__c、ContainmentOutcome__c（RESOLVED/ESCALATED/ABANDONED）、Case__c lookup
- `Bot_Event__c`：Bot_Session__c lookup、EventType__c（INTENT / FAQ_SEARCH / ARTICLE_SHOWN / RESOLUTION_ASK / ESCALATION / CASE_CREATED）、Payload__c（脱敏 JSON）
- Case 字段：使用现有 Contact Reason / 描述 / 产品 + 可选 `Bot_Context__c`（Long Text 存结构化 JSON 供坐席 UI 解析，需长度与 PII 评审）

**已有自定义对象（v4 已确认）：**
- `Chat_Message_Log__c`：记录 Bot 与客户之间的会话消息；字段：`Name`(Text80) / `Session_Id__c`(Text255) / `Direction__c`(Picklist: Inbound/Outbound) / `Sender_Type__c`(Picklist: Agent/Customer/Bot) / `Message_Text__c`(LongText 32768) / `Response_Text__c`(LongText 32768) / `Processed__c`(Checkbox) / `Created_Date__c`(DateTime)

**Case 字段（v4 已确认，新版 pre-chat form 创建）：**
- Record Type：`Customer Service`
- 必填：`First Name` / `Last Name` / `Email` / `Topic Subject`（Picklist，11 值）/ `Description`
- 选填：`Ad ID Number`
- `Topic Subject` 可选值：Account Support / Ad Support / Delete My Account or Data / Delivery / Payments / Pro Contract / Account Manager Support / Ratings Reviews / Replies or Messaging / Report a Safety Issue / Technical Support

**Bot 输出 JSON 结构（v4 已确认）：**
```json
{
  "replyText": "Hello, how can I help you today?",
  "intent": "TRANSFER_TO_HUMAN",
  "shouldEndChat": false,
  "additionalData": { "customerName": "John Doe" }
}
```

---

## 1.4 Engineering Constraint Input

> 本章节约束源自 Gumtree 平台现有代码库（`/projects/gums`），详见 `07-engineering-constraints.md`。Bot 服务应与已有平台模式对齐，除非有明确决策偏离。

### 1.4.1 语言与框架

| 维度 | 约束 |
|------|------|
| **主语言** | **Java 17 + Spring Boot 3.x**（参考最现代服务 dpe: Spring Boot 3.2.5 + Java 17）|
| **备选** | Kotlin + Ktor 2.3（若团队偏好）|
| **构建工具** | Maven（与多数 Java 服务对齐）或 Gradle |
| **服务形态** | 容器化微服务，one service per repo；自带 Helm chart / Dockerfile / Jenkinsfile |

### 1.4.2 数据库与存储

| 组件 | 选型 | 说明 |
|------|------|------|
| **关系型 DB** | **PostgreSQL on GCP Cloud SQL**（+ cloud-sql-proxy sidecar）| 全平台标准；用于 session state、audit log、bot 配置 |
| **缓存** | **Redis** | 平台已有（bapi、capi、gdpr-orchestrator）|
| **Salesforce 侧对象** | `Bot_Session__c` / `Bot_Event__c` / `Case` | 报表与坐席可见数据落在 Salesforce |
| **不使用** | MongoDB、Cassandra（在 user-service / seller-service 用，但不适合 Bot 项目）|

### 1.4.3 向量检索（v4 已确认选型）

| 维度 | 确认内容 |
|------|---------|
| **选型** | **pgvector on Cloud SQL PostgreSQL**（复用已有 Cloud SQL 基础设施，运维成本低；FAQ 规模 ~218 篇文章，不需要大规模 ANN）|
| **分块策略** | 按语义段落或固定 token 窗口滑动重叠（256–512 tokens，重叠 10%–20%），避免切断表格/步骤列表 |
| **数据模型** | 双层表：article-level（`article_id` / `title` / `summary` / 版本 / 发布状态 / 来源 URL）+ chunk-level（`chunk_id` / `article_id` / `chunk_index` / `chunk_text` / `embedding vector(dim)` / `token_count`）|
| **Embedding 模型** | **Vertex AI `text-embedding-004`（GCP native，v6 已确认）**；768 维度；入库与在线 Query 必须使用同一模型与同一维度；通过自建微服务封装 Vertex AI API（离线批量 embed 后 bulk insert）|
| **索引** | **HNSW + cosine（已确认，见 `problem_retrieval_solution_plan_pgvector.md` §7）**：`m=16, ef_construction=64`；查询 `hnsw.ef_search=100`，开 `hnsw.iterative_scan=relaxed_order`；客服知识库场景漏召回代价 > 建索引慢一点，HNSW 为首选 |
| **在线检索** | ANN Top-20 → metadata filter（published / locale / market / audience）→ article-level 去重 → rerank Top-4~8 → 取 Top 4~6 组装 Prompt（Query + Title + Summary + chunks）→ LLM → 结构化回答；**faq_miss 两段式判定**：Retrieval Gate（候选质量 / metadata 命中）→ Answer Gate（`grounding_score < 3.5` 判弱命中/不可答）|
| **Embedding 模型** | 待选（Vertex AI `text-embedding` / Gemini embedding 系列）；入库与在线 Query 必须同一模型 + 同一维度 |
| **详细方案** | 见 `problem_retrieval_solution_plan_pgvector.md` |

> **注**：Salesforce Knowledge API 当前不可用于在线检索（`salesforce-part-spec.md`）。Bot 的知识检索完全走 pgvector → Help_Site_URL__c 作为 canonical URL 展示。

### 1.4.4 事件流

| 组件 | 技术 | 用途 |
|------|------|------|
| **消息队列** | Apache Kafka（Avro + Confluent Schema Registry）| 域事件；Bot 行为事件同步到分析管道 |
| **异步任务** | GCP Pub/Sub | 异步工作流（参考 gdpr-orchestrator 模式）|
| **凭证** | Kafka API key/secret + Schema Registry key/secret via GCP Secret Manager | |

### 1.4.5 部署平台

| 维度 | 约束 |
|------|------|
| **云厂商** | **GCP 100%**（无 AWS / Azure）|
| **主区域** | europe-west4；多区域 europe-west3 |
| **容器编排** | Google Kubernetes Engine（GKE）|
| **服务网格** | Istio（VirtualService 路由、内部 Ingress Gateway）|
| **镜像仓库** | GCP Artifact Registry（`europe-west4-docker.pkg.dev/gum-host-7491/docker-artifact-repo`）|
| **Serverless 备选** | Cloud Run（Jenkins pipeline 已支持）|
| **IaC** | Helm chart per service（`/helm-chart` 目录）|
| **自动扩缩** | Horizontal Pod Autoscaler（HPA）|
| **健康端点** | `/internal/health/liveness`、`/internal/health/readiness`、`/internal/health` |

### 1.4.6 CI/CD

| 工具 | 角色 |
|------|------|
| **Jenkins** | 主 CI/CD — 自定义 Gumtree pipeline library（`com.gumtree.jenkins.*`），部署到 GKE / VM / Cloud Run |
| **GitHub Actions** | 辅助 — PR checks、SonarCloud、Jira workflows |
| **SonarCloud** | 静态分析 / 代码质量 |
| **Pact** | Consumer-driven 契约测试（专用 `pact-verify` Jenkinsfiles）|

**Bot 项目额外要求**：regression eval gate 集成到 Jenkins pipeline — PR 触发 smoke regression（core routing / required escalation / grounding safety / handover schema），release candidate 触发 full regression（参见 eval_spec §15）。

### 1.4.7 可观测性

| 层 | 技术 |
|---|------|
| **日志** | Logback + SLF4J → Logstash appender → GCP Cloud Logging |
| **指标** | Micrometer → Prometheus（PodMonitoring on `/internal/metrics`）|
| **错误追踪** | Sentry / Raven |
| **链路追踪** | Istio sidecar / GCP Cloud Trace（待显式确认）|

**Bot 项目额外需求**：`Bot_Event__c` 事件日志 + 结构化 trace schema（trace_id / session_id / turn_id / prompt_version / model_version / projection_version / active_use_case / action_selected / tool_calls / source_ids / outcome — 见 tech_spec §17.2）。

### 1.4.8 密钥与配置

| 维度 | 技术 |
|------|------|
| **Secrets** | GCP Secret Manager — 所有服务通过 env var 挂载（如 `site-gumtree-*`）|
| **Config** | Spring Cloud Config 或 Helm values per environment（prod / staging / nonprod）|
| **RBAC** | Per-service GKE service account 绑定 GCP IAM（`iam.gke.io/gcp-service-account`）|

### 1.4.9 认证与内部 API 安全

| 维度 | 技术 |
|------|------|
| **服务间通信** | Istio mTLS（service mesh）+ 网络策略 |
| **API 网关** | Istio Internal Ingress Gateway |
| **Auth tokens** | AES-encrypted auth tokens（`site-gumtree-auth-token-aes-key` in Secret Manager）|
| **API 契约** | OpenAPI / Swagger codegen；contract YAML per service |
| **契约测试** | Pact（consumer-driven）|

### 1.4.10 代码仓库结构

- **Multi-repo**：每个服务独立仓库。
- **Bot 项目**：**已创建 greenfield standalone repo，repo 名称：`csagent`（v5 确认）**，遵循现有约定 — 独立 Helm chart、Jenkinsfile、Dockerfile、contract 目录。
- **标准目录**：`/server`、`/helm-chart`、`/contract`。

### 1.4.11 合规与安全（BRD §D）

- UK GDPR 合规；**PII 脱敏规则遵循 GDPR 隐私保护要求执行（v5 已确认）**：transcript / 日志中 email / ad_id / phone 等字段需脱敏；`get_customer_context` 设计 `safe_summary` 以最小化 PII 暴露。
- 不存储用户密码；不在 free text 中索取或存储敏感个人数据。
- 索取识别信息（email / case number 等）时必须解释原因。
- Chat transcript 按 Gumtree 数据保留策略存储。
- AI 驱动回答必须受控以避免 hallucination 与 policy 不一致。
- 高风险话题（fraud / safety / legal）必须有合适的引导与升级路径。

### 1.4.12 外部系统集成

| 集成目标 | 接口 | 说明 |
|----------|------|------|
| Salesforce Knowledge | Knowledge API | 权威元数据、发布状态；配合向量库语义召回；对应 tool_spec `search_knowledge` + `resolve_article` |
| Salesforce Case | REST / Composite / Apex REST | 创建 Case，字段与队列规则与现有一致；对应 tool_spec `create_case_controlled`（runtime_only，限 UC-H/J/K）|
| Salesforce Messaging Session | Embedded Service / Messaging API | Bot 回复写回同一聊天线程；对应 handover 时 `must_preserve_same_thread=true` |
| Salesforce Omni-Channel | Chat Transfer / Omni-Channel API | 转人工 + 传递自定义字段（intent / confidence / knowledgeArticleIds / caseId / summary）；对应 tool_spec `request_handover` |
| Account lookup API | 内部 REST API | 账号状态 + restriction_flags；对应 tool_spec runtime_only `lookup_customer_account`（被 `get_customer_context` 复合调用）|
| Listing/Ad lookup API | 内部 REST API | 广告状态 + moderation_status + visibility_reason；对应 tool_spec runtime_only `lookup_listing_or_ad`（被 `get_customer_context` 复合调用）|
| FAQ 向量库 | 内部 REST API | 语义检索 Top-K；对应 tool_spec `search_knowledge` candidate_api_mapping (`knowledge_vector_search`) |
| Analytics pipeline / Event sink | Kafka + Cloud Logging | Bot trace + session outcome；对应 tool_spec `record_outcome` |
| Email notification service | 内部 / Salesforce Email-to-Case | 异步邮件模板发送；对应 tool_spec human_only `send_followup_email_or_async_update`（Phase 1 Bot 不直接调用）|

### 1.4.13 Pre-chat Form & Case Creation Flow

> 新增于 2026-04-17。基于当前系统实际流程确认。

#### 系统流程

用户在 Enhanced Chat widget 中首先填写 **pre-chat form**，表单提交后系统创建 Salesforce Case，再根据路由规则分配：

```text
用户打开 Chat widget
  → 填写 Pre-chat Form（必填字段 + Description）
  → 提交 → 系统创建 Case
  → 路由判定：
     ├─ 工作时间（9am–8pm Mon–Sun UK）→ Chat queue → 坐席接入 → Live Chat 会话
     └─ 非工作时间 → Offline queue → 坐席离线处理（email 跟进）
```

**关键含义**：
- 坐席在 console 中看到 Case 时，**已经知道用户的表单信息**（Subject / Description / Email 等），这解释了为什么 98.7% 的 transcript 中坐席先说话
- 对 Bot 而言，**表单数据是 INIT 阶段的首个输入** — Bot 不需要等用户在对话中输入才开始工作
- 非工作时间的表单提交不产生 LiveChatTranscript（只创建 Case → 离线 queue），因此**不在 `bq-results-*.csv` 数据集中**

#### 老版表单 vs 新版表单

| 字段 | 老版（当前数据集） | 新版（V1 上线时） | 对 Bot 的影响 |
|------|-------------------|-----------------|-------------|
| First Name | 无 | ✅ 必填 | Bot 可用名字称呼用户 |
| Last Name | 无 | ✅ 必填 | 同上 |
| Email | 非必填（`Subject` 字段） | ✅ 必填 | **Bot 无需再问 email** → `get_customer_context` 可在 INIT 阶段立即触发 |
| Topic Subject | `Subject` 字段（下拉） | `Topic Subject`（下拉） | UC 预分类信号（低精度但有参考，见下方映射表） |
| Ad ID Number | 无 | 选填 | 如果用户填了 → `lookup_listing_or_ad` 可在 INIT 阶段立即触发 |
| Description | 部分有（95.2% 覆盖率） | ✅ 必填 | 用户意图首句，Bot 的主要分类输入 |

#### Topic Subject → UC 映射参考（从历史数据推导）

> 注：下方映射基于老版表单 `Subject` 字段的统计分布。新版表单的 `Topic Subject` 下拉选项可能有变化，需与产品确认。

| Topic Subject 值 | Top 1 UC（占比）| Top 2 UC | Top 3 UC | 评估 |
|------------------|---------------|---------|---------|------|
| Account Support | UC-H (37%) | UC-C (11%) | UC-D (9%) | 低精度 — 什么都有 |
| Ad Support | UC-H (62%) | UC-B (9%) | UC-C (5%) | 中精度 — UC-H 主导 |
| Technical Support | UC-K (38%) | UC-H (21%) | UC-C (11%) | 中精度 |
| Replies & Messaging | UC-C (68%) | UC-H (11%) | UC-J (9%) | 高精度 — UC-C 主导 |
| Report a Safety Issue | UC-J (71%) | UNMAPPED (7%) | UC-I (5%) | 高精度 |
| Delete my account / Data | UC-G (70%) | UC-H (22%) | — | 高精度 |
| Payments | UC-H (30%) | UC-I (22%) | UC-J (14%) | 低精度 |
| Ratings & Reviews | UC-C (31%) | UNMAPPED (24%) | UC-I (17%) | 低精度 |

**设计建议**：
- "Report a Safety Issue" 和 "Delete my account / Data" 可作为**强 UC 先验**（>70% 命中率），直接路由到 UC-J / UC-G
- 其他 Topic Subject 只作为弱信号，必须结合 Description 文本做意图分类
- 新版表单的 Topic Subject 下拉选项清单需要从产品方获取（可能与上表不同）

#### 对 Bot 架构的影响

1. **Context Projection INIT 阶段新增 form_context**：
   ```json
   {
     "form_context": {
       "first_name": "Hill",
       "email": "hill@gmail.com",
       "topic_subject": "Account Support",
       "ad_id": null,
       "description": "need check the status"
     },
     ...
   }
   ```

2. **Identifier 采集策略变化**：新版表单 email 必填 → `get_customer_context` 可在 INIT/DISCOVER 立即调用（不再需要 `should_prompt_for_identifiers_when_missing` for email）

3. **Clarification 需求下降**：老版表单时 25% 会话中坐席需要主动索取 email，新版表单后这部分 clarification 消失

### 1.4.14 Tool Layer Integration（与 `customer_service_tool_spec_v0_2.yaml` 对齐）

| 层 | 组件 | 说明 |
|---|------|------|
| **Agent-visible 工具（5 个）** | `search_knowledge` / `resolve_article` / `get_customer_context` / `request_handover` / `record_outcome` | Bot 唯一可直接发起的 tool call 集；v0.2 增加了 concrete API endpoint mapping + `form_context` 输入 |
| **Runtime-only 工具（v0.2 = 4 个）** | `create_case_controlled` / `lookup_customer_account` / `lookup_listing_or_ad` / **`get_moderation_review_context`**（v0.2 新增，由 `lookup_listing_or_ad` 链式调用，为 UC-A/UC-FP 提供广告审核/删除原因） | 不暴露给 LLM prompt；由 Bot runtime 根据 use case policy 触发 |
| **Human-only 工具（2 个）** | `moderation_enforcement_action` / `send_followup_email_or_async_update` | 不在 Bot 服务调用栈内；由坐席控制台 / back-office workflow 触发 |
| **Runtime capabilities（v0.2 新增，非 tool）** | `fixed_script_library`（固定话术模板库）/ `form_context_ingestion`（pre-chat form 解析 + 自动触发 context lookup）/ `tool_policy_enforcer`（UC scope enforcement）/ `progress_placeholder`（>1.5s 延迟时发送占位消息） | runtime 层内建能力，不暴露给 LLM |
| **Tool scope 治理** | 每次 tool call 前 runtime 检查 `active_use_case ∈ allowed_use_cases`，否则返回 `scope_blocked` 并记录事件 | `tool_policy_enforcer` 组件，enforcement = `hard_block_with_event_log` |
| **Runtime policy 映射** | v0.2 增加 `must_auto_trigger_on_form_context` / `must_verify_published_status` / `must_select_message_by_hours_and_reason` / `must_map_reason_to_public_policy` / `retry_on_failure` | 落地为 runtime 组件里的硬编码 invariant + eval grader 验证 |
| **Concrete API dependencies（v0.2 已映射）** | 所有 tool 的 `concrete_api_dependencies` / `concrete_api_call_chain` 已映射到 `platform_api_detailed_reference.md` 中的实际端点（bapi-server / gumshield-api / user-service / advert-service / livead-search 等）| 14 微服务 / 196 端点；Bot 只使用 read 端点 + case create |
| **Knowledge 文档仓库准入** | tool_spec `source_type` 枚举：`salesforce_knowledge` / `help_centre` / `approved_git_doc` | Git 内部文档须先过治理审批入列 approved collection |

---

## 1.5 Evaluation Input

> 本章对齐 `customer_service_agent_eval_spec.md`，落地到 Gumtree 项目。

### 1.5.1 数据集（eval_spec §7 — **v4：全部已构建**，见 `data/eval_datasets/DATASET_REPORT.md`）

> 数据来源：18,964 raw sessions → 6,835 cleaned → 601 sampled sessions / 11,288 turns + 367 条 human review queue。

| 数据集 | 内容 / 构建来源 | **v4 实际规模** | 状态 |
|--------|---------------|---------------|------|
| **Golden Use Case Dataset** | 基于全 12 UC 高频场景；每条含 task_id / turns / expected UC / outcome / tool sequence | **150 sessions / 2,794 turns** | ✅ 已构建；覆盖全 12 UC + UNMAPPED（UC-C 46% 最大自然池；small UC 有 min-floor 采样） |
| **Clarification Dataset** | 模糊表达、缺失关键信息、query 过宽、同义表达；标注澄清质量 | **60 sessions**（含 24 bad/over-clarified + 18 good/contained） | ✅ 已构建 |
| **Escalation Dataset** | 全 12 UC 覆盖；18 种 escalation reason；user_requested / policy-required / frustration / insufficient_grounding | **150 sessions / 3,424 turns** | ✅ 已构建 |
| **Drift / Control Dataset** | minor drift / soft shift / hard shift；99.7% 自然携带 frustration/escalation signal | **30 sessions / 782 turns** | ✅ 已构建 |
| **Handover Dataset** | summary completeness / UC correctness / escalation reason correctness / transcript linkage | **76 sessions / 1,754 turns** | ✅ 已构建 |
| **Intake / Tool Contract Dataset** | 验证 Bot 按 tool_spec v0.2 `allowed_use_cases` / `disallowed_use_cases` 正确调用工具；含期望 tool_call 序列 + negative cases | **50 sessions / 1,108 turns** | ✅ 已构建 |
| **Bad-Case Bank** | frustration / abandoned / escalation-signal 会话；guardrail regression | **95 sessions / 2,026 turns** | ✅ 已构建 |
| **Missed-Session Dataset** | 无 transcript 的沉没会话 metadata；Bot 首触达承接机会分析 | **2 sessions**（LiveChatTranscript 导出限制；需 Salesforce Case 导出补充） | ⚠️ 数据不足 |
| **Human Review Queue** | quality_score ≥ 60 的会话；**v6 决策：当前阶段跳过人工标注，直接以现有 eval datasets（`csagent/data/eval_datasets/`）为 ground truth**；后续上线后根据真实 Bot 数据再做人工回标补充 | **367 sessions** | ✅ 决策已定（跳过当前人工标注）|

**v4 关键发现**：
- CSAT 覆盖率仅 1.9% — 不可用于数据集质量验证；需在 Bot 上线后启用 CSAT 采集
- 仅 6 条 quality ≥ 80 的"高质量"transcript — 现实中"完美"会话极少，eval 应基于 quality ≥ 60 的 298 条
- **新版 pre-chat form 影响 eval**：老版数据集中 25% 会话坐席需问 email → 新版表单 email 必填后此类 clarification 消失；Clarification Dataset 需在 Bot 上线后 re-review
- **Human review 优先级**：P0 Golden(150) → P1 Intake/Tool(50) → P2 Handover(76) → P3 Bad-case(95) → P4 Drift(30)；每条约 10–15 分钟，建议 Product/Ops + QA + Salesforce 三角色分工

### 1.5.2 Release Threshold

继承 `phase0_normative_freeze.md` §0.3 launch gates。Gumtree 项目特定门槛若需调整，需经规范层 owner 确认。

### 1.5.3 Weekly Review Mechanism（eval_spec §14.3）

每周抽样审核：
- 已解决会话
- 已升级会话
- abandon 会话
- 低满意度 / 明显失败会话
- 高风险边界会话

审核输出必须进入 bad-case bank。

### 1.5.4 Ownership Model（eval_spec §17）

| 角色 | 职责 |
|------|------|
| **Product / Ops** | 定义 use case、设定 success criteria、提供高价值 bad cases、决定上线阈值 |
| **Engineering** | runtime / eval harness、graders、CI gates、tracing、metrics |
| **QA / SMEs** | 审核高风险 cases、校准 grader、参与周度抽样 |
| **共享原则** | 最接近业务问题的人，必须能参与定义 eval task |
