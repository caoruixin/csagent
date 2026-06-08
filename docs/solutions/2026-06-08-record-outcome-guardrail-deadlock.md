---
title: RESOLVE-FAQ record_outcome guardrail deadlock — root cause + proposed fix
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-08
mode: bad-case-driven
---

# RESOLVE-FAQ record_outcome guardrail deadlock — root cause + proposed fix

## 1. Executive summary

FAQ-resolve 路径上存在一个结构性死锁：LLM 给出 grounded answer 后想结案（调 `record_outcome(resolve)`）会被 runtime guard 拒绝；而 phase 从 `RESOLVE` 推进到 `CONFIRM` 的**唯一**条件是 `record_outcome` 在当轮 dispatch 成功。结果：

- LLM 永远进不了 `CONFIRM`；`confirm.yaml` 的 `record_outcome` mandatory critical step 在生产路径上几乎从未触发。
- 用户说「谢谢」时，phase 仍是 `RESOLVE`，加载的仍是 `resolve_faq_grounded_answer.yaml`，不是 `confirm.yaml`。
- `session_outcomes` 表在 resolve 类结案上几乎从不写入；`BotSession.containmentOutcome="resolved"` 来自 runtime 的代偿（`ControlKernel.isResolvedSuccessTerminal`），不来自 LLM 调用 `record_outcome` 的真实事实。

这是 `iteration_governance.md §1.3 LLM owns "next action / response strategy"` 与 §1.4 `Runtime owns "trace contract"` 边界的破裂：runtime 拒绝了 LLM 想做的合理动作，又用 trace 代偿伪装这件事没发生。

推荐方案 **Option C — 收窄 guard（共享 ASKED_FOR_SLOT 判定）**：把
`shouldRejectPrematureResolveOutcome` 从「RESOLVE-FAQ 上一律拒绝 resolve」收窄成「只在 bot 文本被
`ResolveDispositionEvaluator.evaluate()` 判为 `ASKED_FOR_SLOT` 时拒绝」。这样 Sprint 11 §M1 的反误杀
（bot 同一轮里既问 ad-id 又 record_outcome）仍然得到保护，但 grounded 事实答案 + 同轮 record_outcome
的合法路径打通，phase 能正常 `RESOLVE → CONFIRM → CLOSE`。

**关键时序建议**：这个修复对 baseline 的 stamp 来源结构会产生轻量影响（同一份"resolved"信号从 runtime 代偿移到 LLM tool 真实写入），不应抢在 **M-Auto-7 S-Y2 autoloop pilot core gate** 之前落地——否则会污染 pilot 的对照基线。建议作为 **M-Auto-7 close 后 / M-Auto-8 候选** 的独立 1–2 sub-sprint 工作。

## 2. Current-state survey (code-grounded, HEAD verified)

### 2.1 Phase advance 规则

`PhaseEvaluator.mapFinalAnswer()` 在 RESOLVE/FAQ FINAL_ANSWER 终态下的分支
（`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:780-836`）：

```
case "RESOLVE":
    if (isIntakePlan) -> stay RESOLVE "clarification_asked"
    if (recordOutcomeAttemptedAndFailed(result)) -> stay RESOLVE "record_outcome_failed_retry"
    disposition = ResolveDispositionEvaluator.evaluate(plan, result)
    switch (disposition):
      ASKED_FOR_SLOT | ANSWERED_SUBTASK | CONTINUE_RESOLVE -> stay RESOLVE "progressive_resolve_stay"
      ESCALATE -> ESCALATE
      READY_TO_CONFIRM -> CONFIRM "answer_provided"
```

只有 `READY_TO_CONFIRM` 这一个出口能进 `CONFIRM`。

### 2.2 READY_TO_CONFIRM 的唯一兑现条件

`ResolveDispositionEvaluator.evaluate()`
（`server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java:95-141`）：

- 不是 RESOLVE-FAQ 计划 → `CONTINUE_RESOLVE`。
- 终态非 `FINAL_ANSWER` → 走 `ESCALATE` / `ASKED_FOR_SLOT` 等其它分支。
- 终态是 `FINAL_ANSWER`：
  - bot 文本以 `?` 结尾 / 命中 `CLARIFYING_QUESTION_PATTERN` / 命中 `SOFT_NEXT_STEP_PATTERN`（`:62-73`，覆盖 "send the ad id / share the ad id / would you like me to..." 等）→ `ASKED_FOR_SLOT`。
  - 否则，仅当 `recordOutcomeSucceededThisRun(result) == true` 时返回 `READY_TO_CONFIRM`（`:133`）。
  - 否则 → `ANSWERED_SUBTASK`（`:136`）。

`recordOutcomeSucceededThisRun()`（`:192-200`）只看本轮 `toolEvents` 里有没有
`record_outcome` 且 `success==true`。

### 2.3 record_outcome 在 RESOLVE-FAQ 上被 guard 拒绝

