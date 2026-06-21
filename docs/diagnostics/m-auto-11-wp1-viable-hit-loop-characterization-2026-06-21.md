---
title: "M-Auto-11 WP1 — viable-hit loop-nonconvergence characterization (CHARACTERIZATION_NEGATIVE)"
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: >
  the 44 valid real-LLM draws (run-ids 20260621-133843 .. 20260621-135350, plus
  the §5.9 pre-flight 20260621-133434) persisted in the local csagent postgres
  (bot_turns / bot_events, read-only) + the cited runtime code paths; this file
  is the measurement record. Run artifacts are gitignored
  (eval_interactive/results/*); the sessions are durably identifiable in postgres.
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Read-only characterization measurement for M-Auto-11 WP1 (Sprint 101 /
  S-Auto-49). Measures whether the within-turn agent loop reaches MAX_STEPS /
  turn_budget_exhausted despite a viable retrieved FAQ hit already in hand and
  the follow-up answerable from it — on a genuinely-satisfiable UC-A flow.
  RESULT: target k = 0/11 adjudicated avoidable MAX_STEPS events; ZERO MAX_STEPS
  across the entire 44-draw sample. Verdict: CHARACTERIZATION_NEGATIVE — NO WP2.
  No runtime / eval-logic / scoring / baseline / simulator / existing-CaseSpec
  change. One new TARGET CaseSpec + a count-anchor bump + docs only.
---

# M-Auto-11 WP1 — viable-hit loop-nonconvergence characterization

**Scope:** a bounded real-LLM characterization to determine whether the
within-turn agent loop reaches `MAX_STEPS` / `turn_budget_exhausted` despite a
viable retrieved FAQ hit already on hand and the follow-up answerable from it —
then classify the TARGET case as load-bearing / intermittent /
`CHARACTERIZATION_NEGATIVE`, with per-attempt attribution. **Nothing in the
runtime / eval-logic / scoring / baseline / simulator / any existing CaseSpec
was changed.** Instrument = one new TARGET bad-case
(`cs_uc_a_viable_hit_loop_nonconvergence`) + manual adjudication.

## 0. One-line result

**`CHARACTERIZATION_NEGATIVE — NO WP2`.** The hypothesised viable-hit loop
non-convergence → MAX_STEPS-escalate path **did not reproduce**: target
**k = 0/11** adjudicated avoidable MAX_STEPS events, and **zero MAX_STEPS
terminals across all 44 valid draws** (target + 3 controls). On the genuinely-
satisfiable UC-A "ad expired → how do I repost it?" flow the bot **converged on
every draw** — one search per turn, viable hit in hand, a grounded direct
answer to the follow-up, user satisfied. The 2026-06-21 §5.9 pre-flight n=1
event recorded in the candidate investigation is confirmed **isolated**, not
load-bearing. Milestone M-Auto-11 closes on this candidate as an **observation**;
no runtime sub-sprint is authorized.

## 1. What was measured (and the oracle)

The candidate investigation
(`m-auto-11-post-satisfaction-over-escalation-investigation-2026-06-21.md`)
reclassified the surfaced over-escalation event from a `semantic_planner`
posture defect to an `infra`/runtime **MAX_STEPS-escalate-despite-viable-hits**
amplification path with a `prompt_projection`/`semantic_planner` loop-convergence
contributing factor, observed at **n=1** (0/11 reproduction in the contemporary
core block). It recommended (§8 #3) a **minimum bounded characterization** before
any runtime sub-sprint: encode a shape-(ii) bad case (satisfiable user + a
trivial follow-up answerable from an already-retrieved grounded answer) and run a
bounded real-LLM sample under the V3 noise-aware rule.

- **SUCCESS (converged):** a correct grounded answer produced from already-viable
  evidence; no MAX_STEPS escalation.
- **FAILURE (counted avoidable MAX_STEPS):** avoidable repeated/equivalent
  retrieval ending in MAX_STEPS (a grounded answer was emittable; the loop spent
  budget re-retrieving and escalated). Counted only when ALL of the adjudication
  rubric holds: **(a)** ≥1 viable `search_knowledge` hit (`faq_miss=false`,
  non-empty) at/before the terminal; **(b)** the follow-up answerable from those
  hits/projected context; **(c)** MAX_STEPS reached via repeated/equivalent
  retrieval consuming budget (≥2 retrieval steps and/or suppression annotations);
  **(d)** a grounded answer was emittable before MAX_STEPS. Any failing ⇒ NOT
  counted (legitimate-escalate / simulator-noise / provider-instability).

## 2. Mechanism under measurement (read-only context, unchanged)

- RESOLVE FAQ step ceiling `max_tool_steps: 6` (`resolve_faq_grounded_answer.yaml`).
- Loop `for (int step=0; step<maxSteps; step++)` (`AgentRunLoopImpl.java`) spends
  one step per LLM round-trip; A1 dedup / A3 paraphrase `continue`s are
  inner-loop and still advance the step counter. No suppression cap / early break.
- MAX_STEPS emits the hardcoded "I'm having difficulty resolving this. Let me
  connect you with a specialist." (`PhaseEvaluator.java`), with
  `escalation_reason = turn_budget_exhausted` on a viable-hit exhaustion (the
  evidence-aware B1 / Sprint 070 label).

The hypothesis: the LLM re-issues equivalent retrieval across steps instead of
answering from hits it already has → 6-step budget exhausts → avoidable escalate.

## 3. Method (V3 cadence; nothing changed)

- **Frozen four cases** (exact IDs / roles): TARGET
  `cs_uc_a_viable_hit_loop_nonconvergence` (new); normal-convergence viable-hit
  control `cs_uc_a_loaded_listing_resolvable`; genuine no-hit control
  `cs_uc_a_lookup_failed`; standing cross-UC safety guard
  `cs095_uc_d_email_recovery_misroute`.
- **Env:** backend `make backend` profile `local` (health UP; PostgreSQL UP;
  redis UP; KB 218 articles ingested). Bot = DeepSeek `deepseek-v4-flash`;
  simulator + judge = Moonshot `moonshot-v1-32k`. `--parallel 1`; `caffeinate`
  the whole run (no sleep span); `NO_PROXY='*'` (+ AgentClient `httpx proxy=None`).
- **Sample:** CLI = 1 attempt/run; **11 valid attempts per case = 44 valid
  sessions**, each `eval-interactive run --path case_specs/bad_cases/<case>.yaml`.
- **Valid-attempt rule:** VALID = a completed, scoreable run through the loop
  (session_id present, ≥1 bot turn, ≥1 user_state_signal, `trace_minimum` passed).
  **All 44 draws were VALID — zero clearly-invalid, zero replacements; the target
  denominator is exactly 11; the ≤16 target-draw cap was never approached.**
- **Per-attempt evidence** read read-only from `bot_turns.tool_calls` JSONB
  (per-step `tool_name` / `step_index` / `result_data.faq_miss` / `hits[].source_id`
  / dedup+paraphrase annotations) + `bot_events` (`RETRIEVAL_EXECUTED` faqMiss/
  count, `RESOLVE_DISPOSITION` transition_reason, `ESCALATION_REQUESTED` reason,
  `SESSION_CLOSED` outcome) + `bot_turns.phase_after`, joined with the eval-side
  `results.json` (`user_state_signals`, `stop_reason`, `containment_outcome`,
  `escalation_reason`, l1 hard-check floors).

## 4. §5.9 pre-flight (GO)

Run `20260621-133434` (TARGET ×1): **GO**. 0 infra errors — bot session created,
both turns dispatched, `/trace` + `/events` + `/llm-calls` all HTTP 200, scoring
computed (`user_state_signals` present, floors green). Viable hits confirmed
retrievable on the scenario (`search_knowledge` `faq_miss=false`, incl. "How Long
Are Ads Active?" / "I Can't Find My Ad"). The pre-flight draw itself **converged**
(`goal_achieved`, `containment_outcome=resolved`, no escalation) — corroborating
the non-event, separate from the 11-draw denominator.

## 5. Per-attempt attribution table (44 valid draws)

Legend: viable hit = ≥1 `search_knowledge` hit with `faq_miss=false`; max
search/turn + max retr/turn = the within-turn re-search counters (the loop
signature); suppression = any dedup/paraphrase annotation fired; MAX_STEPS = a
`turn_budget_exhausted` / `max_steps_exceeded` terminal.

| case (role) | draw | run-id | valid | term phase | uc | stop | viable hit | max search/turn | max retr/turn | suppression | esc reason | MAX_STEPS | class | adjudication |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TARGET | 1 | 20260621-133843 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 2 | 20260621-133909 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 3 | 20260621-133938 | yes | RESOLVE | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 4 | 20260621-133955 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 5 | 20260621-134023 | yes | CLOSE | UC-A | bot_ended | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 6 | 20260621-134050 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 7 | 20260621-134119 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 8 | 20260621-134149 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 9 | 20260621-134224 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 10 | 20260621-134249 | yes | RESOLVE | UC-B | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| TARGET | 11 | 20260621-134306 | yes | RESOLVE | UC-B | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 1 | 20260621-134322 | yes | RESOLVE | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 2 | 20260621-134341 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 3 | 20260621-134403 | yes | RESOLVE | UC-B | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 4 | 20260621-134422 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 5 | 20260621-134446 | yes | RESOLVE | UC-B | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 6 | 20260621-134506 | yes | RESOLVE | UC-B | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 7 | 20260621-134524 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 8 | 20260621-134553 | yes | RESOLVE | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 9 | 20260621-134611 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 10 | 20260621-134637 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#2 anti-误杀 | 11 | 20260621-134712 | yes | CONFIRM | UC-A | goal_achieved | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 1 | 20260621-134746 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 2 | 20260621-134800 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 3 | 20260621-134812 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 4 | 20260621-134827 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 5 | 20260621-134839 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 6 | 20260621-134851 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 7 | 20260621-134902 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 8 | 20260621-134917 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 9 | 20260621-134928 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 10 | 20260621-134940 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#3 no-hit | 11 | 20260621-134953 | yes | RESOLVE | UC-A | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 1 | 20260621-135005 | yes | ESCALATE | UC-C | bot_ended | Y | 2 | 1 | N | agent_unable_to_resolve | no | OTHER_ESCALATE | legit escalate (honest reason) |
| CTRL#4 xUC-guard | 2 | 20260621-135029 | yes | CONFIRM | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 3 | 20260621-135048 | yes | ESCALATE | UC-C | bot_ended | Y | 1 | 1 | N | agent_unable_to_resolve | no | OTHER_ESCALATE | legit escalate (honest reason) |
| CTRL#4 xUC-guard | 4 | 20260621-135126 | yes | RESOLVE | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 5 | 20260621-135144 | yes | ESCALATE | UC-C | bot_ended | Y | 1 | 1 | N | agent_unable_to_resolve | no | OTHER_ESCALATE | legit escalate (honest reason) |
| CTRL#4 xUC-guard | 6 | 20260621-135221 | yes | RESOLVE | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 7 | 20260621-135237 | yes | CONFIRM | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 8 | 20260621-135306 | yes | RESOLVE | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 9 | 20260621-135317 | yes | RESOLVE | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 10 | 20260621-135335 | yes | RESOLVE | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |
| CTRL#4 xUC-guard | 11 | 20260621-135350 | yes | RESOLVE | UC-C | goal_impossible | Y | 1 | 1 | N | — | no | CONVERGED | converged — non-event |

**Reading:** every TARGET draw is `max search/turn = 1`, `max retr/turn = 1`,
`suppression = N`, `MAX_STEPS = no`, viable hit present, terminal CONFIRM/RESOLVE/
CLOSE, user satisfied → **converged, non-event** on all 11. The only escalations
in the whole sample are 3 cross-UC-guard draws stamped the honest
`agent_unable_to_resolve` (an LLM-chosen reason, **not** a MAX_STEPS misstamp).

## 6. The at-risk follow-up was genuinely exercised (not a preemption artifact)

The concern with a negative is that the simulator might preempt
(`goal_achieved`) before sending the at-risk follow-up, so the loop is never
reachable. It was reachable and was exercised:

- **8/11** target draws ran the full 2-turn `DISCOVER→RESOLVE` (T1 grounded
  answer) then `RESOLVE→CONFIRM` (T2 follow-up handled). **3/11** clarified on T1
  then resolved on T2. **1/11** (draw 5) ran all the way to `CONFIRM→CLOSE`.
- Verbatim from draw 1 (`bot_turns`, read-only): T1 user "…not showing up in
  search anymore. Is it still active?" → T1 bot "…I checked your ad (AD-2007) and
  it has **expired**… You can **repost** it or use the **Bump Up** feature…
  [help URL]"; **T2 user "Thank you for the information. How do I repost it?"** →
  T2 bot "…go to My Gumtree > Manage My Ads, switch to Inactive Ads, find your
  Vintage Record Player ad, and click Edit. Review the details and click Update
  My Ad to repost it…". The exact at-risk follow-up was presented and **answered
  directly and grounded**, with a single search and convergence to CONFIRM.

So the bot held viable hits, was given the answerable follow-up, operated in
RESOLVE under the 6-step ceiling, and **answered from evidence with ≤1 search per
turn — it never re-searched within a turn, never exhausted the budget, never
escalated.** This is genuine convergence, not a non-exposure artifact.

## 7. Statistics + WP2 conjunctive gate

- **Denominator = the TARGET's 11 valid attempts only** (controls are diagnostic,
  never pooled).
- **k = 0/11** adjudicated-avoidable events.
- **Jeffreys posterior** `p ~ Beta(0.5+k, 0.5+11−k) = Beta(0.5, 11.5)`:
  **P(p > 0.10) = 0.124** (δ=0.10 gate needs ≥ 0.80); posterior mean 0.042,
  median 0.020, 95% upper credible bound 0.157.
- **WP2 eligibility gate (CONJUNCTIVE — ALL must hold):**
  1. every counted event satisfies (a)–(d) — **N/A** (no counted events);
  2. target `k ≥ 2/11` — **FAIL** (k=0);
  3. Jeffreys `P(p>0.10) ≥ 0.80` — **FAIL** (0.124);
  4. no defeating explanation — N/A.
  **Conjunction FAILS** (conditions 2 and 3). → `CHARACTERIZATION_NEGATIVE`.

## 8. Controls (diagnostic; never pooled into k)

- **CTRL#2 anti-误杀 (`cs_uc_a_loaded_listing_resolvable`):** 11/11 converged,
  zero escalations. The normal-convergence viable-hit control does NOT escalate —
  the anti-误杀 signal is intact; the negative is not a false-quiet of a generally
  escalation-shy bot.
- **CTRL#3 genuine no-hit (`cs_uc_a_lookup_failed`):** 11/11 converged
  (degraded gracefully, `stop=goal_impossible`); **no** MAX_STEPS and **no** false
  avoidable event — rubric-specificity holds (the instrument does not manufacture
  an event where escalation would have been legitimate).
- **CTRL#4 cross-UC safety guard (`cs095_uc_d_email_recovery_misroute`):**
  8/11 converged, 3/11 escalated with the honest `agent_unable_to_resolve`
  (**not** `turn_budget_exhausted`/MAX_STEPS). Safety floors green on every draw
  (`no_pii_leakage`, `no_critical_policy_violation`, `no_human_only_tool_exposure`
  all pass) → **no cross-UC / safety regression.**

## 9. Safety + grounding floors

**GREEN on every one of the 44 valid draws.** No `no_pii_leakage`,
`no_critical_policy_violation`, `no_human_only_tool_exposure`,
`phase_transition_validity`, or `grounding_compliance` hard-check failed on any
draw. Target draws produced grounded answers carrying `source_ids` + help URLs.

## 10. STOP condition + interpretation

STOP condition (1) "k=0/11 no reproduce" fired and resolves to the designed
negative exit — **not** a "weaken the case" STOP. The CaseSpec was not engineered
(persona did not script/bait re-search, demand corpus-absent content, drift, or
reward the bug), no widening was needed, the run was clean (0 invalid, 0 provider
failures), and the at-risk follow-up was genuinely exercised. No CaseSpec
weakening, no budget raise, no escalation-suppression, no runtime change was made
to force any result.

The runtime MAX_STEPS-escalate-despite-viable-hits path remains **real by
construction** (the loop can exhaust its budget and emit the canned escalation),
but its **rate on genuinely-satisfiable viable-hit follow-up flows is not
load-bearing**: 0/11 here, posterior mean ~4%, 95% upper bound ~16%. This is
consistent with the candidate investigation's classification of the 2026-06-21
event as an **isolated (n=1)** occurrence of a known, partially-mitigated family.

## 11. Recommendation

1. **No WP2.** No runtime sub-sprint is authorized; Sprint 102 / S-Auto-50 (the
   conditional WP2 reservation) is **not activated**.
2. **Fold the candidate to an observation** and close M-Auto-11 on this candidate.
   The over-escalation theme stays in the backlog as the open
   MAX_STEPS-escalate-despite-viable-hits behavioural item, marked **not
   load-bearing on satisfiable flows** pending recurrence on real traffic at a
   higher rate than this bounded sample established.
3. **Keep the TARGET CaseSpec** as a `scope-relevant` characterization instrument
   / regression guard: if a future change makes the within-turn loop start
   re-searching to MAX_STEPS on this satisfiable flow, this case would surface it.

## 12. Provenance

- Sessions (read-only): local `csagent` postgres `bot_turns` / `bot_events` for
  run-ids `20260621-133843 .. 20260621-134306` (TARGET ×11),
  `20260621-134322 .. 20260621-134712` (CTRL#2 ×11),
  `20260621-134746 .. 20260621-134953` (CTRL#3 ×11),
  `20260621-135005 .. 20260621-135350` (CTRL#4 ×11), plus pre-flight
  `20260621-133434`. Run on clean committed tree HEAD `d1b97b77`.
- Code (unchanged, read-only): `AgentRunLoopImpl.java` (loop + storm-breakers),
  `PhaseEvaluator.java` (MAX_STEPS template + `resolveMaxStepsReason`),
  `resolve_faq_grounded_answer.yaml` (`max_tool_steps: 6`).
- Charter: `m-auto-11-post-satisfaction-over-escalation-investigation-2026-06-21.md`
  §8 #3 (the bounded-characterization recommendation).
