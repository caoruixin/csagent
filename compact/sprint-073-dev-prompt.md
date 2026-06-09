# Dev Prompt — Sprint 073 / S-Auto-17 (M-Auto-4 sub-sprint 2 of 3)

> Self-contained executable view of `docs/sprint_objective.md` (prompt-artifact-rules §9).
> Paste this into a fresh dev session. You need NO other doc except `AGENTS.md`
> (auto-loaded) + this prompt. Code-anchor paths in §10 are read on demand.

## 1. Role identity

You are the **dev agent for Sprint 073 / S-Auto-17**, the SECOND sub-sprint of
Milestone **M-Auto-4 — Autoloop Fitness Measurement Reliability**.

**One-line goal:** Turn the S-Auto-16 INERT k-of-n machinery LIVE — fix the
fallback detector to anchor on the configured primary, re-bless the baseline as an
aggregated artifact (with a per-case stability classification), flip the committed
`samples_per_case` 1→3 (now symmetric), and DRAFT (do not launch) the
validation-overnight gate. MEASUREMENT sprint: no skill/prompt/routing/CaseSpec edit.

## 2. Read order (minimal)

1. `AGENTS.md` — auto-loaded governance (Constitution §1, §1.5/§1.7 anti-hardcode,
   §5 eval acceptance, §5.7 real-LLM gate, §7 stanza).
2. **This prompt** — the full contract.
3. On demand only, the §10 code anchors.

Do NOT read `docs/sprints/*` or `docs/archive/*`.

## 3. WHY n=3 IS NOW SOUND TO FLIP LIVE (read before coding)

S-Auto-16 kept the loop at `samples_per_case=1` because `tier_evaluator`'s majority
gate compares candidate-vs-baseline, and flipping n>1 against a still-single-draw
baseline is asymmetric (baseline lucky-pass vs candidate honest-majority-fail →
manufactured regressions). This sub-sprint removes the asymmetry by re-blessing the
baseline as an aggregated (majority) artifact FIRST, THEN flipping n=3 — both sides
majority-over-n. **Order: detector fix → re-bless → live flip → overnight gate.**

## 4. Two S-Auto-16 findings you consume

- **OQ-S72.1 — configured-primary, not empirical modal.** S-Auto-16's detector
  (`eval_runner._modal_model`) defines "primary" as the dominant `callType=="chat"`
  model across a run. Harmless at 0% fallback, but for LIVE gating a whole-run
  fallback would be silently treated as "all primary." Fix: anchor on the
  CONFIGURED primary model name; fail-safe when unset.
- **OQ-S72.2 — anchor bad_cases may be near-coinflip, which k-of-n CANNOT
  stabilize.** S-Auto-16 §6.1: `bad_cases` mean pass-rate ≈ 0.52, 8/12 flaky; at
  p≈0.5 a 3- (or 5-) sample majority gives ~0 variance reduction (math). So the
  re-bless MUST emit a per-case stability classification, and near-coinflip cases
  are surfaced as an `eval_spec`/semantic-ambiguity signal — NOT force-stabilized
  with n=5. n=3 is the live default; n=5 is NOT pulled forward.

## 5. Class

- **Layer (primary):** `infra` — fitness measurement reliability in
  `autoloop/autoloop/scoring/`. No bot-runtime semantic change; no `eval_spec` edit.
- **§7 stanza:** included (self-walked §8). Pure `infra`/measurement → technically
  §7-EXEMPT, included for rigor (edits SHA-locked scoring code + re-blesses the
  baseline + triggers per-sub-sprint Codex).
- **Codex review plan (§4.3):** PER-SUB-SPRINT REQUIRED — edits SHA-locked
  `eval_runner.py` + `baseline_loader.py` (+ `tier_evaluator.py` if touched) +
  re-blesses the baseline → fence-#13 trigger. **You do NOT dispatch Codex.** Folds
  into the M-Auto-4 milestone-shared close as validation/sign-off (not authorship).
  Record deferral + SHA recompute + human-authorized re-bless in handoff §11.

## 6. Scope (execute in order; proposal §8.6 step 5 + step 7)

### #1 — Configured-primary detector fix (OQ-S72.1)

- Change the §0 provider-comparability detector so "primary" = the **configured
  primary model name**, not the empirical modal `callType=="chat"` model
  (`eval_runner._modal_model`). Add config knob
  `fitness.provider_policy.primary_model` (source the exact string from the
  backend's configured primary; S-Auto-16 saw `deepseek-v4-flash` as the chat
  agent). A `chat` call with model != configured primary → `fallback_count>0` →
  `provider_mixed`.
