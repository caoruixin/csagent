---
title: Sprint 27 handoff — PhasePlan directive-shape probe (investigation-only)
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-05-15
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 27 dev-agent handoff. Investigation-only docs-only probe sprint
  triggered by the Sprint 27 planning-turn premise check on
  `R-prompt-phase-plan-directive-followship`
  (`docs/action_bank.md:450`). Sprint 27 enumerates the six
  `PhaseEvaluator.plan(...)` branches that emit
  `.systemInstruction(...)` string literals (lines 411 / 458 / 485 / 517
  / 564 / 616), identifies directives per branch, classifies each under
  the 4-question rubric, surveys two existing reference smoke runs
  (`20260514-111724/results.json` + `20260514-114628/results.json`),
  and lands on **recommendation (R2)** — open a targeted probe sprint
  follow-on. Of the 21 directives identified across the six branches,
  4 classify as `target` (event-shape precondition AND event-shape
  action AND `phase_plan` source); the remaining 17 classify as
  `content-shape-defer` (shape contains a content-shape conjunct).
  Across both reference runs, the LLM emits ZERO `request_handover`
  and ZERO `record_outcome` tool calls — so every target directive's
  precondition-and-fulfillment pair is unobservable in the existing
  smoke. §9.8 stop-condition's "valid finding" path fires; (R2) is the
  driver. No slot. No `PhasePlan` restructure. No probe scenarios
  authored. No code change. `R-prompt-phase-plan-directive-followship`
  at `docs/action_bank.md:450` is **updated** with the probe finding;
  it is **NOT** closed.
---

# Sprint 27 handoff — PhasePlan directive-shape probe

## 1. Context Pack

### 1.1 Relevant docs (sampled & read)

| path | tier (best guess) | status (best guess) | one-line relevance |
|------|-------------------|---------------------|--------------------|
| `docs/sprint_objective.md` | current-runtime | current | Sprint 27 authoritative scope; §"Implement only" 7-step procedure, §"Classification rubric" 4-question rubric, §"§1.7 hard gate", §"Stop conditions" §9.8 valid-finding path. |
| `docs/current/iteration_governance.md` §1 / §3 / §5 / §7 | durable-connective | current | Constitution (§1.3 LLM-owns / §1.7 forbidden), Fix Layer Classification (§3.2 question routing), Eval Acceptance Rules (§5; advisory only — Sprint 27 ships no eval delta), §7 stanza exemption clause cited for docs-only investigation sprints. |
| `docs/current/doc_governance.md` + `docs/current/agent_context_guide.md` | durable-connective | current | Tier model, front-matter schema, per-task reading lists; loaded transitively via `AGENTS.md`. |
| `docs/sprints/sprint-018-handoff.md` §5.2 / §8.7 / §8.8 | sprint-archive | historical | Method note + open-observation rule (n=1 evidence insufficient) + conditional-broadening rule (n=2 same-shape can broaden, n=3 cross-UC promotes); the rule that gates R-item promotion + the rule the Sprint 27 premise check inverts when it finds detection-axis fragmentation. |
| `docs/sprints/sprint-019-handoff.md` §3.3 / §3.5 | sprint-archive | historical | cs_011 T2 silence finding (bot promised handover then went silent — directive source = bot-content, not `phase_plan`) + cs_040 context. The instances cited in the R-item entry. |
| `docs/sprints/sprint-020-handoff.md` §5 | sprint-archive | historical | `already_called` soft-signal slot precedent — the shape a future structural `DirectiveSpec` slot would mirror (always-present projection field, hash-keyed event records). |
| `docs/sprints/sprint-025-handoff.md` §4 / §5 | sprint-archive | historical | Per-LLM-call `llm_calls` field schema (`callType` / `requestSummary` / `responseSummary` / `success` / `latencyMs`) + reference smoke `20260514-111724/results.json` as the canonical Sprint 25 reference. |
| `docs/sprints/sprint-026-handoff.md` §1 / §6 | sprint-archive | historical | Latency-decision sprint that landed on docs-only outcome; the close precedent Sprint 27 mirrors (no slot, recommendation-only). |
| `docs/action_bank.md` lines 440–466 (R-item entry + open-observation block) | durable-connective | current | The R-item entry to be updated (not closed) by Sprint 27. |

Sprint 21 – 24 archives were not loaded; not required by the probe.

### 1.2 Relevant code paths (verified at session start, 2026-05-15)

| path | lines | what it governs |
|------|------:|-----------------|
| `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java` | 21 / 30 | Record declaration; `String systemInstruction` field at line 30. Free-form natural-language string; no structured `DirectiveSpec`. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java` | 207–247 | `buildIntakeSystemInstruction(uc, ucDef)` — the dynamic builder called from the line-564 INTAKE branch. UC-dependent string. |
| `…/PhaseEvaluator.java` | 397–440 | DISCOVER branch; `.systemInstruction(...)` at line 411. |
| `…/PhaseEvaluator.java` | 445–471 | CONFIRM branch; `.systemInstruction(...)` at line 458. |
| `…/PhaseEvaluator.java` | 475–493 | CLOSE branch; `.systemInstruction(...)` at line 485. |
| `…/PhaseEvaluator.java` | 496–526 | ESCALATE branch; `.systemInstruction(...)` at line 517. |
| `…/PhaseEvaluator.java` | 535–579 | RESOLVE INTAKE branch; `.systemInstruction(buildIntakeSystemInstruction(activeUc, ucDef))` at line 564. |
| `…/PhaseEvaluator.java` | 601–648 | RESOLVE FAQ branch; `.systemInstruction(...)` at line 616. |
| `…/PhaseEvaluator.java` | 30 / 112 / 129 / 143 | `INTAKE_UCS = {UC-G, UC-H, UC-I, UC-J, UC-K}`, `UC_TEAM_NAME` map, `INTAKE_ESCALATION_TRIGGER` map, `intakeCompleteTrigger(activeUc)` helper used inside the INTAKE branch's directive D564.7. |
| `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java` | 785–793 | Projection site: `planNode.put("system_instruction", plan.systemInstruction())` at line 789 (sister grounding_instruction + escalation_policy in the same block). |
| `eval_interactive/results/20260514-111724/results.json` | — | Sprint 25 reference smoke. 14 cases, `llm_calls` present. |
| `eval_interactive/results/20260514-114628/results.json` | — | Sprint 26 close smoke. 14 cases, `llm_calls` present. |

### 1.3 Doc-status warnings (drift observed)

- The Sprint 27 planning-turn brief referenced
  `runtime/PhaseEvaluator.java` (top-level `runtime/`), but the actual
  path is `service/runtime/PhaseEvaluator.java` (the full package is
  `com.gumtree.csagent.service.runtime`). Premise §2.2 corrects this;
  no doc edit required because the sprint objective and the dev
  prompt §2 both carry the corrected path.
- Working-tree at session start carried pre-existing unrelated mods on
  `csagent_system_design_review.md` +
  `server/src/main/resources/prompts/system_prompt.txt` (the Sprint
  23 prompt-surface, inherited from Sprint 23) + untracked
  `csagent-solution-_20260514.md`. None are touched by this sprint
  per dev-prompt §10 hard-fence. The deliver-agent-owned files
  (`docs/sprint_objective.md` + `compact/sprint-027-*-prompt.md`) are
  the human's to manage at commit boundary per
  `feedback_commit_at_end_bundles_deliver_artefacts.md`.

### 1.4 Source-of-truth decision

For the directive-enumeration step, the source of truth is the
verbatim string-literal text in
`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
(branches 411 / 458 / 485 / 517 / 564 / 616) plus
`buildIntakeSystemInstruction(...)` at line 207. The text is what is
serialised into `phase_plan.system_instruction` at
`ContextProjectionBuilder.java:789` and seen by the LLM each turn.
Per `doc_governance.md` "code ahead of docs" rule, code is the source
of truth for delivered behaviour.

