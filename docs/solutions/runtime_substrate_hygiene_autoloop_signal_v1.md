---
title: Runtime substrate-hygiene 整体方案 — 让 autoloop fitness 信号变干净（7 类全局缺陷的因果簇 + 模块化 + 顺序依赖）
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-01
mode: forward-looking
supersedes: []
superseded_by: null
notes: >
  Mode-1（forward-looking / Path-1）整体设计。消费 docs/action_bank.md §5.2
  "Sprint 065 / S-Auto-10 bad_cases trace-dive (2026-06-01)" 的 7 条 R-item +
  既有 R-runtime-orchestrator-tool-call-deduplication（2026-05-13）+ 2 篇既有
  research 提案（escalation_reason_runtime_evidence_contract_maxsteps_misstamp.md
  / discover_overclarify_and_search_catch22.md，均 2026-05-24，status: proposal，
  均未实现），把它们综合成一个 substrate-hygiene 架构，划分大模块 + 模块间顺序
  依赖，供 deliver-agent 切 milestone。这是 Mode-1 整体方案，不替 deliver-agent
  做最终 milestone/sprint 拆分；不写业务代码。
  所有代码引用在 HEAD b351648 验证。
---

# Runtime substrate-hygiene 整体方案

## 0. Executive summary

autoloop 拿不到第一个可信 cherry-pick 的 binding constraint，**不是评估噪声，是
customer-service agent runtime 的一批"工具调用纪律 + 终态语义诚实性"缺陷**。它们让
每个 case 的执行路径在 byte-identical 输入下剧烈发散（storm→max-steps→误盖 reason），
使 `tier1_bad_cases_regression_5_to_0` gate 测量的是这些 bug 制造的噪声，而非 propose
quality。

本方案的核心论断有四：

1. **因果结构是"扇入收敛"（fan-in），不是线性链。** human 的假设链
   （gating-race → storm → max-steps → mis-stamp）方向对，但拓扑需要修正：多个**相互
   独立的"浪费步数"源**（identical-retry-storm 15/24、paraphrase-storm 11/24、
   gating-race 4、以及既有提案揭示的 clarification-budget 死代码）**并联**汇入同一个
   下游 sink（max_steps / budget 耗尽 → `resolveMaxStepsReason` / `mapBudgetToEscalationReason`
   单点误盖 reason）。修上游任一"浪费源"都能降低耗尽频率；修下游 sink 只能让"已经发生的
   耗尽"变诚实，不减少浪费。两者治不同的病，都要做，但顺序不同。

2. **这批缺陷应组织成 4 个大模块**：A 工具调用纪律（上游切断，载重）、B Escalation
   语义一致性（下游诚实化）、C Classifier 稳定性 bounding（vendor 噪声，只能 bound 不能
   消除）、D Eval-harness 鲁棒性（simulator，独立可并行）。

3. **顺序约束的关键红线**：**A 必须先于 B 的 eval_spec 配套**。若先在 case_spec 侧
   "接受 turn_budget escalation / 重映射 reason"，会在 eval 层把 storm 的浪费症状盖住
   —— 直接违反 Constitution §5.4（不得用 eval-side override 掩盖真 bot bug）。

4. **autoloop 解锁的最小模块子集 = A + B1（runtime resolver 诚实化）**；D 是低成本并行
   赢；C 与 B 的 eval_spec 配套**不是**第一个 cherry-pick 的前置。可观测解锁判据见 §11。

**与既有 2 篇提案的关系（coverage check 结论）**：本方案**不重新设计** Module B 和
gating-race —— 那两篇 2026-05-24 Mode-2 提案已给出 code-grounded 单症状设计，本方案把
它们作为模块内的承重组件**引用并嵌入**。本方案的独立增量是：(a) 确认 7+ 缺陷是**同一个
substrate-hygiene 主题的扇入收敛**而非孤立 bad case；(b) 给出**跨模块顺序**；(c) 解决两篇
提案与新 R-item 之间的 **enum 冲突**（§4）；(d) 给出**autoloop-fitness-cleanliness 解锁
判据**这一全新视角（两篇 Mode-2 提案早于 autoloop-signal-corruption 框架，未覆盖）。

---

## 1. Current-state survey（code-grounded，HEAD b351648）

### 1.1 Agent run loop 的两层循环（全部分析的地基）

`AgentRunLoopImpl.run(...)`（`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`）：

- **外层** `for (int step = 0; step < maxSteps; step++)`（:179）：每个 step = 一次
  projection + 一次 `llmInvocation.invokeChat`。`maxSteps = plan.maxToolSteps()`（:166）。
- **内层** `for (ToolCall call : calls)`（:316）：对**一次 LLM 响应里的每个 tool_call**
  顺序 dispatch。
- **关键事实①（无去重）**：内层循环对 `calls` 里的每个 call **无条件**
  `toolDispatcher.dispatch(...)`（:449）。**没有任何 `(tool_name, args)` 去重**。一次响应
  里 6 个 identical `get_customer_context` 会 dispatch 6 次；跨 step 的重复同样无拦截。
- **关键事实②（软信号是 observation-only）**：`already_called` projection slot 由
  `ContextProjectionBuilder` 注入（:182-187 注释明示 "Slot is observability-only; no
  short-circuit on dispatch"）。
- **关键事实③（classify 提交即返回）**：classify_use_case 在 DISCOVER plan 上成功提交非空
  `activeUseCase` → 内层循环**立即 return `USE_CASE_IDENTIFIED`**（:520-535），不再 dispatch
  本响应剩余的 call。