- **Fail-safe:** if `primary_model` is unset/empty, do NOT fall back to "modal =
  primary." Prefer failing the run with a clear config error; else mark attempts
  `non_comparable` until configured. Record the choice.
- Keep S-Auto-16's `callType=="chat"` scoping (rerank=kimi is by-design). Keyed on
  model id + callType, NOT content (§1.7-clean).
- Tests (`autoloop/tests/test_eval_runner.py`, mockable): chat model != configured
  primary → `provider_mixed`; whole-run-fallback fixture flagged (the bug the modal
  heuristic misses); unset `primary_model` → fail-safe.

### #2 — Baseline re-bless + per-case stability classification (§8.4 + OQ-S72.2)

- On a **clean committed tree** at the target commit, run each suite **n≥5** times
  via the S-Auto-16 n-loop (real-LLM; backend up; creds `autoloop/.env.local`).
- Build `results/<rebaselined-dir>/<suite>/aggregated.json` per case:
  `{majority_passed, pass_rate, attempts[], model, provider, stability_class}` +
  `git_commit` + `captured_at` + `n`. Reuse `aggregate.py` for the majority/pass_rate.
- **Per-case stability classification** from `pass_rate` (a cardinality policy, not
  content): `stable` (≤low or ≥high → hard anchor); `reducible-flaky` (clear lean,
  jitters → later n=5 candidate); `near-coinflip` (mid band ≈0.5 →
  `eval_spec`/semantic candidate, NOT n=5-forced). Default cutoffs in config knob
  `fitness.stability_thresholds` (tunable; record defaults): `stable` if
  `pass_rate ≤ 0.2` or `≥ 0.8`; `near-coinflip` if `0.4 ≤ pass_rate ≤ 0.6`; else
  `reducible-flaky`. Thresholds on a rate, surfaced as a knob — not inline.
- `baseline_loader.py` `load`/`BaselineSnapshot` load the aggregated artifact
  (per-case majority + pass_rate + stability_class). Point
  `config.fitness.baseline_dir` at the new dated dir; **RETAIN the old single-run
  baseline** (`eval_interactive/results/m-auto-1b-baseline-20260529`) — only the
  pointer moves (reversible). Never overwrite the old dir.
- Re-bless is **human-authorized + recorded** in handoff §11; expect
  `gaming.suspect_baseline_manipulation` to observe the change (intended; explain).

### #3 — Live flip `samples_per_case` 1→3 (now symmetric)

