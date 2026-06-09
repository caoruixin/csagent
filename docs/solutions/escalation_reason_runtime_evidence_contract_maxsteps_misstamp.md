---
title: Escalation-reason 运行时证据契约 — MAX_STEPS 回退误盖 faq_miss_threshold_exceeded（第 4 实例根因 + 提升评估）+ moderation_context 投影缺口的从属定位
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-05-24
mode: bad-case-driven
---

## 0. 阅读指引（给 deliver-agent）

本文是 **Path 2（bad-case-driven）研究产出**，输入是
`docs/diagnostics/failure-briefs/manual-probe-2026-05-24-uc-a-faq-refutation-overescalate.md`
（第 4 实例）及其前序
`docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`。

**本文不重新推导核心 pattern**（已由前序 brief +
`R-escalation-reason-runtime-evidence-contract-review` +
`R-runtime-orchestrator-tool-call-deduplication` 覆盖）。本文 scope 严格限定在
2026-05-24 brief 的 **5 个 open question**，并交付 Mode-2 四项强制产出：

| Mode-2 强制产出 | 本文位置 |
|---|---|
| ① 多层根因（NEW wrinkles，code-grounded，HEAD `9ef9d1e` 验证） | §2 + §3 |
| ② Coverage check（确认/扩展 4 实例重叠） | §4 |
| ③ Compounding-effect（含与 M5 S3 moderation 审计的顺序） | §8 |
| ④ Deliver-consumable proposal（§3.2 layer + sub-sprint + §7 stanza + hard fences） | §5–§7 |

**一句话结论**：4 个实例里 human 一直观察到的 "`faq_miss_threshold_exceeded` 与
`faq_miss:false` 直接矛盾"，**主因不是 LLM 自盖、也不是 faq_miss 计数器**，而是
**运行时 `PhaseEvaluator.resolveMaxStepsReason` 在 agentic loop 耗尽（MAX_STEPS）时的回退映射**
——它只问"这一轮跑过 search_knowledge 没有"，**完全不看 faq_miss 证据**，把任何"搜过、
没收尾、没在 clarify"的耗尽都误盖成 `faq_miss_threshold_exceeded`（`PhaseEvaluator.java:183-193`）。
代码注释自己都承认这是 "mis-map"（`AgentRunLoopImpl.java:516-519`）。这把 R-item 从
"LLM 自盖 reason 该不该 gate" **扩展**为 "运行时自己的回退也是同一矛盾的独立来源"——
**强烈跨过提升门槛**，但**不属于 M5（observability-only）**，应进下一个语义里程碑。

---

## 1. 关键架构事实（承重，五问共用）

### 1.1 两条 FAQ 执行路径（这是全部分析的地基）

`ControlKernel` 按 feature-flag / 是否迁移路由（`ControlKernel.java:346-356`）：

- **路径 ①：LLM tool-call loop**（`AgentRunLoopImpl.run`）——**live path**。LLM 把
  `search_knowledge` / `resolve_article` / `record_outcome` / `request_handover` 作为
  tool_call 发出；`SkillGuardrailDispatcher` guardrails 在此生效。**brief 的 trace 走的是这条路**
  （trace 里有离散的 `get_customer_context` / `search_knowledge` / `record_outcome` /
  `request_handover` tool 事件——这是 LLM tool-call 形态，不是 legacy 直驱）。
- **路径 ②：legacy `PhaseEvaluator.resolveFaq`**（`PhaseEvaluator.java:900-979`）——
  flag off / 未迁移 / `plan()==null` 时的 fallback。`search_knowledge` 由 Java 直驱（非 LLM tool_call）。

**这条分界是 Q1 的命门**：`escalation_reason` + guardrail 活在路径 ①；
**`faq_miss_count` 只在路径 ② 自增**（`PhaseEvaluator.java:927-929`，且 `if (searchResult.isFaqMiss())` 严格门控）。
brief 走路径 ①，所以 `faq_miss_count` 全程停在 0，`BudgetChecker.java:40`
（`faqMissCount >= maxFaqMiss` 才升级）**根本没触发**。

### 1.2 escalation_reason 的两个独立来源

| 来源 | 机制 | 是否检查 faq_miss 证据 | code |
|---|---|---|---|
| **(A) LLM 自发** | LLM 把 `escalation_reason` 作为 `request_handover` 参数发出，Java **逐字透传**（不杜撰） | 否（但有 guardrail 在 dispatch 前拦截，见 §3.1） | `RequestHandoverTool.java:60,87`；`AgentRunLoopImpl.java:627-630`；`EscalationReasonResolver` 仅做 canonical/precedence，不杜撰（`:73,:111`） |
| **(B) 运行时 MAX_STEPS 回退** | loop 耗尽无终态 → `AgentRunResult.maxSteps` → `PhaseEvaluator` `case MAX_STEPS` → `resolveMaxStepsReason` | **否（核心缺陷）** | `AgentRunLoopImpl.java:548-554`；`PhaseEvaluator.java:594-610` + `:183-193` |

**(B) 是 brief 真正命中的来源**（§3.1 给出 trace 级证据）。

---

## 2. 5 个 open question 的 code-grounded 回答（HEAD `9ef9d1e`）

### Q1 — 为何 `faq_miss_threshold_exceeded` 在每次 `faq_miss:false` 时仍触发？LLM 盖 / 计数器盖？

