---
title: Sprint objective — Sprint 56 / M-Auto-1A S-Auto-3 — Loop orchestrator + meta-agent + 3-layer memory
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-27
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-055-objective.md]
superseded_by: null
notes: >
  DRAFT pending human approval (2026-05-27). THIRD sub-sprint of
  Milestone M-Auto-1A — Auto-Evolution Build. S-Auto-3 接通完整
  propose → sandbox → apply → mvn → Spring (alt port) → eval → verdict
  → 3-layer memory 循环。Consumes S-Auto-1 sandbox + S-Auto-2 fitness
  evaluator + runner unchanged. NEW: loop.py + meta_agent/* (analyzer
  + proposer + lessons_compactor + prompts) + memory/* (experiments_log
  raw JSONL + iterations_index sqlite + lessons_log markdown) +
  applier.py 真实实现 (git commit on autoloop/exp-N branch + Spring
  port mgmt + health-probe) + cli.py 真实 wiring (dry-run / run /
  report / apply / audit 全接通; apply 走 OQ-S55.1 Hybrid). Anti-hardcode
  hook 是 placeholder no-op (S-Auto-4 territory). S-Auto-3 close gates:
  (a) dry-run end-to-end + (b) 1 live iteration end-to-end (regardless
  of keep/discard verdict; FULL pipeline must execute without crash).
  §7 REQUIRED (`infra`); Codex deferred to M-Auto-1A milestone-shared
  close. Builds on Sprint 55 / S-Auto-2 commit `eb55322`.
  Human-locked design decisions 2026-05-27: D1 meta-agent LLM credentials
  via `.env.local` + model/provider config in autoloop/config.yaml
  `meta_agent:` block (placeholder values; human fills with actual
  API-key + model); D2 applier.py owns Spring port lifecycle + spawn
  + health-probe; eval_runner subprocess inherits backend URL via
  env-var (no signature change to S-Auto-2 deliverable).
---

# Sprint 56 / M-Auto-1A S-Auto-3 — Loop orchestrator + meta-agent + 3-layer memory

## Class

`infra` (§3.2 default — orchestration, persistence, port mgmt, subprocess lifecycle; no semantic decision change; no projection/scoring/CaseSpec change). **§7 REQUIRED** — S-Auto-3 接通 meta-agent → applier → eval → tier_evaluator 闭环，是 §1.7 enforcement 链最后一环 (S-Auto-1 sandbox 是结构性白名单 / S-Auto-2 tier_evaluator 是结构性 lexicographic gate / S-Auto-3 是把两者串起来的 runtime / S-Auto-4 是 propose-stage anti-hardcode auto-check)。

## Goal

S-Auto-3 close 时：

1. **Dry-run end-to-end**: `python -m autoloop run --experiments 1 --dry-run` 完整跑通：meta-agent propose 出 hypothesis → sandbox validate → anti-hardcode placeholder PASS → 输出 proposed diff + verdict skeleton；不 apply / 不 mvn / 不 Spring / 不真 eval / 不写长期 memory（dry-run 只写一个 dedicated dry-run log）。
2. **Live iteration end-to-end**: `python -m autoloop run --experiments 1`（无 dry-run）：meta-agent propose → sandbox validate → anti-hardcode placeholder PASS → applier.apply（git commit on `autoloop/exp-1` branch；写 Skill YAML edit）→ mvn compile → Spring restart on alt port + health-probe → eval_runner 跑 3 v1 suite（against alt port backend）→ tier_evaluator.evaluate → memory 落 3 层（raw JSONL + sqlite index + lessons.md trigger if iter_count % K == 0）。**Regardless of keep/discard verdict, FULL pipeline must execute without crash** —— close gate 是 pipeline 走通，不是产生 keep。
3. **Anti-hardcode hook 签名定型**: `anti_hardcode_check` 接口签名定 + placeholder always-PASS impl + 集成点在 loop.py propose-stage；S-Auto-4 实现时只换 impl，不改签名。
4. **`autoloop apply --experiment exp-N` 走 OQ-S55.1 Hybrid**: cherry-pick exp-N branch 的 commit 到当前分支 + 输出 proposed `autoloop/config.yaml` `fitness.baseline_dir` patch 到 stdout + 写 `autoloop/results/runs/exp-N/proposed-baseline-update.patch` → **不**自动 commit；human reviews + commits。

**Zero touch** to S-Auto-1/2 deliverable signatures（`sandbox/yaml_diff_validator.py` 不改；`scoring/tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` 不改 signature；可以通过 os.environ 传 `CSAGENT_BACKEND_URL` 给 eval_runner 子进程 —— 这是 env-var 透传，不是签名改）。**Zero touch** to `eval_interactive/eval_interactive/**`、case_spec、`server/`、`eval/` Java、`docs/foundational/`、`docs/current/`、sprint/milestone archives。

## Scope (numbered; this is the contract)

### #1 — `autoloop/loop.py` — top-level orchestrator

State machine per iteration (linear; no concurrency for v1):

```
1. analyzer.analyze(baseline_results, current_lessons, recent_iterations) → FailureTaxonomy
2. proposer.propose(taxonomy, lessons, recent_iterations) → Hypothesis
3. sandbox.validate_skill_yaml_diff(...) → ValidationResult; REJECT → discard + log + return
4. anti_hardcode_check(hypothesis) → AntiHardcodeResult; FAIL → discard + log + return     # placeholder always-PASS in S-Auto-3
5. dry-run ONLY: write to autoloop/results/runs/exp-N/{hypothesis,sandbox_verdict,anti_hardcode_verdict}.json; STOP here
6. applier.apply(hypothesis) → AppliedExperiment {branch_name, commit_sha, skill_file_path, backend_port}
7. eval_runner.run_v1_fitness_suite(results_root, config; with env CSAGENT_BACKEND_URL=http://localhost:<port>) → dict[suite, SuiteRunResult]
8. baseline_loader.load(config.fitness.baseline_dir, config) → BaselineSnapshot
9. tier_evaluator.evaluate(current_results, baseline, config, shadow_results, iteration_id) → LexicographicVerdict
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

**Crash recovery**: any step raising → catch → set `IterationResult.decision = "error"`, `error = <str>`; applier.cleanup() always called in `finally`; memory log records error iteration; loop continues to next iteration (don't crash the whole run on one bad iter).

### #2 — `autoloop/meta_agent/analyzer.py` + `prompts/analyze.txt`

`analyze(baseline_results_json: Path, lessons_md: Path, recent_iterations: list[IterationRecord]) -> FailureTaxonomy`

`FailureTaxonomy` is a dict-of-dicts: 
```python
{
  "skills_critical_steps_advisory_fail": {  # which Skill critical_steps most often advisory FAIL
    "<skill_name>": {"<step_id>": {"fail_count": N, "in_cases": [case_ids]}, ...},
    ...
  },
  "bad_cases_regressing": {                  # which bad_cases failing closure_criterion
    "<case_id>": {"primary_uc": "UC-X", "failure_shape": "..."},
    ...
  },
  "anchor_outcome_closure_criterion_fails": {  # which anchor_outcome failing 
    "<case_id>": {"primary_uc": "UC-X", "closure_criterion_snippet": "..."},
    ...
  },
  "summary": "<1-paragraph human-readable summary>"
}
```

`prompts/analyze.txt`: 系统 prompt 教 LLM "produce sanitized per-Skill failure taxonomy; do NOT propose fixes here, only describe failures; do NOT mention specific case_ids in the summary (avoid eval-phrase leakage per §1.7)".

LLM provider/model from `autoloop/config.yaml` `meta_agent:` block (see #7).

### #3 — `autoloop/meta_agent/proposer.py` + `prompts/propose.txt`

`propose(taxonomy: FailureTaxonomy, lessons: str, recent_iterations: list[IterationRecord], *, config) -> Hypothesis`

```python
@dataclass
class Hypothesis:
    target_skill_file: Path        # one of the 6 Skill YAML paths
    target_field_path: str          # JSONPath-like: $.procedure / $.critical_steps[2].desc / etc.
    before_value: str               # current text from the Skill YAML
    after_value: str                # proposed new text
    rationale: str                  # 1-3 sentence why
    fingerprint: str                # normalized hash of (target_skill_file, target_field_path, after_value); used for anti-repeat
```

`prompts/propose.txt`: 系统 prompt 内嵌:
- v1 mutable surface 严格清单 (6 Skill YAMLs × 4 LLM-soft fields)
- §1.7 forbidden-list verbatim
- "do NOT enumerate user-message keywords; do NOT write IF-THEN decision trees; do NOT reference case_ids; soft-narrative procedural guidance ONLY per M3-Eval S-Eval-3 §5.3 standard"
- "if your previous N iterations on the same target had `decision=discard`, try a different target_field or target_skill (anti-repeat)"
- lessons.md content 段落整段喂入 ("avoid these patterns; reuse these patterns")
- recent K iterations summary (target_skill + decision + discard_reason) 表

LLM call returns JSON matching `Hypothesis` schema; proposer parses + validates schema; invalid LLM output → retry up to 3 times with stricter "respond in JSON" reminder; 3 failures → discard iteration with reason `proposer_llm_returned_invalid_json`.

### #4 — `autoloop/meta_agent/lessons_compactor.py` + `prompts/compact.txt`

`compact(recent_iterations: list[IterationRecord], current_lessons: str, *, config) -> str`

Triggered by loop.py when `iter_count % config.lessons.compaction_window_k == 0` (K default 10 from S-Auto-1 config.yaml). Compacts the last K iterations into a NEW structured lesson段落, appended to `autoloop/results/lessons.md`. 

Lesson 段落 schema (markdown):
```markdown
## Lesson L-<YYYY-MM-DD>-<NNN>

**Window**: iterations exp-<N-K+1> through exp-<N> (K=10)
**Target observation**: <e.g. "8 of 10 iterations on `discover_triage.procedure` discarded at Layer 3 (no improvement); 2 kept">
**Pattern**: <e.g. "appending enumerated decision-tree style content to procedure was rejected 5 times by sandbox / no-improvement; soft narrative additions kept 2 times">
**Heuristic for future propose**: <e.g. "avoid 'IF X THEN Y' enumeration; prefer narrative conditions">
**Affected Skill × field**: <e.g. "discover_triage.procedure">
```

`prompts/compact.txt`: 系统 prompt 教 LLM "summarize patterns from these K iterations; NO mention of specific case_ids; output strictly in the lesson段落 schema above". 

### #5 — `autoloop/memory/experiments_log.py`

Append-only JSONL at `autoloop/results/experiments.jsonl`. One line per iteration. Schema:

```python
{
  "iteration_id": "exp-1",
  "timestamp": "2026-05-28T01:23:45Z",
  "hypothesis": {...},                  # full Hypothesis dict
  "sandbox_verdict": {...},             # full ValidationResult dict
  "anti_hardcode_verdict": {...},
  "applied": {...} | None,
  "verdict": {...} | None,              # LexicographicVerdict serialized; shadow firewall RESPECTED — aggregate-only
  "decision": "keep" | "discard" | "error",
  "discard_reason": str | None,
  "error": str | None,
  "elapsed_seconds": float
}
```

`append(record: dict) -> None` — opens file in append mode, writes one JSON line + newline, fsync.
`read_all() -> list[dict]` — for `autoloop report` subcommand.
`read_recent(n: int) -> list[dict]` — for proposer.propose() recent-K input.

### #6 — `autoloop/memory/iterations_index.py`

sqlite3 stdlib (no new dep). DB at `autoloop/results/iterations.sqlite`. Schema per proposal §3.4 Layer B:

```sql
CREATE TABLE IF NOT EXISTS iterations(
    id TEXT PRIMARY KEY,                    -- "exp-1"
    ts TEXT NOT NULL,                       -- ISO timestamp
    target_skill TEXT NOT NULL,             -- "discover_triage" etc.
    target_field TEXT NOT NULL,             -- "$.procedure" / "$.critical_steps[2].desc"
    edit_summary TEXT,                      -- LLM 1-sentence summary of diff (proposer fills this)
    hypothesis_fingerprint TEXT NOT NULL,   -- normalized hash for anti-repeat
    decision TEXT NOT NULL,                 -- "keep" / "discard" / "error"
    discard_reason TEXT,
    fitness_delta_json TEXT,                -- per-tier delta numbers as JSON string
    parent_iteration_id TEXT,               -- enables tree-of-attempts navigation
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
def query_by_fingerprint(db_path: Path, fingerprint: str) -> list[IterationRecord]: ...   # anti-repeat
```

### #7 — `autoloop/memory/lessons_log.py`

`autoloop/results/lessons.md` markdown read/write。Section divider 约定: each lesson 段落 separated by `---` divider; H2 header (`## Lesson L-...`) starts each.

```python
def read_all(lessons_path: Path) -> str: ...           # full markdown text for proposer prompt
def append_lesson(lessons_path: Path, lesson_md: str) -> None: ...
def count_lessons(lessons_path: Path) -> int: ...
```

### #8 — `autoloop/sandbox/applier.py` 真实实现

S-Auto-1 是 skeleton + cross-file caller-contract doc。S-Auto-3 真实实现：

```python
@dataclass
class AppliedExperiment:
    iteration_id: str
    branch_name: str               # "autoloop/exp-1"
    commit_sha: str
    skill_file_path: Path
    backend_port: int              # alt port Spring is running on
    backend_process: subprocess.Popen   # for cleanup()

def apply(hypothesis: Hypothesis, *, config) -> AppliedExperiment:
    """
    1. git rev-parse HEAD → store original_head
    2. git checkout -b autoloop/exp-<N> (or git switch -c)
    3. Read current Skill YAML; verify before_value matches hypothesis.before_value (else error)
    4. Patch the field per hypothesis.target_field_path; write back to YAML
    5. yaml_diff_validator.validate_skill_yaml_diff(before_yaml, after_yaml, file_path) — SANITY CHECK (loop should have called already, but belt-and-suspenders)
    6. git add <skill_file>; git commit -m "autoloop exp-<N>: <hypothesis.rationale[:80]>"
    7. Find free port: bind to 0 → get port → close → use that port
    8. spawn `mvn spring-boot:run -pl server -Dspring-boot.run.arguments="--server.port=<port>"` in background; capture stdout/stderr
    9. Health-probe: poll `http://localhost:<port>/actuator/health` every 2s up to 120s; expect HTTP 200 + body `{"status":"UP"}`; timeout → kill process, raise SpringStartupTimeoutError
    10. Return AppliedExperiment
    """

def cleanup(applied: AppliedExperiment) -> None:
    """Always called in loop.py finally. Kill backend_process; release port; switch back to original branch (do NOT delete autoloop/exp-N branch — kept for audit + cherry-pick by `autoloop apply`)."""
```

Cross-file rejection: if hypothesis somehow names a non-Skill-YAML file → applier raises BEFORE calling git operations.

### #9 — `autoloop/cli.py` 真实 wiring

`dry-run` / `run` / `report` / `apply` / `audit` 全部接通：

- `check` — UNCHANGED from S-Auto-1
- `dry-run --experiments N` (alias for `run --experiments N --dry-run`) — runs N dry-run iterations
- `run --experiments N` — runs N live iterations; flags: `--dry-run`, `--config <path>` (default `autoloop/config.yaml`)
- `report` — reads `experiments.jsonl` + `iterations.sqlite` + `lessons.md` → render HTML timeline at `autoloop/results/report.html`; columns: iter_id / target_skill / target_field / decision / per-Tier delta / link to per-iter dir
- `apply --experiment exp-N` — **OQ-S55.1 Hybrid**:
  1. `git rev-parse autoloop/exp-N` → verify branch exists
  2. `git cherry-pick autoloop/exp-N` onto current branch (assumes human is on a target branch)
  3. Compute proposed `config.yaml` patch: `fitness.baseline_dir` should advance to whichever results dir was the eval output for exp-N
  4. Print patch to stdout (unified diff style)
  5. Write patch to `autoloop/results/runs/exp-N/proposed-baseline-update.patch`
  6. Print message: "Cherry-pick applied. Review the proposed config.yaml patch above; if accepting, run `patch -p1 < autoloop/results/runs/exp-N/proposed-baseline-update.patch && git add autoloop/config.yaml && git commit --amend --no-edit`. If rejecting baseline advance, just commit the cherry-pick as-is."
  7. Do NOT auto-commit anything
- `audit --experiment exp-N` — reads exp-N from sqlite + experiments.jsonl; prints full record with shadow firewall RESPECTED in default mode; `--include-shadow-detail` flag opens the per-case shadow detail (only this path can show per-case shadow info, and it's human-facing audit not loop-facing)

### #10 — Tests (~30-50 NEW; expected autoloop total 83 → 113-133)

`autoloop/tests/test_loop.py`:
- dry-run end-to-end with mocked subprocess + mocked meta-agent (fixture hypothesis) → IterationResult.decision != "error"; no actual file written to Skill YAML
- live iteration end-to-end with mocked subprocess + mocked Spring + mocked eval_runner returning fixture results.json + tier_evaluator returning fixture verdict; expect full state machine traverses + memory layers written
- propose-stage discard (sandbox REJECT) → iteration short-circuits; no apply / no eval
- mid-iteration crash (e.g. mvn fails) → IterationResult.decision == "error"; cleanup() called; loop continues to next iter (test with 2-iter run)
- branch tag: keep → autoloop/keep-N; discard → autoloop/discard-N

`autoloop/tests/test_meta_agent.py`:
- analyzer with synthetic baseline results.json → expected FailureTaxonomy shape
- proposer with mocked LLM client → valid Hypothesis returned; invalid LLM output → 3 retries → fail with `proposer_llm_returned_invalid_json`
- proposer fingerprint deterministic across same hypothesis input
- proposer respects v1 mutable surface (mocked LLM tries to return a hypothesis with target_skill_file outside the 6 → proposer rejects + retries / fails)
- lessons_compactor with synthetic 10-iter fixture → expected markdown section produced

`autoloop/tests/test_memory.py`:
- experiments_log append + read_recent + read_all round-trip
- iterations_index CRUD + query_by_target + query_by_fingerprint + query_recent
- lessons_log read/write + count_lessons
- shadow firewall in serialized verdict (re-verify the S-Auto-2 firewall property carries through `experiments_log.append` — JSON-roundtrip scan for sentinel `case_id` again)

`autoloop/tests/test_applier.py`:
- apply with valid Hypothesis (mocked git + mocked Spring) → AppliedExperiment with correct branch_name + port + commit_sha
- cleanup() always called even on error
- cross-file Hypothesis → REJECT before git
- SpringStartupTimeoutError on health-probe timeout (mocked health-probe)
- before_value mismatch (file changed since hypothesis generated) → error
- Hard fence: applier.py source grep — no `eval-interactive` invocation (eval is eval_runner's job)

`autoloop/tests/test_cli_integration.py`:
- `python -m autoloop dry-run --experiments 1` exits 0 + writes dry-run log
- `python -m autoloop run --experiments 1` (with mocked subprocess/Spring/eval) exits 0
- `python -m autoloop report` (with fixture experiments.jsonl) writes HTML report
- `python -m autoloop apply --experiment exp-N` (with fixture exp-N branch) cherry-picks + emits patch + NO auto-commit
- `python -m autoloop audit --experiment exp-N` (default) — JSON-roundtrip scan for sentinel `case_id` confirms NO leakage; `--include-shadow-detail` flag does include it

### #11 — `autoloop/config.yaml` `meta_agent:` block 扩展 + 微调

Append to existing config (S-Auto-1 + S-Auto-2 fields untouched):

```yaml
meta_agent:
  # Credentials read from .env.local (KEY name placeholders; human fills actual values).
  # Example .env.local entries the human will add:
  #   AUTOLOOP_META_LLM_API_KEY=<your-key-here>
  #   AUTOLOOP_META_LLM_BASE_URL=<your-base-url-here>   # optional; for non-default providers
  provider: <PLACEHOLDER-set-by-human>           # e.g. "anthropic" / "openai" / "moonshot" / "deepseek"
  model: <PLACEHOLDER-set-by-human>              # e.g. "claude-opus-4-5" / "gpt-4o" / "moonshot-v1-32k"
  temperature: 0.3                                # low for reproducibility
  max_tokens: 4096
  api_key_env: AUTOLOOP_META_LLM_API_KEY         # env var name (read from .env.local)
  base_url_env: AUTOLOOP_META_LLM_BASE_URL       # optional; null → provider default
  request_timeout_seconds: 120

lessons:
  compaction_window_k: 10                         # already in S-Auto-1; explicit confirm
  recent_iterations_for_propose: 5                # how many recent iters to feed proposer
```

Backend URL env-var convention for eval_runner subprocess (no code change to eval_runner.py; just env propagation via `os.environ` modification before applier returns):
```
CSAGENT_BACKEND_URL=http://localhost:<port>
```

Verify in dev: eval_interactive must already read backend URL from some env var or `.env`; if it reads from `eval_interactive/.env` only (no env-var override), document the gap as OQ-S56.x for deliver-agent — would need to either (a) add env-var override to eval_interactive (cross hard fence — STOP and surface) OR (b) write a temp `.env` overlay before invoking subprocess.

### #12 — `autoloop/program.md` 单行 status flips

Update `autoloop/program.md` §4 Forbidden-by-construction table:
- Row 1 (sandbox white-list) — already DELIVERED (S-Auto-1)
- Row 2 (4-tier lexicographic fitness) — already DELIVERED (S-Auto-2)
- **Row 3 (anti-hardcode auto-check) — annotate "DEFERRED to S-Auto-4 (S-Auto-3 ships placeholder hook with signature defined)"**
- Row 4 (shadow regression gate) — already DELIVERED via S-Auto-2 tier_evaluator
- Row 5 (shadow result firewall) — already DELIVERED via S-Auto-2 tier_evaluator
- **Row 6 (no main-branch cherry-pick by the loop itself) — annotate "DELIVERED — `autoloop apply` is Hybrid (cherry-pick + patch emit, NO auto-commit) per OQ-S55.1 disposition 2026-05-27"**

NO other prose touched (single-row contract).

## Hard fences / STOP conditions (do NOT do)

- **No touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow (read-only inputs to eval_runner subprocess)
- **No touch** to S-Auto-1/2 deliverable SIGNATURES (`sandbox/yaml_diff_validator.py` 不改；`scoring/tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` API 不改) — extension via env-var propagation is OK
- **No touch** to `server/`, `eval/` Java, `data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, `docs/sprints/*` archives, `docs/milestones/*` archives, `docs/codex-findings.md`
- **No cherry-pick to main** by the loop itself; only `autoloop apply` (Hybrid) does cherry-pick, and even then NOT auto-commit (human reviews + commits)
- **No new heavy deps** in `autoloop/pyproject.toml` unless meta-agent LLM client absolutely requires one (e.g. `anthropic` SDK if provider=anthropic, `openai` if provider=openai). If a dep is needed, STOP and surface to deliver-agent for explicit authorization BEFORE adding
- **No live LLM call in tests** — meta_agent + applier tests use mocks; live LLM only exercised by the 1 live iteration close-gate run (human-driven)
- **No shadow per-case failure exposed** anywhere in meta_agent / proposer / memory.experiments_log / cli.audit default mode — JSON-roundtrip scan test enforces this (carries forward from S-Auto-2)
- **No semantic hardcode** in proposer prompt or analyzer prompt — prompts must teach LLM the §1.7 forbidden-list, not enumerate keywords / decision trees themselves
- **No anti-hardcode kernel implementation** in S-Auto-3 — that's S-Auto-4 territory; S-Auto-3 ships ONLY the hook signature + placeholder always-PASS
- **STOP and surface** if eval_interactive does NOT read backend URL from any env var (only from `eval_interactive/.env`): document the gap; either patch eval_interactive (cross S-Auto-3 hard fence — surface to deliver-agent) OR write temp `.env` overlay (clarify which mechanism)
- **STOP and surface** if Spring startup health-probe systematically takes >120s (S-Auto-3 default timeout) — may need to widen OR investigate startup pathology
- **STOP and surface** if meta-agent LLM provider config (D1 placeholders) requires a new dep that crosses the no-new-heavy-deps fence — get explicit deliver-agent authorization

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run pytest -q` — Sprint 55 baseline `83 passed` MUST grow by ~30-50 NEW S-Auto-3 tests (total ~113-133 PASS, 0 fail). Sprint 54 + Sprint 55 tests UNCHANGED.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed`.
- **Java baseline UNCHANGED**: skipped per S-Auto-3 Java-zero-touch (verify `git diff --stat HEAD -- server/ eval/ | grep src/main/java` returns empty).
- **NO live LLM call in pytest tests** — all meta_agent tests use mocked LLM client.
- **Live iteration smoke** (dev verifies once at S-Auto-3 close; NOT in pytest):
  - `python -m autoloop run --experiments 1 --dry-run` → exit 0 + dry-run log written
  - `python -m autoloop run --experiments 1` (FULL live) → exit 0 (regardless of keep/discard verdict); inspect `autoloop/results/runs/exp-1/` for the 4 expected output files (hypothesis.json + diff.yaml + verdict.json + decision.json or equivalent shape)
  - Record in handoff §"Live iter smoke" the verdict outcome + elapsed time (the per-iter elapsed observation from milestone §5; expected ~12-15 min per dataset-scope decision; if >40 min surface as observation)

## §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (§3.2 default — orchestration, persistence, port mgmt, subprocess lifecycle; no semantic decision change). Per §7, even pure-infra work that creates the meta-agent + loop runtime MUST carry the stanza because the orchestrator is the §1.7 enforcement plumbing.

**Tier-0 invariant:** adds no Tier-0 invariant. The auto-loop runtime is milestone-scoped infrastructure, NOT a Tier-0 runtime invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2. C2/C3 DEFER continues per M2-close verdict.

**Semantic hardcode:** No semantic hardcode introduced in S-Auto-3 code. The meta-agent prompts (`analyze.txt` / `propose.txt` / `compact.txt`) are PROMPTS to an LLM, not Java/Python decision logic — they teach the LLM the §1.7 forbidden-list. proposer.py validates LLM output against the v1 mutable-surface schema (file_path ∈ 6 Skills × field_path ∈ 4 LLM-soft fields) — this is registry-membership check, not semantic decision. fingerprint is a deterministic hash, not a decision. applier.py is git + subprocess + health-probe — no semantic content. Anti-repeat (proposer queries iterations_index by fingerprint, surfaces past attempts in propose context) is a memory mechanism, not a semantic rule. The hooked S-Auto-4 anti_hardcode_check is the layer that prevents §1.7 violations from semantically slipping through the prompt — it ships in S-Auto-4 as the actual implementation, S-Auto-3 just defines the call-site + placeholder.

**Generalization coverage:** target = the 9 scope items produce a working closed-loop end-to-end (dry-run + 1 live iter both exit 0; FULL pipeline executes). Neighbor = crash recovery (mid-iter mvn / Spring / eval failure → IterationResult.decision = "error" + cleanup → next iter continues). Negative-control test (CRITICAL) = a fixture Hypothesis with `target_field_path = "$.applicable_use_cases"` (structural field) MUST be rejected by sandbox + propagated as iteration discard with reason `sandbox_rejected_structural_field` — verifies the structural defence carries into the loop. Adversarial-prompt test = a fixture meta-agent output containing an §1.7 red-line pattern (e.g. `if user.message.contains('appeal') then UC-H`) — anti_hardcode_check placeholder PASSES it (since it's a placeholder), but S-Auto-4 will plug in real detection; S-Auto-3 ships the test fixture so S-Auto-4 dev has the regression target. Shadow firewall test = `audit` default mode + `experiments_log` serialized output BOTH scan-tested for sentinel `case_id` (carries forward from S-Auto-2 firewall property).

## Codex review plan (§4.3)

**Default**: milestone-shared at M-Auto-1A close. S-Auto-3 is `infra` + does NOT cross §1.7 (it scaffolds the §1.7 enforcement plumbing without directly executing the enforcement). No per-sub-sprint Codex trigger fires (#1 no Tier-0 candidate; #2 no §1.7 cross — meta-agent prompts teach §1.7 to LLM, do not encode it; #3 no hard-fenced surface edit — does not modify `eval_interactive/eval_interactive/**` or `server/`; #4 not fix-iteration).

The deliver-agent does NOT dispatch Codex at S-Auto-3 close. Codex consumes the cumulative S-Auto-1 through S-Auto-4 commit range at M-Auto-1A close.

## Handoff requirements

- Author `docs/sprints/sprint-056-handoff.md` at S-Auto-3 close; leave **§12** empty (deliver-agent + human at milestone close).
- Record in handoff: `git show --numstat` for the S-Auto-3 commit; new `autoloop/{loop.py,meta_agent/*,memory/*}` source + applier.py rewrite + cli.py wiring counts + pass status; the live-iter smoke verdict (keep/discard/error) + elapsed seconds; `autoloop/config.yaml` `meta_agent:` block contents (with placeholders still in place — human fills later); any STOP-surfaced item (e.g. eval_interactive env-var override mechanism question, Spring startup pathology); §7 self-walk; OQ-S56.x list for deliver-agent disposition.

## Commit discipline

Dev stages **only S-Auto-3 scope**: new files under `autoloop/autoloop/loop.py` + `autoloop/autoloop/meta_agent/{__init__,analyzer,proposer,lessons_compactor}.py` + `autoloop/autoloop/meta_agent/prompts/{analyze,propose,compact}.txt` + `autoloop/autoloop/memory/{__init__,experiments_log,iterations_index,lessons_log}.py` + new tests + modified `autoloop/autoloop/sandbox/applier.py` (skeleton → real impl) + modified `autoloop/autoloop/cli.py` (subcommand wiring) + extended `autoloop/config.yaml` (`meta_agent:` + `lessons:` blocks) + modified `autoloop/program.md` (3-row status flips per scope #12) + NEW `docs/sprints/sprint-056-handoff.md`. **No `git add -A`** — deliver-agent close-bundle files bundled by human at sub-sprint or milestone close.

One commit at sub-sprint close (commit-at-end pattern). Commit message: `Sprint 56 / S-Auto-3 — loop orchestrator + meta-agent + 3-layer memory + applier real impl + cli wiring`.

## Scope size (§8.5 note)

M-Auto-1A with S-Auto-3 = 3 of 4 sub-sprints; within §8.5 5-sub-sprint ceiling. S-Auto-3 is the largest sub-sprint in M-Auto-1A by LOC + integration surface; estimated 5-6 dev-days. No conditional / deferred scope (everything in scope is required to hit close gates).

## OQ (open questions — filled during the sub-sprint)

- **OQ-S55.1** (apply baseline pointer ceremony) — RESOLVED inline per Scope #4 + #9 (Hybrid; cherry-pick + patch emit + NO auto-commit).
- **OQ-S55.4** (Java replay surface wiring) — DISPOSED at Sprint 55 close (legacy/superseded; no v1 action); not S-Auto-3 territory.
- _additional OQ-S56.x added by the dev session as ambiguities surface — most likely eval_interactive env-var override mechanism, Spring startup timeout calibration, anti-repeat heuristic effectiveness (deferred to M-Auto-1B evidence)_
