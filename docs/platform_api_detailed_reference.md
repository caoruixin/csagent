# Gumtree 平台接口详细参考文档（CSAgent 相关）

## 文档说明

- **响应详情拆分**: 所有接口响应体已迁移至 'platform_api_response_details.md'，本文仅保留接口定义、入参与错误信息。
- **范围**: 面向 Gumtree 客服实时聊天机器人（CSAgent）可能调用的内部平台 REST API；仅保留与用户/账户查询、广告/列表、订单与支付、GDPR、内容审核、消息与会话、搜索、类目与定价知识相关的端点。
- **排除**: 管理迁移、内部 DevTools、健康检查/Actuator/指标、邮件/推送告警发送、图片管理、事件日志轮询、过期/packaging/credit、认证登录、批量任务触发、Kafka/通知后台等。
- **生成日期**: 2026-04-16
- **方法**: 以各服务仓库中的 **OpenAPI 3 / Swagger 2 契约为权威**；对 `bapi-server` 的 `ReplyController` 等契约未覆盖端点补充阅读 Java 源码；gumshield-api 路径以 `gumshield-api.json` 为准。
- **仓库扫描**: 已遍历 `/Users/cathy.geng/IdeaProjects/，以契约文件可解析且与上述域相关的服务为收录主体；其余项目若无对应契约或未落入 CS 场景则不展开。
- **路径前缀**: 文中 `bapi-server` 对外映射为 **`/api/*`**；`livead-search` / `fullad-search-service` 线上通常为 **`/api` + 契约内相对路径**；`gumshield-ad-search` 契约为 `servers.url: /api`；`block-user-conversations-papi` 为 **`/v1`**；请以实际网关路由为准。

## 全量接口总表（按服务分组）

> 接口总数：**196**

| 序号 | 服务 | Method | Path | 所属章节 |
|---:|---|---|---|---|
| 1 | `bapi-server` | `GET` | `/api/accounts/public_id/{publicId}` | `用户与账户域` |
| 2 | `bapi-server` | `GET` | `/api/accounts/{accountId}` | `用户与账户域` |
| 3 | `bapi-server` | `PUT` | `/api/accounts/{accountId}` | `用户与账户域` |
| 4 | `bapi-server` | `GET` | `/api/accounts/{accountId}/defaultImage` | `用户与账户域` |
| 5 | `bapi-server` | `PUT` | `/api/accounts/{accountId}/defaultImage` | `用户与账户域` |
| 6 | `bapi-server` | `GET` | `/api/accounts/{accountId}/eligible_packages` | `用户与账户域` |
| 7 | `bapi-server` | `GET` | `/api/accounts/{accountId}/packageUsages` | `用户与账户域` |
| 8 | `bapi-server` | `PUT` | `/api/accounts/{accountId}/reply_type/{replyType}` | `用户与账户域` |
| 9 | `bapi-server` | `GET` | `/api/accounts/{accountId}/sellerType/{categoryId}` | `用户与账户域` |
| 10 | `bapi-server` | `GET` | `/api/accounts/{accountId}/url_package` | `用户与账户域` |
| 11 | `bapi-server` | `GET` | `/api/accounts/{id}/adverts` | `用户与账户域` |
| 12 | `bapi-server` | `GET` | `/api/accounts/{id}/emails` | `用户与账户域` |
| 13 | `bapi-server` | `GET` | `/api/emails/{email}/user` | `用户与账户域` |
| 14 | `bapi-server` | `POST` | `/api/users/emails/search` | `用户与账户域` |
| 15 | `bapi-server` | `GET` | `/api/users/{id}` | `用户与账户域` |
| 16 | `bapi-server` | `GET` | `/api/users/{userId}/emails` | `用户与账户域` |
| 17 | `bapi-server` | `GET` | `/api/users/{userId}/emails/{email}/verification-key` | `用户与账户域` |
| 18 | `bapi-server` | `GET` | `/api/users/{username}/accounts` | `用户与账户域` |
| 19 | `bapi-server` | `GET` | `/api/accounts/{id}/adverts` | `广告与列表域` |
| 20 | `bapi-server` | `GET` | `/api/adverts/attributes/{categoryId}` | `广告与列表域` |
| 21 | `bapi-server` | `GET` | `/api/adverts/count/has_live` | `广告与列表域` |
| 22 | `bapi-server` | `GET` | `/api/adverts/id/search` | `广告与列表域` |
| 23 | `bapi-server` | `GET` | `/api/adverts/location/{id}` | `广告与列表域` |
| 24 | `bapi-server` | `GET` | `/api/adverts/search` | `广告与列表域` |
| 25 | `bapi-server` | `GET` | `/api/adverts/show/{id}` | `广告与列表域` |
| 26 | `bapi-server` | `GET` | `/api/adverts/since` | `广告与列表域` |
| 27 | `bapi-server` | `POST` | `/api/adverts/validate` | `广告与列表域` |
| 28 | `bapi-server` | `DELETE` | `/api/adverts/{id}` | `广告与列表域` |
| 29 | `bapi-server` | `GET` | `/api/adverts/{id}` | `广告与列表域` |
| 30 | `bapi-server` | `PUT` | `/api/adverts/{id}` | `广告与列表域` |
| 31 | `bapi-server` | `POST` | `/api/adverts/{id}/bumpup` | `广告与列表域` |
| 32 | `bapi-server` | `POST` | `/api/adverts/{id}/checkAndArchive` | `广告与列表域` |
| 33 | `bapi-server` | `POST` | `/api/adverts/{id}/deleteReason` | `广告与列表域` |
| 34 | `bapi-server` | `GET` | `/api/adverts/{id}/features` | `广告与列表域` |
| 35 | `bapi-server` | `POST` | `/api/adverts/{id}/statuses` | `广告与列表域` |
| 36 | `bapi-server` | `GET` | `/api/orders/search/advert/{advertId}` | `广告与列表域` |
| 37 | `bapi-server` | `GET` | `/api/orders/search/advert/{advertId}/paid` | `广告与列表域` |
| 38 | `bapi-server` | `POST` | `/api/price/advert/{aId}` | `广告与列表域` |
| 39 | `bapi-server` | `POST` | `/api/cs/adverts/auto_delete` | `客服广告操作（Takedown / Publish / 删除）` |
| 40 | `bapi-server` | `POST` | `/api/cs/adverts/auto_publish` | `客服广告操作（Takedown / Publish / 删除）` |
| 41 | `bapi-server` | `POST` | `/api/cs/adverts/auto_reject` | `客服广告操作（Takedown / Publish / 删除）` |
| 42 | `bapi-server` | `POST` | `/api/cs/adverts/delete_by_user` | `客服广告操作（Takedown / Publish / 删除）` |
| 43 | `bapi-server` | `POST` | `/api/cs/adverts/delete_by_user_email` | `客服广告操作（Takedown / Publish / 删除）` |
| 44 | `bapi-server` | `POST` | `/api/cs/adverts/publish` | `客服广告操作（Takedown / Publish / 删除）` |
| 45 | `bapi-server` | `POST` | `/api/cs/adverts/takedown` | `客服广告操作（Takedown / Publish / 删除）` |
| 46 | `bapi-server` | `POST` | `/api/cs/adverts/takedown_notification` | `客服广告操作（Takedown / Publish / 删除）` |
| 47 | `bapi-server` | `POST` | `/api/gdpr` | `GDPR 域` |
| 48 | `bapi-server` | `DELETE` | `/api/gdpr/account` | `GDPR 域` |
| 49 | `bapi-server` | `DELETE` | `/api/gdpr/account/delete-advert-related-data` | `GDPR 域` |
| 50 | `bapi-server` | `POST` | `/api/gdpr/account/deletion-allowed` | `GDPR 域` |
| 51 | `bapi-server` | `GET` | `/api/gdpr/anyAdsPostingSince/{accountId}/{sinceDateTime}` | `GDPR 域` |
| 52 | `bapi-server` | `DELETE` | `/api/gdpr/dormantAccount` | `GDPR 域` |
| 53 | `bapi-server` | `DELETE` | `/api/gdpr/dormantUser/{userId}` | `GDPR 域` |
| 54 | `bapi-server` | `DELETE` | `/api/gdpr/email` | `GDPR 域` |
| 55 | `bapi-server` | `DELETE` | `/api/gdpr/user/{userId}` | `GDPR 域` |
| 56 | `bapi-server` | `GET` | `/api/orders` | `订单、支付（Bapi 侧）与退款辅助` |
| 57 | `bapi-server` | `POST` | `/api/orders` | `订单、支付（Bapi 侧）与退款辅助` |
| 58 | `bapi-server` | `POST` | `/api/orders/email` | `订单、支付（Bapi 侧）与退款辅助` |
| 59 | `bapi-server` | `POST` | `/api/orders/refund` | `订单、支付（Bapi 侧）与退款辅助` |
| 60 | `bapi-server` | `GET` | `/api/orders/search/account/{accountId}` | `订单、支付（Bapi 侧）与退款辅助` |
| 61 | `bapi-server` | `GET` | `/api/orders/search/advert/{advertId}` | `订单、支付（Bapi 侧）与退款辅助` |
| 62 | `bapi-server` | `GET` | `/api/orders/search/advert/{advertId}/paid` | `订单、支付（Bapi 侧）与退款辅助` |
| 63 | `bapi-server` | `GET` | `/api/orders/search/email/{email}` | `订单、支付（Bapi 侧）与退款辅助` |
| 64 | `bapi-server` | `GET` | `/api/orders/search/id/{paymentId}` | `订单、支付（Bapi 侧）与退款辅助` |
| 65 | `bapi-server` | `GET` | `/api/orders/search/paypalref/{paypalRef}` | `订单、支付（Bapi 侧）与退款辅助` |
| 66 | `bapi-server` | `GET` | `/api/orders/{id}` | `订单、支付（Bapi 侧）与退款辅助` |
| 67 | `bapi-server` | `PUT` | `/api/orders/{id}` | `订单、支付（Bapi 侧）与退款辅助` |
| 68 | `bapi-server` | `GET` | `/api/orders/{id}/custom_payment_url` | `订单、支付（Bapi 侧）与退款辅助` |
| 69 | `bapi-server` | `GET` | `/api/orders/{id}/payment_url` | `订单、支付（Bapi 侧）与退款辅助` |
| 70 | `bapi-server` | `POST` | `/api/orders/{id}/transaction` | `订单、支付（Bapi 侧）与退款辅助` |
| 71 | `bapi-server` | `POST` | `/api/refund/addRefundIfNew` | `订单、支付（Bapi 侧）与退款辅助` |
| 72 | `bapi-server` | `POST` | `/api/refund/cancelPayment` | `订单、支付（Bapi 侧）与退款辅助` |
| 73 | `bapi-server` | `GET` | `/api/refund/transactions/refunded` | `订单、支付（Bapi 侧）与退款辅助` |
| 74 | `bapi-server` | `GET` | `/api/refund/transactions/voided` | `订单、支付（Bapi 侧）与退款辅助` |
| 75 | `bapi-server` | `GET` | `/api/adverts/attributes/{categoryId}` | `定价与类目属性（Bapi 代理/遗留面）` |
| 76 | `bapi-server` | `GET` | `/api/attributes` | `定价与类目属性（Bapi 代理/遗留面）` |
| 77 | `bapi-server` | `POST` | `/api/price/advert` | `定价与类目属性（Bapi 代理/遗留面）` |
| 78 | `bapi-server` | `POST` | `/api/price/advert/{aId}` | `定价与类目属性（Bapi 代理/遗留面）` |
| 79 | `bapi-server` | `POST` | `/api/reply` | `站内信 / Reply（契约未发布，源码为准）` |
| 80 | `bapi-server` | `POST` | `/api/reply/validate` | `站内信 / Reply（契约未发布，源码为准）` |
| 81 | `user-service` | `GET` | `/accounts` | `user-service` |
| 82 | `user-service` | `GET` | `/accounts/{accountId}` | `user-service` |
| 83 | `user-service` | `GET` | `/user/id` | `user-service` |
| 84 | `user-service` | `DELETE` | `/users/gdpr/ddr/{userId}` | `user-service` |
| 85 | `user-service` | `DELETE` | `/users/gdpr/dormant/{userId}` | `user-service` |
| 86 | `user-service` | `GET` | `/users/last-logged-in/{userId}` | `user-service` |
| 87 | `user-service` | `GET` | `/users/{email}/user` | `user-service` |
| 88 | `user-service` | `GET` | `/users/{userId}` | `user-service` |
| 89 | `advert-service` | `GET` | `/api/adverts/by-advert-id/{advert-id}` | `advert-service` |
| 90 | `advert-service` | `POST` | `/api/adverts/query` | `advert-service` |
| 91 | `advert-service` | `GET` | `/api/adverts/search` | `advert-service` |
| 92 | `advert-service` | `GET` | `/api/adverts/{account-id}` | `advert-service` |
| 93 | `advert-service` | `GET` | `/api/adverts/{account-id}/{advert-id}` | `advert-service` |
| 94 | `gumshield-api（Trust & Safety）` | `POST` | `/api/adverts/status` | `gumshield-api（Trust & Safety）` |
| 95 | `gumshield-api（Trust & Safety）` | `GET` | `/api/adverts/status/{id}` | `gumshield-api（Trust & Safety）` |
| 96 | `gumshield-api（Trust & Safety）` | `GET` | `/api/adverts/{id}/known-good` | `gumshield-api（Trust & Safety）` |
| 97 | `gumshield-api（Trust & Safety）` | `GET` | `/api/adverts/{id}/known_good` | `gumshield-api（Trust & Safety）` |
| 98 | `gumshield-api（Trust & Safety）` | `GET` | `/api/adverts/{id}/pro_account` | `gumshield-api（Trust & Safety）` |
| 99 | `gumshield-api（Trust & Safety）` | `DELETE` | `/api/blacklists/email` | `gumshield-api（Trust & Safety）` |
| 100 | `gumshield-api（Trust & Safety）` | `POST` | `/api/blacklists/email` | `gumshield-api（Trust & Safety）` |
| 101 | `gumshield-api（Trust & Safety）` | `POST` | `/api/blacklists/email/check` | `gumshield-api（Trust & Safety）` |
| 102 | `gumshield-api（Trust & Safety）` | `POST` | `/api/checklists` | `gumshield-api（Trust & Safety）` |
| 103 | `gumshield-api（Trust & Safety）` | `POST` | `/api/checklists/batch-check` | `gumshield-api（Trust & Safety）` |
| 104 | `gumshield-api（Trust & Safety）` | `DELETE` | `/api/checklists/q/{type}/{attribute}` | `gumshield-api（Trust & Safety）` |
| 105 | `gumshield-api（Trust & Safety）` | `POST` | `/api/checklists/q/{type}/{attribute}` | `gumshield-api（Trust & Safety）` |
| 106 | `gumshield-api（Trust & Safety）` | `GET` | `/api/checklists/q/{type}/{attribute}/{query}/endswith` | `gumshield-api（Trust & Safety）` |
| 107 | `gumshield-api（Trust & Safety）` | `DELETE` | `/api/checklists/{id}` | `gumshield-api（Trust & Safety）` |
| 108 | `gumshield-api（Trust & Safety）` | `GET` | `/api/checklists/{id}` | `gumshield-api（Trust & Safety）` |
| 109 | `gumshield-api（Trust & Safety）` | `PUT` | `/api/checklists/{id}` | `gumshield-api（Trust & Safety）` |
| 110 | `gumshield-api（Trust & Safety）` | `POST` | `/api/checklists/{type}/{attribute}` | `gumshield-api（Trust & Safety）` |
| 111 | `gumshield-api（Trust & Safety）` | `POST` | `/api/checklists/{type}/{attribute}/check` | `gumshield-api（Trust & Safety）` |
| 112 | `gumshield-api（Trust & Safety）` | `POST` | `/api/conversation/flag` | `gumshield-api（Trust & Safety）` |
| 113 | `gumshield-api（Trust & Safety）` | `POST` | `/api/cs-message-review` | `gumshield-api（Trust & Safety）` |
| 114 | `gumshield-api（Trust & Safety）` | `GET` | `/api/cs-review` | `gumshield-api（Trust & Safety）` |
| 115 | `gumshield-api（Trust & Safety）` | `POST` | `/api/cs-review` | `gumshield-api（Trust & Safety）` |
| 116 | `gumshield-api（Trust & Safety）` | `POST` | `/api/cs-review/ad-id/` | `gumshield-api（Trust & Safety）` |
| 117 | `gumshield-api（Trust & Safety）` | `POST` | `/api/cs-review/id-by-reason/` | `gumshield-api（Trust & Safety）` |
| 118 | `gumshield-api（Trust & Safety）` | `POST` | `/api/cs-review/latest/` | `gumshield-api（Trust & Safety）` |
| 119 | `gumshield-api（Trust & Safety）` | `POST` | `/api/refunds` | `gumshield-api（Trust & Safety）` |
| 120 | `gumshield-api（Trust & Safety）` | `GET` | `/api/refunds/automatic-refund` | `gumshield-api（Trust & Safety）` |
| 121 | `gumshield-api（Trust & Safety）` | `GET` | `/api/refunds/automatic-refund-requests` | `gumshield-api（Trust & Safety）` |
| 122 | `gumshield-api（Trust & Safety）` | `GET` | `/api/refunds/manual-refund-requests` | `gumshield-api（Trust & Safety）` |
| 123 | `gumshield-api（Trust & Safety）` | `GET` | `/api/refunds/search` | `gumshield-api（Trust & Safety）` |
| 124 | `gumshield-api（Trust & Safety）` | `GET` | `/api/refunds/{id}` | `gumshield-api（Trust & Safety）` |
| 125 | `gumshield-api（Trust & Safety）` | `GET` | `/api/refunds/{id}/process` | `gumshield-api（Trust & Safety）` |
| 126 | `gumshield-api（Trust & Safety）` | `GET` | `/api/refunds/{reference}` | `gumshield-api（Trust & Safety）` |
| 127 | `gumshield-api（Trust & Safety）` | `POST` | `/api/user-reports` | `gumshield-api（Trust & Safety）` |
| 128 | `gumshield-api（Trust & Safety）` | `GET` | `/api/user-reports/{id}` | `gumshield-api（Trust & Safety）` |
| 129 | `gumshield-api（Trust & Safety）` | `POST` | `/api/user-reports/{id}` | `gumshield-api（Trust & Safety）` |
| 130 | `gumshield-api（Trust & Safety）` | `GET` | `/api/users/{id}/known-good` | `gumshield-api（Trust & Safety）` |
| 131 | `gumshield-api（Trust & Safety）` | `GET` | `/api/users/{id}/known_good` | `gumshield-api（Trust & Safety）` |
| 132 | `livead-search` | `GET` | `/api/advert/{advertId}` | `livead-search` |
| 133 | `livead-search` | `GET` | `/api/category/suggest` | `livead-search` |
| 134 | `livead-search` | `POST` | `/api/keyword/correct` | `livead-search` |
| 135 | `livead-search` | `POST` | `/api/search` | `livead-search` |
| 136 | `livead-search` | `POST` | `/api/search/next-scroll` | `livead-search` |
| 137 | `livead-search` | `POST` | `/api/search/scroll` | `livead-search` |
| 138 | `livead-search` | `POST` | `/api/similar-ads` | `livead-search` |
| 139 | `fullad-search-service` | `GET` | `/api/advert/{advertId}` | `fullad-search-service` |
| 140 | `fullad-search-service` | `POST` | `/api/manage-ads` | `fullad-search-service` |
| 141 | `fullad-search-service` | `POST` | `/api/search` | `fullad-search-service` |
| 142 | `gumshield-ad-search` | `POST` | `/api/adverts/search` | `gumshield-ad-search` |
| 143 | `gumshield-ad-search` | `GET` | `/api/adverts/{advertId}` | `gumshield-ad-search` |
| 144 | `conversation-reporter` | `POST` | `/conversation-reports/` | `conversation-reporter` |
| 145 | `conversation-reporter` | `POST` | `/conversation-reports/search` | `conversation-reporter` |
| 146 | `conversation-reporter` | `POST` | `/conversation-reports/{conversation-report-id}/mark-as-reviewed` | `conversation-reporter` |
| 147 | `conversation-reporter` | `GET` | `/conversations/{conversation-id}/is-already-reported` | `conversation-reporter` |
| 148 | `conversation-reporter-papi` | `POST` | `/conversation-reports` | `conversation-reporter-papi` |
| 149 | `conversation-reporter-papi` | `GET` | `/conversations/{conversation-id}/is-already-reported` | `conversation-reporter-papi` |
| 150 | `block-user-conversations-papi` | `GET` | `/v1/block-users` | `block-user-conversations-papi` |
| 151 | `block-user-conversations-papi` | `DELETE` | `/v1/block-users/{userIdToBlock}` | `block-user-conversations-papi` |
| 152 | `block-user-conversations-papi` | `GET` | `/v1/block-users/{userIdToBlock}` | `block-user-conversations-papi` |
| 153 | `block-user-conversations-papi` | `POST` | `/v1/block-users/{userIdToBlock}` | `block-user-conversations-papi` |
| 154 | `message-moderation-history` | `POST` | `/history/automatic-moderation` | `message-moderation-history` |
| 155 | `message-moderation-history` | `DELETE` | `/history/gdpr/ddr/{email}` | `message-moderation-history` |
| 156 | `message-moderation-history` | `GET` | `/history/gdpr/sdr/{email}` | `message-moderation-history` |
| 157 | `message-moderation-history` | `POST` | `/history/manual-moderation` | `message-moderation-history` |
| 158 | `message-moderation-history` | `POST` | `/history/moderation/search` | `message-moderation-history` |
| 159 | `message-moderation-history` | `GET` | `/history/whitelisted-rules` | `message-moderation-history` |
| 160 | `category-api（Play `routes` + 契约节选）` | `GET` | `/_version` | `OpenAPI 已建模端点（节选，完整字段见契约 `components.schemas`）` |
| 161 | `category-api（Play `routes` + 契约节选）` | `GET` | `/api/categories` | `OpenAPI 已建模端点（节选，完整字段见契约 `components.schemas`）` |
| 162 | `category-api（Play `routes` + 契约节选）` | `GET` | `/api/categories/attributes` | `OpenAPI 已建模端点（节选，完整字段见契约 `components.schemas`）` |
| 163 | `pricing-api` | `GET` | `/api/pricing/{categoryId}/{locationId}` | `pricing-api` |
| 164 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/ad-rule-reports/performance-summary/{rule-name}/{days}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 165 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/ad-rule-reports/{id}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 166 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/ad-screen-items` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 167 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/ad-screen-items/advert/{advert-id}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 168 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/ipranges` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 169 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/ipranges/check/{ip}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 170 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/exclusionlist/{matchListEntryId}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 171 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/matchlist` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 172 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/matchlist-entry/{matchListId}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 173 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/matchlist-entry/{matchListId}/filter/{filterName}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 174 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/matchlist/filter/{filterName}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 175 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/matchlist/{id}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 176 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/stoplist` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 177 | `gumshield-rules（只读 GET；策略管理写入为人工作业）` | `GET` | `/api/policy/stoplist/{term}` | `gumshield-rules（只读 GET；策略管理写入为人工作业）` |
| 178 | `payment-api（Braintree — 仅退款查询相关）` | `POST` | `/api/payment/braintree/refund` | `payment-api（Braintree — 仅退款查询相关）` |
| 179 | `braze-gateway（GDPR 子集）` | `POST` | `/gdpr/ddr` | `braze-gateway（GDPR 子集）` |
| 180 | `braze-gateway（GDPR 子集）` | `DELETE` | `/gdpr/ddr/{email}` | `braze-gateway（GDPR 子集）` |
| 181 | `braze-gateway（GDPR 子集）` | `GET` | `/gdpr/sar/{email}` | `braze-gateway（GDPR 子集）` |
| 182 | `message-moderation-rules（消息审核规则）` | `GET` | `/rules/blacklist` | `message-moderation-rules（消息审核规则）` |
| 183 | `message-moderation-rules（消息审核规则）` | `POST` | `/rules/blacklist` | `message-moderation-rules（消息审核规则）` |
| 184 | `message-moderation-rules（消息审核规则）` | `DELETE` | `/rules/blacklist/{id}` | `message-moderation-rules（消息审核规则）` |
| 185 | `message-moderation-rules（消息审核规则）` | `GET` | `/rules/blacklist/{id}` | `message-moderation-rules（消息审核规则）` |
| 186 | `message-moderation-rules（消息审核规则）` | `PUT` | `/rules/blacklist/{id}` | `message-moderation-rules（消息审核规则）` |
| 187 | `message-moderation-rules（消息审核规则）` | `GET` | `/rules/regex` | `message-moderation-rules（消息审核规则）` |
| 188 | `message-moderation-rules（消息审核规则）` | `POST` | `/rules/regex` | `message-moderation-rules（消息审核规则）` |
| 189 | `message-moderation-rules（消息审核规则）` | `DELETE` | `/rules/regex/{id}` | `message-moderation-rules（消息审核规则）` |
| 190 | `message-moderation-rules（消息审核规则）` | `GET` | `/rules/regex/{id}` | `message-moderation-rules（消息审核规则）` |
| 191 | `message-moderation-rules（消息审核规则）` | `PUT` | `/rules/regex/{id}` | `message-moderation-rules（消息审核规则）` |
| 192 | `message-moderation-rules（消息审核规则）` | `GET` | `/rules/velocity` | `message-moderation-rules（消息审核规则）` |
| 193 | `message-moderation-rules（消息审核规则）` | `POST` | `/rules/velocity` | `message-moderation-rules（消息审核规则）` |
| 194 | `message-moderation-rules（消息审核规则）` | `DELETE` | `/rules/velocity/{id}` | `message-moderation-rules（消息审核规则）` |
| 195 | `message-moderation-rules（消息审核规则）` | `GET` | `/rules/velocity/{id}` | `message-moderation-rules（消息审核规则）` |
| 196 | `message-moderation-rules（消息审核规则）` | `PUT` | `/rules/velocity/{id}` | `message-moderation-rules（消息审核规则）` |

## 1. bapi-server

### 1.1 用户与账户域

#### GET `/api/accounts/public_id/{publicId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAccountByPublicId
- **路径 / 查询 / 头参数**:
  - `publicId` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — publicId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{accountId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAccount
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/api/accounts/{accountId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: updateAccount
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "accountStatus": {
      "type": "string",
      "enum": [
        "ACTIVE",
        "SUSPENDED",
        "CLOSED",
        "COLLECTION_AGENCY"
      ]
    },
    "address1": {
      "type": "string"
    },
    "address2": {
      "type": "string"
    },
    "city": {
      "type": "string"
    },
    "country": {
      "type": "string"
    },
    "county": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "forcePostAsAgency": {
      "type": "boolean"
    },
    "forcePostAsDealer": {
      "type": "boolean"
    },
    "name": {
      "type": "string"
    },
    "postcode": {
      "type": "string"
    },
    "primaryEmail": {
      "type": "string"
    },
    "pro": {
      "type": "boolean"
    },
    "vatNumber": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{accountId}/defaultImage`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getDefaultImage
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/api/accounts/{accountId}/defaultImage`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: setDefaultImage
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "imageId": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{accountId}/eligible_packages`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: hasEligiblePackages
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
  - `location_id` (**query**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — location_id
  - `category_id` (**query**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — category_id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{accountId}/packageUsages`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getPackageUsages
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
  - `packageTypeId` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — packageTypeId
  - `from` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — from
  - `to` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — to
  - `active` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — active
  - `page` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 1
}` — page
  - `batchSize` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 10
}` — batchSize
  - `includeCapabilities` (**query**), 必填=False, 类型/schema: `{
  "type": "boolean",
  "default": false
}` — includeCapabilities
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/api/accounts/{accountId}/reply_type/{replyType}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: updateReplyType
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
  - `replyType` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "enum": [
    "GUMTREE",
    "TRIFECTA"
  ]
}` — replyType
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{accountId}/sellerType/{categoryId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getSellerType
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
  - `categoryId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — categoryId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{accountId}/url_package`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: hasUrlPackage
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
  - `location_id` (**query**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — location_id
  - `category_id` (**query**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — category_id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{id}/adverts`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAdverts
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/accounts/{id}/emails`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getEmailAddressesByAccountId
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/emails/{email}/user`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.UserController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getUserByEmailAddress
- **路径 / 查询 / 头参数**:
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — email
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/users/emails/search`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.UserController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getContactEmail
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "email": {
      "type": "string"
    },
    "status": {
      "type": "string",
      "enum": [
        "DELETED",
        "VERIFICATION_SENT",
        "VERIFIED"
      ]
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/users/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.UserController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getUser
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/users/{userId}/emails`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.UserController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getContactEmails
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — userId
  - `status` (**query**), 必填=False, 类型/schema: `{
  "type": "array",
  "items": {
    "type": "string",
    "enum": [
      "DELETED",
      "VERIFICATION_SENT",
      "VERIFIED"
    ]
  },
  "enum": [
    "DELETED",
    "VERIFICATION_SENT",
    "VERIFIED"
  ]
}` — status
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/users/{userId}/emails/{email}/verification-key`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.UserController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getEmailVerificationKey
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — userId
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — email
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/users/{username}/accounts`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.UserController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAccounts
- **路径 / 查询 / 头参数**:
  - `username` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — username
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

### 1.2 广告与列表域

#### GET `/api/accounts/{id}/adverts`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AccountController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAdverts
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/attributes/{categoryId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getCoreAttributesList
- **路径 / 查询 / 头参数**:
  - `categoryId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — categoryId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/count/has_live`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: hasLiveOrPreLiveAdverts
- **路径 / 查询 / 头参数**:
  - `account_id` (**query**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — account_id
  - `category_id` (**query**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — category_id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/id/search`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: findAdvertIdsForAccount
- **路径 / 查询 / 头参数**:
  - `account_id` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}`
  - `status` (**query**), 必填=False, 类型/schema: `{
  "type": "array",
  "items": {
    "type": "string"
  }
}` — status
  - `num` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 50
}`
  - `page` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 1
}`
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/location/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAdvertLocation
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/search`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchAdverts
- **路径 / 查询 / 头参数**:
  - `account_id` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}`
  - `status` (**query**), 必填=False, 类型/schema: `{
  "type": "array",
  "items": {
    "type": "string"
  }
}` — status
  - `num` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 50
}`
  - `page` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 1
}`
  - `sort_by_status` (**query**), 必填=False, 类型/schema: `{
  "type": "boolean",
  "default": false
}` — sort_by_status
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/show/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAdvert
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/since`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: advertsSince
- **路径 / 查询 / 头参数**:
  - `date` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date
  - `date_max` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date_max
  - `num` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 50
}` — num
  - `page` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 1
}` — page
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/adverts/validate`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: validateAdvert
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "account_id": {
      "type": "integer",
      "format": "int64"
    },
    "area": {
      "type": "string"
    },
    "attributes": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "value": {
            "type": "string"
          }
        }
      }
    },
    "category": {
      "type": "string",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "category_id": {
      "type": "integer",
      "format": "int64",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "replies_email": {
      "type": "string"
    },
    "contact_name": {
      "type": "string"
    },
    "phone_number": {
      "type": "string"
    },
    "reply_link": {
      "type": "string"
    },
    "cookie": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "external_ref": {
      "type": "string"
    },
    "features": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "end_date": {
            "type": "string",
            "format": "date-time"
          },
          "product_name": {
            "type": "string",
            "enum": [
              "INSERTION",
              "BUMP_UP",
              "FEATURE_3_DAY",
              "FEATURE_7_DAY",
              "FEATURE_14_DAY",
              "HOMEPAGE_SPOTLIGHT",
              "URGENT",
              "APPVAULT_RESPONSE_MANAGER",
              "WEBSITE_URL",
              "SEARCH_STANDOUT",
              "EXTENDED_VEHICLE_HISTORY_CHECK",
              "CALL_TRACKING_ACCOUNT_LEVEL",
              "CALL_TRACKING_ACCOUNT_LEVEL_HIDDEN",
              "CALL_TRACKING_ADVERT_LEVEL",
              "CALL_TRACKING_ADVERT_LEVEL_WITH_BLACKLISTING",
              "PREMIUM_JOB",
              "EMG_FREESPEE_PERMISSION"
            ]
          }
        }
      }
    },
    "hostname": {
      "type": "string"
    },
    "images": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    },
    "ip": {
      "type": "string"
    },
    "location_id": {
      "type": "integer",
      "format": "int64"
    },
    "main_image_id": {
      "type": "integer",
      "format": "int64"
    },
    "platform": {
      "type": "string"
    },
    "postcode": {
      "type": "string"
    },
    "price": {
      "type": "object",
      "properties": {
        "amount": {
          "type": "string"
        },
        "currency": {
          "type": "string"
        }
      }
    },
    "threatmetrix_session_id": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "visible_on_map": {
      "type": "boolean"
    },
    "website_url": {
      "type": "string"
    },
    "youtube_link": {
      "type": "string"
    },
    "extend_fields": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/adverts/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteAdvert
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAdvert
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/api/adverts/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: updateAdvert
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "account_id": {
      "type": "integer",
      "format": "int64"
    },
    "area": {
      "type": "string"
    },
    "attributes": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "value": {
            "type": "string"
          }
        }
      }
    },
    "category": {
      "type": "string",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "category_id": {
      "type": "integer",
      "format": "int64",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "replies_email": {
      "type": "string"
    },
    "contact_name": {
      "type": "string"
    },
    "phone_number": {
      "type": "string"
    },
    "reply_link": {
      "type": "string"
    },
    "cookie": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "external_ref": {
      "type": "string"
    },
    "features": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "end_date": {
            "type": "string",
            "format": "date-time"
          },
          "product_name": {
            "type": "string",
            "enum": [
              "INSERTION",
              "BUMP_UP",
              "FEATURE_3_DAY",
              "FEATURE_7_DAY",
              "FEATURE_14_DAY",
              "HOMEPAGE_SPOTLIGHT",
              "URGENT",
              "APPVAULT_RESPONSE_MANAGER",
              "WEBSITE_URL",
              "SEARCH_STANDOUT",
              "EXTENDED_VEHICLE_HISTORY_CHECK",
              "CALL_TRACKING_ACCOUNT_LEVEL",
              "CALL_TRACKING_ACCOUNT_LEVEL_HIDDEN",
              "CALL_TRACKING_ADVERT_LEVEL",
              "CALL_TRACKING_ADVERT_LEVEL_WITH_BLACKLISTING",
              "PREMIUM_JOB",
              "EMG_FREESPEE_PERMISSION"
            ]
          }
        }
      }
    },
    "hostname": {
      "type": "string"
    },
    "images": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    },
    "ip": {
      "type": "string"
    },
    "location_id": {
      "type": "integer",
      "format": "int64"
    },
    "main_image_id": {
      "type": "integer",
      "format": "int64"
    },
    "platform": {
      "type": "string"
    },
    "postcode": {
      "type": "string"
    },
    "price": {
      "type": "object",
      "properties": {
        "amount": {
          "type": "string"
        },
        "currency": {
          "type": "string"
        }
      }
    },
    "threatmetrix_session_id": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "visible_on_map": {
      "type": "boolean"
    },
    "website_url": {
      "type": "string"
    },
    "youtube_link": {
      "type": "string"
    },
    "extend_fields": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/adverts/{id}/bumpup`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: Bumps up advert without creating an order
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — Advert ID
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/adverts/{id}/checkAndArchive`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: archiveAdvert
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/adverts/{id}/deleteReason`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: newDeleteReason
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "string",
  "enum": [
    "GT_SUCCESS_YES",
    "GT_SUCCESS_NO",
    "GT_SUCCESS_NT",
    "SOLD"
  ]
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{id}/features`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getFeatures
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/adverts/{id}/statuses`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: newAdvertStatus
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — id
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "status": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/advert/{advertId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchByAdvertId
- **路径 / 查询 / 头参数**:
  - `advertId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — advertId
  - `date` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date
  - `date_max` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date_max
  - `num` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 50
}` — num
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/advert/{advertId}/paid`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchPaidProductNamesByAccountId
- **路径 / 查询 / 头参数**:
  - `advertId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — advertId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/price/advert/{aId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getPricesByAdId
- **路径 / 查询 / 头参数**:
  - `aId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — aId
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "account_id": {
      "type": "integer",
      "format": "int64"
    },
    "area": {
      "type": "string"
    },
    "attributes": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "value": {
            "type": "string"
          }
        }
      }
    },
    "category": {
      "type": "string",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "category_id": {
      "type": "integer",
      "format": "int64",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "replies_email": {
      "type": "string"
    },
    "contact_name": {
      "type": "string"
    },
    "phone_number": {
      "type": "string"
    },
    "reply_link": {
      "type": "string"
    },
    "cookie": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "external_ref": {
      "type": "string"
    },
    "features": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "end_date": {
            "type": "string",
            "format": "date-time"
          },
          "product_name": {
            "type": "string",
            "enum": [
              "INSERTION",
              "BUMP_UP",
              "FEATURE_3_DAY",
              "FEATURE_7_DAY",
              "FEATURE_14_DAY",
              "HOMEPAGE_SPOTLIGHT",
              "URGENT",
              "APPVAULT_RESPONSE_MANAGER",
              "WEBSITE_URL",
              "SEARCH_STANDOUT",
              "EXTENDED_VEHICLE_HISTORY_CHECK",
              "CALL_TRACKING_ACCOUNT_LEVEL",
              "CALL_TRACKING_ACCOUNT_LEVEL_HIDDEN",
              "CALL_TRACKING_ADVERT_LEVEL",
              "CALL_TRACKING_ADVERT_LEVEL_WITH_BLACKLISTING",
              "PREMIUM_JOB",
              "EMG_FREESPEE_PERMISSION"
            ]
          }
        }
      }
    },
    "hostname": {
      "type": "string"
    },
    "images": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    },
    "ip": {
      "type": "string"
    },
    "location_id": {
      "type": "integer",
      "format": "int64"
    },
    "main_image_id": {
      "type": "integer",
      "format": "int64"
    },
    "platform": {
      "type": "string"
    },
    "postcode": {
      "type": "string"
    },
    "price": {
      "type": "object",
      "properties": {
        "amount": {
          "type": "string"
        },
        "currency": {
          "type": "string"
        }
      }
    },
    "threatmetrix_session_id": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "visible_on_map": {
      "type": "boolean"
    },
    "website_url": {
      "type": "string"
    },
    "youtube_link": {
      "type": "string"
    },
    "extend_fields": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

### 1.3 客服广告操作（Takedown / Publish / 删除）

#### POST `/api/cs/adverts/auto_delete`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: autoDelete
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "lastModifiedDate": {
      "type": "string",
      "format": "date-time"
    },
    "reason": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs/adverts/auto_publish`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: autoPublish
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "lastModifiedDate": {
      "type": "string",
      "format": "date-time"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs/adverts/auto_reject`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: autoReject
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "lastModifiedDate": {
      "type": "string",
      "format": "date-time"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs/adverts/delete_by_user`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteByUser
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "reason": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs/adverts/delete_by_user_email`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteByUser
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "email": {
      "type": "string"
    },
    "reason": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs/adverts/publish`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: publish
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "ids": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    },
    "lastModifiedDates": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "mode": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs/adverts/takedown`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: takedown
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "reason": {
        "type": "string"
      },
      "note": {
        "type": "string"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs/adverts/takedown_notification`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.cs.controller.CSAdvertController（及旧版 `/api/advert/*` 映射）`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: takedownNotification
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "advertId": {
      "type": "integer",
      "format": "int64"
    },
    "accountId": {
      "type": "integer",
      "format": "int64"
    },
    "reason": {
      "type": "string"
    },
    "note": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

### 1.4 GDPR 域

#### POST `/api/gdpr`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: sar
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "password": {
      "type": "string"
    },
    "userIdentifier": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/gdpr/account`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteGdprAccountData
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "password": {
      "type": "string"
    },
    "userIdentifier": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/gdpr/account/delete-advert-related-data`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteAdvertRelatedData
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "password": {
      "type": "string"
    },
    "advertIds": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/gdpr/account/deletion-allowed`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: isDeletionAllowed
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "userIdentifier": {
      "type": "string"
    },
    "ddr": {
      "type": "boolean"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/gdpr/anyAdsPostingSince/{accountId}/{sinceDateTime}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAnyAdsPostingSince
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — account id
  - `sinceDateTime` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — since date time in milliseconds
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/gdpr/dormantAccount`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteDormantAccount
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "password": {
      "type": "string"
    },
    "userId": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/gdpr/dormantUser/{userId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteDormantUser
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — user id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/gdpr/email`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteUserEmail
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "password": {
      "type": "string"
    },
    "userIdentifier": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/gdpr/user/{userId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.GdprController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: deleteGdprUserData
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — user id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

### 1.5 订单、支付（Bapi 侧）与退款辅助

#### GET `/api/orders`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getOrders
- **路径 / 查询 / 头参数**:
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/orders`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: createOrder
- **路径 / 查询 / 头参数**:
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "accountId": {
      "type": "integer",
      "format": "int64"
    },
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "advertId": {
            "type": "integer",
            "format": "int64"
          },
          "productName": {
            "type": "string",
            "enum": [
              "INSERTION",
              "BUMP_UP",
              "FEATURE_3_DAY",
              "FEATURE_7_DAY",
              "FEATURE_14_DAY",
              "HOMEPAGE_SPOTLIGHT",
              "URGENT",
              "APPVAULT_RESPONSE_MANAGER",
              "WEBSITE_URL",
              "SEARCH_STANDOUT",
              "EXTENDED_VEHICLE_HISTORY_CHECK",
              "CALL_TRACKING_ACCOUNT_LEVEL",
              "CALL_TRACKING_ACCOUNT_LEVEL_HIDDEN",
              "CALL_TRACKING_ADVERT_LEVEL",
              "CALL_TRACKING_ADVERT_LEVEL_WITH_BLACKLISTING",
              "PREMIUM_JOB",
              "EMG_FREESPEE_PERMISSION"
            ]
          }
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/orders/email`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: sendOrderInvoice
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "advertId": {
      "type": "integer",
      "format": "int64"
    },
    "email": {
      "type": "string"
    },
    "orderId": {
      "type": "integer",
      "format": "int64"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/orders/refund`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: refundOrderItems
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "amount": {
      "type": "integer",
      "format": "int64"
    },
    "orderItemsIds": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    },
    "paypalRef": {
      "type": "string"
    },
    "reason": {
      "type": "string"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/account/{accountId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchByAccountId
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — accountId
  - `date` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date
  - `date_max` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date_max
  - `num` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 50
}` — num
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/advert/{advertId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchByAdvertId
- **路径 / 查询 / 头参数**:
  - `advertId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — advertId
  - `date` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date
  - `date_max` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date_max
  - `num` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 50
}` — num
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/advert/{advertId}/paid`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchPaidProductNamesByAccountId
- **路径 / 查询 / 头参数**:
  - `advertId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — advertId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/email/{email}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchByEmail
- **路径 / 查询 / 头参数**:
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — email
  - `date` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date
  - `date_max` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — date_max
  - `num` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 50
}` — num
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/id/{paymentId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchById
- **路径 / 查询 / 头参数**:
  - `paymentId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — paymentId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/search/paypalref/{paypalRef}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: searchByPaypalRef
- **路径 / 查询 / 头参数**:
  - `paypalRef` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — paypalRef
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getOrder
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/api/orders/{id}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: expireOrder
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/{id}/custom_payment_url`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getCustomPaymentUrl
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
  - `returnUrl` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}` — returnUrl
  - `cancelReturnUrl` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}` — cancelReturnUrl
  - `userAgent` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}` — userAgent
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/orders/{id}/payment_url`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getPaymentUrl
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/orders/{id}/transaction`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.OrderController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: executeTransaction
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/refund/addRefundIfNew`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.RefundsController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: addRefundIfNew
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "array",
  "items": {
    "type": "string"
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/refund/cancelPayment`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.RefundsController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: cancelPayment
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "array",
  "items": {
    "type": "string"
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refund/transactions/refunded`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.RefundsController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getTransactionsRefundedInLastMinutes
- **路径 / 查询 / 头参数**:
  - `max_age` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 60
}` — max_age
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refund/transactions/voided`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.RefundsController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getTransactionsVoidedInLastMinutes
- **路径 / 查询 / 头参数**:
  - `max_age` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int32",
  "default": 60
}` — max_age
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

### 1.6 定价与类目属性（Bapi 代理/遗留面）

#### GET `/api/adverts/attributes/{categoryId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getCoreAttributesList
- **路径 / 查询 / 头参数**:
  - `categoryId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — categoryId
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/attributes`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AttributeController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getAllAttributes
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/price/advert`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.PriceController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getPrices
- **路径 / 查询 / 头参数**:
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "account_id": {
      "type": "integer",
      "format": "int64"
    },
    "area": {
      "type": "string"
    },
    "attributes": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "value": {
            "type": "string"
          }
        }
      }
    },
    "category": {
      "type": "string",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "category_id": {
      "type": "integer",
      "format": "int64",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "replies_email": {
      "type": "string"
    },
    "contact_name": {
      "type": "string"
    },
    "phone_number": {
      "type": "string"
    },
    "reply_link": {
      "type": "string"
    },
    "cookie": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "external_ref": {
      "type": "string"
    },
    "features": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "end_date": {
            "type": "string",
            "format": "date-time"
          },
          "product_name": {
            "type": "string",
            "enum": [
              "INSERTION",
              "BUMP_UP",
              "FEATURE_3_DAY",
              "FEATURE_7_DAY",
              "FEATURE_14_DAY",
              "HOMEPAGE_SPOTLIGHT",
              "URGENT",
              "APPVAULT_RESPONSE_MANAGER",
              "WEBSITE_URL",
              "SEARCH_STANDOUT",
              "EXTENDED_VEHICLE_HISTORY_CHECK",
              "CALL_TRACKING_ACCOUNT_LEVEL",
              "CALL_TRACKING_ACCOUNT_LEVEL_HIDDEN",
              "CALL_TRACKING_ADVERT_LEVEL",
              "CALL_TRACKING_ADVERT_LEVEL_WITH_BLACKLISTING",
              "PREMIUM_JOB",
              "EMG_FREESPEE_PERMISSION"
            ]
          }
        }
      }
    },
    "hostname": {
      "type": "string"
    },
    "images": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    },
    "ip": {
      "type": "string"
    },
    "location_id": {
      "type": "integer",
      "format": "int64"
    },
    "main_image_id": {
      "type": "integer",
      "format": "int64"
    },
    "platform": {
      "type": "string"
    },
    "postcode": {
      "type": "string"
    },
    "price": {
      "type": "object",
      "properties": {
        "amount": {
          "type": "string"
        },
        "currency": {
          "type": "string"
        }
      }
    },
    "threatmetrix_session_id": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "visible_on_map": {
      "type": "boolean"
    },
    "website_url": {
      "type": "string"
    },
    "youtube_link": {
      "type": "string"
    },
    "extend_fields": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/price/advert/{aId}`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.api.controller.AdvertController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml`
- **功能描述**: getPricesByAdId
- **路径 / 查询 / 头参数**:
  - `aId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — aId
  - `apiKey` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "format": "int64"
    },
    "account_id": {
      "type": "integer",
      "format": "int64"
    },
    "area": {
      "type": "string"
    },
    "attributes": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "value": {
            "type": "string"
          }
        }
      }
    },
    "category": {
      "type": "string",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "category_id": {
      "type": "integer",
      "format": "int64",
      "description": "PostAdvertBean is used in multiple places including AdvertController which accepts 'category_id' and EmgAdvertController which accepts 'category'. Be careful when setting values"
    },
    "replies_email": {
      "type": "string"
    },
    "contact_name": {
      "type": "string"
    },
    "phone_number": {
      "type": "string"
    },
    "reply_link": {
      "type": "string"
    },
    "cookie": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "external_ref": {
      "type": "string"
    },
    "features": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "end_date": {
            "type": "string",
            "format": "date-time"
          },
          "product_name": {
            "type": "string",
            "enum": [
              "INSERTION",
              "BUMP_UP",
              "FEATURE_3_DAY",
              "FEATURE_7_DAY",
              "FEATURE_14_DAY",
              "HOMEPAGE_SPOTLIGHT",
              "URGENT",
              "APPVAULT_RESPONSE_MANAGER",
              "WEBSITE_URL",
              "SEARCH_STANDOUT",
              "EXTENDED_VEHICLE_HISTORY_CHECK",
              "CALL_TRACKING_ACCOUNT_LEVEL",
              "CALL_TRACKING_ACCOUNT_LEVEL_HIDDEN",
              "CALL_TRACKING_ADVERT_LEVEL",
              "CALL_TRACKING_ADVERT_LEVEL_WITH_BLACKLISTING",
              "PREMIUM_JOB",
              "EMG_FREESPEE_PERMISSION"
            ]
          }
        }
      }
    },
    "hostname": {
      "type": "string"
    },
    "images": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      }
    },
    "ip": {
      "type": "string"
    },
    "location_id": {
      "type": "integer",
      "format": "int64"
    },
    "main_image_id": {
      "type": "integer",
      "format": "int64"
    },
    "platform": {
      "type": "string"
    },
    "postcode": {
      "type": "string"
    },
    "price": {
      "type": "object",
      "properties": {
        "amount": {
          "type": "string"
        },
        "currency": {
          "type": "string"
        }
      }
    },
    "threatmetrix_session_id": {
      "type": "string"
    },
    "title": {
      "type": "string"
    },
    "visible_on_map": {
      "type": "boolean"
    },
    "website_url": {
      "type": "string"
    },
    "youtube_link": {
      "type": "string"
    },
    "extend_fields": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

### 1.7 站内信 / Reply（契约未发布，源码为准）

#### POST `/api/reply`
- **服务**: bapi-server
- **Servlet / 前缀**: `/api`
- **控制器**: `com.gumtree.api.controller.ReplyController`
- **OpenAPI 契约**: `bapi-server/contract/contract.yaml` 中相关段落被注释；以源码为准。
- **功能描述**: 买家向广告发帖人发送回复（JSON 或多部分表单带附件）。
- **Consumes**:
  - `application/json` → `@RequestBody ReplyBean`
  - `multipart/form-data` → `@RequestPart("replyBean")` + `@RequestPart("file")`（附件）
- **请求体字段（ReplyBean，JSON 部分）** — 校验注解摘自源码：
| 字段 | Java 类型 | 必填 | 说明 |
|------|------------|------|------|
| senderName | String | 是 | 非 HTML，长度上下限见 `ReplyValidationConstants` |
| senderEmail | String | 是 | 合法邮箱，长度限制 |
| senderTelephone | String | 否 | 非邮箱、非 HTML，最大长度受限 |
| message | String | 是 | 非 HTML，长度上下限 |
| advertId | Long | 是 | 被回复的广告 ID |
| senderIp | String | 是 | 合法 IPv4/IPv6 格式（`ValidationConstants.IP_ADDRESS_REGEX`） |
| senderCookie | String | 是 | 发送方 Cookie 标识 |
| clientId | String | 否 | 客户端跟踪 |
| optInMarketing | boolean | 否 | 默认 false |
| filename | String | 否 | 附件文件名提示 |
| senderUserId | Long | 否 | 若已知登录用户 |
| replyType | ReplyType | 否 | 默认 `MESSAGE_REPLY` |
- **响应**: HTTP `200`，无响应体（`void`）。
- **错误**: 校验失败、广告不存在、下游 `ReplyService` 异常等抛出由 `BaseExceptionHandlingController` 映射的 API 错误。

#### POST `/api/reply/validate`
- **控制器**: `ReplyController`
- **功能描述**: 与 `/api/reply` 相同字段校验但不真正发送（`validate(..., true)`）。
- **请求格式**: 与 POST `/api/reply` 相同（JSON 或 multipart）。
- **响应**: HTTP `200` 无正文。

## 2. user-service

#### GET `/accounts`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.AccountController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: getAccountByPublicId
- **路径 / 查询 / 头参数**:
  - `publicId` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}` — public id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/accounts/{accountId}`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.AccountController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: getAccountById
- **路径 / 查询 / 头参数**:
  - `accountId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — account id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/user/id`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.UserController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: getUserIdByEmailAddress
- **路径 / 查询 / 头参数**:
  - `email` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}` — email
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/users/gdpr/ddr/{userId}`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.GdprController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: ddrUserDelete
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — user id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/users/gdpr/dormant/{userId}`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.GdprController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: dormantUserDelete
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — user id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/users/last-logged-in/{userId}`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.UserController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: getUserLastLoggedInDateTime
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — user id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/users/{email}/user`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.UserController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: getUserByEmailAddress
- **路径 / 查询 / 头参数**:
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — user email address
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/users/{userId}`
- **服务**: user-service
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `com.gumtree.user.mvc.controller.UserController`
- **OpenAPI 契约**: `user-service/contract/contract.yaml`
- **功能描述**: getUserById
- **路径 / 查询 / 头参数**:
  - `userId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — user id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 3. advert-service

#### GET `/api/adverts/by-advert-id/{advert-id}`
- **服务**: advert-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.ad.controller.AdvertController`
- **OpenAPI 契约**: `advert-service/contract/contract.yaml`
- **功能描述**: Get advert by advert ID (cross-shard query)
- **路径 / 查询 / 头参数**:
  - `advert-id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}`
  - `data-type` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — Data type for custom fields
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/adverts/query`
- **服务**: advert-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.ad.controller.AdvertController`
- **OpenAPI 契约**: `advert-service/contract/contract.yaml`
- **功能描述**: Query adverts by conditions
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "required": [
    "accountId"
  ],
  "properties": {
    "accountId": {
      "type": "integer",
      "format": "int64",
      "description": "Account ID (required for partition routing)",
      "example": 123456
    },
    "busId": {
      "type": "integer",
      "description": "Business ID filter",
      "example": 1
    },
    "advertIds": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      },
      "description": "List of advert IDs to filter",
      "example": [
        789012,
        789013
      ]
    },
    "country": {
      "type": "string",
      "description": "Country filter",
      "example": "UK"
    },
    "firstCategory": {
      "type": "integer",
      "description": "First category filter (cates[0])",
      "example": 1000
    },
    "categoryIds": {
      "type": "array",
      "items": {
        "type": "integer",
        "format": "int64"
      },
      "description": "List of category IDs to filter",
      "example": [
        1001,
        1002
      ]
    },
    "postcodeId": {
      "type": "integer",
      "description": "Postcode ID filter",
      "example": 111
    },
    "createDateStart": {
      "$ref": "#/components/schemas/DateCondition",
      "description": "Creation date start filter (query greater than this date)"
    },
    "createDateEnd": {
      "$ref": "#/components/schemas/DateCondition",
      "description": "Creation date end filter (query less than this date)"
    },
    "statusList": {
      "type": "array",
      "items": {
        "type": "integer"
      },
      "description": "List of status values to filter. See AdvertStatus enum for valid values.",
      "example": [
        2,
        20
      ]
    },
    "referenceId": {
      "type": "string",
      "description": "External reference ID filter",
      "example": "EXT-REF-001"
    },
    "fingerprint": {
      "type": "string",
      "description": "Content fingerprint filter",
      "example": "SGVsbG8gV29ybGQh"
    },
    "pageNum": {
      "type": "integer",
      "description": "Page number (starts from 1)",
      "minimum": 1,
      "example": 1
    },
    "pageSize": {
      "type": "integer",
      "description": "Number of items per page (default 50, max 100)",
      "minimum": 1,
      "maximum": 50,
      "default": 30,
      "example": 30
    },
    "orderByList": {
      "type": "array",
      "description": "Order by field list (supports multiple fields for combined sorting, max 5)",
      "maxItems": 5,
      "items": {
        "type": "object",
        "description": "Order by field definition for sorting",
        "required": [
          "field"
        ],
        "properties": {
          "field": {
            "$ref": "#/components/schemas/OrderByField",
            "description": "Field name to sort by"
          },
          "direction": {
            "$ref": "#/components/schemas/OrderDirection",
            "description": "Sort direction (asc)"
          }
        },
        "example": {
          "field": "advert_id",
          "direction": "asc"
        }
      },
      "example": [
        {
          "field": "advert_id",
          "direction": "asc"
        },
        {
          "field": "last_modified_date",
          "direction": "desc"
        }
      ]
    },
    "cursor": {
      "type": "integer",
      "format": "int64",
      "description": "Cursor for cursor-based pagination (advertId)",
      "example": 789012
    },
    "dataType": {
      "type": "string",
      "description": "Data type for custom fields",
      "example": "default"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/search`
- **服务**: advert-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.ad.controller.AdvertController`
- **OpenAPI 契约**: `advert-service/contract/contract.yaml`
- **功能描述**: Search adverts by account ID, created by, and reference ID
- **路径 / 查询 / 头参数**:
  - `account-id` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}`
  - `created-by` (**query**), 必填=False, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}`
  - `reference-id` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}`
  - `data-type` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — Data type for custom fields
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{account-id}`
- **服务**: advert-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.ad.controller.AdvertController`
- **OpenAPI 契约**: `advert-service/contract/contract.yaml`
- **功能描述**: Get adverts by account ID
- **路径 / 查询 / 头参数**:
  - `account-id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}`
  - `data-type` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — Data type for custom fields
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{account-id}/{advert-id}`
- **服务**: advert-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `com.gumtree.ad.controller.AdvertController`
- **OpenAPI 契约**: `advert-service/contract/contract.yaml`
- **功能描述**: Get advert details by account ID and advert ID
- **路径 / 查询 / 头参数**:
  - `account-id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64",
  "example": 123456
}` — Account ID of the advert owner
  - `advert-id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64",
  "example": 789012
}` — Advert ID
  - `data-type` (**query**), 必填=False, 类型/schema: `{
  "type": "string"
}` — Data type for custom fields
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 4. gumshield-api（Trust & Safety）

#### POST `/api/adverts/status`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: recordStatus
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/AdStatusRequest",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/status/{id}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getStatusHistory
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{id}/known-good`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getKnownGood
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{id}/known_good`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getKnownGood
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{id}/pro_account`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: isProAccount
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/blacklists/email`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: deleteBlacklistEmailEntry
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCheckValue",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/blacklists/email`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: createBlacklistEmailEntry
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCheckValue",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/blacklists/email/check`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: checkBlacklistEmailEntry
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCheckValue",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/checklists`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: createEntry
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ChecklistEntryRequest",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/checklists/batch-check`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: batchCheck
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ChecklistBatchCheckRequest",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/checklists/q/{type}/{attribute}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: deleteByQuery
- **路径 / 查询 / 头参数**:
  - `type` (**path**), 必填=True, 类型/schema: `null` — type
  - `attribute` (**path**), 必填=True, 类型/schema: `null` — attribute
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCheckValue",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/checklists/q/{type}/{attribute}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: search
- **路径 / 查询 / 头参数**:
  - `type` (**path**), 必填=True, 类型/schema: `null` — type
  - `attribute` (**path**), 必填=True, 类型/schema: `null` — attribute
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCheckValue",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/checklists/q/{type}/{attribute}/{query}/endswith`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: endsWithSearch
- **路径 / 查询 / 头参数**:
  - `type` (**path**), 必填=True, 类型/schema: `null` — type
  - `attribute` (**path**), 必填=True, 类型/schema: `null` — attribute
  - `query` (**path**), 必填=True, 类型/schema: `null` — query
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/api/checklists/{id}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: deleteEntry
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/checklists/{id}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: findEntryById
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/api/checklists/{id}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: updateEntry
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ChecklistEntryRequest",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/checklists/{type}/{attribute}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: findEntryByValue
- **路径 / 查询 / 头参数**:
  - `type` (**path**), 必填=True, 类型/schema: `null` — type
  - `attribute` (**path**), 必填=True, 类型/schema: `null` — attribute
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCheckValue",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/checklists/{type}/{attribute}/check`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: checkEntryByValue
- **路径 / 查询 / 头参数**:
  - `type` (**path**), 必填=True, 类型/schema: `null` — type
  - `attribute` (**path**), 必填=True, 类型/schema: `null` — attribute
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCheckValue",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/conversation/flag`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: flagConversation
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiFlaggedConversation",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs-message-review`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: save
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCsMessageReview",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/cs-review`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getReviewsForAdverts
- **路径 / 查询 / 头参数**:
  - `id` (**query**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs-review`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: insertReview
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCsReview",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs-review/ad-id/`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getCsReviewsOfAdverts
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCsReviewRequestQuery",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs-review/id-by-reason/`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getCsReviewAdIdsFilterByReason
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCsReviewRequestQuery",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/cs-review/latest/`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getLatestReviews
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiCsReviewRequestQuery",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/refunds`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: createRefundRequest
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/RefundRequest",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refunds/automatic-refund`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: automaticRefundAdvert
- **路径 / 查询 / 头参数**:
  - `advert_id` (**query**), 必填=True, 类型/schema: `null` — advert_id
  - `reason` (**query**), 必填=True, 类型/schema: `null` — reason
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refunds/automatic-refund-requests`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: automaticRefundRequests
- **路径 / 查询 / 头参数**:
  - `advert_id` (**query**), 必填=True, 类型/schema: `null` — advert_id
  - `reason` (**query**), 必填=True, 类型/schema: `null` — reason
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refunds/manual-refund-requests`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: manualRefundRequests
- **路径 / 查询 / 头参数**:
  - `advert_id` (**query**), 必填=True, 类型/schema: `null` — advert_id
  - `reason` (**query**), 必填=True, 类型/schema: `null` — reason
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refunds/search`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: searchRefunds
- **路径 / 查询 / 头参数**:
  - `reference` (**query**), 必填=True, 类型/schema: `null` — reference
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refunds/{id}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: findById
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refunds/{id}/process`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: processRefund
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/refunds/{reference}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: findByReference
- **路径 / 查询 / 头参数**:
  - `reference` (**path**), 必填=True, 类型/schema: `null` — reference
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/user-reports`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: insertReport
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/ApiUserReport",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/user-reports/{id}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getUserReports
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/user-reports/{id}`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: updateUserReports
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "$ref": "#/definitions/UserReportUpdateRequest",
  "_unresolved": true
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/users/{id}/known-good`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getKnownGood
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/users/{id}/known_good`
- **服务**: gumshield-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `见 gumshield-api 各 `*Controller`（契约 tags 指示服务域）`
- **OpenAPI 契约**: `gumshield-api-client/gumshield-api-contract/src/main/resources/gumshield-api.json`
- **功能描述**: getKnownGood
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 5. livead-search

#### GET `/api/advert/{advertId}`
- **服务**: livead-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `livead-search Web 控制器（契约 operationId: search / getAdvertById 等）`
- **OpenAPI 契约**: `livead-search/contract/src/main/resources/livead-search-contract.yaml`
- **功能描述**: Get Advert by ID
- **路径 / 查询 / 头参数**:
  - `advertId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — Advert ID
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/category/suggest`
- **服务**: livead-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `livead-search Web 控制器（契约 operationId: search / getAdvertById 等）`
- **OpenAPI 契约**: `livead-search/contract/src/main/resources/livead-search-contract.yaml`
- **功能描述**: Get Category Suggestions for search term
- **路径 / 查询 / 头参数**:
  - `search` (**query**), 必填=True, 类型/schema: `{
  "type": "string"
}` — Search Term
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/keyword/correct`
- **服务**: livead-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `livead-search Web 控制器（契约 operationId: search / getAdvertById 等）`
- **OpenAPI 契约**: `livead-search/contract/src/main/resources/livead-search-contract.yaml`
- **功能描述**: Correct Keyword
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "required": [
    "keyword"
  ],
  "properties": {
    "keyword": {
      "type": "string",
      "example": "mapbook air",
      "description": "The keyword that needs to be corrected"
    },
    "preTag": {
      "type": "string",
      "example": "<span>",
      "description": "Html tag that is added before corrected token in a keyword. Default is \\<span>"
    },
    "postTag": {
      "type": "string",
      "example": "</span>",
      "description": "Html tag is added after corrected token in a keyword. Default is \\</span>"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/search`
- **服务**: livead-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `livead-search Web 控制器（契约 operationId: search / getAdvertById 等）`
- **OpenAPI 契约**: `livead-search/contract/src/main/resources/livead-search-contract.yaml`
- **功能描述**: Advert Search
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "instructions": {
      "type": "array",
      "items": {
        "type": "object",
        "maxProperties": 1,
        "properties": {
          "q": {
            "type": "object",
            "required": [
              "keywords"
            ],
            "properties": {
              "keywords": {
                "type": "array",
                "items": {
                  "type": "string"
                },
                "example": [
                  "golf",
                  "golf"
                ],
                "description": "keywords"
              },
              "extended": {
                "type": "boolean",
                "example": false,
                "description": "extended"
              }
            }
          },
          "eq": {
            "type": "object",
            "required": [
              "field",
              "value"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "value": {
                "type": "string"
              },
              "unit": {
                "type": "string",
                "enum": [
                  "PENCE",
                  "POUNCE"
                ]
              }
            }
          },
          "range": {
            "type": "object",
            "required": [
              "field"
            ],
            "minProperties": 2,
            "properties": {
              "field": {
                "type": "string"
              },
              "gte": {
                "type": "integer",
                "format": "int64"
              },
              "lt": {
                "type": "integer",
                "format": "int64"
              },
              "lte": {
                "type": "integer",
                "format": "int64"
              },
              "unit": {
                "type": "string",
                "enum": [
                  "PENCE",
                  "POUNCE"
                ]
              },
              "period": {
                "type": "string",
                "enum": [
                  "PER_WEEK",
                  "PER_MONTH"
                ]
              }
            }
          },
          "anyOf": {
            "type": "object",
            "required": [
              "field",
              "values"
            ],
            "properties": {
              "field": {
                "type": "string",
                "description": "Field name as defined in SearchableField or attribute name"
              },
              "values": {
                "type": "array",
                "items": {
                  "type": "object"
                },
                "description": "List of values for the field"
              }
            }
          },
          "multiAnyOf": {
            "type": "object",
            "required": [
              "fieldValues"
            ],
            "properties": {
              "fieldValues": {
                "type": "object",
                "additionalProperties": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "description": "Map of field names to list of values"
              }
            }
          },
          "geo": {
            "type": "object",
            "required": [
              "latitude",
              "longitude",
              "maxDistance"
            ],
            "properties": {
              "latitude": {
                "type": "number",
                "format": "double"
              },
              "longitude": {
                "type": "number",
                "format": "double"
              },
              "minDistance": {
                "type": "number",
                "format": "double"
              },
              "maxDistance": {
                "type": "number",
                "format": "double"
              },
              "calDistance": {
                "type": "boolean"
              }
            }
          },
          "not": {
            "type": "object",
            "required": [
              "instruction"
            ],
            "properties": {
              "instruction": {
                "$ref": "#/components/schemas/SearchInstruction",
                "_circular": true
              }
            }
          },
          "has": {
            "description": "Supported fields: [image]",
            "type": "object",
            "required": [
              "field"
            ],
            "properties": {
              "field": {
                "type": "string"
              }
            }
          }
        }
      }
    },
    "sort": {
      "type": "array",
      "items": {
        "type": "object",
        "maxProperties": 1,
        "properties": {
          "field": {
            "type": "object",
            "required": [
              "field",
              "order"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              },
              "missingFirst": {
                "type": "boolean"
              }
            }
          },
          "random": {
            "type": "object",
            "properties": {}
          },
          "distance": {
            "type": "object",
            "required": [
              "order"
            ],
            "properties": {
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              }
            }
          },
          "score": {
            "type": "object",
            "properties": {}
          },
          "doc": {
            "type": "object",
            "properties": {}
          },
          "delta": {
            "type": "object",
            "required": [
              "field",
              "value",
              "order"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              },
              "value": {
                "type": "integer",
                "format": "int64"
              }
            }
          }
        }
      }
    },
    "aggregations": {
      "type": "array",
      "items": {
        "type": "object",
        "required": [
          "name",
          "field"
        ],
        "properties": {
          "name": {
            "type": "string"
          },
          "field": {
            "type": "string",
            "description": "Field name as defined in SearchableField or attribute name"
          },
          "values": {
            "type": "array",
            "description": "List of exact values to include as buckets. If omitted for attributes list of supported search values defined in AttributeMetadata model will be used.",
            "items": {
              "type": "object"
            }
          },
          "size": {
            "type": "integer",
            "description": "Number of buckets."
          }
        }
      }
    },
    "fetchFields": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "offset": {
      "type": "integer"
    },
    "limit": {
      "type": "integer"
    },
    "activeLabsExperiments": {
      "type": "object",
      "additionalProperties": {
        "type": "string"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/search/next-scroll`
- **服务**: livead-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `livead-search Web 控制器（契约 operationId: search / getAdvertById 等）`
- **OpenAPI 契约**: `livead-search/contract/src/main/resources/livead-search-contract.yaml`
- **功能描述**: Get Next Advert Scroll Search
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "scrollId": {
      "type": "string"
    },
    "timeoutInMins": {
      "type": "integer"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/search/scroll`
- **服务**: livead-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `livead-search Web 控制器（契约 operationId: search / getAdvertById 等）`
- **OpenAPI 契约**: `livead-search/contract/src/main/resources/livead-search-contract.yaml`
- **功能描述**: Advert Scroll Search
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "instructions": {
      "type": "array",
      "items": {
        "type": "object",
        "maxProperties": 1,
        "properties": {
          "q": {
            "type": "object",
            "required": [
              "keywords"
            ],
            "properties": {
              "keywords": {
                "type": "array",
                "items": {
                  "type": "string"
                },
                "example": [
                  "golf",
                  "golf"
                ],
                "description": "keywords"
              },
              "extended": {
                "type": "boolean",
                "example": false,
                "description": "extended"
              }
            }
          },
          "eq": {
            "type": "object",
            "required": [
              "field",
              "value"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "value": {
                "type": "string"
              },
              "unit": {
                "type": "string",
                "enum": [
                  "PENCE",
                  "POUNCE"
                ]
              }
            }
          },
          "range": {
            "type": "object",
            "required": [
              "field"
            ],
            "minProperties": 2,
            "properties": {
              "field": {
                "type": "string"
              },
              "gte": {
                "type": "integer",
                "format": "int64"
              },
              "lt": {
                "type": "integer",
                "format": "int64"
              },
              "lte": {
                "type": "integer",
                "format": "int64"
              },
              "unit": {
                "type": "string",
                "enum": [
                  "PENCE",
                  "POUNCE"
                ]
              },
              "period": {
                "type": "string",
                "enum": [
                  "PER_WEEK",
                  "PER_MONTH"
                ]
              }
            }
          },
          "anyOf": {
            "type": "object",
            "required": [
              "field",
              "values"
            ],
            "properties": {
              "field": {
                "type": "string",
                "description": "Field name as defined in SearchableField or attribute name"
              },
              "values": {
                "type": "array",
                "items": {
                  "type": "object"
                },
                "description": "List of values for the field"
              }
            }
          },
          "multiAnyOf": {
            "type": "object",
            "required": [
              "fieldValues"
            ],
            "properties": {
              "fieldValues": {
                "type": "object",
                "additionalProperties": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "description": "Map of field names to list of values"
              }
            }
          },
          "geo": {
            "type": "object",
            "required": [
              "latitude",
              "longitude",
              "maxDistance"
            ],
            "properties": {
              "latitude": {
                "type": "number",
                "format": "double"
              },
              "longitude": {
                "type": "number",
                "format": "double"
              },
              "minDistance": {
                "type": "number",
                "format": "double"
              },
              "maxDistance": {
                "type": "number",
                "format": "double"
              },
              "calDistance": {
                "type": "boolean"
              }
            }
          },
          "not": {
            "type": "object",
            "required": [
              "instruction"
            ],
            "properties": {
              "instruction": {
                "$ref": "#/components/schemas/SearchInstruction",
                "_circular": true
              }
            }
          },
          "has": {
            "description": "Supported fields: [image]",
            "type": "object",
            "required": [
              "field"
            ],
            "properties": {
              "field": {
                "type": "string"
              }
            }
          }
        }
      }
    },
    "limit": {
      "type": "integer"
    },
    "sort": {
      "type": "array",
      "items": {
        "type": "object",
        "maxProperties": 1,
        "properties": {
          "field": {
            "type": "object",
            "required": [
              "field",
              "order"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              },
              "missingFirst": {
                "type": "boolean"
              }
            }
          },
          "random": {
            "type": "object",
            "properties": {}
          },
          "distance": {
            "type": "object",
            "required": [
              "order"
            ],
            "properties": {
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              }
            }
          },
          "score": {
            "type": "object",
            "properties": {}
          },
          "doc": {
            "type": "object",
            "properties": {}
          },
          "delta": {
            "type": "object",
            "required": [
              "field",
              "value",
              "order"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              },
              "value": {
                "type": "integer",
                "format": "int64"
              }
            }
          }
        }
      }
    },
    "fetchFields": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "timeoutInMins": {
      "type": "integer"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/similar-ads`
- **服务**: livead-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `livead-search Web 控制器（契约 operationId: search / getAdvertById 等）`
- **OpenAPI 契约**: `livead-search/contract/src/main/resources/livead-search-contract.yaml`
- **功能描述**: Find Similar Ads: ads in the same category if possible
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "required": [
    "categoryId",
    "locationId",
    "limit"
  ],
  "properties": {
    "categoryId": {
      "type": "integer",
      "format": "int64"
    },
    "locationId": {
      "type": "integer",
      "format": "int64"
    },
    "limit": {
      "type": "integer"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 6. fullad-search-service

#### GET `/api/advert/{advertId}`
- **服务**: fullad-search-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `fullad-search-service 搜索控制器`
- **OpenAPI 契约**: `fullad-search-service/contract/src/main/resources/fullad-search-service-contract.yaml`
- **功能描述**: Get Advert by ID
- **路径 / 查询 / 头参数**:
  - `advertId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — Advert ID
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/manage-ads`
- **服务**: fullad-search-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `fullad-search-service 搜索控制器`
- **OpenAPI 契约**: `fullad-search-service/contract/src/main/resources/fullad-search-service-contract.yaml`
- **功能描述**: Find Manage Ads: ads with account id and search term
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "required": [
    "account_id",
    "search",
    "statuses",
    "limit",
    "offset"
  ],
  "properties": {
    "account_id": {
      "type": "integer",
      "format": "int64"
    },
    "search": {
      "type": "string"
    },
    "statuses": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "offset": {
      "type": "integer"
    },
    "limit": {
      "type": "integer"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/api/search`
- **服务**: fullad-search-service
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `fullad-search-service 搜索控制器`
- **OpenAPI 契约**: `fullad-search-service/contract/src/main/resources/fullad-search-service-contract.yaml`
- **功能描述**: Advert Search
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "instructions": {
      "type": "array",
      "items": {
        "type": "object",
        "maxProperties": 1,
        "properties": {
          "q": {
            "type": "object",
            "required": [
              "keywords"
            ],
            "properties": {
              "keywords": {
                "type": "array",
                "items": {
                  "type": "string"
                }
              },
              "extended": {
                "type": "boolean"
              }
            }
          },
          "eq": {
            "type": "object",
            "required": [
              "field",
              "value"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "value": {
                "type": "string"
              },
              "unit": {
                "type": "string",
                "enum": [
                  "PENCE",
                  "POUNCE"
                ]
              }
            }
          },
          "range": {
            "type": "object",
            "required": [
              "field"
            ],
            "minProperties": 2,
            "properties": {
              "field": {
                "type": "string"
              },
              "gte": {
                "type": "integer",
                "format": "int64"
              },
              "lt": {
                "type": "integer",
                "format": "int64"
              },
              "lte": {
                "type": "integer",
                "format": "int64"
              },
              "unit": {
                "type": "string",
                "enum": [
                  "PENCE",
                  "POUNCE"
                ]
              },
              "period": {
                "type": "string",
                "enum": [
                  "PER_WEEK",
                  "PER_MONTH"
                ]
              }
            }
          },
          "anyOf": {
            "type": "object",
            "required": [
              "field",
              "values"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "values": {
                "type": "array",
                "items": {
                  "type": "object"
                }
              }
            }
          },
          "geo": {
            "type": "object",
            "required": [
              "latitude",
              "longitude",
              "maxDistance"
            ],
            "properties": {
              "latitude": {
                "type": "number",
                "format": "double"
              },
              "longitude": {
                "type": "number",
                "format": "double"
              },
              "minDistance": {
                "type": "number",
                "format": "double"
              },
              "maxDistance": {
                "type": "number",
                "format": "double"
              },
              "calDistance": {
                "type": "boolean"
              }
            }
          },
          "not": {
            "type": "object",
            "required": [
              "instruction"
            ],
            "properties": {
              "instruction": {
                "$ref": "#/components/schemas/SearchInstruction",
                "_circular": true
              }
            }
          },
          "has": {
            "description": "Supported fields: [image]",
            "type": "object",
            "required": [
              "field"
            ],
            "properties": {
              "field": {
                "type": "string"
              }
            }
          }
        }
      }
    },
    "sort": {
      "type": "array",
      "items": {
        "type": "object",
        "maxProperties": 1,
        "properties": {
          "field": {
            "type": "object",
            "required": [
              "field",
              "order"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              },
              "missingFirst": {
                "type": "boolean"
              }
            }
          },
          "random": {
            "type": "object",
            "properties": {}
          },
          "distance": {
            "type": "object",
            "required": [
              "order"
            ],
            "properties": {
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              }
            }
          },
          "score": {
            "type": "object",
            "properties": {}
          },
          "doc": {
            "type": "object",
            "properties": {}
          },
          "delta": {
            "type": "object",
            "required": [
              "field",
              "value",
              "order"
            ],
            "properties": {
              "field": {
                "type": "string"
              },
              "order": {
                "type": "string",
                "enum": [
                  "ASC",
                  "DESC"
                ]
              },
              "value": {
                "type": "integer",
                "format": "int64"
              }
            }
          }
        }
      }
    },
    "aggregations": {
      "type": "array",
      "items": {
        "type": "object",
        "required": [
          "name",
          "field"
        ],
        "properties": {
          "name": {
            "type": "string"
          },
          "field": {
            "type": "string",
            "description": "Field name as defined in SearchableField or attribute name"
          },
          "values": {
            "type": "array",
            "description": "List of exact values to include as buckets. If omitted for attributes list of supported search values defined in AttributeMetadata model will be used.",
            "items": {
              "type": "object"
            }
          },
          "size": {
            "type": "integer",
            "description": "Number of buckets."
          }
        }
      }
    },
    "fetchFields": {
      "type": "array",
      "items": {
        "type": "string"
      }
    },
    "offset": {
      "type": "integer"
    },
    "limit": {
      "type": "integer"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 7. gumshield-ad-search

#### POST `/api/adverts/search`
- **服务**: gumshield-ad-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `gumshield-ad-search REST 控制器`
- **OpenAPI 契约**: `gumshield-ad-search/contract/src/main/resources/gumshield-ad-search-contract.yaml`
- **功能描述**: Searches adverts executing Customer Support request
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "required": [
    "criteria"
  ],
  "properties": {
    "criteria": {
      "type": "array",
      "items": {
        "type": "object",
        "required": [
          "fields",
          "operator",
          "value"
        ],
        "properties": {
          "fields": {
            "type": "array",
            "items": {
              "type": "string"
            }
          },
          "operator": {
            "type": "string",
            "enum": [
              "eq",
              "not-eq",
              "range",
              "lte",
              "gte",
              "any-of",
              "match-words",
              "match-phrase"
            ]
          },
          "value": {
            "type": "object"
          }
        }
      }
    },
    "sort": {
      "type": "array",
      "items": {
        "required": [
          "field",
          "order"
        ],
        "properties": {
          "field": {
            "type": "string"
          },
          "order": {
            "type": "string",
            "enum": [
              "ASC",
              "DESC"
            ]
          }
        }
      }
    },
    "searchAfter": {
      "type": "array",
      "items": {
        "type": "object"
      }
    },
    "limit": {
      "type": "integer"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/adverts/{advertId}`
- **服务**: gumshield-ad-search
- **Servlet / 前缀**: `/api`
- **控制器 / Handler**: `gumshield-ad-search REST 控制器`
- **OpenAPI 契约**: `gumshield-ad-search/contract/src/main/resources/gumshield-ad-search-contract.yaml`
- **功能描述**: Find an advert by ID
- **路径 / 查询 / 头参数**:
  - `advertId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — Advert ID
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 8. conversation-reporter

#### POST `/conversation-reports/`
- **服务**: conversation-reporter
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `conversation-reporter 服务控制器`
- **OpenAPI 契约**: `conversation-reporter/contract/contract.yaml`
- **功能描述**: Add a new conversation report for a given conversation ID by authenticated reporting user
- **请求体** (`requestBody`):
  - required: None
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "conversation report request",
  "required": [
    "converseeUserId",
    "conversationId",
    "advertId",
    "reasons"
  ],
  "properties": {
    "converseeUserId": {
      "type": "integer",
      "format": "int64",
      "description": "id of the user in the conversation with the reporter",
      "example": 12345679
    },
    "conversationId": {
      "type": "string",
      "description": "id of the conversation the user reported is involved into",
      "example": "5:2c00trz:2hlcwpph4"
    },
    "advertId": {
      "type": "integer",
      "format": "int64",
      "description": "id of the advert associated with the reported conversation",
      "example": 123
    },
    "reasons": {
      "type": "array",
      "items": {
        "type": "string",
        "enum": [
          "FRAUD_SCAM",
          "ABUSE_HARASSMENT",
          "ADULT_INAPPROPRIATE",
          "PHISHING_ATTEMPT",
          "SPAM",
          "POLICY_VIOLATION",
          "NO_REPLY",
          "NO_SHOW"
        ],
        "description": "type of reason for a report",
        "example": "ABUSE_HARASSMENT"
      }
    },
    "comment": {
      "type": "string",
      "description": "optional description of the reason for reporting the conversation",
      "example": "any descriptive text"
    },
    "metadata": {
      "type": "object",
      "description": "map KEY/VALUE of additional fields",
      "example": "IP_ADDRESS / 127.0.0.1",
      "additionalProperties": {
        "type": "object"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/conversation-reports/search`
- **服务**: conversation-reporter
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `conversation-reporter 服务控制器`
- **OpenAPI 契约**: `conversation-reporter/contract/contract.yaml`
- **功能描述**: Search Conversation Reports given list of filters
- **请求体** (`requestBody`):
  - required: None
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "properties": {
    "criteria": {
      "type": "object",
      "description": "criteria that will be applied to the search",
      "example": 1678808826,
      "properties": {
        "reporter": {
          "type": "integer",
          "format": "int64",
          "description": "reporting user id to filter for",
          "example": 1234
        },
        "reportType": {
          "type": "string",
          "enum": [
            "FRAUD_SCAM",
            "ABUSE_HARASSMENT",
            "ADULT_INAPPROPRIATE",
            "PHISHING_ATTEMPT",
            "SPAM",
            "POLICY_VIOLATION",
            "NO_REPLY",
            "NO_SHOW"
          ],
          "description": "type of reason for a report",
          "example": "ABUSE_HARASSMENT"
        },
        "includeReviewed": {
          "type": "boolean",
          "description": "include or not the reports already marked as reviewed",
          "example": true
        },
        "createdDateFilter": {
          "type": "object",
          "required": [
            "operator",
            "createDate"
          ],
          "properties": {
            "operator": {
              "type": "string",
              "enum": [
                "eq",
                "lte",
                "gte"
              ],
              "description": "operator to apply on the filter (< , > , =)",
              "example": "lte"
            },
            "createDate": {
              "type": "string",
              "format": "date",
              "description": "created date",
              "example": "2017-07-21"
            }
          }
        }
      }
    },
    "limit": {
      "type": "integer",
      "description": "max number of item that will be retrieved in the result",
      "example": 500
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/conversation-reports/{conversation-report-id}/mark-as-reviewed`
- **服务**: conversation-reporter
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `conversation-reporter 服务控制器`
- **OpenAPI 契约**: `conversation-reporter/contract/contract.yaml`
- **功能描述**: update the conversation report to mark it as reviewed given the conversation report id
- **路径 / 查询 / 头参数**:
  - `conversation-report-id` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — conversation report id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/conversations/{conversation-id}/is-already-reported`
- **服务**: conversation-reporter
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `conversation-reporter 服务控制器`
- **OpenAPI 契约**: `conversation-reporter/contract/contract.yaml`
- **功能描述**: Check if a report has already been created for given conversation by authenticated user
- **路径 / 查询 / 头参数**:
  - `conversation-id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — conversation id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 9. conversation-reporter-papi

#### POST `/conversation-reports`
- **服务**: conversation-reporter-papi
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `conversation-reporter-papi 控制器`
- **OpenAPI 契约**: `conversation-reporter-papi/contract/contract.yaml`
- **功能描述**: Add a new conversation report for a given conversation ID by authenticated reporting user
- **请求体** (`requestBody`):
  - required: None
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "conversation report request",
  "required": [
    "converseeUserId",
    "conversationId",
    "advertId",
    "reportReasonKeys"
  ],
  "properties": {
    "converseeUserId": {
      "type": "integer",
      "format": "int64",
      "description": "id of the user in the conversation with the reporter",
      "example": 12345679
    },
    "conversationId": {
      "type": "string",
      "description": "id of the conversation the user reported is involved into",
      "example": "5:2c00trz:2hlcwpph4"
    },
    "advertId": {
      "type": "integer",
      "format": "int64",
      "description": "id of the advert associated with the reported conversation",
      "example": 123
    },
    "reportReasonKeys": {
      "type": "array",
      "items": {
        "type": "string"
      },
      "example": "FRAUD_SCAM, ABUSE_HARASSMENT",
      "description": "list of keys of the report reason"
    },
    "comment": {
      "type": "string",
      "description": "optional description of the reason for reporting the conversation",
      "example": "any descriptive text"
    },
    "metadata": {
      "type": "object",
      "description": "map KEY/VALUE of additional fields",
      "example": "IP_ADDRESS / 127.0.0.1",
      "additionalProperties": {
        "type": "object"
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/conversations/{conversation-id}/is-already-reported`
- **服务**: conversation-reporter-papi
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `conversation-reporter-papi 控制器`
- **OpenAPI 契约**: `conversation-reporter-papi/contract/contract.yaml`
- **功能描述**: Check if a report has already been created for given conversation by authenticated user
- **路径 / 查询 / 头参数**:
  - `conversation-id` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — conversation id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 10. block-user-conversations-papi

#### GET `/v1/block-users`
- **服务**: block-user-conversations-papi
- **Servlet / 前缀**: `/v1`
- **控制器 / Handler**: `block-user-conversations-papi 控制器`
- **OpenAPI 契约**: `block-user-conversations-papi/contract/contract.yaml`
- **功能描述**: returns the list of "block-users" blocked by the authenticated user in the security token
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/v1/block-users/{userIdToBlock}`
- **服务**: block-user-conversations-papi
- **Servlet / 前缀**: `/v1`
- **控制器 / Handler**: `block-user-conversations-papi 控制器`
- **OpenAPI 契约**: `block-user-conversations-papi/contract/contract.yaml`
- **功能描述**: Allows a recipient to unblock a sender
- **路径 / 查询 / 头参数**:
  - `userIdToBlock` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — user id to unblock
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/v1/block-users/{userIdToBlock}`
- **服务**: block-user-conversations-papi
- **Servlet / 前缀**: `/v1`
- **控制器 / Handler**: `block-user-conversations-papi 控制器`
- **OpenAPI 契约**: `block-user-conversations-papi/contract/contract.yaml`
- **功能描述**: Returns true if the userIdToBlock is blocked by the authenticated user in the security token
- **路径 / 查询 / 头参数**:
  - `userIdToBlock` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — user id to block
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/v1/block-users/{userIdToBlock}`
- **服务**: block-user-conversations-papi
- **Servlet / 前缀**: `/v1`
- **控制器 / Handler**: `block-user-conversations-papi 控制器`
- **OpenAPI 契约**: `block-user-conversations-papi/contract/contract.yaml`
- **功能描述**: Allows a recipient to block a sender
- **路径 / 查询 / 头参数**:
  - `userIdToBlock` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}` — user id to block
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 11. message-moderation-history

#### POST `/history/automatic-moderation`
- **服务**: message-moderation-history
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-history 控制器`
- **OpenAPI 契约**: `message-moderation-history/contract/contract.yaml`
- **功能描述**: endpoint to save a automatic message moderation
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Create message moderation payload",
  "properties": {
    "message": {
      "type": "object",
      "description": "Message moderation information",
      "required": [
        "id",
        "adId",
        "sellerId",
        "messageBody",
        "sender",
        "messageCreationTime",
        "advertMessageType"
      ],
      "properties": {
        "id": {
          "type": "string",
          "description": "Message id",
          "example": "ca279696-7a77-40c2-ad33-ff1c9af8bbce"
        },
        "adId": {
          "type": "integer",
          "format": "int64",
          "description": "the id of the advertising",
          "example": 11492342332
        },
        "buyerId": {
          "type": "integer",
          "format": "int64",
          "description": "the id of the buyer",
          "example": 22492342332
        },
        "sellerId": {
          "type": "integer",
          "format": "int64",
          "description": "the id of the seller",
          "example": 33492342332
        },
        "messageBody": {
          "type": "string",
          "description": "message body for moderation",
          "example": "Hello. Can I buy something ?"
        },
        "sender": {
          "type": "string",
          "enum": [
            "BUYER",
            "SELLER"
          ],
          "description": "who is sending the message BUYER or SELLER",
          "example": "BUYER"
        },
        "categoryId": {
          "type": "integer",
          "format": "int64",
          "description": "advert category id",
          "example": 1223
        },
        "messageCreationTime": {
          "type": "string",
          "format": "date-time",
          "description": "message creation date-time",
          "example": "2022-11-16T11:29:28Z"
        },
        "advertMessageType": {
          "type": "string",
          "enum": [
            "MADGEX",
            "TRIFECTA",
            "GUMTREE"
          ],
          "description": "message type of the advert",
          "example": "GUMTREE"
        },
        "origin": {
          "type": "string",
          "description": "Origin of the message. Should first be in [CAPI-SERVER, BUYER-SERVER, SELLER-SERVER]",
          "example": "CAPI-SERVER"
        },
        "traceId": {
          "type": "string",
          "format": "uuid",
          "description": "uuid used to trace a message from the frontend call to the moderation",
          "example": "d6ff3b69-6fa2-46e6-b6bb-b0bdc907e988"
        },
        "ipAddress": {
          "type": "string",
          "description": "sender ip address",
          "example": "5.173.17.25"
        },
        "cookie": {
          "type": "string",
          "description": "sender cookie",
          "example": "JSESSIONID=1A530637289A03B07199A44E8D531427"
        },
        "senderEmail": {
          "type": "string",
          "description": "sender email",
          "example": "sender.email@mail.com"
        },
        "title": {
          "type": "string",
          "description": "advert title",
          "example": "new blue sofa"
        },
        "receiverEmail": {
          "type": "string",
          "description": "email of the receiver",
          "example": "receiver@email.com"
        },
        "conversationId": {
          "type": "string",
          "description": "id of the conversation",
          "example": "72767b5c-4cdf-4ca7-8ab0-afe902cb186b"
        },
        "attachments": {
          "type": "array",
          "items": {
            "type": "object",
            "description": "message attachments",
            "required": [
              "name"
            ],
            "properties": {
              "name": {
                "type": "string",
                "description": "name of the file (or url if not provided by core-chat)",
                "example": "filename.jpg"
              },
              "assetUrl": {
                "type": "string",
                "description": "url to download the file",
                "example": "https://cms.example.com/download/345a2bc9-330a-4b5d-b0a6-2b067e3a4ac8.jpg"
              }
            }
          }
        },
        "knownGoodUser": {
          "type": "boolean",
          "description": "is the sender a known good user (15 days after the registration date)",
          "example": true
        }
      }
    },
    "automaticModeration": {
      "type": "object",
      "description": "Message automatic moderation decision information",
      "required": [
        "decision",
        "decisionTs"
      ],
      "properties": {
        "decision": {
          "type": "string",
          "enum": [
            "VALID",
            "DELAYED",
            "DROPPED"
          ],
          "description": "decision of automatic message moderation",
          "example": "DELAYED"
        },
        "decisionTs": {
          "type": "string",
          "format": "date-time",
          "description": "decision date due to the message",
          "example": "2022-11-16T11:29:28Z"
        }
      }
    },
    "messageModerationRules": {
      "type": "array",
      "items": {
        "type": "object",
        "description": "Moderation rule which applied to the message",
        "required": [
          "ruleId",
          "ruleType"
        ],
        "properties": {
          "ruleId": {
            "type": "string",
            "format": "UUID",
            "description": "rule id",
            "example": "ca279696-7a77-40c2-ad33-ff1c9af8bbce"
          },
          "ruleType": {
            "type": "string",
            "enum": [
              "BLACKLIST",
              "REGEX",
              "VELOCITY"
            ],
            "description": "type of the rule which applied to the message",
            "example": "VELOCITY"
          },
          "filterField": {
            "type": "string",
            "enum": [
              "COOKIE",
              "EMAIL",
              "IP_ADDRESS",
              "USER_ID"
            ],
            "description": "the field used to check the velocity",
            "example": "EMAIL"
          },
          "whiteListDuration": {
            "type": "integer",
            "description": "Only used for Velocity rule. Following a manual moderation, this rule is not applied to this user for this duration",
            "example": 86400
          }
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/history/gdpr/ddr/{email}`
- **服务**: message-moderation-history
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-history 控制器`
- **OpenAPI 契约**: `message-moderation-history/contract/contract.yaml`
- **功能描述**: endpoint to send data deletion request
- **路径 / 查询 / 头参数**:
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/history/gdpr/sdr/{email}`
- **服务**: message-moderation-history
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-history 控制器`
- **OpenAPI 契约**: `message-moderation-history/contract/contract.yaml`
- **功能描述**: endpoint to send subject data request
- **路径 / 查询 / 头参数**:
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/history/manual-moderation`
- **服务**: message-moderation-history
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-history 控制器`
- **OpenAPI 契约**: `message-moderation-history/contract/contract.yaml`
- **功能描述**: endpoint to save a manual message moderation
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Message manual moderation request body",
  "required": [
    "messageId",
    "manualModeration"
  ],
  "properties": {
    "messageId": {
      "type": "string",
      "description": "Message id",
      "example": "ca279696-7a77-40c2-ad33-ff1c9af8bbce"
    },
    "manualModeration": {
      "type": "object",
      "description": "Message manual moderation decision information",
      "required": [
        "decision",
        "decisionTs"
      ],
      "properties": {
        "decision": {
          "type": "string",
          "enum": [
            "SEND",
            "DELETE"
          ],
          "description": "decision of manual message moderation",
          "example": "SEND"
        },
        "decisionTs": {
          "type": "string",
          "format": "date-time",
          "description": "manual message decision timestamp",
          "example": "2023-02-22T11:22:28Z"
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/history/moderation/search`
- **服务**: message-moderation-history
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-history 控制器`
- **OpenAPI 契约**: `message-moderation-history/contract/contract.yaml`
- **功能描述**: endpoint to search messages
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Message filter request payload",
  "properties": {
    "maxResults": {
      "type": "integer",
      "description": "max results per one request",
      "example": 100
    },
    "dataSource": {
      "type": "string",
      "enum": [
        "DEFAULT",
        "CORECHAT",
        "COMAAS",
        "CORECHAT_AND_COMAAS"
      ],
      "description": "data source",
      "example": "CORECHAT"
    },
    "filterList": {
      "type": "array",
      "description": "list of filters",
      "items": {
        "type": "object",
        "description": "filter composition",
        "required": [
          "field",
          "operator",
          "value"
        ],
        "properties": {
          "field": {
            "type": "string",
            "enum": [
              "STATUS",
              "MESSAGE_CREATION_TIME",
              "EMAIL",
              "BUYER_IP",
              "BUYER_COOKIE",
              "CONVERSATION_ID",
              "SENDER_IP",
              "SENDER_COOKIE",
              "ADVERT_CATEGORY_ID"
            ],
            "description": "filter field",
            "example": "STATUS"
          },
          "operator": {
            "type": "string",
            "enum": [
              "EQ",
              "GE",
              "LE",
              "IN"
            ],
            "description": "comparison operator",
            "example": "EQ"
          },
          "value": {
            "type": "string",
            "description": "value of filter field",
            "example": "VALID"
          }
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/history/whitelisted-rules`
- **服务**: message-moderation-history
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-history 控制器`
- **OpenAPI 契约**: `message-moderation-history/contract/contract.yaml`
- **功能描述**: whitelisted rules
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 12. category-api（Play `routes` + 契约节选）

以下 **HTTP 路由** 摘自 `category-api/server/conf/routes`（Play）；`category-api/contract/contract.yaml` 仅包含部分路径的 OpenAPI 模型（`Category` / `Attribute` 等大对象与契约中一致）。

- `GET         /_version                                   @controllers.CategoryController.version`
- `GET         /api/seo-config                             @controllers.CategoryController.seoConfig`
- `GET         /api/categories                             @controllers.CategoryController.byName(name = "all")`
- `GET         /api/categories/list                        @controllers.CategoryController.categoryList`
- `GET         /api/categories/virtual                     @controllers.CategoryController.virtualCategories`
- `GET         /api/categories/name/:name/list             @controllers.CategoryController.categoryListFrom(name)`
- `GET         /api/categories/name/:name                  @controllers.CategoryController.byName(name)`
- `GET         /api/categories/id/:id                      @controllers.CategoryController.byId(id: Long)`
- `GET         /api/attributes                             @controllers.CategoryController.attributes`
- `GET         /api/categories/attributes                  @controllers.CategoryController.attributes`
- `GET         /api/categories/name/:name/attributes       @controllers.CategoryController.attributesByCategoryName(name)`
- `GET         /api/categories/filter/:keyValues           @controllers.CategoryController.filterOut(keyValues)`
- `GET         /api/categories/:catId/attributes           @controllers.CategoryController.attributesByCategoryId(catId: Long)`
- `GET         /api/attributes/id/:id                      @controllers.AttributeController.byId(id: Long)`
- `GET         /api/categories/vips                        @controllers.CategoryController.vips`
- `GET         /api/vips                                   @controllers.CategoryController.vips`
- `GET         /api/categories/:catId/vip                  @controllers.CategoryController.vipByCategoryId(catId: Long)`

### 12.1 OpenAPI 已建模端点（节选，完整字段见契约 `components.schemas`）

#### GET `/_version`
- **服务**: category-api
- **Servlet / 前缀**: ``
- **控制器 / Handler**: `controllers.CategoryController / AttributeController`
- **OpenAPI 契约**: `category-api/contract/contract.yaml`
- **功能描述**: Get version
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/categories`
- **服务**: category-api
- **Servlet / 前缀**: ``
- **控制器 / Handler**: `controllers.CategoryController / AttributeController`
- **OpenAPI 契约**: `category-api/contract/contract.yaml`
- **功能描述**: Get categories
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/categories/attributes`
- **服务**: category-api
- **Servlet / 前缀**: ``
- **控制器 / Handler**: `controllers.CategoryController / AttributeController`
- **OpenAPI 契约**: `category-api/contract/contract.yaml`
- **功能描述**: Get attributes
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 13. pricing-api

#### GET `/api/pricing/{categoryId}/{locationId}`
- **服务**: pricing-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `pricing-api REST 控制器`
- **OpenAPI 契约**: `pricing-api/contract/contract.yaml`
- **功能描述**: Provides product prices based on categoryId and locationId. If we don't have price for a given category and location we discover closest available price traversing the category and location hierarchy.
- **路径 / 查询 / 头参数**:
  - `Client-Id` (**header**), 必填=False, 类型/schema: `{
  "type": "string"
}` — Client id for tracking purpose
  - `categoryId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — Starting categoryId for the product price search
  - `locationId` (**path**), 必填=True, 类型/schema: `{
  "type": "integer",
  "format": "int64"
}` — Starting locationId for the product price search
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 14. gumshield-rules（只读 GET；策略管理写入为人工作业）

#### GET `/api/ad-rule-reports/performance-summary/{rule-name}/{days}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: return Rule Performance Data for a particular Rule Name
- **路径 / 查询 / 头参数**:
  - `rule-name` (**path**), 必填=False, 类型/schema: `null` — rule name
  - `days` (**path**), 必填=False, 类型/schema: `null` — number of days
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/ad-rule-reports/{id}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Get ad rule reports
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=False, 类型/schema: `null` — ad id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/ad-screen-items`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns all ad screen items
- **路径 / 查询 / 头参数**:
  - `max-result` (**query**), 必填=False, 类型/schema: `null` — max result to return
  - `session-id` (**query**), 必填=False, 类型/schema: `null` — session_id used to lock rows for user
  - `meta-tag` (**query**), 必填=True, 类型/schema: `null` — meta-tags, k=v separated
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/ad-screen-items/advert/{advert-id}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns all ad screen items for given advert-id
- **路径 / 查询 / 头参数**:
  - `advert-id` (**path**), 必填=True, 类型/schema: `null` — advert to find screen items for
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/ipranges`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns all blacklisted ip ranges
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/ipranges/check/{ip}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Check if an ip is blackListed
- **路径 / 查询 / 头参数**:
  - `ip` (**path**), 必填=True, 类型/schema: `null` — the ip address
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/exclusionlist/{matchListEntryId}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Given the supplied matchlist-entry Id, this call will return the associated exclusion terms
- **路径 / 查询 / 头参数**:
  - `matchListEntryId` (**path**), 必填=True, 类型/schema: `null` — the parent matchlist entry id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/matchlist`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns all matchlists.
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/matchlist-entry/{matchListId}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Given the supplied matchlist Id, this call will return the children (the matchlist entries) of that parent matchlist
- **路径 / 查询 / 头参数**:
  - `matchListId` (**path**), 必填=True, 类型/schema: `null` — the parent matchlist id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/matchlist-entry/{matchListId}/filter/{filterName}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Given the supplied matchlist Id, this call will return the filter matching children (the matchlist entries) of that parent matchlist
- **路径 / 查询 / 头参数**:
  - `matchListId` (**path**), 必填=True, 类型/schema: `null` — The parent matchlist id
  - `filterName` (**path**), 必填=True, 类型/schema: `null` — The filter name
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/matchlist/filter/{filterName}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns all matchlists matching filter name.
- **路径 / 查询 / 头参数**:
  - `filterName` (**path**), 必填=True, 类型/schema: `null` — The filter name
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/matchlist/{id}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns matchlist matching supplied database Id.
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `null` — id
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/stoplist`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns all entries in the stop list
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/api/policy/stoplist/{term}`
- **服务**: gumshield-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `gumshield-rules 各策略 Controller`
- **OpenAPI 契约**: `gumshield-rules/gumshield-rules-contract/src/main/resources/gumshield-rules.json`
- **功能描述**: Returns the entry matching the supplied term in the stoplist if it exists
- **路径 / 查询 / 头参数**:
  - `term` (**path**), 必填=True, 类型/schema: `null` — the stop list term
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 15. payment-api（Braintree — 仅退款查询相关）

#### POST `/api/payment/braintree/refund`
- **服务**: payment-api
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `payment-api Braintree 控制器`
- **OpenAPI 契约**: `payment-api/contract/contract.yaml`
- **功能描述**: Refund already executed transaction for provided Reference ID
- **路径 / 查询 / 头参数**:
  - `Client-Id` (**header**), 必填=False, 类型/schema: `{
  "type": "string"
}` — Client ID for tracking purpose
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "required": [
    "amount_to_refund",
    "reference_id"
  ],
  "type": "object",
  "properties": {
    "reference_id": {
      "type": "string",
      "description": "Braintree transaction to refund against"
    },
    "amount_to_refund": {
      "type": "integer",
      "description": "Total amount to be refunded",
      "format": "int64"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 16. braze-gateway（GDPR 子集）

#### POST `/gdpr/ddr`
- **服务**: braze-gateway
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `braze-gateway GDPR 控制器`
- **OpenAPI 契约**: `braze-gateway/contract/contract.yaml`
- **功能描述**: endpoint to send data disposal request for multiple ids
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "array",
  "items": {
    "type": "string"
  },
  "maxItems": 50
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/gdpr/ddr/{email}`
- **服务**: braze-gateway
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `braze-gateway GDPR 控制器`
- **OpenAPI 契约**: `braze-gateway/contract/contract.yaml`
- **功能描述**: endpoint to send data disposal request
- **路径 / 查询 / 头参数**:
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/gdpr/sar/{email}`
- **服务**: braze-gateway
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `braze-gateway GDPR 控制器`
- **OpenAPI 契约**: `braze-gateway/contract/contract.yaml`
- **功能描述**: endpoint to get user data request
- **路径 / 查询 / 头参数**:
  - `email` (**path**), 必填=True, 类型/schema: `{
  "type": "string"
}`
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 17. message-moderation-rules（消息审核规则）

#### GET `/rules/blacklist`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to get list of blacklist rules
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/rules/blacklist`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to add a new blacklist rule
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Create blacklist rule request payload",
  "required": [
    "configuration"
  ],
  "properties": {
    "configuration": {
      "type": "object",
      "description": "shared rules properties",
      "required": [
        "title",
        "riskAssessment",
        "outcome",
        "isEnabled",
        "exemptedCategories",
        "applyToGoodKnownUsers",
        "applyToPayShipAds"
      ],
      "properties": {
        "title": {
          "type": "string",
          "description": "the name of this rule",
          "example": "Profanity"
        },
        "riskAssessment": {
          "type": "string",
          "description": "The risk assessment for this rule",
          "enum": [
            "RISK",
            "POLICY"
          ],
          "example": "RISK"
        },
        "outcome": {
          "type": "string",
          "description": "The outcome applied on a message when the rule matches with the message",
          "enum": [
            "HOLD",
            "DROP"
          ],
          "example": "HOLD"
        },
        "isEnabled": {
          "type": "boolean",
          "description": "a rule can be enabled or disabled",
          "example": true
        },
        "exemptedCategories": {
          "type": "array",
          "items": {
            "type": "object",
            "description": "technical id of a category",
            "properties": {
              "id": {
                "type": "integer",
                "format": "int64",
                "description": "category id",
                "example": 7343637
              }
            }
          }
        },
        "applyToGoodKnownUsers": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from known good users",
          "example": true
        },
        "applyToPayShipAds": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from pay and ship adverts",
          "example": true
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/rules/blacklist/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to delete blacklist rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the rule to delete
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/rules/blacklist/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to get blacklist rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the rule for getting
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/rules/blacklist/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to edit blacklist rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the rule for changing
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Edit blacklist rule request payload",
  "required": [
    "configuration"
  ],
  "properties": {
    "configuration": {
      "type": "object",
      "description": "shared rules properties",
      "required": [
        "title",
        "riskAssessment",
        "outcome",
        "isEnabled",
        "exemptedCategories",
        "applyToGoodKnownUsers",
        "applyToPayShipAds"
      ],
      "properties": {
        "title": {
          "type": "string",
          "description": "the name of this rule",
          "example": "Profanity"
        },
        "riskAssessment": {
          "type": "string",
          "description": "The risk assessment for this rule",
          "enum": [
            "RISK",
            "POLICY"
          ],
          "example": "RISK"
        },
        "outcome": {
          "type": "string",
          "description": "The outcome applied on a message when the rule matches with the message",
          "enum": [
            "HOLD",
            "DROP"
          ],
          "example": "HOLD"
        },
        "isEnabled": {
          "type": "boolean",
          "description": "a rule can be enabled or disabled",
          "example": true
        },
        "exemptedCategories": {
          "type": "array",
          "items": {
            "type": "object",
            "description": "technical id of a category",
            "properties": {
              "id": {
                "type": "integer",
                "format": "int64",
                "description": "category id",
                "example": 7343637
              }
            }
          }
        },
        "applyToGoodKnownUsers": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from known good users",
          "example": true
        },
        "applyToPayShipAds": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from pay and ship adverts",
          "example": true
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/rules/regex`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to get all regex rules
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/rules/regex`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to add a new regex rule
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Create regex rule request payload",
  "required": [
    "configuration",
    "regularExpressions"
  ],
  "properties": {
    "configuration": {
      "type": "object",
      "description": "shared rules properties",
      "required": [
        "title",
        "riskAssessment",
        "outcome",
        "isEnabled",
        "exemptedCategories",
        "applyToGoodKnownUsers",
        "applyToPayShipAds"
      ],
      "properties": {
        "title": {
          "type": "string",
          "description": "the name of this rule",
          "example": "Profanity"
        },
        "riskAssessment": {
          "type": "string",
          "description": "The risk assessment for this rule",
          "enum": [
            "RISK",
            "POLICY"
          ],
          "example": "RISK"
        },
        "outcome": {
          "type": "string",
          "description": "The outcome applied on a message when the rule matches with the message",
          "enum": [
            "HOLD",
            "DROP"
          ],
          "example": "HOLD"
        },
        "isEnabled": {
          "type": "boolean",
          "description": "a rule can be enabled or disabled",
          "example": true
        },
        "exemptedCategories": {
          "type": "array",
          "items": {
            "type": "object",
            "description": "technical id of a category",
            "properties": {
              "id": {
                "type": "integer",
                "format": "int64",
                "description": "category id",
                "example": 7343637
              }
            }
          }
        },
        "applyToGoodKnownUsers": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from known good users",
          "example": true
        },
        "applyToPayShipAds": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from pay and ship adverts",
          "example": true
        }
      }
    },
    "regularExpressions": {
      "type": "array",
      "items": {
        "type": "object",
        "description": "regular expression",
        "required": [
          "pattern"
        ],
        "properties": {
          "pattern": {
            "type": "string",
            "description": "regular expression pattern",
            "example": "^forbidden$"
          }
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/rules/regex/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to delete regex rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the regex rule to delete
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/rules/regex/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to get regex rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the regex rule for getting
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/rules/regex/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to edit regex rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the regex rule for changing
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Edit regex rule request payload",
  "required": [
    "configuration",
    "regularExpressions"
  ],
  "properties": {
    "configuration": {
      "type": "object",
      "description": "shared rules properties",
      "required": [
        "title",
        "riskAssessment",
        "outcome",
        "isEnabled",
        "exemptedCategories",
        "applyToGoodKnownUsers",
        "applyToPayShipAds"
      ],
      "properties": {
        "title": {
          "type": "string",
          "description": "the name of this rule",
          "example": "Profanity"
        },
        "riskAssessment": {
          "type": "string",
          "description": "The risk assessment for this rule",
          "enum": [
            "RISK",
            "POLICY"
          ],
          "example": "RISK"
        },
        "outcome": {
          "type": "string",
          "description": "The outcome applied on a message when the rule matches with the message",
          "enum": [
            "HOLD",
            "DROP"
          ],
          "example": "HOLD"
        },
        "isEnabled": {
          "type": "boolean",
          "description": "a rule can be enabled or disabled",
          "example": true
        },
        "exemptedCategories": {
          "type": "array",
          "items": {
            "type": "object",
            "description": "technical id of a category",
            "properties": {
              "id": {
                "type": "integer",
                "format": "int64",
                "description": "category id",
                "example": 7343637
              }
            }
          }
        },
        "applyToGoodKnownUsers": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from known good users",
          "example": true
        },
        "applyToPayShipAds": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from pay and ship adverts",
          "example": true
        }
      }
    },
    "regularExpressions": {
      "type": "array",
      "items": {
        "type": "object",
        "description": "regular expression",
        "required": [
          "pattern"
        ],
        "properties": {
          "pattern": {
            "type": "string",
            "description": "regular expression pattern",
            "example": "^forbidden$"
          }
        }
      }
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/rules/velocity`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to get list of velocity rules
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### POST `/rules/velocity`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to add a new velocity rule
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Create velocity rule request payload",
  "required": [
    "configuration",
    "messages",
    "seconds",
    "whiteListDuration",
    "filterField"
  ],
  "properties": {
    "configuration": {
      "type": "object",
      "description": "shared rules properties",
      "required": [
        "title",
        "riskAssessment",
        "outcome",
        "isEnabled",
        "exemptedCategories",
        "applyToGoodKnownUsers",
        "applyToPayShipAds"
      ],
      "properties": {
        "title": {
          "type": "string",
          "description": "the name of this rule",
          "example": "Profanity"
        },
        "riskAssessment": {
          "type": "string",
          "description": "The risk assessment for this rule",
          "enum": [
            "RISK",
            "POLICY"
          ],
          "example": "RISK"
        },
        "outcome": {
          "type": "string",
          "description": "The outcome applied on a message when the rule matches with the message",
          "enum": [
            "HOLD",
            "DROP"
          ],
          "example": "HOLD"
        },
        "isEnabled": {
          "type": "boolean",
          "description": "a rule can be enabled or disabled",
          "example": true
        },
        "exemptedCategories": {
          "type": "array",
          "items": {
            "type": "object",
            "description": "technical id of a category",
            "properties": {
              "id": {
                "type": "integer",
                "format": "int64",
                "description": "category id",
                "example": 7343637
              }
            }
          }
        },
        "applyToGoodKnownUsers": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from known good users",
          "example": true
        },
        "applyToPayShipAds": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from pay and ship adverts",
          "example": true
        }
      }
    },
    "messages": {
      "type": "integer",
      "description": "the maximum number of messages allowed for the given duration",
      "example": 5
    },
    "seconds": {
      "type": "integer",
      "description": "the duration during which the number of messages is counted",
      "example": 300
    },
    "whiteListDuration": {
      "type": "integer",
      "description": "following a  manual moderation, this rule is not applied to this user for this duration",
      "example": 86400
    },
    "filterField": {
      "type": "string",
      "enum": [
        "COOKIE",
        "EMAIL",
        "IP_ADDRESS",
        "USER_ID"
      ],
      "description": "the field used to check the velocity",
      "example": "COOKIE"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### DELETE `/rules/velocity/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to delete velocity rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the rule to delete
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### GET `/rules/velocity/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to get velocity rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the velocity rule for getting
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

#### PUT `/rules/velocity/{id}`
- **服务**: message-moderation-rules
- **Servlet / 前缀**: `/`
- **控制器 / Handler**: `message-moderation-rules 控制器`
- **OpenAPI 契约**: `message-moderation-rules/contract/contract.yaml`
- **功能描述**: service to edit velocity rule
- **路径 / 查询 / 头参数**:
  - `id` (**path**), 必填=True, 类型/schema: `{
  "type": "string",
  "format": "UUID"
}` — id of the rule for changing
- **请求体** (`requestBody`):
  - required: True
  - Content-Type: `application/json`
```json
{
  "type": "object",
  "description": "Edit velocity rule request payload",
  "required": [
    "configuration",
    "messages",
    "seconds",
    "whiteListDuration",
    "filterField"
  ],
  "properties": {
    "configuration": {
      "type": "object",
      "description": "shared rules properties",
      "required": [
        "title",
        "riskAssessment",
        "outcome",
        "isEnabled",
        "exemptedCategories",
        "applyToGoodKnownUsers",
        "applyToPayShipAds"
      ],
      "properties": {
        "title": {
          "type": "string",
          "description": "the name of this rule",
          "example": "Profanity"
        },
        "riskAssessment": {
          "type": "string",
          "description": "The risk assessment for this rule",
          "enum": [
            "RISK",
            "POLICY"
          ],
          "example": "RISK"
        },
        "outcome": {
          "type": "string",
          "description": "The outcome applied on a message when the rule matches with the message",
          "enum": [
            "HOLD",
            "DROP"
          ],
          "example": "HOLD"
        },
        "isEnabled": {
          "type": "boolean",
          "description": "a rule can be enabled or disabled",
          "example": true
        },
        "exemptedCategories": {
          "type": "array",
          "items": {
            "type": "object",
            "description": "technical id of a category",
            "properties": {
              "id": {
                "type": "integer",
                "format": "int64",
                "description": "category id",
                "example": 7343637
              }
            }
          }
        },
        "applyToGoodKnownUsers": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from known good users",
          "example": true
        },
        "applyToPayShipAds": {
          "type": "boolean",
          "description": "A flag to indicate whether this rule is applicable to message from pay and ship adverts",
          "example": true
        }
      }
    },
    "messages": {
      "type": "integer",
      "description": "the maximum number of messages allowed for the given duration",
      "example": 5
    },
    "seconds": {
      "type": "integer",
      "description": "the duration during which the number of messages is counted",
      "example": 300
    },
    "whiteListDuration": {
      "type": "integer",
      "description": "following a  manual moderation, this rule is not applied to this user for this duration",
      "example": 86400
    },
    "filterField": {
      "type": "string",
      "enum": [
        "COOKIE",
        "EMAIL",
        "IP_ADDRESS",
        "USER_ID"
      ],
      "description": "the field used to check the velocity",
      "example": "COOKIE"
    }
  }
}
```
- **错误与失败模式**:
  - 通用：`4xx` 客户端参数/鉴权，`5xx` 上游或内部错误；`default` / `application/problem+json` 返回结构化 `ApiError(s)`（若契约声明）。

## 18. 接口-Tool 映射矩阵

下列为 **CSAgent Tool 名称 → 典型平台 HTTP 依赖**（`✓` 表示该 Tool 可能间接或直接调用该类接口）。

| 接口 / 能力 | search_knowledge | resolve_article | get_customer_context | request_handover | record_outcome | create_case_controlled | lookup_customer_account | lookup_listing_or_ad | moderation_enforcement_action | send_followup_email |
|------|------------------|-----------------|----------------------|------------------|----------------|--------------------------|---------------------------|----------------------|------------------------------|---------------------|
| `search_knowledge` | ✓（向量库 + Salesforce Knowledge；**无**列示平台 REST） | — | — | — | — | — | — | — | — | — |
| `resolve_article` | — | ✓（Salesforce Knowledge；**无**列示平台 REST） | — | — | — | — | — | — | — | — |
| `get_customer_context`（组合） | — | — | ✓ | — | — | — | ✓（内联账户查询） | ✓（内联广告查询） | — | — |
| `lookup_customer_account` | — | — | ✓ | — | — | — | ✓ | — | — | — |
| → bapi `GET /api/emails/{email}/user` | — | — | ✓ | — | — | — | ✓ | — | — | — |
| → bapi `GET /api/accounts/{id}`、`GET /api/accounts/{id}/emails` | — | — | ✓ | — | — | — | ✓ | — | — | — |
| → user-service `GET /users/{userId}`、`GET /users/{email}/user` | — | — | ✓ | — | — | — | ✓ | — | — | — |
| → gumshield-api `GET /api/users/{id}/known-good`（及 `known_good` 变体） | — | — | ✓ | — | — | — | ✓ | — | — | — |
| `lookup_listing_or_ad` | — | — | ✓ | — | — | — | — | ✓ | — | — |
| → bapi `GET /api/adverts/{id}`、`GET /api/adverts/{id}/features`、`POST /api/adverts/search` 等 | — | — | ✓ | — | — | — | — | ✓ | — | — |
| → gumshield-api `GET /api/adverts/status/{id}`、`.../known-good` | — | — | ✓ | — | — | — | — | ✓ | — | — |
| → livead-search `GET /api/advert/{advertId}` | — | — | ✓ | — | — | — | — | ✓ | — | — |
| → fullad-search-service `GET /api/advert/{advertId}`、`POST /api/search` | — | — | ✓ | — | — | — | — | ✓ | — | — |
| → gumshield-ad-search `GET /api/adverts/{advertId}`、`POST /api/adverts/search` | — | — | ✓ | — | — | — | — | ✓ | — | — |
| → advert-service 按 ID 查询 | — | — | ✓ | — | — | — | — | ✓ | — | — |
| `request_handover` | — | — | — | ✓（Salesforce Omni-Channel；**无**列示平台 REST） | — | — | — | — | — | — |
| `record_outcome` | — | — | — | — | ✓（Kafka + Salesforce；**无**列示平台 REST） | — | — | — | — | — |
| `create_case_controlled` | — | — | — | — | — | ✓（Salesforce Case API；**无**列示平台 REST） | — | — | — | — |
| `moderation_enforcement_action`（human_only） | — | — | — | — | — | — | — | — | ✓ | — |
| → bapi `POST /api/cs/adverts/*`、`DELETE /api/adverts/{id}` 等 | — | — | — | — | — | — | — | — | ✓ | — |
| → gumshield-api `POST /api/adverts/status`、`POST /api/cs-review`、`POST /api/user-reports` 等 | — | — | — | — | — | — | — | — | ✓ | — |
| `send_followup_email`（human_only） | — | — | — | — | — | — | — | — | — | ✓（Salesforce Email / braze-gateway GDPR 非通用发信） |
| braze-gateway `GET /gdpr/sar/{email}`、`DELETE /gdpr/ddr/{email}` 等 | — | — | — | — | — | — | — | — | — | ✓（与 CRM 流程配合时） |
