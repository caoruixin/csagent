# Dev prompt — Sprint 101 / S-Auto-49 (M-Auto-11 WP1) — LAUNCH-READY

> Self-contained executable view of `docs/sprint_objective.md` (canonical contract). If
> this prompt and the objective ever diverge, the objective wins. Scope + case IDs are
> frozen; the only remaining pre-launch gate is the human bless of the §4 CaseSpec ground
> truth at encode time.

## Role identity

You are the **dev agent for Sprint 101 / S-Auto-49 (M-Auto-11 WP1)**. One-line goal:
**run a bounded real-LLM characterization to determine whether the within-turn agent loop
reaches MAX_STEPS / `turn_budget_exhausted` despite viable retrieved evidence already on
hand and the follow-up answerable from it — then classify the TARGET case as
load-bearing / intermittent / `CHARACTERIZATION_NEGATIVE`, with per-attempt attribution.**
You change NO runtime/eval/scoring/baseline/existing-CaseSpec/simulator code; you add one
new TARGET CaseSpec, run the sample, and write the findings.

## Read order (minimal)

`AGENTS.md` (auto-loaded) + this prompt. Background (read-only):
`docs/diagnostics/m-auto-11-post-satisfaction-over-escalation-investigation-2026-06-21.md`
(why this is a loop-convergence, NOT a posture, question).

## Class

- Characterization measurement. Instrument = `eval_spec` (one new bad-case + manual
  adjudication); behaviour under measurement = hypothesised `prompt_projection` viable-hit
  under-utilization + `infra` loop-budget consumption.
- **§7 EXEMPT** (characterization). **§4.1 Codex EXEMPT** (no semantic surface).
  Milestone-shared Codex at M-Auto-11 close. **Sprint 102 / S-Auto-50 (WP2) is a
  conditional reservation only — do NOT activate it.**
- **Real-LLM validation REQUIRED** (mocked-LLM cannot establish a behavioural rate, §5.7).

## Mechanism (read-only context)

- RESOLVE FAQ step ceiling `max_tool_steps: 6` (`resolve_faq_grounded_answer.yaml:23`).
- Loop `for (int step=0; step<maxSteps; step++)` (`AgentRunLoopImpl.java:305`) spends one
  step per LLM round-trip; A1 dedup (`~:649`) / A3 paraphrase (`~:700`/`~:748`) `continue`s
  are inner-loop (save dispatch, NOT the step). No suppression cap, no early break →
  `AgentRunResult.maxSteps(...)` (`~:967`).
- MAX_STEPS emits the hardcoded `"I'm having difficulty resolving this. Let me connect you
  with a specialist."` (`PhaseEvaluator.java:642`), `escalation_reason` = honest
  `turn_budget_exhausted` on a viable-hit exhaustion (`PhaseEvaluator.java:215-219`).
- Hypothesis: the LLM re-issues equivalent retrieval across steps instead of answering from
  hits it already has → 6-step budget exhausts → avoidable escalation. **You measure this;
  you change nothing.**

## FROZEN four cases (exact IDs + roles — do NOT leave dynamic)

| # | Case ID | Role |
|---|---------|------|
| 1 | **`cs_uc_a_viable_hit_loop_nonconvergence`** | **TARGET** — NEW (you create it; ground truth below) |
| 2 | **`cs_uc_a_loaded_listing_resolvable`** | normal-convergence viable-hit control (should resolve; must NOT escalate) |
| 3 | **`cs_uc_a_lookup_failed`** | genuine no-hit/unusable-hit control (MAX_STEPS/escalate is LEGITIMATE here; must NOT be counted avoidable) |
| 4 | **`cs095_uc_d_email_recovery_misroute`** | standing safety/cross-UC guard (`tier: core`, UC-D; must not regress) |

`cs_uc_a_generic_policy_question` is NOT in the four. The anti-误杀 signal is carried by #2.

### TARGET ground truth — `cs_uc_a_viable_hit_loop_nonconvergence` (use the human-blessed version)

File `eval_interactive/case_specs/bad_cases/cs_uc_a_viable_hit_loop_nonconvergence.yaml`;
`tier: scope-relevant`; `bad_case_metadata.control_role: characterization-instrument`;
`source_dataset: characterization_m_auto_11`. Schema keys mirror existing bad-cases
(`case_id, source_session_id, source_dataset, bad_case_metadata, closure_criterion,
conditional_outcome_acceptance, form_context, persona, expected, scoring`).

- **Scenario:** UC-A ad-status/visibility with a REAL retrievable FAQ answer (viable
  `search_knowledge` hit, `faq_miss=false`) — analogous to `cs_uc_a_loaded_listing_resolvable`
  (expired ad → repost / Bump Up).
- **Turns:** seed gets the grounded answer (T1); then ONE simple follow-up answerable from
  the already-retrieved/contextual evidence (e.g. "how do I repost it?"). A competent agent
  answers directly — no fresh retrieval loop needed.
- **persona:** `frustration_level: mild`, `verbosity: normal`, `drift_behavior: none`;
  `seed_messages` = ad-status question + the simple follow-up; minimal `hidden_facts`; NO
  `will_request_human_if`.
