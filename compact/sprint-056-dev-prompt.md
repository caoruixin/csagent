# Dev prompt — Sprint 56 / M-Auto-1A S-Auto-3 — Loop orchestrator + meta-agent + 3-layer memory

你是 **dev agent for Sprint 56 / M-Auto-1A S-Auto-3**。本次任务：接通完整 auto-evolution 循环 —— `loop.py` orchestrator + `meta_agent/*` (analyzer + proposer + lessons_compactor + 3 个 prompts) + `memory/*` (raw JSONL + sqlite index + lessons markdown) + `applier.py` 真实实现（git branch + Spring port mgmt + health-probe）+ `cli.py` 真实 wiring（5 个 subcommand 全接通，apply 走 OQ-S55.1 Hybrid）+ `config.yaml` 加 `meta_agent:` + `lessons:` block + `program.md` 3 行 status flip。**Zero touch** to S-Auto-1/2 deliverable signatures、`eval_interactive/eval_interactive/`、case_spec、`server/`、`eval/` Java、governance docs。Close gates：(a) `dry-run --experiments 1` 完整跑通；(b) `run --experiments 1` 完整 live iteration 走通（不论 keep/discard，FULL pipeline must execute without crash）。S-Auto-4 anti-hardcode kernel 是后续 sub-sprint，本 sprint ships **placeholder always-PASS hook** + 签名定型。

## Read order (最小化)

只读两个：

1. `AGENTS.md`（auto-loaded via constitution chain）
2. 本 prompt（self-contained sub-sprint contract，下方完整内嵌）

需要查看具体 code anchors 时引用：
- `autoloop/autoloop/sandbox/yaml_diff_validator.py` (S-Auto-1) — sandbox API 已就绪；loop 调用 + applier sanity check 调用
- `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader}.py` (S-Auto-2) — fitness API 已就绪；loop 调用；eval_runner 通过 env-var `CSAGENT_BACKEND_URL` 接收 alt-port 注入（**不**改 eval_runner signature）
- `autoloop/autoloop/sandbox/applier.py` (S-Auto-1 skeleton) — 本 sprint 真实化
- `autoloop/autoloop/cli.py` (S-Auto-1 placeholder subcommands) — 本 sprint 真实化全部 subcommand
- `autoloop/program.md` (S-Auto-1 v1 LOCKED + S-Auto-2 row 2 status flip) — 本 sprint 改 3 行 status（row 3 + row 6；row 1/2/4/5 已有正确 status）
- `autoloop/config.yaml` (S-Auto-1 + S-Auto-2 sections) — 本 sprint append `meta_agent:` + `lessons:` block
- 若需要确认 eval_interactive 的 backend URL 读取机制：`grep -rln "backend\|base_url\|BASE_URL\|CSAGENT" eval_interactive/eval_interactive/` —— 如果它不从 env-var 读 backend URL 而是只从 `eval_interactive/.env` 读，STOP and surface（见 Hard fences）

## Embedded sub-sprint contract

### Class

`infra` (§3.2 default — orchestration, persistence, port mgmt, subprocess lifecycle; no semantic decision change, no projection change, no scoring change)。**§7 REQUIRED** —— S-Auto-3 接通 meta-agent → applier → eval → tier_evaluator 闭环，是 §1.7 enforcement 链最后一环。

### Goal

S-Auto-3 close 时：

1. **Dry-run end-to-end**: `python -m autoloop run --experiments 1 --dry-run` 完整跑：meta-agent propose 出 hypothesis → sandbox validate → anti-hardcode placeholder PASS → 输出 proposed diff + verdict skeleton；不 apply / 不 mvn / 不 Spring / 不真 eval / 不写长期 memory（dry-run 只写一个 dedicated dry-run log）
2. **Live iteration end-to-end**: `python -m autoloop run --experiments 1`（无 dry-run）：meta-agent propose → sandbox validate → anti-hardcode placeholder PASS → applier.apply（git commit on `autoloop/exp-1` branch；写 Skill YAML edit；Spring 在 alt port 起 + health-probe）→ eval_runner 跑 3 v1 suite（against alt port backend via env-var）→ tier_evaluator.evaluate → memory 落 3 层（raw JSONL + sqlite index + lessons.md trigger if iter_count % K == 0）。**Regardless of keep/discard verdict, FULL pipeline must execute without crash** —— close gate 是 pipeline 走通，不是产生 keep
3. **Anti-hardcode hook 签名定型**: `anti_hardcode_check` 接口签名定 + placeholder always-PASS impl + 集成点在 loop.py propose-stage；S-Auto-4 实现时只换 impl，不改签名
4. **`autoloop apply --experiment exp-N` Hybrid**: cherry-pick exp-N branch + 输出 proposed `config.yaml` baseline_dir patch + 写 patch 文件 + **不**自动 commit；human reviews + commits