- Flip committed `config.fitness.samples_per_case` **1 → 3** (sound only after #2).
- Verify `tier_evaluator` consumes the **baseline-majority** side symmetrically
  (the S-Auto-16 `_effective_case_passed` candidate path exists; confirm the
  baseline side now reads `majority_passed` from the aggregated artifact, not a
  single draw). If a `tier_evaluator.py` change is needed for symmetry, it is in
  scope (SHA-locked → recompute).
- Tests (mockable): candidate-majority vs baseline-majority at n=3; a single noisy
  candidate flip vs a stable baseline majority does NOT gate; a stable candidate
  regression DOES gate.

### #4 — Validation-overnight gate (DRAFT only; human-launched)

- Produce + surface the **per-case stability report** for the anchor bad_cases
  (`cs011`/`cs014`/`cs029`) + shadow `cs01s01`: `pass_rate` + `stability_class`.
- **DRAFT the overnight gate** (do NOT run it — human-launched): the overnight is
  gated on the stability report. If many anchor bad_cases are `near-coinflip`,
  **STOP — do NOT launch**; surface to the human to choose per case: per-case n=5,
  quarantine, or `eval_spec`/semantic remediation. **No `bad_cases` drop-budget.**
- **Near-coinflip case-level review (you produce; human quick-confirms — REQUIRED):**
  do NOT just report a `pass_rate` per `near-coinflip` anchor case — do a
  **case-level trace review first** and surface a structured per-case entry:
  `case_id`; **observed behavior** across the n≥5 draws (passing vs failing draws);
  **pass/fail flip reason** (what differs — escalation-reason family swing,
  turn-count, resolve-vs-escalate, etc.); **eval_spec/semantic-ambiguity judgment**
  (under-specified CaseSpec / genuinely-ambiguous bot behavior vs still-reducible
  flake); **recommended disposition** (per-case n=5 / quarantine /
  `eval_spec`-semantic remediation + one-line rationale). The human does a QUICK
  confirmation, NOT a from-scratch per-case review. This review is the input to the
  overnight go/no-go.
- §6.4 (reaches L3/L4) + §6.2 (no single-flip discards live) come from the
  human-launched overnight AFTER this gate — NOT a S-Auto-17 deliverable. End at
  "re-bless done + detector fixed + n=3 flipped + stability report surfaced +
  overnight gate drafted," then STOP for the human go/no-go.

### #5 — SHA recompute + config

- Recompute `scoring_code_baseline_sha` (`config.yaml:171`) over the **5-file** set
  after editing `eval_runner.py` / `baseline_loader.py` (/ `tier_evaluator.py` if
  touched). Reproduce via the `config.yaml ~:149` one-liner:
  `uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"`.
  Record old→new in handoff. Expect `scoring_code_drift` +
  `suspect_baseline_manipulation` to observe code + baseline changes (intended
  fence-#13 behaviour).
- Committed config deltas: `samples_per_case: 1→3`; `baseline_dir` → new dated dir;
  `+primary_model`; `+stability_thresholds`. Old `baseline_dir` retained in git +
  on disk.

## 7. Hard fences / STOP conditions

- **Do NOT edit any skill YAML soft field, prompt, or routing semantic.** No
  CaseSpec / `eval_spec` edit (§5 escalation tiers = S-Auto-18).
- **Do NOT launch the validation overnight** — human-gated. Produce the stability
  report + gate, then STOP.
- **Do NOT force `near-coinflip` cases with n=5.** Surface as
  `eval_spec`/semantic candidates (OQ-S72.2). n=3 live default; n=5 not pulled
  forward.
- **No `bad_cases` drop-budget.**
- **Do NOT overwrite the retained single-run baseline** — re-bless writes a fresh
  dated dir; only the pointer moves (reversible, §8.7).
- **Configured-primary fail-safe:** never silently trust the empirical modal model
  when `primary_model` is unset.
- **Tier-0 families: no family removed, no threshold lowered** (unchanged from
  S-Auto-16).
- **STOP-and-surface** (handoff §7) if: the re-bless shows many anchor bad_cases
  `near-coinflip` (the designed gate — do NOT launch the overnight); OR
  baseline-majority symmetry needs a bot-facing change; OR the configured-primary
  string cannot be sourced reliably.
- **§5.7 eval evidence gate:** the re-bless + variance re-confirmation are
  real-LLM; mocked-LLM = wiring only.

## 8. §7 Layer-classification + anti-hardcode stanza

**Target failure layer:** `infra` (baseline aggregation + per-case stability
classification + provider-comparability anchor in `autoloop/autoloop/scoring/`). No
`eval_spec` change this sub-sprint.

**Tier-0 invariant:** Adds none. Preserves existing Tier-0 families at current
strictness; the live n=3 flip changes only the measurement decision (majority /
stable-reproduction), not safety policy.

**Semantic hardcode:** None. No skill/prompt soft field; no keyword/regex/enum
routing. Stability classification is a cardinality threshold on `pass_rate` (tunable
config knob); the configured-primary anchor keys on a model id; neither matches
content. Near-coinflip cases are surfaced for human/`eval_spec` triage, not acted on
automatically.

**Generalization coverage:** Measurement sub-sprint — evidence is the re-blessed
aggregated-baseline variance re-confirmation (§6.1) + per-case stability
classification (§6.3 extended to the three-way taxonomy), not case-family counts.
Shadow gate (L4) stays active + firewalled; only aggregate majority crosses to the
loop; role unchanged.

## 9. Test / eval requirements

- **Java:** no regression vs `1213 / 1 / 0 / 2` (autoloop/Python + config only; the
  `primary_model` knob lives in `autoloop/config.yaml`, no server change expected).
- **autoloop pytest:** new detector + baseline + symmetry tests green; no regression
  vs `306`.
- **eval_interactive pytest:** no regression vs `503 / 0` under `uv run`.
- **Real-LLM re-bless (n≥5):** required (baseline artifact + §6.1 variance
  re-confirm; §5.7 — mocks not primary evidence).
- **scoring SHA:** recomputed (5-file) + recorded; drift + suspect explained.

## 10. Code anchors (S-Auto-16 verified 2026-06-03; read/modify on demand)

| Anchor | Path | Use |
|---|---|---|
| `_modal_model` (~:474), `_chat_models` (~:463), `_case_attempt_record` (~:496), `run_v1_fitness_suite:228` | `autoloop/autoloop/scoring/eval_runner.py` | detector fix (#1); n-loop reused for re-bless (#2) |
| `BaselineSnapshot:66`, `load:80` | `autoloop/autoloop/scoring/baseline_loader.py` | load aggregated artifact + stability_class (#2) |
| `_effective_case_passed`, `_evaluate_layer0/1/3` | `autoloop/autoloop/scoring/tier_evaluator.py` | baseline-majority symmetry (#3); edit only if needed |
| `aggregate.py` (majority + pass_rate) | `autoloop/autoloop/scoring/aggregate.py` | reuse for re-bless aggregation (#2); already in 5-file SHA set |
| `_SCORING_CODE_FILES:57`, `_compute_scoring_code_sha` | `autoloop/autoloop/scoring/gaming.py` | SHA recompute (#5); already 5-file |
| `baseline_dir:118`, `samples_per_case:137`, `provider_policy:152`, SHA `:171`, one-liner `~:149` | `autoloop/config.yaml` | config deltas (#5) |
| `eval_interactive/results/m-auto-1b-baseline-20260529` | (old baseline dir) | RETAIN — do not overwrite (#2) |
| `bad_cases/cs011,cs014,cs029` + shadow `cs01s01` | `eval_interactive/case_specs/{bad_cases,...}` + `case_specs_shadow/` | stability report targets (#4); do NOT edit |

## 11. Handoff requirements (author `docs/sprints/sprint-073-handoff.md`)

MUST include: configured-primary detector fix + fail-safe choice; re-bless record
(new `baseline_dir`, n, old dir retained, human-authorization note); the **per-case
stability report** for cs011/cs014/cs029 + cs01s01 (`pass_rate` + `stability_class`);
§6.1 variance re-confirmation on the aggregated baseline; the overnight gate DRAFT +
explicit **STOP-or-GO** recommendation (with the near-coinflip count); **the
structured near-coinflip case-level review** (per case: `case_id` / observed behavior
/ pass-fail flip reason / eval_spec-semantic-ambiguity judgment / recommended
disposition — for the human's quick confirmation); old→new 5-file
`scoring_code_baseline_sha`; confirmation `samples_per_case` committed = 3 + symmetry
evidence; §11 Codex deferral + fence-#13 authorization; §12 self-classification.

## 12. Commit discipline

Stage only authorized-scope files (NOT `git add -A`). One commit per step where
practical (detector / re-bless artifact + pointer / live flip + SHA / overnight gate
doc). Run the re-bless / any eval ONLY on a clean committed tree
(`project_autoloop_dirty_index_hazard`). Do NOT launch the overnight.
Deliver-agent-owned files are bundled by the human at close.

## 13. Self-check checklist (complete before claiming done)

- [ ] Detector anchors on `primary_model` not empirical modal; whole-run-fallback
      fixture flagged; unset → fail-safe; `callType=="chat"` scoping kept; tests green.
- [ ] Re-bless: aggregated artifact per suite (majority + pass_rate +
      `stability_class`); `baseline_dir` → new dated dir; OLD dir retained;
      human-authorization recorded.
- [ ] Per-case stability classification emitted; default thresholds in a config knob
      (not inline); near-coinflip cases identified.
- [ ] Live flip `samples_per_case: 1→3`; candidate-majority vs baseline-majority
      symmetric (tested); single noisy flip does NOT gate; stable regression gates.
- [ ] Overnight gate DRAFTED + stability report for cs011/cs014/cs029/cs01s01
      surfaced; **structured near-coinflip case-level review** (case_id / observed
      behavior / flip reason / eval_spec-semantic judgment / recommended disposition)
      produced for human quick-confirm; overnight NOT launched; STOP-or-GO given.
- [ ] `scoring_code_baseline_sha` recomputed (5-file) + recorded; drift + suspect
      explained.
- [ ] Java / autoloop / eval_interactive baselines no regression.
- [ ] No skill/prompt/routing/CaseSpec edit; no bad_cases drop-budget; no
      near-coinflip n=5 forcing; old baseline retained.
- [ ] Handoff: detector / re-bless / stability report / §6.1 re-confirm / overnight
      gate + STOP-or-GO / SHA / §11 / §12 written.
