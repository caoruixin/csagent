# Phase 2 — Domain Realization Spec

> 把通用规范映射成 Gumtree Customer Service 领域的业务控制模型。
>
> **前置输入**: `phase0_normative_freeze.md`、`phase1_solution_input_pack.md`、`customer_service_agent_tech_spec.md`、`PRD_biz_part.md`、`customer_service_tool_spec_v0_1.yaml`、`customer_service_conversation_samples_organized.xlsx`、`inferred_tool_candidates_from_human_conversations.xlsx`
> **下游输出**: `phase3_detailed_technical_design.md`（待用户补充必要信息后启动）

---

## 2.1 目标

将通用规范层（single agent + bounded loop + grounded before generative + escalation first-class + tool_surface = small_and_strong）映射为 Gumtree Customer Service 的具体业务控制模型。该层是后续 Detailed Technical Design 的业务契约依据。

覆盖范围：
- **12 个 V1 use case**（对齐 `customer_service_tool_spec_v0_1.yaml` §use_cases）：
  - **FAQ / grounded-answer 类（Bot 可 resolve）**：UC-A-01、UC-B-01、UC-C-01、UC-D-01、UC-E-01、UC-F-01、UC-FP-01
  - **Intake + Handover 类（Bot 不 resolve，只做结构化采集与移交）**：UC-G-01、UC-H-01、UC-I-01、UC-J-01、UC-K-01
- **风险分级**：low / medium / high / critical / forbidden
- **升级矩阵**：12 类触发条件
- **知识范围映射**
- **控制策略**（共享 + per-UC override）
- **结果定义与 Salesforce ContainmentOutcome__c 映射**
- **跨 UC 路由 + drift 处理**
- **Guardrails 落地**（含 BRD 品牌口径）
- **per-UC 工具可用性矩阵（§2.10）**：对齐 tool_spec v0.1 的 `allowed_use_cases` / `disallowed_use_cases` / `runtime_policy`

> **命名约定**：Phase 2 use_case_id 使用 `UC-X-01` 后缀（预留同类多变体），tool_spec 使用 `UC-X`（粗粒度）。映射关系：`UC-X-01 ∈ UC-X`。Phase 2 的路由策略与 tool allowed_use_cases 检查以 `UC-X` 作为最粗一级；同类下若将来出现 `UC-X-02` 等细分，需单独声明 tool overrides。

---

## 2.2 Use Case Registry

> Schema 参见 `customer_service_agent_tech_spec.md` §9.2 / `phase0_normative_freeze.md` §0.3。

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
case_volume: 5506   # 占全库 4.6%
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
  - issue_involves_incorrect_deletion  # 降判为 UC-FP-01（说明类）或 UC-H-01（申诉类）
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
case_volume: 3160   # 占全库 2.6%
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
  - issue_involves_payment_failure  # 映射到 UC-F-01 或转人工
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
case_volume: 2627   # 占全库 2.2%
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
case_volume: 4906   # 占全库 4.1%
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
  - issue_involves_account_ban_or_blacklist  # 降判到 UC-FP-01（说明）或 UC-H-01（申诉）
  - user_provides_password  # 合规拒绝 + 升级
  - user_requests_account_deletion  # 降判到 UC-G-01
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
case_volume: 3811   # 占全库 3.2%
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
case_volume: 488   # 占全库 0.4%
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
case_volume: 20875   # 占全库 17.3%（最高占比 use case）
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
  - issue_classified_as_incorrect_deletion  # "误删" → UC-H-01
outcome_class: resolve
```

### UC-G-01: GDPR / Data Deletion Intake（V1 intake-only）

```yaml
use_case_id: UC-G-01
name: GDPR / Data Deletion Intake
description: >
  用户请求账号删除、数据删除（Right to Erasure）、SAR、数据访问请求。
  V1 Bot 不执行 GDPR 合规动作（身份核验、数据擦除、SAR 响应由
  专用流程处理），仅做意图识别 + 固定流程解释 + 结构化 intake
  （email / 请求类型 / 是否希望保留账号）+ handover。
example_user_requests:
  - "Please delete my account"
  - "I want you to erase all my data"
  - "How do I submit a GDPR data deletion request?"
  - "Can you completely erase my account please"
  - "I want to close my Gumtree account"
case_volume: 29638   # 占全库 24.6%；Chat 样本中 4 条 transcript（样本稀少但真实意图高）
knowledge_scope:
  - help_centre_gdpr_overview
  - help_centre_account_deletion_process
  - help_centre_data_rights
risk_level: high
allow_clarification: true       # 仅用于收集 intake 必要字段
allow_bot_resolution: false     # V1 Bot 不做 GDPR 操作
allowed_actions:
  - ask_user                    # 收集 email / 请求类型
  - escalate_human
  - finish
  # 注：tool_spec 下 search_knowledge / resolve_article 不允许用于 UC-G；
  # GDPR 流程解释仅使用固定话术库（prompt-template 级，不经由 retrieve_knowledge 动作）
escalation_conditions:
  - intake_complete              # required_fields 齐 → 立即 handover
  - user_requests_human
  - identity_verification_required  # 任何需要身份核验的细节 → 人工
outcome_class: escalate
tool_spec_mapping: UC-G          # 对应 customer_service_tool_spec_v0_1.yaml
```

### UC-H-01: Incorrect Deletion Appeal / Ad Removal Appeal（V1 intake + case）

```yaml
use_case_id: UC-H-01
name: Incorrect Deletion Appeal / Ad Removal Appeal
description: >
  用户声称广告被错误删除、误判违规、账户被错误限制，申诉复核。
  Chat 样本中占比最高（116/262 ≈ 44%）。V1 Bot 不做裁决或恢复，
  仅做 intake（ad_id / 删除原因声称 / 期望结果）→
  `create_case_controlled`（UC-H 允许）→ `request_handover`，
  并给用户明确等待预期（TTFR ≈ 24h via email）。
example_user_requests:
  - "Why has my ad keep getting removed"
  - "My ad was removed but I didn't break any rules"
  - "Hi my ad was removed"
  - "I appeal my account restriction"
  - "You removed my ad incorrectly"
case_volume: ~4000   # Chat 样本中 116 条（最高）
knowledge_scope:
  - help_centre_community_standards  # 仅用于解释"删除后流程"
  - help_centre_appeal_process
risk_level: high
allow_clarification: true       # 最多 2 轮，收集 ad_id + email
allow_bot_resolution: false
allowed_actions:
  - ask_user
  - escalate_human
  - finish
  # 注：search_knowledge / resolve_article 不在 UC-H allowed_use_cases（tool_spec）
  # Bot 回答用固定话术模板（不 grounded-generate）
escalation_conditions:
  - intake_complete_with_ad_id_and_email  # 标准 intake 齐 → handover
  - user_requests_human
  - clarification_budget_exhausted
  - user_expresses_strong_emotion
outcome_class: escalate
runtime_required:
  - get_customer_context (限 UC-A/UC-FP/UC-K，对 UC-H 不可，因此仅靠 ask_user 收集)
  - create_case_controlled (UC-H 允许)
  - request_handover (UC-H 允许)
tool_spec_mapping: UC-H
```

### UC-I-01: Refund / Payment Dispute Intake（V1 intake-only）

```yaml
use_case_id: UC-I-01
name: Refund / Payment Dispute Intake
description: >
  用户关于 Gumtree Payments 的退款、未收到货 / INAD、扣款错误、
  chargeback 等争议。V1 Bot 不做规则判定、不做金额裁决，
  仅做 intake（email / 订单或广告 id / 问题描述）+ 固定话术
  （"此类问题需人工处理"）+ handover。Chat 样本中占 50 条（19%）。
