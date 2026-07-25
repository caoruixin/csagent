# Failure Brief — E2E 2026-07-25: `search_knowledge` re-issue storm burns the tool-step budget → `turn_budget_exhausted`

> **Source:** real E2E runs of `e2e/tests/test_api_e2e.py::test_06_three_turn_dialogue_progresses_without_repeating_question`
> against a live backend on `localhost:8080` (postgres `:5442`). Not the eval
> simulator — the production `ChatController` → `SessionManager` → `AgentRunLoop`
> path.
> **Trace IDs:** session `81a08aeb-01fd-446e-9eb1-f1519ee45327` (turn 3,
> byte-identical / cross-turn variant) and session
> `f62ad6ce-0a0f-4d88-ac05-8d3dc77914c3` (turn 1, paraphrase / within-turn
> variant). Read directly from `bot_turns.tool_calls` + `bot_turn_llm_calls`.
> **CaseSpec:** N/A — E2E product-criterion test, no CaseSpec, no L3 override.
> **Reproduction rate:** ~20% before the fix attempt (1/5); **~20% after the
> first fix attempt (1/5 excluding one infra-flake run)** — see the
> "Falsified first fix" section: the first fix targeted a real but
> non-dominant sub-mode and is withdrawn.
> **Filed:** 2026-07-25 by dev agent + human (human labels expected behaviour;
> dev agent labels §3 layer + do-not-do list — `iteration_governance.md` §2).
> **Revised:** 2026-07-25, same day, after the human's 6-run re-measurement
> falsified the first fix.
> **Related:** [[sprint-101-handoff]] (M-Auto-11 WP1, the k=0/11 characterization
> this brief materially updates) · [[project-paraphrase-storm-three-class-taxonomy]] ·
> [[project-faq-overescalate-maxsteps-misstamp]] ·
> [[sprint-093-resolve-confirm-deadlock]].

## ⚠️ Status update on a prior characterization — read this first

M-Auto-11 WP1 (`docs/sprints/sprint-101-handoff.md`, 2026-06-21) characterized
"MAX_STEPS over-escalation despite viable hits" as
**`CHARACTERIZATION_NEGATIVE — NO WP2`**: `k = 0/11` adjudicated-avoidable
events, **zero MAX_STEPS terminals across all 44 valid draws**, Jeffreys
posterior mean ~4% with a **95% upper credible bound of ~16%**. It was folded to
an observation, "**not load-bearing on satisfiable flows … pending higher-rate
recurrence on real traffic**" (`sprint-101-handoff.md` §9.2).

**That conclusion is now materially updated.** The same failure shape reproduces
on the real API E2E path at **~20% (1/5)** — at/above the prior 95% upper bound,
and exactly the "higher-rate recurrence" trigger the handoff named. The 2026-06-21
measurement was not wrong; it was **scoped to a flow that cannot reach the
trigger**: its target was a single-sub-question, satisfiable UC-A flow where the
bot ran "one search per turn, viable hit in hand" and *never re-searched within a
turn* (`sprint-101-handoff.md` §6). The defect needs a **third** turn in which the
user asks a *new sub-question inside the same UC* while a **stale cross-turn
standing hit** is already parked and its budget already spent — a configuration
the WP1 CaseSpec never produced. The WP1 target CaseSpec should be kept (it is
still a valid regression instrument) but is **not sufficient coverage** for this
shape; the E2E test above is the reproducing instrument.

## What happened?

A pure **read-only UC-A status/visibility conversation** — "check the status of
AD-2002" → "what does that status mean?" → "what can I do to improve its
visibility?" — escalated to a human on turn 3 with
`escalation_reason = turn_budget_exhausted`, after answering the first two turns
correctly and while holding viable retrieved evidence.

Turn 3 of session `81a08aeb-…` (`RESOLVE → ESCALATE`, `max_tool_steps = 6`):

| step | tool | query | latency | annotation |
|---|---|---|---|---|
| 0 | `search_knowledge` | `improve ad visibility tips` | 0 ms | `cross_turn_paraphrase_suppressed` |
| 1 | `search_knowledge` | `improve ad visibility tips` | 0 ms | `cross_turn_paraphrase_suppressed` |
| 2 | `search_knowledge` | `improve ad visibility tips` | 0 ms | `cross_turn_paraphrase_suppressed` |
| 3 | `search_knowledge` | `improve ad visibility tips` | 0 ms | `cross_turn_paraphrase_suppressed` |
| 4 | `resolve_article` | — | 1 ms | — |
| 5 | `resolve_article` | — | 0 ms | — |
| — | `request_handover` | — | — | synthesized post-loop (`step_index = -1`) |

