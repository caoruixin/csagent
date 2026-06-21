# Dev prompt — Sprint 101 / S-Auto-49 (M-Auto-11 WP1)

> Self-contained executable view of `docs/sprint_objective.md` (canonical contract). If
> this prompt and the objective ever diverge, the objective wins. **Do NOT launch until
> the human has approved the M-Auto-11 scope AND blessed the §4 CaseSpec ground truth.**

## Role identity

You are the **dev agent for Sprint 101 / S-Auto-49 (M-Auto-11 WP1)**. One-line goal:
**run a bounded real-LLM characterization to determine whether the within-turn agent loop
reaches MAX_STEPS / `turn_budget_exhausted` despite viable retrieved evidence already
being on hand and the follow-up being answerable from it — then classify it as
load-bearing / intermittent / isolated, with per-attempt attribution.** You change NO
runtime/eval/scoring/baseline/existing-CaseSpec/simulator code; you add exactly one new
characterization CaseSpec, run the sample, and write the findings.

## Read order (minimal)

`AGENTS.md` (auto-loaded) + this prompt. Read-only code/doc anchors only as cited below.
Background (read-only): `docs/diagnostics/m-auto-11-post-satisfaction-over-escalation-investigation-2026-06-21.md`
(why this is a loop-convergence, NOT a posture, question).

## Class

- Characterization measurement. Instrument = `eval_spec` (one new bad-case + manual
  adjudication); behaviour under measurement = hypothesised `prompt_projection` viable-hit
  under-utilization + `infra` loop-budget consumption.
- **§7 stanza EXEMPT** (characterization-test sprint). **§4.1 Codex EXEMPT** (no semantic
  surface). Milestone-shared Codex at M-Auto-11 close.
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

## CaseSpec ground truth (use the human-blessed version)

### New case — `cs_uc_a_viable_hit_loop_nonconvergence`

File `eval_interactive/case_specs/bad_cases/cs_uc_a_viable_hit_loop_nonconvergence.yaml`;
`tier: scope-relevant`; `bad_case_metadata.control_role: characterization-instrument`;
`source_dataset: characterization_m_auto_11`. Schema keys (match existing bad-cases):
`case_id`, `source_session_id` (synthetic-characterization marker), `source_dataset`,
`bad_case_metadata`, `closure_criterion`, `conditional_outcome_acceptance`, `form_context`,
`persona`, `expected`, `scoring`.

- **Scenario:** UC-A ad-status/visibility with a REAL retrievable FAQ answer (viable
  `search_knowledge` hit, `faq_miss=false`) — analogous to
  `cs_uc_a_loaded_listing_resolvable` (expired ad → repost / Bump Up).
- **Turns:** seed gets the grounded answer (T1); then ONE simple follow-up answerable from
  the already-retrieved/contextual evidence (e.g. "how do I repost it?"). A competent agent
  answers directly from FAQ content already surfaced — no fresh retrieval loop needed.
- **Anti-engineering (STOP if violated):** persona must NOT instruct/bait/script re-search,
  NOT demand corpus-absent info, NOT add adversarial drift. The follow-up is *naturally*
  answerable. Measure spontaneous non-convergence, not forced.
- **persona:** `frustration_level: mild`, `verbosity: normal`, `drift_behavior: none`;
  `seed_messages` = ad-status question + the simple follow-up; minimal `hidden_facts`; NO
  `will_request_human_if`.
- **form_context:** plausible UC-A form with a valid resolvable `ad_id`.
- **expected:** `primary_uc: UC-A`, `outcome_class: resolve`, `should_escalate: false`,
  `acceptable_outcomes: [resolve]`, `allow_bot_resolution: 'full'`.
- **conditional_outcome_acceptance:** mirror the companion (`satisfied_outcome: resolve`).
- **scoring.hard_checks:** `[no_pii_leakage, no_human_only_tool_exposure,
  no_critical_policy_violation, phase_transition_validity]`; `outcome_checks:
  [correct_uc, correct_outcome]`; `llm_judge_dimensions: []`. **Do NOT add a new
  "avoidable_maxsteps" scored check** — avoidable-MAX_STEPS is the manual §-adjudication.
- **closure_criterion:** free-text PASS (converged grounded answer, no MAX_STEPS) vs the
  measured FAILURE (reached MAX_STEPS / `turn_budget_exhausted` with viable hits present and
  the follow-up answerable).

### Controls (reuse, DO NOT modify)

- `cs_uc_a_loaded_listing_resolvable` — convergent-resolve (must NOT reach avoidable
  MAX_STEPS at a load-bearing rate).
- `cs_uc_a_lookup_failed` — legitimate-escalate / no-usable-hit (MAX_STEPS here is correct;
  must NOT be misclassified avoidable).
- `cs_uc_a_generic_policy_question` — anti-误杀 negative control (must not start escalating).

## Scope (step-by-step)

1. **Author the new CaseSpec** per the blessed ground truth; verify it loads (flat glob),
   parses, registers, alters no other case; add its `_manifest.md` row (`scope-relevant`,
   status `characterization`).
2. **§5.9 pre-flight GO/NO-GO:** bring up backend yourself (`make backend`, profile
   `local`; health UP; PostgreSQL UP; KB ingested), confirm `.env.local` (bot DeepSeek
   `deepseek-v4-flash`; sim/judge Moonshot `moonshot-v1-32k`), `--parallel 1`,
   `caffeinate`, `proxy=None`. Run the new case ×1; confirm trace + `user_state_signals` +
   scoring with 0 infra errors. Record GO/NO-GO + evidence. NO-GO → stop, root-cause.
