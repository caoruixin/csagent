# Phase 0 — Freeze Normative Layer

> 锁定母规范，确认本项目不得漂移的内核约束。
>
> **规范来源**:
> - `customer_service_agent_tech_spec.md`（Whole Tech Spec — V1 内核 / 架构 / 控制 / 工具 / Handover / Guardrails / Observability / NFR / Release Criteria）
> - `customer_service_agent_eval_spec.md`（Eval Spec — Eval scope / Dataset / Grader / Metrics / Launch Gates / CI/CD）
> - `customer_service_tool_spec_v0_2.yaml`（Tool Spec v0.2 — V1 tool surface、visibility、risk_tier、per-UC 可用性、concrete API mapping、runtime_policy、runtime capabilities；与母规范一致并对其做领域具化）
> - `salesforce-part-spec.md`（Salesforce 组织配置确认 — Enhanced Chat / Omni-Channel / Knowledge API / Case 字段 / Queue / Off-hours / Pre-chat Form / 自定义对象）
> - `problem_retrieval_solution_plan_pgvector.md`（pgvector 向量检索方案确认 — 分块策略 / 索引 / 在线检索流程）
> - `platform_api_detailed_reference.md`（平台 API 详细参考 — 196 端点 / 14 微服务）

---

## 0.1 这一步的目标

确认哪些内容是"母规范"，后续项目不得随意漂移。本 Gumtree CS Bot 项目继承通用 Customer Service Agent 规范，并明确：

- **不可改写**: V1 范围、控制内核、LLM tool-use 契约（含 agent_visible 工具集合与 per-UC 可用性矩阵）、handover payload contract、release gates、eval suite 类型。
- **可领域化**: use case 列表、knowledge scope、escalation 触发条件细节、风险等级判定、话术与品牌口径、外部系统集成（Salesforce / GCP）。

---

## 0.2 对 Customer Service Agent 固定的共性约束

来自 `customer_service_agent_tech_spec.md` §5 设计原则与 §11 控制内核，以及 `customer_service_tool_spec_v0_2.yaml` §principles：

- single agent + bounded loop（§5.2 / tool_spec `agent_pattern: single_agent_bounded_loop`）
- state outside, context projected（§5.4 / §8 / §10）
- grounded before generative（§5.5 / §14 / tool_spec `grounding_policy: grounded_before_generative`）
- escalation is first-class outcome（§5.6 / §15 / tool_spec `escalation_policy: escalation_is_first_class`）
- **tool surface strategy = small_and_strong**（tool_spec `tool_surface_strategy` / tech_spec §13）— agent_visible 工具数量保持最小、职责清晰、边界强约束，不堆叠可选项
- **sensitive write actions = human_only_or_phase2**（tool_spec `sensitive_write_actions`）— 高风险写操作（删帖、限号、账号限制、异步邮件等）不暴露给 V1 Bot 自由调用
- eval before expansion（§5.7）
- kernel first（§5.1）— 先把控制内核、上下文投影、升级、评测做对，再扩展业务能力层
- harness-centric reliability（§5.3）— 系统可靠性主要来自 harness，而不是 prompt 本身
- V1 优先高频、低中风险、边界清晰场景（§4.1）
- V1 不做复杂多步骤流程编排（§4.2）
- V1 不做高风险自动决策（§4.2）
- 必须有 handover contract（§15）
- 必须有 minimal observability（§17）
- 必须有 release gates（§19）

---

## 0.3 本项目显式继承的规范项

