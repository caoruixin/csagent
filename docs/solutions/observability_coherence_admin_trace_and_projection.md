---
title: Observability coherence — eval report.html + admin trace（每次 LLM invocation）+ Projected Context 三面与架构对齐
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-05-24
mode: bad-case-driven + forward-looking (hybrid)
---

## 1. Executive summary（执行摘要）

human 提了三个看似独立的问题，但它们共享**同一个根因主题**：

> **可观测性表面（eval report.html、admin trace 的 LLM Raw Response、admin trace 的 Projected Context）落后于 agent 的架构演进**——M2（Skill Registry 抽象）和 M3-Eval（四层评估金字塔）改造了 scoring / runtime / plan 层，但展示层与 trace 层没有同步跟上。

三个问题的精确诊断（全部 code-grounded，在 HEAD `84ae017` 验证）：

1. **eval report 与四层评估设计不匹配** —— `report.html` 仍渲染 M3 之前的 Phase-5 §6.9「7 指标仪表盘」+ 单一二元 `case_passed` 徽章；不显示 Tier-2、不显示 `case_passed_authority`（human_review vs programmatic）。这**不是 copy、不是兼容旧逻辑的 shim、也不是刻意保留**，而是**延期的可观测性技术债**，已登记为 OPEN 低优先级 R-item `R-eval-report-observability`（`action_bank.md:779`）。**已有一份准确的 research proposal**：`docs/solutions/eval_report_coherence_proposal.md`（我已逐条在 HEAD 复核，结论成立）。本文档**背书并补充**它，不重复。

2. **admin trace 的 LLM Raw Response 只显示最后一次** —— 每个用户 turn 在 `AgentRunLoopImpl.run()` 里会触发**多次** LLM 调用（agentic loop，按 `maxToolSteps`，FAQ Skill 是 4 次），但 `BotTurn.llm_raw_response` 是**单列**，每个 loop step 被覆盖（`AgentRunLoopImpl.java:221`）。per-invocation 数据其实**已存在**于 `llm_call_log` 表（截断到 500 字符），并已通过 `GET /v1/demo/sessions/{id}/llm-calls` 端点暴露、被 eval harness 消费——只是 admin trace viewer 从没 join 它。

3. **Projected Context 在多次迭代后是否合理** —— `ContextProjectionBuilder` 投影 ~35 个顶层字段。M2 之后 `PhasePlan` 已变成 **Skill 驱动**（`PhaseEvaluator.java:445-461`），但投影构建器只吸收了 Skill 能力的两处（`critical_steps`、`prior_skill_name`），**忽略了** `skill.requiredContextKeys()` 和 `state_inheritance.soft_signal_via_projection`；同时存在**重复字段**（`session.*` vs `budget_state` vs `phase_plan`）、**算了又丢的 `tool_schemas`**、以及 ~15 个「为 shape 稳定而恒为 null」的字段——而 80 行的 system_prompt 从没解释过这 20 个状态槽。

**建议落地形态**：三件事可成为一个 **M5「Observability Coherence」milestone**（或拆给 deliver-agent 决定）。关键的**顺序约束**：**#2（记录并呈现每次 invocation）应先于 #3（投影审计）**——因为只有先能在 trace 里看到每一次调用各自的投影，才能可靠判断 #3 里哪些投影字段是 LLM 真正用到的、哪些是冗余的。#1 是纯展示层（零 server 改动），随时可独立做。

---

## 2. Current-state survey（code-grounded，HEAD `84ae017` 验证）

### 2.A 问题 #1 —— eval report 表面（背书既有 proposal）

既有 proposal `docs/solutions/eval_report_coherence_proposal.md` 的所有承重声明我已在当前 HEAD 复核通过：

| 声明 | 验证位置（HEAD `84ae017`） | 结论 |
|------|------------------------|------|
| `case_passed = all(L1) ∧ all(mandatory L2) ∧ ¬tier2_critical_failed` | `eval_interactive/eval_interactive/scoring/composite.py:141,205` | ✓ |
| `composite = 0.5*outcome + 0.5*judge`（不通过则 0.0） | `composite.py:160-161,235-238` | ✓ |
| summary 成功门槛 `case_passed ∧ composite >= 0.7` | `batch/executor.py:398` | ✓ |
| `case_passed_authority` 已逐 case 落在 results.json | `executor.py:596` | ✓ |
| `tier2_result`（passed/severity/failed_step_ids）已在 JSON | `executor.py:638-644` | ✓ |
| report 仍是「Phase 5 §6.9 7 指标」 | `report/html_report.py:7,42` docstring | ✓ |
| 每 case 仅渲染单一二元徽章 `case_passed` | `html_report.py:451-454` | ✓ |
| report **无** `_render_tier2`、**无** `case_passed_authority`/`suite_authority` 渲染 | grep `html_report.py` 0 命中 | ✓ |
| `_OPT_IN_SETS = ("bad_cases","anchor_outcome")` | `batch/sets.py:24` | ✓ |
| `R-eval-report-observability` OPEN、低优先级、deferred M4+ | `action_bank.md:779` | ✓ |

