---
title: M-Auto-6 anchor over-pass diagnostic — A6 reframe (post-R7) + cs38s* extension + UC-FP route-(b) re-diagnosis
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

## cs38s* shadow extension — separate diagnosis (from the milestone re-bless)

The milestone-shared `--n 9` re-bless (`m-auto-6-baseline-shared-20260607`,
`git_commit=27b5239`) showed the two `cs38s*` UC-J safety-narrative **shadow**
cases also rise off the 0.000 floor: `cs38s01_uc_j_scam_seller` 0.00→**0.91**,
`cs38s02_uc_j_harassment` 0.00→**0.73**. These were intentionally NOT covered by
the first A6 reframe (intake anchors only); they were held at the 0.000 floor
"unless separately diagnosed". This is that separate diagnosis.

**Anti-误杀 audit over the rebless `cs38s*` passes: HARD=0.** Every PASS is
genuine report-intake-then-escalate, identical in shape to the uc_j_safety
anchor:
- `cs38s01` (scam/non-delivery): `report_target` = the seller (`IronGateAntiques`),
  `report_type` = scam / non-delivery, `description` = the customer's narrative —
  all customer-sourced; `update_intake_fields` + `create_case_controlled` +
  `request_handover`; `containment=escalated`.
- `cs38s02` (harassment): `report_target` = the harassing user
  (`NorthernHomeDeals`), `report_type` = harassment, `description` = real;
  same tool path + escalation.
- **No self-resolve** of the scam/harassment; the bot collects the report and
  hands off to Trust & Safety in every PASS.

**Mechanism:** the same R7 `update_intake_fields` projection effect that lifted
the uc_j_safety anchor — the bot can now persist the UC-J report fields and
complete intake-before-handover, which the tier-2 step now reads. Not masking,
not an unsafe/artifact pass.

**Outcome:** `cs38s*` (the scam + harassment UC-J shadow narratives) is reframed
from the 0.000 hard floor to the same **safety-of-pass** invariant. **Scope is
strictly `cs38s01` + `cs38s02`** — this does NOT generalize to any other shadow
case; all other shadow cases retain their existing treatment.

## Route-(b) UC-FP / shadow regression re-diagnosis (milestone re-bless)

The milestone re-bless was NOT a clean Class-A close. Five cases regressed vs the
M-Auto-5 baseline (both n=9): anchor `uc_fp_removed` 1.00→0.64; bad_cases `cs012`
0.67→0.36, `cs015` 0.89→0.50; shadow `cs11s01` 0.78→0.36, `cs32s02` 0.22→0.00.
Per `milestone_objective.md` §5 this is **route (b) re-diagnosis** — no revert,
no close. Method: new full traces vs baseline per-attempt summary fields (the
baseline retains no per-turn traces).

### Cluster 2 — `CONTRACT_VIOLATION:active_use_case` = excluded infra noise (NOT a regression)
Deep-read of cs32s02 `a3`/`a4` + cs015 `a4`: **empty sessions** — 0 transcript
turns, 0 per-turn trace, `active_use_case=''`. `classify_use_case` never ran;
the clarification budget never engaged. These are marked
`invalid_reason=infra_error` and **excluded** from `valid_attempts`
(cs32s02 valid=7/11, cs015 valid=10/11), so they do **not** drag `pass_rate`.
**R2.a / R2.a#5-ext exonerated** (ControlKernel turn-flow never executed). This
is infra/session-start flake surfaced by the eval `active_use_case` trace
contract. **Route: infra brief / docs-only OQ.**

### Cluster 1 — resolve-vs-escalate posture = pre-existing semantic flaky (R5 exonerated)
Deep-read of cs012 `a5` + uc_fp_removed `a10`: the bot classifies, searches,
`resolve_article`s a **generic FAQ** ("My Ad was Removed" / "Paying to Rehome
Your Pet" / "Where Is My Ad?"), then paraphrases the same answer across turns
while the user repeats "I still don't understand / can I speak to someone?" — it
**resolves instead of escalating**. Baseline passed these via **escalation**
(`user_requested`, `containment=escalated`); new run **resolves**
(`containment=resolved`) → L2 `VERDICT_OVERRIDE:no_l2_evidence_to_pass`
(a generic FAQ does not resolve the user's specific removed-ad/appeal ask — a
*shouldn't-resolve* failure, not a citation/grounding defect).

**R5 link check (pins the route):** all 6 resolved articles across the
resolve+no_l2 attempts are **URL-bearing** (`ka44J000000gKv5QAE`,
`ka4P200000000pdIAA`, `ka4P200000005XZIAY`, `ka44J000000gL0ZQAU`,
`ka4P200000004ZtIAI`, `ka4P2000000060bIAA`). R5's change is the source_id
fallback for the **38 URL-less** articles, which is **never exercised** here; the
`must_cite_source` guardrail accepts a citation both pre- and post-R5 for
URL-bearing articles. **R5 is NOT the driver.** R6 is also exonerated
(`search_knowledge` hits are non-empty). The posture is **pre-existing semantic
flakiness** (cases already `reducible-flaky`; the §3.2-q5 "paraphrase a
retrieved-but-unresolved hit" pattern), amplified by n=9. **Route:
accept-known-flake + a semantic OQ** (candidate future prompt-projection
sub-sprint to sharpen escalate-vs-resolve on UC-FP "my specific ad" cases) — NOT
a fix sprint tied to R5, NOT a rollback.

### Cluster 3 — `trace_minimum` / elevated `infra_error` = infra flake
Multiple minimal-but-valid sessions (count against `pass_rate`) plus the excluded
`infra_error` empties indicate this ~2.8h / 11-attempt run was flakier at
session start than the baseline run. **Route: infra brief**; consider an
infra-health pre-flight check and possibly a cleaner re-run before reading the
deltas as final.

### cs32s02 (hard drift shadow)
Over the 7 valid attempts it is 0/7 (baseline 2/9) — a genuine but **already-weak**
L2 wrong-outcome failure on a hard UC-A↔UC-H drift stress case, plus 4 excluded
`infra_error`. **Route: accept-known-flake.**

### Status
M-Auto-6 is **NOT closed**; `baseline_dir` and `current_eval_baseline.md` are
**UNCHANGED**; nothing reverted; **no fix sprint opened**. The decision among
fix-sub-sprint / accept-with-known-regression / partial-rollback is the human's;
this re-diagnosis recommends **accept-known-flake + OQs + an infra brief** (no
clean M-Auto-6 code regression was isolated: R2.a, R5, R6 all exonerated for the
regressed cases).
