---
title: "exp-90 real-iteration result + PRIMARY capability-to-mutation-surface analysis"
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: >
  autoloop/results/runs/exp-90/ (REAL_RUN_summary.json + eval-results.json) +
  autoloop/results/experiments.jsonl exp-90 row; runtime/eval code paths cited inline (read-only)
last_reviewed: 2026-06-19
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Records the accepted outcome of the single real exp-90 iteration (the frozen
  $.grounding_instruction proposal run byte-identical through the real
  apply->eval->tier pipeline against the frozen pre-pilot fitness baseline) and
  the read-only capability-to-mutation-surface analysis for the two PRIMARY
  UC-A cases. NO code/baseline/canonical/surface change; holds (WP1-B, WP2,
  posterior/bounded-resampling, conditional-adjudication auto-triage, exp-91)
  remain in force. The analysis answers one question: does M-Auto-7 still have a
  legitimate registry control surface for the PRIMARY objective, or has it
  reached the boundary of registry-only optimization.
---

# exp-90 real result + PRIMARY capability-to-mutation-surface analysis

## 1. Acceptance record (human-authorized 2026-06-19)

- **exp-90 = DISCARD, ACCEPTED; isolated experiment CLOSED.** One real iteration,
  proposal byte-identical to the reviewed dry-run (fingerprint
  `c3e798d7dd3ef61f`, injected without a proposer LLM call; the same fingerprint
  is in the persisted `experiments.jsonl` exp-90 row). Decision `discard`,
  controlling reason `tier1_outcome_regressed_count_3_bh_1_thresh_2`. Audit
  trail: branch `autoloop/exp-90` + tag `autoloop/discard-90`;
  `autoloop/results/runs/exp-90/REAL_RUN_summary.json`. Post-run tree clean on
  `auto-loop-branch`@`568387df` (the skill edit is isolated to the exp branch).
- **S-Auto-42 = VALIDATED on first real evidence.** The bool-only flaky-tier0
  defer rule fired for the first time on a live candidate: the near-coinflip
  shadow case `cs38s01_uc_j_scam_seller_full_narrative` (escalation_compliance,
  `baseline_flaky=true`, `candidate_majority_failed=true`) was **DEFERRED**, not
  tier0-discarded — so it did **not** mask later evidence, exactly as designed.
  The candidate still discarded for an **independent** Tier-1 regression. The
  HOLD path (`tier_evaluator.evaluate`, the `if decision == "keep" and
  deferred_flaky` override) **never fired**, because the candidate was already
  `discard` at Tier-1.
- **No posterior confirmation / bounded re-sampling will be built on this
  evidence.** This run produced **zero** `HOLD_INCONCLUSIVE_FLAKY` verdicts:
  the one deferred-flaky entry was preempted by a real Tier-1 discard. Per the
  decision rule "if exp-90 fails independently of deferred flaky, do not build
  re-sampling merely because flaky evidence was present", the
  posterior/bounded-resampling path stays deferred. There is no HOLD bottleneck
  to clear.

## 2. exp-90 conclusion (precise; the two PRIMARY failures are NOT one diagnosis)

- The `$.grounding_instruction` hypothesis is **rejected as a complete fix**:
  neither PRIMARY improved (`cs_uc_a_no_ad_id_ad_specific` 0/11→0/13;
  `cs_uc_a_loaded_listing` 1/11→0/12, a within-noise nominal drop, P_regress
  0.391), **and** the candidate introduced **three** Tier-1 outcome regressions
  (`cs095_uc_d_email_recovery_misroute` 0.818→0.0 P_regress 0.998 + BH-flagged;
  `anchor_uc_fp_removed` 0.70→0.20 P_regress 0.928; `anchor_uc_g_gdpr`
  0.636→0.20 P_regress 0.892; cross-case 3 ≥ threshold 2). Reaching Tier-1
  (unlike the tier0-short-circuited exp-88/89) is what finally tested the
  surface hypothesis on real evidence; it failed it.
