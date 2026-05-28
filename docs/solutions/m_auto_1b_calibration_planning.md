---
title: M-Auto-1B — Auto-Evolution Calibration Milestone (planning round)
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (until milestone_objective.md picks up)
authored_by: research-agent
authored_date: 2026-05-28
mode: forward-looking
supersedes: []
superseded_by: null
notes: >
  Path 1 research-driven planning round triggered immediately after
  M-Auto-1A close (2026-05-28 A — Clean PASS sub-classified
  `approve with downgrade-to-signal follow-up`). M-Auto-1A 交付了
  完整 build-time substrate（`autoloop/` 子系统：sandbox + 4-tier
  fitness + loop + meta-agent + 3-layer memory + applier real-impl
  + anti-hardcode kernel + content_validator + 7 gaming checks；
  216 pytest PASS）但故意 WAIVED-BY-HUMAN 把 live-iter 推迟到 M-Auto-1B
  第一步执行（OQ-S56.5 anticipated path）。本 proposal 把 M-Auto-1B
  scope 锁定为 "calibration milestone"：补齐 live-iter 前置条件 + 跑
  第一次真实迭代 + 消费 `R-S57-anti-hardcode-whenever-arrow-synonym-bypass`
  detector-calibration follow-up + 跑第一夜 batch + 第一次 human review
  batch + 第一次 cherry-pick to main。

  人类已经在 M-Auto-1A close lead (`docs/10-handoff.md` §0 / §1)
  明确指明 M-Auto-1B 是 calibration milestone，本 proposal 不重新论证
  方向，只对 sub-sprint 切片 / R-S57 fix path / hard fences /
  compounding effects 做 deep dive 并给出 2 个方案 + 推荐。
---

# M-Auto-1B — Auto-Evolution Calibration Milestone (planning round)

> 阅读顺序。§1 executive summary。§2 是 code-grounded 现状（autoloop/
> 子系统已交付 surface + 缺失的 live-iter prereqs + R-S57 detector
> bypass 的精确技术原因）。§3 gap analysis（M-Auto-1B 必须解决的 4 个
> 维度 + 与 §1 的对齐度）。§4 design alternatives（sub-sprint 切片 +
> R-S57 fix path × 3）。§5 推荐方案 + 理由。§6 sub-sprint 拆分建议。
> §7 layer classification + §7 stanza 预填。§8 hard fences + non-goals。
> §9 风险 + compounding effects。§10 observability / report 配套需求。

## 1. Executive summary

M-Auto-1A 把 auto-evolution 的**结构性防御 substrate** 全部 ship 完成，
代价是把 "第一次跑真实 LLM iteration" 显式推迟到 M-Auto-1B（OQ-S56.5
WAIVED-BY-HUMAN 2026-05-27 + S-Auto-4 Codex Axis B 留下了
`R-S57-anti-hardcode-whenever-arrow-synonym-bypass` 这条 contract-expected
calibration follow-up）。M-Auto-1B 是这条逻辑路径上的下一步——第一次让 loop
真正接触一个 real meta-agent LLM 的 propose 输出。

**本 proposal 的核心论点**：M-Auto-1B 的成败不在 "跑了多少 iterations"，
而在 **"detector 在面对 real meta-agent propose 输出时的 calibration 是
否准"**。原因：

1. S-Auto-4 calibration 表的 11 forbidden + 4 clean + 2 borderline = 17
   sample 全是 deliver-agent + research-agent 手写的 synthetic
   fixture，是 **synthetic distribution**；real meta-agent (DeepSeek /
   Kimi / Anthropic-compatible AICodeWith) 写出的 procedure / desc 叙事在
   长度、结构、abstract-vs-concrete 风格上与 synthetic 完全不同。
2. R-S57 已经在 close 时给出了一个 real Codex adversarial 的 bypass
   datapoint（`Whenever ... =>`）；M-Auto-1B 必须把这条 datapoint 扩展为
   一个 ≥10 sample 的 real-meta-agent calibration batch，再决定 detector
   的 `synonym_map_enabled` toggle + 任何新增 rule。
3. 如果 detector calibration 不准就直接 overnight batch，要么 FLAG_FOR_CODEX
   rate 失控（>25% 警戒阈，会触发 detector noisy 告警）淹没 human
   review；要么相反，false-negative 放过 §1.7 borderline 改动让一个
   "drift to keyword bot" 候选最终被 cherry-pick 进 main。

**推荐 calibration-first 切片（详见 §5）**：M-Auto-1B 拆 2 个 sub-sprint
（保留 §8.5 ceiling 余量给可能的 fix-iteration sub-sprint）：

- **S-Auto-5 — Live-iter bootstrap + detector calibration**：补齐 live-iter
  3 个 prereqs（built `server/` jar / `AUTOLOOP_META_LLM_API_KEY` /
  clean working tree）+ 跑 1-3 个真实 iteration（结果 keep/discard 不
  作为 close gate；目的是产生 ≥10 个 real meta-agent propose sample
  作为 R-S57 calibration 输入）+ 消费 R-S57（推荐 hybrid path：先扩
  synonym map word-boundary，再用 real-meta-agent batch 决定
  `synonym_map_enabled` toggle 是否翻 default）+ 重跑 17-fixture
  calibration sweep + 实跑 real-meta-agent batch + 决定 FLAG_FOR_CODEX
  阈值是否需要重新校准。
- **S-Auto-6 — First overnight batch + first human review + first
  cherry-pick to main**：第一夜 10-20 iterations + 第二天 human +
  deliver-agent §5.6 风格 manual review + 至少 1 个 human-approved
  kept candidate cherry-pick 到 main（这是首次 auto-loop output 真的
  进生产 main 的事件，需要 deliver-agent + human 双签名）+ 校准
  per-iteration elapsed time + 累计 `shadow_disagreement_rate` /
  FLAG rate / gaming flag rate observation。

**为什么不直接一夜跑完所有事情**：因为 detector calibration 与 overnight
batch 之间有强 compounding effect（§9.2）。calibration 不准 →
overnight FLAG flood OR false-negative slip-through → human review
batch 浪费时间 OR 进了 borderline 改动到 main。两个 sub-sprint 切开让
calibration evidence 先固化，再让 overnight batch 在校准好的 detector
上运行。

## 2. Current-state survey (code-grounded; 2026-05-28 HEAD `b6b627b`)

### 2.1 autoloop/ 子系统已交付的 surface

通过 M-Auto-1A 累积 commit range `1fb2062..b6b627b`（9 commits 含
close-prep `ae5ebc8`），下列 `autoloop/` 子系统已完整 ship（HEAD 验证）：

| 模块 | 文件 | 状态 |
|---|---|---|
| Mutable-surface contract | `autoloop/program.md` (315 LOC) | LOCKED contract 已写定 §1-§8 + Appendix；§4 status table 标 sandbox / fitness / anti-hardcode / shadow firewall / cherry-pick guard 全 DELIVERED |
| YAML diff sandbox | `autoloop/autoloop/sandbox/yaml_diff_validator.py` | S-Auto-1 ship；4 positive + 19 negative fixtures |
| 4-tier lexicographic fitness | `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader}.py` | S-Auto-2 ship；v1 narrow dataset 12 bad_cases + 12 anchor_outcome + 23 shadow = 47 cases per iter |
| Loop orchestrator | `autoloop/autoloop/loop.py` (754 LOC after S-Auto-4 +151 wiring) | S-Auto-3 ship；14-step state machine + crash recovery + cleanup-in-finally |
| Meta-agent | `autoloop/autoloop/meta_agent/{analyzer,proposer,lessons_compactor,llm_client}.py` + `prompts/{analyze,propose,compact}.txt` | S-Auto-3 ship；proposer 强制 v1 mutable-surface schema + ProposerInvalidOutputError 3-retry |
| 3-layer memory | `autoloop/autoloop/memory/{experiments_log,iterations_index,lessons_log}.py` | S-Auto-3 ship；JSONL append-only + sqlite3 stdlib + markdown |
| Applier (real-impl) | `autoloop/autoloop/sandbox/applier.py` (505 LOC) | S-Auto-3 ship；cross-file rejection + git branch ops + YAML field patch + free-port socket bind + mvn alt-port Spring spawn + 120s actuator health probe + SIGTERM/SIGKILL cleanup |
| Anti-hardcode kernel | `autoloop/autoloop/sandbox/anti_hardcode_check.py` (413 LOC) | S-Auto-4 ship；11 rules Q1×4+Q2×2+Q4×2+Q5×3；PASS/FAIL/FLAG_FOR_CODEX；NFKC+lowercase+whitespace+optional synonym map normalization |
| Content validator | `autoloop/autoloop/sandbox/content_validator.py` (187 LOC) | S-Auto-4 ship；5 rules zero_length/deny_list/length_overflow/length_underflow/placeholder_corrupted |
| Gaming detector | `autoloop/autoloop/scoring/gaming.py` (627 LOC) | S-Auto-4 ship；7 checks observation-only in v1 |
| CLI | `autoloop/autoloop/cli.py` (19.3K) | S-Auto-3 ship；`check`/`dry-run`/`run`/`apply`/`audit` 5 subcommands fully wired |
| Tests | `autoloop/tests/test_*.py` | 216 PASS = 48 (S-Auto-1) + 35 (S-Auto-2) + 64 (S-Auto-3) + 69 (S-Auto-4) |

