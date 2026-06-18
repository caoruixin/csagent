---
title: "Sprint 093 / S-Auto-39 — RESOLVE→CONFIRM/CLOSE phase-transition corrective (OQ-S86b.3)"
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-18
review_cadence: per sprint
supersedes: docs/sprints/sprint-092-objective.md
superseded_by: null
notes: >
  INSERTED pre-pilot runtime corrective (M-Auto-7). Promotes the human-approved
  plan (handoff §1 2026-06-18 acceptance review + OQ-S86b.3 forensic). Root
  cause: a RESOLVE→CONFIRM/CLOSE phase-transition deadlock — record_outcome(resolve)
  is correctly rejected outside CONFIRM/CLOSE
  (ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome), but
  evaluate() only returns READY_TO_CONFIRM when a record_outcome already succeeded
  this run, so a completed grounded resolve answer can stall in RESOLVE → blank/
  stale containment, turn-budget exhaustion, or mis-attributed escalation. The CS4
  pilot (S-Y2 / Sprint 088) stays HELD (snapshot docs/sprints/sprint-088-objective.md);
  it does NOT resume until this corrective closes + passes Codex. Objective-alignment
  annotation is NOT implemented until then. Full pilot tranche HELD; exp-82
  WITHDRAWN; exp-86 OFF_TARGET (not mergeable / not seed). M-Auto-8 (primary-first
  staged eval) is a separate future milestone, not in scope.
---

# Sprint 093 / S-Auto-39 — RESOLVE→CONFIRM/CLOSE phase-transition corrective

## Class

| Field | Value |
|---|---|
| **Layer (§3.2)** | **`skill_state`** — a multi-turn RESOLVE flow cannot durably advance to CONFIRM/CLOSE for a *completed grounded resolve answer*. Escalates to **`human_review_required`** only under the §Frozen-surface STOP triggers. |
| **§7 stanza** | **REQUIRED** (runtime semantic-adjacent surface) — see §7. |
| **Per-sub-sprint Codex (§4.3 / §4.1)** | **REQUIRED** — nine-question kernel on the runtime diff, reviewed against the Step-0 attribution, the bounded-run evidence, and the zero-LLM cross-check. A binding gate before any pilot-resume discussion. |

## Goal

Give a *completed grounded resolve answer* a legitimate, reliable path
**RESOLVE → CONFIRM/CLOSE** so `record_outcome(resolve)` can land (or a
product-contract-allowed resolved terminal lands correctly) within the persona's
turn window — breaking the deadlock **without relaxing the premature-resolve
guard**.

### What this sprint does NOT do (binding)

- Does **not** relax/weaken `ResolveDispositionEvaluator.shouldRejectPrematureResolveOutcome`
  (a genuinely premature `record_outcome(resolve)` — in RESOLVE before any
  subsequent user turn — must still be rejected).
- Does **not** lower the `record_outcome` product requirement.
- Does **not** add any PRIMARY / UC / case-specific exception.
- Does **not** edit any CaseSpec or any evaluator pass-criterion.
- Does **not** mask the deadlock by raising `max_turns` or by faking a resolved stamp.
- Does **not** add any user-message keyword / content heuristic.
- Does **not** require PRIMARY majority-pass and does **not** claim CS4 success.
- Does **not** resume the M-Auto-7 real-LLM pilot or implement the
  objective-alignment annotation.

## 1. Fix-direction priority (binding order)

1. **PRIMARY — repair the formal `RESOLVE → CONFIRM/CLOSE` phase transition.**
   When (a) a grounded FINAL_ANSWER has formed AND (b) a subsequent user turn has
   occurred, the runtime must be able to enter a confirmable/recordable state
   (CONFIRM/CLOSE) so `record_outcome(resolve)` is permitted and lands. This is
   the required first approach.
2. **SECONDARY (only if PRIMARY is proven not minimally fixable)** — evaluate a
   bounded extension of `isResolvedSuccessTerminal`. This requires an explicit
   in-handoff justification that the formal phase transition could not be
   minimally repaired.

**The terminal stamp must NOT be used as a shortcut to bypass the phase machine,
and no further local resolved-stamp patches may be stacked.** The disposition's
`READY_TO_CONFIRM`-requires-prior-successful-record circularity is the thing to
break — at the phase-transition layer first.

## 2. Scope (ordered steps)

- **Step 0 — Pre-dev escalation attribution (read-only, no LLM).** Across the
  exp-86 + `m-auto-7-prepilot-baseline-20260608` PRIMARY traces, per draw classify
  every `faq_miss_threshold_exceeded` / `turn_budget_exhausted` / `user_requested`
  escalation as **deadlock-downstream runtime mis-stamp** (loop exhaustion after
  repeated record rejection — cf. the known FAQ-over-escalate/MAX_STEPS mis-stamp
  pattern) vs **genuine LLM escalation**. Output a separation list
  (draw → class → evidence: phase sequence, record_outcome guard hits, stop_reason).
  This bounds the true artifact share before any code.
