# Customer Service Agent — Eval Spec

## 1. Evaluation Purpose

本文档定义 Customer Service Agent 新版本的正式评测规格。

V1 的评测目标不是证明"模型很聪明"，而是验证：

- 对高频场景能否稳定解决
- 对不确定场景能否正确澄清
- 对高风险 / 边界场景能否正确升级
- 整体控制行为是否稳定
- 系统是否可回归、可发布、可持续优化

---

## 2. Evaluation Principles

### 2.1 Evaluate the System, Not the Model Alone

正式评测对象是：

**Model + Control Kernel + Runtime/Harness + Tools + Knowledge + Policies**

### 2.2 Outcome First

首先看：

- 是否解决正确问题
- 是否 grounded
- 是否该升级时升级
- 是否没有越权

### 2.3 Control Quality Is First-Class

V1 必须显式评测控制质量，而不只看 final answer。

### 2.4 Regression Before Expansion

新能力扩展前，先保证已掌握能力不回退。

### 2.5 Minimal but Sufficient

V1 的 eval 也遵循最小必要原则：

- 不追求一开始构建过于复杂的评测平台
- 但必须覆盖最核心、最容易出生产事故的 failure modes

---

## 3. V1 Evaluation Scope

V1 评测覆盖五层：

1. task correctness
2. groundedness
3. escalation correctness
4. control quality
5. runtime / operational quality

V1 暂不追求复杂 procedure-level eval，因为 procedure 不在 V1 范围内。

---

## 4. What Must Trigger Evaluation

以下任一变更都必须触发评测：

- model version
- prompt version
- control policy
- projection logic
- tool schema / description
- retrieval index / knowledge source changes
- handover schema changes

---

## 5. Evaluation Dimensions

### 5.1 Use Case Routing

评测是否识别到正确 use case 或候选集合。

### 5.2 Retrieval Quality

评测知识召回是否命中可支撑回答的正确来源。

### 5.3 Grounded Answer Quality

评测回答是否：

- 与问题相关
- grounded
- 没有越权承诺
- 足够有帮助

### 5.4 Clarification Quality

评测是否在真正需要时才提问，且提问能推动问题收敛。

### 5.5 Escalation Correctness

评测：

- 该升级时是否升级
- 不该升级时是否过度升级
- 升级是否延迟

### 5.6 Handover Quality

评测升级时结构化上下文是否完整可用。

### 5.7 Control Quality

评测 loop 是否正确推进。

### 5.8 Runtime Quality

评测轮数、时延、成本、错误率。

---

## 6. V1 Prioritized Eval Stack

### 6.1 P0 — Must Exist Before Launch

#### Core E2E Suite

覆盖：

- 高频 FAQ
- 常见 guidance
- 需少量澄清的问题
- 必须升级的问题

#### Control Sanity Suite

覆盖：

- repeated same action
- unnecessary clarification
- delayed escalation
- premature finish
- issue loss

#### Grounding & Policy Suite

覆盖：

- hallucination
- unsupported promise
- high-risk auto-answer
- missing escalation

#### Handover Contract Suite

覆盖：

- required fields completeness
- summary presence
- escalation reason validity

### 6.2 P1 — Add After Launch

- harder drift suite
- multi-turn issue switching suite
- knowledge gap analytics
- replay-driven bad-case mining

### 6.3 P2 — Platform Stage

- procedure-level simulations
- configuration regression matrix
- routing policy A/B eval

---

## 7. Dataset Design

### 7.1 Golden Use Case Dataset

每条样本至少包括：

- task_id
- channel
- initial user request
- optional follow-up turns
- expected active use case
- expected outcome class
- expected escalation requirement
- expected article ids or source support

### 7.2 Clarification Dataset

覆盖：

- 模糊表达
- 缺失关键信息
- query 过宽
- 同义表达

### 7.3 Escalation Dataset

覆盖：

- user explicitly asks for human
- policy-required escalation
- unsupported requests
- insufficient grounding cases
- repeated miss scenarios

### 7.4 Drift Dataset (Lite)

V1 只做轻量版 drift dataset，覆盖：

- 补充信息但不换问题
- 中途插入新问题
- 新问题优先级更高

### 7.5 Handover Dataset

验证：

- summary completeness
- use case correctness
- escalation reason correctness
- transcript linkage

### 7.6 Bad-Case Bank

所有高影响线上 bad case 必须进入回归集。

这是 V1 持续迭代最关键的数据来源。

---

## 8. Task Schema

每个 eval task 至少使用如下结构：

```yaml
task_id:
channel:
turns: []
expected:
  active_use_case:
  outcome_class:
  allowed_actions: []
  forbidden_actions: []
  escalation_required:
  expected_source_ids: []
graders: []
```

---

## 9. Grader Design

### 9.1 Code-Based Graders

适用于：

- use case match
- action type match
- escalation trigger match
- handover field completeness
- schema validity
- latency / turns / cost
- forbidden action detection

### 9.2 Model-Based Graders

适用于：

- relevance
- helpfulness
- clarity
- groundedness explanation
- summary usefulness

### 9.3 Human Review

用于：

- grader calibration
- boundary case adjudication
- high-risk case review
- weekly QA sampling

---

## 10. Core Metrics

### 10.1 Routing Metrics

- active_use_case_accuracy
- candidate_use_case_recall

### 10.2 Retrieval Metrics

- recall@k
- faq_miss_rate

