---
title: Sprint 27 — PhasePlan directive-shape probe (investigation-only, no slot)
doc_tier: sprint-archive
status: archived
implementation_status: historical
source_of_truth: this file (archived sprint objective; live tree at close was promoted from docs/sprint_objective.md to docs/sprints/sprint-027-objective.md on 2026-05-15)
last_reviewed: 2026-05-15
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Sprint 27 is an investigation-only docs-only probe sprint triggered by
  the Sprint 27 planning-turn premise check on
  `R-prompt-phase-plan-directive-followship` (`docs/action_bank.md:450`).
  The R-item was promoted in Sprint 19 close on n=3 evidence
  (cs_259 + manual-probe + cs_011 T2). The premise check found the three
  cited instances fragment on the detection axis — only the manual-probe
  UC-A RESOLVE instance is fully event-shape detectable; cs_259 has a
  content-shape precondition; cs_011 T2's directive source is bot-content
  (not `phase_plan.systemInstruction`). Per the conditional-broadening
  rule (`docs/sprints/sprint-018-handoff.md` §8.8; n=1 evidence
  insufficient to ship structural action), Sprint 27 does NOT ship a
  slot, does NOT restructure `PhasePlan`, does NOT author probe scenarios.
  Sprint 27 enumerates the six `PhaseEvaluator.plan(...)` branches that
  carry a `systemInstruction` string literal, classifies each identified
  directive by detection shape under a 4-question rubric, surveys
  existing smoke `results.json` artefacts for observed non-fulfillment
  instances, and produces an evidence table + a recommendation
  (R1 / R2 / R3) about what should happen next. The R-item at line 450
  is NOT closed by this sprint; its disposition is updated based on the
  probe findings.
---

# Sprint 27 — PhasePlan directive-shape probe

## Goal

Probe existing smoke artefacts and the six
`server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
branches that emit a `PhasePlan.systemInstruction` string literal, in order
to determine whether the population of fully-event-shape directives is
large enough (n≥2 with observed non-fulfillment) to justify opening a
structural sprint that ships a `pending_directive_unfulfilled` slot.

The deliverable is a **decision document** (handoff) carrying an evidence
table + a recommendation drawn from a closed set (R1 / R2 / R3). No
slot. No `PhasePlan` restructure. No probe scenarios authored. No code
change. Update `R-prompt-phase-plan-directive-followship` at
`docs/action_bank.md:450` with the probe disposition; do NOT close it.

## Sprint class

**Docs-only investigation / probe.** Stanza-exempt under
`docs/current/iteration_governance.md` §7 ("Pure infra, docs-only,
config-governance, and characterization-test sprints are **exempt**
and need not include the stanza."). Precedent: Sprint 15 (config
governance), Sprint 16 (docs + characterization tests), Sprint 17 (G0
docs-only governance bundle), Sprint 18 (G1 brief authoring), Sprint 22
(phase 2 line 358 reconciliation), Sprint 26 (latency-decision sprint
that landed on docs-only outcome). See the
"Layer-classification + anti-hardcode stanza" section below for the
verbatim exemption declaration.

## Baseline

This is an investigation-only docs-only sprint. No eval baseline applies
to its acceptance gate. The probe READS existing eval artefacts
(`eval_interactive/results/**/results.json`) as evidence sources;
`docs/current_eval_baseline.md` is not consulted by this sprint and
must not be edited.

## Premise carried forward (verified in Sprint 27 planning turn)

Verified 2026-05-15. Do NOT re-verify; treat as fact for this sprint:

1. `PhasePlan.systemInstruction` is a hand-written natural-language
   string field at
   `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java:30`.
2. Six `PhaseEvaluator.plan(...)` branches emit `.systemInstruction(...)`
   string literals at
   `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
   lines **411**, **458**, **485**, **517**, **564**, **616** (the
   `service/runtime/` path, NOT a top-level `runtime/` path; corrected
   from the planning-turn brief).
3. The `phase_plan.system_instruction` value is projected to the LLM at
   `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:788–789`.
4. No structured `DirectiveSpec` surface exists today. Directives, when
   present, live as free-form natural-language sentences inside the
   string literal.
