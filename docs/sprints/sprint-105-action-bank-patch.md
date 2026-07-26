---
title: "Sprint 105 — drafted action_bank patch (not yet applied)"
doc_tier: sprint-archive
status: proposal
implementation_status: not_started
source_of_truth: docs/sprints/sprint-105-handoff.md §7 (the findings) + this file (the proposed ledger text)
last_reviewed: 2026-07-26
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Drafted 2026-07-26 while closing Sprint 105 and deliberately NOT applied to
  docs/action_bank.md. The handoff §10 refers to this as "held in the session
  scratchpad"; it was moved here so it survives the session. Intended consumer
  is the deliver agent, once the perf-replan wave's branches have merged, as
  ONE section covering the whole wave rather than four conflicting appends.
---

# action_bank patch — Sprint 105 surfaced items (DRAFT, do not commit yet)

**Status:** draft, held in scratchpad on purpose. **Do not paste into
`docs/action_bank.md` from a worktree session.**

**Why it is held.** Four sprints (104–107) are running concurrently in
separate worktrees and `docs/action_bank.md` is a single 195 KB file at the
repo root. Four sessions appending to §5 independently produce four
conflicting versions of the same region. The per-sprint handoffs under
`docs/sprints/` are already the authoritative, immutable record of what each
sprint surfaced; the ledger only needs one write. **Intended write point:**
the deliver agent, once the four branches merge back to
`perf-replan-2026-07`, as a single section covering the whole wave.

**Two things to fix while pasting:**

1. **Sprint-number collision.** `action_bank.md:814` plans M-Auto-12 WP2 as
   "Sprint 103 / S-Auto-51", while the perf-replan wave has already spent
   Sprint 103 (WS-6-A) and 104–107. The two planning lines double-book
   103–107. Disambiguate every row below as *perf-replan wave* with its
   branch name, or the ledger will read as if one series produced both.
2. This section is written for §5.2 (eval governance / non-runtime backlog),
   which is where the comparable "Sprint NNN surfaced OQs" sections live.

---

## Proposed section text

### perf-replan 2026-07 wave — Sprint 105 surfaced items (2026-07-26)

Sprint 105 (`sprint-105-eval-verdict`, worktree `../csagent-wt-105`; handoff
`docs/sprints/sprint-105-handoff.md`) delivered WS-4 + the Loop C fix and
recorded eight found-but-out-of-contract defects in its §7. Dispositions:

- **CLOSED in-sprint (commit `050a98c0`)** — handoff §7 items 6 + 7. The
  four placeholder row builders (`_timeout_result` / `_cancelled_result` /
  `_error_result` / `_contract_violation_result`) now emit
  `contract_warnings`, so a consumer subscripting the key no longer raises
  `KeyError` on exactly the rows where something went wrong; the
  contract-violation row also carries `severity` on its synthetic L1 entry.
  The spec-load `escalation_trigger`-is-None advisory now names its case id
  and path (the five copies are `anchor_outcome_uc_{g,h,i,j,k}`). No ledger
  row needed.

- **`R-eval-results-json-trace-field-loss` — NEW, open, LOW priority, layer
  `infra`.** `results.json` drops `TraceData.events` and `TraceData.handover`
  entirely and keeps only 2 of 10 `TurnTrace` fields
  (`_build_per_turn_trace` keeps `tool_calls` / `phase_plan` / `projection`;
  `turn_index`, `user_message`, `bot_response`, `source_ids`, `phase_before`,
  `phase_after`, `active_use_case`, `latency_ms` are lost). Consequence:
  offline re-scoring is possible for the composite layer only; L1 checks that
  read session events and L2 `handover_completeness` cannot be recomputed.
  Sprint 105 fixed the one load-bearing gap (`severity` on L1/L2 results,
  commit `d48057d6`), which is why its re-score of 17/17 cases reproduces the
  recorded verdicts exactly. **§5.8 judgement — deliberately NOT filed as a
  failure brief:** the layer is `infra` and the scope IS the eval framework,
  which is the §5.8 preemption trigger, but the defect **does not corrupt any
  measurement that has been taken** — it limits what can be recomputed later.
  Filing it as a brief would freeze every semantic sub-sprint in the wave and
  block §5.6 reruns for no gain. Record this judgement with the item; the
  next reader will otherwise re-litigate it.