**新增的一个上下文事实**（既有 proposal 未点明，对 human 理解 report 很关键）：human 打开的这份 `results/20260523-075141/report.html` 是 **M4-Eval-Cleanup 首轮（first-pass）run**——这也是为什么里面 `cs029` 是 0-turn `contract_violation`（report 第 363-378 行）。`10-handoff.md §0/§1` 记录：该 contract_violation 在隔离重跑 `20260523-095557` 上已清除，M4 最终以 A — Clean PASS 关闭。也就是说 human 正看着一份**未修复前的中间产物**，其「红」既有展示层错配（#1 主因），也叠加了一个已在重跑中消失的瞬时 flake。

报告内部「自相矛盾」的三个数字，其实回答三个不同问题（均正确）：
- 顶部 dashboard：`Passed 0 / Failed 12 / Task Success 0.0%`（= 程序化门槛 `composite >= 0.7`，而 bad-case fixture 的 L3 维度为空 → `composite = 0.5*outcome ≤ 0.5 < 0.7`，**结构性必然全红**）。
- per-case 徽章：5 PASS / 7 FAIL（= `case_passed` = L1 ∧ mandatory-L2 ∧ ¬tier2-critical）。
- `_manifest.md`「M4-Eval-Cleanup close」：PASS×5 / IMPROVING×4 / FAIL×3（= §5.6 人工判定，**这才是 bad-case suite 的权威结论**）。

### 2.B 问题 #2 —— admin trace「LLM Raw Response」只显示最后一次

**数据流（逐层验证）：**

1. **UI 类型层**：`ui/src/types/index.ts:64` —— `TraceStep.llm_raw_response?: string`（每个 step **单个**字符串）；`:63` `projected_context?: Record`（单个）。`TraceViewer.tsx:99-205`（`LlmDetailPanel`）只渲染 `step.llm_raw_response` 这一个值。

2. **API 端点层**：`DemoInspectionController.java:124-126` —— `GET /sessions/{id}/trace` 返回 `List<BotTurn>`，**一个 turn 一行**。UI 的 `TraceStep` 与 `BotTurn` 一一对应。

3. **持久化层**：`model/BotTurn.java:40-41` —— `llm_raw_response` **单列**；`:37-38` `projected_context` 单列（jsonb）。`TraceWriter.recordTurn(...)`（`TraceWriter.java:27-52`）每 turn 写**一个** `BotTurn`，只接收**一个** `llmRawResponse` + **一个** `projectedContext`。

4. **运行时层（根因所在）**：`AgentRunLoopImpl.run()` 是个 `for (step=0; step<maxSteps; step++)` 循环（`:170`）。每个 step：
   - 构建投影（`:181`），赋给 `lastProjection`（`:189`）——**覆盖**；
   - 调用 LLM `llmInvocation.invokeChat(...)`（`:201`）——**每个 step 一次 LLM 调用**；
   - `lastLlmRawResponse = response.getContent()`（`:221`）——**每个 step 覆盖**；
   - 把一个 `LlmCallEvent` 追加进 `llmEvents` 列表（`:224-227`）——这个**确实**逐调用累积，但 `LlmCallEvent`（`model/LlmCallEvent.java:13-21`）只存 `responseSummary`（`of()` 工厂 `:44` 截断到 500 字符），不存完整 raw，也不存 per-call 投影。
   - loop 终止时（final/escalate/maxSteps），`AgentRunResult` 携带的是 `lastProjection` + `lastLlmRawResponse`（最后一次），`llmEvents` 虽在 result 上但 `TraceWriter.recordTurn` **不接收它**。

   FAQ Skill `max_tool_steps: 4`（`resolve_faq_grounded_answer.yaml:24`）→ 一个 turn 最多 4 次 chat 调用，admin 只能看到第 4 次的完整 raw。

