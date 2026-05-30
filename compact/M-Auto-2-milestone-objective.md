---
title: Milestone M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-30
review_cadence: per milestone
supersedes: [docs/milestones/M-Auto-1C_objective.md]
superseded_by: null
notes: >
  M-Auto-1C closed 2026-05-30 with Class C in-flight downgrade per
  joint deliver-agent + human AskUserQuestion 2026-05-30 (recommended
  option: close now; OQ-S62.3 + overnight → M-Auto-2). M-Auto-1C
  inherited 4 unmet hard gates that all trace to the OQ-S62.3
  expansion blocking overnight execution at OS-level kill (2-5 min
  post Step 6 mvn spawn across 9 launch attempts spanning every
  escape route tried — Claude Code Bash + user iTerm + Python
  wrapper with subprocess.Popen(start_new_session=True) + GNU screen
  detached + caffeinate with all 3 sleep-prevention assertions
  confirmed + AC power + 7.5GB free RAM). M-Auto-2 resolves
  OQ-S62.3 in S-Auto-9 (local-Mac in-env diagnostic instrumentation
  per human direction NO cloud / remote server) then exercises the
  now-reliably-executable substrate end-to-end in S-Auto-10 (the
  original M-Auto-1C deferred scope — first overnight + first human
  review + first cherry-pick to main; ~3-5 days).

  This is the NINTH milestone under `iteration_governance.md` §8
  framework. Sub-sprint sequence is preliminary 2-3 sub-sprints
  (S-Auto-9 OQ-S62.3 + S-Auto-10 overnight + cherry-pick + optional
  S-Auto-11 fix-iteration buffer within §8.5 5-sub-sprint ceiling
  margin = 2).

  **Core thesis**: M-Auto-1C ships substrate-validation + drift
  envelope + R-S58 baseline-scan + cv calibration tuning as recorded
  artefacts; the missing piece is a working overnight execution
  environment on the local Mac. M-Auto-2 resolves the
  substrate-execution-environment problem first (S-Auto-9
  ~3-5 days; in-env OS-level instrumentation: dtrace process_exit
  hook, taskpolicy inspection, Console.app crash report review,
  launchd / EDR / Background Activity Manager investigation,
  alternate detachment patterns) then executes the original
  M-Auto-1C deferred scope (S-Auto-10 ~3-5 days; first overnight
  batch ≥10 iterations; first §5.6 manual review; first cherry-pick
  to main via AskUserQuestion). M-Auto-2 acceptance bar inherits the
  4 deferred M-Auto-1C gates: live iter end-to-end through Step 9;
  first overnight ≥10 iterations; first human review of kept
  candidates; first cherry-pick decision via AskUserQuestion.

  **Why local-Mac scope discipline**: human-locked direction 2026-05-30
  "我需要在 local Mac 上面来执行这个 overnight 的 autoloop，不要找这种
  cloud 或者 remote server" + "这个原因到底为什么不能成功执行？这个确实
  是要调研一下" + "希望这个 Milestone 把 autoloop 跑起来，能够正常
  cherry-pick. 先把流程走通". Translation: develop overnight execution
  capability on local Mac (NOT cloud / remote); diagnose the actual
  root cause of OQ-S62.3 expansion (NOT just route around it); get
  the auto-evolution loop running end-to-end through cherry-pick
  before considering Stage-2 unlocks. The local-Mac constraint
  preserves the M-Auto-1A-onward substrate assumption that the loop
  runs on the developer's actual workstation (consistent with the
  S-Auto-7.1 `--auto-reboot` ergonomics + foreground :8080 backend
  pattern + the cumulative human-in-the-loop iteration mode).

  **Why two-or-three sub-sprints**: 2 baseline + 1 optional
  fix-iteration buffer fits comfortably within the §8.5
  5-sub-sprint ceiling; margin = 2-3 for fix-iteration if either
  surfaces second-order substrate brittleness. The clean substrate
  resolution (S-Auto-9) → exercise (S-Auto-10) decomposition mirrors
  M-Auto-1C's intended S-Auto-7.2 (substrate fix) → S-Auto-8
  (overnight + cherry-pick) flow that the original 2-sub-sprint
  plan was meant to deliver; M-Auto-1C's actual delivery was
  substrate-validation only (overnight blocked by OQ-S62.3), and
  M-Auto-2's tighter scope reflects the resolved-substrate-validation
  state inherited from M-Auto-1C.

  **§8.1 conformance**: M-Auto-2 has 2-3 sub-sprints within 3-5
  ceiling.

  **Codex review plan (§4.3)**: milestone-shared default. S-Auto-9
  is expected pure-infra (substrate-environment diagnostic +
  resolution) so §4.3 trigger #2 (§1.7 forbidden-list cross) NOT
  expected unless the resolution touches a hard-fenced surface beyond
  the M-Auto-1C §6 inheritance. S-Auto-10 is `eval_spec` semantic-
  touching via cherry-pick if it lands; same trigger conditions as
  M-Auto-1C S-Auto-8 apply (per-sub-sprint Codex pre-milestone-close
  ONLY IF cherry-pick candidate borderline-§5.3 surfaces). If
  S-Auto-9 dev surfaces substrate brittleness requiring NEW fence
  touch beyond fence #18 + #19 envelope, dev STOP-and-surfaces;
  authorization + Codex upgrade per §4.3 trigger #3 follows
  S-Auto-7.2 precedent (fast-iteration mode per user direction
  2026-05-30 "像那种什么之前的什么 Content-length Validator，可以我
  review后快速放宽通过").

  **R-item coupling (consumed)**: M-Auto-2 consumes the **OQ-S62.3
  expansion** (substrate-execution-environment kill on local Mac at
  2-5 min mark after Step 6 mvn spawn; investigated across 9 launch
  attempts at S-Auto-8; cause not yet diagnosed in-session;
  resolution candidates: dtrace process_exit hook / taskpolicy
  inspection / Console.app crash report deep-dive / Apple system log
  review / alternate detachment patterns including launchd-managed
  plist / `at` job / cron). Resolution at S-Auto-9 close = working
  overnight execution capability on local Mac. NOT a formalized
  R-item per deliver-agent + human discretion at M-Auto-2 open
  (carry-over OQ from sprint-063 handoff §8).

  **R-item coupling (read-only awareness)**: `R-bad-case-parallel-
  session-establishment-flakiness` (bad_cases stays parallel=1;
  overnight batch may further accumulate data points);
  `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` (natural overnight
  optimization target); `R-shadow-fixture-empty-form-session-create-
  400` (cs59s01 / cs59s02 stays excluded); `R-eval-interactive-
  judge-score-never-populated` (M-Auto-2+ observability hygiene LOW
  priority; loader-counted `case_passed` continues as canonical signal
  during S-Auto-10 manual review; potentially in-scope for M-Auto-2
  separately if human prioritizes observability fix); `R-S58-anti-
  hardcode-zero-width-when-arrow-bypass` CLOSED-AS-THEORETICAL-ONLY
  at M-Auto-1C close (reopen if S-Auto-10 overnight propose-
  distribution scan surfaces any Cf observation).

  **Bad-case cherry-pick reference signals UNCHANGED from M-Auto-1C**:
  cs001_uc_c_mechanical_template_escalate (resolve_faq_grounded_answer
  .yaml procedure / critical_steps[*].desc gating `goal_impossible`
  terminal-state on policy-mandated `search_knowledge` completion) +
  wmkb_uc_a_trader_flag_secondary_uc_h (resolve_intake_collect_and_
  handover.yaml critical_steps[*].desc gating handover on intake
  completion). REFERENCE SIGNALS for S-Auto-10 manual review; meta-
  agent owns proposal authorship; cherry-pick decision via
  AskUserQuestion on actual overnight kept-candidate slate.

  **What NOT in M-Auto-2 scope** (deferred):
  - Stage-2 unlock (templates.yaml / system_prompt.txt) — decision
    point AFTER M-Auto-2 close (same deferral as M-Auto-1B + M-Auto-1C);
    evaluated against cumulative M-Auto-1B + M-Auto-1C + M-Auto-2
    evidence (≥3 kept human-approved cherry-picks + 0 borderline
    §5.3 cases would be a positive signal; M-Auto-2 itself adds at
    most 1 cherry-pick to the cumulative count).
  - M3-B Single Handover Orchestrator P0 — deferred per human's
    M-Auto-1-first decision; re-evaluated at M-Auto-2 close.
  - Projection-hygiene milestone candidate (M5 carry-over) —
    independent track.
  - UC-G/H/I/J bad-case seeding (`R-bad-case-suite-uc-ghij-seed-
    from-real-sessions`) — natural optimization target once seeded;
    not pre-seeded.
  - `R-eval-java-module-retirement` — M-Auto-2+ governance-hygiene
    (CONDITIONAL: included if human prioritizes; otherwise carry to
    next planning round).
  - Cloud / remote-server overnight execution — EXPLICITLY OUT OF
    SCOPE per human direction 2026-05-30 (M-Auto-2 stays local-Mac).
  - Deeper substrate fixes for Flyway lock contention / in-memory DB
    / Docker sandbox — M-Auto-2+ as observation (pre-flight from
    S-Auto-7.1 + applier mvn fix from S-Auto-7.2 + OQ-S62.3 resolution
    from S-Auto-9 accept current local-Mac substrate ergonomic).
---

# Milestone M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick

## 1. Milestone class

**Multi-layer milestone, 2-3 coordinated sub-sprints.** Layer + §7-stanza + Codex breakdown:

| Sub-sprint | Layer (primary) | §7 stanza | Codex review |
|---|---|---|---|
| S-Auto-9 / Sprint 064 — OQ-S62.3 local-Mac in-env diagnostic + resolution | `infra` (substrate-environment diagnostic + resolution; OS-level instrumentation; no semantic decision change; no projection / scoring semantic logic edit) | EXEMPT (pure-infra carve-out; self-walked for paper-trail) | Milestone-shared (default) UNLESS dev surfaces substrate brittleness requiring NEW fence touch beyond M-Auto-1C §6 17+2 inheritance — then dev STOP-and-surfaces; per-sub-sprint Codex per §4.3 trigger #3 follows S-Auto-7.2 fast-iteration precedent |
| S-Auto-10 / Sprint 065 — First overnight batch + first human review + first cherry-pick to main (resumes M-Auto-1C original scope) | `eval_spec` (per-iteration fitness on now-reliably-executable substrate; cherry-pick decision via AskUserQuestion + §5.6 manual review) | REQUIRED | Milestone-shared (default) UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint per §4.3 trigger #2 pre-milestone-close) |
| (Optional) S-Auto-11 / Sprint 066 — Fix-iteration buffer | TBD per actual surfaced findings | TBD | TBD per §4.3 triggers |

S-Auto-9 estimated ~3-5 days (1-2 days OS-level investigation including dtrace / taskpolicy / Console.app / launchd review + 1-2 days fix iteration + smoke iter through Step 9 verification via the resolved overnight-execution path). S-Auto-10 estimated ~3-5 days (1-2 days execution including ≥2 baseline rerun drift envelope per inherited Codex M-Auto-1B Axis M6 trigger #1 acceptance + 1 overnight 6-8h + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply + close-bundle). Total milestone ~1.5-2 calendar weeks assuming sub-sprints execute sequentially.

## 2. Goal

Finish what M-Auto-1C substrate-validated but didn't execute end-to-end. At M-Auto-2 close, the M-Auto-1A/B/C substrate has been demonstrated to:

1. **Smoke iter through Step 9 with non-degenerate `tier_evaluator_verdict`** (Layer 0-4 all non-degenerate) via the now-reliably-executable overnight path on local Mac. S-Auto-9 retires M-Auto-1C §12.4 deferred gate #1 (live iter end-to-end through Step 9). The 14-step state machine executes against the real meta-agent LLM end-to-end without OS-level kill at Step 7 eval_runner subprocess.
2. **Pre-batch baseline drift envelope established (≥2 rerun)** at S-Auto-10 BEFORE overnight kick-off, per inherited Codex M-Auto-1B Axis M6 downgrade trigger #1 (the M-Auto-1C S-Auto-8 baseline rerun was 0/46 drift; M-Auto-2 S-Auto-10 third reference point further validates the envelope). If drift exceeds 10/34 cases (~30%; 2× the M-Auto-1A close-day signature), HALT + deliver-agent + human jointly investigate.
3. **First overnight batch executed** at S-Auto-10: 10-20 iterations (target 10-20; final count ≥10 is the gate); 6-8h budget; per-iteration verdicts serialized to `autoloop/results/runs/exp-<N>/` + `experiments.jsonl` + `iterations.sqlite` + `lessons.md` (first K=10 lesson compaction triggers automatically). S-Auto-10 retires M-Auto-1C §12.4 deferred gate #2.
4. **First human review of overnight kept candidates** via §5.6-style joint deliver-agent + human reading of per-turn traces; PASS / FAIL / IMPROVING / borderline-§5.3 classification recorded per candidate; cherry-pick-eligible / deferred-to-M-Auto-3+ / discarded classification recorded. Retires M-Auto-1C §12.4 deferred gate #3.
5. **First cherry-pick decision via AskUserQuestion**: human selects EXACTLY ONE candidate to cherry-pick to main OR 0 candidates with explicit "no human-approved candidate" justification. If cherry-pick lands: `python -m autoloop apply --experiment exp-<N>` Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit per OQ-S55.1; human manually commits with standard deliver-agent footer); `config.fitness.baseline_dir` advances past the cherry-pick commit so the next milestone's baseline is post-cherry-pick. Retires M-Auto-1C §12.4 deferred gate #4.
6. **OQ-S62.3 expansion ROOT CAUSE diagnosed** on local Mac. S-Auto-9 close = (a) the OS-level kill mechanism is identified (e.g., specific signal + originating subsystem) + (b) the autoloop substrate (S-Auto-7.2 fixes UNCHANGED) is unblocked for reliable overnight execution OR (c) a documented operational workaround that reliably works on local Mac (e.g., specific launchd plist / `at` job / process priority adjustment / system setting toggle) is delivered + verified via at least one full smoke iter through Step 9.
7. **R-S58 reopen check** at M-Auto-2 close: if S-Auto-10 overnight propose-distribution scan surfaces any Cf-char observation (per R-S58 reopen condition documented in `docs/action_bank.md` §5.2), re-promote R-S58 to ACTIVE and route to a Fix-D-style follow-on sub-sprint (any candidate from implementation paths (i)/(ii)/(iii) per R-S58 entry). If 0 Cf observations across S-Auto-10 overnight (consistent with M-Auto-1C baseline 0/13), confirm CLOSED-AS-THEORETICAL-ONLY.

**What ships in main at M-Auto-2 close**:

- Zero `server/src/main/java/` / `eval/src/main/java/` / `eval_interactive/eval_interactive/**` / case_spec / case_specs_shadow / `server/src/main/resources/{prompts,scripts,config,mock}/**` edits (these stay byte-identical to M-Auto-1C close on the agent-loop scope).
- S-Auto-9 substrate-environment resolution: typically NO autoloop code edit (the substrate fix is environmental — e.g., a `launchctl` configuration, a `defaults write` system setting, a documented Console.app finding leading to a one-time OS-level workaround). IF S-Auto-9 requires an autoloop code edit (e.g., the resolution involves wrapping the orchestrator in a launchd-managed process), that edit lives under a NEW fence override (#20) authorized at S-Auto-9 open via AskUserQuestion, parallel to M-Auto-1C §6 fence #18 + #19 precedent.
- Optionally (S-Auto-10 cherry-pick): exactly ONE Skill YAML LLM-soft field edit on `server/src/main/resources/skills/*.yaml` (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc only — sandbox-validated, anti-hardcode PASS, gaming 0-ERROR; human-approved via AskUserQuestion + §5.6 manual review; human-committed).
- Auto-loop result accumulation under `autoloop/results/runs/exp-*/`, `experiments.jsonl`, `iterations.sqlite`, `lessons.md` (first K=10 compaction lesson triggers during overnight); the configured `baseline_dir` advances IF cherry-pick lands.

**Dataset scope (v1 unchanged from M-Auto-1A/B/C)**: per-iteration fitness suite = `bad_cases` (12) + `anchor_outcome` (12) + `shadow` (22 — `_manifest.yaml` filtered) = 46 cases per iter, expected ~12-15 min per iteration. `anchor` 159 NOT consulted as per-iteration fitness; M-Auto-2 does NOT extend this scope.

**Layer 0 scope (v1 unchanged from M-Auto-1A/B/C)**: Python `hard_checks` Tier-0 family. The legacy `eval/src/main/java/com/gumtree/csagent/eval/` Java module remains superseded.

M-Auto-2 does NOT widen the mutable surface beyond Skill YAML LLM-soft fields (Stage-1 only). Stage-2 entry decision (templates.yaml / system_prompt.txt unlock) is reserved for a SEPARATE milestone AFTER M-Auto-2 close, judged against the evidence accumulated during M-Auto-1B + M-Auto-1C + M-Auto-2.

## 3. Sub-sprint sequence

### S-Auto-9 / Sprint 064 — OQ-S62.3 local-Mac in-env diagnostic + resolution (NEXT)

**Layer:** `infra` (substrate-environment diagnostic + resolution; OS-level instrumentation; structural; no semantic decision change). **§7 stanza:** EXEMPT (pure-infra carve-out self-walked for paper-trail). **Codex:** Milestone-shared default UNLESS dev STOP-and-surfaces with substrate brittleness requiring NEW fence touch (then per-sub-sprint per §4.3 trigger #3 fast-iteration). **Estimated dev:** ~3-5 days.

**Scope (4-5 steps):**

1. **Diagnostic investigation** (~1-2 days; fast-iteration STOP-and-surface authorized per S-Auto-7.2 precedent):
   - `dtrace -n 'proc:::exit /pid == <autoloop-PID>/ { printf("%s %d %s\n", execname, pid, ustack()); }'` on a fresh overnight attempt to catch the kill signal + ustack.
   - `sudo taskpolicy -p <autoloop-PID>` inspection during the running orchestrator to identify any background-activity / power-saving / focus-mode policy applied.
   - Console.app filter on `process:python` AND `subsystem:com.apple.kernel` OR `subsystem:com.apple.launchd` OR `subsystem:com.apple.WorkloadOptimization` during overnight attempt to surface kill-source.
   - `log show --predicate 'eventMessage CONTAINS "memorystatus" OR eventMessage CONTAINS "TAL" OR eventMessage CONTAINS "WorkloadOptimization"' --start "<launch-time>" --end "<death-time>"` to widen jetsam search.
   - `pmset -g` + `pmset -g assertions` + `pmset -g log` cross-reference at death time.
   - Process-priority inspection (`ps -o pri,nice,pid,user,command -p <autoloop-PID>` during runtime).
   - macOS Background Activity Manager (TAL) / launchd ServiceManagement plist review for any policy applicable to long-running user-spawned processes.
2. **Hypothesis + fix iteration** (~1-2 days):
   - Based on §1 diagnostic evidence, identify the OS-level kill subsystem + signal.
   - Apply fix candidate (priority order: alternate detachment pattern → launchd-managed plist → process-priority adjustment → system setting toggle → autoloop substrate code edit IF environmental fix insufficient).
   - If autoloop code edit required (e.g., wrapping the orchestrator in a `launchd` user-agent plist that survives parent termination), open NEW fence #20 controlled override at S-Auto-9 mid-sprint via AskUserQuestion (S-Auto-7.2 precedent for fast-iteration authorization on substrate brittleness).
3. **Smoke iter verification** (~half-day):
   - Run `python -m autoloop run --experiments 1` via the now-resolved overnight execution path.
   - Verify `experiments.jsonl` row captured with non-degenerate `tier_evaluator_verdict` (Layer 0-4 all non-null).
   - This RETIRES M-Auto-1C §12.4 deferred gate #1 (live iter end-to-end through Step 9).
4. **OQ ledger update + handoff** (~half-day):
   - Document the root cause + fix path + verification evidence.
   - Update `docs/sprints/sprint-064-handoff.md` §0-§N per dev prompt format.
5. **STOP conditions**: if 1-2 days of diagnostic investigation produces NO actionable hypothesis, dev STOP-and-surfaces for deliver-agent + human re-plan (e.g., reconsider alternate-env decision OR open a deeper substrate research-agent track).

**Files in scope** (S-Auto-9 expected; may expand per actual surfaced findings):

- **NEW**: `docs/sprints/sprint-064-{objective,handoff}.md`.
- **NEW (CONDITIONAL on autoloop code edit)**: NEW fence #20 controlled override surface (TBD per actual fix; e.g., `autoloop/autoloop/runner.py` launchd-wrapper helper OR `~/Library/LaunchAgents/com.user.autoloop.plist` template + autoloop CLI invocation pattern).
- **NEW (likely)**: diagnostic scripts (e.g., `scripts/dtrace-autoloop-exit.d` + `scripts/diagnose-oq-s62.3.sh`) under pure-infra carve-out; not load-bearing.
- **NO touch** to all hard-fence surfaces per M-Auto-2 §6 (inheriting M-Auto-1C §6 17+2 with fence #18 + #19 FINALIZED).

### S-Auto-10 / Sprint 065 — First overnight batch + first human review + first cherry-pick to main — AFTER S-Auto-9

**Layer:** `eval_spec` (consumes per-iteration fitness verdict sequence on now-reliably-executable substrate; §5.6 manual review + cherry-pick are eval-side acceptance bars). **§7 stanza:** REQUIRED (semantic-touching via cherry-pick if it lands; same as M-Auto-1C S-Auto-8 framing). **Codex:** milestone-shared (default) at M-Auto-2 close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint Codex pre-milestone-close per §4.3 trigger #2). **Estimated dev:** 1-2 dev-days execution + 1 overnight (6-8h auto-loop) + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply + close-bundle.

**Scope (6 sentences) — UNCHANGED from M-Auto-1C S-Auto-8 (resumes original scope on now-reliably-executable substrate):**

1. **Pre-batch baseline rerun ≥2** (inherited Codex M-Auto-1B Axis M6 acceptance; third reference point beyond M-Auto-1C S-Auto-8 §3 + close-day rerun).
2. **Overnight batch** `python -m autoloop run --experiments 15` via the S-Auto-9-resolved overnight execution path.
3. **Propose-distribution scan for R-S58 reopen check**.
4. **§5.6 manual review** of kept candidates with cs001 + wmkb reference signals.
5. **AskUserQuestion cherry-pick decision** + apply Hybrid + handoff.
6. **Close-bundle**: final observations + R-S58 disposition (closed-as-theoretical-only confirmed OR re-promoted to ACTIVE) + Stage-2 entry decision direction for M-Auto-3+ planning.

**Files in scope** (S-Auto-10 same as M-Auto-1C S-Auto-8 scope; unchanged from M-Auto-1C §3 S-Auto-8 scope listing).

### (Optional) S-Auto-11 / Sprint 066 — Fix-iteration buffer

Reserved within §8.5 5-sub-sprint ceiling for fix-iteration if S-Auto-9 OR S-Auto-10 surfaces second-order substrate brittleness requiring a follow-on sub-sprint. NOT pre-scoped; deliver-agent + human dispatch at S-Auto-10 close if needed.

## 4. Non-goals (explicit)

- M-Auto-2 does NOT unlock Stage-2 mutable surface (templates.yaml / system_prompt.txt / routing_prompt.txt). Stage-2 entry is a SEPARATE milestone decision after M-Auto-2 close.
- M-Auto-2 does NOT introduce a new Tier-0 invariant.
- M-Auto-2 does NOT touch the Single Handover Orchestrator (M3-B P0; deferred per human's M-Auto-1-first decision; re-evaluated at M-Auto-2 close).
- M-Auto-2 does NOT modify `docs/runtime_freeze_and_risk_policy.md`, `docs/foundational/**`, `docs/current/iteration_governance.md`, `docs/teams/**`.
- M-Auto-2 does NOT modify `eval_interactive/eval_interactive/**` (loader.py controlled override from M-Auto-1B S-Auto-7 stays FINALIZED), `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`.
- M-Auto-2 does NOT modify `eval/src/main/java/**`.
- M-Auto-2 does NOT modify the four `autoloop/autoloop/scoring/` baseline files (locked against M-Auto-1B S-Auto-7 close content hash `22548e20…`).
- M-Auto-2 does NOT replace the §5.6 bad-case manual-review human-judgment gate.
- M-Auto-2 does NOT promote any gaming check from observation-only to gating.
- M-Auto-2 does NOT widen the per-iteration fitness suite beyond 46 cases.
- M-Auto-2 does NOT replace Codex anti-hardcode review.
- M-Auto-2 does NOT pre-seed new bad cases.
- M-Auto-2 does NOT alter shadow-firewall posture.
- M-Auto-2 allows AT MOST 1 cherry-pick to main (S-Auto-10 only; S-Auto-9 is substrate-environment fix with no cherry-pick scope).
- M-Auto-2 does NOT pursue cloud / remote-server overnight execution (EXPLICITLY OUT OF SCOPE per human direction 2026-05-30; local-Mac only).
- M-Auto-2 does NOT close `R-eval-interactive-judge-score-never-populated` unless human explicitly prioritizes (otherwise LOW priority M-Auto-3+).

## 5. Milestone acceptance bar

**Hard gates (close decision is PASS only if all clear):**

- [ ] **Tier-0 safety floor unchanged**: M-Auto-2 ships zero `server/src/main/java/` / `eval/src/main/java/` / Python eval-code edits (apart from S-Auto-10 conditional cherry-pick on Skill YAML LLM-soft field). Verified by `git diff --stat <M-Auto-1C-close>..<M-Auto-2-close>` against gated prefixes returning empty (or only the conditional Skill YAML row).
- [ ] **Java test baseline preserved**: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED from M-Auto-1C close.
- [ ] **Python test baseline preserved**: eval_interactive `486 passed, 3 failed` UNCHANGED. `autoloop/tests/` typically grows by 0-N new tests at S-Auto-9 (diagnostic-script tests if applicable; conditional fence #20 helper tests if applicable); S-Auto-10 typically adds 0 new tests.
- [ ] **OQ-S62.3 RESOLVED on local Mac** (S-Auto-9 close gate): root cause diagnosed + fix delivered + at least one full smoke iter through Step 9 captures non-degenerate `tier_evaluator_verdict`. Recorded in `docs/sprints/sprint-064-handoff.md`.
- [ ] **Live iteration end-to-end through Step 9** (M-Auto-1C deferred gate #1): retired at S-Auto-9 close via smoke iter verification.
- [ ] **Pre-batch baseline drift envelope at S-Auto-10 open ≥2 reruns**: median + IQR per-suite envelope established; halt if drift exceeds 10/34 cases.
- [ ] **First overnight batch executed at S-Auto-10**: ≥10 iterations completed end-to-end (target 10-20; final count ≥10 is the gate).
- [ ] **First human review of overnight kept candidates** (M-Auto-1C deferred gate #3): deliver-agent + human jointly read per-turn traces; PASS / FAIL / IMPROVING judgment recorded per candidate.
- [ ] **First cherry-pick decision via AskUserQuestion** (M-Auto-1C deferred gate #4): human selects exactly ONE candidate OR 0 with explicit justification; if cherry-pick lands, apply Hybrid + human manual commit successful + `git log main` shows new commit.
- [ ] **Curated bad-case suite manual review pass (PRIMARY GATE per §5.6)**: deliver-agent + human run the bad-case suite at M-Auto-2 close.
- [ ] **Shadow regression-safety gate** (parity with M-Auto-1B/C): no NEW shadow regression beyond `R-shadow-fixture-empty-form-session-create-400`; if cherry-pick landed, shadow drop ≤3%.
- [ ] **R-S58 final disposition reconfirmed**: if overnight propose-distribution scan surfaces 0 Cf observations, CLOSED-AS-THEORETICAL-ONLY confirmed; if ≥1 Cf observation, re-promote to ACTIVE + route to Fix-D follow-on (potentially S-Auto-11).
- [ ] **Milestone-shared Codex review at M-Auto-2 close**: `pass / 0` (or `approve with downgrade-to-signal follow-up`) over cumulative range.

**Observation-only (recorded; does not gate close):**

- Per-iteration elapsed-time average across overnight.
- `shadow_disagreement_rate` (§6 architecture-health metric) measurement.
- Cumulative FLAG rate across overnight propose-stage anti-hardcode checks.
- Gaming flag counts + severity distribution.
- Lessons compaction: `autoloop/results/lessons.md` should contain ≥1 LLM-distilled lesson if overnight reaches K=10.
- `new_semantic_hardcode_count` (§6) = **0** (S-Auto-9 substrate-environment fix; S-Auto-10 cherry-pick passes anti-hardcode detector).
- `soft_signal_conversion_count` (§6): if cherry-pick lands + edit downgrades a hardcoded behaviour to a soft signal, count as 1; otherwise 0.

## 6. Hard fences (milestone-level)

M-Auto-2 inherits M-Auto-1C §6 17+2 hard fences with the loader.py + eval_runner.py + applier.py + content_validator.py controlled overrides ALL FINALIZED. Most fences unchanged; the conditional fence #20 (NEW) is enumerated below.

1-17 (same as M-Auto-1C §6 fences 1-17 verbatim — UNCHANGED).
18. **APPLIER.PY FENCE #18 FINALIZED**: `autoloop/autoloop/sandbox/applier.py` byte-identical to M-Auto-1C S-Auto-7.2 close (post-OQ-S61.1 + OQ-S62.2 substrate fixes). S-Auto-9 + S-Auto-10 MUST NOT edit.
19. **CONTENT_VALIDATOR.PY FENCE #19 FINALIZED**: `autoloop/autoloop/sandbox/content_validator.py` byte-identical to M-Auto-1C S-Auto-7.2 close (post-OQ-S62.1 rule 3 rewrite). S-Auto-9 + S-Auto-10 MUST NOT edit. `autoloop/config.yaml` `length_overflow_absolute_ceiling` knob remains TUNABLE (currently 1200 post-S-Auto-8; future overnight evidence may surface further calibration).
20. **CONDITIONAL NEW FENCE (S-Auto-9 ONLY; authorized at S-Auto-9 mid-sprint via AskUserQuestion if needed)**: if OQ-S62.3 resolution requires an autoloop code edit beyond environmental fixes, authorize NEW fence #20 controlled override on the specific file(s) touched. Authorization follows S-Auto-7.2 precedent (fast-iteration mode per user direction "可以我review后快速放宽通过"); per §4.3 trigger #3 the mid-sprint authorization UPGRADES Codex review plan to PER-SUB-SPRINT REQUIRED at S-Auto-9 close. S-Auto-10 MUST NOT edit the fence #20 surface.

### 6.1 OQ-S56.1 disposition (inherited UNCHANGED)

The blessing for `eval_interactive/eval_interactive.yaml` env-var indirection carries forward UNCHANGED.

## 7. R-items consumed / surfaced

**Consumed by M-Auto-2 (closed at close)**:

- **OQ-S62.3 expansion** (carry-over from sprint-063 handoff §8) — S-Auto-9 diagnosis + resolution. M-Auto-2 close = OS-level kill mechanism identified + reliable overnight execution capability delivered.
- **M-Auto-1C deferred gates #1-#4** (live iter Step 9 + first overnight + first review + first cherry-pick) — S-Auto-9 retires gate #1; S-Auto-10 retires gates #2-#4.
- **R-S58 final disposition reconfirmed** (or re-promoted if Cf observation surfaces during S-Auto-10 overnight).

**Coupled (read-only awareness; not consumed)**:

- `R-bad-case-parallel-session-establishment-flakiness` (M5-close priority-bumped) — bad_cases stays parallel=1 at S-Auto-10 baseline reruns + close-day.
- `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` — natural S-Auto-10 overnight optimization target.
- `R-bad-case-suite-uc-ghij-seed-from-real-sessions` — NOT pre-seeded.
- `R-shadow-fixture-empty-form-session-create-400` — cs59s01/cs59s02 stays excluded.
- `R-eval-java-module-retirement` — M-Auto-3+ governance-hygiene; NOT consumed.
- `R-eval-interactive-judge-score-never-populated` — observability hygiene; LOW priority; NOT consumed by S-Auto-9 / S-Auto-10 (unless human explicitly prioritizes at planning round).
- M5 carry-over projection-hygiene candidate (#4 C3 dedup + OQ-S52.4) — independent milestone candidate; NOT consumed.

**Surfaced by M-Auto-2 (expected)**:

- Per-iteration elapsed-time observation if average overnight elapsed >40 min.
- Any new bypass surfaced by S-Auto-9 / S-Auto-10 Codex review beyond R-S58.
- `shadow_disagreement_rate` first measurement (§6).
- Any gaming check ERROR-severity hits during overnight.
- Any new bad case discovered during S-Auto-10 manual review.
- Lessons_compactor LLM-distilled lesson surface — if K=10 lesson reveals a meta-prompt opportunity.
- Possible NEW fence #20 controlled override per S-Auto-9 actual surfaced findings.
- Possible OS-level workaround documentation (e.g., a `launchd` plist + invocation runbook) as a new substrate ergonomic artefact.

**NOT consumed by M-Auto-2 (intentionally deferred)**:

- M3-B Single Handover Orchestrator P0 — release_gate.md §1.1 blocker; remains in candidate slate; deliver-agent + human re-evaluate at M-Auto-2 close.
- M5 carry-over projection-hygiene candidate — independent track.
- All other open R-items in `docs/action_bank.md` §5 unrelated to auto-evolution-loop overnight execution + first cherry-pick.

## 8. Codex review plan (per §4.3)

**Default**: milestone-shared review at M-Auto-2 close. Single cumulative Codex pass over the commit range covering S-Auto-9 + S-Auto-10 + optional cherry-pick commit + optional S-Auto-11. Codex consumes:

- Both / all sub-sprint objectives + handoffs.
- This milestone objective (live during execution; archived to `docs/milestones/M-Auto-2_objective.md` at close).
- The cumulative commit range produced by all sub-sprints + cherry-pick (if any).
- The Python test baseline reproducibility check + S-Auto-9 substrate diagnostic evidence + S-Auto-10 overnight result artefacts.
- The bad-case suite manual review notes from the M-Auto-2 close run + shadow rerun result.
- The propose-distribution scan evidence for R-S58 reopen check.
- The pre-batch baseline drift envelope evidence per inherited Codex M-Auto-1B trigger #1 acceptance.

**Per-sub-sprint trigger expectations**:

- **S-Auto-9**: CONDITIONAL per §4.3 trigger #3 ONLY IF dev mid-sprint surfaces substrate brittleness requiring NEW fence #20 controlled override (e.g., autoloop code edit beyond environmental fix). Otherwise milestone-shared default.
- **S-Auto-10**: conditional per §4.3 trigger #2 ONLY IF cherry-pick candidate touches §1.7 borderline. Otherwise milestone-shared default.
- **S-Auto-11** (optional fix-iteration): TBD per actual surfaced findings.

**Verdict set** (§4.1): `approve` / `approve with downgrade-to-signal follow-up` / `reject as semantic hardcode` / `needs human architecture decision`.

## 9. Estimated milestone duration

**Calendar estimate (informational; not a gate):**

- S-Auto-9 / Sprint 064: ~3-5 days (1-2 days OS-level investigation + 1-2 days fix iteration + smoke iter verification + handoff).
- S-Auto-10 / Sprint 065: 1-2 dev-days execution + 1 overnight (6-8h) + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply + close-bundle.
- (Optional) S-Auto-11: 1-3 days if needed.
- M-Auto-2 close: deliver-agent + human bad-case manual review + shadow rerun + milestone-shared Codex review + close-out artefacts ~1-2 days.

**Total**: ~1.5-2 calendar weeks for the full milestone, assuming sub-sprints execute sequentially, S-Auto-9 diagnostic produces actionable hypothesis within 1-2 days, milestone-shared Codex returns `pass` on first pass, and no overnight halt-trigger fires at S-Auto-10.

Risk to duration:
- S-Auto-9 OS-level investigation cannot produce actionable hypothesis within 1-2 days → STOP-and-surface; deliver-agent + human re-plan (consider alternate-env decision OR deeper substrate research-agent track) — could extend by ≥1 week.
- S-Auto-10 overnight infrastructure halt (LLM API ban, mvn cache miss, Spring restart pathology beyond OQ-S62.3) — could lose 1 overnight window.
- First cherry-pick candidate manual review surfaces borderline §5.3 case → per-sub-sprint Codex pre-milestone-close; could extend review by 1-2 days.
- S-Auto-10 pre-batch baseline drift envelope exceeds halt threshold → halt overnight + investigate.

## 10. Stop conditions (milestone-level)

**Stop signals (deliver-agent + human reassess scope; possibly invoke in-flight downgrade):**

1. S-Auto-9 OS-level investigation cannot resolve OQ-S62.3 within 1-2 days of diagnostic work AND no actionable hypothesis surfaces. Halt; surface to deliver-agent + human; re-plan (cloud / remote-server framing reconsideration OR deeper substrate research-agent track OR alternate execution pattern).
2. S-Auto-9 resolution requires a hard-fence edit beyond fence #20 envelope. Halt; surface to deliver-agent + human; possibly re-scope to a wider S-Auto-9 with NEW fence override authorized via AskUserQuestion.
3. S-Auto-9 smoke iter crashes inside the 14-step state machine in a way NOT OQ-S62.3-related (e.g., a previously-unobserved Spring spawn pathology surfaces post-resolution). Halt; root-cause investigation; possibly fix-iteration S-Auto-9.1 before S-Auto-10 begins.
4. S-Auto-10 pre-batch baseline drift envelope >10/34 cases. Halt; pause overnight + investigate.
5. S-Auto-10 overnight halts before iteration 5 (total errors >50% within first 5 iters). Halt; LLM API + infrastructure investigation.
6. S-Auto-10 overnight produces ZERO kept candidates AND deliver-agent + human jointly judge this is a meta-agent prompt issue. Halt; surface as M-Auto-3 meta-prompt refinement R-item; M-Auto-2 close PASS still possible IF all OTHER gates pass.
7. M-Auto-2 close-time bad-case manual review surfaces a NEW failure shape NOT explained by either the cherry-pick edit OR LLM-provider drift. Halt; investigate.
8. M-Auto-2 close-time shadow rerun surfaces ANY NEW regression beyond `R-shadow-fixture-empty-form-session-create-400` AND beyond cherry-pick-attributable drift ≤3%. Halt; investigate; revert cherry-pick if necessary.

**Continue signals (do NOT halt):**

- Per-iteration elapsed time >25 min but <40 min — record as observation, plan for M-Auto-3 optimization.
- LLM-provider drift in close-day bad-case / shadow rerun matching the M-Auto-1B/C signature — observation only.
- Smoke composite_score moves due to LLM provider drift — per §5.5, observation only.
- S-Auto-10 overnight produces 0 kept candidates DUE TO Tier-0 / Tier-1 / Tier-2 / shadow-drop discards (substrate working as designed). M-Auto-2 close PASS possible with 0 cherry-pick + explicit justification.
- S-Auto-9 substrate-environment fix is environmental-only (no autoloop code edit; no NEW fence #20 override needed). PREFERRED outcome.

## 11. Cross-milestone sequencing context

**Prior milestones**:

- **M-Auto-1C (Auto-Evolution Calibration Continuation)** — C — In-flight downgrade 2026-05-30. 4/13 deferred hard gates inherited by M-Auto-2 (live iter end-to-end + first overnight + first review + first cherry-pick all blocked on OQ-S62.3 expansion at M-Auto-1C close). M-Auto-2 resolves OQ-S62.3 + exercises the now-reliably-executable substrate end-to-end.
- **M-Auto-1B (Auto-Evolution Calibration)** — A-with-acceptance-bar-revision 2026-05-30. Substrate calibration + R-S57 closed via Fix-C hybrid.
- **M-Auto-1A (Auto-Evolution Build)** — A — Clean PASS 2026-05-28. Substrate complete.
- **M5 (Observability Coherence)** — A — Clean PASS 2026-05-25. Provides observability surfaces S-Auto-10 human review uses.

**Next milestones (post-M-Auto-2)**:

- **Stage-2 entry decision** — separate milestone determination after M-Auto-2 close. Evaluated against cumulative M-Auto-1B + M-Auto-1C + M-Auto-2 evidence.
- **M3-B Single Handover Orchestrator (P0)** — release_gate.md §1.1 blocker; remains in candidate slate; deliver-agent + human re-evaluate at M-Auto-2 close.
- **Projection-hygiene milestone candidate** (M5 carry-over) — independent track.
- **M-Auto-3 (extended optimization / overnight scale-up + observability hygiene)** + `R-eval-java-module-retirement` + `R-eval-interactive-judge-score-never-populated` (observability hygiene) + UC-G/H/I/J bad-case seeding + Latency / Skill-Tuning / Tier-0 re-evaluation — remain in candidate slate.

## 12. Closure verdict

*Filled by deliver-agent + human at M-Auto-2 close per `iteration_governance.md` §8.4. Placeholder during M-Auto-2 execution.*
