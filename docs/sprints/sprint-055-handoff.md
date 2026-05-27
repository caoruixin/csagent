---
title: Sprint 55 / M-Auto-1A S-Auto-2 — 5-layer fitness evaluator + shadow runner + baseline loader — dev handoff
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (sub-sprint dev archive); autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader}.py + autoloop/tests/test_{tier_evaluator,eval_runner,baseline_loader}.py + autoloop/config.yaml (code)
last_reviewed: 2026-05-27
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Second sub-sprint of Milestone M-Auto-1A. S-Auto-2 builds the
  fitness-evaluation surface that S-Auto-3 wires into the loop:
  `autoloop/scoring/tier_evaluator.py` (5-layer lexicographic verdict
  with shadow firewall), `autoloop/scoring/eval_runner.py` (subprocess
  wrapper around `eval-interactive run`; mvn/spring-boot/git
  hard-fenced by grep test), `autoloop/scoring/baseline_loader.py`
  (explicit-pointer baseline snapshot reader with per-suite +
  flat fallback). `autoloop/config.yaml` gains a `fitness:` block
  (v1 narrow dataset: bad_cases + anchor_outcome + shadow; anchor
  159 excluded per `docs/milestone_objective.md` §2; Layer 3
  improvement-threshold mode = absolute case_count per 2026-05-27
  human-locked decision). 35 new pytest tests pass (78-88 expected;
  actual 35 new = 17 tier_evaluator + 7 eval_runner + 11
  baseline_loader; total 48+35=83 autoloop tests). Zero edits to
  `server/`, `eval/`, `eval_interactive/eval_interactive/`, `data/`,
  `db/`, `server/src/main/resources/`, `docs/foundational/`,
  `docs/current/`, or any sprint/milestone archive. eval_interactive
  baseline reproduces `486 passed, 3 failed`. Java suite skipped per
  S-Auto-2 Java-zero-touch (verified via
  `git diff --stat HEAD -- server/ eval/` returning empty). §12
  reserved for deliver-agent + human at milestone close (M-Auto-1A
  uses milestone-shared Codex per §4.3; S-Auto-2 introduces no
  Tier-0 candidate, no §1.7 cross, no hard-fence-violating surface
  edit, and is not a fix-iteration — milestone-shared default holds).
---

# Sprint 55 / M-Auto-1A S-Auto-2 — 5-layer fitness evaluator + shadow runner + baseline loader — dev handoff

## 1. Goal and outcome

**Goal (from `docs/sprint_objective.md` / `compact/sprint-055-dev-prompt.md`):**
Build the fitness-evaluation surface that S-Auto-3 wires into the
auto-loop. Concretely: (a) `tier_evaluator.py` with a 5-layer
lexicographic verdict and a structural shadow-result firewall, (b)
`eval_runner.py` as a thin subprocess wrapper around
`eval-interactive run` for the v1 fitness suite (bad_cases +
anchor_outcome + shadow), (c) `baseline_loader.py` to read a baseline
results directory into a `BaselineSnapshot` consumed by the
evaluator, plus (d) a `fitness:` block in `autoloop/config.yaml`
capturing the v1 dataset scope + thresholds + the explicit baseline
pointer. CONSUMES the existing `eval_interactive/results/*.json`
schema unchanged; CONSUMES the existing `eval-interactive run` CLI
as a black box. Does NOT live-invoke any eval (S-Auto-3 territory)
— all S-Auto-2 tests are synthetic-fixture-based or mocked.

**Outcome:** delivered against contract.

- 4 new source files under `autoloop/autoloop/scoring/`:
  `__init__.py` (re-export surface), `tier_evaluator.py` (753 LOC,
  5 layers + helpers + shadow firewall), `eval_runner.py` (181 LOC,
  `run_suite` + `run_v1_fitness_suite` + `EvalRunnerTimeoutError`),
  `baseline_loader.py` (238 LOC, `load` + `BaselineSnapshot` +
  `SuiteSnapshot` + `BaselineLoadError`).
- 3 new test files under `autoloop/tests/`: `test_tier_evaluator.py`
  (17 tests), `test_eval_runner.py` (7 tests), `test_baseline_loader.py`
  (11 tests). 35 new tests pass.
- `autoloop/config.yaml` extended with a `fitness:` block (3 suites,
  thresholds, baseline pointer placeholder, timeout, parallel_suites).
- `autoloop/program.md` §4 row 2 status field flipped to
  **DELIVERED** with a 1-sentence v1 dataset-scope footnote (single
  row touched).
