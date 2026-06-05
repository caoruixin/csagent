---
title: Runtime bad-case 调查（c1–c9）— request_handover schema 缺口 + DISCOVER 计数未接线 + admin trace observability + UC-A premise projection
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-05
last_updated: 2026-06-05 (added c7/c8/c9; introduced R3/R4)
mode: bad-case-driven
notes: >
  Bad-case 输入来自 human 提供的 9 条真实 trace（c1–c9）。本 proposal 覆盖
  human 明确要求优先处理的 runtime/infra 层；semantic 层（DISCOVER 后期
  clarification 措辞、UC-A 验证 procedure、FAQ-grounded fidelity、记录
  outcome 时机等）显式留给 autoloop 调 skill procedures 跟进。受 §5.8
  framework-defect priority 影响：M-Auto-5 close 之前不开新 sub-sprint；本
  文档作为 M-Auto-6 候选材料 + 部分轻量 runtime fix 可由 human 决定是否
  提前到 M-Auto-5 close 之后的第一个 runtime sub-sprint。
---

# Executive summary

Human 提供的 **9 条 trace（c1–c9）** 暴露 **4 个 runtime/infra R-item（R1–R4）** + **5 个 semantic OBSERVATION（defer to autoloop）**。

| R-item / OBS | Layer | 触及 case | 触及代码 | Run/Semantic | 处置 |
|---|---|---|---|---|---|
| **R1** request_handover schema 暴露 `intake_fields` + 每 UC required-fields hint | `prompt_projection` | c1, c5, c6 | `ContextProjectionBuilder.java:200-260` | semantic-touching（须含 §7 stanza） | M-Auto-6 候选 |
| **R2** DISCOVER clarification counter live-path 接线 + budget projection + budget-mapping label fix | `infra` + `skill_state` | c3, c9 | `AgentRunLoopImpl`, `ControlKernel.java:305-308`, `BudgetChecker.java` | pure infra（exempt） | M-Auto-6 候选；与 Cluster C.3 合并 |
| **R3** admin trace observability：session list 完整性 + dedup ToolEvent UI 折叠 + ESCALATE 终态显示 | `infra`（observability） | c7（admin 找不到 case）、c8（重复 0ms tool event 不折叠） | `DemoInspectionController.java:86`, `SessionList.tsx`, `TraceViewer.tsx:399-471` | pure infra（exempt） | M-Auto-6 候选；与 Cluster B.1 合并 |
| **R4** ad-context premise projection slot：当 form 无 `ad_id` 而 user 引用自己的广告时，runtime 暴露 `customer_context_status` / `ad_reference_unverified` 结构化信号 | `prompt_projection` | c7 | `ContextProjectionBuilder.java`, `FormContextIngestionService.java:109` | semantic-touching（须含 §7 stanza；但 surface 的是结构化 boolean/enum 信号，零 keyword/regex） | M-Auto-6 候选 |
| **OBS-S1** UC-A skill procedure 增加"verify ad reference before resolve" critical_step | `prompt_projection`（skill yaml） | c7 | `resolve_faq_grounded_answer.yaml` | semantic surface | **defer to autoloop**（与 R4 配合更好；R4 surface 信号，autoloop 调 yaml 措辞） |
| **OBS-S2** DISCOVER ad_status disambiguation cue 过度保守 | `prompt_projection`（discover_triage.yaml） | c9 turn 1 | `discover_triage.yaml:44-45` | semantic surface | defer to autoloop |
| **OBS-S3** FAQ grounded answer fabrication（c8 turn 6 "My Account > Settings > Contact Preferences" 不在 article 正文） | `semantic_planner` | c8 | LLM-owned | semantic surface；已有 `D-faq-grounded-resolve-bypass`（action_bank §4） | defer to autoloop |
| **OBS-S4** record_outcome 在 RESOLVE phase 过早提交（guard 正确） | `semantic_planner` | c2 | guard 正确 | semantic | defer to autoloop |
| **OBS-S5** cross-turn rank-1 first refinement 重复 query | by-design（§11 anti-误杀 floor） | c4 | — | — | 不开 R-item |

**关键 invariants（必须遵守）**：

- 当前 M-Auto-5 close-BLOCKED on S-Auto-22；`docs/10-handoff.md` §0 写明 §5.8 framework-defect priority **ACTIVE**："no new semantic sub-sprints; no §5.6 bad-case rerun for milestone-close evidence while OQ-S77 is open."
- R1/R2/R3/R4 **必须排到 M-Auto-5 close 之后**。
- R2 / R3 是 pure infra，按 §1 / §7 stanza 适用规则 **可豁免** stanza；R1 / R4 触及 projection（LLM-facing surface），**须含 §7 stanza**。
- R1 与 c1/c5/c6 + R2 与 c3/c9 + R3 与 c7/c8 + R4 与 c7 — 这 4 个 R-item 各自独立，可以拆分排（最小 1 个 sub-sprint，最大 4 个）。Scope split 建议见 §6。
- 触发 autoloop semantic 优化（OBS-S1/S2/S3/S4）**必须在 R1/R2/R4 落地 *之后***，否则 noise floor 太高，optimization 无效（详见上一轮对话中关于 autoloop 噪声的分析）。

---

# 1. Case-by-case 现象 + 根因汇总表

按 trace 顺序，每个 case 一句话现象 + code-grounded 根因 + 对应 R-item / OBS。

