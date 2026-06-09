---
title: Sprint 074 / S-Auto-19 — verdict corrections (eval + runtime) + re-bless (M-Auto-5, single sub-sprint)
doc_tier: sprint-archive
status: archived
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-06-04
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-073-objective.md]
superseded_by: docs/sprint_objective.md
notes: >
  ARCHIVED 2026-06-04. Dev portion accepted (commits 47b3060..0591649): the 5
  eval-side measurement-read corrections are SOUND (eval pytest 522/0; anti-误杀
  counter-tests real). BUT the m-auto-5-baseline-20260604 re-bless exposed two gaps
  the corrective S-Auto-20 fixes: (1) runtime #1/#2 were INERT on the real corpus —
  `isResolvedSuccessTerminal`'s READY_TO_CONFIRM gate never holds on the
  simulator-preempted goal_achieved path, so containment stayed blank and 4 cases
  (cs095/uc_a/uc_b goal_achieved; cs012 loop_detected) pass GATE-VACUOUSLY
  (case_passed=true but composite=0, l2_count=0, judge=0 — no outcome judged);
  (2) loop_detected in eval#1's valid-terminal set lets a looping bot vacuous-pass.
  baseline_dir NOT moved (still m-auto-4-baseline-20260604). See sprint-074-handoff.md
  § and the S-Auto-20 contract (docs/sprint_objective.md).

  The single sub-sprint of M-Auto-5 (docs/milestone_objective.md). Corrects all five
  measurement artifacts diagnosed on m-auto-4-baseline-20260604 on BOTH the eval side
  (eval_interactive) and the runtime side (server trace contract), then re-blesses.
  Central discipline = anti-误杀: every structural fix corrects HOW a check reads
  the trace, never WHAT counts as success, and ships with a counter-test proving a
  genuine same-shape failure still fails. #5 (no_pii_leakage) is an explicitly-
  authorized LIGHT relaxation per human (2026-06-04), not a focus. Dev session
  source-of-truth: compact/sprint-074-dev-prompt.md (self-contained per
  prompt-artifact-rules §9).
---

# Sprint 074 / S-Auto-19 — verdict corrections (eval + runtime) + re-bless

## Class

- **Layer (primary)**: `infra` — eval-harness measurement correctness
  (`eval_interactive/eval_interactive/scoring/`) + runtime trace-contract completion
  (`server/.../runtime/`; §1.4 persistence + trace contract). No prompt / routing /
  UC-hypothesis / escalation-posture / CaseSpec rubric edit.
- **§7 stanza**: technically **§7-EXEMPT** (measurement-infra / characterization).
  **Included below** for rigor — edits gate-contributing checks + the runtime trace
  contract + re-blesses.
- **Codex review plan (§4.3)**: **PER-SUB-SPRINT RECOMMENDED** (gate-contributing
  eval checks + server runtime + re-bless). NOT a fence-#13 SHA trigger (the autoloop
  5-file `scoring_code_baseline_sha` set is untouched). Folds into the M-Auto-5
  milestone-shared close. Codex focus: anti-误杀 (each structural fix paired with a
  counter-test) + the runtime stamping not firing on unresolved terminals + the PII
  relaxation scoped to first-party addresses.
- **Position in milestone**: the single sub-sprint of M-Auto-5; then M-Auto-4's
  S-Auto-18 (escalation tiers) resumes on the honest baseline.

## Goal

Correct all five measurement artifacts so each per-case verdict reflects the bot's
actual behaviour, on both the eval side and the runtime trace-contract source, then
re-bless. Preserve every structural check's ability to catch genuine failures
(anti-误杀). Output: corrected eval checks + runtime trace contract + paired tests +
a re-blessed `m-auto-5-baseline-YYYYMMDD` whose verdict-distribution shift vs
`m-auto-4-baseline-20260604` is explained per artifact.

**This sub-sprint does NOT change the bot's customer-service ability** (no semantic /
routing / prompt / CaseSpec edit). The runtime edits are trace-contract completions.

## Execution order

eval fixes → runtime fixes → backend rebuild + restart → re-score captured traces
(eval-only verdict-delta, deterministic) → real-LLM re-bless on a clean tree →
surface the held S-Auto-17 overnight as launchable (do NOT launch it here).