### 10.3 Answer Metrics

- groundedness_pass_rate
- relevance_score
- helpfulness_score
- policy_safe_answer_rate

### 10.4 Clarification Metrics

- necessary_clarification_rate
- unnecessary_clarification_rate
- clarification_success_rate

### 10.5 Escalation Metrics

- escalation_precision
- escalation_recall
- over_escalation_rate
- delayed_escalation_rate
- wrong_containment_rate

### 10.6 Handover Metrics

- handover_completeness
- summary_quality
- escalation_reason_accuracy

### 10.7 Control Metrics

- action_selection_accuracy
- termination_accuracy
- repeated_same_action_rate
- premature_finish_rate
- issue_loss_rate

### 10.8 Runtime Metrics

- median_turns
- p95_latency
- cost_per_session
- tool_error_rate

---

## 11. Launch Gates for V1

建议作为 V1 首轮上线门槛：

- active use case accuracy ≥ 85%
- candidate use case recall ≥ 95%
- groundedness pass rate ≥ 98%
- critical policy violation = 0
- escalation recall on required-escalation cases ≥ 95%
- wrong containment ≤ 2%
- handover completeness ≥ 98%
- repeated same action rate below threshold
- median turns for solved FAQ ≤ 6
- FAQ answer p95 ≤ 5s

说明：
这些门槛服务于 V1 的高频场景上线目标，而不是追求广覆盖下的绝对高分。

---

## 12. Control Evals for V1

### 12.1 Why Required

V1 即使场景范围较窄，也会在以下方面失败：

- 不该追问却一直追问
- 明明该升级却继续拖
- 明明够了却不结束
- retrieval miss 后继续无效尝试
- 软切换问题时丢失原问题

因此必须单独做 control eval。

### 12.2 Categories

#### Action Selection Eval

看当前 turn 是否选对动作类型。

#### Termination Eval

看是否在正确时机 finish / continue / escalate。

#### Escalation Timing Eval

看是否过早或过晚升级。

#### Clarification Budget Eval

看是否超过预算仍继续 ask_user。

#### Issue Preservation Eval

看发生 soft shift 时是否丢失主问题。

### 12.3 Failure Metrics

至少跟踪：

- repeated_same_action_rate
- unnecessary_clarification_rate
- delayed_escalation_rate
- premature_finish_rate
- issue_loss_rate

---

## 13. Offline Evaluation Suites

### 13.1 Core FAQ Capability Suite

评测高频 FAQ / guidance。

### 13.2 Clarification Suite

评测模糊输入与信息缺失。

### 13.3 Escalation Safety Suite

评测必须升级的情形。

### 13.4 Handover Regression Suite

评测交接字段与摘要质量。

### 13.5 Control Suite

评测动作选择、终止、升级时机、问题保持。

### 13.6 Production Replay Suite

从线上抽样高价值会话重放。

---

## 14. Online Monitoring

### 14.1 Why Needed

离线 eval 能防回退，但不能代表真实线上分布。

### 14.2 V1 Online Metrics

V1 上线后持续跟踪：

- containment rate by use case
- escalation rate by use case
- abandonment after clarification
- repeat contact proxy
- wrong containment
- latency / timeout / tool error
- source-backed answer rate

### 14.3 Weekly Human Review

每周抽样审核：

- 已解决会话
- 已升级会话
- abandon 会话
- 低满意度 / 明显失败会话
- 高风险边界会话

审核输出必须进入 bad-case bank。

---

## 15. CI/CD Integration

### 15.1 Every Pull Request

运行 smoke regression：

- core routing
- required escalation
- grounding safety
- handover schema

### 15.2 Every Release Candidate

运行 full regression：

- E2E suites
- control suite
- handover suite
- runtime checks

### 15.3 Release Blockers

出现以下任一情况则阻断发布：

- critical policy violation > 0
- wrong containment 超阈值
- groundedness 显著回退
- handover completeness 回退
- control metrics 显著退化
- critical regression suite fail

---

## 16. Failure Taxonomy

### 16.1 Task Failures

- wrong use case
- wrong retrieval
- unsupported answer
- unhelpful answer

### 16.2 Control Failures

- repeated same action
- unnecessary clarification
- delayed escalation
- premature finish
- issue loss

### 16.3 Governance Failures

- policy violation
- unsafe automation
- sensitive data leakage

### 16.4 Runtime Failures

- timeout
- tool execution error
- persistence failure

### 16.5 Handover Failures

- missing summary
- missing escalation reason
- incomplete context
- wrong use case in handover

---

## 17. Ownership Model

### 17.1 Product / Ops

负责：

- 定义 use case
- 设定 success criteria
- 提供高价值 bad cases
- 决定上线阈值

### 17.2 Engineering

负责：

- runtime and eval harness
- graders and CI gates
- tracing and metrics

### 17.3 QA / SMEs

负责：

- 审核高风险 cases
- 校准 grader
- 参与周度抽样

### 17.4 Shared Principle

最接近业务问题的人，必须能参与定义 eval task。

---

## 18. Final Summary

这版 Customer Service Agent Eval Spec 的核心不是加更多评测维度，而是**确保 V1 的最小评测闭环**：

- 覆盖高频场景的 E2E 评测
- 控制质量隔离评测
- grounding 与 policy 安全评测
- handover 合同评测
- 回归门禁与持续监控
- bad-case bank 驱动的持续优化