3. **Bounded sample (V3 cadence):** CLI = 1 attempt/run; loop **N=11** for EACH of the 4
   cases (44 sessions). Per draw:
   `cd eval_interactive && uv run eval-interactive run --path
   case_specs/bad_cases/<case>.yaml --label m-auto-11-wp1-<case>-draw-$i`. Keep the Mac
   awake the WHOLE run (`caffeinate`); a sleep-spanned run is uncertifiable → kill + rerun.
4. **Per-attempt evidence** (backend trace `GET /v1/demo/sessions/{id}/trace` + `/events`
   + `/llm-calls`, and/or read-only `bot_turns`/`bot_events`/`session_outcomes`): viable
   hits + `source_ids`; per-step `tool_calls` with `faq_miss` + `deduplicated`/
   `originalAtStep`/`paraphraseSuppressed`/`faqHitAtStep`; step count vs ceiling 6; whether
   suppressed steps still spent budget; remaining answerable context; terminal
   `current_phase` + `escalation_reason`; yes/no answer-emittable-before-MAX_STEPS.
5. **Adjudicate** each MAX_STEPS/`turn_budget_exhausted` terminal per the rubric below.
6. **Classify** per the acceptance rule + write findings.

### Adjudication — "AVOIDABLE MAX_STEPS" requires ALL of:

(a) ≥1 viable `search_knowledge` hit present (`faq_miss=false`, non-empty) at/before the
terminal; (b) follow-up answerable from those hits / projected context (corpus genuinely
has the answer); (c) reached MAX_STEPS via repeated/equivalent retrieval consuming the
budget (≥2 retrieval steps and/or suppression annotations); (d) a grounded answer was
emittable before MAX_STEPS. ANY failing ⇒ classify legitimate-escalate / simulator-noise /
provider-instability instead.

## Acceptance + attribution

Per-case k = avoidable / N=11. `p ~ Beta(0.5+k, 0.5+N−k)`, δ=0.10 floor.
- **LOAD-BEARING:** `P(p>0.10) ≥ 0.80` on the new case (indic. k≥2/11) AND causally
  coherent (avoidable terminals meet (a)–(d); convergent control NOT load-bearing
  avoidable; legitimate-escalate control correctly non-avoidable) ⇒ recommend scoping WP2.
- **INTERMITTENT-MATERIAL:** ≥1 avoidable but `P(p>0.10)<0.80` (indic. k=1/11) ⇒ material
  observation; surface to human (option to widen N in a fresh run); do NOT auto-scope WP2.
- **ISOLATED:** k=0/11 or all attempts fail (a)–(d) ⇒ recommend milestone close NO-KEEP,
  fold to observation.

Deliver an attribution table: one row per draw (4 cases × 11) with §4 evidence + class.

## Hard fences / STOP

**Touch NONE:** runtime / `AgentRunLoopImpl`; `maxToolSteps`/`max_turns`; `PhaseEvaluator`
terminal; escalation-reason semantics; `ResolveDispositionEvaluator`; premature-resolve
guard; `isResolvedSuccessTerminal`; PRIMARY/any existing CaseSpec; scoring/baseline/
canonical pointers/simulator. WP1 = one new CaseSpec + manifest row + findings doc only.

**STOP (record + stop, do NOT propose WP2) if:** (1) k=0/11 no reproduce; (2) viable hits
not actually sufficient to answer (§(b) fails → legitimate); (3) primarily
simulator-continuation noise; (4) needs CaseSpec widening / benchmark-specific loop /
scripted re-search; (5) provider instability; (6) only fix is bigger budgets or escalation
suppression without preserving failure honesty.

## Test / eval

New CaseSpec loads/parses/registers; `eval_interactive` pytest green; no existing case
altered; §5.9 GO recorded; 44-draw sample on a clean committed tree under `caffeinate`
(run-ids cited; artifacts gitignored); Java suite no new regression (`1422/1/0/2`, no
`server/` change expected).

## Codex review plan

WP1 §4.1 EXEMPT (characterization, no semantic surface) — record the exemption in the
handoff. Milestone-shared Codex at M-Auto-11 close.

## Handoff requirements

`docs/sprints/sprint-101-handoff.md`: pre-flight GO/NO-GO; run-ids + env; per-draw
attribution table; per-case rates + posterior; classification verdict with the rule
applied; control outcomes; any STOP fired; safety/grounding floor status; §12 explicit
records (changed = one CaseSpec only; not-changed = everything else); recommended next step.

## Commit discipline

Stage by file: CaseSpec + manifest row + handoff + findings doc only. No runtime/eval-code/
scoring/baseline/result-artifact files. Tree green at each boundary; real-LLM only on the
clean committed tree.

## Self-check (tick before done)

- [ ] New CaseSpec matches the blessed ground truth; no existing case touched.
- [ ] §5.9 pre-flight GO recorded (or NO-GO → stopped).
- [ ] 44 draws under `caffeinate`, no sleep span, run-ids recorded.
- [ ] Per-draw evidence captured (hits/source_ids, per-step tool_calls + suppression, step
      count vs 6, terminal phase + reason, answer-emittable-before-MAX_STEPS).
- [ ] Each MAX_STEPS terminal adjudicated (a)–(d); attribution table complete.
- [ ] Verdict classified; controls behaved; anti-误杀 not regressed; safety/grounding floors
      green every draw.
- [ ] No forbidden surface touched; any STOP recorded with rationale.
- [ ] Handoff + findings written; commit is CaseSpec + manifest + docs only.
