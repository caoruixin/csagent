---
title: Sprint 23 — Repeated FAQ Calls + LLM Stall Root-Cause Investigation (Two-Track)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-14
review_cadence: per sprint
supersedes: [docs/sprints/sprint-022-objective.md]
superseded_by: null
notes: >
  Sprint 23 is a two-track root-cause-and-fix sprint targeting two
  user-visible failure shapes surfaced in the 2026-05-10 smoke run:
  Track A — repeated FAQ / search_knowledge tool calls; Track B —
  LLM instability / deadline / placeholder / stall behaviour.
  Investigation+bundle shape per Sprint 19 precedent. UX-over-pass-
  rate framing. Semantic-touching → §7 stanza required in multi-
  layer prospective per-track form. Re-derive both target case lists
  from actual results JSON, NOT from prior summaries; Sprint 19 §3.7
  named a 5-case list (cs_002, cs_011, cs_014, cs_038, cs_040) that
  premise-verification in the Sprint 23 planning turn found to be
  internally inconsistent with Sprint 19 §3.2 (cs_011 has no
  placeholder emission) and incomplete (cs_066 is a clean placeholder
  loop but was attributed to a different shape in Sprint 19 §3.6).
---

# Sprint 23 — Repeated FAQ Calls + LLM Stall Root-Cause Investigation

Date opened: 2026-05-14
Branch: `design-v1-without-human-review`
Sprint class: two-track investigation+bundle, semantic-touching.
§7 stanza **REQUIRED** in multi-layer prospective per-track form
(precedent: Sprint 19 A+B, Sprint 20 A+B). UX-over-pass-rate framing
per human's reframe of 2026-05-14.

## 1. Goal

Determine the actual root cause(s) for two user-visible reliability
failures observed in 2026-05-10 smoke run + the 2026-05-13 manual
probe, and ship narrow fixes only where evidence is conclusive:

1. **Track A — Repeated FAQ / search_knowledge tool calls.** The
   2026-05-13 manual probe documented `1 LLM request → 3 identical
   search_knowledge executions, same params and same results`.
   Sprint 19 §4 walked three hypotheses and concluded the LLM is
   re-emitting the same call across consecutive AgentRunLoop steps
   (multi-step reading). Sprint 20 Track B landed the
   `already_called: [{tool, arguments_hash, at_step}]` projection
   slot to surface the duplicate-call diagnostic to the LLM, but
   **prompt consumption (system_prompt.txt teaching the LLM about
   the slot) was deferred to `R-already-called-prompt-consumption`
   and never landed.** This sprint determines whether repeated
   FAQ calls are caused by: (a) LLM re-emitting identical or
   near-identical tool calls; (b) orchestrator / ToolDispatcher
   amplification; (c) missing or ineffective `already_called`
   prompt consumption; (d) projection failure where prior FAQ
   results are not visible or not salient; (e) phase-plan directive
   non-followship. Bundle the safest narrow fix if evidence is
   conclusive for one root cause; defer the rest as R-items.

2. **Track B — LLM instability / deadline / placeholder / stall.**
   The 2026-05-10 smoke run produced a cross-turn placeholder loop
   shape in multiple cases (clean loops: cs_002, cs_014, cs_040
   per planning-turn re-derivation; emits+recovers shape: cs_038,
   cs_259; cs_011 has zero placeholders; cs_066 verification
   pending dev's own re-derivation; cs_176 has interleaved
   pattern). Sprint 19 §3.7 hypothesised model switch
   `f2d4cb2 fix: switch primary llm to deepseek-v4-flash` (2026-05-06
   21:48) widened latency variance; the placeholder loop is the
   symptom amplifier (single-emit baseline existed pre-switch).
   This sprint determines whether the root cause is: (i) model
   latency; (ii) deadline budget; (iii) retry/backoff behaviour;
   (iv) PhaseEvaluator fallback handling; (v) session-scope
   placeholder repetition; (vi) turn-budget / phase-transition
   mapping. If evidence supports a narrow user-facing fix, ship
   session-scope slow-LLM placeholder coalescing with an honest
   next-step message and regression coverage. If evidence points
   to model latency / timeout config, **propose a separate
   model/budget/retry R-item with supporting data; do NOT widen
   the deadline budget in this sprint.**

UX-over-pass-rate framing: a fix that improves customer-visible
reliability and problem-solving behaviour wins over a fix that
improves pass-rate but masks the user-facing artefact.

## 2. Non-goals

This sprint is NOT a symptom-only placeholder-coalesce sprint
(that framing was explicitly rejected by the human on 2026-05-14).
This sprint does NOT widen the deadline budget under any
circumstance. This sprint does NOT change model provider or model
configuration. This sprint does NOT edit eval-spec surfaces
(CaseSpecs, overrides, personas, judge rubric, case families).
This sprint does NOT pass-rate-optimize at the cost of UX. This
sprint does NOT bundle a fix for any root cause where evidence
is inconclusive.

## 3. Hard fences (verbatim from human directive)

- Do not treat placeholder coalescing as the root cause unless
  evidence shows the root cause is purely user-facing repeated
  fallback messaging.
