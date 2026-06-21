---
title: "Sprint 101 / S-Auto-49 (M-Auto-11 WP1) — dev handoff"
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: >
  this handoff + docs/diagnostics/m-auto-11-wp1-viable-hit-loop-characterization-2026-06-21.md
  (the measurement record) + the 44 valid real-LLM sessions in local csagent
  postgres (read-only). Run artifacts gitignored.
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Characterization sub-sprint. Class: characterization measurement (§7 EXEMPT,
  §4.1 Codex EXEMPT — no semantic surface; milestone-shared Codex at M-Auto-11
  close). Real-LLM validation REQUIRED + DONE. Verdict:
  CHARACTERIZATION_NEGATIVE — NO WP2 (target k=0/11).
---

# Sprint 101 / S-Auto-49 (M-Auto-11 WP1) — dev handoff

## 0. One-line result

**`CHARACTERIZATION_NEGATIVE — NO WP2`.** A bounded real-LLM characterization of
the hypothesised viable-hit loop non-convergence → MAX_STEPS-escalate path found
**target k = 0/11** adjudicated avoidable MAX_STEPS events and **zero MAX_STEPS
terminals across all 44 valid draws**. On the genuinely-satisfiable UC-A
"ad expired → how do I repost it?" flow the bot converged on every draw (one
search/turn, viable hit in hand, grounded direct answer, user satisfied). The
2026-06-21 §5.9 pre-flight n=1 event is confirmed **isolated**. WP2 (Sprint 102 /
S-Auto-50) is **not activated**; M-Auto-11 closes on this candidate as an
observation. **No runtime / eval-logic / scoring / baseline / simulator /
existing-CaseSpec change.** Full evidence:
`docs/diagnostics/m-auto-11-wp1-viable-hit-loop-characterization-2026-06-21.md`.

## 1. §5.9 pre-flight — GO

Run `20260621-133434` (TARGET ×1): **GO**, 0 infra errors. Bot session created;
both turns dispatched; `/trace` + `/events` + `/llm-calls` all HTTP 200; scoring
computed; `user_state_signals` present; safety/grounding floors green. Viable
hits confirmed retrievable on the scenario (`faq_miss=false`). The pre-flight
draw itself converged (`goal_achieved`, resolved, no escalation).

## 2. Env + run-ids

- **Backend:** `make backend` profile `local` on clean committed tree HEAD
  `d1b97b77`. Health UP; PostgreSQL UP; redis UP; KB 218 articles ingested.
- **Models:** bot DeepSeek `deepseek-v4-flash`; simulator + judge Moonshot
  `moonshot-v1-32k` (`.env.local`).
- **Run discipline:** `--parallel 1`; CLI = 1 attempt/run; `caffeinate` the whole
  run (no sleep span); `NO_PROXY='*'` + AgentClient `httpx proxy=None`. Artifacts
  gitignored (`eval_interactive/results/*`).
- **Run-ids (11 each; all VALID):** TARGET `20260621-133843 .. 134306`;
  CTRL#2 `cs_uc_a_loaded_listing_resolvable` `134322 .. 134712`; CTRL#3
  `cs_uc_a_lookup_failed` `134746 .. 134953`; CTRL#4
  `cs095_uc_d_email_recovery_misroute` `135005 .. 135350`; pre-flight `133434`.

## 3. Valid-attempt accounting + replaced/invalid provenance

- VALID = completed, scoreable run through the loop (session_id present, ≥1 bot
  turn, ≥1 user_state_signal, `trace_minimum` passed).
- **All 44 draws VALID. Zero clearly-invalid. Zero replacements.** Target
  denominator = exactly **11**; the ≤16 target-draw cap was never approached. No
  provider/transport failure, no eval/trace infra failure, no 0-turn abort, no
  health flap on any draw.

## 4. Per-attempt attribution table (44 valid draws)

Legend: viable hit = ≥1 `search_knowledge` hit `faq_miss=false`; max search/turn,
max retr/turn = within-turn re-search counters (the loop signature); suppression =
any dedup/paraphrase annotation fired; MAX_STEPS = `turn_budget_exhausted` /
`max_steps_exceeded` terminal.

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

