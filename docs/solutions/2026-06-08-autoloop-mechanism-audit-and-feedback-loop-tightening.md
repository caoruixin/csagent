---
title: AutoLoop 机制审计与反馈闭环收紧 (Path-1 forward-looking research)
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file
authored_by: research-agent
authored_date: 2026-06-08
last_updated: 2026-06-09
mode: forward-looking
notes: >
  Audit of the M-Auto-1A v1 auto-evolution loop under live use in the M-Auto-7
  S-Y2 pilot. Question: are exp-63..exp-68 going "off-direction" because of a
  big design flaw, fixable improvement points, or no significant issue?
  2026-06-09 update — incorporates the validation-tranche run results
  (exp-66 / exp-67 / exp-68; 3 candidates / 0 keeps / all 3 targeted
  `$.escalation_policy` / 0 touched CS4 entity-context gap; ~90 min/iter; infra
  PASS). The new data CONFIRMs the earlier P0-A / P0-B diagnosis and SURFACES
  a third P0 (P0-C — proposer has zero awareness of the active milestone's
  PRIMARY TARGETS, so it hunts off-gap). Verdict remains **Option 2 — no
  go/no-go-blocking flaw, but three feedback-loop defects that together explain
  the 0-keep rate and the off-gap targeting**. Recommended patch sub-sprint
  (S-Y1.5) scope grew from 2-fix to 3-fix.
---

# AutoLoop 机制审计与反馈闭环收紧

## 0. 2026-06-09 Update — Run-1 validation tranche sharpened the finding

**What changed since the 2026-06-08 audit**: A 3-iteration validation tranche
(exp-66 / exp-67 / exp-68; ~4.6 h elapsed, ~90 min/iter; infra PASS —
`--auto-reboot` killed pid 43682; 3 distinct alt-port spawns; no
dirty-index contamination) ran on the post-S-Y1 honest baseline. **All
3 candidates: discard. All 3 targeted `$.escalation_policy`. Zero touched
the CS4 entity-context gap.** Two of three regressed Tier-0 safety on the
same case (`cs11s01_uc_d_two_emails_one_account`).

| Exp | Target | Edit shape | Discard | Rationale-citation |
|---|---|---|---|---|
| exp-66 | resolve_faq `$.escalation_policy` | additive +542 char | Tier-2 critical_flow 3→4 | "Per lessons L-004 and L-005…" |
| exp-67 | confirm `$.escalation_policy` | additive +328 char | Tier-0 escalation_compliance fail on cs11s01 | "lessons L-2026-05-31-004 and L-2026-05-31-005 identify the escalation decision boundary in CONFIRM as fragile" |
| exp-68 | discover_triage `$.escalation_policy` | additive +484 char | Tier-0 escalation_compliance fail on cs11s01 | "Per the analyzer summary and lessons L-004/L-005, tier0 escalation_compliance fails on a stable cluster of high-distress, repeated-failure account-recovery scenarios" |

**Two new findings the original audit didn't crystallize**:

1. **P0-C: Proposer has zero awareness of the active milestone's PRIMARY
   TARGETS.** The S-Y2 Tier-1 PRIMARY targets are `cs_uc_a_no_ad_id_ad_specific`
   + `cs_uc_a_loaded_listing` (per `sprint_objective.md` §Tier classification).
   They are in the baseline's `bad_cases/aggregated.json` as stable-FAIL.
   The proposer's prompt has **no concept of "these specific cases are
   the pilot success metric"** — it sees them mixed in with cs001 / cs011 /
   cs014 / cs11s01 / cs11s02 (the May-31 escalation_compliance cluster),
   and lessons L-004/L-005 (also from May 31, before the CS4 baseline
   existed) point at the latter. Outcome: 0/3 candidates targeted the
   gap, 3/3 hunted the old cluster.
2. **Phase-correctness diagnostic** (a stronger version of "off-gap"): CS4
   cases enter RESOLVE-FAQ (after DISCOVER classifies UC-A). They never
   reach CONFIRM / ESCALATE on the success path. Yet exp-67 edited
   `confirm.yaml` and exp-68 edited `discover_triage.yaml` —  edits that
   structurally cannot help the Tier-1 primary targets. `resolve_faq_grounded_answer.yaml`
   covers UC-A (verified `applicable_use_cases: [UC-A, UC-B, UC-C, UC-D,
   UC-E, UC-F, UC-FP]`) and is the SOLE plausible target for CS4
   entity-context guidance — yet only exp-66 touched it, and on the wrong
   field (`$.escalation_policy` instead of `$.procedure` /
   `$.grounding_instruction` where entity-context guidance would land).

**Implications for the original Option B**:

- The original 2-fix scope (**P0-A** tier_breakdown passthrough + **P0-B**
  candidate eval landscape) would have helped proposer "see" cs11s01
  regressing across iterations, but would NOT have steered it toward CS4.
- The right scope is **P0-A + P0-B + new P0-C** (pilot-target steering
  via `autoloop/config.yaml:pilot` block + propose.txt extension). All
  three are in `autoloop/autoloop/meta_agent/` and `config.yaml`, still
  ~150-250 LOC, still no gate/sandbox/scoring touched, still no re-bless
  needed.
- Lessons-disable (P1) becomes MORE urgent, not less. exp-67 / exp-68
  literally typed "Per L-2026-05-31-004 and L-2026-05-31-005" before
  adding clauses that REGRESS the very case they cite (cs11s01 was a
  case where the bot was already escalating correctly; lessons-driven
  "make escalation more aggressive" pushed it into over-escalation).
  This is a **positive feedback loop** — lessons drive a wrong direction,
  candidates fail, lessons-compactor will summarize the next 10
  iterations into an even stronger version of the same wrong direction.

**Bottom line for S-Y2**: Scaling `-n` as-is is dominantly wasted budget
(~90 min/iter × hours = single-digit candidates, all off-gap).
**Recommendation**: land S-Y1.5 (now 5-fix after 2nd-pass refinement)
BEFORE the next tranche, OR accept §3.4 hand-authored fallback as the
pilot outcome. See updated §6 / §7 / §8.

**2026-06-09 second-pass refinements (human reviewer feedback)**:

1. **4-layer hit-rate decomposition** (§12 item 5 + §11.3 early-stop):
   single `primary_target_hit_rate` 被 run-1 反例证明会假阳性
   （exp-66 命中 resolve_faq 但 field 错了）。拆成 `phase_usecase /
   skill / field_family / full_on_gap` 四个独立 metric + venn-style
   partial-hit breakdown；`full_on_gap_hit_rate` 才是 primary success
   metric。**Round-2 calibrated**：三段 verdict 而非单一阈值——
   ≥ 50% = strong pass / 25–50% = partial pass（扩 -n 16）/ < 25% =
   fail（硬早停）。Run-1 baseline 0% 落在 fail 区间，所以 S-Y1.5
   有效的最低标志是"跳进 ≥ 25% partial pass"，不是直接 50%。
2. **Per-experiment pilot snapshot embedding** (§8 scope item 5 + §10
   fences): 每条 `experiments.jsonl` row + `runs/<id>/hypothesis.json`
   嵌入当时生效的 `pilot_snapshot` block + 稳定 `block_sha256` +
   `lessons_enabled` flag。Audit exp-71/72 时不靠推断重建目标卡。
3. **S-Y1.5 close 人工 rationale checklist** (§8 test/eval req): 跑
   `--dry-run -n 2`，人工 review 4 项：(a) rationale 不 cite
   L-004/005；(b) `target_role` 标了 primary for CS4 cases；(c) target
   skill rationale 解释 UC-A/RESOLVE 相关性；(d) `after_value` 无
   case_id literal。任一 FAIL → S-Y1.5 不 close，回头调 prompt。防"prompt
   注入了但模型没听进去"。
4. **Labels-only directive in propose.txt** (§6 P0-C bullet 4th
   sub-item + §9 stanza + §10 fences): 加直白一句 "primary target case
   IDs are evaluation bookkeeping labels. Use them only to prioritize…
   Do NOT mention, encode, paraphrase, or create rules around these IDs
   or their literal fixture wording in any proposed `after_value`."
   structural defense in depth with anti-hardcode Q4 detector。

---

## 1. Executive summary

**审计结论：Option 2（无 go/no-go-blocking 大漏洞，但有三处反馈闭环缺陷
共同解释了 0-keep + off-gap targeting 现象）。**

- **没有 Option-1 级大漏洞。** Mutable-surface sandbox、5-layer
  lexicographic gate、shadow firewall、anti-hardcode detector、applier
  分支隔离都在按设计工作；exp-63..exp-68 全部正确 discard（不是误判）。
  Run-1 tranche 的 infra（preflight / auto-reboot / per-exp backend /
  baseline-loader / keep-discard gate / experiments.jsonl append）全部
  PASS——**机制本身没有故障**。
- **不是 Option 3（"问题不大就用"）。** 70 logged iterations / 0 keeps
  / 54 discards / 16 errors，更关键的是 **run-1 tranche 3/3 candidates
  全部砸到 `$.escalation_policy` 且 0/3 触碰 S-Y2 PRIMARY TARGETS**。
  这是结构性的，不是采样噪声。
- **是 Option 2。** 三处 P0 + 一处 P1 在 `autoloop/autoloop/meta_agent/`
  的 prompt builder + `autoloop/config.yaml` 的新 `pilot` block，**修复
  半径 ~150-250 LOC Python + ~20 行 config / prompt，不动 gate 逻辑、
  不动 mutable surface、不动 sandbox、不动 scoring code**（不触发
  `scoring_code_drift`），所以**不需要重 bless 当前
  `m-auto-7-prepilot-baseline-20260608`**。建议在 S-Y2 Part C 重 `-n` 跑
  之前先 land。

| 缺陷 | 优先级 | 位置 | 影响 |
|---|---|---|---|
| **反馈信号过薄**：`recent_iterations` 给 meta-agent 时 strip 掉 `verdict.tier_breakdown`，proposer 只看见一行 discard_reason 字符串 | **P0-A** | `proposer.py:188-202` + `analyzer.py:167-181` | proposer 看不到「上一次提议在哪些具体 case 上 regress」，无法学到 cs11s01 是 anti-pattern；exp-67 → exp-68 同样 case 同样规律 fail，proposer 仍然没意识到 |
| **失败地形锚在 baseline，不锚在 candidate**：analyzer 只读 `baseline_dir`，不读 candidate iteration 的 results.json | **P0-B** | `analyzer.py:58-86` + `loop.py:158` | 失败地形永远是静态 baseline；proposer 永远在解 baseline 的题，candidate 引入的新 regression（exp-67/68 把 cs11s01 弄 fail）从未进 taxonomy |
| **Proposer 不知道 active milestone 的 PRIMARY TARGETS** | **P0-C（NEW，2026-06-09 run-1 surfaced）** | `propose.txt` + `analyze.txt` + `loop.py` 全链路无 pilot-target 注入 | run-1 直接证据：3/3 candidates 砸到 `$.escalation_policy` + 0/3 触碰 CS4 entity-context gap；2/3 编辑 phase-incorrect skill（confirm / escalate cover ESCALATE phase，CS4 走 RESOLVE-FAQ） |
| **Lessons 形成 positive feedback loop** | **P1（升级）** | `lessons.md` L-001..L-005 (May 31) + `compact.txt` | run-1 三个 rationale 显式 "Per L-2026-05-31-004 and L-2026-05-31-005"；这些 lesson 都是从 0-keep 窗口压出来的，proposer 服从后引入新的 regression（cs11s01 over-escalation），下一个 lessons-compaction 窗口会把这 strengthen |
| anti-repeat heuristic 太粗 | P2 | `propose.txt:60-68` | 只看 `(target_skill, target_field)` 完全相同；run-1 在 3 个不同 skill 上做同一 field 没触发 |
| 0-keep + clean-checkout 没有 hill-climbing 内存 | P2 | program.md §7 (有意设计) | 当前 v1 design 选择；改要 program.md v2 |
| `suspect_baseline_manipulation.git_lookup_failed` 47/70 噪声 | P2 | `gaming.py:_check_suspect_baseline_manipulation` | observation-only，不影响 gate |
| `tier2_measurement_contract_change_attempt` ERROR 触发频繁 | P2（信号需要 reframe） | `gaming.py:573-602` | 不是 gaming，是 "candidate 改 prose → trace path 变化 → critical_step 不再 reach" 的自然信号；rename 即可 |

**推荐路径（详见 §6 / §7 / §8）**：先 land 一个 **S-Y1.5 micro
sub-sprint（~2-3 d）** 修 P0-A + P0-B + P0-C + P1，然后再启动 S-Y2 Part C
的 `-n` 跑。如果人类不希望额外插 sprint，**预期 §3.4 fallback 几乎肯定
被触发**（run-1 的 0/3 on-gap 已经是非常强的负面证据），让 pilot 以
"valid alternate outcome" 收尾，把 P0 patch 推到 post-M-Auto-7 的
autoloop-readiness 续作。

