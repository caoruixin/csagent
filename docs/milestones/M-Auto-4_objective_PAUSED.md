---
title: "[PAUSED 2026-06-04] Milestone M-Auto-4 — Autoloop Fitness Measurement Reliability"
doc_tier: current-runtime
status: deferred
implementation_status: partial
source_of_truth: this file
last_reviewed: 2026-06-04
review_cadence: per milestone
supersedes: [docs/milestones/M-Auto-3_objective.md]
superseded_by: null
notes: >
  **PAUSED 2026-06-04** — paused after S-Auto-17 (k-of-n LIVE, baseline re-blessed)
  pending milestone M-Auto-5 (eval verdict correctness + trace-contract honesty;
  docs/milestone_objective.md). RESUMES at S-Auto-18 (escalation-family tiers) after
  the M-Auto-5 re-bless; the held validation overnight resumes then. Not closed, not
  superseded — preserved here as the resume contract.

  M-Auto-4 north star = autoloop fitness MEASUREMENT reliability, NOT bot
  capability. Adopts `docs/proposals/autoloop_fitness_measurement_reliability.md`
  as the milestone spec (a dev/research-tier draft authored in a Claude Code
  session on top of a multi-agent diagnosis — NOT Codex-authored; the
  milestone-shared Codex review is the validation pass, not the authorship).

  The proposal's data claims were independently re-verified by the deliver-agent
  on 2026-06-03 before scope lock (temp=0.0 on simulator `eval_interactive.yaml:15`
  / judge `:21` / backend `LlmRequest.java:19`; `seed`/`top_p` absent from
  `OpenAiCompatibleLlmClient.buildRequestBody` `:363`; baseline is a single-draw
  per-suite symlink set `config.yaml:118`; `scoring_code_baseline_sha`
  `35305bd8…` `config.yaml:171` matches the live M-Auto-3 close baseline →
  §3.4 SHA-recompute gotcha is real and current). Source-pinning is exhausted;
  the fix line is gate noise-robustness (k-of-n majority) + rebaseline + a
  bounded escalation-spec loosening.

  M3-B Single Handover Orchestrator is DEFERRED to a later milestone (human
  decision 2026-06-03).

  Locked v1 decisions (proposal §9; do NOT re-litigate): (a) `samples_per_case`
  default n=3 (n=5 only as a later per-case high-flake override); (b) fitness
  eval primary-provider-only — a DeepSeek→Kimi fallback attempt is excluded from
  the majority vote (`provider_mixed`/`non_comparable`); (c) `min_valid_attempts=3`
  — short-after-retries → `infra_error`/`non_comparable`, never gates.

  Sub-sprint sequence is staged so each step is independently safe + reversible
  (§8.7): S-Auto-16 ships the k-of-n machinery committed INERT (`n=1` =
  byte-identical to today) because the live majority gate is only sound once the
  baseline is ALSO aggregated; S-Auto-17 re-blesses the baseline + flips n=3
  live + runs the validation overnight; S-Auto-18 adds the bounded §5
  escalation-spec eval_spec change.
---

# Milestone M-Auto-4 — Autoloop Fitness Measurement Reliability

## Milestone class

- **Primary layer**: `infra` (autoloop fitness measurement reliability —
  sampling / aggregation / baseline trustworthiness in the
  `autoloop/autoloop/scoring/` harness), with **one bounded `eval_spec`
  sub-change** (§5 escalation-family tiers in `eval_interactive` CaseSpecs +
  `hard_checks.py`). Multi-layer prospective per iteration_governance §7 bundle
  variant; the per-sub-sprint layer split is in the §7 stanza below and the
  sub-sprint sequence.
- **§7 stanza**: **REQUIRED** at milestone level (the milestone as a whole
  touches the `eval_spec` layer via §5). Filled below. Individual sub-sprints
  carry their own stanza; the pure-measurement sub-sprints (S-Auto-16/17) are
  `infra` and would be §7-EXEMPT in isolation but include a stanza for rigor
  because they edit the SHA-locked fitness-gate scoring code.
- **Codex review plan (§4.3)**: default **milestone-shared** at M-Auto-4 close
  over the cumulative S-Auto-16..18 commit range. **Per-sub-sprint Codex review
  is REQUIRED for every sub-sprint that edits the SHA-locked scoring files**
  (`tier_evaluator.py` / `eval_runner.py` / `baseline_loader.py` / `gaming.py`)
  per the fence-#13 controlled-override discipline — that fires the §4.3
  hard-fenced-surface trigger. These per-sub-sprint reviews fold into the
  milestone-shared close prompt (M-Auto-1B / M-Auto-3 precedent).

## Goal

