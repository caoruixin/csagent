---
title: "Sprint 098 / S-Auto-46 (M-Auto-10 WP1) — dev handoff: trace-contract active_use_case pre-routing false-positive fix (infra)"
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff) + eval_interactive/eval_interactive/trace/collector.py + eval_interactive/tests/trace/test_contract_validation.py
last_reviewed: 2026-06-21
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  Dev handoff for Sprint 098 / S-Auto-46 (M-Auto-10 WP1). Pure-infra
  (eval-framework) prerequisite per iteration_governance §5.8. Fixes a
  non-deterministic spurious 0-turn CONTRACT_VIOLATION on the L1
  trace_contract_active_use_case check: sessions captured mid-DISCOVER
  (pre-routing) with >=1 recorded turn and a legitimately-empty
  active_use_case were wrongly flagged. The brief's snake/camel hypothesis
  is DISPROVEN by code + data; real root cause is a phase-gating defect.
  §7-EXEMPT, Codex-EXEMPT (pure infra). NO server / scoring / aggregation /
  baseline / CaseSpec change. Unblocks WP2 + the next §5.6 rerun.
---

# Sprint 098 / S-Auto-46 (M-Auto-10 WP1) — dev handoff

**Class:** `infra` (eval-framework trace contract / collector).
**§7 stanza:** EXEMPT (pure infra; no semantic surface touched).
**Codex:** EXEMPT per §4.1 pure-infra clause (recorded in §6 below).
**§5.8:** this IS the framework-defect-priority prerequisite; it unblocks
M-Auto-10 WP2 and the next §5.6 bad-case rerun.

---

## §1 — Read-only root cause (written before any code change)

### 1.1 What the brief hypothesised (and why it is wrong)

The M-Auto-9 close brief
(`R-trace-contract-active-use-case-snakecase-camelcase`, manifest
`_manifest.md` "M-Auto-9 milestone close" → "Surfaced findings") hypothesised
a **snake_case-vs-camelCase key mismatch**: "the L1
`trace_contract_active_use_case` check asserts snake_case `active_use_case`
on the session-phase telemetry, but that phase exposes camelCase
`activeUseCase` (the value IS present in `available_keys`)."

This hypothesis is **disproven** on two independent grounds:

1. **The collector already reads both case variants.**
   `collector.py:_build_session_state` line 416 reads
   `active_uc = g(raw, "activeUseCase", "active_use_case") or ""`, where
   `_g` (`collector.py:261-270`) returns the first non-`None` value across
   *all* supplied key variants. So a present camelCase `activeUseCase` value
   would be read correctly — casing is not the failure.

2. **`available_keys` only proves the *key* is present, not the *value*.**
   `available_keys = sorted(session_raw.keys())` (`collector.py:287`). The
   key `activeUseCase` being in that list means the BotSession DTO field
   exists; it says nothing about whether the value is empty.

### 1.2 Which dict is `raw`, and where `activeUseCase` actually sits

- `raw` / `session_raw` is the JSON of `GET /v1/chat/sessions/{id}`, which
  serialises the **`BotSession` entity directly**
  (`ChatController.java:161-162` → `ResponseEntity<BotSession>`).
- `BotSession.activeUseCase` is a **persisted column**
  (`BotSession.java:49-50`, `@Column(name = "active_use_case")`) and is a
  top-level camelCase key on `session_raw` — confirmed: the recorded
  violations' `available_keys` contain `activeUseCase` (and the sibling
  `previousActiveUseCase`, `predictedUseCase`).
- The sibling use-case fields that *could* have been a fallback —
  `previousActiveUseCase`, `predictedUseCase` — are `@Transient`
  (`BotSession.java:225-226, 286-287`), i.e. **null on a DB-loaded entity**.
  They are not a recovery source.
- The per-turn `BotTurn` rows (`GET /v1/demo/sessions/{id}/trace`) carry
  their own `activeUseCase` (`BotTurn.java:60`), captured as
  `session.getActiveUseCase()` at turn time (ControlKernel build sites
  1330/2040/2469 via `TraceWriter.java:54`).