example_user_requests:
  - "I want a refund for a payment"
  - "Why does Gumtree continue to ignore my emails?"
  - "I paid but the ad was not posted"
  - "Triplicated payment not refunded"
  - "INAD item — seller refuses to refund"
case_volume: ~3000   # Chat 样本中 50 条（UC-I × resolved_in_chat 仅 9，大多需 human）
knowledge_scope:
  - help_centre_payments_faq           # 仅作为政策说明
  - help_centre_refund_process
risk_level: high
allow_clarification: true
allow_bot_resolution: false
allowed_actions:
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - intake_complete_with_email_and_payment_reference
  - user_requests_human
  - any_dispute_or_chargeback_mention       # 所有争议关键词 → 立即升级
  - user_expresses_strong_emotion
outcome_class: escalate
tool_spec_mapping: UC-I
# 注：tool_spec 下 search_knowledge / resolve_article 不允许用于 UC-I；
# Bot 解答只能用固定话术（例如："payment disputes are handled by our team — I'll pass this on"）
```

### UC-J-01: Trust & Safety / Fraud Report Intake（V1 structured intake + case）

```yaml
use_case_id: UC-J-01
name: Trust & Safety / Fraud Report Intake
description: >
  用户举报他人诈骗、可疑广告、被骗、账户被冒用、骚扰等。
  Chat 样本中 25 条（9.5%，risk_tier=critical）。V1 Bot 做
  结构化 intake（举报对象 / 广告 id / URL / 描述 / 是否已报警）→
  `create_case_controlled`（UC-J 允许）→ `request_handover`，
  不做判定、不做封禁、不给任何"已处理"暗示。
example_user_requests:
  - "I have been a victim of fraud on the gumtree platform"
  - "Huge amount of spamming and badly violation ads"
  - "Please ban this seller"
  - "I want to report a scam"
  - "The person posted my items without my permission"
case_volume: ~6238   # 占全库 5.2%；Chat 样本 25 条
knowledge_scope:
  - help_centre_fraud_reporting
  - help_centre_safety_tips
  - help_centre_community_standards
risk_level: critical
allow_clarification: true
allow_bot_resolution: false
allowed_actions:
  - ask_user
  - escalate_human
  - finish
escalation_conditions:
  - intake_complete_with_target_and_description
  - user_requests_human
  - any_imminent_harm_signal              # 人身威胁等 → 立即升级并高优 queue
outcome_class: escalate
runtime_required:
  - create_case_controlled (UC-J 允许)
  - request_handover (UC-J 允许)
tool_spec_mapping: UC-J
# 注：tool_spec 明确 moderation_enforcement_action visibility=human_only；
# Bot 绝不承诺"已删除该广告" / "已封禁该用户"，只能承诺"已转交安全团队审核"
```

### UC-K-01: Technical Issue Intake（V1 intake + case）

```yaml
use_case_id: UC-K-01
name: Technical Issue Intake
description: >
  用户报告功能异常、无法登录、搜索结果错误、支付页 bug、
  应用崩溃等技术问题。V1 Bot 做轻量排障澄清（2 轮内）+
  intake（platform / browser / repro / email）→ 若无法通过
  FAQ 解决 → `create_case_controlled` + `request_handover`。
  Chat 样本 UC-K 未单独出现（部分归入 UNMAPPED 或 UC-I），
  但 Case Reason 数据中 Technical Issue 占 5.8%。
example_user_requests:
  - "The app keeps crashing"
  - "I can't see search results"
  - "Invalid token error when deleting account"
  - "Messages don't open"
  - "Page not loading"
case_volume: ~7041   # 占全库 5.8%
knowledge_scope:
  - help_centre_troubleshooting
  - help_centre_app_issues
  - help_centre_browser_support
risk_level: medium
allow_clarification: true
allow_bot_resolution: partial     # 能通过 get_customer_context 回拉状态解释的走 bot；需后端投诉的走 intake + case
allowed_actions:
  - ask_user
  - answer_grounded                 # 基于 get_customer_context 的 safe_summary 回答（非 Help Centre 知识）
  - escalate_human
  - finish
  # 注：search_knowledge / resolve_article 未在 UC-K allowed_use_cases；
  # answer_grounded 的 grounding 来源仅限 get_customer_context 的 safe_summary + 固定话术库
escalation_conditions:
  - clarification_budget_exhausted
  - user_requests_human
  - issue_requires_backend_investigation
outcome_class: escalate             # 大多数 K 类会 escalate；少数可被 bot 解决
runtime_required:
  - get_customer_context (UC-K 允许；由 agent 触发)
  - create_case_controlled (UC-K 允许；由 runtime 触发)
  - request_handover (UC-K 允许)