- **关键事实④（max_steps 出口不带 reason）**：循环耗尽 → `AgentRunResult.maxSteps(...)`
  （:554），**此处不设 escalation_reason** —— reason 由下游 `PhaseEvaluator` 计算。

### 1.2 去重的"半成品"已经存在（重要）

`ContextProjectionBuilder`：

- `canonicalArgumentsHash(args)`（:1267）：对 args map 做 **order-insensitive SHA-256**，
  截断 16 hex。**这正是去重需要的 key，且已实现。**
- `buildAlreadyCalledNode(priorToolEvents)`（:1235-1251）：产出 `{tool, arguments_hash,
  at_step}` 数组，只收 `success==true` 的事件，跨 step。

即：**确定性去重所需的 key 与软信号 slot 早已在代码里，缺的只是"在 dispatch 处用它做
幂等回挡 + 把软信号从 observation 升级为有约束力"。**

### 1.3 工具门控与 use_case='none'（gating-race）

`ToolDispatcher.dispatch`（:86-103）：dispatch 时取 `session.getActiveUseCase()`（:97），调
`policyEnforcer.isToolAllowed(toolName, activeUseCase)`（:98）；不允许则返回
`"Tool '%s' is not allowed for use case '%s'"`（:100-102，activeUseCase 为 null 时渲染为
`'none'`）。`ToolPolicyEnforcer.isToolAllowed`（:65-75）：`allowed-ucs` 含 `ALL` 才放行，
否则要求 `activeUseCase` 命中 allowed-ucs。

`tool-policy.yaml` 的**不对称**（引自 `discover_overclarify_and_search_catch22.md` §1.3，
HEAD 仍然如此）：`classify_use_case: [ALL]`（UC 未定时即可调），`search_knowledge:
[UC-A..UC-FP]`（**DISCOVER/none 必被拒**）。

**gating-race 的真根因不是 dispatcher，是两份 config 自相矛盾**：`discover_triage.yaml`
procedure 写"先 search 再 classify"，但 tool-policy 让 search 在 none 态必被拒。LLM 越听话
越撞 `not allowed for use case 'none'`，浪费一步。（`discover_overclarify_and_search_catch22.md`
§2.2 RC2 已 code-grounded。）

### 1.4 max_steps → escalation_reason 的单点误盖

`PhaseEvaluator.resolveMaxStepsReason(plan, result, session)`（:173-196）按优先级：

1. INTAKE UC → `incomplete_intake`（:176-178）
2. `clarificationCount > 0` → `clarification_budget_exhausted`（:179-182）
3. **遍历 toolEvents，只要出现过一次 `search_knowledge` → `faq_miss_threshold_exceeded`
   （:183-193）**
4. 否则 → `turn_budget_exhausted`（:195）

**关键缺陷**：第 3 步**只问"调过 search_knowledge 没"（`te.toolName()`），从不读该次调用
结果的 `faq_miss` 旗标**。于是一串返回 `faq_miss=false`+grounded hits 的 paraphrase-storm
把 max_steps 吃满后，被**误盖** `faq_miss_threshold_exceeded`。这与 user memory
`project_faq_overescalate_maxsteps_misstamp` 同源：Sprint 39 guardrail 只堵了 LLM 自盖
向量，这条 runtime-fallback 向量敞开。`escalation_reason_runtime_evidence_contract_maxsteps_misstamp.md`
§1.2 / §2.1 已 code-grounded 同一结论。

### 1.5 classify 盖写与稳定性

`ClassifyUseCaseTool.execute`（:136）：`session.setActiveUseCase(useCaseId)` **直接盖写**
LLM 提出的 UC，唯一例外是 `shouldPreserveStrongPrior`（:121, :188-206）——仅当 form-context
强先验存在且当前 activeUseCase 恰等于该强先验时才拒绝盖写。**不存在通用的"session 级 sticky
cache"。** 但 §1.1 关键事实③ 说明：classify 一旦提交即返回 USE_CASE_IDENTIFIED，正常情况下
一个 session 只 commit 一次 —— 所以**session 内**翻转其实少见；R7 的承重子集（2/24 真随机）是
**跨 rerun 的 vendor 噪声**（temp=0 在共享池推理上不保证 bit-exact），runtime **消除不了**，
只能 bound。

### 1.6 既有提案揭示的同族邻接缺陷（不在 7 R-item 内，但同一主题）

`discover_overclarify_and_search_catch22.md` 额外揭示两条 HEAD 仍在的 substrate 缺陷：

- **RC3（§2.3）**：预算强制升级时 `inferFallbackUseCase`（`ControlKernel.java:1462-1474`）用
  裸正则 `\b(ad|...)\b` 盖一个 UC（conf 0.30），仅为过 trace contract validator —— 即
  §1.7 警告的 semantic hardcode 被当"凑合过 contract"用，且与 escalation mis-stamp 同族
  （"运行时盖一个 LLM 从没选过的东西"）。
- **RC4（§2.4）**：`clarificationCount` 唯一自增点在 legacy `evaluateDiscover:865`，而 DISCOVER
  实际走 AgentRunLoop 路径 → 该计数全程为 0 → `clarification_budget_exhausted` **永不触发**。
  最诚实的预算 reason 在 live 路径上是死代码，于是过度澄清一路跑到 repeated-action 桶被
  `turn_budget_exhausted` + 关键词 UC 盖戳收尾。

这两条是同一 substrate-hygiene 主题里的承重邻接项，本方案在 Module A/B 里标注它们的归属，
建议 deliver-agent 折入（见 §6 coverage check）。

---

## 2. Gap / 因果簇验证（Mode-1 核心产出：验证或推翻 human 的因果链假设）

