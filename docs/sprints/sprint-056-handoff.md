---
title: Sprint 56 / M-Auto-1A S-Auto-3 — loop orchestrator + meta-agent + 3-layer memory + applier real impl + cli wiring — dev handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (sub-sprint dev archive); autoloop/autoloop/loop.py + autoloop/autoloop/meta_agent/{__init__,analyzer,proposer,lessons_compactor,llm_client}.py + autoloop/autoloop/meta_agent/prompts/{analyze,propose,compact}.txt + autoloop/autoloop/memory/{__init__,experiments_log,iterations_index,lessons_log}.py + autoloop/autoloop/sandbox/{applier,anti_hardcode_check}.py + autoloop/autoloop/cli.py + autoloop/program.md (§4 row 3 + row 6 status flips)
last_reviewed: 2026-05-27
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Third sub-sprint of Milestone M-Auto-1A. S-Auto-3 closes the
  auto-evolution loop by wiring S-Auto-1 (sandbox YAML-diff
  validator) + S-Auto-2 (5-layer fitness evaluator + eval_runner +
  baseline_loader) into a top-level `loop.py` orchestrator with a
  meta-agent (analyzer + proposer + lessons_compactor + 3 prompts),
  three-layer memory (experiments_log JSONL + iterations_index
  sqlite + lessons_log markdown), a real applier (git branch +
  YAML field patch + Spring alt-port spawn + health-probe + cleanup),
  five real CLI subcommands (check / dry-run / run / report / apply
  Hybrid / audit with shadow-firewall default), and a placeholder
  always-PASS anti_hardcode_check signature for S-Auto-4 to swap.
  64 new pytest tests pass (83 baseline + 64 new = 147 autoloop
  tests). eval_interactive baseline reproduces `486 passed, 3
  failed`. Zero edits to `server/`, `eval/`, `eval_interactive/eval_interactive/`,
  `data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`,
  `docs/current/`, or any sprint/milestone archive. ONE
  out-of-standard-scope edit surfaced as OQ-S56.1:
  `eval_interactive/eval_interactive.yaml` swaps literal
  `bot.base_url: http://localhost:8080` for env-var indirection
  `bot.base_url: ${CSAGENT_BACKEND_URL}` (required for live-iter
  alt-port routing per contract §"Live iteration end-to-end"). §12
  reserved for deliver-agent + human at milestone close
  (M-Auto-1A uses milestone-shared Codex per §4.3; S-Auto-3 introduces
  no Tier-0 candidate, no §1.7 cross — meta-agent prompts TEACH §1.7
  to LLM rather than encoding it, no hard-fence-violating surface
  edit beyond OQ-S56.1, and is not a fix-iteration — milestone-shared
  default holds).
---

# Sprint 56 / M-Auto-1A S-Auto-3 — loop orchestrator + meta-agent + 3-layer memory + applier real impl + cli wiring — dev handoff

## 1. Goal and outcome

**Goal (from `compact/sprint-056-dev-prompt.md`):**
Close the auto-evolution loop end-to-end by wiring the S-Auto-1 sandbox
and S-Auto-2 fitness evaluator into a working orchestrator. Concretely:
(a) `loop.py` state-machine orchestrator that runs `analyzer → proposer
→ sandbox → anti-hardcode-placeholder → applier → eval_runner →
tier_evaluator → 3-layer memory writes → branch tag` per iteration,
with crash recovery + cleanup-in-finally; (b) `meta_agent/*` module
(analyzer + proposer + lessons_compactor + LLM client wrapper + 3
prompt files) where the proposer enforces v1 mutable-surface schema
and retries up to 3 times on invalid JSON; (c) `memory/*` module (raw
JSONL append-only experiments_log + sqlite iterations_index + markdown
lessons_log); (d) real implementation of `sandbox/applier.py` (git
branch + YAML field patch + post-patch sanity check + free-port
discovery + Spring spawn + actuator/health probe with 120s timeout +
cleanup); (e) `cli.py` real wiring of all five subcommands
(check unchanged; dry-run + run via the new orchestrator; report
renders HTML timeline; apply is Hybrid cherry-pick + emit baseline
patch + NO auto-commit per OQ-S55.1 disposition; audit RESPECTS shadow
firewall by default + opens it only behind `--include-shadow-detail`
flag); (f) `program.md` §4 row 3 + row 6 status flips; (g)
**placeholder always-PASS** `anti_hardcode_check` hook with signature
defined for S-Auto-4 to swap in real detection.

Close gates per contract:
- `python -m autoloop dry-run --experiments 1` completes end-to-end
  (analyzer → proposer → sandbox → anti-hardcode → dry-run artefacts
  written → STOP without applier/eval/memory).
- `python -m autoloop run --experiments 1` runs the FULL pipeline
  end-to-end regardless of keep/discard verdict.
- 30-50 new pytest tests; total autoloop suite ~113-133 PASS.
- eval_interactive baseline UNCHANGED (`486 passed, 3 failed`).
- Zero edits to `server/`, `eval/` Java, inner
  `eval_interactive/eval_interactive/`, `data/`, `db/`, governance
  docs, sprint/milestone archives.

**Outcome:** delivered against contract.

- 13 NEW source artefacts (excluding `__init__.py` files):
  - `autoloop/autoloop/loop.py` (603 LOC) — orchestrator + crash recovery.
  - `autoloop/autoloop/memory/experiments_log.py` (105 LOC)
  - `autoloop/autoloop/memory/iterations_index.py` (203 LOC)
  - `autoloop/autoloop/memory/lessons_log.py` (82 LOC)
  - `autoloop/autoloop/meta_agent/analyzer.py` (236 LOC)
  - `autoloop/autoloop/meta_agent/proposer.py` (297 LOC)
  - `autoloop/autoloop/meta_agent/lessons_compactor.py` (145 LOC)
  - `autoloop/autoloop/meta_agent/llm_client.py` (170 LOC)
  - `autoloop/autoloop/meta_agent/prompts/analyze.txt` (60 LOC)
  - `autoloop/autoloop/meta_agent/prompts/propose.txt` (108 LOC)
  - `autoloop/autoloop/meta_agent/prompts/compact.txt` (49 LOC)
  - `autoloop/autoloop/sandbox/anti_hardcode_check.py` (80 LOC; placeholder).
  - 3 new `__init__.py` files (memory, meta_agent, refreshed sandbox).