## 5. Target k + Jeffreys posterior (target-only)

- Denominator = the TARGET's 11 valid attempts only (controls never pooled).
- **k = 0/11** adjudicated-avoidable events; zero MAX_STEPS terminals.
- Jeffreys `p ~ Beta(0.5+k, 0.5+11−k) = Beta(0.5, 11.5)`:
  **P(p > 0.10) = 0.124** (gate needs ≥ 0.80); mean 0.042, median 0.020,
  95% upper credible bound 0.157.

## 6. Verdict — conjunctive WP2 gate applied

**`CHARACTERIZATION_NEGATIVE — NO WP2`.** WP2 eligibility gate (CONJUNCTIVE):

1. every counted event satisfies rubric (a)–(d) — **N/A** (no counted events);
2. target `k ≥ 2/11` — **FAIL** (k=0);
3. Jeffreys `P(p>0.10) ≥ 0.80` — **FAIL** (0.124);
4. no defeating explanation — N/A.

Conjunction FAILS (conditions 2 + 3). Outcome rule "k = 0/11 or no causally valid
events ⇒ `CHARACTERIZATION_NEGATIVE — NO WP2`; fold to observation; milestone
closes" applies. Sprint 102 / S-Auto-50 (conditional WP2 reservation) **not
activated**; no runtime change authorized.

**At-risk path genuinely exercised (not a preemption artifact):** 8/11 target
draws ran the 2-turn `DISCOVER→RESOLVE` then `RESOLVE→CONFIRM` (the T2 follow-up
handled), 3/11 clarified-then-resolved, 1/11 ran to CLOSE. Draw-1 verbatim: T2
user "Thank you for the information. How do I repost it?" → bot answered directly
and grounded ("…My Gumtree > Manage My Ads > Inactive Ads > Edit > Update My
Ad…") with one search, converging to CONFIRM. The bot held viable hits, got the
answerable follow-up, operated in RESOLVE under the 6-step ceiling, and answered
from evidence with ≤1 search/turn — never re-searching within a turn.

## 7. Control outcomes (diagnostic; never pooled)

- **CTRL#2 anti-误杀 (`cs_uc_a_loaded_listing_resolvable`):** 11/11 converged, 0
  escalations — anti-误杀 intact (not a false-quiet of an escalation-shy bot).
- **CTRL#3 genuine no-hit (`cs_uc_a_lookup_failed`):** 11/11 converged (graceful
  degrade, `goal_impossible`); no MAX_STEPS, no false avoidable event —
  rubric-specificity holds.
- **CTRL#4 cross-UC safety guard (`cs095_uc_d_email_recovery_misroute`):** 8/11
  converged, 3/11 escalated with the honest `agent_unable_to_resolve` (NOT
  `turn_budget_exhausted`/MAX_STEPS). Safety floors green on all 11 → **no
  cross-UC / safety regression.**

## 8. STOP conditions

STOP condition (1) "k=0/11 no reproduce" fired and resolves to the designed
negative exit — **not** a CaseSpec-weakening STOP. No CaseSpec weakening, no
budget raise, no escalation-suppression, no runtime change was made to force a
result. The CaseSpec was not engineered; no widening was needed; the run was
clean; the follow-up was genuinely exercised.

## 9. Safety + grounding floor status

**GREEN on every one of the 44 valid draws.** No `no_pii_leakage`,
`no_critical_policy_violation`, `no_human_only_tool_exposure`,
`phase_transition_validity`, or `grounding_compliance` hard-check failed anywhere.
Target draws produced grounded answers carrying `source_ids` + help URLs.

## 10. Tests

- New TARGET CaseSpec `cs_uc_a_viable_hit_loop_nonconvergence` loads / parses /
  registers via the flat glob (bad_cases count 18 → 19; all case_ids unique;
  frozen-four present); no existing case altered.
- `eval_interactive` pytest: **639 passed**, 5 errors. The 5 errors
  (`tests/test_rescore_s_auto_38_full_baseline.py`) are **pre-existing** and
  unrelated — they reproduce with the TARGET removed (a baseline-reconstruction
  fixture loaded by `analysis/rescore_s_auto_38_full_baseline.py` is missing
  `form_context`; outside `case_specs/bad_cases/`). Confirmed by stashing the
  TARGET and re-running that file.
