---
title: Sprint objective — Sprint 061 / M-Auto-1B S-Auto-7.1 — Pre-flight env check + smoke iter completion
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-29
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-060-objective.md]
superseded_by: null
notes: >
  FOURTH sub-sprint of Milestone M-Auto-1B (extended from 4 to 5 per
  S-Auto-7 close in-place revision per §8.3; §8.5 ceiling = 5
  sub-sprints; margin = 0 — any further fix-iteration would force
  M-Auto-1B to split). S-Auto-7.1 is a fix-iteration sub-sprint for
  the Goal #4 BLOCKED outcome of S-Auto-7 (OQ-S60.7 alt-port Spring
  spawn rc=1 — the OQ-S58.7 1/3-intermittent carryover from S-Auto-5
  surfaced in the post-baseline-run window because the foreground
  :8080 Spring backend competed with the alt-port spawn for shared
  Flyway lock / maven target/ / Redis pool resources).

  S-Auto-7 substrate-fix scope was ACCEPTED at S-Auto-7 close
  2026-05-29 per the per-sub-sprint Codex review (`decision: pass /
  blocking_count: 0`; §4.1 verdict `approve with downgrade-to-signal
  follow-up`; only governance-hygiene trigger = annotate
  milestone_objective.md §6 fence #2 for the in-session loader.py
  controlled override — addressed in the S-Auto-7 close-bundle). The
  remaining gap is the contracted Goal #4 smoke iter through Step 9
  with non-degenerate tier_evaluator_verdict, which structurally
  could not complete in S-Auto-7 due to the upstream Spring spawn
  issue.

  Human-locked S-Auto-7 close decisions 2026-05-29 (AskUserQuestion):
  (a) close classification B — Surfaced findings need fix-iteration,
  sub-classified `approve with downgrade-to-signal follow-up`;
  (b) S-Auto-7.1 dispatch with fix approach (a) per dev handoff §12
  OQ-S60.7 suggestion list — "stop foreground :8080 before running
  smoke iter (most direct proof)"; (c) the deeper principle informing
  approach (a) is: **pre-flight the environment at the outer-loop /
  experiment entry point; in dev, reboot misbehaving services
  before diagnosing mid-iteration crashes** (saved as memory
  `feedback_preflight_env_check_outer_loop`).

  Class = `infra` (substrate ergonomics + environment pre-flight;
  structural plumbing repair on the auto-loop entry point). §7
  stanza EXEMPT per pure-infra carve-out (pre-flight check is HOW the
  loop starts, NOT a semantic decision change; smoke iter execution
  exercises existing logic without modifying it). §7 self-walked for
  paper-trail completeness only.

  Codex review plan: **DEFAULT milestone-shared at M-Auto-1B close**
  (no §4.3 trigger expected; the planned scope does NOT touch any
  new hard-fenced surface, NOT introduce a Tier-0 candidate, NOT
  cross any §1.7 forbidden-list red line). UPGRADES to per-sub-sprint
  Codex per §4.3 trigger #3 ONLY IF the pre-flight implementation
  ends up needing to touch a fence #2 / #13 surface (NOT expected
  per the approach (a) design — pre-flight lives in autoloop/cli.py
  + new autoloop/preflight.py).

  Builds on S-Auto-7 close `b0a3704`. The substrate state at
  S-Auto-7.1 open: `eval_runner.py` Blocker B fix path (b) +
  scoring_code_baseline_sha `22548e20…` + blessed `baseline_dir =
  eval_interactive/results/m-auto-1b-baseline-20260529/` + in-session
  loader.py Blocker C fix all LANDED. Codex-accepted; M-Auto-1B §6
  fence #2 amended with loader.py controlled-override annotation in
  the S-Auto-7 close-bundle.
---

# Sprint 061 / M-Auto-1B S-Auto-7.1 — Pre-flight env check + smoke iter completion

## Class

`infra` (§3.2 Q1 — substrate ergonomics + environment pre-flight; structural plumbing repair on the auto-loop entry point; no semantic decision change, no projection / scoring semantic logic edit, no CaseSpec / judge change). **§7 EXEMPT** per pure-infra carve-out + self-walked for paper-trail completeness (similar to S-Auto-1 / S-Auto-3 / S-Auto-6 / S-Auto-7 self-walk pattern).

## Goal

S-Auto-7.1 close 时:

1. **Pre-flight env check lands at the outer-loop entry point.** A new pre-flight check runs FIRST when `python -m autoloop run` is invoked (or as a standalone `python -m autoloop preflight` command). The check enumerates: (a) is a Spring backend bound on the project's configured port (likely `:8080`)?; (b) is PostgreSQL reachable on the configured port?; (c) is Redis reachable?; (d) is `AUTOLOOP_META_LLM_API_KEY` set and non-empty?; (e) is `git status` clean on the current branch?; (f) does the configured `baseline_dir` exist and load cleanly via `baseline_loader.load`? Each check returns a structured `PreflightResult` with `status: ok | warn | fail` + diagnostic message; the loop refuses to start if any check returns `fail` (unless `--skip-preflight` escape hatch).

2. **Reboot-as-recovery for failing services in dev (the human-locked principle).** When a pre-flight check returns `fail` and the failure mode is "service unreachable" or "port conflict" or "stale process holds resource", the pre-flight check either auto-recovers (e.g., kills the offending foreground :8080 backend if `--auto-reboot` is passed or if running in non-interactive mode the human has pre-authorized) OR surfaces an actionable message ("Run `lsof -i :8080` and `kill <pid>` first, then re-run") to the operator. Production reboot semantics are NOT in scope; the pre-flight ergonomics tool is dev-only per the constitution's principle on dev-environment workflows.

3. **Goal #4 retired — smoke iter completes through Step 9 with non-degenerate `tier_evaluator_verdict`.** With pre-flight running first (and the foreground :8080 backend stopped per its recommendation), `python -m autoloop run --experiments 1` (NO `--dry-run`) drives the full 14-step state machine end-to-end on `auto-loop-branch`. Step 6 (applier alt-port Spring spawn) must succeed; Step 7 (`eval_runner.run_v1_fitness_suite`) must produce non-empty `SuiteRunResult` with the new symlink staging; Step 9 (`tier_evaluator.evaluate`) must produce non-degenerate Layer 0-4 outcomes (each Layer reports PASS / FAIL / informational metric rather than the empty-snapshot short-circuit pattern from S-Auto-5).

4. **OQ-S60.7 dispositioned in the handoff.** The Spring spawn root-cause (Flyway lock contention / maven multi-module target/ race / Redis pool exhaustion / classpath contention) is investigated to the depth needed to confirm the pre-flight stop-foreground-backend approach is sufficient. Deeper root-cause fixes (in-memory DB / Flyway timeout config / Docker sandbox per-iter) are DEFERRED to M-Auto-2+ as observation-only — the dev-environment ergonomic principle (reboot is fine in dev) accepts the workaround as acceptable for M-Auto-1B's auto-evolution scope.

