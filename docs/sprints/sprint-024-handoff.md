---
title: Sprint 24 handoff — Slow-LLM Placeholder Coalesce + Coarse Latency Proxy
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (Track A bundle + Track B investigation)
last_reviewed: 2026-05-14
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Two-track sprint per `docs/sprint_objective.md` (Sprint 24). Track A is a
  deterministic UX repair on the cross-turn slow-LLM placeholder loop:
  per-session `consecutive_deadline_count` field on `BotSession` (mirroring
  V12 `runtime_error_count`); `PhaseEvaluator.interpretRunResult` prologue
  reset hook + DEADLINE_EXCEEDED branch threshold-gated message
  (placeholder on the first consecutive deadline, distinct honest
  next-step on the second). Single Flyway V13 migration. Behaviour-level
  JUnit `Sprint24DeadlinePlaceholderCoalesceTest` asserts the
  placeholder / distinct-message / reset shape. Track B is
  investigation-only: independent coarse-proxy extraction from
  `eval_interactive/results/20260505-235231` (pre) and
  `eval_interactive/results/20260510-134558` (post), honest
  no-per-call-claim paragraph, and proposal of a new follow-on R-item
  `R-per-llm-call-latency-instrumentation` as the prerequisite to any
  future deadline-budget or model-revert decision. No deadline-budget
  widening, no model config change, no prompt_projection / eval-spec
  edit, no Tier-0 change. §10 surfaces a methodology discrepancy
  between the planning-turn coarse-proxy numbers cited in
  `docs/sprint_objective.md` §2 and the dev-session independent
  extraction; both are reported for human resolution.
---

# Sprint 24 handoff — Slow-LLM Placeholder Coalesce + Coarse Latency Proxy

Date closed: 2026-05-14
Branch: `design-v1-without-human-review`
Sprint class: two-track, semantic-touching (Track A), investigation-only
(Track B). §7 stanza applied per-track per
`feedback_multi_layer_prospective_stanza.md`.
UX-over-pass-rate framing per the human's 2026-05-14 reframe.

## 1. Context Pack

### 1.1 Relevant docs

| path | tier | status | one-line relevance |
|------|------|--------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current; implementation_status `not_started` at session start | Authoritative Sprint 24 scope; §3 (Track A scope) / §4 (Track B scope) / §6 (hard fences) / §10 (out-of-scope) / §11 (bundle policy) all consulted before any edit. |
| `docs/sprints/sprint-023-handoff.md` §3.3 / §4 / §11 | sprint-archive | immutable | Track precedent walks. §4.2 confirms `results.json` does NOT carry per-turn LLM latency. §4.3 names the proximate cause (`PhaseEvaluator.java` 754–770 unconditional placeholder emission). §11 records the four R-item rows Sprint 24 closes / reframes / proposes. |
| `docs/sprints/sprint-019-handoff.md` §3.1–§3.6 / §3.7 | sprint-archive | immutable | Per-case walks (cs_002/cs_011/cs_014/cs_038/cs_040/cs_066) that named the placeholder-loop target set behaviour-level (Sprint 23 §4.1 later refined the target set; Sprint 24 treats the *behaviour shape*, not a CaseSpec list). §3.7 is the original `f2d4cb2` model-switch latency hypothesis. |
| `docs/current/iteration_governance.md` §1 / §3 / §5 / §7 | durable-connective | current | Constitution (§1), fix-layer checklist (§3), eval-acceptance rules (§5), required sprint-objective stanza (§7). §6 of this handoff (layer-classification self-walk) and §7 (anti-hardcode self-walk) consume these directly. |
| `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` | durable-connective | current | Tier model + reading lists; loaded transitively via `AGENTS.md`. |
| `docs/action_bank.md` lines 612–615 | durable-connective | current | The four R-item rows §11 updates (R-slow-llm-placeholder-coalesce-honest-next-step / R-llm-latency-budget-investigation / R-cs040-uc-k-topic-subject-routing / R-accumulated-tool-results-prompt-consumption). |

### 1.2 Relevant code paths