**答：都不是。是第三个选项——运行时 MAX_STEPS 回退映射 (B)。** 三段证据：

1. **不是计数器**：`faq_miss_count` 唯一自增点 `PhaseEvaluator.java:927-929`，受
   `if (searchResult.isFaqMiss())` 门控，且只在 legacy 路径 ②。brief 走路径 ①，计数器停 0
   （投影 `ContextProjectionBuilder.java:351,613` 只是 read-only echo）。**计数器不会因
   `record_outcome` error 或重复 search 而自增**（直接回答 Q1 的子问）。

2. **LLM 确实自发盖了 (A)，但被 guardrail 拦下**：`faq_miss_handover_requires_resolve_attempt`
   guardrail（`SkillGuardrailDispatcher.java:217-257`，dispatch 前触发于
   `AgentRunLoopImpl.java:359-364`）的 reject 条件是：handover ✓ ∧ reason==faq_miss_threshold_exceeded ✓
   ∧ 本轮无成功 `resolve_article`（`:230-233`）∧ 本轮 `search_knowledge` 有结果且无 error
   ∧ **`faq_miss != true`**（`:239-240`）∧ hits 非空（`:241-242`）→ **REJECT**。
   brief 的 T3 恰好命中此条（search hits、faq_miss=false、无 resolve_article、然后
   request_handover(faq_miss_threshold_exceeded)）→ **guardrail 必拦**。所以 LLM 那次
   request_handover **不是**最终用户看到的升级。

3. **最终 reason 是 (B) 运行时盖的**——决定性证据是**用户最终看到的那句话**：
   "I'm having difficulty resolving this. Let me connect you with a specialist."
   是 `PhaseEvaluator.java:608` 的**唯一来源**（全仓 grep 仅此一处），它只在
   `case MAX_STEPS` 分支产生。配套的 reason 由 `resolveMaxStepsReason` 计算
   （`:606,:609`）：非 INTAKE、clarificationCount==0、**"这一轮跑过 search_knowledge"
   → 直接 return `faq_miss_threshold_exceeded`（`:192-193`），完全不看 faq_miss 真假**。

   即：loop 耗尽 maxToolSteps（FAQ skill = 4，`resolve_faq_grounded_answer.yaml:24`）→
   `AgentRunLoopImpl.java:553-554` 返回 `maxSteps` → `PhaseEvaluator` 盖
   `faq_miss_threshold_exceeded` + 机械模板。**代码注释自承这是 mis-map**：
   `AgentRunLoopImpl.java:516-519` —— "the legacy mapper mis-maps to ESCALATE /
   faq_miss_threshold_exceeded even when search_knowledge returned hits"。

**为什么 loop 会耗尽**：本轮所有终态动作都被（正确地）拒：
`record_outcome(resolve)` 被 premature-resolve guard 拒（§Q2）；
`request_handover(faq_miss_threshold_exceeded)` 被 faq_miss guard 拒（上文）；
而 LLM 始终没做对的事（对 viable hits 调 `resolve_article`，或 pivot 到 moderation grounding）。
→ 步数耗尽 → MAX_STEPS → 运行时回退把 guardrail 刚拦掉的那个 reason **原样重新盖回**，且不带证据检查。

> **这是 NEW wrinkle 的核心**：Sprint 39（2026-05-18）新增的 `faq_miss_handover_requires_resolve_attempt`
> guardrail 关掉了 (A) LLM-自发 向量；但 (B) 运行时 MAX_STEPS 回退向量**一直没关**——
> 这正是为什么同一矛盾在 2026-05-24（post-guardrail）**又**复现。前序 brief（2026-05-13，
> pre-guardrail）误把它归为 "LLM 自盖 (semantic_planner)"；code 显示终态那句机械模板同样来自
> MAX_STEPS，故 (B) 在 2026-05-13 就已是真凶之一。

### Q2 — `record_outcome(resolve)` ERROR：真 infra bug 还是 redaction artifact？是否触发 fallback 升级？

**答：是 redaction artifact 掩盖的一次 guardrail 拒绝，不是 infra bug；它不直接触发升级。**

- `record_outcome` 在本场景被 **Sprint 11 premature-resolve guard** 拦下（不是工具自身报错）：
  `AgentRunLoopImpl.java:393-432` 在 dispatch 前调
  `skillGuardrailDispatcher.checkBeforeOutcomePersist(...)` → `handlePrematureResolveOutcomeGuard`
  （`SkillGuardrailDispatcher.java:307-327`）→ `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`
  （`:160-187`）：RESOLVE/FAQ plan 上、`outcome_class=resolve`、当前 phase 非 CONFIRM/CLOSE → REJECT。
  reject label `progressive_resolve_record_outcome_premature`（`SkillGuardrailDispatcher.java:89-90`），
  被写进 `accumulatedToolResults.record_outcome = {error: <label>, hint:...}`（`:417-420`），工具**根本没被执行**。
- `[REDACTED_TOKEN]` 是 **trace redactor 的 LONG_TOKEN 误伤**：
  `ToolCallTraceSanitizer.java:122-123` 的 `\b[A-Za-z0-9_\-]{32,}\b` 会吞掉任何 32+ 字符、
  无空格的 token。`progressive_resolve_record_outcome_premature`（44 字符无空格）正好被替换成
  `[REDACTED_TOKEN]`（写出于 `ControlKernel.java:1859-1862` 经 `sanitizeErrorMessage`）。
  反证：`RecordOutcomeTool` 自身的所有真 infra error 串都**含空格**（`RecordOutcomeTool.java:49-69`），
  不会被这条 regex 吞，因此 `[REDACTED_TOKEN]` **不可能**是工具的真 infra 报错。
