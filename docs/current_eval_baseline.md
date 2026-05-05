# Current Eval Baseline

Date: 2026-05-05 (post Sprint 6 — accepted closure)

## Purpose

This file freezes the accepted baseline for the next targeted
runtime-behaviour sprint.

The next sprint should compare new results against the **post-Sprint-6**
accepted state. Sprint 6 implemented exactly G0 / G1 / G2 (Kimi
ReadTimeout mitigation, corrected C1 prompt for cs_176 targeting
`user_requested`, S1 FAQ-grounded-resolve PhasePlan / skill — see
`docs/10-handoff.md` Sprint 6 section). The post-Sprint-4 baseline
remains the historical reference for ReadTimeout-incidence comparison;
the post-Sprint-6 r1 baseline is the current canonical reference for
Sprint 7 regression checks.

## Current sprint baseline (post Sprint 6)

Canonical current result:

`eval_interactive/results/20260505-112736/results.json`

This is the accepted Sprint 6 r1 result (8/14, mean composite
0.4826) used after:

- G0 Kimi `session_create_failed: ReadTimeout` mitigation
  (eval-side `AgentClient.create_session` 120s read timeout +
  accept-and-retry on `httpx.ReadTimeout`, scoped to session-create
  only; preserves Sprint 3 §C0 / §C1 bot-side semantics).
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