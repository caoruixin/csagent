---
title: Milestone M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-04
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Promoted 2026-06-04 from the deliver-agent draft. M-Auto-4 is PAUSED (not
  superseded) — its objective is preserved at
  `docs/milestones/M-Auto-4_objective_PAUSED.md`. **Update 2026-06-04 (S-Auto-21
  open):** M-Auto-4's S-Auto-18 is DEFERRED to M-Auto-7 or later, behind M-Auto-6
  (Clusters B + C of the simulator/eval-framework audit
  `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`). The audit
  surfaced a third blocker for "honest verdicts" — the customer simulator at
  `user_simulator.py:187` inverts roles when sending the transcript to the
  chat-completions API, conditioning the simulator LLM to behave as the support
  agent from turn ~3+ (1,017 / 7,071 sessions contaminated, 14.4 %). S-Auto-19
  corrected the EVAL read column; S-Auto-20 corrected the RUNTIME stamp column;
  **S-Auto-21 corrects the INPUT column**. M-Auto-5 closes after S-Auto-21 + the
  authoritative simulator-fixed re-bless. Then M-Auto-6 = Clusters B + C
  (parallel research-agent dispatch). Then M-Auto-7 may pick up M-Auto-4's
  S-Auto-18 or a semantic-remediation milestone on the honest baseline.

  M-Auto-5 north star = make every per-case eval VERDICT reflect the bot's actual
  behaviour, by (a) correcting eval-harness checks that read session-accumulated
  bot state through a turn-local / wrong-typed lens, and (b) completing the runtime
  TRACE CONTRACT at the source. It is NOT a bot-capability milestone: no UC
  hypothesis, escalation posture, routing, prompt, or skill soft field is edited.

  Per human direction (2026-06-04): the milestone is run as a SINGLE sub-sprint
  (eval side + runtime side + re-bless together) to speed close; and the
  `no_pii_leakage` artifact (#5) is a LIGHT relaxation, not a focus — PII detection
  is meant to be relaxed anyway.

  WHY THIS MILESTONE EXISTS (the M-Auto-4 reframe): M-Auto-4 diagnosed the
  0-keep / 67-iteration pinning as residual provider non-determinism (NOISE) and
  applied k-of-n majority. That diagnosis is correct but necessary-not-sufficient.
  The `m-auto-4-baseline-20260604` re-bless exposes a SECOND, deterministic cause:
  five measurement artifacts that pin cases to FAIL regardless of bot behaviour.
  k-of-n majority does NOTHING for a deterministic false-fail — all n draws fail
  identically. Concretely (this baseline):
    1. `trace_minimum` (L1) hard-fails on blank `containment_outcome`. The runtime
       only stamps containment on a CLOSE phase (ControlKernel.java:555-558 /
       PhaseEvaluator.java:1403), reached only via a dedicated record_outcome-only
       turn (PhaseEvaluator.java:1393). The eval simulator ends on `goal_achieved`
       the moment the user is satisfied (session_runner.py:223), so a one-shot
       resolve never reaches CLOSE → containment blank → score zeroed BEFORE L2/
       judge run. 55/55 goal_achieved + 10/10 goal_impossible draws blank.
    2. `source_citation_present` (L1) hard-fails because per-turn `source_ids` come
       only from the retrieval turn (ControlKernel.java:1661-1681), but the prompt
       tells the bot to answer from accumulated hits on a LATER turn → answer turn
       empty. Session-level resolved/cited source ids exist (BotSession.java:342-348)
       but the check ignores them. 16/35 fails are confirmed artifacts (all 3
       visible-suite fails).
    3. `search-knowledge-before-faq-answer` (TIER2 mandatory) hard-fails because
       `accumulated_tool_results` reads the final-turn ATR snapshot, overwritten
       each turn; the union fallback only fires when the final-turn ATR is empty
       (skill_procedure_check.py:487-506). 17/17 fails gate; search_knowledge was
       called in every one.
    4. `*-intake-complete-before-handover` (TIER2 mandatory) can NEVER pass: the
       eval `intake_fields_collected` accepts only a list (skill_procedure_check.py:534)
       but the backend emits a dict/object (ContextProjectionBuilder.java:387-393),
       so `contains()` is always false. 0 PASS across 76 applicable instances;
       proof: cs066 with intake_complete=true still FAILs.
    5. `no_pii_leakage` (Tier-0 safety) hard-fails on the first-party system address
       `noreply@gumtree.com` (13/13 fails) surfaced helpfully. LIGHT relaxation per
       human direction — not a milestone focus.
  Coupling: #1/#2 also corrupt the L2 outcome layer — `_check_correct_outcome`
  reads `containment_outcome` (outcome_checks.py:226); 34/73 correct_outcome fails
  are blank-containment artifacts. Fixing #1/#2 makes the whole L2 outcome layer
  trustworthy for ~65 currently-masked cases.

  CONSEQUENCE FOR M-Auto-4: M-Auto-4 is PAUSED after S-Auto-17 (k-of-n machinery is
  LIVE; baseline re-blessed). Its held validation overnight (S-Auto-17 §3.4, drafted
  not launched — STOP-or-GO was GO) is RE-GATED: launching it now would measure a
  contaminated gate and re-pin at L0/L1, burning a real-LLM overnight. It resumes
  AFTER the M-Auto-5 re-bless, on honest verdicts. The remaining M-Auto-4 sub-sprint
  S-Auto-18 (bounded §5 escalation-family tiers) ALSO resumes after M-Auto-5,
  because it would otherwise curate tiers against contaminated verdicts — and the
  escalation-reason cross-family signal it targets is itself half-measured today.
---

# Milestone M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty

## Milestone class

- **Primary layer**: `infra` — eval-harness measurement correctness
  (`eval_interactive/eval_interactive/scoring/` checks) **plus** runtime
  trace-contract completion (`server/.../runtime/` containment + source_ids
  emission). Both are §1.4 Runtime-owns territory (persistence, trace and eval
  contract) and eval-harness measurement — NOT §1.3 LLM-owned semantics.
- **§7 stanza**: technically **§7-EXEMPT** (characterization / measurement-infra:
  no prompt, routing, UC-hypothesis, escalation-posture, or CaseSpec-rubric edit).
  Stanza **included below** for rigor because it edits gate-contributing eval checks
  and re-blesses the baseline.
- **Codex review plan (§4.3)**: **PER-SUB-SPRINT RECOMMENDED** (touches
  gate-contributing eval checks + the server runtime trace contract + re-blesses);
  folds into the M-Auto-5 milestone-shared close. The autoloop 5-file
  `scoring_code_baseline_sha` set (`tier_evaluator`/`eval_runner`/`baseline_loader`/
  `gaming`/`aggregate`) is **NOT** in scope — M-Auto-5 edits `eval_interactive`
  harness + `server` runtime, a different code surface — so the fence-#13 SHA trigger
  does **not** fire. The baseline re-bless (pointer move) triggers
  `suspect_baseline_manipulation` (expected, explained). Anti-误杀 is the central
  Codex focus (below).

## Goal

Make every per-case eval **verdict** (`case_passed` and its L1 / L2 / TIER2
sub-results) reflect the bot's **actual behaviour**, by correcting five measurement
artifacts that currently convert correct bot behaviour into hard gate failures. Two
mechanisms, both `infra`:

1. **Eval-harness correctness** — checks must read **session-accumulated** state
   (union across turns; correct types) instead of a **turn-local / wrong-typed**
   snapshot, and stop flagging benign first-party addresses.
2. **Runtime trace-contract completion** — the backend records a terminal
   disposition (`containment_outcome`) on the success path and attaches the
   session's resolved source ids to the answer turn, so the trace contract is
   complete at the source (helps the admin trace + downstream consumers, not only
   the eval).

**Non-negotiable anti-误杀 invariant** (the heart of this milestone, for the
structural checks): every fix must *still fail genuine failures*. Fixing
`trace_minimum` must still fail a genuinely blank-from-crash terminal (`error` /
`contract_violation`). Fixing `intake-complete` must still fail a genuine
handover-before-intake-complete. A fix that simply stops a check from ever firing is
a regression, not a correction. (#5 `no_pii_leakage` is the one explicitly-relaxed
exception — see below.)

**This milestone explicitly does NOT change the bot's customer-service ability.**
The genuine semantic problems the corrected measurement reveals — UC routing
misclassification (`correct_uc`), escalation posture (under/over-escalation, the
high-risk under-escalations, the escalation-reason cross-family question) — are
**deferred to a later semantic milestone**, to be designed against the honest
re-blessed baseline.

## Sub-sprint sequence (S-Auto-19 → S-Auto-20 → S-Auto-21; M-Auto-4's S-Auto-18 DEFERRED behind M-Auto-6)

> Originally scoped as ONE sub-sprint (eval + runtime + re-bless together). Re-bless
> verdict-review exposed a second blocker (runtime stamp inert; `loop_detected`
> vacuous-pass) → corrective S-Auto-20. The 2026-06-04 simulator/eval-framework
> audit exposed a third blocker (input-column contamination via simulator role
> inversion) → corrective S-Auto-21. The authoritative simulator-fixed re-bless
> happens at the end of S-Auto-21; pointer move is human-gated.

1. **S-Auto-19 / Sprint 074 — verdict corrections (eval + runtime) + re-bless.
   [ACCEPTED-WITH-FOLLOWUP 2026-06-04]** Layer `infra`. Archived:
   `docs/sprints/sprint-074-{objective,handoff}.md`; dev prompt
   `compact/sprint-074-dev-prompt.md`. The 5 eval-side read-corrections are SOUND
   (eval pytest 522/0). But the `m-auto-5-baseline-20260604` re-bless exposed: runtime
   #1/#2 were INERT on the real corpus (`isResolvedSuccessTerminal`'s READY_TO_CONFIRM
   gate never holds on the sim-preempted goal_achieved path → 0 `resolved` stamps →
   containment still blank → 4 cases pass GATE-VACUOUSLY: composite=0/l2=0/judge=0), and
   `loop_detected` in eval#1's valid set lets a looping bot vacuous-pass. → corrected by
   S-Auto-20. `baseline_dir` NOT moved.
   **Eval side** (`eval_interactive/.../scoring/`):
   - #3 `accumulated_tool_results` → always union across turns
     (skill_procedure_check.py:487-506), clearing `search-knowledge-before-faq-answer`
     false-fails.
   - #4 `intake_fields_collected` → accept the dict/object the backend emits (use
     its keys), clearing the structural `*-intake-complete-before-handover` 0-PASS
     defect (skill_procedure_check.py:520-537).
   - #5 `no_pii_leakage` → **light relaxation** (per human): stop flagging benign
     first-party / system addresses (e.g. `noreply@gumtree.com`)
     (hard_checks.py:489-501). Not a focus; no heavy Tier-0 ceremony.
   - #1 eval side — `trace_minimum` **terminal-disposition-aware**: when
     `containment_outcome` is blank, derive disposition from the simulator
     `stop_reason` (`goal_achieved` → resolved-equivalent; `goal_impossible`/
     `loop_detected`/`max_turns_exceeded` → valid measured terminal) instead of
     hard-failing; keep hard-failing genuine partial-instrumentation
     (`error`/`contract_violation` blank) + Mode-2 (hard_checks.py:662-704).
   - #2 eval side — `source_citation_present` consults **session-accumulated**
     source ids (union of per-turn `source_ids` across the session) so a grounded
     answer composed from a prior retrieval turn is not a false fail
     (hard_checks.py:819-842).
   **Runtime side** (`server/.../runtime/`; §1.4 trace contract, not semantic):
   - #1 runtime — stamp `containment_outcome="resolved"` when the loop terminates
     having delivered a substantive answer with no escalation/error (so the contract
     is complete at the source without a dedicated record_outcome-only CLOSE turn).
     Never stamp "resolved" on a genuinely unresolved terminal
     (`goal_impossible`/`error`/`loop_detected`/escalation).
   - #2 runtime — attach the session's resolved source ids to the answer turn
     (`BotTurn.sourceIds`) so the answer turn carries its grounding.
   **Then**: backend rebuild + restart, **authoritative re-bless** on the combined
   state → new `baseline_dir` `m-auto-5-baseline-YYYYMMDD` (old
   `m-auto-4-baseline-20260604` retained; pointer-only move, reversible). Real-LLM,
   clean tree (dirty-index hazard). The **held S-Auto-17 validation overnight becomes
   launchable** on the honest baseline after this.

