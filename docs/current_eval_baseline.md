# Current Eval Baseline

Date: 2026-05-04 (post Sprint 1 — runtime behaviour)

## Purpose

This file freezes the canonical result for the next targeted runtime-behaviour sprint.

The next sprint should compare new results against the **post-Sprint-1**
result, not against the previous round-6 baseline.

## Current sprint baseline (post Sprint 1)

Canonical current result:

`eval_interactive/results/20260504-100538/results.json`

This is the canonical Sprint 1 result (smoke-20260504-1805) — first run after
A1/A2/A3 landed.

Pass rate:

`6/14`

Mean composite:

`0.3633`

Mean outcome:

`0.7590`

Mean judge:

`0.6619`

### Stability reference runs

Two follow-up runs to characterise LLM nondeterminism:

- `eval_interactive/results/20260504-101453/results.json` (4/14, mean composite 0.2414)
- `eval_interactive/results/20260504-102131/results.json` (5/14, mean composite 0.2861)

Cross-run targeted blocker stability:

- `L1:escalation_reason_consistency`: **0/0/0** across all three runs (was 1 at baseline) ✓
- `L2:handover_completeness`: 0 / 2 / 0 (handover assembler now stamps an
  issue-specific summary; the run-2 misses are due to a downstream LLM
  variance where the `/v1/demo/handover-logs` call returned no entry for
  the session — pre-existing flake, not caused by Sprint 1)
- cs_066 routes to UC-K: passes in **all three runs** ✓

### Pre-Sprint-1 reference

Round-6 final result (pre-sprint baseline):

`eval_interactive/results/20260504-085942/results.json`

(4/14 passed, mean composite 0.2415, 1× `escalation_reason_consistency`
failure on cs_029.)

Use this only when comparing the Sprint 1 outcome against the prior
round-6 ceiling.

## Known nondeterminism

- `cs_interactive_001` and `cs_interactive_011` can rotate.
- `cs_interactive_192` turn-0 source citation can appear or disappear.
- `cs_interactive_259` stall shape changes across runs.
- `cs_interactive_002` / `cs_interactive_014` LLM routing flips between
  UC-B / UC-C / UC-F across runs (separate from Sprint 1).

## Current target cases (next sprint candidates)

The next sprint should focus on cases still failing across runs:

- `cs_interactive_002` (UC-C, user_distress)
- `cs_interactive_004` (over-escalation on FAQ)
- `cs_interactive_014` (UC-C, user_distress)
- `cs_interactive_015` (UC-FP routing)
- `cs_interactive_029` (soft-OOS UNKNOWN topic + immediate escalation)
- `cs_interactive_095` (UC-A resolve, not escalate)
- `cs_interactive_192` (turn-0 grounding + handover persistence flake)
- `cs_interactive_259` (UC-F payment FAQ resolve)

## Known current blocker patterns (post Sprint 1)

1. Distress detection — bot stamps `faq_miss_threshold_exceeded` where
   the spec expects `user_distress`. Resolver precedence already
   prefers `user_distress`; the runtime needs a detector that *sets*
   it. (Not in Sprint 1.)
2. Pre-LLM routing stability for "Replies & Messaging" — UC-B / UC-C /
   UC-F flips between runs.
3. cs_029 soft-OOS / UNKNOWN-topic + early-escalation interaction.
4. LLM-judge `relevance` / `tone_appropriateness` volatility.