| 规范项 | 继承内容 | 规范源 |
|-------|---------|--------|
| **V1 scope boundary** | In-scope: FAQ、产品/功能说明、操作指导、1–2 轮澄清、明确转人工。Out-of-scope: 复杂多步流程、高风险自动决策、复杂写操作、多 agent、长链路事务、跨渠道生命周期治理、精细化团队路由。 | tech_spec §4.1, §4.2 |
| **control kernel state machine** | INIT → DISCOVER → RESOLVE → CONFIRM → CLOSE / ESCALATE | tech_spec §11.2, §11.3 |
| **LLM 响应契约 (V1)** | LLM 输出采用 OpenAI-style 原生 tool-use 格式 `{user_message, reasoning, tool_calls: [{name, arguments}]}`。意图通过 tool_calls 内容隐式表达（无独立 action 词表）。可调用工具范围由本表 "V1 tool set (agent_visible)" + per-UC 可用性矩阵硬约束。V1 默认不做 `sub_agent_call`、unrestricted multi-tool composition、workflow chaining；不得直接调用 human-only 工具。**[DEVIATION 2026-05-01 — 见 §0.6；由原 5-action 抽象层迁移至单一 tool-use 层。]** | tech_spec §11.4 + §0.6 deviation log |
| **control budgets** | `max_clarification_rounds`、`max_faq_miss`、`max_bot_turns_per_issue`、`max_repeated_same_action`、`max_total_bot_turns_before_forced_escalation` | tech_spec §11.5 |
| **drift handling semantics** | minor drift（保持 use case）/ soft shift（切换 active issue 但保留主问题）/ hard shift（升级或切策略） | tech_spec §11.6 |
| **state model layers** | External State / Session State / Memory（V1 极轻）/ Context（每轮投影） | tech_spec §8 |
| **context projection contract** | 输出包含 `task_summary`、`active_use_case`、`candidate_use_cases`、`recent_messages`、`retrieved_knowledge`、`risk_flags`、`budget_state`、`tool_schemas`（per-UC 可见 tool 列表 + 参数 schema，由 ToolPolicyEnforcer 按 active_use_case 过滤）、**`form_context`**（v0.2 新增 — pre-chat form 数据在 INIT 阶段即可用）。**[DEVIATION 2026-05-01 — 见 §0.6；移除 `allowed_actions` 字段，UC × phase 约束统一由 tool 可用性矩阵承担。]** | tech_spec §10.5 / tool_spec_v0.2 `form_context_ingestion` / §0.6 deviation log |
| **use case registry lite schema** | use_case_id / name / description / example_user_requests / knowledge_scope / risk_level / allow_clarification / allow_bot_resolution / escalation_conditions / outcome_class（per-UC 可调用工具范围由本表 "per-UC 工具可用性矩阵" 强约束，不再单独列举 `allowed_actions`）。**[DEVIATION 2026-05-01 — 见 §0.6。]** | tech_spec §9.2 / §0.6 deviation log |
| **V1 tool set (agent_visible)** | `search_knowledge`、`resolve_article`、`get_customer_context`、`request_handover`、`record_outcome`。每个工具必有 stable name / clear description / parameter schema / permission boundary / standardized success-error payload | tech_spec §13.1, §13.2 / tool_spec §tools |
| **V1 tool set (runtime_only，模型不可直接调用，v0.2.1 = 5 个)** | `create_case_controlled`（仅 UC-H/UC-J/UC-K 允许）、`lookup_customer_account`（由 `get_customer_context` 组装）、`lookup_listing_or_ad`（由 `get_customer_context` 组装）、**`get_moderation_review_context`**（v0.2 新增，仅 UC-A/UC-FP，由 `lookup_listing_or_ad` 链式调用，提供广告删除/审核原因的 grounded 事实依据）、**`get_message_moderation_context`**（v0.2.1 新增，仅 UC-C，由 `get_customer_context` 在 UC-C 消息诊断场景下链式调用，判断消息是否被平台审核拦截）。由 runtime 策略驱动，不暴露给模型上下文或 prompt。 | tool_spec_v0.2.1 §tools (visibility: runtime_only) |
| **V1 runtime capabilities (non-tool，v0.2 新增)** | `fixed_script_library`（管理式模板库，**合规审批已通过（v5）**，为 UC-G/H/I/J/K 无 knowledge retrieval 场景 + 通用 opening/closing/escalation 提供固定话术）、`form_context_ingestion`（INIT 阶段解析 pre-chat form 数据写入 session state，自动触发 `get_customer_context`）、`tool_policy_enforcer`（每次 tool call 前检查 UC 可用性，violation 返回 `scope_blocked`）、`progress_placeholder`（tool call 延迟 >1.5s 时发送占位消息）。这些是 runtime 层内建能力，不是 LLM 可调用的工具。 | tool_spec_v0.2 §runtime_capabilities |
| **human-only tools (Phase2 或人工触发)** | `moderation_enforcement_action`（删帖 / 限号 / 账号限制，critical risk）、`send_followup_email_or_async_update`（异步邮件更新，medium risk；**邮件规则/模版/内容遵循现有 Salesforce 人工 CS 系统，Bot 不直接向用户发送邮件**，由坐席 / back-office 触发）。Phase 1 Bot 不得直接或间接调用。 | tool_spec §tools (visibility: human_only) |
| **per-UC 工具可用性矩阵** | 每个 tool 的 `allowed_use_cases` / `disallowed_use_cases` 作为硬约束；超范围调用由 runtime 拒绝并记录 `scope_blocked` | tool_spec §tools.*.allowed_use_cases |
| **tool risk tier 与 runtime_policy** | low（knowledge）/ medium（composite read / controlled write）/ high（`create_case_controlled`）/ critical（`moderation_enforcement_action`）；每类风险对应 runtime policy（retries、must_log_*、must_validate_required_fields 等） | tool_spec §tools.*.risk_tier, runtime_policy |
| **V1 use case 集合（Topic Subject → UC 两层分类，v8）** | **12 个 UC + 4 个 OUT_OF_SCOPE 分类按 Pre-chat Form Topic Subject 组织**：Ad Support → UC-A/B/FP/H；Account Support → UC-D（⚠️ v8 HR: 路由准确率仅 18.2%，实际 spillover 覆盖全部 UC）；Delete My Account or Data → UC-G；Payments → UC-F/I；Replies or Messaging → UC-C；Report a Safety Issue → UC-J；Technical Support → UC-E/K。每个 UC 有 `parent_topic_subject` 字段标识业务归属。**4 个 Handover-only Topic Subject**（Delivery / Pro Contract / Account Manager Support / Ratings Reviews）→ 不匹配已有 UC 时标记 `OUT_OF_SCOPE_DELIVERY` / `OUT_OF_SCOPE_PRO_CONTRACT` / `OUT_OF_SCOPE_ACCOUNT_MANAGER` / `OUT_OF_SCOPE_RATINGS_REVIEWS` + 固定话术 + `request_handover`（v8 HR: 19 条 OOS，全部 100% escalation）。UC-A..F, UC-FP 为 Bot 可 FAQ-resolvable；UC-G..J 为 V1 intake-then-handover（Bot 不自主 resolve）。**[DEVIATION 2026-07-25 — 见 §0.6；UC-K 由 intake-then-handover 改为 `path: PARTIAL`（可自主 FAQ resolve **且**保留 intake），回归 `phase2_domain_realization_spec.md:493` 一直声明的 `allow_bot_resolution: partial`。]** | tool_spec §use_cases / phase2 §2.11 / §0.6 deviation log |
| **handover trigger policy (V1 minimal)** | user explicitly requests human / high-risk use case / clarification budget exhausted / faq miss threshold exceeded / policy requires escalation / system confidence-grounding insufficient。**[DEVIATION 2026-07-25 — 见 §0.6；`BRD.md:126-138` 的 "customer indicates frustration" 明确**不是**确定性触发器：挫败降级为投影给 LLM 的 advisory 软信号（`user_sentiment_signal`），由 LLM 判断诉求类别后决定；fraud / safety / legal / imminent-harm 保留立即转人工。]** | tech_spec §15.2 / §0.6 deviation log |
| **handover payload contract** | session_id / primary_use_case / candidate_use_cases / current_status / summary / clarification_count / faq_miss_count / articles_shown / escalation_reason / transcript_ref / **form_topic_subject**（v7 新增）/ **topic_uc_mismatch**（v7 新增） | tech_spec §15.3 / phase2 §2.11.6 |
| **mandatory guardrails** | 明确 bot 身份、不伪装人工、不在无依据时编造答案、不处理高风险自动裁决、不做越权承诺、只暴露最小必要信息 | tech_spec §16.1 |
| **versioned configs (V1)** | prompt version / projection policy version / control policy version / tool schema version / eval suite version | tech_spec §16.2 |
| **required event types** | session_started / use_case_inferred / retrieval_executed / article_shown / clarification_asked / escalation_requested / outcome_recorded / session_closed | tech_spec §17.1 |
| **trace fields** | trace_id / session_id / turn_id / prompt_version / model_version / projection_version / active_use_case / tool_calls / source_ids / outcome。**[DEVIATION 2026-05-01 — 见 §0.6；移除 `action_selected`。需要"语义动作"分析时由 tool_calls 派生（如 tool_calls 含 `request_handover` ⇒ 升级；tool_calls 为空且 user_message 非空 ⇒ 提问/答复）。]** | tech_spec §17.2 / §0.6 deviation log |
| **minimal funnel** | total sessions / understood / resolved by bot / escalated / abandoned / wrong containment / repeat-contact proxy / latency p50-p95 / cost per conversation | tech_spec §17.3 |
| **NFR baselines** | FAQ answer p95 ≤ 5s；escalation request p95 ≤ 3s；median turns for solved FAQ ≤ 6；handover request 可重试；state write 可校验；degraded mode 落到安全升级；最小数据暴露；redacted logs；secret 隔离；retention 合规 | tech_spec §18 |
| **Salesforce 平台硬约束（已确认）** | Enhanced Chat Web v1；自研 Bot **最多 50 次会话轮次**（超过必须 transfer to human）；Omni-Channel 标准通道；**Knowledge API 当前无开发 API 能力**（不可用于在线检索，knowledge grounding 完全依赖 pgvector 离线索引）；Pre-chat form 提交后**先创建 Case 再进入 Chat 路由**（Bot 的 "turn 0" = form data）；坐席最大并发 2 会话；**Queue 路由统一（v5 已确认）**：online → CS_NEW_chat，offline → CS_Cases_New，所有 UC 遵循同一规则，不设 per-UC 专属 queue；**Bot_Session__c / Bot_Event__c 追加创建（v5 已确认）**：在现有 Chat_Message_Log__c 基础上追加，升级为 session + event 显式状态模型，便于 control kernel / observability / replay | salesforce-part-spec.md |
| **向量检索选型（已确认）** | **pgvector on Cloud SQL PostgreSQL**；chunk 策略 256–512 tokens sliding window + 10–20% overlap；article-level + chunk-level 双层表；**Embedding 模型：Vertex AI `text-embedding-004`（GCP native，v6 已确认）**，768 维度，入库与在线 Query 使用同一模型与同一维度；**索引：HNSW + cosine（v5 已确认，优先 recall）**：`m=16, ef_construction=64`，查询 `hnsw.ef_search=100`，开 `hnsw.iterative_scan=relaxed_order`（应对 filter 场景漏召回）；**faq_miss 两段式判定**：Retrieval Gate（向量候选质量 / metadata 命中 / 候选分布）→ Answer Gate（`grounding_score < 3.5` 判弱命中/不可答）；不以原始向量距离直接判 faq_miss | problem_retrieval_solution_plan_pgvector.md §3.3 / §7 |
| **平台 API 基线（已确认）** | 196 REST 端点 / 14 微服务（bapi-server 80 / gumshield-api 38 / user-service / advert-service / livead-search 等）；tool_spec_v0.2 所有 `concrete_api_dependencies` 均已映射到实际端点 | platform_api_detailed_reference.md / tool_spec_v0.2 |
| **eval suites (P0)** | Core E2E Suite + Control Sanity Suite + Grounding & Policy Suite + Handover Contract Suite | eval_spec §6.1 |
| **eval suites (P1)** | harder drift / multi-turn issue switching / knowledge gap analytics / replay-driven bad-case mining | eval_spec §6.2 |
| **launch gates (V1)** | active use case accuracy ≥ 85%；candidate recall ≥ 95%；groundedness pass rate ≥ 98%；critical policy violation = 0；escalation recall ≥ 95%；wrong containment ≤ 2%；handover completeness ≥ 98%；repeated same tool-call rate below threshold（由 `max_repeated_same_tool_call` 控制预算强制，门槛与原 "repeated same action rate" 等价）；median turns for solved FAQ ≤ 6；FAQ answer p95 ≤ 5s。**[DEVIATION 2026-05-01 — 见 §0.6；门槛不变，仅术语对齐。]** | eval_spec §11 / §0.6 deviation log |
| **what triggers eval** | model version / prompt version / control policy / projection logic / tool schema / retrieval index / knowledge source / handover schema 任一变更 | eval_spec §4 |
| **CI/CD gates** | PR 触发 smoke regression（core routing / required escalation / grounding safety / handover schema）；release candidate 触发 full regression；release blockers 任一触发即阻断 | eval_spec §15 |
| **release criteria (V1)** | core bounded loop implemented / use case registry lite implemented / projection policy fixed and versioned / handover payload fixed / critical eval suite runnable / critical policy violation = 0 / regression gate in CI / minimal analytics funnel available | tech_spec §19 |
| **deferred to V1.1** | hybrid orchestration、lightweight procedures、knowledge readiness workflow、decision path / replay tooling、stronger analytics breakdown | tech_spec §20.1 |
| **deferred to V2** | full configuration versioning、handback lifecycle、richer routing policy engine、broader channel lifecycle、advanced procedural automation | tech_spec §20.2 |

