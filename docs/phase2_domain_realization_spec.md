# Phase 2 — Domain Realization Spec

> 把通用规范映射成 Gumtree Customer Service 领域的业务控制模型。覆盖 V1 Phase 1 的 7 个 use case（A–F'），定义每个 use case 的行为边界、风险等级、升级条件、知识范围和控制策略。
>
> **前置输入**: `phase0_normative_freeze.md`、`phase1_solution_input_pack.md`
> **下游输出**: `phase3_detailed_technical_design.md`（待产出）

---

## 2.1 目标

将通用规范层（single agent + bounded loop + grounded before generative + escalation first-class）映射为 Gumtree Customer Service 的具体业务控制模型。该层是后续 Detailed Technical Design 的业务契约依据。

---

## 2.2 Use Case Registry

### UC-A-01: Ad Status & Visibility（帖子/广告状态与可见性）

```yaml
use_case_id: UC-A-01
name: Ad Status & Visibility
description: >
  用户询问帖子/广告的状态：审核中、找不到、不显示、跨区展示、
  广告统计等。Bot 提供状态说明、类目/地区可见性解释、
  Help Centre 深链。
example_user_requests:
  - "Where is my ad?"
  - "My ad isn't showing up"
  - "How long does ad review take?"
  - "Why can't I find my listing on Gumtree?"
  - "My ad says processing - how long will it take?"
  - "I posted an ad but it's not visible in search"
knowledge_scope:
  - help_centre_ad_status
  - help_centre_ad_review_process
  - help_centre_category_visibility
  - help_centre_regional_display_rules
risk_level: low
allow_clarification: true
allow_bot_resolution: true
allowed_actions:
  - retrieve_knowledge
  - answer_grounded
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - user_requests_human
  - faq_miss_ge_2
  - clarification_budget_exhausted
  - issue_involves_incorrect_deletion  # 降判为 F' 或 H → 转人工
outcome_class: resolve
```

### UC-B-01: Posting & Editing Guidance（发帖与编辑指引）

```yaml
use_case_id: UC-B-01
name: Posting & Editing Guidance
description: >
  用户询问如何发帖、编辑广告、修改类目/地区/图片/价格等。
  Bot 提供步骤化 FAQ 和深链到对应功能页。
example_user_requests:
  - "How do I post an ad?"
  - "How to change the category of my listing?"
  - "I can't upload images to my ad"
  - "How do I edit my ad price?"
  - "How to change the location on my ad?"
knowledge_scope:
  - help_centre_post_ad
  - help_centre_edit_ad
  - help_centre_category_guide
  - help_centre_image_requirements
  - help_centre_location_settings
risk_level: low
allow_clarification: true
allow_bot_resolution: true
allowed_actions:
  - retrieve_knowledge
  - answer_grounded
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - user_requests_human
  - faq_miss_ge_2
  - clarification_budget_exhausted
  - issue_involves_payment_failure  # 映射到 F 或转人工
outcome_class: resolve
```

### UC-C-01: Messages & Replies（消息与回复）

```yaml
use_case_id: UC-C-01
name: Messages & Replies
description: >
  用户询问如何收发消息、回复广告、恢复已删消息、未收到回复等。
  Bot 提供操作路径 FAQ 和安全提示；对"恢复已删消息"明确不承诺。
example_user_requests:
  - "How do I reply to an ad?"
  - "I'm not getting any replies"
  - "Can I recover deleted messages?"
  - "How to check my inbox?"
  - "I can't see the reply button"
knowledge_scope:
  - help_centre_messaging
  - help_centre_inbox
  - help_centre_notification_settings
  - help_centre_safety_tips
risk_level: low
allow_clarification: true
allow_bot_resolution: true
allowed_actions:
  - retrieve_knowledge
  - answer_grounded
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - user_requests_human
  - faq_miss_ge_2
  - clarification_budget_exhausted
  - issue_involves_fraud_or_scam  # 降判为 J 类 → 转人工
outcome_class: resolve
```

### UC-D-01: Account & Login (Non-Sensitive)（账户与登录）