tool_spec_mapping: UC-K
```

---

## 2.3 Risk Matrix

### Low Risk（Bot 可自主 FAQ resolve）

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
| UC-K-01 Technical Issue | 部分可通过 `get_customer_context` 状态回拉 + 固定话术解决；重 bug 类走 `create_case_controlled` + handover |

### High Risk（V1 Bot 不做自主 resolve，只做 structured intake + handover）

| Use Case | V1 Bot 职责 | 必要工具 |
|----------|------------|---------|
| UC-G-01 GDPR / Data Deletion | 意图识别 + 流程解释 + intake（email / 请求类型）+ handover；所有核验与执行由人工或 back-office | `request_handover` |
| UC-H-01 Incorrect Deletion Appeal | 固定安抚话术 + intake（ad_id / email / 申诉原因）→ `create_case_controlled` + handover；**不 grounded generate**（search_knowledge 对 UC-H 禁用）| `create_case_controlled`, `request_handover` |
| UC-I-01 Refund / Payment Dispute | 固定话术 + intake（email / 订单或广告 id / 描述）+ handover；Bot 不做规则判定、不给金额结论 | `request_handover` |
| UC-J-01 Trust & Safety / Fraud Report | 结构化举报采集（target / URL / 描述 / 已否报警）→ `create_case_controlled` + handover；**绝不使用 `moderation_enforcement_action`**（human_only）| `create_case_controlled`, `request_handover` |

### Critical Risk（即使是人工也需要额外审计；Bot 绝不直接或间接触发）

| 行为 | 说明 | 来源 |
|------|------|-----|
| `moderation_enforcement_action`（删帖 / 限号 / 账号限制）| visibility=human_only，只能由坐席 / back-office 授权后触发 | tool_spec v0.1 |
| Trust & Safety 情报结论（是否为诈骗 / 是否封禁）| 需人工审查 + 法律合规 | BRD §1.3 |
| 退款 / 资金操作 | 需人工审查 + 合规 | BRD §1.3 / PRD §4.3 B-OUT-03 |

### Forbidden Automation（任何版本均不自动化）

| 行为 | 原因 | 来源 |
|------|------|-----|
| 自动执行退款 / 资金操作 | 高欺诈风险 + 合规要求 | BRD §1.3、PRD §4.3 B-OUT-03 |
| 自动判定欺诈结论 | 需人工审查 + 法律合规 | BRD §1.3 |
| 自动解封被封账户 | 安全策略要求人工审核 | BRD §4.3 risk constraint |
| 自动恢复被正确删除的帖子 | 违反平台政策 | PRD §1.6.2.1 F'1 |
| 自动执行 GDPR 数据擦除 | 合规身份核验要求 | UK GDPR |
| Bot 直接调用 `moderation_enforcement_action` 或 `send_followup_email_or_async_update` | visibility=human_only | tool_spec v0.1 |
| 承诺法律/财务建议 | 超出 Bot 权限边界 | BRD §6.3 phrases to avoid |
| 存储或处理用户密码 | 安全合规禁止 | BRD §D legal |
| 在 free text 中索取或存储敏感个人数据 | UK GDPR | BRD §D legal |
| 暗示已采取实际行动而未确认 | 防止误导（真实人工坐席已出现此问题，见 transcript "The ads have now been removed" —— V1 Bot 不得模仿）| BRD §6.3 phrases to avoid |

---

## 2.4 Escalation Matrix

| 触发条件 | 触发类型 | 适用 Use Case | 处理行为 |
|----------|---------|--------------|---------|
| **user_requests_human** | 用户主动请求 | 全部 | 立即进入 ESCALATE，记录 `escalation_reason: user_requested`，构建 handover payload |
| **faq_miss_ge_2** | 检索失败阈值 | UC-A/B/C/D/E/F/FP（allow_bot_resolution=true 的 UC）| 自动升级，`escalation_reason: faq_miss_threshold_exceeded` |
| **clarification_budget_exhausted** | 澄清预算耗尽（2 轮）| 全部 | 自动升级，`escalation_reason: clarification_budget_exhausted` |
| **intake_complete** | intake 必要字段齐备 | UC-G/H/I/J/K | **立即** handover；对 UC-H/J/K 先调 `create_case_controlled` 再 handover，`escalation_reason: intake_complete_for_<uc>` |
| **intake_required_fields_missing_after_max_attempts** | intake 收集失败 | UC-G/H/I/J/K | 仍 handover 并在 payload 中标注 `missing_identifiers`，`escalation_reason: incomplete_intake` |
| **issue_involves_refund_or_dispute** | 争议类子场景 | UC-F-01 → 降判 UC-I-01 | 立即升级（不尝试解决），`escalation_reason: payment_dispute_detected` |
| **user_requests_appeal_or_review** | 申诉/复核请求 | UC-FP-01 → 降判 UC-H-01 | 给固定话术后降判为 UC-H-01，走 intake+case 流程，`escalation_reason: appeal_requires_human` |
| **user_expresses_strong_emotion** | 情绪激动/威胁 | 全部（特别 UC-FP-01 / UC-H-01）| 安抚话术（不使用 "I understand how you feel"）+ 升级，`escalation_reason: user_distress` |
| **any_imminent_harm_signal** | 人身威胁 / 紧急安全 | UC-J-01 | 立即升级 + 高优 queue，`escalation_reason: imminent_harm` |
| **issue_classified_as_incorrect_deletion** | 误删类别 | UC-FP-01, UC-A-01 → 路由到 UC-H-01 | 降判为 UC-H-01（intake + case），`escalation_reason: incorrect_deletion_appeal` |
| **issue_involves_fraud_or_scam** | 欺诈/诈骗 | UC-C-01, UC-E-01 → 路由到 UC-J-01 | 切换到 UC-J-01 结构化 intake，`escalation_reason: trust_safety_required` |
| **issue_involves_account_ban** | 账号被封/黑名单 | UC-D-01 → 路由到 UC-FP-01 或 UC-H-01 | 切换到对应 UC，`escalation_reason: account_compliance` |
| **gdpr_or_data_request_detected** | 数据删除 / SAR | 任意 → 路由到 UC-G-01 | 切换到 UC-G-01 intake，`escalation_reason: gdpr_intake` |
| **identity_verification_required** | 需身份核验 | UC-G-01, UC-D-01 | Bot 不做核验，立即升级，`escalation_reason: identity_verification_required` |
| **out_of_scope_intent** | 非 V1 覆盖意图 | 全部 | 固定引导话术 + 转人工，`escalation_reason: out_of_scope` |
| **service_degraded** | 系统降级 | 全部 | 安全升级，`escalation_reason: service_degraded` |
| **max_bot_turns_exceeded** | Bot 轮数超限 | 全部 | 强制升级，`escalation_reason: turn_budget_exhausted` |
| **tool_scope_blocked** | 模型尝试调用超范围工具 | 全部 | runtime 拒绝调用 + 升级，`escalation_reason: tool_scope_blocked`（用于 Guardrail grader）|

### 升级话术要求（BRD §6.3）

升级时必须使用 `Escalation` 模板话术：

> "I'll pass this to an agent now. I'll share what you've told me so you don't need to repeat yourself."

并附带：
- 等待预期（wait time，若可取得）
- 下一步说明

非工作时段触发升级时，使用 `Out of hours` 模板：

> "The team is currently offline. I can still help with common questions, or log this for follow-up."

---

## 2.5 Knowledge Scope Map

### Use Case → Knowledge Sources

> 对齐 tool_spec v0.2 的 `search_knowledge.allowed_use_cases` = UC-A/B/C/D/E/F/FP；其他 UC（UC-G/H/I/J/K）不启用 `search_knowledge`，只用固定话术模板（`fixed_script_library` runtime capability）。
>
> **关键约束（v4 已确认）**：Salesforce Knowledge API 当前**无可用开发 API 能力**（`salesforce-part-spec.md`）。Bot 知识检索完全依赖 **pgvector 离线索引**（3,963 篇文章，见 `FAQ-knowledge_include_help_url.csv`）。Salesforce Knowledge 仅用于 article Id → 发布状态验证（`resolve_article` 的 `must_verify_published_status` policy）。
>
> **UC-FP 增强（v0.2 新增）**：`get_moderation_review_context` 工具（runtime_only，gumshield cs-review API）为 UC-A/UC-FP 提供广告审核/删除的具体 reason_code，使 Bot 能给出 grounded 的"为什么被删"解释而非 generic "policy violation"。

| Use Case | 允许的 Knowledge 范围 | 期望 Evidence 类型 | `search_knowledge` 可调用？ |
|----------|---------------------|------------------|------------------------|
| UC-A-01 | Help Centre: ad review, ad status, category visibility, regional display | 文章链接 + 摘要段落 | ✅ |
| UC-B-01 | Help Centre: posting guide, editing guide, category/location/image rules | 步骤化 FAQ + 深链 | ✅ |
| UC-C-01 | Help Centre: messaging, inbox, notifications, safety tips | 操作路径 + 安全提示 | ✅ |
| UC-D-01 | Help Centre: password reset, login, email settings, alerts, unsubscribe | 自助链接 + 路径说明 | ✅ |
| UC-E-01 | Help Centre: search guide, product features, feedback channel | 功能说明 + 表单链接 | ✅ |
| UC-F-01 | Help Centre: payments policy, fees, payment process | 政策说明 + 免责声明 | ✅ |
| UC-FP-01 | Help Centre: community standards, ad policies, prohibited content, reposting | 政策摘要 + 标准模板 + 重发指引 | ✅ |
| UC-G-01 | 固定话术 — GDPR 流程解释 + Help Centre GDPR overview（仅作为链接引用，不用于生成答案）| 流程说明 + 外部链接 | ❌（tool_spec disallowed）|
| UC-H-01 | **无** — Bot 不 grounded generate；使用固定安抚 + intake 模板 | — | ❌（tool_spec disallowed）|
| UC-I-01 | **无** — Bot 不给支付结论；使用固定话术 | — | ❌（tool_spec disallowed）|
| UC-J-01 | **无** — Bot 不给安全结论；使用固定结构化举报表 | — | ❌（tool_spec disallowed）|
| UC-K-01 | **无**（通过 get_customer_context 读状态 + 固定话术回复）| 状态说明 | ❌（tool_spec disallowed）|

### Use Case → Disallowed Knowledge

| Use Case | 不可作为事实源的内容 |
|----------|-------------------|
| 全部 | 用户自由输入内容、未审批 Git 文档、外部网站内容 |
| UC-F-01 | 非 Gumtree 官方支付政策、第三方支付平台规则 |
| UC-FP-01 | 内部审核工具截图/日志、未公开的 TMX 规则细节 |
| UC-D-01 | 任何要求 Bot 代为执行账户操作的请求（即使知识中有相关 API）|
| UC-G/H/I/J/K | **全部外部知识源** — 这些 UC 下 `search_knowledge` 被 tool_spec 禁用；只允许固定话术库（由合规+产品审批）|

### 知识就绪度（V1 范围）

V1 仅做：
- use case scoped retrieval
- source ids tracking
- article shown logging
- faq miss tracking

V1.1 才补：readiness score、coverage gap analysis、freshness governance、low-quality source suppression（详见 tech_spec §14.4）。

---

## 2.6 Control Policy by Use Case

### 通用控制策略（所有 Use Case 共享 — 见 tech_spec §11 / PRD §1.6.2.1 G0–G5）

```yaml
discover_policy:
  # ── Pre-chat Form Integration（新增 2026-04-17）──
  # 用户进入对话前已通过 pre-chat form 提供 structured input：
  #   新版表单：first_name*, last_name*, email*, topic_subject* (dropdown),
  #             ad_id_number (optional), description*
  # Bot 在 INIT 阶段即可获得 form_context，不需要等对话才开始分类
  form_context_usage:
    - topic_subject 作为 UC 预分类弱信号（精度因选项而异，见 §1.4.13 映射表）
    - description 作为意图分类主输入（结合 topic_subject）
    - email（必填）→ 立即触发 get_customer_context（不再需要对话中问 email）
    - ad_id_number（选填）→ 若有值则同时触发 listing lookup
    - first_name → 用于 Bot 开场称呼（"Hi {first_name}, ..."）
  strong_routing_signals:
    - topic_subject = "Report a Safety Issue" → UC-J 先验 (71%)
    - topic_subject = "Delete my account / Data" → UC-G 先验 (70%)
  weak_routing_signals:
    - topic_subject = "Account Support" → 必须结合 description 再分类（37% UC-H / 11% UC-C / 9% UC-D）
    - topic_subject = "Ad Support" → 偏 UC-H 但需 description 确认
    - topic_subject = "Payments" → 多 UC 混合，需 description 确认
  # ── 原有 discover 逻辑 ──
  - 入口：form_context.topic_subject + form_context.description → 意图 + 置信度
  - 若 form 信号不足 → 菜单主题选择 或 自由文本补充 → 意图 + 置信度
  - 低置信度时展示澄清按钮（≤ 2 轮，记录 INTENT 与 intent_confidence）
  - 检测到 out-of-scope 意图时记录 candidate_use_cases 并引导
  - 检测到高风险/跨类意图时降判或升级