- **是否触发 fallback 升级？否（不直接）**：tool error 被回灌给 LLM 作为下一步 observation
  （`AgentRunLoopImpl.java:486-492`，下一 step 重建投影），**Java 不强制升级**；handover 短路只在
  `request_handover` **成功 dispatch** 时（`:504-508`）。故后续 request_handover 是 LLM 自选；
  而最终用户看到的升级是 §Q1 的 MAX_STEPS 回退（间接结果）。
- 副作用：guard-reject 在 session 上盖 `recordOutcomeGuardResult="rejected:..."`（`AgentRunLoopImpl.java:425-429`），
  投影于 `ContextProjectionBuilder.java:570-571`；但**不动 faqMissCount**。

> **观测建议（不 redacted 的旁证）**：`record_outcome_guard_result` 投影槽**不经 redactor**，
> 会逐字写 `rejected:progressive_resolve_record_outcome_premature`。deliver-agent 在 admin trace
> 里读这个槽即可零歧义确认 Q2（也是 M5 S2 per-invocation trace 落地后的天然受益点）。

### Q3 — 重复 `search_knowledge`：和 2026-05-13 同一个 orchestrator-dedup bug？还是 record_outcome 失败后的 agentic 重搜？

**答：不是 orchestrator bug。是 LLM 跨 loop step 重发——这里具体是 guardrail 拒掉 record_outcome 后的
agentic 重搜。** 且 2026-05-13 那个"orchestrator dup bug"**本身已被 Sprint 19 §4.3 推翻**：

- `R-runtime-orchestrator-tool-call-deduplication` 现状 **partial / 已 reclassify 为 `semantic_planner`**
  （`docs/action_bank.md`）："LLM emitted three identical search_knowledge calls across three
  consecutive AgentRunLoop steps … **no orchestrator-side amplification; `AgentRunLoopImpl.java` has no
  de-duplication path**"。
- Sprint 19 handoff §4 逐条否掉了三个 orchestrator 放大假设（phase-transition replay / 失败重放 /
  单 LLM 多 tool_use），结论是 **multi-step reading**：每个 outer step 一次 LLM 调用、各发一次 search。
  `accumulatedToolResults` 每 `run()` 全新（`AgentRunLoopImpl.java:161`），且 `for (step…)` 外层循环
  （`:179`）逐 step 重新 projection（已含 `already_called` + `accumulated_tool_results`）——LLM 看到
  "已搜过"仍重发，是 **semantic_planner 行为**，不是 orchestrator 重放。
- 2026-05-24 的重搜与之同构，且更易解释：record_outcome 被拒、handover 被拒，LLM 没别的可做就**重搜**
  ——每次重搜烧一个 step，**直接加速 loop 耗尽**（喂进 §Q1 的 MAX_STEPS）。

> 故 Q3 的提问前提（"和 2026-05-13 的 orchestrator-dedup bug 是不是同一个"）本身要修正：
> **2026-05-13 从来不是 orchestrator bug**。两个实例是同一 `semantic_planner` 重发机制。
> 无需新开 infra dedup 项；现有 `R-runtime-orchestrator-tool-call-deduplication` 的 partial
> 状态 + 拆分（read-side `already_called` soft signal / write-side HandoverOrchestrator）已覆盖。

### Q4 — `moderation_context` 投影缺口是否贡献根因？投影它能让 bot ground 而非升级吗？

**答：投影槽确实缺失（已确认），数据源其实已存在；但对 brief 这个 probe 不会改变结局，
对真实 moderation 移除案才有用。它是 necessary-but-not-sufficient，且属 S3 之后的语义里程碑。**

- **投影槽确缺**：`ContextProjectionBuilder.java` 全文 "moderation" 仅 3 处、**均为注释/工具描述**
  （`:111` get_customer_context 描述串、`:944`/`:998` Javadoc），**无 `moderation_context` 槽**。
  投影只无条件 emit form/customer/listing（`:636-648`）。
- **`required_context_keys` 是死声明**：投影**从不读** `plan.requiredContextKeys()`。它仅在
  `PhaseEvaluator.java:455` 写入 plan，全仓无任何 projection-side reader。故
  `resolve_faq_grounded_answer.yaml:19-23` 里声明 `moderation_context` **对投影零作用**。
- **数据源其实存在（比 brief 设想的更强）**：
  (a) `BotSession.moderationContext` 字段已建（`BotSession.java:114-115`；migration `V1`），
  且在 **INIT 阶段就被填充**——`FormContextIngestionService.autoTriggerCustomerContext` 对
  `{UC-A,UC-C,UC-D,UC-F,UC-FP,UC-K}` 自动跑 get_customer_context，命中 removed/moderated listing 时
  `session.setModerationContext(...)`（`FormContextIngestionService.java:125-127`）；
  (b) `get_customer_context`（AGENT_VISIBLE、允许 UC-A）对 removed/moderated listing 返回
  `moderation_review`（`GetCustomerContextTool.java:54-59`），LLM 本就能调；
  (c) 另有 `get_moderation_review_context` 工具，但 `type: RUNTIME_ONLY`
  （`server/src/main/resources/config/tool-policy.yaml:30-32`，`allowed-ucs: [UC-A, UC-FP]`）
  → **LLM 看不到、无自动调用方**，处于 dormant。