The four calls at steps 0–3 are **byte-identical**. All four were correctly
suppressed — the KB was never re-queried (`latency_ms = 0`) — and yet **all four
consumed a loop step**. 4 of 6 steps went on zero work; the LLM never got a step
to write its answer; the loop exited `MAX_STEPS`, which
`PhaseEvaluator` maps to `ESCALATE / turn_budget_exhausted`.

### The second, dominant variant — session `f62ad6ce-…`, turn 1

`DISCOVER → ESCALATE`, UC-A, RESOLVE-FAQ budget 6. **Ten** `search_knowledge`
calls, of which only two are byte-identical to an earlier one:

```
step 0  ad visibility low visibility how to improve   lat 1590ms   REAL DISPATCH, faq_miss=false
step 1  ad visibility low views                       lat 0        paraphrase_suppressed
step 2  improve ad visibility Gumtree                 lat 0        paraphrase_suppressed
step 3  ad visibility tips Gumtree                    lat 0        paraphrase_suppressed
step 4  ad visibility tips improve                    lat 0        paraphrase_suppressed
step 5  ad visibility tips Gumtree      (= step 3)    lat 0        paraphrase_suppressed
step 6  ad visibility low visibility                  lat 0        paraphrase_suppressed
step 7  improve ad visibility Gumtree   (= step 2)    lat 0        paraphrase_suppressed
step 8  improve ad visibility                         lat 0        paraphrase_suppressed
step 9  ad visibility tips                            lat 0        paraphrase_suppressed
        request_handover                              step -1      synthesized (MAX_STEPS)
```

The first search landed a viable hit, so the **within-turn A3 gate suppressed
every subsequent search in the turn**. Note the two exact repeats at steps 5 and
7 *also* missed A1 — because the calls they duplicate (steps 2, 3) were
themselves suppressed and so never entered A1's cache. That is the same A1 hole
described below, and fixing it would have changed nothing: A3 had already
suppressed them.

**This variant is the dominant one, and A1-style identity dedup cannot reach
it by construction** — 8 of the 10 queries are distinct strings.

### Root cause

