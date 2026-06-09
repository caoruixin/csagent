# Research-agent brief — Runtime substrate hygiene (autoloop signal unblock)

> 这是一个供 human 粘贴给 research-agent 的 brief。粘贴时连同顶部的
> `@docs/teams/research-agent.md` 一起发送即可激活 research-agent 角色。
> 这不是 governed doc，是一次性的 Human → Research-agent 交接稿。

---

@docs/teams/research-agent.md

## 工作模式

**Mode 1 — Forward-looking (Path 1 research-driven)**。消费 `docs/action_bank.md`
§5.2 中已成熟的一组 R-item，产出一个**整体 substrate-hygiene 解决方案 / 架构**，
而非逐条 bug 的孤立修补。这些 R-item 本身是 Path 2 bad-case trace 分析的产物，
但你这次的任务是 Mode 1：把它们综合成一个整体设计，划分成大模块、定义模块间关系
和先后依赖，供 deliver-agent 后续切成 milestone。

## 我的 thoughts + goal

我在搭建一个 skill-driven 的 auto-evolution loop（autoloop），目标是让 agent 能
自我迭代：每个 iteration 提议一个 Skill YAML 的修改，跑 eval，按 fitness 决定是否
保留（cherry-pick）。当前最大的卡点是：**我们连第一个有意义的 cherry-pick 都拿不到。**

我原以为卡点是评估的稳定性（baseline 抖动），所以做了一系列确定性收敛（bot temp→0、
simulator temp→0、60s deadline、parallel=1）。但 2026-06-01 的调查证明：**真正的卡点
不是评估噪声，而是 customer-service agent 本身的 runtime 层有一批全局性的架构缺陷，
它们污染了 autoloop 的 fitness 信号，使任何 cherry-pick 都无法被可信地判定为"改进"。**

我的目标：**先把这批 runtime substrate 缺陷系统性地解决掉，让 autoloop 拿到一个干净的
优化信号**，然后才谈自我迭代。我需要你给我一个整体方案：这批缺陷里哪些其实是同一个
架构问题的不同表征？它们的因果/依赖关系是什么？应该分成哪几个大模块、按什么顺序修？

## 已确立的发现（请勿重做，可直接引用 / 验证）

调查脚本与原始数据在 `/tmp/badcases3pass/`（`trace_audit.py`、`suite_compare.py`、
`analyze.py` + 三个 suite 各 3 passes 的 results.json，run-id 见下）。结论：

1. **7 类全局 runtime 缺陷**（详见 `docs/action_bank.md` §5.2
   "Sprint 065 / S-Auto-10 bad_cases trace-dive surfaced R-items (2026-06-01)"）。
   在 8 个 flipping bad_case × 3 passes = 24 个执行单元中的出现频次：
   - `R-runtime-identical-tool-call-retry-storm`（15/24）— 同一 turn 内同一 tool +
     完全相同 args 重复调用 2–6 次；runtime 不去重。
   - `R-runtime-paraphrase-storm-search-knowledge`（11/24）— 同一 turn 调
     `search_knowledge` ≥3 次，换不同 query 措辞。
   - `R-classifier-non-deterministic-uc-selection-at-temp-zero`（8/24，其中 2 个是
     真随机；其余为 by-design 误分类恢复测试）— `classify_use_case` 在 temp=0 下同一
     case 跨 rerun 给出不同 UC。
   - `R-runtime-escalation-reason-misstamp-maxsteps-faq`（5/24）— max_steps 耗尽时
     resolver 把 escalation_reason 错标为 `faq_miss_threshold_exceeded`，即使最近一次
     `search_knowledge.faq_miss=false` 且有 grounded hits。延续你 memory 里的
     `project_faq_overescalate_maxsteps_misstamp`；Sprint 39 guardrail 只关了 LLM 自标
     vector，runtime resolver 路径仍在 mis-stamp。
   - `R-runtime-escalation-reason-turn-budget-conflated-with-intent`（4/24）—
     `turn_budget_exhausted` 触发的 escalation 被 case_spec 当作和 LLM-intent escalation
     等价。
   - `R-runtime-tool-gating-race-uc-none`（3/24 + 用户实际 session `6f9645dc...`）—
     `active_use_case='none'`（classify 尚未 commit）时，非-classify tool 被允许先执行 →
     `Tool 'search_knowledge' is not allowed for use case 'none'` 错误。
   - `R-simulator-first-message-contract-violation-flake`（3/24）— moonshot simulator
     首条 message 偶发违约（turns=0, stop=contract_violation），temp=0 仍发生。