5. **per-invocation 数据其实已存在、已暴露**：
   - `LlmInvocationService.invokeChat`（`:114`）和 `invokeRouting`（`:266`）每次调用都 `llmCallLogger.log(...)`。
   - `LlmCallLogger.log`（`LlmCallLogger.java:37-58`）逐调用写 `llm_call_log` 表（migration `V9`），含 callType（chat/routing/rerank）、model、prompt/completion tokens、latency、success、**截断到 500 字符的 request/response summary**。
   - **端点已存在**：`DemoInspectionController.java:94` `GET /sessions/{id}/llm-calls`（Sprint 25 `R-per-llm-call-latency-instrumentation` 引入），返回这些 per-call 行；**eval harness 已经在用**（`eval_interactive/.../simulator/agent_client.py:169` `get_llm_calls`；`batch/executor.py:388,503-519` 把它放进 `results.json` 的 `case_results[].llm_calls`）。

**结论**：human 的判断完全正确。「记录」这件事**部分已有**（`llm_call_log` 逐调用、但截断且无 per-call 投影），「呈现」这件事**完全缺失**（admin trace 只 join `bot_turns`，不 join `llm_call_log`）。真正缺的是：(a) admin trace 把 per-invocation 维度呈现出来；(b) 若要「完整呈现每次调用结果」（非 500 字符摘要 + 每次调用各自的投影），需要新增 per-invocation 的完整持久化。

### 2.C 问题 #3 —— Projected Context 多次迭代后的合理性

`ContextProjectionBuilder.java`（1370 行）通过 `buildProjection()`（base，`:337`）+ `build()`（plan-aware run-loop 路径，`:787`）投影出 **~35 个顶层字段**：

- **session 块**（`:345-353`）：session_id / current_phase / active_use_case / total_bot_turns / clarification_count / faq_miss_count / handling_state
- **task_summary**（`:357-358`，自然语言串）、**risk_flags**（`:361-368`）
- **intake_state**（`:375-402`，仅 INTAKE UC G/H/I/J/K）
- **candidate_use_cases**（`:410-418`）、**alternate_candidate_use_cases**（`:431-440`，S31）、**discover_disambiguation_signals**（`:456-457`，S33）、**prior_use_case_carry**（`:472-474`，S41）
- **reroute/issue 槽**（`:484-526`）：previous_active_use_case / drift_type / current_task_type / primary_entity / issue_status_summary
- **task_status / last_entity_context_ref**（`:533-548`）
- **§N0 nullable 串**（`:560-571`）：predicted_use_case / intent_relation / reroute_action / phase_transition_reason / resolve_disposition / record_outcome_guard_result
- **terminal_evidence**（`:577-592`）、**drift_history / task_history**（`:600-605`，从历史 turn 的持久化投影**重解析**重建）
- **budget_state**（`:608-615`）、**tool_schemas**（`:621-633`，base 路径）
- **form_context / customer_context / listing_context**（`:636-648`）、**conversation_history**（`:651-668`，末 10 turn）、**knowledge_hits + knowledge_instruction**（`:670-691`）、**current_user_message**（`:694`）
- **build() 追加**：**already_called**（`:805`，S20）、**phase_plan**（`:812-912`，含 S43 的 critical_steps）、**tool_schemas 被替换**（`:845-863`）、**accumulated_tool_results**（`:916-926`）

**子问题 1：M2 之后是否结合 Skill 能力做了调整？——答：只做了一小部分。**

M2 之后 `PhasePlan` 已是 **Skill 驱动**（`PhaseEvaluator.java:445-461`）：`allowedTools(skill.toolsRequired())`、`objective(skill.objective())`、`systemInstruction(skill.procedure())`、`groundingInstruction(...)`、`escalationPolicy(...)`、`validTerminalOutcomes(...)`、`maxToolSteps(...)`、`requiredContextKeys(skill.requiredContextKeys())`。

但 `ContextProjectionBuilder` 对 Skill 的消费只有**两处**：
- `phase_plan.critical_steps`（`:885-898`，Sprint 43 / M3-Eval 才加）；
- `prior_use_case_carry.prior_skill_name`（`:1113-1121`，Sprint 41，仅做 registry.select 取名字）。

