# Sprint 19 Handoff — Smoke Regression Investigation + Orchestrator Tool-Call De-Dup (parallel A + B)

Date: 2026-05-13
Branch: `design-v1-without-human-review`
Sprint class: investigation-class, bundle-or-defer per
`docs/sprint_objective.md` §"Bundle-or-defer policy". Two disjoint
tracks (A — smoke regression, B — orchestrator de-dup) run under one
sprint scope. **Outcome: proposal-only on both tracks.** No
runtime / test / prompt / CaseSpec / judge / FAQ corpus / eval-harness
edit was made; no smoke rerun was triggered. Sprint 19 deliverables
are this handoff plus the `docs/10-handoff.md` lead refresh.

## 1. Context Pack

Per `docs/current/agent_context_guide.md` Context Pack Prompt;
produced before any code- or doc-reading was committed.

**Relevant docs**

- `docs/sprint_objective.md` (current-runtime; status: current) —
  Sprint 19 scope; the §"Bundle-or-defer policy" + §"Layer-
  classification + anti-hardcode stanza" + §"Do not implement"
  sections are normative.
- `docs/current/iteration_governance.md` (durable-connective;
  status: current) — Constitution §1 (LLM-vs-Runtime ownership +
  Iteration rule + forbidden list), §3 Fix Layer Classification
  Checklist (§3.1 layer set, §3.2 first-match-wins walk, §3.3 "no
  Java guard by default"), §5 Eval Acceptance Rules (acceptance
  bars + shadow allowance for sprints pre-dating the split), §7
  required sprint-objective stanza.
- `docs/current/doc_governance.md` (durable-connective; status:
  current) — tier model + source-of-truth rules; this handoff is a
  `current-runtime` per-sprint write-up keyed off `this file` for
  the §3.2 walks + Track B contract write-up.
- `docs/current/agent_context_guide.md` (durable-connective;
  status: current) — per-task reading lists used to scope this
  investigation (Runtime / phase machine / drift for Track A;
  Tool schema / tool policy for Track B; Eval / governance
  cross-cutting).
- `docs/current/runtime_contract.md` (current-runtime; status:
  current) — the post-Sprint-13 runtime / risk freeze the §3.2 Q2
  walk reads against. No new Tier-0 invariant is opened in
  Sprint 19.
- `docs/current/faq_grounding_contract.md` (current-runtime;
  status: current) — referenced for the §5.1 grounding-floor
  acceptance bar; not directly edited.
- `docs/sprints/sprint-018-handoff.md` (sprint-archive; status:
  archived) — §4 brief catalogue, §6 layer-classification self-
  walk, §8 R-item summary including
  `R-smoke-regression-investigation` (§8.6) and
  `R-runtime-orchestrator-tool-call-deduplication` (§8.5), §10
  open questions. **Read but not edited.**
- `docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`
  (diagnostic; status: current) — Track B trace input; the only
  artefact for the manual-probe session. **Read but not edited.**
- `eval_interactive/results/20260505-235231/results.json`
  (current-runtime; status: diagnostic snapshot) — Track A PASS
  baseline (label `sprint8-r2`; 9 / 14 pass). The per-case
  `transcript` array is the divergence evidence for the 6 regressed
  cases.
- `eval_interactive/results/20260510-134558/results.json`
  (current-runtime; status: diagnostic snapshot) — Track A FAIL
  baseline (label `smoke_rerun_20260510-214558`; 3 / 14 pass).

**Relevant code paths** — sampled to ground the §3.2 walks. None
edited.

- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoop.java`
  (interface) and `AgentRunLoopImpl.java` (40.6 KB) — the bounded
  model↔tool worker. Tool dispatch occurs inside the
  `for step < maxToolSteps` outer loop (line 171) and the
  `for ToolCall call : calls` inner loop (line 278). **There is
  no de-duplication of identical tool calls anywhere on this
  path.** `accumulatedToolResults.put(toolName, result.getData())`
  at line 469 overwrites the previous entry under the same
  `toolName` key — the side-effect (real API round-trip) is not
  short-circuited.
- `server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java`
  (8.9 KB) — central dispatcher. The placeholder-on-slow-tool
  branch at line 147–153 emits a `placeholderMessage` on
  `result` when tool latency exceeds 1.5 s; this is the *tool*
  placeholder, distinct from the *LLM-call* placeholder at
  `PhaseEvaluator.java` line 765. Dispatch is unconditional per
  invocation; no idempotency-key check.
- `server/src/main/java/com/gumtree/csagent/service/runtime/LlmInvocationService.java`
  (15.0 KB) — wraps `LlmClient.chat`. Sprint 8.1 §M2 propagates
  `LlmDeadlineExceededException` (`AgentRunResult.deadlineExceeded`)
  and `LlmUnavailableException` (`AgentRunResult.llmUnavailable`)
  honestly rather than synthesising the safe-escalation. The
  underlying `OpenAiCompatibleLlmClient` retries once on transient
  failure; `FallbackLlmClient` engages a different provider on
  transient failure. If both fail, **infra-class failures** (timeout
  / 5xx / 429 / 401 / 403 / transport) propagate as exceptions; the
  legacy safe-escalation path remains for parse / unknown errors.
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  line 754–770 — maps `DEADLINE_EXCEEDED` / `LLM_UNAVAILABLE` to a
  same-phase stay with the user-facing message
  `"Sorry, I'm a bit slow right now. Please try sending that again
  in a moment."` (deadline) / `"Sorry, I'm having trouble reaching
  the assistant right now. Please try again in a moment."`
  (unavailable). This is the placeholder five of the six Track A
  regressed cases exhibit in their FAIL-run transcripts.
- `server/src/main/java/com/gumtree/csagent/service/runtime/HandoverPayloadAssembler.java`
  line 165 — `payload.put("case_id", session.getCaseId())`. The
  `case_id` field in the escalation payload reads
  `BotSession.caseId` directly; if the session never had a case
  created via `CreateCaseControlledTool` (which writes `caseId`
  back via `PhaseEvaluator.java` line 1412–1414 and
  `ControlKernel.java` line 1490–1492), the assembled payload
  carries a `null` `case_id`.
- `eval_interactive/eval_interactive/scoring/outcome_checks.py`
  line 351–369 — `_check_case_id_present`: scores 0.0 with detail
  `"case_id missing from handover payload"` when the active UC is
  in the `case_id-required` set, outcome is escalated, handover
  data is present, but `case_id` is null / missing. The check was
  added by Codex round 3 / round 4 (2026-05-03 / 2026-05-04) and
  is present in *both* smoke runs.
- `eval_interactive/eval_interactive/scoring/composite.py` line 80
  — `conditional.append("case_id_present")` makes the check a
  hard L2 gate when `outcome_class == "escalate"` on a
  case-id-required UC.

**Doc status warnings**

1. **The sprint objective references "Trace directories beneath
   each results dir (per-case JSON traces)". They do not exist
   on disk.** `eval_interactive/results/20260505-235231/` and
   `eval_interactive/results/20260510-134558/` contain only
   `results.json` + `report.html`. The per-case `transcript` array
   embedded inside `results.json` is the only per-case trace
   artefact available; tool-call sequences, projection snapshots,
   and LLM raw responses for the regressed cases are not
   reachable from this checkout. Track A §3.2 walks below rely on
   `transcript` + `failure_tags` + `stop_reason` +
   `escalation_reason` + `l2_results`. The Track B walk uses the
   manual-probe brief as the trace narrative — no saved manual-
   probe trace file exists either.