---

## 0.4 本项目可领域化的部分（不构成漂移）

| 可领域化项 | 在本项目的处理位置 |
|-----------|-----------------|
| 具体 use case 列表与边界 | `phase2_domain_realization_spec.md` §2.2 |
| 风险等级判定与 forbidden 行为 | `phase2_domain_realization_spec.md` §2.3 |
| 升级触发条件细节 | `phase2_domain_realization_spec.md` §2.4 |
| Knowledge scope 映射 | `phase2_domain_realization_spec.md` §2.5 |
| 控制策略的领域 override | `phase2_domain_realization_spec.md` §2.6 |
| per-UC 工具可用性矩阵与 runtime policy 细化 | `phase2_domain_realization_spec.md` §2.10（与 `customer_service_tool_spec_v0_2.yaml` 对齐）|
| 品牌口径与对话话术 | `phase1_solution_input_pack.md` §1.1（业务输入）+ `phase2_domain_realization_spec.md` §2.6（话术 override） |
| 外部系统集成（Salesforce、GCP、Knowledge API） | `phase1_solution_input_pack.md` §1.4（工程约束）|
| Bot 标识符采集策略（email / ad_id / phone 等 slot 优先级） | `phase2_domain_realization_spec.md` §2.6（clarification policy override）|
| UC-G/H/I/J/K 的 intake 字段与 `create_case_controlled` required_fields 合约 | `phase2_domain_realization_spec.md` §2.10（per-UC case 合约）|
| 试点流量比例与 go/no-go 阈值 | 待 BRD/PRD 业务方确认 |

---

## 0.5 漂移防护