**Skill 已声明、但投影构建器忽略的能力**（`resolve_faq_grounded_answer.yaml` 为证）：
- `required_context_keys: [form_context, customer_context, listing_context, **moderation_context**]`（YAML `:19-23`）——但投影只无条件 emit form/customer/listing（`:636-648`），**没有 moderation_context 槽**；且投影**根本不读** `plan.requiredContextKeys()` 来决定投哪些 context。Skill 声明的「我需要哪些 context」对投影**无效**。
- `state_inheritance.soft_signal_via_projection: [prior_use_case_carry]`（YAML `:52-53`）——Skill 声明「该 Skill 才需要把 prior_use_case_carry 作为软信号投影」，但投影对**所有** UC/Skill **硬编码**地投 `prior_use_case_carry`（`:472`），不读 Skill 的这个声明。
- `tools_required`：`tool_schemas` 在 base 路径用 **pre-M2 的 UC 驱动** `toolPolicyEnforcer.getVisibleToolsForUc(activeUc)` 算（`:621-633`），随后在 run-loop 路径被 **Skill 驱动的 `plan.allowedTools()`** 整段替换（`:845-863`）。即在主路径里 UC 驱动的那次计算是**算了又丢**。

**子问题 2：是否存在冗余 / 设置错误的字段？——以下为 code-grounded 候选（非确证缺陷，需审计）：**

1. **重复值**：`session.total_bot_turns`（`:349`）vs `budget_state.total_bot_turns`（`:609`）；`clarification_count`（`:350` vs `:611`）；`faq_miss_count`（`:351` vs `:613`）；`session.active_use_case`（`:348`）vs `phase_plan.use_case`（`:815`）；`session.current_phase`（`:347`）vs `phase_plan.phase`（`:814`）——同值多投。
2. **`tool_schemas` 计算两次**（`:621-633` 后被 `:845-863` 覆盖）——run-loop 路径冗余计算。
3. **`knowledge_hits` 的双路径**：run-loop `build()` 传 null（`:797`，知识改走 `accumulated_tool_results.search_knowledge`），但 `PhaseEvaluator.java:982-983/1025-1026` 与 `ControlKernel.java:1627-1628` **会**传非空 hits。即存在**两套知识投影机制**——这是 coherence 问题（M3 之后哪条是正典？两条是否都还在跑？），**不是死代码**（我已验证有非空 caller）。
4. **`task_summary`** 是把 topic_subject + UC 名 + confidence + phase 重新拼成的自然语言串（`:1333-1355`），这些都已是结构化字段；对当代 LLM 属冗余复述。
5. **§N0 「恒为 null」字段**约 15 个/turn（previous_active_use_case、drift_type、current_task_type、primary_entity、predicted_use_case、intent_relation、reroute_action、phase_transition_reason、resolve_disposition、record_outcome_guard_result、prior_use_case_carry…）。其「shape 稳定」理由（`:550-559` 注释）是**为 trace 契约 / 契约校验器**服务，但对 LLM 输入而言是噪声。
6. **`drift_history`/`task_history`**（`:1265-1305`）每 turn **重解析**最多 10 个历史 turn 的完整投影 JSON 抽 ~7 字段——既是开销，也与当前 turn 顶层槽信息重叠（顶层是当前值，history 是时间序列）。
7. **三套「已发生」表面**：`already_called`（本 run-loop 已调工具，S20）+ `accumulated_tool_results`（本 run-loop 工具结果）+ `conversation_history[].tool_calls`（历史 turn 工具）——三者语义重叠，易混。

**子问题 2 的关键约束（承重耦合，必须先于任何删减搞清）**：投影字段被三个下游消费：(a) **eval trace 契约校验器**（report 里 `cs029` 的 `CONTRACT_VIOLATION:active_use_case` 正是此校验器在要求 `active_use_case`）；(b) `drift_history`/`task_history` 重建依赖历史 turn 持久化过这些字段；(c) admin trace 渲染。**删/改字段有破坏 eval 契约的真实风险**，所以 #3 是「审计 + 谨慎收敛」，不是「砍字段」。

**system_prompt 漂移**：80 行的 `system_prompt.txt` 只解释了 `tool_schemas`、`already_called`、`accumulated_tool_results`、Skill envelope、以及一棵很长的 escalation_reason 决策树（`:33-81`）。它**从未解释** intake_state / drift_history / task_history / prior_use_case_carry / discover_disambiguation_signals / terminal_evidence / alternate_candidate_use_cases / reroute_action / resolve_disposition 等 ~20 个状态槽。投影逐 sprint 累积，prompt 没跟上——这本身就是「哪些字段承重、哪些是噪声」难以判断的原因。