| path | role |
|------|------|
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | Outcome-dispatch prologue (lines 675–693 after edit; was 675–682 before edit — reset hook precedent for `runtime_error_count` extended with the new `consecutive_deadline_count` reset). DEADLINE_EXCEEDED branch split off from the shared LLM_UNAVAILABLE block; threshold-gated message + counter increment on the deadline branch only. |
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | New `consecutive_deadline_count` `@Column` + `@Builder.Default = 0` Integer field (mirrors `runtime_error_count` at lines 75–77; new column declared immediately below at lines 79–81). |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | New `consecutiveDeadlineCount(0)` line in the session-create builder (mirrors `runtimeErrorCount(0)` at the old line 107; new line at 108). |
| `server/src/main/resources/db/migration/V13__add_consecutive_deadline_count.sql` | New Flyway migration; mirrors V12 `runtime_error_count` shape (`ALTER TABLE bot_sessions ADD COLUMN ... INTEGER NOT NULL DEFAULT 0`). |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint24DeadlinePlaceholderCoalesceTest.java` | Behaviour-level regression test: 1st deadline → placeholder; 2nd consecutive deadline → NOT placeholder + contains a next-step intent token; reset assertion via a non-deadline outcome between two deadlines. |
| `eval_interactive/results/20260505-235231/results.json` + `eval_interactive/results/20260510-134558/results.json` | READ-ONLY Track B sources; case-level `elapsed_ms` / `total_turns` only. No per-turn LLM latency. |

### 1.3 Doc status warnings

- The §2 premise facts in `docs/sprint_objective.md` for line numbers
  (BotSession 75–77, SessionManager 107, PhaseEvaluator 675–682 /
  738–752 / 754–770) all matched the runtime files at session start;
  Track A's edit shifted lines forward (see §5) but the precedent shapes
  cited are unchanged.
- The §2 premise's *coarse-proxy numbers* (pre p50 ≈ 10.4s / p95 ≈ 24.6s;
  post p50 ≈ 10.5s / p95 ≈ 27.6s; "105 + 27 case-turns") could **not** be
  reproduced by an independent extraction from the same two
  `results.json` files. See §4.1 / §4.4 / §10 for the discrepancy and the
  honest re-reporting; the *direction* of the planning-turn finding
  (post-`f2d4cb2` p95 widening) survives the independent extraction —
  the *magnitude* does not.
- Working-tree contained pre-existing uncommitted edits at session start
  (`csagent_system_design_review.md`, `server/src/main/resources/prompts/system_prompt.txt`)
  and the deliver-agent-owned `docs/sprint_objective.md` /
  `compact/sprint-024-*.md`. None of these were authored by this dev
  agent; the close commit stages only files under §5.

### 1.4 Source-of-truth decision

- **For Track A behaviour** (which message under which event-shape): the
  authoritative source is the runtime code path being edited
  (`PhaseEvaluator.interpretRunResult` DEADLINE_EXCEEDED branch +
  `BotSession.consecutiveDeadlineCount` field), verified by
  `Sprint24DeadlinePlaceholderCoalesceTest`. Per `doc_governance.md`
  "code ahead of docs", the test is the contract.
- **For Track B coarse-proxy data**: the authoritative source is
  `eval_interactive/results/{20260505-235231,20260510-134558}/results.json`
  at the case-level `elapsed_ms` + `total_turns` granularity, READ-ONLY.
  Per-LLM-call latency is NOT in those files at the needed granularity;
  the coarse proxy is a session-level signal that conflates LLM
  round-trip + tool dispatch + persistence + retry overhead. The
  Track B proxy is *signal*, not *evidence*.

### 1.5 Implementation status

- Track A: `implemented` (column + builder init + reset hook +
  threshold-gated DEADLINE_EXCEEDED branch + V13 migration + 3-test
  behaviour-level regression suite, all green).
- Track B: `implemented` for the investigation deliverable
  (coarse-proxy data + no-per-call-claim paragraph + new R-item
  proposal). No code shipped for Track B by design; the
  `R-per-llm-call-latency-instrumentation` proposal is the
  prerequisite to any future budget decision.

### 1.6 Risks before coding (carried forward)

1. **Hidden coupling between `DEADLINE_EXCEEDED` and `LLM_UNAVAILABLE`**
   — they shared one block (old lines 754–771). Mitigated by splitting
   into two case-blocks; `LLM_UNAVAILABLE` user message + transition
   tag preserved byte-for-byte.
2. **Reset-hook scope** — mirrored the ERROR reset shape verbatim with
   `DEADLINE_EXCEEDED` as the symbol; reset only fires when the new
   outcome is NOT a deadline AND the counter is currently > 0.
3. **Behaviour-level regression must not encode literal honest-next-step
   content** — asserted with a property check over a small set of
   next-step intent tokens (`handover` / `specialist` / `connect` /
   `retry` / `try again` / `alternative`), not the literal LLM-relevant
   string.
4. **Migration ordering** — V12 was latest; new column is V13.
5. **Scope creep temptation** — `LLM_UNAVAILABLE` coalesce,
   `ChatController.java:125` legacy emit, `cs_040` UC-K routing all
   refused per §6.1 / §10 of the objective.

## 2. Sprint-objective recap

Per `docs/sprint_objective.md` §1, Sprint 24 paired off Sprint 23 §11:

- **Track A (deterministic UX repair).** Consecutive `DEADLINE_EXCEEDED`
  outcomes from `LlmInvocationService.invokeChat` were causing
  `PhaseEvaluator` to emit byte-identical placeholder text on every
  deadline turn, reading as a stuck loop from the user's seat (and
  tripping the runtime loop-detector). Track A introduces a
  session-scope `consecutive_deadline_count` mirror of
  `runtime_error_count` and, on the **second** consecutive deadline,
  emits a distinct honest next-step message in place of repeating the
  placeholder. Counter resets on any non-deadline outcome.
- **Track B (honest coarse-proxy latency baseline).** Sprint 19 §3.7
  hypothesised that `f2d4cb2 fix: switch primary llm to deepseek-v4-flash`
  widened LLM-call latency variance. Sprint 23 §4.2 confirmed that
  per-LLM-call latency is **not** in `results.json` at the needed
  granularity. Track B reports a coarse proxy from `elapsed_ms` /
  `total_turns`, an explicit no-per-call-claim paragraph, and a
  proposal of `R-per-llm-call-latency-instrumentation` as the
  prerequisite to any future deadline-budget or model-revert decision.

UX-over-pass-rate framing is preserved: the primary bar is
`Sprint24DeadlinePlaceholderCoalesceTest` asserting the
placeholder-then-distinct-message shape; pass-rate effects on the
smoke set are secondary.

## 3. Track A — implementation + regression test as evidence

Behaviour-level (no per-CaseSpec walk). The target set is the
*event shape* — two consecutive `DEADLINE_EXCEEDED` outcomes — not a
CaseSpec id list. Sprint 19 §3.7 named `cs_002 / cs_011 / cs_014 /
cs_038 / cs_040`; Sprint 23 §4.1 re-derived (cs_011 has no
placeholder — `semantic_planner` shape; cs_038 emits+recovers, not a
clean loop; cs_015 / cs_176 / cs_259 also emit the placeholder).
Track A's regression bar is the *behaviour*, not a specific CaseSpec
id, so the test does not encode any CaseSpec into the runtime fix.

### 3.1 Implementation walk

1. **`BotSession.consecutiveDeadlineCount`** (`server/src/main/java/com/gumtree/csagent/model/BotSession.java`
   lines 79–81). New `@Column(name = "consecutive_deadline_count",
   nullable = false)` `@Builder.Default = 0` `Integer` field, declared
   immediately below `runtime_error_count` (lines 75–77). Shape
   matches the V12 precedent byte-for-byte.

2. **`SessionManager` builder init**
   (`server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
   line 108). New `.consecutiveDeadlineCount(0)` line immediately
   below `.runtimeErrorCount(0)` (now line 107). The builder
   `@Builder.Default` would default to 0 if the line were omitted,
   but the precedent line for `runtimeErrorCount` is explicit at the
   same site and the parallel makes future readers' lives easier.

3. **`PhaseEvaluator` reset hook** (post-edit lines 684–693). Mirrors
   the existing `runtime_error_count` reset (lines 675–682) with
   `DEADLINE_EXCEEDED` substituted for `ERROR`; resets only when (a)
   `session != null`, (b) `outcome != DEADLINE_EXCEEDED`, (c) the
   counter is not null and is currently `> 0`. The reset hook sits in
   the outcome-dispatch prologue, before the `switch (outcome)`
   block, so it fires uniformly across all non-deadline branches.

