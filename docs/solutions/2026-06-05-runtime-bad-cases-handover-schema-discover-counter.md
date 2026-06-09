---
title: Runtime bad-case 调查（c1–c17）— 工具 schema / DISCOVER 计数 / observability / premise projection / citation 格式 / corpus 治理 / intake partial-stash
doc_tier: proposal
status: partial
implementation_status: partial
source_of_truth: this file (open R-items); docs/sprints/sprint-078-handoff.md (R1/R2/R4 shipped state)
authored_by: research-agent
authored_date: 2026-06-05
last_updated: 2026-06-06 (human accepts post-ship reclassification + selects Sub-sprint C-1 / C-2 split packaging)
mode: bad-case-driven
notes: >
  Bad-case 输入来自 human 提供的 17 条真实 trace（c1–c17）。c1–c9 是
  S-Auto-23 / Sprint 078 前的输入；c10–c17 是 S-Auto-23 ship 后的 smoke
  evaluation 观察。R1.a + R2.a + R4.a 已在 S-Auto-23 落地（commits
  a873d18 / 840a5e2 / 247da11；Codex APPROVE_S_AUTO_23）；R3 / R5 / R6 /
  所有 OBS-S* 待跟进。本 proposal 现作为：(a) S-Auto-23 验证记录 +
  (b) Sub-sprint B 与后续 OBS autoloop 的 backlog 索引 + (c) 新的两个
  post-ship findings（R2.a#5-ext + R7 intake partial-stash）。
---

# Post-ship status banner（2026-06-05）

**S-Auto-23 / Sprint 078 已 ship R1.a + R2.a + R4.a**（M-Auto-6 Sub-sprint A；Codex `APPROVE_S_AUTO_23 / blocking_count=0`；archive `docs/sprints/sprint-078-handoff.md`）。本文档原 §6 推荐的 6 项 R-item 中：

| R-item | 状态 | 证据 |
|---|---|---|
| **R1.a** | ✅ SHIPPED & VALIDATED | commit `a873d18`；`ContextProjectionBuilder.java:262-280` (schema) + `:427-440` (per-UC hint)；c11/c15/c16 first-call success 即验证 |
| **R2.a #3** | ✅ SHIPPED | commit `840a5e2`；live `AgentRunLoopImpl` DISCOVER counter 接线（4 严格条件）+ 8 anti-误杀 tests |
| **R2.a #4** | ✅ SHIPPED | commit `a873d18`；`budgets.clarification {used, max}` 投影（DISCOVER-only soft signal）|
| **R2.a #5** | ✅ SHIPPED **（partial — RESOLVE intake 未覆盖）** | commit `247da11`；phase-aware `mapBudgetToEscalationReason` 重映射，**但严格限定 `phase==DISCOVER + action ∈ {answer, clarify}`**；c14 暴露 RESOLVE-phase intake UC free-text 重复仍标 `turn_budget_exhausted` → 新增 **R2.a#5-ext**（见下） |
| **R4.a** | ✅ SHIPPED（enabler；semantic 闭环待 OBS-S1） | commit `a873d18`；`customer_context_status` enum + `ad_reference {form_ad_id, listing_lookup}`；c12/c16 仍现 c7 模式属预期（sprint_objective 明示 "c7 closure requires R4 + OBS-S1 together"） |
| **R3.a/b/c** | ⏳ PENDING Sub-sprint B（S-Auto-24 / Sprint 079，已草拟 `compact/sprint-079-dev-prompt.md`） | c2/c8/c10/c17 仍存在 |
| **R5** | ⏳ PENDING | c13 仍存在 |
| **R6** | ⏳ PENDING | c12/c17 仍 hit `(temp)` article |
| **🆕 R2.a#5-ext** | 🆕 NEW post-ship finding | c14 RESOLVE-phase intake clarification 重复仍 mislabel；commit `247da11` anti-误杀 invariant #12 显式排除 RESOLVE/INTAKE；需 narrow-extension |
| **🆕 R7** | 🆕 NEW post-ship finding | c14 intake partial-fields 在 turn 间丢失 —— `persistInlineIntakeFields` 只在 `request_handover` 触发；LLM 收集中段无 stash 路径；R1 schema 修了发送契约但没修跨 turn 累计 |
| **OBS-S1/S2/S3/S4/S6/S7** | ⏳ PENDING autoloop | 全部待 yaml 调措辞或 LLM 行为优化；c10/c11/c12/c14/c15/c16/c17 残留 |

**post-ship case 重新分类**：c11/c15/c16 = R1 validation evidence；c12/c16 = R4 enabler shipped 等 OBS-S1 yaml；c13 = R5 待 ship；c14 = 两个真正的新发现（见 §4.7 + §4.8）；c10/c17 = R3.c 待 ship + OBS-S4；c8 = R3.b 待 ship。

---

# Executive summary

Human 提供的 **17 条 trace（c1–c17）** —— c1–c9 触发 S-Auto-23（已 ship R1/R2/R4），c10–c17 是 ship 后 smoke evaluation 观察。post-ship 状态下 R-item 调整为 **8 项**（原 6 项 + 新 2 项）+ **7 项 OBS（defer autoloop）**。

## R-item 总览（post-ship 状态）

| R | 描述 | Layer | 状态 | 触及 case |
|---|---|---|---|---|
| **R1.a** | `request_handover` 工具 schema 暴露 `intake_fields` + 每 UC required-fields hint | `prompt_projection` | ✅ SHIPPED (S-Auto-23) | c1/c5/c6 fix；c11/c15/c16 first-call success 验证 |
| **R2.a** | DISCOVER clarification counter live-path 接线 + budget projection + escalation_reason mapping fix | `infra` + `skill_state` | ✅ SHIPPED (S-Auto-23) | c3 fix；c9 mapping fix（仅 DISCOVER）|
| **R3** | admin trace observability：(3a) session list；(3b) dedup ToolEvent UI 折叠；(3c) informational guard rejection 与 blocking error 区分 | `infra`（observability） | ⏳ PENDING Sub-sprint B（S-Auto-24，已草拟） | c7(3a)、c8(3b)、c2/c10/c17(3c) |
| **R4.a** | entity premise projection slot：`customer_context_status` enum + `ad_reference` 结构化字段 | `prompt_projection` | ✅ SHIPPED (S-Auto-23；enabler；闭环需 OBS-S1) | c7/c12/c14/c15/c16 signal 已 surface |
| **R5** | `ResolveArticleTool` 返回 `display_citation`（URL 优先）；skill yaml `cite_token_field` 切到 `display_citation` | `infra` + `prompt_projection`（skill yaml 1 行） | ⏳ PENDING | c13 |
| **R6** | corpus 治理：`(temp)` template article + 含 `XXXXXXXXX` 占位文章不应 surface 给 autonomous bot；data 加 `bot_visible: bool` + SearchKnowledgeTool 过滤 | `infra`（data + tool 过滤） | ⏳ PENDING | c7/c12/c17 |
| **🆕 R2.a#5-ext** | R2.a#5 phase-aware 映射的 narrow extension：把 `phase ∈ {DISCOVER, RESOLVE} AND active_use_case ∈ {UC-G/H/I/J/K} AND lastAction ∈ {answer, clarify}` 也重映射到 `clarification_budget_exhausted`；RESOLVE 非 intake UC + 任何工具调用重复依然 `turn_budget_exhausted`（保留 anti-误杀 #12 的精神，只是扩展到 intake free-text）| `infra`（mapping function 单行扩展） | 🆕 NEW post-ship | c14 |
| **🆕 R7** | intake_fields **partial-stash mechanism**：runtime 增加一条让 LLM 跨 turn 累计 intake field 的合法路径，不触发 handover validator；推荐 (a) 新工具 `update_intake_fields(fields={...})` 无副作用纯持久化，或 (b) 扩展 `persistInlineIntakeFields` 也接受 `classify_use_case` 等其它 tool call 上的 `intake_fields` 副参数 | `skill_state` + `infra` | 🆕 NEW post-ship | c14 |

## OBS-item 总览（defer to autoloop）