### 2.1 拓扑修正：扇入收敛，不是线性链

human 假设：`gating-race → retry/paraphrase-storm → max-steps 耗尽 → escalation mis-stamp`。

**code-grounded 裁决：方向正确，拓扑需修正为 fan-in。** 理由：

- gating-race 发生在 **DISCOVER**（activeUseCase=none，classify 之前）；retry/paraphrase-storm
  发生在 **RESOLVE**（classify 之后）。二者**不在同一条时间链上**，是**并联**的两个独立"浪费步数"
  源，各自把执行推向耗尽。把它们画成串联会误导"修了上游就自动消下游"。
- 真正的串联只在**尾部**：所有"浪费源"→ 同一个**耗尽事件**（max_steps 或 budget cap）→ 同一个
  **单点 reason 计算器**（`resolveMaxStepsReason` / `mapBudgetToEscalationReason`）→ 误盖。

```
[gating-race (DISCOVER)]  ┐
[identical-retry-storm]   ├─(并联,各自吃步数)→ [max_steps/budget 耗尽] → [单点 reason 计算器] → [mis-stamp]
[paraphrase-storm]        ├                                                         └→ [turn_budget 被 case_spec 当 intent escalation]
[clarification 死代码 RC4]┘
[classifier 噪声 R7] ──(另一条:错 UC → 错 skill/错答 → 也可能吃步数,但主要是 PASS/FAIL 直接翻转)
```

### 2.2 "修哪个上游能切断最多下游症状"

- **降低耗尽频率（治路径发散）**：去重（R1，15/24，**单一最大源**）> paraphrase 约束（R2，11/24）
  > gating-race（R3，4）> clarification 死代码复活（RC4）。修这些 → 耗尽事件变少 → mis-stamp 与
  turn-budget-conflated 的**发生次数**同步下降 + 每 case turn 数收敛（这正是 bad_cases 在判别
  边界抖动的根因）。
- **让"已发生的耗尽"诚实（治语义可读性）**：`resolveMaxStepsReason` evidence-aware（B1）是**唯一
  单点**，一次修复让所有残余耗尽的 reason 诚实。它**不减少**浪费，但让 fitness 的 escalation-correctness
  维度可信。

**结论**：没有"一个上游切断全部"。**R1 去重是降低耗尽频率的最高杠杆单点**（砍掉 15/24 主导噪声源），
**B1 resolver 诚实化是语义可读性的最高杠杆单点**。两者治不同病，构成 Module A 与 Module B 的核心。

### 2.3 R7（classifier）不属于这条扇入

classifier 噪声主要不是"吃步数"，而是**直接翻转 PASS/FAIL**（错 UC → 错 skill → 错答/错升级，
终态直接落到错窗口）。且其承重子集是**跨 rerun vendor 噪声**，runtime 消除不了。它是**另一条独立的
噪声通道**，归 Module C，单独 bound，**不在 A→B 的关键路径上**。

---

## 3. Design alternatives + trade-offs（逐模块）

> Module B 与 gating-race 的方案主体来自既有 2 篇提案，本节**引用其推荐选项**并标注 trade-off，
> 不重复其完整推导。

### Module A — 工具调用纪律（上游切断，载重）

**A1 identical-retry-storm 去重（R1；R-runtime-orchestrator-tool-call-deduplication 的写侧重开）**

- **A1-x（推荐）幂等回挡 + 软信号升级（path-3 hybrid）**：在 dispatch 处维护本 run 的
  `Set<(toolName, canonicalArgumentsHash)>`（复用 §1.2 既有 `canonicalArgumentsHash`）；命中
  duplicate-key 时**返回上次缓存结果 + trace 标注 warning + 不计步/不计预算**，并把
  `already_called` slot 从 observation-only **升级为有约束力的软信号**（projection 明确告诉 LLM
  "本 turn 已用这些 args 调过这些工具，勿重复"）。
  - 优点：确定性回挡是 backstop，软信号让 LLM 自收敛；**纯幂等，不做任何语义判断**，对所有工具
    一致适用，复用既有 hash。LLM 仍拥有"调哪个工具/语义内容"。
  - 缺点：缓存回挡若仅做内层去重，跨 step 顽固重发仍烧 LLM step（需软信号配合）。故必须 hybrid。
- **A1-y（仅软信号）**：只升级 `already_called`，不做缓存。
  - 缺点：**已被证伪**。Sprint 19 §4.3 正是选了"软信号优先"并 reclassify 到 semantic_planner，
    Sprint 20 交付了 `already_called` slot —— 12 个 sprint 后、bot_temp=0 下 storm 仍 15/24。
    软信号单独试过且未收敛 → 这是 A1-x 引入确定性 backstop 的**反硬编码正当性依据**（§7）。
- **A1-z（仅缓存，不升级软信号）**：LLM 看不到"已调过"，可能换路径继续 storm。劣于 hybrid。

**A2 gating-race / DISCOVER search catch-22（R3；引用 `discover_overclarify_and_search_catch22.md` §5.A）**

- **A2-A1（推荐）让 skill 说真话**：改 `discover_triage.yaml` procedure 为"**先 classify 再
  （在 RESOLVE）search**"，与 tool-policy 现状（classify=[ALL]、search 需 UC）一致。**零运行时代码
  改动；且 procedure 是 autoloop 可变面（`$.procedure`）—— 这是 skill-layer fix，将来 autoloop
  自己也能优化。**
- **A2-A2（备选）改 tool-policy 给 DISCOVER 放行 search_knowledge**：保留"先搜后分类"设想，但扩大
  DISCOVER 工具面、需重核语义风险。config-governance fix。
