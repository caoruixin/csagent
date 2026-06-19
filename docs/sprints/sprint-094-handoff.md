---
title: "Sprint 094 / S-Auto-40 (M-Auto-7) — WP1-A handoff: measurement contract → conditional outcome acceptance"
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: this file (dev handoff); code under eval_interactive/
last_reviewed: 2026-06-19
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  WP1-A only. Phase 1 (infra) + Phase 3 (eval_spec) landed and validated by
  unit + zero-LLM §3.3 replay. Phase 2 (bounded real-LLM validation) is PENDING
  a live backend + human-launched run — the §5.7 mocked-LLM evidence gate forbids
  certifying the measurement contract on mocked evidence. Pilot / WP1-B / WP2 /
  objective-alignment annotation remain HELD; exp-82 WITHDRAWN; canonical baseline
  pointer UNCHANGED. No bot/runtime/prompt/reason-enum/handover change.
---

# Sprint 094 / S-Auto-40 (M-Auto-7) — WP1-A handoff

One-line: encode the 2026-06-19 product decision (escalation-after-genuine-help
is a VALID terminal when the user remains UNRESOLVED; resolve when SATISFIED) as
a **declarative, condition-bound** outcome acceptance for the two PRIMARY UC-A
bad cases, on a **positive structured user-state signal** — measurement-first,
with **no bot/runtime/prompt/reason change**.

HEAD at sprint start: `ab9ca077`.

## §0 Status table

| Phase | Layer | State | Evidence |
|---|---|---|---|
| Step 0 — measurement gate | — | **RE-CONFIRMED INSUFFICIENT** | structural: simulator per-turn `goal_status` discarded; no `user_state`; escalation `bot_ended` break precedes `generate_next`. |
| Phase 1 — trace-only measurement contract | `infra` | **DONE + unit-tested** | 50 tests (sim parse + session-runner signal series). |
| Phase 2 — bounded real-LLM validation | `infra` | **PENDING (env-gated)** | backend down at session; §5.7 forbids mocked certification. Not INCONCLUSIVE — simply not yet run. |
| Phase 3 — declarative three-state acceptance | `eval_spec` | **DONE + zero-LLM-tested** | 19 §3.3 replay tests, all green. |
| Baseline migration | — | **DECIDED + specified** (execution waits on Phase 2) | see §6. |
| Codex per-sub-sprint review | — | **PENDING** | dispatch after commit per close memo. |

Full suite: **620 passed**, 6 pre-existing failures (1 stale anchor-count +
5 baseline-rescore `KeyError: form_context`), **proven pre-existing** by a
stash-and-rerun on the clean tree — WP1-A adds **zero new regressions**.

## §1 Step-0 re-confirmation (measurement-first)

Deliver pre-answered INSUFFICIENT; re-confirmed structurally from code (no LLM):

- `simulator/user_simulator.generate_next` produced a per-turn `goal_status`
  but it was **discarded** — only the terminal one became `stop_reason`. There
  was **no** `user_state` field and **no** per-turn persistence.
- `session_runner.run_session`: the `should_end` (`bot_ended`) break at
  ~196–199 runs **before** `generate_next` at ~217, so an escalation/handover
  draw never produces a terminal `goal_status` — matching the 13/13
  `bot_ended` escalations in the Sprint 093 bounded run.
- `trace/models.TraceData` had no user-state field; transcript turns carried
  only `role/message/turn_index/source`.

⇒ every escalation read as **UNKNOWN**, which must not auto-PASS. Proceed
measurement-first.

Escalation-reason cross-check (why reason is NOT a usable UNRESOLVED proxy):
in the Sprint 093 full baseline the two PRIMARY cases escalate under **mixed**
reasons — `user_requested`, `clarification_budget_exhausted`,
`faq_miss_threshold_exceeded`. The budget/miss reasons are bot-side early/lazy
escalations that must still FAIL; only a positive **user-state** signal can
separate genuine-unresolved from over-escalation.

## §2 Phase 1 — measurement contract (`infra`, eval-framework)