For the smoke-survey step, the source of truth is the
`case_results[].llm_calls[]` and `case_results[].failure_tags[]` and
`case_results[].transcript[]` arrays in
`eval_interactive/results/20260514-111724/results.json` and
`eval_interactive/results/20260514-114628/results.json`. The
`responseSummary` field of each `chat`-type `llm_call` carries the
LLM's structured JSON output, from which tool-call names are extracted
structurally (no natural-language content matching; §1.7 respected).

### 1.5 Implementation status

`implementation_status: implemented` — Sprint 27 ships exactly one
artefact (this handoff + a one-bullet update to the R-item at
`docs/action_bank.md:450`). No deferred work; no partial shipment;
no proposal restructured. The R-item itself remains open with an
updated disposition.

### 1.6 Risks before the analysis

- The R-item promotion logic (Sprint 18 §8.7 / §8.8) treats n=3
  cross-UC evidence as ship-ready for structural action. The premise
  check found those n=3 instances fragment across detection axes —
  only the manual-probe instance is fully event-shape detectable.
  The risk of opening (R1) on n=3 nominal evidence when only n=1 is
  detection-compatible is exactly what `feedback_cs_agent_posture.md`
  cautions against in a different domain (mechanical rule
  application). (R2) avoids this risk; (R1) would assume the n=3
  evidence and ship a slot.
- The smoke artefacts are 14 cases × 2 runs = 28 case-runs. Several
  target directives (T1 / T2 / T3 below) require either phase trace
  or projected `intake_state` slot values to confirm precondition-
  fires. Neither is in the present `results.json` shape (see
  `R-per-case-trace-dump-for-smoke-harness` at
  `docs/action_bank.md:448`).
- The buildIntakeSystemInstruction string is dynamically constructed
  per active UC. Per dev-prompt §3 table, the per-UC enumeration is
  done for at least one representative directive set; UC-specific
  text differences (team name, SLA placeholder, intake-complete
  trigger value) do not change directive shape. The directives are
  classified once for the INTAKE branch as a whole.

## 2. Sprint-objective recap

Sprint 27 is a docs-only investigation/probe sprint that probes
whether the n=3 evidence behind
`R-prompt-phase-plan-directive-followship` is large enough on the
detection axis (n≥2 fully-event-shape directives with observed
non-fulfillment) to justify opening a structural sprint that ships a
`pending_directive_unfulfilled` slot. The deliverable is a decision
document with an evidence table + a recommendation drawn from a
closed set (R1 open structural sprint / R2 targeted probe follow-on /
R3 defer further). No slot. No `PhasePlan` restructure. No probe
scenarios authored. No code change.
`R-prompt-phase-plan-directive-followship` at
`docs/action_bank.md:450` is updated with the probe disposition; it
is NOT closed.

## 3. Premise re-verification (each of §2's six points)

All six premise items in `compact/sprint-027-dev-prompt.md` §2 + the
sprint-objective `Premise carried forward` section were verified at
session start at SHA `1f4a1db` (`docs: close sprint 26 …`).

| # | premise | verification |
|---|---------|--------------|
| 1 | `PhasePlan.systemInstruction` at `model/PhasePlan.java:30`. | Read at `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java:30`. Confirmed: `public record PhasePlan(... String systemInstruction, ...)` — field declared at line 30 inside the record header. |
| 2 | Six `.systemInstruction(...)` string-literal emitters at `service/runtime/PhaseEvaluator.java` lines 411 / 458 / 485 / 517 / 564 / 616. | Read each anchor. All six exist at the cited line numbers. Path is `service/runtime/`, not top-level `runtime/`. |
| 3 | `phase_plan.system_instruction` projected to LLM at `service/runtime/ContextProjectionBuilder.java:788–789`. | Read; the projection site is `planNode.put("system_instruction", plan.systemInstruction())` at line 789 (guarded by an `!= null` check at line 788). |
| 4 | No `DirectiveSpec` surface exists today. | Grep for `DirectiveSpec` in `server/`: zero hits. Confirmed absent. |
| 5 | Sprint 25 `llm_calls[]` schema (id / sessionId / turnIndex / callType / model / promptTokens / completionTokens / latencyMs / requestSummary / responseSummary / success / errorMessage / createdAt). Two reference runs: `20260514-111724/results.json` + `20260514-114628/results.json`. | `jq '.case_results[0].llm_calls[0] \| keys'` on both runs returns the 13-key shape; both files exist with 14 `case_results`. |
| 6 | Detection-axis fragmentation of the 3 cited R-item instances: only manual-probe UC-A RESOLVE is fully event-shape detectable. | Carried forward from the planning turn per `compact/sprint-deliver-orchestrator.md`; not re-derived in this dev pass. |