- **A2-runtime（次选，新 R-item path-1）**：dispatcher 把 classify_use_case **hoist 到 calls
  最前**先 commit 再 dispatch 其余。runtime fix，但既有提案判定"让 skill 说真话"比"改网关迁就
  skill"更 contained、更合设计意图。
- **取舍**：优先 A2-A1（skill/config，LLM-first、autoloop-touchable），A2-runtime 仅作为 A2-A1
  之外仍残留乱序时的确定性 backstop 备选。

**A3 paraphrase-storm（R2；soft-signal-first per §1.5）**

- **A3-skill（推荐主路，autoloop-touchable）**：在 `$.grounding_instruction` / `$.procedure` 加
  "若一次 `search_knowledge` 返回 hits 且 `faq_miss=false`，本 turn 勿再 search —— 用既有 hits
  起草回复或升级"。+ **A3-proj**：projection 显式回投上一次 search 结果（让 LLM 不必"再搜确认"）。
- **A3-cap（可选 backstop，infra）**：硬上限 search_knowledge ≤2 次/turn，除非上次 `faq_miss=true`。
  - 取舍：per §1.5 先做 skill+projection（LLM 拥有"是否再搜"），cap 仅作确定性兜底，且 cap 是结构性
    cardinality 限制（非语义硬编码），可接受但不首选。

### Module B — Escalation 语义一致性（下游诚实化；引用 `escalation_reason_..._maxsteps_misstamp.md`）

**B1 resolveMaxStepsReason evidence-aware（R4；该提案 §5.A1 推荐项）**

- **B1（推荐）**：改 `resolveMaxStepsReason` 读 `ToolEvent.resultData` 的 `faq_miss` 旗标；当本轮
  有 viable hits（最近一次 `faq_miss=false`）时，回退到 **`turn_budget_exhausted`**（既有 catch-all,
  PhaseEvaluator:169,195）而非 `faq_miss_threshold_exceeded`。
- **不推荐**：新 R-item path-1 的"新增 `max_steps_exhausted` enum 值"——见 §4 enum 冲突裁决。

**B2 clarification 预算死代码复活（RC4；该提案 §5.B 推荐项 B1）**

- 在 AgentRunLoop live 路径补 `clarificationCount` 自增，让 cap=2 生效，使过度澄清以最贴切的
  `clarification_budget_exhausted` 诚实收场，而非跑到 repeated-action 桶被 turn_budget + 关键词 UC 盖戳。
- infra / skill_state，低风险。

**B3 turn-budget conflation eval 配套 + fallback-UC 诚实（R5 + RC3；最后做）**

- R5（turn-budget 被 case_spec 当 intent escalation）：**eval_spec 配套**。推荐既有 R-item path-1
  的结构化 `expected_outcome: {outcome, valid_reasons}`，把 `turn_budget_exhausted` 显式排除在
  "intent-driven escalate"之外。**必须在 B1 让 runtime 先吐诚实 reason 之后**，否则 case_spec 校验
  的是被污染的 reason。
- RC3（fallback-UC 关键词盖戳）：在真分类可靠（A2/C 落地）后，把 `inferFallbackUseCase` 盖戳在 trace
  显式标注 "runtime keyword fallback, not LLM-classified"；是否移除裸正则须 `human_review_required`
  （Tier-0-adjacent，与 escalation-reason-contract 同议）。

### Module C — Classifier 稳定性 bounding（vendor 噪声；bound 非消除）

- **C1（推荐）sticky-within-session + 低置信投射 alternate-candidate 软信号**：session 内 classify 一旦
  commit 即 sticky（多数已由 §1.1③ 保证）；当 confidence < 阈值（如 0.85）把 alternates 作为软信号
  投到 projection（`prompt_projection`），让 LLM 消歧。**autoloop-touchable**（若 alternate-candidate
  slot 暴露）。
- **不做**：用 keyword/regex/UC-matrix 强制分类（§1.7 forbidden；现有 `shouldPreserveStrongPrior` 已是
  borderline 强先验，**勿再扩**）。
- 取舍：vendor 跨-rerun 噪声 runtime 消除不了（且 user 已显式排除 vendor seed 路径）；C 只能 bound +
  observe。Forward-looking：6 个月后主流 LLM 分类稳定性提升，C 的重要性下降 —— **勿过度工程**。

### Module D — Eval-harness 鲁棒性（simulator；独立可并行）

- **D1（推荐）turn0 确定性 seed-message + 强化 parse-retry**：cases 无 `persona.seed_messages` 时
  turn0 仍走 LLM（`user_simulator.py:107-110`），改为 turn0 旁路 LLM 用 seed/form_context；+ 强化
  `_parse_simulator_response` 失败时带 schema 的 N=3 重试。
- eval_spec / harness 鲁棒性，与 runtime 模块**完全独立**，可与 Module A 并行，砍掉 3/24 turn0 flake。

---

## 4. Enum 冲突裁决（true-conflict resolution per doc_governance.md）

**冲突**：
- 新 R-item `R-runtime-escalation-reason-misstamp-maxsteps-faq` path-1/-3 建议**新增 `max_steps_exhausted`
  enum 值**。
- 既有提案 `escalation_reason_..._maxsteps_misstamp.md` §4 / §9 **硬约束：不得新增/重命名
  escalation_reason enum 值**（`D-new-escalation-reason-enum` deferred），推荐复用 `turn_budget_exhausted`。

