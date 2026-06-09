---
title: LLM 调用模式现代化 — 原生 tool-use + 流式（SSE）+ append-only 上下文
doc_tier: proposal
status: proposal
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-05-24
mode: forward-looking
notes: >
  Path 1 forward-looking research。Human thought：当前 csagent 的 LLM 调用是
  "同步 JSON-object，无 streaming"，行业参考实践是 "SSE streaming + tool-use
  native API"，需借鉴行业实践设计与 CS 业务场景匹配的更优方案。本文给出
  code-grounded 现状勘察、gap 分析、≥2 设计方案 + 推荐、scope 拆分建议、
  §3.2 layer 分类 + §7 stanza 预填、hard fences、风险 + compounding-order
  分析、可观测性配套。所有引用路径在 HEAD（branch refactor/remove-the-shackles）
  验证。本文是建议，不是绑定决定；最终 milestone/sprint 拆分由 deliver-agent 决定。
---

# LLM 调用模式现代化 proposal

## 1. Executive summary（一段）

当前 csagent 的 LLM 调用是一条**同步阻塞 + JSON-object-in-content + 每步重投影**
的链路：`ChatController` 以阻塞 `ResponseEntity` 返回整条回复
（`ChatController.java:55,90`),`OpenAiCompatibleLlmClient` 用 `RestTemplate.postForEntity`
做单次阻塞调用、请求体里**没有原生 `tools` 字段**、响应只读 `message.content`
而**从不读 `message.tool_calls`**(`OpenAiCompatibleLlmClient.java:363-404,406-444`),
工具调用是 LLM 在 `json_object` 文本里吐出的 `{"tool_calls":[...]}` 由 `ActionParser`
做容错解析、解析失败 fallback 到 handover(`ActionParser.java:54-85,120-130`)。
`AgentRunLoopImpl` 每个 loop step 都**重建并整段重发**约 20+ slot 的投影
（含 10 轮对话历史 + `tool_schemas`),工具结果折叠进 `accumulated_tool_results`
JSON 而非原生 `tool` 消息(`AgentRunLoopImpl.java:188-198,210`),**全程无 prompt
caching**。这套设计是为"runtime 拥有投影面(prompt_projection)"的治理模型服务的
（§1.4 Runtime owns "trace and eval contract"),但它与未来 6 个月主流 LLM
（DeepSeek / Kimi / Gemini 系列)的能力走向(原生 agentic tool-use、reasoning、
context caching、streaming)**正在反向背离**:`thinking:{type:disabled}` 被强制关闭
（`OpenAiCompatibleLlmClient.java:367-371`),模型被迫用 JSON-blob 模仿其原生
tool-use 训练。本文建议**沿三条正交轴解耦推进**,采用 **native-tool-use 优先、
流式后置** 的分阶段方案(方案 C),并论证它**必须排在 M5 S3(Skill-driven 投影收敛)
之后**、消费 S3 的字段消费矩阵,否则会重复迁移一个即将被重构的投影面、并污染
bad-case 回归归因。该改造主体是 `infra` + `prompt_projection`,**不引入语义
hardcode,反而能收缩 system prompt 里的 escalation_reason if-else 决策树**,与
Constitution §1.2 / §1.7 同向。

---

## 2. Current-state survey（code-grounded，HEAD 验证）

### 2.1 三条被耦合在一起的轴

当前实现把三个**本应正交**的设计决策捆成了一团。把它们拆开是本提案的分析骨架:

| 轴 | 当前取值 | 行业参考实践 | 所属 owner（§1.4） |
|---|---|---|---|
| **轴 1 — 传输** | 同步阻塞 HTTP（servlet MVC） | SSE streaming | Runtime（transport） |
| **轴 2 — 工具协议** | JSON-object-in-content + 文本解析 | 原生 tool-use API（schema 强约束） | Runtime（tool schema）+ LLM（tool 决策） |
| **轴 3 — 上下文承载** | 每步重投影为 1 个 JSON 字符串 | append-only 原生消息数组 + prompt caching | prompt_projection（§3.1） |

### 2.2 轴 1 — 同步阻塞传输

