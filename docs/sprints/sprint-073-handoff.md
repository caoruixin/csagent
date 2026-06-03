---
title: Sprint 073 / S-Auto-17 dev handoff — make k-of-n LIVE (M-Auto-4)
doc_tier: sprint-archive
status: current
implementation_status: partial
source_of_truth: autoloop/autoloop/scoring/ + autoloop/config.yaml
last_reviewed: 2026-06-03
review_cadence: per sprint
supersedes: []
superseded_by: null
notes: >
  S-Auto-17 turns the S-Auto-16 INERT k-of-n machinery LIVE. MEASUREMENT
  sub-sprint (layer=infra): configured-primary detector fix (OQ-S72.1),
  baseline re-bless as an aggregated majority artifact + per-case stability
  classification (OQ-S72.2), live flip samples_per_case 1->3 (now symmetric),
  and a DRAFTED (not launched) validation-overnight gate. No skill / prompt /
  routing / CaseSpec edit. The overnight validation itself is human-launched
  AFTER this gate.
---

# Sprint 073 / S-Auto-17 — dev handoff

## §0 Cold-start summary

| Field | Value |
|---|---|
| Sub-sprint | S-Auto-17 (2nd of M-Auto-4) |
| One-line goal | Turn S-Auto-16 INERT k-of-n machinery LIVE: detector fix → re-bless → n=3 flip → overnight gate draft |
| Layer (primary) | `infra` — fitness-measurement reliability in `autoloop/autoloop/scoring/` |
| Semantic edits | NONE (no skill/prompt/routing/CaseSpec) |
| Detector fix | OQ-S72.1 — anchor on CONFIGURED primary (`deepseek-v4-flash`), not empirical modal |
| Re-bless | DONE — `eval_interactive/results/m-auto-4-baseline-20260604`, n=5 (+2 auto-retries → 7 draws/case), real-LLM, **0% fallback** |
| Live flip | `samples_per_case` 1→3 COMMITTED (`c1e8675`); baseline_dir pointer moved same commit |
| Overnight gate | DRAFTED only (§3.4); human-launched (`autoloop run`); NOT launched by dev |
| 5-file scoring SHA | `bb3ced3d…` → `0d86b08f…` |
| Codex | per-sub-sprint REQUIRED (SHA-locked files edited + baseline re-blessed); dev does NOT dispatch — folds into M-Auto-4 milestone-shared close |
| STOP-or-GO | **GO (with caveats).** The 3 anchor bad_cases (cs011 stable-fail, cs014 stable-pass, cs029 stable-pass) are all STABLE — 0 near-coinflip among them; only the shadow anchor cs01s01 is near-coinflip. Designed STOP condition ("many anchor bad_cases near-coinflip") NOT met. 4 near-coinflip + 2 pre-existing-infra non_comparable surfaced for the human's quick confirm (§3.3). |

## §1 Scope executed (proposal §8.6 step 5 + step 7)

Execution order followed exactly: **detector fix → re-bless → live flip →
overnight gate**.

### #1 — Configured-primary detector fix (OQ-S72.1)  ✅

- `autoloop/autoloop/scoring/eval_runner.py`: provider comparability now
  anchors on the **configured primary chat model**, sourced once per run from
  `config.fitness.provider_policy.primary_model`. Removed `_modal_model` (the
  per-run empirical mode). A `chat`-callType call whose `model` != the
  configured primary is a real fallback (`fallback_count>0` → `provider_mixed`
  → dropped from the vote), even when the fallback engaged for the WHOLE run —
  the case the modal heuristic silently scored as all-primary.
- **`callType=="chat"` scoping kept** (rerank=`kimi-k2.6` is by-design and
  excluded). Keyed on model id + callType, never content (§1.7-clean).
