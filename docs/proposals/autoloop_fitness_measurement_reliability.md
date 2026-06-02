---
title: Autoloop Fitness Measurement Reliability — k-of-n Sampling + Rebaseline + Noise-Robust Gates
doc_tier: proposal
status: proposal
implementation_status: not_started
source_of_truth: this file (until promoted to a sub-sprint objective)
last_reviewed: 2026-06-03
review_cadence: on_reactivation
supersedes: []
superseded_by: null
notes: >
  Draft sub-sprint proposal for the M-Auto track. Goal is autoloop fitness
  MEASUREMENT reliability, not bot capability. Motivated by the verified
  finding that decoding temperature is already pinned to 0.0 on all three
  LLM components (simulator / judge / csagent backend) yet the backend still
  produces materially different conversations on byte-identical input, so the
  fix line is gate noise-robustness + rebaseline, NOT further source pinning.
  Deliver-agent assigns the S-Auto-N number and slots it. Every code anchor
  cited here was read on 2026-06-03; re-verify line numbers before coding.
  v1 knob decisions (n=3 default; primary-only fitness provider;
  min_valid_attempts=3 with retry→infra_error) locked 2026-06-03 — see §9.
---

# Autoloop Fitness Measurement Reliability

> **Proposed sub-sprint, M-Auto track.** Deliver-agent assigns `S-Auto-N`.
> This is a DRAFT for review; no code is changed by this document.

## §0-context. Why this sprint exists (verified findings)

Across 67 autoloop iterations there are **0 `keep` decisions**. The loop
never reaches Layer 3 (improvement) or Layer 4 (shadow) — it short-circuits
at Layer 0 (Tier-0 safety delta) or Layer 1 (outcome no-regression) every
time. Root cause, verified on 2026-06-03:

- **Decoding temperature is already pinned to 0.0** on the user-simulator
  (`eval_interactive/eval_interactive.yaml:15,21`), the model-graded judge
  (same file), and the csagent backend
  (`server/.../llm/LlmRequest.java:19`, comment records OQ-S65.8 lowered it
  0.3→0.0 *specifically to kill this noise*).
- **The backend still produces different conversations on byte-identical
  input** run-to-run: different turn counts, different
  resolve-vs-escalate decisions, escalation reasons spanning 3 reason
  families. `seed` and `top_p` are **not even present** in the backend
  request body (`OpenAiCompatibleLlmClient.buildRequestBody`, ~`:363`), and
  the DeepSeek/Kimi providers' support for them is unverified — so source
  pinning has ~no headroom left.
- **Controlled-experiment proof of pure measurement noise**: jittery cases
  (e.g. shadow `cs01s01` UC-C: 11/25 pass) flip pass↔fail even across
  iterations whose sandboxed edit targeted an **unrelated** skill, and the
  same edited skill/field appears on both passing and failing runs of the
  same case. The flip cannot be edit-caused.
- **The baseline is a single noisy draw** per suite
  (`config.yaml:118` → three symlinks, each to one timestamped run; no
  median/min/k-of-n). The gates compare one noisy candidate draw against one
  noisy reference draw.
- **Secondary amplifier**: the `escalation_compliance` hard check is
  pure-deterministic, but a noisy backend that escalates with a
  same-intent-but-different-family reason cross-family-mismatches
  `expected.escalation_trigger` and registers as a hard FAIL.

**Conclusion that scopes this sprint**: the fix line is **gate
noise-robustness (k-of-n) + rebaseline + a tightly-bounded escalation-spec
loosening**, NOT further source de-noising (already exhausted). One residual
*source* contributor must be ruled out first — see §0-diagnostic.

## §1. Sprint goal

Raise autoloop **fitness measurement reliability** so that `keep`/`discard`
reflects a *reproducible* regression or improvement rather than single-run
provider noise. Concretely: make the per-case pass/fail signal a
majority vote over repeated samples, rebless the baseline the same way, and
make L0/L1/L3 judge on the aggregated signal.