2. **Sprint 18 handoff §10 question 1 says "Sprints 14 / 14.1 /
   15 / 16 all declared no-runtime-semantic-change", and the
   Sprint 19 objective preamble echoes the same. The git log
   contradicts this.** Between the two smoke runs
   (2026-05-05 23:52 → 2026-05-10 13:45) there are 40 commits
   including substantial runtime work (and one LLM model switch):
   - `8282783 feat: add targeted routing projection and intake orchestration` (Sprint 7)
   - `33b6f07 fix: persist intake fields across clarification turns` (Sprint 7.1)
   - `a99aa62 fix: narrow K0 fallback to evidence-positive escalations only` (Sprint 8.1)
   - `ec5a925 fix: add non-blocking chat entry and budgeted llm retry` (Sprint 8.1)
   - `3ec6d5c fix: distinguish clarification questions from final answers` (Sprint 8.1)
   - `f2d4cb2 fix: switch primary llm to deepseek-v4-flash, widen verification budget` (Sprint 8.1)
   - `fe88e87 fix: add non-blocking chat entry and discover phase boundary handling` (Sprint 8.1)
   - `16ca3c7 fix: close sprint 8.1 retry and replan budget blockers` (Sprint 8.1)
   - `2c813a4 fix: align resolve_article source_id and preserve max-steps raw response` (Sprint 8.2)
   - `7013f00 fix: align terminal tool contracts and trace result fidelity` (Sprint 9)
   - `fee7616 fix: sanitize trace tool errors and generic result data` (Sprint 9)
   - `cb8af10 feat: add runtime reroute MVP` (Sprint 10)
   - `4df13c8 feat: add progressive resolve MVP` (Sprint 11)
   - `0f881c4 fix: require terminal evidence for resolve confirmation` (Sprint 11)
   - `24c25ef test: harden runtime alignment validation` (Sprint 12; test-only)
   - `9415525 feat: add faq kb evidence lineage and citation observability` (Sprint 14)
   - `ea15e5f fix: persist faq grounding observability in trace` (Sprint 14.1)
   - `43408a5 chore: externalize policy scripts and retrieval config` (Sprint 15;
     touched `DriftDetector.java`, `KnowledgeSearchService.java`,
     `RerankService.java`, `application.yml`, `risk-keywords.yaml`,
     `templates.yaml`)
   - `2c62560 docs: define handover exactly-once contract and repro` (Sprint 16;
     `RequestHandoverTool.java` and `SessionManager.java` were touched, the
     commit body states "no runtime behaviour change" but the message-vs-diff
     contradiction is itself worth noting)

   The smoke regression sits on the cumulative delta of all of
   these — *not* on a single Sprint-14-or-later change. The
   2026-05-05 PASS baseline pre-dated all of Sprints 8.1 / 10 / 11 /
   12 / 14 / 14.1 / 15 / 16. The 2026-05-10 FAIL baseline post-
   dated all of them. **The R-item premise that "the bot was
   stable through Sprints 14–16 and the regression came from
   nowhere" is incorrect.** The regressed cases reflect what those
   sprints actually changed — predominantly an engagement-vs-
   template flip (the bot now tries harder before escalating)
   that exposed downstream slow-LLM / state-loss / payload-gap
   surfaces.
3. The 8 non-regressed neighbor cases include cs_192. cs_192's
   2026-05-10 result is `CONTRACT_VIOLATION:active_use_case`,
   `total_turns=0` — a markedly more severe shape than its
   2026-05-05 `L2_GATE:correct_outcome` failure. cs_192 itself is
   *not* in the 6 regressed (it failed in both runs), but the
   shape shift is recorded here as an observation; see §10.

**Source-of-truth decision** — for the Track A §3.2 walks the
authoritative artefact is the pair of `results.json` `transcript`
arrays (case-by-case divergence between PASS and FAIL); for the
Track B walk the authoritative artefacts are
`docs/diagnostics/failure-briefs/manual-probe-2026-05-13-...`
(trace narrative) and the AgentRunLoopImpl / ToolDispatcher source
(de-dup-contract reality). The sprint objective is normative for
scope; `iteration_governance.md` §3.2 is normative for layer
classification.

**Implementation status** — `not_started`. Sprint 19 produces no
runtime change. The two R-items (`R-smoke-regression-investigation`,
`R-runtime-orchestrator-tool-call-deduplication`) remain *open*
post-Sprint-19 but with their layer and remediation direction
classified per the walks below.

**Top risks before drafting**

1. **Anchoring on the Sprint 18 narrative** (Sprints 14–16
   declared no runtime change) — mitigated by reading the actual
   git log in the smoke gap window and listing the 18 code-touching
   commits explicitly in Warning #2 above. Walks below cite
   suspect commits when the transcript evidence points at them.
2. **Over-attributing the regression to one change.** With 18
   code-touching commits in the window, no single revert is the
   right answer. Walks below classify per case and explicitly
   identify the *shape* (slow-LLM placeholder loop, state-loss
   on intake) rather than blaming a single commit.
3. **Bundling a Track A `infra` fix.** The slow-LLM placeholder
   loop is the dominant Track A shape and looks like a clean infra
   target (the placeholder is emitted by `PhaseEvaluator` line 765
   without retry / coalescence on consecutive deadline events). The
   user brief restricts Track A bundles to `prompt_projection` soft
   signal additions; `infra` bundles are reserved for Track B. The
   sprint objective text in §"Bundle-or-defer policy" is broader,
   but the user brief constraint is narrower and wins.
   Resolution: classify these as `infra`, name the fix shape, and
   **defer** to a future infra-track sprint with a remediation
   proposal in §11.
4. **`cs_192` stop-and-escalate false-positive.** The 6 regressed
   cases do not show `CONTRACT_VIOLATION:active_use_case` on any
   2026-05-10 trace, so the cs_192-shape Tier-0 escalation clause
   does *not* fire in Track A. cs_192 itself shifted shape between
   dates (see §10) and is a near-miss neighbor; recorded as an
   observation rather than a stop trigger.
5. **Manual-probe brief vs code-reading contradiction on
   Track B.** The brief states "the T2 LLM Raw Response shows
   exactly one search_knowledge tool_call requested", implying
   orchestrator-side amplification; code reading of AgentRunLoopImpl
   shows no amplification path (one `ToolCall` → one `dispatch`).
   Section 4 walks both possibilities and ends on the most
   parsimonious read: the brief author saw the *last* LLM raw
   response (one tool_call) without seeing the *earlier-step* LLM
   responses (each with one tool_call) within the same T2 RESOLVE
   run, totalling three steps and three `search_knowledge`
   dispatches.

## 2. Sprint-objective recap

From `docs/sprint_objective.md` §Goal (quoted verbatim, key clauses):

> Resolve the two R-items in the Sprint 18 G1 backlog that block
> the G2 case-family construction:
>
> - **Track A — `R-smoke-regression-investigation` (P1).** Diagnose
>   the 64 % → 21 % smoke-set drop … over the 6 regressed cases
>   (cs_002, cs_011, cs_014, cs_038, cs_040, cs_066) … the dev
>   agent walks `docs/current/iteration_governance.md` §3.2 per
>   case and identifies the matching layer. Per-case bundle-or-
>   defer per §"Bundle-or-defer policy" below.
> - **Track B — `R-runtime-orchestrator-tool-call-deduplication`.**
>   From manual-probe 2026-05-13: 1 LLM request → 3 identical
>   `search_knowledge` executions … Investigate the 3 hypotheses
>   (phase-transition re-trigger, Turn 1 failed-call replay, LLM
>   new request), document the orchestrator's intended de-dup /
>   idempotency contract, and — if the per-case layer resolves to
>   `infra` — bundle the fix with a regression test. If the layer
>   resolves to `semantic_planner` (LLM emitted three identical
>   calls), defer the fix and produce a remediation proposal only.

Sprint 19 is **disjoint** by design: Track A diagnoses the smoke
baseline; Track B diagnoses orchestrator transport. The two tracks
were interleaved in one PR.

## 3. Track A — per-case §3.2 walk (6 cases)

