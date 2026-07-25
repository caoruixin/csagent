---
title: "Sprint 104 / WS-6-B handoff — the DISCOVER clarification budget kills converging sessions"
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: cited code paths + persisted turn records for the 10 Sprint 103 sessions (postgres :5442) + eval_interactive runs cited in §4
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Item 1's characterisation FALSIFIES part of this sprint's contract premise
  (§1.4). The clarification counter is not drift-aware and never was; drift is
  a contributing condition, not the mechanism. Two distinct escalation-reason
  mis-stamps were found in one function, one of which contaminated the very
  statistic this sprint was scoped from. Read §1 before §2.
---

# Sprint 104 / WS-6-B handoff

Branch `sprint-104-discover-budget`, worktree `../csagent-wt-104`, based on
`perf-replan-2026-07` @ `82f8a137`.

> **Contract said the base tip was `d7d84f86`.** It is `82f8a137` — the
> contract's own commit ("docs: four parallel sub-sprint contracts…") landed
> after the text was written. `82f8a137` is the correct base; it contains this
> contract. Recorded per §8.6.

## 1. Item 1 — characterisation, written before any file was changed

### 1.1 (a) Where a clarification round is counted, and where it is enforced

**Counted — exactly one site.** `AgentRunLoopImpl.java:592-596`:

```java
if (isDiscoverFreeTextClarification(
        plan.phase(), false, ucCommittedThisTurn, userMsg)
        && !action.isUserMessageSynthesised()) {
    session.setClarificationCount(session.getClarificationCount() + 1);
}
```

The predicate (`AgentRunLoopImpl.java:1497-1505`) is purely structural:

```java
static boolean isDiscoverFreeTextClarification(String phase,
                                               boolean hasToolCalls,
                                               boolean ucCommittedThisTurn,
                                               String botReply) {
    return DISCOVER_PHASE.equalsIgnoreCase(phase)
            && !hasToolCalls
            && !ucCommittedThisTurn
            && botReply != null && !botReply.isBlank();
}
```

**Enforced — exactly one site.** `BudgetChecker.java:32-37`:

```java
if (session.getClarificationCount() >= controlPolicy.getMaxClarificationRounds()) {
    ...
    return Optional.of("max-clarification-rounds");
}
```

called from `ControlKernel.processMessage` **Step 3** (`ControlKernel.java:310`),
which force-escalates on a hit (`:334-339`). `forceEscalate`
(`ControlKernel.java:1317`) transitions to ESCALATE, sets
`handling_state=QUEUE_TO_HUMAN`, and synthesises the turn. **It never builds a
projection and never invokes the LLM.**

`control-policy.yaml:2` — `max-clarification-rounds: 2`. Unchanged by this
sprint.

The resulting session arithmetic, confirmed against the persisted records:

| turn | budget check at Step 3 | LLM runs? | bot free-text in DISCOVER | count after |
|---|---|---|---|---|
| 1 | `0 >= 2` false | yes | yes | 1 |
| 2 | `1 >= 2` false | yes | yes | 2 |
| 3 | `2 >= 2` **true** | **no — force-escalated** | — | 2 |

### 1.2 (b) Is a drift-raising customer turn charged a clarification round?

**No — and this falsifies the contract's stated mechanism.**

The contract says: *"When the customer raises a second, different need while
DISCOVER is still clarifying the first, that new need **consumes a
clarification round**… Drift therefore *accelerates* budget exhaustion."*

What the code actually does: **the customer's turn is not what is charged. The
bot's own outgoing free-text reply is** — one per DISCOVER turn,
unconditionally. `isDiscoverFreeTextClarification` takes only
`(phase, hasToolCalls, ucCommittedThisTurn, botReply)`. There is no drift input
on the counting path, no call into `DriftDetector`, and no way for the counter
to know whether the bot is clarifying need #1 or need #2. A drifted turn is
charged **exactly the same as** a non-drifted turn.

So drift does not *accelerate* anything. What is true is weaker and more
structural: **`max-clarification-rounds` is a per-session counter applied to
what is semantically a per-need question quota.** Two needs, one question each,
budget gone — not because drift was penalised, but because the budget was never
per-need. That distinction matters, because it kills the fix the contract's
framing implies ("don't charge the drift turn"): there is nothing drift-specific
to not-charge.

**And the real defect is worse than either framing.** In all four sessions that
genuinely exhausted the budget, the force-escalated turn is the turn on which
**the customer supplied exactly what the bot had just asked for**:

| session | case | turn-3 customer message (verbatim, truncated) | what it is |
|---|---|---|---|
| `92fa3506` | 185 draw 1 | "The ad is 'Samsung Galaxy S23 Ultra - Unlocked - Excellent Condition'. It's been live for about 3 weeks." | answers the bot's slot request |
| `294004d6` | 185 draw 3 | "The ad is 'Vintage Record Player - Working, Great Condition' — it's been live for 3 days. I need to update the price and add a photo." | answers the bot's slot request |
| `b3b414f2` | 155 neg-ctl | "The email is [EMAIL]. I've checked spam and promotions folders — nothing." | answers the bot's slot request |
| `e50f884a` | 263 neighbour | "Both — I need to know why it was removed, and yes, I'd like a refund…" | answers the bot's disambiguation question |

Draw 1 of `cs_interactive_185` is the cleanest instance. Turn 2 is the drift —
and the bot handled it correctly:

> **[2] USER:** I just realised my two live ads are still up — but I need to
> edit the contact number on one of them. Can you help me update that?
>
> **[2] BOT:** Sure, Trevor! I can help with that. To edit the contact number on
> one of your live ads, I'll ne…

That is engagement plus a single focused slot request — the behaviour the
failure brief asks for. The customer then answered it. The runtime killed the
session on turn 3 before the LLM saw the answer, and stamped
`clarification_budget_exhausted`.

**The counter counts the bot's questions and never counts the customer's
answers.** It is monotonic per session; a clarifying question that *works* is
indistinguishable from one that fails. Every one of these four sessions was
strictly converging at the moment it was terminated.

`projected_context` is `NULL` on turn 3 for all four sessions — direct proof
the LLM was never invoked.

### 1.3 (c) What the LLM is told about its remaining latitude

Three things, and two of them are wrong.

**Correct:** `ContextProjectionBuilder.java:915-922` emits, in DISCOVER only:

```json
"budgets": {"clarification": {"used": 1, "max": 2}}
```

Verified live — `92fa3506` turn 1 carried `{"max": 2, "used": 0}`, turn 2
`{"max": 2, "used": 1}`. The counts are accurate.

**Wrong #1 — the Skill states a limit that contradicts the runtime.**
`discover_triage.yaml:21` ends: *"Only escalate from DISCOVER if the user
explicitly requests a human, the issue is clearly out of scope, or you cannot
disambiguate **after one clarifying turn**."* `:23` repeats it: *"Escalate if …
you cannot disambiguate **after one clarification**."* The runtime grants
**two**. Confirmed the text reaches the model: the projection persisted for
`92fa3506` turn 2 contains the literal string `cannot disambiguate after one
clarifying turn`.

**Wrong #2 — nothing states the consequence, and the system prompt implies the
opposite one.** `system_prompt.txt:78`: *"User's intent is unclear after
multiple clarifications → `clarification_budget_exhausted`."* That presents the
reason as a **semantic judgement the LLM makes**. The runtime stamps it
**pre-emptively, deterministically, without consulting the LLM**, on a turn
whose intent may be perfectly clear. Nothing anywhere tells the model that at
`used == max` its next turn is terminated before it is invoked — so the model
cannot know that "one more clarifying question" actually means "the last thing
I will ever be allowed to do".

This is the `6906577d` / P1-04 shape exactly: the projection advertises
behaviour the runtime does not have.

### 1.4 Two escalation-reason mis-stamps, one of which contaminated this sprint's own premise

All 10 Sprint 103 sessions, read from `bot_sessions`:

| session | case | turns | `clar_count` | stamped reason | what actually happened |
|---|---|---|---|---|---|
| `92fa3506` | 185 d1 | 3 | **2** | `clarification_budget_exhausted` | genuine — Step-3 force-escalate |
| `294004d6` | 185 d3 | 3 | **2** | `clarification_budget_exhausted` | genuine — Step-3 force-escalate |
| `b3b414f2` | 155 negctl | 3 | **2** | `clarification_budget_exhausted` | genuine — Step-3 force-escalate |
| `e50f884a` | 263 neighbour | 3 | **2** | `clarification_budget_exhausted` | genuine — Step-3 force-escalate |
| `32debe76` | 185 d2 | 3 | **1** | `clarification_budget_exhausted` | **MIS-STAMP** — MAX_STEPS in RESOLVE, 8 tool calls |
| `15a3e0b4` | 179 d2 | 2 | **0** | `turn_budget_exhausted` | **MIS-STAMP** — MAX_STEPS in RESOLVE, 7 tool calls |
| `b08d9c2e` | 179 d1 | 5 | 0 | `agent_unable_to_resolve` | LLM-selected |
| `19375dda` | 179 d3 | 3 | 0 | `agent_unable_to_resolve` | LLM-selected |
| `8c3c5d7a` | 030 negctl | 3 | 0 | `user_requested` | correct |
| `45598049` | 170 neighbour | 1 | 1 | (none) | the Sprint 103 §6.3 ERROR session |