**(a) Suppression prevents the dispatch, never the budget.** All three
deterministic backstops in
`server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
— A1 identity cache (`:613-651` pre-fix), within-turn A3 gate (`:653-701`),
cross-turn gate (`:703-768`) — end with `continue` on the **inner**
`for (ToolCall call : calls)` loop. The **outer** counter
`for (int step = 0; step < maxSteps; step++)` is *one LLM invocation*, not one
dispatch. `deepseek-v4-flash` emits exactly one tool call per response here, so
every suppressed repeat still cost a full step.

This is real, but it is **a consequence, not the cause** — see the falsified
first fix below. Refunding those steps does not make the model converge; it just
gives the storm more room.

**(b) THE CAUSE — the model cannot see the queries it has already issued, and
was explicitly told that "materially different" queries are exempt.** Three
sub-findings:

- **Why A1 (byte-identical dedup) did not fire.** A1's cache is populated
  *only at the real dispatch site* (`AgentRunLoopImpl.java:798-800` pre-fix:
  `if (dedupKey != null && result != null && result.isSuccess()) successfulDispatchCache.putIfAbsent(...)`),
  which is unreachable once a suppression gate has `continue`d. The step-0 call
  was consumed by the **cross-turn** gate, so it never entered the A1 cache;
  every later byte-identical repeat therefore missed A1 too and fell through to
  the same cross-turn gate. The trace confirms it: steps 1–3 carry
  `cross_turn_paraphrase_suppressed=true`, `deduplicated=null`. *(The candidate
  hypothesis "A1 only caches successes, and `faq_miss=true` results are not
  successes" is **not** the cause here — every result in this turn was
  `faq_miss=false`. The hypothesis "A1 fired but the step was still spent" is
  **also true** and is cause (a); it is independently visible in the other
  observed manifestation, "turn 1 spun `search_knowledge` ×6 on one query",
  recorded in the E2E test's own comment at `e2e/tests/test_api_e2e.py:572`.)*
- **The model's own reasoning is a factual claim it had no way to check.**
  From `bot_turn_llm_calls.llm_raw_response`, `81a08aeb-…` step 1: *"The prior
  search was about ad status, not visibility improvement tips. A new search with
  a materially different query is warranted"*; step 2: *"The prior
  search_knowledge hits are about 'Where Is My Ad?' which is not directly about
  improving visibility. I should search for tips on improving ad visibility."*
  Every step of the `f62ad6ce-…` storm reasons the same way. These are
  assertions **about its own history**, and the projection contains no data
  against which to test them.
- **`already_called` carries only an opaque hash.**
  `ContextProjectionBuilder.java:1458-1474` emits
  `{tool, arguments_hash, at_step}`. The model can see *that* it called
  `search_knowledge` five times; it cannot see *what it asked*. "Is my new query
  materially different from what I already searched?" is therefore literally
  unanswerable from the projection — and the model answered it optimistically
  every single time.
- **Two independent channels then granted an exemption keyed on exactly that
  unanswerable question.** `search_reuse_instruction`
  (`ContextProjectionBuilder.java:1179-1187`) and
  `resolve_faq_grounded_answer.yaml:31` `grounding_instruction` both ended:
  *"a fresh search_knowledge is only warranted if the prior result was
  faq_miss=true or **your new query is materially different from what you
  already searched**."* The model quoted that clause back, near-verbatim, at
  every step of both storms. **And the exemption never existed in the runtime:**
  the A3 gate keys on `lastSearchKnowledgeViableHit != null` and the cross-turn
  gate on UC / drift / standing-hit / budget — neither has ever compared query
  content, so a "materially different" query is suppressed exactly like a
  paraphrase. The instructions were advertising behaviour the runtime does not
  have.
- **And the suppression itself was silent.** The gate replayed a payload — in
  `81a08aeb-…` a *stale* one captured on turn 2 for a different question — with
  no indication the call had not run. From inside the loop, a silent replay is
  indistinguishable from a genuinely unhelpful search result.

## What should a good CS agent have done?

Turn 3 asks a self-serve, explanation-class question on a **LIVE** ad the bot has
already looked up, with viable KB hits already in hand. A good CS agent answers
it: give the grounded visibility tips from the retrieved articles (with the
source link), or — if it truly has nothing more — say so honestly and offer a
next step. It must **not** hand the customer to a human on a read-only status
question it has already been answering correctly for two turns, and least of all
with the reason *"I ran out of internal steps"*, which is a runtime bookkeeping
fact the customer neither caused nor can act on.

## Why does this matter?

This is a **user-visible instance of the exact symptom the product owner
reported** — "things it shouldn't rush a human on, it escalates" — on the
cheapest, highest-volume, lowest-risk class of contact (read-only status
lookup). It violates the §5.1 acceptance bars for **wrong containment** and
**over-escalation**, and it is a Runtime-side defect: idempotency, budget and
timeouts are Runtime-owned by Constitution **§1.4**, so a backstop that stops the
tool from running but still spends the work budget is the runtime charging the
customer for work it deliberately did not do. It also mis-attributes the
escalation (`turn_budget_exhausted` on a session with viable evidence and no
failed work), extending the documented reason-mislabel pattern
(`project-faq-overescalate-maxsteps-misstamp`).

## Is this a one-off or a pattern?

**Pattern.** Evidence:

1. **~20% reproduction (1/5) on two independent 5–6 run batches**, before and
   after the falsified first fix, of the same E2E test against a live backend —
   and **three** distinct manifestations of one mechanism:
   the cross-turn byte-identical variant (`81a08aeb-…` turn 3, 4 searches),
   the within-turn paraphrase variant (`f62ad6ce-…` turn 1, 10 searches, 8
   distinct strings), and the within-turn identical variant recorded in the
   test's own comment (`e2e/tests/test_api_e2e.py:572`: "turn 1 spun
   `search_knowledge` ×6 on one query, hit the 6-step budget"). All three end
   `request_handover(turn_budget_exhausted)` while holding viable hits.
   The variation in *which* backstop fires and *whether* the queries are
   byte-identical is incidental; the constant is that the model keeps searching
   because it cannot see that it has already searched.
2. **Structural, not stochastic.** Any turn in which the LLM emits ≥
   `max_tool_steps` suppressible calls before answering exhausts the budget. The
   probability is a function of model stuttering, not of the use case; nothing in
   the loop bounds it.
3. **Three prior deterministic backstops exist precisely because this LLM
   repeats** (`AgentRunLoopImpl.java` comments concede four times that the soft
   signals were empirically falsified), and the run loop's latitude is documented
   as "never used for planning — only for spinning"
   (`docs/proposals/performance_priority_replan_2026-07.md` §1.5).
4. Updates M-Auto-11 WP1's `k=0/11` observation (see the status-update section
   above): the prior 95% upper credible bound was ~16%; the observed E2E rate is
   ~20%.

## Which layer is likely responsible?

**`prompt_projection`** (§3.2 Q3 — *"the LLM chose validly within the available
options, but the projection handed to it was wrong or impoverished (missing
slot, missing candidate, missing diagnostic)"*). The model was asked to judge
whether its next query was materially different from its previous ones, was
given only opaque hashes of those previous queries, and was simultaneously told
that "materially different" is a valid exemption. That is a missing-slot defect
in the exact words of Q3.

*(Revised 2026-07-25 from an initial `infra` classification. The budget
accounting defect described in (a) is real and Q1-shaped, but re-measurement
showed it is not what produces the failure — see the falsified first fix. Q3
precedes Q1 here because the loop is not malfunctioning: it is faithfully
bounding a model that has been given no way to stop.)*

Secondary: **`infra`** for (a), retained as a documented observation only.

- **Not `java_guard`** — no Tier-0 invariant is broken, and none is being added.
- **Not `semantic_planner`** — the LLM's choices were reasonable *given the
  projection it received*: it saw results that did not answer the query it had
  issued and re-issued it. §3.2 Q3 explicitly routes "the LLM chose validly but
  the projection handed to it was wrong or impoverished" away from
  `semantic_planner`.
- **Not `eval_spec`** — this is an E2E product-criterion test on the real API
  path, with no CaseSpec and no rubric to blame; the expected behaviour
  (don't escalate a read-only status question) is not in dispute.
- **Not `product_policy`** — no product decision is required to know that
  "I ran out of internal steps" is not a valid reason to escalate.

**Fix legitimacy:** what the runtime projects into the per-turn context is a
Runtime responsibility (§1.4 trace/eval contract; §3.2 Q3 makes projection
impoverishment its own layer). Reporting, verbatim and without judgement, the
queries the model has already issued and whether each one ran is **state, not
persuasion** — it is the same class of surface as the existing `already_called`,
`terminal_evidence`, and `intake_state` slots, and it adds no keyword list, no
similarity metric, and no per-UC branching. The decision to search remains the
model's (§1.3); it simply becomes a decision made against data instead of
against a blank.

This is explicitly **not** another soft signal of the kind already falsified
twice (Sprint 19/20 for A1, S-Auto-13 for A3) and a third time today (the
`repeat_suppressed.hint`). Those all told the model what to conclude. This tells
it nothing and shows it what happened.

## What should NOT be done?

- Do **not** add a keyword list, query-similarity heuristic, or any per-UC
  if-else to decide when a repeat is "legitimate" (§1.5 / §1.7). The rule must be
  keyed purely on the existing structural fact *"did any tool actually execute
  this step"*.
- Do **not** attempt to fix this by rewriting the prompt to tell the model not to
  repeat. That is precisely the soft-signal route already falsified twice
  (Sprint 19/20 for A1, S-Auto-13 for A3 — the `search_reuse_instruction` echo
  is *still in the projection* on the failing turn and was ignored; worse, its
  "materially different query" clause is what the LLM cited as licence to
  retry).
- Do **not** lower `max_tool_steps` (or any budget). That makes the failure fire
  *sooner*; it is the opposite of a fix.
- Do **not** lower or remove `CROSS_TURN_SUPPRESSION_BUDGET = 1`
  (`AgentRunLoopImpl.java:87`). Its comment states that lowering it "re-introduces
  误杀 and is forbidden". The suppression behaviour is correct and must remain
  byte-identical; only its **accounting** and its **visibility to the LLM** are
  the defect.
- Do **not** overwrite the served `accumulated_tool_results.search_knowledge`
  payload with an `error` wrapper to "signal" the repeat.
  `SkillGuardrailDispatcher.handleFaqMissHandoverRequiresResolveAttempt`
  (`SkillGuardrailDispatcher.java:241-258`) reads that map and **lets a handover
  through** when it contains `error` or lacks `hits` — destroying the grounding
  evidence would make over-escalation *easier*. Any repeat diagnostic must be a
  strict superset of the served payload.
- Do **not** grant an unbounded budget refund. Termination is a Runtime §1.4
  duty; any grace must have a fixed absolute ceiling.
- Do **not** "fix" it by widening the E2E assertion to tolerate the handover
  (§5.4 / §1.7).

- Do **not** cap the number of searches a turn may contain, and do **not** turn
  the Nth search into a hard failure. Repeated retrieval is not provably wrong
  behaviour; the runtime already stops the redundant *retrieval* (the KB was hit
  once in both sessions). Hard-failing the count is the documented
  PARAPHRASE_STORM 误杀 trap: *when a detector conflates a not-provably-wrong
  behaviour with failure, demote that sub-class to observation; do not lower a
  safety budget or mask the count to make a number look good.*
- Do **not** refund the tool-step budget for suppressed calls. **Measured and
  falsified — see below.**

## Falsified first fix (2026-07-25, withdrawn same day)

The first attempt treated this as a pure budget-accounting defect: a step in
which every call was backstop-served executed no tool, so it was not charged
against `maxToolSteps`, with the loop ceiling raised to `maxToolSteps + 4` to
keep termination bounded.

**Re-measurement over 6 fresh E2E runs killed it.** 4 passed / 2 failed; one
failure was infra flake (3.9 s, three turns of `tools=[]`, `uc=None`), leaving
**1/5 ≈ 20% — unchanged from before the fix.** Session `f62ad6ce-…` shows why:
because the first search landed a viable hit, A3 suppressed *every* later search
in the turn, so **essentially the whole turn became uncharged and ran to the
absolute ceiling — `10 = 6 + 4`, exactly the constant**. The refund bought the
storm four extra LLM calls and four extra chances to re-phrase, and changed no
outcome.

**Lesson, recorded so it is not re-attempted:** a backstop serve is not "nothing
happened" — it is evidence the model is spinning. Refunding it rewards the spin.
The loop bound is restored to `step < maxToolSteps`, byte-identical to its
original meaning, and a regression test
(`suppressedRepeatStorm_neverExceedsMaxToolStepsLlmCalls`) now pins it there.
The no-progress classification is kept as **log-only observability** —
`no_progress_steps >> 0` on a turn that hit MAX_STEPS is the storm's signature.

## Fix delivered (2026-07-25, revised)

The fix is now at the `prompt_projection` layer: **give the model the facts its
own reasoning depends on, and stop granting an exemption it cannot evaluate.**

`ContextProjectionBuilder.java`:

1. **New `search_attempts_this_turn` slot.** Every `search_knowledge` call
   already issued in the current `AgentRunLoop.run(...)`, in order, with the
   query **verbatim** and the honest execution state: `at_step`, `query`,
   `executed` (false when a backstop served it), `suppression`
   (`a1_identity_cache` / `within_turn_faq_hit` / `cross_turn_standing_hit`),
   `served_from` (which step or turn's result was replayed), plus the result
   state actually handed back (`faq_miss`, `hit_count`, `top_source_ids`).
2. **New `search_attempts_summary` slot:** `attempts`,
   `knowledge_base_queries_executed`, `served_without_executing`,
   `distinct_query_strings`. On the `f62ad6ce-…` turn this reads
   *10 / 1 / 9 / 8* — the shape of the failure, stated as fact.
   `distinct_query_strings` is plain set cardinality of the verbatim strings;
   the runtime does **not** rule on whether two queries mean the same thing
   (§1.3 keeps that with the LLM).
3. **`search_reuse_instruction` rewritten.** The exemption clause the model
   quoted back — *"...or your new query is materially different from what you
   already searched"* — is **removed**. The existing prohibition ("Do NOT call
   search_knowledge again this turn — draft your grounded answer via
   resolve_article...") is preserved verbatim, and two factual statements are
   added: a pointer to `search_attempts_this_turn`, and the mechanical
   consequence — *re-issuing search_knowledge in any wording does not retrieve
   anything new; the runtime serves the same result back without querying the
   knowledge base, and the attempt still consumes one of your remaining tool
   steps.*

`AgentRunLoopImpl.java`:

4. **Per-call `repeat_suppressed` diagnostic retained** —
   `{tool_not_executed, backstop, served_from, identical_serve_count, hint}`
   added to a **copy** of the served payload in `accumulated_tool_results`
   (never to the `ToolEvent`, so the trace contract is byte-unchanged; added as
   a new key so `hits` / `faq_miss` / absence-of-`error` survive for every
   existing reader, including the `SkillGuardrailDispatcher` handover
   predicates). **Recorded honestly: this alone did not work** — it was
   delivered at steps 3 and 8 of `f62ad6ce-…` and ignored both times. It is a
   soft signal and is kept beneath the factual layer, per the repo's
   soft-signal-first convention, not as the fix.
5. **Budget refund withdrawn**, loop bound restored (see above).

Suppression semantics, `CROSS_TURN_SUPPRESSION_BUDGET = 1`, `max_tool_steps`,
every skill/config/prompt resource, and the trace schema are untouched.

**Tests:** `server/src/test/java/com/gumtree/csagent/service/runtime/AgentRunLoopIdempotentRepeatBudgetTest.java`
(8 tests): the no-widening regression guard driven by the observed 10-query
storm; the `search_attempts_this_turn` / `_summary` contract; the assertion that
"materially different" is gone from the instruction; two negative controls that
the work budget is unchanged for real dispatches and for rejections; an
anti-误杀 control that a re-issued search is **never** rejected and never gains
an `error` key; the cross-turn shape; and helper-level coverage.

## Both copies of the exemption are now retired (human-authorized 2026-07-25)

The clause existed **twice** and reached the model through two independent
channels. The projection copy was removed first; the human then lifted the
`server/src/main/resources/skills/` fence on the grounds that a half-withdrawn
licence makes the measurement uninterpretable — a null result could not be
attributed between "the factual projection does not help" and "the surviving
half is still encouraging retries".

`resolve_faq_grounded_answer.yaml:31` `grounding_instruction` now ends:

> *"After a search_knowledge returns a viable hit (faq_miss=false), do NOT
> re-search this turn — draft your grounded answer from the existing hits via
> resolve_article, or escalate; a fresh search_knowledge is only warranted if
> the prior result was faq_miss=true."*

**Only the re-phrase disjunct was deleted.** The `faq_miss=true` carve-out is
kept and now explicitly pinned by a test, because that one is *true*: a
non-viable result clears the A3 tracker and a fresh search really does run. The
surrounding `grounding_instruction` is otherwise byte-unchanged — no rewrite.

### Why this is not a §1.5 / §1.7 violation

Two independent grounds, the second stronger than the first:

1. **Narrowing, not adding.** The edit deletes one disjunct from an existing
   `only warranted if A or B` condition, leaving `only warranted if A`. The
   prompt gets shorter and has strictly fewer branches. No keyword, regex,
   enum, if-else, or per-UC rule is introduced; nothing previously owned by the
   LLM moves to the runtime. §1.5 and §1.7 both prohibit *widening* the
   deterministic/rule surface to paper over a semantic failure — this moves the
   opposite way. Structurally the same move as the WS-1
   `escalation_intent.py` partition ("the pre-WS-1 patterns were partitioned,
   not extended").
2. **The clause was factually false about this runtime — so this is a
   projection-accuracy fix, and §1.5/§1.7 are not engaged at all.** The A3 gate
   fires on `"search_knowledge".equals(toolName) && lastSearchKnowledgeViableHit
   != null`, and the cross-turn gate on UC / drift / standing-hit / budget.
   **Neither has ever compared query content.** A "materially different" query
   is suppressed exactly like a paraphrase. The skill was advertising an
   exemption the runtime does not implement, and the model relied on it. Telling
   the LLM the truth about what the runtime does is a §1.4 duty, not a semantic
   rule change.

Ground (2) is the one to cite: it makes the edit a correction rather than a
policy choice, and it is verifiable from code rather than from judgement.
