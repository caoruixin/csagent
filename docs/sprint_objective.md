---
title: Sprint 077 / S-Auto-22 — eval-gate stall-not-gated fix + runtime resolved-stamp loop-aware downgrade (M-Auto-5 corrective #3 / close blocker)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-05
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-076-objective.md]
superseded_by: null
notes: >
  Corrective sub-sprint #3 of M-Auto-5. Opened 2026-06-05 from the framework-
  defect brief `docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md`.

  Background: the S-Auto-21 simfixed re-bless (`eval_interactive/results/
  m-auto-5-baseline-20260604-simfixed/`) eliminated near-coinflip, fired the
  S-Auto-20 stamp at corpus scale, and produced 9 F→P verdict flips with 0
  P→F flips — but inline trace review of the residual 12/216 gate-vacuous
  passes (5.5 %) revealed an OPPOSITE-direction measurement artifact: when
  `l2_results=[]` AND `containment_outcome="resolved"`, the eval gate
  computes `case_passed=true` regardless of whether `failure_tags` carry
  `STALL:*` or the FINAL `stop_reason` is a terminal-failure shape
  (loop_detected / goal_impossible / error). 2 of 3 sampled vacuous traces
  are real bot stalls being marked PASS (cs012 / uc_fp_removed — bot
  delivered a resolved-stamped answer earlier, then stalled on
  "I'm looking into this for you." until loop_detected fired). This
  inflates F→P-flip magnitudes (notably cs095 0.43→1.00 stable with 4/9
  vacuous; uc_a_visibility 0.00→1.00 stable; uc_fp_removed 0.43→0.89
  stable with ≥2/9 vacuous).

  Per human direction 2026-06-05: M-Auto-5 close is BLOCKED on this fix.
  The S-Auto-21 simfixed run remains on disk as FORENSIC evidence (proves:
  simulator fix eliminated near-coinflip; S-Auto-20 stamp fires at corpus
  scale; this stall-not-gated gap discovered). It is NOT an authoritative
  M-Auto-5 baseline. baseline_dir pointer NOT moved.
  docs/current_eval_baseline.md NOT flipped.

  S-Auto-22 ships the structural fix (eval-side gate + runtime-side
  stamp-downgrade companion), then a fresh re-bless on the corrected
  framework produces the authoritative M-Auto-5 baseline. Then paired-
  evidence review + milestone-shared Codex + (if both pass) pointer move
  + close.

  Framework-defect priority (§5.8): active — while this brief is open, do
  NOT launch new semantic sub-sprints; do NOT perform §5.6 bad-case rerun
  for milestone-close evidence.

  Dev session source-of-truth: compact/sprint-077-dev-prompt.md.
---

# Sprint 077 / S-Auto-22 — eval-gate stall-not-gated fix + runtime resolved-stamp loop-aware downgrade

## Class

- **Layer (primary)**: `infra` — eval-framework scoring gate
  (`eval_interactive/eval_interactive/scoring/composite.py` +
  `eval_interactive/eval_interactive/scoring/hard_checks.py`) and runtime
  trace-contract completion companion (`server/.../runtime/ControlKernel.java`
  + the BotSession containment-stamp persistence). §1.4 (eval contract +
  persistence); NOT §1.3 semantics. NO bot semantic / prompt / routing /
  UC-hypothesis / escalation-posture / skill / CaseSpec-rubric edit.
- **§7 stanza**: §7-EXEMPT (characterization / measurement-infra); stanza
  included below for rigor (touches gate-contributing eval checks + the
  runtime trace contract).
- **Codex review plan (§4.3)**: PER-SUB-SPRINT RECOMMENDED; folds into the
  M-Auto-5 milestone-shared close. NOT a fence-#13 SHA trigger (autoloop
  5-file scoring set untouched).
- **Framework-defect priority (§5.8) active**: while OQ-S77.stall-not-gated
  is open, NO new semantic sub-sprints; NO §5.6 bad-case rerun for
  milestone-close evidence (only the within-sub-sprint anti-误杀 counter-
  test reruns).
- **Position**: M-Auto-5 sub-sprint 4 of 4 (S-Auto-19 → S-Auto-20 →
  S-Auto-21 → **S-Auto-22**). After this sub-sprint ships + the re-re-bless
  on the corrected framework produces an authoritative baseline + paired-
  evidence review + Codex pass, M-Auto-5 closes. M-Auto-4 S-Auto-18
  deferred behind M-Auto-6.

