---
title: Sprint 073 / S-Auto-17 — configured-primary detector fix + baseline re-bless (per-case stability classification) + live n=3 flip + validation-overnight gate (M-Auto-4 sub-sprint 2 of 3)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-06-03
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-072-objective.md]
superseded_by: null
notes: >
  M-Auto-4 / Sprint 073 / S-Auto-17. SECOND sub-sprint of the Autoloop Fitness
  Measurement Reliability milestone (`docs/milestone_objective.md`). Turns the
  S-Auto-16 INERT machinery LIVE: re-bless the baseline as an aggregated artifact,
  flip the committed `samples_per_case` 1→3, and fix the fallback detector to
  anchor on the configured primary. Proposal §8.6 step 5 + step 7
  (`docs/proposals/autoloop_fitness_measurement_reliability.md`). §5
  escalation-family tiers stay in S-Auto-18.

  **WHY NOW IT IS SOUND TO FLIP n=3 LIVE:** S-Auto-16 deliberately kept the loop
  at `samples_per_case=1` because `tier_evaluator`'s majority gate compares
  candidate-vs-baseline, and flipping n>1 against a still-single-draw baseline is
  asymmetric (baseline lucky-pass vs candidate honest-majority-fail → manufactured
  regressions). This sub-sprint removes that asymmetry by re-blessing the baseline
  as an aggregated (majority) artifact FIRST, then flipping n=3 — so both sides are
  majority-over-n. Order within this sub-sprint: detector fix → re-bless → live
  flip → overnight gate.

  **TWO S-Auto-16 carry-forward findings this sub-sprint consumes:**
  - **OQ-S72.1 — configured-primary, not empirical modal.** S-Auto-16's §0 detector
    defines "primary" as the dominant `callType=="chat"` model across a run
    (`eval_runner._modal_model`). Harmless at 0% observed fallback, but for LIVE
    gating a whole-run fallback would be silently treated as "all primary." Fix:
    anchor on the CONFIGURED primary model name; fail-safe (never silently trust
    the empirical mode) when the configured value is absent.
  - **OQ-S72.2 — the milestone anchor bad_cases may be near-coinflip, which k-of-n
    CANNOT stabilize.** S-Auto-16 §6.1: `bad_cases` mean pass-rate ≈ 0.52, 8/12
    flaky; at p≈0.5 a 3- (or 5-) sample majority gives ~0 variance reduction (math,
    not impl). So the re-bless MUST emit a per-case stability classification and the
    near-coinflip cases are surfaced as an `eval_spec`/semantic-ambiguity signal —
    NOT force-stabilized with n=5. n=3 is the live default (empirical-first, human
    2026-06-03); n=5 is NOT pulled forward.

  **Inherited at HEAD (do NOT re-fix / revert):** S-Auto-16 machinery is FINAL —
  `aggregate.py` (pure majority), `eval_runner` n-loop + per-attempt persistence,
  `tier_evaluator` `_effective_case_passed` majority + L0 stable-reproduction,
  `config.fitness` inert knobs, 5-file `scoring_code_baseline_sha`
  `bb3ced3d37b9d39dd527a0f5be4dcbb0bfea41370ecfccb0b0d317b5c6a26e8d`. Baselines:
  Java `1213/1/0/2` (inherited `SystemPromptUserRequestedTiebreakerTest`,
  OQ-S41.5); autoloop pytest `306/0`; eval_interactive `503/0` under `uv run`;
  17-fixture `31`. Current `baseline_dir` = `eval_interactive/results/m-auto-1b-baseline-20260529`
  (single-draw symlinks — the thing this sub-sprint replaces).

  **Codex review plan (§4.3):** PER-SUB-SPRINT REQUIRED (edits SHA-locked scoring
  files `eval_runner.py` + `baseline_loader.py`, possibly `tier_evaluator.py` →
  fence-#13 hard-fenced-surface trigger), FOLDED INTO the M-Auto-4 milestone-shared
  close Codex. Codex is validation/sign-off, not authorship provenance.

  Dev session source-of-truth: `compact/sprint-073-dev-prompt.md` (self-contained
  per prompt-artifact-rules §9).
---

# Sprint 073 / S-Auto-17 — configured-primary detector + baseline re-bless + live n=3 flip + overnight gate

## Class

- **Layer (primary)**: `infra` — autoloop fitness MEASUREMENT reliability
  (baseline aggregation + provider-comparability in `autoloop/autoloop/scoring/`).
  No bot-runtime semantic change; no `eval_spec` / CaseSpec edit (that is S-Auto-18).
- **§7 stanza**: **included** (self-walked below). Pure `infra`/measurement →
  technically §7-EXEMPT, but included for rigor because it edits the SHA-locked
  fitness-gate scoring code and re-blesses the baseline (a controlled fence-#13
  surface) and triggers a per-sub-sprint Codex review.
- **Codex review plan (§4.3)**: **PER-SUB-SPRINT REQUIRED** — edits SHA-locked
  scoring files (`eval_runner.py`, `baseline_loader.py`, possibly
  `tier_evaluator.py`) + re-blesses the baseline → fence-#13 controlled override.
  The dev does NOT dispatch Codex; the review FOLDS INTO the M-Auto-4
  milestone-shared close prompt as a validation/sign-off pass. Record the
  deferral + SHA recompute + the human-authorized re-bless in handoff §11.
- **Position in milestone**: 2nd of 3 (S-Auto-16 CLOSED → **S-Auto-17** →
  S-Auto-18 §5 escalation tiers). Any second-order issue → an M-Auto-4 sub-sprint,
  NOT a new milestone.

## Goal

Make the autoloop fitness gate judge **candidate-majority vs baseline-majority
symmetrically** at the live `samples_per_case=3`, on a re-blessed aggregated
baseline whose per-case **stability classification** tells the human which anchor
bad_cases are genuinely stabilizable vs which are near-coinflip `eval_spec`/semantic
candidates. Fix the fallback detector to anchor on the configured primary so live
gating cannot be fooled by a whole-run fallback. Prepare — but do NOT launch — the
validation overnight; its launch is human-gated on the stability report.

**This sub-sprint does NOT improve the bot's customer-service ability** (no
skill/prompt/routing/CaseSpec edit) and does NOT change the escalation spec.

