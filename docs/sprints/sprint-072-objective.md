---
title: Sprint 072 / S-Auto-16 — §0 fallback diagnostic + autoloop k-of-n fitness core (committed inert; M-Auto-4 sub-sprint 1 of 3)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-03
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-071-objective.md]
superseded_by: null
notes: >
  M-Auto-4 / Sprint 072 / S-Auto-16. FIRST sub-sprint of the Autoloop Fitness
  Measurement Reliability milestone (`docs/milestone_objective.md`). Implements
  proposal §8.6 rollout steps 1–4 from
  `docs/proposals/autoloop_fitness_measurement_reliability.md`:
  (§0) the time-boxed DeepSeek→Kimi fallback diagnostic + primary-only fitness
  enforcement; (2) `aggregate.py` pure majority helper; (3) `eval_runner` n-loop
  + per-attempt persistence; (4) `tier_evaluator` reads the aggregated majority
  signal. The baseline re-bless (§8.4) and the §5 escalation-spec change are
  DEFERRED to S-Auto-17 / S-Auto-18.

  **THE LOAD-BEARING SCOPE BOUNDARY (read first):** `tier_evaluator`'s majority
  gates compare candidate-vs-baseline. The baseline re-bless is DEFERRED, so the
  baseline is still a single noisy draw this sub-sprint. Flipping
  `samples_per_case>1` in the LIVE loop against a single-draw baseline is
  asymmetric and could MANUFACTURE spurious regressions (baseline lucky-pass vs
  candidate honest-majority-fail). Therefore this sub-sprint ships the full
  k-of-n + aggregation + majority-input machinery but **commits it INERT:
  `samples_per_case: 1`, which is byte-identical to today's behaviour (proposal
  §8.7)**. The n>1 path is exercised ONLY in unit tests + the §6.1
  variance-measurement run (scratch dir, NO baseline-pointer move). The live flip
  to n=3 + the symmetric majority gate go live in S-Auto-17 together with the
  re-bless. Do NOT commit `samples_per_case: 3`.

  **Proposal data re-verified by deliver-agent 2026-06-03 before scope lock**
  (all anchors current): `eval_runner.run_v1_fitness_suite:228` / `run_suite:87`
  / `SuiteRunResult:72`; `tier_evaluator._evaluate_layer0:235` / `_layer1:385`
  (bad no-drop `:400-407`, anchor max-drop `:417-431`) / `_layer3:521`;
  `baseline_loader.BaselineSnapshot:66` / `load:80`; `loop.py`
  `_INFRA_ESCALATION_REASONS:776 = {service_degraded, runtime_error_threshold}`
  / `_assess_infra_error:879`; `hard_checks._ESCALATION_REASON_FAMILY:66-105` /
  `_check_escalation_compliance:503`; `OpenAiCompatibleLlmClient.buildRequestBody:363`
  (no seed/top_p); `config.yaml` `:83/:96/:100/:104/:118/:122/:171`;
  `scoring_code_baseline_sha:171 = 35305bd8…` (matches the live M-Auto-3 close
  baseline — editing scoring files WILL trip it). `aggregate.py` does NOT yet
  exist (NEW). bad_cases `cs011`/`cs014`/`cs029` exist.

  **Inherited baselines at HEAD (do NOT re-fix / revert):** Java
  `1213 / 1 / 0 / 2` (sole failure = inherited `SystemPromptUserRequestedTiebreakerTest`,
  OQ-S41.5); eval_interactive pytest `503 / 0` under `uv run`; autoloop pytest
  `276`; 17-fixture `31`; scoring SHA `35305bd8…`. M-Auto-3 substrate is
  FINALIZED.

  **Codex review plan (§4.3):** PER-SUB-SPRINT REQUIRED (edits 3 of the now-5
  SHA-locked scoring files — `eval_runner.py` + `tier_evaluator.py` + `gaming.py`
  to extend `_compute_scoring_code_sha` coverage to the NEW `aggregate.py` →
  fence-#13 hard-fenced-surface trigger), FOLDED INTO the M-Auto-4
  milestone-shared close Codex. Codex is the validation / sign-off pass, NOT the
  authorship-provenance source.

  Dev session source-of-truth: `compact/sprint-072-dev-prompt.md` (self-contained
  per prompt-artifact-rules §9).
---

# Sprint 072 / S-Auto-16 — §0 fallback diagnostic + autoloop k-of-n fitness core (committed inert)

## Class

- **Layer (primary)**: `infra` — autoloop fitness MEASUREMENT reliability
  (sampling / aggregation in `autoloop/autoloop/scoring/`). No bot-runtime
  semantic change; no `eval_spec` change (that is S-Auto-18).
- **§7 stanza**: **included** (self-walked below). The sub-sprint is pure
  `infra`/measurement so it is technically §7-EXEMPT, but the stanza is included
  for rigor because it edits the SHA-locked fitness-gate scoring code and
  triggers a per-sub-sprint anti-hardcode Codex review.
- **Codex review plan (§4.3)**: **PER-SUB-SPRINT REQUIRED** — edits 3 of the
  now-5 SHA-locked scoring files (`eval_runner.py`, `tier_evaluator.py`, and
  `gaming.py` to extend `_compute_scoring_code_sha` coverage to the NEW
  `aggregate.py`), which fires the §4.3 hard-fenced-surface trigger (fence-#13
  controlled override). The dev does NOT dispatch Codex; the per-sub-sprint
  review is FOLDED INTO the deliver-authored M-Auto-4 milestone-shared close
  prompt and is a **validation / sign-off** pass (not an authorship-provenance
  source). Record the deferral + the SHA-recompute in handoff §11.
- **Position in milestone**: 1st of 3 (S-Auto-16 → S-Auto-17 re-bless+flip →
  S-Auto-18 §5 escalation tiers). Any second-order issue → an M-Auto-4
  sub-sprint, NOT a new milestone.

## Goal

Build the autoloop k-of-n fitness-measurement core so that a per-case pass/fail
signal CAN be a majority vote over repeated, provider-comparable samples — and
prove (real-LLM) that it reduces suite-level variance — WITHOUT yet flipping the
live loop, because the live majority gate is only sound once the baseline is
also aggregated (S-Auto-17). Also run the time-boxed §0 fallback diagnostic and
enforce the primary-only fitness provider policy so the eventual vote is over a
single model population.

**This sub-sprint does NOT improve the bot's customer-service ability** (no
skill/prompt/routing edit) and does NOT re-bless the baseline or change the
escalation spec.