## Goal

Close the OPPOSITE-direction measurement artifact the S-Auto-21 simfixed
re-bless exposed: the eval gate computes `case_passed=true` for stalled /
looped / impossible sessions when the per-turn `containment_outcome=
"resolved"` stamp fires earlier and `l2_results=[]` masks the structural
fail. Ship the structural fix in both layers (eval gate + runtime
companion) so the corrected framework can produce a verdict distribution
that reflects the bot's actual behaviour in BOTH directions (no
false-negatives AND no false-positives).

**This sub-sprint does NOT change the bot's customer-service ability.**
The runtime edit is a trace-contract correction (don't credit a stamp
that subsequent state invalidates); the eval edit is gate logic
(promote STALL / terminal-failure shapes to fail; refuse semantic-pass-
by-default on empty-L2).

## Naming clarifications (read before §Scope — there are two fields named with "2")

The four-tier scoring framework persists multiple fields per case in
`results.json`. Two of them carry "2" in their name and **are not the
same layer**:

- **`l2_results`** (plural list of `OutcomeCheckResult`) — the **L2
  outcome checks** layer (the historical L2). This is the field
  OQ-S77.stall-not-gated targets. `l2_results=[]` means no outcome
  check is defined / fired for the case; the mandatory-L2 filter
  becomes vacuously True at `composite.py:182-196`. Fixes #2 and #3
  below operate on this field.
- **`tier2_result`** (singular dict, `Tier2Result`) — the
  **skill-procedure check** (a DIFFERENT, fourth tier). It emits tags
  like `TIER2_ADVISORY:record-outcome-on-grounded-answer` and gates
  `case_passed` only when `severity="critical"` (at `composite.py:203`).
  Do NOT confuse `TIER2_ADVISORY:*` strings in `failure_tags` (which
  come from this skill-procedure layer) with `l2_results` (L2 outcome
  checks). They are separate signals; this sub-sprint does NOT modify
  `tier2_result` semantics.

The `case_passed` formula at `composite.py:205` is:

```
case_passed = l1_passed AND mandatory_l2_passed AND not tier2_critical_failed
```

— so when `l2_results=[]`, `mandatory_l2_passed` is vacuously True;
`tier2_critical_failed` is False if `tier2_result.severity == "advisory"`;
and `l1_passed` may still be True even when a stall happened
(stalls are not L1 checks). That's the gap OQ-S77 closes.

## Scope (executable, #1-#6)

### #1 — Eval gate: STALL signal promotion to `case_passed=false`

When the per-case result indicates a stall, the case MUST be marked
`case_passed=false` regardless of `composite_score`, `l2_results`,
`judge_score`, or `containment_outcome`. The framework already records
the stall signal on multiple equivalent surfaces — the dev picks ONE
of the three below as the structural read (justify the choice in
handoff §1):

- **(1a) `stall_detected == True`** (structured boolean on the
  case_result) — preferred. It is a typed boolean produced by the
  stall detector itself; reading it avoids string-prefix matching and
  is the most stable surface.
- **(1b) `status == "FAIL"`** — the framework already sets a per-case
  status field that says FAIL on these draws even when `case_passed`
  is True. Promoting status=FAIL to case_passed=false closes the
  internal inconsistency; the read is structured.
- **(1c) `failure_tags` contains `STALL:*` (prefix match)** —
  the failure-tags catalogue read. Less preferred because it relies
  on string conventions; use only if (1a) / (1b) are not viable for
  some reason discovered during implementation.

All three are equivalent on the OQ-S77 vacuous-pass corpus (every
draw with `stall_detected=True` also has `STALL:*` in `failure_tags`
and `status="FAIL"`); the dev's choice optimises code clarity, not
semantic coverage. Determine the right insertion point by reading
the composite/case_passed computation flow (likely lives in
`composite.py` around `:205` and the fall-through region near
`gating_l3 = [r for r in l3_results if severity != "advisory"]` at
`composite.py:228`).

Anti-误杀: a case with stall AND a legitimate partial-resolved answer
(rare) still fails — stall overrides resolve. The cost of a false-fail
on this rare combo is much less than the cost of false-passing every
stall. §1.6: a session that stalled is not "generalizable customer
problem-solving".

