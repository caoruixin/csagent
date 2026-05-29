---
title: Sprint objective — Sprint 060 / M-Auto-1B S-Auto-7 — Substrate fix (Blocker A + Blocker B + smoke iter)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-29
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-059-objective.md]
superseded_by: null
notes: >
  THIRD sub-sprint of Milestone M-Auto-1B (extended from the original
  2-sub-sprint sequence to 4 per S-Auto-6 close-empty in-place
  revision; §8.5 ceiling = 5 sub-sprints; margin = 1 for fix-iteration
  S-Auto-7.1 if needed). S-Auto-7 resolves the two substrate
  pre-flight blockers surfaced at S-Auto-6 close-empty
  (`docs/sprints/sprint-059-handoff.md` §0 + §12):

    Blocker A — `autoloop/config.yaml:110` `fitness.baseline_dir`
    still literal `<PLACEHOLDER-set-at-M-Auto-1A-close>` string;
    Layer 3 trivializes against `... or 0` fallback at
    `tier_evaluator.py:485-532`.

    Blocker B — `autoloop/autoloop/scoring/eval_runner.py:99-110`
    invokes `eval-interactive run --output-dir <suite_out_dir>`
    against a Click flag that has NEVER existed in
    `eval_interactive/eval_interactive/cli.py` at any commit
    (verified `git log -S "output_dir" -- eval_interactive/eval_interactive/cli.py`
    empty). Every overnight iter at Step 7 would Click-fail.

  Human-locked S-Auto-6 close decisions 2026-05-29 (AskUserQuestion):
  (a) Class C — In-flight downgrade classification for S-Auto-6;
  (b) S-Auto-7 scope = substrate-fix ONLY (overnight + cherry-pick
  deferred to S-Auto-8 as the original S-Auto-6 scope reattempted);
  (c) Blocker B fix path = (b) REMOVE `--output-dir` from
  `eval_runner.py` + adapt to auto-timestamped output + rebaseline
  scoring_code_baseline_sha (smaller blast radius than CLI-side add).

  Class = `infra` (substrate-fix; structural plumbing repair; no
  semantic decision change). §7 stanza EXEMPT per pure-infra carve-out
  (eval_runner.py change is HOW the subprocess consumes results, NOT
  WHAT eval reads or grades; case_specs / judge / detector / projection
  / scoring semantic logic all UNCHANGED). §7 self-walked for
  paper-trail completeness only.

  Codex review plan: **PER-SUB-SPRINT REQUIRED per §4.3 trigger #3**
  (hard-fenced surface that the milestone objective explicitly named
  out of scope). M-Auto-1B §6 fence #13 locks
  `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py`
  at content-hash `5177b674...`; S-Auto-7 explicitly overrides the
  fence for `eval_runner.py` ONLY + re-baselines the hash. Codex must
  independently verify the controlled fence #13 override at S-Auto-7
  close BEFORE S-Auto-8 overnight can dispatch.

  Builds on S-Auto-6 close-empty `1943ed5` 2026-05-29. The substrate
  state at S-Auto-7 open: 17-fixture detector sweep PASS (S-Auto-5
  Fix-C); calibrated `synonym_map_enabled: true` (S-Auto-5 step 2 Path
  A); `R-S57` closed; `R-S58` open (defer to M-Auto-1B planning round
  AFTER S-Auto-8 overnight evidence per S-Auto-6 §9 recommendation).
---

# Sprint 060 / M-Auto-1B S-Auto-7 — Substrate fix (Blocker A + Blocker B + smoke iter)

## Class

`infra` (§3.2 Q1 — substrate-fix + baseline rebaseline; structural plumbing repair on the eval_runner subprocess invocation + `BaselineSnapshot` source advance; no semantic decision change, no projection / scoring semantic logic edit, no CaseSpec / judge change). **§7 EXEMPT** per pure-infra carve-out + self-walked for paper-trail completeness (similar to S-Auto-1 / S-Auto-3 / S-Auto-6 self-walk pattern). The eval_runner.py modification is HOW the subprocess consumes eval-interactive's output, NOT WHAT eval reads or grades; the calibrated anti-hardcode detector + 17-fixture sweep + 3 detector self-discipline regression tests all PASS unchanged.

## Goal

S-Auto-7 close 时:

1. **Blocker B resolved (eval_runner fix path (b) — must land FIRST to enable Blocker A blessing).** `autoloop/autoloop/scoring/eval_runner.py:99-110` modified: REMOVE the `--output-dir` argument from the subprocess invocation; adapt `run_v1_fitness_suite` to locate eval-interactive's auto-timestamped output directory (e.g., `ls -t eval_interactive/results/ | head -1` post-subprocess to identify the most recent timestamped run; optionally copy or symlink the `results.json` to a stable per-iter path under `autoloop/results/runs/<iter-id>/eval/` for `tier_evaluator.evaluate` consumption). Verified by single-suite end-to-end smoke: `python -c "from autoloop.scoring.eval_runner import run_v1_fitness_suite; r = run_v1_fitness_suite(...); assert r.exit_code == 0 and r.results_root.exists()"` returns success against a real `eval-interactive run --path case_specs/<suite>/` invocation. The `eval_interactive/eval_interactive/cli.py` is UNCHANGED (Blocker B fix path (a) NOT taken; smaller blast radius preserved).

2. **scoring_code_baseline_sha rebaselined.** Run `python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha(config_path='autoloop/config.yaml'))"` post-edit to compute the new content hash over the four scoring files (`tier_evaluator.py` + `eval_runner.py` [modified] + `baseline_loader.py` + `gaming.py`). Update `autoloop/config.yaml` `fitness.scoring_code_baseline_sha` from M-Auto-1A close `5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c` to the new hash. Verify silent steady state: `_check_scoring_code_drift(config=config)` returns `[]` (no `gaming.scoring_code_drift.sha_changed` ERROR fires in the next iteration). This is a controlled fence #13 override with documented re-baselining — precedent at M-Auto-1A close commit `b6b627b` (S-Auto-4 close-day fix-up: literal git commit id corrected to content-hash form).

3. **Blocker A resolved — bless concrete baseline_dir.** Run the v1 47-case fitness suite ONCE on `auto-loop-branch` HEAD with the repaired eval_runner (Steps 1+2 above must land before this step can succeed): `bad_cases` ×12 (parallel=1 per S-Auto-2 default for `R-bad-case-parallel-session-establishment-flakiness`) + `anchor_outcome` ×12 (parallel=4) + `shadow` ×23 (parallel=4). Bless the resulting eval-interactive auto-timestamped directory `eval_interactive/results/<concrete-run-id>` as the M-Auto-1B baseline by updating `autoloop/config.yaml` `fitness.baseline_dir` from the literal `<PLACEHOLDER-set-at-M-Auto-1A-close>` string to the concrete path. Record per-suite `case_passed` count + per-case verdict snapshot in handoff §X "Baseline blessing evidence". This is the first time a non-empty `BaselineSnapshot` is loaded by `loop.py:386-441` → Layer 3 improvement-threshold gate becomes meaningful AND Layer 4 shadow regression `shadow_max_drop_pct: 3.0%` denominator becomes non-zero.

4. **Smoke iter end-to-end verification.** Run `python -m autoloop run --experiments 1` (NO `--dry-run`) on `auto-loop-branch` against the now-blessed baseline + repaired eval_runner. The full 14-step state machine must complete through Step 7 (eval_runner) AND Step 9 (tier_evaluator) — both previously unreachable in S-Auto-5 + S-Auto-6. Acceptance: (a) iteration terminal verdict is keep / discard / error (any of three acceptable); (b) `autoloop/results/runs/exp-<N>/iteration_record.json` contains non-empty `tier_evaluator_verdict` with non-degenerate Layer 0-4 outcomes (each Layer reports PASS / FAIL / informational metric rather than the "missing baseline" short-circuit pattern from S-Auto-5); (c) per-iter elapsed time recorded (the FIRST measurement of full Spring-spawn + 47-case-eval cycle including the meta-agent propose call; previously bounded only by Spring-spawn step at S-Auto-5 exp-2 122.1s). If smoke crashes in an unhandled path NOT caused by Blockers A/B, STOP and surface — substrate brittleness may require fix-iteration S-Auto-7.1.