### 1.3 The decisive evidence (from the live postgres DB, read-only)

The three recorded rotating violations (from the M-Auto-9 close N=3 run
artifacts `eval_interactive/results/20260621-024842|-025341|-025842`) are
still present in the local `csagent` postgres DB. Queried read-only:

| session_id | run | bot_sessions.active_use_case | current_phase | containment_outcome | bot_turns |
|---|---|---|---|---|---|
| `505f62d5…09f4b` (cs029) | run1 | **(empty)** | **DISCOVER** | (empty) | 1 turn, `active_use_case=NULL`, `phase_after=DISCOVER` |
| `45fd346f…4cdf2e` (iwzx) | run2 | **(empty)** | **DISCOVER** | (empty) | 1 turn, `active_use_case=NULL`, `phase_after=DISCOVER` |
| `aa21363a…955bff3477` (cs012) | run3 | **(empty)** | **DISCOVER** | (empty) | 1 turn, `active_use_case=NULL`, `phase_after=DISCOVER` |

So `active_use_case` is genuinely empty at **both** session level **and**
turn level. There is **no exposed value anywhere** to recover — the
turn-fallback idea is ruled out too. The session is stuck mid-**DISCOVER**
with exactly one recorded turn.

The DB-wide phase × UC distribution (sessions with ≥1 turn) confirms the
invariant the contract should actually encode:

```
CLOSE    | UC_SET   |   115
CONFIRM  | UC_SET   |   314
DISCOVER | UC_EMPTY |   673   <- legitimate pre-routing; the false-positive population
DISCOVER | UC_SET   |    11
ESCALATE | UC_EMPTY |   302   <- out-of-scope hard-escalation; already carved out (is_oos_escalation)
ESCALATE | UC_SET   | 10447
RESOLVE  | UC_SET   |  2642
```

Beyond the pre-routing phases, the **only** UC-empty-with-turns population
is `ESCALATE` (the out-of-scope path, already exempt). Every routed phase
(`CONFIRM`/`RESOLVE`/`CLOSE`) is always `UC_SET`.

### 1.4 Exact loss point / true root cause

`_enforce_conditional_session_contracts` (`collector.py:367`) fires:

```python
if turns and not session_state.active_use_case and not is_oos_escalation:
    self._violate(field="active_use_case", ..., reason="missing_after_turns")
```

It uses **turn-count (`turns` non-empty) as the proxy for "routing has
happened."** That premise is false. The server's `UseCaseRouter` sets
`active_use_case` on **exit** from the discovery/triage phase, and discovery
can span **multiple** bot turns during which `active_use_case` is
*legitimately* empty. The server documents this directly:
`PhaseEvaluator.java:476` — `.useCase(activeUc) // may be null in DISCOVER
while still discovering`.

A session captured mid-`DISCOVER` (e.g. a turn-1 latency/timeout or an
early-terminated conversation — consistent with the non-deterministic,
rotating 3/54 incidence) therefore has ≥1 recorded turn but an empty,
**telemetry-faithful** `active_use_case`, and is wrongly flagged. This is a
trace-*contract* false positive (the telemetry is correct; the contract's
firing condition is wrong), classified `infra` per §3.2-Q1/eval-framework.

**Fix shape (read-only conclusion):** require `active_use_case` only once
the session has progressed **past** the pre-routing phases (`INIT`,
`DISCOVER`); keep the genuine contract for every routed phase and keep the
existing out-of-scope-escalation carve-out untouched. No value-recovery is
possible or needed — the gate is on *when the contract applies*.

---

## §2 — Minimal diff description + file list

Confined to the trace-collector read path + its test. **No** `server/`,
scoring, aggregation, baseline-loader, CaseSpec, or other-contract-check
change. `models.py` was **not** touched (the fix needed no model/comment
change).

Files:

1. `eval_interactive/eval_interactive/trace/collector.py`
   - **Added** module constant `PRE_ROUTING_PHASES = frozenset({"INIT",
     "DISCOVER"})` next to the existing `TERMINAL_PHASES`, with a comment
     citing the server's documented behaviour
     (`PhaseEvaluator.java`: "`.useCase(activeUc) // may be null in DISCOVER
     while still discovering`").
   - **Gated** the `active_use_case` conditional violation in
     `_enforce_conditional_session_contracts`: the violation now fires only
     when the session is **not** in a pre-routing phase
     (`... and not in_pre_routing_phase`), where
     `in_pre_routing_phase = session_state.current_phase in PRE_ROUTING_PHASES`.
     The `turns` non-empty condition, the `not session_state.active_use_case`
     condition, and the existing `is_oos_escalation` carve-out are all
     preserved unchanged.
   - **Corrected** the function docstring premise (routing completes on exit
     from the pre-routing phases, not "on the first user message"; turn-count
     alone is the wrong proxy).

2. `eval_interactive/tests/trace/test_contract_validation.py`
   - Imported `PRE_ROUTING_PHASES`.
   - Added the `_discover_turn_camelcase()` fixture helper + a new
     `TestPreRoutingPhaseCarveOut` class (4 tests; see §3).
   - Refined the comment on the existing
     `test_turns_present_still_requires_active_use_case` so its rationale
     reflects the phase-based contract (the test logic and assertions are
     unchanged and still green).

The change is **strictly narrowing**: it only ever *adds* an exemption
condition, so no session that previously did NOT raise a contract violation
can begin raising one (structural guarantee behind the §4 no-regression
claim).

## §3 — Characterization-test results (3 assertions)

`TestPreRoutingPhaseCarveOut` (zero-LLM, offline stub client), all green:

- **(a) spurious violation gone** —
  `test_mid_discover_turn_no_uc_does_not_violate`: a camelCase session DTO
  in `DISCOVER` with one recorded turn and empty `activeUseCase` at session
  AND turn level (the exact recorded violating shape) **collects cleanly in
  strict mode** (previously raised `CONTRACT_VIOLATION:active_use_case`).
- **(b) negative control — genuine contract preserved** —
  `test_routed_phase_no_uc_still_violates_negative_control`: a session in
  `RESOLVE` (routed phase) with no `activeUseCase` and not out-of-scope
  **still raises** `TraceContractError(field="active_use_case",
  reason="missing_after_turns")`.
- **(c) OOS carve-out intact** —
  `test_out_of_scope_escalation_carve_out_intact`: an `ESCALATE` session with
  empty UC, `containmentOutcome="escalated"`, `escalationReason="out_of_scope"`
  remains **non-violating** (the `is_oos_escalation` carve-out is untouched).
- Plus `test_pre_routing_phases_constant` pinning
  `PRE_ROUTING_PHASES == {"INIT", "DISCOVER"}`.

Targeted file: `38 passed in 0.36s` (was 34; +4 new). An in-memory sanity
check confirmed the OLD condition fired on the DISCOVER/1-turn/no-UC shape
and the NEW condition does not.

## §4 — Zero-LLM replay evidence (before/after + no-scored-outcome-change)

The recorded M-Auto-9 §5.6 close artifacts
(`eval_interactive/results/20260621-024842|-025341|-025842`, the N=3
54-session run) are present locally but their session DTOs were discarded by
the contract-violation result-builder. All 54 sessions are still in the
local `csagent` postgres DB, so the replay reconstructs each session
(`active_use_case`, `current_phase`, `containment_outcome`,
`escalation_reason`, turn count) read-only from the DB and runs the **real**
`TraceCollector` in **lenient mode** (captures every contract verdict without
strict-mode short-circuit), toggling `PRE_ROUTING_PHASES` between empty
(pre-fix) and `{INIT,DISCOVER}` (post-fix):

| metric | result |
|---|---|
| recorded sessions replayed | 54 / 54 (0 missing) |
| sessions raising ANY contract warning **BEFORE** fix | **3** |
| sessions raising ANY contract warning **AFTER** fix | **0** |
| sessions whose contract verdict **changed** | **3** |

The 3 changed sessions are exactly the rotating violations, each
`DISCOVER` / 1 turn / empty UC, flipping `["trace_contract_active_use_case"]`
→ `[]`:

- `cs029_uc_d_account_locked_callback` / `505f62d5…09f4b` (run1)
- `iwzx_uc_k_advert_on_hold_restore` / `45fd346f…4cdf2e` (run2)
- `cs012_uc_fp_late_phone_failure_path` / `aa21363a…bff3477` (run3)

The other **51 sessions are byte-identical before/after** (`AFTER ⊆ BEFORE`
for every session). The 3 changed were previously **mis-scored** as
`CONTRACT_VIOLATION` (composite 0, `case_passed=False`); after the fix they
collect normally and receive their real outcome. **No previously
correctly-scored session's outcome changed** — guaranteed structurally
(strictly-narrowing change) and confirmed empirically.

Replay harness: `/tmp/replay_098.py` (scratch, not committed; reads
`results/*/results.json` for the session list + DB for session facts).

## §5 — Suite status (pytest count + attribution; "no Java change")

`./.venv/bin/python -m pytest -q` (eval_interactive):
**639 passed / 0 failed / 5 errors** (14.96s).

- **0 failed.**
- **5 errors** = the documented pre-existing
  `tests/test_rescore_s_auto_38_full_baseline.py` `KeyError: 'form_context'`
  (unrelated to this change; identical to the `635p/0f/5e` baseline).
- **639 passed = 635 baseline + 4** new `TestPreRoutingPhaseCarveOut` tests.
  Clean attribution; no other test's status moved.
- **No Java change** — no file under `server/` was edited (`git diff
  --name-only` confirms only `collector.py`,
  `test_contract_validation.py`, and this handoff). No bot behaviour or
  eval scoring/aggregation/baseline/CaseSpec touched.

## §6 — §8 STOP-check + §4.1 Codex-exemption record

**STOP conditions (none fired):**

- **Scope creep** — not triggered. The fix is a single phase-gate in the
  trace-collector read path; no scoring/aggregation/baseline change was
  needed or made.
- **Outcome change** — not triggered. No previously-correctly-scored
  session flips pass/fail; only the 3 spurious contract violations are
  removed (they were never correctly scored). Verified in §4.
- **Genuine-contract weakening** — not triggered. The negative control
  (routed-phase session with truly-missing UC) still violates (§3b), and
  the change is strictly narrowing.
- **No real-LLM run, no re-bless, no canonical-pointer move, no `server/`
  edit, no scoring/aggregation/baseline/CaseSpec change** — all honoured
  (zero-LLM throughout; DB reads are read-only diagnosis).

**§4.1 Codex exemption (recorded):** this PR is **pure infra** (eval-framework
trace-contract collector + its characterization test; no prompt, runtime
semantic decision, eval spec, judge calibration, or new
keyword/regex/enum influencing routing/escalation). Per the §4.1
pure-infra exemption clause, the nine-question anti-hardcode kernel is
**not** dispatched for this sub-sprint; verdict recorded as **exempt**. No
`codex exec` run.

## §7 — Recommended verdict

**COMPLETE** (infra-prerequisite). The spurious, non-deterministic
`trace_contract_active_use_case` false positive is root-caused (read-only,
DB-evidenced, brief hypothesis disproven), fixed with a minimal
strictly-narrowing collector phase-gate, covered by a zero-LLM
characterization test (incl. negative control + OOS carve-out), and proven
on the recorded M-Auto-9 §5.6 trace set (3 rotating violations gone, 51
sessions unchanged). Trace-contract evidence comparability is restored.

Per iteration_governance §5.8 this **unblocks** M-Auto-10 WP2 and the next
§5.6 bad-case rerun.
