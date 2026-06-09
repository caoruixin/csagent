---
title: Sprint 072 / S-Auto-16 dev handoff — autoloop k-of-n fitness-measurement core (INERT)
doc_tier: sprint-archive
status: archived
implementation_status: implemented
source_of_truth: autoloop/autoloop/scoring/ (code) + autoloop/config.yaml
last_reviewed: 2026-06-03
review_cadence: ad hoc
supersedes: []
superseded_by: null
notes: >
  First sub-sprint of M-Auto-4 (Autoloop Fitness Measurement Reliability).
  MEASUREMENT sprint — no bot capability change (no skill/prompt/routing/
  CaseSpec edit). Ships the full k-of-n majority + aggregation + per-attempt
  persistence machinery but commits it INERT (samples_per_case=1, byte-
  identical to pre-sprint). Live flip to n=3 + symmetric majority gate +
  baseline re-bless are DEFERRED to S-Auto-17; §5 escalation-family spec to
  S-Auto-18. Codex review is per-sub-sprint REQUIRED (fence-#13 SHA-locked
  scoring edits) and folds into the M-Auto-4 milestone-shared close prompt —
  NOT dispatched by the dev agent.
---

# Sprint 072 / S-Auto-16 — Autoloop k-of-n fitness-measurement core (INERT)

Milestone: **M-Auto-4 — Autoloop Fitness Measurement Reliability** (lead
sub-sprint). Layer (primary): **`infra`** — fitness measurement reliability
in `autoloop/autoloop/scoring/`. No bot-runtime semantic change.

## §0 — Fallback diagnostic + primary-only enforcement finding

**Route chosen (cheapest faithful, additive):** provider comparability is
derived from the per-call `llm_calls[].model` + `llm_calls[].callType`
fields that eval-interactive ALREADY persists in each suite's `results.json`
— NOT from server-log parsing and NOT from a new response-object field.
Comparability is scoped to the **agent `chat` callType** (the call
`FallbackLlmClient` wraps); the primary model is the modal `chat`-callType
model across the suite run, and a true fallback is a `chat` call whose model
differs from that primary (`fallback_count>0`). This needed **zero server
change** and, critically, **does not touch `FallbackLlmClient`'s
fallback-decision logic** (hard fence respected) — it only reads telemetry
that already exists. `request_id` is not present in `llm_calls`, so that
optional per-attempt field is recorded as null (consumers tolerate absence).

**Critical detection lesson (recorded so it does not recur):** the eval
runs TWO model populations by design — `chat`=`deepseek-v4-flash` (agent)
and `rerank`=`kimi-k2.6` (a separate reranker). An initial modal-over-ALL-
callTypes implementation picked kimi (rerank calls outnumber chat) and
misflagged ~93% of `chat`/deepseek attempts as `provider_mixed`, dropping
nearly every case to `non_comparable`. The fix (committed) scopes the modal
+ fallback detection to `callType=="chat"`. Numbers below are post-fix,
re-derived from the SAME on-disk attempt results.json (no LLM re-run).

**Primary-only enforcement:** an attempt served by the fallback
(`fallback_count>0`) is marked `provider_mixed` and dropped from the vote;
a transport/deadline/first-turn-abort degraded attempt is marked
`infra_error` and dropped. Both are excluded from BOTH the numerator and
denominator. Retry prefers re-running the primary (`attempt_retry_cap=2`)
to reach `min_valid_attempts=3`; a case still short is `non_comparable`/
`infra_error` and NEVER gates.

**Connect to existing infra-error path (no double-count):** the per-attempt
`infra_error` predicate mirrors `autoloop.loop._INFRA_ESCALATION_REASONS`
(`service_degraded`, `runtime_error_threshold`) + the loop's
`_case_is_infra_degraded` signal. A `service_degraded`/`runtime_error_threshold`
draw is dropped from the vote here, consistent with the loop demoting an
infra-degraded *iteration* to `decision="error"` — it is never scored as a
escalation outcome on both paths.

