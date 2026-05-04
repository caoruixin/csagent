# Current Eval Baseline

Date: 2026-05-05 (post Sprint 3 — accepted closure)

## Purpose

This file freezes the accepted baseline for the next targeted runtime-behaviour sprint.

The next sprint should compare new results against the **post-Sprint-3**
accepted state. Sprint 3 closed all three C0 / C1 / C2 actions (Kimi
endpoint / credential normalization, bounded LLM retry, Replies/Messaging
strong-prior carry-forward into the bot loop), shipped one supporting
alias-encoding fix that was load-bearing for §C2 (the live form sanitizer
HTML-encodes `&` to `&amp;`, so the §B2 alias map needed both forms
registered), and lifted smoke pass count and mean composite above the
post-Sprint-2.1 baseline.

## Current sprint baseline (post Sprint 3)

Canonical current result:

`eval_interactive/results/20260504-191137/results.json`

This is the accepted Sprint 3 smoke result used after:

- C0 fail-fast LLM config validator wired into `LlmClientConfig`
  startup (FATAL on placeholder/blank/malformed primary; WARN on
  default Kimi endpoint; secret-free describe line).
- C1 bounded LLM retry classification (429 / 5xx / transport
  retried once; 401 / 403 surface immediately) with structured
  retry / failure-tag log lines.
- C2 strong-prior carry-forward in `ClassifyUseCaseTool`
  (refuses LLM-driven UC overwrite when the active UC matches
  the deterministic strong-prior derivation; releases on
  `DriftDetector` hard-shift) plus `Replies &amp; Messaging`
  alias registration so the §B2 alias actually fires on the
  live form payload.

Pass rate:

`7/14`

Mean composite:

`0.4055`

Notes:

- This run is accepted as the post-Sprint-3 closure baseline.
- Sprint 3 changed the bot-side runtime behaviour: §C2 closes D11
  (cs014 cross-turn UC drift) and the §B2 encoding fix makes the
  alias path live for cs001 / cs002 / cs014 / cs029 / cs066 / cs095.
- The §C0 / §C1 changes do not change runtime behaviour by themselves;
  they reduce the rate at which infrastructure flakes look like
  semantic failures and they make endpoint / credential
  misconfigurations fail fast at startup with a clear diagnostic.

### Stability reference run

Follow-up smoke run to characterise nondeterminism:

- `eval_interactive/results/20260504-191541/results.json` (6/14, mean composite 0.3589)

Cross-run targeted blocker stability for the Sprint 2 / 2.1 / 3 contracts:

- `L1:escalation_reason_consistency`: **0 / 0** across both Sprint 3 smoke runs ✓
- `CONTRACT_VIOLATION:active_use_case`: **0 / 0** across both Sprint 3 smoke runs ✓ (cs014 D11 closed)
- `TIMEOUT`: **0 / 0** across both Sprint 3 smoke runs ✓ (was 2 / 0 in Sprint 2.1)
- `L1:trace_minimum`: 0 / 1 (cs_259, unrelated to Sprint 3 targets) — was 1 / 0 in Sprint 2.1
- cs_014 routes to UC-C with `faq_miss_threshold_exceeded` in both Sprint 3 smoke runs (3/3 if you also count the targeted run) ✓
- cs_029 target path still preserves `active_use_case=UC-D` and semantic `user_requested` ✓
- cs_066 still routes to UC-K in both Sprint 3 smoke runs ✓
- cs_095 routes to UC-A / `faq_miss_threshold_exceeded` and does not route to UC-K ✓
- cs_002 stamps `user_distress` when the bot LLM produces a distress turn ✓ (run 1)

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

## Known nondeterminism (post Sprint 3)

- `cs_interactive_001` no longer TIMEOUTs (Sprint 3 §C1) but can stamp
  `user_distress` instead of the spec's `faq_miss_threshold_exceeded`
  when the persona phrase trips the B1 detector — this is now a
  spec-vs-runtime alignment question, not a runtime flake.
- `cs_interactive_002` can stamp either `user_distress` (B1 fires)
  or `faq_miss_threshold_exceeded` depending on the per-turn
  message the persona simulator emits. The detector itself is
  deterministic and pinned by Java tests.
