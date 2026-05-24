---
title: DISCOVER 过度澄清不 commit + search_knowledge 在 DISCOVER 的 catch-22 + 预算强制升级时的关键词 fallback-UC 盖戳 — 多层根因 + 提升评估
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-05-24
mode: bad-case-driven
---

## 0. 阅读指引（给 deliver-agent）

本文是 **Path 2（bad-case-driven）研究产出**。输入是 human 在 admin trace
（`http://localhost:5173/admin`）观察到的真实 session
`30710fb6-f3b3-4bb4-96d4-900e18265063`（模型 `deepseek-v4-flash`，7 turn 全程
`DISCOVER → DISCOVER`，最后 `turn_budget_exhausted` 升级，UC-A 盖在升级那一 turn）。
human 的字面问题是"为什么 trace 里几乎所有 `tool_calls` 都是空的、唯一一条
`search_knowledge` 还报错"，深层问题是"为什么这个 agent 7 轮什么都没解决"。

**一句话结论**：那些空 `tool_calls` **不是显示 bug**，trace 是诚实的——LLM 每轮都
真的返回了 `"tool_calls": []`，因为它在 DISCOVER 里一直**只问澄清问题、从不调
`classify_use_case` 提交 use case**。唯一那条 `search_knowledge` 报错，是因为
**discover_triage skill 指示"先搜再分类"，但 tool-policy 把 `search_knowledge`
拦在"必须先有 committed UC"之后——skill 自己的指令在 DISCOVER 不可满足（catch-22）**。
第 7 turn 的升级是**运行时预算强制升级**（在调 LLM 之前），UC-A 是
`inferFallbackUseCase` 的**纯关键词正则**（`"a car ad"` 命中 `\b(ad|...)\b`）盖的、
confidence 0.30，只为满足 trace contract。这是一个**语义里程碑**的多层 bad case，
**不属于当前 M5（observability-only，明令不改 bot 行为）**。

| Mode-2 强制产出 | 本文位置 |
|---|---|
| ① 多层根因（code-grounded，HEAD `9ef9d1e` + 未提交 S2 工作树验证） | §1 + §2 + §3 |
| ② Coverage check（对 `action_bank.md` R-items + `milestone_objective.md` M5 scope） | §4 |
| ③ Compounding-effect + 顺序约束 | §8 |
| ④ Deliver-consumable proposal（§3.2 layer + sub-sprint + §7 stanza + hard fences） | §5–§7 + §9 |

> **HEAD / 工作树说明**：当前 HEAD = `9ef9d1e`（M5 S1 close）。工作树有未提交的 S2
> （Sprint 51 per-invocation trace）改动（`AgentRunLoopImpl` 的 `LlmCallRecord` /
> `BotTurnLlmCall` / `TraceWriter` 等）。**本文的根因代码（ControlKernel 预算/
> forceEscalate/inferFallbackUseCase、ToolDispatcher 网关、tool-policy.yaml、
> discover_triage.yaml、BudgetChecker、PhaseEvaluator）全部是已提交代码，独立于 S2**；
> 仅 §2.1"trace 诚实"一节引用的 `LlmCallRecord` 行号属 S2 工作树改动。所有行号按工作树
> 现状（即 admin trace 当前实际渲染所依据的代码）。

---

## 1. 关键架构事实（承重，四个根因共用）

### 1.1 DISCOVER 走 AgentRunLoop（skill）路径，不是 legacy 路径

`ControlKernel.processMessage` 按 feature-flag 路由（`ControlKernel.java:345-356`）：
flag 启用该 phase 且 `phaseEvaluator.plan()!=null` 时走 **AgentRunLoop**
（`agentRunLoop.run(plan, ...)`，`:356`），否则回退 legacy。`PhaseEvaluator.plan()`
对 DISCOVER 选中 `discover_triage` skill 并 compose 成 `PhasePlan`
（`PhaseEvaluator.java:351,363-377`）。**本 session 的 trace 形态（每 turn `step 0`
的 per-invocation 记录 + discover_triage 的 `max_tool_steps: 2`）证明它走的是
AgentRunLoop 路径。** legacy 的 `PhaseEvaluator.evaluateDiscover`
（`PhaseEvaluator.java:844-877`）**未被执行**——这一点是 §2 RC4 的命门。

### 1.2 三个预算（`control-policy.yaml`）

```yaml
max-clarification-rounds: 2     # :2
max-repeated-same-action: 2     # :6
max-total-bot-turns: 25         # :7
```

`BudgetChecker.checkBudgets` 顺序检查 clarification（`:32-37`）→ faq-miss →
per-path turn（仅 `activeUc!=null`，`:48-65`）→ repeated-action（`:68-73`）→
total（`:76-81`）。**本 session 全程 `activeUseCase==null`，per-path 桶不参与。**

