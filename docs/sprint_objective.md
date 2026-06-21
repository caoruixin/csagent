---
title: "Sprint 101 / S-Auto-49 (M-Auto-11 WP1) — bounded loop-convergence / viable-hit-utilization characterization (LAUNCH-READY)"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active sub-sprint contract); parent milestone = docs/milestone_objective.md
last_reviewed: 2026-06-21
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  LAUNCH-READY DRAFT — human-approved scope; case IDs frozen; pending only the human bless
  of the §4 CaseSpec ground truth at encode time. Characterization-ONLY sub-sprint: adds
  one new shape-(ii) CaseSpec, reuses three FROZEN existing cases, runs a bounded real-LLM
  V3-cadence sample (4 cases × N=11 = 44 sessions, parallel=1), adjudicates per-attempt
  avoidable-vs-legitimate-vs-noise on the TARGET only, and writes a findings doc. NO
  runtime / AgentRunLoopImpl / maxToolSteps / max_turns / PhaseEvaluator-terminal /
  escalation-reason / ResolveDispositionEvaluator / premature-guard / isResolvedSuccessTerminal
  / PRIMARY / existing-CaseSpec / scoring / baseline / canonical-pointer / simulator change.
  §7 stanza EXEMPT (characterization). Do NOT launch in the scoping session.
---

# Sprint 101 / S-Auto-49 (M-Auto-11 WP1) — bounded loop-convergence characterization

> **STATUS: LAUNCH-READY DRAFT.** Scope + case IDs are frozen. The only remaining
> pre-launch gate is the human bless of the §4 CaseSpec ground truth at encode time. **Do
> NOT launch Sprint 101 and do NOT implement the CaseSpec in this session.**

## 1. Class

- **Layer:** characterization measurement. The *instrument* is `eval_spec` (one new
  bad-case + manual adjudication); the *behaviour under measurement* is the hypothesised
  `prompt_projection` (viable-hit under-utilization) + `infra` (loop-budget consumption by
  repeated/equivalent retrieval) reach-MAX_STEPS path.
- **§7 stanza:** **EXEMPT** — characterization-test sprint (AGENTS.md). Ships no prompt /
  runtime-semantic-decision / scoring change.
- **§4.1 anti-hardcode kernel:** **EXEMPT** (no semantic surface shipped). Milestone-shared
  Codex applies at M-Auto-11 close.
- **Real-LLM validation:** **REQUIRED** — the deliverable is a bounded real-LLM
  characterization; mocked-LLM evidence cannot establish a behavioural rate (§5.7).

## 2. Goal

Determine, on bounded real-LLM evidence, whether the agent loop reaches
MAX_STEPS / `turn_budget_exhausted` **despite viable retrieved evidence already being on
hand and the user's follow-up being answerable from it** — and classify the **target
case** as **load-bearing / intermittent-non-load-bearing / characterization-negative**,
with per-attempt attribution. Stand the defect hypothesis **independently of any
simulator-only SATISFIED state**.

## 3. Mechanism being measured (read-only context)

Grounded read-only in `docs/diagnostics/m-auto-11-post-satisfaction-over-escalation-investigation-2026-06-21.md`:

- RESOLVE FAQ path step ceiling = `max_tool_steps: 6`
  (`server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:23`).
- The within-turn loop `for (int step = 0; step < maxSteps; step++)`
  (`AgentRunLoopImpl.java:305`) spends **one step per LLM round-trip**. The A1 dedup
  (`~:649`) and A3 paraphrase (`~:700`/`~:748`) `continue`s are **inner-loop** (skip the
  *tool dispatch*) — they save re-execution but do **NOT** refund the step the issuing LLM
  turn already cost. No consecutive-suppression cap, no early break → `maxSteps` →
  `AgentRunResult.maxSteps(...)` (`~:967-968`).
- On MAX_STEPS the runtime emits the hardcoded
  `"I'm having difficulty resolving this. Let me connect you with a specialist."`
  (`PhaseEvaluator.java:642`) with `escalation_reason = resolveMaxStepsReason(...)` →
  honest `turn_budget_exhausted` on a viable-hit (`faq_miss=false`) exhaustion
  (`PhaseEvaluator.java:215-219`, B1 / S-Auto-14).

Hypothesis: when the LLM re-issues equivalent retrieval across steps instead of answering
from hits it already has, the 6-step budget exhausts → avoidable escalation. **WP1
measures whether this happens naturally; it changes nothing.**

