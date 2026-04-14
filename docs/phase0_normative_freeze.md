# Phase 0 — Freeze Normative Layer

> 锁定母规范，确认本项目不得漂移的内核约束。
>
> **规范来源**:
> - `customer_service_agent_tech_spec.md`（Whole Tech Spec — V1 内核 / 架构 / 控制 / 工具 / Handover / Guardrails / Observability / NFR / Release Criteria）
> - `customer_service_agent_eval_spec.md`（Eval Spec — Eval scope / Dataset / Grader / Metrics / Launch Gates / CI/CD）
> - `customer_service_tool_spec_v0_1.yaml`（Tool Spec v0.1 — V1 tool surface、visibility、risk_tier、per-UC 可用性、runtime_policy；与母规范一致并对其做领域具化）

---

## 0.1 这一步的目标

确认哪些内容是"母规范"，后续项目不得随意漂移。本 Gumtree CS Bot 项目继承通用 Customer Service Agent 规范，并明确：

- **不可改写**: V1 范围、控制内核、动作集、handover payload contract、release gates、eval suite 类型。
- **可领域化**: use case 列表、knowledge scope、escalation 触发条件细节、风险等级判定、话术与品牌口径、外部系统集成（Salesforce / GCP）。

---

## 0.2 对 Customer Service Agent 固定的共性约束

