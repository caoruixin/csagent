---
title: Sprint 063 / S-Auto-8 — first overnight batch + first human review + first cherry-pick to main (M-Auto-1C — Auto-Evolution Calibration Continuation sub-sprint 2 of 2)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-30
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-062-objective.md]
superseded_by: null
notes: >
  M-Auto-1C / Sprint 063 / S-Auto-8. **Layer**: `eval_spec` (consumes
  per-iteration fitness verdict sequence on the now-validated substrate
  after S-Auto-7.2 retired OQ-S61.1 + OQ-S62.1 + OQ-S62.2; §5.6 manual
  review + cherry-pick are eval-side acceptance bars). **§7 stanza**:
  REQUIRED (semantic-touching via cherry-pick if it lands). **Codex
  review plan**: milestone-shared (default) at M-Auto-1C close UNLESS
  cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint
  Codex pre-milestone-close per §4.3 trigger #2 — i.e., manual review
  finds a candidate at the "drift to keyword bot" edge where
  programmatic PASS but human judgment is split).

  **Estimated dev**: ~3-5 days total (1-2 days execution including the
  ≥2 baseline rerun drift envelope per Codex M-Auto-1B Axis M6 trigger
  #1 acceptance + 1 overnight via raw shell/screen/tmux per OQ-S62.3
  operational workaround + 1-2 days deliver-agent + human manual
  review + cherry-pick decision + apply + close-bundle).

  **CRITICAL operational workaround (OQ-S62.3 from S-Auto-7.2)**: the
  Claude Code bash-tool harness sandbox SIGKILLs long-running Python
  orchestrators mid-Step 7 eval_runner. S-Auto-8 dev MUST run the
  smoke iter + overnight batch via **raw shell / screen / tmux /
  nohup outside the bash-tool sandbox**, NOT via Claude Code's
  Bash tool with long-running invocation. The harness limitation is
  environment-level, not autoloop code; the substrate is already
  fixed at S-Auto-7.2 close. If S-Auto-8 dev attempts via bash-tool
  it will hit the same OQ-S62.3 termination and Step 9 / experiments.
  jsonl row write will NOT capture. See §1 below for the embedded
  workaround details.

  **Three pre-overnight readiness items inherited from S-Auto-7.2**:

  1. **≥2 baseline rerun drift envelope** (Codex M-Auto-1B Axis M6
     trigger #1 acceptance per `docs/milestones/M-Auto-1B_objective.md`
     §12.14): the elevated UC-D/E/F/FP LLM-provider drift signature
     observed at M-Auto-1B Phase 2 (anchor_outcome 7/12 → 3/12 +
     shadow 4/22 → 1/22; 7/34 = 20.6% vs M-Auto-1A close 5/34 =
     14.7%) requires establishing a per-suite case_passed median + IQR
     drift envelope BEFORE overnight kick-off. The ≥2 rerun discipline
     is M-Auto-1C's primary risk mitigation against false-positive
     "keeps" produced by overnight comparison against a single noisy
     baseline. If baseline-vs-baseline drift exceeds 10/34 cases
     (~30%; 2× the M-Auto-1A close-day signature), HALT + deliver-
     agent + human jointly investigate per `iteration_governance.md`
     §10 stop condition.

  2. **OQ-S62.3 operational workaround**: run autoloop via raw shell
     (NOT Claude Code bash-tool) so the harness sandbox doesn't
     SIGKILL the orchestrator. Recommended invocation:
     `nohup python -m autoloop run --experiments 15 > autoloop-overnight.log 2>&1 &`
     in a regular Terminal / iTerm / screen / tmux session.

  3. **Stale autoloop/exp-* branch cleanup** (deferred per deliver-
     agent + human joint decision 2026-05-30): the S-Auto-7.2 OQ-
     S62.2 fix makes _git_create_branch idempotent so S-Auto-8 will
     target exp-14 cleanly regardless, but the 8 stale exp-*
     branches (autoloop/exp-2, exp-6, exp-7, exp-8, exp-10, exp-11,
     exp-12, exp-13) add branch-listing noise. Recommended cleanup at
     S-Auto-8 open: `git branch -D autoloop/exp-{2,6,7,8,10,11,12,13}`.
     Destructive; requires explicit human authorization at dispatch.

  **Bad-case cherry-pick reference signals from M-Auto-1B Phase 2**:
  cs001_uc_c_mechanical_template_escalate (Skill YAML edit candidate
  on `resolve_faq_grounded_answer.yaml` procedure / critical_steps[*]
  .desc to gate `goal_impossible` terminal-state on policy-mandated
  `search_knowledge` completion) + wmkb_uc_a_trader_flag_secondary_uc_h
  (Skill YAML edit candidate on `resolve_intake_collect_and_handover
  .yaml` critical_steps[*].desc to gate handover on intake
  completion). These are REFERENCE SIGNALS (deliver-agent + human use
  during manual review at S-Auto-8 close), NOT pre-committed cherry-
  picks; the meta-agent owns proposal authorship and the cherry-pick
  decision is made via AskUserQuestion on the actual overnight kept-
  candidate slate.

  **R-S58 propose-distribution scan**: during overnight monitoring,
  accumulate per-iteration propose-stage real-meta-agent output
  corpus. After overnight closes, scan corpus for U+200B / Unicode
  `category() == "Cf"` format characters in propose text (especially
  around WHEN / WHENEVER + arrow shapes). Provides evidence for R-S58
  final disposition at M-Auto-1C close: defer-to-M-Auto-2 if 0
  occurrences across overnight OR extend-with-S-Auto-9-Fix-D if the
  bypass shape appears at meaningful rate.

  Dev session source-of-truth: `compact/sprint-063-dev-prompt.md`
  (self-contained per `iteration_governance.md` §9). Dev session
  reads ONLY: `AGENTS.md` (auto-loaded) + the dev prompt.