### Scope (#1-#12 编号；这是 contract)

#### #1 — `autoloop/loop.py` top-level orchestrator

State machine per iteration (linear; no concurrency for v1):

```
1.  analyzer.analyze(baseline_results, current_lessons, recent_iterations) → FailureTaxonomy
2.  proposer.propose(taxonomy, lessons, recent_iterations) → Hypothesis
3.  sandbox.validate_skill_yaml_diff(...) → ValidationResult; REJECT → discard + log + return
4.  anti_hardcode_check(hypothesis) → AntiHardcodeResult; FAIL → discard + log + return  # placeholder always-PASS in S-Auto-3
5.  dry-run ONLY: write to autoloop/results/runs/exp-N/{hypothesis,sandbox_verdict,anti_hardcode_verdict}.json; STOP
6.  applier.apply(hypothesis) → AppliedExperiment {branch_name, commit_sha, skill_file_path, backend_port, backend_process}
7.  eval_runner.run_v1_fitness_suite(results_root, config; with env CSAGENT_BACKEND_URL=http://localhost:<port>) → dict[suite, SuiteRunResult]
8.  baseline_loader.load(config.fitness.baseline_dir, config) → BaselineSnapshot
9.  tier_evaluator.evaluate(current_results, baseline, config, shadow_results, iteration_id) → LexicographicVerdict
10. memory.experiments_log.append(iter_record)
11. memory.iterations_index.insert(iter_record)
12. memory.lessons_log: if iter_count % K == 0, trigger lessons_compactor.compact(...)
13. branch tag: git tag autoloop/keep-N or autoloop/discard-N on the exp-N branch
14. applier.cleanup() → stop Spring on alt port; release port
```

API:

```python
@dataclass
class IterationResult:
    iteration_id: str            # "exp-1" / "exp-2" / ...
    hypothesis: Hypothesis | None
    sandbox_verdict: ValidationResult | None
    anti_hardcode_verdict: AntiHardcodeResult | None
    applied: AppliedExperiment | None     # None on dry-run or pre-apply discard
    eval_results: dict[str, SuiteRunResult] | None
    verdict: LexicographicVerdict | None
    decision: Literal["keep", "discard", "error"]
    error: str | None
    elapsed_seconds: float

def run_one_iteration(*, config, iteration_id, dry_run: bool = False) -> IterationResult: ...
def run_iterations(*, config, count: int, dry_run: bool = False) -> list[IterationResult]: ...
```

**Crash recovery**: 任何 step raising → catch → set `IterationResult.decision = "error"`, `error = <str>`; `applier.cleanup()` 永远在 `finally` 块调用；memory 记 error iteration；loop continues to next iteration（不因一个 bad iter crash 整个 run）。

#### #2 — `autoloop/meta_agent/analyzer.py` + `prompts/analyze.txt`

`analyze(baseline_results_json: Path, lessons_md: Path, recent_iterations: list[IterationRecord]) -> FailureTaxonomy`

`FailureTaxonomy` 是 dict-of-dicts：

```python
{
  "skills_critical_steps_advisory_fail": {  # 哪个 Skill critical_steps 最常 advisory FAIL
    "<skill_name>": {"<step_id>": {"fail_count": N, "in_cases": [case_ids]}, ...},
    ...
  },
  "bad_cases_regressing": {  # 哪个 bad_cases 失败 closure_criterion
    "<case_id>": {"primary_uc": "UC-X", "failure_shape": "..."},
    ...
  },
  "anchor_outcome_closure_criterion_fails": {
    "<case_id>": {"primary_uc": "UC-X", "closure_criterion_snippet": "..."},
    ...
  },
  "summary": "<1-paragraph human-readable summary>"
}
```

`prompts/analyze.txt`: 系统 prompt 教 LLM "产 sanitized per-Skill failure taxonomy；不在这步 propose fix；summary 不提具体 case_id（避免 eval-phrase leakage per §1.7）"。