| OBS | 描述 | Layer | 触及 case |
|---|---|---|---|
| **OBS-S1** | UC-A / **UC-H / UC-J** 等 skill procedure 缺 "verify entity context before answering / collecting"（对标 UC-FP `consult-moderation-context-on-removal-explanation` line 114-128 是有的）| `prompt_projection`（skill yaml）| c7, c12, c16 |
| **OBS-S2** | DISCOVER ad_status disambiguation cue 过度保守 | `prompt_projection`（discover_triage.yaml）| c9 |
| **OBS-S3** | FAQ grounded answer fabrication（c8 turn 6 "My Account > Settings > Contact Preferences" 不在 article 正文）；已对应 `D-faq-grounded-resolve-bypass`（action_bank §4） | `semantic_planner` | c8 |
| **OBS-S4** | `record_outcome` 在 RESOLVE phase 过早提交；guard 正确，**LLM 行为问题（n=3 across c2/c10/c17）**；与 R3.c trace UI 区分不冲突 | `semantic_planner` | c2, c10, c17 |
| **OBS-S5** | cross-turn rank-1 first refinement 重复 query | by-design floor | c4 |
| **OBS-S6（NEW）** | intake procedures（UC-J / UC-I / UC-H）问用户过度字面 —— 应从 user 自由文本中推断 `report_type` / `dispute_reason` 等字段；只在真正缺失或歧义时再问 | `prompt_projection`（intake skill yaml）| c11, c14, c15 |
| **OBS-S7（NEW）** | bot 在 escalate 之前不汇总 finding 给 user（c10）；用户体验是"agent 搜了一圈然后 'Let me connect you with a specialist' 没有内容" | `semantic_planner`（也可调 yaml 软提示） | c10 |

## 关键 invariants（post-ship 更新）

- M-Auto-5 已 CLOSE（commit `6236941`，Class A）；§5.8 framework-defect priority 已解除；M-Auto-6 ACTIVE。
- **R1.a + R2.a + R4.a 已在 S-Auto-23 ship**（详见 §0 banner）。
- **剩余待跟进**：R3 (Sub-sprint B 已草拟)、R5、R6、R2.a#5-ext、R7、所有 OBS-S\*。
- pure infra（exempt §7 stanza）：R3、R6、R2.a#5-ext。需 §7 stanza：R5（含 skill yaml 1 行改）、R7（影响 LLM 工具列表）。
- **推荐打包（human 2026-06-06 决定）**：
  - Sub-sprint B（已草拟）：R3.a + R3.b + R3.c
  - **Sub-sprint C-1**：R7 + R2.a#5-ext —— 同属 intake/clarification runtime contract；R7 是 OBS-S6 的 runtime enabler，高优先
  - **Sub-sprint C-2**：R5 + R6 —— 体验改善 + corpus 治理；可并行，不阻塞 OBS-S6 前置能力
  - 不打包成一个大 Sub-sprint C；小 sub-sprint 利于因果归因 + anti-误杀 review
- **autoloop 排期（human 2026-06-06 决定）**：
  - OBS-S1 / OBS-S2 / OBS-S7 可以在 R1/R2/R4 后开始评估是否进入（不依赖未来 R-item）
  - **OBS-S6 必须等 R7 ship 后再开** —— 否则 LLM 推断出字段也无法可靠持久化，autoloop 调 yaml 无效

---

# 1. Case-by-case 现象 + 根因 + R-item 对应表

| Case | trace | 一句话现象 | code-grounded 根因 | R-item / OBS |
|---|---|---|---|---|
| **c1** | `e1f844cb-712...` | UC-J 4× `intake_required_fields_missing_for_intake_complete` reject；session 始终未 handover | `ContextProjectionBuilder.java:200-260` 投影 schema 无 `intake_fields` 字段；validator 设计是 "args 持久化 → validate"（`AgentRunLoopImpl.java:486-494`）；LLM 因无 schema contract 首次不传 | **R1** |
| **c2** | `f070bf75-67a...` | `progressive_resolve_record_outcome_premature` | `ResolveDispositionEvaluator.java:160-186` 正确拒绝 phase≠CONFIRM/CLOSE 的 resolve outcome；guard 行为符合 §1.4 | **OBS-S4** + **R3.c**（trace UI 区分） |
| **c3** | `d56fd1e6-510...` | DISCOVER 2 turn 输出逐字相同 clarification | `BudgetChecker.maxClarificationRounds` counter 在 live `AgentRunLoopImpl` 路径不递增；live 路径无 identical-clarification 保护 | **R2** |
| **c4** | (human 误粘贴同 c3) | search_knowledge paraphrase | rank-1 first refinement 是 M-Auto-3 §11 OBSERVATION-only floor | **OBS-S5** |
| **c5** | `c6f60c19-3fc...` | UC-J 同 c1，第二次带 `intake_fields` 成功 | 同 c1 | **R1** |
| **c6** | `3bd52cf6-b14...` | UC-I 同 c1，第二次成功 | 同 c1 | **R1** |
| **c7** | (admin 找不到；form 无 ad_id) | bot 没问 ad_id 就以"your ad was removed"回答 user | (a) `FormContextIngestionService.java:109` 仅 `ad_id != null` 时自动 lookup → LLM 无 "premise unverified" 信号；(b) `resolve_faq_grounded_answer.yaml` critical_steps 无 verify-context 步；(c) admin UI 未显示此 case；(d) FAQ article `ka41r000000LIEJAA4` 是含 `XXXXXXXXX` 占位的 CS template | **R3.a**（admin）+ **R4**（premise）+ **R6**（temp article filter）+ **OBS-S1**（procedure） |
| **c8** | `e296c331-401...` | (a) turn 2-3 调 search+resolve 但 reply 是 "I'm looking into..." placeholder；(b) turn 6 给 "My Account > Settings > Contact Preferences" 用户怀疑 fabricate；(c) 多次同 source_id 的 0ms 重复 tool event | (a) projection 投递 article 完整 description 正确（`ResolveArticleTool.java:95-110` 投 `description` 字段；`ContextProjectionBuilder.java:969-976`）→ placeholder 是 LLM stylistic / `ActionParser.java:72` empty-reply fallback；(b) **确认 fabricate**：article 正文含 "Your Contact Details / phone box"（turn 5 grounded）但不含 "My Account > Settings > Contact Preferences"（turn 6 ungrounded）= `D-faq-grounded-resolve-bypass`；(c) A1 dedup 正确工作（`AgentRunLoopImpl.java:580-619`，0ms cache hit），`TraceViewer.tsx:399-471` 不识别 `deduplicated` flag | **R3.b**（dedup UI）+ **OBS-S3**（fabricate） |
| **c9** | `d98c704f-7fa...` | 4 turn 都在 DISCOVER，turn 4 user 清晰回主线时 runtime inject `turn_budget_exhausted` | (a) DISCOVER turn 1 未 commit UC-A，与 `discover_triage.yaml:44-45` 过度保守的 ad_status disambiguation 相关 → OBS-S2；(b) bot 重复 disambiguation 同 c3 → R2；(c) `ControlKernel.java:305-308` `mapBudgetToEscalationReason` 默认 fall-through 把 `max-repeated-same-action` 等都映射到 `turn_budget_exhausted`，label 误导 | **R2** + **OBS-S2** |
| **c10** | `e82c8da3-70c...` | UC-D turn 2 调 search+resolve → `record_outcome` 报 `progressive_resolve_record_outcome_premature` → bot 立刻 `request_handover` 但 user-facing reply 只是 "Let me connect you with a specialist" | (a) record_outcome guard 同 c2（OBS-S4 + R3.c）；(b) bot 拿到 article 但无 user-facing summarize → semantic（OBS-S7） | **R3.c** + **OBS-S4** + **OBS-S7** |
| **c11** | `7d20682a-c62...` | UC-J bot 问 user "what type of safety issue"，即使 user 描述已含 scam / 未到货 / 银行转账 / 卖家失联 | UC-J intake skill 字面收 `report_type` field；LLM 没有 procedure 提示从 free text 推断 → semantic；first call 带 `intake_fields` 成功 → 验证 R1 设计前提 | **R1**（已验证）+ **OBS-S6** |
| **c12** | `745878f9-a70...` | 同 c7：bot 没 verify ad_id 就回答 + (temp) 模板 article 被复述成 user-specific 答案；user 持续追问 7 turn 然后 escalate `faq_miss_threshold_exceeded` | 同 c7 根因；(temp) article `ka41r000000LIEJAA4` 命中 → R6 触发 | **R4** + **R6** + **OBS-S1** |
| **c13** | `49f7b981-ec0...` | bot 回答用 `(Source: ka4P200000003sLIAQ)` 而 article 实际有 `canonical_url` | `resolve_faq_grounded_answer.yaml:44` 配的是 `cite_token_field: source_id`；ResolveArticleTool 返回的 `source_url` 字段未被 surface 给 LLM 用于 cite token | **R5** |
| **c14** | `307310c8-b52...` | UC-J 报 listing #12345 不安全；bot (a) 问 user 是何种 safety issue；(b) turn 3 又问已给的 listing_id；(c) 不验证 listing 是否存在；最后 `turn_budget_exhausted` | (a) 同 c11 OBS-S6；(b) `skill_state` 多 turn 状态丢失 —— UC-J 的 IntakeFieldExtractor 不从 free text 抽取，session.intakeFields 直到 LLM call request_handover 才会 persist → 同 R1 + OBS-S6 互动；(c) listing entity 同 c7 ad 模式 → R4 扩展；(d) label 同 c9 R2 mapping fix | **R1** + **R2** + **R4** + **OBS-S6** |
| **c15** | `83b09eec-668...` | UC-I refund：user 已说"courier missed window + package not arrived"；bot 仍问 dispute_reason；不验证 order 存在 | (a) 同 c11 OBS-S6；(b) order_id 同 c7 ad 模式 → R4 扩展；(c) 最终 escalate 携带 intake_fields，验证 R1 设计 | **R1** + **R4** + **OBS-S6** |
| **c16** | `7eda6c17-b0b...` | UC-H ad appeal：bot 拿到 form 提供的 ad_id (AD-29447) + email，但**不 lookup 这个 ad 是否真被删及删除原因**，直接问 user "tell me why you believe removal was a mistake" 然后 escalate | `resolve_intake_collect_and_handover.yaml:54-69` UC-H critical_steps 只检 field 完整性（`ad_id_or_listing_url` / `registered_email` / `stated_reason_or_context`）、**无 "lookup ad before asking user to justify" 步**；对标 UC-FP `consult-moderation-context-on-removal-explanation`（line 114-128 in resolve_faq）是有的 → OBS-S1 扩展到 UC-H | **R4**（projection 信号）+ **OBS-S1 extended**（UC-H procedure） |
| **c17** | `7b714c21-f1e...` | UC-A 有 ad_id (AD-2001)；bot 给了 substantive 答案；中途 `record_outcome` 报 premature error；user 追问具体 policy；最终 escalate `faq_miss_threshold_exceeded` | (a) record_outcome guard 同 c2/c10（n=3 OBS-S4 + R3.c）；(b) bot 找不到 specific policy = corpus gap → 已有 `R-corpus-coverage-audit-per-uc` (action_bank §5.2)；(c) **此 case 是 c7/c12 的 happy-path 对照** —— form 有 ad_id 时流程合理，证明 R4 surface 信号有效后即可消除 c7/c12 类问题 | **R3.c** + **OBS-S4** + 已有 `R-corpus-coverage-audit-per-uc` |

