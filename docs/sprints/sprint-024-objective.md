---
title: Sprint 24 — Slow-LLM Placeholder Coalesce + Coarse Latency Proxy (Two-Track)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-13
review_cadence: per sprint
supersedes: [docs/sprints/sprint-023-objective.md]
superseded_by: null
notes: >
  Two-track sprint paired off Sprint 23 §11 R-items. Track A is a
  deterministic UX repair on the cross-turn placeholder loop surface
  (`PhaseEvaluator.java` DEADLINE_EXCEEDED branch + a new
  session-scope `consecutive_deadline_count` mirroring V12
  `runtime_error_count` shape). Track B is investigation-only on
  latency: a coarse proxy from existing `elapsed_ms` / turn-count /
  run-level aggregates and an honest acknowledgment that per-LLM-call
  latency requires future instrumentation. NO deadline-budget
  widening; NO model config edit; NO `prompt_projection` work; NO
  eval-spec edit; NO Tier-0 invariant. `cs_040` UC-K → UC-C routing
  remains out of scope per the Sprint 23 fence. The legacy
  `ChatController.java:125` placeholder emission point is named as
  out-of-scope (different surface from the cross-turn loop). §7
  stanza takes multi-layer prospective per-track form (Track A:
  `infra`; Track B: `infra` diagnostic).
---

# Sprint 24 — Slow-LLM Placeholder Coalesce + Coarse Latency Proxy

Date opened: 2026-05-13
Branch: `design-v1-without-human-review`
Sprint class: two-track, semantic-touching, **§7 stanza required**
(multi-layer prospective per-track form).
UX-over-pass-rate framing per the human's 2026-05-14 reframe (Sprint
23 §13.4). Pass-rate effects are secondary; the primary bar is the
user-visible repair on the cross-turn placeholder loop.

## 1. Goal

Two issues, paired off Sprint 23 §11:

1. **Track A — deterministic UX repair on the cross-turn slow-LLM
   placeholder loop.** Today, consecutive `DEADLINE_EXCEEDED`
   outcomes from `LlmInvocationService.invokeChat` cause
   `PhaseEvaluator.java` to emit byte-identical placeholder text
   ("Sorry, I'm a bit slow right now. Please try sending that again
   in a moment.") on every deadline turn. From the user's seat this
   reads as a stuck loop. Sprint 24 Track A introduces session-scope
   state-tracking (a `consecutive_deadline_count` mirror of the
   existing `runtime_error_count` shape on `BotSession`) and, on the
   *second* consecutive deadline, emits a **distinct, honest
   next-step message** in place of repeating the placeholder.

2. **Track B — honest coarse-proxy latency baseline.** Sprint 19
   §3.7 hypothesised the commit `f2d4cb2 fix: switch primary llm to
   deepseek-v4-flash` widened LLM latency variance. The paired
   R-item `R-llm-latency-budget-investigation`
   (`docs/action_bank.md:613`) documents that **per-LLM-call latency
   is NOT in `eval_interactive/results/*/results.json`** at the
   per-turn granularity needed to verify the hypothesis directly.
   Sprint 24 Track B does the honest version: report a **coarse
   proxy** from data that IS present (case-level `elapsed_ms`, total
   turns, run-level aggregates) and propose a follow-on
   instrumentation R-item as the prerequisite to any future
   deadline-budget or model-revert decision. No instrumentation
   shipped this sprint; no per-call latency claim made.

## 2. Premise verification (carried forward from planning turn)

The Sprint 24 planning turn verified the following facts directly
against the runtime files. Sprint 24 may proceed without re-verifying
them; if a dev-session probe finds any of these no longer hold, STOP
and surface to human.

- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  lines 754–770 emit the existing placeholder text on
  `TerminalOutcome.DEADLINE_EXCEEDED`. The placeholder string is on
  line 765.
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  lines 75–77 declare `runtime_error_count` as a `@Column` /
  `@Builder.Default = 0` Integer field. This is the shape Track A's
  `consecutive_deadline_count` mirrors.
- `PhaseEvaluator.java` lines 675–752 are the V12 `runtime_error_count`
  precedent: reset-on-non-ERROR (line 681), increment-in-ERROR (line
  738–739), `< 2` threshold gating the user-facing message (line
  741). Track A's session-scope coalesce uses the same shape but on
  `DEADLINE_EXCEEDED`.
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  line 107 (`runtimeErrorCount(0)`) is the precedent for the
  `consecutive_deadline_count` init site.
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  line 204 catches `LlmDeadlineExceededException` and returns
  `AgentRunResult.deadlineExceeded(...)`. The placeholder re-emission
  is therefore at the phase-mapping layer (Track A's surface), NOT
  at the catch site.
- A **second** placeholder emission point exists at
  `server/src/main/java/com/gumtree/csagent/controller/ChatController.java:125`
  — the legacy `LlmDeadlineExceededException` catch in the
  controller layer. It emits the same byte-identical text but fires
  ONLY when the deadline propagates past `AgentRunLoopImpl`. This
  surface is **out of scope** for Sprint 24 (see §6).
- Coarse-proxy data is already extracted from
  `eval_interactive/results/`: pre-`f2d4cb2` p50 ≈ 10.4s / p95 ≈
  24.6s; post-`f2d4cb2` p50 ≈ 10.5s / p95 ≈ 27.6s (105 + 27
  case-turns, case-level `elapsed_ms`). Track B reports these
  numbers verbatim with the no-per-call-claim caveat.
- The Track B paired R-item entry at `docs/action_bank.md:613`
  proposes per-turn instrumentation as scope, NOT extraction. Track
  B's coarse-proxy reframe explicitly acknowledges this in the
  handoff (see §9 below).

## 3. Track A — narrow UX repair (in scope)

Per the human's Option-1 directive verbatim:

- Implement the deterministic UX repair in
  `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`.
- Use a session-scope deadline counter, mirroring the existing
  `runtime_error_count` pattern where appropriate.
- **First** consecutive slow-LLM deadline may emit the existing
  placeholder ("Sorry, I'm a bit slow right now. Please try sending
  that again in a moment.").
- **Second** consecutive slow-LLM deadline must emit a **distinct,
  honest next-step message** instead of repeating the placeholder.
- Reset the counter on any non-deadline outcome.
- Add regression coverage for the two-consecutive-deadline behaviour
  (the test must demonstrate one placeholder + one distinct honest
  message, NOT two identical placeholders).
- Keep `cs_040` UC-K → UC-C routing **out of scope** (§6).

### 3.1 Honest next-step message — content requirements

The 2nd-deadline message must satisfy these constraints (the dev
agent owns the natural wording within them):

- **Name what is happening** in plain language (the request is slow
  enough that the bot is unable to make progress this turn).
- **Offer an actionable next step.** Examples (non-exhaustive; the
  dev agent picks one and may compose):
  - offer a human handover ("Would you like me to connect you with
    a specialist?");
  - suggest the user retry after a short wait;
  - suggest an alternative channel (e.g. email).
- **Do NOT repeat the placeholder text** byte-for-byte; the LLM
  loop-detector treats identical consecutive bot turns as a loop
  signal, which is exactly what Track A is fixing.
- **Do NOT encode keyword/regex/if-else/enum on user content.** The
  coalesce trigger is the *event-shape* (consecutive
  `DEADLINE_EXCEEDED` outcomes), not anything the user said. This
  is the §1.7 hard gate for Track A.
- **Do NOT widen the deadline budget** to mask the symptom (§6 hard
  fence).

### 3.2 Files in scope (Track A)

- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  — DEADLINE_EXCEEDED branch (lines 754–770 today): add increment
  + threshold-check + distinct-message emission. Reset hook in the
  outcome-dispatch prologue (mirror lines 675–682).
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java`
  — add `consecutive_deadline_count` field (mirror the
  `runtime_error_count` `@Column` + `@Builder.Default = 0` shape
  at lines 75–77).
- `server/src/main/java/com/gumtree/csagent/service/runtime/SessionManager.java`
  — init `consecutiveDeadlineCount(0)` in the session builder
  (mirror line 107).
- Session persistence (Flyway migration or equivalent) — if
  schema migration is needed for the new column, ship the SQL
  alongside.
- Regression test (Track A) — a new test under
  `server/src/test/java/.../service/runtime/PhaseEvaluatorTest.java`
  or equivalent location, exercising two consecutive
  `DEADLINE_EXCEEDED` outcomes and asserting:
  - 1st outcome → existing placeholder text;
  - 2nd outcome → distinct honest next-step text (assert
    inequality + assert content properties: contains a
    next-step intent like "handover" / "retry" / "alternative"
    AND does NOT contain the literal placeholder string);
  - reset behaviour: a non-deadline outcome between the two
    re-zeros the counter and a subsequent deadline goes back to
    the existing placeholder.

### 3.3 Bundle policy (Track A)

Track A is a single-bundle, single-shape narrow fix. NO investigation
path inside Track A. If the dev agent encounters a finding that
would require widening scope (e.g. discovers the placeholder is also
emitted from a third surface, or that `LLM_UNAVAILABLE` should
coalesce the same way), defer it as an action_bank R-item; do NOT
expand Track A.

## 4. Track B — coarse-proxy latency report (investigation-only)

Per the human's Option-1 directive verbatim:

- Do NOT claim per-LLM-call latency can be extracted from existing
  eval artefacts.
- Use only honest existing signals: case-level `elapsed_ms`, total
  turns, available run-level aggregates.
- Report the pre/post coarse proxy, **including the observed p95
  increase** (≈24.6s → ≈27.6s, 105 + 27 case-turns).
- Explicitly state that per-LLM-call latency requires future
  instrumentation.
- Add a follow-on R-item for per-LLM-call latency instrumentation /
  persistence before any model-budget or deadline-widening decision.

### 4.1 Files in scope (Track B)

- `eval_interactive/results/**` — **READ-ONLY**. Track B reads the
  case-level `elapsed_ms`, the `total_turns`, and any run-level
  aggregates already present. Track B does NOT write to or modify
  any results.json.
- Sprint 24 handoff (`docs/sprints/sprint-024-handoff.md`) — the
  data table and the R-item proposal are written into the handoff
  document; **no other runtime / config / eval file is touched on
  Track B**.

### 4.2 Track B output requirements

The handoff §X (Track B section) MUST contain:

- The coarse-proxy data: pre/post `f2d4cb2` p50 and p95 from
  case-level `elapsed_ms`, with the n (case-turn count) called out
  and the limitation stated.
- An **explicit no-per-call-claim** paragraph: per-LLM-call latency
  is NOT present at the needed granularity in
  `eval_interactive/results/*/results.json` (this restates
  `docs/action_bank.md:613`); the coarse proxy is a session-level
  / case-level signal and conflates LLM round-trip time with tool
  dispatch + persistence + retry overhead.
- A concrete follow-on R-item proposal named
  **`R-per-llm-call-latency-instrumentation`** (or an equivalent
  name; the dev agent may finalise the slug) at the `infra` /
  eval-harness layer. The R-item must be flagged as a
  **prerequisite** to any future deadline-budget or model-revert
  decision. Plain-language statement in the handoff: "until per-LLM
  -call latency is instrumented and persisted, the coarse proxy is
  a *signal*, not *evidence*; deadline-budget or model-config
  decisions made on this signal alone are not justified."

### 4.3 Bundle policy (Track B)

Track B is **investigation-only**. No code, no config, no eval-spec
edits. No instrumentation shipped — the R-item proposes the
instrumentation; building it is a future sprint.

## 5. Generalization coverage (per track)

### 5.1 Track A coverage

| family | scope |
|---|---|
| target | placeholder-loop shape — cases that emit two or more consecutive `DEADLINE_EXCEEDED` outcomes. Sprint 19 §3.7 named `cs_002 / cs_011 / cs_014 / cs_040`; Sprint 23 §4.1 verified the *placeholder-text* surface on a subset. Track A's regression test covers the *behaviour* (two consecutive deadline outcomes), not a specific CaseSpec id. |
| neighbor | non-regressed cases on the full server unit-test suite must remain green. |
| negative | single-deadline cases AND first-turn deadline cases: the existing placeholder still fires; the distinct honest message does NOT fire on a non-consecutive deadline. The regression test's reset-behaviour assertion is the negative coverage. |
| shadow | none built; Sprint 20 case families are referenced but **not consumed** by Track A (no shadow run executes on this sprint). |

### 5.2 Track B coverage

Track B is not §5.1 acceptance-bar coverage — it is diagnostic
output. The §5 acceptance bars do not apply. The handoff §X data
table is the deliverable.

## 6. Scope decisions and hard fences

### 6.1 Explicit out-of-scope items

- **`cs_040` UC-K → UC-C routing** — carried forward from Sprint
  23 §3.3 and §4.4. Sprint 24 Track A fixes the *placeholder shape*
  on cs_040 (and any other case with the placeholder-loop surface);
  it does NOT touch UC-K vs UC-C routing. The action_bank R-item
  `R-cs040-uc-k-topic-subject-routing` at
  `docs/action_bank.md:614` remains conditionally open under the
  controlled-multi-shape-testing bar; Sprint 24 does not act on it.
- **`ChatController.java:125` legacy placeholder emission point**
  — out of scope. This is the legacy `LlmDeadlineExceededException`
  catch in the controller layer; it fires ONLY when the deadline
  propagates past `AgentRunLoopImpl`. Today's cross-turn loop
  surface is at `PhaseEvaluator.java:754–770`, not the controller.
  Sprint 24 does not touch the controller emission point; if the
  dev agent's investigation surfaces evidence that the controller
  is also user-visible on the loop shape, defer to an action_bank
  R-item — do not bundle.

### 6.2 Hard fences (six, verbatim)

1. No deadline-budget widening.
2. No model config change.
3. No `prompt_projection` work.
4. No eval-spec work.
5. No Tier-0 changes.
6. Do not represent coarse proxy data as per-call latency evidence.

These six fences apply identically to Track A and Track B and to
both the dev agent and the review agent.

## 7. Layer-classification + anti-hardcode stanza (per `iteration_governance.md` §7)

Sprint 24 is semantic-touching (Track A changes runtime emission
behaviour on a phase-mapping decision). The stanza takes the
multi-layer prospective per-track form per
`feedback_multi_layer_prospective_stanza.md` because the two tracks
sit at distinct layers and bundle policies.

### Track A

**Target failure layer:** `infra` — deterministic session-scope
state-tracked coalesce + emission of a distinct honest next-step on
the 2nd consecutive `DEADLINE_EXCEEDED`. The fix is at the
phase-mapping layer (`PhaseEvaluator`), supported by a new column
on `BotSession`. No new Tier-0 invariant; no semantic ownership
shift; no soft-signal projection to the LLM (the trigger is the
event shape, not user content).

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
deadline-budget itself is a Runtime concern (§1.4 "budget /
timeout") but Sprint 24 does NOT change the budget. The fix is in
the user-facing emission shape on cross-turn deadline repetition.

**Semantic hardcode:** No semantic hardcode introduced. The
coalesce trigger is the *event-shape* count of consecutive
`DEADLINE_EXCEEDED` outcomes, NOT a regex / keyword / if-else /
enum on user content. The 2nd-deadline message content names the
slowness and offers an actionable next step; it does NOT branch on
UC, on user phrasing, or on a per-CaseSpec rubric. The §1.7 hard
gate prohibits encoding eval phrases or per-UC matrices into the
message — the dev agent owns natural wording within the §3.1
constraints.

**Generalization coverage:** target / neighbor / negative / shadow
case counts: behaviour-level coverage (no CaseSpec id list) with
target = the two-consecutive-deadline regression test; neighbor =
the full server unit-test suite; negative = the reset-behaviour
assertion in the same test; shadow = 0 (deferred to G2). See §5.1
for the table.

### Track B

**Target failure layer:** `infra` (diagnostic). Track B is
investigation-only: it produces a coarse-proxy data table plus a
follow-on R-item. No layer's behaviour is changed by Track B.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant on
Track B. The R-item Track B proposes is also `infra` /
eval-harness layer and adds no Tier-0 invariant in itself.

**Semantic hardcode:** No semantic hardcode introduced on Track
B. Track B touches no runtime semantic surface.

**Generalization coverage:** Track B is diagnostic and is exempt
from §5.1 acceptance bars per the multi-layer prospective stanza
precedent (Sprint 19 Track B was the first use). The handoff
data-table is the deliverable.

## 8. Success metrics

### 8.1 Track A (UX-over-pass-rate primary)

- **UX bar (primary):** the regression test asserts that two
  consecutive `DEADLINE_EXCEEDED` outcomes emit 1 placeholder + 1
  distinct honest message, NOT 2 identical placeholders. Test
  passes.
- Full server unit-test suite green (`mvn -pl server -am test` or
  equivalent).
- `consecutive_deadline_count` resets correctly on a non-deadline
  outcome between two deadlines; the regression test's reset
  assertion passes.
- No widening of the deadline budget; no model config change; no
  edits to `prompt_projection` or eval-spec.

### 8.2 Track B (diagnostic)

- Coarse-proxy data captured in the handoff (pre/post p50 + p95
  with case-turn n).
- The observed p95 increase (≈24.6s → ≈27.6s) is reported.
- The follow-on R-item is proposed with a concrete name and a
  prerequisite-to-budget-decision flag.
- Explicit no-per-call-claim paragraph present in the handoff.

### 8.3 Safety / grounding / wrong-containment / over-escalation (§5.1 floors)

- **Safety floor unchanged:** Track A introduces no PII / safety
  surface change.
- **Grounding floor unchanged:** Track A introduces no retrieval /
  grounding surface change.
- **Wrong-containment rate:** unchanged (Track A does not move
  routing). cs_040's mis-routing is explicitly out of scope (§6.1).
- **Over-escalation rate:** the 2nd-deadline message MAY offer a
  handover; this is offered, not auto-triggered. The bot does NOT
  call `request_handover` from the placeholder loop surface; the
  user chooses whether to take the offer.

## 9. Action-bank deltas Sprint 24 will produce (proposed; deliver agent applies on close)

(Sprint 24 dev agent proposes these in handoff §11; deliver agent
applies on close. Listed here for the dev agent's reference.)

- `R-slow-llm-placeholder-coalesce-honest-next-step`
  (`docs/action_bank.md:612`) → **status: implemented** (Track A
  closes it). Brief disposition note + cross-reference to the
  Sprint 24 handoff §X bundle summary.
- `R-llm-latency-budget-investigation`
  (`docs/action_bank.md:613`) → **status: reframed-as-coarse-proxy
  + per-call-instrumentation deferred to follow-on R-item**.
  Update the entry to record that Sprint 24 acknowledged the
  per-call data gap and produced the coarse-proxy baseline; the
  per-call instrumentation work is moved to the follow-on R-item.
- **NEW** `R-per-llm-call-latency-instrumentation`
  (or equivalent slug; finalised by the dev agent in the handoff)
  — `infra` / eval-harness layer. Scope: instrument per-turn LLM
  round-trip timing in `LlmInvocationService.invokeChat` or
  surface the existing log-emitted timing into a structured field
  on results.json. Prerequisite to any future deadline-budget or
  model-revert decision.
- `R-cs040-uc-k-topic-subject-routing`
  (`docs/action_bank.md:614`) → **unchanged.** Sprint 24 did NOT
  act on the routing surface; the conditional-opening bar
  (controlled multi-shape testing) is unchanged.

## 10. Files explicitly NOT in scope

- `server/src/main/java/com/gumtree/csagent/controller/ChatController.java`
  — legacy `LlmDeadlineExceededException` catch on line 115 / line
  125 placeholder emission. **Different surface from the
  cross-turn loop targeted by Sprint 24** (see §6.1).
- `server/src/main/resources/prompts/system_prompt.txt` — Sprint
  24 fence #3: no `prompt_projection` work.
- `eval_interactive/case_specs/**` and
  `eval_interactive/case_spec_overrides.yaml` — Sprint 24 fence
  #4: no eval-spec work.
- `eval_interactive/results/**` — READ-ONLY for Track B (§4.1).
  No writes / modifications.
- `docs/runtime_freeze_and_risk_policy.md` — Sprint 24 fence #5:
  no Tier-0 changes.
- Model config / deadline budget config files (under `server/src/main/resources/`
  or equivalent) — Sprint 24 fences #1 and #2.
- Any `prompt_projection` modules (the runtime per-turn projection
  builders) — Sprint 24 fence #3.
- `AlreadyCalledPromptConsumptionTest.java` — Sprint 23 Track A
  test asset; out of scope (different track).
- `docs/sprints/*` and `docs/archive/*` — immutable archives.

## 11. Bundle policy summary

- **Track A bundles** the narrow UX repair: `PhaseEvaluator` edit +
  `BotSession` column + `SessionManager` init + persistence
  migration + regression test. Single commit at end (per the
  human's commit-at-end pattern).
- **Track B is investigation-only.** No code shipped; handoff §X
  documents the coarse-proxy data + the follow-on R-item.

## 12. Review rule

- Codex reviews the diff scope under §3.2 (Track A files) and the
  handoff text under §4 / §9 (Track B data + R-item proposal).
- Codex MUST verify the §6.2 hard fences hold (no budget widening,
  no model config edit, no prompt_projection / eval-spec edit, no
  Tier-0 change, no per-call latency claim).
- Codex MUST verify Track A's regression test asserts BOTH the
  placeholder-emission on the 1st deadline AND the distinct
  honest-message emission on the 2nd, AND the reset behaviour.
- Codex MUST classify a per-LLM-call latency claim in the handoff
  as a **blocking** §6.2 #6 violation.
- Codex MUST NOT flag the `ChatController.java:125` legacy emission
  point as a missed surface — it is explicitly named out-of-scope
  in §6.1.
- Codex MUST NOT flag the `cs_040` UC-K routing as a missed surface
  — same fence.
- Deliver-agent-owned files in the working tree at commit time
  (sprint_objective.md, compact/sprint-024-*.md, the orchestrator
  playbook) are NOT scope drift — they are packaging artefacts of
  the commit-at-end workflow. See
  `feedback_out_of_scope_review_packaging_rollforward.md`.

## 13. Stop conditions (for both dev and review agents)

- If the carried-forward facts in §2 no longer hold (e.g.
  `PhaseEvaluator.java:754–770` has moved, `runtime_error_count`
  has been refactored), STOP and re-verify against the runtime
  files before proceeding.
- If Track A's regression test requires touching a fenced file to
  exercise the two-deadline shape, STOP and surface to human; do
  NOT silently widen scope.
- If Track B's coarse-proxy work surfaces a temptation to either
  widen the deadline budget OR claim per-call evidence from
  case-level `elapsed_ms`, STOP and reread §6.2 #6.
- If any temptation arises to fix the `ChatController.java:125`
  emission point OR re-route cs_040 UC-K → UC-C, STOP and reread
  §6.1.

## 14. Handoff requirements

The dev agent writes `docs/sprints/sprint-024-handoff.md` with the
standard 12-section shape (per Sprint 19 / Sprint 23 precedent):

1. Context Pack (per `agent_context_guide.md`).
2. Sprint-objective recap.
3. Track A — per-case § (the regression test as evidence; no
   per-CaseSpec walk required since Track A's target is
   behaviour-level).
4. Track B — coarse-proxy data table + no-per-call-claim
   paragraph + follow-on R-item proposal.
5. Files changed.
6. Layer-classification self-walk (per-track).
7. Anti-hardcode self-walk (§4.1, 9 questions, per-track).
8. Generalization-coverage table (per §5).
9. Sprint-objective-met check (per-bullet PASS / PARTIAL / GAP
   against §§1, 3, 4, 5, 6, 8).
10. Open questions for human.
11. Action-bank deltas (per §9, proposed; deliver agent applies on
    close).
12. Next recommended action.

Working tree may contain deliver-agent-owned files at session start
(see `feedback_commit_at_end_bundles_deliver_artefacts.md` for the
full list). The dev agent should NOT stage these and should NOT use
`git add -A` / `git add .` for the close commit; stage only the
Track-A authored files explicitly. Final commit shape is the
human's call (commit-at-end pattern).
