# 客服标准回复话术指导手册（基于聊天记录提炼）

本文档依据同目录数据文件 `Transcript_case_csat.csv` 中的在线聊天转写（Body）归纳而成，用于培训与质检参考。

## 1. 数据说明与使用方式

| 项目 | 说明 |
|------|------|
| 数据来源 | 导出字段含对话正文（Body）、主题（Subject）、原因（Reason）、StellaConnect 星级（`StellaConnect__Star_Rating__c`）等。 |
| 有对话正文的记录 | 约 1.18 万条。 |
| 显式 CSAT 星级 | 总量较少（例如 5 星约 76 条、1 星约 39 条），**不宜仅凭星级做统计结论**；话术模式主要从**全量有正文的对话**中归纳。 |
| 语言 | 原始话术为 **英语**（Gumtree UK 场景）；对内说明为中文。 |

**使用建议**：下列英文句式为高频真实用法，可直接作为模板；具体政策、链接与时效须与现行知识库及流程保持一致后再对外使用。

---

## 2. 对话结构总览（推荐顺序）

1. **开场问候** → 2. **共情 / 确认问题** → 3. **必要时索取信息** → 4. **告知正在处理（Hold）** → 5. **给结论或步骤** → 6. **升级 / 邮件跟进（如需要）** → 7. **收尾与关单话术**。

---

## 3. 全局标准话术

### 3.1 开场（极高频，几乎每条会话）

**模板 A（最常用）：**

> Hello, my name is {Agent}. Thanks so much for reaching out! Please allow me a moment to review your request so I can find the best solution for you.

**中文要点**：自报姓名、感谢联系、说明会先看诉求再找方案。

**模板 B（部分坐席在访客已先发消息后）：**

> Hello {Name}, thank you for contacting Gumtree UK. I am sorry for any inconvenience.

**中文要点**：点名问候、品牌名、对不便表示歉意（适用于账户/技术类跟进）。

**模板 C：**

> How may I help?

用于已明确进入主题的简短承接。

---

### 3.2 共情与承接

| 场景 | 英文参考话术 |
|------|----------------|
| 一般负面体验 | I'm sorry to hear that. / I'm sorry to hear about your experience. |
| 带姓名 | I'm sorry to hear that, {Name}. |
| 广告被删 | I'm sorry to hear your ad got removed. |
| 列表被删（正式） | I'm sorry that your listing has been removed and apologise for any inconvenience this may have caused. |
| 愿意跟进 | I'd be happy to look into this for you. |
| 理解账户问题 | I understand you are experiencing an issue. I'm reviewing your account to identify the exact cause. |
| 更强烈共情（差评/纠纷等） | We're truly sorry to hear about {issue} - we understand how upsetting and frustrating this can be. |

**中文要点**：先承认情绪或不便，再进入事实与流程；避免与用户争辩「你有没有说过」类细节，可转向「我需要哪些信息才能帮你」。

---

### 3.3 等待与查阅（Hold）

高频组合（可拆开或连用）：

- Allow me to have a look for you. One moment please.
- Thank you for your patience.
- Thank you one moment please.
- I kindly request that you allow me a few minutes to look into this for you.
- One moment, as I review this for you.
- Please hold while I take a look.
- Taking a look {Name}.

**中文要点**：设定期望「正在查」、避免长时间静默；若需较久，应补一句为何需要等待或下一步何时回复。

---

### 3.4 索取信息（身份 / 案件）

常见问法：

- Could you please confirm your registered email address or an AD ID so I can locate your account and investigate what happened?
- Please confirm the email address on the Gumtree account?
- May I have your ad ID to check on my side?
- Please provide me with your number / email address and name of this user.

**中文要点**：一次要 1～3 项关键信息，避免清单过长；涉及安全/欺诈时，再按流程要更完整要素（见第 5 节）。

---

### 3.5 解释结论与自助步骤