---

# 2. Layer classification per R-item / OBS（按 `iteration_governance.md` §3.2）

| 标签 | §3.2 决策 | layer |
|---|---|---|
| R1 | Q3 projection 缺信息（schema 不声明字段） | `prompt_projection` |
| R2 | Q4 multi-turn 状态丢失 + Q1 wiring infra | `infra` + `skill_state` |
| R3.a/b/c | Q1 扩展 observability | `infra`（observability） |
| R4 | Q3 projection 缺 entity premise 信号 | `prompt_projection` |
| R5 | Q3 projection 缺更优 cite token field | `infra` + `prompt_projection` |
| R6 | Q1（data 治理 + tool 过滤） | `infra`（data + tool 层） |
| OBS-S1 | Q5 LLM 行为；fix 在 skill yaml 增 critical_step | `prompt_projection` |
| OBS-S2 | Q5 + Q3 混合 | `prompt_projection` |
| OBS-S3 | Q5 即便 projection 正确仍 fabricate | `semantic_planner` |
| OBS-S4 | Q5 LLM 时机错；guard 正确 | `semantic_planner` |
| OBS-S5 | by-design floor | — |
| OBS-S6 | Q5 LLM 太字面；fix 在 yaml | `prompt_projection` |
| OBS-S7 | Q5 LLM 不汇总；fix 在 yaml | `semantic_planner` |

---

# 3. Coverage check（vs `action_bank.md` + `milestone_objective.md`）

| 提议 R / OBS | 已有覆盖？ | 处置建议 |
|---|---|---|
| R1 | 无直接 R-item；与 `R-uc-k-intake-complete-case-id-binding` (action_bank §5.2) 互补 | NEW R-item |
| R2 | 与 M-Auto-6 Cluster C.3 "generic clarifier branch wasting opening turn" 重合 | 合并 Cluster C.3 |
| R3 | 与 Cluster B.1 "per_turn_trace 30-50% truncation" + C.2 "46f5b2e9 500 + double-send live UI" 重叠 | 合并 B.1 + C.2 |
| R4 | 无；与 OBS-S1 互补 | NEW R-item |
| R5 | 与 `R-canonical-url-corpus-curation` (action_bank §5) 部分相关（后者治 article 没有 URL；R5 治"有 URL 也不用"） | 互补，可并行 |
| R6 | 无 | NEW R-item |
| OBS-S1 | 无（UC-FP 已有该 step，但 UC-A/H/J 没有；建议同时扩展） | autoloop 目标 |
| OBS-S2 | 无 | autoloop 目标 |
| OBS-S3 | `D-faq-grounded-resolve-bypass` (action_bank §4) —— **c8 turn 6 是该 R-item 的第 2 个独立实例**；n=2 起结构性证据，deferred 状态可重评估 | 提升评估 |
| OBS-S4 | 无；n=3 (c2 + c10 + c17) | autoloop 目标 |
| OBS-S5 | M-Auto-3 §11 三类 taxonomy 已 canonical | 不开 R-item |
| OBS-S6 | 无 | autoloop 目标 |
| OBS-S7 | 无 | autoloop 目标 |

无 R-item 冲突。R3 合并 audit Cluster B.1 / C.2；R2 合并 Cluster C.3；R5 与 `R-canonical-url-corpus-curation` 互补。

---

# 4. Current-state survey（code-grounded）

所有引用路径在 HEAD（commit `021a86a`）验证。

## 4.1 R1 — `request_handover` intake-fields schema 缺口

| 关注点 | 位置 | 行为 |
|---|---|---|
| Schema 注册 | `ContextProjectionBuilder.java:116-120` | `buildRequestHandoverArgsSchema()` |
| Schema 字段 | `ContextProjectionBuilder.java:200-260` | 只声明 `escalation_reason`（required, 23-enum）+ `summary`（optional）；**无 `intake_fields`** |
| Args 持久化（先于 validator） | `AgentRunLoopImpl.java:486-494, 981-1002` | `persistInlineIntakeFields` 把 `call.arguments.intake_fields` 合并入 `session.intakeFields` JSONB |
| Validator | `SkillGuardrailDispatcher.java:265-298` `handleIntakeCompleteRequired` | 读 `session.intakeFields`（已合并 args），调 `IntakeFieldsRegistry.intakeComplete` |
| 必填字段 | `IntakeFieldsRegistry.java:53-67` | UC-G `[case_id]` / UC-H `[ad_id_or_listing_url, registered_email, stated_reason_or_context]` / UC-I `[transaction_reference, dispute_reason]` / UC-J `[report_target, report_type, description]` / UC-K `[case_id]` |
| Reject hint | `SkillGuardrailDispatcher.java:288-298` | "Ask the user for the missing intake fields above, then call request_handover with arguments.intake_fields populated." |

设计意图：**一次带完整 `intake_fields` 的调用应原子通过**。LLM 首次不传是因为 schema 没声明该字段。

## 4.2 R2 — DISCOVER clarification 计数 + budget mapping

| 关注点 | 位置 | 行为 |
|---|---|---|
| Counter 字段 | `BotSession.java:81` | `clarificationCount: Integer = 0` |
| 唯一 `+1` 调用点 | `PhaseEvaluator.java:880` | legacy 路径；live `AgentRunLoopImpl` 不递增 |
| Budget 检查 | `BudgetChecker.java:32-37` | `getClarificationCount() >= maxClarificationRounds()` → 永远 false |
| Config | `control-policy.yaml:1-7` | `max-clarification-rounds=2` / `max-bot-turns-faq=15` / `max-bot-turns-intake=10` / `max-total-bot-turns=25` / `max-repeated-same-action=2` |
| Projection counter | `ContextProjectionBuilder.java:351, 658` | LLM 看 `clarification_count` 永远 0 |
| `turn_budget_exhausted` emit | `ControlKernel.java:305-308`（runtime inject）+ `mapBudgetToEscalationReason` | 除 `max-clarification-rounds` / `max-faq-miss` 外全 fall-through 到 `turn_budget_exhausted` —— `max-repeated-same-action` 也走这条 → label 误导 |

