---
title: (no active sub-sprint — M-Auto-5 close-pending on human-launched authoritative re-bless)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: docs/milestone_objective.md (M-Auto-5)
last_reviewed: 2026-06-04
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-076-objective.md]
superseded_by: null
notes: >
  All three M-Auto-5 dev sub-sprints have shipped: S-Auto-19 (eval-read
  column), S-Auto-20 (runtime-stamp column), S-Auto-21 (input column /
  simulator role-inversion fix). S-Auto-21 evidence: 0/40 contamination on
  the re-rendered bad-case suite (real-LLM); cross-time OLD→NEW 3→0;
  pre-fix corpus sweep 852→0 contaminated turns. Audit's 14.4 % framework
  contamination thesis closed at the source. What remains for M-Auto-5
  close is NOT a dev sub-sprint — it is the HUMAN-LAUNCHED authoritative
  multi-suite re-bless + the deliver+human paired-evidence review + the
  milestone-shared Codex + the `baseline_dir` pointer move + flipping
  `docs/current_eval_baseline.md` status. No new sub-sprint contract is
  written here. The next milestone (M-Auto-6 = audit Clusters B + C with
  parallel research-agent dispatch) is opened AFTER M-Auto-5 close.
---

# (no active dev sub-sprint)

M-Auto-5 has no remaining dev work. The three corrective sub-sprints
(S-Auto-19 → S-Auto-20 → S-Auto-21) all shipped. What remains is the
human-launched authoritative re-bless followed by the deliver+human
milestone close.

## Next actions (NOT a dev sub-sprint)

### Step 1 — HUMAN launches the authoritative re-bless

Per the dev-authored re-bless-ready command in
`docs/sprints/sprint-076-handoff.md` §6:

```bash
# Preconditions: clean tree at ba47ec6 or later; backend booted fresh
# (mvn -o spring-boot:run -Dspring-boot.run.profiles=local); curl
# /actuator/health → UP; Mac kept awake (caffeinate). See feedback memories:
# feedback_restart_backend_before_eyeball + feedback_long_llm_run_no_sleep.

cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 7 \
    --out-dir ../eval_interactive/results/m-auto-5-baseline-20260604-simfixed
```

Writes a fresh dated dir; the old `m-auto-4-baseline-20260604` and the
forensic `m-auto-5-baseline-20260604` + `m-auto-5-baseline-20260605` are
RETAINED. Expect `suspect_baseline_manipulation` to fire — the corrected
measurement legitimately shifts the verdict distribution.

### Step 2 — Deliver + human paired-evidence review (M-Auto-5 close primary gate)

Per the M-Auto-5 acceptance bar in `docs/milestone_objective.md`:

1. Structural fixes carry paired evidence (#1/#2/#3/#4 from S-Auto-19 +
   S-Auto-20's runtime stamp + S-Auto-21's simulator); #5 (PII) shipped
   the sanity test.
2. Verdict-distribution shift vs `m-auto-4-baseline-20260604` is
   explained (S-Auto-19 reads, S-Auto-20 runtime stamp, S-Auto-21
   simulator-clean traces) — no unexplained verdict flip. **Judge layer
   EXCLUDED** per OQ-S76.judge-zero resolution 2026-06-05 (`mean_judge=0.0`
   is chronic by-config, byte-identical 0.0 across all four bracketing
   runs; close write-up cites the four-run evidence table at
   `action_bank.md` `R-eval-interactive-judge-score-never-populated`).
   Canonical signal = composite + outcome + L1 + L2 `failure_tags`.
3. Grounding floor intact; safety floor intact (PII relaxation scoped).
4. Codex §4.1 nine-question kernel pass at milestone close.
5. Re-bless recorded + reversible (pointer move is the deliver-agent
   action; old baselines retained).

### Step 3 — Pointer move + `docs/current_eval_baseline.md` flip

Deliver-agent action AFTER step 2 review passes:

- Move `config.fitness.baseline_dir` from `m-auto-4-baseline-20260604` to
  `m-auto-5-baseline-20260604-simfixed`.
- Flip `docs/current_eval_baseline.md` `implementation_status` to
  `current` (file is presently stale — dated 2026-05-06; a fold-back to
  capture the M-Auto-1 → M-Auto-5 sequence is overdue and out of scope
  for the pointer-move action).

### Step 4 — M-Auto-5 milestone close

Per deliver-agent role §Close — milestone close:

- Archive `docs/codex-findings.md` → `docs/milestones/M-Auto-5_codex-review.md`.
- Archive `docs/milestone_objective.md` → `docs/milestones/M-Auto-5_objective.md`.
- Reset `docs/milestone_objective.md` + `docs/sprint_objective.md` to
  next-milestone-TBD placeholders.
- Update `docs/10-handoff.md` §0 (no active milestone; next = M-Auto-6
  open / research dispatch) + §1 (M-Auto-5 close lead; truncate older
  per `doc_governance.md` retention rule) + §2 (append M-Auto-5
  archive row).
- Run `docs/action_bank.md` §7.1 retention sweep.

### Step 5 — Open M-Auto-6 (audit Clusters B + C)

Per the human direction at S-Auto-21 open: M-Auto-6 = parallel
research-agent dispatch on audit Clusters B (B.1 `per_turn_trace`
truncation + B.2 `primary_uc` vs `active_use_case` authority) and C
(C.1 cs59s session_create 400 + C.2 46f5b2e9 500 + double-send + C.3
generic clarifier branch). Two research agents in parallel; deliver
plans the milestone after their reports.

## Carry items (NOT a sub-sprint)

- **OQ-S76.drift-stop** — surface the literal
  `stop_reason="simulator_drift_blocked"` (currently recorded as
  `stop_reason="error"`, which already fails `trace_minimum` correctly;
  label fidelity only). One-line fix when the `session_runner.py` fence
  next opens. Not a blocker.
- **OQ-S76.judge-zero RESOLVED 2026-06-05** (route c per research-agent
  investigation, collapsed into the chronic
  `R-eval-interactive-judge-score-never-populated` R-item). Chronic by-
  configuration; byte-identical 0.0 across all four M-Auto-5 bracketing
  runs; NOT a regression; NOT a re-bless blocker. Both sub-variants
  (bad_cases / anchor_outcome empty `llm_judge_dimensions` arrays + shadow
  L3 dims all `severity="advisory"` stripped by `composite.py:228`) folded
  into the same R-item. M-Auto-5 paired-evidence review EXCLUDES the judge
  layer (canonical signal = composite + outcome + L1 + L2 `failure_tags`);
  close write-up cites the four-run evidence table at `action_bank.md`
  `R-eval-interactive-judge-score-never-populated`. R-item stays LOW
  priority; NOT promoted to M-Auto-6.
- **OQ-S76.A6** (carry from audit §2.6) — bot self-diagnoses simulator
  drift in its own LLM reasoning; runtime ignores. With S-Auto-21
  shipping clean (0 contamination), A.6 may dissolve; otherwise it is
  a M-Auto-6 candidate (decided at M-Auto-6 planning).