2. **关键因果簇假设**：上面 1/2/4/5/6 很可能是**同一条因果链**的不同表征：
   `gating-race`（classify 未 commit 就调工具，浪费一步）→ `retry-storm` /
   `paraphrase-storm`（工具反复无效调用，吃满 step 预算）→ `max-steps 耗尽` →
   `escalation-reason mis-stamp`（resolver 把耗尽错标成 faq/ turn-budget 语义）。
   请你在 code-grounded survey 中验证或推翻这条链，并据此判断"修哪一个能上游切断
   下游多个症状"。

3. **这批缺陷是全局的，不是 bad_cases 独有**（用 `suite_compare.py` 跨三个 suite 验证）：

   | | bad_cases | anchor_outcome | shadow |
   |---|---|---|---|
   | pass-count range（3 passes） | 2 | 0 | 1 |
   | turns/case median | 3.0 | 4.0 | 3.0 |
   | STORM 出现率 | 47% | 36% | 33% |
   | max-steps escalation 率 | 36% | **50%** | 27% |
   | turn0 contract-viol 率 | 8% | 11% | **26%** |

   anchor/shadow 命中这些 bug 的频率不低于（甚至高于）bad_cases。它们 range 小**不是因为
   case 简单**，而是因为：anchor 的 `expected_outcome="either"`（resolve/escalate 都算过）
   的 case 对路径混乱免疫——无论 bot 是干净解决还是 storm→max-steps→escalate，终态都落在
   接受窗口内（已逐 case 验证：uc_d/uc_e/uc_f/uc_fp 都是走 max-steps 混乱路径但 passed=True）；
   anchor 的 `expected="escalate"` case 则**确定性地错**（每次都做错误类型的 escalation）。
   shadow 则被钉在地板（1-2/22 pass）。bad_cases 之所以抖动，是因为它**被刻意 curate 成
   坐在 pass/fail 判别边界上**——同样的全局混乱，anchor 吸收、shadow 触底、bad_cases 测量。

4. **战略结论**：在这批缺陷未修前，autoloop 在**任何** gate 上都无法产出有意义的
   first cherry-pick：(a) bad_cases gate 被全局 bug 噪声主导；(b) anchor gate 对改进**盲**
   （`either` case 吸收任何路径质量改进，`escalate` case 需要的是 runtime 修复而非
   autoloop 能改的 skill YAML）；(c) shadow gate 只是触底回归守卫。**binding constraint
   是这 7 类 substrate 缺陷本身。**

## autoloop 的可变面（用于判断 fix 归属）

autoloop 当前唯一能改的面是 **Skill YAML 的 LLM-soft 字段**：`$.procedure`、
`$.grounding_instruction`、`$.escalation_policy`、`$.critical_steps[*].desc`
（见 `docs/proposals/autoloop_design.md`）。因此请在方案里**明确区分**每个 fix 的归属：

- **runtime-layer fix**（Java / dispatcher / resolver）— autoloop **改不到**，必须走
  research → deliver → dev/review 正常交付。这是这批缺陷的大多数。
- **skill-layer fix**（Skill YAML 软字段）— autoloop **将来能改**，但现在不应靠它来修
  （信号还没干净）。例如 paraphrase-storm 的路径 (2) 是 skill prompt 约束。
- **eval_spec / harness fix**（case_spec schema / simulator robustness）— 配套修复。

## 核心问题（你的 proposal 必须回答）

1. **因果簇验证**：上面第 2 点的因果链假设是否成立？code-grounded（在 HEAD 验证
   `EscalationReasonResolver` / `ToolDispatcher` / `ToolPolicyEnforcer` /
   `ClassifyUseCaseTool` / `AgentRunLoopImpl` 的实际行为）。哪个上游修复能切断最多下游症状？
2. **模块划分**：这 7 类缺陷应组织成哪几个大模块（例如"工具调用纪律"、"escalation 语义
   一致性"、"intake/classify 时序"、"eval harness 鲁棒性"）？模块间的关系和先后依赖？
3. **顺序约束（compounding-effect）**：哪个必须先修？错误顺序会导致什么更糟的结果？
   （例：若先调 case_spec 接受 turn_budget escalation，会不会反而掩盖 retry-storm 的真实
   症状？）
4. **LLM-first 检验**：每个 runtime fix 是否守住 Constitution §1.5 / §1.7——是用确定性
   kernel invariant（去重、时序、语义一致性）解决，还是滑向 keyword/regex/enum 的语义
   hardcode？哪些"看似 runtime"的其实应推回 prompt_projection / semantic_planner？