任何后续阶段若需修改 §0.3 中表格内的项，必须：

1. 显式说明偏离的规范项与原因。
2. 评估对 release gates 与 eval suite 的影响。
3. 经规范层 owner 确认后更新本文件，并在 git history 中保留变更记录。
4. 在 §0.6 漂移登记新增条目，对偏离做永久性、可追溯的记录。

否则视为违反母规范。

---

## 0.6 漂移登记 (Deviation Log)

按 §0.5 流程登记的所有母规范偏离。每条记录包含变更范围、原设计、新设计、原因、对 release gates / eval suite 的影响、owner 确认、实施引用。

### Deviation 2026-07-25 — 挫败不再是升级触发器 + UC-K 归位到 partial path

**变更范围**: §0.3 表格行 "handover trigger policy (V1 minimal)"（移除"customer indicates frustration"作为确定性触发器；该条目原文写在 `BRD.md:126-138`，未写进本表，但 runtime 侧已有可执行落点）、§0.3 表格行 "V1 use case 集合（Topic Subject → UC 两层分类，v8）"（"UC-G..K 为 V1 intake-then-handover（Bot 不自主 resolve）" → UC-K 改为 partial：可自主 FAQ resolve **且**保留 intake）。本条统一登记产品负责人 2026-07-25 三项决策中的 D2 与 D3/A3（D1 只读用户数据状态查询提升进当前范围，不修改 §0.3 任何行，故不单列条目——见下方"D1 附注"）。

**触发**: `docs/proposals/performance_priority_replan_2026-07.md`（rev 2）§1.2 B1 与 §1.1 A3。产品负责人两个体感症状——"该自己做完的没做完 / 不该马上转人工的马上转了"——经代码核查后定位到两条成文但已过期的业务规则，而非 agent 措辞问题。

---

**D2 — "customer indicates frustration ⇒ must escalate" 修订为"先解决再转"**

**原设计**: `BRD.md:126-138` 原文 "The bot must escalate after: 2 failed intent recognitions, OR customer requests an agent, OR **customer indicates frustration**, OR the issue falls into an out-of-scope category."

**新设计**: 用户表达挫败时，若其诉求仍属可解释 / 只读可查询类（状态查询、政策或流程解释、how-to、对用户自有数据的只读查询），则共情安抚并**在本轮继续尝试解决**；仅在（a）用户明确要求人工，或（b）已做过一次真实尝试并再次失败且无其他可试路径时才转人工。高风险话题——欺诈 / 诈骗、人身安全或威胁、迫在眉睫的伤害或自伤、法律或正式投诉诉求——**保留立即转人工的确定性硬规则，不受语气影响**。

**原因**:
1. **"挫败"是软语义判断，不是布尔触发器**。是否应转人工取决于用户此刻究竟在要求什么，只有 LLM 能读出这一点；Constitution §1.3 把 "escalation posture" 划归 LLM，§1.5 / §1.7 明确禁止用关键词 / regex / enum 表实现软语义决策。
2. **原规则在 runtime 中有且仅有一处可执行落点，而那处判的是语气不是诉求**：`config/risk-keywords.yaml` 的 `escalation-pattern` 备选列表以裸名词短语 `customer service` / `live support` 结尾，因此 "your customer service is useless" / "this is the worst customer service I've had" 会命中 `DriftDetector` → `USER_ESCALATION_REQUEST` → `ControlKernel.processMessage` step 4 `forceEscalate()` 立即转人工。这正是"抱怨客服"被读成"要求客服"的误杀。
3. **抱怨的内容恰恰是"被转走"**。以道歉开场、以排队结束，是用户已经在抱怨的那件事；先给出真实答案才是真正的降级。

**实施（server 侧，本次落地）**:
- `server/src/main/resources/config/risk-keywords.yaml` — `escalation-pattern` 收紧：`customer service` / `live support` 两个名词短语现在必须前置请求动词（`talk|speak|transfer|connect|put me through|get me|escalate`）才命中；真实请求形态（"connect me to customer service" / "put me through to live support" / "I want to speak to customer service"）全部仍然命中。文件头新增治理注释：本文件只承载确定性面（显式要人工 + fraud/safety/GDPR/payment/appeal 硬切换），**挫败刻意不在此表，且禁止以关键词形式加入**。
- `server/src/main/java/.../runtime/EscalationReasonResolver.java` — 新增 `static hasDistressSignal(String)`（`detectDistressSignal` 委托给它，检测逻辑单点未变），供投影层调用而无需注入该 bean。
- `server/src/main/java/.../runtime/ContextProjectionBuilder.java` — 新增 `user_sentiment_signal` 投影槽（`frustration_detected` / `binding: advisory` / `note`）。无信号时整槽省略，常态路径投影与改动前逐字节一致。这就是"降级为软信号"的落地形态：确定性检测器仍然运行，但其输出交给 LLM 判断，不再花在控制决策上。
- `server/src/main/resources/prompts/system_prompt.txt` — 新增 "Customer frustration" 段落陈述上述策略（原则，不是关键词清单）；高风险例外明确列出。
- **runtime 未新增任何 force-escalate 路径**；`ControlKernel` step 2.4 原本就只把 `user_distress` 作为 escalation_reason 的优先级 stamp，不触发终止（该行为未改动）。

**未在本次改动、已登记为残留**: `server/src/main/resources/scripts/templates.yaml` 的 `h_frustration_ack` 模板文案为 "I completely understand your frustration ... Let me connect you to our team right away."，与 D2 相悖。该模板当前**无任何调用点**（`PhaseEvaluator.INTAKE_OPENING_TEMPLATES` 只引用 `h_empathy`），且其文案受 `docs/fixed_script_library_v1.md` §3 + `Sprint15ScriptLibraryConsistencyTest` 的 version-pin 约束，改文案需同步该文档并 bump library_version——超出本次 server-only 范围，留给脚本库 owner。

---

**D3 / A3 — UC-K 从 `path: INTAKE` 归位到 `path: PARTIAL`**

**原设计**: `config/use-case-registry.yaml` 将 UC-K（Technical Issue Intake）登记为 `path: INTAKE` + `allow-bot-resolution: false`；`PhaseEvaluator.java:1017` 原文注释 `// Intake UCs (UC-G/H/I/J/K) must NEVER invoke knowledge search`。后果：任何被判进 UC-K 的解释 / 诊断类问题（技术故障排查）在架构上不可能被自主答完——它连知识库都不许查，`tool-policy.yaml` 也未给 UC-K `search_knowledge` / `resolve_article` 权限。

