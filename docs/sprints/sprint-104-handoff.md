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

Two commits, `67ec96c5` (item 3) and `7c3b2e80` (item 2). Nothing outside
`server/**` plus this handoff.

| file | change | layer + why |
|---|---|---|
| `PhaseEvaluator.java` | removed the `clarificationCount > 0 → clarification_budget_exhausted` branch from `resolveMaxStepsReason`; javadoc records the removal and the Codex-2026-05-03 rationale it supersedes | `infra`. Reason-attribution correctness. §1.4 trace contract. Re-attribution among **existing** canonical values — no enum value added, renamed or removed (frozen: `runtime_freeze_and_risk_policy.md` §1.2.3 / §1.3.5) |
| `discover_triage.yaml` | `procedure` + `escalation_policy`: the literal "after one clarifying turn" / "after one clarification" replaced by a pointer to the projected budget; added a mechanical statement of what the runtime does at the cap | `prompt_projection`. Removing a hardcoded count that contradicted `control-policy.yaml` — the accepted number is now declared in exactly one place |
| `ContextProjectionBuilder.java` | `budgets.clarification` gains `remaining`, `counts_toward_used`, `on_exhaustion`; scope unchanged (DISCOVER-only) | `prompt_projection`. §3.2 Q3 missing-diagnostic; the `6906577d` / P1-04 remedy shape |
| `PhaseEvaluatorMaxStepsResolverTest.java` | 1 test replaced (asserts the opposite outcome for the same input), 2 added | — |
| `PhaseEvaluatorSkillIntegrationTest.java` | golden DISCOVER `procedure` + `escalation_policy` literals **extended** with the new sentences; `assertEquals` on the exact string retained | — |
| `DiscoverClarificationBudgetProjectionTest.java` | new, 8 tests | §5.7 wiring evidence only |

**Anti-hardcode check over the diff**, as required by §4:

```
$ git diff --name-only | grep -E "DriftDetector|RuntimeIntentClassifier|risk-keywords"
(no hits)
$ git diff -- server/src/main/resources/config/control-policy.yaml
(empty — no budget value changed)
```

No keyword, regex, enum expansion, similarity metric or per-UC branch was
added. One test (`slotIsFactual_carriesNoInstructionToCommitOrEscalate`) fails
if a future edit turns the new slot into advice.

## 3. Test numbers

