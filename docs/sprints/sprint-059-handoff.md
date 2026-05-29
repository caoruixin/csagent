---
title: Sprint 059 / S-Auto-6 / M-Auto-1B handoff — close-empty (pre-flight blocked)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-29
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  S-Auto-6 closed empty on 2026-05-29 after two pre-flight blockers were
  surfaced and the deliver-agent + human jointly directed (via two
  AskUserQuestion gates) that the cleanest path was: (i) author a
  pre-flight blocker report, (ii) commit only the handoff, (iii) defer
  the actual overnight + cherry-pick scope to a future substrate-fix
  sub-sprint inside M-Auto-1B. Zero baseline reruns, zero overnight
  iterations, zero cherry-pick. No edits to autoloop/config.yaml,
  autoloop/autoloop/, eval_interactive/, server/, eval/, or docs/
  outside this handoff file.
---

# Sprint 059 / S-Auto-6 / M-Auto-1B — close-empty handoff

## §0. Pre-flight blockers (NEW section; not in standard template)

S-Auto-6 was authored on the assumption that the substrate at S-Auto-5
close HEAD `ae0ec3e` (forward-equivalent at `95089c2` for autoloop and
skills bytes; only docs differ) was ready to drive a first overnight
batch end-to-end. The dev session verified this assumption against
the substrate before consuming any LLM budget and surfaced two
blockers that make the headline Goals #2-#5 structurally infeasible
without a hard-fence override. The deliver-agent + human jointly
chose (via two AskUserQuestion gates) the "stop S-Auto-6 + close
empty + reopen after substrate fix" path; this handoff documents the
findings so the next sub-sprint's planning round has the evidence
load-bearing on its scope decision.

### Blocker A — `autoloop/config.yaml` `fitness.baseline_dir` never advanced past placeholder

**Observation.** `autoloop/config.yaml:110` reads:

```yaml
baseline_dir: eval_interactive/results/<PLACEHOLDER-set-at-M-Auto-1A-close>
```

The literal `<PLACEHOLDER-set-at-M-Auto-1A-close>` string is still
present at HEAD `95089c2`. No live-iter has ever blessed a concrete
baseline directory under `eval_interactive/results/`.

**Why it matters.** `autoloop/autoloop/loop.py:389-431` handles the
`<PLACEHOLDER` substring as a graceful sentinel: `_build_baseline_summary`
returns `{}` and `_load_baseline` returns an empty
`BaselineSnapshot(baseline_run_id="<no-baseline>", snapshots={}, ...)`.
The overnight therefore **runs** without crashing — but downstream
fitness verdicts become degenerate:

- **Layer 0** (Tier-0 safety floor) — unaffected; reads from
  `current_suites` per
  `autoloop/autoloop/scoring/tier_evaluator.py:245-296`.
- **Layer 1** (Tier-1 outcome non-regression) — trivially PASS;
  `_suite_baseline_passed(baseline, suite)` returns `None` against an
  empty snapshot, so the per-suite `cur_n < base_n` regression check at
  `tier_evaluator.py:354-358` short-circuits past the `is not None`
  guard.
- **Layer 2** (Tier-2 critical-flow non-regression) — trivially PASS;
  same `is not None` short-circuit pattern at
  `tier_evaluator.py:399-469`.
- **Layer 3** (improvement threshold) — **trivialized**.
  `tier_evaluator.py:485-532` falls back to `bc_baseline = ... or 0`
  and `ao_baseline = ... or 0`. Any kept candidate that passes ≥1
  case in `bad_cases` or `anchor_outcome` then has `delta ≥ 1 ≥
  improvement_min_cases=1` → trivial PASS. Effectively the "must
  improve" bar collapses to "must not be empty", which is no bar.
- **Layer 4** (shadow regression) — unverified for divide-by-zero
  semantics against `baseline_passed = 0`; at minimum the
  `shadow_max_drop_pct: 3.0` bar is meaningless when the
  denominator is 0.

**Evidence that the code path is real, not theoretical.** The
S-Auto-5 live-iter bootstrap log
`autoloop/results/experiments.jsonl` already shows exp-2's
analyzer rationale containing the verbatim phrase _"No baseline
failure landscape is available this iteration"_ — confirming
`_build_baseline_summary` returns `{}` against the placeholder.

