# Baseline flip proposal — M-Auto-6 close (route-(b) accept-with-known-regression)

**Status**: DRAFTED — NOT APPLIED. Awaiting human sign-off after the
milestone-shared Codex review at `compact/M-Auto-6-review-prompt.md`
returns its verdict.

**Author**: deliver-agent, 2026-06-07.
**Source-of-truth for the close decision**: `docs/action_bank.md` §5
"M-Auto-6 CLOSE (2026-06-07)".

## Decision context

- Close type: route-(b) accept-with-known-regression (per
  `docs/milestone_objective.md` §5).
- Re-bless artifact:
  `eval_interactive/results/m-auto-6-baseline-shared-20260607/`
  (`--n 9`; multi-suite bad_cases + anchor_outcome + shadow;
  `git_commit=27b5239`; primary_model `deepseek-v4-flash`).
- Anti-误杀 / safety: CLEAN (HARD=0 across 36 anchor attempts + 18
  cs38s* attempts; 0/36 self-resolve; route (c) ruled out).
- Five accepted regressions vs the M-Auto-5 baseline: anchor
  `uc_fp_removed` 1.00→0.64; bad_cases `cs012` 0.67→0.36 + `cs015`
  0.89→0.50; shadow `cs11s01` 0.78→0.36 + `cs32s02` 0.22→0.00.
  Attributed to pre-existing semantic flakiness (Cluster 1; R5 / R6
  exonerated) + session-start infra flake (Cluster 2 + 3; R2.a
  exonerated).
- M-Auto-7 follow-ups filed (none gates this close): semantic OQ
  `OQ-M6.uc-fp-resolve-vs-escalate-boundary`; infra brief
  `OQ-M6.empty-trace-and-session-start-flake`; measurement-honesty
  `R-controlkernel-default-resolved-on-close-anti误杀` (CS1);
  `R-user-role-projection-slot-from-listing-ownership` (CS2).

## Deliver-agent recommendation

**Flip the canonical pointer to
`m-auto-6-baseline-shared-20260607` immediately after Codex APPROVE**,
not after CS1 / CS3 / CS4 land. Rationale:

1. CS1 / CS3 / CS4 are filed as **M-Auto-7 candidates** in
   `docs/action_bank.md` §5 — they are not M-Auto-6 blockers, and
   route-(b) explicitly *accepts* the known regression with those
   follow-ups documented.
2. The post-M-Auto-6 runtime is the surface every future sprint
   (M-Auto-7 included) will measure against. Holding `baseline_dir`
   at `m-auto-5-baseline-...-simfixed-stalledfix` while the runtime
   has advanced through 6 sub-sprints would make every M-Auto-7
   candidate evaluation a hybrid measurement (new runtime vs old
   baseline), inviting attribution errors.
3. The route-(b) decision is itself the affirmative acceptance of the
   regression. Holding the pointer is contradictory — it would
   re-litigate the same decision every time a future sprint reads
   the baseline.
4. The honest measurement floor from M-Auto-5 (0/414 vacuous-pass
   gate; trace-contract honesty) is preserved by the M-Auto-6
   re-bless. The known regressions are real but bounded and
   documented; they do not invalidate the new baseline as the
   reference.

If human disagrees and prefers "hold until CS1 / CS3 / CS4 land",
record the hold + the planned promotion date in a follow-up M-Auto-7
contract; the proposed edits below are reusable when the hold lifts.

---

## Proposed edits — DRAFTED, NOT APPLIED

### Edit 1 — `autoloop/config.yaml` lines ~138–146

**Current state** (verbatim from the file):

```yaml
  # vacuous-pass + runtime stamp downgrade). Codex §4.1 milestone-shared
  # review APPROVE_WITH_NON_BLOCKING_OBSERVATIONS (blocking_count=0); §5.9
  # pre-flight sweep PASS (0/414 vacuous-pass + terminal-failure matches);
  # paired-evidence review 10 F→P / 0 P→F across bad_cases + anchor_outcome
  # + shadow (anti-误杀 held in both directions). Older baseline dirs
  # (m-auto-1b-baseline-20260529, m-auto-4-baseline-20260604,
  # m-auto-5-baseline-20260604, m-auto-5-baseline-20260605,
  # m-auto-5-baseline-20260604-simfixed) retained on disk as forensic-only.
  baseline_dir: eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix
```

**Proposed replacement**:

```yaml
  # vacuous-pass + runtime stamp downgrade). Codex §4.1 milestone-shared
  # review APPROVE_WITH_NON_BLOCKING_OBSERVATIONS (blocking_count=0); §5.9
  # pre-flight sweep PASS (0/414 vacuous-pass + terminal-failure matches);
  # paired-evidence review 10 F→P / 0 P→F across bad_cases + anchor_outcome
  # + shadow (anti-误杀 held in both directions). Older baseline dirs
  # (m-auto-1b-baseline-20260529, m-auto-4-baseline-20260604,
  # m-auto-5-baseline-20260604, m-auto-5-baseline-20260605,
  # m-auto-5-baseline-20260604-simfixed,
  # m-auto-5-baseline-20260604-simfixed-stalledfix) retained on disk as
  # forensic-only.
  #
  # M-Auto-6 close (2026-06-07) — route-(b) accept-with-known-regression
  # (per docs/milestone_objective.md §5 + docs/action_bank.md §5).
  # Milestone-shared re-bless ran at git_commit=27b5239, n=9, multi-suite
  # (bad_cases + anchor_outcome + shadow); anti-误杀 CLEAN (HARD=0 across
  # 36 anchor attempts + 18 cs38s* attempts; 0/36 self-resolve); five
  # accepted regressions attributed to pre-existing semantic flakiness
  # (R5/R6/R2.a exonerated per docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md);
  # A6 anti-误杀 reframed from "0.000 floor" to "safety-of-pass"
  # (preflight-eval-checks.md §2/§3/§5/§7; scope = uc_g/h/i/j +
  # cs38s01/cs38s02). Codex milestone-shared §4.3 APPROVE recorded at
  # docs/codex-findings.md / docs/milestones/M-Auto-6_codex-review.md.
  baseline_dir: eval_interactive/results/m-auto-6-baseline-shared-20260607
```

### Edit 2 — `docs/current_eval_baseline.md` head + new canonical section

**Current head (lines 1–13)** — to be **prepended** with a new
"M-Auto-6 close" lead and the M-Auto-5 section demoted to "Previous
canonical baseline":

```markdown
# Current Eval Baseline

Date: 2026-06-05 (M-Auto-5 close — Eval Verdict Correctness + Trace-Contract Honesty)

## Purpose

This file freezes the accepted authoritative baseline for the next
milestone. As of M-Auto-5 close (2026-06-05), the canonical artifact
is the simulator-fixed + stall-not-gated re-re-bless at
`eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/`.
All prior baselines (post-Sprint-8 / M-Auto-1B / M-Auto-4) are
historical reference only and remain in this file for cross-time
comparison.
```

**Proposed replacement**:

```markdown
# Current Eval Baseline

Date: 2026-06-07 (M-Auto-6 close — Runtime substrate hygiene + admin observability + intake/clarification contract + UX/corpus governance; route-(b) accept-with-known-regression)

## Purpose

This file freezes the accepted authoritative baseline for the next
milestone. As of M-Auto-6 close (2026-06-07), the canonical artifact
is the milestone-shared multi-suite re-bless at
`eval_interactive/results/m-auto-6-baseline-shared-20260607/`. The
prior canonical baseline (M-Auto-5 at
`eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/`,
2026-06-05) is demoted to forensic-only reference; all earlier
baselines (post-Sprint-8 / M-Auto-1B / M-Auto-4 / pre-simfixed
M-Auto-5) remain in this file for cross-time comparison.
```