### 1.3 `search_knowledge` 的 use-case 网关

dispatch 前用 `session.getActiveUseCase()` 查 tool-policy
（`ToolDispatcher.java:86,98-103`），不过则返回
`"Tool '%s' is not allowed for use case '%s'"`（`:100-102`）。
`tool-policy.yaml`：
```yaml
search_knowledge:  { allowed-ucs: [UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP] }  # :2-4
classify_use_case: { allowed-ucs: [ALL] }   # :21-23 —— 注释明说"by definition the call happens before activeUseCase is set"
```
**关键不对称**：`classify_use_case` 是 `[ALL]`（设计上允许在 UC 未定时调用），
**但 `search_knowledge` 不是**——它在 DISCOVER（`activeUseCase=='none'`）必被拒。

### 1.4 discover_triage skill 指示"先搜再分类"

`discover_triage.yaml`：`tools_required: [search_knowledge, classify_use_case]`
（`:7-9`）；procedure 明确写"call `search_knowledge` with the user's question as
the query, **then** call `classify_use_case`"（`:20`）；critical_step
`faq-uc-search-before-commit`（`:34-54`，`mandatory_for: [UC-A..UC-FP]`，
`severity: advisory`）的 `trace_check` 是
`tool_event_seq(search_knowledge) < tool_event_seq(classify_use_case)`。
**即 skill 把"search 在 classify 之前"作为推荐程序——而 §1.3 的网关让这在 DISCOVER
不可满足。**

---

## 2. 多层根因（code-grounded，逐 turn 对照 trace）

trace 实况：T1「can't find ad」/ T2「how to post the ad」/ T3「yes」（→
search_knowledge **报错**）/ T4「I post yesterday and can't find it...」/ T5「cars」/
T6「find a car advert」全部 `DISCOVER→DISCOVER`、`tool_calls: []`；T7「a car ad i
posted yesterday」→ `DISCOVER→ESCALATE`、UC-A、`turn_budget_exhausted`、**No LLM
call**。

### 2.1 RC1 —— LLM 在 DISCOVER 死活不 commit（主因，`semantic_planner`）

空 `tool_calls` 是 LLM 真实输出，不是显示问题：AgentRunLoop 把每步 LLM 返回的
`calls` 原样落记录（`AgentRunLoopImpl.java:283-286`，S2 工作树），且
`calls==null||isEmpty()` 时按"澄清/最终回复"处理（`:301-311`）。7 轮里 LLM
**一次都没发 `classify_use_case`**——而提交 UC 的唯一正路就是它
（`:520-535`：classify 成功提交非空 `activeUseCase` → 返回 `USE_CASE_IDENTIFIED`
→ ControlKernel 同 turn replan 进 RESOLVE，`ControlKernel.java:370-389`）。

skill 的 confidence 规则写得很明确（`discover_triage.yaml:20`）：「≥0.5 即 commit；
<0.5 才问**一个**澄清问题；only escalate ... if you cannot disambiguate **after one
clarifying turn**」。但 LLM 在已有明确信号时仍不提交：T2「how to post the ad」是清晰
UC-B，LLM 只回「Are you asking how to create a new ad?」而不 commit；T5→T6
「cars」→「find a car advert」已足够 commit（UC-A/UC-B），LLM 仍在追问。这是 §1.3
LLM-owned 的 "use case hypothesis / next action" 决策失败，**不是 projection 缺槽**
（candidate_use_cases / discover_disambiguation_signals / alternate_candidate_use_cases
都在投影里）。

> **前瞻视角（§role forward-looking）**：`deepseek-v4-flash` 是 flash 小模型，决断性弱、
> 倾向过度澄清。**不应**为它的犹豫加 Java guard 强制 commit（那会把 §1.3 的 UC 决策从
> LLM 夺走，撞 §1.5/§1.7）。修复方向是 prompt_projection 让 commit 规则更显著 + 让系统
> 在过度澄清时**优雅降级**（见 RC4），并相信 6 个月后的主流模型决断性更强。

### 2.2 RC2 —— search_knowledge 在 DISCOVER 的 catch-22（`prompt_projection` / config-governance）

T3 LLM 照 skill 指令「先搜」，发 `search_knowledge(uc_tags:[UC-B])`，被 §1.3 网关拒：
`"Tool 'search_knowledge' is not allowed for use case 'none'"`
（`ToolDispatcher.java:100-102`，因 `activeUseCase==null`）。
**这是 skill 指令（§1.4 先搜再分类）与 tool-policy（§1.3 搜需先有 UC）的直接矛盾**：
LLM 越听话越撞错。按 tool-policy，DISCOVER 的"正路"其实是**先 classify 再搜**
（classify 是 `[ALL]`），但 skill 文本说反了。这不是语义硬编码、也不是 LLM 的错，
是两份 config 自相矛盾——纯 config-governance 缺陷，且很可能加剧了 RC1
（LLM 试图 grounding 却被挡，更没底气 commit）。