## 2. Investigation scope + method

**Scope**: 审计 `autoloop/` 模块的 propose-evaluate-discard 闭环 +
所读 baseline data + 与 eval_interactive harness 的耦合。**不**审计
eval_interactive scoring kernel（M-Auto-5 已 close-out），**不**审计
runtime（M-Auto-6 已 close-out）。

**Method (code-grounded; 每个 claim 都引用 HEAD-verified 路径)**:

1. 读 `autoloop/program.md` v1（locked contract）+ `autoloop/config.yaml`（活配置）。
2. 读 `loop.py` 的 14 步 state-machine + `analyzer.py` / `proposer.py` / `lessons_compactor.py` + 三个 prompts（`analyze.txt` / `propose.txt` / `compact.txt`）。
3. 读 5-layer `tier_evaluator.py`（gate 逻辑）+ `baseline_loader.py`（aggregated baseline 读法）+ `gaming.py`（七项 observation-only 检测）+ `sandbox/anti_hardcode_check.py` + `sandbox/yaml_diff_validator.py` + `sandbox/content_validator.py` + `sandbox/applier.py`。
4. 抽取 `autoloop/results/experiments.jsonl`（68 条 row）+ exp-63..exp-67 完整 `hypothesis.json` / `eval-results.json` / `discard_reason` / `gaming_flags`。
5. 读 `autoloop/results/lessons.md`（L-001..L-005）。
6. 对照 `docs/milestone_objective.md`（M-Auto-7）+ `docs/sprint_objective.md`（S-Y2）+ `docs/current/iteration_governance.md` §1 / §3.2 / §5.6 / §5.8 / §7。

**所有 line citations 均在 `autoloop/exp-67` HEAD（`8b529ce`）验证。**

## 3. Current-state survey

### 3.1 架构（程序员视角）

入口在 `autoloop/autoloop/__main__.py` → `cli.py`：

- `autoloop preflight` —— 环境/baseline/sandbox/applier 自检
- `autoloop run -n N` —— 跑 N 轮 `run_one_iteration`
- `autoloop apply --experiment <id>` —— 把 keep 候选 cherry-pick 到 main（需要人工 review）
- `autoloop report` / `autoloop audit` —— 人类可读输出

`run_one_iteration`（`autoloop/loop.py:129-380`）是 14 步固定顺序：

```
analyzer → proposer → content_validator → sandbox(YAML-diff) → anti_hardcode
  → (dry-run gate) → applier(branch+commit+spring boot) → eval_runner(3 suites)
  → trace persist → infra_error detect → baseline_loader → tier_evaluator(5L)
  → gaming.detect → memory writes → lessons_compactor (every K=10) → git tag → cleanup
```

每一步都有 fall-back：proposer 三次重试后抛 `ProposerInvalidOutputError`、
sandbox/anti-hardcode FAIL → `discard`、suite timeout → `infra_error`、
eval 普遍 degrade → `infra_error`、orchestrator catch-all 包 try/except。
**这层架构没有问题。**

Mutable surface（`program.md` §2 + `config.yaml:12-30`）：

- 6 个 Skill YAML：`discover_triage` / `resolve_faq_grounded_answer` /
  `resolve_intake_collect_and_handover` / `confirm` / `escalate` / `terminal`
- 4 个 field path：`$.procedure` / `$.grounding_instruction` /
  `$.escalation_policy` / `$.critical_steps[*].desc`

5-layer lexicographic fitness（`tier_evaluator.py:122-228`）:

| Layer | 名 | Discard 触发 |
|---|---|---|
| 0 | tier0_safety | candidate 引入 NEW Tier-0 family violation（delta vs baseline，pre-existing baseline failure 被忽略，OQ-S65.6 fix） |
| 1 | tier1_outcome | `bad_cases` 比 baseline pass 少；`anchor_outcome` 比 baseline pass 少超过 `anchor_outcome_max_drop_cases=0` |
| 2 | tier2_critical_flow | mandatory critical_step FAIL 数总和 OR 任一 UC 桶上升 |
| 3 | improvement_threshold | `bad_cases_delta` / `anchor_outcome_delta` / `tier2_reduction` 没有 ≥ 1 case 改进 |
| 4 | shadow_regression | aggregate drop > 3.0%（per-case shadow detail 永不到达 meta-agent） |

Layer 1/2 是 **STRICT 无 regression**——这意味着候选必须既不丢任何
case，又要在 Layer 3 给出至少 +1 改进，**5-layer 任何一层不过都
short-circuit discard**。这是 §1.7 "不许在 visible eval 上优化而牺牲
shadow" 的结构性防御，**没有问题，是有意如此**。

### 3.2 当前 baseline（S-Y2 pilot 用的）

`config.yaml:169` 已指向 `eval_interactive/results/m-auto-7-prepilot-baseline-20260608/`
（pilot-setup-only flip，不是 canonical 升级；`current_eval_baseline.md`
保留指向 `m-auto-6-baseline-shared-20260607`）。该 baseline 是 n=9 aggregated（`bad_cases/aggregated.json`、`anchor_outcome/aggregated.json`、`shadow/aggregated.json`），`git_commit=92c4076`，primary_model
`deepseek-v4-flash`。

Per `sprint_objective.md`：

- `cs_uc_a_generic_policy_question`（anti-误杀 control）= PASS 1.0
- `cs_uc_a_no_ad_id_ad_specific`、`cs_uc_a_loaded_listing`（Tier-1 primary）= FAIL 稳定
- `cs_uc_a_lookup_failed`、`cs_uc_fp_loaded_moderation`（Tier-2 neighbor）= FAIL

S-Y2 pilot 期望 autoloop 提出一个能让 Tier-1 primary 翻 PASS、anti-
误杀 control 保持 PASS 的 skill-yaml `procedure` 候选。

### 3.3 68 条 iteration 总览（experiments.jsonl 统计）

来源：`python3 jq` over `autoloop/results/experiments.jsonl`，在 HEAD 验证。

- **Decision**: `discard: 52` / `error: 16` / `keep: 0`
- **Infra errors（OQ-S65.7/8 path）**: 0
- **Last keep iteration**: None

**Top discard reasons**:

| Count | Reason prefix |
|---|---|
| 20 | `tier1_bad_cases_regression_5_to_0`（candidate 把 5 个 bad_cases 全打回 0） |
| 15 | NONE（error iterations，多半 proposer LLM JSON unparseable / applier crash） |
| 14 | `tier0_escalation_compliance_failed_on_cs001_uc_c_mechanical_template_escalate` |
| 6 | `content_validator_rejected`（length overflow / underflow） |
| 余 | 多个 single-case `tier0_escalation_compliance_failed_on_<case>`（cs014/cs011/cs11s01/cs11s02）+ 1 个 `tier0_no_pii_leakage` + 1 个 anchor_outcome 7→4 |

**Target distribution**:

- `resolve_intake_collect_and_handover.yaml`: 29（43%）
- `resolve_faq_grounded_answer.yaml`: 11
- `discover_triage.yaml`: 8 / `escalate.yaml`: 8
- `confirm.yaml`: 4 / `terminal.yaml`: 3

**Field distribution**: `$.procedure: 17 / $.escalation_policy: 17 /
$.grounding_instruction: 15 / $.critical_steps[*].desc: 13`。基本均衡。

**Gaming flags**:

- `suspect_baseline_manipulation.git_lookup_failed`: **47/68** WARN（噪声）
- `tier2_measurement_contract_change_attempt`: **23/68** ERROR（信号需要 reframe，§5）

### 3.4 exp-63..exp-68 微观（含 2026-06-09 run-1 tranche）

| Exp | Target | Field | Edit shape | Discard reason | Rationale 摘 |
|---|---|---|---|---|---|
| exp-63 | escalate | $.procedure | **pure additive** 143→885 char | tier0 escalation_compliance fail on cs11s02 (UC-D password loop high distress) | "Per L-005, restrict to an additive clause... append targets the dominant failure shape — intake-before-handover across UC-G/H/I/J/K — at the escalate-skill chokepoint" |
| exp-64 | resolve_intake | $.procedure | **pure additive** 1164→1962 char | **tier1 anchor_outcome 7→3** | "dominant failure shape is incomplete intake before handover across UC-G/H/I/J/K... appends a pre-handover re-check clause" |
| exp-65 | resolve_intake | $.grounding_instruction | **pure additive** 232→863 char | tier0 escalation_compliance fail on cs11s02（同 exp-63） | "Per L-005, single-sentence additive append... explicitly names the per-UC intake variant alignment as must-preserve" |
| exp-66 | resolve_faq | **$.escalation_policy** | **pure additive** 556→1098 char | **tier2 critical_flow regression aggregate 3→4** | "Per lessons L-004 and L-005, the dominant residual failure shape is a globally fragile escalation decision boundary... appends a generic CS clause naming stuck-loop and distress patterns as legitimate escalation triggers" |
| exp-67 (run-1) | confirm | **$.escalation_policy** | **pure additive** +328 char | **tier0 escalation_compliance fail on cs11s01_uc_d_two_emails_one_account** | "Recent discards cluster on tier0 escalation_compliance failures driven by a high-distress account-recovery loop fixture, and lessons L-2026-05-31-004 and L-2026-05-31-005 identify the escalation decision boundary in CONFIRM as fragile" |
| exp-68 (run-1) | discover_triage | **$.escalation_policy** | **pure additive** +484 char | **tier0 escalation_compliance fail on cs11s01_uc_d_two_emails_one_account** | "Per the analyzer summary and lessons L-004/L-005, tier0 escalation_compliance fails on a stable cluster of high-distress, repeated-failure account-recovery scenarios because DISCOVER keeps requesting clarification" |

### 3.5 Run-1 validation tranche (2026-06-09) — what the new data added

The 3-iteration tranche (exp-66 / exp-67 / exp-68; ~4.6 h total, ~90 min/
iter; per-exp backends :63951 / :63234 / :62711; preflight + auto-reboot
worked; no dirty-index contamination; experiments.jsonl appends OK)
moved the audit from "structural hypothesis grounded in code" to
"structurally confirmed by live data + sharpened with a new P0".

**Confirmed**:

- **P0-A confirmed by direct evidence**: exp-67 and exp-68 fail Tier-0
  escalation_compliance on **the same case** (`cs11s01_uc_d_two_emails_one_account`).
  exp-67's discard_reason names it; exp-68 follows ~90 minutes later
  with the same case in the same gate. If the proposer had seen the
  rich `verdict.tier_breakdown` from exp-67 (which case regressed, what
  the prior baseline pass-rate was on cs11s01), exp-68 should have
  steered away from "make escalation more aggressive in DISCOVER" — but
  it doubled down because it only saw the one-line discard_reason string.
