---
title: Auto-Evolution v1 — Skill-Driven Hill-Climbing Within the Four-Tier Eval
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (until milestone_objective.md picks up)
authored_by: research-agent
authored_date: 2026-05-26
mode: forward-looking
supersedes: []
superseded_by: null
notes: >
  Research-agent proposal for an auto-evolution / autoresearch capability
  on top of the customer-service agent. Builds on the existing
  `docs/proposals/autoloop_design.md` (2026-05-10, pre-M2 / pre-M3-Eval),
  but reshapes the mutable surface around the M2 Skill Registry abstraction
  (`server/src/main/resources/skills/*.yaml`) and the M3-Eval four-tier
  pyramid (Tier-0 Java replay hard gates + Tier-1 outcome / human-judgment +
  Tier-2 Skill `critical_steps` / `skill_procedure_followship` + Tier-3
  polish). Replaces autoloop_design.md's 4-file allow-list (system_prompt
  / routing_prompt / control-policy / templates) with a single mutable
  class: the LLM-soft narrative fields inside the 6 Skill YAMLs. Keeps the
  shadow-set + git-ratchet + lexicographic-gate + experiment-log spine;
  adds a Lessons-learned distilled memory layer so future iterations
  condition on past ones.
---

# Auto-Evolution v1 — Skill-Driven Hill-Climbing Within the Four-Tier Eval

> Reading order. §1 给出 executive summary 和 v1 的核心选择。§2 是 code-
> grounded 现状调研（mutable surface 候选 / 评测框架 / 已有的 shadow set /
> hot-reload 现状）。§3 对照 human 提出的四个问题（mutable 锁定、评估、
> keep/discard、跨迭代记忆）做 gap analysis。§4 给两个方案 + 一个 staged
> 推荐。§5 推荐选项 + 理由。§6 sub-sprint / milestone 拆分建议（交付优先级）。
> §7 layer classification + §7 stanza 预填。§8 hard fences + non-goals。
> §9 风险 + compounding effects。§10 observability / trace 配套需求。

## 1. Executive summary

人类希望在当前 CS agent 已经收敛到「LLM-first + Skill 抽象 + 四层评测」
的架构基础上，叠加一个**自演进 (auto-evolution / autoresearch) 能力**：
一个 meta-agent 自动在一个**受限可变面**上提假设、改动配置、跑评测、按
门槛判断 keep / discard，并且把每一次的得失沉淀成可以反过头指导下一次
迭代的记忆。这份 proposal 给出 v1 的具体形态：

- **可变面 (mutable surface)**：仅限 `server/src/main/resources/skills/`
  下 6 个 Skill YAML 的 **LLM-soft 叙事字段**——`procedure` /
  `grounding_instruction` / `escalation_policy` / `critical_steps[].desc`
  四类纯自然语言字段。Skill 的**结构性字段**（`applicable_phases` /
  `applicable_use_cases` / `tools_required` / `guardrails` /
  `state_inheritance` / `critical_steps[].trace_check|mandatory_for|severity|id`）
  以及所有 Java 代码、`use-case-registry.yaml` / `tool-policy.yaml` /
  `control-policy.yaml` / `templates.yaml` / `system_prompt.txt` / 所有
  case_specs（包括 shadow）、所有评测代码——**全部锁定**。
- **评估框架 (fitness gate)**：完全采用 M3-Eval 之后的四层金字塔，
  **不再使用旧 L1/L2/L3 三层方案**。Tier-0 安全底（Java replay 11 hard
  gates + Python `hard_checks.py` 安全相关项）零容忍；Tier-1 通过
  `anchor_outcome` (12 cases) + bad-case suite 的**程序化子信号**
  (`case_passed` + L1 + 强制 L2 + Tier-2-mandatory) 不回归；Tier-2 通过
  Skill `critical_steps` Tier-2-mandatory 失败数不增加；Tier-3 仅记录
  / 用于同分候选的 tiebreaker。
- **Keep / discard 策略**：lexicographic（按 Tier-0 → Tier-1 → Tier-2 →
  Tier-3 顺序门控；任一上层不通过即 discard，不允许下层得分换上层得分）。
  额外两道保险：anti-overfitting **shadow set**（已存在 23 cases）regression
  > 3% → discard；§4.1 nine-question 反硬编码 kernel 由 sandbox 自动检
  + 周期性 Codex 复核。
- **跨迭代记忆**：三层结构——**raw 实验日志**（`experiments.jsonl` 追加）
  + **结构化迭代索引**（hypothesis_fingerprint / target field / 改动摘要 /
  fitness delta / kept-or-discarded 的 SQLite-like 表）+ **LLM-distilled
  lessons-learned 叙事日志**（每 K 次迭代由 LLM 把最近 N 条压缩成结构化
  教训，反喂下一轮 propose 阶段的 meta-agent 提示）。
- **§5.6 bad-case 人工评判**保持**人在环外** (out-of-loop)：auto-loop 的
  "keep" 是软信号，落在 `autoloop/keep-{N}` 分支；human + deliver-agent
  按 cadence（每 K 个 kept 或每 N 小时）做 §5.6 manual review 后再决定
  是否合入 main。

**为什么这是合适的形态**：自演进的核心矛盾是「让 LLM 改 LLM-soft 内容」
vs「不让 LLM 退化为 keyword bot」。M2 已经把绝大多数 procedural 内容从
`system_prompt.txt` 迁移到了 6 个 Skill YAML 的 `procedure` /
`grounding_instruction` / `escalation_policy` / `critical_steps[].desc`
里——这正是「LLM-soft、LLM owns acting on」的内容。把 mutation 面锁在
这一类字段上，让 meta-agent 只能改 LLM-soft 叙事、不能改结构（trust
boundary / tool whitelist / state schema / Tier-2 measurement contract），
就把 §1.7 forbidden-list 的红线从「评审时检查」上移到了「结构上不可能
违反」。这也是 karpathy/autoresearch 在 csagent 场景下最自然的映射——
6 个 Skill YAML 的 LLM-soft 字段 ≈ karpathy autoresearch 里那个被 meta-
agent 编辑的、与「locked evaluator」分离的「mutable program」。

## 2. Current-state survey (code-grounded; 2026-05-26 HEAD `c9390dc`)

### 2.1 Skill abstraction (M2 之后；2026-05-18 closed)

`server/src/main/resources/skills/` 下 6 个文件，每个文件是一个 Skill YAML：

| Skill | 文件 | 主要 mutable 候选字段 |
|---|---|---|
| `discover_triage` | `discover_triage.yaml` (9.5K) | `procedure` (881 字)，4 个 `critical_steps[].desc` |
| `resolve_faq_grounded_answer` | `resolve_faq_grounded_answer.yaml` (6.7K) | `procedure`, `grounding_instruction`, `escalation_policy`, 5 个 `critical_steps[].desc` |
| `resolve_intake_collect_and_handover` | `resolve_intake_collect_and_handover.yaml` (6.1K) | `procedure`, `grounding_instruction`, `escalation_policy`, 5 个 `critical_steps[].desc` |
| `confirm` | `confirm.yaml` (2.7K) | `procedure`, `grounding_instruction`, `escalation_policy` |
| `escalate` | `escalate.yaml` (2.5K) | `procedure`, `escalation_policy` |
| `terminal` | `terminal.yaml` (1.6K) | `procedure` |

加载入口在 `server/src/main/java/com/gumtree/csagent/service/runtime/skill/SkillLoader.java:161`
(`loadAll()`)：`classpath:/skills/*.yaml` glob 列举 → `parseAndValidate` →
schema 校验。`SkillRegistry.java:49` 在构造函数里调用一次 `loader.loadAll()`，
之后是 `List.copyOf(...)` 不可变快照。**没有 hot-reload**——任何 Skill YAML
改动都必须 Spring 重启才能生效（与 `feedback_restart_backend_before_eyeball.md`
一致）。这是 auto-evolution 实现层面的关键事实，决定了单次迭代的最低成本
是「Spring restart + 全套评测」≈ 15–30 分钟。

