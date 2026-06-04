---
title: Sprint 075 / S-Auto-20 — corrective: real goal_achieved containment stamp + loop_detected no-vacuous-pass (M-Auto-5 sub-sprint 2)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-04
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-074-objective.md]
superseded_by: null
notes: >
  Corrective sub-sprint of M-Auto-5. S-Auto-19's eval-side read-corrections are kept
  (sound). The m-auto-5-baseline-20260604 re-bless exposed two blockers this
  sub-sprint closes: (1) the runtime resolved-stamp was INERT on the real
  goal_achieved one-shot path (READY_TO_CONFIRM gate too narrow), leaving containment
  blank → 4 cases pass GATE-VACUOUSLY (composite=0, l2_count=0, judge=0, no outcome
  judged); (2) loop_detected in eval#1's valid-terminal set lets a LOOPING bot
  vacuous-pass. Diagnosis is the FIRST STEP (read-only, on the existing m-auto-5
  scratch — no re-run). Then the two fixes. Pointer NOT moved; overnight NOT launched;
  S-Auto-18 NOT started. Dev session source-of-truth: compact/sprint-075-dev-prompt.md.
---

# Sprint 075 / S-Auto-20 — corrective: real containment stamp + no loop_detected vacuous-pass

## Class

- **Layer (primary)**: `infra` — runtime trace-contract completion
  (`server/.../runtime/ControlKernel.java`) + eval-harness measurement correctness
  (`eval_interactive/.../scoring/hard_checks.py`). §1.4 (persistence / trace contract)
  + measurement; NOT §1.3 semantics. No prompt / routing / UC-hypothesis /
  escalation-posture / CaseSpec rubric edit.
- **§7 stanza**: §7-EXEMPT (characterization / measurement-infra); included below for
  rigor (touches gate-contributing checks + the runtime trace contract + re-bless).
- **Codex review plan (§4.3)**: PER-SUB-SPRINT RECOMMENDED; folds into the M-Auto-5
  milestone-shared close. NOT a fence-#13 SHA trigger (autoloop 5-file scoring set
  untouched). Codex focus: the runtime stamp's anti-误杀 gating + real-trace validation
  (§5.7); loop_detected no longer vacuous-passes; no rubric widening.
- **Position**: M-Auto-5 sub-sprint 2 of 2 (S-Auto-19 accepted → **S-Auto-20**), then
  M-Auto-4's S-Auto-18 resumes on the corrected, re-blessed baseline.

## Goal

Close the two blockers S-Auto-19's re-bless exposed so the goal_achieved / one-shot
grounded-answer cases get a **real outcome judgment** (not a gate-vacuous pass) and a
**looping** session can no longer vacuous-pass. Diagnosis first; then fix runtime
stamping + the loop_detected leniency; validate.

**This sub-sprint does NOT change the bot's customer-service ability** (no semantic /
routing / prompt / CaseSpec edit). The runtime edit is a trace-contract completion.

## Step 0 — diagnosis FIRST (read-only; existing m-auto-5 scratch; NO re-run)

Using `eval_interactive/results/m-auto-5-baseline-20260604/_rebless_scratch/_attempts/a0..a6/<suite>/results.json`
(7 draws/case, already on disk — deterministic, no LLM), produce a table covering
EVERY gate-vacuous case (a full scan across all 46 cases × 7 draws — do NOT assume the
4 known are exhaustive; known: `cs095_uc_d_email_recovery_misroute`,
`cs012_uc_fp_late_phone_failure_path`, `anchor_outcome_uc_a_visibility`,
`anchor_outcome_uc_b_posting`). A draw is **gate-vacuous** when
`case_passed==true AND composite_score==0 AND l2_results==[] AND judge_score==0`. For
each affected case report:
- the 7-draw distribution of `stop_reason` (goal_achieved / loop_detected /
  goal_impossible / max_turns_exceeded / bot_ended / error / …);
- per draw: gate-vacuous vs **real** (has L2 / outcome / escalation evidence,
  composite>0);
- the case's `majority_passed` and how many of its passing draws are vacuous.

This sizes the blast radius + pins the exact vacuous set the fixes must convert from
"vacuous pass" to "real pass/fail". Put the table in the handoff. (No behaviour change
in this step.)

## Fix #1 — runtime resolved-stamp fires on the real goal_achieved / one-shot grounded-answer path (THE blocker)