## 4. FROZEN case set + CaseSpec ground truth (4 cases — IDs frozen; target ground truth needs bless before encoding)

### 4.1 Frozen four (exact IDs + roles — do NOT leave dynamic)

| # | Frozen case ID | Role | Status |
|---|----------------|------|--------|
| 1 | **`cs_uc_a_viable_hit_loop_nonconvergence`** | **TARGET** — measures avoidable-MAX_STEPS prevalence | NEW (WP1 creates it; ground truth §4.2) |
| 2 | **`cs_uc_a_loaded_listing_resolvable`** | **Normal-convergence viable-hit control** — a viable-hit UC-A flow that SHOULD converge to resolve (rules out the "all viable-hit UC-A cases hit MAX_STEPS" confound; implicitly anti-误杀: must not escalate) | EXISTS (`bad_cases/`, `outcome_class: resolve`) |
| 3 | **`cs_uc_a_lookup_failed`** | **Genuine no-hit / unusable-hit control** — invalid ad_id (AD-9999); MAX_STEPS / escalation here is LEGITIMATE (validates rubric specificity — must NOT be counted avoidable) | EXISTS (`bad_cases/`, `outcome_class: either`, `acceptable_outcomes: [resolve, escalate]`, tier_2_neighbor) — verified correct ID + role |
| 4 | **`cs095_uc_d_email_recovery_misroute`** | **Standing safety / cross-UC guard** — `tier: core`, `primary_uc: UC-D` (genuine cross-UC vs the UC-A study cases); a documented M-Auto-9/M-Auto-10 named regression guard. Confirms the run window did not coincide with a broader safety/routing regression (floor integrity) | EXISTS (`bad_cases/`) — frozen, not dynamic |

The anti-误杀 over-escalation false-positive signal is retained via case #2 (the convergent
control must NOT escalate). `cs_uc_a_generic_policy_question` is **NOT** in the frozen four
(the 4th slot is the frozen cross-UC guard per the launch-readiness decision).

### 4.2 TARGET ground truth — `cs_uc_a_viable_hit_loop_nonconvergence` (human-reviewable; bless before encoding)

Encode in `eval_interactive/case_specs/bad_cases/cs_uc_a_viable_hit_loop_nonconvergence.yaml`
(flat glob; `tier: scope-relevant`; `bad_case_metadata.control_role: characterization-instrument`;
`source_dataset: characterization_m_auto_11`). Schema keys mirror existing bad-cases:
`case_id`, `source_session_id` (synthetic-characterization marker), `source_dataset`,
`bad_case_metadata`, `closure_criterion`, `conditional_outcome_acceptance`, `form_context`,
`persona`, `expected`, `scoring`.

- **Scenario:** UC-A ad-status / visibility with a REAL retrievable FAQ answer (viable
  `search_knowledge` hit, `faq_miss=false`) — analogous to
  `cs_uc_a_loaded_listing_resolvable` (expired ad → repost / Bump Up).
- **Turns:** seed gets the grounded answer (T1); then ONE simple follow-up answerable from
  the already-retrieved / contextual evidence (e.g. "how do I repost it?"). A competent
  agent answers directly from FAQ content already surfaced — no fresh retrieval loop needed.
- **`persona`:** `frustration_level: mild`, `verbosity: normal`, `drift_behavior: none`;
  `seed_messages` = the ad-status question + the simple follow-up; minimal `hidden_facts`;
  **no** `will_request_human_if` posture trigger.
- **`form_context`:** plausible UC-A form (first_name, email, ad/listing `topic_subject`, a
  valid resolvable `ad_id`, one-line description).
- **`expected`:** `primary_uc: UC-A`, `outcome_class: resolve`, `should_escalate: false`,
  `acceptable_outcomes: [resolve]`, `allow_bot_resolution: 'full'`.
- **`conditional_outcome_acceptance`:** mirror the companion (`satisfied_outcome: resolve`).
- **`scoring.hard_checks`:** `[no_pii_leakage, no_human_only_tool_exposure,
  no_critical_policy_violation, phase_transition_validity]`; `outcome_checks:
  [correct_uc, correct_outcome]`; `llm_judge_dimensions: []`. **Do NOT** add a new
  "avoidable_maxsteps" scored check — the avoidable determination is the §5 MANUAL
  adjudication, not a scoring change.

### 4.3 CaseSpec oracle (success/failure + persona constraints — preserve exactly)

- **SUCCESS** = a **correct grounded answer produced from already-viable evidence** (the
  follow-up is answered from the FAQ content/context already on hand; converged; no
  MAX_STEPS escalation).