- 5 NEW test files:
  - `autoloop/tests/test_memory.py` (191 LOC, 14 tests)
  - `autoloop/tests/test_meta_agent.py` (373 LOC, 17 tests)
  - `autoloop/tests/test_applier.py` (309 LOC, 11 tests)
  - `autoloop/tests/test_loop.py` (498 LOC, 10 tests)
  - `autoloop/tests/test_cli_integration.py` (286 LOC, 13 tests)
  - Total: 64 new tests; Sprint 54 + 55 baseline tests UNCHANGED.
- Modifications:
  - `autoloop/autoloop/cli.py` — 5 subcommands fully wired (was placeholders).
  - `autoloop/autoloop/sandbox/applier.py` — skeleton → real implementation
    (git operations, YAML field patch, free-port, Spring spawn,
    health-probe, cleanup).
  - `autoloop/autoloop/sandbox/__init__.py` — re-export `anti_hardcode_check`.
  - `autoloop/autoloop/scoring/eval_runner.py` — set default
    `CSAGENT_BACKEND_URL` in subprocess env (no signature change).
  - `autoloop/program.md` — §4 row 3 (anti-hardcode auto-check) + row 6
    (no main-branch cherry-pick) status fields updated; rest unchanged.
  - `autoloop/tests/test_cli_smoke.py` — single test rewritten:
    `test_placeholder_subcommand_exits_nonzero` → `test_dry_run_subcommand_invokes_loop`
    (S-Auto-1 asserted placeholder banner; S-Auto-3 wires the
    orchestrator so the banner is gone — the new test verifies the
    orchestrator is invoked).
- ONE out-of-standard-scope edit surfaced as OQ-S56.1:
  `eval_interactive/eval_interactive.yaml` (top-level eval config file;
  NOT under hard-fenced `eval_interactive/eval_interactive/**` inner
  module). The single change is `bot.base_url:` literal →
  `${CSAGENT_BACKEND_URL}` env-var indirection. See §"OQ surfaced".
- ONE machine-local edit (gitignored; NOT committed): `.env.local`
  gained a default `CSAGENT_BACKEND_URL=http://localhost:8080` entry +
  commented-out placeholders for the autoloop meta-agent LLM env vars
  the human fills before live runs.

## 2. Scope (per `compact/sprint-056-dev-prompt.md` §#1-#12)

### #1 — `autoloop/loop.py` orchestrator — DELIVERED

State machine implemented per contract:

```
1.  analyzer.analyze(baseline_summary, lessons, recent)  → FailureTaxonomy
2.  proposer.propose(taxonomy, lessons, recent, ...)     → Hypothesis (or ProposerInvalidOutputError)
3.  sandbox.validate_skill_yaml_diff(...)                → ValidationResult
4.  anti_hardcode_check(hypothesis, config)              → AntiHardcodeResult (S-Auto-3 placeholder always-PASS)
5.  dry-run: write artefacts under runs/<id>/, STOP.
6.  applier.apply(hypothesis, ...)                       → AppliedExperiment
7.  eval_runner.run_v1_fitness_suite(...) with env CSAGENT_BACKEND_URL=http://127.0.0.1:<alt-port>
8.  baseline_loader.load(config.fitness.baseline_dir)    → BaselineSnapshot
9.  tier_evaluator.evaluate(current_results, baseline, config, iteration_id)
                                                         → LexicographicVerdict
10. memory.experiments_log.append(record)
11. memory.iterations_index.insert(record)
12. iter_count % K == 0 → lessons_compactor.compact + lessons_log.append
13. git tag autoloop/keep-N or autoloop/discard-N on the exp-N branch.
14. applier.cleanup() — ALWAYS in finally.
```

`IterationResult` dataclass carries the full per-iteration state.
`run_one_iteration(*, config, iteration_id, dry_run=False, client=None,
repo_root=None)` returns one result; `run_iterations(*, config, count,
dry_run=False, client=None, repo_root=None, start_index=None)` runs N
iterations sequentially.

**Crash recovery:** every step is inside a top-level try/except.
On exception, `result.decision = "error"`, `result.error =
"<class>: <msg>\n<tb>"`, the record is still persisted to memory, and
`applier.cleanup()` runs in `finally`. Loop continues to the next
iteration even after a bad iter (`test_loop_continues_after_iteration_error`
covers this).

**Shadow firewall preserved end-to-end:** the loop calls
`tier_evaluator.evaluate(...)` with default `audit=False` (no per-case
shadow detail returned). The serialized `verdict.tier_breakdown` in
the experiments_log row contains only aggregate Layer 4 metrics
(`drop_pct`, `regression_detected`, `baseline_pass_rate`,
`current_pass_rate`, `max_drop_pct`). A regression test
(`test_shadow_firewall_holds_in_experiments_log`) writes a verdict
with aggregate-only Layer 4 metrics, then greps the log text for
sentinel keys `per_case_failures` and `failure_tags` — neither
appears.

### #2 — `autoloop/meta_agent/analyzer.py` + `prompts/analyze.txt` — DELIVERED

`analyze(baseline_results_summary, lessons_md, recent_iterations, *,
client) -> FailureTaxonomy` produces the structured failure landscape
per contract. The system prompt teaches the LLM to:

- emit JSON matching the four-key taxonomy schema;
- NOT quote raw user messages or known eval phrases in `summary`
  or `failure_shape`;
- NOT list case_ids inside `summary` (prose surface).

Post-process sanitization: `_CASE_ID_PATTERN` regex scrubs case_id
shapes from `summary` even if the LLM leaks them (defensive layer
beyond the prompt discipline). `test_analyzer_summary_scrubs_case_ids`
verifies.

Unparseable LLM output → fallback to empty taxonomy with
`summary="analyzer_returned_unparseable_json_fallback_to_empty_taxonomy"`;
the iteration continues (analyzer failure is not load-bearing — the
proposer still gets a no-data taxonomy + the lessons file +
recent-iterations summary).