`resolve_faq_grounded_answer.yaml` line 37-39 注册了 guardrail
`premature_resolve_outcome_guard`。`SkillGuardrailDispatcher.handlePrematureResolveOutcomeGuard()`
（`server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillGuardrailDispatcher.java:322-342`）
直接委托给 `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome()`
（`server/src/main/java/com/gumtree/csagent/service/runtime/ResolveDispositionEvaluator.java:160-177`）：

```
shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass):
  if (plan == null) return false;
  if (!isResolveFaqPlan(plan)) return false;             // 只在 FAQ-RESOLVE 上
  if (!"resolve"|"resolved".equals(outcomeClass)) return false;
  if (currentPhase == "CONFIRM" | "CLOSE") return false;  // 这俩 phase 允许
  return true;                                            // 其它一律拒绝
```

进 `CONFIRM` 之前调 `record_outcome(resolve)` ⇒ 拒绝；进 `CONFIRM` 需要先成功 record_outcome
⇒ 鸡生蛋。

### 2.4 confirm.yaml 只在 phase == CONFIRM 才会装载

`confirm.yaml` line 3：`applicable_phases: [CONFIRM]`。`PhaseEvaluator.plan()`
（`PhaseEvaluator.java:366-379`）通过
`skillRegistry.select(phase, activeUc)` 选 Skill；phase 不是 CONFIRM 就选不到 confirm。
所以 confirm.yaml 里那条 `record-outcome-on-confirmed-resolve` mandatory critical step（line 32-49）
在 FAQ-resolve 实际流量上从不被走到（除非 `isResolvedSuccessTerminal` 等代偿先把流程踢到
`CONFIRM` 之外的某个路径）。

### 2.5 用户说「谢谢」走不到 confirm.yaml

`PhaseEvaluator.evaluate()`（`:329-347`）按当前 phase 派发到 evaluateConfirm / evaluateResolve …。
若 D16 plan 路径可用（`SkillRegistry`-driven，Sprint 39 NEW M2 起对所有 6 个 phase 都返回非空 plan），
ControlKernel 走 plan-aware 路径。**只要 phase 在用户说「谢谢」时仍是 RESOLVE，加载的就是
`resolve_faq_grounded_answer.yaml`，不是 confirm.yaml**。legacy `evaluateConfirm()` 里那条
`isPositiveConfirmation(lower)`（`PhaseEvaluator.java:1369, 1424-1427`，正则匹配
"yes|yeah|thanks|thank you|great|perfect|got it|all good|..."）——只有 phase 已经是 CONFIRM 才会被到达。

### 2.6 Runtime 代偿：isResolvedSuccessTerminal

`ControlKernel.java:587-654` 给出两条不需要 LLM 调用 `record_outcome` 就能把
`containmentOutcome="resolved"` 写进 session 的旁路：

- 当 `phaseAfter == "CLOSE"` 且 `containment==null` 且 `isResolvedSuccessTerminal(...)` 为真（Sprint 084 / S-Auto-29 之后，对 CLOSE 也通过此 gate 而非默认 resolved）。
- 当 `isResolvedSuccessTerminal(...)` 为真（独立 stamp arm，Sprint 074 / 075 引入）。

`isResolvedSuccessTerminal()`（`:1431-1482`）的接受条件：

- `containment == null`（不覆盖已 stamp 的 escalated）；
- `terminalOutcome == FINAL_ANSWER`；
- `resolveDisposition ∈ {READY_TO_CONFIRM, ANSWERED_SUBTASK}`；
- `articlesShown != null && length > 0`（grounded）。

实际效果：FAQ-RESOLVE 给出 grounded answer 的多数情形落在
`disposition=ANSWERED_SUBTASK` 这条分支，于是 runtime 直接把 containment 写成 "resolved"——但
`session_outcomes` 这张表上 **没有对应行**（因为 RecordOutcomeTool 从未被成功调用）。

这就是 user 描述的"trace 显示 resolved，但 record_outcome 从未成功"现象的代码出处。

### 2.7 doc-comment 与代码已经不自洽

`ResolveDisposition.java:23-26` 的 `READY_TO_CONFIRM` 文档注释写："the user explicitly accepted the
answer, the close phase was reached, or record_outcome(resolve) was earned through a successful
CONFIRM round." 但 evaluator 的判定只检查 "本轮是否有成功的 record_outcome"。它**永远不会**因
"用户接受了答案"或"到了 close phase"返回 READY_TO_CONFIRM——前两条 doc-comment 描述的入口在代码里
没有实现。

## 3. Multi-layer root-cause analysis

按 `iteration_governance.md §3.2` 走 decision questions：