**This sprint does NOT try to improve the bot's customer-service ability.**
No skill YAML soft field is edited here; no prompt/routing semantic change.

## §2. Non-goals / scope guard

- Not a bot-capability sprint (no skill/prompt semantic edits).
- Not a source-pinning sprint (temp already 0.0; seed/top_p out of reach).
- Not a `bad_cases` drop-budget sprint as first priority (see §5; deferred
  unless k-of-n + rebaseline still shows 误杀).
- The §0-diagnostic is **time-boxed instrumentation**, not an independent
  research sprint — it must not block the core harness work.

## §0-diagnostic (prerequisite, time-boxed): DeepSeek→Kimi fallback check

A primary→fallback chain exists (`FallbackLlmClient.java`). If, mid-suite,
the backend silently falls back from the primary (deepseek-v4-flash) to the
fallback (kimi-k2.6), then *two different models* answer within one run —
an independent determinism breaker that k-of-n would average over wrongly
(mixing model populations). Rule it out / quantify it before trusting the
majority vote.

**Decided policy (v1)**: autoloop fitness eval is **primary-provider-only**.
Product runtime keeps its fallback chain; autoloop fitness must keep
provider/model **comparable** across attempts so the majority vote is over a
single model population. The §0-diagnostic's job is therefore to *enforce and
instrument* that policy, not to decide whether to adopt it.

Scope (small, additive instrumentation + the primary-only enforcement):

1. **Capture per attempt**, for every case, the `actual_provider`,
   `actual_model`, `fallback_count`, and upstream `request_id` (if the
   provider returns one). Surface them into the per-case eval result (new
   optional fields; consumers tolerate their absence). `FallbackLlmClient`
   already logs `[chat:fallback-engaged]` / `[chat:fallback-skipped-*]` with
   `primary=`/`fallback=` labels — the cheapest first cut parses those server
   logs keyed by the eval run window; the durable version threads the
   provider label back through the response object.
2. **Primary-only enforcement for fitness attempts**: prefer **retrying the
   primary** on a transient primary failure rather than engaging the
   fallback. If a fitness attempt nonetheless ends up served by the fallback
   (`fallback_count > 0` / `actual_model != primary`), mark that attempt
   `provider_mixed=true` / `non_comparable` and **drop it from the k-of-n
   vote** (neither pass nor fail). If the primary cannot serve within the
   retry cap, mark the attempt `infra_error` — again excluded from the vote,
   never scored as pass/fail.
3. **Connect to existing infra-error detection**: `loop.py`
   `_INFRA_ESCALATION_REASONS = {service_degraded, runtime_error_threshold}`
   and `_assess_infra_error` already demote infra-degraded runs to
   `decision="error"` (never scored as fitness). A `service_degraded`
   escalation on a `faq_miss` case is frequently *this* — an infra/give-up
   masquerade, not a spec problem. The diagnostic must confirm whether the
   escalation jitter is (a) genuine backend decision non-determinism or
   (b) fallback/infra degradation already covered by the infra-error path.

Exit condition: a one-paragraph finding (fallback frequency + whether it
co-occurs with the jittery escalation cases) confirming the primary-only
policy is cheap to enforce and sizing the retry cap. This instrumentation is
in v1 scope; it must not balloon into an independent research sprint.

## §3. Core changes

### 3.1 `eval_runner` — per-case k-of-n repeated sampling (n=3 v1)

- Wrap the single suite invocation in `run_v1_fitness_suite`
  (`autoloop/autoloop/scoring/eval_runner.py:228`) so each suite runs
  `n=fitness.samples_per_case` times. **v1 default `n=3`.** `run_suite`
  (`:87`) stays the single-run primitive.
- **`n=5` is NOT the v1 main line** (cost). It is reserved as an *optional
  per-case override* (`samples_override`) for known high-flake /
  escalation-sensitive cases, applied selectively in a later milestone — not
  switched on globally in v1.
