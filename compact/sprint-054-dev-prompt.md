# Dev prompt — Sprint 54 / M-Auto-1A S-Auto-1 — Mutable-surface contract + YAML diff sandbox

你是 **dev agent for Sprint 54 / M-Auto-1A S-Auto-1**。本次任务：搭建 `autoloop/` 子系统的目录骨架 + 写定人类可读的 `program.md` 契约 + 实现 YAML-diff 沙箱（白名单结构性拒绝任何 meta-agent 越界的 Skill YAML 修改）+ 充分覆盖的 pytest。**Zero touch** to `server/`, `eval/`, `eval_interactive/`, `data/`, `db/`, 任何 Skill YAML, 任何 case_spec —— S-Auto-1 ship 的全是新代码在 `autoloop/` 下。

## Read order (最小化)

只读两个：

1. `AGENTS.md`（auto-loaded via constitution chain — 你已经有了）
2. 本 prompt（self-contained sub-sprint contract，下方完整内嵌）

**不需要**读 `docs/sprint_objective.md` 或 `docs/milestone_objective.md`——本 prompt 是 self-contained executable view（与 source-of-truth `docs/sprint_objective.md` 同步生成）。

需要查看具体 code anchors 时引用以下路径：
- `server/src/main/resources/skills/*.yaml` — 6 个 Skill YAML（白名单文件路径源；S-Auto-1 不修改，只 reference 路径）
- `docs/solutions/auto_evolution_skill_driven_v1.md` — 仅在你需要确认 v1 mutable surface 设计意图时查阅 §3.1（人类已 lock 收紧版）

## Embedded sub-sprint contract

### Class

`infra` (§3.2 default — 无 Java 语义决策改动；无 projection 改动；无 eval-scoring 改动)。**§7 REQUIRED** —— S-Auto-1 定义 auto-evolution 能力的 mutable-surface CONTRACT，是后续 §1.7 结构性强制的长期 anchor；按 §7 规定，即便是纯 infra 工作，只要是后续语义改动的 *基础*，也必须带 stanza 以保证契约可审。

### Goal

搭起 `autoloop/` 子系统骨架 + 实现 YAML-diff 沙箱，强制 M-Auto-1A 的两条 §6 hard fence：
- **fence #9 — no cross-file diff**：meta-agent 每次 propose 只能改一个 Skill YAML 的允许字段；不允许跨 Skill / 跨文件 diff。
- **mutable surface 收紧版**：仅允许 6 个 Skill YAML × 4 类 LLM-soft 字段 (`procedure`, `grounding_instruction`, `escalation_policy`, `critical_steps[*].desc`) 的修改；任何对结构性字段 (`applicable_use_cases`, `tools_required`, `required_context_keys`, `max_tool_steps`, `allow_interim_message`, `valid_terminal_outcomes`, `name`, `description`, `objective`, `applicable_phases`)、guardrails、state_inheritance、critical_step 元数据 (`id`, `trace_check`, `mandatory_for`, `severity`) 的修改均被结构性拒绝。

S-Auto-1 close 时，沙箱能对任意输入 diff 答 ACCEPT/REJECT + 精确 reason，正负面 fixture 全覆盖。沙箱在 S-Auto-3 被 loop orchestrator 消费；没有它，下游 sub-sprint 不能安全 apply meta-agent proposal。

`autoloop/program.md` 是 **人类可读契约**——未来 deliver-agent / Codex / 审 kept iteration 的人类 reference 这个文件来知道 auto-loop 能/不能做什么。

### Scope (#1-#6 编号；这是 contract)

#### #1 — `autoloop/` 目录骨架

在 repo root 下创建：

```
autoloop/
  pyproject.toml                  # uv-managed Python 包；只依赖 PyYAML + pytest
  README.md                       # 一页 synopsis + CLI 用法表
  program.md                      # LOCKED 人类契约 (见 #2)
  config.yaml                     # runtime config
  autoloop/
    __init__.py
    cli.py                        # subcommand router: check/dry-run/run/report/apply/audit
    sandbox/
      __init__.py
      yaml_diff_validator.py      # 本 sub-sprint 主要 deliverable
      applier.py                  # skeleton only (S-Auto-3 填 git commit + branch 逻辑)
  tests/
    __init__.py
    test_yaml_diff_validator.py
    test_cli_smoke.py             # 最小：每个 subcommand 存在且 print --help
    fixtures/
      valid_diffs/                # 4 个 positive
        procedure_edit.diff
        grounding_instruction_edit.diff
        escalation_policy_edit.diff
        critical_steps_desc_edit.diff
      invalid_diffs/              # 12+ 个 negative
        structural_field_applicable_use_cases.diff
        structural_field_tools_required.diff
        trace_check_edit.diff
        mandatory_for_edit.diff
        severity_edit.diff
        critical_step_id_edit.diff
        guardrails_edit.diff
        state_inheritance_edit.diff
        cross_skill_edit.diff
        cross_file_edit_skill_plus_config.diff
        new_skill_file.diff
        deleted_skill_field.diff
        malformed_yaml.diff
        required_context_keys_edit.diff
```

