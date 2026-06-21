---
title: "M-Auto-11 — Loop-convergence / viable-hit utilization (characterization-first)"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active-milestone pointer)
last_reviewed: 2026-06-21
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  DRAFT — deliver-agent scoping output, PENDING HUMAN APPROVAL before any dev session
  is launched. M-Auto-11 is framed as a LOOP-CONVERGENCE / VIABLE-HIT UTILIZATION
  investigation — NOT a post-satisfaction or semantic_planner-posture milestone (that
  hypothesis is disproven, see docs/diagnostics/m-auto-11-post-satisfaction-over-escalation-investigation-2026-06-21.md).
  Characterization-FIRST: WP1 (Sprint 101 / S-Auto-49) is a bounded real-LLM
  characterization only, with NO runtime/eval/scoring/baseline/CaseSpec(existing)/
  simulator change. WP2 (design review of the narrowest loop-convergence intervention)
  is scoped ONLY if WP1 evidence is load-bearing and causally coherent; WP3 (a runtime
  change) is NOT pre-authorized and requires a fresh sprint_objective + §7 stanza +
  human approval. Preserves M-Auto-10's "no closure-path defect" conclusion and the
  delivered honest turn_budget_exhausted labeling (B1 / S-Auto-14).
---

# M-Auto-11 — Loop-convergence / viable-hit utilization (characterization-first)

> **STATUS: DRAFT — pending human approval. Do NOT launch WP1 from this file until the
> human approves the scope and the WP1 CaseSpec ground truth (§5).**

## 1. Milestone question

When relevant knowledge has **already been retrieved and remains usable**, can the
within-turn agent loop **fail to converge** — repeatedly request or re-emit equivalent
retrieval work, exhaust `maxToolSteps`, and fall through to a `turn_budget_exhausted`
escalation **that could have been avoided** by answering from the evidence already on
hand?

This is a **loop-convergence / viable-hit-utilization** question, not a posture or
satisfaction question. The candidate that seeded it
(`R-post-satisfaction-mechanical-over-escalation`) was investigated and its
`semantic_planner`-posture framing **disproven**: the runtime synthesizes the
mechanical escalation on a MAX_STEPS exit (`PhaseEvaluator.java:628-644`); the LLM did
not author it; the runtime never observes user satisfaction
(`docs/diagnostics/m-auto-11-post-satisfaction-over-escalation-investigation-2026-06-21.md`).
M-Auto-11 re-frames the durable, mechanism-grounded part of that finding.

## 2. Milestone class (layer breakdown + §7 coverage)

| WP | Sub-sprint id | §3.2 layer (hypothesis) | §7 stanza | Codex |
|----|---------------|-------------------------|-----------|-------|
| **WP1 — bounded characterization (REQUIRED FIRST)** | Sprint 101 / S-Auto-49 | characterization measurement (instrument is `eval_spec`; the *behaviour under measurement* is hypothesised `prompt_projection` viable-hit under-utilization + `infra` loop-budget consumption) | **EXEMPT** (characterization-test sprint per AGENTS.md) | §4.1 anti-hardcode kernel **EXEMPT** (ships no semantic surface — measurement only); milestone-shared Codex at close |
| **WP2 — design review of the narrowest intervention (CONDITIONAL)** | Sprint 102 / S-Auto-50 *(reserved; NOT pre-committed)* | multi-layer **prospective** (read-only): candidate layers `prompt_projection` (project the already-available viable hits into the next loop step) \| `infra` (soft loop-break that permits answering from existing evidence before hard MAX_STEPS; suppressed-duplicate-step counter semantics) | **EXEMPT** (design/research; ships no behaviour) | design review (read-only `codex exec`), not the §4.1 kernel |
| ~~**WP3 — runtime loop-convergence change (CONDITIONAL)**~~ **NOT pre-authorized** | reserved id only | `prompt_projection` / `infra` (TBD by WP2) | **REQUIRED** if launched (semantic surface) | per-sub-sprint §4.1 kernel REQUIRED + milestone-shared at close |

The milestone is **characterization → conditional design → conditional implementation.**
The WP1→WP2 boundary is a **hard decision gate** (§6): WP2 is scoped only if WP1
evidence is load-bearing. WP3 is not pre-authorized at all.