Make autoloop `keep`/`discard` reflect a **reproducible** regression or
improvement rather than single-run LLM-provider measurement noise. Across 67
prior iterations the loop produced **0 `keep` decisions**, short-circuiting at
Layer 0 (Tier-0 safety delta) or Layer 1 (outcome no-regression) every time —
verified to be residual provider non-determinism at temp=0 (the M-Auto-3
substrate is already clean), not propose-quality and not substrate bugs.

The milestone makes the per-case pass/fail signal a **majority vote over
repeated samples**, re-blesses the baseline the same way, and makes
L0/L1/L3/L4 judge on the aggregated signal — so a single noisy flip no longer
gates. **The bot's customer-service ability is explicitly NOT a target of this
milestone**: no skill YAML soft field, prompt, or routing semantic is edited.

Expressed against the curated bad-case suite: the jittery `faq_miss`-not-distress
cases (`cs011` / `cs014` / `cs029`) and the shadow flake (`cs01s01`) stop
flipping the loop between `keep`/`discard` on re-runs of the same commit, and a
post-milestone validation overnight reaches Layer 3/4 on at least some
iterations instead of being structurally pinned at L0/L1 by noise.

## Sub-sprint sequence (3 sub-sprints; §8.1 range)

> Staged per the proposal §8.6 rollout so each step is independently safe and
> reversible (§8.7). The hard ordering constraint: the live majority gate
> (`samples_per_case>1`) is only sound once the baseline is ALSO aggregated —
> so the machinery (S-Auto-16) ships before the live flip + re-bless (S-Auto-17).

1. **S-Auto-16 / Sprint 072 — §0 diagnostic + k-of-n core machinery (committed
   inert).** Layer `infra`. Proposal §8.6 steps 1–4: (§0) instrument + enforce
   the primary-only fitness provider policy (capture
   `actual_provider`/`actual_model`/`fallback_count`/`request_id` per attempt;
   drop `provider_mixed` from the vote); `aggregate.py` (NEW, pure majority +
   pass_rate, unit-tested); `eval_runner` n-loop + per-attempt persistence +
   `min_valid_attempts=3` + retry + `provider_mixed`/`infra_error` drop;
   `tier_evaluator` L0/L1/L2/L3 read the aggregated `majority_passed` signal
   (Tier-0 delta = stable-reproduction). **Committed config keeps
   `samples_per_case: 1` (byte-identical to today, §8.7); the n>1 path is
   exercised only in the §6.1 variance-measurement run + unit tests, NOT in the
   live loop, because the baseline is still single-draw until S-Auto-17.** Edits
   3 of the now-5 SHA-locked files (`eval_runner.py`, `tier_evaluator.py`, and
   `gaming.py` to bring the NEW `aggregate.py` into the `_compute_scoring_code_sha`
   coverage) → fence-#13 controlled override + recompute
   `scoring_code_baseline_sha` + per-sub-sprint Codex. **[CLOSED 2026-06-03 —
   Route A Clean PASS. Machinery shipped INERT (`samples_per_case=1`); §0 = 0%
   fallback + 26 jitter cases genuine non-determinism (the proposal's "two models
   in one run = fallback" assumption corrected — `rerank=kimi-k2.6` is by-design,
   so comparability scoped to `callType=="chat"`); §6.1 variance ↓ all three
   suites (−7.6%/−5.0%/−14.7% bootstrap); 5-file SHA `35305bd8…`→`bb3ced3d…`;
   autoloop 306/0. Archives `docs/sprints/sprint-072-{objective,handoff}.md`.
   Surfaced OQ-S72.1 + OQ-S72.2 → S-Auto-17.]**

2. **S-Auto-17 / Sprint 073 — configured-primary detector fix + baseline re-bless
   (with per-case stability classification) + live n=3 flip + validation-overnight
   gate. [ACTIVE — drafted; contract in `docs/sprint_objective.md`.]** Layer
   `infra`. (a) **Configured-primary detector fix (OQ-S72.1):** anchor the fitness
   fallback detector's "primary" on the CONFIGURED primary model, not the
   empirical modal chat model (so a whole-run fallback can't be silently treated
   as primary under live gating). (b) **Baseline re-bless (§8.4) with per-case
   stability classification (OQ-S72.2):** build the aggregated baseline artifact
   over n≥5 runs/suite, emitting per case one of `stable` (pass_rate≈0/1 → hard
   anchor), `reducible-flaky` (clear lean but jitters → later per-case n=5
   candidate), or `near-coinflip` (pass_rate≈0.5 → `eval_spec`/semantic-ambiguity
   candidate, NOT forced with n=5). Point `baseline_dir` at a fresh dated dir (old
   retained — reversible). (c) **Live flip** the committed `samples_per_case: 3`
   so the loop judges candidate-majority vs baseline-majority symmetrically. (d)
   **Validation-overnight gate (DRAFT; human-launched):** the overnight is gated
   on the stability report — if many anchor bad_cases are `near-coinflip`, do NOT
   launch; surface to the human to choose per-case n=5 / case quarantine /
   `eval_spec` remediation (NO bad_cases drop-budget). Re-bless is human-authorized
   + recorded (`suspect_baseline_manipulation` observes it — expected). Edits
   SHA-locked scoring files (`eval_runner.py`, `baseline_loader.py`) → recompute
   `scoring_code_baseline_sha` (5-file set) + per-sub-sprint Codex. n=3 is the live
   default (empirical-first); n=5 is not pulled forward.