clarification_policy:
  max_clarification_rounds: 2
  rules:
    - 澄清问题必须推动收敛（不重复问同一问题）
    - 澄清预算耗尽后自动升级
    - 不在 free text 中索取敏感个人数据（如密码、完整身份证号）
    - 索取识别信息（email / case number）时必须解释原因
    # ── 新增：Pre-chat Form 影响 ──
    - 新版表单 email 为必填 → Bot 不再需要澄清"请提供您的 email"
    - 新版表单 ad_id 为选填 → 若用户未填且 UC 需要，仍需 1 轮澄清
    - 新版表单 description 为必填 → 减少"请描述您的问题"类澄清

resolution_policy:
  - 优先 FAQ 向量检索 + Knowledge 校验
  - 回复格式：标题 + 1-3 句摘要 + 可点击 Help Centre 链接（写入 ARTICLE_SHOWN 事件）
  - 禁止纯"请自行搜索"式回复
  - 禁止无依据自由生成
  - 低于检索阈值 → 澄清或 faq_miss 计数
  - 未解决 → 更深检索 1 次（换 query 模板）或转人工

confirmation_policy:
  - 每次回答后必须进行解决确认（"Did this solve your problem?"）— 写入 RESOLUTION_ASK 事件
  - 已解决 → 感谢/结束（Containment）
  - 未解决 → 再检索 1 次或转人工
  - 用户表达申诉诉求时跳过强迫确认，直接给下一步

escalation_policy:
  triggers: 见 §2.4 Escalation Matrix
  payload: 见 §2.7 Handover Payload
  message_requirements:
    - 升级话术必须使用 BRD §6.3 模板
    - 必须告知等待预期与下一步

close_policy:
  - 明确标记 outcome（resolved / escalated / abandoned），写入 ContainmentOutcome__c
  - 记录完整 trace 字段
  - 可选采集 CSAT
  - 一期会话结束语对齐 BRD tone：清晰、简洁、不过度道歉
```

### UC-A-01 特定策略（PRD §1.6.2.1 A）

```yaml
discover_policy_override:
  - 入口呈现快捷按钮：处理中、找不到、已下架/删除、统计/展示疑问、其他
  - 选"其他"→ 自由文本 + G1 检索

resolution_policy_override:
  - 按子意图映射固定话术片段 + RAG 召回
  - 若同时提及违规/误删申诉 → 降判为 UC-FP-01 或 H 类
  - Phase 1.x 可选：调用 listing 查询 API（已登录 + 已批准时）
  - API 失败或无权限 → 回退 A2 话术（仅状态说明 + 链接）
```

### UC-B-01 特定策略（PRD §1.6.2.1 B）

```yaml
resolution_policy_override:
  - 回复为编号步骤（3-5 步）+ 深链（Post Ad、Edit、类目/地区选择页）
  - 涉及付款失败/退款/争议 → 映射到 UC-F-01 或转人工

clarification_policy_override:
  - 可问"移动端还是网页？"以精确匹配操作路径
  - 2 轮仍无法理解 → 转人工
```

### UC-C-01 特定策略（PRD §1.6.2.1 C）

```yaml
resolution_policy_override:
  - 前置安全提示（勿站外付款、防诈骗）
  - "恢复已删消息"→ 明确话术"无法保证恢复" + Help Centre 链接，**绝不承诺**找回
  - 声称被骗/举报用户 → J 类，结构化举报入口或转人工，Bot 不裁决

escalation_policy_override:
  - 用户坚持要求恢复已删消息 → 转人工
  - 涉及被骗指控 → 立即转人工
```

### UC-D-01 特定策略（PRD §1.6.2.1 D）

```yaml
resolution_policy_override:
  - 索要"代操作"或提供密码 → 拒绝存储密码 + 仅给自助链接 + 合规提示
  - 涉及账号被封/黑名单/合规状态可解释 → 降判到 UC-FP-01
  - 涉及账号被错封/申诉诉求 → 降判到 UC-H-01（intake + case + handover）
  - 涉及删除账号/GDPR → 降判到 UC-G-01

escalation_policy_override:
  - 用户提供密码 → 立即升级（合规风险）
  - 异常登录或可疑账户行为 → 转人工