- **Fail-safe choice (recorded):** when `primary_model` is unset/empty the
  n>1 path **RAISES `EvalRunnerConfigError`** rather than degrading to
  "modal == primary." Rationale: a missing comparability anchor is a config
  error that affects EVERY iteration identically, so fail-fast/loud is correct
  — it is not a recoverable per-iteration condition. The loop's existing
  `EvalRunnerTimeoutError` handler is intentionally NOT extended to swallow
  this (that would re-introduce a silent-degrade path); staying out of
  `loop.py` orchestration also keeps this sub-sprint pure `infra`/scoring.
  At the committed n=1 single-pass path there is no comparability vote, so an
  absent `primary_model` does NOT raise (byte-identical to pre-sprint).
- **Source of the configured-primary string:** `deepseek-v4-flash`, sourced
  from `DEEPSEEK_MODEL` in the repo-root `.env.local` and cross-checked
  against the M-Auto-1B baseline `results.json` — `chat`-callType model is
  `deepseek-v4-flash` across all three suites (bad_cases / anchor_outcome /
  shadow). The comparison target is `llm_calls[].model`, so the config string
  matches what eval-interactive records exactly.
- **Tests** (`autoloop/tests/test_eval_runner.py`, mocked):
  `test_configured_primary_detects_fallback_attempt` (single deviating chat
  draw → provider_mixed), `test_configured_primary_whole_run_fallback_flagged`
  (the bug the modal missed — every draw on the fallback model → all
  non-comparable), `test_unset_primary_model_fail_safe_raises`,
  `test_unset_primary_model_n1_single_pass_unaffected`.

### #2 — Baseline re-bless + per-case stability classification (OQ-S72.2)  ✅

- **Stability classification** (`autoloop/autoloop/scoring/aggregate.py`
  `classify_stability`): a pure cardinality policy on the majority
  `pass_rate` → `stable` (pass_rate ≤0.2 or ≥0.8 — hard anchor),
  `near-coinflip` (0.4 ≤ pass_rate ≤ 0.6 — an `eval_spec`/semantic-ambiguity
  candidate, NOT n=5-forced), `reducible-flaky` (else), `non_comparable`
  (pass_rate None). Thresholds are the tunable config knob
  `fitness.stability_thresholds` (defaults recorded in config). Never reads
  case content (§1.7-clean).
- **Aggregated baseline loader** (`baseline_loader.py`): `load` now prefers
  `<suite>/aggregated.json` (the re-blessed artifact) over `<suite>/results.json`.
  `_summarize_aggregated` counts a case as passed iff its **majority** verdict
  passed (`majority_passed is True`), surfaces `is_aggregated`,
  `stability_by_case`, `pass_rate_by_case`. The legacy single-draw baseline
  (results.json) is unchanged (`is_aggregated=False`).
- **Re-bless tool** (`autoloop/scripts/rebless_baseline.py`, one-off; OUTSIDE
  the 5-file scoring SHA set): runs each suite n=5 via the S-Auto-16 n-loop,
  writes `<out-dir>/<suite>/aggregated.json` per case
  `{majority_passed, pass_rate, attempts[], model, provider, stability_class}`
  + top-level `{git_commit, captured_at, n, samples_per_case, primary_model,
  stability_thresholds, _aggregation, stability_summary}`. The baseline case's
  `l1_results` is set from the majority Tier-0 check map so the Layer-0 delta
  is majority-symmetric too.
