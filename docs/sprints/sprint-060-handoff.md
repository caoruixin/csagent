---
title: Sprint 060 / S-Auto-7 / M-Auto-1B substrate-fix handoff
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-29
review_cadence: ad hoc
notes: >
  Dev-agent handoff for S-Auto-7 substrate-fix sub-sprint under
  M-Auto-1B. Class = infra. §7 EXEMPT per pure-infra carve-out
  (self-walked in §1 for paper-trail). Blockers A + B + an
  in-session-surfaced Blocker C are LANDED; Goal #4 smoke iter end-to-
  end is BLOCKED by an upstream OQ-S58.7 carryover (alt-port Spring
  spawn intermittent vs the foreground :8080 backend), STOP-and-
  surfaced per dev prompt. The substrate fixes are independently
  verified via the test suite (228 PASS) and a direct 46-case
  baseline run; the missing piece is the loop-context exercise of
  `run_v1_fitness_suite`, which requires Spring spawn to succeed.
---

# Sprint 060 / S-Auto-7 / M-Auto-1B — substrate-fix handoff

Dev session: S-Auto-7 (substrate fix Blocker A + Blocker B; smoke iter
attempted). Class = `infra`. §7 EXEMPT per pure-infra carve-out;
self-walked in §1 + §11.

## §1 Class + §7 stanza self-walk

- **Class:** `infra` (§3.2 Q1 — substrate-fix; structural plumbing
  repair + baseline rebaseline + baseline directory blessing).
- **§7 stanza:** EXEMPT per pure-infra carve-out (eval_runner.py
  change is HOW the autoloop subprocess consumes eval-interactive's
  output; case_spec/loader.py change is HOW the case-loader walks a
  directory tree; neither touches case_specs / judge / detector /
  projection / scoring semantic logic).
- **Self-walk (paper-trail):**
  - **Target failure layer:** `infra`.
  - **Tier-0 invariant:** adds none.
  - **Semantic hardcode:** none introduced. eval_runner.py adapts to
    eval-interactive's auto-timestamp output convention via
    before/after set-diff + mtime tiebreaker + symlink staging.
    case_spec/loader.py replaces a non-recursive `glob` with a
    recursive `rglob` and skips `_*.yaml` (manifest convention).
    scoring_code_baseline_sha + fitness.baseline_dir advances are
    config field updates.
  - **Generalization coverage:** target = eval_runner end-to-end
    against eval-interactive's actual CLI shape; scoring_code_drift
    silent; baseline_dir reads 46 cases across 3 suites with 0
    warnings from `baseline_loader.load`. Neighbor = autoloop pytest
    228 PASS + eval-interactive pytest 486 PASS / 3 FAIL UNCHANGED.
    Negative-control = `_check_scoring_code_drift(config=config)`
    returns `[]` post-rebaseline; flat case_set dirs (anchor,
    promotion, exploration, smoke, bad_cases, anchor_outcome) load
    the same case count under rglob as under glob. Shadow firewall
    posture UNCHANGED (no autoloop code reads shadow content; only
    counts via `load_case_specs`).

## §2 Goal achievement

| Goal | Status | Evidence pointer |
|------|--------|------------------|
| 1. Blocker B resolved (fix path (b)) | ✅ DONE | §4 |
| 2. scoring_code_baseline_sha rebaselined | ✅ DONE | §5 |
| 3. Blocker A resolved — baseline_dir blessed | ✅ DONE | §6 |
| 4. Smoke iter reaches Step 9 non-degenerate | ⚠️ BLOCKED | §7 |
| 5. Per-sub-sprint Codex prep | ✅ DONE (this handoff) | §8 |

Goal #4 is blocked at the alt-port Spring-spawn stage (Step 6, applier
health probe) which is upstream of any eval_runner.py invocation
(Step 8). This is **NOT a Blocker A or Blocker B failure** — the
substrate fixes are independently verified via the test suite + the
real-CLI baseline run. The Spring spawn failure is the OQ-S58.7
carryover (1/3 success rate observed at S-Auto-5; correlated with the
foreground :8080 backend competing for maven build state +
PostgreSQL/Flyway lock + classpath bottleneck during concurrent
`spring-boot:run`). Per dev prompt: STOP-and-surface as fix-iteration
S-Auto-7.1 candidate.