R2 三个子项：(i) counter 接线；(ii) budget projection；(iii) `max-repeated-same-action` → `clarification_budget_exhausted` 重映射。

## 4.3 R3 — admin observability（3.a + 3.b + 3.c）

| 子项 | 位置 | 行为 |
|---|---|---|
| 3.a — Admin session 接口 | `DemoInspectionController.java:86` | `sessionRepository.findAll()` 无过滤；UI `SessionList.tsx` StatusBadge 根据 `handling_state` 显示，可能漏 ESCALATE 终态 |
| 3.a — Session 持久化 | `SessionManager.java:218, 255, 366` | 无条件 save |
| 3.b — Dedup ToolEvent 持久化 | `AgentRunLoopImpl.java:605-618` | A1 cache hit 时新建 `ToolEvent.deduplicated()`，完整存储 + `deduplicated=true` + `originalAtStep` |
| 3.b — UI trace 折叠 | `TraceViewer.tsx:399-471` | **不检查** `deduplicated` flag；所有 tool_call 无条件展开 |
| 3.c — 信息性 guard rejection（NEW） | `AgentRunLoopImpl.java:553-556` + `SkillGuardrailDispatcher` | record_outcome premature / 其它 informational rejection 在 trace 里以"error"红色徽标显示，与真实 error 不区分；用户看 c2/c10/c17 困惑 |

## 4.4 R4 — entity premise projection slot（扩展自 ad_id-only 到通用 entity）

| 关注点 | 位置 | 行为 |
|---|---|---|
| Form ingestion ad_id lookup | `FormContextIngestionService.java:78-79, 109` | 仅 `ad_id != null && !ad_id.isBlank()` 时触发 `lookup_listing_or_ad` |
| `get_customer_context` 触发 | `ContextProjectionBuilder.java:113`（注释） | INIT 时 email 在 form_context 才 trigger |
| 缺 entity 时的信号 | （无） | LLM-facing projection 不暴露 "ad_id missing → premise unverified" / "listing not found in DB" / "transaction_reference invalid" 等结构化状态 |
| 跨 UC 同模式 | c7（UC-A ad）、c14（UC-J listing）、c15（UC-I order）、c16（UC-H ad） | 同一 entity-verification 模式跨 4 个 UC |

R4 暴露的字段建议（结构化、零关键词）：

```
context_status:
  customer: loaded | missing_email | lookup_failed | lookup_skipped
  ad:       loaded | missing_id | not_found | lookup_failed | lookup_skipped
  listing:  loaded | missing_id | not_found | lookup_failed | lookup_skipped
  order:    loaded | missing_id | not_found | lookup_failed | lookup_skipped
```

LLM 自主决定如何利用（与 OBS-S1 yaml procedure 配合）。

## 4.5 R5 — citation 格式（URL 优先于 source_id）

| 关注点 | 位置 | 行为 |
|---|---|---|
| Tool result 字段 | `ResolveArticleTool.java:95-110` | 返 `title, source_id, source_url, description, ...`；**`source_url` 已存在** |
| Skill cite_token_field | `resolve_faq_grounded_answer.yaml:44` | `cite_token_field: source_id` —— 配置使用 source_id 而非 URL |
| 当 article 有 canonical_url 时 | c13 trace | bot 输出 `(Source: ka4P200000003sLIAQ)` 而非 URL |
| 当 article 无 canonical_url 时 | 部分 `(temp)` article + 38 个 corpus gap article | source_id 是唯一可用 token |

R5 实现思路（runtime-only）：

1. `ResolveArticleTool.java` 增加 `display_citation` 字段：优先 `source_url`，缺时 fallback `source_id`，可附 `title`
2. `resolve_faq_grounded_answer.yaml:44` 改 `cite_token_field: display_citation`（1 行 config）
3. Anti-误杀：`must_cite_source` guardrail（line 40-）的匹配逻辑 fallback 兼容 source_id（旧 trace + 旧 article）

## 4.7 🆕 R2.a#5-ext — RESOLVE-phase intake clarification mapping gap（post-ship 新发现）

`commit 247da11` ship 的 `mapBudgetToEscalationReason(bucket, phase, lastAction)` 严格 guard：

```java
if ("max-repeated-same-action".equals(bucket)
        && "DISCOVER".equalsIgnoreCase(currentPhase)
        && isFreeTextActionKey(lastAction)) {
    return "clarification_budget_exhausted";
}
return mapBudgetToEscalationReason(bucket);  // → "turn_budget_exhausted"
```

**Anti-误杀 invariant #12（commit message 明示）**：RESOLVE/INTAKE phase 重复 TOOL call 保留 `turn_budget_exhausted`。

**c14 暴露的 gap**：c14 是 **UC-J intake，4 turn 全 RESOLVE phase**：
- Turn 1: classify UC-J → ask listing_id（free-text）
- Turn 2: 记下 listing_id → ask safety issue type（free-text）
- Turn 3: 记下 issue type → ask listing_id **再次**（free-text，state loss）
- Turn 4: user 反问 → runtime inject `turn_budget_exhausted`（latency 1ms）

bot 的 action key 是 `"answer"`（free-text），phase 是 RESOLVE，UC 是 intake UC-J。当前 guard 因 `currentPhase != DISCOVER` 不重映射 → label 仍为 `turn_budget_exhausted`，**和 c9 是同一根因模式但发生在 intake 收集阶段**。

**R2.a#5-ext 设计**（守 anti-误杀 #12 精神，扩展 narrow case）：

```java
if ("max-repeated-same-action".equals(bucket)
        && isFreeTextActionKey(lastAction)
        && (
            "DISCOVER".equalsIgnoreCase(currentPhase)
            || ("RESOLVE".equalsIgnoreCase(currentPhase)
                && IntakeFieldsRegistry.isIntakeUseCase(activeUseCase))
        )) {
    return "clarification_budget_exhausted";
}
```

- Anti-误杀守住：RESOLVE 非 intake UC（UC-A/B/D/F/FP 等 FAQ-path）+ 任何 phase 的 tool-call 重复依然 `turn_budget_exhausted`。
- 扩展仅覆盖 "intake UC 的 free-text 收集 clarification 重复"，恰好是 c14 形态。

Files: `ControlKernel.java` 函数签名加 `activeUseCase` 参数；`MapBudgetToClarificationLabelTest` 补 RESOLVE intake / RESOLVE non-intake / tool-call 三 anti-误杀 negative。

## 4.8 🆕 R7 — intake_fields partial-stash mechanism（post-ship 新发现）

**问题**：`persistInlineIntakeFields(session, call)` 在 `AgentRunLoopImpl.java:487-494` 只在 `toolName == HANDOVER_TOOL` 时触发：

```java
if (HANDOVER_TOOL.equals(toolName)
        && IntakeFieldsRegistry.isIntakeUseCase(plan.useCase())) {
    persistInlineIntakeFields(session, call);
}
```

`mergePartialIntakeFromContext`（`:228 / :950-970`）每 turn 跑一次但只读 `formContext` + `userMessage` 的关键词启发，**不抽 free text 中 LLM 已识别但未发的字段**。

**结果（c14）**：LLM turn 2 收 listing_id 但选择 "等收齐再 handover"（因为 schema 没强制 listing_id 在 UC-J 必填集；UC-J 必填是 `[report_target, report_type, description]`）→ session.intakeFields 空 → turn 3 projection `intake_state.fields_collected` 也空 → LLM 又问。R1.a 修了 *最终 send* 契约，没修 *跨 turn 累计* 契约。

**三个候选**：

| 方案 | 描述 | 优 / 缺 |
|---|---|---|
| **R7.a（推荐）** `update_intake_fields` 新无副作用工具 | 增加 `update_intake_fields(fields={...})` tool；服务端实现 = 调用现有 `persistInlineIntakeFields` 同样的 merge 逻辑但 *不* 触发 handover validator；trace 记 tool_event；schema 同 request_handover.intake_fields | 优：纯 runtime 边界扩展；零 §1.7 风险；LLM 可在任意 turn 调用；与 §1.4 边界一致。缺：增加一个工具名（需 verify Tier-0 invariant 不破：tool list 不是 freeze） |
| R7.b 扩展 `persistInlineIntakeFields` 触发条件 | 检查 ANY tool call 的 `arguments.intake_fields` 字段；不限 `request_handover` | 优：无需新工具。缺：把 intake_fields 副参数 hard-code 进所有工具的处理路径；约定不显式（LLM 不知道哪些 tool 接受） |
| R7.c LLM-augmented IntakeFieldExtractor | 每 turn 跑一次 LLM 抽取 user 消息 + bot 历史中的 intake field | 优：透明无 LLM 协议变化。缺：每 turn 多一次 LLM call（成本+延迟）；与 §1.4 "runtime 不做语义" 边界冲突；与 R4 哲学（surface signal，LLM 决策）冲突 |