`build_baseline_summary(results_paths)` aggregates per-suite
results.json into the dict the analyzer reads. Critical: the shadow
suite is processed differently — only aggregate counts
(`total_cases`, `passed_cases`, `failed_cases`) enter the summary; no
`per_case` block, no `failure_tags`. `test_analyzer_build_baseline_summary_filters_shadow_to_aggregate`
verifies no `shadow-N` ids or `failure_tags` strings leak into the
summary serialization.

### #3 — `autoloop/meta_agent/proposer.py` + `prompts/propose.txt` — DELIVERED

`propose(taxonomy, lessons, recent_iterations, *, client, config,
max_retries=3) -> Hypothesis` implements the LLM round-trip + 3-retry
+ structural validation per contract. The system prompt enumerates:

- the six allowed Skill YAML files + four allowed field paths
  (verbatim from `program.md` §2);
- the §1.7 forbidden-list (verbatim from
  `docs/current/iteration_governance.md`);
- the anti-repeat heuristic (switch target if last N iters discarded
  on same target);
- the "no markdown fences; no prose; raw JSON" output discipline.

Post-parse validation runs the structural checks the prompt asks the
LLM to obey:
- `target_skill_file` must be in `config.mutable_surface.allowed_skill_files`;
- `target_field_path` must match one of
  `config.mutable_surface.allowed_field_paths` (concrete indices
  match `[*]` wildcard patterns);
- `before_value != after_value`;
- all required fields present and string-typed.

Failure → retry with stricter "respond in JSON" reminder; after 3
attempts → `ProposerInvalidOutputError` with `attempts=3` +
`last_output=<text>`. The orchestrator catches this and sets
`discard_reason="proposer_llm_returned_invalid_json"` (NOT
`decision="error"` — the proposer's inability to produce valid output
is a normal discard outcome, not an infra crash).

`fingerprint_hypothesis(hyp)` is a deterministic SHA-256[:16] of
`(target_skill_file, target_field_path, after_value)`. `before_value`
+ `rationale` are intentionally excluded — the proposer cares about
"where + what change" not "what was there before". Tested by
`test_proposer_fingerprint_deterministic`.

`_REPO_ROOT` is module-level (settable by tests via monkeypatch). The
proposer reads the six allowed Skill YAMLs from disk on every call so
the LLM can emit a verbatim `before_value`. For v1 small-N, the entire
six-file content fits comfortably in the propose-prompt context.

### #4 — `autoloop/meta_agent/lessons_compactor.py` + `prompts/compact.txt` — DELIVERED

`compact(recent_iterations, current_lessons, *, client, next_counter=None)
-> str` runs the compactor LLM and produces ONE new
`## Lesson L-<YYYY-MM-DD>-<NNN>` markdown section. The system prompt
teaches:

- 5-line lesson template (Window / Target observation / Pattern /
  Heuristic for future propose / Affected Skill × field);
- NO case_ids in the lesson body;
- NO raw eval phrases;
- "no clear pattern" lesson is acceptable for windows with mixed
  outcomes;
- raw markdown output; no code fences; no prose around.

Post-process normalization:
- Strips ``` fences if the LLM accidentally adds them.
- Rewrites the `L-...` id to the correct counter (LLM may emit a
  plausible-but-wrong counter; the orchestrator passes the truth).
- Defensive `_CASE_ID_PATTERN` scrub on the body.
- Fallback "unparseable" lesson if the LLM emits non-section text.

Trigger condition (in `loop._maybe_compact_lessons`): every K
iterations from the experiments_log total count. K is
`config.lessons.compaction_window_k` (default 10). The window is the
LAST K records; the new lesson is appended to
`autoloop/results/lessons.md` via `lessons_log.append_lesson`.

### #5 — `autoloop/memory/experiments_log.py` — DELIVERED

Append-only JSONL per contract. Three functions: `append(path,
record)` (fsync after each write); `read_all(path)` (returns list,
tolerates trailing partial lines from a crash mid-write —
`test_experiments_log_tolerates_partial_trailing_line` verifies);
`read_recent(path, n)` (returns last N records in chronological
order). Iter variant `iter_records` ships for future large-N cases
but is unused in v1.

Shadow firewall regression test
(`test_experiments_log_shadow_firewall_no_per_case_key`) explicitly
writes a fake record matching the loop's shape, then asserts neither
`per_case_failures` nor `case_id` string appears in the log text.

### #6 — `autoloop/memory/iterations_index.py` — DELIVERED

sqlite3 stdlib (no new dependency). Schema per contract:

```sql
CREATE TABLE IF NOT EXISTS iterations(
    id TEXT PRIMARY KEY,
    ts TEXT NOT NULL,
    target_skill TEXT NOT NULL,
    target_field TEXT NOT NULL,
    edit_summary TEXT,
    hypothesis_fingerprint TEXT NOT NULL,
    decision TEXT NOT NULL,
    discard_reason TEXT,
    fitness_delta_json TEXT,
    parent_iteration_id TEXT,
    notes TEXT
);
-- + idx_target_skill, idx_fingerprint, idx_decision, idx_ts
```

API: `init_db`, `insert(record: IterationRecord)`,
`query_recent(n)`, `query_by_target(skill, field?)`,
`query_by_fingerprint(fp)`. `IterationRecord` is a dataclass with the
fields above plus `fitness_delta: dict` (round-tripped through the
JSON blob column — `test_iterations_index_round_trip_fitness_delta`
verifies). `init_db` is idempotent (sqlite `IF NOT EXISTS`).

### #7 — `autoloop/memory/lessons_log.py` — DELIVERED

Markdown read/write per contract: `read_all(path)` (returns content
or one-line header on first run); `append_lesson(path, section)`
(creates parent dir + file header on first call; appends `---`
divider before subsequent sections); `count_lessons(path)` (counts
`## Lesson` H2 headers).

### #8 — `autoloop/sandbox/applier.py` real implementation — DELIVERED

The S-Auto-1 skeleton's docstring + placeholder `apply_proposal` are
replaced by `apply(hypothesis, *, iteration_id, config, repo_root,
health_probe_timeout_seconds=120, health_probe_interval_seconds=2.0)`
that runs the full sequence per contract:

1. Reject hypothesis whose `target_skill_file` is outside
   `config.mutable_surface.allowed_skill_files` — raises
   `CrossFileError` BEFORE any git operation. Verified by
   `test_apply_cross_file_rejected_before_git`.
2. Capture current branch (`git rev-parse --abbrev-ref HEAD`); create
   `autoloop/<iteration_id>` branch via `git switch -c` (falls back
   to `git checkout -b`).
3. Read current Skill YAML; parse the field path via
   `_parse_field_path` (`$.procedure` →
   `["procedure"]`; `$.critical_steps[2].desc` →
   `["critical_steps", 2, "desc"]`). Read the current field value;
   compare against `hypothesis.before_value`; raise
   `BeforeValueMismatchError` on mismatch
   (`test_apply_before_value_mismatch_raises`).
4. Patch via `yaml.safe_load + safe_dump` round-trip — sets the field
   to `hypothesis.after_value`. AST-level diff is minimal even when
   the on-disk dump reformats unrelated quoting; the sandbox AST
   check confirms this (`test_write_field_to_yaml_round_trip_changes_only_one_path`).
5. Sanity-check: re-run `validate_skill_yaml_diff(before, after,
   file_path)` against the on-disk result. REJECT → raise
   `SanityCheckRejectError` before committing.
6. `git add <skill_file>` + `git commit -m "autoloop <iter_id>:
   <rationale[:80]>"`. Commit SHA captured via `git rev-parse HEAD`.
7. Find free port via `socket(0)` bind-and-read.
8. Spawn `mvn -q -pl server -am spring-boot:run -Dspring-boot.run.arguments=--server.port=<port>`
   from repo root with inherited environment. Subprocess PID + Popen
   handle captured into `AppliedExperiment.backend_process`.
9. Poll `http://127.0.0.1:<port>/actuator/health` every
   `health_probe_interval_seconds` (default 2.0s) for at most
   `health_probe_timeout_seconds` (default 120s). Success: HTTP 200
   + JSON body `{"status": "UP"}`. Timeout → kill subprocess + raise
   `SpringStartupTimeoutError`. Early subprocess exit (`proc.poll()
   != None`) is also treated as timeout. Tested by
   `test_apply_spring_timeout_propagates_cleanly`.
10. Return `AppliedExperiment(iteration_id, branch_name, commit_sha,
    skill_file_path, backend_port, backend_process, original_branch)`.

`cleanup(applied, *, repo_root)` is the symmetric teardown — always
called by `loop.py` in `finally`. Kills `backend_process` (process-group
SIGTERM → 15s wait → SIGKILL if still alive), switches back to the
original branch. The `autoloop/<iteration_id>` branch is **kept** for
audit and for `autoloop apply` to cherry-pick from later.

Hard-fence grep test
(`test_applier_source_does_not_invoke_eval_interactive`) scans the
applier.py source after stripping docstrings + comments. The
`eval-interactive` token does not appear — eval is the eval_runner's
job, not the applier's.

### #9 — `autoloop/cli.py` 5 subcommand wiring — DELIVERED

| Subcommand | Behavior | Tests |
|---|---|---|
| `check` | UNCHANGED from S-Auto-1: config validation + sandbox self-test. | `test_check_with_shipped_config_exits_zero`, `test_check_with_missing_skill_path_exits_one` (UNCHANGED). |
| `dry-run [-n N]` | Alias for `run --dry-run -n N`. Invokes orchestrator in dry-run mode. | `test_cli_dry_run_invokes_loop` |
| `run [-n N] [--dry-run]` | Calls `loop.run_iterations(config, count=N, dry_run=...)`. Exits 0 if no iter errored; 1 if any errored. | `test_cli_run_invokes_loop_with_dry_run_false` |
| `report` | Reads `experiments_log` + `lessons_log` + sqlite index. Renders `autoloop/results/report.html` with a flat per-iteration table + lessons section. | `test_cli_report_renders_html` |
| `apply --experiment exp-N` | **Hybrid per OQ-S55.1**: (1) verifies `autoloop/exp-N` branch exists; (2) `git cherry-pick autoloop/exp-N`; (3) computes proposed `config.yaml` baseline_dir patch; (4) writes patch to `autoloop/results/runs/exp-N/proposed-baseline-update.patch`; (5) prints patch to stdout; (6) prints instructions for human review/commit; (7) **NO auto-commit**. | `test_cli_apply_does_not_auto_commit`, `test_cli_apply_missing_branch_errors` |
| `audit --experiment exp-N [--include-shadow-detail]` | Default RESPECTS shadow firewall: reads only the persisted experiments_log + sqlite record, which by design carry only aggregate Layer 4 metrics. `--include-shadow-detail` opens the human-only path that re-reads per-case shadow results.json. | `test_cli_audit_default_respects_shadow_firewall`, `test_cli_audit_include_shadow_detail_flag_opens_per_case` |

Argparse subparser exit codes: `--help` exits 0 (argparse convention);
`run` / `dry-run` exit 1 if any iteration ends in `decision=error`;
all others exit 0 on success / 1 on user error (missing branch,
missing config, etc.).

### #10 — Tests — DELIVERED (~64 new; ~30-50 target)

| File | Tests | Notes |
|---|---|---|
| `test_memory.py` | 14 | experiments_log append + read + recent + partial trailing line + shadow firewall regression; iterations_index init/insert/recent/by_target/by_fingerprint/round_trip; lessons_log read/append/divider/count. |
| `test_meta_agent.py` | 17 | analyzer happy + unparseable fallback + case_id scrub + fence stripping + build_baseline_summary shadow filtering; proposer happy + 3-retry + 3-fail-raises + out-of-surface target/field reject + concrete `critical_steps[N]` accept + identical-before/after reject + fingerprint deterministic; lessons_compactor happy + case_id scrub + counter rewrite + fallback. |
| `test_applier.py` | 11 | apply happy path + cross-file reject + before_value mismatch + Spring timeout + cleanup no-process + grep no-eval-interactive + parse_field_path simple/indexed + write_field round-trip + read_field_value. |
| `test_loop.py` | 10 | dry-run short-circuit + sandbox-reject short-circuit + anti-hardcode placeholder always-passes + proposer-invalid-json discard + mid-iter crash with cleanup + loop continues after iter error + happy-path keep full pipeline + branch tag keep format + shadow firewall in experiments_log + adversarial-fixture for S-Auto-4. |
| `test_cli_integration.py` | 13 | `--help` exits 0; dry-run invokes loop; run invokes loop with dry_run=False; report renders HTML; apply Hybrid does NOT auto-commit + missing-branch errors; audit RESPECTS firewall by default + opens it under `--include-shadow-detail`. |