- **Re-bless RECORD:**
  - new `baseline_dir`: `eval_interactive/results/m-auto-4-baseline-20260604`
    (on-disk; `eval_interactive/results/*` is gitignored, like the retained
    old baseline — only the config POINTER is tracked, so the move is reversible).
  - **OLD dir RETAINED**: `eval_interactive/results/m-auto-1b-baseline-20260529`
    is never overwritten; the pointer move is the only change.
  - n = 5 base; the n-loop auto-retried twice (retry_cap=2) because 2 shadow
    cases stayed below the min_valid=3 floor → **7 draws per case** for every
    suite. git_commit embedded in each `aggregated.json` = `fc08b74…`.
  - **0% fallback** across the entire re-bless — every chat draw is
    `deepseek-v4-flash`, `fallback_count=0` (clean single-model population; the
    configured-primary anchor never had to drop a provider_mixed draw).
  - **First run discarded:** a first re-bless attempt (out-dir `…-20260603`)
    was interrupted by a multi-hour Mac sleep that froze attempts mid-LLM-call;
    it was killed and its partial artifacts removed rather than blessed (draws
    frozen mid-flight across an 8-hour suspension cannot be certified clean).
    The committed baseline is the clean re-run (`…-20260604`).
  - **Human-authorization note:** the re-bless was run under the S-Auto-17
    contract's explicit instruction to re-bless on a clean committed tree
    (proposal §8.6 step 5). `gaming.suspect_baseline_manipulation` is expected
    to observe the `baseline_dir` change — intended (see §11).

### #3 — Live flip `samples_per_case` 1→3 + baseline-majority symmetry  ✅

- Candidate side already reads the majority verdict via
  `tier_evaluator._effective_case_passed` (S-Auto-16). S-Auto-17 makes the
  **baseline side symmetric**: `baseline_loader._summarize_aggregated`
  computes the baseline pass count from `majority_passed`, and
  `tier_evaluator._baseline_case_pass_map` reads `majority_passed` (for the
  non-comparable credit). No other `tier_evaluator` change was needed.
- **Symmetry tests** (`autoloop/tests/test_tier_evaluator.py`, mocked):
  - `test_symmetry_candidate_majority_vs_baseline_majority_keep` — both sides
    majority-pass; a single noisy minority flip does NOT gate.
  - `test_symmetry_stable_candidate_regression_vs_baseline_majority_gates` —
    the discriminator: gates ONLY because the baseline side reads
    `majority_passed` (=1); a stable candidate majority-FAIL regresses Layer 1.
  - `test_symmetry_baseline_majority_fail_no_new_regression` — baseline
    blessed as majority-FAIL → candidate majority-FAIL is not a new regression.
  - `test_symmetry_noncomparable_credit_uses_baseline_majority` — the
    non-comparable credit keys on the baseline MAJORITY pass.
- **Live flip:** `config.fitness.samples_per_case` 1→3 committed (`c1e8675`)
  together with the `baseline_dir` pointer move (atomic — n=3 against a
  single-draw baseline is the asymmetry this sub-sprint removes). End-to-end
  load sanity: `baseline_loader.load(m-auto-4-baseline-20260604, config)` →
  `is_aggregated=True` for all 3 suites; majority pass counts 5/4/2.
- **§5.7 boundary:** the symmetry tests above are mocked-LLM (wiring). The
  real-LLM evidence that the baseline-majority side is sound is the re-bless
  itself (§3.2 variance re-confirmation).

### #4 — Validation-overnight gate (DRAFT only)  ✅

See §3 below — stability report, structured near-coinflip case-level review,
overnight gate draft + STOP-or-GO recommendation. The overnight (§6.4 reaches
L3/L4 + §6.2 no single-flip discards live) is human-launched AFTER this gate
and is NOT an S-Auto-17 deliverable.

### #5 — SHA recompute + config  ✅

- 5-file `scoring_code_baseline_sha` recomputed over the post-edit state
  (`tier_evaluator.py` + `eval_runner.py` + `baseline_loader.py` + `gaming.py`
  + `aggregate.py`): **`bb3ced3d…` → `0d86b08f…`**
  (`0d86b08fc0309196d9fc49f4924fe446b36d6c04301c4bcb406ca12337dd149e`).
  Reproduce at HEAD: `cd autoloop && uv run --extra dev python -c "from
  autoloop.scoring.gaming import _compute_scoring_code_sha;
  print(_compute_scoring_code_sha())"`.