### 2.3 RC3 —— 第 7 turn 是预算强制升级 + UC-A 是关键词正则盖的（`infra`/`java_guard` + §1.7 反硬编码关注点）

T7「No LLM call for this turn」，因为预算检查在调 LLM **之前**
（`ControlKernel.java:279`）。升级文案「I've reached the limit of what I can assist
with on this topic. Let me connect you with a human agent who can help further.」
与 `ControlKernel.java:296-298` 的预算 forceEscalate 文案**逐字一致**，确认是预算路径
（**区别于** MAX_STEPS 路径的「I'm having difficulty resolving this. Let me connect
you with a specialist.」`PhaseEvaluator.java:608`——后者是另一份提案
`escalation_reason_runtime_evidence_contract_maxsteps_misstamp.md` 的对象，见 §4）。

reason `turn_budget_exhausted` 由 `mapBudgetToEscalationReason`
（`ControlKernel.java:657-672`）产生——它是非 clarification / 非 faq-miss 桶的 catch-all。
7 turn、无 UC，per-path（15/10）与 total（25）都不可能触发 → **最可能是
`max-repeated-same-action`（cap=2）**（`trackRepeatedAction`，`ControlKernel.java:1477-1484`：
连续相同 `lastAction` 才自增）。**确切桶可查服务端日志 `budget '<bucket>' exceeded,
forcing ESCALATE`（`ControlKernel.java:281`）。**

UC-A 来自 `applyMissingUseCaseFallback`（`ControlKernel.java:1230-1246`）→
`inferFallbackUseCase`：把 `"a car ad i posted yesterday"` 用正则
`\b(ad|ads|advert|adverts|listing|listings|posting|post)\b` 命中 → 返回 `"UC-A"`
（`:1471-1472`），盖上 `intentConfidence=0.30`（`:1241`）。注释自承这是为满足 trace
contract validator（要求 `active_use_case` 非空，`:1207-1217`）。**所以 trace 上那个
"UC-A" 不是真分类，是关键词创可贴。** 这正是 §1.7 警告的 semantic hardcode 被当成
"凑合过 contract"用——与 human 记忆里「FAQ over-escalate = MAX_STEPS mis-stamp」
（`project_faq_overescalate_maxsteps_misstamp`）同族：**运行时强制收尾时盖一个 LLM
从没选过的 UC/reason**。

### 2.4 RC4 —— clarification 预算在 skill 路径上形同虚设（`infra`/`skill_state`）

`clarificationCount` 的**唯一**自增点是 `PhaseEvaluator.evaluateDiscover:865`
（`if (isClarificationTurn(action)) session.setClarificationCount(+1)`）。但 §1.1 已证
DISCOVER 走 **AgentRunLoop 路径**，`evaluateDiscover` 是 **legacy 未执行路径**。
→ `clarificationCount` 全程停 0 → `BudgetChecker.java:32`（`>= maxClarificationRounds(2)`）
**永不触发**。**这解释了为什么 5+ 轮澄清没在第 3 轮就以 `clarification_budget_exhausted`
干净收场，而是一路跑到 repeated-action 桶、被 `turn_budget_exhausted` + 关键词 UC 盖戳收尾。**
本应最贴切、reason 最诚实的 clarification 预算，在 live 路径上是死代码。

---

## 3. 多层根因小结（Mode-2 产出①）

| 层（§3.2） | 根因 | trace 证据 | code（工作树 / HEAD `9ef9d1e`） |
|---|---|---|---|
| **`semantic_planner`（主，RC1）** | DISCOVER 里 LLM 只澄清、从不 `classify_use_case`，即使信号已清晰 | T1-T6 全 `tool_calls:[]`；T2/T5/T6 信号足却不 commit | `AgentRunLoopImpl.java:301-311,520-535`；`discover_triage.yaml:20`（commit 规则）；`PhaseEvaluator.java:363-377` |
| **`prompt_projection` / config-governance（RC2）** | skill 指令"先搜再分类"与 tool-policy"搜需先有 UC"矛盾，DISCOVER 不可满足 | T3 `search_knowledge` → `not allowed for use case 'none'` | `discover_triage.yaml:20,34-54` ↔ `tool-policy.yaml:2-4,21-23`；`ToolDispatcher.java:98-102` |
| **`infra`/`java_guard` + §1.7（RC3）** | 预算 top-of-turn 强制升级（无 LLM call）；UC-A 由关键词正则盖、conf 0.30，仅为过 contract | T7 No LLM call、`turn_budget_exhausted`、UC-A | `ControlKernel.java:279-298,657-672,1230-1246,1462-1474,1477-1484` |
| **`infra`/`skill_state`（RC4）** | clarification 预算自增点在 legacy `evaluateDiscover`，live AgentRunLoop 路径不自增 → cap=2 永不触发 | 5+ 澄清轮未触发 `clarification_budget_exhausted` | `PhaseEvaluator.java:844-877`（尤 `:865`）vs `ControlKernel.java:345-356`（live 路径） |