- **对 brief 这个 probe 不解决问题**：trace 里 `get_customer_context` 返回
  account_found=false / listing_found=false（probe ad 不在 store）→ 没有 listing → 不会 fetch
  moderation_review → `session.moderationContext` 为空。**即便投影了槽，也无内容可投**。
- **对真实 moderation 移除案有用**：若是真实"被 moderation 移除"的 ad，数据已在 session 上，
  **加一行读 `session.getModerationContext()` 的投影槽**即可让 LLM 在 UC-A turn 看到移除原因
  ——这就是 brief 设想的"ground 而非升级"，但前提是 ad 真在 store 且真被移除。
- **UC-A 的 moderation grounding 可达性**：UC-A 有**数据路**（LLM 可调 get_customer_context 拿
  moderation_review，skill grounding_instruction `:32` 明确允许直接答 account/ad/moderation 工具数据），
  但**缺**：(1) per-turn 投影里没有 moderation 槽；(2) 没有 mandatory 程序推动
  ——`consult-moderation-context-on-removal-explanation` critical step 是 **UC-FP-only**
  （`resolve_faq_grounded_answer.yaml:115-129`，`mandatory_for: [UC-FP]`）。

> **Q4 与 M5 S3 的关系（重要）**：M5 S3 的 C1 审计**只 DOCUMENT 这个缺口**；C2 收敛要"让投影读
> Skill 声明"——但 M5 明令 **"不改变 LLM 可见的语义信息可得性"**
> （`milestone_objective.md` §2/§6/§4 non-goals）。若 S3 C2 顺手开始投 `moderation_context`，
> 那是**新语义信号**，越了 M5 的 observability-only 边界。**所以 moderation_context 的行为修复必须
> 排在 M5 之后的语义里程碑**（详见 §8 顺序分析）。

### Q5 — 提升评估：4 实例 + Tier-0-candidate 是否跨过门槛进入下一个语义里程碑？谁拥有 fix？

**答：跨过。且本次新证据把 R-item 的 scope 显著扩大，更应排期。Fix 是多层的（见 §5/§6）。**

跨门槛的理由（不是简单计数到 4）：

1. **从"症状重复"升级为"机制定位"**：前 3 实例把矛盾归为 LLM 自盖；本次 code-grounded 定位到
   **运行时自身的 MAX_STEPS 回退是独立来源**，且 code 注释自承 mis-map。这是从 "n=4" 到
   "已知确定性根因"的质变——**确定性 bug 比统计 pattern 更该修**。
2. **Sprint 39 guardrail 已证明只堵了一半**：post-guardrail 仍复现，证明 (A) 向量已堵、(B) 向量敞开。
   这是"修了一半、另一半系统性敞开"的明确信号。
3. **跨 UC 跨 reason 的系统性**：`resolveMaxStepsReason` 同一函数还会盖 `incomplete_intake`
   （cs040 的 `intake_complete_for_uc_k` 同源）/ `clarification_budget_exhausted`——**同一回退面**
   服务多个 evidence-claiming reason，修一处护多 UC。
4. **R-item 本就标注 Tier-0-candidate**（`action_bank.md`，"Tier-0 promotion requires
   `human_review_required` per §3.2 Q2"）；本次证据正是触发该 human 决策的时机。

**层归属**（§3.2，多层）：

- **运行时证据契约（主、承重）** → §3.2 **Q2** 命中"看似 java_guard 但无现行 Tier-0 覆盖"
  → **`human_review_required`**（需 human 决定是否开 Tier-0；若开则落 `java_guard`/`infra`）。
  **不是 §1.7 semantic hardcode**：它读的是运行时已有的 `faq_miss` 工具结果旗标（`ToolEvent.resultData`），
  不是对用户文本的 keyword/regex；escalation_reason 的**回退盖值本就是 Runtime-owned**（§1.4 trace/eval contract）。
- **moderation_context 投影 + faq_miss 证据投影** → §3.2 **Q3** `prompt_projection`（投影贫乏导致 LLM
  缺信息）。
- **"saw it yesterday" pivot 到 moderation / 对 viable hits 调 resolve_article** → §3.2 **Q5**
  `semantic_planner`（LLM-owned，**不硬编码**，靠投影 enable）。

---

## 3. 多层根因小结（Mode-2 产出①）

