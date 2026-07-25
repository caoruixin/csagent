---
title: "Sprint 103 / WS-6-A handoff — re-routing on intent switch"
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: cited code paths + eval_interactive runs 20260725-165045 / -165354 / -165622 / -165835 / -170011
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Answers failure-brief ws5-2026-07-25-intent-switch-forces-escalation. The
  capability landed and is used by the LLM in production traces; the primary
  behavioural metric moved on ONE of its two halves and not on the other.
  §5 states exactly which, with the attribution limits. Do not read this as
  "the sprint worked" — read §5.4.
---

# Sprint 103 / WS-6-A handoff — re-routing on intent switch

Branch `perf-replan-2026-07`. Commits `f7ff58f5` (runtime), `eb8c1516`
(§0.6 registration), plus this handoff.

## 1. WP1 — design choice: (b), a distinct `propose_reroute` tool

**Chosen: (b).** Two pieces of code decided it, both checked rather than
assumed.

### 1.1 `classify_use_case` is silently refused on one of the two targets

`ClassifyUseCaseTool.shouldPreserveStrongPrior` (`ClassifyUseCaseTool.java:188-206`)
returns true — and `execute` then returns `{committed: false, reason:
"strong_prior_carry_forward"}` without touching the session
(`:121-133`) — whenever the proposed `use_case_id` differs from an active UC
that `UseCaseRouter.deriveStrongPriorUc(topicSubject, description, registry)`
would still derive.

For `promotion/cs_interactive_179` that guard fires on exactly the shift this
sprint targets:

- spec `form_context.topic_subject` = `Replies & Messaging`;
- `UseCaseRouter.TOPIC_ALIASES` (`UseCaseRouter.java:159-161`) normalises that
  to `Replies or Messaging`;
- `use-case-registry.yaml:103` maps `Replies or Messaging` → `UC-C` under
  `strong-prior-topics`;
- the spec's `expected.primary_uc` is `UC-C`.