No premise-drift stop condition fired. §9.1 not triggered.

## 4. Per-branch enumeration

Each subsection records the branch condition, the verbatim
`systemInstruction` string literal (constructed from the source by
concatenating the `+`-joined fragments), and an enumerated directive
list. A directive is a sentence or clause that prescribes a bot
action (MUST / SHOULD / MUST NOT / SHOULD NOT) keyed on a stated
condition. Directives are labelled `D<line>.<n>` for the evidence
table in §5.

### 4.1 Branch 411 — DISCOVER

**Branch condition:** `"DISCOVER".equals(phase)`. `activeUc` may be
null while still discovering.

**Verbatim `systemInstruction`** (joined string-literal at lines
411–431):

> You are in the DISCOVER phase. Your goal is to identify which Use
> Case applies to the customer. Look at the form context, candidate
> use cases, and conversation history. When the user's intent is
> clear (or you can infer it with a supporting detail), call
> classify_use_case with the matching use_case_id and a confidence
> in [0,1]. Otherwise ask one clear clarifying question, or escalate
> if the user's request is out of scope. Sprint 7 §I0 weak-candidate
> cue: when `candidate_use_cases` is empty or weak AND the form
> context is empty / UNKNOWN topic AND the current user message is
> clearly FAQ-shaped ("how do I X", "can I Y", "what items are
> allowed") OR is payment / sale-proceeds-shaped ("how do I receive
> payment", "how do I get paid when I sell", "how does payout
> work"), do NOT request_handover with `faq_miss_threshold_exceeded`
> after a single user turn. Instead, gather enough evidence to
> classify toward the right FAQ-path UC: call `search_knowledge`
> with the user's question as the query, then call
> `classify_use_case` with the most plausible UC (payment /
> sale-proceeds questions point to UC-F; how-to-post and general
> advertising questions to UC-B; messaging to UC-C; account / login
> to UC-D). Once classified, RESOLVE will run the grounded resolve
> sequence.

**Directives identified:**

- **D411.1** — "When the user's intent is clear (or you can infer
  it with a supporting detail), call `classify_use_case` with the
  matching use_case_id and a confidence in [0,1]."
- **D411.2** — "Otherwise ask one clear clarifying question, or
  escalate if the user's request is out of scope."
- **D411.3** — "Sprint 7 §I0 weak-candidate cue: when
  `candidate_use_cases` is empty or weak AND the form context is
  empty / UNKNOWN topic AND the current user message is clearly
  FAQ-shaped … OR is payment / sale-proceeds-shaped …, do NOT
  `request_handover` with `faq_miss_threshold_exceeded` after a
  single user turn."
- **D411.4** — "Instead, gather enough evidence to classify toward
  the right FAQ-path UC: call `search_knowledge` with the user's
  question as the query, then call `classify_use_case` with the
  most plausible UC (payment / sale-proceeds questions point to
  UC-F; how-to-post … to UC-B; messaging to UC-C; account / login
  to UC-D)."

### 4.2 Branch 458 — CONFIRM

**Branch condition:** `"CONFIRM".equals(phase)`. `activeUc` carried.

**Verbatim `systemInstruction`** (lines 458–464):

> You are in the CONFIRM phase. Interpret whether the user is
> satisfied with the prior answer. If satisfied (e.g., 'thanks',
> 'that helps', 'yes'), call record_outcome with
> outcome='RESOLVED'. If not satisfied (e.g., 'no', 'still not
> working', 'I need more help'), call request_handover with reason
> 'user_dissatisfied' OR transition back to RESOLVE if appropriate.

**Directives identified:**

- **D458.1** — "If satisfied (e.g., 'thanks', 'that helps', 'yes'),
  call `record_outcome` with outcome='RESOLVED'."
- **D458.2** — "If not satisfied (e.g., 'no', 'still not working',
  'I need more help'), call `request_handover` with reason
  'user_dissatisfied' OR transition back to RESOLVE if appropriate."
  (Side note: 'user_dissatisfied' is not in the canonical
  23-value `CANONICAL_ESCALATION_REASONS` set at
  `PhaseEvaluator.java:39–63`; it canonicalises to
  `'service_degraded'` per `canonicalize(...)`. Not a directive
  fault; flagged for cross-doc visibility.)

### 4.3 Branch 485 — CLOSE

**Branch condition:** `"CLOSE".equals(phase)`.

**Verbatim `systemInstruction`** (lines 485–487):

> You are in the CLOSE phase. Thank the user and confirm the
> outcome. Call record_outcome with the appropriate outcome if not
> already recorded.

**Directives identified:**

- **D485.1** — "Thank the user and confirm the outcome."
- **D485.2** — "Call `record_outcome` with the appropriate outcome
  if not already recorded." → **target candidate**.

### 4.4 Branch 517 — ESCALATE

**Branch condition:** `"ESCALATE".equals(phase)`. Allowed tools at
line 505: `request_handover`, `record_outcome` (the `LlmInvocation`
side; `create_case_controlled` is runtime-only per Codex 1.8).

**Verbatim `systemInstruction`** (lines 517–519):

> You are in the ESCALATE phase. Send a clear handover message and
> ensure request_handover has been called with an appropriate
> escalation_reason.

**Directives identified:**