| 层 | NEW wrinkle | 根因 | code（HEAD `9ef9d1e`） |
|---|---|---|---|
| **infra / java_guard 候选（主）** | `faq_miss_threshold_exceeded` 与 `faq_miss:false` 矛盾的**真凶是运行时 MAX_STEPS 回退**，非 LLM 自盖 | `resolveMaxStepsReason` 只问"搜过没"，不看 faq_miss 证据；guardrail 只堵 LLM 自发向量，回退向量敞开 | `PhaseEvaluator.java:183-193,594-610`；`AgentRunLoopImpl.java:516-519,548-554`；guardrail `SkillGuardrailDispatcher.java:217-257` |
| **infra（澄清）** | `record_outcome` ERROR 是 redaction 误伤的 premature-resolve guard reject，非 infra bug，不直接触发升级 | LONG_TOKEN regex 吞 44 字符 label；tool error 回灌 LLM 不强制升级 | `ToolCallTraceSanitizer.java:122-123,399`；`SkillGuardrailDispatcher.java:89-90,307-327`；`AgentRunLoopImpl.java:393-432,486-492` |
| **semantic_planner（澄清）** | dup search 非 orchestrator bug，是 LLM 跨 step 重发（record_outcome 被拒后的重搜）；加速 loop 耗尽 | 无 orchestrator dedup 路径；LLM 见"已搜过"仍重发 | Sprint 19 §4.3 + `action_bank.md`；`AgentRunLoopImpl.java:161,179` |
| **prompt_projection（从属）** | `moderation_context` 投影槽缺失、`required_context_keys` 死声明；数据源已在 session 上被丢弃 | 投影硬编码 form/customer/listing，不读 `requiredContextKeys`；不读 `session.moderationContext` | `ContextProjectionBuilder.java:636-648`（无 moderation 槽）；`FormContextIngestionService.java:125-127`（已填充）；`BotSession.java:114-115` |
| **semantic_planner（从属）** | LLM 没对 viable hits 调 resolve_article、没在 "saw it yesterday" pivot 到 moderation | LLM-owned 决策，投影未给足证据（faq_miss 旗标/ moderation 原因） | skill `resolve_faq_grounded_answer.yaml:31-33,115-129`（moderation step 仅 UC-FP） |

---

## 4. Coverage check（Mode-2 产出②）

| 现有项 | 关系 | 本次结论 |
|---|---|---|
| `R-escalation-reason-runtime-evidence-contract-review`（`action_bank.md`，Tier-0 candidate） | **确认 + 显著扩展** | 现 4 实例（cs040 + cs176 + 2026-05-13 + 2026-05-24）。**扩展点**：R-item 原表述只问"运行时是否该强制 LLM-claimed escalation_reason 带证据"；本次证明**运行时自己的 MAX_STEPS 回退（`resolveMaxStepsReason`）也是同一矛盾的独立来源**。契约必须覆盖**两条向量**：(A) LLM-emitted（已被 Sprint 39 guardrail 部分覆盖）+ (B) runtime-fallback（敞开）。 |
| `R-runtime-orchestrator-tool-call-deduplication`（`action_bank.md`，partial→semantic_planner） | **再确认，无新增** | 2026-05-24 dup 同源（LLM 跨 step 重发）。**不开新 infra dedup 项**；前提（"orchestrator bug"）在 2026-05-13 已被 Sprint 19 §4.3 推翻。 |
| `D-S3-no-prior-search-guard`（`action_bank.md`，deferred） | 已被 Sprint 39 部分实现 | "无前置 search 不许 faq_miss handover"——Sprint 39 的 `faq_miss_handover_requires_resolve_attempt` guardrail 已覆盖"有 hits 须先 resolve"的相邻语义；本提案不重开 D-S3，转而补回退向量。 |
| `D-faq-grounded-resolve-bypass`（`action_bank.md`，deferred） | 相邻 | "对 viable hits 不 resolve 就收尾"正是本案 LLM 行为之一；本提案的 prompt_projection（投 faq_miss 证据）+ semantic_planner enable 与之同向。 |
| `D-new-escalation-reason-enum`（`action_bank.md`，deferred/avoid） | **硬约束** | 修复**不得新增/重命名** enum 值（cross-cut eval `ESCALATION_TRIGGER_VALUES`）。推荐复用既有 `turn_budget_exhausted`（`PhaseEvaluator.java:169,195` 已是 catch-all）。 |
| M5 S3 C1 moderation 审计（`milestone_objective.md` §3 S3） | **从属 + 顺序耦合** | S3 只 DOCUMENT moderation_context 缺口；本提案的 moderation 行为修复**排在 M5 之后**，不得塞进 M5（§8）。 |
| `R-faqMissCount-threshold-and-timing-review`（`action_bank.md`） | 正交 | 那是 legacy 路径 ② 的阈值/时序；本案在路径 ①，计数器停 0，不重叠。 |

**缺口**：现有 backlog **没有任何项**覆盖"运行时 MAX_STEPS 回退误盖 evidence-claiming reason"
这一确定性根因——这是本研究新识别、需登记的 R-item 扩展（见 §9）。

---

## 5. 设计方案 + trade-off

> 这是**未来语义里程碑**的内容（**不属 M5**）。下列为 deliver-agent 可消费的备选。

### 5.A 运行时证据契约（escalation_reason ↔ runtime evidence）

- **A1（推荐，承重）—— evidence-aware MAX_STEPS 回退**：改 `resolveMaxStepsReason`
  （`PhaseEvaluator.java:183-193`），遍历 `result.toolEvents()` 时不只看 `te.toolName()=="search_knowledge"`，
  还读 `te.resultData()` 里的 `faq_miss`（`ToolEvent.resultData` 已携带，`ToolEvent.java:25,42`）：
  **当本轮跑过 search_knowledge 但每次 `faq_miss==false`（有 viable hits）→ 不盖
  `faq_miss_threshold_exceeded`，回退到既有 `turn_budget_exhausted`**（catch-all，零新 enum）。
  - 优点：contained；只读已有运行时证据，**非 §1.7 hardcode**；reuse 既有 enum；一处修护多 UC；
    让运行时回退**诚实**（faq_miss reason 名实相符）。
  - 缺点：需 human Tier-0 决策；改回退 reason 值可能影响 eval 侧对特定 case 的期望（需对 ESCALATION_TRIGGER_VALUES 核对）；
    **只让 reason 诚实，不解决用户问题**（用户仍被升级，只是 reason 更准）。