## Scope (execute in order; proposal §8.6 steps 1–4)

### #1 — §0 fallback diagnostic + primary-only fitness enforcement (time-boxed)

Decided policy (v1, locked): autoloop fitness eval is **primary-provider-only**.
Product runtime keeps its fallback chain; the fitness harness must keep
provider/model COMPARABLE across attempts so the majority vote is over one model
population. The §0 job is to *enforce + instrument* that policy — it is
time-boxed instrumentation, NOT an independent research sprint.

1. **Capture per attempt, for every case**: `actual_provider`, `actual_model`,
   `fallback_count`, and upstream `request_id` (if the provider returns one).
   Surface them into the per-case eval result as NEW OPTIONAL fields (consumers
   tolerate their absence).
   - **Capture route (dev picks the cheapest faithful, ADDITIVE option):**
     `FallbackLlmClient.java` already logs `[chat:fallback-engaged]` /
     `[chat:fallback-skipped-*]` with `primary=`/`fallback=` labels
     (`FallbackLlmClient.java` ~`:59/:80/:108/:127`). The cheapest first cut
     parses those server logs keyed to the eval run window. The durable route
     threads the provider/model label back through the response object. **If you
     thread it through the response object, the change to the server is
     ADDITIVE metadata only — do NOT alter `FallbackLlmClient`'s fallback
     DECISION logic** (hard fence). Recommended: try the response-object thread
     if `eval_interactive` already surfaces a place to carry it; otherwise the
     log-parse first cut. Record which route you chose + why in handoff.
2. **Primary-only enforcement for fitness attempts**: prefer **retrying the
   primary** on a transient primary failure (at the eval_runner attempt level —
   re-invoke the suite) rather than relying on the backend fallback. If a
   fitness attempt is nonetheless served by the fallback (`fallback_count > 0` /
   `actual_model != primary`), mark it `provider_mixed=true` / `non_comparable`
   and **drop it from the k-of-n vote** (neither pass nor fail). If the primary
   cannot serve within the retry cap, mark the attempt `infra_error` — also
   excluded from the vote, never scored as pass/fail.
