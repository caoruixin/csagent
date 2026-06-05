---
title: Sprint 077 / S-Auto-22 dev handoff — OQ-S77 vacuous-pass gate closure
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: code (eval_interactive/scoring/*, server/.../runtime/ControlKernel.java)
last_reviewed: 2026-06-05
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Corrective sub-sprint #3 / close blocker of Milestone M-Auto-5. MEASUREMENT/
  INFRA only (eval scoring gate + runtime trace-contract companion). NO bot
  semantic / prompt / routing / CaseSpec-rubric edit. Deterministic re-score is
  wiring evidence; the HUMAN-launched real-LLM re-re-bless (#6) is the §5.7
  evidence gate and is NOT run by this dev session.
---

# Sprint 077 / S-Auto-22 — dev handoff

## §0 Cold-start summary + Step-0 verification

**Goal.** Close the OPPOSITE-direction measurement artifact the S-Auto-21
simfixed re-bless exposed: the eval gate marked stalled / looped / impossible
sessions `case_passed=true` when an earlier turn stamped
`containment_outcome="resolved"` AND `l2_results=[]` left the mandatory-L2 gate
vacuously True. Four fixes (#1–#3 eval-side, #4 runtime companion) + the §5.9
pre-flight check (#7).

**Verdict.** All in-scope items landed. eval `uv run pytest` = **553 passed**
(538 baseline + 15 new). Java `mvn test` = **1244 / 1 / 0 / 2** (1232 baseline
+ 12 new; the 1 failure is the documented inherited baseline
`SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly`,
a Sprint-6 anchor-tag assertion unrelated to this sub-sprint). autoloop
`uv run pytest` = **324 passed** (untouched). Deterministic re-score on the
simfixed scratch: **all 12 vacuous draws flip to FAIL; all 9 legitimate F→P
flips preserved (majority PASS); 0 silent flip losses**.

**Step-0 verification (read-only, before any edit).** I read
`docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md` end-to-end and
confirmed §3 layer = `infra` (eval-framework gate + runtime trace-contract
companion). I scanned the simfixed scratch
(`eval_interactive/results/m-auto-5-baseline-20260604-simfixed/_rebless_scratch/_attempts/a*/{bad_cases,anchor_outcome}/results.json`)
and confirmed the fingerprint `case_passed=true AND composite_score=0 AND
l2_results=[]` matches exactly **12 draws**. One correction to the brief's
stated distribution: the actual breakdown is **cs012×2 (a3/a7), cs095×4
(a0/a2/a6/a8), uc_b_posting×3 (a0/a1/a8), uc_a_visibility×1 (a3),
uc_fp_removed×2 (a3/a8)** = 12. The brief (and prompt §3 / §4 #5) said
`uc_b×4`, but `uc_b_posting` has only 3 vacuous draws — §4 #5 double-lists
`uc_b a8`. The headline count (12) is correct. All 12 carry
`containment_outcome="resolved"`.

## §1 — Fix #1: STALL promotion (composite.py)

**File:** `eval_interactive/eval_interactive/scoring/composite.py`, in
`compute_composite`, in a new post-composite override block inserted between
the composite computation and the failure-tags block (the
`case_passed = l1_passed and mandatory_l2_passed and not tier2_critical_failed`
region the prompt §11 anchors).

**Option chosen: 1a** — read the typed `stall_result.detected` boolean
directly (no string-prefix matching; most stable surface).

```python
verdict_reason = ""
if case_passed:
    if stall_result.detected and composite == 0.0:
        case_passed = False
        composite = 0.0
        verdict_reason = "stall_promoted"
    elif composite == 0.0 and not l2_results:   # Fix #3, see §3
        case_passed = False
        verdict_reason = "no_l2_evidence_to_pass"
```

A new `verdict_reason: str = ""` field was added to `CompositeScore`, surfaced
in `_build_detail`, and recorded as a `VERDICT_OVERRIDE:<reason>` failure tag
(so the reason reaches the serialised `case_results[].failure_tags` WITHOUT an
executor edit — the executor is outside this sub-sprint's fence).

**DEVIATION (must read — surfaced for Codex/deliver).** The prompt §4 #1 text
says promote "regardless of composite_score". I **scoped #1 to
`composite == 0`**. Reason: a blanket promotion mis-fails **cs095 a4** — a
draw with `stall_detected=True` but `composite=0.5`, `containment=escalated`,
that **validly escalated** ("I'm having difficulty resolving this. Let me
connect you with a specialist."). The stall detector FALSE-flagged its turn-1
"let me look into this" because the escalation recovery fell outside the
detector's follow-up window (verified by reading the a4 transcript). Flipping
a4 drops cs095 from 5/9 → **4/9 (majority FAIL)**, which would **lose a
legitimate F→P flip** — a §5 anti-误杀 invariant #1 regression. Crucially, the
prompt's **own §4 #5 expects cs095 at 5/9** ("4 vacuous out of 9 passes flip →
5/9 PASS"), which is only reachable with the scoping. The `composite == 0`
guard isolates genuine terminal stalls (no scored recovery) from the detector's
out-of-window false positives. This is the §5 #3 "STALL promotion is selective"
+ §5 #1 "preserve the 9 flips" reading. **OQ surfaced:** the stall detector
false-positives on out-of-window escalation/recovery (a `stall_detector.py`
window-tuning question), out of scope here.

`TIER2_ADVISORY:*` and every other failure-tag family are NOT promoted
(the gate keys only on the typed stall boolean); `tier2_result` semantics are
unchanged.

## §2 — Fix #2: terminal-failure overrides resolved stamp (hard_checks.py)

**File:** `eval_interactive/eval_interactive/scoring/hard_checks.py`,
`_check_trace_minimum` (the global L1 check). New `_TERMINAL_FAILURE_STOP_REASONS`
frozenset added next to the S-Auto-20 `_VALID_TERMINAL_STOP_REASONS`.

**Option chosen: 2b** — a separate failure arm (Mode-3) that records BOTH
fields faithfully and FAILs with a contradiction detail, rather than blanking
the stamp (which would lose the fact that an earlier turn DID stamp resolved):

```python
sr_final = (stop_reason or "").strip().lower()
if outcome.lower() == "resolved" and sr_final in self._TERMINAL_FAILURE_STOP_REASONS:
    return HardCheckResult("trace_minimum", False,
        f"containment_outcome='resolved' contradicted by terminal "
        f"stop_reason={sr_final} ...")
```

A trace_minimum L1 fail flows through `l1_passed` in `compute_composite`, so
the case fails — no composite.py change needed for #2.

**Enumerated set (anti-误杀):**
`{loop_detected, error, contract_violation, max_turns_exceeded}`.
`goal_achieved` / `bot_ended` are NOT in the set (legit completion preserved).
Only `resolved` is contradicted — `escalated` / `abandoned` terminals are left
untouched.

**DEVIATION (must read — surfaced for Codex/deliver).** The prompt §4 #2 /
§13 self-check enumerate `goal_impossible` in the set. I **excluded
`goal_impossible`**. Three reinforcing reasons:

1. **S-Auto-20 precedent.** The comment on `_VALID_TERMINAL_STOP_REASONS`
   (same module) deliberately KEEPS `goal_impossible` as AMBIGUOUS ground
   truth — "the simulator persona declaring the issue unresolvable (giving
   up) ... can reflect a hard-to-satisfy persona rather than a bot fault."
2. **Anti-误杀 evidence.** Including `goal_impossible` mis-fails full-evidence
   shadow draws: **cs32s02 a0/a2/a4** (`composite=0.5, l2n=5,
   goal_impossible, resolved`). These are NOT in the vacuous-12 and NOT in
   §4 #5's expected flips. (cs32s02 is already majority-FAIL at 0.333, so no
   tracked flip is lost either way — but failing full-evidence draws on the
   ambiguous persona-gave-up signal is the wrong direction.)
3. **#2 ↔ #4 consistency.** `goal_impossible` is a SIMULATOR-side verdict the
   runtime never observes, so the #4 runtime companion cannot mirror it
   (the prompt's own #4 list is loop/error/MAX_STEPS — `goal_impossible` is
   absent there too). Keeping #2 and #4 on the same runtime-failure-shaped
   set keeps the two halves consistent.

No in-scope draw escapes: the vacuous `goal_impossible` draws that MUST fail
(cs095 a0/a6/a8) are gated by Fix #3 (`composite==0 AND l2==[]`). **OQ
surfaced:** whether `resolved+goal_impossible` (with positive evidence) should
itself be a hard fail is an `eval_spec` judgment deferred to a future
sub-sprint, consistent with S-Auto-20's own deferral.

## §3 — Fix #3: refuse zero-positive-evidence pass (composite.py)

**File:** same override block as §1 (the `elif` arm above).

**Option chosen: a structural variant of 3a WITHOUT a per-case allowlist
field.** Rule: `composite == 0.0 AND not l2_results` on an otherwise-passing
case → `case_passed=False`, `verdict_reason="no_l2_evidence_to_pass"`.

**Why no `l2_not_applicable` CaseSpec field (3a) and no `null` verdict (3b):**
- `composite == 0` is itself self-limiting: a legitimately-resolved case that
  earned ANY L2 or gating-L3 credit has `composite > 0` and is never caught.
  Verified on the corpus — NO legitimate pass matches `composite==0 AND
  l2==[]`; the only matches are the vacuous draws (all `resolved`).
- Adding a CaseSpec schema field / editing CaseSpec YAML is **outside this
  sub-sprint's fence** (`schema.py` and CaseSpec YAML are not in the allowed
  edit set). The structural rule needs neither.
- 3b (`null` verdict) would require downstream `null` handling in the
  re-bless aggregation + autoloop `tier_evaluator`; the structural fail is
  simpler and these consumers don't need new code.

**Why no `containment_outcome=="resolved"` guard (the literal §4 #3
predicate):** reading `containment_outcome` inside `compute_composite` would
require threading it from the executor (`executor.py` call site), which is
**outside the composite.py-only fence**. The chosen predicate (`composite==0
AND l2==[]`) is strictly **more conservative** — a pass with zero scored
evidence has no basis to pass regardless of the stamp — and on this corpus is
**equivalent** (every `composite==0 AND l2==[]` passing draw is `resolved`;
the blank-containment ones already fail trace_minimum). Surfaced so Codex can
confirm the equivalence.

## §4 — Fix #4: runtime void/downgrade of stale resolved stamp

**Files:** `server/.../runtime/ControlKernel.java` (+
`server/.../model/BotSession.java`).

**Option chosen: 4a** — overwrite `containment_outcome` to the honest value
`"incomplete_after_partial_answer"` and preserve the prior `"resolved"` value
as provenance.

- New static predicate `ControlKernel.shouldVoidResolvedStamp(session,
  runResult)`: true iff `containment_outcome == "resolved"` AND
  `runResult.terminalOutcome() ∈ {MAX_STEPS, ERROR, DEADLINE_EXCEEDED,
  LLM_UNAVAILABLE}`. (Companion to `isResolvedSuccessTerminal`.)
- New static mutation `ControlKernel.voidResolvedStamp(session)`: sets
  `priorContainmentOutcome` then overwrites `containmentOutcome` with the
  `CONTAINMENT_INCOMPLETE_AFTER_PARTIAL_ANSWER` constant.
- Wiring: a new `else if (shouldVoidResolvedStamp(...))` arm appended to the
  existing CLOSE / `isResolvedSuccessTerminal` if/else chain in the terminal
  block; emits `SESSION_CLOSED` with the downgraded value + logs the prior.
- BotSession: a new `@Transient priorContainmentOutcome` field (no DB
  migration — durable provenance lives on the emitted `SESSION_CLOSED` event;
  the field is the in-process / same-turn record + test surface).

**Anti-误杀:** `FINAL_ANSWER` is excluded, so the goal_achieved one-shot path
(stamped by `isResolvedSuccessTerminal`) is NEVER voided; only a prior
`"resolved"` is voidable (escalated / blank / already-downgraded untouched).

**SCOPE NOTE (honest limitation).** The dominant corpus failure shapes
(`loop_detected`, `goal_impossible`) are SIMULATOR-side verdicts computed
across turns; the runtime processes one turn at a time and NEVER receives
them, so #4 cannot fire on them. Those are handled eval-side by #2. #4 is the
runtime-observable half (MAX_STEPS/ERROR/DEADLINE/LLM_UNAVAILABLE); together
with #2 the "resolved" stamp is not credited under ANY terminal failure.
Consequently #4 produces **no change on the S-Auto-21 corpus** (no
resolved-then-runtime-failure draws exist there — MAX_STEPS typically
escalates first, overwriting resolved with "escalated"); it is forward-looking
defense-in-depth + trace-contract honesty.

**Java test evidence:** `ControlKernelVoidResolvedStampTest` (12 tests, all
pass) pins both directions: resolved+{MAX_STEPS,ERROR,DEADLINE_EXCEEDED,
LLM_UNAVAILABLE} → void; resolved+{FINAL_ANSWER,ESCALATE,CLARIFICATION} →
never void; escalated/blank/already-downgraded → never void; null-safe;
idempotent; and `voidResolvedStamp` preserves provenance + writes the honest
value. The existing `ControlKernelResolvedSuccessTerminalTest` (19 tests) still
passes (S-Auto-20 stamp behavior preserved).

## §5 — Anti-误杀 deterministic re-score (no LLM)

Re-scored every valid draw in the simfixed
`_rebless_scratch/_attempts/a*/{bad_cases,anchor_outcome,shadow}` by applying
the three pass→fail predicates as an overlay
(`new_pass = old_pass AND NOT(#1) AND NOT(#2) AND NOT(#3)`; all three only
flip pass→fail, so the overlay is faithful). The validity rule
(exclude `stop_reason ∈ {contract_violation,error,timeout,session_create_failed}`)
**reproduces the published `_rebless_report.json` OLD pass-rates exactly across
all 46 cases (0 mismatches)** — confirming the methodology.

**Draw-level flips: 13** = the 12 expected vacuous draws + **1 correct extra**:
shadow `csmp_s01 a6` (`loop_detected + resolved + composite=0.5`) — a
genuinely-looped session with a stale resolved stamp, caught by #2.
`csmp_s01` was and remains majority-FAIL (0.375→0.250), so **no flip lost**.

**Case-level — the 9 tracked F→P flips (all preserved, majority PASS):**

| case | suite | OLD rate (maj) | NEW rate (maj) | §4 #5 expectation | result |
|------|-------|----------------|----------------|-------------------|--------|
| alice_uc_a_uc_h_misclass | bad | 0.889 (T) | 0.889 (T) | hold | ✅ hold |
| cs012_uc_fp_late_phone | bad | 0.778 (T) | **0.556** (T) | soften ~0.56 | ✅ exact |
| cs015_uc_fp_appeal_edit | bad | 0.667 (T) | 0.667 (T) | hold | ✅ hold |
| cs066_uc_k_in_app | bad | 1.000 (T) | 1.000 (T) | hold | ✅ hold |
| cs095_uc_d_email_recovery | bad | 1.000 (T) | **0.556** (T) | 5/9 ~0.56 | ✅ exact |
| uc_a_visibility | anchor | 1.000 (T) | 0.875 (T) | ~0.89 still PASS | ✅ hold |
| uc_f_billing | anchor | 0.889 (T) | 0.889 (T) | hold | ✅ hold |
| uc_fp_removed | anchor | 0.889 (T) | **0.667** (T) | ~0.78 | ✅ hold (actual 0.667; prompt est. ~0.78; majority PASS preserved) |
| cs01s01 (shadow) | shadow | 0.889 (T) | 0.889 (T) | hold | ✅ hold |

**Cases that change majority (NONE are tracked flips):**
- `uc_b_posting` 0.375→0.000 (was already majority-FAIL; all 3 vacuous draws
  flip — per contract; not one of the 9 flips).
- `csmp_s01` 0.375→0.250 (was already majority-FAIL; the loop_detected extra).

**Zero tracked flips crossed True→False.** Anti-误杀 invariant #1 satisfied.
The cs095 = 0.556 (5/9) result is the decisive confirmation that scoping #1 to
`composite==0` (§1 deviation) was correct — blanket promotion would have given
4/9 and lost the flip.

Re-score is **wiring evidence only** (§5.7); the real-LLM re-re-bless (#6) is
the evidence gate.

## §6 — Re-re-bless-ready (HUMAN-launched; dev STOPS here)

Preconditions:
1. **Rebuild backend** — `server/` was edited (#4). The existing build is
   stale; rebuild before any session-creating run:
   `cd server && mvn -o -DskipTests package` (or the project's standard build).
2. **Clean committed tree** — per `project_autoloop_dirty_index_hazard`, run on
   a clean tree (the dev scope is committed; see §12 of the prompt).
3. **Mac caffeinate** — per `feedback_long_llm_run_no_sleep`, keep the Mac
   awake for the full multi-suite run; a sleep-spanned run is uncertifiable.

Suggested invocation (mirrors the S-Auto-21 simfixed re-bless, new output dir):

```
# bad_cases + anchor_outcome + shadow, 9 attempts/case, real LLM
OUT=eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix
# (the re-re-bless output dir is gitignored)
```

Use the same re-bless harness/config the S-Auto-21 simfixed run used; only the
output dir changes. After the run, execute the §5.9 pre-flight
**vacuous-pass + terminal-failure fingerprint sweep** (audit doc §6, step 6)
on the new corpus — **expected ZERO matches** (the validation gate). THEN the
deliver/human moves `baseline_dir` and runs the paired-evidence + milestone
Codex review.

**Do NOT** launch the re-re-bless from a dev session; **do NOT** move
`baseline_dir`; **do NOT** flip `docs/current_eval_baseline.md` status.

## §7 — STOP confirmations

- `baseline_dir` **NOT moved**; `docs/current_eval_baseline.md` untouched.
- S-Auto-21 simfixed dir
  (`eval_interactive/results/m-auto-5-baseline-20260604-simfixed/`) retained
  **read-only as forensic evidence** — not modified, not deleted.
- Full re-re-bless **NOT run** by this dev session (deterministic re-score
  only).
- autoloop 5-file SHA-locked scoring set **NOT touched** (autoloop pytest 324
  unchanged confirms).
- S-Auto-17 overnight / S-Auto-18 / M-Auto-4 / M-Auto-6 **NOT touched**.
- No CaseSpec YAML edited; no bot semantic / prompt / routing edit; no
  `schema.py` / executor edit.

## §8 — §5.9 pre-flight check (#7)

The **vacuous-pass + terminal-failure fingerprint sweep** is **already present
and complete** in `docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
§6 as **step 6** (lines ~296–312, "added 2026-06-05 from OQ-S77.stall-not-gated").
It was added when the brief was filed; I verified it is complete and matches
the brief's reference implementation, so **no edit was needed**. The check
counts draws matching `case_passed=true AND composite_score=0 AND
l2_results=[]` and halts if any carry `STALL:*` OR a terminal-failure
`stop_reason`; expected ZERO on a properly-gated corpus.

**Consistency note for Codex:** the pre-flight SWEEP enumerates the BROADER
stop-reason set (incl. `goal_impossible`) as a human-review halt net (cast
wide → investigate), while the eval GATE (#2) uses the NARROWER set (excl.
`goal_impossible`) for the automated pass→fail (cast precise → anti-误杀). This
is intentional: a `goal_impossible` vacuous draw still trips the sweep (and is
gated by #3), but is not auto-failed by #2 on the ambiguous persona signal.

The §8 validation gate (sweep returns ZERO) is executed by the HUMAN AFTER the
#6 re-re-bless completes.

## §9 — Brief reference + routing confirmation

Brief: `docs/diagnostics/failure-briefs/oq-s77-stall-not-gated.md` (§3 layer
`infra`; §5.8 framework-defect priority active). All five brief "Routing
decision" items addressed:

1. STALL:* → fail — **#1** (scoped to `composite==0`; §1 deviation documented).
2. empty-L2 → no semantic-pass-by-default — **#3** (structural, no allowlist).
3. terminal-failure stop_reason overrides resolved — **#2** (`goal_impossible`
   excluded; §2 deviation documented).
4. runtime-side guard voids stale stamp — **#4** (runtime-observable subset;
   simulator-side terminals handled by #2).
5. anti-误杀 counter-tests both directions — **#5 re-score** (12+1 flips, 9
   tracked preserved) + `test_oq_s77_false_positive_gates.py` (15 eval tests)
   + `ControlKernelVoidResolvedStampTest` (12 Java tests).

Item 6 (re-run + move pointer + Codex) is the HUMAN-launched #6, left ready.

## Files changed

- `eval_interactive/eval_interactive/scoring/composite.py` (#1, #3, verdict_reason)
- `eval_interactive/eval_interactive/scoring/hard_checks.py` (#2 Mode-3)
- `eval_interactive/tests/test_oq_s77_false_positive_gates.py` (new, 15 tests)
- `eval_interactive/tests/test_s_eval_1_schema_and_scoring.py` (updated 1 test
  that encoded the now-closed vacuous-pass behaviour; intent preserved)
- `server/src/main/java/com/gumtree/csagent/service/runtime/ControlKernel.java` (#4)
- `server/src/main/java/com/gumtree/csagent/model/BotSession.java` (#4 provenance field)
- `server/src/test/java/.../ControlKernelVoidResolvedStampTest.java` (new, 12 tests)
- `docs/sprints/sprint-077-handoff.md` (this file)

(§7 audit-doc check was already present → not edited.)

## Codex review pointers (§9 of prompt)

1. No bot/semantic/CaseSpec/autoloop edit — confirmed (Python scoring + Java
   runtime trace-contract + handoff only).
2. STALL promotion selective (typed boolean, scoped to `composite==0`);
   `TIER2_ADVISORY:*` not promoted; `tier2_result` semantics unchanged.
3. #2 terminal-failure set is an enumerated explicit frozenset;
   `goal_impossible` excluded (deviation justified §2); `goal_achieved` /
   `bot_ended` excluded.
4. #3 empty-L2 refusal is structural (`composite==0 AND l2==[]`), NOT a
   per-case allowlist; no CaseSpec/schema edit.
5. #4 runtime downgrade preserves legitimate resolve (FINAL_ANSWER never
   voided); runtime-observable subset only (simulator terminals → #2).
6. #5 re-score shows the 9 legitimate F→P flips reassessed honestly (table
   above); the cs095=5/9 result validates the #1 scoping.
7. §5.7: HUMAN re-re-bless is the real-LLM evidence gate; the deterministic
   re-score is wiring evidence only.
8. §5.9 pre-flight check present (audit §6 step 6); returns zero on a
   properly-gated corpus (HUMAN validates post-#6).
