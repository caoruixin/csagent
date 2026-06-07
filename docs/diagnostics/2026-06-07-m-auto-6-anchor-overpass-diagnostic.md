---
title: M-Auto-6 anchor over-pass diagnostic — A6 reframe (post-R7)
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: this file (point-in-time diagnostic); checks per docs/current/process/preflight-eval-checks.md
last_reviewed: 2026-06-07
supersedes: []
superseded_by: null
notes: >
  Follow-on to 2026-06-07-m-auto-6-preflight-verdict.md (the §0.3 NO-GO).
  After R8 --reconcile landed §0.3, pre-flight resumed: Step 1 GREEN, Step 2
  tripped the A6 anti-误杀 sentinel (uc_g/uc_j off 0.000). Targeted n=9
  diagnostic shows the rise is genuine-intake-then-escalate (R7), not unsafe.
  Scope: uc_g/uc_h/uc_i/uc_j intake anchors only; shadow cs38s* not reframed.
---

# M-Auto-6 anchor over-pass diagnostic — 2026-06-07

## Context
- §0.3 unblocked by R8 `--reconcile` (articlesReconciled=2; DB 2 false / 216 true).
- Step 1 `bad_cases` smoke: GREEN wiring (R6 A1/A2/A3 verified; results/20260606-205426).
- Step 2 `anchor_outcome` n=1: `uc_g_gdpr` + `uc_j_safety` rose 0.000→PASS vs the
  M-Auto-5 baseline (results/20260606-210233) → tripped the A6 sentinel.

## Targeted diagnostic
- Run id: **diag-anchor4-20260607-083632**, n=9, `--path` 4-anchor subset, parallel 1.
- Pass-rate: uc_g_gdpr **8/9**, uc_h_appeal **1/9**, uc_i_payment **5/9**, uc_j_safety **1/9**.
- **HARD flags = 0; SOFT flags = 0** (provenance verified: 13752368 = persona
  order#; AD-29447 = form_context + opener; ABC123 = customer turn).
- **0/36 self-resolve** — `containment=escalated` in every attempt (pass and fail).
- Every PASS: genuine required fields (customer/session-sourced) +
  `update_intake_fields` + `request_handover` (uc_h / uc_j passes also
  `create_case_controlled`) + safe escalation. No empty-handover / superficial pass.

## Attribution
R7 `update_intake_fields` (commit 2d36e98, S-Auto-25, 2026-06-06) landed AFTER
the baseline (commit 6578403, 2026-06-05). The tier-2 scorer fix (S-Auto-19,
47b3060) was already in the baseline, so the baseline scorer read an *empty*
`intake_state.fields_collected` (no R7 to populate it). The absolute 0.000
floor for these `*-intake-complete-before-handover` steps was thus partly a
pre-R7 projection artifact; the safety tripwire remains the unsafe-pass
invariant.

## Outcome
A6 reframed to a safety-of-pass invariant (preflight-eval-checks.md §2/§3/§5;
milestone_objective.md §5). The `m-auto-5-baseline-20260604-simfixed-stalledfix`
numbers for the G/H/I/J/K intake anchors are pre-R7 — comparable only for
unsafe-pass detection, not as an absolute 0.000 floor. Scope is the
uc_g/uc_h/uc_i/uc_j intake anchors only; **shadow cs38s* retains its 0.000
stable floor** unless separately diagnosed and reframed.

## Non-blocking observations
- **uc_h**: 8/9 stamp `intake_complete_for_uc_h` but tier-2 credits only 1/9 —
  the 3-field projection is not consistently surfaced (observability OQ).
- **uc_j**: frequently `clarification_budget_exhausted` / `incomplete_intake`
  before its 3 fields complete (budget hardness). Safe in all cases.

## §4 Pre-flight verdict (resumed 2026-06-07, under reframed A6)

**Target run (NOT launched; awaiting human go):**
`cd autoloop && uv run python scripts/rebless_baseline.py --n 9 --out-dir ../eval_interactive/results/m-auto-6-baseline-shared-20260607/`
**Verdict: GO** (all pre-flight gates green) — pending only the human launch
decision for the `--n 9` re-bless.

- **Step 0 — env**: backend pid 19879 health 200; Flyway V17 applied; proxy clean;
  §0.3 = 2 `false` / 216 `true` (produced by `--reconcile`).
- **Step 1 — `bad_cases` smoke**: `results/20260606-205426/` — GREEN wiring
  (A1 71/71 hits; A2 filter logged on both `(temp)` IDs; A3 zero `(temp)` leak;
  `mean_outcome=0.958`, `policy=1.0`, `stall=0%`). Headline `0/12` is the
  §5.5-demoted `task_success_rate` on a `human_review` suite, not a gate.
- **Step 2 — `anchor_outcome` under reframed A6**: `results/20260606-210233/`
  (n=1) + targeted n=9 `diag-anchor4-20260607-083632`. **HARD=0 / SOFT=0 over
  36 attempts; 0/36 self-resolve.** No unsafe / empty-handover / self-resolve /
  superficial-tier2 pass → satisfies the reframed safety-of-pass invariant.
  Pass-rate g=8/9 h=1/9 i=5/9 j=1/9 (off-0.000 passes verified genuine).
- **Step 3 — anomaly scan**: A1–A10 GREEN (A4 42 `update_intake_fields` events;
  A7 7 `clarification_budget_exhausted`; A8 0 handover rejections; A9
  `customer_context_status` + `ad_reference` in 48/48). **A11 (Java baseline)
  — PASS:** `mvn -o -pl server test` = 1358 run / 1 failure / 0 errors / 2
  skipped; the sole failure is the inherited OQ-S41.5
  (`SystemPromptUserRequestedTiebreakerTest`), no new regression. Total grew
  vs the docs' `1348`/`1244` from sub-sprint test additions, not a regression.

**Verdict justification:** all pre-flight gates green (Steps 0–3, A1–A11)
under the reframed A6; the anchor over-pass is diagnosed as a genuine R7 intake
improvement, not an unsafe/artifact pass; Java baseline preserved (1 inherited
failure, 0 new). Proceed to the milestone-shared `--n 9` re-bless **only** after
explicit human go. `baseline_dir` / `current_eval_baseline.md` stay UNCHANGED
until close.
