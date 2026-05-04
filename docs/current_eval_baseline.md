# Current Eval Baseline

Date: 2026-05-04 (post Sprint 2 — runtime behaviour)

## Purpose

This file freezes the canonical result for the next targeted runtime-behaviour sprint.

The next sprint should compare new results against the **post-Sprint-2**
result, not against the previous post-Sprint-1 baseline.

## Current sprint baseline (post Sprint 2)

Canonical current result:

`eval_interactive/results/20260504-151311/results.json`

This is the canonical Sprint 2 result (smoke-sprint2-1) — first run after
B0/B1/B2/B3 landed.

Pass rate:

`7/14`

Mean composite:

`0.4015`

Mean outcome:

`0.7590`

Mean judge:

`0.6524`

### Stability reference run

One follow-up run to characterise LLM nondeterminism:

- `eval_interactive/results/20260504-151839/results.json` (4/14, mean composite 0.2323)

Cross-run targeted blocker stability for the Sprint 2 contracts:

- `L1:escalation_reason_consistency`: **0 / 0** across both runs (sprint 1 contract preserved) ✓
- `CONTRACT_VIOLATION:active_use_case`: **0 / 0** across both runs (was 1 in Sprint 1; B3 fixed) ✓
- cs_066 routes to UC-K: passes in **both runs** ✓
- cs_095 routes to UC-A (NOT UC-K): passes in **both runs** ✓
- cs_029 stamps `user_requested` (semantic) + has UC-D (fallback): passes in **both runs** ✓

The run-2 raw pass count drop (4/14) is bot-side LLM rate-limit noise
(TIMEOUTs on cs_001 / cs_002 / cs_014) — not a sprint regression.

### Pre-Sprint-2 reference

Sprint 1 final result (pre-Sprint-2 baseline):

`eval_interactive/results/20260504-100538/results.json`

(6/14 passed, mean composite 0.3633.) Use this only when comparing
the Sprint 2 outcome against the Sprint 1 ceiling.

## Known nondeterminism

- `cs_interactive_001`, `cs_interactive_002`, `cs_interactive_014`
  can TIMEOUT under bot-side LLM rate limiting.
- `cs_interactive_011` LLM routing can rotate between runs.
- `cs_interactive_192` turn-0 source citation can appear or disappear.
- `cs_interactive_259` stall shape changes across runs.
- `cs_interactive_066` can stall on bot-side LLM intent-without-tool
  flake even when UC-K is correctly committed.

## Current target cases (next sprint candidates)

The next sprint should focus on cases still failing across runs:

- `cs_interactive_001` — bot-side LLM TIMEOUT robustness
- `cs_interactive_004` — over-escalation on FAQ
- `cs_interactive_011` — UC routing flip
- `cs_interactive_014` — cross-turn UC drift away from UC-C
- `cs_interactive_015` — UC-FP routing
- `cs_interactive_029` — outcome lift (contract is green; L2 still
  fails because UC-D fallback ≠ spec UC-C)
- `cs_interactive_192` — turn-0 grounding + handover persistence flake
- `cs_interactive_259` — UC-F payment FAQ resolve

## Known current blocker patterns (post Sprint 2)

1. Bot-side LLM rate limiting / timeouts on cs_001 / cs_002 / cs_014.
   Routing structure is deterministic; outcome variance comes from
   upstream API.
2. `cs_014` per-turn UC drift away from UC-C (initial route is
   UC-C via strong-prior, but the conversation flow flips to UC-B in
   the bot-turn agent loop).
3. `cs_029` outcome alignment — contract gate green; L2 correct_uc
   gate fails because the deterministic UC-D fallback is reported
   against a spec primary UC-C.
4. LLM-judge `relevance` / `tone_appropriateness` volatility (out of
   Sprint 2 scope; explicitly excluded).