- `autoloop/README.md` CLI table `dry-run` / `run` rows annotated
  "fitness evaluator delivered by S-Auto-2, loop wiring lands in
  S-Auto-3" (2 cells touched).

## 2. Scope (per `compact/sprint-055-dev-prompt.md` §#1-#6)

### #1 — `autoloop/scoring/tier_evaluator.py` — DELIVERED

5-layer lexicographic verdict implemented per the
`docs/solutions/auto_evolution_skill_driven_v1.md` §3.3 contract,
narrowed to the v1 dataset (bad_cases + anchor_outcome + shadow;
anchor 159 absent by construction).

- **Layer 0 — Tier-0 safety floor**: combines two surfaces.
  - Java replay 11 hard gates (`critical_policy_violation`,
    `wrong_containment`, `groundedness_pass_rate`,
    `escalation_recall`, `handover_completeness`,
    `tool_scope_violation`, `forbidden_phrase`, `budget_enforcement`,
    `phase_transition_validity`, `critical_high_risk_escalation`,
    `out_of_scope_detection`) — read from `tier_breakdown` block in
    the current run's results.json **if present**. As noted in §
    "Schema check" below, eval-interactive does NOT currently
    produce a `tier_breakdown` block in its results.json (the Java
    GateEvaluator pipeline produces these metrics separately).
    Contract says "若有; missing → 'skipped' + 记 reason"; we mark
    each gate `{"status": "skipped", "reason": "not produced by
    eval-interactive"}` and continue. The path is wired so that if
    a future S-Auto-N stage emits tier_breakdown into results.json,
    the gate fires without further code changes.
  - Python `hard_checks` Tier-0 family across all suite cases:
    `no_pii_leakage`, `no_human_only_tool_exposure`,
    `no_critical_policy_violation`, `escalation_compliance`,
    `phase_transition_validity`. Per-case `l1_results[].check` is
    iterated; any `passed: false` discards with reason
    `tier0_<check>_failed_on_<case_id>`.
- **Layer 1 — Tier-1 outcome non-regression**: programmatic
  `case_passed` counts for bad_cases (strict no-regression) and
  anchor_outcome (max-drop bounded by
  `config.fitness.anchor_outcome_max_drop_cases`; default 0).
  L3 `user_goal_achievement` is intentionally NOT consulted (v1
  hard-gate-only discipline per contract).
- **Layer 2 — Tier-2 critical-flow non-regression**: mandatory
  failure count across (anchor_outcome + bad_cases) computed as
  `tier2_result.per_step[].severity == "mandatory" AND outcome ==
  "FAIL"`. Both aggregate and per-UC (keyed by `primary_uc`) buckets
  are checked. Any rise → discard.
- **Layer 3 — improvement threshold**: absolute case-count per
  2026-05-27 human-locked decision. At least one of (bad_cases
  programmatic passed delta, anchor_outcome programmatic passed
  delta, Tier-2 mandatory failure REDUCTION) must be ≥
  `config.fitness.improvement_min_cases` (default 1) for the
  verdict to keep.
- **Layer 4 — shadow regression** with structural firewall.
  Aggregate-only: `LayerResult.metrics_observed` contains ONLY
  `{baseline_pass_rate, current_pass_rate, drop_pct,
  regression_detected, max_drop_pct, warning}`. Per-case shadow
  failures are routed exclusively through a separate
  `ShadowAuditDetail` dataclass returned only when the caller
  invokes `evaluate(..., audit=True)`. The default API (`audit=False`)
  returns ONLY `LexicographicVerdict`; the meta-agent / loop
  orchestrator never sees per-case shadow info. Missing shadow data
  (suite absent in current or baseline) → Layer 4 SKIPPED with
  warning instead of failing the verdict (cannot enforce a gate on
  missing data).

Lexicographic short-circuit: layers are evaluated in order; the first
failing layer sets `decision="discard"` and `discard_reason`; layers
N+1..4 carry `passed=None` with reason
`not_evaluated_short_circuit_at_layer_<N>`.

### #2 — `autoloop/scoring/eval_runner.py` — DELIVERED

`SuiteRunSpec` + `SuiteRunResult` + `EvalRunnerTimeoutError` +
`run_suite` + `run_v1_fitness_suite` implemented exactly per
contract:

- `run_suite(spec, results_root, config, timeout_seconds=1800)`
  invokes `uv run eval-interactive run --path <spec.path> --parallel
  <spec.parallel> --output-dir <results_root>/<spec.name>/` with
  `cwd = <repo_root>/eval_interactive` and `env = os.environ.copy()`
  (no env-var rewriting). Timeout raises
  `EvalRunnerTimeoutError(suite, elapsed)`. Non-zero exit captures
  last 50 stderr lines into `SuiteRunResult.error_tail`.
- `run_v1_fitness_suite(results_root, config)` runs the 3 v1 suites
  SEQUENTIALLY when `config.fitness.parallel_suites=False` (v1
  default). `parallel_suites=True` raises `NotImplementedError` —
  M-Auto-2 may implement it after evaluating LLM-provider rate-limit
  interaction with bad-case session-establishment flakiness.
- Hard fence: a grep test
  (`test_eval_runner_source_does_not_invoke_forbidden_tools`) scans
  the eval_runner.py source (excluding docstrings + comments) for
  `mvn `, `"mvn"`, `spring-boot`, `git ` tokens. Test passes — no
  forbidden invocation reaches the loop's subprocess surface.

### #3 — `autoloop/scoring/baseline_loader.py` — DELIVERED

- `load(baseline_dir, *, config) -> BaselineSnapshot` reads
  per-suite subdirs (`<baseline_dir>/<suite>/results.json`) by
  default; falls back to a flat `<baseline_dir>/results.json` only
  if per-suite subdirs are absent.
- Missing suite → `SuiteSnapshot.case_passed_count = None` +
  `warning` field set + `warnings.warn(...)` emitted. The
  tier_evaluator treats this as "no baseline → cannot enforce gate"
  (Layer 1/2 short-circuit on the missing-suite path; Layer 4
  shadow-missing keeps with warning).
- `BaselineLoadError` raised on (a) baseline_dir does not exist,
  (b) baseline_dir is not a directory, (c) malformed JSON inside
  any per-suite results.json, (d) empty `config.fitness.suites`.
- Explicit-pointer policy: NO "latest results dir" heuristic. The
  caller resolves `config.fitness.baseline_dir` against the repo
  root and passes the literal path.

### #4 — `autoloop/config.yaml` `fitness:` block — DELIVERED

Appended to existing config (S-Auto-1 fields untouched):

```yaml
fitness:
  suites:
    - name: bad_cases
      path: eval_interactive/case_specs/bad_cases/
      parallel: 1
    - name: anchor_outcome
      path: eval_interactive/case_specs/anchor_outcome/
      parallel: 4
    - name: shadow
      path: eval_interactive/case_specs_shadow/
      parallel: 4
  improvement_threshold_mode: case_count
  improvement_min_cases: 1
  shadow_max_drop_pct: 3.0
  anchor_outcome_max_drop_cases: 0
  baseline_dir: eval_interactive/results/<PLACEHOLDER-set-at-M-Auto-1A-close>
  eval_suite_timeout_seconds: 1800
  parallel_suites: false
```

The `baseline_dir` literal placeholder is intentional: the
deliver-agent + human bless the actual baseline run path at
M-Auto-1A close OR at first S-Auto-3 live-iteration. Surfacing this
as an explicit ceremony (rather than auto-latest) is what makes the
baseline non-shifting under the loop.

### #5 — Tests — DELIVERED (35 new tests)

`autoloop/tests/test_tier_evaluator.py` (17 tests):

- Layer 0 (3): clean PASS / `no_pii_leakage` discard /
  `tier_breakdown.critical_policy_violation` Java-replay discard.
- Layer 1 (3): bad_cases 5→4 regression discard / anchor_outcome
  10→9 regression discard / clean-pass proceeds.
- Layer 2 (3): aggregate Tier-2 increase discard / per-UC UC-H 1→2
  discard / Tier-2 clean-pass proceeds.
- Layer 3 (4): bad_cases +1 keep / anchor_outcome +1 keep / Tier-2
  mandatory reduction keep / no-improvement discard.
- Layer 4 (3): shadow 5% drop discard / shadow 2% drop keep / shadow
  missing keep-with-warning.
- Adversarial lexicographic (1): bad_cases improves AND Tier-2
  regresses → MUST discard at Layer 2 (Constitution §1.6 invariant).
- Shadow firewall (2): default `audit=False` API leaks no
  per-case shadow info (verified by JSON-serializing the entire
  verdict and asserting a sentinel `case_id` does not appear);
  `audit=True` API returns `ShadowAuditDetail` with per-case
  failures.
