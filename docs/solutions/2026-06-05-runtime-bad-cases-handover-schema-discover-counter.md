---
title: Runtime bad-case 调查 — request_handover schema 缺口 + DISCOVER clarification 计数未接线 + 配套 runtime/infra fix
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-05
mode: bad-case-driven
notes: >
  Bad-case 输入来自 human 提供的 6 条真实 trace（c1–c6）。本 proposal 只覆盖
  human 明确要求优先处理的 runtime/infra 层；semantic 层（DISCOVER 后期
  clarification 措辞、记录 outcome 时机等）显式留给 autoloop 调 skill
  procedures 跟进。受 §5.8 framework-defect priority 影响：M-Auto-5 close
  之前不开新 sub-sprint；本文档作为 M-Auto-6 候选材料 + 部分轻量 runtime fix
  可由 human 决定是否提前到 M-Auto-5 close 之后的第一个 runtime sub-sprint。
---

# Executive summary

Human 提供的 6 条 trace（c1–c6）暴露 **2 个系统性 runtime/infra 缺陷** + **2 项已知 by-design 行为**：

1. **R1（高价值，系统性）**：`request_handover` 在 intake UC（UC-G/H/I/J/K）下的工具 schema 缺失 `intake_fields` 属性声明 —— LLM 第一次调用从不带 `intake_fields`，因而触发 `intake_required_fields_missing_for_intake_complete` 报错；第二次调用 LLM 才根据错误 `hint` 补传 `intake_fields={...}`，runtime 在 dispatch 前把它持久化到 session 后才通过。**c1/c5/c6 共 3 个 case 完全是这一条引起**（c1 LLM 反复失败到第 6 turn 仍未恢复，最终 session 没有正常 handover）。Layer = `prompt_projection`（schema-vs-hint 错位；validator 设计正确）。

2. **R2（高价值，运行路径死代码）**：`BudgetChecker.maxClarificationRounds(=2)` 这个 clarification 预算在新的 live 路径 `AgentRunLoopImpl` 里**永远不被命中** —— 计数器 `session.clarificationCount` 只在旧的 `PhaseEvaluator.evaluateDiscover:880` 才会 `+1`，而 live 路径根本走不到那里。结果：DISCOVER phase 没有任何 clarification 预算上限，LLM 在 c3 中连续两个 turn 输出**逐字相同**的 clarification（即使 user 已补充新信息），既无 dedup 又无 budget escape。Layer = `infra` / `skill_state`（活路径未接线的计数器；不是 LLM 语义问题）。

3. **OBSERVATION-c2**：`progressive_resolve_record_outcome_premature` 是 `ResolveDispositionEvaluator` 在 phase ≠ CONFIRM/CLOSE 时拒绝 `record_outcome(class=resolve)` 的**正确 guard**。LLM 提前提交 → guard 正确弹回。Trace 里 error 是预期副产物；不需要 runtime 修复。仅可考虑（低优先）在 LLM-facing 的 `accumulated_tool_results` 里附加 "outcome 当前不可记录，等待 CONFIRM phase" 的软提示。Layer = `semantic_planner`（defer to autoloop）。

4. **OBSERVATION-c4**：现 trace 与 c3 重合（human 误粘贴）；按描述（"listings not showing in Chrome after clearing cache" / "...Safari works" 两次 query）所属 paraphrase 形态属于 **cross-turn rank-1 first refinement**，按 M-Auto-3 §11 三类 PARAPHRASE_STORM 分类法这是 **OBSERVATION-only**（不 gate，留 anti-误杀 floor），**不是 runtime 缺陷**。若实际是 rank-2+ repeated storm，S-Auto-15 `BotSession`-scoped 跨 turn budget gate 应已起作用 —— 需要 trace 重新核对再判定。本 proposal 不针对 c4 开 R-item。

**关键约束（必须遵守）**：

