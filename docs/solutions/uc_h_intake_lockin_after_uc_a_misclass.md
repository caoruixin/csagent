---
title: 用户问“为什么看不到我的广告” → 被锁进 UC-H Appeal Intake 死循环（session 6252fb2e）
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-05-25
mode: bad-case-driven
related_bad_case: eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml
related_r_items:
  - R-loosen-topic-uc-binding-llm-owned-drift (action_bank.md:685)
  - R-grounding-discipline-iterative-search-fabrication (consumed by Sprint 39 S1)
session_id: 6252fb2e-af09-4768-bb61-d2766825d32b
---

# Executive summary

人类提出的 session 是 `alice_uc_a_uc_h_misclass` 这条 bad case 在
production-like 流量上的**第二条实证**（首条是 2026-05-16 的 Alice 合成
session）。失败链 4 层叠加：

1. **DISCOVER 阶段语义错分（semantic_planner，§3.2 Q5）** — 用户说“为什么
   看不到广告”这种带 “why” 的**理解型问句**，LLM 却以 confidence=0.8
   commit 到 UC-H（Ad Removal Appeal，**INTAKE-path**）。系统 prompt 在
   Sprint 33 cue 里**明文写了**“why 是 UNDERSTAND（应路由 UC-A/UC-FP），
   appeal 是 ACT（才路由 UC-H）”，LLM 视而不见。
2. **Skill-mediated intake lock-in（skill_state + prompt_projection）** —
   UC-H 被 commit 后，`SkillRegistry.select(RESOLVE, UC-H)` 选中
   `resolve_intake_collect_and_handover.yaml`，procedure 被硬编为
   “只问 `intake_state.fields_remaining` 里的下一个 missing 字段”。UC-H 三
   个必填字段是 `ad_id_or_listing_url` / `registered_email` /
   `stated_reason_or_context`（`IntakeFieldsRegistry.java:59`），所以
   bot 必然问“你为什么要 appeal”——这就是用户看到的提问。
3. **RESOLVE 阶段没有可用的 mid-session reroute 渠道（架构 gap）** — M5
   S4 的 Skill-declaration gating 把 `alternate_candidate_use_cases` /
   `discover_disambiguation_signals` 这两个 soft-signal slot 限定为
   DISCOVER-only（只有 `discover_triage.yaml` 在
   `state_inheritance.soft_signal_via_projection` 里声明了它们）。Java
   侧的 `RuntimeIntentClassifier` 是 keyword-based，用户的“why I can't
   see it”不匹配任何 reroute 触发关键词，**用户结构性地被锁死**。
4. **降级到 escalation 失败（observation）** — LLM 第 5 轮放弃，emit
   `request_handover(escalation_reason="system_failure")`，该字面量不在
   23 值 canonical enum 里，被 `PhaseEvaluator.canonicalize(...)` 兜底
   成 `service_degraded`；case 落到 `Ad_Support_Queue`（UC-H 自带队列）。
   从客户看：明明只问“为啥广告不见了”，最后被丢给“广告 appeal 团队”，
   且**自始至终没有得到答案**。

修复优先级：第 1 层（DISCOVER 错分）是**唯一可单独修复的入口**；第 2/3
层的 lock-in 是 M2 Skill 架构的当前形态，单独打开会动到 §1.7 红线；第 4
层只是症状。compounding 顺序见 §9。

---

# 1. 关于“process 流程是不是在 skill yaml 里维护”这个问题

先回答这个架构问题，因为它直接影响“后续修哪个文件”的判断。

**结论：自 M2（Sprint 36–41）起，per-phase × per-UC 的“LLM 该做什么”
（procedure / tool 列表 / 状态继承 / guardrail）已经搬到 6 份 Skill
YAML；但 phase 状态机本身、tool 实现、intake 字段定义、UC 目录、drift
检测、tool 派发策略，依然在 Java 和其他 config YAML 里——按 §1.4
Runtime ownership 的故意设计。**

具体分层（HEAD `refactor/remove-the-shackles` 验证）：

| 层 | 位置 | 谁拥有 | 备注 |
|---|------|--------|------|
| Phase 状态机入口 | `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java:319` `evaluate(...)` switch on DISCOVER / RESOLVE / CONFIRM / CLOSE / ESCALATE | Runtime (Java) | §1.4 Runtime owns trace/eval contract |
| Per-(phase, UC) Skill 选择 | `PhaseEvaluator.java:364` `skillRegistry.select(phase, activeUc)` → `composeSkillPhasePlan(...)` | Runtime (Java) — 但选择目标是 Skill YAML |  |
| **Skill YAML（6 份）** | `server/src/main/resources/skills/*.yaml` | **声明式** | 见下表 |
| UC 目录 + topic-subject 映射 | `server/src/main/resources/config/use-case-registry.yaml` | config | 决定 `candidate_use_cases` |
| Intake 必填字段 | `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java:53` `REQUIRED_FIELDS_BY_UC` | Runtime (Java) | 通过 `{intake_required_fields}` 占位符注入 Skill procedure |
| Drift / reroute 关键词 | `server/src/main/resources/config/risk-keywords.yaml` + `RuntimeIntentClassifier.java` 内联 regex | Runtime (Java + config) | §1.7-adjacent，是 pre-LLM-first era 的遗留；`R-loosen-topic-uc-binding-llm-owned-drift` 已在 backlog |
| Tool 实现 | `server/src/main/java/com/gumtree/csagent/service/tools/*.java` | Runtime (Java) |  |
| Tool 派发策略 | `server/src/main/resources/config/tool-policy.yaml` + `ToolDispatcher.validateAgainstPlan(...)` | config + Java | §1.4 capability floor |
| Phase 转移合法性 | `server/src/main/resources/config/control-policy.yaml` + `ControlPolicyService.isValidTransition(...)` | config + Java |  |
| Per-turn projection | `ContextProjectionBuilder.java` | Runtime (Java) | M5 S4 后按 Skill `required_context_keys` / `state_inheritance.soft_signal_via_projection` 做门控 |