- Do not assume the 5-case Cluster C list is accurate; re-derive
  the target set from results and transcripts.
- Do not conflate cs_040's placeholder loop with its UC-K → UC-C
  routing issue.
- Do not ship eval-only bookkeeping in this sprint.
- Prioritize user-visible reliability and problem-solving
  behavior over pass-rate improvements.
- Do not widen the deadline budget without latency evidence.
- Do not widen the deadline budget in this sprint period (even
  if latency evidence exists, that is a follow-on R-item).

## 4. Known-at-scope-time facts (deliver-agent-verified, do not re-verify)

The deliver agent verified the following in the planning turn
(2026-05-14). The dev still does its own re-derivation per §3
("Do not assume the 5-case Cluster C list is accurate"); the
facts below are starting points, not gaps in the dev's work.

1. **Slow-LLM placeholder text source.** Emitted by
   `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
   lines 754–770. The text is `"Sorry, I'm a bit slow right now.
   Please try sending that again in a moment."`. **NOT** emitted
   by `ProgressPlaceholderService.java` — that service emits a
   separate slow-tool placeholder (lines 20–24), a different
   surface.
2. **Deadline path.**
   `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
   line 204 catches `LlmDeadlineExceededException` and returns.
   Placeholder emission happens after return, at the phase-mapping
   layer. Re-emission is **cross-turn**, not intra-turn.
3. **`already_called` projection slot.** Lives in
   `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
   from Sprint 20 Track B. JSON shape: `already_called: [{tool,
   arguments_hash, at_step}]`. Wired and emits correctly per
   Sprint 20 unit tests. **Prompt consumption — `system_prompt.txt`
   teaching the LLM how to read and act on the slot — was deferred
   to `R-already-called-prompt-consumption` and never landed.** If
   Track A's evidence supports prompt consumption as the safest
   narrow fix, this is the bundled fix; the layer is
   `prompt_projection`.
4. **Sprint 19 §3.7 root-cause hypothesis on Track B.** Model
   switch `f2d4cb2 fix: switch primary llm to deepseek-v4-flash`
   (2026-05-06 21:48) widened latency variance; placeholder loop
   is the symptom amplifier. Single-emit placeholder baseline
   existed pre-switch (cs_038 / cs_259 shape). This is a
   hypothesis, not a settled root cause; Track B's evidence
   collection must support or reject it.
5. **Placeholder-loop case re-derivation.** Planning-turn grep on
   `eval_interactive/results/20260510-134558/results.json` for
   the placeholder text mapped to case_id boundaries: clean loops
   = {cs_002, cs_014, cs_040}; emits+recovers (different shape)
   = cs_038, cs_259; zero placeholders = cs_011; interleaved /
   pending dev verification = cs_066, cs_176. **The dev re-derives
   independently;** these are deliver-agent observations, not
   prescriptions.

## 5. cs_040 special handling (separate-issue fence)

cs_040 appears in Track B's preliminary target set as a clean
placeholder loop. cs_040 also has a UC-K → UC-C routing failure
documented in Sprint 19 §3.5 (prompt_projection lost topic_subject
cue). **These are SEPARATE issues at different layers.** Track B's
narrow fix on cs_040 (if shipped) addresses the placeholder UX
ONLY; the routing failure remains an open R-item (possibly already
captured as `R-prompt-phase-plan-directive-followship` — the dev
verifies and proposes the disposition). The handoff §3 root-cause
matrix must call this out explicitly per-case and the dev's
regression-test rationale must NOT claim to fix the routing.

## 6. Track A scope — Repeated FAQ / search_knowledge

### 6.1 Five hypotheses to evaluate

The dev walks these in order, citing concrete evidence (transcript
excerpts, ToolEvent records, projection contents, code paths) for
each ruling. The §3.2 first-match-wins discipline applies.

a. **LLM re-emitting identical or near-identical tool calls.** This
   is the hypothesis Sprint 19 §4 found consistent with the code.
   Evidence required: ToolEvent stream from a target case showing
   ≥2 identical successful `search_knowledge` dispatches with
   matching `arguments_hash` and matching `resultData`; the
   corresponding `ContextProjectionBuilder.build()` output showing
   the `already_called` slot populated correctly BEFORE the
   re-emission.
b. **Orchestrator / ToolDispatcher amplification.** Sprint 19
   §4.1 walked this and found no de-dup. Evidence required:
   ToolDispatcher / AgentRunLoopImpl walk-through showing whether
   1 LLM-emitted tool_call can produce >1 dispatched
   `search_knowledge` execution. If found, layer is `infra`.
c. **Missing or ineffective `already_called` prompt consumption.**
   The Sprint 20 slot is wired but no system_prompt.txt teaches
   the LLM how to read it. Evidence required: `system_prompt.txt`
   grep for `already_called` (expected: zero hits); a target
   case's serialized prompt confirming the slot is in the JSON
   payload but no teaching text references it.
d. **Projection failure — prior FAQ results not visible or not
   salient.** Evidence required: the same target case's serialized
   prompt examined for whether `accumulated_tool_results` contains
   the prior search_knowledge result and whether the projection
   order / formatting buries it.