- 当前 M-Auto-5 close-BLOCKED on S-Auto-22；`docs/10-handoff.md` §0 写明 §5.8 framework-defect priority **ACTIVE**："no new semantic sub-sprints; no §5.6 bad-case rerun for milestone-close evidence while OQ-S77 is open."
- R1（`prompt_projection`）触及 semantic surface（projected schema 是 LLM 输入的一部分），必须含 §7 stanza。在 M-Auto-5 close 之前不应 launch。
- R2 主体（接线计数器）是 pure infra，不触及 semantic surface；但仍建议同 R1 一起排到 **M-Auto-5 close 之后的第一个 runtime/infra sub-sprint**，作为 **M-Auto-6 候选**（详见 §6 scope split）。M-Auto-6 现已 queued 的 Cluster C.3 "generic clarifier branch wasting opening turn" 与 R2 高度重叠，R2 是 C.3 的 code-grounded 实现路径。

---

# 1. Current-state survey（code-grounded）

所有引用路径在 HEAD（commit `021a86a`）验证过。

## 1.1 `request_handover` 调度路径

| 关注点 | 位置 | 行为 |
|---|---|---|
| Tool schema 注册 | `ContextProjectionBuilder.java:116-120` | `buildRequestHandoverArgsSchema()` |
| Schema 字段 | `ContextProjectionBuilder.java:200-260` | **只有** `escalation_reason`（required, 23-enum）+ `summary`（optional）。**没有 `intake_fields`** |
| Args 持久化点（先于 validator） | `AgentRunLoopImpl.java:486-494` | 当 `toolName=request_handover` && intake UC → 先调 `persistInlineIntakeFields(session, call)` |
| 持久化实现 | `AgentRunLoopImpl.java:981-1002` | 把 `call.arguments.intake_fields` 合并入 `session.intakeFields` JSONB |
| Validator（dispatch 之前） | `AgentRunLoopImpl.java:495-527` → `SkillGuardrailDispatcher.checkBeforeDispatch` | |
| Validator 实现 | `SkillGuardrailDispatcher.java:265-298` `handleIntakeCompleteRequired` | 读 `session.intakeFields`（已经合并了 args），调 `IntakeFieldsRegistry.intakeComplete(uc, collected)` |
| 必填字段定义 | `IntakeFieldsRegistry.java:53-67` | UC-G `[case_id]` / UC-H `[case_id]` / UC-I `[transaction_reference, dispute_reason]` / UC-J `[report_target, report_type, description]` / UC-K `[case_id]` |
| Heuristic extractor（在第一次 projection 之前） | `AgentRunLoopImpl.java:228` → `mergePartialIntakeFromContext` (line 950-970) | 只覆盖 UC-H / UC-J / UC-K 的有限规则提取，且只读 `formContext` + `userMessage` 的关键词模式，**不做语义抽取** |
| Reject hint 提示文本 | `SkillGuardrailDispatcher.java:288-298` | `"Ask the user for the missing intake fields above, then call request_handover with arguments.intake_fields populated."` |

**核心发现：**

- **设计意图**：dispatch 顺序是 "持久化 args → 跑 validator"（line 487-494 comment 写明 "persist inline intake fields BEFORE the guardrail check so a single complete handover call merging the missing fields is allowed through"）。即：**一次带完整 `intake_fields` 的 `request_handover` 应当原子通过**。
- **实际现象**（c1/c5/c6）：LLM 第一次 call **从不带 `intake_fields`** —— 因为 projected schema 里这个字段根本没声明，LLM 没有 contract 知道要传。LLM 只在收到错误 + hint 之后才补一次。
- **结论**：这不是 validator bug、不是 dispatch-顺序 bug、不是 IntakeFieldExtractor bug —— 是 **schema 与 validator 期望的 contract 错位**（schema 没声明 validator 需要的字段；现行 hint 走 "出错后教育 LLM" 的 detour）。

## 1.2 DISCOVER clarification 计数 / loop 保护