- `ChatController.processMessage` 返回 `ResponseEntity<ChatSessionResponse>`
  （`ChatController.java:90-112`),`createSession` 同(`:55`)。无 SSE / `DeferredResult`
  / WebFlux;`server/pom.xml:20` 用 `spring-boot-starter-web`(servlet 阻塞栈)。
- `OpenAiCompatibleLlmClient.doChat` 用 `restTemplate.postForEntity(url, entity, String.class)`
  单次阻塞拿整条响应(`OpenAiCompatibleLlmClient.java:322`),请求体**无 `stream:true`**
  （`buildRequestBody` :363-404 不设置该字段)。
- 用户侧 wall-clock 截止 `USER_FACING_LLM_DEADLINE_MS = 30_000L`,在每次
  create/message 时 `LlmCallContext.setDeadline(...)`(`ChatController.java:41,74,109`);
  单次 invocation 限 2 次 HTTP attempt(primary + fallback);单次 read timeout 12s
  （`OpenAiCompatibleLlmClient.java:62`)。围绕这套阻塞模型,`LlmCallContext` /
  `OpenAiCompatibleLlmClient.chat` / `FallbackLlmClient.chat` 写了大量 deadline /
  budget 数学(`OpenAiCompatibleLlmClient.java:132-236`、`FallbackLlmClient.java:42-151`)。
- 因为无流式,`ProgressPlaceholderService`(注入到 `ToolDispatcher.java:61`)在等待
  期间给"thinking"占位文案 —— 这是**阻塞 UX 的补丁**,不是真正的逐字渲染。

### 2.3 轴 2 — JSON-object 工具协议（非原生 tool-use）

- `LlmInvocationService.invokeChat` 设 `.responseFormat("json_object")`、`temperature(0.3)`、
  `maxTokens(1024)`,system = 模板 + 投影字符串,messages 只放当前 user 一条
  （`LlmInvocationService.java:107-119`)。
- 请求体**完全没有原生 `tools` 数组**:`buildRequestBody`(`OpenAiCompatibleLlmClient.java:363-404`)
  只拼 `model / thinking / temperature / max_tokens / response_format / messages`;
  `messages` 里每条仅 `role + content`(`:394-400`)。
- 响应解析**只读 `choices[0].message.content`,从不读 `message.tool_calls`**
  （`parseResponse` :406-444)。即便 provider 回原生 tool_calls 也会被丢弃。
- 数据模型印证:`ChatMessage` 只有 `role + content`(无 `tool_calls` / `tool_call_id`
  / `tool` role);`LlmRequest` 有 `systemPrompt/messages/temperature/maxTokens/responseFormat`,
  **无 `tools`**;`LlmResponse` 有 `content/finishReason/tokens/latencyMs`,**无结构化
  `toolCalls`**(`ChatMessage.java`、`LlmRequest.java`、`LlmResponse.java`)。