**code-grounded 裁决：采纳既有提案（复用 `turn_budget_exhausted`），拒绝新增 enum。** 理由：该 enum 是
**23 值、三处镜像**：`EscalationReasonResolver.CANONICAL_REASONS`（:51-75）、
`ToolDispatcher.CANONICAL_ESCALATION_REASONS`（:35-58）、`PhaseEvaluator`（同集），并 cross-cut
eval 侧 `ESCALATION_TRIGGER_VALUES`。新增一个值要同改三处 Java + eval schema + 触及所有相关 case_spec，
是**跨切面扩散**，且 `turn_budget_exhausted` 本就是"耗尽且无更具体语义"的诚实 catch-all —— 当本轮有
viable hits 时盖它，**不撒 faq 谎**，语义诚实且 contained。deliver-agent 应在 consumption 时确认采纳此裁决。

---

## 5. Recommended option（整体推荐 + 理由）

**整体推荐**：以 **Module A 为载重上游切断**，**Module B1 紧随其后做 runtime 诚实化**，**Module D 并行**，
**Module C 与 B3 殿后/可选**。

理由（Constitution-aligned）：
- A 是降低路径发散的最高杠杆，且其主路（A2/A3 skill+projection）是 **LLM-first、autoloop-touchable**；
  唯一的确定性引入（A1 去重回挡）是 **Runtime 拥有的 idempotency 不变量（§1.4）**，不是语义硬编码，且
  软信号单独试过未收敛（§7 正当性）。
- B1 是**单点**让 runtime reason 与观测事实一致（Runtime 拥有 trace/eval contract，§1.4），且**移除**了
  一个有损 Java 启发式（toolName-presence→faq 标签），反而**降低**硬编码面。
- D 独立、便宜、并行。
- C 是 vendor 噪声 bounding，不在解锁关键路径，勿过度工程。

---

## 6. Coverage check（与既有 backlog / 提案对比；Mode-1 必做）

| 既有条目 | 关系 | 处置建议 |
|---|---|---|
| `R-runtime-orchestrator-tool-call-deduplication`（action_bank:426，partial→semantic_planner） | **同一缺陷的前身** | Module A1 是其**写侧重开**。Sprint 19 把它 reclassify 到 semantic_planner + 拆出软信号侧（Sprint 20 `already_called` 已交付，observation-only）。新证据（15/24 @ bot_temp=0）证明软信号单独不收敛 → A1-x 引入确定性回挡。建议把该 R-item 的写侧标 `R-runtime-identical-tool-call-retry-storm` 为继任。 |
| `escalation_reason_..._maxsteps_misstamp.md`（2026-05-24 proposal，未实现） | **Module B1 的设计主体** | 直接引用其 §5.A1。本方案不重做，只补"放进扇入收敛 + 顺序约束 + enum 裁决 + autoloop 解锁视角"。 |
| `discover_overclarify_and_search_catch22.md`（2026-05-24 proposal，未实现） | **Module A2 + B2(RC4) + B3(RC3) 的设计主体** | 引用其 §5.A1（skill 说真话）/§5.B（clarification 复活）/§5.D（fallback-UC 诚实）。RC3/RC4 是 7-R-item 未捕获的同族邻接项，建议折入 Module A/B。 |
| `R-escalation-reason-runtime-evidence-contract-review`（action_bank:390，Tier-0 candidate） | **Module B 的 Tier-0 议题** | B1 触及它；per-sub-sprint Codex REQUIRED（§4.3 触发 #1）。 |
| `R-runtime-escalation-reason-turn-budget-conflated-with-intent`（R5） | Module B3（eval_spec 配套） | 殿后；A 之后。 |
| `R-classifier-non-deterministic-uc-selection-at-temp-zero`（R7） | Module C | 独立 bounding，非解锁前置。 |
| `R-simulator-first-message-contract-violation-flake`（R6） | Module D | 独立并行。 |
| `R-overnight-eval-traces-not-persisted`（action_bank 2026-05-31） | **观测配套（载重）** | §10：要验证 substrate 修复是否让 fitness 变干净、要做 §5.6 overnight 候选人 trace review，**必须**先解此项；建议与 Module A 同窗口或先行。 |
| `R-tier1-bad-cases-regression-5-to-0-attribution-unverified`（action_bank 2026-05-31） | **直接下游** | Module A 让 baseline 收敛后，5→0 归因（a 真 propose 回归 / b calc bug / c 窄面）才可诊断。§11 解锁判据覆盖。 |
| `R-autoloop-run-sweeps-dirty-index`（action_bank 2026-05-31） | 正交 | autoloop git 卫生，与本 substrate 主题无重叠；不在本方案 scope。 |

---

## 7. Layer classification（§3.2）+ §7 stanza 预填（逐模块）

### Module A — 工具调用纪律

**§3.2 分类**：A1=`infra`（dispatcher 幂等/状态一致性，Q1/Q4）；A2 主路=`prompt_projection`/skill +
config-governance（Q3；skill↔tool-policy 矛盾），A2-runtime 备选=`infra`；A3 主路=`semantic_planner` +
`prompt_projection`（Q5/Q3），A3-cap 备选=`infra`。无 Tier-0（无现有 Tier-0 覆盖 tool-call cardinality；
若有人主张 Tier-0 升格 → `human_review_required`，**不自创 Tier-0**）。