| Case | trace | 现象（一句话） | code-grounded 根因 | R-item / OBS |
|---|---|---|---|---|
| **c1** | `e1f844cb-712...` | UC-J Trust&Safety report，bot 4 次 `request_handover` 全部以 `intake_required_fields_missing_for_intake_complete` 失败，user 全程未被正常 handover | `ContextProjectionBuilder.java:200-260` 投影的 `request_handover` schema **没有声明 `intake_fields` 字段**；validator 设计上是 "args 持久化 → validate"（`AgentRunLoopImpl.java:486-494`），一次完整调用本应原子通过；但 LLM 因没有 schema contract，**从不**在第一次 call 时带 `intake_fields`；hint 路径教育需要 LLM 自我修正，但某些 LLM 不会接住 | **R1** target |
| **c2** | `f070bf75-67a...` | UC-A `record_outcome(outcome_class=resolve)` 被 `progressive_resolve_record_outcome_premature` reject | `ResolveDispositionEvaluator.java:160-186` 正确拒绝 phase ≠ CONFIRM/CLOSE 时的 resolve outcome；guard 行为符合 §1.4 contract | **OBS-S4** |
| **c3** | `d56fd1e6-510...` | DISCOVER phase 连续 2 turn 输出逐字相同的 clarification（user 已新增信息却被忽略） | `BudgetChecker.maxClarificationRounds(=2)` 在 live `AgentRunLoopImpl` 路径 **从不递增** —— counter 只在 legacy `PhaseEvaluator.java:880` 才 `+1`；live 路径无 identical-clarification dedup；LLM-facing projection 中 `clarification_count` 永远是 0（`ContextProjectionBuilder.java:351, 658`） | **R2** target |
| **c4** | （human 误粘贴同 c3 trace） | 描述：search_knowledge 多次 paraphrase | rank-1 first refinement 是 M-Auto-3 §11 三类 PARAPHRASE_STORM 中 OBSERVATION-only 项；rank-2+ S-Auto-15 `BotSession`-scoped budget gate 已实现 | **OBS-S5** |
| **c5** | `c6f60c19-3fc...` | UC-J 同样 schema 缺口；第二次 call 带 `intake_fields` 成功 | 同 c1 根因 | **R1** target |
| **c6** | `3bd52cf6-b14...` | UC-I refund 同样 schema 缺口；第二次 call 带 `intake_fields` 成功 | 同 c1 根因 | **R1** target |
| **c7** | （admin 中找不到；form 无 ad_id） | bot 没问 ad_id 就回答"your ad was removed because..."，user 反问"if you don't know my ad id, how can you get the conclusion?" | (a) `FormContextIngestionService.java:109` `lookup_listing_or_ad` 只在 `ad_id != null && !ad_id.isBlank()` 时自动触发 → form 无 ad_id 时 LLM 看不到 "premise unverified" 信号；(b) `resolve_faq_grounded_answer.yaml` 的 critical_steps 没有"先验证 ad 存在"步（对标 UC-FP 第 114-128 行 `consult-moderation-context-on-removal-explanation` 是有的）；(c) admin UI 未显示这个 case（trace 持久化路径或 status 过滤问题）；FAQ article `ka41r000000LIEJAA4` 是泛化模板（含 `XXXXXXXXX` 占位），bot 复述时弄丢了 "我不知道你的具体 ad" 的诚实性 | **R3**（admin 显示）+ **R4**（projection signal）+ **OBS-S1**（procedure step） |
| **c8** | `e296c331-401...` | (a) turn 2-3 调了 search+resolve 但 user-facing reply 只是 "Let me check..." / "I'm looking into this for you."；(b) turn 6 给了"My Account > Settings > Contact Preferences" 这个精确 UI path，user 怀疑 fabricate；(c) 多次同 source_id 的 `resolve_article` 0ms 重复事件出现在 trace | (a) projection **正确投递了 article 完整正文**（`ContextProjectionBuilder.java:969-976` + `ResolveArticleTool.java:95-110` 投 `description` 完整字段）→ LLM 收到内容；placeholder 是 LLM 自主输出的 stylistic choice 或 `ActionParser.java:72` 的 empty-reply fallback（须 tool_calls 与 user_message **均**空才触发）；(b) **确认 fabricate**：article `ka4P200000003sLIAQ` 正文含 "Your Contact Details section / phone box"（turn 5 内容 grounded）但 **不含** "My Account > Settings > Contact Preferences"（turn 6 内容 ungrounded）→ `D-faq-grounded-resolve-bypass`（action_bank §4 deferred runtime candidate）;(c) A1 dedup **正确工作**（`AgentRunLoopImpl.java:580-619` 缓存命中 0ms 返回），但 `TraceViewer.tsx:399-471` UI 渲染时 **不识别** `deduplicated` flag → 用户看到"重复"事件实际是 dedup 的审计记录 | **R3**（UI 折叠 dedup event）+ **OBS-S3**（fabrication / autoloop） |
| **c9** | `d98c704f-7fa...` | 4 个 turn 都在 DISCOVER，user 中途偏题但 turn 4 清晰回到 "But why is my ad offline?"，bot 0 ms 回应 + `request_handover(escalation_reason="turn_budget_exhausted")` | (a) DISCOVER 在 turn 1 没 commit UC-A（`discover_triage.yaml:44-45` 的 ad_status disambiguation cue 引导 LLM 先 ask "reason vs appeal" 而非 commit）→ semantic surface（OBS-S2）;(b) bot turn 2/3 输出相同 disambiguation 是 c3 同根因（R2）;(c) `turn_budget_exhausted` 是 `ControlKernel.java:305-308` runtime inject；mapping 是默认 fall-through（`mapBudgetToEscalationReason` 除 max-clarification / max-faq-miss 外都映射到 `turn_budget_exhausted`）→ 即便是 `max-repeated-same-action=2` 触发也被标 `turn_budget_exhausted`，**label 误导**（应当是 `clarification_budget_exhausted`） | **R2** target（含 mapping 修正）+ **OBS-S2**（autoloop） |

---

# 2. Layer classification per R-item / OBS（按 `iteration_governance.md` §3.2）

| 标签 | §3.2 决策路径 | layer |
|---|---|---|
| R1 | Q3 "projection / context handed to LLM was wrong or impoverished"（schema 不声明字段 = projection 缺信息） | `prompt_projection` |
| R2 | Q1 不是；Q4 "multi-turn flow losing state across turns"（counter 跨 turn 应累计但实际不累计） + 同时 Q1 边缘适用（dead-code wiring infra） | `infra` + `skill_state` |
| R3 | Q1 "session failing to start, crash on infra, or hit a timeout / OOM not caused by tool semantics" 的扩展项（observability 缺失影响诊断）| `infra`（observability） |
| R4 | Q3 "projection / context impoverished"（form 无 ad_id 时未暴露 `ad_reference_unverified` 结构化信号） | `prompt_projection` |
| OBS-S1 | Q5 "LLM choosing semantically wrong action even when projection and state are correct" 的边缘项 —— UC-A procedure 缺一步，本质是 skill yaml 增加 critical_step | `prompt_projection`（skill yaml） |
| OBS-S2 | Q5 + Q3 混合 —— discover_triage.yaml 的 disambiguation cue 过度保守 | `prompt_projection` |
| OBS-S3 | Q5 "LLM 选择了语义错误的 action 即便 projection 正确"（c8 turn 6 LLM 收到完整 article 内容仍然 fabricate UI path） | `semantic_planner` |
| OBS-S4 | guard 正确，LLM 选择错 | `semantic_planner` |
| OBS-S5 | by-design taxonomy（M-Auto-3 §11 三类 floor） | — |