---

## 4. Coverage check（Mode-2 产出②）

| 现有项 | 关系 | 本次结论 |
|---|---|---|
| `docs/solutions/escalation_reason_runtime_evidence_contract_maxsteps_misstamp.md`（已有 research 提案，2026-05-24） | **相邻、不重叠** | 那篇是 **FAQ/RESOLVE 路径 MAX_STEPS → `faq_miss_threshold_exceeded` 误盖**（`resolveMaxStepsReason` 不看 faq_miss 证据）。本案是 **DISCOVER 路径 + 预算 top-of-turn 强制升级 → `turn_budget_exhausted`**，文案/路径/触发都不同（§2.3）。**共享** `inferFallbackUseCase` 关键词盖戳（两条升级路径都经 `applyMissingUseCaseFallback`）与"运行时盖 LLM 没选的东西"家族。 |
| `R-escalation-reason-runtime-evidence-contract-review`（`action_bank.md:390`，Tier-0 candidate） | **相邻** | 它管 evidence-claiming reasons（`faq_miss_threshold_exceeded` / `clarification_budget_exhausted` / `intake_complete_*`），**明确排除** `turn_budget_exhausted`（catch-all）。本案 T7 的 `turn_budget_exhausted` 其实是**诚实的兜底**（真耗尽了某预算）；问题不在 reason，在 RC3 的 **UC 关键词盖戳** + RC4 的 **错预算桶**。 |
| `D-S3-no-prior-search-guard`（`action_bank.md:339`，deferred） | **反方向** | 它是"无前置 search 不许 `request_handover(faq_miss_threshold_exceeded)`"。本案 RC2 恰相反——是"DISCOVER 里**根本搜不了**"。不重叠。 |
| `R-runtime-orchestrator-tool-call-deduplication`（`action_bank.md:426`，partial→semantic_planner） | 正交 | 那是 RESOLVE 内重复 search 的 dedup；本案 DISCOVER 不涉及 dedup。 |
| `D-new-escalation-reason-enum`（`action_bank.md:347`，deferred/avoid） | **硬约束** | 修复**不得新增/重命名** enum 值（cross-cut eval `ESCALATION_TRIGGER_VALUES`）。本案不需要新 enum。 |
| M5（`milestone_objective.md`，Observability Coherence） | **明确排除（scope 边界）** | M5 §4 non-goals "**Not changing bot behaviour**"、"Not refactoring `ControlKernel`/`PhaseEvaluator` phase machine"。本案 RC1-RC4 全是 **bot 行为/运行时**修复 → **不属 M5**，进 M5 之后的语义里程碑。 |

**缺口（本研究新识别，需登记 R-item，见 §9）**：现有 backlog **没有任何项**覆盖
(a) **DISCOVER 过度澄清不 commit**（RC1）；(b) **search_knowledge 在 DISCOVER 的
catch-22**（RC2，skill↔tool-policy 矛盾）；(c) **clarification 预算在 AgentRunLoop
路径上死代码**（RC4）。RC3 的 UC 关键词盖戳是 `R-escalation-reason-runtime-evidence-
contract-review` 的**邻居但不同对象**（那个管 reason，本案管 UC）。

---

## 5. 设计方案 + trade-off

> 下列均属 **M5 之后的语义里程碑**。研究 agent 给备选，deliver-agent 做最终拆分。

### 5.A RC2 catch-22 解矛盾（config-governance，最低风险，建议先做）

- **A1（推荐）—— skill 改为"分类优先、搜在 RESOLVE"**：把 `discover_triage.yaml:20`
  的"先 search 再 classify"改为"DISCOVER 的职责是**提交 UC**；grounding 检索在
  RESOLVE 进行"，并把 advisory critical_step `faq-uc-search-before-commit`（`:34-54`）
  降级/移除（它要求的 search-before-classify 顺序在 DISCOVER 不可达）。
  - 优点：与 tool-policy 现状一致（classify=`[ALL]`、search 需 UC）；零运行时代码改动；
    直接消除 LLM 撞错。缺点：放弃"分类前先用检索 disambiguate"的设想（但该设想本就被网关
    挡死、从未生效）。
- **A2 —— tool-policy 给 DISCOVER 放行 search_knowledge**：在 policy 里允许
  `activeUseCase==null`（DISCOVER）调 `search_knowledge`。
  - 优点：保留"先搜后分类"的 skill 设想。缺点：扩大 DISCOVER 的工具面、需重核
    `ToolPolicyEnforcer` 语义；search 结果如何在无 UC 时 grounding 是新问题；风险高于 A1。