```yaml
use_case_id: UC-D-01
name: Account & Login (Non-Sensitive)
description: >
  用户询问密码重置、登录问题、邮箱登录说明、Alerts/Favourites/
  Unsubscribe 等。Bot 提供自助链接和指引；拒绝存储密码、
  不接"代操作"请求。
example_user_requests:
  - "I forgot my password"
  - "How do I reset my password?"
  - "I can't log in to my account"
  - "How do I change my email address?"
  - "How to unsubscribe from alerts?"
  - "I'm not receiving the reset email"
knowledge_scope:
  - help_centre_password_reset
  - help_centre_login
  - help_centre_email_settings
  - help_centre_alerts_favourites
  - help_centre_unsubscribe
risk_level: low
allow_clarification: true
allow_bot_resolution: true
allowed_actions:
  - retrieve_knowledge
  - answer_grounded
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - user_requests_human
  - faq_miss_ge_2
  - clarification_budget_exhausted
  - issue_involves_account_ban_or_blacklist  # 降判为 F' 或二期 H → 转人工
  - user_provides_password  # 合规拒绝 + 升级
outcome_class: resolve
```

### UC-E-01: General Product & Search（通用产品与搜索）

```yaml
use_case_id: UC-E-01
name: General Product & Search
description: >
  用户询问搜索功能使用、Gumtree 产品功能、功能反馈/投诉等。
  Bot 提供功能说明 FAQ 和反馈表单入口引导。
example_user_requests:
  - "How does search work on Gumtree?"
  - "Why are my search results wrong?"
  - "I want to give feedback about the website"
  - "What features does Gumtree offer?"
  - "The website has changed and I don't like it"
knowledge_scope:
  - help_centre_search
  - help_centre_product_features
  - help_centre_feedback_form
risk_level: low
allow_clarification: true
allow_bot_resolution: true
allowed_actions:
  - retrieve_knowledge
  - answer_grounded
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - user_requests_human
  - faq_miss_ge_2
  - clarification_budget_exhausted
  - issue_involves_safety_or_legal  # 引导举报流程或转人工
outcome_class: resolve
```

### UC-F-01: Payment Inquiry (Non-Dispute)（支付咨询-非争议）

```yaml
use_case_id: UC-F-01
name: Payment Inquiry (Non-Dispute)
description: >
  用户询问 Gumtree Payments 规则、到账时间、一般支付流程等。
  Bot 提供政策与流程类 FAQ 和免责声明（不构成财务/法律建议）；
  涉及退款/INAD/扣款错误等争议的立即转人工。
example_user_requests:
  - "How do Gumtree Payments work?"
  - "When will I receive my payment?"
  - "What are the payment fees?"
  - "How do I set up payments for my listing?"
knowledge_scope:
  - help_centre_payments_policy
  - help_centre_payments_process
  - help_centre_payments_fees
  - help_centre_payments_faq
risk_level: medium
allow_clarification: true
allow_bot_resolution: true
allowed_actions:
  - retrieve_knowledge
  - answer_grounded
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - user_requests_human
  - faq_miss_ge_2
  - clarification_budget_exhausted
  - issue_involves_refund_or_dispute  # I 类 → 立即转人工
  - issue_involves_chargeback
  - issue_involves_inad
outcome_class: resolve
```

### UC-FP-01: Correct Deletion / Compliance Takedown Explanation（正确删除/合规下架话术）

```yaml
use_case_id: UC-FP-01
name: Correct Deletion / Compliance Takedown Explanation
description: >
  用户询问帖子被删原因，命中"Correct Deletion / 合规下架"子类
  （TMX/Filter Correct Deletion、Deleted/Blacklisted Correctly、
  Fraud Blacklisted Correctly）。Bot 提供标准解释模板 + 政策链接 +
  合规重发/修改指引。不接"申诉成功/恢复"承诺；要求复核/误删申诉
  时给固定话术 + 转人工。
example_user_requests:
  - "Why was my ad deleted?"
  - "My ad was removed for no reason"
  - "I got blacklisted, why?"
  - "TMX filter deleted my listing"
  - "How do I repost after deletion?"
  - "My ad was taken down but I didn't break any rules"
knowledge_scope:
  - help_centre_community_standards
  - help_centre_ad_policies
  - help_centre_prohibited_content
  - help_centre_reposting_guide
  - help_centre_tmx_filter_explanation
risk_level: medium
allow_clarification: true
allow_bot_resolution: true
allowed_actions:
  - retrieve_knowledge
  - answer_grounded
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - user_requests_human
  - faq_miss_ge_2
  - clarification_budget_exhausted
  - user_requests_appeal_or_review  # 申诉/复核 → 固定话术 + 转人工
  - user_expresses_strong_emotion  # 情绪激动/威胁 → 安抚 + 转人工
  - issue_classified_as_incorrect_deletion  # 分类为"误删" → 二期 H 或转人工
outcome_class: resolve
```