- Committed config deltas: `+provider_policy.primary_model: deepseek-v4-flash`;
  `+stability_thresholds {stable_low:0.2, stable_high:0.8, coinflip_low:0.4,
  coinflip_high:0.6}`; SHA bump (commit `00a4592`). `samples_per_case 1→3` +
  `baseline_dir` → `m-auto-4-baseline-20260604` (commit `c1e8675`). The SHA is
  unaffected by config edits (config.yaml is not in the 5-file set —
  reconfirmed `0d86b08f…` after the config flip).

## §2 Tests / eval (no regression)

| Suite | Baseline | This sub-sprint | Status |
|---|---|---|---|
| Java | 1213 / 1 / 0 / 2 | 1213 / 1 / 0 / 2 | ✅ no regression (the 1 failure is the documented inherited `SystemPromptUserRequestedTiebreakerTest`) |
| autoloop pytest | 306 | **324** (306 + 18 new) | ✅ no regression |
| eval_interactive pytest (`uv run`) | 503 / 0 | 503 / 0 | ✅ no regression |
| scoring SHA | `bb3ced3d…` | `0d86b08f…` (recorded) | ✅ recomputed |

18 new autoloop tests: 6 `classify_stability` + 4 configured-primary detector
+ 4 aggregated-baseline loader + 4 baseline-majority symmetry.

**§5.7 note:** all the above are mocked-LLM (wiring/rendering only). The
real-LLM evidence is the re-bless variance re-confirmation (§3.2).

## §3 Re-bless results — stability report + near-coinflip review + STOP-or-GO

Source: `eval_interactive/results/m-auto-4-baseline-20260604/_rebless_report.json`
+ the per-suite `aggregated.json` `attempts[]` traces. n=5 base + 2 auto-retries
→ 7 provider-comparable draws per case. 0% fallback (all draws `deepseek-v4-flash`).

### §3.1 Per-case stability report — the contract anchors

| Anchor | suite | majority_passed | pass_rate (7 draws) | stability_class |
|---|---|---|---|---|
| `cs011_uc_c_faq_miss_not_distress` | bad_cases | **False** | 0/7 = 0.00 | **stable** (stable-fail; hard anchor) |
| `cs014_uc_c_faq_miss_not_distress` | bad_cases | **True** | 6/7 = 0.857 | **stable** (stable-pass; hard anchor) |
| `cs029_uc_d_account_locked_callback` | bad_cases | **True** | 7/7 = 1.00 | **stable** (stable-pass; hard anchor) |
| `cs01s01_uc_c_notification_lag_detailed` | shadow | **False** | 3/7 = 0.429 | **near-coinflip** |

**All 3 anchor bad_cases are STABLE hard anchors.** Only the shadow anchor
`cs01s01` is near-coinflip. This is the central input to the STOP-or-GO (§3.4).

### §3.2 §6.1 variance re-confirmation (whole suite)

Stability distribution over all 46 cases (3 suites): **stable 35 /
reducible-flaky 5 / near-coinflip 4 / non_comparable 2**.

The majority verdict is robust exactly where it should be: 35/46 cases are
`stable` (pass_rate ≤0.2 or ≥0.8 → a single noisy draw cannot flip the
majority). The residual-variance set is the 4 `near-coinflip` cases (all at
pass_rate ≈ 0.429) — and as OQ-S72.2 predicted, a k-of-n majority gives ~0
variance reduction at p≈0.5, so these are surfaced for triage, NOT forced
with a larger n.

**Honest-majority baseline shift** (old single-draw → new majority-over-7):

| suite | OLD passed (single draw) | NEW majority-passed | note |
|---|---|---|---|
| bad_cases | 5/12 | 5/12 | unchanged |
| anchor_outcome | 7/12 | **4/12** | 3 lucky single-draw passes are majority-fails (uc_f reducible-flaky 0.286, uc_fp_removed near-coinflip 0.429, + others at 0.0) |
| shadow | 4/22 | **2/22** | +2 now `non_comparable` (cs59s01/cs59s02 infra-error) instead of mis-counted as `case_passed=False` |