**与母规范的关系**: 这是**回归**而非新增偏离。`phase2_domain_realization_spec.md:493` 一直写的是 `allow_bot_resolution: partial`（"能通过 get_customer_context 回拉状态解释的走 bot；需后端投诉的走 intake + case"），且同处声明了 UC-K 的 `knowledge_scope: help_centre_troubleshooting / app_issues / browser_support`。§0.3 表格行的 "UC-G..K 为 V1 intake-then-handover" 归纳与该 UC 级规范不一致；本条以 phase2 为准。

**新设计**: registry 新增第三个 `path` 值 `PARTIAL` = 可检索知识**且**可收集 intake 字段，由 LLM 每轮判断用户的诉求需要哪一半。UC-K 成为唯一的 PARTIAL 用例。

**实施（server 侧，本次落地）**:
- `config/use-case-registry.yaml` — UC-K: `allow-bot-resolution: true`, `path: PARTIAL`。
- `config/tool-policy.yaml` — `search_knowledge` / `resolve_article` 的 `allowed-ucs` 各加入 UC-K（capability wiring，Runtime §1.4）。
- `skills/resolve_technical_diagnose_or_intake.yaml`（**新增**）— UC-K 专属 RESOLVE Skill：先诊断（entity context + troubleshooting 检索）后 intake。`SkillRegistry` 禁止两个 Skill 在同一 `(phase, use_case)` 上碰撞，因此 UC-K 只能是"迁移"而不是"同时属于两个 Skill"。guardrails 沿用与 FAQ Skill 相同的 `premature_resolve_outcome_guard` + `must_cite_source`，外加原 UC-K 的 `intake_complete_required`——grounding floor 与 intake floor 都未降低。
- `skills/resolve_intake_collect_and_handover.yaml` — 移除 UC-K 及其 `uc-k-intake-complete-before-handover` critical step（随 UC-K 迁移，内容不变）。
- **四处重复硬编码集合收敛**：`UseCaseRegistryService` 新增 `isIntakeOnlyPath` / `isKnowledgeCapablePath` / `collectsIntakeFields` 三个由 `path` 派生的谓词 + `PATH_FAQ|INTAKE|PARTIAL` 常量。`PhaseEvaluator` 删除自己的 `INTAKE_UCS` 常量，改读 registry（`:1017` 分流、`interpretRunResult` 的 `isIntakePlan`、`resolveMaxStepsReason` 的 step 1）。`ControlKernel` 与 `ResolveDispositionEvaluator` 因构造函数向后兼容面 / 静态工具类无 Spring 上下文，暂保留镜像集合但已收窄为 intake-only 四值，并由新增的 `UseCaseRegistryPathConsistencyTest` 在编译期外锁死漂移（该测试同时断言：任一 knowledge-capable UC 必须解析到能调用 `search_knowledge` 的 Skill；任一 intake-collecting UC 必须解析到带 `intake_complete_required` 的 Skill）。

**对 release gates 与 eval suite 的影响**:
- **eval 侧三处硬编码仍不一致，属本次范围外**（由并行的 WS-1/WS-2 agent 负责）：`eval_interactive/eval_interactive/case_spec/case_outcome_resolver.py:23` `_MANDATORY_ESCALATION_UCS` = {G,H,I,J}（不含 K，与新语义**已经一致**）；`eval_interactive/eval_interactive/scoring/hard_checks.py` 的 `INTAKE_UCS` = {G,H,I,J,K}（含 K，**现在与 server 冲突**——UC-K 的合法 grounded answer 会被判成 intake 违约；该文件正被 WS-1 并行修改，故此处不引行号）；`eval_interactive/eval_interactive/case_spec/policy_table.py:374-409` 已是 `partial`/`either`。
- `wrong containment ≤ 2%` 预期改善（D2 消除抱怨客服触发的误转；A3 让 UC-K 的可答问题真的能被答完）。
- `escalation_recall ≥ 95%` 有回退风险：CaseSpec 中标注 `escalation_trigger=user_distress` 且期望 escalate 的案例，若其诉求实际属可解释类，bot 现在可能正确地不转人工而被判 miss。这正是 rev 2 §3 WS-2 要重写的那批 spec；在 WS-1 落地前任何 pass-rate 都不可信（rev 2 §5）。
- `groundedness pass rate ≥ 98%`：UC-K 新增了检索路径，但同时继承了与 FAQ 路径完全相同的 `must_cite_source` guardrail，grounding floor 未降低。
- `critical policy violation = 0`：高风险硬规则（fraud / safety / GDPR / payment / appeal 硬切换，显式要人工）全部未改动。
- Java 测试基线：改动前 1422 run / 1 failure（inherited `SystemPromptUserRequestedTiebreakerTest`）→ 改动后同一处 inherited failure，无新增失败。

**owner 确认**: 产品负责人 2026-07-25 决策（D1 / D2 / D3 三项），记录于 `docs/proposals/performance_priority_replan_2026-07.md` §2。

**D1 附注（不修改 §0.3 任何行，故不单列条目）**: 只读的用户数据状态查询由 `PRD_biz_part.md:228` 的 Phase 1.x 提升进当前范围。落地为 `skills/resolve_faq_grounded_answer.yaml` 新增 UC-A mandatory critical step `consult-entity-context-before-status-answer`（键在既已 AGENT_VISIBLE 的 `get_customer_context`），写法对齐同文件既有的 UC-FP `consult-moderation-context-on-removal-explanation`。未引入新工具、未改 tool-policy、未改 §0.3 任何冻结项：`get_customer_context` 对 UC-A 本来就是可见的，缺的只是"在给出泛化 FAQ 答案之前必须先查用户自己的实体上下文"这条程序性要求。`lookup_listing_or_ad` 仍为 RUNTIME_ONLY 且无活调用点，本次未依赖它。

### Deviation 2026-05-01 — action 抽象层移除（dual model → single tool-use layer）

**变更范围**: §0.1 "不可改写" 第三项；§0.3 表格 5 行（"allowed actions (V1)"、"context projection contract"、"use case registry lite schema"、"trace fields"、"launch gates (V1)"）。