---

## 3. Gap / Root-cause analysis（多层）

| 问题 | 表层症状 | 根因层 | 根因 |
|------|---------|--------|------|
| #1 | report 全红、四层设计看不到 | `infra`（eval-harness 展示层） | 渲染器自 M3 前未动；M3/M4 刻意把 report rendering 划在 scope 外，债记在 `R-eval-report-observability`（低优先级） |
| #2 | LLM Raw Response 只显最后一次 | `infra`（§1.4 Runtime-owned「trace and eval contract」） | `BotTurn` 单列模型 + run-loop 多次调用覆盖；per-call 数据在 `llm_call_log`（截断）但 admin 不 join |
| #3 | 投影臃肿 / 与 Skill 不齐 | `prompt_projection`（§3.2 Q3；§1.4 Runtime 拥有投影面，但直接喂 §1.3 LLM 决策） | M2 把 plan 改成 Skill 驱动，但投影构建器未随之回填；逐 sprint 字段累积无收敛，prompt 未跟进 |

三者共同根因：**§8 milestone 节奏把架构变更折进 scoring/runtime/plan，但把「展示层 + trace 层 + 投影面回填」当作低优先级延期**——这是一个**复利型可观测性债**（见 memory `project-observability-debt-pattern`）。

---

## 4. Design alternatives + trade-offs（按问题）

### 4.A 问题 #1 —— 直接采用既有 proposal 的 Alternative A

既有 `eval_report_coherence_proposal.md` 已给出 A/B/C 三方案并推荐 **A（additive + 清晰标注 legacy view）**：report 顶部新增「四层判定」区（suite_authority 徽章 + Tier-0/1/2/3 分区），把旧 7 指标盘**降级标注**为「Phase-5 趋势指标（informational；对 human-judgment suite 不是 gate）」。我**完全背书 A**，理由见该文 §6。本文不重复其 §5/§8/§9。

### 4.B 问题 #2 —— 呈现每次 LLM invocation

- **B1（UI-only，最小）**：admin trace viewer 增加调用已存在的 `GET /sessions/{id}/llm-calls`，把 per-call 行（按 turn_index 分组）展示在每个 step 下，作为「LLM Invocations (N)」列表。**零 server/DB 改动**。
  - 优点：最快，复用现成端点 + 表；立刻解决「看不到中间调用」。
  - 缺点：只能显示**截断 500 字符**的 request/response summary（`LlmCallLogger` 的截断），且 `llm_call_log` **不含 per-call 投影**；routing 调用 turn_index 为 null（`invokeRouting` 传 null，`LlmInvocationService.java:266`），分组需处理。无法满足「完整呈现每次调用结果」中的「完整」。

- **B2（完整保真，推荐）**：把 per-invocation 提升为一等持久化对象。在 `bot_turns` 之下新增 `bot_turn_llm_calls`（或在 `BotTurn` 加 `llm_calls jsonb` 数组），由 `AgentRunLoopImpl` 把每个 step 的**完整 raw response + 该 step 的投影 + tool_calls + latency + step_index** 落库；trace 端点返回嵌套结构；UI 在每个 turn 下渲染「Invocation 1..N」，每个可展开看各自的 Projected Context + Raw Response。
  - 优点：真正满足 human 诉求（每次调用的完整结果 + 各自投影）；与 eval harness `per_turn_trace[].llm_calls` 形态对齐；**直接成为 #3 审计的工具**（见 §9 复利）。
  - 缺点：动 server（`AgentRunLoopImpl`/`TraceWriter`/`BotTurn`）+ 新 migration + UI；存储变大（每 turn 存 N 份投影）。需注意只**追加观测**，不改 loop 行为。

- **B3（折中）**：B1 的端点复用 + 仅对 raw response 去截断（`llm_call_log` 存完整 response）但仍不存 per-call 投影。介于两者之间；不推荐（半保真，仍看不到中间投影，而中间投影正是 #3 最需要的）。

### 4.C 问题 #3 —— Projected Context 审计与收敛

