---
title: "Sprint 098 / S-Auto-46 (M-Auto-10 WP1) — trace-contract active_use_case snake/camel reconciliation"
doc_tier: current-runtime
status: proposal
implementation_status: not_started
source_of_truth: this file (active sub-sprint contract); parent milestone = docs/milestone_objective.md (M-Auto-10)
last_reviewed: 2026-06-21
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  DRAFT — deliver-agent scoping output, PENDING HUMAN APPROVAL before launch. First
  executable sub-sprint of M-Auto-10. PREREQUISITE per iteration_governance §5.8: the
  §3-infra eval-framework trace-contract brief must be resolved before the next §5.6
  milestone-level rerun and before any new semantic sub-sprint. Pure infra
  (eval-framework trace collector); §7-EXEMPT; Codex-EXEMPT per §4.1 pure-infra clause.
  NO real-LLM run, NO scoring/aggregation/baseline/CaseSpec/canonical-pointer/re-bless
  change. The fix is a minimal key-reconciliation in the trace collector; the gate is a
  zero-LLM characterization test + a zero-LLM replay over the recorded M-Auto-9 §5.6
  trace set proving the spurious violation is gone AND no previously-correctly-scored
  session's outcome changed. Self-contained dev prompt: compact/sprint-098-dev-prompt.md.
---

# Sprint 098 / S-Auto-46 (M-Auto-10 WP1) — trace-contract `active_use_case` reconciliation

> **ARCHIVED — CLOSED COMPLETE 2026-06-21 (dev `f7c5232d` + deliver close). Handoff:
> `docs/sprints/sprint-098-handoff.md`.** The dev disproved the brief's snake/camel
> hypothesis with DB evidence and root-caused the true defect (turn-count is the wrong
> proxy for "routed"; `active_use_case` is set on *exit* from DISCOVER) — fixed
> strictly-narrowing via a `PRE_ROUTING_PHASES` phase-gate; 3→0 spurious violations on
> the 54-session §5.6 set, 51 byte-identical, no scored-outcome change. The
> "DRAFT pending approval" notes below are the historical pre-launch contract.

> **STATUS (historical): DRAFT — pending human approval. No dev session launched.**

## Class

- **§3.2 layer:** `infra` (eval-framework trace contract / collector).
- **iteration_governance §7 stanza:** **EXEMPT** — pure infra eval-framework; no
  prompt / runtime-semantic / eval-spec / judge surface touched.
- **Codex:** **EXEMPT** per iteration_governance §4.1 pure-infra exemption clause
  (record the exemption explicitly in the handoff verdict).
- **§5.8 status:** this IS the framework-defect-priority prerequisite. It preempts the
  M-Auto-10 semantic/design sub-sprints (WP2/WP3) and the next §5.6 rerun.

## Goal

Make the L1 `trace_contract_active_use_case` check stop firing a spurious 0-turn
`CONTRACT_VIOLATION` on sessions whose session-state telemetry carries camelCase
`activeUseCase` (value present in `available_keys`), so the recorded trace set is
contract-consistent and evidence comparability is restored before the next §5.6
milestone-level rerun. The genuine contract — a session that truly has no active UC
after a bot turn — must still violate.

## Background (verified code anchors — for the dev's read, not a fix prescription)

- The violation is raised in
  `eval_interactive/eval_interactive/trace/collector.py:_enforce_conditional_session_contracts`
  (~line 367-375): `if turns and not session_state.active_use_case and not is_oos_escalation: self._violate(field="active_use_case", ...)`.
- `session_state.active_use_case` is populated in `_build_session_state`
  (`collector.py:~405-450`), which at line ~416 already reads
  `active_uc = g(raw, "activeUseCase", "active_use_case") or ""` — i.e. the
  session-level read **already tries both variants**. So the value is being lost
  **before/around that read**: the authoritative `activeUseCase` lives on an object or
  nesting level that this `raw` does not expose (the brief notes the value IS present
  in the violation's `available_keys`, which is built from the same `raw` at
  `collector.py:287` — so the root-cause must be pinned precisely: which object is
  `raw` here, and where does `activeUseCase` actually sit on the recorded trace).
- The check name is emitted at `eval_interactive/eval_interactive/batch/executor.py:799`
  (`check_name=f"trace_contract_{exc.field}"`); the contract is documented in
  `eval_interactive/eval_interactive/trace/models.py:72`; the existing test lives at
  `eval_interactive/tests/trace/test_contract_validation.py` (asserts
  `trace_contract_active_use_case` at line 524).
- Surfaced 2026-06-21 at the M-Auto-9 §5.6 rerun as **3/54 rotating** violations
  (non-deterministic which sessions). Did NOT block the M-Auto-9 close. May overlap
  the `primary_uc`-vs-`active_use_case` authority OQ (Cluster B.2) — but WP1 does NOT
  open that; it fixes only the key/path reconciliation.

## Scope (numbered, step-by-step)

1. **Read-only root-cause.** Reproduce the spurious violation on a recorded trace
   that carries camelCase `activeUseCase`. Pin the exact loss point: confirm which
   dict is passed as `raw`/`session_raw` into `_build_session_state` /
   `_enforce_conditional_session_contracts`, and where the authoritative
   `activeUseCase` actually sits on the recorded session JSON (top-level vs a nested
   phase/telemetry object). Write the finding into the handoff before changing code.
