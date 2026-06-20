---
title: "M-Auto-9 product/eval_spec decision — escalate-after-genuine-help is a valid terminal; goal_impossible is not an auto-hard-fail"
doc_tier: durable-connective
status: current
implementation_status: implemented
source_of_truth: this file (the recorded product/eval_spec decision); the eval mechanics it governs live in eval_interactive/eval_interactive/scoring/conditional_outcome.py + hard_checks.py
last_reviewed: 2026-06-21
review_cadence: per milestone
supersedes: []
superseded_by: null
notes: >
  Recorded by the human (deliver session, 2026-06-21) as the M-Auto-9 product /
  eval_spec decision that gates WP2 (the satisfiable-companion sub-sprint, Sprint
  097 / S-Auto-45). It RESOLVES the open eval_spec question left in code at
  hard_checks.py:799 ("whether resolved+goal_impossible should itself be a hard
  fail"). The decision largely FORMALIZES the already-implemented stance
  (conditional_outcome.py S-Auto-40 WP1-A + hard_checks _TERMINAL_FAILURE_STOP_REASONS,
  which deliberately excludes goal_impossible) and adds the Sprint-096
  agent_unable_to_resolve honest-reason condition. It is a recorded decision, not
  a code change; no CaseSpec PRIMARY expected-outcome is widened by this record.
---

# M-Auto-9 product / eval_spec decision — escalate-after-help & goal_impossible

**Recorded:** 2026-06-21 (human, deliver session).
**Status:** DECISION (binding for M-Auto-9 eval interpretation; resolves the
`hard_checks.py:799` open `eval_spec` question).
**Gates:** WP2 (Sprint 097 / S-Auto-45 — satisfiable-companion sub-sprint).

## Context

M-Auto-7's registry-only pilot closed NO-KEEP because neither PRIMARY case
(`cs_uc_a_no_ad_id_ad_specific`, `cs_uc_a_loaded_listing`) reaches a recorded
SATISFIED+resolve terminal even when grounding is correct. The 2026-06-20
confirmation investigation found this is **dominated by an unsatisfiable-persona /
product question** — the simulator persona declares `goal_status=impossible` after
a reasonable grounded answer — **not** a proven shared phase-machine closure defect
(charter `docs/proposals/runtime-closure-record-outcome-design-milestone-oq-s93.1.md`
§"Why this milestone exists" + §6). The charter therefore makes "no phase-machine
change" a first-class valid outcome and asks for **a recorded product / `eval_spec`
decision** before WP2. This file is that record.

## The decision

### 1. Escalate-after-genuine-help is an accepted terminal

For the two existing **unsatisfiable** PRIMARY personas, a bot-initiated
escalation is an **accepted terminal** when **all** of:

1. the bot has provided **substantively grounded, relevant** help (not generic /
   superficial FAQ; for the entity-context UCs this means consulting and using the
   specific listing context, per each CaseSpec's `closure_criterion`);
2. the user remains **explicitly unresolved**;
3. **no** more-specific safety / compliance / payment / service / budget reason
   applies;
4. the bot-initiated handover uses the honest **`agent_unable_to_resolve`** reason
   (the 24th canonical `escalation_reason`, shipped Sprint 096 / S-Auto-44 —
   resolver priority 50, strictly lowest).

This is **not** an unconditional resolve-OR-escalate widen: early / lazy /
over-escalation with **no** post-help positively-UNRESOLVED signal still FAILs.
This is the stance already encoded declaratively by
`conditional_outcome.py` (S-Auto-40 WP1-A): SATISFIED → `resolve`; positively
UNRESOLVED after closure-qualified grounded help → escalate is
`CONDITIONAL_ELIGIBLE` (never auto-PASS until a versioned per-trace adjudication
artifact accepts it).

### 2. Do not force RESOLVE to pass a metric

The two existing PRIMARY personas must **not** be forced into a RESOLVE terminal
merely to make a closure metric pass. A recorded resolve is earned **only when the
user is genuinely satisfied** (authoritative `user_state` = SATISFIED). Forcing a
false resolve on a dissatisfied/impossible persona is the failure this decision
exists to prevent, and is fenced by the premature-resolve guard +
escalate-after-help path.

### 3. `goal_impossible` alone is not an automatic hard-fail

`goal_status=impossible` is a **simulator-side** signal that the RUNTIME never
observes; it can reflect a hard-to-satisfy persona rather than a bot fault. It is
therefore **not** sufficient, by itself, to hard-fail a trace. This **resolves the
open `eval_spec` question** recorded at
`eval_interactive/eval_interactive/scoring/hard_checks.py:799`
(`goal_impossible` is already excluded from `_TERMINAL_FAILURE_STOP_REASONS`,
lines 787–800) — the resolution is to **keep that exclusion**. False resolve is
judged on **authoritative `user_state` + closure evidence**, not on the simulator
giving up:

- `resolved` + authoritative `user_state` UNRESOLVED / unmet closure ⇒ **FAIL**
  (the existing zero-evidence / contradicted-stamp rules, OQ-S77 #2/#3, still bite);
- simulator `goal_impossible` **without** contradictory user-state / closure
  evidence is **not** sufficient by itself to hard-fail;
- **conflicting signals** (e.g. `user_state=SATISFIED` but `goal_impossible`, or
  vice-versa) are recorded as an **eval inconsistency** to investigate, **not**
  resolved through a runtime heuristic.

## What this decision does NOT do

- It does **not** widen or modify any existing CaseSpec's expected outcome or
  expected reason (no PRIMARY bar is changed by this record).
- It does **not** authorize any phase-machine / `record_outcome` / premature-guard
  / `source_ids` change. WP0 stays HELD.
- It does **not** itself prove a resolve terminal can land — that is what the WP2
  **satisfiable companion** demonstrates (Sprint 097 / S-Auto-45).

## Code anchors (source of truth for the mechanics)

- `eval_interactive/eval_interactive/scoring/conditional_outcome.py` — the
  declarative three-state (SATISFIED / UNRESOLVED / UNKNOWN) conditional outcome
  acceptance (S-Auto-40 WP1-A); reads `trace.user_state_signals`; never
  back-inferred; CONDITIONAL_ELIGIBLE never auto-PASS.
- `eval_interactive/eval_interactive/scoring/hard_checks.py:787–800` —
  `_TERMINAL_FAILURE_STOP_REASONS` (excludes `goal_impossible`) + the `:799`
  open-question comment this decision resolves.
- `eval_interactive/case_specs/bad_cases/cs_uc_a_loaded_listing.yaml` +
  `cs_uc_a_no_ad_id_ad_specific.yaml` — the two PRIMARY personas
  (`conditional_outcome_acceptance` blocks); unchanged by this decision.
- `EscalationReasonResolver` / `agent_unable_to_resolve` — Sprint 096 / S-Auto-44
  (`docs/sprints/sprint-096-handoff.md`).