- **C1（先审计，产出消费图；推荐第一步）**：不改投影，先产出一份「字段消费矩阵」——每个投影字段 × {system_prompt 是否解释 / eval trace 契约是否要求 / drift-task 重建是否依赖 / Skill 声明是否要求 / 真实 trace 里 LLM 是否引用}。这是**可逆、零风险**的诊断，把「哪些承重、哪些噪声」从猜测变成证据。强依赖 #2 的 per-call 投影可见性。
- **C2（Skill 驱动收敛）**：让投影**读 Skill 声明**——`required_context_keys` 决定投哪些 context 槽；`state_inheritance.soft_signal_via_projection` 决定是否投 `prior_use_case_carry` 等软信号；把 base 路径里被覆盖的 UC 驱动 `tool_schemas` 计算删掉（统一走 plan/Skill 驱动）。**registry/Skill 驱动，不引入 per-UC if-else**（守 §1.7）。
- **C3（去重 / 降噪）**：合并 `session.*` 与 `budget_state`/`phase_plan` 的重复值；评估 `task_summary`、§N0 恒-null 串、`drift_history`/`task_history` 重建对 LLM 的实际价值，把「为 trace 契约而投」与「为 LLM 决策而投」两类需求分离（可能 trace 契约读持久化的全量、LLM 投影读精简子集）。
  - 三者皆**语义触面**（投影直接喂 §1.3 LLM 决策）→ 必须带 §7 stanza + 真实 LLM 重跑 + bad-case suite；**删任何字段前必须过 C1 的消费图**确认无下游依赖。

---

## 5. Recommended options + rationale

- **#1 → 采用既有 proposal 的 Alternative A**（additive + 标注 legacy）。理由：解决 human 冷读困惑、干净消费 `R-eval-report-observability`、保留 anchor suite 的 Phase-5 趋势连续性、零 scoring 改动、零 server 触碰。
- **#2 → B2（完整保真持久化）**，但**可先上 B1 止血**。理由：human 明确要「记录和呈现每一次 llm invocation 调用的结果」，B1 的截断摘要 + 无 per-call 投影不满足「完整」；B2 既满足诉求，又是 #3 审计的前置工具。若 M5 排期紧，B1 作为一周内可交付的过渡，B2 作为正式形态。
- **#3 → C1 先行（审计/消费图）→ 再 C2（Skill 驱动收敛）→ 谨慎 C3（去重降噪）**。理由：投影是承重面（eval 契约 + 历史重建依赖），先证据后收敛是唯一安全路径；C2 的 Skill 驱动是「M2 未回填」的正解且天然守 anti-hardcode；C3 收益最大但风险最高，放最后并强制真实 LLM 重跑。

---

## 6. Scope split + delivery priority + 顺序约束（建议；deliver-agent 决定最终拆分）

建议组成 **M5「Observability Coherence」milestone**，3–4 个 sub-sprint：

| 顺序 | sub-sprint | 问题 | 层 | 是否语义触面 | 备注 |
|------|-----------|------|----|------------|------|
| S1 | Eval Report Coherence | #1 | infra | 否（豁免 §7） | 可独立 / 随时；零 server；即 `eval_report_coherence_proposal.md` |
| S2 | Per-invocation trace（B2） | #2 | infra | 否（豁免 §7） | server+DB+UI；**仅追加观测，不改 loop** |
| S3 | Projection audit（C1）+ Skill 驱动收敛（C2） | #3 | prompt_projection | **是（需 §7 + 真实 LLM 重跑 + bad-case suite）** | **依赖 S2 落地** |
| S4（可选） | Projection 去重降噪（C3） | #3 | prompt_projection | 是 | 风险最高；强制 bad-case + shadow |

**硬顺序约束**：**S2 必须先于 S3/S4**。只有先把「每次 invocation 各自的投影」呈现出来，S3 的字段消费判断才有据；否则是在只能看到「每 turn 最后一次投影」的情况下审计投影——会误判。

**优先级**：#1 = 中高（human 冷读已被误导，且 bad-case suite 是 §5.6 主门）；#2 = 中（调试/审计基础设施，且是 #3 前置）；#3 = 中（架构健康，但承重、需谨慎）。三者都不是 release-gate P0（P0 仍是 Single Handover Orchestrator）。若 M5 选了「UC-G/H/I/J bad-case seeding」，把 S1 放在最前（新 bad case 落进一个已能正确显示四层的 report）。

---

## 7. Layer classification + §7 stanza 预填

### #1（infra；豁免 §7）—— 见既有 proposal §8。