- **D517.1** — "Send a clear handover message."
- **D517.2** — "Ensure `request_handover` has been called with an
  appropriate `escalation_reason`." → **target candidate**. Note:
  the ESCALATE phase is entered only AFTER a successful
  `request_handover` (per the runtime's phase transition contract),
  so this directive is largely tautological — by construction,
  reaching ESCALATE implies the call already happened. Recap
  shape; observable non-fulfillment is degenerate.

### 4.5 Branch 564 — RESOLVE INTAKE (dynamic — `buildIntakeSystemInstruction(activeUc, ucDef)`)

**Branch condition:** `"RESOLVE".equals(phase) && activeUc != null
&& INTAKE_UCS.contains(activeUc)` where
`INTAKE_UCS = {UC-G, UC-H, UC-I, UC-J, UC-K}`
(`PhaseEvaluator.java:30`). The string is built by
`buildIntakeSystemInstruction(...)` at line 207 using the active UC's
team name (UC-G Data Protection / UC-H Ad Support / UC-I Payments /
UC-J Trust & Safety / UC-K Technical Support — line 112) and
intake-complete trigger (`intake_complete_for_uc_{g,h,i,j,k}` — line
129).

**Verbatim `systemInstruction` (representative — UC-K example,
required fields non-empty):**

> You are a Gumtree customer support agent collecting intake
> information for [UC-K name]. Your role: (1) acknowledge the user's
> issue with empathy, (2) ask for any missing required details, (3)
> confirm the team handling this is Technical Support and SLA is
> 24-48 hours, (4) call request_handover when intake is complete.
> Do not attempt to resolve the issue yourself — you are an intake
> agent only. Read the projected `intake_state.fields_remaining`
> array — that is the canonical list of required fields not yet
> collected for UC-K (canonical required set: […]). Ask ONLY for
> the next field in `intake_state.fields_remaining`; do NOT repeat
> questions about fields already in `intake_state.fields_collected`.
> When `intake_state.fields_remaining` is empty AND
> `intake_state.intake_complete` is true, call `request_handover`
> with escalation_reason='intake_complete_for_uc_k' AND include the
> collected values under `arguments.intake_fields` (e.g.
> {"intake_fields": {"field_a": "value_a", ...}}). The runtime
> refuses an `intake_complete_for_*` handover when any required
> field is missing — it will downgrade the call and hint which
> fields are still needed.

The per-UC text differs only in (a) UC display name, (b) team name,
(c) intake-complete trigger value. Directive shape is identical
across UC-G through UC-K.

**Directives identified:**

- **D564.1** — "(1) acknowledge the user's issue with empathy."
- **D564.2** — "(2) ask for any missing required details."
- **D564.3** — "(3) confirm the team handling this is [Team] and
  SLA is 24-48 hours."
- **D564.4** — "(4) call `request_handover` when intake is
  complete." (subsumed by D564.7 below — same surface)
- **D564.5** — "Do not attempt to resolve the issue yourself — you
  are an intake agent only." (MUST NOT)
- **D564.6** — "Read the projected `intake_state.fields_remaining`
  array … Ask ONLY for the next field in
  `intake_state.fields_remaining`; do NOT repeat questions about
  fields already in `intake_state.fields_collected`."
- **D564.7** — "When `intake_state.fields_remaining` is empty AND
  `intake_state.intake_complete` is true, call `request_handover`
  with escalation_reason='intake_complete_for_uc_*' AND include the
  collected values under `arguments.intake_fields`." → **target
  candidate** (canonical formulation; subsumes D564.4).

### 4.6 Branch 616 — RESOLVE FAQ (non-intake)

**Branch condition:** `"RESOLVE".equals(phase) && activeUc != null
&& !INTAKE_UCS.contains(activeUc) && !"INTAKE".equals(ucDef.path())`
— i.e. UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP, UC-FP variants.

**Verbatim `systemInstruction`** (lines 616–624):

> You are a helpful Gumtree customer support agent. Resolve the
> user's issue using the provided tools. FAQ-path RESOLVE flow
> (S1): the intended terminal sequence is search_knowledge ->
> resolve_article -> grounded customer-facing answer (with a
> source_id citation) -> record_outcome. Only escalate via
> request_handover after a valid resolve attempt cannot complete
> (no viable hit, or resolve_article could not produce a grounded
> answer).

**Directives identified:**

- **D616.1** — "FAQ-path RESOLVE flow (S1): the intended terminal
  sequence is `search_knowledge` → `resolve_article` → grounded
  customer-facing answer (with a source_id citation) →
  `record_outcome`." (SHOULD — "intended")
- **D616.2** — "Only escalate via `request_handover` after a valid
  resolve attempt cannot complete (no viable hit, or
  `resolve_article` could not produce a grounded answer)." (MUST
  NOT until precondition) → **target candidate**.

(Reminder: directives in
`groundingInstruction` and `escalationPolicy` strings are explicitly
out of scope; this section enumerates `systemInstruction` only per
dev-prompt §3.)

## 5. Evidence table

One row per identified directive per branch. Q1 / Q2 / Q3 / Q4
answers in the four central columns. **Target rule:** Q1 = event
AND Q2 = event AND Q3 = phase_plan → `target`.

