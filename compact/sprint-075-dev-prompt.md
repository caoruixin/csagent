# Dev Prompt — Sprint 075 / S-Auto-20 (M-Auto-5 corrective sub-sprint)

> Self-contained executable view of `docs/sprint_objective.md` (prompt-artifact-rules §9).
> Paste into a fresh dev session. You need NO other doc except `AGENTS.md`
> (auto-loaded) + this prompt. Code anchors in §8 are read on demand.

## 1. Role identity

You are the **dev agent for Sprint 075 / S-Auto-20**, the corrective sub-sprint of
Milestone **M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty**.

**One-line goal:** close the two blockers S-Auto-19's re-bless exposed so the
`goal_achieved` / one-shot grounded-answer cases get a **real outcome judgment**
(not a gate-vacuous pass), and a **looping** session can no longer vacuous-pass.
Diagnosis first, then two fixes. MEASUREMENT/INFRA only: NO prompt / routing /
UC-hypothesis / escalation-posture / skill-soft-field / CaseSpec-rubric edit. The
runtime edit is a §1.4 trace-contract completion, not a semantic change.

## 2. Read order (minimal)

1. `AGENTS.md` — governance (Constitution §1, §1.5/§1.7 anti-hardcode, §5.4 no
   eval-side override of a real bug, **§5.7 real-LLM evidence gate**, §7 stanza).
2. **This prompt** — the full contract.
3. On demand, the §8 code anchors.

Do NOT read `docs/sprints/*` or `docs/archive/*`.

## 3. WHY (the S-Auto-19 re-bless finding — read before coding)

S-Auto-19 corrected 5 eval-side measurement reads (sound — eval pytest 522/0). It
ALSO added runtime #1/#2 trace-contract changes. The `m-auto-5-baseline-20260604`
re-bless (already on disk) revealed two gaps:
- **Runtime #1/#2 were INERT on the real corpus.** `ControlKernel.isResolvedSuccessTerminal`
  requires `terminalOutcome==FINAL_ANSWER && resolveDisposition==READY_TO_CONFIRM`. On
  the simulator-preempted `goal_achieved` path the bot delivers a grounded answer and
  the sim ends (user satisfied) BEFORE the bot reaches a CONFIRM turn → READY_TO_CONFIRM
  never holds → the stamp NEVER fired (0 `resolved` stamps in the whole re-bless) →
  containment stays blank. S-Auto-19's unit test passed only because it *set*
  READY_TO_CONFIRM (the §5.7 mock-vs-real gap).
- **Consequence: gate-vacuous passes.** With containment blank, eval#1's
  `trace_minimum` tolerates it (passes), but the case has no L2 outcome check and the
  judge isn't populated → it passes the GATE with `composite=0, l2_count=0, judge=0`
  — `case_passed=true` with NO outcome judged. 4 known: `cs095`, `anchor_outcome_uc_a_visibility`,
  `anchor_outcome_uc_b_posting` (goal_achieved) + `cs012` (loop_detected — a LOOPING
  bot vacuous-passing).

Anti-误杀 HELD for genuine fails (anchor uc_g/h/i/j intake, shadow cs38s
scam/harassment still 0%) — so the eval-side reads are good; this sub-sprint fixes the
runtime stamp + the loop_detected leniency.

## 4. Scope

### Step 0 — diagnosis FIRST (read-only; existing scratch; NO re-run)

From `eval_interactive/results/m-auto-5-baseline-20260604/_rebless_scratch/_attempts/a0..a6/<suite>/results.json`
(7 draws/case, on disk — deterministic, no LLM), scan ALL 46 cases × 7 draws. A draw
is **gate-vacuous** when `case_passed==true AND composite_score==0 AND l2_results==[]
AND judge_score==0`. For every case with ≥1 gate-vacuous draw report: the 7-draw
`stop_reason` distribution (goal_achieved / loop_detected / goal_impossible /
max_turns_exceeded / bot_ended / error / …); per draw vacuous-vs-real (real = has
L2/outcome/escalation evidence, composite>0); `majority_passed` + how many passing
draws are vacuous. Do NOT assume the 4 known are exhaustive. Put the table in the
handoff. (No behaviour change.)

### Fix #1 — runtime resolved-stamp fires on the real goal_achieved one-shot path (THE blocker)