### #2（infra；豁免 §7，但因触 server 建议仍写一段）
```markdown
## Layer-classification + anti-hardcode stanza
**Target failure layer:** infra（observability / trace contract，§1.4 Runtime-owned）
**Tier-0 invariant:** 本 sprint 不新增 Tier-0。仅新增 per-invocation 观测记录与呈现，
不改运行时决策、不改 PII/safety floor、不改 LLM 调用次数或时序。
**Semantic hardcode:** 无。新增 `bot_turn_llm_calls`（或 `BotTurn.llm_calls` jsonb）
逐 invocation 落库 + trace 端点嵌套返回 + UI 列表渲染；零 keyword/regex/enum；
不改 AgentRunLoop 的控制流（只在既有 step 边界追加 record）。
**Generalization coverage:** 观测面变更，无语义判定。target=任一多-invocation turn（FAQ
Skill maxToolSteps=4）的 trace 显示 N 条 invocation；neighbor=单-invocation turn 仍正确；
negative/shadow N/A（不触 bot 行为/judge）。Java 测试基线不回归。
```

### #3（prompt_projection；**必须** §7）
```markdown
## Layer-classification + anti-hardcode stanza
**Target failure layer:** prompt_projection（ContextProjectionBuilder 投影面）
**Tier-0 invariant:** 本 sprint 不新增 Tier-0。投影是 §1.4 Runtime「trace and eval
contract」的一部分，但其内容直接喂 §1.3 LLM-owned 决策——收敛字段不得改变 LLM 可见的
语义信息的「可得性」，只去重/降噪/按 Skill 声明门控。
**Semantic hardcode:** 不引入。收敛走 **registry/Skill 驱动**（读 `skill.requiredContextKeys()`
决定 context 槽；读 `state_inheritance.soft_signal_via_projection` 决定软信号；删除被覆盖的
UC 驱动 tool_schemas 计算）；**禁止** per-UC if-else 决定投哪些字段。
**Generalization coverage:** target=被收敛字段的 UC（如 prior_use_case_carry 的 UC-A↔UC-C
续接）；neighbor=同 Skill 其它 UC；negative=被门控掉字段的 UC 不应回归；shadow=held-out。
**必须真实 LLM 重跑 + bad-case suite 人工判定**（mocked-LLM 不能作为投影改动的因果证据，
§5.6 eval evidence gate）。
```

---

## 8. Hard fences + non-goals

**#1**：见既有 proposal §9（display-only；不改 scoring/阈值；不动 fixture；不动 server；不把 Tier-2 显示耦合进 §5.6 人工门）。

**#2**：
- 只**追加观测**——不得改 `AgentRunLoopImpl` 的循环条件、LLM 调用次数、时序、终止语义。
- 不得改 `bot_turns` 既有列语义（向后兼容；新数据加在新表/新 jsonb 列）。
- 不得把 routing/rerank 调用混进 chat invocation 而误导（按 callType 标注）。
- 不动 `server/` 的任何运行时决策路径；不动 eval harness 的 `get_llm_calls` 契约（它已在用现端点）。

**#3**：
- **删/改任何投影字段前**，必须用 C1 消费图确认它不被 (a) eval trace 契约校验器、(b) `drift_history`/`task_history` 重建、(c) admin trace 渲染消费。
- 不得引入 per-UC 字段 if-else（§1.7）；收敛必须 registry/Skill 驱动。
- 不得在收敛中顺手改 `escalation_reason` enum / tool schema / 任何 Tier-0 floor。
- 真实 LLM 重跑为准；mocked-LLM 测试只覆盖投影渲染/wiring。

**Non-goals（三者共同）**：
- 不实现 report 里 3 个 N/A 的 Phase-5 指标（correct_tool_invocation_rate 等，pre-M3 即 N/A）。
- 不重构 ControlKernel/PhaseEvaluator 的相位机（只读其产物）。
- 不改 LLM provider / 模型 / 温度 / deadline。
- 不解决 `R-bad-case-metadata-field-name-canonicalize` / `R-case-families-...-orphan`（正交 R-item）。

---

## 9. Risk + compounding-effect analysis

