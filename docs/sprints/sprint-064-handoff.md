---
title: Sprint 064 / S-Auto-9 / M-Auto-2 — Handoff
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-31
review_cadence: ad hoc
notes: >
  S-Auto-9 sub-sprint 1 of 2-3 in M-Auto-2. Goal: diagnose OQ-S62.3 (the
  persistent "OS-level 2-5 min kill" of overnight autoloop runs on local Mac)
  and unblock reliable overnight execution + retire M-Auto-1C §12.4 deferred
  gate #1 (live iter end-to-end through Step 9 with non-degenerate
  tier_evaluator_verdict). OUTCOME: root cause was NOT an OS/jetsam/launchd/TAL
  kill — it was a self-inflicted process-group suicide in the autoloop's own
  applier (os.killpg on a group the mvn child shared with the orchestrator),
  compounded by a macOS-system-proxy-routed health probe that produced a
  spurious 120s SpringStartupTimeout. Three narrow applier.py fixes under a
  NEW fence #20 controlled override resolve all of it; a smoke iter reached
  Step 9 with a non-null 5-layer verdict. Gate #1 RETIRED.
---

## §0 Sub-sprint summary

- **Sub-sprint**: S-Auto-9 (sub-sprint 1 of 2-3 in M-Auto-2 — auto-evolution
  overnight enablement). Class: `infra` (§7-exempt; pure-infra carve-out).
- **Goal**: diagnose OQ-S62.3 (OS-level kill of overnight autoloop) on local
  Mac + apply a fix enabling reliable overnight execution; retire M-Auto-1C
  §12.4 deferred gate #1 (live iter end-to-end through Step 9 with
  non-degenerate `tier_evaluator_verdict`).
- **Outcome**: **DONE**. OQ-S62.3 root-caused + fixed; smoke iter (exp-18)
  reached Step 9 with a non-null 5-layer verdict. Gate #1 RETIRED.