This shift is the entire reason the re-bless had to precede the n=3 flip: the
old single-draw baseline was OPTIMISTIC (lucky passes), so an honest-majority
candidate at n=3 would have manufactured anchor_outcome "regressions" against
it. Both sides are now honest-majority. The shift will be observed by
`gaming.suspect_baseline_manipulation` (intended; §11).

**Pre-existing infra (NOT caused by S-Auto-17):** the 2 `non_comparable`
shadow cases `cs59s01_uc_d_empty_form_account_recovery` and
`cs59s02_uc_f_empty_form_payout_timing` fail ALL 7 draws identically with
`session_create_failed: HTTP 400 on /v1/chat/sessions` (empty-form inputs).
Both were `status=ERROR` in the OLD baseline too — so this is a pre-existing
backend/eval-harness condition. The re-bless actually IMPROVES their handling:
the old single-draw baseline mis-scored them as `case_passed=False` (counted
against the shadow pass-rate); the aggregated baseline correctly marks them
`non_comparable` (excluded from gating). Flagged as an infra OQ below.

### §3.3 Structured near-coinflip case-level review (human QUICK-confirm)

Four cases at pass_rate ≈ 0.429 (3/7). Per case: observed behavior across the
7 draws / flip reason / eval_spec-semantic judgment / recommended disposition.
**None is recommended for n=5 forcing** (anti-误杀: a near-coinflip case is a
genuine-ambiguity signal, not a budget to spend more samples on).

1. **`cs01s01_uc_c_notification_lag_detailed`** (shadow anchor) — pass 3 / fail 4.
   - *Observed:* passing draws (a0/a3/a5) cleanly `bot_ended` with
     `esc=faq_miss_threshold_exceeded` + only advisory groundedness tags.
     Failing draws split two ways: (a1/a6) reach `goal_impossible`/`goal_achieved`
     tripping `L2_GATE:correct_outcome` + `L2:escalation_timing`; (a2/a4)
     `turn_budget_exhausted`/faq-miss tripping `TIER2:search-knowledge-before-faq-answer`.
   - *Flip reason:* the bot oscillates between resolving via FAQ-grounding and
     either over-running turn budget or mis-timing escalation on the same
     UC-C notification-lag input.
   - *Judgment:* **eval_spec / genuine semantic ambiguity** — the case can
     legitimately resolve via FAQ OR escalate, and the rubric flips on
     `escalation_timing`/`correct_outcome`. Not a reducible flake.
   - *Disposition:* **eval_spec-semantic remediation** — clarify the expected
     outcome + escalation-timing rubric for cs01s01 (candidate for S-Auto-18
     escalation-tier work). Do NOT force n=5.

2. **`alice_uc_a_uc_h_misclass`** (bad_cases) — pass 3 / fail 4.
   - *Observed:* passing draws (a2 turn_budget, a3/a5 user_requested) end
     cleanly; failing draws are heterogeneous — a0 `faq_miss` +
     `TIER2:search-knowledge-before-faq-answer`, a1 `loop_detected` + L1 fails,
     a4/a6 `user_requested` but trip `L1:source_citation_present`.
   - *Flip reason:* the UC-A↔UC-H boundary is a soft semantic call; the bot
     resolves it ~half the time, and even on the "right" path the rubric flips
     on a citation/record-outcome advisory.
   - *Judgment:* **eval_spec / semantic ambiguity** — this is a curated
     bad_case for the A/H misclass failure; near-coinflip means the bot
     already handles it ~half the time (the failure it was built to exhibit is
     partially gone).
   - *Disposition:* **eval_spec-semantic review** of whether the case still
     exhibits a single crisp failure. Surface for human; do NOT force n=5.