- **P0-B confirmed by direct evidence**: exp-66/67/68 rationales **all
  describe the SAME static baseline failure-shape** ("globally fragile
  escalation decision boundary", "high-distress account-recovery loop
  fixture") even though run-1 candidates introduced **NEW** regressions
  on top of baseline (Tier-0 fail on cs11s01 was NOT a baseline failure
  before exp-67's edit landed — confirmed via baseline
  `anchor_outcome/aggregated.json` for cs11s01-shadow + bad_cases). The
  analyzer keeps describing the same static landscape because it only
  reads `baseline_dir`.
- **P1 (lessons feedback loop) is worse than the original audit thought**:
  exp-67/68 rationales explicitly cite May-31 lessons (`L-2026-05-31-004`
  / `L-2026-05-31-005`); those lessons were authored when the CS4 cases
  did not exist (they entered the bad_cases suite on 2026-06-08 commit
  `0b111fd`). lessons are literally telling the proposer to hunt a
  pre-CS4 cluster.

**Sharpened (new P0-C)**: All three run-1 candidates target
`$.escalation_policy`, in three different skills (resolve_faq / confirm /
discover_triage). The proposer is doing a search over WHICH skill's
escalation_policy to edit, but the answer to "should I edit
escalation_policy at all?" is NO for the S-Y2 pilot:

- **S-Y2 PRIMARY TARGETS**: `cs_uc_a_no_ad_id_ad_specific` +
  `cs_uc_a_loaded_listing` (per `sprint_objective.md` §Tier
  classification). Both are UC-A bad_cases that fail because the agent
  ANSWERS without first verifying entity-context (no_ad_id / loaded
  listing's moderation_reason).
- **Phase**: Both cases enter DISCOVER → classify UC-A → RESOLVE-FAQ →
  `search_knowledge` / `resolve_article` → grounded answer. Neither
  reaches CONFIRM (only after a `record_outcome` invoked) or ESCALATE
  (only on `request_handover`). On the **success** path neither uses
  `$.escalation_policy` of any skill.
- **The plausible target** (per `sprint_objective.md` §Scope): "the CS4
  entity-context guidance most plausibly lands in
  `resolve_faq_grounded_answer.yaml` `procedure` / `grounding_instruction`
  or an existing `critical_steps[*].desc`". Verified at HEAD:
  `resolve_faq_grounded_answer.yaml:applicable_use_cases: [UC-A, ...]`,
  `applicable_phases: [RESOLVE]`. **Run-1 hit it once on the wrong field
  (`$.escalation_policy`, not `procedure` / `grounding_instruction`),
  twice on phase-incorrect skills (confirm / discover_triage's
  escalation_policy).**

**The "off-gap" picture is structurally entrenched**: the proposer sees
a baseline failure-taxonomy that flags `cs001 / cs011 / cs014 / cs11s01 /
cs11s02 / cs_uc_a_no_ad_id_ad_specific / cs_uc_a_loaded_listing / ...`
as failing. Without target-priority steering, **and with lessons that
all point at the pre-CS4 cluster**, the proposer's prior probability of
choosing the CS4 cases as the target is approximately equal to the
1/(total-failing-case-count) — well under 10%. Run-1's 0/3 hit-rate is
the expected outcome, not bad luck. **Scaling `-n` won't fix this**;
~90 min × 24 iterations ≈ 36 hours produces ~2-3 on-gap candidates by
random chance, none of which would necessarily be §4.1-acceptable.

**用户诊断的 6 条结构性原因，在数据里全部命中**：

| 用户原因 | Code-grounded 证据 |
|---|---|
| #1 Analyzer 看 baseline，Proposer 写 rationale 时却看 candidate 的 discard 反弹 | `analyzer.py:58-86` 只取 `baseline_results_summary` + `lessons_md` + `recent_iterations` 三块；`recent_iterations` 在 `_build_user_input`（analyzer.py:155-183）只含 `iteration_id / target_skill / target_field / decision / discard_reason`，**不含** candidate 的 results.json |
| #2 没有 candidate eval → analyzer 闭环 | 同上 + `loop.py:158-172` 只把 baseline_summary 喂给 analyzer；candidate iteration 的 `result.eval_results` / `result.verdict.tier_breakdown` 都**没有**回灌给下一轮 analyzer |
| #3 Lessons 强化错 heuristic | `lessons_compactor.py` 每 K=10 轮对所有 10 条（不区分 keep/discard）做 LLM 总结；K=10 全 discard 时它会得出"halt all substitutive edits"这种悲观启发式（实测 L-002 / L-003 / L-004 / L-005 均如此），随后**`propose.txt` 把全量 `LESSONS_MD` 注入 proposer**，proposer 显式服从（exp-63/64/65/66 rationale 都引 L-005） |
| #4 每轮从同一 Skill YAML 起点出发 | 0 keeps × applier cleanup checkout 回原 branch → `_materialize_before_after_yaml`（loop.py:463-475）每轮读到的 before_value 都是 main 当前内容；**M-Auto-1A 设计上有意如此**（program.md §7："The loop will never propose changes the human is forbidden to make")，但 0-keep 放大了"在同一座山上随机换方向挖" |
| #5 Research/deliver agent 不在 loop 内 | propose.txt 输入只有 FAILURE_TAXONOMY / LESSONS_MD / RECENT_ITERATIONS / TARGET_SKILL_FILES_CONTENT；**没有** sprint_objective / 当前活动 milestone / 当前 §5.6 bad-case bias 这类外部 steering |
| #6 exp-64 是 "方向看似对、结果仍错" | exp-64 rationale 切中 §5.1.2 Tier-1 target；但 anchor_outcome 7→3 直接说明 candidate 的 procedure 改写让 **某些原本 PASS 的 anchor case 现在 FAIL**——这正是 § "the proposer's reading of failure shape was plausible but misaligned with eval's actual sensitivity" 的典型形态 |

## 4. Gap analysis

### 4.1 跟 S-Y2 pilot acceptance bar 的差距

`sprint_objective.md` §5.1 把 pilot 的 success 写为：

1. 跑完。
2. Baseline gap 已确认（DONE）。
3. Autoloop 出 ≥1 个 candidate。
4. **PRIMARY SUCCESS**：candidate 合并 + 重 bless 后 Tier-1 primary 翻
   PASS、anti-误杀 control 保持 PASS、Tier-2 neighbor improve/hold。
5. **VALID ALTERNATE OUTCOME**：没有可接受 candidate 时启动 §3.4
   hand-authored fallback——pilot 仍然 valid。

**Gap**：当前 autoloop 在 68 轮上 0 keep。CS4 entity-context 这种需要
"基于一个新 projection slot 引导 LLM 多走一步 verify" 的 procedure 改
动，比"补一句 must-preserve"的难度更大（CS4 的 §5.1.5 SUCCESS bar
要求 Tier-1 翻 PASS 同时 anti-误杀 NOT regress）。在闭环没收紧的情况下，
**§3.4 fallback 大概率被触发**——pilot 仍然 valid 但 PRIMARY SUCCESS 概
率显著低于现实可达。

### 4.2 跟 §1.7 forbidden list 的吻合度（很好）

- "用 prompt 当 if-else dump"——`propose.txt` 明文禁止，anti-hardcode Q1 检测，**没问题**。
- "靠 keyword/regex/if-else 修 semantic failure"——同样有 prompt 禁止 + Q1/Q2 detector，**没问题**。
- "把 raw eval phrase 编进 prompt"——Q4 detector + propose.txt §1.7 mapping，**没问题**。
- "拓宽 eval spec 接受 genuine mistake"——CaseSpec 不在 mutable surface（program.md §3.A.2），**结构性不可能**。
- "为 visible eval 牺牲 shadow"——Layer 4 + shadow firewall，**结构性不可能**。

也就是说：**autoloop 的"不该做什么"边界是清晰且强结构性防御的，
gate 没有在打瞌睡**。68 个 discard 都是 gate 正确工作的结果，不是 gate
在乱叫。

### 4.3 跟 §3.2 Fix Layer 分类的关系

本次 audit 提出的修复 **§3.2 Layer = `infra`**（autoloop 自身的 meta-
agent prompt builder 是 LLM-call 编排，不是 agent 的 semantic decision）。

`iteration_governance.md` §5.8 "Framework-defect priority"：

> A brief whose §3 layer is `infra` AND whose scope is the eval
> framework (simulator, trace emitter, scoring, baseline aggregation,
> judge harness) preempts semantic sub-sprints.

字面意义上 autoloop **不是** eval framework 本身（它 consume eval_interactive
results）；但**精神上**适用——一个 meta-agent 把搜索方向锁死的 loop
等价于"半破的 scoring 还在打分"。**建议人类把本 audit 视作 §5.8
精神适用范围内**，决定是否插一个 S-Y1.5 patch sprint。

## 5. 详细的 P0/P1/P2 缺陷剖析（code-grounded）

### 5.1 P0-A：反馈信号过薄

**Where**: `autoloop/autoloop/meta_agent/proposer.py:188-202` +
`autoloop/autoloop/meta_agent/analyzer.py:167-181`

```python
# proposer.py:188-202 (HEAD-verified)
"RECENT_ITERATIONS:",
json.dumps(
    [
        {
            "iteration_id": r.get("iteration_id"),
            "target_skill": _extract_target_skill(r),
            "target_field": _extract_target_field(r),
            "decision": r.get("decision"),
            "discard_reason": r.get("discard_reason"),
        }
        for r in recent_iterations
    ],
    ...
)
```

`recent_iterations` 这个变量在 `loop.py:160-166` 里是
`_iterations_index.query_recent(...)` 返回的 `IterationRecord` 列表，
随后通过 `_record_to_dict_for_meta_agent`（loop.py:653-668）转成 dict——
**这一步里 `verdict.tier_breakdown` 是带着的**：

```python
# loop.py:653-668
return {
    "iteration_id": record.id,
    "hypothesis": {...},
    "decision": record.decision,
    "discard_reason": record.discard_reason,
    "verdict": {"tier_breakdown": record.fitness_delta.get("tier_breakdown", {})},
}
```

`tier_breakdown` 包含每层的丰富 metric（哪 case 哪个 Tier-0 family 触发、
per-UC mandatory-failure 桶、bad_cases / anchor_outcome 各自的 pass
count、improvement_threshold 各项 delta、shadow aggregate drop_pct），
但 **proposer 和 analyzer 的 prompt builder 把这块 strip 掉了**，只往
LLM 喂 5 个 string field。

**后果**：proposer 看到 `discard_reason =
"tier0_escalation_compliance_failed_on_cs11s02_uc_d_password_change_loop_high_distress"`，
但**不知道**：

- 这次 candidate 在 bad_cases / anchor_outcome / shadow 各自 pass 了几个；
- 除了这一个 case，其他 case 是否也 regress；
- 是否是 cs11s02 单点 flake，还是 family 范围的 regression；
- candidate 的 trace 上 cs11s02 走到了哪个 phase；
- 是 Layer 0 short-circuit 之前 Layer 1/2/3 是不是已经过了。

LLM 在这点上几乎和盲猜没区别。所以每轮只能拿 `discard_reason` 字符串
当神谕，proposer 写出来的 rationale 看起来"plausible"但**没有真正的
反馈梯度**。

**Why this is a P0**: 这是 0-keep 现象的**主要直接因**——不是
gate 太严，是 search 走得太盲。

### 5.2 P0-B：失败地形锚在 baseline，不锚在 candidate

**Where**: `autoloop/autoloop/loop.py:158-172` + `analyzer.py:58-86`

```python
# loop.py:158-172
baseline_summary = _build_baseline_summary(config, root)   # ← 永远是 m-auto-7-prepilot-baseline-20260608
lessons_md = _lessons_log.read_all(paths["lessons"])
recent_iters_records = _iterations_index.query_recent(...)
...
taxonomy = _analyzer.analyze(
    baseline_summary, lessons_md, recent_iters_dicts, client=client,
)
```

```python
# analyzer.py:58-86
def analyze(baseline_results_summary, lessons_md, recent_iterations, *, client):
    ...
    user = _build_user_input(baseline_results_summary, lessons_md, recent_iterations)
    response = client.chat(system=system, user=user)
    ...
    return _sanitize_taxonomy(parsed)
```

`analyze()` 的 `BASELINE_RESULTS_SUMMARY` 是 baseline 的 per-suite
per-case 失败明细（`analyzer.py:88-134`，含 `tier2_mandatory_failures`
+ `failure_shape`）；**但 candidate iteration（哪怕是 exp-66 那种刚
跑完的最近一轮）的 results.json 从未进过 analyzer**。

所以 analyzer 在 exp-67 时看到的 "failure taxonomy" 永远是
m-auto-7-prepilot-baseline 静态地形——包括 "intake-complete-before-
handover advisory miss across UC-G/H/I/J/K"——尽管 exp-64 已经证明
**对那块下手会让 anchor_outcome 从 7 掉到 3**。

**Why this is a P0**: 这是用户描述的"taxonomy 是一套、门禁是另一套"
的根因——proposer 永远在解 baseline 的题，门禁却在判 candidate。

### 5.3 P1：Lessons 把 0-keep 窗口的悲观启发式固化为律令

**Where**: `lessons.md` L-001..L-005 +
`autoloop/autoloop/meta_agent/prompts/compact.txt`

`lessons.md` 当前 5 条 lesson 的来源全是"全 discard 窗口"——
L-001（exp-11..20，全 discard）、L-002（exp-21..30，全 discard）、…、
L-005（exp-51..60，全 discard）。每条 lesson 的 "Heuristic for future
propose" 都被 LLM compactor 总结成越来越保守的指令：

- L-001：minimal, surgical diffs, pre-check character budget
- L-002：additive, narrowly-scoped clauses... preserve existing wording verbatim
- L-003：avoid rewrites on safety-adjacent skills entirely
- L-004：**stop editing prose on any skill in the escalation path until a baseline tier0 run identifies which exact phrases drive escalation_compliance**
- L-005：**halt all substitutive edits**; only single-sentence appended clauses that **explicitly name the recurring failure scenarios** (mechanical-template / faq-miss-without-distress / account-recovery / upload-login / search-pivot) as must-escalate or must-preserve

**问题层层叠加**：

1. `compact.txt` 的 template 没有要求 compactor 区分 "lessons from KEPT
   iterations" vs "lessons from DISCARDED iterations"。0-keep 窗口的
   "Pattern" 永远是悲观的。
2. `propose.txt` 把全量 LESSONS_MD 注入 proposer（`proposer.py:185-186`）。
3. exp-63 / exp-64 / exp-65 / exp-66 的 rationale 都**显式服从 L-005**
   ("Per L-005, this is a single-sentence additive append...")。proposer LLM
   把过去的总结视作"约束"，但那个总结本身是从一个 0-keep 窗口压出来
   的——拿过去的失败教训当未来的搜索方向，等于把搜索锁死在 0-keep
   附近。
4. L-005 甚至越界——"explicitly name the recurring failure scenarios"
   实际上**鼓励** proposer 在 prose 里点名 case family（"mechanical-
   template escalation, faq-miss-without-distress, account-recovery
   loops..."），这非常接近 §1.7 "encoding raw eval phrases" 的违禁边界
   （exp-63 rationale 的 appended chars 几乎逐字复用 L-005 的字符串）。
   anti-hardcode detector 没 FAIL 这些是因为它检测的是 fields 内容里
   case_id / session_id / decision-tree narration，而不是 lessons-driven
   semantic-family pattern matching——但精神上离 §1.7 红线已经很近。

**Why this is P1（不是 P0）**: 即使 lessons 完全清空，没有 5.1/5.2 的
信号反馈修复，proposer 还是会被 baseline taxonomy 锁定方向，所以
lessons 是 amplifier 而不是 root cause。但它是显著的方差放大器。

### 5.4 P2-A：anti-repeat heuristic 太粗

`propose.txt:60-68` 只写"如果最近 N 轮在同一 `(target_skill,
target_field)` 全 discard，就换一个 target"。但**实际数据**：

- 29/68 落在 `resolve_intake.yaml`，散在 procedure / grounding_instruction /
  critical_steps 多个 field 上。
- discard_reason 反复命中 "tier0_escalation_compliance_failed_on_cs001
  / cs014 / cs011 / cs11s01 / cs11s02"——**同一类 check 在不同 case 上
  反复 fire**。

只看 (skill, field) 完全相同的话，proposer 不会触发 "switch target"
建议——它只会切到 `resolve_intake.yaml` 的另一个 field。**没有
"discard-shape 重复" 这个维度的 anti-repeat 维度**。

### 5.5 P2-B：0-keep + 永远 clean-checkout 没有 hill-climbing 内存

**这是 M-Auto-1A v1 的有意设计**（program.md §7：每个 keep 候选只在
`autoloop/keep-N` 分支，下轮重新从 main 起步）。设计目的是：

- 每轮 candidate 在干净 baseline 上独立测，候选间没有 chain dependency。
- 人类对"哪个 candidate 应该 cherry-pick"有完全控制权。

但 0-keep 窗口下，这个特性退化为"每轮都从同一个 6 × 4 baseline 出发，
在不同方向上做 small random walks"。在反馈信号过薄（P0）的环境下，
方差放大显著。

**修复建议**：先修 P0；如果 P0 修完仍然 0-keep，再考虑 "partial-keep"
（保留某些 advisory layer 改进作为下轮起点）这种 v2-grade 设计——但
那是新 milestone 的事。

### 5.6 P2-C：`suspect_baseline_manipulation.git_lookup_failed` 47/68 噪声

`gaming.py:259-302` 跑 `git log --format=%H main -- autoloop/config.yaml`
检测 baseline_dir 是否被恶意篡改。在 autoloop/exp-N 分支上 `main` 的
config.yaml history 经常 "no commits"，于是触发 WARN。

**这是 observation-only flag，不影响 gate**。但它在 audit 输出里淹没
其他真正的信号。

### 5.8 P0-C (NEW, 2026-06-09)：Proposer 不知道 active milestone 的 PRIMARY TARGETS

**Where**: `autoloop/autoloop/meta_agent/prompts/propose.txt`（72 行
prompt 没有 pilot-target 概念）+ `analyzer.py:155-183`（同样没有）+
`loop.py:158`（输入 build 无 pilot context）+ `autoloop/config.yaml`
（无 `pilot` block）

**Run-1 direct evidence** (§3.5):

- 3/3 candidates 砸到 `$.escalation_policy`，覆盖 3 个不同 skill
  （resolve_faq / confirm / discover_triage）。
- 0/3 触碰 S-Y2 PRIMARY TARGETS（`cs_uc_a_no_ad_id_ad_specific` /
  `cs_uc_a_loaded_listing` — UC-A entity-context cases in
  `bad_cases/aggregated.json` as stable-FAIL）。
- 2/3 在 phase-incorrect skill 上动手（confirm.yaml `applicable_phases:
  [CONFIRM]` + escalate.yaml `[ESCALATE]` — CS4 cases 走 RESOLVE-FAQ，
  never reach CONFIRM/ESCALATE 在 success 路径上）。

**Why this happens (code-grounded mechanism)**:

`propose.txt` 给 proposer 的 prompt-input 只有四块（行 71-78）：

```
- FAILURE_TAXONOMY (from analyzer)
- LESSONS_MD
- RECENT_ITERATIONS (compact table)
- TARGET_SKILL_FILES_CONTENT — full current YAML of the six allowed files
```

`FAILURE_TAXONOMY`（analyzer 输出）schema (analyzer.py:36-52)：

```python
class FailureTaxonomy(TypedDict, total=False):
    skills_critical_steps_advisory_fail: dict[str, dict[str, dict[str, Any]]]
    bad_cases_regressing: dict[str, dict[str, Any]]
    anchor_outcome_closure_criterion_fails: dict[str, dict[str, Any]]
    summary: str  (no case_ids; one paragraph)
```

CS4 cases（`cs_uc_a_no_ad_id_ad_specific` / `cs_uc_a_loaded_listing` /
`cs_uc_a_lookup_failed`） **会** 出现在 `bad_cases_regressing` 字段里
（它们是 baseline 的 stable-FAIL bad_cases）。但他们和 `cs001 / cs011 /
cs014 / cs11s01 / cs11s02` 平等地混在一起；proposer 看 prompt 时**没
有任何信号**告诉它："这两个 cs_uc_a_* 是 PRIMARY TARGETS，cs11s01 是
NEIGHBOR / OFF-PRIMARY-TARGET"。

**Verified at HEAD**: `propose.txt` 仅在第 60-68 行有 anti-repeat
heuristic ("if last N iterations on same (skill, field) were all
discarded, switch")。**没有任何 target-priority concept**。同样
`analyze.txt` 第 12-22 行的 input description 也无 pilot-target block。

**Why this is a P0 (not P1/P2)**:

- 即使修了 P0-A + P0-B（让 proposer 看见 tier_breakdown + 让 analyzer
  读 candidate results），proposer 仍然不知道**该往哪里搜**。它会看到
  "cs11s01 在 exp-67 被弄 FAIL 了，candidates' edits introduced new
  regression at cs11s01"——但它会理解为 "我应该让 cs11s01 over-escalate
  得少一点"，继续在 escalation_policy 上动手。
- P0-C 直接攻"该往哪里搜"——给 proposer 显式的 PILOT_PRIMARY_TARGETS
  list + 鼓励它选 phase-correct skill 作为 target。
- 不修 P0-C，S-Y2 几乎没有 PRIMARY SUCCESS 概率（per §3.5 期望命中率）；
  pilot 只能走 §3.4 fallback。

**Why this wasn't visible in the original 2026-06-08 audit**:

原 audit 把"proposer 看不到 sprint context"列为 P2（用户诊断 #5），
理由是 program.md §7 把 autoloop 设计成"general-purpose hill-climber"。
但 M-Auto-7 S-Y2 是首个**TARGETED pilot**——明确定义了 PRIMARY TARGETS
+ anti-误杀 control + neighbor tier。在 targeted-pilot 语境下，缺乏
target-priority 注入从"设计权衡"升级成"P0 阻塞"，原 audit 没充分意识
到这一点。Run-1 数据让升级显式化。

### 5.7 P2-D：`tier2_measurement_contract_change_attempt` 23/68 ERROR 触发——信号需要 reframe

`gaming.py:573-602` 检测：baseline 里 FAIL 的 critical_step，candidate
里 outcome 变成 N/A / skipped。逻辑上是为了防 candidate 修改
`trace_check` 让步骤"消失"（measurement contract gaming）。

但 candidate 是**additive prose 修改**——`trace_check` 字段不在
mutable surface（`program.md` §2 + sandbox 拒绝），所以这个 ERROR 触
发的真实原因 100% 是：**candidate 让 agent 在该 critical_step 之前
就 fail 掉了**——run loop 没走到 step，step 自然 outcome = N/A。

这其实是一个 **"candidate 改了 agent 行为，让一个 baseline 里 reach 的
critical_step 现在 reach 不到"** 的信号。它正确地说明：
"additive prose 不是完全惰性的——它在改 agent 走向"。但 rule_id 把它
描述成 "measurement contract may have changed" 是误导性的——measurement
contract 没变，trace path 变了。

**这条 flag 不应该是 ERROR severity，应该是 INFO 或 WARN**，并且 rule
名应该改成 `tier2_path_diverged_from_baseline` 之类的。但目前 observation-
only，不堵 gate，**P2 backlog**。

## 6. Design alternatives

### Option A — 阻断 S-Y2 pilot，先做完整 autoloop substrate hygiene milestone

**Scope**: 开一个新 milestone M-Auto-8（或 M-Auto-7 之内的紧急 S-Y0
preemption 用 §5.8 framework-defect priority），把 P0-A + P0-B + P1
lessons-rot + P2 三项都修了再启 S-Y2 Part C。

**Pros**:
- 让 S-Y2 pilot 的 PRIMARY SUCCESS bar 有现实可达性，不只是触发
  §3.4 fallback。
- 修完之后所有后续 M-Auto-7+ 的 autoloop pilots 都受益（CS4 之后还有
  S-B CS2-original 的 autoloop 可能性、未来 CS5/CS6...）。
- 干净的 patch-then-pilot 顺序，方便 Codex 在 milestone close 时审。

**Cons**:
- M-Auto-7 推迟 1-2 周（autoloop substrate sprint ~3-5 d + 重 rebless ~1 d）。
- 风险：substrate 改完后**仍然** 0-keep——CS4 task hardness 比 prior
  windows 高，pilot 可能也需要 §3.4 fallback。如果是这样，substrate
  patch 的 ROI 就是 "下次 pilot 受益" 而不是 "S-Y2 SUCCESS"。
- 跟用户对当前 dirty-tree 的 toleration 不太兼容（要先 commit / stash
  exp-67 状态）。

### Option B（推荐）— 插一个 S-Y1.5 / S-Auto-31.5 "pre-pilot autoloop substrate patch"，再走 S-Y2 Part C

**Scope (updated 2026-06-09)**: 极窄 scope，**只**修 P0-A + P0-B +
P0-C + P1。Run-1 数据强化了 P0-A/P0-B 必要性，并新增 P0-C 作为必修项。

具体改动（修复半径估算）：

1. **P0-A fix**（~30 行 Python） — 修
   `proposer.py:_build_user_input` + `analyzer.py:_build_user_input`，
   把 `verdict.tier_breakdown` 加进每条 recent_iteration row 里。
   测试：现有 `tests/test_meta_agent.py` 走通即可。
2. **P0-B fix**（~80 行 Python + ~30 行 prompt 改动） — `analyzer.py`
   新增可选输入 `recent_candidate_results: list[dict]`，把最近 K=3
   candidate 的 per-suite results.json 摘要（**不含 shadow per-case
   detail——firewall 必须显式扩展**）一起喂给 analyzer；`analyze.txt`
   提示词加一段"compare CANDIDATE_RESULTS vs BASELINE_RESULTS, mark
   regressions the candidates INTRODUCED vs pre-existing baseline
   failures"；`loop.py:158` 读最近 K 轮 `eval_traces_path` 持久化的
   per-iter `eval-results.json` 拼起来。
3. **P0-C fix (NEW)**（~80 行 Python + ~40 行 prompt 改动 + ~20 行
   config schema） —
   - `autoloop/config.yaml` 新增 `pilot` block:
     ```yaml
     pilot:
       active_sprint: "S-Y2"     # human/deliver-agent maintained
       primary_targets:           # MUST flip to PASS post-merge
         - cs_uc_a_no_ad_id_ad_specific
         - cs_uc_a_loaded_listing
       anti_kill_control:         # MUST STAY PASS
         - cs_uc_a_generic_policy_question
       tier2_neighbors:           # should improve/hold; not fail-gate
         - cs_uc_a_lookup_failed
         - cs_uc_fp_loaded_moderation
       phase_hint:                # 引导 proposer 选 phase-correct skill
         - DISCOVER
         - RESOLVE
       use_case_hint:             # 引导 proposer 选 UC-coverage 对的 skill
         - UC-A
     ```
   - `analyze.txt` + `propose.txt` 双双注入 `PILOT_PRIMARY_TARGETS`
     block，并在 prompt 指令里加：
     * "(Analyzer)  Mark each entry in `bad_cases_regressing` /
       `anchor_outcome_closure_criterion_fails` with a `target_role`
       field: `primary` / `anti_kill_control` / `tier2_neighbor` /
       `general` based on PILOT_PRIMARY_TARGETS membership."
     * "(Proposer) Selection BIAS: prefer skill files whose
       `applicable_use_cases` intersects PILOT.use_case_hint AND
       `applicable_phases` intersects PILOT.phase_hint. Edits to
       skills outside this set are ALLOWED but the rationale MUST
       explain why a non-bias target is justified."
     * "(Proposer) NEVER edit a Skill in a way that you expect to
       regress an anti_kill_control entry. The pilot acceptance bar
       fails if the control regresses."
     * **"(Proposer) The primary target case IDs in
       PILOT_PRIMARY_TARGETS are evaluation bookkeeping labels. Use
       them ONLY to prioritize which failure cluster to analyze. Do
       NOT mention, encode, paraphrase, or create rules around these
       IDs or their literal fixture wording in any proposed
       `after_value`. The Skill YAML is read by the production agent
       on every customer turn — eval identifiers leaking into prod
       prompts is the §1.7 forbidden-list violation this rule
       prevents."** (NEW 2026-06-09 per Codex / human reviewer
       feedback — strengthens the existing propose.txt:36-49 §1.7
       mapping + anti-hardcode Q4 detector with a directly-targeted
       phrasing.)
   - `loop.py` 在 build_user_input 时把 config['pilot'] 整块以
     `PILOT_PRIMARY_TARGETS` 序列化进 prompt。
   - `propose.txt` 顺带补一条**phase-awareness 注释**：把六个 Skill
     YAML 的 `applicable_phases` / `applicable_use_cases` 提取出来作
     为 `SKILL_PHASE_USECASE_MAP` 注入，帮 LLM 做 phase-correct 选择。
4. **P1 lessons-disable**（~5 行 Python + 1 行 config） — 加
   `lessons.enabled: false` config flag；当 false 时 propose.txt 的
   `LESSONS_MD` block 替换成 `<lessons disabled for this run — historical
   lessons may not reflect the active pilot's targets>`；保留 lessons.md
   文件不删（forensic 用）；compactor 仍然运行（持续观察），但 proposer
   不读。**Run-1 数据使这条从"experimental opt-out"升级为"S-Y2 hard
   default"**——May-31 lessons 早于 CS4 baseline 存在，专门 cite 它们
   是 a feedback loop hazard。

**Test budget**: 修 `tests/test_meta_agent.py` 的 fixture 让 recent_iter
mock 数据带 tier_breakdown；新加测试覆盖 (a) shadow firewall on
candidate-results passthrough、(b) `PILOT_PRIMARY_TARGETS` 注入 +
serialization、(c) `lessons.enabled: false` 路径下 propose.txt 不含
lessons body。其他 24 个 autoloop tests 不动。

**Re-bless**: P0-A + P0-B + P0-C + P1 全部是纯 meta-agent prompt 改动
+ config schema 扩展，**不动 5-layer gate、不动 mutable surface、
不动 sandbox、不动 scoring code**。所以 `gaming.scoring_code_drift` 不
会触发；`config.yaml:scoring_code_baseline_sha` 不变；不需要重 bless
m-auto-7-prepilot-baseline。

**Pros**:
- ~2-3 d 工作量（比原 2-fix 多 1 d——P0-C 占大头）。
- 直接攻 P0 因，run-1 数据已经证明纯 hill-climbing 在 0/3 命中
  PRIMARY TARGETS——这次修完目标命中率应该显著上升（直接给方向）。
- S-Y2 pilot 顺延 ~3 d，仍然在 milestone 估时（§9 给的 3-5 d for S-Y2
  pilot）之内。
- Patch 是 `autoloop/` 内部 infra，**和 S-Y2 hard fences 不冲突**
  （S-Y2 fence 锁 `server/src/main/java/**` byte-identical、case_specs
  byte-identical、scoring code byte-identical；autoloop meta-agent
  prompt builder + autoloop/config.yaml:pilot block 不在该集合）。

**Cons**:
- 仍然有不确定性——P0-C 给 target 但不一定能给 candidate；CS4 task
  hardness 本身可能高于 hill-climbing 能力。验证标准是 **4-层 hit-rate
  分解**（见 §12 item 5 + §11.3 early-stop），不是单一 metric。
  Run-1 反例说明为什么单一 metric 不够：exp-66 选了 resolve_faq（skill
  命中）但改了 `$.escalation_policy`（field 没命中）——这是 false-positive
  steering，单一 hit-rate 会算成"成功"。**`full_on_gap_hit_rate` 才是
  primary success metric**。
- 需要人类同意"在 M-Auto-7 中插一个不在最初 sub-sprint 序列里的 micro
  sub-sprint"（deliver-agent 负责决定，本 audit 只是建议）。
- `autoloop/config.yaml:pilot` block 的 ownership 需要明确——建议
  deliver-agent 在每个 pilot-style sub-sprint 启动时维护，sprint
  archive 时把 block snapshot 进 handoff，**并把 block 内容
  snapshot + sha 进每条 `experiments.jsonl` row**（见 §8 + §10
  fences；2026-06-09 新增），以保证未来 audit exp-71/72 时可以重建
  "proposer 当时看到的目标卡"，避免回看时变成猜。

### Option C — 不修 substrate，直接跑 S-Y2 Part C，预期 §3.4 fallback 触发

**Scope**: 接受当前闭环局限性，按 sprint contract §5.1.5 "valid alternate
outcome" 跑——autoloop `-n 8 / -n 16` 跑出 0 keep 时启 §3.4 hand-authored
fallback，让人类/deliver-agent 直接写 procedure candidate。

**Pros**:
- 0 额外 sprint 工作量；S-Y2 立即推进。
- §3.4 hand-authored fallback 本身就是 valid pilot outcome——M-Auto-7
  milestone 仍然可以 close。
- 推 P0 fix 到 post-M-Auto-7（"autoloop hardening" 续作 milestone），
  让其他 P2/P3 改进一起做一个完整 v2 program.md。

**Cons**:
- pilot 的 PRIMARY value（"autoloop 真的能产 §1.7-acceptable candidate"）
  被弱化为 "autoloop ran end-to-end, but the candidate was hand-authored"。
- 用户的核心问题（"loop 为什么跑偏"）在 close 之前没有得到答复——只是
  延后了。
- L-006 / L-007 还会接着在 0-keep 窗口里被 compactor 总结出来，把
  lessons 推向更悲观——可能开始触碰 §1.7 "raw eval phrase encoding"
  红线（参 §5.3 已经看到 L-005 鼓励点名 case family）。

## 7. Recommended option + rationale

**仍然推荐 Option B**（插 S-Y1.5 autoloop substrate patch；scope 现在
是 4-fix 而不是 2-fix），理由（含 2026-06-09 update 后的 reinforce）：

1. **ROI 仍然高且风险低**：~2-3 d Python infra patch，不动 gate /
   sandbox / mutable surface / scoring code，所以**不需要重 bless**，
   不动 M-Auto-7 已经 done 的 Part C.1 baseline
   (`m-auto-7-prepilot-baseline-20260608`)。
2. **直接攻 root cause——run-1 数据已经验证 root cause 假设**：用户原
   6 条诊断里 #1 / #2 / #3 在 §3.5 已经被 run-1 直接证据 confirm；新发
   现的 P0-C 也由 run-1 的 0/3 on-gap 直接 surface。修这 4 个就是修用户
   实际发现的"跑偏"现象的全部已知 root cause。
3. **保留 S-Y2 pilot 的 PRIMARY SUCCESS 概率**：Run-1 的 0/3 on-gap 已经
   说明**不修 P0-C 时 PRIMARY SUCCESS 概率接近 0**。修 P0-C 后概率提
   升到"hill-climbing 应有水平"（target 命中后还要看 candidate 是否
   §4.1-acceptable）。即便最后仍走 §3.4 fallback，audit 已经做完，未来
   pilot 都受益——特别是 S-B CS2-original（如果走 autoloop）+ 任何
   future targeted pilot。
4. **跟 §1.7 / §5.8 精神兼容**：修复对象是 framework-defect 性质的 infra
   bug，preempt semantic sub-sprint 是 §5.8 鼓励的方向。新加的 P0-C
   引入 `pilot` block 配置——这是 a config-driven steering signal，
   structurally 不是 semantic hardcode（PILOT_PRIMARY_TARGETS 是 case_id
   list，不是 user_message keyword pattern，anti-hardcode detector 不
   应该 FAIL，但 Codex review focus 要确认）。
5. **不破坏 deliver-agent 已经规划好的 M-Auto-7 sequence**：S-A / S-X /
   S-Y1 已经 dev-side closed，S-Y2 是 CORE GATE 还没正式启动 Part C
   （run-1 是 validation tranche）；在 S-Y2 Part C 之前插一个 S-Y1.5
   micro sub-sprint 是 sequence-respectful 的。
6. **保留 Option A 的退路**：如果 S-Y1.5 修完之后 `-n 8` 命中率仍
   < 30%，pilot 走 Option C（§3.4 fallback），并把"autoloop hardening
   v2"（含 hill-climbing memory / partial-keep / anti-pattern lessons /
   tier2_path_diverged rename / gaming flag cleanup）推到 M-Auto-8。
7. **2026-06-09 incremental rationale**: Run-1 data made the "P0-C
   absent → 0 on-gap" hypothesis testable, and the test passed. 不
   修 P0-C 直接进 S-Y2 Part C 是已经知道概率不足而硬上 — 不是 ROI
   judgment 题，是数据已经回答的题。

**Option A 的拒绝理由（不变）**：太重；M-Auto-7 已经投入很多前期工作
（CS1/CS3/CS4 readiness），整个 pilot 拖一周不划算。如果 Option B 后续
验证不够，再走 A。

**Option C 的拒绝理由（强化）**：原 2026-06-08 audit 说"短期省 1-2 d、
长期成本更高"；现在 run-1 数据让"短期省"也站不住——Option C 下一个
`-n 8` 实验大概率 0/8 on-gap，~12h 烧掉 0 on-gap candidate，**这是
比 S-Y1.5 的 2-3 d 还贵的失败**。直接 §3.4 fallback 是技术上 valid
outcome 但放弃了 pilot 的 primary 验证目标——同样的 §3.4 fallback
路径在 S-Y1.5 修完后也仍然可走（如果 P0-C 后命中率提升但 candidate 仍
全部 §4.1-reject）。所以 Option C 没有 dominate Option B 的任何路径。

## 8. Scope split + delivery priority suggestion

**给 deliver-agent 的建议拆分**（不是 binding 决定）：

### S-Y1.5（micro pre-pilot, ~2-3 d, **建议插入** — 2026-06-09 scope expanded）

**§3.2 Layer**: `infra` (eval-framework-adjacent autoloop meta-agent
prompt builder + config schema)

**§7 stanza**: REQUIRED（不再 §7-EXEMPT — P0-C 引入了 `pilot` config
block 这一新表面，需要 stanza 明确"不是 semantic hardcode"）

**Scope (5 items, 2026-06-09)**:

1. (P0-A) 把 `verdict.tier_breakdown` 加进 `proposer.py` + `analyzer.py`
   的 `recent_iterations` block 序列化。
2. (P0-B) 让 analyzer 额外读最近 K candidate iteration 的
   per-suite results.json 摘要（**shadow firewall 显式延伸**——shadow
   suite 出去前必须 aggregate-only-filter），并在 `analyze.txt`
   提示 LLM 区分 "BASELINE_FAILURE" / "CANDIDATE_INTRODUCED_REGRESSION"。
3. (P0-C) 新增 `autoloop/config.yaml:pilot` block schema +
   `PILOT_PRIMARY_TARGETS` + `SKILL_PHASE_USECASE_MAP` 注入到 `analyze.txt`
   + `propose.txt`，让 analyzer 给每个 failing case 加 `target_role`
   分类，让 proposer 有 phase-correct skill-selection bias + 严格保护
   anti_kill_control 的指令 + **labels-only 显式声明**（见 §6 P0-C
   bullet 第 4 子项）。
4. (P1) 加 `lessons.enabled: false` config flag；S-Y2 default false
   （May-31 lessons 早于 CS4 baseline 存在，feedback-loop hazard）。
5. **(NEW 2026-06-09) Per-experiment pilot snapshot**: 每条
   `experiments.jsonl` row + `runs/<id>/hypothesis.json` 都嵌入当时生效
   的 pilot block snapshot：
   ```jsonc
   {
     "iteration_id": "exp-N",
     ...
     "pilot_snapshot": {
       "schema_version": 1,
       "active_sprint": "S-Y2",
       "primary_targets": [...],
       "anti_kill_control": [...],
       "tier2_neighbors": [...],
       "phase_hint": [...],
       "use_case_hint": [...],
       "block_sha256": "<16-char hex>"
     },
     "lessons_enabled": false
   }
   ```
   `block_sha256` 是 pilot block 内容的稳定 hash（normalized JSON
   serialization）；audit subcommand 可以 group by hash 查看"哪些
   iterations 用了同一个目标卡"。loop.py 在每次 iteration 起始时
   读 config 计算 hash 一次，存进 record。

**Test/eval requirements**:
- `tests/test_meta_agent.py` fixture 更新 + 新加 4 个测试：
  * `test_recent_iterations_passthrough_includes_tier_breakdown`（P0-A）
  * `test_candidate_results_passthrough_filters_shadow_per_case`（P0-B
    + shadow firewall——喂 mock 含 shadow per-case content，断言
    serialized prompt 不含 `cs59s` / `cs_case_specs_shadow` 等签名）
  * `test_pilot_primary_targets_block_serialized_in_propose_prompt`（P0-C；
    覆盖 PILOT_PRIMARY_TARGETS + SKILL_PHASE_USECASE_MAP + labels-only
    声明都在序列化输出里）
  * `test_lessons_md_not_in_propose_user_input_when_disabled`（P1）
- `tests/test_loop.py` 新加 2 个测试：
  * `test_experiment_record_embeds_pilot_snapshot`（每条
    experiments.jsonl row 含 `pilot_snapshot` block + 稳定 `block_sha256`）
  * `test_experiment_record_embeds_lessons_enabled_flag`
- 1 个 `test_cli_smoke.py` extension：跑 `autoloop run --dry-run -n 1` 在
  `pilot.enabled=true` config 下，断言 propose prompt 输出含
  PILOT_PRIMARY_TARGETS block，proposer LLM 选择 phase-correct skill
  （resolve_faq / discover_triage / resolve_intake — 任一即可）。
- **(NEW 2026-06-09) S-Y1.5 close 人工 checklist (dry-run rationale review)**:
  S-Y1.5 dev close 前，跑 `autoloop run --dry-run -n 2` 在 S-Y2 pilot
  config 下，对生成的 2 个 proposal 人工 review 以下 4 项（不作为硬
  gate，但作为 S-Y1.5 close handoff 必须 record 的 evidence；否则视为
  "prompt 注入了但模型没听进去"——P0-C 修复无效）：
  1. **Rationale 不引用 L-2026-05-31-004 / L-2026-05-31-005**（confirm
     lessons-disable 实际生效，而不只是 config 改了）。
  2. **`target_role` 在 analyzer 输出里标了 `primary` for the CS4 cases**
     （`cs_uc_a_no_ad_id_ad_specific` / `cs_uc_a_loaded_listing` 任一
     必须被标 primary）。
  3. **target skill 的 rationale 解释了为什么这个 skill 跟 UC-A /
     RESOLVE 相关**（不只是"L-005 说改 escalation_policy"）；至少一
     个 proposal 选 resolve_faq_grounded_answer.yaml 是加分。
  4. **`after_value` 不包含任何 case_id literal（`cs_uc_a_no_ad_id_*`
     等）也不包含 fixture 关键短语**（labels-only 规则有效）。
  Checklist 结果（PASS/FAIL per item + 总 verdict）写到 S-Y1.5 handoff
  作为 evidence；任一 FAIL → S-Y1.5 不 close，回头调整 prompt。
- 不需要重 bless（不动 scoring code、不动 baseline_dir、不动
  `scoring_code_baseline_sha`）。
- 不需要 §5.6 bad-case rerun（不动 agent semantic surface — pilot
  block 是 config-driven steering，不是 agent prompt 改动）。

**Per-sub-sprint Codex**: REQUIRED（per §4.3 + 因为 P0-C 让 §7 stanza
REQUIRED，所以 trigger #2 + #3）。

**Codex 重点 questions**:
- (P0-A) `verdict.tier_breakdown.shadow_regression.metrics_observed`
  现在只含 aggregate keys（`baseline_pass_rate / current_pass_rate /
  drop_pct / regression_detected`）——passthrough 之后 proposer 看到的
  仍然只是这个 aggregate？是的（`tier_evaluator.py:_evaluate_layer4`
  已保证），但 Codex 必须独立确认。
- (P0-B) recent candidate results 读取要走 `loop.py:_persist_eval_traces`
  写出的 `eval-results.json`（per-iter 持久化）——读时**必须** filter
  掉 shadow suite 的 per-case detail，只保留 shadow 的 aggregate counts
  + `cs59s*` regex blacklist 防漏。这是新加的 firewall 责任，必须
  explicit 实现 + test，Codex 必须独立验证。
- (P0-C) `pilot.primary_targets` / `pilot.anti_kill_control` 是 case_id
  list；Codex 需要确认：
  * 这 list **不** include shadow suite case_ids（按 v1 §1.7 shadow
    firewall 精神）。Pilot block schema 要 explicit document "shadow
    case_ids forbidden here"。
  * `PILOT_PRIMARY_TARGETS` 注入 prompt 后，proposer 的 rationale 仍然
    描述 "what kind of edit helps these cases" 而**不是** "search for
    text matching these case_ids"——后者会接近 §1.7 "encoding raw eval
    phrases" 红线。Codex 看几个 dry-run propose 输出。
  * `SKILL_PHASE_USECASE_MAP` 是从 6 个 Skill YAML 提取的 metadata，
    不是 hardcode；如果未来 Skill YAML 的 `applicable_phases` /
    `applicable_use_cases` 变了，map 要自动更新（实现要 lazy-read，
    不要 cache）。
- (P1) `lessons.enabled: false` 时 propose.txt 的 `LESSONS_MD` block
  内容是 `<lessons disabled for this run>` 占位符，**不**是空字符串
  （空字符串容易让 LLM 误以为 prompt 截断）。

### S-Y2（CORE GATE, ~3-5 d, **沿用 sprint_objective.md 当前 placeholder**）

contract 不变。配置上（S-Y1.5 close 时由 deliver-agent 写入）：

- `config.yaml:fitness.baseline_dir` 已经指向
  `m-auto-7-prepilot-baseline-20260608`（DONE）。
- `config.yaml:pilot` block 维护为 S-Y2 的 PRIMARY TARGETS / anti-kill
  control / Tier-2 neighbors / phase_hint=[DISCOVER, RESOLVE] /
  use_case_hint=[UC-A]（pre-pilot 由 deliver-agent 提交，post-pilot
  在 sprint handoff 里 snapshot）。
- `config.yaml:lessons.enabled: false`（S-Y1.5 出的 flag；S-Y2 default
  false 因为 May-31 lessons 早于 CS4 baseline）。
- `-n` 起步 8；评估使用 **4-层 hit-rate 分解**（见 §12 item 5）：
  - `phase_usecase_hit_rate` — candidate skill 是否覆盖 UC-A +
    DISCOVER/RESOLVE。最容易达成（baseline reference ~80%）。
  - `skill_hit_rate` — candidate skill 是否落在
    `resolve_faq_grounded_answer.yaml`。CS4 主路径 reference ~50%。
  - `field_family_hit_rate` — candidate field 是否落在
    `$.procedure` / `$.grounding_instruction` / `$.critical_steps[*].desc`
    （不是 `$.escalation_policy`）。修对修复表面 reference ~70%。
  - `full_on_gap_hit_rate` — 三者全部命中（**primary success metric**）。
    Run-1 baseline = 0/3 = 0%。
- 决策规则（详见 §11.3 early-stop；2026-06-09 round-2 calibrated —
  50% 是 strong pass 不是唯一合格线）：
  * `full_on_gap_hit_rate ≥ 50%` → **strong pass**；看 keep rate；
    不扩 -n 16。
  * `full_on_gap_hit_rate` 25–50% → **partial pass**；扩到 -n 16
    进一步确认（不是 "must improve to ≥ 50%"，只是要更多样本看稳定）。
  * `full_on_gap_hit_rate < 25%` → **硬早停**，surface "P0-C 修不到位"
    作为 follow-up R-item，启 §3.4 fallback。不要再烧 budget。

### S-B / 其他 backlog 不变

P2 项目（anti-repeat 维度扩展、gaming flag 噪声清理、
tier2_path_diverged rename）→ post-M-Auto-7 backlog（可能装到一个
"autoloop hardening v2" milestone 里，含 program.md v2 等更大改动）。

## 9. §7 Layer-classification + anti-hardcode stanza（给 S-Y1.5 准备，2026-06-09 updated）

```markdown
## Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop meta-agent prompt builder
+ analyzer input-shaping + config schema for pilot-target steering —
the eval framework's auto-evolution loop; §5.8 framework-defect
priority applies in spirit).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It
preserves all existing structural defenses (program.md §4): the
mutable-surface sandbox, the 5-layer lexicographic fitness, the
shadow-result firewall, the scoring-code drift guard, the anti-
hardcode detector, and the seven anti-gaming checks.

**Semantic hardcode:** No semantic hardcode introduced. The three
categories of edits in scope are:

1. **PROMPT INPUT shape changes (P0-A / P0-B)** — adding existing
   structured artefacts (`verdict.tier_breakdown` already in the
   experiments log; per-iter `eval-results.json` already persisted by
   `loop.py:_persist_eval_traces`) into the analyzer / proposer LLM
   input JSON. No new agent-runtime signal is generated; existing
   signals are surfaced into the loop's already-existing decision
   layer. The shadow firewall is explicitly EXTENDED (not weakened):
   the new "recent candidate results" passthrough MUST filter out
   shadow per-case detail before it reaches the prompt — only the
   existing `tier_breakdown.shadow_regression` aggregate keys + a
   `cs59s*` / shadow-leak-signature regex blacklist may pass.

2. **Pilot-target steering (P0-C)** — `autoloop/config.yaml:pilot`
   block (case_id lists + UC/phase hints), passed to analyzer /
   proposer as `PILOT_PRIMARY_TARGETS` + `SKILL_PHASE_USECASE_MAP`.
   This is config-driven STEERING (selection bias toward
   phase-correct skill files), NOT a semantic rule against any user
   utterance. The proposer's mutable surface remains the §2 6×4 set;
   anti-hardcode Q1/Q2/Q4/Q5 detectors still apply to the proposed
   `after_value`. Codex must confirm that proposer's resulting
   rationale describes "what kind of edit helps these cases" and not
   "search for text matching these case_ids" (which would approach
   §1.7 "encoding raw eval phrases" red line). Pilot block schema
   forbids shadow case_ids by construction (S-Y1.5 implements +
   tests).

3. **Lessons opt-out (P1)** — `lessons.enabled: false` config flag.
   When false, propose.txt `LESSONS_MD` block becomes a placeholder
   string; lessons.md is preserved on disk for forensic; compactor
   still runs. No agent-runtime impact.

4. **Per-experiment pilot snapshot + lessons flag (NEW 2026-06-09)** —
   every `experiments.jsonl` row + every `runs/<id>/hypothesis.json`
   embeds a `pilot_snapshot` block (case_id lists + hash) +
   `lessons_enabled` flag for that iteration's effective config. This
   is forensic-only; no agent-runtime impact; shadow firewall
   maintained because the snapshot can never contain shadow case_ids
   by config-validator construction.

5. **Labels-only directive in propose.txt (NEW 2026-06-09)** — adds a
   sentence under "# Forbidden patterns (§1.7 Constitution — Forbidden)"
   instructing the proposer to treat PILOT_PRIMARY_TARGETS case_ids as
   bookkeeping labels (prioritization signal only), never to be
   mentioned / encoded / paraphrased in `after_value`. Structural
   defense in depth with anti-hardcode Q4 detector. A unit test asserts
   the literal sentence is present in propose.txt.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: n/a — this is a pure-infra sub-sprint touching only
`autoloop/autoloop/meta_agent/{proposer.py,analyzer.py}` +
`autoloop/autoloop/loop.py` + `autoloop/autoloop/meta_agent/prompts/
{analyze.txt,propose.txt}` + `autoloop/config.yaml` (new `pilot`
block + `lessons.enabled` flag) + `autoloop/tests/test_meta_agent.py`
+ `autoloop/tests/test_cli_smoke.py`. No agent semantic surface
touched. Validation = unit tests + a Y1.5-close mini smoke run
(`uv run python -m autoloop run -n 1 --dry-run`) to confirm the new
prompt shape compiles end-to-end. The downstream success metric is
the S-Y2 Part C `-n 8` PRIMARY-TARGET hit-rate (observed in the
pilot itself, not in this sub-sprint).
```

## 10. Hard fences + non-goals

**S-Y1.5 hard fences (2026-06-09 expanded)** (在 S-Y2 fences 之外补充):

- **No edits** to `autoloop/autoloop/scoring/*.py`（动了会触发
  `gaming.scoring_code_drift.sha_changed`；scoring_code_baseline_sha
  `0d86b08f…` 必须保持稳定）。
- **No edits** to `autoloop/autoloop/sandbox/*.py`（mutable-surface
  sandbox / anti_hardcode / content_validator / applier 都不动）。
- **No edits** to `autoloop/program.md`（locked during M-Auto-1A
  per program.md §8；改 program 需要 program.md v2 的 milestone-level
  authorization；本 sub-sprint **不**触发 v2 流程）。
- **No edits** to `autoloop/config.yaml:mutable_surface` block（mutable
  surface 6 × 4 不动）。
- **No edits** to `autoloop/config.yaml:fitness.baseline_dir`（仍指
  m-auto-7-prepilot-baseline-20260608；S-Y2 之外不动）。
- **No edits** to `eval_interactive/**` / `server/**` / `data/**` /
  `docs/foundational/**` / `docs/runtime_freeze_and_risk_policy.md` /
  `docs/sprints/*` / `docs/milestones/*`。
- **Shadow firewall MUST be preserved**: any new "recent candidate
  results" passthrough MUST aggregate-only-filter shadow suite output
  before serialization into analyzer / proposer prompts; a new unit
  test asserts the filter. 这条是 program.md §3.A.10 + §4 row 4 的精神
  延伸到 candidate-side 反馈通道。
- **`pilot.primary_targets` / `pilot.anti_kill_control` / `pilot.tier2_neighbors`
  MUST NOT include any shadow case_id (NEW, 2026-06-09)**: pilot block
  schema validator (in `loop.py` or a tiny new `config_validator.py`)
  MUST reject any case_id that appears in `eval_interactive/case_specs_shadow/`
  (the check reads only the case_specs_shadow directory listing — file
  names, NOT contents — so it doesn't violate shadow firewall). A
  config-load-time error stops the loop before any iteration runs.
- **`pilot` block ownership (NEW, 2026-06-09)**: deliver-agent maintains
  the block per pilot-style sub-sprint launch; sprint archive must
  snapshot the active block into the handoff `§7.4 Pilot block snapshot`
  section. A pilot that runs without an explicit `config.yaml:pilot`
  block falls back to a sane "no-targets-defined" mode where
  `PILOT_PRIMARY_TARGETS` is empty (proposer behaves as if pre-S-Y1.5
  — current behaviour). This preserves backward compatibility for
  non-targeted future loops.
- **Per-experiment pilot snapshot REQUIRED (NEW, 2026-06-09)**: every
  `experiments.jsonl` row + every `runs/<id>/hypothesis.json` MUST
  embed a `pilot_snapshot` block + `lessons_enabled` flag for that
  iteration's effective config (see §8 S-Y1.5 scope item 5 for schema).
  This is forensic — future audit of exp-N must be able to reconstruct
  the target card the proposer was working from without inferring
  from elsewhere. `loop.py` 在每次 iteration 起始时 compute snapshot +
  hash 一次，写进 record；shadow firewall 适用——snapshot 不含任何
  shadow case_id by construction (per the schema validator above).
- **propose.txt MUST include the labels-only directive (NEW, 2026-06-09)**:
  the literal sentence "The primary target case IDs are evaluation
  bookkeeping labels…" (see §6 P0-C bullet 4th sub-item + §9 stanza)
  MUST appear in propose.txt under "# Forbidden patterns (§1.7
  Constitution — Forbidden)". A unit test asserts the substring is
  present. This is structural defense in depth with anti-hardcode Q4
  detector + propose.txt:36-49 §1.7 mapping.

**Non-goals**（明确不做）:

- **Not changing the 5-layer gate logic**（包括 Layer-0 delta semantics、
  Layer-1 strict no-regression、Layer-3 improvement_min_cases=1）。
- **Not changing the anti-hardcode detector logic / thresholds**。
- **Not changing the lessons compactor logic**（只加 `enabled: false`
  flag；compactor 仍然每 K=10 跑，仍然写 lessons.md，只是 proposer
  在 S-Y2 期间可以 opt-out）。
- **Not opening lessons.md to user-side editing/curating**（编辑生成的
  lesson 不在本次 scope；如果 Option B 之后 lessons 仍然有价值，将来
  可以做 "human-curated lessons" 这种 v2 设计）。
- **Not introducing partial-keep / hill-climbing memory**（program.md §7
  的"每轮从干净 baseline 起步"特性保留；改这个要 program.md v2）。
- **Not promoting `tier2_measurement_contract_change_attempt` 从 ERROR
  到非-ERROR**（renaming + reframing 在 P2 backlog）。
- **Not cleaning up `suspect_baseline_manipulation.git_lookup_failed`
  47/70 noise**（P2 backlog）。
- **Not extending the pilot block to encode "preferred field" steering
  (NEW, 2026-06-09)**: 只 hint phase / use_case；不告诉 proposer "edit
  $.procedure not $.escalation_policy"。理由：field-level steering 太
  接近 §1.7 "if-else rule dump" 红线，而 phase-correct skill +
  PILOT_PRIMARY_TARGETS 列表足以让 LLM 自己选对 field（验证：S-Y2 Part
  C `-n 8` 命中率 ≥ 60% 即可）。如果命中率高但 field 选不对再考虑下
  一轮 P0-D。
- **Not implementing "anti-pattern" memory / negative-example injection
  (NEW, 2026-06-09)**: 即"过去 N 轮做过这个方向都 fail，别再做了"。这
  会让 proposer 看 candidate-side discard pattern，但 v1 design 决定
  proposer 看 baseline+lessons+iters。P0-A + P0-B 已经间接提供这个信
  号（proposer 看见 candidates 引入了 cs11s01 regression）；加 explicit
  anti-pattern memory 是 v2 设计题。

## 11. Risk + compounding-effect analysis

### 11.1 实施 Option B 的风险（2026-06-09 expanded）

| Risk | Severity | Mitigation |
|---|---|---|
| 修了全部 P0 之后 proposer 仍然 0-keep（CS4 task hardness 高于 hill-climbing 能力） | M | 这是 acceptable outcome；sprint_objective.md §5.1.5 已经把 §3.4 fallback 写为 valid alternate outcome；pilot 仍然能 close。新加的成功 metric "target 命中率 ≥ 60%" 让我们能区分"P0-C 没修到"vs"task 本身难"，避免误判 |
| 新加的 "recent candidate results" passthrough 不小心把 shadow per-case detail 漏到 proposer prompt 里（破 shadow firewall） | **H** | (a) 实现时**必须** suite-name == 'shadow' 显式过滤；(b) 加一个 unit test 喂含 shadow per-case content 的 mock results.json，断言 prompt body 不含任何 `cs_case_specs_shadow` / `cs59s` 等签名；(c) Codex review focus question；(d) `pilot` block schema validator 自己也读 `case_specs_shadow/` 目录确认 pilot case_ids 没漏（双层防御） |
| `tier_breakdown` passthrough 体积过大，导致 proposer prompt token 数爆 | L | 1) `tier_breakdown` per layer 是 dict（数字 + UC 名 + case_id），即使 10 个 case 全 fail 也 < 2KB；2) `recent_iterations` 默认 K=5（`config.yaml:lessons.recent_iterations_for_propose`），总共 < 10KB；3) 现有 propose.txt input 总量 < 30K tokens，加 10KB 没问题 |
| Lessons disable 之后 proposer 失去 "context" 反而搜索更乱 | L | 这正是 P1 假设要验证的；run-1 数据已经显示 lessons 在引导错误方向，disable 后即便搜索"乱"也好过"系统性偏错" |
| analyzer 同时读 baseline + recent candidate results 时把两边 case_id 混淆（"this case is failing because it's in baseline OR because candidates regressed it") | M | analyze.txt prompt 显式指示 LLM 分别标 BASELINE_FAILURE / CANDIDATE_INTRODUCED_REGRESSION |
| **`PILOT_PRIMARY_TARGETS` 注入让 proposer 把 case_id 名字塞到 `after_value` (semantic hardcode 红线) (NEW)** | **H** | (a) propose.txt 显式禁止"reference any specific case_id, session_id, or known eval identifier in the after_value"（已有，行 47-49）；(b) anti-hardcode Q4 detector 检测此类 leak（已有）；(c) Codex review focus 看几个 dry-run propose output 验证 rationale 描述 "edit shape" 而不是 "match cs_uc_a_no_ad_id_ad_specific"；(d) 如果发现 leak，回退 PILOT 注入方式到 "use case + phase only, no case_ids in prompt" |
| **`pilot` block 配错（deliver-agent 把 anti_kill_control 漏掉，或把 shadow case_id 写进去）(NEW)** | M | (a) `loop.py` 启动时 validate pilot block；(b) shadow-case-id 检测器 read `case_specs_shadow/` filename listing；(c) anti-kill 缺失时 print loud warning + 拒绝 run |
| **P0-C 修完之后 hit-rate 还是 < 30% (NEW)** | M | -n 8 早停规则：如果 -n 8 跑完命中率 < 30%，stop tranche、记录 evidence、surface P0-D 作为 follow-up R-item，启 §3.4 fallback。不要 -n 16 烧更多 budget。 |

