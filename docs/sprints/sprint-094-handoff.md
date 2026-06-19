---
title: "Sprint 094 / S-Auto-40 (M-Auto-7) — WP1-A handoff: measurement contract → conditional outcome acceptance"
doc_tier: sprint-archive
status: current
implementation_status: implemented
source_of_truth: this file (dev handoff); code under eval_interactive/
last_reviewed: 2026-06-19
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  WP1-A CLOSED 2026-06-19 at clean HEAD 052cc73b (human-accepted). Scope:
  validates measurement + acceptance INFRASTRUCTURE only — it does NOT establish
  a conditional baseline and does NOT demonstrate improved bot behaviour. Phase 1
  (infra) + Phase 3 (eval_spec) landed; Codex §4.1 approve; Phase 2 bounded
  real-LLM run evidence floor MET (42/42 draws); 4/4 CONDITIONAL_ELIGIBLE traces
  human-REJECTED. Pilot / WP1-B / WP2 / objective-alignment annotation /
  auto-triage follow-up remain HELD; exp-82 WITHDRAWN; canonical baseline
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
| Phase 2 — bounded real-LLM validation | `infra` | **DONE — evidence floor MET** | run `results/wp1a-phase2-measurement-20260619` (42/42 draws, 0 err); SATISFIED=6, post-help UNRESOLVED=19; provenance/alignment 0 violations. |
| Closure-quality adjudication | `eval_spec` | **DONE — 4/4 REJECTED** | human verdicts recorded in `conditional_outcome_adjudications.yaml`; no trace flipped to PASS. |
| Phase 3 — declarative three-state acceptance | `eval_spec` | **DONE + zero-LLM-tested** | 19 §3.3 replay tests, all green. |
| Baseline migration | — | **DECIDED + specified** (execution waits on Phase 2) | see §6. |
| Codex per-sub-sprint review | — | **PASS (`approve`)** | §4.1 nine-question kernel, read-only `codex exec` (high); verdict verbatim in `docs/codex-findings.md`. |

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

## §5 Phase 2 — bounded real-LLM validation (DONE — evidence floor MET)

**Run:** `eval_interactive/results/wp1a-phase2-measurement-20260619/draws/`
(gitignored). HEAD `b6329041`; backend SHA = HEAD (no Java changed); simulator
signal schema_version 1; bot=deepseek-v4-flash, sim=moonshot-v1-32k, temp 0.0;
backend `localhost:8080` under `caffeinate`. Provenance hashes pinned
(`conditional_outcome.py 7e3609d0…`, the 2 CaseSpecs, adjudication registry).

**§5.9 pre-flight GO:** fresh boot (`Started CsAgentApplication`, Flyway
connected), health UP, Postgres+Redis up, no proxy env; n=1 smoke proved the
instrumentation live (3 provenance-complete signals; evaluator
`CONDITIONAL_ELIGIBLE [closure_marker_turn=2, adjudicated=False]`).

**Bounded set (Sprint 093 precedent):** 2 PRIMARY ×11 + `generic_policy` (neg-
control) ×5 + `lookup_failed` / `fp_loaded_moderation` (neighbors) ×5 +
`cs11g02_uc_d_explicit_distress` (genuine-escalation control) ×5 = **42 draws,
42 valid, 0 errors, 0 timeouts**.

**Evidence floor — MET:**

| criterion | required | observed |
|---|---|---|
| SATISFIED | ≥1 | **6** |
| post-help UNRESOLVED | ≥3 | **19** |
| UNKNOWN / neutral samples | present | working=30 signals; UNKNOWN-terminal=6 (14.3%) |
| handover/bot_ended retain state | not all-lost | **6/6** |
| same-call provenance violations | 0 | **0** (80/80 signals tagged) |
| turn-alignment violations | 0 | **0** |
| no back-inference / leakage | — | user_state varies independently of outcome/reason (UNRESOLVED appears under resolved, user_requested, faq_miss, turn_budget alike) |

**Zero-LLM anti-widen replay over the 22 real PRIMARY traces:** 15 false-resolve
→ FAIL · 4 escalate+UNRESOLVED → CONDITIONAL_ELIGIBLE (not PASS) · 1
SATISFIED+resolve → PASS · 1 UNKNOWN+resolve → PASS · 1 blank-outcome → FAIL.
**No improper PASS into the accepted branch; no PRIMARY escalate draw
`case_passed=True` (registry empty).** Analyzer: `eval_interactive/analyze_wp1a_phase2.py`.

### §5a Closure-quality adjudication (4/4 REJECTED)

The 4 `CONDITIONAL_ELIGIBLE` escalate traces (all `cs_uc_a_loaded_listing`) were
reviewed against the closure criterion (version `ba55899118d0`) and **human-
REJECTED**, recorded in `eval_interactive/case_specs/conditional_outcome_adjudications.yaml`
(`verdict: reject_escalation`):

- `30878877…` — UC-B misclass + generic tips (faq_miss escalation).
- `f3a4fff1…` — UC-A correct, references listing (title+LIVE, pre-loaded context
  is valid provenance) but **listing context not used substantively** (reverts to
  generic FAQ) + **budget-driven escalation** (turn_budget_exhausted). Borderline.
- `8841784c…` — UC-B misclass + generic tips (user_requested).
- `a5bcf38e…` — UC-B misclass + generic "Creating Effective Ads" article.

`reject_escalation` is an auditable record only — the evaluator flips to PASS
solely on `accept_escalation`, so all 4 stay non-PASS (already `case_passed=False`
independently). Unit test `test_reject_escalation_verdict_does_not_flip_to_pass`
locks this. **No conditional baseline formed; no re-bless; canonical pointer
unchanged; pilot HELD.**

