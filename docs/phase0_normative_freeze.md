# Phase 0 — Freeze Normative Layer

> 确认哪些内容是"母规范"，后续项目不得随意漂移。
>
> **规范来源**: `customer_service_agent_version_new_spec.md`

---

## 0.2 Customer Service Agent 固定的共性约束

- single agent + bounded loop
- state outside, context projected
- grounded before generative
- escalation is first-class
- eval before expansion
- V1 优先高频、低中风险、边界清晰场景
- V1 不做复杂多步骤流程编排
- V1 不做高风险自动决策
- 必须有 handover contract
- 必须有 minimal observability
- 必须有 release gates

---

## 0.3 本项目显式继承的规范项

| 规范项 | 继承内容 |
|-------|--------|
| **V1 scope boundary** | 高频 FAQ / guidance / 帖子状态查询 / 登录账户指引 / 正确删除解释；不做复杂多步流程、高风险自动决策、开放式生成 |
| **control kernel** | single agent + bounded loop；状态机 INIT → DISCOVER → RESOLVE → CONFIRM → CLOSE / ESCALATE |
| **allowed actions** | `ask_user`、`retrieve_knowledge`、`answer_grounded`、`escalate_human`、`finish`。V1 不做 `create_case` 作为通用自主动作、`sub_agent_call`、unrestricted multi-tool composition |
| **use case registry lite schema** | 每条含 use_case_id / name / description / example_user_requests / knowledge_scope / risk_level / allow_clarification / allow_bot_resolution / allowed_actions / escalation_conditions / outcome_class（详见 `phase2_domain_realization_spec.md` §2.2） |
| **handover payload contract** | session_id / primary_use_case / candidate_use_cases / current_status / summary / clarification_count / faq_miss_count / articles_shown / escalation_reason / transcript_ref |
| **eval suites** | P0: Core E2E Suite + Control Sanity Suite + Grounding & Policy Suite + Handover Contract Suite。P1: Drift / Replay / Bad-case |
| **release gates** | active use case accuracy ≥ 85%；candidate recall ≥ 95%；groundedness ≥ 98%；critical policy violation = 0；escalation recall ≥ 95%；wrong containment ≤ 2%；handover completeness ≥ 98%；median turns ≤ 6；FAQ p95 ≤ 5s |
| **deferred to V1.1** | hybrid orchestration、lightweight procedures、knowledge readiness workflow、decision path / replay tooling、stronger analytics |
| **deferred to V2** | full configuration versioning、handback lifecycle、richer routing policy、broader procedural automation |