**推荐 R7.a**。

**Anti-误杀**：必须保证 `update_intake_fields` 单独调用不能 bypass intake-complete validator —— 它只 *写* session.intakeFields，不 *escalate*；handover 路径上 validator 该校验还是校验。

**与 OBS-S6 关系**：R7 是 OBS-S6（LLM 从 free text 推断字段）的 *运行时 enabler*。OBS-S6 autoloop yaml 让 LLM 推断了字段也得能存下来；没 R7，LLM 只能 "凑齐才发"或 "提前发被退回"两难。

## 4.6 R6 — corpus 治理：`(temp)` template articles

| 关注点 | 位置 | 行为 |
|---|---|---|
| Corpus 中 `(temp)` article | `data/knowledge/knowledge_base_articles.json` 行 273, 292 | 2 篇：`ka41r000000LIEJAA4`（"(temp) Ad removed - By CS (general)"）+ `ka41r000000LIEEAA4`（"(temp) NTD Ad Removed Information"）|
| 内容特征 | 同上 | 包含 `XXXXXXXXX` 占位符，是 CS agent 手动填空用的 template，不是 user-facing 完成文章 |
| 当前 search_knowledge 返回时 | c7/c12/c17 trace | 直接 surface 给 LLM；LLM 复述时 `XXXXXXXXX` 会被忽略 / 误填，导致用户看到的是 "your ad was deleted because..."（伪装 user-specific）|

R6 实现思路（pure infra）：

1. `data/knowledge/knowledge_base_articles.json` 给两篇 article 加 `"bot_visible": false`（默认 true）
2. `SearchKnowledgeTool` 过滤 `bot_visible=false` 的命中
3. **Anti-误杀**：保留 article 在 corpus（人类 CS 可用）；retrieval 不返；trace 中可见过滤理由（observability）
4. 长期：data 团队评估是否清理或重写这些 article（不在本 R6 scope）

---

# 5. Multi-layer root-cause + compounding-effect 分析

## 5.1 R-item 顺序约束

1. **R1 → 深度 intake-UC autoloop**：R1 消除 UC-G/H/I/J/K intake handover 的首次 reject + retry；intake case 的 reducible-flaky 下降；后续 autoloop 调 intake yaml（OBS-S6）时噪声更小
2. **R2 → DISCOVER autoloop**：R2 修后 DISCOVER 有 budget escape；OBS-S2（discover_triage 措辞）才能干净测量
3. **R3 → 所有 trace-based 诊断**：admin UI 看不全 case / dedup event 误读 / informational error 与 blocking error 混在一起，让所有 manual review + bad-case 选材失真。R3 是其它 R-item 的 evidence-floor
4. **R4 → OBS-S1（UC-A/H/J 调 verify-context yaml）**：R4 surface 信号，OBS-S1 调 yaml 让 LLM 利用信号。倒序无效
5. **R5 → 立即生效**：用户看到 article URL 而非 ID，体验改善；无依赖
6. **R6 → 立即生效**：bot 不再把 (temp) 文章包装成 user-specific 答案；与 R4+OBS-S1 协同（c7/c12/c17 的多重根因之一）

## 5.2 不能跳过的反向风险

- R1 不能改成"validator 永远从 args 取" —— 破坏 multi-turn 累计
- R2 首版不加 identical-clarification 跨 turn dedup —— 与 §11 rank-1 anti-误杀 floor 冲突
- R4 surface boolean / enum，**不传 reason 内容** —— 否则 LLM 会基于 reason 内容做语义判断（越界到 semantic_planner）
- R4 不用关键词匹配 user message —— 只用 runtime 已知的 form_context 字段状态 + lookup tool 返回
- R5 `must_cite_source` guardrail 必须兼容 source_id（fallback），不强制 URL —— 否则破坏旧 trace / 38 个 无 URL article
- R6 不删 corpus article —— 只加 `bot_visible: false` 字段；retrieval 过滤；保留人类 CS 可用
- R6 不基于 title prefix `(temp)` 关键词匹配做 runtime 过滤 —— 用 data field 而非内容关键词
- R3 折叠 dedup event 必须保留可展开 —— 审计能力不丢
- R3.c 区分 informational rejection 不能弱化 guard 本身的功能性（guard 仍然有效，只是 trace UI 显示更友好）

## 5.3 与 eval framework / autoloop 的耦合

- R1 + R2 修复后预期 `bad_cases` reducible-flaky 6/12 → ≤ 2/12（详见前一轮对话）
- R3 不影响 fitness 数值，但提升 manual review 体感与 bad-case 选材可靠性
- R4 + R6 + OBS-S1 协同后，UC-A / UC-H / UC-J 类 case 的 verdict 会从"bot 假装知道→失败"变为"bot 诚实询问→可能成功 OR 干净 escalate"，会有 P→F + F→P 双向 flip，eval 需要 paired-evidence review
- R5 不影响 case_passed 判定；改善体感
- 整体：R1/R2 修后 autoloop 信号清；R4/R6 修后 UC-A/H/J 真实表现可测；R3 修后人 review 不被噪声蒙蔽

---

# 6. Design alternatives + 推荐

## 6.1 R1（intake schema 暴露）

| 方案 | 推荐 | 备注 |
|---|---|---|
| **R1.a** Schema 字段 + 每 UC required-fields hint | ✓ | 一次性、零 hardcode、code-grounded |
| R1.b runtime 自动派生 intake_fields | ✗ | 与 §1.4 边界冲突 |
| R1.c runtime 看到 fail 时自动重投 | ✗ | 不解决根因，每次仍消耗 1 步 |

## 6.2 R2（DISCOVER counter + budget projection + mapping fix）

| 方案 | 推荐 | 备注 |
|---|---|---|
| **R2.a** counter 接线 + projection 暴露 budget + mapping 修正 | ✓ | 复用 dead code；零内容匹配；与 Cluster C.3 一致 |
| R2.b identical-clarification 跨 turn byte-match dedup | ✗（首版） | 与 §11 floor 冲突 |

## 6.3 R3（admin observability）

| 方案 | 推荐 | 备注 |
|---|---|---|
| **R3.a/b/c** 三子项同包 | ✓ | UI-only；合并 Cluster B.1 + C.2 |
| 3.c 子选项：弱化 guard 输出 trace event | ✗ | 审计能力丢失 |
| 3.c 推荐：trace event 保留 + UI 用不同 badge / collapsed 区分 informational vs blocking | ✓ | 审计完整 + 用户体感清晰 |

## 6.4 R4（entity premise projection slot）

| 方案 | 推荐 | 备注 |
|---|---|---|
| **R4.a** 结构化 boolean/enum slot（`context_status.customer/ad/listing/order`） | ✓ | 零 keyword；为 OBS-S1 提供 anchor；扩展到所有 entity |
| R4.b runtime 强制 "无 ad_id 时 UC-A 必须 ask" hard guard | ✗ | 违 §1.5/§1.7 |
| R4.c form ingestion 阶段就 fail | ✗ | 完全 bypass LLM = hardcode 路由 |

## 6.5 R5（citation 格式 URL 优先）

| 方案 | 推荐 | 备注 |
|---|---|---|
| **R5.a** ResolveArticleTool 增加 `display_citation` 字段 + skill yaml `cite_token_field: display_citation`；must_cite_source guardrail fallback 兼容 source_id | ✓ | 最小改动；零 LLM 行为变化要求；anti-误杀 fallback |
| R5.b system_prompt 加 "use URL when available" 指令 | ✗ | 不可靠；LLM 自由发挥；与 §1.7 contract-shaped 偏好不符 |
| R5.c runtime 后处理 LLM 输出文本替换 source_id → URL | ✗ | LLM 输出后处理违 §1.7 |

## 6.6 R6（corpus 治理 + tool 过滤）

| 方案 | 推荐 | 备注 |
|---|---|---|
| **R6.a** data 加 `bot_visible: false` + SearchKnowledgeTool 过滤；保留 article 不删 | ✓ | data field 驱动；零 keyword；可逆 |
| R6.b runtime 关键词匹配 title startsWith "(temp)" 过滤 | ✗ | 违 §1.7 keyword |
| R6.c 直接删 article 不入 corpus | ✗ | 人类 CS 可能仍需 |

## 6.7 推荐打包（post-ship 更新 2026-06-05）