**Frequency finding (from the §6.1 real-LLM N=5 run; 5 initial + 2 retry
passes = 7 attempts over bad_cases 12 / anchor_outcome 12 / shadow 22):**
Across **322 agent `chat` attempts**, the fallback was engaged **0 times
(0.0%)** — the chat agent served every comparable call from the primary
(`deepseek-v4-flash`). `infra_error` accounted for **14/322 (4.3%)**
attempts (transport/deadline/first-turn-abort), already on the loop's
infra-error path and excluded from the vote (not double-counted). **26
cases showed escalation-reason jitter across attempts** (e.g.
`faq_miss_threshold_exceeded` ↔ `turn_budget_exhausted` ↔ `user_distress`),
and on EVERY one of those `fallback_count==0` — so the jitter is **genuine
backend/LLM non-determinism, NOT fallback or provider degradation** (directly
answering the §1.3 (a)-vs-(b) question: it is (a)). This non-determinism is
exactly what k-of-n majority is designed to absorb.

**STOP-and-surface evaluation:** NOT triggered. The STOP condition is
"fallback frequent enough that primary-only retry can't reach
`min_valid_attempts=3` within a sane retry cap." With 0% fallback and only
4.3% infra_error, primary-only comfortably holds the floor: bad_cases and
anchor_outcome had 0 non-comparable cases (all 12 reached ≥3 valid draws);
shadow had 2/22 non-comparable, both from `infra_error` (not fallback), and
those correctly do not gate. Primary-only fitness is satisfiable today.

## §6.1 — Variance reduction (PRIMARY acceptance evidence; real-LLM, §5.7)

Run: `samples_per_case=5` over the v1 fitness suites via the new n-loop into
a SCRATCH results dir (`autoloop/results/_scratch_s_auto_16_variance/`), on
a clean committed tree, backend up, real LLM (deepseek-v4-flash agent). 7
attempts landed per suite (5 initial + 2 retry passes). Two metrics, both
single-draw vs k-of-3-majority, computed over the per-case valid draws:

- **Bootstrap suite-level std** (apples-to-apples, B=2000): resample k draws
  per case with replacement, take the suite pass-rate; k=1 = single-draw
  policy, k=3 = majority policy. Same estimator both sides → fair.
- **Analytic per-case mean variance** (bootstrap-noise-free): mean over
  cases of `p(1-p)` (single) vs `g(p)(1-g(p))`, `g(p)=3p²-2p³` (3-majority).

| suite | mean pass-rate | bootstrap single std | bootstrap maj-3 std | bootstrap reduction | analytic per-case var single → maj-3 | analytic reduction |
|---|---|---|---|---|---|---|
| bad_cases (12) | 0.524 | 0.1124 | 0.1038 | **−7.6%** (0.924) | 0.1497 → 0.1325 | **−11.4%** |
| anchor_outcome (12) | 0.369 | 0.0768 | 0.0729 | **−5.0%** (0.950) | 0.0714 → 0.0640 | **−10.4%** |
| shadow (22) | 0.157 | 0.0447 | 0.0381 | **−14.7%** (0.853) | 0.0388 → 0.0304 | **−21.5%** |

**Interpretation:** under a fair same-method comparison, the k-of-3 majority
**reduces suite-level run-to-run variance in all three suites** (−7.6% /
−5.0% / −14.7% bootstrap), and the bootstrap-noise-free analytic per-case
variance confirms a monotonic reduction (−11.4% / −10.4% / −21.5%). The
reduction is smallest for bad_cases because its mean pass-rate (0.52) sits
nearest p=0.5 — the regime where a 3-sample majority provides the least
variance reduction (it provides none exactly at p=0.5) — and largest for
shadow (mean 0.16, far from 0.5). NOTE on methodology: an initial draft
compared a 7-point empirical single-std against a 2000-sample bootstrap
majority-std; that mismatch spuriously showed bad_cases +6.7%. The
same-method bootstrap above corrects it; both honest framings are retained
in `variance_report*.json` (scratch, not committed).

