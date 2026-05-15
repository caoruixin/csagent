# Sprint Objective

Date: 2026-05-05

## Sprint name

Prompt / Context Projection and Fix-Layer Diagnostic Sprint 5

## Goal

Stop defaulting every remaining eval failure to Java runtime fixes.

Classify the remaining smoke failures by the correct fix layer, identify prompt / context projection and skill orchestration candidates, and produce a small, evidence-backed implementation plan for the next sprint.

This sprint is diagnostic-first. It should not implement broad runtime changes.

## Baseline

Use the post-Sprint-4 canonical baseline:

`eval_interactive/results/20260504-221916/results.json`

Use this as nondeterminism reference:

`eval_interactive/results/20260504-223153/results.json`

## Implement only

### F0. Fix-layer taxonomy

Create or update:

`docs/fix_layer_taxonomy.md`

Classify each reviewed failure into exactly one primary layer and optional secondary layer:

- java_guard
- prompt_context_projection
- skill_orchestration
- case_spec_eval
- infra_runtime
- judge_calibration
- product_policy_gap
- unknown_needs_human_review

For each classification include:

- case id
- observed failure
- evidence path
- why this layer is primary
- why other layers should not be fixed first
- recommended minimal next action
- confidence: high / medium / low

### F1. Prompt / context projection audit

Inspect the prompt / AgentRunLoop / ContextProjection / PhasePlan surfaces.

Goal:

- Identify where the LLM lacks useful state or constraints.
- Identify cases where Java guard is already sufficient but prompt/context could improve answer quality, tool choice, or handover quality.
- Propose minimal prompt/context changes, but do not implement them unless they are tiny and explicitly scoped.

Output:

`docs/prompt_context_projection_audit.md`

Include:

- current prompt/context surfaces
- missing state / prior / phase goal / allowed-tool cues
- candidate changes
- risks
- target cases
- tests/evals needed before implementation

### F2. Skill orchestration candidate scan

Identify repeated multi-step flows that should become skills or plan templates.

Examples:

- FAQ search → grounded answer → unresolved check → handover
- payment/refund issue triage
- account/login recovery triage
- technical-regression intake
- moderation/trust-safety handover
- soft-OOS classification + safe fallback

Output:

`docs/skill_orchestration_candidates.md`

For each candidate:

- trigger conditions
- required tools
- required state
- terminal outcomes
- Java guard boundaries
- prompt responsibilities
- eval cases that should test it

### F3. Dual-layer design proposal

Create:

`docs/java_guard_prompt_flexibility_design.md`

Define:

- what Java must guarantee
- what prompt should guide
- what skill should orchestrate
- what eval should verify
- stop conditions for future implementation sprints

## Do not implement

- broad Java runtime redesign
- new deterministic routing taxonomy
- L3 judge stabilization
- broad anchor / exploration / promotion expansion as hard gates
- production GDPR / moderation / payment / scam suite expansion
- full trace / transcript alignment
- full service-outcome taxonomy
- broad prompt rewrite
- new skill runtime framework

## Target evidence

Review:

- smoke failures from the post-Sprint-4 baseline
- Sprint 4 nondeterminism reference
- selected anchor / exploration examples only as advisory evidence

Primary smoke cases:

- cs_interactive_176
- cs_interactive_259
- cs_interactive_192
- cs_interactive_004 if still relevant historically
- cs_interactive_011
- cs_interactive_066 variance
- session_create_failed / ReadTimeout cases

## Success metrics

- Every reviewed failure has a fix-layer classification.
- At least 3 prompt/context candidates are identified.
- At least 2 skill orchestration candidates are identified.
- Java-only fixes are recommended only for true invariants.
- Next implementation sprint can be scoped to 3–4 actions.
- No broad implementation is performed in this sprint.

## Review rule

Codex should review the diagnostic quality only.

Codex should not ask for broad implementation during this sprint unless the audit reveals a P0 safety or contract violation.