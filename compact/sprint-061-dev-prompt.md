# Dev prompt — Sprint 061 / M-Auto-1B S-Auto-7.1 — Pre-flight env check + smoke iter completion (fix-iteration for Goal #4 BLOCKED)

You are the **Dev Agent for Sprint 061 / M-Auto-1B S-Auto-7.1**. This is a **fix-iteration sub-sprint** dispatched by the deliver-agent + human after S-Auto-7 closed `B — Surfaced findings need fix-iteration` sub-classified `approve with downgrade-to-signal follow-up` on 2026-05-29. The S-Auto-7 substrate-fix scope (Blocker A + Blocker B + Blocker C + scoring SHA rebaseline) was ACCEPTED by per-sub-sprint Codex (`decision: pass / blocking_count: 0`). The remaining gap is Goal #4 (smoke iter through Step 9) which was structurally BLOCKED at S-Auto-7 by the OQ-S60.7 alt-port Spring spawn issue.

Your job: implement a **pre-flight environment check** at the outer-loop entry point (the human's design principle — saved as `feedback_preflight_env_check_outer_loop` memory) that detects + recovers from foreground :8080 backend contention, then drive the smoke iter to Step 9 with non-degenerate `tier_evaluator_verdict`.

This sub-sprint is class `infra` / §7 EXEMPT / Codex default milestone-shared at M-Auto-1B close (no §4.3 trigger expected for the planned scope).

---

## Read order (minimal)

Read only:

1. **`AGENTS.md`** (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. **This prompt** (self-contained executable view per `iteration_governance.md` §9 invariant).

You may sample (NOT embed) the following code paths for implementation:

- **`autoloop/autoloop/cli.py`** — the existing CLI entry point (`python -m autoloop run`, `python -m autoloop check`, `python -m autoloop apply`). You'll extend this with a new `preflight` subcommand + `--auto-reboot` / `--skip-preflight` flags on the `run` subcommand.
- **`autoloop/autoloop/scoring/baseline_loader.py`** — sample (DO NOT edit; fence #13 locked at hash `22548e20…`). The pre-flight's `check_baseline_dir_loads` calls `baseline_loader.load(config.fitness.baseline_dir)`.
- **`autoloop/config.yaml`** — sample (DO NOT edit; the pre-flight reads from it). Specifically `fitness.baseline_dir` (blessed at `eval_interactive/results/m-auto-1b-baseline-20260529/` from S-Auto-7), the port configuration (likely `bot.base_url` or similar), `fitness.scoring_code_baseline_sha = 22548e20…`.
- **`autoloop/autoloop/meta_agent/llm_client.py`** — read-only sample to understand the `_load_env_local` path mismatch (OQ-S60.10); the dotenv fix in #4 (if you bundle it) lives in `cli.py` as a higher-layer convenience, NOT in `llm_client.py` itself (the meta-agent module is sub-sprint-fenced).
- **`autoloop/tests/test_eval_runner.py`** — sample for test pattern conventions (mocked subprocess + path fixtures); your `autoloop/tests/test_preflight.py` follows the same pattern.
- **`autoloop/.env.local`** — sample (DO NOT commit; gitignored). Verify the dotenv path resolution if you bundle OQ-S60.10.

Do **not** re-read the S-Auto-7 archived contract from `docs/sprints/sprint-060-objective.md` or the S-Auto-7 handoff from `docs/sprints/sprint-060-handoff.md` — both are embedded summary-wise below where load-bearing for S-Auto-7.1 execution.

---

## Class

`infra` (§3.2 Q1 — substrate ergonomics + environment pre-flight; structural plumbing repair on the auto-loop entry point; `cli.py` integration + NEW `preflight.py`; the change is HOW the loop starts, NOT a semantic decision branch). **§7 EXEMPT** per pure-infra carve-out + self-walked for paper-trail completeness.

---

## Goal

S-Auto-7.1 close 时:

1. **Pre-flight env check lands at the outer-loop entry point.** A new pre-flight check runs FIRST when `python -m autoloop run` is invoked (or as a standalone `python -m autoloop preflight` command). The check enumerates: (a) is a Spring backend bound on the project's configured port (likely `:8080`)?; (b) is PostgreSQL reachable on the configured port?; (c) is Redis reachable?; (d) is `AUTOLOOP_META_LLM_API_KEY` set and non-empty?; (e) is `git status` clean on the current branch?; (f) does the configured `baseline_dir` exist and load cleanly via `baseline_loader.load`? Each check returns a structured `PreflightResult` with `status: ok | warn | fail` + diagnostic message; the loop refuses to start if any check returns `fail` (unless `--skip-preflight` escape hatch).

2. **Reboot-as-recovery for failing services in dev (the human-locked principle).** When a pre-flight check returns `fail` and the failure mode is "service unreachable" or "port conflict" or "stale process holds resource", the pre-flight check either auto-recovers (e.g., kills the offending foreground :8080 backend if `--auto-reboot` is passed) OR surfaces an actionable message ("Run `lsof -i :8080` and `kill <pid>` first, then re-run") to the operator. Production reboot semantics are NOT in scope; the pre-flight ergonomics tool is dev-only.

3. **Goal #4 retired — smoke iter completes through Step 9 with non-degenerate `tier_evaluator_verdict`.** With pre-flight running first (and the foreground :8080 backend stopped per its recommendation), `python -m autoloop run --experiments 1` (NO `--dry-run`) drives the full 14-step state machine end-to-end on `auto-loop-branch`. Step 6 (applier alt-port Spring spawn) must succeed; Step 7 (`eval_runner.run_v1_fitness_suite`) must produce non-empty `SuiteRunResult` with the new symlink staging from S-Auto-7 Blocker B fix; Step 9 (`tier_evaluator.evaluate`) must produce non-degenerate Layer 0-4 outcomes (each Layer reports PASS / FAIL / informational metric rather than the empty-snapshot short-circuit from S-Auto-5; NOT the unreachable-step pattern from S-Auto-7 exp-6).

4. **OQ-S60.7 dispositioned in the handoff.** The Spring spawn root-cause (Flyway lock contention / maven multi-module target/ race / Redis pool exhaustion / classpath contention) is investigated to the depth needed to confirm the pre-flight stop-foreground-backend approach is sufficient. Deeper root-cause fixes (in-memory DB / Flyway timeout config / Docker sandbox per-iter) are DEFERRED to M-Auto-2+ as observation-only — the dev-environment ergonomic principle accepts the workaround as acceptable for M-Auto-1B.

5. **Codex review prep**: deliver-agent does NOT dispatch per-sub-sprint Codex for S-Auto-7.1 in default scope (deferred to milestone-shared at M-Auto-1B close). UPGRADE TO PER-SUB-SPRINT CODEX (§4.3 trigger #3) only if you encounter a substrate brittleness mid-sub-sprint that requires touching a new hard-fenced surface — STOP and surface to deliver-agent + human first.

**Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at S-Auto-7 close at hash `22548e20…`). **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py`. **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` (pre-flight is invoked from `cli.py` BEFORE `loop.run_loop()` so the integration is at the entry point, not inside the loop state machine). **Zero touch** to `eval_interactive/eval_interactive/**`. **Zero touch** to `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives (other than S-Auto-7.1's own), milestone archives, `docs/codex-findings.md` scaffold.

---

## Scope (numbered; this is the contract)

### #1 — Implement pre-flight env check module (`autoloop/autoloop/preflight.py` NEW)

Create a new `autoloop/autoloop/preflight.py` module containing:

- A `PreflightResult` dataclass with fields `name: str`, `status: Literal["ok", "warn", "fail"]`, `message: str`, `remediation: Optional[str]`.
- A `run_preflight(config: dict) -> list[PreflightResult]` function that runs all checks in sequence:
  - `check_foreground_backend(config)` — `lsof -i :8080` (or whatever port the config declares); if a Spring process is listening, return `fail` with remediation "Stop the foreground backend with `kill <pid>` or re-run with `--auto-reboot`". Use stdlib `subprocess.run` with timeout. The `lsof` command output parsing should extract the PID (column 2 in default `lsof` output).
  - `check_postgres_reachable(config)` — try opening a TCP connection to the configured PostgreSQL host:port (likely `localhost:5432`); return `fail` if unreachable with remediation "Run `brew services start postgresql@<version>` (or your equivalent)". Use stdlib `socket.create_connection` with 2s timeout.
  - `check_redis_reachable(config)` — same pattern for Redis (likely `localhost:6379`); return `fail` if unreachable with remediation.
  - `check_meta_llm_api_key()` — verify `os.environ.get("AUTOLOOP_META_LLM_API_KEY")` is set in env AND non-empty; return `fail` with remediation "Run `set -a; source autoloop/.env.local; set +a`" (this is the OQ-S60.10 ergonomics surface — see #4 below).
  - `check_clean_working_tree(config)` — run `git status --porcelain`; if non-empty, return `warn` (NOT `fail`; dev may have intentional working-tree state) with remediation "Commit or stash before running the loop".
  - `check_baseline_dir_loads(config)` — invoke `from autoloop.scoring import baseline_loader; baseline_loader.load(Path(config['fitness']['baseline_dir']), config=config)`; if it raises OR returns warnings, return `fail` with remediation.
- An optional `auto_reboot_foreground_backend(pid: int)` helper that the loop entry point calls IFF `--auto-reboot` was passed AND `check_foreground_backend` returned `fail` with the conflict pid.

Stdlib only (no new heavy deps): `subprocess` + `socket` + `pathlib` + `dataclasses` + `typing` + `os` + `signal`. The existing `python-dotenv` (if present in pyproject.toml) MAY be used for the OQ-S60.10 fix in #4 below.

Read `autoloop/autoloop/cli.py` FIRST to understand the existing entry-point structure. The pre-flight should hook into the `run` subcommand AND be invocable as a standalone subcommand `python -m autoloop preflight`.

### #2 — Wire pre-flight into the CLI entry point (`autoloop/autoloop/cli.py` EXTEND)

In `autoloop/autoloop/cli.py`:

1. Import `preflight.run_preflight` + `auto_reboot_foreground_backend`.
2. Add a `preflight` subcommand: `python -m autoloop preflight` runs the checks + prints a structured report to stdout. Exit code 0 if all ok or warn; 1 if any fail.
3. In the `run` subcommand: BEFORE `loop.run_loop()` is invoked, call `run_preflight(config)` and print results. Decision logic:
   - **Without `--skip-preflight`**: if any result has `status: fail`: refuse to start; print remediation list; exit with non-zero status.
   - **With `--auto-reboot` (new flag)**: for the specific `check_foreground_backend` fail case ONLY (other fail cases like missing API key cannot be auto-rebooted), call `auto_reboot_foreground_backend(pid)` to kill the conflict; re-run pre-flight; if still failing, refuse to start. Use SIGTERM with 5s timeout then SIGKILL escalation per OQ-S61.2 default.
   - **With `--skip-preflight` (escape hatch; print warning that this is dev-only)**: proceed regardless. Document in `--help`.

Print the pre-flight report to the same stdout stream as the loop output (NOT to a separate log file) so the dev sees env state alongside iteration progress.

### #3 — Smoke iter via pre-flight path

With #1 + #2 landed, run the smoke iter:

```bash
# Verify the env first (optional — for human eyeball before the live run)
cd autoloop && uv run --extra dev python -m autoloop preflight

# Option A: manually stop the foreground backend
lsof -i :8080  # identify the conflicting PID (likely 8613 per S-Auto-7 handoff §7)
kill <pid>     # stop the foreground Spring

# Option B: rely on --auto-reboot (test this path at least once for handoff §4 evidence)
cd autoloop && uv run --extra dev python -m autoloop run --experiments 1 --auto-reboot

# Either way, the smoke iter must proceed through Step 9
```

The full 14-step state machine must execute end-to-end. Critically: Steps 6 (alt-port Spring spawn), 7 (eval_runner), AND 9 (tier_evaluator) must reach non-degenerate outputs.

Record (for handoff §5):

- Iteration outcome (keep / discard / error; any of three OK per scope acceptance bar).
- Per-iter wall-clock elapsed time (FIRST measurement of full Spring-spawn + 47-case-eval cycle on the now-blessed baseline; expected 12-25 min per M-Auto-1B contract; 40 min is the cap).
- `autoloop/results/runs/exp-<N>/iteration_record.json` contents — verify `tier_evaluator_verdict` is non-empty + Layer 0-4 outcomes are non-degenerate.
- `autoloop/results/runs/exp-<N>/` artefacts: `hypothesis.json`, `diff.yaml`, `sandbox_verdict.json`, `anti_hardcode_verdict.json`, `tier_evaluator_verdict.json`, eval per-suite outputs via the symlink staging from S-Auto-7 Blocker B fix.

**If smoke iter still crashes** in an unhandled path NOT caused by foreground-backend contention (e.g., a different substrate brittleness surfaces): STOP and surface. Likely fix-iteration S-Auto-7.2 candidate — and at that point M-Auto-1B exceeds the §8.5 5-sub-sprint ceiling and the deliver-agent + human must consider splitting M-Auto-1B per §8.5 advice.

**If per-iter elapsed >40 min**: continue this iteration to completion but record the observation as M-Auto-2 optimization R-item.

### #4 — OQ-S60.10 ergonomics fix (optional within S-Auto-7.1 scope; bundle if trivial)

Per S-Auto-7 handoff §12 OQ-S60.10: `python -m autoloop run` does NOT auto-load `autoloop/.env.local`; the caller must `set -a; source .env.local; set +a` first.

If the fix is trivial (~5 LOC at the top of `autoloop/autoloop/cli.py` to invoke `dotenv.load_dotenv(Path(__file__).parent.parent.parent / ".env.local")` at the start of the `run` subcommand handler):

1. Verify `python-dotenv` is already declared in `autoloop/pyproject.toml`. If not, `uv add python-dotenv`.
2. At the top of the `run` subcommand handler (BEFORE `run_preflight` is called), invoke `dotenv.load_dotenv()` with the explicit `autoloop/.env.local` path. This ensures `AUTOLOOP_META_LLM_API_KEY` is in `os.environ` so `check_meta_llm_api_key()` passes without requiring the `set -a; source ...` shell incantation.
3. The pre-flight check `check_meta_llm_api_key()` STILL runs — it now reads from the dotenv-loaded `os.environ`. If the file is missing or the key is absent, pre-flight still fails appropriately.

If the fix is NON-trivial (e.g., requires conditional config-path resolution or interacts with the `meta_agent/llm_client.py:16` loader path mismatch in a deeper way): DEFER as new R-item `R-autoloop-env-local-auto-load` for M-Auto-2 ergonomics. Note the deferral in handoff §7.

DO NOT edit `autoloop/autoloop/meta_agent/llm_client.py` — the auto-load lives in `cli.py` as a higher-layer convenience.

### #5 — Handoff + Codex review prep

Author `docs/sprints/sprint-061-handoff.md` per the "Handoff requirements" section below. Codex review at M-Auto-1B close is default milestone-shared; deliver-agent does NOT dispatch per-sub-sprint Codex for S-Auto-7.1 UNLESS you encounter a §4.3 trigger condition mid-sub-sprint (e.g., need to touch a fence #2 surface beyond loader.py's already-blessed override).

---

## Hard fences / STOP conditions

- **Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py`. Fence #13 reasserted at S-Auto-7 close at hash `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`. Any change triggers `gaming.scoring_code_drift.sha_changed` ERROR.
- **Zero touch** to `eval_interactive/eval_interactive/**` (the loader.py controlled override from S-Auto-7 stays in place; no further edits).
- **Zero touch** to `autoloop/autoloop/sandbox/`, `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/`, `autoloop/autoloop/memory/`.
- **Zero touch** to `eval_interactive/case_specs/`, `eval_interactive/case_specs_shadow/`.
- **Zero touch** to `server/src/main/java/**`, `eval/src/main/java/**`, `data/`, `db/`, `server/src/main/resources/` (cherry-pick to skills/ is S-Auto-8 scope ONLY).
- **Zero touch** to `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives (other than S-Auto-7.1's own), milestone archives, `docs/codex-findings.md` scaffold.
- **No new heavy deps** in `autoloop/pyproject.toml`. Stdlib `subprocess` + `socket` + `pathlib` + `dataclasses` + `os` + `signal` + `typing` only. `python-dotenv` allowed only if #4 ergonomics fix lands and dotenv is not already declared.
- **No LLM call** in `preflight.py` or its tests (pre-flight is deterministic substrate-level discovery).
- **No `git add -A`** — stage S-Auto-7.1 scope explicitly.
- **No cherry-pick to main** in S-Auto-7.1 (M-Auto-1B fence #7 / #17 allows EXACTLY ONCE in S-Auto-8 ONLY).
- **No production reboot semantics**. The pre-flight + auto-reboot ergonomics tool is dev-only.
- **STOP and surface** conditions:
  - Pre-flight implementation requires touching a hard-fenced surface BEYOND `autoloop/cli.py` + NEW `autoloop/preflight.py` (e.g., needs to touch `loop.py` or `meta_agent/`).
  - Smoke iter still crashes after pre-flight stops foreground backend (different brittleness surfaces; likely fix-iteration S-Auto-7.2 candidate; M-Auto-1B exceeds §8.5 ceiling).
  - OQ-S60.10 fix turns out to be non-trivial (>10 LOC or requires new config-path resolution). Defer to new R-item.
  - Pre-flight surfaces an unexpected substrate brittleness (e.g., baseline_dir symlinks broken; case_specs path missing). Surface as observation; do NOT auto-fix mid-sub-sprint.

---

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run --extra dev pytest -q` — S-Auto-7 close baseline `228 passed, 1 warning` MUST grow by ~4-8 new S-Auto-7.1 tests:
  - Each `PreflightResult` check returning ok/warn/fail with mocked subprocess + mocked socket.
  - `auto_reboot_foreground_backend(pid)` issues correct signal sequence (SIGTERM with timeout escalation to SIGKILL).
  - CLI integration: `python -m autoloop preflight` standalone runs all checks (via Click test runner OR direct invocation).
  - CLI integration: `python -m autoloop run` refuses to start without `--skip-preflight` if pre-flight fails.
  - CLI integration: `--auto-reboot` invokes auto-reboot helper on `check_foreground_backend` fail.
  - Total expected `232-236 passed, 1 warning`. The 1 warning is the S-Auto-2 baseline-missing-shadow asserted behaviour UNCHANGED.
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed` (OQ-S47.3 env-specific failures).
- **Java baseline UNCHANGED**: skipped per Java-zero-touch (verify `git diff --stat b0a3704..HEAD -- server/src/main/java/ eval/src/main/java/` returns empty).
- **17-fixture detector calibration sweep**: `cd autoloop && uv run --extra dev pytest -q autoloop/tests/test_anti_hardcode_check.py` UNCHANGED at 31 passed (S-Auto-7.1 does NOT touch the detector).
- **scoring_code_baseline_sha UNCHANGED**: `22548e20…` reasserted; `_check_scoring_code_drift(config) == []` silent (S-Auto-7.1 does NOT touch the four scoring files).
- **Live smoke iter completes**: 1 iter completed end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`. Per-iter elapsed recorded.
- **No live LLM call in pytest tests** — preflight tests use mocked-subprocess + mocked-socket only.

---

## §7 — Layer-classification + anti-hardcode stanza (EXEMPT; self-walked for paper-trail)

**§7 stanza is NOT REQUIRED for pure-infra substrate-fix sub-sprints per `iteration_governance.md` §7**. Self-walked here for paper-trail completeness only:

**Target failure layer:** `infra` (§3.2 Q1 — substrate ergonomics + environment pre-flight; structural plumbing repair on the auto-loop entry point; `cli.py` integration + NEW `preflight.py`; the change is HOW the loop starts, NOT a semantic decision branch).

**Tier-0 invariant:** adds no Tier-0 invariant. Pre-flight is environment discovery + reboot ergonomics, NOT a Tier-0 runtime invariant.

**Semantic hardcode:** No semantic hardcode introduced.

- Pre-flight checks (foreground-backend / PostgreSQL / Redis / API key / clean working tree / baseline_dir loads) are **environment discovery** — they read system state, not encode semantic decisions about user goals / use case hypothesis / escalation posture.
- Reboot semantics (auto-kill foreground :8080 backend on `--auto-reboot`) are **substrate plumbing** — no regex on case content, no keyword list on user utterances, no if-else decision branch on a semantic surface.
- The OQ-S60.10 `dotenv.load_dotenv` ergonomics fix is a config-path resolution, NOT a runtime semantic decision.
- Smoke iter execution exercises the existing semantic logic (S-Auto-5 calibrated detector + meta-agent propose + sandbox + content_validator + tier_evaluator) WITHOUT modifying any of it.

**Generalization coverage:**

- **target** = pre-flight check correctly identifies foreground-backend port conflict on :8080; `--auto-reboot` kills the conflict; smoke iter reaches Step 9 with non-degenerate `tier_evaluator_verdict`.
- **neighbor** = pre-flight checks for other services (PostgreSQL / Redis / API key / working tree / baseline_dir) each report correctly on mocked-fail cases.
- **negative-control** = pre-flight returns all-ok on a clean env; `python -m autoloop preflight` standalone returns 0; `python -m autoloop run` proceeds without warning; smoke iter completes.
- **shadow** = shadow firewall posture UNCHANGED. Pre-flight does NOT read shadow case content; only loads aggregate baseline_dir for the baseline_loader.load check.

---

## Codex review plan (§4.3)

**DEFAULT — MILESTONE-SHARED at M-Auto-1B close.** The S-Auto-7.1 sub-sprint does NOT cross any §4.3 per-sub-sprint trigger in the planned scope.

**UPGRADE TO PER-SUB-SPRINT CODEX (§4.3 trigger #3)** ONLY IF you encounter a substrate brittleness during implementation that requires touching a new fence surface (e.g., `loop.py` integration is unavoidable). STOP and surface to deliver-agent + human BEFORE proceeding.

---

## Handoff requirements

Author `docs/sprints/sprint-061-handoff.md` at S-Auto-7.1 close. Leave **§12** empty (deliver-agent + human at milestone close). Required sections:

- **§1 Class + §7 stanza self-walk** — confirm class `infra` + §7 EXEMPT classification against delivered scope.
- **§2 Goal achievement** — bullet each of the 5 Goal items + evidence pointer.
- **§3 Scope execution log** — for each scope step #1-#5, brief status (DONE / PARTIAL / SKIPPED-WITH-REASON).
- **§4 Pre-flight check evidence** — `git show --numstat` for the new module + cli integration; each `PreflightResult` check on a clean env + on a conflict-foreground-backend env; the auto-reboot path executed at least once with stdout capture (paste the actual transcript).
- **§5 Smoke iter end-to-end record** — iteration outcome (keep/discard/error); per-iter elapsed time; `iteration_record.json` `tier_evaluator_verdict` Layer 0-4 outcomes (non-degenerate verification); cumulative artefact tree under `autoloop/results/runs/exp-<N>/`. **This retires Goal #4 from S-Auto-7.**
- **§6 OQ-S60.7 root-cause disposition** — pre-flight stop-foreground-backend approach validated; deeper root-cause (Flyway lock vs maven race vs Redis pool) characterized at the depth needed to confirm the workaround sufficiency; deeper substrate fixes DEFERRED to M-Auto-2+ as observation.
- **§7 OQ-S60.10 ergonomics fix status** — DONE bundled in commit 1 OR DEFERRED with new R-item annotation.
- **§8 Adversarial spot-check pre-close** — independent verification of (i) fence #13 reasserted (scoring SHA `22548e20…` UNCHANGED); (ii) loader.py from S-Auto-7 in-session override UNCHANGED (no further eval_interactive/ edits); (iii) all M-Auto-1B §6 hard-fenced surfaces UNCHANGED via `git diff --stat b0a3704..HEAD`.
- **§9 Code anchor table** — `git show --numstat` table format showing file paths + lines added/removed.
- **§10 Test count** — autoloop pytest baseline 228 → final count + delta breakdown.
- **§11 §7 stanza self-walk verification** — explicit "self-walk passed; §7 EXEMPT confirmed".
- **§12 OQ-S61.x list** — open questions surfaced; disposition per OQ.

---

## Commit discipline

Stage **only S-Auto-7.1 scope** explicitly:

- NEW `autoloop/autoloop/preflight.py` (pre-flight module).
- Modified `autoloop/autoloop/cli.py` (pre-flight wiring + `preflight` subcommand + `--auto-reboot` / `--skip-preflight` flags + optional OQ-S60.10 dotenv auto-load).
- NEW or extended `autoloop/tests/test_preflight.py` (4-8 new tests).
- Optional: `autoloop/pyproject.toml` (add `python-dotenv` only if OQ-S60.10 fix lands AND dotenv is not already declared).
- NEW `docs/sprints/sprint-061-handoff.md`.

**No `git add -A`**. **No bundle of deliver-agent close artefacts** — those bundle by human at close per `feedback_commit_at_end_bundles_deliver_artefacts`.

**Single-commit pattern preferred** (S-Auto-7.1 is small-medium scope; ~150-250 LOC across preflight + cli + tests):

```
Sprint 061 / S-Auto-7.1 / M-Auto-1B — Pre-flight env check + smoke iter Goal #4 retire

[2-3 sentences describing the preflight.py + cli wiring + auto-reboot path + smoke iter outcome (keep/discard/error) + per-iter elapsed time]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

**Two-commit pattern acceptable IF OQ-S60.10 dotenv fix is split out**:

```
Commit 1: Sprint 061 / S-Auto-7.1 / M-Auto-1B — Pre-flight env check + cli wiring + tests
Commit 2: Sprint 061 / S-Auto-7.1 / M-Auto-1B — OQ-S60.10 dotenv auto-load + smoke iter completion + handoff
```

---

## Self-check checklist (BEFORE you claim sub-sprint complete)

- [ ] `autoloop/autoloop/preflight.py` exists with `PreflightResult` dataclass + `run_preflight(config)` returning a list with all 6 checks.
- [ ] `autoloop/autoloop/cli.py` has new `preflight` subcommand AND the `run` subcommand calls `run_preflight` before `loop.run_loop`.
- [ ] `--auto-reboot` flag works on the `check_foreground_backend` fail path; tested at least once with the live foreground :8080 backend running.
- [ ] `--skip-preflight` flag exists as the dev escape hatch with clear `--help` documentation.
- [ ] autoloop pytest `232-236 passed, 1 warning` (228 + 4-8 new).
- [ ] eval_interactive pytest `486 passed, 3 failed` UNCHANGED.
- [ ] `scoring_code_drift` silent — `_check_scoring_code_drift(config) == []`.
- [ ] Smoke iter completed end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`; per-iter elapsed recorded.
- [ ] `git diff --stat b0a3704..HEAD -- <all M-Auto-1B §6 fenced surfaces>` returns empty.
- [ ] `docs/sprints/sprint-061-handoff.md` authored with all 12 sections (§12 left empty).
- [ ] OQ-S60.7 dispositioned; OQ-S60.10 either bundled or deferred with R-item annotation.
- [ ] NO `git add -A`; staged file list matches the Commit discipline section.

---

## Pre-mitigation already in place (deliver-agent side; reduces dev friction)

- S-Auto-7 substrate (`eval_runner.py` Blocker B fix + scoring SHA `22548e20…` + blessed `baseline_dir` + loader.py recursive + skip-underscore) is committed at `b0a3704` and Codex-accepted (`decision: pass / blocking_count: 0`).
- M-Auto-1B §6 fence #2 amended in the S-Auto-7 close-bundle with the loader.py controlled-override annotation (Codex governance trigger satisfied).
- The Spring spawn root-cause (Flyway lock contention with foreground :8080) is well-characterized in S-Auto-7 handoff §7 + §12 OQ-S60.7; the pre-flight stop-foreground-backend approach is the recommended path per the deliver-agent + human joint decision.
- `feedback_preflight_env_check_outer_loop` memory captures the human's design principle informing the pre-flight pattern.

---

**END OF DEV PROMPT.** Begin with Scope #1 (implement `preflight.py`). Walk each scope step in order. Self-check before claiming complete.