| Q | 触发？ | 判断 |
|---|---|---|
| Q1 infra（crash / timeout / OOM）？ | 否 | run loop 不崩；不是 infra。 |
| Q2 当前 Tier-0 invariant 被破坏？ | **要看怎么定义** | `runtime_freeze_and_risk_policy.md` §1 / §2 没有"resolve outcome only on CONFIRM" 这样的 Tier-0。Sprint 11 §M1 引入这个 guard 时是为了避免"single factual answer 同轮 collapse 成 resolved"——这是行为校准，不是 Tier-0 安全底线。因此 **不是 java_guard**。 |
| Q3 projection 缺关键 slot？ | 部分 | RESOLVE-FAQ 的 projection 没有"用户上一轮是否接受答案"信号；LLM 没有可读的"是否该 record_outcome" 判定依据。这是一个次生 layer。 |
| Q4 多轮 state 丢失？ | 否 | 这个死锁不依赖跨轮 state 丢失。 |
| Q5 LLM 选了语义错误动作？ | 否 | LLM 想做的就是合理动作（grounded answer + record_outcome），是 runtime 拒绝。 |
| Q6 eval CaseSpec / judge 错？ | 否 | 不是 eval 端问题。 |
| Q7 product / policy 决定？ | 否 | 不需要产品权限决策。 |

**主层（primary layer）= `infra` + `semantic_planner` 边界破裂**：

- `infra` 子层（runtime trace contract）—— `shouldRejectPrematureResolveOutcome` 这条 guard 用 phase 做接受/拒绝判据，但 phase 本身又只能通过 record_outcome 推进。这是 runtime 一侧的契约自闭环 bug，归类为 `infra`（runtime invariant 配错）。
- `semantic_planner` 子层（§1.3 LLM owns "next action / response strategy"）—— "现在应不应该 record_outcome" 应该是 LLM 的语义决定，不是 runtime 的硬判定。当前 guard 把它从 LLM 手里夺走。
- 次生 `prompt_projection` —— LLM 看到的 projection 缺一个"前一轮 disposition / 上一轮我说了什么 shape" 的诊断 slot，造成 LLM 在 turn N+1 不知道该不该 record_outcome。

**why 这是 `infra + semantic_planner` 而不是 `java_guard`**：当前 guard 不保护任何 Tier-0 invariant；它保护的是 Sprint 11 §M1 时刻所观察到的一个行为偏差（"single ad-id 请求被同轮 collapsed 成 resolved"），这是 §1.5 禁止的"用 keyword/regex/if-else 修语义失败"——但 Sprint 11 时刻是 `human_review_required` 的合理出口，并不是 Tier-0 提升。这件事的修法应该回到 §1.3 LLM-owned。

## 4. Coverage check vs action_bank / milestone scope

我把这个死锁映射到现有 R-items / OQ：

| 现有条目 | 与本问题的关系 | 结论 |
|---|---|---|
| `R-controlkernel-default-resolved-on-close-anti误杀` (M-Auto-7 S-A, dev-side closed) | 同一 stamp 链路的 **另一头**：去掉了 CLOSE-arm 默认 stamp。但没有动 record_outcome guard 本身的死锁。 | **相邻、不重叠**；S-A 收窄了一条代偿入口，但代偿的存在本身是因为本死锁。 |
| `R-uc-a-entity-context-verify-procedure-via-autoloop` (M-Auto-7 S-Y1/S-Y2, ACTIVE CORE GATE) | 用 autoloop 给 FAQ-RESOLVE 拼新的 `procedure` 文本。它**会**踩到本死锁：若 autoloop 提议"先 lookup → 再 record_outcome"，runtime 仍然拒绝 record_outcome；procedure 行为变化要么靠 LLM 内部判断，要么靠绕过 record_outcome。 | **顺序约束**：autoloop pilot 应该跑在**未修复**的旧 baseline 上（保证测量同一信号），修复**之后**再说。详见 §9 顺序分析。 |
| `OQ-S77.goal-impossible-resolved-evidence` (open, eval_spec) | "resolved+goal_impossible+positive-evidence" 是否 hard-fail。和本问题方向相同（trace 一致性），但属于 eval-side policy；本问题是 runtime-side 死锁。 | **不重叠**。 |
| `R-r5-citation-result-binding-grounding-strengthening` (open) | R5 citation guard 与 record_outcome 同位置（都在 SkillGuardrailDispatcher 里）但语义独立——R5 关心"引用是否来自当前 result"。 | **相邻，不重叠**。 |
| `OQ-S77.stall-detector-window` (open) | 与死锁本身无关。 | **不重叠**。 |
| Sprint 074 / 075 / 077（已 close）里的 `isResolvedSuccessTerminal` / `shouldVoidResolvedStamp` | 这两份代码就是本死锁的现行**代偿**——runtime 直接在 trace 端把 containment 写成 resolved/incomplete，绕过 record_outcome。 | 修复本问题后，这两个代偿可以**降级为 defense-in-depth**（不立刻删，但不再是主路径）。 |
| `confirm.yaml` 的 mandatory `record-outcome-on-confirmed-resolve` critical step | 当前几乎从不被实际走到。修复后这条 critical step 才会真正具有 mandatory 语义。 | **被本修复解锁**。 |

