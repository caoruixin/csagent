# Framework-Defect Brief — OQ-S77.stall-not-gated

> **Brief class:** framework-defect (per `iteration_governance.md` §2 broadened
> intro / §2.1 variant — the eval framework reported PASS, but the underlying
> conversation actually shows the bot stalled).
> **Source artefact:** `eval_interactive/results/m-auto-5-baseline-20260604-simfixed/`
> (12 cases × bad_cases + 12 anchor_outcome + 22 shadow; 9 attempts/case;
> S-Auto-21 simulator-fixed corpus). Forensic, NOT authoritative — the M-Auto-5
> close is blocked on this brief.
> **Filed:** 2026-06-05 (S-Auto-21 close evidence review; opens S-Auto-22 /
> Sprint 077 fix-iteration sub-sprint).
> **Layer (§3):** `infra` — eval-framework scoring gate (`composite.py` +
> `hard_checks.py` + the upstream `containment_outcome` stamp persistence in
> `BotSession` / `ControlKernel`). NOT a bot semantic defect.
> **Framework-defect priority (§5.8):** active. While open, do NOT launch new
> semantic sub-sprints; do NOT perform §5.6 bad-case rerun for close evidence.

## What happened?

In the S-Auto-21 simfixed re-bless, **12 of 216 bad_cases+anchor_outcome
draws (5.5 %) reported `case_passed=true`** despite carrying genuine
failure shapes the eval framework was supposed to gate on. Specifically,
all 12 share this fingerprint:

```
case_passed=true
composite_score=0.0
l2_results=[]
containment_outcome="resolved"     ← S-Auto-20 stamp fired
failure_tags=["STALL:PLACEHOLDER_WITHOUT_FOLLOWUP", ...]  (in 8/12)
stop_reason ∈ {loop_detected, goal_impossible, goal_achieved}
```

Three sampled draws show the pattern is mostly real bot stalls:

- `bad_cases/a3/cs012_uc_fp_late_phone_failure_path` — 4 turns. Bot
  delivered a substantive grounded answer earlier (stamp fires
  `resolved`). Then turns 5-7: user re-asks; bot replies *"I'm looking
  into this for you."* twice in a row → `loop_detected`.
  `failure_tags = [STALL:PLACEHOLDER_WITHOUT_FOLLOWUP,
  TIER2_ADVISORY:record-outcome-on-grounded-answer]`. **case_passed=true**.
- `anchor_outcome/a3/anchor_outcome_uc_fp_removed` — 10 turns, same shape:
  earlier resolved stamp → later "I'm looking into this for you." × 2 →
  `loop_detected`. Same `failure_tags`. **case_passed=true**.
- `anchor_outcome/a0/anchor_outcome_uc_b_posting` — 3 turns. Bot
  legitimately answered "Yes, you can give away free items on Gumtree.
  The Community category is perfect…" → user "Thank you, that helps." →
  `goal_achieved`. `failure_tags = [TIER2_ADVISORY:record-outcome-on-
  grounded-answer]` only — no STALL. This one is a **legitimate pass**
  with an advisory housekeeping nit.

So 2/3 sampled vacuous-pass traces are real bot stalls being marked
PASS; 1/3 is a legitimate pass mislabelled "vacuous" by the strict
fingerprint (because TIER2_ADVISORY alone produced an empty
`l2_results` list).

Distribution of affected cases (12 draws across 5 cases):
- `cs012_uc_fp_late_phone_failure_path` × 2 (bad_cases reducible-flaky)
- `cs095_uc_d_email_recovery_misroute` × 4 (bad_cases stable)
- `anchor_outcome_uc_b_posting` × 4 (reducible-flaky)
- `anchor_outcome_uc_a_visibility` × 1 (stable)
- `anchor_outcome_uc_fp_removed` × 2 (stable)

## What should a good CS eval framework have done?