- **Target**: in `ControlKernel.isResolvedSuccessTerminal` (+ its call site near the
  terminal-stamping block), stamp `containment_outcome="resolved"` when the loop
  terminates having delivered a **substantive grounded customer-facing answer that
  resolved the issue** — WITHOUT requiring `READY_TO_CONFIRM`. Determine the right
  positive runtime signal (e.g. `terminalOutcome==FINAL_ANSWER` + a delivered grounded
  answer + no escalation + no pending intake/clarification); justify it in the handoff
  from the runtime state (`TerminalOutcome` / `ResolveDisposition` / escalation).
- **Anti-误杀 (hard, counter-tested)**: NEVER stamp `resolved` on escalation (already
  `escalated`), `ERROR` / `DEADLINE_EXCEEDED` / `LLM_UNAVAILABLE`, MAX_STEPS, a loop,
  or a mid-resolution turn (ASKED_FOR_SLOT / CONTINUE_RESOLVE). Never overwrite a
  non-null containment.
- **§5.7 validation (REQUIRED — the lesson from S-Auto-19)**: validate the stamp
  actually FIRES against a **real** `goal_achieved` trace. Rebuild + restart the
  backend (`cd server && mvn spring-boot:run`; the current PID 64692 already has the
  S-Auto-19 code — rebuild after YOUR change), then run a single `goal_achieved` case
  (e.g. `cs095_uc_d_email_recovery_misroute`) through the eval against it and confirm
  the persisted `containment_outcome=="resolved"`. A unit test that sets the
  disposition is NOT sufficient evidence.
- **Effect**: once stamped, L2 `correct_outcome` (reads `containment_outcome`,
  `outcome_checks.py:226` — unchanged) judges the case → real pass/fail (for
  `expected_outcome="either"` resolve passes; escalate-expected → resolve correctly
  FAILS). The vacuous pass becomes a real verdict.

### Fix #2 — `loop_detected` must not vacuous-pass (eval)

- **Target**: remove `loop_detected` from
  `hard_checks.HardChecker._VALID_TERMINAL_STOP_REASONS` so a looped session with blank
  containment FAILS `trace_minimum` (a loop is a genuine failure, not a valid measured
  terminal). Re-examine `max_turns_exceeded` and `goal_impossible` with the Step-0
  evidence (a budget-exhausted / gave-up no-resolution should not vacuous-pass either —
  decide each with the data + justify). `goal_achieved` stays valid (now backed by
  Fix #1).
- **Anti-误杀 (counter-tested)**: a genuinely-resolved case still PASSES.

## 5. Validation / acceptance

- **Deterministic re-score** of the existing m-auto-5 scratch with the Fix-#2 change:
  `cs012` (loop_detected) now FAILS; the S-Auto-19 legitimate flips (`alice`, `cs066`,
  `anchor_outcome_uc_f_billing`, `anchor_outcome_uc_fp_removed`) still PASS;
  eval_interactive pytest no regression vs **522**.
- **Fix-#1 real-trace check** (single goal_achieved case, NOT the full re-bless):
  containment now `resolved` on the rebuilt backend.
- **Java** no regression vs **1229/1/0/2**; new/updated runtime unit tests green.
- **Re-bless is the human-launched final step** — STOP before the multi-hour real-LLM
  re-bless; leave it re-bless-ready with the exact command. The full re-bless +
  `baseline_dir` pointer decision happen AFTER, human-gated.

## 6. Hard fences / STOP conditions

- No prompt / routing / UC-hypothesis / escalation-posture / skill soft field /
  CaseSpec rubric edit. Runtime edit is trace-contract only.
- No rubric widening to accept a bot mistake (§1.7 / §5.4).
- Fix #1 anti-误杀: never stamp resolved on an unresolved terminal; **real-trace
  validation REQUIRED** (no mock-only evidence).
- Fix #2 anti-误杀: a genuinely-resolved case still passes.
- **Do NOT move `baseline_dir`** (stays `m-auto-4-baseline-20260604`).
- **Do NOT launch the held S-Auto-17 overnight. Do NOT start S-Auto-18. Do NOT run the
  full real-LLM re-bless** (human-launched after).
- Step 0 is read-only.

## 7. §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — runtime trace-contract completion
(`ControlKernel.java`) + eval-harness measurement correctness (`hard_checks.py`). No
`eval_spec` rubric / `semantic_planner` / `prompt_projection` / routing change.

**Tier-0 invariant:** adds none; preserves Tier-0 families at current strictness. The
runtime stamp records a disposition the bot already reached; it changes no decision.

**Semantic hardcode:** none. Fix #1 keys on runtime terminal state (outcome /
disposition / escalation), not content. Fix #2 removes a stop_reason from a set.

