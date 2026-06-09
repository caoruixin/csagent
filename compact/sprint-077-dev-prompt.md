# Dev Prompt — Sprint 077 / S-Auto-22 (M-Auto-5 corrective sub-sprint #3 / close blocker)

> Self-contained executable view of `docs/sprint_objective.md` (prompt-artifact-rules §9).
> Paste into a fresh dev session. You need NO other doc except `AGENTS.md`
> (auto-loaded) + this prompt. Code anchors in §11 are read on demand.

## 1. Role identity

You are the **dev agent for Sprint 077 / S-Auto-22**, the corrective sub-sprint
#3 / close blocker of Milestone **M-Auto-5 — Eval Verdict Correctness +
Trace-Contract Honesty**.

**One-line goal:** close the OPPOSITE-direction measurement artifact the
S-Auto-21 simfixed re-bless exposed — the eval gate marks stalled / looped /
impossible sessions as `case_passed=true` when an earlier turn stamped
`containment_outcome="resolved"` AND `l2_results=[]` masks the structural
fail. MEASUREMENT/INFRA only: NO bot semantic / prompt / routing /
UC-hypothesis / escalation / skill / CaseSpec-rubric edit. The runtime
edit (#4) is a §1.4 trace-contract correction (don't credit a stamp that
subsequent state invalidates).

## 2. Read order (minimal)

1. `AGENTS.md` — governance (Constitution §1, §1.5/§1.7 anti-hardcode, §5.4
   no eval-side override of a real bug, §5.7 real-LLM evidence gate, §5.8
   framework-defect priority, §5.9 pre-flight QA gate, §7 stanza).
2. `docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md` — the
   framework-defect brief that opens this sub-sprint (six-field schema +
   distribution + sampled traces + tempting-but-wrong fixes).
3. **This prompt** — the full contract.
4. On demand, the §11 code anchors.

Do NOT read `docs/sprints/*` or `docs/archive/*`. The S-Auto-21 simfixed
results at `eval_interactive/results/m-auto-5-baseline-20260604-simfixed/`
are READ-ONLY input — do not modify; do not delete.

## 3. WHY (read before coding)

S-Auto-21 ACCEPTED with the simfixed re-bless producing:

- **0 contamination** on the re-rendered bad-case suite (real-LLM, §5.7).
- **`near-coinflip` eliminated** across all three suites (m-auto-4 had 2+1+1;
  simfixed has 0+0+0).
- **S-Auto-20 stamp fires at corpus scale** (anchor_outcome: 9/9
  goal_achieved → resolved).
- **9 F→P flips, 0 P→F flips** in the verdict shift vs `m-auto-4-baseline-
  20260604`. Anti-误杀 (false-negative direction) held.

But inline trace review at S-Auto-21 close revealed the OPPOSITE-direction
defect:

- **12 of 216 bad_cases+anchor_outcome draws (5.5 %)** share a fingerprint:
  `case_passed=true` + `composite_score=0` + `l2_results=[]` +
  `containment_outcome="resolved"`, with `failure_tags` carrying
  `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP` (8/12) or final
  `stop_reason ∈ {loop_detected, goal_impossible, goal_achieved}`.

Three sampled traces (deliver-agent 2026-06-05):

- `bad_cases/a3/cs012_uc_fp_late_phone_failure_path` — 4 turns. Bot stamped
  `resolved` early. Then turns 5-7: user re-asks; bot replies *"I'm looking
  into this for you."* TWICE → `loop_detected`. failure_tags carry
  STALL:PLACEHOLDER_WITHOUT_FOLLOWUP. **case_passed=true** despite stall.
- `anchor_outcome/a3/anchor_outcome_uc_fp_removed` — 10 turns. Same shape:
  earlier stamped `resolved` → later "I'm looking into this for you." × 2 →
  `loop_detected`. Same STALL tag. **case_passed=true**.
- `anchor_outcome/a0/anchor_outcome_uc_b_posting` — 3 turns. Bot
  legitimately answered "Yes, you can give away free items on Gumtree." →
  user "Thank you, that helps." → `goal_achieved`. Only
  TIER2_ADVISORY:record-outcome-on-grounded-answer. Legitimate pass with
  housekeeping nit (NOT a stall).

