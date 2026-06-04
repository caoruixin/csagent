---
title: Sprint 075 / S-Auto-20 dev handoff — real goal_achieved containment stamp + loop_detected no-vacuous-pass (M-Auto-5 sub-sprint 2)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: server/.../runtime/ControlKernel.java + eval_interactive/eval_interactive/scoring/hard_checks.py
last_reviewed: 2026-06-04
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  Corrective sub-sprint of M-Auto-5. Closes the two blockers the
  m-auto-5-baseline-20260604 re-bless exposed: (1) S-Auto-19's resolved-stamp
  was INERT on the real goal_achieved one-shot path (READY_TO_CONFIRM gate too
  narrow) — broadened to also accept ANSWERED_SUBTASK + a grounding
  requirement; (2) loop_detected let a looping bot vacuous-pass — removed from
  the eval valid-terminal set. MEASUREMENT/INFRA only: NO prompt / routing /
  UC-hypothesis / escalation-posture / skill-soft-field / CaseSpec-rubric edit.
  The runtime edit is a §1.4 trace-contract completion. Both fixes ship
  anti-误杀 counter-tests. Fix #1 is validated against a REAL goal_achieved
  trace per §5.7 (a unit test alone is the gap that made S-Auto-19 inert). CODE
  + ALL TESTS + the deterministic re-score + the real-trace validation are DONE
  and committed. The real-LLM re-bless is left re-bless-ready (NOT run). The
  baseline_dir pointer is NOT moved; the held S-Auto-17 overnight is NOT
  launched; S-Auto-18 is NOT started.
---

# Sprint 075 / S-Auto-20 — dev handoff

## §0 Cold-start summary

