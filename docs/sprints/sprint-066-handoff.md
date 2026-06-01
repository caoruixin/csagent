---
title: Sprint 066 / S-Auto-11 / M-Auto-3 — Handoff
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: this file
last_reviewed: 2026-06-01
review_cadence: ad hoc
notes: >
  S-Auto-11 sub-sprint 1 of M-Auto-3 (Substrate-hygiene — clean the autoloop
  fitness signal). Layer: infra + eval_spec/harness; §7 stanza EXEMPT
  (harness/infra carve-out). Goal: make the autoloop's fitness measurement
  trustworthy + observable BEFORE the substrate fixes land. Shipped: (#3)
  per-iter eval trace persistence to autoloop/results/runs/exp-N/eval-results.json;
  (#4) loop.py infra-error detection (failed/empty suite OR LLM-deadline/
  service_degraded prevalence -> decision="error", skips Tier-0 scoring) for
  OQ-S65.7/8; (D1) SAFE user_simulator turn0/parse-retry hardening.
  KEY FINDINGS (both surfaced as OQs, NOT fixed — out of scope / hard-fenced):
  (1) the stated Java baseline 1183/1/0/2 is STALE — true baseline is
  1183/10/0/2; the 9 extra failures are pre-existing PhaseEvaluator
  `max_tool_steps` golden drift (skills YAML vs test goldens), NOT
  determinism-attributable, so the determinism config was NOT reverted; (2) the
  D1 "CONTRACT_VIOL_TURN0" target is mis-rooted — cs015/fg5q flake is bot-side
  `active_use_case missing_after_turns` (a first-turn LLM-deadline/give-up infra
  masquerade), not a user_simulator turn0 issue (both cases carry seed_messages,
  so turn0 was already deterministic). D1 direction "safe hardening + surface"
  + "proceed #3/#4 now" were human-authorized via AskUserQuestion. fence-#13
  NOT touched; scoring SHA held at 35305bd8...; full live smoke iter blocked by
  unset AUTOLOOP_META_LLM_* creds, so #3/#4 were live-validated against real eval
  output instead. Gates: Java 1183/10/0/2; eval_interactive 499 passed/4 failed
  (4 pre-existing); autoloop 276 passed; 17-fixture 31; scoring SHA unchanged.
---

## §0 Sub-sprint summary

- **Sub-sprint**: S-Auto-11 (sub-sprint 1 of M-Auto-3 — Substrate-hygiene).
  **Layer**: `infra` + `eval_spec`/harness. No customer-service-agent semantic
  decision changed. **§7 stanza EXEMPT** (harness/infra carve-out per §4.1;
  self-walked in the objective). **Local-Mac only**, branch `auto-loop-branch`.
- **Goal**: make the autoloop's fitness measurement trustworthy + observable
  *before* the substrate fixes (A1/B1/A2/A3) land — (a) mark LLM-deadline /
  `service_degraded` / failed-eval iterations as **infra-error** (not a Tier-0
  regression), (b) persist per-iter eval traces for §5.6/§11 review, (c) D1
  `user_simulator` robustness, (d) re-establish the post-`b351648` Java baseline.
- **Commits** (this sub-sprint, on `auto-loop-branch`):
  - `fba1c07` — D1 `user_simulator` turn0 robustness (safe hardening).
  - `70f659c` — per-iter eval trace persistence (#3) + infra-error detection (#4).
  - (handoff commit follows.)
- **Scope files touched**: `eval_interactive/eval_interactive/simulator/user_simulator.py`,
  `eval_interactive/tests/test_user_simulator.py`,
  `autoloop/autoloop/loop.py`, `autoloop/tests/test_loop.py`. No edits to
  `applier.py` / sandbox / meta_agent / skills / case_specs / server / the 4
  SHA-locked scoring files / governance docs / archives.
- **Final test gates**:
  - **Java**: `1183 / 10 / 0 / 2` (`mvn -q -pl server test`) — see §1 (true
    baseline; was stated `1183/1/0/2`).
  - **eval_interactive**: `499 passed, 4 failed` (`uv run python -m pytest`) —
    +14 D1 tests over the prior `485 passed, 4 failed`; all 4 failures
    pre-existing (see §2).
  - **autoloop**: `276 passed` (`uv run --extra dev pytest -q`) — +10 new
    loop.py tests over the prior `266`.
  - **17-fixture detector sweep**: `31 passed`.
  - **scoring SHA**: `35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`
    (UNCHANGED — fence-#13 not touched).

## §1 Java baseline (re-established)

Ran `mvn -q -pl server test` from repo root at HEAD (`49e5a85` + the two
S-Auto-11 commits; the S-Auto-11 edits touch no Java).

**Result: `Tests run: 1183, Failures: 10, Errors: 0, Skipped: 2`.**

The prompt's stated pre-`b351648` baseline (`1183 / 1 / 0 / 2`) is **stale**.
Breakdown of the 10 failures:

| failure | attributable to determinism? | disposition |
|---|---|---|
| `SystemPromptUserRequestedTiebreakerTest.systemPrompt_marksActiveUcTiebreakerExplicitly` | no (known OQ-S41.5 status quo) | STATUS QUO |
| `PhaseEvaluatorPlanTest.plan_discoverPhase_returnsDiscoverPlan` (exp `2` got `3`) | **no** | OQ-S66.1 |
| `PhaseEvaluatorPlanTest.plan_resolveFaqUc_returnsFullPlan` (exp `4` got `6`) | **no** | OQ-S66.1 |
| `PhaseEvaluatorResolveSkillIntegrationTest.faq_uc{A,B,C,D,E,F,FP}_composesGoldenPhasePlan` (×7; exp `4` got `6`) | **no** | OQ-S66.1 |

**Determinism-attributable regression: NONE.** `b351648` changed only
`ChatController` `USER_FACING_LLM_DEADLINE_MS` 30000→60000 and `LlmRequest` +
`LlmInvocationService` temperature 0.3→0 (verified via `git show --stat`). The
9 `PhaseEvaluator` failures are `maxToolSteps` golden-count drift:
`resolve_faq_grounded_answer.yaml max_tool_steps: 6` (test golden asserts `4`)
and `discover_triage.yaml max_tool_steps: 3` (test golden asserts `2`). The
skill-resource + test-file changes that drive these counts (`01770ac`,
`7871c62`, `49d48b1`) are all **ancestors of `b351648`**, and no phase/skill/
test code changed in `b351648`/`0c2d5a3`/`49e5a85` — so these 9 failures
**predate `b351648`** and have been red since ~Sprint 44–52.

**Action taken**: the step-1 STOP trigger (determinism-attributable test
regression) did **NOT** fire, so the M-Auto-2 determinism config was **NOT
reverted**. The 9 failures live on hard-fenced surfaces (skills YAML =
A2/A3 / S-Auto-13; Java test goldens = `server/src/test`) and are out of
S-Auto-11 scope. Surfaced as **OQ-S66.1** for a future sub-sprint (likely
A1/A2/A3) to reconcile the goldens vs the shipped `max_tool_steps`.

## §2 D1 — `user_simulator` turn0 robustness (safe hardening + root-cause correction)

**Root-cause correction (load-bearing).** The objective's D1 hypothesis
(turn0 falls through to a moonshot LLM call → flake) does **not** match the
code or the data:

- `generate_first_message` (turn0) was **already deterministic** for both the
  seed and no-seed branches (`seed_messages[0]`, else
  `form_context.description or user_goal_summary`) — it never called the LLM.
- Both target cases carry `seed_messages` (`cs015` 2 msgs, `fg5q` 2 msgs), so
  their turn0 was deterministic regardless.
- The real "CONTRACT_VIOL_TURN0" flake is a bot-side **`CONTRACT_VIOLATION:active_use_case`**
  (`reason=missing_after_turns`, `total_turns≈0`): the bot's first turn aborts
  before stamping a UC. `eval_interactive/.../trace/collector.py:367` raises this
  in strict mode when `turns and not active_use_case and not OOS`. This is the
  OQ-S65.7/8 "infra masquerading as a signal" phenomenon surfacing at the
  eval-trace-contract layer (a first-turn LLM-deadline / give-up). No
  `user_simulator.py` edit can drive it to 0.

This was surfaced mid-sprint; the human authorized (via AskUserQuestion)
**"safe hardening + surface"** for D1 and **"proceed #3/#4 now"**.

**Changes shipped** (`user_simulator.py`, simulated-USER only — bot untouched):

1. **N=3 corrective parse-retry** in `_call_llm`. Before: a single transport
   retry, and a parse failure silently coerced to raw text. After: up to
   `_MAX_SIMULATOR_ATTEMPTS=3` attempts covering both transport errors and
   unparseable output; on a parse failure the next attempt appends
   `_PARSE_RETRY_INSTRUCTION` (restates the exact JSON schema) so the model
   gets a concrete second chance instead of repeating the malformed shape.
   Budget-exhausted → lenient raw-text fallback (only a pure transport failure
   yields the canned line).
2. **Strict `_try_parse_simulator_response`** (returns `None` on failure) so the
   caller can distinguish malformed from valid; `_parse_simulator_response`
   retained as the lenient final fallback (all 10 original parse tests pass).
3. **Defensive non-empty turn0 guard** in `generate_first_message`
   (`description → user_goal_summary → topic_subject → neutral fallback`), so an
   all-empty CaseSpec can never emit an empty first message (which would itself
   produce a `missing active_use_case` violation). Switched the deprecated
   `goal_summary` alias to canonical `user_goal_summary`.

**Tests**: `test_user_simulator.py` 10 → **24** (added strict-parse,
retry-loop, transport-fallback, and turn0 preference/non-empty tests).

**CONTRACT_VIOL_TURN0 evidence**: target `3/24 → 0` is **not achievable via
D1** (bot-side). Per-case live confirmation on the real `20260601-015546`
bad_cases run: `cs015` and `fg5q` are flagged by `_case_is_infra_degraded`
(active_use_case contract-violation) — i.e. the flake is correctly recognized
as infra, not a simulator defect. Surfaced as **OQ-S66.2** for a bot-side
(A1/B1) or trace-contract (collector.py) sub-sprint.

**eval_interactive suite**: `499 passed, 4 failed`. The +14 D1 tests took
passes `485 → 499`; the 4 failures are **pre-existing** (verified by stashing
the S-Auto-11 changes and re-running them — all 4 still fail on the clean
tree) and unrelated to `user_simulator`:
`test_case_spec_overrides.py::{test_v2_schema_loads_cleanly,
test_smoke_review_report_tracks_smoke_set_and_overrides}`,
`test_corpus_lint.py::test_full_corpus_lints_clean_with_smoke_subset_flag`,
`test_agent_client_session_create_timeout.py::test_session_create_timeout_constants_widened_to_120s`
(`assert 90.0 == 60.0`). The stated `486/3` baseline is **stale by 1** (true
pre-change `485/4`) — surfaced as **OQ-S66.3**.

## §3 Per-iter eval trace persistence (#3 — R-overnight-eval-traces-not-persisted)

**Mechanism** (`autoloop/autoloop/loop.py`, no SHA-locked file touched). After
step 7 (eval), `_persist_eval_traces(results_root, suite_run_results,
iteration_id)` loads each suite's `results.json` (reachable via the
`eval_runner` symlink `runs/<id>/eval/<suite>` → `eval_interactive/results/<ts>/`)
and writes a **real consolidated copy** to:

```
autoloop/results/runs/exp-<N>/eval-results.json
```

Shape: `{iteration_id, generated_at, suites: {<name>: {exit_code, error_tail,
results_json, source_dir, results: <full results.json incl.
case_results[].per_turn_trace[]>, missing}}}`. Because it dereferences the
symlink into a real file, the per-turn traces survive both the volatile
`eval_interactive/results/<ts>/` dir and `applier.cleanup`. `result.eval_traces_path`
is also persisted in the experiments_log row. Best-effort (never raises →
never crashes an iteration). **`eval_runner.py` was NOT edited** — the loop
consumes what the eval already writes (fence-#13 untouched).

**Read-back sample** (live, against the real `20260601-015546` run):
`_persist_eval_traces` wrote `eval-results.json` with `missing=False`,
`source_dir` set, and a readable `per_turn_trace` (case
`alice_uc_a_uc_h_misclass`, 4 turns). Roundtrip + missing-suite unit tests
also pass (`test_persist_eval_traces_roundtrip`,
`test_persist_eval_traces_marks_missing_suite`).

## §4 OQ-S65.7/8 — infra-error detection (#4)

**Mechanism** (`autoloop/autoloop/loop.py`). After eval + trace persistence,
`_assess_infra_error(suite_run_results, config)` classifies the iteration as
infra-error on two independent triggers:

1. **Failed / empty suite** — a real `SuiteRunResult` with a non-zero
   `exit_code`, OR a missing/unreadable `results.json` (the eval produced no
   evidence). `EvalRunnerTimeoutError` from step 7 is likewise classified
   infra-error.
2. **Pervasive LLM-deadline / `service_degraded` prevalence** — fraction of
   cases with an infra signal `>= fitness.infra_error_degraded_fraction`
   (default **0.5**, configurable). Per-case signal (`_case_is_infra_degraded`):
   `status == "ERROR"`; `status == "CONTRACT_VIOLATION"` with
   `contract_violation.field == "active_use_case"` (the first-turn-abort
   masquerade); `escalation_reason ∈ {service_degraded, runtime_error_threshold}`
   (the runtime's `LlmDeadlineExceededException` give-up family per
   `hard_checks._ESCALATION_REASON_FAMILY`); or a `failure_tags` entry containing
   `ReadTimeout`/`Timeout`/`Deadline`/`service_degraded`.

On detection the iteration is reported as **`decision="error"`** (the existing
non-fitness bucket — `cli.py` already documents "errors are infra /
unrecoverable, NOT keep/discard verdict") **plus** distinct `infra_error=True`
and `infra_error_reason` fields persisted in the iter row, and the loop
**returns before `tier_evaluator.evaluate`** — so infra degradation can never
be scored as a Tier-0 fitness regression.

**Negative control (masquerade fence)**: only transport/deadline/first-turn-abort
signals count. A genuine fitness FAIL (e.g. `escalation_reason=user_requested`,
or a non-`active_use_case` contract violation) is **not** an infra signal and
still reaches the tier evaluator as a real regression
(`test_assess_infra_error_genuine_fails_not_infra`).

**Smoke evidence**: the full live smoke iter is **blocked** — `AUTOLOOP_META_LLM_API_KEY`
/ `AUTOLOOP_META_LLM_BASE_URL` are unset (commented out in `.env.local`), so a
real `autoloop run` short-circuits at step 2 (`proposer_llm_returned_invalid_json`)
and never reaches step 7; running it would validate nothing about #3/#4 and I
did not guess the user's credentials. Instead #3/#4 were validated:

- **Unit + integration** (autoloop `276 passed`): 10 new tests incl.
  `test_infra_error_iter_marked_error_and_skips_tier_eval` — a full
  `run_one_iteration` with an infra-degraded eval map asserts
  `decision=="error"`, `infra_error=True`, `tier_evaluator.evaluate`
  **not called**, and the persisted experiments_log row carries `infra_error`.
- **Live against real eval output** (`20260601-015546`): `_case_is_infra_degraded`
  flags exactly the 3 infra cases (`cs015`, `fg5q`, `iwzx`); `_assess_infra_error`
  returns `False` at the default 0.5 (3/12 = 0.25 — correctly does NOT poison a
  mostly-valid iter) and `True` at a 0.05 threshold (positive trigger fires on
  real signals).

**To complete the live smoke** (deliver-agent / human): set
`AUTOLOOP_META_LLM_API_KEY` + `AUTOLOOP_META_LLM_BASE_URL` (uncomment
`.env.local:48-49`), then `scripts/sprint-064-step4-smoke-iter.sh 1` on a clean
committed tree; confirm a Step-9 verdict row + readable
`runs/exp-<N>/eval-results.json`.

## §5 fence-#13 disposition

**fence-#13 NOT touched.** The 4 SHA-locked scoring files
(`tier_evaluator.py`, `eval_runner.py`, `baseline_loader.py`, `gaming.py`) are
unchanged. Both #3 and #4 are implemented purely in `loop.py` orchestration
(consuming `eval_runner`'s existing output). Scoring SHA verified **held** at
`35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`. No SHA
rebaseline. No per-sub-sprint Codex trigger fired (§4.3 #1/#2/#3/#4 all
negative) — Codex remains **milestone-shared** at M-Auto-3 close.

## §6 OQs surfaced + R-items

**OQs surfaced**:

- **OQ-S66.1** — Java baseline drift. True baseline is `1183/10/0/2`, not the
  stated `1183/1/0/2`. 9 pre-existing `PhaseEvaluator` `max_tool_steps`
  golden-count failures (`resolve_faq_grounded_answer.yaml` 6 vs golden 4;
  `discover_triage.yaml` 3 vs golden 2). Hard-fenced (skills YAML = A2/A3 /
  S-Auto-13; Java goldens). Reconcile goldens-vs-shipped in a future sub-sprint;
  pick one source of truth.
- **OQ-S66.2** — D1 root-cause correction. `cs015`/`fg5q`
  `CONTRACT_VIOLATION:active_use_case` is a bot-side first-turn-abort infra
  masquerade, not a `user_simulator` turn0 issue. Real fix lives in bot routing
  (server Java, A1/B1) and/or trace-contract leniency (`collector.py`).
- **OQ-S66.3** — eval_interactive baseline drift. True pre-change baseline is
  `485 passed / 4 failed`, not the stated `486/3`. 4th failure
  `test_session_create_timeout_constants_widened_to_120s` (`assert 90.0==60.0`)
  is a pre-existing timeout-constant drift.
- **OQ-S66.4** (optional / needs authorization) — consider eval-layer
  trace-contract leniency: treat `active_use_case missing_after_turns` with
  `total_turns≈0` as an infra-error / lenient outcome in `collector.py` rather
  than a hard `CONTRACT_VIOLATION` — mirrors #4 at the `eval_interactive` layer
  and would stop the masquerade at its source. Outside D1's in-scope file;
  flagged for deliver-agent + human.

**R-items** (deliver-agent to flip in `action_bank.md` at milestone close — not
edited here per scope discipline):

- `R-overnight-eval-traces-not-persisted` → **addressed by #3** (CLOSE
  candidate; per-iter traces now persisted to `runs/exp-N/eval-results.json`).
- `OQ-S65.7` / `OQ-S65.8` (infra-degradation must not masquerade as fitness) →
  **addressed by #4** (infra-error classification at the autoloop layer).

## §7 Self-check tick-off

- [x] Java baseline re-established + recorded (`1183/10/0/2`); the 9-failure
      delta is pre-existing, NOT determinism-attributable → determinism config
      NOT reverted; surfaced as OQ-S66.1 (no silent revert).
- [x] D1 turn0 deterministic no-seed branch confirmed already-present +
      hardened (non-empty guard); N=3 corrective parse-retry added.
      `CONTRACT_VIOL_TURN0 0/24` NOT achievable via D1 (bot-side) — root cause
      corrected + surfaced (OQ-S66.2); human-authorized "safe hardening + surface".
- [x] Per-iter eval traces persisted to `autoloop/results/runs/exp-<N>/eval-results.json`;
      a real `per_turn_trace` read back (live, on `20260601-015546`).
- [x] loop.py infra-error detection marks LLM-deadline/`service_degraded`/
      failed-eval iters `decision="error"` + `infra_error_reason`, skipping
      `tier_evaluator` (integration test asserts it is not called); negative
      control proves genuine FAILs still reach the tier evaluator.
- [x] Preferred loop.py orchestration; `eval_runner.py` NOT edited; fence-#13
      untouched; scoring SHA held at `35305bd8...`; no per-sub-sprint Codex
      trigger.
- [x] eval_interactive `499 passed / 4 failed` (4 pre-existing; +14 D1 tests) +
      autoloop `276 passed` (+10 new) + 17-fixture `31 passed` preserved.
- [x] No edits to applier.py / sandbox / meta_agent / skills / case_specs /
      server semantic logic / governance docs / archives.
- [~] Smoke iter on a clean committed tree: **blocked** by unset
      AUTOLOOP_META_LLM_* creds (would short-circuit at proposer). #3/#4
      live-validated against real eval output instead; full smoke deferred to
      the human with the exact unblock command (§4). No `git add -A`;
      local-Mac only.
- [x] Handoff §0–§7 filled.
