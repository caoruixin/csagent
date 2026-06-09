---
title: Sprint 065 / S-Auto-10 — First overnight batch + first human review + first cherry-pick to main (M-Auto-2 — Local-Mac OQ-S62.3 diagnostic + first overnight + first cherry-pick sub-sprint 2 of 2-3)
doc_tier: current-runtime
status: current
implementation_status: not_started
source_of_truth: this file
last_reviewed: 2026-05-31
review_cadence: per sub-sprint
supersedes: [docs/sprints/sprint-064-objective.md]
superseded_by: null
notes: >
  M-Auto-2 / Sprint 065 / S-Auto-10. **Layer**: `eval_spec` (per-iteration
  fitness verdict on now-reliably-executable substrate; §5.6 manual
  review + cherry-pick are eval-side acceptance bars; semantic-touching
  via cherry-pick on Skill YAML LLM-soft fields if it lands). **§7
  stanza**: **REQUIRED** (semantic-touching via cherry-pick). **Codex
  review plan**: milestone-shared (default) at M-Auto-2 close UNLESS
  cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint
  per §4.3 trigger #2 pre-milestone-close).

  **Estimated dev**: ~3-5 days total (≤1 day execution including pre-batch
  baseline rerun + overnight batches + propose-distribution scan +
  1-2 days deliver-agent + human §5.6 manual review + cherry-pick
  decision + apply + handoff + close-bundle).

  **Substrate state inherited from S-Auto-9** (now reliably executable
  via the fence #20 applier.py fixes; smoke iter exp-18 confirmed Step
  9 reach in ~71s with non-null 5-layer verdict):
  - **OQ-S62.3 RESOLVED**: `_spawn_spring` now uses `start_new_session=True`
    so the orchestrator no longer suicides via `_terminate_process`'s
    `killpg`.
  - **OQ-S64.1 RESOLVED**: `_spawn_spring` routes mvn stdout to a per-port
    log file (`autoloop/results/spring-boot-<port>.log`), preventing the
    64KB pipe-buffer deadlock that would have frozen the backend mid-eval.
  - **OQ-S64.2 RESOLVED**: `_probe_url_is_up` uses `httpx.Client(trust_env=False)`,
    bypassing the macOS system proxy (`127.0.0.1:7890`) that produced
    spurious 120s `SpringStartupTimeoutError` in sprint-063.
  - **Smoke wrapper**: `scripts/sprint-064-step4-smoke-iter.sh N` is a
    proven launcher pattern (NI=0, signal-trap, foreground or detached);
    S-Auto-10 may use this OR `python -m autoloop run --experiments N`
    directly.

  **OQ-S64.3 observation (carry-over)**: full iterations now run in ~71s
  end-to-end on local Mac, so a 15-iter batch may complete in ~15-20 min
  (vs the original M-Auto-1C S-Auto-8 6-8h budget assumption). The
  6-8h budget is **demoted to observation**; the M-Auto-2 §5 hard gate
  is **≥10 iterations completed end-to-end** regardless of wall time.
  S-Auto-10 plans 15-iter first batch + optional second 30-iter batch
  within same sub-sprint to exercise lessons compaction at K=10 + K=20
  + accumulate richer R-S58 propose-distribution + richer §5.6 manual
  review pool.

  **CRITICAL constraint (human-locked 2026-05-30 + inherited from M-Auto-2)**:
  M-Auto-2 stays on local Mac. **DO NOT pursue cloud / remote-server
  framing**. The fence #20 applier.py fixes have resolved the local-Mac
  substrate brittleness; cloud framing is not needed.

  **Fast-iteration STOP-and-surface authorization** (per S-Auto-7.2 +
  S-Auto-9 precedent + user direction 2026-05-30 "可以我review后快速放
  宽通过"): if S-Auto-10 dev surfaces additional substrate brittleness
  during overnight execution, dev STOP-and-surfaces via AskUserQuestion;
  human reviews + quickly authorizes. NEW fence touch beyond M-Auto-2
  §6 17+3 envelope would re-trigger §4.3 per-sub-sprint Codex.

  **Inherited Codex M-Auto-1C downgrade-to-signal triggers (must address
  during S-Auto-10 execution + handoff)**:

  1. **Drift-envelope metric hygiene at S-Auto-10 handoff §X baseline
     drift envelope**: MUST explicitly cite loader-counted `case_passed`
     (per `composite.py` + `baseline_loader.load`) AND avoid the CLI
     `Passed: N` headline (which is structurally `0` when judge_score
     uniform 0.0 per `R-eval-interactive-judge-score-never-populated`).
     Also reconcile any stale M-Auto-1C-style metric references
     encountered during handoff authoring.
  2. **cv ceiling calibration discipline at S-Auto-10 overnight**: do
     NOT bump `autoloop/config.yaml` `length_overflow_absolute_ceiling`
     (currently 1200 post-S-Auto-8) on a single anecdote unless overnight
     propose-distribution evidence shows a stable short-field expansion
     band. Record PASS / FAIL bands from real proposals across the
     overnight batch BEFORE further tuning. Codex flagged the M-Auto-1C
     1000→1200 bump as thin-evidence (single exp-13 data point);
     S-Auto-10 should accumulate ≥5-10 propose-stage data points before
     considering further adjustment.
  3. **Prompt/template hygiene at S-Auto-10 dev prompt + future review
     prompts**: the stale scoring-drift command (use
     `yaml.safe_load(Path('config.yaml').read_text())` NOT
     `from autoloop.config import load`) AND the `sandbox/gaming.py`
     path typo (gaming.py is in `autoloop/autoloop/scoring/` NOT
     `autoloop/autoloop/sandbox/`) have been corrected in this
     sprint_objective + the matching `compact/sprint-065-dev-prompt.md`.
     Verify at handoff time that no stale references re-appear.

  **Bad-case cherry-pick reference signals** (UNCHANGED from M-Auto-1C):
  `cs001_uc_c_mechanical_template_escalate` (resolve_faq_grounded_answer.yaml
  procedure / critical_steps[*].desc gating `goal_impossible` terminal-
  state on policy-mandated `search_knowledge` completion) +
  `wmkb_uc_a_trader_flag_secondary_uc_h` (resolve_intake_collect_and_
  handover.yaml critical_steps[*].desc gating handover on intake
  completion). **REFERENCE SIGNALS for S-Auto-10 manual review** —
  meta-agent owns proposal authorship; cherry-pick decision via
  AskUserQuestion on actual overnight kept-candidate slate. These are
  hints to direct manual review attention, NOT prescriptive cherry-pick
  targets.

  **Cherry-pick discipline** (UNCHANGED from M-Auto-1C):
  - At most **1** cherry-pick lands at S-Auto-10 close (M-Auto-2 §4
    non-goals reasserts).
  - Cherry-pick targets Skill YAML LLM-soft fields only (procedure /
    grounding_instruction / escalation_policy / critical_steps[*].desc).
  - Cherry-pick must be sandbox-validated, anti-hardcode PASS, gaming
    0-ERROR.
  - Cherry-pick decision via AskUserQuestion: human selects exactly ONE
    candidate OR 0 with explicit "no human-approved candidate"
    justification.
  - Apply via `python -m autoloop apply --experiment exp-<N>` Hybrid
    mode (per OQ-S55.1: cherry-pick + emit baseline patch + NO
    auto-commit; human manually commits with standard deliver-agent
    footer).
  - If cherry-pick lands: `config.fitness.baseline_dir` advances past
    the cherry-pick commit so next milestone's baseline is post-cherry-
    pick.

  Dev session source-of-truth: `compact/sprint-065-dev-prompt.md`
  (self-contained per `iteration_governance.md` §9). Dev session reads
  ONLY: `AGENTS.md` (auto-loaded) + the dev prompt.
---

# Sprint 065 / S-Auto-10 — First overnight batch + first human review + first cherry-pick to main

## Class

- **Layer (primary)**: `eval_spec` (per-iteration fitness verdict on now-reliably-executable substrate; §5.6 manual review + cherry-pick are eval-side acceptance bars; semantic-touching via cherry-pick on Skill YAML LLM-soft fields if it lands).
- **§7 stanza**: **REQUIRED** (semantic-touching via cherry-pick on Skill YAML LLM-soft fields; if 0-cherry-pick S-Auto-10 close, the stanza framing remains required for paper-trail).
- **Codex review plan (§4.3)**: milestone-shared (default) at M-Auto-2 close UNLESS cherry-pick candidate borderline-§5.3 surfaces (then per-sub-sprint per §4.3 trigger #2 pre-milestone-close).
- **Sub-sprint position in milestone**: 2nd of 2-3 (S-Auto-9 OQ-S62.3 diagnostic + resolution CLOSED → **S-Auto-10 first overnight + first cherry-pick** → optional S-Auto-11 fix-iteration buffer → M-Auto-2 close).

## Goal

Exercise the now-reliably-executable substrate end-to-end via first overnight batch + first §5.6 manual review of kept candidates + first cherry-pick decision via AskUserQuestion. Retire M-Auto-1C §12.4 deferred hard gates #2-#4 (first overnight ≥10 iter + first review + first cherry-pick). Surface evidence for R-S58 reopen check (propose-distribution scan for Cf observations) + M-Auto-2 close + post-M-Auto-2 planning (Stage-2 unlock direction).

**Acceptance**: ≥10 iterations completed end-to-end via the now-resolved substrate; per-iter rows written to `autoloop/results/experiments.jsonl` with non-null `verdict.layer_results`; ≥1 propose-distribution data point per overnight (R-S58 reopen check via Cf-char scan); cherry-pick decision via AskUserQuestion (exactly ONE or 0 with explicit justification); if cherry-pick lands, human commit visible on `auto-loop-branch` (NOT main per M-Auto-2 framing — cherry-pick to `main` is the apply target via `autoloop apply` Hybrid mode + human commit on `auto-loop-branch`).

## Scope (6 steps; re-scoped per OQ-S64.3)

1. **Pre-flight env check + pre-batch baseline rerun ≥2** (~half-day):
   - Verify foreground :8080 backend is running (auto-reboot via `--auto-reboot` flag if needed).
   - Verify substrate health: `scripts/sprint-064-step4-smoke-iter.sh 1` reaches Step 9 with non-null 5-layer verdict (smoke confirmation; should match S-Auto-9 exp-18 evidence).
   - Run pre-batch baseline rerun ≥2 (inherited Codex M-Auto-1B Axis M6 acceptance + M-Auto-1C close-day rerun precedent):
     ```bash
     cd eval_interactive
     uv run eval-interactive run --path case_specs/bad_cases/ --parallel 1
     uv run eval-interactive run --path case_specs/anchor_outcome/ --parallel 4
     uv run eval-interactive run --path case_specs_shadow/ --parallel 4
     ```
     Repeat each suite ≥2 times. Record per-suite loader-counted `case_passed` envelope (per Codex M-Auto-1C downgrade trigger #2 — explicit loader-counted, NOT CLI `Passed:N` headline). HALT if drift exceeds 10/34 cases (~30%); deliver-agent + human jointly investigate.

2. **First overnight batch** (~30 min wall time at ~71s/iter):
   - Run `python -m autoloop run --experiments 15 --auto-reboot` via raw shell / Terminal / iTerm OR via `scripts/sprint-064-step4-smoke-iter.sh 15` (S-Auto-9 proven wrapper pattern).
   - Verify substrate doesn't OS-kill (S-Auto-9 fixes hold).
   - Capture: per-iter wall time + per-iter verdict.layer_results + lessons.md compaction at iter 10 (K=10 trigger).
   - Expected: ≥10 iter complete with non-null verdicts; if <10 due to substrate halt, STOP-and-surface (likely OQ-S64.x-style residual brittleness; halt + investigate).

3. **Optional second batch (CONDITIONAL on first batch success)** (~1-1.5h wall time):
   - If first batch completed cleanly, dev MAY run a second batch of 30 iter to exercise:
     - Lessons compaction at K=20 (second trigger).
     - Richer R-S58 propose-distribution scan (more propose-stage data points across two batches; ≥5-10 needed per Codex M-Auto-1C trigger #3).
     - Richer §5.6 manual review candidate pool (more kept candidates → more cherry-pick options).
   - If second batch surfaces ANY new substrate brittleness, STOP-and-surface; do NOT continue blindly. Second batch is OPTIONAL; cleanliness of first batch + sufficiency for cherry-pick decision are the gating questions.
   - If dev judges second batch unnecessary (e.g., first batch already produced a clean cherry-pick candidate slate), skip and proceed to step 4.

4. **Propose-distribution scan for R-S58 reopen check** (~half-day):
   - Across all overnight iterations (first batch + optional second batch), scan `autoloop/results/runs/exp-*/proposal-*` artefacts (or equivalent propose-stage artefacts per actual layout) for **Cf (zero-width / format-control) characters** in proposed Skill YAML content.
   - Expected: 0 Cf characters across ALL propose-stage data points (consistent with M-Auto-1C S-Auto-8 baseline scan 0/13 historical experiments.jsonl rows).
   - If ≥1 Cf observation: re-promote R-S58 from CLOSED-AS-THEORETICAL-ONLY to ACTIVE per the reopen condition in `docs/action_bank.md` §5.2; route to a Fix-D-style follow-on (potentially S-Auto-11 fix-iteration buffer if it's in-scope for M-Auto-2 close; otherwise carry-over to M-Auto-3+).

5. **§5.6 manual review of kept candidates + cherry-pick decision** (~1-2 days):
   - Identify the kept-candidate slate (per-iteration verdicts with Layer 1-4 all PASS; Layer 0 PASS prerequisite).
   - Deliver-agent + human jointly read per-turn traces for each kept candidate using the §5.6 manual-review process (read `case_results[].per_turn_trace[]`; classify PASS / FAIL / IMPROVING / borderline-§5.3 per candidate).
   - Bad-case reference signals (UNCHANGED from M-Auto-1C): `cs001_uc_c_mechanical_template_escalate` + `wmkb_uc_a_trader_flag_secondary_uc_h`. These are hints to direct manual review attention, NOT prescriptive targets.
   - Run AskUserQuestion at cherry-pick decision: human selects EXACTLY ONE candidate OR 0 with explicit "no human-approved candidate" justification.
   - If cherry-pick lands:
     - Run `python -m autoloop apply --experiment exp-<N>` in Hybrid mode (cherry-pick + emit baseline patch + NO auto-commit per OQ-S55.1).
     - Human manually commits with standard deliver-agent footer.
     - `config.fitness.baseline_dir` advances past the cherry-pick commit.
     - The cherry-pick edit is constrained to Skill YAML LLM-soft fields (procedure / grounding_instruction / escalation_policy / critical_steps[*].desc) per fence #3.

6. **OQ ledger update + handoff + close-bundle** (~half-day):
   - Update `docs/sprints/sprint-065-handoff.md` per §"Handoff requirements" below.
   - Surface any new OQs encountered (e.g., per-iteration elapsed time observations, lessons compaction notes, propose-distribution observations).
   - Record M-Auto-2 close-readiness checklist: all 13 acceptance bar items addressed by S-Auto-10 contribution.
   - Record R-S58 disposition: CLOSED-AS-THEORETICAL-ONLY confirmed OR re-promoted to ACTIVE.
   - Surface Stage-2 entry decision direction for M-Auto-3+ planning (≥3 kept human-approved cherry-picks + 0 borderline §5.3 cases would be positive Stage-2 unlock signal; M-Auto-2 itself adds at most 1 cherry-pick).

## Hard fences / STOP conditions

**Hard fences** (M-Auto-2 §6 17+3 list inherited post-§8.3 revision):

- **No edits** to `server/src/main/java/**`, `eval/src/main/java/**`, `eval_interactive/eval_interactive/**`, `eval_interactive/case_specs/**`, `eval_interactive/case_specs_shadow/**`, `data/**`, `db/**`, `docs/foundational/**`, `docs/runtime_freeze_and_risk_policy.md`, `docs/current/**`, `docs/teams/**`, sprint archives `docs/sprints/sprint-001-*` through `docs/sprints/sprint-064-*`, prior milestone archives under `docs/milestones/` (including `M-Auto-1B_*` + `M-Auto-1C_*` archives).
- **No edits** to `autoloop/autoloop/scoring/{tier_evaluator,eval_runner,baseline_loader,gaming}.py` (fence #13 reasserted at hash `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`).
- **No edits** to `autoloop/autoloop/sandbox/{anti_hardcode_check,content_validator}.py` (fence #15 + #19 envelopes FINALIZED at M-Auto-1C close).
- **No edits** to `autoloop/autoloop/sandbox/applier.py` (fence #18 FINALIZED at M-Auto-1C close + fence #20 envelope FINALIZED at S-Auto-9 close; S-Auto-10 MUST NOT edit the fence #20 surface or any other applier.py line).
- **No edits** to `autoloop/autoloop/loop.py`, `autoloop/autoloop/meta_agent/**`, `autoloop/autoloop/memory/**`, `autoloop/autoloop/preflight.py`, `autoloop/autoloop/cli.py`, `autoloop/config.yaml`.
- **Skill YAML edits** in `server/src/main/resources/skills/**`: writable EXACTLY ONCE via the cherry-pick mechanism per fence #3. Cherry-pick must be sandbox-validated, anti-hardcode PASS, gaming 0-ERROR, AND approved via AskUserQuestion. At most **1** cherry-pick at S-Auto-10 close.
- **No new Tier-0 invariant**.
- **No `git add -A`** — stage only S-Auto-10 scope files explicitly (handoff + cherry-pick Skill YAML if it lands + result artefacts under `autoloop/results/runs/exp-*/` if those are checked in per existing convention; otherwise gitignored).
- **No cloud / remote-server framing**: M-Auto-2 stays local-Mac per human-locked direction 2026-05-30; substrate now reliably executable post-S-Auto-9.

**STOP-and-surface conditions** (dev pauses + asks deliver-agent + human via AskUserQuestion):

- §1 pre-batch baseline drift envelope >10/34 cases: halt; deliver-agent + human investigate (likely LLM-provider variance beyond M-Auto-1A signature; possibly post-S-Auto-9-substrate change exposed new variance).
- §2 first overnight batch halts before iteration 5 (total errors >50% within first 5 iters): halt; LLM API + infrastructure investigation.
- §2 first overnight surfaces a substrate brittleness NOT explained by OQ-S62.3 / OQ-S64.1 / OQ-S64.2 (e.g., a previously-unobserved Spring spawn pathology surfaces): halt; root-cause investigation; possibly fix-iteration S-Auto-10.1 OR S-Auto-11.
- §3 optional second batch surfaces ANY new substrate brittleness (do NOT continue blindly).
- §4 propose-distribution scan surfaces ≥1 Cf observation: surface to deliver-agent + human; route to R-S58 re-promotion + Fix-D follow-on (potentially S-Auto-11 if in-scope for M-Auto-2 close).
- §5 cherry-pick candidate touches §1.7 forbidden-list borderline (e.g., proposal encodes visible-eval case text or trace-specific phrasing): halt; per §4.3 trigger #2 the cherry-pick triggers per-sub-sprint Codex pre-milestone-close; deliver-agent + human reconsider cherry-pick selection.
- §5 §5.6 manual review surfaces a NEW failure shape NOT in the cherry-pick scope: halt; surface as new bad-case candidate for `bad_cases/_manifest.md` consideration at M-Auto-2 close.
- Hard-fence touch required beyond M-Auto-2 §6 17+3 envelope: halt; deliver-agent + human authorize NEW fence override OR redirect.
- Human cancels mid-sprint due to time / priority shift.

## Test / eval requirements

- **autoloop pytest baseline preserved**: `cd autoloop && uv run --extra dev pytest -q` returns ≥ 266 PASS (S-Auto-9 close baseline UNCHANGED). S-Auto-10 typically adds 0 new tests (cherry-pick is Skill YAML edit; auto-loop result artefacts are runtime, not test).
- **eval_interactive pytest baseline UNCHANGED**: `cd eval_interactive && uv run python -m pytest --tb=no -q` returns `486 passed, 3 failed`.
- **17-fixture detector sweep UNCHANGED**: `cd autoloop && uv run --extra dev pytest -p no:cacheprovider -q tests/test_anti_hardcode_check.py` returns `31 passed`.
- **scoring_code_baseline_sha REASSERTED**: `cd autoloop && uv run --extra dev python -c "from autoloop.scoring.gaming import _compute_scoring_code_sha; print(_compute_scoring_code_sha())"` returns `22548e20ea50518b35c78cebddde891ddb46ea0452e32e49518ad4caab9188a9`; `_check_scoring_code_drift(config) == []` silent (per `yaml.safe_load(Path('config.yaml').read_text())` invocation pattern — **NOT** `from autoloop.config import load`; per Codex M-Auto-1C downgrade trigger #4).
- **Java baseline UNCHANGED**: zero-touch verified via `git diff --stat <S-Auto-9-close>..HEAD -- server/ eval/src/main/java/` returning empty.
- **Pre-batch baseline drift envelope**: ≥2 reruns per suite; per-suite loader-counted `case_passed` envelope cited (per Codex M-Auto-1C downgrade trigger #2); halt if drift >10/34 cases.
- **≥10 iterations completed end-to-end at S-Auto-10 close**: hard gate per M-Auto-2 §5 acceptance bar item 7.
- **At most 1 Skill YAML cherry-pick**: passes sandbox + anti-hardcode + gaming 0-ERROR + AskUserQuestion approval.

## §7 stanza (REQUIRED — semantic-touching via cherry-pick)

**Target failure layer**: `eval_spec` per `iteration_governance.md` §3.2 question 6 (eval CaseSpec or judge asking the system to do something it can / should do — the cherry-pick mechanism consumes the per-iteration fitness verdict sequence; the §5.6 manual review + cherry-pick selection ARE the eval-side acceptance bars). The cherry-pick edit itself, if it lands, is a Skill YAML LLM-soft field edit — semantic-touching but per fence #14 constrained to NOT encode case wording / user utterance / expected answer / case-status label.

**Tier-0 invariant**: this sub-sprint adds no Tier-0 invariant. C2/C3 candidates from M2 close remain DEFER unchanged. The cherry-pick edit (if any) does NOT introduce a Tier-0 invariant; it modifies a Skill YAML LLM-soft field within the existing runtime contract.

**Semantic hardcode**: no semantic hardcode introduced **at the sub-sprint scope**. The cherry-pick edit itself MUST pass the anti-hardcode detector (17-fixture sweep + propose-stage anti-hardcode check; 0-ERROR gating); the §5.6 manual review + Codex review (per §4.3 trigger #2 if borderline) verify no §1.7 forbidden-list red line crossed. If the cherry-pick is borderline (e.g., the proposal narrowly encodes specific phrasing), human declines via AskUserQuestion 0-cherry-pick option.

**Generalization coverage**: target = the cherry-pick's specific failure-shape (per `bad_case_metadata.failure_shape` if applicable). Neighbor = other UC variants of the same failure shape (typically observed within the overnight kept-candidate slate). Negative-control = bad cases the cherry-pick should NOT trigger (typically observed within the overnight discarded-trajectory slate). Shadow = held-out shadow cases (`case_specs_shadow/`); shadow drop ≤3% per M-Auto-2 §5 acceptance bar item 11. Final coverage matrix recorded in handoff §X if cherry-pick lands.

## Codex review plan (per §4.3)

**Default**: milestone-shared at M-Auto-2 close. Deliver-agent + human dispatch the M-Auto-2 milestone-shared review prompt at close-bundle commit (covers S-Auto-9 + S-Auto-10 + cherry-pick commit if any + optional S-Auto-11).

**Trigger conditions** (dev STOP-and-surfaces if any fire):

- §4.3 trigger #1 (new Tier-0 candidate) DOES NOT fire (no Tier-0 invariant introduced).
- §4.3 trigger #2 (§1.7 forbidden-list red line) **CAN FIRE** if cherry-pick candidate borderline §5.3 (e.g., proposal narrowly encodes specific phrasing that crosses §1.7 borderline). In that case, upgrade S-Auto-10 to PER-SUB-SPRINT Codex per §4.3 trigger #2 pre-milestone-close BEFORE M-Auto-2 milestone-shared dispatches. Authorization follows S-Auto-7.2 + S-Auto-9 fast-iteration precedent.
- §4.3 trigger #3 (hard-fenced surface) DOES NOT fire (S-Auto-10 MUST NOT edit the fence #20 surface OR any other hard-fenced surface beyond the fence #3 cherry-pick mechanism on Skill YAML).
- §4.3 trigger #4 (fix_required on prior sub-sprint) DOES NOT fire UNLESS S-Auto-9 Codex per-sub-sprint review returned `fix_required`. (Expected: S-Auto-9 Codex returns `approve` or `approve with downgrade-to-signal follow-up`.)

## Handoff requirements

`docs/sprints/sprint-065-handoff.md` author at sub-sprint close. Mandatory sections:

1. **§0 Sub-sprint summary**: Goal, scope, cumulative commit list, final test counts, overnight iter counts (first batch + optional second), §5.6 manual review summary, cherry-pick decision (1 or 0), R-S58 disposition, M-Auto-1C §12.4 deferred hard gates #2-#4 retirement confirmation.
2. **§1 Pre-flight + pre-batch baseline drift envelope**: foreground :8080 backend health + smoke iter confirmation + per-suite loader-counted `case_passed` envelope across ≥2 reruns per suite (explicit loader-counted citation per Codex M-Auto-1C trigger #2).
3. **§2 First overnight batch**: launch + PID + per-iter elapsed time + per-iter verdict.layer_results + lessons compaction at K=10.
4. **§3 Optional second batch (if run)**: per above format.
5. **§4 Propose-distribution scan**: per-iter propose-stage Cf observation counts (expected 0; R-S58 reopen condition tracked).
6. **§5 §5.6 manual review**: kept-candidate slate + per-candidate PASS / FAIL / IMPROVING / borderline-§5.3 classification + cherry-pick decision rationale.
7. **§6 Cherry-pick apply (if any)**: AskUserQuestion record + apply Hybrid mode evidence + human commit hash + `config.fitness.baseline_dir` advancement + sandbox / anti-hardcode / gaming PASS evidence.
8. **§7 R-S58 disposition**: CLOSED-AS-THEORETICAL-ONLY confirmed OR re-promoted to ACTIVE; if re-promoted, Fix-D follow-on plan + S-Auto-11 in-scope evaluation.
9. **§8 OQs surfaced (if any)**: any new OQ-S65.x carry-over to S-Auto-11 OR M-Auto-3.
10. **§9 STOP-and-surface log (if any)**: timestamps + AskUserQuestion records.
11. **§10 M-Auto-2 close-readiness checklist (S-Auto-10 contribution)**: tick-off M-Auto-2 §5 acceptance bar items addressed by S-Auto-10.
12. **§11 Stage-2 entry decision direction**: based on cumulative M-Auto-1B + M-Auto-1C + M-Auto-2 evidence (kept human-approved cherry-pick count; borderline §5.3 count); deliver-agent + human direction for M-Auto-3+ planning.

## Commit discipline

- **Multi-commit pattern acceptable** (S-Auto-10 may produce multiple commits: cherry-pick commit + handoff commit + optional result-artefact commit). Single-commit pattern preferred where possible.
- **Cherry-pick commit (if any)**: dedicated commit isolating the Skill YAML LLM-soft field edit ONLY; no co-commit with handoff or other scope.
- **Handoff commit**: handoff + any auto-loop result artefacts that are checked in per existing convention.
- **Commit message format**: `Sprint 065 / S-Auto-10 / M-Auto-2 — <commit description>` with bullet-point body summarizing the per-commit scope.
- **Standard deliver-agent footer** at commit message end.
- **No `git add -A`** — stage explicitly. Each commit stages only the relevant scope files.

## Self-check (dev MUST verify before claiming done)

- [ ] Pre-flight env check passed: foreground :8080 backend healthy.
- [ ] Smoke iter confirmation: 1 iter reaches Step 9 with non-null 5-layer verdict (S-Auto-9 substrate fixes hold).
- [ ] Pre-batch baseline rerun ≥2 executed: per-suite loader-counted `case_passed` envelope cited (NOT CLI `Passed:N` headline; per Codex M-Auto-1C trigger #2).
- [ ] First overnight batch ≥10 iter completed: per-iter wall time + verdict.layer_results captured.
- [ ] Optional second batch (if run): same evidence format.
- [ ] Propose-distribution scan: per-iter Cf observation count documented.
- [ ] §5.6 manual review: kept-candidate slate + per-candidate classification recorded.
- [ ] AskUserQuestion cherry-pick decision: exactly 1 OR 0 with explicit justification.
- [ ] If cherry-pick landed: apply Hybrid mode + human commit + baseline_dir advancement.
- [ ] R-S58 disposition recorded.
- [ ] autoloop pytest passes (≥266 PASS).
- [ ] eval_interactive baseline UNCHANGED (486 passed, 3 failed).
- [ ] 17-fixture detector sweep PASS UNCHANGED.
- [ ] scoring SHA reasserted; `_check_scoring_code_drift(config) == []` silent.
- [ ] Java baseline UNCHANGED (zero-touch verified).
- [ ] Hard-fence diff cumulative against all M-Auto-2 §6 17+3 gated paths returns empty (or only the cherry-pick Skill YAML surface).
- [ ] Handoff §0-§12 sections all filled per format.
- [ ] M-Auto-2 §5 acceptance bar S-Auto-10 contribution items ticked off.
- [ ] Local-Mac constraint honored throughout (NO cloud / remote-server framing introduced).
- [ ] cv ceiling discipline honored (NO bump to `length_overflow_absolute_ceiling` unless ≥5-10 propose-stage data points justify; per Codex M-Auto-1C trigger #3).
- [ ] No stale scoring-drift command references (use `yaml.safe_load(...)`; NOT `from autoloop.config import load`).
- [ ] No `sandbox/gaming.py` path typos (gaming.py is in `autoloop/autoloop/scoring/`).