- 分步说明路径（网站 vs App）：坐席常写 **My Gumtree**、**Manage my Ads**、铅笔/三点菜单等具体操作。
- 附帮助中心链接时，保持一句「可点此查看详细步骤」类引导；语料中常见域名：`help.gumtree.com`。
- 技术排查套话：清除缓存与 Cookie、更新浏览器/应用版本、换浏览器尝试登录等。

---

### 3.6 升级与邮件跟进

**临时冻结 / 需后台调查：**

> I've reviewed your account and found that there's a temporary hold that needs to be addressed. The reason needs to be investigated further, so I've forwarded your case to our support team who will be able to resolve this quickly. I'll reach out to you via email within 24 hours with an update and the next steps to get everything back on track.

变体：24–48 小时、notify you by email once we have a decision、escalate the initial decision to be investigated thoroughly。

**广告被删原因待查：**

> This has been escalated to the relevant department, to find out the reason for the ad removal. As soon as I have feedback I will revert back to you via email.

**中文要点**：说明**谁在做什么**、**下一次联系渠道**、**时间预期**（24h / 24–48h / 3–5 个工作日等需与政策一致）。

---

### 3.7 收尾与挂机

**是否还有其他问题：**

- Is there anything else I can assist you with today?
- Is there anything else, I can help with?

**感谢与结束：**

- Thank you for contacting Gumtree. / Thank you for contacting Gumtree UK.
- Happy to help and feel free to reach out anytime if you need further assistance. Thank you for contacting Gumtree.
- You're most welcome, will that be all for today?

**长时间无回复（Idle close）：**

> As our chat has been idle for a few minutes, I will need to close it. Thanks for contacting Gumtree and have a great day!

**疑似掉线：**

- {Name}, are you still there? / Are you still connected? / It seems you have left the live chat, thank you for contacting Gumtree UK.

**中文要点**：关单前尽量再给一次「是否还需要帮助」；idle 话术需与内部超时规则一致。

---

### 3.8 引导邮件工单

语料中出现：`support.response@gumtree.com`（转发邮件以便调查）、「我会给你发邮件」「请回复邮件中的问题」等。

**中文要点**：说明用户要附什么材料（截图、对话记录、付款凭证等），避免让用户在聊天里贴敏感信息除非流程允许。

---

## 4. 按主题的句式侧重（来自 Subject 分布与内容归纳）

### 4.1 Account Support / Ad Support（账户与广告）

- 广告/分类错误：说明 **categories cannot be changed**，需重新发帖等（语料中有明确句式）。
- **Gmail vs Googlemail** 两个登录身份问题：说明在 Google 可互通、在 Gumtree 为不同账户，建议退出后用正确域名再登录。
- 无法发帖、系统删帖：结合 **restriction / temporary hold** 与升级邮件话术。

### 4.2 Pets（宠物类目）

- **12 个月内最多 2 条宠物广告** 等限制：说明为遵守法规与动物福利，并给政策链接。
- **宠物类费用一般不可退款**，语料中有 **goodwill / one-time exception** 式例外退款说明，须与审批权限一致后使用。
- 退款到账时间示例：**within the next 3 - 5 business days**（以现行财务话术为准）。

### 4.3 Trust & Safety / Fraud（欺诈与安全）

- 建议用户联系 **police**、**bank**；请用户通过邮件发送 **screenshots of the conversation**。
- 说明平台可采取的行动边界（如：将对账户采取适当措施、建案后发邮件告知下一步）。
- 对「误用他人信息发帖」等：移除广告、账户删除限制说明 + 升级相关团队。

### 4.4 Technical / Messaging（消息与 App）

- 无法打开消息：可引导通过 **邮箱直接回复** 通知邮件、避免点击某类按钮等（语料中有分步说明 + 外部视频链，**链接需定期校验**）。
- 回复路径：Messages → 选择用户 → 滚动到底部输入框等逐步说明。

### 4.5 Payments / Pay & Ship