`config.yaml`（205 LOC）blocks：`meta_agent` / `fitness` (含
`scoring_code_baseline_sha`) / `content_validator` / `anti_hardcode`
(`enabled: true`, `synonym_map_enabled: false`,
`flag_for_codex_rate_warn_threshold: 0.25`) / `gaming`
(`observation_only_in_v1: true`)。

### 2.2 缺失的 live-iter prereqs（M-Auto-1A 显式 WAIVED）

`docs/10-handoff.md` §0 Field `Next action` 项 (v) 与
`docs/sprints/sprint-056-handoff.md:601-607` 明确指出：

> A real live iter requires (a) `AUTOLOOP_META_LLM_API_KEY` 填在
> `.env.local`，(b) 一个 built `server/` artefact + `mvn` on PATH，
> (c) 一个 clean working tree on autoloop-branch。

HEAD 验证（2026-05-28）：

- **(a) META LLM API key**：M-Auto-1A close 时 deliver-agent 通过
  `AskUserQuestion` 确认 `autoloop/.env.local` IS set（provider AICodeWith
  / Anthropic-compatible）。**已就绪**。
- **(b) Built `server/` jar**：`ls server/target/` 显示
  `classes/ generated-sources/ generated-test-sources/ maven-status/
  surefire-reports/ test-classes/`——**没有 .jar 文件**。`applier.py`
  的 mvn alt-port Spring spawn 需要 jar；当前 prereq 缺失。M-Auto-1B
  第一个动作必须是 `mvn package -DskipTests=false` 或等效命令把 jar
  build 出来。
- **(c) Clean working tree**：M-Auto-1A close 之后由 human commit
  close-bundle 后理论上是 clean 的；S-Auto-5 dev session 启动前需要
  `git status` 二次确认。

### 2.3 R-S57 的精确技术原因（HEAD 验证）

`autoloop/autoloop/sandbox/anti_hardcode_check.py` 当前
`_SYNONYM_MAP` 结构（HEAD lines 70-83）：

```python
_SYNONYM_MAP = {
    " equals ": " = ",
    " equal ": " = ",
    " is equal to ": " = ",
    "->": "→",
    "=>": "→",
    "==>": "→",
    " when ": " if ",
    " whenever ": " if ",
}
```

Q1 IF/THEN regex（HEAD lines 121-130）：

```python
_RE_Q1_IF_THEN = re.compile(
    r"\bif\b[\s\S]{1,120}?\bthen\b[\s\S]{1,120}",
    re.IGNORECASE,
)
_RE_Q1_ARROW_TREE = re.compile(
    r"\bif\b[\s\S]{1,120}?→[\s\S]{1,120}",
    re.IGNORECASE,
)
```

Codex Axis B adversarial（`docs/codex-findings.md` 已归档到
`docs/milestones/M-Auto-1A_codex-review.md`；同步记录在
`docs/sprints/sprint-057-handoff.md`）：

> `Whenever the customer describes an appeal => route to escalation and skip normal triage.`

bypass 链路（HEAD 调用栈推演）：

1. `_normalize()` NFKC + lower + whitespace collapse →
   `whenever the customer describes an appeal => route to escalation and skip normal triage.`
2. 即使 `synonym_map_enabled=true`：`" whenever "` 键带左右空格，
   句首 `whenever` 没有左侧空格 → **synonym map 未匹配** → `whenever`
   不被替换为 `if`。
3. `=>` → `→` 替换会发生，得到：`whenever the customer describes an
   appeal → route to escalation and skip normal triage.`
4. `_RE_Q1_IF_THEN` 与 `_RE_Q1_ARROW_TREE` 都用 `\bif\b` 锚定首词；
   `\bwhenever\b` 不匹配 → Q1 不 fire → 整个 detector verdict `PASS`。

这就是为什么 R-S57 即使在 `synonym_map_enabled=true` 时仍 bypass。

### 2.4 §5.6 / shadow 在 M-Auto-1A close 的 5/34 drift 观察

`docs/milestones/M-Auto-1A_objective.md` §12 closure verdict 与
§6.2 都记录了一项 cross-milestone observation：

- bad-case rerun 2/12 programmatic drift (`cs012_uc_fp_late_phone_failure_path` +
  `wmkb_uc_a_trader_flag_secondary_uc_h` 都是 `True/0.5 → False/0.0`)
- shadow rerun 3/22 programmatic drift (`cs11s01_uc_d_two_emails_one_account` +
  `cs95s01_uc_d_phone_changed_no_messages` + `cs95s02_uc_d_logged_in_different_browser_no_ads`
  都是 `True/0.5 → False/0.0`)
- 5/34 = 14.7%；**all one-direction `True/0.5 → False/0.0`**；none False→True
- 受影响 cluster: 3 UC-D in shadow + 1 UC-FP + 1 UC-A in bad-case
- bot byte-identical to M5 close confirmed (`git diff --stat 1fb2062..b6b627b
  -- server/ eval/src/main/java/ eval_interactive/eval_interactive/ server/src/main/resources/`
  empty)

M-Auto-1A 的 planning input 明确写：

> M-Auto-1B planning input is "watch for provider drift signal during overnight;
> do NOT auto-discard candidates because of this close-day drift pattern"

这是 M-Auto-1B 必须在 overnight batch 之前 baselin 化处理的 noise floor。

### 2.5 已经存在的 v1 narrow dataset (47 cases / iter)

`docs/milestones/M-Auto-1A_objective.md` §2 "Dataset scope" 已 freeze：

- **per-iteration fitness suite**：`bad_cases` (12) + `anchor_outcome`
  (12) + `shadow` (23) = **47 cases**
- expected ~12-15 min per iteration
- **anchor 159 NOT 使用**（per-iteration fitness signal；legacy
  `expected_tool_sequence` 未经 §5.6 manual review 校验）。anchor 只
  在每个 milestone close 跑一次作 backward-compat check。

这是 M-Auto-1B 的 per-iteration 时间预算基础。Spring restart ~30s +
3 suite 串行 + meta-agent LLM 调用 ~15s × 2 (analyze + propose) +
applier git ops ~5s + tier_evaluator ~5s → 单 iter **target ~15-25 min**；
overnight 6-8 小时 budget 下能跑 **12-24 iterations**。

## 3. Gap analysis — M-Auto-1B 必须解决的 4 个维度

M-Auto-1A 的 close lead 把 "next planning round" 表述为 "M-Auto-1B
candidate selection"；human 在 M-Auto-1A close 时（2026-05-28
`docs/10-handoff.md` §1 末段）也明确：

> M-Auto-1B's first sub-sprint scope decision is whether to
> (a) extend synonym map to cover word-boundary `\bWhenever\b` /
> `\bWhen\b` + `=>`/`->` normalization OR
> (b) add FLAG_FOR_CODEX rule for equivalent WHEN/arrow decision-tree
> shapes, OR
> (c) hybrid.

这是 M-Auto-1B 的一个具体 sub-sprint scope question；但 M-Auto-1B 的总
scope 远不止 detector calibration。本节把 M-Auto-1B 必须解决的 4 个独立
维度列出。

### 3.1 Live-iter pipeline 必须真的跑通一次

M-Auto-1A close gate (5) WAIVED-BY-HUMAN 的代价是 M-Auto-1B 第一步必须
补上。具体要求（来自 `docs/sprints/sprint-056-handoff.md:601-607` +
OQ-S56.5 + `docs/milestone_objective.md` §5 acceptance bar）：

1. **`mvn package`**：build `server/target/*.jar`
2. **API key smoke**：`autoloop/.env.local` 已含 `AUTOLOOP_META_LLM_API_KEY`
   (M-Auto-1A 已确认)。 dev session 需要 `unset && set` 后实跑
   `python -m autoloop check` 验证。
3. **Single real iteration**: `python -m autoloop run --experiments 1`
   不带 `--dry-run`。完整 14-step state machine 必须无 crash 跑完；
   verdict (keep / discard / error) 都可接受——目标是 pipeline-level
   smoke。
4. **Per-iteration elapsed time observation**：记录 wall-clock；如 >40
   min 触发 `docs/milestone_objective.md` §10 stop condition 3 类的
   re-investigation R-item。
5. **Capture real meta-agent propose outputs**：把这 1-3 个 iteration
   产出的 hypothesis JSON + diff YAML 作为 §3.2 calibration 的输入。

### 3.2 R-S57 detector calibration（人类明示的 sub-sprint question）

