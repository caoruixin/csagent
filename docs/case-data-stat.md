# 客服 Case 报表分析：Customer Service Agent 一期 / 二期范围与占比

本文档基于报表 `report1768917670361 (3).xlsb`（工作表 `report1768917670361`）中的历史 Case 结构化字段，对客户咨询类型进行归纳，并映射到 FAQ、帖子查询等一期能力与删帖 / GDPR 等二期能力，供产品与实施对齐指标假设。

**数据字段：** `Subject`、`Case Reason`、`Sub Reason`、`Opened Date`、`Case Origin`

**样本量：** 120,367 条 Case

**分析日期：** 2026-04-01

---

## 1\. 数据概览

### 1.1 渠道分布（Case Origin）

| 渠道 | 条数 | 占比 |
| :---- | :---- | :---- |
| Email | 74,374 | 61.8% |
| Chat | 35,255 | 29.3% |
| Web | 6,480 | 5.4% |
| Phone | 4,240 | 3.5% |
| 其他 | 18 | \<0.1% |

Chat 占比较高，适合作为一期机器人的主要承接渠道之一，并与 Email / Web 引导形成互补。

### 1.2 一级原因 Top（Case Reason）

| Case Reason | 条数 | 占比 |
| :---- | :---- | :---- |
| GDPR | 29,638 | 24.6% |
| Ads | 27,570 | 22.9% |
| （空白） | 11,450 | 9.5% |
| Technical Issue | 7,041 | 5.8% |
| Trust & Safety | 6,238 | 5.2% |
| Ad Management | 5,791 | 4.8% |
| User Reports | 5,373 | 4.5% |
| Accounts | 5,219 | 4.3% |
| 其他 | 若干 | \<5% 各 |

### 1.3 二级原因 Top（Sub Reason，节选）

| Sub Reason | 条数 | 占比 |
| :---- | :---- | :---- |
| Data Deletion Request | 28,699 | 23.8% |
| （空白） | 15,113 | 12.6% |
| TMX/ Filter Correct Deletion | 10,852 | 9.0% |
| Deleted/ Blacklisted Correctly | 10,023 | 8.3% |
| Log In Email Address Inquiry | 3,404 | 2.8% |
| Click here to reply feedback | 2,454 | 2.0% |
| Where is my ad? (Processing) | 2,185 | 1.8% |
| Edit Ad | 2,096 | 1.7% |
| Post Ad | 1,859 | 1.5% |
| TMX/ Filter Incorrect Deletion | 1,808 | 1.5% |
| Can't Find My Ad On Gumtree | 1,594 | 1.3% |
| 其他 | — | — |

**说明：** Sub Reason 约 **200** 种，适合采用「规则映射 \+ 意图识别」混合策略，不宜纯靠穷举脚本维护。

---

## 2\. 咨询类别定义（与机器人能力对齐）

### 2.1 一期：FAQ、查询类、轻量指引（少写库、多链接与状态说明）

| 代码 | 类别 | 典型 Sub Reason / 场景 | 一期机器人能力 |
| :---- | :---- | :---- | :---- |
| A | 帖子 / 广告状态与可见性 | Where is my ad (Processing)、Can't find my ad、Ad not available、Cant see my add、Ad stats、Activation inquiry | 处理进度说明、可见性 / 类目地区说明、帮助中心深链 |
| B | 发帖与编辑指引 | Post Ad、Edit Ad、Change category/location、Images | 分步 FAQ、跳转至对应功能页 |
| C | 消息与回复 | Replying to ad、Replies、How to recover deleted messages、No replies | 使用说明、安全提示（不承诺一定能恢复消息） |
| D | 账户与登录（非敏感执行） | Password、Log in、Log in email、Alerts/Favorites/Unsubscribe | 重置密码、登录邮箱说明、退订路径 |
| E | 通用产品与搜索 | Search、Gumtree Product、Feature inquiry/complaint、Feedback | 功能说明、反馈入口引导 |
| F | 支付咨询（非争议执行） | Gumtree Payments Inquiry 等 | 政策与流程说明；复杂争议转人工 |

### 2.2 一期可承接话术：合规下架 / 「正确删除」解释

大量进线与 **政策解释、预期管理** 相关（非「恢复帖子」承诺）：

| 典型 Sub Reason | 说明 |
| :---- | :---- |
| TMX/ Filter **Correct** Deletion | 说明下架原因与政策链接，指导合规重发 |
| Deleted/Blacklisted **Correctly** | 同上 |
| Fraud Blacklisted **Correctly** | 同上，注意措辞与安全提示 |