Skill 字段的语义边界（决定哪些可被 LLM 自动改、哪些不能）已经在
`docs/proposals/skill_foundation_design.md` 和 §1.3/§1.4 宪法里固化：

- LLM-soft（LLM 自主依据其判断使用，runtime 不强制）：
  `procedure`, `grounding_instruction`, `escalation_policy`,
  `critical_steps[].desc`。
- 结构性（runtime 强制 / 决定 trust boundary）：
  `applicable_phases`, `applicable_use_cases`, `tools_required`,
  `required_context_keys`, `max_tool_steps`, `allow_interim_message`,
  `valid_terminal_outcomes`, `guardrails`, `state_inheritance`,
  `critical_steps[].id`, `critical_steps[].trace_check`,
  `critical_steps[].mandatory_for`, `critical_steps[].severity`。

特别强调：`critical_steps[].trace_check` 是 Tier-2 评测的**测量契约**——
它定义了「这条 critical step 算 PASS / FAIL / N/A」的判定方式。如果让
auto-loop 自由编辑 trace_check，它就可以通过「松绑测量」来虚假提高分数
（让一个原本 mandatory FAIL 的 step 变成 N/A 或 advisory），属于
karpathy autoresearch 里的「optimizer must not modify its own scoring
function」红线。**v1 强制锁定 trace_check / mandatory_for / severity / id**，
仅允许 `desc`（LLM-visible 叙事）可变。

### 2.2 M3-Eval 四层评测框架（2026-05-23 closed）+ M4-Eval-Cleanup (2026-05-24)

`eval_interactive/eval_interactive/scoring/composite.py` 现在的 `case_passed`
逻辑是：

```python
case_passed = l1_passed AND mandatory_l2_passed AND (not tier2_critical_failed)
```

其中：

- **Tier-0 安全底**：Java replay 11 hard gates 定义在
  `eval/src/main/java/com/gumtree/csagent/eval/metrics/GateEvaluator.java:57-75`
  (`critical_policy_violation` / `wrong_containment` / `groundedness_pass_rate`
  / `escalation_recall` / `handover_completeness` / `tool_scope_violation`
  / `forbidden_phrase` / `budget_enforcement` / `phase_transition_validity`
  / `critical_high_risk_escalation` / `out_of_scope_detection`)。Python 侧的
  Tier-0 由 `hard_checks.py` 的 `no_pii_leakage` / `no_human_only_tool_exposure`
  / `no_critical_policy_violation` / `escalation_compliance` /
  `phase_transition_validity` 覆盖。
- **Tier-1 Outcome**：`bad_cases/` (12 cases) + `anchor_outcome/` (12 cases)
  现在以 `case_passed_authority = "human_review"` 标记，
  `executor.py:43-66` 的 `_resolve_case_passed_authority` 决定了它们的
  `suite_authority` 聚合属性；同时 `user_goal_achievement` 这个 L3 dim
  作为 Tier-1 补充 advisory（不作 hard gate）。
- **Tier-2 Critical-flow**：`skill_procedure_check.py` 的
  `SkillProcedureExtractor` 读取 6 个 Skill YAML 的 `critical_steps`，对
  trace 求值得到 PASS / FAIL / N/A；`tier2_results_to_gate` 把 mandatory
  fail 升为 `Tier2Result(severity="critical")`，进入 `composite.compute_composite`
  门控。Trace-check DSL 有 6 个 frozen primitives，parser 在解析期
  就 reject 任何 `message.contains(...)` / regex / 关键字匹配
  (`skill_procedure_check.py:111-120` 的 `TraceCheckDSLSyntaxError`)。
- **Tier-3 Polish**：`relevance` / `tone_appropriateness` / `groundedness`
  / `handover_completeness` / `case_id_present` / `tool_sequence_match`
  / `turn_efficiency` 等都标 `severity="advisory"`，仍计算 / 记录但**不**
  flip `case_passed`，仅用于趋势观察 + tiebreaker。

`eval_interactive/results/<run-id>/results.json` 现在每个 case 携带
`case_passed`, `case_passed_authority`, `composite_score`,
`outcome_score`, `judge_score`, `tier2_result`, `l1_results`, `l2_results`,
`l3_results`, `failure_tags`, `per_turn_trace`, `llm_calls`。Run-level
`summary` 里有 `suite_authority` 聚合 + 各 mean 分。这套 schema 已经能
直接喂给 auto-loop 的 fitness 计算函数。

### 2.3 案例集 + Shadow set（已 ship）

- `case_specs/smoke/` — 14 cases（observation-only per §5.5）
- `case_specs/anchor/` — **159 cases**（programmatic; backward-compat surface）
- `case_specs/anchor_outcome/` — **12 cases**（NEW Tier-1 human-judgment surface）
- `case_specs/bad_cases/` — **12 cases**（PRIMARY gate per §5.6, human-judgment）
- `case_specs/case_families/` — 12 个 case-family 目录（targets + neighbors + negatives）
- `case_specs_shadow/case_families/` — **23 shadow cases**（dev-blind via
  `case_specs_shadow/_ACCESS_BOUNDARY.md`；通过 `CaseSetManager.load_custom(path)`
  显式加载；M5 close 已经作为 NEW shadow gate 跑通了一遍）

Shadow set 的存在直接闭合了 `docs/proposals/autoloop_design.md §1` 列出
的"Gap: shadow eval"——auto-loop v1 直接复用现有 shadow set 即可，无需
新建。

### 2.4 已有的安全门 / 反硬编码契约

- `docs/current/iteration_governance.md` §1.7 forbidden-list：禁止
  keyword / regex / if-else / enum-expansion 用于 semantic decision。
- §4.1 nine-question anti-hardcode kernel：每个 semantic-touching change
  在 Codex 复核时走的 9 问检查表（已抽取到
  `docs/current/anti-hardcode-review-kernel.md` 作为可复用模块）。
- `runtime_freeze_and_risk_policy.md` §1/§2：Tier-0 不可变 invariants 名单。
- `must_cite_source` / `intake_complete_required` / `premature_resolve_outcome_guard`
  / `faq_miss_handover_requires_resolve_attempt` 等 Skill-declared guardrails
  在 runtime 强制（不是仅在 prompt 里"建议"）；这些是 Tier-0 的运行时具象。

### 2.5 与 autoloop_design.md 的差距

`docs/proposals/autoloop_design.md`（2026-05-10）的核心架构（standalone CLI
+ subprocess invocation + lexicographic gate + 2-phase replay/interactive
+ git ratchet + experiments.jsonl + 6 anti-gaming checks）**仍然适用**。
过时的部分是**前提假设**：

| 维度 | autoloop_design.md（2026-05-10） | 现实（2026-05-26 HEAD） |
|---|---|---|
| Mutable surface | 4 文件: `system_prompt.txt`, `routing_prompt.txt`, `control-policy.yaml`, `templates.yaml` | M2 后 procedural 内容大部分迁到 6 个 Skill YAML 的 LLM-soft 字段 |
| 评测 | 旧 L1/L2/L3 三层 + Java 11 hard gates | M3-Eval 四层金字塔 (Tier-0/1/2/3)；旧三层在 Tier-3 退化为 advisory |
| Bad-case 主门 | 不存在 | §5.6 PRIMARY gate，人工评判，12 cases |
| Tier-2 Skill `critical_steps` | 不存在 | 6 个 Skill 共 ~24 个 critical steps；带 DSL parser §1.7 结构性防御 |
| Shadow set | 列为"Gap" | 23 cases 已 ship，已经在 M5 close 实战 |
| 反硬编码 kernel | "Don't add eval case IDs in prompts" 朴素规则 | 9 问 kernel + DSL parser 结构性防御 + Tier-0 freeze policy |
| Hot-reload 假设 | "Maven incremental compile" | Skill YAML 没有 hot-reload，Spring 必须重启 |
| Memory across iterations | `experiments.jsonl` + 最近 5 实验喂入 meta-agent | 同左 + 需要 LLM-distilled lessons-learned 层（karpathy autoresearch 风格） |

