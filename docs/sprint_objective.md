---
title: Sprint objective — Sprint 059 / M-Auto-1B S-Auto-6 — First overnight batch + First human review + First cherry-pick to main
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-28
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-058-objective.md]
superseded_by: null
notes: >
  SECOND and FINAL planned sub-sprint of Milestone M-Auto-1B —
  Auto-Evolution Calibration (per `docs/milestone_objective.md` §3
  sequence). S-Auto-6 takes the calibrated detector (`synonym_map_enabled=true`
  per Fix-C step 2 Path A landed by S-Auto-5 commit `ae0ec3e`) and runs
  the first auto-loop overnight batch against the v1 47-case dataset,
  conducts the first deliver-agent + human §5.6-style manual review of
  kept candidates, and executes the first cherry-pick of an auto-loop
  output to main (M-Auto-1B fence #7 ALLOWS EXACTLY ONCE — changed from
  M-Auto-1A's "no cherry-pick"). §7 REQUIRED (`eval_spec`); **Codex
  milestone-shared at M-Auto-1B close per §4.3 default** UNLESS the
  cherry-pick candidate touches a §5.3 borderline that requires
  per-sub-sprint Codex (deliver-agent + human jointly judge at the
  cherry-pick decision point).

  Builds on S-Auto-5 close `ae0ec3e` (Codex per-sub-sprint verdict
  `pass / 0` sub-classified `approve with downgrade-to-signal
  follow-up` 2026-05-28; the only follow-up trigger is the NEW
  `R-S58-anti-hardcode-zero-width-when-arrow-bypass` which is
  non-blocking for S-Auto-6 per the Codex verdict). The calibrated
  detector has Fix-C step 1 word-boundary regex live AND
  `synonym_map_enabled=true`; 17-fixture sweep preserved 11/11 +
  4/4 + 2/2; detector self-discipline 3 regression tests PASS; rule
  count UNCHANGED at 11.

  Two human-coupled gates inside S-Auto-6 that the dev session
  cannot satisfy alone:

  - **Step #3 §5.6 manual review** of overnight kept candidates is a
    deliver-agent + human joint judgment (NOT programmatic alone);
    dev triggers the eval + opens the trace surfaces, but the PASS /
    FAIL / IMPROVING / borderline-§5.3 decision is human-judgment.
  - **Step #4 cherry-pick decision via AskUserQuestion** is human-driven
    (the dev does NOT pick which kept candidate becomes the cherry-pick
    OR judge no-cherry-pick justification).

  These two gates make S-Auto-6 a hybrid dev + deliver-agent + human
  session. The dev prompt embeds explicit STOP-and-surface points at
  each gate so the dev never silently auto-picks a candidate.

  R-S58 disposition decision (whether to extend M-Auto-1B by a
  Fix-D sub-sprint `S-Auto-7` OR defer to M-Auto-2) is also surfaced
  at S-Auto-6 close — informed by overnight evidence (whether any
  bypass variant from real meta-agent propose distribution PASSes the
  detector beyond R-S58's zero-width signature).
---

# Sprint 059 / M-Auto-1B S-Auto-6 — First overnight batch + First human review + First cherry-pick to main

## Class

`eval_spec` (§3.2 Q6 — sub-sprint consumes the per-iteration fitness verdict sequence on a calibrated detector; the cherry-pick mechanism + §5.6 manual review of kept candidates are eval-side acceptance-bar gates, not runtime semantic changes). The cherry-picked Skill YAML edit (if any) is a single-field-class edit on `procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[*].desc` per the sandbox white-list — semantic-touching at the LLM-soft-narrative surface (§1.3 LLM owns content; Runtime owns boundary).

**§7 REQUIRED** — S-Auto-6 ships at most ONE Skill YAML edit on main (via cherry-pick mechanism); that edit may shift LLM-soft-narrative behaviour. The §7 stanza guards the structural integrity of the substrate machinery + the human-judgment-gate respect.

## Goal

S-Auto-6 close 时:

1. **Pre-batch baseline drift envelope established.** ≥2 baseline reruns of the v1 47-case fitness suite (`bad_cases` ×12 + `anchor_outcome` ×12 + `shadow` ×23) on the calibrated detector at S-Auto-5 close HEAD `ae0ec3e`; median per-suite `case_passed` count + IQR recorded in `docs/sprints/sprint-059-handoff.md` §X "Baseline drift envelope". If baseline-vs-baseline drift exceeds 5/34 cases (the M-Auto-1A close-day signature), surface to deliver-agent + human BEFORE starting overnight.

2. **First overnight batch executed.** `python -m autoloop run --experiments <N>` for N in [10, 20] (target ≥10; budget 6-8h). Each iteration writes to `autoloop/results/runs/exp-<N>/` + `experiments.jsonl` + `iterations.sqlite` + `lessons.md` (K=10 lessons_compactor triggers automatically if iteration count reaches K). Crash recovery (S-Auto-3 substrate) handles transient LLM API errors. Total errors ≤50% acceptable; >50% triggers halt + investigate (NOT scope creep — this is an OQ for substrate sub-sprint candidate).

3. **First §5.6-style manual review of kept candidates.** Deliver-agent + human jointly read per-turn traces (open `eval_interactive/results/<run-id>/` + `autoloop/results/runs/exp-<N>/`) for EACH kept candidate; judge PASS / FAIL / IMPROVING jointly (NOT programmatic alone); filter through the drift envelope from Goal #1; classify as **eligible-for-cherry-pick** / **deferred-to-M-Auto-2+** (kept on `autoloop/keep-<N>` branch) / **discarded** (manual review FAIL despite programmatic PASS).

4. **First cherry-pick decision via AskUserQuestion.** Deliver-agent surfaces the eligible candidate slate; human selects EXACTLY ONE candidate to cherry-pick OR 0 candidates with explicit "no human-approved candidate" justification. If cherry-pick lands: `python -m autoloop apply --experiment exp-<N>` Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit per OQ-S55.1 disposition 2026-05-27); human inspects `git status` + stages explicitly + commits manually with message `Sprint 059 / S-Auto-6 / M-Auto-1B — apply exp-<N> to main`. The cherry-picked Skill YAML diff MUST be a single-file + single-field-class diff on `procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[*].desc` per the M-Auto-1B fence #3 / #7 / #9 chain (sandbox already enforces structurally; this is post-cherry-pick verification).

5. **Observation accumulation + R-S58 disposition recommendation.** `autoloop/config.yaml` `fitness.baseline_dir` advance past the cherry-pick commit (if any) so the next milestone baseline is post-cherry-pick. Cumulative observation captured in handoff: per-iteration elapsed time average; FLAG rate distribution; gaming flag counts + severity; `shadow_disagreement_rate` first measurement (§6 architecture-health metric); whether any NEW bypass variant beyond R-S58 surfaced in real meta-agent propose distribution. Recommendation on R-S58 disposition: (a) defer to M-Auto-2 Fix-D sub-sprint (if no new bypass surface AND zero-width shape did NOT manifest in overnight propose distribution); (b) extend M-Auto-1B with a Fix-D sub-sprint S-Auto-7 (if new bypass surfaces beyond R-S58 OR zero-width shape manifests at meaningful rate). Recommendation is dev-agent + deliver-agent observation; **final R-S58 disposition is a deliver-agent + human planning-round decision at M-Auto-1B close** — NOT a dev-side call.

6. **Milestone-shared Codex review at M-Auto-1B close** (separate later pass dispatched by deliver-agent at milestone close; this sub-sprint does NOT itself dispatch). UNLESS the cherry-pick candidate touches a §5.3 borderline that requires per-sub-sprint Codex — in that case deliver-agent + human dispatch per-sub-sprint Codex at S-Auto-6 close BEFORE M-Auto-1B close.

**Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (M-Auto-1B fence #13 content-hash lock `5177b674...`). **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py` (S-Auto-1/2/3/4/5 substrate + S-Auto-5 calibrated detector — UNCHANGED in S-Auto-6). **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` / `cli.py`. **Zero touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint/milestone archives. **Conditional touch** to `server/src/main/resources/skills/*.yaml`: EXACTLY ONCE via cherry-pick mechanism in Goal #4 ONLY; OUTSIDE the cherry-pick path, zero touch.

## Scope (numbered; this is the contract)

### #1 — Pre-batch baseline rerun + drift envelope

Run the baseline 47-case fitness suite at S-Auto-5 close HEAD `ae0ec3e` twice (or more if median+IQR width is unstable; cap at 3):

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 0 --baseline-rerun
```

(If `--baseline-rerun` is not a wired CLI flag at S-Auto-5 close HEAD, run the eval suites manually via `cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/` + `... --path case_specs/anchor_outcome/` + `... --path case_specs_shadow/` and aggregate per-suite `case_passed` count. Verify the flag's existence by reading `autoloop/autoloop/cli.py` BEFORE running; if absent, STOP and surface to deliver-agent.)

Record per-baseline-run aggregate `case_passed` counts per suite. Compute:

- Median per suite (bad_cases: M_bc; anchor_outcome: M_ao; shadow: M_sh)
- IQR per suite (where ≥3 runs; for 2 runs, use min/max range as proxy)
- Cross-suite total median + IQR

**Acceptance**:

- Baseline-vs-baseline drift ≤ M-Auto-1A close-day signature (5/34 cases = 14.7% across bad-case + shadow combined). If drift > 5/34, STOP and surface to deliver-agent + human BEFORE starting overnight; joint decision to (a) proceed with widened envelope or (b) halt for upstream LLM-provider investigation.

Record the drift envelope in `docs/sprints/sprint-059-handoff.md` §X "Pre-batch baseline drift envelope".

### #2 — First overnight batch

Run:

```bash
cd autoloop && uv run --extra dev python -m autoloop run --experiments 15
```

(Adjust `--experiments` value within [10, 20] based on the per-iteration elapsed-time estimate from S-Auto-5: ~12-15 min/iter → 20-30 iter possible in 6h; substrate cleanup + restart between iterations may add overhead. The S-Auto-5 4 iterations actually completed in 122s / 87s / 37s wall-clock total — well under estimate; full Spring eval cycle wasn't tested due to spawn failure. **Pre-overnight smoke**: dispatch 1 iter with `--experiments 1` BEFORE the overnight to measure actual end-to-end elapsed including the eval cycle; if >40 min, adjust `--experiments` count downward.)

**Pre-overnight smoke required**: BEFORE the overnight batch, run `python -m autoloop run --experiments 1` to measure the actual end-to-end iteration elapsed including the eval cycle (Spring spawn + 47-case eval; S-Auto-5 exp-2 only got to Spring spawn step before failure → the actual `eval_runner` execution time is NOT yet measured live). If first complete iteration succeeds AND elapsed ≤25 min, proceed to overnight at full count. If elapsed 25-40 min, reduce overnight count proportionally. If >40 min, STOP and surface.

During overnight:

- Crash recovery (S-Auto-3 substrate) handles transient LLM API errors automatically; per-iter state persists in `experiments.jsonl` + `iterations.sqlite` so a mid-batch crash doesn't lose completed iterations.
- Spring spawn failures (the OQ-S58.7 pattern from S-Auto-5) are recoverable per-iteration: the loop logs the error + proceeds to next iteration. Do NOT patch `applier.py` mid-overnight (hard fence #13).
- Lessons compactor (S-Auto-3) triggers automatically at K=10 iteration count if reached; first `autoloop/results/lessons.md` LLM-distilled lesson lands during the overnight.

**Stop conditions during the overnight**:

- Total errors >50% within the first 5 iterations: halt + surface to deliver-agent + human; investigate (LLM API, infra, Spring spawn pathology). Recover already-completed iterations via state persistence; do NOT scope-creep into substrate fixes.
- Per-iteration elapsed time average >40 min (sustained, not single outlier): halt + surface; substrate optimization is M-Auto-2+ scope.
- A pattern of bypass shapes from real meta-agent propose distribution PASS through the detector (beyond R-S58 zero-width): record + halt OR continue based on deliver-agent + human joint decision (depends on frequency + severity).

### #3 — §5.6-style manual review of kept candidates

Next morning, deliver-agent + human jointly conduct manual review.

For EACH kept candidate (one per `autoloop/results/runs/exp-<N>/iteration_record.json` with `terminal_decision == "keep"`):

1. Open the per-turn traces:
   - `eval_interactive/results/<run-id>/results.json` for the bad_cases + anchor_outcome runs (sample turn-level detail per kept candidate's eval).
   - `autoloop/results/runs/exp-<N>/{hypothesis.json, diff.yaml, sandbox_verdict.json, anti_hardcode_verdict.json, tier_evaluator_verdict.json}` for the propose-stage + verdict artefacts.
   - The M5 admin trace UI surface (post-M5 S2 deliverable) for per-invocation LLM raw response inspection — useful for understanding WHY the meta-agent proposed THIS edit.
2. Sample turn-level traces on bad_cases (Tier-1 + Tier-2 evidence) + anchor_outcome (closure_criterion alignment).
3. Joint PASS / FAIL / IMPROVING / borderline-§5.3 verdict, recorded per candidate in handoff §X table.
4. Filter through the drift envelope from #1: a programmatic-PASS candidate inside the drift envelope is NOT a true PASS — it may be provider-drift noise.
5. Classification:
   - **eligible-for-cherry-pick**: manual review PASS + NO §5.3 borderline + outside drift envelope (real improvement).
   - **deferred-to-M-Auto-2+**: manual review PASS but borderline §5.3 ("drift to keyword bot"-style edit even though programmatically PASS); kept on `autoloop/keep-<N>` branch for M-Auto-2+ re-review.
   - **discarded**: manual review FAIL despite programmatic PASS (typically: change introduces a new failure shape OR inside drift envelope OR over-fits a narrow target case at expense of neighbor / negative-control).

Record the full candidate slate in `docs/sprints/sprint-059-handoff.md` §X "Manual review of kept candidates" with classification rationale per candidate.

### #4 — Cherry-pick decision via AskUserQuestion

**STOP-and-surface to deliver-agent + human** at this step. The dev session does NOT pick a candidate OR judge no-cherry-pick justification — the AskUserQuestion is dispatched by deliver-agent.

Deliver-agent surfaces the eligible-for-cherry-pick slate via AskUserQuestion with one row per candidate:

- target_skill (file basename)
- target_field (path; e.g., `$.critical_steps[4].desc`)
- before_value (first 80 chars + ...)
- after_value (first 80 chars + ...)
- programmatic verdict (Layer 0-4 PASS summary)
- manual review verdict (PASS + classification rationale)
- deliver-agent recommendation

Human selects EXACTLY ONE candidate to cherry-pick OR 0 candidates with explicit "no human-approved candidate" justification.

**If cherry-pick lands** (human selects 1 candidate):

```bash
cd autoloop && uv run --extra dev python -m autoloop apply --experiment exp-<N>
```

Hybrid mode behaviour (per OQ-S55.1 disposition 2026-05-27): cherry-pick + emit baseline patch (showing the new Skill YAML state) + NO auto-commit. Human inspects:

```bash
git status        # verifies single-file diff on server/src/main/resources/skills/<file>.yaml
git diff          # verifies single-field-class change matching exp-<N>'s diff.yaml
```

If `git diff` shows MORE than ONE file OR a field outside `procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[*].desc`, **STOP and surface**: applier produced an out-of-fence diff, which should be structurally impossible per the M-Auto-1A sandbox. This would be a substrate bug requiring fix-iteration S-Auto-6.1 (or M-Auto-2 substrate hardening).

If verified clean:

```bash
git add server/src/main/resources/skills/<file>.yaml
git commit -m "Sprint 059 / S-Auto-6 / M-Auto-1B — apply exp-<N> to main

[2-3 sentences describing the cherry-picked edit + manual review verdict + deliver-agent + human joint signature]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

The applier branch `autoloop/exp-<N>` stays as the source-of-truth pre-cherry-pick state; `autoloop/keep-<N>` (if applier creates it) carries the post-cherry-pick branch tag.

**If 0 cherry-pick** (human declines all candidates OR no eligible candidates):

Deliver-agent records the "no human-approved candidate" justification in handoff §X with: (a) per-candidate decline rationale (one line each), (b) deliver-agent + human joint signature, (c) impact on M-Auto-1B close gate (0 cherry-pick is PASS if all other gates PASS). All eligible candidates stay on `autoloop/keep-<N>` branches for M-Auto-2+ review.

### #5 — Observation accumulation + R-S58 disposition recommendation

Record in `docs/sprints/sprint-059-handoff.md`:

- **Per-iteration elapsed time table**: exp-<N>, target_skill, target_field, iteration_decision, discard_reason, elapsed_s, propose_succeeded (similar to S-Auto-5 §4 table; one row per overnight iter).
- **Cumulative FLAG rate**: count of `anti_hardcode_flag_for_codex: true` records across overnight + percentage.
- **Gaming flag distribution**: count by `severity` (WARN vs ERROR) + by `rule_id` across overnight.
- **`shadow_disagreement_rate` first measurement** (§6 architecture-health metric): fraction of kept candidates whose `tier_evaluator_verdict.layer_4` shadow regression was within tolerance but the manual review classified as drift / borderline.
- **R-S58 disposition recommendation**:
  - If NO new bypass variant from real meta-agent propose distribution surfaces beyond R-S58 zero-width AND R-S58 zero-width signature did NOT manifest in any overnight propose: **recommend defer to M-Auto-2** (low priority; the bypass requires invisible-character obfuscation which is low-probability in real meta-agent output).
  - If R-S58 zero-width signature DID manifest in overnight propose OR new variant surfaces: **recommend extend M-Auto-1B with S-Auto-7 Fix-D sub-sprint** (§8.5 ceiling: M-Auto-1B + S-Auto-7 = 3 sub-sprints, still under 5).
  - **Final R-S58 disposition is deliver-agent + human at M-Auto-1B close** — NOT dev's call.

If cherry-pick landed, advance `autoloop/config.yaml` `fitness.baseline_dir` past the new commit so M-Auto-2 baseline is post-cherry-pick (recorded as part of the close-bundle by deliver-agent at M-Auto-1B close — NOT a dev-side edit unless deliver-agent explicitly delegates).

## Hard fences / STOP conditions

- **Zero touch** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (M-Auto-1B fence #13).
- **Zero touch** to `autoloop/autoloop/sandbox/{yaml_diff_validator,applier,content_validator,anti_hardcode_check}.py` (S-Auto-1 through S-Auto-5 substrate).
- **Zero touch** to `autoloop/autoloop/loop.py` / `meta_agent/` / `memory/` / `cli.py`.
- **Zero touch** to `eval_interactive/eval_interactive/**`, case_spec, case_specs_shadow, `server/src/main/java/`, `eval/src/main/java/`, `data/`, `db/`, `server/src/main/resources/{prompts,scripts,config,mock}/**`, `docs/foundational/`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-058-*`, milestone archives, `docs/codex-findings.md` scaffold.
- **Conditional touch** to `server/src/main/resources/skills/*.yaml`: EXACTLY ONCE via the Goal #4 cherry-pick mechanism; outside cherry-pick, zero touch. The cherry-pick diff MUST be single-file + single-field-class (`procedure` / `grounding_instruction` / `escalation_policy` / `critical_steps[*].desc` only).
- **At MOST 1 cherry-pick** during S-Auto-6 (M-Auto-1B fence #7 / #17). Any additional kept candidates stay on `autoloop/keep-<N>` branches awaiting M-Auto-2+ review.
- **No `git add -A`** by dev — stage S-Auto-6 scope files explicitly. The cherry-pick commit is a separate single-file stage (`git add server/src/main/resources/skills/<file>.yaml`). Deliver-agent close-bundle artefacts bundled by human at close.
- **No new Tier-0 invariant**. C2/C3 DEFER continues. If overnight surfaces a Tier-0 candidate observation, surface as R-item for M-Auto-2+ planning.
- **No new heavy deps** in `autoloop/pyproject.toml`.
- **No LLM call** in any new code (S-Auto-6 is execution-driven; no new code expected beyond handoff documentation).
- **No human-judgment-gate bypass**: dev does NOT pick cherry-pick candidate OR judge no-cherry-pick alone; AskUserQuestion via deliver-agent is the human-judgment gate per §5.6.
- **No silent skip of pre-overnight smoke**: the `--experiments 1` smoke is mandatory BEFORE the overnight batch (S-Auto-5 did not measure full Spring + eval cycle live; overnight without smoke = risk of 6h wasted on a substrate pathology).
- **STOP and surface** conditions:
  - Pre-batch baseline drift envelope width >5/34 cases (M-Auto-1A close-day signature exceeded).
  - Pre-overnight smoke iter elapsed >40 min.
  - Overnight total errors >50% within first 5 iterations.
  - Per-iter average elapsed >40 min sustained.
  - Multiple bypass shapes from real meta-agent propose distribution PASS detector beyond R-S58.
  - Cherry-pick `apply` mechanism produces out-of-fence diff (multi-file OR field outside white-list).
  - Any kept candidate's manual review surfaces a borderline §5.3 case (deliver-agent + human jointly judge whether to discard or surface for per-sub-sprint Codex re-review BEFORE M-Auto-1B close).

## Test / eval requirements

- **Python autoloop suite**: `cd autoloop && uv run --extra dev pytest -q` — S-Auto-5 close baseline `223 passed, 1 warning` UNCHANGED (S-Auto-6 expected to add 0 new pytest tests — execution-driven sub-sprint; if observation infrastructure requires new tests, add to handoff §X with justification).
- **Existing Python eval_interactive suite UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` reproduces `486 passed, 3 failed`.
- **Java baseline UNCHANGED**: `Tests run: 1183, Failures: 1, Errors: 0, Skipped: 2` UNCHANGED from M-Auto-1A close `b6b627b` IF cherry-pick lands and the Skill YAML edit does NOT regress Java tests (Skill YAML loading is via Spring config + `SkillRegistry`; potential Java-side touch is class-loading + YAML parse only — semantically the edited Skill YAML is consumed by LLM-side projection, not Java decision-paths, so baseline preserved by construction). Run `cd server && mvn test -B -pl server` AFTER cherry-pick (if any) to verify.
- **Live-iter end-to-end at scale**: ≥10 overnight iterations complete to terminal verdict (keep / discard / error all acceptable per S-Auto-5 precedent).
- **§5.6 manual review evidence at sub-sprint close**: per-kept-candidate trace review documented with deliver-agent + human joint PASS/FAIL/IMPROVING/borderline classification.
- **Shadow regression-safety check at M-Auto-1B close** (separate from S-Auto-6; milestone-close gate per M5 / M-Auto-1A precedent). If cherry-pick landed, shadow drop on the 23 shadow cases must be ≤3%.
- **No live LLM call in pytest tests** — overnight LLM calls happen via `python -m autoloop run` (CLI invocation, NOT pytest).

## §7 — Layer-classification + anti-hardcode stanza

**Target failure layer:** `eval_spec` (§3.2 Q6 — sub-sprint's primary action is consuming per-iteration fitness verdict sequence + cherry-pick eligibility decision; both are eval-side acceptance bars, not runtime semantic changes). The cherry-picked Skill YAML edit (if any) is the LLM-soft-narrative surface (§1.3 LLM owns content) — semantic-touching in the sense that it changes LLM-visible Skill instruction text, but the change is sandbox-validated + anti-hardcode-validated + tier_evaluator-validated + §5.6-manual-reviewed BEFORE landing on main. The cherry-pick is therefore a §5.6-gated structured improvement, NOT a §1.7 violation.

**Tier-0 invariant:** adds no Tier-0 invariant. The cherry-picked Skill YAML edit (if any) does NOT modify any current Tier-0 invariant per `docs/runtime_freeze_and_risk_policy.md` §1/§2 (which govern Java-side safety floor, not Skill YAML LLM-soft narrative content). The §3.2 Q2 detector rule in the calibrated anti_hardcode_check.py (existing S-Auto-4 rule + S-Auto-5 Fix-C step 1) actively REJECTS any proposed Tier-0 invention attempt — overnight kept candidates have been filtered through this check at propose-stage.

**Semantic hardcode:** S-Auto-6 introduces no NEW semantic hardcode in source code. The cherry-picked Skill YAML edit (if any) MUST pass the calibrated anti_hardcode_check (Fix-C step 1 + `synonym_map_enabled=true` from S-Auto-5) at propose-stage; manual review §5.6 additionally guards against borderline §5.3 "drift to keyword bot" candidates. The detector + manual review chain is the §1.7 enforcement; S-Auto-6 does NOT modify the chain itself (sandbox + anti_hardcode + content_validator + gaming all hard-fenced per #13). Any post-cherry-pick observation of a §1.7-style regression in the cherry-picked content would be a fix-iteration S-Auto-6.1 (revert + re-evaluate; OR refine via M-Auto-2+).

**Generalization coverage:**

- **target** = (i) ≥10 overnight iterations complete to terminal verdict on `auto-loop-branch` against the calibrated detector + real meta-agent LLM; (ii) ≥1 §5.6 manual review of kept candidates (or 0 if no kept candidates surface); (iii) cherry-pick decision (1 or 0) recorded with deliver-agent + human joint signature.
- **neighbor** = the cherry-picked candidate (if any) does NOT regress neighboring cases — i.e., for the target_skill + target_field that was edited, related test cases in `bad_cases` / `anchor_outcome` / `shadow` do NOT shift from PASS → FAIL beyond the drift envelope from #1.
- **negative-control** = the cherry-picked candidate (if any) does NOT shift the M-Auto-1B close-day bad-case manual review distribution (expected: ≥1 case moves to IMPROVING or PASS if cherry-pick lands targets a R-iwzx / R-bad-case-suite-uc-ghij-seed-from-real-sessions like case; otherwise expected to match M-Auto-1A close distribution).
- **shadow** = shadow firewall posture UNCHANGED in S-Auto-6 (no detector / config / scoring change). Overnight kept candidates have been filtered through Layer 4 shadow regression check (≤3% drop) by tier_evaluator; the cherry-picked candidate's shadow drop ≤3% guarantee carries through to main.

## Codex review plan (§4.3)

**Default**: milestone-shared at M-Auto-1B close. Codex consumes cumulative range `<M-Auto-1A-close>..<M-Auto-1B-close>` covering both sub-sprints + cherry-pick commit (if any).

**Per-sub-sprint Codex CONDITIONAL on cherry-pick borderline-§5.3**:

Deliver-agent + human jointly judge at S-Auto-6 cherry-pick decision point (Goal #4 AskUserQuestion):

- If the cherry-pick candidate is unambiguously clean §5.6 manual review PASS, NO §5.3 borderline → milestone-shared Codex (default).
- If cherry-pick candidate is §5.3 borderline ("drift to keyword bot" risk despite programmatic PASS) → per-sub-sprint Codex required BEFORE M-Auto-1B close (deliver-agent dispatches `compact/sprint-059-codex-review-prompt.md`; Codex verifies (a) the cherry-picked edit is structurally clean; (b) §1.7 forbidden-list compliance; (c) §5.3 borderline acceptable as `approve with downgrade-to-signal follow-up`).
- If cherry-pick candidate's manual review surfaces a CLEAR §5.3 violation → discard the candidate; do NOT cherry-pick; close with 0 cherry-pick.

**Verdict expected** (milestone-shared at M-Auto-1B close): `pass / 0` or `approve with downgrade-to-signal follow-up` (likely; R-S58 deferred trigger).

## Handoff requirements

Author `docs/sprints/sprint-059-handoff.md` at S-Auto-6 close. Leave **§12** empty (deliver-agent + human at milestone close). Required sections:

- **§1 Class + §7 stanza self-walk** — confirm against delivered scope.
- **§2 Goal achievement** — bullet each of the 6 Goal items + evidence pointer.
- **§3 Scope execution log** — for each scope step #1-#5, brief status (DONE / PARTIAL / SKIPPED-WITH-REASON).
- **§4 Pre-batch baseline drift envelope** — per-baseline-run aggregate `case_passed` counts per suite (≥2 reruns); median + IQR per suite + cross-suite total; drift envelope decision (proceed with measured envelope OR halt for upstream LLM-provider investigation).
- **§5 Pre-overnight smoke + overnight batch record** — `--experiments 1` smoke iter outcome + elapsed time; overnight batch: `--experiments <N>` value chosen + rationale; per-iteration table (iter_id, target_skill, target_field, iteration_decision, discard_reason, elapsed_s, propose_succeeded, anti_hardcode_flag_for_codex, gaming_flags count); cumulative counts (keep / discard / error / total).
- **§6 Manual review of kept candidates** — per-kept-candidate row with target_skill, target_field, programmatic verdict, manual review PASS/FAIL/IMPROVING/borderline-§5.3 classification, classification rationale, eligibility-for-cherry-pick / deferred-to-M-Auto-2+ / discarded.
- **§7 Cherry-pick decision** — AskUserQuestion record (deliver-agent surfaces; human selects); selected candidate (or "no human-approved candidate" + justification); `python -m autoloop apply --experiment exp-<N>` Hybrid output; `git status` + `git diff` verification of single-file + single-field-class diff; cherry-pick commit SHA + footer.
- **§8 Observation accumulation** — per-iteration elapsed-time average; cumulative FLAG rate; gaming flag distribution by severity + rule_id; `shadow_disagreement_rate` first measurement; whether NEW bypass variants surfaced from real meta-agent propose distribution beyond R-S58.
- **§9 R-S58 disposition recommendation** — defer-to-M-Auto-2 OR extend-M-Auto-1B-with-S-Auto-7; deliver-agent observation; final disposition is deliver-agent + human at M-Auto-1B close.
- **§10 Code anchor table** — `git show --numstat <commits>` for any commits landed during S-Auto-6 (the cherry-pick commit, if any; the handoff commit; substrate observation commits if any).
- **§11 Test count** — autoloop pytest UNCHANGED at `223 passed, 1 warning` baseline (S-Auto-6 expected to add 0 tests; if tests added, breakdown here).
- **§12 OQ-S59.x list** — open questions surfaced during execution; disposition per OQ.

You do NOT author the milestone-shared Codex review prompt (deliver-agent's job at M-Auto-1B close).

## Commit discipline

S-Auto-6 expected commit pattern (commit-at-end; one or two commits):

**Commit 1 (conditional; cherry-pick only IF a candidate is selected at Goal #4)**:

```
Sprint 059 / S-Auto-6 / M-Auto-1B — apply exp-<N> to main

[2-3 sentences describing the cherry-picked edit: target_skill + target_field + edit summary; manual review verdict; deliver-agent + human joint signature]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage ONLY: `server/src/main/resources/skills/<file>.yaml` (the single-file Skill YAML edit from `python -m autoloop apply --experiment exp-<N>` Hybrid mode output).

**Commit 2 (always; S-Auto-6 close handoff)**:

```
Sprint 059 / S-Auto-6 / M-Auto-1B — first overnight batch + first human review + first cherry-pick <or "no cherry-pick" + justification>

[2-3 sentences describing the overnight batch outcome (N iter completed; keep/discard/error counts); manual review verdict on kept candidates; cherry-pick decision + cherry-pick commit pointer if applicable; R-S58 disposition recommendation]

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

Stage ONLY: `docs/sprints/sprint-059-handoff.md` (the dev-authored handoff archive).

**No `git add -A`**. Deliver-agent close-bundle artefacts (this objective archive rename at close, milestone objective §12 closure verdict, milestone-shared Codex prompt + findings, 10-handoff updates, action_bank R-item annotations) bundled by human at close per `feedback_commit_at_end_bundles_deliver_artefacts.md`.

## Scope size (§8.5 note)

M-Auto-1B with S-Auto-6 = 2 of 2 sub-sprints planned (within §8.5 5-sub-sprint ceiling; margin = 3 if fix-iteration S-Auto-5.1 / S-Auto-6.1 / Fix-D S-Auto-7 needed). S-Auto-6 is execution-driven + observation-driven; estimated 1-2 dev-days execution + 1 overnight (6-8h auto-loop) + 1-2 days deliver-agent + human manual review + cherry-pick decision + apply + close-bundle.

## OQ (open questions — filled during the sub-sprint)

- **OQ-S59.x candidates** (expected; dev surfaces as ambiguities encountered):
  - **OQ-S59.1** — `--baseline-rerun` CLI flag exists OR substitute path. Verify via `python -m autoloop --help` BEFORE running #1.
  - **OQ-S59.2** — pre-overnight smoke iter elapsed time vs. S-Auto-5 estimate (12-25 min target).
  - **OQ-S59.3** — `--experiments <N>` value chosen for overnight + rationale (function of pre-overnight smoke + 6-8h budget).
  - **OQ-S59.4** — whether overnight Spring spawn failure rate exceeds S-Auto-5 single observation (exp-2 1/3 = 33% of attempted spawns; if overnight reproduces, surface OQ to M-Auto-2 substrate optimization).
  - **OQ-S59.5** — whether lessons_compactor K=10 trigger fires during overnight; if yes, first `lessons.md` LLM-distilled lesson content.
  - **OQ-S59.6** — kept-candidate count distribution vs. S-Auto-5 augmented-evidence expectation. If 0 kept candidates from overnight, deliver-agent + human assess whether meta-prompt refinement is M-Auto-2+ scope.
  - **OQ-S59.7** — whether any kept candidate touches `R-iwzx-uc-k-vs-uc-h-routing-spurious-distress` or `R-bad-case-suite-uc-ghij-seed-from-real-sessions` natural-target territory; if yes, the manual review specifically validates against those R-items' expected behaviour.
  - **OQ-S59.8** — whether R-S58 zero-width-bypass shape manifests in overnight propose distribution (informs the R-S58 disposition recommendation at #5).
  - **OQ-S59.9** — cherry-pick decision rationale + 0-cherry-pick justification (if applicable).
  - **OQ-S59.10** — `config.fitness.baseline_dir` advance + new baseline directory path (if cherry-pick lands; deliver-agent close-bundle).

Add OQ entries as ambiguities are encountered; record disposition per OQ in handoff §12.