| suite | before | after | attribution |
|---|---|---|---|
| `cd server && mvn -o test` | 1493 run / 1 F / 0 E / 2 skipped | **1503 run / 1 F / 0 E / 2 skipped** | +10 tests (8 new projection, +2 net resolver). The single failure is the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`. **0 self-introduced.** |
| `cd eval_interactive && uv run pytest -q` | 764 passed / 14 failed / 5 skipped | **764 passed / 14 failed / 5 skipped** | Byte-identical failure list. **0 self-introduced.** Nothing under `eval_interactive/**` was touched. |

Both baselines were recorded before the first edit (commit `aedfbcb7` contains
only the characterisation).

**The pytest baseline needed a correction the contract does not mention, and it
will hit sprints 105–107 identically.** A freshly-created worktree measures
**763 passed / 15 failed / 5 skipped**, not 764/14/5. The extra failure is
`tests/test_transcript_evidence.py::test_cs_interactive_001_like_evidence_detects_unresolved_investigation`,
which dies on `FileNotFoundError: data/eval_datasets/badcase_turns.csv`.
`.gitignore:8` ignores `data/*`, so `git worktree add` does not carry the
`data/` tree at all. Symlinking `data/eval_datasets` and `data/filtered` from
the primary checkout reproduces the contract's 764/14/5 exactly. This is an
environment artefact, not a regression, and I did not "fix" the 14.

3 self-introduced failures appeared mid-sprint and were resolved before the
final run: the golden DISCOVER `PhasePlan` literals in
`PhaseEvaluatorSkillIntegrationTest` pin the whole `discover_triage.yaml`
procedure string, so the Skill edit broke them. They were fixed by **extending**
the expected literal with the added sentences; the `assertEquals` on the exact
string is unchanged.

## 4. Behaviour evidence

### 4.1 How it was run

Backend restarted from this worktree after the final server change, under
`caffeinate -dimsu`, and proven to be serving changed code three ways:

- process classpath root is `/Users/caoruixin/projects/csagent-wt-104/server/target/classes`
  (the stale Sprint 103 JVM on `:8080`, running from the primary checkout for
  1 h 31 m, was stopped first);
- `javap -c -p` on the served `ContextProjectionBuilder` shows the new
  `on_exhaustion` / `counts_toward_used` / `remaining` constants;
- `javap -c -p` on the served `PhaseEvaluator` shows the only remaining
  `clarification_budget_exhausted` reference is the `CANONICAL_ESCALATION_REASONS`
  `Set` literal, not the removed branch.

The backend needed `DB_PORT=5442` passed explicitly — no `DB_*` key exists in
`.env.local` and `application-local.yml:3` defaults to `5432`, where a
different postgres is listening; without it Flyway fails with
`password authentication failed`. Recorded in §5.

Six runs, `--parallel 1`, one attempt per case per invocation:

| run dir | label | specs |
|---|---|---|
| `20260725-182254` | `s104-draw1` | `promotion/cs_interactive_179`, `promotion/cs_interactive_185` |
| `20260725-182528` | `s104-draw2` | same |
| `20260725-182742` | `s104-draw3` | same |
| `20260725-183008` | `s104-negctl` | `anchor/cs_interactive_030`, `anchor/cs_interactive_155` |
| `20260725-183105` | `s104-neighbors` | `promotion/cs_interactive_263` (UC-F), `promotion/cs_interactive_183` (UC-C) |
| `20260725-183438` | `s104-negctl030-rerun` | `anchor/cs_interactive_030` (the first attempt returned `status=ERROR`) |

Run-id timestamps read `20260725-18xxxx` while the host clock read
`2026-07-26 02:2x` — the ~8 h skew Sprint 103 §6.5 recorded. Directories are
cited exactly as produced; ids are not used to order runs.

**Worktree state when these ran:** `HEAD = 7c3b2e80`,
`HEAD:eval_interactive/case_specs = 06f526ab27c91ce1584d954a650aa7be15b0e619`,
`case_specs/` clean. Sprint 106 is rewriting `promotion/` expectations in
parallel, possibly including `cs_interactive_185`'s `primary_uc`; this evidence
is valid against the specs measured here and must be re-attributed at
integration.

Adjudication is by transcript and per-turn phase trace read from
`bot_turns` / `bot_turn_llm_calls` / `bot_sessions`. The programmatic composite
was **0.0000 on all 12 sessions** and is not used (replan §1.3; Sprint 105 is
fixing it in parallel).

### 4.2 `cs_interactive_185` — the spec this sprint was scoped from

| draw | Sprint 103 trace / reason | Sprint 104 trace / reason | `clar` |
|---|---|---|---|
| 1 | `DISCOVER×2; DISCOVER→ESCALATE[UC-A]` (3 turns) — `clarification_budget_exhausted` | `DISCOVER; DISCOVER→RESOLVE[UC-D]; RESOLVE×2; RESOLVE→ESCALATE[UC-D]` (**5 turns**) — `agent_unable_to_resolve` | 2 → **1** |
| 2 | `DISCOVER; DISCOVER→RESOLVE[UC-D]; RESOLVE→ESCALATE[UC-D]` (3) — `clarification_budget_exhausted` *(mis-stamp, §1.4)* | `DISCOVER; DISCOVER→RESOLVE[UC-D]; RESOLVE; RESOLVE→CONFIRM[UC-D]; CONFIRM→ESCALATE[UC-D]` (**5 turns**) — `agent_unable_to_resolve` | 1 → 1 |
| 3 | `DISCOVER×2; DISCOVER→ESCALATE[UC-D]` (3) — `clarification_budget_exhausted` | `DISCOVER×2; DISCOVER→ESCALATE[UC-D]` (3) — `clarification_budget_exhausted` | 2 → 2 |

**2 of 3 draws changed; 1 did not.** In the two that changed, the session left
DISCOVER after **one** clarification instead of two, reached RESOLVE (and CONFIRM
in draw 2), ran 5 turns instead of 3, and ended on the LLM's own semantic reason
rather than being terminated by the runtime before it was consulted.

The mechanism is visible in the trace. Draw 1, turn 2 — the drift turn — the
projection carried `remaining: 1` and the LLM committed instead of asking again
(`bot_turn_llm_calls`, `turn_index=2`, `step_index=0`):

> `{"user_message": "", "reasoning": "User explicitly states login issue (UC-D)
> and ad renewal question (UC-A). Both are clear intents. Since login is the
> primary blocker, classify as UC-D with high…"}`

with `tool_calls = classify_use_case, search_knowledge, get_customer_context,
resolve_article`. Because the turn called `classify_use_case`, `used` stayed at
1, the cap never fired, and the conversation continued:

> **[2] USER:** It's a login issue — I can't get into my account at all. Also,
> one of my ads is about to expire and I need to renew it. Can you check if it's
> still live?
>
> **[2] BOT:** I'm sorry you're having trouble logging in, Trevor. Let me look
> into this for you. Could you please provide the ad ID or a link to the ad
> you're concerned about so I can check its status?
>
> **[3] USER:** My ads are under [EMAIL] — I had [EMAIL] but Microsoft blocked
> it, so I'm using this one now. One ad is for a sofa, the other's a bike — both
> still live, right?
>
> **[3] BOT:** I understand you're having trouble logging in with your new email,
> Trevor. Unfortunately, I wasn't able to find an account under the email you
> provided. Could you please share the ad IDs or links for the sofa and bike ads
> so I can…
>
> **[4] USER:** Actually — while we're at it — can you help me recover my old
> account? …
>
> **[4] BOT:** I understand you're worried about losing your ads, Trevor. …To
> help recover your old account, I'll need the original email address that was
> blocked by Mi…
>
> **[5] BOT:** Let me connect you with a specialist.

Contrast Sprint 103's draw 1 on the same spec (§1.2 above): the customer's
turn-3 answer — the ad title the bot had just asked for — was never seen,
because the runtime force-escalated first.

**Draw 3 saw the same information and chose differently.** Its turn-2 projection
also carried `remaining: 1`; the LLM recognised the drift explicitly —
*"It sounds like you're dealing with two issues: trouble clearing cookies in
Chrome on Windows, and a login issue with a new email"* — and still answered in
free text at both steps, spending the second round and dying on turn 3. This is
the honest limit of the change: **the projection made the correct action
informed and available; it did not compel it**, which is what §1.3 requires and
why the result is 2 of 3 rather than 3 of 3.

### 4.3 `cs_interactive_179` — never a clarification-budget case

179 starts in RESOLVE (strong-prior `Replies & Messaging` → UC-C, Sprint 103
§1.1) and reached `clar > 0` in none of its six draws across both sprints. It
cannot test this sprint's mechanism.

| draw | Sprint 103 | Sprint 104 |
|---|---|---|
| 1 | 5 turns — `agent_unable_to_resolve` | 4 turns, `RESOLVE→RESOLVE; RESOLVE→CONFIRM; CONFIRM→CONFIRM[UC-D]; CONFIRM→ESCALATE[UC-D]` — `user_distress` |
| 2 | 2 turns — **`turn_budget_exhausted`** *(mis-stamp)* | 2 turns — `agent_unable_to_resolve` |
| 3 | 3 turns — `agent_unable_to_resolve` | 3 turns, `CONFIRM→ESCALATE[UC-D]` (8 tool calls) — `faq_miss_threshold_exceeded` |

No `turn_budget_exhausted` appeared in any Sprint 104 session. **This is not
evidence that item 3 fixed anything** — see §4.6.

### 4.4 Neighbours — no improvement

| spec | trace | reason | `clar` |
|---|---|---|---|
| `promotion/cs_interactive_263` (UC-F) | `DISCOVER×2; DISCOVER→ESCALATE[UC-D]` | `clarification_budget_exhausted` | 2 |
| `promotion/cs_interactive_183` (UC-C, `hard_shift`, resolve-class — my choice per the stanza) | `DISCOVER×2; DISCOVER→ESCALATE[UC-D]` | `clarification_budget_exhausted` | 2 |

Both die exactly as `cs_interactive_263` died in Sprint 103 (`e50f884a`). **The
neighbours did not move at all.** Whatever produced the 2-of-3 change on
`cs_interactive_185` did not generalise to either of them in one draw each.

### 4.5 Negative controls

| spec | Sprint 103 | Sprint 104 | verdict |
|---|---|---|---|
| `anchor/cs_interactive_030` | 3 turns, UC-C throughout, `user_requested`, `clar` 0 | 4 turns, `RESOLVE×3; RESOLVE→ESCALATE[UC-C]`, `user_requested`, `clar` **0** | **pass** — no extra rounds, same stop reason, no UC change |
| `anchor/cs_interactive_155` | `DISCOVER×2; DISCOVER→ESCALATE[UC-D]`, `clarification_budget_exhausted`, `clar` **2** | `DISCOVER→RESOLVE[UC-D]; RESOLVE; RESOLVE→ESCALATE[UC-D]`, `user_requested`, `clar` **0** | **mixed — reported, not explained away** |

The contract's bar has two halves. `cs_interactive_155` passes the first
(it did not *gain* clarification rounds — it used 2 fewer) and **fails the
second**: its stop reason changed. The new reason is the semantically correct
one for what happened (the customer asked for a human, and `user_requested` is
priority 1), and the session left DISCOVER on turn 1 with a real tool call
rather than clarifying twice. But at n = 1 draw per control I cannot separate
that from draw noise, and I am not going to claim it as an improvement. It is a
changed negative control.

`cs_interactive_030`'s first attempt returned `status=ERROR` after one turn and
is reported as UNMEASURED; the re-run above is the measured result.

### 4.6 What did **not** move

- **No session resolved. 0 of 6 targets, against a baseline of 0 of 6.** The
  primary business outcome is unchanged, exactly as in Sprint 103.
- **Bot-initiated escalation did not fall.** 6 of 6 target sessions still ended
  in escalation. What changed is *which* escalation: 2 of 3 `cs_interactive_185`
  draws moved from a runtime-imposed pre-emptive terminal to the LLM's own
  `agent_unable_to_resolve` after five turns of engagement. That is a better
  failure, not a success.
- **Both neighbours were unaffected** (§4.4). The change is demonstrated on one
  spec, in 2 of 3 draws.
- **Item 3's fix has no live behavioural instance in this run.** Its trigger is
  a MAX_STEPS exit on a session with `clar > 0`; no Sprint 104 session hit that
  combination (the two with `clar = 1` ended on LLM-selected reasons, not
  MAX_STEPS). Its evidence is the unit tests plus the retrospective Sprint 103
  session `32debe76`, not a new session. I am not claiming the absence of
  `turn_budget_exhausted` in §4.3 as its effect — that catch-all is untouched by
  my change and the absence is a property of these six draws.
- **n = 3 draws × 2 specs, 1 draw × 2 neighbours, 1 draw × 2 controls.** This
  cannot establish a rate, cannot show elimination, and cannot distinguish the
  neighbours' null result from insufficient sampling.

## 5. Real defects found, out of contract — listed, not fixed

1. **`turn_budget_exhausted` has no correct replacement in the frozen enum —
   proposal, not implementation.** Per the contract, item 3's catch-all needs a
   value meaning "the per-turn tool-step budget ran out", and none of the 24
   canonical values means that. `system_prompt.txt:79` defines
   `turn_budget_exhausted` as *"Hit the **per-session** turn budget limit"*, so
   the runtime contradicts its own prompt on every MAX_STEPS exit.
   `agent_unable_to_resolve` cannot absorb it — `PhaseEvaluator.java:55-57`
   states it is never auto-stamped by the runtime, and `system_prompt.txt:83`
   excludes cases that "actually hit a turn / clarification / FAQ-retrieval
   budget cap".
   **Proposed:** `tool_step_budget_exhausted`, priority **43**, i.e. directly
   below `turn_budget_exhausted` (42) inside the Tier-4 budget family, and added
   to `TERMINAL_CLOSE_REASONS`. Not implemented: the enum is frozen
   (`runtime_freeze_and_risk_policy.md` §1.2.3 / §1.3.5) and the change
   cross-cuts the eval-side `ESCALATION_TRIGGER_VALUES`, so it needs the
   coordinated migration §1.3.5 requires.
2. **The structurally correct fix for item 2 is a per-issue clarification budget,
   and it is explicitly deferred.** `runtime_freeze_and_risk_policy.md` §1.3.2
   defers "per-issue clarification budget, per-issue FAQ-miss budget, per-issue
   stall counters" and requires "a new objective doc, scoped acceptance
   criteria, and a new runtime sprint". My §1.2 evidence is precisely the case
   for it: the budget is per-session while the need is per-issue. I did not take
   it; this is the sprint that should be opened next on this failure shape.
3. **A turn can increment `clarificationCount` even after dispatching a tool.**
   `AgentRunLoopImpl:592` passes `hasToolCalls = false` as a literal because the
   branch is per-*step*, not per-turn. A turn that calls a tool at step 0 and
   emits free text at step 1 still increments. Visible on `5f6810c4` turn 2
   (1 tool call, `clar` still went 1 → 2). Not touched — it is arguably correct
   (the turn did end in a question), but it is not what the predicate's javadoc
   describes.
4. **The backend cannot start from a clean checkout without an undocumented
   `DB_PORT=5442`.** No `DB_*` key exists in `.env.local`;
   `application-local.yml:3` defaults to `5432`, where a different postgres is
   listening, so Flyway fails with `password authentication failed for user
   "postgres"` — a misleading error for a wrong-port problem. Worth a `.env.local`
   key or a Makefile default.
5. **A fresh `git worktree` silently changes the pytest baseline** (§3). `data/*`
   is gitignored, so worktree-based sprints measure 15 failures, not 14. Sprints
   105–107 are in parallel worktrees and will hit this.
6. **`cs_interactive_185`'s `primary_uc` still looks wrong for its own persona**
   — Sprint 103 §6.6 recorded this for WS-2. Reconfirmed: the spec expects UC-D,
   and in all three Sprint 104 draws the committed UC was UC-D but the drifted
   asks were ad-renewal / ad-recovery (UC-A/UC-B territory). Recording again for
   Sprint 106; `case_specs/**` untouched.

## 6. Where I think this contract is wrong

1. **The stated base tip is wrong.** The contract says `d7d84f86`; the branch
   tip is `82f8a137`, the commit that added the contract itself. Following the
   instruction to verify `git log` "must read d7d84f86" would have blocked the
   sprint at step 0.
2. **The core premise is wrong about the mechanism, though right that there is a
   defect.** "That new need consumes a clarification round… drift therefore
   *accelerates* budget exhaustion" describes drift-awareness the counting path
   does not have (§1.2). Nothing charges a drifted turn differently. Taken
   literally, the contract's item 2 asks for a fix to accounting that has no
   place to attach — there is no drift-specific charge to remove. The real
   defect is that the counter never registers a *successful* clarification, so a
   converging session is killed at the same rate as a looping one. I re-scoped
   items 2–3 around that, as §3 item 1 permits.
3. **The contract inherits a contaminated statistic from Sprint 103 §6.2 and
   builds the sprint on it.** "5 of the 10 measured sessions escalated with
   `clarification_budget_exhausted`" is true as a count of stamps and false as a
   count of clarification-budget events: only 4 are (§1.4). One of the five is
   an instance of the *other* defect the same contract asks me to fix in item 3.
   The two items are not independent, and the contract treats them as if they
   are.
4. **The pytest baseline is unreachable in the worktree the contract tells me to
   create** (§3, §5.5). The contract's "these numbers may be stale — re-measure
   and attribute the delta" instruction is what saved this; it is good
   instruction and it worked.
5. **Item 3 cannot be delivered as scoped.** It says "implement only if it is a
   re-attribution among existing values", and for `turn_budget_exhausted`
   specifically there is no existing value that is correct — so the item reduces
   to a proposal (§5.1). What *was* implementable in that function turned out to
   be a different mis-stamp the contract did not know about. Item 3 as written
   produces no code; item 3 as the evidence found it produces one clean fix.
6. **Minor, confirming Sprint 103 §7.3:** ~55–75 s per session measured here
   (two-case runs in 99–153 s), against the contract's "roughly 60 s" — which is
   right, and the "4–9 min" figure it corrects was indeed ~5× off.

## 7. Self-check

- [x] Worktree `../csagent-wt-104` on `sprint-104-discover-budget`; nothing
      written outside `server/**` + this handoff (`git status` clean).
- [x] Both baselines recorded before the first edit (commit `aedfbcb7` is
      characterisation-only); self-introduced failures **0** in both suites.
- [x] Item 1's characterisation written down and committed **before** the fix,
      with quoted code and quoted trace evidence — including the part that
      falsifies the contract's premise (§1.2).
- [x] `git grep` over the diff: no `DriftDetector`, no `RuntimeIntentClassifier`,
      no `risk-keywords.yaml`; `control-policy.yaml` untouched, no budget value
      changed.
- [x] Backend restarted from this worktree after the final server change; served
      code proven by classpath root + `javap` on two classes.
- [x] 3 draws × 2 targets + 2 negative controls (one re-run after an ERROR) +
      2 neighbours; adjudicated by transcript and phase trace, not composite.
- [x] What did not move stated as plainly as what did (§4.6): no session
      resolved, escalation did not fall, neighbours unaffected, item 3 has no
      live instance.
- [x] Golden tool-set / declaration assertions: none needed changing (no tool
      declaration was touched, per §1.5). The golden DISCOVER text assertions
      were **extended**, not relaxed.
- [x] No Codex dispatched; no merge; no other sprint's paths touched; nothing
      under `eval_interactive/**`, `eval/**`, `data/**`, `case_specs/**`,
      `autoloop/**`, `e2e/**`.