---

# Sprint 063 / S-Auto-8 — first overnight batch + first human review + first cherry-pick to main

## Class

- **Layer (primary)**: `eval_spec` (consumes per-iteration fitness verdict sequence on the now-validated substrate; cherry-pick decision via AskUserQuestion + §5.6 manual review).
- **§7 stanza**: **REQUIRED** (semantic-touching via cherry-pick if it lands; see §7 below for stanza).
- **Codex review plan (§4.3)**: milestone-shared (default) at M-Auto-1C close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint per §4.3 trigger #2 pre-milestone-close).
- **Sub-sprint position in milestone**: 2nd of 2 (S-Auto-7.2 substrate fix CLOSED → S-Auto-8 first overnight + first cherry-pick → M-Auto-1C close).

## Goal

Exercise the now-validated substrate (post-OQ-S61.1 + OQ-S62.1 + OQ-S62.2 fixes) end-to-end via first overnight batch + first human review of kept candidates + first cherry-pick to main. Retire the 5 M-Auto-1B deferred hard gates inherited by M-Auto-1C (live iter end-to-end through Step 9 + drift envelope + first overnight + first human review + first cherry-pick). Establish baseline drift envelope BEFORE overnight per Codex M-Auto-1B Axis M6 trigger #1 acceptance. Finalize R-S58 disposition based on overnight propose-distribution scan evidence.

## Scope (6 steps)