---

# 3. Coverage check（vs `action_bank.md` + `milestone_objective.md` 现状）

| 提议 R-item | 已有覆盖？ | 处置建议 |
|---|---|---|
| R1 | 无直接 R-item。`R-uc-k-intake-complete-case-id-binding`（action_bank §5.2 G1 surfaced）是 case_id 缺失（UC-K 特定），属 `skill_state`；**不冲突，可同包** | NEW R-item，M-Auto-6 候选 |
| R2 | M-Auto-6 Cluster C.3 "generic clarifier branch wasting opening turn" 高度重合 | **不另开**；R2 是 C.3 的 code-grounded 实现路径；M-Auto-6 派单时合并 |
| R3 | M-Auto-6 Cluster B.1 "per_turn_trace 30-50% truncation" + Cluster C.2 "46f5b2e9 500 + double-send live UI" 都属 admin trace observability cluster | R3 **enrich** B.1 + C.2；可作为同一 observability sub-sprint 的子项 |
| R4 | 无直接 R-item。与 OBS-S1 (UC-A procedure step) 配合形成"runtime surface signal + skill 调措辞"完整闭环 | NEW R-item，M-Auto-6 候选 |
| OBS-S1 | 无 | autoloop semantic 优化目标 |
| OBS-S2 | 无；但 `R-prompt-phase-plan-directive-followship`（action_bank §5.2 Sprint 19 surfaced）边缘相关 | autoloop 目标 |
| OBS-S3 | `D-faq-grounded-resolve-bypass`（action_bank §4 deferred runtime candidate）—— c8 turn 6 是该 R-item 的第 2 个独立实例 | 应将 c8 trace 作为该 R-item 的第 2 个实例 **登记**；deferred 状态可以**重评估** —— n=2 起 R-item 已具备结构性证据 |
| OBS-S4 | 无；guard 正确 | autoloop 目标 |
| OBS-S5 | M-Auto-3 §11 三类 taxonomy 已是 canonical | 不开 R-item |

无 R-item 冲突。R3 与 audit Cluster B.1 / C.2 重叠 → 建议合并交付。

---

# 4. Current-state survey（code-grounded）

所有引用路径在 HEAD（commit `021a86a`）验证过。本节是 R1–R4 的代码事实基础，按 R-item 组织。

## 4.1 R1 — `request_handover` intake-fields schema 缺口

| 关注点 | 位置 | 行为 |
|---|---|---|
| Tool schema 注册 | `ContextProjectionBuilder.java:116-120` | `buildRequestHandoverArgsSchema()` |
| Schema 字段 | `ContextProjectionBuilder.java:200-260` | **只有** `escalation_reason`（required, 23-enum）+ `summary`（optional）。**没有 `intake_fields`** |
| Args 持久化点（先于 validator） | `AgentRunLoopImpl.java:486-494` | 当 `toolName=request_handover` && intake UC → 先调 `persistInlineIntakeFields(session, call)` |
| 持久化实现 | `AgentRunLoopImpl.java:981-1002` | 把 `call.arguments.intake_fields` 合并入 `session.intakeFields` JSONB |
| Validator（dispatch 之前） | `AgentRunLoopImpl.java:495-527` → `SkillGuardrailDispatcher.checkBeforeDispatch` | |
| Validator 实现 | `SkillGuardrailDispatcher.java:265-298` `handleIntakeCompleteRequired` | 读 `session.intakeFields`（已合并 args），调 `IntakeFieldsRegistry.intakeComplete(uc, collected)` |
| 必填字段定义 | `IntakeFieldsRegistry.java:53-67` | UC-G `[case_id]` / UC-H `[case_id]` / UC-I `[transaction_reference, dispute_reason]` / UC-J `[report_target, report_type, description]` / UC-K `[case_id]` |
| Reject hint 提示文本 | `SkillGuardrailDispatcher.java:288-298` | `"Ask the user for the missing intake fields above, then call request_handover with arguments.intake_fields populated."` |

**核心发现**：

- **设计意图**：dispatch 顺序是 "持久化 args → 跑 validator"（line 487-494 comment 写明 "persist inline intake fields BEFORE the guardrail check so a single complete handover call merging the missing fields is allowed through"）。即：**一次带完整 `intake_fields` 的 `request_handover` 应当原子通过**。
- **实际现象**（c1/c5/c6）：LLM 第一次 call **从不带 `intake_fields`** —— 因为 projected schema 里这个字段根本没声明。LLM 只在收到错误 + hint 之后才补一次。
- **结论**：这不是 validator bug、不是 dispatch-顺序 bug、不是 IntakeFieldExtractor bug —— 是 **schema 与 validator 期望的 contract 错位**（schema 没声明 validator 需要的字段；现行 hint 走 "出错后教育 LLM" 的 detour）。

## 4.2 R2 — DISCOVER clarification 计数 / loop 保护

| 关注点 | 位置 | 行为 |
|---|---|---|
| Counter 字段 | `BotSession.java:81` | `clarificationCount: Integer = 0` |
| Counter `+1` 唯一调用点 | `PhaseEvaluator.java:880` | **legacy 路径**；新 live 路径走 `AgentRunLoopImpl` 不经过这里 |
| Live 路径 DISCOVER 入口 | `ControlKernel.java:345-356` | flag-on 后路由到 `AgentRunLoopImpl.run(...)` |
| Live 路径搜索 "clarif" 增量 | `AgentRunLoopImpl.java` | **零**（无 `incrementClarification` / `setClarificationCount` 调用） |
| Budget 检查（依赖 counter） | `BudgetChecker.java:32-37` | `session.getClarificationCount() >= controlPolicy.getMaxClarificationRounds()` → 永远 false |
| 配置阈值 | `ControlPolicyService.java:22, 42, 89` + `control-policy.yaml:1-7` | `max-clarification-rounds=2`、`max-bot-turns-faq=15`、`max-bot-turns-intake=10`、`max-total-bot-turns=25`、`max-repeated-same-action=2` |
| Counter 在 projection 中 | `ContextProjectionBuilder.java:351, 658` | LLM 看到 `clarification_count` 时永远是 0 |
| Cross-turn identical clarification dedup | （无） | 全 server/ 无任何 "identical clarification within phase" 或 last-output-match 的保护 |
| Cross-turn paraphrase（仅 search_knowledge） | `AgentRunLoopImpl.java:671-736` (S-Auto-15) | 不覆盖 free-text reply |
| Within-turn identical retry storm (A1) | `AgentRunLoopImpl.java:580-619` (S-Auto-12) | 不覆盖 free-text reply |
| `turn_budget_exhausted` emit | `ControlKernel.java:305-308`（runtime inject）+ `mapBudgetToEscalationReason()` | 除 `max-clarification-rounds` / `max-faq-miss` 外的 budget hit **默认全部** 映射到 `turn_budget_exhausted` —— 包括 `max-repeated-same-action`，**label 误导** |