- `cs_uc_a_no_ad_id_ad_specific` changed **directionally in the intended way** —
  in the representative trace the bot withheld the generic FAQ answer, **asked
  for the ad reference**, called `get_customer_context` + `search_knowledge` +
  `resolve_article`, and **referenced listing state** ("AD-1001 is live and
  active … posted yesterday"). It **still failed** (0/13), at the
  **record-outcome / RESOLVE→CONFIRM closure layer**, not the grounding surface:
  `L2_GATE:correct_outcome` 12/13, `TIER2_ADVISORY:record-outcome-on-grounded-answer`
  7/13, `failure_reason` `goal_impossible` 10/13.
- `cs_uc_a_loaded_listing` did **not** achieve substantive listing grounding —
  it still showed the Cluster-2 superficial-use pattern (one title/status
  mention then generic "creating effective ads" / Featured-Ad advice) — **and
  also** failed closure/outcome handling: `L2_GATE:correct_outcome` 12/13,
  **`L2_GATE:correct_uc` 8/13** (frequent UC-A→UC-B misclassification),
  `TIER2:record-outcome-on-confirmed-resolve` 6/13.
- Therefore the two PRIMARY failures are **distinct** and must **not** be
  collapsed into a single "grounding" or single "record_outcome" diagnosis:
  `no_ad_id` is grounding-OK / closure-blocked; `loaded_listing` is
  classify-blocked + grounding-superficial + closure-blocked.

## 3. Capability-to-mutation-surface analysis (read-only)

For each causal step: (1) observed failure mechanism; (2) did exp-90 change it;
(3) actual runtime/config consumer; (4) registry-addressable via a whitelisted
field; (5) or runtime/orchestration / record_outcome-confirm / eval-trace work.
Whitelisted surface = `{$.procedure, $.grounding_instruction,
$.escalation_policy, $.critical_steps[*].desc}` × 6 skills; **adding** a new
`critical_step` is OUTSIDE the surface (structural change), only editing an
existing `…desc` is allowed (mutation-surface-scoping-2026-06-19 §3).

### 3.1 `cs_uc_a_no_ad_id_ad_specific`

| step | (1) mechanism | (2) exp-90 changed? | (3) runtime/config consumer | (4)/(5) addressability |
|---|---|---|---|---|
| N1 classify UC-A | correct in rep; `correct_uc` failed only 1/13 — not the blocker | no (resolve_faq edit) | `discover_triage` `classify_use_case` + LLM | registry (discover_triage) — not currently gating |
| N2 ask for ad ref before generic FAQ | baseline bug = straight to generic FAQ; **rep shows bot now asks for the ref** | **yes — directionally fixed** | `resolve_faq_grounded_answer.$.grounding_instruction` → `PhasePlan.groundingInstruction` → `ContextProjectionBuilder.java:1052` + LLM | **registry-addressable; ADDRESSED by exp-90** |
| N3 get_customer_context → grounded answer on listing state | rep references live/active state | partly (grounding teaching) | `get_customer_context` (runtime tool) + grounding_instruction + LLM | registry teaching works; not the blocker |
| N4 **close as SATISFIED+resolve (record_outcome) within budget** | bot grounds but loops to `goal_impossible`; never reaches a recorded resolve the user accepts | **no** | RESOLVE→CONFIRM promotion + premature-resolve guard (`PhaseEvaluator.java:592-604`, `ResolveDispositionEvaluator.java:160`); `confirm.yaml:20/32` teaches record_outcome but the gate + phase promotion are **runtime-frozen**; OQ-S93.1 (`record_outcome` lands 0/N) open | **runtime/orchestration-addressable** (+ secondary answer-quality semantic, LLM-owned/registry-teachable, not the dominant gate) |

### 3.2 `cs_uc_a_loaded_listing`

| step | (1) mechanism | (2) exp-90 changed? | (3) runtime/config consumer | (4)/(5) addressability |
|---|---|---|---|---|
| L1 **classify UC-A (not UC-B)** | `correct_uc` fails **8/13** — frequent UC-A→UC-B misclass (Cluster 1) | **no** — exp-90 never touched `discover_triage` | `discover_triage.$.procedure` / `classify_use_case` + LLM | **registry-addressable — UNTRIED** (edit `discover_triage.$.procedure`; a new UC-A classify critical_step is out-of-surface) |
| L2 **substantive listing grounding (use ≥1 listing field)** | superficial: one title/status mention then generic advice | **yes — and INSUFFICIENT** (still 0/12) | `listing_context` is **fully projected, no truncation** (`ContextProjectionBuilder.java:823-824`) but deep in the projection; `grounding_instruction` controls only **teaching text**, not data prominence; the CaseSpec-named fix ("mandatory consult-then-ground critical step keyed on get_customer_context for UC-A") is **OUTSIDE** the whitelisted surface | **runtime/orchestration-addressable** (projection prominence / structural skill step). Residual low-confidence registry option = `$.procedure` (broad; conflates escalation posture; exp-87 showed in-skill bleed) |
| L3 **close as SATISFIED+resolve (record_outcome)** | `correct_outcome` 12/13, `record-outcome-on-confirmed-resolve` 6/13; bot escalates/ends without an accepted resolve | **no** | same as N4 | **runtime/orchestration-addressable** |

### 3.3 Cross-cutting

- **Trace `source_ids` not projected** despite `resolve_article` running
  (observability debt). Does **not** gate these cases (loaded_listing PASS
  condition (b) reads the bot's `user_message` text; `correct_outcome` reads
  `containment_outcome`; no grounding-floor failure tag fired). →
  **eval/observability-addressable**, minor, non-gating.
- **WP1-A conditional-adjudication auto-triage** (HELD) would only matter if the
  target terminal became *escalate-after-genuine-help*. For these
  cooperative-persona `resolve`-expected `tier_1_target` cases the target is
  **SATISFIED+resolve**, so the conditional gate is functioning correctly and is
  **not** the blocker. (This corrects an over-attribution: the closure failure
  is upstream bot behavior, not the eval gate.)

## 4. Per-blocker classification (exactly one category each)

1. `no_ad_id` — **closure / record_outcome (RESOLVE→CONFIRM, OQ-S93.1)** →
   **runtime/orchestration-addressable**.
2. `loaded_listing` — **UC-A→UC-B misclassification** →
   **registry-addressable** (`discover_triage.$.procedure`; untried by exp-90).
3. `loaded_listing` — **substantive listing grounding salience** →
   **runtime/orchestration-addressable** (dedicated registry teaching surface
   exercised by exp-90 and insufficient; prominence/structural fix is
   out-of-surface).
4. `loaded_listing` — **closure / record_outcome** →
   **runtime/orchestration-addressable**.
5. cross-cutting — **trace `source_ids` projection** →
   **eval/observability-addressable** (minor, non-gating).

## 5. Conclusion — control-surface boundary for the PRIMARY objective

**M-Auto-7 has not fully exhausted its registry surface, but the dominant
PRIMARY blockers are no longer registry-addressable.**

- **One untried registry lever remains**: `discover_triage.$.procedure` for the
  `loaded_listing` UC-A→UC-B misclassification (blocker #2). This is a legitimate
  registry control surface the pilot has not exercised (exp-86..exp-90 all
  targeted `resolve_faq`).
- **The core of the PRIMARY objective — substantive listing grounding (#3) and
  record_outcome/RESOLVE→CONFIRM closure (#1, #4) — has reached the boundary of
  registry-only optimization.** The dedicated soft-field surface
  (`$.grounding_instruction`) was exercised by exp-90 and is insufficient; the
  remaining fixes are runtime/orchestration (projection prominence; the
  record_outcome/CONFIRM closure layer, OQ-S93.1) that lie OUTSIDE the autoloop's
  whitelisted soft-field surface, or require a human-authorized surface
  expansion (program.md §8) to add a mandatory UC-A consult-then-ground step.
- ⇒ **Registry-only optimization cannot, on this evidence, deliver the PRIMARY
  objective by itself.** The classify cluster (#2) is worth one bounded
  registry attempt; the grounding-salience and closure blockers need
  runtime/orchestration work, chiefly the OQ-S93.1 record_outcome/CONFIRM layer
  that already gates the PRIMARY cases regardless of the grounding surface.

## 6. Holds reaffirmed

Per the 2026-06-19 direction: no exp-91; no mutable-surface change; no WP1-B /
WP2; no runtime or evaluation behavior change; no posterior/bounded-resampling;
no re-bless / baseline move / canonical-pointer change; conditional-adjudication
auto-triage stays HELD; exp-82 stays WITHDRAWN. This analysis is read-only and is
submitted for review before any next step.

**Evidence pointers**: `autoloop/results/runs/exp-90/REAL_RUN_summary.json`,
`autoloop/results/runs/exp-90/eval-results.json`,
`autoloop/results/experiments.jsonl` (exp-90 row);
`eval_interactive/case_specs/bad_cases/cs_uc_a_{no_ad_id_ad_specific,loaded_listing}.yaml`;
runtime `PhaseEvaluator.java`, `ResolveDispositionEvaluator.java`,
`ContextProjectionBuilder.java`; eval `outcome_checks.py`, `conditional_outcome.py`,
`simulator/session_runner.py`; `mutation-surface-scoping-2026-06-19.md`,
`failure-clusters-uc-a-listing-2026-06-19.md`.