- 工具调用靠 LLM 在 JSON 文本里吐 `{"user_message","reasoning","tool_calls":[{"name","arguments"}]}`,
  `ActionParser.parse` 解析,需 `cleanResponse` 剥 ```` ```json ```` 围栏,任何解析失败
  → fallback 到 `request_handover`(`ActionParser.java:54-85,120-130,135-149`)。
- system prompt 强制该 JSON 三字段契约,工具 schema 经 `tool_schemas` 投影 slot 动态下发
  （`system_prompt.txt:3-8`)。**escalation_reason 的 23 值枚举 + 决策树是 prompt 里
  一大段 if-else 式文字**(`system_prompt.txt:33-81`)—— 这正踩在 §1.7「prompt 当
  if-else dump」的边线上。
- `thinking:{type:disabled}` 强制关闭(`OpenAiCompatibleLlmClient.java:367-371`)——
  JSON-object 契约与 reasoning token 不相容是直接动因之一。

### 2.4 轴 3 — 每步重投影，无 caching

- `AgentRunLoopImpl.run` 是一个 `for (step < maxSteps)` 阻塞循环:每步先
  `contextProjectionBuilder.build(...)` 重建投影,再 `invokeChat`,解析,派发工具,
  把结果塞进 `accumulatedToolResults`,进下一轮(`AgentRunLoopImpl.java:179-198,210,486-492`)。
- `ContextProjectionBuilder.build` 产出 ~20+ 顶层 slot(session / task_summary /
  intake_state / candidate_use_cases / drift_history / budget_state / **tool_schemas** /
  conversation_history(末 10 轮)/ accumulated_tool_results / already_called / …),
  **每个 loop step 整段重建重发**;prior 工具结果折叠进 `accumulated_tool_results`
  JSON 而非原生 `tool` 角色消息。注释自陈整 payload 约 ~3k token
  （`OpenAiCompatibleLlmClient.java:56-58`）。
- **全栈无 prompt caching / KV-cache 提示 / `cache_control`**(全树 grep 0 命中)。
  由于每步投影会变异(`accumulated_tool_results` 增长),即便 provider 支持前缀缓存
  也基本命不中。

### 2.5 每个用户回合的真实调用量

- `ControlKernel` 单回合最多触发**两次** `AgentRunLoop.run`:DISCOVER 一次
  （`ControlKernel.java:356`),命中 `USE_CASE_IDENTIFIED` 后在预算允许
  （`MIN_RESOLVE_REPLAN_BUDGET_MS = 8_000L`,`:48`)时同回合 replan 进 RESOLVE 再一次
  （`:417`),`mergeAgentRunResults` 合并(`:422`)。
- 每次 `run` 内最多 `maxToolSteps` 个阻塞 LLM round-trip:DISCOVER=2、RESOLVE_FAQ=4、
  RESOLVE_INTAKE=3(`skills/discover_triage.yaml:13`、`resolve_faq_grounded_answer.yaml:24`、
  `resolve_intake_collect_and_handover.yaml:16`;默认 4,`AgentRunLoopProperties.java:34`)。
- **最坏情形:单个用户回合 ≈ 6 次顺序阻塞 LLM 调用(DISCOVER 2 + RESOLVE 4)**,
  每次都整段重发约 3k token 投影 —— 大量是几乎相同的前缀。另有独立的
  `invokeRouting` 分类调用路径(`LlmInvocationService.java:233-289`)。

### 2.6 冻结的可观测性契约（改造必须尊重）

- `GET /sessions/{id}/llm-calls` → `LlmCallLog`(summary,response 截断 500 字),
  eval harness `agent_client.py:169 get_llm_calls` 消费 → `executor.py` 的
  `case_results[].llm_calls`。S2 合同明文**冻结**该端点(`sprint_objective.md:82-84,113-116`)。
- **正在施工的 M5 S2**(`docs/sprint_objective.md` Sprint 51)新增 `bot_turn_llm_calls` 表
  做 per-invocation 全量持久化:`LlmCallRecord`(`AgentRunLoopImpl.java:160,283-286`)、
  `AgentRunResult` 已是 record 且带 `llmCallRecords`(working tree;`AgentRunResult.java:31-47`)、
  新 `BotTurnLlmCall` 实体 + `V15__create_bot_turn_llm_calls.sql`(git status)。
- 当前 customer-facing 回复在 server 侧装配成一个完整 `responseText` 字符串后整条返回
  （`SessionManager.java:368-370`、`PhaseEvaluator.java:1436`)。任何 egress 期的
  安全处理(PII / forbidden-phrase / grounding floor)都作用在**完整字符串**上 ——
  这是流式化必须保留的不变式(见 §9 风险 4)。

---

## 3. Gap 分析

把 gap 按"对 CS 业务的影响"陈述,而非泛泛"行业都这么做"。

### G1 — 感知延迟 / TTFT（轴 1）
阻塞模式下用户必须等**整条**回复(可达 30s 截止;实测每调用 ~3-5s × 多步)才看到
任何字。`ProgressPlaceholderService` 的占位文案是补丁,不是逐字流。对一个"有人味"
的 CS agent,**首字时间(TTFT)与渐进渲染**直接决定挫败感(尤其用户已经在投诉)。

### G2 — token 成本 / 重复 tokenize（轴 3）
6 步回合 × ~3k token ≈ ~18k 输入 token,且大部分是相同前缀,却**每步全量重发、零
缓存**。原生消息数组(append-only)+ provider context caching(DeepSeek / Kimi 均
有自动磁盘缓存,命中可降输入成本约一个数量级)能大幅削减。当前"每步变异投影"
的形态主动破坏了前缀可缓存性。

### G3 — 工具协议的结构脆弱性（轴 2）
LLM 把工具调用塞进 JSON 文本 → 需剥 markdown 围栏、容错解析、解析失败 fallback 到
handover(`ActionParser.java`)。这是一个**containment 失败面**:一次格式抖动 = 一次
本可避免的转人工。原生 tool-use 由 **API 按 JSON schema 强校验** arguments,鲁棒性
高一个量级;且现代模型是按原生 tool-use RL 训练的,用 JSON-blob 模仿反而抑制其能力。

### G4 — 与模型能力走向背离（轴 2 + reasoning）
`thinking:disabled` + JSON-object 契约,使 reasoning / 原生 parallel tool-call /
agentic 多步等"未来 6 个月主流模型的核心增量"无法被利用。这是**前瞻性负债**:
方案越往后,workaround 的相对成本越高。

### G5 — 投影面治理价值（必须保留，不是要废除的）
注意:每步重投影**不是 bug**,它是 `prompt_projection` 层(§3.1)的载体 —— runtime
拥有"投影什么给 LLM"(§1.4 trace/eval 契约),M5 S3 整个 sub-sprint 就是把投影做成
Skill-driven。任何"直接换原生 messages"的天真做法会**溶解掉**整个治理模型赖以
存在的投影层。因此 gap 的正确表述是:**在保留 runtime 投影所有权的前提下,改变
投影的物理承载**(从"重发一个字符串"变为"稳定前缀缓存 + append-only 原生 tool
结果消息"),而非废除投影。

---

## 4. 设计方案 + trade-offs

### 方案 A — 全量对齐（big-bang）
一次性上 native tool-use + SSE streaming + append-only + caching,端到端替换。
- 优:对齐最彻底,一次迁移。
- 劣:blast radius 极大,同时冲击 eval trace 契约 + deadline 模型 + Tier-0 egress
  不变式 + M5 在施工的同一批文件。**最致命**:bad-case 分布若发生漂移,无法归因到
  是哪条轴造成的 —— 而 bad-case 回归正是本仓的 §5.6 主门禁。**不推荐。**

### 方案 B — 仅流式最终答复（streaming-first / UX-first）
保留 JSON-object 协议,只给 UI 加最终 customer message 的 SSE 流。
- 优:直击最可见的 CS 痛点(感知延迟);范围看似小。
- 劣:**流式一个 JSON-object 响应本身是反模式** —— 要么流出用户不该看的 JSON token,
  要么先解析完再"假装流"(失去流式意义);且撞 Tier-0 egress redaction 不变式
  （流出去的 token 收不回);不解决成本/鲁棒/模型对齐。**轴 2 未动时,流式做不干净。**
  作为第一步是个陷阱(见 §9 compounding)。**不推荐先做。**

### 方案 C — native-tool-use 优先，流式解耦后置（推荐）
沿三轴**解耦分阶段**,每阶段独立可交付、可回滚、bad-case 可归因:

- **Phase 1（轴 2 + 轴 3，server 内部，UX 不变）**:引入原生 tool-use API +
  append-only 原生消息数组 + provider prompt caching。
  - `LlmRequest` 增 `tools`(由 `plan.allowedTools()` / Skill 声明生成,Skill-driven,
    天然契合)+ `toolChoice`;`LlmResponse` / `ChatMessage` 增结构化 `tool_calls` /
    `tool` role / `tool_call_id`。
  - `buildRequestBody` 发 `tools`;`parseResponse` 读 `message.tool_calls`;`ActionParser`
    退化为"原生 tool_calls → ToolCall"的薄适配(保留 JSON-blob 解析作 provider 兜底)。
  - `AgentRunLoopImpl` 维护 append-only 消息数组:system(=稳定 system prompt + runtime
    拥有的"状态投影"作为 system/developer 内容)+ user + assistant(tool_calls)+
    tool(result)+ …;**稳定前缀打 cache 标记**,每步只 append 新 tool 结果消息。
  - 仍以阻塞 `ResponseEntity` 返回 UI(零 UX 变更);所有 egress 安全处理仍作用在
    完整响应上(Tier-0 不变式不动)。
  - **门禁:bad-case real-LLM 回归 rerun**(协议切换是典型的可能扰动分布项)。
- **Phase 2（轴 1，UX）**:SSE 流式**仅**最终 customer-facing 答复(及可选"让我查一下"
  preamble);内部 tool-决策步仍非流式(派发前需要完整 tool_calls)。**关键约束:必须
  保留 Tier-0 egress floor** —— 设计为"先过 guardrail/redaction 再快渲染",或句级
  guardrailed 流式;替换 `ProgressPlaceholderService` 占位。
- **Phase 3（可选，前瞻）**:planning 步启用 reasoning/thinking 模型;provider 原生
  parallel tool-call;更细的 caching 分层。
- 优:高价值核心(Phase 1)UX 风险低;尊重投影治理层(投影变成 runtime 拥有的"状态"
  消息 + 原生 tool 结果消息,所有权不丢);Skill-driven allowedTools → 原生 tools 数组
  契合;**能收缩 prompt 的 escalation 枚举 if-else dump**(schema 强约束枚举,软指导
  保留)。
- 劣:多阶段(日历更长);需把 eval trace 契约**追加式**扩展。

### 方案 D — 仅加 caching（do-less 兜底）
保持 JSON-object + 阻塞,只把上下文承载改成 append-only 让 provider 缓存生效(纯省钱)。
- 优:最小。
- 劣:不解决鲁棒/延迟/模型对齐;而要让缓存生效本就得做 append-only 重构 —— 等于做了
  Phase 1 的 80% 工作只拿 20% 收益。仅在 appetite 极低时作为退路。

---

## 5. 推荐方案 + 理由

**推荐方案 C，且把整体作为 M5 之后的新 milestone(暂称 M6 — LLM Invocation
Modernization)候选,排在 M5 S3 之后、消费 S3 的字段消费矩阵。** 理由:

1. **LLM-first / anti-hardcode 同向**(§1.2 / §1.7)。原生 tool-use **增加** LLM 的原生
   agency、**减少** runtime 的模仿脚手架(JSON-blob 解析、围栏剥离、解析失败兜底),
   并能把 `system_prompt.txt:33-81` 的 escalation 枚举决策树从"prompt if-else dump"
   收缩为"schema 强约束枚举 + 软决策指导"。这不是 hardcode 改造,是**去 hardcode**。
2. **解耦 = 可归因**。本仓的验收主门禁是 §5.6 bad-case 人审 + real-LLM rerun。分阶段
   让每次分布漂移都能归因到单条轴;big-bang(方案 A)做不到。
3. **Phase 1 高杠杆低 UX 风险**。成本(G2)+ 鲁棒(G3)+ 模型对齐(G4)三个 gap 都由
   Phase 1 关闭,且对用户零可见变更 —— 适合作为先行、稳的一步。
4. **流式作为受 Tier-0 约束的独立 UX 工程**(Phase 2),不与协议改造混在一起,避免
   方案 B 的"流式 JSON-blob"反模式与 egress 安全坑。
5. **前瞻性**:6 个月后主流模型(Gemini/DeepSeek/Kimi)都以原生 agentic tool-use +
   reasoning + caching + streaming 为一等公民;Phase 1 把地基铺到正确的方向上,Phase 3
   能顺势接住 reasoning 增量,而不是继续给 `thinking:disabled` 打补丁。

---

## 6. Scope 拆分 + 交付优先级建议（deliver-agent 最终决定）

> 以下是**建议**,非绑定。milestone/sprint 的最终边界由 deliver-agent + human 在
> planning round 决定。

- **前置条件(硬)**:本提案**整体排在 M5 关闭之后**。理由见 §9 compounding —— 与
  M5 S2/S3/S4 共享 `AgentRunLoopImpl` / `ContextProjectionBuilder` / `TraceWriter` /
  trace 端点 / UI `TraceViewer` 同一批文件,且 Phase 1 的 append-only 承载是 S3
  字段消费矩阵的直接消费者。
- **建议 milestone M6(语义-touching 仅 Phase 1/2 的承载与传输,需 §7 stanza)**:
  - **M6-S1(轴 2,`infra` + `prompt_projection`)**:扩 `LlmRequest/LlmResponse/ChatMessage`
    模型 + `buildRequestBody`/`parseResponse` 走原生 tool-use + `ActionParser` 薄适配。
    单 provider(primary DeepSeek)先通,FallbackLlmClient 暂走旧协议或同步切。门禁:
    Java 套件 + 单回合 real-LLM 烟测。
  - **M6-S2(轴 3,`prompt_projection`)**:`AgentRunLoopImpl` 改 append-only 消息数组 +
    稳定前缀 cache_control;投影"状态"内容从"每步重发字符串"迁为"system/developer
    状态消息 + 原生 tool 结果消息"。**消费 M5 S3 的 C1 消费矩阵**决定每字段落点。
    门禁:**bad-case real-LLM 回归 rerun**(主门禁)。
  - **M6-S3(轴 2,provider 归一)**:FallbackLlmClient 跨 provider 原生 tool-use +
    caching 归一(DeepSeek vs Kimi 差异吸收);deadline/budget 模型对原子响应假设复核。
  - **M6-S4(轴 1,`infra`/transport)**:SSE 流式最终答复 + UI EventSource/ReadableStream;
    **保留 Tier-0 egress floor**;退役 `ProgressPlaceholderService` 占位。门禁:bad-case
    rerun(行为不应变)+ egress 安全用例。
  - **M6-S5(可选,前瞻)**:reasoning 步 + 原生 parallel tool-call。S4 关闭时定夺是否做。
- **可观测性配套(贯穿,见 §10)**:trace 端点 + S2 的 `bot_turn_llm_calls` + eval
  `get_llm_calls` 需**追加式**扩展(cached token、原生 tool_call 形态、TTFT/流式分块计时)。

---

## 7. Layer 分类 + §7 stanza 预填

### 7.1 §3.2 Fix Layer 分类
走 §3.2 decision questions:Q1(infra?)—— 传输/协议/承载/持久化是 transport + trace
契约,**主层 `infra`**;轴 3 的"投影物理承载"落 **`prompt_projection`**(Q3:改变投影
如何呈递给 LLM,但**不删/不改语义信息**,只改物理形态 + caching)。**不触 `java_guard`**
（不新增 Tier-0)、**不触 `semantic_planner`**(不替 LLM 做语义选择,反而增其 agency)。

### 7.2 §7 stanza 预填草稿（M6 语义-touching sub-sprints 用）

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra（传输 + 工具协议 + 持久化/trace 契约）
+ prompt_projection（轴 3：投影的物理承载与 caching，不改投影的语义信息集）。

**Tier-0 invariant:** 本 milestone 不新增 Tier-0。必须 PRESERVE 既有 floor：
PII redaction / forbidden-phrase / grounding floor 在 customer message egress
上的作用不变（流式阶段尤须保证 —— 不得在 guardrail 通过前把 token 流给用户）；
tool schema / capability boundary（§1.4 Runtime-owned）由原生 tools 数组承载，
口径不变；deadline/budget（§1.4）语义保留。

**Semantic hardcode:** 不引入语义 hardcode；反而**去 hardcode** —— 移除 JSON-blob
解析/围栏剥离/解析失败兜底脚手架，并将 system_prompt.txt:33-81 的 escalation_reason
枚举决策树从「prompt if-else dump」收缩为「原生 tool schema 枚举强约束 + 软决策
指导」。无新增 keyword/regex/enum/per-UC matrix；原生 tools 数组由 Skill-driven
plan.allowedTools() 生成（无 per-UC if-else）。

**Generalization coverage:** 观测/承载/传输面改造。target = FAQ/INTAKE/DISCOVER
各 UC 的多步回合在新协议下行为不变（bad-case 12 案 real-LLM rerun 分布守恒）；
neighbor = 单步回合；negative = 不应触发的工具不因协议变化被误发；shadow = 留出
held-out 多步 trace。**real-LLM rerun 是证据门禁，mocked-LLM 仅覆盖协议 wiring/渲染**
（§5.6 eval evidence gate）。
```