**Insert a new section between the existing `## Purpose` block and
the existing `## Current canonical baseline (M-Auto-5)` heading** (the
existing M-Auto-5 section is RETAINED but RENAMED to "Previous
canonical baseline (M-Auto-5)"):

```markdown
## Current canonical baseline (M-Auto-6)

Canonical artifact:

`eval_interactive/results/m-auto-6-baseline-shared-20260607/`

Suite layout (multi-suite; per-attempt aggregated to majority over
n=9):

- `bad_cases/` — 12 cases × 9 attempts (curated bad-case suite,
  primary acceptance gate per `process/badcase-lifecycle.md` §5.6;
  stability_summary stable=8 / reducible-flaky=3 / near-coinflip=1).
- `anchor_outcome/` — 12 cases × 9 attempts (anchor UCs A/B/C/D/E/
  F/FP/G/H/I/J/K; stability_summary stable=8 / reducible-flaky=3 /
  near-coinflip=1).
- `shadow/` — 22 cases × 9 attempts (held-out; dev does NOT read;
  stability_summary stable=15 / reducible-flaky=5 / non_comparable=2).
- `_rebless_scratch/` — per-attempt scratch retained as forensic.
- `_rebless_report.json` — aggregated case-level pass_rate +
  stability_class.

Re-bless configuration:

- `git_commit`: `27b5239` (the A6 anti-误杀 reframe + anchor
  over-pass diagnostic commit; first of the three close-decision
  commits 27b5239 / 317bdc2 / d315323).
- `primary_model`: `deepseek-v4-flash`.
- `n=9` per case; multi-suite; paired against
  `m-auto-5-baseline-20260604-simfixed-stalledfix`.

`autoloop/config.yaml:baseline_dir` points here as of 2026-06-07.

Close type:

- **Route (b) accept-with-known-regression** per
  `docs/milestone_objective.md` §5.
- Anti-误杀 / safety: CLEAN. HARD=0 across 36 anchor attempts + 18
  cs38s* attempts; 0/36 self-resolve. Route (c) ruled out.
- Five known regressions accepted (no rollback, no fix sprint):
  anchor `uc_fp_removed` 1.00→0.64; bad_cases `cs012` 0.67→0.36 +
  `cs015` 0.89→0.50; shadow `cs11s01` 0.78→0.36 + `cs32s02`
  0.22→0.00.
- Attributed clusters (all per the diagnostic at
  `docs/diagnostics/2026-06-07-m-auto-6-anchor-overpass-diagnostic.md`):
  - **Cluster 1 — pre-existing semantic flakiness** (resolve-vs-escalate
    on UC-FP). R5 exonerated (all 6 resolved articles URL-bearing →
    R5's source_id-fallback path never exercised). R6 exonerated
    (search hits non-empty).
  - **Cluster 2 — excluded `infra_error` (NOT a regression)**.
    Empty/no-turn sessions; `active_use_case=''`;
    `classify_use_case` never ran; excluded from `valid_attempts`.
    R2.a / R2.a#5-ext exonerated (turn-flow never executed).
  - **Cluster 3 — session-start infra flake**. Run flakier than
    baseline; informational, not a runtime defect.

A6 anti-误杀 reframe (landed 2026-06-07 in commits `27b5239` +
`317bdc2`):

- Reframed from "any rise off 0.000 = reject/revert" to
  **"reject only unsafe / empty-handover / self-resolve /
  superficial-tier2"** — strictly tighter on safety (forbids any
  unsafe pass), permissive only of passes the new R7
  `update_intake_fields` projection legitimately enables.
- Scope: intake anchors `uc_g_gdpr` / `uc_h_appeal` /
  `uc_i_payment` / `uc_j_safety` + shadow `cs38s01_uc_j_scam_seller`
  + `cs38s02_uc_j_harassment`. All other shadow + anchor cases keep
  their existing treatment.
- Recorded in `docs/current/process/preflight-eval-checks.md`
  §2/§3/§5/§7 + `docs/milestone_objective.md` §5.

Close evidence:

- Codex §4.3 milestone-shared review:
  [APPROVE | APPROVE_WITH_NON_BLOCKING_OBSERVATIONS],
  `blocking_count: 0` (archived at
  `docs/milestones/M-Auto-6_codex-review.md`). The route-(b)
  attribution + R5 / R6 / R2.a exoneration + A6 governance reframe
  all confirmed at milestone-shared review.
- §5.9 pre-flight sweep: A1-A11 GREEN under reframed A6; HARD=0 / SOFT=0
  / 0-of-36 self-resolve on targeted diagnostic
  `diag-anchor4-20260607-083632`.
- Paired-evidence review: 5 case-level regressions accepted per
  route-(b); anti-误杀 CLEAN.
- Java baseline preserved at `1358 / 1 / 0 / 2` (sole failure =
  inherited OQ-S41.5; provably uncoupled).

Forensic-only retained dirs (do NOT consume as input):

- `eval_interactive/results/m-auto-1b-baseline-20260529/`
- `eval_interactive/results/m-auto-4-baseline-20260604/`
- `eval_interactive/results/m-auto-5-baseline-20260604/`
- `eval_interactive/results/m-auto-5-baseline-20260605/`
- `eval_interactive/results/m-auto-5-baseline-20260604-simfixed/`
- `eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/`
  (demoted from canonical 2026-06-07 at M-Auto-6 close)

Non-blocking observations carried forward to M-Auto-7 (per
`docs/action_bank.md` §5 follow-up ledger; none gates M-Auto-6
close):

- `OQ-M6.uc-fp-resolve-vs-escalate-boundary` — UC-FP "my specific
  ad" cases should verify entity-context before answering / prefer
  escalation over generic-FAQ resolve. Solution doc:
  `docs/solutions/2026-06-07-cs3-cs4-discover-stall-uc-fp-boundary-empty-trace-ux.md`
  §3 (CS4); routes through OBS-S1 + OBS-S2. Layer
  `prompt_projection` + soft `semantic_planner`.
- `OQ-M6.empty-trace-and-session-start-flake` — empty-trace
  `BOT_HANDLING` sessions (CS2-new, P3 admin UX affordance) +
  DISCOVER null-turn placeholder counted by R2.a (CS3, small fix:
  counter null-turn gating + soft cue). Layer `infra` + admin
  observability.
- `R-controlkernel-default-resolved-on-close-anti误杀` (CS1) —
  Path B at `ControlKernel.java:575-579` default-stamps `resolved`
  on phase=CLOSE without grounding (legacy D16.D, pre-M-Auto-6).
  Small certain runtime fix (gate through
  `isResolvedSuccessTerminal`); lowers pass-rate (honesty). Solution:
  `docs/solutions/2026-06-07-cs1-default-resolved-cs2-user-role-projection-gap.md`
  §3.1.
- `R-user-role-projection-slot-from-listing-ownership` (CS2) —
  seller/buyer perspective slip; no `user_role` projection slot.
  Same solution doc §3.2. Layer `prompt_projection`.
- `R-standalone-reconcile-entry-gate-test` (from S-Auto-28 NBO #1)
  — infra-test pickup.
- `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source`
  (from S-Auto-26 NBO #2) — post-M-Auto-6 docs-only sprint.
- Carry-overs from M-Auto-5 (still open): `OQ-S77.stall-detector-window`;
  `OQ-S77.goal-impossible-resolved-evidence`;
  `R-aggregate-retains-per-attempt-composite-l2`;
  `R-eval-interactive-judge-score-never-populated` (chronic LOW).

## Previous canonical baseline (M-Auto-5)
```

(The existing "Current canonical baseline (M-Auto-5)" section is
**renamed** to "Previous canonical baseline (M-Auto-5)" but otherwise
**retained verbatim** — including the "What M-Auto-5 corrected" list,
the close-evidence subsection, the forensic-only retained dirs list,
and the carry-over NBOs subsection — for forensic provenance and
cross-time comparison.)

---

## Apply sequence (when human signs off)

1. Confirm Codex milestone-shared §4.3 review at
   `docs/codex-findings.md` returns `APPROVE_M_AUTO_6_ROUTE_B` (or
   `APPROVE_M_AUTO_6_WITH_NON_BLOCKING_OBSERVATIONS`) with
   `blocking_count: 0`.
2. Verify the route-(b) attribution claims survived Codex review (R5
   / R6 / R2.a exonerated; A6 reframe = tightening; HARD=0 +
   0/36 self-resolve verified).
3. Apply Edit 1 to `autoloop/config.yaml` (the `baseline_dir` flip +
   comment block update).
4. Apply Edit 2 to `docs/current_eval_baseline.md` (the head
   re-anchor + the new M-Auto-6 canonical section + the M-Auto-5
   demotion to "Previous canonical baseline").
5. Confirm `git diff` shows ONLY the two intended files modified;
   confirm no `eval_interactive/results/*/` directories were touched.
6. Commit with message:
   `M-Auto-6 close — baseline pointer flip to m-auto-6-baseline-shared-20260607 (route-(b) accept-with-known-regression)`.
7. Update `docs/10-handoff.md` §0 cold-start status:
   `baseline_dir` row → `m-auto-6-baseline-shared-20260607`.
8. Proceed to deliver-agent housekeeping (archive
   `docs/milestone_objective.md` → `docs/milestones/M-Auto-6_objective.md`;
   §1 retention compression; §2 archive index append; reset
   `docs/sprint_objective.md` to TBD M-Auto-7 placeholder).
9. Run the §7.1 retention sweep on `docs/action_bank.md` (move closed
   M-Auto-6 R-items + closed sub-sprint rows to
   `docs/action_bank_archive.md` §A/§B/§C).

---

## Reversibility

The flip is fully reversible by restoring the file states at commit
`d315323` (the close-decision record commit, before the flip). The
forensic-only `m-auto-5-baseline-20260604-simfixed-stalledfix/`
directory is retained on disk; the comment block in
`autoloop/config.yaml` is the only governance record of the previous
canonical pointer, and it is preserved (renamed to forensic-only).

---

## Hold-until-CS1/CS3/CS4-land variant (NOT recommended)

If human prefers to hold the flip until CS1 / CS3 / CS4 ship in
M-Auto-7:

1. Skip Edit 1 + Edit 2 entirely. `baseline_dir` stays at
   `m-auto-5-baseline-20260604-simfixed-stalledfix`.
2. Still complete steps 5–9 above (deliver-agent housekeeping +
   action_bank retention sweep) — those are independent of the
   baseline pointer.
3. Add a row to the M-Auto-7 contract / `docs/action_bank.md` §4
   recording the planned promotion date + the trigger (CS1 / CS3 /
   CS4 landed + a fresh M-Auto-7 close re-bless run).
4. Note in `docs/10-handoff.md` §0 that `baseline_dir` is
   intentionally held at M-Auto-5 pending CS1 / CS3 / CS4.

This variant invites measurement-attribution errors (new runtime vs
old baseline) and re-litigates the route-(b) acceptance every time
a future sprint reads the baseline. Recommend AGAINST.