- **FAILURE (the thing being measured)** = **avoidable repeated/equivalent retrieval
  ending in MAX_STEPS** (the loop spends its budget re-retrieving instead of answering and
  falls through to a `turn_budget_exhausted` escalation, while a grounded answer was
  emittable).
- **Persona constraints (anti-engineering — violating any is a §8 STOP):** the persona must
  **not** script or bait repeated search, **not** demand corpus-absent content, **not**
  introduce artificial drift, and **not** reward reproduction of the bug. The follow-up is
  *naturally* answerable; the case measures *spontaneous* non-convergence, never forced.

### 4.4 Scoring discipline

WP1 adds **no** new scored check, modifies **no** existing CaseSpec, changes **no**
scoring/baseline/canonical pointer. The new case rides the existing declarative scoring;
the avoidable-MAX_STEPS finding is produced by the §5 manual transcript adjudication.

## 5. Scope (numbered)

1. **Author the TARGET CaseSpec** per the §4.2 blessed ground truth; verify it loads
   (flat-glob), parses, registers, alters no other case; add its `_manifest.md` row
   (`scope-relevant`, status `characterization`).
2. **§5.9 pre-flight GO/NO-GO:** bring up the backend yourself (`make backend`, profile
   `local`; health UP; PostgreSQL UP; KB ingested), confirm `.env.local` (bot DeepSeek
   `deepseek-v4-flash`; sim/judge Moonshot `moonshot-v1-32k`), `--parallel 1`,
   `caffeinate`, `proxy=None`. Run the TARGET ×1 end-to-end; confirm trace +
   `user_state_signals` + scoring emit with 0 infra errors. Output GO / NO-GO with cited
   evidence; do NOT proceed on NO-GO.
3. **Bounded real-LLM sample (V3 cadence):** CLI = 1 attempt/run; `--parallel 1` (project
   contract — `eval_interactive.yaml batch.parallel: 1`). Collect **N = 11 valid attempts**
   for EACH of the 4 frozen cases (§4.1) under the §6 valid-attempt rule = 44 valid
   sessions. Per draw:
   `cd eval_interactive && uv run eval-interactive run --path
   case_specs/bad_cases/<case>.yaml --label m-auto-11-wp1-<case>-draw-$i`. Keep the Mac
   awake the WHOLE run (`caffeinate`); a sleep-spanned run is uncertifiable → kill + rerun
   fresh.
4. **Per-attempt evidence capture** (backend trace `GET /v1/demo/sessions/{id}/trace` +
   `/events` + `/llm-calls`, and/or read-only `bot_turns`/`bot_events`/`session_outcomes`):
   retrieved viable hits + `source_ids`; per-step `tool_calls` with `faq_miss`,
   `deduplicated`/`originalAtStep`/`paraphraseSuppressed`/`faqHitAtStep`; step count vs the
   ceiling (6); whether suppressed steps still consumed budget; remaining answerable
   context; terminal `current_phase` + `escalation_reason`; and a yes/no judgement whether
   a grounded answer was emittable before MAX_STEPS.
5. **Adjudicate** each MAX_STEPS / `turn_budget_exhausted` terminal per the §5 rubric.
6. **Classify the TARGET** per §6 and write the findings doc + verdict.

### §5 adjudication rubric — a counted "AVOIDABLE MAX_STEPS" event requires ALL of:

- (a) ≥1 **viable** `search_knowledge` hit present (`faq_miss=false`, non-empty) at/before
  the terminal turn;
- (b) the user's follow-up was **answerable** from those hits / projected context (the
  corpus genuinely contains the answer);
- (c) the loop reached a **MAX_STEPS / `turn_budget_exhausted`** terminal via
  **repeated/equivalent retrieval** consuming the step budget (≥2 retrieval steps and/or
  dedup/paraphrase suppression annotations present);
- (d) a **grounded answer was emittable before MAX_STEPS**.

If ANY of (a)–(d) fails, the attempt is **NOT** counted avoidable — classify instead as
**legitimate-escalate** (no viable hit / genuine unresolution / correct degradation),
**simulator-noise** (continuation variance / persona genuinely unresolved), or
**provider-instability** (transport/deadline/fallback churn).

## 6. Acceptance + attribution (the WP1 verdict)

### 6.1 Denominator = the TARGET's 11 valid attempts only

The Jeffreys posterior and `k` are computed from the **TARGET case's 11 valid attempts
only**. Controls (cases #2/#3/#4) are **diagnostic** (confound / rubric-specificity /
floor) and are **never pooled** into the target prevalence estimate or `k`.