- **A2（全量契约）—— 双向量 evidence gate**：在 (A) LLM-emitted（扩 guardrail）与 (B) runtime-fallback
  两处统一校验所有 evidence-claiming reason（`faq_miss_threshold_exceeded` /
  `intake_complete_for_uc_*` / `clarification_budget_exhausted`），排除 `user_requested` / `user_distress`
  （已有独立契约，R-item 明确排除）。
  - 优点：一次性关掉 4 实例 + 系统性类。缺点：Tier-0 面更大、scope 更重，须 human 谨慎拆。
- **A3（仅 prompt/semantic）—— 不动运行时**：靠 §5.B 投影让 LLM 少耗尽、多 resolve。
  - 优点：LLM-first、无 Tier-0。缺点：loop 真耗尽时运行时仍误盖，**契约违反作为完整性问题持续存在**；依赖 LLM 行为。

### 5.B prompt_projection enablement（降低耗尽 + 让 LLM 能 ground）

- **B1 — 投 `moderation_context` 槽**：读已填充的 `session.getModerationContext()`，
  仿 form/customer/listing（`ContextProjectionBuilder.java:636-648`）。**依赖 M5 S3 的
  Skill-driven 投影收敛先落地**（否则是把语义信号塞进 M5 observability scope）。
- **B2 — 投 faq_miss 证据摘要**：把"本轮 search_knowledge 已返回 N 条 viable hits（faq_miss=false），
  应 resolve_article 收尾"作为 soft signal 投影，enable LLM 不重搜、不误升。**registry/Skill-driven，
  不引入 per-UC if-else**。

### 5.C semantic_planner（不单独做，由 B enable）

LLM 对 "saw it yesterday" 的 pivot、对 viable hits 的 resolve_article 是 §1.3 LLM-owned——
**不硬编码**，靠 §5.B 的投影证据 enable。

---

## 6. 推荐 + 理由（Deliver-consumable proposal 主体，Mode-2 产出④）

**推荐组合：A1（运行时证据契约，承重）+ B1/B2（投影 enablement，排在 M5 S3 之后）**，
作为**下一个语义里程碑**的 2 个 sub-sprint；semantic_planner 由 B enable，不单列。

理由：
- A1 是**最 contained、最高完整性收益**的修复——它堵住运行时自己制造的 evidence-contract 违反，
  跨 UC 复用（cs040 intake 同源回退面），且**不解决用户问题也无副作用**（reason 更诚实而已）。
  这是 4 实例系统性类的**载重修复**。
- B1/B2 才真正降低**用户被误升级**的概率（让 LLM 看到 moderation 原因 / faq_miss 证据，从而 resolve/pivot）；
  但它们触语义投影面，**必须 real-LLM bad-case 重跑**，且**依赖 M5 S3**（Skill-driven 投影 + C1 消费图）。
- 顺序：**A1 独立于 M5**（运行时，非投影面）→ 可先做或与 M5 并行；**B1/B2 必须在 M5 S3 之后**（§8）。

### 6.1 Sub-sprint 建议（deliver-agent 做最终拆分）

| 建议 sub-sprint | 层（§3.2） | scope（3 句） | 依赖 | Codex |
|---|---|---|---|---|
| **SS-A：escalation-reason 运行时证据契约（A1）** | `human_review_required` → `java_guard`/`infra`（须 human Tier-0 决策） | 改 `resolveMaxStepsReason` 读 `ToolEvent.resultData` 的 faq_miss 旗标；本轮有 viable hits 时回退到 `turn_budget_exhausted` 而非 `faq_miss_threshold_exceeded`。可选：把 guardrail 的 reject hint 升级为"对 hits 调 resolve_article"的更强 enable。覆盖 target/neighbor/negative/shadow + cs040/cs176 回归。 | 无（独立于 M5） | **per-sub-sprint REQUIRED**（§4.3 触发 #1 Tier-0 candidate） |
| **SS-B：moderation_context + faq_miss 证据投影（B1+B2）** | `prompt_projection`（§3.2 Q3） | 投 `moderation_context`（读 session 已填充字段，Skill-driven 门控）+ faq_miss 证据 soft signal；registry/Skill-driven，无 per-UC if-else。real-LLM bad-case 重跑为证据门。 | **M5 S3（Skill-driven 投影 + C1 消费图）** | milestone-shared（除非触发 per-sub trigger） |

> semantic_planner pivot 不单列：由 SS-B 的投影 enable，验证在 bad-case 重跑（"saw it yesterday"
> → resolve/pivot 而非升级）。

---

## 7. §7 stanza 预填（多层 prospective；per-sub-sprint）

### SS-A —— escalation-reason 运行时证据契约