| branch | directive (label + verbatim shorthand) | Q1 precond | Q2 action | Q3 source | Q4 scope | observed_triggers | observed_non_fulfillments | classification |
|--------|----------------------------------------|------------|-----------|-----------|----------|-------------------|---------------------------|----------------|
| 411 | D411.1 — when intent clear → call classify_use_case | content (user-intent clarity) | event (call tool) | phase_plan | single-turn | n/a (not target) | n/a | content-shape-defer |
| 411 | D411.2 — otherwise ask clarifying / escalate if out-of-scope | content (intent unclear) | mixed (ask = content; escalate = event) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 411 | D411.3 — Sprint 7 §I0 weak-candidate cue: do NOT short-circuit handover when FAQ/payment-shaped after 1 turn | mixed (candidate_use_cases=event + form context=event + user-msg-shape=content) — dominant **content** | event (don't call request_handover with reason) | phase_plan | one-shot | n/a | n/a | content-shape-defer (conservative: user-msg-shape conjunct) |
| 411 | D411.4 — gather evidence, call search_knowledge then classify_use_case with mapped UC | content (intent-class disambiguation) | event (call tools) with content-derived args | phase_plan | one-shot | n/a | n/a | content-shape-defer |
| 458 | D458.1 — if satisfied → record_outcome RESOLVED | content (user satisfied; keyword examples) | event (call tool) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 458 | D458.2 — if not satisfied → request_handover('user_dissatisfied') OR back to RESOLVE | content (user not satisfied) | event (call tool / transition) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 485 | D485.1 — thank user & confirm outcome | event (in CLOSE phase) | content (wording) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 485 | **D485.2 — call record_outcome if not already recorded** | event (in CLOSE, not-already-recorded is observable) | event (call tool) | phase_plan | one-shot | 0 (CLOSE phase entry not projected in `results.json`; see §5.x note below) | 0 (no LLM-emitted `record_outcome` across either smoke run — see extraction A and B below) | **target** |
| 517 | D517.1 — send clear handover message | event (in ESCALATE phase) | content (wording) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 517 | **D517.2 — ensure request_handover called with appropriate reason** | event (in ESCALATE phase) | event (verify/call tool) | phase_plan | single-turn | 0 (ESCALATE phase entry not projected; tautological recap — see §5.x note) | 0 (zero LLM-emitted `request_handover` across either smoke run — extraction A and B below) | **target** (tautology recap; degenerate observability) |
| 564 | D564.1 — acknowledge with empathy | event (in INTAKE) | content (wording) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 564 | D564.2 — ask for missing required details | event (intake_state slot) | content (ask wording) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 564 | D564.3 — confirm team handling + SLA | event (in INTAKE) | content (wording) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 564 | D564.4 — call request_handover when intake complete (general formulation) | event (intake_state.intake_complete) | event (call tool) | phase_plan | one-shot | subsumed by D564.7 | subsumed by D564.7 | merged into D564.7 |
| 564 | D564.5 — do NOT attempt to resolve yourself (intake agent only) | event (in INTAKE) | mixed (don't say substantive answer = content; don't call resolve_article = event — runtime L1 `intake_no_knowledge_tool` already enforces the tool side) | phase_plan | persistent | n/a | n/a | content-shape-defer (conservative; ambiguous action) |
| 564 | D564.6 — read intake_state.fields_remaining; ask only next; don't repeat | event (intake_state slot) | content (ask wording) | phase_plan | single-turn | n/a | n/a | content-shape-defer |
| 564 | **D564.7 — when intake_state.intake_complete=true, call request_handover w/ intake_complete_for_uc_*** | event (intake_state slot values) | event (call tool with structured args) | phase_plan | one-shot | 0 (intake_state slot not projected to `results.json`; no case in either run reached confirmed intake-complete — see §5.x note) | 0 (zero `request_handover` emissions of any reason — extraction A and B below) | **target** |
| 616 | D616.1 — intended terminal sequence: search_knowledge → resolve_article → grounded answer (cite source_id) → record_outcome | event (in RESOLVE FAQ) | mixed (3 tool-call steps + 1 content "grounded answer" step) | phase_plan | persistent | n/a | n/a | content-shape-defer (conservative; "grounded answer w/ source_id citation" is non-trivial content step) |
| 616 | **D616.2 — only escalate via request_handover after valid resolve attempt cannot complete** | event (tool-result state: no viable hit OR resolve_article failed) | event (don't call tool until precondition) | phase_plan | single-turn | 0 (no LLM-emitted `request_handover` across either smoke run; can't observe premature escalation — extraction A and B below) | 0 | **target** |

**Targets identified:** D485.2, D517.2, D564.7, D616.2 — n=4.

**Target rows with observed non-fulfillment > 0:** n=0.

### 5.x Extraction methods (every cited count reproducible)

All counts in the evidence table cite the two reference smoke
artefacts. Extractions performed at session start, 2026-05-15.

**Extraction A — total `request_handover` and `record_outcome`
emissions across all LLM `chat` calls** (used by all four target
rows for `observed_non_fulfillments`):

```bash
jq -r '[.case_results[].llm_calls[]
        | select(.callType == "chat")
        | .responseSummary | fromjson?
        | .tool_calls // []
        | .[].name]
       | map(select(. == "request_handover" or . == "record_outcome"))
       | length' \
  eval_interactive/results/20260514-111724/results.json \
  eval_interactive/results/20260514-114628/results.json
```

Literal output:

```
0
0
```

Method: structural extraction of `tool_calls[].name` from the LLM
JSON output. `responseSummary` is parsed as JSON; no
natural-language matching against user-message or bot-response
phrasing. §1.7 hard gate respected.

**Extraction B — full per-case tool-name distribution** (used to
confirm that no `request_handover` / `record_outcome` emission was
missed by an aggregate query):

```bash
jq -r '[.case_results[].llm_calls[]
        | select(.callType == "chat")
        | .responseSummary | fromjson?
        | .tool_calls // []
        | .[].name]
       | group_by(.)
       | map({name: .[0], count: length})' \
  eval_interactive/results/20260514-111724/results.json
```

Literal output (Sprint 25 reference smoke):

```json
[
  {"name": "classify_use_case",   "count":  1},
  {"name": "get_customer_context", "count":  1},
  {"name": "resolve_article",      "count": 16},
  {"name": "search_knowledge",     "count": 20}
]
```

Same query against `20260514-114628/results.json` (Sprint 26 close
smoke):

```json
[
  {"name": "classify_use_case",   "count":  1},
  {"name": "get_customer_context", "count":  1},
  {"name": "resolve_article",      "count": 10},
  {"name": "search_knowledge",     "count": 27}
]
```

Across both runs, `request_handover` and `record_outcome` appear
**zero times** in the LLM's emitted `tool_calls`. Aggregation level:
per-LLM-call, summed across all 14 cases per run.

**Extraction C — intake-UC case spot-checks (used by D564.7
`observed_triggers=0` note)**:

```bash
jq -c '.case_results[]
       | select(.case_id == "cs_interactive_036")
       | .llm_calls | map(select(.callType == "chat")
       | {turnIndex, responseSummary: (.responseSummary | fromjson?)})' \
  eval_interactive/results/20260514-111724/results.json
```

Literal output (UC-I, expected_outcome=escalate, run A):

```json
[{"turnIndex":1,"responseSummary":{"user_message":"…",
  "reasoning":"Acknowledged issue, dispute reason is clear…
  Need transaction_reference to complete intake. Asking for it.",
  "tool_calls":[]}}]
```

The LLM's own `reasoning` line confirms the bot was still in
intake-collection (not at intake-complete). Manual eyeball over
cs_036 / cs_038 / cs_066 (the 3 intake-UC × 2-run = 6 case-runs)
shows every intake-UC case-run ended with the LLM still asking for
required fields; none reached `intake_state.intake_complete=true` in
the projected runtime, and consequently the D564.7 precondition
never fired in any of the 6 intake case-runs. Aggregation level:
per-case, n=6 case-runs inspected by hand. (Note: `reasoning` is
read for context only, NOT matched against keywords — its presence
or absence does not change the count. The count is driven by the
`tool_calls[]` array shape — empty across the 6 intake case-runs.)

**Phase-trace / `intake_state` slot non-availability note** (used by
all four target rows for `observed_triggers`): the present
`results.json` shape carries no per-turn `phase_plan` snapshot, no
per-turn `intake_state` projection, and no per-turn `phase`
transition log. The transcript's `source` field surfaces only
`form_context`, `session_create`, and `null` (per
`jq '[.case_results[].transcript[].source] | group_by(.) | map({...})'`
on both runs). `R-per-case-trace-dump-for-smoke-harness` at
`docs/action_bank.md:448` covers re-enabling per-case trace dumping;
until it lands, target-directive precondition firing cannot be
confirmed from `results.json` alone for any directive whose
precondition depends on phase entry or projected slot value. This
is the §9.8 valid-finding shape.

## 6. Recommendation — **(R2) Targeted probe sprint follow-on**

**Distribution driving the choice:** 4 target directives identified
across the 6 branches (D485.2 / D517.2 / D564.7 / D616.2). For each
target, both the trigger count and the non-fulfillment count are 0
in the present reference smoke artefacts. The non-fulfillment count
is 0 not because the directives are reliably followed in the smoke
runs, but because the LLM never emits the terminal event-shape
tools (`request_handover` and `record_outcome`) the target
directives' required-action shape names — 0 emissions across 67 +
71 = 138 chat calls in the two reference runs combined. The
trigger count is 0 because the present `results.json` shape does
not project the phase trace or `intake_state` slot needed to
confirm precondition firing.

(R1) requires n≥2 target directives with observed non-fulfillment.
We have 4 target directives but 0 observed non-fulfillment. (R1) is
not supported by the evidence.

(R3) requires the population to be structurally incompatible with
event-shape detection across enough branches that a slot would have
too few ship-ready targets. The population IS structurally
compatible — 4 of 21 directives are target-shape. The blocking
issue is not structural incompatibility; it is detection capability
(no phase trace, no `intake_state` projection in `results.json`)
combined with insufficient case coverage of the target preconditions
in the present smoke (intake-UC cases don't reach intake-complete in
3–5 turns; non-intake-UC cases don't emit `request_handover` at the
LLM level). (R3) is not the right fit.

**(R2) is the recommendation.** A follow-on targeted probe sprint
should:

1. Author N≥3 CaseSpecs that demonstrably exercise the target
   directives' preconditions and observe LLM follow-through. Suggested
   minimal coverage: 1–2 cases per target-able directive (skipping
   D517.2 ESCALATE which is a tautology recap):
   - 1–2 cases for **D485.2** (CLOSE record_outcome) — a turn
     sequence that reaches CLOSE phase with no prior `record_outcome`,
     observing whether the LLM emits the call.
   - 1–2 cases for **D564.7** (INTAKE intake-complete handover) — an
     intake UC where the user supplies all required fields in
     2–3 turns, observing whether the LLM emits `request_handover`
     with `intake_complete_for_uc_*` and the structured
     `arguments.intake_fields` payload.
   - 1–2 cases for **D616.2** (RESOLVE FAQ premature-escalation
     prohibition) — a non-intake UC where `search_knowledge` returns
     a viable hit AND the user expresses mild frustration that
     would tempt premature escalation. Observe whether the LLM
     respects the "only escalate after valid resolve attempt cannot
     complete" gate.
2. Treat **D517.2** (ESCALATE ensure-handover) as a recap; the
   follow-on sprint should NOT author probe cases for it because
   ESCALATE phase is entered only after a successful
   `request_handover`. The directive is structurally degenerate.
3. Depend on `R-per-case-trace-dump-for-smoke-harness` at
   `docs/action_bank.md:448` landing first. Without per-turn phase
   trace + `intake_state` projection in `results.json`, the
   precondition-fires axis cannot be confirmed even with authored
   cases. The follow-on probe sprint either (a) waits for the
   trace-dump R-item to land, or (b) scopes its evidence-gathering
   to whatever post-hoc inference can be done from
   `responseSummary.reasoning` text (which is read for context per
   §1.7 but not matched against keywords — a brittle path).
4. **Not** ship a `DirectiveSpec` slot. The follow-on probe is
   evidence-gathering, not structural. If the follow-on probe lands
   ≥2 target directives with confirmed observed non-fulfillment,
   THAT sprint (or the next one after it) would be the (R1)
   trigger.

`R-prompt-phase-plan-directive-followship` at
`docs/action_bank.md:450` is updated with this finding; it is NOT
closed. The R-item disposition becomes "open, awaiting (R2)
follow-on probe + `R-per-case-trace-dump-for-smoke-harness`
dependency".

## 7. Proposed follow-on R-items (named only — NOT opened in `action_bank.md`)

For each non-target directive (or non-target-able subset), Sprint 27
proposes a follow-on R-item name and a one-line scope. These are
**proposals** for the human or a future planning turn to consider;
the dev does NOT open them in `docs/action_bank.md` this sprint.

| proposed R-item name | scope (one line) | shape behind it |
|---------------------|------------------|-----------------|
| `R-discover-weak-candidate-cue-soft-signal` | Replace D411.3 / D411.4 user-message-shape detection (`FAQ-shaped` / `payment-shaped`) with an LLM-projected soft signal that flags candidate UC sets per turn, removing the keyword-shaped precondition from `phase_plan.systemInstruction`. | D411.3 + D411.4 |
| `R-discover-classify-or-clarify-projection` | Replace D411.1 / D411.2 user-intent-clarity content precondition with a projected confidence signal (e.g. `candidate_use_cases[*].confidence`), so the LLM can choose `classify_use_case` vs clarify-question based on observable scoring instead of self-judged content clarity. | D411.1 + D411.2 |
| `R-confirm-sentiment-projection` | Replace D458.1 / D458.2 user-satisfaction content precondition with a projected sentiment signal so the LLM has explicit input before choosing `record_outcome` RESOLVED vs `request_handover` `user_dissatisfied`. Also reconcile `'user_dissatisfied'` → `'service_degraded'` canonicalisation surface (currently hidden in `canonicalize(...)`). | D458.1 + D458.2 |
| `R-close-phase-completion-checklist-projection` | Surface a `record_outcome_recorded={true,false}` slot in the projection so the D485.2 directive's "if not already recorded" precondition becomes a structurally observable bot-side check, not an LLM self-assessment. | D485.1 + D485.2 (sibling — content companion deferred) |
| `R-intake-instruction-decomposition` | Decompose D564.1 / D564.2 / D564.3 / D564.5 / D564.6 from a single free-form prose paragraph into per-clause prompt fragments + paired structured projection slots (per-clause status / per-field state). The current paragraph mixes content-shape (empathy wording, team-name confirmation) with event-shape gates (intake_state slot reads) at the same syntactic level. | D564.1 / .2 / .3 / .5 / .6 |
| `R-resolve-faq-terminal-sequence-projection` | D616.1 SHOULD sequence (`search_knowledge → resolve_article → grounded answer → record_outcome`) is currently a free-form prose hint. Surface a `terminal_sequence_position` slot (e.g. `awaiting_search_knowledge` / `awaiting_resolve_article` / `awaiting_grounded_answer` / `awaiting_record_outcome`) so the LLM has explicit projection of where it is in the sequence and what step is next. | D616.1 |

(These are scope sketches only. Each would require its own §3.2 layer
classification + §7 stanza walk before promotion to an opened R-item.)

## 8. Anti-hardcode self-walk (§4.1; exemption declared)

**Exemption declaration (line 1).** Sprint 27 is a pure
**docs-only investigation / probe** sprint per the sprint
objective's "Sprint class" section. No `.java`, `.py`, `.yml`,
`.yaml`, `.properties` change. No prompt-surface change. No
eval-spec / case-family / override / persona / judge edit. No
foundational / governance / sprint-archive edit. The diff stages
exactly `docs/sprints/sprint-027-handoff.md` (NEW) + a
~3-line append on `docs/action_bank.md` line 450. Per §4.1: "pure
infra, docs-only, config-governance, and characterization-test PRs
are not subject to this review … return `approve` with a one-line
note naming the exemption". Verdict for Codex (if dispatched):
`approve — exemption: docs-only investigation/probe sprint, no
semantic surface touched`.

For traceability, the nine §4.1 questions answered briefly:

1. **Adds keyword / regex / if-else / enum / per-UC matrix for a
   semantic decision?** — No. The diff adds two doc files; no code.
2. **Justified by Tier-0 invariant?** — N/A. No Tier-0 added.
3. **Soft signal alternative considered?** — Yes; §7 proposes 6
   follow-on R-items that lean on projected soft signals over
   hardcoded matchers. (R2) itself defers structural action.
4. **Visible-eval / trace phrasing / CaseSpec id encoded?** — No.
   No CaseSpec text quoted in code. The handoff cites
   `cs_interactive_*` IDs for cross-doc reference only; no rubric or
   override edit.
5. **Moves semantic ownership from LLM to Java?** — No. The
   recommendation is to defer structural action; it explicitly does
   NOT introduce a Java enforcement of any phase-plan directive.
6. **Adds if-else in prompt?** — No. No prompt-surface change.
7. **Preserves tool schema / capability / PII floor / grounding
   floor?** — Yes. No tool schema, capability, PII, or grounding
   surface touched.
8. **Generalization eval coverage shipped?** — Not applicable.
   Investigation sprint; the recommendation is precisely that
   generalization coverage NEEDS to be authored (follow-on probe).
9. **Temporary measure with sunset?** — Not applicable. No code
   change; nothing to sunset.

## 9. Files changed (Sprint 27 diff scope)

| path | change type | one-line description |
|------|-------------|----------------------|
| `docs/sprints/sprint-027-handoff.md` | NEW | This file. Sprint 27 dev-agent handoff (12 sections per dev-prompt §11). |
| `docs/action_bank.md` | EDIT (line 450 entry only) | Append "Sprint 27 probe: (R2) targeted probe follow-on" to the existing `R-prompt-phase-plan-directive-followship` row. R-item NOT closed. |

Out-of-scope hard-fence (per dev-prompt §10):

- No `.java` / `.py` / `.ts` / `.tsx` / `.yml` / `.yaml` /
  `.properties` under `server/` / `ui/` /
  `eval_interactive/eval_interactive/`.
- No `PhasePlan` / `PhaseEvaluator` / `ContextProjectionBuilder` edit.
- No `DirectiveSpec` class creation.
- No CaseSpec / override / persona / case-family / judge-rubric
  edit.
- No `docs/foundational/**` edit.
- No governance-doc edit.
- No sprint-archive edit (`docs/sprints/sprint-001-*` through
  `sprint-026-*.md`).
- No Tier-0 / latency / budget / model-config edit.
- `R-prompt-phase-plan-directive-followship` NOT closed.

Pre-existing untouched working-tree mods (managed by the human, not
this dev): `csagent_system_design_review.md`,
`server/src/main/resources/prompts/system_prompt.txt`, untracked
`csagent-solution-_20260514.md`, plus deliver-agent-owned
`docs/sprint_objective.md` and `compact/sprint-027-*-prompt.md`. None
are staged by this dev.

## 10. Layer-classification self-walk (§3; exempt; layers named)

Sprint 27 is docs-only investigation; §3 layer classification of the
sprint itself is **exempt** per `iteration_governance.md` §7. No
layer is targeted by this sprint's diff (no semantic surface
touched).