## Scope (execute in order; proposal §8.6 step 5 + step 7)

### #1 — Configured-primary detector fix (OQ-S72.1)

- Change the S-Auto-16 §0 provider-comparability detector so "primary" is the
  **configured primary model name**, not the empirical modal `callType=="chat"`
  model (`eval_runner._modal_model`). Add a config knob
  `fitness.provider_policy.primary_model` (sourced from the backend's configured
  primary — confirm the exact model string at implementation; S-Auto-16 observed
  `deepseek-v4-flash` as the chat agent model). A `chat` call whose model differs
  from the configured primary → `fallback_count>0` → `provider_mixed`.
- **Fail-safe (do NOT silently trust the empirical mode):** if
  `primary_model` is unset/empty, the detector MUST NOT fall back to "modal =
  primary." Instead warn loudly and either (a) require the knob (preferred — fail
  the run with a clear config error) or (b) mark attempts `non_comparable` until
  configured. Pick (a) unless it breaks an existing harness path; record the choice.
- Keep the `callType=="chat"` scoping from S-Auto-16 (rerank=kimi is by-design —
  do NOT count non-chat calls as fallback). Still keyed on model id + callType,
  NOT content (§1.7-clean).
- Tests (`autoloop/tests/test_eval_runner.py`, mockable): a chat call with
  model != configured primary → flagged `provider_mixed`; a whole-run-fallback
  fixture (every chat call = fallback model) is correctly flagged (the bug the
  modal heuristic would miss); unset `primary_model` → fail-safe path.

### #2 — Baseline re-bless with per-case stability classification (§8.4 + OQ-S72.2)

- On a **clean committed tree** at the target commit, run each suite **n≥5** times
  via the S-Auto-16 n-loop (real-LLM; backend up; creds in `autoloop/.env.local`).
- Build the aggregated baseline artifact per suite
  (`results/<rebaselined-dir>/<suite>/aggregated.json`): per case
  `{majority_passed, pass_rate, attempts[], model, provider, stability_class}` +
  `git_commit` + `captured_at` + `n`.