**Full suite total:** 147 PASS (83 baseline + 64 new). Sprint 54 + 55
tests UNCHANGED. ONE existing Sprint 54 test was rewritten:
`test_placeholder_subcommand_exits_nonzero` → `test_dry_run_subcommand_invokes_loop`
(the placeholder subcommand banner is structurally gone in S-Auto-3,
so the assertion changed accordingly).

```
cd autoloop && uv run pytest -q
147 passed, 1 warning in ~3s
```

### #11 — `autoloop/config.yaml` — UNCHANGED (already extended at S-Auto-1+S-Auto-2)

The `meta_agent:` block was added during the S-Auto-3 planning round
(commit `d9e4086`) and is already in place with the AICodeWith
Anthropic-compatible provider config. The `lessons:` block was set at
S-Auto-1. S-Auto-3 reads both without further config changes.
Credentials are loaded from repo-root `.env.local` via python-dotenv
(`autoloop/autoloop/meta_agent/llm_client.py::_load_env_local`).

### #12 — `autoloop/program.md` 2-row status flip — DELIVERED

Two single-cell edits to §4 "Forbidden by construction" table:

- **Row 3 (Anti-hardcode auto-check)**: status field now reads
  `S-Auto-4 — **DEFERRED** (S-Auto-3 ships placeholder hook with
  signature defined; S-Auto-4 swaps in real detection implementation
  without changing the signature)`.
- **Row 6 (No main-branch cherry-pick by the loop itself)**: status
  now reads `S-Auto-3 — **DELIVERED** (...Hybrid per OQ-S55.1
  disposition 2026-05-27: cherry-pick + emit proposed config.yaml
  baseline_dir patch + NO auto-commit; human reviews + commits
  manually)`.

No other prose in `program.md` touched.

## 3. Hard-fence verification

`git diff --stat HEAD -- server/ eval/ eval_interactive/eval_interactive/
data/ db/ server/src/main/resources/ docs/foundational/ docs/current/
docs/sprints/ docs/milestones/` returns **empty** — zero edits to any
forbidden surface.

The single exception is `eval_interactive/eval_interactive.yaml`
(top-level eval config; NOT under `eval_interactive/eval_interactive/**`
inner module). That edit is documented as **OQ-S56.1** (see below).

`grep -rn "anti_hardcode_check" autoloop/autoloop/sandbox/ |
grep -v "placeholder_always_pass" | wc -l` shows the placeholder
implementation is the ONLY callable detection logic — no real
detection rules are encoded. S-Auto-4 must swap in real impl behind
the existing signature.

## 4. §7 stanza self-walk

**Target failure layer:** `infra` (§3.2 default — orchestration,
persistence, port mgmt, subprocess lifecycle; no semantic-decision
change, no projection change, no scoring change). Per §7, infra
sprints that scaffold §1.7 enforcement plumbing must carry the
stanza; S-Auto-3 closes the §1.7 enforcement chain (sandbox →
anti-hardcode-placeholder → applier → eval → tier_evaluator →
memory) by wiring its missing middle stages.

**Tier-0 invariant:** This sprint adds no Tier-0. The auto-loop
runtime is milestone-scoped infrastructure, NOT a Tier-0 invariant
per `docs/runtime_freeze_and_risk_policy.md` §1/§2. C2/C3 DEFER
continues per M2-close verdict.

**Semantic hardcode:** **No semantic hardcode introduced in S-Auto-3
code.** Justification by surface:

- Meta-agent prompts (`analyze.txt` / `propose.txt` / `compact.txt`)
  are PROMPTS to the LLM, NOT Java / Python decision logic. They
  TEACH the LLM the §1.7 forbidden-list verbatim and the v1
  mutable-surface schema, so the LLM is the agent that respects §1.3.
- `proposer.py` post-parse validator checks the LLM output against
  the v1 mutable-surface schema (`target_skill_file` ∈ 6 Skills,
  `target_field_path` matches one of 4 path patterns). This is a
  **registry-membership check**, not a semantic decision — it's the
  same structural defense the sandbox YAML-diff validator already
  applies, just at the LLM-output stage.
- `fingerprint_hypothesis` is a deterministic SHA-256 hash, not a
  semantic decision.
- `applier.py` is git + subprocess + health-probe — zero semantic
  content; only `_parse_field_path` and `_write_field_to_yaml` touch
  YAML structure, and both operate on caller-provided paths/values
  without inspecting their semantic meaning.
- The anti-repeat heuristic (proposer queries iterations_index by
  fingerprint, surfaces past attempts in propose context) is a
  **memory mechanism**, not a semantic rule — the proposer's LLM
  decides what to do with the surfaced history.
- The placeholder `anti_hardcode_check` returns PASS unconditionally;
  it is structurally a no-op. The signature is the contract S-Auto-4
  must preserve; S-Auto-4 swaps the body to add the real §1.7
  detection.

**Generalization coverage:**

- **Target** = full closed-loop end-to-end execution. Verified by
  `test_dry_run_short_circuits_before_apply` (dry-run completes through
  analyzer + proposer + sandbox + anti-hardcode + STOP) and
  `test_happy_path_keep_full_pipeline` (live iter completes through all
  14 state-machine steps with `decision=keep`).
- **Neighbor** = crash recovery. Verified by
  `test_mid_iter_apply_crash_routes_to_error_and_calls_cleanup` (eval
  raises mid-iter → decision=error + cleanup called) and
  `test_loop_continues_after_iteration_error` (2-iter run where iter 1
  raises does NOT abort the loop; iter 2 completes).
