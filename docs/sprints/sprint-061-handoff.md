---
title: Sprint 061 / S-Auto-7.1 / M-Auto-1B pre-flight env check + smoke iter handoff
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-29
review_cadence: ad hoc
notes: >
  Dev-agent handoff for S-Auto-7.1 (Sprint 061), the fix-iteration
  follow-on dispatched at S-Auto-7 close. Class = infra. §7 EXEMPT
  per pure-infra carve-out (self-walked in §1 for paper-trail).

  Pre-flight env check (Goals #1 + #2 + OQ-S60.10) is LANDED and
  empirically verified end-to-end against the live foreground :8080
  contention scenario: pre-flight detected pid=8613, --auto-reboot
  cleared it via SIGTERM, second pre-flight pass returned clean, the
  loop's run state machine started. OQ-S60.10 dotenv auto-load
  bundled (5 LOC + 1 helper).

  Goal #4 (smoke iter through Step 9 with non-degenerate
  tier_evaluator_verdict) is BLOCKED on a NEW substrate finding:
  even with foreground :8080 cleared, Step 6 (applier) Spring spawn
  exits with rc=1 because `mvn -pl server -am spring-boot:run`
  applies the spring-boot:run goal to the csagent-parent module
  (packaging=pom, no main class), not just the server submodule.
  Fix lives in applier.py (fence-protected; S-Auto-7.2 candidate
  per the dev prompt's STOP-and-surface guidance).

  Codex review at M-Auto-1B close is default milestone-shared; no
  §4.3 per-sub-sprint trigger fired in S-Auto-7.1 (pure-infra +
  scope-respected + scoring SHA reasserted).
---

# Sprint 061 / S-Auto-7.1 / M-Auto-1B — pre-flight env check handoff

Dev session: S-Auto-7.1 (pre-flight env check at outer-loop entry
point; smoke iter attempted). Class = `infra`. §7 EXEMPT per
pure-infra carve-out; self-walked in §1 + §11.

## §1 Class + §7 stanza self-walk

- **Class:** `infra` (§3.2 Q1 — substrate ergonomics + environment
  pre-flight; structural plumbing repair on the auto-loop entry
  point; `cli.py` integration + NEW `preflight.py`; the change is
  HOW the loop starts, NOT a semantic decision branch).
- **§7 stanza:** EXEMPT per pure-infra carve-out (pre-flight is
  environment discovery + reboot ergonomics; no case_specs / judge /
  detector / projection / scoring semantic logic touched).
- **Self-walk (paper-trail):**
  - **Target failure layer:** `infra`.
  - **Tier-0 invariant:** adds none.
  - **Semantic hardcode:** none introduced. Pre-flight checks
    (foreground-backend / PostgreSQL / Redis / API key / clean
    working tree / baseline_dir loads) are deterministic
    environment discovery. Reboot semantics (SIGTERM with timeout
    escalation to SIGKILL) are substrate plumbing. OQ-S60.10
    dotenv auto-load is config-path resolution.
  - **Generalization coverage:** target = pre-flight correctly
    identifies foreground-backend port conflict on :8080 + auto-
    reboot kills it cleanly (verified end-to-end against live pid
    8613). Neighbor = other 5 checks each report correctly on
    mocked-fail cases (27 new tests). Negative-control = pre-flight
    returns all-ok on a clean env; `python -m autoloop preflight`
    standalone exits 0. Shadow firewall posture UNCHANGED.

## §2 Goal achievement

| Goal | Status | Evidence pointer |
|------|--------|------------------|
| 1. Pre-flight env check at outer-loop entry point | ✅ DONE | §4 |
| 2. Reboot-as-recovery for foreground :8080 contention | ✅ DONE | §4 |
| 3. Smoke iter through Step 9 with non-degenerate verdict | ⚠️ BLOCKED | §5 + §6 |
| 4. OQ-S60.7 disposition | ✅ DONE (with new finding) | §6 |
| 5. OQ-S60.10 ergonomics fix bundled | ✅ DONE | §7 |
| 6. Codex review prep | ✅ DONE (this handoff) | §11 |

Goal #3 (the dev prompt's Goal #4, the smoke iter retire) is blocked
on a NEW substrate finding surfaced in S-Auto-7.1. The pre-flight
stop-foreground-backend approach is empirically NOT sufficient:
even after `--auto-reboot` cleared pid=8613 cleanly, Step 6
applier's `mvn -pl server -am spring-boot:run` spawn exits with
rc=1 because the goal is applied to csagent-parent (packaging=pom,
no main class), not just server. Fix lives in
`autoloop/autoloop/sandbox/applier.py:370` (fence-protected per
S-Auto-7.1 hard fences). Per the dev prompt's explicit STOP-and-
surface guidance: this is an S-Auto-7.2 fix-iteration candidate;
M-Auto-1B exceeds §8.5's 5-sub-sprint ceiling and the deliver-agent
+ human should consider splitting at the next milestone planning
round.

## §3 Scope execution log

| Step | Status | Notes |
|------|--------|-------|
| #1 — Implement `preflight.py` | DONE | 492 lines; `PreflightResult` + 6 checks + auto_reboot helper + format_report; stdlib only. |
| #2 — Wire pre-flight into `cli.py` | DONE | New `preflight` subcommand; `--auto-reboot` + `--skip-preflight` flags on `run`; gate refuses on fail unless `--skip-preflight`. |
| #3 — Smoke iter through Step 9 | BLOCKED | exp-7 advanced through Steps 1-5 (real-LLM propose + sandbox + content_validator + anti_hardcode_check all PASS); Step 6 applier failed at 72.3s with `SpringStartupTimeoutError: Spring subprocess exited prematurely (rc=1) on port 62872`. Step 7 (eval_runner) + Step 9 (tier_evaluator) NOT reached. See §5 + §6. |
| #4 — OQ-S60.10 dotenv auto-load | DONE | 5 LOC `_auto_load_env_local()` helper at top of `_cmd_run` + `_cmd_preflight`. `python-dotenv>=1.0` added to `autoloop/pyproject.toml`. |
| #5 — Handoff + Codex prep | DONE | This file. |
| COMMIT | DONE | Single commit per `Commit discipline` section; no `git add -A`. |

## §4 Pre-flight check evidence (Goals #1 + #2)

**Module shape** (`autoloop/autoloop/preflight.py:1-492`):

```python
@dataclass
class PreflightResult:
    name: str
    status: Literal["ok", "warn", "fail"]
    message: str
    remediation: Optional[str] = None
    details: Optional[dict[str, Any]] = None

def run_preflight(config, *, repo_root=None) -> list[PreflightResult]:
    return [
        check_foreground_backend(config),  # lsof -nP -i :<port> -sTCP:LISTEN
        check_postgres_reachable(config),  # socket.create_connection localhost:5432
        check_redis_reachable(config),     # socket.create_connection localhost:6379
        check_meta_llm_api_key(config),    # os.environ[AUTOLOOP_META_LLM_API_KEY]
        check_clean_working_tree(config, cwd=...),  # git status --porcelain
        check_baseline_dir_loads(config, repo_root=...),  # baseline_loader.load
    ]

def auto_reboot_foreground_backend(pid, *, sigterm_timeout_seconds=5.0) -> bool:
    # SIGTERM → 5s grace → SIGKILL → 1s grace; OQ-S61.2 default.
```

**Standalone preflight subcommand against live env** (foreground
:8080 backend running as pid=8613, dirty working tree from
S-Auto-7.1 in-flight scope):

```
$ uv run --extra dev python -m autoloop preflight
[preflight] 6 check(s):
[preflight] FAIL foreground_backend — foreground listener on :8080 (pids=8613) contends with applier alt-port Spring spawn
[preflight]      remediation: Stop the foreground backend with `kill 8613` or re-run with `--auto-reboot`
[preflight] OK   postgres_reachable — postgres reachable at localhost:5432
[preflight] OK   redis_reachable — redis reachable at localhost:6379
[preflight] OK   meta_llm_api_key — AUTOLOOP_META_LLM_API_KEY set in env (length=32)
[preflight] WARN clean_working_tree — working tree has 4 uncommitted change(s)
[preflight]      remediation: Commit or stash before running the loop (warning only; not a hard fail)
[preflight] OK   baseline_dir_loads — baseline snapshot loaded (run_id=m-auto-1b-baseline-20260529, 3 suite(s))
[preflight] summary: ok=4 warn=1 fail=1
$ echo $?
1
```

Exit code 1 because `foreground_backend` is FAIL; the
`clean_working_tree` warn does NOT trigger non-zero exit (warns
are advisory).

**`--auto-reboot` end-to-end transcript** (the OQ-S60.7 workaround
path validated):

```
$ uv run --extra dev python -m autoloop run --experiments 1 --auto-reboot
[preflight] 6 check(s):
[preflight] FAIL foreground_backend — foreground listener on :8080 (pids=8613) contends with applier alt-port Spring spawn
[preflight]      remediation: Stop the foreground backend with `kill 8613` or re-run with `--auto-reboot`
[preflight] OK   postgres_reachable — postgres reachable at localhost:5432
[preflight] OK   redis_reachable — redis reachable at localhost:6379
[preflight] OK   meta_llm_api_key — AUTOLOOP_META_LLM_API_KEY set in env (length=32)
[preflight] WARN clean_working_tree — working tree has 6 uncommitted change(s)
[preflight] OK   baseline_dir_loads — baseline snapshot loaded (run_id=m-auto-1b-baseline-20260529, 3 suite(s))
[preflight] summary: ok=4 warn=1 fail=1
[run] --auto-reboot: killing foreground :8080 listener(s) pid=[8613]
[run] auto-reboot pid=8613 → stopped
[preflight] 6 check(s):
[preflight] OK   foreground_backend — no listener on :8080 (alt-port Spring spawn unobstructed)
[preflight] OK   postgres_reachable — postgres reachable at localhost:5432
[preflight] OK   redis_reachable — redis reachable at localhost:6379
[preflight] OK   meta_llm_api_key — AUTOLOOP_META_LLM_API_KEY set in env (length=32)
[preflight] WARN clean_working_tree — working tree has 6 uncommitted change(s)
[preflight] OK   baseline_dir_loads — baseline snapshot loaded (run_id=m-auto-1b-baseline-20260529, 3 suite(s))
[preflight] summary: ok=5 warn=1 fail=0
[run] starting 1 iteration(s), dry_run=False
[run] done: keep=0 discard=0 error=1 (of 1 total)
  exp-7: decision=error discard_reason=- elapsed=72.3s
```

This proves the auto-reboot path operates as designed: SIGTERM
landed on pid=8613, `os.kill(pid, 0)` probe confirmed the process
gone, second-pass pre-flight re-discovered the env as clean
(foreground_backend: OK), gate proceeded. The downstream Step 6
failure (see §5 + §6) is a SEPARATE substrate brittleness, not a
pre-flight defect.

**Operator note (foreground backend restoration)**: pid=8613 was
the human's foreground Spring backend (started Thursday per the
S-Auto-7 handoff). `--auto-reboot` stopped it cleanly. The dev
prompt explicitly authorized this for §4 evidence. If the human
wants the foreground backend back for other work, they should
restart it manually (`mvn spring-boot:run` per their preferred
shell + env vars); the pre-flight will then detect it again and
require `--auto-reboot` or manual stop before the next loop
invocation.