- **form_context:** plausible UC-A form with a valid resolvable `ad_id`.
- **expected:** `primary_uc: UC-A`, `outcome_class: resolve`, `should_escalate: false`,
  `acceptable_outcomes: [resolve]`, `allow_bot_resolution: 'full'`.
- **conditional_outcome_acceptance:** mirror the companion (`satisfied_outcome: resolve`).
- **scoring.hard_checks:** `[no_pii_leakage, no_human_only_tool_exposure,
  no_critical_policy_violation, phase_transition_validity]`; `outcome_checks:
  [correct_uc, correct_outcome]`; `llm_judge_dimensions: []`. **No new "avoidable_maxsteps"
  scored check** — avoidable is the manual adjudication.

### CaseSpec oracle (preserve exactly)

- **SUCCESS** = a correct grounded answer produced from already-viable evidence (converged;
  no MAX_STEPS escalation).
- **FAILURE (measured)** = avoidable repeated/equivalent retrieval ending in MAX_STEPS (a
  grounded answer was emittable; the loop spent budget re-retrieving and escalated).
- **Persona constraints (anti-engineering — violating any is a STOP):** must NOT script/bait
  repeated search, NOT demand corpus-absent content, NOT introduce artificial drift, NOT
  reward reproduction of the bug. Naturally answerable follow-up; measure spontaneous
  non-convergence.

## Scope (step-by-step)

1. **Author the TARGET CaseSpec** per the blessed ground truth; verify it loads (flat glob),
   parses, registers, alters no other case; add its `_manifest.md` row (`scope-relevant`,
   status `characterization`).
2. **§5.9 pre-flight GO/NO-GO:** bring up backend yourself (`make backend`, profile
   `local`; health UP; PostgreSQL UP; KB ingested), confirm `.env.local` (bot DeepSeek
   `deepseek-v4-flash`; sim/judge Moonshot `moonshot-v1-32k`), `--parallel 1`,
   `caffeinate`, `proxy=None`. Run the TARGET ×1; confirm trace + `user_state_signals` +
   scoring with 0 infra errors. Record GO/NO-GO + evidence. NO-GO → stop, root-cause.
3. **Bounded sample (V3 cadence), `--parallel 1`:** CLI = 1 attempt/run; collect **N=11
   VALID attempts** for EACH frozen case (44 valid sessions) under the valid-attempt rule
   below. Per draw: `cd eval_interactive && uv run eval-interactive run --path
   case_specs/bad_cases/<case>.yaml --label m-auto-11-wp1-<case>-draw-$i`. Keep the Mac
   awake the WHOLE run (`caffeinate`); a sleep-spanned run is uncertifiable → kill + rerun.
4. **Per-attempt evidence** (backend trace `GET /v1/demo/sessions/{id}/trace` + `/events` +
   `/llm-calls`, and/or read-only `bot_turns`/`bot_events`/`session_outcomes`): viable hits
   + `source_ids`; per-step `tool_calls` with `faq_miss` + `deduplicated`/`originalAtStep`/
   `paraphraseSuppressed`/`faqHitAtStep`; step count vs ceiling 6; whether suppressed steps
   still spent budget; remaining answerable context; terminal `current_phase` +
   `escalation_reason`; yes/no answer-emittable-before-MAX_STEPS.
5. **Adjudicate** each MAX_STEPS/`turn_budget_exhausted` terminal per the rubric below.
6. **Classify the TARGET** + write findings.

### Adjudication — a counted "AVOIDABLE MAX_STEPS" event requires ALL of:

(a) ≥1 viable `search_knowledge` hit present (`faq_miss=false`, non-empty) at/before the
terminal; (b) follow-up answerable from those hits/projected context (corpus genuinely has
the answer); (c) reached MAX_STEPS via repeated/equivalent retrieval consuming budget (≥2
retrieval steps and/or suppression annotations); (d) a grounded answer was emittable before
MAX_STEPS. ANY failing ⇒ classify legitimate-escalate / simulator-noise /
provider-instability instead (NOT counted).

## Acceptance + attribution

**Denominator = the TARGET's 11 valid attempts ONLY.** Controls (#2/#3/#4) are diagnostic
(confound / rubric-specificity / floor) and are **never pooled** into the target `k` or
prevalence. `k` = adjudicated-avoidable events among the target's 11 valid attempts;
`p ~ Beta(0.5+k, 0.5+11−k)` (Jeffreys), δ=0.10.

**Valid-attempt & pre-registered retry contract:**
- Target denominator is FIXED at exactly 11 valid attempts. Do NOT recompute on a smaller
  denominator.
- VALID = a completed, scoreable UC-A bot run through the loop (incl. legitimate-resolve,
  legitimate-escalate, genuine-no-event — these count in the denominator; `k` counts only
  avoidable events).
- CLEARLY INVALID (replaceable) ONLY if no scoreable trace due to (i) provider/transport
  failure (DeepSeek/Moonshot transport error, `LlmDeadlineExceededException` abort,
  fallback churn) or (ii) eval/trace infra failure (no trace/`user_state_signals`, scoring
  exception, 0-turn infra abort, health flap).