**核心发现**：

- DISCOVER live 路径上 clarification 既无 **预算上限**、也无 **identical 重复保护**、也无 **paraphrase 抑制**。
- c3 是 counter 未接线的纯表现；c9 同时遭遇 `max-repeated-same-action=2` 被 fall-through 标成 `turn_budget_exhausted` 的 mapping 误导。
- R2 应当包括：(i) counter 接线；(ii) budget projection 显式暴露给 LLM 软信号；(iii) escalation_reason 映射修正（`max-clarification-rounds` 之外，把 `max-repeated-same-action` 也映射到 `clarification_budget_exhausted` 或新增更准确的语义）。

## 4.3 R3 — admin trace observability

| 关注点 | 位置 | 行为 |
|---|---|---|
| Admin session list 接口 | `DemoInspectionController.java:86` | `sessionRepository.findAll()` 无过滤 |
| Session 持久化 | `SessionManager.java:218, 255, 366` | 无条件 save，无吞会话逻辑 |
| UI session list | `SessionList.tsx`（调 `/v1/demo/sessions`） | client 不过滤；StatusBadge 根据 `handling_state` 显示 |
| handling_state 终态语义 | `BotSession` + `SessionManager` | ESCALATE / handover 后是否被同步到 'CLOSED'？c7 case 可能停在中间态导致 UI 显示边界 case |
| Dedup ToolEvent 持久化 | `AgentRunLoopImpl.java:605-618` | cache hit 时新建 `ToolEvent.deduplicated()`，**完整存储**，含 `deduplicated=true` + `originalAtStep` |
| UI trace 折叠 | `TraceViewer.tsx:399-471` | **不检查** `deduplicated` flag；所有 tool_call 无条件展开 → 用户看到"重复"事件 |
| 已 queue 的相关项 | M-Auto-6 Cluster B.1 + C.2 | per_turn_trace 30-50% truncation；46f5b2e9 500 + double-send live UI |

**核心发现**：

- R3 的两个子项（admin session 列表完整性 + dedup event UI 折叠）都是**纯 UI/observability 层**改动；不动任何 runtime/Java 逻辑。
- 与 M-Auto-6 Cluster B.1 / C.2 在 audit 中已 queue，建议作为同一 observability sub-sprint 的子项交付。

## 4.4 R4 — ad-context premise projection

| 关注点 | 位置 | 行为 |
|---|---|---|
| Form ingestion | `FormContextIngestionService.java:78-79, 109` | `lookup_listing_or_ad` / `get_moderation_review_context` 仅在 `ad_id != null && !ad_id.isBlank()` 时自动触发 |
| `get_customer_context` 触发 | `ContextProjectionBuilder.java:113`（注释） | "Auto-triggered at INIT when email is in form_context" |
| 当前 form 缺 ad_id 时的信号 | （无） | LLM-facing projection 没有暴露 "ad_id missing → premise unverified → cannot assert ad status" 的结构化信号 |
| UC-A skill procedure | `resolve_faq_grounded_answer.yaml`（critical_steps） | **没有** "verify ad reference before resolve" 步；对标 UC-FP yaml 第 114-128 行 `consult-moderation-context-on-removal-explanation` 是有的 |
| Tool policy for UC-A | `tool-policy.yaml:8-10` | `get_customer_context` 是 AGENT_VISIBLE 但不强制 |

**核心发现**：

- R4 不依赖任何关键词 / regex / per-UC matrix —— 它只是把 runtime 已知的事实（form_context.ad_id 是否存在、`lookup_listing_or_ad` 是否成功、`customer_context_status` 是否 loaded）作为结构化 slot 暴露给 LLM。LLM 仍然完全自主决定要不要 challenge premise。
- R4 是 **OBS-S1（UC-A procedure 加 critical_step）的 *前置* 配套**：先在 projection 暴露信号，然后让 autoloop 在 yaml 里调措辞使 LLM 利用这个信号。如果反过来（先调 yaml，没 projection 信号），yaml 让 LLM 看不到的事实做判断，效果差。

## 4.5 c8 OBS-S3 — FAQ content fidelity 现状

| 关注点 | 位置 | 行为 |
|---|---|---|
| `resolve_article` 返回内容 | `ResolveArticleTool.java:95-110` | 返 `source_id`, `title`, `summary`, **`description`（完整正文）**, `canonical_url`, `uc_tags`, `safe_to_show` |
| accumulated_tool_results 渲染 | `ContextProjectionBuilder.java:969-976` | 完整 JSON node 投给 LLM，**含 description 正文** |
| Article `ka4P200000003sLIAQ` 正文 | `data/knowledge/knowledge_base_articles.json:2171-2187` | 含 "Your Contact Details section" + "phone box"（grounded c8 turn 5）；**不含** "My Account > Settings > Contact Preferences"（c8 turn 6 fabricate） |
| Grounding 软约束 | `resolve_faq_grounded_answer.yaml` `grounding_instruction` 等 | soft signal，LLM 自主遵守；S-Auto-13 paraphrase discipline 已加，但本 case 仍出现 ungrounded UI path |

**结论**：projection 正确投了完整 article；c8 turn 6 是 LLM 在 grounded content 之外的 extension —— `D-faq-grounded-resolve-bypass`（action_bank §4）类型，semantic-planner 层，**defer to autoloop**。但 c8 给该 R-item 增加了 1 个独立实例（n=2 起结构性证据），deferred 状态可以重评估。

---

# 5. Multi-layer root-cause + compounding-effect 分析

## 5.1 R1 → R2 → R3 → R4 的顺序约束

1. **R1 应先于深度 intake-UC autoloop**：R1 修复后，UC-G/H/I/J/K intake handover 不再产生首次 reject + retry；intake-相关 case 的 reducible-flaky 大概率下降。后续 autoloop 针对 intake skill 优化时噪声更小。
2. **R2 应先于 DISCOVER 相关 autoloop**：R2 修复后 DISCOVER loop 有 budget escape；OBS-S1（UC-A procedure verify ad）+ OBS-S2（discover_triage 调措辞）才能干净测量。
3. **R3 应优先于任何 trace-based 诊断**：admin UI 看不全 case / dedup event 误读，会让所有后续 manual review + bad-case 分析失真。R3 是其它 R-item 的 *evidence-floor*。
4. **R4 应紧接 OBS-S1（同包或先 R4 后 OBS-S1）**：R4 surface 信号，OBS-S1 调 skill 措辞让 LLM 利用信号。倒过来（先调 yaml 没 projection）yaml 指着空气说"verify ad"是无效的。

