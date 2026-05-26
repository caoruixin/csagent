Paste the content below this line into a fresh Claude Code session on branch `design-v1-without-human-review`. Working tree at session-start carries deliver-agent-owned files + 3 pre-existing unrelated mods — see §10.

---

You are the **Sprint 27 dev agent** on `design-v1-without-human-review`.
Sprint 27 is a **docs-only investigation / probe sprint** triggered by
a planning-turn premise check on `R-prompt-phase-plan-directive-followship`
(`docs/action_bank.md:450`). The R-item was promoted on n=3 cited
instances; the check found those instances fragment on the detection
axis — only the manual-probe UC-A RESOLVE instance is fully event-shape
detectable. Per the conditional-broadening rule (n=1 event-shape
evidence insufficient to ship structural action), Sprint 27 does **not**
ship a slot. The deliverable is a decision document: evidence table +
recommendation drawn from a closed set (R1 / R2 / R3). The
recommendation IS the primary deliverable; the data + rubric drive
the choice, not pre-decided.

## 1. Load order

1. `AGENTS.md` (loads `iteration_governance.md` §1 §3 §5 §7 +
   `doc_governance.md` + `agent_context_guide.md`).
2. `docs/sprint_objective.md` — Sprint 27 contract, end-to-end.
3. `docs/sprints/sprint-018-handoff.md` §5.2 + §8.7 / §8.8 (Method
   note + open-observation / conditional-broadening rules).
4. `docs/sprints/sprint-019-handoff.md` §3.3 + §3.5 (cs_011 T2 silence
   + cs_040 context).
5. `docs/sprints/sprint-020-handoff.md` §5 (`already_called` slot
   precedent — shape a structural sprint would mirror).
6. `docs/sprints/sprint-025-handoff.md` §4 / §5 (`llm_calls` schema).
7. `docs/sprints/sprint-026-handoff.md` §1 + §6 (docs-only close
   precedent).
8. `docs/action_bank.md` lines ~440–466 (R-item entry).

Do NOT load Sprint 21–24 archives unless analysis requires it.

## 2. Premise carried forward (verified 2026-05-15 — do NOT re-verify)

1. `PhasePlan.systemInstruction` at `model/PhasePlan.java:30`.
2. Six `PhaseEvaluator.plan(...)` branches emit `.systemInstruction(...)`
   string literals in `service/runtime/PhaseEvaluator.java` lines
   **411 / 458 / 485 / 517 / 564 / 616** (path is `service/runtime/`,
   NOT top-level `runtime/`; planning-turn brief had path wrong, lines
   correct).
3. `phase_plan.system_instruction` projected to LLM at
   `service/runtime/ContextProjectionBuilder.java:788–789`.
4. No `DirectiveSpec` surface today.
5. Sprint 25 `llm_calls[]` array on each `case_result` carries
   `id / sessionId / turnIndex / callType / model / promptTokens /
   completionTokens / latencyMs / requestSummary / responseSummary /
   success / errorMessage / createdAt`. Two reference runs:
   `eval_interactive/results/20260514-111724/results.json` (Sprint 25
   reference) + `eval_interactive/results/20260514-114628/results.json`
   (Sprint 26 close).

If any of these six items fails on your verification read, STOP (§9.1).

## 3. The six branches to enumerate

For each line, read the surrounding `PhaseEvaluator.plan(...)` branch
end-to-end. Record in the handoff:

| anchor line | what to record |
|---|---|
| 411 | branch condition (phase / UC / predicates) + verbatim `systemInstruction` + directive list |
| 458 | same |
| 485 | same |
| 517 | same |
| 564 | same — note: this branch calls `buildIntakeSystemInstruction(activeUc, ucDef)`; read that method and enumerate per `activeUc` value (UC-A through UC-K) for at least one representative directive set |
| 616 | same |

A **directive** is a clause prescribing a bot action (MUST / SHOULD /
MUST NOT / SHOULD NOT) keyed on a stated condition. Enumerate zero or
more per branch.

## 4. Classification rubric (per directive)

Answer all four in writing:

- **Q1 — Precondition shape.** Content (user message text / inferred
  user intent) vs event (tool call / phase state / intake field /
  observable runtime event).
- **Q2 — Required-action shape.** Event (specific tool call / handover
  / phase transition) vs content (bot response wording).
- **Q3 — Directive source.** `phase_plan` vs `bot-content`. Directives
  extracted from `PhaseEvaluator.java` are always `phase_plan`; flag
  cross-turn dependencies in notes.
- **Q4 — Turn scope.** single-turn / persistent / one-shot.

**Target rule:** Q1=event AND Q2=event AND Q3=phase_plan → `target`
(ship-ready for a future structural slot). Other → `content-shape-
defer`, `bot-content-defer`, `no-directive`. For each non-target,
propose a follow-on R-item name + one-line scope; do NOT open in
`action_bank`.

**Borderline guidance.** Precondition mentioning user-message content
→ content-shape; mentioning tool calls / phase state / slot values →
event-shape. Required action naming a specific tool (`request_handover`,
`search_knowledge`, `resolve_article`, `create_case`) → event-shape;
"say X / use plain language / no jargon" → content-shape. Mixed
clauses: pick dominant, note ambiguity. **Classify conservatively** —
when in doubt, `content-shape-defer`, not `target`.

## 5. Smoke survey (target-classified directives only)

For each `target` directive, count from
`eval_interactive/results/20260514-111724/results.json` AND
`20260514-114628/results.json`:

- **observed_triggers** — case-turns where the precondition fires
  (runtime entered the relevant branch).
- **observed_non_fulfillments** — of triggered turns, how many failed
  the required action.

**Detection method (event-shape signals only):** `case_results[]` →
`llm_calls[].callType` / `latencyMs` / `requestSummary` (read for
context, NOT content-match); `failure_tags`; `transcript[]` `source` /
`role` / `turn_index`; `active_use_case` / `expected_outcome` /
`stop_reason`. Infer precondition-fires from `active_use_case` +
transcript turn ordering + the branch conditions you recorded in §3.
Infer fulfillment from whether the required event (tool call,
handover, phase transition) appears in subsequent entries.

**§1.7 hard gate — FORBIDDEN:** matching user / bot text against
keyword / regex / content patterns to determine fulfillment.
`responseSummary` may be READ to confirm a call happened; it must NOT
be matched against expected directive content. Classification by
structural shape; fulfillment by event-shape. Content-matching =
BLOCKING per §1.7.

Zero triggers is valid — record
`observed_triggers=0, observed_non_fulfillments=0 (no triggers)`.

## 6. Reproducibility rule (HARD)

Every count cites both: **source path** + **extraction method** (`jq
'<filter>' <path>` with literal output, `python3 -c "..."` with literal
result, or "manual eyeball over `<path>`, n=K cases inspected"). A
cell without both is BLOCKING per
`feedback_deliver_agent_cited_numbers_must_be_reproducible.md` +
Sprint 25 fix-iteration precedent (`c8b8c85`). Derived counts spell
out the derivation (which field, which aggregation: per-case vs
per-LLM-call vs per-transcript-entry).

## 7. Evidence table shape

One row per directive per branch. Columns:

`branch_line | directive_text_verbatim | precondition_shape | fulfillment_shape | directive_source | turn_scope | observed_triggers (cited) | observed_non_fulfillments (cited) | classification`

Branches with no directives: one row, `classification=no-directive`.

## 8. Recommendation (the deliverable)

Land on exactly one. Grounded in the evidence table; never pre-decided:

- **(R1) Open structural sprint** — n≥2 `target` directives with
  observed non-fulfillment. Name the `DirectiveSpec` shapes the
  structural sprint should encode + the cases demonstrating
  non-fulfillment.
- **(R2) Targeted probe sprint follow-on** — n=0 or n=1 event-shape
  evidence. Propose (do NOT author) a follow-on probe sprint scoped to
  author N targeted CaseSpecs across UCs.
- **(R3) Defer further** — population structurally incompatible with
  event-shape detection. Name the re-trigger criterion.

## 9. Stop conditions

