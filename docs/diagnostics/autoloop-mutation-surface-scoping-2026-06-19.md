---
title: "Autoloop mutation-surface scoping analysis (UC-A entity-grounding)"
doc_tier: diagnostic
status: diagnostic
implementation_status: historical
source_of_truth: server skill YAMLs + ContextProjectionBuilder / skill_procedure_check (read-only)
last_reviewed: 2026-06-19
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Read-only surface analysis requested after S-Auto-41 exp-88/exp-89 (both steered
  candidates ON_TARGET but discarded at tier0). Compares the 4 mutable registry
  fields and their runtime/eval consumers, identifies the narrowest legitimate
  surface for the UC-A entity-context / listing-grounding behaviour, and corrects
  the exp-88/89 attribution. NO code change; NO real iteration; holds in place.
---

# Autoloop mutation-surface scoping analysis (UC-A entity-grounding)

Read-only. Goal: find the **narrowest legitimate surface** for the UC-A
listing-grounding behaviour that cannot suppress trust-and-safety escalation,
without assuming a `critical_steps[*].desc` answer in advance.

## 1. The 4 mutable fields and their consumers

Mutable surface (`config.pilot-s-auto-38.yaml:mutable_surface`): `$.procedure`,
`$.grounding_instruction`, `$.escalation_policy`, `$.critical_steps[*].desc`,
across 6 skills. For UC-A the relevant skill is
`resolve_faq_grounded_answer.yaml` (`applicable_use_cases: UC-A,B,C,D,E,F,FP`;
`applicable_phases: RESOLVE`).

| field | runtime consumer | eval consumer | semantic footprint |
|---|---|---|---|
| `$.procedure` | folded into `PhasePlan.systemInstruction` (PhaseEvaluator.plan) → the main per-turn teaching prose for the active skill | — | **broadest**: frames resolve-vs-escalate *posture* + grounding + tool flow together |
| `$.grounding_instruction` | `PhasePlan.groundingInstruction` → projected as `grounding_instruction` (ContextProjectionBuilder ~L1051) | — | **narrowest (semantic)**: dedicated "how to ground" field; **escalation-free**; already entity-aware ("If tool data contains specific information about the user's case … answer from that first") |
| `$.escalation_policy` | `PhasePlan.escalationPolicy` → projected as `escalation_policy` | — | governs escalation precedence — **protect, do not edit for grounding** |
| `$.critical_steps[*].desc` | **ALL** steps' `desc` projected when the skill is active — **NOT filtered by `mandatory_for`** (ContextProjectionBuilder ~L1071-1083) | skill_procedure_check grades a step ONLY when `active_use_case ∈ step.mandatory_for` (L677) | runtime footprint = **all** of the skill's FAQ UCs; `mandatory_for` scopes only *grading* |

## 2. Blast radius (structural)

The runtime projects only the **active** skill, selected by `(phase, useCase)`
(`skillRegistry.select(plan.phase(), plan.useCase())`). Therefore **any** edit to
**any** `resolve_faq_grounded_answer` field reaches exactly the FAQ use-cases it
serves — **UC-A, UC-B, UC-C, UC-D, UC-E, UC-F, UC-FP**, and only in the RESOLVE
phase when that skill is active. It **cannot** reach the intake/escalate UCs
(**UC-G/H/I/J/K**), which route to different skills.

## 3. Two key results (against assuming `critical_steps[*].desc`)

- **A new UC-A-only `critical_step` is OUTSIDE the mutable surface.** Adding a
  step changes non-whitelisted paths (`$.critical_steps[N].id`,
  `…mandatory_for`) — additions are reported as structural changes and rejected
  by `yaml_diff_validator`. Only editing an **existing** `…[N].desc` is allowed.
- **Editing an existing `critical_steps[N].desc` gives NO runtime UC-scoping.**
  `resolve_faq` has no UC-A-only step (steps 0–2 are `mandatory_for` all FAQ UCs;
  3–4 are UC-FP). And because projection ignores `mandatory_for`, even a
  hypothetically UC-A-scoped step's `desc` would be shown to the LLM on every FAQ
  UC. So `critical_steps[*].desc` is **not** a narrower *runtime* surface than
  `$.procedure`; `mandatory_for` only narrows tier2 *grading*.

⇒ The narrowest legitimate surface is **`$.grounding_instruction`** — narrowed
**semantically**, not by UC: it is the grounding-dedicated, escalation-free field,
so strengthening "make substantive use of listing-specific fields (status /
category / price / location / posted_date) from tool-returned OR pre-loaded
customer context" there does **not** touch the resolve-vs-escalate posture that
`$.procedure` conflates. (Cluster 1, UC-A→UC-B *misclassification*, is a DISCOVER
classification concern — a separate surface in `discover_triage`, out of scope
for the listing-grounding edit.)

## 4. Correction to the exp-88/89 attribution

Both exp-88 + exp-89 were discarded at tier0 on
`escalation_compliance@cs38s01_uc_j_scam_seller_full_narrative`. This is most
plausibly **flakiness / tier0 mis-attribution, NOT cross-UC bleed from the
grounding edit**:

- `cs38s01` is **UC-J (intake path)** — a `resolve_faq` edit **cannot
  structurally reach it** (§2).
- `cs38s01` is a **near-coinflip flaky shadow case** (frozen baseline
  `pass_rate=0.545`, `flaky=True`, **0/11** escalation_compliance fails sampled).
- Across the 4 resolve_faq-editing candidates its tier0 flipped
  clean/clean/fail/fail (exp-86/87/88/89). tier0 is zero-tolerance, so one flaky
  escalation fail on a near-coinflip shadow case discards the candidate.

⇒ **Two distinct blockers**, not one:
- **(a) surface semantic scoping** — `$.procedure` conflates grounding +
  escalation posture → editing it for grounding risks **in-skill** bleed to the
  other FAQ UCs (exp-87's tier1 `cs01s01` UC-C flip is the plausible in-scope
  example). Narrowing to `$.grounding_instruction` mitigates this.
- **(b) tier0 flake-sensitivity** — near-coinflip shadow cases (cs38s01) can trip
  the zero-tolerance tier0 gate regardless of surface, masking the PRIMARY signal.
  Overlaps `R-autoloop-fitness-measurement-reliability`. **Narrowing the surface
  does NOT fix (b)** — cs38s01 is unreachable by any resolve_faq edit.

**Unproven:** because both runs short-circuited at Layer 0, the grounding edits are
NOT shown to improve the PRIMARY.

## 5. Implications for the next (dry-run-only) proposal

A scoped proposal targeting `$.grounding_instruction` could state, per the 6
required disclosures: (1) PRIMARY mechanism = cluster-2 superficial/ignored
listing context; (2) narrower than `$.procedure` because grounding_instruction is
escalation-free and grounding-dedicated (no resolve-vs-escalate posture); (3)
blast radius = resolve_faq's FAQ UCs (A/B/C/D/E/F/FP), structurally not UC-G–K;
(4) trust-and-safety preservation = does not touch `$.escalation_policy` or any
escalation language, escalation precedence unchanged; (5) expected PRIMARY trace
change = answer references specific listing fields substantively / asks for the
ad reference when absent; (6) no benchmark case names / fixed ad IDs / test
answers (generic durable CS prose).

**But** this does not address blocker (b). A surface-scoped dry-run validates (a);
the tier0 flake-sensitivity on shadow near-coinflip cases is the dominant
empirical blocker and likely needs a measurement-reliability decision first.
HELD for human review before any further real iteration.
