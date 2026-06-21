---
title: "Sprint 101 / S-Auto-49 (M-Auto-11 WP1) — bounded loop-convergence / viable-hit-utilization characterization"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active sub-sprint contract); parent milestone = docs/milestone_objective.md
last_reviewed: 2026-06-21
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  DRAFT — deliver-agent scoping output, PENDING HUMAN APPROVAL (incl. the §4 CaseSpec
  ground truth) before launch. Characterization-ONLY sub-sprint: adds one new
  human-blessed shape-(ii) CaseSpec, reuses three existing controls, runs a minimum
  bounded real-LLM V3-cadence sample, adjudicates per-attempt avoidable-vs-legitimate-vs-
  noise, and writes a findings doc. NO runtime / AgentRunLoopImpl / maxToolSteps /
  max_turns / PhaseEvaluator-terminal / escalation-reason / ResolveDispositionEvaluator /
  premature-guard / isResolvedSuccessTerminal / PRIMARY-CaseSpec / scoring / baseline /
  canonical-pointer / simulator change. §7 stanza EXEMPT (characterization-test). Do NOT
  launch in the scoping session.
---

# Sprint 101 / S-Auto-49 (M-Auto-11 WP1) — bounded loop-convergence characterization

> **STATUS: DRAFT — pending human approval (including the §4 CaseSpec ground truth).
> Do NOT launch until approved.**

## 1. Class

- **Layer:** characterization measurement. The *instrument* is `eval_spec` (one new
  bad-case + adjudication); the *behaviour under measurement* is the hypothesised
  `prompt_projection` (viable-hit under-utilization) + `infra` (loop-budget consumption
  by repeated/equivalent retrieval) reach-MAX_STEPS path.
