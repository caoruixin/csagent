---
title: Iteration processes — HUMAN REFERENCE ONLY (two input paths, narrative)
doc_tier: durable-connective
status: current
implementation_status: implemented
source_of_truth: this file (narrative) + `compact/sprint-deliver-orchestrator.md` (operational source-of-truth for deliver-agent)
last_reviewed: 2026-05-17
review_cadence: per 3-5 milestones (folded back when path or roles evolve)
notes: >
  HUMAN REFERENCE ONLY. This doc is NOT loaded by any agent (not in
  `AGENTS.md` transitive chain; not referenced from any agent-loaded
  doc). It exists for the human's planning-time read — to remember
  WHY the two paths exist, the conceptual flow, and the anti-patterns.
  Operational source-of-truth for the deliver-agent (triage gate
  criteria, 4-route fit decision rubric, edge case handling, lifecycle
  procedures) lives in `compact/sprint-deliver-orchestrator.md`
  "Workflow inputs" section, which the human pastes when spawning a
  new deliver-agent session. The strict topic split (Option γ
  2026-05-17): human-ref = narrative / WHY / examples / anti-patterns;
  compact = operational HOW / decision rubrics / checklists.
---

# Iteration processes — two input paths (HUMAN REFERENCE ONLY)

> **HUMAN REFERENCE ONLY.** This doc is not loaded by any agent. If you are an agent reading this by accident: stop and read `compact/sprint-deliver-orchestrator.md` instead — that is the operational source-of-truth. If you are the human: read this when you want to remember the conceptual framing of the two input paths and the anti-patterns; consult `compact/sprint-deliver-orchestrator.md` for the operational checklists when actively driving a planning round.

The customer service agent's evolution is driven by **two distinct input paths**. Both converge on the same downstream loop (`research-agent proposal → deliver-agent milestone planning → dev/review/close` per `iteration_governance.md` §8). The paths differ only in **what triggers them** and **how the research-agent proposal is constructed**.

## 1. Why two paths

| | Path 1 (Research-driven) | Path 2 (Bad-case-driven) |
|---|---|---|
| **Trigger** | Human idea / strategic direction | Real session bad case observed (user / colleague / sprint-surfaced) |
| **Orientation** | Forward-looking (architectural ambition) | Backward-looking (observed failure) |
| **Primary input artefact** | Human idea statement + scope direction | Real session trace + observed-vs-expected discrepancy |
| **Typical scale** | Multi-milestone architecture | Single failure mode; may fold into existing milestone or open a new R-item |
| **Risk shape** | Scope expansion (overshoot architecture) | Whack-a-mole (under-shoot architecture by fixing only the symptom) |

The two paths exist because **forward-looking architecture and backward-looking failure-driven fixes are different inputs to the same downstream planning loop**. Forcing both into one shape (e.g., always requiring a strategic-direction frame for a bad case) creates ceremony overhead and loses the trace-grounded specificity. Forcing both to skip the research-agent step risks deliver-agent role bloat and Constitution §1.7 boundary erosion.

Both paths preserve:

- The Constitution (`iteration_governance.md` §1) and §1.7 forbidden list.
- The deliver / dev / review agent role separation (`compact/sprint-deliver-orchestrator.md`).
- The §7 stanza requirement for semantic-touching sprints.
- The §8 milestone framework cadence.
- The §5.5 / §5.6 acceptance gate structure (smoke = observation; curated bad-case suite = human-judgment primary gate).

## 2. Path 1 — Research-driven (forward-looking)

### 2.1 Trigger

The human has an architectural idea, a strategic direction, or wants to consume a backlog item from `docs/action_bank.md` that has matured (typically through n ≥ 2 observations or human prioritization).

Examples of valid Path 1 triggers:

- "I want to ship the original roadmap's Sprint 18 semantic planner shadow mode that has been deferred."
- "Let's add per-LLM-call latency instrumentation so we can decide whether to widen the deadline budget" (Sprint 25 was a Path 1 sprint).
- "The single handover orchestrator is now a release-gate blocker; promote `D-single-handover-orchestrator` from deferred to active."