## 3. Goal

Establish, on **bounded real-LLM evidence**, whether the
"reach-MAX_STEPS-despite-viable-hits" behaviour is **reproducible / load-bearing**,
**intermittent-but-material**, or **isolated / non-actionable** — independently of any
simulator-only state — and only then decide whether a narrow loop-convergence
intervention is warranted. Preserve the already-delivered **honest**
`turn_budget_exhausted` labeling (B1 / S-Auto-14); this milestone is about the
potentially-avoidable *behaviour* that reaches MAX_STEPS, not the *label* on it.

## 4. Sub-sprint sequence

### WP1 — Sprint 101 / S-Auto-49 — bounded loop-convergence characterization (REQUIRED FIRST)

Characterization only. Encodes **one** narrow human-blessed shape-(ii) CaseSpec (§5)
plus reuses three existing controls, runs a **minimum bounded real-LLM V3-cadence
sample**, and adjudicates per-attempt whether each MAX_STEPS/`turn_budget_exhausted`
terminal was **avoidable** (viable hits present + answerable follow-up + a grounded
answer was emittable before MAX_STEPS) vs **legitimate** (genuine no-hit, persona
genuinely unresolved, or correct degradation) vs **noise** (simulator continuation
variance / provider instability). Deliverable = a human-reviewable characterization
verdict + attribution table. **Ships no runtime/eval/scoring/baseline/existing-CaseSpec/
simulator change.** Full contract: `docs/sprint_objective.md`; dev prompt
`compact/sprint-101-dev-prompt.md`.

### WP2 — Sprint 102 / S-Auto-50 — design review (CONDITIONAL; reserved id; NOT pre-committed)

Launched **only if** WP1 ⇒ load-bearing AND causally coherent. Read-only design review
of the **narrowest** loop-convergence intervention, weighing (without pre-committing to
any one): (a) projecting the already-available viable-hit evidence more clearly into the
next loop step (`prompt_projection`); (b) a soft loop-break permitting an answer from
existing evidence before hard MAX_STEPS (`infra`); (c) suppressed-duplicate-step counter
semantics. Verdict routes to (a) ship a WP3 charter, or (b) close NO-KEEP (the behaviour
is acceptable / not narrowly fixable without violating a fence). Design-only; STOPS at
the verdict; Codex design-review + human sign-off.

### WP3 — runtime loop-convergence change (CONDITIONAL; NOT pre-authorized; no reserved scope)

Authorized **only** by a fresh `sprint_objective.md` + §7 stanza + human approval after a
route-(a) WP2 verdict. Inherits the M-Auto-9 fences (no premature-guard relax, no
`max_turns`/`maxToolSteps` raise, no `record_outcome` requirement lowering, no
CaseSpec/PRIMARY exception, no user-message-content heuristic, no escalation-reason
semantics change) and adds: **must preserve failure honesty** (a genuinely-unresolved
flow must still escalate honestly — no budget inflation, no escalation suppression).
Validated by the §4/§5 protections + a bounded real-LLM **two-sample V3 regression** gate
against the WP1-measured baseline.

## 5. WP1 CaseSpec ground truth (human-reviewable — REQUIRES bless before encoding)

The full human-reviewable ground truth for the new characterization CaseSpec + the three
controls + the adjudication rubric is specified in `docs/sprint_objective.md` §3–§6. The
new case is a **measurement instrument**, not a curated regression guard: it reproduces
the *natural* conditions (viable UC-A hit available; a simple follow-up answerable from
the retrieved/contextual evidence) under which a non-converging loop would reach
MAX_STEPS — **without** engineering a benchmark-specific loop or scripting re-search
(that is a STOP condition, §6). The human blesses the ground truth before WP1 encodes the
YAML.

## 6. Milestone acceptance bar (characterization-anchored)