For traceability, the layers the **follow-on probe sprint** would
target if (R2) is taken:

- **`eval_spec`** layer — the (R2) follow-on authors targeted
  CaseSpecs (`eval_interactive/case_specs/**`). Per §3.2 Q6, this is
  the eval-spec layer (the eval is being asked to exercise behaviour
  the runtime can and should perform, not to validate behaviour the
  runtime is incapable of).
- **`infra` / eval-harness** layer — `R-per-case-trace-dump-for-
  smoke-harness` (`docs/action_bank.md:448`) is the prerequisite for
  reliable trigger detection. Per §3.2 Q1, this is the eval-harness
  infra layer.
- **`prompt_projection`** layer — IF the follow-on probe surfaces
  observed non-fulfillment ≥ 2, the **next** structural sprint
  would target `prompt_projection` (surface a
  `pending_directive_unfulfilled` soft signal). Per §3.2 Q3, this
  is the correct layer for projection / context handed to the LLM.
  Sprint 27 itself does NOT propose a Java check; the soft-signal
  shape mirrors the Sprint 20 `already_called` precedent.

No layer is `java_guard`. No Tier-0 invariant is named.

## 11. Open questions for the human

1. **Cadence of (R2) follow-on.** The (R2) probe is gated on
   `R-per-case-trace-dump-for-smoke-harness` (line 448) landing
   first. If trace-dump is not on a near-term sprint, should (R2)
   wait, or should the follow-on probe scope a smaller evidence-
   gathering approach (per-case manual eyeball of
   `responseSummary.reasoning` lines, accepting brittle inference)?