- **Per-attempt record** (one row per attempt, per case): `case_passed`,
  `failure_reason`/`failure_tags`, `escalation_reason`, and the §0 provider
  fields `actual_provider` / `actual_model` / `fallback_count` /
  `request_id`, plus the attempt's `valid` flag
  (`false` if `provider_mixed` or `infra_error`).
- **Aggregate per case** by majority vote on `case_passed` **over valid
  attempts only**. Persist per case: the per-attempt rows above, the
  `pass_rate` (passes / valid_attempts), and the `majority_passed` result.
  `provider_mixed` / infra-degraded attempts are excluded from both numerator
  and denominator (a flaky attempt neither passes nor fails the vote).
- **Insufficient-valid-samples rule** (replaces any tie heuristic):
  `min_valid_attempts = 3`. With `n=3` there should normally be no tie. If,
  after dropping `provider_mixed`/`infra_error` attempts, `valid_attempts < 3`,
  **retry** the missing attempts (up to `attempt_retry_cap`). If still
  `< min_valid_attempts` after retries, mark that **case/suite
  `infra_error` / `non_comparable`** — it does NOT participate in
  keep/discard. A `1/2`, `1/1`, or any sub-`min_valid_attempts` result is
  **never** used to gate. **Tie or insufficient evidence counts as neither an
  improvement nor a stable regression** — it is a measurement gap, surfaced
  as `infra_error`, not a fitness verdict.
- Cost guard: `n=3` triples eval wall-clock. Keep `bad_cases parallel:1`
  (session-establishment flakiness, `config.yaml:83`); honor
  `eval_suite_timeout_seconds` (`:122`) per attempt, not per triple.

### 3.2 `tier_evaluator` — judge on aggregated results

- L0/L1/L2/L3 read the **aggregated** (`majority_passed`) per-case signal,
  not a single draw (`tier_evaluator.py` `_evaluate_layer0` ~`:235`,
  `_evaluate_layer1` `:385`, `_evaluate_layer3`, `_evaluate_layer2`).
- L0 Tier-0 **delta** mode: a Tier-0 violation counts as a
  *candidate regression* only if it **reproduces in the majority** of
  candidate samples AND was not present in the baseline majority. A single
  noisy Tier-0 flip no longer discards. (Safety floor strictness unchanged —
  see §4.)
- L1 bad_cases strict no-drop (`:400`) and anchor max-drop (`:391/:417-420`)
  compare candidate-majority vs baseline-majority.

### 3.3 Baseline re-bless — repeated, no single symlink

- Replace the single-run symlinks under `config.yaml:118`
  (`baseline_dir`) with an **aggregated baseline artifact** built from the
  same n runs per suite: store `majority_passed` per case, `pass_rate`,
  and `model`/`provider` metadata (+ git commit, date, n). `baseline_loader`
  loads the aggregated artifact; `BaselineSnapshot` carries the per-case
  majority + pass_rate. Procedure in §6.

### 3.4 Integration gotcha — recompute `scoring_code_baseline_sha`

`config.yaml:171 scoring_code_baseline_sha` is a content hash over the four
scoring files (`tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py`
+ `gaming.py`). Editing `eval_runner.py` and `tier_evaluator.py` **will**
trip `gaming.scoring_code_drift.sha_changed`. The sprint MUST recompute and
re-record the SHA at close (reproduce via the one-liner in the `config.yaml`
comment block) — otherwise every post-sprint iteration emits a spurious
drift ERROR. Also expect `gaming.suspect_baseline_manipulation` to observe
the baseline change; the re-bless must be human-authorized and recorded.

## §4. Gate strategy