Each subsection records: trace excerpt pair (PASS vs FAIL), §3.2
first-match-wins question + classified §3.1 layer, bundle / defer
decision per §"Bundle-or-defer policy", suspect code path /
commit. **All six cases defer.** The unifying observation under §3.4
follows.

### 3.1 cs_interactive_002 (UC-C, messages & replies)

`session_id` PASS = `d17445e9-e866-4752-b7ef-086cc921f489`;
FAIL = `dbdd95be-1d2f-4536-abe4-90a7ed658b34`. PASS:
`passed=true, esc=user_distress, turns=2, fails=[L3:relevance, L3:tone_appropriateness]`.
FAIL: `passed=false, esc=user_distress, turns=4, stop=loop_detected,
fails=[L1:trace_minimum, L2_GATE:correct_outcome, L2:escalation_timing,
L2:correct_outcome, L2:tool_sequence_match, L3:groundedness,
L3:relevance, L3:tone_appropriateness]`.

**Trace excerpt (PASS, 2026-05-05).** Bot template-escalates at T0,
T1 confirms transfer.

```
[T0 user|form_context] I am not receiving notifications from you when messages are arriving in my inbox.
[T0 bot|session_create]  Hi Richard! I'm having difficulty resolving this. Let me connect you with a specialist.
[T1 user] How long do I have to wait to sort this out? Its been this way since day 1.
[T1 bot]  This conversation has been transferred to a human agent. Please wait for them to respond.
```

**Trace excerpt (FAIL, 2026-05-10).** Bot engages at T0–T1 with a
grounded answer + source citation; T2 and T3 then emit the slow-
LLM placeholder, triggering `loop_detected`.

```
[T0 bot|session_create] Hi Richard! I'm here to help with your inquiry about messages & replies. Let me look into this for you.
[T1 user] How long do I have to wait to sort this out? Its been this way since day 1.
[T1 bot] Hi Richard, … Based on our help article (source: ka44J000000gKvyQAE), there are a few things to check: 1. Ensure your primary email …
[T2 user] I've checked my email settings and spam folder — everything looks correct …
[T2 bot] Sorry, I'm a bit slow right now. Please try sending that again in a moment.
[T3 user] Yes, absolutely — you shouldn’t need to enable marketing emails for message notifications. Let’s check your notification settings …
[T3 bot] Sorry, I'm a bit slow right now. Please try sending that again in a moment.
```

**§3.2 first-match walk.**

- Q1 — Is the session failing to start, crash on infra, or hit a
  timeout / OOM not caused by tool semantics? **Yes.** The
  placeholder text is emitted by `PhaseEvaluator.java` line 765 on
  `TerminalOutcome.DEADLINE_EXCEEDED`; the AgentRunLoop returns
  that outcome via `LlmInvocationService.invokeChat` propagating
  `LlmDeadlineExceededException`. Two consecutive deadline events
  (T2 and T3) emit identical placeholder text, which the runtime
  loop detector then flags as a loop.

**§3.1 layer:** `infra` (orchestration / timeout handling).
**Bundle?** Defer. Per the user brief §4 "Files in scope",
`server/**` runtime edits for Track A are restricted to
`prompt_projection` soft-signal additions; an `infra` fix to the
deadline-exceeded handler (e.g. coalescing consecutive placeholder
emissions into a single user-facing "still working" beat, or
holding the user side rather than echoing the same placeholder
text) sits outside that scope. **Defer to remediation proposal**
(§11 / Track A R-item).

**Suspect code / commits.** The placeholder branch was introduced
by `ec5a925 fix: add non-blocking chat entry and budgeted llm retry`
(2026-05-06 19:48) and refined by
`fe88e87 fix: add non-blocking chat entry and discover phase boundary handling`
(2026-05-07 01:08). The 2026-05-05 PASS baseline pre-dated both.
The model switch in `f2d4cb2 fix: switch primary llm to deepseek-v4-flash`
(2026-05-06 21:48) also widened verification budget — a slower
provider compounded with a non-coalescing placeholder branch makes
the two-turn placeholder loop more reachable.

### 3.2 cs_interactive_011 (UC-D, account & login)

PASS: `passed=true, esc=faq_miss_threshold_exceeded, turns=2,
fails=[L3:relevance, L3:tone_appropriateness]`.
FAIL: `passed=false, esc="" (no escalation), turns=3,
stop=goal_impossible, fails=[L1:trace_minimum,
L2_GATE:correct_outcome, L2:escalation_timing, L2:correct_outcome]`.

**Trace excerpt (PASS, 2026-05-05).** Template-escalate at T0.

```
[T0 user|form_context] Login issue
[T0 bot] Hi Wasim! I'm having difficulty resolving this. Let me connect you with a specialist.
[T1 user] Your be the third agent today … pw reset link sent … link is not recognised … incorrect again.
[T1 bot] This conversation has been transferred to a human agent. Please wait for them to respond.
```

**Trace excerpt (FAIL, 2026-05-10).** Bot engages at T1 with a
grounded answer (cache clear / incognito / paste link). User at T2
explicitly asks for human handover ("Can I speak to someone?"). No
T2 bot response exists; the user simulator emits stop_reason
`goal_impossible` and `L1:trace_minimum` flags the missing bot
turn.

```
[T0 bot] Hi Wasim! I'm here to help with your inquiry about account & login. Let me look into this for you.
[T1 user] Your be the third agent today …
[T1 bot] Hi Wasim, I'm sorry you've been going back and forth on this. From the article I've found, the password reset process should work, but it sounds like the link may be expiring or there's a browser issue. Please try these steps: 1) Clear your browser cache and cookies, 2) Use a private/incognito window, 3) Copy the reset link from the email and paste it directly into the address bar … If the issue persists, I'll escalate to a human agent right away.
[T2 user] I've tried all that — incognito, clearing cache, pasting the link manually. Still the same … Can I speak to someone?
<no T2 bot>
```

**§3.2 first-match walk.**

- Q1 — Infra failure / timeout / OOM? Ambiguous. The bot's
  missing T2 response is consistent with (a) a timeout that did
  not surface the placeholder (no `"Sorry, I'm a bit slow"` text
  in the transcript), (b) the user simulator quitting before the
  bot returned, or (c) an LLM call that produced an empty /
  unparseable response and the AgentRunLoop fallback string
  `"I'm having trouble processing your request."` somehow did
  not land. Without per-step trace JSON the three are
  indistinguishable. Q1 does NOT cleanly fire.
- Q2 — Tier-0 invariant? No.
- Q3 — Projection inadequate? No. The user's T1 message names
  "third agent today" + tried everything; T1 projection had the
  full description.
- Q4 — `skill_state` losing state across turns? No.
- Q5 — LLM chose semantically wrong action even with adequate
  projection and state? **Yes at T1.** The bot's T1 response
  promised "If the issue persists, I'll escalate to a human
  agent right away" but, when the user immediately asked for
  exactly that at T2, the bot did not produce an escalation.
  This is a planner-side failure to honour an explicit user
  handover request — the same `R-prompt-phase-plan-directive-
  followship` open-observation surface flagged in the Sprint 18
  G1 brief catalogue (cs_259 §I0 + manual-probe RESOLVE MUST,
  now plus cs_011 T2).

**§3.1 layer:** `semantic_planner` (primary). Secondary `infra`
not ruled out (the T2 silence may also be a timeout that escaped
the placeholder branch).
**Bundle?** Defer. `semantic_planner` is defer-only per policy.

**Suspect commits.** No single change. The engagement-vs-
template flip (`8282783 feat: add targeted routing projection and intake orchestration`
plus `3ec6d5c fix: distinguish clarification questions from final
answers`) made the bot try a grounded answer instead of
template-escalating; the planner side has not yet learned to
escalate on a clear-handover-request following a failed grounded
attempt.

### 3.3 cs_interactive_014 (UC-C, messages & replies)