- Replace each clearly-invalid attempt 1:1 (record provenance). **Cap total target draws ≤
  16** (11 + ≤5 replacements). If 11 valid not reached within the cap → **STOP:
  non-comparable** (do not compute on <11; do not exceed the cap to chase the gate).
- Do NOT opportunistically add attempts beyond 11 valid to flip the gate. Controls follow
  the same valid-attempt rule for their own diagnostic reads but are never pooled.

**WP2 eligibility gate (CONJUNCTIVE — ALL must hold):**
1. every counted event satisfies the full adjudication rubric (a)–(d);
2. target `k ≥ 2/11`;
3. Jeffreys `P(p > 0.10) ≥ 0.80`;
4. no defeating explanation applies (not provider-instability / simulator-noise /
   no-hit-unusable-hit / control-case artefact / CaseSpec-engineering).

Met ⇒ WP2 design review MAY be scoped, but NO runtime change is authorized.

**Outcome rules:**
- Load-bearing conjunction met ⇒ WP2 may be scoped; no runtime change authorized.
- `k = 1/11` ⇒ intermittent / non-load-bearing; NO automatic WP2 (surface to human).
- `k = 0/11` or no causally valid events ⇒ **`CHARACTERIZATION_NEGATIVE — NO WP2`**; fold to
  observation; milestone closes.
- Any STOP ⇒ stop WITHOUT weakening the CaseSpec, increasing budgets, or changing runtime.

Deliver an attribution table: one row per valid attempt (4 cases) with the evidence + class;
mark replaced/invalid draws + provenance.

## Hard fences / STOP

**Touch NONE:** runtime / `AgentRunLoopImpl`; `maxToolSteps`/`max_turns`; `PhaseEvaluator`
terminal; escalation-reason semantics; `ResolveDispositionEvaluator`; premature-resolve
guard; `isResolvedSuccessTerminal`; PRIMARY/any existing CaseSpec; scoring/baseline/
canonical pointers/simulator; WP0; OQ-S99.1; phase machine. WP1 = one new TARGET CaseSpec +
manifest row + findings/handoff doc only.

**STOP (record + stop, do NOT propose WP2) if:** (1) `k=0/11` no reproduce; (2) viable hits
not actually sufficient (rubric (b) fails → legitimate); (3) primarily simulator-continuation
noise; (4) needs CaseSpec widening / benchmark-specific loop / scripted re-search; (5)
provider instability; (6) only fix is bigger budgets or escalation suppression without
preserving failure honesty. On any STOP: write findings + rationale; verdict
`CHARACTERIZATION_NEGATIVE — NO WP2` (or the STOP class); never weaken CaseSpec / raise
budgets / change runtime to force a result.

## Test / eval

New TARGET CaseSpec loads/parses/registers; `eval_interactive` pytest green; no existing
case altered; §5.9 GO recorded; 44 VALID draws (4 × 11) under the valid-attempt rule,
`--parallel 1`, on a clean committed tree under `caffeinate` (run-ids cited; artifacts
gitignored); Java suite no new regression (`1422/1/0/2`, no `server/` change expected).

## Codex review plan

WP1 §4.1 EXEMPT (characterization) — record the exemption in the handoff. Milestone-shared
Codex at M-Auto-11 close.

## Handoff requirements

`docs/sprints/sprint-101-handoff.md`: pre-flight GO/NO-GO; run-ids + env; per-attempt
attribution table (+ replaced/invalid provenance); target `k` + Jeffreys posterior
(target-only); verdict (load-bearing / intermittent / `CHARACTERIZATION_NEGATIVE — NO WP2`)
with the conjunctive gate applied; control outcomes; any STOP fired; safety/grounding floor
status; §12 explicit records (changed = one CaseSpec only; not-changed = everything else);
recommended next step.

## Commit discipline

Stage by file: TARGET CaseSpec + manifest row + handoff + findings doc only. No runtime/
eval-code/scoring/baseline/result-artifact files. Tree green at each boundary; real-LLM
only on the clean committed tree.

## Self-check (tick before done)

- [ ] TARGET CaseSpec matches the blessed ground truth + oracle; no existing case touched;
      the frozen four used exactly.
- [ ] §5.9 pre-flight GO recorded (or NO-GO → stopped).
- [ ] 11 VALID target attempts (valid-attempt rule) + 11 each for the 3 controls = 44 valid;
      `--parallel 1`; under `caffeinate`; no sleep span; run-ids + replaced-draw provenance.
- [ ] Per-draw evidence captured (hits/source_ids, per-step tool_calls + suppression, step
      count vs 6, terminal phase + reason, answer-emittable-before-MAX_STEPS).
- [ ] Each MAX_STEPS terminal adjudicated (a)–(d); `k` from the target's 11 valid attempts
      only; controls NOT pooled.
- [ ] Verdict per the conjunctive gate; controls behaved; cross-UC guard not regressed;
      safety/grounding floors green every valid draw.
- [ ] No forbidden surface touched; any STOP recorded; no CaseSpec-weakening / budget-raise
      / runtime change to force a result.
- [ ] Handoff + findings written; commit is CaseSpec + manifest + docs only.