**Gap**：action_bank §5 中**没有**条目专门记录"FAQ-RESOLVE record_outcome guardrail 死锁 + confirm.yaml 不可达"这件事——它一直被两个 runtime 代偿掩盖。本提案应作为一个**新 R-item** 写入：`R-record-outcome-guardrail-deadlock-on-resolve-faq`。

## 5. Design alternatives

下面四个方案按"侵入度从大到小 / LLM-first 程度从高到低"列出。

### 5.1 Option A — 完全移除 guard（最 LLM-first）

**做什么**：删除 `shouldRejectPrematureResolveOutcome` 的 phase 拒绝逻辑；`record_outcome(resolve)`
在 RESOLVE-FAQ 上一律放行。`ResolveDispositionEvaluator.evaluate()` 保持现状（用
`recordOutcomeSucceededThisRun` 检测 READY_TO_CONFIRM）。LLM 调成功 → READY_TO_CONFIRM → CONFIRM。

| 维度 | 评价 |
|---|---|
| 对齐 §1.3 | 高：LLM 完全拥有 "is this resolved now" 的决定权 |
| 反误杀 | 弱：Sprint 11 §M1 当年的 pathology（bot 同轮既问 ad-id 又 record_outcome）若 model 仍有此倾向，会再次发生 |
| Trace 一致性 | 高：session_outcomes 行与 containmentOutcome 同源 |
| 对 isResolvedSuccessTerminal 代偿 | 仍可保留为 defense-in-depth（LLM 偶尔不 record_outcome 时托底） |
| 对 autoloop pilot | 中等正向（autoloop 提议的 procedure 都能跑通），但**改变了 baseline 信号源**，pilot 评估失稳 |
| 落地复杂度 | 低（删 10 行）；测试改动中等（很多 `Sprint11ProgressiveResolveTest`-类测试需要更新） |

### 5.2 Option B — 软注册：允许同轮 record_outcome 但不当轮推进 CONFIRM

**做什么**：放行 `record_outcome(resolve)`（删 guard）；`evaluate()` 即使 `record_outcome` 本轮成功，
若 bot 文本不是显式"closing"语气，仍返回 `ANSWERED_SUBTASK`（保留在 RESOLVE）。下一轮收到用户 positive 反馈
再推进 CONFIRM。需要一个新的"positive-confirmation 监测"入口（可复用 legacy
`isPositiveConfirmation`，或迁入 D16 plan-aware 路径）。

| 维度 | 评价 |
|---|---|
| 对齐 §1.3 | 中：LLM 调了 record_outcome，但 phase 推进仍依赖 runtime 信号 |
| 反误杀 | 中：避免"单轮 collapse"，但 session_outcomes 行**在用户确认之前**就被写入——若用户立刻"no still wrong"，需要 void/overwrite |
| Trace 一致性 | 中：session_outcomes 行先于 containmentOutcome 落地，反映"LLM 认为 resolved" 而非"用户接受了" |
| 对 isResolvedSuccessTerminal | 仍需保留作为兜底（多轮路径） |
| 落地复杂度 | 中高（需要新增"是否已 record_outcome 但未 confirm"的中间状态 + void-on-rejection 逻辑）|

### 5.3 Option C — 收窄 guard，复用 ASKED_FOR_SLOT 判定（推荐）

**做什么**：把 `shouldRejectPrematureResolveOutcome` 改成委托给 `ResolveDispositionEvaluator.evaluate()` 内部的 ASKED_FOR_SLOT 判定逻辑（同一份正则 / 同一份 `?` 终止判断）。规则变为：

```
shouldRejectPrematureResolveOutcome(plan, currentPhase, outcomeClass, botText):
  if (plan == null) return false;
  if (!isResolveFaqPlan(plan)) return false;
  if (outcomeClass not in {resolve, resolved}) return false;
  if (currentPhase in {CONFIRM, CLOSE}) return false;
  // NEW: 只拒绝 bot 自己同轮问问题 / 软请求 slot 的情况
  if (textShapeImpliesAskedForSlot(botText)) return true;
  return false;  // grounded factual answer 类——放行
```

`textShapeImpliesAskedForSlot` 等价于把 `evaluate()` 里 `asksForSlot` 那段判定提成 helper
（`text.endsWith("?") || CLARIFYING_QUESTION_PATTERN.matches() || SOFT_NEXT_STEP_PATTERN.matches()`）。
这样 guard 与 disposition 两条决策路径**永远共享一个判定**，杜绝 guard 拒了又被 evaluator 等价
ASKED_FOR_SLOT 但措辞不同的歧义。

`evaluate()` 同步调整：当 `FINAL_ANSWER` + 非 ASKED_FOR_SLOT + `record_outcome` 本轮成功 → 返回
`READY_TO_CONFIRM`；非 ASKED_FOR_SLOT + `record_outcome` 未成功 → 仍是 `ANSWERED_SUBTASK`（兜底 stamp
路径保留）。