6 份 Skill YAML：

```
server/src/main/resources/skills/
├── discover_triage.yaml                       # DISCOVER, applicable_use_cases: ["*"]
├── resolve_faq_grounded_answer.yaml          # RESOLVE, UC-A/B/C/D/E/F/FP
├── resolve_intake_collect_and_handover.yaml  # RESOLVE, UC-G/H/I/J/K  ← 本 case 锁死在这里
├── confirm.yaml                               # CONFIRM
├── terminal.yaml                              # CLOSE
└── escalate.yaml                              # ESCALATE
```

每份 Skill YAML 字段：

- `procedure` — LLM 在该 phase 看到的 system_instruction 主体（含
  Sprint 7 / Sprint 33 cue 等历史 teaching）
- `tools_required` — 该 phase 允许的工具白名单
- `required_context_keys` — 期望被 projection emit 的 context key（S4
  起作为门控数据源）
- `state_inheritance` — Skill 切换时哪些 state 继承 / reset / 通过
  projection 软信号承载
- `valid_terminal_outcomes` — 该 phase 合法终态
- `guardrails` — 声明式 guardrail（如 `must_cite_source`,
  `intake_complete_required`），由
  `SkillGuardrailDispatcher.java` 统一派发
- `critical_steps` — Tier-1/Tier-2 关键步骤，参与 four-tier verdict 评估

**用一句话总结：Skill YAML = LLM 的 in-context playbook（“你这一回合
该按什么 procedure 跟用户对话、能调用哪些工具”）；Java + config YAML
= 运行时护栏（“你能做什么、不能做什么、怎么换 phase”）。这是 §1.3 /
§1.4 边界的物理映射。**

---

# 2. Session 6252fb2e 的逐回合解剖

| 回合 | 用户输入 | bot 输出 | DISCOVER→RESOLVE→… 转换 | 关键事件 |
|------|----------|----------|-------------------------|----------|
| 1 | `hi` | 友好澄清问句 | DISCOVER → DISCOVER | 4 candidate UCs：`[UC-A, UC-B, UC-FP, UC-H]`；`topic_subject_carries_multiple_candidate_ucs=true`；`ad_status_observed=null`（form 没带 ad_id）|
| 2 | `why I can't see my advert` | “请提供 ad ID” | **DISCOVER → RESOLVE，commit UC-H @ conf 0.8** | LLM reasoning：“ad status visibility inquiry, which maps to UC-H” — **这是错的**。Ad status visibility = UC-A。 |
| 3 | `ad-2001` | “请告诉我 ad removal appeal 的 reason 或 context” | RESOLVE → RESOLVE | 进入 UC-H intake；`intake_state.fields_remaining = [registered_email, stated_reason_or_context]` |
| 4 | `why I can't see it` | （同样的提问）“请解释 ad removal appeal 的 reason 或 context” | RESOLVE → RESOLVE | **用户第二次表达“why” = UNDERSTAND 信号，被 Skill procedure 忽略**（procedure 写死“ask the next missing field”，没有“re-check intent”分支） |
| 5 | `I should know the reason` | “帮你转接 specialist” | RESOLVE → ESCALATE | LLM emit `request_handover(escalation_reason="system_failure")` → `canonicalize(...)` 兜底成 `service_degraded` → 落 `Ad_Support_Queue` |

**关键观察**：turn 2 的 LLM `reasoning` 字段写的是 “ad status
visibility inquiry, which maps to UC-H”——这句话**自相矛盾**：“ad
status visibility” 是 UC-A 的字面名称（`use-case-registry.yaml:2-7`
`UC-A: "Ad Status & Visibility"`），不是 UC-H。LLM 把“看不到 = 一定
被删了 = 一定要 appeal”当成等价链，跳过了“UC-A 的 FAQ 解释路径”。

---

# 3. Multi-layer root-cause analysis

**Layer A — DISCOVER 语义错分 (`semantic_planner` per §3.2 Q5)**

- 证据：`projected_context.candidate_use_cases = [UC-A, UC-B, UC-FP,
  UC-H]`，`topic_subject_carries_multiple_candidate_ucs = true`，
  `ad_status_observed = null`（**form_context 完全没带 ad_id**，所以
  intake router 也没去查 listing 状态），LLM 仍以 0.8 confidence
  commit UC-H。