3. **S-Auto-18 / Sprint 074 — bounded escalation-spec family tiers
   (`eval_spec`).** Layer `eval_spec`. Proposal §8.6 step 6 / §5: add
   `acceptable_escalation_families` tiers (`primary` / `secondary_allowed` /
   `forbidden`) to the CaseSpec `expected` block; `hard_checks.py`
   `_check_escalation_compliance` (`:503`) reads the tiers (backward compatible
   — falls back to today's single-family derivation when absent). Loosens reason
   MATCHING only; `should_escalate` / must-escalate obligation untouched.
   Per-case tier curation is a human + Codex `eval_spec` task (no auto-widen,
   §1.7). This is the milestone's bounded eval_spec component.

> **Deferred beyond M-Auto-4 (not committed sub-sprints):** the optional
> per-case `samples_override` n=5 for known high-flake cases (proposal §3.1) —
> apply selectively in a later milestone only if S-Auto-17 evidence shows
> residual flake; the `bad_cases` drop-budget (proposal §5) — revisit ONLY if
> k-of-n + rebaseline still shows 误杀, then only as an explicit config knob +
> human + Codex sign-off.

## Non-goals

- **No bot-capability change.** No skill YAML soft field, prompt, or routing
  semantic is edited anywhere in the milestone.
- **No further source-pinning.** temp is already 0.0 on all three components;
  `seed`/`top_p` are unavailable on DeepSeek/Kimi and absent from the request
  body. Source de-noising is exhausted (verified) — out of scope.
- **No Tier-0 relaxation.** Every Tier-0 safety family (PII,
  escalation_compliance, critical_policy, phase_transition) and its threshold
  stays exactly as today. Only the *measurement decision* changes: a Tier-0
  violation must stably reproduce across the k-of-n samples to count. We relax
  the measurement, never the safety policy.
- **No `bad_cases` drop-budget in v1.** Strict no-drop is preserved.
- **No M3-B Single Handover Orchestrator** (deferred to a later milestone).
- **No cross-turn rank-1 first-refinement gate** (M-Auto-3 OBSERVATION carry,
  OQ-S69.1 residual) — only revisit if a future overnight shows it materially
  corrupts fitness; a product/semantic-policy question, NOT a runtime hardcode.

## Milestone acceptance bar (curated bad-case suite anchored, §5.6 / §8.3)

The milestone closes when, under the aggregated/majority pipeline at the live
`samples_per_case=3` (S-Auto-17) against the re-blessed baseline:

1. **No single-flip discards + every anchor case dispositioned (primary §5.6
   gate)**: manual review of the `bad_cases/` suite traces shows the previously
   jittery cases — `cs011`, `cs014`, `cs029` (faq_miss-not-distress) and the
   shadow flake `cs01s01` — no longer cause a `discard` from a single-run flip
   (proposal §6.2), AND each carries an S-Auto-17 per-case stability class.
   **Refined per OQ-S72.2 (S-Auto-16 finding):** because a genuinely
   `near-coinflip` case (pass_rate≈0.5) CANNOT be stabilized by k-of-n (at p≈0.5
   a 3- or 5-sample majority gives ~0 variance reduction), the bar is NOT "every
   anchor case becomes stable." It is: each anchor case is classified `stable` /
   `reducible-flaky` / `near-coinflip`, and the `near-coinflip` ones are
   explicitly **dispositioned** by deliver + human (per-case n=5 / quarantine /
   `eval_spec`-semantic remediation) rather than silently forced — surfaced as
   the genuine `eval_spec`/semantic signal they are. Each reviewed PASS / FAIL /
   IMPROVING / NEAR-COINFLIP-DISPOSITIONED.
2. **Variance drop (proposal §6.1)** — **MET at S-Auto-16** (real-LLM, fair
   same-method bootstrap: k-of-3 majority ↓ suite-level variance −7.6% bad_cases
   / −5.0% anchor / −14.7% shadow; `docs/sprints/sprint-072-handoff.md` §6.1).
   S-Auto-17's re-bless re-confirms it on the aggregated baseline.
3. **Loop unpinned (proposal §6.4)**: a validation overnight under the new
   harness reaches Layer 3/4 on at least some iterations (no longer structurally
   pinned at L0/L1 by noise).
4. **Safety/grounding floors unchanged**: PII / escalation-obligation /
   critical-policy / phase-transition Tier-0 families intact; L4 shadow gate
   intact + firewalled; grounding diagnostics at/above prior level.
5. **No spurious drift**: `gaming.scoring_code_drift` green post-close (SHA
   recomputed); the baseline re-bless is recorded + human-authorized and
   `suspect_baseline_manipulation` is an expected, explained observation.

HARD close gates (iteration_governance §5.5): curated bad-case manual review
(primary) + Codex §4.1 kernel pass + Java no-regression + safety floor +
grounding floor. Smoke composite / pass-rate = observation only.

## Hard fences (milestone level)

- **No skill YAML soft field / prompt / routing semantic edit** anywhere in the
  milestone (this is a measurement milestone; iteration_governance §1.5/§1.7).
- **No change to `FallbackLlmClient`'s fallback DECISION logic.** The product
  runtime keeps its fallback chain; the §0 work only ADDS provider/model
  metadata capture (the fitness harness drops fallback-served attempts at the
  eval layer, it does not disable the backend's fallback).
- **Tier-0 families: no family removed, no threshold lowered.** Only
  noise-driven single flips are filtered (majority / stable-reproduction).
- **`bad_cases` strict no-drop preserved** (no hidden drop-budget); any future
  budget is an explicit config knob, not a code default.
- **§5 loosens reason MATCHING only**; `should_escalate` / must-escalate
  obligation untouched; `forbidden` families still hard-fail; per-case tiers are
  human + Codex curated, not auto-widened.
- **The 4 SHA-locked scoring files** (`tier_evaluator.py` / `eval_runner.py` /
  `baseline_loader.py` / `gaming.py`) may only be edited under a fence-#13
  controlled override with `scoring_code_baseline_sha` recomputed at sub-sprint
  close + per-sub-sprint Codex.
- **Baseline is reversible**: never overwrite the retained single-run baseline;
  the re-bless writes a fresh dated dir and only moves the pointer.
- **Acceptance evidence is real-LLM reruns (§5.7)**; mocked-LLM tests cover
  projection / rendering / dispatch wiring only and are NOT primary evidence for
  a measurement-behaviour claim.

## R-items consumed / surfaced

- **Consumes**: the M5 LLM-provider non-determinism finding (now the explicit
  measurement-reliability thesis); OQ-S65.7/8 infra-error / give-up degradation
  interaction (the §0 diagnostic connects k-of-n to the existing `loop.py`
  infra-error path); the 0-keep / 67-iter finding.
- **Surfaces (expected)**: the §0 fallback-frequency finding; the per-case
  `samples_override` n=5 candidate list (if S-Auto-17 shows residual flake).
  (RESOLVED at scope lock, no longer an OQ: `aggregate.py` joins the
  `scoring_code_baseline_sha` drift-guard set in S-Auto-16 — the hash set widens
  from 4 to 5 files via `gaming.py` `_compute_scoring_code_sha`, since
  `aggregate.py` feeds the fitness verdict.)

## §7 Layer-classification + anti-hardcode stanza (iteration_governance §7.1)

**Target failure layer:** primarily `infra` (autoloop fitness measurement
reliability — harness sampling / aggregation / baseline trustworthiness in
`autoloop/autoloop/scoring/`), with a bounded `eval_spec` sub-change (§5
escalation-family tiers in `eval_interactive` CaseSpecs + `hard_checks.py`).
Multi-layer prospective per §7 bundle variant; per-sub-sprint layer split is in
the sub-sprint sequence above and bundle policy lives here so Codex can verify
scope discipline.

**Tier-0 invariant:** This milestone adds no Tier-0 invariant. It *preserves*
the existing Tier-0 safety families (PII, escalation_compliance, critical_policy,
phase_transition) at their current strictness and only filters noise-driven
single flips (majority / stable-reproduction) before they gate keep/discard.

**Semantic hardcode:** No semantic hardcode introduced. No skill/prompt soft
field is edited; no keyword/regex/enum routing added. The escalation-family
tiers (§5) are declarative CaseSpec metadata curated by human + Codex, not bot
behaviour, and loosen reason MATCHING only — never the escalation obligation.

**Generalization coverage:** Measurement-reliability milestone — the
"generalization" evidence is the repeated-baseline variance metric (proposal
§6.1) and the jittery-case no-single-flip check (§6.2 / acceptance bar 1), not
target/neighbor/negative/shadow case-family counts. The shadow gate (L4) remains
active and firewalled; only the aggregate majority crosses to the loop. Its role
is unchanged.