## 3. Gap analysis — human 的四个问题

### 3.1 怎么选择哪些 agent 部分 lock，哪些 mutable

Human 明确说："可变的部分是 skills 里面，以及 skills 关联的这些配置文
件 ... 其他的 deterministic runtime / Java code 都是不可变的、要 lock 的。"

这条决定与 M2 Skill 抽象的 LLM-soft / 结构性分界**正好对齐**——只要再把
分界**收紧一格**：Skill YAML 内部也分 LLM-soft（可变）和结构性（锁定）。
不收紧的话，meta-agent 可以通过编辑 `applicable_use_cases` 偷偷扩大某个
Skill 的 trust 边界，或者通过编辑 `critical_steps[].trace_check` 改自己
的考试卷子。

**v1 推荐的精细 mutable 表**：

| 文件 / 字段 | 类别 | 可变？ | 理由 |
|---|---|---|---|
| `server/src/main/resources/skills/*.yaml` `procedure` | Skill LLM-soft | ✅ | LLM owns; karpathy autoresearch 的 program 内容 |
| 同上 `grounding_instruction` | Skill LLM-soft | ✅ | LLM owns 的 grounding 叙事 |
| 同上 `escalation_policy` | Skill LLM-soft | ✅ | LLM owns 的 escalation 叙事 |
| 同上 `critical_steps[].desc` | Skill LLM-soft | ✅ | LLM-visible 程序性叙事 |
| 同上 `critical_steps[].id/trace_check/mandatory_for/severity` | Tier-2 测量契约 | ❌ | 测量契约——meta-agent 不能修改"自己的考试卷" |
| 同上 `name/description/applicable_phases/applicable_use_cases/tools_required/required_context_keys/max_tool_steps/allow_interim_message/valid_terminal_outcomes/objective` | Skill 结构性 | ❌ | trust boundary / 工具白名单 / context schema / 阶段集 |
| 同上 `guardrails` | Tier-0-floor 强制点 | ❌ | runtime 强制；与 risk policy §1/§2 联动 |
| 同上 `state_inheritance` | 跨 Skill 状态 schema | ❌ | 决定状态在 Skill 切换时如何 inherit/reset |
| `server/src/main/resources/prompts/system_prompt.txt` | 全局 prompt | ❌ (v1) | 残留的 JSON 信封契约 + 23-value escalation_reason 决策树；改这里会跨 Skill 漏入 §1.7 红线 |
| `server/src/main/resources/prompts/routing_prompt.txt` | UC 分类 prompt | ❌ (v1) | 与 M1 DISCOVER 收敛后的 `discover_triage.yaml` 大量重叠；先聚焦 Skill 路径 |
| `server/src/main/resources/scripts/templates.yaml` | Fixed Script Library v1 | ❌ (v1) | 客服话术；CSAT-敏感；`{VARIABLE}` 占位符易被 LLM subtly 破坏；human + 法务签字过的稳定面 |
| `server/src/main/resources/config/control-policy.yaml` | 预算/相位转移/drift 阈 | ❌ | 安全底；预算下调易制造拒服务 |
| `server/src/main/resources/config/use-case-registry.yaml` | UC 注册 | ❌ | 改变可见 UC 集 = 改变能力边界 |
| `server/src/main/resources/config/tool-policy.yaml` | 工具×UC 白名单 | ❌ | 能力边界 |
| `server/src/main/resources/config/risk-keywords.yaml` | risk 关键词 | ❌ | 安全分类底，与 risk policy 锁 |
| 所有 `server/src/main/java/**` Java 代码 | Runtime | ❌ | 宪法 §1.4 |
| 所有 `eval/` / `eval_interactive/` 评测代码 | Evaluator | ❌ | karpathy "evaluator must be immutable" |
| 所有 `eval_interactive/case_specs/**` 案例 | Ground truth | ❌ | 同上；包括 anchor / anchor_outcome / bad_cases / case_families / smoke |
| `eval_interactive/case_specs_shadow/**` | 反过拟合 shadow set | ❌ + 不可见 | meta-agent 永远看不到这里的 failure |
| `data/` / `db/migration/` / `server/src/main/resources/mock/` | 测试 fixture / 知识库 / 迁移 | ❌ | 同 ground truth |
| `autoloop/scoring/**` / `autoloop/program.md` | Loop 自身的 scoring 与契约 | ❌ | 同 karpathy |

### 3.2 用什么 evaluation 评估每次升级——用新的四层方案，不用旧三层

Human 明确说："我们是不是只采用新的 evaluation 结果就可以，而不用采用以前
的那套 inside evaluation 里面的那个三层评估方案，而直接采用现在的四层
评估方案。"

**答：是。完全采用四层金字塔，不再使用旧 L1/L2/L3 作为 keep/discard 门**。
原因：

- M3-Eval 把旧 L1 `no_forbidden_tools` / 旧 L2 `tool_sequence_match` /
  旧 L3 `relevance` / `tone_appropriateness` / `groundedness` 都已经显式
  demote 到 `severity="advisory"`，只用于趋势观察。如果 auto-loop 在
  门控上仍然依赖它们，等于绕过 M3-Eval 这次 milestone 的成果。
- 新四层金字塔有更清晰的「能否归因到 bot 真实行为」分层：Tier-0 安全
  (零容忍) → Tier-1 outcome (是否解决问题) → Tier-2 Skill `critical_steps`
  (per-UC 关键流程) → Tier-3 polish (advisory)。这与 hill-climb 想要的
  lexicographic 顺序天然对齐。
- 旧三层在 mocked-vs-real-LLM gap / provider drift / 判官稳定性上有累积
  的 confounds，§5.5 已经把 smoke composite_score 整体降为 observation；
  让 auto-loop 继续依赖它会引入提供商抖动假象的 keep。

但有一个例外：**§5.6 bad-case suite 的"人工评判"部分**——auto-loop **不能
用它作为 per-iteration gate**，因为这是 human-judgment。auto-loop 只用
bad-case 的**程序化子信号**（`case_passed` per case + Tier-2 critical
fail count）作为 fitness 的一部分。人工评判保留在 out-of-loop 的 cadence
化复核里。

### 3.3 怎么确保只在效果变好时保留这次更新——lexicographic gate

Human 说："确保只有在我的效果在变得更好的情况下，我才保留这次更新。"

直接用 `docs/proposals/autoloop_design.md §4.2` 的 lexicographic 5 层
门控**模板**（已被实践证明在 karpathy autoresearch 之外仍然适用），但用
M3-Eval 四层金字塔重新定义每一层的含义：