**原设计**: V1 在 LLM 响应层引入 5-action 抽象（`ask_user`、`retrieve_knowledge`、`answer_grounded`、`escalate_human`、`finish`）。LLM 输出 `{action, parameters, user_message, reasoning}`；`ActionParser` 校验 action ∈ VALID_ACTIONS；`PhaseEvaluator` 在每个 phase 内 5-分支 switch on action；context projection 注入 per-phase × per-UC `allowed_actions` 列表；`bot_turns.action_selected` 列持久化所选 action。

**新设计**: LLM 响应直接采用 OpenAI-style 原生 tool-use 格式 `{user_message, reasoning, tool_calls: [{name, arguments}]}`。意图（"是否升级"、"是否检索"、"是否结束"）由 tool_calls 内容 + user_message 是否为空隐式表达，不再额外维护 action 词表。`allowed_actions` 字段从 context projection 中移除；`tool_schemas` 升级为 per-UC 完整 tool schema（name + description + arguments schema），作为 LLM 工具发现的唯一信源。`bot_turns.action_selected` 列被 drop（一次性 Flyway migration）；trace 仅持久化 `tool_calls` JSON。

**原因**:
1. **行业对齐**: OpenAI Assistants、Anthropic tool-use、LangGraph 等主流 agent 平台均采用单层 tool-use 模型；csagent 双层模型为历史遗留，未带来额外控制收益。
2. **冗余消除**: action 与 tool 形成两套并行命名空间（如 `retrieve_knowledge` action 实际仅触发 `search_knowledge` tool），双重约束实质冗余且易漂移；per-UC `allowed_actions` 与 per-tool `allowed_use_cases` 两处独立维护，已观察到不一致风险（policy_table 注释明确写有 "drift test elsewhere can compare the two"，但该测试不存在）。
3. **enforcement 单点化**: 移除 action 层后，所有 UC × phase 约束统一由 `ToolPolicyEnforcer.isToolAllowed()` 承担；prompt-side 与 enforcement-side 同步成本归零。
4. **eval 简化**: Phase 5 已确认 CaseSpec 的 `allowed_actions` / `forbidden_actions` 为 dead fields（never read by scorers）；real action-correctness 检查全部基于 tool_calls，`source_citation_present` 唯一一处读 `action=answer_grounded` 的 L1 check 改为 grounding_mode + 非空 user_message 判定。
5. **debugging 可读性**: `tool_calls=[{name:"request_handover", arguments:{escalation_reason:"user_distress"}}]` 在 trace 中比 `action="escalate_human", parameters={...}` 更精确、更可定位。

**对 release gates 与 eval suite 的影响**:
- launch gate "repeated same action rate" → 改为 "repeated same tool-call rate"，门槛不变（由 `max_repeated_same_tool_call` 控制预算强制）。
- §6.6 control metrics 中 `action_selection_accuracy` / `termination_accuracy` 删除（已被 L2 `correct_outcome` + `tool_sequence_match` 等效覆盖）；`repeated_same_action_rate` 改名 `repeated_same_tool_call_rate`，定义不变。
- L1 `source_citation_present` 重写：原"if `action=answer_grounded` and grounding_mode=faq_source_backed → source_ids non-empty"，改为"if grounding_mode=faq_source_backed and user_message 非空 and tool_calls 不含 `request_handover` → source_ids non-empty"。
- 所有其余 L1 / L2 / L3 scorer 不受影响（已 tool-centric）。
- Interactive eval smoke 集合在 migration 前后回归对比：mean_outcome / mean_judge / policy_compliance 不应回退。

**owner 确认**: liuhe36@yahoo.com（2026-05-01 决策）。

**实施引用**:
- 同步更新文档: `phase1_solution_input_pack.md`（line 399 trace schema 引用 + line 160 budget 改名）、`phase2_domain_realization_spec.md`（删除 12 个 UC 的 `allowed_actions:` block + 重写 §2.6/§2.9）、`phase3_detailed_technical_design.md`（§3.1.4 / §3.2.6 / §3.3.3 / §3.8 共 7 子节）、`phase4_demo_coding_agent_implementation_packet.md`（D11.2 / D12.4 / D13.4 / D14.7 验收标准修订）、`phase5_evaluation_design.md`（§5.4 / §6.6 / CaseSpec dead-fields 标注）。
- server 代码迁移: `ParsedAction` → `LlmToolResponse`；`ActionParser` 改为 tool_calls 解析；`ContextProjectionBuilder.getAllowedActions()` 移除；`PhaseEvaluator` 5-action 分支重构；`bot_turns.action_selected` Flyway drop migration；`system_prompt.txt` 重写为 tool-use 契约。
- eval harness 迁移: `TraceData.action_selected` 移除；`hard_checks.py:404, :424` 重写为 tool_calls introspection；4 个 test fixture 更新。

### Deviation 2026-05-01 — smoke-run failure remediation (legacy escalation evidence + soft-OOS routing + ERROR retry threshold + DeepSeek v4 pro fallback)

**变更范围**: §0.3 表格行 "control kernel state machine"（DISCOVER 入口对 OUT_OF_SCOPE 的处理）、"handover trigger policy (V1 minimal)"（runtime ERROR 触发条件细化）、§0.6 关于"semantic actions are derived from tool_calls"契约的实施补丁（弥补 D16 双路径下的遗漏），以及 §0.3 表格行 "LLM provider (V1 demo)" 服务端 chat-completion 单 provider → primary+fallback 双 provider 形态变更。本条为统一登记 4 项最小修复 + 1 项延后跟进。

**触发**: `eval_interactive/results/20260501-140515/results.json` smoke run 暴露三个失败聚簇（B / C / D），调查后确认均为 D16 AgentRunLoop 重构后遗留的实施缺口；母规范契约本身不变，只补齐落地。修复四独立提出：服务端单 LLM provider 设计在 Kimi 瞬态故障下无优雅降级，与 §0.3 "Reliability — Degraded mode" 行的"LLM unavailable → ESCALATE"硬约束放在一起会触发不必要的人工升级。