- **Per-case stability classification (OQ-S72.2)** from `pass_rate` over the n≥5
  re-bless draws — a measurement (cardinality) policy, NOT a content/semantic rule:
  - `stable` — `pass_rate` ≤ low-cut or ≥ high-cut → a hard anchor.
  - `reducible-flaky` — clear lean but jitters → a later per-case n=5 candidate.
  - `near-coinflip` — `pass_rate` in the mid band (≈0.5) → an
    `eval_spec`/semantic-ambiguity candidate; k-of-n cannot stabilize it; do NOT
    force with n=5.
  - Default cutoffs (config knob `fitness.stability_thresholds`, tunable):
    `stable` if `pass_rate ≤ 0.2` or `≥ 0.8`; `near-coinflip` if `0.4 ≤ pass_rate ≤ 0.6`;
    else `reducible-flaky`. These are thresholds on a rate, not content — surface as
    a config knob with the defaults recorded, not hard-coded inline.
- `baseline_loader.py` `load` / `BaselineSnapshot` load the aggregated artifact
  (per-case majority + pass_rate + stability_class). Point
  `config.fitness.baseline_dir` at the new dated dir; **retain the old single-run
  baseline** (reversible — only the pointer moves; never overwrite the old dir).
- Re-bless is **human-authorized + recorded** in handoff §11; expect
  `gaming.suspect_baseline_manipulation` to observe the `baseline_dir` change
  (intended; explain it).

### #3 — Live flip `samples_per_case` 1→3 (now symmetric)

- Flip the committed `config.fitness.samples_per_case` **1 → 3**. This is sound
  ONLY after #2 (the baseline is now aggregated/majority), so the loop judges
  candidate-majority vs baseline-majority symmetrically.
- Verify `tier_evaluator` consumes the **baseline-majority** side symmetrically
  (the S-Auto-16 `_effective_case_passed` candidate-majority path already exists;
  confirm the baseline side now reads `majority_passed` from the aggregated
  artifact, not a single draw). If a `tier_evaluator.py` change is needed for
  baseline-majority symmetry, that is in scope (SHA-locked → recompute).
- Tests: candidate-majority vs baseline-majority comparison at n=3 (mockable);
  a single noisy candidate flip against a stable baseline majority does NOT gate;
  a stable candidate regression vs stable baseline DOES gate.

### #4 — Validation-overnight gate (DRAFT only; human-launched)

- Produce + surface the **per-case stability report** for the milestone anchor
  bad_cases (`cs011` / `cs014` / `cs029`) + the shadow flake (`cs01s01`): their
  `pass_rate` + `stability_class` from the re-bless.
- **DRAFT the overnight gate procedure** (do NOT run the overnight — it is
  human-launched): the overnight is **gated on the stability report**. If many
  anchor bad_cases are `near-coinflip`, **STOP — do NOT launch**; surface to the
  human to choose, per case: pull the per-case n=5 override, quarantine the case,
  or route to `eval_spec`/semantic remediation. **No `bad_cases` drop-budget.**
- **Near-coinflip case-level review (dev produces; human quick-confirms — REQUIRED):**
  the dev (coding agent) does NOT just report a `pass_rate` for each `near-coinflip`
  anchor case — it does a **case-level trace review first** and surfaces a structured
  per-case entry to the human, with these fields:
  - `case_id`;
  - **observed behavior** across the n≥5 draws (what the bot actually did on passing
    vs failing draws);
  - **pass/fail flip reason** (what differs between the passing and failing draws —
    e.g. escalation-reason family swing, turn-count, resolve-vs-escalate decision);
  - **eval_spec/semantic-ambiguity judgment** — is this an under-specified CaseSpec /
    genuinely-ambiguous bot behavior (an `eval_spec`/semantic signal), or a still-
    reducible measurement flake?
  - **recommended disposition** — per-case n=5 / quarantine / `eval_spec`-semantic
    remediation, with a one-line rationale.
  The human does a **quick confirmation** of these recommendations, NOT a
  from-scratch per-case review. This structured review is the input to the overnight
  go/no-go.
- The §6.4 (loop reaches L3/L4) + §6.2 (no single-flip discards on live cases)
  evidence comes from the human-launched overnight AFTER this gate — it is NOT a
  S-Auto-17 dev deliverable. S-Auto-17 ends at "re-bless done + detector fixed +
  n=3 flipped + stability report surfaced + overnight gate drafted," then STOP for
  the human go/no-go.