| 关注点 | 位置 | 行为 |
|---|---|---|
| Counter 字段 | `BotSession.java:81` | `clarificationCount: Integer = 0` |
| Counter `+1` 唯一调用点 | `PhaseEvaluator.java:880` | **legacy 路径**（旧 phase machine）；新 live 路径走 `AgentRunLoopImpl` 不经过这里 |
| Live 路径 DISCOVER 入口 | `ControlKernel.java:345-356` | flag-on 后路由到 `AgentRunLoopImpl.run(...)` |
| Live 路径搜索"clarif" 增量 | `AgentRunLoopImpl.java` | **零**（`grep -n incrementClarification\|setClarificationCount AgentRunLoopImpl.java` → 无结果） |
| Budget 检查（依赖 counter） | `BudgetChecker.java:32-37` | `session.getClarificationCount() >= controlPolicy.getMaxClarificationRounds()` → 永远 false |
| 配置阈值 | `ControlPolicyService.java:22, 42, 89` | `max-clarification-rounds=2`（config） |
| 计数器在 projection 中 | `ContextProjectionBuilder.java:351, 658` | 当 LLM 看到 `clarification_count` 字段时永远是 0 → 也没法靠 LLM 自我节制 |
| Cross-turn identical clarification dedup | （无） | grep 全 server/ 没有任何 "identical clarification within phase" 或 last-output-match 的保护 |
| Cross-turn paraphrase（仅 search_knowledge） | `AgentRunLoopImpl.java:671-736` (S-Auto-15) | 只针对 `search_knowledge`，不覆盖 free-text reply |
| Within-turn identical retry storm (A1) | `AgentRunLoopImpl.java:580-619` (S-Auto-12) | 只针对 tool call byte-identical，不覆盖 free-text reply |

**核心发现：**

- DISCOVER live 路径上 clarification 既无 **预算上限**、也无 **identical 重复保护**、也无 **paraphrase 抑制**。LLM 是 free-text reply 的唯一决策者，且它能看到的 `clarification_count` 永远为 0，所以连软信号都没有。c3 trace 就是这个空白的直接结果。
- 这是 typical **fix-half-shipped**：counter 在 legacy 路径有，新路径切换时没接线；budget checker 仍在 codebase 但变成 dead code。

## 1.3 `record_outcome` premature guard（c2）

| 位置 | 行为 |
|---|---|
| `SkillGuardrailDispatcher.java:89-90` 报错串 | `progressive_resolve_record_outcome_premature` |
| `ResolveDispositionEvaluator.java:160-186` | 拒绝条件：当前 phase ≠ CONFIRM/CLOSE && outcome class ∈ {resolve, resolved} && plan 是 RESOLVE/FAQ plan |
| `AgentRunLoopImpl.java:529-568` | dispatch 前调 `checkBeforeOutcomePersist`，错误返回 reject verdict + 软提示 |

**核心发现：guard 正确，是 LLM 在 phase=RESOLVE（用户尚未确认）时过早 commit outcome 的语义问题。**Defer to autoloop。

## 1.4 Cross-turn paraphrase（c4）

按 M-Auto-3 §11 三类 PARAPHRASE_STORM 分类法 + S-Auto-15 实现（`AgentRunLoopImpl.java:671-736`）：

- (1) within-turn = HARD gate（A3 keyed on `faq_miss` result state，不是 query 内容）
- (2) cross-turn rank-2+ = HARD gate（S-Auto-15，BotSession-scoped，budget cardinality）
- (3) cross-turn rank-1 first refinement = **OBSERVATION only**（19 实例不 gate，anti-误杀 floor）

c4 的 2 次 query 若属 rank-1 即合法；rank-2+ 则 S-Auto-15 应已 suppress。Human 误粘贴 trace 导致无法精确判定；本 proposal 不开 R-item，仅 OBSERVATION。

---

# 2. Multi-layer root-cause analysis（按 case 组）

按 `iteration_governance.md` §3.2 Fix Layer Classification 9 层：