2. **Treatment of D411.3 / D411.4 (Sprint 7 §I0 weak-candidate
   cue).** These two directives' precondition relies explicitly on
   user-message-shape matching ("FAQ-shaped" / "payment-shaped").
   They live in the `phase_plan.systemInstruction` text today; the
   §1.7 forbidden-list prohibits keyword / regex routing in code,
   but the same text in a prompt arguably also encodes a soft
   matcher the LLM is asked to apply. Is this a §1.7 violation in
   the current prompt, or is "in the prompt, soft-matched by LLM"
   acceptable? `R-discover-weak-candidate-cue-soft-signal` (§7
   proposed) would resolve by projecting a soft signal instead.
3. **D517.2 tautology.** Should the ESCALATE branch's
   `systemInstruction` "ensure request_handover has been called"
   line be reworded (since by construction it has)? Probably a
   `R-resolve-escalate-instruction-cleanup` low-priority item; not
   opened here.
4. **`'user_dissatisfied'` non-canonical reason at D458.2.** The
   string literal at line 463 names `'user_dissatisfied'`, which is
   not in the canonical 23-value `escalation_reason` enum; it
   canonicalises to `'service_degraded'`. Probably benign (the
   canonicalisation closes the L1 trace contract) but flagged for
   visibility in case the human wants the prompt to name a
   canonical reason explicitly.