A NEW in-session Blocker C also surfaced and was resolved with human
in-session authorization: shadow path layout (`case_specs_shadow/`
nested under `case_families/<family>/` plus `_manifest.yaml` at the
top level) was incompatible with eval-interactive's non-recursive
`load_case_specs`. Fix landed at
`eval_interactive/eval_interactive/case_spec/loader.py` (rglob +
underscore-skip filter). See §6 + §12 OQ-S60.x for details.

## §3 Scope execution log

| Step | Status | Notes |
|------|--------|-------|
| #1 — Blocker B fix path (b) | DONE | Approach B (symlink). 88 line additions to eval_runner.py + 5 new tests + 2 existing-test updates. |
| #2 — scoring_code_baseline_sha rebaseline | DONE | 5177b674… → 22548e20…; silent steady state confirmed. |
| #3 — Baseline blessing | DONE | 3 eval-interactive runs (46 cases total); blessed at `eval_interactive/results/m-auto-1b-baseline-20260529/` via per-suite symlinks. Blocker C surfaced + fixed in-session. |
| #4 — Smoke iter | BLOCKED | exp-6 reached Step 6 applier, Spring alt-port spawn rc=1 at 90.4s. NOT a Blocker A/B failure. |
| #5 — Codex prep + handoff | DONE | This file. Deliver-agent dispatches at close. |
| COMMIT 1 | DONE | 559927a; eval_runner + config (SHA) + test_eval_runner. |
| COMMIT 2 | DONE | Author per §13 below; config (baseline_dir) + loader.py + this handoff. |

## §4 Blocker B fix evidence

**Failure shape pre-fix** (S-Auto-6 close-empty surfaced; sprint-059):
`eval_runner.py:99-110` invoked `eval-interactive run` with an
`--output-dir` flag that does not exist in the eval-interactive CLI.
Any real subprocess call would crash on `Error: no such option:
--output-dir`.