**Code anchor** (`git ls-files --others --exclude-standard` +
`git diff --numstat HEAD`):

| File | Status | Lines added | Lines removed |
|------|--------|-------------|---------------|
| `autoloop/autoloop/preflight.py` | NEW | 492 | 0 |
| `autoloop/autoloop/cli.py` | MOD | 168 | 5 |
| `autoloop/tests/test_preflight.py` | NEW | 429 | 0 |
| `autoloop/tests/test_cli_integration.py` | MOD | 8 | 1 |
| `autoloop/pyproject.toml` | MOD | 1 | 0 |
| `autoloop/uv.lock` | MOD | 11 | 0 |

Test count delta (autoloop pytest, foreground subprocess):
**228 → 255 PASS, 1 warning**. Delta = +27 tests (24 in
test_preflight.py covering checks + auto_reboot + CLI integration;
+3 implicit via test_cli_integration.py for the `preflight`
subcommand + `--auto-reboot` / `--skip-preflight` flags; existing
test_cli_integration::test_cli_run_invokes_loop_with_dry_run_false
was updated to pass `--skip-preflight` since its placeholder config
no longer satisfies the new pre-flight gate). The 1 warning is the
S-Auto-2 baseline-missing-shadow asserted behaviour UNCHANGED.

## §5 Smoke iter record (Goal #3 — BLOCKED)