### §6.3 — flaky-vs-stable distinguishability

The harness labelled each case from its 7 draws (per-suite counts):

| suite | flaky (non-unanimous) | stable (unanimous) | non_comparable |
|---|---|---|---|
| bad_cases | 8 | 4 | 0 |
| anchor_outcome | 4 | 8 | 0 |
| shadow | 4 | 16 | 2 (infra_error) |

`aggregate_status` distribution confirms the gate-relevant split: e.g.
bad_cases = {flaky_pass: 6, flaky_fail: 2, stable_fail: 2, stable_pass: 2}.
A `flaky_pass` case (passes in the majority, fails in the minority) keeps
`majority_passed=True` → does NOT gate; a `stable_fail` / `flaky_fail` case
(fails in the majority) has `majority_passed=False` → gates. That bad_cases
is 8/12 flaky is the direct empirical justification for k-of-n: a single
draw there is a coin-flip-ish signal that the majority stabilises.

The gate follows `majority_passed`: a case that fails in the MINORITY of
valid attempts (flaky_pass) keeps `majority_passed=True` and does NOT gate;
a case that fails in the MAJORITY (stable_fail / flaky_fail) has
`majority_passed=False` and gates. Demonstrated by
`tests/test_aggregate.py::test_majority_up_2_of_3_minority_fail_does_not_flip`
vs `::test_majority_down_2_of_3_majority_fail_gates` and at the verdict
level by `tests/test_tier_evaluator.py::test_majority_l1_flaky_minority_fail_does_not_gate`
vs `::test_majority_l1_stable_majority_fail_gates`.

### §6.7 — measurement integrity

In the real-LLM run: 322 `chat` attempts, 0 `provider_mixed` (no fallback to
drop today), 14 `infra_error` dropped, 2 shadow cases `non_comparable`
(<3 valid draws, from infra_error) and correctly NOT gating. The drop /
exclusion mechanics themselves (provider_mixed dropped, infra_error dropped,
<min_valid → non_comparable) are exercised deterministically in the unit
tests below since the live run had no fallback to exercise the
provider_mixed path end-to-end.

Primary-only enforced (fallback attempts dropped `provider_mixed`);
`<min_valid_attempts=3` valid draws → `non_comparable`/`infra_error`, never
gates. Verified in unit tests (`test_eval_runner.py::test_majority_provider_mixed_dropped`,
`::test_majority_infra_error_dropped`, `::test_majority_insufficient_non_comparable`).

### Measurement-only run isolation (acceptance invariant)

CONFIRMED the N=5 variance run:
- (a) did NOT participate in any `keep`/`discard` decision — it called
  `run_v1_fitness_suite` directly (no `tier_evaluator.evaluate`, no loop
  orchestrator, no git commit);
- (b) did NOT update/overwrite the baseline — it wrote to a scratch dir;
  `config.fitness.baseline_dir` (`eval_interactive/results/m-auto-1b-baseline-20260529`)
  is UNCHANGED;
- (c) did NOT trigger or feed an overnight loop gate.
The committed loop stays at `samples_per_case=1`.

## §11 — scoring SHA recompute + Codex deferral + fence-#13 authorization

**`scoring_code_baseline_sha` (5-file set):**
- Files hashed (FIVE; widened from four by adding `aggregate.py`):
  `tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py` +
  `gaming.py` + `aggregate.py`.
- old → new:
  `35305bd84402ac455b126be5bcf8b2cb3b3fa55e3399f2c02443ea74bd8704e8`
  → `bb3ced3d37b9d39dd527a0f5be4dcbb0bfea41370ecfccb0b0d317b5c6a26e8d`.
  (An intra-sprint value `d62537d…` existed between the machinery commit and
  the §0 chat-callType detection fix; the committed final value is `bb3ced3d…`.)
- Reproduce at this HEAD:
  `uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"`.