---

## 2.3 Risk Matrix

### Low Risk（Bot 可自主解决）

| Use Case | 说明 |
|----------|------|
| UC-A-01 Ad Status & Visibility | FAQ 查询，不涉及数据修改或敏感操作 |
| UC-B-01 Posting & Editing Guidance | 操作指引，不执行写操作 |
| UC-C-01 Messages & Replies | 使用说明类，不涉及数据恢复承诺 |
| UC-D-01 Account & Login | 自助链接指引，不代用户操作账户 |
| UC-E-01 General Product & Search | 功能说明与反馈入口 |

### Medium Risk（Bot 可回答但需受控话术，部分子场景须转人工）

| Use Case | 说明 |
|----------|------|
| UC-F-01 Payment Inquiry | 政策说明可答，争议类子场景必须转人工 |
| UC-FP-01 Correct Deletion Explanation | 标准解释可答，申诉/误删/情绪激动必须转人工 |

### High Risk（V1 不做 Bot 自主解决，直接转人工或延期到二期）

| 场景 | 说明 | 处理方式 |
|------|------|---------|
| GDPR / Data Deletion (G) | 身份核验、合规执行，约 3.1 万条 | Phase 2 |
| 误删申诉 (H) | 需审核/工具集成、多轮补全 | Phase 2 |
| 支付退款与争议 (I) | 规则判定、订单/支付系统联动 | Phase 2 |
| Trust & Safety (J) | 欺诈/举报，需人工裁决 | Phase 2（结构化采集可前置） |
| Technical Issue (K) | 排障脚本、建单 | Phase 2 |

### Forbidden Automation（任何版本均不自动化）

| 行为 | 原因 |
|------|------|
| 自动执行退款 / 资金操作 | 高欺诈风险 + 合规要求 |
| 自动判定欺诈结论 | 需人工审查 + 法律合规 |
| 自动解封被封账户 | 安全策略要求人工审核 |
| 自动恢复被正确删除的帖子 | 违反平台政策 |
| 承诺法律/财务建议 | 超出 Bot 权限边界 |
| 存储或处理用户密码 | 安全合规禁止 |

---

## 2.4 Escalation Matrix

| 触发条件 | 触发类型 | 适用 Use Case | 处理行为 |
|----------|---------|--------------|---------|
| **user_requests_human** | 用户主动请求 | 全部 | 立即进入 ESCALATE，记录 `escalation_reason: user_requested`，构建 handover payload |
| **faq_miss_ge_2** | 检索失败阈值 | 全部 | 自动升级，`escalation_reason: faq_miss_threshold_exceeded` |
| **clarification_budget_exhausted** | 澄清预算耗尽（2 轮） | 全部 | 自动升级，`escalation_reason: clarification_budget_exhausted` |
| **issue_involves_refund_or_dispute** | 争议类子场景 | UC-F-01 | 立即升级（不尝试解决），`escalation_reason: policy_required_high_risk` |
| **user_requests_appeal_or_review** | 申诉/复核请求 | UC-FP-01 | 给固定话术后升级，`escalation_reason: appeal_requires_human` |
| **user_expresses_strong_emotion** | 情绪激动/威胁 | 全部（特别是 UC-FP-01） | 安抚话术 + 升级，`escalation_reason: user_distress` |
| **issue_classified_as_incorrect_deletion** | 误删类别 | UC-FP-01, UC-A-01 | 升级到人工或二期 H 流程，`escalation_reason: incorrect_deletion_appeal` |
| **issue_involves_fraud_or_scam** | 欺诈/诈骗 | UC-C-01, UC-E-01 | 引导举报入口或转人工，`escalation_reason: trust_safety_required` |
| **issue_involves_account_ban** | 账号被封/黑名单 | UC-D-01 | 降判到 F' 或转人工，`escalation_reason: account_compliance` |
| **out_of_scope_intent** | 非 V1 覆盖意图 | 全部 | 固定引导话术 + 转人工，`escalation_reason: out_of_scope` |
| **service_degraded** | 系统降级 | 全部 | 安全升级，`escalation_reason: service_degraded` |
| **max_bot_turns_exceeded** | Bot 轮数超限 | 全部 | 强制升级，`escalation_reason: turn_budget_exhausted` |