LLM provider/model 从 `autoloop/config.yaml` `meta_agent:` block 读（见 #11）。

#### #3 — `autoloop/meta_agent/proposer.py` + `prompts/propose.txt`

`propose(taxonomy: FailureTaxonomy, lessons: str, recent_iterations: list[IterationRecord], *, config) -> Hypothesis`

```python
@dataclass
class Hypothesis:
    target_skill_file: Path        # 6 个 Skill YAML 路径之一
    target_field_path: str          # $.procedure / $.critical_steps[2].desc 等
    before_value: str               # 当前 Skill YAML 中的文本
    after_value: str                # proposed 新文本
    rationale: str                  # 1-3 句 why
    fingerprint: str                # normalized hash (target_skill_file, target_field_path, after_value); anti-repeat
```

`prompts/propose.txt`: 系统 prompt 内嵌:
- v1 mutable surface 严格清单 (6 Skill YAMLs × 4 LLM-soft fields)
- §1.7 forbidden-list verbatim
- "不枚举用户消息关键词；不写 IF-THEN 决策树；不引用 case_ids；只软叙事 procedural guidance per M3-Eval S-Eval-3 §5.3 标准"
- "如果前 N 次在同一 target 上 `decision=discard`，换 target_field 或 target_skill（anti-repeat）"
- lessons.md 整段喂入（"avoid these patterns; reuse these patterns"）
- recent K iterations summary 表 (target_skill + decision + discard_reason)

LLM 返回 JSON 匹配 Hypothesis schema；proposer parse + validate；invalid LLM 输出 → 重试 ≤ 3 次 with stricter "respond in JSON" reminder；3 失败 → discard with reason `proposer_llm_returned_invalid_json`。

#### #4 — `autoloop/meta_agent/lessons_compactor.py` + `prompts/compact.txt`

`compact(recent_iterations: list[IterationRecord], current_lessons: str, *, config) -> str`

`iter_count % config.lessons.compaction_window_k == 0` 时触发（K default 10）。压最近 K iter 成 NEW 结构化 lesson 段落 append 到 `autoloop/results/lessons.md`。

Lesson 段落 schema (markdown):
```markdown
## Lesson L-<YYYY-MM-DD>-<NNN>

**Window**: iterations exp-<N-K+1> through exp-<N> (K=10)
**Target observation**: <e.g. "8 of 10 iterations on `discover_triage.procedure` discarded at Layer 3">
**Pattern**: <e.g. "appending enumerated decision-tree style content was rejected; soft narrative kept">
**Heuristic for future propose**: <e.g. "avoid 'IF X THEN Y' enumeration; prefer narrative conditions">
**Affected Skill × field**: <e.g. "discover_triage.procedure">
```

`prompts/compact.txt`: 系统 prompt 教 LLM "summarize patterns from these K iterations；不提具体 case_id；严格按 lesson 段落 schema 输出"。

#### #5 — `autoloop/memory/experiments_log.py`

Append-only JSONL 在 `autoloop/results/experiments.jsonl`。一行 per iteration。Schema:

```python
{
  "iteration_id": "exp-1",
  "timestamp": "2026-05-28T01:23:45Z",
  "hypothesis": {...},
  "sandbox_verdict": {...},
  "anti_hardcode_verdict": {...},
  "applied": {...} | None,
  "verdict": {...} | None,    # LexicographicVerdict serialized; shadow firewall RESPECTED (aggregate-only)
  "decision": "keep" | "discard" | "error",
  "discard_reason": str | None,
  "error": str | None,
  "elapsed_seconds": float
}
```

`append(record: dict) -> None` —— append-mode 写一行 JSON + newline + fsync。
`read_all() -> list[dict]` —— for `autoloop report`。
`read_recent(n: int) -> list[dict]` —— for proposer.propose() recent-K input。

#### #6 — `autoloop/memory/iterations_index.py`

sqlite3 stdlib（无新 dep）。DB 在 `autoloop/results/iterations.sqlite`。Schema per proposal §3.4 Layer B:

```sql
CREATE TABLE IF NOT EXISTS iterations(
    id TEXT PRIMARY KEY,                    -- "exp-1"
    ts TEXT NOT NULL,                       -- ISO timestamp
    target_skill TEXT NOT NULL,
    target_field TEXT NOT NULL,
    edit_summary TEXT,
    hypothesis_fingerprint TEXT NOT NULL,
    decision TEXT NOT NULL,                 -- "keep" / "discard" / "error"
    discard_reason TEXT,
    fitness_delta_json TEXT,
    parent_iteration_id TEXT,
    notes TEXT
);
CREATE INDEX IF NOT EXISTS idx_target_skill ON iterations(target_skill);
CREATE INDEX IF NOT EXISTS idx_fingerprint ON iterations(hypothesis_fingerprint);
CREATE INDEX IF NOT EXISTS idx_decision ON iterations(decision);
CREATE INDEX IF NOT EXISTS idx_ts ON iterations(ts);
```

API:
```python
def init_db(db_path: Path) -> None: ...
def insert(db_path: Path, record: IterationRecord) -> None: ...
def query_recent(db_path: Path, n: int) -> list[IterationRecord]: ...
def query_by_target(db_path: Path, target_skill: str, target_field: str = None) -> list[IterationRecord]: ...
def query_by_fingerprint(db_path: Path, fingerprint: str) -> list[IterationRecord]: ...
```

#### #7 — `autoloop/memory/lessons_log.py`

`autoloop/results/lessons.md` markdown 读写。Section divider 约定：`---` 分隔；H2 header `## Lesson L-...` 开头。

```python
def read_all(lessons_path: Path) -> str: ...
def append_lesson(lessons_path: Path, lesson_md: str) -> None: ...
def count_lessons(lessons_path: Path) -> int: ...
```

#### #8 — `autoloop/sandbox/applier.py` 真实实现