So `derived.get().equals(activeUc)` holds and every different target is
refused. Option (a) would have shipped a capability that is dead on arrival on
one of the two named targets. Making it work would mean carving an exception
into a guard that protects a separate, still-live defect (the UC rotation on
`cs_interactive_014` documented in that method's own comment).

### 1.2 `classify_use_case`'s commit semantics are DISCOVER-bound

`AgentRunLoopImpl` returns `USE_CASE_IDENTIFIED` only when
`DISCOVER_PHASE.equalsIgnoreCase(plan.phase())`, and `ControlKernel` gates its
same-turn replan on the same pairing. Called mid-RESOLVE, the tool would move
`session.activeUseCase` and the loop would keep executing the *previous* UC's
plan for the rest of the turn: `ToolDispatcher.dispatch` policy-checks against
`session.getActiveUseCase()` — the new UC (`ToolDispatcher.java:104-105`) —
while `validateAgainstPlan` whitelists against the old plan
(`ToolDispatcher.java:190-202`). A UC-D → UC-H re-route would leave
`search_knowledge` plan-allowed but policy-blocked, with the RESOLVE-FAQ
guardrails still firing for a session that is now on an intake UC.

Fixing both would have changed `classify_use_case`'s semantics for DISCOVER
too, which is the one path in this area with a proven track record.

### 1.3 What (b) cost and what it bought

Cost: one new tool class, one `tool-policy.yaml` entry, one
`SkillLoader.VALID_TOOL_NAMES` entry, one `TerminalOutcome` value.

Bought: DISCOVER untouched; the trace can tell an intake-time commit from a
mid-session re-route (event payload carries `via: propose_reroute` +
`previous_use_case_id`); and the runtime owns plan/phase consistency by
returning a distinct terminal outcome the kernel replans on.

**What the runtime decides.** Capability only: is the target a known UC
(`UseCaseRegistryService.isKnownUseCase`), is it different from the active one,
and does a Skill serve `(RESOLVE, target)` (`SkillRegistry.select`). It never
judges whether the customer's new ask "really" belongs to the proposed UC —
that is §1.3's. `ProposeRerouteToolTest.execute_honoursAnyKnownServedTarget_regardlessOfSemantics`
pins that: a target that looks unrelated to the active UC is still honoured.
A declined proposal returns `honoured: false` with a reason, is not an error,
and does not end the turn.

## 2. WP2 — `alternate_candidate_use_cases` alone was not sufficient

**It was not sufficient, and this is provable rather than a judgement call.**

The slot is populated only from `session.getIntakeAmbiguousCandidates()`
(`ContextProjectionBuilder.java:615-626`), and `SessionManager` writes that
field only on the `RoutingResult.AMBIGUOUS` branch at session creation
(`SessionManager.java:192-205`). Every session that routed deterministically —
all strong-prior topics, all B2 phrase-bias hits — leaves it null, so the slot
emits `[]`. That is precisely the population where the intake router was
confident and the customer later changed the subject. `cs_interactive_179` is
in it, by §1.1's chain.

Confirmed live, not only by reading: the last projection persisted for session
`b08d9c2e-e836-433b-896e-1ee2347ca859` (draw 1, `cs_interactive_179`) carries

```
alternate_candidate_use_cases = []
reroute_target_use_cases = [{"id":"UC-B","name":"Posting & Editing Guidance"},
                            {"id":"UC-C","name":"Messages & Replies"}, ...]
tool_schemas = [record_outcome, request_handover, propose_reroute]
```

A second gap compounds it: `candidate_use_cases_named` is gated by
`required_context_keys`, declared only on `discover_triage.yaml`. Outside
DISCOVER the LLM sees UC ids as opaque symbols with nothing saying what they
mean.

**What was added, as fact:** `reroute_target_use_cases` — every use case in the
registry as `{id, name}`, minus the active one. That is the argument domain of
`propose_reroute` plus each id's registry name. It states what the tool accepts
and what the ids mean; it does not say when to re-route. Construction is
registry-driven (`useCaseRegistry.getAllUseCases()`), no per-UC branch, and the
runtime never reads the slot back. The tool schema's `target_use_case` enum is
built from the same registry call rather than a literal list, so
`use-case-registry.yaml` is the only place the accepted id set is declared.

`alternate_candidate_use_cases` was also declared on the three Skills that
carry the tool, per the contract — it is necessary but not sufficient, and it
does carry real information on the AMBIGUOUS-intake population.

## 3. WP3 — where a re-route lands, and the §0.6 entry

**Chosen: in-place re-routing, landing in RESOLVE. No new phase edge.
`CONFIRM → DISCOVER` stays declared-but-dead.**

From RESOLVE there is no transition at all; from CONFIRM the re-route uses
`CONFIRM → RESOLVE`, which `control-policy.yaml:18-22` already declares.
`RerouteDecider.java:105-110`'s assertion that the legal table forbids
`RESOLVE → DISCOVER` is respected — nothing in this sprint introduces that
edge, so code and `control-policy.yaml` do not disagree.

Why not restore `CONFIRM → DISCOVER`: a re-route needs to *act* in the new use
case, and DISCOVER's output is another clarifying question — the exact thing
that exhausted the clarification budget in the observed sessions (and, per §5,
still does). When the LLM genuinely cannot name a target it can already ask a
question without changing phase. Deleting the edge instead would contradict
`phase3_detailed_technical_design.md:534`, another owner's contract; it is
recorded as a known residual rather than silently resolved.

The mechanism is `TerminalOutcome.USE_CASE_REROUTED` + a generalisation of the
existing §M3 same-turn replan block in `ControlKernel` (wall-clock budget gate,
event merging, and `effectivePhaseBefore` promotion all reused unchanged).

**§0.6 deviation entry text**: committed in `eb8c1516`, at
`docs/foundational/phase0_normative_freeze.md` §0.6, titled
`### Deviation 2026-07-26 — 会话中途改路由（propose_reroute）+ 同轮 replan 不再是 DISCOVER 专属`.
It is reproduced there in full rather than duplicated here (one artefact, per
the governance note in the same file). §0.3 markers were added to two rows,
both genuinely affected:

- **"control kernel state machine"** — the transition table is unchanged, but
  `active_use_case` can now change outside DISCOVER and the same-turn replan is
  no longer DISCOVER-exclusive.
- **"V1 tool set (agent_visible)"** — gains `propose_reroute`. While there, the
  entry back-records two tools that were added without registration:
  `classify_use_case` (2026-05-02) and `update_intake_fields` (Sprint 080).

Owner confirmation on the entry is pending; it is filed per the §0.5 procedure,
and the (a)/(b) and landing decisions are the dev agent's, on the evidence above.

## 4. Test numbers

| suite | before | after | attribution |
|---|---|---|---|
| `cd server && mvn -o test` | 1481 run / 1 failure / 0 errors / 2 skipped | **1493 run / 1 failure / 0 errors / 2 skipped** | +12 new tests. The single failure is the inherited `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` (requires the literal `"Sprint 6"` in `system_prompt.txt`). **0 self-introduced.** |
| `cd eval_interactive && uv run pytest -q` | 764 passed / 14 failed / 5 skipped | **764 passed / 14 failed / 5 skipped** | Byte-identical failure list. **0 self-introduced.** |

Both baselines were recorded before any file was changed and matched the
contract's stated numbers exactly.

**13 assertions were updated, none weakened.** They are golden tool-set and
declaration-matrix assertions that pin exact sets I deliberately extended:

- `PhaseEvaluatorResolveSkillIntegrationTest` (FAQ ×7, UC-K ×1),
  `PhaseEvaluatorPlanTest` (UC-K), `PhaseEvaluatorSkillIntegrationTest`
  (CONFIRM ×3) — `allowedTools` and `validTerminalOutcomes` still asserted with
  `assertEquals` on an exact `List`/`Set`, with the new members added.
- `Sprint53SkillDeclarationGatingTest.productionSkillYamls_matchAuditDeclarationCoverage`
  — the Sprint 53 audit's "only `discover_triage` declares
  `alternate_candidate_use_cases`" disposition is superseded for the three
  Skills that carry `propose_reroute`. The matrix was **strengthened**, not
  relaxed: it now also asserts `reroute_target_use_cases` and
  `propose_reroute` co-occur exactly (both present or both absent, per Skill),
  and it now covers `resolve_technical_diagnose_or_intake`, which WS-3 added on
  2026-07-25 without extending this matrix.

New tests: `ProposeRerouteToolTest` (8), `AgentRunLoopRerouteBoundaryTest` (4).
Per §5.7 these are wiring evidence only — the mock decides what the "LLM"
chose — and are labelled as such in their javadoc.

## 5. Behavioural evidence

### 5.1 How it was run

Backend restarted after the final server change (JVM start 00:49:28; the
startup log records `ToolDispatcher initialized with 13 tools: [..., propose_reroute, ...]`,
up from 12, so the run went against the changed code). Postgres on 5442,
Redis up, `/actuator/health` UP on all components.

Five runs, `--parallel 1`, one attempt per case per run:

| run id | label | specs |
|---|---|---|
| `20260725-165045` | `s103-draw1` | targets: `promotion/cs_interactive_179`, `promotion/cs_interactive_185` |
| `20260725-165354` | `s103-draw2` | same targets |
| `20260725-165622` | `s103-draw3` | same targets |
| `20260725-165835` | `s103-negctl` | `anchor/cs_interactive_030`, `anchor/cs_interactive_155` (both `drift_behavior: none`, both resolve-class, UC-C and UC-D to match the targets' UCs) |
| `20260725-170011` | `s103-neighbors` | `promotion/cs_interactive_170` (UC-K), `promotion/cs_interactive_263` (UC-F) — both `hard_shift`, both resolve-class |

Note: the run-id timestamps read `20260725-16xxxx` while the host clock read
`2026-07-26 00:49`. See §6.5.

### 5.2 Target sessions — 6 measured, 0 UNMEASURED

| draw | case | phase trace (`before→after[uc]`) | `propose_reroute` | stop | escalation reason |
|---|---|---|---|---|---|
| 1 | 179 | `RESOLVE→RESOLVE[UC-C］; RESOLVE→CONFIRM[UC-C]; CONFIRM→RESOLVE[UC-A]; RESOLVE→CONFIRM[UC-A]; CONFIRM→ESCALATE[UC-A]` | **yes** UC-C→UC-A, turn 3, from CONFIRM | bot_ended | `agent_unable_to_resolve` |
| 1 | 185 | `DISCOVER×2; DISCOVER→ESCALATE[UC-A]` | no — never projected | bot_ended | `clarification_budget_exhausted` |
| 2 | 179 | `RESOLVE→RESOLVE[UC-C]; RESOLVE→ESCALATE[UC-C]` | no | bot_ended | `turn_budget_exhausted` |
| 2 | 185 | `DISCOVER; DISCOVER→RESOLVE[UC-D]; RESOLVE→ESCALATE[UC-D]` | no | bot_ended | `clarification_budget_exhausted` |
| 3 | 179 | `RESOLVE→RESOLVE[UC-C] ×2; RESOLVE→ESCALATE[UC-D]` | **yes** UC-C→UC-D, turn 3, from RESOLVE | bot_ended | `agent_unable_to_resolve` |
| 3 | 185 | `DISCOVER×2; DISCOVER→ESCALATE[UC-D]` | no — never projected | bot_ended | `clarification_budget_exhausted` |

Both re-routes carry model-authored reasoning, not pattern output:

> `{"via":"propose_reroute","use_case_id":"UC-D","previous_use_case_id":"UC-C","reasoning":"Customer is asking about account verification status, which falls under Account & Login (UC-D). The current UC-C (Messages & Replies) is not equipped to handle account verification inquiries."}`

### 5.3 The primary metric, split because the contract's framing collapses two things

The contract asked: how many sessions end with the bot having engaged the
drifted ask, **versus** ending in bot-initiated escalation? The runs show these
are not alternatives — a session can do both, and four did.

Adjudicating "engaged the drifted ask" as: after the customer raises the
second, different need, the bot's next message substantively addresses it
(answers it, or takes a concrete step such as asking for the one identifier it
needs) rather than ignoring it, staying in the old UC, or handing over.

| measure | baseline (failure brief) | this sprint |
|---|---|---|
| a drifted ask was actually raised | 5 of 5 | 5 of 6 (draw 2 / 179 never drifted — the customer stayed on the blocked-seller issue) |
| **engaged the drifted ask** | **0 of 5** | **4 of 5** |
| **ended in bot-initiated escalation** | **4 of 5** | **6 of 6** |
| session resolved | 0 of 5 | **0 of 6** |

### 5.4 What that does and does not support — read this before quoting §5.3

**Engagement improved; the outcome did not.** No session resolved, and
bot-initiated escalation did not fall. Taken at face value it rose (4/5 → 6/6),
but n is far too small for that to be a finding — the honest statement is that
escalation did not improve.

**Only one of the four engagements is attributable to this sprint.** The phase
traces in §5.2 are decisive: `cs_interactive_185` **never reached RESOLVE in
any of its three draws** (two ended inside DISCOVER; the third reached RESOLVE
only on the turn it escalated). `propose_reroute` is declared on the RESOLVE
and CONFIRM Skills, so on those sessions it was never in `plan.allowedTools`,
never projected, and never callable. Whatever engagement happened there came
from DISCOVER's own clarifying behaviour and would have happened without this
change. Claiming 4/5 as this sprint's result would be wrong.

The one clean attribution is **draw 1 / `cs_interactive_179`**, and it is a
clean one: the re-route fired from CONFIRM, landed in RESOLVE on the new UC
(`CONFIRM→RESOLVE[UC-A]` in the persisted turn record — the WP3 mechanism doing
exactly what it was designed to do), and the conversation ran two further turns
on the new ask before escalating on a *third*, different ask.

**The capability is reachable and the LLM uses it.** Of the sessions that both
reached RESOLVE/CONFIRM and had a drift raised, 2 of 2 re-routed.

**n = 3 draws × 2 specs.** This cannot show elimination of anything, and it
cannot distinguish a real escalation increase from draw noise. What it does
support: (i) the capability works end-to-end in production traces, (ii) the LLM
invokes it with sound reasoning when it is available, (iii) the negative
controls do not trigger it, and (iv) **the sprint did not reduce escalation or
produce a resolution.**

### 5.5 Negative controls — pass

`anchor/cs_interactive_030` and `anchor/cs_interactive_155`, both
`drift_behavior: none`. **Zero `propose_reroute` events** across both sessions
(SQL over `bot_events` for the two session ids: 0 rows). `cs_interactive_030`
stayed on UC-C for all three turns and ended on `user_requested` because the
customer asked for a human — the correct behaviour, not a re-route.
`cs_interactive_155` never left DISCOVER. Neither started re-routing.

### 5.6 Neighbours

`promotion/cs_interactive_263` (UC-F): no re-route; never left DISCOVER; ended
`clarification_budget_exhausted`. `promotion/cs_interactive_170` (UC-K):
**`status=ERROR` / `stop_reason=error`, reported as UNMEASURED**, not as a
result — the session cut off after turn 2. Because 170 was the only UC-K spec
in the set, `resolve_technical_diagnose_or_intake` — the third Skill wired in
WP1 — has **no behavioural coverage in this sprint**. Its wiring is covered by
the golden tests only.

### 5.7 Transcripts — before / after

**Before** (quoted from the failure brief / sprint prompt for
`cs_interactive_185`; I did **not** re-run the baseline build, so this is cited
evidence, not evidence I measured):

> "I just realised — my two live ads are still up, but I haven't been able to
> log in to edit or renew them. Can you help me recover access to my account
> first? And also, one of the ads is for a sofa — I think it might've been
> reported by someone, and I haven't seen any notification about it."

— against which the bot exhausted its clarification budget or gave up with
`agent_unable_to_resolve`.

**After**, `cs_interactive_179` draw 1 — the attributable pair. The customer
drifts at turn 3; `propose_reroute` fires UC-C→UC-A on that turn:

> **[3] USER:** Oh — actually, I just realised I also never got the pickup
> address from Leah. Can you help me find that? She listed the cat but didn't
> include her location in the ad details.
>
> **[3] BOT:** I understand you're looking for the pickup address from Leah's
> cat ad. Unfortunately, I don't have access to ad details or seller
> information beyond what's in your account. Could you try finding the ad again
> on Gumtree and messaging Leah to ask for the address? If you need help with
> messaging, I can guide you through that.
>
> **[4] USER:** Right — but I can't message her because the chat's gone and she
> hasn't messaged me back. Is there any way Gumtree can pass on a message to
> her, or share her contact details? It's urgent — the cat pickup is tomorrow.
>
> **[4] BOT:** I understand this is urgent, Anna. Unfortunately, Gumtree can't
> pass on messages or share seller contact details directly — we don't have
> access to that information. Since the chat is gone and she hasn't replied,
> your best bet is to find her cat ad again on Gumtree and send a new message
> through that ad. If you need help finding the ad, I can guide you on how to
> search for it.
>
> **[5] USER:** Actually — I just remembered I also reported Leah's ad last week
> for missing vaccination info, and it got taken down. Can you check if it's
> still live or help me find it again?
>
> **[5] BOT:** Let me connect you with a specialist.

The new need is engaged for two turns instead of forcing a handover. The
session still ends in escalation, on a third distinct ask.

**After**, `cs_interactive_179` draw 3 — the re-route fires with correct
reasoning and produces nothing:

> **[3] USER:** Actually — while we're on this — I also just realised my
> Gumtree account shows as 'unverified' even though I uploaded ID last week.
> Can you check why it's still pending? The cat pickup is tomorrow, so I'd
> really like both sorted.
>
> **[3] BOT:** Let me connect you with a specialist.

The runtime re-routed UC-C→UC-D on that same turn. The UC-D RESOLVE Skill then
escalated with `agent_unable_to_resolve` without attempting retrieval. Landing
in the right use case is necessary but plainly not sufficient.

## 6. Real defects found, out of contract — listed, not fixed

1. **`turn_budget_exhausted` mis-stamped at turn 2.** Draw 2 / 179
   (`15a3e0b4-…`) escalated on its second turn against `max_turns: 15` with
   `escalation_reason=turn_budget_exhausted`. The bot text is the generic
   "I'm having difficulty resolving this." This has the shape of the known
   `resolveMaxStepsReason`-on-loop-exhaustion mis-stamp: a per-turn tool-step
   exhaustion being reported as a session-level turn budget. Not touched.

2. **DISCOVER is now the dominant bottleneck, and it interacts badly with
   drift.** 5 of the 10 measured sessions across all five runs escalated with
   `clarification_budget_exhausted`, and 4 of those never left DISCOVER. When
   the customer raises a second need while DISCOVER is still clarifying the
   first, that new need consumes a clarification round —
   `control-policy.yaml:2` `max-clarification-rounds: 2` — so drift actively
   *accelerates* the budget exhaustion. This is why `cs_interactive_185` could
   not exercise this sprint's capability at all. I did not change any budget
   (the contract forbids lowering one, and raising one to make a symptom
   disappear would be the same mistake in the other direction). This looks like
   the next real work item on this failure shape.

3. **`cs_interactive_170` cannot be measured as configured** — `status=ERROR`
   mid-session. It was the only UC-K spec in my neighbour set, which is why
   `resolve_technical_diagnose_or_intake` has no behavioural coverage here.

4. **`Sprint53SkillDeclarationGatingTest` did not cover
   `resolve_technical_diagnose_or_intake`.** WS-3 / A3 added that Skill on
   2026-07-25 without extending the declaration matrix, so its declarations
   were unguarded for a day. I closed this while updating the test (§4), since
   leaving a matrix that silently skips a Skill would have made my own change
   less reviewable — flagging it as a pre-existing gap, not as sprint scope.

5. **Eval run-ids are stamped with a skewed clock.** The five runs produced
   ids `20260725-16xxxx` while the host clock read `2026-07-26 00:49` — an
   ~8-hour offset, consistent with a UTC/local mix in run-id generation. Every
   citation of a run in this document uses the directory name as produced. If
   run-ids are ever used to order or correlate runs, this will mislead.

6. **`cs_interactive_185`'s `primary_uc` is arguably wrong for its own
   persona.** The spec expects UC-D (Account & Login), but its scripted drift
   in every draw is toward editing live ads (UC-A/UC-B territory). Per the
   contract I did not touch `case_specs/**`; recording it for WS-2.

## 7. Where I think the contract is wrong

1. **"All 14 [pytest] failures come from the missing
   `data/human_review_annotations_2026-04-22_golden.csv`" is not true of one of
   them.** `tests/test_skill_procedure_extractor.py::TestProductionSkillLoad::test_load_all_six_production_skills_populated_critical_steps`
   fails with `expected 6 production Skill YAMLs, found 7` — a stale hardcoded
   count, stale since WS-3 added `resolve_technical_diagnose_or_intake.yaml` on
   2026-07-25. It has nothing to do with the missing CSV. This matters for the
   attribution rule: a dev agent who added a Skill file this sprint would find
   this test failing and, trusting the contract's characterisation, could
   mis-attribute it to the CSV instead of to their own change. (I added no
   Skill file, so the count is unchanged and the failure is identical
   before and after.) The other 13 are CSV-caused as described.

2. **The primary metric's "versus" is a false dichotomy.** "How many sessions
   end with the bot having engaged the drifted ask, versus ending in
   bot-initiated escalation" treats the two as alternatives. Four of the six
   target sessions did both: the bot engaged the new ask and *then* escalated,
   sometimes several turns later and on a different ask entirely. I reported
   the two counts separately in §5.3 rather than pick one, because collapsing
   them would have let a real improvement in engagement disguise a total
   absence of improvement in outcome — the exact failure mode §6 of the
   contract is guarding against.

3. **Minor:** the contract states a single simulator session takes 4–9 minutes.
   Measured here: ~60 seconds per session (two-case runs completed in
   ~95–150 s wall clock). Not a problem, but anyone budgeting a large batch
   from that figure will over-provision by roughly 5×.

## 8. Self-check

- [x] Both suites run, baselines recorded first, self-introduced failures 0.
- [x] Backend restarted after the final server change (13-tool registry line in
      the startup log); behavioural run executed against it.
- [x] 3 draws × 2 target specs, plus 2 negative controls, plus 2 neighbours.
- [x] Before/after transcript pair quoted verbatim (§5.7), with the "before"
      explicitly marked as cited rather than re-measured.
- [x] `git grep`: no new keyword or regex in `DriftDetector` or
      `RuntimeIntentClassifier` — neither file, nor `risk-keywords.yaml`, is in
      the diff at all.
- [x] §0.6 deviation entry written; two affected §0.3 rows marked.
- [x] Golden tool-set / declaration tests updated and strengthened, not weakened.
- [x] Nothing touched under `case_specs/`, `e2e/`, `autoloop/`,
      `docs/sprints/*` (other than this file), `docs/archive/`.
- [x] Out-of-contract findings listed (§6), contract disagreements stated (§7).