PASS: `passed=true, esc=faq_miss_threshold_exceeded, turns=2,
fails=[L3:relevance, L3:tone_appropriateness]`.
FAIL: `passed=false, esc="" (no escalation), turns=4,
stop=loop_detected, fails=[L1:trace_minimum, L1:no_stall,
L2_GATE:correct_outcome, L2:escalation_timing, L2:correct_outcome,
L2:tool_sequence_match, L3:groundedness, L3:relevance,
L3:tone_appropriateness, STALL:PLACEHOLDER_WITHOUT_FOLLOWUP]`.

**Trace excerpt (PASS).** Template-escalate at T0; T1 confirms.

**Trace excerpt (FAIL).** Same shape as cs_002 — bot engages at
T0–T1, then T2 and T3 emit the slow-LLM placeholder, triggering
`loop_detected` and the `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP`
stall failure tag.

```
[T1 bot] Hi Paul, I understand your frustration. Gumtree allows you to set a primary email for receiving messages that can be different from your login email …
[T2 user] I already tried that — the contact email is stuck on the secondary one and won’t revert …
[T2 bot] Sorry, I'm a bit slow right now. Please try sending that again in a moment.
[T3 user] I've tried updating it in My Details, but the contact email stays locked …
[T3 bot] Sorry, I'm a bit slow right now. Please try sending that again in a moment.
```

**§3.2 first-match walk.** Q1 — Yes, same shape as cs_002. The
`STALL:PLACEHOLDER_WITHOUT_FOLLOWUP` fail tag is the eval-side
named version of the same condition: the runtime emitted a
placeholder, the user replied, the runtime emitted the same
placeholder again, no forward progress.

**§3.1 layer:** `infra`.
**Bundle?** Defer (same Track-A-infra scope restriction as cs_002).
**Suspect commits:** same as cs_002 (`ec5a925` + `fe88e87` +
`f2d4cb2`).

### 3.4 cs_interactive_038 (UC-J, trust & safety report)

PASS: `passed=true, esc=intake_complete_for_uc_j, turns=4,
fails=[L2:tool_sequence_match, L3:relevance, L3:tone_appropriateness]`.
FAIL: `passed=false, esc=turn_budget_exhausted, turns=5,
stop=bot_ended, fails=[L1:escalation_compliance, L3:relevance,
L3:tone_appropriateness]`.

**Trace excerpt (PASS).** 4-turn intake; on T3 bot says
"Let me connect you with a specialist" and the eval sees
`escalation_reason=intake_complete_for_uc_j`.