```
## Layer-classification + anti-hardcode stanza  (Module A)

**Target failure layer:** infra（A1 去重 + A2-runtime/A3-cap backstop）/ prompt_projection
+ semantic_planner（A2/A3 主路 skill+projection）。

**Tier-0 invariant:** 本模块不新增 Tier-0 invariant。A1 去重是 Runtime 既有
idempotency/persistence 职责（§1.4）的延伸；不在 docs/runtime_freeze_and_risk_policy.md
§1/§2 新增条目。若评审主张把"per-turn tool-call 幂等"升格 Tier-0，走 human_review_required。

**Semantic hardcode:** 不引入语义硬编码。A1 去重 key 复用既有 canonicalArgumentsHash
（order-insensitive，对所有工具一致，无 keyword/regex/enum/per-UC matrix）；命中只回挡
byte-identical 调用，不做任何语义判断，LLM 仍拥有"调哪个工具/内容"。引入确定性回挡的正当性：
软信号优先（Sprint 19 §4.3 选择）已交付（Sprint 20 already_called）且 12 sprint 后 storm 仍
15/24 @ temp=0——soft-signal-first 已试且未收敛，幂等 backstop 是 §1.4 Runtime 职责而非 §1.5
违反。A2/A3 主路是 skill 软字段 + projection 软信号（autoloop-touchable），不在 Java 加规则。

**Generalization coverage:** target = bad_cases 8 flipping cases 的 storm/gating 子集；
neighbor = anchor_outcome + shadow 同 storm 形态；negative = 正常单次调用不被去重误挡 /
classify-first 不破坏合法 RESOLVE 多调用；shadow = held-out。counts 由 deliver-agent 在
case-family 时定（建议 ≥ 3-pass bad_cases 重跑作为 IMPROVING 证据）。
```

### Module B — Escalation 语义一致性

**§3.2 分类**：B1=`infra`（resolver 与观测事实一致，Q3-trace contract）+ Tier-0 candidate（须 human
决策，per-sub-sprint Codex REQUIRED）；B2=`infra`/`skill_state`；B3=`eval_spec`（R5 case_spec，Q6）+
`infra`/`human_review_required`（RC3 fallback-UC）。

```
## Layer-classification + anti-hardcode stanza  (Module B)

**Target failure layer:** infra（B1 resolver evidence-aware；B2 clarification 计数）/
eval_spec（B3 R5 case_spec valid_reasons）/ human_review_required（B3 RC3 fallback-UC 去留）。

**Tier-0 invariant:** B1 触及 R-escalation-reason-runtime-evidence-contract-review（Tier-0
candidate）；是否将"escalation_reason 须与 runtime 证据一致"升格为 Tier-0 由 human 决策——
本 stanza 不预先新增，触发 §4.3 #1 per-sub-sprint Codex REQUIRED。

**Semantic hardcode:** 不新增/重命名 escalation_reason enum 值（§4 裁决：复用既有
turn_budget_exhausted，不引入 max_steps_exhausted）。B1 是读 ToolEvent.resultData 的 faq_miss
旗标做诚实回退——移除一个有损 Java 启发式（toolName-presence→faq 标签），净降硬编码面。
B3 RC3 移除/标注裸正则 fallback-UC 须 human_review_required，不在本模块擅自删。

**Generalization coverage:** target = max-steps escalation 的 faq/turn-budget 误盖子集
（cs001/cs014/cs095/wmkb）+ cs040/cs176 回归；neighbor = INTAKE incomplete_intake 同函数其他
分支；negative = 真 faq_miss=true 仍盖 faq_miss_threshold_exceeded（不误转）；shadow = held-out。
顺序：B1 runtime 先于 B3 eval_spec（§8 红线）。
```

### Module C — Classifier bounding

```
## Layer-classification + anti-hardcode stanza  (Module C)

**Target failure layer:** semantic_planner（classifier 行为）+ prompt_projection
（低置信 alternate-candidate 软信号）。

**Tier-0 invariant:** 不新增 Tier-0。

**Semantic hardcode:** 不引入。禁止用 keyword/regex/UC-matrix 强制分类（§1.7）；不扩
shouldPreserveStrongPrior 既有强先验面。sticky-within-session 是结构性状态一致性；
alternate-candidate 是软信号投射，LLM 拥有消歧。vendor 跨-rerun 噪声不可由 runtime 消除
（且 user 已排除 vendor seed），本模块 bound 非消除。

**Generalization coverage:** target = 2/24 真随机翻转 case（cs012/cs015）；neighbor = 8-flipping
envelope 其余；negative = 强先验 case 不被 alternate 软信号扰动；shadow = held-out。
```

### Module D — Eval-harness（§7 stanza **EXEMPT**）

Module D 是 simulator/harness 鲁棒性，不触及 agent 语义面 → per §7 / §4.1 **exempt**（characterization /
harness-robustness carve-out）。dev/Codex 在 verdict 里显式列出豁免即可。

---

## 8. Hard fences + non-goals + 顺序约束（compounding-effect，Mode-1 核心）

### 8.1 跨模块顺序（DAG）

```
Module D  ──────────────(并行,独立)──────────────┐
Module A (A1去重 + A2 skill + A3 skill/proj)  ──┬─→ Module B1 (runtime resolver 诚实化)  ──→ Module B3 (eval_spec 配套 + fallback-UC)
                                                └─→ (B2 clarification 复活,可与 B1 同窗)
Module C  ──────────(低优先,可并行;非解锁前置)──────────
```

### 8.2 顺序红线（错序会更糟）

1. **【最重要】A 必须先于 B3 的 eval_spec 配套。** 若先在 case_spec 侧"接受 turn_budget escalation /
   重映射 reason"，会在 eval 层把 storm 的浪费症状盖住 —— 违反 §5.4（不得 eval-side override 掩盖真
   bot bug）。这正是 user Q3 担心的"先调 case_spec 接受 turn_budget 会掩盖 retry-storm 真实症状"——
   **会，所以禁止**。