**Scope discipline**: this sub-sprint promotes the STALL signal only.
Do NOT promote `TIER2_ADVISORY:*` (those come from the skill-procedure
tier; their advisory severity is intentional). Do NOT promote
generic `failure_tags` populations. The cleaner read on a typed
field (1a/1b) is preferred over string-prefix matching, but the
scope is the same: one selective promotion of the existing stall
signal.

### #2 — Eval gate: terminal-failure stop_reason overrides earlier resolved stamp

When the FINAL `stop_reason` is in `{loop_detected, goal_impossible,
error, contract_violation, max_turns_exceeded}`, the gate MUST NOT
credit an earlier `containment_outcome="resolved"` stamp as success.
The session terminated in a failure shape regardless of any per-turn
observation upstream. Enumerate this set explicitly (do not use a "any
non-bot_ended/goal_achieved" complement — the explicit set is auditable
and won't accidentally promote a future benign stop_reason). Decide
whether to (a) override the gate's read of `containment_outcome` for
this case, or (b) fail the trace_minimum check on the stop_reason set
alone (similar to how S-Auto-20 removed `loop_detected` from
`_VALID_TERMINAL_STOP_REASONS`). Dev decides; justify in handoff.

Anti-误杀: `goal_achieved` and `bot_ended` are NOT in the override set
— a legitimately-completed session is still a pass. The override is
narrowly scoped to terminal-failure shapes.

### #3 — Eval gate: refuse semantic-pass-by-default when `l2_results=[]` + `composite=0`

When `l2_results=[]` (i.e. the CaseSpec has no L2 outcome checks AND/OR
no L2 check fired) AND `composite_score=0` AND
`containment_outcome="resolved"`, the gate MUST NOT mark
`case_passed=true` solely on the strength of the stamp. Options:

- **(3a)** Require an explicit case-level allowlist for "L2-not-
  applicable" cases (e.g. a CaseSpec field `l2_not_applicable: true` or
  similar). Cases not on the allowlist that arrive at this state are
  inconclusive → `case_passed=false` with a `verdict_reason=
  "no_l2_evidence_to_pass"` tag.
- **(3b)** Treat this state as inconclusive → `case_passed=null` (not
  true, not false) + `verdict_reason="no_l2_evidence"`. This requires
  downstream aggregation (`_rebless_report.json` stability + autoloop
  `tier_evaluator`) to handle `null`; verify before picking.
- **(3c)** Author L2 outcome checks for the 5 affected cases as a
  compensating CaseSpec edit, leaving the gate's fall-through behavior
  unchanged. This is `eval_spec` work, not `infra`, and it doesn't
  close the gap for the next case that's added without an L2 check —
  so it's a partial fix at best.

Dev picks one (or hybrid) and justifies in handoff. Recommended:
(3a) as the structural fix; (3c) only if a small number of cases are
genuinely L2-not-applicable AND the allowlist mechanism doesn't exist
yet (in which case land the allowlist mechanism + populate it).

### #4 — Runtime companion: void/downgrade `containment_outcome="resolved"` stamp when session subsequently fails

`server/.../runtime/ControlKernel.java` (or wherever the BotSession
`containment_outcome` field is persisted from the per-turn evaluation).
Add a session-level invariant: when the loop terminates in a failure
shape (`loop_detected` / `error` / `MAX_STEPS` / mid-resolution-without-
completion) AFTER an earlier turn had already stamped
`containment_outcome="resolved"`, void or downgrade the stamp so the
persisted trace's session-level field reflects the final state.

Three implementation options:

- **(4a)** Overwrite to a more accurate value (e.g.
  `containment_outcome="incomplete_after_partial_answer"` or
  `containment_outcome=null` with a separate
  `partial_resolution_history` field preserving the prior stamp's
  provenance).
- **(4b)** Add a session-level `containment_outcome_overridden_by:
  "loop_detected"` provenance field so the trace shows what happened.