- **复利 1（顺序，最重要）：#2 是 #3 的前置工具。** 先做 #2（呈现每次 invocation 的投影），#3 的字段消费判断才有据；反序则在「只能看每 turn 最后一次投影」下审计投影，会误删中间步骤才用到的字段。
- **复利 2：#3 与 eval 契约耦合。** report 里 `CONTRACT_VIOLATION:active_use_case` 证明 trace 契约校验器在硬要求某些投影字段。#3 收敛若先于「契约面 vs LLM 面分离」，可能制造新的 contract_violation（正是 #1 报告里那种红）。→ #3 必须先产出消费图（C1）。
- **复利 3：#1 与 #2/#3 解耦。** #1 是 eval-harness 纯展示，零 server；可与 #2/#3 完全并行、随时做，无顺序约束。
- **风险 A（#2 存储）**：每 turn 存 N 份投影，存储与 trace 端点 payload 变大。缓解：投影可只存 diff 或对历史 turn 截断；UI 默认折叠。
- **风险 B（#3 行为漂移）**：投影是 LLM 输入；删字段即使「看似冗余」也可能改变 LLM 行为（尤其当代模型对上下文敏感）。缓解：真实 LLM 重跑 + bad-case suite + shadow，按 §5.6；分批小步收敛。
- **风险 C（#3 误判 knowledge_hits 双路径）**：我已验证它**非死代码**（PhaseEvaluator/ControlKernel 有非空 caller）。收敛前必须先回答「M3 之后哪条知识投影是正典」，否则删错路径会破坏非 run-loop 的决策投影。
- **风险 D（human 可能更想要 #1 的 Alternative B）**：若 human 想要更干净终态（删 Phase-5 盘），需 `phase5_evaluation_design.md` §6.9 的 foundational fold-back（额外 docs 工作 + 失去历史可比性）。这是 human 判断点。

---

## 10. Observability / trace / report implications

- #2 的 B2 直接产出 admin trace 的 per-invocation 视图（每次调用各自的 Projected Context + Raw Response + tool_calls），这正是 #3 审计所需，也补齐 eval `per_turn_trace[].llm_calls` 与 admin 之间的保真断层。
- #1 的 report 四层视图 + #2 的 per-invocation trace + #3 的投影消费图，三者合起来形成一致的「评估—调试—投影」可观测闭环。
- 建议 #2 落地后，把 `LlmCallEvent` 的 500 字符截断（`LlmCallEvent.java:44`）与 `LlmCallLogger` 的截断（`:49-50,83-85`）一并审视：摘要表保留截断（成本），完整 raw 走新 per-invocation 持久化。
- 任何字段级 trace 契约变更都应同步更新 eval 侧的契约校验器（避免再产生 `CONTRACT_VIOLATION` 假红）。

---

## 11. 直接回答 human 在 #1 里的具体疑问

- **「这看起来像 copy 吗？」** —— 不是。`html_report.py` 是一份自 M3 前就存在、未更新的渲染器，不是从别处拷来的副本。
- **「是为了兼容原来 100% 的逻辑吗？」** —— 不是兼容 shim。它就是**没及时清理**的旧渲染器；M3/M4 刻意把 report rendering 划出 scope。
- **「用户可以参考它吗？」** —— 旧 7 指标盘对 **programmatic 套件（anchor/smoke）** 仍是有意义的趋势参考；但对 **human-judgment 套件（bad_cases/anchor_outcome）** 它**不是 gate**，会结构性全红（L3 维度为空 → composite ≤ 0.5 < 0.7）。bad-case 的权威结论在 `eval_interactive/case_specs/bad_cases/_manifest.md` 的「M4-Eval-Cleanup close」段，**不在 report 顶部数字**。
- **「还是因为没及时清理？」** —— 正是。OPEN 低优先级 `R-eval-report-observability`（`action_bank.md:779`）已登记此债。
- **「新逻辑/旧逻辑应该标注（仅供参考 or deprecated）。」** —— 完全同意，这就是既有 proposal 的 **Alternative A**：四层视图为主、旧 7 指标盘标注为「Phase-5 趋势（informational；非 human-judgment suite 的 gate）」。建议 M5 把该 R-item 从低优先级**提升到中高**并排期。

---

## 12. 给 human 的一句话总结

三个问题是同一个病的三个症状：**可观测性表面（eval report / admin trace 的 LLM raw response / Projected Context）落后于架构（M2 Skill、M3 四层评估）**。#1 已有准确 proposal，背书即可；#2 的「每次 invocation」数据已存在于 `llm_call_log` 且有端点、只是 admin 没 join（建议 B2 完整持久化）；#3 投影在 M2 后只回填了两处 Skill 能力、其余仍是 UC 时代的固定字段集 + 冗余 + 噪声（建议先审计后收敛，且 **#2 先于 #3**）。三件可成一个 M5「Observability Coherence」milestone，由 deliver-agent 做最终 scope 拆分。
