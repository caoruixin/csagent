---
title: "Intent switching forces escalation — the bot has no way to re-route"
doc_tier: diagnostic
status: diagnostic
implementation_status: not_started
source_of_truth: live eval_interactive sessions 2026-07-25 (results/20260725-122104, -123621, -124324) + cited runtime code
last_reviewed: 2026-07-25
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  First observation of this failure on record, because WS-5 (2026-07-25) was
  what made it observable: persona.drift_behavior is non-none in 344 of 486
  specs but had never been explained to the simulator, so intent switching
  had never actually been exercised. The very first runs that exercise it
  fail it 4/4 measured multi-turn sessions.
---

# Failure brief — intent switching forces escalation

## What happened?

On specs declaring `drift_behavior: hard_shift`, the simulated customer
raises a second, different need mid-conversation (the intended behaviour).
The bot cannot re-route to the new use case: it stays inside the use case it
was assigned on turn 1, answers within that use case's tool whitelist, and
when that fails it gives up and hands over.

Measured across three draws of two `promotion/` specs (`cs_interactive_179`,
`cs_interactive_185`), five measured sessions — one sixth session is excluded
as `status=ERROR` / UNMEASURED after a backend 500:

| draw | case | stop | escalation reason |
|---|---|---|---|
| 1 | 179 | bot_ended | `agent_unable_to_resolve` |
| 1 | 185 | bot_ended | `agent_unable_to_resolve` |
| 2 | 179 | bot_ended | `agent_unable_to_resolve` |
| 2 | 185 | bot_ended | `clarification_budget_exhausted` |
| 3 | 185 | goal_impossible | (customer asked for a human) |
| 3 | 179 | — | UNMEASURED (backend 500 mid-session) |

`user_state` signals across those sessions: **8 `new_request`**, 2 `working`,
1 `unresolved_after_help`. The drift is firing reliably. **Zero of the five
measured sessions resolved.**

## What should a good CS agent have done?

Recognise that the customer has raised a different need, and either handle it
(re-classify and answer the new ask) or say plainly that it is a separate
issue and finish the original one first. A human agent asked "actually, I
also need to update my payment method" does not respond by escalating the
whole conversation.

Concretely, on `cs_interactive_185` draw 2 the customer moved from "I can't
find More Tools to clear cookies" (UC-D-ish) to "my two live ads are up but I
can't log in to edit or renew them, and one of them may have been reported".
That second ask spans account recovery and ad status — both self-serve
classes under the 2026-07-25 product principle. The bot instead exhausted its
clarification budget and handed over.

## Why does this matter?

This is the product owner's own complaint — *"things it should not rush to a
human on, it escalates immediately"* — with a mechanism and a reproduction.
It is also the single largest untested surface in the corpus: 344 of 486
specs declare a drift behaviour, so the same failure plausibly touches the
majority of the suite once drift is exercised. Constitution §1.3 assigns
"drift / topic shift" to the LLM; today the runtime does not give the LLM the
means to act on it.

## Is this a one-off or a pattern?

**Pattern.** 4 of 4 measured multi-turn sessions ended in bot-initiated
escalation, the fifth in a customer request for a human, across two distinct
specs, two distinct UCs and three independent draws. Corroborated
independently by WS-5's own three-arm run (15 sessions) and by a
pre-existing failure brief for the same shape,
`sprint32-uc-a-uc-c-alternate-uc-readiness.md`, which named the forbidden
fix ("continuing to stamp UC-A through the drift") but predates any ability
to reproduce it.

## Which layer is likely responsible?

**`prompt_projection` primarily, `skill_state` secondarily** — not
`semantic_planner`. The LLM is not choosing badly; it has no available
action that would be correct:

- `classify_use_case` appears only in `discover_triage.yaml:9`, so outside
  DISCOVER the LLM cannot change the active use case at all;
  `ToolDispatcher.validateAgainstPlan` rejects it as `tool_not_in_plan`.
- `ContextProjectionBuilder` filters the projected `tool_schemas` to
  `plan.allowedTools`, so the LLM cannot even see that the tool exists.
- `CONFIRM → DISCOVER` is a dead edge: the transition is declared in
  `control-policy.yaml` and required by
  `phase3_detailed_technical_design.md:534`, but no code path writes it.
  `RerouteDecider`'s `SOFT_SHIFT_TO_DISCOVER` rewrites its target to
  RESOLVE.
- The only mechanism that can change the use case is runtime-side pattern
  matching: `DriftDetector.java:63-90` (24 keywords, `contains()`) plus
  `RuntimeIntentClassifier`'s five Sprint-10 regexes. Everything else
  returns `unknown()` → `CONTINUE_CURRENT` → state untouched.

## What should NOT be done?

**Do not add keywords or regexes to `DriftDetector` /
`RuntimeIntentClassifier`.** That is the tempting fix — the detectors already
exist and one more pattern would make a specific case pass — and it is
forbidden by Constitution §1.5 and §1.7, and by
`sprint32-uc-a-uc-c-alternate-uc-readiness.md`, which explicitly lists it as
the wrong remediation. Drift detection is a soft semantic judgement owned by
the LLM under §1.3; the runtime's job is to give the LLM the capability and
the projection, not to guess the customer's intent with `contains()`.

Equally, do not "fix" this by relaxing the specs so that escalation on a
drifted conversation counts as success. That would be a §5.4 eval-side
override of a real defect, and it is the same mistake WS-2 was created to
undo.

The direction with support is WS-6 in
`docs/proposals/performance_priority_replan_2026-07.md`: give the
RESOLVE/CONFIRM skills a re-routing capability (`classify_use_case` or a
`propose_reroute` tool), restore the `CONFIRM → DISCOVER` edge, and move the
loop latitude out of the four phases where the procedure is already fixed.
Changing the phase machine touches a normative freeze and needs a
`phase0_normative_freeze.md §0.6` deviation entry.