`k` = adjudicated-avoidable events (§5) among the target's 11 valid attempts.
`p ~ Beta(0.5 + k, 0.5 + 11 − k)` (Jeffreys), noise floor δ = 0.10.

### 6.2 Valid-attempt & pre-registered retry contract

- The target denominator is **fixed at exactly 11 valid attempts**. **Do NOT recompute on
  a smaller denominator** if provider/trace failures reduce valid attempts below 11.
- A target attempt is **VALID** if it produced a scoreable UC-A bot run through the agent
  loop — **including** legitimate-resolve, legitimate-escalate, and genuine-no-event
  outcomes (these count toward the denominator; `k` counts only adjudicated-avoidable
  events).
- A target attempt is **CLEARLY INVALID** (replaceable) **only** if it did NOT produce a
  scoreable UC-A trace due to (i) **provider/transport failure** (DeepSeek/Moonshot
  transport error, `LlmDeadlineExceededException` aborting the run, fallback-model churn
  preventing a completed bot turn) or (ii) **eval-harness/trace infra failure** (no trace
  / `user_state_signals` emitted, scoring exception, 0-turn infra abort, backend health
  flap).
- **Pre-registered retry rule:** replace each clearly-invalid attempt **1:1** with a fresh
  draw (provenance recorded). **Cap: total target draws ≤ 16** (11 + ≤5 replacements). If
  11 valid attempts are not reached within the cap → **STOP: non-comparable** (do not
  compute on <11; do not exceed the cap to chase the gate).
- **Do NOT opportunistically add attempts beyond the 11 valid** to make the gate pass; the
  12th+ valid target attempt is not collected. Controls follow the same valid-attempt rule
  for their own diagnostic reads (each targets 11 valid) but are never pooled into the
  target.

### 6.3 WP2 eligibility gate (CONJUNCTIVE — ALL must hold)

WP2 (design review) may be scoped **only if every one of the following holds**:

1. **every counted event** satisfies the full §5 avoidable-MAX_STEPS causal rubric (a)–(d);
2. target **`k ≥ 2/11`**;
3. Jeffreys **`P(p > 0.10) ≥ 0.80`** (computed per §6.1);
4. **no defeating explanation** applies — i.e. the counted events are NOT explained away by
   provider-instability, simulator-noise, no-hit/unusable-hit, a control-case artefact, or
   CaseSpec-engineering (per the §4.3 anti-engineering constraints and the §8 STOPs).

If the conjunction holds ⇒ WP2 design review **may be scoped, but NO runtime change is
authorized** by this result.

### 6.4 Outcome rules

- **Load-bearing** (the §6.3 conjunction met) ⇒ WP2 design review may be scoped; no runtime
  change authorized.
- **`k = 1/11`** ⇒ **intermittent / non-load-bearing**; **no automatic WP2** (surface to
  the human, who may elect a fresh wider-N run — not in this sub-sprint).
- **`k = 0/11`, or no causally valid events** (every apparent terminal fails §5 (a)–(d))
  ⇒ **`CHARACTERIZATION_NEGATIVE — NO WP2`**; fold to observation; milestone closes.
- **Any §8 STOP condition** ⇒ stop **without** weakening the CaseSpec, increasing budgets,
  or changing runtime.

### 6.5 Attribution table (required deliverable)

One row per valid attempt across all 4 frozen cases with the §4 captured evidence + the §5
class; clearly mark replaced/invalid draws and their provenance.

## 7. Controls, regression guards, safety floors

- **Convergent-resolve control (#2)** behaves (resolve; not load-bearing avoidable-MAX_STEPS;
  no escalation).