5. **Per-sub-sprint Codex `pass`** (§4.3 trigger #3). Deliver-agent dispatches Codex at S-Auto-7 close (NOT at open) with `compact/sprint-060-codex-review-prompt.md` self-contained per §9 invariant. Codex independently verifies: (i) Blocker B fix path (b) structural soundness — auto-timestamp consumption is plumbing-correct, no new semantic logic introduced; (ii) the new `scoring_code_baseline_sha` matches actual `_compute_scoring_code_sha()` output post-edit (reproduces independently); (iii) the controlled fence #13 override is justified — Blocker B forces it, no other path enables overnight in M-Auto-1B Stage 1; (iv) the blessed `baseline_dir` points to a real concrete eval-interactive run with non-zero `case_passed` counts per suite; (v) smoke iter reached Step 9 with non-degenerate `tier_evaluator_verdict`. Codex must return `pass / 0` (or `approve with downgrade-to-signal follow-up` for any residual substrate brittleness observation) BEFORE S-Auto-8 overnight starts.

**Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,baseline_loader,gaming}.py` (the other three scoring files in fence #13 group; only `eval_runner.py` is overridden). **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py` (S-Auto-5 calibrated detector + S-Auto-1/3/4 substrate — UNCHANGED). **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` / `cli.py`. **Zero touch** to `eval_interactive/eval_interactive/**` (fix path (b) does NOT modify the eval-interactive CLI), `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`. **Zero touch** to `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/` (NO cherry-pick in S-Auto-7; that's S-Auto-8 scope only), `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives, `docs/codex-findings.md` scaffold.

## Scope (numbered; this is the contract)

### #1 — Blocker B fix path (b): adapt eval_runner.py to auto-timestamped output

Read `autoloop/autoloop/scoring/eval_runner.py` first to understand current `run_v1_fitness_suite` API + helpers + `SuiteRunResult` shape.

Modify the subprocess invocation (current state HEAD lines 99-110):

```python
cmd = [
    "uv", "run", "eval-interactive", "run",
    "--path", str(spec.path),
    "--parallel", str(spec.parallel),
    "--output-dir", str(suite_out_dir.resolve()),  # REMOVE this line
]
```

To:

```python
cmd = [
    "uv", "run", "eval-interactive", "run",
    "--path", str(spec.path),
    "--parallel", str(spec.parallel),
]
```

After the subprocess completes, locate the auto-timestamped output. eval-interactive writes to `eval_interactive/results/<YYYYMMDD-HHMMSS>/`. Two viable approaches:

**Approach A (filesystem mtime-based)**: snapshot `eval_interactive/results/` directory listing BEFORE the subprocess invocation; after, find the NEW directory created. Avoid race conditions with concurrent eval-interactive invocations (S-Auto-7 single-thread; not a concern in practice but document).

**Approach B (results.json self-locating)**: pass `--results-base eval_interactive/results/` (if the flag exists; verify; likely no) OR rely on eval-interactive's deterministic timestamp + match-by-suite-path post-subprocess.

Pick the simpler approach (likely A; the filesystem snapshot pattern is well-tested in subprocess tooling). Document the approach in inline comments. The `suite_out_dir` parameter the original `run_v1_fitness_suite` accepted should be re-interpreted as a STAGING target: after the subprocess writes to its auto-timestamped path, copy or symlink the `results.json` (or the entire `<timestamp>/` directory) to `suite_out_dir` for downstream `tier_evaluator.evaluate` consumption. Adapt `SuiteRunResult.results_root` to point to the staged location.

Verify by direct subprocess invocation (no loop integration yet):

```bash
cd autoloop && uv run --extra dev python -c "
from pathlib import Path
from autoloop.scoring.eval_runner import run_v1_fitness_suite, SuiteSpec
spec = SuiteSpec(path=Path('../eval_interactive/case_specs/bad_cases'), parallel=1)
r = run_v1_fitness_suite(spec, Path('/tmp/test_suite_out'))
print(f'exit_code={r.exit_code}, results_root={r.results_root}, results_root.exists()={r.results_root.exists() if r.results_root else False}')
"
```

Expected: `exit_code=0`, `results_root` points to a path containing `results.json` with non-zero `case_results` count.

**If subprocess invocation crashes** (e.g., `eval-interactive` itself errors against the suite path): STOP and surface — may be a deeper substrate brittleness beyond Blocker B.

**If `--output-dir` flag was somehow added to eval-interactive in the interim** (`git log -S "output_dir" -- eval_interactive/eval_interactive/cli.py` non-empty post-S-Auto-7-open): STOP and surface — the planning input has shifted, deliver-agent + human re-decide.

### #2 — scoring_code_baseline_sha rebaseline

Post-#1 edit, compute the new content hash. The `_compute_scoring_code_sha()` helper at `autoloop/autoloop/scoring/gaming.py` reads the four scoring files via the config path and hashes their content:

```bash
cd autoloop && uv run --extra dev python -c "
from autoloop.scoring.gaming import _compute_scoring_code_sha
sha = _compute_scoring_code_sha(config_path='config.yaml')
print(sha)
"
```

(Replace `config_path='config.yaml'` with whatever invocation form `_compute_scoring_code_sha` accepts; sample the helper signature first.)

The output is a 64-char hex SHA-256. Update `autoloop/config.yaml`:

```yaml
fitness:
  baseline_dir: ...
  scoring_code_baseline_sha: <new-64-char-hex>  # Was: 5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c (M-Auto-1A close hash); S-Auto-7 rebaselined 2026-05-29 after Blocker B fix path (b) modified eval_runner.py per `docs/sprints/sprint-060-handoff.md` §X.
```

(Include the inline comment with the precedent annotation + cross-reference.)

Verify silent steady state:

```bash
cd autoloop && uv run --extra dev python -c "
import yaml
from pathlib import Path
from autoloop.scoring.gaming import _check_scoring_code_drift
config = yaml.safe_load(Path('config.yaml').read_text())
flags = _check_scoring_code_drift(config=config)
assert flags == [], f'Expected empty drift list; got {flags}'
print('PASS: scoring_code_drift silent')
"
```

If the assertion fails, the new hash does not match — recompute + re-update.

### #3 — Blocker A blessing: run 47-case fitness baseline + advance baseline_dir

With the repaired eval_runner + valid scoring SHA, run the v1 47-case fitness suite ONCE on `auto-loop-branch` HEAD. Three suites in sequence (or parallel where permitted):

```bash
# Snapshot eval_interactive/results/ before to identify the new directories created
cd eval_interactive && ls -t results/ > /tmp/results_before.txt

# Suite 1: bad_cases (parallel=1 per R-bad-case-parallel-session-establishment-flakiness)
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1

# Suite 2: anchor_outcome (parallel=4)
cd eval_interactive && uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4

# Suite 3: shadow (parallel=4)
cd eval_interactive && uv run eval-interactive run --path case_specs_shadow/ --parallel 4

# Identify the 3 NEW run-ids created
cd eval_interactive && ls -t results/ | head -5
```

Three NEW timestamped run-ids should appear in `eval_interactive/results/`. The baseline directory pattern needs to consume all three suite outputs.

**Reading the `tier_evaluator.evaluate` + `baseline_loader.load_baseline_snapshot` API contract is required at this step**: the M-Auto-1B baseline may need to be ONE directory containing aggregated per-suite results OR THREE separate directories per suite. Read `autoloop/autoloop/scoring/baseline_loader.py` to understand the expected directory shape. If the loader expects ONE directory with subdirs per suite, copy/symlink the three eval-interactive outputs into a baseline aggregation directory (e.g., `eval_interactive/results/m-auto-1b-baseline-<YYYYMMDD>/{bad_cases,anchor_outcome,shadow}/`). If it expects three separate paths, configure accordingly.

Update `autoloop/config.yaml` `fitness.baseline_dir` from the literal `<PLACEHOLDER-set-at-M-Auto-1A-close>` string to the concrete blessed path. Record:

- The three eval-interactive run-ids.
- Per-suite `case_passed` count + per-case verdict from each `results.json`.
- The blessed `baseline_dir` value in config.yaml.

In handoff §X "Baseline blessing evidence".

**If the baseline run surfaces unexpected behaviour** (e.g., a suite errors out entirely, or per-case verdict shape is unexpected): STOP and surface. The baseline blessing must be high-quality — it anchors all future M-Auto-1B + post-cherry-pick evaluations.

### #4 — Smoke iter end-to-end verification

With Blockers A + B + scoring SHA rebaseline all landed, run the smoke iter:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 1
```

(NO `--dry-run`.) The full 14-step state machine must execute end-to-end. Critically: Step 7 (`eval_runner.run_v1_fitness_suite`) AND Step 9 (`tier_evaluator.evaluate`) must reach non-degenerate outputs — both were structurally unreachable in S-Auto-5 (failed before Step 7) and S-Auto-6 (closed empty).

Record:

- Iteration outcome (keep / discard / error; any acceptable per scope acceptance bar).
- Per-iter wall-clock elapsed time (the FIRST measurement of full Spring-spawn + 47-case-eval cycle).
- `autoloop/results/runs/exp-<N>/iteration_record.json` contents — verify `tier_evaluator_verdict` is non-empty + Layer 0-4 outcomes are non-degenerate (each Layer reports PASS/FAIL/informational metric, NOT the empty-snapshot short-circuit pattern from S-Auto-5).
- `autoloop/results/runs/exp-<N>/` artefacts: `hypothesis.json`, `diff.yaml`, `sandbox_verdict.json`, `anti_hardcode_verdict.json`, `tier_evaluator_verdict.json`, eval per-suite outputs.

**If smoke iter crashes in an unhandled path NOT caused by Blockers A/B**: STOP and surface. Substrate brittleness may require fix-iteration S-Auto-7.1.

**If per-iter elapsed >40 min**: continue this iteration to completion but STOP additional iterations (S-Auto-7 only requires 1 smoke iter). Surface as observation toward §10 substrate optimization R-item for M-Auto-2.

### #5 — Codex per-sub-sprint review prep

Author the handoff (per §11 Handoff requirements below). Do NOT dispatch Codex; deliver-agent dispatches at S-Auto-7 close with `compact/sprint-060-codex-review-prompt.md` (deliver-agent authors at close).

Pre-mitigation against Codex friction (your job before commit):

- Verify `git diff --stat 1943ed5..HEAD` (or whatever the actual commit base is) shows ONLY: `autoloop/autoloop/scoring/eval_runner.py`, `autoloop/config.yaml`, `autoloop/tests/test_eval_runner.py` (new or extended), `docs/sprints/sprint-060-handoff.md`. NO other file changed.
- Verify `git diff --stat -- autoloop/autoloop/scoring/tier_evaluator.py autoloop/autoloop/scoring/baseline_loader.py autoloop/autoloop/scoring/gaming.py` returns empty (the other three scoring files in fence #13 group UNCHANGED — only `eval_runner.py` overridden).
- Verify `git diff --stat -- eval_interactive/eval_interactive/cli.py` returns empty (fix path (b) preserved — eval-interactive CLI byte-identical).
- Reproducibility: include in handoff the exact `_compute_scoring_code_sha()` invocation + output (so Codex can reproduce).

## Hard fences / STOP conditions

- **CONTROLLED FENCE #13 OVERRIDE**: `autoloop/autoloop/scoring/eval_runner.py` is the ONLY file in the fence #13 content-hash-locked group that may be touched. The other three (`tier_evaluator.py` / `baseline_loader.py` / `gaming.py`) STAY untouched. After edit, the override is finalized by re-baselining `scoring_code_baseline_sha` in config.yaml; the fence reasserts at the NEW hash at S-Auto-7 close.
- **Zero touch** to `eval_interactive/eval_interactive/cli.py` (fix path (b) preserves eval-interactive byte-identical; fix path (a) was NOT chosen).
- **Zero touch** to `eval_interactive/eval_interactive/**` (the inner Python module beyond cli.py).
- **Zero touch** to `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`.
- **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py` (S-Auto-1/3/4/5 substrate + calibrated detector).
- **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` / `cli.py`.
- **Zero touch** to `server/src/main/java/**`, `eval/src/main/java/**`, `data/`, `db/`, `server/src/main/resources/` (cherry-pick to skills/ is S-Auto-8 scope ONLY).
- **Zero touch** to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives (other than S-Auto-7's own), milestone archives, `docs/codex-findings.md` scaffold.
- **No new heavy deps** in `autoloop/pyproject.toml`. Stdlib `pathlib` + `subprocess` only (the same dep posture S-Auto-2 ships with).
- **No LLM call** in `eval_runner.py` or its tests (substrate stays deterministic).
- **No `git add -A`** — stage S-Auto-7 scope explicitly.
- **No cherry-pick to main** in S-Auto-7 (M-Auto-1B fence #7 / #17 allows EXACTLY ONCE in S-Auto-8 ONLY).
- **STOP and surface** conditions:
  - `--output-dir` flag has been added to eval-interactive CLI in the interim (planning input shifted; deliver-agent + human re-decide).
  - Direct subprocess invocation of repaired eval_runner crashes for a reason beyond Blocker B.
  - Baseline run (47-case fitness suite) surfaces unexpected per-case verdict shape OR per-suite errors.
  - Smoke iter crashes in unhandled path NOT caused by Blockers A/B (fix-iteration S-Auto-7.1 candidate).
  - Smoke iter elapsed >40 min (substrate optimization R-item for M-Auto-2).
  - `_check_scoring_code_drift(config=config)` returns non-empty after #2 rebaseline (hash mismatch — recompute + re-update; if persistent, surface).
  - `tier_evaluator_verdict` is still degenerate (empty Layer 0-4) post-smoke despite blessed baseline (deeper structural issue in `loop.py` or `tier_evaluator.py`; fix-iteration S-Auto-7.1).

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run --extra dev pytest -q` — S-Auto-5 close baseline `223 passed, 1 warning` MUST grow by ~3-6 new S-Auto-7 tests (`test_eval_runner.py` new tests for auto-timestamp consumption + `SuiteRunResult.results_root` shape + exit_code propagation; possibly +1 for the `_compute_scoring_code_sha` rebaseline assertion if test_gaming.py needs updating with the new fixture hash). Total `226-229 passed, 1 warning`. The 1 warning is the S-Auto-2 baseline-missing-shadow asserted behaviour UNCHANGED.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed` (OQ-S47.3 env-specific failures).
- **Java baseline UNCHANGED**: skipped per Java-zero-touch (verify `git diff --stat 1943ed5..HEAD -- server/src/main/java/ eval/src/main/java/` returns empty).
- **17-fixture detector calibration sweep**: `cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py` UNCHANGED at 31 passed (S-Auto-7 does NOT touch the detector).
- **Detector self-discipline 3 regression tests**: UNCHANGED — PASS.
- **scoring_code_baseline_sha**: NEW value matches actual `_compute_scoring_code_sha()` output post-edit; verified by `_check_scoring_code_drift(config=config) == []`.
- **Live-iter end-to-end smoke**: 1 iter completed end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`.
- **No live LLM call in pytest tests** — eval_runner tests use synthetic-fixture or mocked-subprocess only.

## §7 — Layer-classification + anti-hardcode stanza (EXEMPT; self-walked for paper-trail)

**§7 stanza is NOT REQUIRED for pure-infra substrate-fix sub-sprints per `iteration_governance.md` §7**. Self-walked here for paper-trail completeness only:

**Target failure layer:** `infra` (§3.2 Q1 — substrate-fix; eval_runner.py is structural plumbing wrapping eval-interactive subprocess; the change is HOW the subprocess consumes output, NOT a semantic decision). Baseline blessing is a configuration advance, not a runtime semantic change.

**Tier-0 invariant:** adds no Tier-0 invariant. eval_runner.py modification is structural plumbing repair; the calibrated detector + sandbox + content_validator + tier_evaluator + gaming + meta_agent + loop + memory all UNCHANGED. The existing Q2 detector rule (Tier-0 invariant invention detection) is UNCHANGED.

**Semantic hardcode:** No semantic hardcode introduced. Justification:

- eval_runner.py fix path (b) is a **subprocess invocation pattern adjustment** — removes a non-existent CLI flag + adds filesystem auto-timestamp consumption. No regex, no keyword list, no if-else decision branch, no per-UC matrix. Pure plumbing.
- `scoring_code_baseline_sha` rebaseline is a config field update with the recomputed content hash; no rule logic changes.
- `baseline_dir` advance from placeholder to concrete eval-interactive run path is a config field update; no semantic logic changes.
- Smoke iter execution exercises the existing semantic logic (S-Auto-3/4/5 substrate + calibrated detector + meta-agent propose) WITHOUT modifying any of it.

**Generalization coverage:**

- **target** = (i) eval_runner.run_v1_fitness_suite end-to-end against real `eval-interactive run` subprocess returns `SuiteRunResult` with non-empty `results_root` containing `results.json`; (ii) scoring_code_drift detector silent on rebaselined config; (iii) smoke iter reaches Step 9 with non-degenerate `tier_evaluator_verdict`.
- **neighbor** = autoloop pytest suite UNCHANGED baseline + 3-6 new tests on eval_runner.py adaptation. The other three fence #13 scoring files PASS unchanged (not modified).
- **negative-control** = `_check_scoring_code_drift` returns `[]` post-rebaseline (steady state); `_RE_WHEN_WORD_BOUNDARY` detector behaviour UNCHANGED on the 17-fixture sweep.
- **shadow** = shadow firewall posture UNCHANGED. Shadow leak signatures + tier_evaluator Layer 4 logic UNCHANGED.

## Codex review plan (§4.3)

**PER-SUB-SPRINT REQUIRED — §4.3 trigger #3**. The S-Auto-7 sub-sprint explicitly overrides M-Auto-1B §6 fence #13 (`autoloop/autoloop/scoring/` content-hash lock); the override is BLESSED at S-Auto-7 planning round (the milestone objective in-place revision 2026-05-29 documenting the controlled override) but must be VERIFIED at S-Auto-7 close by Codex independently.

**Codex prompt timing**: deliver-agent authors `compact/sprint-060-codex-review-prompt.md` at S-Auto-7 close (NOT at open), covering the actual delivered commit range.

**Codex must verify**:

1. Blocker B fix path (b) structural soundness — `eval_runner.py` modification is plumbing only (subprocess invocation + filesystem auto-timestamp consumption + `SuiteRunResult` adaptation); no new semantic logic introduced. The change does NOT add regex / keyword / if-else / enum that encodes a semantic decision (§1.7 forbidden surface check).
2. The new `scoring_code_baseline_sha` matches actual `_compute_scoring_code_sha()` output post-edit. Codex independently reproduces the hash computation + assertion.
3. The controlled fence #13 override is justified — Blocker B forces it (`eval-interactive run --output-dir` Click error blocks every overnight iter at Step 7); no other path enables overnight in M-Auto-1B Stage 1. The override is minimal-blast-radius (only `eval_runner.py` touched in the fence #13 group; the other three scoring files UNCHANGED).
4. The blessed `baseline_dir` points to a real concrete eval-interactive run with non-zero `case_passed` counts per suite (Codex spot-checks by reading the blessed `eval_interactive/results/<concrete-run-id>/results.json`).
5. Smoke iter `iteration_record.json` `tier_evaluator_verdict` is non-degenerate (Layer 0-4 outcomes each PASS / FAIL / informational metric; no empty-snapshot short-circuit pattern).
6. Hard-fence verification against the M-Auto-1B §6 17 fences for surfaces OTHER than the controlled fence #13 override — `git diff --stat 1943ed5..HEAD -- <all gated paths except eval_runner.py>` returns empty.
7. `eval_interactive/eval_interactive/cli.py` UNCHANGED (fix path (b) preserved; fix path (a) NOT taken).

**Verdict expected**: `pass / 0` or `approve with downgrade-to-signal follow-up` (likely if any residual substrate brittleness observation surfaces — e.g., Spring spawn rate, eval-interactive auto-timestamp race-condition consideration).

`reject as semantic hardcode` triggers fix-iteration sub-sprint S-Auto-7.1 BEFORE S-Auto-8 can dispatch.

## Handoff requirements

Author `docs/sprints/sprint-060-handoff.md` at S-Auto-7 close. Leave **§12** empty (deliver-agent + human at milestone close). Required sections:

- **§1 Class + §7 stanza self-walk** — confirm class `infra` + §7 EXEMPT classification against delivered scope.
- **§2 Goal achievement** — bullet each of the 5 Goal items + evidence pointer.
- **§3 Scope execution log** — for each scope step #1-#5, brief status (DONE / PARTIAL / SKIPPED-WITH-REASON).
- **§4 Blocker B fix evidence** — `git show --numstat` for the `eval_runner.py` modification; before/after diff with key lines; direct subprocess invocation result; SuiteRunResult shape post-edit.
- **§5 scoring_code_baseline_sha rebaseline evidence** — old hash (`5177b674...`) + new hash + `_compute_scoring_code_sha()` reproducibility check + `_check_scoring_code_drift` silent confirmation.
- **§6 Baseline blessing evidence** — three eval-interactive run-ids; per-suite case_passed counts; per-case verdict snapshot (summary table); blessed `baseline_dir` config value post-edit.
- **§7 Smoke iter end-to-end record** — iteration outcome (keep/discard/error); per-iter elapsed time; `iteration_record.json` `tier_evaluator_verdict` Layer 0-4 outcomes (non-degenerate verification); cumulative artefact tree under `autoloop/results/runs/exp-<N>/`.
- **§8 Adversarial spot-check pre-Codex** — independent verification of (i) fence #13 override scope (only eval_runner.py touched); (ii) eval-interactive CLI unchanged; (iii) scoring SHA reproducibility.
- **§9 Code anchor table** — `git show --numstat <s-auto-7-commit-sha>` table format showing file paths + lines added/removed.
- **§10 Test count** — autoloop pytest baseline 223 → final count + delta breakdown.
- **§11 §7 stanza self-walk verification** — explicit "self-walk passed; §7 EXEMPT confirmed".
- **§12 OQ-S60.x list** — open questions surfaced; disposition per OQ.

Author `compact/sprint-060-codex-review-prompt.md` AS PART OF the S-Auto-7 close-bundle (deliver-agent writes this; embeds §4.1 nine-question kernel verbatim + fence #13 override justification + 7 verification axes above + M-Auto-1B §6 hard fences for non-overridden surfaces).

## Commit discipline

Dev stages **only S-Auto-7 scope** explicitly:

- Modified `autoloop/autoloop/scoring/eval_runner.py` (Blocker B fix path (b)).
- Modified `autoloop/config.yaml` (`fitness.baseline_dir` advance + `fitness.scoring_code_baseline_sha` re-baselined).
- New or extended `autoloop/tests/test_eval_runner.py` (3-6 new tests).
- Optional: if the autoloop subsystem ships a small helper for auto-timestamp consumption (e.g., a `_locate_latest_results` helper), include in `eval_runner.py` body or a new sibling file (deliver-agent + human discuss at planning if sibling file needed).
- New `docs/sprints/sprint-060-handoff.md`.

**No `git add -A`**. **No bundle of deliver-agent close artefacts** (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/codex-findings.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `compact/sprint-060-codex-review-prompt.md`) — those bundle by human at close per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

**Two-commit pattern** (analogous to S-Auto-5 commit-pattern):

**Commit 1 (REQUIRED; Blocker B + scoring SHA rebaseline + tests)**:

```
Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker B fix path (b) + scoring SHA rebaseline + tests

[2-3 sentences describing the eval_runner.py change + new tests + scoring SHA before/after]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage: `autoloop/autoloop/scoring/eval_runner.py`, `autoloop/config.yaml` (`scoring_code_baseline_sha` only — NOT `baseline_dir` yet), `autoloop/tests/test_eval_runner.py`.

**Commit 2 (REQUIRED; Blocker A blessing + smoke iter + handoff)**:

```
Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker A baseline_dir blessing + smoke iter

[2-3 sentences describing the blessed baseline run-ids + per-suite case_passed counts + smoke iter outcome + per-iter elapsed]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage: `autoloop/config.yaml` (`baseline_dir` only), `docs/sprints/sprint-060-handoff.md`.

(Splitting into two commits separates the "fence override + rebaseline" structural change from the "baseline blessing + smoke" verification step; clean audit trail for Codex.)

## Scope size (§8.5 note)

M-Auto-1B with S-Auto-7 = 3 of 4 sub-sprints planned (within §8.5 5-sub-sprint ceiling; margin = 1 if fix-iteration S-Auto-7.1 needed). S-Auto-7 is small-medium by LOC + test count (~3-6 new tests + ~30-100 LOC in eval_runner.py adaptation + ~10 LOC config edits); estimated 2-3 dev-days + Codex ~1-2 days. The fence #13 override + scoring SHA rebaseline are the high-risk items; the baseline blessing + smoke are straightforward execution.

## OQ (open questions — filled during the sub-sprint)

- **OQ-S60.x candidates** (expected; dev surfaces as ambiguities encountered):
  - **OQ-S60.1** — eval-interactive's auto-timestamped output format: directory shape per-suite (does `--path case_specs/bad_cases/` produce a `bad_cases/` subdir under the timestamp, or just a flat `results.json`? Verify).
  - **OQ-S60.2** — `baseline_loader.load_baseline_snapshot` API contract: expects ONE aggregated baseline directory OR THREE separate per-suite directories. Read the loader BEFORE running the 47-case baseline + designing the directory structure.
  - **OQ-S60.3** — eval-interactive subprocess elapsed times: bad_cases (12 cases parallel=1; ~30s/case → 6 min); anchor_outcome (12 cases parallel=4 → 1.5 min); shadow (23 cases parallel=4 → 3 min). Actual times for handoff §6 + smoke iter §7.
  - **OQ-S60.4** — Smoke iter elapsed time vs. S-Auto-6 contract estimate (12-25 min target; 40 min cap).
  - **OQ-S60.5** — Whether `_compute_scoring_code_sha()` is path-configurable OR hard-codes file paths. Read the helper signature BEFORE running #2.
  - **OQ-S60.6** — Whether `autoloop/results/runs/exp-<smoke>/eval/` staging directory pattern needs creation OR if `SuiteRunResult.results_root` directly points to the eval-interactive auto-timestamped path. Plumbing design decision in #1.
  - **OQ-S60.7** — Spring spawn success rate on smoke iter (S-Auto-5 OQ-S58.7 substrate observation: 1/3 = 33% failure on exp-2; first single-iter measurement post-Blocker-B).
  - **OQ-S60.8** — Whether `--results-base` or equivalent flag exists in eval-interactive CLI (the simpler path if it does). Verify via `eval-interactive run --help`.

Add OQ entries as ambiguities are encountered; record disposition per OQ in handoff §12.
