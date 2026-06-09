---
title: 2026-06-04 eval-framework audit — simulator role inversion, trace truncation, runtime 500
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: this file
last_reviewed: 2026-06-04
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Point-in-time audit of the evaluation framework itself, triggered by
  human observation that the customer simulator drifts into agent
  voice from turn 3+. Three defect clusters surfaced: (A) simulator
  role inversion, (B) trace + scoring infrastructure, (C) runtime /
  intake bugs visible in case data. This doc is the input artifact for
  any sub-sprint or research task that addresses these defects. It is
  NOT a behavior contract — see Routing (§5) for which defects route
  to deliver-direct vs research-first.
---

# 2026-06-04 eval-framework audit

## 1. TL;DR

The customer simulator that drives every multi-turn eval case has a
**role-inversion defect** at
`eval_interactive/eval_interactive/simulator/user_simulator.py:187`
that conditions the simulator LLM to behave as the **assistant /
support agent** instead of the customer from turn ~3 onward.
Lower-bound contamination across `eval_interactive/results/**` is
**1,017 / 7,071 sessions (14.4 %)**. The defect emerged in the
~one-week window 2026-05-29 → 2026-06-04 (same case
`iwzx_uc_k_advert_on_hold_restore` is clean on the 5/29 run and
contaminated on the 6/04 runs).

Consequence: every autoloop iteration, pass-rate metric, bad-case
suite rerun, and judge-driven scoring run produced in the last week
is computed on partially-fictional conversations, where the bot was
penalised for failing to handle bot-voice customer turns. The
M-Auto-5 baseline (`current_eval_baseline.md`) and the in-progress
`m-auto-5-baseline-20260605` re-baseline must be treated as
uncertifiable until the simulator is fixed and the bad-case suite is
re-rendered.

Alongside the simulator defect, the same investigation surfaced two
additional clusters:

- **B. Trace + scoring infrastructure** — `per_turn_trace` is
  truncated on 22/22 cases (Sprint 075's `trace_minimum` gate is
  inspecting data that's already 30–50 % missing);
  `primary_uc != active_use_case` on 9/22 cases (40–60 % across
  samples), with no resolved authority between CaseSpec UC tags and
  runtime classifier.
- **C. Runtime / intake** — `cs59s*` (empty form-context) cases die
  on `session_create_failed:400` and produce 0-turn transcripts;
  trace `46f5b2e9-523...` exhibits a UI / runtime defect (double-send
  of "can you check my ad status" followed by `Request failed with
  status code 500`); the generic "Could you tell me a bit more"
  opener fires even when form-context already carries a concrete
  problem, burning one of typically 3–5 available turns.

## 2. Cluster A — Simulator role inversion (load-bearing)

### 2.1 Root cause

`eval_interactive/eval_interactive/simulator/user_simulator.py:177-198`
assembles the simulator's chat-completions request from the session
transcript. Line 187 maps:

| Transcript `role`                       | Semantic                                  | Sent to simulator LLM as |
|-----------------------------------------|-------------------------------------------|--------------------------|
| `"user"` (the simulated customer)       | Simulator's own prior outputs             | `"user"`                 |
| `"bot"` (the CS agent under test)       | The other party                           | `"assistant"`            |

Under the OpenAI chat-completions protocol, `assistant` means "what
*this* model previously said". So after 2–3 turns the simulator
model's in-context pattern says: "A `user` is asking for help; an
`assistant` (i.e. me) is providing help; repeat." The position-0
system message that anchors persona ("you are a customer contacting
Gumtree support…") is a single short instruction against N rounds of
assistant-as-helper conditioning. The terminal "generate your next
customer response as JSON" reminder at line 197 does not undo the
inversion.

### 2.2 Smoking-gun trace

`cs11s01_uc_d_two_emails_one_account`, session
`7b686c76-f2af-4e55-8f2a-216bc4230760`
(`eval_interactive/results/20260604-101457/results.json`), **turn 4
user message is a byte-identical copy of the bot's turn 3 reply**,
including the `(source: ka4P200000005MHIAY)` citation token. This is
not the simulator "imagining" a customer turn — it's the chat
protocol's natural continuation when the model's role-conditioned
context tells it to continue an `assistant` thread.

### 2.3 Amplifiers (prompt-side)

- Persona is anchored only at message position 0; nothing re-injects
  it turn-by-turn. (`user_simulator.py:21-49`, `:177-198`)
- The prompt has no negative-form rule. There is no
  "you are NEVER the support agent — do not apologize, do not say
  *let me check*, do not provide solutions, do not cite sources, do
  not write `help.gumtree.com` URLs".
- The `_MAX_SIMULATOR_ATTEMPTS=3` retry mechanism exists for JSON
  parsing failures only; there is no post-generation customer-voice
  guard that could reject drift and re-prompt.

### 2.4 Contamination quantification

Lower-bound keyword sweep over all `eval_interactive/results/**`
(7,071 sessions) using markers that should never appear in a
customer turn (`"I apologize"`, `"Let me check / look into /
connect / escalate"`, `"our support / technical team"`,
`"I understand your concern"`, `"I'm sorry to hear"`,
`"(source: "`, `"https://help.gumtree.com"`,
`"Thanks for your patience / the details"`):

- **1,017 / 7,071 sessions (14.4 %)** contaminated ≥ 1 turn.
- **1,424** individual user turns contaminated.
- 7/13 hand-sampled sessions with `total_turns ≥ 4` contaminated.
- 0/4 hand-sampled 2026-05-29 sessions contaminated. Same case
  `iwzx_uc_k_advert_on_hold_restore` clean on 5/29, contaminated on
  6/04 → defect emerged within the last week.

The 14.4 % figure is a lower bound: paraphrased agent voice is
missed by the keyword list, and turn-0 (deterministic seed) is
excluded from the count.

### 2.5 Three subtypes (observed)

1. **Agent-voice drift** — simulator writes apologetic / solution-
   oriented prose in customer voice. ~7/13 sample. Example: `cs012`
   turn 4 user: *"I apologize for the confusion. It appears your ad
   was removed due to our policy against selling live animals…"*
2. **Verbatim regurgitation** — simulator emits the bot's prior turn
   word-for-word. ~2/13 sample (alice, cs11s02). Distinct failure
   mode from (1); root cause is the same role inversion.
3. **System-prompt leakage** — simulator emits its own meta-
   instruction as the customer turn. 1/13 sample.
   `cs15s02_uc_fp_keyword_match_appeal` turn 3 user is literally
   `"Based on the conversation above, generate your next customer
   response as JSON."`

### 2.6 Runtime self-diagnosis (unused)

On `csmp_s01_uc_a_search_hits_with_ad_id_mismatch` turn 4, the
agent's own LLM raw response field (visible in the trace's
`.llm_calls`) contains:

> "reasoning": "The user's message is a bot apology, not a new user
> query."

The bot model correctly identified the contamination — and the
runtime responded anyway. This is a separate runtime-decision
defect: even if the bot can self-detect simulator drift, no
downstream code consumes that signal.

## 3. Cluster B — Trace + scoring infrastructure

### 3.1 `per_turn_trace` truncated on every case

22/22 cases in `20260604-110801` and 12/12 in `20260604-045045` have
`len(per_turn_trace) < total_turns`. Examples:

- `cs11s02`: total_turns=5, transcript length=9, per_turn_trace
  length=**3**.
- `cs40s01`: total_turns=3, per_turn_trace length=**1**.
- `cs095_uc_d_email_recovery_misroute`: total_turns=3,
  per_turn_trace length=**1**.

Phase entries inside `per_turn_trace` have no `turn_index` field, so
a reviewer cannot tell which turn each entry corresponds to.

**Interaction with Sprint 075:** the `trace_minimum` runtime stamp
that Sprint 075 broadened to the real `goal_achieved` one-shot path
(`commit 95cd0f4`) now fires correctly — but the data structure it
gates on is itself ~30–50 % incomplete. Closing a gate against
incomplete data is a thermometer in an empty room.

### 3.2 `primary_uc` vs `active_use_case` mismatch on 40–60 % of cases

9/22 cases in `20260604-110801`; 7/12 in `20260604-045045`. The
CaseSpec's `primary_uc` tag and the runtime classifier's
`active_use_case` disagree on close to half the suite. Examples:

- `cs15s01`: primary_uc UC-FP, active UC-K.
- `cs15s02`: primary_uc UC-FP, active UC-A.
- `cs40s01`: primary_uc UC-K, active UC-D.
- `cs76s01`: primary_uc UC-E, active UC-K.

There is no aggregate metric exposing this; the mismatch is hidden
inside per-case `failure_tags` strings. No resolved authority — it
is unclear whether CaseSpec tags are wrong, runtime classifier is
wrong, or the two label slightly different things. Any sprint that
targets UC-routing semantics is blocked on this decision.

## 4. Cluster C — Runtime / intake

### 4.1 `cs59s*` empty-form-context cases die on session_create

`cs59s01_uc_d_empty_form_account_recovery` and
`cs59s02_uc_f_empty_form_payout_timing` consistently report
`ERROR:session_create_failed: HTTPStatusError("Client error '400 '
for url 'http://localhost:8080/v1/chat/sessions'")`, `total_turns=0`,
empty transcript. These cases are *designed* to test the empty-form
path. They have been failing this way across all runs sampled in the
last two weeks → -2 permanent pass slots subtracted from the 22-case
batch.

### 4.2 Live-UI 500 + double-send (trace `46f5b2e9-523...`)

Reported by human probe 2026-06-04 ~19:33. The eval trace stops at
turn 2 (a clean DISCOVER→RESOLVE on UC-F featured-ads). The Admin
message-box continues:

```
ad isn't showing as a featured ad             (turn 2 user)
… featured ads rotate randomly …              (turn 2 bot)
can you check my ad status                    (turn 3 user)
can you check my ad status                    (turn 3 user, duplicate)
Request failed with status code 500
```

The trace was not found in any `eval_interactive/results/` or
`logs/` file by `grep` on the trace id, suggesting this is from the
**live admin / dev UI**, not the eval pipeline. Two distinct
candidate defects:

- a frontend double-submit (same payload sent twice with no
  debouncing or in-flight guard); and / or
- a backend 500 on the duplicate which is not surfaced as a graceful
  error.

Distinct from the simulator class (Cluster A) and the eval-pipeline
class (Cluster B) — this is runtime + UI.

### 4.3 Form-context "preamble + restate" wastes opening turn

~17/22 sessions in `20260604-110801` show the bot's first reply as
the generic "Hi <name>! Thank you for reaching out. I'd like to help
you with your inquiry. Could you tell me a bit more about what you
need help with?" even when `source=form_context` already carries a
concrete problem statement in turn 0. Five other sessions in the
same batch open with the skill-specific variant ("I'm here to help
with your inquiry about <topic>…"). Same `source=form_context` flag
on both branches — the branch-selection logic is non-obvious from
case data alone.

Burns one of ~3–5 turns per session on a non-substantive exchange,
which directly inflates `turn_budget_exhausted` escalations.

## 5. Routing — which defect goes where

| Defect | Cluster | Root cause certainty | Recommended route |
|---|---|---|---|
| Simulator role-inversion (A.1) | A | Single-line code defect identified | **Deliver-direct** — fix + prompt re-anchor + customer-voice guard + multi-turn contract test |
| Simulator subtypes A.2 / A.3 (regurgitation, prompt leak) | A | Same root cause | Bundled into the A.1 fix |
| `per_turn_trace` truncation (B.1) | B | Visible in data; emitter side not yet localized | **Research-first (short)** then deliver — need to confirm runtime emission vs eval consumption |
| `primary_uc` vs `active_use_case` (B.2) | B | Ground-truth authority undecided | **Research-first (decision)** — human + research must pick which side is authoritative before any deliver scope |
| Bot self-diagnoses sim drift, runtime ignores (A.6) | A | Visible; design question | **Triage** — decide whether to wire bot-side diagnosis as a runtime signal, or rely solely on the upstream simulator fix |
| `cs59s*` 400 on session_create (C.1) | C | Likely a request-validation mismatch | **Research-first (short)** — confirm empty form-context contract |
| `46f5b2e9` 500 + double-send (C.2) | C | Symptom only; not yet root-caused | **Research-first** — reproduce + localize before scoping |
| Generic clarifier wastes turn (C.3) | C | Branching logic not localized from data | **Research-first** — read intake branch code |

The simulator fix (A) is the single highest-leverage item: it
unblocks every downstream eval metric. It can be scoped to a
deliver agent today from this document alone.

## 6. Pre-flight checklist for batch eval runs

Before any expensive batch run (bad-case suite rerun, anchor
re-bless, autoloop iteration, milestone close), execute these
checks in order. Each is cheap; together they would have caught
Clusters A–C before a week of compute was burned.

1. **Simulator integrity sample** — pick 3 cases at random from the
   last eval run; for each, read every user turn ≥ 1 and confirm
   it reads as customer voice (no apology, no "let me check", no
   source citations, no agent honorifics, no verbatim repeats of
   the prior bot turn). If any one fails → halt; do not run the
   batch.
2. **Trace completeness sample** — for the same 3 cases, confirm
   `len(per_turn_trace) == total_turns` and each entry carries an
   identifiable turn index.
3. **`primary_uc` / `active_use_case` reconciliation** — for the
   target case list, count mismatches. If > 10 %, halt: the eval
   signal will be dominated by UC-routing disagreement, not by the
   semantic surface being measured.
4. **Smoke 500 / session_create check** — confirm no case in the
   target list aborts on `session_create_failed`. Excluded cases
   should be excluded explicitly, not silently.
5. **Same-case-cross-time** — pick one case from a prior known-clean
   run; rerun it; confirm transcript shape matches. Drift here is
   the canary for any new framework regression.
6. **Vacuous-pass + terminal-failure fingerprint sweep** — added
   2026-06-05 from OQ-S77.stall-not-gated (`docs/diagnostics/
   failure-briefs/oq-s77-stall-not-gated.md`). Across the target run's
   per-attempt `results.json` files, count draws matching the
   fingerprint `case_passed=true AND composite_score=0 AND
   l2_results=[]`. For every match, halt if any of:
   `failure_tags` contains any tag matching `STALL:*` (prefix match);
   OR final `stop_reason` is in `{loop_detected, goal_impossible,
   error, contract_violation, max_turns_exceeded}`. Reference
   implementation: a single Python scan over the output dir
   (`for attempt in _rebless_scratch/_attempts/a*/: for suite in
   {bad_cases, anchor_outcome, shadow}: read results.json's
   case_results array; emit matching draws`). Expected on a
   properly-gated corpus: ZERO matches. A non-zero count means the
   eval gate is computing `case_passed=true` on stalled or
   terminal-failure sessions; halt the batch, route to the eval
   gate (S-Auto-22 or successor), then re-run.

This checklist is a **diagnostic checklist**, not a code gate. It
should be folded into `process/badcase-lifecycle.md` §5.6 as a
prerequisite step before "manual review pass" once the simulator
fix lands.

## 7. Why these defects were not surfaced earlier

- **Reviewer attention is anchored on the bot column.** Pass-rate
  is the headline metric; the eye goes to bot-turn citations, tool
  calls, judge dims. The user-turn column reads as "input", and
  "input" is mentally treated as fixed.
- **Aggregate scoring smooths late-turn drift.** Simulator drift
  fires at turn 3+; composite scores averaged across 5 turns
  dilute it.
- **No simulator contract test exists.**
  `eval_interactive/tests/test_user_simulator.py` covers JSON
  parsing only — there is no `generate_next` multi-turn test, and
  no assertion that simulator outputs are in customer voice.
- **Bad-case suite is trusted by convention.** The 12-case curated
  suite was authored once and rerun across milestones; nobody
  ran the same-case-cross-time contrast (5/29 vs 6/04) that would
  have surfaced Cluster A immediately.
- **`primary_uc` mismatch is hidden in per-case `failure_tags`
  strings.** It is reachable but not aggregated; nothing tells a
  reader "X % of your suite is mis-tagged".
- **Sprint 075's `trace_minimum` work landed against incomplete
  trace data.** Closing the gate was assumed to validate the
  signal; in fact the gate fired correctly on top of data that
  was already 30–50 % short. No invariant check that gate inputs
  are well-formed.

## 8. Provenance

Investigation triggered by human observation on 2026-06-04 that the
customer simulator produces agent-voice turns from ~turn 3. Two
parallel investigations were dispatched: (i) simulator design
audit, (ii) case-data sweep. Findings merged into this document.
No code was modified. Two memory entries written in parallel:
`feedback_review_input_column_skeptically.md` and
`project_autoloop_simulator_contamination.md`.

Key paths cited (verify before any fix):

- `eval_interactive/eval_interactive/simulator/user_simulator.py:21-49` (prompt)
- `eval_interactive/eval_interactive/simulator/user_simulator.py:177-200` (message assembly — line 187 inversion)
- `eval_interactive/eval_interactive/simulator/session_runner.py:182-192` (transcript role labels — these are correct; inversion is downstream)
- `eval_interactive/tests/test_user_simulator.py` (parsing coverage only; no `generate_next` multi-turn coverage)
- `eval_interactive/results/20260604-101457/results.json` (Case A, smoking-gun verbatim regurgitation)
- `eval_interactive/results/20260604-102932/results.json` (Case B, agent-voice slip → handover)
- `eval_interactive/results/20260604-045045/results.json` (curated bad-case attempt 0)
- `eval_interactive/results/20260604-110801/results.json` (22-case spot run)
- `eval_interactive/results/m-auto-5-baseline-20260605/_rebless_report.json` (in-progress re-baseline; stability classifications computed on top of contaminated transcripts and therefore not certifiable until A.1 ships)
- `eval_interactive/results/20260529-101324/results.json` (clean-side comparator)
- `docs/sprints/sprint-075-*` (Sprint 075 `trace_minimum` broadening — context for §3.1)