```markdown
## Layer-classification + anti-hardcode stanza
**Target failure layer:** human_review_required → java_guard / infra
（运行时 MAX_STEPS 回退 reason 映射的 evidence 校验；需 human Tier-0 决策 per §3.2 Q2）。

**Tier-0 invariant:** 本 sub-sprint 是 Tier-0 **candidate**，须 human 决策。候选不变式：
"运行时盖出的 evidence-claiming escalation_reason（faq_miss_threshold_exceeded /
incomplete_intake / clarification_budget_exhausted）必须与运行时已收集的对应证据一致；
无证据时回退到 catch-all turn_budget_exhausted。" 排除 user_requested / user_distress
（已有独立契约）。若 human 不开 Tier-0，则降级为 infra correctness fix。

**Semantic hardcode:** 不引入。读的是运行时已有的 ToolEvent.resultData.faq_miss 旗标
（非用户文本 keyword/regex/if-else）；escalation_reason 的回退盖值本就是 §1.4 Runtime-owned。
**不新增 escalation_reason enum 值**（D-new-escalation-reason-enum 仍 deferred）——复用既有
turn_budget_exhausted。

**Generalization coverage:** target = 2026-05-24 + 2026-05-13 UC-A FAQ MAX_STEPS-误盖；
neighbor = cs040 intake / cs176 同回退面的其它 evidence-claiming reason；negative = 真正
faq_miss=true 的耗尽**仍应**盖 faq_miss_threshold_exceeded（不许过修）；shadow = held-out
FAQ-miss vs FAQ-hit 耗尽 trace。real-LLM 重跑 + bad-case suite 人工判定为准。
```

### SS-B —— moderation_context + faq_miss 证据投影

```markdown
## Layer-classification + anti-hardcode stanza
**Target failure layer:** prompt_projection（ContextProjectionBuilder 投影面，§3.2 Q3）。

**Tier-0 invariant:** 不新增 Tier-0。投影是 §1.4 Runtime "trace/eval contract" 一部分，
但直接喂 §1.3 LLM 决策——本 sub-sprint **新增** LLM 可见语义信息（moderation 原因 + faq_miss 证据），
故**不属 M5**（M5 明令不改语义可得性），属 M5 之后的语义里程碑。

**Semantic hardcode:** 不引入。moderation_context 读 session 已填充字段、按 Skill
required_context_keys 门控（registry/Skill-driven，承接 M5 S3 C2）；faq_miss 证据为 soft signal
投影；**禁止** per-UC if-else 决定投哪些字段。

**Generalization coverage:** target = UC-A 真实 moderation-移除 ground（而非升级）；
neighbor = 同 FAQ skill 其它 UC（UC-C/D/F）+ UC-FP（已有 moderation critical step）；
negative = ad 不在 store（如本 probe）→ moderation 槽为空、不应误导；
shadow = held-out moderation-移除 trace。**real-LLM 重跑 + bad-case suite 为准**
（mocked-LLM 只覆盖投影 wiring，§5.6 eval evidence gate）。
```

---

## 8. Compounding-effect + 顺序约束（Mode-2 产出③）

**复利 1（最重要）—— SS-A 独立、SS-B 依赖 M5 S3，二者不可错序**：
- SS-A 是运行时回退 reason 的 correctness fix，**不碰投影面**，独立于 M5；可先做或与 M5 并行。
- SS-B 改投影内容（新增 moderation_context + faq_miss soft signal），**依赖 M5 S3 的 Skill-driven
  投影收敛 + C1 消费图**。若 SS-B 先于 M5 S3：(a) 等于把语义信号塞进 M5 的 observability scope
  （违反 `milestone_objective.md` §6 "no projection field changed before C1 map" + §2 "no semantic
  availability change"）；(b) 没有 C1 消费图，加 moderation 槽可能触发新的 eval trace
  `CONTRACT_VIOLATION`（投影契约校验器对字段集敏感，见 `observability_coherence_*` §9 复利 2）。

**复利 2 —— 与 M5 S3 的边界协调（必须显式）**：
M5 S3 C1 **只 DOCUMENT** moderation_context 缺口；C2 "让投影读 Skill 声明"时，**必须刻意
NOT 开始投 moderation_context**（否则改变语义可得性、越 M5 scope）。即 M5 S3 应在 C1 map 里
**标注**"moderation_context 是 declared-but-not-emitted，行为修复转 SS-B"，并在 C2 收敛时
**保持 moderation 槽不投**。SS-B 才在 M5 之后打开它。**deliver-agent 应在 M5 S3 contract 里写明
这条 carve-out**，避免 dev 顺手把 moderation 投上去。

**复利 3 —— SS-A 不能单独"解决"用户问题，但必须先于 SS-B 落地完整性**：
- 只做 SS-A：运行时 reason 变诚实（faq_miss=false 时不再盖 faq_miss_threshold_exceeded），
  但用户仍被升级（reason 改 turn_budget_exhausted）。**完整性↑，用户解决率不变**。
- 只做 SS-B：LLM 更可能 resolve/pivot、少耗尽；但 loop 真耗尽时运行时**仍**误盖 → 契约违反残留。
- 故 **SS-A 是 evidence-contract 的载重修复（先/独立）**，SS-B 是 user-resolution 修复（后、依赖 M5）。
  二者正交互补，**都做**才同时满足"reason 诚实"与"少升级"。

**复利 4 —— 错序的更坏结果**：若先 SS-B 后 SS-A，会"在 moderation 案上少升级"造成
"系统性 evidence-contract 缺口已解决"的假象，而 cs040(intake)/cs176 等**非 moderation** 的
MAX_STEPS 误盖仍系统性敞开——**症状被遮蔽，根因未除**（正是 research agent "只修症状不修原因"的禁区）。