| Gate | Change | Strictness |
|---|---|---|
| **Tier-0 safety families** (pii, escalation_compliance, critical_policy, phase_transition) | Judge on majority; a violation must **stably reproduce** across the k-of-n samples to count as a candidate regression | **Floor unchanged.** No family removed; no threshold lowered. Only noise-driven single flips are filtered. |
| **L1 bad_cases no-drop** | Candidate-majority vs baseline-majority | Strict no-drop kept for v1. **bad_cases drop-budget is NOT first priority** — only revisit if k-of-n + rebaseline still shows 误杀. If added later, it must be an explicit `config` knob (no hard-coded change at `:400`) + human + Codex review. |
| **L1 anchor max-drop** | Majority-based; keep `anchor_outcome_max_drop_cases` config (`:104`) | Unchanged default (0) in v1; tune only with evidence. |
| **L3 improvement (`improvement_min_cases=1`, `:96`)** | A case counts as "improved" only if its **majority** flips up; the +1 must be re-sample-stable | Unchanged threshold. |
| **L4 shadow (`shadow_max_drop_pct=3.0`, `:100`)** | Majority-based aggregate (firewall preserved) | **Unchanged.** Generalization gate not relaxed. |

**Anti-误杀 principle (Constitution §5.3/§5.4, §5.7) — stated explicitly:**

- The **Tier-0 safety obligation is NOT relaxed.** Every Tier-0 family (PII,
  escalation_compliance, critical_policy, phase_transition) and its threshold
  stays exactly as today.
- What changes is only the **measurement decision**: a Tier-0 violation is
  judged a *candidate-introduced* safety regression **only if it stably
  reproduces across the k-of-n samples** (and was absent from the baseline
  majority). A single noisy Tier-0 flip is a measurement artifact, not a
  regression, and no longer discards.
- In one line: **we relax the measurement, never the safety policy.** A
  genuine, reproducible Tier-0 violation still discards unconditionally.

## §5. Escalation spec adjustment (eval_spec layer)

The reason-family map **already exists** (`hard_checks.py:66-105`,
`_ESCALATION_REASON_FAMILY`) and same-family siblings already pass; only
cross-family mismatches fail. So the change is NOT "add family matching" —
it is letting a CaseSpec declare a **set of acceptable families with tiers**
instead of deriving one allowed family from a single canonical
`escalation_trigger`.

- Add to the CaseSpec `expected` block (alongside existing `should_escalate`
  / `escalation_trigger` / `acceptable_outcomes`, e.g.
  `bad_cases/cs001...:76-79`):
  - `acceptable_escalation_families:` with explicit tiers —
    `primary:` (the canonical correct family),
    `secondary_allowed:` (defensible alternatives that must NOT hard-fail),
    `forbidden:` (families that remain hard fails because they imply wrong
    routing or unsafe handling).
- `_check_escalation_compliance` (`hard_checks.py:503`) reads these tiers:
  bot reason in `primary` or `secondary_allowed` → pass; in `forbidden` or
  unlisted-and-cross-family → fail. Falls back to today's single-family
  derivation when the new field is absent (backward compatible).
- **Loosen reason MATCHING, never escalation OBLIGATION**: `should_escalate`
  and the must-escalate floor are untouched. A case that should escalate and
  doesn't still fails.
- Connect to §0: `service_degraded` / `runtime_error_threshold` should
  generally be classified as **infra-degraded (drop the attempt)**, not as a
  `secondary_allowed` escalation outcome — so the spec change and the
  infra-error path do not double-count.

Per-case curation of the tiers is a human + Codex `eval_spec` task (do not
auto-widen; §1.7 forbids widening eval to accept a genuine bot mistake).

## §6. Acceptance criteria

1. **Variance drop**: on the *same commit + same config*, a repeated baseline
   (n≥5 runs) shows a clearly lower suite-level score variance under the
   aggregated/majority pipeline than the single-draw pipeline. Report the
   before/after standard deviation per suite. (Real-LLM reruns — §5.7
   mocked-LLM evidence gate: mocks may NOT be the primary evidence here.)
2. **No single-flip discards**: the previously jittery cases (e.g. shadow
   `cs01s01`, bad `cs011`/`cs014`/`cs029`, anchor `uc_fp`) no longer cause a
   `discard` from a single-run flip.
