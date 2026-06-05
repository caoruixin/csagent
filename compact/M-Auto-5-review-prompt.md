# Milestone Review Prompt — M-Auto-5 (Eval Verdict Correctness + Trace-Contract Honesty)

> Self-contained executable view of the M-Auto-5 milestone-shared Codex
> review per `docs/teams/deliver-agent.md` §Review prompt + `docs/current/
> iteration_governance.md` §4 + `docs/current/process/milestone-framework.md`
> §4.3. Paste into a fresh Codex session. You need NO other doc except
> `AGENTS.md` (auto-loaded) + this prompt + the sub-sprint handoff files
> listed in §3 (sub-sprint handoffs are dev artefacts and cannot be
> embedded; their paths are cited).

## 1. Role identity

You are the **Anti-Hardcode + Milestone-Close Review Agent for Milestone
M-Auto-5 — Eval Verdict Correctness + Trace-Contract Honesty**.

**Cumulative commit range under review**: `602d288..6578403` on branch
`auto-loop-branch` (about 20 commits spanning the M-Auto-5 milestone open
through the deliver-side close-evidence bookkeeping, NOT including the
re-re-bless launch record's outcome §9 which the deliver-agent will fill
after your verdict).

**Re-re-bless output dir (read-only, off git)**:
`eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/`
(the authoritative corrected-framework baseline produced 2026-06-05;
`baseline_dir` config pointer NOT moved — that happens only after your
verdict + paired-evidence pass).

**Forensic-only dirs (do NOT consume their stability classifications as
input)**: `m-auto-4-baseline-20260604/`, `m-auto-5-baseline-20260604/`,
`m-auto-5-baseline-20260605/`, `m-auto-5-baseline-20260604-simfixed/`.

## 2. Loader (minimal)

1. `AGENTS.md` — governance (Constitution §1, §1.5 / §1.7 anti-hardcode,
   §4 review process, §5 eval acceptance rules incl. §5.4 / §5.7 / §5.8 /
   §5.9, §7 stanza).
2. **This prompt** — the full review contract + embedded kernel +
   embedded milestone context.
3. Sub-sprint handoff files (dev artefacts; cite paths, do not embed):
   - `docs/sprints/sprint-074-handoff.md` — S-Auto-19 (eval-read column).
   - `docs/sprints/sprint-075-handoff.md` — S-Auto-20 (runtime-stamp
     column + loop_detected).
   - `docs/sprints/sprint-076-handoff.md` — S-Auto-21 (simulator
     role-inversion fix + drift guard + bad-case re-render).
   - `docs/sprints/sprint-077-handoff.md` — S-Auto-22 (eval-gate
     stall-not-gated fix + runtime stamp-downgrade companion; **two
     deviations** documented in §1 + §2 — see this prompt §5 + §6).