5. **Per-sub-sprint Codex prep** is NOT needed in default scope. Codex is deferred to milestone-shared at M-Auto-1B close. UPGRADE TO PER-SUB-SPRINT CODEX (§4.3 trigger #3) only if the pre-flight implementation ends up touching a new hard-fenced surface (NOT expected per approach (a) design — pre-flight lives in `autoloop/cli.py` + NEW `autoloop/autoloop/preflight.py`; neither is fenced).

**Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (the four scoring files at re-baselined hash `22548e20…`; fence #13 reasserted at S-Auto-7 close). **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py` (S-Auto-5 calibrated detector + sandbox substrate UNCHANGED). **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` (the loop core + meta-agent + memory substrate; pre-flight is invoked from `cli.py` BEFORE `loop.run_loop()` so the integration is at the entry point, not inside the loop state machine). **Zero touch** to `eval_interactive/eval_interactive/**` (the loader.py controlled override from S-Auto-7 stays in place; no further eval-interactive edits). **Zero touch** to `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/` (NO cherry-pick in S-Auto-7.1; that's S-Auto-8 scope only), `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives (other than S-Auto-7.1's own archives at close), `docs/codex-findings.md` (the live file carrying the S-Auto-7 per-sub-sprint review until M-Auto-1B close).

## Scope (numbered; this is the contract)

### #1 — Implement pre-flight env check module (`autoloop/autoloop/preflight.py` NEW)

Create a new `autoloop/autoloop/preflight.py` module containing:

- A `PreflightResult` dataclass with fields `name: str`, `status: Literal["ok", "warn", "fail"]`, `message: str`, `remediation: Optional[str]`.
- A `run_preflight(config: dict) -> list[PreflightResult]` function that runs all checks in sequence:
  - `check_foreground_backend(config)` — `lsof -i :8080` (or whatever port the config declares); if a Spring process is listening, return `fail` with remediation "Stop the foreground backend with `kill <pid>` or re-run with `--auto-reboot`". (Approach: use stdlib `subprocess.run` with timeout.)
  - `check_postgres_reachable(config)` — try opening a TCP connection to the configured PostgreSQL host:port (likely `localhost:5432`); return `fail` if unreachable with remediation "Run `brew services start postgresql@<version>` (or your equivalent)".
  - `check_redis_reachable(config)` — same pattern for Redis (likely `localhost:6379`); return `fail` if unreachable with remediation.
  - `check_meta_llm_api_key()` — verify `AUTOLOOP_META_LLM_API_KEY` is set in env (NOT just declared in `.env.local`); return `fail` with remediation "Run `set -a; source autoloop/.env.local; set +a`" (this is the OQ-S60.10 ergonomics surface — see #4 below).
  - `check_clean_working_tree(config)` — run `git status --porcelain`; if non-empty, return `warn` with remediation "Commit or stash before running the loop". (Why `warn` not `fail`: dev may have an intentional working-tree state.)
  - `check_baseline_dir_loads(config)` — invoke `baseline_loader.load(config.fitness.baseline_dir)`; if it raises or returns warnings, return `fail` with remediation.
- An optional `auto_reboot_foreground_backend(pid: int)` helper that the loop entry point calls IFF `--auto-reboot` was passed AND `check_foreground_backend` returned `fail` with the conflict pid.

Stdlib + `subprocess` + `pathlib` + `socket` only (no new heavy deps in `autoloop/pyproject.toml`).

Read `autoloop/autoloop/cli.py` FIRST to understand the existing entry-point structure (`python -m autoloop run` argparse / Click integration). The pre-flight should hook into the `run` subcommand AND be invocable as a standalone subcommand `python -m autoloop preflight`.

### #2 — Wire pre-flight into the CLI entry point (`autoloop/autoloop/cli.py` EXTEND)

In `autoloop/autoloop/cli.py`:

1. Import `preflight.run_preflight` + `auto_reboot_foreground_backend`.
2. Add a `preflight` subcommand: `python -m autoloop preflight` runs the checks + prints a structured report.
3. In the `run` subcommand: BEFORE `loop.run_loop()` is invoked, call `run_preflight(config)` and print results. If any result has `status: fail`:
   - Without `--skip-preflight`: refuse to start; print remediation list; exit with non-zero status.
   - With `--auto-reboot` (new flag): for the specific `check_foreground_backend` fail case, call `auto_reboot_foreground_backend(pid)` to kill the conflict; re-run pre-flight; if still failing, refuse to start.
   - With `--skip-preflight` (escape hatch; documented as dev-only): print warning and proceed regardless.

Print the pre-flight report to the same stdout stream as the loop output (NOT to a separate log file) so the dev sees env state alongside iteration progress.

### #3 — Smoke iter via pre-flight path

With #1 + #2 landed, run the smoke iter:

```bash
# Stop the foreground backend FIRST (or use --auto-reboot per #2)
lsof -i :8080  # identify the conflicting PID
kill <pid>     # stop the foreground Spring

# Then dispatch the smoke iter (pre-flight will verify env)
cd autoloop && uv run --extra dev python -m autoloop run --experiments 1
```

(Optionally test the `--auto-reboot` path: leave the foreground backend running and dispatch with `python -m autoloop run --experiments 1 --auto-reboot`; pre-flight kills the foreground and proceeds.)

The full 14-step state machine must execute end-to-end. Critically: Steps 6 (alt-port Spring spawn), 7 (eval_runner), AND 9 (tier_evaluator) must reach non-degenerate outputs.

Record:

- Iteration outcome (keep / discard / error; any acceptable per scope acceptance bar).
- Per-iter wall-clock elapsed time (FIRST measurement of full Spring-spawn + 47-case-eval cycle on the now-blessed baseline).
- `autoloop/results/runs/exp-<N>/iteration_record.json` contents — verify `tier_evaluator_verdict` is non-empty + Layer 0-4 outcomes are non-degenerate (each Layer reports PASS / FAIL / informational metric, NOT the empty-snapshot short-circuit from S-Auto-5; NOT the unreachable-step pattern from S-Auto-7 exp-6).
- `autoloop/results/runs/exp-<N>/` artefacts: `hypothesis.json`, `diff.yaml`, `sandbox_verdict.json`, `anti_hardcode_verdict.json`, `tier_evaluator_verdict.json`, eval per-suite outputs via the symlink staging from S-Auto-7 Blocker B fix.

**If smoke iter still crashes** in an unhandled path NOT caused by foreground-backend contention (e.g., a different substrate brittleness surfaces): STOP and surface. Likely fix-iteration S-Auto-7.2 candidate — and at that point M-Auto-1B exceeds the §8.5 5-sub-sprint ceiling and the deliver-agent + human must consider splitting M-Auto-1B per §8.5 advice.

**If per-iter elapsed >40 min**: continue this iteration to completion but record the observation as M-Auto-2 optimization R-item.

### #4 — OQ-S60.10 ergonomics fix (optional within S-Auto-7.1 scope; bundle if trivial)

Per dev handoff §12 OQ-S60.10: `python -m autoloop run` does NOT auto-load `autoloop/.env.local`; the caller must `set -a; source .env.local; set +a` first. This is a documented invariant per `meta_agent/llm_client.py:16` but is easy for fresh sessions to miss.

If the dev judges the fix is trivial (~5 LOC at the top of `autoloop/autoloop/cli.py` to invoke `dotenv.load_dotenv(Path(__file__).parent.parent / ".env.local")` at module-load OR at the start of the `run` subcommand handler), bundle it into S-Auto-7.1 commit 1. Add `python-dotenv` to `autoloop/pyproject.toml` if not already declared. (`meta_agent/` itself is fence-13-adjacent; do NOT edit `meta_agent/llm_client.py:16` itself — the auto-load lives in `cli.py` as a higher-layer convenience.)

If the fix is NON-trivial (e.g., requires conditional config-path resolution), DEFER as new R-item `R-autoloop-env-local-auto-load` for M-Auto-2 ergonomics.

### #5 — Handoff + Codex review prep

Author `docs/sprints/sprint-061-handoff.md` (per §"Handoff requirements" below). Codex review at M-Auto-1B close is default milestone-shared; deliver-agent does NOT dispatch per-sub-sprint Codex for S-Auto-7.1 UNLESS the dev encounters a §4.3 trigger condition mid-sub-sprint (e.g., needs to touch a fence #2 surface beyond loader.py's already-blessed override).

## Hard fences / STOP conditions

- **Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py`. Fence #13 reasserted at S-Auto-7 close at hash `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`. Any change triggers `gaming.scoring_code_drift.sha_changed` ERROR.
- **Zero touch** to `eval_interactive/eval_interactive/**` (the loader.py controlled override from S-Auto-7 stays in place; no further edits).
- **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py`, `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/`, `autoloop/autoloop/memory/`.
- **Zero touch** to `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`.
- **Zero touch** to `server/src/main/java/**`, `eval/src/main/java/**`, `data/`, `db/`, `server/src/main/resources/` (cherry-pick to skills/ is S-Auto-8 scope ONLY).
- **Zero touch** to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives (other than S-Auto-7.1's own), milestone archives, `docs/codex-findings.md` scaffold.
- **No new heavy deps** in `autoloop/pyproject.toml`. Stdlib `subprocess` + `socket` + `pathlib` only. `python-dotenv` allowed only if #4 ergonomics fix lands and dotenv is not already declared.
- **No LLM call** in `preflight.py` or its tests (pre-flight is deterministic substrate-level discovery).
- **No `git add -A`** — stage S-Auto-7.1 scope explicitly.
- **No cherry-pick to main** in S-Auto-7.1 (M-Auto-1B fence #7 / #17 allows EXACTLY ONCE in S-Auto-8 ONLY).
- **No production reboot semantics**. The pre-flight + auto-reboot ergonomics tool is dev-only. Production substrate restart semantics are governed by `runtime_freeze_and_risk_policy.md` and on-call runbooks, NOT by this sub-sprint.
- **STOP and surface** conditions:
  - Pre-flight implementation requires touching a hard-fenced surface BEYOND `autoloop/cli.py` + NEW `autoloop/preflight.py` (e.g., needs to touch `loop.py` or `meta_agent/` — both are not strictly fenced at M-Auto-1B level but ARE in the S-Auto-7.1 zero-touch list; surface to deliver-agent + human before proceeding).
  - Smoke iter still crashes after pre-flight stops foreground backend (different brittleness surfaces; likely fix-iteration S-Auto-7.2 candidate; M-Auto-1B exceeds §8.5 ceiling and must split).
  - OQ-S60.10 fix turns out to be non-trivial (>10 LOC or requires new config-path resolution). Defer to new R-item.
  - Pre-flight surfaces an unexpected substrate brittleness (e.g., baseline_dir symlinks broken; case_specs path missing; etc.). Surface as observation; do NOT auto-fix mid-sub-sprint.

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run --extra dev pytest -q` — S-Auto-7 close baseline `228 passed, 1 warning` MUST grow by ~4-8 new S-Auto-7.1 tests covering: each `PreflightResult` check returning ok/warn/fail; auto_reboot helper kills the right pid; CLI integration: `python -m autoloop preflight` standalone runs all checks; CLI integration: `python -m autoloop run` refuses to start without `--skip-preflight` if pre-flight fails. Use mock-subprocess fixtures for `lsof` / Spring backend simulation; mock-socket fixtures for PostgreSQL / Redis reachability. Total expected `232-236 passed, 1 warning`. The 1 warning is the S-Auto-2 baseline-missing-shadow asserted behaviour UNCHANGED.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed` (OQ-S47.3 env-specific failures).
- **Java baseline UNCHANGED**: skipped per Java-zero-touch (verify `git diff --stat <s-auto-7-close-sha>..HEAD -- server/src/main/java/ eval/src/main/java/` returns empty).
- **17-fixture detector calibration sweep**: `cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py` UNCHANGED at 31 passed (S-Auto-7.1 does NOT touch the detector).
- **scoring_code_baseline_sha UNCHANGED**: `22548e20…` reasserted; `_check_scoring_code_drift(config) == []` silent (S-Auto-7.1 does NOT touch the four scoring files).
- **Live smoke iter completes**: 1 iter completed end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`. Per-iter elapsed recorded.
- **No live LLM call in pytest tests** — preflight tests use mocked-subprocess + mocked-socket only.

## §7 — Layer-classification + anti-hardcode stanza (EXEMPT; self-walked for paper-trail)

**§7 stanza is NOT REQUIRED for pure-infra substrate-fix sub-sprints per `iteration_governance.md` §7**. Self-walked here for paper-trail completeness only:

**Target failure layer:** `infra` (§3.2 Q1 — substrate ergonomics + environment pre-flight; structural plumbing repair on the auto-loop entry point; `cli.py` integration + NEW `preflight.py`; the change is HOW the loop starts, NOT a semantic decision branch).

**Tier-0 invariant:** adds no Tier-0 invariant. Pre-flight is environment discovery + reboot ergonomics, NOT a Tier-0 runtime invariant.

**Semantic hardcode:** No semantic hardcode introduced. Justification:

- Pre-flight checks (foreground-backend / PostgreSQL / Redis / API key / clean working tree / baseline_dir loads) are **environment discovery** — they read system state, not encode semantic decisions about user goals / use case hypothesis / escalation posture.
- Reboot semantics (auto-kill foreground :8080 backend on `--auto-reboot`) are **substrate plumbing** — no regex on case content, no keyword list on user utterances, no if-else decision branch on a semantic surface.
- The OQ-S60.10 `dotenv.load_dotenv` ergonomics fix is a config-path resolution, NOT a runtime semantic decision.
- Smoke iter execution exercises the existing semantic logic (S-Auto-5 calibrated detector + meta-agent propose + sandbox + content_validator + tier_evaluator) WITHOUT modifying any of it.

**Generalization coverage:**

- **target** = pre-flight check correctly identifies foreground-backend port conflict on :8080; `--auto-reboot` kills the conflict; smoke iter reaches Step 9 with non-degenerate `tier_evaluator_verdict`.
- **neighbor** = pre-flight checks for other services (PostgreSQL / Redis / API key / working tree / baseline_dir) each report correctly on mocked-fail cases.
- **negative-control** = pre-flight returns all-ok on a clean env; `python -m autoloop preflight` standalone returns 0; `python -m autoloop run` proceeds without warning; smoke iter completes.
- **shadow** = shadow firewall posture UNCHANGED. Pre-flight does NOT read shadow case content; only loads aggregate baseline_dir for the baseline_loader.load check.

## Codex review plan (§4.3)

**DEFAULT — MILESTONE-SHARED at M-Auto-1B close.** The S-Auto-7.1 sub-sprint does NOT cross any §4.3 per-sub-sprint trigger:

- Trigger #1 (Tier-0 candidate): N/A — adds no Tier-0.
- Trigger #2 (§1.7 forbidden-list red line): N/A — pre-flight is environment discovery + reboot ergonomics, NOT a semantic decision encoded as keyword / regex / if-else.
- Trigger #3 (hard-fenced surface explicitly out of scope): N/A — pre-flight implementation lives in `autoloop/cli.py` (extended) + NEW `autoloop/autoloop/preflight.py`; neither is fenced. The S-Auto-7 fence #13 controlled override and the in-session loader.py fence #2 override are settled at S-Auto-7 close.
- Trigger #4 (prior sub-sprint fix_required outcome): N/A — S-Auto-7 closed `approve with downgrade-to-signal follow-up` (the Codex governance trigger is addressed in the S-Auto-7 close-bundle; this S-Auto-7.1 is the contract-anticipated fix-iteration for Goal #4, NOT a Codex-required re-review).

**UPGRADE TO PER-SUB-SPRINT CODEX (§4.3 trigger #3)** ONLY IF the dev encounters a substrate brittleness during implementation that requires touching a new fence surface (e.g., `loop.py` integration is unavoidable). The dev surfaces the upgrade decision to deliver-agent + human via STOP-and-surface BEFORE proceeding.

**Codex prompt timing (if upgrade triggers)**: deliver-agent authors `compact/sprint-061-codex-review-prompt.md` at S-Auto-7.1 close (NOT at open), covering the actual delivered commit range.

## Handoff requirements

Author `docs/sprints/sprint-061-handoff.md` at S-Auto-7.1 close. Leave **§12** empty (deliver-agent + human at milestone close). Required sections:

- **§1 Class + §7 stanza self-walk** — confirm class `infra` + §7 EXEMPT classification against delivered scope.
- **§2 Goal achievement** — bullet each of the 5 Goal items + evidence pointer.
- **§3 Scope execution log** — for each scope step #1-#5, brief status (DONE / PARTIAL / SKIPPED-WITH-REASON).
- **§4 Pre-flight check evidence** — `git show --numstat` for the new module + cli integration; each `PreflightResult` check on a clean env + on a conflict-foreground-backend env; the auto-reboot path executed at least once with screenshot/log capture.
- **§5 Smoke iter end-to-end record** — iteration outcome (keep/discard/error); per-iter elapsed time; `iteration_record.json` `tier_evaluator_verdict` Layer 0-4 outcomes (non-degenerate verification); cumulative artefact tree under `autoloop/results/runs/exp-<N>/`. **This retires Goal #4 from S-Auto-7.**
- **§6 OQ-S60.7 root-cause disposition** — pre-flight stop-foreground-backend approach validated; deeper root-cause (Flyway lock vs maven race vs Redis pool) characterized at the depth needed; deeper substrate fixes (in-memory DB / Flyway timeout / Docker sandbox) DEFERRED to M-Auto-2+ as observation.
- **§7 OQ-S60.10 ergonomics fix status** — DONE bundled in commit 1 OR DEFERRED with new R-item annotation.
- **§8 Adversarial spot-check pre-close** — independent verification of (i) fence #13 reasserted (scoring SHA `22548e20…` UNCHANGED); (ii) loader.py from S-Auto-7 in-session override UNCHANGED (no further eval_interactive/ edits); (iii) all M-Auto-1B §6 hard-fenced surfaces UNCHANGED via `git diff --stat <s-auto-7-close-sha>..HEAD`.
- **§9 Code anchor table** — `git show --numstat` table format showing file paths + lines added/removed.
- **§10 Test count** — autoloop pytest baseline 228 → final count + delta breakdown.
- **§11 §7 stanza self-walk verification** — explicit "self-walk passed; §7 EXEMPT confirmed".
- **§12 OQ-S61.x list** — open questions surfaced; disposition per OQ.

## Commit discipline

Dev stages **only S-Auto-7.1 scope** explicitly:

- NEW `autoloop/autoloop/preflight.py` (pre-flight module).
- Modified `autoloop/autoloop/cli.py` (pre-flight wiring + `preflight` subcommand + `--auto-reboot` / `--skip-preflight` flags + optional OQ-S60.10 dotenv auto-load).
- NEW or extended `autoloop/tests/test_preflight.py` (4-8 new tests).
- Optional: `autoloop/pyproject.toml` (add `python-dotenv` only if OQ-S60.10 fix lands AND dotenv is not already declared).
- NEW `docs/sprints/sprint-061-handoff.md`.

**No `git add -A`**. **No bundle of deliver-agent close artefacts** (`docs/sprint_objective.md` / `docs/milestone_objective.md` / `docs/codex-findings.md` / `docs/10-handoff.md` / `docs/action_bank.md` / `compact/sprint-061-codex-review-prompt.md` if upgrade triggers) — those bundle by human at close per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

**Single-commit pattern preferred** (S-Auto-7.1 is small-medium scope; ~150-250 LOC across preflight + cli + tests + dotenv):

```
Sprint 061 / S-Auto-7.1 / M-Auto-1B — Pre-flight env check + smoke iter Goal #4 retire

[2-3 sentences describing the preflight.py + cli wiring + auto-reboot path + smoke iter outcome (keep/discard/error) + per-iter elapsed time]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Two-commit pattern acceptable IF OQ-S60.10 dotenv fix is split out:

```
Commit 1: Sprint 061 / S-Auto-7.1 / M-Auto-1B — Pre-flight env check + cli wiring + tests
Commit 2: Sprint 061 / S-Auto-7.1 / M-Auto-1B — OQ-S60.10 dotenv auto-load + smoke iter completion + handoff
```

## Scope size (§8.5 note)

M-Auto-1B with S-Auto-7.1 = 5 of 5 sub-sprints planned (HITS §8.5 ceiling EXACTLY; margin = 0 for any further fix-iteration). S-Auto-7.1 is small-medium by LOC + test count (~4-8 new tests + ~150-250 LOC in preflight.py + cli.py); estimated 1-2 dev-days + Codex deferred to milestone close.

**If S-Auto-7.1 surfaces a second-order substrate brittleness** that requires a S-Auto-7.2 (or any further fix-iteration), M-Auto-1B EXCEEDS the §8.5 ceiling and must split per §8.5 advice — the deliver-agent + human jointly decide whether to (a) extend M-Auto-1B with documented scope-overflow + acceptance bar revision, (b) close M-Auto-1B in its current state and roll Spring-spawn root-cause to M-Auto-2, or (c) accept incomplete Goal #4 with explicit "first overnight + first cherry-pick happens without loop-context substrate validation" risk.

## OQ (open questions — filled during the sub-sprint)

- **OQ-S61.x candidates** (expected; dev surfaces as ambiguities encountered):
  - **OQ-S61.1** — Pre-flight check sequencing: should checks run in parallel (faster) OR sequential (deterministic ordering)? Default sequential per defensive design; verify timing under typical dev env.
  - **OQ-S61.2** — `--auto-reboot` semantics: SIGTERM vs SIGKILL on the foreground Spring backend? SIGTERM is graceful; SIGKILL is forceful. Default SIGTERM with 5s timeout then SIGKILL.
  - **OQ-S61.3** — `lsof -i :8080` portability: macOS vs Linux behavior; needs `lsof` installed. Document the dependency or use stdlib `socket.create_connection` for port-busy detection (less informative — no pid).
  - **OQ-S61.4** — Pre-flight failure exit code: 1 (generic error) vs distinct codes per check? Default 1 for v1 simplicity.
  - **OQ-S61.5** — OQ-S60.10 dotenv fix scope: 1-line auto-load at cli.py top vs explicit `load_dotenv` invocation in `run` subcommand handler. Trade-off: module-load auto-load is more transparent but less explicit.
  - **OQ-S61.6** — Pre-flight integration test: does the test invoke a real subprocess for `python -m autoloop preflight` OR call `run_preflight(config)` directly? Direct call is faster but tests less of the CLI plumbing.
  - **OQ-S61.7** — Spring spawn root-cause depth: pre-flight stop-foreground proves sufficient for M-Auto-1B; document deeper root-cause (Flyway lock vs maven race) at investigation depth only, defer to M-Auto-2+.

Add OQ entries as ambiguities are encountered; record disposition per OQ in handoff §12.
