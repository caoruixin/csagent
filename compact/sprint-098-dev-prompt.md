# Dev prompt — Sprint 098 / S-Auto-46 (M-Auto-10 WP1)

> Paste-to-launch executable view of `docs/sprint_objective.md` (Sprint 098).
> Self-contained per `docs/current/process/prompt-artifact-rules.md` §9.1/§9.2 —
> you do NOT need to read any doc other than `AGENTS.md` (auto-loaded) + this prompt
> + the code anchors cited inline.

## 1. Role identity

You are the **dev agent for Sprint 098 / S-Auto-46 (M-Auto-10 WP1)**.

**One-line goal:** make the eval-framework L1 `trace_contract_active_use_case` check
stop firing a spurious 0-turn `CONTRACT_VIOLATION` on sessions whose telemetry
carries camelCase `activeUseCase` (value present), restoring trace-contract evidence
comparability — while keeping the genuine contract (a session that truly has no
active UC after a bot turn must still violate).

This is an **`infra` (eval-framework) prerequisite** sub-sprint. Per
iteration_governance §5.8 it preempts M-Auto-10's later semantic/design work and the
next §5.6 rerun. It is **§7-EXEMPT** and **Codex-EXEMPT** (pure infra).

## 2. Read order (minimal)

1. `AGENTS.md` (auto-loaded — governance chain).
2. This prompt (the full contract is embedded below).
3. Code anchors only as needed:
   - `eval_interactive/eval_interactive/trace/collector.py` (the contract +
     `_build_session_state` + `_enforce_conditional_session_contracts`).
   - `eval_interactive/eval_interactive/batch/executor.py:799` (check-name emit).
   - `eval_interactive/eval_interactive/trace/models.py:72` (contract doc).
   - `eval_interactive/tests/trace/test_contract_validation.py` (existing test;
     `trace_contract_active_use_case` at line 524).

Do NOT read other docs; everything you need is here.

## 3. Embedded contract

### Class

- **§3.2 layer:** `infra` (eval-framework trace contract / collector).
- **§7 stanza:** EXEMPT (pure infra; no semantic surface).
- **Codex:** EXEMPT per §4.1 pure-infra clause — record the exemption in the handoff
  verdict; do NOT dispatch `codex exec` for this sub-sprint.
- **§5.8:** this IS the framework-defect-priority prerequisite; it unblocks WP2 and
  the next §5.6 rerun.

### Background (verified anchors — diagnosis target, not a fix prescription)

- Violation raised in
  `collector.py:_enforce_conditional_session_contracts` (~line 367-375):
  `if turns and not session_state.active_use_case and not is_oos_escalation: self._violate(field="active_use_case", ...)`.
- `session_state.active_use_case` is set in `_build_session_state` (~405-450), which
  at ~line 416 already reads `active_uc = g(raw, "activeUseCase", "active_use_case") or ""`
  — i.e. the session-level read **already tries both case variants**. The value is
  therefore lost **before/around that read**: the authoritative `activeUseCase` lives
  on an object/nesting level this `raw` does not expose.
- The brief notes the value IS present in the violation's `available_keys`, which is
  built from the same `raw` at `collector.py:287` — so root-cause must be pinned
  precisely: which dict is `raw` here, and where does `activeUseCase` actually sit on
  the recorded trace JSON?
- Surfaced 2026-06-21 at the M-Auto-9 §5.6 rerun as **3/54 rotating** (non-deterministic)
  violations. Did not block the M-Auto-9 close. It may overlap the
  `primary_uc`-vs-`active_use_case` authority OQ (Cluster B.2) — **do NOT open that**;
  fix only the key/path reconciliation.

### Scope (numbered)

1. **Read-only root-cause.** Reproduce the spurious violation on a recorded trace
   carrying camelCase `activeUseCase`. Pin the exact loss point (which dict is `raw`/
   `session_raw`; where `activeUseCase` actually sits — top-level vs nested phase/
   telemetry object). Write the finding into the handoff **before** changing code.
2. **Minimal key/path reconciliation fix.** Smallest change that makes the contract
   read the active UC from the correct object/variant, confined to the trace-collector
   read path (`collector.py`; `models.py` comment only if needed). Do NOT touch
   scoring, aggregation, baseline loaders, CaseSpec, or any other contract check.
3. **Characterization test (zero-LLM)** in `tests/trace/test_contract_validation.py`:
   (a) camelCase-`activeUseCase` fixture that previously violated now does NOT; (b)
   negative control — NO active-UC variant present STILL violates after a bot turn
   (genuine contract preserved); (c) out-of-scope-escalation fixture remains
   non-violating (`is_oos_escalation` carve-out untouched).
4. **Regression replay (zero-LLM).** Run the collector over the recorded M-Auto-9
   §5.6 trace set (referenced in `eval_interactive/case_specs/bad_cases/_manifest.md`
   "M-Auto-9 milestone close"): show the 3 rotating violations gone AND **no
   previously-correctly-scored session's outcome changed**. Record before/after counts.
   If the recorded set is gitignored/unavailable, document the representative
   substitute fixtures used.
5. **Suite green.** `eval_interactive` pytest green vs baseline `635 passed / 0 failed
   / 5 errors` (the 5 errors = pre-existing `test_rescore_s_auto_38_full_baseline.py`
   `KeyError: form_context`, unrelated; counts are env-dependent — attribute deltas).
   No `server/` change.

### Hard fences / STOP conditions

- **STOP — scope creep:** if the fix seems to need scoring/aggregation/baseline
  changes (not just the trace read), STOP and re-scope with the deliver-agent. Do NOT
  broaden into unrelated eval refactoring.
- **STOP — outcome change:** if the fix would flip any previously-correctly-scored
  session's pass/fail, STOP — it is then not a pure contract-read reconciliation.
- **STOP — genuine-contract weakening:** if you cannot keep the negative-control
  (truly-missing-UC) session violating, STOP — do not weaken the real contract.
- **NO real-LLM run** (zero-LLM only). **NO** scoring / aggregation / baseline /
  CaseSpec / canonical-pointer / re-bless change. **NO** `server/` edit.
- Touch only `collector.py` (+ `models.py` comment if needed) +
  `tests/trace/test_contract_validation.py`.

### Test / eval requirements

- Zero-LLM characterization test green incl. the negative control.
- Zero-LLM replay evidence in handoff: before/after violation count + no-scored-outcome-change.
- `eval_interactive` pytest green (delta vs `635p/0f/5e` attributed).
- No Java impact (no `server/` edit) — state explicitly.

### Handoff requirements (`docs/sprints/sprint-098-handoff.md`)

1. §1 read-only root-cause (which `raw` object; where `activeUseCase` sits).
2. Minimal diff description + file list (confirm only collector.py [+ models.py
   comment] + the test touched).
3. Characterization-test results (3 assertions).
4. Zero-LLM replay evidence (before/after + no-scored-outcome-change).
5. Suite status (pytest count + attribution; "no Java change").
6. §8 STOP-check (none fired) + §4.1 Codex-exemption record.
7. Recommended verdict (COMPLETE infra-prerequisite; unblocks WP2 + the next §5.6
   rerun per §5.8).

### Commit discipline

Stage only authorized files (NOT `git add -A`). Tree green at each boundary. No
data/artifact files committed.

## 4. Self-check checklist (tick before declaring done)

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
