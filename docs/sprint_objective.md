---
title: (no active sub-sprint — M-Auto-5 close evidence generation #2 in progress; the human-launched authoritative re-re-bless on the corrected framework ≠ M-Auto-5 close itself)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: docs/milestone_objective.md (M-Auto-5)
last_reviewed: 2026-06-05
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-077-objective.md]
superseded_by: null
notes: >
  All four M-Auto-5 dev sub-sprints have shipped:
    - S-Auto-19 (eval-read column corrections)
    - S-Auto-20 (runtime-stamp column + loop_detected eval-side)
    - S-Auto-21 (input column / simulator role-inversion fix)
    - S-Auto-22 (eval-gate STALL signal promotion + terminal-failure
      stop_reason override + zero-evidence refusal + runtime stamp-downgrade
      companion) — ACCEPTED-WITH-DEVIATIONS-DOCUMENTED 2026-06-05.

  S-Auto-22 deviations (both evidence-driven, surfaced for milestone-close
  Codex):
    1. Fix #1 STALL promotion scoped to `composite == 0` (not blanket).
       Reason: cs095 a4 has `stall_detected=True + composite=0.5 +
       containment=escalated` (validly escalated; stall_detector false-positive
       on out-of-window recovery). Blanket #1 would mis-fail it and drop cs095
       to 4/9 majority FAIL, losing a tracked flip. The scoping produces
       cs095 = 5/9, exactly matching the prompt's own §4 #5 expectation. OQ
       surfaced: stall_detector window-tuning (OQ-S77.stall-detector-window).
    2. Fix #2 terminal-failure set excludes `goal_impossible`. Reasons:
       S-Auto-20 precedent (kept as AMBIGUOUS); anti-误杀 evidence (cs32s02
       a0/a2/a4 full-evidence shadow draws would be 误杀); #2 ↔ #4 consistency
       (runtime never observes goal_impossible — simulator-side verdict). The
       vacuous-goal_impossible draws are still gated by Fix #3
       (composite==0 AND l2==[]). OQ surfaced: whether `resolved+goal_impossible
       +positive-evidence` should itself be hard-fail is an eval_spec call
       (OQ-S77.goal-impossible-resolved-evidence).

  S-Auto-22 verification (deterministic re-score on the simfixed scratch):
    - 12 vacuous draws → FAIL (the expected set)
    - +1 correct extra: csmp_s01 a6 (loop_detected + resolved + composite=0.5;
      caught by Fix #2)
    - All 9 legitimate F→P flips PRESERVED (majority PASS retained); cs095 /
      uc_a / uc_fp_removed softened in line with §4 #5 expectations; 0 silent
      flip losses.
    - eval pytest 553 (538 baseline + 15 new in test_oq_s77_false_positive_gates.py)
    - Java 1244/1/0/2 (1232 baseline + 12 new in ControlKernelVoidResolvedStampTest)
    - autoloop pytest 324 unchanged
    - §5.9 pre-flight check (audit doc §6 step 6) already present at brief
      filing — verified consistent; no additional edit needed.

  Audit's 14.4 % framework-contamination thesis (S-Auto-21) AND OPPOSITE-
  direction stall-not-gated gap (S-Auto-22) both closed at the source. What
  remains for M-Auto-5 close is NOT a dev sub-sprint — it is:
    1. HUMAN-LAUNCHED authoritative re-re-bless on the corrected framework
       (backend rebuild required for S-Auto-22 #4); output dir
       `m-auto-5-baseline-20260604-simfixed-stalledfix` per
       `docs/diagnostics/2026-06-05-m-auto-5-rerebless-launch-record.md`.
    2. §5.9 pre-flight sweep on the re-re-blessed corpus — validation gate
       that the OQ-S77 gate gap is closed; expected ZERO matches on a
       properly-gated corpus.
    3. Deliver + human paired-evidence review against the bad-case suite on
       the re-re-blessed baseline (judge layer excluded per OQ-S76.judge-zero;
       canonical signal = composite + outcome + L1 + L2 failure_tags).
    4. Milestone-shared Codex (§4.1 nine-question kernel) over the M-Auto-5
       cumulative commit range (S-Auto-19 through S-Auto-22 plus all
       deliver bookkeeping). The two S-Auto-22 deviations carry into this
       review for Codex's verdict.
    5. If all gates pass: deliver moves `config.fitness.baseline_dir` pointer
       + flips `docs/current_eval_baseline.md` status + executes standard
       milestone-close archive sweep (objective + codex-review →
       docs/milestones/M-Auto-5_*; reset placeholders; §1 retention; §2
       archive index row; action_bank §7.1 retention sweep).
    6. Then open M-Auto-6 (audit Clusters B + C; 2× parallel research-agent
       dispatch). Then M-Auto-4's S-Auto-18 in M-Auto-7 or later.

  The S-Auto-21 simfixed run remains on disk as FORENSIC evidence (proves:
  simulator-fix eliminated near-coinflip; S-Auto-20 stamp fires at corpus
  scale; this stall-not-gated gap discovered). It is NOT authoritative.
  baseline_dir NOT moved. docs/current_eval_baseline.md NOT flipped.

  Framework-defect priority (§5.8) STAYS ACTIVE through the re-re-bless +
  sweep validation. Once the sweep returns zero, OQ-S77.stall-not-gated is
  closed and §5.8 priority lifts.

  Dev session source-of-truth (now archived): compact/sprint-077-dev-prompt.md.
---

# (no active dev sub-sprint)

M-Auto-5 has no remaining dev work. All four corrective sub-sprints
(S-Auto-19 → S-Auto-20 → S-Auto-21 → S-Auto-22) shipped. What remains is
the human-launched authoritative re-re-bless on the corrected framework
followed by validation sweep + paired-evidence review + milestone-shared
Codex + (if all pass) pointer move + close archive sweep.

## Next actions (NOT a dev sub-sprint)

### Step 1 — HUMAN launches the authoritative re-re-bless on the corrected framework

Authoritative launch record:
`docs/diagnostics/2026-06-05-m-auto-5-rerebless-launch-record.md` —
exact SHA, exact command, preconditions (incl. **backend rebuild**
required for the S-Auto-22 #4 runtime change), forensic-baseline
policy, expected anomalies, and post-run procedure. Read that doc, not
this section, before launch.

Output dir: `eval_interactive/results/m-auto-5-baseline-20260604-
simfixed-stalledfix` (mirror of the S-Auto-21 simfixed run for direct
comparison; old baselines retained as forensic-only).

### Step 2 — §5.9 pre-flight sweep on the re-re-blessed corpus (the validation gate)

Per `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
§6 step 6, scan every per-attempt `results.json` in the new output dir
for draws matching `case_passed=true AND composite_score=0 AND
l2_results=[]`. Halt if any match carries `STALL:*` OR a terminal-
failure `stop_reason`. **Expected ZERO matches** — this is the
validation gate that the S-Auto-22 fix closes the OQ-S77 gap at
corpus scale. A non-zero count means the gate is still letting
vacuous-passes through; route as a S-Auto-23 fix-iteration before
proceeding.

### Step 3 — Deliver + human paired-evidence review (M-Auto-5 close primary gate)

Per the M-Auto-5 acceptance bar in `docs/milestone_objective.md`:

1. Structural fixes carry paired evidence (S-Auto-19 + S-Auto-20 +
   S-Auto-21 + S-Auto-22, all shipped).
2. Verdict-distribution shift vs `m-auto-4-baseline-20260604` is
   explainable by the four columns (eval read / runtime stamp /
   simulator input / vacuous-pass gate). No unexplained verdict flip.
   **Judge layer EXCLUDED** per OQ-S76.judge-zero resolution
   2026-06-05; canonical signal = composite + outcome + L1 + L2
   `failure_tags`. Close write-up cites the four-run table at
   `action_bank.md` `R-eval-interactive-judge-score-never-populated`.
3. Grounding floor intact; safety floor intact (PII relaxation
   scoped).
4. Milestone-shared Codex (§4.1 nine-question kernel) — see Step 4.
5. Re-bless recorded + reversible (pointer move is the deliver-agent
   action; old baselines retained).

The two S-Auto-22 deviations (Fix #1 composite==0 scoping; Fix #2
goal_impossible exclusion) are surfaced for Codex's verdict; the dev's
evidence in `docs/sprints/sprint-077-handoff.md` §1 + §2 is the source
documentation.

### Step 4 — Milestone-shared Codex (§4.1) over the M-Auto-5 cumulative range

Deliver authors `compact/M-Auto-5-review-prompt.md` (self-contained
per prompt-artifact-rules §9.1/§9.2). Cumulative commit range covers
S-Auto-19 (Sprint 074) through S-Auto-22 (Sprint 077) plus all
deliver bookkeeping. The two S-Auto-22 deviations are highlighted in
the prompt with the dev's evidence; Codex's verdict on each is part
of the milestone-close decision.

### Step 5 — Pointer move + `docs/current_eval_baseline.md` flip

Deliver-agent action AFTER Steps 2-4 pass:

- Move `config.fitness.baseline_dir` from
  `m-auto-4-baseline-20260604` to
  `m-auto-5-baseline-20260604-simfixed-stalledfix`.
- Flip `docs/current_eval_baseline.md` `implementation_status` to
  `current` (note: the file is stale — dated 2026-05-06; a fold-back
  to capture the M-Auto-1 → M-Auto-5 sequence is overdue and out of
  scope for the pointer-move action).

### Step 6 — M-Auto-5 milestone close

Per deliver-agent role §Close — milestone close:

- Archive `docs/codex-findings.md` → `docs/milestones/M-Auto-5_codex-review.md`.
- Archive `docs/milestone_objective.md` → `docs/milestones/M-Auto-5_objective.md`.
- Reset `docs/milestone_objective.md` + `docs/sprint_objective.md` to
  next-milestone-TBD placeholders.
- Update `docs/10-handoff.md` §0 (no active milestone; next = M-Auto-6
  open / research dispatch), §1 (M-Auto-5 close lead; truncate older
  per `doc_governance.md` retention rule), §2 (append M-Auto-5
  archive row).
- Run `docs/action_bank.md` §7.1 retention sweep.

### Step 7 — Open M-Auto-6 (audit Clusters B + C)

Per the human direction at S-Auto-21 open: M-Auto-6 = parallel
research-agent dispatch on audit Clusters B (B.1 `per_turn_trace`
truncation + B.2 `primary_uc` vs `active_use_case` authority) and C
(C.1 cs59s session_create 400 + C.2 46f5b2e9 500 + double-send + C.3
generic clarifier branch). Two research agents in parallel; deliver
plans the milestone after their reports.

## Carry items (NOT a sub-sprint)

- **OQ-S76.judge-zero — RESOLVED 2026-06-05** (route c per
  research-agent investigation; collapsed into the chronic
  `R-eval-interactive-judge-score-never-populated` R-item). Judge
  layer excluded from paired-evidence review. R-item stays LOW
  priority; NOT promoted to M-Auto-6.
- **OQ-S76.drift-stop** — literal `stop_reason="simulator_drift_blocked"`
  not surfaced (currently `stop_reason="error"`, which already fails
  `trace_minimum` correctly; label fidelity only). NOT a blocker.
- **OQ-S76.A6** — bot self-diagnoses simulator drift; runtime ignores.
  With S-Auto-21 + S-Auto-22 shipping clean, reassess at M-Auto-5
  close (likely dissolves; if any sim-drift reasoning surfaces in
  the re-re-blessed traces, route as M-Auto-6 candidate).
- **OQ-S77.stall-not-gated — RESOLVED PENDING SWEEP VALIDATION**
  2026-06-05. Closes when the §5.9 pre-flight sweep on the re-re-
  blessed corpus returns ZERO matches. Framework-defect priority
  (§5.8) lifts at that point.
- **OQ-S77.stall-detector-window** — NEW (S-Auto-22 dev surfaced
  2026-06-05). The `stall_detector.py` window logic false-positives
  on out-of-window escalation recovery (cs095 a4 = the canonical
  evidence). Not a re-re-bless blocker (#1 scoping handles it). May
  be M-Auto-6 candidate or a later eval-spec sub-sprint. Filed in
  `action_bank.md` §5.2.
- **OQ-S77.goal-impossible-resolved-evidence** — NEW (S-Auto-22 dev
  surfaced 2026-06-05). Whether `resolved+goal_impossible+positive-
  evidence` should itself be a hard fail is an `eval_spec` judgment.
  Deferred to a future sub-sprint, consistent with S-Auto-20's own
  goal_impossible-as-ambiguous decision. Filed in `action_bank.md`
  §5.2.