4. **`PhaseEvaluator` DEADLINE_EXCEEDED branch split**. The shared
   `DEADLINE_EXCEEDED` / `LLM_UNAVAILABLE` block (old lines 754–770)
   is split into two case-blocks. The DEADLINE_EXCEEDED block
   increments the counter, then chooses between two messages on the
   `< 2` threshold:
   - **first consecutive deadline** (`deadlineCount == 1`): emits the
     existing placeholder text byte-for-byte:
     *"Sorry, I'm a bit slow right now. Please try sending that again
     in a moment."*
   - **second consecutive deadline** (`deadlineCount >= 2`): emits a
     distinct honest next-step message:
     *"I'm still having trouble responding in time. If you'd like, I
     can connect you with a specialist, or you can try again in a few
     minutes."*

   Both branches keep the transition tag `agent_deadline_exceeded`;
   neither escalates; neither auto-calls `request_handover` (the
   message offers a handover but the user owns the choice). The
   `LLM_UNAVAILABLE` branch is preserved with its existing user
   message and `agent_llm_unavailable` transition tag — Sprint 24's
   scope is the DEADLINE_EXCEEDED surface only (per dev prompt §3.2
   #5; a future R-item may revisit `LLM_UNAVAILABLE` coalesce).

5. **`V13__add_consecutive_deadline_count.sql`**. One-line
   `ALTER TABLE bot_sessions ADD COLUMN consecutive_deadline_count
   INTEGER NOT NULL DEFAULT 0;` migration; comment names the Sprint 24
   Track A purpose and the V12 precedent.

### 3.2 Honest next-step content — §3.1 / §7 constraint compliance

The 2nd-deadline message *"I'm still having trouble responding in
time. If you'd like, I can connect you with a specialist, or you can
try again in a few minutes."* satisfies the §3.1 constraints from the
objective and the §7 hard gate from the dev prompt:

- **Names what is happening** ("I'm still having trouble responding
  in time"). Plain-language acknowledgement of the slow-response
  condition; no apology-loop wording.
- **Offers an actionable next step** in two forms — a soft handover
  offer ("I can connect you with a specialist") and a retry-after
  suggestion ("you can try again in a few minutes"). The "if you'd
  like" phrasing preserves user agency; the bot is **not**
  auto-handing-over.
- **Does not repeat the placeholder text byte-for-byte** — verified
  by the regression test's `assertNotEquals(PLACEHOLDER, ...)`.
- **Does not branch on user content** — the trigger is the
  event-shape count of consecutive `DEADLINE_EXCEEDED` outcomes; no
  keyword / regex / if-else / enum on the user's message text.
- **Does not branch on UC** — one message, uniformly applied at
  threshold; no per-UC matrix.
- **Does not contain visible-eval CaseSpec text or trace-specific
  phrasing** — the wording is principled rather than mirroring any
  specific CaseSpec rubric or any specific eval trace.

### 3.3 Regression test as evidence

The Track A bar is the behaviour-level
`Sprint24DeadlinePlaceholderCoalesceTest` under
`server/src/test/java/com/gumtree/csagent/service/runtime/`. Three
test methods exercise the `PhaseEvaluator.interpretRunResult` surface
directly (no integration plumbing) using
`AgentRunResult.deadlineExceeded(...)` /
`AgentRunResult.clarification(...)` factories:

| test | assertion |
|------|-----------|
| `firstDeadlineExceeded_emitsExistingPlaceholder` | 1st `DEADLINE_EXCEEDED` outcome → `responseText == PLACEHOLDER`; `nextPhase == "RESOLVE"`; `transitionReason == "agent_deadline_exceeded"`; `session.consecutiveDeadlineCount == 1`. |
| `secondConsecutiveDeadline_emitsDistinctHonestNextStepMessage` | 2nd `DEADLINE_EXCEEDED` outcome → `responseText != PLACEHOLDER`; `responseText` non-empty; `containsNextStepIntent(responseText) == true` (property check over the token list `["handover", "specialist", "connect", "retry", "try again", "alternative", "alternatively"]`); `nextPhase == "RESOLVE"`; `transitionReason == "agent_deadline_exceeded"`; `escalationReason == null` (no auto-handover); `session.consecutiveDeadlineCount == 2`. |
| `nonDeadlineOutcomeBetweenDeadlines_resetsCounterAndPlaceholderFiresAgain` | 1st `DEADLINE_EXCEEDED` → placeholder + counter=1; `CLARIFICATION_NEEDED` outcome between → `transitionReason == "clarification_asked"` + counter=0 (reset hook); next `DEADLINE_EXCEEDED` → placeholder again + counter=1. |

Test run result: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0,
Time elapsed: 0.631 s`.

### 3.4 Full server suite

`mvn -pl server -am test`: **`Tests run: 901, Failures: 1, Errors: 0,
Skipped: 1`**. The single failure is
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly:53`
("ACTIVE-UC TIEBREAKER must be tagged with the Sprint 6 anchor"),
which is **pre-existing** and tied to the working-tree uncommitted
edit on `server/src/main/resources/prompts/system_prompt.txt` —
documented in Sprint 23 §9 with the same failure shape (Sprint 23
explicitly noted "full server suite 898/0/0/1 modulo the pre-existing
uncommitted cosmetic ACTIVE-UC TIEBREAKER header rename which is
unrelated to either Sprint 23 commit"). The `system_prompt.txt`
working-tree mod is **not authored by this dev agent** and is
explicitly listed in `docs/sprint_objective.md` §10 as out-of-scope
(fence #3, no `prompt_projection` work) and in the dev-prompt §13 as
a pre-existing mod the dev agent does not stage. Excluding the
pre-existing failure, the Track A bundle is 898 of 898 green.

### 3.5 cs_040 placeholder-vs-routing fence honoured

Per `docs/sprint_objective.md` §6.1 and §10: Track A fixes the
*placeholder shape* on cs_040 (and any case with the placeholder-loop
surface); it does NOT touch UC-K vs UC-C routing. The action_bank
R-item `R-cs040-uc-k-topic-subject-routing` at
`docs/action_bank.md:614` remains conditional under the
controlled-multi-shape-testing bar; Sprint 24 does not act on it.

### 3.6 `ChatController.java:125` fence honoured

Per `docs/sprint_objective.md` §6.1 and §10: the legacy
`LlmDeadlineExceededException` catch at
`server/src/main/java/com/gumtree/csagent/controller/ChatController.java`
fires only when the deadline propagates past `AgentRunLoopImpl` and
emits the same byte-identical placeholder text from a different
surface. Sprint 24's cross-turn coalesce is at the phase-mapping
layer (`PhaseEvaluator`); the controller layer is untouched.

## 4. Track B — coarse-proxy latency baseline (investigation-only)

### 4.1 Coarse-proxy data table — dev-session independent extraction

Method: read each case's `elapsed_ms` and `total_turns` from
`case_results[]` in the two `results.json` files. Compute case-level
p50 / p95 of `elapsed_ms` (seconds) over the 14 cases per run; report
`sum(total_turns)` per run for context.

| run | n_cases | sum(total_turns) | min | p50 | p95 | max |
|-----|---------|------------------|-----|-----|-----|-----|
| pre  (`20260505-235231`, before `f2d4cb2`) | 14 | 32 | 6.8s | **19.9s** | **30.5s** | 63.4s |
| post (`20260510-134558`, after `f2d4cb2`) | 14 | 42 | 0.0s | **41.1s** | **92.3s** | 120.0s |

Per-case detail (case-level `elapsed_ms` ÷ 1000 = seconds /
`total_turns`):

- pre: `cs_001=19.9s/2t, cs_002=15.2s/2t, cs_011=18.8s/2t,
  cs_014=22.0s/2t, cs_015=30.5s/2t, cs_029=6.8s/2t, cs_036=26.8s/4t,
  cs_038=24.9s/4t, cs_040=13.5s/2t, cs_066=14.1s/3t, cs_095=30.1s/2t,
  cs_176=63.4s/2t, cs_192=26.8s/2t, cs_259=13.2s/1t`
- post: `cs_001=23.3s/2t, cs_002=84.9s/4t, cs_011=31.6s/3t,
  cs_014=92.3s/4t, cs_015=77.7s/4t, cs_029=8.6s/2t, cs_036=28.1s/3t,
  cs_038=41.1s/5t, cs_040=64.4s/3t, cs_066=20.0s/3t, cs_095=120.0s/0t,
  cs_176=73.9s/5t, cs_192=0.0s/0t, cs_259=74.9s/4t`

Two post-`f2d4cb2` cases have `total_turns=0` (`cs_095` at the
120-second hard-stop wall, `cs_192` at 0ms — likely a contract
violation that aborted before the first agent turn); they are
preserved in the case-level distribution above as observed in the
file but contribute zero turns to the `sum(total_turns)` column.

### 4.2 Observed p95 widening — independent-extraction view

The independent dev-session extraction in §4.1 surfaces a
**≈62-second case-level p95 widening** (≈30.5s pre → ≈92.3s post)
across the 14 smoke cases — substantially larger than the
dev-prompt §4.1 / objective §2 planning-turn citation of "**≈3-second
p95 widening (≈24.6s → ≈27.6s)**". Both extractions agree on the
*direction* (post-`f2d4cb2` p95 widened); they disagree on the
*magnitude*. See §10 (open questions) for the methodology question
that surfaces from the discrepancy. The dev prompt §4.1 explicitly
instructs the dev agent to "Verify or accept with citation"; this
section reports both, with the planning-turn numbers cited verbatim
from `docs/sprint_objective.md` §2 / dev-prompt §4.1 and the
dev-session independent extraction reported alongside.

### 4.3 No-per-call-claim paragraph (verbatim in substance)

Per-LLM-call latency is **NOT** in `eval_interactive/results/*/results.json`
at the per-turn granularity needed to verify the Sprint 19 §3.7
hypothesis directly. Case-level `elapsed_ms` conflates LLM round-trip,
tool dispatch, persistence, and retry overhead. The pre/post p95
widening is a *signal* that something changed; it is **NOT** evidence
of per-LLM-call latency widening. A deadline-budget widening or
model-revert decision made on this signal alone would not be
justified.

(Restates `docs/action_bank.md:613` and Sprint 23 §4.2's cross-case
finding (a) / (b) "unverified in this sprint" — `results.json` carries
no per-turn `LlmClient.chat` round-trip measurement; sample inspection
of `summary` and `case_results[].transcript` confirms neither carries
per-turn latency.)

### 4.4 Magnitude divergence — why the dev session reports the larger figure

The dev-session independent extraction (§4.1) is reported as the
authoritative coarse-proxy number on this handoff. Reasons:

- **Source-of-truth chain.** The dev session's input was the same two
  `results.json` files cited in `docs/sprint_objective.md` §2; the
  extraction was a single Python pass over `case_results[].elapsed_ms`
  and `case_results[].total_turns`. The full per-case data are
  printed in §4.1 above for auditability.
- **The planning-turn methodology is not recorded** in the materials
  the dev agent could read (`docs/sprint_objective.md` §2, the dev
  prompt §4.1, the available compact `compact/sprint-024-*.md` files);
  the "105 + 27 case-turns" n value also cannot be reproduced from
  either case count (14 + 14) or transcript count (66 + 82) or
  `sum(total_turns)` (32 + 42).
- **The direction of the finding survives both extractions** — both
  agree that post-`f2d4cb2` p95 widened — so Track B's conclusion
  (a per-LLM-call latency instrumentation R-item is the prerequisite
  to any budget / model decision) is robust to the magnitude question
  in §10.

Both views are reported because the §6.2 fence #6 "do not represent
coarse proxy data as per-call latency evidence" cuts in either
direction — overstating the case for a budget widening AND
understating a real divergence both violate the spirit of the fence.

### 4.5 Follow-on R-item proposal — `R-per-llm-call-latency-instrumentation`

Proposed for inclusion in `docs/action_bank.md` Sprint 24 R-item rows
(see §11 below). Sketch for the action_bank entry:

- **id**: `R-per-llm-call-latency-instrumentation`
- **layer**: `infra` / eval-harness.
- **scope**: instrument per-turn LLM round-trip timing in
  `LlmInvocationService.invokeChat` (the call site where
  `LlmDeadlineExceededException` originates), OR surface the existing
  log-emitted timing in `LlmCallLogger` into a structured field on
  `results.json` (a writer-side enrichment in the eval harness so
  smoke runs persist the per-turn duration). Collect a baseline
  distribution from a smoke rerun; compare against pre-`f2d4cb2` if
  the historical LLM-call log is still queryable, or take the
  current-model baseline as the new ground truth.
- **prerequisite flag**: **PREREQUISITE to any future
  deadline-budget widening or model-revert decision.** Until per-LLM
  -call latency is instrumented and persisted, the coarse proxy is a
  *signal*, not *evidence*; budget or model decisions made on this
  signal alone are not justified.
- **disposition**: `proposed` (Sprint 24 §11; investigation-only this
  sprint; instrumentation is a future sprint's bundle).
- **acceptance bar for the follow-on sprint that builds it**: a
  per-turn duration column in `results.json` (or sibling artefact)
  populated on every smoke run; a worked example comparing post-`f2d4cb2`
  per-call p50/p95 against a fixed-prompt synthetic baseline that
  isolates LLM round-trip from tool dispatch / persistence overhead.

### 4.6 Track B hard fences honoured

- READ-ONLY on `eval_interactive/results/**` (verified by `git diff`:
  no files under `eval_interactive/` are touched by Sprint 24's
  authored changes).
- No instrumentation shipped this sprint (R-item proposes; future
  sprint builds).
- No deadline-budget widening; no model config edit; no
  `prompt_projection` / eval-spec touch (verified by §5's files-changed
  list — no files under `eval_interactive/`, no model/budget config,
  no `system_prompt.txt`, no `eval/**`).

## 5. Files changed

Authored by Sprint 24 dev agent (single bundle for Track A; Track B
ships no code):

| path | change | line range (after edit) |
|------|--------|--------------------------|
| `server/src/main/java/com/gumtree/csagent/model/BotSession.java` | Added new `consecutive_deadline_count` `@Column` + `@Builder.Default = 0` `Integer` field directly below the V12 `runtime_error_count` field. | 79–81 |
| `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java` | Added `.consecutiveDeadlineCount(0)` builder line below `.runtimeErrorCount(0)` in the create-session builder. | 108 |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | (a) Added the consecutive-deadline reset hook in the outcome-dispatch prologue (mirroring the existing ERROR reset). (b) Split the shared `DEADLINE_EXCEEDED` / `LLM_UNAVAILABLE` case-block into two case-blocks; `DEADLINE_EXCEEDED` increments the counter and selects between the existing placeholder (count < 2) and a new distinct honest next-step message (count >= 2); `LLM_UNAVAILABLE` preserves its existing user message and `agent_llm_unavailable` transition tag. | reset hook 684–693; DEADLINE_EXCEEDED branch 765–803; LLM_UNAVAILABLE branch 804–817 |
| `server/src/main/resources/db/migration/V13__add_consecutive_deadline_count.sql` | New Flyway migration; `ALTER TABLE bot_sessions ADD COLUMN consecutive_deadline_count INTEGER NOT NULL DEFAULT 0;` mirroring V12 `runtime_error_count`. | new file |
| `server/src/test/java/com/gumtree/csagent/service/runtime/Sprint24DeadlinePlaceholderCoalesceTest.java` | New behaviour-level regression test (3 methods) asserting the placeholder-then-distinct-message-then-reset shape on `PhaseEvaluator.interpretRunResult`. | new file |
| `docs/sprints/sprint-024-handoff.md` | This handoff. | new file |

**Not authored by this dev agent (do not stage in close commit):**

- `csagent_system_design_review.md` (pre-existing working-tree mod).
- `server/src/main/resources/prompts/system_prompt.txt` (pre-existing
  working-tree mod; the source of the single pre-existing test
  failure noted in §3.4).
- `docs/sprint_objective.md` (deliver-agent owned, untracked).
- `compact/sprint-024-dev-prompt.md` / `compact/sprint-024-review-prompt.md`
  (deliver-agent owned, untracked).

## 6. Layer-classification self-walk (per-track, per `iteration_governance.md` §3)

### 6.1 Track A

Walking the §3.2 first-match-wins questions on the Track A change:

- **Q1 — Infra failure / timeout / OOM?** Yes. The cross-turn
  placeholder loop is the user-visible surface of an `infra` /
  phase-mapping issue: identical bot text on every `DEADLINE_EXCEEDED`
  outcome reads as a stuck loop and trips the runtime loop-detector.
  Track A's fix is in the `PhaseEvaluator` phase-mapping layer
  supported by a new `BotSession` column — the deterministic kernel
  surface that surfaces the deadline event to the user. **Q1 fires;
  Track A's layer is `infra`.**

No subsequent question is consulted (first-match wins). For
completeness:

- Q2 (Tier-0 invariant): no Tier-0 invariant added or invoked.
  Sprint 24 fence #5 holds.
- Q3 (`prompt_projection` impoverished): not the layer. The
  projection / LLM input is unchanged; the trigger is the
  *event-shape* count of consecutive deadlines, not a missing slot
  or candidate the LLM should have seen.
- Q4 (`skill_state`): the new `consecutive_deadline_count` is
  durable per-session state, but it is not multi-turn semantic
  skill state (no entity / task / intake field); it's a counter
  that mirrors `runtime_error_count`'s shape (V12), which Sprint 8.1
  classified `infra`.
- Q5 (`semantic_planner`): no LLM choice is moved or guided by the
  change. The 2nd-deadline message is emitted deterministically.
- Q6 (`eval_spec`): no eval-spec touch.
- Q7 (`product_policy`): no product / policy decision is being
  adjudicated.

### 6.2 Track B

- **Q1 — Infra timeout / OOM / startup crash?** Track B is
  investigation-only; no behaviour change. The diagnostic concerns
  `infra` / eval-harness — per-turn LLM round-trip latency is not
  surfaced by the eval harness. **Q1 fires (informational); Track B's
  layer is `infra` (diagnostic)**, matching the §7 stanza in
  `docs/sprint_objective.md` for Track B.

The proposed follow-on `R-per-llm-call-latency-instrumentation` is
also `infra` / eval-harness; it adds no Tier-0 invariant and no
semantic surface.

## 7. Anti-hardcode self-walk (§4.1, 9 questions, per-track)

### 7.1 Track A

| # | question | answer |
|---|----------|--------|
| 1 | Keyword / regex / if-else / enum / per-UC matrix for a semantic decision? | No. The trigger is the *event-shape* count (`session.consecutiveDeadlineCount`) of consecutive `DEADLINE_EXCEEDED` outcomes. No keyword / regex / if-else on user content; no per-UC matrix. The threshold check (`< 2`) and the new column are deterministic infra plumbing, mirroring V12 `runtime_error_count`. |
| 2 | If yes to (1), justified by a current Tier-0 invariant? | N/A — answer to (1) is no. |
| 3 | Could the same outcome be achieved by projecting a soft signal to the LLM instead of a hard branch? | No. The LLM is not in the deadline-emission path — `PhaseEvaluator` emits the user-facing text deterministically when the LLM call itself did not complete inside the wall-clock budget. There is no LLM choice point to which a soft signal could be projected on this surface. |
| 4 | Encode visible-eval case text, trace-specific phrasing, or a CaseSpec id into runtime / prompt / judge? | No. The 2nd-deadline message is principled prose ("I'm still having trouble responding in time. If you'd like, I can connect you with a specialist, or you can try again in a few minutes."); it does not mirror any CaseSpec rubric or eval-trace phrasing. The test asserts a property (next-step intent token list), not a literal LLM string. |
| 5 | Move semantic ownership from LLM to Java? | No. §1.3 LLM-owned surfaces are untouched — user goal, drift, escalation posture, response strategy on substantive turns. The deadline-fallback surface is already a `Runtime`-owned "trace and eval contract" responsibility (§1.4); Sprint 24 makes the deterministic emission shape kinder to the user under a known infra failure mode. |
| 6 | If-else block in the prompt instead of principle-level guidance? | No prompt edit; Sprint 24 fence #3 holds. |
| 7 | Preserve tool schema, capability / permission boundary, PII / safety floor, grounding floor? | Yes. No tool schema change, no permission change, no PII / safety surface touched, no grounding gate touched. |
| 8 | Generalization eval coverage — target / neighbor / negative / shadow? | Behaviour-level coverage. See §8 for the table. Target = the regression test's two-consecutive-deadline shape; neighbor = full server unit-test suite green (898 of 898 modulo the pre-existing `SystemPromptUserRequestedTiebreakerTest` failure inherited from the unrelated working-tree edit); negative = the reset-behaviour assertion in the same test; shadow = none built; deferred to G2 case-family. |
| 9 | If temporary, sunset plan? | Not temporary. The new column + counter is durable session state mirroring the V12 precedent. The 2nd-deadline message wording is principled; future tightening (if any) would belong in a separate R-item, not in this bundle. |

**Verdict (self-applied):** `approve`. No semantic hardcode introduced.

### 7.2 Track B

| # | question | answer |
|---|----------|--------|
| 1 | Keyword / regex / if-else / enum / per-UC matrix for a semantic decision? | No. Track B is investigation-only; no runtime / prompt / config edit. |
| 2 | If yes to (1), justified by a current Tier-0 invariant? | N/A. |
| 3 | Could the same outcome be achieved by projecting a soft signal to the LLM? | N/A — no decision surface changed. |
| 4 | Encode visible-eval case text? | No. The R-item proposal names the source code surface (`LlmInvocationService.invokeChat` / `LlmCallLogger`) and a class of metric (per-turn round-trip duration), not any case text. |
| 5 | Move semantic ownership from LLM to Java? | No. |
| 6 | If-else block in the prompt? | No prompt edit. |
| 7 | Preserve tool schema, capability / permission boundary, PII / safety floor, grounding floor? | Yes — Track B touches none of these. |
| 8 | Generalization eval coverage? | Track B is diagnostic and is exempt from §5.1 acceptance bars per the multi-layer prospective stanza precedent (Sprint 19 Track B was the first use; restated in `docs/sprint_objective.md` §5.2). The coarse-proxy table in §4.1 is the deliverable. |
| 9 | If temporary, sunset plan? | The new `R-per-llm-call-latency-instrumentation` proposal carries an explicit prerequisite-flag and a future-sprint acceptance bar (§4.5); it is the "downgrade-to-signal" follow-on for the §6.2 fence #6. |

**Verdict (self-applied):** `approve` (Track B is investigation-only;
no semantic hardcode possible without code).

## 8. Generalization-coverage table

### 8.1 Track A

| family | scope | result |
|--------|-------|--------|
| target | placeholder-loop event shape — sessions emitting ≥2 consecutive `DEADLINE_EXCEEDED` outcomes. Behaviour-level (Sprint 19 §3.7 named cs_002/cs_011/cs_014/cs_038/cs_040 as the original target set; Sprint 23 §4.1 re-derived; Sprint 24 treats the *shape*, not the CaseSpec list). | PASS — `Sprint24DeadlinePlaceholderCoalesceTest.secondConsecutiveDeadline_emitsDistinctHonestNextStepMessage`. |
| neighbor | full server unit-test suite, including all existing `PhaseEvaluator*Test`, `AgentRunLoop*Test`, `SessionManager*Test`, `Sprint8*Test`, `Sprint9*Test`, `Sprint10*Test`, etc., AND the integration tests that touch the deadline branch (`AgentRunLoopDeadlineExceededBotResponseIntegrationTest`, `Sprint81HonestFailureIntegrationTest`, `Sprint8Cs259EscalateBranchIntegrationTest.deadlineExceededOutcome_skipsFallback_evenWithEvents`). | PASS — 898 of 898 green; the single failure (`SystemPromptUserRequestedTiebreakerTest`) is pre-existing and unrelated to Sprint 24's authored files (see §3.4). |
| negative | single-deadline and first-turn-deadline cases: the existing placeholder still fires; the distinct honest message does NOT fire on a non-consecutive deadline. | PASS — `Sprint24DeadlinePlaceholderCoalesceTest.firstDeadlineExceeded_emitsExistingPlaceholder` (single deadline → placeholder) + `nonDeadlineOutcomeBetweenDeadlines_resetsCounterAndPlaceholderFiresAgain` (reset re-emits placeholder, not the distinct message). |
| shadow | none built. Per `docs/sprint_objective.md` §5.1 the Sprint 20 case families are referenced but **not consumed** by Track A; no shadow run executes on this sprint. | DEFERRED (G2). |

### 8.2 Track B

Track B is investigation-only and exempt from §5.1 acceptance bars
per the multi-layer prospective stanza (Sprint 19 Track B precedent;
`docs/sprint_objective.md` §5.2). The coarse-proxy data table in
§4.1 is the deliverable.

## 9. Sprint-objective-met check (per-bullet PASS / PARTIAL / GAP)

### 9.1 §1 (Goal)

| bullet | verdict | evidence |
|--------|---------|----------|
| Track A — deterministic UX repair on the cross-turn slow-LLM placeholder loop; session-scope state-tracking; distinct honest next-step on 2nd consecutive deadline. | **PASS** | §3 + §5 + §3.3 regression test. |
| Track B — honest coarse-proxy latency baseline; no per-call claim; follow-on instrumentation R-item proposed. | **PASS** (with §4.4 magnitude divergence and §10 open question) | §4.1–§4.5. |

### 9.2 §3 (Track A scope)

| bullet | verdict |
|--------|---------|
| Implement deterministic UX repair in `PhaseEvaluator.java`. | **PASS** (§5). |
| Use a session-scope deadline counter, mirroring `runtime_error_count`. | **PASS** (§3.1 #1 + §3.1 #3). |
| 1st consecutive slow-LLM deadline emits the existing placeholder. | **PASS** (§3.3 first test). |
| 2nd consecutive slow-LLM deadline emits a distinct honest next-step message. | **PASS** (§3.3 second test). |
| Reset the counter on any non-deadline outcome. | **PASS** (§3.1 #3 reset hook + §3.3 third test). |
| Add regression coverage demonstrating one placeholder + one distinct honest message, NOT two identical placeholders. | **PASS** (§3.3). |
| Keep cs_040 UC-K → UC-C routing **out of scope**. | **PASS** (§3.5). |
| §3.1 honest-next-step content constraints (name + actionable + ≠ placeholder + no user-content branching + no budget widening). | **PASS** (§3.2). |

### 9.3 §4 (Track B scope)

| bullet | verdict |
|--------|---------|
| Do NOT claim per-LLM-call latency can be extracted from existing eval artefacts. | **PASS** (§4.3 no-per-call-claim paragraph). |
| Use only honest existing signals (case-level `elapsed_ms`, total turns, run-level aggregates). | **PASS** (§4.1 table sources case-level `elapsed_ms` + `total_turns` only). |
| Report pre/post coarse proxy, including the observed p95 increase. | **PASS** with §4.2 / §4.4 magnitude divergence reported honestly. |
| Explicitly state that per-LLM-call latency requires future instrumentation. | **PASS** (§4.3 + §4.5). |
| Add a follow-on R-item for per-LLM-call latency instrumentation/persistence, flagged prerequisite to any budget / model decision. | **PASS** (§4.5 + §11). |

### 9.4 §5 (Generalization coverage)

PASS per §8.

### 9.5 §6 (Scope decisions and hard fences)

| fence | verdict |
|-------|---------|
| §6.1 out-of-scope: cs_040 UC-K → UC-C routing. | **PASS** (§3.5; routing unchanged). |
| §6.1 out-of-scope: `ChatController.java:125` legacy emit. | **PASS** (§3.6; controller untouched). |
| §6.2 #1 No deadline-budget widening. | **PASS** (no budget config touched; §5 files-changed list). |
| §6.2 #2 No model config change. | **PASS** (no model config touched; §5). |
| §6.2 #3 No `prompt_projection` work. | **PASS** (no projection-builder touched; pre-existing working-tree `system_prompt.txt` mod is unrelated, see §3.4). |
| §6.2 #4 No eval-spec work. | **PASS** (no `eval_interactive/case_specs/**` or `case_spec_overrides.yaml` touched). |
| §6.2 #5 No Tier-0 changes. | **PASS** (no `docs/runtime_freeze_and_risk_policy.md` touched). |
| §6.2 #6 Do not represent coarse proxy data as per-call latency evidence. | **PASS** (§4.3 + §4.4 explicitly hold both directions of the fence). |

### 9.6 §8 (Success metrics)

| bullet | verdict |
|--------|---------|
| §8.1 Track A UX bar — regression test asserts 1 placeholder + 1 distinct honest message (not 2 identical placeholders). | **PASS** (§3.3 second test). |
| §8.1 Full server unit-test suite green. | **PASS** modulo the pre-existing `SystemPromptUserRequestedTiebreakerTest` failure inherited from an unrelated working-tree edit (§3.4). 898 of 898 authored-files-green. |
| §8.1 Reset behaviour. | **PASS** (§3.3 third test). |
| §8.1 No budget widening / no model config / no projection / no eval-spec. | **PASS** (§9.5). |
| §8.2 Track B coarse-proxy data captured. | **PASS** (§4.1). |
| §8.2 Observed p95 increase reported. | **PASS** (§4.2, with §4.4 magnitude divergence). |
| §8.2 Follow-on R-item proposed with prerequisite-to-budget-decision flag. | **PASS** (§4.5 + §11). |
| §8.2 Explicit no-per-call-claim paragraph. | **PASS** (§4.3). |
| §8.3 Safety / grounding / wrong-containment / over-escalation floors. | **PASS** (no surface change on any of these). |

## 10. Open questions for human

1. **Coarse-proxy magnitude divergence (Track B).** The dev-session
   independent extraction in §4.1 yields case-level p50/p95 of
   pre **19.9s / 30.5s** vs post **41.1s / 92.3s** (n_cases=14 each,
   sum_turns 32 / 42). The planning-turn citation in
   `docs/sprint_objective.md` §2 (and dev prompt §4.1) is pre
   p50≈10.4s / p95≈24.6s vs post p50≈10.5s / p95≈27.6s, n="105 + 27
   case-turns". Both extractions agree on *direction* (post-`f2d4cb2`
   p95 widened); they disagree on *magnitude* (~3-second widening per
   the planning turn vs ~62-second widening per the dev session). The
   n value "105 + 27 case-turns" could not be reconstructed from any
   reasonable extraction (case count 14+14, transcript entries 66+82,
   sum_turns 32+42, bot turns 34+40). **Question for human:** which
   methodology should anchor the next sprint's `R-per-llm-call-latency-instrumentation`
   baseline comparison? If the planning turn used a different source
   (e.g. database `llm_call_log` rows rather than `results.json`),
   that source should be named in the R-item as the comparison
   ground-truth for the post-instrumentation rerun. The Track B
   *conclusion* (per-call instrumentation is the prerequisite to any
   budget / model decision) is robust to this question; only the
   pre-instrumentation magnitude estimate depends on it.

2. **`LLM_UNAVAILABLE` coalesce — defer to follow-on R-item?** Sprint
   24 explicitly scoped Track A to `DEADLINE_EXCEEDED` only (dev
   prompt §3.2 #5). The same cross-turn loop shape is theoretically
   possible on `LLM_UNAVAILABLE` (e.g. successive 5xx retries
   exhausted in two consecutive turns), and would emit the same
   identical "Sorry, I'm having trouble reaching the assistant right
   now…" placeholder on both turns. The dev agent did NOT bundle
   this; per dev prompt §3.2 #5 it should defer to an action_bank
   R-item. **Question for human:** should `R-llm-unavailable-placeholder-coalesce-honest-next-step`
   open as a paired R-item (n=0 evidence in the smoke set today,
   conditional opening per the controlled-multi-shape-testing bar)?

3. **`ChatController.java:125` legacy emission — n-ladder evidence?**
   Per `docs/sprint_objective.md` §6.1 and §10, the controller
   emission point is out-of-scope; it fires only when the deadline
   propagates past `AgentRunLoopImpl`. The dev session did not
   surface evidence that it is user-visible on the loop shape (the
   five Sprint 19 / Sprint 23 placeholder traces all originate at
   `PhaseEvaluator.java` line 765, not the controller). **Question
   for human:** does the controller emission warrant a defensive
   R-item ("audit whether the controller-layer placeholder fires on
   any production session and, if yes, route it through the same
   counter / message shape") or is the cross-turn loop surface fully
   covered by Track A?

## 11. Action-bank deltas (proposed; deliver agent applies on close)

Sprint 24 dev agent proposes the following updates to
`docs/action_bank.md`; the deliver agent applies on close. The four
existing R-item rows at lines 612–615 are touched per Sprint 24
`docs/sprint_objective.md` §9; one new row is appended.

1. **`R-slow-llm-placeholder-coalesce-honest-next-step`**
   (`docs/action_bank.md:612`) → **status: implemented (Sprint 24 Track A)**.
   Brief disposition note for the table cell: *"Closed by Sprint 24
   Track A. Delivered the proposed shape verbatim: V13 migration +
   `BotSession.consecutive_deadline_count` field + `SessionManager`
   builder init + `PhaseEvaluator` reset hook + DEADLINE_EXCEEDED
   threshold-gated message. Distinct 2nd-deadline text: 'I'm still
   having trouble responding in time. If you'd like, I can connect
   you with a specialist, or you can try again in a few minutes.'
   Regression `Sprint24DeadlinePlaceholderCoalesceTest` (3 tests)
   asserts placeholder / distinct-message / reset shape. NO
   deadline-budget widen; NO model config change. See `docs/sprints/sprint-024-handoff.md`
   §3."*

2. **`R-llm-latency-budget-investigation`**
   (`docs/action_bank.md:613`) → **status: reframed-as-coarse-proxy +
   per-call-instrumentation deferred to follow-on R-item**.
   Disposition note for the table cell: *"Sprint 24 Track B reframed
   the entry: confirmed that per-LLM-call latency is NOT in
   `eval_interactive/results/*/results.json` at the per-turn
   granularity needed; produced the coarse-proxy baseline from
   case-level `elapsed_ms` (see Sprint 24 handoff §4.1 / §4.2);
   reported the observed p95 widening with an explicit no-per-call
   -claim paragraph; deferred the per-call instrumentation work to
   the new `R-per-llm-call-latency-instrumentation` R-item below.
   Sprint 24 §10 question 1 surfaces a methodology question on the
   pre/post magnitude. Disposition: **reframed; follow-on instrumentation
   R-item is the prerequisite to any deadline-budget or model-revert
   decision.**"*

3. **NEW `R-per-llm-call-latency-instrumentation`** — appended as a
   new row in `docs/action_bank.md` §5.2 (or wherever Sprint 24's
   surfaced R-items land per the deliver agent's convention).
   Suggested row:

   > | id | source | description |
   > |----|--------|-------------|
   > | `R-per-llm-call-latency-instrumentation` | Sprint 24 §4.5 (Track B coarse-proxy reframe) | `infra` / eval-harness. Per-LLM-call latency is not in `eval_interactive/results/*/results.json` at per-turn granularity (Sprint 23 §4.2 + Sprint 24 §4.3 confirm). Proposed scope: instrument per-turn LLM round-trip timing in `LlmInvocationService.invokeChat` (the call site where `LlmDeadlineExceededException` originates) OR surface the existing `LlmCallLogger`-emitted timing into a structured field on `results.json`; collect a baseline from a smoke rerun and compare against pre-`f2d4cb2`. **PREREQUISITE to any future deadline-budget widening or model-revert decision.** Until per-call latency is instrumented and persisted, the coarse proxy is a signal, not evidence; budget / model decisions made on the proxy alone are not justified. Disposition: **proposed (Sprint 24 §4.5; investigation-only this sprint; instrumentation is a future sprint).** |

4. **`R-cs040-uc-k-topic-subject-routing`**
   (`docs/action_bank.md:614`) → **unchanged.** Sprint 24 did NOT act
   on the routing surface; the conditional-opening bar (controlled
   multi-shape testing) is unchanged. Sprint 24 handoff §3.5 honours
   the fence.

## 12. Next recommended action

Track A landed; Track B documented. Sprint 24 close recommendation:

| row | recommendation |
|-----|----------------|
| immediate | Codex sprint-close review of Track A diff scope (§5) + Track B handoff text (§4 + §11). Codex MUST verify §6.2 hard fences hold (no budget widening, no model config, no projection / eval-spec edit, no Tier-0 change, no per-call latency claim). Codex MUST verify the §3.3 regression test asserts BOTH placeholder (1st) AND distinct honest-message (2nd) AND reset, AND classify any per-LLM-call latency claim in handoff as a §6.2 #6 blocking violation. |
| close commit | Bundle the six authored files (§5) in a single Track-A close commit (commit-at-end). Do NOT stage the four working-tree files inherited from the deliver agent / pre-existing edits (§5 "not authored"). Avoid `git add -A` / `git add .`. |
| follow-on sprint | Build `R-per-llm-call-latency-instrumentation` (§4.5 + §11 row 3) as the next eval-harness sprint. Per §10 question 1, the dev agent on that sprint should reconcile the §4.4 magnitude divergence with the human (which methodology anchors the comparison ground-truth). |
| open observations | §10 questions 2 (`LLM_UNAVAILABLE` coalesce) and 3 (controller-layer emission audit) are recorded as open observations; opening either as a new R-item is at the human's discretion (n=0 evidence today on both; conditional opening per the multi-shape testing bar). |
| closure_verdict | **Sprint 24 close-out (2026-05-14): Codex sprint-close review at `docs/sprints/sprint-024-codex-review.md` returned `decision: pass, blocking_count: 0`. Codex's §4.1 per-PR Anti-Hardcode verdict: `approve` (all 9 questions answered, no semantic hardcode, no hard-fence violation). Codex summary verbatim: "Sprint 24 passes. The reviewed commit range (`6c12ec9..e21b1b6`) ships the Track A event-shape coalesce requested by the sprint objective: `BotSession.consecutiveDeadlineCount`, builder initialization, V13 migration, `PhaseEvaluator` reset/increment/threshold behavior, and a behavior-level regression test covering first deadline, second consecutive deadline, and reset. Track B remains investigation-only in the handoff, includes the coarse-proxy table plus no-per-call-claim paragraph, and proposes `R-per-llm-call-latency-instrumentation` as prerequisite to any future budget or model decision. No semantic hardcode or hard-fence violation was found." Two informational observations (non-blocking): (1) Track B magnitude discrepancy between planning-turn citation and dev-session independent extraction is correctly carried as a human open question for the follow-on instrumentation R-item, not a blocking Track B claim; (2) working-tree uncommitted files (`docs/sprint_objective.md`, `compact/sprint-024-*.md`, `csagent_system_design_review.md`, `server/src/main/resources/prompts/system_prompt.txt`) are deliver-agent-owned or pre-existing and are not scope drift under the packaging-rollforward rule. Classification: **A — Clean close**. No fix iteration required.** |