- **Problem**: `ControlKernel.isResolvedSuccessTerminal` requires
  `terminalOutcome==FINAL_ANSWER && resolveDisposition==READY_TO_CONFIRM`. On the
  simulator-preempted `goal_achieved` path the bot delivers a grounded answer and the
  sim ends (user satisfied) BEFORE the bot reaches a CONFIRM turn, so READY_TO_CONFIRM
  never holds → the stamp NEVER fires → containment stays blank (0 `resolved` stamps
  across the entire m-auto-5 re-bless). S-Auto-19's unit test passed only because it
  *set* READY_TO_CONFIRM — the §5.7 mock-vs-real gap.
- **Target**: stamp `containment_outcome="resolved"` when the loop terminates having
  delivered a **substantive grounded customer-facing answer that resolved the issue**,
  on the real one-shot path — WITHOUT requiring READY_TO_CONFIRM. Find the right
  positive signal (e.g. FINAL_ANSWER + a delivered grounded answer with no escalation
  and no pending intake/clarification) — the dev determines the precise condition from
  the runtime state, justified in the handoff.
- **Anti-误杀 (hard)**: NEVER stamp `resolved` on a genuinely unresolved terminal —
  escalation (already `escalated`), `error`/`ERROR`, `DEADLINE_EXCEEDED`,
  `LLM_UNAVAILABLE`, MAX_STEPS, a loop, or a mid-resolution turn (asked-for-slot /
  continue-resolve). Never overwrite a non-null containment. Counter-test each.
- **§5.7 validation (REQUIRED — the lesson from S-Auto-19)**: validate the stamp
  actually FIRES against a **real** goal_achieved trace — run a single real-LLM
  goal_achieved case (e.g. cs095) against the rebuilt backend and confirm the persisted
  `containment_outcome=="resolved"`. A unit test that sets the disposition is NOT
  sufficient evidence.
- **Effect**: once stamped, L2 `correct_outcome` (reads `containment_outcome`,
  outcome_checks.py:226 — unchanged) judges the case: for `expected_outcome="either"`
  resolve is acceptable → real pass; for escalate-expected cases resolve correctly
  FAILS. The gate-vacuous pass becomes a real pass/fail.

## Fix #2 — `loop_detected` must not vacuous-pass

- **Problem**: eval#1 put `loop_detected` in
  `hard_checks.HardChecker._VALID_TERMINAL_STOP_REASONS`, so a looped session with
  blank containment does NOT fail trace_minimum and (with no other gate) vacuous-passes
  — a looping bot blessed as pass (cs012).
- **Target**: remove `loop_detected` from the valid-terminal set (a loop is a genuine
  failure, not a valid measured terminal) so a looped session with blank containment
  FAILS trace_minimum. Re-examine `max_turns_exceeded` and `goal_impossible` with the
  Step-0 evidence: a budget-exhausted / gave-up no-resolution should not vacuous-pass
  either — the dev decides each with the data and justifies it. `goal_achieved` stays
  valid (now backed by the Fix-#1 runtime stamp).
- **Anti-误杀**: a genuinely-resolved case must still PASS (don't over-correct into
  failing real resolves). Counter-test.

## Validation / acceptance

- **Deterministic re-score** of the existing m-auto-5 scratch with the Fix-#2 eval
  change: confirm `cs012` (loop_detected) now FAILS (no vacuous pass); confirm the
  S-Auto-19 legitimate flips (`alice`, `cs066`, `uc_f_billing`, `uc_fp_removed`) still
  PASS; eval_interactive pytest no regression vs `522`.
- **Fix-#1 real-trace check** (single goal_achieved case, NOT the full re-bless):
  containment now stamped `resolved` on the rebuilt backend.
- **Java**: no regression vs `1229/1/0/2`; new/updated runtime unit tests green.
- **Re-bless is the human-launched final step** (same pattern as S-Auto-19): the dev
  STOPS before the multi-hour real-LLM re-bless, leaving it re-bless-ready with the
  exact command. The full re-bless (validating the stamp at corpus scale + producing
  the corrected honest baseline) + the pointer decision happen AFTER, human-gated.

## Hard fences / STOP conditions

- **No prompt / routing / UC-hypothesis / escalation-posture / skill soft field /
  CaseSpec rubric edit.** Runtime edit is trace-contract only.
- **No rubric widening to accept a bot mistake (§1.7 / §5.4).**
- **Fix #1 anti-误杀**: never stamp resolved on an unresolved terminal; real-trace
  validation REQUIRED (no mock-only evidence).
- **Fix #2 anti-误杀**: a genuinely-resolved case still passes.
- **Do NOT move `baseline_dir`** (stays m-auto-4-baseline-20260604).
- **Do NOT launch the held S-Auto-17 overnight. Do NOT start S-Auto-18.**
- **Do NOT run the full real-LLM re-bless** in this dispatch (human-launched after).
- Diagnosis (Step 0) is read-only.

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — runtime trace-contract completion
(`ControlKernel.java`) + eval-harness measurement correctness (`hard_checks.py`). No
`eval_spec` rubric / `semantic_planner` / `prompt_projection` / routing change.

**Tier-0 invariant:** adds none; preserves Tier-0 families at current strictness. The
runtime stamp records a disposition the bot already reached; it changes no decision.

**Semantic hardcode:** none. Fix #1 keys on runtime terminal state (outcome /
disposition / escalation), not content. Fix #2 removes a stop_reason from a set; no
keyword/regex/enum routing.