4. Diagnostic input artefacts (read for context if needed):
   - `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
     (S-Auto-21 trigger; §6 = the §5.9 pre-flight checklist).
   - `docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md`
     (S-Auto-22 trigger; framework-defect brief per §2.1 broadened
     intro).
   - `docs/diagnostics/2026-06-05-m-auto-5-rebless-launch-record.md`
     (S-Auto-21 simfixed run launch + §9 forensic classification).
   - `docs/diagnostics/2026-06-05-m-auto-5-rerebless-launch-record.md`
     (S-Auto-22 re-re-bless launch; §9 to be filled by deliver-agent
     AFTER your verdict).

Do NOT read `docs/archive/`. Do NOT consume the forensic dirs'
stability classifications.

## 3. Embedded milestone context

**Goal** (from `docs/milestone_objective.md`, paraphrased): make every
per-case eval verdict reflect the bot's actual behaviour, by correcting
four columns of measurement artifact across the eval framework:

- **OUTPUT-read** (S-Auto-19): five eval-side measurement-read
  corrections (ATR cross-turn union; intake dict-keys; first-party PII
  relaxation; trace_minimum terminal-disposition-aware; source_citation
  session-accumulated).
- **RUNTIME-stamp** (S-Auto-20): runtime stamps `containment_outcome=
  "resolved"` on the real `goal_achieved` one-shot path; eval-side
  removes `loop_detected` from `_VALID_TERMINAL_STOP_REASONS` so a
  looped session can't vacuous-pass.
- **INPUT-column** (S-Auto-21): customer simulator role-map inversion
  fix at `eval_interactive/.../simulator/user_simulator.py:187`,
  per-turn persona re-anchor, negative-form `Forbidden` block,
  customer-voice drift guard (D1 keywords / D2 verbatim-regurgitation
  Jaccard / D3 system-prompt-leakage probes; 3-attempt retry +
  `SimulatorDriftError` escape); T1-T7 contract tests + bad-case suite
  re-render with real-LLM evidence (0/40 contamination on the focused
  re-render; 852→0 across the pre-fix corpus).
- **GATE/VACUOUS-PASS** (S-Auto-22): eval-gate STALL signal promotion
  (option 1a typed `stall_result.detected` boolean read, scoped to
  `composite==0` — DEVIATION carried to your verdict) + terminal-failure
  stop_reason override (option 2b separate `_check_trace_minimum`
  failure arm, frozenset `{loop_detected, error, contract_violation,
  max_turns_exceeded}` — `goal_impossible` excluded DEVIATION carried
  to your verdict) + zero-evidence refusal (`composite==0 AND
  l2_results==[]` structural rule, no per-case allowlist) + runtime
  void/downgrade companion (`ControlKernel.shouldVoidResolvedStamp` /
  `voidResolvedStamp` + `BotSession.priorContainmentOutcome`
  provenance; runtime-observable failure terminals `{MAX_STEPS, ERROR,
  DEADLINE_EXCEEDED, LLM_UNAVAILABLE}` only — FINAL_ANSWER never
  voided).

**Sub-sprint sequence** (all ACCEPTED):

1. S-Auto-19 / Sprint 074 — eval read column (ACCEPTED-WITH-FOLLOWUP
   2026-06-04; runtime #1/#2 stamps were initially inert on the real
   corpus, leading to S-Auto-20).
2. S-Auto-20 / Sprint 075 — runtime stamp + `loop_detected` (ACCEPTED
   2026-06-04; broadened `ControlKernel.isResolvedSuccessTerminal` to
   FINAL_ANSWER + ANSWERED_SUBTASK with grounding; §5.7 validated
   against a real `goal_achieved` trace; 9/9 anchor goal_achieved
   stamped resolved at corpus scale).
3. S-Auto-21 / Sprint 076 — simulator role-inversion fix (ACCEPTED
   2026-06-04; #1 root-cause role-map inversion at user_simulator.py
   :187; bad-case re-render 0/40 contamination; the re-bless launched
   2026-06-05 from `1bc77c1` produced the simfixed forensic baseline
   which then revealed the OPPOSITE-direction stall-not-gated gap).
4. S-Auto-22 / Sprint 077 — eval-gate vacuous-pass closure (ACCEPTED-
   WITH-DEVIATIONS-DOCUMENTED 2026-06-05; the re-re-bless on the
   corrected framework launched 2026-06-05 from `6578403`).

**Milestone acceptance bar** (from `docs/milestone_objective.md`):

1. Structural fixes carry paired evidence — characterization +
   anti-误杀 counter-tests per artifact.
2. Verdict-distribution shift vs `m-auto-4-baseline-20260604` is
   explainable — judge layer EXCLUDED (per OQ-S76.judge-zero
   resolution 2026-06-05, route c, chronic by-config); canonical
   signal = composite + outcome + L1 + L2 `failure_tags`.
3. Grounding floor intact; safety floor intact (PII relaxation
   scoped to benign first-party addresses only).
4. **Codex §4.1 nine-question kernel pass** ← this review.
5. Re-bless recorded + reversible; old baselines retained.

## 4. Embedded §4.1 nine-question kernel (canonical copy)

You are the Anti-Hardcode Review Agent. The PR below proposes a change
to this repo. Your job is to decide whether the change introduces a
semantic hardcode — a keyword / regex / if-else / enum / per-UC matrix
that encodes a decision the LLM is supposed to own under
`docs/current/iteration_governance.md` §1.3 — and to issue a verdict.

Scope exemption: pure infra, docs-only, config-governance, and
characterization-test PRs are not subject to this review. If the
cumulative range is purely one of those, return `approve` with a one-
line note naming the exemption.

For the cumulative M-Auto-5 range, walk these nine questions in order.
For each "yes" or each concern, paste the diff snippet (use
`git show <commit>` or `git diff 602d288..6578403 -- <path>`) and the
reasoning.

1. Does the PR add a keyword / regex / if-else / enum / per-UC matrix
   for a semantic decision (drift detection, escalation, UC selection,
   risk classification, follow-up, intake routing)?
2. If yes to (1), is the change justified as protecting a current
   Tier-0 invariant named in `docs/runtime_freeze_and_risk_policy.md`
   §1 / §2?
3. Could the same outcome be achieved by projecting a soft signal to
   the LLM (an additional projected slot, a candidate list, a
   diagnostic flag) instead of a hard branch in Java or the prompt?
4. Does the change encode visible-eval case text, trace-specific
   phrasing, or a CaseSpec id into runtime, prompt, or judge config?
5. Does the change move semantic ownership from the LLM to Java —
   that is, shrink what `docs/current/iteration_governance.md` §1.3
   says the LLM owns?
6. Does the change add an if-else block to the prompt instead of
   principle-level or observable-state guidance?
7. Does the change preserve tool schema, capability / permission
   boundary, PII / safety floor, and grounding floor?
8. Does the PR ship generalization eval coverage — target, neighbor,
   negative, and shadow cases — and not only the target case?
9. If the change is temporary, does it carry an explicit rollback or
   sunset plan (downgrade-to-signal trigger, retirement sprint id)?

Return exactly one verdict:

- `approve` — the change is not a semantic hardcode, or is justified
  as protecting a current Tier-0 invariant with adequate
  generalization coverage and a clear rollback if temporary.
- `approve with downgrade-to-signal follow-up` — the change is
  acceptable as an interim measure, but a follow-up sprint must
  convert it into a soft signal projected to the LLM. Name the
  trigger that should fire the conversion.
- `reject as semantic hardcode` — the change encodes a soft semantic
  decision the LLM should own; questions 1 and 2 fail, or questions
  5 / 6 fail, with no Tier-0 claim and no sunset plan.
- `needs human architecture decision` — the change crosses an
  unresolved governance question (new escalation reason enum value,
  new Tier-0 candidate, LLM-vs-Java boundary shift, new tool surface)
  and a human reviewer must decide before merge.

Do not rewrite the PR. Do not propose a code fix beyond naming the
layer in `docs/current/iteration_governance.md` §3 that the fix should
target.

## 5. Cumulative scope claim (per sub-sprint commits + ship artefacts)

This is the deliver-agent's summary of what landed in each sub-sprint.
Cross-check against the sub-sprint handoff files (§2.3) and the diff
itself (`git log --oneline 602d288..6578403`).

### 5.1 S-Auto-19 / Sprint 074 — eval-read column

Commits: `602d288` (open) → `47b3060` (eval-side 5 measurement-read
corrections + anti-误杀 counter-tests) → `858e58b` (runtime: trace-
contract completions for #1 containment + #2 source_ids) → `4cefd5f`
(eval-only verdict-delta re-score) → `0591649` (dev handoff).
Eval pytest 522 (S-Auto-20 baseline was 522). Java +16 unit tests for
the runtime stamps. Note: S-Auto-19's runtime stamps were INERT on the
real corpus (READY_TO_CONFIRM gate too narrow) — that gap was closed
by S-Auto-20 Fix #1; S-Auto-19's eval-side reads were SOUND and stand.

### 5.2 S-Auto-20 / Sprint 075 — runtime stamp + loop_detected

Commits: `69742f3` (open corrective sub-sprint) → `95cd0f4` (runtime:
broaden resolved-stamp to the real `goal_achieved` one-shot path) →
`e6e57ec` (eval: `loop_detected` no longer vacuous-passes
`trace_minimum`) → `da7ef15` (dev handoff). Fix #1 broadened
`ControlKernel.isResolvedSuccessTerminal` to FINAL_ANSWER +
ANSWERED_SUBTASK with grounding; §5.7-validated against a real
`goal_achieved` trace (session `6965f6dc-…` persisted
`containment_outcome="resolved"`). Fix #2 removed `loop_detected` from
`HardChecker._VALID_TERMINAL_STOP_REASONS`. Java `1232/1/0/2` (+3 unit
tests); eval pytest 524 (+2).

### 5.3 S-Auto-21 / Sprint 076 — simulator role-inversion fix

Commits: `a1d9dcb` (open) → `5471f9e` (#1-#4 sim role-fix + customer-
voice drift guard) → `ba47ec6` (#5 contract tests T1-T7) → `45618df`
(dev handoff) → `681b492` (sub-sprint close bundle) → `1bc77c1`
(OQ-S76.judge-zero resolution bookkeeping) → `9e4907e` (S-Auto-21
re-bless launch record) → `e5b0d27` (SHA self-reference refresh) →
`0bc37e2` (governance §5.8 / §5.9 + 2026-06-04 audit input artifact —
**HUMAN-AUTHORED**). The re-bless launched from `1bc77c1` produced
`results/m-auto-5-baseline-20260604-simfixed/` (retained as forensic).
eval pytest 538 (+14 net new in `test_user_simulator.py` T1-T7 + drift
detector unit coverage); Java untouched-by-construction.

### 5.4 S-Auto-22 / Sprint 077 — eval-gate stall-not-gated + runtime stamp-downgrade companion

Commits: `cf73df1` (open corrective #3 / close blocker; brief +
contract + dev prompt + audit doc §6 pre-flight check #6 addition) →
`021a86a` (contract clarifications: four-tier framework naming + STALL
signal multi-surface options) → `2ea65de` (dev close OQ-S77
vacuous-pass gate) → `0bd63cd` (sub-sprint close bundle + re-re-bless
launch record) → `6578403` (M-Auto-6 planning context — record only,
no scope opened). The re-re-bless launched from `6578403` produced
`results/m-auto-5-baseline-20260604-simfixed-stalledfix/` (the
authoritative corrected-framework baseline). eval pytest 553 (+15 net
new in `test_oq_s77_false_positive_gates.py`); Java 1244/1/0/2 (+12
new in `ControlKernelVoidResolvedStampTest`; the 1 inherited failure
`SystemPromptUserRequestedTiebreakerTest` is OQ-S41.5 unchanged);
autoloop pytest 324 unchanged.

## 6. Specific focus points the deliver-agent surfaces for your verdict

Per human direction 2026-06-05 at the close-evidence review, address
these items EXPLICITLY in your output. Each item is a yes/no question
plus the §4.1 question(s) it tests.

### 6.1 S-Auto-22 Deviation #1 — STALL promotion scoped to `composite==0` (not blanket)

**Deliver-agent's current stance**: **currently accepted as
evidence-supported**, pending your independent verification. Dev's
evidence (`docs/sprints/sprint-077-handoff.md` §1 "DEVIATION"): the
prompt's literal §4 #1 said promote "regardless of composite_score";
dev scoped to `composite==0`. Reason: cs095 a4 has
`stall_detected=True + composite=0.5 + containment=escalated` (validly
escalated; stall_detector false-positive on out-of-window recovery —
verified by reading the a4 transcript). Blanket promotion would mis-fail
cs095 a4 and drop cs095 to 4/9 majority FAIL, losing a tracked F→P
flip; the prompt's own §4 #5 expected cs095 at 5/9 (the scoping
produces exactly that). OQ surfaced: `OQ-S77.stall-detector-window`
(`action_bank.md` §5.2) — stall_detector window-tuning question, NOT
re-re-bless blocker.

**Your task**: independently verify the deliver-agent's stance. Read
the dev's evidence (handoff §1 + the a4 transcript) and the
deterministic re-score (handoff §5). You **may push back**: if you
judge the `composite==0` scoping is a stealth weakening of the
structural fix (§1.5 / §1.7 violated; or §5 anti-误杀 invariant #1
mis-read), classify the issue as **P0 or P1** and state the **minimal
fix scope** (e.g. "narrow scoping to stall_detected==True AND
composite==0 AND <additional predicate X>" or "promote regardless of
composite_score AND open OQ-S77.stall-detector-window as a hard close
blocker"). If you concur with the deliver-agent's stance, say so
explicitly and move on.

Maps to §4.1 questions 1, 2, 5, 6, 8.

### 6.2 S-Auto-22 Deviation #2 — terminal-failure override excludes `goal_impossible`

**Deliver-agent's current stance**: **currently accepted as
evidence-supported**, pending your independent verification. Dev's
three-fold justification (`docs/sprints/sprint-077-handoff.md` §2
"DEVIATION"): (a) S-Auto-20 precedent — `_VALID_TERMINAL_STOP_REASONS`
deliberately kept `goal_impossible` as AMBIGUOUS ground truth ("the
simulator persona declaring the issue unresolvable can reflect a
hard-to-satisfy persona rather than a bot fault"); (b) anti-误杀
evidence — including `goal_impossible` would mis-fail full-evidence
shadow cs32s02 a0/a2/a4 (`composite=0.5, l2n=5, goal_impossible,
resolved` — NOT in vacuous-12, NOT in §4 #5's expected flips); (c) #2
↔ #4 consistency — `goal_impossible` is a SIMULATOR-side verdict the
runtime never observes, so the #4 runtime companion cannot mirror it;
the prompt's own #4 set is `{loop_detected, error, MAX_STEPS}` and
`goal_impossible` is absent there. The vacuous-goal_impossible draws
that MUST fail (cs095 a0/a6/a8) are still gated by Fix #3 (composite
==0 AND l2==[]). OQ surfaced:
`OQ-S77.goal-impossible-resolved-evidence` (`action_bank.md` §5.2) —
`eval_spec` deferral.

**Your task**: independently verify the deliver-agent's stance. Read
the dev's evidence (handoff §2 + the cs32s02 a0/a2/a4 traces + the
S-Auto-20 `_VALID_TERMINAL_STOP_REASONS` comment). You **may push
back**: if you judge excluding `goal_impossible` re-introduces
verdict-incorrectness this milestone existed to close (e.g. the
S-Auto-20 precedent doesn't carry across to S-Auto-22's eval-gate
context; or the cs32s02 anti-误杀 evidence is weaker than claimed; or
the #2 ↔ #4 consistency argument is unsound), classify the issue as
**P0 or P1** and state the **minimal fix scope** (e.g. "include
`goal_impossible` in the terminal-failure set AND make Fix #3 stricter
to compensate for the cs32s02 false-fail"). If you concur with the
deliver-agent's stance, say so explicitly and move on.

Maps to §4.1 questions 1, 5, 7, 8, 9.

### 6.3 §5.9 pre-flight sweep — OQ-S77 closure validity

The deliver-agent ran the §5.9 pre-flight sweep (per `docs/diagnostics/
2026-06-04-eval-framework-and-simulator-audit.md` §6 step 6) on the
re-re-blessed corpus:

- **Total draws scanned**: 414 (9 attempts × 3 suites × 12-22 cases).
- **Fingerprint hits** (`case_passed=true AND composite=0 AND
  l2_results=[] AND (STALL:* OR stop_reason in
  {loop_detected, goal_impossible, error, contract_violation,
  max_turns_exceeded})`): **0**.
- **VERDICT**: PASS (the OQ-S77 fingerprint is closed at corpus scale).

**Question for your verdict**: is 0/414 sufficient evidence to close
OQ-S77.stall-not-gated at the M-Auto-5 milestone close AND to lift the
§5.8 framework-defect priority? Or are there scenarios outside this
fingerprint that the gate could still let through?

Maps to §4.1 questions 7, 8.

### 6.4 Paired-evidence verdict-shift (m-auto-4-baseline-20260604 → simfixed-stalledfix)

| Suite | F→P flips | P→F flips |
|---|---|---|
| bad_cases | **5** (alice, cs012, cs015, cs066, cs095) | **0** |
| anchor_outcome | **3** (uc_a_visibility, uc_f_billing, uc_fp_removed) | **0** |
| shadow | **2** (cs01s01, cs11s01) | **0** |

**Total: 10 F→P, 0 P→F.** Anti-误杀 in the false-negative direction
HELD. All F→P flips are cases where the M-Auto-5 corrections removed
measurement artifacts that were inflating the case's failure count.
The bot is byte-identical (modulo trace-contract persistence) — the
verdict shift is measurement correctness, not bot improvement.

**Question for your verdict**: is the m-auto-4 → stalledfix verdict
distribution shift fully explainable by the four columns (S-Auto-19
eval reads / S-Auto-20 runtime stamp + loop_detected / S-Auto-21
simulator role inversion / S-Auto-22 gate vacuous-pass), with no
unexplained flip? (You can spot-check against the per-case stability
table in `_rebless_report.json` for each baseline.)

Maps to §4.1 questions 4, 5, 7, 8.

### 6.5 Persistent high-risk failures preserved

Across all three runs (m-auto-4 / simfixed / stalledfix), every
high-risk under-escalation case stayed at `pass_rate=0.000` stable:

- **anchor_outcome**: uc_g_gdpr, uc_h_appeal, uc_i_payment,
  uc_j_safety — all 0.000 stable across all three baselines.
- **shadow**: cs38s01 (scam-seller-full-narrative), cs38s02
  (harassment-full-narrative) — both 0.000 stable; also cs15s01/02
  (empty-form-image flag / keyword-match-appeal), cs40s01/02
  (login-intermittent / image-upload-timeout), cs92s01/02
  (charity-donation / pickup-only-no-price), cs32s01 (reverse-drift)
  — all 0.000 stable.

**Question for your verdict**: is the persistent-failure preservation
sufficient evidence that the M-Auto-5 corrections did NOT
over-correct in the false-positive direction on safety-floor /
high-risk surfaces? (I.e., no genuine high-risk failure got
silently flipped to PASS by the eval-gate / runtime / simulator /
gate changes.)

Maps to §4.1 questions 5, 7, 8.

### 6.6 `near-coinflip` re-emergence as corrected measurement signal

The S-Auto-21 simfixed run produced **0+0+0** `near-coinflip` cases
across all three suites (the vacuous-pass gate gap was inflating
genuinely flaky cases to stable 1.000 — most dramatically cs095
0.43 → 1.000 stable, with 4/9 vacuous draws among the passes).

The S-Auto-22 stalledfix run produces:

- bad_cases **2 near-coinflip** (cs001 0.444 + cs095 0.556).
- shadow **1 near-coinflip** (cs01s02 0.444).
- anchor_outcome **0 near-coinflip**.

cs095 0.556 stalledfix matches the dev's deterministic re-score
expectation `5/9 ~0.56` EXACTLY. The re-emergence is the corrected
gate exposing genuine cognitive ambiguity at p ≈ 0.5, not
re-introducing measurement noise.

**Deliver-agent's interpretation**: this is **corrected measurement,
NOT regression**. The corrected gate removed inflated simfixed passes
(cs095 0.43 → 1.000 stable in simfixed had 4/9 vacuous-pass draws)
and cs095 returned to the expected ~5/9 band on the corrected gate
(stalledfix 0.556, exactly matching the dev's deterministic re-score
prediction of "5/9 ~0.56"). cs001 (0.444) and cs01s02 (0.444) are
likewise cases where the prior runs' stability classifications masked
genuine flakiness rather than reflecting it.

**Your task**: verify or push back. If you judge the near-coinflip
re-emergence is regression (e.g. the corrected gate is now over-failing
real successes; or the cs095 / cs001 / cs01s02 traces show real bot
capability not genuine ambiguity), classify as **P0 or P1** and state
the **minimal fix scope**.

Maps to §4.1 questions 4, 7, 8.

### 6.7 Known Cluster C.1 (cs59s* shadow `session_create_failed:400`) deferred to M-Auto-6

Shadow `cs59s01_uc_d_empty_form_account_recovery` and
`cs59s02_uc_f_empty_form_payout_timing` continue to abort with
`session_create_failed:400` across all three baselines, classified as
`non_comparable` (2 of 22 shadow cases). The shadow run's 18 / 198
error mass (9.6%) is entirely these two cases × 9 attempts.

The audit (`docs/diagnostics/2026-06-04-eval-framework-and-simulator-
audit.md` §4.1) identified this as Cluster C.1; routed to M-Auto-6
(parallel research-agent dispatch after M-Auto-5 close).

**Question for your verdict**: is deferring Cluster C.1 to M-Auto-6
reasonable, given that (a) it's NOT a regression introduced by
M-Auto-5; (b) it manifests as `session_create_failed` BEFORE any
in-scope eval/runtime/simulator code path executes; (c) the two
affected cases are explicitly `non_comparable` and excluded from
the majority vote? Or does it block M-Auto-5 close?

Maps to §4.1 questions 7, 8.

### 6.8 Judge layer excluded from paired-evidence per OQ-S76.judge-zero

OQ-S76.judge-zero (resolved 2026-06-05 route c, collapsed into
chronic `R-eval-interactive-judge-score-never-populated` per
`action_bank.md` §5.2) confirmed `mean_judge=0.0` is chronic
by-configuration, byte-identical 0.0 across all four M-Auto-5
bracketing runs (m-auto-4 / m-auto-5-0604 / m-auto-5-0605 /
simfixed). Two sub-variants: (a) `bad_cases/anchor_outcome` ship
`llm_judge_dimensions: []`; (b) `shadow` invokes L3 but every dim is
`severity="advisory"`, stripped at `composite.py:228`. Both reach
the `judge_score=0.0` fallback.

Canonical paired-evidence signal: **composite + outcome + L1 + L2
`failure_tags`** — judge EXCLUDED.

**Question for your verdict**: is the judge-layer exclusion sound for
the M-Auto-5 close paired-evidence review? (The R-item stays LOW
priority; future re-promotion trigger = a semantic-flexibility sprint
that needs a working `groundedness` or similar gate.)

Maps to §4.1 questions 4, 7.

### 6.9 baseline_dir NOT to move until your verdict + paired-evidence pass

The `config.fitness.baseline_dir` config pointer currently stays at
`m-auto-4-baseline-20260604`. The deliver-agent does NOT move it under
any circumstance until:

1. **You return a positive verdict** on this review (`approve` or
   `approve with downgrade-to-signal follow-up`).
2. The deliver+human paired-evidence review against the bad-case suite
   on the re-re-blessed baseline passes.
3. Both reviews carry into the M-Auto-5 close decision.

If your verdict is `reject as semantic hardcode` or `needs human
architecture decision`, the pointer stays unmoved AND the deliver-agent
classifies per `docs/teams/deliver-agent.md` §Milestone close
(fix-iteration / out-of-scope / in-flight downgrade).

This is the deliver-side STOP. Do NOT write code, edit the config,
or otherwise touch the runtime — your output is a verdict, not a
change.

## 7. Output format

Write your decision to `docs/codex-findings.md` using the §4.2 header
+ a top-line milestone-close verdict enum (deliver-agent will archive
to `docs/milestones/M-Auto-5_codex-review.md` at close):

```
## Sprint Review Decision
decision: pass | fix_required | out_of_scope_review
blocking_count: <number>
final_verdict: APPROVE_M_AUTO_5_CLOSE
            | APPROVE_WITH_NON_BLOCKING_OBSERVATIONS
            | BLOCK_M_AUTO_5_CLOSE