### 11.2 不实施 Option B 的风险（2026-06-09 strengthened）

| Risk | Severity | 2026-06-09 evidence |
|---|---|---|
| S-Y2 pilot 跑 §3.4 fallback 收尾——pilot 仍然 valid，但 "autoloop is ready" 的 evidence 缺失 | **H**（↑） | run-1 已经给出强证据：0/3 on-gap；扩展到 -n 16 期望命中数 ~1.6 |
| Lessons.md 在 0-keep 窗口里继续劣化，L-006 / L-007 可能开始触碰 §1.7 "raw eval phrase encoding" 红线 | **H**（↑） | run-1 rationale 三个直接 cite "L-2026-05-31-004 and L-2026-05-31-005" + L-005 已经鼓励"name the recurring failure scenarios"（接近 §1.7 红线）；新 lessons 会让 cs11s01 over-escalation 模式被固化 |
| 用户对"loop 为什么跑偏"的核心质疑没有 closure，下次启动 autoloop 还得重审同问题 | M | 不变 |
| 后续 sub-sprint（S-B CS2-original）也跑 autoloop 时一样的问题再现 | M（↑） | 任何 future targeted pilot 都受同样的 P0-C 阻塞 |
| **CS4 entity-context guidance 永远没人在 RESOLVE-FAQ skill 上提议过 (NEW)** | M | 即便走 §3.4 fallback，pilot 收尾依然是 hand-authored procedure，autoloop 在该 surface 的能力 unknown |