## Scope — eval side (`eval_interactive/.../scoring/`)

### #3 — `accumulated_tool_results` always unions across turns
- **Bug**: `TraceView.accumulated_tool_results` (skill_procedure_check.py:487-506)
  returns the final-turn ATR when non-empty, unioning only when the final turn omits
  the key. A partial final-turn ATR (e.g. `{resolve_article}` after `search_knowledge`
  was evicted) bypasses the union → mandatory `search-knowledge-before-faq-answer`
  false-fails (17/17 gated; search was called in every one).
- **Target**: union ATR across ALL turns unconditionally (presence-preserving).
- **Anti-误杀 counter-test**: a session that NEVER calls `search_knowledge` still FAILs.
- **Characterization test**: early-turn search + different-tool final turn → PASSes.

### #4 — `intake_fields_collected` accepts the dict the backend emits
- **Bug**: `TraceView.intake_fields_collected` (skill_procedure_check.py:520-537)
  returns a tuple only for a list/tuple; the backend emits a dict/object
  (ContextProjectionBuilder.java:387-393) → `()` → `contains()` always false →
  `*-intake-complete-before-handover` can NEVER pass (0 PASS / 76 instances; proof:
  cs066 `intake_complete=true` still FAILs).
- **Target**: when `fields_collected` is a Mapping, use its keys.
- **Anti-误杀 counter-test**: `anchor_outcome_uc_g_gdpr` (only `registered_email`,
  missing `data_request_type`) still FAILs.
- **Characterization test**: cs066 (both fields, `intake_complete=true`) → PASSes.

### #5 — `no_pii_leakage` light relaxation (NOT a focus)
- **Bug**: the email regex (hard_checks.py:489-501) flags the first-party system
  address `noreply@gumtree.com` (13/13 fails).
- **Target**: stop flagging benign first-party / system addresses. Per human
  direction PII detection is to be relaxed anyway — keep this simple (a small
  first-party exclusion); do NOT build heavy Tier-0 ceremony around it.
- **Sanity test (single)**: a real user/third-party email (e.g. `someone@gmail.com`)
  still flags. No further counter-test burden.

### #1 (eval side) — `trace_minimum` terminal-disposition-aware
- **Bug**: `_check_trace_minimum` Mode-1 (hard_checks.py:680-686) hard-fails on blank
  `containment_outcome` regardless of WHY. A one-shot `goal_achieved` resolve
  (simulator-ended, session_runner.py:223) never reaches CLOSE → blank → score zeroed
  before L2/judge. 55 goal_achieved + 10 goal_impossible draws.
- **Target**: when blank, consult the simulator `stop_reason`: `goal_achieved` →
  valid resolved terminal (no Mode-1 fail); `goal_impossible`/`loop_detected`/
  `max_turns_exceeded` → valid measured terminal (Mode-1 does not fire; the case may
  still fail other checks for the right reason); `error`/`contract_violation`/
  `session_create_failed` → genuine partial instrumentation → still FAIL. Mode-2
  (non-empty user turn, blank bot reply, no handover) UNCHANGED.
- **Data dependency**: thread the simulator `stop_reason` (or equivalent terminal
  classification) into `_check_trace_minimum` (eval-harness plumbing).
- **Anti-误杀 counter-test**: blank + `stop_reason=error`/`contract_violation` still
  FAILs; Mode-2 still FAILs.
- **Characterization test**: cs095-style `goal_achieved` + blank containment +
  delivered grounded answer → `trace_minimum` PASSes (and L2/judge then run).

### #2 (eval side) — `source_citation_present` session-accumulated grounding
- **Bug**: `_check_source_citation_present` (hard_checks.py:819-842) requires
  per-turn `source_ids` on each substantive answer turn; per-turn `source_ids` come
  only from the retrieval turn (ControlKernel.java:1661-1681), but the prompt tells
  the bot to answer from accumulated hits on a LATER turn → answer turn empty (16/35
  fails are artifacts; all 3 visible-suite fails).
- **Target**: a substantive answer turn is grounded if its own `source_ids` is
  non-empty OR any earlier turn in the session had `source_ids` (session-accumulated
  union of per-turn source_ids — works with existing trace data; the runtime-side
  cleaner attach is below).