R-S57 是 contract-expected `1 bypass / 3 → approve with
downgrade-to-signal follow-up` 的 follow-up trigger，需要在 M-Auto-1B
按 `docs/action_bank.md` §5 列出的 3 个 implementation path（extend
synonym map / FLAG_FOR_CODEX equivalent / hybrid）中选一个，并通过 real
meta-agent batch 校验。

**Calibration 必须满足的硬约束**（来自 §5.6 anti-hardcode kernel D2 +
S-Auto-4 calibration table）：

- 现有 17-fixture calibration table 必须仍保持 **11/11 forbidden FAIL +
  4/4 clean PASS + 2/2 borderline FLAG_FOR_CODEX**（detector 改动不能
  让现有 clean 案例 false-positive 也不能让 forbidden 案例漏检）。
- 现有 detector self-discipline 三个 regression test 必须仍 PASS：
  - `test_detector_source_does_not_hardcode_eval_case_ids` (grep `cs011 cs015 cs042 cs101 cs59s`)
  - `test_detector_source_does_not_hardcode_user_utterance_literals` (grep `my account is locked / closure_criterion / expected_behavior / source_session_id / primary_uc / failure_tags`)
  - `test_detector_source_rule_count_bounded` (≤30 rules)
- `flag_for_codex_rate_warn_threshold: 0.25`：在 real-meta-agent batch
  上 FLAG_FOR_CODEX 实测 rate 必须 <25%；若 ≥25% 触发 detector noisy
  告警，必须 narrow regex 或 down-regulate rule。

### 3.3 第一夜 batch 必须有 baseline 化的 noise floor

M-Auto-1A 的 close 观察明确：5/34 (14.7%) 单向 `True/0.5 → False/0.0`
drift = LLM-provider non-determinism，**不是** bot regression。这意味着
overnight batch 跑出来的 keep/discard verdict 里也会有这一层 noise——
有些 "regression-looking" 实际是 provider drift。

M-Auto-1B 处理 noise floor 的两种路径：

- **Path A — pre-batch baseline rerun**: overnight batch 前先 rerun
  baseline (M-Auto-1A close 时的 baseline) 一次，建立当晚的 drift
  envelope；overnight batch 的 keep/discard 与 envelope 对比，>envelope
  才算 real regression。
- **Path B — accept noise and use shadow as primary anti-overfit signal**:
  接受 provider drift；shadow regression gate (3% drop) 是主要保险。

`docs/proposals/autoloop_design.md §5.3` 已提出 temperature=0 + fixed
seeds 来压住部分 noise；M-Auto-1A 沿用了这个原则，但仍然观察到 14.7%
drift——说明 temperature=0 不能完全消除 provider 端的 model swap /
version pin / inference batching 引起的漂移。M-Auto-1B 需要把这层 noise
显式建模（推荐 Path A，详见 §5）。

### 3.4 First cherry-pick to main 的人类决策框架

`autoloop/program.md` §3.A 的 hard fence #7：

> No cherry-pick to main of any `autoloop/exp-N` branch in M-Auto-1A.

M-Auto-1B 的目标之一是 **首次** 把一个 auto-loop 产出的 Skill YAML edit
cherry-pick 进 main。这需要一个明确的 human-decision-framework，至少
包含：

1. **kept candidate 的程序化通过证据**（lexicographic 5 layers 全 PASS
   + shadow gate PASS + anti-hardcode FAIL=0 + content_validator PASS）
2. **§5.6-style manual review** 至少对该 candidate 在 bad_cases (12) +
   anchor_outcome (12) 上的 per-turn trace 抽样阅读，判断 PASS / FAIL /
   IMPROVING 是否与 LLM-provider non-determinism 区分开
3. **deliver-agent + human 双签名**：deliver-agent 写一段 close 笔记，
   human 在 AskUserQuestion 显式确认
4. **applier cherry-pick 操作**：使用现有 `python -m autoloop apply
   --experiment exp-<N>` 的 Hybrid 模式（OQ-S55.1 disposition 2026-05-27：
   cherry-pick + emit baseline patch + NO auto-commit；human 手动 commit）

这一框架在 M-Auto-1B 走通一次，就成为 future cycles 的标准
operating procedure。

## 4. Design alternatives + trade-offs

### 4.1 Sub-sprint slicing alternatives

#### Alt-S1 — 单一 sub-sprint（粗粒度；不推荐）

S-Auto-5 一个 sub-sprint 覆盖：live-iter bootstrap + detector
calibration + overnight batch + human review + first cherry-pick。

**优点**：minimum milestone overhead；与 §8.5 "milestone-of-one
sub-sprint" 一致。

**缺点**：
- dev session scope 过大，单一 prompt 难以自包含
- calibration evidence 与 overnight batch 强耦合在一个 commit；codex
  review 难定位 fix-iteration 范围
- 若 detector calibration 在 batch 中暴露问题，必须回退后重跑——浪费
  ~6h
- 没给 deliver-agent + human "calibration 后再启动 overnight" 的决策点

#### Alt-S2 — 2 sub-sprints（推荐）

- **S-Auto-5 — Live-iter bootstrap + detector calibration**：
  - (i) 补齐 live-iter prereqs (mvn package + .env.local verify + working tree clean)
  - (ii) 跑 1-3 个真实 iteration，产出 ≥10 real meta-agent propose sample (hypothesis JSON + after_value)
  - (iii) 消费 R-S57：实施 hybrid fix path（详 §4.2 推荐）
  - (iv) 重跑 17-fixture sweep（必须 11/11+4/4+2/2）
  - (v) 实跑 real-meta-agent batch calibration（FLAG rate <25%）
  - (vi) 决定 `synonym_map_enabled` default toggle 是否翻；通过 calibration evidence 才翻
  - (vii) per-iteration elapsed time observation (target <40 min)
- **S-Auto-6 — First overnight batch + first human review + first cherry-pick**：
  - (i) pre-batch baseline rerun（建立 drift envelope per §3.3 Path A）
  - (ii) overnight batch 10-20 iterations
  - (iii) 第二天 deliver-agent + human §5.6-style manual review of kept candidates
  - (iv) 至少 1 个 human-approved cherry-pick to main
  - (v) 累计 `shadow_disagreement_rate` / FLAG rate / gaming flag rate observation
  - (vi) report.html 生成与 admin trace 联调（如有需要）

**优点**：
- calibration evidence 在 S-Auto-5 close 时固化；overnight batch 在
  S-Auto-6 跑在已 calibrated 的 detector 上
- 每个 sub-sprint scope 自包含；dev prompt 可写自包含 §9 prompt-artifact
  invariant 合格
- S-Auto-5 codex review 单独评估 calibration 决策；S-Auto-6 codex 评估
  overnight + cherry-pick 决策
- 若 S-Auto-5 calibration 暴露问题，可触发 fix-iteration S-Auto-5.1，
  不影响 S-Auto-6 启动

**缺点**：
- milestone duration ~2-3 calendar weeks vs Alt-S1 ~1-1.5 weeks
- 2 个 sub-sprint 各自的 Codex review (deferred to milestone-shared per
  §4.3 default unless detector touches §1.7 surface) 比单 sub-sprint 多
  一道 review-time

#### Alt-S3 — 3 sub-sprints（细粒度）

- S-Auto-5 — Live-iter bootstrap only (prereqs + 1-3 real iter；产出
  calibration sample 但不消费 R-S57)
- S-Auto-6 — Detector calibration (consume R-S57 + 17-fixture sweep +
  real-meta-agent batch)
- S-Auto-7 — Overnight + human review + cherry-pick

**优点**：每个 sub-sprint 最小、scope 最清晰；S-Auto-5 与 S-Auto-6 间
可以让 deliver-agent + human 充分对 calibration evidence 复核。

**缺点**：
- milestone duration ~3-4 calendar weeks
- S-Auto-5 与 S-Auto-6 之间的 commit 边界是人为切，dev 在 S-Auto-5 跑
  完 live-iter 后还要再起 S-Auto-6 session；context overhead 大
- §8.5 sub-sprint count 已经接近 ceiling (5)，若 S-Auto-5/6/7 都顺利
  无 fix-iteration 还好，一旦任何 sub-sprint 出现 fix-iteration 就突破
  ceiling

### 4.2 R-S57 fix path alternatives（detector calibration sub-decision）

R-S57 的 3 个 implementation path（来自 `docs/action_bank.md` §5 R-S57
entry）：

#### Fix-A — Extend synonym map word-boundary substitution

在 `_normalize()` 中，**BEFORE Q1 regex evaluation**，添加 word-boundary
regex 替换：`\b(?:Whenever|When)\b` → `if`。具体写法：

```python
# After NFKC + lower + whitespace, before whole-string synonym map:
if synonym_map_enabled:
    norm = re.sub(r"\b(?:whenever|when)\b", "if", norm)
    for src, dst in _SYNONYM_MAP.items():
        norm = norm.replace(src, dst)
```

**优点**：minimal diff，结构性闭合 bypass 路径；保持 deterministic regex
路径，不引入 LLM-judge。