5. **Premise §2.6 carry-forward.** Sprint 27 dev did not re-derive
   the detection-axis fragmentation of the three cited R-item
   instances (cs_259 / manual-probe / cs_011 T2); it carried the
   planning-turn finding forward per dev-prompt §2 "do NOT
   re-verify". The handoff's recommendation does not pivot on this
   finding (the 0-target-non-fulfillment finding from the smoke
   survey is sufficient evidence for (R2) independently). Still
   noting for the close-turn reviewer.

## 12. Closure verdict (filled at sprint close, 2026-05-15)

| field | value |
|-------|-------|
| status | **PASS.** Decision document shipped (this handoff). Recommendation (R2) targeted probe sprint follow-on made on the basis of the 4-of-21 target-directive distribution + the 0-target-non-fulfillment finding across 138 chat calls in the two reference smoke runs. No code, config, prompt, eval-spec, judge, CaseSpec, override, or Tier-0 change. Codex sprint-close review **intentionally skipped** at human discretion 2026-05-15 per `docs/current/iteration_governance.md` §4.1 exemption clause ("pure infra, docs-only, config-governance, and characterization-test PRs are not subject to this review … return `approve` with a one-line note naming the exemption") — the close commit ships only docs (this handoff archive + the sprint_objective archive + the line-450 action_bank entry update + a 10-handoff §1 refresh + the deliver-agent prompt rollforward). Precedent: Sprint 26 close `1f4a1db` (2026-05-15, A-with-Codex-skipped first instance). Pattern documented at `.claude/agent-memory/sprint-deliver-orchestrator/feedback_close_with_codex_skipped_docs_only_outcome.md`. |
| classification | **A-with-Codex-skipped.** Distinct from Sprint 18 G1 (Codex-skip planned at sprint open as packaging-workflow choice — no dev agent, no Codex round, deliver agent + human as authors-and-reviewers from the start) — Sprint 27's Codex-skip is **human-discretion at close, AFTER the dev landed on a docs-only outcome**, exactly mirroring Sprint 26's variant. Distinct from Sprint 20's A-with-packaging-note (Codex DID run, single `out_of_scope_review` blocker on commit-boundary grounds). Distinct from Sprint 21's A-with-evidence-gap-acknowledgment (Codex DID run, `fix_required` typographical-fidelity blockers human-accepted as close-eligible). |
| Codex outcome | **Intentionally skipped per human direction 2026-05-15** ("I think there's no need to review and we could push forward to the next step"). The §4.1 verdict that would have been returned is `approve (exemption: docs-only investigation/probe sprint, no semantic surface touched)` — see §8 line 1 of this handoff for the dev's own anti-hardcode self-walk arriving at the same verdict. **No `docs/sprints/sprint-027-codex-review.md` archive exists.** This is intentional, not an oversight. |
| R-item disposition applied | `R-prompt-phase-plan-directive-followship` at `docs/action_bank.md:450` **updated** with the Sprint 27 probe finding (recommendation (R2) + target-directive list D485.2 / D517.2 / D564.7 / D616.2 + 0-observed-non-fulfillment finding + (R1) trigger requirement + trace-dump dependency); **NOT closed.** R-item disposition becomes: open, awaiting (a) `R-per-case-trace-dump-for-smoke-harness` (`docs/action_bank.md:448`) landing first AND (b) a targeted (R2) probe sprint authoring N≥3 CaseSpecs exercising D485.2 / D564.7 / D616.2 preconditions with confirmed observed non-fulfillment ≥ 2 (the (R1) structural-sprint trigger). Re-open trigger to revisit: (i) (R2) probe lands and surfaces ≥ 2 target directives with confirmed non-fulfillment (becomes (R1) trigger; opens a structural `pending_directive_unfulfilled` slot sprint), OR (ii) explicit later human direction to revisit the disposition. The 6 follow-on R-items proposed in §7 of this handoff (`R-discover-weak-candidate-cue-soft-signal` etc.) are **named only, NOT opened** in `docs/action_bank.md`; opening any of them is a separate planning decision that requires the per-R-item §3.2 layer walk + §7 stanza walk. |
| date | 2026-05-15 |
