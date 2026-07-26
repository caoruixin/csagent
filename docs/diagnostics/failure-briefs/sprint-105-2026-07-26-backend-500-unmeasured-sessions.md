---
title: "Backend 500 mid-session silently removes ~12% of the recorded evidence base"
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: recorded runs results/20260725-124324 + results/20260725-170011 (case_results[].failure_tags) + eval_interactive/eval_interactive/batch/executor.py
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Surfaced by Sprint 105 (handoff §7 item 4) while re-scoring eight recorded
  runs offline. Filed 2026-07-26 by the Sprint 105 worktree session after
  confirming the two rows are the only non-PASS/FAIL rows in the substrate.
  §5.8 does NOT apply: the layer is `infra`, but the SCOPE is the bot backend
  (an HTTP 500 from :8080), not the eval framework (simulator / trace emitter
  / scoring / baseline aggregation / judge harness). This brief therefore does
  not preempt semantic sub-sprints and does not block a §5.6 rerun. The
  eval-side consequences named in "what should NOT be done" are framework
  scope but are observations, not the failure itself.
---

# Failure brief — backend 500 mid-session, unexplained

## What happened?

Two of the seventeen recorded sessions in the 2026-07-25 substrate died
mid-conversation on an HTTP 500 from the bot backend. The simulator raised
`BotTransportError`, the case row was written by `_error_result`, and the
session produced no verdict.

| run | case | session | died on | endpoint |
|---|---|---|---|---|
| `20260725-124324` (`ws5-goalstatus-draw3`) | `cs_interactive_179` | `b19d6ce8-9584-4b01-b662-637e8431d1ce` | turn 4 | `POST /v1/chat/sessions/{id}/messages` |
| `20260725-170011` (`s103-neighbors`) | `cs_interactive_170` | `45598049-ab93-44c1-afab-7ecf0ec82890` | turn 3 | same |

Both rows carry `status=ERROR`, `failure_tags=["INFRA:BotTransportError",
"ERROR:bot_transport_failed on turn N: HTTPStatusError(\"Server error '500 '
...\")"]`, `per_turn_trace: []` (zero entries), and a blank
`containment_outcome`. These are the only two non-`PASS`/`FAIL` rows in the
entire eight-run substrate.

**They are not the same backend build.** The `ws5-*` draws ran 12:21–12:43
UTC on 2026-07-25; the WS-6-A re-routing commit `f7ff58f5` landed at 16:48
UTC; the `s103-*` runs started 16:50 UTC. So `cs_interactive_179`'s 500
predates the re-route feature by four hours and `cs_interactive_170`'s
postdates it by twelve minutes. Whatever this is, it is not one commit.

**A shape worth testing, offered as a hypothesis and not as a diagnosis.** In
both sessions the failing turn is the first one where the customer changes
what the conversation is about:

- `cs_interactive_179` turn 4 — *"Actually — while we're on this — I also
  just realised my Gumtree account is showing as 'inactive' … It says
  'account suspended'…"* (a declared hard shift; this is a
  `drift_behavior` spec).
- `cs_interactive_170` turn 3 — *"I'm not here to ask questions. I'm telling
  you: removing the call option is hurting sales … I need the call feature
  back"* (a demand with no answerable ask, after the bot had asked for
  clarification).

Two samples is not a pattern in the mechanism, only in the shape. It is
cheap to test and it is the first thing to try.

## What should a good CS agent have done?

Produced a turn. Any turn. This is not a semantic failure — the bot never
replied, so there is nothing to judge. The correct behaviour of the *system*
is that a 500 on one turn leaves a diagnosable trace behind, which today it
does not: `per_turn_trace` is empty for both sessions because trace
collection runs after the session completes, and the session never completed.

## Why does this matter?

Every downstream number in this wave is computed on a substrate that is
~12% smaller than it looks, and the loss is not random — it removes exactly
the turns that were about to exercise the behaviour under study. WS-5's own
brief (`ws5-2026-07-25-intent-switch-forces-escalation.md`) already had to
exclude one of these two sessions from a six-session table, and Sprint 103's
neighbour arm lost one of its two neighbours. Sprint 105's per-session
before/after table quotes 17 cases and can say nothing about 2 of them.

Two further eval-side consequences, both observations rather than the
failure itself:

- **`_compute_summary` folds ERROR rows into the pass rate.** It counts
  every row in `case_results`, so an unmeasured session lands as
  `composite_score = 0.0` inside `failed`, `mean_composite_score`,
  `mean_outcome_score` and the per-UC breakdown
  (`batch/executor.py` `_compute_summary`). The `measurement_valid: False`
  marker exists on the TIMEOUT and CANCELLED rows precisely to prevent this
  reading, **has no consumer anywhere in the codebase**, and is absent from
  the ERROR row entirely.
- **The ERROR row carried no `contract_warnings` key**, so a consumer that
  subscripted it raised `KeyError` on exactly these two rows. Fixed
  2026-07-26 (commit `050a98c0`); named here because it is how the two rows
  were found.

## Is this a one-off or a pattern?

**Pattern, mechanism unknown.** 2 of 17 sessions, in 2 of 8 runs, across two
different backend builds, on the same endpoint, both on a turn that changes
the subject. That is enough to say it recurs and not enough to say why.

**The evidence needed to close it was not retained.** No backend log survives
from either window — there is no `*.log` under either checkout, and the
runtime writes the stack trace to stderr of the `mvn spring-boot:run` process
that served the run. The 500 body is not in `results.json` either; the
simulator records only the `HTTPStatusError` string.

## Which layer is likely responsible?

**`infra`** (§3.2 question 1 — a session failing on transport, not on tool
semantics). **Scope: the bot backend**, not the eval framework. That
distinction is load-bearing for §5.8: a framework-scoped `infra` brief
preempts semantic sub-sprints and blocks §5.6 reruns, and this one is not
framework-scoped, so the parallel Sprint 104/106/107 work is unaffected.

Ownership: `server/**` is Sprint 104's exclusive path and Sprint 104 holds
the backend token, so any reproduction attempt has to run there or after that
worktree merges.

## What should NOT be done?

**Do not add a retry or a transport-error tolerance to the simulator so the
runs "complete".** That is the tempting fix — it makes the substrate 17/17
and the tables tidy — and it is the wrong direction twice over: it converts a
backend fault into a measured session, and it re-contaminates the very
evidence base WS-1 and Sprint 105 were spent cleaning. If a turn 500s, the
session is unmeasured and must stay visibly unmeasured.

**Do not count these rows as bot failures.** They are already correctly
tiered `UNKNOWN` rather than `D3` by
`eval_interactive/eval_interactive/scoring/containment_ladder.py` — an
unmeasured session is never charged to the bot. Any future pass-rate
aggregation should reach the same conclusion, which today it does not (see
`_compute_summary` above).

**Do not chase the mechanism from the recorded rows.** They do not contain
it. The first step is to make the cause capturable: retain backend stderr for
the duration of a run, or have the simulator record the 500 response body
alongside the status line. The second step is to replay the two failing turns
against the current build. Only then is there something to fix.