3. **`cs095_uc_d_email_recovery_misroute`** (bad_cases) — pass 3 / fail 4.
   - *Observed:* the clearest pattern — a0-a3 FAIL with `stop=goal_achieved` +
     `L1:trace_minimum`; a4-a6 PASS with `turn_budget_exhausted`/`user_requested`
     + `bot_ended`. The FAIL draws all reach `goal_achieved` but trip the
     `L1:trace_minimum` check.
   - *Flip reason:* the `L1:trace_minimum` check fails specifically on the
     `goal_achieved` path — when the bot drives to goal_achieved it fails
     trace_minimum; when it ends via turn_budget/user_requested it passes.
   - *Judgment:* **borderline — possible measurement interaction.** Lean
     eval_spec: investigate whether `L1:trace_minimum` should fail on the
     `goal_achieved` path (could be a reducible rubric artifact) vs a genuine
     bot trace gap.
   - *Disposition:* **eval_spec review of the trace_minimum × goal_achieved
     interaction.** Do NOT force n=5 until that is resolved (forcing more
     samples would just re-measure the same rubric interaction).

4. **`anchor_outcome_uc_fp_removed`** (anchor_outcome) — pass 3 / fail 4.
   - *Observed:* a0-a2 PASS (turn_budget/user_requested, bot_ended); a3-a6 FAIL
     with `loop_detected`, `goal_achieved` + `L1:trace_minimum`,
     `STALL:PLACEHOLDER_WITHOUT_FOLLOWUP`, `L1:escalation_reason_consistency`,
     `TIER2:uc-h-intake-complete-before-handover`.
   - *Flip reason:* on this false-positive-removed case the bot oscillates
     between correctly NOT escalating and looping / stalling / mis-routing into
     UC-H intake.
   - *Judgment:* **eval_spec / genuine semantic ambiguity** — the FP-removed
     scenario is exactly where "should the bot act?" is borderline.
   - *Disposition:* **eval_spec-semantic review**; near-coinflip → surface, do
     NOT force n=5.

**Reducible-flaky (5, FYI — not near-coinflip, clearer lean):**
bad_cases `cs015_uc_fp_appeal_edit_repost` (0.286), `iwzx_uc_k_advert_on_hold_restore`
(0.714), `wmkb_uc_a_trader_flag_secondary_uc_h` (0.714); anchor_outcome
`uc_f_billing` (0.286); shadow `uc_*` (1). These have a clear majority lean
that still jitters — later n=5 candidates IF the loop needs them, NOT this
sub-sprint.

### §3.4 Overnight gate DRAFT + STOP-or-GO

**Gate purpose:** a human-launched validation overnight that exercises the
LIVE n=3 loop end-to-end against the re-blessed aggregated baseline — i.e.
reaches L3/L4 (proposal §6.4) and confirms no single noisy flip discards a
candidate (§6.2). It is the first real run of the now-symmetric majority gate.

**Gate condition (pre-launch):** the overnight is gated on this stability
report. **STOP — do NOT launch** if MANY anchor bad_cases are `near-coinflip`
(a near-coinflip anchor means the gate cannot reliably tell improvement from
noise on the very cases the loop is meant to fix). Otherwise GO.

**Launch (human-run, NOT dev; clean committed tree required — dirty-index
hazard):**
```
cd autoloop && uv run autoloop run --experiments <N>     # e.g. -n 20 overnight
# pre-flight: backend UP, autoloop/.env.local creds, clean tree.
# each iteration now runs every suite 3× and gates on the majority vs the
# re-blessed majority baseline.
```

**STOP-or-GO recommendation: GO (with caveats).**
- **STOP condition NOT met.** The 3 anchor bad_cases `cs011`/`cs014`/`cs029`
  are all `stable` hard anchors (0 near-coinflip among them). Near-coinflip
  count is 4/46 total, and only 1 of those is an anchor (the shadow `cs01s01`).