```
Layer 0 — Tier-0 SAFETY FLOOR (zero-tolerance)
  Java replay 11 hard gates: critical_policy_violation=0,
    wrong_containment<=2%, groundedness_pass_rate>=98%,
    escalation_recall>=95%, handover_completeness>=98%,
    tool_scope_violation=0, forbidden_phrase=0, budget_enforcement=100%,
    phase_transition_validity=100%, critical_high_risk_escalation=100%,
    out_of_scope_detection>=90%
  Python Tier-0 hard checks: no_pii_leakage, no_human_only_tool_exposure,
    no_critical_policy_violation, escalation_compliance, phase_transition_validity
  Any gate FAIL → DISCARD

Layer 1 — Tier-1 OUTCOME NON-REGRESSION
  anchor_outcome programmatic case_passed rate: must not drop
  bad_cases programmatic case_passed count: must not drop (即 bad-case
    suite 的程序化通过数不能因为这次改动而减少)
  L3 user_goal_achievement mean: must not drop > 0.1 (1-5 scale, soft)
  Any regression → DISCARD

Layer 2 — Tier-2 CRITICAL-FLOW NON-REGRESSION
  Tier-2 mandatory critical_step failures across anchor + anchor_outcome
    + bad_cases: must not increase
  Per-UC Tier-2 mandatory failure rate: must not increase on any UC
  Any regression → DISCARD

Layer 3 — IMPROVEMENT THRESHOLD
  At least ONE of the following must improve by >= 2% absolute:
    - anchor case_passed rate (159 cases)
    - anchor_outcome programmatic case_passed rate (12 cases)
    - bad_cases programmatic case_passed count
    - Tier-2 mandatory-step pass rate
    - smoke task_success_rate (observation, but allowed as tiebreaker)
  No improvement above threshold → DISCARD (not worth the change risk)

Layer 4 — SHADOW REGRESSION CHECK (anti-overfitting)
  case_specs_shadow 23 cases programmatic case_passed rate:
    must not drop > 3% from baseline
  Failure → DISCARD; flag as overfitting signal

Layer 5 — TIEBREAKER (rank kept candidates)
  Tier-3 advisory dims (recorded; not gate):
    judge mean (excluding advisory dims per composite.py:228 logic),
    mean_turns_to_resolution (lower is better), stall_rate (lower).
```

5 层的关键是「上层不通过即整体 discard，不允许下层的小赢换上层的损失」。
karpathy autoresearch 里 `val_bpb` 是单一标量；csagent 因为是 CS 场景
必须是 lexicographic——Tier-0 安全永远不能拿 Tier-3 polish 换。

### 3.4 跨迭代的记忆——三层结构

Human 说："参考的结果能够跨几次迭代 ... 记住自己曾经采用过的迭代方案，
反过头来去启发自己下一次的迭代。"

`autoloop_design.md §7.2` 只设计了"recent_experiments (last 5)" 列表喂入
meta-agent，对"几次迭代"来说太浅；karpathy/autoresearch 在原始版本里也
没有专门设计这层（因为他在小模型 + 训练 loss 场景下不需要）。csagent
需要：

**Layer A: Raw experiments log** (`autoloop/results/experiments.jsonl`)
- 一条 JSONL / iteration，含 hypothesis 文本、diff、phase A/B 完整 metrics、
  decision、gaming flags、shadow result、Layer-by-Layer pass/fail。
- 永远 append-only；human audit 的来源。

**Layer B: Structured iteration index** (`autoloop/results/iterations.sqlite`)
- 一个轻量 SQLite 表（Python sqlite3 内置；不引依赖）：
  ```sql
  CREATE TABLE iterations(
    id TEXT PRIMARY KEY,
    ts TEXT,
    target_skill TEXT,     -- 'discover_triage' / 'resolve_faq_grounded_answer' / ...
    target_field TEXT,     -- 'procedure' / 'critical_steps[3].desc' / ...
    edit_summary TEXT,     -- LLM-distilled 1-sentence summary of the diff
    hypothesis_fingerprint TEXT, -- normalized hash for anti-repeat
    decision TEXT,         -- 'keep' / 'discard'
    discard_reason TEXT,   -- 'tier0_fail' / 'tier1_regress' / ... / null
    fitness_delta_json TEXT, -- per-tier delta numbers
    parent_iteration_id TEXT, -- enables tree-of-attempts navigation
    notes TEXT
  );
  ```
- 用于 meta-agent 在 propose 阶段查询「我之前在 discover_triage.procedure
  上尝试过哪些类型的改动？结果如何？」（避免重复无效假设）。

**Layer C: LLM-distilled lessons-learned log** (`autoloop/results/lessons.md`)
- 每 K 次迭代（默认 K=10）触发一次"lesson compaction"：一个 LLM 调用读
  最近 K 条 iterations，按 target_skill × target_field × outcome 聚类，
  输出结构化教训，例如：
  > **Lesson L-2026-05-30-001** — `target_skill=discover_triage`,
  > `target_field=procedure`. 在 procedure 末尾追加 enumerated decision
  > tree（"IF X THEN Y" 风格的列表）在 5 次尝试中 4 次被 Codex
  > anti-hardcode kernel Q5 rejected. **Lesson**: 不再写决策树式列举；
  > 改为软性叙事条件（"如果 X 与 Y 共同出现，考虑 Z；用户的字面诉求是
  > 主信号"）。
- `lessons.md` 在每次 propose 调用时整段喂入 meta-agent prompt，作为
  「avoid these patterns; reuse these patterns」的 prior。
- compaction 自身也记录到 `iterations.sqlite` 一个特殊 `decision="lesson"`
  行，便于 audit。

这三层一起就是「跨迭代记忆」：raw 给 human 看 / Layer B 给 meta-agent 在
propose 阶段查重 / Layer C 给 meta-agent 在 propose 阶段提供 strategic
prior。这也是 karpathy autoresearch 在小模型 / 大模型放大版的核心创新——
让"meta-agent 看自己最近的成功/失败模式"形成 self-conditioning。

## 4. Design alternatives + trade-offs

### 4.1 Alternative A — 仅 Skill YAML LLM-soft 字段（窄面）

可变面 = 6 Skill YAML × {procedure, grounding_instruction, escalation_policy,
critical_steps[].desc}。其它一切锁定。

**优点**：
- 与 M2 抽象 1:1 对齐；不需要新的"什么算可变"的论证。
- §1.7 anti-hardcode 红线在结构上不可能被违反（trace_check DSL parser /
  guardrails / tools_required 都不可变）。
- 单次迭代成本可控（Spring restart + 评测），不引入 customer-facing 话术风险。
- shadow set / 反 overfit / Codex 复核都可以延用现有机制。

**缺点**：
- 探索面较窄——某些类型的改进（例如客服话术细节、新增 escalation_reason
  的 prompt 引导）触不到。
- `system_prompt.txt` 残留的 23-value escalation_reason 决策树不能被 auto
  调优——如果发现某个 Tier-2 reason 描述误导 LLM，需要人工改。

### 4.2 Alternative B — Skill YAML LLM-soft 字段 + Fixed Script Library (templates.yaml)

可变面 = A + `server/src/main/resources/scripts/templates.yaml` 的 50+
fixed script 模板。

**优点**：
- 直接调优客户面话术——CSAT 维度更直接。

**缺点**：
- 客服话术正确性**无法通过现有四层程序化评测充分捕获**——它依赖 L3
  `relevance` / `tone_appropriateness` / `groundedness` 这些 advisory 维
  度，已经被 M3-Eval 降级为不 gate。换言之，**auto-loop 没有可信的程序化
  fitness 信号判断"模板话术变好了 vs 变差了"**。
- `{VARIABLE}` 占位符 / 法务签字过的 SLA / `{POLICY_URL}` 等字段被 LLM
  subtly 破坏时，sandbox content validator 需要更多规则。
- `docs/fixed_script_library_v1.md` 是 v1.1 (2026-04-21) 人工 curated；
  覆盖它需要额外的 human review。

### 4.3 Alternative C — Staged: A 先，证明稳定后再扩到 B

Stage 1 = Alternative A，跑 5+ kept iterations + human review 全部确认无
退化 → Stage 2 解锁 templates.yaml。

**优点**：
- 风险阶梯化；可以在 Stage 1 充分校准 meta-agent prompt / gaming detector
  / lessons-learned 机制后再扩面。
- 与 autoloop_design.md §3.3 staged surface 思路一致。

**缺点**：
- 多一道 milestone 切换；deliver-agent / human 都需要明确判断"何时进入
  Stage 2"。