- **取舍**：A1（让 skill 说真话）比 A2（改网关迁就 skill）更 contained、更符合现状设计意图。

### 5.B RC4 让 clarification 预算在 live 路径生效（infra/skill_state，低风险）

- **B1（推荐）—— 在 AgentRunLoop 路径补 clarification 计数**：当 DISCOVER turn 的
  AgentRunResult 是 `CLARIFICATION_NEEDED`（`AgentRunLoopImpl.java:305-308`）时自增
  `clarificationCount`（移到 ControlKernel 的 interpret/transition 后处理，或让
  `BudgetChecker` 也认 AgentRunLoop 的澄清信号）。这样 cap=2 真生效、过度澄清以**诚实的
  `clarification_budget_exhausted`** 干净收场，而非跑到 repeated-action + 关键词盖戳。
  - 优点：让最贴切的预算复活；reason 变诚实；纯 infra、无语义硬编码。缺点：需确认
    `isClarificationTurn` 的 AgentRunLoop 等价判定一致（`AgentRunLoopImpl.isClarificationMessage`
    `:659-669` 已是同一启发式，可复用）。

### 5.C RC1 降低过度澄清（`semantic_planner` via `prompt_projection`，高风险，需 real-LLM 重跑）

- **C1（推荐）—— 把 commit 规则从长 procedure 里提到显著位置 + 投 clarification 计数软信号**：
  discover_triage 的 procedure 现已 ~7900 字符（`action_bank.md:345` Sprint 40 记录），
  "一次澄清后必须 commit 或 escalate"被埋没。把它提为 procedure 顶部的硬性一句，并投一个
  **soft signal**（如 `clarifications_asked_this_session: N` + "you have asked N
  questions; commit a use case or escalate"）让 LLM 看到自己已问太多。
  - 优点：LLM-first、无 Java guard、registry/Skill-driven。缺点：触语义投影面，**必须
    real-LLM bad-case 重跑**；依赖 RC4 的软信号管道（与 M5 S3 Skill-driven 投影收敛同向，
    建议在 M5 S3 之后做，复用其投影机制）。
- **C2 —— 不单独硬编码 commit**：§1.3 的 UC 决策归 LLM；不加"问够 N 次就强制 classify"
  的 Java 分支（撞 §1.7）。RC4 的预算是兜底，C1 的投影是 enable，二者足够。

### 5.D RC3 让 fallback-UC 诚实（`human_review_required` → infra；建议最后做/降级）

- **D1 —— 用运行时已有的 intent 证据替代裸关键词正则**：`inferFallbackUseCase`
  （`ControlKernel.java:1462-1474`）目前是 `\b(ad|...)\b` 裸正则。在 RC1/RC4 落地后，
  绝大多数 session 会有真 classify 或真 clarification 收尾，fallback 退化为罕见路径；
  此时把它的盖戳改为**诚实低置信**（已是 0.30）+ 在 trace 上**显式标注"runtime
  keyword fallback, not LLM-classified"**，避免下游把假 UC 当真。是否**保留/移除**关键词
  正则本身需 `human_review_required`（与 `R-escalation-reason-runtime-evidence-contract-
  review` 同属"运行时盖 LLM 没选的东西"的 Tier-0 讨论）。
  - 优点：trace 诚实；不在 RC1 之前动正则（见 §8 错序风险）。缺点：需 human 决策是否
    动 trace-contract 的 `active_use_case` 非空要求。

---

## 6. 推荐 + 理由（Deliver-consumable proposal 主体，Mode-2 产出④）

**推荐组合**：先 **A1（解 catch-22）+ B1（复活 clarification 预算）**（config+infra，
低风险，可独立于 M5、甚至并行）→ 待 M5 S3 落地后做 **C1（commit 规则显著化 + 软信号，
prompt_projection）**→ 最后 **D1（fallback-UC 诚实化 + trace 标注，须 human Tier-0
讨论）**。理由：

- **A1+B1 是"让系统说真话"的载重修复**，零/极低语义风险：A1 消除 skill 自相矛盾、
  B1 让过度澄清以诚实 reason 收场。二者**直接改善 human 观察到的症状**（不再有莫名
  `search_knowledge` 报错；不再有 `turn_budget_exhausted` + 假 UC-A 收尾）。
- **C1 才真正降低过度澄清**（让 LLM 早 commit），但触语义投影面、依赖 real-LLM 证据门
  + M5 S3 的 Skill-driven 投影机制，故排在后。
- **D1 最后做**：在 RC1/RC4 让真分类可靠后，fallback 退化为罕见兜底，此时再讨论关键词
  正则去留最安全（§8 错序）。

### 6.1 Sub-sprint 建议（deliver-agent 做最终拆分）