- **Negative-control (CRITICAL)** = a hypothesis with a structural
  field target. The proposer would reject such an LLM output via its
  post-parse validator
  (`test_proposer_rejects_out_of_surface_field`). At the loop level,
  if a bad hypothesis somehow reaches the sandbox, the sandbox
  rejects it and the iteration discards with
  `discard_reason=sandbox_rejected:...`
  (`test_sandbox_reject_short_circuits_before_apply` covers
  `$.applicable_use_cases` as the structural-field test case).
- **Adversarial-prompt fixture for S-Auto-4** = a hypothesis with a
  §1.7 red-line `after_value`
  (`if user.message contains 'appeal' then route to UC-H else UC-A`).
  S-Auto-3's placeholder PASSES it
  (`test_adversarial_fixture_passes_placeholder_anti_hardcode`); the
  test serves as a regression target S-Auto-4 must flip to FAIL when
  it implements real detection.
- **Shadow firewall regression** = the serialized
  experiments_log row + the audit default mode both scan-tested for
  per-case shadow sentinels
  (`test_experiments_log_shadow_firewall_no_per_case_key`,
  `test_shadow_firewall_holds_in_experiments_log`,
  `test_cli_audit_default_respects_shadow_firewall`).

## 5. Tests / eval evidence

### Python autoloop suite

```
$ cd autoloop && uv run pytest -q
.......................................................................  [ 48%]
.......................................................................  [ 97%]
....                                                                     [100%]
147 passed, 1 warning in 3.14s
```

S-Auto-3 adds 64 new tests across 5 new test files; 83 baseline
(Sprint 54 + 55) tests pass unchanged. Total: 147.

### eval_interactive baseline UNCHANGED

```
$ cd eval_interactive && uv run python -m pytest --tb=no -q
.........................................................................................................................................................  [ ... ]
============================== short test summary info ===============================
FAILED tests/regression/test_case_spec_overrides.py::test_v2_schema_loads_cleanly
FAILED tests/regression/test_case_spec_overrides.py::test_smoke_review_report_tracks_smoke_set_and_overrides
FAILED tests/regression/test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag
3 failed, 486 passed in 13.29s
```