2/3 sampled are real bot stalls being false-positived. cs095 stable
0.43→1.00 has 4/9 vacuous-pass draws; uc_a_visibility stable 0.00→1.00 has
1/9; uc_fp_removed stable 0.43→0.89 has ≥2/9 — these case-level flips'
*magnitude* is inflated by the gate gap. The flips' *direction* still
supports the simulator fix (clean input → bot can succeed), but the
authoritative M-Auto-5 baseline cannot be blessed on top of this gate
gap.

The S-Auto-21 simfixed run is **forensic evidence**, NOT an authoritative
baseline. `baseline_dir` is NOT moved. M-Auto-5 close is BLOCKED on this
sub-sprint.

## 4. Scope

### 4.0 Naming clarifications (read before Step 0 — there are two fields named with "2")

The four-tier scoring framework persists multiple fields per case in
`results.json`. Two of them carry "2" in their name and **are not the
same layer**:

- **`l2_results`** (plural list of `OutcomeCheckResult`) — the **L2
  outcome-checks layer**. This is the field OQ-S77.stall-not-gated
  targets. `l2_results=[]` means no outcome check defined/fired; the
  mandatory-L2 filter at `composite.py:182-196` becomes vacuously True.
  Fixes #2 and #3 operate on this field.
- **`tier2_result`** (singular dict, `Tier2Result`) — the
  **skill-procedure check**, a DIFFERENT, fourth tier. It emits tags
  like `TIER2_ADVISORY:record-outcome-on-grounded-answer` and only
  flips `case_passed` when `severity == "critical"` (at
  `composite.py:203`). Do NOT confuse `TIER2_ADVISORY:*` strings in
  `failure_tags` (from this skill-procedure layer) with `l2_results`
  (L2 outcome checks). They are separate signals; this sub-sprint
  does NOT modify `tier2_result` semantics.

The `case_passed` formula at `composite.py:205`:

```
case_passed = l1_passed AND mandatory_l2_passed AND not tier2_critical_failed
```

— so when `l2_results=[]`, `mandatory_l2_passed` is vacuously True;
`tier2_critical_failed` is False if `tier2_result.severity == "advisory"`;
and `l1_passed` may still be True (stalls are not L1 checks). That's
the gap OQ-S77 closes.

### Step 0 — verification (read-only)

Read `docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md` end-to-
end. Confirm the §3 layer is `infra` (eval-framework gate logic + runtime
trace-contract companion). Verify the 12-draw fingerprint by scanning
`eval_interactive/results/m-auto-5-baseline-20260604-simfixed/
_rebless_scratch/_attempts/a*/{bad_cases,anchor_outcome}/results.json`
yourself — confirm the distribution (cs012×2, cs095×4, uc_b×4, uc_a×1,
uc_fp_removed×2) and the fingerprint pattern (`case_passed=true AND
composite_score=0 AND l2_results=[]`). Write a brief one-paragraph
confirmation in your handoff §0 BEFORE editing any code.

### #1 — Eval gate: STALL signal promotion to `case_passed=false`

Insertion point: the case-pass computation in `eval_interactive/
eval_interactive/scoring/composite.py` around `:205` (where
`case_passed = l1_passed AND mandatory_l2_passed AND not
tier2_critical_failed` is computed) and the fall-through region near
`gating_l3 = [r for r in l3_results if ...]` at `:228` (the same area
that OQ-S76.judge-zero documented as falling through to
`judge_score=0.0` when the L3 list is empty). Read the module
top-to-bottom; identify where `case_passed` is finalized.

The framework already records the stall signal on multiple equivalent
surfaces. Pick ONE of the three below as the structural read (justify
the choice in handoff §1):

- **(1a) `stall_detected == True`** — preferred. Typed boolean on the
  case_result, produced by the stall detector at
  `eval_interactive/eval_interactive/scoring/stall_detector.py`.
  Reading the boolean directly avoids string-prefix matching and is
  the most stable surface.
- **(1b) `status == "FAIL"`** — the framework already sets a per-case
  status field that says FAIL on these draws even when
  `case_passed=True`. Promoting status=FAIL → case_passed=false
  closes the internal inconsistency; the read is structured.
- **(1c) `failure_tags` contains `STALL:*`** (prefix match) —
  the failure-tags catalogue read. Less preferred because it relies
  on string conventions; use only if (1a) / (1b) are not viable for
  some reason discovered during implementation.