**Trace excerpt (FAIL).** Intake proceeds correctly through
T0–T2; at T3 the bot emits the slow-LLM placeholder ("Sorry, I'm
a bit slow …"); the user's T3 message is then repeated verbatim
at T4 (the user-simulator side did not advance); T4's bot
response is the runtime turn-budget-exhausted escalation message
("I've reached the limit of what I can assist with on this topic.
Let me connect you with a human agent who can help further."),
which surfaces `escalation_reason=turn_budget_exhausted` instead
of `intake_complete_for_uc_j`. The `L1:escalation_compliance` fail
tag fires because the CaseSpec expects the
`intake_complete_for_uc_j` reason, not `turn_budget_exhausted`.

**§3.2 first-match walk.**

- Q1 — Infra timeout: the placeholder at T3 is again a
  `DEADLINE_EXCEEDED` symptom; here it consumed one turn that
  should have been the bot's "Let me connect you …" completion
  message, pushing the loop past the turn budget at T4. **Yes,
  Q1 fires.**

**§3.1 layer:** `infra` (single deadline event consumed a turn,
turn-budget mapping then over-rode the would-be
`intake_complete_for_uc_j` escalation).
**Bundle?** Defer.
**Suspect commits:** `ec5a925` / `fe88e87` (placeholder branch);
the budget mapping at `PhaseEvaluator` line 754–770 surfaces
`turn_budget_exhausted` rather than letting the next iteration
complete intake.

### 3.5 cs_interactive_040 (UC-K, technical issue intake)

PASS: `passed=true, esc=intake_complete_for_uc_k, turns=2,
fails=[L2:tool_sequence_match, L3:relevance, L3:tone_appropriateness]`.
FAIL: `passed=false, esc="", turns=3, stop=loop_detected,
fails=[L1:trace_minimum, L1:no_forbidden_tools,
L1:intake_no_knowledge_tool, L2_GATE:correct_uc, L2_GATE:correct_outcome,
L2:escalation_timing, L2:correct_uc, L2:correct_outcome,
L2:tool_sequence_match, L3:relevance, L3:tone_appropriateness]`.

**Trace excerpt (PASS).** UC=UC-K resolved on T1.

**Trace excerpt (FAIL).** Bot's T0 greeting drops the
`"about technical issue intake"` UC-naming framing — instead
says `"I'd like to help you with your inquiry. Could you tell me
a bit more about what you need help with?"`, suggesting the
intake projection lost the topic-subject hint. UC is mis-
committed to UC-C (per `active_use_case=UC-C` on the FAIL
result — UC mis-route from UC-K). T1 and T2 emit slow-LLM
placeholders; loop detected. The `L1:intake_no_knowledge_tool`
fail tag indicates the bot called `search_knowledge` during
intake, which is a per-phase policy violation (search_knowledge
is RESOLVE-only on intake UCs).

**§3.2 first-match walk.**

- Q1 — Infra timeout? T1 and T2 emit the placeholder — yes, **Q1
  fires** for the surface failure (the loop-detected outcome).
- Secondary observations recorded even though Q1 wins:
  - Q3 — `prompt_projection`: the form_context "technical issue
    intake" topic_subject did not anchor the UC routing in this
    run; the routing-prompt cue may have been weaker post-
    `8282783` (Sprint 7 §I1 routing refactor). Recorded as a
    secondary `prompt_projection` concern.
  - Q4 — `skill_state`: the UC-K classification was lost / not
    persisted, and the bot drifted into UC-C territory.
    Recorded as a secondary `skill_state` concern.

**§3.1 layer:** `infra` (primary; placeholder loop). Secondary
candidates `prompt_projection` (routing) and `skill_state` (UC
persistence).
**Bundle?** Defer on all three candidates (primary `infra`
out-of-scope for Track A; secondary `prompt_projection` is
in-scope for a soft-signal bundle in principle but the
investigation is not yet conclusive enough to name *which* soft
signal — proposal first).
**Suspect commits:** `8282783` (routing refactor that may have
demoted the topic_subject cue), `33b6f07` (intake-field
persistence), `ec5a925` / `fe88e87` (placeholder branch).

### 3.6 cs_interactive_066 (UC-K, technical issue intake)

PASS: `passed=true, esc=intake_complete_for_uc_k, turns=3,
fails=[]`.
FAIL: `passed=false, esc=intake_complete_for_uc_k, turns=3,
stop=bot_ended, fails=[L2_GATE:case_id_present,
L2:case_id_present, L3:relevance, L3:tone_appropriateness]`.

**Trace excerpt (PASS).** 3-turn intake completes; escalation
payload carries `case_id` (L2 detail
`case_id_present: 1.0`).

**Trace excerpt (FAIL).** Same 3-turn intake shape; bot
escalates with `intake_complete_for_uc_k`; L2 detail says
`case_id_present: 0.0, "case_id missing from handover payload"`.

This is the **only** Track A regression that is not driven by
the slow-LLM placeholder loop. The agent transcripts are
functionally equivalent across the two runs; the regression is
the *escalation payload* missing the `case_id` field.

**§3.2 first-match walk.**

- Q1 — Infra crash / timeout? No.
- Q2 — Tier-0 invariant? No.
- Q3 — Projection inadequate? No.
- Q4 — `skill_state` losing state across turns? **Yes.** The
  intake completed (all required fields collected per the
  Sprint 7 §I2 intake-complete guard at AgentRunLoopImpl line
  299–333), and the escalation request fired the handover —
  but the session state never carried a `caseId`. The
  `HandoverPayloadAssembler` at line 165 writes
  `payload.put("case_id", session.getCaseId())`; if the session
  never had a case created via `CreateCaseControlledTool` (which
  is what writes `caseId` back via
  `PhaseEvaluator.java` line 1412–1414 / `ControlKernel.java`
  line 1490–1492), the payload carries a null `case_id` and the
  L2 hard gate fails.

**§3.1 layer:** `skill_state` (the intake-complete branch on
UC-K UCs no longer triggers / no longer persists the
`CreateCaseControlledTool` call before assembling the handover
payload).
**Bundle?** Defer. `skill_state` is defer-only per policy
(multi-turn state changes touch the surface the G2 case family
is supposed to exercise).
**Suspect commits.** Between the two runs, multiple commits
touched the relevant orchestration: `33b6f07` (intake-field
persistence across clarification turns), `16ca3c7` (Sprint 8.1
retry / replan budget), and `2c62560` (Sprint 16 — the commit
body says "no runtime behaviour change" but it added Javadoc to
`RequestHandoverTool` and `SessionManager.recordHandover`; a
behaviour-neutral docstring change cannot lose the case_id, but
the commit being adjacent in the timeline is worth recording
without naming it the cause). Most likely the auto-create-case
step is no longer reliably triggered on UC-K intake_complete
post-Sprint-8.1.

### 3.7 Cross-case observation

Five of the six regressed cases (cs_002, cs_011, cs_014, cs_038,
cs_040) share the **slow-LLM placeholder** surface (one or two
consecutive `"Sorry, I'm a bit slow right now. Please try
sending that again in a moment."` emissions → loop / stall /
turn-budget mapping). The placeholder branch was introduced by
`ec5a925` and refined by `fe88e87`; the underlying LLM-provider
switch in `f2d4cb2` (deepseek-v4-flash) increased per-call
latency variability, making the deadline-exceeded path more
reachable.

The sixth case (cs_066) is a different shape entirely
(`skill_state`: lost `case_id` binding on UC-K intake-complete
escalation).

**Neither shape has a fix that is bundle-eligible under the
Sprint 19 §"Files in scope" constraint** (Track A bundles are
`prompt_projection` soft-signal only). Both shapes are
documented in §11 as remediation proposals against the open
R-item `R-smoke-regression-investigation`, and §11 also opens
two new R-items naming the specific remediations.

## 4. Track B — orchestrator de-dup investigation

**Source artefact:** `docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`,
session `4a2f3680-02a8-4d13-8f0b-f99d7c249b57`. The manual-probe
trace itself is not saved as a file in this checkout; the brief is
the only narrative.

**Brief claim (paraphrased).** On T2 of the manual probe, `1 LLM
request → 3 identical search_knowledge executions, same params and
same results`. The brief proposes three hypotheses:

- (a) phase-transition DISCOVER → RESOLVE triggered RESOLVE init
  logic that re-queued the pending call;
- (b) the Turn 1 failed `search_knowledge` call (rejected by
  `ToolPolicyEnforcer` with `"Tool 'search_knowledge' is not
  allowed for use case 'none'"`) got replayed once tools were
  unlocked post-classify;
- (c) the LLM emitted three separate `tool_use` blocks in its T2
  response.

### 4.1 Walking the three hypotheses against the code

**Hypothesis (a) — phase-transition replay.**
`AgentRunLoopImpl.java` line 502–517 (`§M3`) returns
`AgentRunResult.useCaseIdentified` *immediately* when
`classify_use_case` commits a non-blank `activeUseCase` on a
DISCOVER plan. `ControlKernel` then performs a single bounded
same-turn replan into RESOLVE for the newly committed UC by
constructing a *new* AgentRunLoop invocation. The replanned
AgentRunLoop's `accumulatedToolResults` is a **fresh
LinkedHashMap** (line 153) — the DISCOVER run's accumulated
results do not carry over. The RESOLVE replan therefore does *not*
"re-queue" the DISCOVER `search_knowledge` call.
**Hypothesis (a) is not supported by the code.**

**Hypothesis (b) — Turn 1 failed-call replay.**
`accumulatedToolResults` is a *local* variable inside
`AgentRunLoopImpl.run(...)`. Each user turn invokes
`AgentRunLoop.run` with a fresh local map. State does not
persist across user turns (only `session` state does, and
`session` carries `intakeFields`, `activeUseCase`, `caseId`,
`recordOutcomeAttempted` etc. — *not* a queue of pending tool
calls). A `search_knowledge` rejection on T1 cannot persist into
T2's run-loop state.
**Hypothesis (b) is not supported by the code.**

**Hypothesis (c) — LLM emitted multiple `tool_use` blocks.** The
inner dispatch loop at line 278 (`for ToolCall call : calls`)
faithfully dispatches every entry in the LLM-emitted `tool_calls`
array. If the LLM emits 3 entries with `name=search_knowledge`
and identical arguments, the dispatcher executes 3 real round-
trips. There is **no de-duplication anywhere on this path**.

This is the *only* hypothesis the code supports cleanly.

However, the brief states `"the T2 LLM Raw Response shows exactly
one search_knowledge tool_call requested"`. There are two
readings of this claim:

- **Single-step reading.** One LLM call → one tool_call → one
  dispatch. Three executions cannot follow without amplification.
  Code says no amplification exists. **Contradiction.**
- **Multi-step reading.** The AgentRunLoop's outer loop (line 171)
  runs up to `maxToolSteps` steps; each step has its own LLM call
  and its own LlmCallEvent. The trace UI's "T2 LLM Raw Response"
  may surface only the *last* step's raw response (or only the
  one the reader inspected), even though three steps each issued
  their own LLM call carrying one tool_call. The
  `AgentRunResult.lastLlmRawResponse` field (line 156 / 214 /
  244) carries only the last LLM raw payload — a reader who saw
  that single field while observing three `ToolEvent` entries
  would naturally describe the situation as "1 LLM request → 3
  executions".

The multi-step reading is consistent with the code and produces
the observed `search_knowledge × 3` shape via the LLM re-
emitting the same call across three consecutive steps even
though `accumulatedToolResults.search_knowledge` was already
populated in step `n` when step `n + 1` ran.

### 4.2 De-dup / idempotency contract — current vs intended

**Current contract (de-dup contract for tool calls in the bounded
agent run loop).** None enforced. Specifically:

- The dispatcher at `ToolDispatcher.dispatch(...)` performs no
  same-call de-duplication. Every `ToolCall` passed in is
  executed.
- The AgentRunLoop's inner loop iterates over each `ToolCall` in
  the LLM-emitted list and dispatches each, unconditionally.
- Across loop steps, the LLM-side `accumulatedToolResults` map
  *overwrites* the prior entry under a same `toolName` key, so
  the projection visible to the next LLM step shows only the
  most-recent result. The orchestrator does *not* short-circuit a
  same-call re-emission with the cached prior result.
- For idempotent read tools (`search_knowledge`,
  `get_customer_context`, `lookup_customer_account`,
  `lookup_listing`, `get_message_moderation_context`,
  `get_moderation_review_context`), the cost of redundant
  execution is wasted latency + backend load. For write tools
  (`record_outcome`, `request_handover`,
  `create_case_controlled`), the cost is duplicate side-effects;
  the **handover-exactly-once contract**
  (`docs/proposals/handover_orchestrator_design.md`, Sprint 16
  §H0) is the canonical example of an unresolved write-side
  exactly-once requirement.
- `request_handover` is partly protected: the AgentRunLoop's
  inner loop short-circuits on `result.isSuccess()` (line
  486–490) — a first successful handover terminates the loop,
  preventing a second `request_handover` from dispatching in the
  same step. *Across* user turns it has no protection, and the
  Sprint 16 handover orchestrator is the named future invariant
  owner.

**Intended contract (proposal).** Two layers:

- **Layer 1 — soft signal in the projection.** When the
  `accumulatedToolResults` map already contains a successful
  result for tool `T` with arguments `A`, surface an explicit
  diagnostic slot to the LLM in the next step's projection (e.g.
  `already_called: [{tool: "search_knowledge", arguments_hash:
  "...", at_step: 0}]`). This is a `prompt_projection` soft
  signal — the LLM owns whether to re-emit. **No keyword /
  regex / hard branch** is introduced anywhere on the path.