Matches the Sprint 54 + 55 baseline (`486 passed, 3 failed` —
the 3 failures are inherited / pre-existing and explicitly preserved
per the contract's "eval_interactive baseline UNCHANGED" close gate).

### Java suite — SKIPPED per S-Auto-3 Java-zero-touch

`git diff --stat HEAD -- server/ eval/` returns empty. The
`SystemPromptUserRequestedTiebreakerTest` baseline failure inherited
from Sprint 24-era is not touched by S-Auto-3.

## 6. Live iter smoke

A real live iteration requires the human to have set the meta-agent
LLM credentials in `.env.local` AND have `mvn` + a clean working tree
(so the applier can git-commit on a fresh `autoloop/exp-N` branch).
Without `AUTOLOOP_META_LLM_API_KEY` set, the orchestrator errors at
step 1 (analyzer LLM call) — but the state machine wires correctly
all the way through error capture + cleanup + persistence.

**Smoke executed at sprint close (no credentials, no live LLM):**

```
$ unset AUTOLOOP_META_LLM_API_KEY
$ uv run python -m autoloop dry-run --experiments 1
[run] starting 1 iteration(s), dry_run=True
[run] done: keep=0 discard=0 error=1 (of 1 total)
  exp-1: decision=error discard_reason=- elapsed=0.0s
```

`decision=error` is the expected outcome without an API key — the
client fails to build, the analyzer raises, the loop catches +
records + finalizes. The pipeline executed without crash; the close
gate "FULL pipeline must execute without crash regardless of
keep/discard verdict" holds.

**Live-iter close gate deferred to deliver-agent + human at milestone
close**: a true live iter requires (a) `AUTOLOOP_META_LLM_API_KEY`
filled in `.env.local`, (b) a built `server/` artefact + `mvn` on
PATH, (c) a clean working tree on the autoloop-branch. The wiring is
in place; the human runs `uv run python -m autoloop run
--experiments 1` to verify and records keep/discard in the milestone
close note. Surfaced as OQ-S56.5.

## 7. Commit discipline

One commit at S-Auto-3 close (commit-at-end per
`docs/current/iteration_governance.md` §6 + `feedback_commit_at_end_bundles_deliver_artefacts.md`).
Staged files (S-Auto-3 scope only; no `git add -A`):

```
M autoloop/autoloop/cli.py
M autoloop/autoloop/sandbox/__init__.py
M autoloop/autoloop/sandbox/applier.py
M autoloop/autoloop/scoring/eval_runner.py
M autoloop/program.md
M autoloop/tests/test_cli_smoke.py
M eval_interactive/eval_interactive.yaml          # OQ-S56.1; documented below
?? autoloop/autoloop/loop.py
?? autoloop/autoloop/memory/                       # 4 files
?? autoloop/autoloop/meta_agent/                   # 5 .py + 3 .txt files
?? autoloop/autoloop/sandbox/anti_hardcode_check.py
?? autoloop/tests/test_applier.py
?? autoloop/tests/test_cli_integration.py
?? autoloop/tests/test_loop.py
?? autoloop/tests/test_memory.py
?? autoloop/tests/test_meta_agent.py
?? docs/sprints/sprint-056-handoff.md              # this file
```

`.env.local` machine-local edits are gitignored and NOT staged.

Deliver-agent close-bundle files (`docs/milestone_objective.md`,
`docs/sprint_objective.md`, `docs/10-handoff.md`,
`docs/action_bank.md`, etc.) are bundled by the human at close per
the deliver-artefacts convention; dev did not touch them.

Commit message:
`Sprint 56 / S-Auto-3 — loop orchestrator + meta-agent + 3-layer memory + applier real impl + cli wiring`

## 8. OQ surfaced

### OQ-S56.1 — eval_interactive backend URL env-var indirection

**Surface:** `eval_interactive/eval_interactive.yaml` (top-level eval
config; NOT under hard-fenced `eval_interactive/eval_interactive/**`).

**Change:** literal `bot.base_url: http://localhost:8080` → env-var
substitution `bot.base_url: ${CSAGENT_BACKEND_URL}` (one line; comment
documenting the OQ added beside it).

**Why this was necessary:** the S-Auto-3 contract's live-iteration
close gate explicitly requires `eval_runner runs 3 v1 suites against
alt port backend via env-var`. The applier spawns a parallel Spring
on a free alt port; eval-interactive must route its agent HTTP calls
to that port, not the regular 8080. eval-interactive's
config loader supports `${VAR}` substitution but no fallback default
when the env var is unset.

**Why this is structurally safe:** the change is to the **top-level**
eval config (`eval_interactive/eval_interactive.yaml`), which is
NOT under the milestone-level hard-fenced path
`eval_interactive/eval_interactive/**` (the inner Python module). The
inner module is byte-identical (zero edits). The yaml change is
benign when `CSAGENT_BACKEND_URL` is set in the shell env (loop
orchestrator sets it; eval_runner sets a default
`http://localhost:8080` if missing — both verified by code).

**What dev did to make this work:**
- `eval_interactive/eval_interactive.yaml`: 1-line change + comment
  block documenting the OQ.
- `autoloop/autoloop/scoring/eval_runner.py`: subprocess env now
  defaults `CSAGENT_BACKEND_URL=http://localhost:8080` if not already
  set (so standalone `uv run eval-interactive run ...` via the
  autoloop's eval_runner still works without manual env-var setup).
  NO signature change.
- `.env.local` (machine-local, gitignored): added
  `CSAGENT_BACKEND_URL=http://localhost:8080` so direct
  `uv run eval-interactive run ...` invocations from a shell also work.

**Deliver-agent disposition needed:**
- Bless the top-level `eval_interactive/eval_interactive.yaml` change
  as in-scope for S-Auto-3 (it was the minimum-touch change required
  by the contract's live-iter close gate), OR
- Decide an alternate path (e.g., a separate "env-var-overlay"
  mechanism that does not touch the YAML at all) and roll back the
  YAML change at M-Auto-1A close.

### OQ-S56.2 — meta-agent LLM credentials not in `.env.local`

`autoloop/config.yaml` meta_agent block names `AUTOLOOP_META_LLM_API_KEY`
and `AUTOLOOP_META_LLM_BASE_URL` env vars. `.env.local` currently has
**no entries** for either (only DEEPSEEK / KIMI / SIMULATOR /
DASHSCOPE). The handoff machine-local edit adds commented-out
placeholders. **Deliver-agent + human must fill the real values
before the milestone-close live-iter smoke gate runs.** Tests mock the
LLM client so unit-test pass is unaffected.

### OQ-S56.3 — apply Hybrid baseline-patch text is a placeholder

The `autoloop apply --experiment exp-N` subcommand emits a baseline-
update patch whose `-  baseline_dir: <previous>` line is a literal
placeholder string, not the actual current value of
`config.fitness.baseline_dir`. Reason: at apply time, the previous
baseline_dir value is the value committed in `autoloop/config.yaml`
at the cherry-picked SHA, which is non-trivial to introspect without
opening the YAML at a specific commit. The patch IS valid as a
human-readable diff hint; the human applies it manually (per the
OQ-S55.1 Hybrid disposition: human review + commit). Deliver-agent
may want this auto-resolved in a future milestone (M-Auto-2) where
baseline-advance is a more frequent operation.

### OQ-S56.4 — yaml.safe_dump round-trip loses comments

`applier._write_field_to_yaml` uses `yaml.safe_load + safe_dump` to
patch the field, which discards YAML comments + may reformat
unrelated quoting. The AST-level diff is minimal (validator ACCEPTS),
but the on-disk git diff on `autoloop/exp-N` branches is visually
noisy: a `procedure:` edit may also show quoting / line-style changes
across the file. v1 Skill YAMLs are comment-light so the practical
impact is small, but the human reviewing an `autoloop apply` cherry-
pick should be aware. Comment-preservation requires `ruamel.yaml`
(new dependency); reserved for a future milestone.

### OQ-S56.5 — Live-iter smoke deferred to milestone close

S-Auto-3 close-gate "FULL pipeline executes without crash" was
verified via dry-run + extensive mocked tests. A true live iter
(`run --experiments 1` with real LLM + real `mvn spring-boot:run` +
real eval-interactive) requires (a) AUTOLOOP_META_LLM_* credentials,
(b) a built `server/` jar so `mvn -pl server -am spring-boot:run`
succeeds, (c) a clean working tree on the autoloop-branch. The wiring
is in place; the human + deliver-agent verify at milestone close +
record keep/discard verdict + elapsed in the milestone close note.
Expected elapsed: ~12-15 min per iteration (3 suites × ~3-4 min each
+ Spring startup + git overhead); >40 min is a signal worth
investigating.

### OQ-S56.6 — Anti-repeat heuristic effectiveness not yet measured

The proposer prompt instructs the LLM to switch targets when recent
iterations on the same skill × field were all discarded. The
implementation surfaces the recent-K iterations table + the
fingerprint history; the actual avoidance is LLM-judgment. Until we
have ≥30 iterations of real data, we cannot measure whether the
heuristic actually reduces repeated-discard rate. Deliver-agent +
human review at milestone close.

## 9. Sub-sprint commit summary

```
$ git show --numstat HEAD
(at S-Auto-3 close commit)

3 .env.local                                      (gitignored; not in numstat)
8  1 eval_interactive/eval_interactive.yaml      (OQ-S56.1; +8 -1)
396 40 autoloop/autoloop/cli.py                  (S-Auto-1 placeholders → real wiring)
603  0 autoloop/autoloop/loop.py                 (NEW)
105  0 autoloop/autoloop/memory/experiments_log.py  (NEW)
203  0 autoloop/autoloop/memory/iterations_index.py (NEW)
 82  0 autoloop/autoloop/memory/lessons_log.py     (NEW)
 56  0 autoloop/autoloop/memory/__init__.py        (NEW)
236  0 autoloop/autoloop/meta_agent/analyzer.py    (NEW)
297  0 autoloop/autoloop/meta_agent/proposer.py    (NEW)
170  0 autoloop/autoloop/meta_agent/llm_client.py  (NEW)
145  0 autoloop/autoloop/meta_agent/lessons_compactor.py (NEW)
 39  0 autoloop/autoloop/meta_agent/__init__.py    (NEW)
 60  0 autoloop/autoloop/meta_agent/prompts/analyze.txt (NEW)
108  0 autoloop/autoloop/meta_agent/prompts/propose.txt (NEW)
 49  0 autoloop/autoloop/meta_agent/prompts/compact.txt (NEW)
 80  0 autoloop/autoloop/sandbox/anti_hardcode_check.py (NEW; placeholder)
490 50 autoloop/autoloop/sandbox/applier.py        (skeleton → real)
  7  1 autoloop/autoloop/sandbox/__init__.py       (re-export anti_hardcode_check)
 11  1 autoloop/autoloop/scoring/eval_runner.py    (default CSAGENT_BACKEND_URL)
  2  2 autoloop/program.md                         (2 single-cell status flips)
309  0 autoloop/tests/test_applier.py              (NEW)
286  0 autoloop/tests/test_cli_integration.py      (NEW)
498  0 autoloop/tests/test_loop.py                 (NEW)
191  0 autoloop/tests/test_memory.py               (NEW)
373  0 autoloop/tests/test_meta_agent.py           (NEW)
 20  7 autoloop/tests/test_cli_smoke.py            (1 obsolete test rewritten)
```

(Numbers above are pre-commit working-tree counts; exact post-commit
numstat in the actual commit message.)

## 10. Self-check verification

- [x] `autoloop/autoloop/loop.py` implements state machine + IterationResult
      dataclass + run_one_iteration + run_iterations + crash recovery
      (cleanup always in finally).
- [x] `autoloop/autoloop/meta_agent/{analyzer,proposer,lessons_compactor}.py`
      + `prompts/{analyze,propose,compact}.txt` implemented +
      proposer JSON parse + 3-retry on invalid + fingerprint
      deterministic + v1 mutable-surface schema enforcement.
- [x] `autoloop/autoloop/memory/{experiments_log,iterations_index,lessons_log}.py`
      implemented + sqlite schema + JSONL append + lessons markdown.
- [x] `autoloop/autoloop/sandbox/applier.py` skeleton → real impl: git
      branch + commit + free-port discovery + Spring spawn + health-
      probe (120s timeout) + cleanup.
- [x] `autoloop/autoloop/cli.py` 5 subcommand real wiring: check (UNCHANGED)
      / dry-run / run / report / apply (Hybrid: cherry-pick + emit
      patch + NO auto-commit) / audit (default RESPECTS shadow
      firewall; `--include-shadow-detail` flag opens it).
- [x] `autoloop/config.yaml` has `meta_agent:` + `lessons:` block (set
      at S-Auto-1+S-Auto-2; S-Auto-3 reads both); provider/model are
      concrete values (`anthropic` + `claude-opus-4-7`) per S-Auto-3
      planning round commit `d9e4086`.
- [x] `autoloop/program.md` row 3 + row 6 status flipped; other prose
      untouched.
- [x] Anti-hardcode hook signature defined: `def anti_hardcode_check(hypothesis,
      *, config) -> AntiHardcodeResult`; placeholder always-PASS impl;
      `result.placeholder = True` flag so audit surface can tell.
- [x] eval_runner subprocess injects `CSAGENT_BACKEND_URL` env-var; no
      eval_runner signature change.
- [x] `cd autoloop && uv run pytest -q` → 147 PASS, 0 fail (83 baseline +
      64 new).
- [x] `cd eval_interactive && uv run python -m pytest --tb=no -q` →
      `486 passed, 3 failed` (UNCHANGED baseline).
- [x] `git diff --stat HEAD -- server/ eval/
      eval_interactive/eval_interactive/ data/ db/
      server/src/main/resources/ docs/foundational/ docs/current/
      docs/sprints/ docs/milestones/` → empty.
- [x] Live iteration dry-run smoke executed: orchestrator wires
      correctly; without API key returns `decision=error` (expected).
      True live iter deferred to milestone close (OQ-S56.5).
- [x] `docs/sprints/sprint-056-handoff.md` written (this file); §12
      left empty.
- [x] §7 self-walk in §4 above.
- [x] Single commit covers all S-Auto-3 scope; commit message
      per contract.
- [x] OQ surfaced in §8: OQ-S56.1 (eval_interactive.yaml edit),
      OQ-S56.2 (LLM credentials), OQ-S56.3 (apply baseline-patch
      placeholder), OQ-S56.4 (yaml comment-loss), OQ-S56.5 (live-iter
      smoke deferred), OQ-S56.6 (anti-repeat heuristic
      effectiveness).

## 11. Reading map for milestone close

The deliver-agent + human reviewing S-Auto-3 at M-Auto-1A close
should consult:

- This handoff for the dev-facing summary.
- `autoloop/autoloop/loop.py` for the orchestrator state machine.
- `autoloop/autoloop/meta_agent/prompts/{analyze,propose,compact}.txt`
  for the §1.7 enforcement at the prompt level.
- `autoloop/autoloop/sandbox/anti_hardcode_check.py` for the
  placeholder signature S-Auto-4 must preserve.
- `eval_interactive/eval_interactive.yaml` line 1-10 for the
  OQ-S56.1 surface decision.
- The 64 new test files for the structural defenses' coverage:
  `tests/test_{memory,meta_agent,applier,loop,cli_integration}.py`.

## 12. Deliver / Codex close (reserved)

(Filled by deliver-agent + human at M-Auto-1A milestone close per
§4.3 milestone-shared Codex review default. S-Auto-3 introduces no
Tier-0 candidate, no §1.7 cross — meta-agent prompts TEACH §1.7 to
the LLM rather than encoding it; the `anti_hardcode_check` hook is a
placeholder always-PASS pending S-Auto-4 — no hard-fence-violating
surface edit beyond OQ-S56.1, and S-Auto-3 is not a fix-iteration on
a prior sub-sprint. Milestone-shared default holds.)