All three are equivalent on the OQ-S77 vacuous-pass corpus (every
draw with `stall_detected=True` also carries `STALL:*` in
`failure_tags` and `status="FAIL"`); the choice optimises code
clarity, not semantic coverage. Promoted to `case_passed=false`
regardless of `composite_score`, `l2_results`, `judge_score`, or
`containment_outcome`. Set a `verdict_reason="stall_promoted"` field
(or analogous) so the trace records why.

**Scope discipline (HARD)**: selective, NOT blanket. Do NOT promote
`TIER2_ADVISORY:*` (those come from the skill-procedure tier; their
advisory severity is intentional). Do NOT promote any other
`failure_tags` family. Only the stall signal (whichever of 1a/1b/1c
you choose).

### #2 — Eval gate: terminal-failure stop_reason overrides earlier resolved stamp

In `eval_interactive/eval_interactive/scoring/hard_checks.py` (the same
module that holds `_VALID_TERMINAL_STOP_REASONS` per S-Auto-20 Fix #2):
when the FINAL `stop_reason` is in the enumerated terminal-failure set
`{loop_detected, goal_impossible, error, contract_violation,
max_turns_exceeded}`, the gate must NOT credit
`containment_outcome="resolved"` as a session-level success.

Two implementation options — pick one + justify in handoff:

- **(2a)** Override the gate's read of `containment_outcome` for these
  stop_reasons: treat as if blank, so `trace_minimum`'s existing
  blank-containment failure path fires.
- **(2b)** Add a separate failure check that fails `trace_minimum`
  whenever final `stop_reason` is in the set AND
  `containment_outcome="resolved"` (so the trace records both fields
  faithfully + the gate fails).

Anti-误杀: `goal_achieved` and `bot_ended` are NOT in the set. A
legitimately-completed session that stamped `resolved` still passes.

### #3 — Eval gate: refuse semantic-pass-by-default when `l2_results=[]` + `composite=0`

When `l2_results=[]` AND `composite_score=0` AND
`containment_outcome="resolved"`, the gate must NOT mark
`case_passed=true` on the strength of the stamp alone.

Pick one (or hybrid) — justify in handoff:

- **(3a) Structural fix via allowlist**: add a CaseSpec field
  `l2_not_applicable: true` (or analogous) + the gate reads it. Cases
  NOT on the allowlist arriving at this state → `case_passed=false`
  with `verdict_reason="no_l2_evidence_to_pass"`.
- **(3b) Inconclusive verdict**: `case_passed=null` (not true, not
  false) + `verdict_reason="no_l2_evidence"`. Requires downstream
  aggregation (`_rebless_report.json` stability + autoloop
  `tier_evaluator`) to handle `null`; verify before picking — if the
  downstream consumers can't handle null, this option is blocked.
- **(3c) CaseSpec L2 authoring**: edit the 5 affected CaseSpecs to add
  L2 outcome checks. This is `eval_spec` work AND it doesn't close
  the gap for future cases. NOT recommended as the sole fix.

Recommended: (3a). If (3b) is viable, even cleaner.

### #4 — Runtime companion: void/downgrade `containment_outcome="resolved"` stamp on session terminal failure

`server/.../runtime/ControlKernel.java` (or the BotSession persistence
path the S-Auto-20 stamp lives in). Add a session-level invariant:
when the loop terminates in a failure shape (`loop_detected` / `error`
/ `MAX_STEPS` / mid-resolution-without-completion) AFTER an earlier
turn stamped `containment_outcome="resolved"`, void or downgrade the
session-level field.

Pick one — justify in handoff:

- **(4a)** Overwrite to a more accurate value (e.g.
  `"incomplete_after_partial_answer"`) with a separate
  `partial_resolution_history` provenance field.
- **(4b)** Add a session-level `containment_outcome_overridden_by`
  provenance field (the prior stamp stays; the override is recorded
  separately).
- **(4c)** Leave per-turn stamps alone; expose a derived session-level
  `final_containment_outcome` field consumed by the eval gate from
  #2/#3.

Anti-误杀: NEVER void/downgrade on a legitimately-completed session
(`bot_ended` or `goal_achieved` with no subsequent loop / error). Java
unit tests required for both directions: (a) goal_achieved one-shot
still stamps `resolved` end-to-end (S-Auto-20 behavior preserved);
(b) early-resolved-then-loop ends with downgraded / overridden field.

### #5 — Anti-误杀 counter-tests (deterministic, on existing simfixed scratch)

Re-score every case in `eval_interactive/results/m-auto-5-baseline-
20260604-simfixed/_rebless_scratch/_attempts/a*/` under the corrected
eval gate (no LLM calls — this is a deterministic re-evaluation of
existing traces against the new scoring code). Expect:

- **12 vacuous-pass draws flip to FAIL**:
  - `bad_cases`: cs012 a3, cs095 a0/a2/a6/a8, cs012 a7 (loop+resolved
    pattern via #2 + STALL via #1).
  - `anchor_outcome`: uc_b a0/a1/a8 (goal_achieved+resolved+empty-L2
    via #3), uc_b a8 (same), uc_a a3 (goal_achieved+resolved+empty-L2
    via #3), uc_fp_removed a3/a8 (loop+resolved via #2 + STALL via #1).
- **9 legitimate F→P flips reassessed**:
  - `alice 0.43→0.89` — should hold (no vacuous in the 8/9 passes).
  - `cs012 majority True / 0.78` — softens (2 vacuous out of 7 passes
    flip → 5/9 PASS; new pass_rate ~0.56 reducible-flaky).
  - `cs015 majority True / 0.67` — should hold (no vacuous).
  - `cs066 1.00 stable` — should hold (no vacuous).
  - `cs095 majority True / 1.00 stable` — softens significantly
    (4 vacuous out of 9 passes flip → 5/9 PASS; new pass_rate ~0.56
    reducible-flaky or near-coinflip — surface, do NOT silently flip
    back).
  - `anchor uc_a_visibility 1.00 stable` — softens (1 vacuous; new
    8/9 ~0.89 — likely still PASS).
  - `anchor uc_f_billing 0.89 stable` — should hold (no vacuous in
    the set).
  - `anchor uc_fp_removed 0.89 stable` — softens (2 vacuous; new
    ~0.78 reducible-flaky).
  - `shadow cs01s01 0.89 stable` — should hold (shadow not in vacuous
    set).

**A genuine F→P flip silently lost = anti-误杀 regression → surface
and route before close.**

### #6 — Re-re-bless on the corrected framework (HUMAN-LAUNCHED — dev STOPS here)

Leave re-re-bless-ready in handoff §6 (analogous to S-Auto-21 handoff
§6):

- Backend must be rebuilt for #4 (`server/` was edited; the existing
  `45618df`/`1bc77c1` build is stale). Document the rebuild step.
- Output dir suggestion: `eval_interactive/results/m-auto-5-baseline-
  20260604-simfixed-stalledfix` (or current-dated variant).
- Mac caffeinate per `feedback_long_llm_run_no_sleep`.
- Clean tree per `project_autoloop_dirty_index_hazard`.

Do NOT launch the re-re-bless from this dev session. Do NOT move
`baseline_dir`. Do NOT flip `docs/current_eval_baseline.md` status.

### #7 — Add the §5.9 pre-flight check to the audit doc §6

Edit `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
§6 (the pre-flight checklist). Add as a new numbered step (probably
after step 4 or as a step 6):

> **Vacuous-pass + terminal-failure fingerprint sweep** — across the
> target run's per-attempt `results.json`, count draws where
> `case_passed=true AND composite_score=0 AND l2_results=[]`. For
> every match, halt if any of: `failure_tags` contains `STALL:*`, OR
> final `stop_reason` is in `{loop_detected, goal_impossible, error,
> contract_violation, max_turns_exceeded}`. The check is cheap (a
> single Python scan over the JSON output dir; reference
> implementation in the OQ-S77.stall-not-gated brief, expected zero
> matches on a properly-gated corpus).

Cite `oq-s77-stall-not-gated.md` brief for the rationale.

## 5. Anti-误杀 invariants (HARD)

1. Preserve all 9 legitimate F→P flips from S-Auto-21 (modulo expected
   pass_rate softening on cs095 / uc_a / uc_fp_removed where vacuous
   draws are removed). A flip silently lost is a regression.
2. Stamp downgrade (#4) is scoped to terminal-failure shapes; legitimate
   completion never voids.
3. STALL:* promotion (#1) is selective; TIER2_ADVISORY:* stays advisory.
4. NO CaseSpec-specific allowlist for cs012/cs095/uc_b/uc_a/uc_fp_removed.
5. NO blanket failure_tags-present-means-fail rule.

## 6. Hard fences / STOP conditions

- Edit ONLY: `eval_interactive/eval_interactive/scoring/composite.py`
  + `eval_interactive/eval_interactive/scoring/hard_checks.py` (+
  `eval_interactive/tests/test_*.py`); `server/.../runtime/ControlKernel.java`
  (+ companion files for #4 + Java tests); `docs/diagnostics/
  2026-06-04-eval-framework-and-simulator-audit.md` (§6 pre-flight
  check addition).
- DO NOT touch the S-Auto-21 simfixed forensic dir.
- DO NOT touch CaseSpec YAML files (the #3 option may add a field
  declaration on a small number of cases ONLY if option 3a is picked
  AND the allowlist mechanism requires per-case opt-in — but the
  default for affected cases is gate-as-fail, not allowlist-as-pass).
- DO NOT move `baseline_dir`.
- DO NOT launch the re-re-bless yourself.
- DO NOT touch autoloop 5-file scoring SHA-locked set.
- DO NOT open S-Auto-18 / M-Auto-4 / M-Auto-6 work.
- All eval / rerun only on a clean committed tree.

## 7. Test / eval requirements

- `eval_interactive` pytest under `uv run pytest`: no regression vs
  current `538 passed` (S-Auto-21 baseline). New tests for #1/#2/#3
  + the anti-误杀 #5 set increment.
- Java suite target `1232 / 1 / 0 / 2`; new tests for #4 increment.
- autoloop pytest: untouched. Confirm `324` unchanged.
- Deterministic re-score on the existing simfixed
  `_rebless_scratch/_attempts/a*/`: 12 vacuous-pass draws → FAIL; 9
  legitimate F→P flips reassessed per §4 #5 expectations.
- §5.9 pre-flight check (added in #7) returns ZERO matches on the
  re-re-blessed corpus (HUMAN runs this AFTER #6 completes; you
  capture the expected-zero outcome as the validation gate in your
  handoff §8).

## 8. §7 Layer-classification + anti-hardcode stanza (embedded)

**Target failure layer:** `infra` — eval-framework scoring gate
(`composite.py` + `hard_checks.py`) + runtime trace-contract companion
(`ControlKernel.java` + BotSession persistence). NO `semantic_planner`
/ `prompt_projection` / `skill_state` / `eval_spec` / `judge_calibration`
/ `product_policy` edit.

**Tier-0 invariant:** adds none; preserves all Tier-0 families at
current strictness.

**Semantic hardcode:** none. STALL:* promotion keys on a structural tag
family the framework already emits; terminal-failure stop_reason
override keys on an enumerated set; empty-L2 refusal keys on data-shape;
runtime stamp downgrade keys on session terminal state.

**Generalization coverage:** measurement-infra sub-sprint — evidence
is (a) deterministic re-score on the simfixed scratch, (b) Java
counter-tests for #4, (c) eval contract-test for the §5.9 pre-flight
check on a synthetic vacuous-pass fixture, (d) HUMAN-launched re-re-
bless real-LLM evidence. NOT target/neighbor/negative/shadow case-
family counts.

## 9. Codex review plan (§4.3, embedded)

PER-SUB-SPRINT RECOMMENDED; folds into M-Auto-5 milestone-shared close.
NOT a fence-#13 SHA trigger.

Codex focus (priority):

1. No semantic-side / no bot edit / no per-case allowlist.
2. STALL:* promotion selective; TIER2_ADVISORY:* not promoted.
3. Terminal-failure stop_reason override is enumerated explicit set.
4. Empty-L2 refusal is structural (option 3a/3b mechanism is generic),
   NOT a per-case allowlist for the affected cases.
5. Runtime stamp downgrade preserves legitimate resolve.
6. Anti-误杀 #5 evidence shows the 9 legitimate F→P flips reassessed
   honestly.
7. §5.7 evidence gate: HUMAN re-re-bless is real-LLM evidence; the
   deterministic re-score is wiring evidence only.
8. §5.9 pre-flight check added; check returns zero on re-re-blessed
   corpus.

## 10. Handoff requirements (write `docs/sprints/sprint-077-handoff.md`)

MUST include:

- §0 cold-start summary + Step-0 verification paragraph.
- §1 #1 STALL:* promotion diff + insertion-point justification.
- §2 #2 terminal-failure override diff + enumerated set + option
  (2a/2b) chosen + justification.
- §3 #3 empty-L2 refusal diff + option (3a/3b/3c) chosen + allowlist
  mechanism details if applicable.
- §4 #4 runtime stamp downgrade diff + option (4a/4b/4c) chosen + Java
  unit-test evidence.
- §5 anti-误杀 deterministic re-score table (12 vacuous→FAIL + 9
  legitimate F→P flips reassessed; flag any silent loss).
- §6 re-re-bless-ready command + preconditions (backend rebuild for
  #4; Mac caffeinate; clean tree).
- §7 STOP confirmations (`baseline_dir` NOT moved; S-Auto-21 simfixed
  dir retained as forensic; full re-re-bless NOT run by dev; S-Auto-17
  overnight / S-Auto-18 / M-Auto-6 NOT touched).
- §8 §5.9 pre-flight check addition to audit doc §6 (text + insertion
  location).
- §9 OQ-S77.stall-not-gated brief reference confirmation; all five
  brief routing items addressed.

## 11. Code anchors (on demand)

- `eval_interactive/eval_interactive/scoring/composite.py` — case_passed
  computation; advisory-strip at `:228`; #1 + #3 land here.
- `eval_interactive/eval_interactive/scoring/hard_checks.py` —
  `_VALID_TERMINAL_STOP_REASONS` (S-Auto-20 removed `loop_detected`);
  `HardChecker.trace_minimum` + the read of `containment_outcome`;
  #2 lands here.
- `server/.../runtime/ControlKernel.java` —
  `isResolvedSuccessTerminal` (S-Auto-20 broadened to accept
  ANSWERED_SUBTASK + grounding); #4 stamp-downgrade insertion point.
- `server/.../session/BotSession.java` — containment_outcome
  persistence path.
- `eval_interactive/results/m-auto-5-baseline-20260604-simfixed/
  _rebless_scratch/_attempts/a*/{bad_cases,anchor_outcome}/results.json` —
  the 12 vacuous-pass draws to re-score in #5.
- `docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md` — the
  framework-defect brief (Step 0 read).
- `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
  §6 — pre-flight checklist insertion target (#7).

## 12. Commit discipline (embedded)

- `git add` only authorized files (no `-A`):
  - `eval_interactive/eval_interactive/scoring/composite.py`
  - `eval_interactive/eval_interactive/scoring/hard_checks.py`
  - `eval_interactive/tests/...` (new + updated)
  - `server/.../runtime/ControlKernel.java` (+ companion files for #4)
  - `server/.../test/...` (Java unit tests for #4)
  - `docs/sprints/sprint-077-handoff.md`
  - `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
- One commit per substantive step where practical.
- The re-re-bless output dir is gitignored.
- Deliver-agent commits its own bundle separately.

## 13. Self-check checklist

- [ ] Step 0 verification paragraph in handoff §0.
- [ ] #1 STALL signal promotion landed; one of (1a) `stall_detected`,
      (1b) `status=="FAIL"`, or (1c) `failure_tags STALL:*` chosen +
      justified in handoff §1; NOT a blanket failure_tags promotion;
      `TIER2_ADVISORY:*` NOT promoted; `tier2_result` semantics
      unchanged.
- [ ] #2 enumerated terminal-failure stop_reasons (loop_detected,
      goal_impossible, error, contract_violation, max_turns_exceeded);
      goal_achieved + bot_ended NOT in set.
- [ ] #3 empty-L2 refusal landed; option chosen + justified; no
      per-case allowlist for affected cases.
- [ ] #4 runtime stamp downgrade scoped to terminal-failure shapes;
      bot_ended/goal_achieved no-loop sessions still stamp resolved.
- [ ] #5 deterministic re-score: 12 vacuous → FAIL; 9 legitimate
      flips reassessed (table in handoff §5); no silent flip-back.
- [ ] eval pytest no regression vs 538; new tests increment.
- [ ] Java 1232/1/0/2 + new test count delta.
- [ ] autoloop pytest 324 unchanged.
- [ ] #7 §5.9 pre-flight check added to audit doc §6.
- [ ] `baseline_dir` NOT moved; S-Auto-21 simfixed dir untouched; full
      re-re-bless NOT run.
- [ ] Handoff written per §10.