**Why this was not surfaced sooner.** The S-Auto-5 live-iter bootstrap
(exp-1 through exp-4) failed before any iteration reached the eval
step, so Layer 1-4 evaluations never ran against the empty baseline.
The placeholder was inert. Blocker A only becomes load-bearing the
moment Goal #2 produces an iter that survives Step 6 (applier) and
reaches Step 7 (eval) + Step 9 (tier_evaluator).

### Blocker B — `eval_runner.py` invokes `--output-dir`; `eval-interactive` CLI does not support it

**Observation.** `autoloop/autoloop/scoring/eval_runner.py:99-110`
builds the subprocess command:

```python
cmd = [
    "uv", "run", "eval-interactive", "run",
    "--path", str(spec.path),
    "--parallel", str(spec.parallel),
    "--output-dir", str(suite_out_dir.resolve()),
]
```

`eval-interactive run --help` lists no `--output-dir` flag, and
`eval-interactive run --output-dir /tmp/test 2>&1` returns
`Error: No such option: --output-dir` (Click). Manually verified
in this session.

**The flag has never existed.** `git log --all -S "output_dir" --
eval_interactive/eval_interactive/cli.py` returns empty across the
entire history of the file. Sprint 47 (`1576070`) S-Cleanup-1
touched cli.py but did not add or remove `--output-dir`; Sprint 55
S-Auto-2 (`eb55322`) added `eval_runner.py` with the invocation
above, against an `eval-interactive run` CLI contract that was
never shipped on either side of that commit.