**Generalization coverage:** measurement-infra — evidence is the Step-0 vacuous
distribution + anti-误杀 counter-tests + the deterministic re-score (loop_detected
fails, legitimate flips preserved) + the real-trace stamp validation. L4 shadow
firewall unchanged.

## 8. Code anchors (read/modify on demand)

| Anchor | Path | Use |
|---|---|---|
| `isResolvedSuccessTerminal` (added S-Auto-19; grep it) + its call site near the terminal `setContainmentOutcome("resolved")` block (~`:559`) | `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` | Fix #1 — broaden the stamp condition |
| `resolveAnswerTurnSourceIds` + `recordTurn` (~`:1651`) | `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` | Fix #1 (#2 source_ids already wired; confirm it benefits once containment stamps) |
| `TerminalOutcome`, `ResolveDisposition` enums | `server/src/main/java/com/gumtree/csagent/model/` | the runtime signals Fix #1 keys on |
| `_VALID_TERMINAL_STOP_REASONS` (~`:721`) + `_check_trace_minimum` (`:662-704`) | `eval_interactive/eval_interactive/scoring/hard_checks.py` | Fix #2 — remove loop_detected |
| `_check_correct_outcome` (`:226` reads containment) | `eval_interactive/eval_interactive/scoring/outcome_checks.py` | unchanged — judges the case once Fix #1 stamps |
| `m-auto-5-baseline-20260604/_rebless_scratch/_attempts/a0..a6/<suite>/results.json` | (on disk) | Step-0 diagnosis + deterministic re-score |
| backend health `curl -sf http://localhost:8080/actuator/health` | — | rebuild/restart for Fix-#1 §5.7 validation |

## 9. Handoff requirements (author `docs/sprints/sprint-075-handoff.md`)

MUST include: the **Step-0 vacuous 7-draw distribution table** (full 46×7 scan;
stop_reason distribution + per-draw vacuous-vs-real + the complete gate-vacuous set);
Fix #1 before/after (the runtime condition chosen + justification + file:line) + the
**real-trace validation** (a real goal_achieved session now stamps `resolved` —
show the persisted value); Fix #2 before/after + the deterministic re-score (cs012 now
FAILS; legitimate flips preserved); anti-误杀 counter-tests for both; Java +
eval_interactive no-regression; the re-bless-ready command + preconditions (backend
rebuilt, clean tree, Mac awake); confirmation `baseline_dir` NOT moved / overnight NOT
launched / S-Auto-18 NOT started; §7 self-classification; Codex deferral note.

## 10. Commit discipline

Stage only authorized `server` + `eval_interactive` scope (NOT `git add -A`). One
commit per fix where practical. Run any eval/re-score only on a clean committed tree.
Do NOT run the full re-bless / launch the overnight. Deliver-owned docs are committed
by the deliver-agent; you commit your code scope.

## 11. Self-check checklist (complete before claiming done)

- [ ] Step-0 vacuous distribution table (full 46×7 scan; stop_reason dist + vacuous-vs-
      real per draw + the complete gate-vacuous set).
- [ ] Fix #1: runtime stamps `containment_outcome=resolved` on the real goal_achieved
      one-shot grounded-answer path; never on escalation/error/loop/MAX_STEPS/mid-
      resolution; never overwrites non-null; Java tests green.
- [ ] Fix #1 §5.7: validated against a REAL goal_achieved session (containment now
      `resolved` in the persisted trace) — not mock-only.
- [ ] Fix #2: `loop_detected` removed from `_VALID_TERMINAL_STOP_REASONS` (+ max_turns/
      goal_impossible reconsidered with Step-0 evidence); looped blank-containment now
      FAILS; a genuine resolve still PASSES (counter-test).
- [ ] Deterministic re-score: cs012 FAILS; alice/cs066/uc_f/uc_fp still PASS;
      eval_interactive pytest no regression vs 522.
- [ ] Java no regression vs 1229/1/0/2.
- [ ] `baseline_dir` NOT moved; overnight NOT launched; S-Auto-18 NOT started; full
      re-bless NOT run (left re-bless-ready).
- [ ] Handoff written (Step-0 table + per-fix before/after + real-trace validation +
      re-score + re-bless-ready command).