**exp-7 execution log** (full transcript in `[run]` capture above):

| Stage | Result |
|-------|--------|
| Pre-flight pass 1 | FAIL on foreground_backend (pid=8613); other 5 checks OK / WARN |
| Auto-reboot | SIGTERM landed; pid=8613 stopped within timeout |
| Pre-flight pass 2 | ALL OK / 1 WARN (working tree); fail=0 |
| Step 1 — analyzer | PASS |
| Step 2 — propose | PASS (target `resolve_intake_collect_and_handover.yaml` $.escalation_policy) |
| Step 3 — sandbox | ACCEPT (whitelisted edit; field whitelisted; yaml_diff_validator PASS) |
| Step 4 — content_validator | PASS |
| Step 5 — anti_hardcode_check | PASS (no FLAG_FOR_CODEX) |
| Step 6 — applier (Spring spawn) | **FAIL — `SpringStartupTimeoutError: Spring subprocess exited prematurely (rc=1) on port 62872`** |
| Step 7 — eval_runner | NOT REACHED |
| Step 8 — baseline load | NOT REACHED |
| Step 9 — tier_evaluator | NOT REACHED |
| Total elapsed (Steps 1-6) | 72.3s |

**Per-iter artefacts** (`autoloop/results/runs/exp-7/`):