- Verified `gaming._check_scoring_code_drift(config=...)` returns NO flag at
  this HEAD (config value matches the recomputed hash). The intended
  fence-#13 `scoring_code_drift` observation is resolved by recording the
  new baseline; `suspect_baseline_manipulation` does not fire because
  `baseline_dir` is unchanged.

**Codex review (per-sub-sprint, REQUIRED — fence-#13):** this sub-sprint
edits 3 of the now-5 SHA-locked scoring files (`eval_runner.py`,
`tier_evaluator.py`, `gaming.py`) and adds `aggregate.py`, tripping the
fence-#13 hard-fenced-surface trigger. Per the contract, the dev agent does
**NOT dispatch Codex**; the per-sub-sprint review folds into the
deliver-authored M-Auto-4 milestone-shared close prompt as a
validation/sign-off pass (not an authorship-provenance source). **Deferred
to milestone-shared close.** Fence-#13 SHA-lock edits are authorized by the
sub-sprint contract §4 (Codex review plan) + §5 (#2/#5).

## §12 — self-classification

- **Target failure layer:** `infra` (fitness measurement reliability:
  sampling / aggregation / per-attempt persistence + additive provider-
  metadata capture). No `eval_spec` change.
- **Tier-0 invariant:** adds none. Preserves existing Tier-0 safety families
  (PII, escalation_compliance, critical_policy, phase_transition) at current
  strictness; the L0 change is a measurement decision (stable-reproduction
  across k-of-n) — no family removed, no threshold lowered.
- **Semantic hardcode:** none. No skill/prompt soft field, no keyword/regex/
  enum routing. Majority aggregation is pure cardinality over existing
  pass/fail signals; primary-only keys on `actual_model`/`fallback_count`,
  not content.
- **Generalization coverage:** measurement sub-sprint — evidence is the
  repeated-baseline variance metric (§6.1) + flaky-vs-stable
  distinguishability (§6.3), not case-family counts. Shadow gate (L4) stays
  active + firewalled; only the aggregate majority crosses to the loop, role
  unchanged.

## Test / eval evidence

- **autoloop pytest:** 305 passed (baseline 276; +29 new:
  `test_aggregate.py` 14, `test_eval_runner.py` +8, `test_tier_evaluator.py`
  +7). No regression.
- **eval_interactive pytest:** 503 passed / 0 failed under `uv run`
  (baseline 503/0 — no regression).
- **Java:** NOT rebuilt — §0 took the no-server-change route (provider
  metadata from existing `llm_calls[].model`), so no Java source touched and
  the `1213/1/0/2` baseline (sole failure
  `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5) is untouched.
- **n=1 byte-identical evidence:** committed `samples_per_case=1` →
  `run_v1_fitness_suite` takes the single-pass branch (no aggregation file,
  no injected fields); `tier_evaluator._effective_case_passed` falls back to
  single-draw `case_passed`; the 20 pre-existing tier_evaluator tests + 13
  pre-existing eval_runner tests pass UNCHANGED, and
  `test_tier_evaluator.py::test_n1_unanimous_majority_reproduces_single_draw_verdict`
  proves a unanimous-comparable aggregated verdict == the single-draw verdict.

## Commit map (per §8.6 step; revert-per-step)

- `#2` aggregate.py + test_aggregate.py
- `#1+#3` eval_runner n-loop + per-attempt persistence + test_eval_runner.py
- `#4` tier_evaluator majority + L0 stable-reproduction + test_tier_evaluator.py
- `#5` config.yaml knobs (inert) + gaming.py 5-file SHA + recomputed SHA
- `#0 fix` eval_runner chat-callType-scoped provider detection +
  test_eval_runner regression test + config SHA recompute (post-§6.1 finding)

Deliver-agent-owned files (`docs/milestone_objective.md`,
`docs/sprint_objective.md`, `compact/sprint-072-dev-prompt.md`) are NOT
staged by the dev agent — bundled by the human at close.

## Scope holds carried forward

- Baseline re-bless + live `samples_per_case=3` flip → **S-Auto-17**.
- `§5 acceptable_escalation_families` (escalation-tier spec) → **S-Auto-18**.