- **Step 1 — Failure Brief + §3 classification** under
  `docs/diagnostics/failure-briefs/`; confirm `skill_state` (or escalate per §6).
- **Step 2 — Minimal runtime fix** per the §1 priority order. No content heuristic
  on user_message; the trigger must be structural (grounded FINAL_ANSWER + a
  subsequent user turn), not acknowledgment-keyword detection.
- **Step 3 — Java characterization tests** pinning: a deadlock-shape flow now
  reaches CONFIRM/CLOSE + records; a genuinely premature record (RESOLVE, no
  subsequent user turn) still rejects; no double-record, no early-CLOSE, no
  false-resolved stamp.
- **Step 4 — Bounded NON-pilot real-LLM validation run** (§4).
- **Step 5 — Zero-LLM attribution cross-check** (§5).

## 3. Acceptance — qualifying-event metric (NOT PRIMARY majority-pass)

Success is measured on **qualifying draws**, not on whether the PRIMARY cases
reach majority-pass.

**Qualifying draw shape:** (i) UC/plan entered RESOLVE; AND (ii) a grounded
FINAL_ANSWER formed; AND (iii) a subsequent user turn occurred.

For qualifying draws, the corrective is accepted iff **all**:
1. the flow does **not** stay permanently in RESOLVE;
2. `record_outcome(resolve)` succeeds, **or** a product-contract-allowed resolved
   terminal lands correctly;
3. the **same shape** no longer repeatedly trips
   `progressive_resolve_record_outcome_premature`;
4. blank/stale containment and deadlock loops disappear or drop markedly.

**Power floor:** if the bounded run yields **fewer than 3 qualifying draws**, the
conclusion is **`INCONCLUSIVE`** — the only permitted response is to add more
samples of the *same* bounded validation set; an `INCONCLUSIVE` result does **not**
authorize resuming the pilot.

**Anti-误杀 (must still FAIL / still escalate):** genuine over-escalation,
UC misclassification, genuine STALL, and the genuine-escalation control (§4) must
behave unchanged. The negative control must not regress.

## 4. Bounded NON-pilot real-LLM validation run (human-approved (a))

Run via `eval_interactive` against the honest baseline with the backend rebuilt
on the fix (§5.9 pre-flight first; keep the Mac awake / `caffeinate`). **No
`autoloop run`, no candidate search, no full pilot.**

| case | role | n |
|---|---|---|
| `cs_uc_a_no_ad_id_ad_specific` | PRIMARY target | 11 |
| `cs_uc_a_loaded_listing` | PRIMARY target | 11 |
| `cs_uc_a_generic_policy_question` | negative control (anti-over-correction) | 5 |
| `cs_uc_fp_loaded_moderation` | Tier-2 neighbor | 5 |
| `cs_uc_a_lookup_failed` | Tier-2 neighbor (graceful degrade) | 5 |
| **genuine-escalation high-risk control** (candidate `cs11g02_uc_d_explicit_distress`, risk=high, `should_escalate=true`; dev confirms a baseline-stable should-escalate case) | **NEW** negative control (anti-false-resolve) | 5 |

The genuine-escalation control proves: a should-escalate flow **still escalates**;
the fix does **not** wrongly advance an escalation-required case to CONFIRM/CLOSE;
no false-resolved stamp and no wrong `record_outcome(resolve)`. Still bounded
non-pilot corrective validation — not full fitness, not candidate search.

## 5. Zero-LLM attribution cross-check (human-approved (b)) — evidence boundary

Two distinct evidence kinds; do **not** conflate them:

1. **OLD-trace zero-LLM replay** — re-score the existing recorded traces under the
   unchanged evaluator; proves **evaluator / CaseSpec / scoring semantics are
   unchanged**. This alone does **NOT** validate the runtime behaviour change.
2. **NEW-trace attribution** — for each newly-passing qualifying draw in the
   bounded run, show from the **phase transition, tool result, containment, and
   failure tags** that the new success is caused by the deadlock being lifted
   (not by any scoring change).
3. **Hash pinning** — pin and report the evaluator / CaseSpec / scoring code
   hashes (e.g. `scoring_code_baseline_sha` + CaseSpec file hashes) OLD vs the
   validation run, demonstrating they are byte-identical.

The runtime behaviour change is validated by (the bounded run §4) + (NEW-trace
attribution); the OLD-trace replay only certifies the measurement floor did not move.

## 6. Frozen-surface STOP semantics