| 建议 sub-sprint | 层（§3.2） | scope（3 句） | 依赖 | Codex |
|---|---|---|---|---|
| **SS-1：DISCOVER search catch-22 + clarification 预算复活（A1+B1）** | `prompt_projection`/config-governance + `infra`/`skill_state` | 改 `discover_triage.yaml` 为分类优先、降级 `faq-uc-search-before-commit` advisory；在 AgentRunLoop 路径补 `clarificationCount` 自增让 cap=2 生效。无语义硬编码；real-LLM 重跑确认 catch-22 消失 + 过度澄清以 `clarification_budget_exhausted` 收场。覆盖 target/neighbor/negative/shadow。 | 无（独立于 M5；A1 部分甚至可与 M5 并行） | milestone-shared（A1 改 skill prose + B1 infra；无 Tier-0、无 §1.7 cross） |
| **SS-2：DISCOVER commit 规则显著化 + clarification 软信号（C1）** | `prompt_projection`（§3.2 Q3） | 把"一次澄清后 commit 或 escalate"提到 procedure 顶部；投 `clarifications_asked_this_session` soft signal。registry/Skill-driven，无 per-UC if-else。real-LLM bad-case 重跑为证据门。 | **M5 S3（Skill-driven 投影 + C1 消费图）** + SS-1（B1 的计数） | milestone-shared（除非触发 per-sub trigger） |
| **SS-3：fallback-UC 诚实化 + trace 标注（D1）** | `human_review_required` → `infra` | 在 RC1/RC4 落地后，把 `inferFallbackUseCase` 盖戳在 trace 上显式标注"keyword fallback, not LLM-classified"；是否移除裸正则须 human Tier-0 决策（与 `R-escalation-reason-...` 同议）。 | **SS-1 + SS-2**（先让真分类可靠） | **per-sub-sprint REQUIRED**（§4.3 触发 #1 Tier-0-candidate-adjacent / §1.7） |

---

## 7. §7 stanza 预填（多层 prospective；per-sub-sprint）

### SS-1 —— catch-22 + clarification 预算复活

```markdown
## Layer-classification + anti-hardcode stanza
**Target failure layer:** prompt_projection / config-governance（discover_triage.yaml ↔
tool-policy.yaml 矛盾，§3.2 Q3）+ infra / skill_state（clarificationCount 自增点错路径）。

**Tier-0 invariant:** 本 sub-sprint 不新增 Tier-0。tool-policy 的能力边界是 §1.4
Runtime-owned，本 sub-sprint 让 skill 指令与既有边界**一致**（不放松边界）；clarification
预算是既有控制面，仅修其在 live 路径的生效。

**Semantic hardcode:** 不引入。A1 是 skill 自然语言指令对齐（让 skill 说真话）；B1 复用
既有 `isClarificationTurn` 启发式（`AgentRunLoopImpl:659-669`）补计数，非新 keyword/regex/
if-else 语义分支。不动 `escalation_reason` enum（复用既有 `clarification_budget_exhausted`）。

**Generalization coverage:** target = 本 session 30710fb6 的 DISCOVER 过度澄清 + T3 search
报错；neighbor = 其它 UC（UC-A/C/D/F）在 DISCOVER 的先搜被拒 + 多轮澄清；negative = 单轮即
可 commit 的清晰 intake **仍**应立即 classify（不被新计数误升级）；shadow = held-out DISCOVER
多轮澄清 trace。real-LLM 重跑 + bad-case suite 人工判定为准。
```

### SS-2 —— commit 规则显著化 + clarification 软信号

```markdown
## Layer-classification + anti-hardcode stanza
**Target failure layer:** prompt_projection（discover_triage procedure 显著化 +
clarification 软信号投影，§3.2 Q3）。

**Tier-0 invariant:** 不新增 Tier-0。投影是 §1.4 Runtime "trace/eval contract" 的一部分，
直接喂 §1.3 LLM 的 UC-commit 决策；本 sub-sprint 改的是**显著性/软信号**，不改 LLM 拥有的
决策权，也不强制 commit。

**Semantic hardcode:** 不引入。把既有 commit 规则提前是 prose 重排；`clarifications_asked_
this_session` 是 soft signal（观察值投影），**禁止** "问够 N 次就 Java 强制 classify" 的硬分支
（撞 §1.7）。registry/Skill-driven，承接 M5 S3。

**Generalization coverage:** target = DISCOVER 过度澄清→早 commit；neighbor = 其它 FAQ/intake
UC 的 DISCOVER commit 决断；negative = 真正模糊（如 T1「can't find ad」UC-A vs UC-H）**仍**应
问一次澄清，不被软信号逼成误 commit；shadow = held-out 多轮澄清 trace。**real-LLM 重跑为准**
（mocked-LLM 仅覆盖投影 wiring，§5.6 eval evidence gate）。
```

### SS-3 —— fallback-UC 诚实化