`pyproject.toml` 声明 package + pytest + PyYAML（S-Auto-1 无其他 deps；tier_evaluator / eval-runner deps 在 S-Auto-2 加）。用 `uv` 按现有 repo 惯例（`uv pip install -e .` in `autoloop/`）。

`autoloop/config.yaml` 初始内容：
- `mutable_surface.allowed_field_paths` — 4 个 LLM-soft field path patterns (e.g. `$.procedure`, `$.grounding_instruction`, `$.escalation_policy`, `$.critical_steps[*].desc`)
- `mutable_surface.allowed_skill_files` — 6 个 Skill YAML 路径（discover_triage / resolve_faq_grounded_answer / resolve_intake_collect_and_handover / confirm / escalate / terminal）
- `paths.experiments_log` / `paths.iterations_index` / `paths.lessons_log` / `paths.runs_dir` — defaults under `autoloop/results/`（S-Auto-3 才会真用）
- `lessons.compaction_window_k` — default 10（S-Auto-3 才会读）

#### #2 — `autoloop/program.md` 人类契约

按以下结构编写：

1. **Purpose** — 一段：auto-loop 是 Skill-driven hill-climbing meta-agent，受 M-Auto-1A mutable surface 约束；输出是 CANDIDATES 需人评后 merge to main
2. **Mutable surface (verbatim)** — 6 Skill YAMLs × {`procedure`, `grounding_instruction`, `escalation_policy`, `critical_steps[].desc`}。其余全部锁定。
3. **Hard fences (verbatim copy from M-Auto-1A §6 + proposal §8)** — 12 milestone-level + 8 proposal-level，编号便于 Codex/human 引用 "fence #N"。从 `docs/milestone_objective.md` §6 复制 + 从 `docs/solutions/auto_evolution_skill_driven_v1.md` §8 复制
4. **Forbidden by construction (结构性防御)**:
   - sandbox 白名单 (本 sub-sprint)
   - 4-tier lexicographic fitness (S-Auto-2)
   - anti-hardcode auto-check (S-Auto-4)
   - shadow regression gate (built into lexicographic verdict)
   - shadow result firewall (aggregate yes/no only to meta-agent)
   - no main-branch cherry-pick by the loop itself (human via `python -m autoloop apply`)
5. **§1.7 forbidden-list mapping** — 每个 §1.7 item → 哪一道结构性防御 catches it (从 `docs/solutions/auto_evolution_skill_driven_v1.md` Appendix D 复制表)
6. **What the loop is NOT allowed to do** even if propose 看起来很棒：改 tools_required / 改 trace_check / 编辑任何 case_spec / 编辑任何 Java / 同一 diff 编辑多个 Skill / 直接 main 分支写
7. **What human gives up** by running loop：loop 有时会 propose 人类本来就要 propose 的 changes（这没事）；loop 永远不会 propose 人类被禁止做的 changes（locked surface）
8. **Versioning** — `program.md` v1 locked；未来 Stage-2 解锁 (e.g. templates.yaml) 要求新 milestone + 新 `program.md` v2 + 明确人类授权

**此文件在 M-Auto-1A 执行期间 immutable**——一旦 commit，任何 change 需要新的 sub-sprint 授权。

#### #3 — `autoloop/sandbox/yaml_diff_validator.py` — 主要 deliverable

API：

```python
def validate_skill_yaml_diff(
    before_yaml: str,
    after_yaml: str,
    file_path: str,
) -> ValidationResult: ...

@dataclass
class ValidationResult:
    decision: Literal["ACCEPT", "REJECT"]
    reason: str                              # 一行人类可读
    rejected_paths: list[str]                # 违反白名单的 YAML AST paths (ACCEPT 时空)
    accepted_paths: list[str]                # 被修改且通过白名单的 paths (仅 ACCEPT 时)
    file_path: str                           # 回显便于 audit
```