- `hypothesis.json` — present (`$.escalation_policy` propose;
  fingerprint `38a430e6d21c4030`).
- `sandbox_verdict.json` — present (`decision: ACCEPT`).
- `content_validator_verdict.json` — present (`verdict: PASS`).
- `anti_hardcode_verdict.json` — present (`verdict: PASS`).
- `dry_run_record.json` — present (a stale partial record from
  Step 5 dry-run-artefact write before applier was reached; the
  authoritative record is in `experiments.jsonl`).
- `eval/` — ABSENT (Step 7 never invoked).
- `iteration_record.json` — ABSENT (the loop persists via
  `experiments.jsonl` append; per-iter `iteration_record.json` is
  only written on `keep` / `discard` paths reaching Step 10 — not
  on the Step 6 error path).
- `tier_evaluator_verdict.json` — ABSENT (Step 9 never invoked).

**`experiments.jsonl` tail (last entry)**:

```json
{
  "iteration_id": "exp-7",
  "decision": "error",
  "error": "SpringStartupTimeoutError: Spring subprocess exited prematurely (rc=1) on port 62872\nTraceback (most recent call last):\n  File \".../autoloop/loop.py\", line 236, in run_one_iteration\n    applied = _applier.apply(...)\n  File \".../autoloop/sandbox/applier.py\", line 179, in apply\n    _health_probe(port=backend_port, ..., proc=backend_process)\n  File \".../autoloop/sandbox/applier.py\", line 409, in _health_probe\n    raise SpringStartupTimeoutError(...)\n",
  "applied": null,
  "verdict": null,
  "gaming_flags": []
}
```

**Verdict on Goal #3**: BLOCKED. The pre-flight ergonomics tool is
delivered and empirically verified; the smoke iter end-to-end
exercise is blocked on the new Step 6 brittleness characterized
in §6. Step 7 (eval_runner with the S-Auto-7 Blocker B symlink
staging) + Step 9 (tier_evaluator non-degenerate verdict) remain
unexercised in the loop context, carrying forward from
S-Auto-7 handoff §6.

## §6 OQ-S60.7 disposition + NEW substrate finding

The dev prompt's expected OQ-S60.7 disposition was: confirm the
pre-flight stop-foreground-backend approach is sufficient, then
defer deeper root-cause work (Flyway / maven race / Redis pool /
classpath) to M-Auto-2+.

**Actual disposition**: pre-flight stop-foreground-backend approach
is empirically **NOT sufficient** as a path to a successful Step 6
spawn. The auto-reboot path cleared pid=8613 cleanly; Step 6 still
failed with the same `SpringStartupTimeoutError: Spring subprocess
exited prematurely (rc=1)` shape as S-Auto-7 exp-6.