- **Layer 2 — idempotent-read short-circuit (read tools only).**
  For tools the schema marks as side-effect-free
  (`search_knowledge`, `lookup_*`, `get_*_context`), the
  dispatcher MAY short-circuit a same-args re-emission within
  the same `AgentRunLoop.run(...)` invocation by returning the
  cached prior result *without* executing again. This is an
  `infra` change. Write tools (`record_outcome`,
  `request_handover`, `create_case_controlled`) must NOT use
  this short-circuit; they require a separate exactly-once
  contract owned by the `HandoverOrchestrator` (Sprint 16 §H0).

**Enforcement missing today.** Both layers. There is no
projection diagnostic for prior tool calls; there is no
dispatcher-side de-dup.

### 4.3 §3.2 walk on Track B

Per the multi-step reading of hypothesis (c) above, the LLM
emitted `search_knowledge` across three steps even though the
projection contained the prior step's result.

- Q1 — Infra failure / timeout? No (3 calls all succeeded; no
  exception / timeout / OOM).
- Q2 — Tier-0 invariant broken? No.
- Q3 — Projection inadequate so LLM couldn't choose correctly?
  *Partially.* The projection contained the most-recent
  `search_knowledge` result, which the LLM should have read
  before re-emitting. But the projection does **not** explicitly
  surface "you already called this tool with these args in step
  N" — there is no `already_called` diagnostic slot. A reasonable
  soft-signal addition would help the LLM avoid this shape.
- Q4 — `skill_state` losing state? No (single-turn loop).
- Q5 — LLM chose semantically wrong action even when projection
  / state correct? **Yes.** The previous-step `search_knowledge`
  result is in the projection (under the
  `accumulated_tool_results.search_knowledge` slot); the LLM
  should not re-emit. **This is the first-match-wins answer.**

**§3.1 layer:** `semantic_planner` (primary). Secondary
`prompt_projection` (the soft-signal `already_called` diagnostic
would help the LLM avoid the redundant emission).

**Bundle decision (per sprint objective + user brief).** The
sprint objective and user brief both say: if the Track B layer
resolves to `semantic_planner` → defer the fix and produce a
remediation proposal only. **Defer.** No code change.

### 4.4 Why this matters even though we defer

The de-dup gap is observable in the manual-probe trace (latency
cost: 3 × ~1.2 s ≈ 2.5 s wasted; load cost: 3 × backend search
load) and *could* manifest as a write-side duplicate (e.g. the
LLM emitting two `request_handover` calls or two
`create_case_controlled` calls across steps). The
write-side path is already a known concern per Sprint 16
(`docs/proposals/handover_orchestrator_design.md`); the read-side
gap is new evidence reinforcing the same design need. The
remediation proposal in §11 names both layers explicitly and
references the Sprint 16 design as the write-side anchor.

## 5. Files changed

- `docs/sprints/sprint-019-handoff.md` — **NEW** (this file).
  12-section handoff per the Sprint 17 / Sprint 18 precedent.
- `docs/10-handoff.md` — **UPDATED**. Current-phase paragraph
  rewritten to lead with Sprint 19 (Smoke Regression Investigation
  + Orchestrator De-Dup). Sprint 18 G1 demoted to "Preceding
  sprint" paragraph. No earlier-sprint paragraphs were edited.

No file under `server/`, `eval/`, `eval_interactive/`, `data/`,
`scripts/`, `docs/foundational/`, `docs/current/`,
`docs/runtime_freeze_and_risk_policy.md`, `docs/sprints/sprint-001..018-*`,
or `docs/codex-findings.md` is modified.

`docs/sprint_objective.md` is **not** edited by the dev agent
(deliver-agent surface).

`docs/action_bank.md` is **not** edited in this PR; the proposed
deltas in §11 are recorded here for the deliver agent to apply on
close.

## 6. Layer-classification self-walk

Restating the multi-layer prospective stanza from
`docs/sprint_objective.md` against the actual investigation
findings:

| track / case | §3.2 first-match Q | §3.1 layer (primary) | secondary | bundle decision |
| --- | --- | --- | --- | --- |
| Track A — cs_002 | Q1 | `infra` (placeholder loop) | — | defer |
| Track A — cs_011 | Q5 | `semantic_planner` (no escalation on explicit handover request) | `infra` (T2 silence unexplained) | defer |
| Track A — cs_014 | Q1 | `infra` (placeholder loop) | — | defer |
| Track A — cs_038 | Q1 | `infra` (single deadline event → turn-budget mapping mis-escalation) | — | defer |
| Track A — cs_040 | Q1 | `infra` (placeholder loop) | `prompt_projection` (routing demoted topic_subject cue), `skill_state` (UC mis-persisted) | defer |
| Track A — cs_066 | Q4 | `skill_state` (lost `case_id` binding pre-handover) | — | defer |
| Track B — manual-probe | Q5 | `semantic_planner` (LLM re-emitted search_knowledge across 3 steps) | `prompt_projection` (missing `already_called` soft signal) | defer |

§3.3 "no Java guard by default" check: no walk landed on
`java_guard`. No `human_review_required` flag fired (cs_192
stop-and-escalate condition did NOT trigger; the 6 regressed cases
do not show `CONTRACT_VIOLATION:active_use_case`).

The aggregate is consistent with the prospective stanza in
`docs/sprint_objective.md` §"Layer-classification + anti-hardcode
stanza" (Track A spans `infra` / `semantic_planner` /
`skill_state`; Track B resolves to `semantic_planner`). The
stanza's "Track B prospectively `infra`; if LLM-side, re-resolves
to `semantic_planner` and Track B's fix is deferred" clause fires:
Track B re-resolves to `semantic_planner` and the fix is deferred.

## 7. Anti-hardcode self-walk (§4.1, 9 questions)

The Sprint 19 PR is proposal-only — no runtime / prompt / eval /
judge change is introduced. The §4.1 scope-exemption clause for
"pure infra, docs-only, config-governance, and characterization-
test PRs" applies as **docs-only governance** (handoff +
10-handoff lead refresh; no other surface touched). Returning
`approve` with the exemption named.

For thoroughness, the 9 questions answered as if the handoff
were a semantic-change PR:

1. **Keyword / regex / if-else / enum / per-UC matrix added for a
   semantic decision?** No. The handoff describes failure shapes
   and remediation directions; no runtime code is added.
2. **Tier-0 invariant justification?** N/A (Q1 = no).
3. **Soft-signal projection alternative considered?** Yes — §4.2
   "Intended contract" Layer 1 names the `already_called`
   projection diagnostic as the soft-signal remediation for
   Track B. §11 records it as a future R-item (not opened in this
   PR).
4. **Visible-eval case text encoded into runtime / prompt /
   judge?** No. The handoff cites case ids (cs_002 / cs_011 / …)
   in its own structure, but no `cs_NNN` string is written into
   any runtime / prompt / judge surface.
5. **Semantic ownership moved from LLM to Java?** No. The
   `semantic_planner` findings remain owned by the LLM per
   Constitution §1.3. The `infra` findings concern transport /
   timeout handling (Runtime §1.4 surface) — not semantic
   ownership.