| Case 组 | 观察现象 | 根因 layer | 一句话理由 |
|---|---|---|---|
| **c1, c5, c6** | `request_handover` 第一次 `intake_required_fields_missing_for_intake_complete`，第二次带 `intake_fields` 才成功（c1 始终未成功） | **`prompt_projection`** | Validator 设计正确，dispatch 顺序正确，IntakeFieldExtractor 行为符合预期；问题是 projected tool schema 没向 LLM 声明 `intake_fields` 字段 → LLM 第一次永远不传 → guard 第一次永远失败 |
| **c3** | DISCOVER phase 连续 2 turn 输出逐字相同的 clarification | **`infra`**（主） + `skill_state`（辅） | `clarificationCount` 在 live 路径死代码；live 路径无 identical-clarification 保护；LLM 也看不到 budget。Q1 → 不算 infra crash，Q3 → projection 缺 budget 信号 → 真因更接近 §3.2 Q4 "multi-turn flow losing state"（counter 跨 turn 应累计但实际不累计） |
| **c2** | `progressive_resolve_record_outcome_premature` | **`semantic_planner`** | Guard 正确；LLM 在错误 phase 提前 commit；defer to autoloop |
| **c4** | search_knowledge 多次 paraphrase query | **`semantic_planner`** / by-design OBSERVATION | rank-1 first refinement 是 anti-误杀 floor 允许的；rank-2+ S-Auto-15 应自动 suppress；无 runtime defect |

---

# 3. Coverage check（vs `action_bank.md` §5 + `milestone_objective.md` + 现有 R-items）

| 提议 R-item | 已有覆盖？ | 处置建议 |
|---|---|---|
| **R-intake-fields-schema-projection**（新；c1/c5/c6） | 无直接 R-item。`R-uc-k-intake-complete-case-id-binding`（action_bank §5.2 G1 surfaced）是 case_id 缺失场景，属 `skill_state`，**与本 R-item 不冲突但可同包** | **NEW R-item**，建议路由到 **M-Auto-6** runtime 子集 |
| **R-discover-clarification-counter-wireup**（新；c3） | M-Auto-5 close 后的 **M-Auto-6 Cluster C.3** "generic clarifier branch wasting opening turn"（`docs/10-handoff.md` §0）描述高度重合 | **不另开 milestone**；本 R-item 作为 C.3 的 code-grounded 实现路径 enrich 它；M-Auto-6 派单时合并 |
| c2 OBSERVATION | `R-prompt-phase-plan-directive-followship`（action_bank §5.2 Sprint 19 surfaced，OPEN）边缘相关但不重叠 | 不开 R-item；仅 trace 观察 |
| c4 OBSERVATION | M-Auto-3 §11 三类 PARAPHRASE_STORM taxonomy 已是当前 canonical | 不开 R-item |

无 R-item 冲突。建议 R1 + R2 同包到 M-Auto-6 的一个 **runtime/infra-only sub-sprint** 里（≤ 5 个文件 diff）。

---

# 4. Compounding-effect analysis

**先后顺序要点：**

1. **R1（intake schema）必须先于 R2（DISCOVER counter）落地** —— 不是技术依赖，是 **测量序**：R2 修复后 DISCOVER 不再死循环 → 更多 session 进入 intake UC（UC-G/H/I/J/K）→ R1 缺陷会以更高频率暴露。倒过来如果先修 R2 不修 R1，会出现 "DISCOVER 修好了但 intake handover 失败率上升" 的虚假回归信号，对 fitness/eval 噪声不利。

2. **R1 与 M-Auto-6 Cluster B.2（`primary_uc` vs `active_use_case` 40-60% mismatch）有间接 dependency** —— 若 active_use_case 跟踪本身 unreliable，则 R1 在 dispatch 时读到的 `plan.useCase()` 也不一定准。但 B.2 是 trace observability/语义判定，R1 是 schema 接口暴露，两者解耦；R1 先行不会被 B.2 阻塞。

3. **R1 与 `R-uc-k-intake-complete-case-id-binding`（action_bank §5.2）的关系** —— 前者解决 "LLM 知道要传什么"，后者解决 "runtime 知道 case_id 在哪里"。两者互补，可同包；若仅修 R1 而 case_id binding 仍坏，UC-K 仍会失败但失败 hint 会更清晰（"missing field: case_id" 而非通用 missing-fields）。

4. **不能跳过的反向风险**：若把 R1 改成 "validator 改为永远从 args 取 intake_fields"（移除 session 合并），会破坏 multi-turn 累计的设计 —— 用户分多个 turn 提供 report_type / target / description 时，runtime 需要靠 session 保持已收 fields。所以 R1 必须保留 "session 合并" 设计，只是 **再加** "schema 声明"。