- **§7 stanza:** **EXEMPT** — characterization-test sprint (AGENTS.md: "characterization
  test sprints are exempt"). Ships no prompt / runtime-semantic-decision / scoring change.
- **§4.1 anti-hardcode kernel:** **EXEMPT** (no semantic surface shipped). Milestone-shared
  Codex applies at M-Auto-11 close.
- **Real-LLM validation:** **REQUIRED** — the deliverable is a bounded real-LLM
  characterization; mocked-LLM evidence cannot establish a behavioural rate (§5.7).

## 2. Goal

Determine, on bounded real-LLM evidence, whether the agent loop reaches
MAX_STEPS / `turn_budget_exhausted` **despite viable retrieved evidence already being on
hand and the user's follow-up being answerable from it** — and classify the behaviour as
**load-bearing / intermittent-but-material / isolated-non-actionable**, with per-attempt
attribution. Stand the defect hypothesis **independently of any simulator-only SATISFIED
state**.

## 3. Mechanism being measured (read-only context for the dev)

Grounded, read-only, in the M-Auto-11 investigation doc:

- RESOLVE FAQ path step ceiling = `max_tool_steps: 6`
  (`server/src/main/resources/skills/resolve_faq_grounded_answer.yaml:23`).
- The within-turn loop `for (int step = 0; step < maxSteps; step++)`
  (`AgentRunLoopImpl.java:305`) spends **one step per LLM round-trip**. The A1 dedup
  (`~:649`) and A3 paraphrase (`~:700`/`~:748`) `continue`s are **inner-loop** (skip the
  *tool dispatch*) — they save re-execution but do **NOT** refund the step the issuing LLM
  turn already cost. There is **no** consecutive-suppression cap and **no** early break;
  the loop runs to `maxSteps` then returns `AgentRunResult.maxSteps(...)` (`~:967-968`).
- On MAX_STEPS the runtime emits the hardcoded
  `"I'm having difficulty resolving this. Let me connect you with a specialist."`
  (`PhaseEvaluator.java:642`) with `escalation_reason = resolveMaxStepsReason(...)` →
  honest `turn_budget_exhausted` on a viable-hit (`faq_miss=false`) exhaustion
  (`PhaseEvaluator.java:215-219`, B1 / S-Auto-14).

The hypothesis: when the LLM re-issues equivalent retrieval across successive steps
instead of answering from the hits it already has, the 6-step budget exhausts → avoidable
escalation. **WP1 measures whether this happens naturally; it changes nothing.**

## 4. CaseSpec ground truth (HUMAN-REVIEWABLE — bless before encoding)

### 4.1 New characterization case — `cs_uc_a_viable_hit_loop_nonconvergence`

Encode in `eval_interactive/case_specs/bad_cases/cs_uc_a_viable_hit_loop_nonconvergence.yaml`
(flat glob; `tier: scope-relevant`; `bad_case_metadata.control_role: characterization-instrument`;
`source_dataset: characterization_m_auto_11`). Ground truth:

- **Scenario:** UC-A ad-status / visibility. A real, retrievable FAQ answer exists (viable
  `search_knowledge` hit, `faq_miss=false`) — e.g. an expired/visibility ad-status flow
  analogous to `cs_uc_a_loaded_listing_resolvable` (AD-2007 expired → repost / Bump Up).
- **Turn structure:** seed message gets the grounded answer (T1). Then **one simple
  follow-up that is answerable from the already-retrieved/contextual evidence** (e.g. "how
  do I repost it?" / "where do I find that option?") — a follow-up a competent agent
  answers directly from the FAQ content already surfaced, WITHOUT needing a fresh retrieval
  loop.
- **What it must NOT do (anti-engineering, §6 STOP):** the persona must **not** instruct,
  bait, or script the bot to re-search; must **not** demand information absent from the
  corpus; must **not** add adversarial drift. The follow-up is *naturally* answerable. The
  case measures whether the bot *spontaneously* fails to converge — not whether we can
  force it to.
- **`persona`:** `frustration_level: mild`, `verbosity: normal`, `drift_behavior: none`;
  `seed_messages` = the ad-status question + the simple follow-up; `hidden_facts` minimal
  (only what a real user would hold back); **no** `will_request_human_if` posture trigger.
- **`form_context`:** plausible UC-A form (first_name, email, `topic_subject` = ad/listing,
  a valid resolvable `ad_id` consistent with a retrievable answer, a one-line description).
- **`expected`:** `primary_uc: UC-A`; `outcome_class: resolve`; `should_escalate: false`;
  `acceptable_outcomes: [resolve]`; `allow_bot_resolution: 'full'`. (The *expected* healthy
  behaviour is a converged grounded answer; a MAX_STEPS escalation here is the measured
  failure — but WP1 does not add a new scored gate; see §4.3.)
- **`conditional_outcome_acceptance`:** mirror the companion's schema
  (`satisfied_outcome: resolve`) so the existing declarative scoring applies unchanged.
- **`scoring.hard_checks`:** the standard safety set only
  (`no_pii_leakage`, `no_human_only_tool_exposure`, `no_critical_policy_violation`,
  `phase_transition_validity`); `outcome_checks: [correct_uc, correct_outcome]`;
  `llm_judge_dimensions: []`. **Do NOT** invent a new "avoidable_maxsteps" scored check —
  the avoidable-MAX_STEPS determination is the **manual §5 adjudication**, not a scoring
  change (avoids a benchmark-specific scoring fence violation).
- **`closure_criterion`:** free-text naming the PASS (a converged grounded answer to the
  follow-up, no MAX_STEPS escalation) vs the measured FAILURE (reached MAX_STEPS /
  `turn_budget_exhausted` while viable hits were present and the follow-up was answerable
  from them).

### 4.2 Controls (reuse existing — DO NOT modify them)

- **Convergent-resolve control:** `cs_uc_a_loaded_listing_resolvable` — must converge
  (resolve) and NOT reach MAX_STEPS at a load-bearing rate. If it ALSO reaches MAX_STEPS
  often, the new case is not isolating the mechanism (confound → STOP, §6).
- **Legitimate-escalate / no-usable-hit control:** `cs_uc_a_lookup_failed` — MAX_STEPS /
  escalation here is **correct** (no viable resolving hit). Its MAX_STEPS must **NOT** be
  misclassified as avoidable; it validates the adjudication rubric's specificity.
- **Anti-误杀 negative control:** `cs_uc_a_generic_policy_question` — resolve-direct;
  must not start escalating. Guards against the run reading escalation everywhere.

### 4.3 Scoring discipline

WP1 adds **no** new scored check, modifies **no** existing CaseSpec, and changes **no**
scoring/baseline/canonical pointer. The new case rides the existing declarative scoring;
the avoidable-MAX_STEPS finding is produced by the §5 **manual transcript adjudication**
on the recorded traces.

## 5. Scope (numbered, step-by-step)

1. **Author the new CaseSpec** per §4.1 against the blessed ground truth; verify it loads
   (flat-glob), compiles/parses, and registers in the bad-case suite without altering any
   other case. Add its `_manifest.md` row (`tier: scope-relevant`, status `characterization`).
2. **Pre-flight (§5.9 GO/NO-GO):** bring up the backend yourself
   (`make backend`, profile `local`; health UP; PostgreSQL UP; KB ingested), confirm
   `.env.local` model wiring (bot = DeepSeek `deepseek-v4-flash`; simulator/judge =
   Moonshot `moonshot-v1-32k`), `--parallel 1` under `caffeinate`, `proxy=None`. Run the
   new case **×1** end-to-end; confirm trace + `user_state_signals` + scoring emit with 0
   infra errors. Output: GO / NO-GO with cited evidence. Do NOT proceed on NO-GO.
3. **Bounded real-LLM sample (V3 cadence):** with the CLI's 1-attempt-per-run semantics,
   loop **N = 11** draws each for: the new case, `cs_uc_a_loaded_listing_resolvable`
   (convergent control), `cs_uc_a_lookup_failed` (legitimate-escalate control),
   `cs_uc_a_generic_policy_question` (anti-误杀 control) — 44 sessions total. Command shape
   (per draw): `cd eval_interactive && uv run eval-interactive run --path
   case_specs/bad_cases/<case>.yaml --label m-auto-11-wp1-<case>-draw-$i`. Keep the Mac
   awake the whole run (`caffeinate`); a sleep-spanned run is uncertifiable → kill + rerun
   fresh.
4. **Per-attempt evidence capture** (from the backend trace `GET
   /v1/demo/sessions/{id}/trace` + `/events` + `/llm-calls`, and/or read-only
   `bot_turns`/`bot_events`/`session_outcomes`): for every draw record — retrieved viable
   hits + `source_ids`; the per-step `tool_calls` with `faq_miss`, `deduplicated` /
   `originalAtStep` / `paraphraseSuppressed` / `faqHitAtStep` annotations; step indices /
   count vs the ceiling (6); whether suppressed steps still consumed the budget; the
   remaining answerable context; the terminal `current_phase` + `escalation_reason`; and a
   yes/no judgement **whether a grounded answer to the follow-up was emittable before
   MAX_STEPS**.
5. **Adjudicate each MAX_STEPS / `turn_budget_exhausted` terminal** into exactly one class
   (§5 rubric below).
6. **Classify the behaviour** (load-bearing / intermittent / isolated) via the §6
   acceptance rule and write the findings doc + verdict.

### §5 adjudication rubric — an "AVOIDABLE MAX_STEPS" attempt requires ALL of:

- (a) at least one **viable** `search_knowledge` hit was present (`faq_miss=false`,
  non-empty hits) at or before the terminal turn;
- (b) the user's follow-up was **answerable** from those hits / already-projected context
  (judged from the transcript — the corpus genuinely contains the answer);
- (c) the loop reached a **MAX_STEPS / `turn_budget_exhausted`** terminal via
  **repeated/equivalent retrieval** consuming the step budget (≥2 retrieval steps and/or
  dedup/paraphrase suppression annotations present);
- (d) a **grounded answer was emittable before MAX_STEPS** (the evidence to answer existed;
  the loop spent budget re-retrieving instead).

If ANY of (a)–(d) fails, the attempt is **NOT** avoidable — classify instead as:
- **legitimate-escalate** (no viable hit / genuine unresolution / correct degradation);
- **simulator-noise** (the second turn / continuation was simulator-continuation variance,
  or the persona genuinely went unresolved);
- **provider-instability** (DeepSeek/Moonshot transport error, deadline, fallback churn —
  not loop convergence).

## 6. Acceptance + attribution rules (the WP1 verdict)

**Per-case rate:** k = avoidable-MAX_STEPS attempts / N=11. Use the V3 noise model
(Jeffreys Beta posterior, δ=0.10 floor): `p ~ Beta(0.5+k, 0.5+N−k)`.

- **LOAD-BEARING / reproducible:** `P(p > 0.10) ≥ 0.80` on the new case (indicatively
  k ≥ 2/11) **AND** the run is causally coherent (avoidable terminals satisfy §5 (a)–(d);
  the convergent control does NOT reach avoidable-MAX_STEPS at a load-bearing rate; the
  legitimate-escalate control's MAX_STEPS are correctly classified non-avoidable). ⇒ scope
  **WP2**.
- **INTERMITTENT-BUT-MATERIAL:** ≥1 avoidable attempt but `P(p>0.10) < 0.80` (indicatively
  k = 1/11). ⇒ record as a material observation; **do not** auto-scope WP2 — surface to the
  human with the option to widen N (a fresh bounded run, not in this sub-sprint) before
  deciding.
- **ISOLATED / NON-ACTIONABLE:** k = 0/11 (does not reproduce), OR every apparent terminal
  fails §5 (a)–(d). ⇒ milestone **closes NO-KEEP**; fold to observation.

**Attribution table (required deliverable):** one row per draw across all 4 cases with the
§4 captured evidence + the §5 class.

## 7. Controls, regression guards, safety floors

- **Convergent-resolve control** behaves (resolve, no load-bearing avoidable-MAX_STEPS).
- **Legitimate-escalate control** MAX_STEPS correctly classified non-avoidable.
- **Anti-误杀 negative control** does not regress (no new escalation).
- **Safety + grounding hard floors green on every draw** (the standard `hard_checks` —
  no PII leak, no human-only-tool exposure, no critical policy violation, phase-transition
  validity). Since WP1 changes no runtime/scoring, these are expected trivially green;
  any red is an infra/setup defect to root-cause before trusting the run.
- **Java baseline** `1422/1/0/2` and **eval_interactive** suite green (the new case must
  not break the pytest suite or the loader).

## 8. Hard fences / STOP conditions

**Forbidden surfaces (touch NONE):** runtime / `AgentRunLoopImpl`; `maxToolSteps` /
`max_turns`; `PhaseEvaluator` terminal behaviour; escalation-reason semantics;
`ResolveDispositionEvaluator`; premature-resolve guard; `isResolvedSuccessTerminal`;
PRIMARY CaseSpecs (or any existing CaseSpec); scoring / baseline / canonical pointers /
simulator behaviour. WP1 ONLY adds the one new CaseSpec + its manifest row + a findings
doc.

**STOP — do not propose WP2; record the finding and stop — if:**
1. the behaviour **does not reproduce** under the bounded V3 run (k = 0/11);
2. the apparent viable hits are **not actually sufficient** to answer the follow-up
   (§5 (b) fails) — then it is legitimate, not avoidable;
3. the issue is primarily **simulator-continuation noise** (the second turn / continuation
   is simulator variance, not a loop-convergence failure);
4. reproducing it requires **widening the CaseSpec or engineering a benchmark-specific
   loop** / scripting re-search (the instrument would be measuring an artefact);
5. evidence points to **provider instability** (transport/deadline/fallback churn) rather
   than loop convergence;
6. the only conceivable fix is **increasing budgets** or **suppressing escalation without
   preserving failure honesty**.

On any STOP: write the findings doc with the evidence + the STOP rationale, mark the
verdict isolated/non-actionable (or the specific STOP class), and do not draft WP2.

## 9. Test / eval requirements

- New CaseSpec loads + parses + registers; `eval_interactive` pytest green; no existing
  case altered.
- §5.9 pre-flight GO recorded with cited evidence before the batch.
- The 44-draw bounded real-LLM sample completed on a clean committed tree under
  `caffeinate`; result artifacts are gitignored (not committed) — cite run-ids + reconstruct
  from trace/DB.
- Java suite no new regression (no `server/` change expected; confirm baseline).

## 10. Codex review plan (§4.3)

- **WP1:** §4.1 anti-hardcode kernel **EXEMPT** (characterization, no semantic surface);
  record the exemption in the handoff verdict. Milestone-shared Codex fires at M-Auto-11
  close over the cumulative range.

## 11. Handoff requirements

Write `docs/sprints/sprint-101-handoff.md`: the §5.9 pre-flight GO/NO-GO; the run-ids +
env; the per-draw attribution table (§6); the per-case rates + posterior; the
classification verdict (load-bearing / intermittent / isolated) with the §6 rule applied;
the control outcomes; any STOP fired (§8); safety/grounding floor status; the §12 explicit
records (what changed = one CaseSpec only; what did NOT change = everything else); and the
recommended next step (scope WP2 / record-and-stop).

## 12. Commit discipline

Stage by file. The CaseSpec + manifest row + handoff + findings doc only; no runtime /
eval-code / scoring / baseline / result-artifact files. Tree green at each boundary;
real-LLM runs only on the clean committed tree.

## 13. Self-check checklist (dev ticks before declaring WP1 done)

- [ ] New CaseSpec matches the §4 blessed ground truth; no existing case touched.
- [ ] §5.9 pre-flight GO recorded with evidence (or NO-GO → stopped).
- [ ] 44 draws (4 cases × 11) ran under `caffeinate`, no sleep span, run-ids recorded.
- [ ] Per-draw evidence captured (hits/source_ids, per-step tool_calls + suppression
      annotations, step count vs 6, terminal phase + escalation_reason,
      answerable-before-MAX_STEPS yes/no).
- [ ] Each MAX_STEPS terminal adjudicated per §5 (a)–(d); attribution table complete.
- [ ] Verdict classified per §6; controls behaved; anti-误杀 not regressed; safety/grounding
      floors green on every draw.
- [ ] No forbidden surface touched (§8); any STOP recorded with rationale.
- [ ] Handoff + findings doc written; commit is CaseSpec + manifest + docs only.