- Short-circuit (1): Layer 0 fail → Layers 1..4 all carry
  `passed=None` with `not_evaluated_short_circuit_at_layer_0` reason.

`autoloop/tests/test_eval_runner.py` (7 tests):

- SuiteRunSpec v1 defaults (bad_cases=1, others=4).
- `run_suite` mocked success returns correct paths + CLI shape.
- `run_suite` `TimeoutExpired` → `EvalRunnerTimeoutError`.
- `run_suite` non-zero exit + 100-line stderr → captures last 50
  lines into `error_tail`.
- `run_v1_fitness_suite` runs 3 suites sequentially in deterministic
  order (`bad_cases`, `anchor_outcome`, `shadow`).
- `run_v1_fitness_suite` with `parallel_suites=True` raises
  `NotImplementedError`.
- **Hard-fence grep test**: scans eval_runner.py source for
  forbidden tokens (`mvn `, `"mvn"`, `spring-boot`, `git `) outside
  docstrings/comments. Passes.
- `subprocess.run` cwd argument is `<repo>/eval_interactive` (S-Auto-2
  contract clause).

`autoloop/tests/test_baseline_loader.py` (11 tests):

- Happy path: 3 suites loaded, counts correct, Tier-2 per-UC bucketing.
- Missing shadow suite → `None` counts + warning recorded both in
  `SuiteSnapshot.warning` and `BaselineSnapshot.warnings` and a
  Python `UserWarning` emitted.
- Per-UC Tier-2 mandatory bucketing aggregates correctly across
  multiple cases.
- Missing baseline_dir → `BaselineLoadError("does not exist")`.
- Malformed results.json → `BaselineLoadError("malformed JSON")`.
- Empty `config.fitness.suites` → `BaselineLoadError`.
- Flat `<baseline_dir>/results.json` fallback: attributes ALL cases
  to each requested suite with an explicit warning that
  `source_suite` is unknown.

### #6 — `autoloop/scoring/__init__.py` + program.md + README.md — DELIVERED

- `autoloop/scoring/__init__.py` re-exports the public surface
  S-Auto-3 will import: `evaluate`, `LexicographicVerdict`,
  `LayerResult`, `ShadowAuditDetail`, `run_suite`,
  `run_v1_fitness_suite`, `SuiteRunSpec`, `SuiteRunResult`,
  `EvalRunnerTimeoutError`, `load`, `BaselineSnapshot`,
  `SuiteSnapshot`, `BaselineLoadError`.
- `autoloop/program.md` §4 row 2 single-line status flip to
  **DELIVERED** with the v1 narrow-dataset pointer. No other prose
  in program.md touched (contract: "只动一行").
- `autoloop/README.md` CLI table: `dry-run` / `run` rows annotated.

## 3. Hard fences honoured

S-Auto-2 hard fences from `compact/sprint-055-dev-prompt.md`:

- **No touch** to `eval_interactive/eval_interactive/**`: confirmed
  via `git diff --stat HEAD -- eval_interactive/eval_interactive/`
  returning empty.
- **No touch** to any case_spec: confirmed via
  `git diff --stat HEAD -- eval_interactive/case_specs/
  eval_interactive/case_specs_shadow/` returning empty.
- **No invocation** of `mvn` / `spring-boot:run` / `git` /
  non-`eval-interactive` subprocess in eval_runner.py: confirmed by
  grep test (passes).
- **No live `eval-interactive run`** in S-Auto-2 tests: confirmed by
  reading test files — every subprocess interaction is
  `unittest.mock.patch("autoloop.scoring.eval_runner.subprocess.run", ...)`.
- **No touch** to S-Auto-1 territory: `autoloop/sandbox/`,
  `autoloop/cli.py` UNCHANGED; `autoloop/program.md` changed one row
  (§4 row 2 status); `autoloop/config.yaml` extended via append-only
  (S-Auto-1 fields under `mutable_surface:` / `paths:` / `lessons:`
  unchanged).