6. **Prompt grew an if-else block?** No (no prompt edit).
7. **Tool schema, capability / permission, PII / safety floor,
   grounding floor preserved?** Yes (no change). The
   §5.1 grounding-floor acceptance bar is trivially satisfied —
   no FAQ corpus edit, no projection edit, no judge rubric edit.
8. **Generalization eval coverage (target / neighbor / negative /
   shadow)?** §8 table below; shadow split explicitly deferred to
   G2 per `iteration_governance.md` §5.1's shadow allowance.
9. **Temporary change → sunset plan?** N/A (no change).

Sprint 19 PR-level verdict (per §4.1): **`approve` — docs-only
governance exemption; no semantic change introduced.**

## 8. Generalization-coverage table

| case-family | target (cases) | neighbor (cases) | negative-control (≥2 per bundled fix) | shadow |
| --- | --- | --- | --- | --- |
| Track A — smoke regression | cs_002, cs_011, cs_014, cs_038, cs_040, cs_066 (the 6 regressed) | cs_001, cs_015, cs_029, cs_036, cs_095, cs_176, cs_192, cs_259 (the 8 non-regressed smoke cases — must not regress in any future bundled fix) | N/A — Sprint 19 bundles **no** fixes; the negative-control naming requirement applies per bundled fix only | deferred to G2 (no shadow split exists yet; this gap is explicit per `iteration_governance.md` §5.1 shadow allowance for sprints pre-dating the split) |
| Track B — orchestrator de-dup | manual-probe 2026-05-13 (session `4a2f3680-02a8-4d13-8f0b-f99d7c249b57`) | None named — manual-probe shapes are n=1; G2 case-family construction would synthesise neighbors for both the read-side (search_knowledge ×3) and write-side (Sprint 16 handover-exactly-once) shapes | N/A — Sprint 19 bundles no fix; future read-side de-dup proposal would name ≥2 negatives (e.g. legitimately-different-args search calls; reroute-driven new-UC search; LLM-led ambiguity-clarification re-call) | deferred to G2 |

Sources: target = `eval_interactive/results/20260510-134558/results.json`
(per-case fail tags + stop reasons) + manual-probe brief.
Neighbor = `eval_interactive/results/20260505-235231/results.json`
∩ `eval_interactive/results/20260510-134558/results.json` non-
regressed set.

## 9. Sprint-objective-met check

Per `docs/sprint_objective.md` §"Success metrics" + §"Do not
implement":

- **Target cases addressed.** ✓ §3.2 walks completed for all 6
  regressed cases (Track A §3.1–3.6) and for the manual-probe
  trace (Track B §4). All seven resolve to a defer decision per
  the bundle-or-defer policy. **No regression test was added
  this sprint** because no fix was bundled.
- **Neighbor cases no regression.** ✓ Trivially satisfied —
  Sprint 19 is proposal-only, no smoke rerun was triggered, the
  8 neighbor cases are unchanged from the 2026-05-10 FAIL run
  baseline.
- **Negative-control no false positive.** ✓ Trivially satisfied
  per the bundled-fix-conditional clause.
- **Shadow no regression.** ✓ Trivially satisfied — Sprint 19
  pre-dates the shadow split per
  `iteration_governance.md` §5.1's shadow allowance. The
  generalization-coverage table (§8) names "deferred to G2"
  explicitly.
- **Safety floor unchanged.** ✓ Trivially satisfied — no PII /
  safety / identity-verification / imminent-harm surface was
  edited; no Tier-0 invariant was added per `runtime_freeze_and_risk_policy.md`.
- **Grounding floor unchanged.** ✓ Trivially satisfied — no FAQ
  corpus / grounding contract / projection edit.
- **Wrong-containment rate unchanged or down.** ✓ Trivially
  satisfied (no agent behaviour change).
- **Over-escalation rate unchanged or down.** ✓ Trivially
  satisfied (no agent behaviour change).
- **Architecture-health metrics not regressed.** ✓ Trivially
  satisfied — `iteration_governance.md` §6 metrics remain
  `collection_status: not_started`; Sprint 19 does not open
  collection.

Walking the **"Do not implement"** bullets:

- No edit under `eval/`, `eval_interactive/case_specs/`,
  `eval_interactive/case_spec_overrides.yaml`, or
  `eval_interactive/personas*.yaml`. ✓
- No FAQ corpus edit. ✓
- No prompt edit beyond pure projection. ✓ (no prompt edit at all)
- No judge rubric edit. ✓
- No keyword / regex / if-else / enum / per-UC matrix added for a
  semantic decision. ✓
- No new Tier-0 invariant in
  `docs/runtime_freeze_and_risk_policy.md`. ✓
- No work on the other 16 R-items in `docs/action_bank.md` §5.2. ✓
- No G2 case-family construction. ✓
- No edit to `docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`,
  `docs/foundational/*`, or `docs/sprints/*` (except the new
  Sprint 19 handoff itself). ✓
- No architecture-health metric collection opened. ✓
- No change to `docs/codex-findings.md`. ✓

## 10. Open questions for human

1. **Track A bundle scope.** The user brief §4 restricts Track A
   `server/**` edits to `prompt_projection` soft-signal additions.
   The sprint objective §"Bundle-or-defer policy" is broader and
   would in principle allow an `infra` bundle that coalesces
   consecutive `DEADLINE_EXCEEDED` placeholder emissions (the
   dominant shape behind cs_002 / cs_014 / cs_038 / cs_040). The
   investigation-class sprint posture argues for the narrower
   read (proposal-only). Open question: should a follow-up infra
   sprint take the placeholder-coalescing fix, or should the
   placeholder branch be downgraded entirely (e.g. emit a typing-
   indicator side-channel signal, not a transcript-side
   `"Sorry, I'm a bit slow"` user-facing message)?
2. **cs_192 neighbor shape shift.** cs_192 is in the 8 non-
   regressed neighbor set (it failed in both runs) but its FAIL-
   shape changed substantially: 2026-05-05 was
   `L2_GATE:correct_outcome` + `L3:relevance` + `L3:tone_appropriateness`;
   2026-05-10 is `CONTRACT_VIOLATION:active_use_case`,
   `total_turns=0`. The `CONTRACT_VIOLATION:active_use_case` shape
   is exactly the Sprint 8 §K0 re-open territory the cs_192
   stop-and-escalate clause guards against. **The 6 regressed
   cases do not exhibit this shape**, so the stop-and-escalate
   clause does not fire for Track A — but cs_192 itself does, in
   a different lane. Open question: is cs_192's shape shift
   itself an R-item, or is it expected behaviour of a
   classification-failure path that became more visible after
   the Sprint 8.1 retry-budget refactor? Recommended treatment:
   record as a neighbor observation, **do not** open a new
   Tier-0, but surface for human review.