5. Sprint 25's `llm_calls` array on each `case_result` in
   `eval_interactive/results/**/results.json` is per-LLM-call
   instrumentation that records `requestSummary` (first 80 chars of the
   user message) + `responseSummary` (the bot's JSON-shaped chat output)
   + `callType` + `latencyMs` + `success` + `errorMessage`. Two reference
   runs carry `llm_calls`: `20260514-111724/results.json` (Sprint 25
   reference) and `20260514-114628/results.json` (Sprint 26 close run).
6. Detection-axis fragmentation of the three cited R-item instances
   (recorded in `compact/sprint-deliver-orchestrator.md` for the Sprint 27
   planning turn): only manual-probe UC-A RESOLVE is fully event-shape
   detectable. `cs_259` carries a content-shape precondition; `cs_011 T2`
   has a bot-content directive source (not `phase_plan`).

## Implement only

The dev agent performs these steps in order:

1. **Enumerate the six branches** at `PhaseEvaluator.java` lines 411,
   458, 485, 517, 564, 616. For each branch, identify the phase /
   active-UC scope it serves and read the `systemInstruction` string
   literal in full.
2. **Within each string**, identify zero or more individual directives.
   A directive is a sentence (or clause) that prescribes a bot action
   (MUST / SHOULD / MUST NOT / SHOULD NOT) keyed on a stated condition.
   Inline an enumerated list of the directives identified per branch in
   the handoff.
3. **Classify each identified directive** under the 4-question rubric
   in the "Classification rubric" section below. Record the answers
   verbatim per directive.
4. **Survey existing smoke artefacts** at
   `eval_interactive/results/20260514-111724/results.json` (Sprint 25
   reference) and
   `eval_interactive/results/20260514-114628/results.json` (Sprint 26
   close run). For each fully-event-shape directive identified, count
   observed trigger instances (cases where the precondition fires) and
   observed non-fulfillment instances (cases where the precondition
   fires AND the required action does not). The survey READS the
   `llm_calls[]` array + `case_results[].failure_tags` + the transcript;
   classification by structural shape only — see §1.7 hard gate below.
5. **Produce the evidence table** with one row per identified directive
   per branch, columns: `branch_line | directive_text_verbatim |
   precondition_shape | fulfillment_shape | directive_source |
   turn_scope | observed_triggers | observed_non_fulfillments |
   classification (target | content-shape-defer | bot-content-defer |
   no-directive)`.
6. **Make a recommendation** drawn from this closed set:
   - **(R1) Open structural sprint** — if n≥2 fully-event-shape
     directives (classification = `target`) carry observed
     non-fulfillment instances in the surveyed runs. Name the specific
     `DirectiveSpec` shapes the structural sprint should encode and the
     cases that demonstrate non-fulfillment.
   - **(R2) Targeted probe sprint follow-on** — if n=0 or n=1
     event-shape evidence in the existing smoke. Propose, but do NOT
     author, a follow-on probe sprint whose scope is "author N targeted
     CaseSpecs that exercise the event-shape directive's precondition".
   - **(R3) Defer further** — if the population of directives in the
     six branches is structurally incompatible with event-shape
     detection across enough branches that a slot would have too few
     ship-ready targets to justify the structural sprint. Name the
     re-trigger criterion (e.g. production evidence; later case-family
     authoring round; new R-item if a new event-shape directive
     instance surfaces).
7. **Update `docs/action_bank.md` §5.2 R-item entry at line 450** with
   the probe findings + the recommendation chosen. Do NOT mark the
   R-item closed.

## Classification rubric (apply to each directive)

For each directive identified in step 2 above, answer all four
questions in writing in the handoff:

- **Q1 — Precondition shape.** Does the directive's precondition depend
  on user message content (e.g. "if user asks about pricing", "if user
  is upset") = **content-shape**, or on observable events or phase
  state (e.g. "after `search_knowledge` returns a viable hit", "in
  DISCOVER phase before intake completes") = **event-shape**?
- **Q2 — Required-action shape.** Does the directive's required action
  map to a specific tool call / handover event / phase transition =
  **event-shape**, or to bot response content (e.g. "say X", "do not
  use jargon") = **content-shape**?
- **Q3 — Directive source.** Is the directive source the
  `phase_plan.systemInstruction` text the runtime emits (= **phase_plan**;
  in scope for the candidate slot), or bot user-message content from a
  prior turn (= **bot-content**; out of scope; see cs_011 T2)?
- **Q4 — Turn scope.** Does the directive apply within the current turn
  only (= **single-turn**), persist across turns until rescinded
  (= **persistent**), or fire one-shot when the precondition first
  matches (= **one-shot**)?

**Target classification rule:** a directive is `target` (ship-ready for
a future structural `DirectiveSpec` slot) iff Q1 = event-shape AND
Q2 = event-shape AND Q3 = phase_plan. Any other combination is **not
target** for the structural sprint and must be classified as one of
`content-shape-defer`, `bot-content-defer`, or `no-directive` (the
string had no MUST / MUST NOT / SHOULD / SHOULD NOT clause). Each
non-target directive becomes a proposed follow-on R-item the dev names
in the handoff (do NOT open them in `docs/action_bank.md` this sprint —
proposing the R-item name + scope is sufficient).

## Reproducibility rule (HARD)

Every count in the evidence table — every `observed_triggers` cell,
every `observed_non_fulfillments` cell — MUST cite both:

1. The **exact source path** (e.g.
   `eval_interactive/results/20260514-111724/results.json`).
2. The **extraction method**: a `jq` filter, a `python3 -c "..."`
   one-liner, or the literal phrase "manual eyeball, n=K cases inspected"
   when no scripted extraction was used.

Per `feedback_deliver_agent_cited_numbers_must_be_reproducible.md` and
the Sprint 25 fix-iteration precedent (commit `c8b8c85`,
`docs/sprints/sprint-025-fix-codex-review.md` pass). A cell without
both is BLOCKING. If a count is derived (e.g. "n=3 case-turns triggered
the precondition"), spell out the derivation (which field was filtered,
which aggregation level — per-case vs per-LLM-call vs per-transcript-
entry).

## Do not implement

The following are out of scope for Sprint 27 and are BLOCKING if the
diff touches them:

1. **No code changes.** Not in `server/`, not in
   `eval_interactive/eval_interactive/`, not anywhere in the source
   tree. Pure docs sprint.
2. **No new measurements.** Use existing smoke artefacts at
   `eval_interactive/results/20260514-111724/results.json` and
   `eval_interactive/results/20260514-114628/results.json`. Do NOT run
   a new smoke. Do NOT add new instrumentation. Do NOT modify any
   eval result file.
3. **No new probe scenarios authored.** If existing data is
   insufficient, the recommendation is **(R2)** — propose a follow-on
   probe sprint, do NOT author the scenarios this sprint.
4. **No `PhasePlan` restructure.** Do not modify
   `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java`, do
   not add a `DirectiveSpec` class, do not edit
   `PhaseEvaluator.java`. The structural sprint (if (R1) is chosen)
   does that work; Sprint 27 only classifies.
5. **No slot design beyond a preliminary sketch.** The handoff MAY
   describe what a `DirectiveSpec` shape would look like at the
   sentence level. The handoff MUST NOT propose a Java field schema,
   YAML config schema, or projection contract. That belongs to the
   structural sprint.
6. **No keyword / regex / if-else on user or bot text** — even for
   classification purposes. Each directive is classified by its
   **structural shape** (Q1 precondition shape, Q2 required-action
   shape), not by matching user-message or bot-response text. The
   `responseSummary` and `requestSummary` fields in `llm_calls` may be
   read to establish whether an LLM call happened with a given
   directive in its system instruction; they must NOT be matched
   against user-message phrasing to determine whether a directive
   fired. See §1.7 hard gate below.
7. **No eval-spec edits.** No CaseSpec edit
   (`eval_interactive/case_specs/**`), no override edit
   (`eval_interactive/case_spec_overrides.yaml`), no judge-rubric edit,
   no persona edit (`eval_interactive/personas/**`).
8. **No case-family surfaces** (`eval_interactive/case_families/**`).
9. **No foundational doc edits** (`docs/foundational/**`).
10. **No governance doc edits** (`docs/current/iteration_governance.md`,
    `doc_governance.md`, `agent_context_guide.md`).
11. **No sprint-archive edits** (`docs/sprints/sprint-001-*` through
    `docs/sprints/sprint-026-*.md`).
12. **No Tier-0 invariant add / modify** (`docs/runtime_freeze_and_risk_policy.md`).
13. **No latency / budget / model config edits.** Sprint 26's accept
    decision stands; ChatController.java budget,
    OpenAiCompatibleLlmClient sister timeouts, LlmProperties.java
    defaults, LlmClientConfig.java bean wiring, application-local.yml,
    .env.local — all out of scope.
14. **No `R-prompt-phase-plan-directive-followship` closure.** The
    R-item at `docs/action_bank.md:450` is updated with probe
    disposition; it is NOT marked closed. Closure decision belongs to
    the structural sprint (or to a future deferral round if (R3) is
    chosen).

## §1.7 hard gate

Per `docs/current/iteration_governance.md` §1.7 forbidden lines, the dev
agent must NOT:

- Match user-message text or bot-response text against keyword / regex
  lists to determine directive fulfillment. Classification is by
  structural shape (the four rubric questions); fulfillment counts are
  by event-shape signals (tool calls in `llm_calls`, failure tags,
  phase transitions in transcript metadata) only.
- Encode a directive's literal text into runtime or prompt as part of
  the recommendation. The recommendation names *shape* (e.g. "directive
  type: post-search-MUST-call"), not *content* (no verbatim quote of
  a UC-A RESOLVE directive into a Java file or prompt string).

A handoff that violates §1.7 in its evidence-gathering method is
BLOCKING.

## Success metrics

This sprint passes when, and only when, all of these hold:

- **Evidence table is complete.** Every directive identified across all
  six `PhaseEvaluator.plan(...)` branches has a row with all nine
  columns populated.
- **Classification rubric applied uniformly.** Q1 / Q2 / Q3 / Q4 answers
  are recorded in writing for every directive.
- **Reproducibility bar held.** Every `observed_triggers` and
  `observed_non_fulfillments` count cites source path AND extraction
  command per the Reproducibility rule above. Zero unverifiable counts.
- **Recommendation made.** The handoff lands on exactly one of
  (R1) / (R2) / (R3). The recommendation is grounded in the evidence
  count and the classification distribution, NOT pre-decided.
- **§1.7 hard gate respected.** No keyword / regex / content matching
  on user or bot text in the evidence-gathering method.
- **R-item disposition appended.** `docs/action_bank.md` line 450 is
  updated with the probe finding + chosen recommendation. R-item is
  NOT closed.
- **No hard fence violated.** Diff is docs-only; no source-tree change,
  no governance-doc change, no eval-spec change, no case-family change,
  no sprint-archive change, no foundational-doc change.

## Review rule

Codex sprint-close review MAY run, OR the human MAY skip Codex citing
the §4.1 exemption clause (per Sprint 26 precedent recorded in
`feedback_close_with_codex_skipped_docs_only_outcome.md`). Sprint 27
ships zero semantic-touching code; the exemption clause —
*"pure infra, docs-only, config-governance, and characterization-test
PRs are not subject to this review. If the PR is purely one of those,
return `approve` with a one-line note naming the exemption"* —
applies. If Codex runs, the appropriate verdict is `approve` with the
docs-only exemption note (Sprint 22 precedent). The review prompt in
`compact/sprint-027-review-prompt.md` carries the substantive scope
checks Codex should run even when applying the exemption.

The decision to dispatch Codex or to apply the §4.1 exemption directly
belongs to the human at close. Whichever path is chosen, the close
handoff §12 documents the verdict (Codex `pass` / Codex `approve with
exemption` / human-applied `approve (exemption: docs-only)`) and the
date.

## Layer-classification + anti-hardcode stanza — EXEMPT

Sprint 27 is docs-only investigation / probe sprint. It introduces no
prompt change, no runtime semantic decision change, no eval-spec
change, no judge-calibration change, no override / case-family / FAQ
content change, no Tier-0 candidacy, no code change of any kind. Per
`docs/current/iteration_governance.md` §7 ("Pure infra, docs-only,
config-governance, and characterization-test sprints are **exempt**
and need not include the stanza"), this sprint qualifies for the
exemption. Precedent: Sprint 15 (config governance), Sprint 16 (docs
+ characterization tests), Sprint 17 (G0 docs-only governance bundle),
Sprint 18 (G1 brief authoring), Sprint 22 (phase 2 line 358
reconciliation, docs-only scope correction), Sprint 26 (latency
decision sprint that landed on docs-only outcome and applied the §4.1
exemption at close). No §7 stanza is required; no Tier-0 invariant
is proposed; no semantic hardcode is introduced (no semantic surface
is touched).

The Sprint 21 §1.7-evidence-package gate does **not** apply to this
sprint, because Sprint 27 introduces no approved override, no rubric
edit, no judge-calibration change, and no quotation from brief
source-of-truth fields. The fix-iteration evidence convention from
Sprint 21 is therefore not in force.

## Stop conditions

The dev agent must stop and surface to the human if any of these
fire:

1. The six `PhaseEvaluator.plan(...)` branches at the cited line
   numbers (411, 458, 485, 517, 564, 616) do NOT actually exist at
   those lines. Premise drift; re-verify before proceeding.
2. A `PhasePlan.systemInstruction` string literal at one of the six
   branches has no MUST / SHOULD / MUST NOT / SHOULD NOT directive
   clauses at all. Record as `no-directive` in the evidence table and
   continue — but if all six branches are `no-directive`, the dev
   agent must STOP and surface the finding before drafting the
   recommendation (this would imply the R-item at line 450 is
   premised on directives that don't currently exist).
3. The evidence-gathering method requires content matching on user-
   message or bot-response text to classify a directive. §1.7 hard
   gate; re-scope to structural classification only or STOP.
4. The dev finds itself wanting to add a `DirectiveSpec` class, edit
   `PhasePlan.java`, edit `PhaseEvaluator.java`, or author new
   CaseSpec / case-family / probe scenarios. STOP — those belong to
   the structural sprint (if (R1)) or the follow-on probe sprint
   (if (R2)).
5. The dev finds itself wanting to ship a slot. STOP — even if the
   evidence supports (R1), Sprint 27 only **recommends** opening a
   structural sprint. The structural sprint is a separate sprint with
   its own §7 stanza (semantic-touching) and its own dev / review
   cycle.
6. The dev finds itself wanting to close
   `R-prompt-phase-plan-directive-followship`. STOP — the R-item is
   updated, not closed.
7. The dev finds itself wanting to spawn another Agent via tool call.
   STOP and report.
8. The dev finds the existing two smoke artefacts
   (`20260514-111724` + `20260514-114628`) carry **zero** trigger
   instances for any fully-event-shape directive identified. This is
   a valid finding (recommendation = (R2) targeted probe sprint
   follow-on); proceed and document, but flag it explicitly as the
   driver of the (R2) recommendation in the handoff.

## References

- `docs/action_bank.md:450` — `R-prompt-phase-plan-directive-followship`
  R-item entry to be updated (not closed) at close.
- `docs/sprints/sprint-018-handoff.md` §8.7 + §8.8 — conditional-
  broadening rule + open-observation rule (n=1 / n=2 / n=3 thresholds).
- `docs/sprints/sprint-019-handoff.md` §3.3 / §3.5 — cs_011 + R-item
  promotion context.
- `docs/sprints/sprint-020-handoff.md` §5 — `already_called`
  soft-signal slot precedent (the shape a future structural sprint
  would echo).
- `docs/sprints/sprint-025-handoff.md` §4 / §5 — per-LLM-call
  instrumentation reference (`llm_calls` field schema, request /
  response summary content).
- `docs/sprints/sprint-026-handoff.md` — latency-decision accept
  precedent + the (C) Accept rationale that put Sprint 25
  instrumentation in stable use.
- `docs/current/iteration_governance.md` §1 (Constitution), §3 (Fix
  Layer Classification), §5 (Eval Acceptance Rules), §7 (sprint-
  objective stanza + exemption).
- `eval_interactive/results/20260514-111724/results.json` — Sprint 25
  reference smoke (carries `llm_calls`).
- `eval_interactive/results/20260514-114628/results.json` — Sprint 26
  close smoke (carries `llm_calls`).
- `server/src/main/java/com/gumtree/csagent/service/runtime/PhaseEvaluator.java`
  — the six branches at lines 411 / 458 / 485 / 517 / 564 / 616.
- `server/src/main/java/com/gumtree/csagent/model/PhasePlan.java:30` —
  `systemInstruction` field.
- `server/src/main/java/com/gumtree/csagent/service/runtime/ContextProjectionBuilder.java:788–789`
  — projection site for `phase_plan.system_instruction`.