| Item | Value |
|---|---|
| Sub-sprint | S-Auto-20 (M-Auto-5 corrective, sub-sprint 2 of 2) |
| Fix #1 (runtime) | `ControlKernel.isResolvedSuccessTerminal` broadened — stamp `resolved` on the real goal_achieved one-shot grounded-answer path |
| Fix #2 (eval) | `loop_detected` removed from `HardChecker._VALID_TERMINAL_STOP_REASONS` |
| §5.7 real-trace validation | PASS — a real goal_achieved trace persisted `containment_outcome="resolved"` (session `6965f6dc-8dca-4c0d-844e-4ec87c4650df`) |
| Java tests | 1232 total / 1 failure (inherited `SystemPromptUserRequestedTiebreakerTest`, unrelated) / 0 errors / 2 skipped = no regression vs 1229/1/0/2 |
| eval_interactive pytest | 524 passed (522 baseline + 2 net new tests) — no regression |
| baseline_dir | NOT moved (stays `m-auto-4-baseline-20260604`) |
| Re-bless | left re-bless-ready (NOT run); backend rebuilt + healthy on the Fix-#1 build |
| Commit SHAs | `95cd0f4` (Fix #1 runtime), `e6e57ec` (Fix #2 eval) |

## §1 Step-0 vacuous diagnosis (read-only; existing m-auto-5 scratch; NO re-run)

Full scan: **46 distinct cases × 7 draws = 322 draws** across
`_rebless_scratch/_attempts/a0..a6/{anchor_outcome,bad_cases,shadow}/results.json`.
A draw is **gate-vacuous** when `case_passed==true AND composite_score==0 AND
l2_results==[] AND judge_score==0`.

### §1.1 stop_reason → vacuity correlation (all 322 draws)

| stop_reason | total draws | gate-vacuous-pass draws | real (composite>0) | non-blank containment |
|---|---|---|---|---|
| `bot_ended` | 225 | 0 | 99 | 223 |
| `goal_achieved` | 51 | **25** | 0 | **0** |
| `loop_detected` | 19 | **4** | 0 | 0 |
| `goal_impossible` | 11 | **7** | 0 | 0 |
| `error` | 14 | 0 | 0 | 0 |
| `contract_violation` | 2 | 0 | 0 | 0 |
| `max_turns_exceeded` | 0 | 0 | 0 | 0 |

**Key finding**: ALL 51 `goal_achieved` draws have BLANK containment (0
non-blank) — the S-Auto-19 stamp was 100% inert on the real corpus (confirms
the blocker). `loop_detected` (4) and `goal_impossible` (7) also vacuous-pass.

### §1.2 The complete gate-vacuous set (11 cases — NOT just the 4 known)

The contract named 4 (cs095, anchor uc_a, anchor uc_b, cs012). The full scan
found **11** cases with ≥1 gate-vacuous draw:

| case | suite | exp | 7-draw stop_reason dist | vacuous/7 | majority_passed |
|---|---|---|---|---|---|
| `cs012_uc_fp_late_phone_failure_path` | bad_cases | resolve | goal_achieved:6, loop_detected:1 | **7/7** | True |
| `anchor_outcome_uc_a_visibility` | anchor | either | goal_achieved:5, bot_ended:2 | 5/7 | True |
| `anchor_outcome_uc_b_posting` | anchor | resolve | goal_achieved:7 | 5/7 | True |
| `cs095_uc_d_email_recovery_misroute` | bad_cases | either | goal_achieved:4, bot_ended:3 | 4/7 | True |
| `anchor_outcome_uc_f_billing` | anchor | either | bot_ended:3, goal_achieved:3, goal_impossible:1 | 4/7 | True |
| `cs015_uc_fp_appeal_edit_repost` | bad_cases | resolve | goal_impossible:4, bot_ended:2, loop_detected:1 | 4/7 | True |
| `anchor_outcome_uc_c_messaging` | anchor | either | bot_ended:5, goal_impossible:2 | 2/7 | True |
| `anchor_outcome_uc_fp_removed` | anchor | either | bot_ended:4, goal_achieved:3 | 2/7 | True |
| `alice_uc_a_uc_h_misclass` | bad_cases | resolve | bot_ended:6, loop_detected:1 | 1/7 | True |
| `anchor_outcome_uc_d_login` | anchor | either | bot_ended:6, goal_impossible:1 | 1/7 | True |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | bad_cases | either | bot_ended:5, loop_detected:2 | 1/7 | True |

Per-draw vacuous-vs-real detail: every vacuous draw is a `goal_achieved` /
`loop_detected` / `goal_impossible` blank-containment terminal; every "real"
draw in these cases is a `bot_ended` escalated terminal (composite≥0.5,
l2≥1). Blast radius: Fix #1 converts the `goal_achieved` vacuous draws to real
judgments; Fix #2 converts the `loop_detected` vacuous draws to FAILs;
`goal_impossible` is intentionally left (see §3.2).

## §2 Fix #1 — runtime resolved-stamp fires on the real goal_achieved path

### §2.1 before / after (`ControlKernel.isResolvedSuccessTerminal`, ~`:1298`)

**Before (S-Auto-19)** — disposition gate was `READY_TO_CONFIRM`-only:

```java
if (runResult.terminalOutcome() != FINAL_ANSWER) return false;
return ResolveDisposition.READY_TO_CONFIRM.name().equals(session.getResolveDisposition());
```

On the simulator-preempted `goal_achieved` path the bot delivers a substantive
grounded `FINAL_ANSWER` and the simulator ends the session (user satisfied)
BEFORE a dedicated CONFIRM / record_outcome turn could promote the disposition
to `READY_TO_CONFIRM`. The disposition stays `ANSWERED_SUBTASK`
(`ResolveDispositionEvaluator`: a non-question / non-slot-request grounded
answer). Result: the gate NEVER fired → 0 `resolved` stamps across the entire
m-auto-5 re-bless → all 51 goal_achieved draws blank.

**After (S-Auto-20)** — broaden the disposition allow-list + add grounding:

```java
String disposition = session.getResolveDisposition();
boolean dispositionResolved =
        READY_TO_CONFIRM.name().equals(disposition)
                || ANSWERED_SUBTASK.name().equals(disposition);
if (!dispositionResolved) return false;
String[] articlesShown = session.getArticlesShown();
return articlesShown != null && articlesShown.length > 0;
```

(retains the `containment != null` and `terminalOutcome == FINAL_ANSWER` gates
above.)

### §2.2 condition chosen + justification

The positive runtime signal for a genuinely-resolved one-shot grounded answer is:

- `terminalOutcome == FINAL_ANSWER` — the LLM produced a customer-facing answer
  (excludes MAX_STEPS / ERROR / DEADLINE_EXCEEDED / LLM_UNAVAILABLE /
  CLARIFICATION_NEEDED / USE_CASE_IDENTIFIED / ESCALATE);
- disposition ∈ {`READY_TO_CONFIRM`, `ANSWERED_SUBTASK`} — `ANSWERED_SUBTASK`
  is precisely "the bot delivered a grounded answer or a soft next step"
  (`ResolveDisposition` javadoc), the disposition at a goal_achieved terminal;
- `getArticlesShown()` non-empty — the answer is a SUBSTANTIVE GROUNDED
  resolution, not an ungrounded reply;
- `getContainmentOutcome() == null` — never overwrite an existing value.

This is a §1.4 trace-contract completion: it records the disposition the bot
ALREADY reached and changes no decision. It is NOT a §1.3 semantic change.

### §2.3 anti-误杀 — counter-tested (unit)

`ControlKernelResolvedSuccessTerminalTest` (19 tests, all green). NEVER stamps on:
MAX_STEPS, ERROR, ESCALATE, CLARIFICATION_NEEDED, `ASKED_FOR_SLOT`,
`CONTINUE_RESOLVE` (mid-resolution), an ungrounded `ANSWERED_SUBTASK` answer, an
ungrounded `READY_TO_CONFIRM` answer, a pre-existing non-null containment, or
null args / null disposition. Fires only on FINAL_ANSWER + (READY_TO_CONFIRM |
ANSWERED_SUBTASK) + grounding + blank containment.

### §2.4 §5.7 real-trace validation — PASS (the lesson that made S-Auto-19 inert)

A unit test that *sets* the disposition is insufficient — that is the exact gap
that left S-Auto-19 inert. I rebuilt + restarted the backend on the Fix-#1 build
(killed the stale S-Auto-19 PID 64692; restarted via `mvn spring-boot:run`,
health 200) and ran `cs095_uc_d_email_recovery_misroute` through the eval until
a real `goal_achieved` terminal occurred (the real LLM is stochastic; cs095 is
goal_achieved ~4/7 of the time):

| real draw | stop_reason | persisted `containment_outcome` |
|---|---|---|
| try3 (session `6965f6dc-8dca-4c0d-844e-4ec87c4650df`) | `goal_achieved` | **`resolved`** ✓ stamp FIRED |
| try4 | `goal_achieved` | `` (blank) — correctly abstained |
| try1 / try2 | `bot_ended` (escalated) | `escalated` (unchanged) |

The try3 vs try4 split CONFIRMS the anti-误杀 gate on a real trace: try3's
terminal bot turn was a substantive grounded resolution ("Once you update the
contact email, you should start receiving messages and seeing your ads
again…") → disposition `ANSWERED_SUBTASK` → stamp fired. try4's terminal turn
ended "…Let me know if you run into any issues or have further questions" — a
soft next-step shape that `ResolveDispositionEvaluator.SOFT_NEXT_STEP_PATTERN`
maps to `ASKED_FOR_SLOT` → stamp correctly did NOT fire (the bot left the door
open; not a hard terminal resolve). The stamp fires on a genuine resolve and
abstains on a non-terminal one — exactly the intended behaviour.

## §3 Fix #2 — `loop_detected` no longer vacuous-passes

### §3.1 before / after (`hard_checks.HardChecker._VALID_TERMINAL_STOP_REASONS`, ~`:740`)

**Before**: `{goal_achieved, goal_impossible, loop_detected, max_turns_exceeded}`
**After**:  `{goal_achieved, goal_impossible, max_turns_exceeded}`

A `loop_detected` terminal means the bot emitted two IDENTICAL consecutive
replies (`simulator/session_runner.py:212`,
`bot_replies[-1] == bot_replies[-2]`) — a genuine bot failure, not a
fully-measured session the simulator merely ended early. With it in the valid
set, a looping session with blank containment slipped `trace_minimum` Mode-1
and (with no other gate) vacuous-passed. Removing it routes such a session to
the strict blank-fail arm → `trace_minimum` FAILS → `case_passed=false`.

### §3.2 `goal_impossible` / `max_turns_exceeded` — reconsidered, KEPT (justified)

- **`goal_impossible` KEPT** — it is the simulator persona declaring the issue
  unresolvable (giving up), which is **ambiguous ground truth**: it can reflect
  a hard-to-satisfy persona rather than a bot fault, and several `either`-outcome
  anchor cases reach it legitimately. The deterministic re-score (§3.4) shows
  removing it would MAJORITY-FAIL `cs015_uc_fp_appeal_edit_repost`
  (pass_rate 0.857→0.286) on weak evidence — an anti-误杀 risk. Left in the
  valid set; surfaced as **OQ-S75.1** (a future `eval_spec` sub-sprint should
  decide goal_impossible per-case with expected-outcome ground truth, not a
  blanket flip).
- **`max_turns_exceeded` KEPT** — the corpus exercises ZERO such draws
  (§1.1). No evidence to act on; not speculatively narrowed.

### §3.3 anti-误杀 — counter-tested (unit)

`test_hard_checks.py::TestTraceMinimumTerminalDisposition` (and the new
`TestTraceMinimum*` tests, all green): `loop_detected` blank-containment now
FAILS (`test_fail_blank_containment_loop_detected`); `goal_achieved` /
`goal_impossible` / `max_turns_exceeded` blank still PASS (a genuine resolve
never loops); Mode-2 (blank bot reply) and the error/contract_violation/legacy
strict-fail counter-tests are UNCHANGED.

### §3.4 deterministic re-score (Fix #2 only; existing m-auto-5 scratch; NO LLM)

Method: Fix #2's ONLY effect is `trace_minimum` Mode-1. A captured-passing draw
flips to FAIL iff (blank containment AND stop_reason was-valid-but-now-invalid);
all other draws keep their captured verdict; then recompute
`majority_passed` (`trues > n/2`) + `pass_rate`. Reproduce:
`python3` over `_rebless_scratch/_attempts/a*/<suite>/results.json`.

**Draw-level (every loop_detected blank draw flips PASS→FAIL):**

| case | draws pass before→after | pass_rate before→after | majority |
|---|---|---|---|
| `cs012_uc_fp_late_phone_failure_path` | 7→6 /7 | 1.000→0.857 | True→True |
| `alice_uc_a_uc_h_misclass` | 7→6 /7 | 1.000→0.857 | True→True |
| `cs015_uc_fp_appeal_edit_repost` | 6→5 /7 | 0.857→0.714 | True→True |
| `wmkb_uc_a_trader_flag_secondary_uc_h` | 6→5 /7 | 0.857→0.714 | True→True |

**HONEST FINDING — cs012 at the case-aggregate level does NOT majority-flip
under Fix #2 alone.** The contract's "cs012 now FAILS" framing assumed cs012 was
`loop_detected`-dominated (the "4 known"). The full Step-0 scan (§1.2) shows
cs012 is actually `goal_achieved`-dominated: stop dist `{goal_achieved:6,
loop_detected:1}`. So Fix #2 flips its ONE loop_detected draw PASS→FAIL
(pass_rate 1.0→0.857) at the DRAW level, but the 6 goal_achieved draws keep it
majority-passing deterministically. cs012's true verdict resolves only after
**Fix #1 + the real-LLM re-bless** stamps & L2-judges those 6 goal_achieved
draws (a goal_achieved resolve on an `expected_outcome=resolve` case is
acceptable, so cs012 may legitimately remain a pass at corpus scale — the point
of this sub-sprint is that it is now a REAL judgment, not a vacuous one). This
is surfaced rather than masked (§1.6 / §5.4).

**Legitimate S-Auto-19 flips PRESERVED** (still majority-PASS): `alice`
(0.857), `cs066` (unchanged — no loop draws), `anchor_outcome_uc_f_billing`
(unchanged under Fix #2-A), `anchor_outcome_uc_fp_removed` (unchanged under
Fix #2-A). ✓

For comparison, the NOT-shipped variant B (also removing goal_impossible) would
majority-fail cs015 — the anti-误杀 reason §3.2 keeps goal_impossible.

## §4 Test results (vs baselines)

| suite | baseline | this sprint | delta |
|---|---|---|---|
| Java (`mvn -o test`) | 1229 pass / 1 fail / 0 err / 2 skip | 1232 total, 1 fail, 0 err, 2 skip | no regression (the 1 fail is the inherited `SystemPromptUserRequestedTiebreakerTest`, a prompt-package anchor test unrelated to this change; +4 net new ControlKernel tests; 17→19 in the resolved-terminal class) |
| eval_interactive pytest | 522 passed | 524 passed | +2 net new tests (`test_fail_blank_containment_loop_detected`, `test_loop_detected_not_in_valid_terminal_set`); no regression |

## §5 §7 self-classification (Layer + anti-hardcode stanza)

**Target failure layer:** `infra` — runtime trace-contract completion
(`ControlKernel.java`) + eval-harness measurement correctness
(`hard_checks.py`). No `eval_spec` rubric / `semantic_planner` /
`prompt_projection` / routing change.

**Tier-0 invariant:** adds none; preserves Tier-0 families at current
strictness. The runtime stamp records a disposition the bot already reached; it
changes no decision.

**Semantic hardcode:** none. Fix #1 keys on runtime terminal state (outcome /
disposition / grounding / escalation), not content. Fix #2 removes a stop_reason
from a set; no keyword/regex/enum routing.

**Generalization coverage:** measurement-infra — evidence is the Step-0 vacuous
distribution (§1) + anti-误杀 counter-tests (§2.3 / §3.3) + the deterministic
re-score (§3.4, loop_detected draws fail, legitimate flips preserved) + the
real-trace stamp validation (§2.4). L4 shadow firewall unchanged.

## §6 Re-bless — READY for the human to launch (NOT run)

The full multi-hour real-LLM re-bless is the human-launched final step. NOT run
in this dispatch.

**Exact command** (run on a CLEAN COMMITTED tree, backend up on the Fix-#1
build, Mac awake):

```bash
cd autoloop && uv run python scripts/rebless_baseline.py \
    --n 5 \
    --out-dir ../eval_interactive/results/m-auto-5-baseline-20260605
```

(Use a fresh dated out-dir; do NOT overwrite the existing
`m-auto-5-baseline-20260604` capture — it is the pre-fix evidence this handoff
cites. Match the prior `--n 5`.)

**Preconditions:**
- Backend rebuilt on the Fix-#1 build + healthy. It is CURRENTLY UP at
  `:8080` (PID `11279`, started from `target/classes` containing commit
  `95cd0f4`); `curl -sf http://localhost:8080/actuator/health` → 200. If it has
  since been stopped, restart: `cd server && set -a && . ../.env.local && set +a
  && mvn -o spring-boot:run`.
- Clean committed tree (HEAD = `e6e57ec`). `results/` is gitignored, so the
  §2.4 validation runs did not dirty the tree (`git status` clean).
- Mac awake for the full run (per the long-real-LLM-run rule — a sleep mid-run
  makes the draws uncertifiable).
- LLM creds present in `.env.local` (KIMI / DEEPSEEK / SIMULATOR / DASHSCOPE).

**Post-re-bless (human-gated, AFTER):** validate the stamp at corpus scale
(goal_achieved cases should now carry `containment_outcome=resolved` + a real L2
outcome judgment; loop_detected cases should FAIL), then the `baseline_dir`
pointer decision. The pointer is NOT moved in this dispatch.

## §7 Fences honoured

- `baseline_dir` NOT moved (stays `m-auto-4-baseline-20260604`).
- Held S-Auto-17 overnight NOT launched.
- S-Auto-18 NOT started.
- Full real-LLM re-bless NOT run (left re-bless-ready, §6).
- Step-0 diagnosis was read-only (no re-run).
- Staged ONLY authorized `server` + `eval_interactive` scope (no `git add -A`);
  deliver-owned docs untouched.
- No prompt / routing / UC-hypothesis / escalation-posture / skill-soft-field /
  CaseSpec-rubric edit. No rubric widening to accept a bot mistake.

## §8 Open questions surfaced

- **OQ-S75.1** — `goal_impossible` in the eval valid-terminal set is ambiguous
  ground truth (simulator persona giving up vs genuine bot failure). 7/11
  goal_impossible draws vacuous-pass (§1.1). A future `eval_spec` sub-sprint
  should decide it per-case against expected-outcome ground truth rather than a
  blanket flip (removing it blanket-fails cs015 on weak evidence, §3.2). KEPT
  this sub-sprint as the anti-误杀 conservative choice.
- **cs012 verdict** — resolves only at corpus scale after Fix #1 + the
  re-bless judges its 6 goal_achieved draws (§3.4). Watch it in the post-re-bless
  validation.

## §9 Codex deferral note

Per the sprint_objective Codex review plan (§4.3): PER-SUB-SPRINT RECOMMENDED;
folds into the M-Auto-5 milestone-shared close. NOT a fence-#13 SHA trigger (the
autoloop 5-file scoring set is untouched). Codex focus: (1) Fix #1 never stamps
resolved on an unresolved terminal + IS validated against a REAL goal_achieved
trace (§2.4, not mock-only); (2) Fix #2 — a looped session no longer
vacuous-passes, a genuine resolve still passes; (3) no rubric widening / no
semantic edit; (4) the Step-0 vacuous diagnosis is read-only.

## §10 Commit SHAs

- `95cd0f4` — #1 runtime: broaden resolved-stamp to the real goal_achieved
  one-shot path (`server` scope).
- `e6e57ec` — #2 eval: loop_detected no longer vacuous-passes trace_minimum
  (`eval_interactive` scope).