**Signal**: a positive, structured, per-turn `user_state` emitted by the
simulator **in the same `generate_next` call** that makes the customer turn.
Enum (the model's own emission, never back-inferred):

| value | meaning | three-state |
|---|---|---|
| `satisfied` | the bot's help solved my specific problem | SATISFIED |
| `unresolved_after_help` | bot already tried to help but it did NOT solve my problem; I still need help | UNRESOLVED (positive) |
| `working` | still working through it; no judgement yet | neutral → UNKNOWN |
| `new_request` | now raising a different/new request | neutral → UNKNOWN |
| absent / out-of-vocab / unparseable | — | UNKNOWN |

`goal_status="achieved"` is also read as a positive SATISFIED terminal;
`goal_status="impossible"` is **kept separate** (→ UNKNOWN, never UNRESOLVED).

**Provenance** (persisted per signal): `turn_id` (the bot turn the customer is
reacting to — alignable to the closure marker), `produced_user_turn`,
`user_state`, `goal_status`, `signal_source="simulator_generate_next"`,
`schema_version=1`.

**Persistence path** (no post-`bot_ended` LLM call; no back-inference; no
carry-forward):
`generate_next` → `SessionResult.user_state_signals` (appended in the same loop
iteration, before the achieved/impossible checks) → `TraceData.user_state_signals`
(attached by the batch executor after backend-trace collection) →
serialized to `case_results[].user_state_signals` in results.json.

Files: `simulator/user_simulator.py` (prompt + parse + constants),
`simulator/session_runner.py` (signal series), `trace/models.py` (+field),
`batch/executor.py` (attach + serialize).

**Note on the "no prompt change" fence**: that fence is the **bot/runtime**
prompt. The Phase-1 enrichment is the **simulator's own generation prompt**,
which the WP1-A contract explicitly authorizes ("enrich the same call with a
structured `user_state` field"). No bot/runtime/reason/handover code changed.

**Tests** (mock-LLM wiring only, per §5.7 — NOT behaviour evidence):
`tests/test_user_simulator.py::TestUserStateSignal` (parse contract, vocab
guard, independence from `goal_status`, lenient fallback → None);
`tests/test_session_runner_user_state.py` (post-help UNRESOLVED captured then
escalation carries no terminal signal; early escalation → 0 signals; satisfied
terminal recorded; per-turn independence / no carry-forward).

## §3 Phase 3 — declarative three-state conditional acceptance (`eval_spec`)

New generic evaluator `scoring/conditional_outcome.py` (no case-id hardcode, no
free-text). Activated only when a CaseSpec carries the declarative block:

```yaml
conditional_outcome_acceptance:
  schema_version: 1
  satisfied_outcome: resolve
  unresolved_accept_outcome: escalate
  require_closure_precondition: true
```

Added to **only** `cs_uc_a_no_ad_id_ad_specific` + `cs_uc_a_loaded_listing`.
Neighbors (`cs_uc_a_generic_policy_question`, `cs_uc_a_lookup_failed`, …) carry
no block → unchanged legacy resolve-only path (verified by test).

Wiring: `case_spec/schema.py` stores the raw block (`Optional[dict]`, default
None — backward-compatible); `case_spec/loader.py` passes it through;
`scoring/outcome_checks.py::_check_correct_outcome` **delegates** to the
evaluator when the block is present and returns its verdict as the
`correct_outcome` result. `correct_outcome` is an always-mandatory L2 gate
(threshold 1.0), so the evaluator governs pass/fail without new gate plumbing.

**Three-state decision** (score 1.0 only on a final PASS; 0.0 otherwise, so the
gate never auto-passes a CONDITIONAL_ELIGIBLE):

- `SATISFIED` + resolve → **PASS** · `SATISFIED` + escalate → **FAIL**
- `UNRESOLVED` (positive, post-closure-marker) + escalate + adjudicated → **PASS**
- `UNRESOLVED` + escalate + **no** adjudication → **CONDITIONAL_ELIGIBLE / REVIEW_REQUIRED** (0.0)
- `UNRESOLVED` + escalate + **no** closure marker → **FAIL** (incomplete/non-grounded)
- `UNRESOLVED` + resolve → **FAIL** (false resolve)
- `UNKNOWN`/neutral/new-goal + escalate → **FAIL** (not accepted)

**Closure-quality**: the structural marker is the earliest bot turn with
`tool_calls` + non-empty `source_ids` + a non-empty grounded answer. It proves
only the **precondition** that grounded help was attempted — never that closure
was met → the evaluator emits CONDITIONAL_ELIGIBLE, never auto-PASS on the
marker alone.

**Reproducible adjudication** (binding): CONDITIONAL_ELIGIBLE ≠ final PASS.
Acceptance requires a committed registry entry
(`case_specs/conditional_outcome_adjudications.yaml`) keyed by `trace_id`
(= `session_id`), matched on `case_id` **and** `closure_criterion_version`
(sha256 of the CaseSpec closure criterion — a closure edit invalidates stale
adjudications → back to REVIEW_REQUIRED), with `verdict: accept_escalation`,
reviewer, rationale, timestamp. The evaluator only **reads** this file — it
never calls human judgment at runtime. Because `trace_id` is per-run, a fresh
real-LLM run produces new ids with no adjudication ⇒ every new eligible
escalation is REVIEW_REQUIRED by default (anti-误杀 / anti-widen property).
Registry seeded **empty** (`adjudications: []`).

## §4 §3.3 anti-widen replay matrix (zero-LLM) — all PROVEN

`tests/test_conditional_outcome.py` (19 tests, green). Each row asserts none
may enter the unresolved-accepted branch without genuine post-help UNRESOLVED +
closure + adjudication:

| Shape | Verdict |
|---|---|
| grounded + SATISFIED + resolve | PASS |
| grounded + positive-UNRESOLVED + closure + escalate, no adjudication | CONDITIONAL_ELIGIBLE (0.0) |
| …same + adjudicated | PASS |
| early/lazy escalation (no signal, no marker) | FAIL |
| escalation-after-satisfaction | FAIL |
| UNRESOLVED + escalate but non-grounded (no marker) | FAIL |
| false resolve (UNRESOLVED + resolve) | FAIL |
| UNKNOWN-after-grounded escalation (no signal) | FAIL |
| neutral `working`-only escalation | FAIL |
| `new_request`-only escalation | FAIL |
| pre-help UNRESOLVED (turn_id < marker) | FAIL |
| satisfaction-then-escalation (latest positive wins) | FAIL |
| `goal_status=impossible` not mapped to UNRESOLVED | FAIL (UNKNOWN) |
| stale adjudication (closure-version mismatch) | CONDITIONAL_ELIGIBLE |
| adjudication for a different trace_id | CONDITIONAL_ELIGIBLE |
| **composite**: SATISFIED+resolve | case_passed True |
| **composite**: CONDITIONAL_ELIGIBLE w/o adjudication | case_passed False |
| **composite**: UC-misclass + adjudicated escalate | case_passed False (correct_uc gate) |
| neighbor w/o block | legacy resolve-only (escalate → 0.0) |

## §5 Phase 2 — bounded real-LLM validation (PENDING, env-gated)

Not executed: the backend was unreachable at this session
(`GET /actuator/health` empty) and §5.7 forbids certifying a measurement /
prompt change on mocked-LLM evidence. This is **not** an INCONCLUSIVE stop
under the contract (the STOP conditions — no same-call signal possible, floor
unmet on a *run*, baseline not combinable, free-text-only branch — did not
trigger). It is simply the remaining live-evidence gate, and pilot/WP1-B/WP2
are already HELD on it.

**To run** (human-launched, or dev on a clean committed tree):
1. §5.9 pre-flight GO/no-go on a sample (see §5.1 below).
2. `caffeinate` the Mac (multi-hour real-LLM; a sleep-spanned run is
   uncertifiable — kill + re-run fresh).
3. Small bounded run: 2 PRIMARY + controls/neighbors (no pilot, no candidate
   search). One existing stable satisfiable control MAY be added only to
   validate the measurement contract (not WP2).
4. **Evidence floor** (else INCONCLUSIVE → stop, no re-bless, no pilot):
   ≥1 `SATISFIED`; **≥3 positive post-help `UNRESOLVED`**; `UNKNOWN`/neutral
   samples; handover before/after timing; no outcome leakage.

### §5.1 §5.9 pre-flight checks for this run (read-only)

- backend `/actuator/health` == UP; simulator LLM gateway reachable
  (`llm.base_url`/model set).
- a 1-case dry run emits `user_state_signals` in `case_results[]` (field
  present + non-empty on a multi-turn draw) — i.e. the instrumentation is live,
  not a stale backend (cf. restart-backend memo).
- **turn alignment check**: confirm a signal's `turn_id` lines up with the
  bot-side `TraceData.turns[*].turn_index` numbering (both number the first
  user→bot exchange as turn 1). An off-by-one here would mis-align the
  closure-marker precondition — validate on the first real decidable trace
  before trusting the UNRESOLVED branch.
- no proxy/localhost breakage for the simulator client (macOS httpx memo).

## §6 Baseline migration decision (blocking) — DECIDED

Old traces lack `user_state_signals` ⇒ under the evaluator they reduce to
**UNKNOWN**; an old-trace replay therefore **cannot** rebuild a conditional
baseline for the 2 PRIMARY cases (UNKNOWN+escalate → FAIL, UNKNOWN+resolve →
PASS, but no UNRESOLVED branch is reachable). Decision:

- The **2 PRIMARY** cases get **new-instrumentation decidable baseline draws**
  from the Phase-2 run; their pre-instrumentation counts are **forensic only**.
- **All other** cases **reuse** their existing baseline counts unchanged — the
  conditional block does not touch them, so their scoring semantics are
  identical (proven: neighbor-without-block test).
- This is a **per-case replacement** limited to the 2 PRIMARY. It is safely
  combinable because the other cases' semantics are byte-for-byte unchanged by
  this sprint; only 2 cases acquire a new (strictly additive) signal.
- **Canonical baseline pointer UNCHANGED.** A new run-scoped baseline for the
  2 PRIMARY is formed only after Phase 2 produces decidable draws; if at that
  point the mix is judged not safely combinable, STOP → formal re-bless (not
  done unilaterally here).

## §7 stanza

- **Target failure layer:** `infra` (Phase 1 simulator/trace instrumentation) +
  `eval_spec` (Phase 3 declarative conditional acceptance).
- **Tier-0 invariant:** This sprint adds no Tier-0 invariant.
- **Semantic hardcode:** No semantic hardcode introduced. The signal is the
  simulator's own same-call structured emission; the acceptance is a declarative
  condition-bound read (no case-id, no regex, no free-text). §5.4
  product-authorized; the anti-误杀 matrix (§4) keeps early/lazy/over-escalation
  and false-resolve FAILing.
- **Generalization coverage:** target 2 / neighbor 2 (read-only, no block) /
  negative = the §3.3 edge battery (generic-policy resolve-only + the FAIL
  rows) / shadow = held-out (unchanged; no block).

## §8 Self-check / fences

- [x] Step 0 re-confirmed; measurement-first.
- [x] Phase-1 signal is same-call, provenance-tagged, post-help-alignable;
      missing→UNKNOWN; no back-inference / carry-forward / post-hoc LLM.
- [ ] **Phase-2 evidence floor — PENDING (env-gated real-LLM run).** Not
      certifying on mocked evidence (§5.7).
- [x] Phase-3 declarative, no case-id/free-text; three-state matrix + §3.3
      battery all hold on zero-LLM replay.
- [x] CONDITIONAL_ELIGIBLE never auto-PASS; adjudication artifact
      versioned/auditable; evaluator reads it, no runtime human call.
- [x] Baseline migration decided (§6); canonical pointer unchanged.
- [x] 2 PRIMARY only; neighbors read-only; generic-policy resolve-only.
- [x] Python no new regression (620 passed; 6 failures proven pre-existing).
      Java suite untouched (no Java changed).
- [ ] **Codex `pass` — PENDING** per-sub-sprint review (dispatch after commit).
- [x] Pilot / annotation / WP1-B / WP2 HELD; exp-82 WITHDRAWN; canonical
      pointer unchanged; no bot/runtime/prompt/reason/handover change.

## §9 Commit map

Separable commits, staged explicitly by file (no `git add -A`):
1. Phase 1 — `user_simulator.py`, `session_runner.py`, `trace/models.py`,
   `batch/executor.py` + sim/runner tests.
2. Phase 3 — `scoring/conditional_outcome.py`, `scoring/outcome_checks.py`,
   `case_spec/schema.py`, `case_spec/loader.py`,
   `conditional_outcome_adjudications.yaml` + `test_conditional_outcome.py`.
3. CaseSpec — the two PRIMARY `*.yaml` declarative blocks.
4. Handoff (this file).