1. **Pre-overnight readiness** (~2-4 hours):
   - **OQ-S62.3 workaround verified**: ensure dev session runs autoloop via raw shell / screen / tmux / `nohup ... &` outside Claude Code bash-tool sandbox. Do NOT run via Claude Code Bash tool with long-running invocation.
   - **Stale-branch cleanup option**: at S-Auto-8 open, deliver-agent + human jointly decide whether to run `git branch -D autoloop/exp-{2,6,7,8,10,11,12,13}` to clean up the 8 stale dev-loop branches from S-Auto-7.2 attempts. NOT required (OQ-S62.2 idempotency fix means S-Auto-8 targets exp-14 cleanly regardless); only for branch-listing noise reduction. Record decision in handoff §0.
   - **Foreground backend status**: verify the foreground :8080 backend is running OR ready to be auto-rebooted via S-Auto-7.1 `--auto-reboot` flag. Optionally run `python -m autoloop preflight` to confirm.
   - **Smoke iter end-to-end on real backend** (the Goal #3 measurement that S-Auto-7.2 could not capture via standard path due to OQ-S62.3): `python -m autoloop run --experiments 1` outside bash-tool sandbox; verify `experiments.jsonl` row written + non-degenerate `tier_evaluator_verdict` (Layer 0-4 all non-null) + per-iter elapsed time recorded. This is the FIRST captured measurement of full Spring-spawn + 46-case-eval + tier_evaluator cycle.

2. **Pre-batch baseline drift envelope (≥2 rerun; Codex M-Auto-1B Axis M6 trigger #1 acceptance)** — required BEFORE overnight kick-off:
   - Run the v1 46-case fitness suite TWO OR MORE TIMES BEFORE overnight:
     ```
     cd eval_interactive && uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1
     cd eval_interactive && uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4
     cd eval_interactive && uv run eval-interactive run --path case_specs_shadow/ --parallel 4
     ```
   - Establish per-suite median + IQR + bidirectional drift count. Record in `docs/sprints/sprint-063-handoff.md` §X "Baseline drift envelope".
   - If baseline-vs-baseline drift exceeds 10/34 cases (~30%; 2× the M-Auto-1A close-day signature; matches M-Auto-1B Phase 2's 7/34 = 20.6% elevated-but-in-envelope rate), surface to deliver-agent + human BEFORE starting overnight — provider drift may have widened; halt or proceed-with-widened-envelope is a joint call.
   - **PURPOSE**: the ≥2 rerun discipline is M-Auto-1C's primary risk mitigation against false-positive "keeps" produced by overnight comparison against a single noisy baseline run.

3. **Overnight batch**:
   - Invocation (outside bash-tool sandbox): `nohup python -m autoloop run --experiments 15 > autoloop-overnight.log 2>&1 &` in a screen / tmux session.
   - Target: 10-20 iterations; final count ≥10 is the close gate; budget 6-8h.
   - Each iteration writes to `autoloop/results/runs/exp-<N>/` + `experiments.jsonl` + `iterations.sqlite` + `lessons.md` (first K=10 lesson compaction triggers automatically if iteration count reaches K).
   - Crash recovery handles transient LLM API errors; if total errors >50% within first 5 iters, halt overnight + surface (loop crash recovery is M-Auto-1A S-Auto-3 substrate; M-Auto-1C does NOT add new recovery code).

4. **Propose-distribution scan for R-S58 evidence** (concurrent with overnight monitoring):
   - During overnight, accumulate per-iteration propose-stage real-meta-agent output corpus (the `propose` step outputs visible in `autoloop/results/runs/exp-<N>/propose_output.txt` or similar).
   - After overnight closes, scan corpus for U+200B / Unicode `category() == "Cf"` format characters in propose text (especially around WHEN / WHENEVER + arrow shapes; see R-S58 entry in `docs/action_bank.md` §5).
   - Record per-iteration occurrence count + frequency rate.
   - **PURPOSE**: provides evidence for R-S58 final disposition at M-Auto-1C close (defer-to-M-Auto-2 if 0 occurrences across 10-20 iters OR extend-with-S-Auto-9-Fix-D if bypass shape appears at meaningful rate).

5. **§5.6-style manual review of kept candidates** (morning after overnight):
   - Deliver-agent + human read per-turn traces of EACH kept candidate's bad_cases + anchor_outcome runs (open `eval_interactive/results/<run-id>/` + `autoloop/results/runs/exp-<N>/`).
   - For each kept candidate, judge PASS / FAIL / IMPROVING jointly (NOT programmatic alone), filter through the §2 drift envelope, and classify: **eligible for cherry-pick** (manual review PASS + no §5.3 borderline) OR **deferred to M-Auto-2+** (manual review PASS but borderline §5.3) OR **discarded** (manual review FAIL despite programmatic PASS).
   - **Use the M-Auto-1B Phase 2 REFERENCE SIGNALS as orientation**: cs001 (`resolve_faq_grounded_answer.yaml` procedure / critical_steps[*].desc gating `goal_impossible` terminal-state on policy-mandated search) + wmkb (`resolve_intake_collect_and_handover.yaml` critical_steps[*].desc gating handover on intake completion) are existing failure shapes the meta-agent MAY propose candidates for. The manual review specifically validates against the cs001 + wmkb per-turn traces if such candidates surface, BUT does NOT pre-commit any specific cherry-pick. Reference signals are orientation only.
   - Deliver-agent surfaces candidate slate to human via **AskUserQuestion**: each eligible cherry-pick candidate gets a row with (target_skill, target_field, edit_summary, programmatic verdict, manual review verdict, deliver-agent recommendation).

6. **AskUserQuestion cherry-pick decision + apply Hybrid + close**:
   - Human selects EXACTLY ONE candidate to cherry-pick OR 0 candidates with explicit "no human-approved candidate" justification (close PASS still possible on other gates).
   - For the selected candidate, execute `python -m autoloop apply --experiment exp-<N>` in Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit per OQ-S55.1). Human inspects `git status`, stages explicitly, commits manually with message `Sprint 063 / S-Auto-8 / M-Auto-1C — apply exp-<N> to main` and the standard deliver-agent footer.
   - After cherry-pick (or 0-cherry-pick close decision):
     - Record final observations (per-iteration elapsed-time average; cumulative FLAG rate across overnight; `shadow_disagreement_rate` first measurement against baseline envelope; gaming flag count + severity distribution).
     - Update `autoloop/config.yaml` `fitness.baseline_dir` to advance past the cherry-pick commit (if any).
     - Finalize R-S58 disposition based on §4 propose-distribution scan evidence.
     - Recommend Stage-2 entry decision direction for M-Auto-2+ planning round (≥3 cumulative cherry-picks across M-Auto-1B + M-Auto-1C + 0 borderline §5.3 cases would be a positive signal).
   - Author `docs/sprints/sprint-063-handoff.md` per §"Handoff requirements" below.

## Hard fences / STOP conditions

**Hard fences** (M-Auto-1C §6 17+2 list inherited; see `docs/milestone_objective.md` §6):

- **No edits** to `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-062-*`, prior milestone archives under `docs/milestones/`.
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at hash `22548e20…`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,gaming}.py`, `autoloop/autoloop/sandbox/applier.py` (fence #18 envelope FINALIZED at S-Auto-7.2 close; NO new edits), `autoloop/autoloop/sandbox/content_validator.py` (fence #19 envelope FINALIZED at S-Auto-7.2 close; NO new edits), `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py`, `autoloop/autoloop/cli.py`.
- **Skill YAML cherry-pick is ALLOWED EXACTLY ONCE** during S-Auto-8 — for ONE human-approved kept candidate via `python -m autoloop apply --experiment exp-<N>` Hybrid mode. Any additional kept candidates stay on `autoloop/keep-<N>` branches awaiting M-Auto-2+ review.
- **No new Tier-0 invariant**. C2/C3 DEFER continues per M2-close verdict.
- **No `git add -A`** — stage only S-Auto-8 scope files explicitly.

**STOP-and-surface conditions** (dev pauses + asks deliver-agent + human via AskUserQuestion):

- Baseline drift envelope >10/34 cases (~30%; 2× the M-Auto-1A close-day signature) → halt; provider drift may have widened materially.
- Overnight halts before iteration 5 with total errors >50% within first 5 iters → halt; LLM API + infrastructure investigation.
- Smoke iter end-to-end via raw shell (step 1) crashes through a NEW unobserved path → root-cause investigation; possibly S-Auto-8.1 fix-iteration.
- Overnight produces ZERO kept candidates AND deliver-agent + human jointly judge this is a meta-agent prompt issue → halt; surface as M-Auto-2 meta-prompt refinement R-item; close PASS still possible IF all other gates pass.
- Cherry-pick candidate touches a §1.7 borderline (e.g., the edit moves a soft signal toward keyword routing; or the procedure narrative softens an LLM-owned decision per §1.3 in a way human review finds ambiguous) → upgrade to per-sub-sprint Codex per §4.3 trigger #2 BEFORE cherry-pick commits to main.

## Test / eval requirements

- **Smoke iter via raw shell**: `experiments.jsonl` row captured with non-degenerate `tier_evaluator_verdict` (Layer 0-4 all non-null); per-iter elapsed time recorded.
- **Baseline rerun ≥2**: 3 NEW timestamped run-IDs per suite under `eval_interactive/results/` (or alternative invocation paths). Drift envelope per-suite median + IQR recorded in handoff.
- **Overnight ≥10 iters**: `experiments.jsonl` rows for all completed iters; `iterations.sqlite` rows; first K=10 lesson compaction if applicable.
- **autoloop pytest baseline preserved**: `cd autoloop && uv run --extra dev pytest -q` returns ≥ 266 PASS (S-Auto-7.2 close baseline). S-Auto-8 typically adds 0 new tests (execution-driven sub-sprint).
- **eval_interactive pytest baseline UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed`.
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns `31 passed`.
- **scoring_code_baseline_sha REASSERTED**: `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`; `_check_scoring_code_drift(config) == []` silent.
- **Java baseline UNCHANGED**: zero-touch verified via `git diff --stat HEAD -- server/ eval/src/main/java/` returning empty (or only the conditional Skill YAML row if cherry-pick lands).
- **§5.6 PRIMARY GATE manual review at M-Auto-1C close**: bad_cases (parallel=1) + anchor_outcome (parallel=4) + shadow (parallel=4) reruns + per-case PASS/FAIL/IMPROVING joint judgment recorded.

## §7 stanza (REQUIRED — semantic-touching via cherry-pick)

**Target failure layer**: `eval_spec` (per-iteration fitness verdict sequence consumption + manual review + cherry-pick decision; consumes cs001 + wmkb reference signals from M-Auto-1B Phase 2; potentially `semantic_planner` if a cherry-pick candidate edits Skill YAML procedure / critical_steps[*].desc fields).

**Tier-0 invariant**: This sub-sprint adds no Tier-0 invariant. C2/C3 candidates from M2 close remain DEFER unchanged. Any cherry-pick edit is on Skill YAML LLM-soft narrative fields (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc), NOT a Tier-0 invariant addition.

**Semantic hardcode**: No semantic hardcode introduced by S-Auto-8 dev session itself. Any cherry-pick candidate goes through the meta-loop sandbox + anti-hardcode detector at propose stage; sandbox guarantees the cherry-picked edit does NOT introduce keyword / regex / if-else / enum patterns per §1.7 forbidden list. The manual review at S-Auto-8 close validates this guarantee against per-turn traces.

**Generalization coverage**: target = the failure shape addressed by the cherry-picked candidate (if any; e.g., cs001 mechanical-template-escalate OR wmkb intake-handover-gating OR a different shape surfaced by overnight). Neighbor = other UC-C / UC-A cases that the same Skill YAML edit could affect. Negative = UC unaffected by the cherry-picked field-class. Shadow = the 22 shadow cases as regression-safety parity gate at M-Auto-1C close.

## Codex review plan (per §4.3)

**Default**: milestone-shared at M-Auto-1C close. Deliver-agent + human dispatch the M-Auto-1C milestone-shared review prompt at close-bundle commit (covers S-Auto-7.2 [already per-sub-sprint reviewed] + S-Auto-8 + cherry-pick commit if any).

**Trigger conditions** (dev STOP-and-surfaces if any fire):

- §4.3 trigger #2 (new Tier-0 candidate) DOES NOT fire (no Tier-0 invariant introduced).
- §4.3 trigger #1 (§1.7 forbidden-list red line) DOES NOT fire by sub-sprint structure — but COULD fire if a cherry-pick candidate's manual review surfaces a borderline §5.3 case (the "drift to keyword bot" edge where programmatic PASS but human judgment is split). In that case, upgrade S-Auto-8 to per-sub-sprint Codex BEFORE the cherry-pick commits to main per the original M-Auto-1C planning round Codex review plan.
- §4.3 trigger #3 (hard-fenced surface explicitly named out of scope) DOES NOT fire (cherry-pick on Skill YAML is the BLESSED path per §6 fence #3 + #7).

## Handoff requirements

`docs/sprints/sprint-063-handoff.md` author at sub-sprint close. Mandatory sections:

1. **§0 Sub-sprint summary**: Goal, scope, cumulative commit list, final test counts, smoke iter terminal verdict + baseline drift envelope summary + overnight iter count + cherry-pick decision + R-S58 disposition recommendation.
2. **§1 Cumulative changes**: per-commit + per-file LOC summary including the cherry-pick commit (if any); `git show --stat <commit>` outputs.
3. **§2 OQ-S62.3 workaround verification**: invocation method used (raw shell / screen / tmux / `nohup`); whether harness sandbox kill was avoided; smoke iter `experiments.jsonl` row capture evidence (THIS retires the M-Auto-1B Goal #3 deferral inherited by M-Auto-1C).
4. **§3 Pre-batch baseline drift envelope**: 3 NEW run-IDs per suite; per-suite median + IQR + bidirectional drift count; halt-or-proceed-with-widened-envelope decision recorded.
5. **§4 Overnight batch evidence**: iteration count + per-iteration verdict distribution (keep / discard / error counts); per-iter elapsed time average + min/max; first K=10 lesson reference (if applicable); LLM API error rate.
6. **§5 R-S58 propose-distribution scan evidence**: per-iteration U+200B / Cf occurrence count + frequency rate; final R-S58 disposition recommendation.
7. **§6 §5.6 manual review**: deliver-agent + human joint per-candidate PASS/FAIL/IMPROVING/borderline-§5.3 classifications; per-candidate edit summary; deliver-agent recommendation.
8. **§7 Cherry-pick decision via AskUserQuestion**: AskUserQuestion record; human selection (1 candidate OR 0 with justification); apply Hybrid evidence; human manual commit signature.
9. **§8 OQs surfaced (if any)**: any new OQ-S63.x.
10. **§9 STOP-and-surface log (if any)**: timestamps + AskUserQuestion records.
11. **§10 M-Auto-1C close readiness checklist**: 13 hard gates from M-Auto-1C §5 acceptance bar tick-off; Stage-2 entry decision recommendation direction for M-Auto-2+ planning.

## Commit discipline

- **Multi-commit pattern acceptable** (S-Auto-8 is execution-driven; not subject to single-commit preference of S-Auto-7.2 substrate fix).
- **Commit message format**: `Sprint 063 / S-Auto-8 / M-Auto-1C — <commit description>` with bullet-point body summarizing the per-commit scope.
- **Standard deliver-agent footer** at commit message end.
- **No `git add -A`** — stage explicitly. Each commit stages only the relevant scope files (e.g., baseline rerun results separately from overnight results separately from cherry-pick commit separately from handoff).

## Self-check (dev MUST verify before claiming done)

- [ ] OQ-S62.3 workaround verified: smoke iter via raw shell captured `experiments.jsonl` row with non-degenerate `tier_evaluator_verdict`.
- [ ] Pre-batch baseline rerun ≥2 completed; drift envelope recorded; proceed-OR-halt decision documented.
- [ ] Overnight ≥10 iterations completed; per-iter verdicts serialized.
- [ ] R-S58 propose-distribution scan completed; per-iter occurrence count recorded.
- [ ] §5.6 manual review of kept candidates jointly conducted; per-candidate classification recorded.
- [ ] AskUserQuestion cherry-pick decision recorded; either 1 candidate cherry-picked OR 0 with justification.
- [ ] (If cherry-pick lands) apply Hybrid + human manual commit successful; `config.fitness.baseline_dir` advances.
- [ ] autoloop pytest passes (≥266 PASS).
- [ ] eval_interactive baseline UNCHANGED (486 passed, 3 failed).
- [ ] 17-fixture detector sweep PASS UNCHANGED.
- [ ] scoring SHA reasserted; `_check_scoring_code_drift(config) == []` silent.
- [ ] Hard-fence diff cumulative against all M-Auto-1C §6 gated paths returns empty (or only the conditional Skill YAML row if cherry-pick).
- [ ] Handoff §0-§11 sections all filled per format.
- [ ] M-Auto-1C close readiness: 13 hard gates tick-off (10 from M-Auto-1B inherited + 3 new); Stage-2 entry decision recommendation direction documented.