## 5.2 不能跳过的反向风险

- **若把 R1 改成 "validator 改为永远从 args 取 intake_fields"（移除 session 合并）**：会破坏 multi-turn 累计的设计 —— 用户分多 turn 提供 report_type / target / description 时 runtime 需要靠 session 保持。R1 必须保留 session 合并设计，只是**再加** schema 声明。
- **若把 R2 加 identical-clarification 跨 turn dedup**：与 §11 "rank-1 anti-误杀 floor" 哲学冲突。R2 首版只接 counter + budget projection；identical 保护留作 R2.b 后续观察（详见 §6 设计候选）。
- **若 R4 surface 信号过细（如 ad_id 校验通过/失败的 reason）**：可能引导 LLM 在 reason 内容上做判断 → 越界到 semantic_planner。R4 必须 surface 的是 boolean / enum 状态（loaded / missing_ad_id / lookup_failed），不传 reason。
- **若 R3 折叠 dedup event 后审计能力下降**：dedup event 必须保留可展开（不是真删），否则 §11 PARAPHRASE_STORM 审计失效。R3 折叠是 UI 默认行为；详情可展开。

## 5.3 与 eval framework / autoloop 的耦合

- 上一轮对话中已分析：autoloop 信号噪声主要来自 R1（intake-UC 类 case 的 step 浪费 + 偶发未 recover）+ R2（DISCOVER 类 case 的 loop 深度跨 draw 抖动）。R1 + R2 修复后，预期 `bad_cases` 的 reducible-flaky 比例（M-Auto-5 simfixed 6/12）下降到 ≤ 2/12。
- R3 是 measurement 工具修复，对 autoloop fitness 信号无直接影响，但对人 review / 手动 bad-case 选材的可靠性影响很大。
- R4 + OBS-S1 是 UC-A 性能改善预期的源头；autoloop 在 UC-A 域调 prompt 之前应当至少 R4 落地。

---

# 6. Design alternatives + 推荐

## 6.1 R1（intake schema 暴露）

| 方案 | 描述 | 优点 | 缺点 |
|---|---|---|---|
| **R1.a — Schema 字段 + 每 UC required-fields hint**（推荐） | `buildRequestHandoverArgsSchema()` 增加 `intake_fields` 为 `object`（不显式枚举 properties，允许任意 string key→string value）；在 `intake_state` 投影 + tool description 里附 `required_fields_for_active_uc: [report_target, report_type, description]`（per active_use_case 渲染） | 一次性消除 first-call-fail；LLM 有显式 contract；零 LLM 语义改动；零 hardcode（per-UC required 列表来自 `IntakeFieldsRegistry.java:53-67` 已有定义） | 改动跨 3 文件；需确保不破坏 mocked-LLM 测试 |
| R1.b — runtime 自动派生 intake_fields | runtime 在 dispatch 前从 session 各 state 自动 reconstruct，validator 不依赖 LLM 传 | LLM 完全无感 | 需要语义抽取（要么 keyword/regex 违 §1.7，要么再起 LLM 调用 成本+延迟）；与 §1.4 边界冲突 |
| R1.c — runtime 看到 fail 时自动重投不计 step | 不改 schema；改 `AgentRunLoopImpl` 在 reject verdict 时插入合成软提示且不计 step | hint 更显式 | 每次 intake handover 仍消耗 1 步 budget；trace 仍看到 error；不解决根因 |

**推荐 R1.a**。

## 6.2 R2（DISCOVER counter + budget projection + mapping fix）

| 方案 | 描述 | 优点 | 缺点 |
|---|---|---|---|
| **R2.a — counter 接线 + projection 暴露 budget + mapping 修正**（推荐） | (i) 在 `AgentRunLoopImpl` 的 DISCOVER 分支识别 "本 turn 输出 free-text clarification 且未 commit UC" 时 `session.clarificationCount += 1`；(ii) projection 增加 `budgets.clarification: {used: N, max: 2}`；(iii) `ControlKernel.mapBudgetToEscalationReason` 把 `max-repeated-same-action` 也映射到 `clarification_budget_exhausted`（c9 label 误导问题） | 复用已有 dead code，纯 wiring；零内容匹配；与 Cluster C.3 一致 | 需要识别 "free-text clarification" vs "其它 free-text"，建议以 "phase==DISCOVER && 本 turn 无 tool call 且未 commit UC" 为准 |
| R2.b — 加 identical-clarification 跨 turn dedup（last-output exact-match） | 跨 turn 比对本 turn 输出与上 turn 输出，byte-identical → suppress + 强制 LLM 重新决策 | 直接攻击 c3 / c9 的相同输出循环；anti-误杀（仅 byte-identical） | 需要新 BotSession state + rerun 路径；与 §11 "rank-1 anti-误杀 floor" 哲学冲突；范围更大 |
| R2.c — 合并 R2.a + R2.b | budget 软上限 + identical 硬保护 | 双重保险 | 复杂度上升；首版不必 |

**推荐 R2.a**。R2.b 留作后续观察项。

## 6.3 R3（admin trace observability）

| 方案 | 描述 | 优点 | 缺点 |
|---|---|---|---|
| **R3.a — session 列表显示所有 handling_state + dedup UI 折叠**（推荐） | (i) `SessionList.tsx` StatusBadge 加显 `ESCALATE` / `QUEUE_TO_HUMAN` 终态；(ii) `TraceViewer.tsx:399-471` 渲染 tool_call 时检查 `deduplicated` flag，默认折叠并显示 "↳ X dedup'd repeats" 标记，点击可展开；(iii) 添加诊断日志，记录 handling_state 转化路径以便后续验证 c7 类 case 持久化路径 | 纯 UI 改动；零 runtime/Java；与 audit Cluster B.1/C.2 合并 | 需要 UI 测试支持或视觉验证 |
| R3.b — 推迟到 audit Cluster B/C 的 research dispatch 之后 | 等 audit 全面研究后一起做 | 时机一致 | c7/c8 期间继续浪费 review 时间 |

**推荐 R3.a**（与 R1/R2/R4 同 sub-sprint 包装，或独立 micro-sprint）。

