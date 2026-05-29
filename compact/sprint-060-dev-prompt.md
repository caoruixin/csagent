# Sprint 060 / M-Auto-1B S-Auto-7 — Dev Prompt

## Role identity

你是 **dev agent for Sprint 060 / M-Auto-1B sub-sprint S-Auto-7 — Substrate fix (Blocker A + Blocker B + smoke iter)**.

**One-sentence goal**: resolve the two substrate pre-flight blockers surfaced at S-Auto-6 close-empty (`autoloop/config.yaml:110` `fitness.baseline_dir` placeholder; `autoloop/autoloop/scoring/eval_runner.py:99-110` invokes non-existent `--output-dir` CLI flag) via Blocker B fix path (b) human-locked at S-Auto-6 close (REMOVE `--output-dir` from eval_runner + adapt to eval-interactive's auto-timestamped output + rebaseline `scoring_code_baseline_sha`), then bless a concrete `baseline_dir` by running the 47-case fitness suite, then smoke-verify Step 9 reaches non-degenerate `tier_evaluator_verdict` for the first time in M-Auto-1B.

This prompt is your **self-contained executable view** per `iteration_governance.md` §9 invariant. You do NOT need to read any other repo doc (governance chain auto-loaded via `AGENTS.md`).

S-Auto-7 explicitly overrides **M-Auto-1B §6 fence #13** (scoring code content-hash lock at `5177b674...`) for `eval_runner.py` ONLY — the override is BLESSED at planning round (per the milestone objective in-place revision 2026-05-29) and VERIFIED by Codex at S-Auto-7 close. The other three scoring files (`tier_evaluator.py` / `baseline_loader.py` / `gaming.py`) STAY UNCHANGED.

Class = `infra` (substrate-fix). **§7 EXEMPT** per pure-infra carve-out + self-walked for paper-trail completeness.

## Read order

1. **`AGENTS.md`** (auto-loaded; do NOT re-read explicitly).
2. **THIS prompt** — full executable contract.
3. **Code anchors on demand**:
   - `autoloop/autoloop/scoring/eval_runner.py` — the file you'll modify; current state at HEAD `1943ed5`.
   - `autoloop/autoloop/scoring/baseline_loader.py` — read-only; understand `load_baseline_snapshot` API contract BEFORE running #3 baseline blessing (informs directory shape).
   - `autoloop/autoloop/scoring/gaming.py` — read-only; understand `_compute_scoring_code_sha()` invocation signature for #2 rebaseline.
   - `autoloop/autoloop/scoring/tier_evaluator.py` — read-only; verify Layer 0-4 outcome shapes for #4 smoke verification.
   - `autoloop/autoloop/loop.py` lines 386-441 — read-only; understand `_build_baseline_summary` + `_load_baseline` to verify non-empty `BaselineSnapshot` flows correctly post-blessing.
   - `autoloop/config.yaml` — modify `fitness.scoring_code_baseline_sha` (#2) + `fitness.baseline_dir` (#3).
   - `autoloop/tests/test_eval_runner.py` (or create if absent) — extend with 3-6 new tests.
   - `eval_interactive/eval_interactive/cli.py` — read-only; verify `--output-dir` STILL doesn't exist (sanity check before #1; STOP-and-surface if it does).
   - `docs/sprints/sprint-059-handoff.md` §0 + §12 — read-only; reference for Blocker A + B evidence + OQ-S59.x context.

You do NOT read: `docs/sprint_objective.md`, `docs/milestone_objective.md`, `docs/teams/deliver-agent.md`, sprint archives beyond sprint-059-handoff.md, Codex prompt or findings, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/solutions/**`.

## Class

`infra` (§3.2 Q1 — substrate-fix + baseline rebaseline; structural plumbing repair). **§7 EXEMPT** per pure-infra carve-out (eval_runner.py change is HOW the subprocess consumes results, NOT WHAT eval reads or grades; case_specs / judge / detector / projection / scoring semantic logic ALL UNCHANGED). Self-walk for paper-trail only.

## Goal

By S-Auto-7 close:

1. **Blocker B resolved** (eval_runner fix path (b); MUST land FIRST to enable Blocker A blessing). `eval_runner.py:99-110` modified: REMOVE `--output-dir` arg from subprocess invocation; adapt `run_v1_fitness_suite` to consume eval-interactive's auto-timestamped `eval_interactive/results/<ts>/` output. `eval_interactive/eval_interactive/cli.py` UNCHANGED.
2. **`scoring_code_baseline_sha` rebaselined**. New 64-char SHA-256 computed via `_compute_scoring_code_sha()` post-edit; updated in `autoloop/config.yaml`; `_check_scoring_code_drift(config=config)` returns `[]` (silent).
3. **Blocker A resolved — `baseline_dir` blessed**. v1 47-case fitness suite (bad_cases ×12 + anchor_outcome ×12 + shadow ×23) run ONCE on `auto-loop-branch` HEAD; resulting eval-interactive `results/<run-id>` blessed as M-Auto-1B baseline; `autoloop/config.yaml` `fitness.baseline_dir` advanced from `<PLACEHOLDER-set-at-M-Auto-1A-close>` to concrete path.
4. **Smoke iter end-to-end verification**. `python -m autoloop run --experiments 1` (NO `--dry-run`) reaches Step 9 with non-degenerate `tier_evaluator_verdict` (Layer 0-4 outcomes each PASS/FAIL/informational metric; no empty-snapshot short-circuit).
5. **Per-sub-sprint Codex prep**. Author handoff; deliver-agent dispatches Codex at close (you do NOT dispatch).

**Zero touch** to all hard-fenced surfaces EXCEPT the controlled `eval_runner.py` fence #13 override (Hard fences section below).

## Scope (numbered execution steps)

### #1 — Blocker B fix path (b): adapt eval_runner.py

**Sanity check BEFORE editing**: verify `--output-dir` STILL doesn't exist in eval-interactive CLI:

```bash
cd eval_interactive && uv run eval-interactive run --help 2>&1 | grep -i "output"
```

Expected empty output. If `--output-dir` flag now exists (planning input has shifted), STOP-and-surface.

Read `autoloop/autoloop/scoring/eval_runner.py` to understand current `run_v1_fitness_suite` API + helpers + `SuiteRunResult` shape (dataclass fields: at minimum `exit_code: int` + `results_root: Path`).

Modify the subprocess invocation:

```python
cmd = [
    "uv", "run", "eval-interactive", "run",
    "--path", str(spec.path),
    "--parallel", str(spec.parallel),
    # REMOVED: "--output-dir", str(suite_out_dir.resolve()),
]
```

Adapt the post-subprocess logic to locate eval-interactive's auto-timestamped output. **Approach A (filesystem snapshot; recommended for simplicity)**:

```python
# Snapshot eval_interactive/results/ directory listing BEFORE the subprocess
results_base = Path("eval_interactive/results")  # adjust path per existing convention in eval_runner.py
results_before = set(results_base.iterdir()) if results_base.exists() else set()

# Run subprocess (cmd as above)
proc = subprocess.run(cmd, capture_output=True, text=True, cwd=...)

# Find the NEW directory created post-subprocess
results_after = set(results_base.iterdir())
new_dirs = sorted(results_after - results_before, key=lambda p: p.stat().st_mtime, reverse=True)
if not new_dirs and proc.returncode == 0:
    # Edge case: no new dir but subprocess success → eval-interactive may have written to existing path
    # Fall back to most-recent-by-mtime
    new_dirs = sorted(results_after, key=lambda p: p.stat().st_mtime, reverse=True)[:1]

results_root = new_dirs[0] if new_dirs else None
```

The `suite_out_dir` parameter the original `run_v1_fitness_suite` accepted should be re-interpreted as a STAGING target. Either:
- (a) Copy `results.json` from `results_root` to `suite_out_dir / "results.json"` for downstream `tier_evaluator.evaluate` consumption.
- (b) Symlink `suite_out_dir → results_root` (simpler; preserves the full eval-interactive output).
- (c) Adapt `SuiteRunResult.results_root` to point directly to the eval-interactive path (no staging; caller adapts).

Pick the simplest path that preserves `tier_evaluator.evaluate` API contract. Document inline + in handoff §4.

Verify by direct subprocess invocation BEFORE integration:

```bash
cd autoloop && uv run --extra dev python -c "
from pathlib import Path
from autoloop.scoring.eval_runner import run_v1_fitness_suite, SuiteSpec
spec = SuiteSpec(path=Path('../eval_interactive/case_specs/bad_cases'), parallel=1)
r = run_v1_fitness_suite(spec, Path('/tmp/test_suite_out'))
print(f'exit_code={r.exit_code}')
print(f'results_root={r.results_root}')
print(f'results_root.exists()={r.results_root.exists() if r.results_root else False}')
import json
if r.results_root and (r.results_root / 'results.json').exists():
    data = json.loads((r.results_root / 'results.json').read_text())
    print(f'case_results count: {len(data.get(\"case_results\", []))}')
"
```

(Adjust `SuiteSpec` instantiation to match the actual dataclass signature; sample the file first.)

Expected: `exit_code=0`, `results_root` points to non-empty path containing `results.json` with non-zero `case_results` count.

**Add 3-6 new tests** to `autoloop/tests/test_eval_runner.py` (extend if exists; create if not):

```python
def test_eval_runner_removes_output_dir_arg():
    """Blocker B fix path (b): subprocess command must NOT contain --output-dir."""
    # Use mocked subprocess; capture the cmd list passed; assert no --output-dir.
    ...

def test_eval_runner_locates_auto_timestamped_output_via_mtime():
    """Post-subprocess, eval_runner finds the newest eval_interactive/results/<ts>/ directory."""
    # Fixture: pre-create eval_interactive/results/<ts-old>/; mock subprocess to create eval_interactive/results/<ts-new>/;
    # assert results_root points to <ts-new>.
    ...

def test_eval_runner_returns_suiterunresult_with_results_root():
    """SuiteRunResult.results_root is a valid Path containing results.json."""
    ...

def test_eval_runner_exit_code_propagation():
    """If subprocess exits non-zero, SuiteRunResult.exit_code != 0 and results_root is None or invalid."""
    ...
```

Run the autoloop pytest suite:

```bash
cd autoloop && uv run --extra dev pytest -q
```

Expect 223 → 226-229 PASS (3-6 new). If any existing test fails after #1, STOP and surface (likely a test that asserts `--output-dir` in cmd; update those assertions in this commit).

### #2 — scoring_code_baseline_sha rebaseline

Read `autoloop/autoloop/scoring/gaming.py` to find `_compute_scoring_code_sha` helper. Verify its signature (likely accepts a config dict or config_path argument).

Compute the new hash post-#1 edit:

```bash
cd autoloop && uv run --extra dev python -c "
from autoloop.scoring.gaming import _compute_scoring_code_sha
# Use whatever invocation matches the actual signature:
sha = _compute_scoring_code_sha(config_path='config.yaml')
# OR: import yaml; config = yaml.safe_load(open('config.yaml')); sha = _compute_scoring_code_sha(config=config)
print(sha)
"
```

Output: 64-char hex SHA-256. Update `autoloop/config.yaml` `fitness.scoring_code_baseline_sha`:

```yaml
fitness:
  baseline_dir: <still placeholder for now; advanced in #3>
  scoring_code_baseline_sha: <new-64-char-hex>  # Was: 5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c (M-Auto-1A close hash); S-Auto-7 rebaselined 2026-05-29 after Blocker B fix path (b) modified eval_runner.py per `docs/sprints/sprint-060-handoff.md` §5.
```

(Include the inline comment with precedent + cross-reference.)

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

If assertion fails, the hash doesn't match — recompute + re-update.

**COMMIT 1 ENDS HERE** — stage `autoloop/autoloop/scoring/eval_runner.py` + `autoloop/config.yaml` (`scoring_code_baseline_sha` only — NOT `baseline_dir` yet) + `autoloop/tests/test_eval_runner.py`. Commit message per Commit discipline below.

### #3 — Blocker A blessing: run 47-case fitness baseline + advance baseline_dir

Read `autoloop/autoloop/scoring/baseline_loader.py` to understand `load_baseline_snapshot` API contract — specifically: does it expect ONE directory with subdirs per suite OR THREE separate directories. This informs the directory shape decision below.

Snapshot eval-interactive `results/` BEFORE running:

```bash
cd eval_interactive && ls -t results/ > /tmp/results_before_baseline.txt
```

Run the three suites:

```bash
# Suite 1: bad_cases (parallel=1 per R-bad-case-parallel-session-establishment-flakiness)
cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1

# Suite 2: anchor_outcome (parallel=4)
cd eval_interactive && uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4

# Suite 3: shadow (parallel=4)
cd eval_interactive && uv run eval-interactive run --path case_specs_shadow/ --parallel 4

# Identify the 3 NEW run-ids
cd eval_interactive && ls -t results/ | head -5
```

Three NEW timestamped run-ids should appear. For each, read its `results.json` `summary` block + count per-case verdicts.

**Bless the baseline**: depending on baseline_loader's API contract:

- **If loader expects ONE aggregated directory**: create `eval_interactive/results/m-auto-1b-baseline-<YYYYMMDD>/` and copy/symlink the three eval-interactive outputs as `{bad_cases,anchor_outcome,shadow}/` subdirs (or whatever sub-structure the loader expects).
- **If loader expects THREE separate paths**: configure `autoloop/config.yaml` `fitness.baseline_dir` as a structured value (dict / list) per loader's expectation.
- **If loader expects ONE results.json with aggregated suites**: merge the three results.json into one OR (more likely) the loader iterates `<baseline_dir>/<suite>/results.json` per suite.

(Sample baseline_loader.py BEFORE running suites to know the answer; do NOT design without reading.)

Update `autoloop/config.yaml`:

```yaml
fitness:
  baseline_dir: eval_interactive/results/<concrete-blessed-path>  # Was: <PLACEHOLDER-set-at-M-Auto-1A-close>; S-Auto-7 blessed 2026-05-29 per `docs/sprints/sprint-060-handoff.md` §6.
  scoring_code_baseline_sha: <hash from #2>
```

Record in handoff §6 "Baseline blessing evidence":
- Three eval-interactive run-ids (timestamps).
- Per-suite case_passed count + per-case verdict snapshot (table).
- Total wall-clock for the 47-case baseline.
- Final blessed `baseline_dir` value.
- Subdirs / directory shape if applicable.

**If a baseline run errors out entirely** (e.g., eval-interactive segfaults on a specific case): STOP and surface — baseline quality matters.

### #4 — Smoke iter end-to-end verification

Run:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 1
```

(NO `--dry-run`.) Full 14-step state machine must reach Step 9.

Record:
- Iteration outcome (keep / discard / error — any acceptable).
- Per-iter wall-clock elapsed (FIRST measurement of full Spring + 47-case-eval cycle).
- `autoloop/results/runs/exp-<N>/iteration_record.json` — verify `tier_evaluator_verdict` is non-empty + Layer 0-4 outcomes are non-degenerate.

**Non-degenerate verification**: for each Layer 0/1/2/3/4 in `tier_evaluator_verdict`:
- Field exists with proper structure (not None / empty dict / empty list).
- Layer 1-4 do NOT silently short-circuit via `is not None` guards against empty BaselineSnapshot (the S-Auto-5 pattern).
- Layer 3 improvement_threshold has a concrete `delta` value (not the `... or 0 → trivial PASS` collapse from `tier_evaluator.py:485-532`).

Per-iter elapsed >40 min: complete the iteration if possible; STOP further iterations + surface as observation toward M-Auto-2 R-item.

Smoke iter crashes in unhandled path NOT caused by Blockers A/B: STOP-and-surface. Fix-iteration S-Auto-7.1 candidate.

### #5 — Codex prep + handoff

Author `docs/sprints/sprint-060-handoff.md` per Handoff requirements section. Do NOT dispatch Codex; deliver-agent dispatches at close.

Pre-mitigation against Codex friction:

```bash
# Verify only the expected files changed:
git diff --stat 1943ed5..HEAD
# Expected: 4 files — eval_runner.py, config.yaml, test_eval_runner.py, sprint-060-handoff.md

# Verify other fence #13 scoring files UNCHANGED:
git diff --stat 1943ed5..HEAD -- autoloop/autoloop/scoring/tier_evaluator.py autoloop/autoloop/scoring/baseline_loader.py autoloop/autoloop/scoring/gaming.py
# Expected: empty

# Verify eval-interactive CLI UNCHANGED (fix path (b) preserved):
git diff --stat 1943ed5..HEAD -- eval_interactive/eval_interactive/cli.py
# Expected: empty
```

Include all three verification outputs in handoff §8.

**COMMIT 2 ENDS HERE** — stage `autoloop/config.yaml` (`baseline_dir` only) + `docs/sprints/sprint-060-handoff.md`. Commit message per Commit discipline below.

## Hard fences / STOP conditions

- **CONTROLLED FENCE #13 OVERRIDE**: `autoloop/autoloop/scoring/eval_runner.py` is the ONLY file in the fence #13 content-hash-locked group that may be touched. The other three (`tier_evaluator.py` / `baseline_loader.py` / `gaming.py`) STAY UNCHANGED. The override is finalized by re-baselining `scoring_code_baseline_sha` in config.yaml.
- **Zero touch** to `eval_interactive/eval_interactive/cli.py` (fix path (b) preserves eval-interactive byte-identical; fix path (a) NOT taken).
- **Zero touch** to `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`.
- **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py`.
- **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` / `cli.py`.
- **Zero touch** to `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/` (cherry-pick to skills/ is S-Auto-8 scope only).
- **Zero touch** to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives (other than S-Auto-7's own), milestone archives, `docs/codex-findings.md` scaffold.
- **No new heavy deps** in `autoloop/pyproject.toml`. Stdlib `pathlib` + `subprocess` only.
- **No LLM call** in `eval_runner.py` or its tests.
- **No `git add -A`** — stage explicitly per Commit discipline.
- **No cherry-pick to main** in S-Auto-7.
- **STOP and surface** conditions:
  - `--output-dir` flag has been added to eval-interactive CLI in the interim.
  - Direct subprocess invocation of repaired eval_runner crashes for a reason beyond Blocker B.
  - Baseline run surfaces unexpected per-case verdict shape OR per-suite errors.
  - Smoke iter crashes in unhandled path NOT caused by Blockers A/B.
  - Smoke iter elapsed >40 min.
  - `_check_scoring_code_drift(config=config)` returns non-empty after rebaseline.
  - `tier_evaluator_verdict` still degenerate post-smoke despite blessed baseline.

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run --extra dev pytest -q` — baseline `223 passed, 1 warning` MUST grow by ~3-6 (test_eval_runner.py). Total `226-229 passed, 1 warning`.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed`.
- **Java baseline UNCHANGED**: skipped per Java-zero-touch (`git diff --stat 1943ed5..HEAD -- server/src/main/java/ eval/src/main/java/` empty).
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py` reproduces 31 passed.
- **Detector self-discipline 3 regression tests**: UNCHANGED — PASS.
- **scoring_code_baseline_sha NEW value matches actual `_compute_scoring_code_sha()` output** post-edit; `_check_scoring_code_drift(config=config) == []`.
- **Live-iter smoke**: 1 iter completed through Step 9 with non-degenerate `tier_evaluator_verdict`.

## §7 — Layer-classification + anti-hardcode stanza (EXEMPT; self-walked)

**§7 NOT REQUIRED per pure-infra carve-out**. Self-walked for paper-trail:

- **Target failure layer:** `infra` (§3.2 Q1 — substrate-fix; structural plumbing repair).
- **Tier-0 invariant:** adds none.
- **Semantic hardcode:** none introduced. eval_runner.py fix is subprocess invocation pattern adjustment + filesystem auto-timestamp consumption. scoring_code SHA + baseline_dir advances are config field updates.
- **Generalization coverage:** target = eval_runner end-to-end against real eval-interactive subprocess; scoring_code_drift silent; smoke iter reaches Step 9 non-degenerate. Neighbor = autoloop pytest UNCHANGED + 3-6 new. Negative-control = `_check_scoring_code_drift` returns `[]` post-rebaseline; 17-fixture sweep UNCHANGED. Shadow = firewall posture UNCHANGED.

## Codex review plan (§4.3)

**PER-SUB-SPRINT REQUIRED — §4.3 trigger #3** (hard-fenced surface explicitly named out of scope; fence #13 controlled override). Deliver-agent dispatches `compact/sprint-060-codex-review-prompt.md` at S-Auto-7 close; you do NOT dispatch.

7 verification axes Codex will walk (pre-mitigate against in your handoff §8):

1. Blocker B fix structural soundness — plumbing only, no semantic logic.
2. New `scoring_code_baseline_sha` matches actual `_compute_scoring_code_sha()` (reproducibility).
3. Controlled fence #13 override justified — Blocker B forces it; minimal blast radius.
4. Blessed `baseline_dir` points to real run with non-zero `case_passed`.
5. Smoke `tier_evaluator_verdict` non-degenerate (Layer 0-4).
6. Hard-fence verification for non-overridden surfaces (empty diff).
7. `eval_interactive/eval_interactive/cli.py` UNCHANGED.

Expected verdict: `pass / 0` or `approve with downgrade-to-signal follow-up`.

## Handoff requirements

Author `docs/sprints/sprint-060-handoff.md` at S-Auto-7 close. §12 empty. Required:

- **§1 Class + §7 stanza self-walk** — confirm `infra` + §7 EXEMPT.
- **§2 Goal achievement** — bullet 5 Goals + evidence pointer.
- **§3 Scope execution log** — for each scope step #1-#5, status + notes.
- **§4 Blocker B fix evidence** — diff snippet; subprocess invocation result; SuiteRunResult shape; approach chosen (A/B/C).
- **§5 scoring_code_baseline_sha rebaseline evidence** — old hash + new hash + reproducibility check + `_check_scoring_code_drift` silent confirmation.
- **§6 Baseline blessing evidence** — three run-ids; per-suite case_passed counts (table); total wall-clock; blessed baseline_dir.
- **§7 Smoke iter end-to-end record** — outcome; elapsed; tier_evaluator_verdict Layer 0-4 non-degenerate verification; artefact tree.
- **§8 Adversarial spot-check pre-Codex** — three `git diff --stat` verifications (only expected files; other scoring files empty; eval-interactive CLI empty).
- **§9 Code anchor table** — `git show --numstat` for the two S-Auto-7 commits.
- **§10 Test count** — autoloop pytest 223 → final + delta breakdown.
- **§11 §7 stanza self-walk verification** — "self-walk passed; §7 EXEMPT confirmed".
- **§12 OQ-S60.x list** — open questions + disposition.

You do NOT author Codex prompt or findings.

## Commit discipline

**Two-commit pattern**.

**Commit 1**: Blocker B + scoring SHA rebaseline + tests.

```
Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker B fix path (b) + scoring SHA rebaseline + tests

[2-3 sentences: eval_runner.py change shape; tests added; scoring SHA before -> after]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage: `autoloop/autoloop/scoring/eval_runner.py`, `autoloop/config.yaml` (only `scoring_code_baseline_sha` change), `autoloop/tests/test_eval_runner.py`.

**Commit 2**: Blocker A blessing + smoke iter + handoff.

```
Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker A baseline_dir blessing + smoke iter

[2-3 sentences: three blessed run-ids; per-suite case_passed; smoke outcome + elapsed]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage: `autoloop/config.yaml` (only `baseline_dir` change), `docs/sprints/sprint-060-handoff.md`.

**No `git add -A`**. Deliver-agent close-bundle artefacts bundled by human at close.

## Self-check checklist (before declaring sub-sprint done)

- [ ] Pre-#1 sanity: `eval-interactive run --help | grep -i output` confirms `--output-dir` STILL doesn't exist.
- [ ] `eval_runner.py` Blocker B fix landed: `--output-dir` arg removed from cmd; auto-timestamp consumption logic added; approach (A/B/C) documented inline.
- [ ] Direct subprocess invocation of `run_v1_fitness_suite` returns `exit_code=0` + `results_root.exists() == True` + non-zero `case_results` count.
- [ ] 3-6 new `test_eval_runner.py` tests added; all PASS.
- [ ] `autoloop` pytest suite 223 → 226-229 PASS (1 warning unchanged).
- [ ] `_compute_scoring_code_sha()` invoked; new 64-char hex hash captured.
- [ ] `autoloop/config.yaml` `fitness.scoring_code_baseline_sha` updated to new hash with inline comment + handoff cross-reference.
- [ ] `_check_scoring_code_drift(config=config)` returns `[]` post-update.
- [ ] Commit 1 staged + committed (eval_runner.py + config.yaml SHA change + test_eval_runner.py); footer `Co-Authored-By` present.
- [ ] `baseline_loader.py` API contract understood (one aggregated dir vs three separate vs other shape).
- [ ] 3 eval-interactive suite runs completed (bad_cases parallel=1; anchor_outcome parallel=4; shadow parallel=4); 3 new run-ids identified.
- [ ] Per-suite `case_passed` counts captured (table for handoff §6).
- [ ] Baseline blessed: directory structure built per baseline_loader contract; `autoloop/config.yaml` `fitness.baseline_dir` updated to concrete path.
- [ ] Smoke `python -m autoloop run --experiments 1` (NO --dry-run) ran; outcome captured.
- [ ] `iteration_record.json` `tier_evaluator_verdict` verified non-degenerate (Layer 0-4 each PASS/FAIL/informational metric, no empty-snapshot short-circuit).
- [ ] Per-iter elapsed time recorded; ≤40 min (or surfaced).
- [ ] `git diff --stat 1943ed5..HEAD -- autoloop/autoloop/scoring/tier_evaluator.py autoloop/autoloop/scoring/baseline_loader.py autoloop/autoloop/scoring/gaming.py` returns empty.
- [ ] `git diff --stat 1943ed5..HEAD -- eval_interactive/eval_interactive/cli.py` returns empty.
- [ ] `git diff --stat 1943ed5..HEAD -- server/src/main/java/ eval/src/main/java/ eval_interactive/eval_interactive/ eval_interactive/case_specs/ eval_interactive/case_specs_shadow/ server/src/main/resources/ data/ db/migration/` returns empty.
- [ ] `cd eval_interactive && uv run python -m pytest --tb=no -q` reports `486 passed, 3 failed` UNCHANGED.
- [ ] `docs/sprints/sprint-060-handoff.md` §1-§11 filled; §12 OQ list filled with surfaced OQs; §12 closure note empty.
- [ ] Commit 2 staged + committed (config.yaml baseline_dir change + handoff); footer present.
- [ ] No `git add -A` was used.
- [ ] Surface to deliver-agent + human that S-Auto-7 is ready for Codex per-sub-sprint dispatch.

## OQ (open questions — fill handoff §12)

Expected OQ-S60.x candidates:

- **OQ-S60.1** — eval-interactive auto-timestamped output format (per-suite subdir or flat `results.json`).
- **OQ-S60.2** — `baseline_loader.load_baseline_snapshot` API contract (aggregated vs separate directory).
- **OQ-S60.3** — eval-interactive subprocess elapsed times per suite.
- **OQ-S60.4** — Smoke iter elapsed vs S-Auto-6 contract estimate (12-25 min target; 40 min cap).
- **OQ-S60.5** — `_compute_scoring_code_sha()` signature (path-configurable or hard-coded).
- **OQ-S60.6** — `autoloop/results/runs/exp-<smoke>/eval/` staging pattern (decision: A/B/C from #1).
- **OQ-S60.7** — Spring spawn success rate on smoke iter (compare to S-Auto-5 OQ-S58.7 1/3).
- **OQ-S60.8** — `--results-base` or equivalent flag in eval-interactive CLI (verify `eval-interactive run --help`).

Add OQ entries as ambiguities are encountered; record disposition per OQ.

---

**END OF DEV PROMPT.** Begin with #1 (Blocker B fix path (b)). STOP-and-surface at any STOP condition.