**Fix path chosen**: (b) — REMOVE `--output-dir` from eval_runner and
adapt to eval-interactive's native output convention. Path (a)
(adding the flag to eval-interactive) was rejected at S-Auto-6 close
because eval-interactive is byte-identical-fenced (fence #14).

**Approach choice**: **B (symlink)**.
- (A) copy `results.json` only → loses per-turn traces + report.html.
- (B) symlink `<results_root>/<suite>` → `<eval_interactive_results>/<ts>/`
  → preserves all artefacts; downstream consumers
  (`baseline_loader.load`, `tier_evaluator.evaluate`, `gaming.detect`)
  see the historical `<results_root>/<suite>/results.json` contract
  transparently because their existing `Path / "results.json"` reads
  dereference the symlink without code change.
- (C) point `SuiteRunResult.results_dir` directly at the timestamp
  dir → required every caller to adapt; larger blast radius.

**Code shape** at `autoloop/autoloop/scoring/eval_runner.py:80-160`:

```python
# Snapshot eval-interactive's results/ directory before subprocess.
ei_results_dir = cwd / "results"
before_ts_dirs: set[Path] = (
    {p for p in ei_results_dir.iterdir() if p.is_dir()}
    if ei_results_dir.exists()
    else set()
)
proc = subprocess.run(cmd, cwd=str(cwd), env=sub_env, capture_output=True,
                      text=True, timeout=timeout_seconds)
new_ts_dir = _locate_new_results_dir(ei_results_dir, before_ts_dirs)
if new_ts_dir is not None:
    if suite_link.is_symlink() or suite_link.is_file():
        suite_link.unlink()
    elif suite_link.exists():
        shutil.rmtree(suite_link)
    suite_link.symlink_to(new_ts_dir.resolve(), target_is_directory=True)
```

Plus a `_locate_new_results_dir(ei_results_dir, before_ts_dirs)`
helper using set-difference + mtime tiebreaker (defensive against
concurrent runs even though v1 fitness is sequential).

**Tests** (`autoloop/tests/test_eval_runner.py`):

| Test | Purpose | Verdict |
|------|---------|---------|
| `test_eval_runner_removes_output_dir_arg` (NEW) | Asserts `--output-dir not in cmd`; cmd ends with `--parallel <N>` | PASS |
| `test_eval_runner_locates_auto_timestamped_output_via_set_diff` (NEW) | Synthetic eval_interactive/results/ with pre-existing dir + mock subprocess creates new dir → result symlinks to new dir | PASS |
| `test_eval_runner_returns_suiterunresult_with_correct_paths` (NEW) | `results_dir`/`results_json` preserve historical contract | PASS |
| `test_eval_runner_no_new_dir_leaves_results_path_absent` (NEW) | Failed subprocess (rc=1) → no symlink, `results_json.exists() == False` | PASS |
| `test_locate_new_results_dir_picks_newest_by_mtime_on_multi` (NEW) | Multiple new dirs → newest by mtime wins | PASS |
| `test_run_suite_mocked_success` (UPDATED) | Asserts `--output-dir not in cmd` (was `in cmd`) | PASS |
| `test_run_v1_fitness_suite_runs_all_three_sequentially` (UPDATED) | Suite identification via `--path` basename (was via `--output-dir` basename) | PASS |

**Test suite baseline preservation**:
- autoloop pytest: 223 → 228 PASS, 1 warning UNCHANGED.
- eval_interactive pytest: 486 PASS / 3 FAIL UNCHANGED (3 pre-existing
  failures in `test_case_spec_overrides.py` + `test_corpus_lint.py`).

**Direct subprocess integration**: implicitly verified at #3 baseline
run — three direct `eval-interactive run` invocations all wrote to
`eval_interactive/results/<ts>/results.json` and the loader fix +
eval_runner.py symlink staging both consume that shape. The loop-
context exercise of `run_v1_fitness_suite` itself is BLOCKED at
upstream Spring-spawn (Goal #4 / §7 below).

## §5 scoring_code_baseline_sha rebaseline evidence

**Old hash** (M-Auto-1A close, 2026-05-27, computed at commit `1feef1f`):
`5177b674b5ad249d7c0e706f3c827010c8dab511240f239dbd3be6f689a0331c`

**New hash** (S-Auto-7 close, 2026-05-29, computed at COMMIT 1 `559927a`
HEAD):
`22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`

**Reproducibility**:

```bash
cd autoloop && uv run --extra dev python -c \
  "from autoloop.scoring.gaming import _compute_scoring_code_sha; \
   print(_compute_scoring_code_sha())"
# Expected: 22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9
```

**Silent steady state**:

```bash
cd autoloop && uv run --extra dev python -c "
import yaml; from pathlib import Path
from autoloop.scoring.gaming import _check_scoring_code_drift
config = yaml.safe_load(Path('config.yaml').read_text())
flags = _check_scoring_code_drift(config=config)
assert flags == [], f'Expected empty drift list; got {flags}'
print('PASS: scoring_code_drift silent')
"
# Output: "PASS: scoring_code_drift silent"
```

**Fence #13 verification**: only `eval_runner.py` was modified; the
other three scoring files (`tier_evaluator.py`, `baseline_loader.py`,
`gaming.py`) are byte-identical to their pre-S-Auto-7 form. Confirmed
via `git diff --stat 1943ed5..HEAD -- autoloop/autoloop/scoring/{tier_evaluator,baseline_loader,gaming}.py`
returning empty (see §8).

## §6 Baseline blessing evidence (Blocker A + Blocker C)

**Three eval-interactive runs** executed sequentially against the
foreground `:8080` Spring backend (PID 8613, started Thu by human):

| Suite | Path | Parallel | Run-ID | Wall-clock | total | case_passed (loader-counted) | tier2 mandatory FAILs |
|-------|------|----------|--------|-----------:|------:|-----------------------------:|----------------------:|
| `bad_cases` | `eval_interactive/case_specs/bad_cases/` | 1 | `20260529-101324` | 7m 23s | 12 | 5 (rate 0.417) | 4 |
| `anchor_outcome` | `eval_interactive/case_specs/anchor_outcome/` | 4 | `20260529-102054` | 1m 50s | 12 | 7 (rate 0.583) | 4 |
| `shadow` | `eval_interactive/case_specs_shadow/` | 4 | `20260529-102949` | 3m 33s | 22 | 4 (rate 0.182) | 10 |
| **Total** | — | — | — | **~12m 46s** | **46** | **16 (rate 0.348)** | **18** |

**Note on case count**: the dev prompt expected `bad_cases ×12 +
anchor_outcome ×12 + shadow ×23 = 47`. The actual shadow count is **22**
because the 23rd entry was `case_specs_shadow/_manifest.yaml`, a
manifest metadata file (not a case_spec). The S-Auto-7 in-session
loader fix correctly filters underscore-prefixed yamls.

**Note on case_passed discrepancy with CLI summary**: the eval-
interactive CLI summary reports `summary.passed_cases = 0` for all
three suites; the autoloop `baseline_loader._aggregate_cases`
counts per-case `case_results[].case_passed == True` separately and
reports 5/7/4. These metrics measure different things:
- `summary.passed_cases` (CLI) = strict programmatic pass.
- `case_results[].case_passed` (loader) = per-case PASS verdict
  including programmatic + human-judgment-criterion (relevant for
  bad_cases / anchor_outcome per §5.6 human-judgment-primary gate).
The autoloop loop uses the LATTER for baseline comparison; that is
the metric the lexicographic verdict considers.

**Blocker C (in-session)**: shadow suite layout mismatch.

- Symptom: `eval-interactive run --path case_specs_shadow/` crashed
  with `KeyError: 'form_context'` at YAML parse.
- Root cause: `eval_interactive/case_spec/loader.py:load_case_specs`
  used a non-recursive `directory.glob("*.yaml")`. Shadow YAMLs are
  nested under `case_families/<family>/` so glob found nothing
  underneath; but `_manifest.yaml` at the top level matched the glob
  and crashed parse as it is metadata, not a case_spec.
- In-session human authorization: the planning-time "zero touch
  eval_interactive/eval_interactive/" fence was OVERRIDDEN by human
  in-session ("尽快帮我修复掉吧" / "fix it now, get auto-loop
  running ASAP") — Blocker C deemed a substrate path-handling
  issue, not a scope expansion. Fix landed at the same level as
  the eval_runner.py controlled override per fence #13.
- Fix shape: `directory.glob` → `directory.rglob`; added an
  `_is_case_spec(p)` filter that skips `p.name.startswith("_")`.
  Zero-impact on existing flat case_set dirs (anchor 159 / promotion
  101 / exploration 107 / smoke 14 / bad_cases 12 / anchor_outcome
  12 — all report identical pre- and post-rglob counts because
  none contain subdirs or underscore-yamls). Shadow now loads 22
  cases.
- eval_interactive pytest UNCHANGED at 486 PASS / 3 FAIL — the
  loader change preserves all existing test behaviour.

**Blessed baseline directory**: `eval_interactive/results/m-auto-1b-baseline-20260529/`
contains three per-suite symlinks:

```
m-auto-1b-baseline-20260529/
├── anchor_outcome → /…/eval_interactive/results/20260529-102054
├── bad_cases      → /…/eval_interactive/results/20260529-101324
└── shadow         → /…/eval_interactive/results/20260529-102949
```

**baseline_loader verification**:

```bash
cd autoloop && uv run --extra dev python -c "
import yaml
from pathlib import Path
from autoloop.scoring import baseline_loader
config = yaml.safe_load(Path('config.yaml').read_text())
baseline_path = Path('../eval_interactive/results/m-auto-1b-baseline-20260529').resolve()
snap = baseline_loader.load(baseline_path, config=config)
print('warnings:', snap.warnings)
for n, s in snap.snapshots.items():
    print(f'  {n}: total={s.total_cases} passed={s.case_passed_count} tier2_fails={s.tier2_mandatory_failure_count}')
"
# Output:
#   warnings: []
#   bad_cases: total=12 passed=5 tier2_fails=4
#   anchor_outcome: total=12 passed=7 tier2_fails=4
#   shadow: total=22 passed=4 tier2_fails=10
```

Zero warnings, all three suites loaded, layout matches baseline_loader's
documented `<baseline_dir>/<suite>/results.json` shape exactly.

**autoloop/config.yaml advance**: line 110 advanced from
`<PLACEHOLDER-set-at-M-Auto-1A-close>` to
`eval_interactive/results/m-auto-1b-baseline-20260529` with a 6-line
inline comment naming the three run-IDs + per-case counts + the
shadow=22 (not 23) note.

## §7 Smoke iter end-to-end record (Goal #4 — BLOCKED)

**Two attempts** (exp-5 and exp-6, both after COMMIT 1):

| Attempt | exp-id | Step reached | Elapsed | Failure mode |
|---------|--------|--------------|---------|--------------|
| 1 | exp-5 | Step 1 (analyzer) | 0.0s | `LLMClientError: AUTOLOOP_META_LLM_API_KEY not set` |
| 2 (after `source .env.local`) | exp-6 | Step 6 (applier health probe) | 90.4s | `SpringStartupTimeoutError: Spring subprocess exited prematurely (rc=1) on port 59771` |

**Attempt 1 (exp-5) diagnosis**: env-loading expectation. `.env.local`
contains the credentials but `python -m autoloop run` does not call
`dotenv.load_dotenv()` itself; the caller must `set -a; source
.env.local; set +a` first. Documented + corrected for attempt 2.

**Attempt 2 (exp-6) diagnosis**: alt-port Spring spawn failure.

- Analyzer + proposer ran successfully → hypothesis on
  `server/src/main/resources/skills/resolve_intake_collect_and_handover.yaml`
  `$.procedure`.
- Sandbox ACCEPT (whitelisted edit to `$.procedure`).
- content_validator PASS.
- anti_hardcode PASS.
- Applier patched yaml + committed to `autoloop/exp-6` branch + found
  free port `59771` + spawned `mvn -q -pl server -am spring-boot:run
  -Dspring-boot.run.arguments=--server.port=59771`.
- Spring subprocess crashed (rc=1) within `health_probe_timeout`.
- Loop reported `error` decision; switched HEAD back to
  `auto-loop-branch`; left `autoloop/exp-6` branch in place for
  inspection per applier convention.

**Root cause (most likely)**: the foreground `:8080` Spring backend
(PID 8613, needed for the §6 baseline run) competes with the alt-port
spawn for shared resources during concurrent `spring-boot:run`:

1. **Flyway migration lock** on the shared PostgreSQL database
   (`flyway_schema_history` row-level lock taken at startup; the
   alt-port instance times out waiting for the foreground instance
   to release it on first launch).
2. **Maven multi-module target/ classpath contention** — both
   instances rebuild from the same `server/target/`; concurrent
   write/read may race.
3. **Redis connection pool exhaustion** if both bind the same Redis.

This is the OQ-S58.7 1/3-success carryover from S-Auto-5. NOT caused
by Blocker A or Blocker B; the eval_runner.py code path is never
reached (Spring spawn is Step 6; eval is Step 8).

**Per dev prompt STOP-and-surface**: "Smoke iter crashes in unhandled
path NOT caused by Blockers A/B: STOP-and-surface. Fix-iteration
S-Auto-7.1 candidate." Surfaced as **OQ-S60.7** (see §12).

**Independent substrate verification** (since smoke iter was
blocked):

| Verification | How | Result |
|--------------|-----|--------|
| eval_runner.py structural correctness | autoloop pytest 228 PASS (5 new + 5 updated) | ✅ |
| eval_runner.py symlink behaviour against real eval-interactive output | Synthetic test with monkey-patched _REPO_ROOT mimics real before/after timestamp dir behaviour | ✅ |
| eval-interactive subprocess writes auto-timestamp dir | Three direct CLI invocations at §6 baseline run each created a unique `eval_interactive/results/<ts>/` | ✅ |
| baseline_loader consumes blessed dir cleanly | baseline_loader.load returns 3 SuiteSnapshot, 0 warnings | ✅ |
| scoring_code_drift silent post-rebaseline | _check_scoring_code_drift(config) == [] | ✅ |

The strong combination of (a) unit-test coverage of the new symlink
logic, (b) real-CLI confirmation of eval-interactive's output
convention, and (c) clean baseline_loader consumption demonstrates
that the substrate fix is sound. The missing element is the loop-
context exercise — gated on resolving OQ-S60.7.

**tier_evaluator_verdict non-degenerate verification**: not yet
performed because Step 9 (verdict) was not reached. Deferred to
S-Auto-7.1 or the first successful smoke iter after OQ-S60.7
resolution.

## §8 Adversarial spot-check pre-Codex

Three `git diff --stat` verifications (using dev-prompt reference
`1943ed5` PLUS uncommitted state for final verification at COMMIT 2):

```bash
# 1. Fence #13 scoring siblings UNCHANGED
git diff --stat 1943ed5..HEAD -- \
  autoloop/autoloop/scoring/tier_evaluator.py \
  autoloop/autoloop/scoring/baseline_loader.py \
  autoloop/autoloop/scoring/gaming.py
# Output: empty ✅

# 2. eval-interactive CLI UNCHANGED (fix path (b))
git diff --stat 1943ed5..HEAD -- eval_interactive/eval_interactive/cli.py
# Output: empty ✅

# 3. Java zero-touch
git diff --stat 1943ed5..HEAD -- server/src/main/java/ eval/src/main/java/
# Output: empty ✅

# 4. Resources zero-touch (skills land at S-Auto-8 only)
git diff --stat 1943ed5..HEAD -- server/src/main/resources/skills/
# Output: empty ✅

# 5. case_specs zero-touch
git diff --stat 1943ed5..HEAD -- \
  eval_interactive/case_specs/ eval_interactive/case_specs_shadow/
# Output: empty ✅
```

**Controlled overrides** (allowed per planning + in-session
authorizations):
- `autoloop/autoloop/scoring/eval_runner.py` — fence #13 controlled
  override granted at S-Auto-7 planning round per
  `docs/milestone_objective.md` in-place revision 2026-05-29.
  Rebaselined `scoring_code_baseline_sha` in §5.
- `eval_interactive/eval_interactive/case_spec/loader.py` — granted
  in-session by human at the Blocker C STOP-and-surface
  ("尽快帮我修复掉吧 / fix it now, get auto-loop running ASAP").
  Substrate path-handling change, not a semantic surface.

## §9 Code anchor table

**COMMIT 1** (`559927a`):

| File | +lines | −lines |
|------|-------:|-------:|
| `autoloop/autoloop/scoring/eval_runner.py` | 88 | 16 |
| `autoloop/config.yaml` | 13 | 4 |
| `autoloop/tests/test_eval_runner.py` | 185 | 5 |

**COMMIT 2** (to be created in §13):

| File | Purpose |
|------|---------|
| `autoloop/config.yaml` | Advance `fitness.baseline_dir` to blessed path + inline comment |
| `eval_interactive/eval_interactive/case_spec/loader.py` | Recursive load + skip underscore-prefixed yamls (Blocker C fix) |
| `docs/sprints/sprint-060-handoff.md` | This file |

## §10 Test counts

| Suite | Pre-S-Auto-7 | Post-S-Auto-7 | Delta |
|-------|--------------|---------------|------:|
| `autoloop` pytest | 223 PASS, 1 WARN | 228 PASS, 1 WARN | +5 PASS |
| `eval_interactive` pytest | 486 PASS, 3 FAIL | 486 PASS, 3 FAIL | 0 |
| 17-fixture detector sweep | 31 PASS | 31 PASS (UNCHANGED — not exercised again; not affected by S-Auto-7) | 0 |

Delta breakdown for autoloop:
- `+5` from new test_eval_runner.py tests:
  `test_eval_runner_removes_output_dir_arg`,
  `test_eval_runner_locates_auto_timestamped_output_via_set_diff`,
  `test_eval_runner_returns_suiterunresult_with_correct_paths`,
  `test_eval_runner_no_new_dir_leaves_results_path_absent`,
  `test_locate_new_results_dir_picks_newest_by_mtime_on_multi`.
- 2 existing tests updated in place (no count change):
  `test_run_suite_mocked_success` (asserts `--output-dir not in cmd`)
  and `test_run_v1_fitness_suite_runs_all_three_sequentially`
  (suite identification via `--path` basename).
- Existing 1 warning in `test_tier_evaluator.py::test_layer4_shadow_missing_keeps_with_warning`
  is documented baseline (baseline_loader's intended "missing suite
  → warning" path).

## §11 §7 stanza self-walk verification

§7 EXEMPT confirmed; self-walk content in §1 above. Pure-infra
carve-out applies (no semantic-touching surface modified; eval_runner
adaptation is HOW the subprocess consumes output; loader change is
HOW the case-loader walks; SHA + baseline_dir are config fields).

## §12 Open questions

- **OQ-S60.1 — eval-interactive auto-timestamped output format
  characterization.** Confirmed at §6 baseline run: eval-interactive
  writes ONE timestamped dir per `run` invocation, containing
  `results.json` + `report.html` flat. NOT per-suite-subdir. The
  S-Auto-7 eval_runner.py symlink staging adapts this to the
  per-suite layout downstream consumers expect. **Disposition:
  CLOSED — characterized.**
- **OQ-S60.2 — baseline_loader API contract for v1.** Confirmed at
  §6 baseline_loader verification: primary layout is
  `<baseline_dir>/<suite_name>/results.json`. Loader walks the
  `config.fitness.suites[].name` list and looks for per-suite subdir
  first, then falls back to flat `<baseline_dir>/results.json`. The
  blessed S-Auto-7 baseline uses the per-suite-symlink primary
  layout. **Disposition: CLOSED — characterized.**
- **OQ-S60.3 — Per-suite eval-interactive elapsed times.**
  bad_cases (parallel=1, 12 cases) 7m 23s ≈ 37s/case; anchor_outcome
  (parallel=4, 12 cases) 1m 50s ≈ 9s/case; shadow (parallel=4, 22
  cases) 3m 33s ≈ 10s/case. bad_cases is the wall-clock bottleneck
  by a factor of ~4x because parallel=1 (R-bad-case-parallel-
  session-establishment-flakiness mitigation per M5-close).
  **Disposition: CLOSED — captured.**
- **OQ-S60.4 — Smoke iter elapsed vs estimate.** Goal #4 BLOCKED at
  Step 6 (Spring spawn), 90.4s before crash. The 12-25 min target /
  40 min cap was never reached. Defer accurate timing to first
  successful end-to-end smoke iter after OQ-S60.7 resolution.
  **Disposition: OPEN — pending OQ-S60.7.**
- **OQ-S60.5 — _compute_scoring_code_sha signature.** Inspected at
  S-Auto-7 #2: signature is `_compute_scoring_code_sha() -> str` —
  zero arguments. Hashes the four files in `_SCORING_CODE_FILES`
  relative to `_REPO_ROOT` (autoloop scoring sibling). Used in
  `gaming._check_scoring_code_drift(*, config)` for comparison
  against `config.fitness.scoring_code_baseline_sha`.
  **Disposition: CLOSED — characterized.**
- **OQ-S60.6 — Staging decision A/B/C.** Approach **B (symlink)**
  chosen at #1 per §4 rationale: zero data duplication; preserves
  all eval-interactive artefacts (report.html, per-turn traces);
  preserves the historical `<results_root>/<suite>/results.json`
  downstream-consumer contract transparently; defensive cleanup of
  pre-existing real dirs at the link path via `shutil.rmtree`
  branch. **Disposition: CLOSED — decided.**
- **OQ-S60.7 — Alt-port Spring spawn intermittent (OQ-S58.7
  carryover; smoke iter blocker).** exp-6 reached Step 6 applier
  and failed Spring spawn (rc=1) within 90.4s. Most likely root
  cause is **Flyway migration lock contention** when the foreground
  `:8080` backend is up — both Spring instances open the same
  PostgreSQL `flyway_schema_history` table and the second's lock
  acquisition times out. Secondary candidates: maven multi-module
  `server/target/` build-artefact race; Redis pool exhaustion;
  shared classpath contention. **Disposition: OPEN; fix-iteration
  S-Auto-7.1 candidate per dev prompt.** Suggested experiments:
  (a) stop foreground :8080 before running smoke iter (most direct
  proof); (b) configure alt-port Spring to use an in-memory DB; (c)
  set Flyway timeout / `outOfOrder` setting; (d) sandbox per-iter
  via Docker. **Action**: deliver-agent + human triage at S-Auto-7
  close; may surface as new R-item under M-Auto-1B or carry to
  M-Auto-2.
- **OQ-S60.8 — eval-interactive output-dir flag verified absent.**
  Pre-#1 sanity check (`eval-interactive run --help | grep -i
  "output"`) returned empty output. Fix path (b) preserved
  eval-interactive byte-identical (modulo loader.py per Blocker C
  in-session override). **Disposition: CLOSED — characterized.**
- **OQ-S60.9 — Blocker C in-session loader fix scope verification.**
  Per §6: rglob + skip `_*.yaml`. Negative-control verification (all
  6 flat case_set dirs report same count under rglob as under glob;
  none contain `_*.yaml` files). eval_interactive pytest 486 PASS
  / 3 FAIL UNCHANGED. **Disposition: CLOSED — verified.**
- **OQ-S60.10 — autoloop `.env` loading discipline.** Dev session
  observed that `.env.local` is NOT auto-loaded by `python -m
  autoloop run`; the caller must `set -a; source .env.local; set
  +a` first. This is a documented invariant per `meta_agent/
  llm_client.py:16` but is easy for fresh sessions to miss.
  Consider adding `dotenv.load_dotenv("autoloop/.env.local")` to
  `autoloop/cli.py` to auto-load on `python -m autoloop run`.
  **Disposition: OPEN — minor ergonomics R-item candidate.**

## §13 Commit discipline (for COMMIT 2 — to be created)

**Commit 2** stages:
- `autoloop/config.yaml` — only the `fitness.baseline_dir` change
  (the `scoring_code_baseline_sha` change was in COMMIT 1)
- `eval_interactive/eval_interactive/case_spec/loader.py` — Blocker
  C in-session fix (recursive + skip underscore-prefixed yamls)
- `docs/sprints/sprint-060-handoff.md` — this file

Commit message (per prompt + Blocker C addendum):

```
Sprint 060 / S-Auto-7 / M-Auto-1B — Blocker A baseline_dir blessing + Blocker C loader fix + handoff

Blessed M-Auto-1B baseline directory at
eval_interactive/results/m-auto-1b-baseline-20260529 via per-suite
symlinks to three eval-interactive runs (bad_cases 20260529-101324,
anchor_outcome 20260529-102054, shadow 20260529-102949 — 46 cases
total, ~12m 46s wall-clock). baseline_loader.load returns 3 SuiteSnapshot
with 0 warnings; case_passed (loader-counted) = bad_cases 5/12 +
anchor_outcome 7/12 + shadow 4/22; tier2 mandatory FAILs = 18.

Blocker C (in-session, human-authorized): eval_interactive/eval_interactive/
case_spec/loader.py replaces non-recursive glob with rglob + skips
underscore-prefixed yamls (manifest convention). Zero-impact on existing
flat case_set dirs (anchor 159 / promotion 101 / exploration 107 /
smoke 14 / bad_cases 12 / anchor_outcome 12 — identical counts pre and
post); shadow now loads 22 (the 23rd was _manifest.yaml). eval_interactive
pytest 486 PASS / 3 FAIL UNCHANGED.

Goal #4 (smoke iter end-to-end) BLOCKED at Step 6 alt-port Spring
spawn (rc=1) — OQ-S58.7 carryover (foreground :8080 backend competes
for Flyway lock / maven target/ / Redis pool). NOT a Blocker A/B
failure; substrate fixes independently verified via test suite +
baseline run. STOP-and-surfaced as OQ-S60.7 fix-iteration S-Auto-7.1
candidate.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

**Staging discipline**: explicit file list; NO `git add -A`. Deliver-
agent close-bundle artefacts will be staged by human at S-Auto-7
milestone-close per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

---

**END OF DEV HANDOFF.** §12 reserved for deliver-agent + Codex per
existing convention (this file leaves the closure-verdict slot
empty per dev/deliver separation).