3. **Flaky vs stable distinguishable**: the harness can label a regression
   `flaky` (fails in a minority of samples) vs `stable` (fails in the
   majority); only `stable` gates.
4. **No systematic L0/L1 short-circuit**: an overnight run under the new
   harness reaches Layer 3/4 on at least some iterations (the loop is no
   longer structurally pinned at L0/L1 by noise).
5. **Safety unchanged**: PII / escalation-obligation / critical-policy floors
   are not weakened; Tier-0 families intact; L4 shadow gate intact.
6. **No spurious drift**: `gaming.scoring_code_drift` is green post-close
   (SHA recomputed); the baseline re-bless is recorded and
   `suspect_baseline_manipulation` is an expected, explained observation.
7. **Measurement integrity**: fitness attempts are primary-provider-only and
   comparable (fallback attempts dropped, not voted); a case with
   `< min_valid_attempts=3` valid samples after retries is reported as
   `infra_error`/`non_comparable` and never gates keep/discard; a
   sub-threshold (`1/2`, `1/1`) result counts as neither improvement nor
   stable regression.

## §7. Layer-classification + anti-hardcode stanza (iteration_governance §7.1)

**Target failure layer:** primarily `infra` (fitness measurement
reliability — harness/sampling/baseline), with a bounded `eval_spec`
sub-change (§5 escalation-family tiers). Multi-layer prospective per §7
bundle variant; bundle policy lives here in the proposal so Codex can verify
scope discipline.

**Tier-0 invariant:** This sprint adds no Tier-0 invariant. It *preserves*
existing Tier-0 safety families (PII, escalation_compliance, critical_policy,
phase_transition) and only filters noise-driven single flips before they gate.

**Semantic hardcode:** No semantic hardcode introduced. No skill/prompt soft
field is edited; no keyword/regex/enum routing added. The escalation-family
tiers are declarative CaseSpec metadata curated by human + Codex, not bot
behaviour.

**Generalization coverage:** Measurement-reliability sprint — the
"generalization" evidence is the repeated-baseline variance metric (§6.1)
and the jittery-case no-single-flip check (§6.2), not target/neighbor/
negative/shadow case-family counts. Shadow gate (L4) remains active and
firewalled; its role is unchanged.

## §8. Outputs

### 8.1 Directory structure (new / changed)

```
autoloop/
  config.yaml                         # +samples_per_case, +baseline aggregation knobs, SHA re-record
  autoloop/scoring/
    eval_runner.py                    # k-of-n loop + per-attempt persistence + majority aggregation
    tier_evaluator.py                 # L0/L1/L2/L3 read aggregated/majority signal
    baseline_loader.py                # load aggregated baseline artifact (majority + pass_rate + provider meta)
    aggregate.py        (NEW)         # pure majority-vote + pass_rate helper (unit-testable, no I/O)
  results/
    <new-baseline-dir>/               # aggregated baseline artifact (replaces single-run symlinks)
      <suite>/aggregated.json         # per-case {majority_passed, pass_rate, attempts[], model, provider}
  tests/
    test_aggregate.py   (NEW)
    test_eval_runner.py               # k-of-n + provider_mixed drop + tie rule
    test_tier_evaluator.py            # majority-based L0/L1/L3; stable-vs-flaky
server/.../llm/
  FallbackLlmClient.java / response model   # §0: thread provider/model/fallback_count/request_id (additive)
eval_interactive/eval_interactive/scoring/
  hard_checks.py                      # §5: read acceptable_escalation_families tiers (backward compatible)
eval_interactive/case_specs/**/*.yaml # §5: per-case tiers (human + Codex curated, incremental)
```

### 8.2 Files touched + change points

- `eval_runner.py:228` `run_v1_fitness_suite` — n-loop; `:87` `run_suite`
  unchanged primitive; `SuiteRunResult` (`:72`) gains per-attempt list.
