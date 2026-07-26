---
title: "A `resolved` stamp survives an authoritative UNRESOLVED user_state on 475 of 488 specs"
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: recorded run results/20260725-124324 (cs_interactive_185) + eval_interactive/eval_interactive/scoring/outcome_checks.py:238 + docs/current/m-auto-9-escalate-after-help-product-decision.md
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Surfaced by Sprint 105 (handoff §7 item 8) as "the resolved containment stamp
  is generous — runtime stamping, server/**, Sprint 104's path". Filed
  2026-07-26 with that routing CORRECTED: the runtime provably cannot see the
  contradicting signal (it is simulator-side, per ControlKernel.java:1558-1567),
  and the eval layer already records it and already has a rule for it. The
  layer is `eval_spec`, not `java_guard` / runtime. No new Tier-0 is implied.
  This brief asks for nothing to be made stricter than the M-Auto-9 product
  decision already made it — it reports that the decision has no consumer on
  the 475 specs that do not opt in.
---

# Failure brief — the `resolved` stamp outlives the evidence that contradicts it

## What happened?

`cs_interactive_185` @ `20260725-124324` ends like this:

> **bot (turn 4):** "Unfortunately, Gumtree cannot manually verify accounts or
> resend password reset links to a different email … the login email cannot be
> changed …"
>
> **user (turn 5):** "Can I speak to someone?"

The session's own recorded signals agree with the customer:

| signal | value |
|---|---|
| final `user_state` | `unresolved_after_help` |
| final `goal_status` | `impossible` |
| `stop_reason` | `goal_impossible` |
| `containment_outcome` | **`resolved`** |
| `case_passed` | **`True`** |

The `resolved` stamp is never revisited. Sprint 105's new containment ladder
then keys its **top tier** off that stamp and awards **D1 — "fully automated
closure"** to a session that closes with the customer asking for a human, and
Sprint 105's Loop C fix moves the same session from `composite 0.5000` to
`0.8667`. Both are correct given the stamp. The stamp is the input nobody
audits.

## What should a good CS agent have done?

The bot's *conduct* here was good and is not what this brief is about — it
retrieved, cited a real URL, refused honestly when the ask exceeded its tool
surface, and offered a human. Sprint 105 read the transcript and defended it,
and that judgment stands.

What should not have happened is the **bookkeeping**: a session whose
authoritative user state is UNRESOLVED should not be recorded as `resolved`
containment. The customer's problem — regaining account access — was not
solved by anyone. "The bot behaved well" and "the case was contained" are two
different claims, and only the first one is true here.

## Why does this matter?

**The rule already exists and was already decided.** The M-Auto-9 product
decision (`docs/current/m-auto-9-escalate-after-help-product-decision.md`
§"false resolve", lines 86–95) states it explicitly:

> `resolved` + authoritative `user_state` UNRESOLVED / unmet closure ⇒
> **FAIL** … simulator `goal_impossible` **without** contradictory
> user-state / closure evidence is **not** sufficient by itself to hard-fail

This session has the contradictory user-state evidence. The decision's first
clause is exactly on point, and nothing enforces it.

**Why nothing enforces it:** the only code that consumes `user_state_signals`
for this purpose is `evaluate_conditional_outcome`, and
`outcome_checks.py:238` gates the call behind
`if getattr(case_spec, "conditional_outcome_acceptance", None):`. Only **13
of 488 specs** declare that block (6 `anchor/`, 7 `bad_cases/`).
`cs_interactive_185` is a `promotion/` spec and declares none, so the M-Auto-9
rule never runs for it — nor for the other 474 specs.

**Why it matters more now than last week:** before Sprint 105 the composite
was structurally 0.0 corpus-wide, so a generous stamp cost nothing — nothing
could pass anyway. With Loop C fixed, the stamp is load-bearing in two places
at once: it decides the ladder tier (D1 vs D2) and it feeds `correct_outcome`.
The new ruler inherits the old stamp's generosity, and the ladder's own module
docstring already concedes it cannot audit the stamp it consumes.

## Is this a one-off or a pattern?

**Unknown, and structurally likely to be a pattern.** One confirmed instance —
it is the only case in the 17-session substrate that reaches `case_passed=True`
at all, so the substrate cannot answer the frequency question. What is
measurable is the *exposure*: 475 of 488 specs cannot apply the rule, and the
`unresolved_after_help` / `new_request` user states are common in this
corpus's recorded signals.

The cheap next measurement, which needs no live run: re-score every recorded
run and count sessions with `containment_outcome == "resolved"` and a final
`user_state` in the UNRESOLVED family. Sprint 105's `rescore` command already
loads both fields.

## Which layer is likely responsible?

**`eval_spec`** (§3.2 question 6 — the CaseSpec / scoring configuration is
not asking the system to be judged on evidence it already holds).

**Explicitly not `java_guard` / runtime**, which is where the Sprint 105
handoff first routed it. `ControlKernel.java:1558-1567` documents why the
runtime cannot be the fix site: `goal_impossible` and the persona's
`user_state` are **simulator-side verdicts computed across turns** and are
"never delivered to the runtime, which processes one turn at a time". The
runtime already voids a `resolved` stamp for every terminal it *can* observe
(`shouldVoidResolvedStamp`, `ControlKernel.java:1569-1581`: `MAX_STEPS`,
`ERROR`, `DEADLINE_EXCEEDED`, `LLM_UNAVAILABLE`). It cannot mirror this one.
The same comment names the eval side as the complementary half — that half is
the one with the hole.

## What should NOT be done?

**Do not add `goal_impossible` to `_TERMINAL_FAILURE_STOP_REASONS`.** This has
been considered and rejected twice on anti-误杀 grounds — once in
`hard_checks.py:952-965` (it would mis-fail full-evidence draws; observed on
shadow `cs32s02` at composite 0.5 with 5 L2 checks) and once by the M-Auto-9
product decision, which resolved the open question as *keep the exclusion*.
The signal to act on is the contradicting `user_state`, not the simulator
giving up.

**Do not pattern-match the customer's closing words.** "Can I speak to
someone?" is the tempting trigger and it is the forbidden shape under
Constitution §1.5 / §1.7 — a soft semantic judgement decided by `contains()`.
The authoritative signal is already recorded as structured data
(`user_state_signals[].user_state`); use it.

**Do not widen the CaseSpec so the session passes on purpose**, and equally do
not retro-fail Sprint 105's one moving verdict by hand. §5.4 cuts both ways:
the fix is to make the rule that already exists apply to more than 13 specs,
and then let the number land where it lands.

**Do not change what D1 means as a workaround.** Sprint 105 deliberately built
the ladder from trace facts and left `user_state_signals` out, because feeding
customer stance into a trace-derived tier changes what the tier is. If the
stamp is fixed at its source, D1 becomes correct for free.