- **(4c)** Leave the per-turn stamp alone but expose a derived
  session-level field `final_containment_outcome` that downstream
  consumers (including the eval gate from #2/#3) read instead.

Dev picks one; justify in handoff. Anti-误杀 (HARD): NEVER void a
stamp on a legitimately-completed session (`bot_ended` /
`goal_achieved` with no subsequent loop / error). The override is
strictly scoped to terminal-failure shapes.

Java unit tests required for each change: counter-test for legitimate
resolve still stamps `resolved` end-to-end; counter-test for earlier-
resolved-then-loop ends with the overridden / downgraded field.

### #5 — Anti-误杀 counter-tests (eval + runtime)

The structural fix above MUST preserve all 9 legitimate F→P flips the
S-Auto-21 simfixed run produced. Counter-tests:

- Re-score (deterministic, on the existing simfixed
  `_rebless_scratch/_attempts/a*/`) every case under the corrected eval
  gate. Expect:
  - 12 vacuous-pass draws (cs012 × 2, cs095 × 4, uc_b × 4, uc_a × 1,
    uc_fp_removed × 2) flip to FAIL.
  - The 9 legitimate F→P case-level flips (alice, cs012 majority,
    cs015, cs066, cs095 majority, uc_a_visibility majority,
    uc_f_billing, uc_fp_removed majority, cs01s01) — re-evaluate their
    `majority_passed` under the corrected gate. cs095 stable 1.00
    should become reducible-flaky or near-coinflip; uc_a_visibility
    stable 1.00 should soften; uc_fp_removed stable 0.89 should soften.
    cs066 1.00 stable, cs014 0.89 stable, fg5q 1.00 stable, cs029 1.00
    stable — these are NOT in the vacuous set; they should still PASS
    on the corrected gate. **A genuine F→P flip lost = anti-误杀
    regression; surface and route before close.**
- Java side: characterization test for the runtime stamp-downgrade
  (#4). A new test that asserts: (a) a goal_achieved/grounded-answer
  one-shot leaves `containment_outcome="resolved"` (S-Auto-20
  behavior preserved); (b) a resolve-then-loop session ends with
  the downgraded / overridden field.

### #6 — Re-bless on the corrected framework (real-LLM, §5.7 — HUMAN-LAUNCHED)

After #1-#5 ship + tests pass + Codex per-sub-sprint review passes:

- Author the re-re-bless command in the handoff §6 (analogous to
  S-Auto-21 handoff §6). Output dir suggestion:
  `eval_interactive/results/m-auto-5-baseline-20260604-simfixed-
  stalledfix` (or current-dated variant).
- Dev STOPS before launching. The HUMAN launches the re-bless after
  rebooting the backend (since #4 touches `server/`, this run requires
  a rebuilt backend, not the existing `45618df`/`1bc77c1` build).
- Deliver-agent runs paired-evidence review on the corrected baseline
  + drafts milestone-shared Codex prompt.

## Anti-误杀 invariants (HARD, non-negotiable)

1. **Preserve the 9 legitimate F→P flips from S-Auto-21**, modulo the
   vacuous-pass corrections in cs095 / uc_a / uc_fp_removed (those
   case-level pass_rates SHOULD soften, but the case shouldn't flip
   back to majority-FAIL unless evidence shows it deserves to).
2. **Stamp downgrade is scoped to terminal-failure shapes**, never to
   legitimate completion (`bot_ended` / `goal_achieved` with no
   subsequent loop).
3. **STALL:* promotion is selective**, not blanket-failure-tags
   promotion (`TIER2_ADVISORY:*` stays advisory; only `STALL:*` and
   future explicitly-promoted families warrant fail).
4. **No CaseSpec-specific allowlist for cs012/cs095/uc_b/uc_a/
   uc_fp_removed** — the fix is structural, not per-case (§1.7).
5. **No promotion of `TIER2_ADVISORY:*` to fail** — they exist for a
   reason; the M-Auto-5 close should not re-introduce false-fail bias
   in the opposite direction.

## Hard fences / STOP conditions

- Edit ONLY: `eval_interactive/eval_interactive/scoring/composite.py`
  + `eval_interactive/eval_interactive/scoring/hard_checks.py` (+ test
  files in `eval_interactive/tests/`); `server/.../runtime/ControlKernel.java`
  (+ Java test files). NO bot prompt / NO routing / NO UC-hypothesis
  / NO escalation / NO skill / NO CaseSpec rubric / NO simulator edit.
- DO NOT touch the simfixed S-Auto-21 forensic dir or any of the older
  baselines.
- DO NOT move `baseline_dir`; the post-S-Auto-22 re-re-bless is
  HUMAN-LAUNCHED; pointer move happens only after paired-evidence +
  Codex pass on the corrected baseline.
- DO NOT touch the autoloop 5-file scoring SHA-locked set.
- DO NOT launch a multi-suite re-bless from this dev session (HUMAN-
  GATED, §6).
- DO NOT open S-Auto-18 / M-Auto-4 / M-Auto-6 work.
- All eval / rerun only on a clean committed tree.

## Test / eval requirements

- `eval_interactive` pytest under `uv run pytest`: no regression vs
  current `538 passed` (S-Auto-21 baseline). New tests for #1/#2/#3
  + the anti-误杀 #5 set increment, not replace.
- Java suite: target `1232 / 1 / 0 / 2` baseline; new tests for #4 +
  the #5 runtime counter-test increment. The 1 inherited failure
  (`SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5) stays.
- autoloop pytest: untouched. Confirm `324` unchanged.
- Deterministic re-score on the existing simfixed
  `_rebless_scratch/_attempts/a*/`: 12 vacuous-pass draws flip to
  FAIL; 9 legitimate F→P case-level flips reassessed (specific
  expectations in #5).
- §5.9 pre-flight check (new, added to audit doc §6):
  `case_passed=true AND composite_score=0 AND l2_results=[] AND
  (failure_tags contains STALL:* OR stop_reason in
  {loop_detected, goal_impossible, error, contract_violation,
  max_turns_exceeded})` returns ZERO matches on the re-re-bless
  output. (Self-validating check that #1+#2 closed the gap.)

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` — eval-framework scoring gate
(`composite.py` + `hard_checks.py`) + runtime trace-contract companion
(`ControlKernel.java` + BotSession persistence). NO `semantic_planner`
/ `prompt_projection` / `skill_state` / `eval_spec` / `judge_calibration`
/ `product_policy` edit. The §3 layer for OQ-S77.stall-not-gated is
`infra` (eval-framework gate logic) — see brief.

**Tier-0 invariant:** adds none; preserves all Tier-0 families at
current strictness. The eval-gate edits and the runtime stamp downgrade
are measurement/persistence corrections, not bot-decision invariants.

**Semantic hardcode:** none. The STALL:* promotion (#1) keys on a
structural tag family the eval framework already emits; the terminal-
failure stop_reason override (#2) keys on an explicit enumerated set
(loop_detected / goal_impossible / error / contract_violation /
max_turns_exceeded), not on content/wording; the empty-L2 refusal (#3)
keys on data-shape; the runtime stamp downgrade (#4) keys on session
terminal state. None of these are keyword/regex/UC routing of the bot
or any §1.3 LLM-owned decision.

**Generalization coverage:** measurement-infra sub-sprint — evidence
is (a) deterministic re-score on the existing simfixed
`_rebless_scratch/_attempts/a*/` showing the 12 vacuous-pass draws now
FAIL and the 9 legitimate F→P case-level flips reassessed, (b) Java
counter-tests for #4, (c) eval contract-test for the §5.9 pre-flight
check on a synthetic vacuous-pass fixture, (d) the HUMAN-launched
re-re-bless real-LLM evidence — NOT target/neighbor/negative/shadow
case-family counts.

## Codex review plan (§4.3)

PER-SUB-SPRINT RECOMMENDED; folds into M-Auto-5 milestone-shared
close. NOT a fence-#13 SHA trigger.

Codex focus (in priority order):

1. No semantic-side rubric widening / no bot edit / no per-case
   allowlist for the affected cases.
2. STALL:* promotion (#1) is selective to `STALL:*` family, not
   blanket failure_tags promotion.
3. Terminal-failure stop_reason override (#2) is an enumerated
   explicit set, not a non-bot-ended complement.
4. Empty-L2 refusal (#3) is structural, not a per-case allowlist
   (unless the chosen option IS an allowlist mechanism — then verify
   the mechanism is generic, not cs-specific).
5. Runtime stamp downgrade (#4) preserves legitimate resolve
   (`bot_ended` + `goal_achieved` no-loop sessions still stamp
   `resolved`); fires only on terminal-failure shapes.
6. Anti-误杀 #5 evidence shows the 9 legitimate F→P flips reassessed
   honestly — not silently flipped back to FAIL.
7. §5.7 evidence gate: the HUMAN-launched re-re-bless is the real-LLM
   evidence; the deterministic re-score on simfixed scratch is
   evidence of #1/#2/#3 wiring but NOT primary evidence of the
   corrected baseline.
8. §5.9 pre-flight check is added to the audit doc §6 and the new
   check returns ZERO matches on the re-re-blessed corpus.

## Handoff requirements (dev authors `docs/sprints/sprint-077-handoff.md`)

MUST include:

- §0 cold-start summary (commits, test counts, what shipped / what
  NOT shipped / re-re-bless-ready confirmation).
- §1 #1 STALL:* promotion diff (file:line before/after + justification
  for insertion point).
- §2 #2 terminal-failure stop_reason override diff + the enumerated
  set + justification for option (a/b chosen).
- §3 #3 empty-L2 refusal diff + option (3a/3b/3c) chosen + justification
  + allowlist mechanism details if applicable.
- §4 #4 runtime stamp downgrade diff + option (4a/4b/4c) chosen +
  justification + Java unit-test evidence.
- §5 Anti-误杀 deterministic re-score table: per-case before/after
  pass_rate + stability classification under the corrected gate, on
  the simfixed scratch. Highlight any genuine F→P flip that softened
  vs the 12 vacuous-pass draws that correctly flipped to FAIL.
- §6 Re-re-bless-ready command + preconditions (backend rebuilt for
  #4; clean tree; Mac caffeinate; etc., analogous to S-Auto-21
  handoff §6).
- §7 STOP confirmations (`baseline_dir` NOT moved; old baselines
  retained; S-Auto-21 simfixed dir retained as forensic; full re-re-
  bless NOT run by dev; S-Auto-17 overnight / S-Auto-18 / M-Auto-6
  NOT touched).
- §8 §5.9 pre-flight check addition to audit doc §6 (new check text
  + insertion location).
- §9 OQ-S77.stall-not-gated framework-defect brief reference
  (`docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md`) +
  confirmation the brief's #1-#5 scope items are all addressed.

## Commit discipline

- Stage ONLY authorized files (NOT `git add -A`):
  - `eval_interactive/eval_interactive/scoring/composite.py`
  - `eval_interactive/eval_interactive/scoring/hard_checks.py`
  - `eval_interactive/tests/test_*.py` (new + updated)
  - `server/...runtime/ControlKernel.java` (+ companion files for #4)
  - `server/...test/...` (Java unit tests for #4)
  - `docs/sprints/sprint-077-handoff.md` (when written)
  - `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
    (§6 pre-flight check addition)
- One commit per substantive step where practical (#1, #2, #3
  separately if non-trivial; #4 + tests one commit; handoff one
  commit; audit doc edit one commit).
- The re-re-bless output dir is gitignored — reference in handoff §6,
  do not commit.
- Deliver-owned docs (this sprint_objective, the dev prompt, the
  milestone_objective update, the 10-handoff update, action_bank,
  launch-record §9) are committed separately by the deliver-agent +
  human bundle.

## Self-check checklist (dev completes before claiming done)

- [ ] #1 STALL signal promotion landed; one of (1a) `stall_detected`,
      (1b) `status=="FAIL"`, or (1c) `failure_tags STALL:*` chosen +
      justified in handoff §1; selective, NOT blanket failure_tags
      promotion; `TIER2_ADVISORY:*` NOT promoted; `tier2_result`
      semantics unchanged.
- [ ] #2 terminal-failure stop_reason override (enumerated set
      `{loop_detected, goal_impossible, error, contract_violation,
      max_turns_exceeded}`); `goal_achieved` and `bot_ended` NOT in
      the set.
- [ ] #3 empty-L2 refusal landed; option (3a/3b/3c) chosen + justified;
      no per-case allowlist for cs012/cs095/uc_b/uc_a/uc_fp_removed.
- [ ] #4 runtime stamp downgrade scoped to terminal-failure shapes;
      `bot_ended`/`goal_achieved` no-loop sessions still stamp
      `resolved` end-to-end (counter-test green).
- [ ] #5 deterministic re-score on simfixed scratch: 12 vacuous-pass
      draws → FAIL; 9 legitimate F→P flips reassessed (table in
      handoff §5); no silent flip-back.
- [ ] eval pytest no regression vs 538; new tests increment.
- [ ] Java `1232 / 1 / 0 / 2` baseline + new test count delta; the 1
      inherited failure stays.
- [ ] autoloop pytest 324 unchanged.
- [ ] §5.9 pre-flight check added to audit doc §6; the check returns
      ZERO matches when applied to the re-re-blessed corpus.
- [ ] `baseline_dir` NOT moved; S-Auto-21 simfixed dir NOT touched;
      full re-re-bless NOT run (left re-bless-ready).
- [ ] Handoff written per §Handoff requirements.