---

## 2.5 Knowledge Scope Map

### Use Case → Knowledge Sources

| Use Case | 允许的 Knowledge 范围 | 期望 Evidence 类型 |
|----------|---------------------|------------------|
| UC-A-01 | Help Centre: ad review, ad status, category visibility, regional display | 文章链接 + 摘要段落 |
| UC-B-01 | Help Centre: posting guide, editing guide, category/location/image rules | 步骤化 FAQ + 深链 |
| UC-C-01 | Help Centre: messaging, inbox, notifications, safety tips | 操作路径 + 安全提示 |
| UC-D-01 | Help Centre: password reset, login, email settings, alerts, unsubscribe | 自助链接 + 路径说明 |
| UC-E-01 | Help Centre: search guide, product features, feedback channel | 功能说明 + 表单链接 |
| UC-F-01 | Help Centre: payments policy, fees, payment process | 政策说明 + 免责声明 |
| UC-FP-01 | Help Centre: community standards, ad policies, prohibited content, reposting | 政策摘要 + 标准模板 + 重发指引 |

### Use Case → Disallowed Knowledge

| Use Case | 不可作为事实源的内容 |
|----------|-------------------|
| 全部 | 用户自由输入内容、未审批 Git 文档、外部网站内容 |
| UC-F-01 | 非 Gumtree 官方支付政策 |
| UC-FP-01 | 内部审核工具截图/日志、未公开的 TMX 规则细节 |

---

## 2.6 Control Policy by Use Case

### 通用控制策略（所有 Use Case 共享）

```yaml
discover_policy:
  - 从菜单选择或自由文本推断 active_use_case
  - 低置信度时展示澄清按钮（≤ 2 轮）
  - 检测到 out-of-scope 意图时记录 candidate_use_cases 并引导
  - 检测到高风险/跨类意图时降判或升级

clarification_policy:
  - max_clarification_rounds: 2
  - 澄清问题必须推动收敛（不重复问同一问题）
  - 澄清预算耗尽后自动升级

resolution_policy:
  - 优先向量检索 + Knowledge 校验
  - 回复格式：标题 + 1-3 句摘要 + 可点击链接
  - 禁止纯"请自行搜索"式回复
  - 禁止无依据自由生成
  - 未解决时：更深检索 1 次（换 query 模板）或升级

confirmation_policy:
  - 每次回答后必须进行解决确认（"Did this solve your problem?"）
  - 已解决 → 感谢/结束（Containment）
  - 未解决 → 再检索 1 次或转人工
  - 用户表达申诉诉求时跳过强迫确认，直接给下一步

escalation_policy:
  - 见 §2.4 Escalation Matrix
  - 升级话术包含等待预期/下一步说明
  - 升级时构建完整 handover payload

close_policy:
  - 明确标记 outcome（resolved / escalated / abandoned）
  - 记录 trace 完整字段
  - 可选采集 CSAT
```

### UC-A-01 特定策略

```yaml
discover_policy_override:
  - 入口呈现快捷按钮：处理中、找不到、已下架/删除、统计/展示疑问、其他
  - 选"其他"→ 自由文本 + G1 检索

resolution_policy_override:
  - 按子意图映射固定话术片段 + RAG 召回
  - 若同时提及违规/误删申诉 → 降判为 UC-FP-01 或 H 类
  - Phase 1.x 可选：调用 listing 查询 API（已登录 + 已批准时）
```

### UC-B-01 特定策略

```yaml
resolution_policy_override:
  - 回复为编号步骤（3-5 步）+ 深链（Post Ad、Edit、类目/地区选择页）
  - 涉及付款失败/退款/争议 → 映射到 UC-F-01 或转人工

clarification_policy_override:
  - 可问"移动端还是网页？"以精确匹配操作路径
```

### UC-C-01 特定策略

```yaml
resolution_policy_override:
  - 前置安全提示（勿站外付款、防诈骗）
  - "恢复已删消息"→ 明确话术"无法保证恢复" + Help Centre 链接
  - 声称被骗/举报用户 → J 类，结构化举报入口或转人工

escalation_policy_override:
  - 用户坚持要求恢复已删消息 → G4 升级
```

### UC-D-01 特定策略