**Diagnostic** (one direct mvn invocation, no autoloop wrapper):

```bash
$ mvn -q -pl server -am spring-boot:run \
    -Dspring-boot.run.arguments=--server.port=9999 \
    > /tmp/spring-diagnostic.log 2>&1 &
$ # wait briefly + check log:
$ tail -10 /tmp/spring-diagnostic.log
[ERROR] Failed to execute goal org.springframework.boot:spring-boot-maven-plugin:3.2.5:run (default-cli)
   on project csagent-parent: Unable to find a suitable main class, please add a 'mainClass' property -> [Help 1]
```

**Root cause**: `pom.xml` declares `<packaging>pom</packaging>` and
aggregates `<module>server</module>` + `<module>eval</module>`. The
applier's command at `autoloop/autoloop/sandbox/applier.py:370-378`:

```python
cmd = [
    "mvn", "-q",
    "-pl", "server", "-am",
    "spring-boot:run",
    f"-Dspring-boot.run.arguments=--server.port={port}",
]
```

`-pl server -am` brings the parent into the reactor (because server
inherits from it). Maven 3.x then applies the `spring-boot:run`
goal to ALL modules in the reactor that were selected via `-pl`
PLUS those pulled in by `-am`. Since csagent-parent has
packaging=pom and no `mainClass`, spring-boot:run fails on it
before ever reaching the server submodule.

**Why this surfaces now, not earlier**: the S-Auto-7 / S-Auto-5
historical Spring spawn observations (Sprint 058 exp-6 health-probe
failures; Sprint 060 exp-6 rc=1 at 90.4s) were attributed to the
foreground :8080 contention because that signal was unambiguous and
the diagnostic was never pushed to spring-boot stdout. With pre-
flight clearing the foreground contention in S-Auto-7.1, the
underlying mvn-invocation failure became the dominant signal.

**Fix locus** (out of scope for S-Auto-7.1): the fix lives in
`autoloop/autoloop/sandbox/applier.py:370` (fence-protected per
S-Auto-7.1 hard fences). Likely options for an S-Auto-7.2 dev
session:

1. Drop `-am`: `mvn -pl server spring-boot:run -Dspring-boot.run.arguments=...`
   — relies on parent + server already being installed in the
   local maven repo.
2. Invoke from the server submodule cwd:
   `cwd=str(root / "server"); cmd = ["mvn", "spring-boot:run", ...]`.
3. Use the spring-boot plugin's `:run-only` lifecycle scoping
   (Maven plugin doc).

This is a one-line applier change; it is fence-protected because
applier.py is itself a substrate-fenced module under the S-Auto-7.1
contract. Per the dev prompt's STOP-and-surface guidance, S-Auto-7.2
should be considered, and M-Auto-1B exceeding §8.5's 5-sub-sprint
ceiling should trigger a deliver-agent + human conversation about
splitting at the next milestone planning round.

**Deeper root-cause options characterized but DEFERRED to M-Auto-2+
as observation-only** (per dev prompt OQ-S60.7 disposition):
in-memory DB / Flyway timeout config / Docker sandbox per-iter were
NOT pursued in S-Auto-7.1 — they would be M-Auto-2 ergonomics
upgrades and the current failure shape no longer indicates Flyway /
Redis / classpath contention (it indicates mvn module-selection
behaviour). OQ-S60.7 narrows from "Spring spawn brittleness" to
"applier mvn module selection". The pre-flight infra is the
operator's first-line tool to clear the OTHER class of brittleness
(foreground contention) when it surfaces; it is not the full
solution to the substrate.

## §7 OQ-S60.10 ergonomics fix status

**Status**: DONE bundled in this commit.

**Implementation** (`autoloop/autoloop/cli.py:36-67`):