**Generalization coverage:** measurement-infra — evidence is the Step-0 vacuous
distribution + anti-误杀 counter-tests + the deterministic re-score (loop_detected
fails, legitimate flips preserved) + the real-trace stamp validation, not
target/neighbor/negative/shadow case-family counts. L4 shadow firewall unchanged.

## Codex review plan (§4.3)

PER-SUB-SPRINT RECOMMENDED; folds into M-Auto-5 milestone-shared close. NOT a fence-#13
SHA trigger. Codex focus: (1) Fix #1 never stamps resolved on an unresolved terminal +
is validated against a REAL goal_achieved trace (not mock-only); (2) Fix #2 — a looped
session no longer vacuous-passes, a genuine resolve still passes; (3) no rubric
widening / no semantic edit; (4) the Step-0 vacuous diagnosis is read-only.

## Handoff requirements (dev authors `docs/sprints/sprint-075-handoff.md`)

MUST include: the **Step-0 vacuous 7-draw distribution table** (per case: stop_reason
distribution; per-draw vacuous-vs-real; majority + vacuous-pass count; the full
gate-vacuous set); Fix #1 before/after (the runtime condition chosen + justification +
file:line) + the **real-trace validation** (a real goal_achieved session now stamps
resolved); Fix #2 before/after + the deterministic re-score (cs012 now FAILS;
legitimate flips preserved); anti-误杀 counter-tests for both; Java + eval_interactive
no-regression; the re-bless-ready command + preconditions (backend rebuilt, clean tree,
Mac awake); confirmation pointer NOT moved / overnight NOT launched / S-Auto-18 NOT
started; §7 self-classification; Codex deferral note.

## Commit discipline

Stage only authorized `server` + `eval_interactive` scope (NOT `git add -A`). One commit
per fix where practical. Run any eval/re-score only on a clean committed tree. Do NOT
run the full re-bless / launch the overnight. Deliver-owned docs bundled by the human.

## Self-check checklist (dev completes before claiming done)

- [ ] Step-0 vacuous distribution table produced (full 46×7 scan; stop_reason
      distribution + vacuous-vs-real per draw + the complete gate-vacuous set).
- [ ] Fix #1: runtime stamps `containment_outcome=resolved` on the real goal_achieved
      one-shot grounded-answer path; never on escalation/error/loop/MAX_STEPS/mid-
      resolution; never overwrites non-null; Java tests green.
- [ ] Fix #1 §5.7: validated against a REAL goal_achieved session (containment now
      `resolved` in the persisted trace) — not a mock-only test.
- [ ] Fix #2: `loop_detected` removed from `_VALID_TERMINAL_STOP_REASONS` (+ max_turns/
      goal_impossible reconsidered with Step-0 evidence); a looped blank-containment
      session now FAILS; a genuine resolve still PASSES (counter-test).
- [ ] Deterministic re-score: cs012 now FAILS; alice/cs066/uc_f/uc_fp still PASS;
      eval_interactive pytest no regression vs 522.
- [ ] Java no regression vs 1229/1/0/2.
- [ ] baseline_dir NOT moved; overnight NOT launched; S-Auto-18 NOT started; full
      re-bless NOT run (left re-bless-ready).
- [ ] Handoff written (Step-0 table + per-fix before/after + real-trace validation +
      re-score + re-bless-ready command).