**S-Auto-23 已落地 R1.a + R2.a + R4.a**。剩余打包方案：

### Sub-sprint B（已草拟于 `compact/sprint-079-dev-prompt.md`，S-Auto-24 / Sprint 079）

R3.a + R3.b + R3.c — UI/observability only；不动 server runtime。

### Sub-sprint C-1（PRIMARY — human 2026-06-06 决策）— R7 + R2.a#5-ext

**主题：intake / clarification runtime contract**。R7 是 OBS-S6 autoloop 的 *运行时 enabler*（LLM 即使能从 free text 推断字段，没有 stash 路径也存不下）；R2.a#5-ext 是 R2.a#5 在 RESOLVE-phase intake 形态下的 narrow 补完。两者共享 intake/clarification runtime 语境，因果归因清晰。

| 工作项 | 主要文件 | 估改动 LOC |
|---|---|---|
| **R7** intake partial-stash 工具 | `UpdateIntakeFieldsTool.java`（新文件，复用 `persistInlineIntakeFields` merge 逻辑）+ `ContextProjectionBuilder.java`（声明 schema + tool description）+ tool registration + 单元/集成测试 | +120 / +80 |
| **R2.a#5-ext** mapping 扩展 | `server/.../ControlKernel.java`（函数签名加 `activeUseCase` 参数）+ `MapBudgetToClarificationLabelTest.java`（补 RESOLVE intake / RESOLVE non-intake / tool-call 三 anti-误杀 negative） | +20 / +60 |

总估：~3–4 文件 / ~280 LOC / ~10 单元测试。

**Anti-误杀 invariants**：
- R7 单独调用不能 bypass intake-complete validator（只写不 escalate；handover 路径 validator 不变）
- R7 不能从 `accumulated_tool_results` 自动派生 intake_fields（必须 LLM 显式调用，保持 LLM-first 决策权）
- R2.a#5-ext 不重映射 RESOLVE 非 intake UC（守 anti-误杀 #12 精神）；不重映射任何 phase 的 tool-call 重复

### Sub-sprint C-2（PRIMARY — human 2026-06-06 决策；可并行于 C-1）— R5 + R6

**主题：体验改善 + corpus 治理**。两者不涉及 intake / clarification 语境，与 C-1 完全解耦，因果可独立归因。不阻塞 OBS-S6 前置能力。

| 工作项 | 主要文件 | 估改动 LOC |
|---|---|---|
| **R5** ResolveArticleTool display_citation | `ResolveArticleTool.java` + `resolve_faq_grounded_answer.yaml`（1 行 config）+ must_cite_source guardrail fallback 兼容测试 | +30 / +50 |
| **R6** corpus bot_visible filter | `data/knowledge/knowledge_base_articles.json`（给 `ka41r000000LIEJAA4` + `ka41r000000LIEEAA4` 加 `bot_visible: false`）+ `SearchKnowledgeTool.java`（过滤）+ tool 单元测试 | +20 / +30 |

总估：~4 文件 / ~130 LOC / ~6 单元测试。

**Anti-误杀 invariants**：
- R5 `must_cite_source` guardrail 必须兼容 source_id fallback（旧 trace + 38 个无 URL article 不破）
- R6 默认 `bot_visible: true`（其它 article 不受影响）；仅 2 篇显式 false；retrieval trace 中可见过滤理由

### 弃用方案：原打算单 Sub-sprint C bundle（R2.a#5-ext + R5 + R6 + R7）

human 2026-06-06 否决，理由：(1) R7 + R2.a#5-ext 同属 intake/clarification runtime contract，应同包；(2) R7 是 OBS-S6 enabler 高优先，不该被 R5/R6 体验改善阻塞；(3) 小 sub-sprint 利于因果归因 + anti-误杀 review。

**Packaging 决定权已锁定（C-1 + C-2 并行）。** deliver-agent 启动顺序：C-1 优先；C-2 可同期或后跟。Sub-sprint B（R3）独立排，不与 C-1/C-2 互锁。

---

### 历史版本：原推荐打包（pre-ship 时方案，已部分实现）

**单个 runtime/infra sub-sprint（建议 M-Auto-6 第一批）**：R1.a + R2.a + R3.a/b/c + R4.a + R5.a + R6.a 同包，6 commits。

总估算：~12–15 文件、~600–800 LOC、~15–20 单元/集成测试 + 2 UI 测试。

| 工作项 | 主要文件 | 估改动 LOC |
|---|---|---|
| R1.a schema + projection | `server/.../ContextProjectionBuilder.java` | +60 |
| R1.a 测试 | `ContextProjectionBuilderTest.java` + 集成 | +100 |
| R2.a counter 接线 | `server/.../AgentRunLoopImpl.java` | +25 |
| R2.a budget projection + mapping fix | `server/.../ContextProjectionBuilder.java` + `ControlKernel.java` | +15 |
| R2.a 测试 | `AgentRunLoopImplTest.java` + `ControlKernelTest.java` | +80 |
| R3.a admin UI session list | `ui/.../SessionList.tsx` | +30 |
| R3.b dedup UI folding | `ui/.../TraceViewer.tsx` | +50 |
| R3.c informational badge | `ui/.../TraceViewer.tsx` + `server` trace event tagging | +40 |
| R3.\* UI 测试 | UI test files | +80 |
| R4.a projection slot | `ContextProjectionBuilder.java` + `FormContextIngestionService.java` | +50 |
| R4.a 测试 | 单元 + projection 集成 | +60 |
| R5.a tool 字段 + skill yaml | `ResolveArticleTool.java` + `resolve_faq_grounded_answer.yaml`（1 行） | +30 |
| R5.a 测试 | tool 单元 + grounding 集成 | +50 |
| R6.a data field + tool 过滤 | `data/knowledge/knowledge_base_articles.json` + `SearchKnowledgeTool.java` | +20 / data +4 行 |
| R6.a 测试 | tool 过滤单元 | +30 |

**或拆 2 个 sub-sprint**：

- **Sub-sprint A**（runtime + tool/contract changes）：R1.a + R2.a + R4.a + R5.a + R6.a（5 commit；~500 LOC；共享 server/ 改动语境）
- **Sub-sprint B**（UI/observability only）：R3.a + R3.b + R3.c（3 commit；~200 LOC；隔离 UI 改动便于独立 verify）

Packaging 决定权在 deliver-agent。

---

# 7. §7 stanza pre-fill drafts

## 7.1 同包方案（R1+R2+R3+R4+R5+R6）

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** prompt_projection (R1.a tool schema + R4.a entity premise +
R5.a citation token via skill yaml config)
+ infra (R2.a counter wiring + R2.a mapping fix + R3.a/b/c observability +
R6.a corpus tool filter)
+ skill_state (R2.a clarification counter cross-turn accumulation)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.
（R1.a 暴露 IntakeFieldsRegistry 已有 contract；R2.a 接线已有 BudgetChecker
契约 + 重映射现有 enum；R3.a/b/c UI display only；R4.a 暴露 runtime 已知
事实的结构化 slot；R5.a 增加 tool result field + skill yaml 1 行 config；
R6.a data field 驱动 retrieval 过滤。均不新增 runtime invariant。）

**Semantic hardcode:** No semantic hardcode introduced.
（R1.a required-fields 来自 IntakeFieldsRegistry.java:53-67 已有定义；R2.a
零内容匹配；R3.* UI display；R4.a surface boolean/enum 状态不传 reason 内容；
R5.a 用已有 source_url 字段；R6.a 用 data field bot_visible，不基于 title
keyword 匹配；不引入任何 keyword / regex / per-UC matrix / 内容相似度。）

**Generalization coverage:** target / neighbor / negative / shadow case counts: 17 / ~25 / ~15 / ~22

- target: c1/c5/c6/c11 (UC-J/I 验证 R1)；c3/c9 (DISCOVER R2)；c7/c12 (R3+R4+R6
  UC-A 链)；c8 (R3 dedup + OBS-S3 fabricate)；c10/c2/c17 (R3.c record_outcome
  cosmetic + OBS-S4)；c13 (R5 citation)；c14 (R1+R2+R4 UC-J listing)；c15
  (R1+R4 UC-I order)；c16 (R4 UC-H ad lookup)
- neighbor: case_families/uc_*_* 中所有 intake-complete-for-uc-* 路径 +
  UC-A/H/J 含 entity reference 的 case + DISCOVER 多 turn case
- negative: 非 intake UC 的 handover 不应被误激活 intake_fields；DISCOVER
  已 commit UC 的 turn 不应计入 clarification budget；UC-A 已带 ad_id 时
  不应出现 `missing_id` slot；form 有 ad_id 时 lookup 成功后不应再 surface
  `unverified` 信号；(temp) article 不应 surface 给 LLM 但应保留在 corpus
  可被人查询
