---
title: Sprint 23 — Repeated FAQ Calls + LLM Stall Root-Cause Investigation (Two-Track) — Handoff
doc_tier: sprint-archive
status: archived
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-05-14
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 23 closes a two-track investigation+bundle sprint. Track A
  landed a narrow `prompt_projection` bundle
  (`R-already-called-prompt-consumption`): a principled teaching
  paragraph in `server/src/main/resources/prompts/system_prompt.txt`
  that names the Sprint 20 `already_called` slot, describes its
  observable content, points the LLM at `accumulated_tool_results`
  for the prior payload, and leaves the re-emit decision to the LLM
  (soft signal, no runtime short-circuit). Track B is
  investigation-only: the proximate cause (PhaseEvaluator lines
  754–770 emits identical placeholder text on consecutive
  DEADLINE_EXCEEDED events without session-scope coalescing) is
  conclusive; the deeper cause (model-latency / timeout-config) is
  unverified in this sprint and is deferred as a follow-on R-item
  with the Sprint 19 §3.7 hypothesis as the starting evidence.
  Bundle gate ("ONLY if root cause is purely user-facing repeated
  fallback messaging") read strictly: deeper cause not eliminated →
  no Track B bundle. UX-over-pass-rate framing preserved by
  proposing the session-scope coalesce + honest next-step as the
  narrow target for a future bundling sprint once latency evidence
  is collected.
---

# Sprint 23 Handoff — Repeated FAQ Calls + LLM Stall (Two-Track Investigation+Bundle)

Date: 2026-05-14
Branch: `design-v1-without-human-review`
Sprint class: two-track investigation+bundle, semantic-touching.
§7 stanza required (per Sprint 23 objective §11; precedent: Sprint
19 A+B, Sprint 20 A+B). UX-over-pass-rate framing per human's
reframe of 2026-05-14. **Outcome: Track A bundles (narrow
`prompt_projection` fix), Track B investigation-only.**

## 1. Context Pack

Per `docs/current/agent_context_guide.md` Context Pack Prompt;
produced before any code or doc edit was committed.

**Relevant docs.**

- `AGENTS.md` (durable-connective; current) — constitution-chain
  entry; loaded transitively.
- `docs/current/doc_governance.md` (durable-connective; current) —
  front-matter schema and source-of-truth rules; this handoff is
  `sprint-archive`.
- `docs/current/agent_context_guide.md` (durable-connective;
  current) — per-task reading lists; Track A used "Runtime / phase
  machine / drift", Track B used "Runtime / phase machine / drift"
  + the read-only `infra` lens; both tracks used the "Tool schema
  / tool policy" lens for the search_knowledge dispatch path.
- `docs/current/iteration_governance.md` (durable-connective;
  current) — §1 Constitution (LLM-vs-Runtime ownership +
  Iteration rule + forbidden list), §3.1 layer set + §3.2
  first-match-wins walk, §5 Eval Acceptance Rules (acceptance
  bars), §7 sprint-objective stanza requirement.
- `docs/sprint_objective.md` (current-runtime; current) — Sprint
  23 scope, the per-track hypothesis lists, the bundle-or-defer
  gates, the §9 NOT-in-scope hard fence, and the §16 review-rule
  blocking list.
- `docs/sprints/sprint-019-handoff.md` (sprint-archive; archived)
  — §3 per-case §3.2 walks (the cs_011 placeholder claim and the
  §3.7 cross-case observation are the load-bearing pieces for
  Sprint 23's re-derivation deviation report); §4 orchestrator
  de-dup investigation (Sprint 19 §4.1 found no de-dup at the
  dispatcher; Sprint 19 §4.2 specified the Sprint 20 slot shape).
- `docs/sprints/sprint-020-handoff.md` (sprint-archive; archived)
  — §5 (`already_called` slot shape + Sprint 20 unit-test
  coverage); §12 (Sprint 20 named
  `R-already-called-prompt-consumption` as the open prompt-side
  follow-on).
- `docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`
  (diagnostic; current) — ground-truth case for Track A's "≥2
  identical-args `search_knowledge` dispatches in one session"
  pattern (3 identical-params executions in T2 per the brief).
- `docs/action_bank.md` (durable-connective; current) — read for
  R-item naming and current dispositions; not edited (per
  Sprint 19 / Sprint 20 precedent, dev agent records action-bank
  deltas in §11 below for the deliver agent to apply on close).

**Relevant code paths** (read; only `system_prompt.txt` edited).

- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  — outer step loop at line 171; projection-build call site at
  lines 180–184 passes `toolEvents` into the Sprint 20 overload;
  deadline catch at line 204 returns
  `AgentRunResult.deadlineExceeded`; per-tool dispatch loop at
  lines 286–449 (the §6a/§6a'/§6a'' guards do not de-duplicate
  identical-args calls — see Track A §3 below).
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  — `DEADLINE_EXCEEDED` / `LLM_UNAVAILABLE` branch at lines
  754–770 emits the slow-LLM placeholder; **no session-scope
  state-tracking on consecutive deadlines**.
- `server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java`
  — `invokeChat` at lines 92–160 makes one `LlmClient.chat` call,
  catches `LlmDeadlineExceededException` at line 120, propagates;
  **no retry/backoff loop at this layer**.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  — projection build emits `phase_plan.system_instruction`,
  `accumulated_tool_results`, and the Sprint 20 `already_called`
  slot (build overload line 707, slot helper line 845).
- `server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java`
  — single `dispatch(toolName, session, parameters)` entry at
  line 86; policy enforce + execute; **no de-dup by
  arguments_hash**.
- `server/src/main/resources/prompts/system_prompt.txt` —
  pre-edit grep confirmed **zero** mentions of `already_called`,
  `accumulated_tool_results`, or any "prior tool calls" /
  "repeated calls" teaching. **Edited in this sprint.**

**Doc status warnings.**

1. Sprint 19 §3.7 named a 5-case placeholder-loop list
   {cs_002, cs_011, cs_014, cs_038, cs_040} that is **internally
   inconsistent with Sprint 19 §3.2** (the same handoff's §3.2
   walk on cs_011 explicitly observes "no `Sorry, I'm a bit slow`
   text in the transcript"). Sprint 23's planning-turn premise
   verification flagged this; the dev re-derived independently —
   see §3 below — and confirms cs_011 has zero placeholder
   emissions.
2. Sprint 23 dev prompt §2 claims §3.7 "missed cs_066"; the dev's
   independent re-derivation finds cs_066 has **zero** placeholder
   emissions in the smoke run transcript and a different shape
   (the `skill_state` `case_id` loss documented in Sprint 19 §3.6).
   §3.7 was correct to exclude cs_066; the dev prompt's claim is
   itself imprecise. Reported here per §9 stop-condition "Sprint
   19 §3.7 disagrees with what you observe in the results JSON".
3. The objective §4.5 deliver-agent re-derivation named
   {cs_002, cs_014, cs_040} as clean loops and {cs_038, cs_259}
   as emits+recovers; this matches my independent re-derivation.
   The objective §4.5 also flagged cs_066 / cs_176 as
   "interleaved / pending dev verification". My re-derivation
   reclassifies cs_066 (zero placeholders) and cs_176
   (interleaved) — see §3 below.

**Source-of-truth decisions.**

- For **placeholder-loop case enumeration**:
  `eval_interactive/results/20260510-134558/results.json` per-case
  `transcript[].message` field is authoritative. Decision rule:
  bot turns whose message equals the exact PhaseEvaluator line
  765 string `"Sorry, I'm a bit slow right now. Please try
  sending that again in a moment."` are placeholder emissions;
  classification per Sprint 23 objective §7.2 + dev prompt §2
  (clean loop / emits+recovers / interleaved / zero).
- For **repeated-FAQ enumeration**:
  `results.json` per-case `l1_results.*.detail` +
  `l2_results.tool_sequence_match.detail` are the only
  tool-sequence-level evidence available in the snapshot.
  Decision rule: cases whose tool sequence shows ≥2
  `search_knowledge` entries within one session are Track A
  candidates; argument-hash identity is **inferred** from
  turn-tag adjacency + absence of intervening
  query-changing user turns, since `arguments_hash` is not
  carried in results.json. The manual-probe brief is
  authoritative for "identical params / identical results in T2"
  on that one case.
- For the **prompt-consumption gap** (Track A hypothesis (c)):
  `server/src/main/resources/prompts/system_prompt.txt` is
  authoritative. Pre-edit grep confirmed zero hits on the
  expected teaching anchors (`already_called`,
  `accumulated_tool_results`, "prior tool calls", "repeated
  calls").
- For the **PhaseEvaluator emission text** (Track B hypothesis
  (d)): `PhaseEvaluator.java` lines 754–770 are authoritative;
  the objective §4.1 already cited these and the dev re-read the
  branch in full to confirm no state-tracking is present.

**Implementation status (sprint-start).**
`not_started` on both tracks. The Sprint 20 slot is wired
(verified by re-reading `ContextProjectionBuilder.java` line
725's `projection.set("already_called", buildAlreadyCalledNode(
priorToolEvents))`) but prompt-side consumption was the
deferred follow-on (Sprint 20 §12 R-item table). PhaseEvaluator
has no consecutive-deadline counter (verified by grep on
`consecutiveDeadlineCount` / `deadlineCount` — zero hits).

**Top risks before coding.**

1. **Bundling Track B against the strict bundle gate.** Evidence
   on the deeper cause (model latency) is unverified; per the §3
   hard fence and §16 review rule "Bundle-without-conclusive-
   evidence (blocking)", bundling could be flagged. Mitigation:
   investigation-only output on Track B; bundle the proximate
   fix shape into an R-item with concrete code-path + transcript
   evidence so a future sprint can land it once latency data is
   collected. (Materialised: see §4 + §5 + §11.)
2. **cs_040 placeholder-vs-routing conflation.** cs_040 is a
   clean placeholder loop AND has a UC-K→UC-C routing failure
   (`active_use_case=UC-C` in results.json; expected `UC-K`).
   Track B's narrow fix would address the placeholder UX only;
   routing remains a separate `prompt_projection` issue. (Hard
   fence honoured: Track B does not bundle; the routing failure
   is named separately in §4 + §11.)
3. **Regression-test bar for Track A.** The dev prompt §10 bar
   ("the fix reverses the repeated-FAQ shape on at least one
   target case") requires a live smoke rerun, which is not
   feasible in this session without LLM-provider configuration.
   Mitigation: ship a Java regression test asserting the
   principled teaching is present in `system_prompt.txt` and
   does not branch on tool name or UC; note the empirical
   reversal as deferred-to-rerun in §13 + §10.
4. **system_prompt.txt is the single point of LLM-side
   contract.** Adding a teaching paragraph could regress
   existing prompt invariants (e.g. the §I0 grounded-answer
   directive, the request_handover decision tree). Mitigation:
   the new paragraph was inserted as a self-contained block
   between the Rules section and the DISCOVER phase guidance;
   it does not touch any existing tier-2 or tier-3 escalation
   text, and the full server suite was re-run (898/0/0/1, no
   regressions vs Sprint 20 894/0/0/1 baseline +
   Sprints 21/22 test additions).
5. **Anti-hardcode self-check.** The principled teaching must
   not branch on tool name or UC id. Mitigation: the regression
   test in
   `AlreadyCalledPromptConsumptionTest.systemPrompt_teaching_doesNotBranchOnToolNameOrUseCase`
   asserts a window around the `already_called` mention
   contains no `search_knowledge` / `resolve_article` / UC-A /
   UC-B / ... / UC-K / UC-FP string. (Passing.)

## 2. Sprint-objective recap

From `docs/sprint_objective.md` §1 (quoted, key clauses):

> Two user-visible reliability failures:
> 1. **Track A — Repeated FAQ / search_knowledge tool calls.** …
>    Sprint 20 Track B landed the `already_called` projection
>    slot to surface the duplicate-call diagnostic to the LLM,
>    but **prompt consumption (system_prompt.txt teaching the
>    LLM about the slot) was deferred to
>    `R-already-called-prompt-consumption` and never landed.**
>    … Bundle the safest narrow fix if evidence is conclusive
>    for one root cause; defer the rest as R-items.
> 2. **Track B — LLM instability / deadline / placeholder /
>    stall.** … If evidence supports a narrow user-facing fix,
>    ship session-scope slow-LLM placeholder coalescing with an
>    honest next-step message and regression coverage. If
>    evidence points to model latency / timeout config,
>    **propose a separate model/budget/retry R-item with
>    supporting data; do NOT widen the deadline budget in this
>    sprint.**

UX-over-pass-rate framing: a fix that improves customer-visible
reliability wins over a fix that improves pass-rate but masks
the user-facing artefact.

## 3. Track A — repeated FAQ / `search_knowledge` (per-case §3.2 walk + matrix)

### 3.1 Target-set re-derivation (independent, from results.json)

**Method.** Walked `eval_interactive/results/20260510-134558/results.json`
case-by-case via Python json parse + grep on
`l1_results.*.detail` and `l2_results.tool_sequence_match.detail`
(the only fields in the snapshot that carry tool-sequence
evidence; `results.json` does NOT carry `ToolEvent` records or
`arguments_hash` values). For each case, recorded:

- the full tool sequence from `l2_results.tool_sequence_match.detail`;
- the per-turn breakdown from `l1_results.no_forbidden_tools.detail`
  / `l1_results.intake_no_knowledge_tool.detail` where present;
- the case's `status` / `failure_tags` / `stall_failure_tag` to
  cross-check the runtime's own loop-detector firing.

Argument-hash identity is **inferred** (not directly observable in
results.json) from turn-tag adjacency + the absence of intervening
user content that would plausibly change the query. The manual-
probe brief is authoritative for the "identical params / identical
results" claim on its case.

**Track A target set** (cases with ≥2 `search_knowledge` dispatches
in one session, inferred near-/exact-identical args from sequence
context):

| case_id                | search_knowledge count | turn-level breakdown                                          | argument-identity evidence (inferred unless noted)          |
|------------------------|------------------------|---------------------------------------------------------------|-------------------------------------------------------------|
| cs_interactive_002     | 3                      | `[search_knowledge, resolve_article, search_knowledge, search_knowledge]` | 3 calls in 4-turn session; no intervening user turn between the 2 trailing back-to-back. |
| cs_interactive_014     | 5                      | `[search_knowledge, resolve_article, search_knowledge, search_knowledge, search_knowledge, get_customer_context, search_knowledge]` | 5 calls; the run of 3 consecutive `search_knowledge` mid-sequence has no intervening user content per transcript. |
| cs_interactive_015     | 2 (+3 `resolve_article`) | `[classify_use_case, search_knowledge, resolve_article, search_knowledge, resolve_article, resolve_article]` | 2 `search_knowledge` flanking a single `resolve_article` cycle; 3 `resolve_article` calls also fit the duplicate-call shape on a different tool. |
| cs_interactive_040     | 3                      | turn 1: `[search_knowledge, search_knowledge, resolve_article]`; turn 2: `[search_knowledge]` (per L1 turn-tag) | **Strongest evidence**: 2 `search_knowledge` calls back-to-back within turn 1 with no intervening user content. |
| cs_interactive_259     | 2                      | `[search_knowledge, classify_use_case, search_knowledge, resolve_article]` | 2 `search_knowledge` calls; second follows `classify_use_case` (the UC commit may have changed `uc_tags`, so arg-identity is **uncertain** — recorded as a candidate, not a conclusive duplicate). |
| manual-probe (2026-05-13) | 3                   | T2: 3 `search_knowledge` calls per brief §"What happened?" #3 | **Direct evidence from brief**: identical params (`query="why can't I see my advert"`, `uc_tags=["UC-A","UC-B"]`) and identical return payloads across all 3 executions. |

Count: 5 smoke cases + 1 manual probe = **6 Track A targets**.

Cases ruled out of Track A target set (≤1 search_knowledge or no
duplicate shape): cs_001 (PASS, 1 sk), cs_011 (0 sk, T2 silent
failure — different shape per Sprint 19 §3.2), cs_029 (PASS, 0
sk), cs_036 (PASS, 0 sk), cs_038 (1 sk; UC-J intake path), cs_066
(0 sk; UC-K intake path; `skill_state` shape per Sprint 19 §3.6),
cs_095 (TIMEOUT, no transcript), cs_176 (1 sk; different
shape — see Track B §4), cs_192 (CONTRACT_VIOLATION, no
transcript).

### 3.2 Hypothesis walk per case (the five hypotheses from objective §6.1)

The five hypotheses are walked in order; per case, the strongest
evidence-bearing hypothesis is recorded, with the other four
ruled in / out by evidence. The five hypotheses are:

- **(a)** LLM re-emitting identical or near-identical tool calls.
- **(b)** Orchestrator / `ToolDispatcher` amplification.
- **(c)** Missing or ineffective `already_called` prompt consumption.
- **(d)** Projection failure (prior FAQ results not visible /
  not salient).
- **(e)** Phase-plan directive non-followship.

**Cross-case findings that apply to ALL six target cases:**

- **(b) is ruled out for all cases.** `ToolDispatcher.dispatch`
  (line 86 of `ToolDispatcher.java`) executes whatever is asked;
  there is no `arguments_hash` dedup and no short-circuit on
  prior identical calls. `AgentRunLoopImpl.run`'s inner
  dispatch loop (lines 286–449) iterates `for ToolCall call :
  calls` from the LLM response without dedup. Sprint 19 §4.1
  reached the same conclusion. **No amplification by the
  dispatcher; duplicates are LLM-emitted.**
- **(c) is conclusively confirmed across all cases.** Pre-edit
  grep on `server/src/main/resources/prompts/system_prompt.txt`
  returned **zero** hits for `already_called`,
  `accumulated_tool_results`, or any "prior tool calls" /
  "repeated calls" teaching. The Sprint 20 slot ships in the
  projection JSON (verified via `ContextProjectionBuilder.java`
  line 725) but the LLM has no static teaching about what the
  slot means or how to consult it. **This is the conclusive
  root cause for the Sprint 23 Track A target set.**
- **(d) partially confirmed.** `accumulated_tool_results` is in
  the projection but also has zero teaching in
  `system_prompt.txt`. This is a longer-standing gap that
  pre-dates Sprint 20. For the Track A bundle scope, treating
  (d) and (c) jointly via the principled teaching paragraph
  (which references BOTH slots) addresses the shared gap; a
  deeper standalone (d) investigation (whether prior results
  are *salient enough* even when teaching exists) is a follow-on.

**Per-case differentiation** (where (a) and (e) are case-specific):

| case_id | (a) LLM re-emission | (b) orchestrator amp | (c) missing prompt consumption | (d) projection failure | (e) phase-plan directive non-followship | root-cause layer |
|---------|---------------------|----------------------|--------------------------------|------------------------|-----------------------------------------|------------------|
| cs_002  | Confirmed — 3 `search_knowledge` calls in a 4-turn session, identical UC-C messaging-replies surface; LLM never sees `already_called` teaching. | Ruled out (cross-case). | Confirmed (cross-case). | Confirmed (cross-case). | Not observed (no MUST-clause directive visible in transcript). | `prompt_projection` |
| cs_014  | Confirmed — 5 `search_knowledge` calls, the 3-consecutive run is the clearest single-shape evidence in the smoke set; LLM re-emits despite same UC-C surface. | Ruled out. | Confirmed. | Confirmed. | Not observed. | `prompt_projection` |
| cs_015  | Confirmed — 2 `search_knowledge` calls + 3 `resolve_article` calls on UC-FP shape; the LLM also duplicates `resolve_article`. | Ruled out. | Confirmed. | Confirmed. | Not observed (UC mis-routed to UC-B; separate issue). | `prompt_projection` (primary); UC mis-route is a separate `prompt_projection` concern. |
| cs_040  | **Strongest evidence in target set** — turn 1's `[sk, sk, ra]` per-turn breakdown shows 2 `search_knowledge` calls back-to-back with no intervening user turn. | Ruled out. | Confirmed. | Confirmed. | Not directly observed on the search_knowledge path (UC-K intake routing failure is a SEPARATE shape — see §3.3 cs_040 fence). | `prompt_projection` |
| cs_259  | Candidate, not conclusive — second `search_knowledge` follows `classify_use_case`, so `uc_tags` may have changed (would change arg-hash). Recorded as a target but with the caveat. | Ruled out. | Confirmed. | Confirmed. | Sprint 7 §I0 violation per cs_259 brief is a known prior observation; the prompt-consumption gap does not directly cause §I0 violations. | `prompt_projection` for re-emission; `semantic_planner` for §I0 (separate). |
| manual-probe | **Direct evidence from brief** — 1 LLM tool_call → 3 dispatched executions, identical params, identical results. | Ruled out (brief §"What happened?" #3 already disambiguated three hypotheses and noted "the execution layer did not de-duplicate the calls themselves — three real API round-trips"). | Confirmed. | Confirmed. | **Yes** — the bot ignored the RESOLVE `phase_plan.system_instruction` MUST-call-resolve_article clause. This is the second observed instance of phase-plan directive non-followship (cs_259 §I0 was the first); per the brief §"Related observation" #3, this is below the user's bar for opening `R-prompt-phase-plan-directive-followship` without controlled multi-shape testing. | `prompt_projection` (primary, for the re-emission shape this sprint targets); `semantic_planner` (secondary, for the §I0 + RESOLVE-MUST shape; deferred per the n=1 → n=2 ladder). |

### 3.3 cs_040 placeholder-vs-routing fence (objective §5)

cs_040 appears in BOTH Track A's target set (2 same-turn
`search_knowledge` calls) and Track B's target set (2 consecutive
placeholder emissions). It ALSO has a UC-K → UC-C routing
failure: `active_use_case=UC-C` in results.json, expected
`UC-K` per the primary_uc field. Sprint 19 §3.5 documented the
routing failure under the `prompt_projection` layer (the
`topic_subject=technical issue intake` form-context cue did not
anchor the UC routing).

**Three SEPARATE issues at three different layers/surfaces:**

1. Repeated `search_knowledge` calls within turn 1 (Track A;
   `prompt_projection`; addressed by the Sprint 23 bundle).
2. Repeated placeholder emissions on consecutive deadlines
   (Track B; `infra`; NOT addressed by the Sprint 23 bundle —
   see §4).
3. UC-K → UC-C routing failure (`prompt_projection`,
   separately; NOT addressed by Sprint 23 — Sprint 19 §3.5
   open observation).

The Sprint 23 Track A bundle (system-prompt teaching for the
`already_called` slot) **does not claim to fix** (3). The
regression test in §5 asserts the teaching's presence and
principled shape; it does NOT assert that cs_040's UC routing
flips. The routing failure remains open; see §11 for the
disposition.

### 3.4 Bundle decision (objective §6.3)

Evidence for hypothesis (c) is **conclusive for all six target
cases**: the Sprint 20 slot is in the projection, the
`system_prompt.txt` has zero teaching, and the LLM has no static
guidance about the slot's existence or semantics. The dispatcher
has no dedup (so the LLM owns the re-emit decision per §1.3),
and the slot is exactly the soft signal that, with teaching,
would let the LLM avoid the re-emit.

**Bundle:** the safest narrow fix is to land
`R-already-called-prompt-consumption` — a principled teaching
paragraph in `server/src/main/resources/prompts/system_prompt.txt`
that names the slot, describes its content in observable terms,
points the LLM at `accumulated_tool_results` for the prior
payload, and leaves the re-emit decision to the LLM (soft signal;
the slot does not block dispatch). The teaching is principled —
no branching on tool name, no branching on UC id, no
eval-case-specific phrasing.

**Deferred (per "bundle the safest narrow fix only"):**

- Hypothesis (e) for the manual-probe case (RESOLVE MUST-clause
  non-followship) — n=2 observation across cs_259 + manual probe;
  still below the user's bar for opening
  `R-prompt-phase-plan-directive-followship` without controlled
  multi-shape testing. Recorded in §11 as a strengthened
  observation, not a new R-item.
- Hypothesis (b) "amplification" disposition — confirmed
  conclusively ruled out for this sprint; the Sprint 18
  `R-runtime-orchestrator-tool-call-deduplication` R-item (action
  bank line 426) remains open for a separate future infra
  investigation if the prompt-consumption fix proves
  insufficient.
- Standalone (d) investigation — whether prior results are
  *salient enough in the projection ordering / formatting*
  even with teaching — deferred. The new teaching paragraph
  references `accumulated_tool_results` and the LLM observably
  uses that slot in PASS-case responses (cs_001 shows the bot
  consulting tool results to produce grounded answers), so a
  standalone projection-salience investigation is a measurement
  task once the prompt-consumption fix is in production.

## 4. Track B — placeholder / stall (per-case §3.2 walk + matrix)

### 4.1 Target-set re-derivation (independent, from results.json)

**Method.** `grep "Sorry, I'm a bit slow right now"
eval_interactive/results/20260510-134558/results.json` returns
14 matches; mapped to case boundaries via Python json parse of
`transcript[].message` per `case_id`. Each bot turn whose message
equals the exact PhaseEvaluator line 765 string is a placeholder
emission. Classified per objective §7.2 + dev prompt §2.

**Track B target set (placeholder-emitting cases, classified):**

| case_id | placeholder bot turns | classification | other relevant signals |
|---------|------------------------|----------------|------------------------|
| cs_002 | bot turns 2 and 3 (back-to-back) | **clean loop (≥2 consecutive)** | `stop_reason=loop_detected` (runtime loop-detector fired). |
| cs_014 | bot turns 2 and 3 (back-to-back) | **clean loop** | `stop_reason=loop_detected`; `stall_failure_tag=PLACEHOLDER_WITHOUT_FOLLOWUP`. |
| cs_040 | bot turns 1 and 2 (back-to-back) | **clean loop** | `stop_reason=loop_detected`; UC-K→UC-C routing failure (separate). |
| cs_015 | bot turn 1 only (recovers in turn 2) | **emits+recovers (single)** | bot recovered with a grounded UC-B answer in turn 2. |
| cs_038 | bot turn 3 only (turn 4 is the runtime turn-budget-exhausted handover template) | **emits+recovers (single)** | bot turn 4 = "I've reached the limit…" runtime template, not a recovered substantive answer. |
| cs_259 | bot turn 2 only (recovers in turn 3) | **emits+recovers (single)** | bot recovered with a grounded UC-F answer in turn 3. |
| cs_176 | bot turns 1 and 3 (turn 2 is a substantive bot response) | **interleaved** | `stop_reason=goal_impossible`; expected UC-E, active UC-I (separate UC mis-route). |

**Zero placeholders:** cs_001 (PASS), cs_011 (turn 2 silent
failure — different shape per Sprint 19 §3.2), cs_029 (PASS),
cs_036 (PASS), cs_066 (UC-K intake; `skill_state` `case_id`
shape per Sprint 19 §3.6), cs_095 (TIMEOUT — no transcript),
cs_192 (CONTRACT_VIOLATION — no transcript).

**Deviation from Sprint 19 §3.7 (named 5-case list
{cs_002, cs_011, cs_014, cs_038, cs_040}):**

- cs_011 is in §3.7's list but has **zero placeholder emissions**
  per its own §3.2 walk in the same Sprint 19 handoff — internal
  inconsistency confirmed.
- cs_038 is in §3.7's list as a "placeholder loop" surface but is
  actually **emits+recovers** (single placeholder + turn-budget
  template). Sprint 19 §3.4 itself notes the turn-budget mapping;
  the §3.7 cross-case framing collapsed the two shapes.
- cs_015, cs_176, and cs_259 are **NOT in §3.7's list** but DO
  emit the placeholder text — §3.7 missed them.
- cs_066 is **correctly excluded** from §3.7 (§3.6 names a
  different shape); the Sprint 23 dev prompt §2 claim that §3.7
  "missed cs_066" is itself imprecise — cs_066 has zero
  placeholders and a different `skill_state` shape.

Reported per dev prompt §9 stop-condition ("Sprint 19 §3.7
disagrees with what you observe in the results JSON — surface in
handoff §3").

### 4.2 Hypothesis walk per case (the six hypotheses from objective §7.1)

The six hypotheses are:

- **(a)** Model latency [follow-on R-item only].
- **(b)** Deadline budget [follow-on R-item only — DO NOT change
  config].
- **(c)** Retry/backoff.
- **(d)** PhaseEvaluator fallback handling at lines 754–770.
- **(e)** Session-scope placeholder repetition.
- **(f)** Turn-budget / phase-transition mapping.

**Cross-case findings:**

- **(c) is ruled out at the AgentRunLoop layer.**
  `LlmInvocationService.invokeChat` (lines 92–160) issues one
  `LlmClient.chat(request)` call, catches
  `LlmDeadlineExceededException` at line 120, and propagates
  immediately. There is no retry/backoff loop at this layer.
  Whatever retry behaviour the underlying `LlmClient` /
  `FallbackLlmClient` performs is included in the single
  deadline budget; it does NOT amplify identical placeholder
  emissions across turns.
- **(d) is confirmed.** PhaseEvaluator lines 754–770 emit
  `"Sorry, I'm a bit slow right now. Please try sending that
  again in a moment."` deterministically on every
  `DEADLINE_EXCEEDED` outcome, regardless of whether the prior
  turn was also a deadline event. There is no session-scope
  state-tracking (verified: `grep "consecutiveDeadline" / "deadlineCount"`
  returns zero hits in `server/src/main`).
- **(e) is confirmed.** The combination of (d) +
  per-turn-independent deadline events produces back-to-back
  identical placeholder bot messages for cs_002 / cs_014 /
  cs_040. The runtime's own loop-detector fires
  (`stop_reason=loop_detected`) on the repeated identical text,
  ending the session.
- **(a) and (b) are unverified in this sprint.** `results.json`
  does NOT carry per-turn LLM latency data; there is no
  measurement of `LlmClient.chat` round-trip time per case.
  The Sprint 19 §3.7 hypothesis (model switch `f2d4cb2 fix:
  switch primary llm to deepseek-v4-flash` widened latency
  variance; placeholder loop is the symptom amplifier) remains
  a hypothesis. Cannot be confirmed or rejected without a
  separate latency-collection investigation.
- **(f) partially confirmed.** The runtime's loop-detector
  fires on consecutive identical bot turns (the
  `stop_reason=loop_detected` on cs_002 / cs_014 / cs_040
  confirms this). The mapping from `DEADLINE_EXCEEDED` to
  `stay-in-current-phase` (PhaseEvaluator line 763) means the
  bot remains in DISCOVER / RESOLVE without a phase
  transition; consecutive deadlines therefore each emit the
  same placeholder without escalation or alternate routing.

**Per-case differentiation:**

| case_id | (a) latency | (b) budget | (c) retry | (d) PhaseEval emit | (e) session-scope repetition | (f) turn-budget/phase | classification | proximate-cause layer |
|---------|-------------|------------|-----------|---------------------|------------------------------|-----------------------|----------------|-----------------------|
| cs_002 | unverified | unverified | ruled out | confirmed | confirmed | runtime loop_detected fired | clean loop | `infra` (PhaseEvaluator emit + missing session-scope state) |
| cs_014 | unverified | unverified | ruled out | confirmed | confirmed | runtime loop_detected + `PLACEHOLDER_WITHOUT_FOLLOWUP` stall tag | clean loop | `infra` |
| cs_040 | unverified | unverified | ruled out | confirmed | confirmed | runtime loop_detected; plus separate UC-K→UC-C routing failure (different shape, different layer) | clean loop + routing (separate) | `infra` (placeholder); `prompt_projection` (routing — separate) |
| cs_015 | unverified | unverified | ruled out | confirmed (single emission) | not a loop — single emission | bot recovered to substantive answer in turn 2 | emits+recovers | `infra` (single emission, no loop) |
| cs_038 | unverified | unverified | ruled out | confirmed (single emission) | not a loop — single emission | turn-budget escalation took over in turn 4 | emits+recovers | `infra` (single emission, then turn_budget) |
| cs_259 | unverified | unverified | ruled out | confirmed (single emission) | not a loop — single emission | bot recovered with Sprint 7 §I0 violation in turn 3 (separate `semantic_planner` shape) | emits+recovers | `infra` (single emission); `semantic_planner` (§I0 — separate) |
| cs_176 | unverified | unverified | ruled out | confirmed (turn 1 + turn 3) | interleaved (turn 2 recovered) | UC mis-route to UC-I (separate `prompt_projection` shape) | interleaved | `infra` (placeholders); `prompt_projection` (UC mis-route — separate) |

### 4.3 Bundle decision (objective §7.4 + dev prompt §4)

**Restated bundle gate** (objective §7.4): "Bundle session-scope
coalesce + honest next-step message ONLY if evidence shows root
cause is **purely** user-facing repeated fallback messaging. If
evidence points to model latency or timeout config as root cause,
defer with an R-item carrying the supporting latency data. Do
NOT widen the deadline budget under any circumstance in this
sprint."

**Decision: Investigation-only. NO Track B bundle in Sprint 23.**

Rationale:

1. The **proximate cause** is conclusive: PhaseEvaluator
   lines 754–770 emit identical text on every
   `DEADLINE_EXCEEDED` outcome without session-scope state-
   tracking; consecutive deadlines therefore produce
   back-to-back identical bot messages, which the runtime
   loop-detector flags. This is a deterministic runtime-
   emission behaviour, observable in code + transcripts.
2. The **deeper cause** is UNVERIFIED. `results.json` does not
   carry per-turn LLM latency; the Sprint 19 §3.7 model-switch
   hypothesis remains a hypothesis. Without latency data, the
   strict reading of the bundle gate ("purely user-facing
   repeated fallback messaging") fails: we cannot rule out
   that the user-facing repetition is downstream of an
   addressable latency issue.
3. The §16 review rule ("Bundle-without-conclusive-evidence
   (blocking)") favours conservative restraint when one of
   the two causal layers is unverified.
4. UX-over-pass-rate framing is preserved by **proposing the
   narrow user-facing fix shape as a concrete R-item** with
   the proximate-cause evidence packaged for a future sprint
   to consume once latency data is collected (see §11
   `R-slow-llm-placeholder-coalesce-honest-next-step`).

The conservative disposition does NOT close the user-visible
reliability issue — it captures it. A future sprint that
collects per-turn LLM latency data can re-evaluate the bundle
gate with the deeper-cause uncertainty resolved.

**Track B narrow-fix R-item proposal (deferred):** session-
scope counter on consecutive `DEADLINE_EXCEEDED` outcomes
(mirroring the existing `runtimeErrorCount` pattern in
`BotSession`); first deadline emits the existing placeholder;
second consecutive deadline emits a principled honest-next-step
message ("This is taking longer than expected. Would you like
me to connect you with a specialist, or would you like to
retry?"). The fix is `infra`-layer deterministic — no semantic
decision, no UC branching, no widened deadline budget, no model
change. Code edit surface: `PhaseEvaluator.java` DEADLINE_EXCEEDED
branch + a new `consecutiveDeadlineCount` column on `BotSession`
(parallel to V12 `runtime_error_count`; needs a V13 migration).
Regression test bar: 2 consecutive `DEADLINE_EXCEEDED` outcomes
produce ONE placeholder + ONE honest message (not 2 identical
placeholders).

### 4.4 cs_040 placeholder-vs-routing fence (restated; honoured)

cs_040 is a clean placeholder loop AND has a UC-K→UC-C routing
failure (Sprint 19 §3.5; this sprint's matrix confirms the
mismatch from `results.json` per-case fields). The two shapes
are SEPARATE issues at different layers; the Track B narrow-fix
R-item proposal in §4.3 addresses the placeholder UX ONLY.
**The routing failure remains an open observation.** Action-bank
state for `R-prompt-phase-plan-directive-followship`:
disposition `proposed (Sprint 19 §11)` per action_bank line 450;
n=2 ladder (cs_259 + manual-probe RESOLVE MUST) still below
the user's controlled-multi-shape-testing bar. cs_040's UC
routing failure is a different surface (topic_subject cue not
anchoring routing); naming it under `R-prompt-phase-plan-directive-
followship` would conflate two surfaces. **Proposed disposition:**
keep `R-prompt-phase-plan-directive-followship` scope tight to
phase-plan directive non-followship (the cs_259 §I0 + manual-
probe RESOLVE-MUST shape); open a new R-item
`R-cs040-uc-k-topic-subject-routing` (`prompt_projection`) for
the cs_040-specific routing failure, citing Sprint 19 §3.5 as
prior evidence. See §11.

## 5. Files changed

**Track A bundle (`R-already-called-prompt-consumption`):**

- `server/src/main/resources/prompts/system_prompt.txt` —
  EDITED. Added a new "Re-using prior tool results (the
  `already_called` projection slot)" paragraph between the
  Rules section and the DISCOVER phase guidance. The paragraph
  names the slot, describes its content in observable terms
  (`arguments_hash`, `at_step`, cross-reference to
  `accumulated_tool_results`), specifies the LLM's reuse-the-
  prior-payload action when its planned call matches a slot
  entry, leaves the decision to the LLM (soft signal: "you
  own the judgement"; "the slot does not block dispatch"),
  and specifies the empty-array semantics. The teaching is
  principled — no `search_knowledge` / `resolve_article` /
  UC-A / UC-B / ... / UC-K / UC-FP mentions in the block; the
  rule is "this guidance applies to every tool — there is no
  tool-name or use-case branching."

**Track A regression test:**

- `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java`
  — NEW. Two tests:
  1. `systemPrompt_teachesAlreadyCalledSlot_principled` —
     asserts the four anchors are present (`already_called`,
     `arguments_hash`, `accumulated_tool_results`, and a soft-
     signal ownership phrase such as "you own" / "you may" /
     "not block").
  2. `systemPrompt_teaching_doesNotBranchOnToolNameOrUseCase` —
     asserts a 1400-character window centred on the
     `already_called` mention contains no `search_knowledge`,
     no `resolve_article`, and no UC id (`UC-A`...`UC-K` /
     `UC-FP`).
  Test class loads `prompts/system_prompt.txt` via
  `ClassPathResource`; identical resource-loading pattern to
  `LlmInvocationService.SYSTEM_PROMPT_PATH`. Tests pass:
  `mvn -Dtest=AlreadyCalledPromptConsumptionTest test` →
  Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.

**Track B:** no files changed (investigation-only).

**Server suite re-run after the edits:** 898 / 0 / 0 / 1 (1
pre-existing skip).Sprint 20 baseline was 894 / 0 / 0 / 1;
Sprints 21 / 22 added tests (none in the runtime path) bringing
the pre-Sprint-23 baseline to 896; this sprint's +2 tests
(both in the new file) bring the total to 898. No regressions
in any prior test. Test runtime ~26.5 s.

**No edit to:**

- Any eval-spec surface
  (`eval_interactive/case_specs/**`,
  `eval_interactive/case_spec_overrides.yaml`,
  `eval_interactive/personas*.yaml`, judge rubric, case
  families, shadow case families).
- Any foundational doc (`docs/foundational/**`).
- Any governance doc
  (`docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`).
- Any sprint archive (`docs/sprints/sprint-001..022-*.md`).
- `docs/runtime_freeze_and_risk_policy.md` (no Tier-0
  candidacy).
- FAQ corpus (`data/faq/**`,
  `FAQ-knowledge_include_help_url.csv`).
- Deadline / timeout configuration.
- Model / provider configuration.
- `docs/action_bank.md` (dev agent does NOT edit; §11 records
  recommended deltas for the deliver agent to apply on close).
- `PhaseEvaluator.java` / `AgentRunLoopImpl.java` /
  `ContextProjectionBuilder.java` / `ToolDispatcher.java` —
  read but unchanged (Track B investigation-only).
- `BotSession.java` and the `db/migration/` directory — Track B's
  proposed `consecutiveDeadlineCount` column is **not** added in
  this sprint; it lives in the deferred R-item.

## 6. Layer-classification self-walk

Restating the §11 stanza from `docs/sprint_objective.md` against
the actual deliverables.

| track | declared candidate layers | walk + actual |
|-------|---------------------------|---------------|
| Track A — landed bundle | `prompt_projection` \| `semantic_planner` \| `infra` \| `human_review_required` | §3.2 first-match-wins walk: Q1 no (no infra crash; the dispatcher does not amplify); Q2 no (no Tier-0 invariant — the slot does not enforce); Q3 **YES** — the LLM lacks a soft signal about prior identical-args calls; the existing slot is in the projection but the prompt has zero teaching about it. The fix surfaces an additional teaching paragraph in the static system prompt, which is a `prompt_projection` change. First-match wins → **`prompt_projection`**. Confirmed. |
| Track B — investigation-only | `infra` \| `prompt_projection` \| `human_review_required` | §3.2 walk per case in §4.2 matrix: Q1 fires for the clean-loop / emits+recovers / interleaved cases (the placeholder is emitted by the `infra` deadline path). The proximate fix shape is **`infra`** (a deterministic runtime-emission change). No Tier-0 invariant; no `java_guard` candidacy. Track B does NOT land a fix in this sprint; the layer attribution is recorded for the deferred R-item. |

§3.3 "no Java guard by default" check: no walk landed on
`java_guard`. No `human_review_required` flag fired. The cs_192
stop-gate (objective §11 reminder) did NOT trigger — Track A's
bundle is a static prompt edit, not a Java-guard addition.

## 7. Anti-hardcode self-walk (§4.1, nine questions)

The Sprint 23 PR has a Track A `prompt_projection` bundle (the
prompt teaching paragraph + regression test) plus Track B
investigation-only output. Track B is exempt (no code change);
Track A is semantic-touching and answers below.

1. **Keyword / regex / if-else / enum / per-UC matrix added for a
   semantic decision?** No. The teaching paragraph names a
   projection slot and describes its content in observable terms.
   It does not branch on tool name, UC, or user content. The
   regression test `systemPrompt_teaching_doesNotBranchOnToolNameOrUseCase`
   asserts the principled shape mechanically.
2. **Tier-0 invariant justification?** N/A (Q1 = no). The slot
   was already wired in Sprint 20; this sprint only teaches the
   LLM what the slot means. No Tier-0 invariant added; no
   runtime short-circuit; the LLM owns the re-emit decision per
   §1.3.
3. **Soft-signal projection alternative considered?** YES — the
   teaching paragraph IS the soft-signal consumption mechanism.
   The slot was the soft signal (Sprint 20); the teaching
   completes the LLM-side awareness so the soft signal is
   actionable.
4. **Visible-eval case text encoded into runtime / prompt /
   judge?** No. No `cs_NNN` string is written into the prompt;
   no eval phrase ("messages & replies", "trust & safety report
   intake", etc.) is encoded. The teaching is generic to the
   slot's content.
5. **Semantic ownership moved from LLM to Java?** No.
   Constitution §1.3 LLM-owns scope is unchanged. The slot adds
   an observable field; the teaching tells the LLM how to read
   it; the LLM still owns the next-action decision (including
   the freedom to re-emit if it has new information).
6. **Prompt grew an if-else block?** No. The new paragraph is
   principled prose with three observable-state bullet points
   ("when the planned call matches a slot entry"; "you own the
   judgement"; "empty array means no prior dispatch"). The
   bullets describe *observable state and its implications*, not
   *if X then do Y* runtime branches.
7. **Tool schema, capability / permission, PII / safety floor,
   grounding floor preserved?** Yes. No tool schema edit. No
   permission boundary change. The teaching does not reference
   PII content; the slot carries an arguments **hash** (PII-
   safe). The grounding-floor diagnostics
   (`docs/current/faq_grounding_contract.md`) are not touched.
8. **Generalization eval coverage (target / neighbor / negative /
   shadow)?** See §8 below. The visible-eval targets are the six
   Track A target cases re-derived in §3.1. Neighbor / negative
   are partly covered by the Sprint 20 case-family content for
   the affected briefs; shadow is the Sprint 20 shadow set
   (read-only by dev agent — see Sprint 20 `_ACCESS_BOUNDARY.md`).
   The narrow Java regression test asserts the teaching's
   PRESENCE + SHAPE; the empirical "reverses the shape on at
   least one target case" demonstration requires a live smoke
   rerun, deferred to the deliver agent / human on close.
9. **Temporary change → sunset plan?** Not temporary. The
   teaching paragraph is the long-term `prompt_projection`
   consumption layer for the Sprint 20 slot — Sprint 19 §4.2
   named this as Layer 1 (soft signal + LLM consumption); Sprint
   20 landed the slot half; Sprint 23 lands the prompt half.
   The conditional follow-on
   (`R-idempotent-read-tool-short-circuit`) is the harder Layer
   2 fix (orchestrator-level idempotency); it remains a
   conditional R-item — open only if the soft-signal-plus-
   teaching approach proves insufficient in production.

**Sprint 23 PR-level verdict (per §4.1):** **`approve`** — Track
A is the canonical Sprint 19 §4.2 Layer 1 soft-signal-plus-
teaching shape (no semantic hardcode, no enforcement, principled
teaching, regression test verifies the shape); Track B is
investigation-only (no code change → exempt from per-PR
anti-hardcode review per §4.1 exemption clause). The teaching's
principled-shape regression test
(`systemPrompt_teaching_doesNotBranchOnToolNameOrUseCase`)
makes future regressions visible.

## 8. Generalization-coverage table

Per `docs/current/iteration_governance.md` §5.1 four-class
contract.

| class | count | source / scope | visibility |
|-------|-------|---------------|------------|
| **target** | 6 (Track A) | cs_002, cs_014, cs_015, cs_040, cs_259, manual-probe — the six cases with ≥2 `search_knowledge` calls per §3.1 | visible to dev agent (re-derived from `results.json` + manual-probe brief) |
| **neighbor** | 2 per family × ≥4 affected families (Sprint 20 case-family content) | Sprint 20 `case_families/cs001_uc_c_template_escalate/`, `cs011_uc_d_description_ignored/`, `cs038_uc_j_intake_redundancy/`, `cs040_uc_k_disengaged_jargon/`, `cs259_uc_f_payment_question/`, `manual_probe_uc_a_resolve_must/` neighbor classes — these cases share the broader "prompt-projection consumption" shape that the Sprint 23 teaching addresses, not the specific repeated-FAQ surface per se | visible to dev agent (Sprint 20 referenced via manifests) |
| **negative** | 2 per family × ≥4 families (Sprint 20 case-family content) | Sprint 20 `case_families/*/negative/*.yaml` — cases where the bot SHOULD emit a fresh tool call (e.g. new information warrants a fresh search); the teaching's "you own the judgement / fresh call may be warranted" clause is designed to preserve this behaviour | visible to dev agent |
| **shadow** | 2 per family × ≥4 families (Sprint 20 case-family content) | Sprint 20 `case_specs_shadow/case_families/*` — held-out variants not loaded by the dev agent; read by human + review agent on close | dev agent did NOT consume (Sprint 20 `_ACCESS_BOUNDARY.md` honoured) |

**Coverage gap explicit (per §5.1 + Sprint 23 objective §11):**

- The new Java regression test asserts the **teaching's presence
  and principled shape**; it does NOT empirically demonstrate
  "reverses the repeated-FAQ shape on at least one target
  case" (the dev prompt §10 bar). Empirical demonstration
  requires a live smoke rerun. Deferred to the deliver agent /
  human on close.
- The Sprint 20 case families cover the broader prompt-
  projection consumption surface but were NOT specifically
  built around the `already_called` slot. The four-class
  coverage for the slot-specific shape would require an
  authoring follow-on (out of Sprint 23 scope per the §9
  NOT-in-scope hard fence).

## 9. Sprint-objective-met check (per-bullet PASS / PARTIAL / GAP)

Walking every "Hard fence" and every "Success metric" in
`docs/sprint_objective.md`.

### §3 hard fences (all ✓)

- Do not treat placeholder coalescing as the root cause unless
  evidence shows root cause is purely user-facing repeated
  fallback messaging. **PASS** — Track B is investigation-only;
  no coalesce shipped; the deferred R-item explicitly names the
  shape but does not claim user-facing-only is the deeper
  root cause.
- Do not assume the 5-case Cluster C list is accurate. **PASS** —
  re-derived independently from results.json; deviations from
  Sprint 19 §3.7 reported in §4.1.
- Do not conflate cs_040's placeholder loop with its UC-K → UC-C
  routing issue. **PASS** — both §3.3 and §4.4 explicitly separate
  the two shapes; the Track A bundle's regression test does NOT
  assert routing behaviour.
- Do not ship eval-only bookkeeping. **PASS** — Track A's bundle
  is a runtime-prompt change (LLM-facing teaching), not an
  eval-spec change.
- Prioritize user-visible reliability and problem-solving
  behaviour over pass-rate improvements. **PASS** — Track A's
  fix directly addresses the user-visible duplicate-API-call
  shape; Track B is investigation-only specifically because the
  bundle gate is strict on root-cause clarity.
- Do not widen the deadline budget without latency evidence.
  **PASS** — no deadline-budget edit; no model config edit.
- Do not widen the deadline budget in this sprint period (even
  if latency evidence exists, that is a follow-on R-item).
  **PASS** — no edit; latency data was not collected in this
  sprint.

### §13 success metrics

#### §13.1 Track A

- If bundled fix landed: regression test demonstrates the narrow
  fix reverses the repeated-FAQ shape on at least one target
  case. **PARTIAL** — the Java regression test asserts the
  teaching's presence and principled shape. The empirical
  "reverses the shape on at least one target case" demonstration
  requires a live smoke rerun (not feasible in this session
  without LLM-provider config); explicitly deferred to deliver
  agent / human on close.
- Root-cause matrix is complete: every target case has a layer
  attribution + evidence excerpt. **PASS** — §3.2 matrix covers
  all six target cases.
- Follow-on R-items name remaining causes with supporting
  evidence. **PASS** — §11 names the disposition for hypothesis
  (b) (`R-runtime-orchestrator-tool-call-deduplication` stays
  open), (d) (deferred standalone investigation), and (e)
  (n=2 ladder, below the user's bar).

#### §13.2 Track B

- If bundled fix landed: regression test demonstrates session-
  scope coalesce produces ONE placeholder + ONE honest next-step
  message across 2 consecutive deadline turns. **N/A** — Track
  B did not bundle.
- Root-cause matrix distinguishes placeholder-loop cases from
  emits+recovers cases from `semantic_planner`-failure cases.
  **PASS** — §4.2 matrix with the four classifications (clean
  loop / emits+recovers / interleaved / zero) is in §4.1.
- If model-latency named as root cause, the deferred R-item
  carries supporting latency data. **PARTIAL** — model-latency
  is named as the deeper-cause hypothesis but cannot be
  confirmed without latency data (not in results.json);
  proposed R-item `R-llm-latency-budget-investigation` carries
  the Sprint 19 §3.7 hypothesis as the starting evidence and
  scopes the latency-collection task.

#### §13.3 Both tracks

- Full server suite green (Sprint 20 baseline 894 / 0 / 0 / 1).
  **PASS** — 898 / 0 / 0 / 1 (pre-Sprint-23 baseline was 896
  after Sprints 21 / 22 test additions; +2 from this sprint).
- No eval-spec edits (verified by file-path check at handoff).
  **PASS** — see §5 "No edit to".
- No §1.7 violations (verified by §7 anti-hardcode self-walk).
  **PASS** — all nine questions answered.
- No Tier-0 candidacy. **PASS** — §3.2 walk did not land on
  `java_guard`; no Tier-0 invariant added.
- No deadline-budget widening (verified by config-file check).
  **PASS** — no edit to deadline / timeout configuration.
- Pass-rate is observed and reported but is NOT the acceptance
  bar per UX-over-pass-rate framing. **PASS** — the dev prompt
  is honoured; pass-rate is not in this handoff's success
  criteria.

## 10. Open questions for human

1. **Track B narrow-fix bundle: ship in next sprint, or wait for
   latency data?** Sprint 23's conservative read of the bundle
   gate ("ONLY if root cause is purely user-facing repeated
   fallback messaging") deferred the placeholder coalesce + honest
   next-step fix to an R-item. The R-item proposal is concrete
   (PhaseEvaluator branch + `consecutiveDeadlineCount` column +
   regression test bar). A future sprint can land it as a pure
   `infra` deterministic fix WITHOUT latency data, framed as
   "user-visible UX repair; latency-cause investigation is
   independent". Alternatively, latency data can be collected
   first to disambiguate the root cause. **Recommendation:** ship
   the narrow UX fix in the next sprint; collect latency data
   in parallel via a separate `R-llm-latency-budget-investigation`
   R-item. The UX fix is independently defensible (the bot
   should not say the same thing twice regardless of why
   deadlines hit).

2. **Smoke rerun to verify Track A's empirical "reverses the
   shape" bar.** The Java regression test asserts the teaching's
   presence and principled shape, but the dev prompt §10 bar
   requires empirical demonstration of repeated-FAQ shape
   reversal on at least one target case. A smoke rerun of the
   2026-05-10 case set after Sprint 23 lands would generate
   the evidence. Who triggers the rerun (deliver agent on
   close / human / next dev sprint)?

3. **`R-prompt-phase-plan-directive-followship` n-ladder
   threshold.** Two observations now (cs_259 §I0 violation +
   manual-probe RESOLVE-MUST clause). The user's prior bar was
   "controlled multi-shape testing before opening an R-item".
   The Sprint 20 case-family content for cs_259 / manual-probe
   provides some of that controlled scaffolding. Is the bar
   met or close to met? If yes, the next sprint candidate
   shifts.

4. **cs_040 routing failure disposition.** The objective §5
   asked the dev to propose a disposition. §4.4 recommends
   opening a new R-item `R-cs040-uc-k-topic-subject-routing`
   (`prompt_projection`) rather than broadening
   `R-prompt-phase-plan-directive-followship`. The new R-item
   would be n=1 (cs_040 only); per the user's
   controlled-multi-shape-testing bar, it may not yet warrant
   opening. Alternative: name it as a "single-instance
   observation" in the action bank without a tracked
   R-item, deferring opening until similar shapes recur.

5. **Teaching the LLM about `accumulated_tool_results`
   explicitly.** Sprint 23's teaching paragraph cross-references
   `accumulated_tool_results` as the place to read the prior
   payload when the slot matches. But `accumulated_tool_results`
   itself has zero standalone teaching in `system_prompt.txt`.
   A follow-on prompt sprint could add a short paragraph
   explaining the slot's lifecycle (last-write-wins per tool
   name, populated by every successful dispatch in the run).
   Out of Sprint 23 scope; proposed in §11.

## 11. Action-bank deltas (proposed; deliver agent applies on close)

The dev agent does NOT edit `docs/action_bank.md` directly. The
deltas below are recommended for the deliver agent to apply on
close, mirroring the Sprint 19 / Sprint 20 / Sprint 21 / Sprint
22 precedent.

### Updated rows

- `R-already-called-prompt-consumption` (action_bank.md line
  484; `prompt_projection`) — change disposition from
  `proposed (Sprint 20 §12 + §11 open question 5)` to
  `done — teaching paragraph landed in
  server/src/main/resources/prompts/system_prompt.txt (between
  the Rules section and DISCOVER phase guidance); regression
  test AlreadyCalledPromptConsumptionTest (2 tests) passing;
  full server suite 898/0/0/1; see Sprint 23 handoff §3 / §5`.
  Note that the empirical "reverses the shape on at least
  one target case" smoke-rerun verification is deferred to a
  live rerun by the deliver agent or next sprint.

### New R-items proposed

- `R-slow-llm-placeholder-coalesce-honest-next-step` (`infra` —
  narrow UX fix; conditional follow-on to Sprint 23 Track B
  investigation). Sprint 23 Track B confirmed the proximate
  cause (PhaseEvaluator.java lines 754–770 emit identical
  placeholder text on every `DEADLINE_EXCEEDED` outcome without
  session-scope state-tracking; consecutive deadlines therefore
  produce back-to-back identical bot messages, which the runtime
  loop-detector flags). Proposed scope: add a
  `consecutiveDeadlineCount` column on `BotSession` (mirror of
  V12 `runtime_error_count`); increment in PhaseEvaluator's
  `DEADLINE_EXCEEDED` branch, reset on any non-deadline outcome;
  first deadline emits existing placeholder; second consecutive
  deadline emits principled honest-next-step text naming the
  slowness and offering an actionable next step (handover /
  retry). Regression test bar: 2 consecutive `DEADLINE_EXCEEDED`
  outcomes produce ONE placeholder + ONE honest message. NO
  deadline-budget widening; NO model config change. Layer:
  `infra` (deterministic state-tracking + text substitution).
  Source: Sprint 23 handoff §4.3.
- `R-llm-latency-budget-investigation` (`infra` — diagnostic;
  paired with the R-item above). The Sprint 19 §3.7 hypothesis
  (model switch `f2d4cb2 fix: switch primary llm to
  deepseek-v4-flash` widened latency variance) remains
  unverified because per-turn LLM latency data is not in
  `eval_interactive/results/20260510-134558/results.json`.
  Proposed scope: instrument per-turn LLM round-trip timing in
  `LlmInvocationService.invokeChat` (or surface the existing
  log-emitted timing into a structured field on results.json);
  collect a baseline distribution from a smoke rerun; compare
  to pre-`f2d4cb2` baseline if available. Output: a data table
  per case + per turn that distinguishes "deadline hit because
  model is genuinely slow" from "deadline hit because budget
  was too tight". Decision input to whether a budget /
  retry / model change is needed (out of Sprint 23 scope per
  §3 hard fence). NO budget edit in this R-item — investigation
  only. Source: Sprint 23 handoff §4.2 hypotheses (a) + (b).
- `R-cs040-uc-k-topic-subject-routing` (`prompt_projection`;
  n=1; conditional opening). cs_040 active_use_case=UC-C in
  the 2026-05-10 smoke run vs expected UC-K (form_context
  `topic_subject=technical issue intake`); Sprint 19 §3.5
  documented the topic_subject cue not anchoring routing
  post-`8282783`. Proposed scope: investigate whether the
  topic_subject projection signal is reaching the routing
  prompt with sufficient salience; if not, propose a soft-
  signal addition (e.g. surface the topic_subject in the
  routing projection's `routing_signals` slot with appropriate
  weight). Disposition gate: open only if controlled multi-
  shape testing surfaces a second instance of the same
  routing-cue-loss shape. Source: Sprint 23 handoff §4.4 +
  Sprint 19 §3.5.
- `R-accumulated-tool-results-prompt-consumption`
  (`prompt_projection`; conditional). `accumulated_tool_results`
  is in every projection (since Sprint 8) but
  `system_prompt.txt` has zero standalone teaching about the
  slot. Sprint 23's `already_called` teaching paragraph
  cross-references it but does not stand alone as teaching for
  `accumulated_tool_results`. Proposed scope: add a short
  paragraph (parallel to the Sprint 23 `already_called`
  paragraph) describing the slot's lifecycle (last-write-wins
  per tool name; populated by every successful dispatch in the
  run; the LLM should consult it before deciding to re-emit
  ANY tool). Disposition gate: conditional on Sprint 23's
  teaching not being sufficient — open if a follow-on smoke
  rerun shows the repeated-FAQ shape persisting despite the
  Sprint 23 fix. Source: Sprint 23 handoff §10 question 5.

### Out-of-scope deferrals (Sprint 23 did NOT investigate)

- All `R-*` items in `docs/action_bank.md` §5.2 not named
  above (Wave A5 / A6 L3 review batch follow-ups, FAQ corpus
  audit per UC, duplicated-greeting projection fix,
  faqMissCount threshold, L3 judge form-context trust rubric,
  UC-K case_id binding, per-case trace dump, sprint-narrative
  reconciliation). All explicit per
  `docs/sprint_objective.md` §9 NOT-in-scope hard fence.
- `R-prompt-phase-plan-directive-followship`
  (action_bank.md line 450) — stays at `proposed (Sprint 19
  §11)` with the n=2 observation strengthened per Sprint 23
  manual-probe walk; still below the controlled-multi-shape-
  testing bar per user's prior decision.
- `R-runtime-orchestrator-tool-call-deduplication`
  (action_bank.md line 426) — stays at the Sprint 18 / manual-
  probe disposition; the dispatcher non-dedup finding is
  confirmed in Sprint 23 §3 (cross-case ruling out of
  hypothesis (b)) but Track A's bundle addresses the LLM-side
  consumption rather than the orchestrator-side dedup. The
  orchestrator-dedup R-item remains open as a Layer-2 fallback
  if the soft-signal-plus-teaching approach proves
  insufficient.

## 12. Next recommended action

**Verdict-section placeholder for human + deliver agent to fill
on close.** Recommended verdict shape (mirroring Sprint 19 /
Sprint 20 / Sprint 21 / Sprint 22 precedent):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
summary: <one paragraph naming what landed, what deferred, and
        whether the bundle gate / hard fences / §1.7 forbidden
        list were respected>
```

**Recommended next sprint candidate:**
`R-slow-llm-placeholder-coalesce-honest-next-step` — the narrow
UX repair Track B deferred. The R-item is concrete (scope,
regression bar, layer all named in §11) and the proximate-cause
evidence is conclusive per Sprint 23 §4.2. Pairing with
`R-llm-latency-budget-investigation` (which can run in parallel
as a diagnostic) lets the next sprint close the user-visible
artefact AND collect the latency data needed to decide whether
a deeper model / budget change is warranted later.

**Parallel candidate:** a smoke rerun of the 2026-05-10 case
set to empirically verify the Sprint 23 Track A teaching
reverses the repeated-FAQ shape on at least one target case
(per dev prompt §10 bar). This is not a full sprint — a
deliver-agent-triggered eval rerun + a short results-diff
note in `docs/10-handoff.md` is sufficient.

**Conditional follow-on:**
`R-accumulated-tool-results-prompt-consumption` (§11) — open
only if the next smoke rerun shows the repeated-FAQ shape
persisting despite the Sprint 23 teaching, indicating that the
LLM needs explicit teaching about `accumulated_tool_results`
in addition to the `already_called` cross-reference Sprint 23
provided.