---

## 8. Hard fences + non-goals

**Hard fences:**
- **排序硬约束**:整体在 **M5 关闭后**启动;**Phase 1(轴 2)必须先于 Phase 2(流式,
  轴 1)**;eval trace 契约的追加式扩展先于协议翻转。
- **eval `get_llm_calls`(`GET /sessions/{id}/llm-calls`)只能追加式扩展,不得破坏**
  现有 payload(eval harness 冻结消费,`agent_client.py:169`)。
- **Tier-0 egress floor 不动**:PII / forbidden-phrase / grounding floor 对 customer
  message 的作用必须保留;流式不得在 guardrail 通过前流 token。
- **轴 3 不删/不改投影的语义信息集** —— 只改物理承载;**消费 M5 S3 的 C1 消费矩阵**
  后再定字段落点(承接 M5 §6 S3/S4 fence)。
- **不改 bot 语义决策**(UC routing / drift / escalation 选择 / Skill / 工具语义),
  bad-case 分布须守恒。

**Non-goals:**
- 不换 provider / 不改业务上的 model 选型策略(只是改"如何调用"而非"调谁")。
- 不重构 `ControlKernel` / `PhaseEvaluator` 相位机(本提案读其产物,不改其逻辑)。
- 不改 `composite.py` 评分 / 阈值 / fixture(eval 评分口径不动)。
- 不在本提案内做 Single Handover Orchestrator(独立 P0,`release_gate.md` §1.1)。
- 不顺手改 `LlmCallLogger` 的 500 字截断语义(全量在 S2 新表,保持廉价 summary)。