**缺点**：
- `when` 是英文极常见词，在 clean prose 中（"when the user describes"）
  扩展到 `if the user describes` 可能让 Q1 IF/THEN 误触发——必须通过
  real-meta-agent calibration 验证 FP rate
- 不能盲翻 `synonym_map_enabled: false → true`；必须先以 false flagged 跑
  calibration sweep 看 17-fixture + real-meta-agent 双结果

#### Fix-B — Emit FLAG_FOR_CODEX for equivalent WHEN/arrow shapes

不修改 Q1 IF/THEN 路径，新增一条 FLAG-only rule（不 auto-FAIL）：

```python
_RE_Q1_WHEN_ARROW_FLAG = re.compile(
    r"\b(?:whenever|when)\b[\s\S]{1,120}?(?:→|=>|->)[\s\S]{1,120}",
    re.IGNORECASE,
)

def _q1_when_arrow_flag(text: str) -> tuple[str, str] | None:
    m = _RE_Q1_WHEN_ARROW_FLAG.search(text)
    if m:
        return (_trim(m.group(0)), _FLAG)  # FLAG_FOR_CODEX, not FAIL
    return None
```

**优点**：
- 不让 detector 自动 reject 一个仍属 LLM-judgment-call 的 borderline
- 与 detector "PASS / FAIL / FLAG_FOR_CODEX" 三态契约一致——FLAG 不
  discard，让 Codex / human 在 cadence-化 review 时判断

**缺点**：
- FLAG rate 可能涨上去（11.76% → ???）；real-meta-agent batch 实测
  才知道
- 不闭合 Codex Axis B 的字面 bypass——adversarial 同 phrasing 下次仍能
  PASS 进 build/eval；R-S57 视为 "降级到 signal" 而非 "structurally
  closed"

#### Fix-C — Hybrid（推荐）

**两阶段**：

1. 先实施 Fix-A 的 word-boundary 替换（structural closure of the exact
   Codex bypass shape），但默认 `synonym_map_enabled: false` 不翻。
2. 跑 17-fixture sweep + ≥10 real-meta-agent batch calibration：
   - 如果 17/17 维持 11/11+4/4+2/2 且 real-meta-agent FLAG rate <25%
     且 clean cases 无 false-positive：翻 `synonym_map_enabled: true`
     default。
   - 如果某些 clean prose（如 "when the user describes their issue"）
     在 Fix-A 后被 Q1 误判：保留 Fix-A 实施但 `synonym_map_enabled`
     保留 false，同时实施 Fix-B 的 FLAG rule 作为兜底。
3. 任一路径都不允许：
   - 硬编码 Codex 用过的字面 phrase（D2 detector self-discipline）
   - 硬编码任何 case_id / user utterance / answer / case-status label
   - 切换到 LLM-based judge（D1 detector regex-heuristic-only rule）

**优点**：
- 结构性闭合 + 兜底；calibration evidence 决定 final state
- 与 detector 三态契约 + D1/D2/D3 discipline 完全兼容
- `synonym_map_enabled` toggle 翻不翻由 evidence 决定，避免拍脑袋

**缺点**：
- diff 比 Fix-A 略大；regression test 覆盖范围增加（新增 word-boundary
  替换的正反例 + 新增 FLAG rule 的正反例）
- 17-fixture sweep + real-meta-agent batch 必须双重通过——calibration
  本身的工作量比 Fix-A / Fix-B 单独大

## 5. Recommended option + rationale

### 推荐 Alt-S2（2 sub-sprints）+ Fix-C（hybrid）

**理由**：

1. **Calibration-first 切片符合 compounding effect 约束**：detector
   calibration → overnight batch 是强顺序依赖（§9.2）；切两个
   sub-sprint 让 calibration evidence 在 S-Auto-5 close 时固化，
   S-Auto-6 的 overnight batch 跑在已 calibrated 的 detector 上，risk
   surface 收敛。

2. **与 §8.5 sub-sprint count ceiling 留余量**：Alt-S2 2 个 sub-sprint
   在 5 个 ceiling 下留 3 个余量；若 S-Auto-5 或 S-Auto-6 出现
   fix-iteration（如 detector calibration 暴露设计漏洞需要重做），可以
   起 S-Auto-5.1 / S-Auto-6.1 而不突破 ceiling。Alt-S3 没有这个余量。

3. **Hybrid Fix-C 让 evidence 决定 final state**：R-S57 fix 的关键不是
   "选哪条 path"，而是 "把 real meta-agent batch 跑出来再决定"。Fix-C
   先实施 structural change（word-boundary）但保留 toggle false，跑
   calibration 后决定是否翻 toggle；与 §1.7 "evidence-driven not
   intent-driven" 原则一致。

4. **First cherry-pick to main 的 human-decision-framework 在 S-Auto-6
   走通**：S-Auto-6 把首次 cherry-pick 框架化（程序化通过 + §5.6
   manual review + deliver-agent + human 双签名 + applier Hybrid
   apply），成为 future cycles standard procedure。

5. **与 LLM-first constitution 对齐**：本 milestone 不引入任何 Java /
   prompt / config 硬编码；detector calibration 的所有改动是 generic
   structural regex（word-boundary）而非 specific phrase，符合 D2
   detector self-discipline。fix path 选择由 real meta-agent batch
   evidence 驱动，而非 Codex 一次 adversarial 的孤证。

### M-Auto-1B 形态详细

```
M-Auto-1B = Calibration Milestone
├── S-Auto-5 — Live-iter bootstrap + Detector calibration (Fix-C hybrid)
│     • mvn package → server/target/*.jar
│     • verify .env.local AUTOLOOP_META_LLM_API_KEY
│     • python -m autoloop run --experiments 1 (real iter; keep/discard 不
│       gate close)
│     • capture ≥10 real meta-agent propose samples (1 from this iter; 9+
│       via additional limited iter or via --dry-run with synthetic seed
│       inputs that exercise proposer LLM)
│     • implement Fix-C step 1 (word-boundary substitution in _normalize)
│     • re-run 17-fixture calibration sweep (asserts 11/11+4/4+2/2)
│     • re-run detector self-discipline 3 regression tests
│     • run real-meta-agent batch calibration (FLAG rate <25%; FP=0 on
│       clean prose)
│     • decision: flip synonym_map_enabled true OR retain false + add
│       Fix-B FLAG rule (Fix-C step 2)
│     • per-iteration elapsed time recorded
│     • Codex deferred to milestone-shared UNLESS detector change crosses
│       §1.7 surface (§4.3 trigger #2) — Fix-C structural regex change
│       likely qualifies as per-sub-sprint Codex trigger; deliver-agent
│       judges at sub-sprint planning
│
└── S-Auto-6 — First overnight batch + First human review + First
              cherry-pick to main
      • pre-batch baseline rerun (47 cases × 1 → drift envelope)
      • python -m autoloop run --experiments 15 (or 10-20; budget 6-8h)
      • next-morning: deliver-agent + human read kept candidates
        - each kept candidate gets §5.6-style manual trace review on bad_cases
          + anchor_outcome (sample turns + judge)
        - PASS / FAIL / IMPROVING decided jointly (NOT programmatic alone)
      • at least 1 human-approved kept candidate → python -m autoloop apply
        --experiment exp-<N> (Hybrid: cherry-pick + emit baseline patch +
        NO auto-commit; human commit manually)
      • observation accumulation: shadow_disagreement_rate, FLAG rate,
        gaming flag rate (provider drift signature; per-iter elapsed time)
      • Codex milestone-shared at M-Auto-1B close (no per-sub-sprint
        trigger unless cherry-pick involves §1.7 borderline)
```

### Per-iteration elapsed-time budget (S-Auto-5 baseline)

预估单 iter：
- Spring restart (~30s) + mvn warm cache (~5s) → 35s
- 3 suite eval：bad_cases parallel=1 × 12 cases × ~30s = 6 min；
  anchor_outcome parallel=4 × 12 cases × ~30s = 90s；shadow parallel=4
  × 23 cases × ~30s = 175s → 总 ~10.5 min
- Meta-agent LLM 调用：analyze (~10s) + propose (~15s) + 重试 (~5s
  worst case) → ~30s
- applier git ops + tier_evaluator + memory write → ~30s
- **总 ~12-15 min**

Overnight 6-8h budget → 24-40 iters。保守预算 10-20。

## 6. Scope split + delivery priority suggestion

**Deliver agent 最终决定，研究 agent 仅建议**。下列是参考形态：

### Milestone M-Auto-1B — Auto-Evolution Calibration