- 与 `discover_triage.yaml:20` Sprint 33 cue 的冲突：cue 明文要求
  “If the user's request is ambiguous between understanding and acting,
  ask ONE focused clarifying question this turn before committing
  classify_use_case (for example: 'Do you want to know the reason it
  was removed, or do you want to appeal the removal?')”——LLM 不仅没
  问，还**反向**把“why”读成 appeal 信号。
- 与 confidence 守则的冲突：cue 明文“>= 0.7 means the user's intent is
  unambiguous”，但本回合用户意图明显在 UC-A/UC-FP/UC-H 之间摇摆，
  confidence 应当 < 0.5（→ ask clarifying question）才对，LLM 给了 0.8。
- 这是 `deepseek-v4-flash` 在长 system_instruction（discover_triage
  procedure 是 ~4500 字符的单段文本，含 Sprint 7 / Sprint 33 / Sprint
  31 / Sprint 53 多个 cue 叠加）里**遗漏关键 cue** 的典型表现。

**Layer B — Skill procedure 把 LLM 困在 intake 单向流（`skill_state` +
`prompt_projection`）**

- `resolve_intake_collect_and_handover.yaml:22` procedure 原话：
  “Read the projected `intake_state.fields_remaining` array — that is
  the canonical list of required fields not yet collected... Ask ONLY
  for the next field in `intake_state.fields_remaining`; do NOT repeat
  questions about fields already in `intake_state.fields_collected`.”
- procedure 全文**没有任何**“如果用户行为暗示 UC 错了该怎么办”的指
  令。`valid_terminal_outcomes` 只有 `CLARIFICATION_NEEDED` 和
  `ESCALATE`，**没有 reroute / 回 DISCOVER 的合法出口**。
- `tools_required` 只有 `request_handover`——没有 `search_knowledge`
  / `classify_use_case` / 任何能让 LLM 表达“我想换 UC”的工具。

**Layer C — Skill-declaration gating 锁死了能挽救的 soft signal
（`prompt_projection` 架构 gap）**

- M5 S4 工作（Sprint 53, commit `c9390dc`）把 `alternate_candidate_use_cases`
  / `discover_disambiguation_signals` 这两个 slot 按 Skill 声明门控：
  只有 `discover_triage.yaml` 在 `state_inheritance.soft_signal_via_projection`
  里声明了它们（`discover_triage.yaml:30-32`），`resolve_intake_collect_and_handover.yaml`
  只声明了 `prior_use_case_carry`（`resolve_intake_collect_and_handover.yaml:37`）。
- 后果：进入 RESOLVE-INTAKE 之后，LLM 在 projection 里**看不到**
  `[UC-A, UC-B, UC-FP]` 这三个仍然 plausible 的备选 UC——本来这正是
  Sprint 31 Option β 设计要做的事。
- 注意：这个 gating 在 M5 S4 是**正确的 hygiene 决策**（避免在 RESOLVE
  emit 用不上的 DISCOVER soft signal）；问题不是“不该 gate”，而是
  “RESOLVE-INTAKE Skill 没有自己的 mid-session reroute 设计”。

**Layer D — Java reroute 是 keyword-based 且不覆盖本场景
（`prompt_projection` — §1.7 遗留）**

- `RuntimeIntentClassifier.java:160` `classify(...)` 走的是硬编 regex：
  - `UC_J_RISK_SHIFT_PATTERN`（scam / fraud / harassment）
  - `UC_C_NEW_REPLIES_PATTERN`（haven't got replies / messages）
  - `UC_A_SAME_ISSUE_PATTERN`（still can't see my ad）
  - `UC_A_FOLLOWUP_DURATION_PATTERN`（how long is it active for）
  - DriftDetector hard-shift fallback（`config/risk-keywords.yaml`
    里的 `ad removed` / `appeal` 等）
- 用户的 “why I can't see it” / “I should know the reason” 不匹配
  上面任何一条 regex；`classify(...)` 返回 `UNKNOWN`，
  `applyRerouteDecision(...)` 走 `CONTINUE_CURRENT`，session 留在
  UC-H。
- 即便匹配到了 `UC_A_SAME_ISSUE_PATTERN`，目标是 `SOFT_SHIFT_TO_DISCOVER`
  到 UC-A（FAQ-path）——但因为是 keyword-based、且 §1.7 明令禁止扩展，
  这条路无法兜底所有 UC-A 同义 phrasing。
- backlog 上 `R-loosen-topic-uc-binding-llm-owned-drift`
  （`action_bank.md:685`）已经把这层从“keyword/regex 路径”整体降级为
  “LLM-owned drift”作为 M3+ 候选，但还没排期。

**Layer E — escalation_reason 兜底掩盖根因（**observation**, 非可
独立修的层）**

- LLM emit `system_failure`（非 canonical），
  `PhaseEvaluator.canonicalize(...)` 默认兜底成 `service_degraded`
  （`PhaseEvaluator.java:78-95`），最终
  `escalation_reason = service_degraded` 写入 trace。
- 这一层是症状不是病因；但它**掩盖**了 trace 上的真实失败形态——
  Codex / human review 看 escalation_reason 时会以为是 LLM 抽风 /
  外部 provider 故障，而不是 mis-classification。M5 / OQ-S53.2 也观察
  到 escalation_reason 的“provider drift 噪声”问题。本 case 不在此
  R-item 范围。

---

# 4. Coverage check（与 backlog 对照）

| Backlog item | 与本 case 关系 |
|--------------|----------------|
| **`alice_uc_a_uc_h_misclass`**（bad-case suite，tier 见 `_manifest.md`）| **完全同形态**。本 session 是第二条 production-like 实证。closure_criterion (a)(b)(c) 都未达成，bot 走 FAIL 条件（>5 turns + non-canonical request_handover）。本次 session 应**追加为 `alice_uc_a_uc_h_misclass` 的第二个 source_session_id 证据**，或开新 case `cs6252_uc_a_uc_h_misclass_real_session` 互为 anchor。|
| `R-loosen-topic-uc-binding-llm-owned-drift`（action_bank.md:685）| **架构同源**。该 R-item 的目标是把 topic→candidate-UC 的 HARD gate 降级为 SOFT projection 让 LLM 拥有 cross-topic drift。本 case 是 **within-topic** disambiguation 失败（UC-A/UC-FP/UC-H 都属 Ad Support），所以**该 R-item 落地不会直接解决本 case**（cross-topic ≠ within-topic）。但解决方案的形态学相似（“demote Java cage to soft signal”），可以共享一些设计要素。|
| `R-grounding-discipline-iterative-search-fabrication`（已被 Sprint 39 S1 `must_cite_source` consumed）| 不直接相关。Sprint 39 解决的是 FAQ-path 的 grounded-answer 引用，本 case 根本没走到 FAQ-path。|
| `R-bad-case-suite-uc-ghij-seed-from-real-sessions`（752 行）| **直接相关**。该 R-item 就是要从真实 session 扩展 UC-G/H/I/J bad case。本 session 是一条新种子。|
| `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress`（760 行）| 相关但不同形态。iwzx 是 UC-K vs UC-H，本 case 是 UC-A/UC-FP vs UC-H。共享“within-topic 多 candidate UC + intake-path 锁定 → 用户被困”这一上位 pattern。|
| 当前 active milestone | **没有 active milestone**（M5 已 2026-05-25 收尾，下一个待规划）。本 case 适合作为下一轮 milestone 候选的输入证据。|

**Coverage gap**：

1. backlog 里**没有**专门针对“DISCOVER understanding-vs-acting verb-class
   disambiguation”的 R-item。Sprint 33 cue 是 in-prompt teaching，**没
   有任何 projection slot / runtime helper** 帮 LLM 区分用户的 verb
   语义类（“why/how/what” = UNDERSTAND vs “appeal/dispute/refund/delete”
   = ACT）。
2. backlog 里**没有**“RESOLVE-INTAKE 的 mid-session reroute / 用户求救
   escape hatch”R-item。Alice bad case 的 closure_criterion (c) 隐含
   提到这点，但没有形成独立工程 R-item。

**建议**：本 research 产出后，deliver-agent + human 把下面两个 R-item
追加到 `action_bank.md`（具体措辞由 deliver-agent 拟定）：

- `R-discover-verb-class-understanding-vs-acting-soft-signal`
  （`prompt_projection` candidate；本 case 第 1 层）
- `R-resolve-intake-escape-hatch-when-user-rejects-intake-flow`
  （`skill_state` + `prompt_projection` candidate；本 case 第 2/3 层）

---

# 5. Compounding-effect analysis（修复顺序约束）

| 顺序 | 干预 | 如果先做 | 如果不做 |
|------|------|----------|----------|
| **A (must be first)** | **缩窄/强化 DISCOVER understanding-vs-acting 判别** | 即便 RESOLVE 阶段没有 reroute，错分率下降直接消除大部分 UC-A→UC-H 死循环 | 单做 RESOLVE escape hatch = 在错的 UC 里反复让 LLM 自纠错；compounding 副作用：每次 escape 都消耗 turn budget，正确率 < 直接修 DISCOVER |
| **B (can be parallel)** | **追加 listing-context 在 DISCOVER 自动 lookup**（form_context 有 ad_id 时自动 emit `ad_status_observed`）| 解决 form_context 没 ad_id 时 DISCOVER 完全无 listing-state 证据的问题；本 case 是 form 没带 ad_id（只有 description "where's my ad"），所以 A 必须独立成立、不能依赖 B | 不影响 A 的修复，但 cs015 / fg5q / wmkb 等 cases 的 `ad_status_observed` 信号始终缺失 |
| **C (must be after A)** | **RESOLVE-INTAKE escape hatch** | 在错分率已经下降的基础上兜底，覆盖率合理 | 单做 C：会**鼓励 DISCOVER 偷懒**——“反正 RESOLVE 能 escape”——是 §1.5 forbidden-shape 的隐性版本 |
| **D (parallel, lowest priority)** | **escalation_reason canonicalize 兜底改为 explicit `bot_intent_uncertain`** | 不影响其他层；纯 observability | 不做 = trace 上失败模式继续被 `service_degraded` 掩盖 |
| **E (architecture-aligned, defer)** | `R-loosen-topic-uc-binding-llm-owned-drift` 整体落地 | 长期 cross-topic drift 一并解决 | M3+ 候选；不阻塞本 case 修复 |

**关键 compounding 反例**：如果**只**做 C（RESOLVE escape）而**不**做 A
（DISCOVER 加固），会出现的失败形态：

```
turn 1: hi
turn 2: why I can't see my advert → bot 错分 UC-H（A 没做）
turn 3: ad-2001
turn 4: why I can't see it
turn 5: [C 触发] bot：“看起来你想了解 removal 原因，我帮你回到 FAQ 路径”
turn 6: bot 进入 UC-A FAQ 路径，但已经消耗 5 turn / budget 15 的 1/3
turn 7-15: 正常 FAQ 流程，但因为前 5 轮被 UC-H intake 污染了 task_summary
            / accumulated_tool_results，bot 表现不稳定
```

→ 单做 C = 把错分变成“可恢复但成本高”，**没有从结构上消除问题**，且
违反 §1.5（用 escape hatch 弥补 semantic_planner 错误）。

---

# 6. Design alternatives

## Alt 1（强烈推荐）— DISCOVER verb-class soft-signal projection slot

**思路**：runtime 不做语义判断，而是把“当前用户消息的语法形态”
（interrogative “why/how/what/where” vs imperative “appeal/dispute/
refund/I want/please cancel/I demand”）作为**观察性 soft signal**
emit 到 projection，让 LLM 在 DISCOVER cue 里**显式参照**。

projection slot 设想（**只在 DISCOVER 阶段 emit**，按 Skill 声明门控）：

```yaml
# projection 字段
user_intent_verb_class:
  current_turn_shape: interrogative_understanding | imperative_action | mixed | unclear
  evidence_tokens:                  # 触发的 token（不是 keyword 匹配，是表层语法标记）
    - "why"
    - "?"                            # 句末问号
  uc_implication_hint:               # 仅当 candidate_use_cases 跨 understanding/acting 边界
    understanding_candidates: [UC-A, UC-FP]      # FAQ-path 候选
    acting_candidates: [UC-H]                    # INTAKE-path 候选
```

**实现层**：

- `ContextProjectionBuilder` 新增 helper `buildUserIntentVerbClass(...)`
  （≤ 80 行 Java），按 Skill `state_inheritance.soft_signal_via_projection`
  声明门控（沿用 M5 S4 模式）。
- 实现可以是**词法级而非语义级**——interrogative 标记 = 句首 wh-word OR
  句末 `?`；imperative_action 标记 = explicit action verb 词典
  （`appeal`, `dispute`, `refund`, `cancel`, `delete`, `report`,
  `repost`, `restore`）。**这是 §1.4 的“surface evidence projection”，
  不是 §1.5 的“semantic decision”**——LLM 仍然拥有“看到这些标记后该怎
  么办”的决策。
- `discover_triage.yaml` procedure 追加一段简短 cue（≤ 200 字符）：
  “When `user_intent_verb_class.current_turn_shape == interrogative_understanding`
  AND `uc_implication_hint.acting_candidates` is non-empty, prefer the
  understanding candidates and ask a focused clarifying question
  rather than committing to an acting candidate.”

**Trade-off**：
- ✅ §1.7-clean：词典/标记是 surface evidence projection，决策仍在 LLM。
- ✅ 与 M5 S4 Skill-declaration gating 模式天然契合，落地成本低。
- ✅ 同时帮到 cs015 / fg5q / wmkb 等所有 within-topic understanding-vs-
  acting cases。
- ⚠️ verb 词典本身可能扩张（“why” / “how” / “what about” / “for what
  reason” / 中文 “为什么” 等）；需要规定**词典只对应 surface 标记，不
  对应 UC**，否则就退化成 keyword→UC 路由（§1.7 红线）。
- ⚠️ 不解决 form_context 没带 listing-state 时 LLM 仍然可能误判的根本
  问题——这部分由 Alt 2 兜底。

## Alt 2 — DISCOVER 自动 `lookup_listing_or_ad` 取 `ad_status_observed`

**思路**：当 `form_context.topic_subject == "Ad Support"` AND
`form_context` 含可解析的 ad_id（包括用户在 turn 1/2 提到的 ad_id）
AND `discover_disambiguation_signals.ad_status_observed == null`，
runtime **自动** 在 DISCOVER 进入前 invoke `lookup_listing_or_ad`
工具，把结果灌进 projection。

**Trade-off**：
- ✅ 把现成 Sprint 33 cue 的有效率从“偶发”提到“当 ad_id 可解析时
  100% 触发”。
- ⚠️ Runtime auto-invocation 违反 §1.4 “tool schema” 边界的精神（工具
  调用是 LLM 决策），需要明确 spec 这种 auto-fetch 是 “projection
  enrichment”而非 “tool call”——边界微妙。
- ⚠️ 本 case **不能靠 Alt 2 修复**：用户的 form 是
  `description: "where's my ad"` + `topic_subject: "Ad Support"`，
  没有 ad_id；ad-2001 在 turn 3 才出现，那时 UC-H 已经 commit。
- 结论：**Alt 2 是 Alt 1 的补强，不是替代**；优先级低于 Alt 1，建议
  作为下一个 milestone 的 sub-sprint 候选。

## Alt 3 — RESOLVE-INTAKE escape hatch（新 tool `propose_uc_reroute`）

**思路**：给 `resolve_intake_collect_and_handover.yaml` 加一个新工具
`propose_uc_reroute(target_uc, reason)`，让 LLM 在观察到“用户拒绝
intake / 一直在问原始问题”时主动提出 reroute。Runtime 收到调用后转回
DISCOVER 阶段，把 `target_uc` 作为新 hint。

**Trade-off**：
- ✅ 是 closure_criterion (c) 的工程化形态。
- ❌ **不能单独做**（§5 compounding）；必须先做 Alt 1。
- ❌ 改动面更大：新工具 + 新 phase 转移路径 + control-policy 修改 +
  新 §7 stanza 需要论证不是 §1.7 violation。
- 建议：**作为 Alt 1 后续 sub-sprint**，**不在第一轮 fix 范围内**。

## Alt 4 — 强化 DISCOVER cue（纯 prompt edit）

**思路**：把 Sprint 33 cue 重写得更短、更突出，或在 procedure 顶部加
“Decision tree: 先判断 verb-class，再 commit”。

**Trade-off**：
- ✅ 改动最小（只动 `discover_triage.yaml` procedure）。
- ⚠️ Alice case 已经存在 5 个月（2026-05-16 surfaced），M1/M2/M3-Eval/
  M4/M5 五个 milestone 都没把它 close。证据强烈表明**纯 prompt cue
  对 deepseek-flash 类小/中模型的可靠性不够**。
- ⚠️ §1.6 evaluation rule：eval-pass-rate 升高需要 generalize；单点
  prompt 调优常常在 visible eval 涨、shadow eval 跌。
- 结论：**不推荐作为主方案**；可作为 Alt 1 落地时的附带 prompt 收敛。

---

# 7. Recommended option

**Alt 1（DISCOVER verb-class soft-signal projection slot）作为单
sub-sprint 主交付，Alt 4（DISCOVER cue 收敛）作为同 sub-sprint 的
prompt-side 配套。** Alt 2 / Alt 3 排队到下一个 milestone（先观察
Alt 1 + Alt 4 的回归效果再决定）。

**理由**：

1. **§1.4 / §1.7 同时满足**：projection slot 是 surface evidence
   （词法标记）而非 UC 决策；LLM 仍拥有“看到 interrogative + acting
   候选时怎么办”。
2. **复用 M5 S4 Skill-declaration gating 模式**：新 slot 只需在
   `discover_triage.yaml` 的 `state_inheritance.soft_signal_via_projection`
   里追加一项，gating 由 M5 S4 已经实现的 `skillDeclaresSoftSignal(...)`
   helper 接管，**几乎不动 Java 主路径**。
3. **覆盖率最广**：同一个 slot 帮到 Alice / 本 session / cs015 /
   fg5q / wmkb 五个 bad case 至少 3 个（具体覆盖率由 dev sub-sprint
   重跑评估）。
4. **compounding 顺序正确**：是 §5 表里的 “A (must be first)”。
5. **不动 Skill registry / intake registry / control-policy**：scope
   小，回归面可控，单 sub-sprint 可结。

---

# 8. Scope split + delivery priority suggestion

**注**：最终 milestone / sub-sprint 拆分由 deliver-agent + human 决定；
以下是 research-agent 的建议形状。

## 优先级 P0 — 本 case 主修

**Sub-sprint S-A：DISCOVER verb-class soft-signal slot + prompt 收敛**

- 范围：
  - `ContextProjectionBuilder.java` 新增
    `buildUserIntentVerbClass(...)` helper + projection emit（按 Skill
    `soft_signal_via_projection` 声明门控）
  - `discover_triage.yaml`：`state_inheritance.soft_signal_via_projection`
    追加 `user_intent_verb_class`；procedure 末尾追加 ≤ 200 字 cue
  - **不动** Java phase 状态机、Skill registry、IntakeFieldsRegistry、
    use-case-registry.yaml、control-policy
- Tests：
  - `ContextProjectionBuilderTest` 新增 5-8 个 verb-class 标记 case
    （interrogative / imperative / mixed / unclear；空 message；中文
    句式可选）
  - `discover_triage` integration test 验证 Sprint 33 cue + 新 cue 一起
    工作
- 评估：
  - 把本 session 加入 `case_specs/bad_cases/`（要么作为
    `alice_uc_a_uc_h_misclass` 第二证据，要么新开 case，由 deliver-
    agent 决定）
  - real-LLM rerun Alice + cs6252 + cs015 + fg5q + wmkb 五条 case，
    closure_criterion 至少 PASS Alice + cs6252

**§7 stanza 预填**：

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
Constitution's drift / topic-shift bullet (§1.3 LLM owns) governs the
behaviour the new projection slot enables; the projection itself sits
inside the Runtime's "trace and eval contract" responsibility (§1.4).

**Semantic hardcode:** No semantic hardcode introduced. The new
`user_intent_verb_class` projection slot surfaces **surface lexical
markers** (interrogative wh-words, sentence-final '?', explicit action
verbs from a vocabulary list) — NOT a UC routing decision. The LLM
owns whether to act on the marker. No new keyword→UC mapping is added
to DriftDetector / RuntimeIntentClassifier / use-case-registry.yaml.
The action-verb vocabulary list is treated as `surface_evidence` per
§1.4 — extending it requires the same justification as extending
`risk-keywords.yaml`'s `target-use-case` field, NOT a §1.5 iteration-
rule trigger.

**Generalization coverage:** target = Alice (`alice_uc_a_uc_h_misclass`)
+ this session (cs6252_uc_a_uc_h_misclass_real_session, to be added).
Neighbor = cs015 / fg5q / wmkb (within-Ad-Support understanding-vs-
acting). Negative = explicit imperative-only sessions on
`Ad Support` topic (e.g. "I want to appeal ad-1001") must continue
routing to UC-H without the slot triggering a false-positive
clarifying question. Shadow = held-out within-topic understanding-vs-
acting traces, not visible to the dev agent. Counts deferred to the
case-family expansion in the same sub-sprint or next.
```

## 优先级 P1 — 下一轮 milestone 候选（不在 P0 sub-sprint 内）

**Sub-sprint S-B：DISCOVER 自动 listing-state lookup（Alt 2）**

- 触发条件、scope、§7 stanza 由下一轮 research-agent + deliver-agent
  规划。本 case 不依赖 S-B 修复。

**Sub-sprint S-C：RESOLVE-INTAKE escape hatch（Alt 3）**

- 只在 S-A 落地后**观察到剩余 dead-loop session** 时启动。如果 S-A
  把错分率降到可接受水平，S-C 可能不需要做。

## 优先级 P2 — 配套 observability

**Sub-sprint S-D（可与 S-A 同周交付，纯 observability）：把 LLM emit
的 non-canonical escalation_reason 兜底从 `service_degraded` 改为
`bot_intent_uncertain`，并在 trace 显式高亮**

- 让 trace 上不再用 `service_degraded` 掩盖 mis-classification 后果。
- 改动 `PhaseEvaluator.canonicalize(...)` + 23 值 canonical enum 扩
  1 项（需 docs 同步 `customer_service_tool_spec_v0_3.md` + eval-side
  scoring）。
- 这个 sub-sprint 独立于 S-A，可以提前/延后；建议 deliver-agent 与
  S-A 同周交付以便观察新增 slot 的命中率。

---

# 9. Risk + compounding-effect analysis

| Risk | 严重度 | 缓解 |
|------|--------|------|
| **verb 词典扩张失控** → 退化为 keyword→UC | High | 词典严格只对应 surface 标记（lexical category），**永远不映射 UC**；UC implication 由 LLM 看到 projection 后自行决定。每次扩张词典在 §7 stanza 里明示是 `surface_evidence` 扩展。 |
| **新 cue 与 Sprint 33 cue 冲突 / 互相覆盖** | Medium | S-A dev 验证两段 cue 共存的 integration test；Codex per-sub-sprint review（本 sub-sprint 触发 §4.3 #2 §1.7-adjacent 触发器，建议 per-sub-sprint Codex）。 |
| **shadow regression**：DISCOVER 提问率上升导致用户体验下降 | Medium | §5.6 acceptance bar + 新 negative case（explicit imperative 必须直接 commit，不许 over-clarify）。可参考 `docs/solutions/discover_overclarify_and_search_catch22.md` 的 over-clarify 教训。 |
| **non-canonical `bot_intent_uncertain` enum 扩张影响下游** | Low | S-D 与 S-A 解耦；S-D 触发 docs + eval scoring 同步。 |
| **deepseek-flash 仍可能忽略新 cue** | Medium | 真实 LLM rerun 是 §5.6 主要 gate；若 Alice + cs6252 closure_criterion 在多次重跑下不稳定（< 80%），升级到 Alt 2 + Alt 3 组合。这是 IMPROVING 状态的合法 closure，不是 FAIL。 |
| **本 case 与 `R-loosen-topic-uc-binding-llm-owned-drift` 撞车** | Low | 两者层级不同：本 case 是 within-topic，R-item 是 cross-topic。S-A 的 `user_intent_verb_class` slot 设计上要与未来的 cross-topic widening 兼容（不要 hardcode 当前 topic-bound candidate 集）。 |

**反 §5 顺序的修复路径（do NOT do）**：

1. ❌ 跳过 Alt 1 直接做 Alt 3：把错误 UC 内部的 escape hatch 当主修，
   §1.5 forbidden-shape 隐性版本。
2. ❌ 在 `use-case-registry.yaml` 给 UC-H 加 “require explicit appeal
   intent verb” 字段：是 keyword→UC 的另一种伪装，§1.7 forbidden line。
3. ❌ 在 `RuntimeIntentClassifier` 加 “UC-A understanding pattern” regex：
   §1.5 + §1.7 双红线；且 `R-loosen-topic-uc-binding-llm-owned-drift`
   的方向是缩小这个 component，不是加它。

---

# 10. Observability / trace / report implications

S-A 落地后需要的 observability 更新：

- **Admin trace UI**（M5 S2 已落地的 `LlmInvocationsPanel`）：projection
  里新增 `user_intent_verb_class` 字段会自动渲染（panel 用
  `@JsonUnwrapped` 直接展示完整 projection）。**无新工程**。
- **`report.html`**（M5 S1 已重排为 four-tier verdict）：每个 case 的
  per-turn trace 现在能看到 `user_intent_verb_class` 是否被 emit，方便
  human-review 判断为什么 LLM 做出当前选择。**无新工程**。
- **`docs/diagnostics/`**：建议 S-A close 时新增
  `docs/diagnostics/discover-verb-class-projection-trace.md` 记录前
  3-5 个 real-LLM session 的 trace 证据（参考 M5 S3 的
  `m5-s3-projection-consumption-map.md` 体例）。
- **eval scoring**：四层 tier 框架已经处理（programmatic + LLM judge），
  不需要新评分维度。

S-D（escalation_reason 新枚举值）会触发：

- `customer_service_tool_spec_v0_3.md` 同步新增 `bot_intent_uncertain`。
- `eval_interactive` 评分映射同步。
- `release_gate.md` 不受影响（属 observability 范畴）。

---

# 11. 直接回答用户的两个问题（summary）

**Q1: 为什么 csagent 确认 ad-2001 之后不直接回答我的问题，反而问我为什么 appeal？**

因为：

1. 你 turn 2 的 “why I can't see my advert” 在 DISCOVER 阶段被 LLM
   错分为 UC-H（**Ad Removal Appeal**，intake-path），而不是 UC-A
   （**Ad Status & Visibility**，FAQ-path）或 UC-FP（**Correct
   Deletion Explanation**，FAQ-path）。LLM 自己的 reasoning 写的是
   “ad status visibility inquiry, which maps to UC-H”，**这句话本身
   是错的**——“ad status visibility”就是 UC-A 的字面名称。
2. UC-H 一旦 commit，runtime 通过 `SkillRegistry.select(RESOLVE, UC-H)`
   切到 `resolve_intake_collect_and_handover.yaml` 这个 Skill，它的
   procedure 写死“按 `intake_state.fields_remaining` 顺序问 missing
   字段”。UC-H 的三个必填字段是
   `ad_id_or_listing_url` / `registered_email` / `stated_reason_or_context`
   （定义在 `IntakeFieldsRegistry.java:59`）——所以你 turn 3 给 ad ID
   后，turn 4 必然被问 “请告诉我 ad removal appeal 的 reason”。
3. 进入 RESOLVE-INTAKE 之后，LLM 在 projection 里看不到“其实还有
   UC-A / UC-FP 这两个 plausible 候选”这个信号（M5 S4 把这两个 slot
   gate 为 DISCOVER-only），所以 LLM 没有回头路。
4. 你 turn 4 重复问 “why I can't see it”——这本该是“UC 错了”的强信号，
   但 Skill procedure 完全没设计“re-check intent”分支，Java 侧的
   reroute 又是 keyword-based，匹配不到你的措辞，于是一路困到 turn
   5 escalation。

**简而言之：DISCOVER 错把“求解释的 why”读成了“要发起 appeal”，UC 一旦
固化就无路可退。** 这不是孤例，bad-case suite 里就有同形态的
`alice_uc_a_uc_h_misclass`。

**Q2: csagent 处理用户问题的 process 流程是在哪里定义的？后续是通过
skill yaml 中维护么？**

是，**自 M2（Sprint 36–41）起**，per-phase × per-UC 的“LLM 这一回合
该做什么”已经搬到 6 份 `server/src/main/resources/skills/*.yaml`，由
`SkillRegistry.select(phase, useCase)` 选择，`PhaseEvaluator.composeSkillPhasePlan(...)`
把 Skill 字段 + 占位符替换组装成 `PhasePlan` 喂给 LLM。

**但** 完整的 process 流程横跨多层（按 §1.3 / §1.4 边界故意分层）：

- **Skill YAML 拥有**：procedure（in-context 提示主体）、tools_required、
  required_context_keys、state_inheritance、valid_terminal_outcomes、
  guardrails、critical_steps。
- **Java + config YAML 拥有**：phase 状态机、phase 转移合法性、UC 目录、
  intake 字段定义、tool 实现、tool 派发策略、drift / reroute 检测、
  per-turn projection 构建、escalation_reason canonicalize。

具体见本文 §1 的完整分层表。要修本 case，scope 落在
`ContextProjectionBuilder.java` + `discover_triage.yaml` + 测试，
**不动** Skill registry / IntakeFieldsRegistry / use-case-registry /
control-policy。

---

# 12. Hard fences + non-goals（per `iteration_governance.md` §7）

**本 proposal 不做**：

- 不动 `resolve_intake_collect_and_handover.yaml` 的 procedure / tools_required（保持 M2 freeze）
- 不动 `use-case-registry.yaml` 的 UC 定义 / topic-subject 映射（避免与 `R-loosen-topic-uc-binding-llm-owned-drift` 抢 scope）
- 不动 `IntakeFieldsRegistry.java` 的 UC-H 必填字段集合
- 不引入新的 Java keyword→UC 路由（`RuntimeIntentClassifier` 保持现状）
- 不修改 phase 状态机 / control-policy（不允许 RESOLVE → DISCOVER 转移除非走现有 reroute 路径）
- 不修改 Sprint 39 S1 `must_cite_source` 的 frozen scope（M2 §6 #4）
- 不动 `escalate.yaml` / `terminal.yaml` / `confirm.yaml`
- 不引入 Tier-0 候选

**Out of scope for the recommended sub-sprint S-A**：

- DISCOVER 自动 `lookup_listing_or_ad`（Alt 2，下一轮）
- RESOLVE-INTAKE escape hatch（Alt 3，更后续）
- `R-loosen-topic-uc-binding-llm-owned-drift` 落地（M3+ 候选）
- escalation_reason 兜底改名（S-D，独立 sub-sprint）

---

# 附录 — 关键引用清单（HEAD 验证）

| 路径 | 行号 | 引用内容 |
|------|------|----------|
| `server/src/main/resources/config/use-case-registry.yaml` | 2-7 | UC-A "Ad Status & Visibility" 定义；path=FAQ |
| `server/src/main/resources/config/use-case-registry.yaml` | 38-43 | UC-FP "Correct Deletion Explanation"；path=FAQ |
| `server/src/main/resources/config/use-case-registry.yaml` | 50-55 | UC-H "Ad Removal Appeal"；path=INTAKE；allow-bot-resolution=false |
| `server/src/main/resources/skills/discover_triage.yaml` | 20 | Sprint 33 understanding-vs-acting cue 全文 |
| `server/src/main/resources/skills/discover_triage.yaml` | 30-32 | `state_inheritance.soft_signal_via_projection` 声明 |
| `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` | 5-10 | `applicable_use_cases: [UC-G, UC-H, UC-I, UC-J, UC-K]` |
| `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` | 22 | intake procedure 全文 |
| `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml` | 54-69 | UC-H critical_steps (`uc-h-intake-complete-before-handover`) |
| `server/src/main/java/com/gumtree/csagent/service/runtime/IntakeFieldsRegistry.java` | 53-67 | `REQUIRED_FIELDS_BY_UC` UC-H 三字段定义 |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 34 | `INTAKE_UCS = Set.of("UC-G", "UC-H", "UC-I", "UC-J", "UC-K")` |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 78-95 | `canonicalize(...)` 把 non-canonical escalation_reason 兜底为 `service_degraded` |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 319 | `evaluate(...)` phase switch |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 364 | `skillRegistry.select(phase, activeUc)` |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 444-463 | `composeSkillPhasePlan(...)` Skill → PhasePlan 装配 |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 405-433 | `candidate_use_cases` 按 Skill `requiredContextKeys` 门控（M5 S4） |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 435-468 | `alternate_candidate_use_cases` 按 Skill `softSignalViaProjection` 门控（M5 S4） |
| `server/src/main/java/com/gumtree/csagent/service/runtime/RuntimeIntentClassifier.java` | 160-269 | `classify(...)` keyword-based reroute 路径 |
| `server/src/main/java/com/gumtree/csagent/service/runtime/DriftDetector.java` | 55-94 | `detect(...)` keyword-based 风险/UC 切换 |
| `server/src/main/resources/config/risk-keywords.yaml` | 57-63 | DriftDetector UC-H 关键词（`ad removed` / `appeal` 等） |
| `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` | 4-21 | 同形态 bad case `failure_shape` + `layers_involved` |
| `docs/action_bank.md` | 685 | `R-loosen-topic-uc-binding-llm-owned-drift`（架构同源 R-item） |
| `docs/action_bank.md` | 752 | `R-bad-case-suite-uc-ghij-seed-from-real-sessions`（本 session 是新种子） |