```

### UC-E-01 特定策略（PRD §1.6.2.1 E）

```yaml
resolution_policy_override:
  - 反馈 → 引导官方反馈表单/渠道
  - 投诉指向具体违法/安全 → 引导举报流程或转人工
  - 与广告状态纠缠 → 路由回 UC-A-01（合并意图，避免重复建单）
```

### UC-F-01 特定策略（PRD §1.6.2.1 F）

```yaml
resolution_policy_override:
  - 附加免责声明："不构成财务/法律建议"
  - 退款/INAD/未收到货/扣款错误 → 立即转人工，Bot 不判责

escalation_policy_override:
  - 争议类子场景不尝试解决，直接升级
```

### UC-FP-01 特定策略（PRD §1.6.2.1 F'）

```yaml
resolution_policy_override:
  - 使用标准解释模板（政策依据摘要）+ Community Standards 链接 + 合规重发指引
  - 不接"申诉成功/恢复"承诺
  - 要求复核/误删申诉 → 固定话术 + 路由到 UC-H-01（intake + create_case_controlled + handover）

confirmation_policy_override:
  - 若用户已表达申诉诉求，跳过"已解决？"确认
  - 直接给下一步（人工/表单）

escalation_policy_override:
  - 情绪激动/威胁 → 安抚（不使用 "I understand how you feel"，避免虚假感）+ 立即升级，记录 escalation_reason: user_distress
  - 与合规/产品确认最终话术模板（仍待补，见 §6.4 缺失输入）
```

### UC-G-01 特定策略（GDPR / Data Deletion Intake）

```yaml
discover_policy_override:
  - 识别"delete my account" / "erase my data" / "GDPR" / "SAR" / "right to erasure" 等关键词
  - 一旦命中，立即切换到 UC-G-01 并给固定流程解释话术

clarification_policy_override:
  max_clarification_rounds: 2
  required_intake_fields:
    - registered_email  # 必填
    - request_type      # enum: account_deletion / data_deletion / sar / other
  optional_intake_fields:
    - additional_details

resolution_policy_override:
  - **禁用 search_knowledge**（tool_spec disallowed）
  - 使用固定话术库："Your request needs to be handled by our privacy team for identity verification. I'll pass your details on — you'll hear back by email within {SLA}."
  - 仅在用户要求"如何提交"时返回 Help Centre GDPR overview 的 static 链接（不作为 grounded answer）

escalation_policy_override:
  - intake 字段齐 → 立即 `request_handover`（escalation_reason: gdpr_intake_complete）
  - 用户拒绝提供 email → 给固定话术（"you can also submit via email to {privacy email}"）+ handover
  - 身份核验由人工或后端专用流程完成；Bot 绝不声称"我已删除"

forbidden_behaviors:
  - 承诺删除已完成
  - 告知"数据已从服务器擦除"
  - 跳过人工直接承诺 SAR 响应
```

### UC-H-01 特定策略（Incorrect Deletion Appeal）

```yaml
discover_policy_override:
  - 识别"wrongly removed" / "didn't break rules" / "appeal" / "my ad keep getting removed" / "incorrect deletion" 等
  - 从 UC-FP-01 降判时保留原始用户意图文本

clarification_policy_override:
  max_clarification_rounds: 2
  required_intake_fields:
    - ad_id_or_listing_url   # 至少一个
    - registered_email
  optional_intake_fields:
    - stated_reason_or_context

resolution_policy_override:
  - **禁用 search_knowledge**（tool_spec disallowed）
  - 使用固定安抚话术模板："I'm sorry to hear your ad was removed. Let me help you get this looked into by our team."
  - **绝不调用 moderation_enforcement_action**（human_only）
  - **绝不说"the ads have now been removed" 或 "your restriction is lifted"**（真实坐席 transcript 中出现过的模式，Bot 禁止模仿）
  - 达到 intake_complete → runtime 自动调 `create_case_controlled`（UC-H 允许）→ `request_handover`

escalation_policy_override:
  - 情绪激动/威胁 → 安抚 + 立即升级（不等 intake 齐）
  - 明确威胁法律/监管 → 立即升级 + 高优 queue

customer_output_pattern:
  - acknowledge + intake_request + handover_commitment
  - 示例："I understand this is frustrating. To get this reviewed, could you confirm the ad ID and your registered email? Once I have that, I'll create a case for our support team — you'll hear back by email within 24 hours."
```

### UC-I-01 特定策略（Refund / Payment Dispute Intake）

```yaml
discover_policy_override:
  - 识别"refund" / "chargeback" / "INAD" / "item not as described" / "payment wrong" 等

clarification_policy_override:
  max_clarification_rounds: 2
  required_intake_fields:
    - registered_email
    - ad_id_or_order_reference
    - brief_description

resolution_policy_override:
  - **禁用 search_knowledge**
  - 使用固定免责 + 移交话术："Payment disputes are handled by our specialist team — I'll pass on what you've told me so they can investigate."
  - 不做金额承诺、不做退款承诺、不做责任判定
  - 达到 intake_complete → `request_handover`（UC-I 不允许 create_case_controlled，所以仅做 handover）

escalation_policy_override:
  - 任何争议关键词命中 → 立即升级（不等 intake 齐全）
```

### UC-J-01 特定策略（Trust & Safety / Fraud Report Intake）

```yaml
discover_policy_override:
  - 识别"fraud" / "scam" / "phishing" / "report user" / "stolen" / "fake ad" / "impersonation" 等

clarification_policy_override:
  max_clarification_rounds: 2
  required_intake_fields:
    - report_target   # ad_id / seller_name / URL / phone
    - report_type     # fraud / scam / impersonation / harassment / other
    - description
  optional_intake_fields:
    - contacted_police
    - evidence_references

resolution_policy_override:
  - **禁用 search_knowledge**
  - 使用固定结构化举报话术："Thank you for reporting this. I'll gather a few details and pass them to our Trust & Safety team for review."
  - **绝不调用 moderation_enforcement_action**（human_only）
  - **绝不承诺"账户已封禁" / "广告已删除"**（真实坐席 transcript 出现该承诺，Bot 禁止）
  - 达到 intake_complete → runtime 自动调 `create_case_controlled`（UC-J 允许）→ `request_handover`

escalation_policy_override:
  - 检测到人身威胁 / 紧急安全关键词 → 立即升级 + 高优 queue（imminent_harm）
```

### UC-K-01 特定策略（Technical Issue Intake）

```yaml
discover_policy_override:
  - 识别 "crash" / "can't load" / "error" / "bug" / "not working" / "invalid token" 等

clarification_policy_override:
  max_clarification_rounds: 2
  required_intake_fields:
    - platform     # ios / android / web
    - browser_or_app_version
    - repro_steps_or_error_message
  optional_intake_fields:
    - screenshot_reference

resolution_policy_override:
  - **禁用 search_knowledge**（UC-K 未在 allowed_use_cases）
  - 允许调用 `get_customer_context`（UC-K 唯一同时允许 account+listing 的 UC）用于确认账户/广告状态
  - 状态可解释（如"your account is fine, ad is live"）→ 固定话术回复并进入 confirmation
  - 状态不可解释或疑似后端故障 → intake + `create_case_controlled`（UC-K 允许）+ `request_handover`

escalation_policy_override:
  - 澄清 2 轮仍无定位 → intake + case + handover