- 说明 Gumtree 与 **PayPal 无直接集成**、买卖双方自行协商支付方式，并附安全交易帮助链接（语料示例）。
- 涉及「付款给 Gumtree 代收」等诈骗话术：明确 **不要提供银行卡、CVV** 等。

### 4.6 已知产品问题（搜索半径、维护中等）

- 承认已知问题、技术团队正在处理、致歉并请用户耐心（语料中有完整段落模板）。
- 功能暂不可用（如电话展示）：说明 **maintenance**、无具体恢复时间、建议关注账户更新。

---

## 5. 信息清单模板（纠纷 / 要封禁对方账号类）

语料中 Bulelwa 等坐席使用的结构化索取示例（按流程删减使用）：

- Ad ID（10 位数字）、广告标题  
- 对方邮箱、电话  
- 付款方式与细节（账号、支付邮箱等）  
- 在 Gumtree 上沟通的日期  
- 是否全程在平台内沟通  

结尾可接：**raise a case** → 调查结束后 **email advising of the next best action**。

---

## 6. 质检与语气红线（从低质或争议片段反推）

1. **避免循环质问**：用户已说明「没有编辑选项」时，不宜反复「请说明为何不能编辑」而不给路径；应升级到截图或换端（网站/App）。  
2. **延迟要道歉**：Apologies for the delay.  
3. **被质疑机器人**：明确 **You are speaking to a human** / Yes, I am a real human，并立即承接业务问题。  
4. **用户情绪激动**：保持冷静、用事实与下一步代替辩解；可提供邮件跟进降低聊天压力。  
5. **政策解释**：坚持政策时附带 **help.gumtree.com** 或官网 policies 链接，并说明 **refund / goodwill** 边界。

---

## 7. Bot 话术模板（V1 fixed_script_library 抽取）

> 以下话术从 eval_datasets 的 1,230+ 条真实 agent 消息中提炼，服务于 Bot V1 的 `fixed_script_library` 运行时能力。完整模板库见 `fixed_script_library_v1.md`。

### 7.1 UC-FP 标准解释话术（正确删除/合规下架）

**开场共情**:
> I'm sorry to hear your ad was removed. Let me look into what happened.

**有具体原因时（由 `get_moderation_review_context` 提供 reason）**:
> Your ad was removed because {SPECIFIC_REASON}. You can review our posting policies here: {POLICY_URL}

真实坐席高频 reason 模式（从 18 条 UC-FP 对话提炼）：

| Reason | 坐席原始表述 | Bot 标准化模板 |
|--------|-------------|---------------|
| 宠物广告限额 | "You are only allowed 2 pet ads within a 12 month period" | "each user is allowed a maximum of 2 pet rehoming ads within a 12-month period, and this limit has been reached" |
| 多账号检测 | "ad was rejected due to multiple accounts detected" | "we detected activity from multiple accounts linked to you — only one account is allowed for posting" |
| 福利风险 | "ad was rejected due to welfare risk, we do not allow images or mention of Christmas/Easter gifts in pet adverts" | "images or content in the ad were flagged as a potential welfare risk under our pet policies" |
| 私卖冒充商户 | "Looking at the frequency of your ads it looks like you are a trader" | "your account activity indicates trading as a business, which requires a business account" |
| 按摩广告规范 | "We do not allow pictures of real people with massage ads" | "massage ads have specific requirements — please review our services policies" |
| 手机号移除 | "adding mobile numbers to ads has been taken away to protect users from fraudsters" | "phone numbers are no longer displayed on ads to protect users from scam calls and texts" |
| 账号受限 | "your access has been restricted because some of the ads or activity on your account do not comply with Gumtree's Terms of Use" | "your account has been restricted due to activity that doesn't comply with our Terms of Use" |

**无法获取原因时**:
> The system flagged your ad for review and it was removed. I'm not able to confirm the exact reason, but I can connect you to our team for a full investigation. You'll hear back by email within {SLA_HOURS} hours.

