---
title: Sprint 064 / S-Auto-9 — OQ-S62.3 local-Mac in-env diagnostic + resolution (M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick sub-sprint 1 of 2-3)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-30
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-063-objective.md]
superseded_by: null
notes: >
  M-Auto-2 / Sprint 064 / S-Auto-9. **Layer**: `infra` (substrate-
  environment diagnostic + resolution; OS-level instrumentation; no
  semantic decision change; no projection / scoring logic edit; no
  CaseSpec / judge change). **§7 stanza**: **EXEMPT** per pure-infra
  carve-out (self-walked for paper-trail; the resolution is environmental
  OR a fence-#20 controlled override on a narrow autoloop file). **Codex
  review plan**: milestone-shared (default) at M-Auto-2 close UNLESS
  dev mid-sprint surfaces substrate brittleness requiring NEW fence #20
  controlled override (then per-sub-sprint Codex per §4.3 trigger #3
  following S-Auto-7.2 fast-iteration precedent — user direction
  2026-05-30 "像那种什么之前的什么 Content-length Validator，可以我
  review后快速放宽通过").

  **Estimated dev**: ~3-5 days total (1-2 days OS-level diagnostic
  investigation including dtrace process_exit + taskpolicy + Console.app
  + Apple system log + launchd / TAL / EDR investigation + alternate
  detachment patterns + 1-2 days fix iteration + smoke iter through
  Step 9 verification + handoff).

  **The OQ-S62.3 problem** (carry-over from sprint-063 handoff §8): an
  OS-level kill consistently terminates the autoloop python process at
  the 2-5 min mark on the developer's local Mac, regardless of:
  - Launch source: Claude Code Bash, user's iTerm2, macOS Terminal.
  - Detachment: bash nohup, bash disown, Python `subprocess.Popen
    (start_new_session=True)`, GNU screen detached session.
  - Power assertions: `caffeinate -d -i -s -t 32400` holding all 3
    `PreventSystemSleep + PreventUserIdleSystemSleep +
    PreventUserIdleDisplaySleep` assertions confirmed via `pmset -g
    assertions`.
  - Memory state: tested at 108MB free (OOM expected) AND at 7.5GB
    free (no obvious pressure); both die similarly.
  - AC power connected, lid open.
  - Process tree: PPID=1 (init re-parent) post-launcher exit.

  Substrate Steps 1-6 work end-to-end on every attempt (preflight passes;
  analyzer/proposer LLM calls return; sandbox/cv/anti_hardcode verdicts
  compute; git branch creates; mvn boots Spring on alt port). mvn java
  child process often outlives the python autoloop parent (visible as
  PPID=1 orphan), suggesting selective python kill.

  Smoke iter PID 64073 SURVIVED 17 min via Python wrapper — single
  observation; suggests intermittent / probabilistic kill, not
  deterministic.

  **What is NOT YET diagnosed**: no matching kernel jetsam log entries
  (memorystatus kill subsystem silent for killed PIDs); no `pmset -g log`
  sleep events at death times; specific signal delivered (SIGKILL vs
  SIGTERM vs other) unknown; whether macOS Background Activity Manager
  (TAL), launchd policy, or some other OS subsystem is responsible (no
  obvious EDR / security tool installed; standard developer environment).

  **CRITICAL constraint (human-locked 2026-05-30)**: M-Auto-2 stays on
  local Mac. **DO NOT pursue cloud / remote-server framing**. The local-
  Mac constraint preserves the M-Auto-1A-onward substrate assumption
  that the loop runs on the developer's actual workstation. If diagnostic
  + fix iteration cannot resolve within 1-2 days, STOP-and-surface for
  re-plan — do NOT silently switch to cloud / remote-server execution.

  **Fast-iteration STOP-and-surface authorization on substrate
  brittlenesses** (per S-Auto-7.2 precedent + user direction 2026-05-30
  "像那种什么之前的什么 Content-length Validator，可以我review后快速放宽
  通过"): if S-Auto-9 dev surfaces additional substrate brittleness or
  needs a NEW fence #20 controlled override on a narrow autoloop file
  (e.g., wrapping the orchestrator in a `launchd` user-agent plist that
  survives parent termination), dev STOP-and-surfaces via AskUserQuestion;
  human reviews + quickly authorizes via STOP-2-style "我觉得应该把这个
  问题直接修掉" pattern; NEW fence #20 added post-hoc to M-Auto-2 §6 per
  §8.3 in-place revision pattern; per §4.3 trigger #3 the mid-sprint
  authorization UPGRADES Codex review plan from milestone-shared default
  to PER-SUB-SPRINT REQUIRED at S-Auto-9 close.

  **Diagnostic investigation hypotheses to test in priority order** (dev
  may refine based on actual evidence):

  1. **macOS Background Activity Manager (TAL)**: long-running python
     processes spawned from a developer Terminal session may be subject
     to TAL policy that kills them after a few minutes if not registered
     as a system service. Check via `log show --predicate
     'subsystem == "com.apple.WorkloadOptimization"' --start "<launch-
     time>" --end "<death-time>"`.
  2. **launchd-managed lifecycle**: even with `nohup + disown` and
     `Popen(start_new_session=True)`, macOS may apply per-user-process
     policy based on cgroup-like membership; check `launchctl print
     gui/$(id -u)/<pid>` and `taskpolicy -p <pid>` during runtime.
  3. **dtrace process_exit hook to identify signal + ustack**: `sudo
     dtrace -n 'proc:::exit /pid == <autoloop-PID>/ { printf("%s %d %s
     %s\n", execname, pid, args[0]->ev_signo, ustack()); }'` — captures
     the signal number + the kernel stack at exit.
  4. **Console.app filter on `process:python`**: filter for the actual
     PID during overnight attempt + capture the last 5 lines before
     death; look for jetsam / TAL / power-event / signal-from-PID entries.
  5. **Alternate detachment via launchd user-agent plist**: create
     `~/Library/LaunchAgents/com.user.autoloop.plist` with
     `KeepAlive=false` + `RunAtLoad=false` + `LaunchOnlyOnce=true` +
     `EnableTransactions=false` + explicit `Program` pointing at the
     autoloop CLI + `WorkingDirectory` set; `launchctl bootstrap
     gui/$(id -u) <plist>` + `launchctl kickstart gui/$(id -u)/
     com.user.autoloop` to launch; survives Terminal close + parent
     re-parent + TAL policy. **PREFERRED resolution if diagnostic
     points at TAL / launchd parental policy.**
  6. **Process-priority adjustment**: `renice -n -10 <pid>` OR
     `taskpolicy -b <pid>` (background) vs `-B` (utility); test if
     priority change shifts kill behaviour.

  **Diagnostic scripts** (S-Auto-9 dev may author under pure-infra
  carve-out; not load-bearing):

  - `scripts/dtrace-autoloop-exit.d` — dtrace one-liner ready to run.
  - `scripts/diagnose-oq-s62-3.sh` — composite diagnostic harness
    launching one overnight attempt + capturing all the signals listed
    above.
  - `scripts/launch-autoloop-via-launchd.sh` + `templates/com.user.
    autoloop.plist` — launchd-managed invocation pattern (if hypothesis
    5 is the resolution path).

  Dev session source-of-truth: `compact/sprint-064-dev-prompt.md`
  (self-contained per `iteration_governance.md` §9). Dev session reads
  ONLY: `AGENTS.md` (auto-loaded) + the dev prompt.
---

# Sprint 064 / S-Auto-9 — OQ-S62.3 local-Mac in-env diagnostic + resolution

## Class

- **Layer (primary)**: `infra` (substrate-environment diagnostic + resolution; OS-level instrumentation).
- **§7 stanza**: **EXEMPT** per pure-infra carve-out (self-walked for paper-trail; see §7 below).
- **Codex review plan (§4.3)**: milestone-shared (default) at M-Auto-2 close UNLESS dev mid-sprint surfaces substrate brittleness requiring NEW fence #20 controlled override (then per-sub-sprint per §4.3 trigger #3 fast-iteration).
- **Sub-sprint position in milestone**: 1st of 2-3 (S-Auto-9 OQ-S62.3 diagnostic + resolution → S-Auto-10 first overnight + first cherry-pick → optional S-Auto-11 fix-iteration buffer → M-Auto-2 close).

## Goal

Diagnose the OS-level kill mechanism (OQ-S62.3 expansion from sprint-063 handoff §8) on local Mac + apply a fix that enables reliable overnight execution of `python -m autoloop run --experiments N` for N≥10. Retire M-Auto-1C §12.4 deferred hard gate #1 (live iter end-to-end through Step 9 with non-degenerate `tier_evaluator_verdict`). Set up the now-reliably-executable substrate for S-Auto-10 first overnight batch + first cherry-pick.

**Acceptance**: at least ONE smoke iter via `python -m autoloop run --experiments 1` reaches Step 9 (`tier_evaluator.evaluate`) AND writes a row to `autoloop/results/experiments.jsonl` with non-null `verdict.layer_results` (Layer 0-4 all non-degenerate). The smoke iter may discard at any of Steps 1-8 (sandbox / cv / anti_hardcode reject is acceptable) as long as the substrate doesn't OS-kill before reaching Step 9 OR before writing the row.

## Scope (4-5 steps)

1. **Pre-flight env check + baseline reproduction** (~half-day):
   - Verify foreground :8080 backend is running OR ready to auto-reboot via S-Auto-7.1 `--auto-reboot` flag.
   - Reproduce one fresh overnight attempt via `python -m autoloop run --experiments 15` outside Claude Code bash-tool sandbox (raw shell / Terminal / iTerm; OQ-S62.3 baseline kill expected).
   - Capture: launch timestamp + PID + death timestamp + last 5 lines of stdout/stderr + `ps` snapshot at death + Console.app last 50 lines for the PID.
   - Record in `docs/sprints/sprint-064-handoff.md` §1 "Baseline reproduction".

2. **OS-level diagnostic investigation** (~1-2 days; fast-iteration STOP-and-surface authorized):
   - **Hypothesis 3 first**: `sudo dtrace -n 'proc:::exit /pid == <autoloop-PID>/ { printf("%s %d sig=%d ustack=%s\n", execname, pid, args[0]->ev_signo, ustack()); }'` on a fresh overnight attempt; this is the highest-information-density signal (exact signal + kernel stack at exit).
   - **Hypothesis 1 + 4 in parallel**: `log show --predicate 'subsystem == "com.apple.WorkloadOptimization" OR subsystem == "com.apple.launchd" OR eventMessage CONTAINS "memorystatus" OR eventMessage CONTAINS "TAL"' --start "<launch>" --end "<death>"` + Console.app filter on `process:python` AND PID.
   - **Hypothesis 2**: `launchctl print gui/$(id -u)/<pid>` + `sudo taskpolicy -p <pid>` during runtime; capture any policy entries.
   - **Hypothesis 5 prep**: regardless of diagnostic outcome, prep a `~/Library/LaunchAgents/com.user.autoloop.plist` template + `launchctl bootstrap` runbook IF the diagnostic points at TAL / launchd parental policy.
   - Record per-hypothesis evidence in `docs/sprints/sprint-064-handoff.md` §2 "Per-hypothesis diagnostic evidence".
   - **STOP condition**: if 1-2 days produces NO actionable hypothesis (no signal captured by dtrace, no log entries, no policy entries), STOP-and-surface for deliver-agent + human re-plan (consider alternate-env framing reconsideration OR research-agent track).

3. **Fix iteration** (~1-2 days):
   - Based on §2 evidence, apply fix candidate (priority order per planning notes):
     - **Environmental fix preferred** (no autoloop code edit): launchd-managed plist invocation OR alternate detachment pattern OR system setting toggle (e.g., disable TAL for the python interpreter; `defaults write` system setting; etc.).
     - **Autoloop code edit (CONDITIONAL fence #20)**: if environmental fix insufficient (e.g., the orchestrator must explicitly register itself with launchd; or must apply `taskpolicy` to itself at startup), open NEW fence #20 controlled override at S-Auto-9 mid-sprint via AskUserQuestion (S-Auto-7.2 precedent for fast-iteration authorization). Specific surface(s) TBD per actual fix — examples: `autoloop/autoloop/cli.py` (self-register with launchd if available); NEW `autoloop/autoloop/launchd_wrapper.py` (helper module).
   - Document the fix in `docs/sprints/sprint-064-handoff.md` §3 "Fix delivered".

4. **Smoke iter verification through Step 9** (~half-day):
   - Run `python -m autoloop run --experiments 1` via the now-resolved overnight execution path.
   - Verify the iteration reaches Step 9 (`tier_evaluator.evaluate`) AND writes a row to `autoloop/results/experiments.jsonl`.
   - The row's `verdict.layer_results` must be non-null (Layer 0-4 all non-degenerate); the iteration may discard at any of Steps 1-8 (substrate rejection acceptable) as long as it doesn't OS-kill before Step 9 row write.
   - Capture wall time + PID + final stdout + `experiments.jsonl` row content in `docs/sprints/sprint-064-handoff.md` §4 "Smoke iter Step 9 verification".
   - **THIS retires M-Auto-1C §12.4 deferred hard gate #1**.

5. **OQ ledger update + handoff** (~half-day):
   - Update `docs/sprints/sprint-064-handoff.md` per §"Handoff requirements" below.
   - If NEW fence #20 controlled override was authorized mid-sprint, ensure §M-Auto-2 §6 fence #20 is post-hoc-blessed per §8.3 in-place revision pattern (parallel to M-Auto-1C fence #19 precedent).
   - Surface any new OQs (e.g., if Step 9 verification reveals additional substrate brittleness, capture as OQ-S64.x carry-over to S-Auto-10).

## Hard fences / STOP conditions

**Hard fences** (M-Auto-2 §6 17+2 list inherited; see `docs/milestone_objective.md` §6 once M-Auto-2 launches at post-Codex close-bundle):

- **No edits** to `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-063-*`, prior milestone archives under `docs/milestones/` (including `M-Auto-1C_*` archives).
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at hash `22548e20…`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,applier,content_validator}.py` (fence #15 + #18 + #19 envelopes FINALIZED at M-Auto-1C close; NO new edits at S-Auto-9 unless NEW fence #20 explicitly authorized — see CONDITIONAL fence #20 below).
- **No edits** to `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py` (unless NEW fence #20 explicitly authorized).
- **No Skill YAML edits** in `server/src/main/resources/skills/**` (cherry-pick is S-Auto-10 scope; ZERO cherry-pick at S-Auto-9).
- **No new Tier-0 invariant**.
- **No `git add -A`** — stage only S-Auto-9 scope files explicitly.
- **No cloud / remote-server framing**: M-Auto-2 stays local-Mac per human-locked direction 2026-05-30.

**CONDITIONAL fence #20** (CONDITIONAL, S-Auto-9 ONLY; authorized at S-Auto-9 mid-sprint via AskUserQuestion if needed): if OQ-S62.3 resolution requires an autoloop code edit beyond environmental fixes, authorize NEW fence #20 controlled override on the specific file(s) touched. Authorization follows S-Auto-7.2 precedent (fast-iteration mode per user direction "可以我review后快速放宽通过"); per §4.3 trigger #3 the mid-sprint authorization UPGRADES Codex review plan to PER-SUB-SPRINT REQUIRED at S-Auto-9 close. S-Auto-10 MUST NOT edit the fence #20 surface.

**STOP-and-surface conditions** (dev pauses + asks deliver-agent + human via AskUserQuestion):

- §2 diagnostic investigation produces NO actionable hypothesis within 1-2 days (no signal captured by dtrace, no log entries, no policy entries) → halt; deliver-agent + human re-plan (consider deeper substrate research-agent track OR research alternate-env framing reconsideration).
- §3 fix iteration requires a hard-fence edit beyond fence #20 envelope (e.g., touching `autoloop/autoloop/scoring/` OR `autoloop/autoloop/sandbox/` OR `autoloop/autoloop/loop.py` substantively) → halt; deliver-agent + human authorize NEW fence override OR redirect the fix.
- §3 environmental fix doesn't work AND autoloop code edit candidates are all non-trivial → halt; deliver-agent + human re-plan.
- §4 smoke iter verification reveals NEW substrate pathology NOT explained by OQ-S62.3 (e.g., Spring spawn now fails for a different reason post-resolution) → halt; root-cause investigation.
- Human cancels mid-sprint due to time / priority shift.

## Test / eval requirements

- **autoloop pytest baseline preserved**: `cd autoloop && uv run --extra dev pytest -q` returns ≥ 266 PASS (S-Auto-7.2 close baseline). S-Auto-9 typically adds 0-N new tests (only if NEW fence #20 helper code is authored with tests).
- **eval_interactive pytest baseline UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed`.
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns `31 passed`.
- **scoring_code_baseline_sha REASSERTED**: `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`; `_check_scoring_code_drift(config) == []` silent.
- **Java baseline UNCHANGED**: zero-touch verified via `git diff --stat HEAD -- server/ eval/src/main/java/` returning empty.
- **Smoke iter through Step 9 captured**: `experiments.jsonl` row with non-null `verdict.layer_results` (Layer 0-4 all non-degenerate); the iteration may discard at any of Steps 1-8.

## §7 stanza (EXEMPT — pure-infra carve-out self-walked for paper-trail)

**Target failure layer**: `infra` (substrate-environment kill mechanism on local Mac; OS-level diagnostic + resolution; no semantic decision change; no projection / scoring semantic logic edit; no CaseSpec / judge change). Per `iteration_governance.md` §3.2 question 1 ("Is the session failing to start, crash on infra, or hit a timeout / OOM not caused by tool semantics?") → `infra`.

**Tier-0 invariant**: This sub-sprint adds no Tier-0 invariant. C2/C3 candidates from M2 close remain DEFER unchanged. No Tier-0 invariant is touched (the substrate-environment fix is OS-level; not a runtime contract change).

**Semantic hardcode**: No semantic hardcode introduced. The fix is environmental (launchd plist / taskpolicy / system setting toggle) OR a narrow autoloop code edit under fence #20 (wrapping the orchestrator in a process-lifecycle pattern — no semantic decision logic). No keyword / regex / if-else / enum / per-UC matrix added.

**Generalization coverage**: not applicable for pure-infra (§7 exempt for pure infra per `iteration_governance.md` §7 first paragraph carve-out). The OQ-S62.3 resolution applies to ALL overnight execution attempts; the smoke iter verification through Step 9 is the universal acceptance test.

## Codex review plan (per §4.3)

**Default**: milestone-shared at M-Auto-2 close. Deliver-agent + human dispatch the M-Auto-2 milestone-shared review prompt at close-bundle commit (covers S-Auto-9 + S-Auto-10 + cherry-pick commit if any + optional S-Auto-11).

**Trigger conditions** (dev STOP-and-surfaces if any fire):

- §4.3 trigger #2 (new Tier-0 candidate) DOES NOT fire (no Tier-0 invariant introduced).
- §4.3 trigger #1 (§1.7 forbidden-list red line) DOES NOT fire (substrate-environment fix is OS-level).
- §4.3 trigger #3 (hard-fenced surface explicitly named out of scope) **CAN FIRE** if S-Auto-9 needs to touch a previously-fenced autoloop file beyond environmental fixes. In that case, upgrade S-Auto-9 to PER-SUB-SPRINT Codex per §4.3 trigger #3 at sub-sprint close BEFORE M-Auto-2 milestone-shared dispatches. Authorization follows S-Auto-7.2 + M-Auto-1C fence #19 precedent (mid-sprint AskUserQuestion + human-locked "我觉得应该把这个问题直接修掉"-style authorization).

## Handoff requirements

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

## Commit discipline

- **Multi-commit pattern acceptable** (S-Auto-9 is investigation + iteration-heavy; may produce multiple commits including diagnostic scripts + fix + handoff). Single-commit pattern preferred for the FIX commit itself.
- **Commit message format**: `Sprint 064 / S-Auto-9 / M-Auto-2 — <commit description>` with bullet-point body summarizing the per-commit scope.
- **Standard deliver-agent footer** at commit message end.
- **No `git add -A`** — stage explicitly. Each commit stages only the relevant scope files.

## Self-check (dev MUST verify before claiming done)

- [ ] Baseline reproduction captured: launch + PID + death timestamps documented.
- [ ] Per-hypothesis diagnostic evidence captured: dtrace signal (or no-signal observation) + Console.app + log show + launchctl + taskpolicy results documented for each tested hypothesis.
- [ ] Fix delivered + documented (environmental OR fence #20 code edit).
- [ ] Smoke iter through Step 9 verified: `experiments.jsonl` row with non-null `verdict.layer_results`.
- [ ] If NEW fence #20 controlled override authorized: AskUserQuestion record captured + §8.3 in-place revision pattern applied to M-Auto-2 §6.
- [ ] autoloop pytest passes (≥266 PASS).
- [ ] eval_interactive baseline UNCHANGED (486 passed, 3 failed).
- [ ] 17-fixture detector sweep PASS UNCHANGED.
- [ ] scoring SHA reasserted; `_check_scoring_code_drift(config) == []` silent.
- [ ] Java baseline UNCHANGED (zero-touch verified).
- [ ] Hard-fence diff cumulative against all M-Auto-2 §6 gated paths returns empty (or only the conditional fence #20 surface).
- [ ] Handoff §0-§9 sections all filled per format.
- [ ] M-Auto-2 §5 acceptance bar S-Auto-9 contribution items ticked off.
- [ ] Local-Mac constraint honored throughout (NO cloud / remote-server framing introduced).