5. **eval framework 影响**：S-Auto-22 完成后 M-Auto-5 close 才解锁，期间不应 launch R1/R2 子 sprint。M-Auto-5 close 后建议优先 launch R1+R2 包，再开 audit Cluster B/C 的 2× research-agent dispatch。

---

# 5. Design alternatives + trade-offs

## R1（intake schema 暴露）三个候选

| 方案 | 描述 | 优点 | 缺点 |
|---|---|---|---|
| **R1.a — Schema 字段 + 每 UC required-fields hint**（推荐） | `buildRequestHandoverArgsSchema()` 增加 `intake_fields` 为 `object`（properties = 不显式枚举，允许任意 string key→string value）；在 `ContextProjectionBuilder` 的 `intake_state` 投影 + tool description 里附 `required_fields_for_active_uc: [report_target, report_type, description]`（per active_use_case 渲染） | 一次性消除 first-call-fail；LLM 有显式 contract；零 LLM 语义改动；不增加 hardcode（per-UC required 列表已存在于 `IntakeFieldsRegistry.java`，只是 surface 一下） | 改动跨 3 个文件（schema + projection + 单元测试）；需要确保 schema 改动不破坏现有 mocked-LLM 测试 |
| R1.b — 把 intake_fields 必填全部移到 runtime 自动派生 | runtime 在 dispatch 前从 session 各种 state（form_context / accumulated_tool_results / userMessage history）自动 reconstruct intake_fields，validator 不依赖 LLM 传 | LLM 完全无感；最少 surface 变化 | 需要语义抽取（要么写 keyword/regex —— §1.7 禁止；要么再起一个 LLM 调用 —— 成本+延迟）；与"runtime 不做语义"的 §1.4 边界冲突 |
| R1.c — 把 hint 升级成 1-shot retry（runtime 看到第一次 fail 自动重投，注入 `intake_fields` 给 LLM 决策） | 不改 schema；只改 `AgentRunLoopImpl` 在 reject verdict 出现时插入合成软提示并不计 step | LLM 看见的 hint 更显式 | 仍然每次 intake handover 都消耗 1 步 budget；trace 仍然看到 error 事件；不解决根因 |

**推荐：R1.a**。理由：一次性、code-grounded、不引入 hardcode、不增加 LLM 调用、保持现行 dispatch 顺序。R1.c 是 R1.a 的退化补丁，不值得为它消耗 sub-sprint。R1.b 与 §1.4 边界冲突，否决。

## R2（DISCOVER counter）三个候选

| 方案 | 描述 | 优点 | 缺点 |
|---|---|---|---|
| **R2.a — 接线 counter + projection 显式暴露 budget**（推荐） | 在 `AgentRunLoopImpl` 的 DISCOVER 分支识别 "本 turn 输出 free-text clarification 且未 commit UC" 时 `session.clarificationCount += 1`；保留 `BudgetChecker` 现行 escape 路径（超过 max 时升级到 `clarification_budget_exhausted` request_handover）；在 projection 增加 `clarification_budget: {used: N, max: 2}` 软信号 | 复用已有 dead code，纯 wiring；budget 决策仍是 LLM-friendly；与 Cluster C.3 现路径一致 | 需要识别 "free-text clarification" vs "其它 free-text"，建议以 "phase=DISCOVER && 本 turn 无 tool call commit" 为准；不要做内容分析 |
| R2.b — 加 identical-clarification 跨 turn dedup（last-output exact-match） | 跨 turn 比对本 turn 输出与上 turn 输出，byte-identical → suppress 当前 free-text + 强制 LLM 重新决策 | 直接攻击 c3 的相同输出循环；anti-误杀（仅 byte-identical，不做相似度） | 需要新的 BotSession state（last clarification text）+ rerun 路径；和 §11 "rank-1 anti-误杀 floor" 哲学冲突；范围更大 |
| R2.c — 合并 R2.a + R2.b | budget 是软上限；identical 是硬保护 | 双重保险 | 复杂度上升；首版 不必 |