2. **B1（runtime 改 reason）与 B3（eval_spec 配套）必须协同或紧邻、B1 先。** B1 让本轮有 hits 的耗尽吐
   `turn_budget_exhausted` 而非 `faq_miss_threshold_exceeded`；若 B1 单独落地而 case_spec 仍期待旧 reason，
   期待 `faq_miss_threshold_exceeded` 的 case 会开始 FAIL。故 B1 落地后 eval 侧 valid_reasons 需同步更新
   （这是 §4 裁决"复用既有 enum"的另一好处：不新增值，case_spec 改动面最小）。
3. **A1 去重必须 hybrid（回挡 + 软信号），不可只做一半。** 只回挡不升级软信号 → 顽固 LLM 换路径继续烧
   step；只升级软信号不回挡 → 已被 12 sprint 证伪（仍 15/24）。
4. **B3 RC3 / C 应在"真分类可靠"之后。** fallback-UC 关键词盖戳与 classifier bounding 都假设真分类先
   work；顺序倒置会让"诚实标注 fallback"标注在一个仍频繁触发的路径上，价值打折。
5. **C 不得阻塞解锁。** C 是 vendor 噪声 bounding；把它放进解锁关键路径会无谓拖延 first cherry-pick。

### 8.3 Hard fences / non-goals

- 不替 deliver-agent 做最终 milestone/sprint 拆分（本方案的模块 + 顺序是**建议**）。
- 不写业务代码（proposal-only）。
- **不推荐 vendor `seed` 路径**（user 已显式移除；C 仅 sticky+projection bounding）。
- **不新增 escalation_reason enum 值**（§4 裁决；复用 turn_budget_exhausted）。
- 不把 bad_cases 的 §5.6 human-judgment primary-gate 降级成 autoloop 程序化 gate。
- **不在本轮拓宽 autoloop mutable surface**（Stage-2 是 substrate 干净之后的独立决策，per S-Auto-10 §11 /
  M-Auto-2 §12.8）。注意：A2/A3/C 的 skill-layer 主路**恰好落在现有 mutable surface 内**（`$.procedure`/
  `$.grounding_instruction`）——本方案建议**由 research→deliver→dev 正常交付这些 skill 修复**，而非靠
  autoloop 自动产出；这不是拓宽 surface，是先用人工交付把 substrate 修干净。
- 不用 keyword/regex/if-else/enum expansion 解语义失败（§1.5/§1.7）。

---

## 9. Risk + compounding-effect analysis

| 风险 | 说明 | 缓解 |
|---|---|---|
| A1 去重误挡合法重复 | 某些 UC 合法地对同一工具用相同 args 调两次（罕见但存在，如分页/重试外部失败） | 去重只在 `success==true` 的上次结果上回挡（与 already_called 一致）；外部失败的调用 result 非 success，不进缓存，可合法重试。negative-control case 必须覆盖。 |
| A2 skill 改 procedure 触发 autoloop 漂移 | discover_triage.procedure 是 autoloop 可变面，人工改后 autoloop baseline 需对齐 | 人工交付走正常 dev/review；baseline_dir 在 substrate 修复后推进（与 cherry-pick 同机制）。 |
| B1 改 reason 触发既有 case_spec 回归 | 期待 faq_miss_threshold_exceeded 的 case 改判 turn_budget | §8.2 红线 #2：B1 + eval valid_reasons 协同；先跑 cs040/cs176 回归。 |
| 先修 B3 eval 掩盖 A 的 storm | §8.2 红线 #1 / §5.4 | 强制 A 先于 B3。 |
| C sticky 误把"该重分类"的 session 钉死 | 真 hard-shift（drift）需要换 UC | DriftDetector 在 bot loop 前 flip activeUseCase（既有路径，ClassifyUseCaseTool 注释 :182-187）→ sticky 只钉 LLM 自发翻转，不挡 drift 硬切。 |
| 过度工程 C / cap | 为当前 LLM 噪声做重 workaround | Forward-looking：6 个月后分类稳定性提升；C 保持 bound+observe，cap 仅兜底。 |
| 修了 substrate 但 0-keep 仍在 | 若 5→0 是 propose-quality 真回归（R-tier1 假设 a），substrate 干净后 autoloop 仍 0 keep | §11 解锁判据把"substrate 干净"与"0-keep 归因"解耦：substrate 修复让 0-keep **可诊断**，是否真 propose 回归留给 M-Auto-3 proposer scope。 |

---

## 10. Observability implications（与既有观测 R-item 的配套）

- **`R-overnight-eval-traces-not-persisted`（载重前置）**：要在 substrate 修复后判断 fitness 是否变干净、
  要做 §5.6 overnight 候选人 trace review，**必须先持久化 per-iter eval trace**。建议把它与 Module A
  同窗口或先行 —— 否则修了 substrate 也看不到"路径收敛"的证据。
- **A1 trace 标注**：去重回挡须在 trace 写出 `deduplicated: true` + `original_at_step`，否则下游（report.html /
  admin trace）会看到"工具只调了一次"而无法解释 LLM 行为。与 user memory `project_observability_debt_pattern`
  一致——预期 trace 显示面会滞后，需同步补。
- **B1/B3 reason 诚实化**：escalation_reason 变诚实后，case_spec 的 `expected_outcome` 校验与 report 的
  escalation 维度才有意义；`R-tier1-bad-cases-regression-5-to-0-attribution-unverified` 的诊断依赖此。
- **A1/A3 度量**：储存 per-turn 去重命中数 + per-turn search_knowledge 调用数分布，作为 §11 解锁判据的可观测量。
- **C alternate-candidate**：若投射 alternate-candidate 软信号，需在 admin trace 显示 classify confidence +
  alternates，供 §5.6 人工判断。

---