- **`OQ-S105.premature-finish-rubric` — NEW, open, deferred, layer
  `judge_calibration`.** The `premature_finish` L3 rubric credits any
  handover as a proper ending ("5 = … fully resolved **or properly
  escalated**", with no test of "properly"). Measured 2026-07-26 on all 17
  recorded sessions (34 live judge calls, 0 fallbacks): it scores **5.0 on
  both negative controls** — the zero-retrieval premature handovers it exists
  to catch — and **1.0 on the two sessions that died on a backend 500**,
  which the bot never ended. A tightened rubric fixes both (negative controls
  stable 1.0 over 3 draws; infra failures 5.0) but leaves one genuinely gray
  case flipping `[1,1,5]`, i.e. a reasonable observation and a poor gate.
  **Not urgent:** the failure mode is already caught deterministically at L1
  by `no_premature_escalation` (WS-1), which hard-zeroes the composite. See
  handoff §7 item 1 + §8 item 8 for the build-and-revert record.

- **Corpus configures neither gating L3 dim (0 of 486) — closed as an
  observation, NOT an action.** Sprint 105 first reported this as the
  highest-value follow-up and then withdrew it: with Loop C fixed, advisory
  dims act as a fallback signal, and enabling the two gating dims as an
  always-on floor was measured to make the ruler **looser** (S-Eval-5's
  advisory-exclusion rule turns from tie-breaker into permanent deletion;
  the one moving session went 0.8667 → 0.9500 by discarding a real
  `groundedness = 2.0` deduction). Hand to Sprint 106 (`case_specs/**`) as a
  **note, not a task**: if a spec ever configures a gating dim, the advisory
  dims stop reaching that case's composite.

- **`R-golden-csv-missing-everywhere` — NEW, open, needs a human decision,
  owner Sprint 107 (`data/**`).** `data/human_review_annotations_2026-04-22_golden.csv`
  is absent from **every** checkout including the primary, and costs **13**
  permanently-red `eval_interactive` tests (8 in
  `tests/regression/test_case_spec_overrides.py`, 4 in
  `tests/regression/test_case_spec_persona_snapshots.py`, 1 in
  `tests/test_transcript_evidence.py::test_cs_interactive_004_...`). The file
  is gitignored (`.gitignore:8 data/*`), so this is an environment decision,
  not a code change: **regenerate the CSV, or make those tests skip when it
  is absent** so the stated baseline stops carrying 13 phantom failures.
  Either way the baseline doc should say which.

- **`R-worktree-pytest-baseline-drift` — already claimed by the Sprint 104
  patch; correct the split when merging.** Sprint 104's heads-up and Sprint
  105's first handoff draft both mis-stated the attribution. Measured by
  traceback 2026-07-26: the 14 baseline failures are **13 (golden CSV) + 1
  (`data/eval_datasets/badcase_turns.csv`, worktree-only, because
  `git worktree add` does not carry the gitignored `data/` tree)**. The
  Sprint 105 dev-prompt's stated "13 of the 14 come from the golden CSV" was
  correct. Fix from a worktree root:
  `ln -s /Users/caoruixin/projects/csagent/data/eval_datasets data/eval_datasets`.
  Sprint 107's worktree will hit the same gap.

- **Two failure briefs filed 2026-07-26** (handoff §7 items 4 + 8):
  - `docs/diagnostics/failure-briefs/sprint-105-2026-07-26-backend-500-unmeasured-sessions.md`
    — 2 of 17 recorded sessions died on an HTTP 500 from
    `POST /v1/chat/sessions/{id}/messages`, on two different backend builds,
    both on the turn where the customer changes the subject; ~12% of the
    substrate is unmeasured and no backend log survives. Layer `infra`,
    **scope = bot backend, not the eval framework → §5.8 does not preempt**.
    Owner `server/**` (Sprint 104's exclusive path).
  - `docs/diagnostics/failure-briefs/sprint-105-2026-07-26-resolved-stamp-vs-authoritative-user-state.md`
    — a `resolved` containment stamp survives an authoritative
    `user_state = unresolved_after_help`, and Sprint 105's new ladder now
    awards **D1** on top of it. Layer `eval_spec` (**re-routed** from the
    handoff's "runtime stamping / `server/**`": `ControlKernel.java:1558-1567`
    documents that the runtime never observes the simulator-side signal, so
    it cannot be the fix site).

- **`OQ-S105.user-state-not-consumed` — NEW, open, layer `eval_spec`; the
  enforcement half of the existing `OQ-S77.goal-impossible-resolved-evidence`.**
  The M-Auto-9 product decision already ruled that `resolved` + authoritative
  `user_state` UNRESOLVED ⇒ FAIL
  (`docs/current/m-auto-9-escalate-after-help-product-decision.md`, lines
  86-95). Nothing enforces it outside the opt-in path: `outcome_checks.py:238`
  gates `evaluate_conditional_outcome` behind
  `case_spec.conditional_outcome_acceptance`, and only **13 of 488 specs**
  declare that block (6 `anchor/`, 7 `bad_cases/`). The other 475 — including
  every `promotion/` spec — record `user_state_signals` and never read them.
  **Do NOT resolve this by adding `goal_impossible` to
  `_TERMINAL_FAILURE_STOP_REASONS`**; that was rejected twice on anti-误杀
  grounds (`hard_checks.py:952-965` + the M-Auto-9 decision). Cheap next
  measurement, no live run needed: re-score every recorded run with Sprint
  105's `rescore` command and count `containment_outcome == "resolved"`
  sessions whose final `user_state` is in the UNRESOLVED family.

---

## Cross-check before pasting

- [ ] Sprint-number collision with the M-Auto-12 line resolved (see above).
- [ ] `OQ-S77.goal-impossible-resolved-evidence` (existing, §5.2) updated in
      place to point at the new brief + `OQ-S105.user-state-not-consumed`
      rather than left reading as untouched since 2026-06-05.
- [ ] Sprint 104 / 106 / 107 handoffs read for their own §7 equivalents, so
      the wave gets one section rather than four.
- [ ] Nothing here is a §5.8 preempting brief — confirmed item by item; the
      one framework-scoped `infra` item is filed as an R-item with the
      judgement recorded.