**推荐：R2.a**。原因：纯接线，复用已通过架构评审的 `BudgetChecker`；anti-误杀只靠 cardinality；与 §1.7 禁止 hardcode 一致；为 R2.b 留出后续观察空间，必要时再加。

---

# 6. Recommended option + scope split

## 推荐打包

**单个 runtime/infra sub-sprint（建议 M-Auto-6 第一批）：R1.a + R2.a 同包，总 diff 估 ~5–7 文件、~250 LOC、≤ 4 单元测试。**

| 工作项 | 文件 | 估改动 |
|---|---|---|
| R1.a — schema 字段 | `server/.../ContextProjectionBuilder.java`（`buildRequestHandoverArgsSchema` + `intake_state` 投影） | +40 / -2 |
| R1.a — projection 暴露每 UC required fields | 同上 | +20 |
| R1.a — 单元测试 | `server/.../ContextProjectionBuilderTest.java` | +60 |
| R1.a — 契约测试（验证一次性 handover 通过） | `server/.../AgentRunLoopImplIntegrationTest.java` 或近似 | +40 |
| R2.a — counter 接线 | `server/.../AgentRunLoopImpl.java`（DISCOVER 分支识别 + `setClarificationCount`） | +20 / -0 |
| R2.a — projection 暴露 budget | `server/.../ContextProjectionBuilder.java`（已在 line 351/658 写 count，需补 max） | +6 |
| R2.a — 单元测试 | `server/.../AgentRunLoopImplTest.java` | +50 |

**Anti-误杀 invariants**：

- R1.a：不删除 reject path（保留作为最后防线，新 schema 让首调一次过）。Counter-test：缺字段时仍 reject。
- R2.a：不引入任何内容匹配；counter 只递增不递减；max 配置不动；exhausted 升级到现有 `clarification_budget_exhausted` 路径。

## 与 §5.8 / M-Auto-5 close 的关系

- **M-Auto-5 close 之前不 launch 本 sub-sprint**（per §5.8 framework-defect priority）。
- M-Auto-5 close 后**第一个 runtime/infra sub-sprint** 是合理候选；与 audit Cluster B.2 / C.3 的 2× research-agent dispatch 可并行（dispatch 是 research、本 sub-sprint 是 dev，不冲突）。
- 若 human 希望更快推进 R2.a（infra-only，不触 semantic surface），可以单独切出 **R2.a 的 micro-sprint** 在 M-Auto-5 close 之后立即 launch（≤ 3 文件 diff，1 天工作量）；R1.a 等 §7 stanza 配套到位后再 launch。**deliver-agent 做最终决定**。

---

# 7. Layer classification + §7 stanza pre-fill

打包 sub-sprint（R1.a + R2.a）的 §7 stanza 草案：

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection (R1.a: request_handover schema 暴露 intake_fields)
+ infra/skill_state (R2.a: DISCOVER clarification counter live-path 接线 + budget projection)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.
（R1.a 是已有 IntakeFieldsRegistry contract 的对 LLM-facing 投影补全；
R2.a 是已有 BudgetChecker / max-clarification-rounds contract 的接线，
均不要求新的 runtime-级 invariant。）

**Semantic hardcode:** No semantic hardcode introduced.
（R1.a 暴露的 required-fields 列表完全来自 IntakeFieldsRegistry.java:53-67
的已有定义，未引入新 keyword/regex/enum；R2.a 只递增 cardinality counter，
零内容匹配；budget 软上限仍是 LLM-friendly，超限触发既有的
clarification_budget_exhausted escalation 路径。）

**Generalization coverage:** target / neighbor / negative / shadow case counts: 4 / ~12 / ~6 / ~22
- target: c1 (UC-J), c5 (UC-J), c6 (UC-I), c3 (DISCOVER loop)
- neighbor: 现有 case_families/uc_g_*, uc_h_*, uc_i_*, uc_j_*, uc_k_* 中所有
  intake-complete-for-uc-* escalation 的 case
- negative: 非 intake UC（UC-A, UC-B, UC-D, UC-F, UC-FP）的 handover —— 不应被
  误激活 schema 字段（field 可选；其它 UC 留空合规）；DISCOVER 非 clarification
  free-text reply（commit UC 的 reply）不应被计入 budget