### 2.2 Steps

```
human idea / strategic direction
  ↓
human briefs research agent(s)
  → input: idea statement + relevant codebase area pointers + (optional) cross-checking research agents for validation
  ↓
research agent(s) produce proposal
  → output: solution-proposal document covering:
    (a) current-state code-grounded survey (verified at HEAD)
    (b) design alternatives (≥ 2 options where applicable)
    (c) recommended option + rationale
    (d) sprint / sub-sprint split suggestion
    (e) layer classification per §3.2 + §7 stanza pre-fill draft
    (f) hard fences + non-goals
    (g) deliver cadence suggestion (single sprint vs milestone)
    (h) risk + compounding-effect analysis
  ↓
human reviews
  → may request cross-validation by another research agent
  → may push back on scope / scope-discipline
  → selects one proposal (or merged) for deliver-agent
  ↓
selected proposal → deliver-agent
  ↓
deliver-agent (per iteration_governance.md §8 milestone framework)
  → coverage check: does action_bank already cover? overlap with current milestone?
  → if fits current milestone scope: add as sub-sprint OR pivot current milestone
  → if doesn't fit: stage as milestone candidate (M2 / M3 sequence pick)
  → draft milestone_objective.md OR sprint_objective.md (per fit)
  → draft dev/review prompts
  ↓
human reviews milestone + sub-sprint contracts
  ↓
dev agent (Claude Code) executes per sub-sprint contract
  ↓
review agent (Codex) reviews at milestone close per §4.3
  ↓
deliver-agent + human apply close decision per §5 + §5.5/§5.6/§5.6.2
```

### 2.3 Path 1 entry artefacts (what the human passes when activating)

For the deliver-agent role activation (whether in this session or a fresh `/clear`-spawned session per the `compact/sprint-deliver-orchestrator.md` cold-start protocol):

- **Placeholder 1 — the proposed whole solution**: the research-agent's proposal verbatim or summarized. If multiple research agents were consulted, include all outputs + the human's selection rationale.
- **Placeholder 2 — the next deliver scope**: what the human wants the next milestone or sub-sprint to address (subset of the proposal).

The deliver-agent's first action on Path 1: read both placeholders, then perform the §8 milestone planning (or single-sub-sprint planning per §8.5 single-of-one).

### 2.4 Path 1 example (this session, 2026-05-16)

- Human idea: "drive the customer-service agent toward LLM-first per the research-agent's whole-solution proposal."
- Research-agent's whole-solution proposal: included verbatim as Placeholder 1.
- Human's next deliver scope: "investigate which of the original Sprint 17-23 + Triggered roadmap items are delivered vs not, and lock the next sprint scope." (Placeholder 2.)
- Deliver-agent action: investigated delivery status (matrix in `compact/context-handoff-sprint31-fix2.md` §3.2); identified Sprint 31 / 32 in-flight; ran Sprint 32 close-out + governance upgrade to milestone framework; staged M1 + Sprint 33.

## 3. Path 2 — Bad-case-driven (backward-looking)

### 3.1 Trigger

A real-session bad case has been observed where the bot's behaviour materially diverges from the human-verified expected behaviour. Sources:

- The human or a colleague hits an unexpected behaviour in normal use.
- A planned experiment (e.g., Alice mock account) surfaces a specific failure shape.
- A sprint execution surfaces an architectural concern (e.g., Sprint 32 §13 in-flight downgrade pattern).
- An external user report (post-release; treated specially per §3.5 below).

### 3.2 Triage gate (operational; see compact)

Before any work is scoped, the human + deliver-agent jointly evaluate whether the bad case is **load-bearing**. The full triage criteria checklist (5 items, e.g., "influences release-gate trajectory", "crosses ≥ 1 layer", etc.) is the operational rubric the deliver-agent applies; it lives in `compact/sprint-deliver-orchestrator.md` "Workflow inputs — Path 2 triage gate criteria". Cases that fail triage are discarded (optionally logged for pattern recognition); cases that pass triage proceed to the §3.3 flow below.

### 3.3 Steps (load-bearing case)