| Sub-sprint | 名称 | Layer (§3.2) | Scope 三句话 | Codex |
|---|---|---|---|---|
| S-Auto-5 | Live-iter bootstrap + Detector calibration (Fix-C hybrid) | `infra` (主) + `eval_spec` (calibration) | 补齐 live-iter prereqs (mvn package + .env.local verify) + 跑 1-3 real iter 产出 ≥10 meta-agent propose sample + 实施 Fix-C step 1 (word-boundary regex) + 跑 17-fixture sweep + real-meta-agent batch 决定 `synonym_map_enabled` toggle + Fix-C step 2 (兜底 FLAG rule if needed)。零 server / eval / case_spec 触动。 | **per-sub-sprint（§4.3 trigger #2 — detector 改动直接影响 §1.7 structural guard；Codex 必须在 S-Auto-6 overnight batch 之前独立验证 detector calibration evidence + Fix-C 选择的 structural soundness）** |
| S-Auto-6 | First overnight batch + First human review + First cherry-pick to main | `eval_spec` | pre-batch baseline rerun (envelope) + overnight 10-20 iters + 第二天 deliver-agent + human §5.6 manual review of kept candidates + 至少 1 个 human-approved cherry-pick + observation 累积 (shadow_disagreement_rate / FLAG rate / gaming flag rate / per-iter elapsed). 零 detector / sandbox / scoring code 触动 (S-Auto-5 已 freeze). | milestone-shared |

### 与 §5.6 bad-case suite + shadow 的交接

- **S-Auto-5 close 不跑 §5.6 bad-case manual review**：S-Auto-5 不改 bot
  代码，只改 detector + 跑 limited live iter，bad-case 在
  bad_cases/anchor_outcome 上的 47-case 程序化跑是 per-iteration fitness
  的一部分，不单独跑 §5.6 close gate。
- **S-Auto-6 close 必须跑 §5.6 bad-case manual review**：跨 milestone
  close-day cadence；human + deliver-agent 复核 kept candidates 各
  trace + 总体 12 bad case 当晚状态。
- **shadow regression-safety gate**：M-Auto-1B close 时按 M5 / M-Auto-1A
  范式跑一次 shadow rerun + manual review pass。

### 与其它 milestone candidates 的关系

- **M3-B Single Handover Orchestrator P0** (release_gate.md §1.1
  blocker)：仍 deferred per human's M-Auto-1-first decision；M-Auto-1B
  close 后重新评估优先级。