1. The six branches at lines 411 / 458 / 485 / 517 / 564 / 616 do NOT
   exist (premise drift; planning-turn verified 2026-05-15 at SHA
   `1f4a1db`).
2. All six branches contain `no-directive`. STOP — this would imply
   the R-item is premised on directives that don't currently exist.
3. Your evidence-gathering method reaches for content matching on
   user / bot text. §1.7 hard gate; re-scope or STOP.
4. You want to add `DirectiveSpec`, edit `PhasePlan.java`, edit
   `PhaseEvaluator.java`, edit `ContextProjectionBuilder.java`, or
   author new CaseSpec / case-family / probe scenarios. STOP.
5. You want to ship a slot even if your evidence supports (R1). STOP
   — Sprint 27 only **recommends**.
6. You want to close `R-prompt-phase-plan-directive-followship`. STOP.
7. You want to spawn another Agent via tool call. STOP.

## 10. Working tree + scope

Deliver-agent-owned files uncommitted at session-start per
`feedback_commit_at_end_bundles_deliver_artefacts.md`:
`docs/sprint_objective.md`, `compact/sprint-027-*.md`, possibly
`compact/sprint-deliver-orchestrator.md`. Pre-existing unrelated mods:
`csagent_system_design_review.md`, `system_prompt.txt` (**DO NOT
TOUCH** — Sprint 23 prompt surface, out of scope), untracked
`csagent-solution-_20260514.md`. The human manages these; don't stage,
don't un-stage.

**In scope (write):** `docs/sprints/sprint-027-handoff.md` (NEW,
~250–450 lines); `docs/action_bank.md` line 450 R-item entry — append
probe finding + recommendation; R-item NOT closed.

**Out of scope (HARD — BLOCKING in diff):** any source-tree file
(`.java`, `.py`, `.ts`, `.tsx`, `.yml`, `.yaml`, `.properties` under
`server/`, `ui/`, `eval_interactive/eval_interactive/`); new smoke /
instrumentation / probe scenario file; `PhasePlan` / `PhaseEvaluator`
/ `ContextProjectionBuilder` edit; `DirectiveSpec` class creation;
eval-spec / override / case-family / persona / judge-rubric edit;
`docs/foundational/**` edit; `docs/current/iteration_governance.md` /
`doc_governance.md` / `agent_context_guide.md` edit; sprint-archive
edit (`docs/sprints/sprint-001-*` through `sprint-026-*.md`); Tier-0
edit; latency / budget / model config edit;
`R-prompt-phase-plan-directive-followship` closure.

Commit: stage only `docs/sprints/sprint-027-handoff.md` +
`docs/action_bank.md`. Avoid `git add -A` / `git add .`. If human
bundles deliver-agent files, classify A-with-packaging-note (Sprint 20
precedent).

## 11. Handoff doc contract (`docs/sprints/sprint-027-handoff.md`)

Mirror Sprint 22 / Sprint 26 docs-only shape. Twelve sections: (1)
Context Pack — docs sampled, code paths verified at session-start with
lines, doc-status warnings, source-of-truth decision, implementation
status, risks; (2) Sprint-objective recap (1 paragraph); (3) Premise
re-verification — confirm §2's six facts with line cites; (4) Per-branch
enumeration (subsection per branch 411 / 458 / 485 / 517 / 564 / 616;
condition + verbatim string + directive list); (5) Evidence table
(every count cited per §6/§7); (6) Recommendation (R1 / R2 / R3) with
the distribution driving the choice; (7) Proposed follow-on R-items for
each non-target directive (name + one-line scope; do NOT open); (8)
Anti-hardcode self-walk (§4.1 nine questions; exemption declared
top — docs-only investigation); (9) Files changed (handoff NEW +
`action_bank.md` line 450 update only); (10) Layer-classification
self-walk (§3; exempt; name layers the proposed follow-on would
target); (11) Open questions for human; (12) Closure verdict
placeholder for the close turn (status, classification, Codex outcome
TBD, R-item disposition applied, date).

Begin work. Verify §2 premise first; enumerate branches in order;
classify per §4; survey per §5; produce evidence table per §7;
recommend per §8. Stop on any §9 condition.