5. **autoloop 解锁判据**：修完哪个最小模块子集后，autoloop 的 fitness 信号就足够干净到
   能产出可信的 first cherry-pick？给出可观测的验证条件（例如"bad_cases 3-pass 后
   IDENTICAL_RETRY 从 15/24 降到 ≤2/24 且 per-case turn 数收敛"）。

## 约束（Constitution §1）

- **LLM-first / anti-hardcode**：不要用 keyword/regex/if-else/enum expansion 解决语义
  失败（除非破坏 Tier-0 invariant，见 §1.5）。runtime 修复应是确定性 kernel invariant
  （per-turn 去重、classify-then-tool 时序、escalation-reason 与观测事实一致），不是
  语义硬编码。
- **Forward-looking**：按 6 个月后主流 LLM 能力设计；不要为当前 LLM 局限做过度 workaround。
- **不引入 seed-based 厂商级控制**：我已从 action_bank 移除所有 "是否接受 seed" 路径——
  现阶段不做这么细腻的模型级确定性控制。请勿在方案里把 vendor `seed` 当作推荐路径。
- **不动 autoloop 可变面以外的范围去"优化 eval 数字"**；目标是修 substrate，不是刷分。

## Codebase pointers（起点；你需在 HEAD 逐一验证）

- `server/.../service/runtime/EscalationReasonResolver.java` — escalation reason 解析
  （ESCALATION_MISSTAMP + TURN_BUDGET 因果簇下游）
- `server/.../service/tools/ToolDispatcher.java` — 工具分发 + "not allowed for use case"
  门控（GATING_RACE）
- `server/.../service/tools/ToolPolicyEnforcer.java` — 工具权限门控
- `server/.../service/tools/ClassifyUseCaseTool.java` + `model/BotSession.java`
  `activeUseCase` — classify 时序 + 分类稳定性
- `server/.../service/runtime/AgentRunLoopImpl.java` — agent run loop（max_steps、
  retry-storm 宿主、per-turn 去重的可能落点）
- `server/.../service/runtime/skill/SkillGuardrailDispatcher.java` — skill 软字段边界
  （autoloop 可变面与 runtime 的交界）
- `server/.../model/LlmRequest.java:19` + `service/runtime/LlmInvocationService.java:114,267`
  — bot temperature=0（已设）
- `eval_interactive/eval_interactive/simulator/user_simulator.py:107-110,158-189`
  — simulator 首条 message 逻辑 + `_parse_simulator_response` + retry（CONTRACT_VIOL_TURN0）
- `docs/action_bank.md` §5.2（2026-06-01 trace-dive 7 条 + 2026-05-31 既有 3 条
  `R-autoloop-run-sweeps-dirty-index` / `R-overnight-eval-traces-not-persisted` /
  `R-tier1-bad-cases-regression-5-to-0-attribution-unverified`，注意做 coverage check）

调查数据（只读，run-id）：
- bad_cases sim-temp0 3 passes：`eval_interactive/results/20260601-070530` `071402` `071943`
- anchor_outcome 3 passes：`20260601-014715` `020027` `021440`
- shadow 3 passes：`20260601-014942` `020347` `021710`
- 分析脚本：`/tmp/badcases3pass/{trace_audit,suite_compare,analyze}.py`

## 期望产出

`docs/solutions/<descriptive_name>.md`，按 research-agent.md Mode-1 格式：
executive summary / current-state survey (code-grounded) / gap (因果簇验证) /
design alternatives + trade-offs / recommended option / **scope split（大模块 + 顺序
依赖；具体 milestone 拆分留给 deliver-agent）** / layer classification per §3.2 +
§7 stanza pre-fill（逐模块）/ hard fences + non-goals / risk + compounding-effect /
observability implications（注意与既有 `R-overnight-eval-traces-not-persisted` /
`R-tier1-...-attribution-unverified` 的配套关系）。

## Hard fences / non-goals

- 不替 deliver-agent 做最终 milestone/sprint 拆分（scope split 是建议）。
- 不写业务代码（Don't update code）；这是 proposal。
- 不推荐 vendor `seed` 路径（已显式移除）。
- 不要把 bad_cases 的 §5.6 human-judgment primary-gate 性质降级成 autoloop 程序化 gate；
  bad_cases 仍是人工判断面。
- 不要在本轮就建议拓宽 autoloop 的可变面（mutable surface）——先修 substrate，
  mutable-surface 拓宽是 substrate 干净之后的独立决策（per S-Auto-10 §11 / M-Auto-2 §12
  Stage-2 方向）。