## 6.4 R4（ad-context premise projection slot）

| 方案 | 描述 | 优点 | 缺点 |
|---|---|---|---|
| **R4.a — 结构化 boolean/enum slot 暴露**（推荐） | `ContextProjectionBuilder` 增加 `customer_context_status` 字段，枚举 `{loaded, missing_email, missing_ad_id, lookup_failed, lookup_skipped}`；form 无 ad_id 时为 `missing_ad_id`；并在 `intake_state` / `form_context` 段加 `ad_reference: {form_ad_id: null/<value>, listing_lookup: ok/missing/failed}` | 零 keyword；纯结构化状态；与 R4 哲学一致；为 OBS-S1（autoloop） 提供 anchor | 需要 ContextProjectionBuilder 增加新字段；可能需要单元测试更新 |
| R4.b — runtime 强制 "无 ad_id 时 UC-A 必须 ask ad_id"（hard guard） | 直接在 dispatcher 增加 guard：UC-A + 无 ad_id + LLM 想输出 RESOLVE → reject | 强制；保证不再发生 c7 | 违反 §1.5（不为 soft semantic 加 hard guard）；§1.7 forbidden |
| R4.c — 在 form ingestion 阶段就 fail（无 ad_id 不允许进 UC-A） | INIT 阶段就报错 | 提早 fail | 完全不让 LLM 处理就是 hardcode 路由 |

**推荐 R4.a**。

## 6.5 推荐打包

**单个 runtime/infra sub-sprint（建议 M-Auto-6 第一批）：R1.a + R2.a + R3.a + R4.a 同包**。

总估算：~8-12 文件、~400-500 LOC、~10-15 个单元/集成测试。

| 工作项 | 文件 | 估改动 LOC |
|---|---|---|
| R1.a — schema 字段 + projection | `server/.../ContextProjectionBuilder.java` | +60 |
| R1.a — 测试 | `ContextProjectionBuilderTest.java` + 集成测试 | +100 |
| R2.a — counter 接线 | `server/.../AgentRunLoopImpl.java` | +25 |
| R2.a — budget projection | `server/.../ContextProjectionBuilder.java` | +10 |
| R2.a — mapping fix | `server/.../ControlKernel.java` | +5 |
| R2.a — 测试 | `AgentRunLoopImplTest.java` + `ControlKernelTest.java` | +80 |
| R3.a — UI session list | `ui/.../SessionList.tsx` | +30 |
| R3.a — UI dedup folding | `ui/.../TraceViewer.tsx` | +50 |
| R3.a — UI 测试 | UI test files | +60 |
| R4.a — projection slot | `server/.../ContextProjectionBuilder.java` + `FormContextIngestionService.java` | +40 |
| R4.a — 测试 | 单元测试 + projection 集成测试 | +50 |

**或拆分 2 个 sub-sprint**：

- **Sub-sprint A**（runtime-only）：R1.a + R2.a + R4.a（共享 ContextProjectionBuilder 改动语境）
- **Sub-sprint B**（UI/observability）：R3.a（隔离 UI 改动便于独立 verify）

最终 packaging 决定权在 deliver-agent。

---

# 7. §7 stanza pre-fill drafts

## 7.1 Sub-sprint（R1.a + R2.a + R3.a + R4.a 同包）

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection (R1.a 工具 schema + R4.a 上下文 premise slot)
+ infra (R2.a counter wiring + R2.a label mapping fix + R3.a observability)
+ skill_state (R2.a clarification counter 跨 turn 累计)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.
（R1.a 是已有 IntakeFieldsRegistry contract 的 LLM-facing 投影补全；
R2.a 是已有 BudgetChecker / max-clarification-rounds 的接线 + 映射修正；
R3.a 是 UI display only；R4.a 是已有 form_context / lookup tool 状态的
结构化暴露。均不要求新的 runtime-级 invariant。）

**Semantic hardcode:** No semantic hardcode introduced.
（R1.a 暴露的 required-fields 列表来自 IntakeFieldsRegistry.java:53-67
已有定义；R2.a 只递增 cardinality counter，零内容匹配；R3.a 不动 runtime
决策；R4.a surface 的是 boolean / enum 状态，不传 keyword 或 reason。）

**Generalization coverage:** target / neighbor / negative / shadow case counts: 7 / ~18 / ~10 / ~22
- target: c1 (UC-J)、c5 (UC-J)、c6 (UC-I)、c3 (DISCOVER loop)、c7 (UC-A no ad_id)、
  c8 (admin dedup event)、c9 (DISCOVER loop + budget label)
- neighbor: case_families/uc_g_*, uc_h_*, uc_i_*, uc_j_*, uc_k_* 所有
  intake-complete-for-uc-* escalation case；case_families/uc_a_* 凡 form 不带
  ad_id 的 negative test
- negative: 非 intake UC (UC-A 已带 ad_id, UC-B, UC-D, UC-F, UC-FP) 的 handover
  不应被误激活 intake_fields；DISCOVER 已 commit UC 的 turn 不应计入 clarification
  budget；UC-A 已带 ad_id 时不应出现 `missing_ad_id` slot
- shadow: 既有 shadow 集 (cs01s*, cs15s*, cs32s*, cs38s*, cs59s*, cs76s*, cs92s*)
  cross-suite 验证；至少覆盖 UC-K + UC-J + UC-A + UC-I
```

## 7.2 若拆分为 Sub-sprint A（runtime）+ Sub-sprint B（UI）

**Sub-sprint A**（R1.a + R2.a + R4.a）：同上去掉 R3.a 那一段；coverage 同上去掉 c8 admin item。

**Sub-sprint B**（R3.a）：

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (observability)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced.
（UI-only；不改 runtime；零内容匹配。）

**Generalization coverage:** target / neighbor / negative / shadow case counts: 2 / 0 / ~5 / 0
- target: c7 (admin 找不到 case)、c8 (dedup event UI 折叠)
- neighbor: 无（独立 observability fix）
- negative: ESCALATE / handover 完成的 session 应当依然在 admin 可见且可点开
  trace；A1 dedup 折叠后应当能展开看完整 dedup'd event 列表（审计不丢）
- shadow: 不适用（UI-only）
```

---

# 8. Hard fences + non-goals

**Hard fences（绝不做）：**