- shadow: cs01s*, cs15s*, cs32s*, cs38s*, cs59s*, cs76s*, cs92s* 全 22 case；
  至少覆盖 UC-K + UC-J + UC-A + UC-I + UC-H
```

## 7.2 拆分方案

**Sub-sprint A**（R1 + R2 + R4 + R5 + R6）：同上去掉 R3.\* 那一段；coverage 同上去掉 c8 admin / c2/c10/c17 cosmetic item。

**Sub-sprint B**（R3.a + R3.b + R3.c）：

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (observability)

**Tier-0 invariant:** This sprint adds no Tier-0 invariant.

**Semantic hardcode:** No semantic hardcode introduced.

**Generalization coverage:** target / neighbor / negative / shadow case counts: 5 / 0 / ~5 / 0
- target: c7 (admin 找不到)、c8 (dedup 折叠)、c2/c10/c17 (informational rejection badge)
- neighbor: 无
- negative: ESCALATE / handover 完成的 session 仍可见且 trace 可展开；A1
  dedup 折叠后可展开看完整 event；real error 仍以 error badge 显示
- shadow: 不适用（UI-only）
```

---

# 8. Hard fences + non-goals

**Hard fences（绝不做）：**

- 不引入 keyword / regex / 内容相似度 / per-UC if-else 于任何 R-item
- 不改 `IntakeFieldsRegistry` 字段定义、`SkillGuardrailDispatcher` reject 逻辑、`escalation_reason` 23-enum（R2 的 mapping fix 只重映射现有 enum）、`record_outcome` premature guard（OBS-S4 by-design）
- 不为 c4 写 cross-turn semantic similarity dedup（违 §11 floor）
- 不在 R3 折叠 dedup event 时丢 audit 信息（必须可展开）
- 不在 R3.c 弱化 record_outcome guard 本身（只改 trace UI 显示）
- 不在 R4 暴露 entity reason 内容，只 boolean / enum
- 不在 R4 / R6 用关键词匹配 user message 或 article title
- 不在 R5 强制 URL（无 URL 时 fallback source_id；must_cite_source guardrail 兼容旧 trace）
- 不在 R6 删除 corpus article（人类 CS 可能仍需）；只加 `bot_visible: false` data field
- 不触 simulator / eval framework / scoring SHA / autoloop 5-file 集（§5.8 + fence-#13）
- 不在 M-Auto-5 close 之前 launch
- 不在本 sub-sprint 改 skill yaml 措辞 / procedure（OBS-S1/S2/S3/S6/S7 全 defer to autoloop）；**例外**：R5 的 `cite_token_field: display_citation` 是单行 config 替换，不算 procedure 语义改动

**Non-goals：**

- 不修 c1 中 LLM 反复失败 6 turn 的 LLM 决策（R1 修后自动消失）
- 不为 DISCOVER 加"自动跳过 clarification 进 RESOLVE"捷径
- 不写新 cross-turn semantic-similarity 抑制器
- 不重写 `IntakeFieldExtractor` 为 LLM-based（OBS-S6 由 autoloop 调 yaml 引导 LLM 从 free text 推断）
- 不修 c8 turn 6 fabrication（OBS-S3 / `D-faq-grounded-resolve-bypass` defer to autoloop）
- 不修 c9 disambiguation 过度保守（OBS-S2 autoloop）
- 不修 UC-A/H/J procedure 缺 critical_step "verify entity"（OBS-S1 autoloop；本 sub-sprint R4 只 surface 信号）
- 不修 c10 escalate-without-summary（OBS-S7 autoloop）
- 不修 c11/c14/c15 intake too literal（OBS-S6 autoloop）

---

# 9. Risk + compounding-effect 风险矩阵

| 风险 | 影响 | 缓解 |
|---|---|---|
| **R1.a schema 改动破坏 mocked-LLM 测试** | 测试期望 schema 严格一致 | 同包补 contract 测试；mocked-LLM 测试若硬编码字段 set 需 expand |
| **R1.a `intake_fields` 字段被 LLM 误填到非 intake UC** | schema 在所有 UC 可见 | description 写明 "only required for UC-G/H/I/J/K"；`IntakeFieldsRegistry.isIntakeUseCase` gate 已存在；非 intake UC 持久化无害 |
| **R2.a free-text clarification 识别误差** | 计数偏差 | 严格定义 "phase==DISCOVER && 本 turn 无 tool call 且未 commit UC"；不做内容判断；counter-test 覆盖 commit-UC turn 不计 |
| **R2.a mapping 修正引起 eval CaseSpec 期望变化** | `max-repeated-same-action` 改映 `clarification_budget_exhausted` 后期望 escalation_reason 可能 mismatch | check `eval_interactive/case_specs/` 期望；如需同步是 §5.4 受控更新，不是 mask 真错 |
| **R3.a 未完全锁定 c7 admin 找不到的根因** | 探索 agent 未确认是 handling_state / 持久化 / 缓存 | R3.a 包含 "添加诊断日志记录 handling_state 转化路径" 作为前置；若发现是持久化 bug 升级为独立 R-item |
| **R3.b 折叠 dedup event 影响审计** | PARAPHRASE_STORM 审计困难 | event 完整持久化；展开按钮 + "↳ X dedup'd repeats" 标记 |
| **R3.c informational badge 误判** | 错把真 error 标 informational | 用明确白名单（record_outcome premature + 一组列出的 informational rejection predicate names），其余默认 error |
| **R4.a slot 命名冲突** | 与现有 `intake_state` 字段语义冲突 | 命名空间下置（如 `context_status.customer_lookup`） |
| **R4.a 增加 projection 体积** | 边际 token 成本 | boolean/enum 字段 < 100 字符；可忽略 |
| **R4.a 不修 OBS-S1 时 c7 仍可能复现** | R4 surface 信号但 LLM 不利用 | 这是设计意图；R4 是 enabler；OBS-S1 autoloop 调 yaml；c7 全闭环需双方落地 |
| **R5.a must_cite_source guardrail 兼容性** | 改 cite_token_field 后旧 trace / 38 个无 URL article 失配 | guardrail fallback 兼容 source_id；旧 trace 不重判 |
| **R6.a 漏过滤 / 误过滤** | (temp) 文章漏过滤继续误用；或合法文章被误过滤 | 默认 `bot_visible: true`（不影响现有 article）；只 2 篇 `(temp)` 显式标 false；测试覆盖两个文章过滤 + 其它 article 通过 |
| **R6.a 与 corpus_audit R-item 互动** | 与 `R-corpus-coverage-audit-per-uc` 调整同 corpus | 协调：R6 加 field，不动 article 内容；corpus_audit 后续整理 |
| **同包 6 R-item 增加 reviewer 负担** | Codex 难一次审 6 套 | 6 个独立 commit；Codex 9-question kernel 分别审；同 sub-sprint 但 6 个 commit |
| **M-Auto-5 close 延期** | proposal 进不了 M-Auto-6 队列 | proposal 永久存档；human 选择何时 promote |
| **shadow 不够覆盖 multi-cause case (c7/c10/c14/c16)** | R3/R4/R6 不能完全验证 cross-suite | M-Auto-5 close 后 baseline 必须含完整 22 shadow；本 sub-sprint 验收看 shadow 不退 |

---

# 10. Observability / trace / report implications

- **R1.a**：`intake_fields` 出现在 `request_handover.arguments` → 现有 tool_events trace 已展示；admin 把 `intake_state` 投影 + `required_fields_for_active_uc` 一起渲染（合并 Cluster B）
- **R2.a**：补 admin UI 显示 `budgets.clarification_count` + `budgets.clarification_max`；新的 `clarification_budget_exhausted` mapping 路径需在 trace 体现
- **R3.a/b/c**：直接 observability —— admin session 列全 + dedup event UI 折叠（可展开）+ informational rejection 差异化 badge；验证 c7/c8/c10/c2/c17 类 case 在 admin 体验改善
- **R4.a**：projection 增加 `context_status` slot，admin trace 显示这些字段；为 OBS-S1 autoloop 提供 anchor
- **R5.a**：admin trace + user-facing reply 都显示更完整 citation；改善体感
- **R6.a**：trace 中记录被过滤的 article（"filtered: bot_visible=false"），保持透明
- **Eval 影响**：
  - R1.a 后 intake handover 不再首次 reject → trace 体积下降；不影响 case_passed
  - R2.a mapping 修正改变 escalation_reason 分布；可能影响 eval CaseSpec
  - R3.a/b/c 不影响 eval 数值，提升 manual review 体感
  - R4.a / R6.a / R5.a 增加 projection / tool result 字段，影响 token 计数但不影响 case_passed；R4+R6 可能改变 UC-A/H/J case 的语义 verdict 方向（P→F 与 F→P 双向 flip 可能），需 paired-evidence review
  - 整体预期：`bad_cases` reducible-flaky 6/12 → ≤ 2/12