```markdown
## Layer-classification + anti-hardcode stanza
**Target failure layer:** human_review_required → infra（inferFallbackUseCase 关键词盖戳的
去留 + trace 诚实标注；§3.2 Q2 "看似 java_guard 但无现行 Tier-0 覆盖"）。

**Tier-0 invariant:** Tier-0 **candidate**，须 human 决策（与 `R-escalation-reason-runtime-
evidence-contract-review` 同议）：运行时在强制升级时盖出的 `active_use_case` 是否必须标注为
"非 LLM 分类"、裸关键词正则是否保留。若 human 不开 Tier-0，则降级为 trace-quality fix。

**Semantic hardcode:** 本 sub-sprint **减少**而非新增硬编码——目标是让既有
`inferFallbackUseCase` 关键词正则（`ControlKernel.java:1462-1474`）的产物在 trace 上诚实。
不得在此引入新的 per-case/per-UC 关键词。

**Generalization coverage:** target = 强制升级时 fallback-UC 在 trace 上被误当真分类；
neighbor = MAX_STEPS 路径同一 `applyMissingUseCaseFallback` 盖戳（与另一提案协同）；
negative = 真有 LLM classify 的 session **不**被标注为 fallback；shadow = held-out 强制升级 trace。
```

---

## 8. Compounding-effect + 顺序约束（Mode-2 产出③）

**复利 1（最重要）—— A1+B1 必须先于 D1**：
- 若先做 **D1（动 fallback-UC 关键词/诚实化）而不先做 A1+B1+C1**，则当 LLM 仍过度澄清、
  仍被强制升级时，fallback 要么盖出**另一个**错 UC、要么（若移除正则）让 `active_use_case`
  为空触发 **trace `CONTRACT_VIOLATION`**——**症状被挪、根因未除**（正是 research agent
  "只修症状不修原因"的禁区）。
- 正确序：A1+B1（让系统说真话、过度澄清优雅收场）→ C1（真减少过度澄清）→ D1（此时
  fallback 已退化为罕见兜底，再讨论正则去留最安全）。

**复利 2 —— C1 依赖 M5 S3，不可错序**：C1 投 `clarifications_asked` 软信号、改投影显著性，
**依赖 M5 S3 的 Skill-driven 投影收敛 + C1 消费图**。若 C1 先于 M5 S3：(a) 把语义投影改动
塞进尚未收敛的投影面，与 M5 S3 "no projection field changed before C1 map"
（`milestone_objective.md` §6）冲突；(b) 可能触发新的 eval trace `CONTRACT_VIOLATION`
（投影契约校验器对字段集敏感）。**故 C1 排在 M5 S3 之后，复用其投影机制。**

**复利 3 —— A1 与 B1 同 sub-sprint、互为前提**：只做 A1（解 catch-22）不做 B1，过度澄清仍
跑到 repeated-action + 关键词盖戳（reason 仍不诚实）；只做 B1（复活预算）不做 A1，LLM 仍被
catch-22 报错干扰。二者同向、应捆在 SS-1。

**复利 4 —— RC1 是总闸，但其修复（C1）风险最高、排在低风险修复之后**：A1+B1 改善"系统行为
诚实度"立竿见影且低风险；C1 才改善"LLM 决断力"（真正的总闸）但需 real-LLM 证据 + M5 S3。
先低风险后高风险，符合分支高迭代节奏。

**复利 5 —— 与另一提案（MAX_STEPS misstamp）的协同**：本案 RC3 与
`escalation_reason_runtime_evidence_contract_maxsteps_misstamp.md` **共享**
`applyMissingUseCaseFallback` 盖戳面。SS-3 的 trace 诚实化应与那篇的 SS-A（运行时证据契约）
**协调**：两条强制升级路径（预算 vs MAX_STEPS）共用同一 fallback-UC 机制，一处改诚实标注、
另一处改 reason 证据，**应在同一 Tier-0 讨论里一并决策**，避免只修一条路径。

---

## 9. Hard fences + non-goals + 新 R-item 建议

**Hard fences**：
- **不属 M5**（M5 = observability-only、明令不改 bot 行为 + 不重构 ControlKernel/PhaseEvaluator
  phase machine）。本案 RC1-RC4 全进 M5 之后的语义里程碑。
- **§1.7**：DISCOVER 的 UC-commit 决策归 LLM。**不加** "问够 N 次就 Java 强制 classify"
  的硬分支；C1 只投软信号 + 改 prose 显著性。
- **不新增/重命名 `escalation_reason` enum 值**（`D-new-escalation-reason-enum` deferred）——
  RC4 复用既有 `clarification_budget_exhausted`，RC3 复用 `turn_budget_exhausted`。
- **A1 不放松 tool-policy 能力边界**（让 skill 对齐边界，而非反之）；若选 A2 则须重核
  `ToolPolicyEnforcer` 且单列 risk。