### 11.3 Compounding effects（顺序约束 — 2026-06-09 updated）

- **修复顺序约束**：P0-A 必须在 P0-B 之前（P0-B 加进来的 candidate
  results 信号会被 prompt builder 丢掉一半信息，除非 P0-A 也修了）；
  P0-A + P0-B 必须在 P1 lessons-disable 之前（disable lessons 才有 P1
  假设可验证空间）；**P0-C 可以独立于 P0-A/P0-B 之前修，但建议一并
  上线**（P0-C steering + P0-A/B 信号是协同的——steer 到 RESOLVE-FAQ 但
  没看见 candidate 引入了 cs11s01 regression 的话，依然搜不动）。
- **shadow firewall 的延伸**是 hardest correctness check：candidate
  results passthrough 必须 explicitly drop shadow per-case content。
  做错了等于打破 program.md §3.A.10——这是 milestone-level 失败。所以
  此项 unit test + Codex review 必须 hard-required。
- **`PILOT_PRIMARY_TARGETS` 注入和 §1.7 "raw eval phrase encoding" 的
  安全边界 (NEW)**: case_id 出现在 prompt 是 OK 的（analyzer 已经在
  `bad_cases_regressing` 字段输出 case_id）；不 OK 的是 case_id 出现在
  proposer 的 `after_value`（也就是会被写进 Skill YAML 的内容）。这条
  红线已经被 anti-hardcode Q4 detector + propose.txt §1.7 mapping 守
  住，但 Codex review 必须独立 spot-check 几个 dry-run output。