- 不引入 keyword / regex / 内容相似度匹配于 R2.a 的 budget 判定（cardinality only；anti-误杀 §11 floor）
- 不为 c1/c5/c6 的具体 UC 写 per-UC if/else（required-fields 来源是 `IntakeFieldsRegistry`）
- 不改 `IntakeFieldsRegistry` 的字段定义
- 不改 `SkillGuardrailDispatcher` 的 reject 逻辑（保留作为最后防线）
- 不改 `escalation_reason` 23-enum 枚举（参 `D-new-escalation-reason-enum` deferred / avoid）；R2.a 的 mapping fix 只是 **重映射** 现有 enum 值（`max-repeated-same-action` 由 `turn_budget_exhausted` 改映射到 `clarification_budget_exhausted`），不新增 enum
- 不改 `record_outcome` premature guard（OBS-S4 by-design）
- 不为 c4 写 cross-turn semantic similarity dedup（违 §11 anti-误杀 floor）
- 不在 R3 UI 折叠 dedup event 时丢失 audit 信息（必须可展开）
- 不在 R4 暴露 reason 内容，只暴露 boolean / enum 状态
- 不在 R4 用 keyword 匹配 user 消息 "my ad" 等短语去决定 surface signal（只用 runtime 已知的 form_context 字段状态 + lookup tool 返回）
- 不触 simulator / eval framework / scoring SHA / autoloop 5-file 集（§5.8 + fence-#13）
- 不在 M-Auto-5 close 之前 launch
- 不在本 sub-sprint 改任何 yaml 措辞（OBS-S1 / OBS-S2 / OBS-S3 全部 defer to autoloop）

**Non-goals：**

- 不修复 c1 中 LLM 反复失败 6 turn 的具体 LLM 决策（R1.a 修后自动消失）
- 不为 DISCOVER 加任何"自动跳过 clarification 进 RESOLVE"的捷径
- 不写新的 cross-turn semantic-similarity 抑制器
- 不重写 `IntakeFieldExtractor` 为 LLM-based
- 不修 c8 turn 6 的 fabrication 问题（autoloop 在 OBS-S3 / `D-faq-grounded-resolve-bypass` 处理）
- 不修 c9 的 disambiguation 过度保守（autoloop 在 OBS-S2 处理）
- 不修 UC-A 缺 critical_step "verify ad" 的 procedure（autoloop 在 OBS-S1 处理；本 sub-sprint R4 只 surface 信号）

---

# 9. Risk + compounding-effect 风险矩阵

| 风险 | 影响 | 缓解 |
|---|---|---|
| **R1.a schema 改动破坏 mocked-LLM 测试** | 现有 mocked-LLM 测试期望 schema 与既有形态严格一致 | 同包补 contract 测试；mocked-LLM 测试若硬编码字段 set 需要 expand |
| **R1.a `intake_fields` 字段被 LLM 误填到非 intake UC** | schema 在所有 UC 都可见 | description 写明 "only required for UC-G/H/I/J/K"；runtime 已有 `IntakeFieldsRegistry.isIntakeUseCase` gate，非 intake UC 持久化无害 |
| **R2.a counter 与 LLM 自我节制冲突** | LLM 可能在 budget=0 时仍尝试 clarify | 越过即升级到 `clarification_budget_exhausted` 路径 |
| **R2.a free-text clarification 识别误差** | 判定不准导致计数偏差 | 严格定义为 "phase==DISCOVER && 本 turn 无 tool call 且未 commit UC"；不做内容判断；counter-test 覆盖 commit-UC turn 不计数 |
| **R2.a mapping 修正可能引起 eval-side 期望变化** | `max-repeated-same-action` 由 `turn_budget_exhausted` 改映 `clarification_budget_exhausted` 后，eval CaseSpec 期望 escalation_reason 的可能需要同步 | check `eval_interactive/case_specs/` 中相关 case 的 expected escalation_reason；如有 mismatch 需同期更新 CaseSpec（属 §5.4 受控更新，不是 mask 真错） |
| **R3.a UI 折叠 dedup event 影响审计** | 默认折叠 dedup event 可能让 PARAPHRASE_STORM 审计困难 | 折叠是默认 UI 行为；event 仍完整持久化；提供展开按钮 + "↳ X dedup'd repeats" 标记 |
| **R3.a 不解决 c7 admin 找不到 case 的根因** | 探索 agent 未完全锁定 c7 case 不显示原因（持久化、handling_state、缓存任一可能） | R3.a 包含 "添加诊断日志记录 handling_state 转化路径" 作为前置；如发现是持久化路径 bug 则 escalate 为单独 R-item；UI 折叠是独立子项 |
| **R4.a slot 命名与现有 projection 冲突** | 新增 `customer_context_status` 字段可能与现有 `intake_state` 字段语义冲突 | 设计阶段需 verify ContextProjectionBuilder 现有字段集；命名空间下置（如 `context_status.customer_lookup`） |
| **R4.a 增加 projection 体积** | 多一个字段每 turn 都投 → 边际 token 成本 | 字段 boolean/enum 体积 < 100 字符；可忽略 |
| **同包 4 个 R-item 增加 reviewer 负担** | Codex 难一次审 4 套独立改动 | 各自独立测试用例 + 拆 commit（R1 一个 commit、R2 一个 commit、R3 一个 commit、R4 一个 commit），Codex 9-question kernel 分别审；同 sub-sprint 但 4 个 commit |
| **M-Auto-5 close 仍然延期** | 本 proposal 永远进不了 M-Auto-6 队列 | proposal-tier 永久存档；human 选择何时 promote |
| **shadow 不够覆盖 c7 / c9 这类 multi-cause case** | R3 / R4 不能完全验证 cross-suite 不回归 | 推荐 M-Auto-5 close 后的第一次 baseline 包含完整 shadow 22 case；R1+R2+R3+R4 sub-sprint 验收必须看 shadow 不退 |
| **R4 不修 OBS-S1 的话 c7 仍可能复现** | R4 surface 信号但 LLM 不利用 | 这是设计意图：R4 是 *enabler*，OBS-S1 在 autoloop 阶段调 yaml 让 LLM 利用信号；c7 全闭环修复需要 R4 + OBS-S1 双方落地 |

---

# 10. Observability / trace / report implications