### #5 — SHA recompute + config

- Recompute `scoring_code_baseline_sha` (`config.yaml:171`) over the **5-file** set
  after editing `eval_runner.py` / `baseline_loader.py` (/ `tier_evaluator.py` if
  touched). Reproduce via the `config.yaml ~:149` one-liner; record old→new in
  handoff. Expect `scoring_code_drift` + `suspect_baseline_manipulation` to observe
  the code + baseline change (intended fence-#13 behaviour).
- Committed config deltas this sub-sprint: `samples_per_case: 1→3`;
  `baseline_dir` → new dated dir; `+primary_model`; `+stability_thresholds`. The
  old `baseline_dir` value is retained in git history + the old dir on disk.

## Hard fences / STOP conditions

- **Do NOT edit any skill YAML soft field, prompt, or routing semantic.** No
  CaseSpec / `eval_spec` edit (the §5 escalation tiers are S-Auto-18).
- **Do NOT launch the validation overnight** — it is human-gated. Produce the
  stability report + gate, then STOP.
- **Do NOT force `near-coinflip` cases with n=5.** Surface them as
  `eval_spec`/semantic-ambiguity candidates per OQ-S72.2. n=3 is the live default;
  n=5 is NOT pulled forward as a global or per-case default this sub-sprint.
- **No `bad_cases` drop-budget.**
- **Do NOT overwrite the retained single-run baseline.** The re-bless writes a
  fresh dated dir; only the pointer moves (reversible per §8.7).
- **Configured-primary fail-safe:** never silently trust the empirical modal model
  as primary when `primary_model` is unset.
- **Tier-0 families: no family removed, no threshold lowered.** Only the
  measurement decision (majority / stable-reproduction) — unchanged from S-Auto-16.
- **STOP-and-surface** (handoff §7) if: the re-bless shows many anchor bad_cases
  `near-coinflip` (the OQ-S72.2 scenario — do NOT launch the overnight; this is the
  designed gate); OR baseline-majority symmetry cannot be achieved without changing
  bot-facing behaviour; OR the configured-primary string cannot be sourced
  reliably.
- **Eval evidence gate (§5.7):** the re-bless + variance re-confirmation are
  real-LLM; mocked-LLM tests cover wiring only.

## §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (autoloop fitness measurement reliability —
baseline aggregation + per-case stability classification + provider-comparability
anchor in `autoloop/autoloop/scoring/`). No `eval_spec` change this sub-sprint
(deferred to S-Auto-18).

**Tier-0 invariant:** This sub-sprint adds no Tier-0 invariant. It preserves the
existing Tier-0 safety families at current strictness; the live n=3 flip changes
only the measurement decision (majority / stable-reproduction), not any safety
policy.

**Semantic hardcode:** No semantic hardcode introduced. No skill/prompt soft field
edited; no keyword/regex/enum routing added. The stability classification is a
cardinality threshold on `pass_rate` (config knob, tunable); the configured-primary
anchor keys on a model id; neither matches content. The `near-coinflip` band is
explicitly surfaced for human/`eval_spec` triage rather than acted on
automatically.

**Generalization coverage:** Measurement-reliability sub-sprint — the
"generalization" evidence is the re-blessed aggregated-baseline variance
re-confirmation (proposal §6.1) + the per-case stability classification (§6.3
flaky-vs-stable, extended to the three-way taxonomy), not target/neighbor/negative/
shadow case-family counts. The shadow gate (L4) stays active + firewalled; only the
aggregate majority crosses to the loop; its role is unchanged.

## Test / eval requirements

- **Java**: no new regression vs `1213 / 1 / 0 / 2` (S-Auto-17 is autoloop/Python
  + config only; no server change expected — the `primary_model` knob is in
  `autoloop/config.yaml`, sourced from the backend's configured model, not a new
  server field).
- **autoloop pytest**: new detector + baseline + symmetry tests green; no
  regression vs `306`.
- **eval_interactive pytest**: no regression vs `503 / 0` under `uv run`.
- **Real-LLM re-bless (n≥5)**: required (the baseline artifact + the §6.1 variance
  re-confirmation; §5.7 — mocks not primary evidence).