- **lessons-disable 在 S-Y2 期间是 RUN-LEVEL flag，不删 lessons.md**：
  保留 forensic（compactor 仍然每 K=10 写新 section，可以观察"lessons
  开关在不同设置下 propose 是否真的不同"）。
- **S-Y2 跑完之后再决定 lessons 永久策略**：可能是 (a) 永久 disable，
  (b) 改 compact.txt 让 compactor 区分 keep/discard 窗口生成不同
  flavor 的 lesson，(c) human curated lessons only。这是 post-M-Auto-7
  设计题。
- **early-stop 规则（NEW 2026-06-09, 4-layer hit-rate; calibrated
  2026-06-09 round-2 — 50% 从"唯一合格线"降为"强通过线"）**：S-Y2 Part C
  `-n 8` 跑完后看 **`full_on_gap_hit_rate`**（4-层全部命中：见 §12
  item 5）。Run-1 false-positive 教训：单看 skill 命中会高估
  （exp-66 命中 resolve_faq 但 field 错了），必须看 full_on_gap。
  Calibration 理由：n=8 样本太小，0%→50% 是阶跃；50% 设成 strong
  pass，25%–50% 是 partial pass（需要更多样本确认），真正的硬早停
  在 < 25%。

  | full_on_gap_hit_rate | Verdict | 下一步 |
  |---|---|---|
  | **≥ 50%（≥4/8）** | **steering strong pass** | 看 keep rate。0 keep → 启 §3.4 fallback（candidate 都触 §4.1 reject）；有 keep → 人类 §4.1 review。不需要扩 -n 16。 |
  | **25%–50%（2–3/8）** | **steering partial pass** | 扩到 -n 16 进一步确认；监控 partial-hit 分布（如果是 `skill_hit_rate ≥ 70%` 但 `field_family_hit_rate < 30%`，说明 proposer 找对 skill 但选错 field——这是 P0-D 的明确信号，open R-item 但不阻塞 S-Y2 close）。扩展后无论结果如何都走 keep-rate 路径决定 §3.4 fallback / §4.1 review。 |
  | **< 25%（≤2/8）** | **steering fail** | **硬早停**。不 escalate -n 16。P0-C 修不到位，open follow-up R-item，启 §3.4 fallback。 |

  Run-1 baseline (n=3) 的 `full_on_gap_hit_rate = 0/3 = 0%` 落在 fail
  区间——所以"修了 P0-C 后跳到 partial pass 区间（≥ 25%）" 才是 S-Y1.5
  生效的最低门槛，而不是直接跳到 50%。这条规则把 "目标命中率" 升为
  second-class success metric，并把它和 keep rate 解耦——让 S-Y2 能区分
  "steering 有效但 task 太难" vs "steering 找对 skill 但选错 field" vs
  "steering 完全没生效"。