**边界：** 一期可提供标准话术与链接；**申诉、复核、改判** 仍属人工或二期流程，不宜计入「全自动结案」。

### 2.3 二期：操作性、合规、多轮与高风险

| 代码 | 类别 | 典型场景 | 说明 |
| :---- | :---- | :---- | :---- |
| G | GDPR / 账号与数据 | Data Deletion、Delete account、SAR 等 | 身份核验、流程推进、与后台删除能力联动 |
| H | 删帖与误删申诉 | Delete my ad、**Incorrect** deletion、Blacklist **Incorrectly** | 与审核 / 内部工具集成，多轮补全信息 |
| I | 支付退款与争议 | Refund、INAD、Refund approved 等 | 规则判定 \+ 可能对接订单 / 支付域 |
| J | 信任与安全 | Fraud、Phishing、Victim、各类 Report、Spam 相关 | 以转交与安全流程为主；机器人可做结构化信息采集 |
| K | 技术故障 | Technical Issue、App tech 等 | 排障脚本 \+ 建单；完全自动修复比例通常有限 |

### 2.4 Ads / GDPR 子类参考（便于写意图规则）

**Case Reason \= Ads 时 Sub Reason Top（节选）：**

- TMX/ Filter Correct Deletion — 8,882  
- Deleted/ Blacklisted Correctly — 8,092  
- Where is my ad? (Processing) — 2,185  
- TMX/ Filter Incorrect Deletion — 1,593  
- Ad Support — 1,110  
- Can't Find My Ad On Gumtree — 1,037  
- Edit Ad — 1,029  
- Deleted/ Blacklisted Incorrectly — 694  
- Delete My Ad — 678  
- Change Location / Change Category / Ad Stats 等 — 各数百条

**Case Reason \= GDPR 时 Sub Reason Top：**

- Data Deletion Request — 28,699  
- Delete Account Request — 1,326  
- Account Deletion — 575  
- Data Deletion Awaiting Verification — 157  
- SAR Request 等 — 少量

---

## 3\. 「能解决」占比：定义不同则数字不同

建议对业务方明确采用下列定义之一（或并行跟踪），避免单一百分比产生歧义。

| 指标定义 | 含义 | 基于本报表结构化字段的粗算区间 |
| :---- | :---- | :---- |
| **D1 全自动结案** | 无需人工介入即关闭或用户不再进线 | 通常 **10%～25%**；技术、GDPR、申诉类会显著拉低 |
| **D2 首解 / 显著减负** | 用户获得正确下一步（状态、链接、表单），部分在自助流程中完成 | 若包含 FAQ、帖子查询、账户指引及「正确删除」类标准解释，约 **45%～55%** 进线可被机器人 **有效承接或分流** |
| **D3 结构化后进人工** | 机器人完成分类、字段预填、证据引导后再转坐席 | 覆盖潜力可达 **70%+**，但 **人工仍在环** |

### 3.1 与一期 / 二期的关系（经验区间）

- **一期（FAQ \+ 帖子查询 \+ 指引 \+ 正确删除类解释话术）：** 与结构化字段强相关的「可模板化」咨询约占全量 **25%～35%**；若将「正确删除 / 合规」解释纳入一期话术（不承诺恢复），可进一步降低重复解释成本，但 **其中多数用户仍可能追问或进入申诉**，不宜整体计入 D1。  
- **二期（删除、申诉、GDPR 操作、多轮）：** 与全量约 **30%～40%** 的类型体量相关（GDPR、误删申诉、支付争议等），**实际全自动比例**受合规与风控约束，需以试点渠道 A/B 实测为准。

### 3.2 未分类进线

**Case Reason 或 Sub Reason 为空** 的记录合计约占 **12%**，适合用语义检索 / 意图模型补全，否则易落入「未知 → 转人工」。

---

## 4\. 实施与度量建议

1. **一期优先：** 帖子状态与可见性、审核中说明、编辑与类目 / 地区、登录与密码、消息与回复 FAQ；「正确删除」标准解释 \+ 政策深链。  
2. **一期增强：** 对 Reason / Sub 为空的会话做意图分类 \+ 推荐文章，降低未分类率。  
3. **二期：** GDPR / 删号、删帖与误删申诉、退款争议、Trust & Safety 结构化举报与转交。  
4. **指标：** 同时跟踪 **Containment（会话内解决）**、**CSAT**、**转人工后首响**，避免只汇报单一「解决占比」。

---