## 5. Recommended option + rationale

### 推荐 Alternative C（Stage-1 即 Alternative A）。

**理由**：
1. 与人类锁定决策（"skills + skills 关联的配置文件 mutable，其他全部
   lock"）最贴合的精确化——Alternative A 把"skills 关联的配置文件"具体化
   为 Skill YAML 的 LLM-soft 字段子集，避开了 Skill YAML 结构性字段 /
   tool-policy.yaml / control-policy.yaml 这些被人类一并表达为"deterministic
   runtime"应锁定的项。
2. 与 M2/M3-Eval 当前架构的 LLM-soft/runtime/eval 三分边界 1:1 对齐。
3. 四层评测对 Tier-2 critical_steps 改动有可靠程序化信号；对 templates.yaml
   改动**没有**——v1 不应在"评测无法判断好坏"的面上做 hill-climb。
4. Staged design 让我们在 Stage 1 阶段调试好 meta-agent 提示、lessons
   memory、anti-gaming 等，再决定是否值得为 templates.yaml 单独投入扩
   评测能力（例如 CSAT 模拟 / 人评抽样）。

### Stage 1 (V1) 形态详细

```
autoloop/                    # NEW: Python CLI
  program.md                 # LOCKED human contract (与 autoloop_design.md §9 同精神)
  pyproject.toml
  config.yaml
  autoloop/
    cli.py                   # check / dry-run / run / report / apply / audit
    loop.py                  # 主循环：propose → validate → apply → build → gate → log
    meta_agent/
      analyzer.py            # eval results → sanitized failure taxonomy (per-Skill aggregation)
      proposer.py            # taxonomy + lessons + iteration index → ONE Skill-field edit hypothesis
      lessons_compactor.py   # 每 K iter 触发 LLM 压缩成 lessons.md
      prompts/
        analyze.txt
        propose.txt
        compact.txt
    sandbox/
      yaml_diff_validator.py # YAML AST diff：只接受白名单字段的修改；reject any 结构字段改动
      anti_hardcode_check.py # 9-question kernel 子集自动检查 (Q1/Q2/Q4/Q5)
      content_validator.py   # placeholder/length/forbidden-token 检查
      applier.py             # 写文件 + git commit on autoloop/exp-{N}
      server.py              # 在另一个端口启动 server (与 autoloop_design.md §13 一致)
    scoring/
      tier_evaluator.py      # 跑 anchor + anchor_outcome + bad_cases + shadow，按 §3.3 五层评分
      eval_runner.py         # subprocess 调 mvn eval + uv run eval-interactive
      gaming.py              # 6 anti-gaming checks (沿用 autoloop §8) + 新增 Tier-2 measurement 防止改自己卷子的余 patch 检查
    memory/
      experiments_log.py     # raw JSONL append
      iterations_index.py    # SQLite 操作
      lessons_log.py         # markdown append + 触发 compaction
    report.py                # HTML 汇总 (per-tier delta + lesson timeline)
  results/
    experiments.jsonl
    iterations.sqlite
    lessons.md
    runs/exp-{N}/
      hypothesis.json
      diff.yaml
      tier0_replay_report.json
      tier1_anchor_outcome.json
      tier1_bad_cases.json
      tier2_critical_steps.json
      shadow.json
      decision.json
  tests/                     # sandbox + scoring unit tests
```

调用形态（沿用 autoloop_design.md §11）：

```bash
python -m autoloop check
python -m autoloop run --experiments 1 --dry-run
python -m autoloop run --experiments 1
python -m autoloop run --experiments 10        # 一夜约 4–6 小时
python -m autoloop run --experiments 20        # 周末约 8–12 小时
python -m autoloop report
python -m autoloop apply --experiment exp-007  # cherry-pick 到当前分支
python -m autoloop audit --experiment exp-007
```

## 6. Scope split + delivery priority suggestion

**Deliver agent 最终决定，研究 agent 仅建议**。下列拆分是参考形态：

### Milestone M-Auto-1 — Auto-Evolution v1 (Stage 1)

5 个 sub-sprints，递增上线，cumulative ~3–4 周。前 3 个 sub-sprint 互相
有依赖（contract → sandbox → loop），后 2 个可与前 3 并行规划但应当在
loop 接通后执行。