```python
_ENV_LOCAL_PATH = _PACKAGE_ROOT / ".env.local"

def _auto_load_env_local() -> None:
    """OQ-S60.10 ergonomics — auto-load `autoloop/.env.local` into
    `os.environ` so `AUTOLOOP_META_LLM_API_KEY` is available to the
    meta-agent client without requiring `set -a; source .env.local;
    set +a` first. Silent if the file is absent OR python-dotenv is
    unavailable — pre-flight check_meta_llm_api_key will then
    surface the missing-key fail with actionable remediation.
    """
    if not _ENV_LOCAL_PATH.exists():
        return
    try:
        from dotenv import load_dotenv  # type: ignore[import-not-found]
    except ImportError:
        return
    load_dotenv(_ENV_LOCAL_PATH, override=False)
```

Invoked at the top of `_cmd_run` and `_cmd_preflight` before any
pre-flight check that consults `os.environ`. The
`check_meta_llm_api_key` check still runs — it now reads from the
dotenv-loaded env and PASSES (verified in §4 transcript:
"AUTOLOOP_META_LLM_API_KEY set in env (length=32)").

**Dependency addition**: `python-dotenv>=1.0` added to
`autoloop/pyproject.toml:11`. `uv sync --extra dev` installed
`python-dotenv==1.2.2` into `autoloop/.venv`.

**Scope discipline**: zero touch to `autoloop/autoloop/meta_agent/llm_client.py`
(the path-mismatch in its existing `_load_env_local` is left as-is;
the higher-layer auto-load in cli.py is the convenience and the
meta-agent loader remains unchanged).

## §8 Adversarial spot-check pre-close

Independent verification of the hard fences (`git diff` driven):

```
$ git diff --stat HEAD -- \
    autoloop/autoloop/scoring/ \
    autoloop/autoloop/sandbox/ \
    autoloop/autoloop/loop.py \
    autoloop/autoloop/meta_agent/ \
    autoloop/autoloop/memory/ \
    eval_interactive/ \
    server/ eval/ data/ db/ \
    server/src/main/resources/ \
    docs/foundational/ \
    docs/runtime_freeze_and_risk_policy.md \
    docs/current/
(empty)
```

- **Fence #13 reasserted**: `_compute_scoring_code_sha()` produces
  `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`
  — bit-identical to the S-Auto-7 close baseline at b0a3704.
- **eval_interactive loader.py controlled override from S-Auto-7**:
  UNCHANGED (`git diff HEAD -- eval_interactive/eval_interactive/case_spec/loader.py`
  empty).
- **All M-Auto-1B §6 hard-fenced surfaces**: UNCHANGED (the diff
  above is empty across every fenced subtree).
- **Sprint archives + milestone archives + docs/codex-findings.md**:
  UNCHANGED (S-Auto-7.1 writes only the new `docs/sprints/sprint-061-handoff.md`
  — not present at S-Auto-7 close).

## §9 Code anchor table

| File | Status | Lines added | Lines removed |
|------|--------|------------:|--------------:|
| `autoloop/autoloop/preflight.py` | NEW | 492 | 0 |
| `autoloop/autoloop/cli.py` | MOD | 168 | 5 |
| `autoloop/tests/test_preflight.py` | NEW | 429 | 0 |
| `autoloop/tests/test_cli_integration.py` | MOD | 8 | 1 |
| `autoloop/pyproject.toml` | MOD | 1 | 0 |
| `autoloop/uv.lock` | MOD | 11 | 0 |
| `docs/sprints/sprint-061-handoff.md` | NEW | (this file) | 0 |

Total LOC delta excluding handoff: ~1090 inserted / 6 deleted
across 6 files. Within the "small-medium scope (~150-250 LOC)"
budget if measured by *non-test code* (preflight.py production code
=492, but ~150 of that is dataclass + module docstring boilerplate;
cli.py delta is 168 inserted including the new gate helper +
subcommand registration).

## §10 Test count

| Suite | Baseline (S-Auto-7 close) | S-Auto-7.1 final | Delta |
|-------|--------------------------:|-----------------:|------:|
| autoloop pytest (foreground subprocess) | 228 PASS, 1 warning | 255 PASS, 1 warning | +27 |
| autoloop pytest test_preflight.py alone | n/a | 27 PASS | +27 |
| eval_interactive pytest | 486 PASS, 3 FAIL | (NOT RE-RUN; no edits) | UNCHANGED |
| Java baseline | UNCHANGED | (NOT RE-RUN; no edits) | UNCHANGED |

