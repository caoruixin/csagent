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

The pre-Sprint-8 / smoke-anchored historical text below predates the
four-tier eval framework (M3-Eval), the bad-case + anchor + shadow
suite split (M-Auto-1A onward), and the simulator role-inversion fix
(S-Auto-21). It is preserved verbatim for forensic provenance but is
NOT the current measurement reference.

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
  commits `27b5239` / `317bdc2` / `d315323`).
- `primary_model`: `deepseek-v4-flash`.
- `n=9` per case; multi-suite; paired against
  `m-auto-5-baseline-20260604-simfixed-stalledfix`.

`autoloop/config.yaml:baseline_dir` points here as of 2026-06-07.

Close type:

- **Route (b) accept-with-known-regression** per the M-Auto-6
  milestone contract §5 (archived at
  `docs/milestones/M-Auto-6_objective.md`).
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
  §2/§3/§5/§7 + the archived `docs/milestones/M-Auto-6_objective.md`
  §5.

Close evidence:

- Codex §4.3 milestone-shared review:
  **APPROVE_M_AUTO_6_WITH_NON_BLOCKING_OBSERVATIONS**,
  `blocking_count: 0` (archived at
  `docs/milestones/M-Auto-6_codex-review.md`). Cumulative §4.1 kernel
  walk across `6236941..d315323` aggregate `approve`; F1–F8 all PASS;
  route-(b) attribution + R5 / R6 / R2.a exoneration + A6 governance
  reframe = tightening all confirmed at milestone-shared review.
- §5.9 pre-flight sweep: A1-A11 GREEN under reframed A6; HARD=0 / SOFT=0
  / 0-of-36 self-resolve on targeted diagnostic
  `diag-anchor4-20260607-083632`.
- Paired-evidence review: 5 case-level regressions accepted per
  route-(b); anti-误杀 CLEAN.
- Java baseline preserved at `1358 / 1 / 0 / 2` (sole failure =
  inherited OQ-S41.5; provably uncoupled). Codex independent focused
  re-verification: `148 / 0 / 0 / 0`.