---

## 9. 风险 + compounding-effect 分析

### 9.1 Compounding-order（顺序错了会更糟）
1. **必须在 M5 S3 之后**:S3 把投影做成 Skill-driven 并产出 C1 字段消费矩阵。
   Phase 1 的 append-only 承载(轴 3)是该矩阵的直接消费者(决定每字段在原生消息
   数组里的落点 + 可缓存性)。**先于 S3 做 = 迁移一个即将被重构的投影面**(双倍工 +
   在 `ContextProjectionBuilder`/`AgentRunLoopImpl`/`TraceWriter` 同文件 merge 冲突),
   且投影形态变化本身会扰动 bad-case 分布,**污染 S3 的回归归因**。
2. **受益于 M5 S2**:S2 的 `bot_turn_llm_calls` per-invocation trace 让你能观测新协议
   的逐调用行为(cache 命中、原生 tool_calls、latency)。排在 S2 之后,trace 面已就绪。
3. **Phase 1(轴 2)必须先于 Phase 2(流式)**:协议未原生分离 tool/text 时,流式做不
   干净(方案 B 陷阱)。流式先行 = 要么流不可见 JSON,要么 buffer-then-fake-stream
   的废弃工。**这是最该避免的错序。**
4. **eval trace 扩展先于协议翻转**:否则 bad-case rerun 门禁读不到新 trace。