**复利 5 —— 与 Sprint 39 guardrail 的叠加**：SS-A 必须与既有
`faq_miss_handover_requires_resolve_attempt` guardrail **协同**：guardrail 拒 LLM 自发向量、
SS-A 修运行时回退向量。两者一起才关闭 (A)+(B) 双向量；只改一处仍漏。**SS-A 不得削弱/绕过现有 guardrail**。

---

## 9. Hard fences + non-goals + 新 R-item 扩展建议

**Hard fences**：
- **不属 M5**（M5 = observability-only）。M5 S3 C1 只 DOCUMENT moderation 缺口；本提案行为修复进 M5 之后语义里程碑。
- **§1.7**：escalation 的语义决策由 LLM 拥有。SS-A 读的是运行时 ToolEvent faq_miss 旗标（运行时证据），
  **不是**对用户文本的 keyword/regex/if-else；这是运行时回退面的 correctness，不是 semantic hardcode。
- **无 per-case Java guard**：SS-A 是 UC-general 的 evidence 契约，**须经 human Tier-0 决策**
  （`R-escalation-reason-runtime-evidence-contract-review` 路径），不得为单 case 打补丁。
- **不新增/重命名 escalation_reason enum 值**（`D-new-escalation-reason-enum` deferred）——复用 turn_budget_exhausted。
- **不削弱现有 guardrail**（`faq_miss_handover_requires_resolve_attempt` / premature-resolve）。
- **SS-B 不得先于 M5 S3**；M5 S3 C2 收敛时 moderation_context **保持不投**（carve-out 写进 S3 contract）。
- **Negative 不许过修**：真正 faq_miss=true 的耗尽**仍应**盖 faq_miss_threshold_exceeded。
- **不改 LLM provider/模型/温度/deadline**；不动 `composite.py` scoring。

**Non-goals**：
- 不重开 `R-runtime-orchestrator-tool-call-deduplication` 为 infra dedup（已 reclassify semantic_planner）。
- 不处理 legacy 路径 ② 的 `faq_miss_count` 阈值/时序（`R-faqMissCount-threshold-and-timing-review`，正交）。
- 不把 `get_moderation_review_context` 改成 AGENT_VISIBLE（dormant 工具的 LLM 可见性是独立产品/工具决策）。
- 不实现 UC-A 的 moderation critical step 强制（mandatory_for 扩面是 skill 设计决策，留给语义里程碑评估）。

**建议登记/扩展的 R-item**（deliver-agent 决定）：
- **扩展 `R-escalation-reason-runtime-evidence-contract-review`** 描述，显式加入 **(B) runtime
  MAX_STEPS 回退向量**（`PhaseEvaluator.resolveMaxStepsReason` 不看 faq_miss 证据）——当前 R-item
  表述只覆盖 LLM-claimed 向量。建议在 notes 注明"4th instance（2026-05-24）code-grounded 定位到运行时
  回退是独立来源；Sprint 39 guardrail 只堵 LLM 向量"。

---

## 10. Observability / trace 启示（与 M5 协同）

- **M5 S2（per-invocation trace）直接受益**：本案需区分"LLM-emitted handover（被 guardrail 拒）"
  vs "运行时 MAX_STEPS 回退升级"。`phase_transition_reason`（=`max_steps_exceeded` vs `agent_escalated`，
  `PhaseEvaluator.java:610` vs `:592`）+ per-invocation 的逐 step tool_calls 一旦在 admin trace 可见
  （M5 S2 落地），deliver-agent 即可**零歧义**确认终态是 MAX_STEPS 回退而非 LLM 直接升级。
  **本研究的 Q1 结论建议在 M5 S2 落地后用真实 trace 复核**（当前由"机械模板唯一来源"强推断，证据充分但非直读 trace）。
- **`record_outcome_guard_result` 投影槽不经 redactor**（`ContextProjectionBuilder.java:570-571`）——
  Q2 的 `[REDACTED_TOKEN]` 真值 `rejected:progressive_resolve_record_outcome_premature` 可由此槽直读确认。
- **redaction 误伤值得单独记一笔**（不在本提案 scope）：`ToolCallTraceSanitizer` 的 LONG_TOKEN regex
  把 guardrail reject label 误盖成 `[REDACTED_TOKEN]`，降低了 trace 可读性——可作为 M5 S2/S4 的
  trace-quality 附带项（低优先级 observability nicety）。

---

## 11. 给 human / deliver-agent 的一句话总结

human 在 4 个实例里反复看到的 "`faq_miss_threshold_exceeded` 却 `faq_miss:false`"，**根因不是 LLM
自盖、也不是计数器**，而是**运行时 agentic-loop 耗尽时的回退映射 `resolveMaxStepsReason` 只问"搜过没"、
不看 faq_miss 证据**（`PhaseEvaluator.java:183-193`，code 注释自承 mis-map）；Sprint 39 的 guardrail
只堵了 LLM 自发向量、没堵这条运行时向量，所以同一矛盾 post-guardrail 又复现。**强烈跨过提升门槛**，
fix 是多层的：**SS-A 运行时证据契约（human_review_required→java_guard，须 human Tier-0 决策，独立于 M5、载重）**
+ **SS-B moderation_context/faq_miss 证据投影（prompt_projection，依赖 M5 S3、降低误升级）**；
**都不属 M5（observability-only）**，moderation 投影必须排在 M5 S3 之后并在 S3 C2 里写明 carve-out
（不投），否则会把语义信号偷渡进 M5 scope。
