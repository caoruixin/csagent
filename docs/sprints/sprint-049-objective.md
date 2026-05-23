---
title: Sprint 49 / S-Cleanup-3 — Tier-2 phase-plan-scoped fix (#9) + handover_completeness demotion (#4)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file (S-Cleanup-3 sub-sprint contract archive)
last_reviewed: 2026-05-24
review_cadence: ad hoc
supersedes: [docs/sprints/sprint-048-objective.md]
superseded_by: docs/sprint_objective.md (next sub-sprint TBD)
notes: >
  Third (and per current milestone plan, LAST) sub-sprint of Milestone
  M4-Eval-Cleanup. Highest-risk sub-sprint. Layer: `judge_calibration`
  (Tier-2 wiring #9) + `eval_spec` (handover_completeness demotion #4).
  §7 stanza required. Codex per-sub-sprint review RECOMMENDED (§4.3
  trigger #2 — touches the case_passed gate) — deliver-agent + human
  confirm at launch.

  #9 was promoted from "design decision to document" to "confirmed
  Tier-2 misflip bug fix" by the Tier-2 design memo
  (`docs/solutions/tier2_skill_traversal_design_memo.md`, 2026-05-23).
  Human picked Option (b) phase-plan-scoped evaluation. The fix is
  eval-side only (no `server/` touch; the trace already carries
  `per_turn_trace[].phase_plan.critical_steps[].id`).
---

# Sprint 49 / S-Cleanup-3 — Tier-2 phase-plan-scoped fix (#9) + handover_completeness demotion (#4)

## 1. Sprint identity

- **Sprint number**: 49 (global) / S-Cleanup-3 (M4-Eval-Cleanup sub-
  sprint 3; LAST per current milestone plan).
- **Milestone**: M4-Eval-Cleanup (`docs/milestone_objective.md`).
- **Branch**: `refactor/remove-the-shackles` (continuing).
- **HEAD prior to dev**: `8f32dbd` (S-Cleanup-2 close bundle + governance
  consolidation on top of dev `b989833`).
- **Estimated duration**: 2-3 dev days (highest-risk sub-sprint).

## 2. Goal

Fix the confirmed Tier-2 `skill_procedure_followship` misflip (#9) by
scoping evaluation to the critical-steps the runtime actually presented
(Option (b) per `docs/solutions/tier2_skill_traversal_design_memo.md`),
and demote `handover_completeness` / `case_id_present` from mandatory-on-
escalate to advisory Tier-3 (#4) per the M3-Eval four-tier pyramid. Both
are eval-side; no `server/` change.

This is the LAST M4-Eval-Cleanup sub-sprint; on close, the milestone goes
to its close round (Codex milestone-shared review if per-sub-sprint was
deferred + bad-case suite regression-safety rerun + milestone close
artefacts).

## 3. Layer-classification + anti-hardcode stanza (per §7)

**Target failure layer**: `judge_calibration` (primary — Tier-2 wiring
#9: the gate's stability / correctness on the same prompt + CaseSpec) +
`eval_spec` (secondary — #4 handover_completeness / case_id_present
mandatory→advisory demotion).

**Tier-0 invariant**: This sprint adds no Tier-0 invariant. Existing
Tier-0 invariants in `docs/runtime_freeze_and_risk_policy.md` §1 / §2
unchanged. The fix tightens an eval-side gate's correctness; it does NOT
touch a runtime safety floor.

**Semantic hardcode**: No semantic hardcode introduced. (a) #9 scopes
Tier-2 evaluation by the set of `critical_steps[].id` the runtime emitted
into `per_turn_trace[].phase_plan.critical_steps` — observable trace
state, NOT a keyword / regex / per-UC matrix. The step ids come from the
runtime's own per-turn projection (single-source-of-truth
`critical_steps` per M3-Eval). (b) #4 REMOVES a mandatory gate (demotion
to advisory) — it shrinks, not expands, the deterministic surface; no
hardcode added.

**Generalization coverage**: target = the confirmed misflip — a UC-A
FAQ-resolve session (no escalation) must NOT be flipped by the escalate
Skill's mandatory step (post-fix: `escalate-via-request-handover` is not
in any turn's `phase_plan.critical_steps`, so it is not evaluated).
Neighbor = a UC-A escalation session (enters ESCALATE; the escalate step
IS presented and SHOULD be evaluated); a UC-FP resolve session.
Negative = a legitimately multi-phase session that enters BOTH a resolve
phase and an escalate phase must still evaluate both Skills' presented
steps (the fix must not over-narrow). Shadow = N/A (this is eval-harness
gate logic, not a bot semantic surface; no held-out bot-behaviour cases
apply).

## 4. Files in scope

| # | Item | Files | Expected change | Complexity |
|---|------|-------|-----------------|---|
| (step 1) | Empirical pre-fix confirmation | (no file edit) run `eval-interactive run --set anchor --parallel 1`; record how many of 159 anchor cases the current full-Skill traversal misflips (Tier-2 critical FAIL on the escalate-vs-resolve mandatory pair on a non-matching-path case) | Measurement only; record in handoff §6 as before-numbers | LOW (run + jq) |
| 9 | Tier-2 phase-plan-scoped evaluation | `eval_interactive/eval_interactive/batch/executor.py` (`_compute_tier2_result` lines 365-392); possibly a helper in `eval_interactive/eval_interactive/scoring/skill_procedure_check.py` | Collect the set of `critical_steps[].id` present in `per_turn_trace[].phase_plan.critical_steps` across all turns; evaluate a loaded Skill's step ONLY if its `id` is in that set (else `N/A`). Defensive default: if NO turn carries `phase_plan.critical_steps` (older / error / empty traces), evaluate NOTHING (Tier-2 inert PASS/advisory) rather than fall back to all-Skills — preserves the S-Eval-2 empty-list backward-compat default. | MEDIUM (gate-logic change; defensive defaults) |
| 9 | Tier-2 fix tests | NEW `eval_interactive/tests/test_tier2_phase_plan_scoping.py` (or extend existing Tier-2 test) | target / neighbor / negative per §3: resolve-no-escalate (escalate step NOT evaluated), escalate session (escalate step IS evaluated), multi-phase session (both evaluated), empty-phase_plan trace (inert default). | MEDIUM |
| (step 4) | Empirical post-fix confirmation | (no file edit) re-run `eval-interactive run --set anchor --parallel 1`; show the misflip resolved (before/after delta) | Measurement only; record in handoff §6 as after-numbers | LOW |
| 4 | handover_completeness / case_id_present demotion | `eval_interactive/eval_interactive/scoring/composite.py` (lines ~69, 81, 84 — `_conditional_mandatory_l2`) | Remove `handover_completeness` + `case_id_present` from the `outcome_class == "escalate"` mandatory-L2 path; they become advisory Tier-3 per the M3-Eval pyramid (recorded, not gating). Add / update unit test asserting they no longer flip `case_passed` on an escalate case. | LOW-MEDIUM |
| 4 | governance text (CONDITIONAL) | `docs/current/iteration_governance.md` §5.6 OR a handoff note ONLY | If a governance text clarification is needed to record that handover_completeness / case_id_present are Tier-3 advisory, keep it MINIMAL (1-2 sentences) and Codex MUST verify per §4.3. PREFER a handoff note + the M4-Eval-Cleanup close artefact over a §5.6 edit unless the edit is clearly load-bearing. | LOW (or none) |

## 5. Files NOT in scope (hard fences)

- `server/src/main/java/**` — no runtime code touch. The #9 fix is
  eval-side; the trace already carries `phase_plan.critical_steps`.
- `server/src/main/resources/skills/*.yaml` — no Skill YAML edits. The
  `critical_steps` (incl. `escalate-via-request-handover` mandatory_for
  list) stay AS-IS; the fix is in how the eval side CONSUMES them, not in
  the declarations. (Do NOT "fix" the misflip by editing `mandatory_for`
  — that would be a Skill-contract change with runtime implications.)
- `server/src/main/resources/system_prompt.txt` — no edits.
- `docs/runtime_freeze_and_risk_policy.md` — no edits.
- `docs/current/doc_governance.md` / `agent_context_guide.md` — no edits.
- `docs/sprints/sprint-NNN-*` — immutable archives (only NEW
  `sprint-049-handoff.md`).
- `docs/milestones/*` — immutable archives.
- `docs/10-handoff.md` / `docs/action_bank.md` / `docs/codex-findings.md`
  / `docs/milestone_objective.md` — deliver-agent / review-agent
  territory.
- `eval_interactive/case_specs/**` — no fixture edits (this is a gate-
  logic fix, not a fixture change). The Alice / bad_cases / anchor
  fixtures stay as-is.
- `eval_interactive/case_specs/shadow/` — held-out, dev-blind.
- `eval_interactive/eval_interactive/scoring/skill_procedure_check.py`
  `extract()` per-Skill logic — may add a helper but do NOT change the
  per-step N/A-by-mandatory_for semantics (the #9 fix is at the
  executor-orchestration layer: which steps get fed to the extractor,
  not how a single step is evaluated).
- `eval_interactive/eval_interactive/batch/sets.py` `_OPT_IN_SETS` /
  `is_human_judgment_suite` — S-Cleanup-1/2 surface; no change needed.

## 6. Success metrics

- **#9 empirical (before/after)**: anchor suite (`--set anchor
  --parallel 1`) misflip count recorded pre-fix (step 1) and post-fix
  (step 4); the escalate-vs-resolve mandatory-pair misflip is resolved
  (post-fix count = 0 spurious flips of that shape). Record both numbers
  + the jq extraction command in handoff §6.
- **#9 unit tests**: target / neighbor / negative per §3 all PASS
  (resolve-no-escalate → escalate step N/A; escalate session → escalate
  step evaluated; multi-phase → both evaluated; empty-phase_plan → inert).
- **#4 demotion verified**: `grep -n "handover_completeness\|case_id_present"
  eval_interactive/eval_interactive/scoring/composite.py` confirms neither
  is added to `_conditional_mandatory_l2` on the escalate path; a unit
  test asserts an escalate case with missing handover_completeness does
  NOT flip `case_passed`.
- **Java baseline unchanged** — `1163 / 1-inherited / 0 / 2` (no server/
  touch).
- **Python baseline preserved or improved** — no NEW failures; NEW Tier-2
  + #4 tests PASS.
- **Bad-case suite regression-safety** — `eval-interactive run --set
  bad_cases --parallel 1` distribution qualitatively unchanged (the
  bad-case suite is human_review authority, so the #9 fix should not
  change closure_criterion judgments; confirm no crash / contract change).
- **Codex per-sub-sprint review** (if dispatched per §8) returns `pass`.
- **Dev handoff** at `docs/sprints/sprint-049-handoff.md` §1-§11 complete;
  §12 reserved for deliver-agent + human close classification.

## 7. Stop conditions

- **If step-1 empirical confirmation shows NO misflip on the anchor
  suite** (contradicting the offline reproduction), STOP and surface —
  the offline repro may not reflect the live executor path (e.g.,
  `active_use_case` is threaded differently in production runs); re-
  examine before changing the gate.
- **If the #9 fix requires the trace to carry data it does not** (e.g.,
  `phase_plan.critical_steps` is empty on real runs, not just `{id,
  desc}`), STOP — Option (b) feasibility assumed the trace carries
  presented step ids; if it does not, the fix may need server-side trace
  enrichment which crosses the §5 hard fence → re-scope.
- **If #9 over-narrows** (a legitimately multi-phase session stops
  evaluating a step the runtime DID present), STOP — the negative-control
  test must pass before close.
- **If #4 demotion reveals `handover_completeness` / `case_id_present`
  are load-bearing for a Tier-0 safety invariant** (not just a Tier-3
  advisory), STOP and surface — demotion must not weaken a safety floor.
- **If the governance text edit for #4 grows beyond 1-2 sentences** or
  touches §5.5 / §1.7, STOP — governance rewrites belong in a separate
  fold-back commit per `doc_governance.md`, and Codex must verify.

## 8. Codex review plan

**RECOMMENDED: per-sub-sprint Codex review** (deliver-agent + human
confirm at launch). Rationale: #9 changes the logic of the Tier-2 gate
that decides `case_passed` — a `judge_calibration`-layer change on a
scoring surface (§4.3 trigger #2: "touches a semantic-decision /
scoring surface"). Even though it is a bug fix (not a hardcode), the
anti-hardcode kernel (§4.1) + the §1.7 boundary check should verify the
fix scopes by observable trace state and does not encode a per-UC matrix.
If deliver-agent + human prefer, Codex MAY be deferred to milestone close
per §4.3 default — but given this is the gate that decides case_passed,
per-sub-sprint review is advised.

## 9. Generalization coverage stanza

See §3 stanza. target / neighbor / negative coverage via the NEW Tier-2
unit tests + the anchor-suite before/after empirical confirmation. Shadow
= N/A (eval-harness gate logic, not a bot semantic surface).

## 10. Bundle policy

- Dev stages ONLY S3-scope files (per `iteration_governance.md` §8.7).
- Deliver-agent files (milestone close artefacts) bundled by human at
  close.
- Dev MUST NOT `git add -A`; enumerate staged files.
- If the §4 row "governance text (CONDITIONAL)" edit is made, dev flags
  it explicitly in the handoff for Codex verification.

## 11. Handoff schema

Dev produces `docs/sprints/sprint-049-handoff.md` with sections (per
S-Cleanup-1/2 convention):

1. Identity. 2. Scope landed (numstat). 3. §3 stanza + §4.1 self-walk.
4. Java baseline. 5. Python baseline. 6. Per-item verification +
   **#9 before/after anchor-suite empirical numbers** + #4 demotion grep.
7. #9 negative-control evidence (multi-phase session still evaluates
   both Skills). 8. OQs surfaced. 9. Drift items. 10. Hard fence honored
   checklist. 11. R-item flip request (none expected unless the #9 fix
   closes an R-item). 12. Closure verdict deferred.

## 12. Closure verdict (deferred to sub-sprint close)

To be appended at S-Cleanup-3 close by deliver-agent + human jointly.
Reserved. On S-Cleanup-3 close, M4-Eval-Cleanup goes to milestone close.