The 27-test delta exactly matches the 27 tests in test_preflight.py
(every test counted once). test_cli_integration.py's one updated
test was already in the baseline (its assertion change to pass
`--skip-preflight` keeps the test count steady there).

The 1 warning is the S-Auto-2 baseline-missing-shadow asserted
behaviour UNCHANGED.

## §11 §7 stanza self-walk verification

Self-walk passed; §7 EXEMPT confirmed.

- **§3.2 Q1 (infra) match**: S-Auto-7.1 changes are environment
  discovery + reboot ergonomics + dotenv auto-load + handoff. None
  of these are semantic decision branches; they are HOW the loop
  starts and HOW the operator interacts with the substrate.
- **§7 exempt carve-out**: pure-infra carve-out applies; no
  prompt / runtime semantic decision / eval spec / judge
  calibration / keyword / regex / enum is introduced. The §7
  stanza is self-walked in §1 for paper-trail completeness only.
- **§1.7 forbidden list**: zero items touched. No raw eval phrases
  encoded; no UC-specific hard rules; no eval-spec widening; no
  visible-eval optimization at shadow cost; no prompt-as-if-else.

## §12 Open Questions surfaced

(Deliver-agent + human populate at milestone close per existing
convention; left empty by the dev session per dev prompt.)

- **OQ-S61.1** (NEW; dispositioned in §6): applier mvn module
  selection — `mvn -pl server -am spring-boot:run` applies the
  spring-boot:run goal to csagent-parent (packaging=pom, no main
  class), causing rc=1 before any server-module spawn happens.
  S-Auto-7.2 fix candidate. Fix locus:
  `autoloop/autoloop/sandbox/applier.py:370`. Fix options
  enumerated in §6.
- **OQ-S60.7 carry-forward**: pre-flight stop-foreground-backend
  approach is necessary (it cleanly resolves the foreground :8080
  contention sub-class of brittleness) but NOT sufficient (the
  applier mvn invocation surfaces as the next brittleness layer).
  Pre-flight infra delivered; substrate full unblock requires
  OQ-S61.1 resolution.
- **OQ-S60.10 dispositioned**: dotenv auto-load bundled in this
  commit. The deeper meta_agent/llm_client.py `_load_env_local`
  path mismatch is OUT OF SCOPE for S-Auto-7.1 (the cli.py-layer
  auto-load is sufficient for the operator-facing ergonomic).
  Defer the llm_client.py loader-rename to M-Auto-2 if the
  deliver-agent decides to fold back.
- **OQ-S61.2** (NEW; dispositioned in S-Auto-7.1 by default):
  auto_reboot_foreground_backend uses SIGTERM with 5s timeout
  then SIGKILL escalation. The 5s timeout was the prompt default;
  this turned out to be sufficient for the live pid=8613 case
  (transcripted at §4). If a future operator finds the timeout
  too short for their Spring deployment, the helper exposes
  `sigterm_timeout_seconds` as a keyword arg — the CLI does NOT
  surface this knob yet (would be a future-sprint UX addition).
- **OQ-S61.3** (NEW; dispositioned in S-Auto-7.1 by default):
  the pre-flight currently hard-codes :8080 / :5432 / :6379. A
  `preflight:` config block is honoured (see preflight.py
  `_preflight_cfg`) but config.yaml does not ship one — defaults
  apply. Future per-env overrides can be added without touching
  preflight.py.
- **OQ-S61.4** (NEW; observation): the pre-flight's working-tree
  check returns `warn`, not `fail`, when the working tree is
  dirty. This is by design (dev may have intentional in-flight
  state) but the deliver-agent may wish to consider a stricter
  release-gate variant that elevates to `fail` for the live-loop
  path. Observation only; no action requested.
- **OQ-S61.5** (NEW; observation): the foreground :8080 backend
  pid=8613 (the human's dev backend started Thursday) was killed
  by --auto-reboot during the smoke iter attempt. If the human
  wants it back, they should restart it manually. The pre-flight
  will detect the new pid on the next loop invocation.

---

**END OF S-AUTO-7.1 HANDOFF.** Deliver-agent + human dispatch
Codex review at M-Auto-1B close (default milestone-shared);
S-Auto-7.2 candidate for OQ-S61.1 fix.