### 9.2 风险清单
1. **eval trace 冻结契约 + bad-case 回归门禁**:原生 tool-use 改变 `llm_raw_response`
   形态(结构化 vs JSON blob)。`bot_turn_llm_calls.llm_raw_response` / `LlmCallLog` /
   eval 解析须**追加式版本化**;bad-case 12 案分布(M4 close baseline:PASS×5 /
   IMPROVING×4 / FAIL×3 / OOSR×0)是回归门禁,协议改动须 rerun。
2. **provider 差异**:DeepSeek vs Kimi 的原生 tool-use / streaming / caching 细节
   （parallel call、tool_choice、分块格式)不同;`FallbackLlmClient` 跨 provider hedge
   要求抽象层归一两者。风险:某 provider 的原生模式比其 json_object 模式更不稳。
3. **deadline/budget 模型假设原子响应**:30s 截止 + 2-attempt 预算
   （`OpenAiCompatibleLlmClient.java:130-236`)假设一次拿完整响应。流式改变失败语义
   （半截响应、中途断流),流式重试更难(半截答复不能简单重发)。
4. **Tier-0 egress floor 与流式**:当前 customer message 装配成完整 `responseText`
   后整条返回(`SessionManager.java:368-370`),egress 安全处理见全文。流式逐 token
   下发会改变这个不变式(token 收不回)。**天真加流式会破 PII/forbidden-phrase 底线**
   —— 必须 buffer-then-guardrail-then-render 或句级 guardrailed 流式。
   （注:`PiiRedactionFilter`/`ForbiddenPhraseDetector` 的 egress 精确接线点需在交付期
   核对 —— grep 未见明确 customer-message-egress 调用站,可能在 trace sanitize 侧;
   无论现状如何,**完整字符串可被安全处理看到**这一不变式必须在流式后保留。)