来自 `customer_service_agent_tech_spec.md` §5 设计原则与 §11 控制内核，以及 `customer_service_tool_spec_v0_1.yaml` §principles：

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
| **allowed actions (V1)** | `ask_user`、`retrieve_knowledge`、`answer_grounded`、`escalate_human`、`finish`。V1 默认不做 `create_case` 作为通用自主动作、`sub_agent_call`、unrestricted multi-tool composition、workflow chaining。 | tech_spec §11.4 |
| **control budgets** | `max_clarification_rounds`、`max_faq_miss`、`max_bot_turns_per_issue`、`max_repeated_same_action`、`max_total_bot_turns_before_forced_escalation` | tech_spec §11.5 |
| **drift handling semantics** | minor drift（保持 use case）/ soft shift（切换 active issue 但保留主问题）/ hard shift（升级或切策略） | tech_spec §11.6 |
| **state model layers** | External State / Session State / Memory（V1 极轻）/ Context（每轮投影） | tech_spec §8 |
| **context projection contract** | 输出包含 `task_summary`、`active_use_case`、`candidate_use_cases`、`recent_messages`、`retrieved_knowledge`、`risk_flags`、`budget_state`、`allowed_actions`、`tool_schemas` | tech_spec §10.5 |
| **use case registry lite schema** | use_case_id / name / description / example_user_requests / knowledge_scope / risk_level / allow_clarification / allow_bot_resolution / allowed_actions / escalation_conditions / outcome_class | tech_spec §9.2 |
| **V1 tool set (agent_visible)** | `search_knowledge`、`resolve_article`、`get_customer_context`、`request_handover`、`record_outcome`。每个工具必有 stable name / clear description / parameter schema / permission boundary / standardized success-error payload | tech_spec §13.1, §13.2 / tool_spec §tools |
| **V1 tool set (runtime_only，模型不可直接调用)** | `create_case_controlled`（仅 UC-H/UC-J/UC-K 允许）、`lookup_customer_account`（由 `get_customer_context` 组装）、`lookup_listing_or_ad`（由 `get_customer_context` 组装）。由 runtime 策略驱动，不暴露给模型上下文或 prompt。 | tool_spec §tools (visibility: runtime_only) |
| **human-only tools (Phase2 或人工触发)** | `moderation_enforcement_action`（删帖 / 限号 / 账号限制，critical risk）、`send_followup_email_or_async_update`（异步邮件更新，medium risk，V1.1 或人工触发）。Phase 1 Bot 不得直接或间接调用。 | tool_spec §tools (visibility: human_only) |
| **per-UC 工具可用性矩阵** | 每个 tool 的 `allowed_use_cases` / `disallowed_use_cases` 作为硬约束；超范围调用由 runtime 拒绝并记录 `scope_blocked` | tool_spec §tools.*.allowed_use_cases |
| **tool risk tier 与 runtime_policy** | low（knowledge）/ medium（composite read / controlled write）/ high（`create_case_controlled`）/ critical（`moderation_enforcement_action`）；每类风险对应 runtime policy（retries、must_log_*、must_validate_required_fields 等） | tool_spec §tools.*.risk_tier, runtime_policy |
| **V1 use case 集合** | UC-A（Ad Status & Visibility）、UC-B（Posting & Editing）、UC-C（Messages & Replies）、UC-D（Account & Login, non-sensitive）、UC-E（General Product & Search）、UC-F（Payment Inquiry, non-dispute）、UC-FP（Correct Deletion Explanation）、UC-G（GDPR / Data Deletion — 结构化 intake + handover）、UC-H（Incorrect Deletion Appeal — intake + create_case_controlled + handover）、UC-I（Refund / Payment Dispute — intake + handover）、UC-J（Trust & Safety — structured intake + create_case_controlled + handover）、UC-K（Technical Issue — intake + create_case_controlled + handover）。UC-A..F, UC-FP 为 Bot 可 FAQ-resolvable；UC-G..K 为 V1 intake-then-handover（Bot 不自主 resolve）。 | tool_spec §use_cases |
| **handover trigger policy (V1 minimal)** | user explicitly requests human / high-risk use case / clarification budget exhausted / faq miss threshold exceeded / policy requires escalation / system confidence-grounding insufficient | tech_spec §15.2 |
| **handover payload contract** | session_id / primary_use_case / candidate_use_cases / current_status / summary / clarification_count / faq_miss_count / articles_shown / escalation_reason / transcript_ref | tech_spec §15.3 |
| **mandatory guardrails** | 明确 bot 身份、不伪装人工、不在无依据时编造答案、不处理高风险自动裁决、不做越权承诺、只暴露最小必要信息 | tech_spec §16.1 |
| **versioned configs (V1)** | prompt version / projection policy version / control policy version / tool schema version / eval suite version | tech_spec §16.2 |
| **required event types** | session_started / use_case_inferred / retrieval_executed / article_shown / clarification_asked / escalation_requested / outcome_recorded / session_closed | tech_spec §17.1 |
| **trace fields** | trace_id / session_id / turn_id / prompt_version / model_version / projection_version / active_use_case / action_selected / tool_calls / source_ids / outcome | tech_spec §17.2 |
| **minimal funnel** | total sessions / understood / resolved by bot / escalated / abandoned / wrong containment / repeat-contact proxy / latency p50-p95 / cost per conversation | tech_spec §17.3 |
| **NFR baselines** | FAQ answer p95 ≤ 5s；escalation request p95 ≤ 3s；median turns for solved FAQ ≤ 6；handover request 可重试；state write 可校验；degraded mode 落到安全升级；最小数据暴露；redacted logs；secret 隔离；retention 合规 | tech_spec §18 |
| **eval suites (P0)** | Core E2E Suite + Control Sanity Suite + Grounding & Policy Suite + Handover Contract Suite | eval_spec §6.1 |
| **eval suites (P1)** | harder drift / multi-turn issue switching / knowledge gap analytics / replay-driven bad-case mining | eval_spec §6.2 |
| **launch gates (V1)** | active use case accuracy ≥ 85%；candidate recall ≥ 95%；groundedness pass rate ≥ 98%；critical policy violation = 0；escalation recall ≥ 95%；wrong containment ≤ 2%；handover completeness ≥ 98%；repeated same action rate below threshold；median turns for solved FAQ ≤ 6；FAQ answer p95 ≤ 5s | eval_spec §11 |
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
| per-UC 工具可用性矩阵与 runtime policy 细化 | `phase2_domain_realization_spec.md` §2.10（与 `customer_service_tool_spec_v0_1.yaml` 对齐）|
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

否则视为违反母规范。