真实坐席原版：_"I see the system deleted the ad and I am not able to confirm the reason for this, but I am going to escalate it and as soon as I receive feedback, I will send you an email. Please note, we aim to provide feedback within 24 hours."_

**申诉引导（降判到 UC-H）**:
> I understand you'd like to appeal the removal. Let me help you get this reviewed by our team — I'll need a few details from you.

### 7.2 UC-H 安抚话术（误删申诉 intake）

**开场安抚**（从 35 条 UC-H 对话提炼的高频模式）:
> I'm sorry to hear your ad was removed. Let me help you get this looked into by our team.

真实坐席变体：
- "I am so sorry to hear that you're having trouble with your ad"
- "I'm sorry that your listing has been removed and apologise for any inconvenience this may have caused"
- "I'm really sorry to hear this."

**收集信息**:
> To get this reviewed, could you confirm:
> 1. Your ad ID (the 10-digit number at the bottom of your ad)
> 2. A brief description of what happened

**Case 创建后**:
> Thanks for those details. I've created a case ({CASE_NUMBER}) for our {TEAM_NAME} team. You'll hear back by email within {SLA_HOURS} hours.

真实坐席原版：_"I've forwarded your case to our support team who will be able to resolve this quickly. I'll reach out to you via email within 24 hours with an update and the next steps to get everything back on track."_

**账号被封升级**:
> I can see there's a hold on your account that needs further investigation. I'll pass this to our specialist team — you'll hear back by email within {SLA_HOURS} hours.

真实坐席原版：_"It seems that your account is blocked I need to escalate it and get back to you with feedback once I receive it. Please note that turn around time is 24to48 hours."_

**情绪激动安抚**:
> I completely understand your frustration, and I want to assure you this is being taken seriously. Let me connect you to our team right away.

真实坐席原版：_"I completely understand your frustration, and I want to assure you that account security is extremely important to us."_

### 7.3 UC-G 固定话术（GDPR / 数据删除 intake）

**流程解释**（从 11 条 UC-G 对话提炼）:
> Account and data deletion requests are handled by our privacy team for identity verification. I'll pass your details on — you'll hear back by email within {SLA_HOURS} hours.

真实坐席变体：
- "I will get your case through to our GDPR team, they will be in touch via email to get the SAR process started."
- "I will send this to our GDPR department and they will be in touch within 24-48hours to assist with account deletion"
- "Account can only be deleted by our GDPR team."

**需要身份核验**:
> You'll need to verify your identity before we can process this — our privacy team will send you the next steps by email.

**时间预期**:
> Account deletions can take up to 30 calendar days. Our privacy team will keep you updated by email.

### 7.4 UC-I 固定话术（退款/支付争议 intake）

**免责说明**（从 21 条 UC-I 对话提炼）:
> Payment disputes are handled by our specialist team. I'll pass on what you've told me so they can investigate.

**退款政策链接**:
> You can find more about our refund process here: https://www.gumtree.com/info/safety/p/payments/will-my-payments-be-refunded/

**收集信息**:
> To help our team look into this, could you confirm:
> 1. Your ad ID or order reference
> 2. A brief description of the issue

**Note**: Bot 不做退款承诺。真实坐席有 "I was able to make a one time exception" / "The refund can take between 1-3 days to process" 等判断 — 这些是人工专属裁决。

### 7.5 UC-J 固定话术（信任与安全/欺诈举报 intake）

**确认收到举报**（从 18 条 UC-J 对话提炼）:
> Thank you for reporting this. I'll gather a few details and pass them to our Trust & Safety team for review.

**结构化举报采集**:
> To help us investigate, please share:
> 1. The ad ID or link related to this report
> 2. What happened (as much detail as you can)
> 3. Whether you've contacted the police

**建议报警**（高频固定话术）:
> We'd also recommend reporting this to the police. You can contact Report Fraud on 0300 123 2040 or visit https://www.reportfraud.police.uk/