summary: <one paragraph naming the verdict + the one most-important
         reason; max 4 sentences>
```

`final_verdict` is the canonical milestone-close signal the deliver-
agent reads:

- **`APPROVE_M_AUTO_5_CLOSE`** — no concerns; deliver-agent proceeds
  with pointer move + `current_eval_baseline.md` flip + close archive
  sweep.
- **`APPROVE_WITH_NON_BLOCKING_OBSERVATIONS`** — close proceeds;
  observations carry into M-Auto-6 or future planning. Use this when
  your concerns are real but don't invalidate the M-Auto-5 close
  evidence (e.g. stylistic, future-improvement, or scope-creep
  warnings).
- **`BLOCK_M_AUTO_5_CLOSE`** — at least one P0 or P1 finding
  directly invalidates M-Auto-5 close evidence. Deliver-agent will
  NOT move the pointer; you MUST specify each P0/P1 with file:line +
  the **minimal required fix scope** in §4 below.

Followed by:

- **§1 Cumulative scope assessment** — one paragraph per sub-sprint
  (S-Auto-19 through S-Auto-22) classifying each as
  `approve` / `approve with downgrade-to-signal follow-up` /
  `reject as semantic hardcode` / `needs human architecture decision`.
- **§2 Nine-question kernel walkthrough** — one paragraph per question
  (1-9 from §4 of this prompt) with diff citations for any "yes"
  answer or concern.
- **§3 Specific-focus-points verdicts** — one paragraph per §6.1
  through §6.9 above, with explicit verify-or-push-back answer to each.
- **§4 Blocking findings (if any)** — numbered P0 / P1 with file:line
  + diff snippet + the §3 layer the fix should target + the **minimal
  required fix scope** (the smallest change set that closes the
  finding without expanding M-Auto-5 scope). Each P0 / P1 cites the
  specific scope item that must close before M-Auto-5 closes.
- **§5 Non-blocking observations (if any)** — items that don't gate
  close but should be carried into M-Auto-6 / future planning.

## 8. Constraints

- **Do NOT edit code** under any path. Your output is a verdict + a
  report; you do not propose diffs.
- **Do NOT move `baseline_dir`** or flip `docs/current_eval_baseline.md`
  — that is the deliver-agent's action AFTER your verdict + paired-
  evidence + (if all pass) the close decision.
- **Do NOT modify any file** under the forensic baseline dirs
  (`m-auto-4-baseline-20260604/`, `m-auto-5-baseline-20260604/`,
  `m-auto-5-baseline-20260605/`, `m-auto-5-baseline-20260604-simfixed/`)
  or the authoritative dir
  (`m-auto-5-baseline-20260604-simfixed-stalledfix/`).
- **Forensic dirs read policy**: you **MAY read** them for historical
  comparison and verdict-shift analysis (e.g. cross-baseline pass_rate
  diff; per-case stability trajectory; cross-time evidence for whether
  a cs032s02-style draw existed pre-S-Auto-22). You **MUST NOT** treat
  their stability classifications or pass-rates as authoritative
  close-evidence baselines for your verdict. The **authoritative
  close-evidence baseline is the stalledfix re-re-bless dir only**
  (`m-auto-5-baseline-20260604-simfixed-stalledfix/`); the four
  forensic dirs are pre-correction snapshots.
- **Do NOT re-judge bad-case semantic correctness using fresh LLM
  judgment.** Your review reads code, artefacts (sub-sprint handoffs,
  diagnostic docs, brief), recorded traces and results in the
  baseline dirs, and the stated evidence in this prompt. Do not
  spawn a fresh judge LLM call, do not paraphrase a bot's intent, do
  not opine on whether a given bot reply "should have been better".
  The §5.6 bad-case manual review human verdict is the human's; you
  flag cases worth their attention in §3 / §5 of your output but do
  not adjudicate the case PASS / FAIL boolean.
- **Do NOT turn M-Auto-6 follow-up scope into an M-Auto-5 blocker
  unless it directly invalidates M-Auto-5 close evidence.** M-Auto-6
  candidates (R1/R2 runtime/infra bundle per
  `docs/solutions/2026-06-05-runtime-bad-cases-handover-schema-discover-counter.md`;
  audit Clusters B + C; OQ-S77.stall-detector-window; OQ-S77.goal-
  impossible-resolved-evidence; OQ-S76.A6) are recorded in
  `docs/action_bank.md` §5.2 for forward planning. Note implications
  for M-Auto-6 sequencing in §5 (non-blocking observations); do NOT
  promote them to §4 (blocking) unless the specific item directly
  invalidates a piece of M-Auto-5 close evidence (e.g. "the §5.9
  sweep result is meaningless because the §5.8 framework-defect
  priority's gate definition is wrong" would qualify; "R1 schema
  noise is real and explains uc_f_billing 1.000 stable" would NOT —
  it's a forward-planning signal, not a close blocker).
- **Per-sub-sprint Codex review** was deferred to this milestone-shared
  close per the original deliver-agent convention; your verdict is the
  cumulative one (no per-sub-sprint sub-verdicts required, but §1 of
  your output does ask for a per-sub-sprint classification).