1. **WP1 (hard):** a human-reviewable characterization verdict classifying the
   reach-MAX_STEPS-despite-viable-hits behaviour as **load-bearing / intermittent /
   isolated**, with a per-attempt attribution table on the bounded V3-cadence sample, the
   convergent-resolve and legitimate-escalate controls behaving as expected (the
   convergent control does NOT reach MAX_STEPS at a load-bearing rate; the
   legitimate-escalate control's MAX_STEPS is NOT misclassified as avoidable), the
   anti-误杀 negative control not regressing, and safety/grounding hard floors green on
   every draw. WP1 ships no behaviour.
2. **Decision gate (hard):** WP2 is scoped **only if** WP1 is load-bearing AND causally
   coherent (the avoidable terminals are explained by repeated/equivalent retrieval
   consuming the step budget with viable hits already present — not by no-hit, genuine
   unresolution, simulator continuation noise, or provider instability). If WP1 is
   isolated/non-actionable, the milestone **closes NO-KEEP** and the item is folded as an
   observation (no WP2/WP3).
3. **WP2 (conditional, hard):** a human + Codex-blessed design verdict routing to a WP3
   charter or to close.
4. **WP3 (conditional, hard):** the runtime change passes the §4/§5 protections + the
   two-sample V3 regression gate AND preserves failure honesty (genuinely-unresolved
   flows still escalate honestly).
5. **Milestone close (hard):** the §5.6 curated bad-case suite rerun shows no regression
   vs the M-Auto-10 close baseline, safety+grounding hard floors 100%; milestone-shared
   Codex `pass`/0.

## 7. Non-goals (explicit)

- **NOT a post-satisfaction / semantic_planner-posture milestone** — that hypothesis is
  disproven; SATISFIED `user_state` is NOT a required runtime precondition for the defect
  hypothesis (it is eval context only).
- **NOT a re-label of `turn_budget_exhausted`** — honest labeling is already delivered
  (B1 / S-Auto-14); this milestone does not touch escalation-reason semantics.
- **NOT a budget increase** — raising `maxToolSteps`/`max_turns` is forbidden (a STOP
  condition); a non-converging loop is not fixed by giving it more rope.
- **NOT escalation suppression** — silencing the escalation without preserving failure
  honesty is forbidden (a STOP condition).
- **WP0 `source_ids`/promotion coupling — HELD, OUT of path.**
- **OQ-S99.1 (Gate-D measurement completeness), one-turn-DISCOVER** — separate; not in
  M-Auto-11 unless direct causal evidence emerges (none so far).
- **doc-governance + action-bank retention sweep** — housekeeping, not milestone scope.

## 8. Hard fences (milestone level)

- **WP1 is characterization-only:** it adds exactly one new CaseSpec (human-blessed) +
  runs a bounded eval + writes a findings doc. It touches **no** runtime, **no**
  `AgentRunLoopImpl`, **no** `maxToolSteps`/`max_turns`, **no** `PhaseEvaluator` terminal
  behaviour, **no** escalation-reason semantics, **no** `ResolveDispositionEvaluator`,
  **no** premature-resolve guard, **no** `isResolvedSuccessTerminal`, **no** PRIMARY
  CaseSpecs, **no** scoring/baseline/canonical pointers, **no** simulator behaviour.
- **WP2 is design-only** and **WP3 is not pre-authorized** (see §4).
- **Frozen surfaces** are touched only in WP3-if-route-(a), with full §4/§5 protections +
  §7 stanza + human sign-off.
- No Tier-0 invention without `human_review_required`.

## 9. R-items consumed / surfaced

- **Consumed:** the reclassified `R-post-satisfaction-mechanical-over-escalation`
  (reframed as the loop-convergence investigation; `action_bank.md` §5) + the open
  behavioural half of the MAX_STEPS-misstamp / retry-storm family
  (`R-runtime-paraphrase-storm-search-knowledge` cross-turn rank-1 residual; the
  escalate-despite-viable-hits behaviour).
- **Explicitly NOT consumed (stay open):** WP0 source_ids coupling (HELD); OQ-S99.1;
  one-turn-DISCOVER observation.

## 10. Next valid ids

- WP1: **Sprint 101 / S-Auto-49** (verified free: no `sprint-101`, no `S-Auto-49`).
- WP2 (reserved, conditional): **Sprint 102 / S-Auto-50**.

## 11. Estimated duration (informational)

~1–3 sub-sprints. WP1 is the only committed sub-sprint. WP2 (+WP3) materialize only on a
load-bearing WP1 verdict. If WP1 is isolated, the milestone closes after WP1.