- UI baseline preserved at `10 passed / 0 failed / 0 skipped`;
  `npm run build` success (Codex re-verified).

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
  counter null-turn gating + soft cue). Codex NBO #2 reaffirmed the
  fence: stay at `infra` + soft projection guidance; do NOT add
  content/keyword-based clarification detection. Layer `infra` +
  admin observability.
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
- `R-r5-citation-result-binding-grounding-strengthening` (NEW from
  M-Auto-6 milestone-shared Codex NBO #1) — R5's citation validator
  is presence-only by contract; any structural URL/article-ID
  satisfies it, so it does not prove the cited token came from the
  selected `resolve_article` result. Future grounding-strengthening
  opportunity (bind citation acceptance to the active
  `resolve_article` result), NOT a semantic-hardcode fix. Layer
  `infra` + grounding contract.
- `R-standalone-reconcile-entry-gate-test` (from S-Auto-28 NBO #1)
  — infra-test pickup; pin standalone `--reconcile` through
  `ApplicationArguments` to `run()` so a gate regression fails
  before the shared DB step.
- `R-docs-reconciliation-faq-grounding-contract-vs-must-cite-source`
  (from S-Auto-26 NBO #2) — post-M-Auto-6 docs-only sprint.
- Carry-overs from M-Auto-5 (still open): `OQ-S77.stall-detector-window`;
  `OQ-S77.goal-impossible-resolved-evidence`;
  `R-aggregate-retains-per-attempt-composite-l2`;
  `R-eval-interactive-judge-score-never-populated` (chronic LOW).

## Previous canonical baseline (M-Auto-5)

Canonical artifact:

`eval_interactive/results/m-auto-5-baseline-20260604-simfixed-stalledfix/`

Suite layout:

- `bad_cases/` — 12 cases × 9 attempts (curated bad-case suite, primary
  acceptance gate per `process/badcase-lifecycle.md` §5.6)
- `anchor_outcome/` — 12 cases × 9 attempts (anchor UCs A/B/C/D/E/F/FP/G/H/I/J/K)
- `shadow/` — 22 cases × 9 attempts (held-out; dev does NOT read)
- `_rebless_scratch/` — per-attempt scratch retained as forensic
- `_rebless_report.json` — aggregated case-level pass_rate +
  stability_class (stable / reducible-flaky / near-coinflip / non_comparable)

`autoloop/config.yaml:baseline_dir` points here as of 2026-06-05. The
baseline path is referenced via `aggregated.json` per-suite (k-of-n
majority over n=5 baseline aggregate) so live n=3 candidate evaluation
is symmetric (both sides majority — the asymmetry M-Auto-4 / S-Auto-17
removed).

What M-Auto-5 corrected (in order shipped):

1. **S-Auto-19 / Sprint 074 — eval-read column** (5 measurement-artifact
   fixes): `trace_minimum` unions all turns; intake reads dict keys;
   source citations accumulate across the session; `trace_minimum` reads
   terminal disposition; PII relaxation limited to first-party / system /
   RFC 2606 documentation email addresses. Plus runtime `ControlKernel`
   stamps trace-contract fields + answer-turn `sourceIds`.
2. **S-Auto-20 / Sprint 075 — runtime-stamp column**:
   `ControlKernel.isResolvedSuccessTerminal` broadened to `FINAL_ANSWER` +
   `READY_TO_CONFIRM|ANSWERED_SUBTASK` with grounding; `loop_detected`
   removed from valid blank-containment terminals (an eval framework
   correction — a looped session can no longer pass on absent evidence).
3. **S-Auto-21 / Sprint 076 — input column / simulator**: customer
   simulator role-inversion fix at `user_simulator.py:187` + per-turn
   persona re-anchor + negative-form `Forbidden` block + customer-voice
   drift guard (D1 keywords / D2 8-gram Jaccard ≥ 0.8 / D3 leakage
   probes; 3-attempt retry + `SimulatorDriftError` escape). Pre-fix
   corpus sweep 852 → 0 contaminated customer turns on the simfixed run;
   focused bad-case re-render 0/40.
4. **S-Auto-22 / Sprint 077 — vacuous-pass gate + runtime stamp downgrade**:
   STALL signal promoted to `case_passed=false` when scoped to
   `composite==0`; terminal-failure `_TERMINAL_FAILURE_STOP_REASONS`
   override (`goal_impossible` excluded); zero-evidence refusal
   `composite==0 AND l2_results==[]` structural rule (no per-case
   allowlist); runtime `ControlKernel.shouldVoidResolvedStamp` voids
   stale resolved stamp on `{MAX_STEPS, ERROR, DEADLINE_EXCEEDED,
   LLM_UNAVAILABLE}` to `CONTAINMENT_INCOMPLETE_AFTER_PARTIAL_ANSWER`.

Close evidence:

- Codex §4.1 milestone-shared review:
  `APPROVE_WITH_NON_BLOCKING_OBSERVATIONS`, `blocking_count: 0`
  (archived `docs/milestones/M-Auto-5_codex-review.md`). Both S-Auto-22
  deviations (#1 STALL scoped to `composite==0`; #2 terminal-failure
  excluding `goal_impossible`) independently verified + accepted with
  evidence.
- §5.9 pre-flight sweep: 0/414 vacuous-pass + terminal-failure matches
  on the re-re-blessed corpus (framework-defect priority §5.8 LIFTS).
- Paired-evidence review: 10 F→P case-level flips / 0 P→F across
  bad_cases (5) + anchor_outcome (3) + shadow (2). Anti-误杀 held in
  both directions (persistent high-risk anchor uc_g_gdpr / uc_h_appeal /
  uc_i_payment / uc_j_safety + shadow cs38s* preserved at 0.000; near-
  coinflip ELIMINATED across all three suites — OQ-S72.2 dissolved).

Forensic-only retained dirs (do NOT consume as input):

- `eval_interactive/results/m-auto-1b-baseline-20260529/`
- `eval_interactive/results/m-auto-4-baseline-20260604/`
- `eval_interactive/results/m-auto-5-baseline-20260604/`
- `eval_interactive/results/m-auto-5-baseline-20260605/`
- `eval_interactive/results/m-auto-5-baseline-20260604-simfixed/`

Non-blocking observations carried forward to M-Auto-6 (from Codex):

- `OQ-S77.stall-detector-window` — detector window-tuning question
  (infra / possibly eval_spec); the #1 scoping handles the M-Auto-5
  case set, but calibration remains forward work.
- `OQ-S77.goal-impossible-resolved-evidence` — whether
  `resolved+goal_impossible+positive-evidence` should be hard-fail is an
  `eval_spec` policy question.
- `R-aggregate-retains-per-attempt-composite-l2` — new R-item: per-attempt
  `composite_score` + `l2_results` are not retained in compact aggregate
  attempt rows, so the §5.9 414-draw predicate could not be independently
  reproduced from the aggregate alone (Codex used run-local data). Track
  with M-Auto-6 Cluster B observability bundle.
- Cluster C.1 (cs59s 400 session-create) — pre-existing infra error,
  routed to M-Auto-6.
- Judge layer chronic zero (`R-eval-interactive-judge-score-never-populated`)
  — excluded per OQ-S76.judge-zero resolution; canonical signals
  (composite + outcome + L1 + L2 `failure_tags`) are populated.

## Previous sprint baseline (post Sprint 8) — historical reference

Canonical current result:

`eval_interactive/results/20260505-234448/results.json`

This is the accepted post-Sprint-8 r1 result (8/14, mean composite
0.4784, `sprint8-r1`) used after:

- Sprint 8 §K0 cs259 active-use-case contract hardening
  (extracted `ControlKernel.applyMissingUseCaseFallback` helper;
  called from BOTH `forceEscalate` and the AgentRunLoop ESCALATE
  branch in `processMessage` so any escalation surface that
  reaches here without a committed UC gets the deterministic
  fallback; UC-F regex extended with sale-proceeds vocabulary
  `payout|payouts|proceeds|sale|sold|selling|money` so cs259
  family intents resolve to UC-F even when the persona avoids
  the literal "payment" token).
- All Sprint 7 + Sprint 7.1 contracts unchanged
  (Sprint7CandidateUseCasesProjectionTest,
  Sprint7RoutingTiebreakerTest, Sprint7IntakeStateTest,
  Sprint71PartialIntakePersistenceTest all green).
- All Sprint 6 contracts unchanged (G0 ReadTimeout closure, G1
  cs176 user_requested integration regression, G2 S1
  FAQ-grounded-resolve guard).
- Upstream Kimi credential continues to be the rotated working
  Kimi 2.6 provisioning
  (`https://api.moonshot.ai/v1`, `kimi-k2.6`); 0 ReadTimeout / 0
  `INFRA:ReadTimeout` / 0 `session_create_failed` / 0 `401`-tagged
  auth contamination across both clean smoke runs.

Pass rate:

`8/14`

Mean composite:

`0.4784`

Notes:

- **K0 effect visible across BOTH smoke runs.** cs259 commits
  UC-F in r1 AND r2 (was UC-F in Sprint 7 r1 only; Sprint 7 r2
  produced empty UC + `CONTRACT_VIOLATION:active_use_case`
  because the LLM did not call `classify_use_case` before
  handover). 0 contract violations across both Sprint 8 runs.
- cs066 now PASSES in BOTH r1 and r2 (was FAIL in Sprint 7 r1
  via `STALL_AFTER_TOOL_INTENT` /
  `turn_budget_exhausted`). UC-K + intake_complete_for_uc_k +
  Sprint 7.1 §J0 partial-intake persistence stable across runs.
- cs176 now routes to UC-E (expected) in BOTH runs with
  `faq_miss_threshold_exceeded`. The long-deferred UC-I drift
  no longer reproduces under clean Kimi 2.6.
- 0 ReadTimeout / 0 `INFRA:ReadTimeout` / 0
  `session_create_failed` across r1 + r2.
- 0 `L1:escalation_reason_consistency` failures across r1 + r2.
- 0 `CONTRACT_VIOLATION:active_use_case` across r1 + r2 (was 1
  on Sprint 7 r2).

### Stability reference run

Follow-up smoke run to characterise post-Sprint-8 nondeterminism:

- `eval_interactive/results/20260505-235231/results.json` (9/14,
  mean composite 0.5255, `sprint8-r2`; **higher pass count and
  higher composite than r1** — UC-J cs038 PASSES in r2 plus all
  three intake UCs (UC-I cs036, UC-J cs038, UC-K cs040, cs066)
  PASS). 0 ReadTimeout / 0 `INFRA:ReadTimeout` / 0
  `CONTRACT_VIOLATION` / 0 `L1:escalation_reason_consistency`.
  cs095 r2 hits `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP` (eval-side
  stall detector, not a runtime regression — Eval Governance
  scope).

## Previous sprint baseline (post Sprint 7 clean) — historical reference

Canonical post-Sprint-7-clean result (now superseded as canonical):

`eval_interactive/results/20260505-224809/results.json`

This was the accepted post-Sprint-7-clean r1 result (8/14, mean
composite 0.4707, `sprint7-clean-r1`) used after:

- Sprint 7 §I0 `candidate_use_cases` projection + DISCOVER cue
  (cs259 anchor; UC-F now committed instead of drifting to
  UC-J / UC-E / UC-B on the empty-form payment-sale-proceeds
  shape).
- Sprint 7 §I1 UC-FP vs UC-A routing tiebreaker (moderation
  routing-context cue surfaced from session state into
  `routing_prompt.txt`; cs015 anchor; cs014 / cs066 / cs095
  negative guards preserved by 10 focused tests).
- Sprint 7 §I2 `intake_state` projection + UC-G/H/I/J/K
  intake-complete guard (`IntakeFieldsRegistry`,
  `shouldRejectIncompleteIntakeHandover`,
  `persistInlineIntakeFields`; cs066 anchor; FAQ-path UCs
  unaffected).
- Sprint 7.1 §J0 partial intake-field persistence
  (`IntakeFieldExtractor` +
  `AgentRunLoopImpl.mergePartialIntakeFromContext`; UC-K canonical
  fields seeded from form description and captured from user
  reply across clarification turns).
- Upstream Kimi credential rotated to working Kimi 2.6
  provisioning (`https://api.moonshot.ai/v1`, `kimi-k2.6`); 0
  ReadTimeout / 0 `INFRA:ReadTimeout` / 0
  `session_create_failed` / 0 `401`-tagged auth contamination
  across both clean smoke runs.

Pass rate:

`8/14`

Mean composite:

`0.4707`

Notes:

- Sprint 7 §I0 effect visible: cs259 commits UC-F (was UC-J in
  Sprint 6 r1, UC-E in Sprint 6 r2). Remaining cs259 failure is
  FAQ corpus gap / answerability — no resolve-grade article for
  the payment-sale-proceeds intent. Not a routing or runtime
  blocker.
- Sprint 7 §I1 effect partial: cs015 routing-context cue is
  wired but the cs015 form has no `ad_id`, so
  `FormContextIngestionService.autoTriggerCustomerContext` does
  not populate moderation / listing decision; the routing LLM
  receives the `moderation_status: unknown` stub and falls back
  to UC-A. Single narrow next-sprint candidate is a
  description-keyword moderation cue derived from form text
  (anticipated by Sprint 7 handoff §9).
- Sprint 7 §I2 + §J0 effect visible: cs066 r2 + targeted both
  collected `repro_steps_or_error_message` (form seed) and
  `platform=Website` (user reply) and stamped
  `intake_complete_for_uc_k` correctly without re-asking for
  fields. The eval-side stall detector
  (`STALL_AFTER_TOOL_INTENT`) still fires on the intake
  clarification turns; this is an eval governance issue, not a
  Sprint 7 runtime regression. UC-K + intake_complete guard
  contracts preserved.
- 0 ReadTimeout / 0 `INFRA:ReadTimeout` / 0
  `session_create_failed` across r1 + r2 (Sprint 6 §G0 closure
  intact under clean credentials).
- 0 `L1:escalation_reason_consistency` failures across r1 + r2.
- 0 `CONTRACT_VIOLATION:active_use_case` in r1; 1 in r2 on
  cs259 (Kimi tool-use variance — same flake pattern that was
  carried before Sprint 7).
- cs176 r2 UC-I drift remains explicitly deferred per Sprint 5.1
  codex correction and the Sprint 6 acceptance condition.

### Stability reference run

Follow-up smoke run to characterise post-Sprint-7-clean
nondeterminism:

- `eval_interactive/results/20260505-225708/results.json` (7/14,
  mean composite 0.4118, `sprint7-clean-r2`; 0 ReadTimeout / 0
  `INFRA:ReadTimeout` / 1 `CONTRACT_VIOLATION:active_use_case`
  on cs259 — Kimi classify variance — and 0
  `L1:escalation_reason_consistency` fails). The case-level
  diff vs r1 is within the previously-documented
  persona-simulator + stall-detector nondeterminism band:
  cs038 r2 hits `STALL_AFTER_TOOL_INTENT` /
  `turn_budget_exhausted` (was PASS in r1); cs066 r2 stamps
  `intake_complete_for_uc_k` correctly (was
  `turn_budget_exhausted` in r1).

## Previous sprint baseline (post Sprint 6) — historical reference

Canonical post-Sprint-6 result (now superseded as canonical):

`eval_interactive/results/20260505-112736/results.json`

This was the accepted Sprint 6 r1 result (8/14, mean composite
0.4826) used after:

- G0 Kimi `session_create_failed: ReadTimeout` mitigation
  (Sprint 6.1 closure-normalized): eval-side
  `AgentClient.create_session` 120s read timeout widen as the
  single chosen mitigation; no ReadTimeout retry. Escaped
  `httpx.ReadTimeout` is classified/tagged as `INFRA:ReadTimeout`
  by `SessionRunner` / `BatchExecutor`; 401 / 403 / 4xx / 5xx
  remain non-retryable. Scoped to session-create only; preserves
  Sprint 3 §C0 / §C1 bot-side semantics.
- G1 corrected C1 system-prompt change targeting
  `escalation_reason=user_requested` for `cs_interactive_176` —
  one ACTIVE-UC TIEBREAKER paragraph + one GENUINE TIER-2 ESCAPE
  HATCH paragraph in
  `server/src/main/resources/prompts/system_prompt.txt`.
- G2 S1 FAQ-grounded-resolve as a parametrized PhasePlan branch
  inside `PhaseEvaluator.plan(...)` + a deterministic Java guard
  (`AgentRunLoopImpl.shouldRejectFaqMissHandover`) that refuses
  `request_handover(faq_miss_threshold_exceeded)` when
  search_knowledge has viable evidence and resolve_article has not
  yet been attempted.

Pass rate:

`8/14`

Mean composite:

`0.4826`

Notes:

- 0 ReadTimeout / `INFRA:ReadTimeout` / session_create_failed (was
  2 / 3 across post-Sprint-4 r1 / r2). cs_interactive_014 PASS UC-C
  for the first time on a smoke run since Sprint 4 (was ERROR
  ReadTimeout in baseline r1 + r2).
- 0 CONTRACT_VIOLATIONs (was 1 in baseline r1: cs_259).
- `L1:escalation_reason_consistency` remains 0 across both Sprint 6
  smoke runs ✓.
- cs_192 sequence is now
  `[search_knowledge, search_knowledge, resolve_article,
  resolve_article, request_handover]` (vs spec
  `[search_knowledge, resolve_article, record_outcome]`) — the
  "uncited factual answer" failure mode is closed; remaining gap is
  upstream `correct_outcome=resolve` (knowledge corpus) and is out of
  Sprint 6 scope.
- cs_259 contract violation is closed; UC drift to UC-B / UC-J / UC-E
  vs spec UC-F is upstream classification (deferred to C5/DISCOVER
  cue).
- cs_176 r2 UC-I drift is **explicitly deferred** as residual risk
  per Sprint 5.1 codex correction and the Sprint 6 acceptance
  condition (Sprint 6 spec allows EITHER fixing UC-I drift OR
  explicitly deferring it).

### Stability reference run

Follow-up smoke run to characterise nondeterminism (Sprint 6):

- `eval_interactive/results/20260505-113845/results.json` (7/14, mean
  composite 0.4108, 0 ReadTimeout / 0 CONTRACT_VIOLATION /
  0 `L1:escalation_reason_consistency` fails; 1 TIMEOUT on
  cs_interactive_002 — this is the 120s session-level
  `BatchConfig.timeout_per_session_seconds`, NOT a ReadTimeout, and
  not a Sprint 6 regression.)

## Previous sprint baseline (post Sprint 4) — historical reference

Canonical post-Sprint-4 result:

`eval_interactive/results/20260504-221916/results.json`

This is the accepted Sprint 4 smoke result used after:

- E1 narrow runtime gate in `ControlKernel.applyEscalationReason`:
  refuses to upgrade the canonical session reason to
  `user_distress` (priority 2) on the bot LLM's say-so. The
  deterministic §B1 detector earns this Tier-0 reason via a new
  `applyDeterministicDistressReason` helper (called from step 2.4
  after `detectDistressSignal` matches). LLM-supplied
  `user_distress` claims without a prior B1 hit are downgraded
  to `faq_miss_threshold_exceeded` (same `bot_limit` family as
  cs001's spec `clarification_budget_exhausted`). cs002's
  contract is preserved (B1 fires on the seeds).
- E2 cs029 CaseSpec correction through the approved Wave A6.6 v2
  override path: classification block flips primary_uc UC-C ->
  UC-D / secondary_ucs [UC-D] -> [UC-C] for
  `source_session_id 570Q5000008kDiPIAU` (the persona is
  account-locked, not messaging-blocked; the runtime UC fallback
  in `inferFallbackUseCase` already commits UC-D for the
  account-locked seeds). Closes the L2 `correct_uc` gap (D12)
  with no runtime change.
- E3 override / audit consistency guard:
  * Two supporting overrides formalize pre-existing smoke
    hand-edits — cs011 expected.escalation_trigger pinned to
    `faq_miss_threshold_exceeded` post-§E1 (supersedes the
    earlier Codex round 3 §1.6 `user_distress` hand-edit which
    is no longer valid post-§E1), and cs066 classification
    UC-E -> UC-K + expected.escalation_trigger
    `intake_complete_for_uc_k` (formalizes the Codex round 6
    §P0 reclassification through the audit path).
  * `_REQUIRED_CASE_IDS` pinned with cs029 + cs066 so the smoke
    curator's alphabetical UC coverage step doesn't silently
    drop the Sprint 4 §E2 target case or the cs066 UC-K
    regression guard.
  * New `test_smoke_yaml_matches_override_pipeline_output`
    pytest guard hard-fails on any future direct hand edit of
    `expected.*` fields without a matching approved override.

Pass rate:

`8/14`

Mean composite:

`0.4915`

Notes:

- This run is accepted as the post-Sprint-4 closure baseline.
- Sprint 4 r1 lifts pass count 7/14 -> 8/14 and mean composite
  0.4055 -> 0.4915 over the Sprint 3 canonical.
- cs001 (was FAIL composite 0.000) -> PASS composite 0.786 via §E1.
- cs011 (was previously FAIL via cross-family on the LLM's
  user_distress claim) -> PASS composite 0.800 via §E1 supporting
  override.
- cs029 (was FAIL composite 0.000 via L2 correct_uc) -> PASS
  composite 0.967 via §E2.
- cs066 still routes UC-K with `intake_complete_for_uc_k` via
  the §E3 supporting override (PASS r1 / FAIL r2 turn_budget
  variance — UC-K contract preserved, escalation reason variance
  is upstream LLM noise).
- cs014 / cs002 / cs015 / cs192 / cs259 hit the same upstream
  `session_create_failed: ReadTimeout` path on at least one of
  the two runs (Kimi auto-search latency exceeds the 60s eval
  client timeout for some sessions).

### Stability reference run

Follow-up smoke run to characterise nondeterminism (Sprint 4):

- `eval_interactive/results/20260504-223153/results.json` (6/14, mean composite 0.3615)

Cross-run targeted blocker stability for the Sprint 2 / 2.1 / 3 / 3.1 / 4 contracts (Sprint 4 r1 / r2):

- `L1:escalation_reason_consistency`: **0 / 0** across both Sprint 4 smoke runs ✓
- `CONTRACT_VIOLATION:active_use_case`: 1 / 0 — r1 cs_259 (which itself ERRORed on `session_create_failed`); r2 had zero contract violations
- `ERROR:session_create_failed (ReadTimeout)`: 2 / 3 — Kimi auto-search latency exceeded the 60s eval client timeout on cs_014 + cs_192 (r1) and cs_002 + cs_014 + cs_015 (r2). This is upstream LLM latency, not a Sprint 4 regression.
- cs_001 escalation_reason aligned with spec via §E1 gate (faq_miss_threshold_exceeded vs spec clarification_budget_exhausted — same bot_limit family) in both runs ✓
- cs_002 stamps `user_distress` when the bot LLM produces a distress turn (r1, PASS composite 0.786); r2 ERRORed on session_create. Java contract pinned by `Cs002AlreadyEscalatedDistressReconcileIntegrationTest`.
- cs_011 routes to UC-D / `faq_miss_threshold_exceeded` (matches the §E1 supporting override) in both runs ✓
- cs_014 routes to UC-C with the §C2 carry-forward when it does run; both Sprint 4 runs ERRORed on session_create_failed before the route was live. Java contract pinned by `Cs014RouteAndLoopHandoverIntegrationTest`.
- cs_029 target path preserves `active_use_case=UC-D` (now via the §E2 classification override) and semantic `user_requested` ✓ (PASS composite 0.967 in both runs — perfect stability)
- cs_066 routes to UC-K in both Sprint 4 smoke runs ✓ (PASS r1 / FAIL r2 turn_budget variance — UC-K contract preserved, the FAIL is L1:escalation_compliance cross-family on `turn_budget_exhausted` vs spec `intake_complete_for_uc_k` — upstream LLM intake-completion variance, not Sprint 4 regression)
- cs_095 routes to UC-A / `faq_miss_threshold_exceeded` and does not route to UC-K in both Sprint 4 smoke runs ✓ (negative regression guard preserved)

### Targeted cs014 reference

Targeted cs014 run after Sprint 3 §C2 fix:

`results/20260504-191028/results.json`

Result:

- 1 case run
- composite **0.786**
- `active_use_case=UC-C` ✓
- `escalation_reason=faq_miss_threshold_exceeded` ✓
- `L1:escalation_reason_consistency` remained green
- D11 closed: 3/3 Sprint 3 runs route UC-C (cs014 targeted, smoke r1, smoke r2)

### Previous post-Sprint-2.1 reference

Post-Sprint-2.1 canonical result:

`eval_interactive/results/20260504-172942/results.json`

This is the accepted Sprint 2.1 smoke result used after:

- cs_interactive_014 CaseSpec correction moved to the approved v2 override path.
- cs_interactive_014 audit / smoke-case-review records were aligned with the override.
- cs_interactive_014 route/loop handover persistence integration regression was added.
- Codex Sprint 2.1 review returned `decision: pass`, `blocking_count: 0`.

Pass rate (Sprint 2.1):

`5/14`

Mean composite (Sprint 2.1):

`0.291`

Notes:

- This run was the post-Sprint-2.1 closure baseline.
- Sprint 2.1 did not change production runtime behaviour.
- Sprint 3 superseded this baseline with `20260504-191137` (7/14, 0.4055).

### Sprint 2.1 stability reference run

Follow-up smoke run to characterise Sprint 2.1 nondeterminism:

- `eval_interactive/results/20260504-173601/results.json` (5/14, mean composite 0.294)

Cross-run targeted blocker stability for the Sprint 2 / 2.1 contracts:

- `L1:escalation_reason_consistency`: **0 / 0** across both Sprint 2.1 smoke runs ✓
- cs_014 CaseSpec expected trigger is now `faq_miss_threshold_exceeded` through an approved override ✓
- cs_014 route/loop persistence contract is covered by `Cs014RouteAndLoopHandoverIntegrationTest` ✓
- cs_029 target path still preserves `active_use_case=UC-D` and semantic `user_requested` ✓
- cs_066 still routes to UC-K in both Sprint 2.1 smoke runs ✓
- cs_095 routes to UC-A in the completed Sprint 2.1 smoke run and does not route to UC-K ✓
- cs_002 still stamps `user_distress` when the bot produces a completed turn ✓

### Sprint 2.1 targeted cs014 reference

Targeted cs014 run after Sprint 2.1 P1 fix (pre-§C2):

`eval_interactive/results/20260504-172751/results.json`

Result:

- 1 case run
- composite 0.000
- bot LLM picked UC-F instead of UC-C
- `L1:escalation_reason_consistency` remained green

Interpretation:

- This targeted run demonstrated the D11 live bot-loop UC drift that
  Sprint 3 §C2 closed. The targeted Sprint 3 run
  (`results/20260504-191028/results.json`, composite 0.786, UC-C /
  `faq_miss_threshold_exceeded`) supersedes this.

### Previous post-Sprint-2 reference

Post-Sprint-2 canonical result before the Sprint 2.1 closure fixes:

`eval_interactive/results/20260504-151311/results.json`

Result:

- 7/14 passed
- mean composite 0.4015
- mean outcome 0.7590
- mean judge 0.6524

Use this result only when comparing the Sprint 2.1 accepted state against the
pre-closure Sprint 2 behaviour.

### Pre-Sprint-2 reference

Sprint 1 final result:

`eval_interactive/results/20260504-100538/results.json`

Result:

- 6/14 passed
- mean composite 0.3633

Use this only when comparing Sprint 2 / 2.1 against the Sprint 1 ceiling.

## Known nondeterminism (post Sprint 4)

- `cs_interactive_001` is now aligned: §E1 gate downgrades the bot
  LLM's `user_distress` claim to `faq_miss_threshold_exceeded` (same
  `bot_limit` family as the spec `clarification_budget_exhausted`).
  Stable PASS composite ~0.786 in both Sprint 4 runs.
- `cs_interactive_002` can still stamp either `user_distress` (B1
  fires) or `faq_miss_threshold_exceeded` depending on the per-turn
  message; Sprint 3.1 reconcile path keeps the surfaces consistent
  on already-escalated sessions. r2 ERRORed on session_create.
- `cs_interactive_011` is now aligned via the §E1 supporting override
  (escalation_trigger=faq_miss_threshold_exceeded). Stable PASS in
  both Sprint 4 runs.
- `cs_interactive_014` ERRORed on `session_create_failed: ReadTimeout`
  in both Sprint 4 runs — Kimi auto-search latency variance. The
  Java contract surface
  (`Cs014RouteAndLoopHandoverIntegrationTest`) pins UC-C / FAQ-miss
  when the route runs.
- `cs_interactive_029` is now stable: PASS composite 0.967 in both
  Sprint 4 runs (the §E2 classification override flipped primary_uc
  to UC-D, matching the runtime fallback).
- `cs_interactive_066` is stable on UC-K but the escalation_reason
  varies between `intake_complete_for_uc_k` (PASS) and
  `turn_budget_exhausted` (cross-family L1:escalation_compliance
  fail) depending on whether the bot LLM completes the intake
  before exhausting the turn budget. UC-K regression guard preserved
  in both runs.
- `cs_interactive_095` LLM routing remains stable on UC-A / not UC-K
  (negative regression guard preserved in both Sprint 4 runs).
- `cs_interactive_192` turn-0 source citation can appear or disappear;
  hit `session_create_failed` on r1 and `L1:source_citation_present`
  on r2.
- `cs_interactive_176` (UC-E coverage replacement for cs066 after the
  §E3 reclassification) is unstable. The CaseSpec
  (`eval_interactive/case_specs/smoke/cs_interactive_176.yaml`)
  expects `escalation_trigger=user_requested` (the persona explicitly
  asks "What about giving a phone number to talk to someone").
  Sprint 4 r1 stamps `payment_dispute_detected` (cross-family vs
  `user_requested`) and r2 drifts to `active_use_case=UC-I` /
  `escalation_reason=service_degraded` (also cross-family). Neither
  `faq_miss_threshold_exceeded`, `intake_complete_for_uc_k`,
  `service_degraded`, nor `payment_dispute_detected` is a family-match
  against `user_requested` (Sprint 5.1 codex correction); none is an
  acceptable substitute. Carried forward as a future-sprint candidate.
- `cs_interactive_259` stall shape changes across runs and continues
  to surface unrelated active_use_case / session-create issues.
- L3 `relevance` and `tone_appropriateness` judges remain volatile
  and are still deferred.

## Current target cases (next sprint candidates)

After Sprint 4, the candidate set has shifted. Both Sprint 4 §E1
(cs001 LLM-distress over-claim) and §E2 (cs029 spec-vs-fallback)
are closed. The remaining smoke-side instability is upstream
LLM latency on session_create + L3 judge volatility.

- `cs_interactive_014 / 002 / 015 / 192 / 259` — `session_create_failed:
  ReadTimeout` on the Kimi auto-search path. Either widen the eval
  client timeout, pre-warm the first call, or move auto-search to
  an async pre-fetch; out of Sprint 4 scope.
- `cs_interactive_176` — UC-E case where spec is
  `escalation_trigger=user_requested` (user explicitly asks for human
  help). Bot picks `payment_dispute_detected` (r1) or drifts to
  UC-I / `service_degraded` (r2); both are cross-family vs
  `user_requested`. The corrected Sprint 6 fix (F1 §C1) must
  preserve / produce `user_requested` when the user explicitly asks
  for human help — not substitute a FAQ-family or intake-family
  reason. Sprint 6 acceptance must either include "no unjustified
  active_use_case=UC-I drift on cs_176" or explicitly defer that UC
  drift question (residual risk).
- `cs_interactive_259` — UC-F payment FAQ resolve / contract / stall
  flake (carry forward, deferred from prior sprints).
- L3 `relevance` / `tone_appropriateness` judge calibration —
  carry forward, deferred.

Other known candidates after the reliability / bot-loop stability sprint:

- `cs_interactive_004` — over-escalation on FAQ
- `cs_interactive_011` — UC routing flip
- `cs_interactive_015` — UC-FP routing
- `cs_interactive_192` — turn-0 grounding + handover persistence flake
- `cs_interactive_259` — UC-F payment FAQ resolve / contract flake

## Known current blocker patterns (post Sprint 4)

1. **Kimi auto-search latency exceeding the 60s eval client timeout** (NEW after Sprint 4)
   - cs_002 / cs_014 / cs_015 / cs_192 / cs_259 ERRORed on
     `session_create_failed: ReadTimeout` on at least one of the two
     Sprint 4 smoke runs. The auto-search path on session-create
     makes 4-6 chained Kimi LLM calls (FAQ search, resolve_article,
     intake), and individual Kimi calls run 8-15s — total auto-search
     can exceed 60s.
   - Mitigations (out of Sprint 4 scope): widen the eval client
     timeout to 120s; pre-warm Kimi connections; move auto-search to
     an async pre-fetch; or accept and retry on ReadTimeout.

2. **cs_176 UC-E LLM escalation-reason cross-family flake** (NEW after Sprint 4)
   - Spec is `escalation_trigger=user_requested` (the persona
     explicitly asks "What about giving a phone number to talk to
     someone"). Sprint 4 r1 stamps `payment_dispute_detected`
     (cross-family vs `user_requested`); r2 drifts to UC-I with
     `escalation_reason=service_degraded` (also cross-family). Both
     fail L1:escalation_compliance.
   - Sprint 5.1 codex correction: `faq_miss_threshold_exceeded`,
     `intake_complete_for_uc_k`, `service_degraded`, and
     `payment_dispute_detected` are NOT family-match against
     `user_requested` and are NOT acceptable substitutes.
   - Sprint 6 §F1 §C1 must preserve / produce `user_requested` when
     the user explicitly asks for human help. Residual UC-I drift
     risk on r2 must either be addressed in Sprint 6 acceptance
     ("no unjustified UC-I drift on cs_176 r2") or explicitly
     deferred.

3. **L3 judge volatility** (carried over)
   - `relevance` and `tone_appropriateness` still flip across runs.
   - Out of scope for the next targeted runtime sprint.

## Resolved by Sprint 4

- ~~cs001 escalation-reason alignment~~ → §E1 narrow runtime gate
  in `ControlKernel.applyEscalationReason` (LLM-supplied user_distress
  is downgraded to faq_miss_threshold_exceeded without a deterministic
  §B1 hit). Stable PASS in both Sprint 4 runs.
- ~~D12 cs029 outcome lift~~ → §E2 classification override flips
  primary_uc UC-C -> UC-D / secondary [UC-D] -> [UC-C]. Stable PASS
  composite 0.967 in both Sprint 4 runs.
- ~~Override / audit consistency for smoke set~~ → §E3 supporting
  overrides for cs011 + cs066, smoke_curator pin for cs029 + cs066,
  new pytest guard `test_smoke_yaml_matches_override_pipeline_output`.

## Resolved by Sprint 3

- ~~Bot-side Kimi endpoint / credential / rate-limit robustness~~
  → C0 + C1 (fail-fast LlmConfigValidator, bounded retry on 429 / 5xx
  / transport, secret-free startup describe line).
- ~~D11 cs014 live bot-loop UC drift~~ → C2 (strong-prior carry-forward
  policy in `ClassifyUseCaseTool`; D11 closed in 3/3 Sprint 3 runs).
- ~~cs002 UC drift while distress fires~~ → indirectly closed by C2;
  cs002 r1 stamps `user_distress` and routes UC-C in Sprint 3.

## Recommended next sprint direction

Recommended Sprint 5:

`session_create latency stabilisation + cs176 / cs259 alignment`

Recommended Sprint 5 actions (3 narrow):

- F1. Address the Kimi `session_create_failed: ReadTimeout` failure
  mode. The auto-search path makes 4-6 chained LLM calls; either
  pre-warm the first call, increase the eval-client timeout to
  120s, async pre-fetch the FAQ snapshots before the first user
  turn, or accept-and-retry on ReadTimeout.
- F2. cs_176 UC-E classification flake: the bot LLM picks
  `payment_dispute_detected` for a feature-explanation form
  context. Either a narrow runtime guardrail or a spec override.
- F3. cs_259 routing / stall / contract-violation stabilisation
  (carry forward from Sprint 3 / 4).

Do not expand smoke / anchor / promotion during Sprint 5. The
existing smoke surface is stable enough for the next narrow
reliability + alignment pass.