真实坐席原版：_"Please report this to the police. You can do this quickly and easily by contacting Report Fraud, the National Fraud and Cyber Crime Reporting centre, by calling 0300 123 2040 or visiting https://www.reportfraud.police.uk/"_

**安全资源**:
> For safety tips, visit our Safety Hub: https://www.gumtree.com/info/safety/ and Help Desk: https://help.gumtree.com/s/safety

**Note**: Bot **绝不说** "I have restricted this user's account" / "The ad has been removed"。真实坐席中有 "Please rest assured that we have taken immediate action to restrict this user's account" — Bot 禁止使用此类措辞。

### 7.6 UC-K 固定话术（技术故障 intake）

**基本排障**（从 13 条 UC-K 对话提炼）:
> A few quick things to try:
> 1. Clear your browser's cache and cookies
> 2. Make sure your browser or app is updated to the latest version
> 3. Try using a different browser
>
> Here's a guide on clearing cache: https://help.gumtree.com/s/technical-issues?cat=Troubleshooting&article=Clearing-Cookies-and-Cache2

**确认平台**:
> Are you using the website or the mobile app?

**已知问题**:
> We're aware of this issue and our technical team is looking into it. We don't have a timeframe yet, but we'll update you as soon as we can.

真实坐席原版：_"We are aware of the issue online and our Tech Team is investigating the matter. We hope to have this resolved soon but we do not have a timeframe yet."_

**技术升级**:
> I'll create a case for our technical team to investigate further. You'll hear back by email within {SLA_HOURS} hours.

---

## 8. Bot 禁止话术清单（V1 guardrails）

以下话术 Bot **永远不可输出**（从真实坐席对话中发现的模式 + BRD §6.3 禁止列表）：

| 类别 | 禁止话术 | 原因 |
|------|---------|------|
| 伪装人工 | "Hello, my name is {X}" / "You are speaking to a human" / "I am a real human" | Bot 不可伪装坐席 |
| 假行动 | "I've removed the ad" / "I've restricted their account" / "The ads have now been removed" / "I have also restricted your number" / "Your restriction is lifted" | Bot 不执行 moderation_enforcement_action |
| 假承诺 | "I'll process your refund" / "Your refund has been issued" / "I can guarantee..." | Bot 不做金融操作 |
| 假共情 | "I understand how you feel"（重复使用） | 从 bot 角度显得虚假 |
| AI 标签 | "As an AI language model..." / "I'm an AI..." | 不使用技术术语 |
| 过度道歉 | "I'm sorry to hear that"（每 session >1 次） | 避免机械重复 |
| 企业腔 | "leveraging" / "delighted" / "we apologise for inconvenience" | 不符合 Gumtree UK tone |
| 用户评判 | "you should have done the right thing" / "Posting as a private seller when you are a business is unethical" | 不评判用户（真实 bad-case 中出现过） |
| 数据操作 | "I have deleted your data" / "Your account has been erased" | GDPR 操作由专人完成 |

---

## 9. 文档维护

- 若产品流程、时效、链接变更，请同步更新本手册中的**时间与 URL**，并以知识库为准。  
- 建议每季度用新的 Transcript 导出重新跑一轮高频句式统计，刷新第 3 节模板列表。
- §7 Bot 话术模板与 `fixed_script_library_v1.md` 保持同步。
- §8 禁止话术清单需与 eval grader 的 forbidden_phrases 列表保持一致。

---

*生成说明：§1-6 句式频次通过对 CSV 中 Agent 发言行的脚本统计与人工抽样核对；CSAT 星级仅作辅助参考。§7-8 从 eval_datasets 的 1,230+ 条 agent 消息提炼（UC-FP 18 会话 / UC-H 35 会话 / UC-G 11 会话 / UC-I 21 会话 / UC-J 18 会话 / UC-K 13 会话），2026-04-18 更新。*
