# Phase 1 — Solution Input Pack

> 汇总业务、领域知识、运营模型、工程约束与评测输入，形成进入 Domain Realization 的完整素材包。
>
> **输入来源**: `BRD.md`、`PRD_biz_part.md`、`case-data-stat.md`、`case-samples.md`、`07-engineering-constraints.md`

---

## 1.1 Business Input

### 目标

- **业务目标**: 在 Gumtree 官网 Salesforce Enhanced Chat 上引入自定义 Chat Bot 作为首触达，通过 FAQ 自助与引导式对话提升自助解决率，降低坐席重复性工作量。
- **服务用户**: 所有通过 Web Enhanced Chat 发起咨询的 Gumtree 用户（买家、卖家、新老用户）。
- **成功的业务结果**: D1 全自动结案 10%–25%；D2 首解/显著减负 45%–55%；减少坐席重复解释工作；CSAT 不下降。

### Scope

- **In-scope 场景（Phase 1，代号 A–F'）**:
  - A: 帖子/广告状态与可见性（5,506 条，4.6%）
  - B: 发帖与编辑指引（3,160 条，2.6%）
  - C: 消息与回复（2,627 条，2.2%）
  - D: 账户与登录-非敏感执行（4,906 条，4.1%）
  - E: 通用产品与搜索（3,811 条，3.2%）
  - F: 支付咨询-非争议执行（488 条，0.4%）
  - F': 正确删除/合规下架话术（20,875 条，17.3%）
- **Out-of-scope 场景**: 语音、社交通道、支付/退款/强身份自动化、复杂纠纷/法律/欺诈自动裁决、开放式闲聊。
- **高风险场景**: 支付争议/退款执行、GDPR 数据删除、误删申诉、欺诈判定。
- **必须转人工场景**: 用户主动要求、高风险类别、申诉/复核、情绪激动/威胁、faq_miss ≥ 2、澄清 2 轮仍无法理解。

### Use Cases / Cases

- **高频 use cases**: GDPR/Data Deletion（24.6%）、Ads 类（22.9%）、Technical Issue（5.8%）、Trust & Safety（5.2%）、Accounts（4.3%）。
- **典型用户提问样本**: 见 `case-samples.md`（120,367 条原始数据，Sub Reason 约 200 种）。
- **历史 bad cases**: 待从线上积累，一期上线后建立 bad-case bank。
- **真实数据来源**: Salesforce Case 报表 `report1768917670361.xlsb`。

### Success Metrics

| 指标 | 目标 |
|------|------|
| **containment** | D1 全自动结案 10%–25%；D2 有效承接/分流 45%–55% |
| **escalation quality** | 转人工上下文完整度 ≥ 98%；escalation recall ≥ 95% |
| **user satisfaction proxy** | AI 满意度 40%–70%；CSAT 不低于人工基线 |
| **latency** | FAQ answer p95 ≤ 5s |
| **cost** | 当前约 4.1 FTE / £65K/年 chat capacity；目标通过 containment 释放部分产能 |

---

## 1.2 Domain Knowledge Input

- **权威知识源**: Salesforce Knowledge（与 Help Centre 同步）。
- **可用于 grounding 的内容范围**: Help Centre 文章、Community Standards / 政策页面、操作指南。
- **不能作为事实源**: 用户自由输入内容、未审批的 Git 内部文档（需治理审批后可选纳入）。
- **freshness 要求**: 随 Salesforce Knowledge 发布状态同步。
- **ownership**: Knowledge & Content Ops 团队。
- **当前已知 knowledge gaps**: Case Reason/Sub Reason 为空约占 12%（约 15,113 条），需意图模型补全。

---

## 1.3 Operating Model Input

- **转人工给谁**: Customer Support Agents（通过 Omni-Channel 路由）。
- **人工系统 / queue**: Salesforce Service Console / Omni-Channel 既有队列。
- **handover 后流程**: 坐席在同一会话线程中接续；坐席可见结构化上下文（intent、confidence、articles_shown、caseId、transcript）。
- **工作时间限制**: 有离线时段（out of hours 时 Bot 可继续 FAQ，复杂问题记录后续跟进）。
- **SLA / TTFR**: 现有 AHT 469s；目标降低转人工后首次响应时间。
- **QA / Ops owner**: Customer Service Management + Knowledge owners。
- **bad-case bank owner**: QA / Ops 团队，weekly review cadence。

---

## 1.4 Engineering Constraint Input

> 以下约束从 Gumtree 平台现有代码库（`/projects/gums`）提取，详见 `07-engineering-constraints.md`。Bot 服务应与已有平台模式对齐。