- **Root cause (overturns sprint-063 §8 "OS-level kill" framing)**: the kill
  was **self-inflicted**. `applier._spawn_spring` spawned `mvn spring-boot:run`
  WITHOUT a new session, so the mvn/JVM child shared the autoloop python's
  process group (and, under a non-interactive bash launcher, the launching
  shell's group too). `applier._terminate_process` tears the backend down with
  `os.killpg(os.getpgid(proc.pid), SIGTERM/SIGKILL)` — which therefore killed
  the autoloop orchestrator itself (and its shell). No jetsam, no launchd, no
  TAL, no memory kill. The death fired at the 120s health-probe timeout
  (~90s LLM + 120s timeout ≈ 3 min), which itself was spurious (see OQ-S64.2).
- **Three fixes delivered** (all in `autoloop/autoloop/sandbox/applier.py`,
  under NEW fence #20 — see §5):
  1. **OQ-S62.3** — `start_new_session=True` in `_spawn_spring`: isolates
     mvn+JVM in their own session/pgrp so `killpg` targets only them.
  2. **OQ-S64.1** — mvn stdout → per-port log file (was undrained
     `subprocess.PIPE`): prevents a 64KB-pipe-buffer deadlock that would
     freeze the backend mid-eval (Step 7) once request logging accrued.
  3. **OQ-S64.2** — `trust_env=False` in `_probe_url_is_up`: the health
     probe is always to a localhost alt-port backend and must not be routed
     through a proxy. httpx 0.28.1 (`trust_env=True` default) honored the
     macOS **system** proxy (`127.0.0.1:7890`, a local Clash/V2Ray-style
     proxy) but ignored its ExceptionsList bypass for `127.0.0.1`, so every
     probe request was proxied away from Spring → spurious 120s timeout.
- **Smoke iter verification (exp-18)**: full iteration in **~71s** (substrate
  now fast + reliable); reached Step 9; `verdict.layer_results` is a non-null
  5-layer list (Layer 0 `tier0_safety` PASS, Layer 1 `tier1_outcome` FAIL →
  discard, Layers 2-4 designed short-circuit). **Retires M-Auto-1C §12.4
  deferred gate #1.**
- **Commits** (this bundle): one commit on `auto-loop-branch` (applier.py 3
  fixes + 7 diagnostic scripts + this handoff).
- **Test counts**: autoloop pytest **266 PASS** (UNCHANGED); 17-fixture
  anti_hardcode detector **31 PASS** (UNCHANGED); scoring SHA
  `22548e20…188a9` REASSERTED (`drift=[]`); Java baseline zero-touch (empty
  `git diff` on `server/` + `eval/src/main/java/`); eval_interactive baseline
  **486 passed, 3 failed** (UNCHANGED — applier.py is autoloop-only).
- **Local-Mac constraint honored**: no cloud / remote-server framing
  introduced. All diagnosis + fix + verification on the developer's local Mac.

## §1 Baseline reproduction

Fresh overnight launch reproduced the kill (Step 1, raw Terminal, `nohup
python -u -m autoloop run --experiments 15 --auto-reboot &`):

| field | value |
|---|---|
| launch | 2026-05-31 06:53:02 |
| death | 2026-05-31 06:56:00 |
| elapsed | **178s** (≈3 min — squarely in the historic 2-5 min window) |
| PID | 68280 |
| **NI at launch** | **5** (non-default — see §2 BG_NICE; NOT the cause) |
| `launchctl print gui/$uid/68280` | "Could not find service" (unmanaged nohup process) |
| stdout last line | `[run] starting 15 iteration(s), dry_run=False` |
| stderr | **empty** (no Python traceback) |
| `.ips` crash report | **none generated** → external kill, not a Python crash |
| `log show` for PID 68280 | only 3 XPC-registration entries in the first 2s; **silent at death** |
| jetsam / memorystatus / RunningBoard for PID | **no kill entry** (kill invisible to those subsystems) |
| memory at the time | ~358MB free / 32GB; ~17GB in compressor (tight, but not the cause) |

The absent traceback + absent `.ips` + silent logs initially suggested an
external SIGKILL; §2 shows it was a SIGTERM/SIGKILL the autoloop sent to its
own process group.

## §2 Per-hypothesis diagnostic evidence

Diagnostic scripts live in `scripts/sprint-064-*` (committed). Scratch
artifacts in `/tmp/sprint-064-*` (not committed).

- **H (jetsam / memorystatus / launchd / RunningBoard kill)** — **FALSIFIED.**
  `log show` across the death window shows no kill event for the PID; only
  generic "Ignoring jetsam update because this process is not memory-managed"
  lines for unrelated GUI apps. No `.ips`. (sprint-063 §8's primary
  hypotheses.)
- **H (generic nohup'd-python kill / OS background policy)** — **FALSIFIED.**
  `scripts/sprint-064-signal-trap-probe.sh`: a pure-Python idle process
  (signal-trapping, `time.sleep` loop) launched the SAME way survived **>900s**
  with NI=0. The kill is **autoloop-workload-specific**, not generic.
- **H (zsh BG_NICE / nice=5)** — **FALSIFIED as cause.** The autoloop's NI=5
  came from zsh's default `BG_NICE` (backgrounded `&` jobs get nice +5); the
  idle probe launched from a bash script got NI=0. But
  `scripts/sprint-064-step2b-bash-launch.sh` launched the autoloop from bash
  (NI=0) and it **still died at ~2-3 min** — and took its bash parent with it
  (`zsh: terminated bash`). nice is not the cause.
- **H (dtrace `proc:::signal-send` to identify signal+sender)** — **BLOCKED by
  SIP.** `scripts/sprint-064-step2c-dtrace-signal.sh`: dtrace returns "probe
  description proc:::signal-send does not match any probes. System Integrity
  Protection is on." Not pursued further (disabling SIP is out of scope /
  high-risk).
- **H (process-group suicide) — CONFIRMED (root cause).** The repeated
  "bash parent dies with the autoloop" pattern pointed at process-group
  signaling. Code read of `applier.py` confirmed: `_spawn_spring` (lines
  ~391-430) spawns mvn with no `start_new_session` / `preexec_fn=os.setsid`,
  so mvn shares the autoloop's pgrp; `_terminate_process` (lines ~498-516)
  calls `os.killpg(os.getpgid(proc.pid), 15)` then `(…, 9)`. With mvn in the
  autoloop's group, that killpg kills the autoloop + (under a non-interactive
  bash launcher) the shell. Interactive zsh gives the backgrounded job its own
  pgrp, so sprint-063 saw only the silent python death, never the shell death.
- **H (mvn stdout PIPE deadlock) — latent, fixed (OQ-S64.1).**
  `scripts/sprint-064-step4b-spring-boot-timing.sh` + a pipe-capacity probe:
  Spring boots healthy in ~6s logging ~15.8KB; macOS pipe buffer is 64KB, so
  the PIPE does NOT block during boot. But nothing drains
  `backend_process.stdout`, so the eval phase (Step 7, many backend requests)
  would cross 64KB and freeze the JVM. Fixed proactively (needed to traverse
  Step 7 → Step 9).
- **H (in-loop health probe never connects) — CONFIRMED (OQ-S64.2).** After
  the killpg fix, the smoke iter survived but errored `SpringStartupTimeoutError
  … within 120s`. The captured in-loop Spring log (`spring-boot-<port>.log`,
  now persisted by the OQ-S64.1 fix) shows **Spring Started in 2.5s** then
  **no DispatcherServlet init** (no request ever reached it) until the 120s
  teardown. `scripts/sprint-064-step4c-spawn-repro.py` (exact spawn, urllib
  probe) booted healthy in 6.3s — so spawn mechanics are fine.
  `scripts/sprint-064-step4d-probe-env-test.sh`: `httpx(trust_env=True)` →
  empty body; `httpx(trust_env=False)` + `urllib` → 200 + `{"status":"UP"}`.
  `scutil --proxy` shows a macOS system proxy `127.0.0.1:7890` (HTTP/HTTPS/
  SOCKS) with ExceptionsList including `127.0.0.1`/`localhost`. httpx 0.28.1
  honors the system proxy but not the localhost bypass; urllib honors the
  bypass (which is why step4c's urllib worked). This is also why **sprint-063
  never got past Step 6**: every health probe was proxy-routed → 120s timeout
  → killpg suicide.

## §3 Fix delivered

All three fixes are in `autoloop/autoloop/sandbox/applier.py` (+35 / −8), under
NEW fence #20 (§5). No other code/config/doc-fenced path touched.

1. **OQ-S62.3 — `_spawn_spring` process-group isolation.** Added
   `start_new_session=True` to the mvn `subprocess.Popen`. mvn + JVM become a
   new session/pgrp; `_terminate_process`'s `os.killpg(os.getpgid(proc.pid),…)`
   now targets only the mvn subtree. Self-suicide eliminated.
2. **OQ-S64.1 — `_spawn_spring` stdout to a log file.** Replaced the undrained
   `stdout=subprocess.PIPE` with a per-port log file
   (`autoloop/results/spring-boot-<port>.log`, opened via a `with` so the
   parent fd closes while the child keeps its dup). Removes the 64KB pipe
   deadlock for both boot and the eval phase; preserves Spring output for
   post-mortem.
3. **OQ-S64.2 — `_probe_url_is_up` proxy bypass.** Replaced
   `httpx.get(url, …)` with `httpx.Client(trust_env=False).get(url, …)`. The
   localhost health probe no longer consults env/system proxy (or netrc/SSL)
   config; it connects directly to Spring. Confirmed by step4d
   (`trust_env=False` → 200/UP).

Comments at each edit cite the OQ id + the failure mode so a future reader
understands the non-obvious invariant (killpg relies on mvn being in its own
group; the probe must never be proxied).

## §4 Smoke iter Step 9 verification

`scripts/sprint-064-step4-smoke-iter.sh 3` (NI=0, clean process table, all 3
fixes live). First iteration reached Step 9 and the script stopped on success.

| field | value |
|---|---|
| iteration_id | exp-18 |
| wall time | **~71s** (full iter: analyze→propose→sandbox→cv→anti_hardcode→applier(branch+commit+Spring boot ~6s + health probe ~6s)→eval→tier_evaluator) |
| survived kill window | YES (no killpg suicide) |
| decision | `discard` (discard_reason `tier1_bad_cases_regression_5_to_0`) |
| `verdict.layer_results` | **non-null, 5 layers** |
| Layer 0 `tier0_safety` | passed=**True** (`tier0_safety_floor_clean`) |
| Layer 1 `tier1_outcome` | passed=**False** (`tier1_bad_cases_regression_5_to_0`) — drives the discard |
| Layer 2 `tier2_critical_flow` | passed=None (`not_evaluated_short_circuit_at_layer_1`) |
| Layer 3 `improvement_threshold` | passed=None (`not_evaluated_short_circuit_at_layer_1`) |
| Layer 4 `shadow_regression` | passed=None (`not_evaluated_short_circuit_at_layer_1`) |

**Gate-retirement judgment**: the original gate failure (M-Auto-1B Goal #3)
was that all 13 historical experiments.jsonl rows had `verdict=NULL` — the
substrate never reached Step 9. Now `tier_evaluator.evaluate` runs and produces
a structured 5-layer verdict that drives a real decision (discard on a genuine
Layer 1 bad_cases regression). The Layers 2-4 `passed=None` are the
tier_evaluator's **designed** short-circuit (once Layer 1 fails the decision is
made), not degeneracy — the layer entries themselves are present + non-null
with explicit reasons. **Gate #1 retired.**

**Nuance flagged for deliver-agent + human (final §5.6-style judgment)**: this
verdict short-circuited at Layer 1 (a discard-trajectory proposal). A verdict
that traverses all five layers with real pass/fail at 2-4 requires a
keep-trajectory proposal (one that does not regress bad_cases) — that is a
proposal-quality matter, exercised by S-Auto-10's overnight, not a substrate
matter. Also note Layer 0's java_gates sub-metrics are `status: skipped /
not produced by eval-interactive` (a known eval-interactive harness limitation,
pre-existing, not introduced here).

## §5 NEW fence #20 controlled override

- **Authorized**: 2026-05-31 via `AskUserQuestion` (recommended option:
  "Authorize fence #20 — apply start_new_session=True"). Follows the S-Auto-7.2
  "review后快速放宽通过" precedent (user direction 2026-05-30).
- **Surface**: `autoloop/autoloop/sandbox/applier.py` (previously fence #18
  FINALIZED at M-Auto-1C close). The prompt anticipated fence #20 on
  `cli.py` / new files; the actual root cause + fix live in `applier.py`, so a
  STOP-and-surface was raised and the human authorized the override.
- **Scope of the override**: the three narrow infra fixes in §3 — all in
  `applier.py`, all process-isolation / I/O-routing / connectivity (no semantic
  decision logic, no keyword/regex/enum/per-UC matrix). All three are within
  the fence #20 envelope (same file, same goal: enable reliable overnight
  execution). OQ-S64.1 + OQ-S64.2 were discovered during §4 verification and
  fixed under the same authorization; each was narrated to the human as it was
  applied.
- **§8.3 in-place revision**: M-Auto-2 §6 hard-fence list should be updated to
  add fence #20 (applier.py controlled override, S-Auto-9) post-hoc, parallel
  to the M-Auto-1C fence #19 precedent. (Deliver-agent action at close.)
- **§4.3 Codex implication**: per trigger #3 (touching a hard-fenced surface),
  the mid-sprint authorization **UPGRADES** S-Auto-9 Codex review from the
  milestone-shared default to **PER-SUB-SPRINT REQUIRED at S-Auto-9 close**.
  Codex should verify the three applier.py edits are infra-only (no semantic
  hardcode) against the §4.1 nine-question kernel.

## §6 OQs surfaced

- **OQ-S62.3** — **RESOLVED.** Root cause = killpg process-group suicide (NOT
  an OS/jetsam/launchd/TAL/memory kill, contra sprint-063 §8). Fixed via
  `start_new_session=True`. sprint-063 §8's enumerated hypotheses (caffeinate,
  screen, AC/lid, memory pressure, jetsam) are all FALSIFIED.
- **OQ-S64.1 (NEW, RESOLVED)** — undrained mvn `subprocess.PIPE` would deadlock
  the backend mid-eval at the 64KB pipe buffer. Fixed via per-port log file.
- **OQ-S64.2 (NEW, RESOLVED)** — macOS system proxy (`127.0.0.1:7890`)
  proxy-routed the httpx health probe away from localhost Spring (httpx honors
  system proxy but not its localhost ExceptionsList). Fixed via
  `trust_env=False`. **Operational note for S-Auto-10**: the proxy remains
  active on this Mac; the `trust_env=False` fix makes the autoloop robust to
  it, but any future localhost HTTP from the autoloop/eval path should likewise
  bypass the proxy.
- **OQ-S64.3 (NEW, observation; carry to S-Auto-10)** — full iterations now run
  in **~71s** end-to-end, so a 15-iter overnight may complete in ~15-20 min
  rather than hours. S-Auto-10 should re-scope the "6-8h budget" expectation
  and confirm the eval suite size / per-iter cost; also confirm lesson
  compaction (K=10) triggers within a short batch.
- **OQ-zsh-BG_NICE (NEW, minor, operational)** — zsh's default `BG_NICE`
  demotes `&` jobs to nice=5. Not the kill cause, but for overnight launches
  prefer a bash wrapper (or `unsetopt BG_NICE`) to keep NI=0.
- Carried (unchanged from sprint-063, S-Auto-10/M-Auto-2 scope):
  OQ-eval-judge-disabled, OQ-cv-ceiling-calibration-thin-evidence.

## §7 STOP-and-surface log

- **2026-05-31** — STOP-and-surface raised when the root-cause fix was found to
  require editing `applier.py` (fence #18 FINALIZED), beyond the prompt's
  anticipated fence #20 envelope (cli.py / new files). `AskUserQuestion`
  presented the root cause + the 1-kwarg fix + the fence implication; human
  authorized the fence #20 controlled override (recommended option). See §5.
- OQ-S64.1 + OQ-S64.2 fixes were applied under the same fence #20 authorization
  (same file, same goal), each narrated transparently to the human at the time
  of application rather than re-prompting per line (per the user's
  fast-iteration preference).

## §8 M-Auto-2 close-readiness checklist (S-Auto-9 contribution)

| milestone_objective §5 acceptance item | S-Auto-9 status |
|---|---|
| **OQ-S62.3 RESOLVED on local Mac** (S-Auto-9 close gate, line 310) | ✅ root cause diagnosed (killpg suicide) + fix delivered (3 applier.py fixes) + ≥1 full smoke iter through Step 9 with non-degenerate verdict (exp-18) |
| **Live iteration end-to-end through Step 9** (deferred gate #1, line 311) | ✅ RETIRED (exp-18, non-null 5-layer verdict) |
| **OQ-S62.3 expansion ROOT CAUSE diagnosed** (§5 item 6, line 213) | ✅ (a) mechanism identified (self-inflicted killpg, not OS) + (b) substrate unblocked + verified via smoke iter |
| Pre-batch baseline drift envelope (≥2 rerun) | ⏭ S-Auto-10 scope |
| First overnight batch ≥10 iters (deferred gate #2) | ⏭ S-Auto-10 scope (now feasible; ~71s/iter) |
| First human review of kept candidates (deferred gate #3) | ⏭ S-Auto-10 scope |
| First cherry-pick decision (deferred gate #4) | ⏭ S-Auto-10 scope |
| R-S58 reopen check | ⏭ S-Auto-10 overnight propose-distribution scan |
| Codex per-sub-sprint review (fence #20 → §4.3 trigger #3) | ⏳ REQUIRED at S-Auto-9 close (see §5) |

**Hard-gate baselines this sub-sprint**: autoloop pytest 266 PASS; 17-fixture
detector 31 PASS; scoring SHA reasserted (`drift=[]`); Java zero-touch;
eval_interactive 486 passed / 3 failed (unchanged). Hard-fence diff = only the
authorized fence #20 surface (`applier.py`).

## End of handoff