e. **Phase-plan directive non-followship.** The bot ignores the
   DISCOVER / RESOLVE `phase_plan.system_instruction` MUST clauses.
   Sprint 18 cs_259 brief + manual-probe 2026-05-13 observed this
   shape; the user said n=1 observations do not justify opening
   `R-prompt-phase-plan-directive-followship` without controlled
   multi-shape testing. Evidence required: target case's
   `phase_plan.system_instruction` text + LLM's emitted tool_call
   pattern showing the deviation.

### 6.2 Target set (dev re-derives)

The dev's FIRST Track A task is to re-derive the target set from
`eval_interactive/results/20260510-134558/results.json` and the
2026-05-13 manual-probe brief: cases with ≥2 successful
`search_knowledge` dispatches with same / near-same `arguments_hash`
within one session. Document the search method (grep pattern,
parsing approach), the case-by-case enumeration, and the count.
Do NOT inherit any list from prior summaries.

### 6.3 Bundle-or-defer policy

- If evidence is conclusive for ONE root cause, bundle the safest
  narrow fix (typically a `prompt_projection` soft signal — e.g.
  `R-already-called-prompt-consumption` landing — NOT an orchestrator
  /Java guard). Defer the rest as R-items with supporting evidence.
- If evidence is conclusive for MULTIPLE root causes, bundle the
  safest narrow fix only; defer the rest.
- If evidence is inconclusive, the output is investigation-only:
  evidence table + R-items, no fix. This is an acceptable Track A
  outcome — investigation has standalone value.

## 7. Track B scope — LLM instability / deadline / placeholder

### 7.1 Six hypotheses to evaluate

a. **Model latency.** Evidence required: per-turn LLM round-trip
   timing data from the affected target cases; comparison with the
   pre-`f2d4cb2` baseline if available. If conclusive, this is a
   FOLLOW-ON R-item (`R-llm-latency-budget-retry` or similar with
   the supporting latency data); do NOT widen the budget in this
   sprint.
b. **Deadline budget.** Evidence required: current budget config
   value(s); observed timing distribution. If conclusive, propose
   a follow-on R-item; do NOT change the budget value.
c. **Retry/backoff behaviour.** Evidence required: walk
   `LlmInvocationService` retry logic; look for cross-turn
   amplification.
d. **PhaseEvaluator fallback handling.** Evidence required: walk
   `PhaseEvaluator.java` lines 754–770 + the
   `TerminalOutcome.DEADLINE_EXCEEDED` mapping; identify whether
   consecutive deadline events emit identical text without
   session-scope coalescing.
e. **Session-scope placeholder repetition.** Sprint 19 §3.1's
   diagnosis: two consecutive deadline events emit identical
   placeholder text, which the runtime loop detector flags as a
   loop. If this is the dominant root cause AND a fix is purely
   user-facing repeated fallback messaging, the bundled fix lands
   here.
f. **Turn-budget / phase-transition mapping.** Evidence required:
   walk the AgentRunLoop replan logic + the loop-detector firing
   condition.

### 7.2 Target set (dev re-derives)

The dev's FIRST Track B task is to re-derive the target set from
`eval_interactive/results/20260510-134558/results.json`: cases
with ≥2 consecutive `PhaseEvaluator` placeholder emissions
across turns (grep on the exact placeholder text
`"Sorry, I'm a bit slow right now. Please try sending that again
in a moment."`). Distinguish:

- **Clean loops:** ≥2 consecutive placeholders in the user-visible
  trace. (Deliver-agent observation: cs_002, cs_014, cs_040 from
  the planning-turn grep.)
- **Emits+recovers:** single placeholder then recovery. (Deliver-
  agent observation: cs_038, cs_259.)
- **Zero placeholders:** no placeholder in the case. (Deliver-
  agent observation: cs_011 — Sprint 19 §3.2 classified as
  `semantic_planner` per §3.2 Q5, NOT a Track B case.)
- **Interleaved / pending:** dev verifies. (cs_066, cs_176 from
  planning-turn grep.)

Cases can be in both Track A and Track B target sets, only one,
or neither.

### 7.3 cs_040 placeholder-vs-routing fence

cs_040 is a clean placeholder loop AND a UC-K→UC-C routing case.
Track B's narrow fix addresses the placeholder UX only. The
routing failure is a separate `prompt_projection` layer issue
documented in Sprint 19 §3.5; the dev names it explicitly in §3
of the handoff and proposes a disposition (open new R-item, or
broaden an existing `R-prompt-phase-plan-directive-followship`
if the n≥2 threshold is now met — verify against action_bank
state).

### 7.4 Bundle-or-defer policy

- Bundle session-scope coalesce + honest next-step message ONLY
  if evidence shows root cause is purely user-facing repeated
  fallback messaging.
- If evidence points to model latency or timeout config as root
  cause, defer with an R-item carrying the supporting latency
  data. Do NOT widen the deadline budget under any circumstance
  in this sprint.
- If evidence is inconclusive across the six hypotheses, the
  output is investigation-only.