算法：

1. **File-path check**: `file_path` MUST 属于 `config.mutable_surface.allowed_skill_files` (6 个 Skill YAML)。否则 REJECT (reason: "file outside mutable surface")
2. **YAML parse**: 用 `yaml.safe_load` 解析 `before_yaml` + `after_yaml`。`after_yaml` parse failure → REJECT (reason: "malformed YAML in proposed edit")。`before_yaml` parse failure → REJECT (reason: "baseline YAML is malformed — refusing to compare"；视为 evaluator-side error, not meta-agent fault)
3. **AST diff**: 计算 before vs after 之间发生变化的 YAML node path 集。Path 记法：`$.procedure`, `$.critical_steps[0].desc`, `$.applicable_use_cases[2]`。递归遍历 parsed dict/list 实现（无外部 dep）
4. **白名单匹配**: 每个 changed path，检查匹配某条 `config.mutable_surface.allowed_field_paths` pattern。Wildcards: `[*]` 匹配任意 list index。所以 `$.critical_steps[*].desc` 匹配 `$.critical_steps[0].desc`, `$.critical_steps[3].desc` 等。`$.critical_steps[0].trace_check` **不**匹配 → REJECT
5. 任一 path 被拒 → REJECT (reason: "modification to non-mutable field(s): <paths>")
6. 全部 path 在白名单内 → ACCEPT

**多文件 / 跨文件 diff 在 caller 层处理**——caller (S-Auto-3 loop) 必须 per file invoke `validate_skill_yaml_diff` ONCE；如果 bundle 包含 >1 file，caller 直接 REJECT 不调用 validator。`applier.py` skeleton 文档化此 contract。

**Edge cases 实现必须处理**：
- 新增 `critical_steps` 条目 — REJECT（同时改变 `$.critical_steps[N]` 结构 AND 新增 `$.critical_steps[N].id|trace_check|mandatory_for|severity|desc`）
- 删除 `critical_steps` 条目 — REJECT（同理）
- 修改 `$.critical_steps[N].desc` BUT 同时不小心给 `$.critical_steps[N].mandatory_for` 加了空格（YAML re-serialization 副作用）— REJECT；实现必须 AST-diff 语义内容，不是 byte 内容
- 重排列表条目但内容相同 — REJECT（treat reorder as 结构改动；meta-agent 永远不需要 reorder）
- YAML anchors / refs (`&anchor`, `*ref`) — REJECT 任何添加 / 删除 / 重新放置 anchor 的 diff；只直接 value 改 whitelisted fields 才 accept
- Comment changes — neutral（PyYAML `safe_load` 丢 comment；comment-only diff 在 AST 层变成 NO-OP → REJECT with reason "no whitelisted field changed"）

#### #4 — `autoloop/cli.py` subcommand 骨架

实现 `autoloop/cli.py` with 以下 subcommands：

- `check` — 校验 `config.yaml`（paths 存在、allowed Skill files 磁盘上存在）+ run self-test 跑一个 positive + 一个 negative sandbox fixture 确认期望输出。给人类确认 install 正确用
- `dry-run` — S-Auto-1 placeholder；print "S-Auto-3 territory" + exit 1
- `run` — 同 placeholder
- `report` — 同 placeholder
- `apply --experiment <id>` — 同 placeholder
- `audit --experiment <id>` — 同 placeholder

每个 subcommand 通过 `python -m autoloop <subcommand>` 暴露（entry point in `pyproject.toml`）。

实现选型自由：`argparse`（stdlib）或 `click`（如果你想加 dep）。建议 stdlib `argparse` 避免额外 dep。

#### #5 — 测试

`autoloop/tests/test_yaml_diff_validator.py`:
- **Positive (4)**: 一个 fixture 对应一个允许 field 类型，每个只改该字段，assert `decision == "ACCEPT"`
- **Negative (12+)**: 每个 `fixtures/invalid_diffs/` fixture, assert `decision == "REJECT"` 且 `reason` 包含期望 substring (例如 "applicable_use_cases" for structural-field test)
- **Edge cases**: 至少 3 个 of (新增 critical_step / 删 critical_step / reorder list / YAML anchor 引入 / comment-only diff) — assert REJECT
- **Cross-Skill diff**: caller responsibility；文档化在 test docstring + 一个 `test_cross_file_caller_rejects` test 模拟 caller per file invoke `validate_skill_yaml_diff` and 确认至少一个返回 REJECT