- **Projection-hygiene milestone candidate** (M5 carry-over OQ-S52.4 +
  #4 C3 dedup)：独立 track；M-Auto-1B 不冲突。
- **`R-eval-java-module-retirement`** (governance-hygiene；M-Auto-2+
  target)：M-Auto-1B 不消费；保留为 future fold-back work。
- **UC-G/H/I/J bad-case seeding** (`R-bad-case-suite-uc-ghij-seed-from-real-sessions`)：
  独立 bad-case discovery 工作；新加 case 会自动进入 auto-loop fitness
  (per-iteration bad_cases programmatic case_passed count)，不阻塞
  M-Auto-1B。

### Stage-2 entry decision

M-Auto-1B close 之后，deliver-agent + human 复核是否进入 Stage-2（解锁
`server/src/main/resources/scripts/templates.yaml` 或
`prompts/system_prompt.txt`）。Stage-2 entry 是单独 milestone 决策（不
属 M-Auto-1B scope），评估准则是 M-Auto-1B 跑出 ≥3 个 kept
human-approved cherry-pick + 0 个 borderline §5.3 case。

## 7. Layer classification + §7 stanza pre-fill

M-Auto-1B 整体是 **multi-layer prospective**（infra + eval_spec），不同
sub-sprint 主层不同。

| Sub-sprint | Primary layer (§3.2) | Tier-0 invariant? | Semantic hardcode? |
|---|---|---|---|
| S-Auto-5 | `infra` (live-iter bootstrap) + `eval_spec` (detector calibration) | None | None — Fix-C word-boundary regex 是 generic structural pattern (D2 compliant) + FLAG-only fallback (不 auto-FAIL) |
| S-Auto-6 | `eval_spec` (first real fitness verdict 序列消费) | None | Possibly Y — first cherry-pick to main 候选如果在 human review 中被判 borderline §5.3 case，需触发 §7 stanza 的 sunset plan + downgrade-to-signal R-item |

### §7 stanza 预填（milestone-level；deliver-agent 改写）

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** infra (S-Auto-5 live-iter bootstrap) +
eval_spec (S-Auto-5 detector calibration + S-Auto-6 first fitness
verdict consumption + first cherry-pick)

**Tier-0 invariant:** M-Auto-1B 不新增 Tier-0 invariant。
`docs/runtime_freeze_and_risk_policy.md` §1 / §2 不变。R-S57 fix 是
detector calibration (eval_spec layer; Constitution §1.4 "trace and
eval contract" responsibility) 不是 invariant。M2-close C2 / C3
candidates 继续 DEFERRED 不变。如果 first cherry-pick candidate 在
human review 中触发新 Tier-0 候选讨论，按 §3.2 Q2 tail rule 走
human_review_required，不在 M-Auto-1B 内部 elevate。

**Semantic hardcode:** M-Auto-1B 不引入 semantic hardcode。三道结构性
防御保留 M-Auto-1A 不变：
(1) sandbox `yaml_diff_validator.py` 只接受 6 Skill YAML × 4 LLM-soft
field class 的修改；任何 trace_check/mandatory_for/severity/id/
guardrails/state_inheritance/tools_required/applicable_use_cases 改动
被结构性拒绝。
(2) `anti_hardcode_check.py` 在每次 propose 后跑 §4.1 nine-question
kernel Q1/Q2/Q4/Q5 自检；FAIL → propose-stage discard；FLAG_FOR_CODEX
→ continue but flag。S-Auto-5 Fix-C 实施 word-boundary 替换 +
optional FLAG rule 增强（最多 +2 rule，仍在 ≤30 rule cap 内）；新增
rule 必须通过 D2 self-discipline regression test (grep-asserts no
literal case_id / utterance / answer)。
(3) S-Auto-6 first cherry-pick 必须经过 deliver-agent + human §5.6
manual review (NOT programmatic alone) + AskUserQuestion 显式
confirmation。

Sunset plan: 若 S-Auto-6 first cherry-pick 候选在 human review 中被判
borderline §5.3 case (即程序化 keep 但 manual review 觉得 "drift to
keyword bot"-style edit)，触发该候选的 downgrade-to-signal follow-up
R-item，cherry-pick 暂缓；S-Auto-6 close 改为 fix-iteration scope。

**Generalization coverage:**
- target = (i) live-iter pipeline 单 iter 实跑通 (regardless keep /
  discard / error) + (ii) R-S57 calibration: 17-fixture 维持
  11/11+4/4+2/2 + real-meta-agent batch FLAG rate <25% + FP=0 on
  clean prose;
- neighbor = R-S57 adversarial 变体: "If you encounter X => respond Y"
  / multi-line WHEN/THEN ("When the case is open\nand the user
  describes a refund\nthen escalate") / 大小写变体 / unicode
  full-width 变体 — 均需在 S-Auto-5 calibration 中实测;
- negative = 17-fixture 4 个 clean PASS 案例 + ≥3 个 real meta-agent
  clean prose sample (i.e. "When the user describes their issue,
  gather intake fields before proposing next steps" 类型) — 不能因
  Fix-C 误触发;
- shadow = S-Auto-6 overnight batch 中 23 shadow case 跨 iter
  `shadow_regression_detected: no` 累计；若任何 keep 在 shadow gate 失
  败被 discard，记为 anti-overfitting 守住的成功观察。
```

## 8. Hard fences + non-goals

### Hard fences (milestone-level；无例外)

承袭 M-Auto-1A `docs/milestones/M-Auto-1A_objective.md` §6 全 12 条，
**额外**或**变动**为：

1. **Live-iter 允许且必须执行**：M-Auto-1A 的 fence #7 "No cherry-pick
   to main in M-Auto-1A" 不再适用 M-Auto-1B；M-Auto-1B **允许且必须**
   首次 cherry-pick 一个 human-approved kept candidate 到 main。
2. **First cherry-pick 必须经过 deliver-agent + human 双签名**：S-Auto-6
   close 时 deliver-agent 在 `docs/milestone_objective.md` (M-Auto-1B
   live) §12 closure verdict 写明 cherry-pick 候选 + 选择理由；human
   在 AskUserQuestion 显式确认；applier Hybrid 模式 + human 手动
   commit。
3. **没有第二次 cherry-pick within M-Auto-1B**：M-Auto-1B 只允许 1 次
   cherry-pick to main；任何额外的 kept candidate 即使程序化通过也
   留在 `autoloop/keep-{N}` 分支等 M-Auto-2+ 处理。
4. **不修改 `server/src/main/java/**`**（仍锁定）。
5. **不修改 `eval/` / `eval_interactive/eval_interactive/**`**（仍锁定）。
6. **不修改 case_specs / case_specs_shadow** (任何)。
7. **不修改 `docs/runtime_freeze_and_risk_policy.md` /
   `docs/foundational/**` / `docs/current/iteration_governance.md`**。
8. **不修改 `server/src/main/resources/{prompts,scripts,config}/**`**——
   Stage 2 决策点在 M-Auto-1B close 之后；M-Auto-1B 仍是 Stage 1
   (Alternative A 仅 Skill YAML LLM-soft fields)；Stage 2 (解锁
   templates.yaml 等) 不在 M-Auto-1B scope。
9. **不修改 `autoloop/scoring/` 4 个 baseline files** (`tier_evaluator.py`
   `eval_runner.py` `baseline_loader.py` `gaming.py`) —— 任何改动会触发
   `gaming.scoring_code_drift.sha_changed` ERROR (with baseline SHA
   `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`)；
   detector calibration 改动只允许在 `autoloop/sandbox/anti_hardcode_check.py`
   + `autoloop/config.yaml` 的 `anti_hardcode` block + 相关 tests。
10. **meta-agent 仍不允许跨多文件 diff**：sandbox 强制 single Skill
    YAML + single field class per iteration。
11. **shadow set 失败信息永不喂回 meta-agent**：tier_evaluator firewall
    保持。M-Auto-1B 不放松此 fence。
12. **detector calibration 改动不可硬编码任何 Codex Axis B 字面 phrase
    / 任何 case_id / 任何 user utterance / 任何 answer text / 任何
    case-status label**：detector self-discipline 3 个 regression test
    继续 PASS。
13. **第一夜 batch 跑出 kept candidates 时，sandbox + anti_hardcode +
    gaming 全部必须 verdict 一致才允许进 human review**：任何
    anti_hardcode FAIL or content_validator FAIL 必须已被 propose-stage
    discard；任何 gaming check ERROR-severity 触发 (即使 v1
    observation-only) 必须在 human review 时显示。

### Non-goals (intentionally deferred)

- **不解锁** `server/src/main/resources/scripts/templates.yaml`（Stage 2
  决策点在 M-Auto-1B close 后）
- **不解锁** `server/src/main/resources/prompts/system_prompt.txt` /
  `routing_prompt.txt`（同上）
- **不引入新评测维度**（v1 narrow dataset 47 cases / iter 不变）
- **不引入 LLM-based detector judge**（D1 detector regex-heuristic-only
  保持）
- **不在 M-Auto-1B 内 promote gaming check 从 observation-only 到
  gating**（observation-only_in_v1: true 保持；M-Auto-2 视 ERROR-severity
  累计后再决定）
- **不替代 §5.6 bad-case 人工评判**（first cherry-pick 必须经过 §5.6
  manual review；不是程序化通过即可 cherry-pick）
- **不引入 Stage-2 prereqs**（如 CSAT 模拟器；这是 M-Auto-2+ work）
- **不修改 §5.5 smoke composite_score demotion 状态**（smoke 仍
  observation-only；不作 close gate）
- **不消费 `R-eval-java-module-retirement`**（M-Auto-2+ governance-hygiene
  target）
- **不替代 deliver-agent 的 milestone 决策**——milestone 拆分由
  deliver-agent + human 最终决定

## 9. Risks + compounding effects

### 9.1 风险表

| 风险 | 严重度 | 缓解 |
|---|---|---|
| Fix-C 的 word-boundary 替换让 clean prose "when the user describes" 在 Q1 IF/THEN 误触发 | Critical | S-Auto-5 calibration sweep 17 fixtures + ≥10 real-meta-agent clean prose sample；FP=0 强制；若 FP>0 退化为保留 `synonym_map_enabled: false` + 实施 Fix-B FLAG rule 作为兜底 |
| Real meta-agent (DeepSeek / Kimi / AICodeWith) 输出的 propose 在结构 / 长度 / abstract-vs-concrete 风格上与 synthetic fixtures 完全不同，detector calibration evidence 失效 | High | S-Auto-5 必须实跑 ≥10 个 real meta-agent propose；calibration table 必须实跑这 10 个之后 FLAG rate 仍 <25% 才 close；若 ≥25% 触发 detector noisy 告警，narrow rule 或 down-regulate；考虑增加 fixture |
| Meta-agent LLM (AICodeWith / Anthropic-compatible) API 抖动 / rate-limit / 配额超限导致 overnight batch 中断 | High | S-Auto-5 first iter 显式测量 LLM 调用 latency + error rate；overnight batch 配置 retry on transient error；若 API 突发 ban，loop crash-recovery 保证已 commit 的 iter 不丢，但跳过的 iter 当晚回不来 |
| Per-iteration elapsed time > 40 min 让 overnight batch budget 严重缩水 (6-8h / 40min ≈ 9-12 iters; 比预期 15-20 少) | High | S-Auto-5 first iter 实测 elapsed；若 >30 min 立即考虑 (a) Spring 重启优化 (b) 缩小 parallel; 若 ≥40 min triggered §10 stop condition 3 类 reinvestigation R-item，可能 split S-Auto-6 进一步 |
| LLM-provider non-determinism 让 baseline rerun envelope 不稳定 (重复跑 baseline 也漂移)，导致 overnight 的 "regression" 与 "drift" 不可分 | High | S-Auto-6 pre-batch baseline rerun ≥2 次而非 1 次，建立 drift envelope median + IQR；overnight kept candidate 必须 outside median ± 2×IQR 才算 real change；若 baseline 双跑 drift 已经 >5/34 类水平，记为 systematic drift signal 给 human review (`docs/10-handoff.md` provider drift observation) |
| First cherry-pick 候选在 human review 中被判 borderline §5.3 case ("drift to keyword bot"-style edit even though程序化通过) | High | S-Auto-6 close 提供 AskUserQuestion 让 human 选 cherry-pick / 暂缓 / discard；任何 borderline 候选不 cherry-pick，open R-item 为 detector calibration 后续；M-Auto-1B 允许 0 cherry-pick close — close 仍 PASS 如果其它 gates 都通过且 human + deliver-agent 决定 "本次无 human-approved 候选" |
| `synonym_map_enabled: true` 翻 default 后某些 historical synthetic fixture (现在 PASS 的 clean case 含 "when") 变 FAIL | Medium | S-Auto-5 17-fixture sweep 强制 clean 案例仍 4/4 PASS；若 1 个 false-positive，保留 `synonym_map_enabled: false` 不翻 |
| Cherry-pick 到 main 后 next milestone (M-Auto-2) 的 baseline 必须 advance；`config.fitness.baseline_dir` 与 `scoring_code_baseline_sha` 需同步更新 | Medium | S-Auto-6 first cherry-pick 之后 deliver-agent 在 close-bundle 中跟随更新 `config.yaml` 的 `fitness.baseline_dir` + 重算 `scoring_code_baseline_sha` (注：scoring code 没改，但 baseline run 路径变了)；这是新的 close-out artefact pattern，前所未有 |
| `mvn package` 在 dev session 中失败 (依赖 issue / 编译错误 / disk space) | Medium | S-Auto-5 dev session 第一步 build；若 build fail 直接 halt + report；deliver-agent 优先 unblock build issue，绝不绕过 |
| Anti-hardcode detector calibration 改动让 +n rule 超过 ≤30 上限 | Low | rule count 当前 11；Fix-C 最多 +2 (word-boundary + optional FLAG)；离 30 上限远；regression test 当前已 enforces ≤30 |
| First overnight batch 跑出来 0 个 kept candidates (全 discard) | Low | OK — 不影响 milestone close；M-Auto-1B 的 close gate 是 "完整 pipeline 端到端无 crash + calibration evidence 固化 + ≥1 iteration verdict 序列化"，不强制要求有 kept；若 0 kept 累计成 R-item 提示需要更激进的 propose 策略 |

### 9.2 Compounding effects (顺序依赖)

```
M-Auto-1A (substrate; closed)
  └─→ M-Auto-1B (本 proposal — calibration milestone)
        ├─ S-Auto-5 — Live-iter bootstrap + Detector calibration
        │    │
        │    ├─ (i) mvn package → built jar
        │    │     └─→ (ii) verify .env.local AUTOLOOP_META_LLM_API_KEY
        │    │           └─→ (iii) python -m autoloop run --experiments 1
        │    │                 └─→ (iv) capture real meta-agent propose samples
        │    │
        │    └─ (v) Fix-C step 1 (word-boundary regex)
        │          └─→ (vi) 17-fixture calibration sweep (must 11/11+4/4+2/2)
        │                └─→ (vii) real-meta-agent batch calibration
        │                      └─→ (viii) Fix-C step 2 decision (toggle + FLAG)
        │                            └─→ (ix) per-sub-sprint Codex (§4.3 #2)
        │
        └─ S-Auto-6 — First overnight + First human review + First cherry-pick
              ├─ (i) pre-batch baseline rerun ≥2 → drift envelope
              │     └─→ (ii) overnight 10-20 iters
              │           └─→ (iii) §5.6 manual review
              │                 └─→ (iv) AskUserQuestion 决定 cherry-pick
              │                       └─→ (v) python -m autoloop apply (Hybrid)
              │                             └─→ (vi) human manual git commit
              │
              └─ Codex milestone-shared at M-Auto-1B close
```

**不正确顺序会导致**：

- **跳过 S-Auto-5 (vii) real-meta-agent batch 直接进 S-Auto-6 overnight**：
  detector calibration 仅基于 synthetic fixture → 实际 real meta-agent
  输出与 synthetic distribution gap 让 FLAG rate 不可控；overnight FLAG
  flood / human review burnout 或 false-negative slip-through。
- **跳过 S-Auto-6 (i) pre-batch baseline rerun 直接进 overnight**：
  overnight 的 kept candidate 与 close-day 5/34 provider drift 不可分；
  human review 决策无 envelope 参考，可能误判 provider drift 为
  regression（或反之）。
- **跳过 S-Auto-6 (iii) §5.6 manual review 直接 cherry-pick**：可能让
  borderline §5.3 case 进 main；后续要回滚极困难（cherry-pick 已在
  main，需要 explicit revert commit + deliver-agent + human 复核）。
- **跳过 S-Auto-5 (ix) per-sub-sprint Codex 直接进 S-Auto-6**：detector
  改动是 §4.3 trigger #2 候选 (§1.7 structural guard 改动)；如果跳过，
  milestone-shared Codex 在 M-Auto-1B close 时才发现设计漏洞，触发
  fix-iteration sub-sprint，浪费已跑完的 overnight batch。
- **Fix-C 实施但跳过 calibration evidence 直接翻 `synonym_map_enabled:
  true`**：clean prose ("when the user describes") false-positive 概率
  未知；可能让大批 clean meta-agent propose 在 propose-stage 被错误
  discard，整个 overnight batch 全 discard。

### 9.3 与现有 R-item 的耦合 / 影响

- **`R-S57-anti-hardcode-whenever-arrow-synonym-bypass`** —— S-Auto-5 直接
  消费；M-Auto-1B close 时该 R-item 标 "closed (resolved by Fix-C; details
  in `docs/milestones/M-Auto-1B_objective.md` §12)"。
- **`R-bad-case-parallel-session-establishment-flakiness`** (M5
  priority-bumped) —— 影响 S-Auto-6 overnight batch；bad_cases 仍
  parallel=1（继承 M-Auto-1A S-Auto-2 默认）。recurrence 数据点会继续
  累积。
- **`R-iwzx-uc-k-vs-uc-h-routing-spurious-distress`** (semantic_planner)
  —— overnight batch 的 meta-agent 自然会试图在
  `discover_triage.procedure` / `critical_steps[].desc` 上提改动来
  降低该 case 的 Tier-1 outcome 失败；如果 fitness 有改善，可能成为 first
  cherry-pick 候选（但 cherry-pick 决策由 human review + AskUserQuestion 决定）。
- **`R-shadow-fixture-empty-form-session-create-400`** (M5 NEW) —— shadow
  rerun 中 cs59s01/cs59s02 HTTP-400 deterministic；tier_evaluator 仍
  不计入 bot-side regression (S-Auto-2 已处理)。M-Auto-1B 不动。
- **`R-bad-case-suite-uc-ghij-seed-from-real-sessions`** —— 不在
  M-Auto-1B scope；当新 case 加入后会自动进入 auto-loop fitness，但
  M-Auto-1B 不主动 seed。
- **`R-eval-java-module-retirement`** (M-Auto-2+ governance-hygiene)
  —— M-Auto-1B 不动。

## 10. Observability / trace / report implications

M-Auto-1B 是 auto-loop 第一次跑真实 LLM iter，observability 的需求比
M-Auto-1A 大幅扩展。继承 `project_observability_debt_pattern.md` 经验：
架构上线后观察面常常滞后，必须主动配套。

### 10.1 S-Auto-5 close 增量观察面

- `autoloop/results/runs/exp-1/{hypothesis.json,diff.yaml,...}` 落盘
  (per-iteration artifact tree per `docs/proposals/autoloop_design.md
  §10` + S-Auto-3 deliverable)
- `autoloop/results/experiments.jsonl` append 1 行 (raw experiment log)
- `autoloop/results/iterations.sqlite` insert 1 行 (structured index)
- `autoloop/results/lessons.md` 不写入（lessons compaction 默认 K=10
  iterations 触发；S-Auto-5 只 1-3 iter 不到阈值）
- detector calibration 评估结果（17-fixture + real-meta-agent batch
  结果）记录为 S-Auto-5 handoff §8 单独 calibration evidence section

### 10.2 S-Auto-6 overnight + cherry-pick 增量观察面

- `autoloop/results/runs/exp-N/*` 累计 10-20 iter
- `autoloop/results/experiments.jsonl` append ~10-20 行
- `autoloop/results/iterations.sqlite` insert ~10-20 行
- `autoloop/results/lessons.md` 在 K=10 累计点触发首次 LLM-distilled
  lesson compaction (per `docs/solutions/auto_evolution_skill_driven_v1.md §3.4`)
- 推荐生成 `autoloop/results/report-m-auto-1b.html`（per
  `docs/solutions/auto_evolution_skill_driven_v1.md §10` 的 timeline 视
  化）作为 first-overnight 复盘材料

### 10.3 跨 milestone observability 文档化

参照 `project_observability_debt_pattern.md` 经验：

- **`docs/10-handoff.md` §0 新增一行**：`Auto-loop status: <idle |
  running exp-N | last run YYYY-MM-DD; kept=N discard=M error=K>` —
  cold-start 时一眼看 auto-loop 当前状态
- **`docs/action_bank.md` §6 新增 batch-level summary entry**：
  M-Auto-1B 的 overnight batch 作为一个 entry 而非 per-iteration
  entries；entry 含 kept count / discard count / error count + 是否有
  cherry-pick + cherry-picked exp id
- **`docs/milestones/M-Auto-1B_objective.md` §12 closure verdict 新格式**：
  cherry-pick 记录单独段落（cherry-picked exp id + Skill / field /
  edit summary + manual review verdict + main commit SHA after human
  manual commit）

### 10.4 复用现有观察面

- `eval_interactive/results/<run-id>/results.json` 由 auto-loop
  subprocess 触发；schema 不改 (hard fence #11)
- 现有 admin trace UI (M5 S2 之后) — 在 S-Auto-6 human review kept
  candidates 时打开 admin trace 看具体 LLM raw response per per-turn
  trace
- `docs/codex-findings.md` scaffold — S-Auto-5 per-sub-sprint Codex
  + M-Auto-1B milestone-shared Codex 走标准 sprint-close / milestone-close
  header；archived 至 `docs/milestones/M-Auto-1B_codex-review.md`

### 10.5 Stale-observability 风险

参照 `project_observability_debt_pattern.md`：架构更新后观察面常常滞后。
M-Auto-1B 上线后，以下观察面需要确认：

- 现有 `report.html` (eval_interactive M5 S1 ship 的 four-tier verdict
  surface) 是否能展示 auto-loop 触发的 per-iteration eval run；如果
  current report.html 仅 per-run-id 视化，M-Auto-1B 可能需要在 S-Auto-6
  close 时 cross-run navigation R-item 入 backlog (M-Auto-2+ scope)。
- M5 admin trace UI 已支持 "LLM raw response" 显示；M-Auto-1B human
  review 时实际打开 trace 验证是否易用；如果 trace 缺关键字段
  (e.g. meta-agent 调用 chain 不在 admin trace 内)，记录为
  observability debt R-item。

---

## Appendix A — Sub-sprint planning rough draft (供 deliver-agent 改写)

下列是 S-Auto-5 / S-Auto-6 的 sub-sprint contract 起草建议；deliver-agent
按 §9 self-containment invariant 改写为正式 `docs/sprint_objective.md` +
`compact/sprint-NNN-dev-prompt.md`。

### S-Auto-5 — Live-iter bootstrap + Detector calibration (Fix-C hybrid)

**Layer (§3.2)**: `infra` (主) + `eval_spec` (calibration)
**§7 stanza**: REQUIRED (semantic-touching via detector calibration)
**Codex review**: **per-sub-sprint (§4.3 trigger #2)** — detector
改动是 §1.7 structural guard 改动；Codex 必须在 S-Auto-6 overnight
之前独立验证 Fix-C 选择
**Scope claim**: 补齐 live-iter 3 prereqs (mvn package + .env.local
verify + working tree clean) + 跑 1-3 real iter 产出 ≥10 real
meta-agent propose sample + 实施 Fix-C step 1 (word-boundary regex in
`_normalize`) + 重跑 17-fixture calibration sweep + 实跑 real-meta-agent
batch + Fix-C step 2 decision (`synonym_map_enabled` toggle / Fix-B FLAG
rule fallback) + per-iteration elapsed time observation
**Hard fences**: §8 全 13 条；特别 detector self-discipline 3
regression test 持 PASS；rule count ≤ 30
**Acceptance bar**:
- [ ] `mvn package` 成功，`server/target/*.jar` 存在
- [ ] `python -m autoloop check` 输出 success
- [ ] `python -m autoloop run --experiments 1` (no --dry-run) 完整 14-step
  state machine 跑通 (regardless keep / discard / error)
- [ ] ≥10 real meta-agent propose samples 累计 in
  `autoloop/results/experiments.jsonl`
- [ ] Fix-C step 1 实施：`_normalize` 含 word-boundary regex
  `\b(?:whenever|when)\b` → `if` (在 synonym_map_enabled true 时启用)
- [ ] 17-fixture sweep: 11/11 forbidden FAIL + 4/4 clean PASS + 2/2
  borderline FLAG_FOR_CODEX (regression test 必须 PASS)
- [ ] Detector self-discipline 3 regression test PASS
- [ ] Real-meta-agent batch (≥10): FLAG rate <25%; FP=0 on clean prose
- [ ] `synonym_map_enabled` toggle 决策记入 handoff §X (true / false +
  evidence)；如 false，则同时实施 Fix-B FLAG rule
- [ ] Per-iteration elapsed time recorded; if >40 min, surface as
  observation R-item

### S-Auto-6 — First overnight batch + First human review + First cherry-pick to main

**Layer (§3.2)**: `eval_spec` (first real fitness verdict 序列消费 +
first cherry-pick)
**§7 stanza**: REQUIRED (semantic-touching via cherry-pick to main +
manual review of kept candidates)
**Codex review**: **milestone-shared at M-Auto-1B close (default per
§4.3)** — 除非 cherry-pick 候选触及 §1.7 borderline，则触发
per-sub-sprint Codex
**Scope claim**: pre-batch baseline rerun ≥2 (drift envelope) +
overnight 10-20 iterations + 第二天 deliver-agent + human §5.6 风格
manual review of kept candidates + AskUserQuestion 决定 first cherry-pick
+ 至少 1 个 human-approved kept candidate cherry-pick to main via
`python -m autoloop apply` Hybrid 模式 + observation 累积
**Hard fences**: §8 全 13 条 (cherry-pick 仅 1 次)
**Acceptance bar**:
- [ ] Pre-batch baseline rerun ≥2 次完成；drift envelope median + IQR
  记入 handoff
- [ ] Overnight batch 跑 ≥10 iter (target 10-20)；每 iter verdict
  serialize 到 `experiments.jsonl` + `iterations.sqlite`
- [ ] §5.6 manual review of every kept candidate; PASS / FAIL /
  IMPROVING 判定 by deliver-agent + human jointly
- [ ] At least 1 human-approved cherry-pick (OR 0 cherry-pick with
  explicit "no human-approved candidate" justification + close PASS
  on其它 gates)
- [ ] Cherry-pick 操作: `python -m autoloop apply --experiment exp-<N>`
  Hybrid 模式 + human 手动 commit + main HEAD 更新记录
- [ ] M-Auto-1B `config.fitness.baseline_dir` advance (如有 cherry-pick) +
  `scoring_code_baseline_sha` 保持 (scoring code 不改)
- [ ] §5.6 bad-case suite + shadow rerun PASS at M-Auto-1B close
  (milestone-shared close gate per M5 / M-Auto-1A precedent)
- [ ] Lessons compaction triggered (K=10 default) ≥ 1 次；
  `autoloop/results/lessons.md` 含 ≥ 1 LLM-distilled lesson
- [ ] R-S57 marked closed in `docs/action_bank.md` §5 (resolved by
  S-Auto-5 Fix-C; M-Auto-1B 提供 end-to-end calibration evidence)
- [ ] Observation: `shadow_disagreement_rate` first measurement;
  per-iter FLAG rate; per-iter elapsed time avg; gaming flag rate

---

## Appendix B — 与 `docs/solutions/auto_evolution_skill_driven_v1.md`
                的差距 (M-Auto-1B 视角)

| 维度 | proposal 2026-05-26 描述 | M-Auto-1A 之后实际状态 | M-Auto-1B 计划处置 |
|---|---|---|---|
| Sub-sprint 数 | 5 (S-Auto-1..5) 单 milestone | 拆分为 M-Auto-1A (4 sub-sprint S-Auto-1..4 build) + M-Auto-1B (calibration) | M-Auto-1B 2 sub-sprint (S-Auto-5..6) |
| First overnight | proposal §5 "S-Auto-5 — Overnight stability run" | M-Auto-1A WAIVED live-iter; overnight 推迟 | M-Auto-1B S-Auto-6 跑 |
| Anti-hardcode kernel | proposal §3.3 Layer 0 + S-Auto-4 | M-Auto-1A S-Auto-4 ship + Codex Axis B 1 bypass | M-Auto-1B S-Auto-5 calibrate via Fix-C |
| Synonym map | proposal 未明确 | M-Auto-1A S-Auto-4 ship `synonym_map_enabled: false` default | M-Auto-1B S-Auto-5 evidence-driven toggle decision |
| First cherry-pick | proposal §5 隐式 | M-Auto-1A fence #7 禁止 in M-Auto-1A | M-Auto-1B S-Auto-6 首次 |
| Stage 2 解锁 templates.yaml | proposal §4.3 staged | M-Auto-1A 不解锁 (Stage 1) | M-Auto-1B 不解锁 (仍 Stage 1)；Stage 2 决策在 M-Auto-1B close 之后单独 milestone |
| Lessons compaction | proposal §3.4 K=10 default | M-Auto-1A S-Auto-3 ship lessons_log.py 但未触发 (无 ≥10 iter) | M-Auto-1B S-Auto-6 K=10 累计触发首次 |

---

## Appendix C — 一个 S-Auto-6 cherry-pick decision 假想 walkthrough

**情境**：S-Auto-6 overnight 跑 15 iter，5 kept (exp-3, exp-7, exp-9,
exp-12, exp-14)。

**Programmatic verdict on exp-12** (假想):
- target_skill: `resolve_faq_grounded_answer`
- target_field: `critical_steps[4].desc` (reroute-on-actionable-appeal-intent)
- edit_summary: "soften 'must reroute' to 'should consider rerouting',
  explicit naming of UC-H appeal pathway"
- Tier-0: PASS (无 hard_checks fail)
- Tier-1 anchor_outcome: 9/12 → 9/12 (no regression)
- Tier-1 bad_cases programmatic: 5/12 → 6/12 (cs015 newly PASSes)
- Tier-2 mandatory: unchanged
- Layer 3 absolute case-count threshold: +1 case = PASS
- Layer 4 shadow: 18/23 → 18/23 PASS
- anti_hardcode: PASS / None
- gaming: 0 WARN / 0 ERROR
- decision: KEEP

**§5.6 manual review by deliver-agent + human**:
- 打开 cs015 trace: bot 在第 3 轮明确说 "your removal appeal seems to
  be about the underlying account suspension — let me get you to the
  appeal team" 而非 stuck on "your listing was removed because of
  policy violations"
- 对比 cs011 (similar UC-FP path; not touched in this iter): bot 仍
  unchanged behaviour — 无连带回归
- 神态 / 口吻: 自然，非机械; "I can route you to our appeals team to
  re-examine this case" 等
- 判定: **PASS** (manual review)

**AskUserQuestion 给 human**:
> exp-12 候选程序化 PASS + manual review PASS。是否 cherry-pick 到
> main？选项 (A) Cherry-pick exp-12，commit summary "Sprint Auto-6 /
> M-Auto-1B — soften UC-FP reroute desc to explicitly name appeal
> pathway" (B) 暂缓 — kept candidate 保留在 autoloop/keep-12 分支等
> M-Auto-2 复核 (C) Discard — manual review 中我看到了 [insert reason]

**Human 选 (A)** → `python -m autoloop apply --experiment exp-12`
Hybrid:
- cherry-pick exp-12 commit 到 main HEAD
- emit `config.fitness.baseline_dir` advance patch
- NOT auto-commit (Hybrid)
- human `git status` 检查 → `git add` + `git commit -m "Sprint Auto-6
  / M-Auto-1B — apply exp-12 to main"` 手动

M-Auto-1B `config.fitness.baseline_dir` advance记入 close-bundle。
First cherry-pick complete.

---

> **下一步**（deliver-agent + human 决定）：
> 1. 是否采纳本 proposal 作为 M-Auto-1B 的 planning baseline
> 2. Sub-sprint 切片选 Alt-S2 (推荐 2 sub-sprint) / Alt-S1 (1 sub-sprint)
>    / Alt-S3 (3 sub-sprint)；R-S57 fix path 选 Fix-A / Fix-B / Fix-C (推荐)
> 3. deliver-agent 把 §6 sub-sprint sequence 提到
>    `docs/milestone_objective.md` (M-Auto-1B 起草) + `docs/sprint_objective.md`
>    (S-Auto-5 起草) + `compact/sprint-NNN-dev-prompt.md` (per §9
>    self-containment invariant)
> 4. Codex review plan per §4.3：默认 milestone-shared；S-Auto-5 per-sub-sprint
>    trigger fired (detector 改动 §1.7 structural guard)
> 5. 本 proposal 不抢占 M3-B Single Handover Orchestrator P0 或其它
>    milestone candidate 优先级；M-Auto-1B close 之后 deliver-agent +
>    human 再决定下一个 milestone candidate