```
real session / colleague / experiment surfaces a failure
  ↓
human + deliver-agent triage: load-bearing? (§3.2 criteria)
  → if NO: discard / log only
  → if YES: proceed
  ↓
research agent(s) — bad-case mode
  → input: session trace + form_context + observed-vs-expected discrepancy + (optional) cross-checking research agents
  → MUST do all 4 of:
    (a) Code-grounded multi-layer root-cause analysis (every cited path verified at HEAD)
    (b) Coverage check vs docs/action_bank.md R-items + docs/milestone_objective.md scope
    (c) Compounding-effect analysis (which fix must precede which; what makes the failure WORSE if fixed in wrong order)
    (d) Output a deliver-agent-consumable proposal: layer per §3.2; sub-sprint suggestion; §7 stanza pre-fill; hard fences
  ↓
human reviews proposal
  → push back on scope or §1.7 risk if needed
  → select the design
  ↓
selected proposal → deliver-agent
  ↓
deliver-agent encodes the bad case
  → author <case_id>.yaml in eval_interactive/case_specs/bad_cases/
  → bad_case_metadata: surfaced_by / surfaced_date / source_session_id / failure_shape / layers_involved / related_dimensions / related_r_items
  → closure_criterion: human-verified observable end-state(s) that count as resolved
  → tier assignment: `core` for cross-cutting; `scope-relevant` for surface-specific
  → append row to bad_cases/_manifest.md lifecycle ledger
  ↓
deliver-agent routes the work (4 routes per §3.4)
  ↓
[downstream loop converges with Path 1: milestone_objective.md / sprint_objective.md / dev / review / close]
```

### 3.4 The 4-route fit decision (operational; see compact)

After a load-bearing bad case is encoded, the deliver-agent routes the work to one of **4 routes**: (a) fits current milestone, (b) fits future planned milestone, (c) requires new R-item / new milestone, (d) emergency (Tier-0 safety only). The full decision table (route / when-condition / action) is the operational rubric the deliver-agent applies; it lives in `compact/sprint-deliver-orchestrator.md` "Workflow inputs — Path 2 four-route fit decision". Conceptually: most bad cases route (a) or (b); (c) is for novel architectural concerns; (d) is rare and requires explicit human authorization per `docs/runtime_freeze_and_risk_policy.md`.

### 3.5 Edge cases (operational; see compact)

Three recurring edge cases (production-released user-reported bug; bad case surfaced during sprint execution by dev or review agent; bad case turns out to duplicate an existing closed-as-regression-guard case) have prescribed handling procedures the deliver-agent applies. The full edge-case handling table lives in `compact/sprint-deliver-orchestrator.md` "Workflow inputs — Path 2 edge cases".

### 3.6 Path 2 example (this session, 2026-05-16)

- Surfaced by: human (Alice mock session via real-LLM run, observed during use).
- Triage: load-bearing (D1+D2+D3+D4 cross-layer failure; influences release-gate trajectory).
- Research-agent (bad-case mode): produced the 5-dimension coverage report mapping each dimension to (a) current code state with file:line cites, (b) existing R-item / sprint coverage, (c) compounding-effect warnings (e.g., D2 before D1 = WORSE failure), (d) recommended R-items and sequencing.
- Human + deliver-agent picked option (a) "fits current milestone scope" — Alice case became M1's primary acceptance bar (per `docs/milestone_objective.md` §5).
- Encoded: `eval_interactive/case_specs/bad_cases/alice_uc_a_uc_h_misclass.yaml` (tier `core`).
- Downstream: M1 milestone framework + Sprint 33 first sub-sprint (DISCOVER UC-A/FP/H disambiguation) per `docs/sprint_objective.md`.

## 4. Convergence point

Both paths converge at **"selected proposal → deliver-agent → §8 milestone framework"**. From this point on, the downstream loop is identical:

1. Deliver-agent assesses fit (current milestone / future milestone / new R-item / emergency).
2. Deliver-agent drafts `milestone_objective.md` (new) OR `sprint_objective.md` (new sub-sprint).
3. Human reviews + approves contracts.
4. Dev agent executes per sub-sprint contract; surfaces handoff at sub-sprint close.
5. Deliver-agent + human assess sub-sprint progress (A/B/C/D classification per `compact/sprint-deliver-orchestrator.md`).
6. Milestone close: Codex review per §4.3; deliver-agent + human manual review bad-case suite per §5.6; close decision per §5.5 hard gates.

## 5. Anti-patterns to avoid

- **Skipping the research-agent step on Path 2** to "save time" — deliver-agent then does dual roles (investigation + planning), expands scope, drifts from §1.7 anti-hardcode discipline. The research-agent step is a checkpoint, not bureaucracy.
- **Treating Path 1 proposals as binding** — the research-agent produces a PROPOSAL; the human selects; the deliver-agent pushes back on scope. Don't blindly ship a research-agent's recommendation if it expands scope past §8 cadence.
- **Path 2 proposal that doesn't do the coverage check (§3.3 step (b))** — leads to duplicate work or scope conflicts with the active milestone. Coverage check is non-optional.
- **Encoding every bad case as `core` tier** — bloats the regression suite; manual review becomes intractable. Only cross-cutting / release-gate-relevant cases are `core`; surface-specific cases are `scope-relevant`.
- **Auto-PASS / auto-FAIL on bad case closure criterion** — §5.6 (2026-05-17 refinement) explicitly makes this a human-judgment gate. CI-style programmatic checks on `composite_score >= X` would re-import the §5.5 confounding sources.
- **Bad case discovered mid-milestone forcing scope expansion** — wait for next planning round per `iteration_governance.md` §8.5 ("a sub-sprint that crosses an unrelated architectural surface belongs to a different milestone; the deliver-agent SHALL surface this at sub-sprint planning round rather than smuggle the scope across milestones"). Exception: route (d) emergency only.
- **Path 2 proposal that fixes the symptom without fixing the cause** — e.g., fixing D2 (intake field prefill) without first fixing D1 (DISCOVER mis-classification) silently completes wrong-UC intake. The compounding-effect analysis (§3.3 step (c)) is non-optional.

## 6. Document hierarchy reminder

```
docs/action_bank.md              (backlog, cross-milestone persistent;
                                  R-items flow in from BOTH Path 1
                                  and Path 2)
       ↓ (deliver-agent picks R-items into a milestone)

docs/milestone_objective.md      (current milestone; names sub-sprints +
                                  acceptance bar including which bad cases
                                  per §5.6.2 selection)
       ↓ (deliver-agent picks one sub-sprint contract)

docs/sprint_objective.md         (current sub-sprint dev/review contract)
       ↓ (consumed by dev session)

docs/sprints/sprint-NNN-handoff.md  (dev-authored sub-sprint archive)

docs/codex-findings.md           (review agent's per-sprint or per-milestone
                                  verdict per §4.2/§4.3)

eval_interactive/case_specs/bad_cases/<case>.yaml
                                 (curated bad-case suite; primary
                                  human-judgment gate per §5.6)

eval_interactive/case_specs/bad_cases/_manifest.md
                                 (lifecycle ledger: tier + per-milestone
                                  PASS/FAIL/IMPROVING + downgrade history)
```

This file (`iteration_processes_only_for_human_reference.md`) sits at the durable-connective tier as a HUMAN-REFERENCE-ONLY narrative. It is NOT in the `AGENTS.md` constitution chain. The operational source-of-truth for the deliver-agent (triage criteria, 4-route fit decision, edge cases, lifecycle procedures) lives in `compact/sprint-deliver-orchestrator.md` "Workflow inputs" section, which the human pastes when spawning a new deliver-agent session.

## 7. Backwards compatibility

Pre-2026-05-17 sprints (Sprint 1-32) were not driven by an explicit Path 1 / Path 2 distinction. The processes formalize what was already happening implicitly (Sprint 30 Option β design freeze was Path 1; Sprint 32 surfaced the Option β coverage gap from in-flight findings, which is a Path 2 variant). Pre-2026-05-17 archives are not retroactively re-labelled.

The processes apply prospectively from M1 onward (2026-05-17+).