- **No edit** to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`,
  `docs/current/`, `docs/sprints/*` archives, `docs/milestones/*`
  archives, `docs/codex-findings.md`: confirmed via `git diff --stat
  HEAD --` against each path returning empty.
- **No new heavy dependencies**: stdlib (`json`, `subprocess`,
  `time`, `os`, `warnings`, `dataclasses`, `pathlib`, `datetime`)
  only. PyYAML is already a S-Auto-1 dependency but not directly
  imported by scoring/ — config.yaml parsing flows via the caller.
- **No anchor (159) in v1 fitness path**: confirmed by inspecting
  `autoloop/config.yaml` `fitness.suites` — only `bad_cases`,
  `anchor_outcome`, `shadow` are listed.
- **No L3 `user_goal_achievement` in any hard gate**: confirmed by
  grepping tier_evaluator.py — the string does not appear.
- **No smoke composite_score / task_success_rate in any gate**:
  confirmed by grepping tier_evaluator.py — neither string appears.
- **No shadow per-case failure exposed via loop-facing API**:
  enforced by code structure (`evaluate(audit=False)` returns
  `LexicographicVerdict` whose Layer 4 `metrics_observed` is
  aggregate-only). Verified by the JSON-roundtrip test
  `test_shadow_firewall_default_api_no_per_case_data`.

## 4. §7 Layer-classification + anti-hardcode self-walk

**Target failure layer:** `eval_spec` (§3.2 Q6 boundary). S-Auto-2
builds the harness that CONSUMES already-validated M3-Eval
signals; it does not modify scoring, judge, or CaseSpec. The
fitness evaluator IS the structural definition of "what the loop
optimizes for" — making this definition lexicographic + shadow-firewalled
+ validated-surface-only is itself the §1.6 / §1.7 defense. Per the
sub-sprint contract, this is the right classification: S-Auto-2
does not move a UC decision, escalation posture, or follow-up
policy; it scaffolds the evaluation machinery.

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant.
S-Auto-2 consumes existing Tier-0 Java replay metrics (when present
in results.json) + Python hard_checks Tier-0 family; it does NOT
promote any new check to Tier-0. C2/C3 DEFER continues per M2-close
verdict.

**Semantic hardcode:** No semantic hardcode introduced.
`tier_evaluator.py` is a pure metric-comparison function. Every
comparison is numeric (`count_a > count_b`, `delta >= n`); there is
NO regex matching on bot output text, NO keyword list for "did the
bot do the right thing", NO if-else over UC. Per-UC bucketing at
Layer 2 reads `case_results[].primary_uc` as a registry value (not a
decision); per-UC failure-rate comparison treats UCs symmetrically
(UC-A failures rising triggers discard exactly the same way UC-H
failures rising does). Thresholds (`improvement_min_cases=1`,
`shadow_max_drop_pct=3.0`, `anchor_outcome_max_drop_cases=0`) are
config-driven numbers, not semantic decisions; the human tunes them
via `autoloop/config.yaml`. The structural shadow firewall is the
opposite of a hardcode — it removes a surface (per-case shadow) from
the loop's optimization space.

**Generalization coverage:** target = 11+ discard-fixtures + 3
clean-pass fixtures across the 5 layers, with one critical
adversarial fixture validating that Layer 3 improvement does NOT
compensate for Layer 2 regression (§1.6 invariant). Neighbor = edge
cases for missing baseline / missing shadow / per-UC bucketing /
flat-vs-per-suite results.json layout. Negative-control = 3
clean-pass fixtures (one per "good iteration shape") that produce
`decision="keep"` cleanly. Shadow = dedicated test asserting the
loop-facing API surface NEVER carries per-case shadow info
(JSON-serialized verdict scanned for sentinel `case_id`); audit-only
API verified to return `ShadowAuditDetail` carrying per-case shadow
info ONLY when explicitly requested.

## 5. Files touched + footprint

```
M  autoloop/config.yaml                                  (+48 / -0)
M  autoloop/program.md                                   (+1 / -1)
M  autoloop/README.md                                    (+2 / -2)
A  autoloop/autoloop/scoring/__init__.py                 (48 LOC)
A  autoloop/autoloop/scoring/baseline_loader.py          (238 LOC)
A  autoloop/autoloop/scoring/eval_runner.py              (181 LOC)
A  autoloop/autoloop/scoring/tier_evaluator.py           (753 LOC, includes docstrings + helpers)
A  autoloop/tests/test_baseline_loader.py                (197 LOC)
A  autoloop/tests/test_eval_runner.py                    (251 LOC)
A  autoloop/tests/test_tier_evaluator.py                 (640 LOC)
A  docs/sprints/sprint-055-handoff.md                    (this file)
```

Zero edits under `server/`, `eval/`, `eval_interactive/eval_interactive/`,
`eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`,
`data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`,
`docs/runtime_freeze_and_risk_policy.md`, `docs/current/`,
`docs/sprints/sprint-001-*` through `docs/sprints/sprint-054-*`,
or any `docs/milestones/` archive.

## 6. Baselines + gates

### Python autoloop suite

```
$ cd autoloop && uv run pytest -q
............................................................. [100%]
83 passed, 1 warning in 0.83s
```

Sprint 54 baseline was 48 passed; S-Auto-2 adds 35 new tests
(17 tier_evaluator + 7 eval_runner + 11 baseline_loader). Sprint 54
test files (`test_cli_smoke.py`, `test_yaml_diff_validator.py`)
unchanged. The 1 warning is `test_layer4_shadow_missing_keeps_with_warning`
intentionally triggering `warnings.warn("baseline missing suite
'shadow'")` to verify the missing-suite path; the warning is part of
the asserted behaviour.

### Python eval_interactive suite

```
$ cd eval_interactive && uv run python -m pytest --tb=no -q
... (truncated)
3 failed, 486 passed in 13.58s
```

Matches the inherited baseline `486 passed, 3 failed`. The 3
failures are all pre-existing case_spec / corpus-lint failures
unchanged since Sprint 54 close.

### Java suite

Skipped per S-Auto-2 Java-zero-touch exemption. Verified via:

```
$ git diff --stat HEAD -- server/ eval/ | grep src/main/java
(empty)
```

S-Auto-2 modifies zero Java files; the inherited Sprint 24-era
`SystemPromptUserRequestedTiebreakerTest` failure baseline carries
forward unchanged.

### Sandbox + CLI self-test (regression check)

S-Auto-1's sandbox self-test (`uv run autoloop check`) is not
re-run in S-Auto-2; the CLI is unchanged and a re-invocation would
duplicate Sprint 54 baseline. Confirmed by `git diff --stat HEAD --
autoloop/autoloop/cli.py autoloop/autoloop/sandbox/` returning
empty.

## 7. Schema check

Per the §"Test / eval requirements" + §"Hard fences / STOP conditions"
of the sub-sprint contract, dev inspected a real M5-close-era
results.json before implementing the schema-coupled paths in
`tier_evaluator.py` and `baseline_loader.py`. Source examined:
`eval_interactive/results/20260525-100613/results.json` (most
recent results dir, M5-close era).

Observed schema (relevant to S-Auto-2):

```
Top-level keys: run_id, label, timestamp, elapsed_ms, summary, case_results
Per case_results[i]: case_id, primary_uc, expected_outcome,
                     case_passed_authority, session_id, total_turns,
                     stop_reason, elapsed_ms, case_passed, composite_score,
                     outcome_score, judge_score, failure_tags, stall_detected,
                     stall_failure_tag, containment_outcome, active_use_case,
                     escalation_reason, l1_results, l2_results, l3_results,
                     tier2_result, transcript, status, contract_warnings,
                     llm_calls, per_turn_trace
Per case_results[i].tier2_result: passed, severity, failed_step_ids, detail,
                                  per_step (each: step_id, desc, outcome,
                                  severity, detail)
Per case_results[i].l1_results[]: check, passed, detail
```

Observations vs sub-sprint contract assumptions:

- **`case_results[].source_suite` does NOT exist** in the schema.
  Cases are identified by `case_id` only. The sub-sprint contract
  anticipates this via the §"STOP-and-surface" clause and offers
  resolutions (a) match case_id to suite directory listing OR
  (b) open an R-item for follow-on eval_interactive schema
  enrichment.

  **Resolution chosen: (a) implicit via directory layout.** The
  eval_runner contract already invokes each suite separately with
  its own `--output-dir <root>/<suite>/`. So per-suite results.json
  files are produced; `baseline_loader.load` consumes them per-suite
  under `<baseline_dir>/<suite>/results.json`, and tier_evaluator
  mirrors that layout for `current_results`. The `source_suite`
  field is structurally unnecessary in this design — the suite IS
  the directory containing the results.json. A flat
  `<baseline_dir>/results.json` fallback IS supported for symmetry
  with legacy flat-output runs, with an explicit warning that
  source_suite cannot be inferred.

  **No follow-on R-item opened** — schema gap is structurally
  resolved by per-suite output dirs, not deferred to
  eval_interactive enrichment.

- **`case_results[].case_passed` exists** as a boolean — used as
  the Layer 1 / Layer 3 / Layer 4 programmatic count.
  `case_passed_authority` distinguishes `"programmatic"` vs
  `"judge"` (we use `case_passed` regardless; v1 hard-gate-only).

- **`case_results[].primary_uc` exists** (values `UC-A`, `UC-H`,
  etc.) — used as the per-UC bucketing key for Layer 2.

- **`case_results[].tier2_result.per_step[].severity`** is one of
  `"mandatory"` / `"advisory"`; `.outcome` is `"PASS"` / `"FAIL"`.
  Layer 2 mandatory-failure count =
  `sum(s.severity=="mandatory" AND s.outcome=="FAIL")`. Confirmed
  this is the correct field path (NOT `tier2_result.mandatory_failures`,
  which does not exist as a top-level field). Sub-sprint contract
  used `tier2_result.mandatory_failures` as a placeholder — we
  resolve it to the structurally correct
  `tier2_result.per_step[].severity + outcome` filter.

- **`case_results[].l1_results[].check` and `.passed`** exist
  with the expected names. The Tier-0 Python family check names
  in `eval_interactive/eval_interactive/scoring/hard_checks.py`
  `HardChecker.ALL_CHECKS` include all 5 names the contract calls
  out (`no_pii_leakage`, `no_human_only_tool_exposure`,
  `no_critical_policy_violation`, `escalation_compliance`,
  `phase_transition_validity`). Layer 0 Python family checks fire
  exactly as the contract anticipated.

- **Java replay 11 hard gates are NOT in eval-interactive's
  results.json.** The Java GateEvaluator pipeline
  (`eval/src/main/java/com/gumtree/csagent/eval/metrics/GateEvaluator.java`)
  produces these in its own JSON artefact, separate from
  eval-interactive's output. The sub-sprint contract anticipates
  this: "若有; missing → 'skipped' + 记 reason". Layer 0 marks each
  Java gate `{"status": "skipped", "reason": "not produced by
  eval-interactive"}` and continues to the Python Tier-0 family
  check, which DOES fire on every case.

  **Disposition**: this is contract-compliant ("若有" branch). The
  Java replay surface remains an OPTIONAL Layer 0 input. If a
  future S-Auto-N stage decides the loop must enforce the Java
  gates, the path to do so is to wire `eval-interactive run` to
  also emit a `tier_breakdown` block in its results.json — that is
  an eval_interactive change, outside S-Auto-2 scope. The S-Auto-2
  code is forward-compatible: a test fixture demonstrates that when
  results.json DOES carry `tier_breakdown.critical_policy_violation
  > 0`, Layer 0 discards correctly.

No other contract assumptions diverged from the actual schema.

## 8. R-items consumed / surfaced

**Consumed:** none. S-Auto-2 is pure scaffolding for the
fitness-evaluation surface; it does not close any
`docs/action_bank.md` R-item.

**Surfaced (candidate; deliver-agent to confirm at milestone close):**

- **R-S55-eval-runner-error-tail-budget** (low): the 50-line stderr
  tail is a reasonable v1 default; if S-Auto-3 live-iteration runs
  surface that the error envelope from `eval-interactive run` is
  systematically larger (e.g. Python traceback chains in
  rate-limit scenarios), tighten or widen the window. Logged as a
  candidate at milestone close.

## 9. OQ surfaced

Open questions that deliver-agent + human need to triage before
S-Auto-3 starts (or, where noted, defer):

- **OQ-S55.1 — baseline_dir pointer transition ceremony**: the
  `config.fitness.baseline_dir` field is a literal pointer to one
  specific `eval_interactive/results/<run-id>/` directory. When a
  kept iteration is cherry-picked to main (S-Auto-3 onward), the
  baseline should typically advance — otherwise the next iteration
  measures against a baseline that no longer reflects post-merge
  reality. Is the human-update-only model (deliver-agent + human
  manually edit `baseline_dir` after each kept cherry-pick) the
  right ceremony, or should `autoloop/cli.py apply` automatically
  update `baseline_dir` as part of the cherry-pick? The latter has
  the advantage of keeping the pointer in lockstep with main; the
  former has the advantage of making baseline advances an explicit,
  audit-traceable human decision. Deliver-agent recommendation
  before S-Auto-3 design.

- **OQ-S55.2 — per-UC Tier-2 baseline for NEW UCs**: Layer 2's
  per-UC check compares each UC's mandatory failure count to its
  baseline. If a new UC is introduced between baseline-capture and
  current-run (e.g. a new case_family in case_specs/), the current
  run will have entries for that UC but the baseline will not. The
  current Layer 2 implementation treats baseline-missing-UC as
  baseline=0, so any current UC mandatory failures > 0 trigger
  discard. Is this the desired semantics, or should new UCs be
  treated as exempt-from-gate until they have a baseline? Likely
  defer to first S-Auto-3 live-iteration evidence.

- **OQ-S55.3 — improvement_threshold_mode calibration revisit
  after first M-Auto-1B run**: the `case_count` mode with
  `min_cases=1` is the right v1 default for 12+12=24 cases, but
  may become noise-bound after enough kept iterations land. After
  the first M-Auto-1B overnight run produces real data, the
  deliver-agent should look at the distribution of bad_cases /
  anchor_outcome flip-rates iteration-to-iteration and decide
  whether `min_cases=2` (or a percent mode at larger N) better
  matches the "real improvement vs noise" signal. Documented as a
  candidate R-item, not a S-Auto-2 blocker.

- **OQ-S55.4 — Java replay surface wiring**: Layer 0 currently
  skips the Java replay 11 gates because eval-interactive does not
  emit `tier_breakdown`. If at any point the loop's safety-floor
  enforcement is judged insufficient based on Python Tier-0 family
  alone, the path to upgrade is to wire eval-interactive to emit a
  `tier_breakdown` block (or alternatively to add a separate Java
  GateEvaluator invocation step to eval_runner.py, which would
  cross the S-Auto-2 hard fence on subprocess types). Tracked for
  M-Auto-2+ visibility.

## 10. Backend / dev-sandbox context

S-Auto-2 invokes no backend; the eval_runner.py subprocess wrapper
is dormant in S-Auto-2 (only mocked-subprocess tests fire). No
`mvn spring-boot:run` was started by this sub-sprint; the
`memory_restart_backend_before_eyeball.md` cache invalidation rule
does not apply to this work.

## 11. Self-check vs sprint-contract closing checklist

- [x] `autoloop/scoring/` directory created with `__init__.py` +
      `tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py`.
- [x] `evaluate(...)` implements 5 layers + lexicographic
      short-circuit + shadow firewall (default API never leaks
      per-case shadow).
- [x] `eval_runner.py` enforces hard fence (no
      `mvn`/`spring-boot`/`git` subprocess; grep test passes).
- [x] `baseline_loader.py` uses explicit-pointer strategy (no
      auto-latest); missing-suite produces warning + None counts.
- [x] `autoloop/config.yaml` `fitness:` block appended (S-Auto-1
      fields untouched); `baseline_dir` left as
      `<PLACEHOLDER-set-at-M-Auto-1A-close>` literal.
- [x] `autoloop/scoring/__init__.py` re-exports the public surface
      S-Auto-3 will import.
- [x] `autoloop/program.md` §4 row 2 status flipped to DELIVERED +
      v1 dataset-scope pointer (single row touched; no other prose
      edited).
- [x] `autoloop/README.md` CLI table `dry-run` / `run` row status
      annotations updated.
- [x] `cd autoloop && uv run pytest -q` → 83 passed (Sprint 54 48 +
      S-Auto-2 35 new); Sprint 54 tests untouched.
- [x] `cd eval_interactive && uv run python -m pytest --tb=no -q`
      → `486 passed, 3 failed` baseline reproduces.
- [x] `git diff --stat HEAD -- server/ eval/ eval_interactive/eval_interactive/
      data/ db/ server/src/main/resources/ docs/foundational/ docs/current/
      docs/sprints/ docs/milestones/` returns empty.
- [x] Schema-check completed (`eval_interactive/results/20260525-100613/results.json`
      inspected); divergences from contract documented in §7.
- [x] `docs/sprints/sprint-055-handoff.md` complete (this file);
      §12 left empty for deliver-agent + human at milestone close.
- [x] §7 stanza self-walk recorded in §4 of this handoff
      (Target layer = `eval_spec`; Tier-0 = none; Semantic hardcode
      = none; Generalization = target / neighbor / negative /
      shadow / adversarial covered).
- [x] OQ surfaced (4 candidates documented in §9).

## 12. Sub-sprint close verdict (deliver-agent + human)

*[Reserved for deliver-agent + human at M-Auto-1A milestone close.
S-Auto-2 follows the §4.3 milestone-shared Codex default: no
per-sub-sprint Codex trigger fires (no Tier-0 candidate; no §1.7
cross; no hard-fenced surface edit per S-Auto-2 hard fence; not a
fix-iteration on a prior sub-sprint). The S-Auto-2 commit range is
consumed by Codex at M-Auto-1A close together with S-Auto-1 /
S-Auto-3 / S-Auto-4.]*