```yaml
resolution_policy_override:
  - 索要"代操作"或提供密码 → 拒绝存储密码 + 仅给自助链接 + 合规提示
  - 涉及账号被封/黑名单/合规状态 → 降判到 UC-FP-01 或二期 H

escalation_policy_override:
  - 用户提供密码/异常行为 → 立即升级
```

### UC-E-01 特定策略

```yaml
resolution_policy_override:
  - 反馈 → 引导官方反馈表单/渠道
  - 投诉指向具体违法/安全 → 引导举报流程或转人工
  - 与广告状态纠缠 → 路由回 UC-A-01（合并意图，避免重复建单）
```

### UC-F-01 特定策略

```yaml
resolution_policy_override:
  - 附加免责声明："不构成财务/法律建议"
  - 退款/INAD/未收到货/扣款错误 → 立即转人工，不判责

escalation_policy_override:
  - 争议类子场景不尝试解决，直接升级
```

### UC-FP-01 特定策略

```yaml
resolution_policy_override:
  - 使用标准解释模板（政策依据摘要）+ Community Standards 链接 + 合规重发指引
  - 不接"申诉成功/恢复"承诺
  - 要求复核/误删申诉 → 固定话术 + 转人工或二期 H 流程

confirmation_policy_override:
  - 若用户已表达申诉诉求，跳过"已解决？"确认
  - 直接给下一步（人工/表单）

escalation_policy_override:
  - 情绪激动/威胁 → 安抚 + 立即升级，记录 escalation_reason
  - 与合规/产品确认最终话术模板
```

---

## 2.7 Outcome Definition

| Outcome | 定义 | 统计条件 | 对应 ContainmentOutcome__c |
|---------|------|---------|--------------------------|
| **resolved** | 用户在 Bot 轮次内确认问题已解决，或获取正确下一步后正常结束会话 | 用户确认"已解决" 或 会话正常关闭且无转人工 | `RESOLVED` |
| **clarified_but_unresolved** | Bot 正确理解了问题并提供了 grounded 回答，但用户表示未解决、需要进一步帮助 | 用户回答"未解决"后选择不转人工、自行离开 | `ABANDONED`（子类标记） |
| **escalated** | 会话经由任何触发条件转交人工坐席 | escalation_requested 事件记录 | `ESCALATED` |
| **abandoned** | 用户在 Bot 流程中未完成交互即离开（无确认、无转人工） | 会话超时或用户关闭且无 outcome 记录 | `ABANDONED` |
| **wrong_containment** | Bot 标记为 resolved 但用户实际问题未被解决（通过 repeat-contact proxy 或 weekly review 发现） | repeat contact within 24h on same intent 或 human review 标记 | 回溯修正为 `WRONG_CONTAINMENT` |

### Outcome 质量约束

- **wrong containment ≤ 2%**：通过 repeat-contact proxy 和 weekly human review 持续监控。
- **每周抽样审核**：从 resolved / escalated / abandoned 各抽样，确认 outcome 标记准确性。
- **abandoned 归因**：区分"用户在哪个步骤离开"（intent selection / FAQ display / resolution ask），纳入 drop-off funnel 分析。

---

## 2.8 Use Case 间路由规则（Cross-Use-Case Routing）

V1 允许以下 use case 间的降判/路由：

| 源 Use Case | 目标 | 触发条件 | 行为 |
|-------------|------|---------|------|
| UC-A-01 | UC-FP-01 | 用户提及违规/删除原因 | 切换 active_use_case，保留原问题标记 |
| UC-B-01 | UC-F-01 | 用户提及支付失败 | 切换 active_use_case |
| UC-C-01 | Trust & Safety (J) | 用户声称被骗 | 结构化举报入口或转人工 |
| UC-D-01 | UC-FP-01 | 用户提及账号被封 | 切换到合规解释话术 |
| UC-E-01 | UC-A-01 | 用户从产品问题转到广告状态 | 合并意图，路由回 A |
| UC-FP-01 | H（二期）或转人工 | 用户要求申诉 | 固定话术 + 转人工 |

### Drift 处理规则

- **minor drift**：用户补充信息但不切换问题 → 保持当前 use case，追加上下文。
- **soft shift**：用户引入新问题 → 允许切 active_use_case，但在 state 中保留未解决的 primary issue 标记。
- **hard shift**：新问题属于高风险或优先级更高 → 直接升级或切换策略。