```python
@dataclass
class AppliedExperiment:
    iteration_id: str
    branch_name: str                  # "autoloop/exp-1"
    commit_sha: str
    skill_file_path: Path
    backend_port: int                 # alt port Spring is running on
    backend_process: subprocess.Popen # for cleanup()

def apply(hypothesis: Hypothesis, *, config) -> AppliedExperiment:
    """
    1. git rev-parse HEAD → store original_head
    2. git checkout -b autoloop/exp-<N> (or git switch -c)
    3. Read current Skill YAML; verify before_value matches hypothesis.before_value (else error)
    4. Patch the field per hypothesis.target_field_path; write back to YAML
    5. yaml_diff_validator.validate_skill_yaml_diff(before_yaml, after_yaml, file_path) — SANITY CHECK
    6. git add <skill_file>; git commit -m "autoloop exp-<N>: <hypothesis.rationale[:80]>"
    7. Find free port: bind to 0 → get port → close → use that port
    8. spawn `mvn spring-boot:run -pl server -Dspring-boot.run.arguments="--server.port=<port>"` in background
    9. Health-probe: poll `http://localhost:<port>/actuator/health` every 2s up to 120s; expect 200 + `{"status":"UP"}`; timeout → kill + raise SpringStartupTimeoutError
    10. Return AppliedExperiment
    """

def cleanup(applied: AppliedExperiment) -> None:
    """Always in loop.py finally. Kill backend_process; release port; switch back to original branch (do NOT delete autoloop/exp-N — kept for audit + cherry-pick by `autoloop apply`)."""
```

Cross-file rejection: 如果 hypothesis 命中非 Skill YAML 文件 → applier raises BEFORE git operations。

#### #9 — `autoloop/cli.py` 真实 wiring

- `check` — UNCHANGED from S-Auto-1
- `dry-run --experiments N` (alias for `run --experiments N --dry-run`)
- `run --experiments N` — runs N live iterations；flags: `--dry-run`, `--config <path>` (default `autoloop/config.yaml`)
- `report` — reads experiments.jsonl + iterations.sqlite + lessons.md → render HTML timeline at `autoloop/results/report.html`；columns: iter_id / target_skill / target_field / decision / per-Tier delta / link to per-iter dir
- `apply --experiment exp-N` —— **OQ-S55.1 Hybrid**:
  1. `git rev-parse autoloop/exp-N` → verify branch exists
  2. `git cherry-pick autoloop/exp-N` 到当前分支
  3. Compute proposed `config.yaml` patch: `fitness.baseline_dir` should advance to whichever results dir was the eval output for exp-N
  4. Print patch to stdout (unified diff style)
  5. Write patch to `autoloop/results/runs/exp-N/proposed-baseline-update.patch`
  6. Print message: "Cherry-pick applied. Review the proposed config.yaml patch above; if accepting, run `patch -p1 < autoloop/results/runs/exp-N/proposed-baseline-update.patch && git add autoloop/config.yaml && git commit --amend --no-edit`. If rejecting baseline advance, just commit the cherry-pick as-is."
  7. **不**自动 commit 任何东西
- `audit --experiment exp-N` — 读 exp-N 从 sqlite + experiments.jsonl；默认 shadow firewall RESPECTED；`--include-shadow-detail` flag 开 per-case shadow detail（仅这条路径展示，且是 human-facing audit 非 loop-facing）

#### #10 — 测试 (~30-50 NEW；期望 autoloop 总 pytest 83 → 113-133)

`autoloop/tests/test_loop.py`:
- dry-run end-to-end with mocked subprocess + mocked meta-agent (fixture hypothesis) → IterationResult.decision != "error"；未真写 Skill YAML
- live iteration end-to-end with mocked subprocess + mocked Spring + mocked eval_runner 返回 fixture results.json + tier_evaluator 返回 fixture verdict；期望 full state machine + memory 3 层 写入
- propose-stage discard (sandbox REJECT) → 短路；不 apply / 不 eval
- mid-iteration crash (e.g. mvn fails) → IterationResult.decision == "error"；cleanup() called；loop continues (test with 2-iter run)
- branch tag: keep → autoloop/keep-N；discard → autoloop/discard-N

`autoloop/tests/test_meta_agent.py`:
- analyzer with synthetic baseline results.json → expected FailureTaxonomy shape
- proposer with mocked LLM client → valid Hypothesis returned；invalid LLM output → 3 retries → fail with `proposer_llm_returned_invalid_json`
- proposer fingerprint deterministic across same input
- proposer 拒非 v1 mutable surface target (mocked LLM 返回 target_skill_file 不在 6 个里 → proposer reject + retry / fail)
- lessons_compactor with synthetic 10-iter fixture → expected markdown section produced

`autoloop/tests/test_memory.py`:
- experiments_log append + read_recent + read_all round-trip
- iterations_index CRUD + query_by_target + query_by_fingerprint + query_recent
- lessons_log read/write + count_lessons
- **Shadow firewall regression** in serialized verdict (re-verify S-Auto-2 firewall property carries through `experiments_log.append` — JSON-roundtrip scan for sentinel `case_id`)

`autoloop/tests/test_applier.py`:
- apply with valid Hypothesis (mocked git + mocked Spring) → AppliedExperiment with correct branch_name + port + commit_sha
- cleanup() 永远 called even on error
- cross-file Hypothesis → REJECT before git
- SpringStartupTimeoutError on health-probe timeout (mocked health-probe)
- before_value mismatch → error
- Hard fence: applier.py source grep — no `eval-interactive` invocation (eval 是 eval_runner 的事)

`autoloop/tests/test_cli_integration.py`:
- `python -m autoloop dry-run --experiments 1` exits 0 + 写 dry-run log
- `python -m autoloop run --experiments 1` (mocked subprocess/Spring/eval) exits 0
- `python -m autoloop report` (with fixture experiments.jsonl) 写 HTML
- `python -m autoloop apply --experiment exp-N` (with fixture exp-N branch) cherry-picks + emits patch + **NO auto-commit** (验)
- `python -m autoloop audit --experiment exp-N` (default) — JSON-roundtrip scan for sentinel `case_id` confirms NO leakage；`--include-shadow-detail` flag DOES 包含

#### #11 — `autoloop/config.yaml` `meta_agent:` + `lessons:` block 扩展

Append (S-Auto-1 + S-Auto-2 fields untouched)：

```yaml
meta_agent:
  # Credentials read from .env.local (KEY name placeholders; human fills actual values).
  # Example .env.local entries the human will add:
  #   AUTOLOOP_META_LLM_API_KEY=<your-key-here>
  #   AUTOLOOP_META_LLM_BASE_URL=<your-base-url-here>   # optional
  provider: <PLACEHOLDER-set-by-human>          # e.g. "anthropic" / "openai" / "moonshot" / "deepseek"
  model: <PLACEHOLDER-set-by-human>             # e.g. "claude-opus-4-5" / "gpt-4o" / "moonshot-v1-32k"
  temperature: 0.3
  max_tokens: 4096
  api_key_env: AUTOLOOP_META_LLM_API_KEY        # env-var name (read from .env.local)
  base_url_env: AUTOLOOP_META_LLM_BASE_URL      # optional; null → provider default
  request_timeout_seconds: 120

lessons:
  compaction_window_k: 10                        # 已在 S-Auto-1；显式确认
  recent_iterations_for_propose: 5
```

Backend URL env-var 约定（不改 eval_runner signature；通过 `os.environ` 注入）：
```
CSAGENT_BACKEND_URL=http://localhost:<port>
```

**Verify in dev**: eval_interactive 必须从某 env-var 读 backend URL；如果只从 `eval_interactive/.env` 读没有 env-var override → STOP and surface（见 Hard fences）。

#### #12 — `autoloop/program.md` 单行 status flips

Update §4 Forbidden-by-construction 表的 row 3 + row 6（其他行不动）:
- Row 3 (anti-hardcode auto-check) — annotate "DEFERRED to S-Auto-4 (S-Auto-3 ships placeholder hook with signature defined)"
- Row 6 (no main-branch cherry-pick by the loop itself) — annotate "DELIVERED — `autoloop apply` is Hybrid (cherry-pick + patch emit, NO auto-commit) per OQ-S55.1 disposition 2026-05-27"

**不**改其他 prose（single-row contract）。

### Hard fences / STOP conditions (do NOT do)

- **No touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow（read-only inputs）
- **No touch** to S-Auto-1/2 deliverable SIGNATURES (`sandbox/yaml_diff_validator.py` 不改；`scoring/tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` API 不改) —— env-var propagation OK
- **No touch** to `server/`, `eval/` Java, `data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, `docs/sprints/*` archives, `docs/milestones/*` archives, `docs/codex-findings.md`
- **No cherry-pick to main** by the loop itself；只 `autoloop apply` (Hybrid) cherry-pick，**NOT** auto-commit
- **No new heavy deps** in `autoloop/pyproject.toml` unless meta-agent LLM client 必需（e.g. `anthropic` SDK if provider=anthropic）。需要 dep STOP and surface 给 deliver-agent 显式授权 BEFORE adding
- **No live LLM call in tests** —— meta_agent + applier 测试用 mocks；live LLM 只在 1 live iteration close-gate run（human-driven）
- **No shadow per-case failure exposed** anywhere in meta_agent / proposer / memory.experiments_log / cli.audit default mode —— JSON-roundtrip scan test enforces
- **No semantic hardcode** in proposer prompt / analyzer prompt —— prompts 教 LLM §1.7，**不**枚举关键词 / 决策树
- **No anti-hardcode kernel implementation** in S-Auto-3 —— 那是 S-Auto-4；S-Auto-3 只 ship 签名 + placeholder always-PASS
- **STOP and surface** 如果 eval_interactive 不从 env-var 读 backend URL（只从 `eval_interactive/.env` 读）：文档化 gap；要么 patch eval_interactive（cross S-Auto-3 hard fence — surface to deliver-agent）OR 写 temp `.env` overlay 机制（哪种走）
- **STOP and surface** 如果 Spring startup health-probe 系统性 >120s —— 可能需要 widen OR investigate
- **STOP and surface** 如果 meta-agent LLM provider config (D1 placeholders) 需要新 dep 跨过 no-new-heavy-deps fence

### Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run pytest -q` —— Sprint 55 baseline `83 passed` MUST grow by ~30-50 NEW S-Auto-3 tests (总 ~113-133 PASS, 0 fail)。Sprint 54 + Sprint 55 tests UNCHANGED
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` 复现 `486 passed, 3 failed`
- **Java baseline UNCHANGED**: 跳过 per S-Auto-3 Java-zero-touch
- **NO live LLM call** in pytest tests —— 所有 meta_agent tests 用 mocked LLM client
- **Live iteration smoke** (dev verifies 一次 at S-Auto-3 close；NOT 在 pytest):
  - `python -m autoloop run --experiments 1 --dry-run` → exit 0 + dry-run log written
  - `python -m autoloop run --experiments 1` (FULL live) → exit 0 (不论 keep/discard)；inspect `autoloop/results/runs/exp-1/` 看 4 expected outputs (hypothesis.json + diff.yaml + verdict.json + decision.json 或等价 shape)
  - 在 handoff §"Live iter smoke" 记 verdict + elapsed time（期望 ~12-15 min；>40 min surface as observation）

### §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (§3.2 default — orchestration, persistence, port mgmt, subprocess lifecycle; no 语义决策改动)。per §7 即便纯 infra 工作只要是 §1.7 enforcement plumbing 必须带 stanza。

**Tier-0 invariant:** adds no Tier-0。auto-loop runtime 是 milestone-scoped infrastructure, NOT a Tier-0 invariant per `runtime_freeze_and_risk_policy.md` §1/§2。C2/C3 DEFER 不变。

**Semantic hardcode:** No semantic hardcode introduced in S-Auto-3 code。Meta-agent prompts (`analyze.txt` / `propose.txt` / `compact.txt`) 是 PROMPTS to LLM, 不是 Java/Python 决策逻辑 —— 它们教 LLM §1.7 forbidden-list。proposer.py 校验 LLM 输出 against v1 mutable-surface schema (file_path ∈ 6 Skills × field_path ∈ 4 LLM-soft fields) —— 是 registry-membership check, 不是语义决策。fingerprint 是确定性 hash, 不是决策。applier.py 是 git + subprocess + health-probe —— 无语义内容。Anti-repeat (proposer 查 iterations_index by fingerprint, 在 propose context 中 surface 过去 attempts) 是 memory mechanism, 不是语义规则。Hooked S-Auto-4 anti_hardcode_check 是真正 prevent §1.7 violations 从 prompt 滑过的层 —— 它在 S-Auto-4 ship 真实 implementation；S-Auto-3 只定 call-site + placeholder。

**Generalization coverage:** target = 9 scope items produce 端到端 working closed-loop (dry-run + 1 live iter 都 exit 0; FULL pipeline executes)。Neighbor = crash recovery (mid-iter mvn / Spring / eval failure → IterationResult.decision = "error" + cleanup → next iter continues)。**Negative-control test (CRITICAL)** = a fixture Hypothesis with `target_field_path = "$.applicable_use_cases"` (structural field) MUST 被 sandbox reject + propagated as iteration discard with reason `sandbox_rejected_structural_field` —— 验 structural defence 进入 loop。**Adversarial-prompt test** = a fixture meta-agent 输出 with §1.7 red-line pattern (e.g. `if user.message.contains('appeal') then UC-H`) —— anti_hardcode_check placeholder PASSES it (因为是 placeholder), 但 S-Auto-4 plug in real detection; S-Auto-3 ship test fixture 供 S-Auto-4 dev 用 regression target。**Shadow firewall test** = `audit` default mode + `experiments_log` serialized output BOTH scan-tested for sentinel `case_id` (carries forward from S-Auto-2 firewall)。

### Codex review plan (§4.3)

**Default**: milestone-shared at M-Auto-1A close。S-Auto-3 是 `infra` + 不 cross §1.7 (它 scaffolds §1.7 enforcement plumbing 不直接 execute enforcement)。无 §4.3 per-sub-sprint Codex trigger (#1 无 Tier-0 candidate; #2 无 §1.7 cross — meta-agent prompts 教 §1.7 to LLM 不 encode it; #3 无 hard-fenced surface edit — 不动 `eval_interactive/eval_interactive/**` 或 `server/`; #4 非 fix-iteration)。

Deliver-agent **不**在 S-Auto-3 close 时 dispatch Codex。Codex 在 M-Auto-1A close 时连同 S-Auto-1/2/4 一起消费 S-Auto-3 commit range。

### Handoff requirements

- Author `docs/sprints/sprint-056-handoff.md` at S-Auto-3 close；**§12** 留空 (deliver-agent + human at milestone close)
- Record in handoff: `git show --numstat` for S-Auto-3 commit；新 `autoloop/{loop.py, meta_agent/*, memory/*}` 源 + applier.py rewrite + cli.py wiring counts + pass status；live-iter smoke verdict (keep/discard/error) + elapsed seconds；`autoloop/config.yaml` `meta_agent:` block 内容（placeholders 留 in place）；任何 STOP-surfaced 项 (e.g. eval_interactive env-var override 机制问题, Spring startup pathology)；§7 self-walk；OQ-S56.x list for deliver-agent disposition

### Commit discipline

Dev stages **only S-Auto-3 scope**：新 files under `autoloop/autoloop/loop.py` + `autoloop/autoloop/meta_agent/{__init__,analyzer,proposer,lessons_compactor}.py` + `autoloop/autoloop/meta_agent/prompts/{analyze,propose,compact}.txt` + `autoloop/autoloop/memory/{__init__,experiments_log,iterations_index,lessons_log}.py` + 新 tests + 修改 `autoloop/autoloop/sandbox/applier.py` (skeleton → real impl) + 修改 `autoloop/autoloop/cli.py` (subcommand wiring) + 扩展 `autoloop/config.yaml` (`meta_agent:` + `lessons:` blocks) + 修改 `autoloop/program.md` (3-row status flips per scope #12) + NEW `docs/sprints/sprint-056-handoff.md`。**No `git add -A`** —— deliver-agent close-bundle files 由 human bundle。

一次 commit at sub-sprint close (commit-at-end pattern)。Commit message: `Sprint 56 / S-Auto-3 — loop orchestrator + meta-agent + 3-layer memory + applier real impl + cli wiring`。

## Self-check checklist

完成前 dev 必须勾选：

- [ ] `autoloop/autoloop/loop.py` 实现 state machine + IterationResult dataclass + run_one_iteration + run_iterations + crash recovery (cleanup 永远在 finally)
- [ ] `autoloop/autoloop/meta_agent/{analyzer,proposer,lessons_compactor}.py` + `prompts/{analyze,propose,compact}.txt` 实现 + proposer JSON parse + 3-retry on invalid + fingerprint deterministic + v1 mutable-surface schema enforcement
- [ ] `autoloop/autoloop/memory/{experiments_log,iterations_index,lessons_log}.py` 实现 + sqlite schema + JSONL append + lessons markdown
- [ ] `autoloop/autoloop/sandbox/applier.py` skeleton → real impl: git branch + commit + free-port discovery + Spring spawn + health-probe (120s timeout) + cleanup
- [ ] `autoloop/autoloop/cli.py` 5 subcommand 真实 wiring: check (UNCHANGED) / dry-run / run / report / apply (Hybrid: cherry-pick + emit patch + NO auto-commit) / audit (default mode RESPECTS shadow firewall; --include-shadow-detail flag opens it)
- [ ] `autoloop/config.yaml` 加 `meta_agent:` + `lessons:` block; provider/model 是 PLACEHOLDER literals; credentials env-var name 写定
- [ ] `autoloop/program.md` row 3 + row 6 status flip; 其他 prose 不动
- [ ] anti-hardcode hook 签名定: `def anti_hardcode_check(hypothesis: Hypothesis, *, config) -> AntiHardcodeResult`; placeholder always-PASS impl
- [ ] eval_runner subprocess 通过 os.environ `CSAGENT_BACKEND_URL=http://localhost:<port>` 注入；eval_runner signature 不改
- [ ] `cd autoloop && uv run pytest -q` —— Sprint 55 83 + S-Auto-3 ~30-50 NEW = ~113-133 PASS, 0 fail; Sprint 54 + 55 tests UNCHANGED
- [ ] `cd eval_interactive && uv run python -m pytest --tb=no -q` 复现 `486 passed, 3 failed`
- [ ] `git diff --stat HEAD -- server/ eval/ eval_interactive/eval_interactive/ data/ db/ server/src/main/resources/ docs/foundational/ docs/current/ docs/sprints/ docs/milestones/` 返回空 (autoloop/ + sprint-056-handoff.md 外零 edit; 也 OK if dev 临时 touch `.env.local.example` 加示例 keys 但不 touch real `.env.local`)
- [ ] **Live iteration smoke 跑过一次** (dev 手动；不在 pytest): `python -m autoloop run --experiments 1` exit 0 (不论 keep/discard); record in handoff §"Live iter smoke" + verdict + elapsed
- [ ] `docs/sprints/sprint-056-handoff.md` 完整 (per Handoff requirements); §12 留空
- [ ] §7 self-walk 在 handoff §4 记录 (Target layer / Tier-0 / Semantic hardcode / Generalization)
- [ ] 单次 commit 包含所有 S-Auto-3 scope; commit message 按格式
- [ ] 任何 STOP-surfaced 项写在 handoff §"OQ surfaced" (e.g. eval_interactive env-var override mechanism, Spring startup pathology, anti-repeat heuristic effectiveness)