Three failure clusters surfaced (bot-behaviour, measurement-only — not a WP1-A
fix): UC-A→UC-B misclass; listing context ignored/superficial; budget-driven
escalation before grounded resolution →
`docs/diagnostics/failure-clusters-uc-a-listing-2026-06-19.md`. Auto-adjudication
follow-up → `R-conditional-adjudication-auto-triage` (`docs/action_bank.md` §5).

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
- [x] **Phase-2 evidence floor MET** — 42/42 draws; SATISFIED=6, post-help
      UNRESOLVED=19, UNKNOWN/neutral present, handover 6/6 retains state, 0
      provenance/alignment violations, no leakage. 4/4 CONDITIONAL_ELIGIBLE
      human-REJECTED (no PASS flip).
- [x] Phase-3 declarative, no case-id/free-text; three-state matrix + §3.3
      battery all hold on zero-LLM replay.
- [x] CONDITIONAL_ELIGIBLE never auto-PASS; adjudication artifact
      versioned/auditable; evaluator reads it, no runtime human call.
- [x] Baseline migration decided (§6); canonical pointer unchanged.
- [x] 2 PRIMARY only; neighbors read-only; generic-policy resolve-only.
- [x] Python no new regression (620 passed; 6 failures proven pre-existing).
      Java suite untouched (no Java changed).
- [x] **Codex `pass`** — §4.1 nine-question kernel `approve`, recorded verbatim
      in `docs/codex-findings.md` (read-only `codex exec`, high effort).
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
5. Codex verdict record (`codex-findings.md`).
6. Phase-2 adjudication + reject test + analyzer/driver tooling.
7. Phase-2 docs (handoff update, action_bank follow-up, failure clusters).

## §10 Close — WP1-A CLOSED 2026-06-19 at HEAD `052cc73b` (human-accepted)

### Scope boundary (binding)

WP1-A validates **measurement + acceptance infrastructure**: same-call
`user_state` capture, the declarative three-state conditional acceptance,
anti-hardcode (Codex `approve`), the bounded real-LLM measurement run, the
zero-LLM anti-widen replay, and auditable rejection adjudication. **WP1-A does
NOT establish a conditional baseline and does NOT demonstrate improved bot
behaviour.** The bot-behaviour failures it *measured* (UC-A→UC-B misclass,
non-substantive listing use, budget-driven escalation) are recorded as held
evidence for the M-Auto-7 entity-context work, not fixed here.

### Pre-existing full-suite failures (6) — identified, linked, NOT introduced by WP1-A

All six are identical on the clean pre-WP1-A tree (verified this session by
stash-and-rerun); neither test file was touched by any WP1-A commit
(`git diff f64ad4b2^ 052cc73b` excludes both).

| stable node id | kind | cause | prior evidence |
|---|---|---|---|
| `tests/test_s_eval_1_schema_and_scoring.py::TestBackwardCompatLoad::test_alice_bad_case_loads_unchanged` | FAILED | frozen S-Eval-4 (Sprint 45) anchor asserts `bad_cases == 12`; suite has since grown to **17** (later sprints added `cs_uc_a_*` / `cs_uc_fp_*`). Stale inventory anchor. | The "1 pre-existing unrelated CaseSpec-inventory failure" recorded at Sprint 092 close (`docs/10-handoff.md` §0). Test added Sprint 077 (`330f9b1e`). |
| `tests/test_rescore_s_auto_38_full_baseline.py::test_composite_delta_is_the_five_gate_cases` | ERROR | module `materialized` fixture: `analysis/rescore_s_auto_38_full_baseline.py:run_rescore` → `_parse_case_spec` `KeyError: 'form_context'` re-scoring the S-Auto-38 June-8 baseline scratch. Harness/fixture-data condition. | Harness added Sprint 092 / S-Auto-38 (`2f5c5e9e`). |
| `tests/test_rescore_s_auto_38_full_baseline.py::test_soundness_no_regressions` | ERROR | same `materialized` fixture setup error. | same. |
| `tests/test_rescore_s_auto_38_full_baseline.py::test_reconciliation_layer0_vs_composite` | ERROR | same. | same. |
| `tests/test_rescore_s_auto_38_full_baseline.py::test_old_reconstruction_unchanged_cases_reproduce_june8` | ERROR | same. | same. |
| `tests/test_rescore_s_auto_38_full_baseline.py::test_native_baseline_loader_clean` | ERROR | same. | same. |

WP1-A delta to the suite: **+1 pass** (`test_reject_escalation_verdict_does_not_flip_to_pass`) and the Phase-1/3 test additions → **621 passed**, same 6 failures.

### Pilot-resume readiness (M-Auto-7) — PREP only, not launched

Preconditions in place to resume the M-Auto-7 pilot **from the unchanged
canonical baseline** using the new measurement contract:

- ✅ Same-call `user_state` instrumentation live + provenance-clean (Phase 2).
- ✅ Declarative conditional acceptance + adjudication registry in place;
  evaluator reads it (no runtime human call).
- ✅ Canonical baseline pointer **unchanged** (`autoloop/config.yaml:baseline_dir`
  still the M-Auto-7 pre-pilot baseline) — **no re-bless**.
- ✅ Backend boots clean at HEAD (no Java changed); §5.9 pre-flight pattern proven.

Held until pilot evidence shows them blocking: **exp-82 stays WITHDRAWN** (do not
revive); **no re-bless**; **WP1-B, WP2, objective-alignment annotation, and the
`R-conditional-adjudication-auto-triage` auto-triage follow-up remain HELD**. The
pilot launch itself is a deliver/human-gated action — not performed here.