5. **prompt-caching 前缀稳定性**:append-only 须保前缀字节稳定,否则缓存失效;
   投影里的易变片段(`accumulated_tool_results`)要 append 为新消息而非改前缀。
6. **与 active M5 的文件耦合**:同改 `AgentRunLoopImpl`/`ContextProjectionBuilder`/
   `TraceWriter`/trace 端点/UI。**强制排在 M5 之后**以避免 merge 与归因混乱。
7. **escalation 枚举迁到 schema 强约束是行为相邻改动**:schema 拒绝非法枚举值可能
   改变 LLM 实际选择的 reason。须 bad-case rerun 验证选择分布不漂移(尤其
   `user_requested` vs Tier-2 的优先级,见 `system_prompt.txt:54-65`)。

---

## 10. 可观测性 / trace / report 配套

本提案本质是 M5「Observability Coherence」论点的延续:**架构又一次领先,trace 面须跟上**。

- **admin trace** 须能渲染:每次 invocation 的**结构化原生 tool_calls**(而非 JSON-blob
  文本)、append-only **消息数组**、**cached_prompt_tokens**(缓存命中)、(Phase 2)
  **TTFT / 流式分块计时**。这是 S2「Invocation 1..N」面板的自然延伸。
- **持久化**:`bot_turn_llm_calls`(S2 新表)+ `LlmCallLog` 建议**追加** `cached_prompt_tokens`、
  原生 tool-call 形态列;`AgentRunResult.llmCallRecords` 的 `tool_calls` 字段已可承载
  结构化调用(working tree)。