## 12. Observability / trace / report implications

如果走 Option B：

1. **`autoloop audit --experiment exp-N` 输出需要扩展**：现在 audit 只
   显示 hypothesis + verdict + gaming_flags + sandbox_verdict；建议加
   一节 "proposer_prompt_inputs"，dump 当时实际喂给 proposer 的
   `recent_iterations` block（含新加的 tier_breakdown），让人类能眼
   看出 proposer "看到的失败地形是什么"。

2. **`lessons.md` 加一行 metadata**: 当 `lessons.enabled: false` 时，
   compactor 仍然 append lesson，但在 front matter 加
   `consumed_by_proposer: false`——让下次 audit 能区分 "这是
   forensic-only 的 lesson"。

3. **新 metric：proposer search direction diversity**：可以 post-hoc
   分析 N 轮 propose 的 `(target_skill, target_field, rationale 关键
   词)`，估计搜索方向的 entropy。0-keep 窗口下 entropy 下降是预警
   信号。这是个 audit 工具改进，不阻塞 S-Y1.5。

4. **Eval report 链路**: M-Auto-7 close 的 milestone-shared re-bless
   会产 `m-auto-7-close-baseline-<date>/`；新的 `eval-results.json`
   per-iter trace（loop.py:_persist_eval_traces）继续工作；S-Y1.5 不动
   trace persistence path。