2. **S-Auto-20 / Sprint 075 — corrective: real goal_achieved containment stamp +
   loop_detected no-vacuous-pass. [ACCEPTED 2026-06-04]** Layer `infra`. Archived:
   `docs/sprints/sprint-075-{objective,handoff}.md`; dev prompt
   `compact/sprint-075-dev-prompt.md`. Step-0 vacuous 7-draw scan found 11 cases
   with ≥1 gate-vacuous draw (not 4 as initially scoped); Fix #1 broadened
   `ControlKernel.isResolvedSuccessTerminal` (also accepts ANSWERED_SUBTASK +
   grounding) — validated against a REAL goal_achieved trace per §5.7 (session
   `6965f6dc-…` persisted `containment_outcome="resolved"`); Fix #2 removed
   `loop_detected` from `HardChecker._VALID_TERMINAL_STOP_REASONS`. Java
   `1232 / 1 / 0 / 2`, eval pytest 524 (+2 net new), commits `95cd0f4` + `e6e57ec`
   + `da7ef15`. `baseline_dir` NOT moved.

3. **S-Auto-21 / Sprint 076 — corrective #2: simulator role-inversion fix +
   bad-case suite re-render + M-Auto-5 authoritative re-bless. [ACTIVE]** Layer
   `infra`. Contract: `docs/sprint_objective.md`; dev prompt
   `compact/sprint-076-dev-prompt.md`. Input artifact:
   `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`. Closes the
   third (and final) blocker for honest verdicts — the simulator at
   `user_simulator.py:187` inverts roles when sending the transcript to the
   chat-completions API, conditioning the simulator LLM to behave as the support
   agent from turn ~3+ (audit lower-bound: 14.4 % of sessions contaminated).
   Bundled fix: (#1) root-cause role-map inversion; (#2) per-turn persona
   re-anchor; (#3) negative-form rules in the simulator system prompt; (#4)
   post-generation customer-voice drift guard with retry-and-escape (3-attempt
   budget; `simulator_drift_blocked` end-of-session — anti-误杀); (#5) multi-turn
   contract tests T1-T7 (audit §7 explicitly called out the existing test file
   covers JSON parsing only); (#6) bad-case suite re-render on the simulator-fixed
   code with real-LLM evidence per §5.7; (#7) authoritative re-bless left
   **re-bless-ready** — HUMAN launches; deliver moves the `baseline_dir` pointer
   and flips `docs/current_eval_baseline.md` status. The still-running
   `m-auto-5-baseline-20260605` is NOT killed — it is retained as forensic data.
   NO bot / runtime / prompt / routing / UC / escalation / skill / CaseSpec edit.
   Audit Clusters B and C are routed to M-Auto-6 (not in this sub-sprint).

4. **(M-Auto-6, opened after M-Auto-5 close) — audit Clusters B + C.** Two-track
   research-agent dispatch (B: `per_turn_trace` truncation + primary_uc/
   active_use_case authority; C: cs59s session_create 400, 46f5b2e9 500 +
   double-send, intake clarifier branch). NOT this milestone.

5. **(deferred to M-Auto-7 or later) S-Auto-18 — bounded §5 escalation-family
   tiers (`eval_spec`).** Pushed behind M-Auto-6.

## Non-goals

- **No bot-capability / semantic change.** No UC hypothesis, escalation posture,
  drift detection, follow-up policy, routing, prompt, or skill YAML soft field is
  edited. The runtime changes (#1/#2 runtime side) are **trace-contract /
  persistence** completions (§1.4 infra), not semantic decisions.
- **No CaseSpec-rubric widening to accept a bot mistake (§1.7 / §5.4).** Every
  structural fix corrects HOW a check reads the trace, never WHAT counts as success.
  (#5 PII is an explicitly-authorized relaxation, scoped to benign first-party
  addresses.)
- **No semantic remediation.** UC routing misclassification + escalation posture
  (the genuine failures the corrected measurement reveals) are a SEPARATE later
  milestone, designed against the M-Auto-5 honest baseline.
- **No autoloop fitness-gate (`autoloop/scoring/`) change.** The 5-file
  `scoring_code_baseline_sha` set is out of scope; M-Auto-4's k-of-n machinery is
  final.

## Milestone acceptance bar

Closes when, on the re-blessed `m-auto-5-baseline-YYYYMMDD`:

1. **Structural fixes carry paired evidence (primary gate):** for #1/#2/#3/#4,
   (a) a captured-real-trace characterization test shows the prior false-fail now
   scores correctly, AND (b) an anti-误杀 counter-test shows a genuine failure of the
   same shape still fails. #5 (PII) ships a light relaxation + a single sanity test
   (a real user/third-party email still flags) — no heavy ceremony.
2. **Verdict-distribution shift is explained:** the re-bless report shows, per suite,
   how per-case verdicts changed vs `m-auto-4-baseline-20260604`, attributing each
   change to a specific artifact fix or to a genuine failure now correctly reaching
   L2/judge (no longer L1-masked). No unexplained verdict flip.
3. **Grounding floor intact or stronger:** grounding diagnostics at/above prior
   level; L4 shadow gate intact + firewalled.
4. **No regression:** Java `1213/1/0/2` (the runtime change must not regress the
   Java suite); autoloop pytest `324`; eval_interactive pytest `503/0` under
   `uv run` (plus the new characterization + counter-tests).
5. **Re-bless recorded + reversible:** new dated dir; old retained;
   `suspect_baseline_manipulation` is an expected, explained observation.
6. **Codex §4.1 anti-hardcode kernel pass** — central question: did any structural
   fix widen what counts as success (forbidden) vs correct how the trace is read
   (in scope)?

HARD close gates (§5.5): the paired-evidence review (primary) + Codex §4.1 kernel
pass + Java no-regression + grounding floor. Smoke composite / pass-rate =
observation only (but the re-bless verdict-distribution shift IS the milestone's
central evidence and must be explained, not just observed).

## Hard fences (milestone level)

- **No semantic / capability edit** anywhere (prompt, routing, UC hypothesis,
  escalation posture, skill soft field, CaseSpec rubric).
- **Every structural fix (#1/#2/#3/#4) is paired with an anti-误杀 counter-test.**
  A structural fix that only stops a check firing, with no counter-test that a
  genuine same-shape failure still fails, is rejected.
- **#5 PII**: explicitly-authorized light relaxation scoped to benign first-party /
  system addresses; keep one sanity test that a real user/third-party email still
  flags. Not a focus, not a heavy gate.
- **#1 runtime**: never stamp `containment_outcome="resolved"` on a genuinely
  unresolved terminal (`goal_impossible`/`error`/`loop_detected`/escalation).
- **Baseline reversible**: re-bless writes a fresh dated dir; never overwrite
  `m-auto-4-baseline-20260604`; only the pointer moves.
- **Do NOT launch the held S-Auto-17 overnight until the M-Auto-5 re-bless is done.**
- **Acceptance evidence is real-LLM (§5.7)** for the re-bless + the runtime-emission
  confirmation; captured-real-trace fixtures are valid evidence for the eval-side
  read-correctness (the trace is real; only the scoring code under test changed).

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — eval-harness measurement correctness
(`eval_interactive/.../scoring/`) + runtime trace-contract completion
(`server/.../runtime/`). No `eval_spec` rubric change, no `semantic_planner` /
`prompt_projection` / routing change.

**Tier-0 invariant:** adds none; preserves all Tier-0 families at current strictness,
EXCEPT the explicitly-authorized `no_pii_leakage` relaxation (benign first-party
addresses only; real user/third-party PII still flags — sanity-tested).

**Semantic hardcode:** none. No keyword/regex/enum routing or UC-specific rule is
added. The first-party address exclusion is the company's own published system
addresses (declarative), not a content-matching semantic rule. All other fixes are
structural (cross-turn union; dict-key read; stop_reason-aware terminal disposition;
session-accumulated source-id read; runtime containment/source_ids emission).

**Generalization coverage:** characterization / measurement-infra milestone — the
evidence is the paired characterization + anti-误杀 counter-tests per structural
artifact and the explained re-bless verdict-distribution shift, not target/neighbor/
negative/shadow case-family counts. The shadow gate (L4) stays active + firewalled;
the corrected checks apply identically to all suites.

## Relationship to M-Auto-4 (PAUSED)

M-Auto-4 (Autoloop Fitness Measurement Reliability) is **paused, not closed**, after
S-Auto-17. Its objective is preserved at
`docs/milestones/M-Auto-4_objective_PAUSED.md`. Its k-of-n machinery is LIVE and the
baseline is re-blessed; what remains (S-Auto-18 escalation tiers + the held
validation overnight) is gated on M-Auto-5's honest verdicts. M-Auto-5 does not undo
any M-Auto-4 work; it corrects the per-case verdicts that M-Auto-4's fitness gate
votes on. After M-Auto-5 closes + re-bless + the validation overnight, M-Auto-4
resumes at S-Auto-18 and then closes.

## Promotion record (2026-06-04)

Promoted from the deliver-agent draft on human approval. Executed:
M-Auto-4 objective preserved → `docs/milestones/M-Auto-4_objective_PAUSED.md`
(status: deferred); S-Auto-17 objective archived →
`docs/sprints/sprint-073-objective.md` (sprint-archive); this body promoted to
`docs/milestone_objective.md`; S-Auto-19 promoted to `docs/sprint_objective.md`;
dev prompt authored at `compact/sprint-074-dev-prompt.md`; `docs/10-handoff.md` §0
updated.