```

---

## 2.7 Outcome Definition

### 结果分类与 Salesforce 字段映射

| Outcome | 定义 | 统计条件 | `ContainmentOutcome__c` |
|---------|------|---------|------------------------|
| **resolved** | 用户在 Bot 轮次内确认问题已解决，或获取正确下一步后正常结束会话 | 用户确认"已解决" 或 会话正常关闭且无转人工 | `RESOLVED` |
| **clarified_but_unresolved** | Bot 正确理解了问题并提供了 grounded 回答，但用户表示未解决、需进一步帮助 | 用户回答"未解决"后选择不转人工、自行离开 | `ABANDONED`（子类标记 `unresolved_after_answer`）|
| **escalated** | 会话经任何触发条件转交人工坐席 | escalation_requested 事件记录 | `ESCALATED` |
| **abandoned** | 用户在 Bot 流程中未完成交互即离开（无确认、无转人工）| 会话超时或用户关闭且无 outcome 记录 | `ABANDONED` |
| **wrong_containment** | Bot 标记为 resolved 但用户实际问题未被解决 | repeat contact within 24h on same intent 或 human review 标记 | 回溯修正为 `WRONG_CONTAINMENT` |

### Handling State 联动

| ContainmentOutcome | HandlingState 终态 |
|-------------------|-------------------|
| RESOLVED | CLOSED |
| ESCALATED | HUMAN_HANDLING（最终也会走到 CLOSED）|
| ABANDONED | CLOSED |
| WRONG_CONTAINMENT | CLOSED（需回溯标记）|

### Handover Payload（对齐 tech_spec §15.3 + PRD §B-IN-07）

升级时必须写入 Salesforce 自定义字段（`Bot_Context__c` Long Text，JSON 格式）：

```json
{
  "session_id": "<MessagingSession Id>",
  "primary_use_case": "UC-FP-01",
  "candidate_use_cases": ["UC-A-01"],
  "current_status": "user_requested_appeal",
  "summary": "<≤ 200 chars 摘要>",
  "clarification_count": 1,
  "faq_miss_count": 0,
  "articles_shown": ["kb_id_1", "kb_id_2"],
  "escalation_reason": "appeal_requires_human",
  "transcript_ref": "<reference 或 inline transcript>",
  "intent_confidence": 0.87,
  "case_id": "<Case Id 若已创建>"
}
```

### Outcome 质量约束

- **wrong containment ≤ 2%**（继承 launch gate）
- **每周抽样审核**：从 resolved / escalated / abandoned 各抽样，确认 outcome 标记准确性 → 进 bad-case bank
- **abandoned 归因**：区分"用户在哪个步骤离开"（intent selection / FAQ display / resolution ask），纳入 drop-off funnel 分析

---

## 2.8 Use Case 间路由规则（Cross-Use-Case Routing）

V1 允许以下 use case 间的降判/路由（所有 UC 均在 V1 范围内，不再出现"转二期"路径）：

| 源 Use Case | 目标 | 触发条件 | 行为 |
|-------------|------|---------|------|
| UC-A-01 | UC-FP-01 | 用户提及违规/删除原因 | 切换 active_use_case，保留原问题标记 |
| UC-A-01 | UC-H-01 | 用户表达申诉诉求 / "incorrect deletion" | 降判为 UC-H-01，走 intake+case+handover |
| UC-B-01 | UC-F-01 | 用户提及支付失败（非争议）| 切换 active_use_case |
| UC-B-01 | UC-I-01 | 用户提及退款/争议 | 降判为 UC-I-01，立即转人工 |
| UC-C-01 | UC-J-01 | 用户声称被骗/举报其他用户 | 降判为 UC-J-01 结构化举报 intake |
| UC-D-01 | UC-FP-01 | 用户提及账号被封（可解释）| 切换到合规解释话术 |
| UC-D-01 | UC-H-01 | 用户声称账号被错封 / 申诉 | 降判为 UC-H-01 intake |
| UC-D-01 | UC-G-01 | 用户要求删除账号 | 降判为 UC-G-01 GDPR intake |
| UC-D-01 | 立即升级（无 UC 变更）| 用户提供密码或要求代操作 | 立即转人工，不再做 clarification |
| UC-E-01 | UC-A-01 | 用户从产品问题转到广告状态 | 合并意图，路由回 A |
| UC-E-01 | UC-J-01 | 投诉指向违法/安全 | 降判为 UC-J-01 |
| UC-F-01 | UC-I-01 | 任何争议子场景（refund/INAD/chargeback）| 降判为 UC-I-01，立即转人工 |
| UC-FP-01 | UC-H-01 | 用户要求申诉/复核 | 降判为 UC-H-01 intake+case+handover |
| UC-H-01 | UC-J-01 | intake 中发现涉及欺诈/安全 | 切换到 UC-J-01 结构化举报 |
| 任意 | UC-G-01 | GDPR/数据删除关键词命中 | 降判为 UC-G-01 |
| 任意 | UC-J-01 | 人身威胁 / imminent harm | 立即切换 + 高优升级 |

### Drift 处理规则（继承 tech_spec §11.6）

- **minor drift**：用户补充信息但不切换问题 → 保持当前 use case，追加上下文。
- **soft shift**：用户引入新问题 → 允许切 active_use_case，但在 state 中保留未解决的 primary issue 标记（防 issue loss）。
- **hard shift**：新问题属于高风险或优先级更高 → 直接升级或切换策略。

---

## 2.9 Guardrails（领域落地，对齐 tech_spec §16 + tool_spec v0.2 + `customer_service_agent-Common-Phrases.md`）

> v4 新增参考源：`customer_service_agent-Common-Phrases.md`（从 1.18 万条真实 transcript 提炼的 7 类标准话术模板）对齐到 tool_spec_v0.2 的 `fixed_script_library` runtime capability。Bot 的固定话术必须基于该文档 + BRD §6.3 + 合规审批后的模板库。

| 类别 | 规则 | 落地方式 |
|------|------|---------|
| **Bot 身份披露** | INIT 阶段必须出示身份："I'm the Gumtree Support Assistant" | tech_spec §11.3 INIT；BRD §6.3 opening |
| **不伪装人工** | 任何场景禁止暗示 "I'm a human agent"；真实 transcript 中出现 "Are you a real human?" 的用户提问需用固定诚实回应 | prompt-level + grader 检测 |
| **不在无依据时编造** | UC-A/B/C/D/E/F/FP 的 RESOLVE 回复必须 grounded（含 source_ids）；UC-G/H/I/J/K 完全使用固定话术，不 grounded-generate | tech_spec §14.3 answer contract；eval grounded_pass_rate |
| **不做高风险自动裁决** | 见 §2.3 Forbidden Automation | use case allow_bot_resolution=false 或强制升级 |
| **不做越权承诺** | 禁止 "I've fixed that" / "I'll process your refund" / "The ads have now been removed" / "I have also restricted your number" / "Your restriction is lifted" 等（真实坐席 transcript 出现的模式，Bot 禁止模仿）| BRD §6.3 phrases to avoid + transcript 样本 + grader 检测 forbidden phrases |
| **工具 scope 强约束** | 每次 tool call 前 runtime 检查 `active_use_case ∈ tool.allowed_use_cases`；违规返回 `scope_blocked` 并触发 `tool_scope_blocked` 升级 | tool_spec runtime 实施 |
| **敏感写操作只能由人触发** | `moderation_enforcement_action` / `send_followup_email_or_async_update` — Bot 永远不发起；`create_case_controlled` 由 runtime 按 use case policy 自动触发（非模型自由调用）| tool_spec visibility=human_only / runtime_only |
| **最小信息暴露** | `get_customer_context` 必须返回 `safe_summary` 而非完整 account dump；transcript 与日志 redact PII | tool_spec runtime_policy.must_minimize_pii / must_return_safe_summary_only |
| **品牌口径** | 命名 = "Gumtree Support Assistant"；语气 = friendly/clear/short；禁用 "AI"/"As an AI language model"/反复 sorry | prompt + style grader |
| **GDPR / PII 保护** | 不在 free text 索取或存储敏感数据（密码、完整身份证号）；索取识别信息时解释原因（"so I can locate your account"）| clarification policy + redaction |
| **identifier 收集标准** | 优先顺序：registered_email > ad_id / listing_id > phone；每次收集前必须解释原因；PII 按 schema 进入 state 并在 log 中 redact | 对齐 transcript 样本观察到的 126/499 标识符收集模式 |
| **可恢复的 "Start again"** | 用户可随时重置会话 | UI 入口 + state reset action |
| **WCAG 无障碍** | 文本要素满足基本可读性；菜单按钮可键盘操作 | UI 实现规范 |
| **Bot 占位进度反馈** | tool call 超过 1.5s 需给"正在查询"占位话术，避免用户重复追问"???" / "are you there?"（transcript 常见 frustration 触发）| runtime 层 streaming 占位 |

---

## 2.10 Tool Allocation per Use Case（对齐 `customer_service_tool_spec_v0_2.yaml`）

### 2.10.1 Agent-Visible Tool × Use Case 矩阵

| Tool \\ UC | UC-A | UC-B | UC-C | UC-D | UC-E | UC-F | UC-FP | UC-G | UC-H | UC-I | UC-J | UC-K |
|-----------|:---:|:---:|:---:|:---:|:---:|:---:|:----:|:---:|:---:|:---:|:---:|:---:|
| `search_knowledge` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `resolve_article` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `get_customer_context` | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `request_handover` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `record_outcome` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

> ✅ = tool_spec v0.2 允许（in `allowed_use_cases`）；❌ = 禁用（in `disallowed_use_cases`）
>
> **v0.2 变更**：`get_customer_context` 的 `disallowed_use_cases` 明确为 [UC-B, UC-E, UC-G, UC-H, UC-I, UC-J]。UC-B/UC-E 不需要账户/广告上下文（纯 FAQ）；UC-G/H/I/J 为 intake-only UC，Bot 不做 context lookup（通过 ask_user 收集标识符后直接 handover）。
>
> **Pre-chat form 影响**：`get_customer_context` 增加 `form_context` 输入 + `must_auto_trigger_on_form_context: true` policy — 当 INIT 阶段有 email 时自动触发（不等对话中问 email）。

### 2.10.2 Runtime-Only Tool × Use Case 矩阵（v0.2 = 4 个）

| Tool \\ UC | UC-A | UC-B | UC-C | UC-D | UC-E | UC-F | UC-FP | UC-G | UC-H | UC-I | UC-J | UC-K |
|-----------|:---:|:---:|:---:|:---:|:---:|:---:|:----:|:---:|:---:|:---:|:---:|:---:|
| `lookup_customer_account` | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| `lookup_listing_or_ad` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **`get_moderation_review_context`** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `create_case_controlled` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ |

> **v0.2 新增 `get_moderation_review_context`**（gumshield cs-review API）：仅 UC-A / UC-FP 允许。由 `lookup_listing_or_ad` 链式调用（step 5: `POST /api/cs-review/ad-id/`）。为 UC-FP 提供具体 `review_reason` / `reason_code` / `deletion_reason_code`，使 Bot 能 grounded 解释"为什么帖子被删"而非 generic "policy violation"。`must_map_reason_to_public_policy: true` — runtime 需将内部 reason_code 映射为公开政策解释后才暴露给 Bot。
>
> `lookup_*` + `get_moderation_review_context` 不被模型直接调用；由 `get_customer_context` 复合触发。
>
> `create_case_controlled` 由 runtime 根据 UC-H/J/K 的 `intake_complete` 条件自动触发。

### 2.10.3 Human-Only Tool（Bot 永远不可调用）

| Tool | 允许调用者 | 说明 |
|------|-----------|------|
| `moderation_enforcement_action` | 坐席 console / back-office | 删帖、限号、账号限制；tool_spec visibility=human_only，risk_tier=critical |
| `send_followup_email_or_async_update` | 坐席 / back-office workflow | 异步邮件跟进；V1.1+ 或人工触发 |

### 2.10.4 Per-UC Tool Call Sequence（期望模式，v0.2 含 form_context 触发）

> **INIT 阶段**（所有 UC 共享）：`form_context_ingestion` 解析 pre-chat form → 若有 email → 自动触发 `get_customer_context`（仅对 allowed UC）。此步骤在 UC 路由之前完成。

| Use Case | 典型 tool call 序列 |
|----------|-------------------|
| UC-A-01 | [INIT: auto `get_customer_context`(listing, from form email+ad_id)] → `search_knowledge` → `resolve_article` → `record_outcome` |
| UC-B-01 | `search_knowledge` → `resolve_article` → `record_outcome`（`get_customer_context` 对 UC-B 禁用）|
| UC-C-01 | [INIT: auto `get_customer_context`(account)] → `search_knowledge` → `resolve_article` → `record_outcome` |
| UC-D-01 | [INIT: auto `get_customer_context`(account)] → `search_knowledge` → `resolve_article` → `record_outcome` |
| UC-E-01 | `search_knowledge` → `resolve_article` → `record_outcome`（`get_customer_context` 对 UC-E 禁用）|
| UC-F-01 | [INIT: auto `get_customer_context`(listing)] → `search_knowledge` → `resolve_article` → `record_outcome` |
| UC-FP-01 | [INIT: auto `get_customer_context`(account+listing, 含 `get_moderation_review_context` 链式调用)] → `search_knowledge` → `resolve_article` → `record_outcome` |
| UC-G-01 | [intake via ask_user + fixed_script_library] → `request_handover`(is_business_hours) → `record_outcome` |
| UC-H-01 | [intake via ask_user + fixed_script_library] → runtime `create_case_controlled` → `request_handover` → `record_outcome` |
| UC-I-01 | [intake via ask_user + fixed_script_library] → `request_handover` → `record_outcome` |
| UC-J-01 | [intake via ask_user + fixed_script_library] → runtime `create_case_controlled` → `request_handover` → `record_outcome` |
| UC-K-01 | [INIT: auto `get_customer_context`] → [若 safe_summary 可解释] → `record_outcome`；[若需后端调查] → runtime `create_case_controlled` → `request_handover` → `record_outcome` |

### 2.10.5 `create_case_controlled` Required Fields 合约（v4 已与 tool_spec v0.2 + Salesforce 对齐）

**Salesforce Case 字段（v4 已确认，`salesforce-part-spec.md`）：**
- Record Type：`Customer Service`
- 必填：`First Name` / `Last Name` / `Email` / `Topic Subject`（Picklist）/ `Description`
- 选填：`Ad ID Number`

**Per-UC required_fields（tool_spec v0.2 `per_uc_required_fields`）：**

| UC | Bot 需收集的 Required Fields | Topic Subject 映射 | Queue 路由 |
|----|---------------------------|-------------------|-----------|
| UC-H | `ad_id_or_listing_url`, `registered_email`, `stated_reason_or_context` | `Ad Support` | online: CS_NEW_chat / offline: CS_Cases_New |
| UC-J | `report_target`, `report_type`, `description`；optional: `contacted_police`, `evidence_references` | `Report a Safety Issue` | online: CS_NEW_chat / offline: CS_Cases_New |
| UC-K | `platform`, `repro_steps_or_error_message`；optional: `browser_or_app_version`, `screenshot_reference` | `Technical Support` | online: CS_NEW_chat / offline: CS_Cases_New |

> **注**：`First Name` / `Last Name` / `Email` 来自 pre-chat form（新版表单必填），Bot 不需要再次索取。Bot 只需补充 per-UC 特有字段（ad_id / report_target / platform 等）。
>
> **Queue 路由**：v4 已确认 online → CS_NEW_chat（坐席主动认领），offline → CS_Cases_New。Phase 3 需确认是否需要 per-UC 专属 queue（如 Trust & Safety 专属 queue）或统一用 CS_Cases_New。

### 2.10.6 Tool Runtime Policy 对照（对齐 tool_spec v0.2 `runtime_policy`）

| Tool | Policy 关键约束 | Phase 3 实现要求 |
|------|----------------|----------------|
| `search_knowledge` | `max_retries: 1` / `on_no_results: allow_clarification_or_escalate` / `must_log_article_candidates: true` | pgvector 检索 Top-3；失败 1 次后触发 clarification 或 faq_miss 计数 |
| `resolve_article` | `must_track_article_shown: true` / `customer_output_pattern: title_plus_1_to_3_sentence_summary_plus_link` / **`must_verify_published_status: true`**（v0.2） | 每次 resolve 必写 ARTICLE_SHOWN 事件；需验证文章发布状态 |
| `get_customer_context` | `must_minimize_pii` / `must_return_safe_summary_only` / `should_prompt_for_identifiers_when_missing` / **`must_auto_trigger_on_form_context: true`**（v0.2） | pre-chat form 有 email 时 INIT 自动触发；返回 safe_summary + 结构化 context |
| `get_moderation_review_context`（v0.2 新增） | `must_minimize_pii` / `must_return_safe_summary_only` / **`must_map_reason_to_public_policy: true`** | 内部 reason_code 必须映射为公开政策解释后才暴露给 Bot；由 `lookup_listing_or_ad` 链式调用 |
| `request_handover` | `must_preserve_same_thread` / `must_include_structured_summary` / `must_log_escalation_reason` / **`must_select_message_by_hours_and_reason: true`**（v0.2） | 根据 `is_business_hours` + `escalation_reason` 选择话术模板（在线 vs 离线） |
| `record_outcome` | `must_be_called_on_close_or_escalation: true` / **`retry_on_failure: true`**（v0.2） | 会话结束或 handover 后必调；failed 时自动 retry |
| `create_case_controlled` | `model_direct_invocation_allowed: false` / `must_validate_required_fields: true` / `must_follow_use_case_policy: true` | 由 runtime 策略层触发；validator 检查 §2.10.5 required_fields |

### 2.10.7 Runtime Capabilities（v0.2 新增，非 tool）

| Capability | 类型 | 用途 | 来源 |
|-----------|------|------|------|
| `fixed_script_library` | template_store | 管理式话术模板库。为 UC-G/H/I/J/K（无 knowledge retrieval）+ 通用 opening/closing/escalation 提供固定话术。模板分 14 类：opening / empathy / hold_placeholder / identifier_request / resolution_check / escalation_business_hours / escalation_off_hours / gdpr_intake / appeal_intake / dispute_disclaimer / safety_intake / tech_troubleshoot / policy_explanation / idle_close | BRD §6.3 + `customer_service_agent-Common-Phrases.md` + phase2 per-UC policy overrides + 合规审批 |
| `form_context_ingestion` | session_init | INIT 阶段解析 pre-chat form（first_name / last_name / email / topic_subject / ad_id_number / description）→ 写入 session state → 自动触发 `get_customer_context`（若 email 可用且 UC 允许）| salesforce-part-spec.md pre-chat form |
| `tool_policy_enforcer` | runtime_guard | 每次 tool call 前检查 `active_use_case ∈ tool.allowed_use_cases`；violation → `scope_blocked` + event log | tool_spec v0.2 per-UC matrix |
| `progress_placeholder` | ux_enhancement | tool call 延迟 >1.5s 时发送 "One moment while I look into this" 占位消息 | transcript 观察：用户常在等待时重复追问"???" |

---

## 2.11 Phase 2 → Phase 3 移交清单

进入 Phase 3 Detailed Technical Design 之前，本 Phase 2 输出已固定的契约项：

- ✅ 12 个 use case 的完整 schema（§2.2；UC-A/B/C/D/E/F/FP + UC-G/H/I/J/K）
- ✅ Risk 分级（含 critical）与 forbidden automation 列表（§2.3）
- ✅ 17 类 escalation triggers + reason codes（§2.4）
- ✅ Use case → knowledge scope 映射框架（§2.5；**v4：Knowledge API 不可用已确认，pgvector 为唯一检索后端；3,963 篇文章 CSV 已可用**）
- ✅ 通用 + per-UC 控制策略（§2.6；含 UC-G/H/I/J/K intake 策略 + **v4 pre-chat form integration**）
- ✅ 5 类 outcome + ContainmentOutcome__c 映射（§2.7）
- ✅ Handover payload JSON schema（§2.7）
- ✅ 跨 UC 路由规则 + drift 语义（§2.8）
- ✅ Guardrails 领域落地（§2.9；**v4 对齐 Common-Phrases 文档 + fixed_script_library**）
- ✅ **Per-UC tool allocation 矩阵 + runtime policy（§2.10；v4 对齐 tool_spec v0.2 — 含 `get_moderation_review_context` 新工具 + runtime capabilities + confirmed Salesforce Case 字段）**
- ✅ **7 类 eval 数据集已构建（§phase1 1.5.1）+ 367 条 human review queue 待标注**

**Phase 3 需要在此基础上产出的是实现层细节**（Runtime / State Model 实例化 / Tool 实现 / Integration 详细 / Observability schema 实例 / NFR 验证方式等），不应反向修改本 Phase 2 已固定的契约项。

### Phase 3 之前仍待补齐输入（见 workbook §6.4 更新后的状态）

**已解决（v4）：**
- ~~Salesforce 组织配置~~ → `salesforce-part-spec.md` 已确认
- ~~向量库选型~~ → pgvector on Cloud SQL 已确认
- ~~Off-hours 策略~~ → 检查 New Chat queue agent 在线状态已确认
- ~~Salesforce 自定义对象~~ → `Chat_Message_Log__c` 已存在
- ~~Help Centre 文章清单~~ → `FAQ-knowledge_include_help_url.csv` 3,963 篇已可用
- ~~create_case_controlled per-UC required_fields~~ → tool_spec v0.2 已定义初版

**仍待补齐：**
- UC-FP-01 / UC-H-01 安抚与解释话术**合规终审**
- UC-G/H/I/J/K 固定话术库（`fixed_script_library`）**合规审批**
- UC-G-01 GDPR intake 字段边界（哪些由 Bot 采集 vs 人工核验）
- article → UC 映射（3,963 篇文章的 UC 分类标注）
- Embedding 模型选型
- pgvector 索引策略（IVFFlat vs HNSW）
- faq_miss score_threshold 确定
- gumshield cs-review API Bot 服务账号访问审批
- Golden Dataset human review 标注（367 条 queue）
- CSAT 采集机制与阈值
- PII redaction 规则
- 流量分配策略与 go/no-go 阈值
- per-UC 专属 queue 是否需要（或统一 CS_Cases_New / CS_NEW_chat）