3. **Connect to existing infra-error detection**: `loop.py`
   `_INFRA_ESCALATION_REASONS:776 = {service_degraded, runtime_error_threshold}`
   and `_assess_infra_error:879` already demote infra-degraded runs to
   `decision="error"`. Confirm whether the escalation jitter on `faq_miss` cases
   is (a) genuine backend decision non-determinism or (b) fallback/infra
   degradation already covered by the infra-error path. Do NOT double-count:
   `service_degraded` / `runtime_error_threshold` route to infra-error drop, not
   to a scored escalation outcome.
4. **Exit condition**: a one-paragraph finding in handoff (fallback frequency +
   whether it co-occurs with the jittery escalation cases + whether primary-only
   retry can keep `min_valid_attempts=3` within the cost budget). **STOP-and-
   surface** if fallback is frequent enough that primary-only retry cannot reach
   `min_valid_attempts=3` within a reasonable retry cap — the cost model then
   needs a human decision before S-Auto-17.

### #2 — `aggregate.py` (NEW): pure majority-vote + pass_rate helper

- Create `autoloop/autoloop/scoring/aggregate.py`: a **pure** function (no I/O,
  no network, no file reads) that takes the per-attempt pass/fail records for one
  case and returns `{majority_passed, pass_rate, valid_attempts, attempts[]}`.
- Majority is over **valid attempts only** (exclude `provider_mixed` /
  `infra_error`). `pass_rate = passes / valid_attempts`.
- **Insufficient-valid-samples rule** (replaces any tie heuristic):
  `min_valid_attempts = 3`. If `valid_attempts < min_valid_attempts`, the result
  is `non_comparable` / `infra_error` — it does NOT participate in keep/discard.
  A `1/2`, `1/1`, or any sub-threshold result is **never** used to gate. Tie or
  insufficient evidence = neither improvement nor stable regression (a
  measurement gap, not a fitness verdict).
- Unit-test it (`autoloop/tests/test_aggregate.py`): majority up / down,
  provider_mixed exclusion, infra_error exclusion, sub-threshold → non_comparable,
  pass_rate arithmetic. Pure unit tests (no LLM).
