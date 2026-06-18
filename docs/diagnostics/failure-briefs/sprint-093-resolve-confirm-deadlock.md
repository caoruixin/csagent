# Failure Brief — Sprint 093 / S-Auto-39: RESOLVE→CONFIRM/CLOSE deadlock

> **Source:** exp-86 deep per-turn traces + `m-auto-7-prepilot-baseline-20260608`
> (Step-0 attribution: `docs/diagnostics/sprint-093-step0-escalation-attribution.md`).
> **Trace IDs (examples):** `alice_uc_a_uc_h_misclass` a0 session
> `a7d128e3-…`; `cs_uc_a_no_ad_id_ad_specific` oversample a4.
> **CaseSpec:** UC-A bad-cases, `expected_outcome: resolve`.
> **Run:** exp-86 bad-cases (13 deep runs) + baseline-20260608.
> **Filed:** 2026-06-18 by dev agent (human labels expected behaviour;
> dev agent labels §3 layer + do-not-do list — `iteration_governance.md` §2).
> **Related:** [[project-faq-overescalate-maxsteps-misstamp]] ·
> [[manual-probe-2026-05-24-uc-a-faq-refutation-overescalate]] ·
> [[project-uc-a-entity-verification-product-contract]].

## What happened?

On a grounded UC-A RESOLVE session, the bot retrieves
(`search_knowledge` hit) and grounds an answer (`resolve_article`),
then calls `record_outcome(outcome_class=resolve)`. The runtime
correctly rejects it as `progressive_resolve_record_outcome_premature`
(phase is RESOLVE, not CONFIRM/CLOSE). The session **cannot leave
RESOLVE**: `ResolveDispositionEvaluator.evaluate()` returns
`READY_TO_CONFIRM` only when a `record_outcome` already *succeeded*
this run — which can never happen in RESOLVE because the guard rejects
it there. So a grounded answer maps to `ANSWERED_SUBTASK` →
`progressive_resolve_stay`, and the bot loops: re-answering the same
grounded content across 4–7 turns, retrying `record_outcome` (rejected
1–4×), until it `request_handover`s. The handover stamps
`containment=escalated`, **overwriting an earlier transient `resolved`
stamp**, and surfaces a mis-attributed reason (`user_requested` even
when the user never asked for a human; or `faq_miss_threshold_exceeded`
/ `turn_budget_exhausted` from the MAX_STEPS heuristic). Net: blank/stale
containment, turn-budget exhaustion, or mis-attributed escalation on a
session the bot actually resolved. (Step-0: 13/46 deep PRIMARY draws
deadlock-attributable; ≥6 more "pass" only by luckily hitting
`max_turns` with the `isResolvedSuccessTerminal` stamp.)

## What should a good CS agent have done?

Deliver the grounded answer (correct), and on the **subsequent user
turn** be able to enter CONFIRM so the resolution can be recorded:
either `record_outcome(resolve)` lands (now permitted in CONFIRM) or
the session reaches a product-contract-allowed resolved terminal. The
bot resolved the issue; the runtime must let that resolution be
confirmed and recorded instead of forcing the conversation into a
handover loop.

## Why does this matter?

Wrong-containment / over-escalation of a genuinely resolved FAQ-path
issue, plus a mis-attributed escalation reason — violates the §5.1
acceptance bars (wrong-containment, over-escalation) and the §1.4
trace-contract (the recorded outcome contradicts what the bot did).
The escalation reason is also semantically false (`user_requested`
with no user request), reinforcing the documented runtime
reason-mislabel pattern.

## Is this a one-off or a pattern?

**Pattern.** 13 deadlock-attributable draws across 3 UC-A cases in
exp-86 deep traces; 16 deadlock-shape escalations in the baseline-20260608
PRIMARY draws. Systematic for any UC-A session the simulator persona
keeps engaging past the first grounded answer.

## Which layer is likely responsible?

**`skill_state`** (§3.2 Q4) — a multi-turn phase-machine state defect:
the RESOLVE→CONFIRM transition is unreachable for a grounded answer, so
the flow loses the ability to progress to the phase where the outcome
can be recorded. Not `java_guard` (no Tier-0 is broken; the premature
guard is correct and must be PRESERVED). Not `semantic_planner` (the
LLM's choices — grounded answer, then `record_outcome(resolve)` — are
correct; the runtime blocks the transition). Not `eval_spec` (the
CaseSpec correctly expects `resolve`; widening it to accept the
escalation would mask a real bot/runtime failure — forbidden by §1.7 /
§5.4). Confirmed `skill_state`; no §STOP trigger
(the minimal fix is a phase-transition repair that does NOT relax the
premature guard, weaken a Tier-0/frozen invariant, add a user-message
content heuristic, add a UC/case exception, or fake a stamp).

## What should NOT be done?

- Do **not** relax `shouldRejectPrematureResolveOutcome` to permit
  `record_outcome(resolve)` in RESOLVE — that deletes the premature
  guard (§1.7; explicit hard fence). The fix must route the bot INTO
  CONFIRM where the guard already permits the call.
- Do **not** add a user-message keyword/content heuristic (e.g. detect
  "that helps" / "thanks") to trigger CONFIRM. The trigger must be
  structural (grounded FINAL_ANSWER on a prior turn + a subsequent user
  turn).
- Do **not** bump `max_turns`, fake a `resolved` stamp, or stack
  another local `isResolvedSuccessTerminal` patch as the primary fix
  (SECONDARY only, and only if PRIMARY is proven not minimally
  fixable).
- Do **not** add a per-UC / per-case exception or widen the CaseSpec.
- Do **not** force-convert the ESC_NO_GUARD_HIT shape (genuine
  clarification-budget / user-request / faq_miss escalations) — anti-误杀
  boundary; those must still escalate.