| 维度 | 评价 |
|---|---|
| 对齐 §1.3 | 高：grounded 事实答案 + 同轮 record_outcome 是 LLM 合理决策；guard 只拦明显 pathology |
| 反误杀 | 高：Sprint 11 §M1 当年的 SOFT_NEXT_STEP/CLARIFYING_QUESTION 检测仍然兜底 |
| Trace 一致性 | 高：grounded resolve 路径上，session_outcomes 行 + containmentOutcome 同步落地 |
| 对 isResolvedSuccessTerminal | 保留为 defense-in-depth（LLM 没主动 record 时托底） |
| 对 autoloop pilot | 中性偏正：pilot 跑完后再落地，不影响 pilot 对照 baseline |
| 落地复杂度 | 低：~30 行 Java 改动；测试改动可控（共享 helper 后两条路径同源） |
| §1.5 / §1.7 | 已有的 SOFT_NEXT_STEP_PATTERN 是 pre-existing soft heuristic；不引入**新**正则；通过 §4.1 |

**这是推荐方案。**

### 5.4 Option D — 取消 CONFIRM 作为独立 phase（最大改造）

**做什么**：把 CONFIRM 折叠回 RESOLVE。`record_outcome` 成为单一 LLM-owned 信号；CONFIRM 仅作为"已 record，等用户最后一句"的 trace 标签（不再是 phase）。confirm.yaml 退役或并入 resolve_*。

| 维度 | 评价 |
|---|---|
| 对齐 §1.3 | 最高 |
| 落地复杂度 | **巨大**（Skill loader / projection / 所有 test / baseline / eval 报告 等全部受影响）|
| 风险 | 改 Skill 拓扑会同时改变 D16 freeze 的 governance contract（M5-S4 §3.H 之类）|
| 建议 | 长期方向；暂不在 1-2 sub-sprint 范围内做 |

## 6. Recommended option + rationale

**Option C** — 收窄 guard 复用 ASKED_FOR_SLOT 判定。

理由：

1. **最小可逆**：只动 `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome` + `evaluate()` 内部判定共用（约 30 行 Java），不动 Skill 拓扑、不动 projection schema、不动 baseline 测量管道。
2. **§1.3 对齐但不冒进**：把"是否该 record_outcome"还回 LLM，但保留 Sprint 11 §M1 反误杀（同一份正则继续保护"单轮 collapse"）。
3. **Trace 一致性自然回归**：grounded resolve 路径上，`session_outcomes` 与 `containmentOutcome` 同源，且 `isResolvedSuccessTerminal` 可降为兜底（不立刻删，保 defense-in-depth）。
4. **解锁 confirm.yaml mandatory critical step**：现在那条 `record-outcome-on-confirmed-resolve` mandatory 的 trace_check 才真正具备 mandatory 语义；governance 的 trace 契约自圆其说。
5. **不与 autoloop pilot 抢资源**：本 sub-sprint 不在 M-Auto-7 路径内；建议作为 M-Auto-7 close 之后的独立工作。

否决理由：
- Option A 反误杀太弱（删了所有同源判定）。
- Option B 引入"行先于事实"的不一致 + void-on-rejection 复杂度。
- Option D blast radius 太大。

## 7. Scope split + delivery priority suggestion

> Scope 拆分是**建议**给 deliver-agent；最终的 milestone / sprint 形态由 deliver-agent 决定。

### 7.1 优先级

- **不建议**进 M-Auto-7（pilot 需稳定基线）。
- **建议**作为 M-Auto-7 close 之后的下一个 milestone（候选 M-Auto-8 / 子项目）。
- **不建议**作为单 PR 一次性落；分 3 步可以让 baseline 飘移逐步可观测。

### 7.2 建议拆分

| # | sub-sprint 假名 | 范围 | 估算 |
|---|----|----|----|
| 1 | **S-Z1 (research / measurement)** | 在 M-Auto-7 close 的 baseline 上统计：FAQ-RESOLVE FINAL_ANSWER 中 ASKED_FOR_SLOT shape vs ANSWERED_SUBTASK shape 的比例；`isResolvedSuccessTerminal` stamp 数 vs `record_outcome` 成功数 的差额；Sprint 11 §M1 当年观察到的"single-shot collapse" pathology 在当前 model 上是否复现。Read-only 调查，无代码 / config 改动。Output: `docs/diagnostics/<date>-record-outcome-guardrail-measurement.md`。 | 0.5–1 d |
| 2 | **S-Z2 (code change)** | 在 `ResolveDispositionEvaluator` 中：(a) 抽 `textShapeImpliesAskedForSlot(String)` 成 package-private helper；(b) `evaluate()` 改用该 helper；(c) `shouldRejectPrematureResolveOutcome` 新增 `botText` 参数并复用 helper，guard 收窄为 "只在 ASKED_FOR_SLOT-shape 上拒绝"；(d) `SkillGuardrailDispatcher.handlePrematureResolveOutcomeGuard` 把 bot 文本（`ctx.parsedUserMessage()`）传进来；(e) 文档注释 / Sprint 11 §M1 comment 更新——保留 anti-误杀 框架，移除 phase-only check 的强表述。Tests: 更新 `Sprint11ProgressiveResolveTest`-类、新增 5 个 characterization tests 覆盖"grounded factual + record_outcome → CONFIRM"、"ad-id 软请求 + record_outcome → 拒绝 + stay RESOLVE"、"问号结尾问题 + record_outcome → 拒绝"、"CONFIRM phase 上的 record_outcome 不受影响"、"RESOLVE-INTAKE 路径不受影响"。 | 1–2 d |
| 3 | **S-Z3 (re-bless + 代偿降级)** | 跑 milestone-shared re-bless；对比 S-Z1 baseline；确认 session_outcomes 行数上升 + containment_outcome 分布稳定 + 反误杀 floor 不动。在 `ControlKernel.isResolvedSuccessTerminal` 注释里标注"defense-in-depth, 优先看 record_outcome 成功事件"；不删代码，但把它从"主路径"语义降为"备用 stamp"语义。Codex §4.1 per-sub-sprint **REQUIRED**（这是 §1.3 边界变更，必须独立审）。 | 2 d + re-bless 时长 |

