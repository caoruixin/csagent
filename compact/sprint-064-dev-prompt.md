# Dev session prompt — Sprint 064 / S-Auto-9 / M-Auto-2 — OQ-S62.3 local-Mac in-env diagnostic + resolution

You are the **dev agent for Sprint 064 / S-Auto-9 / M-Auto-2** (Milestone M-Auto-2 sub-sprint 1 of 2-3). Your one-sentence goal: **diagnose the OS-level kill mechanism (OQ-S62.3 expansion) on the developer's local Mac + apply a fix that enables reliable overnight execution of `python -m autoloop run --experiments N` for N≥10; retire M-Auto-1C §12.4 deferred hard gate #1 (live iter end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`) via a smoke iter that captures an `experiments.jsonl` row.**

## ⚠️ CRITICAL operational constraints

1. **Local-Mac only**: M-Auto-2 stays on the developer's local Mac per human-locked direction 2026-05-30 "我需要在 local Mac 上面来执行这个 overnight 的 autoloop，不要找这种 cloud 或者 remote server". DO NOT pursue cloud / remote-server framing. If diagnostic + fix iteration cannot resolve within 1-2 days, STOP-and-surface for deliver-agent + human re-plan — do NOT silently switch to cloud / remote-server execution.

2. **Fast-iteration STOP-and-surface authorization on substrate brittlenesses**: if you surface additional substrate brittleness or need a NEW fence #20 controlled override on a narrow autoloop file (e.g., wrapping the orchestrator in a `launchd` user-agent plist that survives parent termination, OR a self-registration helper in `cli.py`), STOP-and-surface via AskUserQuestion. Human reviews + quickly authorizes via STOP-2-style pattern (S-Auto-7.2 precedent — user direction 2026-05-30 "像那种什么之前的什么 Content-length Validator，可以我review后快速放宽通过"). NEW fence #20 added post-hoc to M-Auto-2 §6 per §8.3 in-place revision pattern; per §4.3 trigger #3 the mid-sprint authorization UPGRADES Codex review plan from milestone-shared default to PER-SUB-SPRINT REQUIRED at S-Auto-9 close.

3. **Substrate Steps 1-6 are KNOWN GOOD**: per S-Auto-7.2 + S-Auto-8 evidence, the autoloop substrate code (post-OQ-S61.1 + OQ-S62.1 + OQ-S62.2 fixes) reaches Step 6 mvn spawn successfully on every overnight launch attempt. The OQ-S62.3 expansion problem is at the OS level, NOT in autoloop code. Your diagnostic + fix focus is OS-level (dtrace + Console.app + launchd + taskpolicy + alternate detachment patterns); autoloop code edits are LAST RESORT under conditional fence #20.

## Read order (minimal)

Read only:

1. `AGENTS.md` (auto-loaded via constitution chain — pulls `doc_governance.md` + `agent_context_guide.md` + `iteration_governance.md`).
2. This prompt (self-contained executable view per `iteration_governance.md` §9 invariant).

You may sample (NOT embed) the following code paths + docs for verification or reference:

- `docs/sprints/sprint-063-handoff.md` §4 (overnight batch evidence table — 9 launch attempts with PIDs + wall times + cause hypothesis per attempt) + §8 (OQ-S62.3 EXPANSION full diagnostic statement). **Critical input for understanding what was already tried.**
- `autoloop/autoloop/sandbox/applier.py` — S-Auto-7.2 OQ-S61.1 mvn fix at lines 370-378 (`mvn -q -pl server spring-boot:run`); OQ-S62.2 idempotency at `_git_create_branch`. **DO NOT EDIT** (fence #18 FINALIZED at M-Auto-1C close).
- `autoloop/autoloop/sandbox/content_validator.py` — S-Auto-7.2 OQ-S62.1 rule 3 rewrite. **DO NOT EDIT** (fence #19 FINALIZED).
- `autoloop/autoloop/loop.py` — 14-step state machine; Step 6 = mvn spawn + Spring health probe; Step 7 = eval_runner subprocess; Step 9 = tier_evaluator.evaluate. **DO NOT EDIT** unless NEW fence #20 explicitly authorized.
- `autoloop/autoloop/preflight.py` — S-Auto-7.1 pre-flight 6 checks + `auto_reboot_foreground_backend` helper.
- `autoloop/autoloop/cli.py` — `run` subcommand entry point; `--auto-reboot` + `--skip-preflight` flags. **DO NOT EDIT** unless NEW fence #20 authorized (self-registration with launchd is a candidate scope).
- `autoloop/config.yaml` — `fitness.baseline_dir` pointed at `eval_interactive/results/m-auto-1b-baseline-20260529/`; `content_validator.length_overflow_absolute_ceiling: 1200`; `anti_hardcode.synonym_map_enabled: true`; `scoring_code_baseline_sha: 22548e20…`.
- `~/Library/LaunchAgents/` — your prep target for hypothesis 5 (launchd user-agent plist) if diagnostic points at TAL / launchd parental policy.

---

## Embedded sub-sprint contract (from `compact/sprint-064-objective.md`)

### Class

- **Layer (primary)**: `infra` (substrate-environment diagnostic + resolution; OS-level instrumentation).
- **§7 stanza**: **EXEMPT** per pure-infra carve-out (self-walked for paper-trail; see §7 below).
- **Codex review plan (§4.3)**: milestone-shared (default) at M-Auto-2 close UNLESS dev mid-sprint surfaces substrate brittleness requiring NEW fence #20 controlled override (then per-sub-sprint per §4.3 trigger #3 fast-iteration).
- **Sub-sprint position in milestone**: 1st of 2-3 (S-Auto-9 OQ-S62.3 diagnostic + resolution → S-Auto-10 first overnight + first cherry-pick → optional S-Auto-11 fix-iteration buffer → M-Auto-2 close).

### Goal

Diagnose the OS-level kill mechanism (OQ-S62.3 expansion from sprint-063 handoff §8) on local Mac + apply a fix that enables reliable overnight execution of `python -m autoloop run --experiments N` for N≥10. Retire M-Auto-1C §12.4 deferred hard gate #1 (live iter end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`).

**Acceptance**: at least ONE smoke iter via `python -m autoloop run --experiments 1` reaches Step 9 (`tier_evaluator.evaluate`) AND writes a row to `autoloop/results/experiments.jsonl` with non-null `verdict.layer_results` (Layer 0-4 all non-degenerate). The smoke iter may discard at any of Steps 1-8 (sandbox / cv / anti_hardcode reject is acceptable) as long as the substrate doesn't OS-kill before reaching Step 9 OR before writing the row.

### Scope (4-5 steps)

**Step 1 — Pre-flight env check + baseline reproduction** (~half-day):

- Verify foreground :8080 backend is running OR ready to auto-reboot via S-Auto-7.1 `--auto-reboot` flag.
  ```bash
  curl -sf http://localhost:8080/actuator/health && echo "OK" || echo "DOWN"
  ```
- Optionally: `cd autoloop && uv run python -m autoloop preflight` to confirm 6 checks pass.
- Reproduce one fresh overnight attempt via raw shell (NOT Claude Code Bash tool):
  ```bash
  cd autoloop && nohup python -u -m autoloop run --experiments 15 > /tmp/sprint-064-baseline.log 2>&1 &
  echo $! > /tmp/sprint-064-baseline.pid
  date +%s > /tmp/sprint-064-baseline-launch.ts
  ```
- Wait for kill (expected at 2-5 min mark based on S-Auto-8 §4 evidence).
- Capture: launch timestamp + PID + death timestamp + last 5 lines of stdout/stderr + `ps` snapshot at death + Console.app last 50 lines for the PID via:
  ```bash
  date +%s > /tmp/sprint-064-baseline-death.ts
  tail -5 /tmp/sprint-064-baseline.log
  log show --predicate "process == 'python'" --info --debug --last 5m | head -50
  ```
- Record in `docs/sprints/sprint-064-handoff.md` §1 "Baseline reproduction".

**Step 2 — OS-level diagnostic investigation** (~1-2 days; fast-iteration STOP-and-surface authorized):

Test hypotheses in priority order. Each test produces an evidence entry for `docs/sprints/sprint-064-handoff.md` §2.

**Hypothesis 3 first (highest information-density)**: dtrace process_exit hook to identify signal + ustack.

```bash
# Run in one Terminal:
sudo dtrace -n 'proc:::exit /pid == <autoloop-PID>/ { printf("execname=%s pid=%d sig=%d ustack:\n", execname, pid, args[0]->ev_signo); ustack(); }' > /tmp/sprint-064-dtrace.log 2>&1 &
DTRACE_PID=$!

# Launch overnight in another Terminal:
cd autoloop && nohup python -u -m autoloop run --experiments 1 > /tmp/sprint-064-dtrace-attempt.log 2>&1 &
AUTOLOOP_PID=$!
echo "AUTOLOOP_PID=$AUTOLOOP_PID"
# Update the dtrace command's <autoloop-PID> if needed (you can stop + restart dtrace with the actual PID)

# Wait for kill (2-5 min)
wait $AUTOLOOP_PID
cat /tmp/sprint-064-dtrace.log
```

If dtrace fails or produces no signal entry, fall back to hypothesis 1 + 4.

**Hypothesis 1 + 4 in parallel**: `log show` filter + Console.app on `process:python`.

```bash
# Launch overnight; capture launch time
date "+%Y-%m-%d %H:%M:%S" > /tmp/launch.ts
cd autoloop && nohup python -u -m autoloop run --experiments 1 > /tmp/sprint-064-log-attempt.log 2>&1 &
AUTOLOOP_PID=$!

# Wait for kill
wait $AUTOLOOP_PID
DEATH_TS=$(date "+%Y-%m-%d %H:%M:%S")
echo "death=$DEATH_TS"

# Capture log show for WorkloadOptimization + launchd + memorystatus + TAL
log show \
  --predicate 'subsystem == "com.apple.WorkloadOptimization" OR subsystem == "com.apple.launchd" OR eventMessage CONTAINS "memorystatus" OR eventMessage CONTAINS "TAL" OR eventMessage CONTAINS "killed"' \
  --start "$(cat /tmp/launch.ts)" \
  --end "$DEATH_TS" \
  > /tmp/sprint-064-log-show.log 2>&1

# Filter for the PID
grep -E "pid[= ]+$AUTOLOOP_PID\b|process[= ]+python.*$AUTOLOOP_PID" /tmp/sprint-064-log-show.log
```

**Hypothesis 2**: launchctl + taskpolicy inspection during runtime.

```bash
# While overnight running, in another Terminal:
launchctl print gui/$(id -u)/<AUTOLOOP_PID> > /tmp/sprint-064-launchctl.log 2>&1
sudo taskpolicy -p <AUTOLOOP_PID> > /tmp/sprint-064-taskpolicy.log 2>&1
ps -o pri,nice,pid,user,command -p <AUTOLOOP_PID> >> /tmp/sprint-064-taskpolicy.log
```

**Hypothesis 5 prep**: regardless of diagnostic outcome, prep a launchd user-agent plist template:

```xml
<!-- ~/Library/LaunchAgents/com.user.autoloop.plist (template; fill in absolute paths) -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.user.autoloop</string>
    <key>ProgramArguments</key>
    <array>
        <string>/Users/<USER>/projects/csagent-latest/autoloop/.venv/bin/python</string>
        <string>-u</string>
        <string>-m</string>
        <string>autoloop</string>
        <string>run</string>
        <string>--experiments</string>
        <string>15</string>
    </array>
    <key>WorkingDirectory</key>
    <string>/Users/<USER>/projects/csagent-latest/autoloop</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>AUTOLOOP_META_LLM_API_KEY</key>
        <string><FILL-OR-LOAD-FROM-DOTENV></string>
    </dict>
    <key>StandardOutPath</key>
    <string>/tmp/autoloop-launchd-stdout.log</string>
    <key>StandardErrorPath</key>
    <string>/tmp/autoloop-launchd-stderr.log</string>
    <key>RunAtLoad</key>
    <false/>
    <key>KeepAlive</key>
    <false/>
    <key>LaunchOnlyOnce</key>
    <true/>
    <key>EnableTransactions</key>
    <false/>
    <key>ProcessType</key>
    <string>Adaptive</string>
</dict>
</plist>
```

```bash
# Load + launch:
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/com.user.autoloop.plist
launchctl kickstart gui/$(id -u)/com.user.autoloop
# Verify alive + check stdout:
launchctl print gui/$(id -u)/com.user.autoloop
tail -f /tmp/autoloop-launchd-stdout.log
```

Record per-hypothesis evidence in `docs/sprints/sprint-064-handoff.md` §2.

**STOP condition**: if 1-2 days produces NO actionable hypothesis (no signal captured by dtrace, no log entries, no policy entries), STOP-and-surface for deliver-agent + human re-plan via AskUserQuestion.

**Step 3 — Fix iteration** (~1-2 days):

Based on §2 evidence, apply fix candidate (priority order):

- **Environmental fix preferred** (no autoloop code edit; no fence override needed):
  - Launchd-managed plist invocation (hypothesis 5; if TAL / launchd-parental-policy is the root cause).
  - Alternate detachment pattern (e.g., `screen -dm` with explicit `-L` logging + alternate session policy).
  - System setting toggle (e.g., `defaults write com.apple.WorkloadOptimization`-style; only if `log show` evidence directly points at TAL).
  - Process-priority adjustment (`taskpolicy -B <pid>` for utility tier or `renice -n -10 <pid>` for higher priority).

- **Autoloop code edit (CONDITIONAL fence #20)**: if environmental fix insufficient, open NEW fence #20 controlled override at S-Auto-9 mid-sprint via AskUserQuestion. Examples:
  - `autoloop/autoloop/cli.py` self-register with launchd at startup (if hypothesis 5 evidence points at parental-policy but plist-based launch is too operationally heavy for the dev workflow).
  - NEW `autoloop/autoloop/launchd_wrapper.py` (helper module that wraps the orchestrator in a launchd-managed lifecycle).
  - NEW `autoloop/autoloop/process_hardening.py` (apply `taskpolicy` to self at startup).

Document the fix in `docs/sprints/sprint-064-handoff.md` §3 "Fix delivered".

**Step 4 — Smoke iter verification through Step 9** (~half-day):

```bash
# Via the now-resolved overnight execution path (environmental OR launchd OR fence #20 wrapper):
cd autoloop && nohup python -u -m autoloop run --experiments 1 > /tmp/sprint-064-smoke.log 2>&1 &
echo $! > /tmp/sprint-064-smoke.pid

# Wait for completion (expected 12-25 min for full Steps 1-9):
wait $(cat /tmp/sprint-064-smoke.pid)
tail -50 /tmp/sprint-064-smoke.log

# Verify experiments.jsonl row written with non-null verdict.layer_results:
tail -1 autoloop/results/experiments.jsonl | python3 -c "import json, sys; row = json.loads(sys.stdin.read()); print('layer_results:', row.get('verdict', {}).get('layer_results'))"
```

The row's `verdict.layer_results` must be non-null (Layer 0-4 all non-degenerate); the iteration may discard at any of Steps 1-8 (substrate rejection acceptable) as long as it doesn't OS-kill before Step 9 row write.

Capture wall time + PID + final stdout + `experiments.jsonl` row content in `docs/sprints/sprint-064-handoff.md` §4 "Smoke iter Step 9 verification".

**THIS retires M-Auto-1C §12.4 deferred hard gate #1**.

**Step 5 — OQ ledger update + handoff** (~half-day):

- Update `docs/sprints/sprint-064-handoff.md` per §"Handoff requirements" below.
- If NEW fence #20 controlled override was authorized mid-sprint, ensure M-Auto-2 §6 fence #20 is post-hoc-blessed per §8.3 in-place revision pattern (parallel to M-Auto-1C fence #19 precedent).
- Surface any new OQs (e.g., if Step 9 verification reveals additional substrate brittleness, capture as OQ-S64.x carry-over to S-Auto-10).

### Hard fences / STOP conditions

**Hard fences** (M-Auto-2 §6 17+2 list inherited):

- **No edits** to `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-063-*`, prior milestone archives.
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at hash `22548e20…`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,applier,content_validator}.py` (fence #15 + #18 + #19 FINALIZED at M-Auto-1C close).
- **No edits** to `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py` (unless NEW fence #20 explicitly authorized).
- **No Skill YAML edits** in `server/src/main/resources/skills/**` (cherry-pick is S-Auto-10 scope).
- **No new Tier-0 invariant**.
- **No `git add -A`** — stage explicitly.
- **No cloud / remote-server framing**: M-Auto-2 stays local-Mac.

**CONDITIONAL fence #20** (S-Auto-9 ONLY; authorize at mid-sprint via AskUserQuestion if needed): NEW controlled override on specific autoloop file(s) touched by the OQ-S62.3 fix. Authorization follows S-Auto-7.2 precedent; per §4.3 trigger #3 the mid-sprint authorization UPGRADES Codex review plan to PER-SUB-SPRINT REQUIRED at S-Auto-9 close. S-Auto-10 MUST NOT edit the fence #20 surface.

**STOP-and-surface conditions** (pause + ask deliver-agent + human via AskUserQuestion):

- §2 diagnostic investigation produces NO actionable hypothesis within 1-2 days (no signal captured, no log entries, no policy entries) → halt; deliver-agent + human re-plan (consider deeper substrate research-agent track OR alternate-env framing reconsideration).
- §3 fix iteration requires a hard-fence edit beyond fence #20 envelope (e.g., touching scoring/ OR sandbox/ OR loop.py substantively) → halt; deliver-agent + human authorize NEW fence override OR redirect the fix.
- §3 environmental fix doesn't work AND autoloop code edit candidates are all non-trivial → halt; deliver-agent + human re-plan.
- §4 smoke iter verification reveals NEW substrate pathology NOT explained by OQ-S62.3 (e.g., Spring spawn now fails for a different reason post-resolution) → halt; root-cause investigation.

### Test / eval requirements

- **autoloop pytest baseline preserved**: `cd autoloop && uv run --extra dev pytest -q` returns ≥ 266 PASS. S-Auto-9 typically adds 0-N new tests (only if NEW fence #20 helper code is authored with tests).
- **eval_interactive pytest baseline UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed`.
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns `31 passed`.
- **scoring_code_baseline_sha REASSERTED**: `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`; `_check_scoring_code_drift(config) == []` silent.
- **Java baseline UNCHANGED**: zero-touch verified via `git diff --stat HEAD -- server/ eval/src/main/java/` empty.
- **Smoke iter through Step 9 captured**: `experiments.jsonl` row with non-null `verdict.layer_results` (Layer 0-4 all non-degenerate).

### §7 stanza (EXEMPT — pure-infra carve-out self-walked for paper-trail)

**Target failure layer**: `infra` (substrate-environment kill mechanism on local Mac; OS-level diagnostic + resolution; no semantic decision change). Per `iteration_governance.md` §3.2 question 1 → `infra`.

**Tier-0 invariant**: This sub-sprint adds no Tier-0 invariant. No Tier-0 invariant is touched.

**Semantic hardcode**: No semantic hardcode introduced. The fix is environmental (launchd plist / taskpolicy / system setting toggle) OR a narrow autoloop code edit under fence #20 (wrapping the orchestrator in a process-lifecycle pattern — no semantic decision logic). No keyword / regex / if-else / enum / per-UC matrix added.

**Generalization coverage**: not applicable for pure-infra (§7 exempt). The OQ-S62.3 resolution applies to ALL overnight execution attempts; the smoke iter verification through Step 9 is the universal acceptance test.

### Codex review plan (per §4.3)

**Default**: milestone-shared at M-Auto-2 close.

**Trigger conditions** (STOP-and-surface if any fire):

- §4.3 trigger #3 (hard-fenced surface explicitly named out of scope) **CAN FIRE** if S-Auto-9 needs to touch a previously-fenced autoloop file beyond environmental fixes. In that case, upgrade S-Auto-9 to PER-SUB-SPRINT Codex per §4.3 trigger #3 at sub-sprint close BEFORE M-Auto-2 milestone-shared dispatches. Authorization follows S-Auto-7.2 + M-Auto-1C fence #19 precedent.

### Handoff requirements

`docs/sprints/sprint-064-handoff.md` author at sub-sprint close. Mandatory sections:

1. **§0 Sub-sprint summary**: Goal, scope, cumulative commit list, final test counts, OQ-S62.3 root cause + fix path + smoke iter Step 9 verification evidence + M-Auto-1C §12.4 deferred hard gate #1 retirement confirmation.
2. **§1 Baseline reproduction**: launch + PID + death timestamps + last 5 lines + Console.app capture.
3. **§2 Per-hypothesis diagnostic evidence**: hypothesis 1-5 (or more) per-hypothesis test results + evidence.
4. **§3 Fix delivered**: root cause + fix path (environmental OR fence #20 code edit) + per-commit per-file LOC summary.
5. **§4 Smoke iter Step 9 verification**: wall time + PID + final stdout + `experiments.jsonl` row content (verdict.layer_results non-null).
6. **§5 NEW fence #20 controlled override (if authorized)**: authorization timestamp + AskUserQuestion record + specific surface(s) touched + per §8.3 in-place revision pattern post-hoc-blessing.
7. **§6 OQs surfaced (if any)**: any new OQ-S64.x carry-over to S-Auto-10.
8. **§7 STOP-and-surface log (if any)**: timestamps + AskUserQuestion records.
9. **§8 M-Auto-2 close-readiness checklist (S-Auto-9 contribution)**: tick-off M-Auto-2 §5 acceptance bar items addressed by S-Auto-9.

### Commit discipline

- **Multi-commit pattern acceptable**. Single-commit pattern preferred for the FIX commit itself.
- **Commit message format**: `Sprint 064 / S-Auto-9 / M-Auto-2 — <commit description>` with bullet-point body summarizing the per-commit scope.
- **Standard deliver-agent footer** at commit message end (Co-Authored-By if applicable).
- **No `git add -A`** — stage explicitly.

---

## Self-check (MUST verify before claiming done)

- [ ] Baseline reproduction captured: launch + PID + death timestamps documented in `docs/sprints/sprint-064-handoff.md` §1.
- [ ] Per-hypothesis diagnostic evidence captured: dtrace signal (or no-signal observation) + Console.app + log show + launchctl + taskpolicy results documented for each tested hypothesis in §2.
- [ ] Fix delivered + documented (environmental OR fence #20 code edit) in §3.
- [ ] Smoke iter through Step 9 verified: `experiments.jsonl` row with non-null `verdict.layer_results` captured in §4.
- [ ] If NEW fence #20 controlled override authorized: AskUserQuestion record captured + §8.3 in-place revision pattern applied to M-Auto-2 §6 + fence #20 documented in handoff §5.
- [ ] autoloop pytest passes (≥266 PASS): `cd autoloop && uv run --extra dev pytest -q`.
- [ ] eval_interactive baseline UNCHANGED (486 passed, 3 failed): `cd eval_interactive && uv run python -m pytest --tb=no -q`.
- [ ] 17-fixture detector sweep PASS UNCHANGED: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py`.
- [ ] Scoring SHA reasserted: `cd autoloop && uv run python -c "from autoloop.scoring.gaming import _check_scoring_code_drift; from autoloop.config import load; cfg = load('config.yaml'); print(_check_scoring_code_drift(cfg))"` returns `[]`.
- [ ] Java baseline UNCHANGED (zero-touch): `git diff --stat HEAD -- server/ eval/src/main/java/` returns empty.
- [ ] Hard-fence diff cumulative against all M-Auto-2 §6 gated paths: `git diff --stat <S-Auto-9-start>..HEAD -- <gated prefixes>` returns empty (or only the conditional fence #20 surface).
- [ ] Handoff §0-§9 sections all filled per format.
- [ ] M-Auto-2 §5 acceptance bar S-Auto-9 contribution items ticked off (gate #4 "OQ-S62.3 RESOLVED on local Mac" + gate #5 "Live iteration end-to-end through Step 9").
- [ ] Local-Mac constraint honored throughout (NO cloud / remote-server framing introduced; verify by re-reading the dev session log for any cloud/SSH/remote reference).