- **SS-3 不得在 SS-1/SS-2 之前动 `inferFallbackUseCase` 正则**（复利 1 错序风险）；移除/保留
  须 human Tier-0 决策。
- **SS-2 不得先于 M5 S3**；复用 M5 S3 的 Skill-driven 投影机制（复利 2）。
- **Negative 不许过修**：真正模糊的首轮（如 T1「can't find ad」UC-A vs UC-H）**仍**应问一次
  澄清，不被 RC1/RC4 修复逼成误 commit 或过早升级。
- **不改 LLM provider/模型/温度/deadline**；不动 `composite.py` scoring。

**Non-goals**：
- 不把 `get_moderation_review_context` 等 dormant 工具改成 AGENT_VISIBLE（独立工具决策）。
- 不重写 budget/control-policy 数值（cap=2/25 的调参是独立 config-governance，本案只修
  "预算在哪条路径生效 / reason 是否诚实"，不调数值）。
- 不解决另一提案的 MAX_STEPS→faq_miss 误盖（正交，见 §4 / §8 复利 5）。

**建议登记的新 R-item**（deliver-agent 决定）：
- **`R-discover-overclarify-no-commit`**（`semantic_planner`/`prompt_projection`）——
  DISCOVER 里 LLM 过度澄清、从不 `classify_use_case`；source: manual-probe 2026-05-24
  session `30710fb6`。
- **`R-discover-search-knowledge-catch22`**（config-governance）——discover_triage skill
  指令"先搜再分类"与 tool-policy "搜需先有 UC" 矛盾。
- **`R-clarification-budget-dead-on-agentrunloop-path`**（`infra`/`skill_state`）——
  `clarificationCount` 自增点在 legacy `evaluateDiscover`，live AgentRunLoop 路径不自增。
- （可选）**扩展** `R-escalation-reason-runtime-evidence-contract-review` 的 notes，把
  `inferFallbackUseCase` 的 **UC 关键词盖戳**列为与 reason 误盖并列的"运行时盖 LLM 没选的
  东西"子项。

---

## 10. Observability / trace 启示（与 M5 协同）

- **本案直接受益于 M5 S2（per-invocation trace）**：human 这次正是在 admin trace 上看到空
  `tool_calls` 才发现问题。S2 落地后，逐 turn 的 projected_context（含 candidate_use_cases /
  discover_disambiguation_signals）可见，**可零歧义确认 RC1**（LLM 看到了什么却仍不 commit）
  ——本研究 RC1 当前由"skill 规则 + LLM 输出"推断，建议 S2 落地后用本 session 的逐 turn
  projection 复核。
- **trace 应区分"LLM-classified UC" vs "runtime keyword fallback UC（conf 0.30）"**：当前
  二者在 trace 上无视觉区分，导致 human 把 T7 的假 UC-A 当真分类。这是 SS-3 的 trace 诚实化
  目标，也可作为 M5 S2/S4 的 trace-quality 附带项（低优先级 observability nicety）。
- **`[] vs null` 归一**：`LlmCallRecord` 层恒为 `[]`（`AgentRunLoopImpl.java:286` 兜底），
  但 BotTurn 单列/前端可能渲染成 null；M5 S2 落地时顺手归一，避免 human 误读为"有时有有时无"。

---

## 11. 给 human / deliver-agent 的一句话总结

human 看到的"trace 里 `tool_calls` 全空、唯一一条 `search_knowledge` 还报错、7 轮卡死后
`turn_budget_exhausted` 升级"，**根因是四层叠加**：(RC1) LLM 在 DISCOVER 过度澄清、从不调
`classify_use_case` 提交 UC（`semantic_planner`，空 tool_calls 是诚实记录）；(RC2)
discover_triage skill 指示"先搜再分类"与 tool-policy"搜需先有 UC"矛盾，LLM 越听话越撞错
（config-governance catch-22）；(RC3) 第 7 turn 是**运行时预算强制升级**（无 LLM call），
UC-A 是 `inferFallbackUseCase` 关键词正则盖的假分类（§1.7 创可贴）；(RC4) 最贴切的
clarification 预算在 live AgentRunLoop 路径上是死代码，所以没干净收场反而跑到关键词盖戳。
**全部不属 M5（observability-only）**，进 M5 之后的语义里程碑。建议顺序：**SS-1（解
catch-22 + 复活 clarification 预算，config+infra，低风险，可独立/并行）→ SS-2（commit 规则
显著化 + 软信号，prompt_projection，依赖 M5 S3）→ SS-3（fallback-UC 诚实化，须 human
Tier-0，最后做）**；RC3 的 fallback-UC 与另一提案（MAX_STEPS misstamp）共享盖戳面，应在同一
Tier-0 讨论里协同决策。