---

# Appendix A — 17 case 对应表（post-ship 更新）

post-ship 视角下重新分类（c1–c9 = S-Auto-23 触发输入；c10–c17 = ship 后 smoke 观察）：

| Case | trace 片段 | 核心异常 | 状态 | R-item / OBS |
|---|---|---|---|---|
| c1 | `e1f844cb-712...` | UC-J 4× intake reject 全失败 | ✅ FIXED by R1.a (S-Auto-23) | R1 |
| c2 | `f070bf75-67a...` | record_outcome premature | ⏳ R3.c pending + autoloop | R3.c + OBS-S4 |
| c3 | `d56fd1e6-510...` | DISCOVER 重复 clarification | ✅ FIXED by R2.a#3 (S-Auto-23) | R2 |
| c4 | (误粘贴) | search paraphrase | by-design | OBS-S5 |
| c5 | `c6f60c19-3fc...` | UC-J 同 c1 retry 成功 | ✅ FIXED by R1.a | R1 |
| c6 | `3bd52cf6-b14...` | UC-I 同 c1 retry 成功 | ✅ FIXED by R1.a | R1 |
| c7 | (admin 找不到) | UC-A 不验 ad 就答 | R4 signal shipped；R3.a + R6 pending；OBS-S1 autoloop | R3.a + R4 + R6 + OBS-S1 |
| c8 | `e296c331-401...` | placeholder + fabricate + dedup UI | ⏳ R3.b pending + autoloop | R3.b + OBS-S3 |
| c9 | `d98c704f-7fa...` | DISCOVER loop + label 误导 | ✅ FIXED by R2.a#3 + #5 (S-Auto-23) | R2 |
| **c10** | `e82c8da3-70c...` | record_outcome reject → escalate 无 summary | ⏳ R3.c pending + autoloop | R3.c + OBS-S4 + OBS-S7 |
| **c11** | `7d20682a-c62...` | **R1 验证 ✓** first-call success；问 user 分类是 semantic | R1 验证；OBS-S6 autoloop | R1 ✓ + OBS-S6 |
| **c12** | `745878f9-a70...` | 同 c7 + (temp) article 复述 | R4 signal shipped；R6 pending；OBS-S1 autoloop | R4 ✓(enabler) + R6 + OBS-S1 |
| **c13** | `49f7b981-ec0...` | cite ID 不用 URL | ⏳ R5 pending | R5 |
| **c14** | `307310c8-b52...` | **🆕 新发现**：RESOLVE-phase intake state loss + mapping label 误导 | 🆕 R2.a#5-ext + R7 NEW | R2.a#5-ext + R7 + OBS-S6 |
| **c15** | `83b09eec-668...` | **R1 验证 ✓** first-call；问 user dispute_reason semantic | R1 验证；OBS-S6 autoloop；R4 entity slot 已 surface order? **待 verify R4.a 是否覆盖 transaction_reference** | R1 ✓ + R4(verify ext) + OBS-S6 |
| **c16** | `7eda6c17-b0b...` | **R1 验证 ✓** first-call；UC-H 不 lookup ad 是 OBS-S1 ext | R1 验证；OBS-S1 UC-H autoloop | R1 ✓ + R4 ✓(enabler) + OBS-S1 ext |
| **c17** | `7b714c21-f1e...` | UC-A 健康 path（form 有 ad_id）+ record_outcome cosmetic + corpus gap | ⏳ R3.c + R6 pending | R3.c + OBS-S4 + R6 + 已有 `R-corpus-coverage-audit-per-uc` |

---

# Appendix B — R-item 编号与 action_bank 登记建议

deliver-agent promote 时，建议在 `action_bank.md` §5.2 新 sub-section 登记：

| 建议 id | 描述 | 源 case |
|---|---|---|
| `R-request-handover-intake-fields-schema-projection` | request_handover schema 暴露 `intake_fields` + per-UC required-fields hint | c1/c5/c6/c11 |
| `R-discover-clarification-counter-live-wireup-and-mapping-fix` | live `AgentRunLoopImpl` 接线 counter；projection 暴露 budget；`max-repeated-same-action` 重映射 | c3/c9/c14 |
| `R-admin-trace-observability-three-fixes` | admin session list 完整性 + dedup UI 折叠 + informational rejection 区分 | c7/c8/c2/c10/c17 |
| `R-entity-context-premise-projection-slot` | 通用 entity premise（customer/ad/listing/order）结构化 slot | c7/c12/c14/c15/c16 |
| `R-resolve-article-display-citation-with-url` | ResolveArticleTool 增加 display_citation；skill yaml cite_token_field 切换；must_cite_source 兼容 fallback | c13 |
| `R-faq-corpus-bot-visible-filter` | data 加 `bot_visible: false` 字段；SearchKnowledgeTool 过滤 `(temp)` template articles | c7/c12/c17 |
| `D-faq-grounded-resolve-bypass`（已存在，重评估） | n=2 起（c8 turn 6 是第 2 实例） | c8 |

OBS 建议同时在 `action_bank.md` autoloop 候选区登记（不开 R-item 但留 trace）：

| OBS | 触及 case |
|---|---|
| `OBS-uc-a-h-j-verify-entity-procedure` | c7/c12/c16 |
| `OBS-discover-ad-status-disambiguation-too-cautious` | c9 |
| `OBS-record-outcome-premature-llm-timing` (n=3) | c2/c10/c17 |
| `OBS-intake-procedure-infer-fields-from-free-text` | c11/c14/c15 |
| `OBS-escalate-with-summary` | c10 |

---

# Appendix C — 与 deliver-agent 的交接清单

deliver-agent 若选择 promote 本 proposal 到 sub-sprint(s)，需决定：

**post-ship + human 2026-06-06 决策锁定**：

1. **打包形态（已锁定）**：
   - Sub-sprint A = R1.a + R2.a + R4.a ✅ SHIPPED (S-Auto-23)
   - Sub-sprint B = R3.a/b/c — 已草拟，S-Auto-24
   - **Sub-sprint C-1 = R7 + R2.a#5-ext**（intake/clarification runtime contract；OBS-S6 enabler）
   - **Sub-sprint C-2 = R5 + R6**（体验 + corpus；与 C-1 解耦，可并行）
   - 弃用：原 R2.a#5-ext + R5 + R6 + R7 单大包
2. **launch 时机**：B 与 C-1/C-2 不互锁；C-1 高优先（阻塞 OBS-S6）；C-2 可同期
3. **sub-sprint id**：B = S-Auto-24 / Sprint 079（草拟中）；C-1 / C-2 待 deliver 分配
4. **dev prompt 模板**：参 prompt-artifact-rules §9.1–§9.6；含完整 §7 stanza + file-path fence + anti-误杀 invariants
5. **Codex review trigger**：C-1（R7 触工具列表 + R2.a#5-ext 触 mapping）= semantic-touching，须 §4.1 9-question kernel；C-2（R5 触 skill yaml 1 行 + R6 触 data + tool 过滤）= semantic-touching；均 per-sub-sprint
6. **Generalization coverage 具体 case ID**：deliver 在各 §7 stanza 填 target / neighbor / negative / shadow case 名单
7. **OBS autoloop 排序（已锁定）**：
   - OBS-S1 / OBS-S2 / OBS-S7 可以即时排（R1/R2/R4 已为 noise floor 前提）
   - OBS-S6 必须等 C-1 (R7) ship 后再开
   - OBS-S3 / OBS-S4 / OBS-S5 排序由 deliver 决定，不阻塞
8. **`D-faq-grounded-resolve-bypass` 重评估**：c8 给出第 2 实例后，下个 milestone planning 评估是否从 deferred 提升
9. **`R-corpus-coverage-audit-per-uc` 触发**：c17 验证 corpus gap 仍存在（UC-A 找不到 specific policy 解释），可作为该 R-item 触发 evidence
10. **post-C-1 验证锚点**：C-1 ship 后预期 c14 形态闭环（state 不再丢 + label 准确）；C-2 ship 后预期 c12/c17 不再 hit `(temp)` article + c13 用 URL 引文

— 完 —