`autoloop/tests/test_cli_smoke.py`:
- 每个 subcommand `python -m autoloop <name> --help` exit 0 且 print help
- `python -m autoloop check` against known-good config exit 0；against config with non-existent skill path exit 1

#### #6 — 文档收尾

- `autoloop/README.md`: 一页 — 啥是 auto-loop (3 句)、CLI synopsis (subcommand 表)、指向 `program.md` 看完整 contract、指向 `docs/milestone_objective.md` 看 milestone scope、指向 `docs/solutions/auto_evolution_skill_driven_v1.md` 看设计 rationale
- `pyproject.toml`: `[project]` with `name = "csagent-autoloop"`, `version = "0.1.0"`, `dependencies = ["PyYAML>=6.0"]`, `[project.scripts]` 暴露 `autoloop = "autoloop.cli:main"`

### Hard fences / STOP conditions (do NOT do)

- **No touch** to `server/`, `eval/`, `eval_interactive/`, `data/`, `db/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**` (包括不动 `iteration_governance.md`), `docs/sprints/*` archives, `docs/milestones/*` archives
- **No touch** to any file under `server/src/main/resources/{skills,prompts,scripts,config,mock}/**`。沙箱 VALIDATES diffs against Skill YAMLs but never writes to them
- **No editing** of `docs/codex-findings.md` 在执行期 (it is scaffold; deliver-agent + Codex 在 milestone close 写)
- **No new heavy dependencies** in `pyproject.toml`。PyYAML + pytest 是仅有 S-Auto-1 deps。其他 (e.g. PyYAML alternative, AST 库) 需 deliver-agent + human approval BEFORE adding
- **No live LLM call** anywhere in S-Auto-1 code。Meta-agent LLM 在 S-Auto-3 接；S-Auto-1 只 validate 合成 fixture diffs
- **No subprocess invocation** of `mvn` / `spring-boot` / `eval-interactive` / `uv run` from S-Auto-1 code。S-Auto-2 / S-Auto-3 才会有
- **No `git add -A`** 当 stage commit。Stage only 本 sub-sprint 创建的 files (`autoloop/**` paths enumerated above + `docs/sprints/sprint-054-handoff.md` at close)
- **STOP and surface** to deliver-agent 如果 YAML AST diff 实现 reveals 生产 Skill files 中有歧义 (e.g. 某 Skill YAML 用了不 round-trip 过 `safe_load → re-serialize` 的 YAML feature)。不要 silently work around；milestone §10 stop condition #1 covers this
- **STOP and surface** 如果 white-list field-path patterns 能 cleanly 表达但歧义匹配到意外字段 (e.g. 某 Skill 顶层 `procedure` 字段 AND 嵌套 `procedure`)。文档化 case；deliver-agent + human 在写 validator 前 disambiguate

### Test / eval requirements

- **Python**: `cd autoloop && uv run pytest -q` — 所有 `autoloop/tests/` tests PASS。期望新 test count ~20-30 (12+ negative fixtures + 4 positive + edge cases + CLI smoke)
- **Existing Python suite unchanged**: `cd eval_interactive && uv run python -m pytest --tb=no -q` 复现 `486 passed, 3 failed`（不动 eval_interactive）
- **Java baseline unchanged**: `mvn test -B` from repo root 复现 `1183 / 1-inherited / 0 / 2`（不动 server/eval Java）。如果 dev 通过 `git diff --stat | grep -E '(server|eval)/src/main/java'` 返回空确认 Java 零编辑，可 skip Java suite check
- **No eval invocation** in S-Auto-1。无 `eval-interactive run` 调用

### §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (§3.2 default — 纯 infra, 无语义决策改动, 无 projection 改动, 无 scoring 改动)。S-Auto-1 是后续 §1.7 强制的结构性 anchor；它本身不做语义决策。

**Tier-0 invariant:** adds no Tier-0 invariant。沙箱强制 M-Auto-1A mutable-surface contract, 这是 milestone-scoped boundary, 不是 `docs/runtime_freeze_and_risk_policy.md` §1/§2 意义上的 Tier-0 runtime invariant。C2/C3 DEFER 不变 per M2-close verdict。

**Semantic hardcode:** No semantic hardcode introduced。沙箱白名单匹配 REGEX-FREE (用 YAML AST path 比较, 非 text matching)。白名单本身是小 literal set: 4 YAML paths × 6 file paths, 是 registry / contract (人类 lock 的 mutable surface from proposal §3.1), 不是语义决策。如果 dev 发现白名单不能不用 text matching / fuzzy logic 表达, STOP and surface — 这信号 YAML structure 有 gotcha 需另设计。