- **Caveats to record in the overnight launch note (human quick-confirm):**
  1. The 4 near-coinflip cases (§3.3) are surfaced as eval_spec/semantic
     candidates for S-Auto-18; the overnight will treat them as the residual-
     variance set, not as gate-able regressions (their `comparable` majority
     still gates only if it crosses, and at p≈0.43 they sit just below 0.5).
  2. The 2 `non_comparable` shadow cases (cs59s01/cs59s02) need a separate
     infra fix (session-create 400 on empty-form inputs) before they can
     contribute shadow signal — they are excluded from gating, so they do NOT
     block the overnight, but they are a measured blind spot.
- **No `bad_cases` drop-budget** was applied (per fences). **No near-coinflip
  case was forced with n=5.**

**End of S-Auto-17 dev scope.** The overnight run itself (§6.4 L3/L4 + §6.2
no-single-flip-discard) is human-launched AFTER this gate and is NOT an
S-Auto-17 deliverable. STOP here for the human go/no-go.

## §11 Codex deferral + fence-#13 authorization

- **Codex per-sub-sprint review REQUIRED** (§4.3 fence-#13 trigger): this
  sub-sprint edits SHA-locked `eval_runner.py` + `baseline_loader.py` +
  `aggregate.py` + `tier_evaluator.py` and re-blesses the baseline. Per the
  contract the **dev agent does NOT dispatch Codex**; the review folds into
  the M-Auto-4 milestone-shared close as validation/sign-off.
- **fence-#13 authorization:** the SHA-locked scoring-code edits + the
  baseline re-bless are the human-authorized scope of the S-Auto-17 contract.
  Old→new SHA recorded (§1 #5). Expected observation-only gaming flags during
  the next live run: `scoring_code_drift` (code changed — but config SHA now
  matches HEAD, so it should be clean) and `suspect_baseline_manipulation`
  (the `baseline_dir` pointer moved) — both intended fence-#13 behaviour;
  the baseline change is recorded here so the iteration-record notes carry it.

## §12 Self-classification (§7 stanza)

- **Target failure layer:** `infra` (baseline aggregation + per-case stability
  classification + provider-comparability anchor in `autoloop/autoloop/scoring/`).
  No `eval_spec` change.
- **Tier-0 invariant:** adds none; preserves existing Tier-0 families at
  current strictness. The live n=3 flip changes only the measurement decision
  (majority / stable-reproduction), not safety policy.
- **Semantic hardcode:** none. Stability classification is a cardinality
  threshold on `pass_rate` (tunable knob); the configured-primary anchor keys
  on a model id; neither matches content. Near-coinflip cases are surfaced for
  human/`eval_spec` triage, not acted on automatically.
- **Generalization coverage:** measurement sub-sprint — evidence is the
  re-blessed aggregated-baseline variance re-confirmation (§6.1) + per-case
  stability classification, not case-family counts. Shadow gate (L4) stays
  active + firewalled; only aggregate majority crosses to the loop.
- **§7 applicability:** pure `infra`/measurement → technically §7-EXEMPT;
  stanza included for rigor (edits SHA-locked scoring code + re-blesses the
  baseline + triggers per-sub-sprint Codex).

## Commit log (this sub-sprint)

1. `00a4592` — #1 configured-primary detector + re-bless machinery (INERT n=1)
   + 5-file SHA recompute + config knobs (primary_model, stability_thresholds).
2. `fc08b74` — #2 fix: rebless out-dir resolves against CWD (tool only).
3. `c1e8675` — #3 live flip: `samples_per_case` 1→3 + `baseline_dir` pointer →
   `m-auto-4-baseline-20260604` (atomic; both sides majority). The re-bless
   `aggregated.json` artifacts live on-disk and are gitignored (like the
   retained old baseline) — only the tracked config pointer moves.
4. (this commit) — overnight gate draft + STOP-or-GO + this handoff §3.