- `cs_interactive_014` is now stable on UC-C across runs (Sprint 3
  §C2 closed D11). The remaining variability is L3 judge volatility
  on the bot's grounded final answer.
- `cs_interactive_011` LLM routing can rotate between runs.
- `cs_interactive_192` turn-0 source citation can appear or disappear.
- `cs_interactive_259` stall shape changes across runs and can still produce unrelated active_use_case contract issues.
- `cs_interactive_066` can stall on a bot-side LLM intent-without-tool flake even when UC-K is correctly committed.
- L3 `relevance` and `tone_appropriateness` judges remain volatile and are still deferred.

## Current target cases (next sprint candidates)

After Sprint 3, the candidate set has shifted. Both cs014 D11 drift
and cs001 / cs002 / cs014 LLM-flake instability are closed. The
remaining surface is:

- `cs_interactive_001` — escalation-reason alignment (`user_distress` vs spec `faq_miss_threshold_exceeded`); spec vs runtime alignment, not a runtime flake.
- `cs_interactive_002` — same family; runtime detector deterministic, persona-simulator picks the message.
- `cs_interactive_029` — B3 regression guard; D12 outcome lift remains deferred.
- `cs_interactive_066` — UC-K regression guard.
- `cs_interactive_095` — not-UC-K regression guard.
- `cs_interactive_259` — UC-F payment FAQ resolve / contract / stall flake (not a Sprint 3 target; carry forward).

Other known candidates after the reliability / bot-loop stability sprint:

- `cs_interactive_004` — over-escalation on FAQ
- `cs_interactive_011` — UC routing flip
- `cs_interactive_015` — UC-FP routing
- `cs_interactive_192` — turn-0 grounding + handover persistence flake
- `cs_interactive_259` — UC-F payment FAQ resolve / contract flake

## Known current blocker patterns (post Sprint 3)

1. **cs001 / cs002 escalation-reason alignment** (NEW after Sprint 3)
   - The bot now reaches both cases reliably without timing out.
   - The remaining failure mode is the spec expecting
     `faq_miss_threshold_exceeded` while the runtime emits
     `user_distress` (B1 fires on the persona phrasing).
   - Spec-vs-runtime alignment, not a runtime regression.

2. **D12 cs029 outcome lift** (carried over)
   - Sprint 2 B3 fixed the active_use_case contract violation for the target case.
   - L2 `correct_uc` still fails because the deterministic UC-D fallback differs from the spec primary UC-C.
   - Spec-vs-fallback alignment question; remains out of runtime-reliability scope.

3. **L3 judge volatility** (carried over)
   - `relevance` and `tone_appropriateness` still flip across runs.
   - Out of scope for the next targeted runtime sprint.

## Resolved by Sprint 3

- ~~Bot-side Kimi endpoint / credential / rate-limit robustness~~
  → C0 + C1 (fail-fast LlmConfigValidator, bounded retry on 429 / 5xx
  / transport, secret-free startup describe line).
- ~~D11 cs014 live bot-loop UC drift~~ → C2 (strong-prior carry-forward
  policy in `ClassifyUseCaseTool`; D11 closed in 3/3 Sprint 3 runs).
- ~~cs002 UC drift while distress fires~~ → indirectly closed by C2;
  cs002 r1 stamps `user_distress` and routes UC-C in Sprint 3.

## Recommended next sprint direction

Recommended Sprint 4:

`cs001 / cs002 / cs029 spec-vs-runtime alignment`

Recommended Sprint 4 actions (3 narrow):

- E1. Decide whether cs001 / cs002 specs accept `user_distress` as a
  valid expected reason on personas that use distress phrasing, and
  apply via the approved CaseSpec override / audit path.
- E2. Decide cs029 outcome lift (D12): widen the spec to accept UC-D
  as a secondary OR add a sub-detector that picks UC-C when "messages
  / replies / inbox" appear in the soft-OOS user message.
- E3. Optional cs259 routing / stall stabilisation if the
  Sprint 3 baseline run-2 `trace_minimum` is not a one-off.

Do not expand smoke / anchor / promotion during Sprint 4. The
existing smoke surface is now stable enough for spec alignment.