5. **NEW (2026-06-09) — 4-layer PRIMARY TARGET hit-rate decomposition
   (post-Codex/human review补充)**:
   S-Y1.5 应在 `autoloop report` / `audit` 输出中加 **4 个独立 metric +
   1 个 composite**：

   | Metric | 计算 | 作用 / Run-1 baseline |
   |---|---|---|
   | `phase_usecase_hit_rate` | candidate 的 target_skill `applicable_phases ∩ pilot.phase_hint ≠ ∅` AND `applicable_use_cases ∩ (pilot.use_case_hint ∪ {"*"}) ≠ ∅` 的比例 | "有没有跑到正确业务路径"；run-1 = 3/3 (discover_triage / confirm / resolve_faq 都覆盖 UC-A or "*") |
   | `skill_hit_rate` | candidate 的 target_skill ∈ {`resolve_faq_grounded_answer.yaml`} 的比例 | "有没有接近 CS4 主路径"；run-1 = 1/3（只有 exp-66） |
   | `field_family_hit_rate` | candidate 的 target_field ∈ {`$.procedure`, `$.grounding_instruction`, `$.critical_steps[*].desc`}（**不含 `$.escalation_policy`**）的比例 | "有没有选对修改表面"；run-1 = 0/3（全是 escalation_policy） |
   | `full_on_gap_hit_rate` | 三者**同时**满足的比例（**primary success metric**） | run-1 = 0/3 = 0%。S-Y1.5 后 -n 8 verdict 三段（详见 §11.3）：≥ 50% = strong pass；25–50% = partial pass（扩 -n 16 进一步确认）；< 25% = fail（硬早停）。 |
   | `partial_hit_breakdown` | 输出三个 hit 的 venn-style 计数（e.g. "skill-only: 1; field-only: 0; phase_usecase-only: 2; full: 0"） | 区分 "proposer 找对 skill 但选错 field" vs "完全没生效" |

   实现：~50 行 audit 输出扩展 + 用 6 个 Skill YAML 的
   `applicable_phases` / `applicable_use_cases` 元数据做交集判断
   （SKILL_PHASE_USECASE_MAP 已经在 S-Y1.5 P0-C scope 里加进
   propose.txt，复用同一数据源）；不动 keep/discard gate；不动
   baseline。
   **Design choice rationale**: field-level steering 不写进 prompt
   （避免太像手把手教模型答案，靠近 §1.7 "if-else rule dump" 红线），
   但 audit metric 必须看 field-level——否则会出现 run-1 这种 false
   positive（"看起来命中了 resolve_faq，其实又在改 escalation_policy"）。
   这是 "steer broadly via prompt, measure narrowly via audit" 的明确
   分离。

6. **NEW (2026-06-09) — lessons feedback-loop monitor**: 每轮 `audit`
   输出"该轮 rationale 是否 cite 了 lessons L-NNN" 的 boolean 标记。
   `lessons.enabled: false` 时此标记应该 100% false；如果 != false
   说明 prompt builder 漏改。一个被动监测器。

## 13. Sources of truth used in this audit (2026-06-09 update)

每条 claim 都引用 HEAD-verified 路径（autoloop/exp-67 HEAD = `8b529ce`
+ 2026-06-09 run-1 validation tranche logs / experiments.jsonl）：

- `autoloop/program.md` v1 §1–§8（locked contract）
- `autoloop/config.yaml`（mutable_surface / paths / lessons / meta_agent /
  fitness / content_validator / anti_hardcode / gaming blocks）
- `autoloop/autoloop/loop.py`（14-step orchestrator, 939 lines）
- `autoloop/autoloop/meta_agent/analyzer.py`（237 lines）
- `autoloop/autoloop/meta_agent/proposer.py`（298 lines）
- `autoloop/autoloop/meta_agent/lessons_compactor.py`
- `autoloop/autoloop/meta_agent/prompts/{analyze,propose,compact}.txt`
- `autoloop/autoloop/scoring/tier_evaluator.py`（5-layer lexicographic）
- `autoloop/autoloop/scoring/baseline_loader.py`（aggregated baseline）
- `autoloop/autoloop/scoring/gaming.py`（7 anti-gaming checks）
- `autoloop/autoloop/sandbox/{yaml_diff_validator,anti_hardcode_check,
  content_validator,applier}.py`
- `autoloop/results/experiments.jsonl`（70 logged iterations as of run-1 close 2026-06-09; exp-67 + exp-68 timestamps `2026-06-08T15:08:09+00:00` and `2026-06-08T16:39:29+00:00`）
- `autoloop/results/lessons.md`（L-001..L-005, all authored 2026-05-31 — pre-dates CS4 bad_cases by 8 days）
- `autoloop/results/runs/exp-{63..68}/`（per-iter artefacts; exp-67/68 ran on per-exp backends :63234 / :62711）
- `autoloop/results/spring-boot-{63234,63951,62711}.log`（run-1 per-exp backend stdout — confirm preflight + auto-reboot worked + per-exp port distinct）
- `server/src/main/resources/skills/{resolve_faq_grounded_answer,discover_triage,confirm,escalate,resolve_intake_collect_and_handover,terminal}.yaml`（HEAD-verified `applicable_phases` / `applicable_use_cases` blocks driving §5.8 phase-correctness diagnostic）
- `docs/milestone_objective.md`（M-Auto-7 ACTIVE）
- `docs/sprint_objective.md`（S-Y2 / Sprint 087 / S-Auto-32 PLACEHOLDER）
- `docs/current/iteration_governance.md` §1 / §3.2 / §4 / §5.6 / §5.8 / §7
- `eval_interactive/results/m-auto-7-prepilot-baseline-20260608/
  {bad_cases,anchor_outcome,shadow}/aggregated.json` (n=9, git_commit
  `92c4076`, primary_model `deepseek-v4-flash`)

## 14. Open questions for deliver-agent (2026-06-09 update)

1. **是否同意插 S-Y1.5 micro sub-sprint？** Research agent 推荐 yes；
   最终决定权在 deliver-agent + human。Run-1 数据让"插 vs 不插"的
   trade-off 不再对称——不插的话 S-Y2 期望命中率 < 20%（基于 0/3 + 类
   似先验）。如果 no，请记录 disposition 到 `action_bank.md` §5 作为
   R-item「R-autoloop-feedback-loop-thinness」 的存档，并明确接受 §3.4
   fallback 作为 S-Y2 PRIMARY outcome。
2. **S-Y1.5 是否 §7-EXEMPT？** Research agent 2026-06-09 update 推荐
   §7 REQUIRED（不再 EXEMPT，因为 P0-C 引入 `pilot` block 这一新表
   面，需要 stanza 明确"不是 semantic hardcode"——见 §9 stanza draft）。
3. **lessons.enabled: false 在 S-Y2 默认值是什么？** Research agent 推
   荐 default **false** for S-Y2（May-31 lessons 早于 CS4 baseline，
   run-1 直接证据显示它们 driving the wrong direction）。如果
   deliver-agent 想做 A/B 对比，建议改在 S-Y1.5 内 -n 1 + -n 1 各跑
   一次（4 h 总投入），结果记录到 sprint handoff 作为后续 lessons-
   policy 决策依据。
4. **`tier2_measurement_contract_change_attempt` 是否在 S-Y1.5 里
   一并 reframe？** Research agent 仍然推荐 backlog（不增加 sub-sprint
   scope 蔓延）。
5. **S-Y1.5 跑完后 lesson L-006（应是 exp-61..70 的 compactor 输出）
   是否要预防性清理？(NEW, 2026-06-09)** L-006 还没生成（compactor 触
   发条件是 total % K == 0，目前 total = 70，下一次触发是 80）。如果
   S-Y1.5 期间 lessons.enabled toggle 让 compactor 仍然运行，那 L-006
   会被生成进 lessons.md。建议在 S-Y1.5 close handoff 中记录"L-006 的
   `consumed_by_proposer` metadata = false（因 S-Y2 default-disable
   lessons）"，避免日后误把它当 active heuristic。
6. **`pilot` block 的 schema 是否需要 versioning？(NEW, 2026-06-09)**
   pilot block 是 S-Y1.5 引入的新 config 表面，未来 pilot 可能想加 field
   （e.g. preferred_field / negative_examples）。建议加 `pilot.schema_version: 1`
   字段，让 loop.py validator 在 schema mismatch 时给清晰错误。

---

End of proposal (2026-06-09 2nd-pass update). Awaiting human +
deliver-agent decision on Option A / B / C. Research agent's
recommendation remains **Option B**, with scope expanded to 5-fix
(P0-A / P0-B / P0-C / P1 + per-experiment pilot snapshot embedding)
+ 4-layer hit-rate decomposition + S-Y1.5-close human checklist +
labels-only propose.txt directive. Run-1 validation tranche data
(exp-66/67/68; 0/3 on-gap; 2/3 phase-incorrect) confirmed the
original P0-A/P0-B diagnosis and forced P0-C from P2-backlog to
P0-blocking; 2nd-pass human review then surfaced the 4 supplements
(hit-rate decomposition / per-exp pilot snapshot / dry-run
checklist / labels-only directive) that tighten the patch without
expanding scope materially (~30 LOC + 1 prompt sentence + 1
dry-run review pass added on top of the original P0-A+B+C+P1
~250 LOC budget). No code changes have been made by the research
agent.