3. **Track B brief-vs-code contradiction.** The manual-probe
   brief's "1 LLM request → 3 executions" framing reads naturally
   as orchestrator amplification, which the code does not support.
   The most parsimonious resolution is the multi-step reading in
   §4.1 — the brief author saw the *last* LLM raw response while
   observing three ToolEvents. **A clean repro with full LLM-call
   tracing (all step-N LlmCallEvents inspected, not just
   `lastLlmRawResponse`) would confirm.** Open question: should
   a Sprint-19+ investigation re-run the manual probe with
   per-step LlmCallEvent logging enabled and verify? This sits
   between Track B and the per-case JSON trace gap in the
   results directories (see Doc-status warning #1).
4. **Sprint 18 narrative correction.** Sprint 18 G1's claim that
   Sprints 14 / 14.1 / 15 / 16 made "no runtime semantic change"
   is contradicted by the git log (Doc-status warning #2 above).
   The actual delta in the smoke gap window is 18 code-touching
   commits across runtime / config / FAQ-evidence / handover.
   The Sprint 18 handoff is an archive and is not edited; the
   correction belongs in a `docs/current/` reconciliation note
   per `doc_governance.md` §"Code ahead of docs" once the human
   confirms the narrative gap is worth a separate note (or
   whether it folds back into a later
   `iteration_governance.md`-style summary).
5. **`R-runtime-orchestrator-tool-call-deduplication` next step.**
   The Sprint 19 deferred-fix proposal (§4.2 + §11) splits the
   contract into a read-side soft-signal layer
   (`prompt_projection` `already_called` slot) and an optional
   read-side dispatcher short-circuit (`infra`). The write-side
   exactly-once contract is already owned by the Sprint 16
   `HandoverOrchestrator` design freeze. Open question: should
   Sprint 19+ open the read-side soft-signal track as a *separate*
   sprint from the write-side handover-orchestrator track, or
   should they merge into a single "exactly-once owner" sprint?
   Recommended: keep them separate so the read-side can land in
   a small `prompt_projection` sprint without waiting on the
   handover-orchestrator commitment.
6. **Per-case JSON trace gap.** The results directories no longer
   contain per-case trace JSON files — only `results.json` +
   `report.html`. This blocks the kind of step-by-step
   reconstruction Sprint 19 and G2 both need (Track A §3.2 walks
   would benefit from tool_call / phase_plan / projection
   evidence; Track B needs per-step LlmCallEvent inspection).
   Open question: re-enable per-case trace dumping in the smoke
   harness for G2-era runs? This is a small `eval_interactive/`
   change and would belong in an eval-harness sprint, not in
   Sprint 19.

## 11. Action-bank deltas (proposed; deliver agent applies on close)

The dev agent does NOT edit `docs/action_bank.md` in this PR;
the deltas below are the recommended changes for the deliver
agent to apply at sprint close.

**Updated rows:**

- `R-smoke-regression-investigation` — change disposition from
  `P1 must resolve before G2 — investigation pending` to
  `P1 investigation complete (Sprint 19) — defer remediation to
  follow-on track sprints; root cause is multi-shape (slow-LLM
  placeholder loop + UC-K state-loss on intake-complete);
  remediation proposals named below`.
- `R-runtime-orchestrator-tool-call-deduplication` — change
  disposition from `infra-layer per §3.2 Q1; investigation
  pending` to `layer reclassified to semantic_planner per
  Sprint 19 §4.3; defer remediation to follow-on
  prompt_projection sprint (read-side soft signal) and to the
  Sprint 16 HandoverOrchestrator design for the write-side; see
  Sprint 19 §4.2 for the de-dup contract write-up`.

**New R-items proposed (recommended; deliver agent decides
whether to open):**

- `R-slow-llm-placeholder-coalesce` (`infra`). The five-of-six
  regressed-case shape behind Track A: two consecutive
  `LlmDeadlineExceededException` paths emit the same user-facing
  `"Sorry, I'm a bit slow right now. Please try sending that
  again in a moment."` message, which the loop / stall detector
  then flags as a loop. Recommended remediation:
  - (a) coalesce consecutive deadline events within a session
    into a single user-facing beat (or move the placeholder to a
    side-channel typing-indicator path that does not consume a
    transcript turn); and / or
  - (b) escalate honestly on the *second* consecutive deadline
    rather than re-emitting the same placeholder.
  Scope: `PhaseEvaluator.java` line 754–770 +
  `AgentRunResult.deadlineExceeded` propagation. Source: Sprint 19
  §3.7. **No keyword / regex / if-else / enum / per-UC matrix**
  is introduced.
- `R-uc-k-intake-complete-case-id-binding` (`skill_state`). The
  cs_066 shape — UC-K intake completes per the Sprint 7 §I2
  intake-complete guard, the bot escalates with
  `intake_complete_for_uc_k`, but the assembled handover payload
  carries a null `case_id` because the auto-create-case step
  before escalation no longer reliably runs. Recommended scope:
  audit `CreateCaseControlledTool` call-site coverage across
  intake UCs (UC-G / H / I / J / K) and the post-intake-complete
  escalation path in `ControlKernel.java` line 1490–1595. Source:
  Sprint 19 §3.6.
- `R-prompt-projection-already-called-soft-signal`
  (`prompt_projection`). The Track B remediation Layer 1
  (§4.2). Add an `already_called: [{tool, arguments_hash,
  at_step}]` diagnostic slot to the per-step projection so the
  LLM can see "you already called this tool with these args"
  before re-emitting. Soft signal only — the LLM owns whether to
  re-emit. Scope: `ContextProjectionBuilder.java` projection
  assembly + a JSON-schema documentation update.
- `R-idempotent-read-tool-short-circuit` (`infra`, optional /
  conditional on the soft-signal-only approach proving
  insufficient). The Track B remediation Layer 2 (§4.2). For
  side-effect-free tools (`search_knowledge`, `lookup_*`,
  `get_*_context`), short-circuit identical-args re-emission
  within the same `AgentRunLoop.run(...)` by returning the
  cached prior result. Write tools opt out; they are owned by
  the Sprint 16 `HandoverOrchestrator` design freeze.
- `R-per-case-trace-dump-for-smoke-harness` (`infra` / eval
  harness). Doc-status warning #1 + open question 6. Re-enable
  per-case JSON trace dumping (tool_calls, phase_plan,
  projection, LlmCallEvents) under each
  `eval_interactive/results/<run-id>/` directory so future Track
  A § 3.2 walks and Track B per-step investigations have first-
  party evidence. Scope: `eval_interactive/eval_interactive/`
  Python package (smoke runner).
- `R-sprint-narrative-vs-git-log-reconciliation` (governance /
  docs-only). Doc-status warning #2 + open question 4. The
  Sprint 18 G1 narrative that Sprints 14 / 14.1 / 15 / 16 made
  "no runtime semantic change" is contradicted by the git log
  (40 commits in the smoke gap window, 18 touching code). A
  reconciliation note in `docs/current/` or a fold-back into a
  `iteration_governance.md`-adjacent governance doc should land
  on the doc_governance.md §"Code ahead of docs" cadence. Scope:
  docs-only.

**Out-of-scope deferrals (Sprint 19 did NOT investigate, listed
for completeness):**

- The remaining 16 R-items in `docs/action_bank.md` §5.2 (Wave
  A5 / A6 L3 review batch, FAQ corpus audit per UC,
  duplicated-greeting projection fix, faqMissCount threshold,
  L3 judge form-context trust rubric, etc.) — explicit per
  `docs/sprint_objective.md` §"Do not implement".
- G2 case-family construction (target / neighbor / negative /
  shadow split for the 10 G1 briefs) — Sprint 20 territory per
  `docs/sprint_objective.md` notes.

## 12. Next recommended action

**Recommended next sprint: Sprint 20 — G2 Interactive Case
Family + Shadow Split.** Sprint 19 closes the two G2 prerequisites
the Sprint 18 G1 backlog named (smoke-regression diagnosis +
orchestrator de-dup contract write-up). G2's case-family
construction can now proceed with explicit guidance about which
2026-05-10 fail tags to *treat as out-of-scope baseline noise*
(the slow-LLM placeholder shape behind cs_002 / cs_011 / cs_014 /
cs_038 / cs_040 + the cs_066 case_id-binding loss) vs which to
*model as a target / neighbor / negative case family* (the
Cluster A persistent failures from G1, with their G1 briefs as
the primary evidence). The Sprint 18 G1 brief catalogue (§4 in
that handoff) names the 10 briefs that anchor G2; the Sprint 19
§3 walks + §4 walk add the regression-shape annotations that
distinguish "real semantic failure" from "infra-shape noise" for
the smoke set.

A parallel, smaller sprint can pick up the read-side
`R-prompt-projection-already-called-soft-signal` proposal from
§11 — it does not depend on G2 case-family construction and is
the lowest-risk Track B follow-on.

The Wave A5 / A6 L3 review batch and the placeholder-coalesce
infra sprint remain disjoint and can be parallel-tracked behind
G2 without contention.