- Java suite: not run — **no `server/` change** in this sub-sprint (read-only
  runtime context only); the documented inherited baseline (`1422/1/0/2`) is
  unaffected by a CaseSpec-only + docs-only change.

## 11. §4.1 Codex review

**EXEMPT** — characterization measurement, no semantic surface touched (no prompt,
no runtime semantic decision, no eval-logic, no judge calibration; the only
non-doc edit is one new bad-case + a test count-anchor bump). Milestone-shared
Codex review at M-Auto-11 close per the §4 dispatch convention.

## 12. Required explicit records (changed / not-changed)

**Changed (4 files):**

1. `eval_interactive/case_specs/bad_cases/cs_uc_a_viable_hit_loop_nonconvergence.yaml`
   — NEW TARGET characterization CaseSpec (commit `599eae4c`).
2. `eval_interactive/case_specs/bad_cases/_manifest.md` — one new ledger row for
   the TARGET (commit `599eae4c`).
3. `eval_interactive/tests/test_s_eval_1_schema_and_scoring.py` — count-anchor
   bump **18 → 19** in `test_alice_bad_case_loads_unchanged` (commit `d1b97b77`).
   **Deviation note:** the WP1 prompt's commit discipline anticipated "one
   CaseSpec only / no eval-code". This single-integer count anchor mechanically
   tracks how many bad cases exist and is bumped on every bad-case add (its own
   comment documents M-Auto-7 →17 and Sprint 097 WP2 →18, committed as a
   characterization-test update in `9757f695`). The hard acceptance bar
   "`eval_interactive` pytest green" cannot be met without it; it touches **no**
   eval/scoring/simulator/runtime LOGIC and cannot confound a real-LLM behavioural
   measurement. Recorded here per governance ("ship the safe in-scope part +
   surface"). Reviewer may treat it as count-anchor maintenance.
4. `docs/diagnostics/m-auto-11-wp1-viable-hit-loop-characterization-2026-06-21.md`
   + this handoff — the measurement record + dev handoff (docs).

**Schema-conformance correction (within file #1):** the WP1 prompt's blessed
ground truth specified `expected.allow_bot_resolution: 'full'`, which is **not**
in the frozen schema domain (`AllowBotResolution = Literal["true","false","partial"]`,
`eval_interactive/eval_interactive/case_spec/schema.py:84`) — the case would fail
to load. Mapped to `'true'` (the schema's "bot fully allowed to resolve", exactly
what the mirror-companion `cs_uc_a_loaded_listing_resolvable` uses for the same
fully-resolvable intent). Documented in the YAML; not a weakening of the oracle.

**NOT changed (everything else):** runtime / `AgentRunLoopImpl`;
`maxToolSteps` / `max_turns`; `PhaseEvaluator` terminal; escalation-reason
semantics; `ResolveDispositionEvaluator`; premature-resolve guard;
`isResolvedSuccessTerminal`; the PRIMARY and every other existing CaseSpec;
scoring logic / baseline / canonical pointers / simulator; the phase machine;
WP0; OQ-S99.1. No `server/` file touched.

## 13. Recommended next step

1. **No WP2.** Do not activate Sprint 102 / S-Auto-50; no runtime sub-sprint.
2. **Close M-Auto-11** on this candidate as an **observation**: fold
   `R-post-satisfaction-mechanical-over-escalation` to the open
   MAX_STEPS-escalate-despite-viable-hits backlog item, marked **not load-bearing
   on satisfiable flows** (0/11; posterior mean ~4%, 95% upper ~16%) pending
   higher-rate recurrence on real traffic.
3. **Keep the TARGET CaseSpec** as a `scope-relevant` characterization /
   regression instrument (would surface a future regression that makes the
   within-turn loop re-search to MAX_STEPS on this satisfiable flow).
4. Milestone-shared Codex review at M-Auto-11 close + deliver-side §5.6
   bad-case rerun + milestone archive are the remaining close gates.