The two families are trivially separable by the bot's final text — Step-3
force-escalate emits *"I've reached the limit of what I can assist with on this
topic."*; a MAX_STEPS exit emits *"I'm having difficulty resolving this."*

**Consequence for this sprint's premise.** The contract (from Sprint 103 §6.2)
states: *"5 of the 10 measured sessions escalated with
`clarification_budget_exhausted`, and 4 of those never left DISCOVER."* The
count of 5 is right; the reading is not. Only **4** are clarification-budget
cases at all. The 5th (`32debe76`) reached RESOLVE on turn 2 and died on a
tool-step storm — it is an item-3 defect wearing an item-2 label. The
"4 of those never left DISCOVER" is therefore not a coincidence to be explained;
it is the definition of the genuine population.

**Both mis-stamps are produced by one function**,
`PhaseEvaluator.resolveMaxStepsReason` (`:214-229`, `:209-211`):

```java
if (session != null && session.getClarificationCount() != null
        && session.getClarificationCount() > 0) {
    return "clarification_budget_exhausted";
}
```

and the catch-all `return "turn_budget_exhausted";`.

**The `clarification_budget_exhausted` branch is provably never true.** Proof,
four steps, all from code:

1. `resolveMaxStepsReason` is called from exactly one place —
   `PhaseEvaluator.java:674`, inside `interpretRunResult`.
2. `interpretRunResult` has exactly one caller in `src/main/java` —
   `ControlKernel.java:501`, which is inside `processMessage` (the only method
   declared between `:264` and `:520`).
3. `processMessage` runs the budget check at `:310` and returns early on a hit.
   So any turn reaching `resolveMaxStepsReason` had `clarificationCount < max`
   at turn start.
4. Within a turn the two outcomes are mutually exclusive: the increment
   (`AgentRunLoopImpl.java:595`) lives in the `calls == null || calls.isEmpty()`
   branch, which **returns** at `:598`/`:601`; `AgentRunResult.maxSteps(...)` is
   only reached at `:1172`, after the `for (int step = 0; step < maxSteps; …)`
   loop at `:416` falls through. A turn cannot do both.

∴ at `resolveMaxStepsReason` time, `clarificationCount` is always `< max`.
Stamping "budget exhausted" there is always a false statement about the runtime.

`15a3e0b4` turn 2, the `turn_budget_exhausted` case, is a search-repeat storm —
7 calls of which 5 are the byte-identical query `unblock user after deleting
conversation`, i.e. the `e2e-2026-07-25-idempotent-repeat-burns-turn-budget`
shape, against a spec with `max_turns: 15` and `total_bot_turns = 2`.

### 1.5 Item 4 — is `propose_reroute` the missing capability in DISCOVER?

**No, and it should not be added there.** In DISCOVER there is no committed use
case to re-route *from*; `classify_use_case` is the correct commit tool and it
is already declared on `discover_triage.yaml:9` and reached the model in every
one of these sessions. Sprint 103 §1.2 established that `classify_use_case`'s
commit semantics are DISCOVER-bound, which is exactly where these sessions are.

The gap is the contract's second branch: **DISCOVER escalates instead of
advancing.** On `92fa3506` turn 2 the LLM had `classify_use_case` available,
had an unambiguous UC-A ask ("edit the contact number on one of my live ads"),
and chose a free-text slot request instead — which the Skill's own confidence
guidance encourages ("below 0.5 … ask one focused clarifying question"), and
which the budget then punished. No tool declaration changes; the golden
tool-set and declaration-matrix assertions are untouched by this sprint.

## 2. What changed, per file

See §2 of the delivered diff below (written after implementation).

## 3. Test numbers

## 4. Behaviour evidence

## 5. Real defects found, out of contract

## 6. Where I think this contract is wrong

## 7. Self-check