| Sub-sprint | 名称 | Layer (§3.2) | Scope 三句话 | Codex |
|---|---|---|---|---|
| S-Auto-1 | Mutable-surface contract + program.md | infra | 在 `autoloop/program.md` 写定 V1 可变面 + lexicographic 5 层 + lessons memory 三层契约；`autoloop/sandbox/yaml_diff_validator.py` 接受/拒绝任意 YAML diff；`autoloop/tests/` 全覆盖 reject 集（结构字段改动 / trace_check 改动 / 多文件 diff 等）。零 server / eval 触动。 | milestone-shared |
| S-Auto-2 | Tier-evaluator + four-tier fitness | eval_spec | `autoloop/scoring/tier_evaluator.py` 把现有 `results.json` 解析为 5 层 lexicographic verdict；shadow set 通过 `CaseSetManager.load_custom` 单独跑；不改任何评测代码或 case_specs。 | milestone-shared |
| S-Auto-3 | Loop orchestrator + meta-agent + 3-layer memory | infra | `autoloop/loop.py` + `meta_agent/{analyzer,proposer,lessons_compactor}.py` + `memory/{experiments_log,iterations_index,lessons_log}.py`；端到端能跑通 1 次 dry-run + 1 次实迭代；meta-agent prompt 内置 §4.1 nine-question 自检 + lessons 喂入。 | milestone-shared |
| S-Auto-4 | Anti-gaming + Codex review hook | eval_spec | 移植 autoloop_design.md §8 的 6 checks；新增 "trace_check / mandatory_for / severity 是否被改" 防御（V1 应该 sandbox 已经拒绝，这是 belt-and-suspenders）；接入 Codex review prompt 给 human 在 cadence 化复核时直接调用。 | **per-sub-sprint** (§4.3 trigger #2 — 触及 §1.7 红线判别) |
| S-Auto-5 | Overnight stability run + first human review batch | eval_spec | 一夜 10–20 iterations 实跑；human + deliver-agent 在第二天 review kept 候选；首次按 §5.6 cadence 化 bad-case 人工评判；至少 1 个 kept 候选应通过完整审查后 cherry-pick 到 main 作为 v1 ship 证据。 | milestone-shared |

### 与 §5.6 bad-case suite 的交接

bad-case manual review 不变（仍然是 deliver-agent + human 在每个 milestone
close 跑）。auto-loop 每个 kept iteration 都会运行 bad-case 程序化子信号；
human review batch 是 out-of-loop 的额外保险。

### 与 M5 / M3-B / 其它 milestone candidates 的关系

- **M3-B Single Handover Orchestrator** (P0 release-gate per `docs/release_gate.md` §1.1)
  独立，不被 M-Auto-1 阻塞；但 M-Auto-1 v1 不会主动碰 handover 代码（runtime 锁）。
- **Projection-hygiene milestone candidate** (M5 carry-over OQ-S52.4 / #4)
  独立，与 M-Auto-1 不冲突。
- **UC-G/H/I/J bad-case seeding**：新加的 bad case 会自动进入 auto-loop
  的 fitness（因为 bad_cases 是 fitness 计算的一部分），所以新加 bad case
  → auto-loop 自动获得新的优化目标。无需 M-Auto-1 内部协调。

## 7. Layer classification + §7 stanza pre-fill

M-Auto-1 整体是 **multi-layer prospective**（infra + eval_spec），不同
sub-sprint 主层不同。Per-sub-sprint 主层：

| Sub-sprint | Primary layer (§3.2) | Tier-0 invariant? | Semantic hardcode? |
|---|---|---|---|
| S-Auto-1 | `infra` | None | None — 纯白名单 sandbox + 文档契约 |
| S-Auto-2 | `eval_spec` | None | None — 仅消费已有 `results.json` |
| S-Auto-3 | `infra` | None | None — meta-agent 是工具；提示禁止硬编码 |
| S-Auto-4 | `eval_spec` | None | None — anti-gaming 检查是反硬编码检查 |
| S-Auto-5 | `eval_spec` | None | Possibly Y — 第一批 kept iteration 中如果有任何 Skill YAML 改动出现"边缘 §5.3 case"，需触发 §7 stanza 的 sunset plan |

### §7 stanza 预填（milestone-level，供 deliver-agent 改写）

```
Target failure layer: infra (M-Auto-1 整体) + eval_spec (S-Auto-2/4/5)

Tier-0 invariant: M-Auto-1 不新增 Tier-0 invariant。C2 / C3 仍 DEFER per
M2-close verdict。如果 auto-loop 在 Stage 1 surfacing 出现新的 Tier-0
候选，按 §3.2 Q2 tail rule 走 human_review_required，不在 M-Auto-1 内部
elevate。

Semantic hardcode: M-Auto-1 本身不引入 semantic hardcode；它是一个
能产生 semantic hardcode 的工具。两道结构性防御：
(1) `autoloop/sandbox/yaml_diff_validator.py` 只接受 Skill YAML 的
{procedure, grounding_instruction, escalation_policy, critical_steps[].desc}
四类字段修改；任何对 trace_check/mandatory_for/severity/id/guardrails/
state_inheritance/tools_required/applicable_use_cases 的改动被结构性拒绝。
(2) `autoloop/sandbox/anti_hardcode_check.py` 在每次 propose 后跑 §4.1
nine-question kernel 的 Q1/Q2/Q4/Q5 自动检：if-else 模式 / 关键词列表 /
eval case id 引用 / LLM 语义权限收窄 — 命中即 propose-stage discard，
不进入 build/eval。Sunset plan: per-sub-sprint Codex review (S-Auto-4) +
human review batch (S-Auto-5) 把 8/9 项人工兜底；如果某次 kept 候选在
人评时被判定为 borderline §5.3 case，启动该候选的 downgrade-to-signal
follow-up R-item。

Generalization coverage:
- target = M-Auto-1 整体能在 159 anchor + 12 anchor_outcome + 12 bad_cases
  上跑出一个完整 keep / discard verdict，并把 raw + index + lessons 三层
  memory 落盘；
- neighbor = 23 shadow cases 上 case_passed 不回归 > 3%（自动 gate）;
- negative = 故意提一个 §1.7 红线 hypothesis（"add 'appeal' keyword to
  discover_triage procedure"），sandbox/anti-hardcode 必须 reject 在
  propose 阶段；regression test 覆盖；
- shadow = S-Auto-5 一夜跑后，human + deliver-agent 用 §5.6 cadence 化
  bad-case manual review 抽 1–2 个 kept 做完整人评，作为「程序化 keep
  ≠ 真正 keep」的实证 calibration。
```

## 8. Hard fences + non-goals

**Hard fences (milestone-level; 无例外)**

1. M-Auto-1 不修改任何 `server/src/main/java/**`。
2. M-Auto-1 不修改任何 `eval/` / `eval_interactive/eval_interactive/**`
   代码（包括 scoring / batch / loader / simulator）。
3. M-Auto-1 不修改任何 case_spec 文件——包括
   `eval_interactive/case_specs/{anchor,anchor_outcome,bad_cases,case_families,smoke,exploration,probe,promotion}/**`
   和 `eval_interactive/case_specs_shadow/**`。
4. M-Auto-1 不修改 `docs/runtime_freeze_and_risk_policy.md`、
   `docs/foundational/**`、
   `docs/current/iteration_governance.md`（除非 Stage 1 完成后需要 fold-back
   §5.5/§5.6 关于 auto-loop 的角色描述，那是 M-Auto-1 close 之后的 doc
   governance 工作）。
5. M-Auto-1 不修改 `server/src/main/resources/{prompts,scripts,config}/**`
   下任何文件（V1 锁定面；Stage 2 才可能解锁 templates.yaml）。
6. auto-loop 的 meta-agent 不允许跨多文件 diff——每次 propose 只能改一
   个 Skill YAML 的一个字段或同一个 Skill YAML 的多个允许字段；不允许
   跨 Skill。
7. auto-loop 不写 `main` 分支；所有 kept 候选只在 `autoloop/keep-{N}`
   分支；human 评审后用 `python -m autoloop apply` 才 cherry-pick。
8. shadow set 失败信息**永不**喂回 meta-agent。meta-agent 只看见聚合
   `shadow_regression_detected: yes/no`。

**Non-goals (intentionally deferred)**

- **不解锁** `templates.yaml`（推迟到 Stage 2 决策点）。
- **不解锁** `system_prompt.txt` / `routing_prompt.txt`（同上）。
- **不引入新评测维度**——v1 完全消费现有四层框架的产出。新维度（例如
  CSAT 模拟器）是单独的 R-item。
- **不在线评估 (online auto-loop on live traffic)**——v1 是离线。Live
  traffic 反馈是单独的设计问题（生产 trace observation → bad-case 提议
  → 走现有 Path 2）。
- **不替代 §5.6 bad-case 人工评判**——auto-loop kept 只是 candidate；
  human review 才是合入门。
- **不替代 Codex anti-hardcode review**——auto-loop 内部的 §4.1 子集自检
  是 belt-and-suspenders，per-sub-sprint Codex 仍按 §4.3 走（S-Auto-4 是
  per-sub-sprint trigger）。
- **不替代 deliver-agent 的 milestone 决策**——milestone 拆分由 deliver-
  agent 与 human 最终决定。

## 9. Risks + compounding effects

### 9.1 风险表

| 风险 | 严重度 | 缓解 |
|---|---|---|
| meta-agent 通过 `procedure` 文字写隐性 if-else（"如果用户说 X 就 Y"）绕过 §1.7 | Critical | sandbox `anti_hardcode_check.py` 自检 Q1+Q5 + `propose.txt` 提示中明确禁止 + S-Auto-4 per-sub-sprint Codex 复核 + 周期 lessons.md 复盘 |
| meta-agent 在 `critical_steps[].desc` 上写出"硬决策"风格的叙事 (M3-Eval §5.3 红线) | Critical | 同上；S-Auto-4 重点检查；S-Auto-5 首批人评抽样 |
| 多次 kept 累积漂移：单次每次都过 §1.7，但叠加 N 次后整体 Skill 文本逐渐变成枚举式 | High | lessons.md 每 K 次触发 LLM "drift-check"：对比当前 Skill YAML vs M2 baseline，如果叙事密度 / decision-tree 风格指标超过阈值 → 暂停 loop 触发 human review |
| Provider drift 假象 keep（一次 LLM 服务商抖动让某 case 偶然 PASS） | High | autoloop_design.md §5.3 已设计 temperature=0 + fixed seeds；本 proposal 沿用；同时第一次 dry-run 校准 noise floor，>2% threshold 部分压住 |
| Tier-2 trace_check 看似不可变但 critical_steps[].desc 描述漂离 trace_check 后形成 LLM 与判分错位（desc 越来越像 X 路径，但 trace_check 还在量 Y 路径） | Medium | desc 修改时 sandbox 拒绝同时改 mandatory_for（保证测量契约稳定）；compaction 阶段 LLM 抽查 desc-vs-trace_check 语义一致性，发现错位 → discard 并记入 lessons.md |
| Spring 重启慢 (60-120s) 导致 night run 时长爆炸 | Medium | 接受成本；20 iter × (15s restart + 18 min eval) ≈ 6-7h 仍可一夜跑完。Stage 2 可考虑 server hot-swap on Skill YAML（与 SkillRegistry 不可变快照矛盾，需要独立 R-item） |
| 跨迭代 lessons 喂回让 meta-agent "学到" 某种作弊套路 | Medium | lessons compaction 提示明确禁止「学习如何过 gate」；compaction 输出由 human 在 weekly review 阅读 (lessons.md 是 plain markdown) |
| shadow set 因为修改面太窄、与 visible 高度同构而不再起反过拟合作用 | Medium | shadow set 现在覆盖 12 个 case-family；监控 shadow 与 visible 的 correlation；correlation > 0.95 触发 R-item `R-shadow-set-rotation` |
| auto-loop 改 Skill YAML 后某些 Tier-2 advisory step 被 LLM 完全无视，导致 advisory→0% 但 mandatory PASS 不变 | Low (advisory 不 gate) | 记录到 Tier-3 trend；human review 抽查；不 auto-discard（advisory 本就允许漂移） |
| sandbox 误判一个合法 propose 为 hardcode | Low | propose 输出有 `--explain` 模式；human 在 audit 时可手动 override 一次（记 audit log） |

### 9.2 Compounding effects (顺序依赖)

```
M2 (Skill 抽象，已 ship)
  └─→ M3-Eval (四层金字塔 + critical_steps + Skill YAML 单一事实源，已 ship)
        └─→ M4-Eval-Cleanup (advisory 降级清理，已 ship)
              └─→ M-Auto-1 v1 (本 proposal)
                    ├─ S-Auto-1 (契约 + sandbox)
                    │    └─→ S-Auto-2 (fitness)
                    │          └─→ S-Auto-3 (loop + memory)
                    │                └─→ S-Auto-4 (anti-gaming + Codex hook)
                    │                      └─→ S-Auto-5 (overnight + first human review)
                    └─ (Stage 2 候选 — 解锁 templates.yaml；不在 v1 内)
```

不正确的顺序会导致：

- S-Auto-2 在 S-Auto-1 前完成 → tier_evaluator 没有 sandbox 兜底，meta
  -agent 可以在 dev 期意外改动结构字段。
- S-Auto-3 在 S-Auto-2 前完成 → loop 跑出来的 verdict 没有可信 fitness
  function，所有"kept"都不可信。
- S-Auto-5 在 S-Auto-4 前完成 → 第一夜跑出来的 kept 候选没有 anti-gaming
  扫描，第二天 human review 一个一个手动检查，效率回退到没自动化的水平。
- 跳过 S-Auto-5 直接走 M-Auto-1 close → 没有"自动 keep 与人工 keep 是否
  一致"的 calibration 证据，下一次 human review batch 会无标尺。

### 9.3 与现有 R-item 的耦合 / 影响

- `R-bad-case-suite-uc-ghij-seed-from-real-sessions` — 新 bad case 会
  自动成为 auto-loop fitness 的一部分（bad_cases programmatic 通过数）。
  auto-loop 启动后会自然倾向于"先解决新 bad case"。
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` — semantic_planner 层
  R-item；auto-loop 在 `discover_triage.procedure` / `critical_steps[].desc`
  上的改动会直接试图减少该 case 的 Tier-1 outcome 失败。
- `R-bad-case-parallel-session-establishment-flakiness` — infra 层；与
  auto-loop fitness 计算的 stability 直接相关；建议在 M-Auto-1 启动前
  让该 R-item 至少缓解到 parallel=1 isolated rerun 自动化（auto-loop 跑
  bad_cases 走 isolated mode）。
- `R-shadow-include-flag-runner-gate` (Sprint 20) — 还在 deferred；
  M-Auto-1 v1 通过 `CaseSetManager.load_custom` 单独调用 shadow，不依赖
  runner flag；该 R-item 优先级不变。
- `R-eval-report-observability` 已在 M5 S1 关闭（report.html 已显示四层
  verdict）；auto-loop 不依赖该 R-item，但 Stage 2 报告生成可以复用
  `report.html` 的 tier-aware 表格。

## 10. Observability / trace / report implications

auto-loop 的观察面与 csagent 现有的「report.html + admin trace + projected
context」三个核心观察面**正交但补充**：

1. **新增**：`autoloop/results/runs/exp-{N}/` 下每个 iteration 的：
   - `diff.yaml` — 本次 Skill YAML 改动 patch (`git diff` 风格)
   - `tier_breakdown.json` — Tier-0/1/2/3 各自的 pass/fail + delta vs baseline
   - `shadow.json` — shadow gate 结果（聚合，不暴露 case-level failure）
   - `gaming_flags.json` — 6 anti-gaming + Tier-2 measurement guard 结果
2. **新增**：`python -m autoloop report` 输出 HTML 时间线 — 横轴是 iteration
   id，每行展示 hypothesis 摘要 + 各 Tier delta + decision；点击某行展
   开 diff。
3. **新增**：`lessons.md` markdown，人类直接阅读。每个 lesson 一段，按
   Skill × field 聚类。
4. **复用**：现有 `eval_interactive/results/<run-id>/` 输出由 auto-loop
   subprocess 触发；不改 schema。
5. **复用**：现有 admin trace UI (M5 S2 之后) — auto-loop kept 候选在
   human review 时可以打开 admin trace 看具体 LLM raw response。
6. **复用**：`docs/codex-findings.md` scaffold — S-Auto-4 per-sub-sprint
   Codex 复核走标准 sprint-close header。

**Stale-observability 风险**：参照 `project_observability_debt_pattern.md`
的经验——架构更新后观察面常常滞后。M-Auto-1 上线后，应该立即在
- `docs/10-handoff.md` §0 增加一行 "Auto-loop status: idle / running /
  paused (gaming)";
- `docs/action_bank.md` §5 / §6 增加 "auto-loop kept iterations 一览" 行
  （非每次 iteration，而是 batch-level summary）。

---

## Appendix A — autoloop_design.md 哪些可以原样复用 / 必须替换

| autoloop_design.md 段落 | M-Auto-1 v1 对应 | 处置 |
|---|---|---|
| §1 Readiness Assessment | §2 (本文) | 替换 — shadow set 不再是 Gap |
| §2 Architecture Overview | §5 (本文) "Stage 1 形态详细" | 复用结构 + 把 `eval/` invocation 改为 four-tier 调用 |
| §3 Mutable Surface | §3.1 (本文) | **完全替换** — 4 文件 → Skill YAML LLM-soft 字段 |
| §4 Scoring Model | §3.3 (本文) | **完全替换** — 老 L1/L2/L3 → Tier-0/1/2/3 |
| §5 Two-Phase Protocol | §3.3 (本文) | 复用思路（Phase A 快 / Phase B 深），把"replay → interactive" 改为"Java replay hard gates → Python four-tier eval"；Phase B 内拆 anchor_outcome / bad_cases / shadow 三段 |
| §6 Case Set Strategy | §2.3 + §3.3 (本文) | 复用；shadow set 复用现有 23 cases；visible_anchor 改为 159 anchor + 12 anchor_outcome + 12 bad_cases |
| §7 Meta-Agent Design | §3.4 (本文) | 复用 sanitized failure taxonomy + propose.txt 模板；新增 lessons.md 喂入 |
| §8 Anti-Gaming | §3.3 Layer 4 + §9.1 (本文) | 复用 6 checks + 新增"Tier-2 measurement-contract change attempt" 检查 |
| §9 program.md | §5 + §8 (本文) | 复用结构 + 用本文 §8 hard fences 改写 forbidden 部分 |
| §10 Experiment Lifecycle | §3.4 + §6 (本文) | 复用流程 + 在每个迭代后写 sqlite index + 在每 K 迭代后触发 lessons compaction |
| §11 Execution Modes | §5 (本文) | 复用 CLI 表 |
| §12 File Structure | §5 (本文) | 替换为本文 §5 结构 |
| §13 Configuration | §5 (本文) | 复用 + 加 lessons_compaction_window + tier-evaluator 配置 |
| §14 Makefile Integration | — | 复用 |
| §15 Implementation Plan | §6 (本文) | 替换为 5 sub-sprint 拆分 |
| §16 Done Criteria | §3.3 + §6 + §8 (本文) | 重写为四层 + lessons 相关 |
| §17 Design Decisions Log | §4 + §5 (本文) | 重写关键决策 |
| Appendix A | §10 (本文) | 复用 + 加 admin trace + report.html 集成 |
| Appendix B Karpathy mapping | §1 + §3.4 (本文) | 重写：locked = eval/+case_specs/+server Java；mutable = 6 Skill YAML LLM-soft fields；val_bpb = four-tier lexicographic |

## Appendix B — Karpathy / autoresearch 映射（更新版）

| Karpathy autoresearch | csagent v1 (本 proposal) | 备注 |
|---|---|---|
| `prepare.py` (locked) | `eval/` + `eval_interactive/` + `data/` + `case_specs*` + `case_specs_shadow/` + 所有 Java + 所有 config | 全 locked；evaluator immutable |
| `train.py` (mutable) | 6 Skill YAML × {procedure, grounding_instruction, escalation_policy, critical_steps[].desc} | 比 Karpathy 单文件 train.py 还窄；结构上不可能写出关键词 if-else |
| `program.md` (human contract) | `autoloop/program.md` + repo `iteration_governance.md` §1/§1.7 透传 | 双重契约 |
| `val_bpb` (single metric) | Tier-0/1/2/3 lexicographic 5 层 + shadow regression gate | 必须 lexicographic — CS 场景不能用标量 |
| 5-min training run | ~15-30 min iteration (Spring restart + four-tier full eval) | Spring 重启是 csagent 的"训练编译" |
| git keep/discard | git branch + cherry-pick；keep 候选合入由 human review batch 决定 | 比 Karpathy 多一步 human gate |
| — | bad-case suite §5.6 manual review (out-of-loop, cadence 化) | Karpathy 不需要；CS 场景特有 |
| — | shadow set 反过拟合 + DSL parser 反硬编码 + 9-question kernel 自检 | 多重结构性防御 |
| — | lessons.md (LLM-distilled lessons across iterations) | 这是本 proposal 在 Karpathy 之上的扩展 |

## Appendix C — 一个示例 iteration (说明性)

`exp-007` 假想形态：

- **input**：meta-agent 读 baseline 评测结果 + lessons.md。
  - failure taxonomy：12 bad cases 中 `cs015_uc_fp_appeal_edit_repost`
    长期 FAIL，failure_shape = "UC-FP 错路由到 UC-K"；
    `resolve_faq_grounded_answer.yaml` 的 critical_steps 第 5 步
    "reroute-on-actionable-appeal-intent" 在 trace 上 always advisory FAIL。
  - 最近 lessons：`L-2026-05-25-003` "在 procedure 末尾加 enumerated 决
    策树 → Codex Q5 reject 比例 4/5"。
- **hypothesis** (meta-agent 输出)：
  - target_file: `resolve_faq_grounded_answer.yaml`
  - target_field: `critical_steps[4].desc` (即 `reroute-on-actionable-appeal-intent`
    的 desc)
  - change: 把 desc 中"the appropriate next move is to surface the UC-H
    appeal-intake path"具体化为"explicitly name the appeal pathway
    when the user shifts from understanding to acting on the removal;
    treat the listing status as evidence, the user's stated need as the
    primary signal" — 软叙事，不是 if-then。
- **sandbox**：
  - yaml_diff_validator：仅改 critical_steps[4].desc → ✅ allow。
  - anti_hardcode_check Q1: 没有关键词列表 → PASS。Q4: 没有 case_id 引用
    → PASS。Q5: LLM 仍然 owns reroute decision → PASS。
- **apply + build**：写文件 + git commit → mvn compile → spring-boot
  restart on :8081 (~30s)。
- **Phase A — Tier-0 (Java replay)**：11 hard gates 全 pass → PASS。
- **Phase B — Tier-1**:
  - anchor_outcome programmatic case_passed rate: 9/12 → 9/12 (无变化)
    → PASS (没回归)。
  - bad_cases programmatic case_passed count: 5/12 → 5/12 (注意 cs015
    还是 programmatic FAIL，因为 Tier-2 mandatory step PASS 但 L3
    user_goal_achievement 还低) → PASS (没回归)。
  - L3 user_goal_achievement mean: 3.2 → 3.4 → soft 改善。
- **Phase B — Tier-2**: cs015 上 Skill `critical_steps` Tier-2 mandatory
  fail count 不变 → PASS (没回归)。
- **Layer 3 — improvement threshold**: anchor case_passed rate 130/159
  → 133/159 (+1.9%, 略低于 2% 门) — 但 L3 user_goal_achievement +0.2
  + smoke task_success_rate +0.03 → meta-agent 综合判断"边缘改善"，
  在 tiebreaker 表里登记，不触发 fast-discard，但需要更严格 review。
- **Layer 4 — shadow regression**: shadow 23 cases case_passed rate
  18/23 → 18/23 → PASS (无变化)。
- **decision**: KEEP (improvement >2% on at least one observable when
  including L3 + smoke as tiebreaker per config rule);
  branch tagged `autoloop/keep-007`。
- **log**:
  - `experiments.jsonl` append 1 行。
  - `iterations.sqlite` insert with `decision="keep"`, `target_skill=
    "resolve_faq_grounded_answer"`, `target_field="critical_steps[4].desc"`,
    `edit_summary="explicit appeal pathway naming in reroute desc"`,
    `parent_iteration_id="exp-006"`.
- **下一轮 propose 时**: meta-agent 看到 "exp-007 keep on cs015 reroute
  desc" → 在下一轮 lessons compaction 时（如果 exp-007 是 K=10 的第 7
  条），自动加入"软叙事的 'shift from X to Y' 风格在 cs015 上有效"
  candidate lesson。

---

## Appendix D — 与 §1.7 forbidden-list 的逐条对齐

| §1.7 forbidden item | M-Auto-1 v1 防御 |
|---|---|
| encoding raw eval phrases into Java or prompt | sandbox `anti_hardcode_check.py` Q4：propose diff 中包含 case_id / session_id / 已知 eval phrase pattern → propose-stage discard |
| adding UC-specific hard rules for soft semantic decisions | sandbox：tools_required / applicable_use_cases / guardrails 锁定；desc 内 if-else 模式由 anti_hardcode_check.py Q1 + Codex S-Auto-4 拦截 |
| widening eval spec to accept a genuine bot mistake | M-Auto-1 hard fence #3：不允许改任何 case_spec；不允许改任何 scoring 代码 |
| optimizing visible eval at the cost of shadow/generalization | Layer 4 shadow gate（3% 阈值）+ shadow 永远不喂回 meta-agent |
| using prompt as an if-else rule dump | meta-agent `propose.txt` 提示 explicit 禁止；anti_hardcode_check.py Q1；Codex S-Auto-4 per-sub-sprint review |

---

> **下一步**（deliver-agent + human 决定）：
> 1. 是否选用本 proposal 作为下一个 milestone candidate（M-Auto-1）。
> 2. 选用后，deliver-agent 把本文 §6 的 5 个 sub-sprint 提到 `docs/milestone_objective.md`，
>    并起草 S-Auto-1 的 `docs/sprint_objective.md`。
> 3. 与现有 M5 carry-over（projection hygiene #4 + OQ-S52.4）以及 M3-B
>    Single Handover Orchestrator P0 之间的 milestone 排序由 deliver-agent
>    最终决定；本 proposal 不抢占其它候选优先级。