### 7.3 Pre-flight QA gate（§5.9）

S-Z3 的 re-bless 跑批前，pre-flight 必须额外检查：
- `BotEvent` 中 `RECORD_OUTCOME_GUARD` 事件的拒绝率（应明显下降）；
- `session_outcomes` 表新增行数（应有 net 增加）；
- `isResolvedSuccessTerminal` stamp 频次（应有 net 减少）；
- 安全 floor (`uc_g/h/i/j + cs38s*`) 未触发新的"误判 resolve"。

## 8. Layer classification + §7 stanza pre-fill (multi-layer prospective)

按 `iteration_governance.md §7.1`，本 sub-sprint(s) 触碰 semantic surface（runtime semantic decision = "should record_outcome happen on RESOLVE-FAQ"），属 **semantic-touching sprint**，**必须**带 stanza。多 sub-sprint 因此 stanza 是 multi-layer prospective（每条 sub-sprint 一份）。

### 8.1 S-Z1 stanza pre-fill (research)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra （diagnostic-only sub-sprint — measure §3.2 Q1 infra layer scope before code change; no production code touched）

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced.

**Generalization coverage:** target / neighbor / negative / shadow case counts: 0/0/0/0 — diagnostic-only sub-sprint, no CaseSpec authoring. (Counts deferred to S-Z2.)
```

### 8.2 S-Z2 stanza pre-fill (guard narrowing)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (RESOLVE-FAQ phase advance contract) + semantic_planner (§1.3 LLM owns "next action / response strategy")

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. It REMOVES an over-broad runtime gate that was added in Sprint 11 §M1 without a current Tier-0 invariant covering it; the SOFT_NEXT_STEP_PATTERN / CLARIFYING_QUESTION_PATTERN anti-误杀 surface is preserved verbatim.

**Semantic hardcode:** No NEW semantic hardcode introduced. Pre-existing regex (SOFT_NEXT_STEP_PATTERN, CLARIFYING_QUESTION_PATTERN) re-used through a single shared helper; same patterns, narrower scope. Sunset plan: helper becomes downgrade-to-signal candidate once a projection-based positive-confirmation slot is available (carry to S-Z4 follow-on, not bundled here).

**Generalization coverage:** target / neighbor / negative / shadow case counts: 5 / 3 / 3 / 2 (target = grounded-factual+record_outcome→CONFIRM; neighbor = clarifying-question+record_outcome→reject, soft-next-step+record_outcome→reject; negative = INTAKE-RESOLVE record_outcome path unchanged, escalate/abandon outcome_class unaffected, CONFIRM-phase record_outcome unaffected; shadow = 2 ad-id-style traces from m-auto-6 baseline). Authored as characterization tests in S-Z2; CaseSpec authoring (if needed) deferred to S-Z3 re-bless.
```

### 8.3 S-Z3 stanza pre-fill (re-bless + 代偿降级)

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (trace-contract honesty — isResolvedSuccessTerminal demoted from primary to defense-in-depth; no behavior change beyond stamp source)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced. Pure measurement + comment / classification change in ControlKernel.