**Why it matters.** Every overnight iter that survives Steps 1-6
and reaches Step 7 (`loop.py:251`,
`eval_runner.run_v1_fitness_suite`) will subprocess-fail
immediately with the Click error. `SuiteRunResult.exit_code != 0`,
`results_root` stays empty, and `tier_evaluator.evaluate` at
`loop.py:269` finds no `results.json` under
`autoloop/results/runs/<iter-id>/eval/`. The iter terminates with
either an unhandled exception or a degenerate verdict — either
way, no useful keep / discard signal is produced. The §5.6
manual-review surface (Goal #3) is empty; the AskUserQuestion
cherry-pick gate (Goal #4) has no eligible slate; observation
accumulation (Goal #5) becomes a count of identical CLI errors.

**Why this was not surfaced sooner.** The substrate is hard-fenced
at content-hash `5177b674…` over the four scoring files; the
fence was deliberately frozen at S-Auto-4 close 2026-05-27 before
the first live-iter ran. The S-Auto-5 live-iter bootstrap
exercised Steps 4-6 but not Step 7, so the CLI mismatch never
manifested. S-Auto-6 is the first session that would actually
drive an iter through Step 7 — and the contract's hard fences
forbid touching either side of the mismatch.

### Joint decision (AskUserQuestion record)

Two gates fired during this session:

| gate | question | options surfaced | selection |
|---|---|---|---|
| Pre-flight #1 | `baseline_dir` is still placeholder; how should S-Auto-6 proceed? | (a) bless rerun #1 as new baseline_dir, (b) accept empty-baseline overnight, (c) stop + defer | (a) bless |
| Pre-flight #2 | `eval_runner.py` invokes `--output-dir` which has never existed in CLI; both surfaces fenced; how to proceed? | (1) stop + close empty, (2) narrow fence override (eval_runner.py only), (3) Goal #1 only + defer Goal #2-5, (4) full fence override + end-to-end smoke | (1) stop + close empty |

The second gate superseded the first: once it became clear that
Goal #2 onward is structurally blocked by Blocker B regardless of
whether baseline_dir is advanced, the value of running Goal #1
baseline reruns also collapsed (no overnight evidence to anchor
the drift envelope against, no cherry-pick decision to inform).
Closing empty preserves zero hard-fence overrides and zero LLM
budget; M-Auto-1B planning round has clean ground to add a
substrate-fix sub-sprint.

## §1. Class + §7 stanza self-walk

The S-Auto-6 contract classed the sub-sprint as `eval_spec`
(§3.2 Q6) and §7 REQUIRED, because the cherry-picked Skill YAML
edit (if any) would touch an LLM-soft-narrative surface (§1.3 LLM
owns content). **No semantic-touching change shipped in this
session.** The class hypothesis stands but is unexercised.

Self-walk of the four §7 fields against delivered scope:

- **Target failure layer.** Would have been `eval_spec`. Delivered
  scope = zero semantic-touching change → no failure layer was
  acted on.
- **Tier-0 invariant.** Adds none. Trivially satisfied (no change).
- **Semantic hardcode.** None introduced. Trivially satisfied (no
  change).
- **Generalization coverage.** Target / neighbor / negative /
  shadow case families unused (no overnight evidence to filter
  through them). Trivially satisfied (no change).

The handoff itself is docs-only and would normally be exempt from
the §7 stanza (per iteration_governance.md §7). The stanza is
self-walked here for paper-trail completeness, not because docs-only
edits require it.

## §2. Goal achievement

| goal | status | evidence pointer |
|---|---|---|
| #1 Pre-batch baseline drift envelope established | SKIPPED-WITH-REASON | Blocker B makes downstream Goal #2 infeasible; running Goal #1 in isolation produces no actionable signal without an overnight to anchor against. §0 Blocker A also independently affects the run-blessing step. |
| #2 First overnight batch executed | SKIPPED-WITH-REASON | Blocker B: `eval_runner.py` invokes non-existent `--output-dir` flag; every iter at Step 7 would error. Hard-fence override required to fix; user selected "stop + close empty" path. |
| #3 First §5.6-style manual review of kept candidates | SKIPPED-WITH-REASON | No overnight → no kept candidates to review. |
| #4 First cherry-pick decision via AskUserQuestion | SKIPPED-WITH-REASON | No eligible candidates → no slate to surface. |
| #5 Observation accumulation + R-S58 disposition recommendation | SKIPPED-WITH-REASON | No overnight observation evidence. R-S58 disposition recommendation deferred to M-Auto-1B planning round (see §9 below). |
| #6 Milestone-shared Codex review at M-Auto-1B close | DEFERRED (out of S-Auto-6 scope per §4.3 default) | No per-sub-sprint Codex trigger fired this session (no Tier-0 candidate, no §1.7 red line, no hard-fence violation, no prior-sub-sprint fix_required iteration). Codex dispatch remains deliver-agent + human at M-Auto-1B close. |

## §3. Scope execution log

| scope step | status | notes |
|---|---|---|
| #1 Pre-batch baseline rerun + drift envelope | SKIPPED-WITH-REASON | Pre-flight blockers surfaced before any baseline rerun launched. |
| #2 Pre-overnight smoke + overnight batch | SKIPPED-WITH-REASON | Structurally infeasible per Blocker B. |
| #3 §5.6 manual review of kept candidates | SKIPPED-WITH-REASON | No kept candidates. |
| #4 AskUserQuestion cherry-pick decision | SKIPPED-WITH-REASON | No eligible slate. |
| #5 Observation accumulation + R-S58 recommendation | SKIPPED-WITH-REASON | No evidence. R-S58 disposition deferred. |

The two AskUserQuestion gates that DID fire in this session (the
two pre-flight gates) are recorded in §0; they are not scope
steps in the contract sense but are part of the close decision
chain.

## §4. Pre-batch baseline drift envelope

SKIPPED. No baseline reruns executed. The contract called for ≥2
reruns of the 47-case fitness suite (bad_cases ×12 + anchor_outcome
×12 + shadow ×23) on the calibrated detector at HEAD `95089c2`
(forward-equivalent to S-Auto-5 close `ae0ec3e` for substrate
bytes). The expected per-suite case_passed median + IQR + drift
envelope width are unmeasured this session.

For the next sub-sprint's planning round, the baseline-rerun cost
remains roughly:

- bad_cases (parallel=1, 12 cases) — ~1.4-3h per rerun.
- anchor_outcome (parallel=4, 12 cases) — ~0.4-1h per rerun.
- shadow (parallel=4, 23 cases) — ~0.7-1.5h per rerun.
- Total per rerun: ~2.5-5.5h. Two reruns: ~5-11h.

## §5. Pre-overnight smoke + overnight batch record

SKIPPED. The smoke `--experiments 1` was not invoked; the overnight
batch was not invoked. No per-iteration table to report; no
cumulative keep / discard / error counts; no
`autoloop/results/lessons.md` written; no K=10 lessons_compactor
trigger evaluated.

## §6. Manual review of kept candidates

SKIPPED. No kept candidates produced.

## §7. Cherry-pick decision

SKIPPED. No eligible candidates slate; no AskUserQuestion cherry-pick
gate fired. **0 cherry-pick** is recorded as the formal outcome,
with justification: substrate pre-flight blocked, no overnight
evidence, no candidates to consider. This is consistent with the
M-Auto-1B fence #7 / §6 "AT MOST 1 cherry-pick" — 0 is permitted.

`autoloop/config.yaml` `fitness.baseline_dir` was NOT advanced this
session (it remains the literal placeholder string). No commit to
config.yaml.

## §8. Observation accumulation

SKIPPED. No overnight to accumulate from. The S-Auto-5 bootstrap
data in `autoloop/results/experiments.jsonl` covers 4 prior exp
attempts (exp-1 SDK ModuleNotFoundError, exp-2 SpringStartupTimeoutError,
exp-3 + exp-4 content_validator length_overflow); none reached
Step 7 (eval). That data was already documented in S-Auto-5
handoff and is not re-summarized here.

## §9. R-S58 disposition recommendation

**Recommendation: defer R-S58 disposition to M-Auto-1B planning
round, AFTER the substrate-fix sub-sprint lands.** The bypass
surface check (Scope #5 of the original contract) requires a
real-meta-agent propose distribution to scan against, which only
the overnight batch produces. Without that distribution, neither
"defer-to-M-Auto-2" nor "extend-M-Auto-1B-with-S-Auto-7-Fix-D" can
be evidenced.

The substrate-fix sub-sprint (provisionally S-Auto-7 / Fix-D in
the contract's language, but more naturally a substrate-only
sub-sprint that does not invoke R-S58 framing) is the logical
next step regardless of R-S58. Once it lands and an overnight runs
against a calibrated baseline + a working eval_runner, the
R-S58 disposition becomes evidence-driven.

## §10. Code anchor table

Anchors verified in this session (read-only):

| path | range | finding |
|---|---|---|
| `autoloop/config.yaml` | line 110 | `baseline_dir: eval_interactive/results/<PLACEHOLDER-set-at-M-Auto-1A-close>` — placeholder unchanged. |
| `autoloop/autoloop/loop.py` | lines 386-441 | `_build_baseline_summary` + `_load_baseline` graceful empty-snapshot handling on placeholder. |
| `autoloop/autoloop/loop.py` | lines 244-264 | Step 7 invokes `eval_runner.run_v1_fitness_suite` then unconditionally serializes results — no exit_code check. |
| `autoloop/autoloop/scoring/eval_runner.py` | lines 99-110 | subprocess invocation includes `--output-dir <suite_out_dir>`. |
| `autoloop/autoloop/scoring/tier_evaluator.py` | lines 475-532 | Layer 3 `_evaluate_layer3` `... or 0` fallback trivializes improvement threshold against empty baseline. |
| `eval_interactive/eval_interactive/cli.py` | (no anchor — flag absent) | `eval-interactive run --help` lists no `--output-dir`; `git log -S "output_dir"` against the file is empty across full history. |
| `autoloop/results/experiments.jsonl` | tail (exp-1 to exp-4) | All 4 S-Auto-5 bootstrap iters failed before Step 7; the eval_runner CLI mismatch never manifested. |

The only commit produced this session is the handoff commit itself;
its numstat is captured by `git show --numstat` against the
S-Auto-6 close SHA.

## §11. Test count

UNCHANGED from S-Auto-5 close. No source files modified this
session; no tests invoked. Expected baselines (per the dev prompt):

- `cd autoloop && uv run --extra dev pytest -q` → `223 passed, 1 warning` UNCHANGED.
- `cd eval_interactive && uv run python -m pytest --tb=no -q` → `486 passed, 3 failed` UNCHANGED.
- `cd server && mvn test -B -pl server` → `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED (also: no Skill YAML edit means no Java-side risk regression by construction).

Not re-run this session because no semantic-touching or code change
landed. Hard-fence zero-touch verifies the same.

## §12. OQ-S59.x list

| id | observation | disposition recommendation |
|---|---|---|
| **OQ-S59.A** | `autoloop/config.yaml` `fitness.baseline_dir` is still the literal `<PLACEHOLDER-set-at-M-Auto-1A-close>` string. `loop.py:389-431` handles it gracefully via empty `BaselineSnapshot`, but Layer 3 trivializes (`tier_evaluator.py:485-532` `... or 0` fallback) and Layer 4 shadow_max_drop_pct against baseline=0 is unverified for divide-by-zero. Confirmed unexercised in S-Auto-5 bootstrap (`experiments.jsonl` exp-1 to exp-4 never reached Step 9). | Substrate-fix sub-sprint blesses a concrete baseline run by running the 47-case fitness suite on HEAD, advancing `baseline_dir` past the placeholder, and committing the config edit. Requires deliver-agent + human delegation for the config.yaml edit (per close-bundle convention). |
| **OQ-S59.B** | `autoloop/autoloop/scoring/eval_runner.py:99-110` invokes `eval-interactive run ... --output-dir <suite_out_dir>`. The `--output-dir` flag has NEVER existed in `eval_interactive/eval_interactive/cli.py` at any commit (verified via `git log -S "output_dir"` returning empty across full history). Every overnight iter that reaches Step 7 fails with `Error: No such option: --output-dir`. Both surfaces are hard-fenced; eval_runner is in the content-hash-locked group at `5177b674…` (fence #13). | Substrate-fix sub-sprint reconciles the mismatch. Two viable shapes: (a) ADD `--output-dir` to eval-interactive CLI (changes `eval_interactive/` — hard-fenced; requires deliver-agent + human override; risk: side effects in eval-interactive consumers), OR (b) REMOVE `--output-dir` from eval_runner.py and adapt eval_runner to consume eval-interactive's auto-timestamped `results/<ts>/` output (changes `eval_runner.py` — hard-fenced; requires deliver-agent + human override + scoring_code_baseline_sha rebaseline; risk: scoring drift detector fires until config is updated). Option (b) is the smaller blast radius. Final shape is the substrate-fix sub-sprint's call. |
| **OQ-S59.C** | The S-Auto-2 (`eb55322`) commit landed an `eval_runner.py` that integration-tested only against mocked subprocess; no live-iter has driven it end-to-end. The same pattern may surface additional latent integration bugs at Step 7 / Step 9 once Blocker B is resolved. | Substrate-fix sub-sprint's smoke step (`--experiments 1` full 14-step state machine) is the first real integration test of S-Auto-2 + S-Auto-3 + S-Auto-4 substrate end-to-end. Plan for STOP-and-surface budget if additional latent bugs surface during smoke. |
| **OQ-S59.D** | The "≤5/34 cases" drift envelope width in the S-Auto-6 contract refers to M-Auto-1A close-day signature, but the 47-case fitness suite has 47 cases (bad_cases 12 + anchor_outcome 12 + shadow 23). The denominator "34" may have referred to a different scope (e.g., bad_cases + anchor_outcome + shadow_subset, or a pre-shadow-expansion era). | Substrate-fix sub-sprint's planning round should resolve the denominator: 34 vs 47. Most likely the intent is bad_cases + anchor_outcome (24) + a 10-shadow-subset (34 total) per a prior scope decision; alternative reading is that "5/34" is a rate of 14.7% applied to whatever the actual suite total is. Surface for explicit human-locking at next planning round. |
| **OQ-S59.E** | S-Auto-5 OQ-S58.7 ("Spring spawn 1/3 = 33%") observation remains unconverted to action. With Blocker B in the way, even if Spring spawn succeeds the eval fails at Step 7 → the Spring-spawn rate question can only be answered after Blocker B is resolved. | Substrate-fix sub-sprint's smoke is the first chance to measure Spring spawn success rate over more than 1 attempt. If Spring spawn rate stays high (>33% failure) after Blocker B is fixed, additional applier hardening is in scope for M-Auto-1B; otherwise defer to M-Auto-2. |

---

**End of close-empty handoff.** §12 (the OQ list) is filled; the
deliver-agent + human at M-Auto-1B close write the milestone-level
verdict separately. The next sub-sprint contract (substrate-fix)
is to be drafted by the deliver-agent at the M-Auto-1B planning
round, anchored against §0 + §12 of this handoff.