- **eval `get_llm_calls` 追加式扩展**(不破冻结契约):增 cached token、native tool-call
  形态,供 eval 后验分析协议迁移的成本/鲁棒收益。
- **report.html**(M5 S1 已做四层 verdict):若引入 cost/latency 观测,可考虑在
  observation 层补 cached-token / TTFT 聚合(非门禁,纯 observation,遵 §5.5 降级原则)。
- 建议在 M6 各 sub-sprint 记录 architecture-health 信号:`new_semantic_hardcode_count`
  应为负(去 hardcode);`planner_ownership_ratio` 应上升(原生 agency 增)
  （§6 定义,采集 still not_started,可手计)。

---

## 11. 一页纸结论（给 human / deliver-agent）

- **现状(HEAD 验证)**:同步阻塞 + JSON-object-in-content + 每步重投影 + 无 caching;
  单回合最坏 ~6 次顺序阻塞 LLM 调用,每次重发 ~3k token;`thinking:disabled`;原生
  tool-use 完全未用(请求无 `tools`,响应不读 `message.tool_calls`)。
- **建议**:方案 C —— **native-tool-use 优先 + append-only/caching,流式后置且受
  Tier-0 约束**;作为 **M5 之后的新 milestone(M6)**,**消费 M5 S3 的字段消费矩阵**。
- **为什么这样**:解耦三轴 → bad-case 可归因;Phase 1 高杠杆零 UX 风险关闭成本/鲁棒/
  模型对齐三个 gap;流式作为独立受约束 UX 工程避开"流式 JSON-blob"反模式与 egress 坑;
  且整体**去 hardcode、增 LLM agency**,与 Constitution §1.2/§1.7 同向、面向 6 个月后
  主流模型能力。
- **最关键的顺序约束**:① 排在 M5 之后(共享文件 + 消费 S3 矩阵);② Phase 1(协议)
  先于 Phase 2(流式);③ eval trace 契约先追加式扩展。错序(尤其流式先行)= 废弃工 +
  污染回归归因。
- **下一步**:human 选定该提案后,交 deliver-agent 做 M6 milestone + sub-sprint 拆分
  （本文 §6 是建议,非绑定);deliver-agent 按 §4.3 决定 Codex review plan(协议/承载
  sub-sprint 是语义相邻 surface,建议 per-sub-sprint Codex 至少覆盖 M6-S1/S2/S4)。
```