Modifying the **frozen-surface-adjacent implementation**
(`ResolveDispositionEvaluator` / `isResolvedSuccessTerminal` / the
ControlKernel phase-transition path) **is permitted this sprint** under this
contract's explicit authorization + the §7 disclosure + per-sub-sprint Codex
review.

**STOP and escalate to `human_review_required`** only if the minimal fix would
require any of:
- relaxing the premature `record_outcome(resolve)` product constraint;
- weakening a Tier-0 / frozen-invariant *semantic*;
- adding a user-message keyword / content heuristic;
- adding a PRIMARY / UC / case-specific exception;
- masking the deadlock via a faked resolved stamp or a `max_turns` increase.

## 7. §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `skill_state` (runtime multi-turn phase machine; escalate
to `human_review_required` per §6).

**Tier-0 invariant:** Adds **no** Tier-0 invariant and **preserves** the
premature-resolve guard + the §1.4 trace-contract. Modifies a frozen-surface-adjacent
implementation (`ResolveDispositionEvaluator` / `isResolvedSuccessTerminal`) under
the §6 authorization; no `runtime_freeze_and_risk_policy.md` invariant is weakened.

**Semantic hardcode:** **No semantic hardcode introduced.** The fix is a structural
phase-transition path (grounded FINAL_ANSWER + subsequent user turn → confirmable),
not a keyword / regex / enum / per-UC rule, and adds no content heuristic on
user_message.

**Generalization coverage:** target / neighbor / negative / shadow =
**2 / 2 / 2 / (held-out)** — PRIMARY `no_ad_id` + `loaded_listing`; neighbors
`cs_uc_fp_loaded_moderation` + `cs_uc_a_lookup_failed`; negatives
`cs_uc_a_generic_policy_question` (anti-over-correction) + the genuine-escalation
high-risk control (anti-false-resolve); shadow held-out (dev does not read).

## 8. Test / eval requirements

- Java + Python suites: no new regression vs the documented baselines.
- New Java characterization tests per §2 Step 3.
- Bounded real-LLM validation run per §4 (§5.9 pre-flight = go/no-go first).
- Zero-LLM attribution cross-check per §5, with hashes pinned + reported.
- Real-LLM rerun is the eval evidence for the behaviour change (§5.7): the bounded
  §4 run + §5 NEW-trace attribution are primary; OLD-trace replay is the
  measurement-floor cross-check only.

## 9. Hard fences / STOP conditions

- No relaxing the premature-resolve guard; no lowering the `record_outcome`
  requirement.
- No CaseSpec edit; no evaluator pass-criterion edit.
- No PRIMARY / UC / case-specific exception; no user-message content heuristic.
- No `max_turns` increase or faked resolved stamp to mask the deadlock.
- Over-escalation, UC misclassification, genuine STALL, and the genuine-escalation
  control must still fail / still escalate.
- Terminal-stamp extension is SECONDARY only (per §1) and requires the
  proven-not-minimally-fixable justification in the handoff.
- **STOP** + escalate to `human_review_required` under any §6 trigger.
- Objective-alignment annotation NOT implemented; M-Auto-7 real-LLM pilot NOT
  resumed; full pilot tranche stays HELD; exp-82 stays WITHDRAWN.

## 10. Codex review plan (§4.3)

Per-sub-sprint Codex REQUIRED. Nine-question anti-hardcode kernel on the runtime
diff + the §2 Step-3 tests, reviewed alongside the Step-0 attribution, the §4
bounded-run evidence, and the §5 cross-check (incl. the pinned hashes). Verdict →
`docs/codex-findings.md` (§4.2 header). Prompt drafted by deliver at close. Pilot
resume is NOT discussed until this verdict is `pass`.

## 11. Handoff requirements

`docs/sprints/sprint-093-handoff.md` records: the Step-0 escalation-attribution
separation list; the Failure Brief + §3 classification; the runtime diff + commit
sha (and, if the SECONDARY path was taken, the proven-not-minimally-fixable
justification); the Java characterization tests; the §4 bounded-run results with
the qualifying-draw count + the per-qualifying-draw verdict (or `INCONCLUSIVE` if
<3); the §5 OLD-replay + NEW-attribution split with pinned hashes; the
genuine-escalation control result; the Codex verdict; and an explicit restatement
that no PRIMARY-majority flip is required, no CS4 success is claimed, the pilot
stays HELD, and exp-82 stays WITHDRAWN.

## 12. Commit discipline

Stage explicitly by file (NO `git add -A`). Runtime fix + tests are one commit;
the bounded-run/cross-check artefacts are gitignored data. Deliver close-bundle
docs are bundled by the human at close. Run any real-LLM step on a clean committed
tree (autoloop sweeps the staged index).