## 11. Autoloop 解锁判据（user 核心问题 #5：修完哪个最小子集，fitness 信号就干净到能产首个 cherry-pick）

**最小解锁子集 = Module A（A1+A2+A3）+ Module B1**；Module D 为低成本并行赢。Module C + B3 **不是**首个
cherry-pick 的前置。

**可观测验证条件**（在 sim_temp=0 + bot_temp=0 + 60s deadline + parallel=1 下重跑 bad_cases 3-pass）：

1. **路径纪律收敛（Module A 生效）**：
   - `IDENTICAL_RETRY` 从 **15/24 → ≤2/24**；
   - `PARAPHRASE_STORM` 从 **11/24 → ≤3/24**；
   - `GATING_RACE` 从 **4 → ≤1**。
2. **每-case turn 数收敛**：8 flipping cases 的 3-pass `case_passed` range 从 **2 → ≤1**；per-case turn 数
   的 PASS-path vs FAIL-path 发散显著缩小（这是 bad_cases 不再坐在判别边界上的直接信号）。
3. **escalation 诚实（Module B1 生效）**：`ESCALATION_MISSTAMP`（max-steps 误盖 faq）从 **5/24 → ≤1/24**；
   max-steps 退出时若本轮有 hits 则吐 `turn_budget_exhausted`，无 viable hits 才 faq。
4. **跨 suite 印证**：anchor_outcome 的 max-steps escalation 率从 **50% 显著下降**；shadow / anchor 的 STORM 率
   同步降。
5. **turn0 flake 清零（Module D）**：`CONTRACT_VIOL_TURN0` 从 **3/24 → 0/24**。

**解锁的含义**：上述满足后，`tier1_bad_cases_regression_5_to_0` gate 测量的将是 propose-quality 而非 storm
噪声 —— baseline 稳定，kept-slate 非空成为可能；若仍 0-keep，则（依赖 `R-overnight-eval-traces-not-persisted`
持久化的 trace）`R-tier1-...-attribution-unverified` 的 (a)/(b)/(c) 归因变得**可诊断**，把"是否真 propose
回归"干净地交给 M-Auto-3 proposer scope。**至此 autoloop 才第一次拥有可信的 first-cherry-pick 判据。**

---

## 12. 给 deliver-agent 的 scope split 建议（大模块 + 顺序；最终拆分由 deliver-agent 定）

| 建议模块 | 主层 | 关键组件 | 依赖 | Codex 触发 |
|---|---|---|---|---|
| **A 工具调用纪律** | infra + prompt_projection + semantic_planner | A1 去重(R1) / A2 skill-classify-first(R3) / A3 grounding+proj(R2) | 无（载重起点）；建议与 `R-overnight-eval-traces-not-persisted` 同窗 | A1 milestone-shared（infra 幂等）；A2/A3 skill 软字段 semantic-touching → §7 stanza REQUIRED |
| **B Escalation 语义一致性** | infra + eval_spec + human_review_required | B1 resolver evidence-aware(R4) / B2 clarification 复活(RC4) / B3 R5+RC3 | **A 之后**；B3 紧随 B1 | **B1 per-sub-sprint REQUIRED**（Tier-0 candidate, §4.3 #1）；B3 含 RC3 human_review_required |
| **C Classifier bounding** | semantic_planner + prompt_projection | C1 sticky + alternate-candidate 投射(R7) | 低优先；可并行；**非解锁前置** | semantic-touching → §7 stanza REQUIRED |
| **D Eval-harness 鲁棒性** | eval_spec / harness | D1 turn0 seed + parse-retry(R6) | 独立并行 | §7 **EXEMPT**（harness carve-out） |

**推荐里程碑节奏**（建议，非决定）：M-Auto-3 取 **A + B1 + D** 作为"substrate-hygiene / 解锁
first-cherry-pick"里程碑（直接对应 §11 解锁子集）；B3 + C 作为紧随的"escalation eval 配套 + classifier
bounding"里程碑。A2/A3/C 的 skill-layer 修复由人工 dev 正常交付（**不靠 autoloop 自动产出**，不拓宽 mutable
surface）。

---

## 附:本方案引用的代码锚点（HEAD b351648 验证）

- `AgentRunLoopImpl.java`：外层 loop :179；内层 dispatch loop :316；dispatch :449（无去重）；already_called
  observation-only :182-187；classify-commit return :520-535；max_steps 出口 :554。
- `PhaseEvaluator.java`：`resolveMaxStepsReason` :173-196（faq 误盖 :183-193，只读 toolName）；MAX_STEPS case :594-610。
- `ContextProjectionBuilder.java`：already_called slot :857；`buildAlreadyCalledNode` :1235-1251；
  `canonicalArgumentsHash` :1267（去重 key 已实现）。
- `ToolDispatcher.java`：dispatch 门控 :97-103；`validateAgainstPlan` :183-195；canonical 23-enum :35-58。
- `ToolPolicyEnforcer.java`：`isToolAllowed` :65-75。
- `ClassifyUseCaseTool.java`：盖写 :136；`shouldPreserveStrongPrior` :121,188-206。
- `EscalationReasonResolver.java`：`resolve` 优先级合并 :232-247；faq=41/turn_budget=42 :110-112；
  TERMINAL_CLOSE :117-121。
- （引自 `discover_overclarify_and_search_catch22.md`，HEAD 仍在）`ControlKernel.java`：budget forceEscalate
  :279-298；`mapBudgetToEscalationReason` :657-672；`inferFallbackUseCase` 关键词 UC :1462-1474；
  `applyMissingUseCaseFallback` :1230-1246；`trackRepeatedAction` :1477-1484。