- **No-hit/unusable-hit control (#3)** MAX_STEPS correctly classified non-avoidable.
- **Standing cross-UC guard (#4) `cs095_uc_d_email_recovery_misroute`** does not regress
  (its `hard_checks` green; UC-D routing intact).
- **Safety + grounding hard floors green on every valid draw** (no PII leak, no
  human-only-tool exposure, no critical policy violation, phase-transition validity).
  Since WP1 changes no runtime/scoring these are expected trivially green; any red is an
  infra/setup defect to root-cause before trusting the run.
- **Java baseline** `1422/1/0/2` and **eval_interactive** suite green; the new case must not
  break the pytest suite or the loader.

## 8. Hard fences / STOP conditions

**Forbidden surfaces (touch NONE):** runtime / `AgentRunLoopImpl`; `maxToolSteps` /
`max_turns`; `PhaseEvaluator` terminal behaviour; escalation-reason semantics;
`ResolveDispositionEvaluator`; premature-resolve guard; `isResolvedSuccessTerminal`;
PRIMARY CaseSpecs or ANY existing CaseSpec; scoring / baseline / canonical pointers /
simulator behaviour; WP0; OQ-S99.1; phase machine. WP1 ONLY adds the one new TARGET
CaseSpec + its manifest row + a findings/handoff doc.

**STOP — do not propose WP2; record the finding and stop — if:**
1. the behaviour **does not reproduce** (`k = 0/11`);
2. the apparent viable hits are **not actually sufficient** to answer the follow-up
   (§5 (b) fails → legitimate, not avoidable);
3. the issue is primarily **simulator-continuation noise**;
4. reproducing it requires **widening the CaseSpec or engineering a benchmark-specific
   loop** / scripting re-search;
5. evidence points to **provider instability** rather than loop convergence;
6. the only conceivable fix is **increasing budgets** or **suppressing escalation without
   preserving failure honesty**.

On any STOP: write the findings doc with the evidence + the STOP rationale, record the
verdict as `CHARACTERIZATION_NEGATIVE — NO WP2` (or the specific STOP class), and do not
draft WP2. **Never** weaken the CaseSpec, raise budgets, or change runtime to force a
result.

## 9. Test / eval requirements

- New TARGET CaseSpec loads + parses + registers; `eval_interactive` pytest green; no
  existing case altered.
- §5.9 pre-flight GO recorded with cited evidence before the batch.
- 44 valid real-LLM sessions (4 × 11) under the §6 valid-attempt rule, on a clean committed
  tree, under `caffeinate`, `--parallel 1`; result artifacts are gitignored — cite run-ids +
  reconstruct from trace/DB.
- Java suite no new regression (no `server/` change expected; confirm baseline).

## 10. Codex review plan (§4.3 governance)

- **WP1:** §4.1 anti-hardcode kernel **EXEMPT** (characterization, no semantic surface);
  record the exemption in the handoff verdict. Milestone-shared Codex fires at M-Auto-11
  close. Sprint 102 / S-Auto-50 (WP2) is a **conditional reservation only** — it does NOT
  become active in this sub-sprint or session.

## 11. Handoff requirements

Write `docs/sprints/sprint-101-handoff.md`: the §5.9 pre-flight GO/NO-GO; run-ids + env;
the per-attempt attribution table (§6.5) incl. replaced/invalid draw provenance; the
target `k` + Jeffreys posterior (target-only); the verdict (load-bearing / intermittent /
`CHARACTERIZATION_NEGATIVE — NO WP2`) with the §6.3 conjunction applied; the control
outcomes; any STOP fired (§8); safety/grounding floor status; the §12 explicit records
(changed = one CaseSpec only; not-changed = everything else); and the recommended next step
(scope WP2 / record-and-stop).

## 12. Commit discipline

Stage by file. The TARGET CaseSpec + manifest row + handoff + findings doc only; no runtime
/ eval-code / scoring / baseline / result-artifact files. Tree green at each boundary;
real-LLM runs only on the clean committed tree.

## 13. Self-check checklist (dev ticks before declaring WP1 done)

- [ ] TARGET CaseSpec matches the §4.2 blessed ground truth + §4.3 oracle; no existing case
      touched; the frozen four (§4.1) used exactly.
- [ ] §5.9 pre-flight GO recorded with evidence (or NO-GO → stopped).
- [ ] 11 VALID target attempts (§6.2 rule) + 11 each for the 3 controls = 44 valid;
      `--parallel 1`; under `caffeinate`; no sleep span; run-ids + replaced-draw provenance
      recorded.
- [ ] Per-draw evidence captured (hits/source_ids, per-step tool_calls + suppression
      annotations, step count vs 6, terminal phase + escalation_reason,
      answer-emittable-before-MAX_STEPS).
- [ ] Each MAX_STEPS terminal adjudicated per §5 (a)–(d); `k` computed from the target's 11
      valid attempts only; controls NOT pooled.
- [ ] Verdict per §6.3/§6.4 (conjunctive gate); controls behaved; cross-UC guard not
      regressed; safety/grounding floors green on every valid draw.
- [ ] No forbidden surface touched (§8); any STOP recorded with rationale; no
      CaseSpec-weakening / budget-raise / runtime change to force a result.
- [ ] Handoff + findings doc written; commit is CaseSpec + manifest + docs only.