**修复一（Cluster B+D — legacy `recordTurn` 缺失 `request_handover` tool_call 持久化）**: 当 `phaseAfter == ESCALATE` 时，legacy `ControlKernel.recordTurn()`（`server/src/main/java/.../control/ControlKernel.java:367-425`）目前仅写入 `search_knowledge` 到 `bot_turns.tool_calls`，未追加 `request_handover`，导致 legacy fallback 与 `forceEscalate()` 路径下 evidence 丢失，eval 的 `L1:escalation_compliance` 误判失败。修复方案：在 legacy `recordTurn` 中检测到 escalate 且 `tool_calls` 尚未包含 `request_handover` 时，追加 `{"name":"request_handover","arguments":{"escalation_reason": session.getEscalationReason()}}`；与 `ControlKernel.forceEscalate()`（同文件 `:253-304`）共用一个 `synthesizeHandoverToolCall` 私有 helper，确保两条路径行为一致。**Why not alternatives**: (a) 放宽 L1 检查 → 违反本节"semantic actions live in tool_calls JSON"契约；(b) 删除 legacy 路径强制走 AgentRunLoop → 违反 `agent.run-loop.enabled-phases` 的 D16.E rollback 安全网设计。

**修复二（Cluster B+C 3a — `SessionManager.createSession` 对 OUT_OF_SCOPE 一刀切立即升级）**: 当 `UseCaseRouter.route()` 返回 `OUT_OF_SCOPE` 时，`SessionManager.createSession`（`server/src/main/java/.../session/SessionManager.java:170-178`）当前一律设置 `currentPhase=ESCALATE / handlingState=QUEUE_TO_HUMAN / containmentOutcome=escalated`，绕过 DISCOVER。这对 hard OOS（Delivery / Pro Contract 等 handover-only registry 命中）是正确的，但对 soft OOS（如 `UNKNOWN_TOPIC`、Topic Subject 文本与 registry 拼写不完全一致）等同样立即升级，剥夺了 1 轮澄清机会。修复方案：在 `createSession` 内拆分 OOS 分支——hard OOS（命中 `UseCaseRouter` 显式 handover-only registry）保持当前行为；soft OOS（`UNKNOWN_TOPIC` / 模糊匹配）转入 `DISCOVER` 且 `activeUseCase=null`，留 1 轮澄清窗口。**Risk**: 可能回归 trust-and-safety 类必须立即升级的场景；mitigation：hard OOS 显式列表镜像自 UseCaseRouter handover-only registry，单点维护。

**修复三（Cluster B+C 3b — `PhaseEvaluator.interpretRunResult` 对 `AgentRunResult.ERROR` 一次即升级）**: `PhaseEvaluator.interpretRunResult()`（`server/src/main/java/.../control/PhaseEvaluator.java:489-517`）当前将任意 `AgentRunResult.ERROR`（projection 失败 / LLM 异常 / parser 失败等运行时错误）直接映射为 `ESCALATE`，未给瞬态错误重试空间。修复方案：仅当当前 phase 内 `runtimeErrorCount ≥ 2` 时才映射为 `ESCALATE`，否则保持 phase 不变并下一轮重新 prompt；新增 `BotSession.runtimeErrorCount` 计数字段（实施前先 grep 确认是否复用既有计数器），escalation reason 设为新 enum 值 `runtime_error_threshold`（22-value canonical enum 扩展为 23）。

**修复四（DeepSeek v4 pro 作为服务端 chat-completion 备选 provider）**: 当前 csagent 服务端 LLM 只有单一 provider Kimi 2.6（`KIMI_MODEL=kimi-k2.6` 见 `.env.local`，`server/src/main/resources/application-local.yml:16-19` `llm.kimi` 配置块），瞬态故障（timeout / 5xx / rate-limit 等）会全部经修复三的阈值后落到 `ESCALATE`，缺少更前置的优雅降级。修复方案：新增 **DeepSeek v4 pro** 作为 fallback；primary 仍为 Kimi 2.6，`LlmInvocationService` 在命中可重试的 transient 异常类时，自动以同 prompt 向 DeepSeek v4 pro 重试一次；两者均失败时再按既有路径产出 `AgentRunResult.ERROR`（再由修复三的阈值处理）。**配置面变更**（实施 PR 落地）：`.env.local` 新增 `DEEPSEEK_API_KEY`（required）、`DEEPSEEK_BASE_URL`（default `https://api.deepseek.com/v1`）、`DEEPSEEK_MODEL`（default `deepseek-v4-pro`）；`application-local.yml` 新增 `llm.deepseek` 配置块，结构镜像现有 `llm.kimi` / `llm.dashscope`（同文件 `:15-25`）。**Scope 边界**：fallback 仅作用于 chat completion（agent loop）；embedding 继续走 DashScope `text-embedding-v3`，不受影响；`eval_interactive` 自身使用的 judge / simulator LLM（DashScope `qwen-plus`，见 `eval_interactive.yaml`）也不受影响。**实施细节延后**：DeepSeek API OpenAI-compatible，可复用现有 `OpenAiCompatibleLlmClient` 仅 swap base-url/model/key，或新建 `DeepSeekClient` — 由实施者权衡；触发 fallback 的 transient exception 集合（哪些 5xx / 哪些 timeout / 是否包含 rate-limit）由实施者按 client 实际抛出的异常类列举。

**延后跟进 (Fix 3c — 不在本次实施范围)**: `PhaseEvaluator.evaluateDiscover()`（`server/src/main/java/.../control/PhaseEvaluator.java:256-281`）的 DISCOVER phase plan 缺乏显式 `classify_use_case` 工具，LLM 在 DISCOVER 阶段无法 commit UC。本条已识别为 known issue，待后续单独设计；本次仅在 deviation log 登记，不输出修复设计。

**对 release gates 与 eval suite 的影响**:
- launch gate `escalation_recall ≥ 95%` 与 `wrong_containment ≤ 2%` 预期改善（修复一直接消除 evidence 丢失误判；修复二消除 soft-OOS 立即升级造成的 wrong_containment；修复四在 Kimi 瞬态故障下避免不必要的 `runtime_error_threshold` 升级）。
- L1 `escalation_compliance` 检查不变（仍读 `tool_calls` 中的 `request_handover.escalation_reason`），但现在两条服务端路径（AgentRunLoop / legacy recordTurn）都将 evidence 写入 `bot_turns.tool_calls`，契约统一。
- `escalation_trigger` canonical enum 新增 `runtime_error_threshold`（由 22 → 23 个允许值）；CaseSpec 与 EscalationGrader 的枚举校验需同步更新。
- 新增 `BotSession.runtimeErrorCount` 字段不影响现有 trace schema；不需要 Flyway destructive migration（仅 ADD COLUMN）。
- 修复四对 eval suite 行为透明：fallback 触发与否由 `llm_call_log.model` 列体现（`kimi-k2.6` vs `deepseek-v4-pro`），既有 trace schema 不动；§3.8.5 startup log 与 per-request log 行需同时 echo DeepSeek provider/model。
- 修复四不影响 `eval_interactive` 自身的 LLM 调用（DashScope qwen-plus），不影响 embedding pipeline（DashScope `text-embedding-v3`）。