The eval framework should have marked these draws **`case_passed=false`**
because the bot demonstrably stalled (STALL:PLACEHOLDER_WITHOUT_FOLLOWUP
tag is present) AND/OR the session ended on `loop_detected` (the user
gave up because the bot didn't make progress). The early
`containment_outcome="resolved"` stamp from an earlier turn must not
override a subsequent loop / stall / goal_impossible terminal — that
stamp is a per-turn observation, not a session-final verdict.

Concretely, the framework should have applied at least one of:

- **Promote STALL:* failure_tags to `case_passed=false`.** When
  `failure_tags` carries any `STALL:*` (PLACEHOLDER_WITHOUT_FOLLOWUP or
  any future stall family), the case is structurally a fail regardless
  of composite/L2/judge.
- **Downgrade or overwrite earlier `containment_outcome="resolved"`
  when the FINAL `stop_reason` is `loop_detected` / `goal_impossible` /
  `error`.** A session that ended in a loop or impossibility was not
  resolved, no matter what an earlier turn stamped.
- **Refuse semantic-pass-by-default when `l2_results=[]`** unless the
  case is explicitly allowlisted as L2-not-applicable (and even then,
  the floor should not be `containment_outcome="resolved"` alone).

## Why does this matter?

The M-Auto-5 milestone goal (`docs/milestone_objective.md` Goal) is
"make every per-case eval verdict reflect the bot's actual behaviour".
S-Auto-19 + S-Auto-20 + S-Auto-21 corrected three layers of
false-negative measurement artifacts (eval reads, runtime stamp,
simulator input). This brief discloses a fourth layer of measurement
artifact — **false-positive verdicts** — that the corrections exposed
but did not address. Without this fix, M-Auto-5's authoritative close
would replace one bias (false-failures inflated by simulator
contamination + missing runtime stamps + wrong-typed eval reads) with
another (false-passes inflated by stalls being un-gated when L2 is
empty).

Magnitude impact on the S-Auto-21 simfixed verdict distribution: at
least 12 of the F→P flips' aggregate `pass_rate` mass is suspect.
Specifically:

- `cs095` went 0.43 → **1.00 stable** — 4/9 draws (44 %) are
  vacuous-pass.
- `anchor_outcome_uc_a_visibility` went 0.00 → **1.00 stable** — 1/9
  vacuous-pass; further sampling needed to characterize.
- `anchor_outcome_uc_fp_removed` went 0.43 → **0.89 stable** — at
  least 2/9 vacuous-pass.

The 9 F→P flips' *direction* is still supported by the simulator-fix
evidence (clean input column produces traces where the bot can succeed);
the *magnitude* of each flip is contaminated by this gate gap. Therefore
the simfixed run is valid as **forensic evidence** (proves: simulator
fix removed contamination + S-Auto-20 stamp fires + near-coinflip
eliminated + discovered this gate gap) but is NOT a valid authoritative
M-Auto-5 baseline.

Violates Constitution §1.6 ("Eval is evidence, not authority. A
pass-rate increase is insufficient unless it improves generalizable
customer problem-solving and does not regress safety, grounding, wrong
containment, or architecture health.") — wrong-containment regressed
in the false-positive direction, even though it improved in the
false-negative direction.

## Is this a one-off or a pattern?

**Pattern.** 12 draws across 5 distinct cases, 3 distinct
stop_reasons (loop_detected, goal_achieved, goal_impossible). The
underlying gate gap is uniform: when `l2_results=[]` AND
`containment_outcome="resolved"`, the framework computes
`case_passed=true` regardless of whether `failure_tags` carries
STALL:* or whether the FINAL `stop_reason` is a terminal-failure
shape. Reproduces deterministically per (case, attempt) on the
simfixed corpus.

## Which layer is likely responsible?

**`infra`** — eval-framework scoring gate. Most likely sites:

- `eval_interactive/eval_interactive/scoring/composite.py` — the
  case-pass computation that combines L1 / L2 / containment / composite
  into the final `case_passed` boolean (the same module that, at line
  228-232, falls through to `judge_score = 0.0` when the gating L3 list
  is empty per OQ-S76.judge-zero — it is plausible that the analogous
  fall-through path exists for `case_passed`).
- `eval_interactive/eval_interactive/scoring/hard_checks.py` — `trace_minimum`
  treats `containment_outcome="resolved"` as success regardless of
  later loop_detected.
- `server/...runtime/ControlKernel.java` (`isResolvedSuccessTerminal` +
  the per-turn stamp persistence in BotSession) — S-Auto-20 broadened
  the stamp to fire on the goal_achieved one-shot path; it did NOT
  add a "void the stamp if the session subsequently loops" guard.

Per §3.2 decision questions: this is **not** semantic (§1.3) — the bot
demonstrably stalled; the failure is in measurement-side gate logic.
NOT `eval_spec` (the CaseSpec is asking the system to do something it
can and should do — answer the user); the gap is in HOW the gate
reads the trace, not WHAT it asks for.

## What should NOT be done?

The tempting-but-wrong fixes:

- **Adding a CaseSpec-specific allowlist for cs012/cs095/uc_b/uc_a/
  uc_fp_removed.** This is a per-case patch that closes the symptom on
  the named cases but leaves the gap open for every future case that
  trips the same shape. Violates §1.7 ("adding UC-specific hard rules
  for soft semantic decisions"). The fix must be structural — the gate
  treats STALL:* / loop_detected / empty-L2 uniformly across the
  corpus.
- **Promoting `TIER2_ADVISORY:*` failure_tags to `case_passed=false`.**
  The advisory tier exists for a reason — those are housekeeping nits
  (e.g. didn't call `record_outcome` after a grounded answer), not
  failures. Promoting all of them would over-correct in the
  false-fail direction. Only `STALL:*` and the loop/error/impossible
  terminal-failure shapes warrant promotion.
- **Hardcoding `containment_outcome="resolved" → case_passed=false`
  when stop_reason=loop_detected** at the eval-side without fixing
  the upstream runtime stamp.** This may pass the gap on this fingerprint
  but creates a new inconsistency: the trace will show
  `containment_outcome="resolved"` AND the eval says PASS=false, with
  no per-trace explanation that the stamp was earlier-than-final.
  The cleaner fix is to ALSO add a runtime-side guard that
  voids/downgrades the stamp when the session subsequently loops (so
  the trace is internally consistent).
- **Adding a generic "if any failure_tag is present, fail" rule.** Many
  failure_tags are informational / advisory (per the existing tier
  taxonomy). A blanket promotion would re-introduce the false-fail
  bias the M-Auto-5 sub-sprint sequence just removed. The rule must
  be selective on `STALL:*` and the terminal-failure-shape stop_reasons.

## Routing decision

S-Auto-22 / Sprint 077 — corrective #3 of M-Auto-5 (per human direction
2026-06-05). Scope items:

1. Eval-side gate: when `failure_tags` contains `STALL:*` → promote to
   `case_passed=false` regardless of composite/L2/containment.
2. Eval-side gate: when `l2_results=[]` AND `composite=0` → do not
   semantic-pass on `containment_outcome="resolved"` alone unless the
   case is explicitly allowlisted as L2-not-applicable.
3. Eval-side gate: when final `stop_reason="loop_detected"` (or
   `goal_impossible` / `error` etc., to be enumerated) → override or
   downgrade the earlier `containment_outcome="resolved"` so the gate
   does not credit a stale stamp.
4. Runtime-side guard (companion to #3): void or downgrade the
   `containment_outcome="resolved"` stamp when the session
   subsequently loops / errors / hits MAX_STEPS (so trace and gate
   stay internally consistent).
5. Anti-误杀 counter-tests in BOTH directions: a genuine resolve still
   passes (the 9 legitimate F→P flips from the simfixed run, e.g.
   cs066 / cs014 / cs012 turn-1 cases, still PASS); a stall still
   fails; an early-resolved-then-looped session FAILS.
6. After ship: re-run the multi-suite re-bless on top of S-Auto-22 →
   `m-auto-5-baseline-20260604-simfixed-stalledfix` (or equivalent);
   re-run paired-evidence review + milestone-shared Codex on the
   corrected baseline; THEN move `baseline_dir` pointer and close
   M-Auto-5.

Pre-flight checklist contribution (§5.9): the cheapest read-only
check that would have caught this is —

> Across the target run's per-attempt `results.json`, count draws
> where `case_passed=true AND composite_score=0 AND l2_results=[]`.
> For every match, halt if any of: `failure_tags` contains `STALL:*`,
> OR final `stop_reason` is in `{loop_detected, goal_impossible,
> error, contract_violation, max_turns_exceeded}`.

This check will be added to the pre-flight section of
`docs/diagnostics/2026-06-04-eval-framework-and-simulator-audit.md`
§6 per §5.9.

## Provenance

- Investigation: 2026-06-05, deliver-agent inline trace review at the
  S-Auto-21 sub-sprint close. Three traces sampled in detail
  (`cs012_uc_fp_late_phone_failure_path` a3,
  `anchor_outcome_uc_b_posting` a0,
  `anchor_outcome_uc_fp_removed` a3); full 12-draw fingerprint list
  enumerated from the simfixed `_rebless_scratch/_attempts/a*/`.
- Run artefact: `eval_interactive/results/m-auto-5-baseline-20260604-
  simfixed/` (forensic-only; pointer NOT moved).
- Surrounding context: `docs/diagnostics/2026-06-05-m-auto-5-rebless-
  launch-record.md` §9 (forensic classification of the simfixed run).
- Sub-sprint that consumes this brief: S-Auto-22 / Sprint 077 (open
  2026-06-05; contract at `docs/sprint_objective.md`).