- **Drift-guard coverage (RESOLVED decision — not an OQ):** `aggregate.py`
  produces the `majority_passed` that `eval_runner` + `tier_evaluator` use for the
  fitness verdict, so it MUST be inside the `scoring_code_baseline_sha` coverage.
  Edit `gaming.py` `_compute_scoring_code_sha()` to hash a **5-file** set
  (`tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py` + `gaming.py` +
  `aggregate.py`) and update the `config.yaml` comment block (`~:129-132`) that
  documents "four files" → five. Leaving `aggregate.py` outside the set would be a
  scoring-code drift hole. (`gaming.py` is itself in the set, so this edit is
  captured by the close-time SHA recompute in #5.)

### #3 — `eval_runner` n-loop + per-attempt persistence + majority aggregation

- Wrap the single suite invocation in `run_v1_fitness_suite`
  (`autoloop/autoloop/scoring/eval_runner.py:228`) so each suite runs
  `n = fitness.samples_per_case` times. `run_suite` (`:87`) stays the single-run
  primitive. `SuiteRunResult` (`:72`) gains a per-attempt list.
- **Per-attempt record** (one row per attempt per case): `case_passed`,
  `failure_reason`/`failure_tags`, `escalation_reason`, the §1 provider fields
  (`actual_provider`/`actual_model`/`fallback_count`/`request_id`), and the
  attempt's `valid` flag (`false` if `provider_mixed` or `infra_error`).
- **Aggregate per case** via `aggregate.py`: persist the per-attempt rows, the
  `pass_rate`, and `majority_passed`. `provider_mixed` / infra-degraded attempts
  are excluded from numerator AND denominator.
- **Retry**: if after dropping invalid attempts `valid_attempts <
  min_valid_attempts`, retry the missing attempts up to
  `aggregation.attempt_retry_cap` (=2). If still short → mark the case/suite
  `infra_error`/`non_comparable` (never gates).
- **Cost guard**: honor `eval_suite_timeout_seconds` (`config.yaml:122`) **per
  attempt**, not per triple. Keep `bad_cases parallel:1` (`config.yaml:83`).
- **INERT default**: the committed `fitness.samples_per_case = 1`. At n=1 the
  n-loop runs once, `majority_passed` = that single attempt, and the comparison
  is byte-identical to today (proposal §8.7). The n>1 path is exercised only in
  tests + the §6.1 measurement run.
- Tests (`autoloop/tests/test_eval_runner.py`, mockable / no real LLM): k-of-n
  loop count, `provider_mixed` drop, `infra_error` drop, retry-to-cap,
  insufficient → non_comparable, per-attempt persistence shape.

### #4 — `tier_evaluator` reads the aggregated majority signal

- `_evaluate_layer0` (~`:235`), `_evaluate_layer1` (`:385`; bad no-drop
  `:400-407`; anchor max-drop `:417-431`), `_evaluate_layer3` (`:521`),
  `_evaluate_layer2` (`:445`) read the **aggregated** (`majority_passed`)
  per-case signal instead of a single draw.
- **L0 Tier-0 delta + stable-reproduction**: a Tier-0 violation counts as a
  candidate regression only if it reproduces in the **majority** of candidate
  samples AND was absent from the baseline majority. A single noisy Tier-0 flip
  no longer discards. **Safety-floor strictness is UNCHANGED** — no family
  removed, no threshold lowered; only the measurement decision changes.
- L1 bad_cases strict no-drop (`:400-407`) and anchor max-drop (`:417-431`)
  compare candidate-majority vs the baseline snapshot.
- **n=1 invariance**: at the committed `samples_per_case=1`, `majority_passed`
  equals the single attempt's pass and the baseline is the existing single-draw
  snapshot → L0/L1/L2/L3 behaviour is byte-identical to today. The majority
  logic only diverges from today at n>1 (tests + measurement run).
- Tests (`autoloop/tests/test_tier_evaluator.py`, mockable): majority-based
  L0/L1/L3; stable-vs-flaky distinguishable (a regression failing in a minority
  of samples is `flaky` and does NOT gate; failing in the majority is `stable`
  and gates); n=1 reproduces the pre-sprint verdict on a fixture.

### #5 — `config.yaml` knobs (committed inert) + SHA recompute

- Add to `autoloop/config.yaml` `fitness`:
  ```yaml
  samples_per_case: 1                 # COMMITTED INERT this sub-sprint (=today's behaviour);
                                      # S-Auto-17 flips to 3 WITH the re-blessed baseline
  aggregation:
    method: majority
    min_valid_attempts: 3
    attempt_retry_cap: 2
  provider_policy:
    fitness_provider: primary_only
    prefer_retry_primary: true
    on_fallback: drop_attempt_non_comparable
    record_per_attempt: [actual_provider, actual_model, fallback_count, request_id]
  ```
- Do NOT add `baseline_aggregation` / do NOT move `baseline_dir` (that is the
  S-Auto-17 re-bless). Leave `anchor_outcome_max_drop_cases:0`,
  `improvement_min_cases:1`, `shadow_max_drop_pct:3.0` unchanged.
- **Recompute `scoring_code_baseline_sha` (`config.yaml:171`)** at close — editing
  `eval_runner.py` + `tier_evaluator.py` + `gaming.py` (the latter to widen the
  hash set per #2) trips the content hash, now over the **5-file** set
  (`tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py` + `gaming.py` +
  `aggregate.py`). Reproduce via the one-liner in the `config.yaml` comment block
  (`~:149`):
  `uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"`
  at this sub-sprint's HEAD; record old + new value in handoff. Expect
  `gaming.suspect_baseline_manipulation` / `scoring_code_drift` to observe the
  change — that is the intended fence-#13 controlled-override behaviour.

### #6 — Acceptance evidence (real-LLM; §5.7)

- **§6.1 variance drop (PRIMARY evidence for this sub-sprint)**: on a clean tree
  at this commit, run each suite n≥5 times via the new n-loop into a SCRATCH
  results dir (NOT the baseline pointer). Report the before/after suite-level
  score standard deviation: single-draw vs aggregated/majority. This is real-LLM
  (mocks may NOT be the primary evidence — §5.7). Requires the backend up + creds
  in `autoloop/.env.local` + a clean committed tree
  (`project_autoloop_dirty_index_hazard`).
- **Measurement-only run isolation (acceptance invariant):** the n≥5 / n=3
  variance run is a MEASUREMENT artifact only. It MUST NOT (a) participate in any
  `keep`/`discard` decision, (b) update or overwrite the baseline (it writes to a
  scratch dir; `baseline_dir` is untouched), or (c) trigger or feed an overnight
  loop gate. The committed loop stays at `samples_per_case=1` (today's behaviour);
  the n>1 path runs only as this offline measurement + the unit tests.
- **§6.3 flaky-vs-stable distinguishable**: demonstrate (from the n≥5 run or a
  fixture) that the harness labels a regression `flaky` (minority) vs `stable`
  (majority); only `stable` gates.
- **§6.7 measurement integrity**: fitness attempts are primary-provider-only;
  fallback-served attempts are marked `provider_mixed`/`non_comparable` and
  dropped; `< min_valid_attempts=3` after retries → `infra_error`/`non_comparable`,
  never gates.
- Unit suites green: `test_aggregate.py`, `test_eval_runner.py`,
  `test_tier_evaluator.py`.

## Hard fences / STOP conditions

- **Do NOT commit `samples_per_case: 3`** — the committed value is `1` (inert).
  The live flip is S-Auto-17 (needs the re-blessed baseline first).
- **Do NOT re-bless the baseline / do NOT move `baseline_dir` / do NOT overwrite
  the retained single-run baseline.** The §6.1 variance run writes to a scratch
  dir only.
- **Do NOT edit any skill YAML soft field, prompt, or routing semantic.** No
  `eval_spec` / CaseSpec edit (the §5 escalation tiers are S-Auto-18).
- **Do NOT alter `FallbackLlmClient`'s fallback DECISION logic.** §0 capture is
  ADDITIVE metadata only.
- **Tier-0 families: no family removed, no threshold lowered.** Only
  noise-driven single-flip filtering (majority / stable-reproduction).
- **`bad_cases` strict no-drop preserved**; no drop-budget.
- **`aggregate.py` MUST join the `scoring_code_baseline_sha` coverage** (5-file
  set, via the `gaming.py` `_compute_scoring_code_sha` edit in #2) — it is used
  for the fitness verdict, so leaving it outside the drift guard is a hole. This
  is a RESOLVED decision, not an OQ.
- **Scope hold (deferred — do NOT pull forward):** the §5
  `acceptable_escalation_families` change stays in S-Auto-18; the baseline
  re-bless + the live `samples_per_case=3` flip stay in S-Auto-17.
- **STOP-and-surface** (handoff §7, do not improvise) if: the §0 diagnostic shows
  primary-only retry cannot reach `min_valid_attempts=3` within a sane retry cap;
  OR making `tier_evaluator` majority-aware cannot keep n=1 byte-identical to
  today; OR provider metadata cannot be captured additively without changing the
  fallback decision logic.
- **Eval evidence gate (§5.7)**: mocked-LLM tests cover wiring only; the §6.1
  variance metric MUST be real-LLM.

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop fitness measurement reliability —
sampling / aggregation / per-attempt persistence in `autoloop/autoloop/scoring/`
+ additive provider-metadata capture). No `eval_spec` change this sub-sprint
(deferred to S-Auto-18).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It *preserves*
the existing Tier-0 safety families (PII, escalation_compliance, critical_policy,
phase_transition) at current strictness; the L0 change is a measurement decision
(stable-reproduction across k-of-n), not a policy relaxation.

**Semantic hardcode:** No semantic hardcode introduced. No skill/prompt soft
field edited; no keyword/regex/enum routing added. Majority aggregation is a pure
cardinality computation over existing pass/fail signals; the primary-only
provider policy keys on `actual_model`/`fallback_count`, not content.

**Generalization coverage:** Measurement-reliability sub-sprint — the
"generalization" evidence is the repeated-baseline variance metric (§6.1) and the
flaky-vs-stable distinguishability check (§6.3), not target/neighbor/negative/
shadow case-family counts. The shadow gate (L4) remains active + firewalled; only
the aggregate majority crosses to the loop; its role is unchanged.

## Test / eval requirements

- **Java**: no new regression vs the inherited `1213 / 1 / 0 / 2` baseline (sole
  failure = `SystemPromptUserRequestedTiebreakerTest`, OQ-S41.5). If the §0
  capture threads metadata through the response object, add/extend Java tests for
  the additive field + rebuild (`mvn`; restart backend before any eyeball/eval —
  no hot reload, `feedback_restart_backend_before_eyeball`).
- **autoloop pytest**: `test_aggregate.py` (new) + `test_eval_runner.py` +
  `test_tier_evaluator.py` green; no regression vs the `276` baseline.
- **eval_interactive pytest**: no regression vs `503 / 0` under `uv run`.
- **Real-LLM §6.1 variance run**: required as primary acceptance evidence (§5.7).
- **scoring SHA**: recomputed + recorded; `scoring_code_drift` explained.

## Codex review plan (§4.3)

PER-SUB-SPRINT REQUIRED (edits 3 of the now-5 SHA-locked scoring files —
`eval_runner.py`, `tier_evaluator.py`, `gaming.py` — → fence-#13
hard-fenced-surface trigger). Codex is the **validation / sign-off** pass, NOT an
authorship-provenance source. The dev does NOT dispatch Codex. Record in handoff
§11: the per-sub-sprint deferral into the M-Auto-4 milestone-shared close prompt,
the old→new `scoring_code_baseline_sha`, and the fence-#13 controlled-override
authorization. Codex checklist focus (proposal §8.5): no skill/prompt/routing
edit; Tier-0 unchanged strictness; bad_cases no-drop preserved; k-of-n
aggregation pure + unit-tested; `aggregate.py` inside the 5-file SHA coverage;
provider_mixed/infra_error dropped not silently passed; insufficient-valid →
non_comparable not pass/fail; SHA recomputed; n=1 byte-identical to today; the
measurement-only n>1 run never gates / never re-blesses; real-LLM acceptance
evidence.

## Handoff requirements (dev authors `docs/sprints/sprint-072-handoff.md`)

Standard sub-sprint handoff. MUST include: §0 fallback-frequency finding
(one paragraph); the §6.1 variance before/after std-dev table (real-LLM);
confirmation the measurement-only n>1 run did not gate / re-bless / feed an
overnight; the old→new `scoring_code_baseline_sha` over the 5-file set (now
including `aggregate.py`); confirmation `samples_per_case` committed =1 (inert) +
n=1 byte-identical evidence; §11 Codex deferral note; §12 self-classification.

## Commit discipline

Stage only authorized-scope files (NOT `git add -A`). One commit per §8.6 rollout
step where practical (diagnostic / aggregate.py / eval_runner / tier_evaluator /
config+SHA) so revert is per-step (§8.7). Run the loop / any eval ONLY on a clean
committed tree (`project_autoloop_dirty_index_hazard`). Deliver-agent-owned files
are bundled by the human at close.

## Self-check checklist (dev completes before claiming done)

- [ ] §0 diagnostic ran; fallback-frequency finding written; primary-only
      enforcement in place; STOP-and-surface evaluated.
- [ ] `aggregate.py` is pure (no I/O) + unit-tested (majority / exclusion /
      sub-threshold / pass_rate) AND added to the `scoring_code_baseline_sha`
      coverage via `gaming.py` (5-file set; `config.yaml` "four files" comment
      updated to five).
- [ ] `eval_runner` n-loop + per-attempt persistence + retry + drop rules;
      `run_suite:87` primitive unchanged; tests green.
- [ ] `tier_evaluator` reads aggregated majority; L0 stable-reproduction;
      n=1 byte-identical to today (fixture-proven).
- [ ] `config.yaml`: `samples_per_case: 1` committed (NOT 3); aggregation +
      provider_policy knobs added; `baseline_dir` UNCHANGED.
- [ ] `scoring_code_baseline_sha` recomputed + recorded (old→new) over the
      5-file set; drift explained.
- [ ] §6.1 variance drop measured real-LLM (scratch dir, no pointer move);
      §6.3 flaky-vs-stable shown; §6.7 measurement integrity shown; the n>1
      measurement run did NOT gate / re-bless / feed an overnight.
- [ ] Java / autoloop / eval_interactive baselines no regression.
- [ ] No skill/prompt/routing/CaseSpec/eval_spec edit; no fallback-decision
      change; no baseline re-bless.
- [ ] Handoff §0-finding / §6.1-table / SHA / §11-Codex-deferral / §12-class
      written.