- The honest next-step message must be **honest**: name what's
  happening ("This is taking longer than expected") and offer an
  actionable next step (handover offer, retry suggestion). Do NOT
  mask the underlying slowness with reassuring filler. Do NOT
  promise a specific recovery timeline.

## 8. Files in scope

Read-only:

- `eval_interactive/results/20260510-134558/results.json` —
  evidence source for both tracks.
- `docs/diagnostics/failure-briefs/manual-probe-2026-05-13-ad-visibility-multi-layer-failure.md`
  — manual-probe ground-truth for Track A.
- `docs/sprints/sprint-019-handoff.md` §3 (per-case §3.2 walks) +
  §4 (orchestrator de-dup investigation).
- `docs/sprints/sprint-020-handoff.md` §5 (`already_called` slot
  shape).
- `docs/action_bank.md` (read; dev proposes deltas in §12;
  deliver agent applies on close).

Edit (only if evidence supports a bundled fix):

- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  — Track B fix surface IF root cause is purely user-facing repeated
  fallback messaging.
- `server/src/main/java/com/gumtree/csagent/service/runtime/AgentRunLoopImpl.java`
  — Track A IF orchestrator de-dup is the cause; Track B IF
  session-state addition needed for the coalesce.
- `server/src/main/java/com/gumtree/csagent/service/tools/ToolDispatcher.java`
  — Track A IF orchestrator/dispatcher amplification is the cause.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java`
  — Track A IF projection failure is the cause OR if Track A
  bundles a new soft signal.
- `server/src/main/resources/system_prompt.txt`
  — Track A IF the bundled fix is `R-already-called-prompt-consumption`
  (teaches the LLM about the slot landed in Sprint 20).
- `server/src/test/java/com/gumtree/csagent/**` — regression tests
  for any bundled fix.

Write (new files):

- `docs/sprints/sprint-023-handoff.md` — handoff with root-cause
  matrix as §3 or §4.

Refresh:

- `docs/10-handoff.md` — Sprint 23 lead.

## 9. Files NOT in scope (hard fence)

- All eval-spec surfaces: `eval_interactive/case_specs/**`,
  `eval_interactive/case_spec_overrides.yaml`,
  `eval_interactive/personas*.yaml`, the judge rubric
  (`eval_interactive/eval_interactive/case_spec/llm_persona_reviewer.py`).
- Sprint 20 case families: `eval_interactive/case_specs/case_families/**`,
  `eval_interactive/case_specs_shadow/case_families/**`.
- Foundational docs (`docs/foundational/**`).
- Governance docs (`docs/current/iteration_governance.md`,
  `docs/current/doc_governance.md`,
  `docs/current/agent_context_guide.md`).
- All sprint archives (`docs/sprints/sprint-001-*.md` through
  `docs/sprints/sprint-022-*.md`).
- `docs/runtime_freeze_and_risk_policy.md` (no Tier-0 candidacy in
  this sprint).
- FAQ corpus (`data/faq/**`, `FAQ-knowledge_include_help_url.csv`).
- Deadline / timeout configuration.
- Model switch / model provider configuration.

## 10. §1.7 forbidden-line guardrail (both tracks)

- No keyword/regex/per-UC matrix for a semantic decision in any
  bundled fix.
- No eval-rubric widening to accept the new bot output.
- No prompt if-else block. If Track A's fix lands as
  `R-already-called-prompt-consumption`, the teaching text MUST be
  principled (teach the LLM what the slot means and what
  observable state implies; do NOT branch on tool name or UC).
- Track B's honest next-step message must NOT mask the underlying
  slowness with reassuring filler. Honest = name the slowness +
  offer actionable next step.
- No new Tier-0 invariant introduced.
- No new semantic ownership migrated from LLM to Java.

## 11. Layer-classification + anti-hardcode stanza

**Target failure layer:** multi-layer prospective per-track
(precedent: Sprint 19 A+B). The dev walks §3.2 per case in each
track and identifies the matching layer.

- **Track A primary candidate layers:** `prompt_projection` |
  `semantic_planner` | `infra` | `human_review_required`. Resolves
  to one layer at sprint close based on evidence. If
  `R-already-called-prompt-consumption` lands as the bundled fix
  (teaching the LLM about the Sprint 20 slot), layer is
  `prompt_projection`. If orchestrator/dispatcher amplification is
  the cause, layer is `infra` and the fix is a deterministic
  invariant (e.g. de-dup successful identical dispatches within a
  session). If the LLM is choosing to re-emit despite the slot
  being correctly populated and the prompt teaching it, layer is
  `semantic_planner` and the disposition is investigation-only
  (deferred fix).
- **Track B primary candidate layers:** `infra` | `prompt_projection`
  | `human_review_required`. The user-facing fix (placeholder
  coalesce with honest next-step) is `infra` deterministic. The
  "model latency / timeout config" follow-on R-item is `infra`
  but OUT-OF-SCOPE this sprint per §3 hard fence.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. The
runtime placeholder branch + the AgentRunLoop deadline path live
inside the Runtime's "trace and eval contract" responsibility
(§1.4) and are deterministic invariants. Any §3.2 Q2 branch that
re-classifies a finding as `java_guard` (a Tier-0 candidate) MUST
be deferred and routed to `human_review_required` per §3.2 tail
rule — do not invent a new Tier-0.

**Semantic hardcode:** No semantic hardcode introduced. Both
tracks' bundles forbid keyword/regex/if-else/enum/per-UC-matrix
additions; any finding whose fix would be such an addition is
deferred. Specifically:

- Track A's `prompt_projection` fix (if bundled) teaches the LLM
  about an observable slot in principled language and does not
  branch on tool name or UC.
- Track A's `infra` fix (if bundled) de-duplicates identical
  successful dispatches by `arguments_hash` — a deterministic
  identity check, not a semantic match.
- Track B's `infra` fix (if bundled) coalesces consecutive
  identical placeholder emissions within one session — a
  deterministic text-equality + state-tracking check, not a
  semantic match.

**Generalization coverage:** target = the case(s) the dev re-derived
into Track A's and Track B's target sets (counts populated in handoff
§4 root-cause matrix); neighbor = the rest of the smoke set + the
manual-probe case; negative = none in this sprint (deferred to G2
case-family iteration if the bundled fix lands); shadow = none in
this sprint (deferred to G2 case-family iteration). Explicit gap:
the visible cases are the eval bar; the human / review agent reads
shadow separately per §5.1 if a fix lands.

## 12. Bundle-or-defer summary (per-track)

| Track | Bundle gate | If gate fails |
|-------|-------------|---------------|
| A | Evidence conclusive for ONE root cause AND fix is safest narrow form | Investigation-only output: evidence table + R-items |
| B | Evidence shows root cause is purely user-facing repeated fallback messaging | Defer with model-latency follow-on R-item carrying latency data |

Bundle ≠ ship-at-any-cost. Investigation-only is an acceptable
outcome for either or both tracks.

## 13. Success metrics

### 13.1 Track A

- If bundled fix landed: dev's regression test demonstrates the
  narrow fix reverses the repeated-FAQ shape on at least one
  target case.
- Root-cause matrix is complete: every target case has a layer
  attribution + evidence excerpt.
- Follow-on R-items name remaining causes with supporting evidence.

### 13.2 Track B

- If bundled fix landed: dev's regression test demonstrates
  session-scope coalesce produces ONE placeholder + ONE honest
  next-step message across 2 consecutive deadline turns (NOT 2
  identical placeholders).
- Root-cause matrix distinguishes placeholder-loop cases from
  emits+recovers cases from `semantic_planner`-failure cases.
- If model-latency named as root cause, the deferred R-item
  carries supporting latency data.

### 13.3 Both tracks

- Full server suite green (Sprint 20 baseline 894/0/0/1).
- No eval-spec edits (verified by file-path check at handoff).
- No §1.7 violations (verified by handoff §7 anti-hardcode self-
  walk).
- No Tier-0 candidacy.
- No deadline-budget widening (verified by config-file check).
- Pass-rate is observed and reported but is NOT the acceptance bar
  per UX-over-pass-rate framing.

## 14. Deliverables

1. **Root-cause matrix** in handoff §3 or §4 covering both tracks:
   case_id × turn × LLM raw tool calls × dispatched tool calls ×
   projection contents × accumulated tool results × root-cause
   layer. Track A and Track B may have separate matrices.
2. **Minimal fix or fixes** only where evidence is conclusive per
   §12 bundle gate.
3. **Regression tests** for any shipped fix.
4. **Follow-on R-items** for any root causes not fixed in
   Sprint 23, with supporting evidence packaged in each R-item.
5. **Sprint 23 handoff** clearly separating **symptom**, **root
   cause**, **fixed layer**, and **deferred layer** per case.
6. **`docs/10-handoff.md`** refreshed with Sprint 23 lead.

## 15. Do not implement

- Symptom-only placeholder coalesce as the sprint deliverable.
- Eval-only bookkeeping.
- Pass-rate-optimization at the cost of UX.
- Deadline-budget widening (any change to deadline / timeout
  configuration).
- Eval-spec edits (CaseSpec, override, persona, judge rubric,
  case family).
- Case-list inheritance from prior summaries without independent
  re-derivation from the results JSON.
- Conflation of cs_040's placeholder loop with its UC-K→UC-C
  routing issue.
- Any new Tier-0 invariant.
- Any keyword/regex/per-UC matrix for a semantic decision.
- Any reassuring-filler honest-message text that masks slowness.

## 16. Review rule

The review agent (Codex) is allowed to flag:

- Missing root-cause matrix (blocking).
- Bundle-without-conclusive-evidence (blocking).
- Deadline-budget widening (blocking).
- Conflation of cs_040 placeholder + routing (blocking).
- §1.7 violations: rubric widen, reassuring-filler honest-message,
  prompt if-else block, new Tier-0, new semantic hardcode
  (blocking).
- Inherited case lists not independently re-derived from results
  JSON (blocking).
- Missing target / neighbor / negative / shadow generalization-
  coverage table per §5.1 (blocking for any landed fix; deferred-
  to-G2 acceptable for negative / shadow).

Codex is NOT allowed to flag as scope drift:

- Deliver-agent-owned files in the dev's commit (per
  `feedback_out_of_scope_review_packaging_rollforward.md`): the
  commit-at-end workflow accumulates `compact/sprint-*.md`,
  `docs/sprint_objective.md` appends, and
  `compact/sprint-deliver-orchestrator.md` in the working tree;
  these are path-based packaging artefacts, not behaviour drift.
  Codex notes them in the verdict for the historical record but
  does not block on them.

Out-of-scope concerns Codex identifies go to `docs/action_bank.md`
as deferred items, not blocker findings.

---

## Sprint 23 fix iteration

Date opened: 2026-05-14
Branch: `design-v1-without-human-review`
Parent close decision: `fix_required` (Codex review `docs/codex-findings.md` lines 1–4, three P1 blocking findings on Track A).
Sprint class: investigation+bundle fix iteration with a built-in downgrade branch; semantic-touching by inheritance. §7 stanza inherits the parent Sprint 23 stanza (lines 396–450); Track A's final layer is conditional on the rerun outcome (see "§7 stanza note" below). This is a fix iteration, not a new sprint; the per-case §3.2 walk does not apply (no new failure is being diagnosed — Track A's repeated-FAQ shape and its candidate root causes are settled at the parent objective).

### Status

Parent Sprint 23 (commit `39cb1b9`) shipped:

- The `already_called` system-prompt teaching paragraph in `server/src/main/resources/prompts/system_prompt.txt` (Track A `prompt_projection` bundle).
- `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java` (regression test asserting teaching presence + principled shape; not behaviour reversal).
- Track B as investigation-only (no bundle).

Codex re-review (`docs/codex-findings.md` lines 1–28) returned `fix_required / blocking_count: 3`:

- **Finding 1 (matrix columns, P1, lines 6–12):** Track A's root-cause matrix at `docs/sprints/sprint-023-handoff.md:346` and Track B's at `:532` record hypothesis labels and classifications but omit the required `turn`, raw LLM tool calls, dispatched calls, projection contents, and accumulated tool results columns mandated by the parent objective's review rule (parent objective §16).
- **Finding 2 (inferred-vs-conclusive evidence for Track A bundle, P1, lines 14–20):** `docs/sprints/sprint-023-handoff.md:273` admits argument-hash identity is inferred (no `ToolEvent` records in results.json), but `:326` later declares missing prompt consumption "conclusively confirmed" and `:384` bundles the prompt change. Per the parent objective's "Evidence conclusive for ONE root cause" gate (lines 226–229), conclusiveness is not established.
- **Finding 3 (regression evidence does not reverse target shape, P1, lines 22–28):** Parent objective §13.1 (lines 466–469) requires the bundled-fix regression test to demonstrate the narrow fix reverses the repeated-FAQ shape on at least one target case. The Java test only asserts text anchors and absence of tool/UC strings; the handoff §13.1 itself marks this **PARTIAL** at `docs/sprints/sprint-023-handoff.md:858–863`. No empirical target-reversal evidence has been produced.

Codex's non-blocking notes (lines 30–37) confirm: §4.1 anti-hardcode verdict is `approve`; no deadline-budget widening; no eval-spec edits; cs_040 conflation not blocking; full server suite 898/0/0/1.

### Sources

The fix dev reads in this order:

1. `docs/sprint_objective.md` — the parent objective (lines 1–556) and this fix-iteration append.
2. `docs/codex-findings.md` lines 1–28 — the three blocking findings, verbatim.
3. `docs/sprints/sprint-023-handoff.md` — parent dev's claims, specifically §3 / §3.1 / §3.2 / §3.4 / §5 / §11 / §13.1 (lines 256–426, 622–698, 854–871).
4. `server/src/main/resources/prompts/system_prompt.txt` lines 20–32 — the teaching paragraph the parent commit landed.
5. `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java` — the existing regression test (assertions to be reclassified as supporting coverage, not as primary evidence).
6. `eval_interactive/results/20260510-134558/results.json` — the original target-set source.
7. The trace JSON files under `eval_interactive/results/20260510-134558/` (per-session transcripts) — re-derive the matrix's observable columns where available.

### Implement only

1. **Augment the Track A root-cause matrix in `docs/sprints/sprint-023-handoff.md` §3.2 (currently at line 346)** with the six observable fields Codex requested in Finding 1, per target case:
   - `turn`
   - raw LLM tool calls (the tool_call entries emitted in that turn's LLM response, before dispatch)
   - dispatched tool calls (the actual dispatched ToolEvent records or their nearest available proxy)
   - projection contents (the `already_called` slot state at that turn, plus the projection-payload excerpt that fed the LLM)
   - `accumulated_tool_results` contents (the slot state at that turn)
   - argument hash / same-args status (per call: matches an `already_called` entry, or not)
   - For any column whose value is not directly recoverable from the trace files, the matrix cell must read `unavailable: <cause>` where `<cause>` names the specific reason (e.g. `unavailable: results.json snapshot lacks ToolEvent records`). No silent omissions.
2. **Augment the Track B root-cause matrix in `docs/sprints/sprint-023-handoff.md` §4.2 (currently at line 532)** with the same six fields per target case. Track B remains investigation-only; the augmentation is for Finding 1 coverage, not a new bundle.
3. **Run a one-shot target rerun** of the relevant duplicate-tool-call cases (or the smallest subset needed to show reversal) against the post-`39cb1b9` prompt. The rerun uses the real LLM under the harness's normal configuration; a mocked LLM is NOT acceptable as the evidence gate (see "Do not implement" below). Produce a new `eval_interactive/results/<timestamp>/results.json` directory.
4. **Branch on the rerun outcome:**
   - **PASS branch (reversal shown on ≥1 target):** update `docs/sprints/sprint-023-handoff.md` §11 (line 967) and §13.1 (line 854) status from PARTIAL to PASS; append a `## Fix iteration` section to `docs/sprints/sprint-023-handoff.md` documenting the augmented matrix + rerun command + results path + per-target reversal verdict.
   - **DOWNGRADE branch (no reversal on any target, OR rerun infeasible):** in this same fix commit, revert `server/src/main/resources/prompts/system_prompt.txt` to its `HEAD~1` state for the lines the Sprint 23 dev added (the teaching paragraph); delete `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java`; flag `R-already-called-prompt-consumption` to be re-opened as `proposed` in `docs/action_bank.md` (deliver agent applies the action_bank delta at close). Append a `## Fix iteration` section documenting the downgrade + the cause (rerun result or infeasibility reason).
5. **The action_bank disposition phrasing constraint** applies to both branches and is hard:
   - PASS branch: the R-item's eventual updated row reads "**landed with target-reversal evidence**" — NOT flat "done". The dev names this phrasing in its fix-handoff §11 deltas; the deliver agent applies it at close.
   - DOWNGRADE branch: the R-item's eventual updated row reads "re-opened as `proposed` after Sprint 23 fix iteration; bundle reverted, no target-reversal evidence". The dev names this phrasing in its fix-handoff §11 deltas.

### Do not implement

- **No mocked-LLM integration test as the primary causal proof for prompt-consumption reversal.** A mocked LLM controls the variable being measured; the test reduces to whether the mock bakes in the desired behaviour. A mocked-LLM integration test may be added as **supporting coverage only** and must be explicitly labeled "supporting coverage" in both the test file and the fix-handoff §5. The real-LLM target rerun is the evidence gate.
- No new Track A bundled fix beyond the existing prompt-teaching paragraph (or its revert on the downgrade branch).
- No Track B work of any kind (deferred per parent objective §7.4).
- No eval-spec edits (`eval_interactive/case_specs/**`, `eval_interactive/case_spec_overrides.yaml`, `eval_interactive/case_specs_shadow/**`, persona files, judge rubric).
- No Sprint 20 case-family edits.
- No foundational doc edits (`docs/foundational/**`).
- No governance doc edits (`docs/current/iteration_governance.md`, `doc_governance.md`, `agent_context_guide.md`).
- No sprint-archive edits (`docs/sprints/sprint-001-*.md` through `docs/sprints/sprint-022-*.md`).
- No edit to `docs/sprint_objective.md` (this file; deliver-agent owned).
- No deadline / timeout configuration edit.
- No model configuration edit.
- No new prompt edit beyond the revert-on-downgrade scenario.
- No claim of PASS without target-reversal evidence.
- No deferral of the downgrade decision to a follow-on sprint. The branch resolves in this same fix iteration.

### Target cases / regression coverage

- **Track A target set (parent handoff §3.1, lines 283–290):** cs_002, cs_014, cs_015, cs_040, cs_259, manual-probe. The dev picks the smallest subset that exhibits the duplicate `search_knowledge` shape and runs the harness rerun on at least one of them. The smallest defensible subset is one case; the dev may rerun more if a single rerun is inconclusive.
- **Matrix augmentation scope:** all six Track A target cases (matrix Finding 1 coverage) AND all seven Track B target cases listed at parent handoff §4.1 lines 440–448 (cs_002, cs_014, cs_040, cs_015, cs_038, cs_259, cs_176). The augmentation is a column extension on each target, not a re-derivation of the target set itself.
- **Regression evidence for the bundle (PASS branch):** the target rerun's `results.json` — specifically the tool-sequence on the rerun target case — is the primary evidence. The existing `AlreadyCalledPromptConsumptionTest` (text-anchor assertions) is supporting coverage only.
- **Regression evidence (DOWNGRADE branch):** the absence of reversal is itself the evidence; the dev includes the rerun command, the results.json path, and the per-target tool sequences showing the duplicate-call shape persists.

### Success metrics (the strict evidence gate)

- **Finding 1 closes** iff the augmented matrix in §3.2 / §4.2 carries all six observable columns OR `unavailable: <cause>` cells with a specific cause per missing column.
- **Finding 2 closes** iff:
  - PASS branch: the target rerun's results.json shows the duplicate-`search_knowledge` shape (≥2 same-args calls in the same session) reversed on at least one of the Track A target cases; the dev links the specific case_id + the before/after tool sequences.
  - DOWNGRADE branch: the prompt-teaching paragraph is reverted; `AlreadyCalledPromptConsumptionTest.java` is deleted; `R-already-called-prompt-consumption` is flagged for re-open as `proposed` in the fix-handoff §11 deltas (deliver agent applies at close).
- **Finding 3 closes** iff:
  - PASS branch: the regression evidence is the target rerun (named as primary), and the existing Java test is named as supporting coverage in the fix-handoff and (if retained) in a comment in the test file itself.
  - DOWNGRADE branch: the existing Java test is deleted; no PASS claim is made.
- **action_bank phrasing check** holds per the "Implement only" §5 phrasing constraint.
- **Full server suite green:** `mvn test` from `server/` returns 898/0/0/1 on the PASS branch (897/0/0/1 on the DOWNGRADE branch after the test deletion).
- **No §1.7 violations:** the fix iteration's anti-hardcode self-walk in the fix-handoff (one paragraph; not the full nine questions — those were walked at parent close) confirms no keyword/regex/per-UC matrix, no rubric widening, no honest-message reassuring-filler.

### Review rule

The fix reviewer (Codex) is allowed to flag as blocking:

- Matrix columns still missing or partially populated without explicit `unavailable: <cause>` cells (Finding 1 re-fail).
- PASS branch claimed without a target rerun results.json + per-target before/after tool-sequence diff (Finding 2 / Finding 3 re-fail).
- DOWNGRADE branch missing any of: prompt revert + test deletion + R-item re-open flag in §11 (Finding 2 / Finding 3 re-fail).
- **Use of a mocked-LLM integration test as the primary causal proof** (this fix iteration's hard fence; see "Do not implement").
- action_bank phrasing not matching the constraint (e.g. PASS branch uses "done" instead of "landed with target-reversal evidence").
- Any §1.7 violation, new Tier-0, eval-spec edit, deadline-budget widening, or governance-doc edit (inherited fences from parent objective §9).

The fix reviewer is NOT allowed to flag as scope drift:

- Deliver-agent-owned files bundled into the fix commit (`compact/sprint-023-fix-*.md`, `compact/sprint-deliver-orchestrator.md`, this `docs/sprint_objective.md` append) — these are path-based packaging artefacts per `feedback_out_of_scope_review_packaging_rollforward.md`. Codex notes them in the verdict for the historical record but does not block on them.

Out-of-scope concerns Codex identifies go to `docs/action_bank.md` as deferred items, not blocker findings.

### §7 stanza note

The fix iteration inherits the parent Sprint 23 stanza at lines 395–450 verbatim. Two clarifications:

- **No new Tier-0 invariant.** Parent stanza "Tier-0 invariant" line (lines 419–425) holds.
- **No new semantic hardcode.** Parent stanza "Semantic hardcode" line (lines 427–441) holds in both branches: PASS branch leaves the existing principled teaching paragraph in place (no UC/tool branching); DOWNGRADE branch reverts it. Neither branch introduces a hardcode.
- **Track A's final layer is conditional on the rerun outcome:**
  - PASS branch (reversal shown on ≥1 target): Track A resolves to `prompt_projection`. The bundle stands.
  - DOWNGRADE branch (no reversal OR rerun infeasible): Track A resolves to `proposed` (the R-item re-opens); the bundle is reverted. The parent stanza's `prompt_projection` candidate is rejected in this branch and the fix iteration is the rejection record.

### Deliverables

1. Augmented Track A matrix in `docs/sprints/sprint-023-handoff.md` §3.2 (six observable columns per target case).
2. Augmented Track B matrix in `docs/sprints/sprint-023-handoff.md` §4.2 (six observable columns per target case).
3. New `eval_interactive/results/<timestamp>/` directory containing the target-rerun results.json (both branches produce this; the PASS branch's contents differ from DOWNGRADE's only in the tool sequences).
4. **PASS branch additional deliverables:**
   - Updated §11 / §13.1 in handoff (PARTIAL → PASS, with the rerun-evidence cite).
   - `## Fix iteration` section appended to handoff with subsections: branch taken; augmented matrix summary; rerun command + results path; per-target reversal verdict; §11/§13.1 status update; action_bank disposition phrasing ("landed with target-reversal evidence"); mocked-LLM supporting-coverage status (if added).
5. **DOWNGRADE branch additional deliverables:**
   - Revert of the teaching paragraph in `server/src/main/resources/prompts/system_prompt.txt` (return to the pre-`39cb1b9` state for those lines).
   - Deletion of `server/src/test/java/com/gumtree/csagent/service/runtime/AlreadyCalledPromptConsumptionTest.java`.
   - `## Fix iteration` section appended to handoff with subsections: branch taken; augmented matrix summary; rerun command + results path; per-target failure verdict; revert + delete log; action_bank disposition phrasing ("re-opened as proposed after Sprint 23 fix iteration; bundle reverted, no target-reversal evidence"); cause of downgrade (no reversal vs infeasibility, with details).
