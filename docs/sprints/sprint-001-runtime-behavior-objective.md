# Sprint Objective

Date: 2026-05-04

## Sprint name

Targeted Runtime Behavior Sprint 1

## Goal

Move from evaluator-honesty fixes to runtime behavior fixes.

The previous rounds made the eval more honest. This sprint should improve the customer service agent runtime behavior itself.

## Implement only

1. Deterministic EscalationReasonResolver
2. UC-K technical regression routing
3. Server-side handover payload assembler

## Do not implement

- full claim classifier
- full trace / transcript alignment
- large new smoke suite expansion
- full service-outcome taxonomy
- broad rubric rewrite
- broad production coverage expansion

## Target cases

- `cs_interactive_002`
- `cs_interactive_014`
- `cs_interactive_029`
- `cs_interactive_066`
- `cs_interactive_095`
- `cs_interactive_259`

## Baseline

Use this as the current sprint baseline:

`eval_interactive/results/20260504-085942/results.json`

Use this only as a stability reference:

`eval_interactive/results/20260504-085359/results.json`

Use this only as historical round-5 baseline:

`eval_interactive/results/20260504-081853/results.json`

## Success metrics

Primary success metrics:

- `escalation_reason_consistency` failures: 1 → 0
- explicit callback / human request should beat budget reasons
- `cs_interactive_029` should not serialize `turn_budget_exhausted` as the semantic escalation reason when the user asked for a call
- `cs_interactive_066` should route to UC-K
- handover payload should include issue-specific summary and escalation reason
- generic handover summaries should fail or be replaced deterministically

Secondary success metrics:

- Smoke pass target: 6–8 / 14
- If raw pass count does not improve because of LLM nondeterminism, targeted blocker count must still improve.

## Review rule

The next Codex review must only check whether this sprint objective was met.

Codex should not perform a broad review of missing production cases, future groundedness work, or deferred rubric redesign unless it directly blocks A1, A2, or A3.