- **R1.a**：`intake_fields` 出现在 `request_handover.arguments` → 现有 `tool_events` trace 已能展示；admin trace UI 把 `intake_state` 投影连同 `required_fields_for_active_uc` 一起渲染（与 Cluster B 合并）
- **R2.a**：`clarification_count` 已在 projection（line 351/658）但前端 admin trace 是否显示需 verify（参 `project_observability_debt_pattern` memory）；R2.a 同包补 admin UI 显示 `budgets.clarification_count` + `budgets.clarification_max`；新的 `clarification_budget_exhausted` mapping 路径需要在 trace 体现
- **R3.a**：直接是 observability 改动 —— admin session list 必须列全 + dedup event UI 折叠（保留可展开）。验证 c7 / c8 类 case 在 admin 可见
- **R4.a**：projection 增加 `customer_context_status` / `ad_reference` slot，admin trace 应显示这些字段，为后续 OBS-S1 调 yaml 时的 trace-driven 调优提供 anchor
- **Eval 影响**：
  - R1.a 修后所有 intake handover 不再产生首次 reject 的 tool_event → trace 体积下降；不影响 case_passed 判定
  - R2.a mapping 修正改变 escalation_reason 分布，影响 eval CaseSpec expected 字段（需同步更新）
  - R3.a 不影响 eval 数值，仅影响 manual review 体感
  - R4.a 增加 projection 字段，影响 token 计数但不影响 case_passed
  - 整体预期：`bad_cases` reducible-flaky 6/12 → ≤ 2/12（详见 §5.3 + 上一轮对话）

---

# Appendix A — 与 human 反馈的 9 个 case 对应表

| Case | trace id 片段 | 核心异常 | R-item / OBS |
|---|---|---|---|
| c1 | `e1f844cb-712...` | UC-J 4× `intake_required_fields_missing_for_intake_complete` reject；session 始终未 handover | **R1** target |
| c2 | `f070bf75-67a...` | UC-A `progressive_resolve_record_outcome_premature` | **OBS-S4**（guard 正确，defer to autoloop） |
| c3 | `d56fd1e6-510...` | DISCOVER 2 turn 输出完全相同 clarification | **R2** target |
| c4 | (human 误粘贴同 c3 trace) | 描述：search_knowledge 多次 paraphrase | **OBS-S5**（rank-1 anti-误杀 floor） |
| c5 | `c6f60c19-3fc...` | UC-J `intake_required_fields_missing_for_intake_complete` × 1 后第二次成功 | **R1** target |
| c6 | `3bd52cf6-b14...` | UC-I `intake_required_fields_missing_for_intake_complete` × 1 后第二次成功 | **R1** target |
| c7 | (admin 找不到；form 无 ad_id) | bot 没问 ad_id 就回答 "your ad was removed because..."；admin 找不到这个 case | **R3**（admin 列表）+ **R4**（premise projection）+ **OBS-S1**（UC-A procedure，autoloop） |
| c8 | `e296c331-401...` | tool result 内容投递正常但 bot turn 2-3 placeholder；turn 6 fabricate UI path；多次 0ms 重复 tool event | **R3**（dedup UI 折叠）+ **OBS-S3**（fabricate / `D-faq-grounded-resolve-bypass`） |
| c9 | `d98c704f-7fa...` | DISCOVER 3 turn 同 disambiguation；turn 4 user 清晰回主线时 runtime inject `turn_budget_exhausted`（label 误导） | **R2** target（含 mapping fix）+ **OBS-S2**（disambiguation 过度保守，autoloop） |

---

# Appendix B — R-item 编号与 action_bank 登记建议

deliver-agent promote 时，建议在 `action_bank.md` §5.2 "Sprint 077+ surfaced backlog" 或类似的新 sub-section 登记以下 R-items（id 用 kebab-case）：

| 建议 id | 描述 | 状态 | 源 |
|---|---|---|---|
| `R-request-handover-intake-fields-schema-projection` | request_handover 工具 schema 暴露 `intake_fields` + 每 UC required-fields hint；消除 first-call-fail 系统性 pattern | open | c1/c5/c6 |
| `R-discover-clarification-counter-live-wireup` | live `AgentRunLoopImpl` 路径接线 `clarificationCount += 1`；projection 暴露 budget；`max-repeated-same-action` 重映射到 `clarification_budget_exhausted` | open；合并 Cluster C.3 | c3/c9 |
| `R-admin-trace-observability-session-list-and-dedup-folding` | admin UI 显示所有 handling_state；TraceViewer 折叠 dedup event 但保留展开 | open；合并 Cluster B.1/C.2 | c7/c8 |
| `R-ad-context-premise-projection-slot` | ContextProjectionBuilder 暴露 `customer_context_status` + `ad_reference` 结构化 slot；为 OBS-S1 提供 anchor | open；与 OBS-S1 配套 | c7 |
| `D-faq-grounded-resolve-bypass`（已存在，重评估） | n=2 起结构性证据；c8 turn 6 是第 2 个独立实例 | 从 deferred 提升评估 | c8 |

---

# Appendix C — 与 deliver-agent 的交接清单

deliver-agent 若选择 promote 本 proposal 到 sub-sprint(s)，需决定：

1. **打包形态**：
   - 方案 A（推荐）：R1.a + R2.a + R3.a + R4.a 同 sub-sprint，4 个 commit；一份 §7 stanza
   - 方案 B：拆分 Sub-sprint A（R1+R2+R4 runtime）+ Sub-sprint B（R3 UI）；两份 §7 stanza
2. **launch 时机**：M-Auto-5 close 后第一个 sub-sprint？还是排在 audit Cluster B/C 2× research dispatch 之后？建议第一个（R3 与 audit cluster 合并 = audit 不需要再单独 dispatch）
3. **是否合并 Cluster C.3 + B.1 + C.2**：本 proposal 已识别合并点；建议合并以避免重复
4. **sub-sprint id**：建议下个可用号（约 S-Auto-23 / Sprint 078，视 S-Auto-22 完成情况）
5. **dev prompt 模板**：参 prompt-artifact-rules §9.1–§9.6；包含完整 §7 stanza + file-path fence + anti-误杀 invariants
6. **Codex review trigger**：semantic-touching（R1.a + R4.a 投影），需 §4.1 9-question kernel；建议 per-sub-sprint 而非 milestone-shared
7. **Generalization coverage 具体 case ID**：deliver 在 §7 stanza 填具体 target / neighbor / negative / shadow case 名单
8. **OBS-S1/S2/S3 的 autoloop 排序**：建议 R1+R2+R4 落地 → re-bless 评估 reducible-flaky 是否下降 → 然后开 autoloop 调 OBS-S1（UC-A procedure）+ OBS-S2（DISCOVER disambiguation）+ OBS-S3（FAQ fidelity）
9. **`D-faq-grounded-resolve-bypass` 重评估**：c8 给出第 2 个实例后，建议在下个 milestone planning 时评估是否从 deferred 提升

— 完 —