**Generalization coverage:** target / neighbor / negative / shadow case counts: covered by full milestone-shared re-bless (n=9 multi-suite); explicit anti-误杀 floor cases (uc_g/h/i/j + cs38s*) MUST stay green.
```

## 9. Hard fences + non-goals

### 9.1 Hard fences（任何阶段都不做）

- **不**新增任何 `user_message` content keyword / regex / if-else（§1.5 / §1.7）。SOFT_NEXT_STEP_PATTERN / CLARIFYING_QUESTION_PATTERN 是 pre-existing，**不扩展**这两个正则。
- **不**修改 `confirm.yaml` / `resolve_faq_grounded_answer.yaml` 的 `procedure` 语义文本（避免与 autoloop pilot 抢同一份语义产物）。
- **不**修改 `applicable_phases` / Skill 选择拓扑（不动 D16 freeze）。
- **不**删除 `ControlKernel.isResolvedSuccessTerminal` / `shouldVoidResolvedStamp`（保留为 defense-in-depth）。
- **不**修改 RecordOutcomeTool 自身（不动 outcome_class 规范）。
- **不**修改 `tool-policy.yaml` 中 `record_outcome` 的 `allowed-ucs: [ALL]`。
- **不**在 system_prompt 里加任何 phase / record_outcome 教学（system_prompt 是 §1.7 禁区）。

### 9.2 Non-goals（明确不做）

- 不在本 milestone 取消 CONFIRM phase（Option D 留作长期）。
- 不在本 milestone 改造 `session_outcomes` schema 或 `SessionOutcomeRepository`。
- 不在 M-Auto-7 时间窗内落地（顺序约束，见 §10）。
- 不替 autoloop pilot 做"何时该 record_outcome" 的 procedure 文本（那是 S-Y2 的工作）。

## 10. Risk + compounding-effect analysis

### 10.1 Compounding-effect（顺序约束）

| 序 | 内容 | 不按此序的代价 |
|---|---|---|
| 1 | M-Auto-7 S-Y2 autoloop pilot 在**当前 guard 未改**的 baseline 上跑（CORE GATE） | 若先改 guard：pilot 的对照基线从"runtime-代偿的 resolved" 切到 "record_outcome 真正落 row 的 resolved"，pilot 的 `procedure` 评分 signal 与历史 baseline 不可比，pilot 失稳 |
| 2 | M-Auto-7 close（pilot 落地 + milestone re-bless） | （同上） |
| 3 | S-Z1（diagnostic） | 在新 baseline 上量化 ASKED_FOR_SLOT shape 比例 |
| 4 | S-Z2（code change） | — |
| 5 | S-Z3（re-bless + 代偿降级） | 改完代码不 re-bless：session_outcomes / containment_outcome 漂移不可知 |
| 6 | 后续 follow-on：若 LLM 主动 record_outcome 比例足够稳定，再触发 isResolvedSuccessTerminal 进一步收窄的 sub-sprint（非必须） | — |

**同步触发的代偿降级**：S-A (`R-controlkernel-default-resolved-on-close-anti误杀`) 已经把 CLOSE-arm 默认 stamp 砍掉；本提案完成后 `isResolvedSuccessTerminal` 在 grounded-resolve 路径上的"主路径"作用也降为兜底。两个收窄各自独立、累加效应是 trace stamp 来源彻底由 LLM tool 真实事件控制。

### 10.2 Risk

| ID | 风险 | 缓解 |
|---|---|---|
| R-1 | Sprint 11 §M1 pathology（single ad-id-request + 同轮 record_outcome）在当前 model 上仍存在 → 修复后又出现一波"误 resolve" | S-Z1 预先量化；S-Z2 保留 SOFT_NEXT_STEP/CLARIFYING_QUESTION 共享判定 |
| R-2 | session_outcomes 行数突然上升导致下游 eval 报表 / aggregate 出现"看起来"的 pass-rate 抖动 | S-Z3 milestone-shared re-bless + 与 baseline paired 对比；视为 measurement-honesty shift，非 regression |
| R-3 | 反误杀 floor（uc_g/h/i/j + cs38s* + safety-of-pass）受牵连 | S-Z3 pre-flight 强制覆盖；A6 safety-of-pass 不变 |
| R-4 | autoloop pilot 已经落地了一个 procedure，假设 record_outcome 不可调；新版可调后该 procedure 行为变化 | S-Z3 之前先复审 pilot 落地的 procedure：是否需要在 record_outcome 行为变化后重新评估 |
| R-5 | `ResolveDispositionEvaluator.evaluate()` 文档注释（READY_TO_CONFIRM 三条入口）与现实**已经**不一致；本修复只补齐其中一条（同轮 record_outcome 成功），另两条（"用户明确接受"、"close phase 到达"）仍然没有代码实现 | S-Z2 同步修文档注释；其它两条留作 Option D 长期方向，**不**强行实现 |
| R-6 | guard 收窄引起若干现有 integration/characterization test（`AgentRunLoopS1FaqGroundedResolveGuardTest` 等）需要重写 | S-Z2 计划内；用 5 个新的 characterization test 替换/补充，覆盖新规则 |
| R-7 | M-Auto-7 期间出现 user-impacting 死锁报告（PROD trace 显示 record_outcome 反复被拒）—— hot-fix 压力 | 文档化代偿（`isResolvedSuccessTerminal`）已经稳定运行 ≥ 3 个 milestone；trace 端无 user-visible 影响；不需要 hot-fix |

### 10.3 与 §1.7 forbidden list 的对照

- "encoding raw eval phrases into Java or prompt" —— **不触碰**。
- "adding UC-specific hard rules for soft semantic decisions" —— 反向，本提案是**移除**一条 UC-无关但语义硬规则；§1.7 校验过。
- "widening eval spec to accept a genuine bot mistake" —— **不触碰**（eval-side 不动）。
- "optimizing visible eval at the cost of shadow/generalization" —— 修复后 visible eval pass rate 可能**下降**（measurement honesty），非"伪 optimization"。
- "using prompt as an if-else rule dump" —— **不动 prompt**。

## 11. Observability / trace / report implications

### 11.1 直接受益

- `BotEvent.RECORD_OUTCOME_GUARD` 事件（`ControlKernel.emitRecordOutcomeGuardEvent`）的"reject 率"应**显著下降**——一个可量化的 success metric。
- `BotEvent.RESOLVE_DISPOSITION` 事件中 `READY_TO_CONFIRM` 比例上升，`ANSWERED_SUBTASK` 比例下降——LLM-owned 决策真正在 trace 上可见。
- `session_outcomes` 表：resolve 类行数将出现量级增长（从近 0 → 数百/baseline 跑）。这是 trace-contract honesty 的体现。
- `confirm.yaml` 的 `record-outcome-on-confirmed-resolve` mandatory critical step 终于在生产 trace 中具备 mandatory 语义——之前几乎不可达。

### 11.2 需要 admin trace / eval report 配套

- Admin trace UI 现在已经有 `RESOLVE_DISPOSITION` / `RECORD_OUTCOME_GUARD` 两条事件；本修复**不需要**新增事件类型。
- Eval aggregate（`autoloop` + `eval_interactive`）目前从 containmentOutcome 取信号；修复后**信号来源**变了（更多来自 record_outcome 真实事件，更少来自 isResolvedSuccessTerminal 兜底），aggregate 数字本身不变（仍是 containmentOutcome），但 trace explanation 路径变短——这是好事。
- 建议在 S-Z3 close 时同步更新 `docs/current/faq_grounding_contract.md`（已有 R-item `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source` 处理 must_cite_source 漂移；本修复同时漂移 `record_outcome` 路径描述）。

### 11.3 已知 doc drift（修复时同步处理）

- `ResolveDisposition.java:23-26` 的 READY_TO_CONFIRM 注释中"用户明确接受"、"close phase 到达"两条入口在代码中**从未**实现——修复时同步修注释（保留三条文字 OR 删除未实现的两条，二选一；建议保留并标注"前两条由 isResolvedSuccessTerminal 在 trace 层兜底实现，非 evaluator 路径"）。
- `confirm.yaml:32-49` `record-outcome-on-confirmed-resolve` mandatory severity 与现实严重不符——修复后才真实生效；无需修改 YAML。

### 11.4 新 R-item 入 action_bank §5

`R-record-outcome-guardrail-deadlock-on-resolve-faq` —— 一段 row：

```
| R-record-outcome-guardrail-deadlock-on-resolve-faq | This solution doc (2026-06-08) + Sprint 11 §M1 root + Sprint 074/075 isResolvedSuccessTerminal compensation | ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome rejects record_outcome(resolve) on RESOLVE phase, but READY_TO_CONFIRM is the only RESOLVE→CONFIRM path and is conditioned on the same record_outcome succeeding — confirm.yaml unreachable on the live FAQ-resolve path; runtime compensates via isResolvedSuccessTerminal (containment="resolved" without session_outcomes row). Solution: narrow the guard to ASKED_FOR_SLOT-shape only, sharing the existing SOFT_NEXT_STEP/CLARIFYING_QUESTION detection; isResolvedSuccessTerminal demoted to defense-in-depth. | infra + semantic_planner | open / proposed; sequenced AFTER M-Auto-7 close to protect the autoloop pilot baseline; recommended Option C |
```

## 12. Open questions for human + deliver-agent

1. **顺序确认**：建议本工作排在 M-Auto-7 close 之后；human 是否接受这个顺序，还是希望并行？如果并行，pilot baseline 的可比性需要单独讨论（S-Y2 跑两条 baseline，开销大）。
2. **代偿降级范围**：S-Z3 把 `isResolvedSuccessTerminal` 注释成 defense-in-depth，是否同时**收窄**它的 disposition 接受面（例如仅 READY_TO_CONFIRM、移除 ANSWERED_SUBTASK），需要单独评估，本提案不绑定该决定。
3. **`evaluate()` 文档注释处理**：READY_TO_CONFIRM 三条入口注释要"删未实现两条"还是"标注由 trace 层兜底"？前者更准确，后者更连贯。建议后者。
4. **legacy `evaluateConfirm.isPositiveConfirmation` 是否清理**：D16 plan-aware 路径全开后该 legacy 路径事实死代码；本修复不动，但可作为后续整理项。
5. **是否抽出独立 milestone**：本提案 3 个 sub-sprint 是否值得包成一个独立 milestone（如 "M-Auto-8 Phase-advance contract honesty"），由 deliver-agent 与 human 共同决定。