- shadow: 既有 shadow 集（cs01s*, cs15s*, cs32s*, cs38s*, cs59s*, cs76s*, cs92s*）
  cross-suite 验证；至少覆盖 UC-K + UC-J + UC-A
```

`generalization coverage` 数字是粗估，最终由 deliver-agent + dev-agent 在执行前确定具体案例 ID。

---

# 8. Hard fences + non-goals

**Hard fences（绝不做）：**

- 不引入 keyword / regex / 内容相似度匹配于 R2.a 的 budget 判定（cardinality only；anti-误杀 §11 floor）
- 不为 c1/c5/c6 的具体 UC 写 per-UC if/else（required-fields 来源是已有 `IntakeFieldsRegistry`）
- 不改 `IntakeFieldsRegistry` 的字段定义（contract 已稳定，本 sub-sprint 只暴露）
- 不改 `SkillGuardrailDispatcher` 的 reject 逻辑（保持作为最后防线）
- 不改 `escalation_reason` 23-enum 枚举（参 `D-new-escalation-reason-enum` deferred / avoid）
- 不改 `record_outcome` premature guard（c2 by-design）
- 不为 c4 写 cross-turn semantic similarity dedup（违反 §11 anti-误杀 floor）
- 不触 simulator / eval framework / scoring SHA / autoloop 5-file 集（§5.8 + fence-#13）
- 不在 M-Auto-5 close 之前 launch

**Non-goals：**

- 不修复 c1 中 LLM 反复失败 6 turn 的具体 LLM 决策路径（那是 R1.a 修后自动消失；若仍残留按 autoloop semantic 跟进）
- 不为 DISCOVER 加任何"自动跳过 clarification 进 RESOLVE"的捷径（让 LLM 自己决定 commit UC）
- 不写新的 PARAPHRASE_STORM 跨 turn 抑制器（c4 OBSERVATION）
- 不重写 IntakeFieldExtractor 为 LLM-based（语义抽取不在本 sub-sprint scope；future R-item）

---

# 9. Risk + compounding-effect

| 风险 | 影响 | 缓解 |
|---|---|---|
| **R1.a schema 改动破坏 mocked-LLM 测试** | 现有 mocked-LLM 测试期望 schema 与既有形态严格一致 | 同包补 contract 测试；mocked-LLM 测试若硬编码字段 set 需要 expand，不引入新 schema 校验维度 |
| **R1.a `intake_fields` 字段被 LLM 误填到非 intake UC 的 handover** | 字段在所有 UC 都可见（schema 不能动态屏蔽）→ LLM 可能在 UC-A 等 handover 也填 `intake_fields={...}` | description 写明 "only required for UC-G/H/I/J/K when intake_complete_for_uc_* reason"；runtime 已有 `IntakeFieldsRegistry.isIntakeUseCase` gate，非 intake UC 持久化不会被 validator 检查 → 副作用仅是无害的 JSONB 写入；可加测试覆盖 |
| **R1.a + 同步语义指令冲突** | tool description 加入 "ensure intake_fields populated" 字样可能与 `system_prompt.txt` 现有 escalation 指令冲突 | 仅在 tool description 加 1 句不超过 30 词的引导；不动 `system_prompt.txt`（Sprint 39 后 system prompt 改动需走 prompt governance 路径） |
| **R2.a counter 与 LLM 自我节制冲突** | LLM 可能在 budget=0 时仍试图 clarify | budget 是软上限，越过即升级到 `clarification_budget_exhausted` escalation；与 §1.5 一致（不是 hardcode 而是已有 budget 契约） |
| **R2.a "DISCOVER + free-text reply 算 clarification" 识别误差** | 判定不准导致计数偏多/偏少 | 严格定义为 "phase==DISCOVER && 本 turn LLM 输出 final answer / clarification 且未 commit UC（无 classify_use_case tool call）"；不做内容判断；counter-test 覆盖 commit-UC turn 不计数 |
| **同包打两个 R-item 增加 reviewer 负担** | Codex 难一次审两套独立改动 | 各自独立测试用例 + 两个 commit 分开（schema 一个 commit、counter 一个 commit），Codex per-PR 仍可 9-question kernel 分别审 |
| **M-Auto-5 close 仍然延期** | 本 proposal 永远进不了 M-Auto-6 队列 | 本文档是 proposal-tier，永久存档；human 选择何时 promote |
| **§7 generalization coverage shadow 集不够** | 不能验证 cross-suite 不回归 | 推荐 M-Auto-5 close 后的第一次 baseline 包含完整 shadow 22 case；R1.a + R2.a sub-sprint 验收必须看 shadow 不退 |

---

# 10. Observability / trace / report implications

- **R1.a**：`intake_fields` 出现在 `request_handover.arguments` 上 → 现有 `tool_events` trace 已能展示；建议 admin trace UI 把 `intake_state` 投影连同 `required_fields_for_active_uc` 一起渲染（Cluster B 的 trace 缺口里也包含此屏需求），与 M-Auto-6 observability 包合并。
- **R2.a**：`clarification_count` 已在 projection 第 351/658 行渲染，但前端 admin trace 是否显示需 verify（参 `project_observability_debt_pattern` memory：display/trace 在 M2/M3 落后于 runtime）。建议 R2.a 同包补一个 admin UI 字段（`budgets.clarification_count` + `budgets.clarification_max`）。
- **Eval 影响**：R1.a 修后所有 intake handover 不再产生首次 reject 的 tool_event → trace 体积下降；不会影响 case_passed 判定（reject 不计入失败，只是 cosmetic）；fitness 指标可能因 step 节省略微改善（次级效应，不主推）。

---

# Appendix A — 与 human 反馈的 case 对应表

| Case | trace id 片段 | 核心错误 | 本 proposal 处置 |
|---|---|---|---|
| c1 | `e1f844cb-712...` | UC-J 4× `intake_required_fields_missing_for_intake_complete` reject；session 始终未 handover | R1.a 修复（target case） |
| c2 | `f070bf75-67a...` | UC-A `progressive_resolve_record_outcome_premature` | OBSERVATION；不修（guard 正确） |
| c3 | `d56fd1e6-510...` | DISCOVER 2 turn 输出完全相同 clarification | R2.a 修复（target case） |
| c4 | （human 误粘贴同 c3 trace） | 描述：search_knowledge 多次 paraphrase | OBSERVATION；不修（按 rank-1 anti-误杀 floor） |
| c5 | `c6f60c19-3fc...` | UC-J `intake_required_fields_missing_for_intake_complete` × 1 后第二次成功 | R1.a 修复（target case） |
| c6 | `3bd52cf6-b14...` | UC-I `intake_required_fields_missing_for_intake_complete` × 1 后第二次成功 | R1.a 修复（target case） |

---

# Appendix B — 与 deliver-agent 的交接清单

deliver-agent 若选择 promote 本 proposal 到一个 sub-sprint，需要决定的事项：

1. **打包形态**：R1.a + R2.a 同包（推荐），或 R2.a 先单独 micro-sprint？
2. **launch 时机**：M-Auto-5 close 后第一个 runtime sub-sprint？还是排在 audit Cluster B/C 2× research dispatch 之后？
3. **是否合并 Cluster C.3 "generic clarifier branch wasting opening turn"**：R2.a 与 C.3 高度重合，建议合并 R2.a = C.3 实现路径。
4. **sub-sprint id**：建议下个可用号（约 S-Auto-23 / Sprint 078）。
5. **dev prompt 模板**：参 prompt-artifact-rules §9.1-§9.6；包含 §7 stanza 完整版 + 完整的 file-path fence + anti-误杀 invariants。
6. **Codex review trigger**：semantic-touching（R1.a 投影），需 §4.1 9-question kernel；建议作为 per-sub-sprint 而非 milestone-shared（M-Auto-6 milestone 才会触发 milestone-shared）。
7. **Generalization coverage 具体 case ID**：deliver 在 §7 stanza 填入具体的 target / neighbor / negative / shadow case 名单（本 proposal 给的是模式，不是 ID）。

— 完 —