- `tier_evaluator.py` `_evaluate_layer0` (~`:235`), `_evaluate_layer1`
  (`:385`, bad `:400`, anchor `:417-420`), `_evaluate_layer3` — majority input.
- `baseline_loader.py` `load` / `BaselineSnapshot` — aggregated artifact.
- `config.yaml`: `:96/:100/:104/:118/:122/:171` (see §8.3).
- `hard_checks.py:503` `_check_escalation_compliance` + `:66-105` family map
  (read tiers; map unchanged).
- `FallbackLlmClient.java` (+ response object) — provider/fallback metadata.

### 8.3 Config example (additions to `autoloop/config.yaml` → `fitness`)

```yaml
fitness:
  samples_per_case: 3                 # k-of-n; v1 main line = 3 (do NOT default to 5)
  # samples_override: {}              # OPTIONAL per-case n bump (e.g. 5) for known
                                      # high-flake / escalation-sensitive cases; NOT v1 main line
  aggregation:
    method: majority                  # majority vote on case_passed over VALID attempts only
    min_valid_attempts: 3             # < 3 valid after retries → case/suite = infra_error / non_comparable
    attempt_retry_cap: 2              # retry missing/invalid attempts before declaring infra_error
    # NO tie heuristic: insufficient/tie evidence is neither improvement nor stable regression
  provider_policy:
    fitness_provider: primary_only    # product runtime may fallback; fitness must stay comparable
    prefer_retry_primary: true        # retry primary on transient failure rather than engage fallback
    on_fallback: drop_attempt_non_comparable   # fallback-served attempt → excluded from vote
    record_per_attempt: [actual_provider, actual_model, fallback_count, request_id]
  baseline_dir: eval_interactive/results/<rebaselined-dir>   # aggregated artifact, not single-run symlinks
  baseline_aggregation:
    samples: 5                        # ONE-TIME human re-bless over >=5 runs/suite (not per-iteration cost)
    store: [majority_passed, pass_rate, model, provider, git_commit, captured_at, n]
  # tier-0 delta + majority + stable-reproduction enforced in tier_evaluator (no new gate knob in v1)
  anchor_outcome_max_drop_cases: 0    # unchanged in v1
  improvement_min_cases: 1            # unchanged
  shadow_max_drop_pct: 3.0            # unchanged
  # bad_cases drop-budget intentionally absent in v1 (see §5); revisit ONLY if k-of-n + rebaseline
  # still shows 误杀, and then only as an explicit knob with human + Codex sign-off
  scoring_code_baseline_sha: "<RECOMPUTE_AT_CLOSE>"   # 4-file content hash; MUST update after editing scoring files
```

### 8.4 Baseline re-bless procedure

1. On a **clean tree** at the target commit, run each suite `samples=5`
   times via the new n-loop path (or the existing runner 5×).
2. Build `aggregated.json` per suite: per-case `majority_passed` +
   `pass_rate` + per-attempt detail + `model`/`provider` + `git_commit` +
   `captured_at` + `n`.
3. Write to a fresh dated `results/<rebaselined-dir>/<suite>/aggregated.json`
   (do **not** overwrite the old baseline; point `baseline_dir` at the new
   one — reversible).
4. Record the re-bless in the sub-sprint handoff (human-authorized;
   `gaming.suspect_baseline_manipulation` will observe it — expected).
5. Recompute `scoring_code_baseline_sha` and record it.

### 8.5 Codex review checklist (sprint-close, anti-hardcode kernel §4.1)

- [ ] No skill YAML soft field / prompt / routing semantic edit (this is a
      measurement sprint; confirm the mutable-surface files are untouched).
- [ ] Tier-0 safety families unchanged in strictness; only noise filtering
      added (majority/stable-reproduction); no family removed, no threshold
      lowered.
- [ ] `bad_cases` strict no-drop preserved (no hidden drop-budget); any
      future budget is an explicit config knob, not a code default.
- [ ] §5 escalation tiers loosen reason MATCHING only; `should_escalate` /
      must-escalate obligation untouched; `forbidden` families still fail;
      per-case tiers are human-curated, not auto-widened (§1.7).