**Generalization coverage:** target = 沙箱正确 accept 4 positive fixture diffs (一个 per allowed field type) 且 reject 12+ negative fixture diffs (structural field / trace_check / mandatory_for / severity / id / guardrails / state_inheritance / cross-Skill / cross-file / new file / deleted field / malformed YAML)。Neighbor = 沙箱正确处理 edge cases (anchor 引入 / reorder / comment-only diff)。Negative = 沙箱正确 reject 手工 adversarial diff 看着像有效 `procedure` edit 但 smuggle 一个 `trace_check` change (REJECT)。Shadow = N/A for S-Auto-1 (shadow 在 S-Auto-2 tier_evaluator exercise, 不在沙箱)。

### Codex review plan (§4.3)

**Default**: milestone-shared at M-Auto-1A close。S-Auto-1 是 `infra` + 不跨 §1.7 (它结构性 ENFORCE §1.7 而非 cross)。无 per-sub-sprint Codex trigger 触发。

Deliver-agent 在 S-Auto-1 close **不**dispatch Codex。Codex review 在 M-Auto-1A close 时连同 S-Auto-2/3/4 一起消费 S-Auto-1 commit range (cumulative milestone range)。

### Handoff requirements

- Author `docs/sprints/sprint-054-handoff.md` at S-Auto-1 close；**§12** 留空 (deliver-agent + human at milestone close)
- Record in handoff: `git show --numstat` for S-Auto-1 commit；`autoloop/tests/` test count + pass status；`autoloop/program.md` 最终内容 (或指针 + 任何对本 sub-sprint #2 outline 偏差的 summary)；任何 STOP-surfaced 的 YAML AST diff 实现歧义 (if applicable)；§7 self-walk 确认 `infra` 分类 + no Tier-0 + no semantic hardcode
- Document in handoff §"OQ surfaced": 任何 unresolved-but-non-blocking 问题 dev 想让 deliver-agent 在 S-Auto-2 开始前决定 (e.g. AST-diff 库选择如果 PyYAML stdlib 不够；baseline_loader.py contract preview)

### Commit discipline

Dev stages **only S-Auto-1 scope**：所有 #1 directory tree 下的 new files under `autoloop/**` + `docs/sprints/sprint-054-handoff.md`。**No `git add -A`** — deliver-agent close-bundle files (`docs/milestone_objective.md` updates, `docs/10-handoff.md` refresh, `docs/action_bank.md` updates) 在 sub-sprint or milestone close 由 human bundle。

一次 commit at sub-sprint close (commit-at-end pattern)。Commit message 格式：`Sprint 54 / S-Auto-1 — autoloop subsystem scaffold + YAML diff sandbox` 接一段简短 summary 6 个 scope items + 新 test count。

## Self-check checklist

完成前 dev 必须勾选：

- [ ] `autoloop/` directory 创建完整（pyproject.toml + README.md + program.md + config.yaml + 完整 package tree per #1）
- [ ] `autoloop/program.md` 8 section 全部到位；hard fences 是 verbatim 复制（非摘要）
- [ ] `validate_skill_yaml_diff` API 实现完整，包含所有 #3 边缘 case 处理
- [ ] 4 个 positive fixtures + 12+ negative fixtures 全到位
- [ ] `pytest` 全 PASS；新 test count 20-30 范围
- [ ] `cd eval_interactive && uv run python -m pytest --tb=no -q` 复现 `486 passed, 3 failed`
- [ ] `git diff --stat` 确认零 lines under `server/`, `eval/`, `eval_interactive/eval_interactive/`, `data/`, `db/`, `server/src/main/resources/` (autoloop/ 外其他地方只能是 `docs/sprints/sprint-054-handoff.md`)
- [ ] `python -m autoloop check` 在 known-good config 下 exit 0；改 config 让某 skill 路径不存在后 exit 1
- [ ] `python -m autoloop dry-run --help` (and 其他 placeholders) 都 print help 且不 crash
- [ ] `docs/sprints/sprint-054-handoff.md` 完整 (per Handoff requirements 上方)；§12 留空
- [ ] §7 self-walk 在 handoff 中记录 (Target layer / Tier-0 / Semantic hardcode / Generalization 四字段)
- [ ] 单次 commit 包含 `autoloop/**` + handoff；commit message 按格式
- [ ] 任何 STOP-surfaced 项写在 handoff §"OQ surfaced"