### 1.4.1 语言与框架

| 维度 | 约束 |
|------|------|
| **主语言** | **Java 17 + Spring Boot 3.x**（参考平台最现代服务 dpe: Spring Boot 3.2.5 + Java 17）|
| **备选** | Kotlin + Ktor 2.3（若团队偏好）|
| **构建工具** | Maven（与多数 Java 服务对齐）或 Gradle |

### 1.4.2 数据库与存储

| 组件 | 选型 | 说明 |
|------|------|------|
| **关系型 DB** | **PostgreSQL on GCP Cloud SQL**（+ cloud-sql-proxy sidecar）| 全平台标准；用于 session state、audit log、bot 配置 |
| **缓存** | **Redis** | 平台已有（bapi, capi, gdpr-orchestrator）|
| **Salesforce 侧对象** | Bot_Session__c、Bot_Event__c、Case | 报表与坐席可见数据落在 Salesforce |

### 1.4.3 向量检索

| 现状 | 说明 |
|------|------|
| **平台无已有向量搜索基础设施** | 当前搜索为 Elasticsearch 8.x 关键词/分面检索（livead-search），无 Pinecone/Weaviate/pgvector 引用 |
| **必须从零搭建** | FAQ 向量库为本项目新增能力 |

**候选方案（GCP-only 约束下）**:

| 方案 | 优势 | 劣势 |
|------|------|------|
| **pgvector on Cloud SQL PostgreSQL** | 复用已有 Cloud SQL 基础设施，运维成本低 | 大规模 ANN 性能受限 |
| **Vertex AI Vector Search** | 托管、原生 GCP、自动扩缩 | 供应商锁定，额外成本 |
| **Self-hosted on GKE**（Qdrant / Weaviate） | 灵活，功能丰富 | 需自行运维，GKE 资源开销 |

**决策状态**: TBD — 需评估一期 FAQ 规模后选型。

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

**Bot 项目额外需求**: regression eval gate 集成到 Jenkins pipeline — 每次 PR 运行 smoke regression，每次 release candidate 运行 full regression。

### 1.4.7 可观测性

| 层 | 技术 |
|---|------|
| **日志** | Logback + SLF4J → Logstash appender → GCP Cloud Logging |
| **指标** | Micrometer → Prometheus（PodMonitoring on `/internal/metrics`）|
| **错误追踪** | Sentry / Raven |
| **链路追踪** | 通过 Istio sidecar / GCP Cloud Trace（待显式确认）|

**Bot 项目额外需求**: Bot_Event__c 事件日志 + 结构化 trace schema（trace_id / session_id / turn_id / prompt_version / model_version / projection_version / active_use_case / action_selected / tool_calls / source_ids / outcome）。

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

- **Multi-repo**: 每个服务独立仓库。
- **Bot 项目**: **新建 greenfield standalone repo**，遵循现有约定 — 独立 Helm chart、Jenkinsfile、Dockerfile、contract 目录。
- **标准目录**: `/server`、`/helm-chart`、`/contract`。

### 1.4.11 合规与安全

- UK GDPR 合规；PII 脱敏。
- 不存储用户密码。
- 日志脱敏（敏感字段 redacted）。
- Chat transcript 按 Gumtree 数据保留策略存储。

### 1.4.12 外部系统集成

| 集成目标 | 接口 | 说明 |
|----------|------|------|
| Salesforce Knowledge | Knowledge API | 权威元数据、发布状态；配合向量库语义召回 |
| Salesforce Case | REST / Composite / Apex REST | 创建 Case，字段与队列规则与现有一致 |
| Salesforce Messaging Session | Embedded Service / Messaging API | Bot 回复写回同一聊天线程 |
| Salesforce Omni-Channel | Chat Transfer / Omni-Channel API | 转人工 + 传递自定义字段 |
| FAQ 向量库 | 内部 REST API | 语义检索 Top-K |

---

## 1.5 Evaluation Input

- **golden dataset**: 基于 A–F' 高频场景构建（含 task_id / channel / turns / expected outcome）。
- **clarification dataset**: 模糊表达、缺失关键信息、query 过宽、同义表达。
- **escalation dataset**: user asks for human、policy-required、unsupported requests、insufficient grounding、repeated miss。
- **bad-case bank**: 一期上线后从 weekly human review 和线上采样积累。
- **release threshold**: 见 `phase0_normative_freeze.md` release gates。
- **weekly review mechanism**: 抽样审核已解决/已升级/abandon/低满意度/高风险边界会话，输出进 bad-case bank。