2. **Minimal key/path reconciliation fix.** Apply the **smallest** change that makes
   the contract read the active UC from the correct object/variant (e.g. extend the
   variant set or the source-object lookup at the single point where it is lost). The
   fix must be confined to the trace-collector read path
   (`collector.py`, and `models.py` doc only if a comment needs updating). Do NOT
   touch scoring, aggregation, baseline loaders, CaseSpec, or any other contract
   check's behaviour.
3. **Characterization test (zero-LLM).** Extend
   `eval_interactive/tests/trace/test_contract_validation.py` with: (a) a fixture
   exposing camelCase `activeUseCase` (value present) that previously violated and now
   does NOT; (b) a negative-control fixture with NO active-UC variant present that
   STILL violates `trace_contract_active_use_case` after a bot turn (the genuine
   contract is preserved); (c) an out-of-scope-escalation fixture remains
   non-violating (the existing `is_oos_escalation` carve-out is untouched).
4. **Regression replay (zero-LLM).** Run the trace collector over the recorded
   M-Auto-9 §5.6 trace set (the 54-session set referenced in
   `eval_interactive/case_specs/bad_cases/_manifest.md` "M-Auto-9 milestone close")
   and show: the 3 rotating `trace_contract_active_use_case` violations are gone, and
   **no previously-correctly-scored session's scored outcome changed** (the fix only
   removes the spurious violation; it does not flip any pass/fail). Record before/after
   counts in the handoff. If the recorded set is gitignored/unavailable, document the
   substitute fixture set used and why it is representative.
5. **Suite green.** `eval_interactive` pytest green at the documented baseline
   (`635 passed / 0 failed / 5 errors` — the 5 errors are the pre-existing
   `test_rescore_s_auto_38_full_baseline.py` `KeyError: form_context` fixtures,
   unrelated; counts are pytest-invocation/env dependent — attribute any delta). No
   Java change (no `server/` edit).

## Hard fences / STOP conditions

- **STOP — scope creep:** if the minimal fix appears to require changing scoring /
  aggregation / baseline semantics (not just the trace read), STOP and re-scope with
  the deliver-agent. Do NOT broaden into unrelated eval refactoring (the §5.8
  prerequisite is narrow by design).
- **STOP — outcome change:** if the fix would alter any previously-correctly-scored
  session's pass/fail outcome, STOP — that means the change is not a pure
  contract-read reconciliation.
- **STOP — genuine-contract weakening:** if you cannot keep the negative-control
  (truly-missing-UC) session violating, STOP — do not weaken the real contract to
  silence the false positive.
- **NO real-LLM run** (zero-LLM only). **NO** scoring / aggregation / baseline /
  CaseSpec / canonical-pointer / re-bless change. **NO** `server/` change.
- Touch only: `eval_interactive/eval_interactive/trace/collector.py` (+ `models.py`
  comment if needed) and `eval_interactive/tests/trace/test_contract_validation.py`.

## Test / eval requirements

- Zero-LLM characterization test (scope #3) green, including the negative control.
- Zero-LLM regression replay (scope #4) evidence in the handoff: before/after
  violation count + a no-scored-outcome-change assertion.
- `eval_interactive` pytest green (attribute any count delta vs `635p/0f/5e`).
- No Java suite impact (no `server/` edit); state this explicitly.

## §7 stanza

**EXEMPT** — pure infra eval-framework sub-sprint (no semantic surface). Per
iteration_governance §7, infra sub-sprints need not include the stanza.

## Codex review plan

**EXEMPT** per iteration_governance §4.1 pure-infra exemption clause. The dev records
the exemption in the handoff verdict (no `codex exec` dispatch for WP1). The
milestone-shared §4.1 Codex at M-Auto-10 close re-verifies the cumulative range.

## Handoff requirements (`docs/sprints/sprint-098-handoff.md`)

1. The §1 read-only root-cause finding (which `raw` object, where `activeUseCase`
   actually sits).
2. The minimal diff description + file list (confirm only collector.py [+ models.py
   comment] + the test were touched).
3. Zero-LLM characterization-test results (3 assertions: fixed / negative-control
   still violates / oos still carved out).
4. Zero-LLM replay evidence (before/after violation count over the §5.6 trace set +
   no-scored-outcome-change assertion).
5. Suite status (eval_interactive pytest count + attribution; "no Java change").
6. §8 STOP-check (none fired) + the §4.1 Codex-exemption record.
7. Recommended verdict (COMPLETE infra-prerequisite, unblocking WP2 + the next §5.6
   rerun per §5.8).

## Commit discipline

Stage only the authorized files (NOT `git add -A`); deliver-agent-owned docs are
bundled by the human at close. Tree green at each commit boundary. No data/artifact
files committed (results are gitignored).

## Self-check checklist (dev ticks before declaring WP1 done)

- [ ] Root cause pinned read-only and written to handoff §1 before any code change.
- [ ] Fix confined to the trace-collector read path; no scoring/aggregation/baseline/
      CaseSpec/`server/` edit.
- [ ] Characterization test: spurious violation gone; negative control still violates;
      oos carve-out intact.
- [ ] Zero-LLM replay: 3 rotating violations gone; no previously-correctly-scored
      session's outcome changed (before/after recorded).
- [ ] eval_interactive pytest green; count delta attributed; no Java change.
- [ ] No real-LLM run; no re-bless; no canonical-pointer move.
- [ ] Handoff §1–§7 complete; §4.1 Codex exemption recorded.