- [ ] `service_degraded`/`runtime_error_threshold` routed to infra-error
      drop, not counted as a valid escalation outcome (no double-count).
- [ ] k-of-n aggregation is pure + unit-tested; flaky/provider-mixed
      attempts excluded from the vote, not silently passed.
- [ ] Fitness eval is primary-provider-only; a fallback-served attempt is
      marked `provider_mixed`/`non_comparable` and dropped from the vote;
      each attempt records `actual_provider`/`actual_model`/`fallback_count`/
      `request_id`.
- [ ] Insufficient valid samples (`< min_valid_attempts=3` after retries) →
      case/suite `infra_error`/`non_comparable`, NOT a pass/fail; a
      `1/2`/`1/1` sub-threshold result never gates; tie/insufficient is
      neither improvement nor stable regression.
- [ ] `scoring_code_baseline_sha` recomputed; `scoring_code_drift` green.
- [ ] Baseline re-bless recorded + human-authorized; old baseline retained.
- [ ] Acceptance evidence is **real-LLM reruns** (§5.7), not mocked-LLM.
- [ ] Shadow firewall preserved (only aggregate majority crosses to the loop).

### 8.6 Rollout order

1. §0-diagnostic instrumentation (additive; ship + read fallback finding).
2. `aggregate.py` + unit tests (pure, no I/O) — lowest risk.
3. `eval_runner` n-loop + per-attempt persistence (behind
   `samples_per_case`; `=1` reproduces today's behaviour exactly).
4. `tier_evaluator` majority input + Tier-0 stable-reproduction.
5. Baseline re-bless (§8.4) + `scoring_code_baseline_sha` recompute.
6. §5 escalation tiers (hard_checks reader + incremental per-case curation).
7. Validation overnight run; collect §6 acceptance evidence; Codex close.

### 8.7 Rollback

- **Single switch**: `samples_per_case: 1` + point `baseline_dir` back at the
  retained single-run baseline → exact pre-sprint behaviour (the n-loop and
  majority code remain but are inert at n=1).
- §5 is backward compatible: removing `acceptable_escalation_families` from a
  CaseSpec restores single-family derivation.
- Baseline is reversible (old dir retained; only the pointer moves).
- Each rollout step is an independent commit; revert in reverse order. After
  any revert touching the four scoring files, recompute
  `scoring_code_baseline_sha`.

## §9. Resolved decisions (v1, locked 2026-06-03)

1. **`samples_per_case`**: v1 default **n=3**. Do **not** default to 5 (cost).
   `n=5` is reserved as an *optional per-case override* for known
   high-flake / escalation-sensitive cases, applied selectively in a later
   milestone — not the v1 main line.
2. **Fitness provider policy**: fitness eval is **primary-provider-only** —
   product runtime may fall back, but autoloop fitness must keep
   provider/model comparable. A deepseek→kimi fallback attempt does **not**
   join the majority vote (marked `provider_mixed`/`non_comparable`). Prefer
   **retrying the primary**; past the retry cap, mark the attempt
   `infra_error` rather than pass/fail. Every attempt records
   `actual_provider` / `actual_model` / `fallback_count` / `request_id`.
3. **Tie / insufficient valid samples**: with n=3 there should be no tie. If
   dropping `provider_mixed`/`infra_error` attempts leaves
   `< min_valid_attempts = 3` valid samples, **retry first**; if still
   short, mark the case/suite `infra_error` / `non_comparable`. A `1/2`,
   `1/1`, or any sub-threshold result **never** participates in keep/discard.
   Tie or insufficient evidence is **neither an improvement nor a stable
   regression** — it is a measurement gap, not a fitness verdict.

These were the prior §9 open questions; they are now locked into §0, §3, §4,
§5, and §8.3 above. The baseline re-bless uses n≥5 as a one-time human run
(§8.4) — distinct from the per-iteration `samples_per_case=3`.