- **Anti-误杀 counter-test**: a session that retrieves on NO turn but emits a
  substantive factual answer still FAILs (genuine ungrounded — shadow never-searched).
- **Characterization test**: alice-style (search+resolve early, grounded answer
  later) → PASSes.

## Scope — runtime side (`server/.../runtime/`; §1.4 trace contract, not semantic)

### #1 runtime — stamp containment on the success terminal
- **Target**: when the agent loop terminates having delivered a substantive answer
  with no escalation/error, stamp `containment_outcome="resolved"` (a complete
  terminal disposition) even without a dedicated record_outcome-only CLOSE turn.
  Today only CLOSE / ESCALATE stamp it (ControlKernel.java:555-558,
  PhaseEvaluator.java:1403/1416).
- **Hard constraint**: NEVER stamp "resolved" on a genuinely unresolved terminal
  (`goal_impossible` from the bot's view / `error` / `loop_detected` / escalation).
  This is the runtime anti-误杀: a complete disposition, not a blanket "resolved".
- **Tests**: Java unit/integration — a one-shot grounded-answer terminal now records
  `containment_outcome=resolved`; an escalation still records `escalated`; an error
  terminal records neither (left blank → eval Mode-1 still catches it).

### #2 runtime — attach resolved source ids to the answer turn
- **Target**: persist the session's resolved source ids on the answer-turn
  `BotTurn.sourceIds` (ControlKernel.java:1661-1681 builds it only from the current
  turn's `knowledgeHits()`), so the answer turn carries its grounding for the trace
  UI + downstream consumers as well as the eval.
- **Tests**: Java — an answer turn composed from a prior retrieval turn now carries
  the resolved source ids; a turn with no session grounding carries none.

## Re-bless (after eval + runtime land + backend restart)

- On a **clean committed tree**, real-LLM, backend up (creds in
  `autoloop/.env.local`), run each suite via the existing re-bless tool → new
  `baseline_dir` `m-auto-5-baseline-YYYYMMDD`. **Retain** `m-auto-4-baseline-20260604`
  (pointer-only move, reversible). Expect `suspect_baseline_manipulation` (explain).
- Keep the Mac awake for the full real-LLM run (a sleep-spanned run is uncertifiable
  — kill + re-run fresh).
- Do NOT launch the held S-Auto-17 validation overnight here — only surface it as now
  launchable on the honest baseline.

## Hard fences / STOP conditions

- **No prompt / routing / UC-hypothesis / escalation-posture / skill soft field /
  CaseSpec rubric edit.** Runtime edits are trace-contract only.
- **No rubric widening to accept a bot mistake (§1.7 / §5.4).** Structural fixes
  correct how the trace is read, never what counts as success.
- **Every structural fix (#1/#2/#3/#4) ships its anti-误杀 counter-test.** Incomplete
  without it.
- **#1 runtime stamping** must not fire on unresolved terminals.
- **#5 PII**: light first-party relaxation only; one sanity test that real
  user/third-party PII still flags. Not a focus.
- **Re-bless reversible**: fresh dated dir; never overwrite the retained baseline.
- **Run re-bless / eval only on a clean committed tree** (dirty-index hazard); keep
  the Mac awake.
- **Do NOT launch the overnight.**

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — eval-harness measurement correctness
(`eval_interactive/.../scoring/`) + runtime trace-contract completion
(`server/.../runtime/`). No `eval_spec` / `semantic_planner` / `prompt_projection` /
routing change.

**Tier-0 invariant:** adds none; preserves Tier-0 families at current strictness
EXCEPT the explicitly-authorized `no_pii_leakage` first-party relaxation (real
user/third-party PII still flags — sanity-tested).

**Semantic hardcode:** none. Structural reads (cross-turn union; dict-key read;
stop_reason-aware terminal disposition; session-accumulated source-id read) +
runtime containment/source_ids emission. First-party address exclusion is
declarative (company's own published system addresses), not a content rule.

**Generalization coverage:** measurement-infra — evidence is the paired
characterization + anti-误杀 counter-tests per structural artifact + the re-bless
verdict-distribution shift over all suites, not target/neighbor/negative/shadow
case-family counts. L4 shadow firewall unchanged.

## Test / eval requirements

- **eval_interactive pytest**: new characterization + anti-误杀 counter-tests green;
  no regression vs `503/0` under `uv run`.
- **Java**: runtime trace-contract change must not regress vs `1213/1/0/2`; new unit/
  integration tests for #1/#2 runtime green.
- **autoloop pytest**: untouched → no regression vs `324`.
- **Eval-only verdict-delta**: re-score the captured `m-auto-4-baseline-20260604`
  draws with the corrected eval checks (deterministic, no LLM) to isolate the
  eval-side contribution before the runtime change.
- **Real-LLM re-bless (§5.7)**: required — the authoritative `m-auto-5-baseline`
  artifact + the runtime-emission confirmation (containment/source_ids now present
  in real traces). Captured-trace fixtures are valid evidence for eval-side
  read-correctness.

## Codex review plan (§4.3)

PER-SUB-SPRINT RECOMMENDED; folds into M-Auto-5 milestone-shared close. NOT a
fence-#13 SHA trigger. Codex checklist: (1) each structural fix paired with an
anti-误杀 counter-test that genuinely fails; (2) #1 runtime never stamps resolved on
an unresolved terminal; (3) #5 PII relaxation scoped to first-party addresses, real
PII still flags; (4) no rubric widening — reads corrected, success-definition
unchanged; (5) no semantic/routing edit; (6) re-bless reversible + recorded.

## Handoff requirements (dev authors `docs/sprints/sprint-074-handoff.md`)

MUST include: per-artifact before/after (bug, corrected read/emission, file:line);
paired characterization + anti-误杀 counter-test for #1/#2/#3/#4 + the #5 sanity
test; the eval-only verdict-delta + the post-re-bless verdict-distribution shift vs
m-auto-4-baseline-20260604 (per suite + per artifact: false-fails cleared vs genuine
failures now surfaced); the re-bless record (new dir, old retained,
human-authorization, suspect_baseline_manipulation note); confirmation the held
overnight is now launchable (NOT launched); the list of genuine failures the
corrected measurement surfaces (input to the later semantic milestone — UC routing,
escalation posture); §7 self-classification; Codex deferral note.

## Commit discipline

Stage only authorized eval_interactive + server scope (NOT `git add -A`). One commit
per fix where practical (eval fixes / runtime fixes / re-bless artifact + pointer).
Run re-bless / eval only on a clean committed tree; keep the Mac awake for the
real-LLM run. Do NOT launch the overnight.

## Self-check checklist (dev completes before claiming done)

- [ ] #3 ATR unions across all turns; never-searched still FAILs; early-search +
      other-final-turn PASSes.
- [ ] #4 `intake_fields_collected` reads dict keys; cs066 PASSes; uc_g_gdpr still FAILs.
- [ ] #5 first-party exclusion (`noreply@gumtree.com` + confirmed set); real
      user/third-party email sanity-test still FAILs. (light — not a focus)
- [ ] #1 eval `trace_minimum` terminal-disposition-aware; goal_achieved/goal_impossible
      blank not Mode-1-fail; error/contract_violation blank + Mode-2 still FAIL;
      stop_reason threaded.
- [ ] #2 eval `source_citation_present` session-accumulated; alice-style PASSes;
      never-searched still FAILs.
- [ ] #1 runtime stamps `containment_outcome=resolved` on the success terminal; never
      on unresolved terminals; Java tests green.
- [ ] #2 runtime attaches resolved source ids to the answer turn; Java tests green.
- [ ] Backend rebuilt + restarted; eval-only verdict-delta produced.
- [ ] Real-LLM re-bless on a clean tree → `m-auto-5-baseline-YYYYMMDD`; old retained;
      Mac kept awake; suspect_baseline_manipulation explained.
- [ ] Java / autoloop / eval_interactive baselines no regression.
- [ ] No semantic / routing / CaseSpec-rubric edit; overnight NOT launched.
- [ ] Handoff written (per-artifact before/after + paired tests + verdict-delta +
      re-bless record + surfaced-genuine-failures list).