- **scoring SHA**: recomputed (5-file) + recorded; `scoring_code_drift` +
  `suspect_baseline_manipulation` explained.

## Codex review plan (§4.3)

PER-SUB-SPRINT REQUIRED (edits SHA-locked scoring files `eval_runner.py` +
`baseline_loader.py` [+ `tier_evaluator.py` if touched] + re-blesses the baseline →
fence-#13 hard-fenced-surface trigger). Codex is validation/sign-off, NOT an
authorship-provenance source. The dev does NOT dispatch Codex. Record in handoff
§11: the per-sub-sprint deferral into the M-Auto-4 milestone-shared close prompt,
old→new `scoring_code_baseline_sha`, the human-authorized re-bless (with the old
`baseline_dir` retained), and the fence-#13 authorization. Codex checklist focus:
no skill/prompt/routing/CaseSpec edit; Tier-0 unchanged strictness; bad_cases
no-drop preserved (no drop-budget); configured-primary anchor keys on model id not
content + fail-safe present; stability classification is a tunable rate threshold
not a content rule; near-coinflip cases surfaced not force-stabilized; baseline
re-bless reversible (old dir retained); live n=3 flip is candidate-majority vs
baseline-majority symmetric; SHA recomputed; real-LLM evidence.

## Handoff requirements (dev authors `docs/sprints/sprint-073-handoff.md`)

Standard sub-sprint handoff. MUST include: the configured-primary detector fix +
fail-safe choice; the re-bless record (new `baseline_dir`, n, old dir retained,
human-authorization note); the **per-case stability report** for the anchor
bad_cases (cs011/cs014/cs029 + cs01s01) with `pass_rate` + `stability_class`; the
§6.1 variance re-confirmation on the aggregated baseline; the overnight gate
DRAFT + the explicit STOP-or-GO recommendation (with the near-coinflip count); **the
structured near-coinflip case-level review** (per case: `case_id` / observed
behavior / pass-fail flip reason / eval_spec-semantic-ambiguity judgment /
recommended disposition — for the human's quick confirmation); the old→new 5-file
`scoring_code_baseline_sha`; confirmation `samples_per_case` committed = 3 + the
symmetry evidence; §11 Codex deferral note + fence-#13 authorization; §12
self-classification.

## Commit discipline

Stage only authorized-scope files (NOT `git add -A`). One commit per step where
practical (detector fix / re-bless artifact + pointer / live flip + SHA / overnight
gate doc). Run the re-bless / any eval ONLY on a clean committed tree
(`project_autoloop_dirty_index_hazard`). Deliver-agent-owned files are bundled by
the human at close. Do NOT launch the overnight.

## Self-check checklist (dev completes before claiming done)

- [ ] Configured-primary detector: anchors on `primary_model`, not empirical
      modal; whole-run-fallback fixture flagged; unset → fail-safe; `callType=="chat"`
      scoping kept; tests green.
- [ ] Baseline re-bless: aggregated artifact per suite with majority + pass_rate +
      `stability_class`; `baseline_dir` → new dated dir; OLD dir retained
      (reversible); human-authorization recorded.
- [ ] Per-case stability classification emitted; default thresholds in a config
      knob (not inline); near-coinflip cases identified.
- [ ] Live flip `samples_per_case: 1→3`; candidate-majority vs baseline-majority
      symmetric (tested); single noisy flip does NOT gate, stable regression gates.
- [ ] Overnight gate DRAFTED + stability report for cs011/cs014/cs029/cs01s01
      surfaced; **structured near-coinflip case-level review** (case_id / observed
      behavior / flip reason / eval_spec-semantic judgment / recommended disposition)
      produced for human quick-confirm; overnight NOT launched; STOP-or-GO given.
- [ ] `scoring_code_baseline_sha` recomputed (5-file) + recorded (old→new); drift +
      suspect_baseline_manipulation explained.
- [ ] Java / autoloop / eval_interactive baselines no regression.
- [ ] No skill/prompt/routing/CaseSpec/eval_spec edit; no bad_cases drop-budget; no
      near-coinflip n=5 forcing; old baseline retained.
- [ ] Handoff: detector fix / re-bless record / stability report / §6.1 re-confirm /
      overnight gate + STOP-or-GO / SHA / §11 / §12 written.