**owner 确认**: liuhe36@yahoo.com（2026-05-01 决策；实施仍待编码）。

**实施引用（待落地，本次仅文档登记）**:
- 同步更新文档: `phase3_detailed_technical_design.md`（§3.2.3 turn 持久化 + §3.3.1 / §3.3.2 state machine 入口 + §3.3.3 `interpretRunResult` 映射规则 + §3.6.1 trigger policy 行 + §3.8.5 LLM provider logging + §3.9.1 Reliability "Degraded mode" 行）、`phase4_demo_coding_agent_implementation_packet.md`（D0.3 `.env.local` 新增 DeepSeek 三个变量 + DM4 Key files 增加 fallback bullet）、`phase5_evaluation_design.md`（§7 L1 `escalation_compliance` 行 + §10 escalation_trigger enum 表）。
- server 代码改动（待实施，不在本 PR 范围）: `ControlKernel.recordTurn` + `forceEscalate` 提取 `synthesizeHandoverToolCall`；`SessionManager.createSession` 拆分 hard/soft OOS；`PhaseEvaluator.interpretRunResult` 引入 ERROR 阈值 + `BotSession.runtimeErrorCount` 字段；`LlmInvocationService`（或其底层 client wrapper）增加 Kimi → DeepSeek v4 pro fallback 路径，配套 `application-local.yml` 新增 `llm.deepseek` 块、`.env.local` 新增 `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL`。
- eval harness 改动（待实施）: `escalation_trigger` enum 列表加入 `runtime_error_threshold`；CaseSpec 枚举校验测试更新。

**Phase 2 follow-up — 2026-05-02 — forceEscalate canonicalization (#17 + #19)**:
- 将 round-1 已落地于 `PhaseEvaluator.interpretRunResult` + `ControlKernel.applyTransition` 边界的 escalation_reason canonicalization 不变量，扩展到第三个写入点 `ControlKernel.forceEscalate()`（`server/src/main/java/.../runtime/ControlKernel.java:99,110`），消除 smoke run 在该路径上残留的 `CONTRACT_VIOLATION:enum_violation` 噪音。
- `user_requested_escalation` → `user_requested`（drift / user-explicit-handover 路径，`processMessage` L110）。
- `budget_exceeded:max-repeated-same-action` → `turn_budget_exhausted`（budget-exceeded 路径，`processMessage` L99）。
- 关闭 smoke 集合中的 `cs_interactive_001`（budget 路径）与 `cs_interactive_029`（drift 路径）两个 CONTRACT_VIOLATION 案例。

**Phase 2 follow-up — 2026-05-02 — Fix 3c: classify_use_case tool**:
- 闭合 round-1 Step 3a（soft OUT_OF_SCOPE → DISCOVER routing）留下的执行缺口：DISCOVER 阶段 LLM 此前只能调用 `search_knowledge`，没有 commit UC 的工具，导致软 OOS 会话只能反复澄清直至预算耗尽，触发 `CONTRACT_VIOLATION:active_use_case missing_after_turns`。
- 新增 AGENT_VISIBLE 工具 `classify_use_case`（args: `use_case_id ∈ {UC-A..UC-K}`、`confidence ∈ [0,1]`、`reasoning: string`；effect: 校验后写入 `session.activeUseCase` + `session.intentConfidence`，发射 `CLASSIFICATION_COMMITTED` event；DISCOVER plan 唯一新增白名单），由 `PhaseEvaluator.interpretRunResult` 现有 DISCOVER → RESOLVE 边沿在 `activeUseCase != null` 时自动转移。
- 关闭 `cs_interactive_001 / 002 / 014 / 029 / 259` 五个 `active_use_case missing_after_turns` CV（remaining failures 退回到 L1/L2 真实质量信号）。

**Phase 2 follow-up — 2026-05-02 — #16: LLM canonical escalation_reason selection guidance + drift_hard_shift canonicalization**:
- 将 round-1 落地的 escalation_reason canonicalization 不变量从服务端写入点（`PhaseEvaluator.interpretRunResult` / `applyTransition` / `forceEscalate` 的 budget+drift+user-explicit 三路径 / `recordRunResult` 合成回退）进一步外推到 LLM-emit 边界：服务端枚举已是 canonical 23 值，但 LLM 在 `request_handover` 工具调用中仍会挑选不匹配的 canonical 值（cs_011 把"用户显式要求人工"读成 `user_distress`、cs_038 把 trust/safety 紧急情形映射成 `intake_complete_for_uc_j`），导致 L1:escalation_compliance 红灯。
- prompt 修复方案：`server/src/main/resources/prompts/system_prompt.txt` 注入显式 decision tree（情境 → canonical 值），按 USER-EXPLICIT / DISTRESS&SAFETY / APPEALS / COMPLIANCE / INTAKE-COMPLETE / BOT-LIMITS / INFRASTRUCTURE 六组分类，强调 prefer specific over generic；同时在 `ToolDispatcher.dispatch()` 增加 belt-and-suspenders defensive validator——LLM 非 canonical reason 落地时 log warn 并强制 coerce 到 `service_degraded`，杜绝再有 `CONTRACT_VIOLATION:enum_violation` 漏入 trace。
- 顺手修复 #19 的兄弟问题：`ControlKernel.processMessage` 第 130 行 `session.setEscalationReason("drift_hard_shift")` 是非 canonical 字面量，按代码语义（hard topic shift → 系统已无法在当前 UC 内继续服务）map 到 canonical `service_degraded`，cs_259 在 session-create 时刻命中的 `CONTRACT_VIOLATION:escalation_reason='drift_hard_shift'` 一并清零。
- 目标案例：`cs_interactive_011 / 036 / 038 / 066`（清 `L1:escalation_compliance`）+ `cs_interactive_259`（清 `CONTRACT_VIOLATION:drift_hard_shift`）+ `cs_interactive_001 / 002 / 014 / 029`（Fix 3c 之后由 active_use_case CV 暴露出的 L1 escalation_compliance 也连带清掉）。
